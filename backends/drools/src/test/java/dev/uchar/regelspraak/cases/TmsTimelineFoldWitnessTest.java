package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;

import dev.uchar.regelspraak.transpiler.kern.Namen;
import dev.uchar.regelspraak.transpiler.tms.State;
import dev.uchar.regelspraak.transpiler.tms.Value;

import org.drools.model.Rule;
import org.junit.jupiter.api.Test;
import org.kie.api.event.rule.DefaultRuleRuntimeEventListener;
import org.kie.api.event.rule.ObjectInsertedEvent;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static dev.uchar.regelspraak.cases.TmsDocuments.aggregation;
import static dev.uchar.regelspraak.cases.TmsDocuments.sourceRole;
import static dev.uchar.regelspraak.cases.TmsDocuments.check;
import static dev.uchar.regelspraak.cases.TmsDocuments.consistent;
import static dev.uchar.regelspraak.cases.TmsDocuments.factTypeOneToMany;
import static dev.uchar.regelspraak.cases.TmsDocuments.ownRead;
import static dev.uchar.regelspraak.cases.TmsDocuments.computingRule;
import static dev.uchar.regelspraak.cases.TmsDocuments.waarde;
import static dev.uchar.regelspraak.cases.TmsTimelineDocuments.WHOLE;
import static dev.uchar.regelspraak.cases.TmsTimelineDocuments.instance;
import static dev.uchar.regelspraak.cases.TmsTimelineDocuments.months;
import static dev.uchar.regelspraak.cases.TmsTimelineDocuments.objecttype;
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
 * A total and a sum per member over a timeline on the exec-model branch (RUNTIME-SEMANTICS ch. 3.6, the
 * per-element fold): each member's timeline collapses over time first, at its own granularity, in the member's
 * contribution rule, and the scalars fold over the set as any fold does; a member without a timeline contributes
 * nothing, so over no contribution the total is 0 and the sum leeg.
 *
 * <p>The model: a Jutter keeps his Vondsten (bewaring: jutter, one Jutter; vondst, many Vondsten). "Het tarief van een
 * Vondst moet berekend worden als het tijdsevenredig deel per maand van zijn jaartarief gedurende de tijd dat hij
 * bewaard is", a target on a month grid that only that rule writes, so it stands where the pro-rata part is empty
 * (ch. 7.8); "De last van een Jutter moet berekend worden als het totaal van de tarieven van zijn vondsten" and "De
 * som van een Jutter moet berekend worden als de som van de tarieven van zijn vondsten". So the timeline the two folds
 * collapse is derived by another rule. By hand: V1 (jaartarief 12 in 2024, bewaard on [2024-01-01, 2024-04-01)) gets
 * the tarief 12 × 31/31, 12 × 29/29 and 12 × 31/31 in January, February and March 2024, three segments, which total
 * 36; V2 (tarief 5 on [2024-01-01, 2024-03-01) as input, no jaartarief, no true period) keeps its input, whose total
 * is 2 × 5 = 10; V3 (jaartarief 12 in 2024, no true period) and V4 (bewaard in May 2024, no jaartarief) get no
 * segment and hold no timeline, so they contribute nothing. J1 keeps V1, V2 and V3: last 36 + 10 = 46, som 46. J2
 * keeps none: last 0, som leeg. J3 keeps V4 alone: last 0, som leeg, where a member that contributed 0 would make the
 * sum 0. Every order of the three rules; beside them the generated rules in a hundred sampled orders, where at the
 * fixpoint every object holds at most one {@link Value} per target, and in some order the last of J1 was 10 on the
 * way, before the tarief of V1 was derived.
 *
 * <p><b>Measured</b> (2026-10-09, every document of {@link #main}, all six orders): the verifier passes all six, and the
 * reference interpreter's runner passes every one, "oracle: PASS (11 expectations)". The ASP runner refuses all six,
 * "TRANSLATION FAULT: the per-element temporal fold of "tarief" over a DERIVED member timeline is not translated: a
 * rule writes "tarief" on "Vondst", so its segment bounds are not case data and the whole-unit facts this fold
 * integrates against cannot be computed (no corpus case)"; with the rule of the tarief taken out and V1's three
 * segments of 12 given as input, it passes the case with the same totals and sums, "clingo: PASS (7 expectations)",
 * and so does the reference interpreter.
 *
 * <p><b>Red</b> on b9f77aa7, the code before the change: in every order the first rule of the three is refused, "but
 * was: &lt;[[tarief, last, som] Excluded: tms-transpiler SKIP: regel: tarief — het tijdsafhankelijke attribuut 'tarief'
 * van Vondst: tijdlijnen zijn buiten de proef van de truth-maintenance-tak, …, [last, tarief, som] Excluded: … regel:
 * last — een Aggregation TOTAAL over het tijdsafhankelijke attribuut 'tarief' in een waarde ('het totaal van de
 * tarieven van zijn vondsten'): tijdlijnen zijn buiten de proef van de truth-maintenance-tak, …, [som, last, tarief]
 * Excluded: … regel: som — een Aggregation SOM over het tijdsafhankelijke attribuut 'tarief' …&gt;", and the build of
 * the sampled orders on the refusal of the tarief. <b>Red by mutation</b> (not committed): with a member without a
 * timeline contributing 0, every order fails on J3, "case: verwacht leeg, werkelijk 0".
 */
class TmsTimelineFoldWitnessTest {

    static final String MODEL = TmsDocuments.model(List.of(
            objecttype("Jutter", List.of(TmsDocuments.attribuut("last", WHOLE), TmsDocuments.attribuut("som", WHOLE)),
                    List.of()),
            objecttype("Vondst", List.of(timeAttribute("jaartarief", WHOLE, "year"),
                    timeAttribute("tarief", WHOLE, "month")), List.of(timeCharacteristic("bewaard", "month")))),
            List.of(factTypeOneToMany("bewaring", "jutter", "Jutter", "vondst", "Vondst", "vondsten")));

    static final String RATE = computingRule("tarief", "BEREKENING", "Vondst", "tarief",
            timeProportional("jaartarief", "Vondst", check("bewaard", false), "month"), "null");
    static final String LAST = computingRule("last", "BEREKENING", "Jutter", "last", aggregation("total",
            ownRead("tarief", "Vondst"), "Vondst", sourceRole("bewaring", "vondst"), "null", "null", WHOLE,
            "het totaal van de tarieven van zijn vondsten"), "null");
    static final String SUM = computingRule("som", "BEREKENING", "Jutter", "som", aggregation("sum",
            ownRead("tarief", "Vondst"), "Vondst", sourceRole("bewaring", "vondst"), "null", "null", WHOLE,
            "de som van de tarieven van zijn vondsten"), "null");

    static final List<String> RULES = List.of(RATE, LAST, SUM);

    static final String JAARTARIEF = timeline(List.of(segment("2024-01-01", "2025-01-01", "12")));

    static JsonNode caseDocument() throws Exception {
        return TmsTimelineDocuments.caseDocument(null, List.of(
                        instance("J1", "Jutter", Map.of(), List.of(), Map.of(),
                                Map.of("vondsten", List.of("V1", "V2", "V3"))),
                        instance("J2", "Jutter", Map.of(), List.of(), Map.of(), Map.of()),
                        instance("J3", "Jutter", Map.of(), List.of(), Map.of(), Map.of("vondsten", List.of("V4"))),
                        instance("V1", "Vondst", Map.of("jaartarief", JAARTARIEF), List.of(),
                                Map.of("bewaard", List.of(period("2024-01-01", "2024-04-01"))), Map.of()),
                        instance("V2", "Vondst", Map.of("tarief", timeline(List.of(
                                segment("2024-01-01", "2024-03-01", "5")))), List.of(), Map.of(), Map.of()),
                        instance("V3", "Vondst", Map.of("jaartarief", JAARTARIEF), List.of(), Map.of(), Map.of()),
                        instance("V4", "Vondst", Map.of(), List.of(),
                                Map.of("bewaard", List.of(period("2024-05-01", "2024-06-01"))), Map.of())),
                List.of(expectedTimeline("V1", "tarief", months(2024, 1, 3, "12")),
                        expectedTimeline("V2", "tarief", List.of(segment("2024-01-01", "2024-03-01", "5"))),
                        expectedTimeline("V3", "tarief", List.of()), expectedTimeline("V4", "tarief", List.of()),
                        waarde("J1", "last", "46"), waarde("J1", "som", "46"),
                        waarde("J2", "last", "0"), waarde("J2", "som", null),
                        waarde("J3", "last", "0"), waarde("J3", "som", null), consistent()));
    }

    @Test void a_total_and_a_sum_per_member_over_a_derived_timeline_in_every_order_of_the_rules()
            throws Exception {
        JsonNode caseDocument = caseDocument();
        List<String> outcomes = new ArrayList<>(), verwacht = new ArrayList<>();
        for (List<String> order : TmsDocuments.permutations(RULES)) {
            List<String> names = TmsDocuments.ruleNames(order);
            Path ir = TmsTimelineDocuments.document(MODEL, order, List.of());
            outcomes.add(names + " " + TmsFaultWitnessTest.play(ir, caseDocument, DroolsBackend.Branch.EXEC_MODEL));
            verwacht.add(names + " gespeeld");
        }
        assertEquals(verwacht, outcomes);
    }

    @Test void at_the_fixpoint_at_most_one_value_per_target_and_the_last_intermediate_without_the_derived_rate()
            throws Exception {
        Path ir = TmsTimelineDocuments.document(MODEL, RULES, List.of());
        DroolsBackend.Build b = DroolsBackend.build(ir, DroolsBackend.Branch.EXEC_MODEL);
        int intermediateState = 0;
        for (List<Rule> order : TmsDocuments.sample(b.execModelTranspiler().model().getRules(), 20261009L, 100)) {
            List<String> names = order.stream().map(Rule::getName).toList();
            ClassLoader l = b.beanLoader();
            Object j1 = TmsDocuments.beanObject(l, "Jutter", Map.of(), List.of());
            Object j2 = TmsDocuments.beanObject(l, "Jutter", Map.of(), List.of());
            Object v1 = TmsDocuments.beanObject(l, "Vondst", Map.of(), List.of());
            Object v2 = TmsDocuments.beanObject(l, "Vondst", Map.of(), List.of());
            Object v3 = TmsDocuments.beanObject(l, "Vondst", Map.of(), List.of());
            TmsTimelineDocuments.set(v1, "jaartarief", List.of(
                    TmsTimelineDocuments.beanSegment(l, "2024-01-01", "2025-01-01", "12")));
            TmsTimelineDocuments.set(v1, "bewaard", List.of(
                    TmsTimelineDocuments.beanSegment(l, "2024-01-01", "2024-04-01", null)));
            TmsTimelineDocuments.set(v2, "tarief", List.of(
                    TmsTimelineDocuments.beanSegment(l, "2024-01-01", "2024-03-01", "5")));
            TmsTimelineDocuments.set(v3, "jaartarief", List.of(
                    TmsTimelineDocuments.beanSegment(l, "2024-01-01", "2025-01-01", "12")));
            for (Object v : List.of(v1, v2, v3)) keep(v, j1);
            Map<Object, String> labels = new IdentityHashMap<>(Map.of(j1, "J1", j2, "J2", v1, "V1", v2, "V2", v3, "V3"));
            int[] intermediate = {0};
            Map<String, Integer> waarden = new TreeMap<>();
            List<String> states = new ArrayList<>();
            TmsDocuments.vuur(b, order, List.of(j1, j2, v1, v2, v3), sessie -> sessie.addEventListener(
                    new DefaultRuleRuntimeEventListener() {
                        @Override public void objectInserted(ObjectInsertedEvent e) {
                            if (e.getObject() instanceof Value w && w.onderwerp() == j1
                                    && w.attribuut().naam().equals("last")
                                    && ((BigDecimal) w.waarde()).compareTo(new BigDecimal("10")) == 0) intermediate[0]++;
                        }
                    }), sessie -> {
                for (Object f : sessie.getObjects(o -> o instanceof Value))
                    waarden.merge(labels.get(((Value) f).onderwerp()) + "." + ((Value) f).attribuut().naam(), 1,
                            Integer::sum);
                for (Object f : sessie.getObjects(o -> o instanceof State))
                    states.add(labels.get(((State) f).onderwerp()) + "." + ((State) f).attribuut().naam());
            });
            assertEquals(Map.of("J1.last", 1, "J1.som", 1, "J2.last", 1, "V1.tarief", 1), waarden,
                    () -> "waarden in volgorde " + names);
            states.sort(null);
            // the jaartarief no form writes stands as well: every attribute on a timeline a rule reads has a standing,
            // which carries its input in the convention the case gives it (RUNTIME-SEMANTICS ch. 8.4)
            assertEquals(List.of("V1.jaartarief", "V1.tarief", "V2.jaartarief", "V2.tarief", "V3.jaartarief",
                    "V3.tarief"), states, () -> "standen in volgorde " + names);
            assertEquals(0, new BigDecimal("46").compareTo((BigDecimal) TmsDocuments.lees(j1, "last")),
                    () -> "last van J1 in volgorde " + names);
            assertEquals(0, new BigDecimal("46").compareTo((BigDecimal) TmsDocuments.lees(j1, "som")),
                    () -> "som van J1 in volgorde " + names);
            assertEquals(0, BigDecimal.ZERO.compareTo((BigDecimal) TmsDocuments.lees(j2, "last")),
                    () -> "last van J2 in volgorde " + names);
            assertNull(TmsDocuments.lees(j2, "som"), () -> "som van J2 in volgorde " + names);
            assertEquals(List.of("[2024-01-01, 2024-02-01)=12", "[2024-02-01, 2024-03-01)=12",
                    "[2024-03-01, 2024-04-01)=12"), TmsTimelineWitnessTest.timelineOf(v1, "tarief"),
                    () -> "tarief van V1 in volgorde " + names);
            assertEquals(List.of("[2024-01-01, 2024-03-01)=5"), TmsTimelineWitnessTest.timelineOf(v2, "tarief"),
                    () -> "tarief van V2 in volgorde " + names);
            assertNull(TmsDocuments.lees(v3, "tarief"), () -> "tarief van V3 in volgorde " + names);
            if (intermediate[0] > 0) intermediateState++;
        }
        assertTrue(intermediateState > 0, "geen volgorde gaf J1 onderweg de last 10, zonder het tarief van V1");
    }

    /** Links the Vondst bean {@code vondst} to the Jutter bean {@code jutter}, as the case runner writes the role
     *  "vondsten" of a Jutter: on the to-one field of the Vondst. */
    private static void keep(Object vondst, Object jutter) throws Exception {
        for (java.lang.reflect.Method m : vondst.getClass().getMethods())
            if (m.getName().equals(Namen.setter("jutter")) && m.getParameterCount() == 1) {
                m.invoke(vondst, jutter);
                return;
            }
        throw new AssertionError("geen setter voor jutter op " + vondst.getClass());
    }

    /** Writes the document in every order of its rules, with its case, under {@code args[0]}: a map per order holding
     *  {@code ir.json} and {@code case.json}, for the verifier, ref and clingo. */
    public static void main(String[] args) throws Exception {
        Path out = Path.of(args[0]);
        Map<String, Object[]> parts = new LinkedHashMap<>();
        parts.put("vouw-per-lid", new Object[]{MODEL, RULES, caseDocument()});
        for (Map.Entry<String, Object[]> d : parts.entrySet()) {
            @SuppressWarnings("unchecked") List<String> rules = (List<String>) d.getValue()[1];
            int n = 0;
            for (List<String> order : TmsDocuments.permutations(rules))
                TmsTimelineWitnessTest.write(out.resolve(d.getKey()).resolve(String.format("v%03d", n++)),
                        TmsTimelineDocuments.ir((String) d.getValue()[0], order, List.of()),
                        (JsonNode) d.getValue()[2]);
        }
    }
}
