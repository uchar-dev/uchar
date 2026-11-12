package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static dev.uchar.regelspraak.cases.TmsDocuments.binary;
import static dev.uchar.regelspraak.cases.TmsDocuments.check;
import static dev.uchar.regelspraak.cases.TmsDocuments.comparison;
import static dev.uchar.regelspraak.cases.TmsDocuments.compound;
import static dev.uchar.regelspraak.cases.TmsDocuments.computingRule;
import static dev.uchar.regelspraak.cases.TmsDocuments.consistent;
import static dev.uchar.regelspraak.cases.TmsDocuments.number;
import static dev.uchar.regelspraak.cases.TmsDocuments.numberValue;
import static dev.uchar.regelspraak.cases.TmsDocuments.ownRead;
import static dev.uchar.regelspraak.cases.TmsDocuments.predicate;
import static dev.uchar.regelspraak.cases.TmsTimelineDocuments.WHOLE;
import static dev.uchar.regelspraak.cases.TmsTimelineDocuments.always;
import static dev.uchar.regelspraak.cases.TmsTimelineDocuments.characteristic;
import static dev.uchar.regelspraak.cases.TmsTimelineDocuments.dayCount;
import static dev.uchar.regelspraak.cases.TmsTimelineDocuments.expectedTimeline;
import static dev.uchar.regelspraak.cases.TmsTimelineDocuments.instance;
import static dev.uchar.regelspraak.cases.TmsTimelineDocuments.objectType;
import static dev.uchar.regelspraak.cases.TmsTimelineDocuments.openFrom;
import static dev.uchar.regelspraak.cases.TmsTimelineDocuments.openTo;
import static dev.uchar.regelspraak.cases.TmsTimelineDocuments.ownTotal;
import static dev.uchar.regelspraak.cases.TmsTimelineDocuments.period;
import static dev.uchar.regelspraak.cases.TmsTimelineDocuments.segment;
import static dev.uchar.regelspraak.cases.TmsTimelineDocuments.timeAttribute;
import static dev.uchar.regelspraak.cases.TmsTimelineDocuments.timeCharacteristic;
import static dev.uchar.regelspraak.cases.TmsTimelineDocuments.timeProportional;
import static dev.uchar.regelspraak.cases.TmsTimelineDocuments.timeline;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A condition read per period on a rule with a timeline target (RUNTIME-SEMANTICS ch. 3.10, the clip generalised), the
 * whole-period qualifier in it per bucket (ch. 3.9), a scalar as the whole value (ch. 3.11) and two writers of one
 * timeline target per day (ch. 5.7, "Two writers per day"), on both branches of the lowering with truth maintenance
 * ({@code PeriodCondition}, {@code PeriodArithmetic.clipped}). Every value below is computed by hand.
 *
 * <p><b>A, conditions per period on the period arithmetic and a scalar.</b> J1: strandloon 100 on
 * [2024-01-01, 2024-04-01) and 150 up to 2024-07-01, toeslag 30 on [2024-03-01, 2024-05-01), aangesteld on
 * [2024-02-01, 2024-06-01). "Het loon … als zijn strandloon plus zijn toeslag gedurende de tijd dat hij aangesteld is":
 * 100, 130, 180 and 150 on [02-01, 03-01), [03-01, 04-01), [04-01, 05-01), [05-01, 06-01). "Het vrije loon … als zijn
 * strandloon min zijn toeslag gedurende de tijd dat hij niet aangesteld is": 0 until 01-01, 100 up to 02-01, 150 on
 * [06-01, 07-01), 0 from 07-01. "Het hoge loon … als zijn strandloon plus 5 gedurende de tijd dat hij aan ten minste één
 * van de volgende voorwaarden voldoet: hij is aangesteld, zijn strandloon is ongelijk aan 100": a gap day reads leeg,
 * which is unequal to 100, so the composite holds until 01-01 and from 02-01: 5 until 01-01, 105 on [02-01, 04-01),
 * 155 up to 07-01, 5 from 07-01. "De premie … gesteld op 25 gedurende de tijd dat zijn strandloon groter is dan 120": 25
 * on [04-01, 07-01). J2 has no timeline: loon and premie the empty timeline, vrij loon 0 and hoog loon 5 on all time.
 * J3 is J1 with aangesteld given as two adjacent periods, [02-01, 04-01) and [04-01, 06-01): the periods of the check are
 * maximal, one period, so J3 has J1's timelines and J1's readings.
 *
 * <p><b>B, the whole-period qualifier per bucket.</b> J1: wachtplichtig (for each day) on [2025-11-16, 2026-03-13),
 * wachtloon 15 on [2025-01-01, 2026-07-01). "De dagen … als het aantal dagen in de maand dat hij wachtplichtig is
 * gedurende de tijd dat hij niet gedurende de gehele maand wachtplichtig is": 0 until 2025-11-01, 15 in November 2025,
 * 12 in March 2026 and 0 from 2026-04-01, the count over all time 0 in every month without a true day (ch. 3.8);
 * "de premie … 50 gedurende de tijd dat hij gedurende de gehele maand wachtplichtig is": 50 on
 * [2025-12-01, 2026-03-01); "de rust … 7 gedurende de tijd dat hij gedurende de gehele maand niet wachtplichtig is", the
 * negation inside the qualifier per day: 7 until 2025-11-01 and from 2026-04-01; "de jaarpremie … 20 gedurende de tijd
 * dat zijn wachtloon gedurende het gehele jaar groter is dan 10": 20 on [2025-01-01, 2026-01-01). J2 has no timeline:
 * premie and jaarpremie empty, dagen 0 and rust 7 on all time.
 *
 * <p><b>C, two writers per day.</b> "De wachtdagen … 30 gedurende de tijd dat hij gedurende de gehele maand wachtplichtig
 * is" and "… het aantal dagen in de maand dat hij wachtplichtig is gedurende de tijd dat hij aan alle volgende
 * voorwaarden voldoet: hij is niet gedurende de gehele maand wachtplichtig, hij is niet gedurende de gehele maand niet
 * wachtplichtig", the months that are partly wachtplichtig, with J1 as in B and an input of 4 in January 2025, a month
 * neither decides: 4, 15, 30 on [2025-12-01, 2026-03-01) and 12. Two writers that decide a day with one value, the day
 * count under the qualifier and the one without a condition: 0 until 2025-11-01, 15, 31, 31, 28, 12 and 0 from
 * 2026-04-01, each month as the writer the document names first wrote it. Two
 * that differ on a day, the day count without a condition beside the 30: EF-016 on December 2025 with 30 and 31, the
 * rules and the values in the order of the document. And the two writers of a pro-rata part that a scalar gate each
 * admits on another Jutter: 306 and 31. A reader of the target of the first two, "het totaal van zijn wachtdagen", reads
 * their union with the input: 4 + 15 + 3 × 30 + 12 = 121.
 *
 * <p><b>D, what the branch refuses by name</b>, each in one sentence on both branches.
 *
 * <p><b>E, the readings</b> of A and B (RUNTIME-SEMANTICS ch. 5.12): the canonical text on both branches, line by line
 * that of the reference interpreter's trace ({@code runner --trace}).
 *
 * <p><b>Measured</b> 2026-11-03 on the documents of {@link #main}: the verifier passes all of them, and the reference
 * interpreter's runner gives the expectations here on every one and the trace of E.
 */
class TmsConditionPerPeriodWitnessTest {

    // ---------------- A: conditions per period on the period arithmetic and a scalar ----------------

    static final String MODEL_A = TmsDocuments.model(List.of(objectType("Jutter", List.of(
                    timeAttribute("strandloon", WHOLE, "month"), timeAttribute("toeslag", WHOLE, "month"),
                    timeAttribute("loon", WHOLE, "month"), timeAttribute("vrij", WHOLE, "month"),
                    timeAttribute("hoog", WHOLE, "month"), timeAttribute("premie", WHOLE, "month")),
            List.of(timeCharacteristic("aangesteld", "month"), characteristic("actief")))), List.of());

    static final String LOON = computingRule("loon", "BEREKENING", "Jutter", "loon",
            binary("plus", ownRead("strandloon", "Jutter"), ownRead("toeslag", "Jutter")), check("aangesteld", false));
    static final String VRIJ = computingRule("vrij", "BEREKENING", "Jutter", "vrij",
            binary("minus", ownRead("strandloon", "Jutter"), ownRead("toeslag", "Jutter")), check("aangesteld", true));
    static final String HOOG = computingRule("hoog", "BEREKENING", "Jutter", "hoog",
            binary("plus", ownRead("strandloon", "Jutter"), number("5")), compound("atLeast", 1, List.of(
                    check("aangesteld", false), comparison("notEqual", ownRead("strandloon", "Jutter"), number("100")))));
    static final String PREMIE = computingRule("premie", "GELIJKSTELLING", "Jutter", "premie", number("25"),
            comparison("greater", ownRead("strandloon", "Jutter"), number("120")));

    static final String STRANDLOON = timeline(List.of(segment("2024-01-01", "2024-04-01", "100"),
            segment("2024-04-01", "2024-07-01", "150")));
    static final String TOESLAG = timeline(List.of(segment("2024-03-01", "2024-05-01", "30")));

    static JsonNode caseA() throws Exception {
        return TmsTimelineDocuments.caseDocument(null, List.of(
                        instance("J1", "Jutter", Map.of("strandloon", STRANDLOON, "toeslag", TOESLAG), List.of(),
                                Map.of("aangesteld", List.of(period("2024-02-01", "2024-06-01"))), Map.of()),
                        instance("J2", "Jutter", Map.of(), List.of(), Map.of(), Map.of()),
                        instance("J3", "Jutter", Map.of("strandloon", STRANDLOON, "toeslag", TOESLAG), List.of(),
                                Map.of("aangesteld", List.of(period("2024-02-01", "2024-04-01"),
                                        period("2024-04-01", "2024-06-01"))), Map.of())),
                List.of(expectedTimeline("J1", "loon", List.of(segment("2024-02-01", "2024-03-01", "100"),
                                segment("2024-03-01", "2024-04-01", "130"), segment("2024-04-01", "2024-05-01", "180"),
                                segment("2024-05-01", "2024-06-01", "150"))),
                        expectedTimeline("J1", "vrij", List.of(openTo("2024-01-01", "0"),
                                segment("2024-01-01", "2024-02-01", "100"), segment("2024-06-01", "2024-07-01", "150"),
                                openFrom("2024-07-01", "0"))),
                        expectedTimeline("J1", "hoog", List.of(openTo("2024-01-01", "5"),
                                segment("2024-02-01", "2024-04-01", "105"), segment("2024-04-01", "2024-07-01", "155"),
                                openFrom("2024-07-01", "5"))),
                        expectedTimeline("J1", "premie", List.of(segment("2024-04-01", "2024-07-01", "25"))),
                        expectedTimeline("J3", "loon", List.of(segment("2024-02-01", "2024-03-01", "100"),
                                segment("2024-03-01", "2024-04-01", "130"), segment("2024-04-01", "2024-05-01", "180"),
                                segment("2024-05-01", "2024-06-01", "150"))),
                        expectedTimeline("J3", "vrij", List.of(openTo("2024-01-01", "0"),
                                segment("2024-01-01", "2024-02-01", "100"), segment("2024-06-01", "2024-07-01", "150"),
                                openFrom("2024-07-01", "0"))),
                        expectedTimeline("J3", "hoog", List.of(openTo("2024-01-01", "5"),
                                segment("2024-02-01", "2024-04-01", "105"), segment("2024-04-01", "2024-07-01", "155"),
                                openFrom("2024-07-01", "5"))),
                        expectedTimeline("J3", "premie", List.of(segment("2024-04-01", "2024-07-01", "25"))),
                        expectedTimeline("J2", "loon", List.of()), expectedTimeline("J2", "vrij", List.of(always("0"))),
                        expectedTimeline("J2", "hoog", List.of(always("5"))), expectedTimeline("J2", "premie", List.of()),
                        consistent()));
    }

    @Test void conditions_per_period_on_the_period_arithmetic_and_a_scalar_in_every_order_on_both_branches()
            throws Exception {
        assertEquals(TmsPeriodArithmeticWitnessTest.gespeeld(4 * 3 * 2), TmsPeriodArithmeticWitnessTest.inEveryOrder(
                MODEL_A, List.of(LOON, VRIJ, HOOG, PREMIE), caseA()));
    }

    /** "Het loon … als zijn strandloon plus zijn toeslag indien hij actief is": a condition that reads no timeline gates
     *  the period arithmetic, which writes its whole timeline where the condition holds and nothing where it does not
     *  (RUNTIME-SEMANTICS ch. 3.10). */
    @Test void a_condition_that_reads_no_timeline_gates_the_period_arithmetic() throws Exception {
        String gated = computingRule("loon", "BEREKENING", "Jutter", "loon",
                binary("plus", ownRead("strandloon", "Jutter"), ownRead("toeslag", "Jutter")), check("actief", false));
        JsonNode c = TmsTimelineDocuments.caseDocument(null, List.of(
                        instance("J1", "Jutter", Map.of("strandloon", STRANDLOON, "toeslag", TOESLAG), List.of("actief"),
                                Map.of(), Map.of()),
                        instance("J2", "Jutter", Map.of("strandloon", STRANDLOON), List.of(), Map.of(), Map.of())),
                List.of(expectedTimeline("J1", "loon", List.of(openTo("2024-01-01", "0"),
                                segment("2024-01-01", "2024-03-01", "100"), segment("2024-03-01", "2024-04-01", "130"),
                                segment("2024-04-01", "2024-05-01", "180"), segment("2024-05-01", "2024-07-01", "150"),
                                openFrom("2024-07-01", "0"))),
                        expectedTimeline("J2", "loon", List.of()), consistent()));
        BothBranches.green(document(MODEL_A, List.of(gated)), c, "de poort van een voorwaarde zonder tijdlijn");
    }

    /** "Het loon … als zijn strandloon plus zijn toeslag gedurende de tijd dat hij aan alle volgende voorwaarden voldoet:
     *  hij is aangesteld, hij is actief": a member that reads no timeline holds on every day or on none, so on J1, actief,
     *  the loon is that of A, and on J2, aangesteld as J1 and not actief, the empty timeline. */
    static final String LOON_ACTIEF = computingRule("loon", "BEREKENING", "Jutter", "loon",
            binary("plus", ownRead("strandloon", "Jutter"), ownRead("toeslag", "Jutter")),
            compound("all", 0, List.of(check("aangesteld", false), check("actief", false))));

    static JsonNode caseActief() throws Exception {
        List<String> aangesteld = List.of(period("2024-02-01", "2024-06-01"));
        return TmsTimelineDocuments.caseDocument(null, List.of(
                        instance("J1", "Jutter", Map.of("strandloon", STRANDLOON, "toeslag", TOESLAG), List.of("actief"),
                                Map.of("aangesteld", aangesteld), Map.of()),
                        instance("J2", "Jutter", Map.of("strandloon", STRANDLOON, "toeslag", TOESLAG), List.of(),
                                Map.of("aangesteld", aangesteld), Map.of())),
                List.of(expectedTimeline("J1", "loon", List.of(segment("2024-02-01", "2024-03-01", "100"),
                                segment("2024-03-01", "2024-04-01", "130"), segment("2024-04-01", "2024-05-01", "180"),
                                segment("2024-05-01", "2024-06-01", "150"))),
                        expectedTimeline("J2", "loon", List.of()), consistent()));
    }

    @Test void a_member_that_reads_no_timeline_holds_on_every_day_or_on_none() throws Exception {
        BothBranches.green(document(MODEL_A, List.of(LOON_ACTIEF)), caseActief(), "een lid zonder tijdlijn");
    }

    /** "De premie … 25 gedurende de tijd dat 120 kleiner is dan zijn strandloon": the timeline on the right, its
     *  operator reversed, so the premie is that of A, 25 on [2024-04-01, 2024-07-01). */
    static final String PREMIE_RIGHT = computingRule("premie", "GELIJKSTELLING", "Jutter", "premie", number("25"),
            comparison("less", number("120"), ownRead("strandloon", "Jutter")));

    static JsonNode caseRight() throws Exception {
        return TmsTimelineDocuments.caseDocument(null, List.of(instance("J1", "Jutter",
                        Map.of("strandloon", STRANDLOON), List.of(), Map.of(), Map.of())),
                List.of(expectedTimeline("J1", "premie", List.of(segment("2024-04-01", "2024-07-01", "25"))),
                        consistent()));
    }

    @Test void a_comparison_with_the_timeline_on_the_right_reverses_its_operator() throws Exception {
        BothBranches.green(document(MODEL_A, List.of(PREMIE_RIGHT)), caseRight(), "de tijdlijn rechts");
    }

    /** "De premie … 25 gedurende de tijd dat zijn strandloon groter is dan 120" where the case gives the strandloon as
     *  the asOfDate-bucket scalar: the condition reads the timeline whole and faults (RUNTIME-SEMANTICS ch. 8.4), and
     *  its reading faults at the comparison. */
    static JsonNode caseBucket() throws Exception {
        return TmsTimelineDocuments.caseDocument("2024-02-15", List.of(instance("J1", "Jutter",
                Map.of("strandloon", numberValue("130")), List.of(), Map.of(), Map.of())), List.of(consistent()));
    }

    @Test void a_condition_per_period_over_the_bucket_scalar_faults() throws Exception {
        Path ir = document(MODEL_A, List.of(PREMIE));
        assertEquals("InputFormFault: fout in de regel \"premie\": a condition per period reads the time-dependent"
                + " attribute \"strandloon\" of \"J1\" whole; the case supplies only the asOfDate-bucket scalar, which"
                + " carries no period", TmsFaultWitnessTest.playBoth(ir, caseBucket()));
        // the fault stands at the read that raised it, with its kind EF-037 (ch. 5.7b), and at the comparison that
        // passes it on, as in a scalar condition, and fails the reading, and nothing after it is reached
        // (RUNTIME-SEMANTICS ch. 5.12)
        Path c = Files.writeString(Files.createTempFile("tms-emmerscalair", ".json"), caseBucket().toString());
        c.toFile().deleteOnExit();
        String r = "reading /rules/rules/0/versions/0 \"J1\" ";
        readings(ir, c, List.of("case " + c.getFileName(), r + "0 Comparison fault EF-037", r + "1 AttributeReference fault EF-037",
                r + "2 Literal unreached", r + "3 Literal unreached", r + "FAULTS", "READINGS 1"));
    }

    // ---------------- B: the whole-period qualifier per bucket ----------------

    static final String MODEL_B = TmsDocuments.model(List.of(objectType("Jutter", List.of(
                    timeAttribute("wachtloon", WHOLE, "month"), timeAttribute("dagen", WHOLE, "month"),
                    timeAttribute("premie", WHOLE, "month"), timeAttribute("rust", WHOLE, "month"),
                    timeAttribute("jaarpremie", WHOLE, "month"), timeAttribute("wachtdagen", WHOLE, "month")),
            List.of(timeCharacteristic("wachtplichtig", "day")))), List.of());

    /** "hij is [niet] {@code characteristic}" under "[niet] gedurende de gehele {@code granularity}". */
    static String wholePeriodCheck(String characteristic, boolean negated, String granularity, boolean qualifierNegated) {
        return "{\"node\":\"CharacteristicCheck\",\"characteristic\":\"" + characteristic + "\",\"negated\":" + negated
                + ",\"wholePeriod\":{\"node\":\"WholePeriod\",\"granularity\":\"" + granularity + "\",\"negated\":"
                + qualifierNegated + "},\"source\":{\"snippet\":\"hij " + (qualifierNegated ? "niet " : "")
                + "gedurende de gehele " + granularity + (negated ? " niet " : " ") + characteristic + " is\"}}";
    }

    /** A comparison under "gedurende het gehele {@code granularity}". */
    static String wholePeriodComparison(String op, String left, String right, String granularity) {
        return "{\"node\":\"Comparison\",\"op\":\"" + op + "\",\"left\":" + left + ",\"right\":" + right
                + ",\"wholePeriod\":{\"node\":\"WholePeriod\",\"granularity\":\"" + granularity + "\",\"negated\":false},"
                + "\"source\":{\"snippet\":\"" + op + " gedurende het gehele " + granularity + "\"}}";
    }

    static final String WACHTPLICHTIG = check("wachtplichtig", false);

    static final String DAGEN = computingRule("dagen", "BEREKENING", "Jutter", "dagen", dayCount(WACHTPLICHTIG, "month"),
            wholePeriodCheck("wachtplichtig", false, "month", true));
    static final String PREMIE_B = computingRule("premie", "GELIJKSTELLING", "Jutter", "premie", number("50"),
            wholePeriodCheck("wachtplichtig", false, "month", false));
    static final String RUST = computingRule("rust", "GELIJKSTELLING", "Jutter", "rust", number("7"),
            wholePeriodCheck("wachtplichtig", true, "month", false));
    static final String JAARPREMIE = computingRule("jaarpremie", "GELIJKSTELLING", "Jutter", "jaarpremie", number("20"),
            wholePeriodComparison("greater", ownRead("wachtloon", "Jutter"), number("10"), "year"));

    static final List<String> WACHT = List.of(period("2025-11-16", "2026-03-13"));

    static JsonNode caseB() throws Exception {
        return TmsTimelineDocuments.caseDocument(null, List.of(
                        instance("J1", "Jutter", Map.of("wachtloon", timeline(List.of(segment("2025-01-01", "2026-07-01",
                                "15")))), List.of(), Map.of("wachtplichtig", WACHT), Map.of()),
                        instance("J2", "Jutter", Map.of(), List.of(), Map.of(), Map.of())),
                List.of(expectedTimeline("J1", "dagen", List.of(openTo("2025-11-01", "0"),
                                segment("2025-11-01", "2025-12-01", "15"), segment("2026-03-01", "2026-04-01", "12"),
                                openFrom("2026-04-01", "0"))),
                        expectedTimeline("J1", "premie", List.of(segment("2025-12-01", "2026-03-01", "50"))),
                        expectedTimeline("J1", "rust", List.of(openTo("2025-11-01", "7"), openFrom("2026-04-01", "7"))),
                        expectedTimeline("J1", "jaarpremie", List.of(segment("2025-01-01", "2026-01-01", "20"))),
                        expectedTimeline("J2", "dagen", List.of(always("0"))), expectedTimeline("J2", "premie", List.of()),
                        expectedTimeline("J2", "rust", List.of(always("7"))), expectedTimeline("J2", "jaarpremie", List.of()),
                        consistent()));
    }

    @Test void the_whole_period_qualifier_per_bucket_in_every_order_on_both_branches() throws Exception {
        assertEquals(TmsPeriodArithmeticWitnessTest.gespeeld(4 * 3 * 2), TmsPeriodArithmeticWitnessTest.inEveryOrder(
                MODEL_B, List.of(DAGEN, PREMIE_B, RUST, JAARPREMIE), caseB()));
    }

    // ---------------- C: two writers per day ----------------

    static final String VOLLE_MAAND = computingRule("volle maand", "GELIJKSTELLING", "Jutter", "wachtdagen", number("30"),
            wholePeriodCheck("wachtplichtig", false, "month", false));
    static final String GEDEELTELIJKE_MAAND = computingRule("gedeeltelijke maand", "BEREKENING", "Jutter", "wachtdagen",
            dayCount(WACHTPLICHTIG, "month"), compound("all", 0, List.of(
                    wholePeriodCheck("wachtplichtig", false, "month", true),
                    wholePeriodCheck("wachtplichtig", true, "month", true))));
    static final String TELLING_VOLLE_MAAND = computingRule("telling volle maand", "BEREKENING", "Jutter", "wachtdagen",
            dayCount(WACHTPLICHTIG, "month"), wholePeriodCheck("wachtplichtig", false, "month", false));
    static final String ELKE_MAAND = computingRule("elke maand", "BEREKENING", "Jutter", "wachtdagen",
            dayCount(WACHTPLICHTIG, "month"), "null");

    static JsonNode caseTwoWriters() throws Exception {
        return TmsTimelineDocuments.caseDocument(null, List.of(instance("J1", "Jutter", Map.of("wachtdagen",
                        timeline(List.of(segment("2025-01-01", "2025-02-01", "4")))), List.of(),
                        Map.of("wachtplichtig", WACHT), Map.of())),
                List.of(expectedTimeline("J1", "wachtdagen", List.of(segment("2025-01-01", "2025-02-01", "4"),
                        segment("2025-11-01", "2025-12-01", "15"), segment("2025-12-01", "2026-03-01", "30"),
                        segment("2026-03-01", "2026-04-01", "12"))), consistent()));
    }

    @Test void two_writers_each_decide_their_own_days_with_the_input_on_the_others_in_every_order_on_both_branches()
            throws Exception {
        assertEquals(TmsPeriodArithmeticWitnessTest.gespeeld(2), TmsPeriodArithmeticWitnessTest.inEveryOrder(
                MODEL_B, List.of(VOLLE_MAAND, GEDEELTELIJKE_MAAND), caseTwoWriters()));
    }

    static JsonNode caseEqualWriters() throws Exception {
        return TmsTimelineDocuments.caseDocument(null, List.of(instance("J1", "Jutter", Map.of(), List.of(),
                        Map.of("wachtplichtig", WACHT), Map.of())),
                List.of(expectedTimeline("J1", "wachtdagen", List.of(openTo("2025-11-01", "0"),
                        segment("2025-11-01", "2025-12-01", "15"), segment("2025-12-01", "2026-01-01", "31"),
                        segment("2026-01-01", "2026-02-01", "31"), segment("2026-02-01", "2026-03-01", "28"),
                        segment("2026-03-01", "2026-04-01", "12"), openFrom("2026-04-01", "0"))),
                        consistent()));
    }

    @Test void two_writers_that_decide_a_day_with_one_value_hold_together_in_every_order_on_both_branches()
            throws Exception {
        assertEquals(TmsPeriodArithmeticWitnessTest.gespeeld(2), TmsPeriodArithmeticWitnessTest.inEveryOrder(
                MODEL_B, List.of(TELLING_VOLLE_MAAND, ELKE_MAAND), caseEqualWriters()));
    }

    /** "Het totaal … als het totaal van zijn wachtdagen", beside the two writers of {@link #caseTwoWriters}. */
    static final String READER = computingRule("totaal", "BEREKENING", "Jutter", "totaal",
            ownTotal("wachtdagen", "Jutter", "null"), "null");
    static final String MODEL_READER = TmsDocuments.model(List.of(objectType("Jutter", List.of(
                    timeAttribute("wachtdagen", WHOLE, "month"), TmsDocuments.attribute("totaal", WHOLE)),
            List.of(timeCharacteristic("wachtplichtig", "day")))), List.of());

    static JsonNode caseReader() throws Exception {
        return TmsTimelineDocuments.caseDocument(null, List.of(instance("J1", "Jutter", Map.of("wachtdagen",
                        timeline(List.of(segment("2025-01-01", "2025-02-01", "4")))), List.of(),
                        Map.of("wachtplichtig", WACHT), Map.of())),
                List.of(expectedTimeline("J1", "wachtdagen", List.of(segment("2025-01-01", "2025-02-01", "4"),
                        segment("2025-11-01", "2025-12-01", "15"), segment("2025-12-01", "2026-03-01", "30"),
                        segment("2026-03-01", "2026-04-01", "12"))), TmsDocuments.value("J1", "totaal", "121"),
                        consistent()));
    }

    /** A reader of a timeline target of two writers reads its standing, their union with the input on the days none
     *  decides: the total of 4, 15, 30 in each of three months and 12 is 121. */
    @Test void a_reader_of_a_target_of_two_writers_reads_their_union_in_every_order_on_both_branches() throws Exception {
        assertEquals(TmsPeriodArithmeticWitnessTest.gespeeld(6), TmsPeriodArithmeticWitnessTest.inEveryOrder(
                MODEL_READER, List.of(VOLLE_MAAND, GEDEELTELIJKE_MAAND, READER), caseReader()));
    }

    static JsonNode caseClash() throws Exception {
        return TmsTimelineDocuments.caseDocument(null, List.of(instance("J1", "Jutter", Map.of(), List.of(),
                Map.of("wachtplichtig", WACHT), Map.of())), List.of(consistent()));
    }

    /** The 30 and the day count without a condition both decide December 2025, with 30 and 31: EF-016, the rules and
     *  their values on that day in the order of the document, in both orders. */
    @Test void two_writers_that_differ_on_a_day_fault_with_ef_016_in_every_order_on_both_branches() throws Exception {
        JsonNode c = caseClash();
        String sentence = "WritersConflict: geen fixpunt: twee schrijvers van één doel geven verschillende waarden — de"
                + " regel \"%s\" schrijft %s en de regel \"%s\" schrijft %s in het attribuut wachtdagen van J1, een"
                + " Jutter; divergentie is een fout (RUNTIME-SEMANTICS ch. 5.7)";
        assertEquals(String.format(sentence, "volle maand", "30", "elke maand", "31"),
                TmsFaultWitnessTest.playBoth(document(MODEL_B, List.of(VOLLE_MAAND, ELKE_MAAND)), c));
        assertEquals(String.format(sentence, "elke maand", "31", "volle maand", "30"),
                TmsFaultWitnessTest.playBoth(document(MODEL_B, List.of(ELKE_MAAND, VOLLE_MAAND)), c));
    }

    /** "Het jaardeel … het tijdsevenredig deel per jaar van zijn jaartarief gedurende de tijd dat hij borgplichtig is,
     *  indien zijn score groter is dan 5" beside the same over wachtplichtig "indien zijn score kleiner is dan 6": on
     *  each Jutter one of the two decides, 306 and 31 (366 × 306/366 and 366 × 31/366). */
    static final String MODEL_JAARDEEL = TmsDocuments.model(List.of(objectType("Jutter", List.of(
                    TmsDocuments.attribute("score", WHOLE), timeAttribute("jaartarief", WHOLE, "year"),
                    timeAttribute("jaardeel", WHOLE, "year")),
            List.of(timeCharacteristic("borgplichtig", "month"), timeCharacteristic("wachtplichtig", "month")))), List.of());
    static final String JAARDEEL_BORG = computingRule("jaardeel borg", "BEREKENING", "Jutter", "jaardeel",
            timeProportional("jaartarief", "Jutter", check("borgplichtig", false), "year"),
            comparison("greater", ownRead("score", "Jutter"), number("5")));
    static final String JAARDEEL_WACHT = computingRule("jaardeel wacht", "BEREKENING", "Jutter", "jaardeel",
            timeProportional("jaartarief", "Jutter", check("wachtplichtig", false), "year"),
            comparison("less", ownRead("score", "Jutter"), number("6")));

    static JsonNode caseJaardeel() throws Exception {
        String rate = timeline(List.of(segment("2024-01-01", "2025-01-01", "366")));
        Map<String, List<String>> both = Map.of("borgplichtig", List.of(period("2024-03-01", "2025-01-01")),
                "wachtplichtig", List.of(period("2024-01-01", "2024-02-01")));
        return TmsTimelineDocuments.caseDocument(null, List.of(
                        instance("J1", "Jutter", Map.of("score", numberValue("10"), "jaartarief", rate), List.of(), both,
                                Map.of()),
                        instance("J2", "Jutter", Map.of("score", numberValue("3"), "jaartarief", rate), List.of(), both,
                                Map.of())),
                List.of(expectedTimeline("J1", "jaardeel", List.of(segment("2024-01-01", "2025-01-01", "306"))),
                        expectedTimeline("J2", "jaardeel", List.of(segment("2024-01-01", "2025-01-01", "31")))));
    }

    @Test void two_writers_of_a_pro_rata_part_each_under_a_gate_in_every_order_on_both_branches() throws Exception {
        assertEquals(TmsPeriodArithmeticWitnessTest.gespeeld(2), TmsPeriodArithmeticWitnessTest.inEveryOrder(
                MODEL_JAARDEEL, List.of(JAARDEEL_BORG, JAARDEEL_WACHT), caseJaardeel()));
    }

    // ---------------- D: what the branch refuses by name ----------------

    @Test void every_form_outside_what_the_branch_carries_is_refused_by_name_on_both_branches() throws Exception {
        JsonNode a = TmsTimelineDocuments.caseDocument(null, List.of(instance("J1", "Jutter",
                Map.of("strandloon", STRANDLOON), List.of(), Map.of(), Map.of())), List.of(consistent()));
        String plus5 = binary("plus", ownRead("strandloon", "Jutter"), number("5"));
        // a comparison of two timelines
        refused(document(MODEL_A, List.of(computingRule("hoog", "BEREKENING", "Jutter", "hoog", plus5,
                        comparison("greater", ownRead("strandloon", "Jutter"), ownRead("toeslag", "Jutter"))))), a,
                "regel: hoog — een voorwaarde per periode op het tijdsafhankelijke doel 'hoog' van Jutter in een vorm die"
                        + " deze tak niet per periode leest ('greater')", "DROOLS-309");
        // a leeg test of a timeline
        refused(document(MODEL_A, List.of(computingRule("hoog", "BEREKENING", "Jutter", "hoog", plus5,
                        predicate("empty", ownRead("toeslag", "Jutter"))))), a,
                "regel: hoog — een voorwaarde per periode op het tijdsafhankelijke doel 'hoog' van Jutter in een vorm die"
                        + " deze tak niet per periode leest ('empty')", "DROOLS-309");
        // a whole-period qualifier on a characteristic that holds one truth value
        refused(document(MODEL_A, List.of(computingRule("hoog", "BEREKENING", "Jutter", "hoog", plus5,
                        wholePeriodCheck("actief", false, "month", false)))), a,
                "regel: hoog — een voorwaarde per periode op het tijdsafhankelijke doel 'hoog' van Jutter in een vorm die"
                        + " deze tak niet per periode leest ('hij gedurende de gehele month actief is')", "DROOLS-309");
        // a characteristic for each day in the condition of a target for each month (IR-SPEC WF-126)
        JsonNode b = TmsTimelineDocuments.caseDocument(null, List.of(instance("J1", "Jutter", Map.of(), List.of(),
                Map.of("wachtplichtig", WACHT), Map.of())), List.of(consistent()));
        refused(document(MODEL_B, List.of(computingRule("premie", "GELIJKSTELLING", "Jutter", "premie", number("50"),
                        WACHTPLICHTIG))), b,
                "regel: premie — 'hij wachtplichtig is' ligt op een raster van DAG, dat niet past in het MAAND-raster"
                        + " van het tijdsafhankelijke doel 'premie'", "DROOLS-310");
    }

    /** "De som van de opbrengst van alle vondsten van de Jutter" as an operand of a comparison in a condition per period,
     *  compared with 100 € ({@code ir.json}) and as the bound of the own timeline eigen opbrengst
     *  ({@code ir-bound.json}), the documents of {@code ref/test/fixtures/conditions-per-period/member-sum}: refused as a
     *  comparison of two timelines is, as the reference interpreter and the ASP backend refuse it (RUNTIME-SEMANTICS
     *  ch. 3.6). Measured before the refusal: both branches gave toeslag 5 € on all time and no toeslag on any day. */
    @Test void a_sum_over_member_timelines_in_a_comparison_per_period_is_refused_by_name_on_both_branches()
            throws Exception {
        Path dir = Path.of("..", "..", "ref", "test", "fixtures", "conditions-per-period", "member-sum");
        JsonNode c = new com.fasterxml.jackson.databind.ObjectMapper().readTree(Files.readString(dir.resolve("case.json")));
        refused(dir.resolve("ir.json"), c, "regel: toeslag — een voorwaarde per periode op het tijdsafhankelijke doel"
                + " 'toeslag' van Jutter in een vorm die deze tak niet per periode leest ('de som van de opbrengst van alle"
                + " vondsten van de Jutter groter is dan 100 €')", "DROOLS-309");
        refused(dir.resolve("ir-bound.json"), c, "regel: toeslag — een voorwaarde per periode op het tijdsafhankelijke"
                + " doel 'toeslag' van Jutter in een vorm die deze tak niet per periode leest ('zijn eigen opbrengst groter"
                + " is dan de som van de opbrengst van alle vondsten van de Jutter')", "DROOLS-309");
    }

    // ---------------- E: the readings ----------------

    /** The readings of A on J1, J2 and J3, line by line the reference interpreter's trace of the same document and case. */
    @Test void the_readings_of_conditions_per_period_are_those_of_the_reference_interpreter() throws Exception {
        Path dir = Files.createTempDirectory("tms-voorwaarde-per-periode");
        dir.toFile().deleteOnExit();
        Path ir = Files.writeString(dir.resolve("ir.json"), TmsTimelineDocuments.ir(MODEL_A,
                List.of(LOON, VRIJ, HOOG, PREMIE), List.of()));
        Path c = Files.writeString(dir.resolve("case.json"), caseA().toString());
        String r = "reading /rules/rules/";
        readings(ir, c, List.of("case case.json",
                r + "0/versions/0 \"J1\" 0 CharacteristicCheck [[2024-02-01 2024-06-01 empty]]",
                r + "0/versions/0 \"J1\" 1 Binary [[2024-02-01 2024-03-01 100], [2024-03-01 2024-04-01 130],"
                        + " [2024-04-01 2024-05-01 180], [2024-05-01 2024-06-01 150]]",
                r + "0/versions/0 \"J1\" 2 AttributeReference [[2024-01-01 2024-04-01 100],"
                        + " [2024-04-01 2024-07-01 150]]",
                r + "0/versions/0 \"J1\" 3 AttributeReference [[2024-03-01 2024-05-01 30]]",
                r + "0/versions/0 \"J1\" HOLDS",
                r + "0/versions/0 \"J2\" 0 CharacteristicCheck []",
                r + "0/versions/0 \"J2\" 1 Binary []",
                r + "0/versions/0 \"J2\" 2 AttributeReference []",
                r + "0/versions/0 \"J2\" 3 AttributeReference []",
                r + "0/versions/0 \"J2\" HOLDS",
                r + "0/versions/0 \"J3\" 0 CharacteristicCheck [[2024-02-01 2024-06-01 empty]]",
                r + "0/versions/0 \"J3\" 1 Binary [[2024-02-01 2024-03-01 100], [2024-03-01 2024-04-01 130],"
                        + " [2024-04-01 2024-05-01 180], [2024-05-01 2024-06-01 150]]",
                r + "0/versions/0 \"J3\" 2 AttributeReference [[2024-01-01 2024-04-01 100],"
                        + " [2024-04-01 2024-07-01 150]]",
                r + "0/versions/0 \"J3\" 3 AttributeReference [[2024-03-01 2024-05-01 30]]",
                r + "0/versions/0 \"J3\" HOLDS",
                r + "1/versions/0 \"J1\" 0 CharacteristicCheck [[- 2024-02-01 empty], [2024-06-01 - empty]]",
                r + "1/versions/0 \"J1\" 1 Binary [[- 2024-01-01 0], [2024-01-01 2024-02-01 100],"
                        + " [2024-06-01 2024-07-01 150], [2024-07-01 - 0]]",
                r + "1/versions/0 \"J1\" 2 AttributeReference [[2024-01-01 2024-04-01 100],"
                        + " [2024-04-01 2024-07-01 150]]",
                r + "1/versions/0 \"J1\" 3 AttributeReference [[2024-03-01 2024-05-01 30]]",
                r + "1/versions/0 \"J1\" HOLDS",
                r + "1/versions/0 \"J2\" 0 CharacteristicCheck [[- - empty]]",
                r + "1/versions/0 \"J2\" 1 Binary [[- - 0]]",
                r + "1/versions/0 \"J2\" 2 AttributeReference []",
                r + "1/versions/0 \"J2\" 3 AttributeReference []",
                r + "1/versions/0 \"J2\" HOLDS",
                r + "1/versions/0 \"J3\" 0 CharacteristicCheck [[- 2024-02-01 empty], [2024-06-01 - empty]]",
                r + "1/versions/0 \"J3\" 1 Binary [[- 2024-01-01 0], [2024-01-01 2024-02-01 100],"
                        + " [2024-06-01 2024-07-01 150], [2024-07-01 - 0]]",
                r + "1/versions/0 \"J3\" 2 AttributeReference [[2024-01-01 2024-04-01 100],"
                        + " [2024-04-01 2024-07-01 150]]",
                r + "1/versions/0 \"J3\" 3 AttributeReference [[2024-03-01 2024-05-01 30]]",
                r + "1/versions/0 \"J3\" HOLDS",
                r + "2/versions/0 \"J1\" 0 Compound [[- 2024-01-01 empty], [2024-02-01 - empty]]",
                r + "2/versions/0 \"J1\" 1 CharacteristicCheck [[2024-02-01 2024-06-01 empty]]",
                r + "2/versions/0 \"J1\" 2 Comparison [[- 2024-01-01 empty], [2024-04-01 2024-07-01 150],"
                        + " [2024-07-01 - empty]]",
                r + "2/versions/0 \"J1\" 3 AttributeReference [[2024-01-01 2024-04-01 100],"
                        + " [2024-04-01 2024-07-01 150]]",
                r + "2/versions/0 \"J1\" 4 Literal 100",
                r + "2/versions/0 \"J1\" 5 Binary [[- 2024-01-01 5], [2024-02-01 2024-04-01 105],"
                        + " [2024-04-01 2024-07-01 155], [2024-07-01 - 5]]",
                r + "2/versions/0 \"J1\" 6 AttributeReference [[2024-01-01 2024-04-01 100],"
                        + " [2024-04-01 2024-07-01 150]]",
                r + "2/versions/0 \"J1\" 7 Literal 5",
                r + "2/versions/0 \"J1\" HOLDS",
                r + "2/versions/0 \"J2\" 0 Compound [[- - empty]]",
                r + "2/versions/0 \"J2\" 1 CharacteristicCheck []",
                r + "2/versions/0 \"J2\" 2 Comparison [[- - empty]]",
                r + "2/versions/0 \"J2\" 3 AttributeReference empty",
                r + "2/versions/0 \"J2\" 4 Literal 100",
                r + "2/versions/0 \"J2\" 5 Binary [[- - 5]]",
                r + "2/versions/0 \"J2\" 6 AttributeReference []",
                r + "2/versions/0 \"J2\" 7 Literal 5",
                r + "2/versions/0 \"J2\" HOLDS",
                r + "2/versions/0 \"J3\" 0 Compound [[- 2024-01-01 empty], [2024-02-01 - empty]]",
                r + "2/versions/0 \"J3\" 1 CharacteristicCheck [[2024-02-01 2024-06-01 empty]]",
                r + "2/versions/0 \"J3\" 2 Comparison [[- 2024-01-01 empty], [2024-04-01 2024-07-01 150],"
                        + " [2024-07-01 - empty]]",
                r + "2/versions/0 \"J3\" 3 AttributeReference [[2024-01-01 2024-04-01 100],"
                        + " [2024-04-01 2024-07-01 150]]",
                r + "2/versions/0 \"J3\" 4 Literal 100",
                r + "2/versions/0 \"J3\" 5 Binary [[- 2024-01-01 5], [2024-02-01 2024-04-01 105],"
                        + " [2024-04-01 2024-07-01 155], [2024-07-01 - 5]]",
                r + "2/versions/0 \"J3\" 6 AttributeReference [[2024-01-01 2024-04-01 100],"
                        + " [2024-04-01 2024-07-01 150]]",
                r + "2/versions/0 \"J3\" 7 Literal 5",
                r + "2/versions/0 \"J3\" HOLDS",
                r + "3/versions/0 \"J1\" 0 Comparison [[2024-04-01 2024-07-01 150]]",
                r + "3/versions/0 \"J1\" 1 AttributeReference [[2024-01-01 2024-04-01 100],"
                        + " [2024-04-01 2024-07-01 150]]",
                r + "3/versions/0 \"J1\" 2 Literal 120",
                r + "3/versions/0 \"J1\" 3 Literal [[2024-04-01 2024-07-01 25]]",
                r + "3/versions/0 \"J1\" HOLDS",
                r + "3/versions/0 \"J2\" 0 Comparison []",
                r + "3/versions/0 \"J2\" 1 AttributeReference empty",
                r + "3/versions/0 \"J2\" 2 Literal 120",
                r + "3/versions/0 \"J2\" 3 Literal []",
                r + "3/versions/0 \"J2\" HOLDS",
                r + "3/versions/0 \"J3\" 0 Comparison [[2024-04-01 2024-07-01 150]]",
                r + "3/versions/0 \"J3\" 1 AttributeReference [[2024-01-01 2024-04-01 100],"
                        + " [2024-04-01 2024-07-01 150]]",
                r + "3/versions/0 \"J3\" 2 Literal 120",
                r + "3/versions/0 \"J3\" 3 Literal [[2024-04-01 2024-07-01 25]]",
                r + "3/versions/0 \"J3\" HOLDS",
                "READINGS 1"));
    }

    // ---------------- helpers ----------------

    /** The output of {@code --readings} on {@code ir} and {@code c} on both branches: {@code expected}, the readings in
     *  their sorted order after the line of the case. */
    static void readings(Path ir, Path c, List<String> expected) {
        for (boolean drl : List.of(false, true)) {
            java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
            List<String> arguments = new ArrayList<>(List.of("--readings"));
            if (drl) arguments.add("--drl");
            arguments.addAll(List.of(ir.toString(), c.toString()));
            int code = DroolsBackendMain.run(arguments.toArray(String[]::new),
                    new java.io.PrintStream(out, true, java.nio.charset.StandardCharsets.UTF_8));
            assertEquals(expected, out.toString(java.nio.charset.StandardCharsets.UTF_8).lines().toList(),
                    (drl ? "DRL_TEXT" : "EXEC_MODEL") + ": de canonieke tekst");
            assertEquals(0, code);
        }
    }

    /** Both branches refuse {@code ir} with the code {@code code}, in one sentence that names {@code fragment}. */
    static void refused(Path ir, JsonNode c, String fragment, String code) throws Exception {
        String exec = null;
        for (boolean viaDrl : new boolean[] { false, true }) {
            CaseRunner.Excluded e = assertThrows(CaseRunner.Excluded.class, () -> CaseRunner.playCase(ir, c, viaDrl),
                    code);
            assertEquals(code, e.code().id(), e.getMessage());
            assertTrue(e.getMessage().contains(fragment), "'" + fragment + "' in: " + e.getMessage());
            if (exec == null) exec = e.getMessage();
            else assertEquals(exec, e.getMessage(), "drl-tekst weigert in de zin van exec-model");
        }
    }

    static Path document(String model, List<String> rules) throws Exception {
        return TmsTimelineDocuments.document(model, rules, List.of());
    }

    /** Writes the documents of A, B and C with their cases under {@code args[0]}, for the verifier, ref and clingo. */
    public static void main(String[] args) throws Exception {
        Path out = Path.of(args[0]);
        TmsTimelineWitnessTest.write(out.resolve("a-voorwaarden"),
                TmsTimelineDocuments.ir(MODEL_A, List.of(LOON, VRIJ, HOOG, PREMIE), List.of()), caseA());
        TmsTimelineWitnessTest.write(out.resolve("b-kwalificator"),
                TmsTimelineDocuments.ir(MODEL_B, List.of(DAGEN, PREMIE_B, RUST, JAARPREMIE), List.of()), caseB());
        TmsTimelineWitnessTest.write(out.resolve("c-twee-schrijvers-invoer"),
                TmsTimelineDocuments.ir(MODEL_B, List.of(VOLLE_MAAND, GEDEELTELIJKE_MAAND), List.of()), caseTwoWriters());
        TmsTimelineWitnessTest.write(out.resolve("c-twee-schrijvers-gelijk"),
                TmsTimelineDocuments.ir(MODEL_B, List.of(TELLING_VOLLE_MAAND, ELKE_MAAND), List.of()), caseEqualWriters());
        TmsTimelineWitnessTest.write(out.resolve("c-botsing"),
                TmsTimelineDocuments.ir(MODEL_B, List.of(VOLLE_MAAND, ELKE_MAAND), List.of()), caseClash());
        TmsTimelineWitnessTest.write(out.resolve("c-botsing-omgekeerd"),
                TmsTimelineDocuments.ir(MODEL_B, List.of(ELKE_MAAND, VOLLE_MAAND), List.of()), caseClash());
        TmsTimelineWitnessTest.write(out.resolve("a-actief"),
                TmsTimelineDocuments.ir(MODEL_A, List.of(LOON_ACTIEF), List.of()), caseActief());
        TmsTimelineWitnessTest.write(out.resolve("a-tijdlijn-rechts"),
                TmsTimelineDocuments.ir(MODEL_A, List.of(PREMIE_RIGHT), List.of()), caseRight());
        TmsTimelineWitnessTest.write(out.resolve("a-emmerscalair"),
                TmsTimelineDocuments.ir(MODEL_A, List.of(PREMIE), List.of()), caseBucket());
        TmsTimelineWitnessTest.write(out.resolve("c-lezer-twee-schrijvers"),
                TmsTimelineDocuments.ir(MODEL_READER, List.of(VOLLE_MAAND, GEDEELTELIJKE_MAAND, READER), List.of()),
                caseReader());
        TmsTimelineWitnessTest.write(out.resolve("c-jaardeel"),
                TmsTimelineDocuments.ir(MODEL_JAARDEEL, List.of(JAARDEEL_BORG, JAARDEEL_WACHT), List.of()), caseJaardeel());
    }
}
