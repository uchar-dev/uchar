package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * A fold over the members of a set source inside a rule's value, in the exec-model branch: the earliest and the
 * latest date over a to-many role ("de eerste van de vinddag van alle vondsten van de Jutter"), with a condition on
 * the elements, two roles deep, and as an operand of a computation, beside the number folds as an operand.
 * RUNTIME-SEMANTICS ch. 1.2e: eerste and laatste are ordering folds over the contributed values, empty contributors
 * are skipped and an empty or all-empty source is leeg; ch. 1.2 gives the empty operand of each operator. Every
 * document here is authored in this class on Jutterswet material, and each is one measured form of the card's form
 * space, with the values {@code ref} and clingo answer (clingo does not fold a date-and-time over a set; there
 * {@code ref} alone).
 *
 * <p><b>Red before the fix</b> (measured 2026-09-30 on the jar of the claim, exec-model branch): every date fold
 * refused {@code regel (waarde buiten scope)}; a date plus a number fold, a clamp around a count and a count plus a
 * navigated read refused {@code regel (aggregatie-in-arithmetiek buiten scope)}; the minimum and the maximum of a
 * number plus 1 answered leeg over an empty source where ref and clingo answer 1 ({@code verwacht 1, werkelijk
 * leeg}), and a percentage of a sum answered leeg for every subject ({@code verwacht 1.90, werkelijk leeg}); a date
 * fold in a condition refused {@code voorwaarde buiten Phase-B scope}, which names no form, and so did a fold bound by
 * a Let, with or without a division beside it ({@code waarde buiten scope}). The minimum and the maximum over a date,
 * which only a hand-written document spells, refused by the member-type sentence of {@code kern.Vouwwaarde}. One row is a flank, green before and
 * after: the number folds the branch already played. The row over a date fold in a condition pinned its refusal by name
 * until the fold there became an accumulate too (2026-10-01); red against that transpiler in {@code voorwaarde met een
 * ordenende vouw over een datumbron …}, it plays since.
 */
class DateFoldOverRoleWitnessTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    @Test void the_first_and_the_last_date_over_a_role() throws Exception {
        // The card's reproduction and its twin: "De eerste vinddag van een Jutter moet berekend worden als de eerste
        // van de vinddag van alle vondsten van de Jutter", and "de laatste van". J1's first member is neither the
        // earliest nor the latest date and its last filled member is the earliest, so a positional reading fails
        // both; J2 has no vondsten, J3 one without vinddag, J4 one. ref and clingo: J1 05-03-2026 and 20-03-2026.
        String ir = document(regel("eerste vinddag", "Jutter", "eerste vinddag", fold("first", "vinddag", null)),
                regel("laatste vinddag", "Jutter", "laatste vinddag", fold("last", "vinddag", null)));
        assertNull(play(ir, caseDocument(situation(true, false, null),
                        perJutter("eerste vinddag", date("2026-03-05"), null, null, date("2025-12-31")),
                        perJutter("laatste vinddag", date("2026-03-20"), null, null, date("2025-12-31")))),
                "eerste en laatste over de vondsten");
    }

    @Test void set_and_initialised() throws Exception {
        // "moet gesteld worden op de eerste van …", and "moet geïnitialiseerd worden op de eerste van …" where J4
        // carries its own eerste vinddag, which the initialisation leaves standing. ref and clingo: J4 01-01-2020.
        String equal = document(set("eerste vinddag", "Jutter", "eerste vinddag", fold("first", "vinddag", null)));
        assertNull(play(equal, caseDocument(situation(true, false, null),
                        perJutter("eerste vinddag", date("2026-03-05"), null, null, date("2025-12-31")))),
                "gesteld");
        String init = document(initialisatie("eerste vinddag", "Jutter", "eerste vinddag", fold("first", "vinddag", null)));
        assertNull(play(init, caseDocument(situation(true, false, "2020-01-01"),
                        perJutter("eerste vinddag", date("2026-03-05"), null, null, date("2020-01-01")))),
                "geïnitialiseerd");
    }

    @Test void a_condition_on_the_elements() throws Exception {
        // "… van alle vondsten van de Jutter die zwaar zijn", zwaar derived by a rule declared after the fold (a Vondst
        // is zwaar when its gewicht is above 5: V2, V3 and V6), and "… die later zijn dan 10-03-2026", a test on the
        // folded date itself. ref and clingo: J1 20-03-2026 and 12-03-2026; J4 31-12-2025 and leeg.
        String zwaar = document(regel("eerste vinddag", "Jutter", "eerste vinddag", fold("first", "vinddag", CHARACTERISTIC_CHECK_ZWAAR)),
                characteristicRule("zwaar", "Vondst", comparison("greater", own("gewicht", "Vondst"), numberLiteral("5"))));
        assertNull(play(zwaar, caseDocument(situation(false, false, null),
                        perJutter("eerste vinddag", date("2026-03-20"), null, null, date("2025-12-31")))),
                "een later afgeleid kenmerk als voorwaarde");
        String later = document(regel("eerste vinddag", "Jutter", "eerste vinddag",
                fold("first", "vinddag", comparison("later", own("vinddag", "Vondst"), dateLiteral("2026-03-10")))));
        assertNull(play(later, caseDocument(situation(true, false, null),
                        perJutter("eerste vinddag", date("2026-03-12"), null, null, null))),
                "een toets op de gevouwen datum zelf");
    }

    @Test void a_date_that_is_derived_later() throws Exception {
        // "De laatste bergdag van een Jutter moet berekend worden als de laatste van de bergdag van alle vondsten van
        // de Jutter", declared before "De bergdag van een Vondst moet berekend worden als de vinddag van de Vondst plus
        // 1 dag". ref and clingo: J1 21-03-2026, J4 01-01-2026.
        String ir = document(regel("laatste bergdag", "Jutter", "laatste bergdag", fold("last", "bergdag", null)),
                regel("bergdag", "Vondst", "bergdag", binary("plus", own("vinddag", "Vondst"), duration("1"), DATE)));
        assertNull(play(ir, caseDocument(situation(true, false, null),
                        perJutter("laatste bergdag", date("2026-03-21"), null, null, date("2026-01-01")))),
                "een vouw over een later afgeleide datum");
    }

    @Test void the_date_fold_plus_and_minus_a_duration() throws Exception {
        // "(de eerste van de vinddag van alle vondsten van de Jutter) plus 30 dagen" and "(de laatste van …) min 7
        // dagen"; an empty date propagates, so J2 and J3 stay leeg. ref and clingo: J1 04-04-2026 and 13-03-2026.
        String plus = document(regel("meldgrens", "Jutter", "meldgrens",
                binary("plus", fold("first", "vinddag", null), duration("30"), DATE)));
        assertNull(play(plus, caseDocument(situation(true, false, null),
                        perJutter("meldgrens", date("2026-04-04"), null, null, date("2026-01-30")))),
                "de eerste plus 30 dagen");
        String min = document(regel("meldgrens", "Jutter", "meldgrens",
                binary("minus", fold("last", "vinddag", null), duration("7"), DATE)));
        assertNull(play(min, caseDocument(situation(true, false, null),
                        perJutter("meldgrens", date("2026-03-13"), null, null, date("2025-12-24")))),
                "de laatste min 7 dagen");
    }

    @Test void a_date_plus_a_number_fold_as_a_duration() throws Exception {
        // "De meldgrens van een Jutter moet berekend worden als zijn startdag plus de maximale waarde van de zoekduur
        // van alle vondsten van de Jutter", the zoekduur in dagen; an empty duration coalesces to 0, an empty date
        // propagates. ref and clingo: J1 21-03-2026, J2 01-03-2026, J3 leeg, J4 06-01-2026.
        String ir = document(regel("meldgrens", "Jutter", "meldgrens",
                binary("plus", own("startdag", "Jutter"), fold("maximum", "zoekduur", null), DATE)));
        assertNull(play(ir, caseDocument(situation(true, false, null),
                        perJutter("meldgrens", date("2026-03-21"), date("2026-03-01"), null, date("2026-01-06")))),
                "een datum plus het maximum van een duur");
    }

    @Test void the_duration_between_the_first_and_the_last() throws Exception {
        // "de tijdsduur van de eerste van … tot de laatste van … in hele dagen"; an empty operand propagates. ref and
        // clingo: J1 15, J4 0.
        String ir = document(regel("spanne", "Jutter", "spanne", "{\"node\":\"BuiltinCall\",\"kind\":\"duration\",\"arguments\":["
                + fold("first", "vinddag", null) + "," + fold("last", "vinddag", null) + "],\"type\":" + DAGEN + "}"));
        assertNull(play(ir, caseDocument(situation(true, false, null),
                        perJutter("spanne", number("15"), null, null, number("0")))),
                "de tijdsduur tussen twee datumvouwen");
    }

    @Test void two_roles_deep() throws Exception {
        // "de eerste van de vinddag van alle vondsten van alle beheerde jutters van de Strandvoogd": S1 beheert J1 and
        // J3, S2 beheert J2, S3 beheert J4, S4 niemand. ref and clingo: S1 05-03-2026 and 20-03-2026, S3 31-12-2025.
        String ir = document(regel("eerste vinddag", "Strandvoogd", "eerste vinddag", pathFold("first", "vinddag")),
                regel("laatste vinddag", "Strandvoogd", "laatste vinddag", pathFold("last", "vinddag")));
        List<String> expected = new ArrayList<>();
        expected.addAll(perVoogd("eerste vinddag", date("2026-03-05"), null, date("2025-12-31"), null));
        expected.addAll(perVoogd("laatste vinddag", date("2026-03-20"), null, date("2025-12-31"), null));
        assertNull(play(ir, caseDocument(situation(true, false, null), expected)), "de vouw over een pad van twee rollen");
    }

    @Test void a_date_and_time() throws Exception {
        // "Het eerste vindtijdstip van een Jutter moet berekend worden als de eerste van het vindtijdstip van alle
        // vondsten van de Jutter". ref: J1 05-03-2026 23:59:59.999, J4 31-12-2025 00:00:00.001 (clingo does not fold a
        // date-and-time over a set).
        String ir = document(regel("eerste vindtijdstip", "Jutter", "eerste vindtijdstip", fold("first", "vindtijdstip", null)));
        assertNull(play(ir, caseDocument(situation(true, true, null),
                        perJutter("eerste vindtijdstip", tijdstip("2026-03-05T23:59:59.999"), null, null,
                                tijdstip("2025-12-31T00:00:00.001")))),
                "de eerste van een datum-en-tijd");
    }

    @Test void a_number_fold_as_operand() throws Exception {
        // ch. 1.2: plus coalesces an empty operand, so the minimum or maximum of an empty source plus 1 is 1; a
        // percentage of an empty sum is 0. ref and clingo answer each value below.
        String min = document(regel("telling", "Jutter", "telling", binary("plus", fold("minimum", "gewicht", null), numberLiteral("1"), WHOLE)));
        assertNull(play(min, caseDocument(situation(true, false, null), perJutter("telling", number("3"), number("1"), number("1"), number("10")))),
                "het minimum plus 1");
        String max = document(regel("telling", "Jutter", "telling", binary("plus", fold("maximum", "gewicht", null), numberLiteral("1"), WHOLE)));
        assertNull(play(max, caseDocument(situation(true, false, null), perJutter("telling", number("8"), number("1"), number("1"), number("10")))),
                "het maximum plus 1");
        String pct = document(regel("uitkomst", "Jutter", "uitkomst", binary("percentageOf", numberLiteral("10"),
                fold("sum", "gewicht", null), "{\"node\":\"Number\",\"whole\":false,\"decimals\":0,\"sign\":\"unspecified\"}")));
        assertNull(play(pct, caseDocument(situation(true, false, null), perJutter("uitkomst", number("1.90"), number("0.00"), number("0.00"), number("0.90")))),
                "een percentage van een som");
        String clamped = document(regel("telling", "Jutter", "telling", "{\"node\":\"Clamp\",\"base\":"
                + binary("minus", count(), numberLiteral("2"), WHOLE) + ",\"minimum\":" + numberLiteral("0") + ",\"type\":" + WHOLE + "}"));
        assertNull(play(clamped, caseDocument(situation(true, false, null), perJutter("telling", number("2"), number("0"), number("0"), number("0")))),
                "een begrenzing om een telling");
        String nav = document(regel("telling", "Jutter", "telling", binary("plus", count(),
                "{\"node\":\"AttributeReference\",\"attribute\":\"bonus\",\"subjectType\":\"Strandvoogd\",\"path\":[{\"node\":\"RoleBinding\","
                        + "\"factType\":\"StrandvoogdBeheertJutter\",\"role\":\"strandvoogd\"}]}", WHOLE)));
        assertNull(play(nav, caseDocument(situation(true, false, null), perJutter("telling", number("104"), number("0"), number("101"), number("1")))),
                "een telling plus een genavigeerde lezing");
    }

    @Test void the_number_folds_that_already_played_play_equally() throws Exception {
        // A flank, green before and after: the count plus 1 (the card's k1b), the sum plus 1, the sum reduced by 1.
        String count = document(regel("telling", "Jutter", "telling", binary("plus", count(), numberLiteral("1"), WHOLE)));
        assertNull(play(count, caseDocument(situation(true, false, null), perJutter("telling", number("5"), number("1"), number("2"), number("2")))),
                "het aantal plus 1");
        String sum = document(regel("telling", "Jutter", "telling", binary("plus", fold("sum", "gewicht", null), numberLiteral("1"), WHOLE)));
        assertNull(play(sum, caseDocument(situation(true, false, null), perJutter("telling", number("20"), number("1"), number("1"), number("10")))),
                "de som plus 1");
        String vm = document(regel("telling", "Jutter", "telling", binary("reducedBy", fold("sum", "gewicht", null), numberLiteral("1"), WHOLE)));
        assertNull(play(vm, caseDocument(situation(true, false, null), perJutter("telling", number("18"), null, null, number("8")))),
                "de som verminderd met 1");
    }

    @Test void a_date_fold_in_a_condition() throws Exception {
        // "Een Jutter is vroeg indien de eerste van de vinddag van alle vondsten van de Jutter eerder is dan
        // 01-03-2026", and the same with the minimum of the vinddag, which only a hand-written document can spell: ref
        // and clingo answer J4 vroeg. Refused by name until the fold in a condition became an accumulate on the rule
        // whose result the test reads (2026-10-01).
        for (String kind : new String[] { "first", "minimum" }) {
            String ir = document(characteristicRule("vroeg", "Jutter",
                    comparison("earlier", fold(kind, "vinddag", null), dateLiteral("2026-03-01"))));
            List<String> vroeg = new ArrayList<>();
            for (String j : List.of("J1", "J2", "J3", "J4"))
                vroeg.add("{\"kind\":\"characteristic" + (j.equals("J4") ? "Present" : "Absent") + "\",\"label\":\"" + j
                        + "\",\"characteristic\":\"vroeg\"}");
            assertNull(play(ir, caseDocument(situation(true, false, null), vroeg)), kind);
        }
    }

    @Test void the_minimum_and_the_maximum_over_dates() throws Exception {
        // "de minimale waarde van de vinddag van alle vondsten van de Jutter" and "de maximale waarde van …", which
        // the RegelSpraak frontend types out and a hand-written document carries (WF-038 admits a date under an
        // ordering fold): the same folds as eerste and laatste (RUNTIME-SEMANTICS ch. 1.2e), bare and as the operand of
        // a date plus a duration. ref and clingo: the values of the eerste/laatste rows.
        String bare = document(regel("eerste vinddag", "Jutter", "eerste vinddag", fold("minimum", "vinddag", null)),
                regel("laatste vinddag", "Jutter", "laatste vinddag", fold("maximum", "vinddag", null)));
        assertNull(play(bare, caseDocument(situation(true, false, null),
                        perJutter("eerste vinddag", date("2026-03-05"), null, null, date("2025-12-31")),
                        perJutter("laatste vinddag", date("2026-03-20"), null, null, date("2025-12-31")))),
                "minimum en maximum als hele waarde");
        String plus = document(regel("meldgrens", "Jutter", "meldgrens",
                binary("plus", fold("minimum", "vinddag", null), duration("30"), DATE)));
        assertNull(play(plus, caseDocument(situation(true, false, null),
                        perJutter("meldgrens", date("2026-04-04"), null, null, date("2026-01-30")))),
                "het minimum plus 30 dagen");
    }

    @Test void a_fold_under_also_applies() throws Exception {
        // "De telling van een Jutter moet berekend worden als de vangst plus 1. Daarbij geldt: de vangst is het aantal
        // vondsten van de Jutter": a Let (IR-SPEC ch. 4.10) whose bound value is the fold. ref and clingo: J1 5, J2 1,
        // J3 2, J4 2.
        String ir = document(regel("telling", "Jutter", "telling", let("vangst", count(),
                binary("plus", binder("vangst"), numberLiteral("1"), WHOLE), WHOLE)));
        assertNull(play(ir, caseDocument(situation(true, false, null), perJutter("telling", number("5"), number("1"), number("2"), number("2")))),
                "een vouw als gebonden waarde van een Let");
    }

    // ---------------- documents ----------------

    private static final String WHOLE = "{\"node\":\"Number\",\"whole\":true,\"decimals\":0,\"sign\":\"unspecified\"}";
    private static final String DAGEN = "{\"node\":\"Number\",\"whole\":true,\"decimals\":0,\"sign\":\"unspecified\",\"unit\":"
            + "{\"node\":\"Unit\",\"abbreviation\":\"dag\",\"canonical\":\"dag\",\"factorToCanonical\":{\"num\":1,\"den\":1}}}";
    private static final String DATE = "{\"node\":\"Date\",\"precision\":\"day\"}";
    private static final String TIJDSTIP = "{\"node\":\"Date\",\"precision\":\"millisecond\"}";
    private static final String CHARACTERISTIC_CHECK_ZWAAR = "{\"node\":\"CharacteristicCheck\",\"characteristic\":\"zwaar\",\"negated\":false}";

    /** Strandvoogd, Jutter and Vondst; one jutter heeft meerdere vondsten, one strandvoogd beheert meerdere beheerde
     *  jutters. */
    private static final String MODEL = "{\"objectTypes\":["
            + objectType("Strandvoogd", "Strandvoogden", true, List.of(attr("eerste vinddag", DATE), attr("laatste vinddag", DATE),
                    attr("bonus", WHOLE)), "")
            + "," + objectType("Jutter", "Jutters", true, List.of(attr("eerste vinddag", DATE), attr("laatste vinddag", DATE),
                    attr("laatste bergdag", DATE), attr("meldgrens", DATE), attr("startdag", DATE), attr("telling", WHOLE),
                    attr("uitkomst", "{\"node\":\"Number\",\"whole\":false,\"decimals\":2,\"sign\":\"unspecified\"}"),
                    attr("spanne", DAGEN), attr("eerste vindtijdstip", TIJDSTIP), attr("gewicht", WHOLE)),
                    "{\"node\":\"Characteristic\",\"name\":\"vroeg\",\"kind\":\"adjectival\"}")
            + "," + objectType("Vondst", "Vondsten", false, List.of(attr("vinddag", DATE), attr("bergdag", DATE),
                    attr("vindtijdstip", TIJDSTIP), attr("gewicht", WHOLE), attr("zoekduur", DAGEN)),
                    "{\"node\":\"Characteristic\",\"name\":\"zwaar\",\"kind\":\"adjectival\"}")
            + "],\"factTypes\":["
            + factType("JutterHeeftVondst", "jutter", "Jutter", "vondst", "vondsten", "Vondst") + ","
            + factType("StrandvoogdBeheertJutter", "strandvoogd", "Strandvoogd", "beheerde jutter", "beheerde jutters", "Jutter")
            + "],\"parameters\":[],\"domains\":[],\"unitSystems\":[],\"dimensions\":[],\"timelines\":[],\"dayKinds\":[]}";

    private static String objectType(String naam, String plural, boolean animate, List<String> attributes, String characteristic) {
        return "{\"node\":\"ObjectType\",\"name\":\"" + naam + "\",\"animate\":" + animate + ",\"plural\":\"" + plural
                + "\",\"attributes\":[" + String.join(",", attributes) + "],\"characteristics\":[" + characteristic + "]}";
    }

    private static String attr(String naam, String datatype) {
        return "{\"node\":\"Attribute\",\"name\":\"" + naam + "\",\"datatype\":" + datatype + "}";
    }

    private static String factType(String naam, String oneRole, String oneType, String manyRole, String manyPlural, String manyType) {
        return "{\"node\":\"FactType\",\"name\":\"" + naam + "\",\"reciprocal\":false,\"roles\":["
                + "{\"node\":\"Role\",\"name\":\"" + oneRole + "\",\"objectType\":\"" + oneType + "\",\"many\":false},"
                + "{\"node\":\"Role\",\"name\":\"" + manyRole + "\",\"objectType\":\"" + manyType + "\",\"plural\":\"" + manyPlural
                + "\",\"many\":true}]}";
    }

    private static String own(String naam, String type) {
        return "{\"node\":\"AttributeReference\",\"attribute\":\"" + naam + "\",\"subjectType\":\"" + type + "\"}";
    }

    private static String numberLiteral(String n) {
        return "{\"node\":\"Literal\",\"value\":{\"number\":\"" + n + "\"},\"type\":" + WHOLE + "}";
    }

    private static String duration(String n) {
        return "{\"node\":\"Literal\",\"value\":{\"number\":\"" + n + "\"},\"type\":" + DAGEN + "}";
    }

    private static String dateLiteral(String iso) {
        return "{\"node\":\"Literal\",\"value\":{\"date\":\"" + iso + "\"},\"type\":" + DATE + "}";
    }

    private static String binary(String op, String left, String right, String type) {
        return "{\"node\":\"Binary\",\"op\":\"" + op + "\",\"left\":" + left + ",\"right\":" + right + ",\"type\":" + type + "}";
    }

    private static String let(String binder, String value, String body, String type) {
        return "{\"node\":\"Let\",\"type\":" + type + ",\"binder\":\"" + binder + "\",\"value\":" + value
                + ",\"body\":" + body + "}";
    }

    private static String binder(String naam) {
        return "{\"node\":\"BinderRef\",\"name\":\"" + naam + "\"}";
    }

    private static String comparison(String op, String left, String right) {
        return "{\"node\":\"Comparison\",\"op\":\"" + op + "\",\"left\":" + left + ",\"right\":" + right + "}";
    }

    /** A fold of {@code kind} over {@code lid} of the vondsten of the Jutter; {@code filter} null for none. */
    private static String fold(String kind, String member, String filter) {
        return "{\"node\":\"Aggregation\",\"kind\":\"" + kind + "\",\"arguments\":[" + own(member, "Vondst") + "],\"elementType\":"
                + "\"Vondst\"" + (filter == null ? "" : ",\"filter\":" + filter) + ",\"sourceRole\":{\"node\":\"RoleBinding\","
                + "\"factType\":\"JutterHeeftVondst\",\"role\":\"vondst\"},\"type\":" + foldType(kind, member) + "}";
    }

    /** A fold of {@code kind} over {@code lid} of the vondsten of all beheerde jutters of the Strandvoogd. */
    private static String pathFold(String kind, String member) {
        return "{\"node\":\"Aggregation\",\"kind\":\"" + kind + "\",\"arguments\":[" + own(member, "Vondst") + "],\"elementType\":"
                + "\"Vondst\",\"sourcePath\":[{\"node\":\"RoleBinding\",\"factType\":\"StrandvoogdBeheertJutter\",\"role\":"
                + "\"beheerde jutter\"},{\"node\":\"RoleBinding\",\"factType\":\"JutterHeeftVondst\",\"role\":\"vondst\"}],"
                + "\"type\":" + foldType(kind, member) + "}";
    }

    private static String foldType(String kind, String member) {
        return switch (member) {
            case "vinddag", "bergdag" -> DATE;
            case "vindtijdstip" -> TIJDSTIP;
            case "zoekduur" -> DAGEN;
            default -> WHOLE;
        };
    }

    /** "het aantal vondsten van de Jutter". */
    private static String count() {
        return "{\"node\":\"Aggregation\",\"kind\":\"count\",\"arguments\":[" + own("vondsten", "Jutter") + "],\"elementType\":"
                + "\"Vondst\",\"sourceRole\":{\"node\":\"RoleBinding\",\"factType\":\"JutterHeeftVondst\",\"role\":\"vondst\"},"
                + "\"type\":{\"node\":\"Number\",\"whole\":true,\"decimals\":0,\"sign\":\"nonNegative\"}}";
    }

    /** A computing rule ("moet berekend worden als"). */
    private static String regel(String naam, String type, String target, String value) {
        return version(naam, type, target, value, null, "calculation");
    }

    /** A computing rule written "moet gesteld worden op". */
    private static String set(String naam, String type, String target, String value) {
        return version(naam, type, target, value, null, "equation");
    }

    private static String initialisatie(String naam, String type, String target, String value) {
        return version(naam, type, target, value, null, null);
    }

    private static String characteristicRule(String characteristic, String type, String condition) {
        return version(characteristic, type, characteristic, "{\"node\":\"Literal\",\"value\":{\"boolean\":true},\"type\":{\"node\":\"Boolean\"}}",
                condition, "");
    }

    /** One rule with one version: {@code presentation} a computing rule's tag, null for an initialisation, empty for a
     *  characteristic attribution. */
    private static String version(String naam, String type, String target, String value, String condition, String presentation) {
        return "{\"node\":\"Rule\",\"name\":\"" + naam + "\",\"versions\":[{\"validity\":{\"node\":\"Validity\"},\"target\":"
                + own(target, type) + ",\"value\":" + value + (condition == null ? "" : ",\"condition\":" + condition)
                + ",\"initialisation\":" + (presentation == null)
                + (presentation == null || presentation.isEmpty() ? "" : ",\"presentation\":\"" + presentation + "\"") + "}]}";
    }

    private static String document(String... rules) {
        return "{\"format\":\"uchar-ir-v1\",\"model\":" + MODEL + ",\"rules\":{\"rules\":[" + String.join(",", rules) + "],"
                + "\"dayKindDefinitions\":[],\"objectCreations\":[],\"allocations\":[],\"consistencies\":[],\"uniquenesses\":[],"
                + "\"factCreations\":[],\"startPointDeterminations\":[],\"ruleGroups\":[]},\"diagnostics\":[]}";
    }

    // ---------------- cases ----------------

    /** The situation of every row: four Jutters with the four source shapes, and four Strandvoogden.
     *  J1: V1 12-03-2026 (gewicht 4, zoekduur 10), V2 20-03-2026 (7, 3), V3 without vinddag (6, 20), V4 05-03-2026 (2);
     *  J2 no vondsten; J3 V5 without vinddag and gewicht; J4 V6 31-12-2025 (9, 5). V2, V3 and V6 zwaar when
     *  {@code zwaar} is set; the vindtijdstip when {@code tijdstip} is set; J4's own eerste vinddag when given. */
    private static List<String> situation(boolean zwaar, boolean tijdstip, String ownFirstFindDayJ4) {
        List<String> out = new ArrayList<>();
        out.add(instance("S1", "Strandvoogd", "\"bonus\":{\"number\":\"100\"}", "\"beheerde jutters\":[\"J1\",\"J3\"]", false));
        out.add(instance("S2", "Strandvoogd", "", "\"beheerde jutters\":[\"J2\"]", false));
        out.add(instance("S3", "Strandvoogd", "", "\"beheerde jutters\":[\"J4\"]", false));
        out.add(instance("S4", "Strandvoogd", "", "", false));
        out.add(instance("J1", "Jutter", "\"startdag\":" + date("2026-03-01"), "", false));
        out.add(instance("J2", "Jutter", "\"startdag\":" + date("2026-03-01"), "", false));
        out.add(instance("J3", "Jutter", "", "", false));
        out.add(instance("J4", "Jutter", (ownFirstFindDayJ4 == null ? "" : "\"eerste vinddag\":" + date(ownFirstFindDayJ4) + ",")
                + "\"startdag\":" + date("2026-01-01"), "", false));
        out.add(vondst("V1", "J1", "2026-03-12", "4", "10", false, tijdstip ? "2026-03-12T10:00:00.000" : null));
        out.add(vondst("V2", "J1", "2026-03-20", "7", "3", zwaar, tijdstip ? "2026-03-20T08:30:00.000" : null));
        out.add(vondst("V3", "J1", null, "6", "20", zwaar, null));
        out.add(vondst("V4", "J1", "2026-03-05", "2", null, false, tijdstip ? "2026-03-05T23:59:59.999" : null));
        out.add(vondst("V5", "J3", null, null, null, false, null));
        out.add(vondst("V6", "J4", "2025-12-31", "9", "5", zwaar, tijdstip ? "2025-12-31T00:00:00.001" : null));
        return out;
    }

    private static String vondst(String label, String jutter, String vinddag, String weight, String zoekduur, boolean zwaar,
                                 String tijdstip) {
        List<String> a = new ArrayList<>();
        if (vinddag != null) a.add("\"vinddag\":" + date(vinddag));
        if (weight != null) a.add("\"gewicht\":" + number(weight));
        if (zoekduur != null) a.add("\"zoekduur\":" + number(zoekduur));
        if (tijdstip != null) a.add("\"vindtijdstip\":" + tijdstip(tijdstip));
        return instance(label, "Vondst", String.join(",", a), "\"jutter\":[\"" + jutter + "\"]", zwaar);
    }

    private static String instance(String label, String type, String attributes, String roles, boolean zwaar) {
        return "{\"label\":\"" + label + "\",\"objectType\":\"" + type + "\",\"attributes\":{" + attributes + "},\"roles\":{"
                + roles + "},\"characteristics\":[" + (zwaar ? "\"zwaar\"" : "") + "]}";
    }

    private static String date(String iso) { return "{\"date\":\"" + iso + "\"}"; }

    private static String tijdstip(String iso) { return "{\"dateTime\":\"" + iso + "\"}"; }

    private static String number(String n) { return "{\"number\":\"" + n + "\"}"; }

    /** One expectation per Jutter J1–J4 on {@code attribute}; a null value expects leeg. */
    private static List<String> perJutter(String attribute, String... waarden) {
        return per(List.of("J1", "J2", "J3", "J4"), attribute, waarden);
    }

    private static List<String> perVoogd(String attribute, String... waarden) {
        return per(List.of("S1", "S2", "S3", "S4"), attribute, waarden);
    }

    private static List<String> per(List<String> labels, String attribute, String... waarden) {
        List<String> out = new ArrayList<>();
        for (int i = 0; i < labels.size(); i++)
            out.add(waarden[i] == null
                    ? "{\"kind\":\"attributeEmpty\",\"label\":\"" + labels.get(i) + "\",\"attribute\":\"" + attribute + "\"}"
                    : "{\"kind\":\"attributeValue\",\"label\":\"" + labels.get(i) + "\",\"attribute\":\"" + attribute
                      + "\",\"value\":" + waarden[i] + "}");
        return out;
    }

    @SafeVarargs
    private static String caseDocument(List<String> instances, List<String>... expected) {
        List<String> all = new ArrayList<>();
        for (List<String> v : expected) all.addAll(v);
        return "{\"format\":\"uchar-case-v0\",\"asOfDate\":null,\"input\":{\"instances\":[" + String.join(",", instances)
                + "],\"parameters\":{}},\"expected\":[" + String.join(",", all) + "]}";
    }

    private static Path temporary(String ir) throws Exception {
        Path tmp = Files.createTempFile("datumvouw-over-rol", ".json");
        tmp.toFile().deleteOnExit();
        Files.writeString(tmp, ir);
        return tmp;
    }

    /** The exec-model branch's verdict on one case: null when every expectation holds. */
    private static String play(String ir, String caseDocument) throws Exception {
        return CaseRunner.playCase(temporary(ir), JSON.readTree(caseDocument), false);
    }
}
