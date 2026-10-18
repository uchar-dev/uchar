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
 * Every golden of the corpus with case files, replayed through the exec-model branch as the CLI replays it without a
 * flag, against the expectations gates 5, 6 and 12 pin. A golden must play every case or be refused with a reason: a
 * document the branch carries but computes wrong is a silent wrong value, the one outcome the branch may never give.
 * The count of played goldens is held to a floor, so that a step that makes the branch drop goldens it played fails
 * here by number. Gate 12 pins the same count ({@code EXPECTED_PLAYED}) and is the authority. This witness stays in the
 * suite beside it, by decision of card tms-omschakeling: it runs in-process at every {@code mvn package}, between two
 * runs of the gate, so a golden computed wrong or a drop below the floor fails the build before the gate runs at all.
 *
 * <p>The refusals are counted per family, not pinned, and the count shows only in the message of a missed floor: the
 * kind of form the manifest refuses first ({@code transpiler SKIP: <family>: …}), a cycle refused before the build and
 * two rules of one name, which carry the label of the branch ({@code exec-model}), the loader's refusal
 * ({@code loader}), or two names on one bean member ({@code beannaambotsing}). The reasons themselves name forms and
 * snippets.
 *
 * <p><b>Red before the fix of the parameters</b> (measured 2026-10-08 on the branch's fourth step): two goldens
 * played with wrong values, "jutterswet-aanvoergeld: Result[status=FAIL, reason=, caseFailures=[case.json: attribuut
 * aanvoergeld: verwacht 322.50, werkelijk 810.00; …], casesPlayed=1], jutterswet-jaarvaststelling:
 * Result[status=FAIL, reason=, caseFailures=[case1-oudejaarsdag-2025.json: attribuut uitkering: verwacht 250,
 * werkelijk 12; attribuut afdrachtpercentage: verwacht 10, werkelijk 12; …], casesPlayed=2]"; each read a parameter of
 * one Java type that another rule read ({@code TmsParameterWitnessTest}). No other golden failed.
 */
class TmsCorpusWitnessTest {

    // cwd under surefire is the module dir (backends/drools); the corpus is two levels up
    private static final Path GOLDEN = Path.of("..", "..", "corpus", "golden");

    /** The floor of the goldens the branch plays, measured 2026-10-08 at the lowering of the defaults groups, until
     *  which it was an equality. An equality makes every corpus card that adds a golden the branch plays change this
     *  test, and parallel cards would then share one registry, which WAY-OF-WORKING §1 forbids. The floor catches the
     *  branch shrinking; what the witness is for stays the same: no golden FAILs. The refusals per family are reported
     *  in the message and not pinned, since every golden a corpus card adds would move one of them.
     *
     *  <p>Raised from 66 to 120 on purpose (card tms-aggregaties-creatie-en-fouten, acceptance (b)), measured
     *  2026-10-08 through this test after the branch took on aggregations, object and fact creation and faults as
     *  facts: 120 of the 292 goldens with cases play, none FAILs.
     *
     *  <p>Raised from 120 to 171 on purpose (card tms-expressievormen-en-rekendatum, step 1, acceptance (b)), measured
     *  2026-10-09 through this test after the branch took on the value forms, the evaluation date and the faults of
     *  date arithmetic as facts: 171 of the 294 goldens with cases play, none FAILs.
     *
     *  <p>Raised from 171 to 190 on purpose (card tms-expressievormen-en-rekendatum, step 2, acceptance (b)), measured
     *  2026-10-09 over the corpus with the jar of this step after the branch took on the quantifier fold, the role check
     *  and uniqueness: 190 of the 294 goldens with cases play, none FAILs.
     *
     *  <p>Raised from 190 to 201 on purpose (card tms-expressievormen-en-rekendatum, step 3, acceptance (b)), measured
     *  2026-10-09 over the corpus with the jar of this step after the branch took on two or more writers of one target,
     *  a computed leeg outside the standing domain and writers keyed per concrete type: 201 of the 294 goldens with
     *  cases play, none FAILs.
     *
     *  <p>Raised from 201 to 216 on purpose (card tms-verdeling-en-tijdlijnen, step 1, acceptance (b)), measured
     *  2026-10-09 over the corpus with the jar of this step after the branch took on allocations: 216 of the 294 goldens
     *  with cases play, none FAILs; the fifteen that rose are the goldens with an allocation.
     *
     *  <p>Raised from 216 to 235 on purpose (card tms-verdeling-en-tijdlijnen, step 2, acceptance (b)), measured
     *  2026-10-09 over the corpus with the jar of this step after the branch took on timelines as values, read by a
     *  total, written by a pro-rata part or a day count, and as a uniqueness key: 235 of the 294 goldens with cases
     *  play, none FAILs; the nineteen that rose are the eighteen goldens of the step and jutterswet-klipwacht.
     *
     *  <p>Raised from 235 to 248 on purpose (card tms-verdeling-en-tijdlijnen, step 3, acceptance (b)), measured
     *  2026-10-09 over the corpus with the jar of this step after the branch took on the whole-period qualifier and day
     *  kinds: 248 of the 294 goldens with cases play, none FAILs; the thirteen that rose are the goldens of the step,
     *  and the branch then played every golden the destructive exec-model branch played.
     *
     *  <p>Since card tms-omschakeling (2026-10-10) this branch is the exec-model branch, and gate 12 pins its count as an
     *  equality, {@code EXPECTED_PLAYED} 248, equal to this floor. The pin of gate 12 is the truth: where the two
     *  disagree, the gate decides, and a corpus card that adds a golden the branch plays raises the gate's pin and
     *  leaves this floor as it is. */
    private static final int EXPECTED_MIN_PLAYED = 248;

    @Test void every_golden_plays_all_its_cases_or_is_refused() throws Exception {
        List<Path> goldens;
        try (Stream<Path> s = Files.walk(GOLDEN)) {
            goldens = s.filter(p -> p.getFileName().toString().equals("ir.json")).map(Path::getParent).sorted().toList();
        }
        Map<String, Integer> count = new TreeMap<>();
        List<String> fout = new ArrayList<>();
        int withoutCases = 0;
        for (Path g : goldens) {
            List<Path> cases;
            try (Stream<Path> s = Files.list(g)) {
                cases = s.filter(f -> f.getFileName().toString().matches("case.*\\.json")).sorted().toList();
            }
            if (cases.isEmpty()) { withoutCases++; continue; }
            String naam = GOLDEN.relativize(g).toString();
            CaseRunner.Result r = CaseRunner.viaExecModel(g.resolve("ir.json"), cases);
            switch (r.status()) {
                case "PLAYED" -> count.merge("PLAYED", 1, Integer::sum);
                case "EXCLUDE" -> count.merge(family(r.reason()), 1, Integer::sum);
                default -> fout.add(naam + ": " + r);
            }
        }
        assertEquals(List.of(), fout, "goldens die de tak draagt maar verkeerd uitrekent");
        int played = count.getOrDefault("PLAYED", 0), without = withoutCases;
        assertTrue(played >= EXPECTED_MIN_PLAYED, () -> "gespeelde goldens " + played + " onder de vloer "
                + EXPECTED_MIN_PLAYED + "; per familie " + count + ", zonder cases " + without);
    }

    /** The family of a refusal: the kind of form the manifest refuses first, the label of the branch (a cycle, two
     *  rules of one name), or the loader. */
    private static String family(String reden) {
        if (reden.startsWith("transpiler SKIP: ")) {
            String rest = reden.substring("transpiler SKIP: ".length());
            int colon = rest.indexOf(':');
            return colon < 0 ? "transpiler" : rest.substring(0, colon);
        }
        if (reden.startsWith("exec-model: ")) return "exec-model";
        if (reden.startsWith("loader: ")) return "loader";
        if (reden.startsWith("beannaambotsing: ")) return "beannaambotsing";
        return "anders: " + reden;
    }
}
