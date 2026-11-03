package dev.uchar.regelspraak.cases;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * A form that reads the timeline of an attribute whole and finds the asOfDate-bucket scalar of the case faults with the
 * kind EF-037 (RUNTIME-SEMANTICS ch. 8.4, 5.7b), on both branches, with the attribute and the label of the object whose
 * input it is among its parameters: a condition per period, the period arithmetic over an own attribute and over the
 * members of a sum, the base of a pro-rata part, the value and the compared timeline of a gedurende clip, a member of a
 * per-element total, and the whole-period qualifier over a comparison, a gevuld test and a gevuld test over a path. Over
 * a path the sentence names the subject of the rule ("the werf of "S1""), whose bean is not the input's, so the
 * parameters name the attribute alone. The documents are the suite fixtures that the reference interpreter and the ASP
 * backend share ({@code ref/test/fixtures/bucket-scalar}); their point read is a form this backend does not carry.
 *
 * <p><b>Red</b> on {@code 5c1ccaa47}, the code before the kind had a code: the red path of every fixture on both
 * branches gave {@code FAULT null []}, an {@code InputFormFault} without a kind.
 */
class BucketScalarCodeWitnessTest {

    private static final Path FIXTURES = Path.of("..", "..", "ref", "test", "fixtures", "bucket-scalar");

    @Test void every_place_faults_with_the_kind_and_its_parameters_on_both_branches() {
        List<String> got = new ArrayList<>();
        for (String fixture : List.of("condition", "arithmetic", "sum", "pro-rata", "clip-value", "clip-compared",
                "per-element", "qualifier-comparison", "qualifier-own", "qualifier-path"))
            for (DroolsBackend.Branch branch : DroolsBackend.Branch.values()) {
                Path dir = FIXTURES.resolve(fixture);
                CaseRunner.RedResult r = CaseRunner.runRed(dir.resolve("ir.json"), dir.resolve("case.json"), branch);
                got.add(fixture + " " + branch + " " + r.status() + " " + r.code() + " " + r.params());
            }
        List<String> expected = new ArrayList<>();
        for (String[] e : List.of(
                new String[]{"condition", "{attribute=strandloon, label=J1}"},
                new String[]{"arithmetic", "{attribute=strandloon, label=J1}"},
                new String[]{"sum", "{attribute=opbrengst, label=V1}"},
                new String[]{"pro-rata", "{attribute=onderhoudstarief, label=B1}"},
                new String[]{"clip-value", "{attribute=vergoeding, label=J1}"},
                new String[]{"clip-compared", "{attribute=controle, label=J1}"},
                new String[]{"per-element", "{attribute=opbrengst, label=V1}"},
                new String[]{"qualifier-comparison", "{attribute=wachtloon, label=J1}"},
                new String[]{"qualifier-own", "{attribute=bemanning, label=S1}"},
                new String[]{"qualifier-path", "{attribute=havengeld}"}))
            for (DroolsBackend.Branch branch : DroolsBackend.Branch.values())
                expected.add(e[0] + " " + branch + " FAULT EF-037 [" + e[1] + "]");
        assertEquals(expected, got);
    }
}
