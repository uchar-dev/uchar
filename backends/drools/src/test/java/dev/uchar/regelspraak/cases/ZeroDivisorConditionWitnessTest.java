package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.ObjectMapper;

import dev.uchar.regelspraak.engine.ScenarioResult;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * A division by a numeric zero in a CONDITION: "Een Vondst is zwaar indien zijn gewogen gewicht gedeeld door zijn
 * geschatte gewicht groter is dan 1", over V3, whose geschatte gewicht is 0,00 under a gewogen gewicht of 60,00. Since
 * the card zero-division-channel (2026-10-19) the quotient is leeg, so the comparison does not hold, and the condition
 * deposits a §9.5 fact of kind EF-005 on the rule and the subject that evaluates it (RUNTIME-SEMANTICS ch. 1.4, point
 * 4), in ref, in clingo and on both branches of this backend, in every form below; the probe of the deposit rule hears
 * the code where the rule's consequence evaluates the condition again. Until that card ref faulted with "division by
 * numeric zero", clingo with its fail-fast guard, and this backend in the sentence below the history. On the destructive lowering a condition is tested in a constraint and not in a
 * consequence, so the fault leaves the fire as Drools' {@code ConstraintEvaluationException} and not as the
 * {@code ConsequenceException} that {@code ZeroDivisorWitnessTest} pins. Until card tms-omschakeling (2026-10-10) the
 * predicates of the destructive exec-model branch carried their rule: Drools' message named the rule and the
 * condition, and the replay, the red path and the scenario report read the sentence a division in a rule's value
 * gives, with the rule and the operands. The exec-model branch with truth maintenance tests the condition in a rule of
 * its own and keeps the fault as a fact, which its write-back throws at the fixpoint in that same sentence; the row of
 * a consumer that fires itself pins that branch since that card. Every document is written here
 * on Jutterswet material, with the outcome ref and clingo give it.
 *
 * <p><b>Red before the fix</b> (measured 2026-10-02 on the backend of {@code 70a60dc3}): the exec-model branch left
 * every form with "ConstraintEvaluationException: Error evaluating constraint '' in  and in more rules", naming
 * neither the rule nor the condition; the DRL-text branch named both, and still does.
 */
class ZeroDivisorConditionWitnessTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    /** V3 is not zwaar, and the rule zwaar deposits EF-005 on V3. */
    private static final String V3_NOT_ZWAAR = "{\"kind\":\"characteristicAbsent\",\"label\":\"V3\",\"characteristic\":"
            + "\"zwaar\"}," + TmsDocuments.inconsistent("zwaar", "V3", "EF-005");

    private static final String DIVISION_GREATER_THAN_ONE = greaterThan(division(own("gewogen gewicht"), own("geschatte gewicht")),
            number("1"), "zijn gewogen gewicht gedeeld door zijn geschatte gewicht groter is dan 1");

    @Test void a_comparison_with_a_number() throws Exception {
        Path ir = document(zwaar(DIVISION_GREATER_THAN_ONE));
        assertNull(play(ir, false, zeroAndOrdinary(V3_NOT_ZWAAR)));
    }

    @Test void the_condition_of_an_ordinary_rule() throws Exception {
        // "De verhouding van een Vondst moet gesteld worden op 1,00 indien …"
        Path ir = document(rule("verhouding", "Vondst", "verhouding", "De verhouding van een Vondst",
                "{\"node\":\"Literal\",\"value\":{\"number\":\"1.00\"},\"type\":" + TWO_DECIMALS + ",\"source\":{\"snippet\":"
                        + "\"1,00\"}}", DIVISION_GREATER_THAN_ONE, ",\"presentation\":\"equation\""));
        assertNull(play(ir, false, zeroAndOrdinary("{\"kind\":\"attributeEmpty\",\"label\":\"V3\",\"attribute\":"
                + "\"verhouding\"}," + TmsDocuments.inconsistent("verhouding", "V3", "EF-005"))));
    }

    @Test void a_comparison_with_a_parameter() throws Exception {
        // "… groter is dan de drempel", the drempel 1,00: the parameter is a pattern variable beside the subject
        Path ir = document(zwaar(greaterThan(division(own("gewogen gewicht"), own("geschatte gewicht")),
                "{\"node\":\"ParameterReference\",\"parameter\":\"drempel\",\"source\":{\"snippet\":\"de drempel\"}}",
                "zijn gewogen gewicht gedeeld door zijn geschatte gewicht groter is dan de drempel")));
        assertNull(play(ir, false, zeroAndOrdinary(V3_NOT_ZWAAR)));
    }

    @Test void under_a_counting_quantifier() throws Exception {
        // "… indien hij aan ten minste één van de volgende voorwaarden voldoet: - zijn gewogen gewicht is leeg - …"
        String empty = "{\"node\":\"Predicate\",\"kind\":\"empty\",\"operand\":" + own("gewogen gewicht") + ",\"negated\":"
                + "false,\"digits\":0,\"source\":{\"snippet\":\"zijn gewogen gewicht is leeg\"}}";
        Path ir = document(zwaar("{\"node\":\"Compound\",\"quantifier\":\"atLeast\",\"threshold\":1,\"parts\":[" + empty + ","
                + DIVISION_GREATER_THAN_ONE + "],\"source\":{\"snippet\":\"hij aan ten minste één van de volgende voorwaarden "
                + "voldoet\"}}"));
        assertNull(play(ir, false, zeroAndOrdinary(V3_NOT_ZWAAR)));
    }

    @Test void in_the_subselection_of_a_fold() throws Exception {
        // "Het aantal zware vondsten van een Jutter moet berekend worden als het aantal vondsten van de Jutter die aan de
        // volgende voorwaarde voldoen: zijn gewogen gewicht gedeeld door zijn geschatte gewicht is groter dan 1"
        String filter = "{\"node\":\"Compound\",\"quantifier\":\"the\",\"threshold\":0,\"parts\":[" + DIVISION_GREATER_THAN_ONE
                + "],\"source\":{\"snippet\":\"aan de volgende voorwaarde voldoen\"}}";
        String count = "{\"node\":\"Aggregation\",\"kind\":\"count\",\"arguments\":[{\"node\":\"AttributeReference\","
                + "\"attribute\":\"vondsten\",\"subjectType\":\"Jutter\",\"source\":{\"snippet\":\"vondsten van de Jutter\"}}],"
                + "\"elementType\":\"Vondst\",\"filter\":" + filter + ",\"sourceRole\":" + ROLE_VONDST + ",\"type\":{\"node\":"
                + "\"Number\",\"whole\":true,\"decimals\":0,\"sign\":\"nonNegative\"},\"source\":{\"snippet\":\"het aantal "
                + "vondsten van de Jutter die aan de volgende voorwaarde voldoen\"}}";
        Path ir = document(rule("zware vondsten", "Jutter", "aantal zware vondsten", "Het aantal zware vondsten van een "
                + "Jutter", count, null, ",\"presentation\":\"calculation\""));
        // V1 counts (400 by 320), V3 does not and deposits under the rule that reads the fold, on J1
        assertNull(play(ir, false, zeroAndOrdinary("{\"kind\":\"attributeValue\",\"label\":\"J1\",\"attribute\":"
                + "\"aantal zware vondsten\",\"value\":{\"number\":\"1\"}}," + TmsDocuments.inconsistent("zware vondsten",
                "J1", "EF-005"))));
    }

    @Test void in_the_matrix_of_a_quantifier_fold() throws Exception {
        // "Een Jutter is ijverig indien het gewogen gewicht van ten minste één van de vondsten van de Jutter groter is dan
        // 100 gedeeld door de verhouding van de Jutter", the verhouding of J1 0,00
        String matrix = greaterThan("{\"node\":\"AttributeReference\",\"attribute\":\"gewogen gewicht\",\"subjectType\":"
                + "\"Vondst\",\"source\":{\"snippet\":\"het gewogen gewicht van ten minste één van de vondsten van de Jutter\"}}",
                division(number("100"), viaJutter("verhouding")), "het gewogen gewicht van ten minste één van de vondsten van "
                        + "de Jutter groter is dan 100 gedeeld door de verhouding van de Jutter");
        String quantifier = "{\"node\":\"QuantifierFold\",\"quantifier\":\"atLeast\",\"threshold\":1,\"elementType\":\"Vondst\","
                + "\"test\":" + matrix + ",\"source\":{\"snippet\":\"ten minste één van de vondsten van de Jutter\"},"
                + "\"sourceRole\":" + ROLE_VONDST + "}";
        Path ir = document(rule("ijverig", "Jutter", "ijverig", "Een Jutter is ijverig", WAAR, quantifier, ""));
        assertNull(play(ir, false, caseDocument(jutter("0.00") + "," + vondst("V1", "320.00", "400.00", true),
                "{\"kind\":\"characteristicAbsent\",\"label\":\"J1\",\"characteristic\":\"ijverig\"},"
                        + TmsDocuments.inconsistent("ijverig", "J1", "EF-005"))));
    }

    @Test void over_a_role() throws Exception {
        // "… indien zijn gewogen gewicht gedeeld door de verhouding van de jutter van de Vondst groter is dan 1", V3 of
        // J1 with a verhouding of 0,00 (V1 has no jutter: its divisor is leeg, ch. 1.2, which deposits EF-031)
        Path ir = document(zwaar(greaterThan(division(own("gewogen gewicht"), viaJutter("verhouding")), number("1"),
                "zijn gewogen gewicht gedeeld door de verhouding van de jutter van de Vondst groter is dan 1")));
        assertNull(play(ir, false, caseDocument(jutter("0.00") + "," + vondst("V1", "320.00", "400.00", false) + ","
                + vondst("V3", "0.00", "60.00", true), V3_NOT_ZWAAR + "," + TmsDocuments.inconsistent("zwaar", "V1", "EF-031"))));
    }

    /** The scenario report passes the case that pins the deposit, and the red route runs it to a model; until the card
     *  zero-division-channel both carried the sentence of the fault. */
    @Test void the_scenario_report_passes_and_the_red_route_runs_to_a_model() throws Exception {
        Path ir = document(zwaar(DIVISION_GREATER_THAN_ONE));
        Path c = Files.writeString(Files.createTempDirectory("nuldeler-voorwaarde").resolve("case1-nul.json"),
                zeroAndOrdinary(V3_NOT_ZWAAR));
        List<ScenarioResult> report = CaseRunner.report(ir, List.of(c));
        assertEquals(ScenarioResult.Status.PASS, report.get(0).status(), report::toString);
        CaseRunner.RedResult r = CaseRunner.runRed(ir, c);
        assertEquals("NOFAULT", r.status(), r::detail);
    }

    @Test void a_consumer_that_itself_fires_reads_the_rule_and_the_condition() throws Exception {
        // the distribution's service fires the KieBase itself, on the exec-model branch through the entry: the
        // fire reaches its fixpoint with the deposit of the condition standing as an inconsistency, which
        // DroolsBackend.fixpointOf gives as data, no fault, and DroolsBackend.setDerivedValues writes back. Until the card
        // zero-division-channel the condition's fault stood instead, and the write-back threw it.
        // The destructive exec-model branch threw Drools' ConstraintEvaluationException out of the fire instead, which
        // DroolsBackend.designate named, and so did the destructive DRL-text branch until card
        // drl-teksttak-op-truth-maintenance ("[Rule "zwaar" …"); since that card both branches render the analysis.
        for (DroolsBackend.Branch branch : DroolsBackend.Branch.values())
            CaseRunner.forMeasurement(document(zwaar(DIVISION_GREATER_THAN_ONE)), branch)
                    .withSession(JSON.readTree(ZERO_AND_ORDINARY), (build, session, labels) -> {
                        assertEquals(null, DroolsBackend.fire(build, session).noFixpoint(),
                                branch + ": the fire reaches its fixpoint");
                        dev.uchar.regelspraak.transpiler.tms.Fixpoint fixpoint = DroolsBackend.fixpointOf(build, session);
                        assertEquals(List.of(), fixpoint.faults(), branch + ": no fault");
                        assertEquals(1, fixpoint.inconsistencies().size(), () -> branch + ": the deposit as data, "
                                + fixpoint.inconsistencies());
                        DroolsBackend.setDerivedValues(build, session);
                        return null;
                    });
    }

    // ---------------- documents ----------------

    private static final String TWO_DECIMALS = "{\"node\":\"Number\",\"whole\":false,\"decimals\":2,\"sign\":\"unspecified\"}";
    private static final String WHOLE = "{\"node\":\"Number\",\"whole\":true,\"decimals\":0,\"sign\":\"unspecified\"}";

    private static String attribute(String name, String type) {
        return "{\"node\":\"Attribute\",\"name\":\"" + name + "\",\"datatype\":" + type + "}";
    }

    private static String characteristic(String name) {
        return "{\"node\":\"Characteristic\",\"name\":\"" + name + "\",\"kind\":\"adjectival\"}";
    }

    private static final String ROLE_VONDST = "{\"node\":\"RoleBinding\",\"factType\":\"JutterHeeftVondst\",\"role\":\"vondst\"}";

    private static final String MODEL = "{\"objectTypes\":[{\"node\":\"ObjectType\",\"name\":\"Jutter\",\"animate\":true,"
            + "\"plural\":\"Jutters\",\"attributes\":[" + attribute("verhouding", TWO_DECIMALS) + ","
            + attribute("aantal zware vondsten", WHOLE) + "],\"characteristics\":[" + characteristic("ijverig") + "]},"
            + "{\"node\":\"ObjectType\",\"name\":\"Vondst\",\"animate\":false,\"plural\":\"Vondsten\",\"attributes\":["
            + attribute("geschatte gewicht", TWO_DECIMALS) + "," + attribute("gewogen gewicht", TWO_DECIMALS) + ","
            + attribute("verhouding", TWO_DECIMALS) + "],\"characteristics\":[" + characteristic("zwaar") + "]}],"
            + "\"factTypes\":[{\"node\":\"FactType\",\"name\":\"JutterHeeftVondst\",\"reciprocal\":false,\"roles\":["
            + "{\"node\":\"Role\",\"name\":\"jutter\",\"objectType\":\"Jutter\",\"many\":false},{\"node\":\"Role\",\"name\":"
            + "\"vondst\",\"objectType\":\"Vondst\",\"plural\":\"vondsten\",\"many\":true}]}],\"parameters\":[{\"node\":"
            + "\"Parameter\",\"name\":\"drempel\",\"datatype\":" + TWO_DECIMALS + "}],\"domains\":[],\"unitSystems\":[],"
            + "\"dimensions\":[],\"timelines\":[],\"dayKinds\":[]}";

    private static String own(String attribute) {
        return "{\"node\":\"AttributeReference\",\"attribute\":\"" + attribute + "\",\"subjectType\":\"Vondst\","
                + "\"source\":{\"snippet\":\"zijn " + attribute + "\"}}";
    }

    /** "de &lt;attribuut&gt; van de jutter van de Vondst", over the role jutter. */
    private static String viaJutter(String attribute) {
        return "{\"node\":\"AttributeReference\",\"attribute\":\"" + attribute + "\",\"subjectType\":\"Jutter\",\"source\":"
                + "{\"snippet\":\"de " + attribute + " van de jutter van de Vondst\"},\"path\":[{\"node\":\"RoleBinding\","
                + "\"factType\":\"JutterHeeftVondst\",\"role\":\"jutter\"}]}";
    }

    private static String number(String value) {
        return "{\"node\":\"Literal\",\"value\":{\"number\":\"" + value + "\"},\"type\":" + WHOLE + ",\"source\":"
                + "{\"snippet\":\"" + value + "\"}}";
    }

    private static String division(String left, String right) {
        return "{\"node\":\"Binary\",\"op\":\"dividedBy\",\"left\":" + left + ",\"right\":" + right + ",\"type\":"
                + TWO_DECIMALS + ",\"source\":{\"snippet\":\"een deling\"}}";
    }

    private static String greaterThan(String left, String right, String source) {
        return "{\"node\":\"Comparison\",\"op\":\"greater\",\"left\":" + left + ",\"right\":" + right + ",\"source\":"
                + "{\"snippet\":\"" + source + "\"}}";
    }

    private static final String WAAR = "{\"node\":\"Literal\",\"value\":{\"boolean\":true},\"type\":{\"node\":\"Boolean\"},"
            + "\"source\":{\"snippet\":\"waar\"}}";

    /** "Een Vondst is zwaar indien &lt;voorwaarde&gt;". */
    private static String zwaar(String condition) {
        return rule("zwaar", "Vondst", "zwaar", "Een Vondst is zwaar", WAAR, condition, "");
    }

    private static String rule(String name, String type, String target, String targetSource, String value, String condition,
                                String tail) {
        return "{\"node\":\"Rule\",\"name\":\"" + name + "\",\"versions\":[{\"validity\":{\"node\":\"Validity\"},\"target\":"
                + "{\"node\":\"AttributeReference\",\"attribute\":\"" + target + "\",\"subjectType\":\"" + type + "\",\"source\":"
                + "{\"snippet\":\"" + targetSource + "\"}},\"value\":" + value
                + (condition == null ? "" : ",\"condition\":" + condition) + ",\"initialisation\":false" + tail + "}]}";
    }

    private static Path document(String rules) throws Exception {
        String ir = "{\"format\":\"uchar-ir-v1\",\"model\":" + MODEL + ",\"rules\":{\"rules\":[" + rules + "],"
                + "\"dayKindDefinitions\":[],\"objectCreations\":[],\"allocations\":[],\"consistencies\":[],\"uniquenesses\":[],"
                + "\"factCreations\":[],\"startPointDeterminations\":[],\"ruleGroups\":[],\"prioritizedDefaults\":[]},"
                + "\"diagnostics\":[]}";
        Path tmp = Files.createTempFile("nuldeler-voorwaarde", ".json");
        tmp.toFile().deleteOnExit();
        return Files.writeString(tmp, ir);
    }

    // ---------------- cases ----------------

    private static String jutter(String ratio) {
        return "{\"label\":\"J1\",\"objectType\":\"Jutter\",\"attributes\":{\"verhouding\":{\"number\":\"" + ratio
                + "\"}},\"roles\":{},\"characteristics\":[]}";
    }

    private static String vondst(String label, String geschat, String weighted, boolean ofJ1) {
        return "{\"label\":\"" + label + "\",\"objectType\":\"Vondst\",\"attributes\":{\"geschatte gewicht\":{\"number\":\""
                + geschat + "\"},\"gewogen gewicht\":{\"number\":\"" + weighted + "\"}},\"roles\":{" + (ofJ1 ? "\"jutter\":"
                + "[\"J1\"]" : "") + "},\"characteristics\":[]}";
    }

    private static String caseDocument(String instances, String expected) {
        return "{\"format\":\"uchar-case-v0\",\"asOfDate\":null,\"input\":{\"instances\":[" + instances + "],\"parameters\":"
                + "{\"drempel\":{\"number\":\"1.00\"}}},\"expected\":[" + expected + "]}";
    }

    /** J1 with a verhouding of 2,00; V1 (400 by 320) and V3 (60 by 0,00), both of J1; {@code expected} the
     *  expectations. */
    private static String zeroAndOrdinary(String expected) {
        return caseDocument(jutter("2.00") + "," + vondst("V1", "320.00", "400.00", true) + ","
                + vondst("V3", "0.00", "60.00", true), expected);
    }

    /** The same case without expectations, for a consumer that reads the fixpoint itself. */
    private static final String ZERO_AND_ORDINARY = zeroAndOrdinary("");

    /** One branch's verdict on one case: null when every expectation holds. */
    private static String play(Path ir, boolean viaDrl, String caseDocument) throws Exception {
        return CaseRunner.playCase(ir, JSON.readTree(caseDocument), viaDrl);
    }
}
