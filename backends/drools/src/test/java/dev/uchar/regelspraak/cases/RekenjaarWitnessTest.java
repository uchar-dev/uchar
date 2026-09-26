package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import dev.uchar.regelspraak.engine.BeanCompiler;
import dev.uchar.regelspraak.loader.IrLoader;
import dev.uchar.regelspraak.schema.FactModelGenerator;
import dev.uchar.regelspraak.transpiler.DrlTranspiler;
import dev.uchar.regelspraak.transpiler.kern.Dekking;
import dev.uchar.regelspraak.transpiler.model.ExecModelTranspiler;

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
 * branch's refusal, so a branch that starts lowering there fails the row until the row expects its values.
 */
class RekenjaarWitnessTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    // cwd under surefire is the module dir (backends/drools); the corpus is two levels up.
    private static final Path CORPUS = Path.of("..", "..", "corpus", "golden");

    /** The fault without an evaluation date, written out here rather than read from the runtime's constant, so a
     *  drift of the constant away from the two OCaml executors' sentence fails this witness. */
    private static final String ZONDER_DATUM =
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

    @Test void als_regelwaarde_het_jaar_van_de_rekendatum_in_beide_takken() throws Exception {
        // "Het meldjaar van een Vondst moet gesteld worden op het Rekenjaar." The last two sessions are one day and
        // one year apart: the calendar year of the date, not a count of days since some epoch.
        Path ir = document(waardeRegel());
        BeideTakken.groen(ir, zaak("\"2026-03-15\"", "meldjaar", "{\"number\":\"2026\"}"), "the context value as a rule value");
        BeideTakken.groen(ir, zaak("\"2026-12-31\"", "meldjaar", "{\"number\":\"2026\"}"), "the last day of 2026");
        BeideTakken.groen(ir, zaak("\"2027-01-01\"", "meldjaar", "{\"number\":\"2027\"}"), "the first day of 2027");
    }

    @Test void als_operand_van_een_berekening_of_een_vergelijking_ook() throws Exception {
        // "… berekend worden als het Rekenjaar min zijn vondstjaar" and "… gesteld worden op 1 indien het Rekenjaar
        // groter is dan zijn vondstjaar": the node inside an expression and inside a condition.
        BeideTakken.groen(document(rekenRegel()), zaak("\"2026-03-15\"", "ouderdom", "{\"number\":\"26\"}"),
                "the context value as the operand of a subtraction");
        BeideTakken.groen(document(vergelijkRegel()), zaak("\"2026-03-15\"", "meldcode", "{\"number\":\"1\"}"),
                "the context value as a comparison operand");
    }

    @Test void zonder_rekendatum_faalt_het_in_beide_takken() throws Exception {
        // The case expects a year, so a branch that read one from anywhere but the session would fail the row as a
        // mismatch, and one that read leeg as well: only the fault satisfies it.
        Path ir = document(waardeRegel());
        JsonNode c = zaak("null", "meldjaar", "{\"number\":\"2026\"}");
        for (boolean viaDrl : new boolean[] { false, true }) {
            String tak = viaDrl ? "drl-tekst" : "exec-model";
            RuntimeException e = assertThrows(RuntimeException.class, () -> CaseRunner.speelCase(ir, c, viaDrl), tak);
            assertFalse(e instanceof CaseRunner.Excluded, tak + " faults at fire, it does not refuse the form: " + e.getMessage());
            assertTrue(keten(e).contains(ZONDER_DATUM), tak + " names the missing date: " + keten(e));
        }
    }

    @Test void de_rekendatum_speelt_ernaast_in_beide_takken() throws Exception {
        // The sibling context value in the same position, read as the evaluation date.
        BeideTakken.groen(document(IrPad.regel("peildatum", "GELIJKSTELLING", doel("peildatum"), REKENDATUM, "null")),
                zaak("\"2026-03-15\"", "peildatum", "{\"date\":\"2026-03-15\"}"), "Rekendatum as a rule value still plays");
    }

    // ---------------- without the replay: each transpiler lowers the node ----------------

    @Test void beide_transpilers_verlagen_de_knoop() throws Exception {
        for (String regel : List.of(waardeRegel(), rekenRegel(), vergelijkRegel())) {
            IrLoader.Loaded l = IrLoader.loadAll(JSON.readTree(Files.readString(document(regel))));
            String sleutel = "regel: " + l.model().resolvedRegels.get(0).naam();
            Dekking drl = dekkingVan(DrlTranspiler.build(l.model(), l.lets(), l.prioritizedDefaults(), l.bronPaden(),
                    l.rolBindingen(), l.navigatieBindingen()).dekking(), sleutel);
            assertEquals(Dekking.Verdict.LOWERED, drl.verdict(), "drl-tekst — " + sleutel + ": " + drl.reden());
            Dekking exec = dekkingVan(execDekking(l), sleutel);
            assertEquals(Dekking.Verdict.LOWERED, exec.verdict(), "exec-model — " + sleutel + ": " + exec.reden());
        }
    }

    // ---------------- the twisted corpus rows: per branch, ref's answer or the branch's refusal ----------------

    /** One twisted position. {@code waarden} patches the case's expectations by {@code label.attribute} (a JSON value,
     *  or null for leeg); {@code inconsistent} turns its {@code consistent} into {@code inconsistent}; a null refusal
     *  means the branch must play the patched case. */
    private record Plek(String golden, String pointer, String zaak, String datum, Map<String, String> waarden,
                        boolean inconsistent, String execWeigering, String drlWeigering, String omschrijving) {}

    @Test void op_de_andere_plekken_het_antwoord_van_ref_of_de_weigering_van_de_tak() throws Exception {
        List<Plek> plekken = List.of(
            new Plek("synth2-budgetgrens-onder", "/rules/consistencies/0/versions/0/assertion/right", "case1.json",
                    "2026-03-15", Map.of(), false,
                    "schending buiten Phase-D scope", null, "the assertion of a consistency"),
            new Plek("synth2-budgetgrens-onder", "/rules/objectCreations/0/versions/0/initialisations/0/value/right",
                    "case1.json", "2026-03-15", Map.of("W1.opvolgerrang", "{\"number\":\"2027\"}"), true,
                    null, null, "an initialisation of an object creation"),
            new Plek("jutterswet-voorwaardedeling", "/rules/allocations/0/versions/0/condition/right", "case.json",
                    "2026-03-15", mapMetLeeg("J1.bergdeel", "J2.bergdeel"), false,
                    null, null, "the guard of an allocation"),
            new Plek("jutterswet-dubbelverbinding", "/rules/factCreations/1/versions/0/condition/right",
                    "case1-binnen-venster.json", null, Map.of("H1.keurtal", "{\"number\":\"0\"}"), false,
                    null, null, "the guard of a fact creation"),
            new Plek("jutterswet-drempelvangst", "/rules/rules/0/versions/0/value/filter/right", "case.json",
                    "2026-03-15", Map.of("J1.drempelgewicht", "{\"number\":\"0\"}", "J1.drempelaandeel", "{\"number\":\"0\"}"),
                    false, "aggregatie buiten Phase-C scope", null, "the filter of a fold"),
            new Plek("anchor/jutterswet-strandrechtketen", "/rules/prioritizedDefaults/1/defaults/1/consequence/right",
                    "case1-basis.json", "2026-03-15",
                    Map.of("W1.uitkering", "{\"number\":\"20260.00\"}", "K1.uitkeringstotaal", "{\"number\":\"20270.00\"}"),
                    false, null, "the DRL-text branch has no defaults lowering", "the consequence of a prioritized default"));
        for (Plek p : plekken) {
            Path ir = getwist(p.golden(), p.pointer());
            JsonNode c = zaakVan(p);
            String waar = p.omschrijving() + " (" + p.golden() + p.pointer() + ")";
            tak(ir, c, false, p.execWeigering(), "exec-model — " + waar);
            tak(ir, c, true, p.drlWeigering(), "drl-tekst — " + waar);
        }
    }

    // ---------------- fixtures ----------------

    private static void tak(Path ir, JsonNode c, boolean viaDrl, String weigering, String boodschap) throws Exception {
        if (weigering == null) {
            assertNull(CaseRunner.speelCase(ir, c, viaDrl), boodschap);
            return;
        }
        CaseRunner.Excluded e = assertThrows(CaseRunner.Excluded.class, () -> CaseRunner.speelCase(ir, c, viaDrl), boodschap);
        assertTrue(e.getMessage().contains(weigering), boodschap + " names '" + weigering + "': " + e.getMessage());
    }

    private static Map<String, String> mapMetLeeg(String... sleutels) {
        java.util.Map<String, String> m = new java.util.HashMap<>();
        for (String s : sleutels) m.put(s, null);
        return m;
    }

    /** The golden's case with the row's date set and its expectations patched to the reference interpreter's answer.
     *  Every patched key must hit an expectation, so a row cannot pass on a key that names nothing. */
    private static JsonNode zaakVan(Plek p) throws Exception {
        ObjectNode c = (ObjectNode) JSON.readTree(Files.readString(CORPUS.resolve(p.golden()).resolve(p.zaak())));
        if (p.datum() != null) c.put("asOfDate", p.datum());
        java.util.Set<String> geraakt = new java.util.HashSet<>();
        ArrayNode verwacht = JSON.createArrayNode();
        for (JsonNode e : c.get("expected")) {
            String soort = e.path("kind").asText();
            String sleutel = e.path("label").asText() + "." + e.path("attribute").asText();
            if ((soort.equals("attributeValue") || soort.equals("attributeEmpty")) && p.waarden().containsKey(sleutel)) {
                geraakt.add(sleutel);
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
        assertEquals(p.waarden().keySet(), geraakt, "every patched expectation exists in " + p.golden() + "/" + p.zaak());
        c.set("expected", verwacht);
        return c;
    }

    private static String waardeRegel() {
        return IrPad.regel("meldjaar", "GELIJKSTELLING", doel("meldjaar"), REKENJAAR, "null");
    }

    private static String rekenRegel() {
        return IrPad.regel("ouderdom", "BEREKENING", doel("ouderdom"),
                "{\"node\":\"Binary\",\"op\":\"minus\",\"left\":" + REKENJAAR + ",\"right\":" + lees("vondstjaar")
                        + ",\"type\":" + GETAL + ",\"source\":{\"snippet\":\"het Rekenjaar min zijn vondstjaar\"}}", "null");
    }

    private static String vergelijkRegel() {
        return IrPad.regel("meldcode", "GELIJKSTELLING", doel("meldcode"), IrPad.literal("1", GETAL, "1"),
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
        return tijdelijk("rekenjaar", (ObjectNode) JSON.readTree(DOCUMENT.formatted(GETAL, DAG, regel)));
    }

    /** One Vondst found in 2000, in a session on {@code asOf} (a JSON date string, or {@code null}). */
    private static JsonNode zaak(String asOf, String attribuut, String waarde) throws Exception {
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
    private static Path getwist(String golden, String pointer) throws Exception {
        ObjectNode ir = (ObjectNode) JSON.readTree(Files.readString(CORPUS.resolve(golden).resolve("ir.json")));
        JsonPointer p = JsonPointer.compile(pointer);
        JsonNode oud = ir.at(p);
        assertEquals("Literal", oud.path("node").asText(), "the twist hits a literal: " + golden + pointer);
        ObjectNode nieuw = (ObjectNode) JSON.readTree(REKENJAAR);
        nieuw.set("type", oud.get("type"));
        ((ObjectNode) ir.at(p.head())).set(p.last().getMatchingProperty(), nieuw);
        return tijdelijk("rekenjaar-twist", ir);
    }

    /** The whole cause chain as one line: a fault at fire reaches the caller wrapped by Drools. */
    private static String keten(Throwable t) {
        StringBuilder sb = new StringBuilder();
        for (Throwable c = t; c != null; c = c.getCause() == c ? null : c.getCause())
            sb.append(sb.length() > 0 ? " <- " : "").append(c.getClass().getSimpleName()).append(": ").append(c.getMessage());
        return sb.toString();
    }

    private static Dekking dekkingVan(List<Dekking> dekking, String sleutel) {
        for (Dekking d : dekking) if (sleutel.equals(d.sleutel())) return d;
        throw new AssertionError("no manifest entry " + sleutel + " in " + dekking);
    }

    private static List<Dekking> execDekking(IrLoader.Loaded l) {
        ClassLoader cl = BeanCompiler.compileer(FactModelGenerator.genereer(l.model()));
        ClassLoader vorige = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(cl);
        try {
            return ExecModelTranspiler.build(l.model(), cl, l.prioritizedDefaults(), l.rolBindingen(), l.lets(),
                    l.bronPaden(), l.navigatieBindingen()).dekking();
        } finally {
            Thread.currentThread().setContextClassLoader(vorige);
        }
    }

    private static Path tijdelijk(String naam, ObjectNode ir) throws Exception {
        Path tmp = Files.createTempFile(naam, ".json");
        tmp.toFile().deleteOnExit();
        Files.writeString(tmp, ir.toString());
        return tmp;
    }
}
