package io.github.yhavinga.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.yhavinga.regelspraak.engine.BeanCompiler;
import io.github.yhavinga.regelspraak.loader.IrLoader;
import io.github.yhavinga.regelspraak.model.Datatype;
import io.github.yhavinga.regelspraak.model.RegelSpraakModel;
import io.github.yhavinga.regelspraak.model.RegelSpraakModel.Granulariteit;
import io.github.yhavinga.regelspraak.model.RegelSpraakModel.Objecttype;
import io.github.yhavinga.regelspraak.runtime.RegelSpraakRuntime;
import io.github.yhavinga.regelspraak.schema.ContractValidator;
import io.github.yhavinga.regelspraak.schema.FactModelGenerator;
import io.github.yhavinga.regelspraak.transpiler.DrlTranspiler;
import io.github.yhavinga.regelspraak.transpiler.kern.Dekking;
import io.github.yhavinga.regelspraak.transpiler.kern.Namen;
import io.github.yhavinga.regelspraak.transpiler.model.ExecModelTranspiler;

import org.drools.modelcompiler.KieBaseBuilder;
import org.kie.api.KieBase;
import org.kie.api.io.ResourceType;
import org.kie.api.runtime.KieSession;
import org.kie.internal.utils.KieHelper;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The uchar case runner for the Drools backend: it loads an {@code ir.json} off the wire (via
 * {@link IrLoader} — never the RegelSpraak source), builds the Drools {@link KieBase} through the ported
 * exec-model back-end, and replays every {@code case*.json} against it — a FRESH {@link KieSession} per
 * case (the pom notes Drools' KieBuilder flakiness: the KieBase is built ONCE per model, the session is
 * fresh per case). Expectations are compared with the production comparison discipline of
 * {@code ScenarioRunner.Loop}: {@code BigDecimal.compareTo == 0} (scale-safe), null &hArr; leeg, boolean
 * read, and the §9.5 Inconsistentie channel for consistent/inconsistent.
 *
 * <p>A golden is EXCLUDED (not a failure) when the loader refuses a form, the exec transpiler markers a
 * construct (does not lower it), the bean contract is violated, or a case carries an input/expectation
 * form this v0 backend does not carry — each with a reason the differential gate lists. A golden that
 * loads, lowers wholly, and whose cases all match the pinned expectations is PLAYED: because those pinned
 * values are the SAME expectations ref and clingo verify, a played pass IS the differential square.
 */
public final class CaseRunner {

    private static final ObjectMapper JSON = new ObjectMapper();

    /** A golden is not carried by the v0 backend — a reason-carrying exclusion, never a silent skip. */
    static final class Excluded extends RuntimeException {
        Excluded(String reason) { super(reason); }
    }

    /** The outcome of one golden: PLAYED (with a per-case pass/fail list) or EXCLUDED (with a reason). */
    public record Result(String status, String reason, List<String> caseFailures, int casesPlayed) {
        public static Result excluded(String reason) { return new Result("EXCLUDE", reason, List.of(), 0); }
        public static Result played(int n, List<String> failures) {
            return new Result(failures.isEmpty() ? "PLAYED" : "FAIL", "", failures, n);
        }
    }

    private final RegelSpraakModel model;
    private final KieBase kieBase;
    private final ClassLoader cl;
    /** §9.8 the dagsoort predicates the DRL-text branch declares as globals (name -> predicate). The exec-model
     *  branch wires those predicates straight into its lambdas and needs none, so it passes an empty map. */
    private final Map<String, java.util.function.Predicate<LocalDate>> dagsoortGlobalen;

    private CaseRunner(RegelSpraakModel model, KieBase kieBase, ClassLoader cl,
                       Map<String, java.util.function.Predicate<LocalDate>> dagsoortGlobalen) {
        this.model = model; this.kieBase = kieBase; this.cl = cl; this.dagsoortGlobalen = dagsoortGlobalen;
    }

    /** Load {@code irPath}, build the KieBase, and replay each of {@code casePaths}. Any {@link Excluded}
     *  (loader refusal, transpiler marker, contract violation, unsupported form) becomes an EXCLUDE result. */
    public static Result runGolden(Path irPath, List<Path> casePaths) {
        return replay(irPath, casePaths, false);
    }

    /** The same replay through the OTHER transpiler branch: {@link DrlTranspiler} emits DRL TEXT, which is
     *  compiled to a {@link KieBase} here (the classic DRL build; the exec-model branch is untouched). The
     *  gate path never calls this — it is the harness/measurement entry (a {@code --drl} run of
     *  {@link DroolsBackendMain}), so gate 12's numbers cannot move with it. */
    public static Result viaDrlTekst(Path irPath, List<Path> casePaths) {
        return replay(irPath, casePaths, true);
    }

    private static Result replay(Path irPath, List<Path> casePaths, boolean viaDrl) {
        try {
            CaseRunner runner = build(irPath, viaDrl);
            List<String> failures = new ArrayList<>();
            int played = 0;
            for (Path cp : casePaths) {
                JsonNode c = JSON.readTree(Files.readString(cp));
                if (c.has("expectedFault")) continue;   // red-path case in a green zone — not this gate's business
                played++;
                String fail = runner.runCase(cp.getFileName().toString(), c);
                if (fail != null) failures.add(fail);
            }
            return Result.played(played, failures);
        } catch (Excluded e) {
            return Result.excluded(e.getMessage());
        } catch (IrLoader.Refusal e) {
            return Result.excluded("loader: " + e.getMessage());
        } catch (java.io.IOException e) {
            throw new RuntimeException("could not read " + irPath + ": " + e.getMessage(), e);
        }
    }

    /** The witness seam of {@code backends/drools/src/test}: the beans one case's INPUT produces, before any
     *  rule fires. Every gate observes this layer only through post-fire expectation matches, so what an input
     *  BECAME has no other observable surface: the bucket a scalar landed in, the segments a timeline placed on
     *  a {@code List<Segment>} member, or WHICH LEG of a §13.3.9.2 reciprocal pair a role filled — each could be
     *  wrong and still fold to the right total, passing unseen. Roles are therefore set here too, in the replay's
     *  own order (attributes, then roles), so the mirrored self-pair fill is assertable bean-by-bean. Globals
     *  stay out: they live on a {@link KieSession}, which is fire's concern, not this layer's. */
    static Map<String, Object> invoerBeans(Path irPath, JsonNode c) throws java.io.IOException {
        CaseRunner runner = build(irPath);
        runner.rekendatum = asOfDatum(c);
        ClassLoader vorige = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(runner.cl);
        try {
            JsonNode instances = c.get("input").get("instances");
            for (JsonNode i : instances) runner.beanVoor(i);
            for (JsonNode i : instances) runner.zetAttributenEnKenmerken(i);
            for (JsonNode i : instances) runner.zetRollen(i);
            runner.spiegelWederkerigeRollen();
            return runner.labelNaarBean;
        } finally {
            Thread.currentThread().setContextClassLoader(vorige);
        }
    }

    /** One case's input beans plus every fact the session holds AFTER fire — the §13.4.6 witness seam of
     *  {@code backends/drools/src/test}. A MINTED object carries no case label (ref mints into the reserved
     *  {@code "::"} namespace, which the case loader refuses as a label), so no expectation kind the case
     *  schema can express names it: every corpus observation of a creation is INDIRECT, through a fold or a
     *  navigated read on a labelled input. Neither other seam reaches it either — {@link #invoerBeans} stops
     *  before any rule fires, {@link #speelCase} returns only the comparator's verdict. So THAT the mint
     *  exists, WHICH members its toekenningen filled, and that BOTH ends of its back-link were written have
     *  no other observable surface, while each could be wrong and still leave the pinned total right (a mint
     *  reachable one way only; a member written on the subject instead of the mint). */
    record NaFire(Map<String, Object> invoer, List<Object> feiten) {}

    static NaFire naFire(Path irPath, JsonNode c) throws java.io.IOException {
        CaseRunner runner = build(irPath);
        runner.rekendatum = asOfDatum(c);
        ClassLoader vorige = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(runner.cl);
        KieSession sessie = runner.kieBase.newKieSession();
        try {
            JsonNode instances = c.get("input").get("instances");
            for (JsonNode i : instances) runner.beanVoor(i);
            for (JsonNode i : instances) runner.zetAttributenEnKenmerken(i);
            for (JsonNode i : instances) runner.zetRollen(i);
            runner.spiegelWederkerigeRollen();
            runner.zetGlobals(sessie, c);
            for (Object bean : runner.labelNaarBean.values()) sessie.insert(bean);
            int n = sessie.fireAllRules(100_000);
            // The sister runCase path reports the cap as a case failure; this seam has no verdict to return, so
            // a run that never reached a fixpoint must not hand back a half-fired fact base as if it had settled.
            if (n >= 100_000) throw new IllegalStateException("geen fixpunt (kringverwijzing?)");
            return new NaFire(new LinkedHashMap<>(runner.labelNaarBean), new ArrayList<>(sessie.getObjects()));
        } finally {
            sessie.dispose();
            Thread.currentThread().setContextClassLoader(vorige);
        }
    }

    /** The comparator seam of {@code backends/drools/src/test}: one authored in-memory case through the FULL
     *  replay (build, inputs, fire, compare), returning the per-case failure line — null on a match. The
     *  read-back comparator has no other directly observable surface: gate 12 pins post-fire values on corpus
     *  cases whose pinned lists happen to match segment-for-segment, so a comparator that ignores waarde or
     *  order survives the whole gate set (lens 1 proved it by mutation) — its verdicts are asserted here. */
    static String speelCase(Path irPath, JsonNode c) throws java.io.IOException {
        return build(irPath).runCase("case", c);
    }

    /** Load {@code irPath}, refuse the non-replayable forms, generate+contract-check the beans, lower every
     *  construct, and build the {@link KieBase} — the shared prelude of both the green replay and the red probe.
     *  Throws {@link Excluded} / {@link IrLoader.Refusal} for any form the v0 backend does not carry. */
    private static CaseRunner build(Path irPath) throws java.io.IOException {
        return build(irPath, false);
    }

    /** As above, with {@code viaDrl} choosing WHICH transpiler branch produces the KieBase: the exec-model
     *  back-end (false — every gate path) or {@link DrlTranspiler}'s DRL TEXT compiled here (true). */
    private static CaseRunner build(Path irPath, boolean viaDrl) throws java.io.IOException {
        JsonNode ir = JSON.readTree(Files.readString(irPath));
        IrLoader.Loaded loaded = IrLoader.loadAll(ir);   // the model plus the ch. 7 prioritized-defaults extension beside it
        RegelSpraakModel model = loaded.model();

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
            if (viaDrl) return drlTak(model, loaded, cl);
            ExecModelTranspiler et = ExecModelTranspiler.build(model, cl, loaded.prioritizedDefaults(),
                    loaded.rolBindingen(), loaded.lets(), loaded.keuzes());
            // every construct must LOWER — a marker means the exec KieBase silently omits it,
            // so the case's expectations could not be produced; that golden is not carried.
            for (Dekking d : et.dekking())
                if (!d.lowered()) throw new Excluded("transpiler " + d.verdict() + ": " + d.sleutel()
                        + (d.reden().isEmpty() ? "" : " — " + d.reden()));
            KieBase kieBase = KieBaseBuilder.createKieBaseFromModel(et.model());
            return new CaseRunner(model, kieBase, cl, Map.of());
        } finally {
            Thread.currentThread().setContextClassLoader(vorige);
        }
    }

    /** The DRL-TEXT branch of the same prelude: {@link DrlTranspiler} emits DRL, which is compiled to a
     *  {@link KieBase} by the classic DRL build ({@code KieHelper}, mvel dialect). Three forms are refused
     *  here, each loudly and with the reason, because the text branch is NARROWER than the exec-model one:
     *  <ul>
     *    <li>a ch. 7 prioritizedDefaults extension — that lowering exists only in the exec branch, so a DRL
     *        KieBase would silently omit the default and answer with an unwritten attribute;</li>
     *    <li>any construct the text branch markers (its {@code TODO} discipline) — the DRL still compiles,
     *        which is exactly why the marker must be refused rather than fired;</li>
     *    <li>a DRL the Drools compiler rejects — a text-branch limit, not a semantic divergence.</li>
     *  </ul> */
    private static CaseRunner drlTak(RegelSpraakModel model, IrLoader.Loaded loaded, ClassLoader cl) {
        if (!loaded.prioritizedDefaults().isEmpty())
            throw new Excluded("drl: ch. 7 prioritizedDefaults — the DRL-text branch has no defaults lowering");
        DrlTranspiler t = DrlTranspiler.build(model);
        for (Dekking d : t.dekking())
            if (!d.lowered()) throw new Excluded("drl-transpiler " + d.verdict() + ": " + d.sleutel()
                    + (d.reden().isEmpty() ? "" : " — " + d.reden()));
        KieHelper helper = new KieHelper();
        helper.setClassLoader(cl);
        helper.addContent(t.drl(), ResourceType.DRL);
        KieBase kieBase;
        try {
            kieBase = helper.build();
        } catch (RuntimeException e) {
            String m = String.valueOf(e.getMessage()).replace('\n', ' ');
            throw new Excluded("drl compile: " + (m.length() > 200 ? m.substring(0, 200) + "…" : m));
        }
        return new CaseRunner(model, kieBase, cl, t.dagsoortGlobalen());
    }

    // ---------------- the red path (corpus/rood, gate 8's Drools leg) ----------------

    /** What the v0 Drools backend OBSERVES when it replays one {@code expectedFault} case:
     *  FAULT (a genuine runtime fault at fire, with the fault text ref/clingo pin against),
     *  EXCLUDE (a form the backend does not carry, with the reason), or NOFAULT (the case ran to a
     *  model — the backend does not enforce this red-path constraint, so its form is not Drools-carried). */
    public record RoodResult(String status, String detail) {}

    /** Replay a single {@code corpus/rood} case and report what the backend observes. A fault the loader or a
     *  case-input form raises before fire is an EXCLUDE (the form is not carried); a fault raised AT fire — a
     *  division by zero, a fixpoint failure — is the FAULT this gate leg pins. */
    public static RoodResult runRood(Path irPath, Path casePath) {
        CaseRunner runner;
        try {
            runner = build(irPath);
        } catch (Excluded e) {
            return new RoodResult("EXCLUDE", e.getMessage());
        } catch (IrLoader.Refusal e) {
            return new RoodResult("EXCLUDE", "loader: " + e.getMessage());
        } catch (java.io.IOException e) {
            throw new RuntimeException("could not read " + irPath + ": " + e.getMessage(), e);
        }
        try {
            JsonNode c = JSON.readTree(Files.readString(casePath));
            String fault = runner.fireRood(c);
            return fault == null
                    ? new RoodResult("NOFAULT", "ran to a model without faulting")
                    : new RoodResult("FAULT", fault);
        } catch (Excluded e) {
            return new RoodResult("EXCLUDE", e.getMessage());
        } catch (java.io.IOException e) {
            throw new RuntimeException("could not read " + casePath + ": " + e.getMessage(), e);
        } catch (RuntimeException e) {
            // a fault raised while loading inputs or firing rules — a division by zero surfaces here, wrapped by
            // Drools; the whole cause chain is the report so the pinned fragment can match the deepest message.
            return new RoodResult("FAULT", faultText(e));
        }
    }

    /** Fire one red-path case: load its world, fire the rules, and return the fault text if the run does not
     *  reach a fixpoint. Returns {@code null} when the run completes cleanly (a NOFAULT — the backend produced a
     *  model where ref/clingo reject). A fault raised at fire propagates as an exception the caller renders. */
    private String fireRood(JsonNode c) {
        labelNaarBean.clear();
        labelType.clear();
        rekendatum = asOfDatum(c);
        ClassLoader vorige = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(cl);
        KieSession sessie = kieBase.newKieSession();
        try {
            JsonNode input = c.get("input");
            JsonNode instances = input.get("instances");
            for (JsonNode i : instances) beanVoor(i);
            for (JsonNode i : instances) zetAttributenEnKenmerken(i);
            for (JsonNode i : instances) zetRollen(i);
            spiegelWederkerigeRollen();
            zetGlobals(sessie, c);
            for (Object bean : labelNaarBean.values()) sessie.insert(bean);
            int n = sessie.fireAllRules(100_000);
            return n >= 100_000 ? "geen fixpunt (kringverwijzing?)" : null;
        } finally {
            sessie.dispose();
            Thread.currentThread().setContextClassLoader(vorige);
        }
    }

    /** The full cause chain of a fault as one line — {@code SimpleName: message <- SimpleName: message …} — so the
     *  pinned fragment can match the deepest wrapped message (a Drools rule fault wraps the runtime's exception). */
    private static String faultText(Throwable t) {
        StringBuilder sb = new StringBuilder();
        Throwable c = t;
        while (c != null) {
            if (sb.length() > 0) sb.append(" <- ");
            sb.append(c.getClass().getSimpleName());
            if (c.getMessage() != null) sb.append(": ").append(c.getMessage());
            Throwable next = c.getCause();
            if (next == c) break;   // guard a self-referential cause chain
            c = next;
        }
        return sb.toString();
    }

    // ---------------- one case ----------------

    private final Map<String, Object> labelNaarBean = new LinkedHashMap<>();
    private final Map<String, Objecttype> labelType = new LinkedHashMap<>();
    /** The case's evaluation date, read BEFORE the inputs are set: the asOfDate-bucket convention places a
     *  scalar input on a temporal attribute in the bucket this date falls in ({@link #temporeleInvoer}). */
    private LocalDate rekendatum;

    private String runCase(String naam, JsonNode c) {
        labelNaarBean.clear();
        labelType.clear();
        rekendatum = asOfDatum(c);
        ClassLoader vorige = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(cl);
        KieSession sessie = kieBase.newKieSession();
        try {
            JsonNode input = c.get("input");
            JsonNode instances = input.get("instances");
            for (JsonNode i : instances) beanVoor(i);
            for (JsonNode i : instances) zetAttributenEnKenmerken(i);
            for (JsonNode i : instances) zetRollen(i);
            spiegelWederkerigeRollen();
            zetGlobals(sessie, c);
            for (Object bean : labelNaarBean.values()) sessie.insert(bean);
            int n = sessie.fireAllRules(100_000);
            if (n >= 100_000) return naam + ": geen fixpunt (kringverwijzing?)";
            List<String> mis = new ArrayList<>();
            for (JsonNode v : c.get("expected")) {
                String m = vergelijk(v, sessie);
                if (m != null) mis.add(m);
            }
            return mis.isEmpty() ? null : naam + ": " + String.join("; ", mis);
        } finally {
            sessie.dispose();
            Thread.currentThread().setContextClassLoader(vorige);
        }
    }

    private Object beanVoor(JsonNode i) {
        String label = i.get("label").asText();
        Object bean = labelNaarBean.get(label);
        if (bean != null) return bean;
        String otNaam = i.get("objectType").asText();
        Objecttype ot = model.objecttypeOpNaam(otNaam);
        if (ot == null) throw new Excluded("case names an unknown objectType: " + otNaam);
        try {
            bean = Class.forName(FactModelGenerator.PAKKET + "." + Namen.ident(ot.naam, true), true, cl)
                    .getConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("kon bean voor label '" + label + "' niet maken", e);
        }
        labelNaarBean.put(label, bean);
        labelType.put(label, ot);
        return bean;
    }

    /** Whether {@code attr} on {@code ot} is a §13.3.6 temporal bean member — a {@code List<Segment>} field,
     *  the shape {@link FactModelGenerator} derives from a declared attribuutGranulariteit. */
    private boolean temporeel(Objecttype ot, String attr) {
        return ot.attribuutGranulariteit.containsKey(attr);
    }

    /** Whether {@code attr} on {@code ot} is a §3.6 dimensioned bean member — a {@code Map<String, BigDecimal>}
     *  field. Checked BEFORE {@link #temporeel}: the generator gives a dimensioned declaration the Map shape
     *  even when a granularity is declared beside it. */
    private boolean gedimensioneerd(Objecttype ot, String attr) {
        return !ot.dimensiesVan(attr).isEmpty();
    }

    private void zetAttributenEnKenmerken(JsonNode i) {
        Object bean = labelNaarBean.get(i.get("label").asText());
        Objecttype ot = model.objecttypeOpNaam(i.get("objectType").asText());
        JsonNode attrs = i.get("attributes");
        if (attrs != null) for (Map.Entry<String, JsonNode> e : attrs.properties()) {
            Datatype dt = ot.attribuut(e.getKey());
            if (dt == null) throw new Excluded("case sets undeclared attribute '" + e.getKey() + "' on " + ot.naam);
            if (gedimensioneerd(ot, e.getKey()))
                throw new Excluded("dimensioned attribute input '" + e.getKey() + "' (§3.6, a Map bean member) "
                        + "is not carried by the v0 backend");
            if (temporeel(ot, e.getKey())) {
                roep(bean, Namen.setter(e.getKey()), List.class,
                        temporeleInvoer(e.getValue(), e.getKey(), ot.attribuutGranulariteit.get(e.getKey())));
                continue;
            }
            roep(bean, Namen.setter(e.getKey()), argType(dt), waardeNaarArg(e.getValue(), e.getKey()));
        }
        JsonNode kenmerken = i.get("characteristics");
        if (kenmerken != null) for (JsonNode k : kenmerken) {
            // A CASE defect, not an uncarried form (maintainer decision 2026-09-09) — the sister arm below has
            // said so about the mirror-image mistake since it was written, and this arm is the same seam from
            // the other side. A TEMPORAL kenmerk's bean member is a List<Segment>, so a scalar-true assertion
            // reaches no reader here; in the interpreter and the ASP backend it reached none either, and BOTH
            // answered 0 where the golden pins 30 rather than saying anything (measured 2026-09-09 on
            // jutterswet-tegenwacht and anchor/jutterswet-dubbelwacht). All three readers refuse it at load
            // since that decision. It stayed an Excluded until then, which filed a broken case under "a form
            // the v0 backend does not carry" — a counted, reasoned refusal in the gate report for a case that
            // is simply wrong, and the one classification that could keep it out of sight.
            if (ot.kenmerkGranulariteit.containsKey(k.asText()))
                throw new IllegalArgumentException("scalar characteristic input '" + k.asText() + "' on the "
                        + "time-dependent kenmerk of " + ot.naam + ": deliver it as a characteristicTimelines "
                        + "entry — the static characteristics list asserts a scalar truth no temporal reader "
                        + "consults");
            roep(bean, Namen.setter(k.asText()), boolean.class, Boolean.TRUE);
        }
        JsonNode ctls = i.get("characteristicTimelines");
        if (ctls != null) for (Map.Entry<String, JsonNode> e : ctls.properties()) {
            // a CASE defect, not an uncarried form — ref refuses this shape at load (case.ml: "a
            // characteristic timeline on a non-temporal characteristic"), so it is hard here too; an
            // Excluded would file a broken corpus case under "not carried", silently green.
            if (!ot.kenmerkGranulariteit.containsKey(e.getKey()))
                throw new IllegalArgumentException("kenmerktijdlijn op niet-temporeel kenmerk '" + e.getKey() + "'");
            roep(bean, Namen.setter(e.getKey()), List.class, kenmerkTijdreeks(e.getValue(), e.getKey()));
        }
    }

    private void zetRollen(JsonNode i) {
        Object bean = labelNaarBean.get(i.get("label").asText());
        Objecttype tI = model.objecttypeOpNaam(i.get("objectType").asText());
        JsonNode rollen = i.get("roles");
        if (rollen == null) return;
        for (Map.Entry<String, JsonNode> e : rollen.properties()) {
            for (JsonNode labelNode : e.getValue()) {
                Object doel = labelNaarBean.get(labelNode.asText());
                if (doel == null) throw new Excluded("role '" + e.getKey() + "' names unknown label " + labelNode.asText());
                Objecttype tL = objecttypeVanBean(doel);
                // §13.4.1 the WRITTEN role IS the navigation — and for a SELF-TYPED pair it is the ONLY route:
                // the by-pair search below counts both same-typed sibling roles and derives null by construction
                // (RegelSpraakModel.verbindendeRol; ref BLOCKS the same derivation in connecting_pair). Resolved
                // through the very authority FactModelGenerator.zelfpaarRollen derived those fields from, and
                // ACCEPTED ONLY when it names one of them, so a hit can only write a field the bean carries. That
                // membership test is what keeps this from being a general by-name override: a by-pair-AMBIGUOUS
                // cross-type pair derives no bean field at all, so resolving it by name would call a setter no
                // bean has. Where the by-pair search already succeeds the outcome is unchanged — a by-name hit is
                // always one of the by-pair candidates, so a unique pair resolves to the same role either way.
                String viaNaam = model.verbindendeRolViaNaam(tI, tL, e.getKey());
                boolean zelfpaar = viaNaam != null && FactModelGenerator.zelfpaarRollen(model, tI).contains(viaNaam);
                // the field on THIS bean (to-one from here): the written self-pair role, else the type pair
                String rolIL = zelfpaar ? viaNaam : model.verbindendeRol(tI, tL);
                if (rolIL != null && !model.naarVeelRol(tI, tL, rolIL)) {
                    naamdelingsPoort(tI, rolIL, tL, e.getKey());
                    zetToOne(bean, rolIL, doel);
                } else {
                    String rolLI = model.verbindendeRol(tL, tI);  // the field lives on the target (this is the one side)
                    if (rolLI == null || model.naarVeelRol(tL, tI, rolLI))
                        // NOT UNIQUE and UNIQUE BUT TO-MANY are different failures and are reported apart (lens 2,
                        // 2026-08-30): the erflijn shape resolves `erfvolger` by name PERFECTLY, and what stops it
                        // is its CARDINALITY — a to-many role's storage is the far end's field — where the old
                        // single sentence blamed a non-unique derivation that had in fact succeeded.
                        throw new Excluded("role '" + e.getKey() + "' (" + tI.naam + " -> " + tL.naam + ") reaches no "
                                + "to-one reference field: " + (viaNaam == null
                                    ? "the written role name resolves to no unique role"
                                    : model.naarVeelRol(tI, tL, viaNaam)
                                    ? "the written role name resolves to '" + viaNaam + "', which is TO-MANY — its "
                                      + "storage is the far end's field, not a scalar reference here"
                                    : "the written role name resolves to the to-one role '" + viaNaam + "', which is "
                                      + "not one of the §13.3.9 reference fields the bean model derives")
                                + ", and " + (rolIL != null
                                    ? "this direction resolves the to-many role '" + rolIL + "'"
                                    : rolLI != null
                                    ? "the far direction resolves the to-many role '" + rolLI + "'"
                                    : "neither direction of the type pair derives a to-one role"));
                    naamdelingsPoort(tL, rolLI, tI, e.getKey());
                    zetToOne(doel, rolLI, bean);
                }
            }
        }
    }

    /** The §13.3.9 NAME-SHARING gate on a resolved to-one role write: {@code drager}'s bean must carry the
     *  reference field for {@code rol} TYPED (a field of {@code doel}'s class), not merely a property of that
     *  name. Both roles here come from the very authorities {@link FactModelGenerator#leden} derives the fields
     *  from ({@code zelfpaarRollen} / {@code verbindendeRol}), so the ONE thing that can still be missing is the
     *  reference itself, displaced by a declared member of the same name (leden()'s dedup: attribute >
     *  characteristic > reference). Without this gate the write below reaches {@code set<X>(BigDecimal)} with a
     *  fact — "kon setter setOpvolger(Wachter) niet aanroepen", an unclassified ERROR the gate refuses to excuse
     *  — where the honest outcome is a loud, reason-carrying exclusion (lens 2, 2026-08-30). The §13.3.9.2
     *  wederkerig twin of this limit is refused in {@link #build} for the whole model. */
    private void naamdelingsPoort(Objecttype drager, String rol, Objecttype doel, String geschreven) {
        if (FactModelGenerator.heeftReferentieVeld(model, drager, rol, doel)) return;
        throw new Excluded("role '" + geschreven + "' resolves to the to-one role '" + rol + "' on " + drager.naam
                + ", which shares its name with a declared member — the bean cannot carry the reference field "
                + "(name-sharing bean limit: attribute > characteristic > reference)");
    }

    /** One to-one role write, WF-013-guarded: a second, DIFFERENT filler on the same to-one field is a case
     *  shape ref refuses at load, so last-wins here would play a broken case green — the mirror pass's own
     *  argument, applied to the forward leg (lens 2, 2026-08-30). The same filler twice stays idempotent. */
    private void zetToOne(Object drager, String rol, Object doel) {
        Object huidig = lees(drager, Namen.getter(rol));
        if (huidig != null && huidig != doel)
            throw new IllegalArgumentException("to-one rol '" + rol + "' krijgt een tweede, ander doel — "
                    + "ref weigert die case bij het laden (WF-013)");
        roep(drager, Namen.setter(rol), doel.getClass(), doel);
    }

    /** §13.3.9.2 mirrored: ONE reciprocal entry holds in BOTH directions, so every self-pair leg the case wrote
     *  gets its reverse materialised — exactly what {@code Case.materialise_reciprocal} does in ref, and what
     *  RUNTIME-SEMANTICS §4.1 requires ("the pinned runtime writes both to-one legs so navigation never reads a
     *  spuriously empty side"). Both goldens pin it: a partner that declares no roles of its own still reads its
     *  side, and the reverse read {@code Jutter( maat == $subject )} an aggregation lowers to needs the leg
     *  present. Run as a SEPARATE PASS over the finished links, like ref's, so the outcome cannot depend on the
     *  order instances appear in: a conflict must be seen wherever the colliding leg was written. Idempotent
     *  (a case supplying both legs means one relation), and a CONFLICTING leg on the to-one role is hard — ref
     *  refuses that case at load (WF-013), so overwriting it here would file a broken case under a green replay.
     *
     *  <p>Scoped to the WEDERKERIG shape on purpose, and ref-arbitrated: an ORDINARY self-typed pair (two distinct
     *  sibling roles, {@code voorganger}/{@code opvolger}) must NOT mirror. ref's {@code Case.materialise_reciprocal}
     *  mirrors only when {@code ft.wederkerig}, so a case writing {@code W1{opvolger:[W2]}} leaves W2's voorganger
     *  reading EMPTY — measured on the erflijn shape (card lens 2026-08-30: the counting probe reads 1 opvolger on
     *  W1 and 0 voorgangers on W2; the mirrored hypothesis MISMATCHes in ref). {@link #zetRollen} therefore writes
     *  that leg directed, on the written role's field only. */
    private void spiegelWederkerigeRollen() {
        for (Object bean : labelNaarBean.values()) {
            String rol = FactModelGenerator.zelfpaarRol(model, objecttypeVanBean(bean));
            if (rol == null) continue;
            Object partner = lees(bean, Namen.getter(rol));
            if (partner == null) continue;
            Object terug = lees(partner, Namen.getter(rol));
            if (terug == bean) continue;
            if (terug != null)
                throw new IllegalArgumentException("wederkerige rol '" + rol + "': het doel is al verbonden met "
                        + "een ander — de omgekeerde poot botst (WF-013 to-one)");
            roep(partner, Namen.setter(rol), bean.getClass(), bean);
        }
    }

    private Objecttype objecttypeVanBean(Object bean) {
        for (Objecttype ot : model.objecttypen.values())
            if (Namen.ident(ot.naam, true).equals(bean.getClass().getSimpleName())) return ot;
        throw new IllegalStateException("geen objecttype voor bean " + bean.getClass());
    }

    private void zetGlobals(KieSession sessie, JsonNode c) {
        JsonNode params = c.get("input").get("parameters");
        if (params != null) for (Map.Entry<String, JsonNode> e : params.properties()) {
            if (e.getValue().has("timeline"))
                throw new Excluded("granular (timeline) parameter '" + e.getKey() + "' is not carried by the v0 backend");
            setGlobalVeilig(sessie, Namen.ident(e.getKey(), false), waardeNaarArg(e.getValue(), e.getKey()));
        }
        if (rekendatum != null) setGlobalVeilig(sessie, "rekendatum", rekendatum);
        // §9.8 the DRL-text branch reaches its dagsoort predicates through globals (the exec branch closes over
        // them); empty for the exec branch, so this loop is a no-op on every gate path.
        for (Map.Entry<String, java.util.function.Predicate<LocalDate>> e : dagsoortGlobalen.entrySet())
            setGlobalVeilig(sessie, e.getKey(), e.getValue());
    }

    /** The case's {@code asOfDate}, or null when it carries none. */
    private static LocalDate asOfDatum(JsonNode c) {
        JsonNode asOf = c.get("asOfDate");
        return asOf == null || asOf.isNull() ? null : datumVanCaseWire(asOf.asText(), "asOfDate");
    }

    private static void setGlobalVeilig(KieSession sessie, String naam, Object waarde) {
        try { sessie.setGlobal(naam, waarde); }
        catch (RuntimeException onbekend) { /* global not declared: no rule reads it */ }
    }

    // ---------------- §13.3.6 temporal input (the List<Segment> bean members) ----------------

    /** The {@code List<Segment>} a case value denotes for a temporal attribute at {@code gran} — the two input
     *  forms {@code ref} carries (ref/lib/case.ml), mirrored so both executors read the SAME timeline:
     *  <ul>
     *    <li>a {@code {"timeline":[…]}} value: the written segments, validated exactly as ref validates them;</li>
     *    <li>a SCALAR value: the asOfDate-bucket convention recorded in {@code corpus/golden/README.md} — the
     *        scalar IS the value of the bucket the evaluation date falls in. ref keeps that scalar aside and
     *        folds precisely this one-bucket timeline inside the ch. 3.6 collapse ({@code Eval}'s temporal
     *        total: value × whole units of its own grid); the bean model has no scalar slot beside the
     *        {@code List<Segment>} field, so the bucket is materialised here and
     *        {@link RegelSpraakRuntime#rsTijdTotaal} folds the identical segment to the identical scalar.</li>
     *  </ul>
     *  Deliberate consequence of that materialisation: the bean now carries a shape ref REFUSES to serve —
     *  ref has no point-in-time read at all (ch. 3.3: a temporal attribute in a scalar slot is refused, the
     *  collapses are the only bridges), and its no-filter ch. 3.6 total even faults on a case-supplied FULL
     *  timeline ("the asOfDate-bucket convention carries the total", eval.ml). The divergence surface is thus
     *  not a wrong value but a read Drools would answer where ref faults; such a case can never become a
     *  golden, because gate 5 replays every case through ref first — the executors agree wherever the corpus
     *  looks, by construction. */
    private List<Object> temporeleInvoer(JsonNode w, String attr, Granulariteit gran) {
        if (w == null || w.isNull()) return null;   // an absent value is an absent timeline (leeg, ch. 3.1)
        if (w.has("timeline")) return tijdreeks(w.get("timeline"), attr, gran, false);
        if (rekendatum == null)
            throw new Excluded("scalar input on temporal attribute '" + attr + "' without an asOfDate: the "
                    + "bucket convention (corpus/golden/README.md) has no evaluation date to place it in");
        LocalDate[] emmer = emmer(gran, rekendatum, attr);
        return List.of(segment(emmer[0], emmer[1], segmentWaarde(w, attr)));
    }

    /** The segments of a timeline value, with ref's load-time validation mirrored: at least one segment (an
     *  EXPECTATION may be empty — {@code leegToegestaan}), {@code from} before {@code to}, both boundaries on the
     *  declared granularity grid, and the list ordered and non-overlapping as written. A malformed timeline is a
     *  hard failure, never a silent acceptance: the case schema (gate 4) and ref (gate 5) reject it first, so
     *  reaching it here means the corpus and this reader disagree. */
    private List<Object> tijdreeks(JsonNode segs, String veld, Granulariteit gran, boolean leegToegestaan) {
        if (segs == null || !segs.isArray())
            throw new IllegalArgumentException("misvormde tijdlijnwaarde op '" + veld + "'");
        if (segs.isEmpty() && !leegToegestaan)
            throw new IllegalArgumentException("tijdlijnwaarde op '" + veld + "' draagt geen enkel segment");
        List<Object> uit = new ArrayList<>();
        LocalDate vorigeTot = null;
        for (JsonNode sj : segs) {
            LocalDate van = grensDatum(sj, "from", veld), tot = grensDatum(sj, "to", veld);
            if (!van.isBefore(tot))
                throw new IllegalArgumentException("segment 'to' op '" + veld + "' ligt niet na 'from'");
            if (!emmer(gran, van, veld)[0].equals(van) || !emmer(gran, tot, veld)[0].equals(tot))
                throw new IllegalArgumentException("segmentgrens op '" + veld + "' ligt niet op het "
                        + gran + "-raster");
            if (vorigeTot != null && vorigeTot.isAfter(van))
                throw new IllegalArgumentException("tijdlijnsegmenten op '" + veld + "' zijn ongeordend of overlappen");
            vorigeTot = tot;
            uit.add(segment(van, tot, segmentWaarde(sj.get("value"), veld)));
        }
        return uit;
    }

    /** The TRUE-period {@code List<Segment>} a {@code characteristicTimelines} entry denotes. True periods are
     *  DAY-precise regardless of the declared granularity (ref: the granularity governs the kenmerk's bucketing,
     *  never its period grid — the ch. 3.7 pro-rata's whole point), so no grid check applies; ref sorts the
     *  periods and rejects overlap, and this does the same. The segments carry a null waarde: the runtime builds
     *  its own true-period timelines that way (rsTijdOfUnie) and every consumer reads van/tot only (rsTijdKnip). */
    private List<Object> kenmerkTijdreeks(JsonNode periodes, String kenmerk) {
        if (periodes == null || !periodes.isArray())
            throw new IllegalArgumentException("misvormde kenmerktijdlijn op '" + kenmerk + "'");
        List<LocalDate[]> gesorteerd = new ArrayList<>();
        for (JsonNode pj : periodes) {
            LocalDate van = grensDatum(pj, "from", kenmerk), tot = grensDatum(pj, "to", kenmerk);
            if (!van.isBefore(tot))
                throw new IllegalArgumentException("periode 'to' op '" + kenmerk + "' ligt niet na 'from'");
            gesorteerd.add(new LocalDate[]{ van, tot });
        }
        gesorteerd.sort((a, b) -> a[0].equals(b[0]) ? a[1].compareTo(b[1]) : a[0].compareTo(b[0]));
        List<Object> uit = new ArrayList<>();
        LocalDate vorigeTot = null;
        for (LocalDate[] p : gesorteerd) {
            if (vorigeTot != null && p[0].isBefore(vorigeTot))
                throw new IllegalArgumentException("perioden op '" + kenmerk + "' overlappen");
            vorigeTot = p[1];
            uit.add(segment(p[0], p[1], null));
        }
        return uit;
    }

    /** The calendar bucket {@code [van, tot)} containing {@code d} (ch. 3.2): year buckets run 1 Jan → 1 Jan,
     *  month buckets first-of-month → first-of-next. Bucketing is DEFINED for month and year only — ref faults on
     *  the other granularities — so a DAG/WEEK/KWARTAAL declaration is a loud not-carried, never a guessed grid. */
    private static LocalDate[] emmer(Granulariteit gran, LocalDate d, String veld) {
        switch (gran) {
            case JAAR:
                return new LocalDate[]{ LocalDate.of(d.getYear(), 1, 1), LocalDate.of(d.getYear() + 1, 1, 1) };
            case MAAND:
                LocalDate van = d.withDayOfMonth(1);
                return new LocalDate[]{ van, van.plusMonths(1) };
            default:
                throw new Excluded("granularity " + gran + " on '" + veld + "' is not carried by the v0 backend: "
                        + "bucketing is defined for month and year only (ch. 3.2)");
        }
    }

    /** The numeric value of one segment. The generated {@code Segment} holds a {@code BigDecimal} waarde, so a
     *  non-numeric timeline value has no bean representation — a reason-carrying not-carried, never a coercion. */
    private BigDecimal segmentWaarde(JsonNode w, String veld) {
        Object v = waardeNaarArg(w, veld);
        if (v == null || v instanceof BigDecimal) return (BigDecimal) v;
        throw new Excluded("non-numeric timeline value on '" + veld + "' is not carried by the v0 backend — "
                + "the generated Segment holds a BigDecimal waarde");
    }

    private static LocalDate grensDatum(JsonNode seg, String veld, String waar) {
        JsonNode d = seg == null ? null : seg.get(veld);
        if (d == null || !d.isTextual())
            throw new IllegalArgumentException("segment op '" + waar + "' mist een '" + veld + "'-datum");
        // the field name carries BOTH coordinates: a case may hold many timelines and each holds two
        // boundaries, so "onderhoudstarief.from" is the only form a producer can act on
        return datumVanCaseWire(d.asText(), waar + "." + veld);
    }

    /** One {@code Segment} fact of the generated bean model, built through the runtime's own reflective
     *  constructor — the Segment class is a runtime objecttype, unknown at compile time. */
    private Object segment(LocalDate van, LocalDate tot, BigDecimal waarde) {
        try {
            return RegelSpraakRuntime.newSegment(
                    Class.forName(FactModelGenerator.PAKKET + ".Segment", true, cl), van, tot, waarde);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("de gegenereerde Segment-factklasse ontbreekt", e);
        }
    }

    // ---------------- comparison ----------------

    private String vergelijk(JsonNode v, KieSession sessie) {
        String kind = v.get("kind").asText();
        return switch (kind) {
            case "attributeValue" -> attribuut(v, false);
            case "attributeEmpty" -> attribuut(v, true);
            case "characteristicPresent" -> kenmerk(v, true);
            case "characteristicAbsent" -> kenmerk(v, false);
            case "consistent" -> consistentie(sessie, false, v);
            case "inconsistent" -> consistentie(sessie, true, v);
            case "attributeTimeline" -> tijdlijn(v);
            default -> throw new Excluded("unknown expectation kind: " + kind);
        };
    }

    private String attribuut(JsonNode v, boolean leegVerwacht) {
        String attr = v.get("attribute").asText();
        String label = v.get("label").asText();
        Objecttype ot = labelType.get(label);
        if (ot != null && gedimensioneerd(ot, attr))
            throw new Excluded("dimensioned attribute read-back '" + attr + "' (§3.6, a Map bean member) "
                    + "is not carried by the v0 backend");
        // a temporal attribute's bean member is the List<Segment> timeline itself: the scalar comparison below
        // would compare a list against a scalar. Its read-back form is the attributeTimeline expectation.
        if (ot != null && temporeel(ot, attr))
            throw new Excluded("scalar read-back of temporal attribute '" + attr + "' is not carried by the v0 "
                    + "backend — the bean holds a List<Segment>; use an attributeTimeline expectation");
        Object bean = labelNaarBean.get(label);
        Object werkelijk = bean == null ? null : lees(bean, Namen.getter(attr));
        if (leegVerwacht)
            return werkelijk == null ? null : "verwacht leeg, werkelijk " + werkelijk;
        Object verwacht = waardeNaarArg(v.get("value"), attr);
        return gelijk(verwacht, werkelijk) ? null
                : "attribuut " + attr + ": verwacht " + render(verwacht) + ", werkelijk " + render(werkelijk);
    }

    /** The §13.3.6 timeline read-back: the doel's {@code List<Segment>} against the pinned segment list, with
     *  ref's comparison discipline — equal length, van/tot EXACT, waarde scale-safe ({@code compareTo == 0}),
     *  compared index-for-index. Both sides arrive ordered (the expectation through the validator, the bean
     *  list through the runtime's ordered writes), so an order-blind comparator has no observable witness —
     *  the claim stops at what is falsifiable. A never-written target (a null field) matches an explicitly
     *  empty expectation: both read leeg on every day (ch. 3.1), exactly as ref and clingo pin it. */
    private String tijdlijn(JsonNode v) {
        String attr = v.get("attribute").asText(), label = v.get("label").asText();
        Objecttype ot = labelType.get(label);
        // both are CASE defects, not uncarried forms — ref rejects the same shapes at load ("expected
        // timeline on %S, which declares no granularity"), so they are hard failures here too, and the
        // unknown-label arm would otherwise reach emmer() with a null granularity (an unnamed NPE).
        if (ot == null)
            throw new IllegalArgumentException("attributeTimeline-verwachting op onbekend label '" + label + "'");
        if (!temporeel(ot, attr))
            throw new IllegalArgumentException("attributeTimeline-verwachting op niet-temporeel attribuut '" + attr + "'");
        Granulariteit gran = ot.attribuutGranulariteit.get(attr);
        List<Object> verwacht = tijdreeks(v.get("timeline"), attr, gran, true);
        Object gelezen = labelNaarBean.containsKey(label) ? lees(labelNaarBean.get(label), Namen.getter(attr)) : null;
        if (gelezen == null)
            return verwacht.isEmpty() ? null
                    : "attribuut " + attr + ": draagt geen tijdreeks, verwacht " + renderTl(verwacht);
        List<?> werkelijk = (List<?>) gelezen;
        boolean gelijk = werkelijk.size() == verwacht.size();
        for (int k = 0; gelijk && k < verwacht.size(); k++)
            gelijk = segGelijk(verwacht.get(k), werkelijk.get(k));
        return gelijk ? null
                : "attribuut " + attr + ": verwacht " + renderTl(verwacht) + ", werkelijk " + renderTl(werkelijk);
    }

    private static boolean segGelijk(Object verw, Object werkelijk) {
        return java.util.Objects.equals(segVeld(verw, "van"), segVeld(werkelijk, "van"))
                && java.util.Objects.equals(segVeld(verw, "tot"), segVeld(werkelijk, "tot"))
                && gelijk(segVeld(verw, "waarde"), segVeld(werkelijk, "waarde"));
    }

    /** One field of a {@code Segment} fact, read through the runtime's property bridge (the class is a runtime
     *  objecttype — no compile-time type to cast to). */
    private static Object segVeld(Object seg, String veld) { return RegelSpraakRuntime.get(seg, veld); }

    private static String renderTl(List<?> segs) {
        if (segs.isEmpty()) return "(leeg)";
        List<String> uit = new ArrayList<>();
        for (Object s : segs)
            uit.add("[" + segVeld(s, "van") + "," + segVeld(s, "tot") + ")=" + render(segVeld(s, "waarde")));
        return String.join(" ", uit);
    }

    private String kenmerk(JsonNode v, boolean aanwezigVerwacht) {
        Object bean = labelNaarBean.get(v.get("label").asText());
        Object gelezen = bean == null ? null : lees(bean, Namen.getter(v.get("characteristic").asText()));
        boolean aanwezig = Boolean.TRUE.equals(gelezen);
        return aanwezig == aanwezigVerwacht ? null
                : "kenmerk " + v.get("characteristic").asText() + ": verwacht "
                + (aanwezigVerwacht ? "aanwezig" : "afwezig") + ", werkelijk " + (aanwezig ? "aanwezig" : "afwezig");
    }

    /** The §9.5 consistency check. A {@code consistent} expectation demands NO Inconsistentie fact at all
     *  (unchanged presence semantics). An {@code inconsistent} expectation is verified against the pinned §9.5
     *  KEY (the {@code rule}) — a matching Inconsistentie fact must exist. For a DEFAULT-GROUP conflict key (ch. 7.2/7.9,
     *  the {@code Onderwerp.doel[/path]} form whose prefix is a declared objecttype, e.g. {@code Jutter.heffing/0})
     *  the pinned SUBJECT (the {@code label}) is verified too (ratified Q4) — sharpening the third leg's witness to
     *  the exact path key ref/clingo pin. For a rule-name key (a consistentie/uniciteit/verdeling violation), only
     *  the key is checked: the subject the exec-model attaches can legitimately differ from ref's (a pre-existing,
     *  presence-witnessed divergence), so demanding the label there would break currently-played goldens. An
     *  expectation carrying no rule falls back to bare presence. */
    private String consistentie(KieSession sessie, boolean inconsistentVerwacht, JsonNode v) {
        List<Object> facts = new ArrayList<>();
        try {
            Class<?> inc = Class.forName(FactModelGenerator.PAKKET + ".Inconsistentie", true, cl);
            facts.addAll(sessie.getObjects(inc::isInstance));
        } catch (ClassNotFoundException e) { /* no Inconsistentie bean -> none */ }
        if (!inconsistentVerwacht)
            return facts.isEmpty() ? null : "verwacht consistent, werkelijk inconsistent (" + renderIncs(facts) + ")";
        String rule = v.has("rule") && !v.get("rule").isNull() ? v.get("rule").asText() : null;
        String label = v.has("label") && !v.get("label").isNull() ? v.get("label").asText() : null;
        if (facts.isEmpty())
            return "verwacht inconsistent" + sleutelTekst(rule, label) + ", werkelijk consistent";
        if (rule == null) return null;   // presence-only expectation, satisfied by any fact
        boolean defaultsSleutel = isDefaultsSleutel(rule);
        for (Object f : facts) {
            if (!rule.equals(lees(f, "getRegel"))) continue;
            if (!defaultsSleutel || label == null) return null;     // rule-name key: the key match suffices
            if (label.equals(labelVanBean(lees(f, "getSubject")))) return null;
        }
        return "verwacht inconsistent" + sleutelTekst(rule, label) + ", niet gevonden onder " + renderIncs(facts);
    }

    /** Whether a §9.5 key is a ch. 7.2/7.9 default-group conflict key — the {@code Onderwerp.doel[/path]} form whose
     *  prefix before the first {@code .} is a declared objecttype. Rule-name keys (free Dutch phrases) never match. */
    private boolean isDefaultsSleutel(String rule) {
        int dot = rule.indexOf('.');
        return dot > 0 && model.objecttypeOpNaam(rule.substring(0, dot)) != null;
    }

    private static String sleutelTekst(String rule, String label) {
        return rule == null ? "" : " (" + rule + (label != null ? " @ " + label : "") + ")";
    }

    /** The case label whose bean IS this fact's subject (identity) — the reverse of {@link #labelNaarBean},
     *  so the §9.5 fact's subject can be checked against the expectation's {@code label}. null if not an input bean. */
    private String labelVanBean(Object bean) {
        if (bean == null) return null;
        for (Map.Entry<String, Object> e : labelNaarBean.entrySet()) if (e.getValue() == bean) return e.getKey();
        return null;
    }

    private String renderIncs(List<Object> facts) {
        List<String> uit = new ArrayList<>();
        for (Object f : facts) uit.add(lees(f, "getRegel") + " @ " + labelVanBean(lees(f, "getSubject")));
        return String.join(", ", uit);
    }

    // ---------------- value helpers ----------------

    private static boolean gelijk(Object verw, Object werkelijk) {
        if (verw == null) return werkelijk == null;
        if (verw instanceof BigDecimal b) return werkelijk instanceof BigDecimal w && b.compareTo(w) == 0;
        return verw.equals(werkelijk);
    }

    /** The Java value a uchar case value node denotes — the already-normalised scalar the bean setter /
     *  global takes. An input form the v0 backend does not carry (a list/struct/constructor value, a
     *  rational lexeme, a timeline) is a reason-carrying {@link Excluded}. */
    private Object waardeNaarArg(JsonNode w, String veld) {
        if (w == null || w.isNull()) return null;
        if (w.has("number")) {
            String s = w.get("number").asText();
            if (s.contains("/")) throw new Excluded("rational value on '" + veld + "' is not carried by the v0 backend");
            return new BigDecimal(s);
        }
        if (w.has("date")) return datumVanCaseWire(w.get("date").asText(), veld);
        if (w.has("dateTime")) return datumTijdVanCaseWire(w.get("dateTime").asText(), veld);
        if (w.has("boolean")) return w.get("boolean").asBoolean();
        if (w.has("text")) return w.get("text").asText();
        if (w.has("enumeration")) return w.get("enumeration").asText();
        if (w.has("timeline")) throw new Excluded("timeline value on '" + veld + "' is not carried by the v0 backend");
        throw new Excluded("value form on '" + veld + "' is not carried by the v0 backend: " + w);
    }

    /* The uchar-case-v0 {@code date} lexeme, matched WHOLE: four-two-two digit fields, digits only, no sign
     * and no extended year. It is the schema's own pattern, held here as well, for the same reason as the
     * dateTime one below — gate 4 checks case FILES, this reader checks every case date that reaches the
     * backend by any route. */
    private static final java.util.regex.Pattern CASE_DATUM =
            java.util.regex.Pattern.compile("\\d{4}-\\d{2}-\\d{2}");

    /** The calendar day a case {@code date} names — or a loud refusal naming the FORM, never a guess.
     *  <p>{@link LocalDate#parse} is laxer than the wire it reads: beside the four-digit year it also accepts
     *  the ISO forms with a SIGNED, extended year, which the schema pattern excludes and the interpreter
     *  refuses — {@code +012026-03-15} is year 12026 and {@code -0500-01-01} is year −500. Measured
     *  2026-09-10 on the three positions a date reaches this reader (a value, the case's {@code asOfDate}, a
     *  timeline segment's boundary): with such a lexeme in the {@code asOfDate} of one golden and in a
     *  segment boundary of another, this backend replayed both as PLAYED while the interpreter faulted on the
     *  same two documents. That is a divergence the differential gate cannot see, since a case evaluated at
     *  the wrong date or bucketed on the wrong grid may still satisfy every pinned expectation. Each reader
     *  holds its own fence (IR-SPEC ch. 4.1), so the fence sits here rather than only in the schema, which
     *  sees files and not the values a generator or a hand run hands this backend directly. A malformed
     *  lexeme is a document defect, not a form this backend does not carry, so it is a hard error and never
     *  an {@link Excluded}. */
    private static LocalDate datumVanCaseWire(String lexeem, String veld) {
        if (!CASE_DATUM.matcher(lexeem).matches())
            throw new IllegalArgumentException("malformed date '" + lexeem + "' on '" + veld
                    + "': the uchar-case-v0 lexeme is yyyy-mm-dd — fixed field widths, digits only,"
                    + " no sign and no extended year");
        try { return LocalDate.parse(lexeem); }
        catch (java.time.format.DateTimeParseException e) {
            throw new IllegalArgumentException("malformed date '" + lexeem + "' on '" + veld
                    + "': the field widths are right but the calendar is not (" + e.getMessage() + ")");
        }
    }

    /* The uchar-case-v0 {@code dateTime} lexeme, matched WHOLE: fixed field widths, {@code T} as the
     * separator, the seconds field always written, a fraction that is optional but EXACTLY three digits
     * (whole milliseconds). It is the schema's own pattern, held here as well — gate 4 checks case FILES,
     * this reader checks every case value that reaches the backend by any route. */
    private static final java.util.regex.Pattern CASE_DATUM_TIJD =
            java.util.regex.Pattern.compile("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(?:\\.\\d{3})?");

    /** The instant a case {@code dateTime} names — or a loud refusal naming the FORM, never a guess.
     *  <p>{@link LocalDateTime#parse} is laxer than the wire it reads: it accepts {@code …T12:00} without a
     *  seconds field, and a fraction of ANY width, which it reads POSITIONALLY — {@code .1} as a hundred
     *  milliseconds and {@code .1234} as 123.4. The shared interpreter/ASP case reader read those same three
     *  digits differently ({@code .1} as ONE millisecond, {@code .100000} as a hundred THOUSAND), and the
     *  literal reader refused them outright: one wire, three answers. Inside the schema's fence all three
     *  agree, and that fence was the only reason the divergence never showed. Each reader now holds it
     *  itself, so a case reaching a reader by another route (a generator, a hand run, a second producer) is
     *  refused rather than answered three ways. A malformed lexeme is a document defect, not a form this
     *  backend does not carry, so it is a hard error and never an {@link Excluded}. */
    private static LocalDateTime datumTijdVanCaseWire(String lexeem, String veld) {
        if (!CASE_DATUM_TIJD.matcher(lexeem).matches())
            throw new IllegalArgumentException("malformed dateTime '" + lexeem + "' on '" + veld
                    + "': the uchar-case-v0 lexeme is yyyy-mm-ddThh:mm:ss[.fff] — fixed field widths, 'T',"
                    + " the seconds field written, no zone or offset, a fraction of exactly three digits");
        try { return LocalDateTime.parse(lexeem); }
        catch (java.time.format.DateTimeParseException e) {
            throw new IllegalArgumentException("malformed dateTime '" + lexeem + "' on '" + veld
                    + "': the field widths are right but the calendar is not (" + e.getMessage() + ")");
        }
    }

    private static Class<?> argType(Datatype dt) {
        if (dt == null) return Object.class;
        if (dt.isNumeriek() || dt.isPercentage()) return BigDecimal.class;
        if (dt.isTekst() || dt instanceof Datatype.Enumeratie) return String.class;
        if (dt instanceof Datatype.Booleaan) return Boolean.class;
        if (dt instanceof Datatype.DatumInDagen) return LocalDate.class;
        if (dt instanceof Datatype.DatumTijd) return LocalDateTime.class;
        return Object.class;
    }

    private static void roep(Object bean, String methode, Class<?> type, Object waarde) {
        try { bean.getClass().getMethod(methode, type).invoke(bean, waarde); }
        catch (ReflectiveOperationException e) {
            throw new RuntimeException("kon setter " + methode + "(" + type.getSimpleName() + ") niet aanroepen op "
                    + bean.getClass().getSimpleName(), e);
        }
    }

    private static Object lees(Object bean, String getter) {
        for (String mth : new String[]{ getter, "is" + getter.substring(3) }) {
            try { return bean.getClass().getMethod(mth).invoke(bean); }
            catch (NoSuchMethodException ignore) { /* try next form */ }
            catch (ReflectiveOperationException e) { throw new RuntimeException("kon " + mth + " niet lezen", e); }
        }
        throw new IllegalStateException("geen getter " + getter + " op " + bean.getClass().getSimpleName());
    }

    private static String render(Object o) { return o == null ? "leeg" : o.toString(); }
}
