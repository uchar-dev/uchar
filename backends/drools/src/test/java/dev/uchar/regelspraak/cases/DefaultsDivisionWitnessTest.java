package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A division, or a root, in a default: "Afwijking van verhouding: De verhouding van een Vondst moet berekend worden als
 * zijn gewogen gewicht gedeeld door zijn geschatte gewicht indien …" under the base "De verhouding van een Vondst moet
 * gesteld worden op 1,00". Over a leeg divisor the consequence is leeg, so the default falls through, and its deposit on
 * the §9.5 channel does not persist (RUNTIME-SEMANTICS ch. 7.1); a default that decides with a deposit keeps it. Every
 * document here is authored in this class on Jutterswet material, with the values {@code ref} and clingo answer; the
 * square root stays refused: this branch has no §6.6 deposit for a default. Every row runs on the exec-model branch and on
 * {@code DRL_TEXT}, its rendering as DRL text, with the same verdict. The corpus witness of the division is the golden
 * {@code jutterswet-deling-in-afwijking}, in both orders of its rules.
 */
class DefaultsDivisionWitnessTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    @Test void a_division_in_a_deviation() throws Exception {
        // The division under "zijn gewogen gewicht is gevuld" and "zijn geschatte gewicht is groter dan 0" over the base
        // 1,00 ("Uitzondering op" lowers to the same group). ref and clingo: V1 (240 by 320) 0,75, V2 1,00.
        String group = group(defaultEntry(all(filled("gewogen gewicht"), greaterThanZero("geschatte gewicht")), division()),
                defaultEntry(ALTIJD, EEN));
        played(group, caseDocument(vondst("V1", "320.00", "240.00") + "," + vondst("V2", "50.00", null),
                ratio("V1", "0.75") + "," + ratio("V2", "1.00")));
    }

    @Test void a_division_in_the_base_rule() throws Exception {
        // The division as the base, under "Afwijking van verhouding: niet gewogen" (1,00 where the gewogen gewicht is
        // leeg). ref and clingo: V1 0,75, V2 1,00.
        String group = group(defaultEntry(emptyTest("gewogen gewicht"), EEN),
                defaultEntry(all(filled("gewogen gewicht"), greaterThanZero("geschatte gewicht")), division()));
        played(group, caseDocument(vondst("V1", "320.00", "240.00") + "," + vondst("V2", "50.00", null),
                ratio("V1", "0.75") + "," + ratio("V2", "1.00")));
    }

    @Test void a_division_with_an_empty_divisor() throws Exception {
        // The division under "zijn gewogen gewicht gevuld is" alone: V3 has no geschat gewicht, so its consequence is leeg,
        // the default falls through to 1,00, and nothing persists on the channel (ch. 7.1). ref: V1 0,75, V2 and V3 1,00,
        // consistent.
        String group = group(defaultEntry(filled("gewogen gewicht"), division()), defaultEntry(ALTIJD, EEN));
        played(group, caseDocument(vondst("V1", "320.00", "240.00") + "," + vondst("V2", "50.00", null) + ","
                        + vondst("V3", null, "60.00"),
                ratio("V1", "0.75") + "," + ratio("V2", "1.00") + "," + ratio("V3", "1.00") + "," + CONSISTENT));
    }

    @Test void a_rounded_division() throws Exception {
        // "… gedeeld door zijn geschatte gewicht naar beneden afgerond op 2 decimalen". ref and clingo: V1 (200 by 300)
        // 0,66, V2 and V3 1,00.
        String group = group(defaultEntry(filled("gewogen gewicht"), rounded(division())), defaultEntry(ALTIJD, EEN));
        played(group, caseDocument(vondst("V1", "300.00", "200.00") + "," + vondst("V2", "50.00", null) + ","
                        + vondst("V3", null, "60.00"),
                ratio("V1", "0.66") + "," + ratio("V2", "1.00") + "," + ratio("V3", "1.00")));
    }

    @Test void a_square_root() throws Exception {
        // "… de wortel van zijn gewogen gewicht naar beneden afgerond op 2 decimalen indien zijn gewogen gewicht gevuld
        // is": V3's radicand is negative, its consequence leeg, and the default falls through. ref: V1 (240) 15,49, V2
        // and V3 1,00, consistent; clingo refuses a rounded root by name.
        String squareRoot = "{\"node\":\"BuiltinCall\",\"kind\":\"squareRoot\",\"arguments\":[" + own("gewogen gewicht")
                + "],\"type\":{\"node\":\"Number\",\"whole\":false,\"decimals\":0,\"sign\":\"nonNegative\"},\"source\":"
                + "{\"snippet\":\"de wortel van zijn gewogen gewicht\"}}";
        String group = group(defaultEntry(filled("gewogen gewicht"), rounded(squareRoot)), defaultEntry(ALTIJD, EEN));
        refused(group, caseDocument(vondst("V1", null, "240.00") + "," + vondst("V2", "50.00", null) + ","
                        + vondst("V3", null, "-16.00"),
                ratio("V1", "15.49") + "," + ratio("V2", "1.00") + "," + ratio("V3", "1.00") + "," + CONSISTENT),
                "een wortel in een default ('de wortel van zijn gewogen gewicht'), waarvan deze tak de §6.6-deponering "
                        + "niet maakt: een default deponeert alleen waar hij beslist (RUNTIME-SEMANTICS ch. 7.1)");
    }

    @Test void a_division_under_another_operation_decides_with_its_deposit() throws Exception {
        // "(zijn gewogen gewicht gedeeld door zijn geschatte gewicht) plus 5,00": over a leeg divisor the default decides
        // 5,00 and its deposit persists (ref and clingo report the inconsistency), under the target form of the stack.
        String plus = "{\"node\":\"Binary\",\"op\":\"plus\",\"left\":" + division() + ",\"right\":{\"node\":\"Literal\",\"value\":"
                + "{\"number\":\"5.00\"},\"type\":" + TWO_DECIMALS + ",\"source\":{\"snippet\":\"5,00\"}},\"type\":"
                + TWO_DECIMALS + ",\"source\":{\"snippet\":\"(…) plus 5,00\"}}";
        String group = group(defaultEntry(filled("gewogen gewicht"), plus), defaultEntry(ALTIJD, EEN));
        played(group, caseDocument(vondst("V1", "320.00", "240.00"), ratio("V1", "5.75") + "," + CONSISTENT));
        played(group, caseDocument(vondst("V3", null, "60.00"), ratio("V3", "5.00") + ","
                + "{\"kind\":\"inconsistent\",\"rule\":\"Vondst.verhouding\",\"label\":\"V3\",\"code\":\"EF-031\"}"));
    }

    @Test void a_default_that_a_higher_one_displaces_does_not_deposit() throws Exception {
        // "indien 5,00 gedeeld door zijn geschatte gewicht leeg is" below a base that decides first: the justification
        // of the lower default is not evaluated, V3 has no deposit. Over the same division as the highest default the
        // default decides and its deposit stands: V4.
        String emptyDivision = "{\"node\":\"Predicate\",\"kind\":\"empty\",\"operand\":" + division()
                + ",\"negated\":false,\"digits\":0,\"source\":{\"snippet\":\"leeg\"}}";
        played(group(defaultEntry(ALTIJD, EEN), defaultEntry(emptyDivision, EEN)),
                caseDocument(vondst("V3", null, "60.00"), ratio("V3", "1.00") + "," + CONSISTENT));
        played(group(defaultEntry(emptyDivision, EEN), defaultEntry(ALTIJD, EEN)),
                caseDocument(vondst("V4", "0.00", "60.00"), ratio("V4", "1.00") + ","
                        + "{\"kind\":\"inconsistent\",\"rule\":\"Vondst.verhouding\",\"label\":\"V4\",\"code\":\"EF-005\"}"));
    }

    // ---------------- documents ----------------

    private static final String TWO_DECIMALS = "{\"node\":\"Number\",\"whole\":false,\"decimals\":2,\"sign\":\"unspecified\"}";

    private static final String MODEL = "{\"objectTypes\":[{\"node\":\"ObjectType\",\"name\":\"Vondst\",\"animate\":false,"
            + "\"plural\":\"Vondsten\",\"attributes\":[" + attribute("geschatte gewicht") + "," + attribute("gewogen gewicht") + ","
            + attribute("verhouding") + "],\"characteristics\":[]}],\"factTypes\":[],\"parameters\":[],\"domains\":[],"
            + "\"unitSystems\":[],\"dimensions\":[],\"timelines\":[],\"dayKinds\":[]}";

    private static String attribute(String name) {
        return "{\"node\":\"Attribute\",\"name\":\"" + name + "\",\"datatype\":" + TWO_DECIMALS + "}";
    }

    private static String own(String attribute) {
        return "{\"node\":\"AttributeReference\",\"attribute\":\"" + attribute + "\",\"subjectType\":\"Vondst\","
                + "\"source\":{\"snippet\":\"zijn " + attribute + "\"}}";
    }

    /** "zijn gewogen gewicht gedeeld door zijn geschatte gewicht". */
    private static String division() {
        return "{\"node\":\"Binary\",\"op\":\"dividedBy\",\"left\":" + own("gewogen gewicht") + ",\"right\":"
                + own("geschatte gewicht") + ",\"type\":" + TWO_DECIMALS + ",\"source\":{\"snippet\":\"zijn gewogen gewicht"
                + " gedeeld door zijn geschatte gewicht\"}}";
    }

    /** "&lt;basis&gt; naar beneden afgerond op 2 decimalen". */
    private static String rounded(String base) {
        return "{\"node\":\"Rounding\",\"base\":" + base + ",\"mode\":\"floor\",\"decimals\":2,\"type\":" + TWO_DECIMALS
                + ",\"source\":{\"snippet\":\"naar beneden afgerond op 2 decimalen\"}}";
    }

    private static String filled(String attribute) {
        return "{\"node\":\"Predicate\",\"kind\":\"filled\",\"operand\":" + own(attribute)
                + ",\"negated\":false,\"digits\":0,\"source\":{\"snippet\":\"zijn " + attribute + " gevuld is\"}}";
    }

    private static String emptyTest(String attribute) {
        return "{\"node\":\"Predicate\",\"kind\":\"empty\",\"operand\":" + own(attribute)
                + ",\"negated\":false,\"digits\":0,\"source\":{\"snippet\":\"zijn " + attribute + " leeg is\"}}";
    }

    private static String greaterThanZero(String attribute) {
        return "{\"node\":\"Comparison\",\"op\":\"greater\",\"left\":" + own(attribute) + ",\"right\":{\"node\":\"Literal\","
                + "\"value\":{\"number\":\"0\"},\"type\":{\"node\":\"Number\",\"whole\":true,\"decimals\":0,\"sign\":\"unspecified\"},"
                + "\"source\":{\"snippet\":\"0\"}},\"source\":{\"snippet\":\"zijn " + attribute + " is groter dan 0\"}}";
    }

    private static String all(String... parts) {
        return "{\"node\":\"Compound\",\"quantifier\":\"all\",\"threshold\":0,\"parts\":[" + String.join(",", parts)
                + "],\"source\":{\"snippet\":\"hij aan alle volgende voorwaarden voldoet\"}}";
    }

    private static final String ALTIJD = "{\"node\":\"Compound\",\"quantifier\":\"all\",\"threshold\":0,\"parts\":[],"
            + "\"source\":{\"snippet\":\"altijd\"}}";

    /** The base "De verhouding van een Vondst moet gesteld worden op 1,00". */
    private static final String EEN = "{\"node\":\"Literal\",\"value\":{\"number\":\"1.00\"},\"type\":" + TWO_DECIMALS
            + ",\"source\":{\"snippet\":\"1,00\"}}";

    private static String defaultEntry(String justification, String consequence) {
        return "{\"node\":\"Default\",\"justification\":" + justification + ",\"consequence\":" + consequence + "}";
    }

    /** The group on the verhouding of a Vondst, highest default first. */
    private static String group(String... defaults) {
        String group = "{\"node\":\"PrioritizedDefaults\",\"name\":\"verhouding\",\"subject\":\"Vondst\",\"attribute\":"
                + "\"verhouding\",\"defaults\":[" + String.join(",", defaults) + "],\"validity\":{\"node\":\"Validity\"}}";
        return "{\"format\":\"uchar-ir-v1\",\"model\":" + MODEL + ",\"rules\":{\"rules\":[],\"dayKindDefinitions\":[],"
                + "\"objectCreations\":[],\"allocations\":[],\"consistencies\":[],\"uniquenesses\":[],\"factCreations\":[],"
                + "\"startPointDeterminations\":[],\"ruleGroups\":[],\"prioritizedDefaults\":[" + group + "]},\"diagnostics\":[]}";
    }

    // ---------------- cases ----------------

    /** A Vondst with an optional geschat and gewogen gewicht. */
    private static String vondst(String label, String geschat, String weighted) {
        String a = (geschat == null ? "" : "\"geschatte gewicht\":{\"number\":\"" + geschat + "\"}")
                + (geschat != null && weighted != null ? "," : "")
                + (weighted == null ? "" : "\"gewogen gewicht\":{\"number\":\"" + weighted + "\"}");
        return "{\"label\":\"" + label + "\",\"objectType\":\"Vondst\",\"attributes\":{" + a + "},\"roles\":{},\"characteristics\":[]}";
    }

    private static String ratio(String label, String value) {
        return "{\"kind\":\"attributeValue\",\"label\":\"" + label + "\",\"attribute\":\"verhouding\",\"value\":{\"number\":\""
                + value + "\"}}";
    }

    private static final String CONSISTENT = "{\"kind\":\"consistent\"}";

    private static String caseDocument(String instances, String expected) {
        return "{\"format\":\"uchar-case-v0\",\"asOfDate\":null,\"input\":{\"instances\":[" + instances
                + "],\"parameters\":{}},\"expected\":[" + expected + "]}";
    }

    /** Both branches play the group {@code ir} on {@code caseDocument}, with exactly its expectations. */
    private static void played(String ir, String caseDocument) throws Exception {
        Path tmp = Files.createTempFile("defaults-deling", ".json");
        tmp.toFile().deleteOnExit();
        Files.writeString(tmp, ir);
        JsonNode c = JSON.readTree(caseDocument);
        for (DroolsBackend.Branch branch : List.of(DroolsBackend.Branch.EXEC_MODEL, DroolsBackend.Branch.DRL_TEXT))
            assertNull(CaseRunner.playCase(tmp, c, branch), branch.toString());
    }

    /** Both branches refuse the group {@code ir} by name, before {@code caseDocument} is replayed. */
    private static void refused(String ir, String caseDocument, String form) throws Exception {
        Path tmp = Files.createTempFile("defaults-deling", ".json");
        tmp.toFile().deleteOnExit();
        Files.writeString(tmp, ir);
        JsonNode c = JSON.readTree(caseDocument);
        for (DroolsBackend.Branch branch : List.of(DroolsBackend.Branch.EXEC_MODEL, DroolsBackend.Branch.DRL_TEXT)) {
            CaseRunner.Excluded e = assertThrows(CaseRunner.Excluded.class, () -> CaseRunner.playCase(tmp, c, branch),
                    branch.toString());
            assertTrue(e.getMessage().contains("defaults: defaults A:Vondst.verhouding — " + form), branch + ": "
                    + e.getMessage());
        }
    }
}
