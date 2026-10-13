package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;

import java.nio.file.Path;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
 *
 * <p>A third arm, until the switch-over of card drl-teksttak-op-truth-maintenance (step 6): {@code DRL_TMS}, the
 * analysis of the exec-model branch rendered as DRL text, gives on every row the verdict of the exec-model branch, of
 * which it is the second rendering: a match where that branch matches, the same mismatch line where it mismatches, and
 * a refusal that names the same fragment where it refuses. A row whose document {@code DRL_TMS} refuses for a kind its
 * rendering does not carry yet, one of {@link #NOG_NIET}, does not run on it yet; any other refusal of that branch
 * fails the row. Each step of the card takes the kinds it carries off the list, and every row refused for them then
 * runs on {@code DRL_TMS}.
 */
final class BeideTakken {
    private BeideTakken() {}

    /** The kinds {@code DRL_TMS} does not carry yet, each with the step of card drl-teksttak-op-truth-maintenance that
     *  brings it: the consequences its rendering refuses by their simple names, and the day kinds, whose predicates
     *  the branch does not set as globals yet. Measured at step 2b; the kinds of the rules of a fold left at step 3a,
     *  which carries them, so that every row of a fold or a quantifier fold runs on {@code DRL_TMS}, those of a
     *  uniqueness and of the §9.5 channel at step 3b, so that every row of a uniqueness runs there too, those of a
     *  defaults stack at step 4, and those of a creation, a link and an allocation and the day kinds at step 5, after
     *  which the branch carries every kind and the list is empty. */
    static final Map<String, String> NOG_NIET = Map.ofEntries();

    private static final Pattern DRAAGT_NOG_NIET = Pattern.compile("drl-tms: de DRL-weergave draagt (\\w+) nog niet");

    /** The verdict of a row {@code DRL_TMS} does not carry yet, compared by identity. */
    private static final String NIET_GEDRAGEN = new String("niet gedragen");

    /** Whether {@code reden}, a refusal of {@code DRL_TMS}, refuses a kind of {@link #NOG_NIET}. */
    static boolean nogNiet(String reden) {
        Matcher m = DRAAGT_NOG_NIET.matcher(reden);
        return m.matches() && NOG_NIET.containsKey(m.group(1));
    }

    /** The verdict of {@code DRL_TMS} on the row, or {@link #NIET_GEDRAGEN} where it refuses the document for a kind of
     *  {@link #NOG_NIET}; any other refusal leaves here. */
    private static String drlTms(Path ir, JsonNode c) throws Exception {
        try {
            return CaseRunner.speelCase(ir, c, DroolsBackend.Branch.DRL_TMS);
        } catch (CaseRunner.Excluded e) {
            if (nogNiet(e.getMessage())) return NIET_GEDRAGEN;
            throw e;
        }
    }

    /** The third arm of a refusal row: {@code DRL_TMS} refuses, naming {@code fragment}, unless it refuses for a kind
     *  of {@link #NOG_NIET}. */
    private static void drlTmsWeigert(Path ir, JsonNode c, String fragment, String boodschap) {
        CaseRunner.Excluded e = org.junit.jupiter.api.Assertions.assertThrows(CaseRunner.Excluded.class,
                () -> CaseRunner.speelCase(ir, c, DroolsBackend.Branch.DRL_TMS), "drl-tms — " + boodschap);
        if (!nogNiet(e.getMessage()))
            assertTrue(e.getMessage().contains(fragment), "drl-tms names '" + fragment + "': " + e.getMessage());
    }

    /** Both branches MATCH the case's expectations. */
    static void groen(Path ir, JsonNode c, String boodschap) throws Exception {
        assertNull(CaseRunner.speelCase(ir, c, false), "exec-model — " + boodschap);
        assertNull(CaseRunner.speelCase(ir, c, true), "drl-tekst — " + boodschap);
        String drlTms = drlTms(ir, c);
        if (drlTms != NIET_GEDRAGEN) assertNull(drlTms, "drl-tms — " + boodschap);
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
        drlTmsWeigert(ir, c, fragment, boodschap);
    }

    /** Both branches REFUSE the document where they name the form in their own words: the exec-model branch
     *  {@code tms}, after the prefix of its manifest, and the DRL-text branch {@code drl}. The row of {@link #geweigerd}
     *  for a form whose refusal the exec-model branch words apart from the DRL-text branch. */
    static void geweigerdApart(Path ir, JsonNode c, String tms, String drl, String boodschap) throws Exception {
        CaseRunner.Excluded e = org.junit.jupiter.api.Assertions.assertThrows(CaseRunner.Excluded.class,
                () -> CaseRunner.speelCase(ir, c, DroolsBackend.Branch.EXEC_MODEL), "tms — " + boodschap);
        assertTrue(e.getMessage().contains(tms), "tms names '" + tms + "': " + e.getMessage());
        e = org.junit.jupiter.api.Assertions.assertThrows(CaseRunner.Excluded.class,
                () -> CaseRunner.speelCase(ir, c, true), "drl-tekst — " + boodschap);
        assertTrue(e.getMessage().contains(drl), "drl-tekst names '" + drl + "': " + e.getMessage());
        drlTmsWeigert(ir, c, tms, boodschap);
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
        String drlTms = drlTms(ir, c);
        if (drlTms != NIET_GEDRAGEN) assertEquals(exec, drlTms, "drl-tms — " + boodschap);
        return exec;
    }
}
