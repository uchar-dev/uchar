package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import dev.uchar.regelspraak.cases.CaseRunner.Excluded;
import dev.uchar.regelspraak.engine.BeanCompiler;
import dev.uchar.regelspraak.graaf.ProducentConsument;
import dev.uchar.regelspraak.graaf.ProducentConsument.ElementSleutel;
import dev.uchar.regelspraak.ir.ResolvedStartpunt;
import dev.uchar.regelspraak.loader.IrLoader;
import dev.uchar.regelspraak.model.RegelSpraakModel;
import dev.uchar.regelspraak.model.RegelSpraakModel.Objecttype;
import dev.uchar.regelspraak.schema.ContractValidator;
import dev.uchar.regelspraak.schema.FactModelGenerator;
import dev.uchar.regelspraak.transpiler.DrlTranspiler;
import dev.uchar.regelspraak.transpiler.kern.Dekking;
import dev.uchar.regelspraak.transpiler.model.ExecModelTranspiler;
import dev.uchar.regelspraak.transpiler.tms.Creatiefase;
import dev.uchar.regelspraak.transpiler.tms.HeeftKenmerk;
import dev.uchar.regelspraak.transpiler.tms.Koppeling;
import dev.uchar.regelspraak.transpiler.tms.Kringen;
import dev.uchar.regelspraak.transpiler.tms.TmsTranspiler;
import dev.uchar.regelspraak.transpiler.tms.Waarde;

import dev.uchar.regelspraak.runtime.Breuk;

import org.drools.modelcompiler.KieBaseBuilder;
import org.drools.modelcompiler.constraints.ConstraintEvaluationException;
import org.kie.api.KieBase;
import org.kie.api.conf.EqualityBehaviorOption;
import org.kie.api.event.rule.AfterMatchFiredEvent;
import org.kie.api.event.rule.AgendaEventListener;
import org.kie.api.event.rule.DefaultAgendaEventListener;
import org.kie.api.event.rule.DefaultRuleRuntimeEventListener;
import org.kie.api.event.rule.ObjectInsertedEvent;
import org.kie.api.event.rule.RuleRuntimeEventListener;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.rule.ConsequenceException;
import org.kie.api.runtime.rule.FactHandle;
import org.kie.internal.utils.KieHelper;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;

/**
 * The public entry of the Drools backend: one {@code uchar-ir-v1} document in, a built {@link KieBase} out, along
 * the path gate 12 replays. {@link CaseRunner} builds every golden through {@link #build(JsonNode, Branch)}, so what
 * a consumer fires is what the gate plays; there is no second copy of this path to drift from it.
 *
 * <p>The consumer it exists for is the toolchain distribution — CLI, decision service, scenario runner — that
 * builds this repository as a git submodule and fires rule sets through it: a source goes through the frontend
 * ({@code Resolver.resolve}, then {@code IrEmitter.emit}) and the emitted document comes here. What the loader
 * returns beside the model — the ch. 7 prioritized defaults, the role bindings, the Let index, the fold-source
 * paths, the navigation bindings — reaches the transpiler here and nowhere else. A consumer that built the
 * transpiler itself could leave one out and still get a KieBase: a defaults family then fires nowhere and a
 * navigated read is answered as the subject's own, with no marker and no error.
 *
 * <p>The refusals are the replay's own, sentence for sentence: an {@link IrLoader.Refusal} for a form the loader
 * does not carry, an {@link IrLoader.Documentfout} for a document that is not well-formed, and a
 * {@link CaseRunner.Excluded} for a form the backend does not carry past the loader — among them every construct
 * the chosen branch does not lower, so no KieBase is handed out that silently lacks a rule.
 *
 * <p>Firing stays with the consumer, as the replay does it: a fresh {@link org.kie.api.runtime.KieSession} per
 * case, the fact beans instantiated from {@link Build#beanLoader()} while that loader is the thread's context
 * class loader, the parameters and the evaluation date as globals, and on the DRL-text branch also
 * {@link Build#dagsoortGlobalen()}; a {@code ConsequenceException} out of the fire is named by {@link #benoem}, and a
 * fire that reached its fixpoint is followed by {@link #zetAfgeleideWaarden}, which on the truth-maintenance branch
 * throws the evaluation faults that stand at the fixpoint and otherwise puts what that branch derived on the beans, and
 * does nothing on the other two.
 *
 * <p>A truth-maintenance build with a creation fires in generations, which a plain {@code fireAllRules} does not do:
 * its creation rules sit in agenda groups of their own and would never fire. The replay fires through {@link #vuur},
 * which is not public: how a consumer calls it is for the card that switches the toolchain over
 * ({@code tms-omschakeling}). Until then {@link #zetAfgeleideWaarden} refuses a session of such a build that was fired
 * without it, rather than hand out an answer without the creations.
 */
public final class DroolsBackend {

    private static final ObjectMapper JSON = new ObjectMapper();

    private DroolsBackend() {}

    /** Which transpiler lowers the document. The exec-model branch is the one gate 12's square replays; the
     *  DRL-text branch is narrower — no ch. 7 defaults lowering, more markers — and the gate counts it apart. The
     *  truth-maintenance branch is a trial ({@code TmsTranspiler}): it derives with logical inserts instead of writing
     *  the beans, carries a narrow set of forms, and is in no gate. */
    public enum Branch { EXEC_MODEL, DRL_TEXT, TRUTH_MAINTENANCE }

    /** A built document. Exactly one transpiler is set, the one {@link #branch()} names; {@code dekking} is its
     *  coverage manifest, every entry lowered, because the build refuses a document with any other entry. */
    public record Build(RegelSpraakModel model, ClassLoader beanLoader, ExecModelTranspiler execModelTranspiler,
                        DrlTranspiler drlTranspiler, TmsTranspiler tmsTranspiler, List<Dekking> dekking,
                        KieBase kieBase) {
        public Build {
            int gezet = (execModelTranspiler == null ? 0 : 1) + (drlTranspiler == null ? 0 : 1)
                    + (tmsTranspiler == null ? 0 : 1);
            if (gezet != 1)
                throw new IllegalArgumentException("a Build carries exactly one transpiler, got "
                        + (gezet == 0 ? "none" : gezet));
        }

        public Branch branch() {
            return execModelTranspiler != null ? Branch.EXEC_MODEL
                    : drlTranspiler != null ? Branch.DRL_TEXT : Branch.TRUTH_MAINTENANCE;
        }

        /** The §9.8 day-kind predicates the DRL text declares as globals (name -> predicate); a session must have
         *  each set before it fires. Empty on the exec-model branch, which wires them into its lambdas. */
        public Map<String, Predicate<LocalDate>> dagsoortGlobalen() {
            return drlTranspiler == null ? Map.of() : drlTranspiler.dagsoortGlobalen();
        }
    }

    /** {@link #build(JsonNode, Branch)} on the document read from {@code irPath}. */
    public static Build build(Path irPath, Branch branch) throws IOException {
        return build(JSON.readTree(Files.readString(irPath)), branch);
    }

    /** Load {@code document}, refuse the forms the backend does not carry, generate and contract-check the fact
     *  beans, lower every construct through {@code branch}'s transpiler with every side table the loader returns,
     *  and build the {@link KieBase}.
     *
     *  @throws IrLoader.Refusal      a form the loader does not carry; the replay reports it as {@code loader: …}
     *  @throws IrLoader.Documentfout a document that is not well-formed {@code uchar-ir-v1}
     *  @throws CaseRunner.Excluded   a form the backend does not carry past the loader: two declarations on one
     *      Java name of the bean model, a reciprocal self-relation the bean model cannot hold, a bean-contract
     *      violation, a construct the branch does not lower, on the DRL-text branch a prioritized-defaults
     *      extension or a DRL text the Drools compiler rejects, and on the truth-maintenance branch a cycle of the
     *      dependency graph that WF-045 or WF-046 forbids */
    public static Build build(JsonNode document, Branch branch) {
        Objects.requireNonNull(branch, "branch");
        IrLoader.Loaded loaded = IrLoader.loadAll(document);   // the model plus the ch. 7 prioritized-defaults extension beside it
        RegelSpraakModel model = loaded.model();
        weigerBeannaambotsing(loaded);

        // A form the v0 backend cannot REPLAY is refused up front — a LOUD per-form exclusion, never a
        // silent value-divergence in the gate. Carrying it to fire only to fault or misvalue there is
        // honestly NOT-CARRIED, not a semantic exec-model difference, so it belongs in the auto-excluded
        // channel with a reason, not the value-divergence list whose banner claims a genuine ref/clingo delta.
        // (§13.4.6 objectcreatie was refused here until 2026-08-30; the creation consequence now executes —
        // both back-link ends written, the kenmerkinitialisatie honoured — so the shapes it still cannot carry
        // are refused per creation by the transpiler, which knows WHICH one, not blanket for every model.)
        //   §13.3.9.2 name-sharing bean limit — leden() ports every member through a name-dedup (attribute >
        //     kenmerk > reference), so a declared member sharing the reciprocal role's name displaces the
        //     self-pair reference field; the mirror pass would then read a Boolean or set an impossible type,
        //     an unclassified ERROR at best. Refuse the shape loudly instead (lens finding, 2026-08-30).
        for (Objecttype ot : model.objecttypen.values()) {
            if (!FactModelGenerator.noemtWederkerig(model, ot)) continue;
            String zelf = FactModelGenerator.zelfpaarRol(model, ot);
            //   the UNDERIVABLE half of the same shape — an ambiguous pair (two wederkerig feittypen on one
            //   objecttype) or a to-many reciprocal role: the transpilers navigate by the written role name
            //   toward a getter no bean carries, an unclassified fire-time ERROR (lens 2, 2026-08-30).
            if (zelf == null)
                throw new Excluded("wederkerig self-relation (§13.3.9.2) on " + ot.naam + " derives no scalar "
                        + "reference field (an ambiguous or to-many self-pair role) — not carried by the v0 backend");
            if (!FactModelGenerator.heeftReferentieVeld(model, ot, zelf, ot))
                throw new Excluded("wederkerig self-pair role '" + zelf + "' on " + ot.naam + " shares its name "
                        + "with a declared member — the bean cannot carry the self-typed reference field "
                        + "(name-sharing bean limit)");
        }

        Map<String, String> bronnen = FactModelGenerator.genereer(model);
        ClassLoader cl = BeanCompiler.compileer(bronnen);
        List<String> schendingen = ContractValidator.valideer(model, cl);
        if (!schendingen.isEmpty()) throw new Excluded("contract: " + schendingen.get(0));

        ClassLoader vorige = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(cl);
        try {
            return switch (branch) {
                case EXEC_MODEL -> execModelTak(loaded, cl);
                case DRL_TEXT -> drlTak(loaded, cl);
                case TRUTH_MAINTENANCE -> tmsTak(loaded, cl);
            };
        } finally {
            Thread.currentThread().setContextClassLoader(vorige);
        }
    }

    /** Two declarations on one Java name of the bean model, refused before a bean is generated, in the sentences of
     *  {@link FactModelGenerator}: an object type whose fact class another type, a class the generator writes itself
     *  or a {@code java.lang} type already names; two roles on one reference field of one type; and a member whose
     *  bean property belongs to another declaration, where a form of the rule layer reads or writes it (the forms of
     *  {@link ProducentConsument}, and a §9.9 start-point determination, which names its anchor member). Such a member
     *  that nothing uses is left alone: its field is simply absent, and a case that names it is refused by the case
     *  reader. Here and not in the loader, because the rule graph and every other reading of the loaded model keep
     *  the two names apart; only the bean model does not. */
    private static void weigerBeannaambotsing(IrLoader.Loaded loaded) {
        RegelSpraakModel model = loaded.model();
        String klasse = FactModelGenerator.klassebotsing(model);
        if (klasse != null) throw new Excluded(klasse);
        List<FactModelGenerator.Verdringing> verdringingen = FactModelGenerator.verdringingen(model);
        if (verdringingen.isEmpty()) return;
        String rol = FactModelGenerator.rolbotsing(verdringingen);
        if (rol != null) throw new Excluded(rol);
        Map<ProducentConsument.ElementSleutel, String> gebruik = new java.util.HashMap<>();
        for (ProducentConsument.Vorm v : ProducentConsument.vormen(model, loaded.prioritizedDefaults(),
                ProducentConsument.wireNavigatie(loaded))) {
            String wie = v.soort().name().toLowerCase(java.util.Locale.ROOT) + " '" + v.naam() + "'";
            for (ProducentConsument.ElementSleutel e : v.schrijft()) gebruik.putIfAbsent(e, wie);
            for (ProducentConsument.ElementSleutel e : v.leest()) gebruik.putIfAbsent(e, wie);
        }
        for (ResolvedStartpunt sp : model.resolvedStartpunten)
            gebruik.putIfAbsent(ProducentConsument.elementVan(model.objecttypeOpNaam(sp.objecttype()), sp.lid(), false),
                    "de startpuntbepaling van tijdlijn '" + sp.tijdlijn() + "'");
        for (FactModelGenerator.Verdringing v : verdringingen) {
            FactModelGenerator.Herkomst h = v.verdrongen();
            // a role without a field of its own is refused where it is used, by the gate that asks the field's name
            // and type (FactModelGenerator.heeftReferentieVeld); rolbotsing above took the one shape that gate passes
            if (h.soort() == FactModelGenerator.Herkomst.Soort.ROL) continue;
            String wie = gebruik.get(ProducentConsument.ElementSleutel.van(h.bron(), h.naam(),
                    h.soort() == FactModelGenerator.Herkomst.Soort.KENMERK));
            if (wie != null) throw new Excluded(FactModelGenerator.verdringingReden(v) + "; " + wie + " leest of schrijft het");
        }
    }

    /** The exec-model branch: {@link ExecModelTranspiler} lowers the model to an executable model, which
     *  {@link KieBaseBuilder} turns into the KieBase. */
    private static Build execModelTak(IrLoader.Loaded loaded, ClassLoader cl) {
        ExecModelTranspiler et = ExecModelTranspiler.build(loaded.model(), cl, loaded.prioritizedDefaults(),
                loaded.rolBindingen(), loaded.lets(), loaded.bronPaden(), loaded.navigatieBindingen());
        // every construct must LOWER — a marker means the exec KieBase silently omits it,
        // so no answer it gives could be trusted; that document is not carried.
        for (Dekking d : et.dekking())
            if (!d.lowered()) throw new Excluded("transpiler " + d.verdict() + ": " + d.sleutel()
                    + (d.reden().isEmpty() ? "" : " — " + d.reden()));
        weigerDubbeleRegelnaam(et.model(), "exec-model");
        KieBase kieBase = KieBaseBuilder.createKieBaseFromModel(et.model());
        return new Build(loaded.model(), cl, et, null, null, List.copyOf(et.dekking()), kieBase);
    }

    /** Two rules of one name in one package: the KieBase keeps one rule per name, so the second would replace the first
     *  and the document would play without it (measured 2026-10-02 on two forms of one name, which WF-020 forbids and a
     *  hand-written document can still carry). The DRL-text branch's compiler refuses the same document with
     *  "Duplicate rule name"; the exec-model and the truth-maintenance branch refuse it in those words, after their
     *  {@code tak}, before the KieBase is built. A rule without a name (a nameless form, WF-107) has no name to share
     *  and is not counted: the exec-model branch plays two nameless creations ({@code TakgelijkheidWitnessTest}). */
    private static void weigerDubbeleRegelnaam(org.drools.model.Model model, String tak) {
        java.util.Set<String> namen = new java.util.HashSet<>();
        for (org.drools.model.Rule r : model.getRules())
            if (r.getName() != null && !namen.add(r.getFullName()))
                throw new Excluded(tak + ": Duplicate rule name: " + r.getName() + " — twee regels van één naam in "
                        + "pakket " + r.getPackage() + "; de KieBase houdt één regel per naam, zodat de tweede de eerste "
                        + "zou vervangen (vormnamen zijn uniek, WF-020)");
    }

    /** The truth-maintenance branch: {@link TmsTranspiler} lowers the forms it carries to an executable model that
     *  derives with logical inserts. The KieBase runs in EQUALITY mode, as the branch's design requires: there a fact
     *  inserted as stated takes over an equal logical fact, where IDENTITY mode keeps the two as two facts on one key
     *  and ignores a delete of the stated one in some states. Equal logical facts merge into one fact with a
     *  justification per rule in either mode; that merge rests on the equals and hashCode of the branch's fact classes.
     *  A form the transpiler does not lower is refused from its manifest, as on the exec-model branch; a cycle of the
     *  dependency graph that WF-045 or WF-046 forbids is refused before the KieBase is built, since truth maintenance
     *  gives such a cycle no single fixpoint ({@code Kringen}). */
    private static Build tmsTak(IrLoader.Loaded loaded, ClassLoader cl) {
        TmsTranspiler t = TmsTranspiler.build(loaded.model(), cl, loaded.prioritizedDefaults(), loaded.rolBindingen(),
                loaded.lets(), loaded.bronPaden(), loaded.navigatieBindingen());
        for (Dekking d : t.dekking())
            if (!d.lowered()) throw new Excluded("tms-transpiler " + d.verdict() + ": " + d.sleutel()
                    + (d.reden().isEmpty() ? "" : " — " + d.reden()));
        // past the manifest every form of the document is one this branch lowers, so the graph over all forms is the
        // graph over the forms it carries
        String kring = Kringen.weigering(loaded);
        if (kring != null) throw new Excluded(kring);
        weigerDubbeleRegelnaam(t.model(), "tms");
        KieBase kieBase = KieBaseBuilder.createKieBaseFromModel(t.model(), EqualityBehaviorOption.EQUALITY);
        return new Build(loaded.model(), cl, null, null, t, List.copyOf(t.dekking()), kieBase);
    }

    /**
     * Puts what the truth-maintenance branch derived in {@code sessie} on the fact beans, after a fire that reached its
     * fixpoint: that branch never writes a bean while it fires, so until this call the beans hold the case input only.
     * On the exec-model and the DRL-text branch it does nothing, since their rules write the beans as they fire. A
     * consumer that fires a KieBase itself calls it as {@link CaseRunner} does, after every fire that ends at a
     * fixpoint and before it reads the beans.
     *
     * <p>The write-back tells the engine nothing, so the session must not be fired again afterwards: the beans then hold
     * values the network never saw arrive, and a rule that reads the bean pattern would be evaluated against a state
     * that no propagation produced.
     *
     * <p>On the truth-maintenance branch an evaluation fault, a division by a numeric zero (RUNTIME-SEMANTICS ch. 1.4,
     * point 4), is a fact the fire inserts logically, not an exception out of the fire, so that a fault on an
     * intermediate state goes again when that state passes. Before anything is written, this throws the faults that
     * stand at the fixpoint: one as the {@link Breuk.DelingDoorNul} the exec-model branch's fire gives through
     * {@link #benoem}, naming the rule and the operands, and two or more as {@code Evaluatiefouten}, in an order that does
     * not depend on the order of the rules ({@code TmsTranspiler.zetAfgeleideWaarden}).
     */
    public static void zetAfgeleideWaarden(Build build, KieSession sessie) {
        Objects.requireNonNull(build, "build");
        Objects.requireNonNull(sessie, "sessie");
        if (build.tmsTranspiler() != null) build.tmsTranspiler().zetAfgeleideWaarden(sessie);
    }

    /** The generation budget of a run with creations, per evaluation stratum: at most this many generations of one
     *  stratum change something, and the next must change nothing, a generation being a pass of the reference
     *  interpreter ({@link #vuur}). The reference interpreter bounds the fixpoint of each stratum by 100 passes that
     *  change something, a recursive chain advancing one generation per pass, and the ASP backend the depth of a chain
     *  of mints by an equal {@code oc_budget} (RUNTIME-SEMANTICS ch. 5.5 and 5.7); {@code corpus/rood}
     *  {@code finite-chain-over-budget} and {@code synth2-budgetgrens-onder} pin the two sides of the bound. */
    static final int GENERATIES = 100;

    /** What {@link #vuur} did: the firings of all its fires together, and the sentence of a run that ends without a
     *  fixpoint at the generation budget, null otherwise. A run that reaches the fire cap has no fixpoint either; the
     *  caller reads that off {@code vuringen}. */
    record Vuring(int vuringen, String geenFixpunt) {}

    /**
     * Fires {@code sessie} of {@code build} to its fixpoint, at most {@code plafond} firings in all. On the exec-model
     * and the DRL-text branch, and on the truth-maintenance branch without a lowered creation, that is one fire.
     *
     * <p>On the truth-maintenance branch with a creation a creation fires only at a fixpoint of the rule layer, and
     * what it makes is stated and never lapses, as in the reference interpreter (RUNTIME-SEMANTICS ch. 5.5): a logical
     * object could keep itself standing through its own output after the state that made it passed, and its
     * initialisations would follow later changes, where ch. 5.5 evaluates them when the object is made. So this fires
     * in generations, here and not in the rules, so that a second rendering of the branch can share it. The creation
     * rules sit in agenda groups of their own ({@code TmsTranspiler.FEITCREATIE}, {@code TmsTranspiler.OBJECTCREATIE}),
     * so a fire without their focus takes the rule layer to rest and leaves them pending. Then per generation a phase of
     * fact creations and a phase of object creations, in the order of a pass of the reference interpreter: each opens a
     * new {@code Creatiefase}, gives its group the focus and fires, which fires the creations that hold at that fixpoint
     * and then, once the group is empty, takes the rule layer to rest again in the same fire. What a phase made carries
     * its number, and a creation rule does not take an object or a link of the phase it fires in, so a recursive chain
     * advances one generation per phase.
     *
     * <p>The generations run per evaluation stratum of the reference interpreter, in ascending order
     * ({@code TmsTranspiler.creatiestrata}): the phases of stratum k carry k, and a creation rule fires only in a phase
     * of its own stratum or one above. A creation writes irrevocably, so it reads strictly and sits above the producers
     * of what it reads (RUNTIME-SEMANTICS ch. 6.1); the reference interpreter evaluates stratum k to its fixpoint before
     * a form of stratum k+1 fires (ch. 6.3). In one phase for all strata, a creation that reads what another creation
     * of the same generation makes would read the state before it, and its initialisation would stay fixed on that
     * value. The strata order only the creations, which are stated: the rule layer stays under truth maintenance and
     * reaches its fixpoint after every phase.
     *
     * <p>The budget counts as the reference interpreter counts its passes, per stratum: every pass that changes
     * something, until the first that changes nothing. A generation is such a pass: the change of the rule layer after
     * the creations of the generation before, which the reference interpreter's next pass makes before its creations,
     * and the creations of this generation. A change of the rule layer is net, a value, a characteristic or a link that
     * stands differently after the generation than before it, the facts the creations state left out, as they are the
     * creations' own change; a standing or a contribution is no change, as an object without a written value is none
     * for the reference interpreter. It counts in this stratum only where a form of this stratum or below writes the
     * member ({@code TmsTranspiler.lidstratum}): the reference interpreter evaluates a form of a higher stratum only
     * after this stratum's fixpoint. So a stratum whose last creation still makes the rule layer change has one pass
     * more than generations that create. A stratum ends at its first generation that changes nothing; a generation past
     * {@link #GENERATIES} that still changes something ends the run without a fixpoint.
     *
     * <p>The reference interpreter also counts a first pass of a stratum in which only rules change: the rules of that
     * stratum, which no earlier stratum evaluated. Here the rule layer made those changes while an earlier stratum's
     * phases fired, and the first generation counts by its creations alone. That cannot change the outcome: in a pass
     * of the reference interpreter the rules run before the creations, so its first pass of a stratum makes the
     * creations of the first generation, and a first pass in which only rules change is followed by one that changes
     * nothing.
     */
    static Vuring vuur(Build build, KieSession sessie, int plafond) {
        TmsTranspiler tms = build.tmsTranspiler();
        Set<String> creaties = tms == null ? Set.of() : tms.creatieregels();
        if (creaties.isEmpty()) return new Vuring(sessie.fireAllRules(plafond), null);
        int[] gemaakt = {0};
        AgendaEventListener teller = new DefaultAgendaEventListener() {
            @Override public void afterMatchFired(AfterMatchFiredEvent e) {
                if (creaties.contains(e.getMatch().getRule().getName())) gemaakt[0]++;
            }
        };
        // what a creation inserts is stated and is the creation's own change, never one of the rule layer
        Set<Object> gesteld = Collections.newSetFromMap(new IdentityHashMap<>());
        RuleRuntimeEventListener stellingen = new DefaultRuleRuntimeEventListener() {
            @Override public void objectInserted(ObjectInsertedEvent e) {
                if (e.getRule() != null && creaties.contains(e.getRule().getName())) gesteld.add(e.getObject());
            }
        };
        sessie.addEventListener(teller);
        sessie.addEventListener(stellingen);
        try {
            int gevuurd = sessie.fireAllRules(plafond);
            FactHandle fase = null;
            int nummer = 0;
            for (int stratum : tms.creatiestrata()) {
                // the net change of the rule layer after the creations of the previous generation, of this stratum
                int reactie = 0;
                Map<Plaats, Set<Object>> stand = stand(sessie, gesteld);
                for (int generatie = 1; gevuurd < plafond; generatie++) {
                    gemaakt[0] = 0;
                    for (String groep : List.of(TmsTranspiler.FEITCREATIE, TmsTranspiler.OBJECTCREATIE)) {
                        // Drools makes a group on demand only in a KieBase that declares a group beside MAIN, which
                        // every creation rule does; null means the KieBase lacks the creation rules the transpiler reports
                        org.kie.api.runtime.rule.AgendaGroup g = sessie.getAgenda().getAgendaGroup(groep);
                        if (g == null)
                            throw new IllegalStateException("de KieBase kent de agendagroep " + groep + " niet, terwijl"
                                    + " het model creatieregels heeft: " + creaties);
                        if (fase != null) sessie.delete(fase);
                        fase = sessie.insert(new Creatiefase(++nummer, stratum));
                        g.setFocus();
                        gevuurd += sessie.fireAllRules(plafond - gevuurd);
                        if (gevuurd >= plafond) return new Vuring(gevuurd, null);
                    }
                    if (generatie > GENERATIES && gemaakt[0] > 0)
                        return new Vuring(gevuurd, "geen fixpunt binnen " + GENERATIES + " generaties: generatie "
                                + generatie + " maakt nog " + gemaakt[0] + (gemaakt[0] == 1 ? " object of link"
                                : " objecten of links") + " — een creatie die niet stopt is een fout (RUNTIME-SEMANTICS"
                                + " ch. 5.5 en 5.7)");
                    if (generatie > GENERATIES && reactie > 0)
                        return new Vuring(gevuurd, "geen fixpunt binnen " + GENERATIES + " generaties: generatie "
                                + generatie + " maakt niets meer, maar de regellaag verandert nog " + reactie
                                + (reactie == 1 ? " waarde, kenmerk of link" : " waarden, kenmerken of links")
                                + " na wat generatie " + (generatie - 1) + " maakte — ook die pas telt, zoals in de"
                                + " referentie-interpreter (RUNTIME-SEMANTICS ch. 5.5 en 5.7)");
                    if (gemaakt[0] == 0) break;
                    Map<Plaats, Set<Object>> na = stand(sessie, gesteld);
                    reactie = reactie(stand, na, tms, stratum);
                    stand = na;
                }
            }
            return new Vuring(gevuurd, null);
        } finally {
            sessie.removeEventListener(teller);
            sessie.removeEventListener(stellingen);
        }
    }

    /** One place of the state a pass of the reference interpreter changes: an attribute ({@code 'w'}) or a
     *  characteristic ({@code 'k'}) of an object, or a link of a relation ({@code 'l'}), the objects by identity. */
    private record Plaats(char soort, Id onderwerp, Object lid, Id tweede) {}

    /** An object compared by identity, as the branch's facts compare their subject. */
    private record Id(Object o) {
        @Override public boolean equals(Object x) { return x instanceof Id i && i.o == o; }
        @Override public int hashCode() { return System.identityHashCode(o); }
    }

    /** What stands in {@code sessie} per place: the values of an attribute, a characteristic, a link; without the facts
     *  a creation stated ({@code gesteld}). A number counts by its value, so 5.0 and 5.00 are one value. */
    private static Map<Plaats, Set<Object>> stand(KieSession sessie, Set<Object> gesteld) {
        Map<Plaats, Set<Object>> uit = new HashMap<>();
        for (Object f : sessie.getObjects(o -> o instanceof Waarde || o instanceof HeeftKenmerk || o instanceof Koppeling)) {
            if (gesteld.contains(f)) continue;
            if (f instanceof Waarde w)
                uit.computeIfAbsent(new Plaats('w', new Id(w.onderwerp()), w.attribuut(), null), p -> new HashSet<>())
                        .add(w.waarde() instanceof BigDecimal b ? b.stripTrailingZeros() : w.waarde());
            else if (f instanceof HeeftKenmerk h)
                uit.put(new Plaats('k', new Id(h.onderwerp()), h.kenmerk(), null), Set.of(Boolean.TRUE));
            else if (f instanceof Koppeling k)
                uit.put(new Plaats('l', new Id(k.eerste()), k.relatie(), new Id(k.tweede())), Set.of(Boolean.TRUE));
        }
        return uit;
    }

    /** The places that stand differently in {@code na} than in {@code voor} and that a form of {@code stratum} or below
     *  writes ({@link TmsTranspiler#lidstratum}). A link counts in every stratum, though the rule layer makes none: a
     *  creation states the links it makes, and the links of the input do not change while the session fires. */
    private static int reactie(Map<Plaats, Set<Object>> voor, Map<Plaats, Set<Object>> na, TmsTranspiler tms,
                               int stratum) {
        Set<Plaats> plaatsen = new HashSet<>(voor.keySet());
        plaatsen.addAll(na.keySet());
        int n = 0;
        for (Plaats p : plaatsen)
            if (!Objects.equals(voor.get(p), na.get(p)) && (p.soort() == 'l'
                    || tms.lidstratum(p.onderwerp().o(), (ElementSleutel) p.lid()) <= stratum)) n++;
        return n;
    }


    /** The DRL-TEXT branch: {@link DrlTranspiler} emits DRL, which is compiled to a {@link KieBase} by the classic
     *  DRL build ({@code KieHelper}, mvel dialect). Three forms are refused here, each loudly and with the reason,
     *  because the text branch is NARROWER than the exec-model one:
     *  <ul>
     *    <li>a ch. 7 prioritizedDefaults extension — that lowering exists only in the exec branch, so a DRL
     *        KieBase would silently omit the default and answer with an unwritten attribute;</li>
     *    <li>any construct the text branch markers (its {@code TODO} discipline) — the DRL still compiles,
     *        which is exactly why the marker must be refused rather than fired;</li>
     *    <li>a DRL the Drools compiler rejects — a text-branch limit, not a semantic divergence.</li>
     *  </ul> */
    private static Build drlTak(IrLoader.Loaded loaded, ClassLoader cl) {
        if (!loaded.prioritizedDefaults().isEmpty())
            throw new Excluded("drl: ch. 7 prioritizedDefaults — the DRL-text branch has no defaults lowering");
        // ch. 4.10: the DRL branch lowers the §11 variabelendeel's Let chain, so it gets the document's Let INDEX
        // beside the model — the ch. 2.10 unit derivation needs it to answer a BinderRef read, which carries no
        // route back up to its own binding (the exec branch has taken the same index since the Let-node card).
        // The defaults list is empty by the refusal above; it is passed rather than defaulted because the
        // ch. 7.8 standing classification's other_writer clause reads it (see DrlTranspiler.build).
        // ch. 4.3: and the symbolic role bindings, for the same reason the exec branch has taken them since
        // 2026-08-30 — a fold whose argument is an ATTRIBUTE name has no written role to resolve its range
        // from, so on a repeated or self-typed pair the wire's sourceRole is the only route there is. Withheld,
        // this branch markered exactly the folds its sister played.
        DrlTranspiler t = DrlTranspiler.build(loaded.model(), loaded.lets(), loaded.prioritizedDefaults(),
                loaded.bronPaden(), loaded.rolBindingen(), loaded.navigatieBindingen());
        for (Dekking d : t.dekking())
            if (!d.lowered()) throw new Excluded("drl-transpiler " + d.verdict() + ": " + d.sleutel()
                    + (d.reden().isEmpty() ? "" : " — " + d.reden()));
        KieHelper helper = new KieHelper();
        helper.setClassLoader(cl);
        // Without a name KieHelper files the text as file0.drl in the resource root, and KieBuilder warns that a
        // file in folder '' declares DRL_PACKAGE. The path's folders follow the package; the extension gives the type.
        helper.addContent(t.drl(), DrlTranspiler.DRL_PACKAGE.replace('.', '/') + "/regels.drl");
        KieBase kieBase;
        try {
            kieBase = helper.build();
        } catch (RuntimeException e) {
            // Drools' message opens with a header that carries the path above, 127 characters up to "text=" on the
            // first message, and only then the error itself (line, column, what failed). The bound has to leave that
            // text standing; at 200, 73 characters of it remained.
            String m = String.valueOf(e.getMessage()).replace('\n', ' ');
            throw new Excluded("drl compile: " + (m.length() > 400 ? m.substring(0, 400) + "…" : m));
        }
        return new Build(loaded.model(), cl, null, t, null, List.copyOf(t.dekking()), kieBase);
    }

    /** Names the fault behind {@code e}, the exception Drools' default handler throws out of the fire when a consequence
     *  throws: a division by a numeric zero in a consequence (a rule's value, a default's justification or consequence)
     *  becomes {@link Breuk.DelingDoorNul} naming the rule and the operands, with what the consequence threw as its
     *  cause; any other {@code e} is returned unchanged. {@link CaseRunner} calls it where it fires, so the replay, the
     *  red path and the scenario report read the sentence; a consumer that fires a KieBase itself gets Drools'
     *  {@code ConsequenceException}, which names the rule, and can call this the same way. A division in a condition
     *  is evaluated outside a consequence: see {@link #benoem(ConstraintEvaluationException)}.
     *
     *  <p>The KieBases built here keep Drools' default consequence handler on purpose. Drools takes a handler by class
     *  name and instantiates it in every new session; where the kie API and this backend are loaded by different
     *  class loaders (the distribution's Quarkus test of its decision service) that instance does not cast to the kie
     *  interface, and every {@code newKieSession} there threw a {@code ClassCastException} (2026-10-02). */
    public static RuntimeException benoem(ConsequenceException e) {
        for (Throwable t = e.getCause(); t != null; t = t.getCause() == t ? null : t.getCause())
            if (t instanceof Breuk.DelingDoorNul nul) return nul.inDeRegel(e.getRule().getName(), e.getCause());
        return e;
    }

    /** Names the fault behind {@code e}, the exception Drools throws out of the fire when a constraint throws, on the
     *  exec-model branch: a condition is tested in a constraint and not in a consequence. Its predicates carry their
     *  rule (the transpiler's {@code BenoemdeToets}), so Drools' message names the rule and the condition, and a
     *  division by a numeric zero in it is in the cause chain already named after the rule, with the operands: that
     *  {@link Breuk.DelingDoorNul} is returned, with the division as {@link Breuk#delen} threw it as its cause. Any
     *  other {@code e} is returned unchanged. The DRL-text branch throws Drools' own MVEL exception, which names the
     *  condition and the rule, and does not come here; for the subselection of a fold that divides, which that branch
     *  tests on the element bound again in the source of the accumulate, Drools names no rule. */
    public static RuntimeException benoem(ConstraintEvaluationException e) {
        for (Throwable t = e.getCause(); t != null; t = t.getCause() == t ? null : t.getCause())
            if (t instanceof Breuk.DelingDoorNul nul && nul.regel() != null) return nul;
        return e;
    }
}
