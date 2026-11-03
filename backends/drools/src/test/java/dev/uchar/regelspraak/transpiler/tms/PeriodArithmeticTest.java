package dev.uchar.regelspraak.transpiler.tms;

import dev.uchar.regelspraak.runtime.Breuk;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * The functions of the period arithmetic per day ({@link PeriodArithmetic}) on the worked examples of RUNTIME-SEMANTICS
 * ch. 3.4 and 3.6, the values the reference interpreter's tests pin: every operator of ch. 1.2 but the power per day
 * with its own leeg rule ({@code WV-3.4-per-dag}), the deposit of a division heard once per code, the exact values
 * between two operators, and the calendar between the day family and the month family of time units
 * ({@code WV-3.6-g-vreemde-noemer}, {@code WV-3.6-kalender-week-over-maanden}, {@code -jaargrens}, {@code -maand},
 * {@code -kwartaal}, {@code WV-3.6-kalender}, {@code WV-3.6-kalender-geval-1}).
 *
 * <p><b>Red</b> before the change: the functions of the operators but plus and min, of the parameter and of the
 * calendar did not exist.
 */
class PeriodArithmeticTest {

    private static final LocalDate MIN = Period.OPEN_FROM, MAX = Period.OPEN_TO;

    private static LocalDate d(String s) {
        return LocalDate.parse(s);
    }

    private static Period p(LocalDate from, LocalDate to, String value) {
        return new Period(from, to, new BigDecimal(value));
    }

    private static Period p(String from, String to, String value) {
        return p(d(from), d(to), value);
    }

    // a = 100 on [2024-01-01, 2024-07-01), b = 30 on [2024-04-01, 2024-10-01): the first example of ch. 3.4
    private static final List<Period> A = List.of(p("2024-01-01", "2024-07-01", "100"));
    private static final List<Period> B = List.of(p("2024-04-01", "2024-10-01", "30"));

    @Test void every_operator_per_day_with_its_own_leeg_rule() {
        assertEquals(List.of(p(MIN, d("2024-04-01"), "0"), p("2024-04-01", "2024-07-01", "3000"), p(d("2024-07-01"), MAX, "0")),
                PeriodArithmetic.written(PeriodArithmetic.times(A, B), null, "a maal b"));
        // verminderd met propagates a leeg left operand: a gap before 2024-01-01 and from 2024-07-01
        assertEquals(List.of(p("2024-01-01", "2024-04-01", "100"), p("2024-04-01", "2024-07-01", "70")),
                PeriodArithmetic.written(PeriodArithmetic.reduced(A, B), null, "a verminderd met b"));
        assertEquals(List.of(p(MIN, d("2024-01-01"), "0"), p("2024-01-01", "2024-07-01", "10"), p(d("2024-07-01"), MAX, "0")),
                PeriodArithmetic.written(PeriodArithmetic.percentageOf(PeriodArithmetic.scalar(new BigDecimal("10")), A),
                        null, "10% van a"));
        Object flag = ComputeHelpers.depositFlag();
        assertEquals(List.of(p(MIN, d("2024-01-01"), "0"), p("2024-04-01", "2024-07-01", "3.33333"), p(d("2024-07-01"), MAX, "0")),
                PeriodArithmetic.written(PeriodArithmetic.dividedAbs(A, B, flag), 5, "a gedeeld door (ABS) b"));
        assertEquals(List.of("EF-031"), ComputeHelpers.depositCodes(flag));
    }

    /** The quotient 10/3 of {@code a gedeeld door b} stays exact up to the next operator: times 3 it is 10. */
    @Test void the_values_between_two_operators_are_exact() {
        Object flag = ComputeHelpers.depositFlag();
        List<?> quotient = PeriodArithmetic.divided(A, B, flag);
        assertEquals(List.of(p(MIN, d("2024-04-01"), "0"), p("2024-04-01", "2024-07-01", "10"), p(d("2024-07-01"), MAX, "0")),
                PeriodArithmetic.written(PeriodArithmetic.times(quotient, PeriodArithmetic.scalar(new BigDecimal("3"))),
                        0, "(a gedeeld door b) maal 3"));
        assertEquals(List.of("EF-031"), ComputeHelpers.depositCodes(flag));
        // a segment of this branch holds a decimal, so 10/3 written into the unbounded form faults by name
        ComputeFault f = assertThrows(ComputeFault.class, () -> PeriodArithmetic.written(quotient, null, "verhouding"));
        assertEquals("the period arithmetic of ch. 3.4 writes 10/3 on [2024-04-01, 2024-07-01) into the timeline of"
                + " \"verhouding\", of the unbounded form: the value does not terminate, and a segment of this branch"
                + " holds a decimal (a limit of this branch, not of the document)", f.sentence());
        assertEquals(null, f.code());
    }

    /** A divisor leeg in one period and 0 in another: both periods are leeg, and the probe hears each code once,
     *  however many periods divide so (ch. 3.4, 5.6). */
    @Test void a_division_by_leeg_and_by_zero_is_leeg_there_and_heard_once_per_code() {
        List<Period> divisor = List.of(p("2024-02-01", "2024-03-01", "0"), p("2024-04-01", "2024-05-01", "0"),
                p("2024-05-01", "2024-06-01", "4"));
        Object flag = ComputeHelpers.depositFlag();
        assertEquals(List.of(p(MIN, d("2024-01-01"), "0"), p("2024-05-01", "2024-06-01", "25"), p(d("2024-07-01"), MAX, "0")),
                PeriodArithmetic.written(PeriodArithmetic.divided(A, divisor, flag), 0, "a gedeeld door c"));
        assertEquals(List.of("EF-005", "EF-031"), ComputeHelpers.depositCodes(flag));
    }

    @Test void an_unbound_time_dependent_parameter_faults_in_the_words_of_the_reference_interpreter() {
        ComputeFault f = assertThrows(ComputeFault.class, () -> PeriodArithmetic.parameter(null, "rentevoet"));
        assertEquals("parameter \"rentevoet\" is unbound in this case (binding is fail-fast, ch. 5.7)", f.sentence());
        assertEquals("EF-014", f.code());
        assertEquals(java.util.Map.of("parameter", "rentevoet"), f.params());
        assertEquals(A, PeriodArithmetic.parameter(A, "rentevoet"));
    }

    private static Breuk q(long num, long den) {
        return Breuk.van(BigInteger.valueOf(num), BigInteger.valueOf(den));
    }

    private static Object exact(Object v) {
        return Breuk.van(v);
    }

    @Test void the_calendar_integrates_between_the_day_and_the_month_family() {
        // 10 €/dg over 2024 on a month grid: the twelve months hold 366 days
        assertEquals(exact(new BigDecimal("3660")), exact(PeriodArithmetic.weighed(List.of(p("2024-01-01", "2025-01-01", "10")),
                "MAAND", "DAG")));
        // 7 €/wk over February 2024 on a month grid: 29 days, 29/7 weeks
        assertEquals(exact(new BigDecimal("29")), exact(PeriodArithmetic.weighed(List.of(p("2024-02-01", "2024-03-01", "7")),
                "MAAND", "WEEK")));
        // 31 €/mnd over the 31 days of January and 91 €/kw over the 91 days of the first quarter of 2024 on a day grid
        assertEquals(exact(new BigDecimal("31")), exact(PeriodArithmetic.weighed(List.of(p("2024-01-01", "2024-02-01", "31")),
                "DAG", "MAAND")));
        assertEquals(exact(new BigDecimal("91")), exact(PeriodArithmetic.weighed(List.of(p("2024-01-01", "2024-04-01", "91")),
                "DAG", "KWARTAAL")));
        // 366 €/jr on a day grid across the turn of the year: 366/365 + 366/366
        assertEquals(q(731, 365), exact(PeriodArithmetic.weighed(List.of(p("2027-12-31", "2028-01-02", "366")), "DAG", "JAAR")));
        // the anchor: 500 €/jr on 54 days of the leap year 2028, and 400 €/jr on 77 days and 500 €/jr on 40 of 2025-2026
        assertEquals(q(4500, 61), exact(PeriodArithmetic.weighed(List.of(p("2028-02-11", "2028-04-05", "500")), "DAG", "JAAR")));
        assertEquals(q(10160, 73), exact(PeriodArithmetic.weighed(List.of(p("2025-11-16", "2026-02-01", "400"),
                p("2026-02-01", "2026-03-13", "500")), "DAG", "JAAR")));
        // a segment shorter than one unit of the grid adds 0, by the calendar too
        assertEquals(exact(BigDecimal.ZERO), exact(PeriodArithmetic.weighed(List.of(p("2024-01-10", "2024-01-20", "10")),
                "MAAND", "DAG")));
        // without a calendar the whole units of the grid
        assertEquals(exact(new BigDecimal("120")), exact(PeriodArithmetic.weighed(List.of(p("2024-01-01", "2025-01-01", "10")),
                "MAAND", null)));
    }

    @Test void the_total_by_the_calendar_adds_an_open_zero_and_faults_on_another_open_value() {
        List<Period> dagrente = List.of(p(MIN, d("2028-02-11"), "0"), p("2028-02-11", "2028-04-05", "500"),
                p(d("2028-04-05"), MAX, "0"));
        assertEquals(q(4500, 61), exact(PeriodArithmetic.total(dagrente, "DAG", "JAAR", "dagrente", "A1")));
        List<Period> open = List.of(p(MIN, d("2026-02-01"), "400"), p(d("2026-02-01"), MAX, "500"));
        assertThrows(OpenSegmentFault.class, () -> PeriodArithmetic.total(open, "DAG", "JAAR", "dagrente", "A1"));
    }
}
