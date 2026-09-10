package io.github.yhavinga.regelspraak.loader;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.github.yhavinga.regelspraak.ir.Geldigheid;
import io.github.yhavinga.regelspraak.model.RegelSpraakModel;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The REFUSING half of this backend's IR-wire date reader: the {@code vanaf}/{@code tot} bounds of a
 * {@code Geldigheid}, which is the ONE place a date reaches the loader as a lexeme. The sister classes
 * {@code CaseWireDatumWitnessTest} and {@code CaseWireDatumTijdWitnessTest} do the same on the case wire;
 * this one exists because the IR wire held a bare {@code LocalDate.parse} on both bounds until 2026-09-11
 * and — unlike the case wire — has NO schema pattern behind it either, so nothing anywhere refused a
 * malformed validity bound.
 *
 * <p><b>What the laxness cost, measured 2026-09-11</b> on a hand-edited copy of
 * {@code jutterswet-vensterheffing} (two rule versions, an in-window case and a before-window case), with
 * the {@code vanaf} of the first version replaced:
 *
 * <pre>
 *   +012026-01-01   verifier PASS   interpreter/ASP: the version APPLIES   here: it did NOT (year 12026)
 *   -0500-01-01     verifier PASS   interpreter/ASP: the version APPLIES   here: it DID  (year -500)
 *   2026-02-30      verifier PASS   interpreter/ASP: a boundary like any   here: bare parse exception
 *   niet-een-datum  verifier PASS   interpreter/ASP: a boundary like any   here: bare parse exception
 * </pre>
 *
 * <p>The first row is the divergence: one document, two answers. The other three readers of a validity
 * window hold the bound as an OPAQUE STRING and order it lexicographically (the verifier says so in as many
 * words beside its overlap check: "ISO dates compare lexicographically"), so a signed year sorts before
 * every four-digit one and the version applies; this reader turns the same lexeme into year 12026 and the
 * version never applies. Row two is the quiet half of the same defect: there the two readings happen to
 * AGREE, so the malformed bound leaves no trace at all. Rows three and four are the opposite asymmetry —
 * this reader is the only one that notices, and it says so with a bare {@code DateTimeParseException} that
 * names neither the field nor the form.
 *
 * <p><b>The lexeme.</b> A validity bound is a strict ISO date, matched WHOLE: four-two-two digit fields,
 * digits only, no sign and no extended year, and then a real calendar day. That is WF-084's date-literal
 * grammar (IR-SPEC ch. 4.1) applied to the other date position on this wire, and it is exactly the
 * precondition the three lexicographic readers assume without checking. Anything else is refused HARD (an
 * {@link IllegalArgumentException}), never as an {@link IrLoader.Refusal}: a Refusal means "form not
 * carried by the v0 Drools backend" and would file a malformed document under the differential gate's
 * auto-excluded channel, which is a green gate over a document nobody read.
 *
 * <p><b>Both year edges the pattern admits are pinned as read.</b> No corpus document sits anywhere near
 * year 0000 or 9999 — the nine bounds the corpus carries are all 2025 or 2026 — so a clamp narrowed from
 * below (a "plausible year range", a no-year-zero rule) would pass every other case here and every gate.
 *
 * <p><b>Why the refusal must say WHICH half failed.</b> The reader answers in two branches and the cases
 * below assert the branch as well as the refusal. A bound whose FIELDS are wrong is told the form it should
 * have had; a bound whose fields are right but whose calendar is not is told the calendar, and is NOT told
 * the field widths, which are correct and would send its producer looking in the wrong place.
 *
 * <p><b>The reach of the clamp is asserted, not assumed.</b> One document kind would leave nine of the ten
 * call sites untested, so {@link #elke_geldigheidsdrager_wordt_langs_dezelfde_klem_gelezen()} twists a
 * bound on every node kind that carries a validity window and demands all nine. The one gap left, named
 * here rather than hidden: {@code ResolvedFeitCreatie} has TWO call sites (with and without a
 * {@code feittype}) and all four corpus witnesses carry a {@code feittype}, so the second branch is covered
 * by construction — one shared helper — and not by a document.
 *
 * <p>The models are corpus goldens read off the wire and edited in memory; no corpus file is touched and no
 * corpus expectation is read.
 */
class IrWireDatumWitnessTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    /** cwd under surefire is the module dir (backends/drools); the corpus is two levels up. */
    private static final Path CORPUS = Path.of("..", "..", "corpus", "golden");

    /** The form branch of the refusal names the lexeme it expected. */
    private static final String LEXEEM_VORM = "yyyy-mm-dd";
    /** The calendar branch names the calendar instead — the fields were right. */
    private static final String KALENDER = "the calendar is not";

    /** The golden the single-bound cases twist: two rule versions, one with BOTH bounds written. */
    private static final String VENSTER = "jutterswet-vensterheffing";
    /** The rule whose window the twists land on — the refusal must name it, since a document carries many. */
    private static final String VENSTERREGEL = "vensterheffing in 2026";

    // ---------------- the field classes the pattern refuses ----------------

    @Test void een_getekend_uitgebreid_jaar_wordt_geweigerd() throws Exception {
        // The measured divergence. Read here as year 12026, so the version stops applying; read by the three
        // string-comparing readers as a bound that sorts before every four-digit year, so it always applies.
        weigertOpVorm("+012026-01-01", "a signed extended year is not the wire lexeme");
    }

    @Test void een_negatief_jaar_wordt_geweigerd() throws Exception {
        // The quiet half: year -500 and the string "-0500-01-01" both order BEFORE the evaluation date, so
        // every reader agrees and the malformed bound leaves no trace. It is also the form that pins WHOLE
        // matching from the other side — a reader searching for the pattern INSIDE the value finds
        // "0500-01-01" here and answers about a date nobody wrote.
        weigertOpVorm("-0500-01-01", "a negative year is not the wire lexeme");
    }

    @Test void een_jaarveld_van_vijf_cijfers_wordt_geweigerd() throws Exception {
        // The year field is exactly four wide, not "at least four": widening {4} to {4,} leaves every refusal
        // above intact (the signed forms still fail on the sign) while this one silently changes branch, so
        // the boundary is pinned here from above as well.
        weigertOpVorm("10000-01-01", "a five-digit year is not the wire lexeme");
    }

    @Test void een_smal_maand_of_dagveld_wordt_geweigerd() throws Exception {
        // Fixed widths. This is the shape a hand-written document reaches for first, and it is also the one
        // form where the lexicographic readers go wrong SILENTLY in the ordinary range: "2026-1-01" sorts
        // after "2026-09-30", so a window written this way selects the wrong versions with no reader saying so.
        weigertOpVorm("2026-1-01", "a one-digit month is not the wire lexeme");
        weigertOpVorm("2026-01-1", "a one-digit day is not the wire lexeme");
    }

    @Test void een_zonesuffix_wordt_geweigerd() throws Exception {
        // uchar pins a calendar day; there is no zone on this wire, so a Z is not a suffix to drop.
        weigertOpVorm("2026-01-01Z", "a zone suffix is not the wire lexeme");
    }

    @Test void het_basisformaat_wordt_geweigerd() throws Exception {
        weigertOpVorm("20260101", "the separator-less basic format is not the wire lexeme");
    }

    @Test void omringende_tekst_wordt_geweigerd() throws Exception {
        // Whole-string: the lexeme IS the bound. A reader searching for a lexeme INSIDE the value would answer
        // here, and would answer whatever else the surrounding text meant to say.
        weigertOpVorm("rommel 2026-01-01 x", "the lexeme is the bound, not a substring in it");
        weigertOpVorm(" 2026-01-01", "a leading space is not trimmed away");
    }

    @Test void een_datum_tijd_lexeem_wordt_geweigerd() throws Exception {
        // A validity window is day-precision (IR-SPEC ch. 3.10); a bound that accepted a datetime would
        // silently drop the time of day and pretend the window still started where its producer wrote it.
        weigertOpVorm("2026-01-01T00:00:00", "a dateTime lexeme is not a validity bound");
    }

    @Test void een_lege_of_zinloze_tekst_wordt_geweigerd() throws Exception {
        // Measured as the widest hole of all: with "niet-een-datum" as a bound the verifier reports PASS and
        // both string-comparing executors quietly order the window by that text. Nothing on this wire refused
        // it, in any reader, until this clamp.
        weigertOpVorm("niet-een-datum", "arbitrary text is not a validity bound");
        weigertOpVorm("", "an empty string is not an open bound — null is");
    }

    // ---------------- the calendar forms: the widths pass, the day does not exist ----------------

    @Test void een_niet_bestaande_dag_wordt_geweigerd_op_de_kalender() throws Exception {
        // Every field has its width and its range looks plausible; February has no 30th. The schema pattern
        // this clamp mirrors CANNOT catch this row — a regular expression does not know the calendar — which
        // is exactly why the clamp is not made redundant by the pattern landing on the wire.
        weigertOpKalender("2026-02-30", "a real month, not a real day");
    }

    @Test void negenentwintig_februari_buiten_een_schrikkeljaar_wordt_geweigerd_op_de_kalender() throws Exception {
        // The one calendar refusal that depends on the YEAR field, so a clamp that checked the day against a
        // fixed month length instead of the real calendar is caught here and nowhere else.
        weigertOpKalender("2023-02-29", "February 29 exists only in a leap year");
    }

    @Test void een_maand_buiten_het_jaar_wordt_geweigerd_op_de_kalender() throws Exception {
        weigertOpKalender("2026-13-01", "two digits, but not a month of a year");
    }

    // ---------------- the counter-measurement: what the wire admits is READ ----------------

    @Test void het_ongetwiste_venster_wordt_gelezen_zoals_het_op_de_wire_staat() throws Exception {
        // The baseline every twist below is measured against: a clamp that refuses too much is as wrong as
        // one that refuses nothing.
        Geldigheid g = venster(laad(VENSTER));
        assertEquals(LocalDate.of(2026, 1, 1), g.vanaf(), "the golden's own lower bound");
        assertEquals(LocalDate.of(2026, 12, 31), g.tot(), "the golden's own upper bound");
    }

    @Test void de_jaarranden_die_het_patroon_toelaat_worden_gelezen() throws Exception {
        // The clamp pinned from BELOW. Both years are inside the pattern and the three string-comparing
        // readers order both without complaint, and no corpus document sits anywhere near either edge — so a
        // clamp narrowed to a "plausible" year range would pass every other case in this file and every gate.
        assertEquals(LocalDate.of(0, 1, 1), vanafGelezen("0000-01-01"),
                "year 0000 is four digits, and every other reader of this wire takes it");
        assertEquals(LocalDate.of(9999, 12, 31), totGelezen("9999-12-31"),
                "year 9999 is the last the four-digit field can write, and every other reader takes it");
    }

    @Test void een_schrikkeldag_wordt_gelezen() throws Exception {
        assertEquals(LocalDate.of(2024, 2, 29), vanafGelezen("2024-02-29"),
                "February 29 of a leap year is a real day — the calendar branch must not refuse it");
    }

    @Test void een_open_grens_blijft_open() throws Exception {
        // null is the open bound and must reach the clamp as null, not as a lexeme to parse: the second rule
        // version of this golden has an open lower bound and is the corpus witness for it.
        RegelSpraakModel m = laad(VENSTER);
        Geldigheid open = m.resolvedRegels.get(1).geldigheid();
        assertEquals(null, open.vanaf(), "an absent lower bound stays absent");
        assertEquals(LocalDate.of(2025, 12, 31), open.tot(), "and the bound beside it is still read");
    }

    // ---------------- both bounds name themselves ----------------

    @Test void beide_grenzen_noemen_hun_eigen_veld_en_hun_eigen_drager() throws Exception {
        // A document carries hundreds of validity bounds (848 in the corpus as it stands), so a refusal that
        // named only "vanaf" would leave a producer searching. Both coordinates are asserted here: which
        // bound, and which of the many carriers it belongs to.
        String onder = weigering(() -> vanafGelezen("+012026-01-01"), "+012026-01-01", "a malformed lower bound");
        assertTrue(onder.contains("vanaf"), "the refusal names the bound it arrived on: " + onder);
        assertFalse(onder.contains("tot"), "and does not name the other bound: " + onder);
        assertTrue(onder.contains(VENSTERREGEL), "and names the carrier it sits on: " + onder);

        String boven = weigering(() -> totGelezen("+012026-12-31"), "+012026-12-31", "a malformed upper bound");
        assertTrue(boven.contains("tot"), "the other bound names itself just as precisely: " + boven);
        assertTrue(boven.contains(VENSTERREGEL), "and names its carrier too: " + boven);
    }

    @Test void een_naamloze_drager_valt_terug_op_zijn_knoopsoort() throws Exception {
        // A carrier's `naam` is ["string","null"] on all nine kinds, so a nameless carrier is a legal wire
        // document — no corpus document is one (0 of 424 measured 2026-09-11), which is why the fall-back
        // would otherwise never be exercised and could silently degrade to a bare "geldigheid.vanaf" that
        // locates nothing. The refusal names the node kind instead, and still names the bound.
        JsonNode root = wire(VENSTER);
        JsonNode regel = root.path("resolvedRegels").path(0);
        ((ObjectNode) regel).putNull("naam");
        ((ObjectNode) regel.path("geldigheid")).put("vanaf", "+012026-01-01");

        String melding = weigering(() -> IrLoader.load(root), "+012026-01-01", "a bound on a nameless carrier");
        assertTrue(melding.contains("ResolvedRegel"), "the refusal names the node kind: " + melding);
        assertTrue(melding.contains("geldigheid.vanaf"), "and still names the bound: " + melding);
        assertFalse(melding.contains("'geldigheid.vanaf'"),
                "and never degrades to the bare field, which locates nothing: " + melding);
    }

    // ---------------- the reach: every node kind that carries a window ----------------

    @Test void elke_geldigheidsdrager_wordt_langs_dezelfde_klem_gelezen() throws Exception {
        // The loader reads a validity window from TEN call sites across NINE node kinds. A witness on one kind
        // would let a later edit reintroduce a bare parse on any of the other nine and stay green, which is
        // the gap the sister case-wire witness recorded about itself. Every kind the corpus exercises is
        // twisted here, and the count is asserted, so a kind that stops being covered fails by name.
        List<String> goldens = List.of("anchor/jutterswet-kustregister", "anchor/jutterswet-penninguitgifte",
                "anchor/jutterswet-bergingstoezicht", "anchor/jutterswet-erflijn", "anchor/jutterswet-hardheid",
                "jutterswet-schouwkalender");
        Set<String> gedekt = new TreeSet<>();
        for (String golden : goldens) {
            for (String soort : dragerSoorten(golden)) {
                if (!gedekt.add(soort)) continue;
                String melding = weigering(() -> laadMetGetwisteDrager(golden, soort, "+012026-01-01"),
                        "+012026-01-01", "a malformed bound on a " + soort);
                assertTrue(melding.contains(LEXEEM_VORM),
                        soort + " — the refusal states the form it expected: " + melding);
            }
        }
        assertEquals(Set.of("PrioritizedDefaults", "ResolvedConsistentie", "ResolvedDagsoort",
                        "ResolvedFeitCreatie", "ResolvedObjectCreatie", "ResolvedRegel",
                        "ResolvedUniciteit", "ResolvedUniciteitUnie", "ResolvedVerdeling"),
                gedekt, "every node kind that carries a validity window is read through the clamp");
    }

    // ---------------- helpers ----------------

    private static JsonNode wire(String golden) throws Exception {
        Path p = CORPUS.resolve(golden).resolve("ir.json");
        assertTrue(Files.exists(p), "golden ir.json present: " + p.toAbsolutePath());
        return JSON.readTree(Files.readString(p));
    }

    private static RegelSpraakModel laad(String golden) throws Exception {
        return IrLoader.load(wire(golden));
    }

    /** The window of the first rule version of {@link #VENSTER} — the one the twists land on. */
    private static Geldigheid venster(RegelSpraakModel m) {
        return m.resolvedRegels.get(0).geldigheid();
    }

    /** {@link #VENSTER} with one bound of its first rule version replaced, loaded. */
    private static Geldigheid grensGelezen(String grens, String lexeem) throws Exception {
        JsonNode root = wire(VENSTER);
        JsonNode g = root.path("resolvedRegels").path(0).path("geldigheid");
        assertTrue(g.isObject(), "the twisted rule version carries a Geldigheid node");
        ((ObjectNode) g).put(grens, lexeem);
        return venster(IrLoader.load(root));
    }

    private static LocalDate vanafGelezen(String lexeem) throws Exception {
        return grensGelezen("vanaf", lexeem).vanaf();
    }

    private static LocalDate totGelezen(String lexeem) throws Exception {
        return grensGelezen("tot", lexeem).tot();
    }

    /** The node kinds in {@code golden} that carry a {@code geldigheid} child, in document order. */
    private static List<String> dragerSoorten(String golden) throws Exception {
        List<String> uit = new ArrayList<>();
        verzamel(wire(golden), uit);
        return uit;
    }

    private static void verzamel(JsonNode n, List<String> uit) {
        if (n.isObject()) {
            JsonNode g = n.get("geldigheid");
            if (g != null && g.isObject() && "Geldigheid".equals(g.path("node").asText())) {
                uit.add(n.path("node").asText());
            }
            n.forEach(kind -> verzamel(kind, uit));
        } else if (n.isArray()) {
            n.forEach(kind -> verzamel(kind, uit));
        }
    }

    /** {@code golden} with the FIRST validity window on a {@code soort} node given {@code lexeem} as its
     *  lower bound, loaded — the per-node-kind reach probe. */
    private static Object laadMetGetwisteDrager(String golden, String soort, String lexeem) throws Exception {
        JsonNode root = wire(golden);
        assertTrue(twist(root, soort, lexeem), golden + ": carries a " + soort + " with a validity window");
        return IrLoader.load(root);
    }

    private static boolean twist(JsonNode n, String soort, String lexeem) {
        if (n.isObject()) {
            JsonNode g = n.get("geldigheid");
            if (g != null && g.isObject() && "Geldigheid".equals(g.path("node").asText())
                    && soort.equals(n.path("node").asText())) {
                ((ObjectNode) g).put("vanaf", lexeem);
                return true;
            }
        }
        for (JsonNode kind : n) if (twist(kind, soort, lexeem)) return true;
        return false;
    }

    /** The refusal message {@code lexeem} must raise — or an assertion failure naming what the reader did
     *  instead: the value it accepted, the bare exception it let through, or the "not carried" Refusal it
     *  filed. */
    private static String weigering(ThrowingRead lees, String lexeem, String waarom) throws Exception {
        Object gelezen;
        try {
            gelezen = lees.lees();
        } catch (IllegalArgumentException weigering) {
            String melding = weigering.getMessage();
            assertTrue(melding != null && melding.contains("'" + lexeem + "'"),
                    waarom + " — the refusal quotes the value it refused: " + melding);
            return melding;
        } catch (IrLoader.Refusal nietGedragen) {
            throw new AssertionError(waarom + " — but the loader REFUSED it as a form it does not carry ("
                    + nietGedragen.getMessage() + "), which files a malformed document under the differential"
                    + " gate's auto-excluded channel and leaves the gate green over a document nobody read");
        } catch (RuntimeException anders) {
            throw new AssertionError(waarom + " — but the reader let it through to a bare "
                    + anders.getClass().getName() + ": " + anders.getMessage());
        }
        throw new AssertionError(waarom + " — but the reader ACCEPTED it and read " + gelezen);
    }

    private interface ThrowingRead { Object lees() throws Exception; }

    /** Refused by the pattern, with the expected FORM in the message and the calendar left out of it. The
     *  negative half is load-bearing: without it a form branch that ALSO carries the calendar wording passes
     *  every case here, and the two branches this reader deliberately splits become indistinguishable to a
     *  producer reading the message. */
    private static void weigertOpVorm(String lexeem, String waarom) throws Exception {
        String melding = weigering(() -> vanafGelezen(lexeem), lexeem, waarom);
        assertTrue(melding.contains(LEXEEM_VORM),
                waarom + " — a refusal a producer can act on states the form it expected: " + melding);
        assertFalse(melding.contains(KALENDER),
                waarom + " — and does not also blame the calendar, which was never read here: " + melding);
    }

    /** Refused by the calendar, with the calendar in the message and the field widths left alone. */
    private static void weigertOpKalender(String lexeem, String waarom) throws Exception {
        String melding = weigering(() -> vanafGelezen(lexeem), lexeem, waarom);
        assertTrue(melding.contains(KALENDER),
                waarom + " — the refusal names the calendar: " + melding);
        assertFalse(melding.contains(LEXEEM_VORM),
                waarom + " — and does not blame the field widths, which are right here: " + melding);
    }
}
