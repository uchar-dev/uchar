package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;

import dev.uchar.regelspraak.transpiler.tms.Creatiefase;

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
 * The fire cap of the truth-maintenance branch grows with the input (card tms-omschakeling, point 10): {@code max(10 000,
 * FACTOR × R × N0)}, with R the rules of the KieBase and N0 the facts just before the fire, computed again at the start
 * of every creation phase. The exec-model and the DRL-text branch keep their fixed cap of 100 000.
 *
 * <p>The two rows the fix round of step 3 sharpened (point 3), so that each tells the computation it claims from a
 * near one. Through the public {@code vuur}, the values document of {@link TmsHerhaaldVurenWitnessTest} with J1 and
 * 600 Vondsten puts FACTOR × R × N0 above the floor, 4 × 10 × 601 = 24 040, and the cap is that product and not the
 * floor. On the golden {@code jutterswet-dubbelverbinding}, case 1, through the seam with FACTOR and floor 1, the fact
 * creations run four phases or more, and a later phase holds more facts at its start than the first (2026-10-10: N0 = 7,
 * 28 at the first phase, 40 at the fourth); the cap at the end is FACTOR × R × N for the N of that later phase, 4 × 19
 * × 40 = 3 040, so the witness tells a cap computed again at every phase from one computed once after the first fire.
 * With an artificially small factor (0.01, floor 1) the values document stops at the cap, with a sentence that names
 * the cap, R and N0.
 *
 * <p><b>Red before the change</b> (measured 2026-10-10 on {@code 095a586a}): the witness does not compile there, since
 * {@code Vuring} had no {@code plafond} and {@code vuur} neither a public form nor a factor; the cap was the fixed
 * 100 000 the caller passed, and at the cap {@code geenFixpunt} was null. <b>Red by mutation</b> (measured 2026-10-10
 * on the code of the fix round, the mutants not committed): with the floor alone the public row failed on "the
 * public cap is FACTOR × R × N0, not the floor ==&gt; expected: &lt;24040&gt; but was: &lt;10000&gt;"; with the cap
 * computed again at the first phase only, the row of the phases failed on "the cap is FACTOR × R × N for the N of phase
 * 4, 4 × 19 × 40, not that of the first phase, 28, nor N0 = 7 ==&gt; expected: &lt;3040&gt; but was: &lt;2128&gt;";
 * and with no computation per phase, the mutant of step 3, on the same sentence with "but was: &lt;532&gt;". Until the
 * fix round the public row asserted the formula over three beans, below the floor, and the row of the phases only that
 * the cap counted more facts than N0, which a cap computed once after the first fire also does.
 */
class TmsVuurplafondWitnessTest {

    @Test void het_publieke_plafond_is_het_product_waar_dat_boven_de_vloer_ligt() throws Exception {
        DroolsBackend.Build b = DroolsBackend.build(TmsHerhaaldVurenWitnessTest.waardedocument(),
                DroolsBackend.Branch.TRUTH_MAINTENANCE);
        TmsHerhaaldVurenWitnessTest.inSessie(b, () -> {
            KieSession sessie = gevuld(b, VONDSTEN);
            try {
                long r = regels(b.kieBase()), n0 = sessie.getFactCount();
                long product = DroolsBackend.FACTOR * r * n0;
                assertTrue(product > DroolsBackend.VLOER, () -> "FACTOR × R × N0 = " + DroolsBackend.FACTOR + " × " + r
                        + " × " + n0 + " lies above the floor " + DroolsBackend.VLOER);
                DroolsBackend.Vuring vuring = DroolsBackend.vuur(b, sessie);
                assertNull(vuring.geenFixpunt(), "the fixpoint under the public cap");
                assertEquals(product, vuring.plafond(), "the public cap is FACTOR × R × N0, not the floor");
            } finally {
                sessie.dispose();
            }
        });
    }

    @Test void een_kleine_factor_stopt_de_run_op_het_plafond_met_r_en_n0_in_de_zin() throws Exception {
        DroolsBackend.Build b = DroolsBackend.build(TmsHerhaaldVurenWitnessTest.waardedocument(),
                DroolsBackend.Branch.TRUTH_MAINTENANCE);
        TmsHerhaaldVurenWitnessTest.inSessie(b, () -> {
            KieSession sessie = gevuld(b);
            try {
                long r = regels(b.kieBase()), n0 = sessie.getFactCount();
                int plafond = (int) Math.max(1, Math.ceil(0.01 * r * n0));
                DroolsBackend.Vuring vuring = DroolsBackend.vuur(b, sessie, 0.01, 1);
                assertEquals(plafond, vuring.plafond(), "max(1, 0.01 × R × N0), rounded up");
                assertEquals(plafond, vuring.vuringen(), "the run stops at the cap");
                assertEquals("geen fixpunt: het vuurplafond van " + plafond + " vuringen is bereikt, het grootste van 1"
                        + " en 0.01 × " + r + " regels × " + n0 + " feiten vlak vóór het vuren; zonder kring vuurt de tak"
                        + " per regel en feit een begrensd aantal keren, dus dit is een fout van de verlaging",
                        vuring.geenFixpunt());
            } finally {
                sessie.dispose();
            }
        });
    }

    @Test void een_latere_creatiefase_met_meer_feiten_zet_het_plafond() throws Exception {
        Path ir = Path.of("..", "..", "corpus", "golden", "jutterswet-dubbelverbinding", "ir.json");
        JsonNode zaak = TmsDocumenten.JSON.readTree(Files.readString(ir.resolveSibling("case1-binnen-venster.json")));
        CaseRunner runner = CaseRunner.voorMeting(ir, DroolsBackend.Branch.TRUTH_MAINTENANCE);
        runner.metSessie(zaak, (bouw, sessie, labels) -> {
            long r = regels(bouw.kieBase()), n0 = sessie.getFactCount();
            // the facts at the start of each phase: the driver computes the cap right after it inserts the phase
            List<Long> fasen = new ArrayList<>();
            sessie.addEventListener(new DefaultRuleRuntimeEventListener() {
                @Override public void objectInserted(ObjectInsertedEvent e) {
                    if (e.getObject() instanceof Creatiefase) fasen.add(sessie.getFactCount());
                }
            });
            DroolsBackend.Vuring vuring = DroolsBackend.vuur(bouw, sessie, DroolsBackend.FACTOR, 1);
            assertNull(vuring.geenFixpunt(), "the fixpoint");
            assertTrue(fasen.size() >= 2, () -> "two phases or more: " + fasen);
            long grootste = Math.max(n0, fasen.stream().mapToLong(Long::longValue).max().orElseThrow());
            int waar = fasen.indexOf(grootste);
            assertTrue(waar >= 1 && grootste > fasen.get(0), () -> "a later phase holds more facts than the first, and"
                    + " the most: N0 = " + n0 + ", per phase " + fasen);
            assertEquals(DroolsBackend.FACTOR * r * grootste, vuring.plafond(), () -> "the cap is FACTOR × R × N for"
                    + " the N of phase " + (waar + 1) + ", " + DroolsBackend.FACTOR + " × " + r + " × " + grootste
                    + ", not that of the first phase, " + fasen.get(0) + ", nor N0 = " + n0);
            return null;
        });
    }

    @Test void de_andere_takken_houden_hun_vaste_plafond() throws Exception {
        for (DroolsBackend.Branch tak : List.of(DroolsBackend.Branch.EXEC_MODEL, DroolsBackend.Branch.DRL_TEXT)) {
            DroolsBackend.Build b = DroolsBackend.build(TmsDocumenten.document(TmsDocumenten.model(List.of("erkend",
                    "vergund")), List.of(TmsDocumenten.kenmerkregel("vergund indien erkend", "vergund",
                    TmsDocumenten.check("erkend", false)))), tak);
            TmsHerhaaldVurenWitnessTest.inSessie(b, () -> {
                KieSession sessie = b.kieBase().newKieSession();
                try {
                    DroolsBackend.voegIn(b, sessie, new Invoer().boon(TmsDocumenten.jutterBoon(b.beanLoader(),
                            List.of("erkend"))));
                    DroolsBackend.Vuring vuring = DroolsBackend.vuur(b, sessie);
                    assertEquals(100_000, vuring.plafond(), tak + ": the fixed cap");
                    assertNull(vuring.geenFixpunt(), tak + ": the fixpoint");
                } finally {
                    sessie.dispose();
                }
            });
        }
    }

    /** The Vondsten of the public run: enough that FACTOR × R × N0 lies above the floor. */
    private static final int VONDSTEN = 600;

    /** A session of {@code b} with J1, V1 (gewicht 10) and V2 (gewicht 4) of the values document put in. */
    private static KieSession gevuld(DroolsBackend.Build b) throws Exception {
        return gevuld(b, 2);
    }

    /** A session of {@code b} with J1 and {@code vondsten} Vondsten of the values document put in, each found by J1,
     *  the first of gewicht 10 and every other of gewicht 4. */
    private static KieSession gevuld(DroolsBackend.Build b, int vondsten) throws Exception {
        Object j1 = TmsDocumenten.boon(b.beanLoader(), "Jutter", Map.of(), List.of());
        List<Object> bonen = new ArrayList<>(List.of(j1));
        for (int i = 0; i < vondsten; i++) {
            Object v = TmsDocumenten.boon(b.beanLoader(), "Vondst", Map.of("gewicht", i == 0 ? "10" : "4"), List.of());
            TmsHerhaaldVurenWitnessTest.zetRol(v, "vinder", j1);
            bonen.add(v);
        }
        KieSession sessie = b.kieBase().newKieSession();
        DroolsBackend.voegIn(b, sessie, new Invoer().bonen(bonen));
        return sessie;
    }

    /** The rules of {@code kieBase}, the R of the cap. */
    private static long regels(KieBase kieBase) {
        long n = 0;
        for (KiePackage p : kieBase.getKiePackages()) n += p.getRules().size();
        return n;
    }
}
