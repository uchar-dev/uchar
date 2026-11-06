package dev.uchar.regelspraak.runtime;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.Temporal;
import java.util.List;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The date step of RUNTIME-SEMANTICS ch. 3.8 at the edge of what a date holds: "A step whose result leaves the
 * representable millisecond range is a fault, never a silent wrap-around", with the worked example
 * "2024-01-01T00:00:00 ± 2 000 000 000 000 uur = fault, and + 60 000 000 000 dagen = fault". Both branches of
 * this backend step through {@code RegelSpraakRuntime.rsDatumReken}, so these rows hold that one function to the
 * reference interpreter, row for row. Every expectation is what {@code ref} answered on a RegelSpraak document with one
 * rule per unit and direction ("Het uurinstant vooruit van een Getijpost moet berekend worden als zijn meldtijdstip
 * plus zijn uurstap vooruit", and so on), one case per row, measured 2026-10-09: its value where it gave one, written
 * as the millisecond count it printed, and a fault where it faulted. The range is the reference interpreter's machine
 * integer, [-2^62, 2^62 - 1] ms around 1970-01-01T00:00:00, some 146 million years either way; ch. 3.8 names no number.
 *
 * <p>Where {@code ref} checks more than the result, the rows follow it. A step in a clock unit faults when the step
 * itself, n times the unit in milliseconds, leaves the range, even where the result would not: from 2024 that makes
 * "min 1 281 023 894 008 uur" a fault although the instant it names lies inside. A step in a calendar unit faults when
 * the midnight of the day it lands on leaves the range, even where the instant does not: "min 53 376 015 307 dagen"
 * from 09:00. Both are corners some 146 million years out; they are here so that a change on either side shows.
 *
 * <p>Four rows are corners where 63-bit arithmetic wraps: "plus 3 074 457 345 618 258 603 kwartalen" (three times
 * the count is 2^63 + 1), "min -4 611 686 018 427 387 904 milliseconden" (the negation of the smallest count is that
 * count again) and on a date without a time "plus 700 000 000 000 000 000 weken". ch. 3.8 calls a wrap-around a
 * fault, and {@code ref} and this function both fault there.
 *
 * <p>A date without a time has the range of its midnight (ch. 3.8, the owner's decision of 2026-10-09 on card
 * {@code datumrekening-slaat-stil-om}): the epoch days -53 375 995 583 to 53 375 995 583, -146136543-09-09 to
 * 146140482-04-24. A step on a date that lands outside faults in {@code ref}'s sentence for a date.
 *
 * <p>RED FIRST, measured on the claim's base (main 9b9a42ad), row by row: every row that expects a fault failed.
 * The count of 2^64 + 1 days answered 2024-01-02, because {@code BigDecimal.longValue} keeps the low 64 bits; the day
 * kind's walk read the same count as one step and answered 2024-12-25, and a count of 2^63 as no step at all,
 * 2024-01-01; the three ch. 3.8 examples answered +228160941-04-01T08:00, -228156894-10-02T16:00 and
 * +164276444-06-02T00:00; each step one unit past ref's bound answered a date one unit further on; and beyond
 * java.time's years the rows threw java.time's own {@code DateTimeException}. The 24 rows that expect a value passed
 * already, each equal to ref's answer: within the range the step was right.
 */
class DateStepRangeWitnessTest {

    /** The sentences, written out rather than read from the runtime: a drift away from the reference interpreter's
     *  words fails here. */
    private static final String RANGE = "datum-tijd arithmetic leaves the representable millisecond range";
    private static final String DATUM = "date arithmetic leaves the representable range of a date";

    private static final LocalDateTime T0 = LocalDateTime.of(2024, 1, 1, 0, 0);
    private static final LocalDate D0 = LocalDate.of(2024, 1, 1);

    /** The instant {@code ref} printed as {@code @<ms>ms}: milliseconds since 1970-01-01T00:00:00. */
    private static LocalDateTime ms(long ms) {
        return LocalDateTime.ofEpochSecond(Math.floorDiv(ms, 1000L), (int) Math.floorMod(ms, 1000L) * 1_000_000,
                ZoneOffset.UTC);
    }

    private static Temporal step(Temporal date, String itemCount, String unit, boolean optellen) {
        return RegelSpraakRuntime.rsDatumReken(date, new BigDecimal(itemCount), unit, optellen);
    }

    /** One row: {@code expected} is ref's answer, a date or date-time, or null where ref faulted with {@code sentence}. */
    private record Row(String description, Temporal start, String itemCount, String unit, boolean optellen,
                       Temporal expected, String sentence) {}

    private static Row value(String description, Temporal start, String itemCount, String unit, boolean optellen,
                              Temporal expected) {
        return new Row(description, start, itemCount, unit, optellen, expected, null);
    }

    private static Row fault(String description, Temporal start, String itemCount, String unit, boolean optellen,
                            String sentence) {
        return new Row(description, start, itemCount, unit, optellen, null, sentence);
    }

    private static void play(List<Row> rows) {
        for (Row r : rows) {
            if (r.sentence() == null) {
                assertEquals(r.expected(), step(r.start(), r.itemCount(), r.unit(), r.optellen()), r.description());
            } else {
                IllegalStateException e = assertThrows(IllegalStateException.class,
                        () -> step(r.start(), r.itemCount(), r.unit(), r.optellen()), r.description());
                assertTrue(e.getMessage().contains(r.sentence()), r.description() + ": " + e.getMessage());
            }
        }
    }

    @Test void the_examples_of_chapter_3_8_are_a_fault() {
        play(List.of(
                fault("2024-01-01T00:00:00 plus 2 000 000 000 000 uur", T0, "2000000000000", "HOURS", true, RANGE),
                fault("2024-01-01T00:00:00 min 2 000 000 000 000 uur", T0, "2000000000000", "HOURS", false, RANGE),
                fault("2024-01-01T00:00:00 plus 60 000 000 000 dagen", T0, "60000000000", "DAYS", true, RANGE)));
    }

    @Test void a_count_outside_the_range_does_not_turn_over() {
        String count = "duration count 18446744073709551617 is out of range";
        play(List.of(
                fault("2^64 + 1 dagen op een datum-tijd", T0, "18446744073709551617", "DAYS", true, count),
                fault("2^64 + 1 dagen op een datum", D0, "18446744073709551617", "DAYS", true, count),
                fault("2^64 + 1 dagen eraf", D0, "18446744073709551617", "DAYS", false, count),
                fault("2^62 milliseconden eraf: ref's telling, niet het bereik", T0, "4611686018427387904", "MILLIS",
                        false, "duration count 4611686018427387904 is out of range")));
        // The day kind's walk read the count the same way: 2^64 + 1 as one step, and 2^63 as Long.MIN_VALUE, whose
        // negation is itself, so the walk did not start and returned the date unchanged.
        Predicate<LocalDate> kerst = d -> d.getMonthValue() == 12 && (d.getDayOfMonth() == 25 || d.getDayOfMonth() == 26);
        IllegalStateException e = assertThrows(IllegalStateException.class, () -> RegelSpraakRuntime.rsDatumPlusDagsoort(
                D0, new BigDecimal("18446744073709551617"), kerst, true), "2^64 + 1 kerstdagen");
        assertTrue(e.getMessage().contains(count), e.getMessage());
        e = assertThrows(IllegalStateException.class, () -> RegelSpraakRuntime.rsDatumPlusDagsoort(
                D0, new BigDecimal("9223372036854775808"), kerst, true), "2^63 kerstdagen");
        assertTrue(e.getMessage().contains("duration count 9223372036854775808 is out of range"), e.getMessage());
    }

    @Test void the_bound_in_clock_units_is_that_of_ref() {
        play(List.of(
                value("uur plus, het laatste", T0, "1281023420655", "HOURS", true, ms(4611686018425200000L)),
                fault("uur plus, een te ver", T0, "1281023420656", "HOURS", true, RANGE),
                value("uur min, de stap op de grens", T0, "1281023894007", "HOURS", false, ms(-4611684314358000000L)),
                fault("uur min, de stap buiten het bereik, het tijdstip erbinnen", T0, "1281023894008", "HOURS", false,
                        RANGE),
                value("minuut plus, het laatste", T0, "76861405239336", "MINUTES", true, ms(4611686018427360000L)),
                fault("minuut plus, een te ver", T0, "76861405239337", "MINUTES", true, RANGE),
                value("minuut min, de stap op de grens", T0, "76861433640456", "MINUTES", false,
                        ms(-4611684314360160000L)),
                fault("minuut min, de stap buiten het bereik", T0, "76861433640457", "MINUTES", false, RANGE),
                value("seconde plus, het laatste", T0, "4611684314360187", "SECONDS", true, ms(4611686018427387000L)),
                fault("seconde plus, een te ver", T0, "4611684314360188", "SECONDS", true, RANGE),
                value("seconde min, de stap op de grens", T0, "4611686018427387", "SECONDS", false,
                        ms(-4611684314360187000L)),
                fault("seconde min, de stap buiten het bereik", T0, "4611686018427388", "SECONDS", false, RANGE),
                value("milliseconde plus, het laatste: 2^62 - 1", T0, "4611684314360187903", "MILLIS", true,
                        ms(4611686018427387903L)),
                fault("milliseconde plus, een te ver", T0, "4611684314360187904", "MILLIS", true, RANGE),
                value("milliseconde min, het grootste aantal", T0, "4611686018427387903", "MILLIS", false,
                        ms(-4611684314360187903L)),
                fault("milliseconde min -2^62: ref keert het teken niet om en geeft een tijdstip", T0,
                        "-4611686018427387904", "MILLIS", false, RANGE)));
    }

    @Test void the_bound_in_calendar_units_is_that_of_ref() {
        play(List.of(
                value("dag plus, het laatste", T0, "53375975860", "DAYS", true, ms(4611686018371200000L)),
                fault("dag plus, een te ver", T0, "53375975861", "DAYS", true, RANGE),
                fault("dag plus vanaf 23:59:59.999: het tijdstip valt buiten", LocalDateTime.of(2024, 1, 1, 23, 59, 59,
                        999_000_000), "53375975860", "DAYS", true, RANGE),
                value("dag min, het laatste", T0, "53376015306", "DAYS", false, ms(-4611686018371200000L)),
                fault("dag min, een te ver", T0, "53376015307", "DAYS", false, RANGE),
                fault("dag min vanaf 09:00: het tijdstip valt erbinnen, de middernacht van zijn dag niet",
                        LocalDateTime.of(2024, 1, 1, 9, 0), "53376015307", "DAYS", false, RANGE),
                value("week plus, het laatste", T0, "7625139408", "WEEKS", true, ms(4611686018025600000L)),
                fault("week plus, een te ver", T0, "7625139409", "WEEKS", true, RANGE),
                value("week min, het laatste", T0, "7625145043", "WEEKS", false, ms(-4611686017939200000L)),
                fault("week min, een te ver", T0, "7625145044", "WEEKS", false, RANGE),
                fault("week plus 7 * 10^17", T0, "700000000000000000", "WEEKS", true, RANGE),
                value("maand plus, het laatste", T0, "1753661499", "MONTHS", true, ms(4611686016384000000L)),
                fault("maand plus, een te ver", T0, "1753661500", "MONTHS", true, RANGE),
                value("maand min, het laatste", T0, "1753662795", "MONTHS", false, ms(-4611686016470400000L)),
                fault("maand min, een te ver", T0, "1753662796", "MONTHS", false, RANGE),
                value("kwartaal plus, het laatste", T0, "584553833", "KWARTAAL", true, ms(4611686016384000000L)),
                fault("kwartaal plus, een te ver", T0, "584553834", "KWARTAAL", true, RANGE),
                value("kwartaal min, het laatste", T0, "584554265", "KWARTAAL", false, ms(-4611686016470400000L)),
                fault("kwartaal min, een te ver", T0, "584554266", "KWARTAAL", false, RANGE),
                fault("kwartaal plus 3 074 457 345 618 258 603: ref houdt van drie keer dat aantal 1 maand over", T0,
                        "3074457345618258603", "KWARTAAL", true, RANGE),
                value("jaar plus, het laatste", T0, "146138458", "YEARS", true, ms(4611686008608000000L)),
                fault("jaar plus, een te ver", T0, "146138459", "YEARS", true, RANGE),
                value("jaar min, het laatste", T0, "146138566", "YEARS", false, ms(-4611686008521600000L)),
                fault("jaar min, een te ver", T0, "146138567", "YEARS", false, RANGE),
                fault("jaar plus 5 * 10^17", T0, "500000000000000000", "YEARS", true, RANGE)));
    }

    @Test void a_date_without_time_has_the_range_of_its_midnight() {
        play(List.of(
                value("dag plus, het laatste", D0, "53375975860", "DAYS", true, LocalDate.of(146140482, 4, 24)),
                fault("dag plus, een te ver", D0, "53375975861", "DAYS", true, DATUM),
                value("dag min, het laatste", D0, "53376015306", "DAYS", false, LocalDate.of(-146136543, 9, 9)),
                fault("dag min, een te ver", D0, "53376015307", "DAYS", false, DATUM),
                fault("dag plus 60 000 000 000", D0, "60000000000", "DAYS", true, DATUM),
                fault("dag min 60 000 000 000", D0, "60000000000", "DAYS", false, DATUM),
                value("maand plus tot de laatste dag", LocalDate.of(2024, 1, 24), "1753661499", "MONTHS", true,
                        LocalDate.of(146140482, 4, 24)),
                fault("maand plus, een dag te ver", LocalDate.of(2024, 1, 25), "1753661499", "MONTHS", true, DATUM),
                fault("maand plus 2 000 000 000", D0, "2000000000", "MONTHS", true, DATUM),
                value("jaar plus, het laatste", D0, "146138458", "YEARS", true, LocalDate.of(146140482, 1, 1)),
                fault("jaar plus, een te ver", D0, "146138459", "YEARS", true, DATUM),
                fault("jaar plus 999 997 975", D0, "999997975", "YEARS", true, DATUM),
                fault("week plus 7 * 10^17", D0, "700000000000000000", "WEEKS", true, DATUM),
                fault("kwartaal plus 3 074 457 345 618 258 603", D0, "3074457345618258603", "KWARTAAL", true, DATUM)));
    }

    /** The construction and the Easter Sunday of a date hold to the same range (ch. 3.8): the last day and the
     *  Easter Sunday of the last year lie inside, a day further and a year beyond do not. */
    @Test void a_constructed_date_has_the_range_of_its_midnight() {
        BigDecimal j = new BigDecimal("146140482");
        assertEquals(LocalDate.of(146140482, 4, 24), RegelSpraakRuntime.rsDatumMet(j, BigDecimal.valueOf(4),
                BigDecimal.valueOf(24)));
        IllegalStateException e = assertThrows(IllegalStateException.class, () -> RegelSpraakRuntime.rsDatumMet(j,
                BigDecimal.valueOf(4), BigDecimal.valueOf(25)), "146140482-04-25");
        assertTrue(e.getMessage().contains(DATUM), e.getMessage());
        e = assertThrows(IllegalStateException.class, () -> RegelSpraakRuntime.rsDatumMet(new BigDecimal("200000000"),
                BigDecimal.ONE, BigDecimal.ONE), "jaar 200 000 000");
        assertTrue(e.getMessage().contains(DATUM), e.getMessage());
        assertEquals(LocalDate.of(146140482, 3, 29), RegelSpraakRuntime.rsEerstePaasdag(j));
        e = assertThrows(IllegalStateException.class, () -> RegelSpraakRuntime.rsEerstePaasdag(
                new BigDecimal("4294969320")), "paasjaar 4 294 969 320: intValue hield daarvan 2024");
        assertTrue(e.getMessage().contains(DATUM), e.getMessage());
    }

    /**
     * The day kind's step on a datum-tijd ({@code rsDatumTijdPlusDagsoort}) walks the civil days and puts the time of
     * day back, and the day it lands on must lie in the range as at every other calendar step: the reference
     * interpreter makes the same test after its walk. One walk is a few days, but its start can already lie at the
     * edge, computed by another rule. The rows are a composed step, measured 2026-10-09 in {@code ref} on "Het verre
     * tijdstip … als zijn meldtijdstip plus zijn wachtdagen" and "Het sluittijdstip … als zijn verre tijdstip plus zijn
     * wachttermijn" (springtijdagen, the 1st and the 15th, as in {@code jutterswet-getijklok}), and its mirror with
     * {@code min}: 2024-01-01T00:00:00 plus 53 375 975 860 days is the last day of the range, and one springtijdag
     * further, 146140482-05-01, faults. A count of 0 keeps the test, as in {@code ref}; a leeg count returns the
     * datum-tijd untested, as there.
     *
     * <p>One row is a corner within a day of -2^62, one springtijdag back from -146136543-09-09T00:00, where a floor
     * division of the millisecond count that adds a day's length less one to the negated count passes 2^62 - 1; the
     * step the row describes leaves the range and faults, in {@code ref} as here.
     *
     * <p>RED FIRST, measured on this branch at 16905a6d: the step answered +146140482-05-01T00:00 and
     * -146136543-09-01T00:00 where these rows expect a fault; the rows that expect a value passed.
     */
    @Test void the_day_kind_step_on_a_date_time_tests_the_day_on_which_it_lands() {
        Predicate<LocalDate> springtij = d -> d.getDayOfMonth() == 1 || d.getDayOfMonth() == 15;
        LocalDateTime lastDay = (LocalDateTime) step(T0, "53375975860", "DAYS", true);
        assertEquals(ms(4611686018371200000L), lastDay, "2024-01-01T00:00:00 plus 53 375 975 860 dagen");
        IllegalStateException e = assertThrows(IllegalStateException.class, () -> RegelSpraakRuntime
                .rsDatumTijdPlusDagsoort(lastDay, BigDecimal.ONE, springtij, true), "dan plus 1 springtijdag");
        assertTrue(e.getMessage().contains(RANGE), e.getMessage());
        assertEquals(lastDay, RegelSpraakRuntime.rsDatumTijdPlusDagsoort(lastDay, BigDecimal.ZERO, springtij, true),
                "plus 0 springtijdagen: de dag zelf ligt in het bereik");
        assertEquals(lastDay, RegelSpraakRuntime.rsDatumTijdPlusDagsoort(lastDay, null, springtij, true),
                "plus een leeg aantal");
        assertEquals(ms(4611686017593600000L), RegelSpraakRuntime.rsDatumTijdPlusDagsoort(
                LocalDateTime.of(146140482, 4, 1, 0, 0), BigDecimal.ONE, springtij, true),
                "vanaf 146140482-04-01 plus 1 springtijdag: de 15e ligt er nog in");
        assertEquals(ms(-4611686017852800000L), RegelSpraakRuntime.rsDatumTijdPlusDagsoort(
                LocalDateTime.of(-146136543, 9, 20, 0, 0), BigDecimal.ONE, springtij, false),
                "vanaf -146136543-09-20 min 1 springtijdag: de 15e ligt er nog in");
        e = assertThrows(IllegalStateException.class, () -> RegelSpraakRuntime.rsDatumTijdPlusDagsoort(
                LocalDateTime.of(-146136543, 9, 9, 0, 0), BigDecimal.ONE, springtij, false),
                "vanaf de eerste dag van het bereik min 1 springtijdag");
        assertTrue(e.getMessage().contains(RANGE), e.getMessage());
    }
}
