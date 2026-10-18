package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The red case {@code corpus/rood/datumtijdstap-buiten-bereik} through both branches. Gate 8's Drools leg replays
 * a red case on the exec-model branch alone; the date step is one runtime function that both branches call
 * ({@code RegelSpraakRuntime.rsDatumReken}), and this witness holds that each branch reaches it with the case's count
 * and faults in the sentence the case pins for the backend: the two ch. 3.8 examples in hours, plus and min, the one in
 * days, and a count of 2^64 + 1 days.
 *
 * <p>Each case is replayed as written, with its {@code expectedFault} swapped for an empty {@code expected}, so a
 * branch that plays it without a fault reports a match and the row fails on the missing exception.
 *
 * <p>RED FIRST, measured on the claim's base (main 9b9a42ad): every branch played every case without a fault —
 * "Expected java.lang.RuntimeException to be thrown, but nothing was thrown" in each of the twelve rows. The ch. 3.8
 * examples had answered +228160941-04-01T08:00, -228156894-10-02T16:00 and +164276444-06-02T00:00, and the count of
 * 2^64 + 1 days one day, 2024-01-02T00:00.
 *
 * <p>The red case {@code corpus/rood/dagsoortstap-buiten-bereik} goes through the same replay: a step to the last day
 * of the range in one rule, and one day of a kind further in the next. The day kind's step is its own runtime function
 * ({@code RegelSpraakRuntime.rsDatumTijdPlusDagsoort}), called by both branches. RED FIRST, measured on this
 * branch at 16905a6d: the exec-model and the DRL-text branch answered +146140482-05-01T00:00 without a fault. The
 * truth-maintenance branch refused day kinds by name, and the row pinned that refusal, until the card
 * tms-verdeling-en-tijdlijnen (2026-10-09), which carries them: since then its row faults as the other two do.
 *
 * <p>{@code DRL_TEXT}, the analysis of the exec-model branch rendered as DRL text (card
 * drl-teksttak-op-truth-maintenance), faults on the four cases of the calendar step as the other branches do, and on
 * the step of a day kind since its step 5, which sets the predicates of the analysis as globals of the text; until then
 * it refused the day kinds by name, and this witness pinned that refusal. <b>Red</b> on {@code 2704d81d}, before the
 * day kinds of step 5: the row of the day-kind step failed on {@code DRL_TMS} with "drl-tms: een document met
 * dagsoorten".
 */
class DateStepRangeBranchesWitnessTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    // cwd under surefire is the module dir (backends/drools); the corpus is two levels up.
    private static final Path RED = Path.of("..", "..", "corpus", "rood", "datumtijdstap-buiten-bereik");

    @Test void each_case_fails_in_each_branch_with_the_fixed_sentence() throws Exception {
        Path ir = RED.resolve("ir.json");
        List<Path> cases;
        try (Stream<Path> s = Files.list(RED)) {
            cases = s.filter(p -> p.getFileName().toString().matches("case.*\\.json")).sorted().toList();
        }
        assertEquals(4, cases.size(), "the red case's four case files: " + cases);
        for (Path casePath : cases) {
            JsonNode c = JSON.readTree(Files.readString(casePath));
            for (DroolsBackend.Branch branch : DroolsBackend.Branch.values()) fails(ir, casePath, c, branch);
        }
    }

    @Test void the_compound_day_kind_step_fails_in_the_branches_that_carry_day_kinds() throws Exception {
        Path map = RED.resolveSibling("dagsoortstap-buiten-bereik");
        Path ir = map.resolve("ir.json"), casePath = map.resolve("case1.json");
        JsonNode c = JSON.readTree(Files.readString(casePath));
        for (DroolsBackend.Branch branch : DroolsBackend.Branch.values()) fails(ir, casePath, c, branch);
    }

    /** {@code c} replayed on {@code branch} faults at fire, with the fragment it pins for the backend in the cause chain. */
    private static void fails(Path ir, Path casePath, JsonNode c, DroolsBackend.Branch branch) {
        String sentence = c.get("expectedFault").get("drools").asText();
        String row = casePath.getFileName() + " on " + branch;
        RuntimeException e = assertThrows(RuntimeException.class,
                () -> CaseRunner.playCase(ir, withoutFault(c), branch), row);
        assertFalse(e instanceof CaseRunner.Excluded, row + " faults at fire, it does not refuse the form: "
                + e.getMessage());
        assertTrue(chain(e).contains(sentence), row + " names '" + sentence + "': " + chain(e));
    }

    /** The case as written, with its {@code expectedFault} swapped for an empty {@code expected}. */
    private static ObjectNode withoutFault(JsonNode c) {
        ObjectNode z = ((ObjectNode) c).deepCopy();
        z.remove("expectedFault");
        z.set("expected", JSON.createArrayNode());
        return z;
    }

    private static String chain(Throwable t) {
        StringBuilder sb = new StringBuilder();
        for (Throwable c = t; c != null; c = c.getCause() == c ? null : c.getCause())
            sb.append(sb.length() > 0 ? " <- " : "").append(c.getClass().getSimpleName()).append(": ").append(c.getMessage());
        return sb.toString();
    }
}
