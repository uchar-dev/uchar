package io.github.yhavinga.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.yhavinga.regelspraak.runtime.RegelSpraakRuntime;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Two witnesses on the temporal WRITE, both through {@link CaseRunner#speelCase} over corpus goldens'
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
 * <p>The oscillation of {@code jutterswet-waardering-v0vorm} — a leeg-source gelijkstelling against an
 * INITIALISATIE, a SCALAR write and a different mechanism (ratified as a listed gate-12 divergence) — is
 * asserted UNCHANGED here, so the termination fix cannot silently move that listed entry.
 */
class TemporeelFixpuntWitnessTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    // cwd under surefire is the module dir (backends/drools); the corpus is two levels up.
    private static final Path CORPUS = Path.of("..", "..", "corpus", "golden");

    // ---------------- 1. termination ----------------

    @Test void twee_ongeankerde_temporele_schrijvers_bereiken_een_fixpunt() throws Exception {
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
        assertNull(speel("anchor/jutterswet-borgdeel", c), "two temporal siblings must settle, not run away");
    }

    @Test void drie_temporele_schrijvers_op_een_subject_bereiken_een_fixpunt() throws Exception {
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
        assertNull(speel("synth2-prorata-breed", c), "three temporal siblings must settle, not run away");
    }

    @Test void de_gelijste_scalar_oscillatie_blijft_oscilleren() throws Exception {
        // NOT a timeline write and NOT this fix's domain: a leeg-source gelijkstelling (writes null) against an
        // INITIALISATIE (refills 25) on a SCALAR attribute. It is a ratified, listed gate-12 divergence; the
        // timeline guard must leave it exactly where it was, so its non-termination is pinned rather than assumed.
        String c = """
                { "format": "uchar-case-v0", "asOfDate": null,
                  "input": { "instances": [
                      { "label": "J1", "objectType": "Jutter",
                        "attributes": {}, "roles": {}, "characteristics": [ "erkend" ] } ],
                    "parameters": {} },
                  "expected": [ { "kind": "consistent" } ] }
                """;
        String fout = speel("anchor/jutterswet-waardering-v0vorm", c);
        assertNotNull(fout, "the listed scalar oscillation must still be observable");
        assertTrue(fout.contains("geen fixpunt"), "unchanged in kind, not merely in outcome: " + fout);
    }

    // ---------------- 2. the ch. 7.8 timeline arm, both sides ----------------

    @Test void dagtelling_met_lege_opbrengst_laat_de_invoertijdreeks_staan() throws Exception {
        // no borgplichtig period at all -> the ch. 3.8 day count yields the temporal leeg, which decides
        // nothing on this standing target: the input timeline stands WHOLE.
        assertNull(speel("anchor/jutterswet-dagentelling", dagentelling("", """
                { "kind": "attributeTimeline", "label": "J1", "attribute": "borgdagen", "timeline": [
                    { "from": "2024-03-01", "to": "2024-04-01", "value": { "number": "99" } } ] }""")),
                "an empty day count writes nothing and the input stands");
    }

    @Test void dagtelling_met_gevulde_opbrengst_verdringt_de_invoertijdreeks() throws Exception {
        // the other side of the same norm: a deciding fold displaces the input ENTIRELY (per target, never per
        // bucket) — the March input segment is gone, replaced by the counted March bucket.
        assertNull(speel("anchor/jutterswet-dagentelling", dagentelling("""
                "borgplichtig": [ { "from": "2024-03-01", "to": "2024-03-11" } ]""", """
                { "kind": "attributeTimeline", "label": "J1", "attribute": "borgdagen", "timeline": [
                    { "from": "2024-03-01", "to": "2024-04-01", "value": { "number": "10" } } ] }""")),
                "a deciding day count displaces the input timeline");
    }

    @Test void prorata_met_lege_opbrengst_laat_de_invoertijdreeks_staan() throws Exception {
        // the same norm on the ch. 3.7 pro-rata arm: no true period -> empty result -> the input stands whole.
        assertNull(speel("anchor/jutterswet-borgdeel", borgdeel("", """
                { "kind": "attributeTimeline", "label": "J1", "attribute": "borgdeel", "timeline": [
                    { "from": "2024-03-01", "to": "2024-04-01", "value": { "number": "99" } } ] }""")),
                "an empty pro-rata fold writes nothing and the input stands");
    }

    @Test void prorata_met_gevulde_opbrengst_verdringt_de_invoertijdreeks() throws Exception {
        // a January-only true period prorates the JAAR base over the January MAAND bucket; the unrelated March
        // input segment does NOT survive beside it.
        assertNull(speel("anchor/jutterswet-borgdeel", borgdeel("""
                "borgplichtig": [ { "from": "2024-01-01", "to": "2024-02-01" } ]""", """
                { "kind": "attributeTimeline", "label": "J1", "attribute": "borgdeel", "timeline": [
                    { "from": "2024-01-01", "to": "2024-02-01", "value": { "number": "12" } } ] }""")),
                "a deciding pro-rata fold displaces the input timeline");
    }

    // ---------------- the premise of the timeline idempotency guard ----------------

    @Test void een_echt_segment_draagt_geen_equals() throws Exception {
        // WHY the timeline write cannot use the scalar guard: the generated Segment bean declares no equals,
        // so Objects.equals is unequal on two segment-for-segment identical timelines and an equals-guarded
        // write would re-propagate its subject on EVERY recompute. rsTijdreeksGelijk compares per segment.
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
        List<?> a = tijdreeks(ir, c, "J1", "borgtarief");
        List<?> b = tijdreeks(ir, c, "J1", "borgtarief");   // a SECOND load: equal values, distinct instances
        assertEquals(1, a.size());
        assertFalse(java.util.Objects.equals(a, b), "Segment declares no equals — the scalar guard always differs");
        assertTrue(RegelSpraakRuntime.rsTijdreeksGelijk(a, b), "the per-segment comparison sees them as equal");
    }

    // ---------------- helpers ----------------

    /** One authored case over a corpus golden's {@code ir.json}; the per-case failure line, null on a match. */
    private static String speel(String golden, String caseJson) throws Exception {
        Path ir = CORPUS.resolve(golden).resolve("ir.json");
        return CaseRunner.speelCase(ir, JSON.readTree(caseJson));
    }

    /** The dagentelling case: a pinned borgdagen INPUT segment, the given kenmerk periods, the given expectation. */
    private static String dagentelling(String kenmerkPerioden, String expectedNode) {
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
                """.formatted(kenmerkPerioden, expectedNode);
    }

    /** The borgdeel case: a JAAR base, a pinned borgdeel INPUT segment, the given kenmerk periods + expectation. */
    private static String borgdeel(String kenmerkPerioden, String expectedNode) {
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
                """.formatted(kenmerkPerioden, expectedNode);
    }

    /** The {@code List<Segment>} the input layer placed on {@code label}'s {@code veld} bean member. */
    private static List<?> tijdreeks(Path ir, String caseJson, String label, String veld) throws Exception {
        Map<String, Object> beans = CaseRunner.invoerBeans(ir, JSON.readTree(caseJson));
        Object bean = beans.get(label);
        assertNotNull(bean, "the case's instance " + label + " produced a bean");
        return (List<?>) RegelSpraakRuntime.get(bean, veld);
    }
}
