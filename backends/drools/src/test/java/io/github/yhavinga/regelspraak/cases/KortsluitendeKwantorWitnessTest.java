package io.github.yhavinga.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.yhavinga.regelspraak.runtime.RegelSpraakRuntime;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * RUNTIME-SEMANTICS ch. 5.10 — the composite condition's SHORT CIRCUIT, in the one place this back-end has
 * to spell it out.
 *
 * <p>The conjunctive quantifiers get it for free: ALLE/DE lower to successive in-pattern constraints, which
 * form an alpha chain, so a later member is simply never reached when an earlier one fails. The counting
 * quantifiers do not: they fold every member into ONE predicate lambda, and a lambda that counts all members
 * evaluates the member that divides by the zero an earlier member tested for — a thrown
 * {@code ArithmeticException} where ref and clingo answer.
 *
 * <p>The corpus golden {@code jutterswet-wachtdeling} carries exactly that shape: two rules over the SAME two
 * members in the same wire order, differing only in the quantifier. Member 1 is true exactly where member 2
 * would divide by zero. Its pinned expectations are hand-computed and verified by ref and clingo (gates 5 and
 * 6); this test reads the fired beans directly rather than through the gate's replay entry point, so a
 * regression names the attribute instead of a case-count mismatch.
 *
 * <p>W1 (ploegomvang 0) is the stop; W2 and W3 are the controls that prove member 2 IS read — and decides,
 * both ways — whenever member 1 does not settle the outcome.
 */
class KortsluitendeKwantorWitnessTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    // cwd under surefire is the module dir (backends/drools); the corpus is two levels up.
    private static final Path WACHTDELING =
            Path.of("..", "..", "corpus", "golden", "jutterswet-wachtdeling", "ir.json");

    /** ploegomvang 0: member 1 holds, so GEEN and TEN MINSTE 1 both stop there — with OPPOSITE verdicts.
     *  Member 2 is never read, so the division by zero it carries never happens. Reaching the assertions at
     *  all is half the witness: a counting fold without the stop throws before returning. */
    @Test void een_lid_na_de_stop_deelt_niet_door_nul() throws Exception {
        Object w = wacht("0", "100");
        assertNull(RegelSpraakRuntime.get(w, "geenteken"),
                "GEEN stopt bij de eerste treffer: de uitkomst staat op onwaar, het doel blijft ongeschreven");
        assertEquals(0, new BigDecimal("1").compareTo((BigDecimal) RegelSpraakRuntime.get(w, "minsteteken")),
                "TEN MINSTE 1 stopt bij DEZELFDE treffer: de uitkomst staat op waar, het doel wordt geschreven");
    }

    /** ploegomvang 4: member 1 does not hold, so neither quantifier settles — member 2 is read, divides
     *  cleanly (100/4 = 25 > 0) and holds. GEEN then has a hit and fails; TEN MINSTE 1 reaches its threshold. */
    @Test void zonder_stop_beslist_het_tweede_lid_wel() throws Exception {
        Object w = wacht("4", "100");
        assertNull(RegelSpraakRuntime.get(w, "geenteken"),
                "het tweede lid is een treffer, dus GEEN faalt");
        assertEquals(0, new BigDecimal("1").compareTo((BigDecimal) RegelSpraakRuntime.get(w, "minsteteken")),
                "het tweede lid is een treffer, dus TEN MINSTE 1 haalt de drempel");
    }

    /** ploegomvang 7: member 1 does not hold and member 2 divides to 0, which is not greater than 0 — no hits
     *  at all. The verdicts flip: GEEN holds, TEN MINSTE 1 does not. Without member 2 actually being read
     *  this row is indistinguishable from the previous one, which is why it is here. */
    @Test void geen_enkele_treffer_keert_beide_uitkomsten_om() throws Exception {
        Object w = wacht("7", "3");
        assertEquals(0, new BigDecimal("1").compareTo((BigDecimal) RegelSpraakRuntime.get(w, "geenteken")),
                "nul treffers: GEEN houdt stand");
        assertNull(RegelSpraakRuntime.get(w, "minsteteken"),
                "nul treffers: TEN MINSTE 1 haalt de drempel niet");
    }

    // ---------------- model ----------------

    /** Fire the golden on ONE Wacht and hand back its bean. */
    private static Object wacht(String ploegomvang, String vangstwaarde) throws Exception {
        return CaseRunner.naFire(WACHTDELING, geval(ploegomvang, vangstwaarde)).invoer().get("W1");
    }

    private static JsonNode geval(String ploegomvang, String vangstwaarde) throws Exception {
        return JSON.readTree("""
                { "format": "uchar-case-v0", "asOfDate": null,
                  "input": { "instances": [
                      { "label": "W1", "objectType": "Wacht",
                        "attributes": { "ploegomvang": { "number": "%s" },
                                        "vangstwaarde": { "number": "%s" } },
                        "roles": {}, "characteristics": [] } ],
                    "parameters": {} },
                  "expected": [] }
                """.formatted(ploegomvang, vangstwaarde));
    }
}
