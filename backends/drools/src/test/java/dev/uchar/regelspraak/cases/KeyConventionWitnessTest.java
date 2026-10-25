package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import dev.uchar.regelspraak.transpiler.tms.TmsTranspiler.DrlPiece;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The two KEY CONVENTIONS of the ch. 5.9 uniqueness lowering, each measured against ref first
 * (2026-09-03) and each pre-existing:
 *
 * <ol>
 *   <li><b>Name normalisation.</b> A uniqueness key names an attribute in another CAPITALISATION than the
 *       declaration. The IR spells a name as the author wrote it, ref and clingo compare declared names
 *       normalised ({@code Checks.norm} / {@code Checks.attr_temporal}) and the verifier passes the document,
 *       so this is a VALID one-component model — but the Drools key sites tested temporality with an exact
 *       {@code Map.containsKey}, read the key as scalar, and then joined two {@code List<Segment>} timelines
 *       with {@code equals} (Segment has no {@code equals}): every collision ref and clingo find went
 *       silently missing, and the ch. 5.9 arity guard stayed silent on a shape ref FAULTS on.</li>
 *   <li><b>The cross-source mixed union.</b> Each source of a "verenigd met" union declares its OWN key list,
 *       so one component can be temporal on source A and scalar on source B. ref plays it (per-source
 *       {@code ever_tuples}: the temporal source contributes one tuple per ever-value, the scalar source one
 *       tuple with its value) and clingo agrees; Drools crashed with a {@code ConstraintEvaluationException}
 *       — an ERROR, which under the gate-12 regime is never a valid not-carried outcome.</li>
 * </ol>
 *
 * <p>Every model here is authored in memory on top of a corpus {@code ir.json} (no corpus file is written or
 * read for its expectations), and every expectation was measured through ref and clingo BEFORE it was pinned.
 *
 * <p>The witnesses read the §9.5 channel through {@link CaseRunner#afterFire}, not through
 * {@link CaseRunner#playCase}: for a rule-name key the case comparator matches on the RULE NAME ALONE (the
 * subject the exec model attaches may legitimately differ from ref's), so an {@code inconsistent} expectation
 * naming S2 is already satisfied by a flag on S1 — a comparator seam too blunt to see a half-found collision.
 * {@link #flags} pins the exact {@code regel @ label} SET, which is what ref pins per instance.
 */
class KeyConventionWitnessTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    // cwd under surefire is the module dir (backends/drools); the corpus is two levels up.
    private static final Path GOLDEN = Path.of("..", "..", "corpus", "golden");
    private static final Path RED = Path.of("..", "..", "corpus", "red");

    private static final Path STRANDREGISTER = GOLDEN.resolve("anchor/jutterswet-strandregister/ir.json");
    private static final Path NUMMERREGISTER = GOLDEN.resolve("anchor/jutterswet-nummerregister/ir.json");
    private static final Path TWO_TEMPORAL = RED.resolve("uniqueness-two-temporal-keys/ir.json");

    /** The shared ch. 5.9 key-shape wording, byte-equal to ref's ({@code eval.ml, ever_tuples}). */
    /** The exec-model branch's refusal of a key list with two temporal components. */
    private static final String TOO_MANY = "een uniciteitssleutel 'kavelnummer' van Strandvondst op een tijdlijn naast"
            + " een tweede: een sleutel heeft hoogstens één tijdsafhankelijk deel";

    @TempDir static Path tmp;

    // ---------------- 1. name normalisation: the single form ----------------

    @Test void a_differently_capitalised_key_finds_the_same_collision() throws Exception {
        // ref, measured 2026-09-03 on this exact model: PASS (2 expectations) — S1 and S2 both flagged;
        // clingo agrees. Before the convention was pulled straight the backend answered "consistent":
        // "Vondstnummer" missed the granulariteit map, the scalar branch compared two List<Segment> with
        // equals, and a Segment carries no equals — so not even the instance's own key matched itself.
        Path ir = withKeys(STRANDREGISTER, o -> IrPath.source(o, 0, 0).set("keys", JSON.createArrayNode().add("Vondstnummer")));
        assertEquals(Set.of("eenmalig nummer @ S1", "eenmalig nummer @ S2"),
                flags(ir, twoStrandvondsten()),
                "a differently capitalised key names the SAME declared attribute (ref's Checks.norm)");
    }

    @Test void the_bare_twin_of_the_single_key() throws Exception {
        // the bare twin: the corpus model untouched, so the only difference with the arm above is the
        // capitalisation of the key. Both must flag the same two instances.
        assertEquals(Set.of("eenmalig nummer @ S1", "eenmalig nummer @ S2"),
                flags(STRANDREGISTER, twoStrandvondsten()),
                "the declared spelling is the same model and must flag the same instances");
    }

    // ---------------- 2. name normalisation: the union form ----------------

    @Test void a_differently_capitalised_union_key_finds_the_same_collision() throws Exception {
        // the union's own key site. TWO Strandvondsten and NO Wrakvondst, deliberately: with a Wrakvondst in
        // play the mis-read source still counts ITSELF (equals on the identical list object) and the sum
        // crosses 1 anyway, so the gap hides. ref, measured on this model: PASS (2 expectations); clingo
        // agrees. Before the fix the backend answered "consistent".
        Path ir = withKeys(NUMMERREGISTER, o -> IrPath.source(o, 0, 0).set("keys", JSON.createArrayNode().add("Vondstnummer")));
        assertEquals(Set.of("eenmalig vondstnummer @ S1", "eenmalig vondstnummer @ S2"),
                flags(ir, twoStrandvondsten()),
                "a union source's key is the same declared attribute in another capitalisation");
    }

    @Test void the_bare_twin_of_the_union_key() throws Exception {
        assertEquals(Set.of("eenmalig vondstnummer @ S1", "eenmalig vondstnummer @ S2"),
                flags(NUMMERREGISTER, twoStrandvondsten()),
                "the declared spelling flags the same two instances within one source");
    }

    // ---------------- 3. name normalisation: the ch. 5.9 arity guard ----------------

    @Test void a_differently_capitalised_key_counts_along_for_the_arity() throws Exception {
        // the guard counts the TEMPORAL components of a key list; counting them with an exact match made a
        // mis-capitalised component invisible, so a two-temporal key list read as one and ran on. ref on this
        // model: "EVALUATION FAULT: a uniqueness key list carries at most one temporal key component (…)";
        // clingo raises the same wording at translation. The exec-model branch refuses the shape by name when
        // it lowers the uniqueness (UniquenessKeyFormWitnessTest), so its count shows in the refusal.
        Path ir = withKeys(TWO_TEMPORAL, o -> IrPath.source(o, 0, 0).set("keys", JSON.createArrayNode().add("Vondstnummer").add("kavelnummer")));
        CaseRunner.Excluded e = assertThrows(CaseRunner.Excluded.class,
                () -> CaseRunner.playCase(ir, twoTemporalKeys(), DroolsBackend.Branch.EXEC_MODEL));
        assertTrue(e.getMessage().contains(TOO_MANY),
                "the arity guard must count a mis-capitalised component too, got: " + e.getMessage());
    }

    @Test void the_bare_twin_of_the_arity_guard() throws Exception {
        CaseRunner.Excluded e = assertThrows(CaseRunner.Excluded.class,
                () -> CaseRunner.playCase(TWO_TEMPORAL, twoTemporalKeys(), DroolsBackend.Branch.EXEC_MODEL));
        assertTrue(e.getMessage().contains(TOO_MANY), "the declared spelling is refused on the same shape, got: "
                + e.getMessage());
    }

    // ---------------- 4. the cross-source mixed union ----------------

    @Test void a_cross_source_mixed_union_finds_the_collision() throws Exception {
        // vondstnummer is temporal on Strandvondst and scalar on Wrakvondst — verifier-clean, and ref plays
        // it: PASS (2 expectations), S1 and W1 both flagged; clingo agrees. Before the fix BOTH mixed arms
        // were wrong, and the loud one crashed the whole replay:
        //   ERROR ConstraintEvaluationException — cause: "rsOoitWaarden: een temporele sleutel moet een
        //   List<Segment> zijn, kreeg java.math.BigDecimal"
        // (the Wrakvondst rule binds a scalar key and joins it against Strandvondst's timeline), while the
        // mirror arm (the Strandvondst rule joining its timeline against Wrakvondst's scalar) silently
        // missed. The flag set pins BOTH arms: S1 comes from the second, W1 from the first.
        assertEquals(Set.of("eenmalig vondstnummer @ S1", "eenmalig vondstnummer @ W1"),
                flags(mixedUnion(), mixedCase("7", "7")),
                "the scalar source claims its value, the temporal source every value it EVER carries");
    }

    @Test void a_cross_source_mixed_union_without_collision_is_silent() throws Exception {
        // the sharp twin of the arm above: the same mixed shape with 7 against 8. ref: PASS (1 expectation,
        // consistent); clingo agrees. Before the fix this crashed too — the ERROR was unconditional, so a
        // "carry" that flagged everything would look just as green on the colliding case alone.
        assertEquals(Set.of(), flags(mixedUnion(), mixedCase("7", "8")),
                "a scalar that is not among the timeline's ever-values claims nothing");
    }

    @Test void the_bare_twin_of_the_mixed_union() throws Exception {
        // the unmixed twin: the corpus model, where vondstnummer is temporal on BOTH sources, with the same
        // two labels colliding on 7. Same flags — the mixed lowering must not shift the pure form.
        assertEquals(Set.of("eenmalig vondstnummer @ S1", "eenmalig vondstnummer @ W1"),
                flags(NUMMERREGISTER, JSON.readTree("""
                        { "format": "uchar-case-v0", "asOfDate": null,
                          "input": { "instances": [
                              { "label": "S1", "objectType": "Strandvondst",
                                "attributes": { "vondstnummer": { "timeline": [
                                    { "from": "2024-01-01", "to": "2025-01-01", "value": { "number": "7" } } ] } },
                                "roles": {}, "characteristics": [] },
                              { "label": "W1", "objectType": "Wrakvondst",
                                "attributes": { "vondstnummer": { "timeline": [
                                    { "from": "2026-01-01", "to": "2027-01-01", "value": { "number": "7" } } ] } },
                                "roles": {}, "characteristics": [] } ],
                            "parameters": {} },
                          "expected": [ { "kind": "consistent" } ] }
                        """)),
                "two timelines that ever carry 7 collide across the sources, as they always did");
    }

    // ---------------- 5. the DRL text carries the same two conventions ----------------

    /* Until card drl-teksttak-op-truth-maintenance the DRL-text branch was a lowering of its own, which no gate fired at
     * these documents, so three rows here read its emitted key sites: a temporal key test on vondstnummer and no scalar
     * "vondstnummer == $k0" for the mis-capitalised key, no mixed membership arm in the union, and both mixed arms in
     * the cross-source union. Since that card the text renders the analysis, whose key sites are the exec-model
     * branch's; the rows below ask the DRL-text branch for the same session as the exec-model branch on the documents
     * and cases of the rows above (BranchDumps: the verdict, the input, what lees gives and the facts at the fixpoint). */

    @Test void the_drl_text_reads_a_differently_capitalised_key_temporally() throws Exception {
        Path ir = withKeys(STRANDREGISTER, o -> IrPath.source(o, 0, 0).set("keys", JSON.createArrayNode().add("Vondstnummer")));
        BranchDumps.equal(ir, twoStrandvondsten(), "the mis-capitalised key");
        String drl = drl(ir);
        assertTrue(!drl.contains("Vondstnummer"), "the written spelling must not reach the DRL text: " + drl);
    }

    @Test void the_drl_text_also_normalises_the_union_key() throws Exception {
        Path ir = withKeys(NUMMERREGISTER, o -> IrPath.source(o, 0, 0).set("keys", JSON.createArrayNode().add("Vondstnummer")));
        BranchDumps.equal(ir, twoStrandvondsten(), "the mis-capitalised key of a union");
        String drl = drl(ir);
        assertTrue(!drl.contains("Vondstnummer"), "the written spelling must not reach the DRL text: " + drl);
    }

    @Test void the_drl_text_plays_the_mixed_union_in_both_directions() throws Exception {
        BranchDumps.equal(mixedUnion(), mixedCase("7", "7"), "the cross-source mixed union");
    }

    // ---------------- models ----------------

    /** The text of the DRL-text branch of one model, the analysis rendered as DRL text, without the names of its
     *  functions: since card drl-function-names-from-rule a function is named after its rule, whose name the text
     *  spells camel-cased there (a rule "eenmalig vondstnummer: …" names {@code toets_eenmaligVondstnummer…}), which
     *  is no key site and no written spelling of a key. */
    private static String drl(Path ir) throws Exception {
        DroolsBackend.Build b = DroolsBackend.build(ir, DroolsBackend.Branch.DRL_TEXT);
        String text = b.drl();
        for (DrlPiece p : b.execModelTranspiler().drlPieces())
            for (DrlPiece.DrlFunction f : p.functions()) text = text.replace(f.name(), "");
        return text;
    }

    /** {@code vondstnummer} temporal on Strandvondst, SCALAR on Wrakvondst: the cross-source mixed key. The
     *  document stays verifier-clean (measured 2026-09-03) — each source declares its own key list. */
    private static Path mixedUnion() throws Exception {
        return withKeys(NUMMERREGISTER, o -> IrPath.attribute(IrPath.objectType(o, "Wrakvondst"), "vondstnummer")
                .remove("timeline"));
    }

    /** A corpus {@code ir.json} with one authored edit, written to a temp file — the corpus file itself is
     *  never touched (the mutation lives in this test, not in the shared zone). */
    private static Path withKeys(Path source, Consumer<ObjectNode> edit) throws Exception {
        ObjectNode ir = (ObjectNode) JSON.readTree(Files.readString(source));
        edit.accept(ir);
        Path out = Files.createTempFile(tmp, "sleutelconventie", ".json");
        Files.writeString(out, ir.toString());
        return out;
    }

    // ---------------- cases ----------------

    /** Two Strandvondsten whose vondstnummer timelines never overlap in TIME yet both ever carry 7 — the
     *  ch. 5.9 ever-reading, within one type and (through the union) within one source. */
    private static JsonNode twoStrandvondsten() throws Exception {
        return JSON.readTree("""
                { "format": "uchar-case-v0", "asOfDate": null,
                  "input": { "instances": [
                      { "label": "S1", "objectType": "Strandvondst",
                        "attributes": { "vondstnummer": { "timeline": [
                            { "from": "2024-01-01", "to": "2025-01-01", "value": { "number": "7" } } ] } },
                        "roles": {}, "characteristics": [] },
                      { "label": "S2", "objectType": "Strandvondst",
                        "attributes": { "vondstnummer": { "timeline": [
                            { "from": "2026-01-01", "to": "2027-01-01", "value": { "number": "7" } } ] } },
                        "roles": {}, "characteristics": [] } ],
                    "parameters": {} },
                  "expected": [ { "kind": "consistent" } ] }
                """);
    }

    /** One Strandvondst supplying BOTH temporal key components — enough to reach the arity guard, which
     *  judges the SHAPE and needs no collision. */
    private static JsonNode twoTemporalKeys() throws Exception {
        return JSON.readTree("""
                { "format": "uchar-case-v0", "asOfDate": null,
                  "input": { "instances": [
                      { "label": "S1", "objectType": "Strandvondst",
                        "attributes": {
                            "vondstnummer": { "timeline": [
                                { "from": "2024-01-01", "to": "2025-01-01", "value": { "number": "1" } } ] },
                            "kavelnummer": { "timeline": [
                                { "from": "2024-01-01", "to": "2025-01-01", "value": { "number": "2" } } ] } },
                        "roles": {}, "characteristics": [] } ],
                    "parameters": {} },
                  "expected": [ { "kind": "consistent" } ] }
                """);
    }

    /** A Strandvondst carrying {@code s} over one period and a Wrakvondst carrying the plain value {@code w}. */
    private static JsonNode mixedCase(String s, String w) throws Exception {
        return JSON.readTree("""
                { "format": "uchar-case-v0", "asOfDate": null,
                  "input": { "instances": [
                      { "label": "S1", "objectType": "Strandvondst",
                        "attributes": { "vondstnummer": { "timeline": [
                            { "from": "2024-01-01", "to": "2025-01-01", "value": { "number": "%s" } } ] } },
                        "roles": {}, "characteristics": [] },
                      { "label": "W1", "objectType": "Wrakvondst",
                        "attributes": { "vondstnummer": { "number": "%s" } },
                        "roles": {}, "characteristics": [] } ],
                    "parameters": {} },
                  "expected": [ { "kind": "consistent" } ] }
                """.formatted(s, w));
    }

    // ---------------- helpers ----------------

    /** The §9.5 channel after fire as {@code "<regel> @ <label>"} — the per-INSTANCE flagging ref pins. */
    private static Set<String> flags(Path ir, JsonNode c) throws Exception {
        CaseRunner.AfterFire after = CaseRunner.afterFire(ir, c);
        Set<String> out = new LinkedHashSet<>();
        for (Object f : after.sessionFacts()) {
            if (!"Inconsistentie".equals(f.getClass().getSimpleName())) continue;
            Object subject = read(f, "getSubject");
            String label = "?";
            for (var e : after.input().entrySet()) if (e.getValue() == subject) label = e.getKey();
            out.add(read(f, "getRule") + " @ " + label);
        }
        return out;
    }

    /** A no-arg read on a CHILD-loaded bean (the fact classes are not on this test's classpath). */
    private static Object read(Object bean, String getter) throws Exception {
        return bean.getClass().getMethod(getter).invoke(bean);
    }
}
