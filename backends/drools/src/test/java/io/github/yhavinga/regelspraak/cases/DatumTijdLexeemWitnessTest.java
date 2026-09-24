package io.github.yhavinga.regelspraak.cases;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.yhavinga.regelspraak.kern.Literalen;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The datum-tijd literal LEXEME (IR-SPEC ch. 4.1), pinned on BOTH transpiler branches: the canonical ISO
 * form {@code yyyy-mm-ddThh:mm:ss[.SSS]} is the one lexeme, and it plays; the RegelSpraak source snippet
 * {@code dd-m-yyyy hh:mm:ss[.SSS]} is refused by name, like every other form.
 *
 * <p><b>Narrowed 2026-09-23 (card {@code datumtijd-lezers-versmalling}).</b> Until then this witness pinned
 * the snippet as a SECOND lexeme that had to fire identically to the canonical one, because the RegelSpraak
 * emitter wrote the snippet into {@code waarde}. Since card {@code p5-datumtijd-lexeem} the emitter writes
 * ISO there and WF-095 admits ISO alone, so a snippet in {@code waarde} has no producer and no verifier-clean
 * document. The pins that said "the snippet plays" now say "the snippet is refused on both branches, and the
 * refusal names the lexeme"; the canonical pins are unchanged.
 *
 * <p><b>Why this lived in the suite and not in the corpus — and what changed.</b> Measured 2026-09-05, no
 * golden could carry a datum-tijd literal at all: {@code ref}'s {@code literal_value} had no
 * {@code Datum_tijd} arm ("EVALUATION FAULT: literal of this datatype not implemented", in EITHER lexeme)
 * and the ASP backend faulted before it ("unscalable datatype in the ASP translation" — its
 * {@code dt_term} carried an own-subject read and datetime &plusmn; duration, no literal). Gates 5 and 6
 * would have gone red on such a golden, so gate 12 could not be the fence and this witness was.
 * <b>That is history since 2026-09-06:</b> {@code ref} carries the literal and the ASP translation carries
 * both the datum-tijd ordering and the literal, and {@code corpus/golden/jutterswet-meldstip} IS the golden
 * this paragraph said could not exist — replayed by gates 5, 6 and 12 against hand-computed values. What
 * this witness keeps is what the golden still cannot say: the pins below hold the canonical lexeme on
 * BOTH Drools branches at sub-day and millisecond resolution, and hold the refusals a corpus golden may not
 * contain — the snippet among them.
 *
 * <p><b>Red before the fix</b> (measured through the backend CLI on these exact models):
 * <ul>
 *   <li>exec-model branch, canonical lexeme &rarr; {@code EXCLUDE transpiler SKIP: regel: Kenmerktoekenning
 *       verjaard — regel (voorwaarde buiten Phase-B scope)}: the shared reader returned null, the whole rule
 *       left the KieBase, and the SKIP reason named a scope it never was;</li>
 *   <li>DRL-text branch, canonical lexeme &rarr; {@code FAIL: kenmerk verjaard: verwacht aanwezig, werkelijk
 *       afwezig}: worse than a skip. That branch fell through to the DATE reader, which matched the ISO
 *       lexeme's date PREFIX, and compared the datum-tijd against MIDNIGHT — a silent wrong answer wherever
 *       the time of day decides;</li>
 *   <li>both branches, snippet lexeme &rarr; {@code PLAYED}. Only the lexeme differed.</li>
 * </ul>
 *
 * <p><b>Where the pinned values come from.</b> They are hand-computed (EERDER is strict {@code <} on the
 * instant) and then confirmed by {@code ref/} on the LITERAL-FREE TWIN of each model — the same comparison
 * with the threshold instant supplied as a second datum-tijd attribute through the case wire, which ref does
 * carry: {@code oracle: PASS (3 expectations)} on both, and flipping one pin makes ref red. So ref pins the
 * comparison's semantics; what ref cannot carry is only the literal itself.
 */
class DatumTijdLexeemWitnessTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    /** The canonical uchar wire lexeme and the RegelSpraak source snippet for the SAME instant. */
    private static final String ISO_MIDDAG = "2024-01-31T12:00:00";
    private static final String SNIPPET_MIDDAG = "31-01-2024 12:00:00";
    private static final String ISO_HALVE = "2024-01-31T12:00:00.500";
    private static final String SNIPPET_HALVE = "31-01-2024 12:00:00.500";

    /** V1 before the threshold, V2 exactly on it, V3 after — so a reader that lost the TIME OF DAY (the
     *  measured degradation to midnight) cannot produce this triple by accident. */
    private static final String SUBDAG_CASE = zaak("2024-01-31T09:00:00", "2024-01-31T12:00:00", "2024-01-31T15:00:00");
    /** The same three positions one millisecond field down: the {@code .SSS} group is read, not dropped. */
    private static final String MS_CASE = zaak("2024-01-31T12:00:00.100", "2024-01-31T12:00:00.500", "2024-01-31T12:00:00.900");

    // ---------------- the exec-model branch (the gate-12 path) ----------------

    @Test void het_canonieke_lexeem_speelt_op_de_exec_model_tak() throws Exception {
        assertNull(speel(model(ISO_MIDDAG), SUBDAG_CASE),
                "the canonical ISO datum-tijd lexeme must fire like any other literal");
    }

    @Test void de_millisecondevelden_tellen_mee_op_de_exec_model_tak() throws Exception {
        assertNull(speel(model(ISO_HALVE), MS_CASE), "the .SSS group is milliseconds, and it decides here");
    }

    // ---------------- the DRL-text branch (gate 13's second leg) ----------------

    @Test void het_canonieke_lexeem_speelt_op_de_drl_teksttak() throws Exception {
        assertEquals("PLAYED", drlStatus(model(ISO_MIDDAG), SUBDAG_CASE),
                "the DRL-text branch used to answer the canonical lexeme against midnight");
        assertEquals("PLAYED", drlStatus(model(ISO_HALVE), MS_CASE));
    }

    // ---------------- the source snippet is refused, on both branches ----------------

    /** Until 2026-09-23 both snippet lexemes PLAYED on both branches (the old pins
     *  {@code het_bronsnippet_speelt_identiek_op_de_exec_model_tak} and the snippet half of the DRL pin).
     *  Now each branch refuses the rule and names the unreadable lexeme, exactly as it does for the
     *  zone-suffixed form below. */
    @Test void het_bronsnippet_wordt_op_beide_takken_luid_geweigerd() throws Exception {
        for (String[] paar : new String[][] {{SNIPPET_MIDDAG, SUBDAG_CASE}, {SNIPPET_HALVE, MS_CASE}}) {
            String snippet = model(paar[0]);
            CaseRunner.Excluded ex = assertThrows(CaseRunner.Excluded.class, () -> speel(snippet, paar[1]),
                    "the exec-model branch refuses the rule for snippet " + paar[0]);
            assertTrue(ex.getMessage().contains("datum-tijd-lexeem"),
                    "and names the unreadable lexeme: " + ex.getMessage());
            CaseRunner.Result r = drl(snippet, paar[1]);
            assertEquals("EXCLUDE", r.status(), "the DRL-text branch refuses snippet " + paar[0]);
            assertTrue(r.reason().contains("datum-tijd-lexeem"),
                    "in the same words as the other branch: " + r.reason());
        }
    }

    // ---------------- neither branch may degrade an unreadable lexeme ----------------

    @Test void een_onleesbaar_datumtijd_lexeem_weigert_luid_op_beide_takken() throws Exception {
        // "2024-01-31T12:00" has no seconds, so no datum-tijd reader accepts it. The DATE reader would still
        // match its prefix — which is exactly the fall-through that made a wrong answer look played — so the
        // pin is that BOTH branches refuse the rule instead of quietly comparing against 2024-01-31T00:00:00.
        String kapot = model("2024-01-31T12:00");
        CaseRunner.Excluded ex = assertThrows(CaseRunner.Excluded.class, () -> speel(kapot, SUBDAG_CASE),
                "the exec-model branch refuses the rule");
        assertTrue(ex.getMessage().contains("datum-tijd-lexeem"),
                "and says WHY, not 'outside Phase-B scope': " + ex.getMessage());
        CaseRunner.Result r = drl(kapot, SUBDAG_CASE);
        assertEquals("EXCLUDE", r.status(), "the DRL-text branch refuses instead of degrading to the bare date");
        assertTrue(r.reason().contains("Kenmerktoekenning verjaard"),
                "the refusal names the construct it dropped: " + r.reason());
        assertTrue(r.reason().contains("datum-tijd-lexeem"),
                "and names the cause in the same words as the other branch: " + r.reason());
    }

    // ---------------- the lexeme FENCE itself (unit pins on the shared reader) ----------------

    /**
     * What the reader must REFUSE. Every form below was silently ACCEPTED before the anchoring fix
     * (measured 2026-09-06 against the pre-fix reader): the pattern matched with {@code find()}, so a
     * zone suffix, an offset, surrounding text and any surplus fraction digit were simply not looked at,
     * and one-digit fields passed because the widths were {@code \d{1,2}}. Each of these is a document a
     * conforming producer might write believing it was understood; a reader that answers one of them is a
     * reader that guesses. Refusing is never inert here — both branches drop the construct LOUDLY (the
     * end-to-end pin below), which is the whole point: the alternative is a wrong instant, silently.
     * The source snippet has its own pin below.
     */
    @Test void de_lezer_weigert_elke_vorm_buiten_het_lexeem() {
        assertNull(Literalen.parseDatumTijd("2024-01-31T12:00:00Z"), "no zone suffix — a Z is not dropped");
        assertNull(Literalen.parseDatumTijd("2024-01-31T12:00:00+01:00"), "no offset — an offset is not dropped");
        assertNull(Literalen.parseDatumTijd("2024-01-31 12:00:00"), "a space is not the canonical separator");
        assertNull(Literalen.parseDatumTijd("2024-01-31 12:00:00.000"), "idem, with the fraction written");
        assertNull(Literalen.parseDatumTijd("2024-1-31T09:00:00"), "fixed widths: a one-digit month is not the lexeme");
        assertNull(Literalen.parseDatumTijd("2024-01-31T9:00:00"), "fixed widths: a one-digit hour is not the lexeme");
        assertNull(Literalen.parseDatumTijd("2024-01-31T12:00:00.1"),
                "a one-digit fraction is the width the readers disagree on (IR-SPEC ch. 4.1) — refused, not guessed");
        assertNull(Literalen.parseDatumTijd("2024-01-31T12:00:00.1234"),
                "a four-digit fraction was TRUNCATED to 123 ms before the fix — silently a different instant");
        assertNull(Literalen.parseDatumTijd("2024-01-31T12:00"), "the seconds field is always written");
        assertNull(Literalen.parseDatumTijd("rommel 2024-01-31T12:00:00 x"), "whole-string: the lexeme is the value, not a substring in it");
        assertNull(Literalen.parseDatumTijd("2024-01-31"), "a date without a time of day is not a datum-tijd");
        assertNull(Literalen.parseDatumTijd("2024-02-30T12:00:00"), "a real day of a real month (the calendar still decides)");
    }

    /** What the reader must keep READING: the canonical lexeme, with and without its fraction. */
    @Test void de_lezer_leest_het_canonieke_lexeem_en_het_millisecondeveld() {
        assertEquals(LocalDateTime.of(2024, 1, 31, 12, 0, 0), Literalen.parseDatumTijd(ISO_MIDDAG));
        assertEquals(LocalDateTime.of(2024, 1, 31, 12, 0, 0, 500_000_000), Literalen.parseDatumTijd(ISO_HALVE),
                ".500 is five hundred milliseconds, scaled to nanoOfSecond");
    }

    /** The four source-snippet shapes this reader used to READ (pinned here until 2026-09-23 as "the reader
     *  may not be stricter than the producer that feeds it"): the plain snippet, with its fraction, with the
     *  §13.2.19 {@code dd. } de-dato prefix, and with the token's variable-width day/month. No producer feeds
     *  them to this reader any more — the frontend reads its own source with a reader of its own and writes
     *  ISO into {@code waarde} — so each is now no value, which both branches report as an unreadable lexeme. */
    @Test void de_lezer_weigert_het_bronsnippet() {
        assertNull(Literalen.parseDatumTijd(SNIPPET_MIDDAG), "the day-first snippet is not the wire lexeme");
        assertNull(Literalen.parseDatumTijd(SNIPPET_HALVE), "nor with its fraction");
        assertNull(Literalen.parseDatumTijd("dd. 25-12-2024 10:30:00.500"), "nor with the de-dato prefix");
        assertNull(Literalen.parseDatumTijd("1-3-2025 09:00:00"), "nor with one-digit day and month");
    }

    /** End-to-end, per branch, for a form that PLAYED before the fix: the zone-suffixed lexeme. It is not the
     *  canonical lexeme (uchar pins a local instant, no zone), yet the pre-fix reader dropped the {@code Z} and
     *  answered as if it were — the exec-model branch played it, the DRL branch played it. Both must now refuse,
     *  and the refusal must NAME the lexeme rather than blame a construct scope it never was. */
    @Test void het_zone_lexeem_wordt_op_beide_takken_luid_geweigerd() throws Exception {
        String zone = model("2024-01-31T12:00:00Z");
        CaseRunner.Excluded ex = assertThrows(CaseRunner.Excluded.class, () -> speel(zone, SUBDAG_CASE),
                "the exec-model branch must refuse the rule, not play a zone-stripped instant");
        assertTrue(ex.getMessage().contains("datum-tijd-lexeem"),
                "the exec-model refusal names the unreadable lexeme: " + ex.getMessage());
        CaseRunner.Result r = drl(zone, SUBDAG_CASE);
        assertEquals("EXCLUDE", r.status(), "the DRL-text branch must refuse it too");
        assertTrue(r.reason().contains("datum-tijd-lexeem"),
                "the DRL refusal names the unreadable lexeme: " + r.reason());
    }

    // ---------------- helpers: replay ----------------

    /** One authored model + case through the exec-model replay; the per-case failure line, null on a match. */
    private static String speel(String ir, String caseJson) throws Exception {
        Path tmp = Files.createTempFile("datumtijd-lexeem-witness", ".json");
        try {
            Files.writeString(tmp, ir);
            return CaseRunner.speelCase(tmp, JSON.readTree(caseJson));
        } finally {
            Files.deleteIfExists(tmp);
        }
    }

    /** The same model + case through the OTHER branch: DRL TEXT compiled to a KieBase. */
    private static CaseRunner.Result drl(String ir, String caseJson) throws Exception {
        Path irPad = Files.createTempFile("datumtijd-lexeem-witness-ir", ".json");
        Path zaakPad = Files.createTempFile("datumtijd-lexeem-witness-case", ".json");
        try {
            Files.writeString(irPad, ir);
            Files.writeString(zaakPad, caseJson);
            return CaseRunner.viaDrlTekst(irPad, List.of(zaakPad));
        } finally {
            Files.deleteIfExists(irPad);
            Files.deleteIfExists(zaakPad);
        }
    }

    private static String drlStatus(String ir, String caseJson) throws Exception {
        CaseRunner.Result r = drl(ir, caseJson);
        return "PLAYED".equals(r.status()) ? r.status() : r.status() + " " + r.reason() + r.caseFailures();
    }

    // ---------------- helpers: the wire shapes ----------------

    /** Three Vondsten around the threshold; V1 earlier (verjaard), V2 equal and V3 later (EERDER is strict). */
    private static String zaak(String v1, String v2, String v3) {
        return """
                { "format": "uchar-case-v0", "asOfDate": "2026-03-15",
                  "input": { "instances": [
                      { "label": "V1", "objectType": "Vondst",
                        "attributes": { "vondstmoment": { "dateTime": "%s" } }, "roles": {}, "characteristics": [] },
                      { "label": "V2", "objectType": "Vondst",
                        "attributes": { "vondstmoment": { "dateTime": "%s" } }, "roles": {}, "characteristics": [] },
                      { "label": "V3", "objectType": "Vondst",
                        "attributes": { "vondstmoment": { "dateTime": "%s" } }, "roles": {}, "characteristics": [] } ],
                    "parameters": {} },
                  "expected": [
                    { "kind": "characteristicPresent", "label": "V1", "characteristic": "verjaard" },
                    { "kind": "characteristicAbsent",  "label": "V2", "characteristic": "verjaard" },
                    { "kind": "characteristicAbsent",  "label": "V3", "characteristic": "verjaard" } ] }
                """.formatted(v1, v2, v3);
    }

    /** One objecttype with a datum-tijd attribute, one kenmerktoekenning whose condition compares that
     *  attribute against a datum-tijd literal in {@code lexeem} — the only thing that varies here. */
    private static String model(String lexeem) {
        return """
                { "format": "uchar-ir-v1",
                  "model": { "objectTypes": [ { "node": "ObjectType", "name": "Vondst", "animate": false,
                    "plural": "Vondsten",
                    "attributes": [ { "node": "Attribute", "name": "vondstmoment",
                                      "datatype": { "node": "Date", "precision": "millisecond" } } ],
                    "characteristics": [ { "node": "Characteristic", "name": "verjaard", "kind": "BIJVOEGLIJK" } ] } ],
                  "factTypes": [], "parameters": [], "domains": [], "unitSystems": [], "dimensions": [],
                  "timelines": [], "dayKinds": [] },
                  "rules": { "resolvedRegels": [ { "node": "Rule", "name": "Kenmerktoekenning verjaard", "versions": [ {
                      "validity": { "node": "Geldigheid", "vanaf": null, "tot": null },
                      "target": { "node": "AttribuutRef", "attribuut": "verjaard", "objecttype": "Vondst",
                      "bron": "Een Vondst is verjaard" },
                      "value": { "node": "Literal", "value": { "boolean": true }, "type": { "node": "Booleaan" }, "bron": "waar" },
                      "condition": { "node": "Vergelijking", "op": "EERDER",
                      "links": { "node": "AttribuutRef", "attribuut": "vondstmoment", "objecttype": "Vondst",
                        "bron": "zijn vondstmoment" },
                      "rechts": { "node": "Literal", "value": { "dateTime": "%s" }, "type": { "node": "Date", "precision": "millisecond" }, "bron": "31-01-2024 12:00:00" },
                      "periode": null, "bron": "zijn vondstmoment eerder is dan 31-01-2024 12:00:00" },
                      "initialisation": false } ] } ],
                  "resolvedDagsoorten": [], "objectCreaties": [], "verdelingen": [], "consistenties": [],
                  "uniciteiten": [], "feitCreaties": [], "resolvedStartpunten": [],
                  "regelGroepen": [] },
                  "diagnostics": [] }
                """.formatted(lexeem);
    }
}
