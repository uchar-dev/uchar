package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.ObjectMapper;

import dev.uchar.regelspraak.engine.ScenarioResultaat;

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
 * gets Drools' ConsequenceException and names it through {@code DroolsBackend.benoem} (since 2026-10-02, no handler
 * on a class name: {@code NuldelerAfnemerWitnessTest}). A leeg divisor
 * stays leeg with its §9.5 deposit, and a leeg dividend over a zero divisor stays 0 (ch. 1.2). Every document is authored
 * here on Jutterswet material, with the outcomes ref and clingo give it.
 *
 * <p><b>Red before the fix</b> (measured 2026-10-02 on the backend of {@code da03d480}): the fire threw Drools'
 * {@code ConsequenceException} ("Exception executing consequence for rule "verhouding" in defaultpkg:
 * java.lang.ArithmeticException: Breuk deling door 0"), in a rule's value, in a default's consequence and in the
 * absolute division alike, and the scenario report carried that text.
 */
class NuldelerWitnessTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    /** The fault is the runtime's own named type, carrying the sentence as its message (by name, so this witness
     *  compiled against the backend before the type existed). */
    private static void benoemd(String zin, RuntimeException e) {
        assertEquals("DelingDoorNul", e.getClass().getSimpleName(), () -> e.getClass().getName() + ": " + e.getMessage());
        assertEquals(zin, e.getMessage());
    }

    private static String zin(String regel) {
        return "deling door nul in de regel \"" + regel + "\": 60 gedeeld door 0 — een numerieke nul als deler is een "
                + "fout, geen leeg (RUNTIME-SEMANTICS ch. 1.4, punt 4)";
    }

    @Test void een_nuldeler_in_de_waarde_van_een_regel() throws Exception {
        Path ir = document(regel(deling("dividedBy")), "");
        benoemd(zin("verhouding"), assertThrows(RuntimeException.class, () -> speel(ir, false, NUL_EN_GEWOON)));
    }

    @Test void een_nuldeler_in_de_consequentie_van_een_default() throws Exception {
        // "Afwijking van verhouding: gewogen" under the base 1,00, the division where the gewogen gewicht is gevuld. The
        // destructive exec-model branch carried the group as one rule, named by its target on its owner type, and gave
        // the division by zero that name. The truth-maintenance branch refuses a division in a default by name: it
        // computes a default as soon as what it reads stands, so a division by zero there is a fault on an intermediate
        // state, and whether it counts waits on the norm decision of card 20261008-fout-op-tussenstand.
        Path ir = document("", groep(standaard(gevuld("gewogen gewicht"), deling("dividedBy")), standaard(ALTIJD, EEN)));
        CaseRunner.Excluded e = assertThrows(CaseRunner.Excluded.class, () -> CaseRunner.speelCase(ir,
                JSON.readTree(NUL_EN_GEWOON), DroolsBackend.Branch.EXEC_MODEL));
        org.junit.jupiter.api.Assertions.assertTrue(e.getMessage().contains("defaults: defaults A:Vondst.verhouding — een"
                + " deling in een default ('zijn gewogen gewicht gedeeld door zijn geschatte gewicht'): "), e.getMessage());
    }

    @Test void een_nuldeler_in_de_absolute_deling() throws Exception {
        Path ir = document(regel(deling("dividedByAbs")), "");
        benoemd(zin("verhouding"), assertThrows(RuntimeException.class, () -> speel(ir, false, NUL_EN_GEWOON)));
    }

    @Test void de_drl_teksttak_noemt_dezelfde_fout() throws Exception {
        Path ir = document(regel(deling("dividedBy")), "");
        benoemd(zin("verhouding"), assertThrows(RuntimeException.class, () -> speel(ir, true, NUL_EN_GEWOON)));
    }

    @Test void het_scenariorapport_draagt_de_zin() throws Exception {
        Path ir = document(regel(deling("dividedBy")), "");
        Path c = Files.writeString(Files.createTempDirectory("nuldeler").resolve("case1-nul.json"), NUL_EN_GEWOON);
        List<ScenarioResultaat> rapport = CaseRunner.rapport(ir, List.of(c));
        assertEquals(ScenarioResultaat.Status.FAIL, rapport.get(0).status(), rapport::toString);
        assertEquals("case1-nul.json: ERROR DelingDoorNul: " + zin("verhouding"), rapport.get(0).diagnose());
    }

    @Test void de_rode_route_houdt_de_vastgepinde_fragmenten() throws Exception {
        // gate 8 pins "Breuk deling door 0" and "deling door 0" on three red cases: the chain still carries them
        Path ir = document(regel(deling("dividedBy")), "");
        Path c = Files.writeString(Files.createTempFile("nuldeler", ".json"), NUL_EN_GEWOON);
        CaseRunner.RoodResult r = CaseRunner.runRood(ir, c);
        assertEquals("FAULT", r.status(), r::detail);
        assertTrue(r.detail().startsWith("DelingDoorNul: " + zin("verhouding")), r.detail());
        assertTrue(r.detail().contains("Breuk deling door 0"), r.detail());
    }

    @Test void een_lege_teller_boven_een_nuldeler_blijft_nul() throws Exception {
        // ch. 1.2: the dividend is tested first and coalesces, so leeg ÷ 0 = 0; ref and clingo: V1 0,75, V3 0,00
        Path ir = document(regel(deling("dividedBy")), "");
        String zaak = zaak(vondst("V1", "320.00", "240.00") + "," + vondst("V3", "0.00", null),
                verhouding("V1", "0.75") + "," + verhouding("V3", "0.00"));
        assertNull(speel(ir, false, zaak), "de lege teller");
    }

    // ---------------- documents ----------------

    private static final String TWEE_DECIMALEN = "{\"node\":\"Number\",\"whole\":false,\"decimals\":2,\"sign\":\"unspecified\"}";

    private static final String MODEL = "{\"objectTypes\":[{\"node\":\"ObjectType\",\"name\":\"Vondst\",\"animate\":false,"
            + "\"plural\":\"Vondsten\",\"attributes\":[" + attribuut("geschatte gewicht") + "," + attribuut("gewogen gewicht") + ","
            + attribuut("verhouding") + "],\"characteristics\":[]}],\"factTypes\":[],\"parameters\":[],\"domains\":[],"
            + "\"unitSystems\":[],\"dimensions\":[],\"timelines\":[],\"dayKinds\":[]}";

    private static String attribuut(String naam) {
        return "{\"node\":\"Attribute\",\"name\":\"" + naam + "\",\"datatype\":" + TWEE_DECIMALEN + "}";
    }

    private static String eigen(String attribuut) {
        return "{\"node\":\"AttributeReference\",\"attribute\":\"" + attribuut + "\",\"subjectType\":\"Vondst\","
                + "\"source\":{\"snippet\":\"zijn " + attribuut + "\"}}";
    }

    /** "zijn gewogen gewicht gedeeld door [(ABS)] zijn geschatte gewicht". */
    private static String deling(String op) {
        return "{\"node\":\"Binary\",\"op\":\"" + op + "\",\"left\":" + eigen("gewogen gewicht") + ",\"right\":"
                + eigen("geschatte gewicht") + ",\"type\":" + TWEE_DECIMALEN + ",\"source\":{\"snippet\":\"zijn gewogen gewicht"
                + " gedeeld door zijn geschatte gewicht\"}}";
    }

    private static String gevuld(String attribuut) {
        return "{\"node\":\"Predicate\",\"kind\":\"filled\",\"operand\":" + eigen(attribuut)
                + ",\"negated\":false,\"digits\":0,\"source\":{\"snippet\":\"zijn " + attribuut + " gevuld is\"}}";
    }

    private static final String ALTIJD = "{\"node\":\"Compound\",\"quantifier\":\"all\",\"threshold\":0,\"parts\":[],"
            + "\"source\":{\"snippet\":\"altijd\"}}";

    /** The base "De verhouding van een Vondst moet gesteld worden op 1,00". */
    private static final String EEN = "{\"node\":\"Literal\",\"value\":{\"number\":\"1.00\"},\"type\":" + TWEE_DECIMALEN
            + ",\"source\":{\"snippet\":\"1,00\"}}";

    private static String standaard(String toets, String gevolg) {
        return "{\"node\":\"Default\",\"justification\":" + toets + ",\"consequence\":" + gevolg + "}";
    }

    /** The defaults group on the verhouding of a Vondst, highest default first. */
    private static String groep(String... standaarden) {
        return "{\"node\":\"PrioritizedDefaults\",\"name\":\"verhouding\",\"subject\":\"Vondst\",\"attribute\":"
                + "\"verhouding\",\"defaults\":[" + String.join(",", standaarden) + "],\"validity\":{\"node\":\"Validity\"}}";
    }

    /** The rule "verhouding" with {@code waarde} as its value, without a condition. */
    private static String regel(String waarde) {
        return "{\"node\":\"Rule\",\"name\":\"verhouding\",\"versions\":[{\"validity\":{\"node\":\"Validity\"},\"target\":"
                + "{\"node\":\"AttributeReference\",\"attribute\":\"verhouding\",\"subjectType\":\"Vondst\",\"source\":"
                + "{\"snippet\":\"De verhouding van een Vondst\"}},\"value\":" + waarde + ",\"initialisation\":false,"
                + "\"presentation\":\"calculation\"}]}";
    }

    private static Path document(String regels, String groepen) throws Exception {
        String ir = "{\"format\":\"uchar-ir-v1\",\"model\":" + MODEL + ",\"rules\":{\"rules\":[" + regels + "],"
                + "\"dayKindDefinitions\":[],\"objectCreations\":[],\"allocations\":[],\"consistencies\":[],\"uniquenesses\":[],"
                + "\"factCreations\":[],\"startPointDeterminations\":[],\"ruleGroups\":[],\"prioritizedDefaults\":[" + groepen
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

    private static String verhouding(String label, String waarde) {
        return "{\"kind\":\"attributeValue\",\"label\":\"" + label + "\",\"attribute\":\"verhouding\",\"value\":{\"number\":\""
                + waarde + "\"}}";
    }

    private static String zaak(String instanties, String verwacht) {
        return "{\"format\":\"uchar-case-v0\",\"asOfDate\":null,\"input\":{\"instances\":[" + instanties
                + "],\"parameters\":{}},\"expected\":[" + verwacht + "]}";
    }

    /** V1 (240 by 320, 0,75) beside V3, whose geschatte gewicht is 0,00 under a gewogen gewicht of 60,00. */
    private static final String NUL_EN_GEWOON = zaak(vondst("V1", "320.00", "240.00") + "," + vondst("V3", "0.00", "60.00"),
            verhouding("V1", "0.75"));

    /** One branch's verdict on one case: null when every expectation holds. */
    private static String speel(Path ir, boolean viaDrl, String zaak) throws Exception {
        return CaseRunner.speelCase(ir, JSON.readTree(zaak), viaDrl);
    }
}
