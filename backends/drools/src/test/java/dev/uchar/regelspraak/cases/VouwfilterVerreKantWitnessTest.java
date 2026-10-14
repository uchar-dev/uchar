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
class VouwfilterVerreKantWitnessTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    // ---------------- the subselection of a fold in a rule's value ----------------

    @Test void de_eerste_late_vinddag_na_de_sluitdag() throws Exception {
        // "De eerste late vinddag van een Jutter moet berekend worden als de eerste van de vinddag van alle vondsten van de
        // Jutter die aan de volgende voorwaarde voldoen: zijn vinddag is later dan de sluitdag van de jutter van de
        // Vondst", beside "De sluitdag van een Jutter moet gesteld worden op zijn startdag", in either order. ref and
        // clingo: sluitdag 01-03-2026, eerste late vinddag 10-03-2026.
        String eerste = regel("eerste late vinddag", "Jutter", "eerste late vinddag",
                vouw("first", eigen("vinddag", "Vondst"), LATER_DAN_DE_SLUITDAG, DATUM), null);
        String zaak = zaak(DRIE_VONDSTEN, dag("J", "sluitdag", "2026-03-01") + "," + dag("J", "eerste late vinddag", "2026-03-10"));
        assertNull(speel(document(List.of(SLUITDAG, eerste)), zaak), "de schrijver voor de vouw");
        assertNull(speel(document(List.of(eerste, SLUITDAG)), zaak), "de vouw voor de schrijver");
    }

    @Test void de_laatste_late_vinddag() throws Exception {
        // "de laatste van de vinddag van alle vondsten van de Jutter die …". ref and clingo: 20-03-2026.
        String laatste = regel("laatste late vinddag", "Jutter", "laatste late vinddag",
                vouw("last", eigen("vinddag", "Vondst"), LATER_DAN_DE_SLUITDAG, DATUM), null);
        assertNull(speel(document(List.of(SLUITDAG, laatste)),
                zaak(DRIE_VONDSTEN, dag("J", "laatste late vinddag", "2026-03-20"))), "de laatste");
    }

    @Test void het_aantal_late_vondsten() throws Exception {
        // "het aantal vondsten van de Jutter die …". ref and clingo: 2.
        assertNull(speel(document(List.of(SLUITDAG, aantal(LATER_DAN_DE_SLUITDAG))),
                zaak(DRIE_VONDSTEN, getal("J", "aantal late vondsten", 2))), "de telling");
    }

    @Test void de_som_en_het_maximum() throws Exception {
        // "de som van het gewicht van alle vondsten …" and "de maximale waarde van het gewicht van alle vondsten …".
        // ref and clingo: 18 and 11.
        String som = regel("late gewicht", "Jutter", "late gewicht",
                vouw("sum", eigen("gewicht", "Vondst"), LATER_DAN_DE_SLUITDAG, GEHEEL), null);
        String max = regel("zwaarste late gewicht", "Jutter", "zwaarste late gewicht",
                vouw("maximum", eigen("gewicht", "Vondst"), LATER_DAN_DE_SLUITDAG, GEHEEL), null);
        assertNull(speel(document(List.of(SLUITDAG, som, max)),
                zaak(DRIE_VONDSTEN, getal("J", "late gewicht", 18) + "," + getal("J", "zwaarste late gewicht", 11))),
                "de som en het maximum");
    }

    @Test void een_regel_die_het_element_bijwerkt() throws Exception {
        // "Een Vondst is gedateerd indien zijn vinddag gevuld is" declared before the rule sluitdag: it updates every
        // element before the sluitdag is written. Declared after it, it updated them after the write, and the form
        // played before the fix as well. ref and clingo: 10-03-2026 in both orders.
        String gedateerd = kenmerkRegel("gedateerd", "Vondst", predicaat("filled", eigen("vinddag", "Vondst")));
        String eerste = regel("eerste late vinddag", "Jutter", "eerste late vinddag",
                vouw("first", eigen("vinddag", "Vondst"), LATER_DAN_DE_SLUITDAG, DATUM), null);
        String zaak = zaak(DRIE_VONDSTEN, dag("J", "eerste late vinddag", "2026-03-10"));
        assertNull(speel(document(List.of(gedateerd, SLUITDAG, eerste)), zaak), "de bijwerking voor de schrijver");
        assertNull(speel(document(List.of(SLUITDAG, eerste, gedateerd)), zaak), "de bijwerking na de schrijver");
    }

    @Test void een_eigen_voorwaarde_op_het_onderwerp() throws Exception {
        // The same fold "indien zijn sluitdag gevuld is": the rule fires only after the writer. ref and clingo: 10-03-2026.
        String eerste = regel("eerste late vinddag", "Jutter", "eerste late vinddag",
                vouw("first", eigen("vinddag", "Vondst"), LATER_DAN_DE_SLUITDAG, DATUM),
                predicaat("filled", eigen("sluitdag", "Jutter")));
        assertNull(speel(document(List.of(SLUITDAG, eerste)),
                zaak(DRIE_VONDSTEN, dag("J", "eerste late vinddag", "2026-03-10"))), "de eigen voorwaarde");
    }

    @Test void een_kenmerk_van_het_verre_object() throws Exception {
        // "… die aan de volgende voorwaarde voldoen: zijn jutter actief is", and "… zijn jutter niet actief is", with
        // "Een Jutter is actief indien zijn sluitdag gevuld is". ref and clingo: J actief, 3 and 0.
        String actief = kenmerkRegel("actief", "Jutter", predicaat("filled", eigen("sluitdag", "Jutter")));
        String zaak3 = zaak(DRIE_VONDSTEN, kenmerk("J", "actief", true) + "," + getal("J", "aantal late vondsten", 3));
        String zaak0 = zaak(DRIE_VONDSTEN, kenmerk("J", "actief", true) + "," + getal("J", "aantal late vondsten", 0));
        assertNull(speel(document(List.of(aantal(de(kenmerkcheck("actief", false, JUTTER_VAN_DE_VONDST))), actief, SLUITDAG)),
                zaak3), "het kenmerk van de jutter");
        assertNull(speel(document(List.of(aantal(de(kenmerkcheck("actief", true, JUTTER_VAN_DE_VONDST))), actief, SLUITDAG)),
                zaak0), "het ontkende kenmerk van de jutter");
    }

    @Test void een_leegtoets_op_het_verre_lid() throws Exception {
        // "… die aan de volgende voorwaarde voldoen: de sluitdag van de jutter van de Vondst is leeg": the source empties
        // when the sluitdag is written. ref and clingo: 0.
        assertNull(speel(document(List.of(aantal(de(predicaat("empty", sluitdagVanDeJutter()))), SLUITDAG)),
                zaak(DRIE_VONDSTEN, getal("J", "aantal late vondsten", 0))), "de leeg-toets");
    }

    @Test void een_rol_die_niets_bereikt() throws Exception {
        // "… de afgeleide openingsdag van de haven van de Vondst is leeg" and "… is ongelijk aan 01-03-2026", with "De
        // afgeleide openingsdag van een Haven moet gesteld worden op zijn openingsdag": V2's haven has no openingsdag, V3
        // has no haven, so the far member reads leeg over an absent relation (ch. 1.2). ref and clingo: 2 and 2.
        String zaak = zaak(HAVENS, dag("H1", "afgeleide openingsdag", "2026-03-01") + "," + getal("J", "aantal late vondsten", 2));
        assertNull(speel(document(List.of(aantal(de(predicaat("empty", openingsdagVanDeHaven()))), AFGELEIDE_OPENINGSDAG)),
                zaak), "de leeg-toets over een lege rol");
        assertNull(speel(document(List.of(aantal(de(vergelijking("notEqual", openingsdagVanDeHaven(),
                datumLiteraal("2026-03-01")))), AFGELEIDE_OPENINGSDAG)), zaak), "ongelijk over een lege rol");
    }

    @Test void twee_verre_objecten() throws Exception {
        // "… die aan alle volgende voorwaarden voldoen: zijn vinddag is later dan de sluitdag van de jutter van de Vondst;
        // zijn vinddag is later dan de afgeleide openingsdag van de haven van de Vondst". ref and clingo: 1 (V4).
        String filter = alle(vergelijking("later", eigen("vinddag", "Vondst"), sluitdagVanDeJutter()),
                vergelijking("later", eigen("vinddag", "Vondst"), openingsdagVanDeHaven()));
        assertNull(speel(document(List.of(aantal(filter), AFGELEIDE_OPENINGSDAG, SLUITDAG)),
                zaak(HAVENS, getal("J", "aantal late vondsten", 1))), "twee verre objecten");
    }

    @Test void een_datumrekensom_met_het_verre_lid() throws Exception {
        // "… die aan de volgende voorwaarde voldoen: zijn vinddag min 7 dg is later dan de sluitdag van de jutter van de
        // Vondst". Refused by name before the fix. ref and clingo: 10-03-2026.
        String filter = de(vergelijking("later", datumMin(eigen("vinddag", "Vondst"), 7), sluitdagVanDeJutter()));
        String eerste = regel("eerste late vinddag", "Jutter", "eerste late vinddag",
                vouw("first", eigen("vinddag", "Vondst"), filter, DATUM), null);
        assertNull(speel(document(List.of(eerste, SLUITDAG)),
                zaak(DRIE_VONDSTEN, dag("J", "eerste late vinddag", "2026-03-10"))), "de datumrekensom");
    }

    @Test void een_keten_van_twee_rollen() throws Exception {
        // "… zijn vinddag is later dan de sluitdag van de thuisploeg van de jutter van de Vondst", the sluitdag of the
        // Ploeg by a rule; once with the thuisploeg in the case, once laid during the run by the fact creation "Een lid
        // van een Ploeg is een geworven jutter van de Ploeg". Refused by name before the fix. ref and clingo: 10-03-2026.
        String filter = de(vergelijking("later", eigen("vinddag", "Vondst"), genavigeerd("sluitdag", "Ploeg",
                "de sluitdag van de thuisploeg van de jutter van de Vondst", rolbinding("JutterHeeftVondst", "jutter"),
                rolbinding("PloegHeeftJutter", "thuisploeg"))));
        String eerste = regel("eerste late vinddag", "Jutter", "eerste late vinddag",
                vouw("first", eigen("vinddag", "Vondst"), filter, DATUM), null);
        String verwacht = dag("P", "sluitdag", "2026-03-01") + "," + dag("J", "eerste late vinddag", "2026-03-10");
        assertNull(speel(document(List.of(eerste, PLOEGSLUITDAG)), zaak(ploegVondsten("thuisploeg"), verwacht)),
                "de thuisploeg uit de case");
        assertNull(speel(document(List.of(eerste, PLOEGSLUITDAG), List.of(), List.of(), List.of(WERVING), List.of()),
                zaak(ploegVondsten("wervende ploeg"), verwacht)), "de thuisploeg uit de werving");
    }

    // ---------------- other writers of the far member ----------------

    @Test void het_verre_lid_uit_een_defaultsgroep() throws Exception {
        // The sluitdag by a group: "Regel sluitdag" and "Afwijking van sluitdag: opgave gaat voor" (the opgegeven sluitdag
        // where it is filled). ref and clingo: J (opgave 12-03-2026) 20-03-2026, K 10-03-2026.
        String eerste = regel("eerste late vinddag", "Jutter", "eerste late vinddag",
                vouw("first", eigen("vinddag", "Vondst"), LATER_DAN_DE_SLUITDAG, DATUM), null);
        String zaak = zaak(List.of(jutter("J", "\"startdag\":{\"date\":\"2026-03-01\"},\"opgegeven sluitdag\":{\"date\":\"2026-03-12\"}"),
                        jutter("K", "\"startdag\":{\"date\":\"2026-03-01\"}"), vondst("V1", "2026-02-15", 5, "J"),
                        vondst("V2", "2026-03-10", 7, "J"), vondst("V3", "2026-03-20", 11, "J"), vondst("W1", "2026-03-10", 1, "K"),
                        vondst("W2", "2026-03-20", 1, "K")),
                dag("J", "eerste late vinddag", "2026-03-20") + "," + dag("K", "eerste late vinddag", "2026-03-10"));
        assertNull(speel(document(List.of(eerste), List.of(), List.of(), List.of(), List.of(GROEP_SLUITDAG)), zaak),
                "de sluitdag uit een defaultsgroep");
    }

    @Test void het_verre_lid_uit_een_verdeling() throws Exception {
        // "… het bergingsaandeel van de vondstberger van de Vondst is groter dan 10", the aandeel from "De opbrengst van een
        // berging wordt verdeeld over de bergingsaandelen van alle bergers van de berging, in gelijke delen". ref and
        // clingo: B1 20, B3 10, J 1.
        String filter = de(vergelijking("greater", genavigeerd("bergingsaandeel", "Berger",
                "het bergingsaandeel van de vondstberger van de Vondst", rolbinding("BergerVanVondst", "vondstberger")),
                getalLiteraal(10)));
        String zaak = zaak(List.of(instantie("B", "Berging", "\"opbrengst\":{\"number\":\"40\"}", ""),
                        instantie("C", "Berging", "\"opbrengst\":{\"number\":\"10\"}", ""),
                        instantie("B1", "Berger", "", "\"berging\":[\"B\"]"), instantie("B2", "Berger", "", "\"berging\":[\"B\"]"),
                        instantie("B3", "Berger", "", "\"berging\":[\"C\"]"), jutter("J", ""),
                        instantie("V1", "Vondst", "", "\"jutter\":[\"J\"],\"vondstberger\":[\"B1\"]"),
                        instantie("V2", "Vondst", "", "\"jutter\":[\"J\"],\"vondstberger\":[\"B3\"]"),
                        instantie("V3", "Vondst", "", "\"jutter\":[\"J\"]")),
                getal("B1", "bergingsaandeel", 20) + "," + getal("B3", "bergingsaandeel", 10) + ","
                        + getal("J", "aantal late vondsten", 1));
        assertNull(speel(document(List.of(aantal(filter)), List.of(), List.of(VERDELING), List.of(), List.of()), zaak),
                "het aandeel uit een verdeling");
    }

    @Test void het_verre_object_uit_een_creatie() throws Exception {
        // "… de meldingsdag van de melding van de Vondst is later dan 01-03-2026", the melding from "Een Vondst heeft een
        // melding met meldingsdag gelijk aan zijn vinddag indien zijn vinddag gevuld is". The creation links the melding
        // to the element and updates it, so the form played before the fix as well. ref and clingo: 2.
        String filter = de(vergelijking("later", genavigeerd("meldingsdag", "Melding", "de meldingsdag van de melding van de Vondst",
                rolbinding("VondstHeeftMelding", "melding")), datumLiteraal("2026-03-01")));
        String zaak = zaak(List.of(jutter("J", ""), vondst("V1", "2026-02-15", 5, "J"), vondst("V2", "2026-03-10", 7, "J"),
                        vondst("V3", "2026-03-20", 11, "J"), instantie("V4", "Vondst", "", "\"jutter\":[\"J\"]")),
                getal("J", "aantal late vondsten", 2));
        assertNull(speel(document(List.of(aantal(filter)), List.of(MELDING), List.of(), List.of(), List.of()), zaak),
                "de melding uit een creatie");
    }

    // ---------------- a fold in a condition, and the matrix of a quantifier fold ----------------

    @Test void de_telling_in_een_voorwaarde() throws Exception {
        // "De laatklasse van een Jutter moet berekend worden als 1 indien het aantal vondsten van de Jutter die aan de
        // volgende voorwaarde voldoen: zijn vinddag is later dan de sluitdag van de jutter van de Vondst groter is dan 0".
        // ref and clingo: 1.
        String laatklasse = regel("laatklasse", "Jutter", "laatklasse", getalLiteraal(1),
                vergelijking("greater", telling(LATER_DAN_DE_SLUITDAG), getalLiteraal(0)));
        assertNull(speel(document(List.of(laatklasse, SLUITDAG)), zaak(DRIE_VONDSTEN, getal("J", "laatklasse", 1))),
                "de telling in de voorwaarde");
    }

    @Test void de_kwantor_in_een_voorwaarde() throws Exception {
        // "… indien de vinddag van ten minste één van de vondsten van de Jutter die aan de volgende voorwaarde voldoen:
        // zijn vinddag is later dan de sluitdag van de jutter van de Vondst gevuld is". ref and clingo: 1.
        String laatklasse = regel("laatklasse", "Jutter", "laatklasse", getalLiteraal(1),
                kwantorvouw("atLeast", 1, LATER_DAN_DE_SLUITDAG, predicaat("filled", eigen("vinddag", "Vondst"))));
        assertNull(speel(document(List.of(laatklasse, SLUITDAG)), zaak(DRIE_VONDSTEN, getal("J", "laatklasse", 1))),
                "de kwantor in de voorwaarde");
    }

    @Test void een_leegtoets_in_een_voorwaardevouw() throws Exception {
        // "… 1 indien het aantal vondsten van de Jutter die aan de volgende voorwaarde voldoen: de sluitdag van de jutter
        // van de Vondst is leeg groter is dan 0": true only on the transient leeg. ref and clingo: leeg.
        String laatklasse = regel("laatklasse", "Jutter", "laatklasse", getalLiteraal(1),
                vergelijking("greater", telling(de(predicaat("empty", sluitdagVanDeJutter()))), getalLiteraal(0)));
        assertNull(speel(document(List.of(laatklasse, SLUITDAG)), zaak(DRIE_VONDSTEN, leeg("J", "laatklasse"))),
                "de leeg-toets in de voorwaardevouw");
    }

    @Test void de_matrix_van_een_kwantor() throws Exception {
        // "… indien de vinddag van ten minste één van de vondsten van de Jutter later is dan de afgeleide openingsdag van
        // zijn haven" and the same under geen: the matrix reads through the haven of the element, not the subject. ref and
        // clingo: 1 under ten minste één, leeg under geen.
        String matrix = vergelijking("later", eigen("vinddag", "Vondst"), genavigeerd("afgeleide openingsdag", "Haven",
                "de afgeleide openingsdag van zijn haven", rolbinding("HavenVanVondst", "haven")));
        String tenMinste = regel("laatklasse", "Jutter", "laatklasse", getalLiteraal(1), kwantorvouw("atLeast", 1, null, matrix));
        String geen = regel("laatklasse", "Jutter", "laatklasse", getalLiteraal(1), kwantorvouw("none", 0, null, matrix));
        assertNull(speel(document(List.of(tenMinste, AFGELEIDE_OPENINGSDAG)), zaak(HAVENS, getal("J", "laatklasse", 1))),
                "de matrix onder ten minste één");
        assertNull(speel(document(List.of(geen, AFGELEIDE_OPENINGSDAG)), zaak(HAVENS, leeg("J", "laatklasse"))),
                "de matrix onder geen");
    }

    @Test void het_filter_van_een_kwantor_via_een_andere_rol() throws Exception {
        // "… indien de vinddag van ten minste één van de vondsten van de Jutter die aan de volgende voorwaarde voldoen: zijn
        // vinddag is later dan de afgeleide openingsdag van de haven van de Vondst gevuld is". ref and clingo: 1.
        String laatklasse = regel("laatklasse", "Jutter", "laatklasse", getalLiteraal(1), kwantorvouw("atLeast", 1,
                de(vergelijking("later", eigen("vinddag", "Vondst"), openingsdagVanDeHaven())),
                predicaat("filled", eigen("vinddag", "Vondst"))));
        assertNull(speel(document(List.of(laatklasse, AFGELEIDE_OPENINGSDAG)), zaak(HAVENS, getal("J", "laatklasse", 1))),
                "het kwantorfilter via de haven");
    }

    // ---------------- flanks: forms that played before ----------------

    @Test void vormen_die_al_speelden() throws Exception {
        // The sluitdag in the case input; a literal bound "later dan 01-03-2026"; a characteristic on the element ("Een
        // Vondst is laat indien zijn vinddag later is dan de sluitdag van de jutter van de Vondst", the filter "hij is
        // laat"). ref and clingo: 10-03-2026 in all three.
        String eerste = regel("eerste late vinddag", "Jutter", "eerste late vinddag",
                vouw("first", eigen("vinddag", "Vondst"), LATER_DAN_DE_SLUITDAG, DATUM), null);
        assertNull(speel(document(List.of(eerste)), zaak(List.of(jutter("J", "\"sluitdag\":{\"date\":\"2026-03-01\"}"),
                vondst("V1", "2026-02-15", 5, "J"), vondst("V2", "2026-03-10", 7, "J"), vondst("V3", "2026-03-20", 11, "J")),
                dag("J", "eerste late vinddag", "2026-03-10"))), "de sluitdag in de invoer");
        String letterlijk = regel("eerste late vinddag", "Jutter", "eerste late vinddag", vouw("first", eigen("vinddag", "Vondst"),
                de(vergelijking("later", eigen("vinddag", "Vondst"), datumLiteraal("2026-03-01"))), DATUM), null);
        assertNull(speel(document(List.of(SLUITDAG, letterlijk)), zaak(DRIE_VONDSTEN, dag("J", "eerste late vinddag", "2026-03-10"))),
                "een letterlijke grens");
        String laat = kenmerkRegel("laat", "Vondst", vergelijking("later", eigen("vinddag", "Vondst"), sluitdagVanDeJutter()));
        String brug = regel("eerste late vinddag", "Jutter", "eerste late vinddag",
                vouw("first", eigen("vinddag", "Vondst"), de(kenmerkcheck("laat", false, null)), DATUM), null);
        assertNull(speel(document(List.of(SLUITDAG, laat, brug)), zaak(DRIE_VONDSTEN, dag("J", "eerste late vinddag", "2026-03-10"))),
                "een kenmerk op het element");
    }

    // ---------------- forms that refuse by name ----------------

    @Test void een_unieselectie_via_een_rol_speelt_met_truth_maintenance() throws Exception {
        // "het aantal vondsten van de Jutter die gedateerd zijn en vondsten van de Jutter wier jutter actief is": a union
        // selection whose subselection reads through a role of the element. The selections share one element pattern
        // and its test, so the destructive exec-model branch had no join for it and refused the form; before, it
        // counted with the characteristic as it stood at insertion. The exec-model branch with truth maintenance plays
        // it as ref answers, 3, on this case (TmsZoalsRefWitnessTest, unieselectie-via-rol).
        String unie = "{\"node\":\"Aggregation\",\"kind\":\"count\",\"arguments\":[{\"node\":\"InstanceSetUnion\",\"branches\":["
                + tak(de(kenmerkcheck("gedateerd", false, null))) + "," + tak(de(kenmerkcheck("actief", false, JUTTER_VAN_DE_VONDST)))
                + "],\"source\":{\"snippet\":\"vondsten die gedateerd zijn en vondsten wier jutter actief is\"}}],\"type\":" + TELGETAL
                + ",\"source\":{\"snippet\":\"het aantal vondsten die gedateerd zijn en vondsten wier jutter actief is\"}}";
        String actief = kenmerkRegel("actief", "Jutter", predicaat("filled", eigen("sluitdag", "Jutter")));
        String ir = document(List.of(regel("aantal late vondsten", "Jutter", "aantal late vondsten", unie, null), actief, SLUITDAG));
        assertNull(speelTms(ir, zaak(DRIE_VONDSTEN, getal("J", "aantal late vondsten", 3))), "ref agrees: 3");
    }

    @Test void de_teksttak_speelt_een_toets_via_de_rol_van_het_element() throws Exception {
        // The destructive DRL-text lowering wrote the subselection and the matrix into the element pattern, where a
        // later write on the far object is not seen (before: verwacht 2, werkelijk 0; verwacht 1, werkelijk leeg), and
        // refused them by name ("via een rol van het element") until card drl-teksttak-op-truth-maintenance; since then
        // the text renders the analysis, which binds the far object's standing, and plays all three forms.
        String laatklasse = regel("laatklasse", "Jutter", "laatklasse", getalLiteraal(1),
                vergelijking("greater", telling(LATER_DAN_DE_SLUITDAG), getalLiteraal(0)));
        String matrix = regel("laatklasse", "Jutter", "laatklasse", getalLiteraal(1), kwantorvouw("atLeast", 1, null,
                vergelijking("later", eigen("vinddag", "Vondst"), genavigeerd("afgeleide openingsdag", "Haven",
                        "de afgeleide openingsdag van zijn haven", rolbinding("HavenVanVondst", "haven")))));
        List<String[]> vormen = List.of(
                new String[] { document(List.of(SLUITDAG, aantal(LATER_DAN_DE_SLUITDAG))),
                        zaak(DRIE_VONDSTEN, getal("J", "aantal late vondsten", 2)) },
                new String[] { document(List.of(laatklasse, SLUITDAG)), zaak(DRIE_VONDSTEN, getal("J", "laatklasse", 1)) },
                new String[] { document(List.of(matrix, AFGELEIDE_OPENINGSDAG)), zaak(HAVENS, getal("J", "laatklasse", 1)) });
        for (String[] v : vormen) {
            assertNull(speel(v[0], v[1]), "de exectak speelt");
            assertNull(speelDrl(v[0], v[1]), "de teksttak speelt");
        }
    }

    // ---------------- documents ----------------

    private static final String DATUM = "{\"node\":\"Date\",\"precision\":\"day\"}";
    private static final String GEHEEL = "{\"node\":\"Number\",\"whole\":true,\"decimals\":0,\"sign\":\"unspecified\"}";
    private static final String TELGETAL = "{\"node\":\"Number\",\"whole\":true,\"decimals\":0,\"sign\":\"nonNegative\"}";
    private static final String DAG = "{\"node\":\"Unit\",\"abbreviation\":\"dg\",\"system\":\"Tijd\",\"canonical\":\"dg\","
            + "\"factorToCanonical\":{\"num\":1,\"den\":1}}";

    private static String attribuut(String naam, String datatype) {
        return "{\"node\":\"Attribute\",\"name\":\"" + naam + "\",\"datatype\":" + datatype + "}";
    }

    private static String attributen(String datatype, String... namen) {
        List<String> a = new ArrayList<>();
        for (String n : namen) a.add(attribuut(n, datatype));
        return String.join(",", a);
    }

    private static String objecttype(String naam, String meervoud, boolean bezield, String kenmerken, String attributen) {
        return "{\"node\":\"ObjectType\",\"name\":\"" + naam + "\",\"animate\":" + bezield + ",\"plural\":\"" + meervoud
                + "\",\"attributes\":[" + attributen + "],\"characteristics\":[" + kenmerken + "]}";
    }

    private static String kenmerken(String... namen) {
        List<String> k = new ArrayList<>();
        for (String n : namen) k.add("{\"node\":\"Characteristic\",\"name\":\"" + n + "\",\"kind\":\"adjectival\"}");
        return String.join(",", k);
    }

    /** A fact type between one {@code eenType} and one ({@code veelMeervoud} null) or many {@code veelType}. */
    private static String feittype(String naam, String eenRol, String eenType, String veelRol, String veelMeervoud, String veelType) {
        return "{\"node\":\"FactType\",\"name\":\"" + naam + "\",\"reciprocal\":false,\"roles\":["
                + "{\"node\":\"Role\",\"name\":\"" + eenRol + "\",\"objectType\":\"" + eenType + "\",\"many\":false},"
                + "{\"node\":\"Role\",\"name\":\"" + veelRol + "\",\"objectType\":\"" + veelType + "\""
                + (veelMeervoud == null ? ",\"many\":false}" : ",\"plural\":\"" + veelMeervoud + "\",\"many\":true}") + "]}";
    }

    /** Ploeg, Jutter, Vondst, Haven, Melding, Berging and Berger. A Jutter has many vondsten, one thuisploeg and one
     *  wervende ploeg; a Vondst one haven, one melding and one vondstberger; a Berging many bergers. */
    private static final String MODEL = "{\"objectTypes\":["
            + objecttype("Ploeg", "Ploegen", false, "", attributen(DATUM, "startdag", "sluitdag")) + ","
            + objecttype("Jutter", "Jutters", true, kenmerken("actief"),
                    attributen(DATUM, "startdag", "opgegeven sluitdag", "sluitdag", "eerste late vinddag", "laatste late vinddag")
                    + "," + attributen(GEHEEL, "aantal late vondsten", "late gewicht", "zwaarste late gewicht", "laatklasse")) + ","
            + objecttype("Vondst", "Vondsten", false, kenmerken("gedateerd", "laat"),
                    attribuut("vinddag", DATUM) + "," + attribuut("gewicht", GEHEEL)) + ","
            + objecttype("Haven", "Havens", false, "", attributen(DATUM, "openingsdag", "afgeleide openingsdag")) + ","
            + objecttype("Melding", "Meldingen", false, "", attribuut("meldingsdag", DATUM)) + ","
            + objecttype("Berging", "Bergingen", false, "", attributen(GEHEEL, "opbrengst", "onverdeelde rest")) + ","
            + objecttype("Berger", "Bergers", true, "", attribuut("bergingsaandeel", GEHEEL))
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

    private static String rolbinding(String feittype, String rol) {
        return "{\"node\":\"RoleBinding\",\"factType\":\"" + feittype + "\",\"role\":\"" + rol + "\"}";
    }

    private static final String JUTTER_VAN_DE_VONDST = rolbinding("JutterHeeftVondst", "jutter");

    private static String eigen(String attribuut, String type) {
        return "{\"node\":\"AttributeReference\",\"attribute\":\"" + attribuut + "\",\"subjectType\":\"" + type
                + "\",\"source\":{\"snippet\":\"zijn " + attribuut + "\"}}";
    }

    /** A read of {@code attribuut} on {@code type} over the role hops {@code pad}. */
    private static String genavigeerd(String attribuut, String type, String snippet, String... pad) {
        return "{\"node\":\"AttributeReference\",\"attribute\":\"" + attribuut + "\",\"subjectType\":\"" + type
                + "\",\"source\":{\"snippet\":\"" + snippet + "\"},\"path\":[" + String.join(",", pad) + "]}";
    }

    /** "de sluitdag van de jutter van de Vondst". */
    private static String sluitdagVanDeJutter() {
        return genavigeerd("sluitdag", "Jutter", "de sluitdag van de jutter van de Vondst", JUTTER_VAN_DE_VONDST);
    }

    /** "de afgeleide openingsdag van de haven van de Vondst". */
    private static String openingsdagVanDeHaven() {
        return genavigeerd("afgeleide openingsdag", "Haven", "de afgeleide openingsdag van de haven van de Vondst",
                rolbinding("HavenVanVondst", "haven"));
    }

    private static String datumLiteraal(String iso) {
        return "{\"node\":\"Literal\",\"value\":{\"date\":\"" + iso + "\"},\"type\":" + DATUM + ",\"source\":{\"snippet\":\"" + iso + "\"}}";
    }

    private static String getalLiteraal(int n) {
        return "{\"node\":\"Literal\",\"value\":{\"number\":\"" + n + "\"},\"type\":" + GEHEEL + ",\"source\":{\"snippet\":\"" + n + "\"}}";
    }

    /** "&lt;datum&gt; min &lt;n&gt; dg". */
    private static String datumMin(String datum, int dagen) {
        return "{\"node\":\"Binary\",\"op\":\"minus\",\"left\":" + datum + ",\"right\":{\"node\":\"Literal\",\"value\":{\"number\":\""
                + dagen + "\"},\"type\":{\"node\":\"Number\",\"whole\":true,\"decimals\":0,\"sign\":\"unspecified\",\"unit\":" + DAG
                + "},\"source\":{\"snippet\":\"" + dagen + " dg\"}},\"type\":" + DATUM + ",\"source\":{\"snippet\":\"min " + dagen + " dg\"}}";
    }

    private static String vergelijking(String op, String links, String rechts) {
        return "{\"node\":\"Comparison\",\"op\":\"" + op + "\",\"left\":" + links + ",\"right\":" + rechts
                + ",\"source\":{\"snippet\":\"vergelijking " + op + "\"}}";
    }

    private static String predicaat(String soort, String operand) {
        return "{\"node\":\"Predicate\",\"kind\":\"" + soort + "\",\"operand\":" + operand
                + ",\"negated\":false,\"digits\":0,\"source\":{\"snippet\":\"" + soort + "\"}}";
    }

    /** A characteristic check, on the subject itself ({@code navigatie} null) or over one role. */
    private static String kenmerkcheck(String kenmerk, boolean ontkend, String navigatie) {
        return "{\"node\":\"CharacteristicCheck\",\"characteristic\":\"" + kenmerk + "\",\"negated\":" + ontkend
                + ",\"source\":{\"snippet\":\"" + kenmerk + "\"}" + (navigatie == null ? "" : ",\"navigation\":" + navigatie) + "}";
    }

    /** "aan de volgende voorwaarde voldoen: &lt;deel&gt;". */
    private static String de(String deel) {
        return "{\"node\":\"Compound\",\"quantifier\":\"the\",\"threshold\":0,\"parts\":[" + deel
                + "],\"source\":{\"snippet\":\"aan de volgende voorwaarde voldoen\"}}";
    }

    /** "aan alle volgende voorwaarden voldoen: &lt;delen&gt;". */
    private static String alle(String... delen) {
        return "{\"node\":\"Compound\",\"quantifier\":\"all\",\"threshold\":0,\"parts\":[" + String.join(",", delen)
                + "],\"source\":{\"snippet\":\"aan alle volgende voorwaarden voldoen\"}}";
    }

    private static final String LATER_DAN_DE_SLUITDAG =
            de(vergelijking("later", eigen("vinddag", "Vondst"), sluitdagVanDeJutter()));

    /** "de &lt;soort&gt; van de &lt;argument&gt; van alle vondsten van de Jutter die &lt;filter&gt;". */
    private static String vouw(String soort, String argument, String filter, String type) {
        return "{\"node\":\"Aggregation\",\"kind\":\"" + soort + "\",\"arguments\":[" + argument + "],\"elementType\":\"Vondst\","
                + "\"filter\":" + filter + ",\"sourceRole\":" + rolbinding("JutterHeeftVondst", "vondst") + ",\"type\":" + type
                + ",\"source\":{\"snippet\":\"" + soort + " van alle vondsten van de Jutter die …\"}}";
    }

    /** "het aantal vondsten van de Jutter die &lt;filter&gt;". */
    private static String telling(String filter) {
        return vouw("count", "{\"node\":\"AttributeReference\",\"attribute\":\"vondsten\",\"subjectType\":\"Jutter\","
                + "\"source\":{\"snippet\":\"vondsten van de Jutter\"}}", filter, TELGETAL);
    }

    /** "Het aantal late vondsten van een Jutter moet berekend worden als het aantal vondsten van de Jutter die &lt;filter&gt;." */
    private static String aantal(String filter) {
        return regel("aantal late vondsten", "Jutter", "aantal late vondsten", telling(filter), null);
    }

    /** A quantifier fold over the vondsten of the Jutter; {@code filter} null for none, {@code drempel} 0 under geen. */
    private static String kwantorvouw(String kwantor, int drempel, String filter, String matrix) {
        return "{\"node\":\"QuantifierFold\",\"quantifier\":\"" + kwantor + "\"" + (drempel == 0 ? "" : ",\"threshold\":" + drempel)
                + ",\"elementType\":\"Vondst\"" + (filter == null ? "" : ",\"filter\":" + filter) + ",\"test\":" + matrix
                + ",\"source\":{\"snippet\":\"" + kwantor + " van de vondsten van de Jutter\"},\"sourceRole\":"
                + rolbinding("JutterHeeftVondst", "vondst") + "}";
    }

    /** One selection of an instance-set union over the vondsten of the Jutter. */
    private static String tak(String filter) {
        return "{\"node\":\"InstanceSetBranch\",\"elementType\":\"Vondst\",\"sourceRole\":" + rolbinding("JutterHeeftVondst", "vondst")
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
    private static String kenmerkRegel(String kenmerk, String type, String voorwaarde) {
        return "{\"node\":\"Rule\",\"name\":\"" + kenmerk + "\",\"versions\":[{\"validity\":{\"node\":\"Validity\"},\"target\":"
                + "{\"node\":\"AttributeReference\",\"attribute\":\"" + kenmerk + "\",\"subjectType\":\"" + type + "\",\"source\":"
                + "{\"snippet\":\"Een " + type + " is " + kenmerk + "\"}},\"value\":{\"node\":\"Literal\",\"value\":{\"boolean\":true},"
                + "\"type\":{\"node\":\"Boolean\"},\"source\":{\"snippet\":\"waar\"}},\"condition\":" + voorwaarde
                + ",\"initialisation\":false}]}";
    }

    /** "De sluitdag van een Jutter moet gesteld worden op zijn startdag." */
    private static final String SLUITDAG = regel("sluitdag", "Jutter", "sluitdag", eigen("startdag", "Jutter"), null);

    /** "De sluitdag van een Ploeg moet gesteld worden op zijn startdag." */
    private static final String PLOEGSLUITDAG = regel("sluitdag van de ploeg", "Ploeg", "sluitdag", eigen("startdag", "Ploeg"), null);

    /** "De afgeleide openingsdag van een Haven moet gesteld worden op zijn openingsdag." */
    private static final String AFGELEIDE_OPENINGSDAG = regel("afgeleide openingsdag", "Haven", "afgeleide openingsdag",
            eigen("openingsdag", "Haven"), null);

    /** "Regel sluitdag" with "Afwijking van sluitdag: opgave gaat voor": the opgegeven sluitdag where it is filled. */
    private static final String GROEP_SLUITDAG = "{\"node\":\"PrioritizedDefaults\",\"name\":\"sluitdag\",\"subject\":\"Jutter\","
            + "\"attribute\":\"sluitdag\",\"defaults\":[{\"node\":\"Default\",\"justification\":"
            + predicaat("filled", eigen("opgegeven sluitdag", "Jutter")) + ",\"consequence\":" + eigen("opgegeven sluitdag", "Jutter")
            + "},{\"node\":\"Default\",\"justification\":{\"node\":\"Compound\",\"quantifier\":\"all\",\"threshold\":0,\"parts\":[],"
            + "\"source\":{\"snippet\":\"altijd\"}},\"consequence\":" + eigen("startdag", "Jutter") + "}],\"validity\":{\"node\":\"Validity\"}}";

    /** "De opbrengst van een berging wordt verdeeld over de bergingsaandelen van alle bergers van de berging, in gelijke
     *  delen, afgerond op 0 decimalen naar beneden; als onverdeelde rest blijft de onverdeelde rest van de berging over." */
    private static final String VERDELING = "{\"node\":\"Allocation\",\"name\":\"verdeel de opbrengst van een berging\",\"versions\":["
            + "{\"sourceType\":\"Berging\",\"sourceAttribute\":\"opbrengst\",\"targetType\":\"Berger\",\"targetAttribute\":"
            + "\"bergingsaandeel\",\"method\":\"equalShares\",\"roundingDecimals\":0,\"remainderAttribute\":\"onverdeelde rest\","
            + "\"descending\":false,\"validity\":{\"node\":\"Validity\"},\"connectingRole\":" + rolbinding("bergingsdeelname", "berger") + "}]}";

    /** "Een Vondst heeft een melding met meldingsdag gelijk aan zijn vinddag indien zijn vinddag gevuld is." */
    private static final String MELDING = "{\"node\":\"ObjectCreation\",\"name\":\"maak een melding\",\"versions\":[{\"createdType\":"
            + "\"Melding\",\"subject\":\"Vondst\",\"initialisations\":[{\"node\":\"MemberInit\",\"member\":\"meldingsdag\",\"value\":"
            + eigen("vinddag", "Vondst") + ",\"characteristic\":false}],\"condition\":" + predicaat("filled", eigen("vinddag", "Vondst"))
            + ",\"validity\":{\"node\":\"Validity\"},\"connectingRole\":" + rolbinding("VondstHeeftMelding", "melding") + "}]}";

    /** "Een lid van een Ploeg is een geworven jutter van de Ploeg." */
    private static final String WERVING = "{\"node\":\"FactCreation\",\"name\":\"werving\",\"versions\":[{\"factType\":"
            + "\"PloegHeeftJutter\",\"anchorRole\":\"thuisploeg\",\"targetRole\":\"lid\",\"anchor\":\"Ploeg\",\"path\":["
            + rolbinding("PloegWerftJutter", "geworven jutter") + "],\"validity\":{\"node\":\"Validity\"}}]}";

    private static String document(List<String> regels) {
        return document(regels, List.of(), List.of(), List.of(), List.of());
    }

    private static String document(List<String> regels, List<String> creaties, List<String> verdelingen,
                                   List<String> feitcreaties, List<String> groepen) {
        return "{\"format\":\"uchar-ir-v1\",\"model\":" + MODEL + ",\"rules\":{\"rules\":[" + String.join(",", regels) + "],"
                + "\"dayKindDefinitions\":[],\"objectCreations\":[" + String.join(",", creaties) + "],\"allocations\":["
                + String.join(",", verdelingen) + "],\"consistencies\":[],\"uniquenesses\":[],\"factCreations\":["
                + String.join(",", feitcreaties) + "],\"startPointDeterminations\":[],\"ruleGroups\":[],\"prioritizedDefaults\":["
                + String.join(",", groepen) + "]},\"diagnostics\":[]}";
    }

    // ---------------- cases ----------------

    private static String instantie(String label, String type, String attributen, String rollen) {
        return "{\"label\":\"" + label + "\",\"objectType\":\"" + type + "\",\"attributes\":{" + attributen + "},\"roles\":{"
                + rollen + "},\"characteristics\":[]}";
    }

    private static String jutter(String label, String attributen) {
        return instantie(label, "Jutter", attributen, "");
    }

    private static String vondst(String label, String vinddag, int gewicht, String jutter) {
        return instantie(label, "Vondst", "\"vinddag\":{\"date\":\"" + vinddag + "\"},\"gewicht\":{\"number\":\"" + gewicht + "\"}",
                "\"jutter\":[\"" + jutter + "\"]");
    }

    /** J (startdag 01-03-2026) with V1 (15-02-2026, 5), V2 (10-03-2026, 7) and V3 (20-03-2026, 11). */
    private static final List<String> DRIE_VONDSTEN = List.of(jutter("J", "\"startdag\":{\"date\":\"2026-03-01\"}"),
            vondst("V1", "2026-02-15", 5, "J"), vondst("V2", "2026-03-10", 7, "J"), vondst("V3", "2026-03-20", 11, "J"));

    /** H1 (openingsdag 01-03-2026) and H2 (none); J (startdag 01-03-2026) with V1 (15-02-2026, haven H1), V2 (10-03-2026,
     *  H2), V3 (20-03-2026, no haven) and V4 (25-03-2026, H1). */
    private static final List<String> HAVENS = List.of(
            instantie("H1", "Haven", "\"openingsdag\":{\"date\":\"2026-03-01\"}", ""), instantie("H2", "Haven", "", ""),
            jutter("J", "\"startdag\":{\"date\":\"2026-03-01\"}"),
            instantie("V1", "Vondst", "\"vinddag\":{\"date\":\"2026-02-15\"}", "\"jutter\":[\"J\"],\"haven\":[\"H1\"]"),
            instantie("V2", "Vondst", "\"vinddag\":{\"date\":\"2026-03-10\"}", "\"jutter\":[\"J\"],\"haven\":[\"H2\"]"),
            instantie("V3", "Vondst", "\"vinddag\":{\"date\":\"2026-03-20\"}", "\"jutter\":[\"J\"]"),
            instantie("V4", "Vondst", "\"vinddag\":{\"date\":\"2026-03-25\"}", "\"jutter\":[\"J\"],\"haven\":[\"H1\"]"));

    /** P (startdag 01-03-2026) and J, J linked to P under {@code rol}, with the three vondsten of {@link #DRIE_VONDSTEN}. */
    private static List<String> ploegVondsten(String rol) {
        return List.of(instantie("P", "Ploeg", "\"startdag\":{\"date\":\"2026-03-01\"}", ""),
                instantie("J", "Jutter", "", "\"" + rol + "\":[\"P\"]"), vondst("V1", "2026-02-15", 5, "J"),
                vondst("V2", "2026-03-10", 7, "J"), vondst("V3", "2026-03-20", 11, "J"));
    }

    private static String dag(String label, String attribuut, String iso) {
        return "{\"kind\":\"attributeValue\",\"label\":\"" + label + "\",\"attribute\":\"" + attribuut + "\",\"value\":{\"date\":\""
                + iso + "\"}}";
    }

    private static String getal(String label, String attribuut, int n) {
        return "{\"kind\":\"attributeValue\",\"label\":\"" + label + "\",\"attribute\":\"" + attribuut + "\",\"value\":{\"number\":\""
                + n + "\"}}";
    }

    private static String leeg(String label, String attribuut) {
        return "{\"kind\":\"attributeEmpty\",\"label\":\"" + label + "\",\"attribute\":\"" + attribuut + "\"}";
    }

    private static String kenmerk(String label, String kenmerk, boolean aanwezig) {
        return "{\"kind\":\"" + (aanwezig ? "characteristicPresent" : "characteristicAbsent") + "\",\"label\":\"" + label
                + "\",\"characteristic\":\"" + kenmerk + "\"}";
    }

    private static String zaak(List<String> instanties, String verwacht) {
        return "{\"format\":\"uchar-case-v0\",\"asOfDate\":null,\"input\":{\"instances\":[" + String.join(",", instanties)
                + "],\"parameters\":{}},\"expected\":[" + verwacht + "]}";
    }

    /** The exec-model branch's verdict on one case: null when every expectation holds. */
    private static String speel(String ir, String zaak) throws Exception {
        return speel(ir, zaak, false);
    }

    /** The DRL-text branch's verdict on one case. */
    private static String speelDrl(String ir, String zaak) throws Exception {
        return speel(ir, zaak, true);
    }

    /** The exec-model branch's verdict on one case. */
    private static String speelTms(String ir, String zaak) throws Exception {
        Path tmp = Files.createTempFile("vouwfilter-verre-kant", ".json");
        tmp.toFile().deleteOnExit();
        Files.writeString(tmp, ir);
        return CaseRunner.speelCase(tmp, JSON.readTree(zaak), DroolsBackend.Branch.EXEC_MODEL);
    }

    private static String speel(String ir, String zaak, boolean viaDrl) throws Exception {
        Path tmp = Files.createTempFile("vouwfilter-verre-kant", ".json");
        tmp.toFile().deleteOnExit();
        Files.writeString(tmp, ir);
        JsonNode c = JSON.readTree(zaak);
        return CaseRunner.speelCase(tmp, c, viaDrl);
    }
}
