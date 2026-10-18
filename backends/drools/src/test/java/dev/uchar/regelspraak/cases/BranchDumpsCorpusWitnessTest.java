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
 * The harness of card drl-teksttak-op-truth-maintenance as a witness of the suite (the owner's decision on norm
 * question 1 of Fable advice 4): the two branches of the analysis, the exec-model branch and the DRL-text branch, over
 * the whole corpus. Gate 12 holds each golden to its expectations and the two branches to the same status line; this
 * holds them to the same session.
 *
 * <p>Every golden the exec-model branch plays, and every case of it, is fired through both branches as a consumer fires
 * it, on one build per branch, and the dumps of the two sessions ({@link SessionDump}) must be equal: the input, what
 * {@code DroolsBackend.read} gives, the facts at the fixpoint and the outcome of the fire. The firings and their order
 * are left out, as in {@link BranchDumps}: they follow the agenda, which differs between the two renderings, while the
 * fixpoint is the same. A case whose fire reaches no fixpoint, at a cap, at the creation budget or by an exception,
 * would compare only its input and its line {@code fixpunt}, strictly, since the facts where a fire stops depend on the
 * order of the firings; no case of a played golden is one (measured 2026-10-14), and {@link #WITHOUT_FIXPOINT} names
 * them, so that one that arrives shows by name. A golden the exec-model branch does not play is gate 12's channel, and
 * this witness skips it.
 *
 * <p>Every case under {@code corpus/rood} is played through the red path of both branches
 * ({@link CaseRunner#runRed(Path, Path, DroolsBackend.Branch)}), which must give the same {@code RedResult}: the same
 * status and the same text. Gate 8's Drools leg plays the exec-model branch alone, so without this row no gate would
 * bring a red case to the DRL-text branch (decision 46 of the card). That the entry builds the branch it is given, which
 * this row cannot see while the two branches agree, is the witness {@link RedBranchChoiceWitnessTest}. A red path that
 * throws instead of giving a {@code RedResult} fails the row with its exception, on either branch: two branches that
 * throw the same exception have given no result to compare (lens on step 6b).
 *
 * <p>Floors, measured 2026-10-14 at step 6b of the card, after its five goldens and its red case entered the corpus:
 * {@value #EXPECTED_MIN_GOLDENS} goldens compared, with {@value #EXPECTED_MIN_CASES} cases, and
 * {@value #EXPECTED_MIN_RED_CASES} red case files. A smaller count is a corpus that lost goldens or cases, or a
 * selection that silently stopped comparing; a growing corpus needs no change here. The floor on the cases came with
 * step 6c (lens on step 6b): a golden that loses one of its cases still counts as compared. The two rows take about
 * 17 s together (measured 2026-10-14).
 *
 * <p><b>Red</b> with mutation W2 of Fable advice 4, in a copy (not committed; the diff and the output are kept with
 * step 6b of the card): a DRL rendering that inserts every consequence as a stated fact instead of a logical one, so
 * that the facts of a retracted justification stay on the DRL-text branch.
 */
class BranchDumpsCorpusWitnessTest {

    // cwd under surefire is the module dir (backends/drools); the corpus is two levels up
    private static final Path CORPUS = Path.of("..", "..", "corpus");

    /** The goldens compared, measured 2026-10-14: the 250 the exec-model branch played before step 6b and its five. */
    static final int EXPECTED_MIN_GOLDENS = 255;
    /** The cases of the goldens compared, measured 2026-10-14 at step 6b and again at step 6c, on the same corpus. */
    static final int EXPECTED_MIN_CASES = 695;
    /** The case files under {@code corpus/rood}, measured 2026-10-14: the 49 of before step 6b and its one. */
    static final int EXPECTED_MIN_RED_CASES = 50;
    /** The cases of played goldens whose fire reaches no fixpoint, measured 2026-10-14: none. */
    static final List<String> WITHOUT_FIXPOINT = List.of();

    @Test void every_case_of_every_played_golden_has_the_same_dump_on_both_branches() throws Exception {
        List<Path> goldens;
        try (Stream<Path> s = Files.walk(CORPUS.resolve("golden"))) {
            goldens = s.filter(p -> p.getFileName().toString().equals("ir.json")).map(Path::getParent).sorted()
                    .toList();
        }
        List<String> differences = new ArrayList<>(), withoutFixpoint = new ArrayList<>();
        int compared = 0, cases = 0;
        for (Path g : goldens) {
            List<Path> casePaths = new ArrayList<>();
            for (Path casePath : casePaths(g))
                if (!TmsDocuments.JSON.readTree(casePath.toFile()).has("expectedFault")) casePaths.add(casePath);
            if (casePaths.isEmpty()) continue;
            String naam = CORPUS.resolve("golden").relativize(g).toString();
            Path ir = g.resolve("ir.json");
            if (!CaseRunner.viaExecModel(ir, casePaths).status().equals("PLAYED")) continue;
            CaseRunner exec = CaseRunner.forMeasurement(ir, DroolsBackend.Branch.EXEC_MODEL), drl;
            try {
                drl = CaseRunner.forMeasurement(ir, DroolsBackend.Branch.DRL_TEXT);
            } catch (RuntimeException e) {
                differences.add(naam + ": de DRL-teksttak bouwt het document niet: " + e);
                continue;
            }
            compared++;
            for (Path casePath : casePaths) {
                JsonNode c = TmsDocuments.JSON.readTree(casePath.toFile());
                String which = naam + "/" + casePath.getFileName();
                String a = BranchDumps.dump(exec, c), b = BranchDumps.dump(drl, c);
                if (a.lines().noneMatch(r -> r.equals("fixpunt ja"))) withoutFixpoint.add(which);
                String difference = firstDifference(comparable(a), comparable(b));
                if (difference != null) differences.add(which + ": " + difference);
                cases++;
            }
        }
        assertEquals(List.of(), differences, "cases waarvan de dumps van de twee takken verschillen");
        assertEquals(WITHOUT_FIXPOINT, withoutFixpoint, "de cases zonder fixpunt");
        int n = compared, m = cases;
        assertTrue(n >= EXPECTED_MIN_GOLDENS, () -> "vergeleken goldens " + n + " (" + m + " cases), onder de vloer "
                + EXPECTED_MIN_GOLDENS);
        assertTrue(m >= EXPECTED_MIN_CASES, () -> "vergeleken cases " + m + " (in " + n + " goldens), onder de vloer "
                + EXPECTED_MIN_CASES);
    }

    @Test void every_red_case_gives_the_same_red_result_on_both_branches() throws Exception {
        List<Path> casePaths;
        try (Stream<Path> s = Files.walk(CORPUS.resolve("rood"))) {
            casePaths = s.filter(p -> p.getFileName().toString().matches("case.*\\.json")).sorted().toList();
        }
        List<String> differences = new ArrayList<>();
        for (Path casePath : casePaths) {
            Path ir = casePath.resolveSibling("ir.json");
            CaseRunner.RedResult exec = CaseRunner.runRed(ir, casePath, DroolsBackend.Branch.EXEC_MODEL);
            CaseRunner.RedResult drl = CaseRunner.runRed(ir, casePath, DroolsBackend.Branch.DRL_TEXT);
            if (!exec.equals(drl))
                differences.add(CORPUS.resolve("rood").relativize(casePath) + ": exec-model " + exec + " | drl-tekst " + drl);
        }
        assertEquals(List.of(), differences, "rode cases met een ander resultaat op de twee takken");
        int n = casePaths.size();
        assertTrue(n >= EXPECTED_MIN_RED_CASES, () -> "rode cases " + n + ", onder de vloer "
                + EXPECTED_MIN_RED_CASES);
    }

    /** What the two branches must have alike in a dump: at a fixpoint everything but the firings, and without one only
     *  the input and the line {@code fixpunt}. */
    private static String comparable(String dump) {
        if (dump.lines().anyMatch(r -> r.equals("fixpunt ja"))) return BranchDumps.withoutFirings(dump);
        return Stream.of(dump.split("\n")).filter(r -> r.startsWith("invoer ") || r.startsWith("fixpunt "))
                .collect(Collectors.joining("\n"));
    }

    /** The first line where {@code exec} and {@code drl} part, with both lines; null where they are equal. */
    private static String firstDifference(String exec, String drl) {
        if (exec.equals(drl)) return null;
        String[] a = exec.split("\n"), b = drl.split("\n");
        for (int i = 0; i < Math.max(a.length, b.length); i++) {
            String x = i < a.length ? a[i] : "<einde>", y = i < b.length ? b[i] : "<einde>";
            if (!x.equals(y)) return "regel " + (i + 1) + ": exec-model " + x + " | drl-tekst " + y;
        }
        return "gelijke regels, ander einde";
    }

    private static List<Path> casePaths(Path map) throws Exception {
        try (Stream<Path> s = Files.list(map)) {
            return s.filter(p -> p.getFileName().toString().matches("case.*\\.json")).sorted().toList();
        }
    }
}
