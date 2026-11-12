package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The case reader of the replay held to the reference interpreter's case reader ({@code ref/lib/case.ml}): a case that
 * reader refuses at load is a case error here as well (an {@link IllegalArgumentException}), and a case it reads is read
 * here to the same input. Every row takes a corpus golden and changes one thing in one of its case files.
 */
class CaseReaderWitnessTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    // cwd under surefire is the module dir (backends/drools); the corpus is two levels up.
    private static final Path CORPUS = Path.of("..", "..", "corpus", "golden");

    /** Two jutters and the parameter vondstdrempel 12: J1 (14 vondsten) is penninggerechtigd, J2 (3) is not. */
    private static final String PENNINGDREMPEL = "jutterswet-penningdrempel";
    /** Vondst.staat, an enumeration. */
    private static final String KEURSTAAT = "jutterswet-keurstaat";
    /** B1.onderhoudstarief, an input on a year timeline; B1.onderhoudsbijdrage, an expected timeline. */
    private static final String BAKENMEESTER = "jutterswet-bakenmeester";

    // ---------------- parameters ----------------

    @Test void a_parameter_written_in_another_spelling_binds_its_declaration() throws Exception {
        ObjectNode c = caseFile(PENNINGDREMPEL, "case");
        parameters(c).removeAll();
        parameters(c).set("vondstDrempel", number("12"));
        assertNull(play(PENNINGDREMPEL, c), "the threshold binds under its declared name, so J1 is penninggerechtigd");
    }

    @Test void an_undeclared_parameter_is_a_case_error() throws Exception {
        ObjectNode c = caseFile(PENNINGDREMPEL, "case");
        parameters(c).set("onbekendeparameter", number("1"));
        caseError(PENNINGDREMPEL, c, "not declared");
    }

    @Test void a_parameter_value_of_another_datatype_is_a_case_error() throws Exception {
        for (String variant : new String[]{"text", "date"}) {
            ObjectNode c = caseFile(PENNINGDREMPEL, "case");
            parameters(c).set("vondstdrempel", JSON.createObjectNode().put(variant, "2024-01-01"));
            caseError(PENNINGDREMPEL, c, "value variant \"" + variant + "\" does not match the declared datatype");
        }
    }

    // ---------------- values against their datatype ----------------

    @Test void an_input_value_the_datatype_does_not_hold_is_a_case_error() throws Exception {
        String[][] rows = {
                {"{\"text\": \"14\"}", "value variant \"text\" does not match the declared datatype"},
                {"{\"boolean\": true}", "value variant \"boolean\" does not match the declared datatype"},
                {"{\"number\": \"1E1\"}", "is not the uchar-case-v0 number lexeme"},
                {"{\"number\": \"+14\"}", "is not the uchar-case-v0 number lexeme"},
                {"{\"number\": \"14\", \"unit\": \"kg\"}", "expected one value variant"},
                {"null", "expected an object, got null"},
        };
        for (String[] row : rows) {
            ObjectNode c = caseFile(PENNINGDREMPEL, "case");
            attributes(c, 0).set("aantal aangemelde vondsten", JSON.readTree(row[0]));
            caseError(PENNINGDREMPEL, c, row[1]);
        }
    }

    @Test void an_expected_value_the_datatype_does_not_hold_is_a_case_error() throws Exception {
        String[][] rows = {
                {"{\"value\": {\"number\": \"14\", \"text\": \"x\"}}", "expected one value variant"},
                {"{}", "expected an object, got null"},
                {"{\"value\": {\"text\": \"14\"}}", "value variant \"text\" does not match the declared datatype"},
        };
        for (String[] row : rows) {
            ObjectNode c = caseFile(PENNINGDREMPEL, "case");
            ObjectNode v = (ObjectNode) JSON.readTree(row[0]);
            v.put("kind", "attributeValue").put("label", "J1").put("attribute", "aantal aangemelde vondsten");
            c.putArray("expected").add(v);
            caseError(PENNINGDREMPEL, c, row[1]);
        }
    }

    @Test void an_enumeration_value_is_a_declared_label() throws Exception {
        // keurstaat: Vondst.staat is an enumeration; an undeclared label and a text are refused, in the input and in
        // an expectation alike
        ObjectNode c = caseFile(KEURSTAAT, "case1-gaaf");
        ObjectNode staat = attributes(c, 0);
        String attribute = staat.fieldNames().next();
        staat.set(attribute, JSON.createObjectNode().put("enumeration", "onzin"));
        caseError(KEURSTAAT, c, "\"onzin\" is not a declared enumeration value");
        staat.set(attribute, JSON.createObjectNode().put("text", "beschadigd"));
        caseError(KEURSTAAT, c, "value variant \"text\" does not match the declared datatype");
    }

    @Test void a_segment_without_a_value_is_a_case_error() throws Exception {
        // bakenmeester: B1.onderhoudstarief is the input on a timeline, B1.onderhoudsbijdrage the expected one
        for (String segment : new String[]{"{\"from\": \"2025-01-01\", \"to\": \"2026-01-01\"}",
                "{\"from\": \"2025-01-01\", \"to\": \"2026-01-01\", \"value\": null}"}) {
            ObjectNode c = caseFile(BAKENMEESTER, "case1");
            ObjectNode tarief = (ObjectNode) attributes(c, 0).get("onderhoudstarief");
            ((ArrayNode) tarief.get("timeline")).set(0, JSON.readTree(segment));
            caseError(BAKENMEESTER, c, "B1.onderhoudstarief.timeline[0]");
        }
        ObjectNode c = caseFile(BAKENMEESTER, "case1");
        for (JsonNode v : c.get("expected"))
            if (v.path("kind").asText().equals("attributeTimeline"))
                ((ObjectNode) v.get("timeline").get(0)).remove("value");
        caseError(BAKENMEESTER, c, "missing field \"value\"");
    }

    @Test void a_parameter_the_case_does_not_bind_faults_where_a_rule_reads_it() {
        // case1 leaves aanvoertarief unbound, read in a value; case2 aanvoerdrempel, read in a condition
        Path red = Path.of("..", "..", "corpus", "red", "aanvoergeld-parameter-unbound");
        Map<String, String> unbound = Map.of("case1-tarief-ongebonden", "aanvoertarief",
                "case2-drempel-ongebonden", "aanvoerdrempel");
        for (DroolsBackend.Branch branch : DroolsBackend.Branch.values())
            unbound.forEach((name, parameter) -> {
                CaseRunner.RedResult r = CaseRunner.runRed(red.resolve("ir.json"), red.resolve(name + ".json"), branch);
                assertEquals("FAULT", r.status(), branch + " " + name + ": " + r.detail());
                assertEquals("EF-014", r.code(), branch + " " + name);
                assertEquals(List.of(Map.of("parameter", parameter)), r.params(), branch + " " + name);
            });
    }

    // ---------------- fixtures ----------------

    private static Path ir(String golden) {
        Path p = CORPUS.resolve(golden).resolve("ir.json");
        assertTrue(Files.exists(p), "golden ir.json present: " + p.toAbsolutePath());
        return p;
    }

    private static ObjectNode caseFile(String golden, String name) throws Exception {
        return (ObjectNode) JSON.readTree(Files.readString(CORPUS.resolve(golden).resolve(name + ".json")));
    }

    /** The attributes of the instance at {@code index} of the input of case {@code c}. */
    private static ObjectNode attributes(ObjectNode c, int index) {
        return (ObjectNode) c.get("input").get("instances").get(index).get("attributes");
    }

    private static ObjectNode parameters(ObjectNode c) {
        return (ObjectNode) c.get("input").get("parameters");
    }

    private static ObjectNode number(String lexeme) {
        return JSON.createObjectNode().put("number", lexeme);
    }

    /** The verdict of case {@code c} on {@code golden} through the replay, null on a match. */
    private static String play(String golden, JsonNode c) throws Exception {
        return CaseRunner.playCase(ir(golden), c);
    }

    /** Case {@code c} on {@code golden} is refused as a defect of the case, with a sentence that holds {@code words}. */
    private static void caseError(String golden, JsonNode c, String words) {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> play(golden, c));
        assertTrue(e.getMessage().contains(words), e.getMessage());
    }
}
