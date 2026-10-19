package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;

import java.nio.file.Path;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * One case fired through both branches of the analysis of card drl-teksttak-op-truth-maintenance, the exec-model branch
 * and the DRL-text branch, its rendering as DRL text: the same verdict, and the same dump of the session
 * ({@link SessionDump}) in its input, in what {@code DroolsBackend.fixpointOf} gives and in the facts at the fixpoint. The
 * firings are left out of the comparison: in a document with a fold their order and number follow the agenda, which
 * differs between the two renderings, while the fixpoint is the same. Made for {@link DrlTextFoldPathWitnessTest} (fix
 * round 3a), and shared since a second witness compares dumps over the two branches (fix round 3b).
 */
final class BranchDumps {

    private BranchDumps() {}

    /** Fires {@code caseDocument} through both branches: the same verdict, and the same dump but for the firings. Returns the
     *  dump of the exec-model branch without them; {@code which} names the case in a failure. */
    static String equal(Path ir, JsonNode caseDocument, String which) throws Exception {
        String exec = TmsFaultWitnessTest.play(ir, caseDocument, DroolsBackend.Branch.EXEC_MODEL);
        assertEquals(exec, TmsFaultWitnessTest.play(ir, caseDocument, DroolsBackend.Branch.DRL_TEXT), "het oordeel, " + which);
        String out = withoutFirings(fixpoint(dump(ir, caseDocument, DroolsBackend.Branch.EXEC_MODEL), which));
        assertEquals(out, withoutFirings(fixpoint(dump(ir, caseDocument, DroolsBackend.Branch.DRL_TEXT), which)),
                "de dumps van de twee takken, " + which);
        return out;
    }

    /** {@code dump}, of a session that reached its fixpoint: only there does it hold what {@code read} gives. */
    private static String fixpoint(String dump, String which) {
        assertTrue(dump.lines().anyMatch(r -> r.startsWith("firings ") && r.endsWith(" noFixpoint null")),
                () -> "geen fixpunt, " + which + ":\n" + dump);
        return dump;
    }

    /** The dump of the session of {@code caseDocument} on {@code branch}, fired as a consumer fires it. */
    static String dump(Path ir, JsonNode caseDocument, DroolsBackend.Branch branch) throws Exception {
        return CaseRunner.forMeasurement(ir, branch).withSession(caseDocument, (build, session, labels) -> {
            SessionDump d = SessionDump.op(session, labels);
            StringBuilder out = new StringBuilder();
            d.write(out, build, DroolsBackend.fire(build, session), labels);
            return out.toString();
        });
    }

    /** The dump of the session of {@code caseDocument} on the branch of {@code runner}, whose KieBase is built once, fired as a
     *  consumer fires it; a fire that throws gives the dump of a session whose fire threw. Made for
     *  {@link BranchDumpsCorpusWitnessTest}, which fires every case of a golden on one build per branch. */
    static String dump(CaseRunner runner, JsonNode caseDocument) {
        return runner.withSession(caseDocument, (build, session, labels) -> {
            SessionDump d = SessionDump.op(session, labels);
            StringBuilder out = new StringBuilder();
            DroolsBackend.Firing v;
            try {
                v = DroolsBackend.fire(build, session);
            } catch (RuntimeException e) {
                d.write(out, e, labels);
                return out.toString();
            }
            d.write(out, build, v, labels);
            return out.toString();
        });
    }

    /** {@code dump} without the lines of the firings. */
    static String withoutFirings(String dump) {
        return Stream.of(dump.split("\n")).filter(r -> !r.startsWith("firings ") && !r.startsWith("trace-"))
                .collect(Collectors.joining("\n"));
    }
}
