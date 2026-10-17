package dev.uchar.regelspraak.loader;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import dev.uchar.regelspraak.cases.IrPad;
import dev.uchar.regelspraak.ir.Geldigheid;
import dev.uchar.regelspraak.model.RegelSpraakModel;

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
 * The REFUSING half of this backend's IR-wire date reader: the {@code from}/{@code to} bounds of a
 * {@code Validity}, which is the ONE place a date reaches the loader as a lexeme. The sister classes
 * {@code CaseWireDatumWitnessTest} and {@code CaseWireDatumTijdWitnessTest} do the same on the case wire;
 * this one exists because the IR wire held a bare {@code LocalDate.parse} on both bounds until 2026-09-11
 * and — unlike the case wire — has NO schema pattern behind it either, so nothing anywhere refused a
 * malformed validity bound.
 *
 * <p><b>What the laxness cost, measured 2026-09-11</b> on a hand-edited copy of
 * {@code jutterswet-vensterheffing} (two rule versions, an in-window case and a before-window case), with
 * the lower bound of the first version replaced:
 *
 * <pre>
 *   +012026-01-01   verifier PASS   interpreter/ASP: the version APPLIES   here: it did NOT (year 12026)
 *   -0500-01-01     verifier PASS   interpreter/ASP: the version APPLIES   here: it DID  (year -500)
 *   2026-02-30      verifier PASS   interpreter/ASP: a boundary like any   here: bare parse exception
 *   niet-een-datum  verifier PASS   interpreter/ASP: a boundary like any   here: bare parse exception
 * </pre>
 *
 * <p>The first row is the divergence: one document, two answers. Until 2026-09-23 the other three readers
 * of a validity window held the bound as an OPAQUE STRING and ordered it lexicographically (since card
 * ref-geldigheid-datumlezing only the verifier still does), so a signed year sorted before
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
 * call sites untested, so {@link #each_validity_carrier_is_read_along_the_same_clamp()} twists a
 * bound on every kind of carrier a validity window stands on and demands all nine: the version of each of the six
 * named forms (a uniqueness counted twice, as a single-type and as a union version), and the day-kind definition
 * and the defaults group, which keep their v0 field.
 *
 * <p>The models are corpus goldens read off the wire and edited in memory; no corpus file is touched and no
 * corpus expectation is read.
 */
class IrWireDateWitnessTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    /** cwd under surefire is the module dir (backends/drools); the corpus is two levels up. */
    private static final Path CORPUS = Path.of("..", "..", "corpus", "golden");

    /** The form branch of the refusal names the lexeme it expected. */
    private static final String LEXEME_FORM = "yyyy-mm-dd";
    /** The calendar branch names the calendar instead — the fields were right. */
    private static final String CALENDAR = "the calendar is not";

    /** The golden the single-bound cases twist: two rule versions, one with BOTH bounds written. */
    private static final String WINDOW = "jutterswet-vensterheffing";
    /** The version whose window the twists land on — the refusal must name its place, since a document carries
     *  many windows. */
    private static final String WINDOW_VERSION = "/rules/rules/0/versions/0/validity";

    // ---------------- the field classes the pattern refuses ----------------

    @Test void a_signed_extended_year_is_refused() throws Exception {
        // The measured divergence. Read here as year 12026, so the version stops applying; read by the three
        // string-comparing readers as a bound that sorts before every four-digit year, so it always applies.
        refusedOnForm("+012026-01-01", "a signed extended year is not the wire lexeme");
    }

    @Test void a_negative_year_is_refused() throws Exception {
        // The quiet half: year -500 and the string "-0500-01-01" both order BEFORE the evaluation date, so
        // every reader agrees and the malformed bound leaves no trace. It is also the form that pins WHOLE
        // matching from the other side — a reader searching for the pattern INSIDE the value finds
        // "0500-01-01" here and answers about a date nobody wrote.
        refusedOnForm("-0500-01-01", "a negative year is not the wire lexeme");
    }

    @Test void a_year_field_of_five_digits_is_refused() throws Exception {
        // The year field is exactly four wide, not "at least four": widening {4} to {4,} leaves every refusal
        // above intact (the signed forms still fail on the sign) while this one silently changes branch, so
        // the boundary is pinned here from above as well.
        refusedOnForm("10000-01-01", "a five-digit year is not the wire lexeme");
    }

    @Test void a_narrow_month_or_day_field_is_refused() throws Exception {
        // Fixed widths. This is the shape a hand-written document reaches for first, and it is also the one
        // form where the lexicographic readers go wrong SILENTLY in the ordinary range: "2026-1-01" sorts
        // after "2026-09-30", so a window written this way selects the wrong versions with no reader saying so.
        refusedOnForm("2026-1-01", "a one-digit month is not the wire lexeme");
        refusedOnForm("2026-01-1", "a one-digit day is not the wire lexeme");
    }

    @Test void a_zone_suffix_is_refused() throws Exception {
        // uchar pins a calendar day; there is no zone on this wire, so a Z is not a suffix to drop.
        refusedOnForm("2026-01-01Z", "a zone suffix is not the wire lexeme");
    }

    @Test void the_basic_format_is_refused() throws Exception {
        refusedOnForm("20260101", "the separator-less basic format is not the wire lexeme");
    }

    @Test void surrounding_text_is_refused() throws Exception {
        // Whole-string: the lexeme IS the bound. A reader searching for a lexeme INSIDE the value would answer
        // here, and would answer whatever else the surrounding text meant to say.
        refusedOnForm("rommel 2026-01-01 x", "the lexeme is the bound, not a substring in it");
        refusedOnForm(" 2026-01-01", "a leading space is not trimmed away");
    }

    @Test void a_date_time_lexeme_is_refused() throws Exception {
        // A validity window is day-precision (IR-SPEC ch. 3.10); a bound that accepted a datetime would
        // silently drop the time of day and pretend the window still started where its producer wrote it.
        refusedOnForm("2026-01-01T00:00:00", "a dateTime lexeme is not a validity bound");
    }

    @Test void an_empty_or_meaningless_text_is_refused() throws Exception {
        // Measured as the widest hole of all: with "niet-een-datum" as a bound the verifier reports PASS and
        // both string-comparing executors quietly order the window by that text. Nothing on this wire refused
        // it, in any reader, until this clamp.
        refusedOnForm("niet-een-datum", "arbitrary text is not a validity bound");
        refusedOnForm("", "an empty string is not an open bound — null is");
    }

    // ---------------- the calendar forms: the widths pass, the day does not exist ----------------

    @Test void a_non_existing_day_is_refused_on_the_calendar() throws Exception {
        // Every field has its width and its range looks plausible; February has no 30th. The schema pattern
        // this clamp mirrors CANNOT catch this row — a regular expression does not know the calendar — which
        // is exactly why the clamp is not made redundant by the pattern landing on the wire.
        refusedOnCalendar("2026-02-30", "a real month, not a real day");
    }

    @Test void twenty_nine_february_outside_a_leap_year_is_refused_on_the_calendar() throws Exception {
        // The one calendar refusal that depends on the YEAR field, so a clamp that checked the day against a
        // fixed month length instead of the real calendar is caught here and nowhere else.
        refusedOnCalendar("2023-02-29", "February 29 exists only in a leap year");
    }

    @Test void a_month_outside_the_year_is_refused_on_the_calendar() throws Exception {
        refusedOnCalendar("2026-13-01", "two digits, but not a month of a year");
    }

    // ---------------- the counter-measurement: what the wire admits is READ ----------------

    @Test void the_untwisted_window_is_read_as_it_stands_on_the_wire() throws Exception {
        // The baseline every twist below is measured against: a clamp that refuses too much is as wrong as
        // one that refuses nothing.
        Geldigheid g = window(load(WINDOW));
        assertEquals(LocalDate.of(2026, 1, 1), g.vanaf(), "the golden's own lower bound");
        assertEquals(LocalDate.of(2026, 12, 31), g.tot(), "the golden's own upper bound");
    }

    @Test void the_year_edges_the_pattern_admits_are_read() throws Exception {
        // The clamp pinned from BELOW. Both years are inside the pattern and the three string-comparing
        // readers order both without complaint, and no corpus document sits anywhere near either edge — so a
        // clamp narrowed to a "plausible" year range would pass every other case in this file and every gate.
        assertEquals(LocalDate.of(0, 1, 1), fromRead("0000-01-01"),
                "year 0000 is four digits, and every other reader of this wire takes it");
        assertEquals(LocalDate.of(9999, 12, 31), toRead("9999-12-31"),
                "year 9999 is the last the four-digit field can write, and every other reader takes it");
    }

    @Test void a_leap_day_is_read() throws Exception {
        assertEquals(LocalDate.of(2024, 2, 29), fromRead("2024-02-29"),
                "February 29 of a leap year is a real day — the calendar branch must not refuse it");
    }

    @Test void an_open_bound_stays_open() throws Exception {
        // null is the open bound and must reach the clamp as null, not as a lexeme to parse: the second rule
        // version of this golden has an open lower bound and is the corpus witness for it.
        RegelSpraakModel m = load(WINDOW);
        Geldigheid open = m.resolvedRegels.get(1).geldigheid();
        assertEquals(null, open.vanaf(), "an absent lower bound stays absent");
        assertEquals(LocalDate.of(2025, 12, 31), open.tot(), "and the bound beside it is still read");
    }

    // ---------------- both bounds name themselves ----------------

    @Test void both_bounds_name_their_own_field_and_their_own_carrier() throws Exception {
        // A document carries hundreds of validity bounds (848 in the corpus as it stands), so a refusal that
        // named only "from" would leave a producer searching. Both coordinates are asserted here: which
        // bound, and which of the many carriers it belongs to.
        String lowerBoundRefusal = refusal(() -> fromRead("+012026-01-01"), "+012026-01-01", "a malformed lower bound");
        assertTrue(lowerBoundRefusal.contains("'" + WINDOW_VERSION + "/from'"),
                "the refusal names the bound it arrived on, on the version that carries it: " + lowerBoundRefusal);
        assertFalse(lowerBoundRefusal.contains("/to'"), "and does not name the other bound: " + lowerBoundRefusal);

        String upperBoundRefusal = refusal(() -> toRead("+012026-12-31"), "+012026-12-31", "a malformed upper bound");
        assertTrue(upperBoundRefusal.contains("'" + WINDOW_VERSION + "/to'"),
                "the other bound names itself and its carrier just as precisely: " + upperBoundRefusal);
    }

    @Test void an_unnamed_carrier_falls_back_on_its_node_kind() throws Exception {
        // The two carriers outside the named forms — a day-kind definition and a defaults group — name themselves
        // by their name field (dayKind, name), which is ["string","null"], so a nameless carrier is a legal wire
        // document; no corpus document is one, which is why the fall-back would otherwise never be exercised and
        // could silently degrade to a bare "validity.from" that locates nothing. The refusal names the node kind instead, and
        // still names the bound. A version of a named form needs no such fall-back: it is named by its pointer,
        // with or without a name on its form.
        JsonNode root = wire("jutterswet-schouwkalender");
        JsonNode dayKindDefinition = root.path("rules").path("dayKindDefinitions").path(0);
        ((ObjectNode) dayKindDefinition).putNull("dayKind");
        ((ObjectNode) dayKindDefinition.path("validity")).put("from", "+012026-01-01");
        String message = refusal(() -> IrLoader.load(root), "+012026-01-01", "a bound on a nameless carrier");
        assertTrue(message.contains("DayKindDefinition"), "the refusal names the node kind: " + message);
        assertTrue(message.contains("validity.from"), "and still names the bound: " + message);
        assertFalse(message.contains("'validity.from'"),
                "and never degrades to the bare field, which locates nothing: " + message);

        JsonNode window = wire(WINDOW);
        ((ObjectNode) IrPad.rulesLijst(window, "rules").get(0)).putNull("name");
        ((ObjectNode) IrPad.versie(window, "rules", 0, 0).get("validity")).put("from", "+012026-01-01");
        String version = refusal(() -> IrLoader.load(window), "+012026-01-01", "a bound on a nameless form");
        assertTrue(version.contains("'" + WINDOW_VERSION + "/from'"), "the version names its place: " + version);
    }

    // ---------------- the reach: every node kind that carries a window ----------------

    @Test void each_validity_carrier_is_read_along_the_same_clamp() throws Exception {
        // The loader reads a validity window from TEN call sites across NINE node kinds. A witness on one kind
        // would let a later edit reintroduce a bare parse on any of the other nine and stay green, which is
        // the gap the sister case-wire witness recorded about itself. Every kind the corpus exercises is
        // twisted here, and the count is asserted, so a kind that stops being covered fails by name.
        List<String> goldens = List.of("anchor/jutterswet-kustregister", "anchor/jutterswet-penninguitgifte",
                "anchor/jutterswet-bergingstoezicht", "anchor/jutterswet-erflijn", "anchor/jutterswet-hardheid",
                "jutterswet-schouwkalender");
        Set<String> covered = new TreeSet<>();
        for (String golden : goldens) {
            for (String kind : carrierKinds(golden)) {
                if (!covered.add(kind)) continue;
                String message = refusal(() -> loadWithTwistedCarrier(golden, kind, "+012026-01-01"),
                        "+012026-01-01", "a malformed bound on a " + kind);
                assertTrue(message.contains(LEXEME_FORM),
                        kind + " — the refusal states the form it expected: " + message);
            }
        }
        assertEquals(Set.of("PrioritizedDefaults", "DayKindDefinition", "Allocation", "Consistency", "FactCreation",
                        "ObjectCreation", "Rule", "Uniqueness", "Uniqueness (union)"),
                covered, "every kind of carrier a validity window stands on is read through the clamp");
    }

    // ---------------- helpers ----------------

    private static JsonNode wire(String golden) throws Exception {
        Path p = CORPUS.resolve(golden).resolve("ir.json");
        assertTrue(Files.exists(p), "golden ir.json present: " + p.toAbsolutePath());
        return JSON.readTree(Files.readString(p));
    }

    private static RegelSpraakModel load(String golden) throws Exception {
        return IrLoader.load(wire(golden));
    }

    /** The window of the first rule version of {@link #WINDOW} — the one the twists land on. */
    private static Geldigheid window(RegelSpraakModel m) {
        return m.resolvedRegels.get(0).geldigheid();
    }

    /** {@link #WINDOW} with one bound of its first rule version replaced, loaded. */
    private static Geldigheid boundRead(String bound, String lexeme) throws Exception {
        JsonNode root = wire(WINDOW);
        JsonNode g = IrPad.versie(root, "rules", 0, 0).path("validity");
        assertTrue(g.isObject(), "the twisted rule version carries a Validity node");
        ((ObjectNode) g).put(bound, lexeme);
        return window(IrLoader.load(root));
    }

    private static LocalDate fromRead(String lexeme) throws Exception {
        return boundRead("from", lexeme).vanaf();
    }

    private static LocalDate toRead(String lexeme) throws Exception {
        return boundRead("to", lexeme).tot();
    }

    /** The kinds of carrier in {@code golden} a validity window stands on, in document order: the versions of a
     *  named form under the form's node kind (a uniqueness version with two or more sources as its union), and a
     *  node outside the named forms that carries its own {@code validity} under its own. */
    private static List<String> carrierKinds(String golden) throws Exception {
        List<String> out = new ArrayList<>();
        collect(wire(golden), out);
        return out;
    }

    private static void collect(JsonNode n, List<String> out) {
        if (n.isObject()) {
            JsonNode g = n.get("validity");
            if (n.has("node") && g != null && g.isObject() && "Validity".equals(g.path("node").asText()))
                out.add(n.path("node").asText());
            if (n.path("versions").isArray())
                for (JsonNode v : n.get("versions"))
                    if (v.path("validity").isObject()) out.add(carrier(n, v));
            n.forEach(child -> collect(child, out));
        } else if (n.isArray()) {
            n.forEach(child -> collect(child, out));
        }
    }

    /** The carrier kind of version {@code v} of form {@code form}. */
    private static String carrier(JsonNode form, JsonNode v) {
        String kind = form.path("node").asText();
        return kind.equals("Uniqueness") && v.path("sources").size() >= 2 ? "Uniqueness (union)" : kind;
    }

    /** {@code golden} with the FIRST validity window on a {@code kind} node given {@code lexeme} as its
     *  lower bound, loaded — the per-node-kind reach probe. */
    private static Object loadWithTwistedCarrier(String golden, String kind, String lexeme) throws Exception {
        JsonNode root = wire(golden);
        assertTrue(twist(root, kind, lexeme), golden + ": carries a " + kind + " with a validity window");
        return IrLoader.load(root);
    }

    private static boolean twist(JsonNode n, String kind, String lexeme) {
        if (n.isObject()) {
            JsonNode g = n.get("validity");
            if (n.has("node") && g != null && g.isObject() && "Validity".equals(g.path("node").asText())
                    && kind.equals(n.path("node").asText())) {
                ((ObjectNode) g).put("from", lexeme);
                return true;
            }
            if (n.path("versions").isArray())
                for (JsonNode v : n.get("versions"))
                    if (v.path("validity").isObject() && kind.equals(carrier(n, v))) {
                        ((ObjectNode) v.get("validity")).put("from", lexeme);
                        return true;
                    }
        }
        for (JsonNode child : n) if (twist(child, kind, lexeme)) return true;
        return false;
    }

    /** The refusal message {@code lexeme} must raise — or an assertion failure naming what the reader did
     *  instead: the value it accepted, the bare exception it let through, or the "not carried" Refusal it
     *  filed. */
    private static String refusal(ThrowingRead read, String lexeme, String why) throws Exception {
        Object readValue;
        try {
            readValue = read.read();
        } catch (IllegalArgumentException refusal) {
            String message = refusal.getMessage();
            assertTrue(message != null && message.contains("'" + lexeme + "'"),
                    why + " — the refusal quotes the value it refused: " + message);
            return message;
        } catch (IrLoader.Refusal notCarried) {
            throw new AssertionError(why + " — but the loader REFUSED it as a form it does not carry ("
                    + notCarried.getMessage() + "), which files a malformed document under the differential"
                    + " gate's auto-excluded channel and leaves the gate green over a document nobody read");
        } catch (RuntimeException otherException) {
            throw new AssertionError(why + " — but the reader let it through to a bare "
                    + otherException.getClass().getName() + ": " + otherException.getMessage());
        }
        throw new AssertionError(why + " — but the reader ACCEPTED it and read " + readValue);
    }

    private interface ThrowingRead { Object read() throws Exception; }

    /** Refused by the pattern, with the expected FORM in the message and the calendar left out of it. The
     *  negative half is load-bearing: without it a form branch that ALSO carries the calendar wording passes
     *  every case here, and the two branches this reader deliberately splits become indistinguishable to a
     *  producer reading the message. */
    private static void refusedOnForm(String lexeme, String why) throws Exception {
        String message = refusal(() -> fromRead(lexeme), lexeme, why);
        assertTrue(message.contains(LEXEME_FORM),
                why + " — a refusal a producer can act on states the form it expected: " + message);
        assertFalse(message.contains(CALENDAR),
                why + " — and does not also blame the calendar, which was never read here: " + message);
    }

    /** Refused by the calendar, with the calendar in the message and the field widths left alone. */
    private static void refusedOnCalendar(String lexeme, String why) throws Exception {
        String message = refusal(() -> fromRead(lexeme), lexeme, why);
        assertTrue(message.contains(CALENDAR),
                why + " — the refusal names the calendar: " + message);
        assertFalse(message.contains(LEXEME_FORM),
                why + " — and does not blame the field widths, which are right here: " + message);
    }
}
