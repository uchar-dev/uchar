package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * The EDGES of the cross-source mixed union and of the union's own key sites — the three properties the
 * lowering claims but no witness held, each measured against ref and clingo first (2026-09-03).
 *
 * <ol>
 *   <li><b>Scale.</b> The union compares the scalar with each value the timeline ever carries by number, not
 *       by scale: the analysis takes those values without trailing zeros ({@code rsOoitWaarden}) and compares
 *       two keys with {@code compareTo} ({@code UniquenessKey.collidesWith}), because ref compares LEXEMES of
 *       rationals: 7 and 7.00 are one number to ref and to clingo, so a backend that compares {@code BigDecimal}
 *       by {@code equals} (scale-sensitive) answers {@code consistent} where both other executors flag two
 *       instances.</li>
 *   <li><b>An absent scalar key.</b> The key rule of a source inserts a key only where every component off the
 *       timeline is gevuld, so an instance that supplies no value at all inserts none. It must claim nothing —
 *       ref and clingo call that case {@code consistent}.</li>
 *   <li><b>The declared spelling in the DRL text.</b> The DRL text names bean members, and a source of the union
 *       reads its key through the standing of the member, which reads the bean field. The field must be the
 *       DECLARED spelling; the written one names a field the generated fact class does not carry. Until card
 *       drl-teksttak-op-truth-maintenance the text was a lowering of its own, with two key sites per source.</li>
 * </ol>
 *
 * <p>Every model is authored in memory on top of a corpus {@code ir.json} — no corpus file is written, and
 * no expectation is taken from one. The behavioural witnesses read the §9.5 channel through
 * {@link CaseRunner#afterFire} for the reason {@link KeyConventionWitnessTest} documents: the case
 * comparator matches a rule-name key on the NAME ALONE and cannot see a half-found collision.
 */
class MixedUnionEdgeWitnessTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    // cwd under surefire is the module dir (backends/drools); the corpus is two levels up.
    private static final Path NUMMERREGISTER =
            Path.of("..", "..", "corpus", "golden", "anchor/jutterswet-nummerregister/ir.json");

    @TempDir static Path tmp;

    // ---------------- 1. the scale flank of the mixed union ----------------

    @Test void the_mixed_union_collides_across_the_scale() throws Exception {
        // S1 ever carries the timeline value 7, W1 the plain value 7.00. ref, measured on this exact model:
        // "oracle: PASS (2 expectations)" — S1 and W1 both flagged; clingo agrees, and the reason is the same
        // in both: ref's Num is a RATIONAL and its lexeme carries no scale, so 7.00 IS 7. BigDecimal.equals
        // does compare scale, so the membership test has to strip it — without that this document answers
        // "consistent" in Drools alone.
        assertEquals(Set.of("eenmalig vondstnummer @ S1", "eenmalig vondstnummer @ W1"),
                flags(mixedUnion(), mixedCase("{ \"number\": \"7.00\" }")),
                "the scalar 7.00 is the number the timeline ever carries, whatever its scale");
    }

    // ---------------- 2. an absent key on the scalar side ----------------

    @Test void an_absent_scalar_key_claims_nothing() throws Exception {
        // W1 supplies no vondstnummer at all (only reekscode), so the key rule of its source, which tests every
        // component off the timeline gevuld, inserts no key for it. ref on this model: "oracle: PASS (1
        // expectations)", consistent; clingo agrees.
        assertEquals(Set.of(), flags(mixedUnion(), mixedCase("null")),
                "an instance without a key value collides with nothing");
    }

    // ---------------- 3. the declared field in the DRL text ----------------

    @Test void the_drl_text_names_the_declared_field_also_in_the_union() throws Exception {
        // "vondstNummer" is deliberate: ident()'s de-shout rule repairs "Vondstnummer" by accident (it
        // lower-cases the first letter of a title-cased word) and leaves this one intact, so only a real
        // resolution through the name contract can render the declared field here. Each source reads its key
        // through the standing of the member, and the standing of each source type reads the bean field: with two
        // sources that is a read of the field on both types, a count, because the sibling source spells the key as
        // declared and would keep a bare "contains" green on its own.
        String drl = drl(edited(NUMMERREGISTER, o -> {
            IrPath.source(o, 0, 0).set("keys", JSON.createArrayNode().add("vondstNummer"));
            ((ArrayNode) o.get("rules").get("uniquenesses")).remove(1);
        }));
        assertEquals(2, count(drl, ".Strandvondst( $invoer : (ComputeHelpers.periods(vondstnummer)) )"),
                "the respelled source reads the declared field: " + drl);
        assertEquals(2, count(drl, ".Wrakvondst( $invoer : (ComputeHelpers.periods(vondstnummer)) )"),
                "and so does its sibling: " + drl);
        assertFalse(drl.contains("vondstNummer"), "the written spelling must not reach the DRL text: " + drl);
    }

    // ---------------- models ----------------

    /** {@code vondstnummer} temporal on Strandvondst, SCALAR on Wrakvondst: the cross-source mixed key. The
     *  document stays verifier-clean (measured 2026-09-03) — each source declares its own key list. */
    private static Path mixedUnion() throws Exception {
        return edited(NUMMERREGISTER, o -> IrPath.attribute(IrPath.objectType(o, "Wrakvondst"), "vondstnummer")
                .remove("timeline"));
    }

    /** A corpus {@code ir.json} with one authored edit, written to a temp file — the corpus file itself is
     *  never touched (the mutation lives in this test, not in the shared zone). */
    private static Path edited(Path source, Consumer<ObjectNode> edit) throws Exception {
        ObjectNode ir = (ObjectNode) JSON.readTree(Files.readString(source));
        edit.accept(ir);
        Path out = Files.createTempFile(tmp, "gemengde-unie", ".json");
        Files.writeString(out, ir.toString());
        return out;
    }

    /** A Strandvondst carrying 7 over one period, and a Wrakvondst whose vondstnummer is the given value
     *  literal — {@code null} standing for "the attribute is absent", which the loader leaves unset. */
    private static JsonNode mixedCase(String wrakKey) throws Exception {
        String attributes = "null".equals(wrakKey)
                ? "{ \"reekscode\": { \"number\": \"3\" } }"
                : "{ \"vondstnummer\": " + wrakKey + " }";
        return JSON.readTree("""
                { "format": "uchar-case-v0", "asOfDate": null,
                  "input": { "instances": [
                      { "label": "S1", "objectType": "Strandvondst",
                        "attributes": { "vondstnummer": { "timeline": [
                            { "from": "2024-01-01", "to": "2025-01-01", "value": { "number": "7" } } ] } },
                        "roles": {}, "characteristics": [] },
                      { "label": "W1", "objectType": "Wrakvondst",
                        "attributes": %s, "roles": {}, "characteristics": [] } ],
                    "parameters": {} },
                  "expected": [ { "kind": "consistent" } ] }
                """.formatted(attributes));
    }

    // ---------------- helpers ----------------

    /** The text of the DRL-text branch of one model, the analysis rendered as DRL text. */
    private static String drl(Path ir) throws Exception {
        return DroolsBackend.build(ir, DroolsBackend.Branch.DRL_TEXT).drl();
    }

    /** Non-overlapping occurrences of a literal. */
    private static int count(String haystack, String needle) {
        int n = 0;
        for (int i = haystack.indexOf(needle); i >= 0; i = haystack.indexOf(needle, i + needle.length())) n++;
        return n;
    }

    /** The §9.5 channel after fire as {@code "<regel> @ <label>"} — the per-INSTANCE flagging ref pins. */
    private static Set<String> flags(Path ir, JsonNode c) throws Exception {
        CaseRunner.AfterFire after = CaseRunner.afterFire(ir, c);
        Set<String> out = new LinkedHashSet<>();
        for (Object f : after.sessionFacts()) {
            if (!"Inconsistentie".equals(f.getClass().getSimpleName())) continue;
            Object subject = f.getClass().getMethod("getSubject").invoke(f);
            String label = "?";
            for (var e : after.input().entrySet()) if (e.getValue() == subject) label = e.getKey();
            out.add(f.getClass().getMethod("getRule").invoke(f) + " @ " + label);
        }
        return out;
    }
}
