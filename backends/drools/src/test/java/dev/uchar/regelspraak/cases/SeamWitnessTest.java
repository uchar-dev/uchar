package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The seam of {@link CaseRunner} writes the dump of every session the replay fires ({@link SessionDump}, under the
 * property {@value SessionDump#PROPERTY}), on both branches, also where the fire reaches no fixpoint (lens on step 6a
 * of card drl-teksttak-op-truth-maintenance). The comparison of the two branches over the dumps of the suite sees only
 * the sessions the seam writes, so a seam that skips a session of one branch hides that session from the comparison
 * without a sign: the count of the sessions holds a floor far below the sessions the suite fires.
 *
 * <p>A child JVM, with the property set to a fresh directory, replays two documents on both branches: the red case
 * {@code runaway-recursion}, whose creation reaches the creation budget and so no fixpoint, and the golden
 * {@code jutterswet-penningdrempel}, which reaches one. Each of the four sessions leaves exactly one dump, named by the
 * hash of its document and its branch, whose line {@code fixpoint} says {@code nee} with the sentence of the budget, or
 * {@code ja}. A child JVM, because the seam reads the property once, when its class loads, and the JVM of the suite has
 * loaded it long before, with or without the property.
 *
 * <p><b>Red</b> with the mutant of the lens (not committed; the diff and the output are kept with step 6b of the card): a
 * seam that writes no dump on the DRL-text branch where {@code vuur} reports no fixpoint, so the dump of the budget case on
 * that branch was missing.
 */
class SeamWitnessTest {

    // cwd under surefire is the module dir (backends/drools); the corpus is two levels up
    private static final Path CORPUS = Path.of("..", "..", "corpus");
    private static final Path WITHOUT_FIXPOINT = CORPUS.resolve("rood").resolve("runaway-recursion");
    private static final Path WITH_FIXPOINT = CORPUS.resolve("golden").resolve("jutterswet-penningdrempel");

    @Test void the_seam_dumps_every_session_of_both_branches_also_without_fixpoint() throws Exception {
        Path map = Files.createTempDirectory("naad");
        Path withoutIr = WITHOUT_FIXPOINT.resolve("ir.json").toAbsolutePath(), withIr = WITH_FIXPOINT.resolve("ir.json")
                .toAbsolutePath();
        List<String> task = List.of(Path.of(System.getProperty("java.home"), "bin", "java").toString(),
                "-D" + SessionDump.PROPERTY + "=" + map, "-cp", System.getProperty("java.class.path"),
                SeamWitnessTest.class.getName(), withoutIr.toString(),
                WITHOUT_FIXPOINT.resolve("case1.json").toAbsolutePath().toString(), withIr.toString(),
                WITH_FIXPOINT.resolve("case.json").toAbsolutePath().toString());
        Process p = new ProcessBuilder(task).redirectErrorStream(true).start();
        String out = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        assertEquals(0, p.waitFor(), () -> "de kind-JVM:\n" + out);

        String budget = "case: " + TmsCreationStratumWitnessTest.budget("werf eeuwig", 0, "Wachter");
        List<String> verdicts = new ArrayList<>(), dumps = new ArrayList<>();
        for (Path ir : List.of(withoutIr, withIr))
            for (DroolsBackend.Branch branch : TmsDocuments.BRANCHES) {
                verdicts.add(branch + " " + ir + ": " + (ir == withoutIr ? budget : "gespeeld"));
                dumps.add(SessionDump.hash(ir) + "." + branch.name().toLowerCase(Locale.ROOT) + ".1.txt");
            }
        assertEquals(verdicts, out.lines().filter(r -> r.startsWith("EXEC_MODEL ") || r.startsWith("DRL_TEXT "))
                .toList(), () -> "de oordelen van de kind-JVM:\n" + out);
        List<String> written;
        try (Stream<Path> s = Files.list(map)) {
            written = s.map(f -> f.getFileName().toString()).sorted().toList();
        }
        assertEquals(dumps.stream().sorted().toList(), written, "de dumps van de naad, één per sessie");
        for (String d : dumps) {
            List<String> fixpoint = Files.readAllLines(map.resolve(d)).stream().filter(r -> r.startsWith("fixpunt "))
                    .toList();
            // the dump quotes a sentence with its quotes escaped, and the sentence of the budget quotes the creation
            String expected = d.startsWith(SessionDump.hash(withoutIr)) ? "fixpunt nee \"" + TmsCreationStratumWitnessTest
                    .budget("werf eeuwig", 0, "Wachter").replace("\"", "\\\"") + "\"" : "fixpunt ja";
            assertEquals(List.of(expected), fixpoint, () -> "de regel fixpunt van " + d);
        }
    }

    /** The child JVM of the witness: replays each pair of a document and a case of {@code args} on both branches and
     *  prints a line per session, the branch, the document and the verdict. A red case has no expectations, so it plays
     *  with none, as the witnesses of the creation budget play it. */
    public static void main(String[] args) throws Exception {
        for (int i = 0; i + 1 < args.length; i += 2) {
            Path ir = Path.of(args[i]);
            JsonNode caseDocument = TmsDocuments.JSON.readTree(Files.readString(Path.of(args[i + 1])));
            if (!caseDocument.has("expected")) ((ObjectNode) caseDocument).putArray("expected");
            for (DroolsBackend.Branch branch : TmsDocuments.BRANCHES)
                System.out.println(branch + " " + ir + ": " + TmsFaultWitnessTest.play(ir, caseDocument, branch));
        }
    }
}
