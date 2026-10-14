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
 * drl-teksttak-op-truth-maintenance): {@link CaseRunner#runRood(Path, Path, DroolsBackend.Branch)} on {@code DRL_TEXT}
 * fires the DRL text. The red row of {@link TakdumpsCorpusWitnessTest} demands the same {@link CaseRunner.RoodResult}
 * of both branches on every red case, and the two branches give the same result on every one, so that row cannot tell
 * which branch the entry built: an entry that builds the exec-model branch whatever it is given compares that branch
 * with itself, and the row stays green. Gate 8 drives the exec-model branch alone.
 *
 * <p>The seam of {@link CaseRunner} ({@link Sessiedump}, under the property {@value Sessiedump#EIGENSCHAP}) names the
 * dump of every session it writes after the branch of the build that fired the session
 * ({@code DroolsBackend.Build#branch}), which is the branch the entry chose. A child JVM, with the property set to a
 * fresh directory, plays the red case {@code division-by-numeric-zero}, a fault at fire, through the red entry once per
 * branch: each call leaves exactly one dump, under its own branch, and the two calls report the same
 * {@code RoodResult}. A child JVM, because the seam reads the property once, when its class loads, and the JVM of the
 * suite has loaded it long before.
 *
 * <p><b>Red</b> with the mutation of the lens, in a copy (not committed; the diff and the output are kept with step 6c
 * of the card): the entry builds with {@code build(irPath)}, the exec-model branch, whatever branch it is given. The
 * child then leaves two dumps of the exec-model branch and none of the DRL-text branch, while the red row of
 * {@link TakdumpsCorpusWitnessTest} stays green.
 */
class RodeTakkeuzeWitnessTest {

    // cwd under surefire is the module dir (backends/drools); the corpus is two levels up
    private static final Path ZAAK = Path.of("..", "..", "corpus", "rood", "division-by-numeric-zero");

    @Test void de_rode_ingang_bouwt_de_tak_die_ze_krijgt() throws Exception {
        Path map = Files.createTempDirectory("rode-takkeuze");
        Path ir = ZAAK.resolve("ir.json").toAbsolutePath();
        List<String> opdracht = List.of(Path.of(System.getProperty("java.home"), "bin", "java").toString(),
                "-D" + Sessiedump.EIGENSCHAP + "=" + map, "-cp", System.getProperty("java.class.path"),
                RodeTakkeuzeWitnessTest.class.getName(), ir.toString(),
                ZAAK.resolve("case1.json").toAbsolutePath().toString());
        Process p = new ProcessBuilder(opdracht).redirectErrorStream(true).start();
        String uit = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        assertEquals(0, p.waitFor(), () -> "de kind-JVM:\n" + uit);

        List<String> dumps = TmsDocumenten.TAKKEN.stream()
                .map(tak -> Sessiedump.hash(ir) + "." + tak.name().toLowerCase(Locale.ROOT) + ".1.txt").sorted().toList();
        List<String> geschreven;
        try (Stream<Path> s = Files.list(map)) {
            geschreven = s.map(f -> f.getFileName().toString()).sorted().toList();
        }
        assertEquals(dumps, geschreven, () -> "de dumps van de rode ingang, één per tak; de kind-JVM:\n" + uit);
        List<String> resultaten = uit.lines().filter(r -> r.startsWith("EXEC_MODEL ") || r.startsWith("DRL_TEXT "))
                .map(r -> r.substring(r.indexOf(' ') + 1)).toList();
        assertEquals(2, resultaten.size(), () -> "een resultaat per tak; de kind-JVM:\n" + uit);
        assertEquals(resultaten.get(0), resultaten.get(1), "het resultaat van de rode ingang op de twee takken");
    }

    /** The child JVM of the witness: plays the red case of {@code args} through the red entry on both branches and
     *  prints a line per call, the branch and the result. */
    public static void main(String[] args) {
        Path ir = Path.of(args[0]), zaak = Path.of(args[1]);
        for (DroolsBackend.Branch tak : TmsDocumenten.TAKKEN) {
            CaseRunner.RoodResult r = CaseRunner.runRood(ir, zaak, tak);
            System.out.println(tak + " " + r.status() + " " + r.detail());
        }
    }
}
