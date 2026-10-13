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
 * <p>{@code DRL_TMS}, the analysis of the exec-model branch rendered as DRL text (card
 * drl-teksttak-op-truth-maintenance), faults on the four cases of the calendar step as the other branches do, and
 * refuses the day kinds by name until its step 5b, which this witness pins.
 */
class DatumstapBereikTakkenWitnessTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    // cwd under surefire is the module dir (backends/drools); the corpus is two levels up.
    private static final Path ROOD = Path.of("..", "..", "corpus", "rood", "datumtijdstap-buiten-bereik");

    @Test void elk_geval_faalt_in_elke_tak_met_de_vastgezette_zin() throws Exception {
        Path ir = ROOD.resolve("ir.json");
        List<Path> gevallen;
        try (Stream<Path> s = Files.list(ROOD)) {
            gevallen = s.filter(p -> p.getFileName().toString().matches("case.*\\.json")).sorted().toList();
        }
        assertEquals(4, gevallen.size(), "the red case's four case files: " + gevallen);
        for (Path geval : gevallen) {
            JsonNode c = JSON.readTree(Files.readString(geval));
            for (DroolsBackend.Branch tak : DroolsBackend.Branch.values()) faalt(ir, geval, c, tak);
        }
    }

    @Test void de_samengestelde_dagsoortstap_faalt_in_de_takken_die_dagsoorten_dragen() throws Exception {
        Path map = ROOD.resolveSibling("dagsoortstap-buiten-bereik");
        Path ir = map.resolve("ir.json"), geval = map.resolve("case1.json");
        JsonNode c = JSON.readTree(Files.readString(geval));
        for (DroolsBackend.Branch tak : DroolsBackend.Branch.values()) {
            if (tak == DroolsBackend.Branch.DRL_TMS) weigertDagsoorten(ir, c);
            else faalt(ir, geval, c, tak);
        }
    }

    /** {@code DRL_TMS} refuses a document with day kinds by name until the analysis gives their predicates (card
     *  drl-teksttak-op-truth-maintenance, step 5b); this row then expects the fault there too. */
    private static void weigertDagsoorten(Path ir, JsonNode c) {
        CaseRunner.Excluded e = assertThrows(CaseRunner.Excluded.class,
                () -> CaseRunner.speelCase(ir, zonderFout(c), DroolsBackend.Branch.DRL_TMS), "DRL_TMS");
        assertTrue(e.getMessage().startsWith("drl-tms: een document met dagsoorten"), e.getMessage());
    }

    /** {@code c} replayed on {@code tak} faults at fire, with the fragment it pins for the backend in the cause chain. */
    private static void faalt(Path ir, Path geval, JsonNode c, DroolsBackend.Branch tak) {
        String zin = c.get("expectedFault").get("drools").asText();
        String rij = geval.getFileName() + " on " + tak;
        RuntimeException e = assertThrows(RuntimeException.class,
                () -> CaseRunner.speelCase(ir, zonderFout(c), tak), rij);
        assertFalse(e instanceof CaseRunner.Excluded, rij + " faults at fire, it does not refuse the form: "
                + e.getMessage());
        assertTrue(keten(e).contains(zin), rij + " names '" + zin + "': " + keten(e));
    }

    /** The case as written, with its {@code expectedFault} swapped for an empty {@code expected}. */
    private static ObjectNode zonderFout(JsonNode c) {
        ObjectNode z = ((ObjectNode) c).deepCopy();
        z.remove("expectedFault");
        z.set("expected", JSON.createArrayNode());
        return z;
    }

    private static String keten(Throwable t) {
        StringBuilder sb = new StringBuilder();
        for (Throwable c = t; c != null; c = c.getCause() == c ? null : c.getCause())
            sb.append(sb.length() > 0 ? " <- " : "").append(c.getClass().getSimpleName()).append(": ").append(c.getMessage());
        return sb.toString();
    }
}
