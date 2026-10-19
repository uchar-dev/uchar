package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;

import org.drools.model.Rule;
import org.junit.jupiter.api.Test;
import org.kie.api.event.rule.DefaultRuleRuntimeEventListener;
import org.kie.api.event.rule.ObjectInsertedEvent;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static dev.uchar.regelspraak.cases.TmsDocuments.check;
import static dev.uchar.regelspraak.cases.TmsDocuments.number;
import static dev.uchar.regelspraak.cases.TmsDocuments.numberValue;
import static dev.uchar.regelspraak.cases.TmsDocuments.inconsistent;
import static dev.uchar.regelspraak.cases.TmsDocuments.characteristicRule;
import static dev.uchar.regelspraak.cases.TmsDocuments.ownRead;
import static dev.uchar.regelspraak.cases.TmsDocuments.computingRule;
import static dev.uchar.regelspraak.cases.TmsDocuments.comparison;
import static dev.uchar.regelspraak.cases.TmsTimelineDocuments.WHOLE;
import static dev.uchar.regelspraak.cases.TmsTimelineDocuments.dayCount;
import static dev.uchar.regelspraak.cases.TmsTimelineDocuments.instance;
import static dev.uchar.regelspraak.cases.TmsTimelineDocuments.characteristic;
import static dev.uchar.regelspraak.cases.TmsTimelineDocuments.objectType;
import static dev.uchar.regelspraak.cases.TmsTimelineDocuments.period;
import static dev.uchar.regelspraak.cases.TmsTimelineDocuments.segment;
import static dev.uchar.regelspraak.cases.TmsTimelineDocuments.timeAttribute;
import static dev.uchar.regelspraak.cases.TmsTimelineDocuments.timeCharacteristic;
import static dev.uchar.regelspraak.cases.TmsTimelineDocuments.timeline;
import static dev.uchar.regelspraak.cases.TmsTimelineDocuments.expectedTimeline;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Uniqueness over a key on a timeline on the exec-model branch, ever-uniqueness (RUNTIME-SEMANTICS ch. 5.9, rule
 * 1): every value the key's timeline ever carries is claimed, so two instances collide where they carried one value
 * in different periods, and an instance that carries one value in two periods claims it once and does not collide
 * with itself. The key is a timeline a rule derives, behind a gate another rule derives, so it stands only at the
 * fixpoint.
 *
 * <p>The model: "Een Vondst is geregistreerd indien zijn code groter is dan 0"; "Het nummer van een Vondst moet
 * berekend worden als het aantal dagen in het jaar dat hij bewaard is, indien hij geregistreerd is", a day count onto
 * a target on the year grid that only this rule writes, so it stands where the count is empty or the rule does not
 * apply (ch. 7.8); and "De nummers van alle Vondsten moeten uniek zijn" (eenmalig nummer). By hand, with code 1
 * unless said otherwise: V1, bewaard on [2024-01-01, 2024-01-11), gets nummer 10 in 2024; V2, on [2025-03-01,
 * 2025-03-11), 10 in 2025; V3, on [2024-01-01, 2024-01-06), 5 in 2024; V4, nummer 5 in 2024 and 7 in 2025 as input
 * and no true period, keeps its input; V5, nummer 3 in 2024 and 3 in 2025 as input, keeps it and claims 3 once; V6,
 * on [2026-01-01, 2026-01-08) over the input 10 in 2023, gets 7 in 2026, which replaces the input whole; V7, on
 * [2027-01-01, 2027-01-03) over the input 3 in 2023, gets 2 in 2027; V8, code 0 and so not geregistreerd, on
 * [2024-01-01, 2024-01-11) over the input 4 in 2028, keeps its input. So 10 is claimed by V1 and V2, 5 by V3 and V4,
 * 7 by V4 and V6, and 3, 2 and 4 once each: eenmalig nummer is inconsistent at V1, V2, V3, V4 and V6, and only there.
 * Both orders of the two rules; beside them the generated rules in a hundred sampled orders, where the channel at the
 * fixpoint is exactly those five, and in some order V5 was marked on the way, while V7 still held its input 3.
 *
 * <p><b>Measured</b> (2026-10-09, the document of {@link #main} in both orders): the verifier passes both, and the
 * reference interpreter's runner and the ASP runner pass the case in both, "PASS (13 expectations)". An expectation
 * "inconsistent" asks only that the channel carries the fact, so beside the case three probes add one that V5, V7 or
 * V8 is marked: both runners answer MISMATCH on each, the channel carrying exactly "eenmalig nummer × V1; … × V2; …
 * × V3; … × V4; … × V6".
 *
 * <p><b>Red</b> on b9f77aa7, the code before the change: both orders, "but was: &lt;[[registratie, nummer] Excluded:
 * tms-transpiler SKIP: regel: nummer — het tijdsafhankelijke attribuut 'nummer' van Vondst: tijdlijnen zijn buiten de
 * proef van de truth-maintenance-tak, …&gt;", and the build of the sampled orders on the same refusal; the
 * uniqueness itself was refused there in the same words ("uniciteit: eenmalig nummer — het tijdsafhankelijke
 * attribuut 'nummer' van Vondst: …", every refusal of the document listed from that build's manifest). <b>Red by
 * mutation</b> (not committed): with the key rule claiming only the least value of a timeline, both orders fail,
 * "case: verwacht inconsistent (eenmalig nummer @ V6), niet gevonden onder …", and the sampled orders find the
 * channel without V6.
 */
class TmsEverUniquenessWitnessTest {

    static final String MODEL = TmsDocuments.model(List.of(objectType("Vondst", List.of(
                    TmsDocuments.attribute("code", WHOLE), timeAttribute("nummer", WHOLE, "year")),
            List.of(timeCharacteristic("bewaard", "month"), characteristic("geregistreerd")))), List.of());

    static final String REGISTRATIE = characteristicRule("registratie", "Vondst", "geregistreerd",
            comparison("greater", ownRead("code", "Vondst"), number("0")));
    static final String NUMBER = computingRule("nummer", "BEREKENING", "Vondst", "nummer",
            dayCount(check("bewaard", false), "year"), check("geregistreerd", false));
    static final String EENMALIG = TmsUniquenessWitnessTest.uniqueness("eenmalig nummer",
            List.of(TmsUniquenessWitnessTest.source("Vondst", List.of("nummer"))), null,
            "De nummers van alle Vondsten moeten uniek zijn.");

    static final List<String> RULES = List.of(REGISTRATIE, NUMBER);

    /** The year {@code year} as a segment with the number {@code lexeme}. */
    static String year(int year, String lexeme) {
        return segment(year + "-01-01", (year + 1) + "-01-01", lexeme);
    }

    /** A Vondst of the case with the code {@code code}, bewaard on {@code [van, tot)} where {@code van} is not null, and
     *  the nummer {@code inputData} as input where it is not empty. */
    static String vondst(String label, String code, String van, String to, List<String> inputData) {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("code", numberValue(code));
        if (!inputData.isEmpty()) values.put("nummer", timeline(inputData));
        return instance(label, "Vondst", values, List.of(),
                van == null ? Map.of() : Map.of("bewaard", List.of(period(van, to))), Map.of());
    }

    static JsonNode caseDocument() throws Exception {
        return TmsTimelineDocuments.caseDocument(null, List.of(
                        vondst("V1", "1", "2024-01-01", "2024-01-11", List.of()),
                        vondst("V2", "1", "2025-03-01", "2025-03-11", List.of()),
                        vondst("V3", "1", "2024-01-01", "2024-01-06", List.of()),
                        vondst("V4", "1", null, null, List.of(year(2024, "5"), year(2025, "7"))),
                        vondst("V5", "1", null, null, List.of(year(2024, "3"), year(2025, "3"))),
                        vondst("V6", "1", "2026-01-01", "2026-01-08", List.of(year(2023, "10"))),
                        vondst("V7", "1", "2027-01-01", "2027-01-03", List.of(year(2023, "3"))),
                        vondst("V8", "0", "2024-01-01", "2024-01-11", List.of(year(2028, "4")))),
                List.of(expectedTimeline("V1", "nummer", List.of(year(2024, "10"))),
                        expectedTimeline("V2", "nummer", List.of(year(2025, "10"))),
                        expectedTimeline("V3", "nummer", List.of(year(2024, "5"))),
                        expectedTimeline("V4", "nummer", List.of(year(2024, "5"), year(2025, "7"))),
                        expectedTimeline("V5", "nummer", List.of(year(2024, "3"), year(2025, "3"))),
                        expectedTimeline("V6", "nummer", List.of(year(2026, "7"))),
                        expectedTimeline("V7", "nummer", List.of(year(2027, "2"))),
                        expectedTimeline("V8", "nummer", List.of(year(2028, "4"))),
                        inconsistent("eenmalig nummer", "V1"), inconsistent("eenmalig nummer", "V2"),
                        inconsistent("eenmalig nummer", "V3"), inconsistent("eenmalig nummer", "V4"),
                        inconsistent("eenmalig nummer", "V6")));
    }

    @Test void a_value_that_ever_stood_on_the_timeline_collides_in_both_orders_of_the_rules() throws Exception {
        JsonNode caseDocument = caseDocument();
        List<String> outcomes = new ArrayList<>(), expected = new ArrayList<>();
        for (List<String> order : TmsDocuments.permutations(RULES)) {
            List<String> names = TmsDocuments.ruleNames(order);
            Path ir = TmsTimelineDocuments.document(MODEL, order, List.of(EENMALIG));
            outcomes.add(names + " " + TmsFaultWitnessTest.play(ir, caseDocument, DroolsBackend.Branch.EXEC_MODEL));
            expected.add(names + " gespeeld");
        }
        assertEquals(expected, outcomes);
    }

    @Test void the_channel_holds_at_the_fixpoint_exactly_the_collisions_of_the_derived_keys() throws Exception {
        Path ir = TmsTimelineDocuments.document(MODEL, RULES, List.of(EENMALIG));
        DroolsBackend.Build b = DroolsBackend.build(ir, DroolsBackend.Branch.EXEC_MODEL);
        int intermediateState = 0;
        for (List<Rule> order : TmsDocuments.sample(b.execModelTranspiler().model().getRules(), 20261009L, 100)) {
            List<String> names = order.stream().map(Rule::getName).toList();
            ClassLoader l = b.beanLoader();
            Map<Object, String> labels = new IdentityHashMap<>();
            List<Object> beans = new ArrayList<>();
            Object[][] input = {
                    {"V1", "1", "2024-01-01", "2024-01-11", new String[0]},
                    {"V2", "1", "2025-03-01", "2025-03-11", new String[0]},
                    {"V3", "1", "2024-01-01", "2024-01-06", new String[0]},
                    {"V4", "1", null, null, new String[]{"2024", "5", "2025", "7"}},
                    {"V5", "1", null, null, new String[]{"2024", "3", "2025", "3"}},
                    {"V6", "1", "2026-01-01", "2026-01-08", new String[]{"2023", "10"}},
                    {"V7", "1", "2027-01-01", "2027-01-03", new String[]{"2023", "3"}},
                    {"V8", "0", "2024-01-01", "2024-01-11", new String[]{"2028", "4"}}};
            for (Object[] o : input) {
                Object v = TmsDocuments.beanObject(l, "Vondst", Map.of("code", (String) o[1]), List.of());
                if (o[2] != null) TmsTimelineDocuments.set(v, "bewaard",
                        List.of(TmsTimelineDocuments.beanSegment(l, (String) o[2], (String) o[3], null)));
                String[] inputData = (String[]) o[4];
                List<Object> segments = new ArrayList<>();
                for (int i = 0; i < inputData.length; i += 2) {
                    int j = Integer.parseInt(inputData[i]);
                    segments.add(TmsTimelineDocuments.beanSegment(l, j + "-01-01", (j + 1) + "-01-01", inputData[i + 1]));
                }
                if (!segments.isEmpty()) TmsTimelineDocuments.set(v, "nummer", segments);
                labels.put(v, (String) o[0]);
                beans.add(v);
            }
            Object v5 = beans.get(4);
            int[] intermediate = {0};
            List<String> channel = new ArrayList<>();
            TmsDocuments.fire(b, order, beans, session -> session.addEventListener(
                    new DefaultRuleRuntimeEventListener() {
                        @Override public void objectInserted(ObjectInsertedEvent e) {
                            Object f = e.getObject();
                            if (f.getClass().getSimpleName().equals("Inconsistentie")) {
                                try {
                                    if (f.getClass().getMethod("getSubject").invoke(f) == v5) intermediate[0]++;
                                } catch (ReflectiveOperationException x) {
                                    throw new IllegalStateException(x);
                                }
                            }
                        }
                    }), session -> channel.addAll(TmsDocuments.inconsistencies(session, labels)));
            assertEquals(List.of("eenmalig nummer @ V1", "eenmalig nummer @ V2", "eenmalig nummer @ V3",
                    "eenmalig nummer @ V4", "eenmalig nummer @ V6"), channel, () -> "volgorde " + names);
            assertEquals(List.of("[2027-01-01, 2028-01-01)=2"), TmsTimelineWitnessTest.timelineOf(beans.get(6), "nummer"),
                    () -> "nummer van V7 in volgorde " + names);
            assertEquals(List.of("[2028-01-01, 2029-01-01)=4"), TmsTimelineWitnessTest.timelineOf(beans.get(7), "nummer"),
                    () -> "nummer van V8 in volgorde " + names);
            if (intermediate[0] > 0) intermediateState++;
        }
        assertTrue(intermediateState > 0, "geen volgorde liet V5 onderweg met de invoer van V7 botsen");
    }

    /** Writes the document in both orders of its rules, with its case, under {@code args[0]}: a map per order holding
     *  {@code ir.json} and {@code case.json}, for the verifier, ref and clingo. */
    public static void main(String[] args) throws Exception {
        Path out = Path.of(args[0]);
        int n = 0;
        for (List<String> order : TmsDocuments.permutations(RULES))
            TmsTimelineWitnessTest.write(out.resolve("ooit-uniciteit").resolve(String.format("v%03d", n++)),
                    TmsTimelineDocuments.ir(MODEL, order, List.of(EENMALIG)), caseDocument());
    }
}
