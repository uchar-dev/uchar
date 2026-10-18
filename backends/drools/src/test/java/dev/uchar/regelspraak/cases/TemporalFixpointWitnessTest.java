package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import dev.uchar.regelspraak.runtime.RegelSpraakRuntime;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Two witnesses on the temporal WRITE, both through {@link CaseRunner#playCase} over corpus goldens'
 * {@code ir.json} with cases authored HERE (no corpus expectation is read or touched):
 *
 * <ol>
 *   <li>TERMINATION — two temporal writers on one subject reach a fixpoint. Each temporal write whose value
 *       reads an own attribute carries the §9.9 freshness expr, which leaves its pattern class-reactive; an
 *       unconditional {@code drools.update} then re-activates the sibling forever (NO_LOOP blocks only
 *       self-retrigger). Gate 12 sees only "geen fixpunt" on a whole golden, so the two shapes are pinned
 *       here directly — including the ANCHOR-free one, which the idempotency guard used to skip.</li>
 *   <li>RUNTIME-SEMANTICS ch. 7.8, the timeline arm — on a STANDING temporal target a fold with an EMPTY
 *       result decides nothing and the case-supplied input timeline stands WHOLE, while a fold that DOES
 *       decide displaces that input entirely. Both sides are asserted for both executable folds (the ch. 3.7
 *       pro-rata and the ch. 3.8 day count): a one-sided implementation (never write / always write) fails
 *       one of the two.</li>
 * </ol>
 *
 * <p>DUAL-BRANCH since the drl-staande-waarden card (2026-09-18): every replay row below runs through BOTH
 * Drools transpiler branches ({@link BothBranches}) — the exec-model back-end and the classic DRL-text build.
 * Until then the whole matrix ran through the exec branch alone, and the DRL branch, which carried no target
 * classification, wiped the input timeline of {@code jutterswet-borgdeel} and {@code jutterswet-dagentelling}
 * on an empty fold, unwitnessed. The termination rows mirror too: the DRL branch reaches its fixpoint through
 * a narrow {@code @watch} where the exec branch uses a per-segment comparison, so "settles" is a claim about
 * two different mechanisms and each has to be made. The last row (Segment carries no equals) is the input
 * layer's, which is branch-independent, and says so at the site.
 *
 * <p>This class used to ALSO pin the scalar oscillation of {@code jutterswet-waardering-v0vorm} — a
 * leeg-source gelijkstelling against an INITIALISATIE — as a cross-domain non-interference witness: the
 * timeline guard was not to move that then-listed gate-12 divergence. The card drools-scalaire-standing-norm
 * (2026-08-30) BUILT the scalar arm of the same ch. 7.8 norm, which retires that oscillation by construction
 * (the leeg write is suppressed, so the initialisatie decides once). Its subject is gone, so the witness is
 * gone with it; the outcome that replaced it is pinned by {@code StandingScalarNormWitnessTest}.
 */
class TemporalFixpointWitnessTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    // cwd under surefire is the module dir (backends/drools); the corpus is two levels up.
    private static final Path CORPUS = Path.of("..", "..", "corpus", "golden");

    // ---------------- 1. termination ----------------

    @Test void two_unanchored_temporal_writers_reach_a_fixpoint() throws Exception {
        // borgdeel (MAAND) and jaardeel (JAAR) both prorate an own JAAR base over the same kenmerk-tijdlijn on
        // one Jutter, and NEITHER rides a startpunt-anker: this is exactly the pair the anchored-only guard let
        // ping-pong. A non-terminating fire yields "geen fixpunt"; the consistent expectation is the mere carrier.
        String c = """
                { "format": "uchar-case-v0", "asOfDate": null,
                  "input": { "instances": [
                      { "label": "J1", "objectType": "Jutter",
                        "attributes": {
                            "borgtarief": { "timeline": [
                                { "from": "2024-01-01", "to": "2025-01-01", "value": { "number": "12" } } ] },
                            "jaartarief": { "timeline": [
                                { "from": "2024-01-01", "to": "2025-01-01", "value": { "number": "36" } } ] } },
                        "roles": {}, "characteristics": [],
                        "characteristicTimelines": { "borgplichtig": [
                            { "from": "2024-01-01", "to": "2024-03-01" } ] } } ],
                    "parameters": {} },
                  "expected": [ { "kind": "consistent" } ] }
                """;
        play("anchor/jutterswet-borgdeel", c, "two temporal siblings must settle, not run away");
    }

    @Test void three_temporal_writers_on_a_subject_reach_a_fixpoint() throws Exception {
        // the same shape three ways over one Ligplaats — each write re-activates two siblings, so a guard that
        // only damps pairs would still run away here.
        String c = """
                { "format": "uchar-case-v0", "asOfDate": null,
                  "input": { "instances": [
                      { "label": "L1", "objectType": "Ligplaats",
                        "attributes": {
                            "jaarforfait": { "timeline": [
                                { "from": "2024-01-01", "to": "2025-01-01", "value": { "number": "120" } } ] },
                            "staffelbasis": { "timeline": [
                                { "from": "2024-01-01", "to": "2025-01-01", "value": { "number": "240" } } ] },
                            "seizoenbasis": { "timeline": [
                                { "from": "2024-01-01", "to": "2025-01-01", "value": { "number": "360" } } ] } },
                        "roles": {}, "characteristics": [],
                        "characteristicTimelines": { "bezet": [
                            { "from": "2024-04-01", "to": "2024-07-01" } ] } } ],
                    "parameters": {} },
                  "expected": [ { "kind": "consistent" } ] }
                """;
        play("synth2-prorata-breed", c, "three temporal siblings must settle, not run away");
    }

    // ---------------- 2. the ch. 7.8 timeline arm, both sides ----------------

    @Test void day_count_with_empty_opbrengst_leaves_the_input_time_sequence_standing() throws Exception {
        // no borgplichtig period at all -> the ch. 3.8 day count yields the temporal leeg, which decides
        // nothing on this standing target: the input timeline stands WHOLE.
        play("anchor/jutterswet-dagentelling", dayCount("", """
                { "kind": "attributeTimeline", "label": "J1", "attribute": "borgdagen", "timeline": [
                    { "from": "2024-03-01", "to": "2024-04-01", "value": { "number": "99" } } ] }"""),
                "an empty day count writes nothing and the input stands");
    }

    @Test void day_count_with_filled_opbrengst_displaces_the_input_time_sequence() throws Exception {
        // the other side of the same norm: a deciding fold displaces the input ENTIRELY (per target, never per
        // bucket) — the March input segment is gone, replaced by the counted March bucket.
        play("anchor/jutterswet-dagentelling", dayCount("""
                "borgplichtig": [ { "from": "2024-03-01", "to": "2024-03-11" } ]""", """
                { "kind": "attributeTimeline", "label": "J1", "attribute": "borgdagen", "timeline": [
                    { "from": "2024-03-01", "to": "2024-04-01", "value": { "number": "10" } } ] }"""),
                "a deciding day count displaces the input timeline");
    }

    @Test void prorata_with_empty_opbrengst_leaves_the_input_time_sequence_standing() throws Exception {
        // the same norm on the ch. 3.7 pro-rata arm: no true period -> empty result -> the input stands whole.
        play("anchor/jutterswet-borgdeel", borgdeel("", """
                { "kind": "attributeTimeline", "label": "J1", "attribute": "borgdeel", "timeline": [
                    { "from": "2024-03-01", "to": "2024-04-01", "value": { "number": "99" } } ] }"""),
                "an empty pro-rata fold writes nothing and the input stands");
    }

    @Test void prorata_with_filled_opbrengst_displaces_the_input_time_sequence() throws Exception {
        // a January-only true period prorates the JAAR base over the January MAAND bucket; the unrelated March
        // input segment does NOT survive beside it.
        play("anchor/jutterswet-borgdeel", borgdeel("""
                "borgplichtig": [ { "from": "2024-01-01", "to": "2024-02-01" } ]""", """
                { "kind": "attributeTimeline", "label": "J1", "attribute": "borgdeel", "timeline": [
                    { "from": "2024-01-01", "to": "2024-02-01", "value": { "number": "12" } } ] }"""),
                "a deciding pro-rata fold displaces the input timeline");
    }

    // ---------------- the premise of the timeline value of the analysis ----------------

    @Test void a_real_segment_carries_no_equals() throws Exception {
        // NOT mirrored, and branch-independently so: this row stops at the INPUT layer (inputBeans), before
        // any rule fires, so there is no transpiler branch in it to choose.
        // WHY a fact cannot hold a timeline of beans: the generated Segment bean declares no equals, so
        // Objects.equals is unequal on two segment-for-segment identical timelines, and truth maintenance merges
        // equal facts by equals. The analysis therefore holds a timeline as its own value (transpiler.tms.Period).
        // Until card drl-teksttak-op-truth-maintenance this row also held the runtime's per-segment comparison,
        // rsTijdreeksGelijk, the guard of the lowering before truth maintenance; nothing else called it, and it went.
        String c = """
                { "format": "uchar-case-v0", "asOfDate": null,
                  "input": { "instances": [
                      { "label": "J1", "objectType": "Jutter",
                        "attributes": { "borgtarief": { "timeline": [
                            { "from": "2024-01-01", "to": "2025-01-01", "value": { "number": "12" } } ] } },
                        "roles": {}, "characteristics": [] } ],
                    "parameters": {} } }
                """;
        Path ir = CORPUS.resolve("anchor").resolve("jutterswet-borgdeel").resolve("ir.json");
        List<?> a = timelineSegments(ir, c, "J1", "borgtarief");
        List<?> b = timelineSegments(ir, c, "J1", "borgtarief");   // a SECOND load: equal values, distinct instances
        assertEquals(1, a.size());
        assertFalse(java.util.Objects.equals(a, b), "Segment declares no equals — two equal timelines are unequal");
    }

    // ---------------- helpers ----------------

    /** One authored case over a corpus golden's {@code ir.json}, replayed through BOTH transpiler branches —
     *  each must MATCH the expectation ({@link BothBranches}). */
    private static void play(String golden, String caseJson, String message) throws Exception {
        BothBranches.green(CORPUS.resolve(golden).resolve("ir.json"), JSON.readTree(caseJson), message);
    }

    /** The dagentelling case: a pinned borgdagen INPUT segment, the given kenmerk periods, the given expectation. */
    private static String dayCount(String characteristicPeriods, String expectedNode) {
        return """
                { "format": "uchar-case-v0", "asOfDate": null,
                  "input": { "instances": [
                      { "label": "J1", "objectType": "Jutter",
                        "attributes": { "borgdagen": { "timeline": [
                            { "from": "2024-03-01", "to": "2024-04-01", "value": { "number": "99" } } ] } },
                        "roles": {}, "characteristics": [],
                        "characteristicTimelines": { %s } } ],
                    "parameters": {} },
                  "expected": [ %s ] }
                """.formatted(characteristicPeriods, expectedNode);
    }

    /** The borgdeel case: a JAAR base, a pinned borgdeel INPUT segment, the given kenmerk periods + expectation. */
    private static String borgdeel(String characteristicPeriods, String expectedNode) {
        return """
                { "format": "uchar-case-v0", "asOfDate": null,
                  "input": { "instances": [
                      { "label": "J1", "objectType": "Jutter",
                        "attributes": {
                            "borgtarief": { "timeline": [
                                { "from": "2024-01-01", "to": "2025-01-01", "value": { "number": "12" } } ] },
                            "borgdeel": { "timeline": [
                                { "from": "2024-03-01", "to": "2024-04-01", "value": { "number": "99" } } ] } },
                        "roles": {}, "characteristics": [],
                        "characteristicTimelines": { %s } } ],
                    "parameters": {} },
                  "expected": [ %s ] }
                """.formatted(characteristicPeriods, expectedNode);
    }

    /** The {@code List<Segment>} the input layer placed on {@code label}'s {@code veld} bean member. */
    private static List<?> timelineSegments(Path ir, String caseJson, String label, String veld) throws Exception {
        Map<String, Object> beans = CaseRunner.inputBeans(ir, JSON.readTree(caseJson));
        Object bean = beans.get(label);
        assertNotNull(bean, "the case's instance " + label + " produced a bean");
        return (List<?>) RegelSpraakRuntime.get(bean, veld);
    }
}
