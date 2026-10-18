package dev.uchar.regelspraak.cases;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The red entry of the case runner builds the branch it is given (lens on step 6b of card
 * drl-teksttak-op-truth-maintenance): {@link CaseRunner#runRed(Path, Path, DroolsBackend.Branch)} on {@code DRL_TEXT}
 * fires the DRL text. The red row of {@link BranchDumpsCorpusWitnessTest} demands the same {@link CaseRunner.RedResult}
 * of both branches on every red case, and the two branches give the same result on every one, so that row cannot tell
 * which branch the entry built: an entry that builds the exec-model branch whatever it is given compares that branch
 * with itself, and the row stays green. Gate 8 drives the exec-model branch alone.
 *
 * <p>The seam of {@link CaseRunner} ({@link SessionDump}, under the property {@value SessionDump#PROPERTY}) names the
 * dump of every session it writes after the branch of the build that fired the session
 * ({@code DroolsBackend.Build#branch}), which is the branch the entry chose. A child JVM, with the property set to a
 * fresh directory, plays the red case {@code division-by-numeric-zero}, a fault at fire, through the red entry once per
 * branch: each call leaves exactly one dump, under its own branch, and the two calls report the same
 * {@code RedResult}. A child JVM, because the seam reads the property once, when its class loads, and the JVM of the
 * suite has loaded it long before.
 *
 * <p><b>Red</b> with the mutation of the lens, in a copy (not committed; the diff and the output are kept with step 6c
 * of the card): the entry builds with {@code build(irPath)}, the exec-model branch, whatever branch it is given. The
 * child then leaves two dumps of the exec-model branch and none of the DRL-text branch, while the red row of
 * {@link BranchDumpsCorpusWitnessTest} stays green.
 */
class RedBranchChoiceWitnessTest {

    // cwd under surefire is the module dir (backends/drools); the corpus is two levels up
    private static final Path CASE = Path.of("..", "..", "corpus", "rood", "division-by-numeric-zero");

    @Test void the_red_entry_builds_the_branch_that_it_gets() throws Exception {
        Path map = Files.createTempDirectory("rode-takkeuze");
        Path ir = CASE.resolve("ir.json").toAbsolutePath();
        List<String> task = List.of(Path.of(System.getProperty("java.home"), "bin", "java").toString(),
                "-D" + SessionDump.PROPERTY + "=" + map, "-cp", System.getProperty("java.class.path"),
                RedBranchChoiceWitnessTest.class.getName(), ir.toString(),
                CASE.resolve("case1.json").toAbsolutePath().toString());
        Process p = new ProcessBuilder(task).redirectErrorStream(true).start();
        String out = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        assertEquals(0, p.waitFor(), () -> "de kind-JVM:\n" + out);

        List<String> dumps = TmsDocuments.BRANCHES.stream()
                .map(branch -> SessionDump.hash(ir) + "." + branch.name().toLowerCase(Locale.ROOT) + ".1.txt").sorted().toList();
        List<String> written;
        try (Stream<Path> s = Files.list(map)) {
            written = s.map(f -> f.getFileName().toString()).sorted().toList();
        }
        assertEquals(dumps, written, () -> "de dumps van de rode ingang, één per tak; de kind-JVM:\n" + out);
        List<String> results = out.lines().filter(r -> r.startsWith("EXEC_MODEL ") || r.startsWith("DRL_TEXT "))
                .map(r -> r.substring(r.indexOf(' ') + 1)).toList();
        assertEquals(2, results.size(), () -> "een resultaat per tak; de kind-JVM:\n" + out);
        assertEquals(results.get(0), results.get(1), "het resultaat van de rode ingang op de twee takken");
    }

    /** The child JVM of the witness: plays the red case of {@code args} through the red entry on both branches and
     *  prints a line per call, the branch and the result. */
    public static void main(String[] args) {
        Path ir = Path.of(args[0]), caseDocument = Path.of(args[1]);
        for (DroolsBackend.Branch branch : TmsDocuments.BRANCHES) {
            CaseRunner.RedResult r = CaseRunner.runRed(ir, caseDocument, branch);
            System.out.println(branch + " " + r.status() + " " + r.detail());
        }
    }
}
