package io.github.yhavinga.regelspraak.cases;

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
 * <p><b>Why.</b> The document is the negatief {@code corpus/negatief/wf022-kenmerk-onwaar} (built on the rood
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
class KenmerkwaardeWitnessTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    /** cwd under surefire is the module dir (backends/drools); the corpus is two levels up. */
    private static final Path NEGATIEF =
            Path.of("..", "..", "corpus", "negatief", "wf022-kenmerk-onwaar", "ir.json");

    @TempDir Path tmp;

    @Test void een_kenmerktoekenning_met_onwaar_weigeren_beide_takken_bij_naam() throws Exception {
        Path ir = NEGATIEF;
        Path casus = schrijf("casus.json", CASUS_AFWEZIG);
        for (boolean viaDrl : new boolean[] { false, true }) {
            String tak = viaDrl ? "drl-tekst" : "exec-model";
            CaseRunner.Result r = viaDrl ? CaseRunner.viaDrlTekst(ir, List.of(casus))
                                         : CaseRunner.viaExecModel(ir, List.of(casus));
            assertEquals("EXCLUDE", r.status(), tak + " hoort de waarde onwaar te weigeren, niet te spelen: " + r);
            assertTrue(r.reason().contains("WF-022"), tak + " noemt de regel van de norm: " + r.reason());
            assertTrue(r.reason().contains("'geen herleving'"), tak + " noemt de regel: " + r.reason());
            assertTrue(r.reason().contains("'herleefd'"), tak + " noemt het doel: " + r.reason());
        }
    }

    @Test void dezelfde_regel_met_waar_speelt_in_beide_takken() throws Exception {
        Path ir = schrijf("waar-ir.json", document("waar"));
        Path casus = schrijf("casus-waar.json", CASUS_AANWEZIG);
        for (boolean viaDrl : new boolean[] { false, true }) {
            CaseRunner.Result r = viaDrl ? CaseRunner.viaDrlTekst(ir, List.of(casus))
                                         : CaseRunner.viaExecModel(ir, List.of(casus));
            assertEquals("PLAYED", r.status(), (viaDrl ? "drl-tekst" : "exec-model") + ": " + r);
        }
    }

    private Path schrijf(String naam, String inhoud) throws Exception {
        Path p = tmp.resolve(naam);
        Files.writeString(p, inhoud);
        return p;
    }

    /** The negatief's document with the attribution's value set to {@code waarde}. */
    private static String document(String waarde) throws Exception {
        ObjectNode doc = (ObjectNode) JSON.readTree(Files.readString(NEGATIEF));
        ObjectNode literal = (ObjectNode) IrPad.versie(doc, "resolvedRegels", 0, 0).get("value");
        literal.putObject("value").put("boolean", waarde.equals("waar"));
        literal.set("source", IrPad.bronrecord(waarde));
        return doc.toPrettyString();
    }

    /** R1 has "nieuw recht ontstaan", R2 has not. ref leaves herleefd absent on both for the onwaar document. */
    private static final String CASUS_AFWEZIG = """
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

    private static final String CASUS_AANWEZIG = """
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
