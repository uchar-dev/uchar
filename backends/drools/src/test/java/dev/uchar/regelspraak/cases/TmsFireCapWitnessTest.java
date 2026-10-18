package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;

import dev.uchar.regelspraak.transpiler.tms.CreationPhase;

import org.junit.jupiter.api.Test;
import org.kie.api.KieBase;
import org.kie.api.definition.KiePackage;
import org.kie.api.event.rule.DefaultRuleRuntimeEventListener;
import org.kie.api.event.rule.ObjectInsertedEvent;
import org.kie.api.runtime.KieSession;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The fire cap of the exec-model branch grows with the input (card tms-omschakeling, point 10): {@code max(10 000,
 * FACTOR × R × N0)}, with R the rules of the KieBase and N0 the facts just before the fire, computed again at the start
 * of every creation phase. The DRL-text branch kept a fixed cap of 100 000 until card
 * drl-teksttak-op-truth-maintenance, which gave it the same cap.
 *
 * <p>The two rows the fix round of step 3 sharpened (point 3), so that each tells the computation it claims from a
 * near one. Through the public {@code vuur}, the values document of {@link TmsRepeatedFireWitnessTest} with J1 and
 * 600 Vondsten puts FACTOR × R × N0 above the floor, 4 × 10 × 601 = 24 040, and the cap is that product and not the
 * floor. On the golden {@code jutterswet-dubbelverbinding}, case 1, through the seam with FACTOR and floor 1, the fact
 * creations run four phases or more, and a later phase holds more facts at its start than the first (2026-10-10: N0 = 7,
 * 28 at the first phase, 40 at the fourth); the cap at the end is FACTOR × R × N for the N of that later phase, 4 × 19
 * × 40 = 3 040, so the witness tells a cap computed again at every phase from one computed once after the first fire.
 * With an artificially small factor (0.01, floor 1) the values document stops at the cap, with a sentence that names
 * the cap, R and N0.
 *
 * <p><b>Red before the change</b> (measured 2026-10-10 on {@code 095a586a}): the witness does not compile there, since
 * {@code Firing} had no {@code plafond} and {@code vuur} neither a public form nor a factor; the cap was the fixed
 * 100 000 the caller passed, and at the cap {@code noFixpoint} was null. <b>Red by mutation</b> (measured 2026-10-10
 * on the code of the fix round, the mutants not committed): with the floor alone the public row failed on "the
 * public cap is FACTOR × R × N0, not the floor ==&gt; expected: &lt;24040&gt; but was: &lt;10000&gt;"; with the cap
 * computed again at the first phase only, the row of the phases failed on "the cap is FACTOR × R × N for the N of phase
 * 4, 4 × 19 × 40, not that of the first phase, 28, nor N0 = 7 ==&gt; expected: &lt;3040&gt; but was: &lt;2128&gt;";
 * and with no computation per phase, the mutant of step 3, on the same sentence with "but was: &lt;532&gt;". Until the
 * fix round the public row asserted the formula over three beans, below the floor, and the row of the phases only that
 * the cap counted more facts than N0, which a cap computed once after the first fire also does.
 */
class TmsFireCapWitnessTest {

    @Test void the_public_cap_is_the_product_where_that_lies_above_the_floor() throws Exception {
        DroolsBackend.Build b = DroolsBackend.build(TmsRepeatedFireWitnessTest.valueDocument(),
                DroolsBackend.Branch.EXEC_MODEL);
        TmsRepeatedFireWitnessTest.inSession(b, () -> {
            KieSession session = filled(b, VONDSTEN);
            try {
                long r = rules(b.kieBase()), n0 = session.getFactCount();
                long product = DroolsBackend.FACTOR * r * n0;
                assertTrue(product > DroolsBackend.FLOOR, () -> "FACTOR × R × N0 = " + DroolsBackend.FACTOR + " × " + r
                        + " × " + n0 + " lies above the floor " + DroolsBackend.FLOOR);
                DroolsBackend.Firing firing = DroolsBackend.fire(b, session);
                assertNull(firing.noFixpoint(), "the fixpoint under the public cap");
                assertEquals(product, firing.cap(), "the public cap is FACTOR × R × N0, not the floor");
            } finally {
                session.dispose();
            }
        });
    }

    @Test void a_small_factor_stops_the_run_on_the_cap_with_r_and_n0_in_the_sentence() throws Exception {
        DroolsBackend.Build b = DroolsBackend.build(TmsRepeatedFireWitnessTest.valueDocument(),
                DroolsBackend.Branch.EXEC_MODEL);
        TmsRepeatedFireWitnessTest.inSession(b, () -> {
            KieSession session = filled(b);
            try {
                long r = rules(b.kieBase()), n0 = session.getFactCount();
                int cap = (int) Math.max(1, Math.ceil(0.01 * r * n0));
                DroolsBackend.Firing firing = DroolsBackend.fire(b, session, 0.01, 1);
                assertEquals(cap, firing.cap(), "max(1, 0.01 × R × N0), rounded up");
                assertEquals(cap, firing.firings(), "the run stops at the cap");
                assertEquals("geen fixpunt: het vuurplafond van " + cap + " vuringen is bereikt, het grootste van 1"
                        + " en 0.01 × " + r + " regels × " + n0 + " feiten vlak vóór het vuren; zonder kring vuurt de tak"
                        + " per regel en feit een begrensd aantal keren, dus dit is een fout van de verlaging",
                        firing.noFixpoint());
            } finally {
                session.dispose();
            }
        });
    }

    @Test void a_later_creation_phase_with_more_facts_sets_the_cap() throws Exception {
        Path ir = Path.of("..", "..", "corpus", "golden", "jutterswet-dubbelverbinding", "ir.json");
        JsonNode caseDocument = TmsDocuments.JSON.readTree(Files.readString(ir.resolveSibling("case1-binnen-venster.json")));
        CaseRunner runner = CaseRunner.forMeasurement(ir, DroolsBackend.Branch.EXEC_MODEL);
        runner.withSession(caseDocument, (build, session, labels) -> {
            long r = rules(build.kieBase()), n0 = session.getFactCount();
            // the facts at the start of each phase: the driver computes the cap right after it inserts the phase
            List<Long> phases = new ArrayList<>();
            session.addEventListener(new DefaultRuleRuntimeEventListener() {
                @Override public void objectInserted(ObjectInsertedEvent e) {
                    if (e.getObject() instanceof CreationPhase) phases.add(session.getFactCount());
                }
            });
            DroolsBackend.Firing firing = DroolsBackend.fire(build, session, DroolsBackend.FACTOR, 1);
            assertNull(firing.noFixpoint(), "the fixpoint");
            assertTrue(phases.size() >= 2, () -> "two phases or more: " + phases);
            long largest = Math.max(n0, phases.stream().mapToLong(Long::longValue).max().orElseThrow());
            int where = phases.indexOf(largest);
            assertTrue(where >= 1 && largest > phases.get(0), () -> "a later phase holds more facts than the first, and"
                    + " the most: N0 = " + n0 + ", per phase " + phases);
            assertEquals(DroolsBackend.FACTOR * r * largest, firing.cap(), () -> "the cap is FACTOR × R × N for"
                    + " the N of phase " + (where + 1) + ", " + DroolsBackend.FACTOR + " × " + r + " × " + largest
                    + ", not that of the first phase, " + phases.get(0) + ", nor N0 = " + n0);
            return null;
        });
    }

    /** The Vondsten of the public run: enough that FACTOR × R × N0 lies above the floor. */
    private static final int VONDSTEN = 600;

    /** A session of {@code b} with J1, V1 (gewicht 10) and V2 (gewicht 4) of the values document put in. */
    private static KieSession filled(DroolsBackend.Build b) throws Exception {
        return filled(b, 2);
    }

    /** A session of {@code b} with J1 and {@code vondsten} Vondsten of the values document put in, each found by J1,
     *  the first of gewicht 10 and every other of gewicht 4. */
    private static KieSession filled(DroolsBackend.Build b, int vondsten) throws Exception {
        Object j1 = TmsDocuments.beanObject(b.beanLoader(), "Jutter", Map.of(), List.of());
        List<Object> beans = new ArrayList<>(List.of(j1));
        for (int i = 0; i < vondsten; i++) {
            Object v = TmsDocuments.beanObject(b.beanLoader(), "Vondst", Map.of("gewicht", i == 0 ? "10" : "4"), List.of());
            TmsRepeatedFireWitnessTest.setRole(v, "vinder", j1);
            beans.add(v);
        }
        KieSession session = b.kieBase().newKieSession();
        DroolsBackend.insert(b, session, new Input().beans(beans));
        return session;
    }

    /** The rules of {@code kieBase}, the R of the cap. */
    private static long rules(KieBase kieBase) {
        long n = 0;
        for (KiePackage p : kieBase.getKiePackages()) n += p.getRules().size();
        return n;
    }
}
