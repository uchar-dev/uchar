package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.ObjectMapper;

import dev.uchar.regelspraak.engine.ScenarioResultaat;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A division by a numeric zero in a CONDITION: "Een Vondst is zwaar indien zijn gewogen gewicht gedeeld door zijn
 * geschatte gewicht groter is dan 1", over V3, whose geschatte gewicht is 0,00 under a gewogen gewicht of 60,00. ref
 * faults with "division by numeric zero" and clingo with its fail-fast guard on V3 (RUNTIME-SEMANTICS ch. 1.4, point
 * 4), in every form below. On the destructive lowering a condition is tested in a constraint and not in a
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

    /** The fault is the runtime's own named type, with the sentence as its message (by name, so this witness compiled
     *  against the backend before the fix). */
    private static void named(String sentence, RuntimeException e) {
        assertEquals("DelingDoorNul", e.getClass().getSimpleName(), () -> e.getClass().getName() + ": " + e.getMessage());
        assertEquals(sentence, e.getMessage());
    }

    private static String sentence(String regel, String dividend) {
        return "deling door nul in de regel \"" + regel + "\": " + dividend + " gedeeld door 0 — een numerieke nul als deler "
                + "is een fout, geen leeg (RUNTIME-SEMANTICS ch. 1.4, punt 4)";
    }

    private static final String DIVISION_GREATER_THAN_ONE = greaterThan(division(own("gewogen gewicht"), own("geschatte gewicht")),
            number("1"), "zijn gewogen gewicht gedeeld door zijn geschatte gewicht groter is dan 1");

    @Test void a_comparison_with_a_number() throws Exception {
        Path ir = document(zwaar(DIVISION_GREATER_THAN_ONE));
        named(sentence("zwaar", "60"), assertThrows(RuntimeException.class, () -> play(ir, false, ZERO_AND_ORDINARY)));
    }

    @Test void the_condition_of_an_ordinary_rule() throws Exception {
        // "De verhouding van een Vondst moet gesteld worden op 1,00 indien …"
        Path ir = document(regel("verhouding", "Vondst", "verhouding", "De verhouding van een Vondst",
                "{\"node\":\"Literal\",\"value\":{\"number\":\"1.00\"},\"type\":" + TWO_DECIMALS + ",\"source\":{\"snippet\":"
                        + "\"1,00\"}}", DIVISION_GREATER_THAN_ONE, ",\"presentation\":\"equation\""));
        named(sentence("verhouding", "60"), assertThrows(RuntimeException.class, () -> play(ir, false, ZERO_AND_ORDINARY)));
    }

    @Test void a_comparison_with_a_parameter() throws Exception {
        // "… groter is dan de drempel", the drempel 1,00: the parameter is a pattern variable beside the subject
        Path ir = document(zwaar(greaterThan(division(own("gewogen gewicht"), own("geschatte gewicht")),
                "{\"node\":\"ParameterReference\",\"parameter\":\"drempel\",\"source\":{\"snippet\":\"de drempel\"}}",
                "zijn gewogen gewicht gedeeld door zijn geschatte gewicht groter is dan de drempel")));
        named(sentence("zwaar", "60"), assertThrows(RuntimeException.class, () -> play(ir, false, ZERO_AND_ORDINARY)));
    }

    @Test void under_a_counting_quantifier() throws Exception {
        // "… indien hij aan ten minste één van de volgende voorwaarden voldoet: - zijn gewogen gewicht is leeg - …"
        String empty = "{\"node\":\"Predicate\",\"kind\":\"empty\",\"operand\":" + own("gewogen gewicht") + ",\"negated\":"
                + "false,\"digits\":0,\"source\":{\"snippet\":\"zijn gewogen gewicht is leeg\"}}";
        Path ir = document(zwaar("{\"node\":\"Compound\",\"quantifier\":\"atLeast\",\"threshold\":1,\"parts\":[" + empty + ","
                + DIVISION_GREATER_THAN_ONE + "],\"source\":{\"snippet\":\"hij aan ten minste één van de volgende voorwaarden "
                + "voldoet\"}}"));
        named(sentence("zwaar", "60"), assertThrows(RuntimeException.class, () -> play(ir, false, ZERO_AND_ORDINARY)));
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
        Path ir = document(regel("zware vondsten", "Jutter", "aantal zware vondsten", "Het aantal zware vondsten van een "
                + "Jutter", count, null, ",\"presentation\":\"calculation\""));
        named(sentence("zware vondsten", "60"), assertThrows(RuntimeException.class, () -> play(ir, false, ZERO_AND_ORDINARY)));
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
        Path ir = document(regel("ijverig", "Jutter", "ijverig", "Een Jutter is ijverig", WAAR, quantifier, ""));
        named(sentence("ijverig", "100"), assertThrows(RuntimeException.class,
                () -> play(ir, false, caseDocument(jutter("0.00") + "," + vondst("V1", "320.00", "400.00", true)))));
    }

    @Test void over_a_role() throws Exception {
        // "… indien zijn gewogen gewicht gedeeld door de verhouding van de jutter van de Vondst groter is dan 1", V3 of
        // J1 with a verhouding of 0,00 (V1 has no jutter: its divisor is leeg, ch. 1.2)
        Path ir = document(zwaar(greaterThan(division(own("gewogen gewicht"), viaJutter("verhouding")), number("1"),
                "zijn gewogen gewicht gedeeld door de verhouding van de jutter van de Vondst groter is dan 1")));
        named(sentence("zwaar", "60"), assertThrows(RuntimeException.class, () -> play(ir, false,
                caseDocument(jutter("0.00") + "," + vondst("V1", "320.00", "400.00", false) + "," + vondst("V3", "0.00", "60.00", true)))));
    }

    @Test void the_scenario_report_and_the_red_route_carry_the_sentence() throws Exception {
        Path ir = document(zwaar(DIVISION_GREATER_THAN_ONE));
        Path c = Files.writeString(Files.createTempDirectory("nuldeler-voorwaarde").resolve("case1-nul.json"), ZERO_AND_ORDINARY);
        List<ScenarioResultaat> rapport = CaseRunner.rapport(ir, List.of(c));
        assertEquals(ScenarioResultaat.Status.FAIL, rapport.get(0).status(), rapport::toString);
        assertEquals("case1-nul.json: ERROR DelingDoorNul: " + sentence("zwaar", "60"), rapport.get(0).diagnose());
        // gate 8 pins "Breuk deling door 0" on its red cases: the chain still carries it
        CaseRunner.RedResult r = CaseRunner.runRed(ir, c);
        assertEquals("FAULT", r.status(), r::detail);
        assertTrue(r.detail().startsWith("DelingDoorNul: " + sentence("zwaar", "60")), r.detail());
        assertTrue(r.detail().contains("Breuk deling door 0"), r.detail());
    }

    @Test void a_consumer_that_itself_fires_reads_the_rule_and_the_condition() throws Exception {
        // the distribution's service fires the KieBase itself, on the exec-model branch through the entry: the
        // fire reaches its fixpoint with the fault of the condition standing as a fact, DroolsBackend.lees gives it as
        // data, and DroolsBackend.zetAfgeleideWaarden throws it in the sentence of the replay, which names the rule.
        // The destructive exec-model branch threw Drools' ConstraintEvaluationException out of the fire instead, which
        // DroolsBackend.designate named, and so did the destructive DRL-text branch until card
        // drl-teksttak-op-truth-maintenance ("[Rule "zwaar" …"); since that card both branches render the analysis.
        for (DroolsBackend.Branch branch : DroolsBackend.Branch.values())
            CaseRunner.forMeasurement(document(zwaar(DIVISION_GREATER_THAN_ONE)), branch)
                    .withSession(JSON.readTree(ZERO_AND_ORDINARY), (bouw, sessie, labels) -> {
                        assertEquals(null, DroolsBackend.vuur(bouw, sessie).geenFixpunt(),
                                branch + ": the fire reaches its fixpoint");
                        List<dev.uchar.regelspraak.transpiler.tms.Fout> fouten = DroolsBackend.lees(bouw, sessie).fouten();
                        assertEquals(1, fouten.size(), fouten::toString);
                        assertEquals(sentence("zwaar", "60"), fouten.get(0).melding(), branch + ": the fault as data");
                        named(sentence("zwaar", "60"), assertThrows(RuntimeException.class,
                                () -> DroolsBackend.zetAfgeleideWaarden(bouw, sessie)));
                        return null;
                    });
    }

    // ---------------- documents ----------------

    private static final String TWO_DECIMALS = "{\"node\":\"Number\",\"whole\":false,\"decimals\":2,\"sign\":\"unspecified\"}";
    private static final String WHOLE = "{\"node\":\"Number\",\"whole\":true,\"decimals\":0,\"sign\":\"unspecified\"}";

    private static String attribuut(String naam, String type) {
        return "{\"node\":\"Attribute\",\"name\":\"" + naam + "\",\"datatype\":" + type + "}";
    }

    private static String kenmerk(String naam) {
        return "{\"node\":\"Characteristic\",\"name\":\"" + naam + "\",\"kind\":\"adjectival\"}";
    }

    private static final String ROLE_VONDST = "{\"node\":\"RoleBinding\",\"factType\":\"JutterHeeftVondst\",\"role\":\"vondst\"}";

    private static final String MODEL = "{\"objectTypes\":[{\"node\":\"ObjectType\",\"name\":\"Jutter\",\"animate\":true,"
            + "\"plural\":\"Jutters\",\"attributes\":[" + attribuut("verhouding", TWO_DECIMALS) + ","
            + attribuut("aantal zware vondsten", WHOLE) + "],\"characteristics\":[" + kenmerk("ijverig") + "]},"
            + "{\"node\":\"ObjectType\",\"name\":\"Vondst\",\"animate\":false,\"plural\":\"Vondsten\",\"attributes\":["
            + attribuut("geschatte gewicht", TWO_DECIMALS) + "," + attribuut("gewogen gewicht", TWO_DECIMALS) + ","
            + attribuut("verhouding", TWO_DECIMALS) + "],\"characteristics\":[" + kenmerk("zwaar") + "]}],"
            + "\"factTypes\":[{\"node\":\"FactType\",\"name\":\"JutterHeeftVondst\",\"reciprocal\":false,\"roles\":["
            + "{\"node\":\"Role\",\"name\":\"jutter\",\"objectType\":\"Jutter\",\"many\":false},{\"node\":\"Role\",\"name\":"
            + "\"vondst\",\"objectType\":\"Vondst\",\"plural\":\"vondsten\",\"many\":true}]}],\"parameters\":[{\"node\":"
            + "\"Parameter\",\"name\":\"drempel\",\"datatype\":" + TWO_DECIMALS + "}],\"domains\":[],\"unitSystems\":[],"
            + "\"dimensions\":[],\"timelines\":[],\"dayKinds\":[]}";

    private static String own(String attribuut) {
        return "{\"node\":\"AttributeReference\",\"attribute\":\"" + attribuut + "\",\"subjectType\":\"Vondst\","
                + "\"source\":{\"snippet\":\"zijn " + attribuut + "\"}}";
    }

    /** "de &lt;attribuut&gt; van de jutter van de Vondst", over the role jutter. */
    private static String viaJutter(String attribuut) {
        return "{\"node\":\"AttributeReference\",\"attribute\":\"" + attribuut + "\",\"subjectType\":\"Jutter\",\"source\":"
                + "{\"snippet\":\"de " + attribuut + " van de jutter van de Vondst\"},\"path\":[{\"node\":\"RoleBinding\","
                + "\"factType\":\"JutterHeeftVondst\",\"role\":\"jutter\"}]}";
    }

    private static String number(String waarde) {
        return "{\"node\":\"Literal\",\"value\":{\"number\":\"" + waarde + "\"},\"type\":" + WHOLE + ",\"source\":"
                + "{\"snippet\":\"" + waarde + "\"}}";
    }

    private static String division(String left, String right) {
        return "{\"node\":\"Binary\",\"op\":\"dividedBy\",\"left\":" + left + ",\"right\":" + right + ",\"type\":"
                + TWO_DECIMALS + ",\"source\":{\"snippet\":\"een deling\"}}";
    }

    private static String greaterThan(String left, String right, String bron) {
        return "{\"node\":\"Comparison\",\"op\":\"greater\",\"left\":" + left + ",\"right\":" + right + ",\"source\":"
                + "{\"snippet\":\"" + bron + "\"}}";
    }

    private static final String WAAR = "{\"node\":\"Literal\",\"value\":{\"boolean\":true},\"type\":{\"node\":\"Boolean\"},"
            + "\"source\":{\"snippet\":\"waar\"}}";

    /** "Een Vondst is zwaar indien &lt;voorwaarde&gt;". */
    private static String zwaar(String voorwaarde) {
        return regel("zwaar", "Vondst", "zwaar", "Een Vondst is zwaar", WAAR, voorwaarde, "");
    }

    private static String regel(String naam, String type, String doel, String targetSource, String waarde, String voorwaarde,
                                String tail) {
        return "{\"node\":\"Rule\",\"name\":\"" + naam + "\",\"versions\":[{\"validity\":{\"node\":\"Validity\"},\"target\":"
                + "{\"node\":\"AttributeReference\",\"attribute\":\"" + doel + "\",\"subjectType\":\"" + type + "\",\"source\":"
                + "{\"snippet\":\"" + targetSource + "\"}},\"value\":" + waarde
                + (voorwaarde == null ? "" : ",\"condition\":" + voorwaarde) + ",\"initialisation\":false" + tail + "}]}";
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

    private static String vondst(String label, String geschat, String gewogen, boolean ofJ1) {
        return "{\"label\":\"" + label + "\",\"objectType\":\"Vondst\",\"attributes\":{\"geschatte gewicht\":{\"number\":\""
                + geschat + "\"},\"gewogen gewicht\":{\"number\":\"" + gewogen + "\"}},\"roles\":{" + (ofJ1 ? "\"jutter\":"
                + "[\"J1\"]" : "") + "},\"characteristics\":[]}";
    }

    private static String caseDocument(String instanties) {
        return "{\"format\":\"uchar-case-v0\",\"asOfDate\":null,\"input\":{\"instances\":[" + instanties + "],\"parameters\":"
                + "{\"drempel\":{\"number\":\"1.00\"}}},\"expected\":[]}";
    }

    /** J1 with a verhouding of 2,00; V1 (400 by 320) and V3 (60 by 0,00), both of J1. */
    private static final String ZERO_AND_ORDINARY = caseDocument(jutter("2.00") + "," + vondst("V1", "320.00", "400.00", true) + ","
            + vondst("V3", "0.00", "60.00", true));

    /** One branch's verdict on one case: null when every expectation holds. */
    private static String play(Path ir, boolean viaDrl, String caseDocument) throws Exception {
        return CaseRunner.playCase(ir, JSON.readTree(caseDocument), viaDrl);
    }
}
