package dev.uchar.regelspraak.transpiler;

import dev.uchar.regelspraak.runtime.RegelSpraakRuntime;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * The two day-kind duration statics, held to uchar's RUNTIME-SEMANTICS ch. 3.8 — the same worked
 * examples {@code ref/test} pins verbatim, so a divergence surfaces HERE and not only as a corpus
 * replay failure.
 *
 * <p>Until 2026-09-19 both statics were named by the two transpilers and by NOTHING else: no test,
 * no gate script, no corpus document in any zone. Golden {@code jutterswet-bergingsklok} now
 * exercises them through both branches, and this class holds the corners a green golden cannot —
 * the gap fence, the leap-century step, the leeg rules and the zero rule.
 */
class DayKindDurationTest {

    /** the chapter's own day kind: 25 or 26 December. */
    private static final Predicate<LocalDate> KERST =
            d -> d.getMonthValue() == 12 && (d.getDayOfMonth() == 25 || d.getDayOfMonth() == 26);

    private static final Predicate<LocalDate> NEVER = d -> false;

    private static LocalDate d(String iso) {
        return LocalDate.parse(iso);
    }

    private static LocalDate plus(String from, String n) {
        return RegelSpraakRuntime.rsDatumPlusDagsoort(d(from), new BigDecimal(n), KERST, true);
    }

    private static LocalDate min(String from, String n) {
        return RegelSpraakRuntime.rsDatumPlusDagsoort(d(from), new BigDecimal(n), KERST, false);
    }

    private static long count(String van, String to, boolean absolute) {
        return RegelSpraakRuntime.rsTijdsduurDagsoort(d(van), d(to), KERST, absolute).longValueExact();
    }

    @Test
    void count_is_start_exclusive_and_end_inclusive() {
        assertEquals(2, count("2024-12-20", "2024-12-31", false));
        assertEquals(1, count("2024-12-25", "2024-12-26", false)); // the 25th is the excluded start
        assertEquals(-2, count("2024-12-31", "2024-12-20", false));
        assertEquals(2, count("2024-12-31", "2024-12-20", true));
        assertEquals(0, count("2024-12-25", "2024-12-25", false));
    }

    @Test
    void count_over_an_unsatisfiable_day_kind_is_zero_and_no_fault() {
        assertEquals(
                0,
                RegelSpraakRuntime.rsTijdsduurDagsoort(d("2024-12-20"), d("2024-12-31"), NEVER, false)
                        .longValueExact());
    }

    @Test
    void count_propagates_empty_never_zero() {
        assertNull(RegelSpraakRuntime.rsTijdsduurDagsoort(null, d("2024-12-31"), KERST, false));
        assertNull(RegelSpraakRuntime.rsTijdsduurDagsoort(d("2024-12-20"), null, KERST, false));
    }

    @Test
    void step_never_counts_the_startdag_along() {
        assertEquals(d("2024-12-26"), plus("2024-12-20", "2"));
        assertEquals(d("2024-12-26"), plus("2024-12-25", "1")); // the start IS of the kind
        assertEquals(d("2025-12-25"), plus("2024-12-26", "1"));
        assertEquals(d("2024-12-26"), min("2024-12-27", "1"));
        assertEquals(d("2023-12-26"), min("2024-12-25", "1"));
    }

    @Test
    void step_from_zero_or_empty_gives_the_startdag_back() {
        assertEquals(d("2024-12-20"), plus("2024-12-20", "0"));
        assertNull(RegelSpraakRuntime.rsDatumPlusDagsoort(null, new BigDecimal("2"), KERST, true));
        assertEquals(
                d("2024-12-20"), RegelSpraakRuntime.rsDatumPlusDagsoort(d("2024-12-20"), null, KERST, true));
    }

    @Test
    void step_turns_over_at_a_negative_count_and_cuts_a_rational_off_to_zero() {
        assertEquals(d("2024-12-26"), plus("2024-12-27", "-1"));
        assertEquals(d("2024-12-26"), plus("2024-12-20", "2.9"));
    }

    @Test
    void step_falls_on_the_scope_and_does_not_run_endlessly() {
        IllegalStateException e =
                assertThrows(
                        IllegalStateException.class,
                        () ->
                                RegelSpraakRuntime.rsDatumPlusDagsoort(
                                        d("2024-12-20"), BigDecimal.ONE, NEVER, true));
        // the same fragment ref and clingo pin, and corpus/rood/dagsoort-onvervulbare-wandeling
        assertEquals(
                true,
                e.getMessage()
                        .contains("no day of the kind within " + RegelSpraakRuntime.DAGSOORT_REIKWIJDTE
                                + " calendar days"));
    }

    @Test
    void the_scope_lies_above_the_longest_gregorian_recurrence() {
        // the century-skipped leap day: 2096-02-29 -> 2104-02-29, 2 921 days, still steps. And the case the
        // reach was RAISED for (maintainer decision 2026-09-19): a year-bound day kind more than a decade out.
        Predicate<LocalDate> leapDay = x -> x.getMonthValue() == 2 && x.getDayOfMonth() == 29;
        LocalDate out =
                RegelSpraakRuntime.rsDatumPlusDagsoort(
                        d("2096-02-29"), BigDecimal.ONE, leapDay, true);
        assertEquals(d("2104-02-29"), out);
        assertEquals(2921, out.toEpochDay() - d("2096-02-29").toEpochDay());

        Predicate<LocalDate> in2040 =
                x -> x.getYear() == 2040 && (x.getDayOfMonth() == 1 || x.getDayOfMonth() == 15);
        assertEquals(
                d("2040-01-01"),
                RegelSpraakRuntime.rsDatumPlusDagsoort(d("2026-03-10"), BigDecimal.ONE, in2040, true));
    }

    @Test
    void date_time_steps_on_the_dagdeel_and_keeps_the_clock_time() {
        // maintainer decision 2026-09-19: the day kind is judged on the civil day part alone and the original
        // local clock time rides through unchanged; zero, leeg and negative follow the day-precision rules
        java.time.LocalDateTime t = java.time.LocalDateTime.parse("2024-12-20T09:15:30");
        assertEquals(
                java.time.LocalDateTime.parse("2024-12-26T09:15:30"),
                RegelSpraakRuntime.rsDatumTijdPlusDagsoort(t, new BigDecimal("2"), KERST, true));
        assertEquals(
                java.time.LocalDateTime.parse("2024-12-25T09:15:30"),
                RegelSpraakRuntime.rsDatumTijdPlusDagsoort(
                        java.time.LocalDateTime.parse("2024-12-26T09:15:30"), BigDecimal.ONE, KERST, false));
        assertEquals(t, RegelSpraakRuntime.rsDatumTijdPlusDagsoort(t, BigDecimal.ZERO, KERST, true));
        assertEquals(t, RegelSpraakRuntime.rsDatumTijdPlusDagsoort(t, null, KERST, true));
        assertNull(RegelSpraakRuntime.rsDatumTijdPlusDagsoort(null, BigDecimal.ONE, KERST, true));
    }
}
