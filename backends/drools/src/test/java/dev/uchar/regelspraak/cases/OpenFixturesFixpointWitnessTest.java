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
 * {@link ReadingRulesFixpointWitnessTest} and {@link BranchDumpsCorpusWitnessTest} over the documents with
 * {@code inferFromEmpty: false} outside the corpus: the fixtures of {@code src/test/resources/open-values/} and
 * {@code open-readings/} and those of {@code ref/test/fixtures/strict-empty/}. They hold the shapes of a fold that can be
 * open which no golden holds: value folds, unions, quantifier folds read without order, two routes to a member and folds
 * nested in a filter. Every case is fired on both branches through the build without readings and the build with them
 * ({@code DroolsBackend.build(…, readings)}), and the two dumps ({@link SessionDump}) must be equal once the facts of the
 * reading rules, their readings and the firings are taken out: a build with readings leaves the production rules' facts
 * and input as they are. And the dumps of each build on the two branches must be equal but for the firings, those of the
 * build with readings with their readings: which member a fold names where it is open follows no order of the agenda.
 *
 * <p>A document that the build without readings refuses stands in {@link #NOT_BUILT} with its code, as gate 12 lists a
 * golden it excludes; one that the build with readings refuses where the one without plays stands in
 * {@link #REFUSED_WITH_READINGS} with its code; both lists fail a stale entry. A case that pins a fault
 * ({@code expectedFault}) is skipped, as in the corpus witness.
 *
 * <p><b>Red</b> on {@code 62c393c94}, before card fold-order-in-readings: 127 cases per branch differed with and without
 * readings (the places of the open members of a fold, their {@code ListPlace} input, the identity in the sentence of a
 * {@code FilterFault}), and 25 between the branches without readings, one of them ({@code reading-nested/case3}) in the
 * open member a fold in a filter passed up.
 *
 * <p>Floors, measured 2026-11-24 at card fold-order-in-readings, with its fixture {@code reading-shared-field}: {@value #EXPECTED_MIN_DOCUMENTS}
 * documents and {@value #EXPECTED_MIN_CASES} cases per branch, and {@value #EXPECTED_MIN_READINGS} reading facts at the
 * fixpoints of those cases per branch.
 */
class OpenFixturesFixpointWitnessTest {

    private static final Path RESOURCES = Path.of("src", "test", "resources");
    private static final Path STRICT = Path.of("..", "..", "ref", "test", "fixtures", "strict-empty");

    static final int EXPECTED_MIN_DOCUMENTS = 95;
    static final int EXPECTED_MIN_CASES = 417;
    static final int EXPECTED_MIN_READINGS = 2451;

    /** The documents the build without readings refuses, with the code of the refusal, per branch: the forms of a document
     *  with {@code inferFromEmpty: false} this backend does not carry (backends/drools/README.md, "UncarriedForms"). */
    static final List<String> NOT_BUILT = List.of(
            "EXEC_MODEL strict-empty/fact-creation INFER_FROM_EMPTY_FALSE",
            "EXEC_MODEL strict-empty/let-closure INFER_FROM_EMPTY_FALSE",
            "EXEC_MODEL strict-empty/object-creation INFER_FROM_EMPTY_FALSE",
            "DRL_TEXT strict-empty/fact-creation INFER_FROM_EMPTY_FALSE",
            "DRL_TEXT strict-empty/let-closure INFER_FROM_EMPTY_FALSE",
            "DRL_TEXT strict-empty/object-creation INFER_FROM_EMPTY_FALSE");

    /** The documents the build with readings refuses where the one without plays, with the code of the refusal. */
    static final List<String> REFUSED_WITH_READINGS = List.of();

    @Test void a_build_with_readings_leaves_the_production_facts_of_every_open_fixture_alone() throws Exception {
        List<Path> documents = new ArrayList<>();
        for (Path root : List.of(RESOURCES.resolve("open-values"), RESOURCES.resolve("open-readings"), STRICT))
            try (Stream<Path> s = Files.list(root)) {
                documents.addAll(s.filter(p -> Files.isRegularFile(p.resolve("ir.json"))).sorted().toList());
            }
        List<String> differences = new ArrayList<>(), refused = new ArrayList<>(), notBuilt = new ArrayList<>();
        java.util.Map<String, String> execWithout = new java.util.HashMap<>(), execWith = new java.util.HashMap<>();
        for (DroolsBackend.Branch branch : TmsDocuments.BRANCHES) {
            int documentsOfBranch = 0, casesOfBranch = 0;
            long readings = 0;
            for (Path d : documents) {
                List<Path> casePaths = new ArrayList<>();
                try (Stream<Path> s = Files.list(d)) {
                    for (Path c : s.filter(p -> p.getFileName().toString().matches("case.*\\.json")).sorted().toList())
                        if (!TmsDocuments.JSON.readTree(c.toFile()).has("expectedFault")) casePaths.add(c);
                }
                if (casePaths.isEmpty()) continue;
                String name = d.getParent().getFileName() + "/" + d.getFileName();
                Path ir = d.resolve("ir.json");
                CaseRunner without, with;
                try {
                    without = CaseRunner.forMeasurement(ir, branch);
                } catch (CaseRunner.Excluded e) {
                    notBuilt.add(branch + " " + name + " " + e.code());
                    continue;
                }
                try {
                    with = CaseRunner.forMeasurement(ir, branch, true);
                } catch (CaseRunner.Excluded e) {
                    refused.add(branch + " " + name + " " + e.code());
                    continue;
                }
                documentsOfBranch++;
                for (Path casePath : casePaths) {
                    JsonNode c = TmsDocuments.JSON.readTree(casePath.toFile());
                    String which = branch + " " + name + "/" + casePath.getFileName();
                    String a = BranchDumps.dump(without, c), b = BranchDumps.dump(with, c);
                    readings += b.lines().filter(OpenFixturesFixpointWitnessTest::readingFact).count();
                    String difference = firstDifference(production(a), production(b));
                    if (difference != null) differences.add(which + ": met en zonder lezingen, " + difference);
                    String key = name + "/" + casePath.getFileName();
                    if (branch == DroolsBackend.Branch.EXEC_MODEL) {
                        execWithout.put(key, a);
                        execWith.put(key, b);
                    } else if (execWithout.containsKey(key)) {
                        String other = firstDifference(BranchDumps.withoutFirings(execWithout.get(key)),
                                BranchDumps.withoutFirings(a));
                        if (other != null) differences.add(key + ": de twee takken zonder lezingen, " + other);
                        String read = firstDifference(BranchDumps.withoutFirings(execWith.get(key)),
                                BranchDumps.withoutFirings(b));
                        if (read != null) differences.add(key + ": de twee takken met lezingen, " + read);
                    } else differences.add(key + ": speelt alleen op de DRL-teksttak");
                    casesOfBranch++;
                }
            }
            int n = documentsOfBranch, m = casesOfBranch;
            long r = readings;
            System.out.println("OpenFixturesFixpointWitnessTest " + branch + ": " + n + " documenten, " + m + " cases, "
                    + r + " lezingen");
            assertTrue(n >= EXPECTED_MIN_DOCUMENTS, () -> branch + ": vergeleken documenten " + n + ", onder de vloer "
                    + EXPECTED_MIN_DOCUMENTS);
            assertTrue(m >= EXPECTED_MIN_CASES, () -> branch + ": vergeleken cases " + m + ", onder de vloer "
                    + EXPECTED_MIN_CASES);
            assertTrue(r >= EXPECTED_MIN_READINGS, () -> branch + ": lezingen aan de fixpunten " + r + ", onder de vloer "
                    + EXPECTED_MIN_READINGS);
        }
        differences.forEach(System.out::println);
        notBuilt.forEach(System.out::println);
        assertEquals(NOT_BUILT, notBuilt, "documenten die de build zonder lezingen weigert");
        assertEquals(REFUSED_WITH_READINGS, refused, "documenten die de build met lezingen weigert");
        assertEquals(List.of(), differences, "cases waarvan de productiefeiten verschillen");
    }

    /** Whether {@code line} of a dump is a fact of a reading rule. */
    private static boolean readingFact(String line) {
        return line.startsWith("fact Reading(");
    }

    /** {@code dump} as the build without readings must give it, as {@link ReadingRulesFixpointWitnessTest} takes it:
     *  without the firings, the facts of the reading rules, the section of their readings and the count of the facts. */
    private static String production(String dump) {
        return Stream.of(dump.split("\n")).filter(r -> !r.startsWith("firings ") && !r.startsWith("trace-")
                        && !r.startsWith("facts ") && !r.startsWith("reading ") && !readingFact(r))
                .collect(Collectors.joining("\n"));
    }

    private static String firstDifference(String without, String with) {
        if (without.equals(with)) return null;
        String[] a = without.split("\n"), b = with.split("\n");
        for (int i = 0; i < Math.max(a.length, b.length); i++) {
            String x = i < a.length ? a[i] : "<einde>", y = i < b.length ? b[i] : "<einde>";
            if (!x.equals(y)) return "regel " + (i + 1) + ": " + x + " | " + y;
        }
        return "gelijke regels, ander einde";
    }
}
