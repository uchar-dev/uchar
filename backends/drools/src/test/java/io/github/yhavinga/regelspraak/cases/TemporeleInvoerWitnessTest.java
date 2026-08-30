package io.github.yhavinga.regelspraak.cases;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The §13.3.6 temporal case-input witness: the shapes {@link CaseRunner} places on the generated
 * {@code List<Segment>} bean members, pinned where gate 12 cannot see them. The gate replays the corpus
 * cases and checks their pinned ref≡clingo VALUES; it never observes the timeline a case input became — a
 * wrong bucket that happens to fold to the right total would pass it. These cases therefore read the input
 * attribute straight back with an {@code attributeTimeline} expectation, so the segment list itself is the
 * assertion.
 *
 * <p>The models are corpus goldens (their {@code ir.json} off the wire, as {@code RoundTripWitnessTest}
 * does); the CASES are authored here and written to a temp file — a corpus case's expectations are gate 12's
 * business and are never touched. Where a value is asserted it is the value the corpus pins for that golden
 * (the ref≡clingo truth), never a value read back off this backend.
 */
class TemporeleInvoerWitnessTest {

    // cwd under surefire is the module dir (backends/drools); the corpus is two levels up.
    private static final Path CORPUS = Path.of("..", "..", "corpus", "golden");

    @Test void scalar_op_een_temporeel_attribuut_wordt_het_asOfDate_emmersegment() throws Exception {
        // corpus/golden/README.md: the scalar IS the value of the asOfDate bucket. JAAR granularity and
        // asOfDate 2026-03-15 make that the ONE calendar-year bucket [2026-01-01, 2027-01-01) — the same
        // one-bucket timeline ref folds inside the ch. 3.6 collapse, hence the same total.
        assertEquals("PLAYED", speel("jutterswet-penningjaar", penningjaar("""
                [ { "from": "2026-01-01", "to": "2027-01-01", "value": { "number": "14" } } ]""")).status());
    }

    @Test void een_ander_jaaremmer_is_een_mismatch_niet_een_vrijbrief() throws Exception {
        // the comparator is not vacuous: the neighbouring year is grid-aligned but the WRONG bucket.
        CaseRunner.Result r = speel("jutterswet-penningjaar", penningjaar("""
                [ { "from": "2025-01-01", "to": "2026-01-01", "value": { "number": "14" } } ]"""));
        assertEquals("FAIL", r.status(), "a wrong bucket must be reported, never absorbed");
    }

    @Test void een_grens_naast_het_raster_faalt_luid() throws Exception {
        // ref refuses a segment boundary off the declared granularity grid at load; so does this reader —
        // a misaligned month boundary on a JAAR attribute is a hard failure, never a silent acceptance.
        assertThrows(IllegalArgumentException.class, () -> speel("jutterswet-penningjaar", penningjaar("""
                [ { "from": "2026-03-01", "to": "2026-04-01", "value": { "number": "14" } } ]""")));
    }

    @Test void tijdlijninvoer_landt_segment_voor_segment_op_de_bean() throws Exception {
        // a multi-segment input reads back segment-for-segment: van/tot exact, order significant, and the
        // value scale-safe — "10.00" must match the "10" that went in (BigDecimal.compareTo, not equals).
        String c = """
                { "format": "uchar-case-v0", "asOfDate": null,
                  "input": { "instances": [
                      { "label": "J1", "objectType": "Jutter",
                        "attributes": { "wachtvergoeding per maand": { "timeline": [
                            { "from": "2023-01-01", "to": "2024-01-01", "value": { "number": "10" } },
                            { "from": "2024-01-01", "to": "2024-07-01", "value": { "number": "11" } } ] } },
                        "roles": {}, "characteristics": [] } ],
                    "parameters": {} },
                  "expected": [ { "kind": "attributeTimeline", "label": "J1",
                      "attribute": "wachtvergoeding per maand", "timeline": [
                          { "from": "2023-01-01", "to": "2024-01-01", "value": { "number": "10.00" } },
                          { "from": "2024-01-01", "to": "2024-07-01", "value": { "number": "11" } } ] } ] }
                """;
        assertEquals("PLAYED", speel("jutterswet-getijdenwacht", c).status());
    }

    @Test void overlappende_invoersegmenten_falen_luid() throws Exception {
        String c = """
                { "format": "uchar-case-v0", "asOfDate": null,
                  "input": { "instances": [
                      { "label": "J1", "objectType": "Jutter",
                        "attributes": { "wachtvergoeding per maand": { "timeline": [
                            { "from": "2023-01-01", "to": "2024-01-01", "value": { "number": "10" } },
                            { "from": "2023-06-01", "to": "2024-07-01", "value": { "number": "11" } } ] } },
                        "roles": {}, "characteristics": [] } ],
                    "parameters": {} },
                  "expected": [ { "kind": "consistent" } ] }
                """;
        assertThrows(IllegalArgumentException.class, () -> speel("jutterswet-getijdenwacht", c));
    }

    @Test void kenmerktijdlijn_landt_op_het_kenmerkveld_en_stuurt_de_clip() throws Exception {
        // the only observable of a true-period timeline is what it CLIPS: with borgplichtig delivered, the
        // MAAND-granular borgtarief clips to the corpus-pinned borgdeel; had the kenmerk timeline not reached
        // the bean's List<Segment> field, the clip would yield nothing. The two segments are the values
        // corpus/golden/synth2-clip-over-prorata pins for ref and clingo.
        String c = """
                { "format": "uchar-case-v0", "asOfDate": null,
                  "input": { "instances": [
                      { "label": "J1", "objectType": "Jutter",
                        "attributes": { "borgtarief": { "timeline": [
                            { "from": "2024-01-01", "to": "2025-01-01", "value": { "number": "12" } } ] } },
                        "roles": {}, "characteristics": [],
                        "characteristicTimelines": { "borgplichtig": [
                            { "from": "2024-02-01", "to": "2024-04-01" } ] } } ],
                    "parameters": {} },
                  "expected": [ { "kind": "attributeTimeline", "label": "J1", "attribute": "borgdeel",
                      "timeline": [
                          { "from": "2024-02-01", "to": "2024-03-01", "value": { "number": "12" } },
                          { "from": "2024-03-01", "to": "2024-04-01", "value": { "number": "12" } } ] } ] }
                """;
        assertEquals("PLAYED", speel("synth2-clip-over-prorata", c).status());
    }

    /** The penningjaar case with one scalar input on its JAAR-granular attribute, read straight back as the
     *  timeline it became. */
    private static String penningjaar(String verwachteTijdlijn) {
        return """
                { "format": "uchar-case-v0", "asOfDate": "2026-03-15",
                  "input": { "instances": [
                      { "label": "J1", "objectType": "Jutter",
                        "attributes": { "aantal aangemelde vondsten": { "number": "14" } },
                        "roles": {}, "characteristics": [] } ],
                    "parameters": { "vondstdrempel": { "number": "12" } } },
                  "expected": [ { "kind": "attributeTimeline", "label": "J1",
                      "attribute": "aantal aangemelde vondsten", "timeline": %s } ] }
                """.formatted(verwachteTijdlijn);
    }

    /** Replay one TEST-AUTHORED case against a corpus golden's model. */
    private static CaseRunner.Result speel(String golden, String caseJson) throws Exception {
        Path ir = CORPUS.resolve(golden).resolve("ir.json");
        assertTrue(Files.exists(ir), "golden ir.json present: " + ir.toAbsolutePath());
        Path tmp = Files.createTempFile("uchar-witness-case", ".json");
        try {
            Files.writeString(tmp, caseJson);
            return CaseRunner.runGolden(ir, List.of(tmp));
        } finally {
            Files.deleteIfExists(tmp);
        }
    }
}
