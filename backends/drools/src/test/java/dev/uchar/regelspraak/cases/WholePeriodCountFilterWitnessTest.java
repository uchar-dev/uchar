package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The form of card drl-aantal-filter-zink through {@code DRL_TEXT} (card drl-teksttak-op-truth-maintenance, step 3a,
 * FABLE-2 §7.4): a whole-period qualification in the filter of a count, "het aantal posten van het Kustvak die
 * gedurende het gehele jaar bemand zijn", in a computation rule and in the guard of an object creation. The
 * destructive DRL-text lowering sank both documents with a compiler error in the accumulate of its anchor attempt
 * ({@code Unable to Analyse Expression rsGeheelPeriode($subject.getBemand(), "JAAR", 7, null)}). In the rendering of the
 * analysis the filter stands in the test of the rule that inserts a member's contribution, as a function that calls
 * the route of the whole period, and no accumulate holds it.
 *
 * <p>The two documents, the corpus goldens {@code jutterswet-aantalfilter-gehele-jaar} (the computation rule) and
 * {@code jutterswet-aantalfilter-gehele-jaar-creatieguard} (the creation guard), are own work: written in RegelSpraak and
 * emitted by the frontend, which does not lower a die/dat-subselection with a qualifier on the operand of an
 * aggregation yet (it writes an {@code Unresolved} filter and a warning). The filter of each count is therefore written
 * in by hand, the check of the characteristic "bemand" of the member under the qualifier of the whole year, and the
 * warning is dropped; so the goldens carry no {@code source.rgs}, which gate 10 would hold to their IR, and their NOTE.md
 * quotes the source. The expectations are computed by hand from RUNTIME-SEMANTICS ch. 3.9: a post counts where each
 * year its true periods touch is covered on every day, so a post manned for the first half of 2026 does not count, a
 * post manned through 2025 and 2026 does, and a post without any period counts vacuously.
 *
 * <p>This witness fires both branches, the exec-model branch and {@code DRL_TEXT}. Measured on {@code 4c8a687b}, before
 * step 3a, when the documents were the card's own and lay in the test resources, per document (verifier, ref, clingo,
 * exec-model branch, the destructive DRL-text branch, {@code DRL_TMS}): the computation rule PASS, PASS 4, PASS 4,
 * PLAYED 4, EXCLUDE on the compiler error above, EXCLUDE "drl-tms: de DRL-weergave draagt Bijdrage nog niet"; the
 * creation guard PASS, PASS 3, PASS 3, PLAYED 3, the same compiler error, and EXCLUDE "drl-tms: de DRL-weergave draagt
 * Creatie nog niet". The creation guard plays through the rendering as DRL text since it carries a creation (step 5);
 * until then it refused the guard for the creation, and this witness pinned that refusal. Since the switch-over (step 6)
 * that rendering is the DRL-text branch, and since step 6b both documents are goldens, which gates 5, 6 and 12 replay
 * in the reference interpreter, the ASP backend and both branches.
 *
 * <p><b>Red</b> on {@code 4c8a687b}: the test of the computation rule fails on the refusal of the contribution. The
 * test of the creation guard held there too, since the rendering met the creation first: it pinned the loud refusal
 * until step 5, not a form step 3a brings. <b>Red</b> on {@code b4d83a69}, before the creations of step 5: the creation
 * guard failed on {@code DRL_TMS} with "drl-tms: de DRL-weergave draagt Creatie nog niet".
 */
class WholePeriodCountFilterWitnessTest {

    private static final ObjectMapper JSON = new ObjectMapper();
    // cwd under surefire is the module dir (backends/drools); the corpus is two levels up
    private static final Path GOLDEN = Path.of("..", "..", "corpus", "golden");
    private static final Path COMPUTING_RULE = GOLDEN.resolve("jutterswet-aantalfilter-gehele-jaar");
    private static final Path CREATION_GUARD = GOLDEN.resolve("jutterswet-aantalfilter-gehele-jaar-creatieguard");

    /** A function of the text that holds the route of the whole period, and its name. */
    private static final Pattern WHOLE_PERIOD = Pattern.compile(
            "function boolean (r\\d+_toets\\d+)\\([^)]*\\) \\{\\n    return ComputeHelpers\\.wholePeriod\\(");

    @Test void the_computing_rule_plays_through_the_drl_text_branch_like_through_the_exec_model_branch() throws Exception {
        Path ir = COMPUTING_RULE.resolve("ir.json");
        List<Path> cases = cases(ir);
        assertEquals(4, cases.size(), "de cases van de rekenregel");
        for (Path casePath : cases) {
            JsonNode c = JSON.readTree(casePath.toFile());
            assertNull(CaseRunner.playCase(ir, c, DroolsBackend.Branch.EXEC_MODEL), "exec-model — " + casePath);
            assertNull(CaseRunner.playCase(ir, c, DroolsBackend.Branch.DRL_TEXT), "drl-tekst — " + casePath);
        }
        String text = DroolsBackend.build(ir, DroolsBackend.Branch.DRL_TEXT).drl();
        List<String> functions = new ArrayList<>();
        Matcher m = WHOLE_PERIOD.matcher(text);
        while (m.find()) functions.add(m.group(1));
        assertEquals(2, functions.size(), () -> "een functie van het filter per bijdrageregel:\n" + text);
        for (String f : functions)
            assertTrue(Pattern.compile("rule \"[^\"]*: bijdrage aan vouw \\d+\"\\n(?:(?!end\\n)[^\\n]*\\n)*?    eval\\( " + f
                    + "\\(").matcher(text).find(), () -> f + " is de toets van een bijdrageregel:\n" + text);
        for (String regel : text.split("\n"))
            assertFalse(regel.contains("accumulate(") && regel.contains("geheelPeriode"),
                    () -> "het filter staat in een accumulate: " + regel);
    }

    /** The two rules of the computation rule in the other order of the document, its only other order (card
     *  drl-aantal-filter-zink, acceptance (a): in every order of the rules involved), play every case on both branches.
     *  Green from the start, on {@code 0458b2d0}: truth maintenance reaches the same fixpoint in either order, and no
     *  step of card drl-teksttak-op-truth-maintenance changes that; the row guards it for the text. */
    @Test void the_computing_rule_also_plays_in_the_other_order_of_its_two_rules() throws Exception {
        Path ir = COMPUTING_RULE.resolve("ir.json");
        JsonNode document = JSON.readTree(ir.toFile());
        ArrayNode rules = (ArrayNode) document.get("rules").get("rules");
        assertEquals(List.of("postental van een kustvak", "waaktarief van een kustvak"),
                List.of(rules.get(0).get("name").asText(), rules.get(1).get("name").asText()), "de regels in het document");
        List<JsonNode> reversed = new ArrayList<>();
        rules.forEach(reversed::add);
        Collections.reverse(reversed);
        rules.removeAll();
        rules.addAll(reversed);
        Path other = Files.createTempFile("heelperiode-andere-volgorde", ".json");
        other.toFile().deleteOnExit();
        Files.writeString(other, JSON.writeValueAsString(document));
        for (Path casePath : cases(ir)) {
            JsonNode c = JSON.readTree(casePath.toFile());
            for (DroolsBackend.Branch branch : List.of(DroolsBackend.Branch.EXEC_MODEL, DroolsBackend.Branch.DRL_TEXT))
                assertNull(CaseRunner.playCase(other, c, branch), branch + " — " + casePath);
        }
    }

    @Test void the_creation_guard_plays_on_both_branches() throws Exception {
        Path ir = CREATION_GUARD.resolve("ir.json");
        List<Path> cases = cases(ir);
        assertEquals(3, cases.size(), "de cases van de creatieguard");
        for (Path casePath : cases) {
            JsonNode c = JSON.readTree(casePath.toFile());
            for (DroolsBackend.Branch branch : List.of(DroolsBackend.Branch.EXEC_MODEL, DroolsBackend.Branch.DRL_TEXT))
                assertNull(CaseRunner.playCase(ir, c, branch), branch + " — " + casePath);
        }
    }

    private static List<Path> cases(Path ir) throws Exception {
        try (Stream<Path> s = Files.list(ir.getParent())) {
            return s.filter(p -> p.getFileName().toString().matches("case.*\\.json")).sorted().toList();
        }
    }
}
