package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Acceptance (a) of card fixpoint-readings: the reading rules leave the fixpoint of the other rules alone. Every golden
 * the exec-model branch plays, and every case of it, is fired on both branches through the build without readings and
 * the build with them ({@code DroolsBackend.build(…, readings)}), and the dumps of the two sessions ({@link SessionDump})
 * must be equal once the facts of the reading rules are taken out: the input, what {@code DroolsBackend.fixpointOf}
 * gives, every other fact at the fixpoint and the outcome of the fire. The firings are left out, as in
 * {@link BranchDumpsCorpusWitnessTest}: the reading rules fire, and their firings change the order of the agenda, not the
 * fixpoint. A reading rule reads what the other rules derive and no rule reads what it inserts, so under truth
 * maintenance the other facts cannot depend on it; this holds that over the corpus, and that the build with readings
 * plans reading rules at all: a corpus whose sessions hold no reading would pass the comparison without one.
 *
 * <p>Floors, measured 2026-10-21 at the card's first commit of code: {@value #EXPECTED_MIN_GOLDENS} goldens with
 * {@value #EXPECTED_MIN_CASES} cases per branch, and {@value #EXPECTED_MIN_READINGS} reading facts at the fixpoints of
 * those cases per branch. A smaller count is a corpus that lost goldens or cases, or a build that stopped planning
 * reading rules; a growing corpus needs no change here.
 */
class ReadingRulesFixpointWitnessTest {

    private static final Path CORPUS = Path.of("..", "..", "corpus");

    static final int EXPECTED_MIN_GOLDENS = 261;
    static final int EXPECTED_MIN_CASES = 706;
    static final int EXPECTED_MIN_READINGS = 3635;

    @Test void the_reading_rules_leave_the_fixpoint_of_every_other_rule_alone() throws Exception {
        List<Path> goldens;
        try (Stream<Path> s = Files.walk(CORPUS.resolve("golden"))) {
            goldens = s.filter(p -> p.getFileName().toString().equals("ir.json")).map(Path::getParent).sorted()
                    .toList();
        }
        List<String> differences = new ArrayList<>();
        int compared = 0, cases = 0;
        long readings = 0, firingsWithout = 0, firingsWith = 0;
        for (DroolsBackend.Branch branch : TmsDocuments.BRANCHES) {
            int goldensOfBranch = 0, casesOfBranch = 0;
            long readingsOfBranch = 0;
            for (Path g : goldens) {
                List<Path> casePaths = new ArrayList<>();
                for (Path casePath : casePaths(g))
                    if (!TmsDocuments.JSON.readTree(casePath.toFile()).has("expectedFault")) casePaths.add(casePath);
                if (casePaths.isEmpty()) continue;
                String name = CORPUS.resolve("golden").relativize(g).toString();
                Path ir = g.resolve("ir.json");
                if (!CaseRunner.viaExecModel(ir, casePaths).status().equals("PLAYED")) continue;
                CaseRunner without = CaseRunner.forMeasurement(ir, branch), with;
                try {
                    with = CaseRunner.forMeasurement(ir, branch, true);
                } catch (RuntimeException e) {
                    differences.add(branch + " " + name + ": de build met lezingen bouwt niet: " + e);
                    continue;
                }
                goldensOfBranch++;
                for (Path casePath : casePaths) {
                    JsonNode c = TmsDocuments.JSON.readTree(casePath.toFile());
                    String which = branch + " " + name + "/" + casePath.getFileName();
                    String a = BranchDumps.dump(without, c), b = BranchDumps.dump(with, c);
                    if (b.lines().noneMatch(r -> r.equals("fixpoint yes")))
                        differences.add(which + ": de build met lezingen bereikt geen fixpunt");
                    readingsOfBranch += b.lines().filter(ReadingRulesFixpointWitnessTest::readingFact).count();
                    firingsWithout += firings(a);
                    firingsWith += firings(b);
                    String difference = firstDifference(production(a), production(b));
                    if (difference != null) differences.add(which + ": " + difference);
                    casesOfBranch++;
                }
            }
            int n = goldensOfBranch, m = casesOfBranch;
            long r = readingsOfBranch;
            assertTrue(n >= EXPECTED_MIN_GOLDENS, () -> branch + ": vergeleken goldens " + n + ", onder de vloer "
                    + EXPECTED_MIN_GOLDENS);
            assertTrue(m >= EXPECTED_MIN_CASES, () -> branch + ": vergeleken cases " + m + ", onder de vloer "
                    + EXPECTED_MIN_CASES);
            assertTrue(r >= EXPECTED_MIN_READINGS, () -> branch + ": lezingen aan de fixpunten " + r + ", onder de vloer "
                    + EXPECTED_MIN_READINGS);
            compared += n;
            cases += m;
            readings += r;
            System.out.println("ReadingRulesFixpointWitnessTest " + branch + ": " + n + " goldens, " + m + " cases, " + r
                    + " lezingen");
        }
        System.out.println("ReadingRulesFixpointWitnessTest: " + compared + " goldens, " + cases + " cases, " + readings
                + " lezingen; vuringen zonder " + firingsWithout + ", met " + firingsWith);
        assertEquals(List.of(), differences, "cases waarvan de productiefeiten met en zonder lezingen verschillen");
    }

    /** Whether {@code line} of a dump is a fact of a reading rule. */
    private static boolean readingFact(String line) {
        return line.startsWith("fact Reading(");
    }

    /** {@code dump} as the build without readings must give it: without the firings, without the facts of the reading
     *  rules and the section of their readings, and without the count of the facts, which counts those. */
    private static String production(String dump) {
        return Stream.of(dump.split("\n")).filter(r -> !r.startsWith("firings ") && !r.startsWith("trace-")
                        && !r.startsWith("facts ") && !r.startsWith("reading ") && !readingFact(r))
                .collect(Collectors.joining("\n"));
    }

    private static long firings(String dump) {
        for (String r : dump.split("\n"))
            if (r.startsWith("firings ") && !r.startsWith("firings throws")) return Long.parseLong(r.split(" ")[1]);
        return 0;
    }

    private static String firstDifference(String without, String with) {
        if (without.equals(with)) return null;
        String[] a = without.split("\n"), b = with.split("\n");
        for (int i = 0; i < Math.max(a.length, b.length); i++) {
            String x = i < a.length ? a[i] : "<einde>", y = i < b.length ? b[i] : "<einde>";
            if (!x.equals(y)) return "regel " + (i + 1) + ": zonder " + x + " | met " + y;
        }
        return "gelijke regels, ander einde";
    }

    private static List<Path> casePaths(Path map) throws Exception {
        try (Stream<Path> s = Files.list(map)) {
            return s.filter(p -> p.getFileName().toString().matches("case.*\\.json")).sorted().toList();
        }
    }
}
