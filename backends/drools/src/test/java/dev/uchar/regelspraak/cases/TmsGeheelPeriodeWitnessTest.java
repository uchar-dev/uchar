package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;

import dev.uchar.regelspraak.transpiler.tms.Waarde;

import org.drools.model.Rule;
import org.junit.jupiter.api.Test;
import org.kie.api.event.rule.DefaultRuleRuntimeEventListener;
import org.kie.api.event.rule.ObjectInsertedEvent;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static dev.uchar.regelspraak.cases.TmsDocumenten.check;
import static dev.uchar.regelspraak.cases.TmsDocumenten.consistent;
import static dev.uchar.regelspraak.cases.TmsDocumenten.getal;
import static dev.uchar.regelspraak.cases.TmsDocumenten.getalwaarde;
import static dev.uchar.regelspraak.cases.TmsDocumenten.kenmerkregel;
import static dev.uchar.regelspraak.cases.TmsDocumenten.lezing;
import static dev.uchar.regelspraak.cases.TmsDocumenten.rekenregel;
import static dev.uchar.regelspraak.cases.TmsDocumenten.verwacht;
import static dev.uchar.regelspraak.cases.TmsDocumenten.waarde;
import static dev.uchar.regelspraak.cases.TmsTijdlijnDocumenten.GEHEEL;
import static dev.uchar.regelspraak.cases.TmsTijdlijnDocumenten.dagentelling;
import static dev.uchar.regelspraak.cases.TmsTijdlijnDocumenten.instantie;
import static dev.uchar.regelspraak.cases.TmsTijdlijnDocumenten.kenmerk;
import static dev.uchar.regelspraak.cases.TmsTijdlijnDocumenten.maanden;
import static dev.uchar.regelspraak.cases.TmsTijdlijnDocumenten.objecttype;
import static dev.uchar.regelspraak.cases.TmsTijdlijnDocumenten.periode;
import static dev.uchar.regelspraak.cases.TmsTijdlijnDocumenten.segment;
import static dev.uchar.regelspraak.cases.TmsTijdlijnDocumenten.tijdattribuut;
import static dev.uchar.regelspraak.cases.TmsTijdlijnDocumenten.tijdkenmerk;
import static dev.uchar.regelspraak.cases.TmsTijdlijnDocumenten.tijdlijn;
import static dev.uchar.regelspraak.cases.TmsTijdlijnDocumenten.verwachteTijdlijn;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The whole-period qualifier on the truth-maintenance branch (RUNTIME-SEMANTICS ch. 3.9, 3.9a): "gedurende het gehele
 * jaar" and "gedurende de gehele maand" fold a timeline that the left-hand side binds, the subject's own or one a step
 * over a to-one role reaches, over the calendar years or months it touches. Every timeline the documents fold is a day
 * count ("het aantal dagen in de maand dat …", ch. 3.8) that another rule derives, so the fold reads what is derived
 * later, in every order of the rules; each case is replayed with an evaluation date and in a session without one, with
 * the same expectations, since the qualifier's grid is its own and no evaluation date is read.
 *
 * <p><b>A, the subject's own timeline.</b> "De wachtdagen van een Jutter moet berekend worden als het aantal dagen in de
 * maand dat hij wachtplichtig is"; "De trouwpremie … op 10 indien zijn wachtdagen gedurende het gehele jaar groter is
 * dan zijn drempel"; "De vulpremie … op 20 indien zijn wachtdagen gedurende de gehele maand gevuld is"; "De stilpremie
 * … op 30 indien zijn wachtdagen niet gedurende het gehele jaar groter is dan zijn drempel". By hand, with drempel 20
 * where it is set: J1, wachtplichtig through 2024, its input wachtdagen 5 in January 2024 replaced by the count, one
 * segment per month of at least 29 days: trouwpremie 10, vulpremie 20, stilpremie leeg. J2, wachtplichtig
 * [2024-01-15, 2024-07-01): wachtdagen 17, 29, 31, 30, 31 and 30 in January to June, which leave July to December of
 * the touched year 2024 without a value and January below the bound: trouwpremie leeg and stilpremie 30, while each of
 * the six touched months is covered whole: vulpremie 20; read off the bean, where it holds no timeline, the same rules
 * would answer 10, 20 and leeg. J6, wachtplichtig [2024-02-01, 2024-07-01): every month above 20, the year still not
 * covered: trouwpremie leeg, stilpremie 30, where the grid of the member, a month, would give 10. J3, never
 * wachtplichtig, with wachtdagen 25 in every month of 2024 as input, which the count does not replace (ch. 7.8): 10, 20,
 * leeg. J4, without periods and without input: the timeline has no segments, touches no bucket and passes, and its
 * negation fails: 10, 20, leeg. J5, as J1 without a drempel: a leeg bound fails every day, never a fault: trouwpremie
 * leeg, stilpremie 30, vulpremie 20. Every order of the four rules; beside them the generated rules in a hundred sampled
 * orders on a J2 of its own, where the trouwpremie of 10 that the timeline's input answers stands in some order on the
 * way and is gone at the fixpoint.
 *
 * <p><b>B, a step over a to-one role.</b> "De havendagen van een Werf moet berekend worden als het aantal dagen in de
 * maand dat hij open is"; "De werftoeslag van een Schuit … op 10 indien de havendagen van de werf van de Schuit
 * gedurende het gehele jaar gevuld is"; "Het kadegeld … op 5 indien de werf van de Schuit gedurende het gehele jaar
 * bedrijvig is"; "Het stilgeld … op 3 indien de havendagen van de werf van de Schuit niet gedurende het gehele jaar
 * gevuld is". S1 has no werf: the step reaches nothing, the null timeline passes both tests and fails the negation:
 * 10, 5, leeg. S2's werf W1 is open and bedrijvig through 2024: 10, 5, leeg. S3's werf W2 only in the first half of
 * 2024: leeg, leeg, 3, where the havendagen read off W2's bean, which holds none, would pass. S4's werf W3 is never open
 * and holds havendagen 3 in every month of 2024 as input, which stands: 10, and it has no bedrijvig periods, which pass:
 * 5; stilgeld leeg. Every order of the four rules.
 *
 * <p><b>C, the counting quantifiers with the qualifier's negation</b>, the form of {@code jutterswet-sleepvloot}: "De
 * vaardagen van een Boot moet berekend worden als het aantal dagen in de maand dat hij uitgevaren is", and "Een Vloot is
 * inzetbaar / krap / paraat indien de vaardagen van ten minste 2 / ten hoogste 1 / precies 1 van de boten van de Vloot
 * niet gedurende het gehele jaar groter is dan 20". The negation is read per member, so the quantifier counts the boats
 * for which the fold fails (ch. 3.9a). V1: B1 uitgevaren through 2024 (passes), B2 in the first half (fails), B3 never,
 * with vaardagen 15 in every month as input (fails): two denials, inzetbaar, not krap, not paraat; counting the passes,
 * one, would give the opposite of all three. V2: B4 through 2024 (passes) and B5 without a timeline (passes vacuously):
 * no denial, krap only. V3: B6 in the first half: one denial, krap and paraat. V4 without boats: krap only. Every order
 * of the four rules.
 *
 * <p><b>A3, the equalities against a leeg bound.</b> "De ongelijkpremie … op 10 indien zijn wachtdagen gedurende het
 * gehele jaar ongelijk is aan zijn drempel" and the gelijkpremie, 20, with "gelijk aan". J1 (drempel 20, wachtplichtig
 * through 2024): no day equals 20, ongelijkpremie 10, gelijkpremie leeg. J5 (no drempel): a leeg bound fails every
 * day, the inequality included (ch. 3.9: "A leeg comparison bound makes the test false"), both leeg. Every order of the
 * three rules.
 *
 * <p><b>E, the input given as the asOfDate-bucket scalar</b> (ch. 8.4): the fold reads the full timeline, so a bucket
 * scalar faults where the evaluation reaches the fold, and a timeline a rule derives replaces it. "Een Jutter is
 * vrijgesteld indien zijn drempel kleiner is dan 5", the wachtdagen of A, and "De trouwpremie … op 10 indien hij niet
 * vrijgesteld is en zijn wachtdagen gedurende het gehele jaar groter is dan zijn drempel", with the evaluation date
 * 2024-06-15. J1 (drempel 20, wachtdagen 25 as the bucket scalar, never wachtplichtig) faults: "the whole-period
 * qualifier over "wachtdagen" needs the full timeline; "J1" supplies only the asOfDate-bucket scalar (ch. 3.9, ch.
 * 8.4)". J2 (the same, wachtplichtig through 2024): the day count replaces the scalar, trouwpremie 10. J3 (drempel 3):
 * vrijgesteld, so the composite stops before the fold, trouwpremie leeg and no fault. Over the step of B, S1's werf with
 * havendagen 3 as the bucket scalar: "… over "havendagen" needs the full timeline; the werf of "S1" supplies only the
 * asOfDate-bucket scalar (ch. 3.9, ch. 8.4)". Every order of the rules.
 *
 * <p><b>R</b>: the forms the branch does not fold are refused by name: the negation inside the qualifier of a
 * characteristic check, a comparison over a timeline read over a path, and one between two timelines.
 *
 * <p><b>Measured</b> (2026-10-09, every document of {@link #main}, every order, each case; the verifier passes every
 * one). B and C: the reference interpreter's runner and the ASP runner pass every order of both cases (13 expectations
 * each). A: the ASP runner refuses a whole-period comparison against an attribute bound at translation ("a
 * whole-period comparison against a bound other than a literal or a scalar parameter is not translated … (no corpus
 * case)"), and with the bound written as the literal 20 (J5 then expecting what J1 does) passes all 24 orders of both
 * cases (21 expectations). The reference interpreter passes 8 of the 24 orders; in the other 16, where a premie's rule
 * precedes the day count in the document, it keeps a premie it wrote on the input state, "MISMATCH: J2.trouwpremie =
 * 10, expected leeg … J6.trouwpremie = 10" and, where the input is J1's January, "J1.stilpremie = 30, expected leeg":
 * the recorded residual of ch. 6.1, a condition-guarded write whose condition later turns false keeps its value; the
 * leeg/gevuld test, a negative edge there, differs in no order. A3: the reference interpreter answers "MISMATCH:
 * J5.ongelijkpremie = 10, expected leeg" in every order, its comparison of ch. 1.2 making "ongelijk aan" against leeg
 * true per day, where ch. 3.9 makes a leeg bound fail every day (no golden and no test of its own suite has the case),
 * and in three orders the same residual for J1's gelijkpremie; the ASP runner refuses the attribute bound. E: the
 * reference interpreter passes J1 and S1 without a fault, reading the bucket scalar as no timeline, which passes
 * (trouwpremie and werftoeslag 10), and passes J2 and J3 (4 expectations); the ASP runner answers S1's werftoeslag 10 and
 * refuses the attribute bound of J1, J2 and J3. This branch follows ch. 8.4 there, as for the pro-rata base of
 * {@code TmsInvoervormWitnessTest}. R: the reference interpreter and the ASP runner both carry the inner negation of
 * the check, and both refuse the other two forms ("whole-period qualifier on a comparison whose operands are not a
 * time-dependent attribute of "Schuit" and a scalar bound", "… between TWO time-dependent operands has no ch. 3.9
 * reading …").
 *
 * <p><b>Red</b> on 0e879163, the code before the change: every test, each on the refusal of the qualifier, "Excluded:
 * tms-transpiler SKIP: regel: trouwpremie — een Comparison met wholePeriod ('zijn wachtdagen gedurende het gehele jaar
 * groter is dan zijn drempel'): tijdlijnen zijn buiten de proef van de truth-maintenance-tak", and for B "… een
 * Predicate met wholePeriod …" and for R the same sentence in place of the named refusal.
 */
class TmsGeheelPeriodeWitnessTest {

    /** The whole-period qualifier: "het gehele jaar" or "de gehele maand", with or without its own negation. */
    static String heel(boolean jaar, boolean niet) {
        return "{\"node\":\"WholePeriod\",\"granularity\":\"" + (jaar ? "year" : "month") + "\",\"negated\":" + niet + "}";
    }

    /** A comparison in its wire spelling under the qualifier {@code periode} ({@link #heel}). */
    static String heleVergelijking(String op, String links, String rechts, String periode, String bron) {
        return "{\"node\":\"Comparison\",\"op\":\"" + op + "\",\"left\":" + links + ",\"right\":" + rechts
                + ",\"wholePeriod\":" + periode + ",\"source\":{\"snippet\":\"" + bron + "\"}}";
    }

    /** A leeg ({@code empty}) or gevuld ({@code filled}) test under the qualifier {@code periode}. */
    static String heelPredicaat(String kind, String operand, String periode, String bron) {
        return "{\"node\":\"Predicate\",\"kind\":\"" + kind + "\",\"operand\":" + operand + ",\"negated\":false,"
                + "\"digits\":0,\"wholePeriod\":" + periode + ",\"source\":{\"snippet\":\"" + bron + "\"}}";
    }

    /** A check of {@code kenmerk} of the object one role away, {@code rol} of {@code feittype}, under the qualifier. */
    static String heleCheck(String kenmerk, String feittype, String rol, String periode, String bron) {
        return "{\"node\":\"CharacteristicCheck\",\"characteristic\":\"" + kenmerk + "\",\"negated\":false,"
                + "\"wholePeriod\":" + periode + ",\"source\":{\"snippet\":\"" + bron + "\"},\"navigation\":{\"node\":"
                + "\"RoleBinding\",\"factType\":\"" + feittype + "\",\"role\":\"" + rol + "\"}}";
    }

    /** "{@code doel} … moet gesteld worden op {@code lexeem} indien {@code voorwaarde}". */
    static String premie(String naam, String type, String doel, String lexeem, String voorwaarde) {
        return rekenregel(naam, "GELIJKSTELLING", type, doel, getal(lexeem), voorwaarde);
    }

    /** The case of {@code instanties} with the evaluation date 2024-06-15 and in a session without one. */
    static List<JsonNode> zaken(List<String> instanties, List<String> verwachtingen) throws Exception {
        return List.of(TmsTijdlijnDocumenten.zaak("2024-06-15", instanties, verwachtingen),
                TmsTijdlijnDocumenten.zaak(null, instanties, verwachtingen));
    }

    /** Every order of {@code regels} in the document of {@code model}, each replayed on every case of {@code zaken}
     *  through the truth-maintenance branch: "[rule names] case i gespeeld" where it plays. */
    static List<String> inElkeVolgorde(String model, List<String> regels, List<JsonNode> zaken) throws Exception {
        List<String> uit = new ArrayList<>();
        for (List<String> volgorde : TmsDocumenten.permutaties(regels)) {
            List<String> namen = TmsDocumenten.regelnamen(volgorde);
            Path ir = TmsTijdlijnDocumenten.document(model, volgorde, List.of());
            for (int i = 0; i < zaken.size(); i++)
                uit.add(namen + " zaak " + i + " " + TmsFoutWitnessTest.speel(ir, zaken.get(i),
                        DroolsBackend.Branch.EXEC_MODEL));
        }
        return uit;
    }

    /** What {@link #inElkeVolgorde} gives where every order plays every case. */
    static List<String> gespeeld(List<String> regels, int zaken) throws Exception {
        List<String> uit = new ArrayList<>();
        for (List<String> volgorde : TmsDocumenten.permutaties(regels))
            for (int i = 0; i < zaken; i++) uit.add(TmsDocumenten.regelnamen(volgorde) + " zaak " + i + " gespeeld");
        return uit;
    }

    // ---------------- A: the subject's own timeline ----------------

    static final String MODEL_A = TmsDocumenten.model(List.of(objecttype("Jutter", List.of(
            TmsDocumenten.attribuut("drempel", GEHEEL), tijdattribuut("wachtdagen", GEHEEL, "month"),
            TmsDocumenten.attribuut("trouwpremie", GEHEEL), TmsDocumenten.attribuut("vulpremie", GEHEEL),
            TmsDocumenten.attribuut("stilpremie", GEHEEL)),
            List.of(tijdkenmerk("wachtplichtig", "month")))), List.of());

    static final String WACHTDAGEN = rekenregel("wachtdagen", "BEREKENING", "Jutter", "wachtdagen",
            dagentelling(check("wachtplichtig", false), "month"), "null");
    static final String TROUWPREMIE = premie("trouwpremie", "Jutter", "trouwpremie", "10",
            heleVergelijking("greater", lezing("wachtdagen", "Jutter"), lezing("drempel", "Jutter"), heel(true, false),
                    "zijn wachtdagen gedurende het gehele jaar groter is dan zijn drempel"));
    static final String VULPREMIE = premie("vulpremie", "Jutter", "vulpremie", "20",
            heelPredicaat("filled", lezing("wachtdagen", "Jutter"), heel(false, false),
                    "zijn wachtdagen gedurende de gehele maand gevuld is"));
    static final String STILPREMIE = premie("stilpremie", "Jutter", "stilpremie", "30",
            heleVergelijking("greater", lezing("wachtdagen", "Jutter"), lezing("drempel", "Jutter"), heel(true, true),
                    "zijn wachtdagen niet gedurende het gehele jaar groter is dan zijn drempel"));

    static final List<String> REGELS_A = List.of(WACHTDAGEN, TROUWPREMIE, VULPREMIE, STILPREMIE);

    static final Map<String, List<String>> HEEL_2024 = Map.of("wachtplichtig",
            List.of(periode("2024-01-01", "2025-01-01")));

    static List<String> instantiesA() {
        String drempel = getalwaarde("20");
        return List.of(
                instantie("J1", "Jutter", Map.of("drempel", drempel, "wachtdagen",
                        tijdlijn(List.of(segment("2024-01-01", "2024-02-01", "5")))), List.of(), HEEL_2024, Map.of()),
                instantie("J2", "Jutter", Map.of("drempel", drempel), List.of(),
                        Map.of("wachtplichtig", List.of(periode("2024-01-15", "2024-07-01"))), Map.of()),
                instantie("J3", "Jutter", Map.of("drempel", drempel, "wachtdagen", tijdlijn(maanden(2024, 1, 12, "25"))),
                        List.of(), Map.of(), Map.of()),
                instantie("J4", "Jutter", Map.of("drempel", drempel), List.of(), Map.of(), Map.of()),
                instantie("J5", "Jutter", Map.of(), List.of(), HEEL_2024, Map.of()),
                instantie("J6", "Jutter", Map.of("drempel", drempel), List.of(),
                        Map.of("wachtplichtig", List.of(periode("2024-02-01", "2024-07-01"))), Map.of()));
    }

    /** The day counts of 2024, January to December, as segments. */
    static List<String> dagen2024() {
        String[] n = {"31", "29", "31", "30", "31", "30", "31", "31", "30", "31", "30", "31"};
        List<String> uit = new ArrayList<>();
        for (int m = 1; m <= 12; m++)
            uit.add(segment(String.format("2024-%02d-01", m), m == 12 ? "2025-01-01" : String.format("2024-%02d-01", m + 1),
                    n[m - 1]));
        return uit;
    }

    static List<String> verwachtingenA() {
        List<String> uit = new ArrayList<>();
        uit.add(verwachteTijdlijn("J1", "wachtdagen", dagen2024()));
        uit.add(verwachteTijdlijn("J2", "wachtdagen", List.of(segment("2024-01-01", "2024-02-01", "17"),
                segment("2024-02-01", "2024-03-01", "29"), segment("2024-03-01", "2024-04-01", "31"),
                segment("2024-04-01", "2024-05-01", "30"), segment("2024-05-01", "2024-06-01", "31"),
                segment("2024-06-01", "2024-07-01", "30"))));
        Map<String, String[]> premies = new LinkedHashMap<>();
        premies.put("J1", new String[]{"10", "20", null});
        premies.put("J2", new String[]{null, "20", "30"});
        premies.put("J3", new String[]{"10", "20", null});
        premies.put("J4", new String[]{"10", "20", null});
        premies.put("J5", new String[]{null, "20", "30"});
        premies.put("J6", new String[]{null, "20", "30"});
        premies.forEach((j, p) -> {
            uit.add(waarde(j, "trouwpremie", p[0]));
            uit.add(waarde(j, "vulpremie", p[1]));
            uit.add(waarde(j, "stilpremie", p[2]));
        });
        uit.add(consistent());
        return uit;
    }

    @Test void de_eigen_tijdlijn_later_afgeleid_in_elke_volgorde_met_en_zonder_rekendatum() throws Exception {
        List<JsonNode> zaken = zaken(instantiesA(), verwachtingenA());
        assertEquals(gespeeld(REGELS_A, zaken.size()), inElkeVolgorde(MODEL_A, REGELS_A, zaken));
    }

    @Test void een_premie_van_een_tussenstand_is_aan_het_fixpunt_weg() throws Exception {
        Path ir = TmsTijdlijnDocumenten.document(MODEL_A, REGELS_A, List.of());
        DroolsBackend.Build b = DroolsBackend.build(ir, DroolsBackend.Branch.EXEC_MODEL);
        int onderweg = 0;
        for (List<Rule> volgorde : TmsDocumenten.steekproef(b.execModelTranspiler().model().getRules(), 20261009L, 100)) {
            List<String> namen = volgorde.stream().map(Rule::getName).toList();
            Object j2 = TmsDocumenten.boon(b.beanLoader(), "Jutter", Map.of("drempel", "20"), List.of());
            TmsTijdlijnDocumenten.zet(j2, "wachtplichtig", List.of(
                    TmsTijdlijnDocumenten.boonsegment(b.beanLoader(), "2024-01-15", "2024-07-01", null)));
            int[] tien = {0};
            TmsDocumenten.vuur(b, volgorde, List.of(j2), sessie -> sessie.addEventListener(
                    new DefaultRuleRuntimeEventListener() {
                        @Override public void objectInserted(ObjectInsertedEvent e) {
                            if (e.getObject() instanceof Waarde w && w.onderwerp() == j2
                                    && w.attribuut().naam().equals("trouwpremie")) tien[0]++;
                        }
                    }), sessie -> { });
            assertNull(TmsDocumenten.lees(j2, "trouwpremie"), () -> "trouwpremie van J2 in volgorde " + namen);
            assertEquals(0, new BigDecimal("30").compareTo((BigDecimal) TmsDocumenten.lees(j2, "stilpremie")),
                    () -> "stilpremie van J2 in volgorde " + namen);
            if (tien[0] > 0) onderweg++;
        }
        assertTrue(onderweg > 0, "geen volgorde gaf J2 onderweg de trouwpremie van zijn invoer");
    }

    // ---------------- A3: the equalities against a leeg bound ----------------

    static final String MODEL_A3 = TmsDocumenten.model(List.of(objecttype("Jutter", List.of(
            TmsDocumenten.attribuut("drempel", GEHEEL), tijdattribuut("wachtdagen", GEHEEL, "month"),
            TmsDocumenten.attribuut("ongelijkpremie", GEHEEL), TmsDocumenten.attribuut("gelijkpremie", GEHEEL)),
            List.of(tijdkenmerk("wachtplichtig", "month")))), List.of());

    static final String ONGELIJKPREMIE = premie("ongelijkpremie", "Jutter", "ongelijkpremie", "10",
            heleVergelijking("notEqual", lezing("wachtdagen", "Jutter"), lezing("drempel", "Jutter"), heel(true, false),
                    "zijn wachtdagen gedurende het gehele jaar ongelijk is aan zijn drempel"));
    static final String GELIJKPREMIE = premie("gelijkpremie", "Jutter", "gelijkpremie", "20",
            heleVergelijking("equal", lezing("wachtdagen", "Jutter"), lezing("drempel", "Jutter"), heel(true, false),
                    "zijn wachtdagen gedurende het gehele jaar gelijk is aan zijn drempel"));

    static final List<String> REGELS_A3 = List.of(WACHTDAGEN, ONGELIJKPREMIE, GELIJKPREMIE);

    static List<String> instantiesA3() {
        return List.of(instantie("J1", "Jutter", Map.of("drempel", getalwaarde("20")), List.of(), HEEL_2024, Map.of()),
                instantie("J5", "Jutter", Map.of(), List.of(), HEEL_2024, Map.of()));
    }

    static List<String> verwachtingenA3() {
        return List.of(waarde("J1", "ongelijkpremie", "10"), waarde("J1", "gelijkpremie", null),
                waarde("J5", "ongelijkpremie", null), waarde("J5", "gelijkpremie", null), consistent());
    }

    @Test void een_lege_grens_faalt_elke_dag_ook_voor_ongelijk_aan() throws Exception {
        List<JsonNode> zaken = zaken(instantiesA3(), verwachtingenA3());
        assertEquals(gespeeld(REGELS_A3, zaken.size()), inElkeVolgorde(MODEL_A3, REGELS_A3, zaken));
    }

    // ---------------- B: a step over a to-one role ----------------

    static final String MODEL_B = TmsDocumenten.model(List.of(
            objecttype("Werf", List.of(tijdattribuut("havendagen", GEHEEL, "month")),
                    List.of(tijdkenmerk("open", "month"), tijdkenmerk("bedrijvig", "month"))),
            objecttype("Schuit", List.of(TmsDocumenten.attribuut("werftoeslag", GEHEEL),
                    TmsDocumenten.attribuut("kadegeld", GEHEEL), TmsDocumenten.attribuut("stilgeld", GEHEEL)), List.of())),
            List.of(TmsDocumenten.feittypeEenNaarVeel("ligplaats", "werf", "Werf", "schuit", "Schuit", "schuiten")));

    static final String HAVENDAGEN = rekenregel("havendagen", "BEREKENING", "Werf", "havendagen",
            dagentelling(check("open", false), "month"), "null");
    static final String HAVENDAGEN_VAN_DE_WERF = TmsDocumenten.padlezing("havendagen", "Werf", "ligplaats", "werf");
    static final String WERFTOESLAG = premie("werftoeslag", "Schuit", "werftoeslag", "10",
            heelPredicaat("filled", HAVENDAGEN_VAN_DE_WERF, heel(true, false),
                    "de havendagen van de werf van de Schuit gedurende het gehele jaar gevuld is"));
    static final String KADEGELD = premie("kadegeld", "Schuit", "kadegeld", "5",
            heleCheck("bedrijvig", "ligplaats", "werf", heel(true, false),
                    "de werf van de Schuit gedurende het gehele jaar bedrijvig is"));
    static final String STILGELD = premie("stilgeld", "Schuit", "stilgeld", "3",
            heelPredicaat("filled", HAVENDAGEN_VAN_DE_WERF, heel(true, true),
                    "de havendagen van de werf van de Schuit niet gedurende het gehele jaar gevuld is"));

    static final List<String> REGELS_B = List.of(HAVENDAGEN, WERFTOESLAG, KADEGELD, STILGELD);

    static List<String> instantiesB() {
        Map<String, List<String>> heel = Map.of("open", List.of(periode("2024-01-01", "2025-01-01")),
                "bedrijvig", List.of(periode("2024-01-01", "2025-01-01")));
        Map<String, List<String>> half = Map.of("open", List.of(periode("2024-01-01", "2024-07-01")),
                "bedrijvig", List.of(periode("2024-01-01", "2024-07-01")));
        return List.of(
                instantie("S1", "Schuit", Map.of(), List.of(), Map.of(), Map.of()),
                instantie("S2", "Schuit", Map.of(), List.of(), Map.of(), Map.of("werf", List.of("W1"))),
                instantie("S3", "Schuit", Map.of(), List.of(), Map.of(), Map.of("werf", List.of("W2"))),
                instantie("S4", "Schuit", Map.of(), List.of(), Map.of(), Map.of("werf", List.of("W3"))),
                instantie("W1", "Werf", Map.of(), List.of(), heel, Map.of()),
                instantie("W2", "Werf", Map.of(), List.of(), half, Map.of()),
                instantie("W3", "Werf", Map.of("havendagen", tijdlijn(maanden(2024, 1, 12, "3"))), List.of(), Map.of(),
                        Map.of()));
    }

    static List<String> verwachtingenB() {
        List<String> uit = new ArrayList<>();
        Map<String, String[]> gelden = new LinkedHashMap<>();
        gelden.put("S1", new String[]{"10", "5", null});
        gelden.put("S2", new String[]{"10", "5", null});
        gelden.put("S3", new String[]{null, null, "3"});
        gelden.put("S4", new String[]{"10", "5", null});
        gelden.forEach((s, g) -> {
            uit.add(waarde(s, "werftoeslag", g[0]));
            uit.add(waarde(s, "kadegeld", g[1]));
            uit.add(waarde(s, "stilgeld", g[2]));
        });
        uit.add(consistent());
        return uit;
    }

    @Test void over_een_stap_zonder_koppeling_en_met_de_tijdlijn_van_de_stap_later_afgeleid() throws Exception {
        List<JsonNode> zaken = zaken(instantiesB(), verwachtingenB());
        assertEquals(gespeeld(REGELS_B, zaken.size()), inElkeVolgorde(MODEL_B, REGELS_B, zaken));
    }

    // ---------------- C: the counting quantifiers with the qualifier's negation ----------------

    static final String MODEL_C = TmsDocumenten.model(List.of(
            objecttype("Vloot", List.of(), List.of(kenmerk("inzetbaar"), kenmerk("krap"), kenmerk("paraat"))),
            objecttype("Boot", List.of(tijdattribuut("vaardagen", GEHEEL, "month")),
                    List.of(tijdkenmerk("uitgevaren", "month")))),
            List.of(TmsDocumenten.feittypeEenNaarVeel("indeling", "vloot", "Vloot", "boot", "Boot", "boten")));

    static final String VAARDAGEN = rekenregel("vaardagen", "BEREKENING", "Boot", "vaardagen",
            dagentelling(check("uitgevaren", false), "month"), "null");

    /** "Een Vloot is {@code doel} indien de vaardagen van {@code wat} van de boten van de Vloot niet gedurende het gehele
     *  jaar groter is dan 20". */
    static String telling(String naam, String doel, String kwantor, int drempel, String wat) {
        String bron = "de vaardagen van " + wat + " van de boten van de Vloot niet gedurende het gehele jaar groter is"
                + " dan 20";
        String toets = heleVergelijking("greater", lezing("vaardagen", "Boot"), getal("20"), heel(true, true), bron);
        return kenmerkregel(naam, "Vloot", doel, "{\"node\":\"QuantifierFold\",\"quantifier\":\"" + kwantor + "\","
                + "\"threshold\":" + drempel + ",\"elementType\":\"Boot\",\"test\":" + toets + ","
                + TmsDocumenten.bronrol("indeling", "boot") + ",\"source\":{\"snippet\":\"" + bron + "\"}}");
    }

    static final List<String> REGELS_C = List.of(VAARDAGEN, telling("inzet", "inzetbaar", "atLeast", 2, "ten minste 2"),
            telling("krapte", "krap", "atMost", 1, "ten hoogste 1"), telling("paraatheid", "paraat", "exactly", 1,
                    "precies 1"));

    static List<String> instantiesC() {
        Map<String, List<String>> heel = Map.of("uitgevaren", List.of(periode("2024-01-01", "2025-01-01")));
        Map<String, List<String>> half = Map.of("uitgevaren", List.of(periode("2024-01-01", "2024-07-01")));
        return List.of(
                instantie("V1", "Vloot", Map.of(), List.of(), Map.of(), Map.of("boten", List.of("B1", "B2", "B3"))),
                instantie("V2", "Vloot", Map.of(), List.of(), Map.of(), Map.of("boten", List.of("B4", "B5"))),
                instantie("V3", "Vloot", Map.of(), List.of(), Map.of(), Map.of("boten", List.of("B6"))),
                instantie("V4", "Vloot", Map.of(), List.of(), Map.of(), Map.of()),
                instantie("B1", "Boot", Map.of(), List.of(), heel, Map.of()),
                instantie("B2", "Boot", Map.of(), List.of(), half, Map.of()),
                instantie("B3", "Boot", Map.of("vaardagen", tijdlijn(maanden(2024, 1, 12, "15"))), List.of(), Map.of(),
                        Map.of()),
                instantie("B4", "Boot", Map.of(), List.of(), heel, Map.of()),
                instantie("B5", "Boot", Map.of(), List.of(), Map.of(), Map.of()),
                instantie("B6", "Boot", Map.of(), List.of(), half, Map.of()));
    }

    static List<String> verwachtingenC() {
        List<String> uit = new ArrayList<>();
        Map<String, boolean[]> vloten = new LinkedHashMap<>();
        vloten.put("V1", new boolean[]{true, false, false});
        vloten.put("V2", new boolean[]{false, true, false});
        vloten.put("V3", new boolean[]{false, true, true});
        vloten.put("V4", new boolean[]{false, true, false});
        vloten.forEach((v, k) -> {
            uit.add(verwacht(v, "inzetbaar", k[0]));
            uit.add(verwacht(v, "krap", k[1]));
            uit.add(verwacht(v, "paraat", k[2]));
        });
        uit.add(consistent());
        return uit;
    }

    @Test void de_tellende_kwantoren_tellen_de_ontkenning_per_lid_in_elke_volgorde() throws Exception {
        List<JsonNode> zaken = zaken(instantiesC(), verwachtingenC());
        assertEquals(gespeeld(REGELS_C, zaken.size()), inElkeVolgorde(MODEL_C, REGELS_C, zaken));
    }

    // ---------------- E: the input given as the asOfDate-bucket scalar ----------------

    static final String MODEL_E = TmsDocumenten.model(List.of(objecttype("Jutter", List.of(
            TmsDocumenten.attribuut("drempel", GEHEEL), tijdattribuut("wachtdagen", GEHEEL, "month"),
            TmsDocumenten.attribuut("trouwpremie", GEHEEL)),
            List.of(tijdkenmerk("wachtplichtig", "month"), kenmerk("vrijgesteld")))), List.of());

    static final String VRIJSTELLING = kenmerkregel("vrijstelling", "Jutter", "vrijgesteld",
            TmsDocumenten.vergelijking("less", lezing("drempel", "Jutter"), getal("5")));
    static final String TROUW_E = premie("trouwpremie", "Jutter", "trouwpremie", "10", TmsDocumenten.samengesteld("all",
            0, List.of(check("vrijgesteld", true), heleVergelijking("greater", lezing("wachtdagen", "Jutter"),
                    lezing("drempel", "Jutter"), heel(true, false),
                    "zijn wachtdagen gedurende het gehele jaar groter is dan zijn drempel"))));

    static final List<String> REGELS_E = List.of(WACHTDAGEN, VRIJSTELLING, TROUW_E);

    static final String EMMERFOUT = "Invoervormfout: fout in de regel \"trouwpremie\": the whole-period qualifier over"
            + " \"wachtdagen\" needs the full timeline; \"J1\" supplies only the asOfDate-bucket scalar (ch. 3.9, ch. 8.4)";

    static JsonNode emmerFout() throws Exception {
        return TmsTijdlijnDocumenten.zaak("2024-06-15", List.of(instantie("J1", "Jutter", Map.of("drempel",
                getalwaarde("20"), "wachtdagen", getalwaarde("25")), List.of(), Map.of(), Map.of())), List.of(consistent()));
    }

    static JsonNode emmerSpeelt() throws Exception {
        return TmsTijdlijnDocumenten.zaak("2024-06-15", List.of(
                        instantie("J2", "Jutter", Map.of("drempel", getalwaarde("20"), "wachtdagen", getalwaarde("25")),
                                List.of(), HEEL_2024, Map.of()),
                        instantie("J3", "Jutter", Map.of("drempel", getalwaarde("3"), "wachtdagen", getalwaarde("25")),
                                List.of(), Map.of(), Map.of())),
                List.of(verwachteTijdlijn("J2", "wachtdagen", dagen2024()), waarde("J2", "trouwpremie", "10"),
                        waarde("J3", "trouwpremie", null), consistent()));
    }

    static final String MODEL_E_PAD = MODEL_B;
    static final List<String> REGELS_E_PAD = List.of(HAVENDAGEN, WERFTOESLAG);

    static final String EMMERFOUT_PAD = "Invoervormfout: fout in de regel \"werftoeslag\": the whole-period qualifier"
            + " over \"havendagen\" needs the full timeline; the werf of \"S1\" supplies only the asOfDate-bucket scalar"
            + " (ch. 3.9, ch. 8.4)";

    static JsonNode emmerOverPad() throws Exception {
        return TmsTijdlijnDocumenten.zaak("2024-06-15", List.of(
                instantie("S1", "Schuit", Map.of(), List.of(), Map.of(), Map.of("werf", List.of("W1"))),
                instantie("W1", "Werf", Map.of("havendagen", getalwaarde("3")), List.of(), Map.of(), Map.of())),
                List.of(consistent()));
    }

    @Test void over_een_emmerscalair_faalt_de_vouw_waar_de_evaluatie_haar_bereikt() throws Exception {
        List<String> uit = new ArrayList<>(), verwacht = new ArrayList<>();
        for (List<String> volgorde : TmsDocumenten.permutaties(REGELS_E)) {
            List<String> namen = TmsDocumenten.regelnamen(volgorde);
            Path ir = TmsTijdlijnDocumenten.document(MODEL_E, volgorde, List.of());
            uit.add(namen + " " + TmsFoutWitnessTest.speel(ir, emmerFout(), DroolsBackend.Branch.EXEC_MODEL));
            uit.add(namen + " " + TmsFoutWitnessTest.speel(ir, emmerSpeelt(), DroolsBackend.Branch.EXEC_MODEL));
            verwacht.add(namen + " " + EMMERFOUT);
            verwacht.add(namen + " gespeeld");
        }
        for (List<String> volgorde : TmsDocumenten.permutaties(REGELS_E_PAD)) {
            List<String> namen = TmsDocumenten.regelnamen(volgorde);
            uit.add(namen + " " + TmsFoutWitnessTest.speel(TmsTijdlijnDocumenten.document(MODEL_E_PAD, volgorde,
                    List.of()), emmerOverPad(), DroolsBackend.Branch.EXEC_MODEL));
            verwacht.add(namen + " " + EMMERFOUT_PAD);
        }
        assertEquals(verwacht, uit);
    }

    // ---------------- R: the forms the branch does not fold ----------------

    /** "Een Jutter is X indien {@code voorwaarde}", in a document of model A extended by a characteristic X and a second
     *  timeline, vaardagen, and of model B. */
    static final String MODEL_R = TmsDocumenten.model(List.of(objecttype("Jutter", List.of(
            TmsDocumenten.attribuut("drempel", GEHEEL), tijdattribuut("wachtdagen", GEHEEL, "month"),
            tijdattribuut("vaardagen", GEHEEL, "month")),
            List.of(tijdkenmerk("wachtplichtig", "month"), kenmerk("trouw")))), List.of());

    /** The rule "Een Jutter is trouw indien {@code voorwaarde}". */
    static String trouw(String voorwaarde) {
        return kenmerkregel("trouw", "Jutter", "trouw", voorwaarde);
    }

    @Test void de_vormen_die_de_tak_niet_vouwt_worden_geweigerd_met_naam() throws Exception {
        String binnen = "{\"node\":\"CharacteristicCheck\",\"characteristic\":\"wachtplichtig\",\"negated\":true,"
                + "\"wholePeriod\":" + heel(true, false) + ",\"source\":{\"snippet\":\"hij gedurende het gehele jaar"
                + " niet wachtplichtig is\"}}";
        String overPad = premie("werftoeslag", "Schuit", "werftoeslag", "10", heleVergelijking("greater",
                HAVENDAGEN_VAN_DE_WERF, getal("5"), heel(true, false),
                "de havendagen van de werf van de Schuit gedurende het gehele jaar groter is dan 5"));
        String tweeTijdlijnen = heleVergelijking("greater", lezing("wachtdagen", "Jutter"), lezing("vaardagen", "Jutter"),
                heel(true, false), "zijn wachtdagen gedurende het gehele jaar groter is dan zijn vaardagen");
        assertEquals(List.of(
                "transpiler SKIP: regel: trouw — een CharacteristicCheck met wholePeriod ('hij gedurende het gehele"
                        + " jaar niet wachtplichtig is') met de ontkenning binnen de kwalificatie: deze tak"
                        + " draagt deze vorm nog niet, zoals de andere twee takken (RUNTIME-SEMANTICS ch. 3.9, 3.12); geen"
                        + " golden met cases heeft haar",
                "transpiler SKIP: regel: werftoeslag — een Comparison met wholePeriod ('de havendagen van de werf van"
                        + " de Schuit gedurende het gehele jaar groter is dan 5') over een tijdlijn langs een pad ('de"
                        + " havendagen van zijn werf'): de referentie-interpreter vouwt alleen een eigen tijdsafhankelijk"
                        + " attribuut van het onderwerp (RUNTIME-SEMANTICS ch. 3.9, 3.12)",
                "transpiler SKIP: regel: trouw — een Comparison met wholePeriod ('zijn wachtdagen gedurende het gehele"
                        + " jaar groter is dan zijn vaardagen') tussen twee tijdlijnen: de vouw toetst één tijdlijn van het"
                        + " onderwerp tegen een grens (RUNTIME-SEMANTICS ch. 3.9)"),
                List.of(weigering(MODEL_R, trouw(binnen)), weigering(MODEL_B, overPad),
                        weigering(MODEL_R, trouw(tweeTijdlijnen))));
    }

    /** The refusal of the document of {@code model} with the one rule {@code regel}, as the build raises it. */
    static String weigering(String model, String regel) throws Exception {
        Path ir = TmsTijdlijnDocumenten.document(model, List.of(regel), List.of());
        return org.junit.jupiter.api.Assertions.assertThrows(CaseRunner.Excluded.class,
                () -> DroolsBackend.build(ir, DroolsBackend.Branch.EXEC_MODEL)).getMessage();
    }

    // ---------------- the documents, for the other executors ----------------

    /** Writes every document of this class, in every order of its rules, with its cases, under {@code args[0]}: a map
     *  per part and order, holding {@code ir.json}, {@code case1.json} (with an evaluation date) and, where the part has
     *  one, {@code case2.json} (without), for the verifier, ref and clingo. */
    public static void main(String[] args) throws Exception {
        Path uit = Path.of(args[0]);
        Map<String, Object[]> delen = new LinkedHashMap<>();
        delen.put("a-eigen", new Object[]{MODEL_A, REGELS_A, zaken(instantiesA(), verwachtingenA())});
        delen.put("a3-gelijkheid", new Object[]{MODEL_A3, REGELS_A3, zaken(instantiesA3(), verwachtingenA3())});
        delen.put("b-stap", new Object[]{MODEL_B, REGELS_B, zaken(instantiesB(), verwachtingenB())});
        delen.put("c-tellend", new Object[]{MODEL_C, REGELS_C, zaken(instantiesC(), verwachtingenC())});
        delen.put("e-emmer-fout", new Object[]{MODEL_E, REGELS_E, List.of(emmerFout())});
        delen.put("e-emmer-speelt", new Object[]{MODEL_E, REGELS_E, List.of(emmerSpeelt())});
        delen.put("e-emmer-pad", new Object[]{MODEL_E_PAD, REGELS_E_PAD, List.of(emmerOverPad())});
        // the refused forms, each with one case, to measure what the other executors do with them
        List<JsonNode> jutter = zaken(List.of(instantie("J1", "Jutter", Map.of("drempel", getalwaarde("20"),
                "vaardagen", tijdlijn(maanden(2024, 1, 12, "10"))), List.of(), HEEL_2024, Map.of())), List.of(consistent()));
        String binnen = "{\"node\":\"CharacteristicCheck\",\"characteristic\":\"wachtplichtig\",\"negated\":true,"
                + "\"wholePeriod\":" + heel(true, false) + ",\"source\":{\"snippet\":\"hij gedurende het gehele jaar"
                + " niet wachtplichtig is\"}}";
        delen.put("r1-binnen", new Object[]{MODEL_R, List.of(trouw(binnen)), jutter});
        delen.put("r2-over-pad", new Object[]{MODEL_B, List.of(premie("werftoeslag", "Schuit", "werftoeslag", "10",
                heleVergelijking("greater", HAVENDAGEN_VAN_DE_WERF, getal("5"), heel(true, false),
                        "de havendagen van de werf van de Schuit gedurende het gehele jaar groter is dan 5"))),
                zaken(instantiesB(), List.of(consistent()))});
        delen.put("r3-twee-tijdlijnen", new Object[]{MODEL_R, List.of(trouw(heleVergelijking("greater",
                lezing("wachtdagen", "Jutter"), lezing("vaardagen", "Jutter"), heel(true, false),
                "zijn wachtdagen gedurende het gehele jaar groter is dan zijn vaardagen"))), jutter});
        for (Map.Entry<String, Object[]> d : delen.entrySet()) {
            @SuppressWarnings("unchecked") List<String> regels = (List<String>) d.getValue()[1];
            @SuppressWarnings("unchecked") List<JsonNode> zaken = (List<JsonNode>) d.getValue()[2];
            int n = 0;
            for (List<String> volgorde : TmsDocumenten.permutaties(regels)) {
                Path map = uit.resolve(d.getKey()).resolve(String.format("v%03d", n++));
                TmsTijdlijnWitnessTest.schrijf(map, TmsTijdlijnDocumenten.ir((String) d.getValue()[0], volgorde,
                        List.of()), zaken.get(0));
                java.nio.file.Files.move(map.resolve("case.json"), map.resolve("case1.json"));
                if (zaken.size() > 1) java.nio.file.Files.writeString(map.resolve("case2.json"), TmsDocumenten.JSON
                        .writerWithDefaultPrettyPrinter().writeValueAsString(zaken.get(1)));
            }
        }
    }
}
