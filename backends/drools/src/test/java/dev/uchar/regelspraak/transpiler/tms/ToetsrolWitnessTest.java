package dev.uchar.regelspraak.transpiler.tms;

import dev.uchar.regelspraak.cases.DroolsBackend;
import dev.uchar.regelspraak.transpiler.tms.plan.LhsItem;
import dev.uchar.regelspraak.transpiler.tms.plan.Linkerkant;
import dev.uchar.regelspraak.transpiler.tms.plan.Toetsitem;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The test of a rule's left-hand side carries its role in the plan (card drl-teksttak-op-truth-maintenance, step 1b):
 * what it asks of the condition, which the analysis decides and a rendering into text cannot tell from the condition
 * itself. One compiled condition stands behind the test of a form's rule, the rule of its fault and the rule of its
 * deposit; the role says that the first asks that it holds without a fault ({@code ZONDER_FOUT}), the second that it
 * faults ({@code FOUT}) and the third that it reaches a place that deposits, heard by the probe in its slot
 * ({@code DEPONERING}). This witness pins the role of every planned test, in plan order, for two goldens; that the
 * rendering of the roles tests what the wrapped conditions tested before the step is what the harness
 * {@code Vuurspoor} shows.
 *
 * <ul>
 *   <li>{@code jutterswet-filterwacht}: a filter that divides, so that it can fault and deposit, in two folds, one read
 *       in the value of "zware vondsten", whose condition cannot fault, and one in the condition of "drukte". Each
 *       fold's contribution rule tests the filter without its fault, beside the rules of its filter fault and its
 *       filter deposit; "drukte" has the three roles once more for its own condition, which reaches the fold.</li>
 *   <li>{@code jutterswet-vondstnummer}: a consistency, whose test is the negation of its assertion, and a uniqueness,
 *       whose key rule tests whether the key takes part, a test the form composes itself ({@code VORM}).</li>
 * </ul>
 *
 * <p><b>Red</b> before the step: a test carried only its compiled predicate, the wrappers of the analysis included,
 * and no role.
 */
class ToetsrolWitnessTest {

    private static final Path GOLDEN = Path.of("..", "..", "corpus", "golden");

    @Test void een_toets_draagt_zijn_rol_naast_de_voorwaarde() throws Exception {
        assertEquals(List.of(
                "vondstental: geen toets",
                "vondstental: geen toets",
                "zware vondsten: ZONDER_FOUT",
                "zware vondsten: FOUT",
                "zware vondsten: DEPONERING sonde 2",
                "zware vondsten: VOORWAARDE",
                "drukte: ZONDER_FOUT",
                "drukte: FOUT",
                "drukte: DEPONERING sonde 2",
                "drukte: ZONDER_FOUT",
                "drukte: FOUT",
                "drukte: DEPONERING sonde 4"), rollen("jutterswet-filterwacht"));
    }

    @Test void een_toets_die_de_vorm_zelf_samenstelt_heeft_de_rol_vorm() throws Exception {
        assertEquals(List.of(
                "aanmelding niet voor de vondstdag: VOORWAARDE",
                "eenmalig vondstnummer: VORM"), rollen("jutterswet-vondstnummer"));
    }

    /** Per planned left-hand side, in plan order, the role of its test. */
    private static List<String> rollen(String golden) throws Exception {
        List<Linkerkant> plannen = DroolsBackend.build(GOLDEN.resolve(golden).resolve("ir.json"),
                DroolsBackend.Branch.EXEC_MODEL).execModelTranspiler().linkerkanten();
        return plannen.stream().map(k -> k.vorm() + ": " + rol(k.items())).toList();
    }

    private static String rol(List<LhsItem> items) {
        for (LhsItem item : items)
            if (item instanceof Toetsitem t) return t.rol() + (t.sonde() < 0 ? "" : " sonde " + t.sonde());
        return "geen toets";
    }
}
