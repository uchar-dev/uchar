package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import dev.uchar.regelspraak.engine.BeanCompiler;
import dev.uchar.regelspraak.loader.IrLoader;
import dev.uchar.regelspraak.schema.FactModelGenerator;
import dev.uchar.regelspraak.transpiler.kern.Dekking;
import dev.uchar.regelspraak.transpiler.tms.TmsTranspiler;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The context value {@code evaluationYear} (the records' {@code Rekenjaar}), which {@code spec/RUNTIME-SEMANTICS.md}
 * ch. 8.2 reads as the calendar year of the session's evaluation date: a whole number without a unit, derived from
 * that date, and a fault where the session carries none. Both branches lower it through
 * {@code RegelSpraakRuntime.rsRekenjaar}; the reference interpreter and the ASP backend compute the same year from the
 * case's {@code asOfDate}, and the golden {@code jutterswet-rekenjaar} plays in all of them with the same values.
 *
 * <p>RED FIRST, measured on the claim's base (main b88a8ccc). There the replay refused every document that carried the
 * node before either branch lowered anything, in the one sentence the reference interpreter and the ASP backend also
 * refused it with: the golden came out EXCLUDE in both branches, and so did each authored row below.
 *
 * <p>The twisted rows are the positions the refusal was pinned at, now with an answer to compare. One literal of a
 * playing golden is turned into the node, the case's date is set where it had none, and the expectations are the
 * reference interpreter's answer on the twisted document. Where a branch does not lower the node, the row pins that
 * branch's refusal, so a branch that starts lowering there fails the row until the row expects its values. Since card
 * tms-omschakeling (2026-10-10) the exec-model branch derives with truth maintenance; it plays the assertion of a
 * consistency and the filter of a fold that the destructive exec-model branch refused.
 */
class EvaluationYearWitnessTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    // cwd under surefire is the module dir (backends/drools); the corpus is two levels up.
    private static final Path CORPUS = Path.of("..", "..", "corpus", "golden");

    /** The fault without an evaluation date, written out here rather than read from the runtime's constant, so a
     *  drift of the constant away from the two OCaml executors' sentence fails this witness. */
    private static final String WITHOUT_DATE =
            "the evaluationYear context value needs an evaluation date; the case carries asOfDate null";

    private static final String GETAL = "{\"node\":\"Number\",\"whole\":true,\"decimals\":0,\"sign\":\"unspecified\"}";
    private static final String DAG = "{\"node\":\"Date\",\"precision\":\"day\"}";
    private static final String REKENJAAR =
            "{\"node\":\"ContextValue\",\"name\":\"evaluationYear\",\"type\":" + GETAL + ",\"source\":{\"snippet\":\"het Rekenjaar\"}}";
    private static final String REKENDATUM =
            "{\"node\":\"ContextValue\",\"name\":\"evaluationDate\",\"type\":" + DAG + ",\"source\":{\"snippet\":\"de Rekendatum\"}}";

    /** One object type and one rule, as the RegelSpraak frontend emits the source of each row. */
    private static final String DOCUMENT = """
        { "format": "uchar-ir-v1",
          "model": { "objectTypes": [ { "node": "ObjectType", "name": "Vondst", "animate": false,
              "attributes": [
                { "node": "Attribute", "name": "meldjaar", "datatype": %1$s },
                { "node": "Attribute", "name": "ouderdom", "datatype": %1$s },
                { "node": "Attribute", "name": "meldcode", "datatype": %1$s },
                { "node": "Attribute", "name": "vondstjaar", "datatype": %1$s },
                { "node": "Attribute", "name": "peildatum", "datatype": %2$s } ],
              "characteristics": [] } ],
            "factTypes": [], "parameters": [], "domains": [], "unitSystems": [], "dimensions": [], "timelines": [],
            "dayKinds": [] },
          "rules": { "rules": [ %3$s ], "dayKindDefinitions": [], "objectCreations": [], "allocations": [],
            "consistencies": [], "uniquenesses": [], "factCreations": [], "startPointDeterminations": [], "ruleGroups": [] },
          "diagnostics": [] }
        """;

    // ---------------- the authored rows: both branches ----------------

    @Test void the_year_of_the_evaluation_date_as_rule_value_in_both_branches() throws Exception {
        // "Het meldjaar van een Vondst moet gesteld worden op het Rekenjaar." The last two sessions are one day and
        // one year apart: the calendar year of the date, not a count of days since some epoch.
        Path ir = document(valueRule());
        BothBranches.green(ir, caseDocument("\"2026-03-15\"", "meldjaar", "{\"number\":\"2026\"}"), "the context value as a rule value");
        BothBranches.green(ir, caseDocument("\"2026-12-31\"", "meldjaar", "{\"number\":\"2026\"}"), "the last day of 2026");
        BothBranches.green(ir, caseDocument("\"2027-01-01\"", "meldjaar", "{\"number\":\"2027\"}"), "the first day of 2027");
    }

    @Test void as_operand_of_a_calculation_or_a_comparison_also() throws Exception {
        // "… berekend worden als het Rekenjaar min zijn vondstjaar" and "… gesteld worden op 1 indien het Rekenjaar
        // groter is dan zijn vondstjaar": the node inside an expression and inside a condition.
        BothBranches.green(document(computeRule()), caseDocument("\"2026-03-15\"", "ouderdom", "{\"number\":\"26\"}"),
                "the context value as the operand of a subtraction");
        BothBranches.green(document(compareRule()), caseDocument("\"2026-03-15\"", "meldcode", "{\"number\":\"1\"}"),
                "the context value as a comparison operand");
    }

    @Test void without_evaluation_date_it_fails_in_both_branches() throws Exception {
        // The case expects a year, so a branch that read one from anywhere but the session would fail the row as a
        // mismatch, and one that read leeg as well: only the fault satisfies it.
        Path ir = document(valueRule());
        JsonNode c = caseDocument("null", "meldjaar", "{\"number\":\"2026\"}");
        for (boolean viaDrl : new boolean[] { false, true }) {
            String branch = viaDrl ? "drl-tekst" : "exec-model";
            RuntimeException e = assertThrows(RuntimeException.class, () -> CaseRunner.playCase(ir, c, viaDrl), branch);
            assertFalse(e instanceof CaseRunner.Excluded, branch + " faults at fire, it does not refuse the form: " + e.getMessage());
            assertTrue(chain(e).contains(WITHOUT_DATE), branch + " names the missing date: " + chain(e));
        }
    }

    @Test void the_evaluation_date_plays_beside_it_in_both_branches() throws Exception {
        // The sibling context value in the same position, read as the evaluation date.
        BothBranches.green(document(IrPath.regel("peildatum", "GELIJKSTELLING", doel("peildatum"), REKENDATUM, "null")),
                caseDocument("\"2026-03-15\"", "peildatum", "{\"date\":\"2026-03-15\"}"), "Rekendatum as a rule value still plays");
    }

    // ---------------- without the replay: the analysis lowers the node ----------------

    /** The manifest of the analysis, which both branches render; until card drl-teksttak-op-truth-maintenance the
     *  DRL-text branch had a lowering, and a manifest, of its own, and the row asked both. */
    @Test void the_analysis_lowers_the_node() throws Exception {
        for (String regel : List.of(valueRule(), computeRule(), compareRule())) {
            IrLoader.Loaded l = IrLoader.loadAll(JSON.readTree(Files.readString(document(regel))));
            String sleutel = "regel: " + l.model().resolvedRegels.get(0).naam();
            Dekking tms = coverageOf(tmsCoverage(l), sleutel);
            assertEquals(Dekking.Verdict.LOWERED, tms.verdict(), "tms — " + sleutel + ": " + tms.reden());
        }
    }

    // ---------------- the twisted corpus rows: per branch, ref's answer or the branch's refusal ----------------

    /** One twisted position. {@code waarden} patches the case's expectations by {@code label.attribute} (a JSON value,
     *  or null for leeg); {@code inconsistent} turns its {@code consistent} into {@code inconsistent}; a null refusal
     *  means both branches must play the patched case. Until card drl-teksttak-op-truth-maintenance the DRL-text
     *  branch had a refusal of its own per position, "the DRL-text branch has no defaults lowering" on the two
     *  positions in a prioritized default. */
    private record Site(String golden, String pointer, String caseDocument, String date, Map<String, String> waarden,
                        boolean inconsistent, String refusal, String description) {}

    @Test void at_the_other_sites_the_answer_of_ref_or_the_refusal_of_the_branch() throws Exception {
        List<Site> sites = List.of(
            new Site("synth2-budgetgrens-onder", "/rules/consistencies/0/versions/0/assertion/right", "case1.json",
                    "2026-03-15", Map.of(), false,
                    null, "the assertion of a consistency"),
            new Site("synth2-budgetgrens-onder", "/rules/objectCreations/0/versions/0/initialisations/0/value/right",
                    "case1.json", "2026-03-15", Map.of("W1.opvolgerrang", "{\"number\":\"2027\"}"), true,
                    null, "an initialisation of an object creation"),
            new Site("jutterswet-voorwaardedeling", "/rules/allocations/0/versions/0/condition/right", "case.json",
                    "2026-03-15", mapWithEmpty("J1.bergdeel", "J2.bergdeel"), false,
                    null, "the guard of an allocation"),
            new Site("jutterswet-dubbelverbinding", "/rules/factCreations/1/versions/0/condition/right",
                    "case1-binnen-venster.json", null, Map.of("H1.keurtal", "{\"number\":\"0\"}"), false,
                    null, "the guard of a fact creation"),
            new Site("jutterswet-drempelvangst", "/rules/rules/0/versions/0/value/filter/right", "case.json",
                    "2026-03-15", Map.of("J1.drempelgewicht", "{\"number\":\"0\"}", "J1.drempelaandeel", "{\"number\":\"0\"}"),
                    false, null, "the filter of a fold"),
            new Site("anchor/jutterswet-strandrechtketen", "/rules/prioritizedDefaults/1/defaults/1/consequence/right",
                    "case1-basis.json", "2026-03-15",
                    Map.of("W1.uitkering", "{\"number\":\"20260.00\"}", "K1.uitkeringstotaal", "{\"number\":\"20270.00\"}"),
                    false, null, "the consequence of a prioritized default"),
            // "zijn bergingsdiepte groter is dan het Rekenjaar": W1's depth of 25 is not greater than 2026, so the
            // exception for a deep wreck no longer applies and the wreck rule's grondslag maal 2, 20.00, stands (ref
            // and clingo, measured 2026-09-26). The exec branch declined the justification as a whole until
            // 2026-09-29, when a justification that is no subject-local predicate began to lower on its own; it
            // reads the context value as a rule's value does, and plays ref's answer
            new Site("jutterswet-wrakrecht", "/rules/prioritizedDefaults/1/defaults/0/justification/right",
                    "case3-eigen-uitzondering.json", "2026-03-15",
                    Map.of("W1.uitkering", "{\"number\":\"20.00\"}", "K1.uitkeringstotaal", "{\"number\":\"20.00\"}"),
                    false, null, "the justification of a prioritized default"));
        for (Site p : sites) {
            Path ir = twisted(p.golden(), p.pointer());
            JsonNode c = caseOf(p);
            String where = p.description() + " (" + p.golden() + p.pointer() + ")";
            for (DroolsBackend.Branch branch : DroolsBackend.Branch.values())
                branch(ir, c, branch, p.refusal(), branch + " — " + where);
        }
    }

    // ---------------- fixtures ----------------

    private static void branch(Path ir, JsonNode c, DroolsBackend.Branch branch, String refusal, String message)
            throws Exception {
        if (refusal == null) {
            assertNull(CaseRunner.playCase(ir, c, branch), message);
            return;
        }
        CaseRunner.Excluded e = assertThrows(CaseRunner.Excluded.class, () -> CaseRunner.playCase(ir, c, branch), message);
        assertTrue(e.getMessage().contains(refusal), message + " names '" + refusal + "': " + e.getMessage());
    }

    private static Map<String, String> mapWithEmpty(String... keyNames) {
        java.util.Map<String, String> m = new java.util.HashMap<>();
        for (String s : keyNames) m.put(s, null);
        return m;
    }

    /** The golden's case with the row's date set and its expectations patched to the reference interpreter's answer.
     *  Every patched key must hit an expectation, so a row cannot pass on a key that names nothing. */
    private static JsonNode caseOf(Site p) throws Exception {
        ObjectNode c = (ObjectNode) JSON.readTree(Files.readString(CORPUS.resolve(p.golden()).resolve(p.caseDocument())));
        if (p.date() != null) c.put("asOfDate", p.date());
        java.util.Set<String> touched = new java.util.HashSet<>();
        ArrayNode verwacht = JSON.createArrayNode();
        for (JsonNode e : c.get("expected")) {
            String soort = e.path("kind").asText();
            String sleutel = e.path("label").asText() + "." + e.path("attribute").asText();
            if ((soort.equals("attributeValue") || soort.equals("attributeEmpty")) && p.waarden().containsKey(sleutel)) {
                touched.add(sleutel);
                ObjectNode n = JSON.createObjectNode();
                String w = p.waarden().get(sleutel);
                n.put("kind", w == null ? "attributeEmpty" : "attributeValue");
                n.put("label", e.path("label").asText());
                n.put("attribute", e.path("attribute").asText());
                if (w != null) n.set("value", JSON.readTree(w));
                verwacht.add(n);
            } else if (soort.equals("consistent") && p.inconsistent()) {
                verwacht.add(JSON.createObjectNode().put("kind", "inconsistent"));
            } else {
                verwacht.add(e);
            }
        }
        assertEquals(p.waarden().keySet(), touched, "every patched expectation exists in " + p.golden() + "/" + p.caseDocument());
        c.set("expected", verwacht);
        return c;
    }

    private static String valueRule() {
        return IrPath.regel("meldjaar", "GELIJKSTELLING", doel("meldjaar"), REKENJAAR, "null");
    }

    private static String computeRule() {
        return IrPath.regel("ouderdom", "BEREKENING", doel("ouderdom"),
                "{\"node\":\"Binary\",\"op\":\"minus\",\"left\":" + REKENJAAR + ",\"right\":" + lees("vondstjaar")
                        + ",\"type\":" + GETAL + ",\"source\":{\"snippet\":\"het Rekenjaar min zijn vondstjaar\"}}", "null");
    }

    private static String compareRule() {
        return IrPath.regel("meldcode", "GELIJKSTELLING", doel("meldcode"), IrPath.literal("1", GETAL, "1"),
                "{\"node\":\"Comparison\",\"op\":\"greater\",\"left\":" + REKENJAAR + ",\"right\":" + lees("vondstjaar")
                        + ",\"source\":{\"snippet\":\"het Rekenjaar groter is dan zijn vondstjaar\"}}");
    }

    private static String doel(String attribuut) {
        return "{\"node\":\"AttributeReference\",\"attribute\":\"" + attribuut + "\",\"subjectType\":\"Vondst\","
                + "\"source\":{\"snippet\":\"de " + attribuut + " van een Vondst\"}}";
    }

    private static String lees(String attribuut) {
        return "{\"node\":\"AttributeReference\",\"attribute\":\"" + attribuut + "\",\"subjectType\":\"Vondst\","
                + "\"source\":{\"snippet\":\"zijn " + attribuut + "\"}}";
    }

    private static Path document(String regel) throws Exception {
        return temporary("rekenjaar", (ObjectNode) JSON.readTree(DOCUMENT.formatted(GETAL, DAG, regel)));
    }

    /** One Vondst found in 2000, in a session on {@code asOf} (a JSON date string, or {@code null}). */
    private static JsonNode caseDocument(String asOf, String attribuut, String waarde) throws Exception {
        return JSON.readTree("""
            { "format": "uchar-case-v0", "asOfDate": %s,
              "input": { "instances": [ { "label": "V1", "objectType": "Vondst",
                  "attributes": { "vondstjaar": { "number": "2000" } }, "roles": {}, "characteristics": [] } ],
                "parameters": {} },
              "expected": [ { "kind": "attributeValue", "label": "V1", "attribute": "%s", "value": %s } ] }
            """.formatted(asOf, attribuut, waarde));
    }

    /** The golden with the node at {@code pointer} replaced by evaluationYear under the replaced node's own type. The node
     *  there must be a Literal: a twist that hit anything else would test a document other than the one described. */
    private static Path twisted(String golden, String pointer) throws Exception {
        ObjectNode ir = (ObjectNode) JSON.readTree(Files.readString(CORPUS.resolve(golden).resolve("ir.json")));
        JsonPointer p = JsonPointer.compile(pointer);
        JsonNode oud = ir.at(p);
        assertEquals("Literal", oud.path("node").asText(), "the twist hits a literal: " + golden + pointer);
        ObjectNode recomputed = (ObjectNode) JSON.readTree(REKENJAAR);
        recomputed.set("type", oud.get("type"));
        ((ObjectNode) ir.at(p.head())).set(p.last().getMatchingProperty(), recomputed);
        return temporary("rekenjaar-twist", ir);
    }

    /** The whole cause chain as one line: a fault at fire reaches the caller wrapped by Drools. */
    private static String chain(Throwable t) {
        StringBuilder sb = new StringBuilder();
        for (Throwable c = t; c != null; c = c.getCause() == c ? null : c.getCause())
            sb.append(sb.length() > 0 ? " <- " : "").append(c.getClass().getSimpleName()).append(": ").append(c.getMessage());
        return sb.toString();
    }

    private static Dekking coverageOf(List<Dekking> dekking, String sleutel) {
        for (Dekking d : dekking) if (sleutel.equals(d.sleutel())) return d;
        throw new AssertionError("no manifest entry " + sleutel + " in " + dekking);
    }

    private static List<Dekking> tmsCoverage(IrLoader.Loaded l) {
        ClassLoader cl = BeanCompiler.compile(FactModelGenerator.genereer(l.model()));
        ClassLoader previous = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(cl);
        try {
            return TmsTranspiler.build(l.model(), cl, l.prioritizedDefaults(), l.roleBindings(), l.lets(),
                    l.sourcePaths(), l.path()).dekking();
        } finally {
            Thread.currentThread().setContextClassLoader(previous);
        }
    }

    private static Path temporary(String naam, ObjectNode ir) throws Exception {
        Path tmp = Files.createTempFile(naam, ".json");
        tmp.toFile().deleteOnExit();
        Files.writeString(tmp, ir.toString());
        return tmp;
    }
}
