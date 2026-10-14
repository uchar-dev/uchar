package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;

import dev.uchar.regelspraak.graaf.ProducentConsument.ElementSleutel;
import dev.uchar.regelspraak.transpiler.tms.Emmerinvoer;

import org.drools.model.Rule;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static dev.uchar.regelspraak.cases.TmsDocumenten.aggregatie;
import static dev.uchar.regelspraak.cases.TmsDocumenten.bronrol;
import static dev.uchar.regelspraak.cases.TmsDocumenten.check;
import static dev.uchar.regelspraak.cases.TmsDocumenten.consistent;
import static dev.uchar.regelspraak.cases.TmsDocumenten.feittypeEenNaarVeel;
import static dev.uchar.regelspraak.cases.TmsDocumenten.getal;
import static dev.uchar.regelspraak.cases.TmsDocumenten.getalwaarde;
import static dev.uchar.regelspraak.cases.TmsDocumenten.inconsistent;
import static dev.uchar.regelspraak.cases.TmsDocumenten.kenmerkregel;
import static dev.uchar.regelspraak.cases.TmsDocumenten.lezing;
import static dev.uchar.regelspraak.cases.TmsDocumenten.rekenregel;
import static dev.uchar.regelspraak.cases.TmsDocumenten.vergelijking;
import static dev.uchar.regelspraak.cases.TmsDocumenten.waarde;
import static dev.uchar.regelspraak.cases.TmsTijdlijnDocumenten.GEHEEL;
import static dev.uchar.regelspraak.cases.TmsTijdlijnDocumenten.dagentelling;
import static dev.uchar.regelspraak.cases.TmsTijdlijnDocumenten.eigenTotaal;
import static dev.uchar.regelspraak.cases.TmsTijdlijnDocumenten.instantie;
import static dev.uchar.regelspraak.cases.TmsTijdlijnDocumenten.kenmerk;
import static dev.uchar.regelspraak.cases.TmsTijdlijnDocumenten.maanden;
import static dev.uchar.regelspraak.cases.TmsTijdlijnDocumenten.objecttype;
import static dev.uchar.regelspraak.cases.TmsTijdlijnDocumenten.periode;
import static dev.uchar.regelspraak.cases.TmsTijdlijnDocumenten.segment;
import static dev.uchar.regelspraak.cases.TmsTijdlijnDocumenten.tijdattribuut;
import static dev.uchar.regelspraak.cases.TmsTijdlijnDocumenten.tijdkenmerk;
import static dev.uchar.regelspraak.cases.TmsTijdlijnDocumenten.tijdlijn;
import static dev.uchar.regelspraak.cases.TmsTijdlijnDocumenten.tijdsevenredig;
import static dev.uchar.regelspraak.cases.TmsTijdlijnDocumenten.verwachteTijdlijn;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The two conventions of the case input of an attribute on a timeline on the exec-model branch (RUNTIME-SEMANTICS
 * ch. 8.4): the asOfDate-bucket scalar, one value for the period of the attribute's grid that holds the evaluation date,
 * serves the subject's own total without a clip (ch. 3.6); the full timeline serves every other read; "an executor that
 * needs the one and finds the other faults — never guesses". The case runner states each bucket scalar beside the bean
 * ({@link Emmerinvoer}), the branch reads the input through the attribute's standing in the convention the case gives
 * it, and a form that finds the other convention faults where the evaluation reaches it, as a logical fault that counts
 * at the fixpoint. Every case has the evaluation date 2024-06-01, so a bucket scalar on a month grid is June 2024 and
 * on a year grid 2024. Each part runs in every order of its rules in the document.
 *
 * <p><b>A, the base of a pro-rata part</b> (ch. 3.7), on the chain of {@link TmsTijdlijnWitnessTest} part A: J1 (score
 * 10, so actief, which a rule derives) has its jaartarief 366 as a bucket scalar, and the jaardeel rule reaches the base:
 * the fault names J1. J2 (score 3, not actief) has the same scalar, and the rule's condition keeps the base unread, so
 * no fault. J3 brings a full timeline and plays as in that witness.
 *
 * <p><b>B, a fault on an intermediate state</b>: the jaardeel "indien hij niet vrijgesteld is", with vrijgesteld derived
 * later; J1 (score 3) has the jaartarief as a bucket scalar and is vrijgesteld at the fixpoint, so the session plays. In
 * the generated rules in a hundred sampled orders, J1 alone as the case runner sets it up, no fault stands at the
 * fixpoint, and in some order the jaardeel rule reached the base before J1 was vrijgesteld, and its fault went again.
 *
 * <p><b>C, the subject's own total without a clip</b> (ch. 3.6), "het totaal van zijn bedrag, indien hij actief is": J1
 * (actief, a full timeline) faults; J2 (not actief, a full timeline) is not reached; J3 (actief, the bedrag 7 as a bucket
 * scalar) totals 7, one month of 7; J4 (actief, no bedrag) totals leeg. Without J1 the case plays.
 *
 * <p><b>D, the clip of a total</b> (ch. 3.6): "het totaal van zijn vergoeding gedurende de tijd dat hij wachtplichtig
 * is" and "… gedurende de tijd dat zijn controle groter is dan 5", where "De controle … als het tijdsevenredig deel per
 * maand van zijn jaarcontrole gedurende de tijd dat hij borgplichtig is" is the controle's only writer, so its input
 * stands where it computes nothing (ch. 7.8). J1 has the vergoeding 11 as a bucket scalar: both totals fault on the
 * clipped timeline, before the clause. J2 has the vergoeding 10 a month through 2024 and the controle 7 as a bucket
 * scalar, without a true period, so the pro-rata part is empty and the scalar stands: the comparison clip faults on the
 * timeline it compares. J3 is J2 with the jaarcontrole 12 in 2024 and borgplichtig March–May: the derived controle, 12 in
 * March, April and May, replaces the scalar, so topsom = 3 × 10 = 30 and wachtsom 0; alone, J3 plays.
 *
 * <p><b>E, a member of a per-element fold</b> (ch. 3.6, §7.1): "het totaal van de tarieven van zijn vondsten die gekeurd
 * zijn", gekeurd derived from the keurscore, and the tarief derived by a pro-rata part that is its only writer. J1 keeps
 * V1 (a derived tarief) and V2 (the tarief 5 as a bucket scalar, gekeurd): the fault names V2. J4 keeps V6 and V5, written
 * in that order, both bucket scalars and gekeurd: the fault names V6, the first in fold order. J2 keeps V3, a bucket
 * scalar that is not gekeurd, so the filter keeps it out: last 0. J3 keeps V4, whose bucket scalar 7 the pro-rata part
 * replaces with 12 a month January–March: last 36. J2 and J3 alone play.
 *
 * <p><b>F, an ever-unique key</b> (ch. 5.9), the nummer as a day count "gedurende de tijd dat hij bewaard is, indien hij
 * geregistreerd is" ({@link TmsOoitUniciteitWitnessTest}): V1 (geregistreerd, bewaard ten days in January 2024, the
 * nummer 7 as a bucket scalar) gets 10 in 2024, which replaces the scalar; V4 gets 10 in 2025, so the two collide; V2
 * (not geregistreerd) keeps its scalar 7, and V3 (geregistreerd, no true period, so the day count is empty) keeps its
 * scalar 5: each faults. V1 and V4 alone play, both on the channel.
 *
 * <p><b>G, the key's form</b> (ch. 5.9), two uniquenesses in both orders: "kavel per kust", scoped by the kust, over the
 * kavelnummer; S1 has no kust and the kavelnummer 3 as a bucket scalar, and faults all the same ("this form error
 * precedes the scope narrowing"). "code en nummer" over the code and the vondstnummer: S2 has no code and the
 * vondstnummer as a bucket scalar, and does not fault, since the reference interpreter builds a row's tuple component by
 * component and stops at an empty one; S3 has the code 1 and faults.
 *
 * <p><b>H, a characteristic on a timeline in the flat list</b>: in the clip of a total, the condition of a pro-rata part
 * and the condition of a day count, wachtplichtig given as "characteristics": ["wachtplichtig"] instead of its true
 * periods. The case reader refuses it at load in every branch (maintainer decision 2026-09-09, ch. 8.4), so this branch
 * reads no characteristic in that convention.
 *
 * <p><b>Measured</b> (2026-10-09, every document of {@link #main} in every order, 143 documents, 177 cases): the verifier
 * passes all. The reference interpreter's runner and the ASP runner, case by case:
 * <ul>
 * <li>A: both pass every order, with J1's jaardeel never written: they read a base given as a bucket scalar as no
 * timeline. This branch follows ch. 8.4 and faults; the reference interpreter and the ASP backend follow with the card
 * proratabasis-als-emmerscalair.
 * <li>B: both pass.
 * <li>C: the reference interpreter faults, "the temporal total of "bedrag" over a case-supplied full timeline is not
 * exercised by any corpus case; the asOfDate-bucket convention carries the total", and passes the case without J1; the
 * ASP runner refuses both in those words, the second one for J2, whose full timeline the reference interpreter never
 * reaches: that translation is eager, this interpreter lazy (ch. 3.6).
 * <li>D: on J1 both fault, "the gedurende clip of "vergoeding" needs the full timeline; the case supplies only the
 * asOfDate-bucket scalar", the ASP runner naming "J1" in place of "the case"; both pass J3 alone. On J2 alone both pass,
 * with topsom 0: they read the compared timeline given as a bucket scalar as no timeline. This branch follows ch. 8.4
 * and faults there as well; reported with part A.
 * <li>E: the reference interpreter faults, "the per-element temporal total of "tarief" over "V2": the case supplies an
 * asOfDate-bucket scalar, …", and on J4 alone over "V6"; it passes J2 and J3 alone in five orders and faults over "V4" in
 * the sixth (keuring, last, tarief), where it reads the fold before the pro-rata part replaced the scalar: the class of
 * the card fout-op-tussenstand. The ASP runner refuses every case of E, "the per-element temporal fold of "tarief" over a
 * DERIVED member timeline is not translated: …".
 * <li>F: the reference interpreter faults, "the ever-unique key "nummer" needs the full timeline; "V2" supplies only a
 * scalar (ch. 5.9)", and passes V1 and V4 alone; the ASP runner refuses both cases over "V1", whose scalar it checks
 * before the day count replaces it.
 * <li>G: with "kavel per kust" first both fault over "S1"; with "code en nummer" first the reference interpreter faults
 * over "S3", and the ASP runner over "S2", whose empty code it does not stop at.
 * <li>H: both refuse the case at load, "CASE ERROR: J1.wachtplichtig: characteristic "wachtplichtig" is declared
 * time-dependent on "Jutter", so it is delivered as a "characteristicTimelines" entry — …".
 * </ul>
 * Where several objects fault, the reference interpreter stops at the first it meets, and this branch reports every
 * fault that stands at the fixpoint ({@code Evaluatiefouten}), each in the reference interpreter's words with the object
 * named.
 *
 * <p><b>Red</b> on c08ac038, the code before the change, with only the class {@link Emmerinvoer} added so that this
 * witness compiles: seven of the thirteen tests fail. A: "case: attribuut jaardeel: verwacht (leeg), werkelijk
 * [2024-01-01,2025-01-01)=306; …", the scalar read as the timeline of 2024. C, D and E: "gespeeld", the full timeline
 * totalled, the scalar clipped, the scalar summed. F: "case: verwacht consistent, werkelijk inconsistent
 * (eenmalig nummer &#64; V1, eenmalig nummer &#64; V4)", the scalars read as keys. G: "[gespeeld, gespeeld]". B: no order
 * saw a fault on the way.
 * The other six pass there as well and here: the cases that play, and H, which the case reader refused already.
 */
class TmsInvoervormWitnessTest {

    /** The evaluation date of every case of this class: a scalar input on a temporal attribute lies in its bucket. */
    static final String DATUM = "2024-06-01";

    /** The outcomes of {@code zaak} on the document of {@code model} with {@code regels}, in every order of the rules,
     *  each as "[names] outcome" ({@link TmsFoutWitnessTest#speel}), beside the expected list with {@code verwacht}. */
    static void inElkeVolgorde(String model, List<String> regels, List<String> uniciteiten, JsonNode zaak,
                               String verwacht) throws Exception {
        List<String> uitkomsten = new ArrayList<>(), verwachting = new ArrayList<>();
        for (List<String> volgorde : TmsDocumenten.permutaties(regels)) {
            List<String> namen = TmsDocumenten.regelnamen(volgorde);
            Path ir = TmsTijdlijnDocumenten.document(model, volgorde, uniciteiten);
            uitkomsten.add(namen + " " + TmsFoutWitnessTest.speel(ir, zaak, DroolsBackend.Branch.EXEC_MODEL));
            verwachting.add(namen + " " + verwacht);
        }
        assertEquals(verwachting, uitkomsten);
    }

    // ---------------- A: the base of a pro-rata part ----------------

    static final List<String> REGELS_A = TmsTijdlijnWitnessTest.REGELS_A;
    static final Map<String, List<String>> BORG = TmsTijdlijnWitnessTest.BORG;

    static JsonNode zaakA() throws Exception {
        return TmsTijdlijnDocumenten.zaak(DATUM, List.of(
                        instantie("J1", "Jutter", Map.of("score", getalwaarde("10"), "jaartarief", getalwaarde("366"),
                                "bedrag", TmsTijdlijnWitnessTest.BEDRAG), List.of(), BORG, Map.of()),
                        instantie("J2", "Jutter", Map.of("score", getalwaarde("3"), "jaartarief", getalwaarde("366"),
                                "bedrag", TmsTijdlijnWitnessTest.BEDRAG, "maanddeel", TmsTijdlijnWitnessTest.JUNI),
                                List.of(), BORG, Map.of()),
                        instantie("J3", "Jutter", Map.of("score", getalwaarde("10"), "jaartarief",
                                TmsTijdlijnWitnessTest.TARIEF, "maanddeel", TmsTijdlijnWitnessTest.JUNI), List.of(), BORG,
                                Map.of())),
                List.of(verwachteTijdlijn("J1", "jaardeel", List.of()), verwachteTijdlijn("J1", "maanddeel", List.of()),
                        waarde("J1", "wachtsom", "0"), waarde("J1", "topsom", "0"),
                        verwachteTijdlijn("J2", "jaardeel", List.of()),
                        verwachteTijdlijn("J2", "maanddeel", List.of(segment("2024-06-01", "2024-07-01", "9"))),
                        waarde("J2", "wachtsom", "9"), waarde("J2", "topsom", "0"),
                        verwachteTijdlijn("J3", "jaardeel", TmsTijdlijnWitnessTest.JAARDELEN),
                        verwachteTijdlijn("J3", "maanddeel", TmsTijdlijnWitnessTest.maanddelen()),
                        waarde("J3", "wachtsom", "3178"), waarde("J3", "topsom", "0"), consistent()));
    }

    @Test void de_basis_van_een_pro_rata_als_emmerscalair_faalt_waar_de_regel_haar_bereikt() throws Exception {
        inElkeVolgorde(TmsTijdlijnWitnessTest.MODEL_A, REGELS_A, List.of(), zaakA(), "Invoervormfout: fout in de regel"
                + " \"jaardeel\": the pro-rata base \"jaartarief\" needs the full timeline; \"J1\" supplies only the"
                + " asOfDate-bucket scalar (ch. 3.7, ch. 8.4)");
    }

    // ---------------- B: a fault on an intermediate state goes again ----------------

    static JsonNode zaakB() throws Exception {
        return TmsTijdlijnDocumenten.zaak(DATUM, List.of(
                        instantie("J1", "Jutter", Map.of("score", getalwaarde("3"), "jaartarief", getalwaarde("366")),
                                List.of(), BORG, Map.of()),
                        instantie("J2", "Jutter", Map.of("score", getalwaarde("10"), "jaartarief",
                                TmsTijdlijnWitnessTest.TARIEF), List.of(), BORG, Map.of())),
                List.of(verwachteTijdlijn("J1", "jaardeel", List.of()),
                        verwachteTijdlijn("J2", "jaardeel", TmsTijdlijnWitnessTest.JAARDELEN), consistent()));
    }

    static final List<String> REGELS_B = List.of(TmsTijdlijnWitnessTest.VRIJSTELLING,
            TmsTijdlijnWitnessTest.JAARDEEL_NIET_VRIJ);

    @Test void een_basis_die_de_regel_aan_het_fixpunt_niet_bereikt_faalt_niet() throws Exception {
        inElkeVolgorde(TmsTijdlijnWitnessTest.MODEL_B, REGELS_B, List.of(), zaakB(), "gespeeld");
    }

    /** Part B over the generated rules in sampled orders, J1 alone as the case runner sets it up: the bucket scalar on
     *  the bean and its {@link Emmerinvoer} beside it. At the fixpoint no fault stands and the jaardeel is never
     *  written, and in some order the jaardeel rule reached the base before J1 was vrijgesteld, and its fault went
     *  again. */
    @Test void een_fout_op_een_tussenstand_gaat_weer_weg() throws Exception {
        Path ir = TmsTijdlijnDocumenten.document(TmsTijdlijnWitnessTest.MODEL_B, REGELS_B, List.of());
        DroolsBackend.Build b = DroolsBackend.build(ir, DroolsBackend.Branch.EXEC_MODEL);
        int tussenstand = 0;
        for (List<Rule> volgorde : TmsDocumenten.steekproef(b.execModelTranspiler().model().getRules(), 20261009L, 100)) {
            List<String> namen = volgorde.stream().map(Rule::getName).toList();
            Object j1 = TmsDocumenten.boon(b.beanLoader(), "Jutter", Map.of("score", "3"), List.of());
            TmsTijdlijnDocumenten.zet(j1, "jaartarief", List.of(
                    TmsTijdlijnDocumenten.boonsegment(b.beanLoader(), "2024-01-01", "2025-01-01", "366")));
            TmsTijdlijnDocumenten.zet(j1, "borgplichtig", List.of(
                    TmsTijdlijnDocumenten.boonsegment(b.beanLoader(), "2024-03-01", "2025-03-01", null)));
            Emmerinvoer emmer = new Emmerinvoer(j1, ElementSleutel.van("Jutter", "jaartarief", false));
            TmsLetWitnessTest.Feiten feiten = new TmsLetWitnessTest.Feiten();
            assertNull(TmsDocumenten.fixpunt(b, volgorde, List.of(j1, emmer), sessie -> sessie.addEventListener(feiten)),
                    () -> "volgorde " + namen);
            assertEquals(feiten.ingevoegd, feiten.ingetrokken, () -> "foutfeiten in volgorde " + namen);
            assertNull(TmsDocumenten.lees(j1, "jaardeel"), () -> "jaardeel in volgorde " + namen);
            if (feiten.fouten > 0) tussenstand++;
        }
        assertTrue(tussenstand > 0, "geen volgorde liet de jaardeelregel de basis lezen voor J1 vrijgesteld was");
    }

    // ---------------- C: the subject's own total without a clip ----------------

    static final String MODEL_C = TmsDocumenten.model(List.of(objecttype("Jutter", List.of(
            TmsDocumenten.attribuut("score", GEHEEL), tijdattribuut("bedrag", GEHEEL, "month"),
            TmsDocumenten.attribuut("topsom", GEHEEL)), List.of(kenmerk("actief")))), List.of());

    static final String TOPSOM_ZONDER_KNIP = rekenregel("topsom", "BEREKENING", "Jutter", "topsom",
            eigenTotaal("bedrag", "Jutter", "null"), check("actief", false));

    static final List<String> REGELS_C = List.of(TmsTijdlijnWitnessTest.ACTIEF, TOPSOM_ZONDER_KNIP);

    /** J3 (actief, the bedrag 7 as a bucket scalar), J4 (actief, no bedrag), J2 (not actief, a full timeline), and with
     *  {@code metJ1} J1 (actief, a full timeline). */
    static JsonNode zaakC(boolean metJ1) throws Exception {
        List<String> instanties = new ArrayList<>();
        if (metJ1) instanties.add(instantie("J1", "Jutter", Map.of("score", getalwaarde("10"), "bedrag",
                TmsTijdlijnWitnessTest.BEDRAG), List.of(), Map.of(), Map.of()));
        instanties.add(instantie("J2", "Jutter", Map.of("score", getalwaarde("3"), "bedrag",
                TmsTijdlijnWitnessTest.BEDRAG), List.of(), Map.of(), Map.of()));
        instanties.add(instantie("J3", "Jutter", Map.of("score", getalwaarde("10"), "bedrag", getalwaarde("7")),
                List.of(), Map.of(), Map.of()));
        instanties.add(instantie("J4", "Jutter", Map.of("score", getalwaarde("10")), List.of(), Map.of(), Map.of()));
        return TmsTijdlijnDocumenten.zaak(DATUM, instanties, List.of(waarde("J2", "topsom", null),
                waarde("J3", "topsom", "7"), waarde("J4", "topsom", null)));
    }

    @Test void het_eigen_totaal_zonder_knip_faalt_op_een_volledige_tijdlijn_waar_de_regel_hem_bereikt() throws Exception {
        inElkeVolgorde(MODEL_C, REGELS_C, List.of(), zaakC(true), "Invoervormfout: fout in de regel \"topsom\": the"
                + " temporal total of \"bedrag\" over a case-supplied full timeline is not exercised by any corpus case;"
                + " the asOfDate-bucket convention carries the total, and \"J1\" supplies a full timeline");
    }

    @Test void het_eigen_totaal_zonder_knip_neemt_de_emmerscalair() throws Exception {
        inElkeVolgorde(MODEL_C, REGELS_C, List.of(), zaakC(false), "gespeeld");
    }

    // ---------------- D: the clip of a total, its value and the timeline it compares ----------------

    static final String MODEL_D = TmsDocumenten.model(List.of(objecttype("Jutter", List.of(
            tijdattribuut("vergoeding", GEHEEL, "month"), tijdattribuut("controle", GEHEEL, "month"),
            tijdattribuut("jaarcontrole", GEHEEL, "year"), TmsDocumenten.attribuut("wachtsom", GEHEEL),
            TmsDocumenten.attribuut("topsom", GEHEEL)),
            List.of(tijdkenmerk("wachtplichtig", "month"), tijdkenmerk("borgplichtig", "month")))), List.of());

    /** "De controle van een Jutter moet berekend worden als het tijdsevenredig deel per maand van zijn jaarcontrole
     *  gedurende de tijd dat hij borgplichtig is": its only writer, so the input stands where it computes nothing. */
    static final String CONTROLE = rekenregel("controle", "BEREKENING", "Jutter", "controle",
            tijdsevenredig("jaarcontrole", "Jutter", check("borgplichtig", false), "month"), "null");
    static final String WACHT = rekenregel("wachtsom", "BEREKENING", "Jutter", "wachtsom",
            eigenTotaal("vergoeding", "Jutter", check("wachtplichtig", false)), "null");
    static final String TOP = rekenregel("topsom", "BEREKENING", "Jutter", "topsom",
            eigenTotaal("vergoeding", "Jutter", vergelijking("greater", lezing("controle", "Jutter"), getal("5"))), "null");

    static final List<String> REGELS_D = List.of(CONTROLE, WACHT, TOP);

    static final String VERGOEDING_2024 = tijdlijn(maanden(2024, 1, 12, "10"));

    /** J1: the vergoeding 11 as a bucket scalar, wachtplichtig through 2024. */
    static String j1D() {
        return instantie("J1", "Jutter", Map.of("vergoeding", getalwaarde("11")), List.of(),
                Map.of("wachtplichtig", List.of(periode("2024-01-01", "2025-01-01"))), Map.of());
    }

    /** J2: the vergoeding 10 a month through 2024, the controle 7 as a bucket scalar, no true period. */
    static String j2D() {
        return instantie("J2", "Jutter", Map.of("vergoeding", VERGOEDING_2024, "controle", getalwaarde("7")), List.of(),
                Map.of(), Map.of());
    }

    /** J3: as J2, with the jaarcontrole 12 in 2024 and borgplichtig March–May 2024, so the controle is derived. */
    static String j3D() {
        return instantie("J3", "Jutter", Map.of("vergoeding", VERGOEDING_2024, "controle", getalwaarde("7"),
                        "jaarcontrole", tijdlijn(List.of(segment("2024-01-01", "2025-01-01", "12")))), List.of(),
                Map.of("borgplichtig", List.of(periode("2024-03-01", "2024-06-01"))), Map.of());
    }

    @Test void de_waarde_van_een_knip_en_de_vergeleken_tijdlijn_als_emmerscalair_falen() throws Exception {
        inElkeVolgorde(MODEL_D, REGELS_D, List.of(), TmsTijdlijnDocumenten.zaak(DATUM, List.of(j1D(), j2D(), j3D()),
                        List.of(consistent())),
                "Evaluatiefouten: aan het fixpunt staan 3 evaluatiefouten, in de regels \"topsom\" (2×), \"wachtsom\": fout"
                        + " in de regel \"topsom\": the gedurende clip of \"vergoeding\" compares \"controle\" per segment"
                        + " and needs its full timeline; \"J2\" supplies only the asOfDate-bucket scalar (ch. 3.6, ch."
                        + " 8.4); fout in de regel \"topsom\": the gedurende clip of \"vergoeding\" needs the full timeline;"
                        + " \"J1\" supplies only the asOfDate-bucket scalar; fout in de regel \"wachtsom\": the gedurende"
                        + " clip of \"vergoeding\" needs the full timeline; \"J1\" supplies only the asOfDate-bucket"
                        + " scalar");
    }

    @Test void een_vergeleken_tijdlijn_die_een_regel_afleidt_vervangt_de_emmerscalair() throws Exception {
        inElkeVolgorde(MODEL_D, REGELS_D, List.of(), TmsTijdlijnDocumenten.zaak(DATUM, List.of(j3D()), List.of(
                verwachteTijdlijn("J3", "controle", maanden(2024, 3, 3, "12")), waarde("J3", "topsom", "30"),
                waarde("J3", "wachtsom", "0"))), "gespeeld");
    }

    // ---------------- E: a member of a per-element fold ----------------

    static final String MODEL_E = TmsDocumenten.model(List.of(
            objecttype("Jutter", List.of(TmsDocumenten.attribuut("last", GEHEEL)), List.of()),
            objecttype("Vondst", List.of(tijdattribuut("jaartarief", GEHEEL, "year"),
                    tijdattribuut("tarief", GEHEEL, "month"), TmsDocumenten.attribuut("keurscore", GEHEEL)),
                    List.of(tijdkenmerk("bewaard", "month"), kenmerk("gekeurd")))),
            List.of(feittypeEenNaarVeel("bewaring", "jutter", "Jutter", "vondst", "Vondst", "vondsten")));

    /** "Het tarief van een Vondst moet berekend worden als het tijdsevenredig deel per maand van zijn jaartarief
     *  gedurende de tijd dat hij bewaard is": its only writer, so the input stands where it computes nothing. */
    static final String TARIEF_E = rekenregel("tarief", "BEREKENING", "Vondst", "tarief",
            tijdsevenredig("jaartarief", "Vondst", check("bewaard", false), "month"), "null");
    static final String GEKEURD = kenmerkregel("keuring", "Vondst", "gekeurd",
            vergelijking("greater", lezing("keurscore", "Vondst"), getal("5")));
    static final String LAST = rekenregel("last", "BEREKENING", "Jutter", "last", aggregatie("total",
            lezing("tarief", "Vondst"), "Vondst", bronrol("bewaring", "vondst"), check("gekeurd", false), "null", GEHEEL,
            "het totaal van de tarieven van zijn vondsten die gekeurd zijn"), "null");

    static final List<String> REGELS_E = List.of(TARIEF_E, GEKEURD, LAST);

    static final String JAARTARIEF_12 = tijdlijn(List.of(segment("2024-01-01", "2025-01-01", "12")));
    static final Map<String, List<String>> BEWAARD_Q1 = Map.of("bewaard", List.of(periode("2024-01-01", "2024-04-01")));

    /** A Vondst with the keurscore {@code score} and the {@code waarden} beside it, bewaard on {@code perioden}. */
    static String vondst(String label, String score, Map<String, String> waarden, Map<String, List<String>> perioden) {
        Map<String, String> alle = new LinkedHashMap<>(waarden);
        alle.put("keurscore", getalwaarde(score));
        return instantie(label, "Vondst", alle, List.of(), perioden, Map.of());
    }

    static String jutter(String label, List<String> vondsten) {
        return instantie(label, "Jutter", Map.of(), List.of(), Map.of(), Map.of("vondsten", vondsten));
    }

    /** J2 keeps V3, the tarief 50 as a bucket scalar, not gekeurd: last 0. J3 keeps V4, the tarief 7 as a bucket scalar
     *  that the pro-rata part replaces with 12 a month January–March: last 36. */
    static List<String> spelendeE() {
        return List.of(jutter("J2", List.of("V3")), jutter("J3", List.of("V4")),
                vondst("V3", "2", Map.of("tarief", getalwaarde("50")), Map.of()),
                vondst("V4", "8", Map.of("tarief", getalwaarde("7"), "jaartarief", JAARTARIEF_12), BEWAARD_Q1));
    }

    @Test void een_lid_als_emmerscalair_faalt_en_de_vouw_noemt_het_eerste_in_vouwvolgorde() throws Exception {
        List<String> instanties = new ArrayList<>(List.of(jutter("J1", List.of("V1", "V2")),
                jutter("J4", List.of("V6", "V5")),
                vondst("V1", "8", Map.of("jaartarief", JAARTARIEF_12), BEWAARD_Q1),
                vondst("V2", "8", Map.of("tarief", getalwaarde("5")), Map.of()),
                vondst("V5", "9", Map.of("tarief", getalwaarde("4")), Map.of()),
                vondst("V6", "9", Map.of("tarief", getalwaarde("6")), Map.of())));
        instanties.addAll(spelendeE());
        inElkeVolgorde(MODEL_E, REGELS_E, List.of(), TmsTijdlijnDocumenten.zaak(DATUM, instanties,
                        List.of(consistent())),
                "Evaluatiefouten: aan het fixpunt staan 2 evaluatiefouten, in de regel \"last\" (2×): fout in de regel"
                        + " \"last\": the per-element temporal total of \"tarief\" over \"V2\": the case supplies an"
                        + " asOfDate-bucket scalar, which carries no period of its own; a set fold needs each member's own"
                        + " timeline (ch. 3.6/§7.1, ch. 8.4); fout in de regel \"last\": the per-element temporal total of"
                        + " \"tarief\" over \"V6\": the case supplies an asOfDate-bucket scalar, which carries no period of"
                        + " its own; a set fold needs each member's own timeline (ch. 3.6/§7.1, ch. 8.4)");
    }

    @Test void een_lid_dat_het_filter_niet_haalt_of_een_afgeleide_tijdlijn_heeft_faalt_niet() throws Exception {
        inElkeVolgorde(MODEL_E, REGELS_E, List.of(), TmsTijdlijnDocumenten.zaak(DATUM, spelendeE(), List.of(
                waarde("J2", "last", "0"), waarde("J3", "last", "36"),
                verwachteTijdlijn("V4", "tarief", maanden(2024, 1, 3, "12")))), "gespeeld");
    }

    // ---------------- F: an ever-unique key ----------------

    static final String MODEL_F = TmsOoitUniciteitWitnessTest.MODEL;
    static final List<String> REGELS_F = TmsOoitUniciteitWitnessTest.REGELS;
    static final String EENMALIG = TmsOoitUniciteitWitnessTest.EENMALIG;

    /** A Vondst with the code {@code code}, bewaard on {@code [van, tot)} where {@code van} is not null, and the nummer
     *  {@code nummer} as a bucket scalar where it is not null. */
    static String vondstF(String label, String code, String van, String tot, String nummer) {
        Map<String, String> waarden = new LinkedHashMap<>();
        waarden.put("code", getalwaarde(code));
        if (nummer != null) waarden.put("nummer", getalwaarde(nummer));
        return instantie(label, "Vondst", waarden, List.of(),
                van == null ? Map.of() : Map.of("bewaard", List.of(periode(van, tot))), Map.of());
    }

    /** V1 is geregistreerd and bewaard ten days in January 2024, its nummer 7 as a bucket scalar, which the day count
     *  replaces with 10 in 2024; V4 is bewaard ten days in March 2025, nummer 10 in 2025. */
    static List<String> spelendeF() {
        return List.of(vondstF("V1", "1", "2024-01-01", "2024-01-11", "7"),
                vondstF("V4", "1", "2025-03-01", "2025-03-11", null));
    }

    @Test void een_ooit_sleutel_als_emmerscalair_faalt() throws Exception {
        List<String> instanties = new ArrayList<>(spelendeF());
        // V2 is not geregistreerd, so its nummer 7 is not computed and stands; V3 is geregistreerd without a true
        // period, so the day count is empty and its nummer 5 stands
        instanties.add(vondstF("V2", "0", null, null, "7"));
        instanties.add(vondstF("V3", "1", null, null, "5"));
        inElkeVolgorde(MODEL_F, REGELS_F, List.of(EENMALIG), TmsTijdlijnDocumenten.zaak(DATUM, instanties,
                        List.of(consistent())),
                "Evaluatiefouten: aan het fixpunt staan 2 evaluatiefouten, in de regel \"eenmalig nummer\" (2×): fout in"
                        + " de regel \"eenmalig nummer\": the ever-unique key \"nummer\" needs the full timeline; \"V2\""
                        + " supplies only a scalar (ch. 5.9); fout in de regel \"eenmalig nummer\": the ever-unique key"
                        + " \"nummer\" needs the full timeline; \"V3\" supplies only a scalar (ch. 5.9)");
    }

    @Test void een_ooit_sleutel_die_de_dagentelling_afleidt_vervangt_de_emmerscalair() throws Exception {
        inElkeVolgorde(MODEL_F, REGELS_F, List.of(EENMALIG), TmsTijdlijnDocumenten.zaak(DATUM, spelendeF(), List.of(
                        verwachteTijdlijn("V1", "nummer", List.of(TmsOoitUniciteitWitnessTest.jaar(2024, "10"))),
                        verwachteTijdlijn("V4", "nummer", List.of(TmsOoitUniciteitWitnessTest.jaar(2025, "10"))),
                        inconsistent("eenmalig nummer", "V1"), inconsistent("eenmalig nummer", "V4"))),
                "gespeeld");
    }

    // ---------------- G: the key's form before the scope and after an empty component ----------------

    static final String MODEL_G = TmsDocumenten.model(List.of(
            objecttype("Strandvondst", List.of(TmsDocumenten.attribuut("code", GEHEEL),
                    tijdattribuut("vondstnummer", GEHEEL, "year"), tijdattribuut("kavelnummer", GEHEEL, "year")),
                    List.of()),
            objecttype("Kust", List.of(), List.of())),
            List.of(feittypeEenNaarVeel("ligging", "kust", "Kust", "strandvondst", "Strandvondst", "strandvondsten")));

    static final String PER_KUST = TmsUniciteitWitnessTest.uniciteit("kavel per kust",
            List.of(TmsUniciteitWitnessTest.bron("Strandvondst", List.of("kavelnummer"))),
            TmsDocumenten.rolbinding("ligging", "kust"),
            "De kavelnummers van alle Strandvondsten van de kust moeten uniek zijn.");
    static final String CODE_EN_NUMMER = TmsUniciteitWitnessTest.uniciteit("code en nummer",
            List.of(TmsUniciteitWitnessTest.bron("Strandvondst", List.of("code", "vondstnummer"))), null,
            "De codes en vondstnummers van alle Strandvondsten moeten uniek zijn.");

    @Test void de_vorm_van_de_sleutel_gaat_voor_de_scope_en_na_een_leeg_deel() throws Exception {
        JsonNode zaak = TmsTijdlijnDocumenten.zaak(DATUM, List.of(
                        // no kust: it takes no part in the scoped form, and its kavelnummer as a bucket scalar faults
                        instantie("S1", "Strandvondst", Map.of("kavelnummer", getalwaarde("3")), List.of(), Map.of(),
                                Map.of()),
                        // no code: the tuple stops at the empty component, and the vondstnummer is not read
                        instantie("S2", "Strandvondst", Map.of("vondstnummer", getalwaarde("7")), List.of(), Map.of(),
                                Map.of()),
                        instantie("S3", "Strandvondst", Map.of("code", getalwaarde("1"), "vondstnummer",
                                getalwaarde("8")), List.of(), Map.of(), Map.of())),
                List.of(consistent()));
        List<String> uitkomsten = new ArrayList<>();
        for (List<String> volgorde : TmsDocumenten.permutaties(List.of(PER_KUST, CODE_EN_NUMMER)))
            uitkomsten.add(TmsFoutWitnessTest.speel(TmsTijdlijnDocumenten.document(MODEL_G, List.of(), volgorde), zaak,
                    DroolsBackend.Branch.EXEC_MODEL));
        String verwacht = "Evaluatiefouten: aan het fixpunt staan 2 evaluatiefouten, in de regels \"code en nummer\","
                + " \"kavel per kust\": fout in de regel \"code en nummer\": the ever-unique key \"vondstnummer\" needs"
                + " the full timeline; \"S3\" supplies only a scalar (ch. 5.9); fout in de regel \"kavel per kust\": the"
                + " ever-unique key \"kavelnummer\" needs the full timeline; \"S1\" supplies only a scalar (ch. 5.9)";
        assertEquals(List.of(verwacht, verwacht), uitkomsten);
    }

    // ---------------- H: a characteristic on a timeline in the flat list ----------------

    static final String MODEL_H = TmsDocumenten.model(List.of(objecttype("Jutter", List.of(
            tijdattribuut("vergoeding", GEHEEL, "month"), tijdattribuut("jaartarief", GEHEEL, "year"),
            tijdattribuut("jaardeel", GEHEEL, "year"), tijdattribuut("stormdagen", GEHEEL, "year"),
            TmsDocumenten.attribuut("wachtsom", GEHEEL)),
            List.of(tijdkenmerk("wachtplichtig", "month")))), List.of());

    /** The clip of a total, the condition of a pro-rata part and the condition of a day count, each over wachtplichtig. */
    static final Map<String, String> REGELS_H = Map.of(
            "knip", WACHT,
            "pro-rata", rekenregel("jaardeel", "BEREKENING", "Jutter", "jaardeel", tijdsevenredig("jaartarief", "Jutter",
                    check("wachtplichtig", false), "year"), "null"),
            "dagentelling", rekenregel("stormdagen", "BEREKENING", "Jutter", "stormdagen",
                    dagentelling(check("wachtplichtig", false), "year"), "null"));

    static JsonNode zaakH() throws Exception {
        return TmsTijdlijnDocumenten.zaak(DATUM, List.of(instantie("J1", "Jutter", Map.of("vergoeding", VERGOEDING_2024,
                "jaartarief", JAARTARIEF_12), List.of("wachtplichtig"), Map.of(), Map.of())), List.of(consistent()));
    }

    @Test void een_tijdsafhankelijk_kenmerk_in_de_platte_lijst_weigert_de_caselezer() throws Exception {
        List<String> uitkomsten = new ArrayList<>(), verwacht = new ArrayList<>();
        for (Map.Entry<String, String> r : new java.util.TreeMap<>(REGELS_H).entrySet()) {
            uitkomsten.add(r.getKey() + " " + TmsFoutWitnessTest.speel(TmsTijdlijnDocumenten.document(MODEL_H,
                    List.of(r.getValue()), List.of()), zaakH(), DroolsBackend.Branch.EXEC_MODEL));
            verwacht.add(r.getKey() + " IllegalArgumentException: scalar characteristic input 'wachtplichtig' on the"
                    + " time-dependent kenmerk of Jutter: deliver it as a characteristicTimelines entry — the static"
                    + " characteristics list asserts a scalar truth no temporal reader consults");
        }
        assertEquals(verwacht, uitkomsten);
    }

    // ---------------- the documents, for the other executors ----------------

    /** Writes every document of this class, in every order of its rules, with its cases, under {@code args[0]}: a map
     *  per part and order, holding {@code ir.json} and the case files, for the verifier, ref and clingo. */
    public static void main(String[] args) throws Exception {
        Path uit = Path.of(args[0]);
        List<String> instantiesE = new ArrayList<>(List.of(jutter("J1", List.of("V1", "V2")),
                jutter("J4", List.of("V6", "V5")),
                vondst("V1", "8", Map.of("jaartarief", JAARTARIEF_12), BEWAARD_Q1),
                vondst("V2", "8", Map.of("tarief", getalwaarde("5")), Map.of()),
                vondst("V5", "9", Map.of("tarief", getalwaarde("4")), Map.of()),
                vondst("V6", "9", Map.of("tarief", getalwaarde("6")), Map.of())));
        instantiesE.addAll(spelendeE());
        List<String> instantiesF = new ArrayList<>(spelendeF());
        instantiesF.add(vondstF("V2", "0", null, null, "7"));
        instantiesF.add(vondstF("V3", "1", null, null, "5"));
        Map<String, Object[]> delen = new LinkedHashMap<>();
        delen.put("a-basis", new Object[]{TmsTijdlijnWitnessTest.MODEL_A, REGELS_A, List.of(), List.of(zaakA())});
        delen.put("b-tussenstand", new Object[]{TmsTijdlijnWitnessTest.MODEL_B, REGELS_B, List.of(), List.of(zaakB())});
        delen.put("c-eigen-totaal", new Object[]{MODEL_C, REGELS_C, List.of(), List.of(zaakC(true), zaakC(false))});
        delen.put("d-knip", new Object[]{MODEL_D, REGELS_D, List.of(), List.of(
                TmsTijdlijnDocumenten.zaak(DATUM, List.of(j1D(), j2D(), j3D()), List.of(consistent())),
                TmsTijdlijnDocumenten.zaak(DATUM, List.of(j3D()), List.of(
                        verwachteTijdlijn("J3", "controle", maanden(2024, 3, 3, "12")), waarde("J3", "topsom", "30"),
                        waarde("J3", "wachtsom", "0"))),
                TmsTijdlijnDocumenten.zaak(DATUM, List.of(j1D()), List.of(consistent())),
                TmsTijdlijnDocumenten.zaak(DATUM, List.of(j2D()), List.of(waarde("J2", "topsom", "0"),
                        waarde("J2", "wachtsom", "0"))))});
        delen.put("e-lid", new Object[]{MODEL_E, REGELS_E, List.of(), List.of(
                TmsTijdlijnDocumenten.zaak(DATUM, instantiesE, List.of(consistent())),
                TmsTijdlijnDocumenten.zaak(DATUM, spelendeE(), List.of(waarde("J2", "last", "0"),
                        waarde("J3", "last", "36"), verwachteTijdlijn("V4", "tarief", maanden(2024, 1, 3, "12")))),
                TmsTijdlijnDocumenten.zaak(DATUM, List.of(jutter("J4", List.of("V6", "V5")),
                        vondst("V5", "9", Map.of("tarief", getalwaarde("4")), Map.of()),
                        vondst("V6", "9", Map.of("tarief", getalwaarde("6")), Map.of())), List.of(consistent())))});
        delen.put("f-sleutel", new Object[]{MODEL_F, REGELS_F, List.of(EENMALIG), List.of(
                TmsTijdlijnDocumenten.zaak(DATUM, instantiesF, List.of(consistent())),
                TmsTijdlijnDocumenten.zaak(DATUM, spelendeF(), List.of(
                        verwachteTijdlijn("V1", "nummer", List.of(TmsOoitUniciteitWitnessTest.jaar(2024, "10"))),
                        verwachteTijdlijn("V4", "nummer", List.of(TmsOoitUniciteitWitnessTest.jaar(2025, "10"))),
                        inconsistent("eenmalig nummer", "V1"), inconsistent("eenmalig nummer", "V4"))))});
        for (Map.Entry<String, Object[]> d : delen.entrySet()) {
            @SuppressWarnings("unchecked") List<String> regels = (List<String>) d.getValue()[1];
            @SuppressWarnings("unchecked") List<String> uniciteiten = (List<String>) d.getValue()[2];
            @SuppressWarnings("unchecked") List<JsonNode> zaken = (List<JsonNode>) d.getValue()[3];
            int n = 0;
            for (List<String> volgorde : TmsDocumenten.permutaties(regels))
                schrijf(uit.resolve(d.getKey()).resolve(String.format("v%03d", n++)),
                        TmsTijdlijnDocumenten.ir((String) d.getValue()[0], volgorde, uniciteiten), zaken);
        }
        int n = 0;
        for (List<String> volgorde : TmsDocumenten.permutaties(List.of(PER_KUST, CODE_EN_NUMMER)))
            schrijf(uit.resolve("g-sleutelvorm").resolve(String.format("v%03d", n++)),
                    TmsTijdlijnDocumenten.ir(MODEL_G, List.of(), volgorde), List.of(TmsTijdlijnDocumenten.zaak(DATUM,
                            List.of(instantie("S1", "Strandvondst", Map.of("kavelnummer", getalwaarde("3")), List.of(),
                                            Map.of(), Map.of()),
                                    instantie("S2", "Strandvondst", Map.of("vondstnummer", getalwaarde("7")), List.of(),
                                            Map.of(), Map.of()),
                                    instantie("S3", "Strandvondst", Map.of("code", getalwaarde("1"), "vondstnummer",
                                            getalwaarde("8")), List.of(), Map.of(), Map.of())),
                            List.of(consistent()))));
        for (Map.Entry<String, String> r : new java.util.TreeMap<>(REGELS_H).entrySet())
            schrijf(uit.resolve("h-kenmerk").resolve(r.getKey()), TmsTijdlijnDocumenten.ir(MODEL_H, List.of(r.getValue()),
                    List.of()), List.of(zaakH()));
    }

    /** {@code ir.json} and the cases {@code case.json}, {@code case2.json}, … under {@code map}. */
    static void schrijf(Path map, String ir, List<JsonNode> zaken) throws Exception {
        TmsTijdlijnWitnessTest.schrijf(map, ir, zaken.get(0));
        for (int i = 1; i < zaken.size(); i++)
            java.nio.file.Files.writeString(map.resolve("case" + (i + 1) + ".json"), TmsDocumenten.JSON
                    .writerWithDefaultPrettyPrinter().writeValueAsString(zaken.get(i)));
    }
}
