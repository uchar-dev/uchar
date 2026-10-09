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
 * Every golden of the corpus with case files, replayed through the truth-maintenance branch as the CLI's {@code --tms}
 * flag replays it, against the expectations gates 5, 6 and 12 pin. A golden must play every case or be refused with a
 * reason: a document the branch carries but computes wrong is a silent wrong value, the one outcome the branch may
 * never give, and no gate sweeps this branch. The count of played goldens is held to a floor, so that a step that
 * makes the branch drop goldens it played fails here by number.
 *
 * <p>The refusals are counted per family and reported, not pinned: the kind of form the manifest refuses first
 * ({@code tms-transpiler SKIP: <family>: …}), a cycle refused before the build ({@code tms}), the loader's refusal
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
     *  date arithmetic as facts: 171 of the 294 goldens with cases play, none FAILs. */
    private static final int EXPECTED_MIN_PLAYED = 171;

    @Test void elke_golden_speelt_al_zijn_cases_of_wordt_geweigerd() throws Exception {
        List<Path> goldens;
        try (Stream<Path> s = Files.walk(GOLDEN)) {
            goldens = s.filter(p -> p.getFileName().toString().equals("ir.json")).map(Path::getParent).sorted().toList();
        }
        Map<String, Integer> telling = new TreeMap<>();
        List<String> fout = new ArrayList<>();
        int zonderCases = 0;
        for (Path g : goldens) {
            List<Path> zaken;
            try (Stream<Path> s = Files.list(g)) {
                zaken = s.filter(f -> f.getFileName().toString().matches("case.*\\.json")).sorted().toList();
            }
            if (zaken.isEmpty()) { zonderCases++; continue; }
            String naam = GOLDEN.relativize(g).toString();
            CaseRunner.Result r = CaseRunner.viaTruthMaintenance(g.resolve("ir.json"), zaken);
            switch (r.status()) {
                case "PLAYED" -> telling.merge("PLAYED", 1, Integer::sum);
                case "EXCLUDE" -> telling.merge(familie(r.reason()), 1, Integer::sum);
                default -> fout.add(naam + ": " + r);
            }
        }
        assertEquals(List.of(), fout, "goldens die de tak draagt maar verkeerd uitrekent");
        int gespeeld = telling.getOrDefault("PLAYED", 0), zonder = zonderCases;
        assertTrue(gespeeld >= EXPECTED_MIN_PLAYED, () -> "gespeelde goldens " + gespeeld + " onder de vloer "
                + EXPECTED_MIN_PLAYED + "; per familie " + telling + ", zonder cases " + zonder);
    }

    /** The family of a refusal: the kind of form the manifest refuses first, a cycle, or the loader. */
    private static String familie(String reden) {
        if (reden.startsWith("tms-transpiler SKIP: ")) {
            String rest = reden.substring("tms-transpiler SKIP: ".length());
            int dubbelepunt = rest.indexOf(':');
            return dubbelepunt < 0 ? "tms-transpiler" : rest.substring(0, dubbelepunt);
        }
        if (reden.startsWith("tms: ")) return "tms";
        if (reden.startsWith("loader: ")) return "loader";
        if (reden.startsWith("beannaambotsing: ")) return "beannaambotsing";
        return "anders: " + reden;
    }
}
