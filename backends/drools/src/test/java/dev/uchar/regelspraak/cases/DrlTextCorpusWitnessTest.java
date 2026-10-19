package dev.uchar.regelspraak.cases;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Every golden of the corpus with case files, replayed through the DRL-text branch ({@code DRL_TEXT}), the analysis of
 * the exec-model branch rendered as DRL text (card drl-teksttak-op-truth-maintenance), as the CLI replays it with
 * {@code --drl}, against the expectations gates 5, 6 and 12 pin. A golden must play every case or be refused with a
 * reason; a golden the branch carries but computes wrong is a fault of the rendering, since the analysis is the
 * exec-model branch's. A text the Drools compiler rejects is such a fault too: it fails this witness by the name of the
 * golden, never a refusal. The count of played goldens is held to a floor, raised per step of the card as the rendering
 * carried more.
 *
 * <p>The refusals are counted per family and reported in the message of a missed floor: a form the rendering does not
 * carry ({@code drl: de DRL-weergave draagt <soort> nog niet}), and the refusals the branch shares with the
 * exec-model branch, which come from the same analysis: where this branch refuses with no
 * {@code drl:} sentence of its own, the exec-model branch refuses the golden in the same words.
 *
 * <p><b>Red</b> on {@code 5eda2eff}, before the branch: {@code CaseRunner.viaDrlTms} did not exist, and this class did
 * not compile. With the floor of step 3a red on {@code 4c8a687b}, before that step: 113 goldens played, under the
 * floor of 161. With the floor of step 3b red on {@code 0458b2d0}, before that step: 161 goldens played, under the
 * floor of 166. With the floor of step 4 red on {@code e2bd685e}, before that step: 166 goldens played, under the floor
 * of 203. With the floor of step 5 red on {@code b4d83a69}, before the rendering of that step: 203 goldens played, under
 * the floor of 250. Until the switch-over (step 6 of the card) the analysis rendered as DRL text was a third branch,
 * {@code DRL_TMS} with the flag {@code --drl-tms}, beside the destructive DRL-text lowering, and this class, then
 * {@code DrlTmsCorpusWitnessTest}, replayed that branch; since then it is the DRL-text branch.
 */
class DrlTextCorpusWitnessTest {

    // cwd under surefire is the module dir (backends/drools); the corpus is two levels up
    private static final Path GOLDEN = Path.of("..", "..", "corpus", "golden");

    /** The floor of the goldens the branch plays, measured 2026-10-13 at step 2b: 113 of the 295 goldens with cases,
     *  the 117 whose text the rendering gives and Drools compiles (step 2a) less the four with day kinds; the acceptance
     *  of the step asks at least 88. A later step that carries more raises it on purpose: step 3a, which carries the
     *  rules of a fold, to the 161 measured then, the 113 and the 48 goldens it refused for a contribution only; step
     *  3b, which carries those of a uniqueness and of the channel, to the 166 measured then, with the five goldens it
     *  refused for the keys of a uniqueness only; step 4, which carries those of a defaults stack, to the 203 measured
     *  then, with the 37 goldens it refused for a decision of a default only; and step 5, which carries the creations,
     *  the links, the allocations and the day kinds, to the 250 measured then, every golden the exec-model branch
     *  plays. */
    private static final int EXPECTED_MIN_PLAYED = 250;

    @Test void every_golden_plays_all_its_cases_or_is_refused() throws Exception {
        List<Path> goldens;
        try (Stream<Path> s = Files.walk(GOLDEN)) {
            goldens = s.filter(p -> p.getFileName().toString().equals("ir.json")).map(Path::getParent).sorted().toList();
        }
        Map<String, Integer> count = new TreeMap<>();
        List<String> fault = new ArrayList<>(), differences = new ArrayList<>();
        int withoutCases = 0;
        for (Path g : goldens) {
            List<Path> cases;
            try (Stream<Path> s = Files.list(g)) {
                cases = s.filter(f -> f.getFileName().toString().matches("case.*\\.json")).sorted().toList();
            }
            if (cases.isEmpty()) { withoutCases++; continue; }
            String name = GOLDEN.relativize(g).toString();
            CaseRunner.Result r;
            try {
                r = CaseRunner.viaDrlText(g.resolve("ir.json"), cases);
            } catch (RuntimeException e) {
                fault.add(name + ": " + e);
                continue;
            }
            switch (r.status()) {
                case "PLAYED" -> count.merge("PLAYED", 1, Integer::sum);
                case "EXCLUDE" -> {
                    count.merge(family(r.reason()), 1, Integer::sum);
                    // a refusal of the shared analysis is the exec-model branch's, word for word
                    if (!r.reason().startsWith("drl: ")) {
                        CaseRunner.Result exec = CaseRunner.viaExecModel(g.resolve("ir.json"), cases);
                        if (!exec.status().equals("EXCLUDE") || !exec.reason().equals(r.reason()))
                            differences.add(name + ": drl " + r.reason() + " | exec-model " + exec);
                    }
                }
                default -> fault.add(name + ": " + r);
            }
        }
        assertEquals(List.of(), fault, "goldens die de tak draagt maar verkeerd uitrekent, of waarvan de tekst niet"
                + " compileert");
        assertEquals(List.of(), differences, "weigeringen van de gedeelde analyse die van de exec-model-tak verschillen");
        int played = count.getOrDefault("PLAYED", 0), without = withoutCases;
        assertTrue(played >= EXPECTED_MIN_PLAYED, () -> "gespeelde goldens " + played + " onder de vloer "
                + EXPECTED_MIN_PLAYED + "; per familie " + count + ", zonder cases " + without);
    }

    /** The family of a refusal: the form the rendering does not carry, the kind of form the manifest
     *  refuses first, the label of a branch (a cycle, two rules of one name), the loader, or two names on one bean
     *  member. */
    private static String family(String reason) {
        if (reason.startsWith("drl: de DRL-weergave draagt ")) {
            int stillNot = reason.indexOf(" nog niet");
            return stillNot < 0 ? reason : reason.substring(0, stillNot);
        }
        if (reason.startsWith("transpiler SKIP: ")) {
            String rest = reason.substring("transpiler SKIP: ".length());
            int colon = rest.indexOf(':');
            return colon < 0 ? "transpiler" : rest.substring(0, colon);
        }
        if (reason.startsWith("drl: ")) return "drl";
        // a cycle, refused by the shared analysis in its own words
        if (reason.startsWith("exec-model: ")) return "exec-model";
        if (reason.startsWith("loader: ")) return "loader";
        if (reason.startsWith("beannaambotsing: ")) return "beannaambotsing";
        return "anders: " + reason;
    }
}
