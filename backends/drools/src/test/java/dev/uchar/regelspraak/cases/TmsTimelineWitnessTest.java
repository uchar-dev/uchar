package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;

import dev.uchar.regelspraak.transpiler.tms.State;
import dev.uchar.regelspraak.transpiler.tms.Period;
import dev.uchar.regelspraak.transpiler.tms.Value;

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

import static dev.uchar.regelspraak.cases.TmsDocuments.check;
import static dev.uchar.regelspraak.cases.TmsDocuments.consistent;
import static dev.uchar.regelspraak.cases.TmsDocuments.number;
import static dev.uchar.regelspraak.cases.TmsDocuments.numberValue;
import static dev.uchar.regelspraak.cases.TmsDocuments.ownRead;
import static dev.uchar.regelspraak.cases.TmsDocuments.computingRule;
import static dev.uchar.regelspraak.cases.TmsDocuments.comparison;
import static dev.uchar.regelspraak.cases.TmsDocuments.value;
import static dev.uchar.regelspraak.cases.TmsTimelineDocuments.WHOLE;
import static dev.uchar.regelspraak.cases.TmsTimelineDocuments.dayCount;
import static dev.uchar.regelspraak.cases.TmsTimelineDocuments.ownTotal;
import static dev.uchar.regelspraak.cases.TmsTimelineDocuments.instance;
import static dev.uchar.regelspraak.cases.TmsTimelineDocuments.characteristic;
import static dev.uchar.regelspraak.cases.TmsTimelineDocuments.months;
import static dev.uchar.regelspraak.cases.TmsTimelineDocuments.objectType;
import static dev.uchar.regelspraak.cases.TmsTimelineDocuments.period;
import static dev.uchar.regelspraak.cases.TmsTimelineDocuments.segment;
import static dev.uchar.regelspraak.cases.TmsTimelineDocuments.timeAttribute;
import static dev.uchar.regelspraak.cases.TmsTimelineDocuments.timeCharacteristic;
import static dev.uchar.regelspraak.cases.TmsTimelineDocuments.timeline;
import static dev.uchar.regelspraak.cases.TmsTimelineDocuments.timeProportional;
import static dev.uchar.regelspraak.cases.TmsTimelineDocuments.expectedTimeline;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Timelines as values on the exec-model branch (RUNTIME-SEMANTICS ch. 3): a timeline in a fact is an immutable
 * {@code List<Period>}, bound in the left-hand side off its standing or off the bean, written by a pro-rata part or a
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
 * the fixpoint every object holds one {@link State} per timeline it reads, derived or input, and at most one
 * {@link Value} per target, and in some order the wachtsom of J1 was 0 on the way.
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
class TmsTimelineWitnessTest {

    // ---------------- A: a chain of timeline writers and the totals over them ----------------

    static final String BORGPLICHTIG = check("borgplichtig", false);

    static final String MODEL_A = TmsDocuments.model(List.of(objectType("Jutter", List.of(
            TmsDocuments.attribute("score", WHOLE), timeAttribute("jaartarief", WHOLE, "year"),
            timeAttribute("jaardeel", WHOLE, "year"), timeAttribute("maanddeel", WHOLE, "month"),
            timeAttribute("bedrag", WHOLE, "month"), TmsDocuments.attribute("wachtsom", WHOLE),
            TmsDocuments.attribute("topsom", WHOLE)),
            List.of(timeCharacteristic("borgplichtig", "month"), characteristic("actief")))), List.of());

    static final String ACTIEF = TmsDocuments.characteristicRule("actief", "Jutter", "actief",
            comparison("greater", ownRead("score", "Jutter"), number("5")));
    static final String JAARDEEL = computingRule("jaardeel", "BEREKENING", "Jutter", "jaardeel",
            timeProportional("jaartarief", "Jutter", BORGPLICHTIG, "year"), check("actief", false));
    static final String MONTH_PART = computingRule("maanddeel", "BEREKENING", "Jutter", "maanddeel",
            timeProportional("jaardeel", "Jutter", BORGPLICHTIG, "month"), "null");
    static final String WACHTSOM = computingRule("wachtsom", "BEREKENING", "Jutter", "wachtsom",
            ownTotal("maanddeel", "Jutter", BORGPLICHTIG), "null");
    static final String TOPSOM = computingRule("topsom", "BEREKENING", "Jutter", "topsom",
            ownTotal("bedrag", "Jutter", comparison("greater", ownRead("maanddeel", "Jutter"), number("100"))), "null");

    static final List<String> RULES_A = List.of(ACTIEF, JAARDEEL, MONTH_PART, WACHTSOM, TOPSOM);

    static final String RATE = timeline(List.of(segment("2024-01-01", "2025-01-01", "366"),
            segment("2025-01-01", "2026-01-01", "365")));
    static final String AMOUNT = timeline(List.of(segment("2024-01-01", "2025-07-01", "7")));
    static final String JUNE = timeline(List.of(segment("2024-06-01", "2024-07-01", "9")));
    static final Map<String, List<String>> BORG = Map.of("borgplichtig", List.of(period("2024-03-01", "2025-03-01")));
    static final List<String> YEAR_PARTS = List.of(segment("2024-01-01", "2025-01-01", "306"),
            segment("2025-01-01", "2026-01-01", "59"));

    static List<String> monthParts() {
        List<String> out = new ArrayList<>(months(2024, 3, 10, "306"));
        out.addAll(months(2025, 1, 2, "59"));
        return out;
    }

    static JsonNode caseA() throws Exception {
        return TmsTimelineDocuments.caseDocument(null, List.of(
                        instance("J1", "Jutter", Map.of("score", numberValue("10"), "jaartarief", RATE, "bedrag", AMOUNT),
                                List.of(), BORG, Map.of()),
                        instance("J2", "Jutter", Map.of("score", numberValue("3"), "jaartarief", RATE, "bedrag", AMOUNT,
                                "maanddeel", JUNE), List.of(), BORG, Map.of()),
                        instance("J3", "Jutter", Map.of("score", numberValue("10"), "jaartarief", RATE, "maanddeel", JUNE),
                                List.of(), BORG, Map.of())),
                List.of(expectedTimeline("J1", "jaardeel", YEAR_PARTS), expectedTimeline("J1", "maanddeel", monthParts()),
                        value("J1", "wachtsom", "3178"), value("J1", "topsom", "70"),
                        expectedTimeline("J2", "jaardeel", List.of()),
                        expectedTimeline("J2", "maanddeel", List.of(segment("2024-06-01", "2024-07-01", "9"))),
                        value("J2", "wachtsom", "9"), value("J2", "topsom", "0"),
                        expectedTimeline("J3", "jaardeel", YEAR_PARTS), expectedTimeline("J3", "maanddeel", monthParts()),
                        value("J3", "wachtsom", "3178"), value("J3", "topsom", "0"), consistent()));
    }

    @Test void a_chain_of_timeline_writers_and_the_totals_over_it_in_every_order_of_the_rules() throws Exception {
        JsonNode caseDocument = caseA();
        List<String> outcomes = new ArrayList<>(), expected = new ArrayList<>();
        for (List<String> order : TmsDocuments.permutations(RULES_A)) {
            List<String> names = TmsDocuments.ruleNames(order);
            Path ir = TmsTimelineDocuments.document(MODEL_A, order, List.of());
            outcomes.add(names + " " + TmsFaultWitnessTest.play(ir, caseDocument, DroolsBackend.Branch.EXEC_MODEL));
            expected.add(names + " gespeeld");
        }
        assertEquals(expected, outcomes);
    }

    @Test void at_the_fixpoint_a_state_and_at_most_one_value_per_object_and_member() throws Exception {
        Path ir = TmsTimelineDocuments.document(MODEL_A, RULES_A, List.of());
        DroolsBackend.Build b = DroolsBackend.build(ir, DroolsBackend.Branch.EXEC_MODEL);
        int intermediateState = 0;
        for (List<Rule> order : TmsDocuments.sample(b.execModelTranspiler().model().getRules(), 20261009L, 100)) {
            List<String> names = order.stream().map(Rule::getName).toList();
            Object j1 = jutter(b.beanLoader(), "10", true), j2 = jutter(b.beanLoader(), "3", false);
            Map<Object, String> labels = new IdentityHashMap<>(Map.of(j1, "J1", j2, "J2"));
            int[] intermediate = {0};
            Map<String, Integer> states = new TreeMap<>(), waarden = new TreeMap<>();
            TmsDocuments.fire(b, order, List.of(j1, j2), session -> session.addEventListener(
                    new DefaultRuleRuntimeEventListener() {
                        @Override public void objectInserted(ObjectInsertedEvent e) {
                            if (e.getObject() instanceof Value w && w.onderwerp() == j1
                                    && w.attribute().naam().equals("wachtsom")
                                    && ((BigDecimal) w.value()).compareTo(new BigDecimal("3178")) != 0) intermediate[0]++;
                        }
                    }), session -> {
                for (Object f : session.getObjects(o -> o instanceof State))
                    states.merge(labels.get(((State) f).onderwerp()) + "." + ((State) f).attribute().naam(), 1,
                            Integer::sum);
                for (Object f : session.getObjects(o -> o instanceof Value))
                    waarden.merge(labels.get(((Value) f).onderwerp()) + "." + ((Value) f).attribute().naam(), 1,
                            Integer::sum);
            });
            // the jaartarief and the bedrag no form writes stand as well, the bedrag of J2 as leeg: every attribute on a
            // timeline a rule reads has a standing, which carries its input in the convention the case gives it
            // (RUNTIME-SEMANTICS ch. 8.4)
            assertEquals(Map.of("J1.bedrag", 1, "J1.jaardeel", 1, "J1.jaartarief", 1, "J1.maanddeel", 1, "J2.bedrag", 1,
                    "J2.jaardeel", 1, "J2.jaartarief", 1, "J2.maanddeel", 1), states, () -> "standen in volgorde " + names);
            assertEquals(Map.of("J1.jaardeel", 1, "J1.maanddeel", 1, "J1.topsom", 1, "J1.wachtsom", 1, "J2.topsom", 1,
                    "J2.wachtsom", 1), waarden, () -> "waarden in volgorde " + names);
            assertEquals(List.of("[2024-01-01, 2025-01-01)=306", "[2025-01-01, 2026-01-01)=59"),
                    TmsTimelineWitnessTest.timelineOf(j1, "jaardeel"), () -> "jaardeel van J1 in volgorde " + names);
            assertEquals(12, TmsTimelineWitnessTest.timelineOf(j1, "maanddeel").size(), () -> "volgorde " + names);
            assertEquals(0, new BigDecimal("3178").compareTo((BigDecimal) TmsDocuments.read(j1, "wachtsom")),
                    () -> "wachtsom van J1 in volgorde " + names);
            assertEquals(0, new BigDecimal("70").compareTo((BigDecimal) TmsDocuments.read(j1, "topsom")),
                    () -> "topsom van J1 in volgorde " + names);
            assertNull(TmsDocuments.read(j2, "jaardeel"), () -> "een nooit geschreven doel is null, volgorde " + names);
            if (intermediate[0] > 0) intermediateState++;
        }
        assertTrue(intermediateState > 0, "geen volgorde gaf J1 onderweg een andere wachtsom dan 3178");
    }

    /** A Jutter bean as case A's J1 (score 10, with bedrag) or J2 (score 3, without), with the timelines on it. */
    static Object jutter(ClassLoader beanLoader, String score, boolean amount) throws Exception {
        Object j = TmsDocuments.beanObject(beanLoader, "Jutter", Map.of("score", score), List.of());
        TmsTimelineDocuments.set(j, "jaartarief", List.of(
                TmsTimelineDocuments.beanSegment(beanLoader, "2024-01-01", "2025-01-01", "366"),
                TmsTimelineDocuments.beanSegment(beanLoader, "2025-01-01", "2026-01-01", "365")));
        TmsTimelineDocuments.set(j, "borgplichtig", List.of(
                TmsTimelineDocuments.beanSegment(beanLoader, "2024-03-01", "2025-03-01", null)));
        if (amount) TmsTimelineDocuments.set(j, "bedrag", List.of(
                TmsTimelineDocuments.beanSegment(beanLoader, "2024-01-01", "2025-07-01", "7")));
        return j;
    }

    /** The timeline on member {@code member} of {@code beanObject} as "[van, tot)=waarde", or null where the bean holds none. */
    static List<String> timelineOf(Object beanObject, String member) throws Exception {
        Object l = TmsDocuments.read(beanObject, member);
        if (l == null) return null;
        List<String> out = new ArrayList<>();
        for (Period t : Period.list((List<?>) l))
            out.add("[" + t.van() + ", " + t.to() + ")=" + (t.value() == null ? "leeg"
                    : t.value().stripTrailingZeros().toPlainString()));
        return out;
    }

    // ---------------- B: a negated read of what a rule derives later ----------------

    static final String MODEL_B = TmsDocuments.model(List.of(objectType("Jutter", List.of(
            TmsDocuments.attribute("score", WHOLE), timeAttribute("jaartarief", WHOLE, "year"),
            timeAttribute("jaardeel", WHOLE, "year")),
            List.of(timeCharacteristic("borgplichtig", "month"), characteristic("vrijgesteld")))), List.of());

    static final String VRIJSTELLING = TmsDocuments.characteristicRule("vrijstelling", "Jutter", "vrijgesteld",
            comparison("less", ownRead("score", "Jutter"), number("5")));
    static final String YEAR_PART_NON_FREE = computingRule("jaardeel", "BEREKENING", "Jutter", "jaardeel",
            timeProportional("jaartarief", "Jutter", BORGPLICHTIG, "year"), check("vrijgesteld", true));

    static JsonNode caseB() throws Exception {
        return TmsTimelineDocuments.caseDocument(null, List.of(
                        instance("J1", "Jutter", Map.of("score", numberValue("3"), "jaartarief", RATE, "jaardeel",
                                timeline(List.of(segment("2024-01-01", "2025-01-01", "4")))), List.of(), BORG, Map.of()),
                        instance("J2", "Jutter", Map.of("score", numberValue("10"), "jaartarief", RATE), List.of(), BORG,
                                Map.of())),
                List.of(expectedTimeline("J1", "jaardeel", List.of(segment("2024-01-01", "2025-01-01", "4"))),
                        expectedTimeline("J2", "jaardeel", YEAR_PARTS), consistent()));
    }

    @Test void a_negated_read_of_what_is_later_derived_in_both_orders() throws Exception {
        JsonNode caseDocument = caseB();
        List<String> outcomes = new ArrayList<>(), expected = new ArrayList<>();
        for (List<String> order : TmsDocuments.permutations(List.of(VRIJSTELLING, YEAR_PART_NON_FREE))) {
            List<String> names = TmsDocuments.ruleNames(order);
            Path ir = TmsTimelineDocuments.document(MODEL_B, order, List.of());
            outcomes.add(names + " " + TmsFaultWitnessTest.play(ir, caseDocument, DroolsBackend.Branch.EXEC_MODEL));
            expected.add(names + " gespeeld");
        }
        assertEquals(expected, outcomes);
    }

    // ---------------- B2: a comparison with a value a rule replaces later ----------------

    static final String MODEL_B2 = TmsDocuments.model(List.of(objectType("Jutter", List.of(
            TmsDocuments.attribute("score", WHOLE), TmsDocuments.attribute("teller", WHOLE),
            timeAttribute("jaartarief", WHOLE, "year"), timeAttribute("jaardeel", WHOLE, "year")),
            List.of(timeCharacteristic("borgplichtig", "month")))), List.of());

    static final String COUNTER = computingRule("teller", "BEREKENING", "Jutter", "teller", ownRead("score", "Jutter"), "null");
    static final String YEAR_PART_UNDER_FIVE = computingRule("jaardeel", "BEREKENING", "Jutter", "jaardeel",
            timeProportional("jaartarief", "Jutter", BORGPLICHTIG, "year"),
            comparison("less", ownRead("teller", "Jutter"), number("5")));

    static JsonNode caseB2() throws Exception {
        return TmsTimelineDocuments.caseDocument(null, List.of(
                        instance("J1", "Jutter", Map.of("score", numberValue("10"), "teller", numberValue("3"),
                                "jaartarief", RATE, "jaardeel", timeline(List.of(segment("2024-01-01", "2025-01-01", "4")))),
                                List.of(), BORG, Map.of()),
                        instance("J2", "Jutter", Map.of("score", numberValue("2"), "teller", numberValue("8"),
                                "jaartarief", RATE), List.of(), BORG, Map.of())),
                List.of(value("J1", "teller", "10"),
                        expectedTimeline("J1", "jaardeel", List.of(segment("2024-01-01", "2025-01-01", "4"))),
                        value("J2", "teller", "2"), expectedTimeline("J2", "jaardeel", YEAR_PARTS), consistent()));
    }

    @Test void a_comparison_with_a_value_that_a_rule_later_replaces_in_both_orders() throws Exception {
        JsonNode caseDocument = caseB2();
        List<String> outcomes = new ArrayList<>(), expected = new ArrayList<>();
        for (List<String> order : TmsDocuments.permutations(List.of(COUNTER, YEAR_PART_UNDER_FIVE))) {
            List<String> names = TmsDocuments.ruleNames(order);
            Path ir = TmsTimelineDocuments.document(MODEL_B2, order, List.of());
            outcomes.add(names + " " + TmsFaultWitnessTest.play(ir, caseDocument, DroolsBackend.Branch.EXEC_MODEL));
            expected.add(names + " gespeeld");
        }
        assertEquals(expected, outcomes);
    }

    // ---------------- C: an empty timeline written outside the standing domain ----------------

    static final String MODEL_C = TmsDocuments.model(List.of(objectType("Jutter", List.of(
            timeAttribute("jaartarief", WHOLE, "year"), timeAttribute("jaardeel", WHOLE, "year")),
            List.of(timeCharacteristic("borgplichtig", "month")))), List.of());

    static final String YEAR_PART_DATED = IrPath.regel("jaardeel vanaf 2020", "BEREKENING",
            "{\"node\":\"AttributeReference\",\"attribute\":\"jaardeel\",\"subjectType\":\"Jutter\",\"source\":{\"snippet\":"
                    + "\"Het jaardeel van een Jutter\"}}", timeProportional("jaartarief", "Jutter", BORGPLICHTIG, "year"),
            "null", TmsDocuments.validity("2020-01-01", null));

    static JsonNode caseC() throws Exception {
        String rate = timeline(List.of(segment("2024-01-01", "2025-01-01", "366")));
        String four = timeline(List.of(segment("2024-01-01", "2025-01-01", "4")));
        return TmsTimelineDocuments.caseDocument("2025-06-01", List.of(
                        instance("J1", "Jutter", Map.of("jaartarief", rate, "jaardeel", four), List.of(), Map.of(), Map.of()),
                        instance("J2", "Jutter", Map.of("jaartarief", rate, "jaardeel", four), List.of(),
                                Map.of("borgplichtig", List.of(period("2024-03-01", "2024-04-01"))), Map.of())),
                List.of(expectedTimeline("J1", "jaardeel", List.of()),
                        expectedTimeline("J2", "jaardeel", List.of(segment("2024-01-01", "2025-01-01", "31"))),
                        consistent()));
    }

    @Test void outside_the_standing_domain_an_empty_calculation_writes_the_empty_timeline() throws Exception {
        Path ir = TmsTimelineDocuments.document(MODEL_C, List.of(YEAR_PART_DATED), List.of());
        assertEquals("gespeeld", TmsFaultWitnessTest.play(ir, caseC(), DroolsBackend.Branch.EXEC_MODEL));
        DroolsBackend.Build b = DroolsBackend.build(ir, DroolsBackend.Branch.EXEC_MODEL);
        Object j1 = TmsDocuments.beanObject(b.beanLoader(), "Jutter", Map.of(), List.of());
        TmsTimelineDocuments.set(j1, "jaartarief", List.of(
                TmsTimelineDocuments.beanSegment(b.beanLoader(), "2024-01-01", "2025-01-01", "366")));
        TmsTimelineDocuments.set(j1, "jaardeel", List.of(
                TmsTimelineDocuments.beanSegment(b.beanLoader(), "2024-01-01", "2025-01-01", "4")));
        TmsDocuments.fire(b, b.execModelTranspiler().model().getRules(), List.of(j1),
                session -> session.setGlobal("rekendatum", java.time.LocalDate.parse("2025-06-01")), session -> { });
        assertEquals(List.of(), timelineOf(j1, "jaardeel"), "een lege lijst op de bean, niet null en niet de invoer");
    }

    // ---------------- D: the clip over a characteristic on a timeline, ch. 3.6 ----------------

    static final String MODEL_D = TmsDocuments.model(List.of(objectType("Jutter", List.of(
            timeAttribute("vergoeding", WHOLE, "month"), TmsDocuments.attribute("wachtsom", WHOLE),
            TmsDocuments.attribute("stilsom", WHOLE), TmsDocuments.attribute("topsom", WHOLE)),
            List.of(timeCharacteristic("wachtplichtig", "month")))), List.of());

    static final String GUARD = computingRule("wachtsom", "BEREKENING", "Jutter", "wachtsom",
            ownTotal("vergoeding", "Jutter", check("wachtplichtig", false)), "null");
    static final String SILENT = computingRule("stilsom", "BEREKENING", "Jutter", "stilsom",
            ownTotal("vergoeding", "Jutter", check("wachtplichtig", true)), "null");
    static final String TOP = computingRule("topsom", "BEREKENING", "Jutter", "topsom",
            ownTotal("vergoeding", "Jutter", comparison("greater", ownRead("vergoeding", "Jutter"), number("10"))),
            "null");

    static JsonNode caseD() throws Exception {
        String vergoeding = timeline(List.of(segment("2023-01-01", "2024-01-01", "10"),
                segment("2024-01-01", "2024-07-01", "11")));
        return TmsTimelineDocuments.caseDocument(null, List.of(
                        instance("J1", "Jutter", Map.of("vergoeding", vergoeding), List.of(),
                                Map.of("wachtplichtig", List.of(period("2023-07-01", "2024-02-01"))), Map.of()),
                        instance("J2", "Jutter", Map.of("vergoeding", vergoeding), List.of(), Map.of(), Map.of()),
                        instance("J3", "Jutter", Map.of(), List.of(),
                                Map.of("wachtplichtig", List.of(period("2023-07-01", "2024-02-01"))), Map.of())),
                List.of(value("J1", "wachtsom", "71"), value("J1", "stilsom", "115"), value("J1", "topsom", "66"),
                        value("J2", "wachtsom", "0"), value("J2", "stilsom", "186"), value("J2", "topsom", "66"),
                        value("J3", "wachtsom", "0"), value("J3", "stilsom", "0"), value("J3", "topsom", "0"),
                        consistent()));
    }

    @Test void a_clip_over_a_time_dependent_characteristic_reads_its_periods_in_every_order() throws Exception {
        JsonNode caseDocument = caseD();
        List<String> outcomes = new ArrayList<>(), expected = new ArrayList<>();
        for (List<String> order : TmsDocuments.permutations(List.of(GUARD, SILENT, TOP))) {
            List<String> names = TmsDocuments.ruleNames(order);
            Path ir = TmsTimelineDocuments.document(MODEL_D, order, List.of());
            outcomes.add(names + " " + TmsFaultWitnessTest.play(ir, caseDocument, DroolsBackend.Branch.EXEC_MODEL));
            expected.add(names + " gespeeld");
        }
        assertEquals(expected, outcomes);
    }

    // ---------------- E: a day count onto a target on a fixed start point, ch. 3.2 ----------------

    static final String MODEL_E = TmsDocuments.model(List.of(objectType("Jutter", List.of(
            timeAttribute("stormdagen", WHOLE, "year", "--10-01"), timeAttribute("kalenderdagen", WHOLE, "year")),
            List.of(timeCharacteristic("wachtplichtig", "month")))), List.of());

    static final String STORM_DAYS = computingRule("stormdagen", "BEREKENING", "Jutter", "stormdagen",
            dayCount(check("wachtplichtig", false), "year"), "null");
    static final String CALENDAR_DAYS = computingRule("kalenderdagen", "BEREKENING", "Jutter", "kalenderdagen",
            dayCount(check("wachtplichtig", false), "year"), "null");

    static JsonNode caseE() throws Exception {
        return TmsTimelineDocuments.caseDocument(null, List.of(instance("J1", "Jutter", Map.of(), List.of(),
                        Map.of("wachtplichtig", List.of(period("2024-09-28", "2024-10-03"))), Map.of())),
                List.of(expectedTimeline("J1", "stormdagen", List.of(segment("2023-10-01", "2024-10-01", "3"),
                                segment("2024-10-01", "2025-10-01", "2"))),
                        expectedTimeline("J1", "kalenderdagen", List.of(segment("2024-01-01", "2025-01-01", "5"))),
                        consistent()));
    }

    @Test void a_day_count_to_a_target_on_a_fixed_start_point_in_both_orders() throws Exception {
        JsonNode caseDocument = caseE();
        List<String> outcomes = new ArrayList<>(), expected = new ArrayList<>();
        for (List<String> order : TmsDocuments.permutations(List.of(STORM_DAYS, CALENDAR_DAYS))) {
            List<String> names = TmsDocuments.ruleNames(order);
            Path ir = TmsTimelineDocuments.document(MODEL_E, order, List.of());
            outcomes.add(names + " " + TmsFaultWitnessTest.play(ir, caseDocument, DroolsBackend.Branch.EXEC_MODEL));
            expected.add(names + " gespeeld");
        }
        assertEquals(expected, outcomes);
    }

    // ---------------- the documents, for the other executors ----------------

    /** Writes every document of this class, in every order of its rules, with its case, under {@code args[0]}: a map
     *  per part and order, holding {@code ir.json} and {@code case.json}, for the verifier, ref and clingo. */
    public static void main(String[] args) throws Exception {
        Path out = Path.of(args[0]);
        Map<String, Object[]> parts = new LinkedHashMap<>();
        parts.put("a-keten", new Object[]{MODEL_A, RULES_A, caseA()});
        parts.put("b-ontkend", new Object[]{MODEL_B, List.of(VRIJSTELLING, YEAR_PART_NON_FREE), caseB()});
        parts.put("b2-vervangen", new Object[]{MODEL_B2, List.of(COUNTER, YEAR_PART_UNDER_FIVE), caseB2()});
        parts.put("c-gedateerd", new Object[]{MODEL_C, List.of(YEAR_PART_DATED), caseC()});
        parts.put("d-knip", new Object[]{MODEL_D, List.of(GUARD, SILENT, TOP), caseD()});
        parts.put("e-vast-startpunt", new Object[]{MODEL_E, List.of(STORM_DAYS, CALENDAR_DAYS), caseE()});
        for (Map.Entry<String, Object[]> d : parts.entrySet()) {
            @SuppressWarnings("unchecked") List<String> rules = (List<String>) d.getValue()[1];
            int n = 0;
            for (List<String> order : TmsDocuments.permutations(rules))
                write(out.resolve(d.getKey()).resolve(String.format("v%03d", n++)),
                        TmsTimelineDocuments.ir((String) d.getValue()[0], order, List.of()),
                        (JsonNode) d.getValue()[2]);
        }
    }

    static void write(Path map, String ir, JsonNode caseDocument) throws Exception {
        Files.createDirectories(map);
        Files.writeString(map.resolve("ir.json"), TmsDocuments.JSON.writerWithDefaultPrettyPrinter()
                .writeValueAsString(TmsDocuments.JSON.readTree(ir)));
        Files.writeString(map.resolve("case.json"), TmsDocuments.JSON.writerWithDefaultPrettyPrinter()
                .writeValueAsString(caseDocument));
    }
}
