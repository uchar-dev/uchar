package dev.uchar.regelspraak.transpiler.tms;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * A date construction whose parts lie outside the int range faults with the kind ch. 3.8 gives it, as the reference
 * interpreter does: a year outside the years of the representable range is EF-007, tested first; a month, day or clock
 * part outside its range, however far, is EF-032 with the parts as its parameters.
 *
 * <p>Red before: {@code dateTimeWith} with the year 3000000000 converted the year to an int before it tested the range
 * and broke the fire with an {@code ArithmeticException: Overflow}; a month, day or hour beyond an int faulted without a
 * code, "the part 5000000000 is no whole number within the range of a date part".
 */
class DateConstructionRangeWitnessTest {

    private static BigDecimal n(String s) {
        return new BigDecimal(s);
    }

    @Test void a_date_time_year_beyond_an_int_is_ef007() {
        ComputeFault f = assertThrows(ComputeFault.class, () -> ComputeHelpers.dateTimeWith("dateTimeWith",
                n("3000000000"), n("1"), n("1"), n("0"), n("0"), n("0"), n("0")));
        assertEquals("EF-007", f.code(), f.getMessage());
    }

    @Test void a_date_year_beyond_an_int_is_ef007() {
        ComputeFault f = assertThrows(ComputeFault.class, () -> ComputeHelpers.dateWith("dateWith", n("3000000000"),
                n("1"), n("1")));
        assertEquals("EF-007", f.code(), f.getMessage());
    }

    /** A month beyond an int names no calendar day: EF-032 with the parts as its parameters. */
    @Test void a_month_beyond_an_int_is_ef032_with_its_parts() {
        ComputeFault f = assertThrows(ComputeFault.class, () -> ComputeHelpers.dateWith("dateWith", n("2026"),
                n("5000000000"), n("1")));
        assertEquals("EF-032", f.code(), f.getMessage());
        assertEquals(Map.of("year", "2026", "month", "5000000000", "day", "1"), f.params());
    }

    /** A day beyond an int in a date-time, the same. */
    @Test void a_day_beyond_an_int_is_ef032_with_its_parts() {
        ComputeFault f = assertThrows(ComputeFault.class, () -> ComputeHelpers.dateTimeWith("dateTimeWith", n("2026"),
                n("3"), n("-5000000000"), n("0"), n("0"), n("0"), n("0")));
        assertEquals("EF-032", f.code(), f.getMessage());
        assertEquals(Map.of("year", "2026", "month", "3", "day", "-5000000000"), f.params());
    }

    /** An hour beyond an int names no clock time: EF-032 with the clock parts. */
    @Test void an_hour_beyond_an_int_is_ef032_with_its_parts() {
        ComputeFault f = assertThrows(ComputeFault.class, () -> ComputeHelpers.dateTimeWith("dateTimeWith", n("2026"),
                n("3"), n("1"), n("5000000000"), n("0"), n("0"), n("0")));
        assertEquals("EF-032", f.code(), f.getMessage());
        assertEquals(Map.of("hour", "5000000000", "minute", "0", "second", "0", "millisecond", "0"), f.params());
    }

    /** Parts within an int keep their date. */
    @Test void parts_within_range_make_their_moment() {
        assertEquals(java.time.LocalDate.of(2026, 3, 1), ComputeHelpers.dateWith("dateWith", n("2026"), n("3"), n("1")));
        assertEquals(java.time.LocalDateTime.of(2026, 12, 31, 23, 59, 59, 999_000_000), ComputeHelpers.dateTimeWith(
                "dateTimeWith", n("2026"), n("12"), n("31"), n("23"), n("59"), n("59"), n("999")));
    }
}
