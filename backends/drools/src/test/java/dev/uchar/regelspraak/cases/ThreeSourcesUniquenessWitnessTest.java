package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A uniqueness over the union of three sources (RUNTIME-SEMANTICS ch. 5.9, rule 1), fired through both branches of the
 * analysis of card drl-teksttak-op-truth-maintenance (lens on step 3b): "De reekscode met het volgnummer van
 * Strandvondst verenigd met Wrakvondst verenigd met Drijfvondst moeten uniek zijn." The analysis carries two or more
 * sources, but until this witness no session fired the keys of a third one: the plan of the forms of
 * {@code DrlRenderingWitnessTest} has sources 0 and 1 and is only compiled, and the witnesses of a uniqueness that fire
 * have two sources at most. Each case plays on both branches, and its dump is the same on both ({@link BranchDumps}).
 *
 * <p>The document, the corpus golden {@code jutterswet-uniciteit-drie-bronnen}, is own work on Jutterswet material: a
 * key of two components, whose order counts, over three object types. In the first case S1 (Strandvondst) and D1
 * (Drijfvondst) carry the key (7, 1) and collide, while W1 (Wrakvondst) carries (1, 7); in the second D1 carries (1, 7)
 * and nothing collides. It was the card's own document in the test resources, measured by hand at fix round 3b (the
 * verifier PASS, ref and clingo PASS on both cases); since step 6b of the card it is a golden, which gates 5, 6 and 12
 * replay in the reference interpreter, the ASP backend and both branches, and its NOTE.md carries the hand
 * computation. Its second case satisfies a do-nothing executor, since "consistent" is true of an empty channel, and gate
 * 7 lists it among its survivors: what it pins is that the channel stays empty.
 *
 * <p><b>Red by mutation</b>, measured at fix round 3b on {@code 6313a221} in a copy whose rendering swaps the first two
 * components of a key of source 2 and of no other source, the mutation the lens on step 3b found no red witness for:
 * the exec-model branch played both cases, and {@code DRL_TMS} answered "case: verwacht inconsistent (eenmalige
 * reekscode @ S1), niet gevonden onder eenmalige reekscode @ D1, eenmalige reekscode @ W1" on the first and "case:
 * verwacht consistent, werkelijk inconsistent (eenmalige reekscode @ D1, eenmalige reekscode @ S1)" on the second.
 */
class ThreeSourcesUniquenessWitnessTest {

    private static final ObjectMapper JSON = new ObjectMapper();
    // cwd under surefire is the module dir (backends/drools); the corpus is two levels up
    private static final Path DOCUMENT = Path.of("..", "..", "corpus", "golden", "jutterswet-uniciteit-drie-bronnen");

    @Test void the_union_over_three_sources_plays_on_both_branches_with_equal_dumps() throws Exception {
        Path ir = DOCUMENT.resolve("ir.json");
        List<Path> cases;
        try (Stream<Path> s = Files.list(DOCUMENT)) {
            cases = s.filter(p -> p.getFileName().toString().matches("case.*\\.json")).sorted().toList();
        }
        assertEquals(2, cases.size(), "de cases van het document");
        List<String> verdicts = new ArrayList<>();
        for (Path casePath : cases)
            for (DroolsBackend.Branch branch : List.of(DroolsBackend.Branch.EXEC_MODEL, DroolsBackend.Branch.DRL_TEXT)) {
                String verdict = CaseRunner.playCase(ir, JSON.readTree(casePath.toFile()), branch);
                if (verdict != null) verdicts.add(branch + " — " + casePath.getFileName() + ": " + verdict);
            }
        assertEquals(List.of(), verdicts, "de cases die niet spelen");
        for (Path casePath : cases) {
            String which = casePath.getFileName().toString();
            String dump = BranchDumps.equal(ir, JSON.readTree(casePath.toFile()), which);
            assertTrue(dump.contains("feit UniquenessKey(uniqueness=String(\"eenmalige reekscode\"), source=Integer(2),"
                    + " element=\"D1\""), () -> "de sleutel van de derde bron staat niet in de dump van " + which + ":\n"
                    + dump);
        }
    }
}
