package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The pre-rounded conversion factor, pinned as the tracked delta it is (CONSTRUCTS row 26). Since L3 of the v1 wire
 * migration a unit's factor is an exact fraction on the wire, and {@code ref} and the ASP backend reckon with it
 * (IR-SPEC ch. 2.5: {@code factor(u)/factor(v)} as a rational, one rounding at the end). This backend lays the fraction
 * onto the record's decimal factor, rounded to 16 significant digits, and restates between two units with the ratio
 * of those decimals (the loader's class comment). The corpus does not see it — every factor there is a whole number —
 * but a verifier-clean document the frontend emits does, wherever a restated value lands on a rounding boundary. Each
 * row below is such a document, emitted by the frontend from the source it quotes, and pins what BOTH branches
 * answer; {@code ref} answers the exact value on the same document and case (measured 2026-09-24), and the ASP backend
 * refuses the additive join over two units by name.
 *
 * <p>The rows are expected to turn red the day the transpilers restate through the fraction the loader keeps beside
 * the decimal ({@code Eenheidstelsel.exacteFactoren}); their pin then becomes {@code BothBranches.green} on the exact
 * values, and row 26 loses this delta. Until then a change in how the branches round shows up here, by name.
 */
class PreRoundedFactorWitnessTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    /**
     * The factor 1/3, stored as 0.3333333333333333. Source:
     * <pre>
     * Eenheidsysteem Wachtrooster
     *     de dienst (mv: diensten) dn
     *     de wacht (mv: wachten) wa = 1/3 dn
     * Objecttype de Jutter (mv: Jutters) (bezield)
     *     de dienstduur Numeriek (getal met 2 decimalen) met eenheid dn;
     *     de wachtduur Numeriek (getal met 2 decimalen) met eenheid wa;
     *     de inzet Numeriek (getal met 2 decimalen) met eenheid dn;
     *     de afgeronde inzet Numeriek (geheel getal) met eenheid dn;
     * Regel inzet: … als de dienstduur van de Jutter plus de wachtduur van de Jutter.
     * Regel afgeronde inzet: … als de dienstduur van de Jutter plus de wachtduur van de Jutter
     *     naar beneden afgerond op 0 decimalen.
     * </pre>
     * 1.00 dn plus 3.00 wa is 1 + 3 · 1/3 = 2.00 dn exactly, and rounded down it stays 2. Both branches restate the
     * 3.00 wa as 3 · 0.3333333333333333 = 0.9999999999999999, so the sum is 1.9999999999999999 — written unrounded into
     * the 2-decimal attribute — and rounded down it is 1.
     */
    @Test void the_factor_one_third_computes_pre_rounded_in_both_branches() throws Exception {
        Path ir = document("Wachtrooster", "dn", "dienst", "wa", "wacht", 3, "dienstduur", "wachtduur", "inzet",
                "afgeronde inzet", "floor", "naar beneden");
        JsonNode caseDocument = caseDocument("dienstduur", "1.00", "wachtduur", "3.00", "inzet", "2.00", "afgeronde inzet", "2");
        String exec = BothBranches.red(ir, caseDocument, "attribuut inzet: verwacht 2.00, werkelijk 1.9999999999999999",
                "de voorafgeronde factor 1/3 blijft onder de 2");
        assertTrue(exec.contains("attribuut afgeronde inzet: verwacht 2, werkelijk 1"), exec);
        BothBranches.red(ir, caseDocument, "attribuut afgeronde inzet: verwacht 2, werkelijk 1",
                "naar beneden afgerond valt de voorafgeronde som op 1");
    }

    /**
     * The factor 1/24, stored as 0.04166666666666667 — a factor the parity zone declares for its hour. Source as
     * above, with {@code Eenheidsysteem Getijrooster}, {@code het tij (mv: tijen) tj},
     * {@code de slag (mv: slagen) sl = 1/24 tj}, the attributes {@code tijduur} (tj), {@code slagduur} (sl),
     * {@code looptijd} (tj, 2 decimals) and {@code afgeronde looptijd} (tj, whole), and the second rule rounding
     * {@code naar boven}. 0.00 tj plus 216.00 sl is 216/24 = 9.00 tj exactly, and rounded up it stays 9. Both branches
     * restate the 216.00 sl as 216 · 0.04166666666666667 = 9.00000000000000072, rounded once to 16 digits:
     * 9.000000000000001, and rounded up that is 10. (At 24.00 sl the one rounding hides the excess — 1.00000000000000008
     * rounds to 1 — which is why the row stands at a value whose restatement lands just above an integer near the top
     * of its decade.) That was the destructive DRL-text branch until card drl-teksttak-op-truth-maintenance, as the
     * destructive exec-model branch was. The analysis with truth maintenance multiplies by the same decimal factor
     * without that rounding, 9.00000000000000072, and rounded up 10 as well, on both branches since that card. Both are
     * the registered deviation of CONSTRUCTS row 26; {@code ref} answers 9.00 and 9.
     */
    @Test void the_factor_one_twenty_fourth_computes_pre_rounded_in_both_branches() throws Exception {
        Path ir = document("Getijrooster", "tj", "tij", "sl", "slag", 24, "tijduur", "slagduur", "looptijd",
                "afgeronde looptijd", "ceiling", "naar boven");
        JsonNode caseDocument = caseDocument("tijduur", "0.00", "slagduur", "216.00", "looptijd", "9.00", "afgeronde looptijd", "9");
        for (DroolsBackend.Branch branch : DroolsBackend.Branch.values()) {
            String tms = CaseRunner.playCase(ir, caseDocument, branch);
            assertTrue(tms != null && tms.contains("attribuut looptijd: verwacht 9.00, werkelijk 9.00000000000000072"),
                    branch + ": de decimale factor 1/24 komt boven de 9 uit, zonder afronding op 16 cijfers: " + tms);
            assertTrue(tms.contains("attribuut afgeronde looptijd: verwacht 9, werkelijk 10"),
                    branch + ": naar boven afgerond valt de voorafgeronde som op 10");
        }
    }

    // ---------------- the document the frontend emits for the two sources, with its names ----------------

    /** The emitted document: one unit system of a base unit {@code basis} and a unit {@code deel} = 1/{@code noemer}
     *  of it; a Jutter with the two addends, the sum and the rounded sum; the two rules. */
    private static Path document(String system, String base, String baseWord, String part, String partWord,
                                 int denominator, String left, String right, String sum, String rounded,
                                 String mode, String modeSource) throws Exception {
        String baseUnit = unit(system, base, base, 1);
        String partUnit = unit(system, part, base, denominator);
        String sumType = number(false, 2, baseUnit);
        String addition = ("{\"node\":\"Binary\",\"op\":\"plus\",\"left\":%s,\"right\":%s,\"type\":%s,"
                + "\"source\":{\"snippet\":\"de %s van de Jutter plus de %s van de Jutter\"}}")
                .formatted(ownRead(left), ownRead(right), sumType, left, right);
        String rounding = ("{\"node\":\"Rounding\",\"base\":%s,\"mode\":\"%s\",\"decimals\":0,\"type\":%s,"
                + "\"source\":{\"snippet\":\"de %s van de Jutter plus de %s van de Jutter %s afgerond op 0 decimalen\"}}")
                .formatted(addition, mode, number(true, 0, baseUnit), left, right, modeSource);
        String objecttype = ("{\"node\":\"ObjectType\",\"name\":\"Jutter\",\"animate\":true,\"plural\":\"Jutters\","
                + "\"attributes\":[%s,%s,%s,%s],\"characteristics\":[]}").formatted(
                attribuut(left, sumType), attribuut(right, number(false, 2, partUnit)), attribuut(sum, sumType),
                attribuut(rounded, number(true, 0, baseUnit)));
        String units = "[" + declared(baseUnit, baseWord) + "," + declared(partUnit, partWord) + "]";
        String doc = ("{\"format\":\"uchar-ir-v1\",\"model\":{\"objectTypes\":[%s],\"factTypes\":[],\"parameters\":[],"
                + "\"domains\":[],\"unitSystems\":[{\"node\":\"UnitSystem\",\"name\":\"%s\",\"units\":%s}],"
                + "\"dimensions\":[],\"timelines\":[],\"dayKinds\":[]},\"rules\":{\"rules\":[%s,%s],"
                + "\"dayKindDefinitions\":[],\"objectCreations\":[],\"allocations\":[],\"consistencies\":[],"
                + "\"uniquenesses\":[],\"factCreations\":[],\"startPointDeterminations\":[],"
                + "\"ruleGroups\":[]},\"diagnostics\":[]}").formatted(objecttype, system, units,
                regel(sum, addition), regel(rounded, rounding));
        Path tmp = Files.createTempFile("voorafgeronde-factor", ".json");
        tmp.toFile().deleteOnExit();
        Files.writeString(tmp, doc);
        return tmp;
    }

    private static String unit(String system, String abbreviation, String canonical, int denominator) {
        return ("{\"node\":\"Unit\",\"abbreviation\":\"%s\",\"system\":\"%s\",\"canonical\":\"%s\","
                + "\"factorToCanonical\":{\"num\":1,\"den\":%d}}").formatted(abbreviation, system, canonical, denominator);
    }

    /** A unit as its system declares it: the unit's fields, then its word and the word's plural. */
    private static String declared(String unit, String word) {
        return unit.substring(0, unit.length() - 1)
                + ",\"spellings\":[\"%s\",\"%sen\"]}".formatted(word, word);
    }

    private static String number(boolean whole, int decimals, String unit) {
        return "{\"node\":\"Number\",\"whole\":%b,\"decimals\":%d,\"sign\":\"unspecified\",\"unit\":%s}"
                .formatted(whole, decimals, unit);
    }

    private static String attribuut(String naam, String datatype) {
        return "{\"node\":\"Attribute\",\"name\":\"%s\",\"datatype\":%s}".formatted(naam, datatype);
    }

    private static String ownRead(String attribuut) {
        return "{\"node\":\"AttributeReference\",\"attribute\":\"%s\",\"subjectType\":\"Jutter\",\"source\":{\"snippet\":\"de %s van de Jutter\"}}"
                .formatted(attribuut, attribuut);
    }

    private static String regel(String doel, String waarde) {
        return IrPath.regel(doel, "BEREKENING", ("{\"node\":\"AttributeReference\",\"attribute\":\"%s\",\"subjectType\":\"Jutter\","
                + "\"source\":{\"snippet\":\"De %s van een Jutter\"}}").formatted(doel, doel), waarde, "null");
    }

    /** One Jutter with the two addends, and the EXACT values of the sum and the rounded sum as expectations. */
    private static JsonNode caseDocument(String left, String lw, String right, String rw, String sum, String sw,
                                 String rounded, String aw) throws Exception {
        return JSON.readTree(("""
                { "format": "uchar-case-v0", "asOfDate": null,
                  "input": { "instances": [ { "label": "J1", "objectType": "Jutter",
                      "attributes": { "%s": { "number": "%s" }, "%s": { "number": "%s" } },
                      "roles": {}, "characteristics": [] } ], "parameters": {} },
                  "expected": [
                    { "kind": "attributeValue", "label": "J1", "attribute": "%s", "value": { "number": "%s" } },
                    { "kind": "attributeValue", "label": "J1", "attribute": "%s", "value": { "number": "%s" } },
                    { "kind": "consistent" } ] }
                """).formatted(left, lw, right, rw, sum, sw, rounded, aw));
    }
}
