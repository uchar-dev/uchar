package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * A set fold whose test per element reads through a role of the ELEMENT: "de eerste van de vinddag van alle vondsten
 * van de Jutter die aan de volgende voorwaarde voldoen: zijn vinddag is later dan de sluitdag van de jutter van de
 * Vondst", where another rule, a defaults group or an allocation writes that far member. The subselection of a fold
 * (RUNTIME-SEMANTICS ch. 3.4a) and the matrix of a quantifier fold (ch. 3.9a) are read with the element as subject,
 * and a rule is re-evaluated whenever what it reads changes (ch. 5.1). Every document here is authored in this class
 * on Jutterswet material, one measured form each, with the values {@code ref} and clingo answer.
 *
 * <p><b>Red before the fix</b> (measured 2026-10-01 on the transpiler of {@code 2a0c6d60}, exec-model branch): the
 * subselection was a test on the element's pattern, which runs when the element is inserted or updated and not when
 * the object it reaches is, so the fold kept the members it had before the writer: {@code attribuut eerste late
 * vinddag: verwacht 2026-03-10, werkelijk leeg}, a count {@code verwacht 2, werkelijk 0}, a leeg test on the far member
 * {@code verwacht 0, werkelijk 3}, and in a condition {@code verwacht 1, werkelijk leeg}; the matrix of a quantifier
 * fold over another object than the subject the same. A date computation with the far member and a chain of two roles
 * in a subselection were refused by name ({@code vouw EERSTE over een setbron die deze tak niet in een waarde vouwt}).
 * A fold with the far member in the case input, a literal bound, a characteristic on the element that a rule derives
 * from the far member, and a rule that updates the element after the writer, played before and play after.
 *
 * <p>The DRL-text branch writes such a test into the element pattern of its accumulate or quantifier and does not see
 * the later write either ({@code verwacht 2, werkelijk 0}); it refuses the form by name.
 */
class FoldFilterFarSideWitnessTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    // ---------------- the subselection of a fold in a rule's value ----------------

    @Test void the_first_late_vinddag_after_the_sluitdag() throws Exception {
        // "De eerste late vinddag van een Jutter moet berekend worden als de eerste van de vinddag van alle vondsten van de
        // Jutter die aan de volgende voorwaarde voldoen: zijn vinddag is later dan de sluitdag van de jutter van de
        // Vondst", beside "De sluitdag van een Jutter moet gesteld worden op zijn startdag", in either order. ref and
        // clingo: sluitdag 01-03-2026, eerste late vinddag 10-03-2026.
        String eerste = regel("eerste late vinddag", "Jutter", "eerste late vinddag",
                fold("first", own("vinddag", "Vondst"), LATER_THAN_SLUITDAG, DATE), null);
        String caseDocument = caseDocument(THREE_VONDSTEN, day("J", "sluitdag", "2026-03-01") + "," + day("J", "eerste late vinddag", "2026-03-10"));
        assertNull(play(document(List.of(SLUITDAG, eerste)), caseDocument), "de schrijver voor de vouw");
        assertNull(play(document(List.of(eerste, SLUITDAG)), caseDocument), "de vouw voor de schrijver");
    }

    @Test void the_last_late_vinddag() throws Exception {
        // "de laatste van de vinddag van alle vondsten van de Jutter die …". ref and clingo: 20-03-2026.
        String last = regel("laatste late vinddag", "Jutter", "laatste late vinddag",
                fold("last", own("vinddag", "Vondst"), LATER_THAN_SLUITDAG, DATE), null);
        assertNull(play(document(List.of(SLUITDAG, last)),
                caseDocument(THREE_VONDSTEN, day("J", "laatste late vinddag", "2026-03-20"))), "de laatste");
    }

    @Test void the_count_of_late_vondsten() throws Exception {
        // "het aantal vondsten van de Jutter die …". ref and clingo: 2.
        assertNull(play(document(List.of(SLUITDAG, aantal(LATER_THAN_SLUITDAG))),
                caseDocument(THREE_VONDSTEN, number("J", "aantal late vondsten", 2))), "de telling");
    }

    @Test void the_sum_and_the_maximum() throws Exception {
        // "de som van het gewicht van alle vondsten …" and "de maximale waarde van het gewicht van alle vondsten …".
        // ref and clingo: 18 and 11.
        String sum = regel("late gewicht", "Jutter", "late gewicht",
                fold("sum", own("gewicht", "Vondst"), LATER_THAN_SLUITDAG, WHOLE), null);
        String max = regel("zwaarste late gewicht", "Jutter", "zwaarste late gewicht",
                fold("maximum", own("gewicht", "Vondst"), LATER_THAN_SLUITDAG, WHOLE), null);
        assertNull(play(document(List.of(SLUITDAG, sum, max)),
                caseDocument(THREE_VONDSTEN, number("J", "late gewicht", 18) + "," + number("J", "zwaarste late gewicht", 11))),
                "de som en het maximum");
    }

    @Test void a_rule_that_updates_the_element() throws Exception {
        // "Een Vondst is gedateerd indien zijn vinddag gevuld is" declared before the rule sluitdag: it updates every
        // element before the sluitdag is written. Declared after it, it updated them after the write, and the form
        // played before the fix as well. ref and clingo: 10-03-2026 in both orders.
        String dated = characteristicRule("gedateerd", "Vondst", predicate("filled", own("vinddag", "Vondst")));
        String eerste = regel("eerste late vinddag", "Jutter", "eerste late vinddag",
                fold("first", own("vinddag", "Vondst"), LATER_THAN_SLUITDAG, DATE), null);
        String caseDocument = caseDocument(THREE_VONDSTEN, day("J", "eerste late vinddag", "2026-03-10"));
        assertNull(play(document(List.of(dated, SLUITDAG, eerste)), caseDocument), "de bijwerking voor de schrijver");
        assertNull(play(document(List.of(SLUITDAG, eerste, dated)), caseDocument), "de bijwerking na de schrijver");
    }

    @Test void its_own_condition_on_the_subject() throws Exception {
        // The same fold "indien zijn sluitdag gevuld is": the rule fires only after the writer. ref and clingo: 10-03-2026.
        String eerste = regel("eerste late vinddag", "Jutter", "eerste late vinddag",
                fold("first", own("vinddag", "Vondst"), LATER_THAN_SLUITDAG, DATE),
                predicate("filled", own("sluitdag", "Jutter")));
        assertNull(play(document(List.of(SLUITDAG, eerste)),
                caseDocument(THREE_VONDSTEN, day("J", "eerste late vinddag", "2026-03-10"))), "de eigen voorwaarde");
    }

    @Test void a_characteristic_of_the_far_object() throws Exception {
        // "… die aan de volgende voorwaarde voldoen: zijn jutter actief is", and "… zijn jutter niet actief is", with
        // "Een Jutter is actief indien zijn sluitdag gevuld is". ref and clingo: J actief, 3 and 0.
        String actief = characteristicRule("actief", "Jutter", predicate("filled", own("sluitdag", "Jutter")));
        String case3 = caseDocument(THREE_VONDSTEN, kenmerk("J", "actief", true) + "," + number("J", "aantal late vondsten", 3));
        String case0 = caseDocument(THREE_VONDSTEN, kenmerk("J", "actief", true) + "," + number("J", "aantal late vondsten", 0));
        assertNull(play(document(List.of(aantal(the(characteristicCheck("actief", false, VONDST_JUTTER))), actief, SLUITDAG)),
                case3), "het kenmerk van de jutter");
        assertNull(play(document(List.of(aantal(the(characteristicCheck("actief", true, VONDST_JUTTER))), actief, SLUITDAG)),
                case0), "het ontkende kenmerk van de jutter");
    }

    @Test void an_empty_test_on_the_far_member() throws Exception {
        // "… die aan de volgende voorwaarde voldoen: de sluitdag van de jutter van de Vondst is leeg": the source empties
        // when the sluitdag is written. ref and clingo: 0.
        assertNull(play(document(List.of(aantal(the(predicate("empty", jutterSluitdag()))), SLUITDAG)),
                caseDocument(THREE_VONDSTEN, number("J", "aantal late vondsten", 0))), "de leeg-toets");
    }

    @Test void a_role_that_reaches_nothing() throws Exception {
        // "… de afgeleide openingsdag van de haven van de Vondst is leeg" and "… is ongelijk aan 01-03-2026", with "De
        // afgeleide openingsdag van een Haven moet gesteld worden op zijn openingsdag": V2's haven has no openingsdag, V3
        // has no haven, so the far member reads leeg over an absent relation (ch. 1.2). ref and clingo: 2 and 2.
        String caseDocument = caseDocument(HAVENS, day("H1", "afgeleide openingsdag", "2026-03-01") + "," + number("J", "aantal late vondsten", 2));
        assertNull(play(document(List.of(aantal(the(predicate("empty", havenOpeningsdag()))), DERIVED_OPENINGSDAG)),
                caseDocument), "de leeg-toets over een lege rol");
        assertNull(play(document(List.of(aantal(the(comparison("notEqual", havenOpeningsdag(),
                dateLiteral("2026-03-01")))), DERIVED_OPENINGSDAG)), caseDocument), "ongelijk over een lege rol");
    }

    @Test void two_far_objects() throws Exception {
        // "… die aan alle volgende voorwaarden voldoen: zijn vinddag is later dan de sluitdag van de jutter van de Vondst;
        // zijn vinddag is later dan de afgeleide openingsdag van de haven van de Vondst". ref and clingo: 1 (V4).
        String filter = all(comparison("later", own("vinddag", "Vondst"), jutterSluitdag()),
                comparison("later", own("vinddag", "Vondst"), havenOpeningsdag()));
        assertNull(play(document(List.of(aantal(filter), DERIVED_OPENINGSDAG, SLUITDAG)),
                caseDocument(HAVENS, number("J", "aantal late vondsten", 1))), "twee verre objecten");
    }

    @Test void a_date_arithmetic_with_the_far_member() throws Exception {
        // "… die aan de volgende voorwaarde voldoen: zijn vinddag min 7 dg is later dan de sluitdag van de jutter van de
        // Vondst". Refused by name before the fix. ref and clingo: 10-03-2026.
        String filter = the(comparison("later", dateMinus(own("vinddag", "Vondst"), 7), jutterSluitdag()));
        String eerste = regel("eerste late vinddag", "Jutter", "eerste late vinddag",
                fold("first", own("vinddag", "Vondst"), filter, DATE), null);
        assertNull(play(document(List.of(eerste, SLUITDAG)),
                caseDocument(THREE_VONDSTEN, day("J", "eerste late vinddag", "2026-03-10"))), "de datumrekensom");
    }

    @Test void a_chain_of_two_roles() throws Exception {
        // "… zijn vinddag is later dan de sluitdag van de thuisploeg van de jutter van de Vondst", the sluitdag of the
        // Ploeg by a rule; once with the thuisploeg in the case, once laid during the run by the fact creation "Een lid
        // van een Ploeg is een geworven jutter van de Ploeg". Refused by name before the fix. ref and clingo: 10-03-2026.
        String filter = the(comparison("later", own("vinddag", "Vondst"), navigated("sluitdag", "Ploeg",
                "de sluitdag van de thuisploeg van de jutter van de Vondst", roleBinding("JutterHeeftVondst", "jutter"),
                roleBinding("PloegHeeftJutter", "thuisploeg"))));
        String eerste = regel("eerste late vinddag", "Jutter", "eerste late vinddag",
                fold("first", own("vinddag", "Vondst"), filter, DATE), null);
        String verwacht = day("P", "sluitdag", "2026-03-01") + "," + day("J", "eerste late vinddag", "2026-03-10");
        assertNull(play(document(List.of(eerste, PLOEG_CLOSING_DAY)), caseDocument(ploegVondsten("thuisploeg"), verwacht)),
                "de thuisploeg uit de case");
        assertNull(play(document(List.of(eerste, PLOEG_CLOSING_DAY), List.of(), List.of(), List.of(WERVING), List.of()),
                caseDocument(ploegVondsten("wervende ploeg"), verwacht)), "de thuisploeg uit de werving");
    }

    // ---------------- other writers of the far member ----------------

    @Test void the_far_member_out_of_a_defaults_group() throws Exception {
        // The sluitdag by a group: "Regel sluitdag" and "Afwijking van sluitdag: opgave gaat voor" (the opgegeven sluitdag
        // where it is filled). ref and clingo: J (opgave 12-03-2026) 20-03-2026, K 10-03-2026.
        String eerste = regel("eerste late vinddag", "Jutter", "eerste late vinddag",
                fold("first", own("vinddag", "Vondst"), LATER_THAN_SLUITDAG, DATE), null);
        String caseDocument = caseDocument(List.of(jutter("J", "\"startdag\":{\"date\":\"2026-03-01\"},\"opgegeven sluitdag\":{\"date\":\"2026-03-12\"}"),
                        jutter("K", "\"startdag\":{\"date\":\"2026-03-01\"}"), vondst("V1", "2026-02-15", 5, "J"),
                        vondst("V2", "2026-03-10", 7, "J"), vondst("V3", "2026-03-20", 11, "J"), vondst("W1", "2026-03-10", 1, "K"),
                        vondst("W2", "2026-03-20", 1, "K")),
                day("J", "eerste late vinddag", "2026-03-20") + "," + day("K", "eerste late vinddag", "2026-03-10"));
        assertNull(play(document(List.of(eerste), List.of(), List.of(), List.of(), List.of(GROUP_SLUITDAG)), caseDocument),
                "de sluitdag uit een defaultsgroep");
    }

    @Test void the_far_member_out_of_an_allocation() throws Exception {
        // "… het bergingsaandeel van de vondstberger van de Vondst is groter dan 10", the aandeel from "De opbrengst van een
        // berging wordt verdeeld over de bergingsaandelen van alle bergers van de berging, in gelijke delen". ref and
        // clingo: B1 20, B3 10, J 1.
        String filter = the(comparison("greater", navigated("bergingsaandeel", "Berger",
                "het bergingsaandeel van de vondstberger van de Vondst", roleBinding("BergerVanVondst", "vondstberger")),
                numberLiteral(10)));
        String caseDocument = caseDocument(List.of(instance("B", "Berging", "\"opbrengst\":{\"number\":\"40\"}", ""),
                        instance("C", "Berging", "\"opbrengst\":{\"number\":\"10\"}", ""),
                        instance("B1", "Berger", "", "\"berging\":[\"B\"]"), instance("B2", "Berger", "", "\"berging\":[\"B\"]"),
                        instance("B3", "Berger", "", "\"berging\":[\"C\"]"), jutter("J", ""),
                        instance("V1", "Vondst", "", "\"jutter\":[\"J\"],\"vondstberger\":[\"B1\"]"),
                        instance("V2", "Vondst", "", "\"jutter\":[\"J\"],\"vondstberger\":[\"B3\"]"),
                        instance("V3", "Vondst", "", "\"jutter\":[\"J\"]")),
                number("B1", "bergingsaandeel", 20) + "," + number("B3", "bergingsaandeel", 10) + ","
                        + number("J", "aantal late vondsten", 1));
        assertNull(play(document(List.of(aantal(filter)), List.of(), List.of(VERDELING), List.of(), List.of()), caseDocument),
                "het aandeel uit een verdeling");
    }

    @Test void the_far_object_out_of_a_creation() throws Exception {
        // "… de meldingsdag van de melding van de Vondst is later dan 01-03-2026", the melding from "Een Vondst heeft een
        // melding met meldingsdag gelijk aan zijn vinddag indien zijn vinddag gevuld is". The creation links the melding
        // to the element and updates it, so the form played before the fix as well. ref and clingo: 2.
        String filter = the(comparison("later", navigated("meldingsdag", "Melding", "de meldingsdag van de melding van de Vondst",
                roleBinding("VondstHeeftMelding", "melding")), dateLiteral("2026-03-01")));
        String caseDocument = caseDocument(List.of(jutter("J", ""), vondst("V1", "2026-02-15", 5, "J"), vondst("V2", "2026-03-10", 7, "J"),
                        vondst("V3", "2026-03-20", 11, "J"), instance("V4", "Vondst", "", "\"jutter\":[\"J\"]")),
                number("J", "aantal late vondsten", 2));
        assertNull(play(document(List.of(aantal(filter)), List.of(MESSAGE), List.of(), List.of(), List.of()), caseDocument),
                "de melding uit een creatie");
    }

    // ---------------- a fold in a condition, and the matrix of a quantifier fold ----------------

    @Test void the_count_in_a_condition() throws Exception {
        // "De laatklasse van een Jutter moet berekend worden als 1 indien het aantal vondsten van de Jutter die aan de
        // volgende voorwaarde voldoen: zijn vinddag is later dan de sluitdag van de jutter van de Vondst groter is dan 0".
        // ref and clingo: 1.
        String laatklasse = regel("laatklasse", "Jutter", "laatklasse", numberLiteral(1),
                comparison("greater", count(LATER_THAN_SLUITDAG), numberLiteral(0)));
        assertNull(play(document(List.of(laatklasse, SLUITDAG)), caseDocument(THREE_VONDSTEN, number("J", "laatklasse", 1))),
                "de telling in de voorwaarde");
    }

    @Test void the_quantifier_in_a_condition() throws Exception {
        // "… indien de vinddag van ten minste één van de vondsten van de Jutter die aan de volgende voorwaarde voldoen:
        // zijn vinddag is later dan de sluitdag van de jutter van de Vondst gevuld is". ref and clingo: 1.
        String laatklasse = regel("laatklasse", "Jutter", "laatklasse", numberLiteral(1),
                quantifierFold("atLeast", 1, LATER_THAN_SLUITDAG, predicate("filled", own("vinddag", "Vondst"))));
        assertNull(play(document(List.of(laatklasse, SLUITDAG)), caseDocument(THREE_VONDSTEN, number("J", "laatklasse", 1))),
                "de kwantor in de voorwaarde");
    }

    @Test void an_empty_test_in_a_condition_fold() throws Exception {
        // "… 1 indien het aantal vondsten van de Jutter die aan de volgende voorwaarde voldoen: de sluitdag van de jutter
        // van de Vondst is leeg groter is dan 0": true only on the transient leeg. ref and clingo: leeg.
        String laatklasse = regel("laatklasse", "Jutter", "laatklasse", numberLiteral(1),
                comparison("greater", count(the(predicate("empty", jutterSluitdag()))), numberLiteral(0)));
        assertNull(play(document(List.of(laatklasse, SLUITDAG)), caseDocument(THREE_VONDSTEN, empty("J", "laatklasse"))),
                "de leeg-toets in de voorwaardevouw");
    }

    @Test void the_matrix_of_a_quantifier() throws Exception {
        // "… indien de vinddag van ten minste één van de vondsten van de Jutter later is dan de afgeleide openingsdag van
        // zijn haven" and the same under geen: the matrix reads through the haven of the element, not the subject. ref and
        // clingo: 1 under ten minste één, leeg under geen.
        String matrix = comparison("later", own("vinddag", "Vondst"), navigated("afgeleide openingsdag", "Haven",
                "de afgeleide openingsdag van zijn haven", roleBinding("HavenVanVondst", "haven")));
        String atLeast = regel("laatklasse", "Jutter", "laatklasse", numberLiteral(1), quantifierFold("atLeast", 1, null, matrix));
        String none = regel("laatklasse", "Jutter", "laatklasse", numberLiteral(1), quantifierFold("none", 0, null, matrix));
        assertNull(play(document(List.of(atLeast, DERIVED_OPENINGSDAG)), caseDocument(HAVENS, number("J", "laatklasse", 1))),
                "de matrix onder ten minste één");
        assertNull(play(document(List.of(none, DERIVED_OPENINGSDAG)), caseDocument(HAVENS, empty("J", "laatklasse"))),
                "de matrix onder geen");
    }

    @Test void the_filter_of_a_quantifier_via_another_role() throws Exception {
        // "… indien de vinddag van ten minste één van de vondsten van de Jutter die aan de volgende voorwaarde voldoen: zijn
        // vinddag is later dan de afgeleide openingsdag van de haven van de Vondst gevuld is". ref and clingo: 1.
        String laatklasse = regel("laatklasse", "Jutter", "laatklasse", numberLiteral(1), quantifierFold("atLeast", 1,
                the(comparison("later", own("vinddag", "Vondst"), havenOpeningsdag())),
                predicate("filled", own("vinddag", "Vondst"))));
        assertNull(play(document(List.of(laatklasse, DERIVED_OPENINGSDAG)), caseDocument(HAVENS, number("J", "laatklasse", 1))),
                "het kwantorfilter via de haven");
    }

    // ---------------- flanks: forms that played before ----------------

    @Test void forms_that_already_played() throws Exception {
        // The sluitdag in the case input; a literal bound "later dan 01-03-2026"; a characteristic on the element ("Een
        // Vondst is laat indien zijn vinddag later is dan de sluitdag van de jutter van de Vondst", the filter "hij is
        // laat"). ref and clingo: 10-03-2026 in all three.
        String eerste = regel("eerste late vinddag", "Jutter", "eerste late vinddag",
                fold("first", own("vinddag", "Vondst"), LATER_THAN_SLUITDAG, DATE), null);
        assertNull(play(document(List.of(eerste)), caseDocument(List.of(jutter("J", "\"sluitdag\":{\"date\":\"2026-03-01\"}"),
                vondst("V1", "2026-02-15", 5, "J"), vondst("V2", "2026-03-10", 7, "J"), vondst("V3", "2026-03-20", 11, "J")),
                day("J", "eerste late vinddag", "2026-03-10"))), "de sluitdag in de invoer");
        String literal = regel("eerste late vinddag", "Jutter", "eerste late vinddag", fold("first", own("vinddag", "Vondst"),
                the(comparison("later", own("vinddag", "Vondst"), dateLiteral("2026-03-01"))), DATE), null);
        assertNull(play(document(List.of(SLUITDAG, literal)), caseDocument(THREE_VONDSTEN, day("J", "eerste late vinddag", "2026-03-10"))),
                "een letterlijke grens");
        String late = characteristicRule("laat", "Vondst", comparison("later", own("vinddag", "Vondst"), jutterSluitdag()));
        String bridge = regel("eerste late vinddag", "Jutter", "eerste late vinddag",
                fold("first", own("vinddag", "Vondst"), the(characteristicCheck("laat", false, null)), DATE), null);
        assertNull(play(document(List.of(SLUITDAG, late, bridge)), caseDocument(THREE_VONDSTEN, day("J", "eerste late vinddag", "2026-03-10"))),
                "een kenmerk op het element");
    }

    // ---------------- forms that refuse by name ----------------

    @Test void a_union_selection_via_a_role_plays_with_truth_maintenance() throws Exception {
        // "het aantal vondsten van de Jutter die gedateerd zijn en vondsten van de Jutter wier jutter actief is": a union
        // selection whose subselection reads through a role of the element. The selections share one element pattern
        // and its test, so the destructive exec-model branch had no join for it and refused the form; before, it
        // counted with the characteristic as it stood at insertion. The exec-model branch with truth maintenance plays
        // it as ref answers, 3, on this case (TmsLikeRefWitnessTest, unieselectie-via-rol).
        String union = "{\"node\":\"Aggregation\",\"kind\":\"count\",\"arguments\":[{\"node\":\"InstanceSetUnion\",\"branches\":["
                + branch(the(characteristicCheck("gedateerd", false, null))) + "," + branch(the(characteristicCheck("actief", false, VONDST_JUTTER)))
                + "],\"source\":{\"snippet\":\"vondsten die gedateerd zijn en vondsten wier jutter actief is\"}}],\"type\":" + COUNT_NUMBER
                + ",\"source\":{\"snippet\":\"het aantal vondsten die gedateerd zijn en vondsten wier jutter actief is\"}}";
        String actief = characteristicRule("actief", "Jutter", predicate("filled", own("sluitdag", "Jutter")));
        String ir = document(List.of(regel("aantal late vondsten", "Jutter", "aantal late vondsten", union, null), actief, SLUITDAG));
        assertNull(playTms(ir, caseDocument(THREE_VONDSTEN, number("J", "aantal late vondsten", 3))), "ref agrees: 3");
    }

    @Test void the_text_branch_plays_a_test_via_the_role_of_the_element() throws Exception {
        // The destructive DRL-text lowering wrote the subselection and the matrix into the element pattern, where a
        // later write on the far object is not seen (before: verwacht 2, werkelijk 0; verwacht 1, werkelijk leeg), and
        // refused them by name ("via een rol van het element") until card drl-teksttak-op-truth-maintenance; since then
        // the text renders the analysis, which binds the far object's standing, and plays all three forms.
        String laatklasse = regel("laatklasse", "Jutter", "laatklasse", numberLiteral(1),
                comparison("greater", count(LATER_THAN_SLUITDAG), numberLiteral(0)));
        String matrix = regel("laatklasse", "Jutter", "laatklasse", numberLiteral(1), quantifierFold("atLeast", 1, null,
                comparison("later", own("vinddag", "Vondst"), navigated("afgeleide openingsdag", "Haven",
                        "de afgeleide openingsdag van zijn haven", roleBinding("HavenVanVondst", "haven")))));
        List<String[]> vormen = List.of(
                new String[] { document(List.of(SLUITDAG, aantal(LATER_THAN_SLUITDAG))),
                        caseDocument(THREE_VONDSTEN, number("J", "aantal late vondsten", 2)) },
                new String[] { document(List.of(laatklasse, SLUITDAG)), caseDocument(THREE_VONDSTEN, number("J", "laatklasse", 1)) },
                new String[] { document(List.of(matrix, DERIVED_OPENINGSDAG)), caseDocument(HAVENS, number("J", "laatklasse", 1)) });
        for (String[] v : vormen) {
            assertNull(play(v[0], v[1]), "de exectak speelt");
            assertNull(playDrl(v[0], v[1]), "de teksttak speelt");
        }
    }

    // ---------------- documents ----------------

    private static final String DATE = "{\"node\":\"Date\",\"precision\":\"day\"}";
    private static final String WHOLE = "{\"node\":\"Number\",\"whole\":true,\"decimals\":0,\"sign\":\"unspecified\"}";
    private static final String COUNT_NUMBER = "{\"node\":\"Number\",\"whole\":true,\"decimals\":0,\"sign\":\"nonNegative\"}";
    private static final String DAG = "{\"node\":\"Unit\",\"abbreviation\":\"dg\",\"system\":\"Tijd\",\"canonical\":\"dg\","
            + "\"factorToCanonical\":{\"num\":1,\"den\":1}}";

    private static String attribuut(String naam, String datatype) {
        return "{\"node\":\"Attribute\",\"name\":\"" + naam + "\",\"datatype\":" + datatype + "}";
    }

    private static String attributes(String datatype, String... names) {
        List<String> a = new ArrayList<>();
        for (String n : names) a.add(attribuut(n, datatype));
        return String.join(",", a);
    }

    private static String objecttype(String naam, String plural, boolean animate, String kenmerken, String attributes) {
        return "{\"node\":\"ObjectType\",\"name\":\"" + naam + "\",\"animate\":" + animate + ",\"plural\":\"" + plural
                + "\",\"attributes\":[" + attributes + "],\"characteristics\":[" + kenmerken + "]}";
    }

    private static String kenmerken(String... names) {
        List<String> k = new ArrayList<>();
        for (String n : names) k.add("{\"node\":\"Characteristic\",\"name\":\"" + n + "\",\"kind\":\"adjectival\"}");
        return String.join(",", k);
    }

    /** A fact type between one {@code oneType} and one ({@code manyPlural} null) or many {@code manyType}. */
    private static String feittype(String naam, String oneRole, String oneType, String manyRole, String manyPlural, String manyType) {
        return "{\"node\":\"FactType\",\"name\":\"" + naam + "\",\"reciprocal\":false,\"roles\":["
                + "{\"node\":\"Role\",\"name\":\"" + oneRole + "\",\"objectType\":\"" + oneType + "\",\"many\":false},"
                + "{\"node\":\"Role\",\"name\":\"" + manyRole + "\",\"objectType\":\"" + manyType + "\""
                + (manyPlural == null ? ",\"many\":false}" : ",\"plural\":\"" + manyPlural + "\",\"many\":true}") + "]}";
    }

    /** Ploeg, Jutter, Vondst, Haven, Melding, Berging and Berger. A Jutter has many vondsten, one thuisploeg and one
     *  wervende ploeg; a Vondst one haven, one melding and one vondstberger; a Berging many bergers. */
    private static final String MODEL = "{\"objectTypes\":["
            + objecttype("Ploeg", "Ploegen", false, "", attributes(DATE, "startdag", "sluitdag")) + ","
            + objecttype("Jutter", "Jutters", true, kenmerken("actief"),
                    attributes(DATE, "startdag", "opgegeven sluitdag", "sluitdag", "eerste late vinddag", "laatste late vinddag")
                    + "," + attributes(WHOLE, "aantal late vondsten", "late gewicht", "zwaarste late gewicht", "laatklasse")) + ","
            + objecttype("Vondst", "Vondsten", false, kenmerken("gedateerd", "laat"),
                    attribuut("vinddag", DATE) + "," + attribuut("gewicht", WHOLE)) + ","
            + objecttype("Haven", "Havens", false, "", attributes(DATE, "openingsdag", "afgeleide openingsdag")) + ","
            + objecttype("Melding", "Meldingen", false, "", attribuut("meldingsdag", DATE)) + ","
            + objecttype("Berging", "Bergingen", false, "", attributes(WHOLE, "opbrengst", "onverdeelde rest")) + ","
            + objecttype("Berger", "Bergers", true, "", attribuut("bergingsaandeel", WHOLE))
            + "],\"factTypes\":["
            + feittype("JutterHeeftVondst", "jutter", "Jutter", "vondst", "vondsten", "Vondst") + ","
            + feittype("PloegHeeftJutter", "thuisploeg", "Ploeg", "lid", "leden", "Jutter") + ","
            + feittype("PloegWerftJutter", "wervende ploeg", "Ploeg", "geworven jutter", "geworven jutters", "Jutter") + ","
            + feittype("HavenVanVondst", "haven", "Haven", "havenvondst", "havenvondsten", "Vondst") + ","
            + feittype("VondstHeeftMelding", "gemelde vondst", "Vondst", "melding", null, "Melding") + ","
            + feittype("bergingsdeelname", "berging", "Berging", "berger", "bergers", "Berger") + ","
            + feittype("BergerVanVondst", "vondstberger", "Berger", "geborgen vondst", "geborgen vondsten", "Vondst")
            + "],\"parameters\":[],\"domains\":[],\"unitSystems\":[{\"node\":\"UnitSystem\",\"name\":\"Tijd\",\"units\":["
            + "{\"node\":\"Unit\",\"abbreviation\":\"dg\",\"system\":\"Tijd\",\"canonical\":\"dg\",\"factorToCanonical\":"
            + "{\"num\":1,\"den\":1},\"spellings\":[\"dag\"]}]}],\"dimensions\":[],\"timelines\":[],\"dayKinds\":[]}";

    private static String roleBinding(String feittype, String rol) {
        return "{\"node\":\"RoleBinding\",\"factType\":\"" + feittype + "\",\"role\":\"" + rol + "\"}";
    }

    private static final String VONDST_JUTTER = roleBinding("JutterHeeftVondst", "jutter");

    private static String own(String attribuut, String type) {
        return "{\"node\":\"AttributeReference\",\"attribute\":\"" + attribuut + "\",\"subjectType\":\"" + type
                + "\",\"source\":{\"snippet\":\"zijn " + attribuut + "\"}}";
    }

    /** A read of {@code attribuut} on {@code type} over the role hops {@code pad}. */
    private static String navigated(String attribuut, String type, String snippet, String... pad) {
        return "{\"node\":\"AttributeReference\",\"attribute\":\"" + attribuut + "\",\"subjectType\":\"" + type
                + "\",\"source\":{\"snippet\":\"" + snippet + "\"},\"path\":[" + String.join(",", pad) + "]}";
    }

    /** "de sluitdag van de jutter van de Vondst". */
    private static String jutterSluitdag() {
        return navigated("sluitdag", "Jutter", "de sluitdag van de jutter van de Vondst", VONDST_JUTTER);
    }

    /** "de afgeleide openingsdag van de haven van de Vondst". */
    private static String havenOpeningsdag() {
        return navigated("afgeleide openingsdag", "Haven", "de afgeleide openingsdag van de haven van de Vondst",
                roleBinding("HavenVanVondst", "haven"));
    }

    private static String dateLiteral(String iso) {
        return "{\"node\":\"Literal\",\"value\":{\"date\":\"" + iso + "\"},\"type\":" + DATE + ",\"source\":{\"snippet\":\"" + iso + "\"}}";
    }

    private static String numberLiteral(int n) {
        return "{\"node\":\"Literal\",\"value\":{\"number\":\"" + n + "\"},\"type\":" + WHOLE + ",\"source\":{\"snippet\":\"" + n + "\"}}";
    }

    /** "&lt;datum&gt; min &lt;n&gt; dg". */
    private static String dateMinus(String date, int days) {
        return "{\"node\":\"Binary\",\"op\":\"minus\",\"left\":" + date + ",\"right\":{\"node\":\"Literal\",\"value\":{\"number\":\""
                + days + "\"},\"type\":{\"node\":\"Number\",\"whole\":true,\"decimals\":0,\"sign\":\"unspecified\",\"unit\":" + DAG
                + "},\"source\":{\"snippet\":\"" + days + " dg\"}},\"type\":" + DATE + ",\"source\":{\"snippet\":\"min " + days + " dg\"}}";
    }

    private static String comparison(String op, String left, String right) {
        return "{\"node\":\"Comparison\",\"op\":\"" + op + "\",\"left\":" + left + ",\"right\":" + right
                + ",\"source\":{\"snippet\":\"vergelijking " + op + "\"}}";
    }

    private static String predicate(String soort, String operand) {
        return "{\"node\":\"Predicate\",\"kind\":\"" + soort + "\",\"operand\":" + operand
                + ",\"negated\":false,\"digits\":0,\"source\":{\"snippet\":\"" + soort + "\"}}";
    }

    /** A characteristic check, on the subject itself ({@code navigation} null) or over one role. */
    private static String characteristicCheck(String kenmerk, boolean negated, String navigation) {
        return "{\"node\":\"CharacteristicCheck\",\"characteristic\":\"" + kenmerk + "\",\"negated\":" + negated
                + ",\"source\":{\"snippet\":\"" + kenmerk + "\"}" + (navigation == null ? "" : ",\"navigation\":" + navigation) + "}";
    }

    /** "aan de volgende voorwaarde voldoen: &lt;deel&gt;". */
    private static String the(String part) {
        return "{\"node\":\"Compound\",\"quantifier\":\"the\",\"threshold\":0,\"parts\":[" + part
                + "],\"source\":{\"snippet\":\"aan de volgende voorwaarde voldoen\"}}";
    }

    /** "aan alle volgende voorwaarden voldoen: &lt;delen&gt;". */
    private static String all(String... parts) {
        return "{\"node\":\"Compound\",\"quantifier\":\"all\",\"threshold\":0,\"parts\":[" + String.join(",", parts)
                + "],\"source\":{\"snippet\":\"aan alle volgende voorwaarden voldoen\"}}";
    }

    private static final String LATER_THAN_SLUITDAG =
            the(comparison("later", own("vinddag", "Vondst"), jutterSluitdag()));

    /** "de &lt;soort&gt; van de &lt;argument&gt; van alle vondsten van de Jutter die &lt;filter&gt;". */
    private static String fold(String soort, String argument, String filter, String type) {
        return "{\"node\":\"Aggregation\",\"kind\":\"" + soort + "\",\"arguments\":[" + argument + "],\"elementType\":\"Vondst\","
                + "\"filter\":" + filter + ",\"sourceRole\":" + roleBinding("JutterHeeftVondst", "vondst") + ",\"type\":" + type
                + ",\"source\":{\"snippet\":\"" + soort + " van alle vondsten van de Jutter die …\"}}";
    }

    /** "het aantal vondsten van de Jutter die &lt;filter&gt;". */
    private static String count(String filter) {
        return fold("count", "{\"node\":\"AttributeReference\",\"attribute\":\"vondsten\",\"subjectType\":\"Jutter\","
                + "\"source\":{\"snippet\":\"vondsten van de Jutter\"}}", filter, COUNT_NUMBER);
    }

    /** "Het aantal late vondsten van een Jutter moet berekend worden als het aantal vondsten van de Jutter die &lt;filter&gt;." */
    private static String aantal(String filter) {
        return regel("aantal late vondsten", "Jutter", "aantal late vondsten", count(filter), null);
    }

    /** A quantifier fold over the vondsten of the Jutter; {@code filter} null for none, {@code drempel} 0 under geen. */
    private static String quantifierFold(String quantifier, int threshold, String filter, String matrix) {
        return "{\"node\":\"QuantifierFold\",\"quantifier\":\"" + quantifier + "\"" + (threshold == 0 ? "" : ",\"threshold\":" + threshold)
                + ",\"elementType\":\"Vondst\"" + (filter == null ? "" : ",\"filter\":" + filter) + ",\"test\":" + matrix
                + ",\"source\":{\"snippet\":\"" + quantifier + " van de vondsten van de Jutter\"},\"sourceRole\":"
                + roleBinding("JutterHeeftVondst", "vondst") + "}";
    }

    /** One selection of an instance-set union over the vondsten of the Jutter. */
    private static String branch(String filter) {
        return "{\"node\":\"InstanceSetBranch\",\"elementType\":\"Vondst\",\"sourceRole\":" + roleBinding("JutterHeeftVondst", "vondst")
                + ",\"filter\":" + filter + ",\"source\":{\"snippet\":\"vondsten van de Jutter die …\"}}";
    }

    /** One rule with one version; {@code voorwaarde} null for none. */
    private static String regel(String naam, String type, String doel, String waarde, String voorwaarde) {
        return "{\"node\":\"Rule\",\"name\":\"" + naam + "\",\"versions\":[{\"validity\":{\"node\":\"Validity\"},"
                + "\"target\":{\"node\":\"AttributeReference\",\"attribute\":\"" + doel + "\",\"subjectType\":\"" + type + "\","
                + "\"source\":{\"snippet\":\"De " + doel + "\"}},\"value\":" + waarde
                + (voorwaarde == null ? "" : ",\"condition\":" + voorwaarde) + ",\"initialisation\":false,\"presentation\":\"equation\"}]}";
    }

    /** "Een &lt;type&gt; is &lt;kenmerk&gt; indien &lt;voorwaarde&gt;." */
    private static String characteristicRule(String kenmerk, String type, String voorwaarde) {
        return "{\"node\":\"Rule\",\"name\":\"" + kenmerk + "\",\"versions\":[{\"validity\":{\"node\":\"Validity\"},\"target\":"
                + "{\"node\":\"AttributeReference\",\"attribute\":\"" + kenmerk + "\",\"subjectType\":\"" + type + "\",\"source\":"
                + "{\"snippet\":\"Een " + type + " is " + kenmerk + "\"}},\"value\":{\"node\":\"Literal\",\"value\":{\"boolean\":true},"
                + "\"type\":{\"node\":\"Boolean\"},\"source\":{\"snippet\":\"waar\"}},\"condition\":" + voorwaarde
                + ",\"initialisation\":false}]}";
    }

    /** "De sluitdag van een Jutter moet gesteld worden op zijn startdag." */
    private static final String SLUITDAG = regel("sluitdag", "Jutter", "sluitdag", own("startdag", "Jutter"), null);

    /** "De sluitdag van een Ploeg moet gesteld worden op zijn startdag." */
    private static final String PLOEG_CLOSING_DAY = regel("sluitdag van de ploeg", "Ploeg", "sluitdag", own("startdag", "Ploeg"), null);

    /** "De afgeleide openingsdag van een Haven moet gesteld worden op zijn openingsdag." */
    private static final String DERIVED_OPENINGSDAG = regel("afgeleide openingsdag", "Haven", "afgeleide openingsdag",
            own("openingsdag", "Haven"), null);

    /** "Regel sluitdag" with "Afwijking van sluitdag: opgave gaat voor": the opgegeven sluitdag where it is filled. */
    private static final String GROUP_SLUITDAG = "{\"node\":\"PrioritizedDefaults\",\"name\":\"sluitdag\",\"subject\":\"Jutter\","
            + "\"attribute\":\"sluitdag\",\"defaults\":[{\"node\":\"Default\",\"justification\":"
            + predicate("filled", own("opgegeven sluitdag", "Jutter")) + ",\"consequence\":" + own("opgegeven sluitdag", "Jutter")
            + "},{\"node\":\"Default\",\"justification\":{\"node\":\"Compound\",\"quantifier\":\"all\",\"threshold\":0,\"parts\":[],"
            + "\"source\":{\"snippet\":\"altijd\"}},\"consequence\":" + own("startdag", "Jutter") + "}],\"validity\":{\"node\":\"Validity\"}}";

    /** "De opbrengst van een berging wordt verdeeld over de bergingsaandelen van alle bergers van de berging, in gelijke
     *  delen, afgerond op 0 decimalen naar beneden; als onverdeelde rest blijft de onverdeelde rest van de berging over." */
    private static final String VERDELING = "{\"node\":\"Allocation\",\"name\":\"verdeel de opbrengst van een berging\",\"versions\":["
            + "{\"sourceType\":\"Berging\",\"sourceAttribute\":\"opbrengst\",\"targetType\":\"Berger\",\"targetAttribute\":"
            + "\"bergingsaandeel\",\"method\":\"equalShares\",\"roundingDecimals\":0,\"remainderAttribute\":\"onverdeelde rest\","
            + "\"descending\":false,\"validity\":{\"node\":\"Validity\"},\"connectingRole\":" + roleBinding("bergingsdeelname", "berger") + "}]}";

    /** "Een Vondst heeft een melding met meldingsdag gelijk aan zijn vinddag indien zijn vinddag gevuld is." */
    private static final String MESSAGE = "{\"node\":\"ObjectCreation\",\"name\":\"maak een melding\",\"versions\":[{\"createdType\":"
            + "\"Melding\",\"subject\":\"Vondst\",\"initialisations\":[{\"node\":\"MemberInit\",\"member\":\"meldingsdag\",\"value\":"
            + own("vinddag", "Vondst") + ",\"characteristic\":false}],\"condition\":" + predicate("filled", own("vinddag", "Vondst"))
            + ",\"validity\":{\"node\":\"Validity\"},\"connectingRole\":" + roleBinding("VondstHeeftMelding", "melding") + "}]}";

    /** "Een lid van een Ploeg is een geworven jutter van de Ploeg." */
    private static final String WERVING = "{\"node\":\"FactCreation\",\"name\":\"werving\",\"versions\":[{\"factType\":"
            + "\"PloegHeeftJutter\",\"anchorRole\":\"thuisploeg\",\"targetRole\":\"lid\",\"anchor\":\"Ploeg\",\"path\":["
            + roleBinding("PloegWerftJutter", "geworven jutter") + "],\"validity\":{\"node\":\"Validity\"}}]}";

    private static String document(List<String> rules) {
        return document(rules, List.of(), List.of(), List.of(), List.of());
    }

    private static String document(List<String> rules, List<String> creations, List<String> allocations,
                                   List<String> factCreations, List<String> groups) {
        return "{\"format\":\"uchar-ir-v1\",\"model\":" + MODEL + ",\"rules\":{\"rules\":[" + String.join(",", rules) + "],"
                + "\"dayKindDefinitions\":[],\"objectCreations\":[" + String.join(",", creations) + "],\"allocations\":["
                + String.join(",", allocations) + "],\"consistencies\":[],\"uniquenesses\":[],\"factCreations\":["
                + String.join(",", factCreations) + "],\"startPointDeterminations\":[],\"ruleGroups\":[],\"prioritizedDefaults\":["
                + String.join(",", groups) + "]},\"diagnostics\":[]}";
    }

    // ---------------- cases ----------------

    private static String instance(String label, String type, String attributes, String roles) {
        return "{\"label\":\"" + label + "\",\"objectType\":\"" + type + "\",\"attributes\":{" + attributes + "},\"roles\":{"
                + roles + "},\"characteristics\":[]}";
    }

    private static String jutter(String label, String attributes) {
        return instance(label, "Jutter", attributes, "");
    }

    private static String vondst(String label, String vinddag, int weight, String jutter) {
        return instance(label, "Vondst", "\"vinddag\":{\"date\":\"" + vinddag + "\"},\"gewicht\":{\"number\":\"" + weight + "\"}",
                "\"jutter\":[\"" + jutter + "\"]");
    }

    /** J (startdag 01-03-2026) with V1 (15-02-2026, 5), V2 (10-03-2026, 7) and V3 (20-03-2026, 11). */
    private static final List<String> THREE_VONDSTEN = List.of(jutter("J", "\"startdag\":{\"date\":\"2026-03-01\"}"),
            vondst("V1", "2026-02-15", 5, "J"), vondst("V2", "2026-03-10", 7, "J"), vondst("V3", "2026-03-20", 11, "J"));

    /** H1 (openingsdag 01-03-2026) and H2 (none); J (startdag 01-03-2026) with V1 (15-02-2026, haven H1), V2 (10-03-2026,
     *  H2), V3 (20-03-2026, no haven) and V4 (25-03-2026, H1). */
    private static final List<String> HAVENS = List.of(
            instance("H1", "Haven", "\"openingsdag\":{\"date\":\"2026-03-01\"}", ""), instance("H2", "Haven", "", ""),
            jutter("J", "\"startdag\":{\"date\":\"2026-03-01\"}"),
            instance("V1", "Vondst", "\"vinddag\":{\"date\":\"2026-02-15\"}", "\"jutter\":[\"J\"],\"haven\":[\"H1\"]"),
            instance("V2", "Vondst", "\"vinddag\":{\"date\":\"2026-03-10\"}", "\"jutter\":[\"J\"],\"haven\":[\"H2\"]"),
            instance("V3", "Vondst", "\"vinddag\":{\"date\":\"2026-03-20\"}", "\"jutter\":[\"J\"]"),
            instance("V4", "Vondst", "\"vinddag\":{\"date\":\"2026-03-25\"}", "\"jutter\":[\"J\"],\"haven\":[\"H1\"]"));

    /** P (startdag 01-03-2026) and J, J linked to P under {@code rol}, with the three vondsten of {@link #THREE_VONDSTEN}. */
    private static List<String> ploegVondsten(String rol) {
        return List.of(instance("P", "Ploeg", "\"startdag\":{\"date\":\"2026-03-01\"}", ""),
                instance("J", "Jutter", "", "\"" + rol + "\":[\"P\"]"), vondst("V1", "2026-02-15", 5, "J"),
                vondst("V2", "2026-03-10", 7, "J"), vondst("V3", "2026-03-20", 11, "J"));
    }

    private static String day(String label, String attribuut, String iso) {
        return "{\"kind\":\"attributeValue\",\"label\":\"" + label + "\",\"attribute\":\"" + attribuut + "\",\"value\":{\"date\":\""
                + iso + "\"}}";
    }

    private static String number(String label, String attribuut, int n) {
        return "{\"kind\":\"attributeValue\",\"label\":\"" + label + "\",\"attribute\":\"" + attribuut + "\",\"value\":{\"number\":\""
                + n + "\"}}";
    }

    private static String empty(String label, String attribuut) {
        return "{\"kind\":\"attributeEmpty\",\"label\":\"" + label + "\",\"attribute\":\"" + attribuut + "\"}";
    }

    private static String kenmerk(String label, String kenmerk, boolean aanwezig) {
        return "{\"kind\":\"" + (aanwezig ? "characteristicPresent" : "characteristicAbsent") + "\",\"label\":\"" + label
                + "\",\"characteristic\":\"" + kenmerk + "\"}";
    }

    private static String caseDocument(List<String> instanties, String verwacht) {
        return "{\"format\":\"uchar-case-v0\",\"asOfDate\":null,\"input\":{\"instances\":[" + String.join(",", instanties)
                + "],\"parameters\":{}},\"expected\":[" + verwacht + "]}";
    }

    /** The exec-model branch's verdict on one case: null when every expectation holds. */
    private static String play(String ir, String caseDocument) throws Exception {
        return play(ir, caseDocument, false);
    }

    /** The DRL-text branch's verdict on one case. */
    private static String playDrl(String ir, String caseDocument) throws Exception {
        return play(ir, caseDocument, true);
    }

    /** The exec-model branch's verdict on one case. */
    private static String playTms(String ir, String caseDocument) throws Exception {
        Path tmp = Files.createTempFile("vouwfilter-verre-kant", ".json");
        tmp.toFile().deleteOnExit();
        Files.writeString(tmp, ir);
        return CaseRunner.playCase(tmp, JSON.readTree(caseDocument), DroolsBackend.Branch.EXEC_MODEL);
    }

    private static String play(String ir, String caseDocument, boolean viaDrl) throws Exception {
        Path tmp = Files.createTempFile("vouwfilter-verre-kant", ".json");
        tmp.toFile().deleteOnExit();
        Files.writeString(tmp, ir);
        JsonNode c = JSON.readTree(caseDocument);
        return CaseRunner.playCase(tmp, c, viaDrl);
    }
}
