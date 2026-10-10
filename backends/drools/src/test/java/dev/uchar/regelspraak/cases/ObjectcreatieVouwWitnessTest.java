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
import static org.junit.jupiter.api.Assertions.assertTrue;

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
class ObjectcreatieVouwWitnessTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    // ---------------- the card's reproductions ----------------

    @Test void r1_tien_procent_van_een_som() throws Exception {
        // The card's R1: "… met heffing gelijk aan 10% van de som van het gewicht van alle vondsten van de Jutter". An
        // empty sum is leeg, and 10% of leeg is 0 (ch. 1.2). ref and clingo: 1,10, 0,00, 0,00 and 0,90. The DRL-text
        // branch plays it too.
        String ir = document(false, creatie(null, toekenning("heffing", procent(10, SOM))), leesTerug("heffing"));
        String zaak = zaak(situatie(), perJutter("heffing", getal("1.10"), getal("0.00"), getal("0.00"), getal("0.90")));
        assertNull(speel(ir, zaak), "exectak");
        assertNull(speelDrl(ir, zaak), "DRL-teksttak");
    }

    @Test void r2_het_minimum_van_een_lege_bron_plus_een() throws Exception {
        // The card's R2: "… met weeggrens gelijk aan (de minimale waarde van het gewicht van alle vondsten van de
        // Jutter) plus 1". Over no vondsten, or only one without gewicht, the minimum is leeg and leeg plus 1 is 1.
        // ref and clingo: 5, 1, 1 and 10.
        String ir = document(false, creatie(null, toekenning("weeggrens", plus(MINIMUM, getalLiteraal(1), GEHEEL))),
                leesTerug("weeggrens"));
        assertNull(speel(ir, zaak(situatie(), perJutter("weeggrens", getal("5"), getal("1"), getal("1"), getal("10")))),
                "het minimum plus 1");
    }

    @Test void r3_de_eerste_vinddag() throws Exception {
        // The card's R3: "… met afrekendag gelijk aan de eerste van de vinddag van alle vondsten van de Jutter". ref and
        // clingo: 05-03-2026, leeg, leeg and 20-03-2026. The DRL-text branch markers the fold by name.
        String ir = document(false, creatie(null, toekenning("afrekendag", EERSTE)), leesTerug("afrekendag"));
        String zaak = zaak(situatie(), perJutter("afrekendag", datum("2026-03-05"), null, null, datum("2026-03-20")));
        assertNull(speel(ir, zaak), "de eerste vinddag");
        CaseRunner.Excluded drl = assertThrows(CaseRunner.Excluded.class, () -> speelDrl(ir, zaak));
        assertTrue(drl.getMessage().contains("TODO aggregatie EERSTE"), drl.getMessage());
    }

    // ---------------- the folds as an operand, and their empty source ----------------

    @Test void de_getalvouwen_als_operand_lezen_een_lege_bron_als_leeg() throws Exception {
        // 10% of a count, a minimum and a maximum; a minimum min 1 (leeg min 1 is -1); a sum verminderd met 1 (leeg
        // verminderd met 1 stays leeg); a maximum plus 1. ref and clingo per row, J1 to J4.
        String[][] rijen = {
                {"heffing", "10% van het aantal", "0.30", "0.00", "0.10", "0.10"},
                {"heffing", "10% van het minimum", "0.40", "0.00", "0.00", "0.90"},
                {"heffing", "10% van het maximum", "0.70", "0.00", "0.00", "0.90"},
                {"telling", "het minimum min 1", "3", "-1", "-1", "8"},
                {"telling", "de som verminderd met 1", "10", null, null, "8"},
                {"weeggrens", "het maximum plus 1", "8", "1", "1", "10"},
        };
        String[] waarden = {
                procent(10, TELLING), procent(10, MINIMUM), procent(10, MAXIMUM),
                binair("minus", MINIMUM, getalLiteraal(1), GEHEEL, "(de minimale waarde …) min 1"),
                binair("reducedBy", SOM, getalLiteraal(1), GEHEEL, "(de som …) verminderd met 1"),
                plus(MAXIMUM, getalLiteraal(1), GEHEEL),
        };
        for (int i = 0; i < rijen.length; i++) {
            String[] r = rijen[i];
            String ir = document(false, creatie(null, toekenning(r[0], waarden[i])), leesTerug(r[0]));
            assertNull(speel(ir, zaak(situatie(), perJutter(r[0], getal(r[2]), getal(r[3]), getal(r[4]), getal(r[5])))),
                    r[1]);
        }
    }

    @Test void een_datumvouw_plus_of_min_een_duur() throws Exception {
        // "(de eerste van …) plus 30 dagen" and "(de laatste van …) min 7 dagen": an empty date propagates (ch. 1.2).
        // ref and clingo: 04-04-2026 and 19-04-2026, and 05-03-2026 and 13-03-2026; J2 and J3 leeg.
        String plusDuur = binair("plus", EERSTE, dagen(30), DATUM, "(de eerste van …) plus 30 dagen");
        String minDuur = binair("minus", LAATSTE, dagen(7), DATUM, "(de laatste van …) min 7 dagen");
        assertNull(speel(document(false, creatie(null, toekenning("afrekendag", plusDuur)), leesTerug("afrekendag")),
                zaak(situatie(), perJutter("afrekendag", datum("2026-04-04"), null, null, datum("2026-04-19")))),
                "de eerste plus 30 dagen");
        assertNull(speel(document(false, creatie(null, toekenning("afrekendag", minDuur)), leesTerug("afrekendag")),
                zaak(situatie(), perJutter("afrekendag", datum("2026-03-05"), null, null, datum("2026-03-13")))),
                "de laatste min 7 dagen");
    }

    @Test void een_kaal_minimum_over_een_lege_bron_munt_het_object_toch() throws Exception {
        // "… met weeggrens gelijk aan de minimale waarde van het gewicht van alle vondsten van de Jutter, telling gelijk
        // aan 5": an empty minimum is leeg, and the creation mints its object all the same (ch. 5.5 asks only the
        // condition). ref and clingo: weeggrens 4, leeg, leeg and 9; telling 5 for every Jutter.
        String ir = document(false, creatie(null, toekenning("weeggrens", MINIMUM), toekenning("telling", getalLiteraal(5))),
                leesTerug("weeggrens"), leesTerug("telling"));
        assertNull(speel(ir, zaak(situatie(), perJutter("weeggrens", getal("4"), null, null, getal("9")),
                perJutter("telling", getal("5"), getal("5"), getal("5"), getal("5")))), "een kaal minimum en een constante");
    }

    @Test void twee_toekenningen_met_elk_een_vouw() throws Exception {
        // R1 and R2 in one creation, and a count beside a date fold. ref and clingo: the values of R1 and R2; telling
        // 3, 0, 1 and 1, afrekendag that of R3.
        String ir = document(false, creatie(null, toekenning("heffing", procent(10, SOM)),
                        toekenning("weeggrens", plus(MINIMUM, getalLiteraal(1), GEHEEL))),
                leesTerug("heffing"), leesTerug("weeggrens"));
        assertNull(speel(ir, zaak(situatie(), perJutter("heffing", getal("1.10"), getal("0.00"), getal("0.00"), getal("0.90")),
                perJutter("weeggrens", getal("5"), getal("1"), getal("1"), getal("10")))), "R1 en R2 samen");
        String getalEnDatum = document(false, creatie(null, toekenning("telling", TELLING), toekenning("afrekendag", EERSTE)),
                leesTerug("telling"), leesTerug("afrekendag"));
        assertNull(speel(getalEnDatum, zaak(situatie(), perJutter("telling", getal("3"), getal("0"), getal("1"), getal("1")),
                perJutter("afrekendag", datum("2026-03-05"), null, null, datum("2026-03-20")))), "een telling en een datumvouw");
    }

    @Test void de_markering_als_wacht_bij_meer_afrekeningen() throws Exception {
        // "Eén afrekenaar heeft meerdere afrekeningen": the creation keys its guard on the creation marker, and the
        // Jutter reads back "de som van de heffing van alle afrekeningen van de Jutter". ref and clingo: the values of
        // R1 and of R2.
        assertNull(speel(document(true, creatie(null, toekenning("heffing", procent(10, SOM))), leesTerugSom("heffing", TWEE_DECIMALEN)),
                zaak(situatie(), perJutter("heffing", getal("1.10"), getal("0.00"), getal("0.00"), getal("0.90")))),
                "10% van de som");
        assertNull(speel(document(true, creatie(null, toekenning("weeggrens", plus(MINIMUM, getalLiteraal(1), GEHEEL))),
                        leesTerugSom("weeggrens", GEHEEL)),
                zaak(situatie(), perJutter("weeggrens", getal("5"), getal("1"), getal("1"), getal("10")))),
                "het minimum plus 1");
    }

    @Test void een_begrenzing_een_tijdsduur_en_een_genavigeerde_lezing_rond_een_vouw() throws Exception {
        // "(het aantal vondsten van de Jutter) min 2, met een minimum van 0"; "de tijdsduur van de eerste van … tot de
        // laatste van … in hele dagen"; "de bonus van de strandvoogd van de Jutter plus (de som …)", with S1's bonus 3.
        // ref and clingo: 1, 0, 0 and 0; 7, leeg, leeg and 0 dagen; 14, 3, 3 and 12.
        String begrensd = "{\"node\":\"Clamp\",\"base\":" + binair("minus", TELLING, getalLiteraal(2), GEHEEL,
                "(het aantal vondsten van de Jutter) min 2") + ",\"minimum\":" + getalLiteraal(0) + ",\"type\":" + GEHEEL
                + ",\"source\":{\"snippet\":\"(het aantal vondsten van de Jutter) min 2, met een minimum van 0\"}}";
        assertNull(speel(document(false, creatie(null, toekenning("telling", begrensd)), leesTerug("telling")),
                zaak(situatie(), perJutter("telling", getal("1"), getal("0"), getal("0"), getal("0")))), "de begrenzing");
        String tijdsduur = "{\"node\":\"BuiltinCall\",\"kind\":\"duration\",\"arguments\":[" + EERSTE + "," + LAATSTE
                + "],\"type\":" + getalIn(DG) + ",\"source\":{\"snippet\":\"de tijdsduur van de eerste van … tot de laatste"
                + " van … in hele dagen\"}}";
        assertNull(speel(document(false, creatie(null, toekenning("spanne", tijdsduur)), leesTerug("spanne")),
                zaak(situatie(), perJutter("spanne", getal("7"), null, null, getal("0")))), "de tijdsduur");
        String bonus = "{\"node\":\"AttributeReference\",\"attribute\":\"bonus\",\"subjectType\":\"Strandvoogd\",\"source\":"
                + "{\"snippet\":\"de bonus van de strandvoogd van de Jutter\"},\"path\":[" + rolbinding("StrandvoogdBeheertJutter",
                "strandvoogd") + "]}";
        List<String> metStrandvoogd = new ArrayList<>(List.of(instantie("S1", "Strandvoogd", "\"bonus\":{\"number\":\"3\"}", "")));
        for (String j : situatie()) metStrandvoogd.add(j.replace("\"objectType\":\"Jutter\",\"attributes\":{},\"roles\":{}",
                "\"objectType\":\"Jutter\",\"attributes\":{},\"roles\":{\"strandvoogd\":[\"S1\"]}"));
        assertNull(speel(document(false, creatie(null, toekenning("telling", plus(bonus, SOM, GEHEEL))), leesTerug("telling")),
                zaak(metStrandvoogd, perJutter("telling", getal("14"), getal("3"), getal("3"), getal("12")))),
                "een genavigeerde lezing plus de som");
    }

    @Test void een_subselectie_op_een_later_afgeleid_kenmerk() throws Exception {
        // "… met afrekendag gelijk aan de laatste van de vinddag van alle vondsten van de Jutter die zwaar zijn", with
        // "Een Vondst is zwaar indien zijn gewicht groter is dan 5" declared after the creation. ref and clingo:
        // 05-03-2026 (V2 is the zware vondst of J1), leeg, leeg and 20-03-2026.
        String zwaar = "{\"node\":\"Rule\",\"name\":\"zwaar\",\"versions\":[{\"validity\":{\"node\":\"Validity\"},\"target\":"
                + "{\"node\":\"AttributeReference\",\"attribute\":\"zwaar\",\"subjectType\":\"Vondst\",\"source\":{\"snippet\":"
                + "\"Een Vondst is zwaar\"}},\"value\":{\"node\":\"Literal\",\"value\":{\"boolean\":true},\"type\":{\"node\":"
                + "\"Boolean\"},\"source\":{\"snippet\":\"waar\"}},\"condition\":{\"node\":\"Comparison\",\"op\":\"greater\","
                + "\"left\":{\"node\":\"AttributeReference\",\"attribute\":\"gewicht\",\"subjectType\":\"Vondst\",\"source\":"
                + "{\"snippet\":\"zijn gewicht\"}},\"right\":" + getalLiteraal(5) + ",\"source\":{\"snippet\":\"zijn gewicht "
                + "groter is dan 5\"}},\"initialisation\":false}]}";
        String zwareLaatste = "{\"node\":\"Aggregation\",\"kind\":\"last\",\"arguments\":[" + vondstLid("vinddag")
                + "],\"elementType\":\"Vondst\",\"filter\":{\"node\":\"CharacteristicCheck\",\"characteristic\":\"zwaar\","
                + "\"negated\":false,\"source\":{\"snippet\":\"zwaar zijn\"}},\"sourceRole\":" + rolbinding("JutterHeeftVondst",
                "vondst") + ",\"type\":" + DATUM + ",\"source\":{\"snippet\":\"de laatste van de vinddag van alle vondsten van de"
                + " Jutter die zwaar zijn\"}}";
        assertNull(speel(document(false, creatie(null, toekenning("afrekendag", zwareLaatste)), leesTerug("afrekendag"), zwaar),
                zaak(situatie(), perJutter("afrekendag", datum("2026-03-05"), null, null, datum("2026-03-20")))),
                "de laatste zware vinddag");
    }

    // ---------------- the write boundary ----------------

    @Test void de_toekenning_wordt_herschreven_in_de_eenheid_van_het_lid() throws Exception {
        // ch. 2.4: the value lands in the declared unit of the member it is written to. "… met massa gelijk aan zijn
        // eigen massa" (kg into g), J1 to J4 each 2 kg; and "… met massa gelijk aan de som van de vondstmassa van alle
        // vondsten van de Jutter", V1 4 kg, V2 7 kg, V5 9 kg. ref: 2000 g each; 11000 g, leeg, leeg and 9000 g. clingo
        // does not translate the form (a restatement at an initialisation).
        String eigen = "{\"node\":\"AttributeReference\",\"attribute\":\"eigen massa\",\"subjectType\":\"Jutter\",\"source\":"
                + "{\"snippet\":\"zijn eigen massa\"}}";
        List<String> metMassa = new ArrayList<>();
        for (String j : situatie()) metMassa.add(j.replace("\"objectType\":\"Jutter\",\"attributes\":{}",
                "\"objectType\":\"Jutter\",\"attributes\":{\"eigen massa\":{\"number\":\"2\"}}"));
        assertNull(speel(document(false, creatie(null, toekenning("massa", eigen)), leesTerug("massa")),
                zaak(metMassa, perJutter("massa", getal("2000"), getal("2000"), getal("2000"), getal("2000")))),
                "een eigen attribuut in kg");
        List<String> metVondstmassa = new ArrayList<>();
        for (String v : situatie()) metVondstmassa.add(v.replace("\"gewicht\":{\"number\":\"4\"}",
                        "\"gewicht\":{\"number\":\"4\"},\"vondstmassa\":{\"number\":\"4\"}")
                .replace("\"gewicht\":{\"number\":\"7\"}", "\"gewicht\":{\"number\":\"7\"},\"vondstmassa\":{\"number\":\"7\"}")
                .replace("\"gewicht\":{\"number\":\"9\"}", "\"gewicht\":{\"number\":\"9\"},\"vondstmassa\":{\"number\":\"9\"}"));
        String massasom = vouw("sum", "vondstmassa", getalIn(KG), "de som van de vondstmassa van alle vondsten van de Jutter");
        assertNull(speel(document(false, creatie(null, toekenning("massa", massasom)), leesTerug("massa")),
                zaak(metVondstmassa, perJutter("massa", getal("11000"), null, null, getal("9000")))),
                "een som in kg");
    }

    // ---------------- what does not go along, by name ----------------

    @Test void een_deling_of_wortel_in_een_toekenning_weigert_met_naam() throws Exception {
        // "… met heffing gelijk aan zijn basis gedeeld door zijn deler", basis 2 for each Jutter and deler 2 for all but
        // J2: ref and clingo write 1,00, leeg, 1,00 and 1,00 and turn the case inconsistent (ch. 1.2, ch. 5.6). The
        // destructive exec-model branch evaluated the toekenningen once, when it minted, and reported the Fout of that
        // evaluation itself; until 2026-10-01 it answered consistent here, and then refused the form by the site's kind.
        // The truth-maintenance branch refuses a division or a root in a member initialisation by name: it makes no
        // §6.5 or §6.6 deposit there. Coverage lost against the destructive branch, accepted (card tms-omschakeling);
        // each case below keeps the values ref answers, for the card that carries the form.
        String eigen = binair("dividedBy", eigen("basis"), eigen("deler"), ONBEGRENSD, "zijn basis gedeeld door zijn deler");
        List<String> metDeler = new ArrayList<>();
        for (String j : situatie()) metDeler.add(j.replace("\"objectType\":\"Jutter\",\"attributes\":{}",
                "\"objectType\":\"Jutter\",\"attributes\":{\"basis\":{\"number\":\"2\"}"
                        + (j.contains("\"J2\"") ? "" : ",\"deler\":{\"number\":\"2\"}") + "}"));
        geweigerd(document(false, creatie(null, toekenning("heffing", eigen)), leesTerug("heffing")), zaak(metDeler,
                perJutter("heffing", getal("1.00"), null, getal("1.00"), getal("1.00")), INCONSISTENT),
                "een deling in een ledeninitialisatie ('zijn basis gedeeld door zijn deler')");
        // "(100 gedeeld door (de som …)) rekenkundig afgerond op 2 decimalen": ref and clingo 9,09, leeg, leeg and 11,11,
        // inconsistent; over J1 and J4 alone, whose sums are filled, consistent.
        String doorSom = afgerondOp2(binair("dividedBy", getalLiteraal(100), SOM, ONBEGRENSD, "100 gedeeld door (de som …)"));
        String somIr = document(false, creatie(null, toekenning("heffing", doorSom)), leesTerug("heffing"));
        geweigerd(somIr, zaak(situatie(), perJutter("heffing", getal("9.09"), null, null, getal("11.11")), INCONSISTENT),
                "een deling in een ledeninitialisatie ('100 gedeeld door (de som …)')");
        List<String> gevuld = new ArrayList<>();
        for (String i : situatie()) if (!i.contains("\"J2\"") && !i.contains("\"J3\"")) gevuld.add(i);
        geweigerd(somIr, zaak(gevuld, "{\"kind\":\"attributeValue\",\"label\":\"J1\",\"attribute\":\"heffing\",\"value\":"
                + getal("9.09") + "},{\"kind\":\"attributeValue\",\"label\":\"J4\",\"attribute\":\"heffing\",\"value\":"
                + getal("11.11") + "}", CONSISTENT), "een deling in een ledeninitialisatie ('100 gedeeld door (de som …)')");
        // "(de som …) gedeeld door 4": a literal divisor never deposits. ref and clingo: 2,75, 0,00, 0,00 and 2,25,
        // consistent.
        geweigerd(document(false, creatie(null, toekenning("heffing", binair("dividedBy", SOM, getalLiteraal(4),
                ONBEGRENSD, "(de som …) gedeeld door 4"))), leesTerug("heffing")), zaak(situatie(),
                perJutter("heffing", getal("2.75"), getal("0.00"), getal("0.00"), getal("2.25")), CONSISTENT),
                "een deling in een ledeninitialisatie ('(de som …) gedeeld door 4')");
        // "de wortel van ((de som …) min 10) rekenkundig afgerond op 2 decimalen": J1 1, the others negative. ref: 1,00,
        // leeg, leeg and leeg, inconsistent; clingo does not translate the rounded root.
        String wortel = afgerondOp2("{\"node\":\"BuiltinCall\",\"kind\":\"squareRoot\",\"arguments\":["
                + binair("minus", SOM, getalLiteraal(10), GEHEEL, "(de som …) min 10") + "],\"type\":{\"node\":\"Number\","
                + "\"whole\":false,\"decimals\":0,\"sign\":\"nonNegative\"},\"source\":{\"snippet\":\"de wortel van …\"}}");
        geweigerd(document(false, creatie(null, toekenning("heffing", wortel)), leesTerug("heffing")), zaak(situatie(),
                perJutter("heffing", getal("1.00"), null, null, null), INCONSISTENT),
                "een wortel in een ledeninitialisatie ('de wortel van …')");
    }

    /** The truth-maintenance branch refuses the creation "maak een afrekening" of {@code ir} by name, on the form
     *  {@code vorm}, before {@code zaak} is replayed. */
    private static void geweigerd(String ir, String zaak, String vorm) throws Exception {
        Path tmp = Files.createTempFile("objectcreatie-vouw", ".json");
        tmp.toFile().deleteOnExit();
        Files.writeString(tmp, ir);
        CaseRunner.Excluded e = org.junit.jupiter.api.Assertions.assertThrows(CaseRunner.Excluded.class,
                () -> CaseRunner.speelCase(tmp, JSON.readTree(zaak), DroolsBackend.Branch.EXEC_MODEL));
        org.junit.jupiter.api.Assertions.assertTrue(e.getMessage().contains("objectcreatie: maak een afrekening — " + vorm),
                e.getMessage());
    }

    @Test void vijf_vouwen() throws Exception {
        // Four ordering folds and a sum: the subject, and per fold a count and a result, are eleven variables, which
        // the consequence binds. ref and clingo: weeggrens 4, telling 7, afrekendag 05-03-2026, slotdag 12-03-2026 and
        // heffing 11,00 for J1, the values of V5 for J4, and leeg for J2 and J3.
        String ir = document(false, creatie(null, toekenning("weeggrens", MINIMUM), toekenning("telling", MAXIMUM),
                        toekenning("afrekendag", EERSTE), toekenning("slotdag", LAATSTE), toekenning("heffing", SOM)),
                leesTerug("weeggrens"), leesTerug("telling"), leesTerug("afrekendag"), leesTerug("slotdag"), leesTerug("heffing"));
        String zaak = zaak(situatie(), perJutter("weeggrens", getal("4"), null, null, getal("9")),
                perJutter("telling", getal("7"), null, null, getal("9")),
                perJutter("afrekendag", datum("2026-03-05"), null, null, datum("2026-03-20")),
                perJutter("slotdag", datum("2026-03-12"), null, null, datum("2026-03-20")),
                perJutter("heffing", getal("11.00"), null, null, getal("9.00")));
        assertNull(speel(ir, zaak));
    }

    @Test void een_datumvouw_in_de_voorwaarde_van_de_creatie() throws Exception {
        // "Een Jutter heeft een afrekening met telling gelijk aan 5 indien de eerste van de vinddag van alle vondsten van
        // de Jutter later is dan 01-03-2026": the fold is an accumulate on the creation's rule and the condition reads
        // it, as in an ordinary rule's condition. ref and clingo: 5, leeg, leeg and 5.
        String voorwaarde = "{\"node\":\"Comparison\",\"op\":\"later\",\"left\":" + EERSTE + ",\"right\":{\"node\":\"Literal\","
                + "\"value\":{\"date\":\"2026-03-01\"},\"type\":" + DATUM + ",\"source\":{\"snippet\":\"01-03-2026\"}},\"source\":"
                + "{\"snippet\":\"de eerste van … later is dan 01-03-2026\"}}";
        assertNull(speel(document(false, creatie(voorwaarde, toekenning("telling", getalLiteraal(5))), leesTerug("telling")),
                zaak(situatie(), perJutter("telling", getal("5"), null, null, getal("5")))));
    }

    // ---------------- the flank ----------------

    @Test void de_getalvouwen_in_de_voorwaarde_van_de_creatie() throws Exception {
        // "… met telling gelijk aan 5 indien de som van het gewicht van alle vondsten van de Jutter groter is dan 3", and
        // the same with the count greater than 1. An empty sum is leeg and leeg is not greater than 3 (ch. 1.2). ref and
        // clingo: 5, leeg, leeg and 5; 5, leeg, leeg and leeg.
        String somGroter = "{\"node\":\"Comparison\",\"op\":\"greater\",\"left\":" + SOM + ",\"right\":" + getalLiteraal(3)
                + ",\"source\":{\"snippet\":\"de som … groter is dan 3\"}}";
        String aantalGroter = "{\"node\":\"Comparison\",\"op\":\"greater\",\"left\":" + TELLING + ",\"right\":" + getalLiteraal(1)
                + ",\"source\":{\"snippet\":\"het aantal vondsten van de Jutter groter is dan 1\"}}";
        assertNull(speel(document(false, creatie(somGroter, toekenning("telling", getalLiteraal(5))), leesTerug("telling")),
                zaak(situatie(), perJutter("telling", getal("5"), null, null, getal("5")))), "de som in de voorwaarde");
        assertNull(speel(document(false, creatie(aantalGroter, toekenning("telling", getalLiteraal(5))), leesTerug("telling")),
                zaak(situatie(), perJutter("telling", getal("5"), null, null, null))), "het aantal in de voorwaarde");
    }

    // ---------------- documents ----------------

    private static final String GEHEEL = "{\"node\":\"Number\",\"whole\":true,\"decimals\":0,\"sign\":\"unspecified\"}";
    /** A number that is not whole with 0 decimals: the unbounded form of IR-SPEC ch. 2.4 (c), as the frontend types a
     *  percentage and a division. */
    private static final String ONBEGRENSD = "{\"node\":\"Number\",\"whole\":false,\"decimals\":0,\"sign\":\"unspecified\"}";
    private static final String TWEE_DECIMALEN = "{\"node\":\"Number\",\"whole\":false,\"decimals\":2,\"sign\":\"unspecified\"}";
    private static final String DATUM = "{\"node\":\"Date\",\"precision\":\"day\"}";
    private static final String DG = "{\"node\":\"Unit\",\"abbreviation\":\"dg\",\"system\":\"Tijd\",\"canonical\":\"dg\","
            + "\"factorToCanonical\":{\"num\":1,\"den\":1}}";
    private static final String G = "{\"node\":\"Unit\",\"abbreviation\":\"g\",\"system\":\"Massa\",\"canonical\":\"g\","
            + "\"factorToCanonical\":{\"num\":1,\"den\":1}}";
    private static final String KG = "{\"node\":\"Unit\",\"abbreviation\":\"kg\",\"system\":\"Massa\",\"canonical\":\"g\","
            + "\"factorToCanonical\":{\"num\":1000,\"den\":1}}";

    /** A whole number in {@code eenheid}. */
    private static String getalIn(String eenheid) {
        return "{\"node\":\"Number\",\"whole\":true,\"decimals\":0,\"sign\":\"unspecified\",\"unit\":" + eenheid + "}";
    }

    private static String attribuut(String naam, String datatype) {
        return "{\"node\":\"Attribute\",\"name\":\"" + naam + "\",\"datatype\":" + datatype + "}";
    }

    private static String objecttype(String naam, String meervoud, boolean bezield, String kenmerken, String... attributen) {
        return "{\"node\":\"ObjectType\",\"name\":\"" + naam + "\",\"animate\":" + bezield + ",\"plural\":\"" + meervoud
                + "\",\"attributes\":[" + String.join(",", attributen) + "],\"characteristics\":[" + kenmerken + "]}";
    }

    private static String rol(String naam, String type, String meervoud) {
        return "{\"node\":\"Role\",\"name\":\"" + naam + "\",\"objectType\":\"" + type + "\""
                + (meervoud == null ? ",\"many\":false}" : ",\"plural\":\"" + meervoud + "\",\"many\":true}");
    }

    private static String feittype(String naam, String eenRol, String veelRol) {
        return "{\"node\":\"FactType\",\"name\":\"" + naam + "\",\"reciprocal\":false,\"roles\":[" + eenRol + "," + veelRol + "]}";
    }

    /** Jutter, Afrekening, Vondst and Strandvoogd; one jutter has many vondsten, one strandvoogd many beheerde
     *  jutters, and one afrekenaar one afrekening, or many where {@code meerdere}. */
    private static String model(boolean meerdere) {
        return "{\"objectTypes\":["
                + objecttype("Jutter", "Jutters", true, "", attribuut("heffing", TWEE_DECIMALEN), attribuut("weeggrens", GEHEEL),
                        attribuut("telling", GEHEEL), attribuut("afrekendag", DATUM), attribuut("slotdag", DATUM),
                        attribuut("spanne", getalIn(DG)), attribuut("massa", getalIn(G)), attribuut("eigen massa", getalIn(KG)),
                        attribuut("basis", GEHEEL),
                        attribuut("deler", GEHEEL)) + ","
                + objecttype("Afrekening", "Afrekeningen", false, "", attribuut("heffing", TWEE_DECIMALEN),
                        attribuut("weeggrens", GEHEEL), attribuut("telling", GEHEEL), attribuut("afrekendag", DATUM),
                        attribuut("slotdag", DATUM), attribuut("spanne", getalIn(DG)), attribuut("massa", getalIn(G))) + ","
                + objecttype("Vondst", "Vondsten", false, "{\"node\":\"Characteristic\",\"name\":\"zwaar\",\"kind\":\"nominal\"}",
                        attribuut("vinddag", DATUM), attribuut("gewicht", GEHEEL), attribuut("vondstmassa", getalIn(KG))) + ","
                + objecttype("Strandvoogd", "Strandvoogden", false, "", attribuut("bonus", GEHEEL))
                + "],\"factTypes\":["
                + feittype("JutterHeeftVondst", rol("jutter", "Jutter", null), rol("vondst", "Vondst", "vondsten")) + ","
                + feittype("JutterHeeftAfrekening", rol("afrekenaar", "Jutter", null),
                        rol("afrekening", "Afrekening", meerdere ? "afrekeningen" : null)) + ","
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

    private static String rolbinding(String feittype, String rol) {
        return "{\"node\":\"RoleBinding\",\"factType\":\"" + feittype + "\",\"role\":\"" + rol + "\"}";
    }

    private static String eigen(String attribuut) {
        return "{\"node\":\"AttributeReference\",\"attribute\":\"" + attribuut + "\",\"subjectType\":\"Jutter\",\"source\":"
                + "{\"snippet\":\"zijn " + attribuut + "\"}}";
    }

    private static String vondstLid(String lid) {
        return "{\"node\":\"AttributeReference\",\"attribute\":\"" + lid + "\",\"subjectType\":\"Vondst\",\"source\":"
                + "{\"snippet\":\"de " + lid + " van alle vondsten van de Jutter\"}}";
    }

    /** A fold of kind {@code soort} over the {@code lid} of the vondsten of the Jutter. */
    private static String vouw(String soort, String lid, String type, String snippet) {
        return "{\"node\":\"Aggregation\",\"kind\":\"" + soort + "\",\"arguments\":[" + vondstLid(lid) + "],\"elementType\":"
                + "\"Vondst\",\"sourceRole\":" + rolbinding("JutterHeeftVondst", "vondst") + ",\"type\":" + type
                + ",\"source\":{\"snippet\":\"" + snippet + "\"}}";
    }

    private static final String SOM = vouw("sum", "gewicht", GEHEEL, "de som van het gewicht van alle vondsten van de Jutter");
    private static final String MINIMUM = vouw("minimum", "gewicht", GEHEEL,
            "de minimale waarde van het gewicht van alle vondsten van de Jutter");
    private static final String MAXIMUM = vouw("maximum", "gewicht", GEHEEL,
            "de maximale waarde van het gewicht van alle vondsten van de Jutter");
    private static final String EERSTE = vouw("first", "vinddag", DATUM, "de eerste van de vinddag van alle vondsten van de Jutter");
    private static final String LAATSTE = vouw("last", "vinddag", DATUM, "de laatste van de vinddag van alle vondsten van de Jutter");
    /** "het aantal vondsten van de Jutter", as the frontend writes it. */
    private static final String TELLING = "{\"node\":\"Aggregation\",\"kind\":\"count\",\"arguments\":[{\"node\":"
            + "\"AttributeReference\",\"attribute\":\"vondsten\",\"subjectType\":\"Jutter\",\"source\":{\"snippet\":\"vondsten van"
            + " de Jutter\"}}],\"elementType\":\"Vondst\",\"sourceRole\":" + rolbinding("JutterHeeftVondst", "vondst")
            + ",\"type\":{\"node\":\"Number\",\"whole\":true,\"decimals\":0,\"sign\":\"nonNegative\"},\"source\":{\"snippet\":"
            + "\"het aantal vondsten van de Jutter\"}}";

    private static String getalLiteraal(int n) {
        return "{\"node\":\"Literal\",\"value\":{\"number\":\"" + n + "\"},\"type\":" + GEHEEL + ",\"source\":{\"snippet\":\"" + n + "\"}}";
    }

    private static String dagen(int n) {
        return "{\"node\":\"Literal\",\"value\":{\"number\":\"" + n + "\"},\"type\":" + getalIn(DG) + ",\"source\":{\"snippet\":\""
                + n + " dagen\"}}";
    }

    private static String binair(String op, String links, String rechts, String type, String snippet) {
        return "{\"node\":\"Binary\",\"op\":\"" + op + "\",\"left\":" + links + ",\"right\":" + rechts + ",\"type\":" + type
                + ",\"source\":{\"snippet\":\"" + snippet + "\"}}";
    }

    private static String plus(String links, String rechts, String type) {
        return binair("plus", links, rechts, type, "… plus …");
    }

    /** &lt;basis&gt; rekenkundig afgerond op 2 decimalen. */
    private static String afgerondOp2(String basis) {
        return "{\"node\":\"Rounding\",\"base\":" + basis + ",\"mode\":\"halfAwayFromZero\",\"decimals\":2,\"type\":"
                + TWEE_DECIMALEN + ",\"source\":{\"snippet\":\"… rekenkundig afgerond op 2 decimalen\"}}";
    }

    /** "&lt;p&gt;% van &lt;operand&gt;". */
    private static String procent(int p, String operand) {
        return binair("percentageOf", getalLiteraal(p), operand, ONBEGRENSD, p + "% van …");
    }

    private static String toekenning(String lid, String waarde) {
        return "{\"node\":\"MemberInit\",\"member\":\"" + lid + "\",\"value\":" + waarde + ",\"characteristic\":false}";
    }

    /** "Regel maak een afrekening: Een Jutter heeft een afrekening met &lt;toekenningen&gt; [indien &lt;voorwaarde&gt;]." */
    private static String creatie(String voorwaarde, String... toekenningen) {
        return "{\"node\":\"ObjectCreation\",\"name\":\"maak een afrekening\",\"versions\":[{\"createdType\":\"Afrekening\","
                + "\"subject\":\"Jutter\",\"initialisations\":[" + String.join(",", toekenningen) + "],"
                + (voorwaarde == null ? "" : "\"condition\":" + voorwaarde + ",")
                + "\"validity\":{\"node\":\"Validity\"},\"connectingRole\":" + rolbinding("JutterHeeftAfrekening", "afrekening") + "}]}";
    }

    private static String regel(String naam, String doel, String waarde) {
        return "{\"node\":\"Rule\",\"name\":\"" + naam + "\",\"versions\":[{\"validity\":{\"node\":\"Validity\"},\"target\":"
                + "{\"node\":\"AttributeReference\",\"attribute\":\"" + doel + "\",\"subjectType\":\"Jutter\",\"source\":{\"snippet\":"
                + "\"De " + doel + " van een Jutter\"}},\"value\":" + waarde + ",\"initialisation\":false,\"presentation\":\"calculation\"}]}";
    }

    /** "De &lt;lid&gt; van een Jutter moet berekend worden als de &lt;lid&gt; van de afrekening van de Jutter." */
    private static String leesTerug(String lid) {
        return regel("lees " + lid + " terug", lid, "{\"node\":\"AttributeReference\",\"attribute\":\"" + lid + "\",\"subjectType\":"
                + "\"Afrekening\",\"source\":{\"snippet\":\"de " + lid + " van de afrekening van de Jutter\"},\"path\":["
                + rolbinding("JutterHeeftAfrekening", "afrekening") + "]}");
    }

    /** "… als de som van de &lt;lid&gt; van alle afrekeningen van de Jutter." */
    private static String leesTerugSom(String lid, String type) {
        return regel("lees " + lid + " terug", lid, "{\"node\":\"Aggregation\",\"kind\":\"sum\",\"arguments\":[{\"node\":"
                + "\"AttributeReference\",\"attribute\":\"" + lid + "\",\"subjectType\":\"Afrekening\",\"source\":{\"snippet\":"
                + "\"de " + lid + " van alle afrekeningen van de Jutter\"}}],\"elementType\":\"Afrekening\",\"sourceRole\":"
                + rolbinding("JutterHeeftAfrekening", "afrekening") + ",\"type\":" + type + ",\"source\":{\"snippet\":"
                + "\"de som van de " + lid + " van alle afrekeningen van de Jutter\"}}");
    }

    /** One creation and the rules beside it. */
    private static String document(boolean meerdere, String creatie, String... regels) {
        return "{\"format\":\"uchar-ir-v1\",\"model\":" + model(meerdere) + ",\"rules\":{\"rules\":[" + String.join(",", regels)
                + "],\"dayKindDefinitions\":[],\"objectCreations\":[" + creatie + "],\"allocations\":[],\"consistencies\":[],"
                + "\"uniquenesses\":[],\"factCreations\":[],\"startPointDeterminations\":[],\"ruleGroups\":[]},\"diagnostics\":[]}";
    }

    // ---------------- cases ----------------

    private static String instantie(String label, String type, String attributen, String rollen) {
        return "{\"label\":\"" + label + "\",\"objectType\":\"" + type + "\",\"attributes\":{" + attributen + "},\"roles\":{"
                + rollen + "},\"characteristics\":[]}";
    }

    private static String vondst(String label, String vinddag, String gewicht, String jutter) {
        List<String> a = new ArrayList<>();
        if (vinddag != null) a.add("\"vinddag\":{\"date\":\"" + vinddag + "\"}");
        if (gewicht != null) a.add("\"gewicht\":{\"number\":\"" + gewicht + "\"}");
        return instantie(label, "Vondst", String.join(",", a), "\"jutter\":[\"" + jutter + "\"]");
    }

    /** The situation of the class comment. */
    private static List<String> situatie() {
        return List.of(instantie("J1", "Jutter", "", ""), instantie("J2", "Jutter", "", ""), instantie("J3", "Jutter", "", ""),
                instantie("J4", "Jutter", "", ""), vondst("V1", "2026-03-12", "4", "J1"), vondst("V2", "2026-03-05", "7", "J1"),
                vondst("V3", null, null, "J1"), vondst("V4", null, null, "J3"), vondst("V5", "2026-03-20", "9", "J4"));
    }

    private static String getal(String n) { return n == null ? null : "{\"number\":\"" + n + "\"}"; }

    private static String datum(String iso) { return "{\"date\":\"" + iso + "\"}"; }

    private static final String INCONSISTENT = "{\"kind\":\"inconsistent\"}";
    private static final String CONSISTENT = "{\"kind\":\"consistent\"}";

    /** The expectation on {@code lid} of J1 to J4, a null value leeg. */
    private static String perJutter(String lid, String... waarden) {
        List<String> uit = new ArrayList<>();
        for (int i = 0; i < waarden.length; i++) {
            String label = "J" + (i + 1);
            uit.add(waarden[i] == null
                    ? "{\"kind\":\"attributeEmpty\",\"label\":\"" + label + "\",\"attribute\":\"" + lid + "\"}"
                    : "{\"kind\":\"attributeValue\",\"label\":\"" + label + "\",\"attribute\":\"" + lid + "\",\"value\":" + waarden[i] + "}");
        }
        return String.join(",", uit);
    }

    private static String zaak(List<String> instanties, String... verwacht) {
        return "{\"format\":\"uchar-case-v0\",\"asOfDate\":null,\"input\":{\"instances\":[" + String.join(",", instanties)
                + "],\"parameters\":{}},\"expected\":[" + String.join(",", verwacht) + "]}";
    }

    /** The exec-model branch's verdict on one case: null when every expectation holds. */
    private static String speel(String ir, String zaak) throws Exception {
        return speel(ir, zaak, false);
    }

    private static String speelDrl(String ir, String zaak) throws Exception {
        return speel(ir, zaak, true);
    }

    private static String speel(String ir, String zaak, boolean drl) throws Exception {
        Path tmp = Files.createTempFile("objectcreatie-vouw", ".json");
        tmp.toFile().deleteOnExit();
        Files.writeString(tmp, ir);
        JsonNode c = JSON.readTree(zaak);
        return CaseRunner.speelCase(tmp, c, drl);
    }
}
