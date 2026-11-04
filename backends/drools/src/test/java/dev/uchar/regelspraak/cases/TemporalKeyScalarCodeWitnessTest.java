package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * An ever-unique key whose component on a timeline the case gives as the asOfDate-bucket scalar faults with the kind
 * EF-020 (RUNTIME-SEMANTICS ch. 5.9, 5.7b), on both branches, with the key component and the label of the object
 * among its parameters, as the reference interpreter and the ASP backend fault: the union of the red case
 * {@code ever-unique-temporal-key-bucket-scalar}, and the single form of {@code uniqueness-temporal-key-scalar} with
 * the same evaluation date. Without an evaluation date the case reader refuses the scalar first (DROOLS-051), so the
 * red cases without one do not reach the key.
 */
class TemporalKeyScalarCodeWitnessTest {

    private static final Path RED = Path.of("..", "..", "corpus", "red");
    private static final ObjectMapper JSON = new ObjectMapper();

    @Test void an_ever_unique_key_given_as_the_bucket_scalar_faults_with_the_kind_on_both_branches() throws Exception {
        Path union = RED.resolve("ever-unique-temporal-key-bucket-scalar");
        Path single = RED.resolve("uniqueness-temporal-key-scalar");
        ObjectNode withDate = (ObjectNode) JSON.readTree(Files.readString(single.resolve("case1.json")));
        withDate.put("asOfDate", "2024-06-01");
        Path singleCase = Files.createTempFile("uniciteit-peildatum", ".json");
        singleCase.toFile().deleteOnExit();
        Files.writeString(singleCase, withDate.toString());
        List<String> got = new ArrayList<>();
        List<String> expected = new ArrayList<>();
        for (DroolsBackend.Branch branch : DroolsBackend.Branch.values()) {
            CaseRunner.RedResult u = CaseRunner.runRed(union.resolve("ir.json"), union.resolve("case1.json"), branch);
            got.add("union " + branch + " " + u.status() + " " + u.code() + " " + u.params());
            expected.add("union " + branch + " FAULT EF-020 [{key=vondstnummer, label=S1}]");
            CaseRunner.RedResult s = CaseRunner.runRed(single.resolve("ir.json"), singleCase, branch);
            got.add("single " + branch + " " + s.status() + " " + s.code() + " " + s.params());
            expected.add("single " + branch + " FAULT EF-020 [{key=kavelnummer, label=S1}]");
        }
        assertEquals(expected, got);
    }
}
