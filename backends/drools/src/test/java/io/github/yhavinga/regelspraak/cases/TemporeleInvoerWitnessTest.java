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
 * READ-BACK comparator is witnessed here as well, through {@link CaseRunner#speelCase}: gate 12's corpus
 * pins only lists that happen to match segment-for-segment, so a comparator blinded to waarde or boundaries
 * survives the whole gate set (lenses 1 and 2 proved it by mutation) — its verdicts on a wrong value, a
 * shifted van, a shifted tot, a length mismatch, and a never-written field are asserted directly below.
 * Order-blindness alone has no observable witness (both compared lists arrive ordered), so no test claims it.
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

    @Test void maand_emmer_loopt_van_de_eerste_tot_de_eerste() throws Exception {
        // the MAAND grid, calendar-bound like the JAAR one: first-of-month to first-of-next, never [d, d+1m).
        String c = """
                { "format": "uchar-case-v0", "asOfDate": "2024-03-15",
                  "input": { "instances": [
                      { "label": "J1", "objectType": "Jutter",
                        "attributes": { "wachtvergoeding per maand": { "number": "10" } },
                        "roles": {}, "characteristics": [] } ],
                    "parameters": {} } }
                """;
        List<?> tl = tijdreeks("jutterswet-getijdenwacht", c, "J1", "wachtvergoeding per maand");
        assertEquals(1, tl.size(), "the scalar becomes exactly one month-bucket segment");
        pinSegment(tl.get(0), "2024-03-01", "2024-04-01", "10");
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

    @Test void overlappende_kenmerkperioden_falen_luid() throws Exception {
        // ref rejects overlapping true periods at load; a silent union here would hide a broken case.
        String c = """
                { "format": "uchar-case-v0", "asOfDate": null,
                  "input": { "instances": [
                      { "label": "B1", "objectType": "Bakenmeester",
                        "attributes": {}, "roles": {}, "characteristics": [],
                        "characteristicTimelines": { "dienstdoend": [
                            { "from": "2025-03-01", "to": "2025-06-01" },
                            { "from": "2025-05-01", "to": "2025-08-01" } ] } } ],
                    "parameters": {} } }
                """;
        assertThrows(IllegalArgumentException.class,
                () -> tijdreeks("jutterswet-bakenmeester", c, "B1", "dienstdoend"));
    }

    @Test void kenmerktijdlijn_op_niet_temporeel_kenmerk_is_hard() throws Exception {
        // a case defect, not an uncarried form: ref refuses this shape at load, so an Excluded would file a
        // broken corpus case under "not carried" — hard failure, mirrored.
        String c = """
                { "format": "uchar-case-v0", "asOfDate": null,
                  "input": { "instances": [
                      { "label": "J1", "objectType": "Jutter",
                        "attributes": {}, "roles": {}, "characteristics": [],
                        "characteristicTimelines": { "penninggerechtigd": [
                            { "from": "2026-01-01", "to": "2027-01-01" } ] } } ],
                    "parameters": {} } }
                """;
        assertThrows(IllegalArgumentException.class,
                () -> tijdreeks("jutterswet-penningjaar", c, "J1", "penninggerechtigd"));
    }

    // ---------------- the attributeTimeline read-back comparator (via speelCase) ----------------

    @Test void vergelijker_matcht_schaalveilig() throws Exception {
        // the same timeline at another scale matches: waarde compares by compareTo, never equals.
        assertNull(speelBakenmeester("""
                { "kind": "attributeTimeline", "label": "B1", "attribute": "onderhoudstarief", "timeline": [
                    { "from": "2025-01-01", "to": "2026-01-01", "value": { "number": "24.00" } } ] }"""));
    }

    @Test void vergelijker_ziet_een_verkeerde_waarde() throws Exception {
        String fout = speelBakenmeester("""
                { "kind": "attributeTimeline", "label": "B1", "attribute": "onderhoudstarief", "timeline": [
                    { "from": "2025-01-01", "to": "2026-01-01", "value": { "number": "25" } } ] }""");
        assertTrue(fout != null && fout.contains("verwacht") && fout.contains("werkelijk"),
                "a wrong waarde is a named mismatch, never a pass: " + fout);
    }

    @Test void vergelijker_ziet_een_verschoven_grens() throws Exception {
        // on-grid but one year long shifted: van/tot compare exactly, so this is a mismatch.
        assertTrue(speelBakenmeester("""
                { "kind": "attributeTimeline", "label": "B1", "attribute": "onderhoudstarief", "timeline": [
                    { "from": "2025-01-01", "to": "2027-01-01", "value": { "number": "24" } } ] }""") != null);
    }

    @Test void vergelijker_ziet_een_verschoven_van() throws Exception {
        // same length, same tot, same waarde, ONLY van differs (on-grid, so the validator admits it): the
        // van clause of segGelijk is the sole discriminator here — lens 2 proved its removal survived every
        // other check in the gate set, so this witness is what guards it.
        assertTrue(speelBakenmeester("""
                { "kind": "attributeTimeline", "label": "B1", "attribute": "onderhoudstarief", "timeline": [
                    { "from": "2024-01-01", "to": "2026-01-01", "value": { "number": "24" } } ] }""") != null,
                "a segment that STARTS on the wrong day is a mismatch");
    }

    @Test void vergelijker_ziet_een_lengteverschil() throws Exception {
        assertTrue(speelBakenmeester("""
                { "kind": "attributeTimeline", "label": "B1", "attribute": "onderhoudstarief", "timeline": [
                    { "from": "2025-01-01", "to": "2026-01-01", "value": { "number": "24" } },
                    { "from": "2026-01-01", "to": "2027-01-01", "value": { "number": "24" } } ] }""") != null);
    }

    @Test void nooit_geschreven_veld_matcht_alleen_de_lege_verwachting() throws Exception {
        // a never-written List<Segment> member is null: it reads leeg on every day (ch. 3.1), so it matches
        // the explicitly empty expectation and nothing else.
        assertNull(speelKaal("""
                { "kind": "attributeTimeline", "label": "B1", "attribute": "onderhoudstarief", "timeline": [] }"""));
        String fout = speelKaal("""
                { "kind": "attributeTimeline", "label": "B1", "attribute": "onderhoudstarief", "timeline": [
                    { "from": "2025-01-01", "to": "2026-01-01", "value": { "number": "24" } } ] }""");
        assertTrue(fout != null && fout.contains("draagt geen tijdreeks"), "absence is named: " + fout);
    }

    @Test void verwachting_op_onbekend_label_is_hard() throws Exception {
        // an expectation naming no case instance is a case defect; before this check it reached emmer()
        // with a null granularity and died as an unnamed NPE.
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> speelBakenmeester("""
                { "kind": "attributeTimeline", "label": "B9", "attribute": "onderhoudstarief", "timeline": [] }"""));
        assertTrue(e.getMessage().contains("onbekend label"), e.getMessage());
    }

    /** One authored bakenmeester case — the case1 input (tarief timeline + dienstdoend period) with the given
     *  expectation nodes — through the full replay; returns the failure line, null on a match. */
    private static String speelBakenmeester(String expectedNode) throws Exception {
        String c = """
                { "format": "uchar-case-v0", "asOfDate": null,
                  "input": { "instances": [
                      { "label": "B1", "objectType": "Bakenmeester",
                        "attributes": { "onderhoudstarief": { "timeline": [
                            { "from": "2025-01-01", "to": "2026-01-01", "value": { "number": "24" } } ] } },
                        "roles": {}, "characteristics": [],
                        "characteristicTimelines": { "dienstdoend": [
                            { "from": "2025-03-01", "to": "2025-04-16" } ] } } ],
                    "parameters": {} },
                  "expected": [ %s ] }
                """.formatted(expectedNode);
        return CaseRunner.speelCase(CORPUS.resolve("jutterswet-bakenmeester").resolve("ir.json"), JSON.readTree(c));
    }

    /** The same replay with NO tarief input, so the read-back field was never written. */
    private static String speelKaal(String expectedNode) throws Exception {
        String c = """
                { "format": "uchar-case-v0", "asOfDate": null,
                  "input": { "instances": [
                      { "label": "B1", "objectType": "Bakenmeester",
                        "attributes": {}, "roles": {}, "characteristics": [],
                        "characteristicTimelines": {} } ],
                    "parameters": {} },
                  "expected": [ %s ] }
                """.formatted(expectedNode);
        return CaseRunner.speelCase(CORPUS.resolve("jutterswet-bakenmeester").resolve("ir.json"), JSON.readTree(c));
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
