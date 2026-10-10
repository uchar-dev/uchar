package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import dev.uchar.regelspraak.cases.CaseRunner.Excluded;
import dev.uchar.regelspraak.engine.BeanCompiler;
import dev.uchar.regelspraak.graaf.ProducentConsument;
import dev.uchar.regelspraak.ir.ResolvedStartpunt;
import dev.uchar.regelspraak.loader.IrLoader;
import dev.uchar.regelspraak.model.RegelSpraakModel;
import dev.uchar.regelspraak.model.RegelSpraakModel.Objecttype;
import dev.uchar.regelspraak.schema.ContractValidator;
import dev.uchar.regelspraak.schema.FactModelGenerator;
import dev.uchar.regelspraak.transpiler.DrlTranspiler;
import dev.uchar.regelspraak.transpiler.kern.Dekking;
import dev.uchar.regelspraak.transpiler.kern.Namen;
import dev.uchar.regelspraak.transpiler.model.ExecModelTranspiler;
import dev.uchar.regelspraak.transpiler.tms.Creatiebudget;
import dev.uchar.regelspraak.transpiler.tms.Creatiefase;
import dev.uchar.regelspraak.transpiler.tms.Emmerinvoer;
import dev.uchar.regelspraak.transpiler.tms.Fixpunt;
import dev.uchar.regelspraak.transpiler.tms.Gevuurd;
import dev.uchar.regelspraak.transpiler.tms.Kringen;
import dev.uchar.regelspraak.transpiler.tms.Lijstplaats;
import dev.uchar.regelspraak.transpiler.tms.Teruggezet;
import dev.uchar.regelspraak.transpiler.tms.TmsTranspiler;

import dev.uchar.regelspraak.runtime.Accessors;
import dev.uchar.regelspraak.runtime.Breuk;

import org.drools.modelcompiler.KieBaseBuilder;
import org.drools.modelcompiler.constraints.ConstraintEvaluationException;
import org.kie.api.KieBase;
import org.kie.api.conf.EqualityBehaviorOption;
import org.kie.api.event.rule.AfterMatchFiredEvent;
import org.kie.api.event.rule.AgendaEventListener;
import org.kie.api.event.rule.DefaultAgendaEventListener;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.rule.ConsequenceException;
import org.kie.api.runtime.rule.FactHandle;
import org.kie.internal.utils.KieHelper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
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
 * <p>A session of a build goes through four calls, as the replay makes them ({@link CaseRunner}): a fresh
 * {@link org.kie.api.runtime.KieSession} of {@link Build#kieBase()}, with {@link Build#beanLoader()} as the thread's
 * context class loader, the parameters and the evaluation date ({@code rekendatum}) as globals and on the DRL-text
 * branch also {@link Build#dagsoortGlobalen()}; then {@link #voegIn}, which puts in the fact beans and, on the
 * truth-maintenance branch, what that branch reads beside them; then {@link #vuur}, which fires to the fixpoint under a
 * cap and, for a model with a creation, in generations; then {@link #lees} or {@link #zetAfgeleideWaarden}. On the
 * truth-maintenance branch the beans hold the input alone until the write-back, which comes once, at the end of the
 * session, and closes it: a consumer that fires one session more than once, changing input in between with
 * {@code update}, reads every answer but the last with {@link #lees}. A session of a model with a creation fires once,
 * since what a creation made is stated and does not lapse: such a consumer opens a fresh session per input. On the
 * other two branches the rules write the
 * beans as they fire, the write-back does nothing and {@link #lees} refuses. A {@code ConsequenceException} out of the
 * fire is named by {@link #benoem}.
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
     * Puts {@code invoer} into {@code sessie}, a fresh session of {@code build}: the fact beans, and on the
     * truth-maintenance branch what that branch reads beside them and the bean model does not hold, as that branch's own
     * facts, so that a consumer names it in the words of the model and states none of them. That is an
     * {@code Emmerinvoer} for every bucket scalar {@code invoer} names ({@link Invoer#emmerscalair}), without which the
     * branch reads an input timeline as a full timeline (RUNTIME-SEMANTICS ch. 8.4); and where a rule of the model reads
     * the order of a to-many list ({@code TmsTranspiler.lijstvelden}), a {@code Lijstplaats} for every member of every
     * such list, in the order {@link Invoer#lijst} gives or else in the order of the beans. On the exec-model and the
     * DRL-text branch it inserts the beans alone, since those branches read nothing else.
     *
     * <p>On the truth-maintenance branch it checks all of it before it inserts anything, and refuses by name: a session
     * that already holds a fact, since the places of a list are numbered over the beans of one input and a bean
     * inserted without this call has none; a bean that is not of the build's bean model or that comes twice; a reference
     * field of a bean that holds an object outside the input, named with the bean, the field and the object's type, since
     * the branch derives nothing for an object outside the session and every value read through the field would stay
     * empty without a word; a bucket
     * scalar on a bean outside the input or on a member its type does not declare as an attribute on a timeline; and a
     * list that names a bean outside the input, a member whose reference field {@code rol} does not hold the holder, a
     * member twice, or not every bean of the input whose field of that relation holds the holder, or that comes twice. A
     * place the rules need is thus never missing at the fire, where it would stop the run with an
     * {@code IllegalStateException}.
     *
     * @throws IllegalStateException    {@code sessie} is not fresh, on the truth-maintenance branch
     * @throws IllegalArgumentException a statement of {@code invoer} that does not fit its beans or the model, on the
     *                                  truth-maintenance branch
     */
    public static void voegIn(Build build, KieSession sessie, Invoer invoer) {
        Objects.requireNonNull(build, "build");
        Objects.requireNonNull(sessie, "sessie");
        Objects.requireNonNull(invoer, "invoer");
        List<Object> bonen = invoer.bonen();
        TmsTranspiler tms = build.tmsTranspiler();
        if (tms == null) {
            for (Object boon : bonen) sessie.insert(boon);
            return;
        }
        if (sessie.getFactCount() != 0)
            throw new IllegalStateException("voegIn vult een verse sessie, maar deze houdt al " + sessie.getFactCount()
                    + " feiten: de plaatsen van een lijst worden over de bonen van één invoer geteld, en een boon die"
                    + " zonder voegIn in de sessie kwam, heeft er geen; geef alle bonen in één Invoer aan een verse"
                    + " sessie, en verander tussen twee fires hun waarden met update");
        Map<Object, Objecttype> typen = new IdentityHashMap<>();
        // a case of ten thousand objects has a handful of bean classes
        Map<Class<?>, Objecttype> perKlasse = new HashMap<>();
        for (Object boon : bonen)
            if (typen.put(boon, perKlasse.computeIfAbsent(boon.getClass(), k -> objecttype(build, boon))) != null)
                throw new IllegalArgumentException("voegIn: de boon " + naam(boon) + " staat twee keer in de invoer");
        weigerDoelBuitenDeInvoer(build, bonen, typen, perKlasse);
        List<Object> feiten = new ArrayList<>(bonen);
        for (Invoer.Emmer e : invoer.emmers()) feiten.add(emmerinvoer(typen, e));
        feiten.addAll(lijstplaatsen(build, tms.lijstvelden(), bonen, typen, invoer.lijsten()));
        for (Object feit : feiten) sessie.insert(feit);
    }

    /** Refuses a reference field of a bean of the input that holds an object outside the input. The branch derives only
     *  for the objects in the session, so every value of such an object, and every value read through the field, would
     *  stay empty without a word; and the link the field gives still counts, so a creation it blocks makes no object in
     *  its place. */
    private static void weigerDoelBuitenDeInvoer(Build build, List<Object> bonen, Map<Object, Objecttype> typen,
                                                 Map<Class<?>, Objecttype> perKlasse) {
        Map<Objecttype, List<String>> velden = new HashMap<>();
        for (Object boon : bonen)
            for (String veld : velden.computeIfAbsent(typen.get(boon), t -> referentievelden(build.model(), t))) {
                Object doel = Accessors.getter(boon.getClass(), veld).apply(boon);
                if (doel != null && !typen.containsKey(doel))
                    throw new IllegalArgumentException("voegIn: het veld '" + veld + "' van " + naam(boon) + " wijst naar"
                            + " een " + perKlasse.computeIfAbsent(doel.getClass(), k -> objecttype(build, doel)).naam
                            + " (" + naam(doel) + ") die niet als boon in de invoer staat: de tak leidt alleen af voor de"
                            + " objecten in de sessie, dus geef elk object waarnaar een boon wijst ook zelf als boon op");
            }
    }

    /** The object type whose bean {@code boon} is, a bean of the build's bean model. */
    private static Objecttype objecttype(Build build, Object boon) {
        for (Objecttype ot : build.model().objecttypen.values()) {
            if (!Namen.ident(ot.naam, true).equals(boon.getClass().getSimpleName())) continue;
            try {
                if (Class.forName(FactModelGenerator.PAKKET + "." + Namen.ident(ot.naam, true), false,
                        build.beanLoader()) == boon.getClass()) return ot;
            } catch (ClassNotFoundException e) {
                throw new IllegalStateException("geen bonenklasse voor objecttype " + ot.naam, e);
            }
        }
        throw new IllegalArgumentException("voegIn: " + naam(boon) + " is geen boon van dit model: een boon is een"
                + " instantie van een klasse die Build.beanLoader() laadt");
    }

    /** The {@code Emmerinvoer} of bucket scalar {@code e}, keyed as {@code ProducentConsument.elementVan} keys the
     *  attribute, on the type that declares it. */
    private static Emmerinvoer emmerinvoer(Map<Object, Objecttype> typen, Invoer.Emmer e) {
        Objecttype ot = typen.get(e.boon());
        if (ot == null)
            throw new IllegalArgumentException("voegIn: emmerscalair '" + e.attribuut() + "' op " + naam(e.boon())
                    + ", een boon die niet in de invoer staat");
        if (ot.attribuut(e.attribuut()) == null)
            throw new IllegalArgumentException("voegIn: emmerscalair '" + e.attribuut() + "' op " + naam(e.boon())
                    + ", maar " + ot.naam + " heeft geen attribuut van die naam");
        if (!Namen.bevatNaam(ot.granulariteitLeden(), e.attribuut()))
            throw new IllegalArgumentException("voegIn: emmerscalair '" + e.attribuut() + "' op " + naam(e.boon())
                    + ", maar dat attribuut van " + ot.naam + " ligt niet op een tijdlijn; alleen de invoer van een"
                    + " tijdlijn kent de emmerconventie (RUNTIME-SEMANTICS ch. 8.4)");
        return new Emmerinvoer(e.boon(), ProducentConsument.elementVan(ot, e.attribuut(), false));
    }

    /** One to-many list: the reference field {@code veld} its members hold toward {@code houder}, and the relation that
     *  field stands for on the members' bean ({@link FactModelGenerator#herkomst}), since beans of two types can hold
     *  fields of one name toward one holder over two fact types, and a fold reads the members of one relation. The
     *  holder is compared by identity, as the beans do not override {@code equals}. */
    private record Lijstsleutel(String veld, Object houder, FactModelGenerator.Herkomst relatie) {}

    /**
     * The places of the members of every to-many list a rule reads ({@code gelezen}, the fields the members hold toward
     * the holder): per field, holder and relation, the beans of the input whose field holds the holder, in the order of
     * the beans, or in the order of the list {@code opgegeven} gives for that holder. A given list is checked against the
     * beans whether or not a rule reads its field, and counts only where one does; split by the relations of its
     * members, the order kept, it must name every bean of the input whose field of that relation holds the holder.
     */
    private static List<Lijstplaats> lijstplaatsen(Build build, Set<String> gelezen, List<Object> bonen,
                                                   Map<Object, Objecttype> typen, List<Invoer.Lijst> opgegeven) {
        RegelSpraakModel model = build.model();
        Set<String> velden = new HashSet<>(gelezen);
        for (Invoer.Lijst l : opgegeven) velden.add(Namen.ident(l.rol(), false));
        Map<Objecttype, List<String>> referentievelden = new HashMap<>();
        Map<Objecttype, Map<String, FactModelGenerator.Herkomst>> relaties = new HashMap<>();
        Function<Object, Function<String, FactModelGenerator.Herkomst>> relatie = boon -> veld ->
                relaties.computeIfAbsent(typen.get(boon), t -> new HashMap<>())
                        .computeIfAbsent(veld, v -> FactModelGenerator.herkomst(model, typen.get(boon), v));
        // per list, the beans whose field holds the holder, in the order of the beans
        Map<Lijstsleutel, List<Object>> werkelijk = new LinkedHashMap<>();
        for (Object boon : bonen)
            for (String veld : referentievelden.computeIfAbsent(typen.get(boon), t -> referentievelden(model, t))) {
                if (!velden.contains(veld)) continue;
                Object houder = Accessors.getter(boon.getClass(), veld).apply(boon);
                if (houder != null)
                    werkelijk.computeIfAbsent(new Lijstsleutel(veld, houder, relatie.apply(boon).apply(veld)),
                            k -> new ArrayList<>()).add(boon);
            }
        Map<Lijstsleutel, List<Object>> lijsten = new LinkedHashMap<>();
        werkelijk.forEach((k, leden) -> { if (gelezen.contains(k.veld())) lijsten.put(k, leden); });
        Set<Lijstsleutel> gegeven = new HashSet<>();
        for (Invoer.Lijst l : opgegeven) {
            String veld = Namen.ident(l.rol(), false);
            String wat = "voegIn: de lijst van " + naam(l.houder()) + " onder de rol '" + l.rol() + "'";
            Map<Lijstsleutel, List<Object>> delen = new LinkedHashMap<>();
            Set<Object> genoemd = java.util.Collections.newSetFromMap(new IdentityHashMap<>());
            for (Object lid : l.leden()) {
                Objecttype ot = typen.get(lid);
                if (ot == null) throw new IllegalArgumentException(wat + " noemt " + naam(lid) + ", dat geen boon van de invoer is");
                if (!referentievelden.computeIfAbsent(ot, t -> referentievelden(model, t)).contains(veld))
                    throw new IllegalArgumentException(wat + " noemt " + naam(lid) + ", maar een " + ot.naam
                            + " heeft geen referentieveld '" + veld + "'; de rol is die van de houder, de naam van het"
                            + " veld waarmee een lid naar de houder wijst");
                Object houder = Accessors.getter(lid.getClass(), veld).apply(lid);
                if (houder != l.houder())
                    throw new IllegalArgumentException(wat + " noemt " + naam(lid) + ", maar diens veld '" + veld
                            + "' wijst " + (houder == null ? "nergens heen" : "naar " + naam(houder)));
                if (!genoemd.add(lid)) throw new IllegalArgumentException(wat + " noemt " + naam(lid) + " twee keer");
                delen.computeIfAbsent(new Lijstsleutel(veld, houder, relatie.apply(lid).apply(veld)),
                        k -> new ArrayList<>()).add(lid);
            }
            if (delen.isEmpty())
                for (Map.Entry<Lijstsleutel, List<Object>> e : werkelijk.entrySet())
                    if (e.getKey().veld().equals(veld) && e.getKey().houder() == l.houder())
                        throw new IllegalArgumentException(wat + " is leeg gegeven, maar " + naam(e.getValue().get(0))
                                + " wijst met zijn veld '" + veld + "' naar de houder");
            for (Map.Entry<Lijstsleutel, List<Object>> deel : delen.entrySet()) {
                if (!gegeven.add(deel.getKey())) throw new IllegalArgumentException(wat + " is twee keer gegeven");
                for (Object lid : werkelijk.getOrDefault(deel.getKey(), List.of()))
                    if (!genoemd.contains(lid))
                        throw new IllegalArgumentException(wat + " mist " + naam(lid) + ", dat met zijn veld '" + veld
                                + "' naar de houder wijst: een lid buiten de gegeven volgorde heeft geen plaats");
                if (gelezen.contains(veld)) lijsten.put(deel.getKey(), deel.getValue());
            }
        }
        List<Lijstplaats> uit = new ArrayList<>();
        lijsten.forEach((k, leden) -> {
            for (int i = 0; i < leden.size(); i++) uit.add(new Lijstplaats(leden.get(i), k.veld(), i));
        });
        return uit;
    }

    /** The reference fields of a bean of {@code ot}, its inherited ones among them: the members the bean model derives
     *  whose type is the bean class of an object type (§13.3.9, {@link FactModelGenerator#leden}). */
    private static List<String> referentievelden(RegelSpraakModel model, Objecttype ot) {
        Set<String> klassen = new HashSet<>();
        for (Objecttype t : model.objecttypen.values()) klassen.add(Namen.ident(t.naam, true));
        List<String> uit = new ArrayList<>();
        for (Objecttype t : ot.keten())
            for (FactModelGenerator.Lid lid : FactModelGenerator.leden(model, t))
                if (klassen.contains(lid.javaType())) uit.add(lid.eigenschap());
        return uit;
    }

    /** An object in a sentence of {@link #voegIn}: its type and its identity, as the facts of the branch print it. */
    private static String naam(Object o) {
        return o.getClass().getSimpleName() + "@" + Integer.toHexString(System.identityHashCode(o));
    }

    /**
     * What stands in {@code sessie} of the truth-maintenance branch after a {@link #vuur} that reached its fixpoint,
     * read without writing anything on a bean: per object the derived attribute values, in the form the bean field takes
     * them, a timeline as the bean model's segments, and the derived characteristics; the objects and links the
     * creations made; the evaluation faults that stand, as data and not thrown; and the §9.5 deposits, the
     * {@code Inconsistentie} facts. A consumer that fires one session of a model without a creation more than once reads
     * every answer but the last here: the beans hold the input alone until {@link #zetAfgeleideWaarden}, which writes
     * exactly this, and after which the session does not fire again. A fault names its object by type and identity.
     *
     * <p>It reads only a session whose last {@link #vuur} reached its fixpoint, so what it gives is always a fixpoint:
     * that of the input as it stood at that {@code vuur}; an {@code update} after it shows at the next one.
     *
     * @throws UnsupportedOperationException on the exec-model and the DRL-text branch, whose rules write the derived
     *                                       values on the beans as they fire
     * @throws IllegalStateException         a session that was not fired through {@link #vuur}, among them one fired
     *                                       with a plain {@code fireAllRules}, or whose last {@code vuur} ended without
     *                                       a fixpoint ({@link Vuring#geenFixpunt}) or with an exception; the sentence
     *                                       says which
     */
    public static Fixpunt lees(Build build, KieSession sessie) {
        Objects.requireNonNull(build, "build");
        Objects.requireNonNull(sessie, "sessie");
        if (build.tmsTranspiler() == null)
            throw new UnsupportedOperationException("lees leest wat de tak met truth maintenance afleidde; op de "
                    + build.branch() + "-tak schrijven de regels de afgeleide waarden al tijdens het vuren op de bonen");
        return build.tmsTranspiler().lees(sessie, null);
    }

    /**
     * Puts what the truth-maintenance branch derived in {@code sessie} on the fact beans, after the last {@link #vuur}
     * of the session, which reached its fixpoint: exactly what {@link #lees} gives, read by the same code. It sets each
     * derived characteristic and attribute value on its bean, and each link a creation laid on the reference fields that
     * hold it (§13.3.9). Until this call the beans hold the input alone, since that branch writes no bean while it fires.
     * On the exec-model and the DRL-text branch it does nothing, since their rules write the beans as they fire.
     *
     * <p>Before anything is written, it throws the evaluation faults that stand at the fixpoint, and then writes nothing:
     * one fault as its own exception, which is a {@link Breuk.DelingDoorNul} for a division by a numeric zero (an
     * {@code ArithmeticException}, named after the rule with the operands, as the exec-model branch's fire gives it
     * through {@link #benoem}), a {@code Rekenfout} for a fault of a computation or of the evaluation date, a
     * {@code Schrijversconflict} for two writers of one attribute that disagree (an {@code IllegalStateException},
     * RUNTIME-SEMANTICS ch. 5.7) and an {@code Invoervormfout} for a timeline input in the other convention (ch. 8.4);
     * two or more as {@code Evaluatiefouten}, ordered by the name of the form and then by the sentence, with the faults
     * as {@code fouten()}. {@link #lees} gives the same faults as data.
     *
     * <p>It refuses the sessions {@link #lees} refuses, with the same {@code IllegalStateException}, before anything is
     * written: one not fired through {@link #vuur}, and one whose last {@code vuur} ended without a fixpoint.
     *
     * <p>The write-back tells the engine nothing, and a bean then holds derived values beside its input, which a further
     * fire would read as input: an initialisation of a member that holds a value written back would not fire again. So
     * the write-back closes the session with a stated mark, and {@link #vuur} refuses it.
     */
    public static void zetAfgeleideWaarden(Build build, KieSession sessie) {
        Objects.requireNonNull(build, "build");
        Objects.requireNonNull(sessie, "sessie");
        if (build.tmsTranspiler() != null) build.tmsTranspiler().zetAfgeleideWaarden(sessie);
    }

    /** As {@link #zetAfgeleideWaarden(Build, KieSession)}, with {@code label} giving the case label of an input bean
     *  and null for an object a creation made, so that a fault names its object as the reference interpreter names it:
     *  "V, een Vondst" in the fault of two writers that disagree (RUNTIME-SEMANTICS ch. 5.7), where a consumer without
     *  labels gets the type and the identity. The replay writes back through it. */
    public static void zetAfgeleideWaarden(Build build, KieSession sessie, Function<Object, String> label) {
        Objects.requireNonNull(build, "build");
        Objects.requireNonNull(sessie, "sessie");
        Objects.requireNonNull(label, "label");
        if (build.tmsTranspiler() != null) build.tmsTranspiler().zetAfgeleideWaarden(sessie, label);
    }

    /** The fixed fire cap of the exec-model and the DRL-text branch, as before the truth-maintenance branch: their rules
     *  write the beans as they fire, and the cap ends a run whose rules keep overwriting each other. */
    static final int VAST_PLAFOND = 100_000;

    /**
     * The factor of the fire cap of the truth-maintenance branch, {@code FACTOR × R × N0} ({@link #vuur}). Every firing
     * of that branch makes or renews a logical fact of one rule on one subject, and without a cycle, which the build
     * refuses ({@code Kringen}), the renewals per rule and subject are bounded by the depth of the dependency graph, so
     * a run fires a number of times that grows with the rules and the facts. The scale models of card
     * {@code tms-omschakeling} fired 0.56 and 0.74 times per rule per fact before the fire (2026-10-10); 4 is
     * provisional, until the measurement over the played goldens sets it at twice the largest ratio it finds, at least 4.
     */
    static final int FACTOR = 4;

    /** The least fire cap of the truth-maintenance branch: below it a small document would get a cap of a few hundred
     *  firings, which a fixpoint can need. */
    static final int VLOER = 10_000;

    /** What {@link #vuur} did: the firings of all its fires together; the cap it fired under, on the truth-maintenance
     *  branch the cap of its last phase; and the sentence of a run that ends without a fixpoint, at the cap or at the
     *  creation budget ({@code Creatiebudget}), and null at a fixpoint. A run that ends at the budget counts the firings
     *  of its fires before the one in which the creation rule threw. */
    public record Vuring(int vuringen, int plafond, String geenFixpunt) {}

    /**
     * Fires {@code sessie} of {@code build} to its fixpoint under a cap on the firings of the run. On the exec-model and
     * the DRL-text branch the cap is a fixed {@value #VAST_PLAFOND}. On the truth-maintenance branch it is
     * {@code max(}{@value #VLOER}{@code , FACTOR × R × N0)}, with {@code R} the rules of the session's KieBase and
     * {@code N0} the facts the session holds just before it fires ({@link #FACTOR}): the cap is a safeguard against a
     * fault of the lowering, not a norm, so it grows with the input. The sentence at the cap names {@code R}, {@code N0}
     * and the cap. A run that reaches the cap or the creation budget has no fixpoint, and {@link Vuring#geenFixpunt}
     * says so. On the truth-maintenance branch every run leaves a stated mark of how it ended, at its fixpoint, without
     * one, or with an exception out of the fire ({@code Gevuurd}), and {@link #lees} and {@link #zetAfgeleideWaarden}
     * read a session only where the last run reached its fixpoint.
     *
     * <p>On the truth-maintenance branch it refuses a session that {@link #zetAfgeleideWaarden} wrote back, whose beans
     * hold derived values beside their input: a consumer that fires more than once reads between the fires with
     * {@link #lees}, or opens a fresh session with beans that hold input alone.
     *
     * <p>On the truth-maintenance branch it fires a session of a model with a creation once, and refuses it at every
     * later call. An object or a fact creation is stated and never lapses, and so are the initialisations, the links
     * and the fault of a failed initialisation it brought (RUNTIME-SEMANTICS ch. 5.5, {@code Gevuurd}): after an input
     * change a second fire would keep them beside an input they no longer follow from, a state no fresh evaluation
     * gives. The sentence says so, and that the way is a fresh session per input.
     *
     * <p>A model with a creation fires in generations; see {@link #vuur(Build, KieSession, int)}. There the cap is
     * computed again over the facts at the start of every phase, so the objects the creations made count, and never
     * lowered: a later phase may hold fewer facts than an earlier one, and the cap bounds the run as a whole.
     */
    public static Vuring vuur(Build build, KieSession sessie) {
        Objects.requireNonNull(build, "build");
        Objects.requireNonNull(sessie, "sessie");
        return build.tmsTranspiler() == null ? vuur(build, sessie, VAST_PLAFOND) : vuur(build, sessie, FACTOR, VLOER);
    }

    /** As {@link #vuur(Build, KieSession)} on every branch under the cap {@code max(vloer, factor × R × N0)}, rounded
     *  up: the seam a witness gives a small factor and floor to reach the cap, a factor below 1 among them. */
    static Vuring vuur(Build build, KieSession sessie, double factor, int vloer) {
        if (!(factor > 0)) throw new IllegalArgumentException("vuurfactor " + factor + " is niet groter dan 0");
        return drijf(build, sessie, new Plafond(factor, vloer, regels(sessie)));
    }

    /**
     * Fires {@code sessie} of {@code build} to its fixpoint, at most {@code plafond} firings in all, a cap that does not
     * grow: the seam of the scale harness, which sets a cap that never cuts it off, and of a witness that sets a small
     * one. On the exec-model and the DRL-text branch, and on the truth-maintenance branch without a lowered creation,
     * that is one fire.
     *
     * <p>On the truth-maintenance branch with a creation a creation fires only at a fixpoint of the rule layer, and
     * what it makes is stated and never lapses, as in the reference interpreter (RUNTIME-SEMANTICS ch. 5.5): a logical
     * object could keep itself standing through its own output after the state that made it passed, and its
     * initialisations would follow later changes, where ch. 5.5 evaluates them when the object is made. So this fires
     * in generations, here and not in the rules, so that a second rendering of the branch can share it. The creation
     * rules sit in agenda groups of their own ({@code TmsTranspiler.FEITCREATIE}, {@code TmsTranspiler.OBJECTCREATIE}),
     * so a fire without their focus takes the rule layer to rest and leaves them pending. A generation is one phase: it
     * opens a new {@code Creatiefase}, puts both groups on the focus stack, the fact creations on top, and fires. That
     * fires the fact creations that hold at the fixpoint, then the object creations, and once both groups are empty it
     * takes the rule layer to rest again in the same fire. Between the two groups the rule layer does not fire, and
     * Drools evaluates a rule only when the agenda takes it from the group that has the focus, so every creation of a
     * generation reads the same settled world, as in the reference interpreter (RUNTIME-SEMANTICS ch. 5.5,
     * Generations): an object creation does not read what a fact creation of its own generation links. What a phase
     * made carries its number, and a creation rule does not take an object or a link of the phase it fires in, so a
     * recursive chain advances one generation per phase. Until 2026-10-09 a generation was a phase of fact creations
     * and a phase of object creations with the rule layer at rest between them, so an object creation read the links
     * the fact creations of its own generation had laid: an initialisation that counts the maatgevers of a Wachter,
     * over the relation a fact creation in the same recursive group lays, gave 1 where the reference interpreter
     * gives 0.
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
     * <p>A stratum ends at its first generation that makes nothing. The creation budget (RUNTIME-SEMANTICS ch. 5.5) is
     * the depth of a chain of mints, not a count of generations: the creation rule computes the depth of what it makes
     * from the mark of its subject and throws {@link Creatiebudget} at a mint past {@code TmsTranspiler.CREATIEBUDGET},
     * and this ends the run there, with its sentence, as a run without a fixpoint. The generations need no bound of
     * their own: every generation but the last makes an object or lays a link, and under the budget a stratum makes
     * finitely many of both. The rule layer has no budget here either: truth maintenance takes it to rest within the
     * fire, and the cap bounds that. Until 2026-10-09 this counted the generations of a stratum that make
     * something, at most 100, as the reference interpreter did that day: a chain whose every mint waits a generation for
     * a link, and a fact creation that lays a closure of 102 hops, ended without a fixpoint at their 101st generation.
     */
    static Vuring vuur(Build build, KieSession sessie, int plafond) {
        return drijf(build, sessie, new Plafond(plafond));
    }

    /** The rules of the KieBase {@code sessie} fires: the {@code R} of the cap. */
    private static long regels(KieSession sessie) {
        long n = 0;
        for (org.kie.api.definition.KiePackage p : sessie.getKieBase().getKiePackages()) n += p.getRules().size();
        return n;
    }

    /** The cap of one run of {@link #drijf}: a fixed number of firings, or {@code max(vloer, factor × R × N)} over the
     *  facts {@code N} at the start of a phase, rounded up and never lowered within the run. */
    private static final class Plafond {
        /** 0 for a fixed cap. */
        private final double factor;
        private final int vloer;
        private final long regels;
        private int waarde;
        private long feiten = -1;

        /** A fixed cap. */
        Plafond(int vast) {
            this(0, vast, 0);
        }

        Plafond(double factor, int vloer, long regels) {
            this.factor = factor;
            this.vloer = vloer;
            this.regels = regels;
            this.waarde = vloer;
        }

        /** Computes the growing cap over the facts {@code sessie} holds now, where that raises it. */
        void herreken(KieSession sessie) {
            if (factor == 0) return;
            long n = sessie.getFactCount();
            // a double holds every product below 2^53 exactly, and a cap is an int
            double nieuw = Math.min(Integer.MAX_VALUE, Math.max(vloer, Math.ceil(factor * regels * n)));
            if (feiten < 0 || nieuw > waarde) {
                waarde = (int) nieuw;
                feiten = n;
            }
        }

        /** The sentence of a run that reached the cap. */
        String zin() {
            return factor == 0
                    ? "geen fixpunt: het vuurplafond van " + waarde + " vuringen is bereikt"
                    : "geen fixpunt: het vuurplafond van " + waarde + " vuringen is bereikt, het grootste van " + vloer
                      + " en " + java.math.BigDecimal.valueOf(factor).stripTrailingZeros().toPlainString() + " × "
                      + regels + " regels × " + feiten + " feiten vlak vóór het vuren; zonder kring vuurt de tak per"
                      + " regel en feit een begrensd aantal keren, dus dit is een fout van de verlaging";
        }
    }

    /** The driver of every {@code vuur}. On the truth-maintenance branch it refuses a session of a model with a creation
     *  that it fired before, and a session that was written back, and it marks how the run ended ({@link Gevuurd}),
     *  which {@link #lees} and {@link #zetAfgeleideWaarden} read: the mark
     *  of the previous run goes before the fire, so none stands while the session fires, and a fire that throws leaves
     *  a mark with its exception, so that no reading follows a run that broke off. */
    private static Vuring drijf(Build build, KieSession sessie, Plafond plafond) {
        TmsTranspiler tms = build.tmsTranspiler();
        if (tms == null) return vuurOnderPlafond(build, sessie, plafond);
        List<FactHandle> merken = List.copyOf(sessie.<FactHandle>getFactHandles(o -> o instanceof Gevuurd));
        // what a creation made is stated and does not lapse, so a later fire would keep it beside an input it no longer
        // follows from; checked first, since a written-back session of such a model cannot be read between fires either
        if (!merken.isEmpty() && !tms.creatieregels().isEmpty())
            throw new IllegalStateException("dit model heeft creatieregels, en deze sessie is al via DroolsBackend.vuur"
                    + " gevuurd: wat een creatie maakte, met de initialisaties, de koppelingen en de fout van een mislukte"
                    + " initialisatie, is gesteld en vervalt niet (RUNTIME-SEMANTICS ch. 5.5), dus een volgende fire zou"
                    + " na een invoerwijziging een toestand geven die geen verse evaluatie geeft; open per invoer een"
                    + " verse sessie");
        if (!sessie.getObjects(o -> o instanceof Teruggezet).isEmpty())
            throw new IllegalStateException("deze sessie is teruggezet (zetAfgeleideWaarden): haar bonen dragen afgeleide"
                    + " waarden naast de invoer, en een volgende fire zou die als invoer lezen; lees tussen twee fires met"
                    + " DroolsBackend.lees en zet pas na de laatste fire terug, of open een verse sessie met bonen die"
                    + " alleen invoer dragen");
        for (FactHandle merk : merken) sessie.delete(merk);
        Vuring vuring;
        try {
            vuring = vuurOnderPlafond(build, sessie, plafond);
        } catch (RuntimeException e) {
            try {
                sessie.insert(new Gevuurd("de fire brak af met " + e.getClass().getSimpleName() + ": " + e.getMessage()));
            } catch (RuntimeException merkfout) {
                e.addSuppressed(merkfout);
            }
            throw e;
        }
        sessie.insert(new Gevuurd(vuring.geenFixpunt()));
        return vuring;
    }

    /** Fires {@code sessie} as {@link #vuur(Build, KieSession, int)} describes, under {@code plafond}: in one go, or for
     *  a model with a creation on the truth-maintenance branch in generations per stratum. */
    private static Vuring vuurOnderPlafond(Build build, KieSession sessie, Plafond plafond) {
        TmsTranspiler tms = build.tmsTranspiler();
        Set<String> creaties = tms == null ? Set.of() : tms.creatieregels();
        plafond.herreken(sessie);
        if (creaties.isEmpty()) {
            int gevuurd = sessie.fireAllRules(plafond.waarde);
            return new Vuring(gevuurd, plafond.waarde, gevuurd >= plafond.waarde ? plafond.zin() : null);
        }
        // Drools makes a group on demand only in a KieBase that declares a group beside MAIN, which every creation
        // rule does; null means the KieBase lacks the creation rules the transpiler reports
        org.kie.api.runtime.rule.Agenda agenda = sessie.getAgenda();
        for (String groep : List.of(TmsTranspiler.FEITCREATIE, TmsTranspiler.OBJECTCREATIE))
            if (agenda.getAgendaGroup(groep) == null)
                throw new IllegalStateException("de KieBase kent de agendagroep " + groep + " niet, terwijl het model"
                        + " creatieregels heeft: " + creaties);
        int[] gemaakt = {0};
        AgendaEventListener teller = new DefaultAgendaEventListener() {
            @Override public void afterMatchFired(AfterMatchFiredEvent e) {
                if (creaties.contains(e.getMatch().getRule().getName())) gemaakt[0]++;
            }
        };
        sessie.addEventListener(teller);
        try {
            int gevuurd = sessie.fireAllRules(plafond.waarde);
            FactHandle fase = null;
            int nummer = 0;
            for (int stratum : tms.creatiestrata()) {
                while (gevuurd < plafond.waarde) {
                    gemaakt[0] = 0;
                    if (fase != null) sessie.delete(fase);
                    fase = sessie.insert(new Creatiefase(++nummer, stratum));
                    plafond.herreken(sessie);
                    // the focus is a stack: the group given the focus last fires first
                    agenda.getAgendaGroup(TmsTranspiler.OBJECTCREATIE).setFocus();
                    agenda.getAgendaGroup(TmsTranspiler.FEITCREATIE).setFocus();
                    try {
                        gevuurd += sessie.fireAllRules(plafond.waarde - gevuurd);
                    } catch (ConsequenceException e) {
                        Creatiebudget budget = creatiebudget(e);
                        if (budget == null) throw e;
                        return new Vuring(gevuurd, plafond.waarde, budget.getMessage());
                    }
                    if (gemaakt[0] == 0) break;
                }
            }
            return new Vuring(gevuurd, plafond.waarde, gevuurd >= plafond.waarde ? plafond.zin() : null);
        } finally {
            sessie.removeEventListener(teller);
        }
    }

    /** The {@link Creatiebudget} a creation rule threw, which Drools' default handler hands on inside {@code e}, or null
     *  where {@code e} carries another fault. */
    private static Creatiebudget creatiebudget(ConsequenceException e) {
        for (Throwable t = e.getCause(); t != null; t = t.getCause() == t ? null : t.getCause())
            if (t instanceof Creatiebudget budget) return budget;
        return null;
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
     *  condition and the rule, and does not come here; for the subselection of a fold that can fault
     *  ({@code kern.Faalbaar}), which that branch tests on the element bound again in the source of the accumulate,
     *  Drools names no rule. */
    public static RuntimeException benoem(ConstraintEvaluationException e) {
        for (Throwable t = e.getCause(); t != null; t = t.getCause() == t ? null : t.getCause())
            if (t instanceof Breuk.DelingDoorNul nul && nul.regel() != null) return nul;
        return e;
    }
}
