package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;
import dev.uchar.regelspraak.loader.IrLoader;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Acceptance (a) of card reading-after-refusal: the build with readings excludes every golden the replay excludes, with
 * the replay's own line, on both branches. The lowering opens the reading of a form before it lowers the form and closes
 * it after; a form it refuses part-way must not leave that reading open, or the next form fails where it opens its own
 * and the build with readings ends in an {@code ERROR} of its own where the replay gives the refusal
 * ({@code anchor/jutterswet-bootberging}, {@code jutterswet-ketenkwantor}, {@code jutterswet-muntketen}, DROOLS-067, and
 * {@code jutterswet-havenrooster}, DROOLS-179, until the card).
 *
 * <p>Per golden with a case file, the build without readings on the exec-model branch says whether the replay excludes
 * it: every exclusion of the corpus is one of the build (a loader refusal, a form the transpiler does not lower, a
 * collision of bean names), and gate 12 holds the line of the DRL-text branch to that of the exec-model branch. For each
 * golden so excluded, the status line of {@code DroolsBackendMain} without and with {@code --readings} must be the same
 * {@code EXCLUDE} line, on either branch. A golden whose build lowers every form plays, or the case reader, which the two
 * builds share, excludes it per case; {@code ReadingRulesFixpointWitnessTest} builds with readings every golden that
 * plays, and gate 17 compares the lines of every golden.
 *
 * <p>Floor, measured 2026-10-21: {@value #EXPECTED_MIN_EXCLUDED} goldens the replay excludes (the {@code EXCLUDE}
 * entries of gate 12's {@code EXPECTED_CHANNEL}). A smaller count is a corpus that lost such goldens or a pre-filter that
 * stopped seeing them, and the comparison would pass on fewer.
 */
class ReadingsExclusionWitnessTest {

    private static final Path CORPUS = Path.of("..", "..", "corpus");

    static final int EXPECTED_MIN_EXCLUDED = 46;

    @Test void the_build_with_readings_excludes_every_golden_the_replay_excludes_with_the_same_line() throws Exception {
        List<Path> goldens;
        try (Stream<Path> s = Files.walk(CORPUS.resolve("golden"))) {
            goldens = s.filter(p -> p.getFileName().toString().equals("ir.json")).map(Path::getParent).sorted()
                    .toList();
        }
        List<String> differences = new ArrayList<>();
        int excluded = 0;
        for (Path g : goldens) {
            List<Path> casePaths = new ArrayList<>();
            for (Path casePath : casePaths(g)) {
                JsonNode c = TmsDocuments.JSON.readTree(casePath.toFile());
                if (!c.has("expectedFault")) casePaths.add(casePath);
            }
            if (casePaths.isEmpty()) continue;
            Path ir = g.resolve("ir.json");
            try {
                DroolsBackend.build(ir, DroolsBackend.Branch.EXEC_MODEL);
                continue;
            } catch (CaseRunner.Excluded | IrLoader.Refusal e) {
                excluded++;
            }
            String name = CORPUS.resolve("golden").relativize(g).toString();
            for (DroolsBackend.Branch branch : TmsDocuments.BRANCHES) {
                String plain = statusLine(branch, false, ir, casePaths);
                String withReadings = statusLine(branch, true, ir, casePaths);
                if (!plain.startsWith("EXCLUDE "))
                    differences.add(name + " " + branch + ": de build weigert, de replay geeft " + plain);
                else if (!withReadings.equals(plain))
                    differences.add(name + " " + branch + ": replay " + plain + " | met lezingen " + withReadings);
            }
        }
        int n = excluded;
        System.out.println("ReadingsExclusionWitnessTest: " + n + " goldens die de replay uitsluit, op "
                + TmsDocuments.BRANCHES.size() + " takken vergeleken");
        assertTrue(n >= EXPECTED_MIN_EXCLUDED, () -> "goldens die de replay uitsluit " + n + ", onder de vloer "
                + EXPECTED_MIN_EXCLUDED);
        assertEquals(List.of(), differences, "goldens die de build met lezingen anders uitsluit dan de replay");
    }

    /** The last line {@code DroolsBackendMain} prints for {@code ir} and {@code casePaths} on {@code branch}, with
     *  {@code --readings} where {@code readings}: its status line. */
    private static String statusLine(DroolsBackend.Branch branch, boolean readings, Path ir, List<Path> casePaths) {
        List<String> arguments = new ArrayList<>();
        if (readings) arguments.add("--readings");
        if (branch == DroolsBackend.Branch.DRL_TEXT) arguments.add("--drl");
        arguments.add(ir.toString());
        for (Path c : casePaths) arguments.add(c.toString());
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        DroolsBackendMain.run(arguments.toArray(String[]::new), new PrintStream(out, true, StandardCharsets.UTF_8));
        List<String> lines = out.toString(StandardCharsets.UTF_8).lines().toList();
        return lines.isEmpty() ? "<geen uitvoer>" : lines.get(lines.size() - 1);
    }

    private static List<Path> casePaths(Path map) throws Exception {
        try (Stream<Path> s = Files.list(map)) {
            return s.filter(p -> p.getFileName().toString().matches("case.*\\.json")).sorted().toList();
        }
    }
}
