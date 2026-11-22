package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import dev.uchar.regelspraak.loader.IrLoader;
import dev.uchar.regelspraak.refusal.DroolsCode;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Complete knowledge gives the ordinary outcome (RUNTIME-SEMANTICS ch. 1.5): every golden that does not carry
 * {@code inferFromEmpty}, with {@code inferFromEmpty: false} added and played in sessions that treat every input member as
 * complete ({@link Input#closedInputs}), plays its own cases on both branches. Gate 5 holds the reference interpreter to
 * the same on the goldens it carries. The rules here are those of open values, which a document with the property gets
 * whatever its input, so a standing, a stack or a fold that reads a settled leeg value as open shows here.
 *
 * <p>No golden is named. One this backend refuses with the property is refused with DROOLS-370, the code of a form whose
 * reading of ch. 1.5 it does not carry, or with the code it is refused with without the property. The floors hold how many
 * goldens and cases play, measured 2026-11-22; a refusal that grows falls below them.
 *
 * <p><b>Red</b> in sessions that do not treat their input as complete ({@code playCases(…, false)}): a member the case does
 * not name is open, and so is what reads it.
 */
class CompleteInputPreservationWitnessTest {

    private static final Path GOLDEN = Path.of("..", "..", "corpus", "golden");

    /** The goldens that play with the property, and their cases. */
    private static final int EXPECTED_MIN_GOLDENS = 237, EXPECTED_MIN_CASES = 691;

    @Test void a_golden_with_the_property_and_complete_input_plays_its_cases(@TempDir Path out) throws Exception {
        List<Path> goldens;
        try (Stream<Path> s = Files.walk(GOLDEN)) {
            goldens = s.filter(p -> p.getFileName().toString().equals("ir.json")).sorted().toList();
        }
        List<String> differences = new ArrayList<>();
        int played = 0, cases = 0, uncarried = 0;
        for (Path golden : goldens) {
            ObjectNode document = (ObjectNode) TmsDocuments.JSON.readTree(golden.toFile());
            List<JsonNode> contents = cases(golden.getParent());
            if (document.has("inferFromEmpty") || contents.isEmpty()) continue;
            document.put("inferFromEmpty", false);
            Path ir = out.resolve(goldens.indexOf(golden) + ".json");
            TmsDocuments.JSON.writeValue(ir.toFile(), document);
            List<DroolsCode> refusals = new ArrayList<>();
            for (DroolsBackend.Branch branch : DroolsBackend.Branch.values()) {
                DroolsCode refused = null;
                try {
                    List<String> verdicts = CaseRunner.playCases(ir, contents, branch, true);
                    for (int i = 0; i < verdicts.size(); i++)
                        if (verdicts.get(i) != null) differences.add(branch + " " + golden + " case " + i + ": " + verdicts.get(i));
                } catch (CaseRunner.Excluded e) {
                    refused = e.code();
                } catch (IrLoader.Refusal e) {
                    refused = e.code();
                }
                if (refused != null && refused != DroolsCode.INFER_FROM_EMPTY_FALSE && refused != refusal(golden, branch))
                    differences.add(branch + " " + golden + " refused with " + refused);
                refusals.add(refused);
            }
            assertEquals(refusals.get(0), refusals.get(1), golden::toString);
            if (refusals.get(0) == DroolsCode.INFER_FROM_EMPTY_FALSE) uncarried++;
            if (refusals.get(0) != null) continue;
            played++;
            cases += contents.size();
        }
        assertEquals(List.of(), differences);
        assertTrue(played >= EXPECTED_MIN_GOLDENS && cases >= EXPECTED_MIN_CASES,
                played + " goldens play, " + cases + " cases, " + uncarried + " goldens DROOLS-370");
    }

    /** The code {@code golden} is refused with as it stands, null where it is built. */
    private static DroolsCode refusal(Path golden, DroolsBackend.Branch branch) throws Exception {
        try {
            DroolsBackend.build(golden, branch, false);
            return null;
        } catch (CaseRunner.Excluded e) {
            return e.code();
        } catch (IrLoader.Refusal e) {
            return e.code();
        }
    }

    private static List<JsonNode> cases(Path dir) throws Exception {
        List<JsonNode> out = new ArrayList<>();
        try (Stream<Path> s = Files.list(dir)) {
            for (Path c : s.filter(p -> p.getFileName().toString().matches("case.*\\.json")).sorted().toList())
                out.add(TmsDocuments.JSON.readTree(c.toFile()));
        }
        return out;
    }
}
