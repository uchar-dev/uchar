package dev.uchar.regelspraak.cases;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The red CLI in a JVM of its own, as a reader runs it on one case ({@code java -cp <jar> …DroolsRedMain <ir> <case>}): one
 * row per way it ends, with its whole standard output and its exit code. Gate 8 runs it through the batch of
 * {@link DroolsBackendMain}, which {@link BatchWitnessTest} holds to {@link DroolsRedMain#run}.
 */
class DroolsRedMainWitnessTest {

    // cwd under surefire is the module dir (backends/drools); the corpus is two levels up
    private static final Path CORPUS = Path.of("..", "..", "corpus");

    private static void row(int code, String output, String... arguments) throws Exception {
        List<String> task = new ArrayList<>(List.of(Path.of(System.getProperty("java.home"), "bin", "java").toString(),
                "-XX:TieredStopAtLevel=1", "-Duchar.messages.locale=en", "-Dstdout.encoding=UTF-8", "-cp",
                System.getProperty("java.class.path"), DroolsRedMain.class.getName()));
        task.addAll(List.of(arguments));
        Process p = new ProcessBuilder(task).redirectError(ProcessBuilder.Redirect.DISCARD).start();
        String out = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        assertEquals(output, out);
        assertEquals(code, p.waitFor(), out);
    }

    private static String[] case1(String red) {
        Path directory = CORPUS.resolve("red").resolve(red);
        return new String[]{directory.resolve("ir.json").toString(), directory.resolve("case1.json").toString()};
    }

    @Test void a_fault() throws Exception {
        row(0, "FAULT EF-011 {}: ComputeFault: error in the rule \"begrenswaarde van een jutter\": the evaluationYear context"
                + " value needs an evaluation date; the case carries asOfDate null <- ComputeFault: the evaluationYear context"
                + " value needs an evaluation date; the case carries asOfDate null\n", case1("clamp-reads-bounds-in-order"));
    }

    @Test void a_refusal() throws Exception {
        row(2, "EXCLUDE DROOLS-050: role 'ligger' resolves to the to-one role 'ligger' on Schuit, whose field Schuit.ligger"
                + " another role already holds, the role 'ligger' of fact type 'pontonligging' — the bean cannot carry the"
                + " reference field (name-sharing bean limit: attribute > characteristic > reference)\n",
                case1("ambiguous-case-role-key"));
    }

    @Test void a_model() throws Exception {
        Path golden = CORPUS.resolve("golden").resolve("jutterswet-penningdrempel");
        row(1, "NOFAULT ran to a model without faulting\n", golden.resolve("ir.json").toString(),
                golden.resolve("case.json").toString());
    }

    @Test void an_error() throws Exception {
        row(3, "ERROR RuntimeException: could not read none.json: none.json\n", "none.json", "none.json");
    }

    @Test void the_usage() throws Exception {
        row(3, "", "none.json");
    }
}
