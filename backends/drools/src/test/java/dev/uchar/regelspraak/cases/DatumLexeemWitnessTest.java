package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.ObjectMapper;

import dev.uchar.regelspraak.kern.Literalen;
import dev.uchar.regelspraak.loader.IrLoader;

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
 * Each is a document no producer writes and the verifier refuses; the interpreter faults on all three. Since
 * that card the reader answers no value for them, and both branches refused the rule while lowering it.
 *
 * <p><b>Since the fix round of the L4 landing the loader refuses them first</b>, in WF-084's sentence at the
 * literal's value, before either branch lowers anything — the position rows below pin that refusal at a
 * comparison, a value position, a period bound and a used day-kind definition. Until then only the comparison
 * named the lexeme: at every other position the rule was refused under that position's own reason ("waarde
 * buiten scope", "TODO periodevergelijking-grens", "TODO dagsoortcontrole"), which names no lexeme, and a branch
 * that turned the reader's null into a played rule went unseen (measured by the lens, 2026-09-23: dropping the
 * null guard on the exec-model value position answered "verwacht 2024-01-20, werkelijk leeg" with the whole suite
 * green). Those guards stay in both branches, and no document read off the wire reaches them any more. The reader
 * rows keep pinning the reader's own grammar.
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
        for (String lexeem : MISVORMD)
            geweigerdBijHetLaden(model(lexeem), CASE, "/rules/rules/0/versions/0/condition/right/value", lexeem);
    }

    // ---------------- the other positions: refused under their own reason, never answered ----------------

    /** A GELIJKSTELLING whose value is the date literal. The ISO lexeme plays on both branches; a misformed one
     *  was refused as "waarde buiten scope" (exec) and as a datumliteral marker (DRL), and is refused at load now. */
    @Test void een_misvormd_datumlexeem_op_een_waardepositie_wordt_op_beide_takken_geweigerd() throws Exception {
        assertNull(speel(waardeModel("2024-01-20"), WAARDE_CASE), "the ISO lexeme plays at a value position");
        assertEquals("PLAYED", drl(waardeModel("2024-01-20"), WAARDE_CASE).status());
        for (String lexeem : MISVORMD)
            geweigerdBijHetLaden(waardeModel(lexeem), WAARDE_CASE, "/rules/rules/0/versions/0/value/value", lexeem);
    }

    /** A period bound (§8.4.3): the corpus golden {@code jutterswet-premievenster} with its first window's
     *  upper bound misformed. Before the fix both branches read {@code 2025-01-2026} as 2025-01-20 and played
     *  the case; then the exec-model branch named the lexeme and the DRL branch refused the bound; now the loader
     *  refuses it. */
    @Test void een_misvormde_periodegrens_wordt_op_beide_takken_geweigerd() throws Exception {
        Path golden = Path.of("..", "..", "corpus", "golden", "jutterswet-premievenster");
        assertTrue(Files.exists(golden.resolve("ir.json")), "golden present: " + golden.toAbsolutePath());
        com.fasterxml.jackson.databind.node.ObjectNode doc =
                (com.fasterxml.jackson.databind.node.ObjectNode) JSON.readTree(golden.resolve("ir.json").toFile());
        com.fasterxml.jackson.databind.node.ObjectNode tot = (com.fasterxml.jackson.databind.node.ObjectNode)
                IrPad.versie(doc, "rules", 1, 0).path("condition").path("to").path("value");
        assertEquals("2025-01-01", tot.path("date").asText(), "the bound this pin twists");
        tot.put("date", "2025-01-2026");
        String ir = JSON.writeValueAsString(doc);
        String zaak = Files.readString(golden.resolve("case1-binnen-venster.json"));
        geweigerdBijHetLaden(ir, zaak, "/rules/rules/1/versions/0/condition/to/value", "2025-01-2026");
    }

    /** A USED day-kind definition: the rule tests "zijn vondstdag is een peildag" and the day kind compares
     *  the bound day with the date literal. Before the fix {@code 2024-01-2026} and {@code 20-01-2024} both played
     *  as 2024-01-20 on the exec-model branch; then each branch refused the rule under its day-kind reason; now the
     *  loader refuses the literal. */
    @Test void een_misvormd_datumlexeem_in_een_gebruikte_dagsoort_wordt_op_beide_takken_geweigerd() throws Exception {
        // Both branches refuse the rule that tests the day kind as its condition, by name, in the one sentence of the
        // analysis, as the reference interpreter and the ASP backend refuse that form outside a day-kind definition
        // (the document is verifier-clean, measured 2026-10-10). The destructive exec-model branch played it, and the
        // destructive DRL-text branch until card drl-teksttak-op-truth-maintenance; the loss is accepted (card
        // tms-omschakeling). The malformed lexemes below are refused at load, before any branch.
        CaseRunner.Excluded e = assertThrows(CaseRunner.Excluded.class,
                () -> speelTms(dagsoortModel("2024-01-20"), DAGSOORT_CASE));
        assertTrue(e.getMessage().contains("een Predicate dayKind als voorwaarde ('zijn vondstdag is een peildag')"),
                e.getMessage());
        CaseRunner.Result tekst = drl(dagsoortModel("2024-01-20"), DAGSOORT_CASE);
        assertEquals("EXCLUDE", tekst.status(), "the DRL-text branch refuses the ISO lexeme's rule as well: " + tekst);
        assertEquals(e.getMessage(), tekst.reason());
        for (String lexeem : MISVORMD)
            geweigerdBijHetLaden(dagsoortModel(lexeem), DAGSOORT_CASE, "/rules/dayKindDefinitions/0/condition/right/value",
                    lexeem);
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
        return MODEL_KOP.formatted("{ \"node\": \"Attribute\", \"name\": \"peildag\", \"datatype\": { \"node\": \"Date\", \"precision\": \"day\" } }") + """
                  "timelines": [], "dayKinds": [] },
                "rules": {
                  "rules": [ { "node": "Rule", "name": "Peildag", "versions": [ {
                      "validity": { "node": "Validity" },
                      "target": { "node": "AttributeReference", "attribute": "peildag", "subjectType": "Vondst",
                      "source": { "snippet": "De peildag van een Vondst" } },
                      "value": { "node": "Literal", "value": { "date": "%s" }, "type": { "node": "Date", "precision": "day" }, "source": { "snippet": "20-01-2024" } },
                      "initialisation": false,
                      "presentation": "equation" } ] } ],
                  "dayKindDefinitions": [],
                """.formatted(lexeem) + MODEL_STAART;
    }

    private static String dagsoortModel(String lexeem) {
        return MODEL_KOP.formatted("{ \"node\": \"Attribute\", \"name\": \"vondstdag\", \"datatype\": { \"node\": \"Date\", \"precision\": \"day\" } }") + """
                  "timelines": [], "dayKinds": [ { "node": "DayKind", "name": "peildag" } ] },
                "rules": {
                  "rules": [ { "node": "Rule", "name": "Kenmerktoekenning verjaard", "versions": [ {
                      "validity": { "node": "Validity" },
                      "target": { "node": "AttributeReference", "attribute": "verjaard", "subjectType": "Vondst",
                      "source": { "snippet": "Een Vondst is verjaard" } },
                      "value": { "node": "Literal", "value": { "boolean": true }, "type": { "node": "Boolean" }, "source": { "snippet": "waar" } },
                      "condition": { "node": "Predicate", "kind": "dayKind",
                      "operand": { "node": "AttributeReference", "attribute": "vondstdag", "subjectType": "Vondst",
                        "source": { "snippet": "zijn vondstdag" } },
                      "dayKind": "peildag", "negated": false, "digits": 0,
                      "source": { "snippet": "zijn vondstdag is een peildag" } },
                      "initialisation": false } ] } ],
                  "dayKindDefinitions": [ { "node": "DayKindDefinition", "dayKind": "peildag",
                    "condition": { "node": "Comparison", "op": "equal",
                      "left": { "node": "BoundDay", "type": { "node": "Date", "precision": "day" }, "source": { "snippet": "de dag" } },
                      "right": { "node": "Literal", "value": { "date": "%s" }, "type": { "node": "Date", "precision": "day" }, "source": { "snippet": "20-01-2024" } },
                      "source": { "snippet": "de dag gelijk is aan 20-01-2024" } },
                    "validity": { "node": "Validity" } } ],
                """.formatted(lexeem) + MODEL_STAART;
    }

    private static final String MODEL_KOP = """
            { "format": "uchar-ir-v1",
              "model": {
                "objectTypes": [ { "node": "ObjectType", "name": "Vondst", "animate": false, "plural": "Vondsten",
                  "attributes": [ %s ],
                  "characteristics": [ { "node": "Characteristic", "name": "verjaard", "kind": "adjectival" } ] } ],
                "factTypes": [], "parameters": [], "domains": [], "unitSystems": [], "dimensions": [],
            """;

    private static final String MODEL_STAART = """
                "objectCreations": [], "allocations": [], "consistencies": [],
                "uniquenesses": [], "factCreations": [], "startPointDeterminations": [],
                "ruleGroups": [] },
              "diagnostics": [] }
            """;

    // ---------------- helpers ----------------

    /** Both branches refuse the document at load, at {@code plek}, in the verifier's WF-084 sentence: the loader is
     *  shared, and each branch is asked on its own so neither can stop reaching it unseen. */
    private static void geweigerdBijHetLaden(String ir, String zaak, String plek, String lexeem) {
        for (boolean viaDrl : new boolean[] { false, true }) {
            IrLoader.Documentfout e = assertThrows(IrLoader.Documentfout.class,
                    () -> { if (viaDrl) drl(ir, zaak); else speel(ir, zaak); },
                    (viaDrl ? "drl-tekst" : "exec-model") + " refuses '" + lexeem + "' at load");
            assertEquals(plek, e.plek(), e.getMessage());
            assertTrue(e.getMessage().endsWith(": date literal lexeme '" + lexeem + "' is not the ISO form yyyy-mm-dd"
                    + " (fixed field widths, valid ranges — ch. 4.1)"), e.getMessage());
        }
    }

    /** As {@link #speel}, on the exec-model branch. */
    private static String speelTms(String ir, String caseJson) throws Exception {
        Path tmp = Files.createTempFile("datum-lexeem-witness", ".json");
        try {
            Files.writeString(tmp, ir);
            return CaseRunner.speelCase(tmp, JSON.readTree(caseJson), DroolsBackend.Branch.EXEC_MODEL);
        } finally {
            Files.deleteIfExists(tmp);
        }
    }

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
                      "attributes": [ { "node": "Attribute", "name": "vondstdag", "datatype": { "node": "Date", "precision": "day" } } ],
                      "characteristics": [ { "node": "Characteristic", "name": "verjaard", "kind": "adjectival" } ] } ],
                    "factTypes": [], "parameters": [], "domains": [], "unitSystems": [], "dimensions": [],
                    "timelines": [], "dayKinds": [] },
                  "rules": {
                  "rules": [ { "node": "Rule", "name": "Kenmerktoekenning verjaard", "versions": [ {
                      "validity": { "node": "Validity" },
                      "target": { "node": "AttributeReference", "attribute": "verjaard", "subjectType": "Vondst",
                      "source": { "snippet": "Een Vondst is verjaard" } },
                      "value": { "node": "Literal", "value": { "boolean": true }, "type": { "node": "Boolean" }, "source": { "snippet": "waar" } },
                      "condition": { "node": "Comparison", "op": "earlier",
                      "left": { "node": "AttributeReference", "attribute": "vondstdag", "subjectType": "Vondst",
                        "source": { "snippet": "zijn vondstdag" } },
                      "right": { "node": "Literal", "value": { "date": "%s" }, "type": { "node": "Date", "precision": "day" }, "source": { "snippet": "31-01-2024" } },
                      "source": { "snippet": "zijn vondstdag eerder is dan 31-01-2024" } },
                      "initialisation": false } ] } ],
                  "dayKindDefinitions": [], "objectCreations": [], "allocations": [], "consistencies": [],
                  "uniquenesses": [], "factCreations": [], "startPointDeterminations": [],
                  "ruleGroups": [] },
                  "diagnostics": [] }
                """.formatted(lexeem);
    }
}
