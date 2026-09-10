package io.github.yhavinga.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.github.yhavinga.regelspraak.runtime.RegelSpraakRuntime;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The ch. 5.7a witness on the CASE-INPUT arm: a supplied role link establishes the relation, and the
 * SISTER role — the role the supplying instance itself fills — is where a TO-MANY written role stores it.
 *
 * <p>What moved (maintainer decision 2026-09-10). A written to-many role's storage is the far end's
 * field, and on a SELF-TYPED pair the by-pair search that used to name that field counts both same-typed
 * siblings and derives null by construction. {@link CaseRunner#zetRollen} therefore refused the whole
 * form — "reaches no to-one reference field … neither direction of the type pair derives a to-one role" —
 * although the field exists (FactModelGenerator's self-pair derivation makes it) and the very next line
 * could have written it. The written role name is the route: it names one feittype, and that feittype's
 * OTHER role is the back-reference.
 *
 * <p>Every expectation here is the corpus golden's own, replayed through {@code ref/} and the ASP backend
 * by gates 5 and 6; the pins below are the Drools leg of the same square, plus the bean-level assertions
 * those totals do not imply — WHICH bean carries the reference, and that the mirror writes no more than
 * the relation it was given.
 */
class ZusterrolInvoerWitnessTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    // cwd under surefire is the module dir (backends/drools); the corpus is two levels up.
    private static final Path CORPUS = Path.of("..", "..", "corpus", "golden");

    /** The ch. 5.7a worked example: `ketenband` relates Wachter to Wachter through a TO-ONE `voorligger`
     *  and a TO-MANY `opvolger`, and two rules navigate through the opposite role of the one they read. */
    private static final String KETENBAND = "jutterswet-ketenband";

    @Test void een_naar_veel_zelfpaarrol_landt_op_het_zusterveld_van_het_doel() throws Exception {
        // The written key names `opvolger` (TO-MANY), so the storage is the far end's field: each target's
        // own `voorligger` reference back at W1. Before the by-name sister route this was the refusal.
        Map<String, Object> beans = CaseRunner.invoerBeans(ir(KETENBAND), JSON.readTree(caseFile("case2")));
        assertSame(beans.get("W1"), RegelSpraakRuntime.get(beans.get("W2"), "voorligger"),
                "the to-many key 'opvolgers' stores W1 as the voorligger of each target");
        assertSame(beans.get("W1"), RegelSpraakRuntime.get(beans.get("W3"), "voorligger"));
        assertNull(RegelSpraakRuntime.get(beans.get("W1"), "voorligger"),
                "and no more than that: nobody fills `voorligger` toward W1, so W1's own reference stays empty");
    }

    @Test void de_drie_spellingen_van_een_relatie_leveren_dezelfde_bonen() throws Exception {
        // The law itself, at bean level: one relation, three ways to spell it, one fact base. case1 writes
        // both legs, case2 the to-many leg only, case3 the to-one legs only.
        for (String c : new String[] { "case1", "case2", "case3" }) {
            Map<String, Object> beans = CaseRunner.invoerBeans(ir(KETENBAND), JSON.readTree(caseFile(c)));
            assertSame(beans.get("W1"), RegelSpraakRuntime.get(beans.get("W2"), "voorligger"), c);
            assertSame(beans.get("W1"), RegelSpraakRuntime.get(beans.get("W3"), "voorligger"), c);
            assertNull(RegelSpraakRuntime.get(beans.get("W1"), "voorligger"), c);
        }
    }

    @Test void alle_drie_de_casussen_spelen_met_de_gepinde_waarden() throws Exception {
        // The gate-12 leg of the square, per case: the golden's OWN hand-computed expectations
        // (voorliggerrang leeg/10/10, opvolgeraantal 2/0/0) replay through the fired session.
        for (String c : new String[] { "case1", "case2", "case3" })
            assertNull(CaseRunner.speelCase(ir(KETENBAND), JSON.readTree(caseFile(c))),
                    "ref and clingo pin these values; the Drools leg must reach them too: " + c);
    }

    @Test void een_verdraaide_verwachting_faalt_wel_degelijk() throws Exception {
        // PLAYED is not empty greenness: twist the one expectation that names the UNSUPPLIED direction —
        // W2's voorliggerrang in the to-many-leg-only case — and the replay must say so.
        ObjectNode verdraaid = (ObjectNode) JSON.readTree(caseFile("case2"));
        int getwist = 0;
        for (JsonNode v : verdraaid.get("expected"))
            if ("W2".equals(v.path("label").asText()) && "voorliggerrang".equals(v.path("attribute").asText())) {
                ((ObjectNode) v.get("value")).put("number", "11");
                getwist++;
            }
        assertEquals(1, getwist, "exactly the one expectation naming the unsupplied direction was twisted");
        assertNotNull(CaseRunner.speelCase(ir(KETENBAND), verdraaid),
                "a twisted expectation is a named mismatch, never a silent pass");
    }

    @Test void een_botsende_terugpoot_op_de_zusterrol_is_hard() throws Exception {
        // The WF-013 flank of the mirror, on the arm that now writes it: W1 names W2 as its opvolger, so
        // W2's `voorligger` must become W1 — but the case already gave W2 a different voorligger. ref
        // refuses that case at LOAD with the same rule, so playing it green here would file a broken case
        // under a passing replay.
        String botsend = """
                { "format": "uchar-case-v0", "asOfDate": null,
                  "input": { "instances": [
                      { "label": "W1", "objectType": "Wachter", "attributes": { "rangnummer": { "number": "10" } },
                        "roles": { "opvolgers": ["W2"] }, "characteristics": [] },
                      { "label": "W2", "objectType": "Wachter", "attributes": { "rangnummer": { "number": "20" } },
                        "roles": { "voorligger": ["W3"] }, "characteristics": [] },
                      { "label": "W3", "objectType": "Wachter", "attributes": { "rangnummer": { "number": "30" } },
                        "roles": {}, "characteristics": [] } ],
                    "parameters": {} },
                  "expected": [
                      { "kind": "attributeValue", "label": "W1", "attribute": "opvolgeraantal", "value": { "number": "1" } } ] }
                """;
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> CaseRunner.invoerBeans(ir(KETENBAND), JSON.readTree(botsend)));
        assertTrue(e.getMessage().contains("WF-013"), "the refusal names the rule it enforces: " + e.getMessage());
    }

    // ---------------- fixtures ----------------

    private static Path ir(String golden) {
        Path p = CORPUS.resolve(golden).resolve("ir.json");
        assertTrue(Files.exists(p), "golden ir.json present: " + p.toAbsolutePath());
        return p;
    }

    /** The golden's own case file, read off disk — no expectation is authored here. */
    private static String caseFile(String naam) throws Exception {
        Path p = CORPUS.resolve(KETENBAND).resolve(naam + ".json");
        assertTrue(Files.exists(p), "golden case present: " + p.toAbsolutePath());
        return Files.readString(p);
    }
}
