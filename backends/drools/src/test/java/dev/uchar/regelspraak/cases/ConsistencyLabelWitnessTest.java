package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

/**
 * The §9.5 {@code inconsistent} expectation's {@code label} pin, held on BOTH transpiler branches for a
 * RULE-NAME key. ref matches a channel fact on the version-stripped rule name AND the exact subject label
 * ({@code ref/lib/case.ml}, the {@code Inconsistent} arm); the runner checked the subject only for a
 * default-group conflict key, so any fact from the pinned rule satisfied the pin wherever it landed.
 *
 * <p><b>Red before the fix</b> (measured 2026-09-23 on main db390c28 through the backend CLI, on the swapped case
 * below): exec-model {@code PLAYED 1}, DRL-text {@code PLAYED 1}, while ref answers
 * {@code MISMATCH: expected inconsistent (a channel fact from rule "geen wrakhout ter keuring" on "V2"), but the
 * channel carries: geen wrakhout ter keuring × V1; herkeuring volgt de staat × V2} and the same for the second pin.
 *
 * <p><b>The fixture is a corpus golden</b>, {@code corpus/golden/jutterswet-keurspelling}, which carries two
 * consistency rules. Hand computation: V1 (staat wrakhout, herkeuring wrakhout) violates only "geen wrakhout ter
 * keuring"; V2 (staat gaaf, herkeuring beschadigd) violates only "herkeuring volgt de staat"; the staten differ,
 * so "eenmalige staat" is silent. Each rule fires on exactly one subject, and on each subject the OTHER rule is
 * the one that fires, so a pin with the labels swapped names a rule that did fire, on a subject where it did not.
 */
class ConsistencyLabelWitnessTest {

    private static final ObjectMapper JSON = new ObjectMapper();
    // cwd under surefire is the module dir (backends/drools); the corpus is two levels up.
    private static final Path IR = Path.of("..", "..", "corpus", "golden", "jutterswet-keurspelling", "ir.json");

    @Test void the_correct_label_plays_in_both_branches() throws Exception {
        BothBranches.green(IR, caseOf("V1", "V2"), "each rule pinned on the subject it fires on");
    }

    @Test void a_swapped_label_fails_in_both_branches() throws Exception {
        JsonNode c = caseOf("V2", "V1");
        BothBranches.red(IR, c, "verwacht inconsistent (geen wrakhout ter keuring @ V2), niet gevonden onder",
                "the rule fires, but on V1, not on the pinned V2");
        BothBranches.red(IR, c, "verwacht inconsistent (herkeuring volgt de staat @ V1), niet gevonden onder",
                "the rule fires, but on V2, not on the pinned V1");
    }

    /** A label naming no instance fails as a mismatch in both branches. ref refuses the same case at LOAD
     *  ({@code inconsistent expectation references unknown label}) — a stricter line this runner does not draw. */
    @Test void an_unknown_label_fails_in_both_branches() throws Exception {
        BothBranches.red(IR, caseOf("V9", "V2"), "verwacht inconsistent (geen wrakhout ter keuring @ V9), niet gevonden onder",
                "no instance V9 exists");
    }

    private static JsonNode caseOf(String wrakhoutLabel, String herkeuringLabel) throws Exception {
        return JSON.readTree("""
                {"format": "uchar-case-v0", "asOfDate": null,
                 "input": {"instances": [
                   {"label": "V1", "objectType": "Vondst", "roles": {}, "characteristics": [],
                    "attributes": {"staat": {"enumeration": "wrakhout"}, "herkeuring": {"enumeration": "wrakhout"}}},
                   {"label": "V2", "objectType": "Vondst", "roles": {}, "characteristics": [],
                    "attributes": {"staat": {"enumeration": "gaaf"}, "herkeuring": {"enumeration": "beschadigd"}}}],
                  "parameters": {}},
                 "expected": [
                   {"kind": "inconsistent", "rule": "geen wrakhout ter keuring", "label": "%s"},
                   {"kind": "inconsistent", "rule": "herkeuring volgt de staat", "label": "%s"}]}
                """.formatted(wrakhoutLabel, herkeuringLabel));
    }
}
