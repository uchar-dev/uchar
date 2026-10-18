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
 * A fold over a role in the toekenning of an object creation ("Een Jutter heeft een afrekening met heffing gelijk aan
 * 10% van de som van het gewicht van alle vondsten van de Jutter"): the six folds of a set source, bare and as an
 * operand, two toekenningen with a fold each, a creation over a to-many role (the creation marker as its guard), and a
 * clamp, a time span and a navigated read around a fold. RUNTIME-SEMANTICS ch. 5.5: the member initialisations are
 * evaluated on the subject, as a rule's value is; ch. 1.2 gives the empty source of each fold and the empty operand of
 * each operator; ch. 2.4 restates the value into the declared unit of the member it is written to. The same folds in a
 * rule's value play since a fold became an accumulate beside the subject pattern. Every document here is authored in
 * this class on Jutterswet material, each one measured form of the card's form space with the values {@code ref} and
 * clingo answer (clingo does not translate a unit restatement at an initialisation; there {@code ref} alone). The
 * Afrekening is read back on its Jutter, because a case names no minted instance.
 *
 * <p>Situation (each case): J1 with V1 (vinddag 12-03-2026, gewicht 4), V2 (05-03-2026, 7) and V3 (no values); J2
 * without vondsten; J3 with only V4 (no values); J4 with V5 (20-03-2026, 9).
 *
 * <p><b>Red before the fix</b> (measured 2026-10-01 on the transpiler of the claim, exec-model branch): a creation
 * lowered a toekenning with a number fold through an evaluator of its own, which answered a percentage leeg for every
 * subject ({@code verwacht 1.10, werkelijk leeg}) and bound a minimum or a maximum without a count, so over an empty
 * source the creation did not fire at all ({@code verwacht 1, werkelijk leeg}; a constant beside it stayed leeg too);
 * it refused a clamp and a navigated read around a fold ({@code aggregatie-toekenning buiten scope}), and every date
 * fold ({@code toekenning-waarde buiten scope}). A toekenning without a fold was written without the restatement into
 * the member's unit ({@code verwacht 2000, werkelijk 2}); a division in a toekenning answered consistent where a
 * divisor was leeg, because a creation has no companion that raises the Fout; and a date fold in the creation's
 * condition refused in a sentence that names no form. One row is a flank, green before and after: the number folds
 * in the creation's condition. The division row was a named refusal from that day; it plays with the Fout reported
 * since the follow-up round of the same day, and was red against the refusal ({@code Excluded … kanaal-deponerende
 * toekenning (deling)}). The row over five folds pinned a refusal until a consequence bound more than nine variables
 * (2026-10-01); red against that transpiler in {@code te veel gebonden variabelen: globals en vouwen}, it plays since.
 * The row over a date fold in the creation's condition pinned its refusal by name until that fold became an accumulate
 * on the creation's rule (2026-10-01); red against that transpiler in {@code voorwaarde met een ordenende vouw over een
 * datumbron …}, it plays since.
 */
class ObjectCreationFoldWitnessTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    // ---------------- the card's reproductions ----------------

    @Test void r1_ten_percent_of_a_sum() throws Exception {
        // The card's R1: "… met heffing gelijk aan 10% van de som van het gewicht van alle vondsten van de Jutter". An
        // empty sum is leeg, and 10% of leeg is 0 (ch. 1.2). ref and clingo: 1,10, 0,00, 0,00 and 0,90. The DRL-text
        // branch plays it too.
        String ir = document(false, creatie(null, memberInit("heffing", percent(10, SUM))), readBack("heffing"));
        String caseDocument = caseDocument(situation(), perJutter("heffing", number("1.10"), number("0.00"), number("0.00"), number("0.90")));
        assertNull(play(ir, caseDocument), "exectak");
        assertNull(playDrl(ir, caseDocument), "DRL-teksttak");
    }

    @Test void r2_the_minimum_of_an_empty_source_plus_one() throws Exception {
        // The card's R2: "… met weeggrens gelijk aan (de minimale waarde van het gewicht van alle vondsten van de
        // Jutter) plus 1". Over no vondsten, or only one without gewicht, the minimum is leeg and leeg plus 1 is 1.
        // ref and clingo: 5, 1, 1 and 10.
        String ir = document(false, creatie(null, memberInit("weeggrens", plus(MINIMUM, numberLiteral(1), WHOLE))),
                readBack("weeggrens"));
        assertNull(play(ir, caseDocument(situation(), perJutter("weeggrens", number("5"), number("1"), number("1"), number("10")))),
                "het minimum plus 1");
    }

    @Test void r3_the_first_vinddag() throws Exception {
        // The card's R3: "… met afrekendag gelijk aan de eerste van de vinddag van alle vondsten van de Jutter". ref and
        // clingo: 05-03-2026, leeg, leeg and 20-03-2026. Both branches play it since card
        // drl-teksttak-op-truth-maintenance; until then the DRL-text branch markered the fold ("TODO aggregatie EERSTE").
        String ir = document(false, creatie(null, memberInit("afrekendag", FIRST)), readBack("afrekendag"));
        String caseDocument = caseDocument(situation(), perJutter("afrekendag", date("2026-03-05"), null, null, date("2026-03-20")));
        assertNull(play(ir, caseDocument), "de eerste vinddag");
        assertNull(playDrl(ir, caseDocument), "de eerste vinddag, drl-tekst");
    }

    // ---------------- the folds as an operand, and their empty source ----------------

    @Test void the_number_folds_read_as_operand_and_an_empty_source_as_empty() throws Exception {
        // 10% of a count, a minimum and a maximum; a minimum min 1 (leeg min 1 is -1); a sum verminderd met 1 (leeg
        // verminderd met 1 stays leeg); a maximum plus 1. ref and clingo per row, J1 to J4.
        String[][] rows = {
                {"heffing", "10% van het aantal", "0.30", "0.00", "0.10", "0.10"},
                {"heffing", "10% van het minimum", "0.40", "0.00", "0.00", "0.90"},
                {"heffing", "10% van het maximum", "0.70", "0.00", "0.00", "0.90"},
                {"telling", "het minimum min 1", "3", "-1", "-1", "8"},
                {"telling", "de som verminderd met 1", "10", null, null, "8"},
                {"weeggrens", "het maximum plus 1", "8", "1", "1", "10"},
        };
        String[] waarden = {
                percent(10, COUNTING), percent(10, MINIMUM), percent(10, MAXIMUM),
                binary("minus", MINIMUM, numberLiteral(1), WHOLE, "(de minimale waarde …) min 1"),
                binary("reducedBy", SUM, numberLiteral(1), WHOLE, "(de som …) verminderd met 1"),
                plus(MAXIMUM, numberLiteral(1), WHOLE),
        };
        for (int i = 0; i < rows.length; i++) {
            String[] r = rows[i];
            String ir = document(false, creatie(null, memberInit(r[0], waarden[i])), readBack(r[0]));
            assertNull(play(ir, caseDocument(situation(), perJutter(r[0], number(r[2]), number(r[3]), number(r[4]), number(r[5])))),
                    r[1]);
        }
    }

    @Test void a_date_fold_plus_or_minus_a_duration() throws Exception {
        // "(de eerste van …) plus 30 dagen" and "(de laatste van …) min 7 dagen": an empty date propagates (ch. 1.2).
        // ref and clingo: 04-04-2026 and 19-04-2026, and 05-03-2026 and 13-03-2026; J2 and J3 leeg.
        String plusDuration = binary("plus", FIRST, days(30), DATE, "(de eerste van …) plus 30 dagen");
        String minusDuration = binary("minus", LAST, days(7), DATE, "(de laatste van …) min 7 dagen");
        assertNull(play(document(false, creatie(null, memberInit("afrekendag", plusDuration)), readBack("afrekendag")),
                caseDocument(situation(), perJutter("afrekendag", date("2026-04-04"), null, null, date("2026-04-19")))),
                "de eerste plus 30 dagen");
        assertNull(play(document(false, creatie(null, memberInit("afrekendag", minusDuration)), readBack("afrekendag")),
                caseDocument(situation(), perJutter("afrekendag", date("2026-03-05"), null, null, date("2026-03-13")))),
                "de laatste min 7 dagen");
    }

    @Test void a_bare_minimum_over_an_empty_source_mints_the_object_nevertheless() throws Exception {
        // "… met weeggrens gelijk aan de minimale waarde van het gewicht van alle vondsten van de Jutter, telling gelijk
        // aan 5": an empty minimum is leeg, and the creation mints its object all the same (ch. 5.5 asks only the
        // condition). ref and clingo: weeggrens 4, leeg, leeg and 9; telling 5 for every Jutter.
        String ir = document(false, creatie(null, memberInit("weeggrens", MINIMUM), memberInit("telling", numberLiteral(5))),
                readBack("weeggrens"), readBack("telling"));
        assertNull(play(ir, caseDocument(situation(), perJutter("weeggrens", number("4"), null, null, number("9")),
                perJutter("telling", number("5"), number("5"), number("5"), number("5")))), "een kaal minimum en een constante");
    }

    @Test void two_assignments_with_each_a_fold() throws Exception {
        // R1 and R2 in one creation, and a count beside a date fold. ref and clingo: the values of R1 and R2; telling
        // 3, 0, 1 and 1, afrekendag that of R3.
        String ir = document(false, creatie(null, memberInit("heffing", percent(10, SUM)),
                        memberInit("weeggrens", plus(MINIMUM, numberLiteral(1), WHOLE))),
                readBack("heffing"), readBack("weeggrens"));
        assertNull(play(ir, caseDocument(situation(), perJutter("heffing", number("1.10"), number("0.00"), number("0.00"), number("0.90")),
                perJutter("weeggrens", number("5"), number("1"), number("1"), number("10")))), "R1 en R2 samen");
        String numberAndDate = document(false, creatie(null, memberInit("telling", COUNTING), memberInit("afrekendag", FIRST)),
                readBack("telling"), readBack("afrekendag"));
        assertNull(play(numberAndDate, caseDocument(situation(), perJutter("telling", number("3"), number("0"), number("1"), number("1")),
                perJutter("afrekendag", date("2026-03-05"), null, null, date("2026-03-20")))), "een telling en een datumvouw");
    }

    @Test void the_marking_as_guard_at_more_afrekeningen() throws Exception {
        // "Eén afrekenaar heeft meerdere afrekeningen": the creation keys its guard on the creation marker, and the
        // Jutter reads back "de som van de heffing van alle afrekeningen van de Jutter". ref and clingo: the values of
        // R1 and of R2.
        assertNull(play(document(true, creatie(null, memberInit("heffing", percent(10, SUM))), readBackSum("heffing", TWO_DECIMALS)),
                caseDocument(situation(), perJutter("heffing", number("1.10"), number("0.00"), number("0.00"), number("0.90")))),
                "10% van de som");
        assertNull(play(document(true, creatie(null, memberInit("weeggrens", plus(MINIMUM, numberLiteral(1), WHOLE))),
                        readBackSum("weeggrens", WHOLE)),
                caseDocument(situation(), perJutter("weeggrens", number("5"), number("1"), number("1"), number("10")))),
                "het minimum plus 1");
    }

    @Test void a_clamp_a_duration_and_a_navigated_read_around_a_fold() throws Exception {
        // "(het aantal vondsten van de Jutter) min 2, met een minimum van 0"; "de tijdsduur van de eerste van … tot de
        // laatste van … in hele dagen"; "de bonus van de strandvoogd van de Jutter plus (de som …)", with S1's bonus 3.
        // ref and clingo: 1, 0, 0 and 0; 7, leeg, leeg and 0 dagen; 14, 3, 3 and 12.
        String clamped = "{\"node\":\"Clamp\",\"base\":" + binary("minus", COUNTING, numberLiteral(2), WHOLE,
                "(het aantal vondsten van de Jutter) min 2") + ",\"minimum\":" + numberLiteral(0) + ",\"type\":" + WHOLE
                + ",\"source\":{\"snippet\":\"(het aantal vondsten van de Jutter) min 2, met een minimum van 0\"}}";
        assertNull(play(document(false, creatie(null, memberInit("telling", clamped)), readBack("telling")),
                caseDocument(situation(), perJutter("telling", number("1"), number("0"), number("0"), number("0")))), "de begrenzing");
        String duration = "{\"node\":\"BuiltinCall\",\"kind\":\"duration\",\"arguments\":[" + FIRST + "," + LAST
                + "],\"type\":" + numberIn(DG) + ",\"source\":{\"snippet\":\"de tijdsduur van de eerste van … tot de laatste"
                + " van … in hele dagen\"}}";
        assertNull(play(document(false, creatie(null, memberInit("spanne", duration)), readBack("spanne")),
                caseDocument(situation(), perJutter("spanne", number("7"), null, null, number("0")))), "de tijdsduur");
        String bonus = "{\"node\":\"AttributeReference\",\"attribute\":\"bonus\",\"subjectType\":\"Strandvoogd\",\"source\":"
                + "{\"snippet\":\"de bonus van de strandvoogd van de Jutter\"},\"path\":[" + roleBinding("StrandvoogdBeheertJutter",
                "strandvoogd") + "]}";
        List<String> withStrandvoogd = new ArrayList<>(List.of(instance("S1", "Strandvoogd", "\"bonus\":{\"number\":\"3\"}", "")));
        for (String j : situation()) withStrandvoogd.add(j.replace("\"objectType\":\"Jutter\",\"attributes\":{},\"roles\":{}",
                "\"objectType\":\"Jutter\",\"attributes\":{},\"roles\":{\"strandvoogd\":[\"S1\"]}"));
        assertNull(play(document(false, creatie(null, memberInit("telling", plus(bonus, SUM, WHOLE))), readBack("telling")),
                caseDocument(withStrandvoogd, perJutter("telling", number("14"), number("3"), number("3"), number("12")))),
                "een genavigeerde lezing plus de som");
    }

    @Test void a_subselection_on_a_later_derived_characteristic() throws Exception {
        // "… met afrekendag gelijk aan de laatste van de vinddag van alle vondsten van de Jutter die zwaar zijn", with
        // "Een Vondst is zwaar indien zijn gewicht groter is dan 5" declared after the creation. ref and clingo:
        // 05-03-2026 (V2 is the zware vondst of J1), leeg, leeg and 20-03-2026.
        String zwaar = "{\"node\":\"Rule\",\"name\":\"zwaar\",\"versions\":[{\"validity\":{\"node\":\"Validity\"},\"target\":"
                + "{\"node\":\"AttributeReference\",\"attribute\":\"zwaar\",\"subjectType\":\"Vondst\",\"source\":{\"snippet\":"
                + "\"Een Vondst is zwaar\"}},\"value\":{\"node\":\"Literal\",\"value\":{\"boolean\":true},\"type\":{\"node\":"
                + "\"Boolean\"},\"source\":{\"snippet\":\"waar\"}},\"condition\":{\"node\":\"Comparison\",\"op\":\"greater\","
                + "\"left\":{\"node\":\"AttributeReference\",\"attribute\":\"gewicht\",\"subjectType\":\"Vondst\",\"source\":"
                + "{\"snippet\":\"zijn gewicht\"}},\"right\":" + numberLiteral(5) + ",\"source\":{\"snippet\":\"zijn gewicht "
                + "groter is dan 5\"}},\"initialisation\":false}]}";
        String zwareLast = "{\"node\":\"Aggregation\",\"kind\":\"last\",\"arguments\":[" + vondstMember("vinddag")
                + "],\"elementType\":\"Vondst\",\"filter\":{\"node\":\"CharacteristicCheck\",\"characteristic\":\"zwaar\","
                + "\"negated\":false,\"source\":{\"snippet\":\"zwaar zijn\"}},\"sourceRole\":" + roleBinding("JutterHeeftVondst",
                "vondst") + ",\"type\":" + DATE + ",\"source\":{\"snippet\":\"de laatste van de vinddag van alle vondsten van de"
                + " Jutter die zwaar zijn\"}}";
        assertNull(play(document(false, creatie(null, memberInit("afrekendag", zwareLast)), readBack("afrekendag"), zwaar),
                caseDocument(situation(), perJutter("afrekendag", date("2026-03-05"), null, null, date("2026-03-20")))),
                "de laatste zware vinddag");
    }

    // ---------------- the write boundary ----------------

    @Test void the_assignment_is_rewritten_in_the_unit_of_the_member() throws Exception {
        // ch. 2.4: the value lands in the declared unit of the member it is written to. "… met massa gelijk aan zijn
        // eigen massa" (kg into g), J1 to J4 each 2 kg; and "… met massa gelijk aan de som van de vondstmassa van alle
        // vondsten van de Jutter", V1 4 kg, V2 7 kg, V5 9 kg. ref: 2000 g each; 11000 g, leeg, leeg and 9000 g. clingo
        // does not translate the form (a restatement at an initialisation).
        String own = "{\"node\":\"AttributeReference\",\"attribute\":\"eigen massa\",\"subjectType\":\"Jutter\",\"source\":"
                + "{\"snippet\":\"zijn eigen massa\"}}";
        List<String> withMass = new ArrayList<>();
        for (String j : situation()) withMass.add(j.replace("\"objectType\":\"Jutter\",\"attributes\":{}",
                "\"objectType\":\"Jutter\",\"attributes\":{\"eigen massa\":{\"number\":\"2\"}}"));
        assertNull(play(document(false, creatie(null, memberInit("massa", own)), readBack("massa")),
                caseDocument(withMass, perJutter("massa", number("2000"), number("2000"), number("2000"), number("2000")))),
                "een eigen attribuut in kg");
        List<String> withVondstMass = new ArrayList<>();
        for (String v : situation()) withVondstMass.add(v.replace("\"gewicht\":{\"number\":\"4\"}",
                        "\"gewicht\":{\"number\":\"4\"},\"vondstmassa\":{\"number\":\"4\"}")
                .replace("\"gewicht\":{\"number\":\"7\"}", "\"gewicht\":{\"number\":\"7\"},\"vondstmassa\":{\"number\":\"7\"}")
                .replace("\"gewicht\":{\"number\":\"9\"}", "\"gewicht\":{\"number\":\"9\"},\"vondstmassa\":{\"number\":\"9\"}"));
        String massSum = fold("sum", "vondstmassa", numberIn(KG), "de som van de vondstmassa van alle vondsten van de Jutter");
        assertNull(play(document(false, creatie(null, memberInit("massa", massSum)), readBack("massa")),
                caseDocument(withVondstMass, perJutter("massa", number("11000"), null, null, number("9000")))),
                "een som in kg");
    }

    // ---------------- what does not go along, by name ----------------

    @Test void a_division_or_square_root_in_an_assignment_refuses_by_name() throws Exception {
        // "… met heffing gelijk aan zijn basis gedeeld door zijn deler", basis 2 for each Jutter and deler 2 for all but
        // J2: ref and clingo write 1,00, leeg, 1,00 and 1,00 and turn the case inconsistent (ch. 1.2, ch. 5.6). The
        // destructive exec-model branch evaluated the toekenningen once, when it minted, and reported the Fout of that
        // evaluation itself; until 2026-10-01 it answered consistent here, and then refused the form by the site's kind.
        // The exec-model branch refuses a division or a root in a member initialisation by name: it makes no §6.5 or
        // §6.6 deposit there. Coverage lost against the destructive branch, accepted (card tms-omschakeling); each case
        // below keeps the values ref answers, for the card that carries the form. The rows pin the whole sentence, the
        // deposit the branch does not make among it; until the lens on step 5a1 of that card they pinned the form
        // alone, which a refusal with another reason passed as well.
        String own = binary("dividedBy", own("basis"), own("deler"), UNBOUNDED, "zijn basis gedeeld door zijn deler");
        List<String> withDivisor = new ArrayList<>();
        for (String j : situation()) withDivisor.add(j.replace("\"objectType\":\"Jutter\",\"attributes\":{}",
                "\"objectType\":\"Jutter\",\"attributes\":{\"basis\":{\"number\":\"2\"}"
                        + (j.contains("\"J2\"") ? "" : ",\"deler\":{\"number\":\"2\"}") + "}"));
        refused(document(false, creatie(null, memberInit("heffing", own)), readBack("heffing")), caseDocument(withDivisor,
                perJutter("heffing", number("1.00"), null, number("1.00"), number("1.00")), INCONSISTENT),
                DIVISION.formatted("zijn basis gedeeld door zijn deler"));
        // "(100 gedeeld door (de som …)) rekenkundig afgerond op 2 decimalen": ref and clingo 9,09, leeg, leeg and 11,11,
        // inconsistent; over J1 and J4 alone, whose sums are filled, consistent.
        String bySum = roundedTo2(binary("dividedBy", numberLiteral(100), SUM, UNBOUNDED, "100 gedeeld door (de som …)"));
        String sumIr = document(false, creatie(null, memberInit("heffing", bySum)), readBack("heffing"));
        refused(sumIr, caseDocument(situation(), perJutter("heffing", number("9.09"), null, null, number("11.11")), INCONSISTENT),
                DIVISION.formatted("100 gedeeld door (de som …)"));
        List<String> filled = new ArrayList<>();
        for (String i : situation()) if (!i.contains("\"J2\"") && !i.contains("\"J3\"")) filled.add(i);
        refused(sumIr, caseDocument(filled, "{\"kind\":\"attributeValue\",\"label\":\"J1\",\"attribute\":\"heffing\",\"value\":"
                + number("9.09") + "},{\"kind\":\"attributeValue\",\"label\":\"J4\",\"attribute\":\"heffing\",\"value\":"
                + number("11.11") + "}", CONSISTENT), DIVISION.formatted("100 gedeeld door (de som …)"));
        // "(de som …) gedeeld door 4": a literal divisor never deposits. ref and clingo: 2,75, 0,00, 0,00 and 2,25,
        // consistent.
        refused(document(false, creatie(null, memberInit("heffing", binary("dividedBy", SUM, numberLiteral(4),
                UNBOUNDED, "(de som …) gedeeld door 4"))), readBack("heffing")), caseDocument(situation(),
                perJutter("heffing", number("2.75"), number("0.00"), number("0.00"), number("2.25")), CONSISTENT),
                DIVISION.formatted("(de som …) gedeeld door 4"));
        // "de wortel van ((de som …) min 10) rekenkundig afgerond op 2 decimalen": J1 1, the others negative. ref: 1,00,
        // leeg, leeg and leeg, inconsistent; clingo does not translate the rounded root.
        String squareRoot = roundedTo2("{\"node\":\"BuiltinCall\",\"kind\":\"squareRoot\",\"arguments\":["
                + binary("minus", SUM, numberLiteral(10), WHOLE, "(de som …) min 10") + "],\"type\":{\"node\":\"Number\","
                + "\"whole\":false,\"decimals\":0,\"sign\":\"nonNegative\"},\"source\":{\"snippet\":\"de wortel van …\"}}");
        refused(document(false, creatie(null, memberInit("heffing", squareRoot)), readBack("heffing")), caseDocument(situation(),
                perJutter("heffing", number("1.00"), null, null, null), INCONSISTENT),
                SQUARE_ROOT.formatted("de wortel van …"));
    }

    /** The refusal of a division in a member initialisation, whose §6.5 deposit the branch does not make, with the
     *  snippet of the division to fill in. */
    private static final String DIVISION = "een deling in een ledeninitialisatie ('%s'), waarvan deze tak de"
            + " §6.5-deponering niet maakt: deze tak draagt deze vorm nog niet";
    /** The refusal of a root in a member initialisation, whose §6.6 deposit the branch does not make. */
    private static final String SQUARE_ROOT = "een wortel in een ledeninitialisatie ('%s'), waarvan deze tak de"
            + " §6.6-deponering niet maakt: deze tak draagt deze vorm nog niet";

    /** The exec-model branch refuses the creation "maak een afrekening" of {@code ir} by name, in the sentence that
     *  ends in {@code reden}, before {@code caseDocument} is replayed. */
    private static void refused(String ir, String caseDocument, String reden) throws Exception {
        Path tmp = Files.createTempFile("objectcreatie-vouw", ".json");
        tmp.toFile().deleteOnExit();
        Files.writeString(tmp, ir);
        CaseRunner.Excluded e = org.junit.jupiter.api.Assertions.assertThrows(CaseRunner.Excluded.class,
                () -> CaseRunner.playCase(tmp, JSON.readTree(caseDocument), DroolsBackend.Branch.EXEC_MODEL));
        org.junit.jupiter.api.Assertions.assertEquals("transpiler SKIP: objectcreatie: maak een afrekening — " + reden,
                e.getMessage());
    }

    @Test void five_folds() throws Exception {
        // Four ordering folds and a sum: the subject, and per fold a count and a result, are eleven variables, which
        // the consequence binds. ref and clingo: weeggrens 4, telling 7, afrekendag 05-03-2026, slotdag 12-03-2026 and
        // heffing 11,00 for J1, the values of V5 for J4, and leeg for J2 and J3.
        String ir = document(false, creatie(null, memberInit("weeggrens", MINIMUM), memberInit("telling", MAXIMUM),
                        memberInit("afrekendag", FIRST), memberInit("slotdag", LAST), memberInit("heffing", SUM)),
                readBack("weeggrens"), readBack("telling"), readBack("afrekendag"), readBack("slotdag"), readBack("heffing"));
        String caseDocument = caseDocument(situation(), perJutter("weeggrens", number("4"), null, null, number("9")),
                perJutter("telling", number("7"), null, null, number("9")),
                perJutter("afrekendag", date("2026-03-05"), null, null, date("2026-03-20")),
                perJutter("slotdag", date("2026-03-12"), null, null, date("2026-03-20")),
                perJutter("heffing", number("11.00"), null, null, number("9.00")));
        assertNull(play(ir, caseDocument));
    }

    @Test void a_date_fold_in_the_condition_of_the_creation() throws Exception {
        // "Een Jutter heeft een afrekening met telling gelijk aan 5 indien de eerste van de vinddag van alle vondsten van
        // de Jutter later is dan 01-03-2026": the fold is an accumulate on the creation's rule and the condition reads
        // it, as in an ordinary rule's condition. ref and clingo: 5, leeg, leeg and 5.
        String voorwaarde = "{\"node\":\"Comparison\",\"op\":\"later\",\"left\":" + FIRST + ",\"right\":{\"node\":\"Literal\","
                + "\"value\":{\"date\":\"2026-03-01\"},\"type\":" + DATE + ",\"source\":{\"snippet\":\"01-03-2026\"}},\"source\":"
                + "{\"snippet\":\"de eerste van … later is dan 01-03-2026\"}}";
        assertNull(play(document(false, creatie(voorwaarde, memberInit("telling", numberLiteral(5))), readBack("telling")),
                caseDocument(situation(), perJutter("telling", number("5"), null, null, number("5")))));
    }

    // ---------------- the flank ----------------

    @Test void the_number_folds_in_the_condition_of_the_creation() throws Exception {
        // "… met telling gelijk aan 5 indien de som van het gewicht van alle vondsten van de Jutter groter is dan 3", and
        // the same with the count greater than 1. An empty sum is leeg and leeg is not greater than 3 (ch. 1.2). ref and
        // clingo: 5, leeg, leeg and 5; 5, leeg, leeg and leeg.
        String sumGreater = "{\"node\":\"Comparison\",\"op\":\"greater\",\"left\":" + SUM + ",\"right\":" + numberLiteral(3)
                + ",\"source\":{\"snippet\":\"de som … groter is dan 3\"}}";
        String countGreater = "{\"node\":\"Comparison\",\"op\":\"greater\",\"left\":" + COUNTING + ",\"right\":" + numberLiteral(1)
                + ",\"source\":{\"snippet\":\"het aantal vondsten van de Jutter groter is dan 1\"}}";
        assertNull(play(document(false, creatie(sumGreater, memberInit("telling", numberLiteral(5))), readBack("telling")),
                caseDocument(situation(), perJutter("telling", number("5"), null, null, number("5")))), "de som in de voorwaarde");
        assertNull(play(document(false, creatie(countGreater, memberInit("telling", numberLiteral(5))), readBack("telling")),
                caseDocument(situation(), perJutter("telling", number("5"), null, null, null))), "het aantal in de voorwaarde");
    }

    // ---------------- documents ----------------

    private static final String WHOLE = "{\"node\":\"Number\",\"whole\":true,\"decimals\":0,\"sign\":\"unspecified\"}";
    /** A number that is not whole with 0 decimals: the unbounded form of IR-SPEC ch. 2.4 (c), as the frontend types a
     *  percentage and a division. */
    private static final String UNBOUNDED = "{\"node\":\"Number\",\"whole\":false,\"decimals\":0,\"sign\":\"unspecified\"}";
    private static final String TWO_DECIMALS = "{\"node\":\"Number\",\"whole\":false,\"decimals\":2,\"sign\":\"unspecified\"}";
    private static final String DATE = "{\"node\":\"Date\",\"precision\":\"day\"}";
    private static final String DG = "{\"node\":\"Unit\",\"abbreviation\":\"dg\",\"system\":\"Tijd\",\"canonical\":\"dg\","
            + "\"factorToCanonical\":{\"num\":1,\"den\":1}}";
    private static final String G = "{\"node\":\"Unit\",\"abbreviation\":\"g\",\"system\":\"Massa\",\"canonical\":\"g\","
            + "\"factorToCanonical\":{\"num\":1,\"den\":1}}";
    private static final String KG = "{\"node\":\"Unit\",\"abbreviation\":\"kg\",\"system\":\"Massa\",\"canonical\":\"g\","
            + "\"factorToCanonical\":{\"num\":1000,\"den\":1}}";

    /** A whole number in {@code unit}. */
    private static String numberIn(String unit) {
        return "{\"node\":\"Number\",\"whole\":true,\"decimals\":0,\"sign\":\"unspecified\",\"unit\":" + unit + "}";
    }

    private static String attribuut(String naam, String datatype) {
        return "{\"node\":\"Attribute\",\"name\":\"" + naam + "\",\"datatype\":" + datatype + "}";
    }

    private static String objecttype(String naam, String plural, boolean animate, String kenmerken, String... attributes) {
        return "{\"node\":\"ObjectType\",\"name\":\"" + naam + "\",\"animate\":" + animate + ",\"plural\":\"" + plural
                + "\",\"attributes\":[" + String.join(",", attributes) + "],\"characteristics\":[" + kenmerken + "]}";
    }

    private static String rol(String naam, String type, String plural) {
        return "{\"node\":\"Role\",\"name\":\"" + naam + "\",\"objectType\":\"" + type + "\""
                + (plural == null ? ",\"many\":false}" : ",\"plural\":\"" + plural + "\",\"many\":true}");
    }

    private static String feittype(String naam, String oneRole, String manyRole) {
        return "{\"node\":\"FactType\",\"name\":\"" + naam + "\",\"reciprocal\":false,\"roles\":[" + oneRole + "," + manyRole + "]}";
    }

    /** Jutter, Afrekening, Vondst and Strandvoogd; one jutter has many vondsten, one strandvoogd many beheerde
     *  jutters, and one afrekenaar one afrekening, or many where {@code multiple}. */
    private static String model(boolean multiple) {
        return "{\"objectTypes\":["
                + objecttype("Jutter", "Jutters", true, "", attribuut("heffing", TWO_DECIMALS), attribuut("weeggrens", WHOLE),
                        attribuut("telling", WHOLE), attribuut("afrekendag", DATE), attribuut("slotdag", DATE),
                        attribuut("spanne", numberIn(DG)), attribuut("massa", numberIn(G)), attribuut("eigen massa", numberIn(KG)),
                        attribuut("basis", WHOLE),
                        attribuut("deler", WHOLE)) + ","
                + objecttype("Afrekening", "Afrekeningen", false, "", attribuut("heffing", TWO_DECIMALS),
                        attribuut("weeggrens", WHOLE), attribuut("telling", WHOLE), attribuut("afrekendag", DATE),
                        attribuut("slotdag", DATE), attribuut("spanne", numberIn(DG)), attribuut("massa", numberIn(G))) + ","
                + objecttype("Vondst", "Vondsten", false, "{\"node\":\"Characteristic\",\"name\":\"zwaar\",\"kind\":\"nominal\"}",
                        attribuut("vinddag", DATE), attribuut("gewicht", WHOLE), attribuut("vondstmassa", numberIn(KG))) + ","
                + objecttype("Strandvoogd", "Strandvoogden", false, "", attribuut("bonus", WHOLE))
                + "],\"factTypes\":["
                + feittype("JutterHeeftVondst", rol("jutter", "Jutter", null), rol("vondst", "Vondst", "vondsten")) + ","
                + feittype("JutterHeeftAfrekening", rol("afrekenaar", "Jutter", null),
                        rol("afrekening", "Afrekening", multiple ? "afrekeningen" : null)) + ","
                + feittype("StrandvoogdBeheertJutter", rol("strandvoogd", "Strandvoogd", null),
                        rol("beheerde jutter", "Jutter", "beheerde jutters"))
                + "],\"parameters\":[],\"domains\":[],\"unitSystems\":[{\"node\":\"UnitSystem\",\"name\":\"Tijd\",\"units\":"
                + "[{\"node\":\"Unit\",\"abbreviation\":\"dg\",\"system\":\"Tijd\",\"canonical\":\"dg\",\"factorToCanonical\":"
                + "{\"num\":1,\"den\":1},\"spellings\":[\"dag\"]}]},{\"node\":\"UnitSystem\",\"name\":\"Massa\",\"units\":"
                + "[{\"node\":\"Unit\",\"abbreviation\":\"g\",\"system\":\"Massa\",\"canonical\":\"g\",\"factorToCanonical\":"
                + "{\"num\":1,\"den\":1},\"spellings\":[\"gram\"]},{\"node\":\"Unit\",\"abbreviation\":\"kg\",\"system\":\"Massa\","
                + "\"canonical\":\"g\",\"factorToCanonical\":{\"num\":1000,\"den\":1},\"spellings\":[\"kilogram\"]}]}],"
                + "\"dimensions\":[],\"timelines\":[],\"dayKinds\":[]}";
    }

    private static String roleBinding(String feittype, String rol) {
        return "{\"node\":\"RoleBinding\",\"factType\":\"" + feittype + "\",\"role\":\"" + rol + "\"}";
    }

    private static String own(String attribuut) {
        return "{\"node\":\"AttributeReference\",\"attribute\":\"" + attribuut + "\",\"subjectType\":\"Jutter\",\"source\":"
                + "{\"snippet\":\"zijn " + attribuut + "\"}}";
    }

    private static String vondstMember(String member) {
        return "{\"node\":\"AttributeReference\",\"attribute\":\"" + member + "\",\"subjectType\":\"Vondst\",\"source\":"
                + "{\"snippet\":\"de " + member + " van alle vondsten van de Jutter\"}}";
    }

    /** A fold of kind {@code soort} over the {@code lid} of the vondsten of the Jutter. */
    private static String fold(String soort, String member, String type, String snippet) {
        return "{\"node\":\"Aggregation\",\"kind\":\"" + soort + "\",\"arguments\":[" + vondstMember(member) + "],\"elementType\":"
                + "\"Vondst\",\"sourceRole\":" + roleBinding("JutterHeeftVondst", "vondst") + ",\"type\":" + type
                + ",\"source\":{\"snippet\":\"" + snippet + "\"}}";
    }

    private static final String SUM = fold("sum", "gewicht", WHOLE, "de som van het gewicht van alle vondsten van de Jutter");
    private static final String MINIMUM = fold("minimum", "gewicht", WHOLE,
            "de minimale waarde van het gewicht van alle vondsten van de Jutter");
    private static final String MAXIMUM = fold("maximum", "gewicht", WHOLE,
            "de maximale waarde van het gewicht van alle vondsten van de Jutter");
    private static final String FIRST = fold("first", "vinddag", DATE, "de eerste van de vinddag van alle vondsten van de Jutter");
    private static final String LAST = fold("last", "vinddag", DATE, "de laatste van de vinddag van alle vondsten van de Jutter");
    /** "het aantal vondsten van de Jutter", as the frontend writes it. */
    private static final String COUNTING = "{\"node\":\"Aggregation\",\"kind\":\"count\",\"arguments\":[{\"node\":"
            + "\"AttributeReference\",\"attribute\":\"vondsten\",\"subjectType\":\"Jutter\",\"source\":{\"snippet\":\"vondsten van"
            + " de Jutter\"}}],\"elementType\":\"Vondst\",\"sourceRole\":" + roleBinding("JutterHeeftVondst", "vondst")
            + ",\"type\":{\"node\":\"Number\",\"whole\":true,\"decimals\":0,\"sign\":\"nonNegative\"},\"source\":{\"snippet\":"
            + "\"het aantal vondsten van de Jutter\"}}";

    private static String numberLiteral(int n) {
        return "{\"node\":\"Literal\",\"value\":{\"number\":\"" + n + "\"},\"type\":" + WHOLE + ",\"source\":{\"snippet\":\"" + n + "\"}}";
    }

    private static String days(int n) {
        return "{\"node\":\"Literal\",\"value\":{\"number\":\"" + n + "\"},\"type\":" + numberIn(DG) + ",\"source\":{\"snippet\":\""
                + n + " dagen\"}}";
    }

    private static String binary(String op, String left, String right, String type, String snippet) {
        return "{\"node\":\"Binary\",\"op\":\"" + op + "\",\"left\":" + left + ",\"right\":" + right + ",\"type\":" + type
                + ",\"source\":{\"snippet\":\"" + snippet + "\"}}";
    }

    private static String plus(String left, String right, String type) {
        return binary("plus", left, right, type, "… plus …");
    }

    /** &lt;basis&gt; rekenkundig afgerond op 2 decimalen. */
    private static String roundedTo2(String base) {
        return "{\"node\":\"Rounding\",\"base\":" + base + ",\"mode\":\"halfAwayFromZero\",\"decimals\":2,\"type\":"
                + TWO_DECIMALS + ",\"source\":{\"snippet\":\"… rekenkundig afgerond op 2 decimalen\"}}";
    }

    /** "&lt;p&gt;% van &lt;operand&gt;". */
    private static String percent(int p, String operand) {
        return binary("percentageOf", numberLiteral(p), operand, UNBOUNDED, p + "% van …");
    }

    private static String memberInit(String member, String waarde) {
        return "{\"node\":\"MemberInit\",\"member\":\"" + member + "\",\"value\":" + waarde + ",\"characteristic\":false}";
    }

    /** "Regel maak een afrekening: Een Jutter heeft een afrekening met &lt;toekenningen&gt; [indien &lt;voorwaarde&gt;]." */
    private static String creatie(String voorwaarde, String... initialisations) {
        return "{\"node\":\"ObjectCreation\",\"name\":\"maak een afrekening\",\"versions\":[{\"createdType\":\"Afrekening\","
                + "\"subject\":\"Jutter\",\"initialisations\":[" + String.join(",", initialisations) + "],"
                + (voorwaarde == null ? "" : "\"condition\":" + voorwaarde + ",")
                + "\"validity\":{\"node\":\"Validity\"},\"connectingRole\":" + roleBinding("JutterHeeftAfrekening", "afrekening") + "}]}";
    }

    private static String regel(String naam, String doel, String waarde) {
        return "{\"node\":\"Rule\",\"name\":\"" + naam + "\",\"versions\":[{\"validity\":{\"node\":\"Validity\"},\"target\":"
                + "{\"node\":\"AttributeReference\",\"attribute\":\"" + doel + "\",\"subjectType\":\"Jutter\",\"source\":{\"snippet\":"
                + "\"De " + doel + " van een Jutter\"}},\"value\":" + waarde + ",\"initialisation\":false,\"presentation\":\"calculation\"}]}";
    }

    /** "De &lt;lid&gt; van een Jutter moet berekend worden als de &lt;lid&gt; van de afrekening van de Jutter." */
    private static String readBack(String member) {
        return regel("lees " + member + " terug", member, "{\"node\":\"AttributeReference\",\"attribute\":\"" + member + "\",\"subjectType\":"
                + "\"Afrekening\",\"source\":{\"snippet\":\"de " + member + " van de afrekening van de Jutter\"},\"path\":["
                + roleBinding("JutterHeeftAfrekening", "afrekening") + "]}");
    }

    /** "… als de som van de &lt;lid&gt; van alle afrekeningen van de Jutter." */
    private static String readBackSum(String member, String type) {
        return regel("lees " + member + " terug", member, "{\"node\":\"Aggregation\",\"kind\":\"sum\",\"arguments\":[{\"node\":"
                + "\"AttributeReference\",\"attribute\":\"" + member + "\",\"subjectType\":\"Afrekening\",\"source\":{\"snippet\":"
                + "\"de " + member + " van alle afrekeningen van de Jutter\"}}],\"elementType\":\"Afrekening\",\"sourceRole\":"
                + roleBinding("JutterHeeftAfrekening", "afrekening") + ",\"type\":" + type + ",\"source\":{\"snippet\":"
                + "\"de som van de " + member + " van alle afrekeningen van de Jutter\"}}");
    }

    /** One creation and the rules beside it. */
    private static String document(boolean multiple, String creatie, String... rules) {
        return "{\"format\":\"uchar-ir-v1\",\"model\":" + model(multiple) + ",\"rules\":{\"rules\":[" + String.join(",", rules)
                + "],\"dayKindDefinitions\":[],\"objectCreations\":[" + creatie + "],\"allocations\":[],\"consistencies\":[],"
                + "\"uniquenesses\":[],\"factCreations\":[],\"startPointDeterminations\":[],\"ruleGroups\":[]},\"diagnostics\":[]}";
    }

    // ---------------- cases ----------------

    private static String instance(String label, String type, String attributes, String roles) {
        return "{\"label\":\"" + label + "\",\"objectType\":\"" + type + "\",\"attributes\":{" + attributes + "},\"roles\":{"
                + roles + "},\"characteristics\":[]}";
    }

    private static String vondst(String label, String vinddag, String weight, String jutter) {
        List<String> a = new ArrayList<>();
        if (vinddag != null) a.add("\"vinddag\":{\"date\":\"" + vinddag + "\"}");
        if (weight != null) a.add("\"gewicht\":{\"number\":\"" + weight + "\"}");
        return instance(label, "Vondst", String.join(",", a), "\"jutter\":[\"" + jutter + "\"]");
    }

    /** The situation of the class comment. */
    private static List<String> situation() {
        return List.of(instance("J1", "Jutter", "", ""), instance("J2", "Jutter", "", ""), instance("J3", "Jutter", "", ""),
                instance("J4", "Jutter", "", ""), vondst("V1", "2026-03-12", "4", "J1"), vondst("V2", "2026-03-05", "7", "J1"),
                vondst("V3", null, null, "J1"), vondst("V4", null, null, "J3"), vondst("V5", "2026-03-20", "9", "J4"));
    }

    private static String number(String n) { return n == null ? null : "{\"number\":\"" + n + "\"}"; }

    private static String date(String iso) { return "{\"date\":\"" + iso + "\"}"; }

    private static final String INCONSISTENT = "{\"kind\":\"inconsistent\"}";
    private static final String CONSISTENT = "{\"kind\":\"consistent\"}";

    /** The expectation on {@code lid} of J1 to J4, a null value leeg. */
    private static String perJutter(String member, String... waarden) {
        List<String> out = new ArrayList<>();
        for (int i = 0; i < waarden.length; i++) {
            String label = "J" + (i + 1);
            out.add(waarden[i] == null
                    ? "{\"kind\":\"attributeEmpty\",\"label\":\"" + label + "\",\"attribute\":\"" + member + "\"}"
                    : "{\"kind\":\"attributeValue\",\"label\":\"" + label + "\",\"attribute\":\"" + member + "\",\"value\":" + waarden[i] + "}");
        }
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
        Path tmp = Files.createTempFile("objectcreatie-vouw", ".json");
        tmp.toFile().deleteOnExit();
        Files.writeString(tmp, ir);
        JsonNode c = JSON.readTree(caseDocument);
        return CaseRunner.playCase(tmp, c, drl);
    }
}
