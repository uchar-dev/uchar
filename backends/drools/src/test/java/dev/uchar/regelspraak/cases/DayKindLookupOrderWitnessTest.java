package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;


import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The ORDER in which a §6.10/§6.11 duration unit is recognised, held to RUNTIME-SEMANTICS ch. 3.8: the
 * calendar table first, the day-kind registry only where the table has nothing — "a spelling outside the
 * table is a declared day kind", so outside is what the registry gets.
 *
 * <p><b>Why the witness lives here and not in the corpus.</b> The document that tells the two orders apart
 * has to register a CALENDAR spelling as a day-kind unit, and WF-017 (widened to the thirty spellings of
 * that table on 2026-09-20) refuses exactly that. So it cannot be a golden, a red case or a negative with a
 * case file: it is ill-formed on the wire by design, and the transpilers are the only place left that can be
 * asked what they would make of it. That is the shape of the flank this card closed — the wire stopped
 * carrying the contradiction, and this class is why an executor could not act on it either way.
 *
 * <p><b>Red first, measured 2026-09-20</b> on the jar from the commit before the reversal, with
 * {@code jutterswet-bergingsklok}'s unit spelled {@code wk} and {@code wk} registered beside it (the fixture
 * below) and the golden's own case1:
 *
 * <pre>
 *   ref                 8 MISMATCHes — it read the CALENDAR table: 2026-03-31 / 2026-02-17 / 5 / 5, …
 *   Drools exec-model   PLAYED 1     — it read the REGISTRY: the day-kind 2026-04-15 / 2026-02-01 / 3 / 3
 *   Drools DRL-text     PLAYED 1     — likewise
 * </pre>
 *
 * One document, two readings, and the day-kind reading was the one two of the four executors gave. After the
 * reversal all four give the calendar reading, which is what the rows below demand of both branches.
 */
class DayKindLookupOrderWitnessTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    /** cwd under surefire is the module dir (backends/drools); the corpus is two levels up. */
    private static final Path CORPUS = Path.of("..", "..", "corpus", "golden");

    /** The §6.10/§6.11 day-kind golden: a springtijdag walk over date + duration and both tijdsduur forms. */
    private static final String BERGINGSKLOK = "jutterswet-bergingsklok";

    /** The golden's own case1 inputs with the CALENDAR outcome pinned — `wk` is the week of the ch. 3.8 table,
     *  so the two steps are ± 3 weeks and the two counts are whole weeks. Hand-computed, and reproduced
     *  exactly by {@code ref} on this document (13 expectations, measured 2026-09-20). */
    private static final String CALENDAR_CASE = """
        { "format": "uchar-case-v0", "asOfDate": null,
          "input": { "instances": [
            { "label": "P1", "objectType": "Bergingsploeg",
              "attributes": { "meldingsdatum": { "date": "2026-03-10" }, "opleverdatum": { "date": "2026-04-20" },
                              "bergingsbestek": { "number": "3" } },
              "roles": {}, "characteristics": [] },
            { "label": "P2", "objectType": "Bergingsploeg",
              "attributes": { "meldingsdatum": { "date": "2026-03-15" }, "opleverdatum": { "date": "2026-03-15" },
                              "bergingsbestek": { "number": "1" } },
              "roles": {}, "characteristics": [] },
            { "label": "P3", "objectType": "Bergingsploeg",
              "attributes": { "meldingsdatum": { "date": "2026-03-20" }, "opleverdatum": { "date": "2026-03-01" },
                              "bergingsbestek": { "number": "0" } },
              "roles": {}, "characteristics": [] } ],
            "parameters": {} },
          "expected": [
            { "kind": "attributeValue", "label": "P1", "attribute": "uiterste bergingsdatum", "value": { "date": "2026-03-31" } },
            { "kind": "attributeValue", "label": "P1", "attribute": "vroegste meldingsdatum", "value": { "date": "2026-02-17" } },
            { "kind": "attributeValue", "label": "P1", "attribute": "bergingsspanne", "value": { "number": "5" } },
            { "kind": "attributeValue", "label": "P1", "attribute": "bergingsafstand", "value": { "number": "5" } },
            { "kind": "attributeValue", "label": "P2", "attribute": "uiterste bergingsdatum", "value": { "date": "2026-03-22" } },
            { "kind": "attributeValue", "label": "P2", "attribute": "vroegste meldingsdatum", "value": { "date": "2026-03-08" } },
            { "kind": "attributeValue", "label": "P2", "attribute": "bergingsspanne", "value": { "number": "0" } },
            { "kind": "attributeValue", "label": "P2", "attribute": "bergingsafstand", "value": { "number": "0" } },
            { "kind": "attributeValue", "label": "P3", "attribute": "uiterste bergingsdatum", "value": { "date": "2026-03-20" } },
            { "kind": "attributeValue", "label": "P3", "attribute": "vroegste meldingsdatum", "value": { "date": "2026-03-20" } },
            { "kind": "attributeValue", "label": "P3", "attribute": "bergingsspanne", "value": { "number": "-2" } },
            { "kind": "attributeValue", "label": "P3", "attribute": "bergingsafstand", "value": { "number": "2" } },
            { "kind": "consistent" } ] }
        """;

    // ---------------- the row: a registered calendar spelling stays calendar arithmetic ----------------

    @Test void a_geregistreerde_calendar_spelling_stays_calendar_arithmetic_in_both_branches() throws Exception {
        // THE ROW. `wk` is registered as a day-kind unit AND is the week of the calendar table. Both branches
        // must read the table, like the verifier, ref and the ASP backend, and reach it BEFORE the registry.
        // Revert either lookup and this row fails by name, in whichever branch was reverted: the values are
        // eight cells apart (the step and the count both move), not a token.
        BothBranches.green(temporary("dagsoort-opzoekvolgorde", splitDocument()), JSON.readTree(CALENDAR_CASE),
                "a registered calendar spelling is read through the table, not through the day-kind registry");
    }

    @Test void the_drl_text_shows_the_calendar_route() throws Exception {
        // The same answer one level below the replay, on the branch whose lowering is TEXT: the emitted rule
        // must call the calendar helper with the table's own unit, and must not deposit the day-kind walk.
        // A replay that happened to agree by arithmetic accident could not say which route produced it.
        String drl = drl(splitDocument());
        assertTrue(drl.contains("ComputeHelpers.dateStep(") && drl.contains("\"WEEKS\""),
                "date arithmetic on a `wk` duration lowers to the calendar helper: " + firstDateLine(drl));
        assertFalse(drl.contains("ComputeHelpers.dayKindStep("),
                "and never to the day-kind walk: " + firstDateLine(drl));
        assertFalse(drl.contains("rsTijdsduurDagsoort"),
                "the tijdsduur position takes the same route: " + firstDateLine(drl));
    }

    // ---------------- the same table, read case-insensitively ----------------

    @Test void a_capital_spelling_is_the_same_calendar_unit() throws Exception {
        // ch. 3.8 recognises a unit "by SPELLING, case-insensitive", and until 2026-09-20 this branch pair
        // did not: the plural reducer handed an unrecognised spelling straight back in its ORIGINAL case, so
        // only the nine `-en` plurals survived a capital. Measured on this document before the repair — the
        // golden's unit respelled `WK` and NOTHING registered, a verifier-clean document — `ref` replayed all
        // 13 expectations while both branches here refused the rule ("datum-rekenkunde (dagsoort/onbekende
        // tijdseenheid)"): a 3-against-2 split with no day kind involved at all. It is LOUD, never silent,
        // which is why it cost no corpus verdict; it is still two executors answering a question the other
        // three answer.
        BothBranches.green(temporary("dagsoort-hoofdletterspelling", capitalDocument()),
                JSON.readTree(CALENDAR_CASE),
                "a capitalised calendar spelling is the same unit — the table is case-insensitive");
    }

    // ---------------- the counter-measurement: a REAL day kind still walks the calendar ----------------

    @Test void a_spelling_outside_the_table_stays_the_day_kind_walk() throws Exception {
        // A reversal that simply stopped consulting the registry would pass the row above and break every
        // day-kind duration there is. The unmodified golden's unit is `springtijdagen`, which stands outside
        // the table, so ch. 3.8 makes it a declared day kind — and both branches must still walk it.
        Path ir = CORPUS.resolve(BERGINGSKLOK).resolve("ir.json");
        JsonNode caseDocument = JSON.readTree(Files.readString(CORPUS.resolve(BERGINGSKLOK).resolve("case1.json")));
        BothBranches.green(ir, caseDocument, "a spelling outside the table is still a declared day kind");
    }

    @Test void the_drl_text_shows_the_day_kind_route_for_a_spelling_outside_the_table() throws Exception {
        String drl = drl((ObjectNode) JSON.readTree(Files.readString(CORPUS.resolve(BERGINGSKLOK).resolve("ir.json"))));
        assertTrue(drl.contains("ComputeHelpers.dayKindStep("), "the day-kind step survives the reversal");
        assertTrue(drl.contains("rsTijdsduurDagsoort"), "and so does the day-kind count");
    }

    // ---------------- fixtures ----------------

    /** The golden with its day-kind unit RESPELLED as the calendar abbreviation `wk`, and `wk` registered as the
     *  day kind's plural — its unit spelling — in place of the spelling it replaced. That is the whole
     *  contradiction: the document says `wk` is a day-kind unit while the table says it is the week. */
    private static ObjectNode splitDocument() throws Exception {
        ObjectNode ir = (ObjectNode) JSON.readTree(Files.readString(CORPUS.resolve(BERGINGSKLOK).resolve("ir.json")));
        rename(ir, "wk");
        ((ObjectNode) IrPath.modelList(ir, "dayKinds").get(0)).put("plural", "wk");
        return ir;
    }

    /** The golden with its unit respelled `WK` and NOTHING registered beside it — no day kind is involved,
     *  so what this document asks is only whether the table is read case-insensitively. */
    private static ObjectNode capitalDocument() throws Exception {
        ObjectNode ir = (ObjectNode) JSON.readTree(Files.readString(CORPUS.resolve(BERGINGSKLOK).resolve("ir.json")));
        rename(ir, "WK");
        return ir;
    }

    /** The golden with its day-kind unit RESPELLED as the calendar abbreviation, in the case {@code spelling}
     *  gives — the declarations and the wire's repetitions of them alike, so no position is left disagreeing
     *  with the declaration it came from. */
    private static void rename(JsonNode x, String spelling) {
        if (x instanceof ObjectNode o) {
            if ("Unit".equals(o.path("node").asText()) && "springtijdagen".equals(o.path("abbreviation").asText())) {
                o.put("abbreviation", spelling);
                if ("springtijdagen".equals(o.path("canonical").asText())) o.put("canonical", spelling);
            }
            o.forEach(k -> rename(k, spelling));
        } else if (x.isArray()) {
            x.forEach(k -> rename(k, spelling));
        }
    }

    /** The text of the DRL-text branch, built through the backend's entry, so what this asserts is the text the branch
     *  actually compiles: the analysis rendered as DRL text. */
    private static String drl(ObjectNode ir) {
        return DroolsBackend.build(ir, DroolsBackend.Branch.DRL_TEXT).drl();
    }

    /** The first emitted line that mentions a date helper — the diagnostic a bare contains() cannot give. */
    private static String firstDateLine(String drl) {
        for (String r : drl.split("\n"))
            if (r.contains("ComputeHelpers.dateStep(") || r.contains("ComputeHelpers.dayKindStep(")
                    || r.contains("rsTijdsduur")) return r.trim();
        return "(no date helper in the emitted text at all)";
    }

    private static Path temporary(String name, ObjectNode ir) throws Exception {
        Path tmp = Files.createTempFile(name, ".json");
        tmp.toFile().deleteOnExit();
        Files.writeString(tmp, ir.toString());
        return tmp;
    }
}
