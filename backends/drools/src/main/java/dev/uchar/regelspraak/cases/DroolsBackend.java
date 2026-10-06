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
import dev.uchar.regelspraak.transpiler.model.ExecModelTranspiler;

import dev.uchar.regelspraak.runtime.Breuk;

import org.drools.modelcompiler.KieBaseBuilder;
import org.drools.modelcompiler.constraints.ConstraintEvaluationException;
import org.kie.api.KieBase;
import org.kie.api.runtime.rule.ConsequenceException;
import org.kie.internal.utils.KieHelper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
 * {@link Build#dagsoortGlobalen()}; a {@code ConsequenceException} out of the fire is named by {@link #benoem}.
 */
public final class DroolsBackend {

    private static final ObjectMapper JSON = new ObjectMapper();

    private DroolsBackend() {}

    /** Which transpiler lowers the document. The exec-model branch is the one gate 12's square replays; the
     *  DRL-text branch is narrower — no ch. 7 defaults lowering, more markers — and the gate counts it apart. */
    public enum Branch { EXEC_MODEL, DRL_TEXT }

    /** A built document. Exactly one transpiler is set, the one {@link #branch()} names; {@code dekking} is its
     *  coverage manifest, every entry lowered, because the build refuses a document with any other entry. */
    public record Build(RegelSpraakModel model, ClassLoader beanLoader, ExecModelTranspiler execModelTranspiler,
                        DrlTranspiler drlTranspiler, List<Dekking> dekking, KieBase kieBase) {
        public Build {
            if ((execModelTranspiler == null) == (drlTranspiler == null))
                throw new IllegalArgumentException("a Build carries exactly one transpiler, got "
                        + (execModelTranspiler == null ? "none" : "both"));
        }

        public Branch branch() { return execModelTranspiler != null ? Branch.EXEC_MODEL : Branch.DRL_TEXT; }

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
     *      violation, a construct the branch does not lower, and on the DRL-text branch a prioritized-defaults
     *      extension or a DRL text the Drools compiler rejects */
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
        weigerDubbeleRegelnaam(et.model());
        KieBase kieBase = KieBaseBuilder.createKieBaseFromModel(et.model());
        return new Build(loaded.model(), cl, et, null, List.copyOf(et.dekking()), kieBase);
    }

    /** Two rules of one name in one package: the KieBase keeps one rule per name, so the second would replace the first
     *  and the document would play without it (measured 2026-10-02 on two forms of one name, which WF-020 forbids and a
     *  hand-written document can still carry). The DRL-text branch's compiler refuses the same document with
     *  "Duplicate rule name"; this branch refuses it in those words before the KieBase is built. A rule without a name
     *  (a nameless form, WF-107) has no name to share and is not counted: this branch plays two nameless creations
     *  ({@code TakgelijkheidWitnessTest}). */
    private static void weigerDubbeleRegelnaam(org.drools.model.Model model) {
        java.util.Set<String> namen = new java.util.HashSet<>();
        for (org.drools.model.Rule r : model.getRules())
            if (r.getName() != null && !namen.add(r.getFullName()))
                throw new Excluded("exec-model: Duplicate rule name: " + r.getName() + " — twee regels van één naam in "
                        + "pakket " + r.getPackage() + "; de KieBase houdt één regel per naam, zodat de tweede de eerste "
                        + "zou vervangen (vormnamen zijn uniek, WF-020)");
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
        return new Build(loaded.model(), cl, null, t, List.copyOf(t.dekking()), kieBase);
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
     *  rule and the condition, and does not come here. */
    public static RuntimeException benoem(ConstraintEvaluationException e) {
        for (Throwable t = e.getCause(); t != null; t = t.getCause() == t ? null : t.getCause())
            if (t instanceof Breuk.DelingDoorNul nul && nul.regel() != null) return nul;
        return e;
    }
}
