package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import dev.uchar.regelspraak.transpiler.kern.Namen;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The BOUNDARIES of the shared name contract ({@link Namen#norm} / {@link Namen#declaredName}), each
 * measured against ref first (2026-09-03).
 *
 * <p>ref is the arbiter, so a name equality WIDER than ref's is not a fix but a new divergence. ref's
 * {@code Checks.norm} is {@code lowercase_ascii (collapse_ws s)}: whitespace collapse over SPACE and TAB
 * only, case folding over A-Z only. The Java one-liner it was first written as ({@code trim() +
 * replaceAll("\\s+", " ") + toLowerCase(Locale.ROOT)}) folds two further axes — a NEWLINE inside a name and
 * a NON-ASCII capital — and on both of them a verifier-clean document that ref and clingo play read
 * "temporal" in the back-end where ref reads "scalar", so the key join met a {@code BigDecimal} bean member
 * and the replay died with a bare {@code ConstraintEvaluationException}. The arity guard, counting the same
 * notion, fired a ch. 5.9 form fault on a shape ref calls valid.
 *
 * <p>The second boundary is the other direction: a key that ref, clingo and the verifier DO resolve
 * ("vondstNummer" for a declared "vondstnummer") still has to be handed to the runtime's property resolver
 * in its DECLARED spelling, because the bean member was generated from the declaration. Of five
 * capitalisations of one key only two used to survive; the other three died with "geen getter voor …".
 *
 * <p>Every model is authored in memory on top of a corpus {@code ir.json} — no corpus file is written, and
 * no expectation is taken from one.
 */
class NameNormWitnessTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    // cwd under surefire is the module dir (backends/drools); the corpus is two levels up.
    private static final Path GOLDEN = Path.of("..", "..", "corpus", "golden");
    private static final Path RED = Path.of("..", "..", "corpus", "rood");

    private static final Path STRANDREGISTER = GOLDEN.resolve("anchor/jutterswet-strandregister/ir.json");
    private static final Path NUMMERREGISTER = GOLDEN.resolve("anchor/jutterswet-nummerregister/ir.json");
    private static final Path TWO_TEMPORAL = RED.resolve("uni-two-temporal-keys/ir.json");

    @TempDir static Path tmp;

    // ---------------- 1. the norm contract itself, axis by axis ----------------

    @Test void norm_folds_only_ascii_case() {
        // ref: String.lowercase_ascii folds A-Z and nothing else, so "Ö" and "ö" stay DIFFERENT characters
        // and "COÖRDINAAT" is not the name "coördinaat". Measured on the OCaml side, 2026-09-03.
        assertEquals("coÖrdinaat", Namen.norm("COÖRDINAAT"));
        assertFalse(Namen.norm("COÖRDINAAT").equals(Namen.norm("coördinaat")),
                "a non-ASCII capital is NOT case-folded — ref reads two different names");
        assertEquals(Namen.norm("VONDSTNUMMER"), Namen.norm("vondstnummer"), "ASCII case IS folded");
    }

    @Test void norm_splits_only_on_space_and_tab() {
        // ref: collapse_ws splits on ' ' and '\t' only. A newline (or \r, \f, \x0B) is a CHARACTER inside
        // the name, and there is no trim beyond dropping empty runs of those two separators.
        assertEquals("vondst\nnummer", Namen.norm("vondst\nnummer"));
        assertFalse(Namen.norm("vondst\nnummer").equals(Namen.norm("vondst nummer")),
                "a newline is not whitespace to ref — the two names differ");
        assertEquals("vondst nummer", Namen.norm("  vondst\t\t nummer  "), "space and tab DO collapse");
        assertEquals("vondstnummer\n", Namen.norm("vondstnummer\n"), "and a trailing newline is not trimmed");
    }

    // ---------------- 2. the norm boundary, played end to end ----------------

    @Test void a_diacritical_case_variant_is_no_temporal_key() throws Exception {
        // "coördinaat" is declared as an attribute, "COÖRDINAAT" carries the granulariteit: ref folds neither
        // into the other, so the key is SCALAR and the two scalar 7s collide. ref, measured on this exact
        // model: PASS (2 expectations) — S1 and S2 flagged; clingo agrees. With a Locale-folding norm the
        // key read temporal, the join of the lowering of that time read a BigDecimal bean member as a timeline
        // and the replay died:
        //   ERROR ConstraintEvaluationException — cause: "rsOoitWaarden: een temporele sleutel moet een
        //   List<Segment> zijn, kreeg java.math.BigDecimal"
        // Re-measured 2026-09-23 on the M1 form (see granularityUnder), with S1 and S2 expected flagged:
        // verifier PASS, ref and clingo PASS (2 expectations), both Drools branches PLAYED 1.
        assertEquals(Set.of("eenmalig nummer @ S1", "eenmalig nummer @ S2"),
                flags(diacritic(), scalarCase("coördinaat", "7", "7")),
                "ref folds A-Z only, so the granulariteit under another diacritic case is another name");
    }

    @Test void a_newline_in_a_name_is_no_separator() throws Exception {
        // the same boundary on the whitespace axis: the attribute is declared "vondst nummer", the
        // granulariteit is keyed "vondst\nnummer". ref: PASS (2 expectations); clingo agrees. Re-measured
        // 2026-09-23 on the M1 form: ref and clingo PASS (2 expectations), both Drools branches PLAYED 1 — but
        // the VERIFIER now rejects the document, WF-057 FAIL: the attribute name "vondst\nnummer" carries a
        // control character. Under v0 the newline sat in a side-map key; since M1 it is a declared name, which
        // WF-057 reads. No gate brings this shape to an executor any more, so the test pins this backend's
        // norm on its own, where ref draws the same boundary.
        assertEquals(Set.of("eenmalig nummer @ S1", "eenmalig nummer @ S2"),
                flags(newline(), scalarCase("vondst nummer", "7", "7")),
                "a newline inside a name does not collapse, so the two names are not the same name");
    }

    @Test void the_arity_guard_does_not_count_what_ref_does_not_count() throws Exception {
        // key list ["coördinaat", "kavelnummer"] where only kavelnummer is temporal to ref: ONE temporal
        // component, so the shape is valid. ref, measured: "oracle: PASS (1 expectations)" — consistent
        // (the tuples (7,3) and (9,3) differ); clingo agrees. A wider norm counted two and raised the ch. 5.9
        // form fault on a shape all three executors accept. Re-measured 2026-09-23 on the M1 form: verifier
        // PASS, ref and clingo PASS (1 expectations), both Drools branches PLAYED 1.
        assertEquals(Set.of(), flags(ariteit(), mixedKeyCase()),
                "the guard must count the temporal components ref counts, not one more");
    }

    // ---------------- 3. the declared spelling reaches the bean ----------------

    @Test void every_case_variant_of_a_key_finds_the_same_collision() throws Exception {
        // the class the card names — "a differently capitalised key" — measured across five spellings on the
        // corpus case (two timelines that both ever carry 7). verifier PASS and ref "oracle: PASS
        // (2 expectations)" on all five; clingo agrees. Only the first two used to play: the temporality test
        // normalised the key but the JOIN handed it to the runtime RAW, so "vondstNummer" pascal-cased into
        // getVondstNummer — a getter the fact class (generated from the DECLARATION) does not have:
        //   ERROR ConstraintEvaluationException — cause: "geen getter voor 'vondstNummer' op …Strandvondst"
        for (String spelling : java.util.List.of(
                "VONDSTNUMMER", "Vondstnummer", "vondstNummer", "VOndstnummer", "vondstnummeR")) {
            Path ir = edited(STRANDREGISTER, o -> {
                IrPath.bron(o, 0, 0).set("keys", JSON.createArrayNode().add(spelling));
                IrPath.rulesList(o, "uniquenesses").remove(1);
            });
            assertEquals(Set.of("eenmalig nummer @ S1", "eenmalig nummer @ S2"),
                    flags(ir, twoTimelines()),
                    "spelling '" + spelling + "' names the declared attribute and must flag both instances");
        }
    }

    @Test void a_case_variant_of_a_union_key_finds_the_same_collision() throws Exception {
        // the union's own key site resolves through the same contract. ref on this model: PASS (2
        // expectations) — both Strandvondsten flagged within the one source; clingo agrees.
        Path ir = edited(NUMMERREGISTER, o -> IrPath.bron(o, 0, 0).set("keys", JSON.createArrayNode().add("vondstNummer")));
        assertEquals(Set.of("eenmalig vondstnummer @ S1", "eenmalig vondstnummer @ S2"),
                flags(ir, twoTimelines()),
                "a union source's key resolves to its declared spelling before the bean is read");
    }

    // ---------------- 4. the DRL text holds both conventions ----------------

    @Test void the_drl_text_names_the_declared_field() throws Exception {
        // the text route emits a PROPERTY NAME, so the same resolution has to happen there: ident() of the
        // written key would render "vondstNummer", a field the fact class does not carry. Since card
        // drl-teksttak-op-truth-maintenance the text is the analysis rendered, which reads the key through the standing
        // of the declared member, and the standing reads the bean field.
        Path ir = edited(STRANDREGISTER, o -> IrPath.bron(o, 0, 0).set("keys", JSON.createArrayNode().add("vondstNummer")));
        String drl = drl(ir);
        assertTrue(drl.contains(".Strandvondst( $invoer : (ComputeHelpers.periods(vondstnummer)) )"), drl);
        assertFalse(drl.contains("vondstNummer"), "the written spelling must not reach the DRL text: " + drl);
    }

    @Test void the_double_temporal_key_is_refused_in_both_branches() throws Exception {
        // ref FAULTS on this shape ("a uniqueness key list carries at most one temporal key component (…)"),
        // clingo raises the same wording at translation, and the analysis refuses it by name, on both branches. Until
        // card drl-teksttak-op-truth-maintenance the DRL-text branch booked it as a MARKER with its onopgelost
        // sentinel ("// onopgelost uniciteitscontrole (ch. 5.9: at most one temporal key component)").
        refusesDuplicateTemporal(TWO_TEMPORAL, "uniciteit: dubbel temporeel enkelvoudig — een uniciteitssleutel"
                + " 'kavelnummer' van Strandvondst op een tijdlijn naast een tweede");
    }

    @Test void the_double_temporal_union_key_is_refused_in_both_branches() throws Exception {
        // the same guard per SOURCE. ref on this model: "EVALUATION FAULT: a uniqueness key list carries at
        // most one temporal key component (…)"; the analysis refuses the union by name, on both branches. Until card
        // drl-teksttak-op-truth-maintenance both source rules of the DRL-text branch carried the sentinel.
        Path ir = edited(NUMMERREGISTER, o -> {
            for (JsonNode b : IrPath.version(o, "uniquenesses", 0, 0).get("sources"))
                ((ObjectNode) b).set("keys", JSON.createArrayNode().add("vondstnummer").add("volgnummer"));
            IrPath.rulesList(o, "uniquenesses").remove(1);
        });
        refusesDuplicateTemporal(ir, "uniciteit-unie: eenmalig vondstnummer — een uniciteitssleutel 'volgnummer' van"
                + " Strandvondst op een tijdlijn naast een tweede");
    }

    /** Both branches refuse {@code ir} at the build, by name, in the sentence that holds {@code fragment}: a key with
     *  two temporal parts (RUNTIME-SEMANTICS ch. 5.9). */
    private static void refusesDuplicateTemporal(Path ir, String fragment) {
        for (DroolsBackend.Branch branch : DroolsBackend.Branch.values()) {
            CaseRunner.Excluded e = org.junit.jupiter.api.Assertions.assertThrows(CaseRunner.Excluded.class,
                    () -> DroolsBackend.build(ir, branch), branch.toString());
            assertTrue(e.getMessage().contains(fragment) && e.getMessage().contains("(RUNTIME-SEMANTICS ch. 5.9)"),
                    branch + ": " + e.getMessage());
        }
    }

    // ---------------- models ----------------

    /** {@code coördinaat} declared, {@code COÖRDINAAT} carrying the granulariteit — the diacritic axis. */
    private static Path diacritic() throws Exception {
        return edited(STRANDREGISTER, o -> {
            renameAttribute(o, "coördinaat");
            granularityUnder(o, "coördinaat", "COÖRDINAAT");
            singleUniqueness(o, JSON.createArrayNode().add("coördinaat"));
        });
    }

    /** {@code vondst nummer} declared, {@code vondst\nnummer} carrying the granulariteit — the whitespace axis. */
    private static Path newline() throws Exception {
        return edited(STRANDREGISTER, o -> {
            renameAttribute(o, "vondst nummer");
            granularityUnder(o, "vondst nummer", "vondst\nnummer");
            singleUniqueness(o, JSON.createArrayNode().add("vondst nummer"));
        });
    }

    /** The diacritic model with a TWO-component key: scalar {@code coördinaat} + temporal {@code kavelnummer}. */
    private static Path ariteit() throws Exception {
        return edited(STRANDREGISTER, o -> {
            renameAttribute(o, "coördinaat");
            granularityUnder(o, "coördinaat", "COÖRDINAAT");
            singleUniqueness(o, JSON.createArrayNode().add("coördinaat").add("kavelnummer"));
        });
    }

    /** Rename Strandvondst's {@code vondstnummer} declaration, keeping the declared order. */
    private static void renameAttribute(ObjectNode ir, String recomputed) {
        IrPath.attribute(IrPath.objectType(ir, "Strandvondst"), "vondstnummer").put("name", recomputed);
    }

    /** The granulariteit keyed under another spelling of the declared member, which stays scalar. Until M1 that
     *  was a granularity entry for {@code spelling} with no member of that name, and the measurements quoted in
     *  the tests above were taken on that form. Since M1 a facet hangs on its member, so {@code spelling} is
     *  declared as a second attribute carrying the JAAR facet: the facet names the key sites look up are the
     *  same as before ({@code spelling} and kavelnummer), and the declared member stays FIRST, so the bean's one
     *  property for the identifier both spellings share is still the scalar one (the first derivation wins). The
     *  tests quote their re-measurement on this form beside the v0 one (2026-09-23). */
    private static void granularityUnder(ObjectNode ir, String declared, String spelling) {
        ObjectNode ot = IrPath.objectType(ir, "Strandvondst");
        ObjectNode member = IrPath.attribute(ot, declared);
        member.remove("timeline");
        IrPath.timeline(IrPath.addAttribute(ot, spelling, member.get("datatype").deepCopy()), "year");
    }

    /** Keep only the first uniciteit (the scoped second one plays no part here) and give it {@code keyNames}. */
    private static void singleUniqueness(ObjectNode ir, JsonNode keyNames) {
        var lijst = IrPath.rulesList(ir, "uniquenesses");
        while (lijst.size() > 1) lijst.remove(1);
        IrPath.bron(ir, 0, 0).set("keys", keyNames);
    }

    /** A corpus {@code ir.json} with one authored edit, written to a temp file. */
    private static Path edited(Path bron, Consumer<ObjectNode> edit) throws Exception {
        ObjectNode ir = (ObjectNode) JSON.readTree(Files.readString(bron));
        edit.accept(ir);
        Path out = Files.createTempFile(tmp, "naamnorm", ".json");
        Files.writeString(out, ir.toString());
        return out;
    }

    /** The text of the DRL-text branch of one model: the analysis rendered as DRL text. */
    private static String drl(Path ir) throws Exception {
        return DroolsBackend.build(ir, DroolsBackend.Branch.DRL_TEXT).drl();
    }

    // ---------------- cases ----------------

    /** Two Strandvondsten carrying {@code attr} as a PLAIN value. */
    private static JsonNode scalarCase(String attr, String v1, String v2) throws Exception {
        return JSON.readTree("""
                { "format": "uchar-case-v0", "asOfDate": null,
                  "input": { "instances": [
                      { "label": "S1", "objectType": "Strandvondst",
                        "attributes": { "%s": { "number": "%s" } }, "roles": {}, "characteristics": [] },
                      { "label": "S2", "objectType": "Strandvondst",
                        "attributes": { "%s": { "number": "%s" } }, "roles": {}, "characteristics": [] } ],
                    "parameters": {} },
                  "expected": [ { "kind": "consistent" } ] }
                """.formatted(attr, v1, attr, v2));
    }

    /** Two Strandvondsten with a scalar coördinaat (7 / 9) and the SAME kavelnummer timeline. */
    private static JsonNode mixedKeyCase() throws Exception {
        return JSON.readTree("""
                { "format": "uchar-case-v0", "asOfDate": null,
                  "input": { "instances": [
                      { "label": "S1", "objectType": "Strandvondst",
                        "attributes": { "coördinaat": { "number": "7" },
                            "kavelnummer": { "timeline": [
                                { "from": "2024-01-01", "to": "2025-01-01", "value": { "number": "3" } } ] } },
                        "roles": {}, "characteristics": [] },
                      { "label": "S2", "objectType": "Strandvondst",
                        "attributes": { "coördinaat": { "number": "9" },
                            "kavelnummer": { "timeline": [
                                { "from": "2024-01-01", "to": "2025-01-01", "value": { "number": "3" } } ] } },
                        "roles": {}, "characteristics": [] } ],
                    "parameters": {} },
                  "expected": [ { "kind": "consistent" } ] }
                """);
    }

    /** The corpus shape: two vondstnummer timelines that never overlap in time yet both ever carry 7. */
    private static JsonNode twoTimelines() throws Exception {
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

    // ---------------- helpers ----------------

    /** The §9.5 channel after fire as {@code "<regel> @ <label>"} — the per-INSTANCE flagging ref pins. */
    private static Set<String> flags(Path ir, JsonNode c) throws Exception {
        CaseRunner.AfterFire after = CaseRunner.afterFire(ir, c);
        Set<String> out = new LinkedHashSet<>();
        for (Object f : after.sessionFacts()) {
            if (!"Inconsistentie".equals(f.getClass().getSimpleName())) continue;
            Object subject = f.getClass().getMethod("getSubject").invoke(f);
            String label = "?";
            for (Map.Entry<String, Object> e : after.invoer().entrySet())
                if (e.getValue() == subject) label = e.getKey();
            out.add(f.getClass().getMethod("getRegel").invoke(f) + " @ " + label);
        }
        return out;
    }
}
