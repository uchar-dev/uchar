package io.github.yhavinga.regelspraak.cases;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The datum-tijd literal LEXEME (IR-SPEC ch. 4.1), pinned on BOTH transpiler branches: the canonical ISO
 * form {@code yyyy-mm-ddThh:mm:ss[.SSS]} and the RegelSpraak source snippet {@code dd-m-yyyy hh:mm:ss[.SSS]}
 * denote the same instant, so they must produce the same fired outcome. The IR is a WIRE, not a dump of one
 * frontend: a conforming producer writing the canonical lexeme may not be answered differently from one
 * writing the snippet.
 *
 * <p><b>Why this lives in the suite and not in the corpus.</b> No golden can carry a datum-tijd literal
 * today, measured 2026-09-05: {@code ref}'s {@code literal_value} has no {@code Datum_tijd} arm at all
 * ("EVALUATION FAULT: literal of this datatype not implemented", in EITHER lexeme) and the ASP backend
 * faults before it ("unscalable datatype in the ASP translation" — its {@code dt_term} carries an
 * own-subject read and datetime &plusmn; duration, no literal). Gates 5 and 6 would therefore go red on such
 * a golden, so gate 12 cannot be the fence here; this witness is.
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

    @Test void het_bronsnippet_speelt_identiek_op_de_exec_model_tak() throws Exception {
        assertNull(speel(model(SNIPPET_MIDDAG), SUBDAG_CASE),
                "the snippet lexeme denotes the same instant and must fire identically");
    }

    @Test void de_millisecondevelden_tellen_mee_op_de_exec_model_tak() throws Exception {
        assertNull(speel(model(ISO_HALVE), MS_CASE), "the .SSS group is milliseconds, and it decides here");
        assertNull(speel(model(SNIPPET_HALVE), MS_CASE), "the snippet's .SSS group reads the same way");
    }

    // ---------------- the DRL-text branch (gate 13's second leg) ----------------

    @Test void beide_lexemen_spelen_identiek_op_de_drl_teksttak() throws Exception {
        assertEquals("PLAYED", drlStatus(model(ISO_MIDDAG), SUBDAG_CASE),
                "the DRL-text branch used to answer the canonical lexeme against midnight");
        assertEquals("PLAYED", drlStatus(model(SNIPPET_MIDDAG), SUBDAG_CASE));
        assertEquals("PLAYED", drlStatus(model(ISO_HALVE), MS_CASE));
        assertEquals("PLAYED", drlStatus(model(SNIPPET_HALVE), MS_CASE));
    }

    // ---------------- neither branch may degrade an unreadable lexeme ----------------

    @Test void een_onleesbaar_datumtijd_lexeem_weigert_luid_op_beide_takken() throws Exception {
        // "2024-01-31T12:00" has no seconds, so no datum-tijd reader accepts it. The DATE reader would still
        // match its prefix — which is exactly the fall-through that made a wrong answer look played — so the
        // pin is that BOTH branches refuse the rule instead of quietly comparing against 2024-01-31T00:00:00.
        String kapot = model("2024-01-31T12:00");
        assertThrows(CaseRunner.Excluded.class, () -> speel(kapot, SUBDAG_CASE),
                "the exec-model branch refuses the rule");
        CaseRunner.Result r = drl(kapot, SUBDAG_CASE);
        assertEquals("EXCLUDE", r.status(), "the DRL-text branch refuses instead of degrading to the bare date");
        assertTrue(r.reason().contains("Kenmerktoekenning verjaard"),
                "the refusal names the construct it dropped: " + r.reason());
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
                { "formaat": "regelspraak-ir-v0",
                  "objecttypen": { "Vondst": { "node": "Objecttype", "naam": "Vondst",
                    "attributen": { "vondstmoment": { "node": "DatumTijd" } },
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
                      "links": { "node": "AttribuutRef", "attribuut": "vondstmoment", "objecttype": "Vondst",
                        "onderwerpBron": "zijn", "navigatieRol": null, "type": { "node": "DatumTijd" },
                        "bron": "zijn vondstmoment", "set": false, "navigatiePad": [] },
                      "rechts": { "node": "Literal", "waarde": "%s", "type": { "node": "DatumTijd" },
                        "bron": "31-01-2024 12:00:00" },
                      "periode": null, "bron": "zijn vondstmoment eerder is dan 31-01-2024 12:00:00" },
                    "geldigheid": { "node": "Geldigheid", "vanaf": null, "tot": null } } ],
                  "resolvedDagsoorten": [], "objectCreaties": [], "verdelingen": [], "consistenties": [],
                  "uniciteiten": [], "uniciteitUnies": [], "feitCreaties": [], "resolvedStartpunten": [],
                  "regelGroepen": [], "diagnostics": [] }
                """.formatted(lexeem);
    }
}
