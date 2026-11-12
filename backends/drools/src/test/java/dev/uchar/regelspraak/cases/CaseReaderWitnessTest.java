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
