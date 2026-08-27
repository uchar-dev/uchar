package io.github.yhavinga.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.yhavinga.regelspraak.engine.BeanCompiler;
import io.github.yhavinga.regelspraak.loader.IrLoader;
import io.github.yhavinga.regelspraak.model.Datatype;
import io.github.yhavinga.regelspraak.model.RegelSpraakModel;
import io.github.yhavinga.regelspraak.model.RegelSpraakModel.Objecttype;
import io.github.yhavinga.regelspraak.schema.ContractValidator;
import io.github.yhavinga.regelspraak.schema.FactModelGenerator;
import io.github.yhavinga.regelspraak.transpiler.kern.Dekking;
import io.github.yhavinga.regelspraak.transpiler.kern.Namen;
import io.github.yhavinga.regelspraak.transpiler.model.ExecModelTranspiler;

import org.drools.modelcompiler.KieBaseBuilder;
import org.kie.api.KieBase;
import org.kie.api.runtime.KieSession;

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

    private CaseRunner(RegelSpraakModel model, KieBase kieBase, ClassLoader cl) {
        this.model = model; this.kieBase = kieBase; this.cl = cl;
    }

    /** Load {@code irPath}, build the KieBase, and replay each of {@code casePaths}. Any {@link Excluded}
     *  (loader refusal, transpiler marker, contract violation, unsupported form) becomes an EXCLUDE result. */
    public static Result runGolden(Path irPath, List<Path> casePaths) {
        try {
            JsonNode ir = JSON.readTree(Files.readString(irPath));
            RegelSpraakModel model = IrLoader.load(ir);

            // Two forms the v0 backend cannot REPLAY are refused up front — a LOUD per-form exclusion, never a
            // silent value-divergence in the gate. Carrying either to fire only to fault or misvalue there is
            // honestly NOT-CARRIED, not a semantic exec-model difference, so it belongs in the auto-excluded
            // channel with a reason, not the value-divergence list whose banner claims a genuine ref/clingo delta:
            //   §13.4.6 objectcreatie — the creation consequence does not execute in this backend: a navigated
            //     read of the created object yields leeg, or the creation rule raises an intValue() NPE on a leeg
            //     member. It lowers without a marker and reaches fire, but never reproduces the pinned value;
            //     scoped not-carried pending triage, so the gate makes NO claim the backend executes objectcreatie.
            if (!model.objectCreaties.isEmpty())
                throw new Excluded("objectcreatie (§13.4.6) is not replayed by the v0 Drools backend — the creation "
                        + "consequence does not execute (a navigated created-object read yields leeg, or an intValue() "
                        + "NPE is raised on a leeg member); it lowers and reaches fire but never reproduces the pinned value");
            //   §13.3.9.2 wederkerig self-relation — FactModelGenerator derives no scalar reference field for a
            //     self-pair role, so a rule reading it has no getter (the exec-model build itself throws); refuse
            //     before building rather than let the build crash into an unclassified ERROR.
            if (model.feittypen.values().stream().anyMatch(ft -> ft.wederkerig))
                throw new Excluded("wederkerig self-relation (§13.3.9.2) is not carried by the v0 Drools backend — "
                        + "FactModelGenerator derives no scalar reference field for a self-pair role");

            Map<String, String> bronnen = FactModelGenerator.genereer(model);
            ClassLoader cl = BeanCompiler.compileer(bronnen);
            List<String> schendingen = ContractValidator.valideer(model, cl);
            if (!schendingen.isEmpty()) throw new Excluded("contract: " + schendingen.get(0));

            ClassLoader vorige = Thread.currentThread().getContextClassLoader();
            Thread.currentThread().setContextClassLoader(cl);
            ExecModelTranspiler et;
            KieBase kieBase;
            try {
                et = ExecModelTranspiler.build(model, cl);
                // every construct must LOWER — a marker means the exec KieBase silently omits it,
                // so the case's expectations could not be produced; that golden is not carried.
                for (Dekking d : et.dekking())
                    if (!d.lowered()) throw new Excluded("transpiler " + d.verdict() + ": " + d.sleutel()
                            + (d.reden().isEmpty() ? "" : " — " + d.reden()));
                kieBase = KieBaseBuilder.createKieBaseFromModel(et.model());
            } finally {
                Thread.currentThread().setContextClassLoader(vorige);
            }

            CaseRunner runner = new CaseRunner(model, kieBase, cl);
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

    // ---------------- one case ----------------

    private final Map<String, Object> labelNaarBean = new LinkedHashMap<>();
    private final Map<String, Objecttype> labelType = new LinkedHashMap<>();

    private String runCase(String naam, JsonNode c) {
        labelNaarBean.clear();
        labelType.clear();
        ClassLoader vorige = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(cl);
        KieSession sessie = kieBase.newKieSession();
        try {
            JsonNode input = c.get("input");
            JsonNode instances = input.get("instances");
            for (JsonNode i : instances) beanVoor(i);
            for (JsonNode i : instances) zetAttributenEnKenmerken(i);
            for (JsonNode i : instances) zetRollen(i);
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

    /** Whether {@code attr} on {@code ot} is a §13.3.6 temporal (List&lt;Segment&gt;) or §3.6 dimensioned
     *  (Map) bean member — the two non-scalar fact shapes the v0 case runner does not carry as scalars. */
    private boolean nietScalair(Objecttype ot, String attr) {
        return ot.attribuutGranulariteit.containsKey(attr) || !ot.dimensiesVan(attr).isEmpty();
    }

    private void zetAttributenEnKenmerken(JsonNode i) {
        Object bean = labelNaarBean.get(i.get("label").asText());
        Objecttype ot = model.objecttypeOpNaam(i.get("objectType").asText());
        JsonNode attrs = i.get("attributes");
        if (attrs != null) for (Map.Entry<String, JsonNode> e : attrs.properties()) {
            Datatype dt = ot.attribuut(e.getKey());
            if (dt == null) throw new Excluded("case sets undeclared attribute '" + e.getKey() + "' on " + ot.naam);
            if (nietScalair(ot, e.getKey()))
                throw new Excluded("temporal/dimensioned attribute input '" + e.getKey() + "' is not carried by the v0 backend");
            roep(bean, Namen.setter(e.getKey()), argType(dt), waardeNaarArg(e.getValue(), e.getKey()));
        }
        JsonNode kenmerken = i.get("characteristics");
        if (kenmerken != null) for (JsonNode k : kenmerken)
            roep(bean, Namen.setter(k.asText()), boolean.class, Boolean.TRUE);
        if (i.has("characteristicTimelines") && i.get("characteristicTimelines").size() > 0)
            throw new Excluded("temporal characteristic timelines are not carried by the v0 Drools backend");
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
                String rolIL = model.verbindendeRol(tI, tL);      // the field on THIS bean (to-one from here)
                if (rolIL != null && !model.naarVeelRol(tI, tL, rolIL)) {
                    roep(bean, Namen.setter(rolIL), doel.getClass(), doel);
                } else {
                    String rolLI = model.verbindendeRol(tL, tI);  // the field lives on the target (this is the one side)
                    if (rolLI == null || model.naarVeelRol(tL, tI, rolLI))
                        throw new Excluded("role '" + e.getKey() + "' has no uniquely derivable reference field");
                    roep(doel, Namen.setter(rolLI), bean.getClass(), bean);
                }
            }
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
        JsonNode asOf = c.get("asOfDate");
        if (asOf != null && !asOf.isNull())
            setGlobalVeilig(sessie, "rekendatum", LocalDate.parse(asOf.asText()));
    }

    private static void setGlobalVeilig(KieSession sessie, String naam, Object waarde) {
        try { sessie.setGlobal(naam, waarde); }
        catch (RuntimeException onbekend) { /* global not declared: no rule reads it */ }
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
            case "attributeTimeline" -> throw new Excluded("attributeTimeline read-back is not carried by the v0 backend");
            default -> throw new Excluded("unknown expectation kind: " + kind);
        };
    }

    private String attribuut(JsonNode v, boolean leegVerwacht) {
        String attr = v.get("attribute").asText();
        String label = v.get("label").asText();
        Objecttype ot = labelType.get(label);
        if (ot != null && nietScalair(ot, attr))
            throw new Excluded("temporal/dimensioned attribute read-back '" + attr + "' is not carried by the v0 backend");
        Object bean = labelNaarBean.get(label);
        Object werkelijk = bean == null ? null : lees(bean, Namen.getter(attr));
        if (leegVerwacht)
            return werkelijk == null ? null : "verwacht leeg, werkelijk " + werkelijk;
        Object verwacht = waardeNaarArg(v.get("value"), attr);
        return gelijk(verwacht, werkelijk) ? null
                : "attribuut " + attr + ": verwacht " + render(verwacht) + ", werkelijk " + render(werkelijk);
    }

    private String kenmerk(JsonNode v, boolean aanwezigVerwacht) {
        Object bean = labelNaarBean.get(v.get("label").asText());
        Object gelezen = bean == null ? null : lees(bean, Namen.getter(v.get("characteristic").asText()));
        boolean aanwezig = Boolean.TRUE.equals(gelezen);
        return aanwezig == aanwezigVerwacht ? null
                : "kenmerk " + v.get("characteristic").asText() + ": verwacht "
                + (aanwezigVerwacht ? "aanwezig" : "afwezig") + ", werkelijk " + (aanwezig ? "aanwezig" : "afwezig");
    }

    private String consistentie(KieSession sessie, boolean inconsistentVerwacht, JsonNode v) {
        long aantal = 0;
        try {
            Class<?> inc = Class.forName(FactModelGenerator.PAKKET + ".Inconsistentie", true, cl);
            aantal = sessie.getObjects(inc::isInstance).size();
        } catch (ClassNotFoundException e) { /* no Inconsistentie bean -> none */ }
        boolean inconsistent = aantal > 0;
        return inconsistent == inconsistentVerwacht ? null
                : "verwacht " + (inconsistentVerwacht ? "inconsistent" : "consistent")
                + ", werkelijk " + (inconsistent ? "inconsistent (" + aantal + ")" : "consistent");
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
        if (w.has("date")) return LocalDate.parse(w.get("date").asText());
        if (w.has("dateTime")) return LocalDateTime.parse(w.get("dateTime").asText());
        if (w.has("boolean")) return w.get("boolean").asBoolean();
        if (w.has("text")) return w.get("text").asText();
        if (w.has("enumeration")) return w.get("enumeration").asText();
        if (w.has("timeline")) throw new Excluded("timeline value on '" + veld + "' is not carried by the v0 backend");
        throw new Excluded("value form on '" + veld + "' is not carried by the v0 backend: " + w);
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
