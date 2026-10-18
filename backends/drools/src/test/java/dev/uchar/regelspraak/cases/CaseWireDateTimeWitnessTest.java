package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.ObjectMapper;

import dev.uchar.regelspraak.kern.Literalen;
import dev.uchar.regelspraak.runtime.RegelSpraakRuntime;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The REFUSING half of this backend's own case reader: which {@code dateTime} values
 * {@code CaseRunner}'s case-wire reader rejects, and what its refusal says. The reading half is
 * covered by the gates — gates 11 and 12 read 30 conforming {@code dateTime} values, so a reader that
 * became too strict turns them red. The refusing half had no cover at all: counted 2026-09-06, none of
 * those 30 values sits outside the lexeme, so the fence fires on no corpus case and removing it changes
 * no gate's verdict. That is what these cases hold.
 *
 * <p><b>The lexeme.</b> {@code uchar-case-v0} writes a {@code dateTime} as
 * {@code yyyy-mm-ddThh:mm:ss[.fff]}, matched WHOLE: fixed field widths, {@code T} as the separator, the
 * seconds field always written, no zone suffix and no offset, and a fraction that is optional but
 * exactly three digits (whole milliseconds). Anything else is refused HARD (an
 * {@link IllegalArgumentException}), never as a {@link CaseRunner.Excluded}: an exclusion would file a
 * malformed case under "not carried by the v0 backend" and leave the differential gate green over a
 * case nobody read.
 *
 * <p><b>Why the refusal must say WHICH half failed.</b> The reader answers in two branches, and the
 * cases below assert the branch as well as the refusal. A value whose FIELDS are wrong is told the form
 * it should have had; a value whose fields are right but whose calendar is not ({@code 2026-02-30},
 * hour 25) is told the calendar, and is NOT told the field widths, which are correct and would send its
 * producer looking in the wrong place.
 *
 * <p><b>What a missing fence costs, measured.</b> Before the fence landed (2026-09-06) this position
 * held {@code LocalDateTime.parse}, which accepts {@code …T12:00} without a seconds field and reads a
 * fraction of ANY width POSITIONALLY: {@code .1} as a hundred milliseconds, {@code .1234} as 123.4. The
 * shared interpreter/ASP case reader read those same digits as ONE millisecond and a hundred THOUSAND,
 * and the literal reader refused them outright — one wire, three answers, invisible because the schema
 * pattern kept every corpus value inside the agreeing region. The three fraction widths of case 6 are
 * exactly the values that drove them apart.
 *
 * <p>The model is the corpus golden {@code jutterswet-meldstip} read off the wire (its {@code
 * Aanmelding.meldstip} is a {@code DatumTijd} attribute); the cases are authored here, and no corpus
 * expectation is read or touched. The reads go through {@link CaseRunner#inputBeans}, the input layer
 * before any rule fires, so what the reader made of a value is the assertion itself.
 *
 * <p>The LITERAL reader ({@code Literalen.parseDatumTijd}) is a different reader with its own witness
 * ({@code DateTimeLexemeWitnessTest}). Until 2026-09-23 its grammar was WIDER than this one — it also
 * read the day-first source snippet — and the last case here pinned that asymmetry. Since card
 * {@code datumtijd-lezers-versmalling} both readers refuse the snippet, and the last case pins that
 * agreement on the one string that used to split them.
 */
class CaseWireDateTimeWitnessTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    /** cwd under surefire is the module dir (backends/drools); the corpus is two levels up. */
    private static final Path MELDSTIP = Path.of("..", "..", "corpus", "golden", "jutterswet-meldstip", "ir.json");

    /** The form branch of the refusal names the lexeme it expected. */
    private static final String LEXEME_FORM = "yyyy-mm-ddThh:mm:ss[.fff]";
    /** The calendar branch names the calendar instead — the fields were right. */
    private static final String CALENDAR = "the calendar is not";

    /** The RegelSpraak source snippet for 2026-03-15 12:00 — neither a literal lexeme (WF-095) nor a case value. */
    private static final String SOURCE_SNIPPET = "15-03-2026 12:00:00.000";

    // ---------------- the eight field classes: the pattern refuses, and names the form ----------------

    @Test void a_zone_suffix_is_refused() throws Exception {
        // uchar pins a LOCAL instant; there is no zone on this wire, so a Z is not a suffix to drop.
        refusesByForm("2026-03-15T12:00:00Z", "a zone suffix is not the case lexeme");
    }

    @Test void a_numeric_offset_is_refused() throws Exception {
        // same reason as the Z, and the shape a producer is most likely to send: an offset carries a
        // second instant beside the one written, and applying it would answer a different question.
        refusesByForm("2026-03-15T12:00:00+01:00", "a numeric offset is not the case lexeme");
    }

    @Test void a_space_in_place_of_the_T_is_refused() throws Exception {
        // the case wire writes T. A space is the separator of the day-first SOURCE snippet, which is a
        // literal spelling, not a case value (see the asymmetry case at the bottom).
        refusesByForm("2026-03-15 12:00:00", "a space is not the separator this wire writes");
    }

    @Test void a_narrow_date_field_is_refused() throws Exception {
        // fixed widths: a one-digit month or day is the source token's shape, not this wire's.
        refusesByForm("2026-3-15T09:00:00", "a one-digit month is not the case lexeme");
    }

    @Test void a_narrow_time_field_is_refused() throws Exception {
        refusesByForm("2026-03-15T9:00:00", "a one-digit hour is not the case lexeme");
    }

    @Test void a_fraction_of_another_breedte_than_three_is_refused() throws Exception {
        // the three widths on which the three readers gave three different instants: three digits are
        // milliseconds, and any other width is refused rather than read under one reader's convention.
        refusesByForm("2026-03-15T12:00:00.1",
                "a one-digit fraction was read as a hundred milliseconds here and as one elsewhere");
        refusesByForm("2026-03-15T12:00:00.1234", "a four-digit fraction was truncated to 123 ms");
        refusesByForm("2026-03-15T12:00:00.100000", "a six-digit fraction was read as a hundred ms here");
    }

    @Test void a_fraction_of_two_digits_is_refused() throws Exception {
        // Width TWO gets its own case because it is the one width the pattern's own quantifier can absorb
        // without any other case noticing: widening {3} to {2,3} leaves every assertion above green (the
        // one-, four- and six-digit fractions stay refused) while this reader starts reading .12 as 120
        // milliseconds — and the interpreter/ASP reader refuses it. That is the one-wire-three-answers class
        // this whole fence exists for, so the boundary is pinned from BOTH sides, not only from above.
        refusesByForm("2026-03-15T12:00:00.12", "a two-digit fraction is not the case lexeme either");
    }

    @Test void a_missing_seconds_field_is_refused() throws Exception {
        // accepted before the fence, as 12:00:00 — a guess that happens to be right only when the
        // producer meant the whole minute, and there is no way to know that it did.
        refusesByForm("2026-03-15T12:00", "the seconds field is always written");
    }

    @Test void surrounding_text_is_refused() throws Exception {
        // whole-string: the lexeme IS the value. A reader searching for a lexeme INSIDE the value would
        // answer here, and would answer whatever else the surrounding text meant to say.
        refusesByForm("rommel 2026-03-15T12:00:00.000 x", "the lexeme is the value, not a substring in it");
    }

    // ---------------- the two calendar forms: the widths pass, the date does not exist ----------------

    @Test void a_non_existing_day_is_refused_on_the_calendar() throws Exception {
        // every field has its width and its range looks plausible; February has no 30th.
        refusesByCalendar("2026-02-30T12:00:00.000", "a real month, not a real day");
    }

    @Test void an_hour_outside_the_clock_is_refused_on_the_calendar() throws Exception {
        refusesByCalendar("2026-03-15T25:00:00.000", "two digits, but not an hour of a day");
    }

    // ---------------- the counter-measurement: the canonical lexeme is READ ----------------

    @Test void the_canonical_form_with_and_without_fraction_is_read() throws Exception {
        // a fence that refuses too much is as wrong as one that refuses nothing, and the gates would
        // only catch it through 30 corpus values; both canonical shapes are asserted here directly.
        assertEquals(LocalDateTime.of(2026, 3, 15, 12, 0, 0), meldstip("2026-03-15T12:00:00"),
                "the fraction is optional: without it the value is the whole second");
        assertEquals(LocalDateTime.of(2026, 3, 15, 12, 0, 0, 500_000_000), meldstip("2026-03-15T12:00:00.500"),
                ".500 is five hundred milliseconds, scaled to nanoOfSecond");
        assertEquals(LocalDateTime.of(2026, 3, 15, 12, 0, 0, 1_000_000), meldstip("2026-03-15T12:00:00.001"),
                "three digits are milliseconds: .001 is ONE millisecond, the width the readers split on");
    }

    // ---------------- one string, refused on both wires ----------------

    @Test void the_source_snippet_is_no_literal_lexeme_and_no_case_value() throws Exception {
        // Until 2026-09-23 this case pinned an ASYMMETRY: the literal reader read the snippet as noon on
        // 15 March (WF-095 then admitted two spellings, because the emitter wrote the snippet into
        // `waarde`) while the case reader refused it. The emitter writes ISO since p5-datumtijd-lexeem
        // and WF-095 admits ISO alone, so the literal reader narrowed too. Both halves are asserted in
        // one case, so widening EITHER reader back to the snippet fails here.
        assertNull(Literalen.parseDatumTijd(SOURCE_SNIPPET), "the day-first snippet is not a literal lexeme");
        refusesByForm(SOURCE_SNIPPET, "and the same string is not a valid case value");
    }

    // ---------------- helpers ----------------

    /** The value the case reader made of {@code lexeme} on {@code Aanmelding.meldstip}, read back off the
     *  input bean before any rule fires. */
    private static Object meldstip(String lexeme) throws Exception {
        assertTrue(Files.exists(MELDSTIP), "golden ir.json present: " + MELDSTIP.toAbsolutePath());
        String caseDocument = """
                { "format": "uchar-case-v0", "asOfDate": null,
                  "input": { "instances": [
                      { "label": "A1", "objectType": "Aanmelding",
                        "attributes": { "meldstip": { "dateTime": "%s" } },
                        "roles": {}, "characteristics": [] } ],
                    "parameters": {} } }
                """.formatted(lexeme);
        Map<String, Object> beans = CaseRunner.inputBeans(MELDSTIP, JSON.readTree(caseDocument));
        return RegelSpraakRuntime.get(beans.get("A1"), "meldstip");
    }

    /** The refusal message {@code lexeme} must raise — or an assertion failure naming what the reader did
     *  instead: the value it accepted, the bare exception it let through, or the exclusion it filed. */
    private static String refusal(String lexeme, String why) throws Exception {
        Object read;
        try {
            read = meldstip(lexeme);
        } catch (IllegalArgumentException refusal) {
            String melding = refusal.getMessage();
            assertTrue(melding.contains("'" + lexeme + "'"),
                    why + " — the refusal quotes the value it refused: " + melding);
            assertTrue(melding.contains("'meldstip'"),
                    why + " — and names the field it arrived on: " + melding);
            return melding;
        } catch (CaseRunner.Excluded uitgesloten) {
            throw new AssertionError(why + " — but the reader EXCLUDED it (" + uitgesloten.getMessage()
                    + "), which files a malformed case under 'not carried' and leaves the gate green");
        } catch (RuntimeException other) {
            throw new AssertionError(why + " — but the reader let it through to a bare "
                    + other.getClass().getName() + ": " + other.getMessage());
        }
        throw new AssertionError(why + " — but the reader ACCEPTED it and read " + read);
    }

    /** Refused by the pattern, with the expected FORM in the message and the calendar left out of it. The
     *  negative half is the mirror of the one below, and it is load-bearing for the same reason: without it a
     *  form branch that ALSO carries the calendar wording passes every case here (measured), and the two
     *  branches this reader deliberately splits become indistinguishable to a producer reading the message. */
    private static void refusesByForm(String lexeme, String why) throws Exception {
        String melding = refusal(lexeme, why);
        assertTrue(melding.contains(LEXEME_FORM),
                why + " — a refusal a producer can act on states the form it expected: " + melding);
        assertFalse(melding.contains(CALENDAR),
                why + " — and does not also blame the calendar, which was never read here: " + melding);
    }

    /** Refused by the calendar, with the calendar in the message and the field widths left alone. */
    private static void refusesByCalendar(String lexeme, String why) throws Exception {
        String melding = refusal(lexeme, why);
        assertTrue(melding.contains(CALENDAR),
                why + " — the refusal names the calendar: " + melding);
        assertFalse(melding.contains(LEXEME_FORM),
                why + " — and does not blame the field widths, which are right here: " + melding);
    }
}
