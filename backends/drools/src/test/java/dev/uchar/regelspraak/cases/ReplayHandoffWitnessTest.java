package dev.uchar.regelspraak.cases;

import dev.uchar.regelspraak.engine.ScenarioResult;
import dev.uchar.regelspraak.transpiler.tms.ConditionReading;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Step 5 of card fixpoint-readings: the scenario report hands each session it fired to a consumer
 * ({@code CaseRunner.report(…, Handoff)}), on a build with readings, so that the scenario runner of the distribution
 * explains the session the report judged instead of playing the case a second time. Over every golden the exec-model
 * branch plays: the report with a consumer is the report without one, report for report, the verdicts, the
 * expectations, the trace and the coverage; the consumer gets every case the report played out once, with a session
 * whose readings and fixpoint it can read; and the input beans it got carry the derived values once the report is
 * done, on the anchor {@code jutterswet-vondstwaardering} by hand.
 *
 * <p>Floor {@value #EXPECTED_MIN_CASES} cases handed over, measured 2026-10-21.
 */
class ReplayHandoffWitnessTest {

    private static final Path CORPUS = Path.of("..", "..", "corpus");

    static final int EXPECTED_MIN_CASES = 706;

    @Test void the_report_hands_over_the_sessions_it_judged() throws Exception {
        List<Path> goldens;
        try (Stream<Path> s = Files.walk(CORPUS.resolve("golden"))) {
            goldens = s.filter(p -> p.getFileName().toString().equals("ir.json")).map(Path::getParent).sorted()
                    .toList();
        }
        List<String> differences = new ArrayList<>();
        int handed = 0;
        for (Path g : goldens) {
            List<Path> casePaths = new ArrayList<>();
            try (Stream<Path> s = Files.list(g)) {
                for (Path p : s.filter(p -> p.getFileName().toString().matches("case.*\\.json")).sorted().toList())
                    if (!TmsDocuments.JSON.readTree(p.toFile()).has("expectedFault")) casePaths.add(p);
            }
            if (casePaths.isEmpty()) continue;
            Path ir = g.resolve("ir.json");
            if (!CaseRunner.viaExecModel(ir, casePaths).status().equals("PLAYED")) continue;
            String name = CORPUS.resolve("golden").relativize(g).toString();
            Map<String, Integer> readings = new LinkedHashMap<>();
            List<ScenarioResult> without = CaseRunner.report(ir, casePaths);
            List<ScenarioResult> with = CaseRunner.report(ir, casePaths, (file, build, session, labels, firing) -> {
                if (readings.put(file, DroolsBackend.readingsOf(build, session).size()) != null)
                    differences.add(name + "/" + file + ": twee keer doorgegeven");
                DroolsBackend.fixpointOf(build, session);
            });
            if (!without.equals(with)) differences.add(name + ": het rapport met doorgifte verschilt van dat zonder");
            for (ScenarioResult r : with)
                if (r.status() != ScenarioResult.Status.MARKER && !readings.containsKey(r.name()))
                    differences.add(name + "/" + r.name() + ": niet doorgegeven");
            handed += readings.size();
        }
        assertEquals(List.of(), differences, "rapporten waarvan de doorgifte niet klopt");
        int n = handed;
        assertTrue(n >= EXPECTED_MIN_CASES, () -> "doorgegeven cases " + n + ", onder de vloer " + EXPECTED_MIN_CASES);
        System.out.println("ReplayHandoffWitnessTest: " + n + " cases doorgegeven");
    }

    /** The beans handed over carry the derived values once the report wrote them back: M's waardering of 50 in the
     *  first case of the anchor, which its defaults stack decides, beside the readings of the session. */
    @Test void the_beans_handed_over_carry_the_write_back() throws Exception {
        Path g = CORPUS.resolve("golden").resolve("anchor").resolve("jutterswet-vondstwaardering");
        List<Object> beans = new ArrayList<>();
        List<ConditionReading.Status> statuses = new ArrayList<>();
        List<ScenarioResult> report = CaseRunner.report(g.resolve("ir.json"), List.of(g.resolve("case1-erkend.json")),
                (file, build, session, labels, firing) -> {
                    beans.add(labels.get("M"));
                    for (ConditionReading r : DroolsBackend.readingsOf(build, session)) statuses.add(r.status());
                });
        assertEquals(ScenarioResult.Status.PASS, report.get(0).status());
        assertEquals(1, beans.size());
        assertEquals(0, new BigDecimal("50").compareTo((BigDecimal) TmsDocuments.read(beans.get(0), "waardering")));
        assertTrue(statuses.contains(ConditionReading.Status.HOLDS), () -> "de lezingen van de sessie: " + statuses);
    }
}
