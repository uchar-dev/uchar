package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;

import dev.uchar.regelspraak.engine.ScenarioResult;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Acceptance (d) of card fixpoint-readings: a reading rule belongs to no form, so the observer of a session
 * ({@code DroolsBackend.Observer}) and the coverage ({@code DroolsBackend.coverage}) do not see it. Every case of every
 * golden the exec-model branch plays is fired on both branches through the build without readings and the build with
 * them, each with an observer: the forms the two observers saw are the same, and so is the coverage made of them, row
 * for row, and so are the firings per form with the labels they bound ({@code Observer#firings}), the trace of a
 * scenario report. That last one is more than the reading rules' being of no form: an extra rule in the KieBase could
 * change the order in which the agenda fires the others, and under truth maintenance a form could fire once more or
 * less on its way to the same fixpoint. Measured 2026-10-21 it does not, on any case of the corpus; a consumer that
 * plays a case on the build with readings beside a report of the build without, as the scenario runner of the
 * distribution compares them, finds the same firings.
 *
 * <p>Floor {@value #EXPECTED_MIN_CASES} cases per branch, measured 2026-10-21.
 */
class ReadingRulesObserverWitnessTest {

    private static final Path CORPUS = Path.of("..", "..", "corpus");

    static final int EXPECTED_MIN_CASES = 706;

    @Test void the_observer_and_the_coverage_do_not_see_the_reading_rules() throws Exception {
        List<Path> goldens;
        try (Stream<Path> s = Files.walk(CORPUS.resolve("golden"))) {
            goldens = s.filter(p -> p.getFileName().toString().equals("ir.json")).map(Path::getParent).sorted()
                    .toList();
        }
        List<String> differences = new ArrayList<>();
        for (DroolsBackend.Branch branch : TmsDocuments.BRANCHES) {
            int cases = 0;
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
                CaseRunner without = CaseRunner.forMeasurement(ir, branch), with = CaseRunner.forMeasurement(ir, branch, true);
                for (Path casePath : casePaths) {
                    JsonNode c = TmsDocuments.JSON.readTree(casePath.toFile());
                    String which = branch + " " + name + "/" + casePath.getFileName();
                    Observed a = observed(without, c), b = observed(with, c);
                    if (!a.forms().equals(b.forms()))
                        differences.add(which + ": vormen " + a.forms() + " zonder, " + b.forms() + " met lezingen");
                    if (!a.coverage().equals(b.coverage()))
                        differences.add(which + ": de dekking verschilt met lezingen");
                    if (!a.firings().equals(b.firings()))
                        differences.add(which + ": vuringen " + a.firings() + " zonder, " + b.firings() + " met lezingen");
                    cases++;
                }
            }
            int n = cases;
            assertTrue(n >= EXPECTED_MIN_CASES, () -> branch + ": cases " + n + ", onder de vloer " + EXPECTED_MIN_CASES);
        }
        assertEquals(List.of(), differences, "cases waarin de waarnemer of de dekking de leesregels ziet");
    }

    private record Observed(Set<String> forms, List<ScenarioResult.CoverageRule> coverage,
                            List<ScenarioResult.Firing> firings) {}

    private static Observed observed(CaseRunner r, JsonNode c) {
        return r.withSession(c, (build, session, labels) -> {
            Map<Object, String> label = new IdentityHashMap<>();
            labels.forEach((l, o) -> label.put(o, l));
            DroolsBackend.Observer o = new DroolsBackend.Observer(label::get);
            DroolsBackend.fire(build, session, o);
            return new Observed(o.forms(), DroolsBackend.coverage(build, o.forms()), o.firings());
        });
    }
}
