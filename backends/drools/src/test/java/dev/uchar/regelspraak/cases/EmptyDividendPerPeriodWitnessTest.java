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
 * A leeg dividend gives the quotient before the divisor is read per period as well (RUNTIME-SEMANTICS ch. 1.4 point 4,
 * ch. 3.4): the divisor of a division over timelines deposits only in the periods where its dividend is filled, and
 * where the dividend is leeg on every day it is not evaluated, its reading {@code unreached} (ch. 5.12). The document
 * {@code empty-dividend-per-period} computes "loon = bonus ÷ (opbrengst ÷ aandeel)" on day timelines, opbrengst 90,
 * aandeel 0 in the first quarter of 2024 and 3 in the second, and {@code ir-rounded} the same under a rounding: case 1,
 * bonus 60 in the second quarter only, is consistent; case 2, bonus 60 in both, deposits EF-005 and EF-031 of the
 * first; case 3, bonus leeg on every day, writes 0 everywhere and is consistent. {@code ref-trace-<document>-<case>.txt}
 * is the reference interpreter's {@code runner --trace} of each pair.
 */
class EmptyDividendPerPeriodWitnessTest {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Path DOCUMENT = Path.of("src", "test", "resources", "empty-dividend-per-period");
    private static final List<String> DOCUMENTS = List.of("ir", "ir-rounded");
    private static final List<String> CASES = List.of("case1-nuldeler-in-leeg-kwartaal",
            "case2-nuldeler-in-gevuld-kwartaal", "case3-leeg-deeltal-altijd");

    @Test void a_period_with_a_leeg_dividend_does_not_read_the_divisor() throws Exception {
        for (String document : DOCUMENTS)
            for (String caseFile : CASES) {
                JsonNode c = JSON.readTree(Files.readString(DOCUMENT.resolve(caseFile + ".json")));
                for (DroolsBackend.Branch branch : DroolsBackend.Branch.values())
                    assertNull(CaseRunner.playCase(DOCUMENT.resolve(document + ".json"), c, branch),
                            () -> document + " " + caseFile + " op " + branch);
            }
    }

    @Test void the_readings_are_the_reference_interpreters() throws Exception {
        for (String document : DOCUMENTS)
            for (String caseFile : CASES) {
                List<String> trace = Files.readAllLines(DOCUMENT.resolve("ref-trace-" + document + "-" + caseFile + ".txt"));
                for (DroolsBackend.Branch branch : DroolsBackend.Branch.values()) {
                    List<String> readings = CaseRunner.readings(DOCUMENT.resolve(document + ".json"),
                            List.of(DOCUMENT.resolve(caseFile + ".json")), branch).stream()
                            .filter(l -> l.startsWith("reading ")).toList();
                    assertEquals(trace, readings, () -> document + " " + caseFile + " op " + branch);
                }
            }
    }
}
