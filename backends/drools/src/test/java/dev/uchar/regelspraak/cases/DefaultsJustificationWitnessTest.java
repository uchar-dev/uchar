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
 * lumped sentence ({@code niet-reduceerbare justificatie (far-side/aggregation/global): <default>}); the row where one
 * group reads what a later group writes answers {@code kenmerk zwaar: verwacht afwezig, werkelijk aanwezig} in its
 * first case, and its second case, run on its own, {@code kenmerk ijverig: verwacht afwezig, werkelijk aanwezig}; the
 * row over a quantifier fold is refused in that same lumped sentence, which names neither the form nor the condition.
 * That row pinned the exec-model branch's refusal by name until 2026-10-10; the exec-model branch with truth
 * maintenance plays its document as the reference interpreter answers it ({@code TmsLikeRefWitnessTest}, {@code
 * defaults-quantifier-fold-justification}), so the row is gone.
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
 * exec-model branch and on {@code DRL_TEXT}, its rendering as DRL text, with the same verdict, a refusal in the same
 * words. <b>Red</b> on {@code e2bd685e}, before the step: every
 * row failed on {@code DRL_TMS} with "drl-tms: de DRL-weergave draagt Afloop nog niet".
 */
class DefaultsJustificationWitnessTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    // ---------------- the count and its folds ----------------

    @Test void the_count_in_an_exception_counts() throws Exception {
        // "De premie van een Jutter moet berekend worden als 10", "Uitzondering op premie: … als 20 indien het
        // aantal vondsten van de Jutter groter is dan 2" (the card's R1). ref and clingo: J1 (three vondsten) 20,
        // the others 10.
        String ir = document(VONDSTMODEL, List.of(), List.of(premieGroup(greater(count(null), number(2)))));
        assertNull(play(ir, caseDocument(vondstPicture(true), premies("J1"), "")), "het aantal in de rechtvaardiging");
    }

    @Test void sum_minimum_and_maximum_folds_with_and_without_filter() throws Exception {
        // One document per fold, each an exception above the base 10. Gewichten: J1 6 (zwaar), 4, 9 (zwaar); J2 2;
        // J3 none; J4 7 (zwaar), 8 (zwaar); J5 3, 12. The thresholds separate each fold from the others and a
        // filtered fold from the unfiltered one. ref and clingo per row, the jutters whose premie is 20.
        String[][] rows = {
                {"sum", "", "16", "J1"},
                {"sum", "zwaar", "14", "J1 J4"},
                {"minimum", "", "5", "J4"},
                {"maximum", "", "8", "J1 J5"},
                {"minimum", "zwaar", "5", "J1 J4"},
                {"maximum", "zwaar", "8", "J1"},
                {"count", "zwaar", "1", "J1 J4"},
        };
        for (String[] r : rows) {
            String filter = r[1].isEmpty() ? null : characteristicCheck(r[1]);
            String fold = r[0].equals("count") ? count(filter) : fold(r[0], "gewicht", filter);
            String ir = document(VONDSTMODEL, List.of(), List.of(premieGroup(greater(fold, number(Integer.parseInt(r[2]))))));
            assertNull(play(ir, caseDocument(vondstPicture(true), premies(r[3].split(" ")), "")),
                    r[0] + (filter == null ? "" : " over de zware vondsten") + " groter dan " + r[2]);
        }
    }

    @Test void the_count_sees_a_filter_characteristic_that_is_derived_later() throws Exception {
        // The filter reads a characteristic a rule derives: "Een Vondst is zwaar indien het gewicht van de Vondst
        // groter is dan 5", declared after the group. ref and clingo: J1 and J4 (two zware vondsten each) 20, J5
        // (one) 10.
        String ir = document(VONDSTMODEL, List.of(ZWAARTE),
                List.of(premieGroup(greater(count(characteristicCheck("zwaar")), number(1)))));
        assertNull(play(ir, caseDocument(vondstPicture(false), premies("J1", "J4"), "")), "de telling na de afleiding van zwaar");
    }

    @Test void the_sum_sees_a_later_derived_weight() throws Exception {
        // "Het gewicht van een Vondst moet berekend worden als het ruwgewicht van de Vondst min 1", declared after the
        // group, and the input gives each ruwgewicht one above the gewicht of the other rows. ref and clingo: J1 20.
        String ir = document(VONDSTMODEL, List.of(WEIGHT),
                List.of(premieGroup(greater(fold("sum", "gewicht", null), number(16)))));
        List<String> vondsten = new ArrayList<>();
        for (String[] v : VONDSTEN) vondsten.add(vondst(v[0], v[1], "\"ruwgewicht\":{\"number\":\""
                + (Integer.parseInt(v[2]) + 1) + "\"}", false));
        assertNull(play(ir, caseDocument(jutters(vondsten), premies("J1"), "")), "de som na de afleiding van het gewicht");
    }

    @Test void a_characteristic_target_decides_only_when_the_count_is_finished() throws Exception {
        // "Een Jutter is ijverig", "Uitzondering op ijver: … Een Jutter is niet ijverig indien het aantal vondsten
        // van de Jutter die zwaar zijn groter is dan 1", with zwaar derived as above. A characteristic the group
        // attributes too early is never withdrawn (ch. 5.3), so the group must decide after the count is final.
        // ref and clingo: J1 and J4 not ijverig, J2, J3 and J5 ijverig.
        String group = characteristicGroup("ijver", "Jutter", "ijverig",
                decision(greater(count(characteristicCheck("zwaar")), number(1)), false), decision(ALTIJD, true));
        String ir = document(VONDSTMODEL, List.of(ZWAARTE), List.of(group));
        String expected = String.join(",", characteristic("J1", "ijverig", false), characteristic("J2", "ijverig", true),
                characteristic("J3", "ijverig", true), characteristic("J4", "ijverig", false), characteristic("J5", "ijverig", true));
        assertNull(play(ir, caseDocument(vondstPicture(false), expected, "")), "ijverig na de telling");
    }

    // ---------------- reads over more than one role ----------------

    @Test void two_roles_far() throws Exception {
        // "De gemelde premie van een Melding moet berekend worden als 10", "Uitzondering op gemelde premie: … als 20
        // indien de premie van de jutter van de vondst van de Melding groter is dan 5" (the card's R2). M1 reaches
        // J1 (premie 8), M2 J2 (3), M3 a vondst without jutter, M4 no vondst. ref and clingo: M1 20, the others 10.
        String ir = document(REPORT_MODEL, List.of(), List.of(reportedGroup(greater(viaVondstJutter("premie"), number(5)))));
        assertNull(play(ir, caseDocument(messagePicture("premie"), reported("M1"), "")), "twee rollen ver");
    }

    @Test void three_roles_far() throws Exception {
        // "Uitzondering op controlepremie: … als 20 indien de premie van de jutter van de vondst van de melding van de
        // Controle groter is dan 5". C1 reaches J1 (premie 8). ref and clingo: C1 20, the others 10.
        String check = objectType("Controle", "Controles", false, List.of(attribute("controlepremie")), List.of());
        String model = model(List.of(JUTTER, VONDST, MESSAGE, check), List.of(JUTTER_VONDST, VONDST_MESSAGE,
                factType("MeldingHeeftControle", "melding", "Melding", "controle", "controles", "Controle")), "");
        String chain = path("premie", "Jutter", "de premie van de jutter van de vondst van de melding van de Controle",
                "MeldingHeeftControle/melding", "VondstHeeftMelding/vondst", "JutterHeeftVondst/jutter");
        String group = group("controlepremie", "Controle", "controlepremie",
                defaultEntry(greater(chain, number(5)), number(20)), defaultEntry(ALTIJD, number(10)));
        List<String> inst = new ArrayList<>(reportInstances("premie"));
        inst.add(instance("C1", "Controle", "", "\"melding\":[\"M1\"]", ""));
        inst.add(instance("C2", "Controle", "", "\"melding\":[\"M2\"]", ""));
        inst.add(instance("C3", "Controle", "", "\"melding\":[\"M3\"]", ""));
        inst.add(instance("C4", "Controle", "", "", ""));
        String expected = String.join(",", value("C1", "controlepremie", 20), value("C2", "controlepremie", 10),
                value("C3", "controlepremie", 10), value("C4", "controlepremie", 10));
        assertNull(play(document(model, List.of(), List.of(group)), caseDocument(inst, expected, "")), "drie rollen ver");
    }

    @Test void two_roles_far_a_later_derived_value_on_a_characteristic_target() throws Exception {
        // "Een Melding is dringend", "Uitzondering op dringendheid: … Een Melding is niet dringend indien de premie van
        // de jutter van de vondst van de Melding groter is dan 5", and "De premie van een Jutter moet gesteld worden op
        // de ruwe premie van de Jutter" declared after it. ref and clingo: M1 not dringend, the others dringend.
        String group = characteristicGroup("dringendheid", "Melding", "dringend",
                decision(greater(viaVondstJutter("premie"), number(5)), false), decision(ALTIJD, true));
        String ir = document(REPORT_MODEL, List.of(PREMIE_OUT_RAW_PREMIE), List.of(group));
        String expected = String.join(",", characteristic("M1", "dringend", false), characteristic("M2", "dringend", true),
                characteristic("M3", "dringend", true), characteristic("M4", "dringend", true));
        assertNull(play(ir, caseDocument(reportInstances("ruwe premie"), expected, "")), "dringend na de afleiding van de premie");
        // The group with the threshold 15, over a premie that a defaults group writes (20 where the drempel of the
        // Jutter is above 0, else 10), with the Melding declared first, so the reading group is built first. ref and
        // clingo: M1 (J1, drempel 1, premie 20) not dringend, M2 (J2, no drempel, premie 10) dringend.
        group = characteristicGroup("dringendheid", "Melding", "dringend",
                decision(greater(viaVondstJutter("premie"), number(15)), false), decision(ALTIJD, true));
        String premie = group("premie", "Jutter", "premie",
                defaultEntry(greater(own("drempel", "Jutter"), number(0)), number(20)), defaultEntry(ALTIJD, number(10)));
        String model = model(List.of(MESSAGE, VONDST, JUTTER), List.of(JUTTER_VONDST, VONDST_MESSAGE), "");
        List<String> inst = List.of(instance("M1", "Melding", "", "\"vondst\":[\"V1\"]", ""),
                instance("M2", "Melding", "", "\"vondst\":[\"V2\"]", ""),
                vondst("V1", "J1", "", false), vondst("V2", "J2", "", false),
                instance("J1", "Jutter", "\"drempel\":{\"number\":\"1\"}", "", ""), instance("J2", "Jutter", "", "", ""));
        assertNull(play(document(model, List.of(), List.of(group, premie)), caseDocument(inst,
                        String.join(",", characteristic("M1", "dringend", false), characteristic("M2", "dringend", true),
                                value("J1", "premie", 20), value("J2", "premie", 10)), "")),
                "dringend over een premie uit een latere groep");
    }

    @Test void the_group_decides_again_if_the_far_side_value_changes_after_it() throws Exception {
        // "De bonuspremie van een Jutter moet berekend worden als 20 indien hij niet ijverig is" (a negated read, so
        // the stratification puts it below salience 0), "De premie van een Jutter moet gesteld worden op de
        // bonuspremie van de Jutter" (salience 0), and the gemelde premie of the card's R2 over that premie with the
        // threshold 15. The premie rule is the producer the group reads; in its band, 0, the group would read the
        // premie before the bonuspremie is written. It waits in the band of the bonuspremie instead. ref and clingo:
        // J1 (not ijverig) premie 20 and M1 20; J2 (ijverig) premie leeg and M2 10.
        String jutter = objectType("Jutter", "Jutters", true, List.of(attribute("premie"), attribute("bonuspremie")),
                List.of("ijverig"));
        String model = model(List.of(jutter, VONDST, MESSAGE), List.of(JUTTER_VONDST, VONDST_MESSAGE), "");
        String notIjverig = "{\"node\":\"CharacteristicCheck\",\"characteristic\":\"ijverig\",\"negated\":true,"
                + "\"source\":{\"snippet\":\"hij niet ijverig is\"}}";
        List<String> rules = List.of(rule("bonuspremie", "Jutter", "bonuspremie", number(20), notIjverig, "calculation"),
                rule("premie uit bonus", "Jutter", "premie", own("bonuspremie", "Jutter"), null, "equation"));
        String ir = document(model, rules, List.of(reportedGroup(greater(viaVondstJutter("premie"), number(15)))));
        List<String> inst = List.of(instance("J1", "Jutter", "", "", ""), instance("J2", "Jutter", "", "", "\"ijverig\""),
                vondst("V1", "J1", "", false), vondst("V2", "J2", "", false),
                instance("M1", "Melding", "", "\"vondst\":[\"V1\"]", ""),
                instance("M2", "Melding", "", "\"vondst\":[\"V2\"]", ""));
        String expected = String.join(",", value("J1", "premie", 20),
                "{\"kind\":\"attributeEmpty\",\"label\":\"J2\",\"attribute\":\"premie\"}",
                value("M1", "gemelde premie", 20), value("M2", "gemelde premie", 10));
        assertNull(play(ir, caseDocument(inst, expected, "")), "de gemelde premie na de late premie");
    }

    @Test void a_characteristic_target_waits_for_the_band_of_a_far_side_producer() throws Exception {
        // The row above with a characteristic target: "Een Melding is dringend", "Uitzondering: Een Melding is niet
        // dringend indien de premie van de jutter van de vondst van de Melding groter is dan 15". The premie rule is
        // in band 0 but waits on the bonuspremie, a negated read in band -1; a group that decided in band 0 attributed
        // dringend to M1 before the premie was 20, and the join cannot withdraw it. ref and clingo: M1 not dringend,
        // M2 dringend.
        String jutter = objectType("Jutter", "Jutters", true, List.of(attribute("premie"), attribute("bonuspremie")),
                List.of("ijverig"));
        String model = model(List.of(jutter, VONDST, MESSAGE), List.of(JUTTER_VONDST, VONDST_MESSAGE), "");
        String notIjverig = "{\"node\":\"CharacteristicCheck\",\"characteristic\":\"ijverig\",\"negated\":true,"
                + "\"source\":{\"snippet\":\"hij niet ijverig is\"}}";
        List<String> rules = List.of(rule("bonuspremie", "Jutter", "bonuspremie", number(20), notIjverig, "calculation"),
                rule("premie uit bonus", "Jutter", "premie", own("bonuspremie", "Jutter"), null, "equation"));
        String group = characteristicGroup("dringendheid", "Melding", "dringend",
                decision(greater(viaVondstJutter("premie"), number(15)), false), decision(ALTIJD, true));
        List<String> inst = List.of(instance("J1", "Jutter", "", "", ""), instance("J2", "Jutter", "", "", "\"ijverig\""),
                vondst("V1", "J1", "", false), vondst("V2", "J2", "", false),
                instance("M1", "Melding", "", "\"vondst\":[\"V1\"]", ""),
                instance("M2", "Melding", "", "\"vondst\":[\"V2\"]", ""));
        assertNull(play(document(model, rules, List.of(group)), caseDocument(inst,
                        String.join(",", value("J1", "premie", 20), characteristic("M1", "dringend", false),
                                characteristic("M2", "dringend", true)), "")),
                "dringend na de late premie");
    }

    @Test void a_negation_reader_of_the_characteristic_target_fires_after_the_group() throws Exception {
        // A flank: "Een Melding is rustig indien de Melding niet dringend is" reads the target of the dringendheid
        // group (threshold 5) as a negation, in band -1, while the group reads a premie that a rule in band 0 derives
        // from the ruwe premie. The group stays in the band of that producer and fires before the reader; a group
        // placed strictly below its producers would share band -1 with the reader, which as an ordinary rule fires
        // first and attributes rustig to every melding. ref and clingo: M1 not dringend and rustig, M2 dringend and not
        // rustig.
        String message = objectType("Melding", "Meldingen", false, List.of(attribute("gemelde premie")),
                List.of("dringend", "rustig"));
        String model = model(List.of(JUTTER, VONDST, message), List.of(JUTTER_VONDST, VONDST_MESSAGE), "");
        String notDringend = "{\"node\":\"CharacteristicCheck\",\"characteristic\":\"dringend\",\"negated\":true,"
                + "\"source\":{\"snippet\":\"de Melding niet dringend is\"}}";
        String rest = rule("rust", "Melding", "rustig", "{\"node\":\"Literal\",\"value\":{\"boolean\":true},"
                + "\"type\":{\"node\":\"Boolean\"},\"source\":{\"snippet\":\"waar\"}}", notDringend, null);
        String group = characteristicGroup("dringendheid", "Melding", "dringend",
                decision(greater(viaVondstJutter("premie"), number(5)), false), decision(ALTIJD, true));
        List<String> inst = List.of(instance("J1", "Jutter", "\"ruwe premie\":{\"number\":\"8\"}", "", ""),
                instance("J2", "Jutter", "\"ruwe premie\":{\"number\":\"3\"}", "", ""),
                vondst("V1", "J1", "", false), vondst("V2", "J2", "", false),
                instance("M1", "Melding", "", "\"vondst\":[\"V1\"]", ""),
                instance("M2", "Melding", "", "\"vondst\":[\"V2\"]", ""));
        assertNull(play(document(model, List.of(rest, PREMIE_OUT_RAW_PREMIE), List.of(group)), caseDocument(inst,
                        String.join(",", characteristic("M1", "dringend", false), characteristic("M1", "rustig", true),
                                characteristic("M2", "dringend", true), characteristic("M2", "rustig", false)), "")),
                "rustig na de beslissing over dringend");
    }

    @Test void a_negation_reader_of_the_characteristic_target_sinks_under_the_group() throws Exception {
        // "Een Melding is rustig indien de Melding niet dringend is" reads the target of the dringendheid group under
        // negation. The stratification sees no producer of dringend among the rules and puts the reader one band below
        // them, -1; the group sits in band -1 too, in two ways: below the rule that writes the meldpremie it reads on
        // its own type, and in the band of a premie two roles away that waits on a negated read. In one band the
        // reader, an ordinary rule, fires first and attributes rustig before dringend is decided. ref and clingo: M1
        // not dringend and rustig, M2 dringend and not rustig, in both documents.
        String message = objectType("Melding", "Meldingen", false,
                List.of(attribute("meldpremie"), attribute("ruwe meldpremie")), List.of("dringend", "rustig"));
        String notDringend = "{\"node\":\"CharacteristicCheck\",\"characteristic\":\"dringend\",\"negated\":true,"
                + "\"source\":{\"snippet\":\"de Melding niet dringend is\"}}";
        String rest = rule("rust", "Melding", "rustig", "{\"node\":\"Literal\",\"value\":{\"boolean\":true},"
                + "\"type\":{\"node\":\"Boolean\"},\"source\":{\"snippet\":\"waar\"}}", notDringend, null);
        String meldpremie = rule("meldpremie", "Melding", "meldpremie", own("ruwe meldpremie", "Melding"), null, "equation");
        String ownGroup = characteristicGroup("dringendheid", "Melding", "dringend",
                decision(greater(own("meldpremie", "Melding"), number(5)), false), decision(ALTIJD, true));
        String expected = String.join(",", characteristic("M1", "dringend", false), characteristic("M1", "rustig", true),
                characteristic("M2", "dringend", true), characteristic("M2", "rustig", false));
        assertNull(play(document(model(List.of(message), List.of(), ""), List.of(meldpremie, rest), List.of(ownGroup)),
                        caseDocument(List.of(instance("M1", "Melding", "\"ruwe meldpremie\":{\"number\":\"8\"}", "", ""),
                                instance("M2", "Melding", "\"ruwe meldpremie\":{\"number\":\"3\"}", "", "")), expected, "")),
                "rustig na dringend, op het eigen type");
        String jutter = objectType("Jutter", "Jutters", true, List.of(attribute("premie"), attribute("bonuspremie")),
                List.of("ijverig"));
        String notIjverig = "{\"node\":\"CharacteristicCheck\",\"characteristic\":\"ijverig\",\"negated\":true,"
                + "\"source\":{\"snippet\":\"hij niet ijverig is\"}}";
        List<String> rules = List.of(rule("bonuspremie", "Jutter", "bonuspremie", number(20), notIjverig, "calculation"),
                rule("premie uit bonus", "Jutter", "premie", own("bonuspremie", "Jutter"), null, "equation"), rest);
        String farSideGroup = characteristicGroup("dringendheid", "Melding", "dringend",
                decision(greater(viaVondstJutter("premie"), number(15)), false), decision(ALTIJD, true));
        List<String> inst = List.of(instance("J1", "Jutter", "", "", ""), instance("J2", "Jutter", "", "", "\"ijverig\""),
                vondst("V1", "J1", "", false), vondst("V2", "J2", "", false),
                instance("M1", "Melding", "", "\"vondst\":[\"V1\"]", ""),
                instance("M2", "Melding", "", "\"vondst\":[\"V2\"]", ""));
        assertNull(play(document(model(List.of(jutter, VONDST, message), List.of(JUTTER_VONDST, VONDST_MESSAGE), ""),
                        rules, List.of(farSideGroup)), caseDocument(inst, expected, "")),
                "rustig na dringend, twee rollen ver in de band van een negatielezer");
    }

    // ---------------- a parameter ----------------

    @Test void a_parameter_in_the_justification() throws Exception {
        // "… als 20 indien het aantal vondsten van de Jutter groter is dan de vondstdrempel", vondstdrempel 2, and
        // "Uitzondering op bonus: … als 2 indien het gewicht van de Vondst groter is dan de gewichtsgrens",
        // gewichtsgrens 5. ref and clingo: J1 20, the other jutters 10; the bonus 2 where the gewicht exceeds 5.
        String model = model(List.of(JUTTER, VONDST), List.of(JUTTER_VONDST),
                parameter("vondstdrempel") + "," + parameter("gewichtsgrens"));
        String bonus = group("bonus", "Vondst", "bonus",
                defaultEntry(greater(own("gewicht", "Vondst"), parameterRef("gewichtsgrens")), number(2)),
                defaultEntry(ALTIJD, number(1)));
        String ir = document(model, List.of(), List.of(premieGroup(greater(count(null), parameterRef("vondstdrempel"))), bonus));
        StringBuilder expected = new StringBuilder(premies("J1"));
        for (String[] v : VONDSTEN)
            expected.append(",").append(value(v[0], "bonus", Integer.parseInt(v[2]) > 5 ? 2 : 1));
        assertNull(play(ir, caseDocument(vondstPicture(true), expected.toString(),
                        "\"vondstdrempel\":{\"number\":\"2\"},\"gewichtsgrens\":{\"number\":\"5\"}")),
                "de vondstdrempel en de gewichtsgrens");
    }

    // ---------------- one group reads what another group writes ----------------

    @Test void a_group_reads_the_target_of_a_group_that_comes_after_it() throws Exception {
        // The premie of a Jutter from a group (10, and 20 where its drempel is above 0). One role away, the
        // Vondst type declared first: "Een Vondst is zwaar", "Uitzondering: Een Vondst is niet zwaar indien de premie
        // van de jutter van de Vondst groter is dan 15". On the Jutter itself, declared first: "Een Jutter is
        // ijverig", "Uitzondering: niet ijverig indien de premie van de Jutter groter is dan 15". ref and clingo: V1
        // (jutter J1, drempel 1) not zwaar, V2 zwaar; J1 not ijverig, J2 ijverig.
        String premie = group("premie", "Jutter", "premie",
                defaultEntry(greater(own("drempel", "Jutter"), number(0)), number(20)), defaultEntry(ALTIJD, number(10)));
        String zwaar = characteristicGroup("zwaarte", "Vondst", "zwaar",
                decision(greater(viaJutter("premie"), number(15)), false), decision(ALTIJD, true));
        String model = model(List.of(VONDST, JUTTER), List.of(JUTTER_VONDST), "");
        List<String> inst = List.of(vondst("V1", "J1", "", false), vondst("V2", "J2", "", false),
                instance("J1", "Jutter", "\"drempel\":{\"number\":\"1\"}", "", ""), instance("J2", "Jutter", "", "", ""));
        assertNull(play(document(model, List.of(), List.of(zwaar, premie)), caseDocument(inst,
                        String.join(",", characteristic("V1", "zwaar", false), characteristic("V2", "zwaar", true),
                                value("J1", "premie", 20), value("J2", "premie", 10)), "")),
                "een rol ver, de lezende groep eerst");
        String ijver = characteristicGroup("ijver", "Jutter", "ijverig",
                decision(greater(own("premie", "Jutter"), number(15)), false), decision(ALTIJD, true));
        assertNull(play(document(model(List.of(JUTTER, VONDST), List.of(JUTTER_VONDST), ""), List.of(),
                        List.of(ijver, premie)),
                caseDocument(List.of(instance("J1", "Jutter", "\"drempel\":{\"number\":\"1\"}", "", ""),
                                instance("J2", "Jutter", "", "", "")),
                        String.join(",", characteristic("J1", "ijverig", false), characteristic("J2", "ijverig", true)), "")),
                "op het eigen type, de lezende groep eerst");
    }

    @Test void unordered_without_premature_conflict() throws Exception {
        // "Uitzonderingen ongeordend": premie 20 indien het aantal vondsten van de Jutter groter is dan 1, and 30
        // indien het aantal vondsten van de Jutter die zwaar zijn kleiner is dan 2, with zwaar derived after the
        // group. Before zwaar is derived the count of zware vondsten is 0 and both grounds hold, which is a conflict
        // the fixpoint does not have. ref and clingo: J4 (two zware) 20 and consistent; J5 (one zware) no premie
        // and inconsistent.
        String a = groupUnordered("grond veel vondsten", defaultEntry(greater(count(null), number(1)), number(20)));
        String b = groupUnordered("grond weinig zware vondsten",
                defaultEntry(comparison("less", count(characteristicCheck("zwaar")), number(2)), number(30)));
        String ir = document(VONDSTMODEL, List.of(ZWAARTE), List.of(a, b));
        assertNull(play(ir, caseDocument(List.of(instance("J4", "Jutter", "", "", ""),
                        vondst("V5", "J4", "\"gewicht\":{\"number\":\"7\"}", false),
                        vondst("V6", "J4", "\"gewicht\":{\"number\":\"8\"}", false)),
                        value("J4", "premie", 20) + ",{\"kind\":\"consistent\"}", "")), "zonder conflict");
        assertNull(play(ir, caseDocument(List.of(instance("J5", "Jutter", "", "", ""),
                        vondst("V7", "J5", "\"gewicht\":{\"number\":\"3\"}", false),
                        vondst("V8", "J5", "\"gewicht\":{\"number\":\"12\"}", false)),
                        "{\"kind\":\"attributeEmpty\",\"label\":\"J5\",\"attribute\":\"premie\"},{\"kind\":\"inconsistent\"}", "")),
                "met conflict");
    }

    // ---------------- documents ----------------

    private static final String WHOLE = "{\"node\":\"Number\",\"whole\":true,\"decimals\":0,\"sign\":\"unspecified\"}";
    private static final String NIET_NEGATIEF = "{\"node\":\"Number\",\"whole\":true,\"decimals\":0,\"sign\":\"nonNegative\"}";
    private static final String ALTIJD = "{\"node\":\"Compound\",\"quantifier\":\"all\",\"threshold\":0,\"parts\":[],"
            + "\"source\":{\"snippet\":\"altijd\"}}";

    private static final String JUTTER = objectType("Jutter", "Jutters", true,
            List.of(attribute("premie"), attribute("drempel"), attribute("ruwe premie")), List.of("ijverig"));
    private static final String VONDST = objectType("Vondst", "Vondsten", false,
            List.of(attribute("gewicht"), attribute("ruwgewicht"), attribute("bonus")), List.of("zwaar"));
    private static final String MESSAGE = objectType("Melding", "Meldingen", false,
            List.of(attribute("gemelde premie")), List.of("dringend"));
    private static final String JUTTER_VONDST = factType("JutterHeeftVondst", "jutter", "Jutter", "vondst", "vondsten", "Vondst");
    private static final String VONDST_MESSAGE = factType("VondstHeeftMelding", "vondst", "Vondst", "melding", "meldingen", "Melding");
    private static final String VONDSTMODEL = model(List.of(JUTTER, VONDST), List.of(JUTTER_VONDST), "");
    private static final String REPORT_MODEL = model(List.of(JUTTER, VONDST, MESSAGE), List.of(JUTTER_VONDST, VONDST_MESSAGE), "");

    /** "Een Vondst is zwaar indien het gewicht van de Vondst groter is dan 5." */
    private static final String ZWAARTE = rule("zwaarte", "Vondst", "zwaar", "{\"node\":\"Literal\",\"value\":{\"boolean\":true},"
            + "\"type\":{\"node\":\"Boolean\"},\"source\":{\"snippet\":\"waar\"}}", greater(own("gewicht", "Vondst"), number(5)), null);
    /** "Het gewicht van een Vondst moet berekend worden als het ruwgewicht van de Vondst min 1." */
    private static final String WEIGHT = rule("gewicht", "Vondst", "gewicht", "{\"node\":\"Binary\",\"op\":\"minus\",\"left\":"
            + own("ruwgewicht", "Vondst") + ",\"right\":" + number(1) + ",\"type\":" + WHOLE
            + ",\"source\":{\"snippet\":\"het ruwgewicht van de Vondst min 1\"}}", null, "calculation");
    /** "De premie van een Jutter moet gesteld worden op de ruwe premie van de Jutter." */
    private static final String PREMIE_OUT_RAW_PREMIE = rule("premie", "Jutter", "premie", own("ruwe premie", "Jutter"), null,
            "equation");

    /** label, jutter, gewicht, zwaar in the input rows. */
    private static final String[][] VONDSTEN = {
            {"V1", "J1", "6", "zwaar"}, {"V2", "J1", "4", ""}, {"V3", "J1", "9", "zwaar"}, {"V4", "J2", "2", ""},
            {"V5", "J4", "7", "zwaar"}, {"V6", "J4", "8", "zwaar"}, {"V7", "J5", "3", ""}, {"V8", "J5", "12", ""}};

    private static String attribute(String name) {
        return "{\"node\":\"Attribute\",\"name\":\"" + name + "\",\"datatype\":" + WHOLE + "}";
    }

    private static String objectType(String name, String plural, boolean animate, List<String> attributes, List<String> characteristics) {
        List<String> k = new ArrayList<>();
        for (String n : characteristics) k.add("{\"node\":\"Characteristic\",\"name\":\"" + n + "\",\"kind\":\"adjectival\"}");
        return "{\"node\":\"ObjectType\",\"name\":\"" + name + "\",\"animate\":" + animate + ",\"plural\":\"" + plural
                + "\",\"attributes\":[" + String.join(",", attributes) + "],\"characteristics\":[" + String.join(",", k) + "]}";
    }

    private static String factType(String name, String role1, String type1, String role2, String plural2, String type2) {
        return "{\"node\":\"FactType\",\"name\":\"" + name + "\",\"reciprocal\":false,\"roles\":["
                + "{\"node\":\"Role\",\"name\":\"" + role1 + "\",\"objectType\":\"" + type1 + "\",\"many\":false},"
                + "{\"node\":\"Role\",\"name\":\"" + role2 + "\",\"objectType\":\"" + type2 + "\",\"plural\":\"" + plural2
                + "\",\"many\":true}]}";
    }

    private static String parameter(String name) {
        return "{\"node\":\"Parameter\",\"name\":\"" + name + "\",\"datatype\":" + WHOLE + "}";
    }

    private static String model(List<String> types, List<String> factTypes, String parameters) {
        return "{\"objectTypes\":[" + String.join(",", types) + "],\"factTypes\":[" + String.join(",", factTypes)
                + "],\"parameters\":[" + parameters + "],\"domains\":[],\"unitSystems\":[],\"dimensions\":[],"
                + "\"timelines\":[],\"dayKinds\":[]}";
    }

    private static String number(int n) {
        return "{\"node\":\"Literal\",\"value\":{\"number\":\"" + n + "\"},\"type\":" + WHOLE + ",\"source\":{\"snippet\":\"" + n + "\"}}";
    }

    private static String own(String attribute, String type) {
        return "{\"node\":\"AttributeReference\",\"attribute\":\"" + attribute + "\",\"subjectType\":\"" + type
                + "\",\"source\":{\"snippet\":\"de " + attribute + " van de " + type + "\"}}";
    }

    private static String parameterRef(String name) {
        return "{\"node\":\"ParameterReference\",\"parameter\":\"" + name + "\",\"source\":{\"snippet\":\"de " + name + "\"}}";
    }

    /** A navigated read of {@code attribute} on {@code type} over the hops, each "FactType/role". */
    private static String path(String attribute, String type, String snippet, String... hops) {
        List<String> h = new ArrayList<>();
        for (String hop : hops) {
            String[] d = hop.split("/");
            h.add("{\"node\":\"RoleBinding\",\"factType\":\"" + d[0] + "\",\"role\":\"" + d[1] + "\"}");
        }
        return "{\"node\":\"AttributeReference\",\"attribute\":\"" + attribute + "\",\"subjectType\":\"" + type
                + "\",\"source\":{\"snippet\":\"" + snippet + "\"},\"path\":[" + String.join(",", h) + "]}";
    }

    /** "de &lt;attribuut&gt; van de jutter van de Vondst": one hop. */
    private static String viaJutter(String attribute) {
        return path(attribute, "Jutter", "de " + attribute + " van de jutter van de Vondst", "JutterHeeftVondst/jutter");
    }

    /** "de &lt;attribuut&gt; van de jutter van de vondst van de Melding": two hops. */
    private static String viaVondstJutter(String attribute) {
        return path(attribute, "Jutter", "de " + attribute + " van de jutter van de vondst van de Melding",
                "VondstHeeftMelding/vondst", "JutterHeeftVondst/jutter");
    }

    private static String characteristicCheck(String characteristic) {
        return "{\"node\":\"CharacteristicCheck\",\"characteristic\":\"" + characteristic + "\",\"negated\":false,"
                + "\"source\":{\"snippet\":\"" + characteristic + " zijn\"}}";
    }

    private static final String VONDST_RANGE = "\"elementType\":\"Vondst\",\"sourceRole\":{\"node\":\"RoleBinding\","
            + "\"factType\":\"JutterHeeftVondst\",\"role\":\"vondst\"}";

    /** "het aantal vondsten van de Jutter [die … zijn]". */
    private static String count(String filter) {
        return "{\"node\":\"Aggregation\",\"kind\":\"count\",\"arguments\":[" + own("vondsten", "Jutter") + "],"
                + VONDST_RANGE + (filter == null ? "" : ",\"filter\":" + filter) + ",\"type\":" + NIET_NEGATIEF
                + ",\"source\":{\"snippet\":\"het aantal vondsten van de Jutter\"}}";
    }

    /** "de som van / de minimale waarde van / de maximale waarde van het &lt;attribuut&gt; van alle vondsten van de Jutter". */
    private static String fold(String category, String attribute, String filter) {
        return "{\"node\":\"Aggregation\",\"kind\":\"" + category + "\",\"arguments\":[" + own(attribute, "Vondst") + "],"
                + VONDST_RANGE + (filter == null ? "" : ",\"filter\":" + filter) + ",\"type\":" + WHOLE
                + ",\"source\":{\"snippet\":\"de " + category + " van het " + attribute + " van alle vondsten van de Jutter\"}}";
    }

    private static String greater(String left, String right) { return comparison("greater", left, right); }

    private static String comparison(String op, String left, String right) {
        return "{\"node\":\"Comparison\",\"op\":\"" + op + "\",\"left\":" + left + ",\"right\":" + right
                + ",\"source\":{\"snippet\":\"vergelijking\"}}";
    }

    /** An ordinary rule with one version; {@code condition} null for none, {@code presentation} null on a
     *  characteristic assignment ("calculation" for "berekend worden als", "equation" for "gesteld worden op"). */
    private static String rule(String name, String type, String target, String value, String condition, String presentation) {
        return "{\"node\":\"Rule\",\"name\":\"" + name + "\",\"versions\":[{\"validity\":{\"node\":\"Validity\"},"
                + "\"target\":{\"node\":\"AttributeReference\",\"attribute\":\"" + target + "\",\"subjectType\":\"" + type + "\","
                + "\"source\":{\"snippet\":\"de " + target + "\"}},\"value\":" + value
                + (condition == null ? "" : ",\"condition\":" + condition) + ",\"initialisation\":false"
                + (presentation == null ? "" : ",\"presentation\":\"" + presentation + "\"") + "}]}";
    }

    private static String defaultEntry(String justification, String consequence) {
        return "{\"node\":\"Default\",\"justification\":" + justification + ",\"consequence\":" + consequence + "}";
    }

    private static String decision(String justification, boolean decision) {
        return "{\"node\":\"Default\",\"decision\":" + decision + ",\"justification\":" + justification + "}";
    }

    private static String group(String name, String type, String attribute, String... defaults) {
        return "{\"node\":\"PrioritizedDefaults\",\"name\":\"" + name + "\",\"subject\":\"" + type + "\",\"attribute\":\""
                + attribute + "\",\"defaults\":[" + String.join(",", defaults) + "],\"validity\":{\"node\":\"Validity\"}}";
    }

    private static String groupUnordered(String name, String... defaults) {
        return "{\"node\":\"PrioritizedDefaults\",\"name\":\"" + name + "\",\"subject\":\"Jutter\",\"attribute\":\"premie\","
                + "\"unordered\":true,\"defaults\":[" + String.join(",", defaults) + "],\"validity\":{\"node\":\"Validity\"}}";
    }

    private static String characteristicGroup(String name, String type, String characteristic, String... defaults) {
        return "{\"node\":\"PrioritizedDefaults\",\"name\":\"" + name + "\",\"subject\":\"" + type + "\",\"characteristic\":\""
                + characteristic + "\",\"defaults\":[" + String.join(",", defaults) + "],\"validity\":{\"node\":\"Validity\"}}";
    }

    /** The premie of a Jutter: 20 where {@code justification} holds, else 10. */
    private static String premieGroup(String justification) {
        return group("premie", "Jutter", "premie", defaultEntry(justification, number(20)), defaultEntry(ALTIJD, number(10)));
    }

    /** The gemelde premie of a Melding: 20 where {@code justification} holds, else 10. */
    private static String reportedGroup(String justification) {
        return group("gemelde premie", "Melding", "gemelde premie", defaultEntry(justification, number(20)), defaultEntry(ALTIJD, number(10)));
    }

    private static String document(String model, List<String> rules, List<String> groups) {
        return "{\"format\":\"uchar-ir-v1\",\"model\":" + model + ",\"rules\":{\"rules\":[" + String.join(",", rules) + "],"
                + "\"dayKindDefinitions\":[],\"objectCreations\":[],\"allocations\":[],\"consistencies\":[],"
                + "\"uniquenesses\":[],\"factCreations\":[],\"startPointDeterminations\":[],\"ruleGroups\":[],"
                + "\"prioritizedDefaults\":[" + String.join(",", groups) + "]},\"diagnostics\":[]}";
    }

    // ---------------- cases ----------------

    private static String instance(String label, String type, String attributes, String roles, String characteristics) {
        return "{\"label\":\"" + label + "\",\"objectType\":\"" + type + "\",\"attributes\":{" + attributes + "},\"roles\":{"
                + roles + "},\"characteristics\":[" + characteristics + "]}";
    }

    private static String vondst(String label, String jutter, String attributes, boolean zwaar) {
        return instance(label, "Vondst", attributes, jutter == null ? "" : "\"jutter\":[\"" + jutter + "\"]",
                zwaar ? "\"zwaar\"" : "");
    }

    private static List<String> jutters(List<String> vondsten) {
        List<String> out = new ArrayList<>();
        for (int i = 1; i <= 5; i++) out.add(instance("J" + i, "Jutter", "", "", ""));
        out.addAll(vondsten);
        return out;
    }

    /** The jutters J1..J5 and the vondsten of {@link #VONDSTEN}, zwaar as input where {@code withZwaar}. */
    private static List<String> vondstPicture(boolean withZwaar) {
        List<String> vondsten = new ArrayList<>();
        for (String[] v : VONDSTEN)
            vondsten.add(vondst(v[0], v[1], "\"gewicht\":{\"number\":\"" + v[2] + "\"}", withZwaar && !v[3].isEmpty()));
        return jutters(vondsten);
    }

    /** J1 (premie or ruwe premie 8), J2 (3), J3; V1 of J1, V2 of J2, V3 without jutter; M1..M3 of V1..V3, M4 alone. */
    private static List<String> reportInstances(String premieAttribute) {
        return List.of(instance("J1", "Jutter", "\"" + premieAttribute + "\":{\"number\":\"8\"}", "", ""),
                instance("J2", "Jutter", "\"" + premieAttribute + "\":{\"number\":\"3\"}", "", ""),
                instance("J3", "Jutter", "", "", ""),
                vondst("V1", "J1", "", false), vondst("V2", "J2", "", false), vondst("V3", null, "", false),
                instance("M1", "Melding", "", "\"vondst\":[\"V1\"]", ""),
                instance("M2", "Melding", "", "\"vondst\":[\"V2\"]", ""),
                instance("M3", "Melding", "", "\"vondst\":[\"V3\"]", ""),
                instance("M4", "Melding", "", "", ""));
    }

    private static List<String> messagePicture(String premieAttribute) { return reportInstances(premieAttribute); }

    private static String value(String label, String attribute, int number) {
        return "{\"kind\":\"attributeValue\",\"label\":\"" + label + "\",\"attribute\":\"" + attribute
                + "\",\"value\":{\"number\":\"" + number + "\"}}";
    }

    private static String characteristic(String label, String characteristic, boolean present) {
        return "{\"kind\":\"characteristic" + (present ? "Present" : "Absent") + "\",\"label\":\"" + label
                + "\",\"characteristic\":\"" + characteristic + "\"}";
    }

    /** The premie of J1..J5: 20 for the named jutters, 10 for the others. */
    private static String premies(String... twenty) {
        List<String> t = List.of(twenty), out = new ArrayList<>();
        for (int i = 1; i <= 5; i++) out.add(value("J" + i, "premie", t.contains("J" + i) ? 20 : 10));
        return String.join(",", out);
    }

    /** The gemelde premie of M1..M4: 20 for the named meldingen, 10 for the others. */
    private static String reported(String... twenty) {
        List<String> t = List.of(twenty), out = new ArrayList<>();
        for (int i = 1; i <= 4; i++) out.add(value("M" + i, "gemelde premie", t.contains("M" + i) ? 20 : 10));
        return String.join(",", out);
    }

    private static String caseDocument(List<String> instances, String expected, String parameters) {
        return "{\"format\":\"uchar-case-v0\",\"asOfDate\":null,\"input\":{\"instances\":[" + String.join(",", instances)
                + "],\"parameters\":{" + parameters + "}},\"expected\":[" + expected + "]}";
    }

    /** The verdict on one case of the exec-model branch and of {@code DRL_TEXT}, which must be the same: null when
     *  every expectation holds. */
    private static String play(String ir, String caseDocument) throws Exception {
        Path tmp = Files.createTempFile("defaults-rechtvaardiging", ".json");
        tmp.toFile().deleteOnExit();
        Files.writeString(tmp, ir);
        JsonNode c = JSON.readTree(caseDocument);
        String exec = CaseRunner.playCase(tmp, c, DroolsBackend.Branch.EXEC_MODEL);
        assertEquals(exec, CaseRunner.playCase(tmp, c, DroolsBackend.Branch.DRL_TEXT), "DRL_TEXT naast de exec-model-tak");
        return exec;
    }
}
