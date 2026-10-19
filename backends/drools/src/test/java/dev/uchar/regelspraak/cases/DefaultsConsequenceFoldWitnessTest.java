package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The consequence of a default that folds over a role: the latest date over the vondsten of a Berging ("de laatste
 * van de bergdag van alle vondsten van de Berging"), a count, a sum, a minimum or a maximum, the fold inside a
 * computation, and two folds as the operands of one ordering fold, in the base of an {@code Afwijking van} tree, in an
 * exception, in the group the frontend's opgave marker generates (grammar/DIVERGENCES.md §7) and in the group on
 * {@code berekende X} it moves a tree to. RUNTIME-SEMANTICS ch. 7.1: a default decides with its consequence where its
 * justification holds and the consequence is not leeg; ch. 1.2e gives the empty fold. The same fold in an ordinary
 * rule's value plays since the date fold over a role became an accumulate beside the subject pattern. {@code ref/}
 * answers every row here, and the ASP backend every row but the maximum of two number folds, which it does not
 * translate; every document is authored in this class on Jutterswet material, and each is the document of one measured
 * form of the card's form space, with the values {@code ref} and clingo answer.
 *
 * <p><b>Red before the fix</b> (measured 2026-09-30 on the transpiler of the claim, exec-model branch): every row that
 * plays was refused for the whole group in one lumped sentence that names neither the form nor the consequence
 * ({@code niet-reduceerbare consequentie (far-side/aggregation): <default>}; {@code <default>} because a default carries
 * no source text on the wire); the two refusal rows found that same sentence where they look for the form. The row over
 * two number folds came after the fix and was red on the transpiler of the claim in the same sentence. The row over
 * five folds pinned a refusal of the group until a consequence bound more than nine variables (2026-10-01); red against
 * that transpiler in {@code te veel gebonden variabelen (aggregaties, vouwen en globals)}, it plays since. *
 * <p><b>Both branches of the analysis</b> (card drl-teksttak-op-truth-maintenance, step 4): every row runs on the
 * exec-model branch and on {@code DRL_TEXT}, its rendering as DRL text, with the same verdict, a refusal in the same
 * words. <b>Red</b> on {@code e2bd685e}, before the step: every
 * row that plays failed on {@code DRL_TMS} with "drl-tms: de DRL-weergave draagt Afloop nog niet"; the refusal row
 * held there already, since the refusal comes from the analysis, which the two branches share.
 */
class DefaultsConsequenceFoldWitnessTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    // ---------------- the card's reproductions ----------------

    @Test void r1_the_base_of_a_deviation_folds() throws Exception {
        // The card's R1: "De sluitdag van een Berging moet berekend worden als de laatste van de bergdag van alle
        // vondsten van de Berging", "Afwijking van sluitdag: gemelde sluitdag gaat voor … gesteld worden op zijn gemelde
        // sluitdag indien zijn gemelde sluitdag gevuld is". ref and clingo: 09-03-2026, and 12-03-2026 with the gemelde.
        String ir = document(false, List.of(), closingDayGroup("sluitdag", LAST_BERGDAG));
        assertNull(play(ir, caseDocument(List.of(berging("B", null, null, null), vondst("V1", "B", "2026-03-03", null, null),
                vondst("V2", "B", "2026-03-09", null, null)), date("B", "sluitdag", "2026-03-09"))), "zonder gemelde sluitdag");
        assertNull(play(ir, caseDocument(List.of(berging("B", "2026-03-12", null, null), vondst("V1", "B", "2026-03-03", null, null)),
                date("B", "sluitdag", "2026-03-12"))), "met gemelde sluitdag");
        // until card drl-teksttak-op-truth-maintenance the DRL-text branch refused the document ("… no defaults
        // lowering"); since then speel holds it to the verdict of the exec-model branch on both cases
    }

    @Test void r3_the_input_above_a_deviation_whose_base_folds() throws Exception {
        // The card's R3: "de sluitdag Datum in dagen (mag opgegeven worden)" above R1. The marker moves the tree to
        // 'berekende sluitdag' and generates the group 'opgave boven berekening sluitdag' on the sluitdag, so the fold
        // is the base of the group on 'berekende sluitdag'. ref and clingo: B1 09-03-2026, B2 the gemelde 12-03-2026,
        // B3 leeg, B4 20-03-2026, B5 (a vondst without bergdag) leeg, B6 the opgegeven 15-03-2026.
        String ir = document(true, List.of(DEVIATING_INPUT),
                group("sluitdag", "Berging", "berekende sluitdag", reportedPrecedes("sluitdag"),
                        defaultEntry(ALTIJD, LAST_BERGDAG)),
                inputGroup(null));
        assertNull(play(ir, caseDocument(situation(true), closingDays("2026-03-09", "2026-03-12", null, "2026-03-20", null,
                "2026-03-15"))), "één vouw");
    }

    @Test void r3_with_two_folds_as_operands() throws Exception {
        // As R3, the base "de laatste van (de laatste van de bergdag van alle vondsten van de Berging) en (de laatste
        // van de meldingsdag van alle vondsten van de Berging)". ref and clingo: B1 12-03-2026 (V3 meldt het laatst).
        String two = "{\"node\":\"Aggregation\",\"kind\":\"last\",\"arguments\":[" + LAST_BERGDAG + ","
                + fold("last", "meldingsdag", DATE, "de laatste van de meldingsdag van alle vondsten van de Berging")
                + "],\"type\":" + DATE + ",\"source\":{\"snippet\":\"de laatste van (…) en (…)\"}}";
        String ir = document(true, List.of(DEVIATING_INPUT),
                group("sluitdag", "Berging", "berekende sluitdag", reportedPrecedes("sluitdag"), defaultEntry(ALTIJD, two)),
                inputGroup(null));
        assertNull(play(ir, caseDocument(situation(true), closingDays("2026-03-12", "2026-03-12", null, "2026-03-20", null,
                "2026-03-15"))), "twee vouwen");
    }

    @Test void the_generated_group_initialises_with_a_fold() throws Exception {
        // "de sluitdag … (mag opgegeven worden)", "De sluitdag van een Berging moet gesteld worden op zijn gemelde
        // sluitdag indien zijn gemelde sluitdag gevuld is" and "De sluitdag van een Berging moet geïnitialiseerd worden
        // op de laatste van de bergdag van alle vondsten van de Berging": the marker lifts the initialisation into its
        // group as the lowest default, so the fold is the consequence of the generated group itself. ref and clingo:
        // the values of R3.
        String computed = regel("sluitdag", "Berging", "berekende sluitdag", own("gemelde sluitdag", "Berging"),
                filled(own("gemelde sluitdag", "Berging")), "equation");
        String ir = document(true, List.of(computed, DEVIATING_INPUT), inputGroup(LAST_BERGDAG));
        assertNull(play(ir, caseDocument(situation(true), closingDays("2026-03-09", "2026-03-12", null, "2026-03-20", null,
                "2026-03-15"))), "de initialisatie in de gegenereerde groep");
    }

    // ---------------- the folds, in an exception and in a computation ----------------

    @Test void an_exception_folds_under_a_justification_that_counts() throws Exception {
        // "De sluitdag van een Berging moet gesteld worden op zijn gemelde sluitdag", "Uitzondering op sluitdag: … moet
        // berekend worden als de eerste van de bergdag van alle vondsten van de Berging indien het aantal vondsten van
        // de Berging groter is dan 1": the justification's count and the consequence's fold both become an accumulate
        // on the group's rule. ref and clingo: B1 (three vondsten) 03-03-2026, B2 (one) its gemelde 12-03-2026.
        String count = "{\"node\":\"Comparison\",\"op\":\"greater\",\"left\":" + COUNTING + ",\"right\":" + number(1)
                + ",\"source\":{\"snippet\":\"het aantal vondsten van de Berging groter is dan 1\"}}";
        String ir = document(false, List.of(), group("sluitdag", "Berging", "sluitdag",
                defaultEntry(count, fold("first", "bergdag", DATE, "de eerste van de bergdag van alle vondsten van de Berging")),
                defaultEntry(ALTIJD, own("gemelde sluitdag", "Berging"))));
        assertNull(play(ir, caseDocument(situation(false), closingDays("2026-03-03", "2026-03-12", null, "2026-03-20", null))),
                "de telling in de rechtvaardiging, de vouw in de consequentie");
    }

    @Test void the_number_folds_in_the_consequence() throws Exception {
        // "Het maatgewicht van een Berging moet berekend worden als <vouw>", "Afwijking van maatgewicht: … gesteld
        // worden op zijn gemelde gewicht indien zijn gemelde gewicht gevuld is". Gewichten: B1 4, 7 and 2; B5 one
        // vondst without gewicht. ref and clingo per row, B1 to B5; an empty sum, minimum or maximum is leeg and then
        // decides nothing (ch. 7.1), an empty count is 0.
        String[][] rows = {
                {"sum", "de som van het gewicht", "13", "", ""},
                {"minimum", "de minimale waarde van het gewicht", "2", "", ""},
                {"maximum", "de maximale waarde van het gewicht", "7", "", ""},
                {"count", "het aantal", "3", "0", "1"},
        };
        for (String[] r : rows) {
            String v = r[0].equals("count") ? COUNTING
                    : fold(r[0], "gewicht", WHOLE, r[1] + " van alle vondsten van de Berging");
            String ir = document(false, List.of(), group("maatgewicht", "Berging", "maatgewicht",
                    defaultEntry(filled(own("gemeld gewicht", "Berging")), own("gemeld gewicht", "Berging")),
                    defaultEntry(ALTIJD, v)));
            assertNull(play(ir, caseDocument(situation(false), gewichten(r[2], "30", r[3], "25", r[4]))), r[1]);
        }
    }

    @Test void a_fold_in_a_calculation() throws Exception {
        // "(de laatste van de bergdag van alle vondsten van de Berging) plus 14 dagen" and "(de som van het gewicht van
        // alle vondsten van de Berging) plus 1" as the base under the afwijking. ref and clingo: B1 23-03-2026 and 14;
        // over no vondsten the date stays leeg, and the sum is leeg, which "plus 1" makes 1 (ch. 1.2).
        String plusDuration = "{\"node\":\"Binary\",\"op\":\"plus\",\"left\":" + LAST_BERGDAG + ",\"right\":{\"node\":"
                + "\"Literal\",\"value\":{\"number\":\"14\"},\"type\":" + DAGEN + ",\"source\":{\"snippet\":\"14 dagen\"}},"
                + "\"type\":" + DATE + ",\"source\":{\"snippet\":\"(de laatste van …) plus 14 dagen\"}}";
        String dateIr = document(false, List.of(), closingDayGroup("sluitdag", plusDuration));
        assertNull(play(dateIr, caseDocument(situation(false), closingDays("2026-03-23", "2026-03-12", null, "2026-03-20", null))),
                "de vouw plus een duur");
        String plusOne = "{\"node\":\"Binary\",\"op\":\"plus\",\"left\":" + fold("sum", "gewicht", WHOLE,
                "de som van het gewicht van alle vondsten van de Berging") + ",\"right\":" + number(1) + ",\"type\":" + WHOLE
                + ",\"source\":{\"snippet\":\"(de som van …) plus 1\"}}";
        String numberIr = document(false, List.of(), group("maatgewicht", "Berging", "maatgewicht",
                defaultEntry(filled(own("gemeld gewicht", "Berging")), own("gemeld gewicht", "Berging")),
                defaultEntry(ALTIJD, plusOne)));
        assertNull(play(numberIr, caseDocument(situation(false), gewichten("14", "30", "1", "25", "1"))), "de som plus 1");
    }

    @Test void two_number_folds_as_operands() throws Exception {
        // "Het maatgewicht van een Berging moet berekend worden als de maximale waarde van (de som van het gewicht van
        // alle vondsten van de Berging) en (het aantal vondsten van de Berging)" under the afwijking of the gemeld
        // gewicht. ref: B1 13, B3 0 (the empty sum is skipped), B5 1. The ASP backend does not translate a listed fold
        // whose operands are number folds, in an ordinary rule neither, so here ref alone answers.
        String two = "{\"node\":\"Aggregation\",\"kind\":\"maximum\",\"arguments\":[" + fold("sum", "gewicht", WHOLE,
                "de som van het gewicht van alle vondsten van de Berging") + "," + COUNTING + "],\"type\":" + WHOLE
                + ",\"source\":{\"snippet\":\"de maximale waarde van (…) en (…)\"}}";
        String ir = document(false, List.of(), group("maatgewicht", "Berging", "maatgewicht",
                defaultEntry(filled(own("gemeld gewicht", "Berging")), own("gemeld gewicht", "Berging")),
                defaultEntry(ALTIJD, two)));
        assertNull(play(ir, caseDocument(situation(false), gewichten("13", "30", "0", "25", "1"))),
                "het maximum van een som en een telling");
    }

    // ---------------- two roles away ----------------

    @Test void the_fold_two_roles_far() throws Exception {
        // "De sluitdag van een Kustwacht moet berekend worden als de laatste van de bergdag van alle vondsten van alle
        // bergingen van de Kustwacht", under "Afwijking van sluitdag: … zijn gemelde sluitdag". K1 beheert B1 and B2,
        // K2 B3 and B5 and has a gemelde sluitdag, K3 niets. ref and clingo: K1 09-03-2026, K2 18-03-2026, K3 leeg.
        String path = "{\"node\":\"Aggregation\",\"kind\":\"last\",\"arguments\":[{\"node\":\"AttributeReference\","
                + "\"attribute\":\"bergdag\",\"subjectType\":\"Vondst\",\"source\":{\"snippet\":\"de bergdag van alle vondsten"
                + " van alle bergingen van de Kustwacht\"}}],\"elementType\":\"Vondst\",\"sourcePath\":["
                + roleBinding("KustwachtBeheertBerging", "berging") + "," + roleBinding("BergingBetreftVondst", "vondst")
                + "],\"type\":" + DATE + ",\"source\":{\"snippet\":\"de laatste van de bergdag van alle vondsten van alle"
                + " bergingen van de Kustwacht\"}}";
        String ir = document(false, List.of(), group("sluitdag", "Kustwacht", "sluitdag",
                defaultEntry(filled(own("gemelde sluitdag", "Kustwacht")), own("gemelde sluitdag", "Kustwacht")),
                defaultEntry(ALTIJD, path)));
        assertNull(play(ir, caseDocument(situation(false), String.join(",", date("K1", "sluitdag", "2026-03-09"),
                date("K2", "sluitdag", "2026-03-18"), empty("K3", "sluitdag")))), "twee rollen ver");
    }

    @Test void a_justification_two_roles_far_above_a_fold() throws Exception {
        // "Uitzondering op sluitdag: … moet berekend worden als de laatste van de bergdag van alle vondsten van de
        // Berging indien de sluitingsdatum van de strandvoogd van de kustwacht van de Berging gevuld is", above "gesteld
        // worden op zijn gemelde sluitdag". S1 (a sluitingsdatum) oversees K1, which beheert B1 and B2. ref and clingo:
        // B1 09-03-2026, B2 01-03-2026 (its vondst, not its gemelde), B4 its gemelde 20-03-2026.
        String chain = "{\"node\":\"AttributeReference\",\"attribute\":\"sluitingsdatum\",\"subjectType\":\"Strandvoogd\","
                + "\"source\":{\"snippet\":\"de sluitingsdatum van de strandvoogd van de kustwacht van de Berging\"},\"path\":["
                + roleBinding("KustwachtBeheertBerging", "kustwacht") + "," + roleBinding("StrandvoogdBeheertKustwacht",
                "strandvoogd") + "]}";
        String ir = document(false, List.of(), group("sluitdag", "Berging", "sluitdag",
                defaultEntry(filled(chain), LAST_BERGDAG), defaultEntry(ALTIJD, own("gemelde sluitdag", "Berging"))));
        assertNull(play(ir, caseDocument(situation(false), closingDays("2026-03-09", "2026-03-01", null, "2026-03-20", null))),
                "de keten in de rechtvaardiging, de vouw in de consequentie");
    }

    // ---------------- the moment the group decides ----------------

    @Test void the_fold_waits_on_the_group_that_writes_its_member() throws Exception {
        // "De sluitdag van een Berging moet berekend worden als de laatste van de uiterste bergdag van alle vondsten van
        // de Berging" under the afwijking, and after it a defaults group on the Vondst: "De uiterste bergdag van een
        // Vondst moet gesteld worden op zijn bergdag", "Afwijking van uiterste bergdag: … berekend worden als zijn
        // meldingsdag plus 2 dagen indien zijn meldingsdag gevuld is". ref and clingo: B1 14-03-2026 (V3 meldt 12-03).
        String ir = document(false, List.of(), closingDayGroup("sluitdag",
                        fold("last", "uiterste bergdag", DATE, "de laatste van de uiterste bergdag van alle vondsten van de Berging")),
                latestBergdagGroup(filled(own("meldingsdag", "Vondst"))));
        assertNull(play(ir, caseDocument(situation(false), closingDays("2026-03-14", "2026-03-12", null, "2026-03-20", null))),
                "de uiterste bergdag uit een latere groep");
    }

    @Test void the_subselection_reads_a_later_derived_date() throws Exception {
        // The subselection of the fold reads the uiterste bergdag, which later rules or a later group write. First an
        // initialisation to the bergdag and a gelijkstelling to the meldingsdag where filled, under "die aan de
        // volgende voorwaarde voldoen: zijn uiterste bergdag is later dan 05-03-2026": before the gelijkstelling V1
        // (bergdag 09-03) passes, after it V2 and V3 do, and V3 has no bergdag. ref and clingo: B1 03-03-2026.
        String later = "{\"node\":\"Compound\",\"quantifier\":\"the\",\"threshold\":0,\"parts\":[{\"node\":\"Comparison\","
                + "\"op\":\"later\",\"left\":" + own("uiterste bergdag", "Vondst") + ",\"right\":{\"node\":\"Literal\","
                + "\"value\":{\"date\":\"2026-03-05\"},\"type\":" + DATE + ",\"source\":{\"snippet\":\"05-03-2026\"}},"
                + "\"source\":{\"snippet\":\"zijn uiterste bergdag is later dan 05-03-2026\"}}],\"source\":{\"snippet\":"
                + "\"aan de volgende voorwaarde voldoen: zijn uiterste bergdag is later dan 05-03-2026\"}}";
        String init = "{\"node\":\"Rule\",\"name\":\"registratie\",\"versions\":[{\"validity\":{\"node\":\"Validity\"},"
                + "\"target\":" + target("uiterste bergdag", "Vondst") + ",\"value\":" + own("bergdag", "Vondst")
                + ",\"initialisation\":true}]}";
        String input = regel("opgave", "Vondst", "uiterste bergdag", own("meldingsdag", "Vondst"),
                filled(own("meldingsdag", "Vondst")), "equation");
        String ir = document(false, List.of(init, input), closingDayGroup("sluitdag",
                filtered("last", "bergdag", later, "de laatste van de bergdag van alle vondsten van de Berging die …")));
        assertNull(play(ir, caseDocument(situation(false), closingDays("2026-03-03", "2026-03-12", null, "2026-03-20", null))),
                "een initialisatie, dan een gelijkstelling");
        // Then a group writes it, and the subselection tests it on leeg: "de eerste van de meldingsdag … die aan de
        // volgende voorwaarde voldoen: zijn uiterste bergdag is leeg", the uiterste bergdag the bergdag or, above a
        // gewicht of 3, the meldingsdag plus 2 dagen. Before the group every vondst passes (V1 meldt 05-03), after it
        // V3 alone. ref and clingo: B1 12-03-2026.
        String isEmpty = "{\"node\":\"Compound\",\"quantifier\":\"the\",\"threshold\":0,\"parts\":[{\"node\":\"Predicate\","
                + "\"kind\":\"empty\",\"operand\":" + own("uiterste bergdag", "Vondst") + ",\"negated\":false,\"digits\":0,"
                + "\"source\":{\"snippet\":\"zijn uiterste bergdag is leeg\"}}],\"source\":{\"snippet\":\"aan de volgende"
                + " voorwaarde voldoen: zijn uiterste bergdag is leeg\"}}";
        String zwaar = "{\"node\":\"Comparison\",\"op\":\"greater\",\"left\":" + own("gewicht", "Vondst") + ",\"right\":"
                + number(3) + ",\"source\":{\"snippet\":\"zijn gewicht groter is dan 3\"}}";
        String emptyIr = document(false, List.of(), closingDayGroup("sluitdag",
                        filtered("first", "meldingsdag", isEmpty, "de eerste van de meldingsdag van alle vondsten van de Berging die …")),
                latestBergdagGroup(zwaar));
        assertNull(play(emptyIr, caseDocument(situation(false), closingDays("2026-03-12", "2026-03-12", null, "2026-03-20", null))),
                "een groep, getoetst op leeg");
    }

    // ---------------- what does not go ----------------

    @Test void a_fold_without_a_bean_field_is_refused_with_the_form() throws Exception {
        // "de laatste van de meldingsdatum van alle meldingen van de Berging" over "meerdere kennende bergingen kennen
        // meerdere meldingen": both roles plural, so the bean model stores the relation on neither end (§13.3.9), in a
        // rule's value as in a consequence. ref and clingo answer 08-03-2026; the exec-model branch refuses the
        // group and names it and the fold.
        String message = objectType("Melding", "Meldingen", List.of(attribute("meldingsdatum", DATE)), List.of());
        String knows = "{\"node\":\"FactType\",\"name\":\"BergingKentMelding\",\"reciprocal\":false,\"roles\":["
                + "{\"node\":\"Role\",\"name\":\"kennende berging\",\"objectType\":\"Berging\",\"plural\":\"kennende bergingen\","
                + "\"many\":true},{\"node\":\"Role\",\"name\":\"melding\",\"objectType\":\"Melding\",\"plural\":\"meldingen\","
                + "\"many\":true}]}";
        String v = "{\"node\":\"Aggregation\",\"kind\":\"last\",\"arguments\":[" + own("meldingsdatum", "Melding")
                + "],\"elementType\":\"Melding\",\"sourceRole\":" + roleBinding("BergingKentMelding", "melding") + ",\"type\":"
                + DATE + ",\"source\":{\"snippet\":\"de laatste van de meldingsdatum van alle meldingen van de Berging\"}}";
        String ir = "{\"format\":\"uchar-ir-v1\",\"model\":{\"objectTypes\":[" + berging(false) + "," + message
                + "],\"factTypes\":[" + knows + "],\"parameters\":[],\"domains\":[],\"unitSystems\":[],\"dimensions\":[],"
                + "\"timelines\":[],\"dayKinds\":[]}," + rules(List.of(), List.of(closingDayGroup("sluitdag", v))) + "}";
        CaseRunner.Excluded e = assertThrows(CaseRunner.Excluded.class, () -> playTms(ir, caseDocument(List.of(), "")));
        assertTrue(e.getMessage().contains("defaults: defaults A:Berging.sluitdag — de aggregatie-join van 'de laatste van"
                + " de meldingsdatum van alle meldingen van de Berging' heeft geen beanveld"), e.getMessage());
        assertTrue(e.getMessage().contains("beide zijden meervoudig"), e.getMessage());
    }

    @Test void five_folds_in_a_consequence() throws Exception {
        // "de laatste van (…), (…), (…), (…) en (…)" over five folds: each binds its count and its result, so the
        // consequence binds eleven variables, the subject among them. ref and clingo: B1 12-03-2026 (the last
        // meldingsdag; nothing writes the uiterste bergdag here), B2 and B4 their gemelde sluitdag, B3 and B5 leeg.
        List<String> five = new ArrayList<>();
        for (String[] l : new String[][] {{"last", "bergdag"}, {"first", "bergdag"}, {"last", "meldingsdag"},
                {"first", "meldingsdag"}, {"last", "uiterste bergdag"}})
            five.add(fold(l[0], l[1], DATE, "de " + (l[0].equals("last") ? "laatste" : "eerste") + " van de " + l[1]
                    + " van alle vondsten van de Berging"));
        String v = "{\"node\":\"Aggregation\",\"kind\":\"last\",\"arguments\":[" + String.join(",", five) + "],\"type\":"
                + DATE + ",\"source\":{\"snippet\":\"de laatste van (…), (…), (…), (…) en (…)\"}}";
        String ir = document(false, List.of(), closingDayGroup("sluitdag", v));
        assertNull(play(ir, caseDocument(situation(false), closingDays("2026-03-12", "2026-03-12", null, "2026-03-20", null))));
    }

    // ---------------- documents ----------------

    private static final String DATE = "{\"node\":\"Date\",\"precision\":\"day\"}";
    private static final String WHOLE = "{\"node\":\"Number\",\"whole\":true,\"decimals\":0,\"sign\":\"unspecified\"}";
    private static final String NIET_NEGATIEF = "{\"node\":\"Number\",\"whole\":true,\"decimals\":0,\"sign\":\"nonNegative\"}";
    private static final String DAGEN = "{\"node\":\"Number\",\"whole\":true,\"decimals\":0,\"sign\":\"unspecified\",\"unit\":"
            + "{\"node\":\"Unit\",\"abbreviation\":\"dag\",\"canonical\":\"dag\",\"factorToCanonical\":{\"num\":1,\"den\":1}}}";
    private static final String ALTIJD = "{\"node\":\"Compound\",\"quantifier\":\"all\",\"threshold\":0,\"parts\":[],"
            + "\"source\":{\"snippet\":\"altijd\"}}";

    private static String attribute(String naam, String datatype) {
        return "{\"node\":\"Attribute\",\"name\":\"" + naam + "\",\"datatype\":" + datatype + "}";
    }

    private static String objectType(String naam, String plural, List<String> attributes, List<String> kenmerken) {
        return "{\"node\":\"ObjectType\",\"name\":\"" + naam + "\",\"animate\":false,\"plural\":\"" + plural
                + "\",\"attributes\":[" + String.join(",", attributes) + "],\"characteristics\":[" + String.join(",", kenmerken) + "]}";
    }

    /** The Berging; with the opgave marker on its sluitdag, the members the frontend generates for it (§7). */
    private static String berging(boolean marking) {
        List<String> a = new ArrayList<>(List.of(attribute("gemelde sluitdag", DATE), attribute("sluitdag", DATE)));
        if (marking) a.addAll(List.of(attribute("opgegeven sluitdag", DATE), attribute("berekende sluitdag", DATE)));
        a.addAll(List.of(attribute("gemeld gewicht", WHOLE), attribute("maatgewicht", WHOLE)));
        return objectType("Berging", "Bergingen", a, marking
                ? List.of("{\"node\":\"Characteristic\",\"name\":\"afwijkende opgave sluitdag\",\"kind\":\"possessive\"}")
                : List.of());
    }

    private static final String VONDST = objectType("Vondst", "Vondsten", List.of(attribute("bergdag", DATE),
            attribute("meldingsdag", DATE), attribute("gewicht", WHOLE), attribute("uiterste bergdag", DATE)), List.of());
    private static final String KUSTWACHT = objectType("Kustwacht", "Kustwachten", List.of(attribute("gemelde sluitdag", DATE),
            attribute("sluitdag", DATE)), List.of());
    private static final String STRANDVOOGD = objectType("Strandvoogd", "Strandvoogden", List.of(attribute("sluitingsdatum", DATE)),
            List.of());

    private static String factType(String naam, String oneRole, String oneType, String manyRole, String manyPlural, String manyType) {
        return "{\"node\":\"FactType\",\"name\":\"" + naam + "\",\"reciprocal\":false,\"roles\":["
                + "{\"node\":\"Role\",\"name\":\"" + oneRole + "\",\"objectType\":\"" + oneType + "\",\"many\":false},"
                + "{\"node\":\"Role\",\"name\":\"" + manyRole + "\",\"objectType\":\"" + manyType + "\",\"plural\":\"" + manyPlural
                + "\",\"many\":true}]}";
    }

    private static String roleBinding(String factType, String role) {
        return "{\"node\":\"RoleBinding\",\"factType\":\"" + factType + "\",\"role\":\"" + role + "\"}";
    }

    private static String own(String attribute, String type) {
        return "{\"node\":\"AttributeReference\",\"attribute\":\"" + attribute + "\",\"subjectType\":\"" + type
                + "\",\"source\":{\"snippet\":\"zijn " + attribute + "\"}}";
    }

    private static String target(String attribute, String type) {
        return "{\"node\":\"AttributeReference\",\"attribute\":\"" + attribute + "\",\"subjectType\":\"" + type
                + "\",\"source\":{\"snippet\":\"de " + attribute + " van een " + type + "\"}}";
    }

    private static String number(int n) {
        return "{\"node\":\"Literal\",\"value\":{\"number\":\"" + n + "\"},\"type\":" + WHOLE + ",\"source\":{\"snippet\":\"" + n + "\"}}";
    }

    private static String filled(String operand) {
        return "{\"node\":\"Predicate\",\"kind\":\"filled\",\"operand\":" + operand + ",\"negated\":false,\"digits\":0,"
                + "\"source\":{\"snippet\":\"gevuld is\"}}";
    }

    /** A fold over the vondsten of the Berging: "de &lt;kind&gt; van de &lt;lid&gt; van alle vondsten van de Berging". */
    private static String fold(String kind, String member, String type, String snippet) {
        return filtered(kind, member, null, type, snippet);
    }

    private static String filtered(String kind, String member, String filter, String snippet) {
        return filtered(kind, member, filter, DATE, snippet);
    }

    private static String filtered(String kind, String member, String filter, String type, String snippet) {
        return "{\"node\":\"Aggregation\",\"kind\":\"" + kind + "\",\"arguments\":[{\"node\":\"AttributeReference\","
                + "\"attribute\":\"" + member + "\",\"subjectType\":\"Vondst\",\"source\":{\"snippet\":\"de " + member
                + " van alle vondsten van de Berging\"}}],\"elementType\":\"Vondst\""
                + (filter == null ? "" : ",\"filter\":" + filter) + ",\"sourceRole\":" + roleBinding("BergingBetreftVondst", "vondst")
                + ",\"type\":" + type + ",\"source\":{\"snippet\":\"" + snippet + "\"}}";
    }

    private static final String LAST_BERGDAG = fold("last", "bergdag", DATE,
            "de laatste van de bergdag van alle vondsten van de Berging");

    /** "het aantal vondsten van de Berging". */
    private static final String COUNTING = "{\"node\":\"Aggregation\",\"kind\":\"count\",\"arguments\":[{\"node\":"
            + "\"AttributeReference\",\"attribute\":\"vondsten\",\"subjectType\":\"Berging\",\"source\":{\"snippet\":\"vondsten"
            + " van de Berging\"}}],\"elementType\":\"Vondst\",\"sourceRole\":" + roleBinding("BergingBetreftVondst", "vondst")
            + ",\"type\":" + NIET_NEGATIEF + ",\"source\":{\"snippet\":\"het aantal vondsten van de Berging\"}}";

    private static String defaultEntry(String justification, String consequence) {
        return "{\"node\":\"Default\",\"justification\":" + justification + ",\"consequence\":" + consequence + "}";
    }

    private static String group(String naam, String type, String attribute, String... defaults) {
        return "{\"node\":\"PrioritizedDefaults\",\"name\":\"" + naam + "\",\"subject\":\"" + type + "\",\"attribute\":\""
                + attribute + "\",\"defaults\":[" + String.join(",", defaults) + "],\"validity\":{\"node\":\"Validity\"}}";
    }

    /** The afwijking of R1: "gesteld worden op zijn gemelde sluitdag indien zijn gemelde sluitdag gevuld is". */
    private static String reportedPrecedes(String attribute) {
        return defaultEntry(filled(own("gemelde " + attribute, "Berging")), own("gemelde " + attribute, "Berging"));
    }

    /** The sluitdag of a Berging: the gemelde sluitdag where filled, above {@code base}. */
    private static String closingDayGroup(String naam, String base) {
        return group(naam, "Berging", "sluitdag", reportedPrecedes("sluitdag"), defaultEntry(ALTIJD, base));
    }

    /** The uiterste bergdag of a Vondst: its meldingsdag plus 2 dagen where {@code justification} holds, else its
     *  bergdag. */
    private static String latestBergdagGroup(String justification) {
        String plus = "{\"node\":\"Binary\",\"op\":\"plus\",\"left\":" + own("meldingsdag", "Vondst") + ",\"right\":{\"node\":"
                + "\"Literal\",\"value\":{\"number\":\"2\"},\"type\":" + DAGEN + ",\"source\":{\"snippet\":\"2 dagen\"}},"
                + "\"type\":" + DATE + ",\"source\":{\"snippet\":\"zijn meldingsdag plus 2 dagen\"}}";
        return group("uiterste bergdag", "Vondst", "uiterste bergdag", defaultEntry(justification, plus),
                defaultEntry(ALTIJD, own("bergdag", "Vondst")));
    }

    /** The group the opgave marker generates on the sluitdag (§7): the opgave, the computation, and where the author
     *  wrote an initialisation, that initialisation as the lowest default. */
    private static String inputGroup(String initialisatie) {
        String opgegeven = "{\"node\":\"Default\",\"justification\":" + ALTIJD + ",\"consequence\":"
                + own("opgegeven sluitdag", "Berging") + ",\"source\":{\"snippet\":\"opgegeven sluitdag\"}}";
        String computed = "{\"node\":\"Default\",\"justification\":" + ALTIJD + ",\"consequence\":"
                + own("berekende sluitdag", "Berging") + ",\"source\":{\"snippet\":\"berekende sluitdag\"}}";
        String init = initialisatie == null ? "" : ",{\"node\":\"Default\",\"consequence\":" + initialisatie
                + ",\"initialisation\":true}";
        return "{\"node\":\"PrioritizedDefaults\",\"name\":\"opgave boven berekening sluitdag\",\"subject\":\"Berging\","
                + "\"attribute\":\"sluitdag\",\"defaults\":[" + opgegeven + "," + computed + init + "],\"validity\":{\"node\":\"Validity\"}}";
    }

    /** The rule the opgave marker generates: "Een Berging heeft afwijkende opgave sluitdag" where the opgave and the
     *  computation are both filled and differ. */
    private static final String DEVIATING_INPUT = "{\"node\":\"Rule\",\"name\":\"afwijking opgave sluitdag\",\"versions\":[{"
            + "\"validity\":{\"node\":\"Validity\"},\"target\":{\"node\":\"AttributeReference\",\"attribute\":\"afwijkende opgave"
            + " sluitdag\",\"subjectType\":\"Berging\",\"source\":{\"snippet\":\"Een Berging heeft afwijkende opgave sluitdag\"}},"
            + "\"value\":{\"node\":\"Literal\",\"value\":{\"boolean\":true},\"type\":{\"node\":\"Boolean\"},\"source\":{\"snippet\":"
            + "\"waar\"}},\"condition\":{\"node\":\"Compound\",\"quantifier\":\"all\",\"threshold\":0,\"parts\":["
            + filled(own("opgegeven sluitdag", "Berging")) + "," + filled(own("berekende sluitdag", "Berging")) + ","
            + "{\"node\":\"Comparison\",\"op\":\"notEqual\",\"left\":" + own("opgegeven sluitdag", "Berging") + ",\"right\":"
            + own("berekende sluitdag", "Berging") + ",\"source\":{\"snippet\":\"zijn opgegeven sluitdag is ongelijk aan zijn"
            + " berekende sluitdag\"}}],\"source\":{\"snippet\":\"hij aan alle volgende voorwaarden voldoet\"}},"
            + "\"initialisation\":false}]}";

    /** The consistency the opgave marker generates: no afwijkende opgave. */
    private static final String CHECK_INPUT = "{\"node\":\"Consistency\",\"name\":\"controle opgave sluitdag\",\"versions\":[{"
            + "\"subject\":\"Berging\",\"assertion\":{\"node\":\"CharacteristicCheck\",\"characteristic\":\"afwijkende opgave"
            + " sluitdag\",\"negated\":true,\"source\":{\"snippet\":\"hij heeft geen afwijkende opgave sluitdag\"}},\"validity\":"
            + "{\"node\":\"Validity\"},\"source\":{\"snippet\":\"de sluitdag Datum in dagen (mag opgegeven worden)\"}}]}";

    /** An ordinary rule with one version; {@code condition} null for none. */
    private static String regel(String naam, String type, String target, String value, String condition, String presentation) {
        return "{\"node\":\"Rule\",\"name\":\"" + naam + "\",\"versions\":[{\"validity\":{\"node\":\"Validity\"},\"target\":"
                + target(target, type) + ",\"value\":" + value + (condition == null ? "" : ",\"condition\":" + condition)
                + ",\"initialisation\":false,\"presentation\":\"" + presentation + "\"}]}";
    }

    private static String rules(List<String> rules, List<String> groups) {
        return "\"rules\":{\"rules\":[" + String.join(",", rules) + "],\"dayKindDefinitions\":[],\"objectCreations\":[],"
                + "\"allocations\":[],\"consistencies\":[" + (groups.stream().anyMatch(g -> g.contains("opgave boven berekening"))
                ? CHECK_INPUT : "") + "],\"uniquenesses\":[],\"factCreations\":[],\"startPointDeterminations\":[],"
                + "\"ruleGroups\":[],\"prioritizedDefaults\":[" + String.join(",", groups) + "]},\"diagnostics\":[]";
    }

    /** Berging, Vondst, Kustwacht and Strandvoogd with their three relations, and the rules and groups. */
    private static String document(boolean marking, List<String> rules, String... groups) {
        String model = "{\"objectTypes\":[" + STRANDVOOGD + "," + KUSTWACHT + "," + berging(marking) + "," + VONDST
                + "],\"factTypes\":[" + factType("BergingBetreftVondst", "berging", "Berging", "vondst", "vondsten", "Vondst") + ","
                + factType("KustwachtBeheertBerging", "kustwacht", "Kustwacht", "berging", "bergingen", "Berging") + ","
                + factType("StrandvoogdBeheertKustwacht", "strandvoogd", "Strandvoogd", "kustwacht", "kustwachten", "Kustwacht")
                + "],\"parameters\":[],\"domains\":[],\"unitSystems\":[],\"dimensions\":[],\"timelines\":[],\"dayKinds\":[]}";
        return "{\"format\":\"uchar-ir-v1\",\"model\":" + model + "," + rules(rules, List.of(groups)) + "}";
    }

    // ---------------- cases ----------------

    private static String instance(String label, String type, String attributes, String roles) {
        return "{\"label\":\"" + label + "\",\"objectType\":\"" + type + "\",\"attributes\":{" + attributes + "},\"roles\":{"
                + roles + "},\"characteristics\":[]}";
    }

    private static String attrs(String... paren) {
        List<String> out = new ArrayList<>();
        for (int i = 0; i < paren.length; i += 2) if (paren[i + 1] != null) out.add("\"" + paren[i] + "\":" + paren[i + 1]);
        return String.join(",", out);
    }

    private static String day(String iso) { return iso == null ? null : "{\"date\":\"" + iso + "\"}"; }

    private static String berging(String label, String gemeldeSluitdag, String reportedWeight, String kustwacht) {
        return instance(label, "Berging", attrs("gemelde sluitdag", day(gemeldeSluitdag),
                "gemeld gewicht", reportedWeight == null ? null : "{\"number\":\"" + reportedWeight + "\"}"),
                kustwacht == null ? "" : "\"kustwacht\":[\"" + kustwacht + "\"]");
    }

    private static String vondst(String label, String berging, String bergdag, String meldingsdag, String weight) {
        return instance(label, "Vondst", attrs("bergdag", day(bergdag), "meldingsdag", day(meldingsdag),
                "gewicht", weight == null ? null : "{\"number\":\"" + weight + "\"}"), "\"berging\":[\"" + berging + "\"]");
    }

    /** Strandvoogd S1 (sluitingsdatum 25-03-2026) oversees K1, S2 K2; K2 has a gemelde sluitdag. B1 (K1): V1 bergdag
     *  09-03, meldingsdag 05-03, gewicht 4; V2 03-03, 06-03, 7; V3 no bergdag, 12-03, 2. B2 (K1, gemeld 12-03-2026 and
     *  30): V4 01-03, 02-03, 5. B3 (K2): no vondsten. B4 (gemeld 20-03-2026 and 25): no vondsten. B5 (K2): V5, empty.
     *  With the marker, B6 carries an opgegeven sluitdag 15-03-2026. The member order of B1 is not its value order. */
    private static List<String> situation(boolean marking) {
        List<String> out = new ArrayList<>(List.of(
                instance("S1", "Strandvoogd", attrs("sluitingsdatum", day("2026-03-25")), ""),
                instance("S2", "Strandvoogd", "", ""),
                instance("K1", "Kustwacht", "", "\"strandvoogd\":[\"S1\"]"),
                instance("K2", "Kustwacht", attrs("gemelde sluitdag", day("2026-03-18")), "\"strandvoogd\":[\"S2\"]"),
                instance("K3", "Kustwacht", "", ""),
                berging("B1", null, null, "K1"), berging("B2", "2026-03-12", "30", "K1"), berging("B3", null, null, "K2"),
                berging("B4", "2026-03-20", "25", null), berging("B5", null, null, "K2"),
                vondst("V1", "B1", "2026-03-09", "2026-03-05", "4"), vondst("V2", "B1", "2026-03-03", "2026-03-06", "7"),
                vondst("V3", "B1", null, "2026-03-12", "2"), vondst("V4", "B2", "2026-03-01", "2026-03-02", "5"),
                vondst("V5", "B5", null, null, null)));
        if (marking) out.add(instance("B6", "Berging", attrs("opgegeven sluitdag", day("2026-03-15")), ""));
        return out;
    }

    private static String date(String label, String attribute, String iso) {
        return "{\"kind\":\"attributeValue\",\"label\":\"" + label + "\",\"attribute\":\"" + attribute
                + "\",\"value\":{\"date\":\"" + iso + "\"}}";
    }

    private static String empty(String label, String attribute) {
        return "{\"kind\":\"attributeEmpty\",\"label\":\"" + label + "\",\"attribute\":\"" + attribute + "\"}";
    }

    /** The sluitdag of B1, B2, … in order; null is leeg. */
    private static String closingDays(String... iso) {
        List<String> out = new ArrayList<>();
        for (int i = 0; i < iso.length; i++)
            out.add(iso[i] == null ? empty("B" + (i + 1), "sluitdag") : date("B" + (i + 1), "sluitdag", iso[i]));
        return String.join(",", out);
    }

    /** The maatgewicht of B1 to B5 in order; an empty string is leeg. */
    private static String gewichten(String... n) {
        List<String> out = new ArrayList<>();
        for (int i = 0; i < n.length; i++)
            out.add(n[i].isEmpty() ? empty("B" + (i + 1), "maatgewicht")
                    : "{\"kind\":\"attributeValue\",\"label\":\"B" + (i + 1) + "\",\"attribute\":\"maatgewicht\",\"value\":"
                    + "{\"number\":\"" + n[i] + "\"}}");
        return String.join(",", out);
    }

    private static String caseDocument(List<String> instances, String expected) {
        return "{\"format\":\"uchar-case-v0\",\"asOfDate\":null,\"input\":{\"instances\":[" + String.join(",", instances)
                + "],\"parameters\":{}},\"expected\":[" + expected + "]}";
    }

    /** The verdict on one case of the exec-model branch and of {@code DRL_TEXT}, which must be the same: null when
     *  every expectation holds. */
    private static String play(String ir, String caseDocument) throws Exception {
        String exec = playTms(ir, caseDocument, DroolsBackend.Branch.EXEC_MODEL);
        assertEquals(exec, playTms(ir, caseDocument, DroolsBackend.Branch.DRL_TEXT), "DRL_TEXT naast de exec-model-tak");
        return exec;
    }

    /** The verdict of the exec-model branch on one case, where {@code DRL_TEXT} refuses the document in the same
     *  words if that branch refuses it. */
    private static String playTms(String ir, String caseDocument) throws Exception {
        try {
            return playTms(ir, caseDocument, DroolsBackend.Branch.EXEC_MODEL);
        } catch (CaseRunner.Excluded e) {
            CaseRunner.Excluded drl = assertThrows(CaseRunner.Excluded.class,
                    () -> playTms(ir, caseDocument, DroolsBackend.Branch.DRL_TEXT), "DRL_TEXT weigert ook");
            assertEquals(e.getMessage(), drl.getMessage(), "de weigering van DRL_TEXT");
            throw e;
        }
    }

    /** The verdict of the branch {@code branch} of the analysis on one case. */
    private static String playTms(String ir, String caseDocument, DroolsBackend.Branch branch) throws Exception {
        Path tmp = Files.createTempFile("defaults-consequentie", ".json");
        tmp.toFile().deleteOnExit();
        Files.writeString(tmp, ir);
        return CaseRunner.playCase(tmp, JSON.readTree(caseDocument), branch);
    }
}
