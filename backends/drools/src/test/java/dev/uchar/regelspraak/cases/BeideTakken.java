package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The BRANCH MIRROR of the RUNTIME-SEMANTICS ch. 7.8 witnesses: one authored case replayed through BOTH
 * Drools branches — the exec-model branch and the DRL-text branch, the same two branches
 * {@code CaseRunner}'s two golden-level entries select — with the SAME verdict demanded of each.
 *
 * <p>WHY it exists: the ch. 7.8 standing norm was built in the exec branch alone and the witnesses replayed
 * it there alone, so for a month the DRL branch erased on an empty computation exactly what the norm
 * preserves — four corpus goldens said so and no witness did. A row that runs in one branch pins one branch.
 *
 * <p>A branch that cannot CARRY the document throws {@code CaseRunner.Excluded} out of here rather than
 * skipping: a row that silently stopped covering a branch is the failure mode this class exists to prevent.
 *
 * <p>Since card drl-teksttak-op-truth-maintenance both branches render one analysis, so where they mismatch they give
 * the same line. Until that card the DRL-text branch was a lowering of its own, and a third arm ran each row on the
 * analysis rendered as DRL text, which became the DRL-text branch at the switch-over.
 */
final class BeideTakken {
    private BeideTakken() {}

    /** Both branches MATCH the case's expectations. */
    static void groen(Path ir, JsonNode c, String boodschap) throws Exception {
        assertNull(CaseRunner.speelCase(ir, c, false), "exec-model — " + boodschap);
        assertNull(CaseRunner.speelCase(ir, c, true), "drl-tekst — " + boodschap);
    }

    /** Both branches REFUSE the document, each naming {@code fragment} — the per-form-exclusion sibling of
     *  {@link #groen}. A refusal is a diagnosis, and {@code FactModelGenerator}'s reason functions exist so that
     *  the two branches give the SAME one: a producer reading the gate report must not have to know which branch
     *  answered. A row that asserts one branch's wording pins one branch, and the other is then free to drift —
     *  which it had, on the fold join's cardinality half, until 2026-09-18. */
    static void geweigerd(Path ir, JsonNode c, String fragment, String boodschap) throws Exception {
        for (boolean viaDrl : new boolean[] { false, true }) {
            String tak = viaDrl ? "drl-tekst" : "exec-model";
            CaseRunner.Excluded e = org.junit.jupiter.api.Assertions.assertThrows(CaseRunner.Excluded.class,
                    () -> CaseRunner.speelCase(ir, c, viaDrl), tak + " — " + boodschap);
            assertTrue(e.getMessage().contains(fragment), tak + " names '" + fragment + "': " + e.getMessage());
        }
    }

    /** Both branches MISMATCH with the same line, naming {@code fragment} — the flank form: what the expectation
     *  claims is observably NOT what happened, in both branches. Returns the exec-model verdict line. */
    static String rood(Path ir, JsonNode c, String fragment, String boodschap) throws Exception {
        String exec = CaseRunner.speelCase(ir, c, false);
        assertNotNull(exec, "exec-model — " + boodschap);
        assertTrue(exec.contains(fragment), "exec-model names '" + fragment + "': " + exec);
        assertEquals(exec, CaseRunner.speelCase(ir, c, true), "drl-tekst — " + boodschap);
        return exec;
    }
}
