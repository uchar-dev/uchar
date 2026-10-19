package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * A numeric comparison with the subject's own operand on the left and a navigated one on the right: "Een Vondst is
 * zwaar indien zijn gewicht groter is dan de afgeleide diepte van de haven van de jutter van de Vondst". The mirrored
 * sentence ("de afgeleide diepte van de haven van de jutter van de Vondst kleiner is dan zijn gewicht") and "zijn
 * gewicht maal 1 groter is dan …" mean the same and lowered; this one did not. Every document here is authored in this
 * class on Jutterswet material, with the values {@code ref} and clingo answer; the far value is derived by a rule, so
 * the comparison also needs the far-side join a navigated condition takes.
 *
 * <p><b>Red before the fix</b> (measured 2026-10-01 on the transpiler of {@code 2a0c6d60}, exec-model branch): every
 * form with the own operand on the left was refused ({@code regel (voorwaarde buiten Phase-B scope)}), in a
 * characteristic assignment and in an ordinary rule, over one role and over two, under groter, groter of gelijk and
 * ongelijk; in a subselection the fold was refused. The mirrored form and the product played before and play after.
 */
class ComparisonOwnLeftWitnessTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    @Test void a_characteristic_two_roles_far() throws Exception {
        // "Een Vondst is zwaar indien zijn gewicht groter is dan de afgeleide diepte van de haven van de jutter van de
        // Vondst". ref and clingo: V1 (12 against 10) zwaar; V2 (9), V3 (haven without diepte), V4 (jutter without
        // haven) not.
        assertNull(play(document(zwaarIf(comparison("greater", own("gewicht"), depthOfTheHaven()))),
                caseDocument(characteristic("V1", true) + "," + characteristic("V2", false) + "," + characteristic("V3", false) + "," + characteristic("V4", false))),
                "groter, twee rollen ver");
    }

    @Test void a_characteristic_check_under_greater_or_equal_and_not_equal() throws Exception {
        // "… groter of gelijk is aan …" and "… ongelijk is aan …": a leeg far value is ongelijk to a gewicht (RUNTIME-
        // SEMANTICS ch. 1.2). ref and clingo: V1 alone, and all four.
        assertNull(play(document(zwaarIf(comparison("greaterOrEqual", own("gewicht"), depthOfTheHaven()))),
                caseDocument(characteristic("V1", true) + "," + characteristic("V2", false) + "," + characteristic("V3", false) + "," + characteristic("V4", false))),
                "groter of gelijk");
        assertNull(play(document(zwaarIf(comparison("notEqual", own("gewicht"), depthOfTheHaven()))),
                caseDocument(characteristic("V1", true) + "," + characteristic("V2", true) + "," + characteristic("V3", true) + "," + characteristic("V4", true))),
                "ongelijk");
    }

    @Test void a_characteristic_a_role_far() throws Exception {
        // "… indien zijn gewicht groter is dan de afgeleide drempel van de jutter van de Vondst". ref and clingo: V1 (12
        // against 8) and V2 (9) zwaar, V3 and V4 (jutters without a drempel) not.
        assertNull(play(document(zwaarIf(comparison("greater", own("gewicht"), jutterThreshold()))),
                caseDocument(characteristic("V1", true) + "," + characteristic("V2", true) + "," + characteristic("V3", false) + "," + characteristic("V4", false))),
                "groter, één rol ver");
    }

    @Test void an_ordinary_rule() throws Exception {
        // "De uitkomst van een Vondst moet gesteld worden op zijn gewicht indien zijn gewicht groter is dan …", two roles
        // and one role away. ref and clingo: V1 12 and the others leeg; V1 12, V2 9, V3 and V4 leeg.
        assertNull(play(document(outcomeIf(comparison("greater", own("gewicht"), depthOfTheHaven()))),
                caseDocument(number("V1", 12) + "," + empty("V2") + "," + empty("V3") + "," + empty("V4"))), "twee rollen ver");
        assertNull(play(document(outcomeIf(comparison("greater", own("gewicht"), jutterThreshold()))),
                caseDocument(number("V1", 12) + "," + number("V2", 9) + "," + empty("V3") + "," + empty("V4"))), "één rol ver");
    }

    @Test void in_a_subselection() throws Exception {
        // "Het aantal zware vondsten van een Jutter moet berekend worden als het aantal vondsten van de Jutter die aan de
        // volgende voorwaarde voldoen: zijn gewicht is groter dan de afgeleide drempel van de jutter van de Vondst". ref
        // and clingo: J1 2, J2 and J3 0.
        String count = "{\"node\":\"Aggregation\",\"kind\":\"count\",\"arguments\":[{\"node\":\"AttributeReference\","
                + "\"attribute\":\"vondsten\",\"subjectType\":\"Jutter\",\"source\":{\"snippet\":\"vondsten van de Jutter\"}}],"
                + "\"elementType\":\"Vondst\",\"filter\":{\"node\":\"Compound\",\"quantifier\":\"the\",\"threshold\":0,\"parts\":["
                + comparison("greater", own("gewicht"), jutterThreshold()) + "],\"source\":{\"snippet\":\"aan de volgende"
                + " voorwaarde voldoen\"}},\"sourceRole\":" + roleBinding("JutterHeeftVondst", "vondst") + ",\"type\":{\"node\":"
                + "\"Number\",\"whole\":true,\"decimals\":0,\"sign\":\"nonNegative\"},\"source\":{\"snippet\":\"het aantal vondsten"
                + " van de Jutter die …\"}}";
        String rule = "{\"node\":\"Rule\",\"name\":\"aantal zware vondsten\",\"versions\":[{\"validity\":{\"node\":\"Validity\"},"
                + "\"target\":{\"node\":\"AttributeReference\",\"attribute\":\"aantal zware vondsten\",\"subjectType\":\"Jutter\","
                + "\"source\":{\"snippet\":\"Het aantal zware vondsten van een Jutter\"}},\"value\":" + count
                + ",\"initialisation\":false,\"presentation\":\"calculation\"}]}";
        assertNull(play(document(rule), caseDocument(counter("J1", 2) + "," + counter("J2", 0) + "," + counter("J3", 0))),
                "in een subselectie");
    }

    @Test void the_mirrored_form_and_the_product_already_played() throws Exception {
        // "… de afgeleide diepte van de haven van de jutter van de Vondst kleiner is dan zijn gewicht" and "… zijn gewicht
        // maal 1 groter is dan …". ref and clingo: V1 alone.
        String expected = characteristic("V1", true) + "," + characteristic("V2", false) + "," + characteristic("V3", false) + "," + characteristic("V4", false);
        assertNull(play(document(zwaarIf(comparison("less", depthOfTheHaven(), own("gewicht")))), caseDocument(expected)),
                "gespiegeld");
        String product = "{\"node\":\"Binary\",\"op\":\"times\",\"left\":" + own("gewicht") + ",\"right\":{\"node\":\"Literal\","
                + "\"value\":{\"number\":\"1\"},\"type\":" + WHOLE + ",\"source\":{\"snippet\":\"1\"}},\"type\":{\"node\":"
                + "\"Number\",\"whole\":false,\"decimals\":0,\"sign\":\"unspecified\"},\"source\":{\"snippet\":\"zijn gewicht maal 1\"}}";
        assertNull(play(document(zwaarIf(comparison("greater", product, depthOfTheHaven()))), caseDocument(expected)), "maal 1");
    }

    // ---------------- documents ----------------

    private static final String WHOLE = "{\"node\":\"Number\",\"whole\":true,\"decimals\":0,\"sign\":\"unspecified\"}";

    private static String attribute(String name) {
        return "{\"node\":\"Attribute\",\"name\":\"" + name + "\",\"datatype\":" + WHOLE + "}";
    }

    private static String roleBinding(String factType, String role) {
        return "{\"node\":\"RoleBinding\",\"factType\":\"" + factType + "\",\"role\":\"" + role + "\"}";
    }

    /** Haven (diepte, afgeleide diepte), Jutter (drempel, afgeleide drempel, aantal zware vondsten) and Vondst (gewicht,
     *  uitkomst, kenmerk zwaar). A Vondst has one jutter, a Jutter one haven. */
    private static final String MODEL = "{\"objectTypes\":["
            + "{\"node\":\"ObjectType\",\"name\":\"Haven\",\"animate\":false,\"plural\":\"Havens\",\"attributes\":["
            + attribute("diepte") + "," + attribute("afgeleide diepte") + "],\"characteristics\":[]},"
            + "{\"node\":\"ObjectType\",\"name\":\"Jutter\",\"animate\":true,\"plural\":\"Jutters\",\"attributes\":["
            + attribute("drempel") + "," + attribute("afgeleide drempel") + "," + attribute("aantal zware vondsten")
            + "],\"characteristics\":[]},"
            + "{\"node\":\"ObjectType\",\"name\":\"Vondst\",\"animate\":false,\"plural\":\"Vondsten\",\"attributes\":["
            + attribute("gewicht") + "," + attribute("uitkomst") + "],\"characteristics\":[{\"node\":\"Characteristic\","
            + "\"name\":\"zwaar\",\"kind\":\"adjectival\"}]}],\"factTypes\":["
            + "{\"node\":\"FactType\",\"name\":\"JutterHeeftVondst\",\"reciprocal\":false,\"roles\":[{\"node\":\"Role\","
            + "\"name\":\"jutter\",\"objectType\":\"Jutter\",\"many\":false},{\"node\":\"Role\",\"name\":\"vondst\",\"objectType\":"
            + "\"Vondst\",\"plural\":\"vondsten\",\"many\":true}]},"
            + "{\"node\":\"FactType\",\"name\":\"HavenHeeftJutter\",\"reciprocal\":false,\"roles\":[{\"node\":\"Role\","
            + "\"name\":\"haven\",\"objectType\":\"Haven\",\"many\":false},{\"node\":\"Role\",\"name\":\"havenjutter\",\"objectType\":"
            + "\"Jutter\",\"plural\":\"havenjutters\",\"many\":true}]}],"
            + "\"parameters\":[],\"domains\":[],\"unitSystems\":[],\"dimensions\":[],\"timelines\":[],\"dayKinds\":[]}";

    private static String own(String attribute) {
        return "{\"node\":\"AttributeReference\",\"attribute\":\"" + attribute + "\",\"subjectType\":\"Vondst\","
                + "\"source\":{\"snippet\":\"zijn " + attribute + "\"}}";
    }

    /** "de afgeleide diepte van de haven van de jutter van de Vondst": two roles. */
    private static String depthOfTheHaven() {
        return "{\"node\":\"AttributeReference\",\"attribute\":\"afgeleide diepte\",\"subjectType\":\"Haven\",\"source\":"
                + "{\"snippet\":\"de afgeleide diepte van de haven van de jutter van de Vondst\"},\"path\":["
                + roleBinding("JutterHeeftVondst", "jutter") + "," + roleBinding("HavenHeeftJutter", "haven") + "]}";
    }

    /** "de afgeleide drempel van de jutter van de Vondst": one role. */
    private static String jutterThreshold() {
        return "{\"node\":\"AttributeReference\",\"attribute\":\"afgeleide drempel\",\"subjectType\":\"Jutter\",\"source\":"
                + "{\"snippet\":\"de afgeleide drempel van de jutter van de Vondst\"},\"path\":[" + roleBinding("JutterHeeftVondst", "jutter")
                + "]}";
    }

    private static String comparison(String op, String left, String right) {
        return "{\"node\":\"Comparison\",\"op\":\"" + op + "\",\"left\":" + left + ",\"right\":" + right
                + ",\"source\":{\"snippet\":\"vergelijking " + op + "\"}}";
    }

    /** "Een Vondst is zwaar indien &lt;voorwaarde&gt;." */
    private static String zwaarIf(String condition) {
        return "{\"node\":\"Rule\",\"name\":\"zwaar\",\"versions\":[{\"validity\":{\"node\":\"Validity\"},\"target\":"
                + "{\"node\":\"AttributeReference\",\"attribute\":\"zwaar\",\"subjectType\":\"Vondst\",\"source\":{\"snippet\":"
                + "\"Een Vondst is zwaar\"}},\"value\":{\"node\":\"Literal\",\"value\":{\"boolean\":true},\"type\":{\"node\":"
                + "\"Boolean\"},\"source\":{\"snippet\":\"waar\"}},\"condition\":" + condition + ",\"initialisation\":false}]}";
    }

    /** "De uitkomst van een Vondst moet gesteld worden op zijn gewicht indien &lt;voorwaarde&gt;." */
    private static String outcomeIf(String condition) {
        return "{\"node\":\"Rule\",\"name\":\"uitkomst\",\"versions\":[{\"validity\":{\"node\":\"Validity\"},\"target\":"
                + "{\"node\":\"AttributeReference\",\"attribute\":\"uitkomst\",\"subjectType\":\"Vondst\",\"source\":{\"snippet\":"
                + "\"De uitkomst van een Vondst\"}},\"value\":" + own("gewicht") + ",\"condition\":" + condition
                + ",\"initialisation\":false,\"presentation\":\"equation\"}]}";
    }

    /** The reader {@code reader} before the two rules that derive the far values from the input. */
    private static String document(String reader) {
        String depth = "{\"node\":\"Rule\",\"name\":\"afgeleide diepte\",\"versions\":[{\"validity\":{\"node\":\"Validity\"},"
                + "\"target\":{\"node\":\"AttributeReference\",\"attribute\":\"afgeleide diepte\",\"subjectType\":\"Haven\",\"source\":"
                + "{\"snippet\":\"De afgeleide diepte van een Haven\"}},\"value\":{\"node\":\"AttributeReference\",\"attribute\":"
                + "\"diepte\",\"subjectType\":\"Haven\",\"source\":{\"snippet\":\"zijn diepte\"}},\"initialisation\":false,"
                + "\"presentation\":\"equation\"}]}";
        String threshold = "{\"node\":\"Rule\",\"name\":\"afgeleide drempel\",\"versions\":[{\"validity\":{\"node\":\"Validity\"},"
                + "\"target\":{\"node\":\"AttributeReference\",\"attribute\":\"afgeleide drempel\",\"subjectType\":\"Jutter\","
                + "\"source\":{\"snippet\":\"De afgeleide drempel van een Jutter\"}},\"value\":{\"node\":\"AttributeReference\","
                + "\"attribute\":\"drempel\",\"subjectType\":\"Jutter\",\"source\":{\"snippet\":\"zijn drempel\"}},"
                + "\"initialisation\":false,\"presentation\":\"equation\"}]}";
        return "{\"format\":\"uchar-ir-v1\",\"model\":" + MODEL + ",\"rules\":{\"rules\":[" + String.join(",", List.of(reader, depth, threshold))
                + "],\"dayKindDefinitions\":[],\"objectCreations\":[],\"allocations\":[],\"consistencies\":[],\"uniquenesses\":[],"
                + "\"factCreations\":[],\"startPointDeterminations\":[],\"ruleGroups\":[],\"prioritizedDefaults\":[]},\"diagnostics\":[]}";
    }

    // ---------------- cases ----------------

    private static String instance(String label, String type, String attributes, String roles) {
        return "{\"label\":\"" + label + "\",\"objectType\":\"" + type + "\",\"attributes\":{" + attributes + "},\"roles\":{"
                + roles + "},\"characteristics\":[]}";
    }

    /** H1 (diepte 10) and H2 (none); J1 (drempel 8, haven H1), J2 (haven H2) and J3 (no haven); V1 (gewicht 12) and V2
     *  (9) of J1, V3 (12) of J2, V4 (12) of J3. */
    private static final List<String> SITUATIE = List.of(
            instance("H1", "Haven", "\"diepte\":{\"number\":\"10\"}", ""), instance("H2", "Haven", "", ""),
            instance("J1", "Jutter", "\"drempel\":{\"number\":\"8\"}", "\"haven\":[\"H1\"]"),
            instance("J2", "Jutter", "", "\"haven\":[\"H2\"]"), instance("J3", "Jutter", "", ""),
            instance("V1", "Vondst", "\"gewicht\":{\"number\":\"12\"}", "\"jutter\":[\"J1\"]"),
            instance("V2", "Vondst", "\"gewicht\":{\"number\":\"9\"}", "\"jutter\":[\"J1\"]"),
            instance("V3", "Vondst", "\"gewicht\":{\"number\":\"12\"}", "\"jutter\":[\"J2\"]"),
            instance("V4", "Vondst", "\"gewicht\":{\"number\":\"12\"}", "\"jutter\":[\"J3\"]"));

    private static String characteristic(String label, boolean present) {
        return "{\"kind\":\"" + (present ? "characteristicPresent" : "characteristicAbsent") + "\",\"label\":\"" + label
                + "\",\"characteristic\":\"zwaar\"}";
    }

    private static String number(String label, int n) {
        return "{\"kind\":\"attributeValue\",\"label\":\"" + label + "\",\"attribute\":\"uitkomst\",\"value\":{\"number\":\"" + n + "\"}}";
    }

    private static String empty(String label) {
        return "{\"kind\":\"attributeEmpty\",\"label\":\"" + label + "\",\"attribute\":\"uitkomst\"}";
    }

    private static String counter(String label, int n) {
        return "{\"kind\":\"attributeValue\",\"label\":\"" + label + "\",\"attribute\":\"aantal zware vondsten\",\"value\":"
                + "{\"number\":\"" + n + "\"}}";
    }

    private static String caseDocument(String expected) {
        return "{\"format\":\"uchar-case-v0\",\"asOfDate\":null,\"input\":{\"instances\":[" + String.join(",", SITUATIE)
                + "],\"parameters\":{}},\"expected\":[" + expected + "]}";
    }

    /** The exec-model branch's verdict on one case: null when every expectation holds. */
    private static String play(String ir, String caseDocument) throws Exception {
        Path tmp = Files.createTempFile("vergelijking-eigen-links", ".json");
        tmp.toFile().deleteOnExit();
        Files.writeString(tmp, ir);
        JsonNode c = JSON.readTree(caseDocument);
        return CaseRunner.playCase(tmp, c, false);
    }
}
