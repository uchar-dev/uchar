package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * The justification of a default that COUNTS (an aggregation over linked instances) or that reads further than one
 * role (a chain of navigations), and the moment a defaults group decides. RUNTIME-SEMANTICS ch. 7.4: the group decides
 * at the fixpoint of every producer of its justification reads. The exec-model branch refused both forms for the whole
 * group ("niet-reduceerbare justificatie"), while the same condition in an ordinary rule played; and where a group
 * read the target of another defaults group that came after it in the build, it decided before that target was
 * written and never corrected a characteristic it had attributed. {@code ref/} and the ASP backend answer every row
 * here; every document is authored in this class on Jutterswet material, and each is the document of one measured
 * form of the card's form space, with the values {@code ref} and clingo answer.
 *
 * <p><b>Red before the fix</b> (measured 2026-09-29 on the jar of the claim, exec-model branch): the ten rows with a
 * count, a sum, a minimum, a maximum, a chain or a parameter in a justification are refused for the whole group in one
 * lumped sentence ({@code niet-reduceerbare justificatie (far-side/aggregatie/global): <default>}); the row where one
 * group reads what a later group writes answers {@code kenmerk zwaar: verwacht afwezig, werkelijk aanwezig} in its
 * first case, and its second case, run on its own, {@code kenmerk ijverig: verwacht afwezig, werkelijk aanwezig}; the
 * row over a quantifier fold is refused in that same lumped sentence, which names neither the form nor the condition.
 * That row pinned the exec-model branch's refusal by name until 2026-10-10; the truth-maintenance branch plays its
 * document as the reference interpreter answers it ({@code TmsZoalsRefWitnessTest},
 * {@code defaults-kwantorvouw-rechtvaardiging}), so the row is gone.
 *
 * <p>Three rows came after the first fix (2026-09-30), and hold the band a group takes (groepVolgorde). The row where
 * the reached value is written after a group in band 0 could decide: on the transpiler of the claim refused in the
 * lumped sentence; on the first fix, where a group took only the salience of its direct producers, green through the
 * far-side join that fix carried, and red with that join left out ({@code attribuut gemelde premie: verwacht 20,
 * werkelijk 10}). The same row with a characteristic target: red on the first fix, join and all ({@code kenmerk dringend:
 * verwacht afwezig, werkelijk aanwezig}), since a join cannot withdraw an attributed characteristic. With the band both
 * are green and the join is gone. The row with a negation reader of a group's characteristic is a flank, green before
 * and after; with every group placed strictly below its producers it answers {@code kenmerk rustig: verwacht afwezig,
 * werkelijk aanwezig}.
 *
 * <p>The row where the reader of the characteristic sits in the band of the group itself came last (2026-09-30), with
 * the reader placed below the group: before that, in both its documents, {@code kenmerk rustig: verwacht afwezig,
 * werkelijk aanwezig} — on the transpiler of the claim already for the document on the own type, which that
 * transpiler lowers, and on the band of the group for the document two roles away. *
 * <p><b>Both branches of the analysis</b> (card drl-teksttak-op-truth-maintenance, step 4): every row runs on the
 * exec-model branch and on {@code DRL_TMS}, its rendering as DRL text, with the same verdict, a refusal in the same
 * words. <b>Red</b> on {@code e2bd685e}, before the step: every
 * row failed on {@code DRL_TMS} with "drl-tms: de DRL-weergave draagt Afloop nog niet".
 */
class DefaultsRechtvaardigingWitnessTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    // ---------------- the count and its folds ----------------

    @Test void het_aantal_in_een_uitzondering_telt() throws Exception {
        // "De premie van een Jutter moet berekend worden als 10", "Uitzondering op premie: … als 20 indien het
        // aantal vondsten van de Jutter groter is dan 2" (the card's R1). ref and clingo: J1 (three vondsten) 20,
        // the others 10.
        String ir = document(VONDSTMODEL, List.of(), List.of(premiegroep(groter(telling(null), getal(2)))));
        assertNull(speel(ir, zaak(vondstbeeld(true), premies("J1"), "")), "het aantal in de rechtvaardiging");
    }

    @Test void som_minimum_en_maximum_vouwen_met_en_zonder_filter() throws Exception {
        // One document per fold, each an exception above the base 10. Gewichten: J1 6 (zwaar), 4, 9 (zwaar); J2 2;
        // J3 none; J4 7 (zwaar), 8 (zwaar); J5 3, 12. The thresholds separate each fold from the others and a
        // filtered fold from the unfiltered one. ref and clingo per row, the jutters whose premie is 20.
        String[][] rijen = {
                {"sum", "", "16", "J1"},
                {"sum", "zwaar", "14", "J1 J4"},
                {"minimum", "", "5", "J4"},
                {"maximum", "", "8", "J1 J5"},
                {"minimum", "zwaar", "5", "J1 J4"},
                {"maximum", "zwaar", "8", "J1"},
                {"count", "zwaar", "1", "J1 J4"},
        };
        for (String[] r : rijen) {
            String filter = r[1].isEmpty() ? null : kenmerkcheck(r[1]);
            String vouw = r[0].equals("count") ? telling(filter) : vouw(r[0], "gewicht", filter);
            String ir = document(VONDSTMODEL, List.of(), List.of(premiegroep(groter(vouw, getal(Integer.parseInt(r[2]))))));
            assertNull(speel(ir, zaak(vondstbeeld(true), premies(r[3].split(" ")), "")),
                    r[0] + (filter == null ? "" : " over de zware vondsten") + " groter dan " + r[2]);
        }
    }

    @Test void de_telling_ziet_een_filterkenmerk_dat_later_wordt_afgeleid() throws Exception {
        // The filter reads a characteristic a rule derives: "Een Vondst is zwaar indien het gewicht van de Vondst
        // groter is dan 5", declared after the group. ref and clingo: J1 and J4 (two zware vondsten each) 20, J5
        // (one) 10.
        String ir = document(VONDSTMODEL, List.of(ZWAARTE),
                List.of(premiegroep(groter(telling(kenmerkcheck("zwaar")), getal(1)))));
        assertNull(speel(ir, zaak(vondstbeeld(false), premies("J1", "J4"), "")), "de telling na de afleiding van zwaar");
    }

    @Test void de_som_ziet_een_later_afgeleid_gewicht() throws Exception {
        // "Het gewicht van een Vondst moet berekend worden als het ruwgewicht van de Vondst min 1", declared after the
        // group, and the input gives each ruwgewicht one above the gewicht of the other rows. ref and clingo: J1 20.
        String ir = document(VONDSTMODEL, List.of(GEWICHT),
                List.of(premiegroep(groter(vouw("sum", "gewicht", null), getal(16)))));
        List<String> vondsten = new ArrayList<>();
        for (String[] v : VONDSTEN) vondsten.add(vondst(v[0], v[1], "\"ruwgewicht\":{\"number\":\""
                + (Integer.parseInt(v[2]) + 1) + "\"}", false));
        assertNull(speel(ir, zaak(jutters(vondsten), premies("J1"), "")), "de som na de afleiding van het gewicht");
    }

    @Test void een_kenmerkdoel_beslist_pas_als_de_telling_af_is() throws Exception {
        // "Een Jutter is ijverig", "Uitzondering op ijver: … Een Jutter is niet ijverig indien het aantal vondsten
        // van de Jutter die zwaar zijn groter is dan 1", with zwaar derived as above. A characteristic the group
        // attributes too early is never withdrawn (ch. 5.3), so the group must decide after the count is final.
        // ref and clingo: J1 and J4 not ijverig, J2, J3 and J5 ijverig.
        String groep = kenmerkgroep("ijver", "Jutter", "ijverig",
                besluit(groter(telling(kenmerkcheck("zwaar")), getal(1)), false), besluit(ALTIJD, true));
        String ir = document(VONDSTMODEL, List.of(ZWAARTE), List.of(groep));
        String verwacht = String.join(",", kenmerk("J1", "ijverig", false), kenmerk("J2", "ijverig", true),
                kenmerk("J3", "ijverig", true), kenmerk("J4", "ijverig", false), kenmerk("J5", "ijverig", true));
        assertNull(speel(ir, zaak(vondstbeeld(false), verwacht, "")), "ijverig na de telling");
    }

    // ---------------- reads over more than one role ----------------

    @Test void twee_rollen_ver() throws Exception {
        // "De gemelde premie van een Melding moet berekend worden als 10", "Uitzondering op gemelde premie: … als 20
        // indien de premie van de jutter van de vondst van de Melding groter is dan 5" (the card's R2). M1 reaches
        // J1 (premie 8), M2 J2 (3), M3 a vondst without jutter, M4 no vondst. ref and clingo: M1 20, the others 10.
        String ir = document(MELDMODEL, List.of(), List.of(gemeldgroep(groter(viaVondstJutter("premie"), getal(5)))));
        assertNull(speel(ir, zaak(meldbeeld("premie"), gemeld("M1"), "")), "twee rollen ver");
    }

    @Test void drie_rollen_ver() throws Exception {
        // "Uitzondering op controlepremie: … als 20 indien de premie van de jutter van de vondst van de melding van de
        // Controle groter is dan 5". C1 reaches J1 (premie 8). ref and clingo: C1 20, the others 10.
        String controle = objecttype("Controle", "Controles", false, List.of(attribuut("controlepremie")), List.of());
        String model = model(List.of(JUTTER, VONDST, MELDING, controle), List.of(JUTTER_VONDST, VONDST_MELDING,
                feittype("MeldingHeeftControle", "melding", "Melding", "controle", "controles", "Controle")), "");
        String keten = pad("premie", "Jutter", "de premie van de jutter van de vondst van de melding van de Controle",
                "MeldingHeeftControle/melding", "VondstHeeftMelding/vondst", "JutterHeeftVondst/jutter");
        String groep = groep("controlepremie", "Controle", "controlepremie",
                standaard(groter(keten, getal(5)), getal(20)), standaard(ALTIJD, getal(10)));
        List<String> inst = new ArrayList<>(meldinstanties("premie"));
        inst.add(instantie("C1", "Controle", "", "\"melding\":[\"M1\"]", ""));
        inst.add(instantie("C2", "Controle", "", "\"melding\":[\"M2\"]", ""));
        inst.add(instantie("C3", "Controle", "", "\"melding\":[\"M3\"]", ""));
        inst.add(instantie("C4", "Controle", "", "", ""));
        String verwacht = String.join(",", waarde("C1", "controlepremie", 20), waarde("C2", "controlepremie", 10),
                waarde("C3", "controlepremie", 10), waarde("C4", "controlepremie", 10));
        assertNull(speel(document(model, List.of(), List.of(groep)), zaak(inst, verwacht, "")), "drie rollen ver");
    }

    @Test void twee_rollen_ver_een_later_afgeleide_waarde_op_een_kenmerkdoel() throws Exception {
        // "Een Melding is dringend", "Uitzondering op dringendheid: … Een Melding is niet dringend indien de premie van
        // de jutter van de vondst van de Melding groter is dan 5", and "De premie van een Jutter moet gesteld worden op
        // de ruwe premie van de Jutter" declared after it. ref and clingo: M1 not dringend, the others dringend.
        String groep = kenmerkgroep("dringendheid", "Melding", "dringend",
                besluit(groter(viaVondstJutter("premie"), getal(5)), false), besluit(ALTIJD, true));
        String ir = document(MELDMODEL, List.of(PREMIE_UIT_RUWE_PREMIE), List.of(groep));
        String verwacht = String.join(",", kenmerk("M1", "dringend", false), kenmerk("M2", "dringend", true),
                kenmerk("M3", "dringend", true), kenmerk("M4", "dringend", true));
        assertNull(speel(ir, zaak(meldinstanties("ruwe premie"), verwacht, "")), "dringend na de afleiding van de premie");
        // The group with the threshold 15, over a premie that a defaults group writes (20 where the drempel of the
        // Jutter is above 0, else 10), with the Melding declared first, so the reading group is built first. ref and
        // clingo: M1 (J1, drempel 1, premie 20) not dringend, M2 (J2, no drempel, premie 10) dringend.
        groep = kenmerkgroep("dringendheid", "Melding", "dringend",
                besluit(groter(viaVondstJutter("premie"), getal(15)), false), besluit(ALTIJD, true));
        String premie = groep("premie", "Jutter", "premie",
                standaard(groter(eigen("drempel", "Jutter"), getal(0)), getal(20)), standaard(ALTIJD, getal(10)));
        String model = model(List.of(MELDING, VONDST, JUTTER), List.of(JUTTER_VONDST, VONDST_MELDING), "");
        List<String> inst = List.of(instantie("M1", "Melding", "", "\"vondst\":[\"V1\"]", ""),
                instantie("M2", "Melding", "", "\"vondst\":[\"V2\"]", ""),
                vondst("V1", "J1", "", false), vondst("V2", "J2", "", false),
                instantie("J1", "Jutter", "\"drempel\":{\"number\":\"1\"}", "", ""), instantie("J2", "Jutter", "", "", ""));
        assertNull(speel(document(model, List.of(), List.of(groep, premie)), zaak(inst,
                        String.join(",", kenmerk("M1", "dringend", false), kenmerk("M2", "dringend", true),
                                waarde("J1", "premie", 20), waarde("J2", "premie", 10)), "")),
                "dringend over een premie uit een latere groep");
    }

    @Test void de_groep_beslist_opnieuw_als_de_verre_waarde_na_haar_verandert() throws Exception {
        // "De bonuspremie van een Jutter moet berekend worden als 20 indien hij niet ijverig is" (a negated read, so
        // the stratification puts it below salience 0), "De premie van een Jutter moet gesteld worden op de
        // bonuspremie van de Jutter" (salience 0), and the gemelde premie of the card's R2 over that premie with the
        // threshold 15. The premie rule is the producer the group reads; in its band, 0, the group would read the
        // premie before the bonuspremie is written. It waits in the band of the bonuspremie instead. ref and clingo:
        // J1 (not ijverig) premie 20 and M1 20; J2 (ijverig) premie leeg and M2 10.
        String jutter = objecttype("Jutter", "Jutters", true, List.of(attribuut("premie"), attribuut("bonuspremie")),
                List.of("ijverig"));
        String model = model(List.of(jutter, VONDST, MELDING), List.of(JUTTER_VONDST, VONDST_MELDING), "");
        String nietIjverig = "{\"node\":\"CharacteristicCheck\",\"characteristic\":\"ijverig\",\"negated\":true,"
                + "\"source\":{\"snippet\":\"hij niet ijverig is\"}}";
        List<String> regels = List.of(regel("bonuspremie", "Jutter", "bonuspremie", getal(20), nietIjverig, "calculation"),
                regel("premie uit bonus", "Jutter", "premie", eigen("bonuspremie", "Jutter"), null, "equation"));
        String ir = document(model, regels, List.of(gemeldgroep(groter(viaVondstJutter("premie"), getal(15)))));
        List<String> inst = List.of(instantie("J1", "Jutter", "", "", ""), instantie("J2", "Jutter", "", "", "\"ijverig\""),
                vondst("V1", "J1", "", false), vondst("V2", "J2", "", false),
                instantie("M1", "Melding", "", "\"vondst\":[\"V1\"]", ""),
                instantie("M2", "Melding", "", "\"vondst\":[\"V2\"]", ""));
        String verwacht = String.join(",", waarde("J1", "premie", 20),
                "{\"kind\":\"attributeEmpty\",\"label\":\"J2\",\"attribute\":\"premie\"}",
                waarde("M1", "gemelde premie", 20), waarde("M2", "gemelde premie", 10));
        assertNull(speel(ir, zaak(inst, verwacht, "")), "de gemelde premie na de late premie");
    }

    @Test void een_kenmerkdoel_wacht_op_de_band_van_een_verre_producent() throws Exception {
        // The row above with a characteristic target: "Een Melding is dringend", "Uitzondering: Een Melding is niet
        // dringend indien de premie van de jutter van de vondst van de Melding groter is dan 15". The premie rule is
        // in band 0 but waits on the bonuspremie, a negated read in band -1; a group that decided in band 0 attributed
        // dringend to M1 before the premie was 20, and the join cannot withdraw it. ref and clingo: M1 not dringend,
        // M2 dringend.
        String jutter = objecttype("Jutter", "Jutters", true, List.of(attribuut("premie"), attribuut("bonuspremie")),
                List.of("ijverig"));
        String model = model(List.of(jutter, VONDST, MELDING), List.of(JUTTER_VONDST, VONDST_MELDING), "");
        String nietIjverig = "{\"node\":\"CharacteristicCheck\",\"characteristic\":\"ijverig\",\"negated\":true,"
                + "\"source\":{\"snippet\":\"hij niet ijverig is\"}}";
        List<String> regels = List.of(regel("bonuspremie", "Jutter", "bonuspremie", getal(20), nietIjverig, "calculation"),
                regel("premie uit bonus", "Jutter", "premie", eigen("bonuspremie", "Jutter"), null, "equation"));
        String groep = kenmerkgroep("dringendheid", "Melding", "dringend",
                besluit(groter(viaVondstJutter("premie"), getal(15)), false), besluit(ALTIJD, true));
        List<String> inst = List.of(instantie("J1", "Jutter", "", "", ""), instantie("J2", "Jutter", "", "", "\"ijverig\""),
                vondst("V1", "J1", "", false), vondst("V2", "J2", "", false),
                instantie("M1", "Melding", "", "\"vondst\":[\"V1\"]", ""),
                instantie("M2", "Melding", "", "\"vondst\":[\"V2\"]", ""));
        assertNull(speel(document(model, regels, List.of(groep)), zaak(inst,
                        String.join(",", waarde("J1", "premie", 20), kenmerk("M1", "dringend", false),
                                kenmerk("M2", "dringend", true)), "")),
                "dringend na de late premie");
    }

    @Test void een_negatielezer_van_het_kenmerkdoel_vuurt_na_de_groep() throws Exception {
        // A flank: "Een Melding is rustig indien de Melding niet dringend is" reads the target of the dringendheid
        // group (threshold 5) as a negation, in band -1, while the group reads a premie that a rule in band 0 derives
        // from the ruwe premie. The group stays in the band of that producer and fires before the reader; a group
        // placed strictly below its producers would share band -1 with the reader, which as an ordinary rule fires
        // first and attributes rustig to every melding. ref and clingo: M1 not dringend and rustig, M2 dringend and not
        // rustig.
        String melding = objecttype("Melding", "Meldingen", false, List.of(attribuut("gemelde premie")),
                List.of("dringend", "rustig"));
        String model = model(List.of(JUTTER, VONDST, melding), List.of(JUTTER_VONDST, VONDST_MELDING), "");
        String nietDringend = "{\"node\":\"CharacteristicCheck\",\"characteristic\":\"dringend\",\"negated\":true,"
                + "\"source\":{\"snippet\":\"de Melding niet dringend is\"}}";
        String rust = regel("rust", "Melding", "rustig", "{\"node\":\"Literal\",\"value\":{\"boolean\":true},"
                + "\"type\":{\"node\":\"Boolean\"},\"source\":{\"snippet\":\"waar\"}}", nietDringend, null);
        String groep = kenmerkgroep("dringendheid", "Melding", "dringend",
                besluit(groter(viaVondstJutter("premie"), getal(5)), false), besluit(ALTIJD, true));
        List<String> inst = List.of(instantie("J1", "Jutter", "\"ruwe premie\":{\"number\":\"8\"}", "", ""),
                instantie("J2", "Jutter", "\"ruwe premie\":{\"number\":\"3\"}", "", ""),
                vondst("V1", "J1", "", false), vondst("V2", "J2", "", false),
                instantie("M1", "Melding", "", "\"vondst\":[\"V1\"]", ""),
                instantie("M2", "Melding", "", "\"vondst\":[\"V2\"]", ""));
        assertNull(speel(document(model, List.of(rust, PREMIE_UIT_RUWE_PREMIE), List.of(groep)), zaak(inst,
                        String.join(",", kenmerk("M1", "dringend", false), kenmerk("M1", "rustig", true),
                                kenmerk("M2", "dringend", true), kenmerk("M2", "rustig", false)), "")),
                "rustig na de beslissing over dringend");
    }

    @Test void een_negatielezer_van_het_kenmerkdoel_zakt_onder_de_groep() throws Exception {
        // "Een Melding is rustig indien de Melding niet dringend is" reads the target of the dringendheid group under
        // negation. The stratification sees no producer of dringend among the rules and puts the reader one band below
        // them, -1; the group sits in band -1 too, in two ways: below the rule that writes the meldpremie it reads on
        // its own type, and in the band of a premie two roles away that waits on a negated read. In one band the
        // reader, an ordinary rule, fires first and attributes rustig before dringend is decided. ref and clingo: M1
        // not dringend and rustig, M2 dringend and not rustig, in both documents.
        String melding = objecttype("Melding", "Meldingen", false,
                List.of(attribuut("meldpremie"), attribuut("ruwe meldpremie")), List.of("dringend", "rustig"));
        String nietDringend = "{\"node\":\"CharacteristicCheck\",\"characteristic\":\"dringend\",\"negated\":true,"
                + "\"source\":{\"snippet\":\"de Melding niet dringend is\"}}";
        String rust = regel("rust", "Melding", "rustig", "{\"node\":\"Literal\",\"value\":{\"boolean\":true},"
                + "\"type\":{\"node\":\"Boolean\"},\"source\":{\"snippet\":\"waar\"}}", nietDringend, null);
        String meldpremie = regel("meldpremie", "Melding", "meldpremie", eigen("ruwe meldpremie", "Melding"), null, "equation");
        String eigenGroep = kenmerkgroep("dringendheid", "Melding", "dringend",
                besluit(groter(eigen("meldpremie", "Melding"), getal(5)), false), besluit(ALTIJD, true));
        String verwacht = String.join(",", kenmerk("M1", "dringend", false), kenmerk("M1", "rustig", true),
                kenmerk("M2", "dringend", true), kenmerk("M2", "rustig", false));
        assertNull(speel(document(model(List.of(melding), List.of(), ""), List.of(meldpremie, rust), List.of(eigenGroep)),
                        zaak(List.of(instantie("M1", "Melding", "\"ruwe meldpremie\":{\"number\":\"8\"}", "", ""),
                                instantie("M2", "Melding", "\"ruwe meldpremie\":{\"number\":\"3\"}", "", "")), verwacht, "")),
                "rustig na dringend, op het eigen type");
        String jutter = objecttype("Jutter", "Jutters", true, List.of(attribuut("premie"), attribuut("bonuspremie")),
                List.of("ijverig"));
        String nietIjverig = "{\"node\":\"CharacteristicCheck\",\"characteristic\":\"ijverig\",\"negated\":true,"
                + "\"source\":{\"snippet\":\"hij niet ijverig is\"}}";
        List<String> regels = List.of(regel("bonuspremie", "Jutter", "bonuspremie", getal(20), nietIjverig, "calculation"),
                regel("premie uit bonus", "Jutter", "premie", eigen("bonuspremie", "Jutter"), null, "equation"), rust);
        String verreGroep = kenmerkgroep("dringendheid", "Melding", "dringend",
                besluit(groter(viaVondstJutter("premie"), getal(15)), false), besluit(ALTIJD, true));
        List<String> inst = List.of(instantie("J1", "Jutter", "", "", ""), instantie("J2", "Jutter", "", "", "\"ijverig\""),
                vondst("V1", "J1", "", false), vondst("V2", "J2", "", false),
                instantie("M1", "Melding", "", "\"vondst\":[\"V1\"]", ""),
                instantie("M2", "Melding", "", "\"vondst\":[\"V2\"]", ""));
        assertNull(speel(document(model(List.of(jutter, VONDST, melding), List.of(JUTTER_VONDST, VONDST_MELDING), ""),
                        regels, List.of(verreGroep)), zaak(inst, verwacht, "")),
                "rustig na dringend, twee rollen ver in de band van een negatielezer");
    }

    // ---------------- a parameter ----------------

    @Test void een_parameter_in_de_rechtvaardiging() throws Exception {
        // "… als 20 indien het aantal vondsten van de Jutter groter is dan de vondstdrempel", vondstdrempel 2, and
        // "Uitzondering op bonus: … als 2 indien het gewicht van de Vondst groter is dan de gewichtsgrens",
        // gewichtsgrens 5. ref and clingo: J1 20, the other jutters 10; the bonus 2 where the gewicht exceeds 5.
        String model = model(List.of(JUTTER, VONDST), List.of(JUTTER_VONDST),
                parameter("vondstdrempel") + "," + parameter("gewichtsgrens"));
        String bonus = groep("bonus", "Vondst", "bonus",
                standaard(groter(eigen("gewicht", "Vondst"), parameterRef("gewichtsgrens")), getal(2)),
                standaard(ALTIJD, getal(1)));
        String ir = document(model, List.of(), List.of(premiegroep(groter(telling(null), parameterRef("vondstdrempel"))), bonus));
        StringBuilder verwacht = new StringBuilder(premies("J1"));
        for (String[] v : VONDSTEN)
            verwacht.append(",").append(waarde(v[0], "bonus", Integer.parseInt(v[2]) > 5 ? 2 : 1));
        assertNull(speel(ir, zaak(vondstbeeld(true), verwacht.toString(),
                        "\"vondstdrempel\":{\"number\":\"2\"},\"gewichtsgrens\":{\"number\":\"5\"}")),
                "de vondstdrempel en de gewichtsgrens");
    }

    // ---------------- one group reads what another group writes ----------------

    @Test void een_groep_leest_het_doel_van_een_groep_die_na_haar_komt() throws Exception {
        // The premie of a Jutter from a group (10, and 20 where its drempel is above 0). One role away, the
        // Vondst type declared first: "Een Vondst is zwaar", "Uitzondering: Een Vondst is niet zwaar indien de premie
        // van de jutter van de Vondst groter is dan 15". On the Jutter itself, declared first: "Een Jutter is
        // ijverig", "Uitzondering: niet ijverig indien de premie van de Jutter groter is dan 15". ref and clingo: V1
        // (jutter J1, drempel 1) not zwaar, V2 zwaar; J1 not ijverig, J2 ijverig.
        String premie = groep("premie", "Jutter", "premie",
                standaard(groter(eigen("drempel", "Jutter"), getal(0)), getal(20)), standaard(ALTIJD, getal(10)));
        String zwaar = kenmerkgroep("zwaarte", "Vondst", "zwaar",
                besluit(groter(viaJutter("premie"), getal(15)), false), besluit(ALTIJD, true));
        String model = model(List.of(VONDST, JUTTER), List.of(JUTTER_VONDST), "");
        List<String> inst = List.of(vondst("V1", "J1", "", false), vondst("V2", "J2", "", false),
                instantie("J1", "Jutter", "\"drempel\":{\"number\":\"1\"}", "", ""), instantie("J2", "Jutter", "", "", ""));
        assertNull(speel(document(model, List.of(), List.of(zwaar, premie)), zaak(inst,
                        String.join(",", kenmerk("V1", "zwaar", false), kenmerk("V2", "zwaar", true),
                                waarde("J1", "premie", 20), waarde("J2", "premie", 10)), "")),
                "een rol ver, de lezende groep eerst");
        String ijver = kenmerkgroep("ijver", "Jutter", "ijverig",
                besluit(groter(eigen("premie", "Jutter"), getal(15)), false), besluit(ALTIJD, true));
        assertNull(speel(document(model(List.of(JUTTER, VONDST), List.of(JUTTER_VONDST), ""), List.of(),
                        List.of(ijver, premie)),
                zaak(List.of(instantie("J1", "Jutter", "\"drempel\":{\"number\":\"1\"}", "", ""),
                                instantie("J2", "Jutter", "", "", "")),
                        String.join(",", kenmerk("J1", "ijverig", false), kenmerk("J2", "ijverig", true)), "")),
                "op het eigen type, de lezende groep eerst");
    }

    @Test void ongeordend_zonder_voortijdig_conflict() throws Exception {
        // "Uitzonderingen ongeordend": premie 20 indien het aantal vondsten van de Jutter groter is dan 1, and 30
        // indien het aantal vondsten van de Jutter die zwaar zijn kleiner is dan 2, with zwaar derived after the
        // group. Before zwaar is derived the count of zware vondsten is 0 and both grounds hold, which is a conflict
        // the fixpoint does not have. ref and clingo: J4 (two zware) 20 and consistent; J5 (one zware) no premie
        // and inconsistent.
        String a = groepOngeordend("grond veel vondsten", standaard(groter(telling(null), getal(1)), getal(20)));
        String b = groepOngeordend("grond weinig zware vondsten",
                standaard(vergelijking("less", telling(kenmerkcheck("zwaar")), getal(2)), getal(30)));
        String ir = document(VONDSTMODEL, List.of(ZWAARTE), List.of(a, b));
        assertNull(speel(ir, zaak(List.of(instantie("J4", "Jutter", "", "", ""),
                        vondst("V5", "J4", "\"gewicht\":{\"number\":\"7\"}", false),
                        vondst("V6", "J4", "\"gewicht\":{\"number\":\"8\"}", false)),
                        waarde("J4", "premie", 20) + ",{\"kind\":\"consistent\"}", "")), "zonder conflict");
        assertNull(speel(ir, zaak(List.of(instantie("J5", "Jutter", "", "", ""),
                        vondst("V7", "J5", "\"gewicht\":{\"number\":\"3\"}", false),
                        vondst("V8", "J5", "\"gewicht\":{\"number\":\"12\"}", false)),
                        "{\"kind\":\"attributeEmpty\",\"label\":\"J5\",\"attribute\":\"premie\"},{\"kind\":\"inconsistent\"}", "")),
                "met conflict");
    }

    // ---------------- documents ----------------

    private static final String GEHEEL = "{\"node\":\"Number\",\"whole\":true,\"decimals\":0,\"sign\":\"unspecified\"}";
    private static final String NIET_NEGATIEF = "{\"node\":\"Number\",\"whole\":true,\"decimals\":0,\"sign\":\"nonNegative\"}";
    private static final String ALTIJD = "{\"node\":\"Compound\",\"quantifier\":\"all\",\"threshold\":0,\"parts\":[],"
            + "\"source\":{\"snippet\":\"altijd\"}}";

    private static final String JUTTER = objecttype("Jutter", "Jutters", true,
            List.of(attribuut("premie"), attribuut("drempel"), attribuut("ruwe premie")), List.of("ijverig"));
    private static final String VONDST = objecttype("Vondst", "Vondsten", false,
            List.of(attribuut("gewicht"), attribuut("ruwgewicht"), attribuut("bonus")), List.of("zwaar"));
    private static final String MELDING = objecttype("Melding", "Meldingen", false,
            List.of(attribuut("gemelde premie")), List.of("dringend"));
    private static final String JUTTER_VONDST = feittype("JutterHeeftVondst", "jutter", "Jutter", "vondst", "vondsten", "Vondst");
    private static final String VONDST_MELDING = feittype("VondstHeeftMelding", "vondst", "Vondst", "melding", "meldingen", "Melding");
    private static final String VONDSTMODEL = model(List.of(JUTTER, VONDST), List.of(JUTTER_VONDST), "");
    private static final String MELDMODEL = model(List.of(JUTTER, VONDST, MELDING), List.of(JUTTER_VONDST, VONDST_MELDING), "");

    /** "Een Vondst is zwaar indien het gewicht van de Vondst groter is dan 5." */
    private static final String ZWAARTE = regel("zwaarte", "Vondst", "zwaar", "{\"node\":\"Literal\",\"value\":{\"boolean\":true},"
            + "\"type\":{\"node\":\"Boolean\"},\"source\":{\"snippet\":\"waar\"}}", groter(eigen("gewicht", "Vondst"), getal(5)), null);
    /** "Het gewicht van een Vondst moet berekend worden als het ruwgewicht van de Vondst min 1." */
    private static final String GEWICHT = regel("gewicht", "Vondst", "gewicht", "{\"node\":\"Binary\",\"op\":\"minus\",\"left\":"
            + eigen("ruwgewicht", "Vondst") + ",\"right\":" + getal(1) + ",\"type\":" + GEHEEL
            + ",\"source\":{\"snippet\":\"het ruwgewicht van de Vondst min 1\"}}", null, "calculation");
    /** "De premie van een Jutter moet gesteld worden op de ruwe premie van de Jutter." */
    private static final String PREMIE_UIT_RUWE_PREMIE = regel("premie", "Jutter", "premie", eigen("ruwe premie", "Jutter"), null,
            "equation");

    /** label, jutter, gewicht, zwaar in the input rows. */
    private static final String[][] VONDSTEN = {
            {"V1", "J1", "6", "zwaar"}, {"V2", "J1", "4", ""}, {"V3", "J1", "9", "zwaar"}, {"V4", "J2", "2", ""},
            {"V5", "J4", "7", "zwaar"}, {"V6", "J4", "8", "zwaar"}, {"V7", "J5", "3", ""}, {"V8", "J5", "12", ""}};

    private static String attribuut(String naam) {
        return "{\"node\":\"Attribute\",\"name\":\"" + naam + "\",\"datatype\":" + GEHEEL + "}";
    }

    private static String objecttype(String naam, String meervoud, boolean bezield, List<String> attributen, List<String> kenmerken) {
        List<String> k = new ArrayList<>();
        for (String n : kenmerken) k.add("{\"node\":\"Characteristic\",\"name\":\"" + n + "\",\"kind\":\"adjectival\"}");
        return "{\"node\":\"ObjectType\",\"name\":\"" + naam + "\",\"animate\":" + bezield + ",\"plural\":\"" + meervoud
                + "\",\"attributes\":[" + String.join(",", attributen) + "],\"characteristics\":[" + String.join(",", k) + "]}";
    }

    private static String feittype(String naam, String rol1, String type1, String rol2, String meervoud2, String type2) {
        return "{\"node\":\"FactType\",\"name\":\"" + naam + "\",\"reciprocal\":false,\"roles\":["
                + "{\"node\":\"Role\",\"name\":\"" + rol1 + "\",\"objectType\":\"" + type1 + "\",\"many\":false},"
                + "{\"node\":\"Role\",\"name\":\"" + rol2 + "\",\"objectType\":\"" + type2 + "\",\"plural\":\"" + meervoud2
                + "\",\"many\":true}]}";
    }

    private static String parameter(String naam) {
        return "{\"node\":\"Parameter\",\"name\":\"" + naam + "\",\"datatype\":" + GEHEEL + "}";
    }

    private static String model(List<String> typen, List<String> feittypen, String parameters) {
        return "{\"objectTypes\":[" + String.join(",", typen) + "],\"factTypes\":[" + String.join(",", feittypen)
                + "],\"parameters\":[" + parameters + "],\"domains\":[],\"unitSystems\":[],\"dimensions\":[],"
                + "\"timelines\":[],\"dayKinds\":[]}";
    }

    private static String getal(int n) {
        return "{\"node\":\"Literal\",\"value\":{\"number\":\"" + n + "\"},\"type\":" + GEHEEL + ",\"source\":{\"snippet\":\"" + n + "\"}}";
    }

    private static String eigen(String attribuut, String type) {
        return "{\"node\":\"AttributeReference\",\"attribute\":\"" + attribuut + "\",\"subjectType\":\"" + type
                + "\",\"source\":{\"snippet\":\"de " + attribuut + " van de " + type + "\"}}";
    }

    private static String parameterRef(String naam) {
        return "{\"node\":\"ParameterReference\",\"parameter\":\"" + naam + "\",\"source\":{\"snippet\":\"de " + naam + "\"}}";
    }

    /** A navigated read of {@code attribuut} on {@code type} over the hops, each "FactType/role". */
    private static String pad(String attribuut, String type, String snippet, String... hops) {
        List<String> h = new ArrayList<>();
        for (String hop : hops) {
            String[] d = hop.split("/");
            h.add("{\"node\":\"RoleBinding\",\"factType\":\"" + d[0] + "\",\"role\":\"" + d[1] + "\"}");
        }
        return "{\"node\":\"AttributeReference\",\"attribute\":\"" + attribuut + "\",\"subjectType\":\"" + type
                + "\",\"source\":{\"snippet\":\"" + snippet + "\"},\"path\":[" + String.join(",", h) + "]}";
    }

    /** "de &lt;attribuut&gt; van de jutter van de Vondst": one hop. */
    private static String viaJutter(String attribuut) {
        return pad(attribuut, "Jutter", "de " + attribuut + " van de jutter van de Vondst", "JutterHeeftVondst/jutter");
    }

    /** "de &lt;attribuut&gt; van de jutter van de vondst van de Melding": two hops. */
    private static String viaVondstJutter(String attribuut) {
        return pad(attribuut, "Jutter", "de " + attribuut + " van de jutter van de vondst van de Melding",
                "VondstHeeftMelding/vondst", "JutterHeeftVondst/jutter");
    }

    private static String kenmerkcheck(String kenmerk) {
        return "{\"node\":\"CharacteristicCheck\",\"characteristic\":\"" + kenmerk + "\",\"negated\":false,"
                + "\"source\":{\"snippet\":\"" + kenmerk + " zijn\"}}";
    }

    private static final String VONDST_BEREIK = "\"elementType\":\"Vondst\",\"sourceRole\":{\"node\":\"RoleBinding\","
            + "\"factType\":\"JutterHeeftVondst\",\"role\":\"vondst\"}";

    /** "het aantal vondsten van de Jutter [die … zijn]". */
    private static String telling(String filter) {
        return "{\"node\":\"Aggregation\",\"kind\":\"count\",\"arguments\":[" + eigen("vondsten", "Jutter") + "],"
                + VONDST_BEREIK + (filter == null ? "" : ",\"filter\":" + filter) + ",\"type\":" + NIET_NEGATIEF
                + ",\"source\":{\"snippet\":\"het aantal vondsten van de Jutter\"}}";
    }

    /** "de som van / de minimale waarde van / de maximale waarde van het &lt;attribuut&gt; van alle vondsten van de Jutter". */
    private static String vouw(String soort, String attribuut, String filter) {
        return "{\"node\":\"Aggregation\",\"kind\":\"" + soort + "\",\"arguments\":[" + eigen(attribuut, "Vondst") + "],"
                + VONDST_BEREIK + (filter == null ? "" : ",\"filter\":" + filter) + ",\"type\":" + GEHEEL
                + ",\"source\":{\"snippet\":\"de " + soort + " van het " + attribuut + " van alle vondsten van de Jutter\"}}";
    }

    private static String groter(String links, String rechts) { return vergelijking("greater", links, rechts); }

    private static String vergelijking(String op, String links, String rechts) {
        return "{\"node\":\"Comparison\",\"op\":\"" + op + "\",\"left\":" + links + ",\"right\":" + rechts
                + ",\"source\":{\"snippet\":\"vergelijking\"}}";
    }

    /** An ordinary rule with one version; {@code voorwaarde} null for none, {@code presentatie} null on a
     *  characteristic assignment ("calculation" for "berekend worden als", "equation" for "gesteld worden op"). */
    private static String regel(String naam, String type, String doel, String waarde, String voorwaarde, String presentatie) {
        return "{\"node\":\"Rule\",\"name\":\"" + naam + "\",\"versions\":[{\"validity\":{\"node\":\"Validity\"},"
                + "\"target\":{\"node\":\"AttributeReference\",\"attribute\":\"" + doel + "\",\"subjectType\":\"" + type + "\","
                + "\"source\":{\"snippet\":\"de " + doel + "\"}},\"value\":" + waarde
                + (voorwaarde == null ? "" : ",\"condition\":" + voorwaarde) + ",\"initialisation\":false"
                + (presentatie == null ? "" : ",\"presentation\":\"" + presentatie + "\"") + "}]}";
    }

    private static String standaard(String rechtvaardiging, String gevolg) {
        return "{\"node\":\"Default\",\"justification\":" + rechtvaardiging + ",\"consequence\":" + gevolg + "}";
    }

    private static String besluit(String rechtvaardiging, boolean besluit) {
        return "{\"node\":\"Default\",\"decision\":" + besluit + ",\"justification\":" + rechtvaardiging + "}";
    }

    private static String groep(String naam, String type, String attribuut, String... defaults) {
        return "{\"node\":\"PrioritizedDefaults\",\"name\":\"" + naam + "\",\"subject\":\"" + type + "\",\"attribute\":\""
                + attribuut + "\",\"defaults\":[" + String.join(",", defaults) + "],\"validity\":{\"node\":\"Validity\"}}";
    }

    private static String groepOngeordend(String naam, String... defaults) {
        return "{\"node\":\"PrioritizedDefaults\",\"name\":\"" + naam + "\",\"subject\":\"Jutter\",\"attribute\":\"premie\","
                + "\"unordered\":true,\"defaults\":[" + String.join(",", defaults) + "],\"validity\":{\"node\":\"Validity\"}}";
    }

    private static String kenmerkgroep(String naam, String type, String kenmerk, String... defaults) {
        return "{\"node\":\"PrioritizedDefaults\",\"name\":\"" + naam + "\",\"subject\":\"" + type + "\",\"characteristic\":\""
                + kenmerk + "\",\"defaults\":[" + String.join(",", defaults) + "],\"validity\":{\"node\":\"Validity\"}}";
    }

    /** The premie of a Jutter: 20 where {@code rechtvaardiging} holds, else 10. */
    private static String premiegroep(String rechtvaardiging) {
        return groep("premie", "Jutter", "premie", standaard(rechtvaardiging, getal(20)), standaard(ALTIJD, getal(10)));
    }

    /** The gemelde premie of a Melding: 20 where {@code rechtvaardiging} holds, else 10. */
    private static String gemeldgroep(String rechtvaardiging) {
        return groep("gemelde premie", "Melding", "gemelde premie", standaard(rechtvaardiging, getal(20)), standaard(ALTIJD, getal(10)));
    }

    private static String document(String model, List<String> regels, List<String> groepen) {
        return "{\"format\":\"uchar-ir-v1\",\"model\":" + model + ",\"rules\":{\"rules\":[" + String.join(",", regels) + "],"
                + "\"dayKindDefinitions\":[],\"objectCreations\":[],\"allocations\":[],\"consistencies\":[],"
                + "\"uniquenesses\":[],\"factCreations\":[],\"startPointDeterminations\":[],\"ruleGroups\":[],"
                + "\"prioritizedDefaults\":[" + String.join(",", groepen) + "]},\"diagnostics\":[]}";
    }

    // ---------------- cases ----------------

    private static String instantie(String label, String type, String attributen, String rollen, String kenmerken) {
        return "{\"label\":\"" + label + "\",\"objectType\":\"" + type + "\",\"attributes\":{" + attributen + "},\"roles\":{"
                + rollen + "},\"characteristics\":[" + kenmerken + "]}";
    }

    private static String vondst(String label, String jutter, String attributen, boolean zwaar) {
        return instantie(label, "Vondst", attributen, jutter == null ? "" : "\"jutter\":[\"" + jutter + "\"]",
                zwaar ? "\"zwaar\"" : "");
    }

    private static List<String> jutters(List<String> vondsten) {
        List<String> uit = new ArrayList<>();
        for (int i = 1; i <= 5; i++) uit.add(instantie("J" + i, "Jutter", "", "", ""));
        uit.addAll(vondsten);
        return uit;
    }

    /** The jutters J1..J5 and the vondsten of {@link #VONDSTEN}, zwaar as input where {@code metZwaar}. */
    private static List<String> vondstbeeld(boolean metZwaar) {
        List<String> vondsten = new ArrayList<>();
        for (String[] v : VONDSTEN)
            vondsten.add(vondst(v[0], v[1], "\"gewicht\":{\"number\":\"" + v[2] + "\"}", metZwaar && !v[3].isEmpty()));
        return jutters(vondsten);
    }

    /** J1 (premie or ruwe premie 8), J2 (3), J3; V1 of J1, V2 of J2, V3 without jutter; M1..M3 of V1..V3, M4 alone. */
    private static List<String> meldinstanties(String premieAttribuut) {
        return List.of(instantie("J1", "Jutter", "\"" + premieAttribuut + "\":{\"number\":\"8\"}", "", ""),
                instantie("J2", "Jutter", "\"" + premieAttribuut + "\":{\"number\":\"3\"}", "", ""),
                instantie("J3", "Jutter", "", "", ""),
                vondst("V1", "J1", "", false), vondst("V2", "J2", "", false), vondst("V3", null, "", false),
                instantie("M1", "Melding", "", "\"vondst\":[\"V1\"]", ""),
                instantie("M2", "Melding", "", "\"vondst\":[\"V2\"]", ""),
                instantie("M3", "Melding", "", "\"vondst\":[\"V3\"]", ""),
                instantie("M4", "Melding", "", "", ""));
    }

    private static List<String> meldbeeld(String premieAttribuut) { return meldinstanties(premieAttribuut); }

    private static String waarde(String label, String attribuut, int getal) {
        return "{\"kind\":\"attributeValue\",\"label\":\"" + label + "\",\"attribute\":\"" + attribuut
                + "\",\"value\":{\"number\":\"" + getal + "\"}}";
    }

    private static String kenmerk(String label, String kenmerk, boolean aanwezig) {
        return "{\"kind\":\"characteristic" + (aanwezig ? "Present" : "Absent") + "\",\"label\":\"" + label
                + "\",\"characteristic\":\"" + kenmerk + "\"}";
    }

    /** The premie of J1..J5: 20 for the named jutters, 10 for the others. */
    private static String premies(String... twintig) {
        List<String> t = List.of(twintig), uit = new ArrayList<>();
        for (int i = 1; i <= 5; i++) uit.add(waarde("J" + i, "premie", t.contains("J" + i) ? 20 : 10));
        return String.join(",", uit);
    }

    /** The gemelde premie of M1..M4: 20 for the named meldingen, 10 for the others. */
    private static String gemeld(String... twintig) {
        List<String> t = List.of(twintig), uit = new ArrayList<>();
        for (int i = 1; i <= 4; i++) uit.add(waarde("M" + i, "gemelde premie", t.contains("M" + i) ? 20 : 10));
        return String.join(",", uit);
    }

    private static String zaak(List<String> instanties, String verwacht, String parameters) {
        return "{\"format\":\"uchar-case-v0\",\"asOfDate\":null,\"input\":{\"instances\":[" + String.join(",", instanties)
                + "],\"parameters\":{" + parameters + "}},\"expected\":[" + verwacht + "]}";
    }

    /** The verdict on one case of the exec-model branch and of {@code DRL_TMS}, which must be the same: null when
     *  every expectation holds. */
    private static String speel(String ir, String zaak) throws Exception {
        Path tmp = Files.createTempFile("defaults-rechtvaardiging", ".json");
        tmp.toFile().deleteOnExit();
        Files.writeString(tmp, ir);
        JsonNode c = JSON.readTree(zaak);
        String exec = CaseRunner.speelCase(tmp, c, DroolsBackend.Branch.EXEC_MODEL);
        assertEquals(exec, CaseRunner.speelCase(tmp, c, DroolsBackend.Branch.DRL_TMS), "DRL_TMS naast de exec-model-tak");
        return exec;
    }
}
