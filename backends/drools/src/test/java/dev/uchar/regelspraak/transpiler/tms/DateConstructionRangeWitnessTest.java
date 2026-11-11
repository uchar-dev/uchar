package dev.uchar.regelspraak.transpiler.tms;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * A date construction whose parts lie outside the int range faults with the kind ch. 3.8 gives it, as the reference
 * interpreter does: a year outside the years of the representable range is EF-007, tested first.
 *
 * <p>Red before: {@code dateTimeWith} with the year 3000000000 converted the year to an int before it tested the range
 * and broke the fire with an {@code ArithmeticException: Overflow}.
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
}
