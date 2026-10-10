package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;

import org.junit.jupiter.api.Test;
import org.kie.api.KieBase;
import org.kie.api.definition.KiePackage;
import org.kie.api.runtime.KieSession;

import java.nio.file.Files;
import java.nio.file.Path;
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
 * <p>The values document of {@link TmsHerhaaldVurenWitnessTest} reaches its fixpoint under the public cap, and with an
 * artificially small factor (0.01, floor 1, through the package-private seam) it stops at the cap, with a sentence that
 * names the cap, R and N0. On the golden {@code jutterswet-dubbelverbinding}, whose fact creations lay links in the
 * phases after the first fire, the cap at the end is the factor times R times the facts at the start of a phase, more
 * than N0: the fire before that phase derived facts, and the phases before it laid links.
 *
 * <p><b>Red before the change</b> (measured 2026-10-10 on {@code 095a586a}): the witness does not compile there, since
 * {@code Vuring} had no {@code plafond} and {@code vuur} neither a public form nor a factor; the cap was the fixed
 * 100 000 the caller passed, and at the cap {@code geenFixpunt} was null. <b>Red by mutation</b> (measured the same day,
 * not committed): with the cap computed once, before the first fire, the row of the creation phases failed on "the cap
 * at the end, 1000 × 19 × 7, counts the facts at the start of a phase, more than N0 = 7 ==&gt; expected: &lt;true&gt;
 * but was: &lt;false&gt;".
 */
class TmsVuurplafondWitnessTest {

    @Test void het_plafond_groeit_met_de_regels_en_de_feiten() throws Exception {
        DroolsBackend.Build b = DroolsBackend.build(TmsHerhaaldVurenWitnessTest.waardedocument(),
                DroolsBackend.Branch.TRUTH_MAINTENANCE);
        TmsHerhaaldVurenWitnessTest.inSessie(b, () -> {
            KieSession sessie = gevuld(b);
            try {
                long r = regels(b.kieBase()), n0 = sessie.getFactCount();
                DroolsBackend.Vuring vuring = DroolsBackend.vuur(b, sessie);
                assertNull(vuring.geenFixpunt(), "the fixpoint under the public cap");
                assertEquals(Math.max(10_000, 4 * r * n0), vuring.plafond(), "max(10 000, 4 × R × N0)");
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

    @Test void een_creatiefase_rekent_het_plafond_opnieuw_uit() throws Exception {
        Path ir = Path.of("..", "..", "corpus", "golden", "jutterswet-dubbelverbinding", "ir.json");
        JsonNode zaak = TmsDocumenten.JSON.readTree(Files.readString(ir.resolveSibling("case1-binnen-venster.json")));
        CaseRunner runner = CaseRunner.voorMeting(ir, DroolsBackend.Branch.TRUTH_MAINTENANCE);
        runner.metSessie(zaak, (bouw, sessie, labels) -> {
            long r = regels(bouw.kieBase()), n0 = sessie.getFactCount();
            DroolsBackend.Vuring vuring = DroolsBackend.vuur(bouw, sessie, 1000.0, 1);
            assertNull(vuring.geenFixpunt(), "the fixpoint");
            assertTrue(DroolsBackend.lees(bouw, sessie).koppelingen().size() > 0, "the creations laid links");
            assertEquals(0, vuring.plafond() % (1000 * r), "the cap is 1000 × R × N for the N of a phase");
            long n = vuring.plafond() / (1000 * r);
            assertTrue(n > n0 && n <= sessie.getFactCount(), () -> "the cap at the end, 1000 × " + r + " × " + n
                    + ", counts the facts at the start of a phase, more than N0 = " + n0);
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

    /** A session of {@code b} with J1, V1 (gewicht 10) and V2 (gewicht 4) of the values document put in. */
    private static KieSession gevuld(DroolsBackend.Build b) throws Exception {
        Object j1 = TmsDocumenten.boon(b.beanLoader(), "Jutter", Map.of(), List.of());
        Object v1 = TmsDocumenten.boon(b.beanLoader(), "Vondst", Map.of("gewicht", "10"), List.of());
        Object v2 = TmsDocumenten.boon(b.beanLoader(), "Vondst", Map.of("gewicht", "4"), List.of());
        TmsHerhaaldVurenWitnessTest.zetRol(v1, "vinder", j1);
        TmsHerhaaldVurenWitnessTest.zetRol(v2, "vinder", j1);
        KieSession sessie = b.kieBase().newKieSession();
        DroolsBackend.voegIn(b, sessie, new Invoer().bonen(List.of(j1, v1, v2)));
        return sessie;
    }

    /** The rules of {@code kieBase}, the R of the cap. */
    private static long regels(KieBase kieBase) {
        long n = 0;
        for (KiePackage p : kieBase.getKiePackages()) n += p.getRules().size();
        return n;
    }
}
