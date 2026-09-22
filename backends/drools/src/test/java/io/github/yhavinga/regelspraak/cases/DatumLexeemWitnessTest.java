package io.github.yhavinga.regelspraak.cases;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.yhavinga.regelspraak.kern.Literalen;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The day-precision date literal LEXEME (WF-084, IR-SPEC ch. 4.1) on the Drools reader, pinned on BOTH
 * transpiler branches: {@code yyyy-mm-dd}, fixed field widths, matched WHOLE — the grammar the reference
 * interpreter's {@code date_of_iso} and the verifier already hold.
 *
 * <p><b>Red before the fix</b> (card {@code datum-lexeem-hele-match}, measured 2026-09-23 through the backend
 * CLI on {@link #model} with the lexemes below). The shared reader {@code Literalen.parseDatum} SEARCHED for
 * {@code \d{4}-\d{1,2}-\d{1,2}} with {@code find()} and fell back to a day-first {@code d-m-yyyy} arm, so:
 * <ul>
 *   <li>{@code 2024-01-2026} read as 2024-01-20 — a case pinned to that misreading answered {@code PLAYED 1}
 *       on the exec-model branch AND on the DRL-text branch: a silent wrong answer, no diagnostic;</li>
 *   <li>{@code 31-01-2024} (the day-first source snippet, which WF-084 refuses) read as 2024-01-31 and
 *       played on both branches;</li>
 *   <li>{@code 2024-1-31} (one-digit month, outside the fixed widths) read as 2024-01-31 and played on both.</li>
 * </ul>
 * Each is a document no producer writes and the verifier refuses; the interpreter faults on all three. Now
 * the reader answers no value for them and both branches refuse the rule, naming the unreadable lexeme.
 */
class DatumLexeemWitnessTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    /** The three misreadings measured before the fix. */
    private static final String[] MISVORMD = {"2024-01-2026", "31-01-2024", "2024-1-31"};

    /** V1 the day before 31 January, V2 on it (EERDER is strict). */
    private static final String CASE = """
            { "format": "uchar-case-v0", "asOfDate": "2026-03-15",
              "input": { "instances": [
                  { "label": "V1", "objectType": "Vondst",
                    "attributes": { "vondstdag": { "date": "2024-01-30" } }, "roles": {}, "characteristics": [] },
                  { "label": "V2", "objectType": "Vondst",
                    "attributes": { "vondstdag": { "date": "2024-01-31" } }, "roles": {}, "characteristics": [] } ],
                "parameters": {} },
              "expected": [
                { "kind": "characteristicPresent", "label": "V1", "characteristic": "verjaard" },
                { "kind": "characteristicAbsent",  "label": "V2", "characteristic": "verjaard" } ] }
            """;

    // ---------------- the reader itself ----------------

    @Test void de_lezer_leest_het_iso_lexeem() {
        assertEquals(LocalDate.of(2024, 1, 31), Literalen.parseDatum("2024-01-31"));
        assertEquals(LocalDate.of(2024, 2, 29), Literalen.parseDatum("2024-02-29"), "a leap day in a leap year");
    }

    @Test void de_lezer_weigert_elke_vorm_buiten_het_hele_lexeem() {
        assertNull(Literalen.parseDatum("2024-01-2026"), "a multi-digit day is not a prefix to read as 2024-01-20");
        assertNull(Literalen.parseDatum("2024-001-31"), "a multi-digit month is not the lexeme either");
        assertNull(Literalen.parseDatum("x 2024-01-31 y"), "the lexeme is the value, not a substring in it");
        assertNull(Literalen.parseDatum("2024-01-31T12:00:00"), "a datum-tijd is not a date");
        assertNull(Literalen.parseDatum("2024-1-31"), "fixed widths: a one-digit month");
        assertNull(Literalen.parseDatum("2024-01-5"), "fixed widths: a one-digit day");
        assertNull(Literalen.parseDatum("31-01-2024"), "the day-first source snippet (WF-084 refuses it)");
        assertNull(Literalen.parseDatum("dd. 31-01-2024"), "nor with the de-dato prefix");
        assertNull(Literalen.parseDatum("2023-02-29"), "the calendar decides");
        assertNull(Literalen.parseDatum(""), "an empty lexeme");
    }

    // ---------------- both branches ----------------

    @Test void het_iso_lexeem_speelt_op_beide_takken() throws Exception {
        assertNull(speel(model("2024-01-31"), CASE), "the exec-model branch plays the ISO lexeme");
        CaseRunner.Result r = drl(model("2024-01-31"), CASE);
        assertEquals("PLAYED", r.status(), "the DRL-text branch plays it: " + r.reason() + r.caseFailures());
    }

    @Test void een_misvormd_datumlexeem_wordt_op_beide_takken_luid_geweigerd() throws Exception {
        for (String lexeem : MISVORMD) {
            String ir = model(lexeem);
            CaseRunner.Excluded ex = assertThrows(CaseRunner.Excluded.class, () -> speel(ir, CASE),
                    "the exec-model branch refuses the rule for '" + lexeem + "' instead of playing a misreading");
            assertTrue(ex.getMessage().contains("onleesbaar datum-lexeem"),
                    "and names the unreadable lexeme: " + ex.getMessage());
            CaseRunner.Result r = drl(ir, CASE);
            assertEquals("EXCLUDE", r.status(), "the DRL-text branch refuses '" + lexeem + "' too");
            assertTrue(r.reason().contains("Kenmerktoekenning verjaard"),
                    "the refusal names the construct it dropped: " + r.reason());
            assertTrue(r.reason().contains("onleesbaar datum-lexeem"),
                    "in the same words as the other branch: " + r.reason());
        }
    }

    // ---------------- helpers ----------------

    private static String speel(String ir, String caseJson) throws Exception {
        Path tmp = Files.createTempFile("datum-lexeem-witness", ".json");
        try {
            Files.writeString(tmp, ir);
            return CaseRunner.speelCase(tmp, JSON.readTree(caseJson));
        } finally {
            Files.deleteIfExists(tmp);
        }
    }

    private static CaseRunner.Result drl(String ir, String caseJson) throws Exception {
        Path irPad = Files.createTempFile("datum-lexeem-witness-ir", ".json");
        Path zaakPad = Files.createTempFile("datum-lexeem-witness-case", ".json");
        try {
            Files.writeString(irPad, ir);
            Files.writeString(zaakPad, caseJson);
            return CaseRunner.viaDrlTekst(irPad, List.of(zaakPad));
        } finally {
            Files.deleteIfExists(irPad);
            Files.deleteIfExists(zaakPad);
        }
    }

    /** One objecttype with a date attribute and one kenmerktoekenning comparing it against a date literal
     *  carrying {@code lexeem} — the only thing that varies here. */
    private static String model(String lexeem) {
        return """
                { "formaat": "regelspraak-ir-v0",
                  "objecttypen": { "Vondst": { "node": "Objecttype", "naam": "Vondst",
                    "attributen": { "vondstdag": { "node": "DatumInDagen" } },
                    "attribuutMeervoud": {}, "attribuutDimensies": {}, "attribuutGranulariteit": {},
                    "attribuutTijdlijnAantal": {}, "attribuutTijdlijnNaam": {},
                    "kenmerken": [ "verjaard" ], "kenmerkSoorten": { "verjaard": "BIJVOEGLIJK" },
                    "kenmerkGranulariteit": {}, "kenmerkTijdlijnAantal": {},
                    "bezield": false, "meervoud": "Vondsten" } },
                  "domeinen": {}, "parameters": {}, "eenheidstelsels": {}, "feittypen": {}, "dimensies": {},
                  "tijdlijnen": [], "dagsoorten": [], "dagsoortPerEenheid": {},
                  "regels": [ { "node": "Regel", "naam": "Kenmerktoekenning verjaard", "soort": "regel" } ],
                  "resolvedRegels": [ { "node": "ResolvedRegel", "naam": "Kenmerktoekenning verjaard",
                    "soort": "KENMERKTOEKENNING",
                    "doel": { "node": "AttribuutRef", "attribuut": "verjaard", "objecttype": "Vondst",
                      "onderwerpBron": "Een Vondst", "navigatieRol": null, "type": { "node": "Booleaan" },
                      "bron": "Een Vondst is verjaard", "set": false, "navigatiePad": [] },
                    "waarde": { "node": "Literal", "waarde": "waar", "type": { "node": "Booleaan" }, "bron": "waar" },
                    "voorwaarde": { "node": "Vergelijking", "op": "EERDER",
                      "links": { "node": "AttribuutRef", "attribuut": "vondstdag", "objecttype": "Vondst",
                        "onderwerpBron": "zijn", "navigatieRol": null, "type": { "node": "DatumInDagen" },
                        "bron": "zijn vondstdag", "set": false, "navigatiePad": [] },
                      "rechts": { "node": "Literal", "waarde": "%s", "type": { "node": "DatumInDagen" },
                        "bron": "31-01-2024" },
                      "periode": null, "bron": "zijn vondstdag eerder is dan 31-01-2024" },
                    "geldigheid": { "node": "Geldigheid", "vanaf": null, "tot": null } } ],
                  "resolvedDagsoorten": [], "objectCreaties": [], "verdelingen": [], "consistenties": [],
                  "uniciteiten": [], "uniciteitUnies": [], "feitCreaties": [], "resolvedStartpunten": [],
                  "regelGroepen": [], "diagnostics": [] }
                """.formatted(lexeem);
    }
}
