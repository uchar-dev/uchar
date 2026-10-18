package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * A §6.5 division or a §6.6 root in a rule's value whose operand folds over a role: "(100 gedeeld door (de som van het
 * gewicht van alle vondsten van de Jutter)) rekenkundig afgerond op 2 decimalen", a minimum as the divisor, a sum as
 * the dividend, a fold inside a compound divisor, and the root of a sum minus 10. RUNTIME-SEMANTICS ch. 1.2: a filled
 * dividend over an empty divisor, and a negative radicand, degrade to leeg and deposit on the §9.5 channel (ch. 5.6);
 * an empty sum or minimum is leeg (ch. 1.2e), so over no vondsten the division deposits. Both Drools branches surface
 * the deposit through a companion of the rule; until this witness a companion read only an own or to-one navigated
 * attribute, so a fold operand got none. Every document here is authored in this class on Jutterswet material, each
 * one measured form with the values {@code ref} and clingo answer (clingo does not translate a rounded root; there
 * {@code ref} alone).
 *
 * <p>Situation (each case, unless a row says otherwise): J1 with V1 (vinddag 12-03-2026, gewicht 4), V2 (05-03-2026,
 * 7) and V3 (no values); J2 without vondsten; J3 with only V4 (no values); J4 with V5 (20-03-2026, 9). Every Jutter has
 * basis 2, and deler 2 except J1.
 *
 * <p><b>Red before the fix</b> (measured 2026-10-01 on the transpiler of main, both branches): every row whose case is
 * inconsistent answered {@code verwacht inconsistent, werkelijk consistent}, in the DRL-text branch also the row with
 * the fold under a Let, which the exec branch already refused. Two rows are flanks, green before and after: a filled
 * sum deposits nothing, and a count of 0 as the divisor is a fault in every executor, not a deposit.
 */
class FoldCompanionWitnessTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    @Test void a_division_by_an_empty_sum_reports_the_error() throws Exception {
        // J2 and J3 fold no gewicht: the sum is leeg, 100 over leeg is leeg and deposits. ref and clingo: 9,09, leeg,
        // leeg and 11,11, inconsistent.
        String ir = document(regel(rounded(binary("dividedBy", number(100), SUM, UNBOUNDED)), null));
        String caseDocument = caseDocument(situation(), heffing("9.09", null, null, "11.11"), INCONSISTENT);
        assertNull(play(ir, caseDocument), "exectak");
        assertNull(playDrl(ir, caseDocument), "DRL-teksttak");
    }

    @Test void a_division_by_an_empty_minimum_reports_the_error() throws Exception {
        // The minimum of the gewicht as the divisor. ref and clingo: 25,00, leeg, leeg and 11,11, inconsistent.
        String ir = document(regel(rounded(binary("dividedBy", number(100), MINIMUM, UNBOUNDED)), null));
        String caseDocument = caseDocument(situation(), heffing("25.00", null, null, "11.11"), INCONSISTENT);
        assertNull(play(ir, caseDocument), "exectak");
        assertNull(playDrl(ir, caseDocument), "DRL-teksttak");
    }

    @Test void a_sum_divided_by_an_empty_divisor_reports_the_error() throws Exception {
        // "((de som …) gedeeld door zijn deler)": the fold is the dividend. J1 (sum 11) has no deler and deposits; J2
        // and J3 divide a leeg sum, which is 0 and deposits nothing. ref and clingo: leeg, 0,00, 0,00 and 4,50,
        // inconsistent.
        String ir = document(regel(rounded(binary("dividedBy", SUM, own("deler"), UNBOUNDED)), null));
        String caseDocument = caseDocument(situation(), heffing(null, "0.00", "0.00", "4.50"), INCONSISTENT);
        assertNull(play(ir, caseDocument), "exectak");
        assertNull(playDrl(ir, caseDocument), "DRL-teksttak");
    }

    @Test void a_compound_divisor_with_a_fold() throws Exception {
        // "(100 gedeeld door ((de som …) verminderd met 1))": leeg verminderd met 1 stays leeg (ch. 1.2), so the divisor
        // is leeg where the sum is. ref and clingo: 10,00, leeg, leeg and 12,50, inconsistent.
        String divisor = binary("reducedBy", SUM, number(1), WHOLE);
        String ir = document(regel(rounded(binary("dividedBy", number(100), divisor, UNBOUNDED)), null));
        String caseDocument = caseDocument(situation(), heffing("10.00", null, null, "12.50"), INCONSISTENT);
        assertNull(play(ir, caseDocument), "exectak");
        assertNull(playDrl(ir, caseDocument), "DRL-teksttak");
    }

    @Test void the_square_root_of_a_negative_sum_reports_the_error() throws Exception {
        // "de wortel van ((de som …) min 10) rekenkundig afgerond op 2 decimalen": J1 1, the others negative (leeg
        // min 10 is -10, ch. 1.2). ref: 1,00, leeg, leeg and leeg, inconsistent; clingo does not translate the rounded
        // root.
        String squareRoot = "{\"node\":\"BuiltinCall\",\"kind\":\"squareRoot\",\"arguments\":[" + binary("minus", SUM, number(10), WHOLE)
                + "],\"type\":{\"node\":\"Number\",\"whole\":false,\"decimals\":0,\"sign\":\"nonNegative\"},\"source\":{\"snippet\":"
                + "\"de wortel van ((de som …) min 10)\"}}";
        String ir = document(regel(rounded(squareRoot), null));
        String caseDocument = caseDocument(situation(), heffing("1.00", null, null, null), INCONSISTENT);
        assertNull(play(ir, caseDocument), "exectak");
        assertNull(playDrl(ir, caseDocument), "DRL-teksttak");
    }

    @Test void under_the_condition_of_the_rule() throws Exception {
        // The division of the first row "indien zijn drempel gevuld is", drempel 1 for J1, J2 and J4: J2 deposits, J3
        // (no drempel) computes nothing. Without J2 the case is consistent: the companion carries the rule's condition.
        // ref and clingo: 9,09, leeg, leeg and 11,11, inconsistent; and 9,09, leeg and 11,11 for J1, J3 and J4,
        // consistent.
        String voorwaarde = "{\"node\":\"Predicate\",\"kind\":\"filled\",\"operand\":" + own("drempel")
                + ",\"negated\":false,\"digits\":0,\"source\":{\"snippet\":\"zijn drempel gevuld is\"}}";
        String ir = document(regel(rounded(binary("dividedBy", number(100), SUM, UNBOUNDED)), voorwaarde));
        List<String> with = new ArrayList<>();
        for (String i : situation()) with.add(withThreshold(i, "J1", "J2", "J4"));
        String caseDocument = caseDocument(with, heffing("9.09", null, null, "11.11"), INCONSISTENT);
        assertNull(play(ir, caseDocument), "exectak, J2 met drempel");
        assertNull(playDrl(ir, caseDocument), "DRL-teksttak, J2 met drempel");
        List<String> withoutJ2 = new ArrayList<>();
        for (String i : situation()) if (!i.contains("\"J2\"")) withoutJ2.add(withThreshold(i, "J1", "J4"));
        String consistent = caseDocument(withoutJ2, verwacht("J1", "9.09") + "," + verwacht("J3", null) + "," + verwacht("J4", "11.11"),
                CONSISTENT);
        assertNull(play(ir, consistent), "exectak, J3 zonder drempel");
        assertNull(playDrl(ir, consistent), "DRL-teksttak, J3 zonder drempel");
    }

    @Test void a_fold_under_a_let_beside_a_division_plays_in_both_branches() throws Exception {
        // "(de vangst gedeeld door zijn deler) rekenkundig afgerond op 2 decimalen. Daarbij geldt: de vangst is de som
        // …": J1's vangst is 11 over no deler and deposits; a leeg vangst ends the Let before the division (ch. 10.6), so
        // J2 and J3 deposit nothing. ref and clingo: leeg, leeg, leeg and 4,50, inconsistent. A companion re-reads the
        // site's need and the need is a fold, which the destructive DRL-text lowering could not read there, as the
        // destructive exec-model branch could not; it refused the rule by name ("een vouw over een setbron en een deling
        // of wortel onder één Let of IfThenElse") until card drl-teksttak-op-truth-maintenance. The analysis with truth
        // maintenance has no companions: it deposits where the division reads leeg, and answers as ref
        // (TmsLikeRefWitnessTest, vouw-onder-let-naast-deling), on both branches since that card.
        String let = "{\"node\":\"Let\",\"type\":" + TWO_DECIMALS + ",\"binder\":\"vangst\",\"value\":" + SUM + ",\"body\":"
                + rounded(binary("dividedBy", "{\"node\":\"BinderRef\",\"name\":\"vangst\",\"source\":{\"snippet\":\"de vangst\"}}",
                        own("deler"), UNBOUNDED)) + ",\"source\":{\"snippet\":\"de vangst is de som …\"}}";
        String ir = document(regel(let, null));
        String caseDocument = caseDocument(situation(), heffing(null, null, null, "4.50"), INCONSISTENT);
        Path tmp = Files.createTempFile("vouw-begeleider", ".json");
        tmp.toFile().deleteOnExit();
        Files.writeString(tmp, ir);
        BothBranches.green(tmp, JSON.readTree(caseDocument), "a fold under a Let beside a division");
    }

    // ---------------- the flanks ----------------

    @Test void a_filled_sum_reports_nothing() throws Exception {
        // The first row over J1 and J4 alone: every sum is filled. ref and clingo: 9,09 and 11,11, consistent.
        String ir = document(regel(rounded(binary("dividedBy", number(100), SUM, UNBOUNDED)), null));
        List<String> only = new ArrayList<>();
        for (String i : situation()) if (!i.contains("\"J2\"") && !i.contains("\"J3\"")) only.add(i);
        String caseDocument = caseDocument(only, verwacht("J1", "9.09") + "," + verwacht("J4", "11.11"), CONSISTENT);
        assertNull(play(ir, caseDocument), "exectak");
        assertNull(playDrl(ir, caseDocument), "DRL-teksttak");
    }

    @Test void a_divisor_that_is_zero_fails() throws Exception {
        // "(100 gedeeld door het aantal vondsten van de Jutter)": J2 counts 0, a division by numeric zero, which is no
        // §9.5 deposit but a fault: ref "division by numeric zero", clingo the same guard, and both Drools branches stop
        // at fire.
        String ir = document(regel(rounded(binary("dividedBy", number(100), COUNTING, UNBOUNDED)), null));
        String caseDocument = caseDocument(situation(), heffing("33.33", null, "100.00", "100.00"), CONSISTENT);
        assertThrows(RuntimeException.class, () -> play(ir, caseDocument), "exectak");
        assertThrows(RuntimeException.class, () -> playDrl(ir, caseDocument), "DRL-teksttak");
    }

    // ---------------- documents ----------------

    private static final String WHOLE = "{\"node\":\"Number\",\"whole\":true,\"decimals\":0,\"sign\":\"unspecified\"}";
    /** A number that is not whole with 0 decimals: the unbounded form of IR-SPEC ch. 2.4 (c), as the frontend types a
     *  division. */
    private static final String UNBOUNDED = "{\"node\":\"Number\",\"whole\":false,\"decimals\":0,\"sign\":\"unspecified\"}";
    private static final String TWO_DECIMALS = "{\"node\":\"Number\",\"whole\":false,\"decimals\":2,\"sign\":\"unspecified\"}";
    private static final String DATE = "{\"node\":\"Date\",\"precision\":\"day\"}";

    private static String attribuut(String naam, String datatype) {
        return "{\"node\":\"Attribute\",\"name\":\"" + naam + "\",\"datatype\":" + datatype + "}";
    }

    /** Jutter and Vondst; one jutter has many vondsten. */
    private static final String MODEL = "{\"objectTypes\":["
            + "{\"node\":\"ObjectType\",\"name\":\"Jutter\",\"animate\":true,\"plural\":\"Jutters\",\"attributes\":["
            + attribuut("heffing", TWO_DECIMALS) + "," + attribuut("basis", WHOLE) + "," + attribuut("deler", WHOLE) + ","
            + attribuut("drempel", WHOLE) + "],\"characteristics\":[]},"
            + "{\"node\":\"ObjectType\",\"name\":\"Vondst\",\"animate\":false,\"plural\":\"Vondsten\",\"attributes\":["
            + attribuut("vinddag", DATE) + "," + attribuut("gewicht", WHOLE) + "],\"characteristics\":[]}"
            + "],\"factTypes\":[{\"node\":\"FactType\",\"name\":\"JutterHeeftVondst\",\"reciprocal\":false,\"roles\":["
            + "{\"node\":\"Role\",\"name\":\"jutter\",\"objectType\":\"Jutter\",\"many\":false},"
            + "{\"node\":\"Role\",\"name\":\"vondst\",\"objectType\":\"Vondst\",\"plural\":\"vondsten\",\"many\":true}]}],"
            + "\"parameters\":[],\"domains\":[],\"unitSystems\":[],\"dimensions\":[],\"timelines\":[],\"dayKinds\":[]}";

    private static String roleBinding() {
        return "{\"node\":\"RoleBinding\",\"factType\":\"JutterHeeftVondst\",\"role\":\"vondst\"}";
    }

    private static String own(String attribuut) {
        return "{\"node\":\"AttributeReference\",\"attribute\":\"" + attribuut + "\",\"subjectType\":\"Jutter\",\"source\":"
                + "{\"snippet\":\"zijn " + attribuut + "\"}}";
    }

    private static String fold(String soort, String snippet) {
        return "{\"node\":\"Aggregation\",\"kind\":\"" + soort + "\",\"arguments\":[{\"node\":\"AttributeReference\",\"attribute\":"
                + "\"gewicht\",\"subjectType\":\"Vondst\",\"source\":{\"snippet\":\"het gewicht van alle vondsten van de Jutter\"}}],"
                + "\"elementType\":\"Vondst\",\"sourceRole\":" + roleBinding() + ",\"type\":" + WHOLE + ",\"source\":{\"snippet\":\""
                + snippet + "\"}}";
    }

    private static final String SUM = fold("sum", "de som van het gewicht van alle vondsten van de Jutter");
    private static final String MINIMUM = fold("minimum", "de minimale waarde van het gewicht van alle vondsten van de Jutter");
    /** "het aantal vondsten van de Jutter", as the frontend writes it. */
    private static final String COUNTING = "{\"node\":\"Aggregation\",\"kind\":\"count\",\"arguments\":[{\"node\":"
            + "\"AttributeReference\",\"attribute\":\"vondsten\",\"subjectType\":\"Jutter\",\"source\":{\"snippet\":\"vondsten van"
            + " de Jutter\"}}],\"elementType\":\"Vondst\",\"sourceRole\":" + roleBinding()
            + ",\"type\":{\"node\":\"Number\",\"whole\":true,\"decimals\":0,\"sign\":\"nonNegative\"},\"source\":{\"snippet\":"
            + "\"het aantal vondsten van de Jutter\"}}";

    private static String number(int n) {
        return "{\"node\":\"Literal\",\"value\":{\"number\":\"" + n + "\"},\"type\":" + WHOLE + ",\"source\":{\"snippet\":\"" + n + "\"}}";
    }

    private static String binary(String op, String left, String right, String type) {
        return "{\"node\":\"Binary\",\"op\":\"" + op + "\",\"left\":" + left + ",\"right\":" + right + ",\"type\":" + type
                + ",\"source\":{\"snippet\":\"" + op + "\"}}";
    }

    /** &lt;basis&gt; rekenkundig afgerond op 2 decimalen. */
    private static String rounded(String base) {
        return "{\"node\":\"Rounding\",\"base\":" + base + ",\"mode\":\"halfAwayFromZero\",\"decimals\":2,\"type\":"
                + TWO_DECIMALS + ",\"source\":{\"snippet\":\"… rekenkundig afgerond op 2 decimalen\"}}";
    }

    /** "De heffing van een Jutter moet berekend worden als &lt;waarde&gt; [indien &lt;voorwaarde&gt;]." */
    private static String regel(String waarde, String voorwaarde) {
        return "{\"node\":\"Rule\",\"name\":\"heffing\",\"versions\":[{\"validity\":{\"node\":\"Validity\"},\"target\":"
                + "{\"node\":\"AttributeReference\",\"attribute\":\"heffing\",\"subjectType\":\"Jutter\",\"source\":{\"snippet\":"
                + "\"De heffing van een Jutter\"}},\"value\":" + waarde + (voorwaarde == null ? "" : ",\"condition\":" + voorwaarde)
                + ",\"initialisation\":false,\"presentation\":\"calculation\"}]}";
    }

    private static String document(String regel) {
        return "{\"format\":\"uchar-ir-v1\",\"model\":" + MODEL + ",\"rules\":{\"rules\":[" + regel + "],"
                + "\"dayKindDefinitions\":[],\"objectCreations\":[],\"allocations\":[],\"consistencies\":[],\"uniquenesses\":[],"
                + "\"factCreations\":[],\"startPointDeterminations\":[],\"ruleGroups\":[]},\"diagnostics\":[]}";
    }

    // ---------------- cases ----------------

    private static String instance(String label, String type, String attributes, String roles) {
        return "{\"label\":\"" + label + "\",\"objectType\":\"" + type + "\",\"attributes\":{" + attributes + "},\"roles\":{"
                + roles + "},\"characteristics\":[]}";
    }

    private static String jutter(String label, boolean divisor) {
        return instance(label, "Jutter", "\"basis\":{\"number\":\"2\"}" + (divisor ? ",\"deler\":{\"number\":\"2\"}" : ""), "");
    }

    private static String vondst(String label, String vinddag, String weight, String jutter) {
        List<String> a = new ArrayList<>();
        if (vinddag != null) a.add("\"vinddag\":{\"date\":\"" + vinddag + "\"}");
        if (weight != null) a.add("\"gewicht\":{\"number\":\"" + weight + "\"}");
        return instance(label, "Vondst", String.join(",", a), "\"jutter\":[\"" + jutter + "\"]");
    }

    /** The situation of the class comment. */
    private static List<String> situation() {
        return List.of(jutter("J1", false), jutter("J2", true), jutter("J3", true), jutter("J4", true),
                vondst("V1", "2026-03-12", "4", "J1"), vondst("V2", "2026-03-05", "7", "J1"), vondst("V3", null, null, "J1"),
                vondst("V4", null, null, "J3"), vondst("V5", "2026-03-20", "9", "J4"));
    }

    /** The Jutter instance {@code i} with drempel 1 where its label is one of {@code labels}. */
    private static String withThreshold(String i, String... labels) {
        for (String l : labels)
            if (i.startsWith("{\"label\":\"" + l + "\",\"objectType\":\"Jutter\""))
                return i.replace("\"attributes\":{", "\"attributes\":{\"drempel\":{\"number\":\"1\"},");
        return i;
    }

    private static final String INCONSISTENT = "{\"kind\":\"inconsistent\"}";
    private static final String CONSISTENT = "{\"kind\":\"consistent\"}";

    private static String verwacht(String label, String heffing) {
        return heffing == null
                ? "{\"kind\":\"attributeEmpty\",\"label\":\"" + label + "\",\"attribute\":\"heffing\"}"
                : "{\"kind\":\"attributeValue\",\"label\":\"" + label + "\",\"attribute\":\"heffing\",\"value\":{\"number\":\""
                        + heffing + "\"}}";
    }

    /** The heffing of J1 to J4, a null value leeg. */
    private static String heffing(String... waarden) {
        List<String> out = new ArrayList<>();
        for (int i = 0; i < waarden.length; i++) out.add(verwacht("J" + (i + 1), waarden[i]));
        return String.join(",", out);
    }

    private static String caseDocument(List<String> instanties, String... verwacht) {
        return "{\"format\":\"uchar-case-v0\",\"asOfDate\":null,\"input\":{\"instances\":[" + String.join(",", instanties)
                + "],\"parameters\":{}},\"expected\":[" + String.join(",", verwacht) + "]}";
    }

    /** The exec-model branch's verdict on one case: null when every expectation holds. */
    private static String play(String ir, String caseDocument) throws Exception {
        return play(ir, caseDocument, false);
    }

    private static String playDrl(String ir, String caseDocument) throws Exception {
        return play(ir, caseDocument, true);
    }

    private static String play(String ir, String caseDocument, boolean drl) throws Exception {
        Path tmp = Files.createTempFile("vouw-begeleider", ".json");
        tmp.toFile().deleteOnExit();
        Files.writeString(tmp, ir);
        JsonNode c = JSON.readTree(caseDocument);
        return CaseRunner.playCase(tmp, c, drl);
    }
}
