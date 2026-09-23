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
 *
 * <p><b>Only the comparison names the lexeme.</b> At every other position the literal reaches, the rule is
 * refused under that position's OWN reason — never answered. The pins below hold the reason that actually
 * comes out at a value position, a period bound and a used day-kind definition, so a branch that turns a
 * null into a played rule (measured by the lens, 2026-09-23: dropping the null guard on the exec-model value
 * position answered "verwacht 2024-01-20, werkelijk leeg" with the whole suite green) fails here.
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
        assertNull(Literalen.parseDatum("dd. 2024-01-31"), "the de-dato prefix before an ISO lexeme is not the lexeme");
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

    // ---------------- the other positions: refused under their own reason, never answered ----------------

    /** A GELIJKSTELLING whose value is the date literal. The ISO lexeme plays on both branches; a
     *  misformed one is refused as "waarde buiten scope" (exec) and as a datumliteral marker (DRL). */
    @Test void een_misvormd_datumlexeem_op_een_waardepositie_wordt_op_beide_takken_geweigerd() throws Exception {
        assertNull(speel(waardeModel("2024-01-20"), WAARDE_CASE), "the ISO lexeme plays at a value position");
        assertEquals("PLAYED", drl(waardeModel("2024-01-20"), WAARDE_CASE).status());
        for (String lexeem : MISVORMD) {
            String ir = waardeModel(lexeem);
            CaseRunner.Excluded ex = assertThrows(CaseRunner.Excluded.class, () -> speel(ir, WAARDE_CASE),
                    "the exec-model branch refuses the value '" + lexeem + "' instead of writing leeg");
            assertTrue(ex.getMessage().contains("Peildag") && ex.getMessage().contains("waarde buiten scope"),
                    ex.getMessage());
            CaseRunner.Result r = drl(ir, WAARDE_CASE);
            assertEquals("EXCLUDE", r.status(), "the DRL-text branch refuses the value '" + lexeem + "'");
            assertTrue(r.reason().contains("TODO datumliteral: " + lexeem), r.reason());
        }
    }

    /** A period bound (§8.4.3): the corpus golden {@code jutterswet-premievenster} with its first window's
     *  upper bound misformed. Before the fix both branches read {@code 2025-01-2026} as 2025-01-20 and played
     *  the case; now the exec-model branch names the lexeme and the DRL branch refuses the bound. */
    @Test void een_misvormde_periodegrens_wordt_op_beide_takken_geweigerd() throws Exception {
        Path golden = Path.of("..", "..", "corpus", "golden", "jutterswet-premievenster");
        assertTrue(Files.exists(golden.resolve("ir.json")), "golden present: " + golden.toAbsolutePath());
        com.fasterxml.jackson.databind.node.ObjectNode doc =
                (com.fasterxml.jackson.databind.node.ObjectNode) JSON.readTree(golden.resolve("ir.json").toFile());
        com.fasterxml.jackson.databind.node.ObjectNode tot =
                (com.fasterxml.jackson.databind.node.ObjectNode) doc.path("rules").path("resolvedRegels").path(1).path("voorwaarde").path("tot");
        assertEquals("2025-01-01", tot.path("waarde").asText(), "the bound this pin twists");
        tot.put("waarde", "2025-01-2026");
        String ir = JSON.writeValueAsString(doc);
        String zaak = Files.readString(golden.resolve("case1-binnen-venster.json"));
        CaseRunner.Excluded ex = assertThrows(CaseRunner.Excluded.class, () -> speel(ir, zaak));
        assertTrue(ex.getMessage().contains("vondstpremie van een vondst")
                && ex.getMessage().contains("onleesbaar datum-lexeem"), ex.getMessage());
        CaseRunner.Result r = drl(ir, zaak);
        assertEquals("EXCLUDE", r.status());
        assertTrue(r.reason().contains("TODO periodevergelijking-grens"), r.reason());
    }

    /** A USED day-kind definition: the rule tests "zijn vondstdag is een peildag" and the day kind compares
     *  "de dag" with the date literal. Before the fix {@code 2024-01-2026} and {@code 20-01-2024} both played
     *  as 2024-01-20 on the exec-model branch; now each branch refuses the rule under its day-kind reason. */
    @Test void een_misvormd_datumlexeem_in_een_gebruikte_dagsoort_wordt_op_beide_takken_geweigerd() throws Exception {
        assertNull(speel(dagsoortModel("2024-01-20"), DAGSOORT_CASE), "the ISO lexeme plays in a day kind");
        assertEquals("PLAYED", drl(dagsoortModel("2024-01-20"), DAGSOORT_CASE).status());
        for (String lexeem : MISVORMD) {
            String ir = dagsoortModel(lexeem);
            CaseRunner.Excluded ex = assertThrows(CaseRunner.Excluded.class, () -> speel(ir, DAGSOORT_CASE),
                    "the exec-model branch refuses the day kind over '" + lexeem + "'");
            assertTrue(ex.getMessage().contains("Kenmerktoekenning verjaard")
                    && ex.getMessage().contains("voorwaarde buiten Phase-B scope"), ex.getMessage());
            CaseRunner.Result r = drl(ir, DAGSOORT_CASE);
            assertEquals("EXCLUDE", r.status(), "the DRL-text branch refuses it too");
            assertTrue(r.reason().contains("TODO dagsoortcontrole 'peildag'"), r.reason());
        }
    }

    /** V1's peildag is written from the literal; the ISO lexeme writes 2024-01-20. */
    private static final String WAARDE_CASE = """
            { "format": "uchar-case-v0", "asOfDate": "2026-03-15",
              "input": { "instances": [
                  { "label": "V1", "objectType": "Vondst", "attributes": {}, "roles": {}, "characteristics": [] } ],
                "parameters": {} },
              "expected": [ { "kind": "attributeValue", "label": "V1", "attribute": "peildag",
                              "value": { "date": "2024-01-20" } } ] }
            """;

    /** V1 falls on the day the day kind names (for the ISO lexeme), V2 does not. */
    private static final String DAGSOORT_CASE = """
            { "format": "uchar-case-v0", "asOfDate": "2026-03-15",
              "input": { "instances": [
                  { "label": "V1", "objectType": "Vondst",
                    "attributes": { "vondstdag": { "date": "2024-01-20" } }, "roles": {}, "characteristics": [] },
                  { "label": "V2", "objectType": "Vondst",
                    "attributes": { "vondstdag": { "date": "2024-01-21" } }, "roles": {}, "characteristics": [] } ],
                "parameters": {} },
              "expected": [
                { "kind": "characteristicPresent", "label": "V1", "characteristic": "verjaard" },
                { "kind": "characteristicAbsent",  "label": "V2", "characteristic": "verjaard" } ] }
            """;

    private static String waardeModel(String lexeem) {
        return MODEL_KOP.formatted("{ \"node\": \"Attribute\", \"name\": \"peildag\", \"datatype\": { \"node\": \"DatumInDagen\" } }") + """
                  "timelines": [], "dayKinds": [], "dagsoortPerEenheid": {} },
                "rules": {
                  "resolvedRegels": [ { "node": "ResolvedRegel", "naam": "Peildag", "soort": "GELIJKSTELLING",
                    "doel": { "node": "AttribuutRef", "attribuut": "peildag", "objecttype": "Vondst",
                      "onderwerpBron": "een Vondst", "navigatieRol": null, "type": { "node": "DatumInDagen" },
                      "bron": "De peildag van een Vondst", "set": false, "navigatiePad": [] },
                    "waarde": { "node": "Literal", "waarde": "%s", "type": { "node": "DatumInDagen" }, "bron": "20-01-2024" },
                    "voorwaarde": null,
                    "geldigheid": { "node": "Geldigheid", "vanaf": null, "tot": null } } ],
                  "resolvedDagsoorten": [],
                """.formatted(lexeem) + MODEL_STAART;
    }

    private static String dagsoortModel(String lexeem) {
        return MODEL_KOP.formatted("{ \"node\": \"Attribute\", \"name\": \"vondstdag\", \"datatype\": { \"node\": \"DatumInDagen\" } }") + """
                  "timelines": [], "dayKinds": [ "peildag" ], "dagsoortPerEenheid": {} },
                "rules": {
                  "resolvedRegels": [ { "node": "ResolvedRegel", "naam": "Kenmerktoekenning verjaard",
                    "soort": "KENMERKTOEKENNING",
                    "doel": { "node": "AttribuutRef", "attribuut": "verjaard", "objecttype": "Vondst",
                      "onderwerpBron": "Een Vondst", "navigatieRol": null, "type": { "node": "Booleaan" },
                      "bron": "Een Vondst is verjaard", "set": false, "navigatiePad": [] },
                    "waarde": { "node": "Literal", "waarde": "waar", "type": { "node": "Booleaan" }, "bron": "waar" },
                    "voorwaarde": { "node": "Predicaat", "soort": "DAGSOORT",
                      "operand": { "node": "AttribuutRef", "attribuut": "vondstdag", "objecttype": "Vondst",
                        "onderwerpBron": "zijn", "navigatieRol": null, "type": { "node": "DatumInDagen" },
                        "bron": "zijn vondstdag", "set": false, "navigatiePad": [] },
                      "dagsoort": "peildag", "negatie": false, "cijfers": 0, "periode": null,
                      "bron": "zijn vondstdag is een peildag" },
                    "geldigheid": { "node": "Geldigheid", "vanaf": null, "tot": null } } ],
                  "resolvedDagsoorten": [ { "node": "ResolvedDagsoort", "naam": "peildag",
                    "voorwaarde": { "node": "Vergelijking", "op": "GELIJK",
                      "links": { "node": "Constante", "naam": "de dag", "type": { "node": "DatumInDagen" }, "bron": "de dag" },
                      "rechts": { "node": "Literal", "waarde": "%s", "type": { "node": "DatumInDagen" }, "bron": "20-01-2024" },
                      "periode": null, "bron": "de dag gelijk is aan 20-01-2024" },
                    "geldigheid": { "node": "Geldigheid", "vanaf": null, "tot": null } } ],
                """.formatted(lexeem) + MODEL_STAART;
    }

    private static final String MODEL_KOP = """
            { "format": "uchar-ir-v1",
              "model": {
                "objectTypes": [ { "node": "ObjectType", "name": "Vondst", "animate": false, "plural": "Vondsten",
                  "attributes": [ %s ],
                  "characteristics": [ { "node": "Characteristic", "name": "verjaard", "kind": "BIJVOEGLIJK" } ] } ],
                "factTypes": [], "parameters": [], "domains": [], "unitSystems": [], "dimensions": [],
            """;

    private static final String MODEL_STAART = """
                "objectCreaties": [], "verdelingen": [], "consistenties": [],
                "uniciteiten": [], "uniciteitUnies": [], "feitCreaties": [], "resolvedStartpunten": [],
                "regelGroepen": [] },
              "diagnostics": [] }
            """;

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
                { "format": "uchar-ir-v1",
                  "model": {
                    "objectTypes": [ { "node": "ObjectType", "name": "Vondst", "animate": false, "plural": "Vondsten",
                      "attributes": [ { "node": "Attribute", "name": "vondstdag", "datatype": { "node": "DatumInDagen" } } ],
                      "characteristics": [ { "node": "Characteristic", "name": "verjaard", "kind": "BIJVOEGLIJK" } ] } ],
                    "factTypes": [], "parameters": [], "domains": [], "unitSystems": [], "dimensions": [],
                    "timelines": [], "dayKinds": [], "dagsoortPerEenheid": {} },
                  "rules": {
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
                  "regelGroepen": [] },
                  "diagnostics": [] }
                """.formatted(lexeem);
    }
}
