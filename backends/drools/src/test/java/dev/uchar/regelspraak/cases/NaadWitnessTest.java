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
 * The seam of {@link CaseRunner} writes the dump of every session the replay fires ({@link Sessiedump}, under the
 * property {@value Sessiedump#EIGENSCHAP}), on both branches, also where the fire reaches no fixpoint (lens on step 6a
 * of card drl-teksttak-op-truth-maintenance). The comparison of the two branches over the dumps of the suite sees only
 * the sessions the seam writes, so a seam that skips a session of one branch hides that session from the comparison
 * without a sign: the count of the sessions holds a floor far below the sessions the suite fires.
 *
 * <p>A child JVM, with the property set to a fresh directory, replays two documents on both branches: the red case
 * {@code runaway-recursion}, whose creation reaches the creation budget and so no fixpoint, and the golden
 * {@code jutterswet-penningdrempel}, which reaches one. Each of the four sessions leaves exactly one dump, named by the
 * hash of its document and its branch, whose line {@code fixpunt} says {@code nee} with the sentence of the budget, or
 * {@code ja}. A child JVM, because the seam reads the property once, when its class loads, and the JVM of the suite has
 * loaded it long before, with or without the property.
 *
 * <p><b>Red</b> with the mutant of the lens (not committed; the diff and the output are kept with step 6b of the card): a
 * seam that writes no dump on the DRL-text branch where {@code vuur} reports no fixpoint, so the dump of the budget case on
 * that branch was missing.
 */
class NaadWitnessTest {

    // cwd under surefire is the module dir (backends/drools); the corpus is two levels up
    private static final Path CORPUS = Path.of("..", "..", "corpus");
    private static final Path ZONDER_FIXPUNT = CORPUS.resolve("rood").resolve("runaway-recursion");
    private static final Path MET_FIXPUNT = CORPUS.resolve("golden").resolve("jutterswet-penningdrempel");

    @Test void de_naad_dumpt_elke_sessie_van_beide_takken_ook_zonder_fixpunt() throws Exception {
        Path map = Files.createTempDirectory("naad");
        Path zonderIr = ZONDER_FIXPUNT.resolve("ir.json").toAbsolutePath(), metIr = MET_FIXPUNT.resolve("ir.json")
                .toAbsolutePath();
        List<String> opdracht = List.of(Path.of(System.getProperty("java.home"), "bin", "java").toString(),
                "-D" + Sessiedump.EIGENSCHAP + "=" + map, "-cp", System.getProperty("java.class.path"),
                NaadWitnessTest.class.getName(), zonderIr.toString(),
                ZONDER_FIXPUNT.resolve("case1.json").toAbsolutePath().toString(), metIr.toString(),
                MET_FIXPUNT.resolve("case.json").toAbsolutePath().toString());
        Process p = new ProcessBuilder(opdracht).redirectErrorStream(true).start();
        String uit = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        assertEquals(0, p.waitFor(), () -> "de kind-JVM:\n" + uit);

        String budget = "case: " + TmsCreatiestratumWitnessTest.budget("werf eeuwig", 0, "Wachter");
        List<String> oordelen = new ArrayList<>(), dumps = new ArrayList<>();
        for (Path ir : List.of(zonderIr, metIr))
            for (DroolsBackend.Branch tak : TmsDocumenten.TAKKEN) {
                oordelen.add(tak + " " + ir + ": " + (ir == zonderIr ? budget : "gespeeld"));
                dumps.add(Sessiedump.hash(ir) + "." + tak.name().toLowerCase(Locale.ROOT) + ".1.txt");
            }
        assertEquals(oordelen, uit.lines().filter(r -> r.startsWith("EXEC_MODEL ") || r.startsWith("DRL_TEXT "))
                .toList(), () -> "de oordelen van de kind-JVM:\n" + uit);
        List<String> geschreven;
        try (Stream<Path> s = Files.list(map)) {
            geschreven = s.map(f -> f.getFileName().toString()).sorted().toList();
        }
        assertEquals(dumps.stream().sorted().toList(), geschreven, "de dumps van de naad, één per sessie");
        for (String d : dumps) {
            List<String> fixpunt = Files.readAllLines(map.resolve(d)).stream().filter(r -> r.startsWith("fixpunt "))
                    .toList();
            // the dump quotes a sentence with its quotes escaped, and the sentence of the budget quotes the creation
            String verwacht = d.startsWith(Sessiedump.hash(zonderIr)) ? "fixpunt nee \"" + TmsCreatiestratumWitnessTest
                    .budget("werf eeuwig", 0, "Wachter").replace("\"", "\\\"") + "\"" : "fixpunt ja";
            assertEquals(List.of(verwacht), fixpunt, () -> "de regel fixpunt van " + d);
        }
    }

    /** The child JVM of the witness: replays each pair of a document and a case of {@code args} on both branches and
     *  prints a line per session, the branch, the document and the verdict. A red case has no expectations, so it plays
     *  with none, as the witnesses of the creation budget play it. */
    public static void main(String[] args) throws Exception {
        for (int i = 0; i + 1 < args.length; i += 2) {
            Path ir = Path.of(args[i]);
            JsonNode zaak = TmsDocumenten.JSON.readTree(Files.readString(Path.of(args[i + 1])));
            if (!zaak.has("expected")) ((ObjectNode) zaak).putArray("expected");
            for (DroolsBackend.Branch tak : TmsDocumenten.TAKKEN)
                System.out.println(tak + " " + ir + ": " + TmsFoutWitnessTest.speel(ir, zaak, tak));
        }
    }
}
