package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.ObjectMapper;

import dev.uchar.regelspraak.engine.ScenarioResult;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A division by a numeric zero: "De verhouding van een Vondst moet berekend worden als zijn gewogen gewicht gedeeld door
 * zijn geschatte gewicht", over a Vondst whose geschatte gewicht is 0,00. RUNTIME-SEMANTICS ch. 1.4, point 4 keeps the
 * hard fault of the mirror until the v1 delta to the §9.5 channel lands in every executor at once (the red case
 * {@code division-by-numeric-zero}); ref faults with "division by numeric zero" and clingo with its fail-fast guard, on
 * the subject V3. This backend faults as well, and names it: the rule the division ran in and the operands, in a
 * sentence of its own, where the replay and the scenario report read it; a consumer that fires the KieBase itself
 * gets Drools' ConsequenceException and names it through {@code DroolsBackend.designate} (since 2026-10-02, no handler
 * on a class name: {@code ZeroDivisorConsumerWitnessTest}). A leeg divisor
 * stays leeg with its §9.5 deposit, and a leeg dividend over a zero divisor stays 0 (ch. 1.2). Every document is authored
 * here on Jutterswet material, with the outcomes ref and clingo give it.
 *
 * <p><b>Red before the fix</b> (measured 2026-10-02 on the backend of {@code da03d480}): the fire threw Drools'
 * {@code ConsequenceException} ("Exception executing consequence for rule "verhouding" in defaultpkg:
 * java.lang.ArithmeticException: Breuk deling door 0"), in a rule's value, in a default's consequence and in the
 * absolute division alike, and the scenario report carried that text.
 */
class ZeroDivisorWitnessTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    /** The fault is the runtime's own named type, carrying the sentence as its message (by name, so this witness
     *  compiled against the backend before the type existed). */
    private static void named(String sentence, RuntimeException e) {
        assertEquals("DelingDoorNul", e.getClass().getSimpleName(), () -> e.getClass().getName() + ": " + e.getMessage());
        assertEquals(sentence, e.getMessage());
    }

    private static String sentence(String regel) {
        return "deling door nul in de regel \"" + regel + "\": 60 gedeeld door 0 — een numerieke nul als deler is een "
                + "fout, geen leeg (RUNTIME-SEMANTICS ch. 1.4, punt 4)";
    }

    @Test void a_zero_divisor_in_the_value_of_a_rule() throws Exception {
        Path ir = document(regel(division("dividedBy")), "");
        named(sentence("verhouding"), assertThrows(RuntimeException.class, () -> play(ir, false, ZERO_AND_ORDINARY)));
    }

    @Test void a_zero_divisor_in_the_consequence_of_a_default() throws Exception {
        // "Afwijking van verhouding: gewogen" under the base 1,00, the division where the gewogen gewicht is gevuld. The
        // destructive exec-model branch carried the group as one rule, named by its target on its owner type, and gave
        // the division by zero that name. The exec-model branch with truth maintenance refuses a division in a default
        // by name: it computes a default as soon as what it reads stands, so a division by zero there is a fault on an
        // intermediate state, and whether it counts waits on the norm decision of card 20261008-fout-op-tussenstand.
        Path ir = document("", group(defaultEntry(filled("gewogen gewicht"), division("dividedBy")), defaultEntry(ALTIJD, EEN)));
        CaseRunner.Excluded e = assertThrows(CaseRunner.Excluded.class, () -> CaseRunner.playCase(ir,
                JSON.readTree(ZERO_AND_ORDINARY), DroolsBackend.Branch.EXEC_MODEL));
        org.junit.jupiter.api.Assertions.assertTrue(e.getMessage().contains("defaults: defaults A:Vondst.verhouding — een"
                + " deling in een default ('zijn gewogen gewicht gedeeld door zijn geschatte gewicht'): "), e.getMessage());
    }

    @Test void a_zero_divisor_in_the_absolute_division() throws Exception {
        Path ir = document(regel(division("dividedByAbs")), "");
        named(sentence("verhouding"), assertThrows(RuntimeException.class, () -> play(ir, false, ZERO_AND_ORDINARY)));
    }

    @Test void the_drl_text_branch_names_the_same_fault() throws Exception {
        Path ir = document(regel(division("dividedBy")), "");
        named(sentence("verhouding"), assertThrows(RuntimeException.class, () -> play(ir, true, ZERO_AND_ORDINARY)));
    }

    @Test void the_scenario_report_carries_the_sentence() throws Exception {
        Path ir = document(regel(division("dividedBy")), "");
        Path c = Files.writeString(Files.createTempDirectory("nuldeler").resolve("case1-nul.json"), ZERO_AND_ORDINARY);
        List<ScenarioResult> report = CaseRunner.report(ir, List.of(c));
        assertEquals(ScenarioResult.Status.FAIL, report.get(0).status(), report::toString);
        assertEquals("case1-nul.json: ERROR DelingDoorNul: " + sentence("verhouding"), report.get(0).diagnostic());
    }

    @Test void the_red_route_keeps_the_pinned_fragments() throws Exception {
        // gate 8 pins "Breuk deling door 0" and "deling door 0" on three red cases: the chain still carries them
        Path ir = document(regel(division("dividedBy")), "");
        Path c = Files.writeString(Files.createTempFile("nuldeler", ".json"), ZERO_AND_ORDINARY);
        CaseRunner.RedResult r = CaseRunner.runRed(ir, c);
        assertEquals("FAULT", r.status(), r::detail);
        assertTrue(r.detail().startsWith("DelingDoorNul: " + sentence("verhouding")), r.detail());
        assertTrue(r.detail().contains("Breuk deling door 0"), r.detail());
    }

    @Test void an_empty_numerator_above_a_zero_divisor_stays_zero() throws Exception {
        // ch. 1.2: the dividend is tested first and coalesces, so leeg ÷ 0 = 0; ref and clingo: V1 0,75, V3 0,00
        Path ir = document(regel(division("dividedBy")), "");
        String caseDocument = caseDocument(vondst("V1", "320.00", "240.00") + "," + vondst("V3", "0.00", null),
                ratio("V1", "0.75") + "," + ratio("V3", "0.00"));
        assertNull(play(ir, false, caseDocument), "de lege teller");
    }

    // ---------------- documents ----------------

    private static final String TWO_DECIMALS = "{\"node\":\"Number\",\"whole\":false,\"decimals\":2,\"sign\":\"unspecified\"}";

    private static final String MODEL = "{\"objectTypes\":[{\"node\":\"ObjectType\",\"name\":\"Vondst\",\"animate\":false,"
            + "\"plural\":\"Vondsten\",\"attributes\":[" + attribute("geschatte gewicht") + "," + attribute("gewogen gewicht") + ","
            + attribute("verhouding") + "],\"characteristics\":[]}],\"factTypes\":[],\"parameters\":[],\"domains\":[],"
            + "\"unitSystems\":[],\"dimensions\":[],\"timelines\":[],\"dayKinds\":[]}";

    private static String attribute(String naam) {
        return "{\"node\":\"Attribute\",\"name\":\"" + naam + "\",\"datatype\":" + TWO_DECIMALS + "}";
    }

    private static String own(String attribute) {
        return "{\"node\":\"AttributeReference\",\"attribute\":\"" + attribute + "\",\"subjectType\":\"Vondst\","
                + "\"source\":{\"snippet\":\"zijn " + attribute + "\"}}";
    }

    /** "zijn gewogen gewicht gedeeld door [(ABS)] zijn geschatte gewicht". */
    private static String division(String op) {
        return "{\"node\":\"Binary\",\"op\":\"" + op + "\",\"left\":" + own("gewogen gewicht") + ",\"right\":"
                + own("geschatte gewicht") + ",\"type\":" + TWO_DECIMALS + ",\"source\":{\"snippet\":\"zijn gewogen gewicht"
                + " gedeeld door zijn geschatte gewicht\"}}";
    }

    private static String filled(String attribute) {
        return "{\"node\":\"Predicate\",\"kind\":\"filled\",\"operand\":" + own(attribute)
                + ",\"negated\":false,\"digits\":0,\"source\":{\"snippet\":\"zijn " + attribute + " gevuld is\"}}";
    }

    private static final String ALTIJD = "{\"node\":\"Compound\",\"quantifier\":\"all\",\"threshold\":0,\"parts\":[],"
            + "\"source\":{\"snippet\":\"altijd\"}}";

    /** The base "De verhouding van een Vondst moet gesteld worden op 1,00". */
    private static final String EEN = "{\"node\":\"Literal\",\"value\":{\"number\":\"1.00\"},\"type\":" + TWO_DECIMALS
            + ",\"source\":{\"snippet\":\"1,00\"}}";

    private static String defaultEntry(String test, String consequence) {
        return "{\"node\":\"Default\",\"justification\":" + test + ",\"consequence\":" + consequence + "}";
    }

    /** The defaults group on the verhouding of a Vondst, highest default first. */
    private static String group(String... defaults) {
        return "{\"node\":\"PrioritizedDefaults\",\"name\":\"verhouding\",\"subject\":\"Vondst\",\"attribute\":"
                + "\"verhouding\",\"defaults\":[" + String.join(",", defaults) + "],\"validity\":{\"node\":\"Validity\"}}";
    }

    /** The rule "verhouding" with {@code value} as its value, without a condition. */
    private static String regel(String value) {
        return "{\"node\":\"Rule\",\"name\":\"verhouding\",\"versions\":[{\"validity\":{\"node\":\"Validity\"},\"target\":"
                + "{\"node\":\"AttributeReference\",\"attribute\":\"verhouding\",\"subjectType\":\"Vondst\",\"source\":"
                + "{\"snippet\":\"De verhouding van een Vondst\"}},\"value\":" + value + ",\"initialisation\":false,"
                + "\"presentation\":\"calculation\"}]}";
    }

    private static Path document(String rules, String groups) throws Exception {
        String ir = "{\"format\":\"uchar-ir-v1\",\"model\":" + MODEL + ",\"rules\":{\"rules\":[" + rules + "],"
                + "\"dayKindDefinitions\":[],\"objectCreations\":[],\"allocations\":[],\"consistencies\":[],\"uniquenesses\":[],"
                + "\"factCreations\":[],\"startPointDeterminations\":[],\"ruleGroups\":[],\"prioritizedDefaults\":[" + groups
                + "]},\"diagnostics\":[]}";
        Path tmp = Files.createTempFile("nuldeler", ".json");
        tmp.toFile().deleteOnExit();
        return Files.writeString(tmp, ir);
    }

    // ---------------- cases ----------------

    /** A Vondst with an optional geschat and gewogen gewicht. */
    private static String vondst(String label, String geschat, String gewogen) {
        String a = (geschat == null ? "" : "\"geschatte gewicht\":{\"number\":\"" + geschat + "\"}")
                + (geschat != null && gewogen != null ? "," : "")
                + (gewogen == null ? "" : "\"gewogen gewicht\":{\"number\":\"" + gewogen + "\"}");
        return "{\"label\":\"" + label + "\",\"objectType\":\"Vondst\",\"attributes\":{" + a + "},\"roles\":{},\"characteristics\":[]}";
    }

    private static String ratio(String label, String value) {
        return "{\"kind\":\"attributeValue\",\"label\":\"" + label + "\",\"attribute\":\"verhouding\",\"value\":{\"number\":\""
                + value + "\"}}";
    }

    private static String caseDocument(String instances, String expected) {
        return "{\"format\":\"uchar-case-v0\",\"asOfDate\":null,\"input\":{\"instances\":[" + instances
                + "],\"parameters\":{}},\"expected\":[" + expected + "]}";
    }

    /** V1 (240 by 320, 0,75) beside V3, whose geschatte gewicht is 0,00 under a gewogen gewicht of 60,00. */
    private static final String ZERO_AND_ORDINARY = caseDocument(vondst("V1", "320.00", "240.00") + "," + vondst("V3", "0.00", "60.00"),
            ratio("V1", "0.75"));

    /** One branch's verdict on one case: null when every expectation holds. */
    private static String play(Path ir, boolean viaDrl, String caseDocument) throws Exception {
        return CaseRunner.playCase(ir, JSON.readTree(caseDocument), viaDrl);
    }
}
