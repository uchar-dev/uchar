package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import dev.uchar.regelspraak.refusal.DroolsCode;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * The witnesses of the reference interpreter for a document with {@code inferFromEmpty: false} (RUNTIME-SEMANTICS
 * ch. 1.5, WV-1.5-witness-*), read where they stand, {@code ref/test/fixtures/strict-empty/<name>/}, with their own
 * case files and expectations: a witness whose forms this backend carries plays every case on both branches; any
 * other is refused on both branches with DROOLS-370 and the slug of the first form of {@code UncarriedForms} it holds.
 * The list names every witness once, with the one or the other.
 *
 * <p><b>Red</b> on the code before the reading was carried: every witness was refused with {@code form=document}.
 */
class OpenValuesRefWitnessTest {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Path WITNESSES = Path.of("..", "..", "ref", "test", "fixtures", "strict-empty");

    /** Per witness, null where it plays, else the slug of its refusal. */
    private static final Map<String, String> WITNESS = new LinkedHashMap<>();
    static {
        for (String plays : List.of("absent-link", "absent-link-path", "arithmetic", "characteristic", "closed-input",
                "comparison", "composite-filled-test", "compound-all", "compound-any", "compound-counting", "compound-none",
                "consistency", "defaults-characteristic", "defaults-initialisation-exception", "defaults-initialisation-only",
                "defaults-stack", "defaults-subtype",
                "empty-by-fault", "exact-digits",
                "filled-test", "filled-test-not-closure", "initialisation", "input-above-computation",
                "input-above-computation-open", "let", "listed-fold", "navigated-read", "quantifier-fold", "set-fold",
                "settled-empty",
                "settled-empty-date", "settled-empty-fold", "settled-empty-stack"))
            WITNESS.put(plays, null);
        for (String slug : List.of("fact-creation", "object-creation")) WITNESS.put(slug, slug);
        WITNESS.put("let-closure", "if-then-else");
    }

    @Test void every_witness_plays_or_is_refused_with_the_slug_of_its_form() throws Exception {
        List<String> found = new ArrayList<>();
        try (Stream<Path> s = Files.list(WITNESSES)) {
            s.filter(Files::isDirectory).forEach(p -> found.add(p.getFileName().toString()));
        }
        found.sort(null);
        assertEquals(WITNESS.keySet().stream().sorted().toList(), found, "every witness of ref, each once");
        int played = 0;
        for (Map.Entry<String, String> w : WITNESS.entrySet()) {
            Path dir = WITNESSES.resolve(w.getKey()), ir = dir.resolve("ir.json");
            List<Path> cases;
            try (Stream<Path> s = Files.list(dir)) {
                cases = s.filter(p -> p.getFileName().toString().startsWith("case")).sorted().toList();
            }
            assertFalse(cases.isEmpty(), w::getKey);
            List<JsonNode> contents = new ArrayList<>();
            for (Path c : cases) contents.add(JSON.readTree(Files.readString(c)));
            for (DroolsBackend.Branch branch : DroolsBackend.Branch.values()) {
                String what = branch + " " + w.getKey();
                if (w.getValue() != null) {
                    CaseRunner.Excluded e = assertThrows(CaseRunner.Excluded.class,
                            () -> CaseRunner.playCases(ir, contents, branch, false), what);
                    assertEquals(DroolsCode.INFER_FROM_EMPTY_FALSE, e.code(), what);
                    assertEquals(DroolsCode.INFER_FROM_EMPTY_FALSE.reason("form", w.getValue()).text(), e.getMessage(), what);
                    continue;
                }
                List<String> verdicts = CaseRunner.playCases(ir, contents, branch, false);
                for (int i = 0; i < cases.size(); i++) {
                    assertNull(verdicts.get(i), what + "/" + cases.get(i).getFileName());
                    played++;
                }
            }
        }
        // the cases of the thirty-one witnesses that play, on two branches, counted 2026-11-23
        assertEquals(2 * 140, played);
    }
}
