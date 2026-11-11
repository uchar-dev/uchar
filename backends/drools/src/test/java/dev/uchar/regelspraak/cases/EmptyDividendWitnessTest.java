package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * A leeg dividend gives the quotient before the divisor is read (RUNTIME-SEMANTICS ch. 1.4 point 4): the divisor is not
 * evaluated, so a fault or deposit in it does not fall, and its reading is {@code unreached} (ch. 5.12). The document
 * {@code empty-dividend} computes "loon = bonus ÷ (opbrengst ÷ aandeel)": case 1, bonus leeg with 90 ÷ 0 in the divisor,
 * writes 0 and is consistent; case 2, 60 ÷ (90 ÷ 3), writes 2; case 3, 60 ÷ (90 ÷ 0), writes leeg with the EF-005 of the
 * divisor and the EF-031 of the outer division. {@code ref-trace-case1.txt} is the reference interpreter's
 * {@code runner --trace} on case 1 with its divisor read lazily.
 *
 * <p>Red before: case 1 deposited the EF-005 of the divisor on both branches, and the readings of the divisor and its
 * operands were their values instead of {@code unreached}.
 */
class EmptyDividendWitnessTest {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Path DOCUMENT = Path.of("src", "test", "resources", "empty-dividend");

    @Test void a_leeg_dividend_does_not_read_its_divisor() throws Exception {
        play("case1-leeg-deeltal.json");
    }

    @Test void a_filled_dividend_reads_its_divisor() throws Exception {
        play("case2-gevuld-deeltal.json");
        play("case3-gevuld-deeltal-nuldeler.json");
    }

    @Test void the_divisor_under_a_leeg_dividend_is_unreached() throws Exception {
        List<String> trace = Files.readAllLines(DOCUMENT.resolve("ref-trace-case1.txt"));
        for (DroolsBackend.Branch branch : DroolsBackend.Branch.values()) {
            List<String> readings = CaseRunner.readings(DOCUMENT.resolve("ir.json"),
                    List.of(DOCUMENT.resolve("case1-leeg-deeltal.json")), branch).stream()
                    .filter(l -> l.startsWith("reading ")).toList();
            assertEquals(trace, readings, () -> "op " + branch);
        }
    }

    private static void play(String caseFile) throws Exception {
        JsonNode c = JSON.readTree(Files.readString(DOCUMENT.resolve(caseFile)));
        for (DroolsBackend.Branch branch : DroolsBackend.Branch.values())
            assertNull(CaseRunner.playCase(DOCUMENT.resolve("ir.json"), c, branch), () -> caseFile + " op " + branch);
    }
}
