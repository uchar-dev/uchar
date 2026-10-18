package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import dev.uchar.regelspraak.loader.IrLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * The guard harness of card drl-teksttak-op-truth-maintenance: while the card rebuilds the exec-model branch's
 * lowering into one analysis with two renderings, the branch must not change. For every golden the exec-model branch
 * plays and every case of it, the harness sets the case up as the replay does ({@link CaseRunner#withSession}), fires it
 * through {@link DroolsBackend#vuur} and writes the dump of the session ({@link SessionDump}): its input, the firings, the
 * order and the count per rule, everything {@link DroolsBackend#lees} gives, and every object in working memory at the
 * fixpoint.
 * One file per golden, its cases in the order of their names. Gate 12 pins only the expectations of a case; this pins
 * every fact and every firing. A step of the card compares the dump of its code with the one before it with
 * {@code diff -r}: {@code lees}, {@code feiten} and {@code spoor-telling} must be equal, and a difference in
 * {@code spoor-volgorde} is explained. The seam in {@link CaseRunner} writes the same dump for every session the suite
 * fires through the replay, with the property {@value SessionDump#PROPERTY} set to a fresh directory:
 * {@code mvn -o -q -f backends/drools/pom.xml package -Duchar.vuurspoor=<directory>}.
 *
 * <p>A harness, not a witness and not a gate: its name ends without {@code Test}, so surefire does not run it, and no
 * gate does. Run it from the root of the repository, with the test classpath from the dependency plugin, which
 * resolves offline once the module has been built:
 * <pre>
 *   mvn -o -q -f backends/drools/pom.xml test-compile
 *   CP=$(mktemp -d)/cp.txt
 *   mvn -o -q -f backends/drools/pom.xml dependency:build-classpath -Dmdep.outputFile="$CP"
 *   java -cp "backends/drools/target/test-classes:backends/drools/target/classes:$(cat "$CP")" \
 *       dev.uchar.regelspraak.cases.FireTrace corpus/golden &lt;table&gt; &lt;fresh directory&gt; [&lt;branch&gt;]
 * </pre>
 * The table is tab-separated, one golden per line: its directory under {@code corpus/golden} and the last line the
 * jar gives for it on the exec-model branch; the goldens whose line starts with {@code PLAYED} are dumped, so a
 * comparison runs over one fixed set. The directory must be empty or absent. It prints one line with the goldens,
 * the cases, the goldens that failed and the time, and exits with 1 where a golden failed.
 *
 * <p>The branch is {@code EXEC_MODEL} without the fourth argument. With {@code DRL_TEXT} it dumps the sessions of the
 * analysis rendered as DRL text (card drl-teksttak-op-truth-maintenance), over the same goldens, so that a step compares
 * {@code lees} and {@code feiten} of each case with the dump of the exec-model branch; a golden that branch refuses gets
 * a file of one line, {@code WEIGERING} and the reason, and counts among the refused, not among the failed.
 */
public final class FireTrace {

    private FireTrace() {}

    private static final ObjectMapper JSON = new ObjectMapper();

    public static void main(String[] args) throws IOException {
        if (args.length != 3 && args.length != 4)
            throw new IllegalArgumentException("gebruik: Vuurspoor <corpus/golden> <tabel> <verse uitmap> [<tak>]");
        DroolsBackend.Branch branch = args.length == 4 ? DroolsBackend.Branch.valueOf(args[3])
                : DroolsBackend.Branch.EXEC_MODEL;
        Path golden = Path.of(args[0]);
        if (!Files.isDirectory(golden))
            throw new IllegalArgumentException("geen map " + golden.toAbsolutePath()
                    + ": draai vanuit de wortel van de repository, of geef de map corpus/golden mee");
        Path outputDirectory = Path.of(args[2]);
        if (Files.isDirectory(outputDirectory)) {
            try (Stream<Path> s = Files.list(outputDirectory)) {
                if (s.findAny().isPresent())
                    throw new IllegalArgumentException("de uitmap " + outputDirectory + " is niet leeg: elke meting schrijft naar"
                            + " een verse map");
            }
        }
        Files.createDirectories(outputDirectory);
        List<String> played = new ArrayList<>();
        for (String regel : Files.readAllLines(Path.of(args[1]))) {
            String[] columns = regel.split("\t", 2);
            if (columns.length == 2 && columns[1].startsWith("PLAYED")) played.add(columns[0]);
        }
        long begin = System.nanoTime();
        int cases = 0, fouten = 0, refused = 0;
        for (String g : played) {
            StringBuilder out = new StringBuilder();
            try {
                cases += dump(golden.resolve(g), branch, out);
            } catch (CaseRunner.Excluded | IrLoader.Refusal e) {
                // only DRL_TEXT can refuse here, for a form its rendering does not carry: on the exec-model branch
                // every golden of the table plays
                if (branch == DroolsBackend.Branch.EXEC_MODEL) {
                    fouten++;
                    out.append("FOUT ").append(e).append('\n');
                } else {
                    refused++;
                    out.setLength(0);
                    out.append("WEIGERING ").append(e instanceof IrLoader.Refusal ? "loader: " : "")
                            .append(e.getMessage().replace('\n', ' ')).append('\n');
                }
            } catch (RuntimeException | IOException e) {
                fouten++;
                out.append("FOUT ").append(e).append('\n');
            }
            Files.writeString(outputDirectory.resolve(g.replace('/', '_') + ".txt"), out.toString());
        }
        System.out.println("vuurspoor: " + played.size() + " goldens, " + cases + " cases, " + fouten + " fouten, "
                + (branch == DroolsBackend.Branch.EXEC_MODEL ? "" : refused + " geweigerd op " + branch + ", ")
                + (System.nanoTime() - begin) / 1_000_000 + " ms");
        if (fouten > 0) System.exit(1);
    }

    /** Appends the dump on {@code branch} of every case of the golden in {@code map} that is no red case, and returns their
     *  number. */
    private static int dump(Path map, DroolsBackend.Branch branch, StringBuilder out) throws IOException {
        List<Path> paths;
        try (Stream<Path> s = Files.list(map)) {
            paths = s.filter(p -> p.getFileName().toString().startsWith("case")
                    && p.getFileName().toString().endsWith(".json")).sorted().toList();
        }
        CaseRunner runner = CaseRunner.forMeasurement(map.resolve("ir.json"), branch);
        int n = 0;
        for (Path pad : paths) {
            JsonNode c = JSON.readTree(Files.readString(pad));
            if (c.has("expectedFault")) continue;
            n++;
            out.append("### ").append(pad.getFileName()).append('\n');
            runner.withSession(c, (bouw, sessie, labels) -> {
                SessionDump d = SessionDump.op(sessie, labels);
                DroolsBackend.Vuring v;
                try {
                    v = DroolsBackend.vuur(bouw, sessie);
                } catch (RuntimeException e) {
                    d.write(out, e, labels);
                    return null;
                }
                d.write(out, bouw, v, labels);
                return null;
            });
        }
        return n;
    }
}
