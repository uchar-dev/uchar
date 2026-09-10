package io.github.yhavinga.regelspraak.cases;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.yhavinga.regelspraak.runtime.RegelSpraakRuntime;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The REFUSING half of this backend's {@code date} case reader, on all THREE positions a date reaches it:
 * an attribute/parameter/segment/read-back VALUE ({@code waardeNaarArg}), the case's {@code asOfDate}, and a
 * timeline segment's {@code from}/{@code to}. The sister case {@link CaseWireDatumTijdWitnessTest} does the
 * same for {@code dateTime}; this one exists because the {@code date} side held a bare
 * {@code LocalDate.parse} on each of those three positions until 2026-09-10, and that parse is LAXER than
 * the wire it reads.
 *
 * <p><b>What the laxness cost, measured 2026-09-10.</b> {@code LocalDate.parse} refuses nearly everything
 * the schema pattern refuses — a narrow field, a zone suffix, the basic format, surrounding text, a date
 * that is not on the calendar — but it accepts the two ISO forms with a SIGNED, extended year:
 * {@code +012026-03-15} is year 12026 and {@code -0500-01-01} is year −500. The interpreter refuses both
 * ("malformed date lexeme … (expected ISO yyyy-mm-dd)"). Run through this backend, {@code +012026-03-15}
 * as the {@code asOfDate} of {@code jutterswet-penningjaar} and {@code -0500-01-01} as a segment boundary
 * of {@code jutterswet-bakenmeester} each replayed as a PLAYED golden while the interpreter faulted on the
 * same two documents — a divergence the differential gate cannot see, because a case that reaches the wrong
 * evaluation date or the wrong bucket grid may still satisfy every pinned expectation.
 *
 * <p><b>The lexeme.</b> {@code uchar-case-v0} writes a {@code date} as {@code yyyy-mm-dd}, matched WHOLE:
 * four-two-two digit fields, digits only, no sign and no extended year — the schema's own pattern
 * ({@code ^[0-9]{4}-[0-9]{2}-[0-9]{2}$}) — and then a real calendar day. Anything else is refused HARD (an
 * {@link IllegalArgumentException}), never as a {@link CaseRunner.Excluded}: an exclusion would file a
 * malformed case under "not carried by the v0 backend" and leave the differential gate green over a case
 * nobody read.
 *
 * <p><b>The clamp follows the INTERPRETER, and the schema pattern is the same fence one field narrower.</b>
 * Every form below was run through {@code ref} first (as the {@code asOfDate} of
 * {@code jutterswet-penningjaar}) and the clamp reproduces its verdict exactly — not wider, not narrower:
 *
 * <pre>
 *   2026-03-15  0000-01-01  9999-12-31  2024-02-29     ref reads it        — accepted here
 *   +012026-03-15  -0500-01-01  10000-01-01           ref: malformed      — refused here, FORM branch
 *   2026-3-15  2026-03-5  2026-03-15Z  20260315
 *   " 2026-03-15"  2026-03-15T12:00:00                ref: malformed      — refused here, FORM branch
 *   2023-02-29  2026-02-30                            ref: day out of range   — CALENDAR branch
 *   2026-13-01                                        ref: month out of range — CALENDAR branch
 * </pre>
 *
 * <p>Read that table by FORM CLASS, not lexeme for lexeme: the ref runs above used {@code asOfDate} values
 * on 15 March, while the cases below mostly work on {@code jutterswet-jutdatum} and write 21 June, so 10 of
 * the 17 forms are asserted here on the identical lexeme and the other 7 on the same form one day apart
 * ({@code 2026-6-21} for {@code 2026-3-15}, {@code 20260621} for {@code 20260315}, and so on). The clamp
 * decides on the form and never reads the day digits before the pattern, so the classes are what carry —
 * but the agreement is lexeme-identical for 10 rows, not for all 17.
 *
 * The two year edges the pattern DOES admit ({@code 0000-01-01}, {@code 9999-12-31}) are pinned as read, so
 * a clamp narrowed from below — a year range, a "no year zero" rule — fails here instead of passing
 * unnoticed: no corpus case sits near either edge.
 *
 * <p><b>Why the refusal must say WHICH half failed.</b> The reader answers in two branches, and the cases
 * below assert the branch as well as the refusal. A value whose FIELDS are wrong is told the form it should
 * have had; a value whose fields are right but whose calendar is not is told the calendar, and is NOT told
 * the field widths, which are correct and would send its producer looking in the wrong place.
 *
 * <p>The models are corpus goldens read off the wire; the cases are authored here, and no corpus
 * expectation is read or touched. The reads go through {@link CaseRunner#invoerBeans}, the input layer
 * before any rule fires, so what the reader made of a value is the assertion itself.
 */
class CaseWireDatumWitnessTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    /** cwd under surefire is the module dir (backends/drools); the corpus is two levels up. */
    private static final Path CORPUS = Path.of("..", "..", "corpus", "golden");

    /** The form branch of the refusal names the lexeme it expected. */
    private static final String LEXEEM_VORM = "yyyy-mm-dd";
    /** The calendar branch names the calendar instead — the fields were right. */
    private static final String KALENDER = "the calendar is not";

    // ---------------- the value route: the field classes the pattern refuses ----------------

    @Test void een_getekend_uitgebreid_jaar_wordt_geweigerd() throws Exception {
        // The measured leak. Accepted before the clamp as year 12026 — a value four figures away from what
        // its producer wrote, read without a word while the interpreter refused the same document.
        weigertOpVorm("+012026-06-21", "a signed extended year is not the case lexeme");
    }

    @Test void een_negatief_jaar_wordt_geweigerd() throws Exception {
        // The other half of the same leak: a leading '-' makes the year negative, and every field after it
        // shifts. This is also the form that pins WHOLE matching from the other side — a reader searching for
        // the pattern INSIDE the value finds "0500-01-01" here and answers about a date nobody wrote.
        weigertOpVorm("-0500-01-01", "a negative year is not the case lexeme");
    }

    @Test void een_jaarveld_van_vijf_cijfers_wordt_geweigerd() throws Exception {
        // The year field is exactly four wide, not "at least four": widening {4} to {4,} leaves every
        // refusal above intact (the signed forms still fail on the sign) while this one silently changes
        // branch, so the boundary is pinned here from above as well.
        weigertOpVorm("10000-01-01", "a five-digit year is not the case lexeme");
    }

    @Test void een_smal_maand_of_dagveld_wordt_geweigerd() throws Exception {
        // fixed widths: a one-digit month or day is the day-first source token's shape, not this wire's.
        weigertOpVorm("2026-6-21", "a one-digit month is not the case lexeme");
        weigertOpVorm("2026-06-5", "a one-digit day is not the case lexeme");
    }

    @Test void een_zonesuffix_wordt_geweigerd() throws Exception {
        // uchar pins a calendar day; there is no zone on this wire, so a Z is not a suffix to drop.
        weigertOpVorm("2026-06-21Z", "a zone suffix is not the case lexeme");
    }

    @Test void het_basisformaat_wordt_geweigerd() throws Exception {
        weigertOpVorm("20260621", "the separator-less basic format is not the case lexeme");
    }

    @Test void omringende_tekst_wordt_geweigerd() throws Exception {
        // whole-string: the lexeme IS the value. A reader searching for a lexeme INSIDE the value would
        // answer here, and would answer whatever else the surrounding text meant to say.
        weigertOpVorm("rommel 2026-06-21 x", "the lexeme is the value, not a substring in it");
        weigertOpVorm(" 2026-06-21", "a leading space is not trimmed away");
    }

    @Test void een_datum_tijd_lexeem_wordt_geweigerd() throws Exception {
        // the two wires are not one wire: a dateTime value belongs under the "dateTime" key, where the
        // sister clamp reads it, and a date position that accepted it would silently drop the time of day.
        weigertOpVorm("2026-06-21T12:00:00", "a dateTime lexeme is not a date value");
    }

    // ---------------- the two calendar forms: the widths pass, the day does not exist ----------------

    @Test void een_niet_bestaande_dag_wordt_geweigerd_op_de_kalender() throws Exception {
        // every field has its width and its range looks plausible; February has no 30th.
        weigertOpKalender("2026-02-30", "a real month, not a real day");
    }

    @Test void negenentwintig_februari_buiten_een_schrikkeljaar_wordt_geweigerd_op_de_kalender() throws Exception {
        // the one calendar refusal that depends on the YEAR field, so a clamp that checked the day against a
        // fixed month length instead of the real calendar is caught here and nowhere else.
        weigertOpKalender("2023-02-29", "February 29 exists only in a leap year");
    }

    @Test void een_maand_buiten_het_jaar_wordt_geweigerd_op_de_kalender() throws Exception {
        weigertOpKalender("2026-13-01", "two digits, but not a month of a year");
    }

    // ---------------- the counter-measurement: what the wire admits is READ ----------------

    @Test void de_canonieke_vorm_wordt_gelezen() throws Exception {
        // a clamp that refuses too much is as wrong as one that refuses nothing.
        assertEquals(LocalDate.of(2026, 6, 21), vondstdatum("2026-06-21"),
                "the canonical lexeme is the value it names");
    }

    @Test void de_jaarranden_die_het_patroon_toelaat_worden_gelezen() throws Exception {
        // The clamp is pinned from BELOW here. Both years are inside the schema pattern and the interpreter
        // reads both (measured), and no corpus case sits anywhere near either edge — so a clamp narrowed to
        // a "plausible" year range would pass every other case in this file and every gate.
        assertEquals(LocalDate.of(0, 1, 1), vondstdatum("0000-01-01"),
                "year 0000 is four digits, and the interpreter reads it");
        assertEquals(LocalDate.of(9999, 12, 31), vondstdatum("9999-12-31"),
                "year 9999 is the last the four-digit field can write, and the interpreter reads it");
    }

    @Test void een_schrikkeldag_wordt_gelezen() throws Exception {
        assertEquals(LocalDate.of(2024, 2, 29), vondstdatum("2024-02-29"),
                "February 29 of a leap year is a real day — the calendar branch must not refuse it");
    }

    // ---------------- route 2: the case's asOfDate ----------------

    @Test void de_asOfDate_weigert_dezelfde_vormen_en_noemt_zijn_veld() throws Exception {
        // The position where the leak was worst: this date IS the evaluation date, so a misread here moves
        // every vintage selection, every bucket and every Rekendatum read at once — and the case still
        // replayed as PLAYED. The refusal names the property, which is the only handle a producer has.
        String melding = weigeringAsOf("+012026-03-15");
        assertTrue(melding.contains("'asOfDate'"), "the refusal names the property it arrived on: " + melding);
        assertTrue(melding.contains(LEXEEM_VORM), "and states the form it expected: " + melding);
        assertFalse(melding.contains(KALENDER), "and does not blame the calendar: " + melding);
        weigeringAsOf("-0500-01-01");
    }

    @Test void de_asOfDate_leest_een_geldige_datum_nog_steeds() throws Exception {
        // the counter-measurement on this route: the evaluation date still places the scalar in its
        // calendar-year bucket, at a year edge the corpus never visits.
        List<?> tl = emmerVan("0000-01-01");
        assertEquals(1, tl.size(), "the scalar becomes exactly one bucket segment");
        assertEquals(LocalDate.of(0, 1, 1), RegelSpraakRuntime.get(tl.get(0), "van"));
        assertEquals(LocalDate.of(1, 1, 1), RegelSpraakRuntime.get(tl.get(0), "tot"));
    }

    // ---------------- route 3: a timeline segment's from/to ----------------

    @Test void een_segmentgrens_weigert_dezelfde_vormen_en_noemt_zijn_plek() throws Exception {
        // The second measured leak: a boundary read as year −500 silently rewrites the bucket grid the whole
        // timeline hangs on, and the golden still replayed. The refusal names the position — which timeline
        // and which of its two boundaries — because a case may carry many, and a bare parse error names none.
        String melding = weigeringPeriode("-0500-01-01", "2025-04-16", "-0500-01-01");
        assertTrue(melding.contains("dienstdoend") && melding.contains("from"),
                "the refusal names the timeline and the boundary it arrived on: " + melding);
        assertTrue(melding.contains(LEXEEM_VORM), "and states the form it expected: " + melding);
        assertFalse(melding.contains(KALENDER), "and does not blame the calendar: " + melding);

        String tot = weigeringPeriode("2025-03-01", "+012026-01-01", "+012026-01-01");
        assertTrue(tot.contains("dienstdoend") && tot.contains("to"),
                "the other boundary names itself just as precisely: " + tot);
    }

    @Test void een_segmentgrens_leest_een_geldige_periode_nog_steeds() throws Exception {
        List<?> tl = periode("2025-03-01", "2025-04-16");
        assertEquals(1, tl.size());
        assertEquals(LocalDate.of(2025, 3, 1), RegelSpraakRuntime.get(tl.get(0), "van"));
        assertEquals(LocalDate.of(2025, 4, 16), RegelSpraakRuntime.get(tl.get(0), "tot"));
    }

    // ---------------- helpers ----------------

    private static Path ir(String golden) {
        Path p = CORPUS.resolve(golden).resolve("ir.json");
        assertTrue(Files.exists(p), "golden ir.json present: " + p.toAbsolutePath());
        return p;
    }

    /** The value the case reader made of {@code lexeem} on {@code Vondst.vondstdatum} (a {@code date}
     *  attribute), read back off the input bean before any rule fires. */
    private static Object vondstdatum(String lexeem) throws Exception {
        String zaak = """
                { "format": "uchar-case-v0", "asOfDate": null,
                  "input": { "instances": [
                      { "label": "V1", "objectType": "Vondst",
                        "attributes": { "vondstdatum": { "date": "%s" } },
                        "roles": {}, "characteristics": [] } ],
                    "parameters": {} } }
                """.formatted(lexeem);
        Map<String, Object> beans = CaseRunner.invoerBeans(ir("jutterswet-jutdatum"), JSON.readTree(zaak));
        return RegelSpraakRuntime.get(beans.get("V1"), "vondstdatum");
    }

    /** The one-bucket timeline {@code asOf} places the scalar input in, on the JAAR-granular attribute of
     *  {@code jutterswet-penningjaar} — the observable trace of the evaluation date the reader parsed. */
    private static List<?> emmerVan(String asOf) throws Exception {
        String zaak = """
                { "format": "uchar-case-v0", "asOfDate": "%s",
                  "input": { "instances": [
                      { "label": "J1", "objectType": "Jutter",
                        "attributes": { "aantal aangemelde vondsten": { "number": "14" } },
                        "roles": {}, "characteristics": [] } ],
                    "parameters": {} } }
                """.formatted(asOf);
        Map<String, Object> beans = CaseRunner.invoerBeans(ir("jutterswet-penningjaar"), JSON.readTree(zaak));
        return (List<?>) RegelSpraakRuntime.get(beans.get("J1"), "aantal aangemelde vondsten");
    }

    /** The true-period segments a {@code characteristicTimelines} entry denotes — the day-precise boundary
     *  route, so any calendar day is admissible here and the granularity grid never masks the clamp. */
    private static List<?> periode(String van, String tot) throws Exception {
        String zaak = """
                { "format": "uchar-case-v0", "asOfDate": null,
                  "input": { "instances": [
                      { "label": "B1", "objectType": "Bakenmeester",
                        "attributes": {}, "roles": {}, "characteristics": [],
                        "characteristicTimelines": { "dienstdoend": [
                            { "from": "%s", "to": "%s" } ] } } ],
                    "parameters": {} } }
                """.formatted(van, tot);
        Map<String, Object> beans = CaseRunner.invoerBeans(ir("jutterswet-bakenmeester"), JSON.readTree(zaak));
        return (List<?>) RegelSpraakRuntime.get(beans.get("B1"), "dienstdoend");
    }

    /** The refusal message {@code lexeem} must raise on a route — or an assertion failure naming what the
     *  reader did instead: the value it accepted, the bare exception it let through, or the exclusion it
     *  filed. {@code lees} performs the read; {@code toon} renders what a read returned. */
    private static String weigering(ThrowingRead lees, String lexeem, String waarom) throws Exception {
        Object gelezen;
        try {
            gelezen = lees.lees();
        } catch (IllegalArgumentException weigering) {
            String melding = weigering.getMessage();
            assertTrue(melding.contains("'" + lexeem + "'"),
                    waarom + " — the refusal quotes the value it refused: " + melding);
            return melding;
        } catch (CaseRunner.Excluded uitgesloten) {
            throw new AssertionError(waarom + " — but the reader EXCLUDED it (" + uitgesloten.getMessage()
                    + "), which files a malformed case under 'not carried' and leaves the gate green");
        } catch (RuntimeException anders) {
            throw new AssertionError(waarom + " — but the reader let it through to a bare "
                    + anders.getClass().getName() + ": " + anders.getMessage());
        }
        throw new AssertionError(waarom + " — but the reader ACCEPTED it and read " + gelezen);
    }

    private interface ThrowingRead { Object lees() throws Exception; }

    /** The value route's refusal, with the field it arrived on asserted as well. */
    private static String weigeringWaarde(String lexeem, String waarom) throws Exception {
        String melding = weigering(() -> vondstdatum(lexeem), lexeem, waarom);
        assertTrue(melding.contains("'vondstdatum'"),
                waarom + " — and names the field it arrived on: " + melding);
        return melding;
    }

    private static String weigeringAsOf(String lexeem) throws Exception {
        return weigering(() -> emmerVan(lexeem), lexeem, "an asOfDate outside the lexeme");
    }

    /** {@code fout} is whichever of the two boundaries is the malformed one — the refusal must quote THAT
     *  one, not the well-formed boundary beside it. */
    private static String weigeringPeriode(String van, String tot, String fout) throws Exception {
        return weigering(() -> periode(van, tot), fout, "a segment boundary outside the lexeme");
    }

    /** Refused by the pattern, with the expected FORM in the message and the calendar left out of it. The
     *  negative half is the mirror of the one below, and it is load-bearing for the same reason: without it a
     *  form branch that ALSO carries the calendar wording passes every case here, and the two branches this
     *  reader deliberately splits become indistinguishable to a producer reading the message. */
    private static void weigertOpVorm(String lexeem, String waarom) throws Exception {
        String melding = weigeringWaarde(lexeem, waarom);
        assertTrue(melding.contains(LEXEEM_VORM),
                waarom + " — a refusal a producer can act on states the form it expected: " + melding);
        assertFalse(melding.contains(KALENDER),
                waarom + " — and does not also blame the calendar, which was never read here: " + melding);
    }

    /** Refused by the calendar, with the calendar in the message and the field widths left alone. */
    private static void weigertOpKalender(String lexeem, String waarom) throws Exception {
        String melding = weigeringWaarde(lexeem, waarom);
        assertTrue(melding.contains(KALENDER),
                waarom + " — the refusal names the calendar: " + melding);
        assertFalse(melding.contains(LEXEEM_VORM),
                waarom + " — and does not blame the field widths, which are right here: " + melding);
    }
}
