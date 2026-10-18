package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.ObjectMapper;

import dev.uchar.regelspraak.core.Literals;
import dev.uchar.regelspraak.loader.IrLoader;

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
 * <p><b>Since the fix round of the L4 landing the loader refuses every form outside the canonical lexeme</b>, in
 * WF-095's sentence at the literal's value and before either branch lowers anything; the refusal rows below pin
 * that. Until then each branch refused the rule while lowering it, naming the "datum-tijd-lexeem"; the branches
 * keep that refusal, and no document read off the wire reaches it any more. The reader rows keep pinning the
 * reader's own grammar.
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
class DateTimeLexemeWitnessTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    /** The canonical uchar wire lexeme and the RegelSpraak source snippet for the SAME instant. */
    private static final String ISO_NOON = "2024-01-31T12:00:00";
    private static final String SNIPPET_NOON = "31-01-2024 12:00:00";
    private static final String ISO_HALVE = "2024-01-31T12:00:00.500";
    private static final String SNIPPET_HALVE = "31-01-2024 12:00:00.500";

    /** V1 before the threshold, V2 exactly on it, V3 after — so a reader that lost the TIME OF DAY (the
     *  measured degradation to midnight) cannot produce this triple by accident. */
    private static final String SUB_DAY_CASE = caseDocument("2024-01-31T09:00:00", "2024-01-31T12:00:00", "2024-01-31T15:00:00");
    /** The same three positions one millisecond field down: the {@code .SSS} group is read, not dropped. */
    private static final String MS_CASE = caseDocument("2024-01-31T12:00:00.100", "2024-01-31T12:00:00.500", "2024-01-31T12:00:00.900");

    // ---------------- the exec-model branch (the gate-12 path) ----------------

    @Test void the_canonical_lexeme_plays_on_the_exec_model_branch() throws Exception {
        assertNull(play(model(ISO_NOON), SUB_DAY_CASE),
                "the canonical ISO datum-tijd lexeme must fire like any other literal");
    }

    @Test void the_millisecond_fields_count_along_on_the_exec_model_branch() throws Exception {
        assertNull(play(model(ISO_HALVE), MS_CASE), "the .SSS group is milliseconds, and it decides here");
    }

    // ---------------- the DRL-text branch (gate 13's second leg) ----------------

    @Test void the_canonical_lexeme_plays_on_the_drl_text_branch() throws Exception {
        assertEquals("PLAYED", drlStatus(model(ISO_NOON), SUB_DAY_CASE),
                "the DRL-text branch used to answer the canonical lexeme against midnight");
        assertEquals("PLAYED", drlStatus(model(ISO_HALVE), MS_CASE));
    }

    // ---------------- the source snippet is refused, on both branches ----------------

    /** Until 2026-09-23 both snippet lexemes PLAYED on both branches (the old pins
     *  {@code het_bronsnippet_speelt_identiek_op_de_exec_model_tak} and the snippet half of the DRL pin).
     *  Then each branch refused the rule and named the unreadable lexeme; now the loader refuses the literal,
     *  exactly as it does the zone-suffixed form below. */
    @Test void the_source_snippet_is_loudly_refused_on_both_branches() throws Exception {
        for (String[] pair : new String[][] {{SNIPPET_NOON, SUB_DAY_CASE}, {SNIPPET_HALVE, MS_CASE}})
            refusedAtLoad(model(pair[0]), pair[1], pair[0]);
    }

    // ---------------- neither branch may degrade an unreadable lexeme ----------------

    @Test void an_unreadable_date_time_lexeme_refuses_loudly_on_both_branches() throws Exception {
        // "2024-01-31T12:00" has no seconds, so no datum-tijd reader accepts it. The DATE reader would still
        // match its prefix — which is exactly the fall-through that made a wrong answer look played — so the
        // pin is that BOTH branches refuse the document instead of quietly comparing against 2024-01-31T00:00:00.
        refusedAtLoad(model("2024-01-31T12:00"), SUB_DAY_CASE, "2024-01-31T12:00");
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
    @Test void the_reader_refuses_every_form_outside_the_lexeme() {
        assertNull(Literals.parseDateTime("2024-01-31T12:00:00Z"), "no zone suffix — a Z is not dropped");
        assertNull(Literals.parseDateTime("2024-01-31T12:00:00+01:00"), "no offset — an offset is not dropped");
        assertNull(Literals.parseDateTime("2024-01-31 12:00:00"), "a space is not the canonical separator");
        assertNull(Literals.parseDateTime("2024-01-31 12:00:00.000"), "idem, with the fraction written");
        assertNull(Literals.parseDateTime("2024-1-31T09:00:00"), "fixed widths: a one-digit month is not the lexeme");
        assertNull(Literals.parseDateTime("2024-01-31T9:00:00"), "fixed widths: a one-digit hour is not the lexeme");
        assertNull(Literals.parseDateTime("2024-01-31T12:00:00.1"),
                "a one-digit fraction is the width the readers disagree on (IR-SPEC ch. 4.1) — refused, not guessed");
        assertNull(Literals.parseDateTime("2024-01-31T12:00:00.1234"),
                "a four-digit fraction was TRUNCATED to 123 ms before the fix — silently a different instant");
        assertNull(Literals.parseDateTime("2024-01-31T12:00"), "the seconds field is always written");
        assertNull(Literals.parseDateTime("rommel 2024-01-31T12:00:00 x"), "whole-string: the lexeme is the value, not a substring in it");
        assertNull(Literals.parseDateTime("2024-01-31"), "a date without a time of day is not a datum-tijd");
        assertNull(Literals.parseDateTime("2024-02-30T12:00:00"), "a real day of a real month (the calendar still decides)");
    }

    /** What the reader must keep READING: the canonical lexeme, with and without its fraction. */
    @Test void the_reader_reads_the_canonical_lexeme_and_the_millisecond_field() {
        assertEquals(LocalDateTime.of(2024, 1, 31, 12, 0, 0), Literals.parseDateTime(ISO_NOON));
        assertEquals(LocalDateTime.of(2024, 1, 31, 12, 0, 0, 500_000_000), Literals.parseDateTime(ISO_HALVE),
                ".500 is five hundred milliseconds, scaled to nanoOfSecond");
    }

    /** The four source-snippet shapes this reader used to READ (pinned here until 2026-09-23 as "the reader
     *  may not be stricter than the producer that feeds it"): the plain snippet, with its fraction, with the
     *  §13.2.19 {@code dd. } de-dato prefix, and with the token's variable-width day/month. No producer feeds
     *  them to this reader any more — the frontend reads its own source with a reader of its own and writes
     *  ISO into {@code waarde} — so each is now no value, which both branches report as an unreadable lexeme. */
    @Test void the_reader_refuses_the_source_snippet() {
        assertNull(Literals.parseDateTime(SNIPPET_NOON), "the day-first snippet is not the wire lexeme");
        assertNull(Literals.parseDateTime(SNIPPET_HALVE), "nor with its fraction");
        assertNull(Literals.parseDateTime("dd. 25-12-2024 10:30:00.500"), "nor with the de-dato prefix");
        assertNull(Literals.parseDateTime("1-3-2025 09:00:00"), "nor with one-digit day and month");
    }

    /** End-to-end, per branch, for a form that PLAYED before the fix: the zone-suffixed lexeme. It is not the
     *  canonical lexeme (uchar pins a local instant, no zone), yet the pre-fix reader dropped the {@code Z} and
     *  answered as if it were — the exec-model branch played it, the DRL branch played it. Both must now refuse,
     *  and the refusal must NAME the lexeme rather than blame a construct scope it never was. */
    @Test void the_zone_lexeme_is_loudly_refused_on_both_branches() throws Exception {
        refusedAtLoad(model("2024-01-31T12:00:00Z"), SUB_DAY_CASE, "2024-01-31T12:00:00Z");
    }

    // ---------------- helpers: replay ----------------

    /** Both branches refuse the document at load, at the comparison literal's value, in the verifier's WF-095
     *  sentence: the loader is shared, and each branch is asked on its own so neither can stop reaching it unseen. */
    private static void refusedAtLoad(String ir, String caseDocument, String lexeme) {
        for (boolean viaDrl : new boolean[] { false, true }) {
            IrLoader.DocumentError e = assertThrows(IrLoader.DocumentError.class,
                    () -> { if (viaDrl) drl(ir, caseDocument); else play(ir, caseDocument); },
                    (viaDrl ? "drl-tekst" : "exec-model") + " refuses '" + lexeme + "' at load");
            assertEquals("/rules/rules/0/versions/0/condition/right/value", e.site(), e.getMessage());
            assertTrue(e.getMessage().endsWith(": millisecond-precision date literal lexeme '" + lexeme + "' is not the"
                    + " canonical ISO form yyyy-mm-ddThh:mm:ss[.fff] (fixed field widths, 'T' never a space, the seconds"
                    + " field written, no zone or offset, a fraction of exactly three digits; the day-first source"
                    + " snippet belongs in `source` — ch. 4.1)"), e.getMessage());
        }
    }

    /** One authored model + case through the exec-model replay; the per-case failure line, null on a match. */
    private static String play(String ir, String caseJson) throws Exception {
        Path tmp = Files.createTempFile("datumtijd-lexeem-witness", ".json");
        try {
            Files.writeString(tmp, ir);
            return CaseRunner.playCase(tmp, JSON.readTree(caseJson));
        } finally {
            Files.deleteIfExists(tmp);
        }
    }

    /** The same model + case through the OTHER branch: DRL TEXT compiled to a KieBase. */
    private static CaseRunner.Result drl(String ir, String caseJson) throws Exception {
        Path irPath = Files.createTempFile("datumtijd-lexeem-witness-ir", ".json");
        Path casePath = Files.createTempFile("datumtijd-lexeem-witness-case", ".json");
        try {
            Files.writeString(irPath, ir);
            Files.writeString(casePath, caseJson);
            return CaseRunner.viaDrlText(irPath, List.of(casePath));
        } finally {
            Files.deleteIfExists(irPath);
            Files.deleteIfExists(casePath);
        }
    }

    private static String drlStatus(String ir, String caseJson) throws Exception {
        CaseRunner.Result r = drl(ir, caseJson);
        return "PLAYED".equals(r.status()) ? r.status() : r.status() + " " + r.reason() + r.caseFailures();
    }

    // ---------------- helpers: the wire shapes ----------------

    /** Three Vondsten around the threshold; V1 earlier (verjaard), V2 equal and V3 later (EERDER is strict). */
    private static String caseDocument(String v1, String v2, String v3) {
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
     *  attribute against a datum-tijd literal in {@code lexeme} — the only thing that varies here. */
    private static String model(String lexeme) {
        return """
                { "format": "uchar-ir-v1",
                  "model": { "objectTypes": [ { "node": "ObjectType", "name": "Vondst", "animate": false,
                    "plural": "Vondsten",
                    "attributes": [ { "node": "Attribute", "name": "vondstmoment",
                                      "datatype": { "node": "Date", "precision": "millisecond" } } ],
                    "characteristics": [ { "node": "Characteristic", "name": "verjaard", "kind": "adjectival" } ] } ],
                  "factTypes": [], "parameters": [], "domains": [], "unitSystems": [], "dimensions": [],
                  "timelines": [], "dayKinds": [] },
                  "rules": { "rules": [ { "node": "Rule", "name": "Kenmerktoekenning verjaard", "versions": [ {
                      "validity": { "node": "Validity" },
                      "target": { "node": "AttributeReference", "attribute": "verjaard", "subjectType": "Vondst",
                      "source": { "snippet": "Een Vondst is verjaard" } },
                      "value": { "node": "Literal", "value": { "boolean": true }, "type": { "node": "Boolean" }, "source": { "snippet": "waar" } },
                      "condition": { "node": "Comparison", "op": "earlier",
                      "left": { "node": "AttributeReference", "attribute": "vondstmoment", "subjectType": "Vondst",
                        "source": { "snippet": "zijn vondstmoment" } },
                      "right": { "node": "Literal", "value": { "dateTime": "%s" }, "type": { "node": "Date", "precision": "millisecond" }, "source": { "snippet": "31-01-2024 12:00:00" } },
                      "source": { "snippet": "zijn vondstmoment eerder is dan 31-01-2024 12:00:00" } },
                      "initialisation": false } ] } ],
                  "dayKindDefinitions": [], "objectCreations": [], "allocations": [], "consistencies": [],
                  "uniquenesses": [], "factCreations": [], "startPointDeterminations": [],
                  "ruleGroups": [] },
                  "diagnostics": [] }
                """.formatted(lexeme);
    }
}
