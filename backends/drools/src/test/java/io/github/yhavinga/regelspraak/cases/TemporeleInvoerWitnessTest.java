package io.github.yhavinga.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.yhavinga.regelspraak.runtime.RegelSpraakRuntime;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The §13.3.6 temporal case-input witness: the shapes {@link CaseRunner} places on the generated
 * {@code List<Segment>} bean members. Gate 12 replays the corpus cases and checks their pinned ref≡clingo
 * VALUES — it never sees the timeline a case input became, so a wrong bucket that happens to fold to the
 * right total would pass it unnoticed. These cases read the beans straight off {@link CaseRunner#invoerBeans}
 * instead, making the segment list itself the assertion.
 *
 * <p>The models are corpus goldens (their {@code ir.json} off the wire, as {@code RoundTripWitnessTest} does);
 * the cases are authored here — no corpus expectation is read or touched. The {@code attributeTimeline}
 * READ-BACK comparator has its witness in gate 12, where {@code jutterswet-bakenmeester} and
 * {@code synth2-clip-over-prorata} pass only if it compares van/tot and value exactly.
 */
class TemporeleInvoerWitnessTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    // cwd under surefire is the module dir (backends/drools); the corpus is two levels up.
    private static final Path CORPUS = Path.of("..", "..", "corpus", "golden");

    @Test void scalar_op_een_temporeel_attribuut_wordt_het_asOfDate_emmersegment() throws Exception {
        // corpus/golden/README.md: the scalar IS the value of the asOfDate bucket. JAAR granularity and
        // asOfDate 2026-03-15 make that the ONE calendar-year bucket [2026-01-01, 2027-01-01) — the same
        // one-bucket timeline ref folds inside the ch. 3.6 collapse, hence the same total.
        List<?> tl = tijdreeks("jutterswet-penningjaar", penningjaar("2026-03-15"),
                "J1", "aantal aangemelde vondsten");
        assertEquals(1, tl.size(), "the scalar becomes exactly one bucket segment");
        pinSegment(tl.get(0), "2026-01-01", "2027-01-01", "14");
    }

    @Test void het_emmerraster_is_kalendergebonden_niet_datumgebonden() throws Exception {
        // a December date lands in the SAME year bucket — the grid is the calendar's, never the date's
        // anniversary; this is what distinguishes the mirrored bucket from a naive [d, d+1 jaar).
        List<?> tl = tijdreeks("jutterswet-penningjaar", penningjaar("2026-12-31"),
                "J1", "aantal aangemelde vondsten");
        pinSegment(tl.get(0), "2026-01-01", "2027-01-01", "14");
    }

    @Test void een_scalar_zonder_asOfDate_wordt_geweigerd_niet_geraden() throws Exception {
        // no evaluation date, no bucket: the convention has nowhere to put the value. A guessed span would be
        // a silent divergence from ref, so this is a reason-carrying refusal.
        CaseRunner.Excluded e = assertThrows(CaseRunner.Excluded.class,
                () -> tijdreeks("jutterswet-penningjaar", penningjaar(null), "J1", "aantal aangemelde vondsten"));
        assertTrue(e.getMessage().contains("asOfDate"), "the reason names the missing evaluation date: " + e.getMessage());
    }

    @Test void tijdlijninvoer_landt_segment_voor_segment_op_de_bean() throws Exception {
        String c = """
                { "format": "uchar-case-v0", "asOfDate": null,
                  "input": { "instances": [
                      { "label": "J1", "objectType": "Jutter",
                        "attributes": { "wachtvergoeding per maand": { "timeline": [
                            { "from": "2023-01-01", "to": "2024-01-01", "value": { "number": "10" } },
                            { "from": "2024-01-01", "to": "2024-07-01", "value": { "number": "11.50" } } ] } },
                        "roles": {}, "characteristics": [] } ],
                    "parameters": {} } }
                """;
        List<?> tl = tijdreeks("jutterswet-getijdenwacht", c, "J1", "wachtvergoeding per maand");
        assertEquals(2, tl.size(), "both segments land, in the written order");
        pinSegment(tl.get(0), "2023-01-01", "2024-01-01", "10");
        pinSegment(tl.get(1), "2024-01-01", "2024-07-01", "11.50");
    }

    @Test void een_grens_naast_het_raster_faalt_luid() throws Exception {
        // ref refuses a segment boundary off the declared granularity grid at load; so does this reader — a
        // mid-year boundary on a JAAR attribute is a hard failure, never a silent acceptance.
        String c = """
                { "format": "uchar-case-v0", "asOfDate": null,
                  "input": { "instances": [
                      { "label": "J1", "objectType": "Jutter",
                        "attributes": { "jaarpremie": { "timeline": [
                            { "from": "2023-03-01", "to": "2024-01-01", "value": { "number": "10" } } ] } },
                        "roles": {}, "characteristics": [] } ],
                    "parameters": {} } }
                """;
        assertThrows(IllegalArgumentException.class,
                () -> tijdreeks("jutterswet-getijdenwacht", c, "J1", "jaarpremie"));
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
                    "parameters": {} } }
                """;
        assertThrows(IllegalArgumentException.class,
                () -> tijdreeks("jutterswet-getijdenwacht", c, "J1", "wachtvergoeding per maand"));
    }

    @Test void kenmerktijdlijn_landt_dagprecies_op_het_kenmerkveld() throws Exception {
        // a true period is DAY-precise regardless of the kenmerk's granularity (the ch. 3.7 pro-rata's whole
        // point): 16 April on a MAAND-granular kenmerk must survive unrounded. The segments carry no value —
        // only van/tot are ever read — and the periods are delivered unsorted here to pin the ordering.
        String c = """
                { "format": "uchar-case-v0", "asOfDate": null,
                  "input": { "instances": [
                      { "label": "B1", "objectType": "Bakenmeester",
                        "attributes": {}, "roles": {}, "characteristics": [],
                        "characteristicTimelines": { "dienstdoend": [
                            { "from": "2025-08-01", "to": "2025-09-01" },
                            { "from": "2025-03-01", "to": "2025-04-16" } ] } } ],
                    "parameters": {} } }
                """;
        List<?> tl = tijdreeks("jutterswet-bakenmeester", c, "B1", "dienstdoend");
        assertEquals(2, tl.size());
        pinSegment(tl.get(0), "2025-03-01", "2025-04-16", null);
        pinSegment(tl.get(1), "2025-08-01", "2025-09-01", null);
    }

    /** The penningjaar case with one scalar input on its JAAR-granular attribute, at {@code asOf} (or none). */
    private static String penningjaar(String asOf) {
        return """
                { "format": "uchar-case-v0", "asOfDate": %s,
                  "input": { "instances": [
                      { "label": "J1", "objectType": "Jutter",
                        "attributes": { "aantal aangemelde vondsten": { "number": "14" } },
                        "roles": {}, "characteristics": [] } ],
                    "parameters": {} } }
                """.formatted(asOf == null ? "null" : "\"" + asOf + "\"");
    }

    /** The {@code List<Segment>} the input layer placed on {@code label}'s {@code veld} bean member. */
    private static List<?> tijdreeks(String golden, String caseJson, String label, String veld) throws Exception {
        Path ir = CORPUS.resolve(golden).resolve("ir.json");
        assertTrue(Files.exists(ir), "golden ir.json present: " + ir.toAbsolutePath());
        JsonNode c = JSON.readTree(caseJson);
        Map<String, Object> beans = CaseRunner.invoerBeans(ir, c);
        Object bean = beans.get(label);
        assertTrue(bean != null, "the case's instance " + label + " produced a bean");
        return (List<?>) RegelSpraakRuntime.get(bean, veld);
    }

    /** One segment's exact shape: van/tot to the day, waarde scale-safe (a null waarde is a true period). */
    private static void pinSegment(Object seg, String van, String tot, String waarde) {
        assertEquals(LocalDate.parse(van), RegelSpraakRuntime.get(seg, "van"));
        assertEquals(LocalDate.parse(tot), RegelSpraakRuntime.get(seg, "tot"));
        BigDecimal w = (BigDecimal) RegelSpraakRuntime.get(seg, "waarde");
        if (waarde == null) assertNull(w, "a true-period segment carries no value");
        else assertEquals(0, new BigDecimal(waarde).compareTo(w), "waarde " + waarde + " vs " + w);
    }
}
