package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;

import dev.uchar.regelspraak.graph.ProducerConsumer.ElementKey;
import dev.uchar.regelspraak.transpiler.tms.BucketInput;

import org.drools.model.Rule;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static dev.uchar.regelspraak.cases.TmsDocuments.aggregation;
import static dev.uchar.regelspraak.cases.TmsDocuments.sourceRole;
import static dev.uchar.regelspraak.cases.TmsDocuments.check;
import static dev.uchar.regelspraak.cases.TmsDocuments.consistent;
import static dev.uchar.regelspraak.cases.TmsDocuments.factTypeOneToMany;
import static dev.uchar.regelspraak.cases.TmsDocuments.number;
import static dev.uchar.regelspraak.cases.TmsDocuments.numberValue;
import static dev.uchar.regelspraak.cases.TmsDocuments.inconsistent;
import static dev.uchar.regelspraak.cases.TmsDocuments.characteristicRule;
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
 * The two conventions of the case input of an attribute on a timeline on the exec-model branch (RUNTIME-SEMANTICS
 * ch. 8.4): the asOfDate-bucket scalar, one value for the period of the attribute's grid that holds the evaluation date,
 * serves the subject's own total without a clip (ch. 3.6); the full timeline serves every other read; "an executor that
 * needs the one and finds the other faults — never guesses". The case runner states each bucket scalar beside the bean
 * ({@link BucketInput}), the branch reads the input through the attribute's standing in the convention the case gives
 * it, and a form that finds the other convention faults where the evaluation reaches it, as a logical fault that counts
 * at the fixpoint. Every case has the evaluation date 2024-06-01, so a bucket scalar on a month grid is June 2024 and
 * on a year grid 2024. Each part runs in every order of its rules in the document.
 *
 * <p><b>A, the base of a pro-rata part</b> (ch. 3.7), on the chain of {@link TmsTimelineWitnessTest} part A: J1 (score
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
 * geregistreerd is" ({@link TmsEverUniquenessWitnessTest}): V1 (geregistreerd, bewaard ten days in January 2024, the
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
 * fault that stands at the fixpoint ({@code EvaluationFaults}), each in the reference interpreter's words with the object
 * named.
 *
 * <p><b>Red</b> on c08ac038, the code before the change, with only the class {@link BucketInput} added so that this
 * witness compiles: seven of the thirteen tests fail. A: "case: attribuut jaardeel: verwacht (leeg), werkelijk
 * [2024-01-01,2025-01-01)=306; …", the scalar read as the timeline of 2024. C, D and E: "gespeeld", the full timeline
 * totalled, the scalar clipped, the scalar summed. F: "case: verwacht consistent, werkelijk inconsistent
 * (eenmalig nummer &#64; V1, eenmalig nummer &#64; V4)", the scalars read as keys. G: "[gespeeld, gespeeld]". B: no order
 * saw a fault on the way.
 * The other six pass there as well and here: the cases that play, and H, which the case reader refused already.
 */
class TmsInputFormWitnessTest {

    /** The evaluation date of every case of this class: a scalar input on a temporal attribute lies in its bucket. */
    static final String DATE = "2024-06-01";

    /** The outcomes of {@code caseDocument} on the document of {@code model} with {@code rules}, in every order of the rules,
     *  each as "[names] outcome" ({@link TmsFaultWitnessTest#play}), beside the expected list with {@code expected}. */
    static void inEveryOrder(String model, List<String> rules, List<String> uniquenesses, JsonNode caseDocument,
                               String expected) throws Exception {
        List<String> outcomes = new ArrayList<>(), expectation = new ArrayList<>();
        for (List<String> order : TmsDocuments.permutations(rules)) {
            List<String> names = TmsDocuments.ruleNames(order);
            Path ir = TmsTimelineDocuments.document(model, order, uniquenesses);
            outcomes.add(names + " " + TmsFaultWitnessTest.play(ir, caseDocument, DroolsBackend.Branch.EXEC_MODEL));
            expectation.add(names + " " + expected);
        }
        assertEquals(expectation, outcomes);
    }

    // ---------------- A: the base of a pro-rata part ----------------

    static final List<String> RULES_A = TmsTimelineWitnessTest.RULES_A;
    static final Map<String, List<String>> BORG = TmsTimelineWitnessTest.BORG;

    static JsonNode caseA() throws Exception {
        return TmsTimelineDocuments.caseDocument(DATE, List.of(
                        instance("J1", "Jutter", Map.of("score", numberValue("10"), "jaartarief", numberValue("366"),
                                "bedrag", TmsTimelineWitnessTest.AMOUNT), List.of(), BORG, Map.of()),
                        instance("J2", "Jutter", Map.of("score", numberValue("3"), "jaartarief", numberValue("366"),
                                "bedrag", TmsTimelineWitnessTest.AMOUNT, "maanddeel", TmsTimelineWitnessTest.JUNE),
                                List.of(), BORG, Map.of()),
                        instance("J3", "Jutter", Map.of("score", numberValue("10"), "jaartarief",
                                TmsTimelineWitnessTest.RATE, "maanddeel", TmsTimelineWitnessTest.JUNE), List.of(), BORG,
                                Map.of())),
                List.of(expectedTimeline("J1", "jaardeel", List.of()), expectedTimeline("J1", "maanddeel", List.of()),
                        value("J1", "wachtsom", "0"), value("J1", "topsom", "0"),
                        expectedTimeline("J2", "jaardeel", List.of()),
                        expectedTimeline("J2", "maanddeel", List.of(segment("2024-06-01", "2024-07-01", "9"))),
                        value("J2", "wachtsom", "9"), value("J2", "topsom", "0"),
                        expectedTimeline("J3", "jaardeel", TmsTimelineWitnessTest.YEAR_PARTS),
                        expectedTimeline("J3", "maanddeel", TmsTimelineWitnessTest.monthParts()),
                        value("J3", "wachtsom", "3178"), value("J3", "topsom", "0"), consistent()));
    }

    @Test void the_base_of_a_pro_rata_as_bucket_scalar_fails_where_the_rule_reaches_it() throws Exception {
        inEveryOrder(TmsTimelineWitnessTest.MODEL_A, RULES_A, List.of(), caseA(), "InputFormFault: fout in de regel"
                + " \"jaardeel\": the pro-rata base \"jaartarief\" needs the full timeline; \"J1\" supplies only the"
                + " asOfDate-bucket scalar (ch. 3.7, ch. 8.4)");
    }

    // ---------------- B: a fault on an intermediate state goes again ----------------

    static JsonNode caseB() throws Exception {
        return TmsTimelineDocuments.caseDocument(DATE, List.of(
                        instance("J1", "Jutter", Map.of("score", numberValue("3"), "jaartarief", numberValue("366")),
                                List.of(), BORG, Map.of()),
                        instance("J2", "Jutter", Map.of("score", numberValue("10"), "jaartarief",
                                TmsTimelineWitnessTest.RATE), List.of(), BORG, Map.of())),
                List.of(expectedTimeline("J1", "jaardeel", List.of()),
                        expectedTimeline("J2", "jaardeel", TmsTimelineWitnessTest.YEAR_PARTS), consistent()));
    }

    static final List<String> RULES_B = List.of(TmsTimelineWitnessTest.VRIJSTELLING,
            TmsTimelineWitnessTest.YEAR_PART_NON_FREE);

    @Test void a_base_that_the_rule_does_not_reach_at_the_fixpoint_does_not_fail() throws Exception {
        inEveryOrder(TmsTimelineWitnessTest.MODEL_B, RULES_B, List.of(), caseB(), "gespeeld");
    }

    /** Part B over the generated rules in sampled orders, J1 alone as the case runner sets it up: the bucket scalar on
     *  the bean and its {@link BucketInput} beside it. At the fixpoint no fault stands and the jaardeel is never
     *  written, and in some order the jaardeel rule reached the base before J1 was vrijgesteld, and its fault went
     *  again. */
    @Test void a_fault_on_an_intermediate_state_goes_away_again() throws Exception {
        Path ir = TmsTimelineDocuments.document(TmsTimelineWitnessTest.MODEL_B, RULES_B, List.of());
        DroolsBackend.Build b = DroolsBackend.build(ir, DroolsBackend.Branch.EXEC_MODEL);
        int intermediateState = 0;
        for (List<Rule> order : TmsDocuments.sample(b.execModelTranspiler().model().getRules(), 20261009L, 100)) {
            List<String> names = order.stream().map(Rule::getName).toList();
            Object j1 = TmsDocuments.beanObject(b.beanLoader(), "Jutter", Map.of("score", "3"), List.of());
            TmsTimelineDocuments.set(j1, "jaartarief", List.of(
                    TmsTimelineDocuments.beanSegment(b.beanLoader(), "2024-01-01", "2025-01-01", "366")));
            TmsTimelineDocuments.set(j1, "borgplichtig", List.of(
                    TmsTimelineDocuments.beanSegment(b.beanLoader(), "2024-03-01", "2025-03-01", null)));
            BucketInput bucket = new BucketInput(j1, ElementKey.van("Jutter", "jaartarief", false));
            TmsLetWitnessTest.Facts facts = new TmsLetWitnessTest.Facts();
            assertNull(TmsDocuments.fixpoint(b, order, List.of(j1, bucket), session -> session.addEventListener(facts)),
                    () -> "volgorde " + names);
            assertEquals(facts.inserted, facts.withdrawn, () -> "foutfeiten in volgorde " + names);
            assertNull(TmsDocuments.read(j1, "jaardeel"), () -> "jaardeel in volgorde " + names);
            if (facts.faults > 0) intermediateState++;
        }
        assertTrue(intermediateState > 0, "geen volgorde liet de jaardeelregel de basis lezen voor J1 vrijgesteld was");
    }

    // ---------------- C: the subject's own total without a clip ----------------

    static final String MODEL_C = TmsDocuments.model(List.of(objectType("Jutter", List.of(
            TmsDocuments.attribute("score", WHOLE), timeAttribute("bedrag", WHOLE, "month"),
            TmsDocuments.attribute("topsom", WHOLE)), List.of(characteristic("actief")))), List.of());

    static final String TOPSOM_WITHOUT_CLIP = computingRule("topsom", "BEREKENING", "Jutter", "topsom",
            ownTotal("bedrag", "Jutter", "null"), check("actief", false));

    static final List<String> RULES_C = List.of(TmsTimelineWitnessTest.ACTIEF, TOPSOM_WITHOUT_CLIP);

    /** J3 (actief, the bedrag 7 as a bucket scalar), J4 (actief, no bedrag), J2 (not actief, a full timeline), and with
     *  {@code withJ1} J1 (actief, a full timeline). */
    static JsonNode caseC(boolean withJ1) throws Exception {
        List<String> instances = new ArrayList<>();
        if (withJ1) instances.add(instance("J1", "Jutter", Map.of("score", numberValue("10"), "bedrag",
                TmsTimelineWitnessTest.AMOUNT), List.of(), Map.of(), Map.of()));
        instances.add(instance("J2", "Jutter", Map.of("score", numberValue("3"), "bedrag",
                TmsTimelineWitnessTest.AMOUNT), List.of(), Map.of(), Map.of()));
        instances.add(instance("J3", "Jutter", Map.of("score", numberValue("10"), "bedrag", numberValue("7")),
                List.of(), Map.of(), Map.of()));
        instances.add(instance("J4", "Jutter", Map.of("score", numberValue("10")), List.of(), Map.of(), Map.of()));
        return TmsTimelineDocuments.caseDocument(DATE, instances, List.of(value("J2", "topsom", null),
                value("J3", "topsom", "7"), value("J4", "topsom", null)));
    }

    @Test void the_own_total_without_clip_fails_on_a_full_timeline_where_the_rule_reaches_it() throws Exception {
        inEveryOrder(MODEL_C, RULES_C, List.of(), caseC(true), "InputFormFault: fout in de regel \"topsom\": the"
                + " temporal total of \"bedrag\" over a case-supplied full timeline is not exercised by any corpus case;"
                + " the asOfDate-bucket convention carries the total, and \"J1\" supplies a full timeline");
    }

    @Test void the_own_total_without_clip_takes_the_bucket_scalar() throws Exception {
        inEveryOrder(MODEL_C, RULES_C, List.of(), caseC(false), "gespeeld");
    }

    // ---------------- D: the clip of a total, its value and the timeline it compares ----------------

    static final String MODEL_D = TmsDocuments.model(List.of(objectType("Jutter", List.of(
            timeAttribute("vergoeding", WHOLE, "month"), timeAttribute("controle", WHOLE, "month"),
            timeAttribute("jaarcontrole", WHOLE, "year"), TmsDocuments.attribute("wachtsom", WHOLE),
            TmsDocuments.attribute("topsom", WHOLE)),
            List.of(timeCharacteristic("wachtplichtig", "month"), timeCharacteristic("borgplichtig", "month")))), List.of());

    /** "De controle van een Jutter moet berekend worden als het tijdsevenredig deel per maand van zijn jaarcontrole
     *  gedurende de tijd dat hij borgplichtig is": its only writer, so the input stands where it computes nothing. */
    static final String CHECK = computingRule("controle", "BEREKENING", "Jutter", "controle",
            timeProportional("jaarcontrole", "Jutter", check("borgplichtig", false), "month"), "null");
    static final String GUARD = computingRule("wachtsom", "BEREKENING", "Jutter", "wachtsom",
            ownTotal("vergoeding", "Jutter", check("wachtplichtig", false)), "null");
    static final String TOP = computingRule("topsom", "BEREKENING", "Jutter", "topsom",
            ownTotal("vergoeding", "Jutter", comparison("greater", ownRead("controle", "Jutter"), number("5"))), "null");

    static final List<String> RULES_D = List.of(CHECK, GUARD, TOP);

    static final String VERGOEDING_2024 = timeline(months(2024, 1, 12, "10"));

    /** J1: the vergoeding 11 as a bucket scalar, wachtplichtig through 2024. */
    static String j1D() {
        return instance("J1", "Jutter", Map.of("vergoeding", numberValue("11")), List.of(),
                Map.of("wachtplichtig", List.of(period("2024-01-01", "2025-01-01"))), Map.of());
    }

    /** J2: the vergoeding 10 a month through 2024, the controle 7 as a bucket scalar, no true period. */
    static String j2D() {
        return instance("J2", "Jutter", Map.of("vergoeding", VERGOEDING_2024, "controle", numberValue("7")), List.of(),
                Map.of(), Map.of());
    }

    /** J3: as J2, with the jaarcontrole 12 in 2024 and borgplichtig March–May 2024, so the controle is derived. */
    static String j3D() {
        return instance("J3", "Jutter", Map.of("vergoeding", VERGOEDING_2024, "controle", numberValue("7"),
                        "jaarcontrole", timeline(List.of(segment("2024-01-01", "2025-01-01", "12")))), List.of(),
                Map.of("borgplichtig", List.of(period("2024-03-01", "2024-06-01"))), Map.of());
    }

    @Test void the_value_of_a_clip_and_the_compared_timeline_as_bucket_scalar_fail() throws Exception {
        inEveryOrder(MODEL_D, RULES_D, List.of(), TmsTimelineDocuments.caseDocument(DATE, List.of(j1D(), j2D(), j3D()),
                        List.of(consistent())),
                "EvaluationFaults: aan het fixpunt staan 3 evaluatiefouten, in de regels \"topsom\" (2×), \"wachtsom\": fout"
                        + " in de regel \"topsom\": the gedurende clip of \"vergoeding\" compares \"controle\" per segment"
                        + " and needs its full timeline; \"J2\" supplies only the asOfDate-bucket scalar (ch. 3.6, ch."
                        + " 8.4); fout in de regel \"topsom\": the gedurende clip of \"vergoeding\" needs the full timeline;"
                        + " \"J1\" supplies only the asOfDate-bucket scalar; fout in de regel \"wachtsom\": the gedurende"
                        + " clip of \"vergoeding\" needs the full timeline; \"J1\" supplies only the asOfDate-bucket"
                        + " scalar");
    }

    @Test void a_compared_timeline_that_a_rule_derives_replaces_the_bucket_scalar() throws Exception {
        inEveryOrder(MODEL_D, RULES_D, List.of(), TmsTimelineDocuments.caseDocument(DATE, List.of(j3D()), List.of(
                expectedTimeline("J3", "controle", months(2024, 3, 3, "12")), value("J3", "topsom", "30"),
                value("J3", "wachtsom", "0"))), "gespeeld");
    }

    // ---------------- E: a member of a per-element fold ----------------

    static final String MODEL_E = TmsDocuments.model(List.of(
            objectType("Jutter", List.of(TmsDocuments.attribute("last", WHOLE)), List.of()),
            objectType("Vondst", List.of(timeAttribute("jaartarief", WHOLE, "year"),
                    timeAttribute("tarief", WHOLE, "month"), TmsDocuments.attribute("keurscore", WHOLE)),
                    List.of(timeCharacteristic("bewaard", "month"), characteristic("gekeurd")))),
            List.of(factTypeOneToMany("bewaring", "jutter", "Jutter", "vondst", "Vondst", "vondsten")));

    /** "Het tarief van een Vondst moet berekend worden als het tijdsevenredig deel per maand van zijn jaartarief
     *  gedurende de tijd dat hij bewaard is": its only writer, so the input stands where it computes nothing. */
    static final String RATE_E = computingRule("tarief", "BEREKENING", "Vondst", "tarief",
            timeProportional("jaartarief", "Vondst", check("bewaard", false), "month"), "null");
    static final String GEKEURD = characteristicRule("keuring", "Vondst", "gekeurd",
            comparison("greater", ownRead("keurscore", "Vondst"), number("5")));
    static final String LAST = computingRule("last", "BEREKENING", "Jutter", "last", aggregation("total",
            ownRead("tarief", "Vondst"), "Vondst", sourceRole("bewaring", "vondst"), check("gekeurd", false), "null", WHOLE,
            "het totaal van de tarieven van zijn vondsten die gekeurd zijn"), "null");

    static final List<String> RULES_E = List.of(RATE_E, GEKEURD, LAST);

    static final String JAARTARIEF_12 = timeline(List.of(segment("2024-01-01", "2025-01-01", "12")));
    static final Map<String, List<String>> KEPT_Q1 = Map.of("bewaard", List.of(period("2024-01-01", "2024-04-01")));

    /** A Vondst with the keurscore {@code score} and the {@code waarden} beside it, bewaard on {@code periods}. */
    static String vondst(String label, String score, Map<String, String> waarden, Map<String, List<String>> periods) {
        Map<String, String> all = new LinkedHashMap<>(waarden);
        all.put("keurscore", numberValue(score));
        return instance(label, "Vondst", all, List.of(), periods, Map.of());
    }

    static String jutter(String label, List<String> vondsten) {
        return instance(label, "Jutter", Map.of(), List.of(), Map.of(), Map.of("vondsten", vondsten));
    }

    /** J2 keeps V3, the tarief 50 as a bucket scalar, not gekeurd: last 0. J3 keeps V4, the tarief 7 as a bucket scalar
     *  that the pro-rata part replaces with 12 a month January–March: last 36. */
    static List<String> playingE() {
        return List.of(jutter("J2", List.of("V3")), jutter("J3", List.of("V4")),
                vondst("V3", "2", Map.of("tarief", numberValue("50")), Map.of()),
                vondst("V4", "8", Map.of("tarief", numberValue("7"), "jaartarief", JAARTARIEF_12), KEPT_Q1));
    }

    @Test void a_member_as_bucket_scalar_fails_and_the_fold_names_the_first_in_fold_order() throws Exception {
        List<String> instances = new ArrayList<>(List.of(jutter("J1", List.of("V1", "V2")),
                jutter("J4", List.of("V6", "V5")),
                vondst("V1", "8", Map.of("jaartarief", JAARTARIEF_12), KEPT_Q1),
                vondst("V2", "8", Map.of("tarief", numberValue("5")), Map.of()),
                vondst("V5", "9", Map.of("tarief", numberValue("4")), Map.of()),
                vondst("V6", "9", Map.of("tarief", numberValue("6")), Map.of())));
        instances.addAll(playingE());
        inEveryOrder(MODEL_E, RULES_E, List.of(), TmsTimelineDocuments.caseDocument(DATE, instances,
                        List.of(consistent())),
                "EvaluationFaults: aan het fixpunt staan 2 evaluatiefouten, in de regel \"last\" (2×): fout in de regel"
                        + " \"last\": the per-element temporal total of \"tarief\" over \"V2\": the case supplies an"
                        + " asOfDate-bucket scalar, which carries no period of its own; a set fold needs each member's own"
                        + " timeline (ch. 3.6/§7.1, ch. 8.4); fout in de regel \"last\": the per-element temporal total of"
                        + " \"tarief\" over \"V6\": the case supplies an asOfDate-bucket scalar, which carries no period of"
                        + " its own; a set fold needs each member's own timeline (ch. 3.6/§7.1, ch. 8.4)");
    }

    @Test void a_member_that_does_not_pass_the_filter_or_has_a_derived_timeline_does_not_fail() throws Exception {
        inEveryOrder(MODEL_E, RULES_E, List.of(), TmsTimelineDocuments.caseDocument(DATE, playingE(), List.of(
                value("J2", "last", "0"), value("J3", "last", "36"),
                expectedTimeline("V4", "tarief", months(2024, 1, 3, "12")))), "gespeeld");
    }

    // ---------------- F: an ever-unique key ----------------

    static final String MODEL_F = TmsEverUniquenessWitnessTest.MODEL;
    static final List<String> RULES_F = TmsEverUniquenessWitnessTest.RULES;
    static final String EENMALIG = TmsEverUniquenessWitnessTest.EENMALIG;

    /** A Vondst with the code {@code code}, bewaard on {@code [van, tot)} where {@code van} is not null, and the nummer
     *  {@code nummer} as a bucket scalar where it is not null. */
    static String vondstF(String label, String code, String van, String to, String vondstNumber) {
        Map<String, String> waarden = new LinkedHashMap<>();
        waarden.put("code", numberValue(code));
        if (vondstNumber != null) waarden.put("nummer", numberValue(vondstNumber));
        return instance(label, "Vondst", waarden, List.of(),
                van == null ? Map.of() : Map.of("bewaard", List.of(period(van, to))), Map.of());
    }

    /** V1 is geregistreerd and bewaard ten days in January 2024, its nummer 7 as a bucket scalar, which the day count
     *  replaces with 10 in 2024; V4 is bewaard ten days in March 2025, nummer 10 in 2025. */
    static List<String> playingF() {
        return List.of(vondstF("V1", "1", "2024-01-01", "2024-01-11", "7"),
                vondstF("V4", "1", "2025-03-01", "2025-03-11", null));
    }

    @Test void an_ever_key_as_bucket_scalar_fails() throws Exception {
        List<String> instances = new ArrayList<>(playingF());
        // V2 is not geregistreerd, so its nummer 7 is not computed and stands; V3 is geregistreerd without a true
        // period, so the day count is empty and its nummer 5 stands
        instances.add(vondstF("V2", "0", null, null, "7"));
        instances.add(vondstF("V3", "1", null, null, "5"));
        inEveryOrder(MODEL_F, RULES_F, List.of(EENMALIG), TmsTimelineDocuments.caseDocument(DATE, instances,
                        List.of(consistent())),
                "EvaluationFaults: aan het fixpunt staan 2 evaluatiefouten, in de regel \"eenmalig nummer\" (2×): fout in"
                        + " de regel \"eenmalig nummer\": the ever-unique key \"nummer\" needs the full timeline; \"V2\""
                        + " supplies only a scalar (ch. 5.9); fout in de regel \"eenmalig nummer\": the ever-unique key"
                        + " \"nummer\" needs the full timeline; \"V3\" supplies only a scalar (ch. 5.9)");
    }

    @Test void an_ever_key_that_the_day_count_derives_replaces_the_bucket_scalar() throws Exception {
        inEveryOrder(MODEL_F, RULES_F, List.of(EENMALIG), TmsTimelineDocuments.caseDocument(DATE, playingF(), List.of(
                        expectedTimeline("V1", "nummer", List.of(TmsEverUniquenessWitnessTest.year(2024, "10"))),
                        expectedTimeline("V4", "nummer", List.of(TmsEverUniquenessWitnessTest.year(2025, "10"))),
                        inconsistent("eenmalig nummer", "V1"), inconsistent("eenmalig nummer", "V4"))),
                "gespeeld");
    }

    // ---------------- G: the key's form before the scope and after an empty component ----------------

    static final String MODEL_G = TmsDocuments.model(List.of(
            objectType("Strandvondst", List.of(TmsDocuments.attribute("code", WHOLE),
                    timeAttribute("vondstnummer", WHOLE, "year"), timeAttribute("kavelnummer", WHOLE, "year")),
                    List.of()),
            objectType("Kust", List.of(), List.of())),
            List.of(factTypeOneToMany("ligging", "kust", "Kust", "strandvondst", "Strandvondst", "strandvondsten")));

    static final String PER_KUST = TmsUniquenessWitnessTest.uniqueness("kavel per kust",
            List.of(TmsUniquenessWitnessTest.source("Strandvondst", List.of("kavelnummer"))),
            TmsDocuments.roleBinding("ligging", "kust"),
            "De kavelnummers van alle Strandvondsten van de kust moeten uniek zijn.");
    static final String CODE_AND_NUMBER = TmsUniquenessWitnessTest.uniqueness("code en nummer",
            List.of(TmsUniquenessWitnessTest.source("Strandvondst", List.of("code", "vondstnummer"))), null,
            "De codes en vondstnummers van alle Strandvondsten moeten uniek zijn.");

    @Test void the_form_of_the_key_precedes_the_scope_and_follows_an_empty_part() throws Exception {
        JsonNode caseDocument = TmsTimelineDocuments.caseDocument(DATE, List.of(
                        // no kust: it takes no part in the scoped form, and its kavelnummer as a bucket scalar faults
                        instance("S1", "Strandvondst", Map.of("kavelnummer", numberValue("3")), List.of(), Map.of(),
                                Map.of()),
                        // no code: the tuple stops at the empty component, and the vondstnummer is not read
                        instance("S2", "Strandvondst", Map.of("vondstnummer", numberValue("7")), List.of(), Map.of(),
                                Map.of()),
                        instance("S3", "Strandvondst", Map.of("code", numberValue("1"), "vondstnummer",
                                numberValue("8")), List.of(), Map.of(), Map.of())),
                List.of(consistent()));
        List<String> outcomes = new ArrayList<>();
        for (List<String> order : TmsDocuments.permutations(List.of(PER_KUST, CODE_AND_NUMBER)))
            outcomes.add(TmsFaultWitnessTest.play(TmsTimelineDocuments.document(MODEL_G, List.of(), order), caseDocument,
                    DroolsBackend.Branch.EXEC_MODEL));
        String expected = "EvaluationFaults: aan het fixpunt staan 2 evaluatiefouten, in de regels \"code en nummer\","
                + " \"kavel per kust\": fout in de regel \"code en nummer\": the ever-unique key \"vondstnummer\" needs"
                + " the full timeline; \"S3\" supplies only a scalar (ch. 5.9); fout in de regel \"kavel per kust\": the"
                + " ever-unique key \"kavelnummer\" needs the full timeline; \"S1\" supplies only a scalar (ch. 5.9)";
        assertEquals(List.of(expected, expected), outcomes);
    }

    // ---------------- H: a characteristic on a timeline in the flat list ----------------

    static final String MODEL_H = TmsDocuments.model(List.of(objectType("Jutter", List.of(
            timeAttribute("vergoeding", WHOLE, "month"), timeAttribute("jaartarief", WHOLE, "year"),
            timeAttribute("jaardeel", WHOLE, "year"), timeAttribute("stormdagen", WHOLE, "year"),
            TmsDocuments.attribute("wachtsom", WHOLE)),
            List.of(timeCharacteristic("wachtplichtig", "month")))), List.of());

    /** The clip of a total, the condition of a pro-rata part and the condition of a day count, each over wachtplichtig. */
    static final Map<String, String> RULES_H = Map.of(
            "knip", GUARD,
            "pro-rata", computingRule("jaardeel", "BEREKENING", "Jutter", "jaardeel", timeProportional("jaartarief", "Jutter",
                    check("wachtplichtig", false), "year"), "null"),
            "dagentelling", computingRule("stormdagen", "BEREKENING", "Jutter", "stormdagen",
                    dayCount(check("wachtplichtig", false), "year"), "null"));

    static JsonNode caseH() throws Exception {
        return TmsTimelineDocuments.caseDocument(DATE, List.of(instance("J1", "Jutter", Map.of("vergoeding", VERGOEDING_2024,
                "jaartarief", JAARTARIEF_12), List.of("wachtplichtig"), Map.of(), Map.of())), List.of(consistent()));
    }

    @Test void a_time_dependent_characteristic_in_the_flat_list_refuses_the_case_reader() throws Exception {
        List<String> outcomes = new ArrayList<>(), expected = new ArrayList<>();
        for (Map.Entry<String, String> r : new java.util.TreeMap<>(RULES_H).entrySet()) {
            outcomes.add(r.getKey() + " " + TmsFaultWitnessTest.play(TmsTimelineDocuments.document(MODEL_H,
                    List.of(r.getValue()), List.of()), caseH(), DroolsBackend.Branch.EXEC_MODEL));
            expected.add(r.getKey() + " IllegalArgumentException: scalar characteristic input 'wachtplichtig' on the"
                    + " time-dependent kenmerk of Jutter: deliver it as a characteristicTimelines entry — the static"
                    + " characteristics list asserts a scalar truth no temporal reader consults");
        }
        assertEquals(expected, outcomes);
    }

    // ---------------- the documents, for the other executors ----------------

    /** Writes every document of this class, in every order of its rules, with its cases, under {@code args[0]}: a map
     *  per part and order, holding {@code ir.json} and the case files, for the verifier, ref and clingo. */
    public static void main(String[] args) throws Exception {
        Path out = Path.of(args[0]);
        List<String> instancesE = new ArrayList<>(List.of(jutter("J1", List.of("V1", "V2")),
                jutter("J4", List.of("V6", "V5")),
                vondst("V1", "8", Map.of("jaartarief", JAARTARIEF_12), KEPT_Q1),
                vondst("V2", "8", Map.of("tarief", numberValue("5")), Map.of()),
                vondst("V5", "9", Map.of("tarief", numberValue("4")), Map.of()),
                vondst("V6", "9", Map.of("tarief", numberValue("6")), Map.of())));
        instancesE.addAll(playingE());
        List<String> instancesF = new ArrayList<>(playingF());
        instancesF.add(vondstF("V2", "0", null, null, "7"));
        instancesF.add(vondstF("V3", "1", null, null, "5"));
        Map<String, Object[]> parts = new LinkedHashMap<>();
        parts.put("a-basis", new Object[]{TmsTimelineWitnessTest.MODEL_A, RULES_A, List.of(), List.of(caseA())});
        parts.put("b-tussenstand", new Object[]{TmsTimelineWitnessTest.MODEL_B, RULES_B, List.of(), List.of(caseB())});
        parts.put("c-eigen-totaal", new Object[]{MODEL_C, RULES_C, List.of(), List.of(caseC(true), caseC(false))});
        parts.put("d-knip", new Object[]{MODEL_D, RULES_D, List.of(), List.of(
                TmsTimelineDocuments.caseDocument(DATE, List.of(j1D(), j2D(), j3D()), List.of(consistent())),
                TmsTimelineDocuments.caseDocument(DATE, List.of(j3D()), List.of(
                        expectedTimeline("J3", "controle", months(2024, 3, 3, "12")), value("J3", "topsom", "30"),
                        value("J3", "wachtsom", "0"))),
                TmsTimelineDocuments.caseDocument(DATE, List.of(j1D()), List.of(consistent())),
                TmsTimelineDocuments.caseDocument(DATE, List.of(j2D()), List.of(value("J2", "topsom", "0"),
                        value("J2", "wachtsom", "0"))))});
        parts.put("e-lid", new Object[]{MODEL_E, RULES_E, List.of(), List.of(
                TmsTimelineDocuments.caseDocument(DATE, instancesE, List.of(consistent())),
                TmsTimelineDocuments.caseDocument(DATE, playingE(), List.of(value("J2", "last", "0"),
                        value("J3", "last", "36"), expectedTimeline("V4", "tarief", months(2024, 1, 3, "12")))),
                TmsTimelineDocuments.caseDocument(DATE, List.of(jutter("J4", List.of("V6", "V5")),
                        vondst("V5", "9", Map.of("tarief", numberValue("4")), Map.of()),
                        vondst("V6", "9", Map.of("tarief", numberValue("6")), Map.of())), List.of(consistent())))});
        parts.put("f-sleutel", new Object[]{MODEL_F, RULES_F, List.of(EENMALIG), List.of(
                TmsTimelineDocuments.caseDocument(DATE, instancesF, List.of(consistent())),
                TmsTimelineDocuments.caseDocument(DATE, playingF(), List.of(
                        expectedTimeline("V1", "nummer", List.of(TmsEverUniquenessWitnessTest.year(2024, "10"))),
                        expectedTimeline("V4", "nummer", List.of(TmsEverUniquenessWitnessTest.year(2025, "10"))),
                        inconsistent("eenmalig nummer", "V1"), inconsistent("eenmalig nummer", "V4"))))});
        for (Map.Entry<String, Object[]> d : parts.entrySet()) {
            @SuppressWarnings("unchecked") List<String> rules = (List<String>) d.getValue()[1];
            @SuppressWarnings("unchecked") List<String> uniquenesses = (List<String>) d.getValue()[2];
            @SuppressWarnings("unchecked") List<JsonNode> cases = (List<JsonNode>) d.getValue()[3];
            int n = 0;
            for (List<String> order : TmsDocuments.permutations(rules))
                write(out.resolve(d.getKey()).resolve(String.format("v%03d", n++)),
                        TmsTimelineDocuments.ir((String) d.getValue()[0], order, uniquenesses), cases);
        }
        int n = 0;
        for (List<String> order : TmsDocuments.permutations(List.of(PER_KUST, CODE_AND_NUMBER)))
            write(out.resolve("g-sleutelvorm").resolve(String.format("v%03d", n++)),
                    TmsTimelineDocuments.ir(MODEL_G, List.of(), order), List.of(TmsTimelineDocuments.caseDocument(DATE,
                            List.of(instance("S1", "Strandvondst", Map.of("kavelnummer", numberValue("3")), List.of(),
                                            Map.of(), Map.of()),
                                    instance("S2", "Strandvondst", Map.of("vondstnummer", numberValue("7")), List.of(),
                                            Map.of(), Map.of()),
                                    instance("S3", "Strandvondst", Map.of("code", numberValue("1"), "vondstnummer",
                                            numberValue("8")), List.of(), Map.of(), Map.of())),
                            List.of(consistent()))));
        for (Map.Entry<String, String> r : new java.util.TreeMap<>(RULES_H).entrySet())
            write(out.resolve("h-kenmerk").resolve(r.getKey()), TmsTimelineDocuments.ir(MODEL_H, List.of(r.getValue()),
                    List.of()), List.of(caseH()));
    }

    /** {@code ir.json} and the cases {@code case.json}, {@code case2.json}, … under {@code map}. */
    static void write(Path map, String ir, List<JsonNode> cases) throws Exception {
        TmsTimelineWitnessTest.write(map, ir, cases.get(0));
        for (int i = 1; i < cases.size(); i++)
            java.nio.file.Files.writeString(map.resolve("case" + (i + 1) + ".json"), TmsDocuments.JSON
                    .writerWithDefaultPrettyPrinter().writeValueAsString(cases.get(i)));
    }
}
