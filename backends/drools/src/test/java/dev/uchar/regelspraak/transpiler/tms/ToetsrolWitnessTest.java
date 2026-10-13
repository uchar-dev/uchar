package dev.uchar.regelspraak.transpiler.tms;

import dev.uchar.regelspraak.cases.DroolsBackend;
import dev.uchar.regelspraak.transpiler.tms.plan.LhsItem;
import dev.uchar.regelspraak.transpiler.tms.plan.Regelplan;
import dev.uchar.regelspraak.transpiler.tms.plan.Toetsitem;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The test of a rule's left-hand side carries its role in the plan (card drl-teksttak-op-truth-maintenance, step 1b):
 * what it asks of the condition, which the analysis decides and a rendering into text cannot tell from the condition
 * itself. One compiled condition stands behind the test of a form's rule, the rule of its fault and the rule of its
 * deposit; the role says that the first asks that it holds without a fault ({@code ZONDER_FOUT}), the second that it
 * faults ({@code FOUT}) and the third that it reaches a place that deposits, heard by the probe in its slot
 * ({@code DEPONERING}). This witness pins the role of every test of every rule of a form, in the order of the model,
 * for two goldens; that the rendering of the roles tests what the wrapped conditions tested before the step is what
 * the harness {@code Vuurspoor} shows.
 *
 * <ul>
 *   <li>{@code jutterswet-filterwacht}: a filter that divides, so that it can fault and deposit, in two folds, one read
 *       in the value of "zware vondsten", whose condition cannot fault, and one in the condition of "drukte". "drukte"
 *       has the three roles for its own condition, which reaches the fold; each fold's contribution rule, after the
 *       rules of the forms, tests the filter without its fault, beside the rules of its filter fault and its filter
 *       deposit.</li>
 *   <li>{@code jutterswet-vondstnummer}: a consistency, whose test is the negation of its assertion, and a uniqueness,
 *       whose key rule tests whether the key takes part and whose rule over the keys tests whether a key is taken
 *       twice, both tests the form composes itself ({@code VORM}); its channel rule tests nothing.</li>
 * </ul>
 *
 * <p>Since step 1d the witness reads the rule plans of the analysis ({@code Modelplan}). Until then it read the
 * planned left-hand sides in the order they were built, and only those of rules whose reads {@code Lezingen} planned;
 * the roles are the same, the contribution rules of the folds now stand after the rules of the forms, and the rule
 * over the keys and the channel rule of the uniqueness are pinned as well.
 *
 * <p><b>Red</b> before step 1b: a test carried only its compiled predicate, the wrappers of the analysis included,
 * and no role.
 */
class ToetsrolWitnessTest {

    private static final Path GOLDEN = Path.of("..", "..", "corpus", "golden");

    @Test void een_toets_draagt_zijn_rol_naast_de_voorwaarde() throws Exception {
        assertEquals(List.of(
                "vondstental: geen toets",
                "zware vondsten: VOORWAARDE",
                "drukte: ZONDER_FOUT",
                "drukte: FOUT",
                "drukte: DEPONERING sonde 4",
                "vondstental: geen toets",
                "zware vondsten: ZONDER_FOUT",
                "zware vondsten: FOUT",
                "zware vondsten: DEPONERING sonde 2",
                "drukte: ZONDER_FOUT",
                "drukte: FOUT",
                "drukte: DEPONERING sonde 2"), rollen("jutterswet-filterwacht"));
    }

    @Test void een_toets_die_de_vorm_zelf_samenstelt_heeft_de_rol_vorm() throws Exception {
        assertEquals(List.of(
                "aanmelding niet voor de vondstdag: VOORWAARDE",
                "eenmalig vondstnummer: VORM",
                "eenmalig vondstnummer: VORM",
                "eenmalig vondstnummer: geen toets"), rollen("jutterswet-vondstnummer"));
    }

    /** Per rule of a form, in the order of the model, the role of each of its tests, or that it has none. */
    private static List<String> rollen(String golden) throws Exception {
        List<String> uit = new ArrayList<>();
        for (Regelplan p : DroolsBackend.build(GOLDEN.resolve(golden).resolve("ir.json"),
                DroolsBackend.Branch.EXEC_MODEL).execModelTranspiler().modelplan().regels()) {
            if (p.vorm() == null) continue;
            boolean getoetst = false;
            for (LhsItem item : p.linkerkant())
                if (item instanceof Toetsitem t) {
                    uit.add(p.vorm() + ": " + t.rol() + (t.sonde() < 0 ? "" : " sonde " + t.sonde()));
                    getoetst = true;
                }
            if (!getoetst) uit.add(p.vorm() + ": geen toets");
        }
        return uit;
    }
}
