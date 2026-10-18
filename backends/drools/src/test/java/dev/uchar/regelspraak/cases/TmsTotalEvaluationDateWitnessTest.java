package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;

import org.drools.model.Rule;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static dev.uchar.regelspraak.cases.TmsDocuments.check;
import static dev.uchar.regelspraak.cases.TmsDocuments.consistent;
import static dev.uchar.regelspraak.cases.TmsDocuments.number;
import static dev.uchar.regelspraak.cases.TmsDocuments.numberValue;
import static dev.uchar.regelspraak.cases.TmsDocuments.reading;
import static dev.uchar.regelspraak.cases.TmsDocuments.computingRule;
import static dev.uchar.regelspraak.cases.TmsDocuments.comparison;
import static dev.uchar.regelspraak.cases.TmsDocuments.waarde;
import static dev.uchar.regelspraak.cases.TmsTimelineDocuments.WHOLE;
import static dev.uchar.regelspraak.cases.TmsTimelineDocuments.ownTotal;
import static dev.uchar.regelspraak.cases.TmsTimelineDocuments.instance;
import static dev.uchar.regelspraak.cases.TmsTimelineDocuments.kenmerk;
import static dev.uchar.regelspraak.cases.TmsTimelineDocuments.objecttype;
import static dev.uchar.regelspraak.cases.TmsTimelineDocuments.timeAttribute;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The subject's own temporal total without a clause in a session without an evaluation date (RUNTIME-SEMANTICS ch. 3.6,
 * 8.4): that route takes the asOfDate-bucket scalar, the session's date being its horizon, so without a date it has no
 * bucket to take, and the reference interpreter faults where it evaluates the total, whether the attribute holds a value
 * or not: "the temporal total of "bedrag" needs the evaluation date: the case carries the asOfDate bucket's value". On
 * this branch the fault is a {@code Fout} that counts only where it stands at the fixpoint, like every evaluation fault.
 *
 * <p>The document: "Een Jutter is vrijgesteld indien zijn score kleiner is dan 5" and "De topsom van een Jutter moet
 * berekend worden als het totaal van zijn bedrag, indien hij niet vrijgesteld is", bedrag on a month grid. In a session
 * without a date: J1 (score 3) is vrijgesteld, so the total is not evaluated at the fixpoint and the session plays,
 * topsom leeg; where the topsom's rule fires before the vrijstelling, it evaluates the total on that intermediate state
 * and faults, and the fault goes with the state. J2 (score 10) is not vrijgesteld, and the session faults. With the
 * evaluation date 2024-06-15: J2 with bedrag 7 as the bucket scalar of June 2024 totals 7, J3 (score 10) without a
 * bedrag leeg, and J1, with the same bedrag, stays leeg. Both orders of the two rules; beside them the generated rules in
 * a hundred sampled orders on a J1 of its own, in some of which the fault was inserted and withdrawn.
 *
 * <p><b>Measured</b> (2026-10-09, the documents {@link #main} writes, both orders): the verifier passes them. The
 * reference interpreter's runner passes the session with J1 (2 expectations) and the one with a date (4), and faults on
 * J2 in the sentence above. The ASP runner passes the session with a date and refuses both sessions without one at
 * translation, in the same sentence, also the one with J1, whose total no evaluation reaches: it decides before any
 * condition is read.
 *
 * <p><b>Red</b> on 0e879163, the code before the change: the session with J2 played, topsom leeg, "[vrijstelling,
 * topsom] niet vrijgesteld gespeeld" in both orders, and no sampled order inserted a fault ("but was: &lt;false&gt;").
 */
class TmsTotalEvaluationDateWitnessTest {

    static final String MODEL = TmsDocuments.model(List.of(objecttype("Jutter", List.of(
            TmsDocuments.attribuut("score", WHOLE), timeAttribute("bedrag", WHOLE, "month"),
            TmsDocuments.attribuut("topsom", WHOLE)), List.of(kenmerk("vrijgesteld")))), List.of());

    static final String VRIJSTELLING = TmsDocuments.characteristicRule("vrijstelling", "Jutter", "vrijgesteld",
            comparison("less", reading("score", "Jutter"), number("5")));
    static final String TOPSOM = computingRule("topsom", "BEREKENING", "Jutter", "topsom", ownTotal("bedrag", "Jutter",
            "null"), check("vrijgesteld", true));

    static final List<String> RULES = List.of(VRIJSTELLING, TOPSOM);

    static final String WITHOUT_DATE = "Rekenfout: fout in de regel \"topsom\": the temporal total of \"bedrag\" needs"
            + " the evaluation date: the case carries the asOfDate bucket's value";

    static JsonNode vrijgesteld() throws Exception {
        return TmsTimelineDocuments.caseDocument(null, List.of(instance("J1", "Jutter", Map.of("score", numberValue("3")),
                List.of(), Map.of(), Map.of())), List.of(waarde("J1", "topsom", null), consistent()));
    }

    static JsonNode notVrijgesteld() throws Exception {
        return TmsTimelineDocuments.caseDocument(null, List.of(instance("J2", "Jutter", Map.of("score", numberValue("10")),
                List.of(), Map.of(), Map.of())), List.of(consistent()));
    }

    static JsonNode withDate() throws Exception {
        return TmsTimelineDocuments.caseDocument("2024-06-15", List.of(
                        instance("J1", "Jutter", Map.of("score", numberValue("3"), "bedrag", numberValue("7")), List.of(),
                                Map.of(), Map.of()),
                        instance("J2", "Jutter", Map.of("score", numberValue("10"), "bedrag", numberValue("7")), List.of(),
                                Map.of(), Map.of()),
                        instance("J3", "Jutter", Map.of("score", numberValue("10")), List.of(), Map.of(), Map.of())),
                List.of(waarde("J1", "topsom", null), waarde("J2", "topsom", "7"), waarde("J3", "topsom", null),
                        consistent()));
    }

    @Test void the_total_without_clip_fails_without_evaluation_date_where_the_evaluation_reaches_it() throws Exception {
        List<String> outcomes = new ArrayList<>(), verwacht = new ArrayList<>();
        for (List<String> order : TmsDocuments.permutations(RULES)) {
            List<String> names = TmsDocuments.ruleNames(order);
            Path ir = TmsTimelineDocuments.document(MODEL, order, List.of());
            outcomes.add(names + " vrijgesteld " + TmsFaultWitnessTest.play(ir, vrijgesteld(),
                    DroolsBackend.Branch.EXEC_MODEL));
            outcomes.add(names + " niet vrijgesteld " + TmsFaultWitnessTest.play(ir, notVrijgesteld(),
                    DroolsBackend.Branch.EXEC_MODEL));
            outcomes.add(names + " met datum " + TmsFaultWitnessTest.play(ir, withDate(),
                    DroolsBackend.Branch.EXEC_MODEL));
            verwacht.add(names + " vrijgesteld gespeeld");
            verwacht.add(names + " niet vrijgesteld " + WITHOUT_DATE);
            verwacht.add(names + " met datum gespeeld");
        }
        assertEquals(verwacht, outcomes);
    }

    @Test void the_fault_of_an_intermediate_state_is_withdrawn() throws Exception {
        Path ir = TmsTimelineDocuments.document(MODEL, RULES, List.of());
        DroolsBackend.Build b = DroolsBackend.build(ir, DroolsBackend.Branch.EXEC_MODEL);
        int intermediateState = 0;
        for (List<Rule> order : TmsDocuments.sample(b.execModelTranspiler().model().getRules(), 20261009L, 100)) {
            List<String> names = order.stream().map(Rule::getName).toList();
            Object j1 = TmsDocuments.beanObject(b.beanLoader(), "Jutter", Map.of("score", "3"), List.of());
            TmsLetWitnessTest.Facts sessionFacts = new TmsLetWitnessTest.Facts();
            assertNull(TmsDocuments.fixpunt(b, order, List.of(j1), sessie -> sessie.addEventListener(sessionFacts)),
                    () -> "volgorde " + names);
            assertEquals(sessionFacts.inserted, sessionFacts.withdrawn, () -> "foutfeiten in volgorde " + names);
            assertNull(TmsDocuments.lees(j1, "topsom"), () -> "topsom in volgorde " + names);
            if (sessionFacts.fouten > 0) intermediateState++;
        }
        assertTrue(intermediateState > 0, "geen volgorde liet het totaal vóór de vrijstelling rekenen");
    }

    /** Writes the document in both orders with its three cases under {@code args[0]}, for the verifier, ref and clingo. */
    public static void main(String[] args) throws Exception {
        Path out = Path.of(args[0]);
        int n = 0;
        for (List<String> order : TmsDocuments.permutations(RULES)) {
            Path map = out.resolve(String.format("v%03d", n++));
            TmsTimelineWitnessTest.write(map, TmsTimelineDocuments.ir(MODEL, order, List.of()), vrijgesteld());
            java.nio.file.Files.move(map.resolve("case.json"), map.resolve("case1-vrijgesteld.json"));
            java.nio.file.Files.writeString(map.resolve("case2-niet-vrijgesteld.json"), TmsDocuments.JSON
                    .writerWithDefaultPrettyPrinter().writeValueAsString(notVrijgesteld()));
            java.nio.file.Files.writeString(map.resolve("case3-met-datum.json"), TmsDocuments.JSON
                    .writerWithDefaultPrettyPrinter().writeValueAsString(withDate()));
        }
    }
}
