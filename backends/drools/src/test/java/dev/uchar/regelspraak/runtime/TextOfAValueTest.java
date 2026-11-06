package dev.uchar.regelspraak.runtime;

import dev.uchar.regelspraak.transpiler.tms.ConstructorValue;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * The table of RUNTIME-SEMANTICS ch. 1.2, "Text coercion", row for row, held to {@code RegelSpraakRuntime.rsTekst},
 * which both branches call for an inserted part of a text composition and an argument of a concatenation with the form
 * of its part; the reference interpreter holds the same rows in its own suite (WV-1.2c-text-*). A number reads the
 * decimals of the type of its part whatever scale the bean holds it at, and without a finite bound (form null) its
 * exact form, a rational the same as the decimal it equals.
 */
class TextOfAValueTest {

    private static String text(Object value) { return RegelSpraakRuntime.rsTekst(value, null, null); }

    private static String text(Object value, Object form) { return RegelSpraakRuntime.rsTekst(value, form, null); }

    @Test void every_row_of_the_table() {
        assertEquals("", text(null));
        assertEquals("", text(null, 2));
        assertEquals("wrakhout", text("wrakhout"));
        assertEquals("waar", text(Boolean.TRUE));
        assertEquals("onwaar", text(Boolean.FALSE));
        assertEquals("5,50", text(new BigDecimal("5.5"), 2));
        assertEquals("5,50", text(new BigDecimal("5.500"), 2));
        assertEquals("5,00", text(new BigDecimal("5"), 2));
        assertEquals("5", text(new BigDecimal("5.00"), 0));
        assertEquals("1250,50", text(new BigDecimal("1250.5"), 2));
        assertEquals("-0,50", text(new BigDecimal("-0.5"), 2));
        assertEquals("0,125", text(new BigDecimal("0.125"), 3));
        assertEquals("0,00", text(new BigDecimal("0"), 2));
        assertEquals("12,50", text(new BigDecimal("12.5"), 2));
        assertEquals("1250", text(new BigDecimal("1.25E+3"), 0));
        assertEquals("1,75", text(Breuk.van(BigInteger.valueOf(7), BigInteger.valueOf(4)), 2));
        assertEquals("5,5", text(new BigDecimal("5.50")));
        assertEquals("0,125", text(new BigDecimal("0.125")));
        assertEquals("0", text(new BigDecimal("0.00")));
        assertEquals("4/3", text(Breuk.van(BigInteger.valueOf(4), BigInteger.valueOf(3))));
        assertEquals("-1/3", text(Breuk.van(BigInteger.valueOf(-1), BigInteger.valueOf(3))));
        assertEquals("1,75", text(Breuk.van(BigInteger.valueOf(7), BigInteger.valueOf(4))));
        assertEquals("21-06-2026", text(LocalDate.of(2026, 6, 21)));
        assertEquals("31-12-0999", text(LocalDate.of(999, 12, 31)));
        assertEquals("01-03-+12026", text(LocalDate.of(12026, 3, 1)));
        assertEquals("21-06-2026 09:01:59.999", text(LocalDateTime.of(2026, 6, 21, 9, 1, 59, 999_000_000)));
        assertEquals("21-06-2026 09:01:59.000", text(LocalDateTime.of(2026, 6, 21, 9, 1, 59)));
        assertEquals("Afgewezen", text(new ConstructorValue("Afgewezen", null), List.of("Toegekend", 2, "Afgewezen")));
        assertEquals("Toegekend(1250,50)",
                text(new ConstructorValue("Toegekend", new BigDecimal("1250.5")), Arrays.asList("Toegekend", 2, "Afgewezen", null)));
    }

    /** A part that writes the unit of its number: the number, a space and the unit in parentheses, as the analysis
     *  spells it (IR-SPEC ch. 2.5); a leeg part writes "" without it, and a value that is no number has none
     *  (WF-129). */
    @Test void a_part_with_its_unit() {
        assertEquals("5,50 (EUR)", RegelSpraakRuntime.rsTekst(new BigDecimal("5.5"), 2, "EUR"));
        assertEquals("12,50 (%)", RegelSpraakRuntime.rsTekst(new BigDecimal("12.5"), 2, "%"));
        assertEquals("4/3 (EUR/jr)", RegelSpraakRuntime.rsTekst(Breuk.van(BigInteger.valueOf(4), BigInteger.valueOf(3)),
                null, "EUR/jr"));
        assertEquals("", RegelSpraakRuntime.rsTekst(null, 2, "EUR"));
        assertThrows(IllegalStateException.class, () -> RegelSpraakRuntime.rsTekst("wrakhout", null, "EUR"));
    }

    /** The bound of the part guarantees that the value fits its decimals; a value that does not is a broken
     *  guarantee, never rounded. */
    @Test void a_number_that_does_not_fit_the_decimals_of_its_part_fails() {
        assertThrows(IllegalStateException.class, () -> text(new BigDecimal("5.55"), 1));
    }

    @Test void a_list_or_a_record_has_no_text() {
        assertThrows(IllegalStateException.class, () -> text(List.of(new BigDecimal("12.5"), new BigDecimal("30"))));
        assertThrows(IllegalStateException.class, () -> text(Map.of("waarde", new BigDecimal("125.5"))));
    }
}
