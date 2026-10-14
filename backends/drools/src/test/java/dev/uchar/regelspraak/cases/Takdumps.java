package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;

import java.nio.file.Path;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * One case fired through both branches of the analysis of card drl-teksttak-op-truth-maintenance, the exec-model branch
 * and {@code DRL_TMS}, its rendering as DRL text: the same verdict, and the same dump of the session
 * ({@link Sessiedump}) in its input, in what {@code DroolsBackend.lees} gives and in the facts at the fixpoint. The
 * firings are left out of the comparison: in a document with a fold their order and number follow the agenda, which
 * differs between the two renderings, while the fixpoint is the same. Made for {@link DrlTmsVouwpadWitnessTest} (fix
 * round 3a), and shared since a second witness compares dumps over the two branches (fix round 3b).
 */
final class Takdumps {

    private Takdumps() {}

    /** Fires {@code zaak} through both branches: the same verdict, and the same dump but for the firings. Returns the
     *  dump of the exec-model branch without them; {@code welke} names the case in a failure. */
    static String gelijk(Path ir, JsonNode zaak, String welke) throws Exception {
        String exec = TmsFoutWitnessTest.speel(ir, zaak, DroolsBackend.Branch.EXEC_MODEL);
        assertEquals(exec, TmsFoutWitnessTest.speel(ir, zaak, DroolsBackend.Branch.DRL_TEXT), "het oordeel, " + welke);
        String uit = zonderVuringen(fixpunt(dump(ir, zaak, DroolsBackend.Branch.EXEC_MODEL), welke));
        assertEquals(uit, zonderVuringen(fixpunt(dump(ir, zaak, DroolsBackend.Branch.DRL_TEXT), welke)),
                "de dumps van de twee takken, " + welke);
        return uit;
    }

    /** {@code dump}, of a session that reached its fixpoint: only there does it hold what {@code lees} gives. */
    private static String fixpunt(String dump, String welke) {
        assertTrue(dump.lines().anyMatch(r -> r.startsWith("vuringen ") && r.endsWith(" geenFixpunt null")),
                () -> "geen fixpunt, " + welke + ":\n" + dump);
        return dump;
    }

    /** The dump of the session of {@code zaak} on {@code tak}, fired as a consumer fires it. */
    static String dump(Path ir, JsonNode zaak, DroolsBackend.Branch tak) throws Exception {
        return CaseRunner.voorMeting(ir, tak).metSessie(zaak, (bouw, sessie, labels) -> {
            Sessiedump d = Sessiedump.op(sessie, labels);
            StringBuilder uit = new StringBuilder();
            d.schrijf(uit, bouw, DroolsBackend.vuur(bouw, sessie), labels);
            return uit.toString();
        });
    }

    /** {@code dump} without the lines of the firings. */
    static String zonderVuringen(String dump) {
        return Stream.of(dump.split("\n")).filter(r -> !r.startsWith("vuringen ") && !r.startsWith("spoor-"))
                .collect(Collectors.joining("\n"));
    }
}
