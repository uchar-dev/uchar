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
 * Every golden of the corpus with case files, replayed through {@code DRL_TMS}, the analysis of the exec-model branch
 * rendered as DRL text (card drl-teksttak-op-truth-maintenance, from step 2b until the switch-over), as the CLI replays
 * it with {@code --drl-tms}, against the expectations gates 5, 6 and 12 pin. A golden must play every case or be refused
 * with a reason; a golden the branch carries but computes wrong is a fault of the rendering, since the analysis is the
 * exec-model branch's. A text the Drools compiler rejects is such a fault too: it fails this witness by the name of the
 * golden, never a refusal. The count of played goldens is held to a floor, raised per step of the card as the rendering
 * carries more.
 *
 * <p>The refusals are counted per family and reported in the message of a missed floor: a form the rendering does not
 * carry ({@code drl-tms: de DRL-weergave draagt <soort> nog niet}), and the refusals the branch shares with the
 * exec-model branch, which come from the same analysis: where this branch refuses with no
 * {@code drl-tms:} sentence of its own, the exec-model branch refuses the golden in the same words.
 *
 * <p><b>Red</b> on {@code 5eda2eff}, before the branch: {@code CaseRunner.viaDrlTms} did not exist, and this class did
 * not compile. With the floor of step 3a red on {@code 4c8a687b}, before that step: 113 goldens played, under the
 * floor of 161. With the floor of step 3b red on {@code 0458b2d0}, before that step: 161 goldens played, under the
 * floor of 166. With the floor of step 4 red on {@code e2bd685e}, before that step: 166 goldens played, under the floor
 * of 203. With the floor of step 5 red on {@code b4d83a69}, before the rendering of that step: 203 goldens played, under
 * the floor of 250.
 */
class DrlTmsCorpusWitnessTest {

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

    @Test void elke_golden_speelt_al_zijn_cases_of_wordt_geweigerd() throws Exception {
        List<Path> goldens;
        try (Stream<Path> s = Files.walk(GOLDEN)) {
            goldens = s.filter(p -> p.getFileName().toString().equals("ir.json")).map(Path::getParent).sorted().toList();
        }
        Map<String, Integer> telling = new TreeMap<>();
        List<String> fout = new ArrayList<>(), anders = new ArrayList<>();
        int zonderCases = 0;
        for (Path g : goldens) {
            List<Path> zaken;
            try (Stream<Path> s = Files.list(g)) {
                zaken = s.filter(f -> f.getFileName().toString().matches("case.*\\.json")).sorted().toList();
            }
            if (zaken.isEmpty()) { zonderCases++; continue; }
            String naam = GOLDEN.relativize(g).toString();
            CaseRunner.Result r;
            try {
                r = CaseRunner.viaDrlTekst(g.resolve("ir.json"), zaken);
            } catch (RuntimeException e) {
                fout.add(naam + ": " + e);
                continue;
            }
            switch (r.status()) {
                case "PLAYED" -> telling.merge("PLAYED", 1, Integer::sum);
                case "EXCLUDE" -> {
                    telling.merge(familie(r.reason()), 1, Integer::sum);
                    // a refusal of the shared analysis is the exec-model branch's, word for word
                    if (!r.reason().startsWith("drl-tms: ")) {
                        CaseRunner.Result exec = CaseRunner.viaExecModel(g.resolve("ir.json"), zaken);
                        if (!exec.status().equals("EXCLUDE") || !exec.reason().equals(r.reason()))
                            anders.add(naam + ": drl-tms " + r.reason() + " | exec-model " + exec);
                    }
                }
                default -> fout.add(naam + ": " + r);
            }
        }
        assertEquals(List.of(), fout, "goldens die de tak draagt maar verkeerd uitrekent, of waarvan de tekst niet"
                + " compileert");
        assertEquals(List.of(), anders, "weigeringen van de gedeelde analyse die van de exec-model-tak verschillen");
        int gespeeld = telling.getOrDefault("PLAYED", 0), zonder = zonderCases;
        assertTrue(gespeeld >= EXPECTED_MIN_PLAYED, () -> "gespeelde goldens " + gespeeld + " onder de vloer "
                + EXPECTED_MIN_PLAYED + "; per familie " + telling + ", zonder cases " + zonder);
    }

    /** The family of a refusal: the form the rendering does not carry, the kind of form the manifest
     *  refuses first, the label of a branch (a cycle, two rules of one name), the loader, or two names on one bean
     *  member. */
    private static String familie(String reden) {
        if (reden.startsWith("drl-tms: de DRL-weergave draagt ")) {
            int nogNiet = reden.indexOf(" nog niet");
            return nogNiet < 0 ? reden : reden.substring(0, nogNiet);
        }
        if (reden.startsWith("transpiler SKIP: ")) {
            String rest = reden.substring("transpiler SKIP: ".length());
            int dubbelepunt = rest.indexOf(':');
            return dubbelepunt < 0 ? "transpiler" : rest.substring(0, dubbelepunt);
        }
        if (reden.startsWith("drl-tms: ")) return "drl-tms";
        // a cycle, refused by the shared analysis in its own words
        if (reden.startsWith("exec-model: ")) return "exec-model";
        if (reden.startsWith("loader: ")) return "loader";
        if (reden.startsWith("beannaambotsing: ")) return "beannaambotsing";
        return "anders: " + reden;
    }
}
