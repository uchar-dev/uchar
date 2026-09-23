package io.github.yhavinga.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.yhavinga.regelspraak.transpiler.kern.Tijdlijnen;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Temporal facet lookups and the §9.5 rule key under the shared name contract (card drools-temporele-naamnorm,
 * 2026-09-23). The verifier, ref and clingo treat two spellings of one declared name as the same name (ref's
 * {@code Checks.norm}: whitespace collapse over space and tab, ASCII lowercase). Both Drools branches asked "is this
 * member temporal?" by EXACT spelling at 72 sites, and read its granularity and period count the same way. A
 * reference spelled differently from its declaration was therefore scalar to Drools and temporal to everyone else.
 */
class TemporeleNaamnormWitnessTest {

    private static final ObjectMapper JSON = new ObjectMapper();
    // cwd under surefire is the module dir (backends/drools); the corpus is two levels up.
    private static final Path SEINJAAR = Path.of("..", "..", "corpus", "golden", "jutterswet-seinjaar", "ir.json");
    private static final Path KEURSPELLING = Path.of("..", "..", "corpus", "golden", "jutterswet-keurspelling", "ir.json");

    private static final String JAAR = "[{\"from\":\"2024-01-01\",\"to\":\"2025-01-01\"}]";
    private static final String HALF = "[{\"from\":\"2024-01-01\",\"to\":\"2024-07-01\"}]";

    // ---------------- 1. route R9: the qualified guard spelled differently from the anker's declaration -------------

    @Test void de_heelperiode_guard_met_afwijkende_spelling_speelt_op_de_anker() throws Exception {
        // Golden jutterswet-seinjaar: the guard "gedurende het gehele jaar Gehesen" names the anker's `gehesen`
        // (Kustvak) under the verifier's key; the doel Sein declares `Gehesen` itself. S1 hoisted all of 2024, S2 half.
        // Measured RED FIRST on the pre-fix jar (main f2638a49), for each V1 below, both branches:
        //     exec     : EXCLUDE transpiler SKIP: feitcreatie: vakbeseining door het kustvak — feitcreatie (voorwaarde
        //                niet naar anker of doel te lowereren — gehele-periodekwalificatie: alleen de anker is
        //                gebonden (WF-109): …)
        //     drl-tekst: EXCLUDE drl-transpiler MARKER: feitcreatie: vakbeseining door het kustvak — TODO voorwaarde
        //                niet naar anker of doel te lowereren — gehele-periodekwalificatie: …
        // ref and clingo: PASS on the golden's own cases (2 / 0 / 2). The anker attempt now finds the characteristic.
        BeideTakken.groen(SEINJAAR, geval("\"characteristicTimelines\": { \"gehesen\": " + JAAR + " }", "2"),
                "V1 heel 2024 gehesen: beide seinen");
        BeideTakken.groen(SEINJAAR, geval("\"characteristicTimelines\": { \"gehesen\": " + HALF + " }", "0"),
                "V1 half 2024 gehesen: geen sein");
        // The doel reading would answer 1 in both cases (S1 whole year, S2 half), so 1 is refused as a value.
        BeideTakken.rood(SEINJAAR, geval("\"characteristicTimelines\": { \"gehesen\": " + JAAR + " }", "1"),
                "vakseinaantal: verwacht 1, werkelijk 2", "de doellezing is niet wat er speelt");
    }

    // ---------------- 2. the period count reads by the same contract as the presence test -----------------------

    @Test void het_periodeaantal_leest_de_gedeclareerde_naam() {
        // Without this, a raster written "Waterstand" for a declared "waterstand" passed the (normed) temporality test
        // and then read as SINGLE-period here. Pre-fix: false on the second and third line.
        assertTrue(Tijdlijnen.meerPeriodiek(Map.of("waterstand", 2), "waterstand"), "zoals gedeclareerd");
        assertTrue(Tijdlijnen.meerPeriodiek(Map.of("waterstand", 2), "Waterstand"), "andere hoofdletter");
        assertTrue(Tijdlijnen.meerPeriodiek(Map.of("hoge waterstand", 2), "Hoge \t waterstand"), "witruimtevouw");
        // ref's norm folds A-Z only and splits on space and tab only; wider is a new divergence, not a fix.
        assertFalse(Tijdlijnen.meerPeriodiek(Map.of("coördinaat", 2), "COÖRDINAAT"), "geen niet-ASCII-vouw");
        assertFalse(Tijdlijnen.meerPeriodiek(Map.of("hoge waterstand", 2), "hoge\nwaterstand"), "een newline is een teken");
    }

    // ---------------- 3. the §9.5 rule key: norm plus version strip, as ref matches it ----------------------------

    @Test void de_regelnaam_van_een_inconsistentiepin_volgt_refs_sleutel() throws Exception {
        // ref (ref/lib/case.ml, the Inconsistent arm): norm (strip_version fact) = norm (strip_version pin). Measured
        // on this golden and case through ref's runner 2026-09-23: the three spellings below answer
        // "oracle: PASS (1 expectations)". Pre-fix both branches compared byte-exact and FAILed each with
        // "verwacht inconsistent (<pin> @ V1), niet gevonden onder …".
        for (String pin : new String[] { "Geen  Wrakhout ter keuring", "geen wrakhout ter keuring #2",
                                         "Geen wrakhout ter keuring  #17" })
            BeideTakken.groen(KEURSPELLING, inconsistent(pin), "pin '" + pin + "'");
        // Not a version suffix to strip_version, so a different rule. ref refuses these three at LOAD ("inconsistent
        // expectation references unknown rule"); this runner has no such check and reports the missing fact.
        for (String pin : new String[] { "geen wrakhout ter keuring#2", "geen wrakhout ter keuring #2a",
                                         "geen wrakhout ter keuring #" })
            BeideTakken.rood(KEURSPELLING, inconsistent(pin), "verwacht inconsistent (" + pin + " @ V1), niet gevonden onder",
                    "pin '" + pin + "'");
    }

    @Test void de_versiestrip_is_die_van_de_verifier() {
        assertEquals("X", CaseRunner.zonderVersie("X #2"));
        assertEquals("X ", CaseRunner.zonderVersie("X  #17"), "one space comes off, the rest is norm's");
        assertEquals("X#2", CaseRunner.zonderVersie("X#2"), "no space before #");
        assertEquals("X #", CaseRunner.zonderVersie("X #"), "no digit after #");
        assertEquals("X #2a", CaseRunner.zonderVersie("X #2a"));
        assertEquals("X\t#2", CaseRunner.zonderVersie("X\t#2"), "the space must be a space");
        assertEquals(" #2", CaseRunner.zonderVersie(" #2"), "# at index 1: i >= 2 fails");
        assertEquals("a #1", CaseRunner.zonderVersie("a #1 #2"), "only the last suffix");
        assertTrue(CaseRunner.zelfdeRegel("Regel A #3", "regel   a"));
        assertFalse(CaseRunner.zelfdeRegel("regel a #3", "regel b"));
    }

    // ---------------- fixtures ----------------

    /** The seinjaar world with V1's own timeline block and the expected {@code vakseinaantal}. */
    private static JsonNode geval(String vakTijdlijn, String verwacht) throws Exception {
        return JSON.readTree("""
                { "format": "uchar-case-v0", "asOfDate": "2024-06-15",
                  "input": { "instances": [
                      { "label": "V1", "objectType": "Kustvak", "attributes": {}, "characteristics": [], %s,
                        "roles": { "posten": ["P1"] } },
                      { "label": "P1", "objectType": "Post", "attributes": {}, "characteristics": ["gehesen"],
                        "roles": { "kustvak": ["V1"], "seinen": ["S1", "S2"] } },
                      { "label": "S1", "objectType": "Sein", "attributes": {}, "characteristics": [], "roles": {},
                        "characteristicTimelines": { "Gehesen": %s } },
                      { "label": "S2", "objectType": "Sein", "attributes": {}, "characteristics": [], "roles": {},
                        "characteristicTimelines": { "Gehesen": %s } } ],
                    "parameters": {} },
                  "expected": [
                      { "kind": "attributeValue", "label": "V1", "attribute": "vakseinaantal",
                        "value": { "number": "%s" } },
                      { "kind": "consistent" } ] }
                """.formatted(vakTijdlijn, JAAR, HALF, verwacht));
    }

    private static JsonNode inconsistent(String rule) throws Exception {
        return JSON.readTree("""
                {"format": "uchar-case-v0", "asOfDate": null,
                 "input": {"instances": [
                   {"label": "V1", "objectType": "Vondst", "roles": {}, "characteristics": [],
                    "attributes": {"staat": {"enumeration": "wrakhout"}, "herkeuring": {"enumeration": "wrakhout"}}},
                   {"label": "V2", "objectType": "Vondst", "roles": {}, "characteristics": [],
                    "attributes": {"staat": {"enumeration": "gaaf"}, "herkeuring": {"enumeration": "beschadigd"}}}],
                  "parameters": {}},
                 "expected": [{"kind": "inconsistent", "rule": %s, "label": "V1"}]}
                """.formatted(JSON.writeValueAsString(rule)));
    }
}
