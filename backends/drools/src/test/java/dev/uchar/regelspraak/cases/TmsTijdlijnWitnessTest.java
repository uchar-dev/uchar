package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;

import dev.uchar.regelspraak.transpiler.tms.Stand;
import dev.uchar.regelspraak.transpiler.tms.Tijdvak;
import dev.uchar.regelspraak.transpiler.tms.Waarde;

import org.drools.model.Rule;
import org.junit.jupiter.api.Test;
import org.kie.api.event.rule.DefaultRuleRuntimeEventListener;
import org.kie.api.event.rule.ObjectInsertedEvent;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static dev.uchar.regelspraak.cases.TmsDocumenten.check;
import static dev.uchar.regelspraak.cases.TmsDocumenten.consistent;
import static dev.uchar.regelspraak.cases.TmsDocumenten.getal;
import static dev.uchar.regelspraak.cases.TmsDocumenten.getalwaarde;
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
 * Timelines as values on the truth-maintenance branch (RUNTIME-SEMANTICS ch. 3): a timeline in a fact is an immutable
 * {@code List<Tijdvak>}, bound in the left-hand side off its standing or off the bean, written by a pro-rata part or a
 * day count as a whole (ch. 3.7, 3.8, 3.11), read by the temporal total (ch. 3.6), and written back onto the bean as
 * its segments.
 *
 * <p><b>A, a chain.</b> "Een Jutter is actief indien zijn score groter is dan 5"; "Het jaardeel van een Jutter moet
 * berekend worden als het tijdsevenredig deel per jaar van zijn jaartarief gedurende de tijd dat hij borgplichtig is,
 * indien hij actief is"; "Het maanddeel … als het tijdsevenredig deel per maand van zijn jaardeel gedurende de tijd dat
 * hij borgplichtig is"; "De wachtsom … als het totaal van zijn maanddeel gedurende de tijd dat hij borgplichtig is";
 * "De topsom … als het totaal van zijn bedrag gedurende de tijd dat zijn maanddeel groter is dan 100". So the gate of
 * the first writer, the base of the second, the timeline the first total clips and the timeline whose comparison clips
 * the second total are each derived by another rule. J1 (score 10, jaartarief 366 in 2024 and 365 in 2025, borgplichtig
 * on [2024-03-01, 2025-03-01), bedrag 7 on [2024-01-01, 2025-07-01)), by hand: jaardeel 2024 = 366 × 306/366 = 306 and
 * 2025 = 365 × 59/365 = 59; maanddeel = 306 in each month March–December 2024 and 59 in January and February 2025, the
 * whole months true, each bucket read off the year's base at its start; wachtsom = 10 × 306 + 2 × 59 = 3178; topsom =
 * 7 in each of the ten months whose maanddeel exceeds 100 = 70. J2 (score 3, so not actief, maanddeel 9 in June 2024 as
 * input): jaardeel never written, so the pro-rata over it is empty and leaves the input on the standing target
 * maanddeel whole (ch. 7.8), wachtsom 9, topsom 0. J3 (as J1 without bedrag, maanddeel 9 in June 2024 as input): the
 * computed maanddeel replaces the input whole, wachtsom 3178, and topsom 0, the clip over an absent timeline (ch. 3.6).
 * Every order of the five rules in the document; beside it the generated rules in a hundred sampled orders, where at
 * the fixpoint every object holds one {@link Stand} per derived timeline it reads and at most one {@link Waarde} per
 * target, and in some order the wachtsom of J1 was 0 on the way.
 *
 * <p><b>B, a flank on an intermediate state.</b> "Een Jutter is vrijgesteld indien zijn score kleiner is dan 5" and the
 * jaardeel "indien hij niet vrijgesteld is": a negated read of what a rule derives later. J1 (score 3, jaardeel 4 in
 * 2024 as input) keeps its input, J2 (score 10) gets 306 and 59, in both orders of the two rules.
 *
 * <p><b>C, the write-back.</b> A target never written is null on the bean (J2's jaardeel in A). A version with a bounded
 * validity, "geldig vanaf 1 januari 2020", is outside the standing domain (ch. 7.8 holds only version-free writers), so
 * where it computes the empty timeline it writes it (ch. 1.2g): J1, with jaardeel 4 in 2024 as input and no true period,
 * ends with an empty list on the bean, not null; J2, borgplichtig in March 2024, with jaardeel 31.
 *
 * <p><b>D, a clip over a characteristic on a timeline</b>, the worked example of ch. 3.6 verbatim: vergoeding 10 per
 * month through 2023 and 11 January–June 2024, wachtplichtig on [2023-07-01, 2024-02-01): "het totaal van zijn
 * vergoeding gedurende de tijd dat hij wachtplichtig is" 6 × 10 + 1 × 11 = 71, the negated form 6 × 10 + 5 × 11 = 115,
 * "gedurende de tijd dat zijn vergoeding groter is dan 10" 6 × 11 = 66; without periods 0, 186 and 66; without a
 * vergoeding 0, 0 and 0. The true periods are bound as a timeline off the bean: read as a characteristic that holds one
 * truth value they would test false, and every clip would be empty. Every order of the three rules.
 *
 * <p><b>E, a target on a fixed start point</b>, the day count of ch. 3.2 verbatim: "het aantal dagen in het jaar dat hij
 * wachtplichtig is", wachtplichtig on [2024-09-28, 2024-10-03), into a target on the year grid {@code --10-01}: 3 on
 * [2023-10-01, 2024-10-01) and 2 on [2024-10-01, 2025-10-01); on the calendar grid 5 on [2024-01-01, 2025-01-01). Both
 * orders.
 *
 * <p><b>B2, a comparison with a value a rule replaces later.</b> "De teller van een Jutter moet berekend worden als zijn
 * score" and the jaardeel "indien zijn teller kleiner is dan 5": J1 (score 10, teller 3 as input, jaardeel 4 in 2024 as
 * input) holds the condition only on the intermediate state where the teller is still its input; at the fixpoint the
 * teller is 10, so the jaardeel keeps its input. J2 (score 2, teller 8) gets 306 and 59. Both orders.
 *
 * <p><b>Measured</b> (2026-10-09, every document of {@link #main}, every order: 131 documents): the verifier passes all;
 * the reference interpreter's runner passes all but one, and the ASP runner passes all but one. The reference
 * interpreter is order-dependent on B2: with the jaardeel before the teller it keeps the timeline it wrote on the
 * intermediate state, "MISMATCH: J1.jaardeel = [2024-01-01,2025-01-01)=306 [2025-01-01,2026-01-01)=59, expected
 * [2024-01-01,2025-01-01)=4", and with the teller first it passes; the ASP runner passes both orders. That is the
 * class of the card fout-op-tussenstand (PLAN §9, 2026-10-09: an intermediate state is not observable), here for a
 * timeline: this branch withdraws the write and answers the fixpoint in every order. On B, the negated read, the
 * reference interpreter passes both orders: it puts the reader of a negated characteristic in a stratum above its
 * writer (ch. 6.3), so it never sees the intermediate state. On C the ASP runner refuses, "TRANSLATION FAULT: the case
 * supplies input for computed target "jaardeel": ref (like v0) overwrites it, which has no monotone encoding here (no
 * corpus case)", and the reference interpreter passes: it writes the empty timeline over the input.
 *
 * <p><b>Red</b> on b9f77aa7, the code before the change: all seven tests, each on the refusal of the member, "Excluded:
 * tms-transpiler SKIP: regel: jaardeel — het tijdsafhankelijke attribuut 'jaardeel' van Jutter: tijdlijnen zijn buiten
 * de proef van de truth-maintenance-tak", and for D the refusal of "een Aggregation total over de eigen tijdlijn".
 */
class TmsTijdlijnWitnessTest {

    // ---------------- A: a chain of timeline writers and the totals over them ----------------

    static final String BORGPLICHTIG = check("borgplichtig", false);

    static final String MODEL_A = TmsDocumenten.model(List.of(objecttype("Jutter", List.of(
            TmsDocumenten.attribuut("score", GEHEEL), tijdattribuut("jaartarief", GEHEEL, "year"),
            tijdattribuut("jaardeel", GEHEEL, "year"), tijdattribuut("maanddeel", GEHEEL, "month"),
            tijdattribuut("bedrag", GEHEEL, "month"), TmsDocumenten.attribuut("wachtsom", GEHEEL),
            TmsDocumenten.attribuut("topsom", GEHEEL)),
            List.of(tijdkenmerk("borgplichtig", "month"), kenmerk("actief")))), List.of());

    static final String ACTIEF = TmsDocumenten.kenmerkregel("actief", "Jutter", "actief",
            vergelijking("greater", lezing("score", "Jutter"), getal("5")));
    static final String JAARDEEL = rekenregel("jaardeel", "BEREKENING", "Jutter", "jaardeel",
            tijdsevenredig("jaartarief", "Jutter", BORGPLICHTIG, "year"), check("actief", false));
    static final String MAANDDEEL = rekenregel("maanddeel", "BEREKENING", "Jutter", "maanddeel",
            tijdsevenredig("jaardeel", "Jutter", BORGPLICHTIG, "month"), "null");
    static final String WACHTSOM = rekenregel("wachtsom", "BEREKENING", "Jutter", "wachtsom",
            eigenTotaal("maanddeel", "Jutter", BORGPLICHTIG), "null");
    static final String TOPSOM = rekenregel("topsom", "BEREKENING", "Jutter", "topsom",
            eigenTotaal("bedrag", "Jutter", vergelijking("greater", lezing("maanddeel", "Jutter"), getal("100"))), "null");

    static final List<String> REGELS_A = List.of(ACTIEF, JAARDEEL, MAANDDEEL, WACHTSOM, TOPSOM);

    static final String TARIEF = tijdlijn(List.of(segment("2024-01-01", "2025-01-01", "366"),
            segment("2025-01-01", "2026-01-01", "365")));
    static final String BEDRAG = tijdlijn(List.of(segment("2024-01-01", "2025-07-01", "7")));
    static final String JUNI = tijdlijn(List.of(segment("2024-06-01", "2024-07-01", "9")));
    static final Map<String, List<String>> BORG = Map.of("borgplichtig", List.of(periode("2024-03-01", "2025-03-01")));
    static final List<String> JAARDELEN = List.of(segment("2024-01-01", "2025-01-01", "306"),
            segment("2025-01-01", "2026-01-01", "59"));

    static List<String> maanddelen() {
        List<String> uit = new ArrayList<>(maanden(2024, 3, 10, "306"));
        uit.addAll(maanden(2025, 1, 2, "59"));
        return uit;
    }

    static JsonNode zaakA() throws Exception {
        return TmsTijdlijnDocumenten.zaak(null, List.of(
                        instantie("J1", "Jutter", Map.of("score", getalwaarde("10"), "jaartarief", TARIEF, "bedrag", BEDRAG),
                                List.of(), BORG, Map.of()),
                        instantie("J2", "Jutter", Map.of("score", getalwaarde("3"), "jaartarief", TARIEF, "bedrag", BEDRAG,
                                "maanddeel", JUNI), List.of(), BORG, Map.of()),
                        instantie("J3", "Jutter", Map.of("score", getalwaarde("10"), "jaartarief", TARIEF, "maanddeel", JUNI),
                                List.of(), BORG, Map.of())),
                List.of(verwachteTijdlijn("J1", "jaardeel", JAARDELEN), verwachteTijdlijn("J1", "maanddeel", maanddelen()),
                        waarde("J1", "wachtsom", "3178"), waarde("J1", "topsom", "70"),
                        verwachteTijdlijn("J2", "jaardeel", List.of()),
                        verwachteTijdlijn("J2", "maanddeel", List.of(segment("2024-06-01", "2024-07-01", "9"))),
                        waarde("J2", "wachtsom", "9"), waarde("J2", "topsom", "0"),
                        verwachteTijdlijn("J3", "jaardeel", JAARDELEN), verwachteTijdlijn("J3", "maanddeel", maanddelen()),
                        waarde("J3", "wachtsom", "3178"), waarde("J3", "topsom", "0"), consistent()));
    }

    @Test void een_keten_van_tijdlijnschrijvers_en_de_totalen_erover_in_elke_volgorde_van_de_regels() throws Exception {
        JsonNode zaak = zaakA();
        List<String> uitkomsten = new ArrayList<>(), verwacht = new ArrayList<>();
        for (List<String> volgorde : TmsDocumenten.permutaties(REGELS_A)) {
            List<String> namen = TmsDocumenten.regelnamen(volgorde);
            Path ir = TmsTijdlijnDocumenten.document(MODEL_A, volgorde, List.of());
            uitkomsten.add(namen + " " + TmsFoutWitnessTest.speel(ir, zaak, DroolsBackend.Branch.TRUTH_MAINTENANCE));
            verwacht.add(namen + " gespeeld");
        }
        assertEquals(verwacht, uitkomsten);
    }

    @Test void aan_het_fixpunt_een_stand_en_hoogstens_een_waarde_per_object_en_lid() throws Exception {
        Path ir = TmsTijdlijnDocumenten.document(MODEL_A, REGELS_A, List.of());
        DroolsBackend.Build b = DroolsBackend.build(ir, DroolsBackend.Branch.TRUTH_MAINTENANCE);
        int tussenstand = 0;
        for (List<Rule> volgorde : TmsDocumenten.steekproef(b.tmsTranspiler().model().getRules(), 20261009L, 100)) {
            List<String> namen = volgorde.stream().map(Rule::getName).toList();
            Object j1 = jutter(b.beanLoader(), "10", true), j2 = jutter(b.beanLoader(), "3", false);
            Map<Object, String> labels = new IdentityHashMap<>(Map.of(j1, "J1", j2, "J2"));
            int[] onderweg = {0};
            Map<String, Integer> standen = new TreeMap<>(), waarden = new TreeMap<>();
            TmsDocumenten.vuur(b, volgorde, List.of(j1, j2), sessie -> sessie.addEventListener(
                    new DefaultRuleRuntimeEventListener() {
                        @Override public void objectInserted(ObjectInsertedEvent e) {
                            if (e.getObject() instanceof Waarde w && w.onderwerp() == j1
                                    && w.attribuut().naam().equals("wachtsom")
                                    && ((BigDecimal) w.waarde()).compareTo(new BigDecimal("3178")) != 0) onderweg[0]++;
                        }
                    }), sessie -> {
                for (Object f : sessie.getObjects(o -> o instanceof Stand))
                    standen.merge(labels.get(((Stand) f).onderwerp()) + "." + ((Stand) f).attribuut().naam(), 1,
                            Integer::sum);
                for (Object f : sessie.getObjects(o -> o instanceof Waarde))
                    waarden.merge(labels.get(((Waarde) f).onderwerp()) + "." + ((Waarde) f).attribuut().naam(), 1,
                            Integer::sum);
            });
            assertEquals(Map.of("J1.jaardeel", 1, "J1.maanddeel", 1, "J2.jaardeel", 1, "J2.maanddeel", 1), standen,
                    () -> "standen in volgorde " + namen);
            assertEquals(Map.of("J1.jaardeel", 1, "J1.maanddeel", 1, "J1.topsom", 1, "J1.wachtsom", 1, "J2.topsom", 1,
                    "J2.wachtsom", 1), waarden, () -> "waarden in volgorde " + namen);
            assertEquals(List.of("[2024-01-01, 2025-01-01)=306", "[2025-01-01, 2026-01-01)=59"),
                    TmsTijdlijnWitnessTest.tijdlijnVan(j1, "jaardeel"), () -> "jaardeel van J1 in volgorde " + namen);
            assertEquals(12, TmsTijdlijnWitnessTest.tijdlijnVan(j1, "maanddeel").size(), () -> "volgorde " + namen);
            assertEquals(0, new BigDecimal("3178").compareTo((BigDecimal) TmsDocumenten.lees(j1, "wachtsom")),
                    () -> "wachtsom van J1 in volgorde " + namen);
            assertEquals(0, new BigDecimal("70").compareTo((BigDecimal) TmsDocumenten.lees(j1, "topsom")),
                    () -> "topsom van J1 in volgorde " + namen);
            assertNull(TmsDocumenten.lees(j2, "jaardeel"), () -> "een nooit geschreven doel is null, volgorde " + namen);
            if (onderweg[0] > 0) tussenstand++;
        }
        assertTrue(tussenstand > 0, "geen volgorde gaf J1 onderweg een andere wachtsom dan 3178");
    }

    /** A Jutter bean as case A's J1 (score 10, with bedrag) or J2 (score 3, without), with the timelines on it. */
    static Object jutter(ClassLoader beanLoader, String score, boolean bedrag) throws Exception {
        Object j = TmsDocumenten.boon(beanLoader, "Jutter", Map.of("score", score), List.of());
        TmsTijdlijnDocumenten.zet(j, "jaartarief", List.of(
                TmsTijdlijnDocumenten.boonsegment(beanLoader, "2024-01-01", "2025-01-01", "366"),
                TmsTijdlijnDocumenten.boonsegment(beanLoader, "2025-01-01", "2026-01-01", "365")));
        TmsTijdlijnDocumenten.zet(j, "borgplichtig", List.of(
                TmsTijdlijnDocumenten.boonsegment(beanLoader, "2024-03-01", "2025-03-01", null)));
        if (bedrag) TmsTijdlijnDocumenten.zet(j, "bedrag", List.of(
                TmsTijdlijnDocumenten.boonsegment(beanLoader, "2024-01-01", "2025-07-01", "7")));
        return j;
    }

    /** The timeline on member {@code lid} of {@code boon} as "[van, tot)=waarde", or null where the bean holds none. */
    static List<String> tijdlijnVan(Object boon, String lid) throws Exception {
        Object l = TmsDocumenten.lees(boon, lid);
        if (l == null) return null;
        List<String> uit = new ArrayList<>();
        for (Tijdvak t : Tijdvak.lijst((List<?>) l))
            uit.add("[" + t.van() + ", " + t.tot() + ")=" + (t.waarde() == null ? "leeg"
                    : t.waarde().stripTrailingZeros().toPlainString()));
        return uit;
    }

    // ---------------- B: a negated read of what a rule derives later ----------------

    static final String MODEL_B = TmsDocumenten.model(List.of(objecttype("Jutter", List.of(
            TmsDocumenten.attribuut("score", GEHEEL), tijdattribuut("jaartarief", GEHEEL, "year"),
            tijdattribuut("jaardeel", GEHEEL, "year")),
            List.of(tijdkenmerk("borgplichtig", "month"), kenmerk("vrijgesteld")))), List.of());

    static final String VRIJSTELLING = TmsDocumenten.kenmerkregel("vrijstelling", "Jutter", "vrijgesteld",
            vergelijking("less", lezing("score", "Jutter"), getal("5")));
    static final String JAARDEEL_NIET_VRIJ = rekenregel("jaardeel", "BEREKENING", "Jutter", "jaardeel",
            tijdsevenredig("jaartarief", "Jutter", BORGPLICHTIG, "year"), check("vrijgesteld", true));

    static JsonNode zaakB() throws Exception {
        return TmsTijdlijnDocumenten.zaak(null, List.of(
                        instantie("J1", "Jutter", Map.of("score", getalwaarde("3"), "jaartarief", TARIEF, "jaardeel",
                                tijdlijn(List.of(segment("2024-01-01", "2025-01-01", "4")))), List.of(), BORG, Map.of()),
                        instantie("J2", "Jutter", Map.of("score", getalwaarde("10"), "jaartarief", TARIEF), List.of(), BORG,
                                Map.of())),
                List.of(verwachteTijdlijn("J1", "jaardeel", List.of(segment("2024-01-01", "2025-01-01", "4"))),
                        verwachteTijdlijn("J2", "jaardeel", JAARDELEN), consistent()));
    }

    @Test void een_ontkende_lezing_van_wat_later_afgeleid_wordt_in_beide_volgordes() throws Exception {
        JsonNode zaak = zaakB();
        List<String> uitkomsten = new ArrayList<>(), verwacht = new ArrayList<>();
        for (List<String> volgorde : TmsDocumenten.permutaties(List.of(VRIJSTELLING, JAARDEEL_NIET_VRIJ))) {
            List<String> namen = TmsDocumenten.regelnamen(volgorde);
            Path ir = TmsTijdlijnDocumenten.document(MODEL_B, volgorde, List.of());
            uitkomsten.add(namen + " " + TmsFoutWitnessTest.speel(ir, zaak, DroolsBackend.Branch.TRUTH_MAINTENANCE));
            verwacht.add(namen + " gespeeld");
        }
        assertEquals(verwacht, uitkomsten);
    }

    // ---------------- B2: a comparison with a value a rule replaces later ----------------

    static final String MODEL_B2 = TmsDocumenten.model(List.of(objecttype("Jutter", List.of(
            TmsDocumenten.attribuut("score", GEHEEL), TmsDocumenten.attribuut("teller", GEHEEL),
            tijdattribuut("jaartarief", GEHEEL, "year"), tijdattribuut("jaardeel", GEHEEL, "year")),
            List.of(tijdkenmerk("borgplichtig", "month")))), List.of());

    static final String TELLER = rekenregel("teller", "BEREKENING", "Jutter", "teller", lezing("score", "Jutter"), "null");
    static final String JAARDEEL_ONDER_VIJF = rekenregel("jaardeel", "BEREKENING", "Jutter", "jaardeel",
            tijdsevenredig("jaartarief", "Jutter", BORGPLICHTIG, "year"),
            vergelijking("less", lezing("teller", "Jutter"), getal("5")));

    static JsonNode zaakB2() throws Exception {
        return TmsTijdlijnDocumenten.zaak(null, List.of(
                        instantie("J1", "Jutter", Map.of("score", getalwaarde("10"), "teller", getalwaarde("3"),
                                "jaartarief", TARIEF, "jaardeel", tijdlijn(List.of(segment("2024-01-01", "2025-01-01", "4")))),
                                List.of(), BORG, Map.of()),
                        instantie("J2", "Jutter", Map.of("score", getalwaarde("2"), "teller", getalwaarde("8"),
                                "jaartarief", TARIEF), List.of(), BORG, Map.of())),
                List.of(waarde("J1", "teller", "10"),
                        verwachteTijdlijn("J1", "jaardeel", List.of(segment("2024-01-01", "2025-01-01", "4"))),
                        waarde("J2", "teller", "2"), verwachteTijdlijn("J2", "jaardeel", JAARDELEN), consistent()));
    }

    @Test void een_vergelijking_met_een_waarde_die_een_regel_later_vervangt_in_beide_volgordes() throws Exception {
        JsonNode zaak = zaakB2();
        List<String> uitkomsten = new ArrayList<>(), verwacht = new ArrayList<>();
        for (List<String> volgorde : TmsDocumenten.permutaties(List.of(TELLER, JAARDEEL_ONDER_VIJF))) {
            List<String> namen = TmsDocumenten.regelnamen(volgorde);
            Path ir = TmsTijdlijnDocumenten.document(MODEL_B2, volgorde, List.of());
            uitkomsten.add(namen + " " + TmsFoutWitnessTest.speel(ir, zaak, DroolsBackend.Branch.TRUTH_MAINTENANCE));
            verwacht.add(namen + " gespeeld");
        }
        assertEquals(verwacht, uitkomsten);
    }

    // ---------------- C: an empty timeline written outside the standing domain ----------------

    static final String MODEL_C = TmsDocumenten.model(List.of(objecttype("Jutter", List.of(
            tijdattribuut("jaartarief", GEHEEL, "year"), tijdattribuut("jaardeel", GEHEEL, "year")),
            List.of(tijdkenmerk("borgplichtig", "month")))), List.of());

    static final String JAARDEEL_GEDATEERD = IrPad.regel("jaardeel vanaf 2020", "BEREKENING",
            "{\"node\":\"AttributeReference\",\"attribute\":\"jaardeel\",\"subjectType\":\"Jutter\",\"source\":{\"snippet\":"
                    + "\"Het jaardeel van een Jutter\"}}", tijdsevenredig("jaartarief", "Jutter", BORGPLICHTIG, "year"),
            "null", TmsDocumenten.geldigheid("2020-01-01", null));

    static JsonNode zaakC() throws Exception {
        String tarief = tijdlijn(List.of(segment("2024-01-01", "2025-01-01", "366")));
        String vier = tijdlijn(List.of(segment("2024-01-01", "2025-01-01", "4")));
        return TmsTijdlijnDocumenten.zaak("2025-06-01", List.of(
                        instantie("J1", "Jutter", Map.of("jaartarief", tarief, "jaardeel", vier), List.of(), Map.of(), Map.of()),
                        instantie("J2", "Jutter", Map.of("jaartarief", tarief, "jaardeel", vier), List.of(),
                                Map.of("borgplichtig", List.of(periode("2024-03-01", "2024-04-01"))), Map.of())),
                List.of(verwachteTijdlijn("J1", "jaardeel", List.of()),
                        verwachteTijdlijn("J2", "jaardeel", List.of(segment("2024-01-01", "2025-01-01", "31"))),
                        consistent()));
    }

    @Test void buiten_het_staande_domein_schrijft_een_lege_berekening_de_lege_tijdlijn() throws Exception {
        Path ir = TmsTijdlijnDocumenten.document(MODEL_C, List.of(JAARDEEL_GEDATEERD), List.of());
        assertEquals("gespeeld", TmsFoutWitnessTest.speel(ir, zaakC(), DroolsBackend.Branch.TRUTH_MAINTENANCE));
        DroolsBackend.Build b = DroolsBackend.build(ir, DroolsBackend.Branch.TRUTH_MAINTENANCE);
        Object j1 = TmsDocumenten.boon(b.beanLoader(), "Jutter", Map.of(), List.of());
        TmsTijdlijnDocumenten.zet(j1, "jaartarief", List.of(
                TmsTijdlijnDocumenten.boonsegment(b.beanLoader(), "2024-01-01", "2025-01-01", "366")));
        TmsTijdlijnDocumenten.zet(j1, "jaardeel", List.of(
                TmsTijdlijnDocumenten.boonsegment(b.beanLoader(), "2024-01-01", "2025-01-01", "4")));
        TmsDocumenten.vuur(b, b.tmsTranspiler().model().getRules(), List.of(j1),
                sessie -> sessie.setGlobal("rekendatum", java.time.LocalDate.parse("2025-06-01")), sessie -> { });
        assertEquals(List.of(), tijdlijnVan(j1, "jaardeel"), "een lege lijst op de boon, niet null en niet de invoer");
    }

    // ---------------- D: the clip over a characteristic on a timeline, ch. 3.6 ----------------

    static final String MODEL_D = TmsDocumenten.model(List.of(objecttype("Jutter", List.of(
            tijdattribuut("vergoeding", GEHEEL, "month"), TmsDocumenten.attribuut("wachtsom", GEHEEL),
            TmsDocumenten.attribuut("stilsom", GEHEEL), TmsDocumenten.attribuut("topsom", GEHEEL)),
            List.of(tijdkenmerk("wachtplichtig", "month")))), List.of());

    static final String WACHT = rekenregel("wachtsom", "BEREKENING", "Jutter", "wachtsom",
            eigenTotaal("vergoeding", "Jutter", check("wachtplichtig", false)), "null");
    static final String STIL = rekenregel("stilsom", "BEREKENING", "Jutter", "stilsom",
            eigenTotaal("vergoeding", "Jutter", check("wachtplichtig", true)), "null");
    static final String TOP = rekenregel("topsom", "BEREKENING", "Jutter", "topsom",
            eigenTotaal("vergoeding", "Jutter", vergelijking("greater", lezing("vergoeding", "Jutter"), getal("10"))),
            "null");

    static JsonNode zaakD() throws Exception {
        String vergoeding = tijdlijn(List.of(segment("2023-01-01", "2024-01-01", "10"),
                segment("2024-01-01", "2024-07-01", "11")));
        return TmsTijdlijnDocumenten.zaak(null, List.of(
                        instantie("J1", "Jutter", Map.of("vergoeding", vergoeding), List.of(),
                                Map.of("wachtplichtig", List.of(periode("2023-07-01", "2024-02-01"))), Map.of()),
                        instantie("J2", "Jutter", Map.of("vergoeding", vergoeding), List.of(), Map.of(), Map.of()),
                        instantie("J3", "Jutter", Map.of(), List.of(),
                                Map.of("wachtplichtig", List.of(periode("2023-07-01", "2024-02-01"))), Map.of())),
                List.of(waarde("J1", "wachtsom", "71"), waarde("J1", "stilsom", "115"), waarde("J1", "topsom", "66"),
                        waarde("J2", "wachtsom", "0"), waarde("J2", "stilsom", "186"), waarde("J2", "topsom", "66"),
                        waarde("J3", "wachtsom", "0"), waarde("J3", "stilsom", "0"), waarde("J3", "topsom", "0"),
                        consistent()));
    }

    @Test void een_knip_over_een_tijdsafhankelijk_kenmerk_leest_zijn_perioden_in_elke_volgorde() throws Exception {
        JsonNode zaak = zaakD();
        List<String> uitkomsten = new ArrayList<>(), verwacht = new ArrayList<>();
        for (List<String> volgorde : TmsDocumenten.permutaties(List.of(WACHT, STIL, TOP))) {
            List<String> namen = TmsDocumenten.regelnamen(volgorde);
            Path ir = TmsTijdlijnDocumenten.document(MODEL_D, volgorde, List.of());
            uitkomsten.add(namen + " " + TmsFoutWitnessTest.speel(ir, zaak, DroolsBackend.Branch.TRUTH_MAINTENANCE));
            verwacht.add(namen + " gespeeld");
        }
        assertEquals(verwacht, uitkomsten);
    }

    // ---------------- E: a day count onto a target on a fixed start point, ch. 3.2 ----------------

    static final String MODEL_E = TmsDocumenten.model(List.of(objecttype("Jutter", List.of(
            tijdattribuut("stormdagen", GEHEEL, "year", "--10-01"), tijdattribuut("kalenderdagen", GEHEEL, "year")),
            List.of(tijdkenmerk("wachtplichtig", "month")))), List.of());

    static final String STORMDAGEN = rekenregel("stormdagen", "BEREKENING", "Jutter", "stormdagen",
            dagentelling(check("wachtplichtig", false), "year"), "null");
    static final String KALENDERDAGEN = rekenregel("kalenderdagen", "BEREKENING", "Jutter", "kalenderdagen",
            dagentelling(check("wachtplichtig", false), "year"), "null");

    static JsonNode zaakE() throws Exception {
        return TmsTijdlijnDocumenten.zaak(null, List.of(instantie("J1", "Jutter", Map.of(), List.of(),
                        Map.of("wachtplichtig", List.of(periode("2024-09-28", "2024-10-03"))), Map.of())),
                List.of(verwachteTijdlijn("J1", "stormdagen", List.of(segment("2023-10-01", "2024-10-01", "3"),
                                segment("2024-10-01", "2025-10-01", "2"))),
                        verwachteTijdlijn("J1", "kalenderdagen", List.of(segment("2024-01-01", "2025-01-01", "5"))),
                        consistent()));
    }

    @Test void een_dagentelling_naar_een_doel_op_een_vast_startpunt_in_beide_volgordes() throws Exception {
        JsonNode zaak = zaakE();
        List<String> uitkomsten = new ArrayList<>(), verwacht = new ArrayList<>();
        for (List<String> volgorde : TmsDocumenten.permutaties(List.of(STORMDAGEN, KALENDERDAGEN))) {
            List<String> namen = TmsDocumenten.regelnamen(volgorde);
            Path ir = TmsTijdlijnDocumenten.document(MODEL_E, volgorde, List.of());
            uitkomsten.add(namen + " " + TmsFoutWitnessTest.speel(ir, zaak, DroolsBackend.Branch.TRUTH_MAINTENANCE));
            verwacht.add(namen + " gespeeld");
        }
        assertEquals(verwacht, uitkomsten);
    }

    // ---------------- the documents, for the other executors ----------------

    /** Writes every document of this class, in every order of its rules, with its case, under {@code args[0]}: a map
     *  per part and order, holding {@code ir.json} and {@code case.json}, for the verifier, ref and clingo. */
    public static void main(String[] args) throws Exception {
        Path uit = Path.of(args[0]);
        Map<String, Object[]> delen = new LinkedHashMap<>();
        delen.put("a-keten", new Object[]{MODEL_A, REGELS_A, zaakA()});
        delen.put("b-ontkend", new Object[]{MODEL_B, List.of(VRIJSTELLING, JAARDEEL_NIET_VRIJ), zaakB()});
        delen.put("b2-vervangen", new Object[]{MODEL_B2, List.of(TELLER, JAARDEEL_ONDER_VIJF), zaakB2()});
        delen.put("c-gedateerd", new Object[]{MODEL_C, List.of(JAARDEEL_GEDATEERD), zaakC()});
        delen.put("d-knip", new Object[]{MODEL_D, List.of(WACHT, STIL, TOP), zaakD()});
        delen.put("e-vast-startpunt", new Object[]{MODEL_E, List.of(STORMDAGEN, KALENDERDAGEN), zaakE()});
        for (Map.Entry<String, Object[]> d : delen.entrySet()) {
            @SuppressWarnings("unchecked") List<String> regels = (List<String>) d.getValue()[1];
            int n = 0;
            for (List<String> volgorde : TmsDocumenten.permutaties(regels))
                schrijf(uit.resolve(d.getKey()).resolve(String.format("v%03d", n++)),
                        TmsTijdlijnDocumenten.ir((String) d.getValue()[0], volgorde, List.of()),
                        (JsonNode) d.getValue()[2]);
        }
    }

    static void schrijf(Path map, String ir, JsonNode zaak) throws Exception {
        Files.createDirectories(map);
        Files.writeString(map.resolve("ir.json"), TmsDocumenten.JSON.writerWithDefaultPrettyPrinter()
                .writeValueAsString(TmsDocumenten.JSON.readTree(ir)));
        Files.writeString(map.resolve("case.json"), TmsDocumenten.JSON.writerWithDefaultPrettyPrinter()
                .writeValueAsString(zaak));
    }
}
