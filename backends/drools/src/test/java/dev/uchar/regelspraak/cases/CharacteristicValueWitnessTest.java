package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * WF-022's value clause ({@code spec/IR-SPEC.md} ch. 4.6): a characteristic attribution writes the boolean
 * constant true and nothing else. A flat {@code KENMERKTOEKENNING} carrying {@code onwaar} is ill-formed, and
 * both Drools branches refuse it themselves, by name, instead of answering it.
 *
 * <p><b>Why.</b> The document is the negative {@code corpus/negative/wf022-characteristic-false} (built on the red
 * card, 2026-09-23), loaded as gate 12 and the replay API load a document: without the verifier. Measured that
 * day with the case below: the verifier rejects the document on WF-022, the ASP backend stops at
 * translation ("characteristic attribution with a non-waar value") and {@code ref} runs the rule and leaves
 * {@code herleefd} absent — but the exec-model branch set {@code herleefd} PRESENT on R1 (it writes the
 * constant true for every characteristic attribution and never read the value) and the DRL-text branch
 * PLAYED with it absent. Gate 12 and the replay API load a document without the verifier, so the refusal has
 * to live in this backend. It lives in the loader, which both branches share, so they cannot part company on
 * it.
 *
 * <p><b>The rows.</b> The refusal names the rule, the target and the rule number, in both branches. Beside
 * it, the corpus-shaped form — the same rule with {@code waar} — still plays in both, with R1 present and R2
 * absent: the refusal is on the value, not on the construct.
 */
class CharacteristicValueWitnessTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    /** cwd under surefire is the module dir (backends/drools); the corpus is two levels up. */
    private static final Path NEGATIVE =
            Path.of("..", "..", "corpus", "negative", "wf022-characteristic-false", "ir.json");

    @TempDir Path tmp;

    @Test void a_characteristic_attribution_with_false_is_refused_in_both_branches_by_name() throws Exception {
        Path ir = NEGATIVE;
        Path casePath = write("casus.json", CASE_ABSENT);
        for (boolean viaDrl : new boolean[] { false, true }) {
            String branch = viaDrl ? "drl-tekst" : "exec-model";
            CaseRunner.Result r = viaDrl ? CaseRunner.viaDrlText(ir, List.of(casePath))
                                         : CaseRunner.viaExecModel(ir, List.of(casePath));
            assertEquals("EXCLUDE", r.status(), branch + " hoort de waarde onwaar te weigeren, niet te spelen: " + r);
            assertTrue(r.reason().contains("WF-022"), branch + " noemt de regel van de norm: " + r.reason());
            assertTrue(r.reason().contains("'geen herleving'"), branch + " noemt de regel: " + r.reason());
            assertTrue(r.reason().contains("'herleefd'"), branch + " noemt het doel: " + r.reason());
        }
    }

    @Test void the_same_rule_with_true_plays_in_both_branches() throws Exception {
        Path ir = write("waar-ir.json", document("waar"));
        Path casePath = write("casus-waar.json", CASE_PRESENT);
        for (boolean viaDrl : new boolean[] { false, true }) {
            CaseRunner.Result r = viaDrl ? CaseRunner.viaDrlText(ir, List.of(casePath))
                                         : CaseRunner.viaExecModel(ir, List.of(casePath));
            assertEquals("PLAYED", r.status(), (viaDrl ? "drl-tekst" : "exec-model") + ": " + r);
        }
    }

    private Path write(String naam, String inhoud) throws Exception {
        Path p = tmp.resolve(naam);
        Files.writeString(p, inhoud);
        return p;
    }

    /** The negative's document with the attribution's value set to {@code value}. */
    private static String document(String value) throws Exception {
        ObjectNode doc = (ObjectNode) JSON.readTree(Files.readString(NEGATIVE));
        ObjectNode literal = (ObjectNode) IrPath.version(doc, "rules", 0, 0).get("value");
        literal.putObject("value").put("boolean", value.equals("waar"));
        literal.set("source", IrPath.sourceReference(value));
        return doc.toPrettyString();
    }

    /** R1 has "nieuw recht ontstaan", R2 has not. ref leaves herleefd absent on both for the onwaar document. */
    private static final String CASE_ABSENT = """
            { "format": "uchar-case-v0", "asOfDate": null,
              "input": { "instances": [
                  { "label": "R1", "objectType": "Recht", "attributes": {}, "roles": {},
                    "characteristics": [ "nieuw recht ontstaan" ] },
                  { "label": "R2", "objectType": "Recht", "attributes": {}, "roles": {}, "characteristics": [] } ],
                "parameters": {} },
              "expected": [ { "kind": "characteristicAbsent", "label": "R1", "characteristic": "herleefd" },
                            { "kind": "characteristicAbsent", "label": "R2", "characteristic": "herleefd" },
                            { "kind": "consistent" } ] }
            """;

    private static final String CASE_PRESENT = """
            { "format": "uchar-case-v0", "asOfDate": null,
              "input": { "instances": [
                  { "label": "R1", "objectType": "Recht", "attributes": {}, "roles": {},
                    "characteristics": [ "nieuw recht ontstaan" ] },
                  { "label": "R2", "objectType": "Recht", "attributes": {}, "roles": {}, "characteristics": [] } ],
                "parameters": {} },
              "expected": [ { "kind": "characteristicPresent", "label": "R1", "characteristic": "herleefd" },
                            { "kind": "characteristicAbsent", "label": "R2", "characteristic": "herleefd" },
                            { "kind": "consistent" } ] }
            """;
}
