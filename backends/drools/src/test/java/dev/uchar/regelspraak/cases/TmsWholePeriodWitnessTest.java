package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;

import dev.uchar.regelspraak.transpiler.tms.Value;

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

import static dev.uchar.regelspraak.cases.TmsDocuments.check;
import static dev.uchar.regelspraak.cases.TmsDocuments.consistent;
import static dev.uchar.regelspraak.cases.TmsDocuments.number;
import static dev.uchar.regelspraak.cases.TmsDocuments.numberValue;
import static dev.uchar.regelspraak.cases.TmsDocuments.characteristicRule;
import static dev.uchar.regelspraak.cases.TmsDocuments.reading;
import static dev.uchar.regelspraak.cases.TmsDocuments.computingRule;
import static dev.uchar.regelspraak.cases.TmsDocuments.verwacht;
import static dev.uchar.regelspraak.cases.TmsDocuments.waarde;
import static dev.uchar.regelspraak.cases.TmsTimelineDocuments.WHOLE;
import static dev.uchar.regelspraak.cases.TmsTimelineDocuments.dayCount;
import static dev.uchar.regelspraak.cases.TmsTimelineDocuments.instance;
import static dev.uchar.regelspraak.cases.TmsTimelineDocuments.kenmerk;
import static dev.uchar.regelspraak.cases.TmsTimelineDocuments.months;
import static dev.uchar.regelspraak.cases.TmsTimelineDocuments.objecttype;
import static dev.uchar.regelspraak.cases.TmsTimelineDocuments.period;
import static dev.uchar.regelspraak.cases.TmsTimelineDocuments.segment;
import static dev.uchar.regelspraak.cases.TmsTimelineDocuments.timeAttribute;
import static dev.uchar.regelspraak.cases.TmsTimelineDocuments.timeCharacteristic;
import static dev.uchar.regelspraak.cases.TmsTimelineDocuments.timeline;
import static dev.uchar.regelspraak.cases.TmsTimelineDocuments.expectedTimeline;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The whole-period qualifier on the exec-model branch (RUNTIME-SEMANTICS ch. 3.9, 3.9a): "gedurende het gehele
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
 * {@code TmsInputFormWitnessTest}. R: the reference interpreter and the ASP runner both carry the inner negation of
 * the check, and both refuse the other two forms ("whole-period qualifier on a comparison whose operands are not a
 * time-dependent attribute of "Schuit" and a scalar bound", "… between TWO time-dependent operands has no ch. 3.9
 * reading …").
 *
 * <p><b>Red</b> on 0e879163, the code before the change: every test, each on the refusal of the qualifier, "Excluded:
 * tms-transpiler SKIP: regel: trouwpremie — een Comparison met wholePeriod ('zijn wachtdagen gedurende het gehele jaar
 * groter is dan zijn drempel'): tijdlijnen zijn buiten de proef van de truth-maintenance-tak", and for B "… een
 * Predicate met wholePeriod …" and for R the same sentence in place of the named refusal.
 */
class TmsWholePeriodWitnessTest {

    /** The whole-period qualifier: "het gehele jaar" or "de gehele maand", with or without its own negation. */
    static String whole(boolean year, boolean not) {
        return "{\"node\":\"WholePeriod\",\"granularity\":\"" + (year ? "year" : "month") + "\",\"negated\":" + not + "}";
    }

    /** A comparison in its wire spelling under the qualifier {@code periode} ({@link #whole}). */
    static String wholeComparison(String op, String left, String right, String period, String bron) {
        return "{\"node\":\"Comparison\",\"op\":\"" + op + "\",\"left\":" + left + ",\"right\":" + right
                + ",\"wholePeriod\":" + period + ",\"source\":{\"snippet\":\"" + bron + "\"}}";
    }

    /** A leeg ({@code empty}) or gevuld ({@code filled}) test under the qualifier {@code periode}. */
    static String wholePredicate(String kind, String operand, String period, String bron) {
        return "{\"node\":\"Predicate\",\"kind\":\"" + kind + "\",\"operand\":" + operand + ",\"negated\":false,"
                + "\"digits\":0,\"wholePeriod\":" + period + ",\"source\":{\"snippet\":\"" + bron + "\"}}";
    }

    /** A check of {@code kenmerk} of the object one role away, {@code rol} of {@code feittype}, under the qualifier. */
    static String wholeCheck(String kenmerk, String feittype, String rol, String period, String bron) {
        return "{\"node\":\"CharacteristicCheck\",\"characteristic\":\"" + kenmerk + "\",\"negated\":false,"
                + "\"wholePeriod\":" + period + ",\"source\":{\"snippet\":\"" + bron + "\"},\"navigation\":{\"node\":"
                + "\"RoleBinding\",\"factType\":\"" + feittype + "\",\"role\":\"" + rol + "\"}}";
    }

    /** "{@code doel} … moet gesteld worden op {@code lexeme} indien {@code voorwaarde}". */
    static String premie(String naam, String type, String doel, String lexeme, String voorwaarde) {
        return computingRule(naam, "GELIJKSTELLING", type, doel, number(lexeme), voorwaarde);
    }

    /** The case of {@code instanties} with the evaluation date 2024-06-15 and in a session without one. */
    static List<JsonNode> cases(List<String> instanties, List<String> verwachtingen) throws Exception {
        return List.of(TmsTimelineDocuments.caseDocument("2024-06-15", instanties, verwachtingen),
                TmsTimelineDocuments.caseDocument(null, instanties, verwachtingen));
    }

    /** Every order of {@code rules} in the document of {@code model}, each replayed on every case of {@code cases}
     *  through the exec-model branch: "[rule names] case i gespeeld" where it plays. */
    static List<String> inEveryOrder(String model, List<String> rules, List<JsonNode> cases) throws Exception {
        List<String> out = new ArrayList<>();
        for (List<String> order : TmsDocuments.permutations(rules)) {
            List<String> names = TmsDocuments.ruleNames(order);
            Path ir = TmsTimelineDocuments.document(model, order, List.of());
            for (int i = 0; i < cases.size(); i++)
                out.add(names + " zaak " + i + " " + TmsFaultWitnessTest.play(ir, cases.get(i),
                        DroolsBackend.Branch.EXEC_MODEL));
        }
        return out;
    }

    /** What {@link #inEveryOrder} gives where every order plays every case. */
    static List<String> played(List<String> rules, int cases) throws Exception {
        List<String> out = new ArrayList<>();
        for (List<String> order : TmsDocuments.permutations(rules))
            for (int i = 0; i < cases; i++) out.add(TmsDocuments.ruleNames(order) + " zaak " + i + " gespeeld");
        return out;
    }

    // ---------------- A: the subject's own timeline ----------------

    static final String MODEL_A = TmsDocuments.model(List.of(objecttype("Jutter", List.of(
            TmsDocuments.attribuut("drempel", WHOLE), timeAttribute("wachtdagen", WHOLE, "month"),
            TmsDocuments.attribuut("trouwpremie", WHOLE), TmsDocuments.attribuut("vulpremie", WHOLE),
            TmsDocuments.attribuut("stilpremie", WHOLE)),
            List.of(timeCharacteristic("wachtplichtig", "month")))), List.of());

    static final String WACHTDAGEN = computingRule("wachtdagen", "BEREKENING", "Jutter", "wachtdagen",
            dayCount(check("wachtplichtig", false), "month"), "null");
    static final String TROUWPREMIE = premie("trouwpremie", "Jutter", "trouwpremie", "10",
            wholeComparison("greater", reading("wachtdagen", "Jutter"), reading("drempel", "Jutter"), whole(true, false),
                    "zijn wachtdagen gedurende het gehele jaar groter is dan zijn drempel"));
    static final String VULPREMIE = premie("vulpremie", "Jutter", "vulpremie", "20",
            wholePredicate("filled", reading("wachtdagen", "Jutter"), whole(false, false),
                    "zijn wachtdagen gedurende de gehele maand gevuld is"));
    static final String SILENT_PREMIE = premie("stilpremie", "Jutter", "stilpremie", "30",
            wholeComparison("greater", reading("wachtdagen", "Jutter"), reading("drempel", "Jutter"), whole(true, true),
                    "zijn wachtdagen niet gedurende het gehele jaar groter is dan zijn drempel"));

    static final List<String> RULES_A = List.of(WACHTDAGEN, TROUWPREMIE, VULPREMIE, SILENT_PREMIE);

    static final Map<String, List<String>> WHOLE_2024 = Map.of("wachtplichtig",
            List.of(period("2024-01-01", "2025-01-01")));

    static List<String> instancesA() {
        String threshold = numberValue("20");
        return List.of(
                instance("J1", "Jutter", Map.of("drempel", threshold, "wachtdagen",
                        timeline(List.of(segment("2024-01-01", "2024-02-01", "5")))), List.of(), WHOLE_2024, Map.of()),
                instance("J2", "Jutter", Map.of("drempel", threshold), List.of(),
                        Map.of("wachtplichtig", List.of(period("2024-01-15", "2024-07-01"))), Map.of()),
                instance("J3", "Jutter", Map.of("drempel", threshold, "wachtdagen", timeline(months(2024, 1, 12, "25"))),
                        List.of(), Map.of(), Map.of()),
                instance("J4", "Jutter", Map.of("drempel", threshold), List.of(), Map.of(), Map.of()),
                instance("J5", "Jutter", Map.of(), List.of(), WHOLE_2024, Map.of()),
                instance("J6", "Jutter", Map.of("drempel", threshold), List.of(),
                        Map.of("wachtplichtig", List.of(period("2024-02-01", "2024-07-01"))), Map.of()));
    }

    /** The day counts of 2024, January to December, as segments. */
    static List<String> days2024() {
        String[] n = {"31", "29", "31", "30", "31", "30", "31", "31", "30", "31", "30", "31"};
        List<String> out = new ArrayList<>();
        for (int m = 1; m <= 12; m++)
            out.add(segment(String.format("2024-%02d-01", m), m == 12 ? "2025-01-01" : String.format("2024-%02d-01", m + 1),
                    n[m - 1]));
        return out;
    }

    static List<String> expectationsA() {
        List<String> out = new ArrayList<>();
        out.add(expectedTimeline("J1", "wachtdagen", days2024()));
        out.add(expectedTimeline("J2", "wachtdagen", List.of(segment("2024-01-01", "2024-02-01", "17"),
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
            out.add(waarde(j, "trouwpremie", p[0]));
            out.add(waarde(j, "vulpremie", p[1]));
            out.add(waarde(j, "stilpremie", p[2]));
        });
        out.add(consistent());
        return out;
    }

    @Test void the_own_timeline_later_derived_in_every_order_with_and_without_evaluation_date() throws Exception {
        List<JsonNode> cases = cases(instancesA(), expectationsA());
        assertEquals(played(RULES_A, cases.size()), inEveryOrder(MODEL_A, RULES_A, cases));
    }

    @Test void a_premie_of_an_intermediate_state_is_away_at_the_fixpoint() throws Exception {
        Path ir = TmsTimelineDocuments.document(MODEL_A, RULES_A, List.of());
        DroolsBackend.Build b = DroolsBackend.build(ir, DroolsBackend.Branch.EXEC_MODEL);
        int intermediate = 0;
        for (List<Rule> order : TmsDocuments.sample(b.execModelTranspiler().model().getRules(), 20261009L, 100)) {
            List<String> names = order.stream().map(Rule::getName).toList();
            Object j2 = TmsDocuments.beanObject(b.beanLoader(), "Jutter", Map.of("drempel", "20"), List.of());
            TmsTimelineDocuments.set(j2, "wachtplichtig", List.of(
                    TmsTimelineDocuments.beanSegment(b.beanLoader(), "2024-01-15", "2024-07-01", null)));
            int[] ten = {0};
            TmsDocuments.vuur(b, order, List.of(j2), sessie -> sessie.addEventListener(
                    new DefaultRuleRuntimeEventListener() {
                        @Override public void objectInserted(ObjectInsertedEvent e) {
                            if (e.getObject() instanceof Value w && w.onderwerp() == j2
                                    && w.attribuut().naam().equals("trouwpremie")) ten[0]++;
                        }
                    }), sessie -> { });
            assertNull(TmsDocuments.lees(j2, "trouwpremie"), () -> "trouwpremie van J2 in volgorde " + names);
            assertEquals(0, new BigDecimal("30").compareTo((BigDecimal) TmsDocuments.lees(j2, "stilpremie")),
                    () -> "stilpremie van J2 in volgorde " + names);
            if (ten[0] > 0) intermediate++;
        }
        assertTrue(intermediate > 0, "geen volgorde gaf J2 onderweg de trouwpremie van zijn invoer");
    }

    // ---------------- A3: the equalities against a leeg bound ----------------

    static final String MODEL_A3 = TmsDocuments.model(List.of(objecttype("Jutter", List.of(
            TmsDocuments.attribuut("drempel", WHOLE), timeAttribute("wachtdagen", WHOLE, "month"),
            TmsDocuments.attribuut("ongelijkpremie", WHOLE), TmsDocuments.attribuut("gelijkpremie", WHOLE)),
            List.of(timeCharacteristic("wachtplichtig", "month")))), List.of());

    static final String NOT_EQUAL_PREMIE = premie("ongelijkpremie", "Jutter", "ongelijkpremie", "10",
            wholeComparison("notEqual", reading("wachtdagen", "Jutter"), reading("drempel", "Jutter"), whole(true, false),
                    "zijn wachtdagen gedurende het gehele jaar ongelijk is aan zijn drempel"));
    static final String EQUAL_PREMIE = premie("gelijkpremie", "Jutter", "gelijkpremie", "20",
            wholeComparison("equal", reading("wachtdagen", "Jutter"), reading("drempel", "Jutter"), whole(true, false),
                    "zijn wachtdagen gedurende het gehele jaar gelijk is aan zijn drempel"));

    static final List<String> RULES_A3 = List.of(WACHTDAGEN, NOT_EQUAL_PREMIE, EQUAL_PREMIE);

    static List<String> instancesA3() {
        return List.of(instance("J1", "Jutter", Map.of("drempel", numberValue("20")), List.of(), WHOLE_2024, Map.of()),
                instance("J5", "Jutter", Map.of(), List.of(), WHOLE_2024, Map.of()));
    }

    static List<String> expectationsA3() {
        return List.of(waarde("J1", "ongelijkpremie", "10"), waarde("J1", "gelijkpremie", null),
                waarde("J5", "ongelijkpremie", null), waarde("J5", "gelijkpremie", null), consistent());
    }

    @Test void an_empty_bound_fails_every_day_also_for_not_equal_to() throws Exception {
        List<JsonNode> cases = cases(instancesA3(), expectationsA3());
        assertEquals(played(RULES_A3, cases.size()), inEveryOrder(MODEL_A3, RULES_A3, cases));
    }

    // ---------------- B: a step over a to-one role ----------------

    static final String MODEL_B = TmsDocuments.model(List.of(
            objecttype("Werf", List.of(timeAttribute("havendagen", WHOLE, "month")),
                    List.of(timeCharacteristic("open", "month"), timeCharacteristic("bedrijvig", "month"))),
            objecttype("Schuit", List.of(TmsDocuments.attribuut("werftoeslag", WHOLE),
                    TmsDocuments.attribuut("kadegeld", WHOLE), TmsDocuments.attribuut("stilgeld", WHOLE)), List.of())),
            List.of(TmsDocuments.factTypeOneToMany("ligplaats", "werf", "Werf", "schuit", "Schuit", "schuiten")));

    static final String HAVENDAGEN = computingRule("havendagen", "BEREKENING", "Werf", "havendagen",
            dayCount(check("open", false), "month"), "null");
    static final String WERF_HAVENDAGEN = TmsDocuments.pathRead("havendagen", "Werf", "ligplaats", "werf");
    static final String WERFTOESLAG = premie("werftoeslag", "Schuit", "werftoeslag", "10",
            wholePredicate("filled", WERF_HAVENDAGEN, whole(true, false),
                    "de havendagen van de werf van de Schuit gedurende het gehele jaar gevuld is"));
    static final String KADEGELD = premie("kadegeld", "Schuit", "kadegeld", "5",
            wholeCheck("bedrijvig", "ligplaats", "werf", whole(true, false),
                    "de werf van de Schuit gedurende het gehele jaar bedrijvig is"));
    static final String SILENT_MONEY = premie("stilgeld", "Schuit", "stilgeld", "3",
            wholePredicate("filled", WERF_HAVENDAGEN, whole(true, true),
                    "de havendagen van de werf van de Schuit niet gedurende het gehele jaar gevuld is"));

    static final List<String> RULES_B = List.of(HAVENDAGEN, WERFTOESLAG, KADEGELD, SILENT_MONEY);

    static List<String> instancesB() {
        Map<String, List<String>> whole = Map.of("open", List.of(period("2024-01-01", "2025-01-01")),
                "bedrijvig", List.of(period("2024-01-01", "2025-01-01")));
        Map<String, List<String>> half = Map.of("open", List.of(period("2024-01-01", "2024-07-01")),
                "bedrijvig", List.of(period("2024-01-01", "2024-07-01")));
        return List.of(
                instance("S1", "Schuit", Map.of(), List.of(), Map.of(), Map.of()),
                instance("S2", "Schuit", Map.of(), List.of(), Map.of(), Map.of("werf", List.of("W1"))),
                instance("S3", "Schuit", Map.of(), List.of(), Map.of(), Map.of("werf", List.of("W2"))),
                instance("S4", "Schuit", Map.of(), List.of(), Map.of(), Map.of("werf", List.of("W3"))),
                instance("W1", "Werf", Map.of(), List.of(), whole, Map.of()),
                instance("W2", "Werf", Map.of(), List.of(), half, Map.of()),
                instance("W3", "Werf", Map.of("havendagen", timeline(months(2024, 1, 12, "3"))), List.of(), Map.of(),
                        Map.of()));
    }

    static List<String> expectationsB() {
        List<String> out = new ArrayList<>();
        Map<String, String[]> apply = new LinkedHashMap<>();
        apply.put("S1", new String[]{"10", "5", null});
        apply.put("S2", new String[]{"10", "5", null});
        apply.put("S3", new String[]{null, null, "3"});
        apply.put("S4", new String[]{"10", "5", null});
        apply.forEach((s, g) -> {
            out.add(waarde(s, "werftoeslag", g[0]));
            out.add(waarde(s, "kadegeld", g[1]));
            out.add(waarde(s, "stilgeld", g[2]));
        });
        out.add(consistent());
        return out;
    }

    @Test void over_a_step_without_link_and_with_the_timeline_of_the_step_later_derived() throws Exception {
        List<JsonNode> cases = cases(instancesB(), expectationsB());
        assertEquals(played(RULES_B, cases.size()), inEveryOrder(MODEL_B, RULES_B, cases));
    }

    // ---------------- C: the counting quantifiers with the qualifier's negation ----------------

    static final String MODEL_C = TmsDocuments.model(List.of(
            objecttype("Vloot", List.of(), List.of(kenmerk("inzetbaar"), kenmerk("krap"), kenmerk("paraat"))),
            objecttype("Boot", List.of(timeAttribute("vaardagen", WHOLE, "month")),
                    List.of(timeCharacteristic("uitgevaren", "month")))),
            List.of(TmsDocuments.factTypeOneToMany("indeling", "vloot", "Vloot", "boot", "Boot", "boten")));

    static final String SAILING_DAYS = computingRule("vaardagen", "BEREKENING", "Boot", "vaardagen",
            dayCount(check("uitgevaren", false), "month"), "null");

    /** "Een Vloot is {@code doel} indien de vaardagen van {@code description} van de boten van de Vloot niet gedurende het gehele
     *  jaar groter is dan 20". */
    static String count(String naam, String doel, String quantifier, int threshold, String description) {
        String bron = "de vaardagen van " + description + " van de boten van de Vloot niet gedurende het gehele jaar groter is"
                + " dan 20";
        String test = wholeComparison("greater", reading("vaardagen", "Boot"), number("20"), whole(true, true), bron);
        return characteristicRule(naam, "Vloot", doel, "{\"node\":\"QuantifierFold\",\"quantifier\":\"" + quantifier + "\","
                + "\"threshold\":" + threshold + ",\"elementType\":\"Boot\",\"test\":" + test + ","
                + TmsDocuments.sourceRole("indeling", "boot") + ",\"source\":{\"snippet\":\"" + bron + "\"}}");
    }

    static final List<String> RULES_C = List.of(SAILING_DAYS, count("inzet", "inzetbaar", "atLeast", 2, "ten minste 2"),
            count("krapte", "krap", "atMost", 1, "ten hoogste 1"), count("paraatheid", "paraat", "exactly", 1,
                    "precies 1"));

    static List<String> instancesC() {
        Map<String, List<String>> whole = Map.of("uitgevaren", List.of(period("2024-01-01", "2025-01-01")));
        Map<String, List<String>> half = Map.of("uitgevaren", List.of(period("2024-01-01", "2024-07-01")));
        return List.of(
                instance("V1", "Vloot", Map.of(), List.of(), Map.of(), Map.of("boten", List.of("B1", "B2", "B3"))),
                instance("V2", "Vloot", Map.of(), List.of(), Map.of(), Map.of("boten", List.of("B4", "B5"))),
                instance("V3", "Vloot", Map.of(), List.of(), Map.of(), Map.of("boten", List.of("B6"))),
                instance("V4", "Vloot", Map.of(), List.of(), Map.of(), Map.of()),
                instance("B1", "Boot", Map.of(), List.of(), whole, Map.of()),
                instance("B2", "Boot", Map.of(), List.of(), half, Map.of()),
                instance("B3", "Boot", Map.of("vaardagen", timeline(months(2024, 1, 12, "15"))), List.of(), Map.of(),
                        Map.of()),
                instance("B4", "Boot", Map.of(), List.of(), whole, Map.of()),
                instance("B5", "Boot", Map.of(), List.of(), Map.of(), Map.of()),
                instance("B6", "Boot", Map.of(), List.of(), half, Map.of()));
    }

    static List<String> expectationsC() {
        List<String> out = new ArrayList<>();
        Map<String, boolean[]> vloten = new LinkedHashMap<>();
        vloten.put("V1", new boolean[]{true, false, false});
        vloten.put("V2", new boolean[]{false, true, false});
        vloten.put("V3", new boolean[]{false, true, true});
        vloten.put("V4", new boolean[]{false, true, false});
        vloten.forEach((v, k) -> {
            out.add(verwacht(v, "inzetbaar", k[0]));
            out.add(verwacht(v, "krap", k[1]));
            out.add(verwacht(v, "paraat", k[2]));
        });
        out.add(consistent());
        return out;
    }

    @Test void the_counting_quantifiers_count_the_negation_per_member_in_every_order() throws Exception {
        List<JsonNode> cases = cases(instancesC(), expectationsC());
        assertEquals(played(RULES_C, cases.size()), inEveryOrder(MODEL_C, RULES_C, cases));
    }

    // ---------------- E: the input given as the asOfDate-bucket scalar ----------------

    static final String MODEL_E = TmsDocuments.model(List.of(objecttype("Jutter", List.of(
            TmsDocuments.attribuut("drempel", WHOLE), timeAttribute("wachtdagen", WHOLE, "month"),
            TmsDocuments.attribuut("trouwpremie", WHOLE)),
            List.of(timeCharacteristic("wachtplichtig", "month"), kenmerk("vrijgesteld")))), List.of());

    static final String VRIJSTELLING = characteristicRule("vrijstelling", "Jutter", "vrijgesteld",
            TmsDocuments.comparison("less", reading("drempel", "Jutter"), number("5")));
    static final String TROUW_E = premie("trouwpremie", "Jutter", "trouwpremie", "10", TmsDocuments.compound("all",
            0, List.of(check("vrijgesteld", true), wholeComparison("greater", reading("wachtdagen", "Jutter"),
                    reading("drempel", "Jutter"), whole(true, false),
                    "zijn wachtdagen gedurende het gehele jaar groter is dan zijn drempel"))));

    static final List<String> RULES_E = List.of(WACHTDAGEN, VRIJSTELLING, TROUW_E);

    static final String BUCKET_ERROR = "Invoervormfout: fout in de regel \"trouwpremie\": the whole-period qualifier over"
            + " \"wachtdagen\" needs the full timeline; \"J1\" supplies only the asOfDate-bucket scalar (ch. 3.9, ch. 8.4)";

    static JsonNode bucketError() throws Exception {
        return TmsTimelineDocuments.caseDocument("2024-06-15", List.of(instance("J1", "Jutter", Map.of("drempel",
                numberValue("20"), "wachtdagen", numberValue("25")), List.of(), Map.of(), Map.of())), List.of(consistent()));
    }

    static JsonNode bucketPlays() throws Exception {
        return TmsTimelineDocuments.caseDocument("2024-06-15", List.of(
                        instance("J2", "Jutter", Map.of("drempel", numberValue("20"), "wachtdagen", numberValue("25")),
                                List.of(), WHOLE_2024, Map.of()),
                        instance("J3", "Jutter", Map.of("drempel", numberValue("3"), "wachtdagen", numberValue("25")),
                                List.of(), Map.of(), Map.of())),
                List.of(expectedTimeline("J2", "wachtdagen", days2024()), waarde("J2", "trouwpremie", "10"),
                        waarde("J3", "trouwpremie", null), consistent()));
    }

    static final String MODEL_E_PATH = MODEL_B;
    static final List<String> RULES_E_PATH = List.of(HAVENDAGEN, WERFTOESLAG);

    static final String BUCKET_ERROR_PATH = "Invoervormfout: fout in de regel \"werftoeslag\": the whole-period qualifier"
            + " over \"havendagen\" needs the full timeline; the werf of \"S1\" supplies only the asOfDate-bucket scalar"
            + " (ch. 3.9, ch. 8.4)";

    static JsonNode bucketOverPath() throws Exception {
        return TmsTimelineDocuments.caseDocument("2024-06-15", List.of(
                instance("S1", "Schuit", Map.of(), List.of(), Map.of(), Map.of("werf", List.of("W1"))),
                instance("W1", "Werf", Map.of("havendagen", numberValue("3")), List.of(), Map.of(), Map.of())),
                List.of(consistent()));
    }

    @Test void over_a_bucket_scalar_the_fold_fails_where_the_evaluation_reaches_it() throws Exception {
        List<String> out = new ArrayList<>(), verwacht = new ArrayList<>();
        for (List<String> order : TmsDocuments.permutations(RULES_E)) {
            List<String> names = TmsDocuments.ruleNames(order);
            Path ir = TmsTimelineDocuments.document(MODEL_E, order, List.of());
            out.add(names + " " + TmsFaultWitnessTest.play(ir, bucketError(), DroolsBackend.Branch.EXEC_MODEL));
            out.add(names + " " + TmsFaultWitnessTest.play(ir, bucketPlays(), DroolsBackend.Branch.EXEC_MODEL));
            verwacht.add(names + " " + BUCKET_ERROR);
            verwacht.add(names + " gespeeld");
        }
        for (List<String> order : TmsDocuments.permutations(RULES_E_PATH)) {
            List<String> names = TmsDocuments.ruleNames(order);
            out.add(names + " " + TmsFaultWitnessTest.play(TmsTimelineDocuments.document(MODEL_E_PATH, order,
                    List.of()), bucketOverPath(), DroolsBackend.Branch.EXEC_MODEL));
            verwacht.add(names + " " + BUCKET_ERROR_PATH);
        }
        assertEquals(verwacht, out);
    }

    // ---------------- R: the forms the branch does not fold ----------------

    /** "Een Jutter is X indien {@code voorwaarde}", in a document of model A extended by a characteristic X and a second
     *  timeline, vaardagen, and of model B. */
    static final String MODEL_R = TmsDocuments.model(List.of(objecttype("Jutter", List.of(
            TmsDocuments.attribuut("drempel", WHOLE), timeAttribute("wachtdagen", WHOLE, "month"),
            timeAttribute("vaardagen", WHOLE, "month")),
            List.of(timeCharacteristic("wachtplichtig", "month"), kenmerk("trouw")))), List.of());

    /** The rule "Een Jutter is trouw indien {@code voorwaarde}". */
    static String trouw(String voorwaarde) {
        return characteristicRule("trouw", "Jutter", "trouw", voorwaarde);
    }

    @Test void the_forms_that_the_branch_does_not_fold_are_refused_by_name() throws Exception {
        String inside = "{\"node\":\"CharacteristicCheck\",\"characteristic\":\"wachtplichtig\",\"negated\":true,"
                + "\"wholePeriod\":" + whole(true, false) + ",\"source\":{\"snippet\":\"hij gedurende het gehele jaar"
                + " niet wachtplichtig is\"}}";
        String overPath = premie("werftoeslag", "Schuit", "werftoeslag", "10", wholeComparison("greater",
                WERF_HAVENDAGEN, number("5"), whole(true, false),
                "de havendagen van de werf van de Schuit gedurende het gehele jaar groter is dan 5"));
        String twoTimelines = wholeComparison("greater", reading("wachtdagen", "Jutter"), reading("vaardagen", "Jutter"),
                whole(true, false), "zijn wachtdagen gedurende het gehele jaar groter is dan zijn vaardagen");
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
                List.of(refusal(MODEL_R, trouw(inside)), refusal(MODEL_B, overPath),
                        refusal(MODEL_R, trouw(twoTimelines))));
    }

    /** The refusal of the document of {@code model} with the one rule {@code regel}, as the build raises it. */
    static String refusal(String model, String regel) throws Exception {
        Path ir = TmsTimelineDocuments.document(model, List.of(regel), List.of());
        return org.junit.jupiter.api.Assertions.assertThrows(CaseRunner.Excluded.class,
                () -> DroolsBackend.build(ir, DroolsBackend.Branch.EXEC_MODEL)).getMessage();
    }

    // ---------------- the documents, for the other executors ----------------

    /** Writes every document of this class, in every order of its rules, with its cases, under {@code args[0]}: a map
     *  per part and order, holding {@code ir.json}, {@code case1.json} (with an evaluation date) and, where the part has
     *  one, {@code case2.json} (without), for the verifier, ref and clingo. */
    public static void main(String[] args) throws Exception {
        Path out = Path.of(args[0]);
        Map<String, Object[]> parts = new LinkedHashMap<>();
        parts.put("a-eigen", new Object[]{MODEL_A, RULES_A, cases(instancesA(), expectationsA())});
        parts.put("a3-gelijkheid", new Object[]{MODEL_A3, RULES_A3, cases(instancesA3(), expectationsA3())});
        parts.put("b-stap", new Object[]{MODEL_B, RULES_B, cases(instancesB(), expectationsB())});
        parts.put("c-tellend", new Object[]{MODEL_C, RULES_C, cases(instancesC(), expectationsC())});
        parts.put("e-emmer-fout", new Object[]{MODEL_E, RULES_E, List.of(bucketError())});
        parts.put("e-emmer-speelt", new Object[]{MODEL_E, RULES_E, List.of(bucketPlays())});
        parts.put("e-emmer-pad", new Object[]{MODEL_E_PATH, RULES_E_PATH, List.of(bucketOverPath())});
        // the refused forms, each with one case, to measure what the other executors do with them
        List<JsonNode> jutter = cases(List.of(instance("J1", "Jutter", Map.of("drempel", numberValue("20"),
                "vaardagen", timeline(months(2024, 1, 12, "10"))), List.of(), WHOLE_2024, Map.of())), List.of(consistent()));
        String inside = "{\"node\":\"CharacteristicCheck\",\"characteristic\":\"wachtplichtig\",\"negated\":true,"
                + "\"wholePeriod\":" + whole(true, false) + ",\"source\":{\"snippet\":\"hij gedurende het gehele jaar"
                + " niet wachtplichtig is\"}}";
        parts.put("r1-binnen", new Object[]{MODEL_R, List.of(trouw(inside)), jutter});
        parts.put("r2-over-pad", new Object[]{MODEL_B, List.of(premie("werftoeslag", "Schuit", "werftoeslag", "10",
                wholeComparison("greater", WERF_HAVENDAGEN, number("5"), whole(true, false),
                        "de havendagen van de werf van de Schuit gedurende het gehele jaar groter is dan 5"))),
                cases(instancesB(), List.of(consistent()))});
        parts.put("r3-twee-tijdlijnen", new Object[]{MODEL_R, List.of(trouw(wholeComparison("greater",
                reading("wachtdagen", "Jutter"), reading("vaardagen", "Jutter"), whole(true, false),
                "zijn wachtdagen gedurende het gehele jaar groter is dan zijn vaardagen"))), jutter});
        for (Map.Entry<String, Object[]> d : parts.entrySet()) {
            @SuppressWarnings("unchecked") List<String> rules = (List<String>) d.getValue()[1];
            @SuppressWarnings("unchecked") List<JsonNode> cases = (List<JsonNode>) d.getValue()[2];
            int n = 0;
            for (List<String> order : TmsDocuments.permutations(rules)) {
                Path map = out.resolve(d.getKey()).resolve(String.format("v%03d", n++));
                TmsTimelineWitnessTest.write(map, TmsTimelineDocuments.ir((String) d.getValue()[0], order,
                        List.of()), cases.get(0));
                java.nio.file.Files.move(map.resolve("case.json"), map.resolve("case1.json"));
                if (cases.size() > 1) java.nio.file.Files.writeString(map.resolve("case2.json"), TmsDocuments.JSON
                        .writerWithDefaultPrettyPrinter().writeValueAsString(cases.get(1)));
            }
        }
    }
}
