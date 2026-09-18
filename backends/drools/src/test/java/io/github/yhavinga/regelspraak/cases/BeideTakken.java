package io.github.yhavinga.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The BRANCH MIRROR of the RUNTIME-SEMANTICS ch. 7.8 witnesses: one authored case replayed through BOTH
 * Drools transpiler branches — the exec-model back-end and the classic DRL-text build, the same two branches
 * {@code CaseRunner}'s two golden-level entries select — with the SAME verdict demanded of each.
 *
 * <p>WHY it exists: the ch. 7.8 standing norm was built in the exec branch alone and the witnesses replayed
 * it there alone, so for a month the DRL branch erased on an empty computation exactly what the norm
 * preserves — four corpus goldens said so and no witness did. A row that runs in one branch pins one branch.
 *
 * <p>A branch that cannot CARRY the document throws {@code CaseRunner.Excluded} out of here rather than
 * skipping: a row that silently stopped covering a branch is the failure mode this class exists to prevent.
 */
final class BeideTakken {
    private BeideTakken() {}

    /** Both branches MATCH the case's expectations. */
    static void groen(Path ir, JsonNode c, String boodschap) throws Exception {
        assertNull(CaseRunner.speelCase(ir, c, false), "exec-model — " + boodschap);
        assertNull(CaseRunner.speelCase(ir, c, true), "drl-tekst — " + boodschap);
    }

    /** Both branches MISMATCH, each naming {@code fragment} — the flank form: what the expectation claims is
     *  observably NOT what happened, in both branches. Returns the exec-model verdict line. */
    static String rood(Path ir, JsonNode c, String fragment, String boodschap) throws Exception {
        String exec = CaseRunner.speelCase(ir, c, false);
        String drl = CaseRunner.speelCase(ir, c, true);
        assertNotNull(exec, "exec-model — " + boodschap);
        assertNotNull(drl, "drl-tekst — " + boodschap);
        assertTrue(exec.contains(fragment), "exec-model names '" + fragment + "': " + exec);
        assertTrue(drl.contains(fragment), "drl-tekst names '" + fragment + "': " + drl);
        return exec;
    }
}
