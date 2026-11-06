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
 * Nul tot een negatieve macht (card ref-macht-van-nul): a negative exponent inverts the power of its base, and the
 * inverse of a numeric zero is a division by it, leeg with one §9.5 deposit of kind EF-005 (RUNTIME-SEMANTICS ch. 1.4
 * point 4, ch. 2.2), as 1 ÷ 0 is. The document {@code power-of-zero}, authored on Jutterswet material with its
 * {@code source.rgs} beside it, raises a gewicht to the powers -1 and -0,5 under a rounding, in a rule's value, in a
 * condition and in the filter of a fold. Verifier PASS, ref PASS on both cases (case 1: V1 of gewicht 0 leeg with four
 * deposits, on V1 for the three forms over a Vondst and on J1 for the fold; case 2 without a zero, consistent). The
 * ASP backend does not translate a negative exponent.
 *
 * <p><b>Red before the fix</b> (measured 2026-11-06 on {@code 05c9bc104}): case 1 ends both branches in
 * {@code ERROR ConsequenceException … rule "lichtheid" … java.lang.ArithmeticException: Breuk met noemer 0}, the
 * runtime's inverse of zero; case 2 plays.
 */
class PowerOfZeroWitnessTest {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Path DOCUMENT = Path.of("src", "test", "resources", "power-of-zero");

    @Test void a_zero_base_under_a_negative_exponent_deposits_on_both_branches() throws Exception {
        play("case1-gewicht-nul.json");
    }

    @Test void a_document_without_a_zero_base_plays_as_before() throws Exception {
        play("case2-geen-nul.json");
    }

    /** The readings of case 1 on both branches are the trace of the reference interpreter
     *  ({@code ref-trace-case1.txt}, its {@code runner --trace}, measured 2026-11-06), line by line: the power under
     *  its rounding is not reached (RUNTIME-SEMANTICS ch. 5.12), its literal exponent reads its value. Until this card
     *  a fractional power under a rounding broke the build with readings: its node had no term, and the term of its
     *  exponent was not the one the call received. */
    @Test void the_readings_are_the_trace_of_the_reference_interpreter() throws Exception {
        List<String> trace = Files.readAllLines(DOCUMENT.resolve("ref-trace-case1.txt"));
        for (DroolsBackend.Branch branch : DroolsBackend.Branch.values()) {
            List<String> readings = CaseRunner.readings(DOCUMENT.resolve("ir.json"),
                    List.of(DOCUMENT.resolve("case1-gewicht-nul.json")), branch).stream()
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
