package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * IR-SPEC ch. 4.5 / RUNTIME-SEMANTICS ch. 5.10: a consistency whose assertion is a composite under the quantifier
 * {@code the}. On a {@code Compound} that quantifier reads as {@code all} does — every part must hold, h = n — and
 * {@code ref} and the ASP backend answer it so. Both Drools branches excluded it until the card
 * aantal-bronarm-en-drools-lader (the exec-model branch with a transpiler SKIP, "schending buiten Phase-D scope", the
 * DRL-text branch with a marker), while they played the same assertion under {@code all}; since that card both
 * branches lower it through their {@code all} arm. RegelSpraak spells no {@code the} in a consistency (its
 * kwantificatie there has no "de", and "het volgende criterium" is read as {@code all}), so no golden carries the
 * form; this witness does, on an in-memory variant of {@code jutterswet-keurcriteria}, read and never written.
 *
 * <p>Measured before the card on the same two documents with the same finds: {@code ref} and the ASP backend
 * matched every expectation below, both Drools branches excluded the rule.
 */
class ConsistencyTheQuantifierWitnessTest {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Path GOLDEN = Path.of("..", "..", "corpus", "golden", "jutterswet-keurcriteria");
    private static final String NAME = "keuring naar de criteria";

    @Test void the_quantifier_over_two_criteria_requires_both() throws Exception {
        // V1 meets both criteria; V9 meets the first only; V5 has no gewicht, so the first does not hold
        BothBranches.green(temporary(withThe(2)), caseDocument("[" + vondst("V1", "2.50", "12") + "]", null),
                "the over two criteria, both hold: consistent");
        BothBranches.green(temporary(withThe(2)), caseDocument("[" + vondst("V9", "2.50", "5") + "]", "V9"),
                "the over two criteria, one holds: violated, as under all");
        BothBranches.green(temporary(withThe(2)), caseDocument("[" + vondst("V5", null, "12") + "]", "V5"),
                "the over two criteria, a leeg operand: its criterion does not hold");
    }

    @Test void the_quantifier_over_a_criterion_requires_that_one() throws Exception {
        BothBranches.green(temporary(withThe(1)), caseDocument("[" + vondst("V9", "2.50", "5") + "]", null),
                "the over the first criterion alone, which holds: consistent");
        BothBranches.green(temporary(withThe(1)), caseDocument("[" + vondst("V4", "0.00", "5") + "]", "V4"),
                "the over the first criterion alone, which does not hold: violated");
    }

    // ---------------- fixtures ----------------

    /** The golden reduced to its first consistency, under {@code the} and over its first {@code parts} criteria. */
    private static ObjectNode withThe(int parts) throws Exception {
        ObjectNode ir = (ObjectNode) JSON.readTree(Files.readString(GOLDEN.resolve("ir.json")));
        ObjectNode rules = (ObjectNode) ir.get("rules");
        ObjectNode k = (ObjectNode) rules.get("consistencies").get(0).deepCopy();
        k.put("name", NAME);
        ObjectNode assertion = (ObjectNode) k.get("versions").get(0).get("assertion");
        assertion.put("quantifier", "the").put("threshold", 0);
        ArrayNode partsList = (ArrayNode) assertion.get("parts");
        while (partsList.size() > parts) partsList.remove(partsList.size() - 1);
        rules.putArray("consistencies").add(k);
        return ir;
    }

    private static String vondst(String label, String weight, String length) {
        return "{ \"label\": \"" + label + "\", \"objectType\": \"Vondst\", \"attributes\": {"
                + (weight == null ? "" : "\"gewicht\": { \"number\": \"" + weight + "\" }, ")
                + "\"lengte\": { \"number\": \"" + length + "\" } }, \"roles\": {}, \"characteristics\": [] }";
    }

    /** One case: consistent, or the rule violated on {@code geschonden}. */
    private static JsonNode caseDocument(String instances, String geschonden) throws Exception {
        String expected = geschonden == null ? "{ \"kind\": \"consistent\" }"
                : "{ \"kind\": \"inconsistent\", \"rule\": \"" + NAME + "\", \"label\": \"" + geschonden + "\" }";
        return JSON.readTree("{ \"format\": \"uchar-case-v0\", \"asOfDate\": null, \"input\": { \"instances\": "
                + instances + ", \"parameters\": {} }, \"expected\": [ " + expected + " ] }");
    }

    private static Path temporary(ObjectNode ir) throws Exception {
        Path tmp = Files.createTempFile("consistentie-de", ".json");
        tmp.toFile().deleteOnExit();
        Files.writeString(tmp, ir.toString());
        return tmp;
    }
}
