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
 * between two operators, the calendar between the day family and the month family of time units
 * ({@code WV-3.6-g-vreemde-noemer}, {@code WV-3.6-kalender-week-over-maanden}, {@code -jaargrens}, {@code -maand},
 * {@code -kwartaal}, {@code WV-3.6-kalender}, {@code WV-3.6-kalender-geval-1}), a part of a unit of the grid by its
 * days ({@code WV-3.6-naar-rato}) and the total of an absent timeline.
 *
 * <p><b>Red</b> before the change: the functions of the operators but plus and min, of the parameter and of the
 * calendar did not exist; and the total counted whole units of the grid per segment, so a part of a unit added 0
 * (ALEF's case 0), the calendar measured only the whole units of the grid, and the subject's own total of an absent
 * input and the contribution of a member without a timeline were leeg.
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
                "MAAND", null, "DAG")));
        // 7 €/wk over February 2024 on a month grid: 29 days, 29/7 weeks
        assertEquals(exact(new BigDecimal("29")), exact(PeriodArithmetic.weighed(List.of(p("2024-02-01", "2024-03-01", "7")),
                "MAAND", null, "WEEK")));
        // 31 €/mnd over the 31 days of January and 91 €/kw over the 91 days of the first quarter of 2024 on a day grid
        assertEquals(exact(new BigDecimal("31")), exact(PeriodArithmetic.weighed(List.of(p("2024-01-01", "2024-02-01", "31")),
                "DAG", null, "MAAND")));
        assertEquals(exact(new BigDecimal("91")), exact(PeriodArithmetic.weighed(List.of(p("2024-01-01", "2024-04-01", "91")),
                "DAG", null, "KWARTAAL")));
        // 366 €/jr on a day grid across the turn of the year: 366/365 + 366/366
        assertEquals(q(731, 365), exact(PeriodArithmetic.weighed(List.of(p("2027-12-31", "2028-01-02", "366")), "DAG", null, "JAAR")));
        // the anchor: 500 €/jr on 54 days of the leap year 2028, and 400 €/jr on 77 days and 500 €/jr on 40 of 2025-2026
        assertEquals(q(4500, 61), exact(PeriodArithmetic.weighed(List.of(p("2028-02-11", "2028-04-05", "500")), "DAG", null, "JAAR")));
        assertEquals(q(10160, 73), exact(PeriodArithmetic.weighed(List.of(p("2025-11-16", "2026-02-01", "400"),
                p("2026-02-01", "2026-03-13", "500")), "DAG", null, "JAAR")));
        // the calendar measures the whole segment, also where it is shorter than one unit of the grid: 10 €/dg over
        // ten days of January on a month grid
        assertEquals(exact(new BigDecimal("100")), exact(PeriodArithmetic.weighed(List.of(p("2024-01-10", "2024-01-20", "10")),
                "MAAND", null, "DAG")));
        // without a calendar the length in units of the grid
        assertEquals(exact(new BigDecimal("120")), exact(PeriodArithmetic.weighed(List.of(p("2024-01-01", "2025-01-01", "10")),
                "MAAND", null, null)));
    }

    @Test void the_total_by_the_calendar_adds_an_open_zero_and_faults_on_another_open_value() {
        List<Period> dagrente = List.of(p(MIN, d("2028-02-11"), "0"), p("2028-02-11", "2028-04-05", "500"),
                p(d("2028-04-05"), MAX, "0"));
        assertEquals(q(4500, 61), exact(PeriodArithmetic.total(dagrente, "DAG", null, "JAAR", "dagrente", "A1")));
        List<Period> open = List.of(p(MIN, d("2026-02-01"), "400"), p(d("2026-02-01"), MAX, "500"));
        assertThrows(OpenSegmentFault.class, () -> PeriodArithmetic.total(open, "DAG", null, "JAAR", "dagrente", "A1"));
    }

    /** A part of a unit of the grid counts by its days ({@code WV-3.6-naar-rato}, spec/proposals/IMPLICIT-TIME.md 12a):
     *  each fragment over the days of the period of the grid that holds it, on the calendar or from a fixed start
     *  point, so two fragments that split one unit count that unit. */
    @Test void a_part_of_a_unit_counts_by_its_days() {
        // ALEF's own case: 400 €/jr until 2021-05-15 and 500 €/jr after, on a year grid, clipped to [2021-04-01, 2021-07-21)
        assertEquals(exact(new BigDecimal("140")), exact(PeriodArithmetic.weighed(List.of(p("2021-04-01", "2021-05-15", "400"),
                p("2021-05-15", "2021-07-21", "500")), "JAAR", null, null)));
        // 120 €/jr on five days of 2024
        assertEquals(q(100, 61), exact(PeriodArithmetic.weighed(List.of(p("2024-09-28", "2024-10-03", "120")), "JAAR", null,
                null)));
        // 100 €/mnd in January 2024 in two fragments, 14/31 and 17/31 of the month
        assertEquals(exact(new BigDecimal("100")), exact(PeriodArithmetic.weighed(List.of(p("2024-01-01", "2024-01-15", "100"),
                p("2024-01-15", "2024-02-01", "100")), "MAAND", null, null)));
        // 30 per month over [2024-04-16, 2024-06-01): half of April and all of May
        assertEquals(exact(new BigDecimal("45")), exact(PeriodArithmetic.weighed(List.of(p("2024-04-16", "2024-06-01", "30")),
                "MAAND", null, null)));
        // the storm year from 1 October: three days of the storm year that holds 29 February 2024 and two of the next
        assertEquals(q(6090, 4453), exact(PeriodArithmetic.weighed(List.of(p("2024-09-28", "2024-10-03", "100")), "JAAR",
                "--10-01", null)));
        // the week grid from 30 September 2024: five days of one week and two of the next
        assertEquals(exact(BigDecimal.ONE), exact(PeriodArithmetic.weighed(List.of(p("2024-10-02", "2024-10-09", "1")), "WEEK",
                "2024-09-30", null)));
        // a week of the calendar's grid counts seven days wherever it starts
        assertEquals(q(3, 7), exact(PeriodArithmetic.weighed(List.of(p("2024-01-03", "2024-01-06", "1")), "WEEK", null, null)));
    }

    /** An absent timeline totals 0 on every route (ch. 3.6, spec/proposals/IMPLICIT-TIME.md 12c), and so does one whose
     *  segments are all leeg. */
    @Test void an_absent_timeline_totals_zero() {
        assertEquals(exact(BigDecimal.ZERO), exact(PeriodArithmetic.total(null, "MAAND", null, null, "bedrag", "A1")));
        assertEquals(exact(BigDecimal.ZERO), exact(PeriodArithmetic.total(List.of(new Period(d("2024-01-01"), d("2024-03-01"),
                null)), "MAAND", null, null, "bedrag", "A1")));
        assertEquals(exact(BigDecimal.ZERO), exact(ComputeHelpers.ownTotal(null, "A1", d("2024-03-15"), "bedrag", "MAAND",
                null, null)));
        assertEquals(exact(BigDecimal.ZERO), exact(ComputeHelpers.contribution("vouw", "S1", "A1", null, "MAAND", null,
                false, false).value()));
    }
}
