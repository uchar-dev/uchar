package dev.uchar.regelspraak.transpiler.tms;

import dev.uchar.regelspraak.runtime.Breuk;
import dev.uchar.regelspraak.runtime.RegelSpraakRuntime;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The periods of a fixed start point's grid the temporal total measures a segment on (RUNTIME-SEMANTICS ch. 3.2 job 1,
 * ch. 3.6): a day counts 1/len of the period of the grid that holds it, and the walls of a grid laid from a month end
 * are clamped (ch. 3.8), so a period between clamped walls is one whole unit, as the reference interpreter and the
 * ASP backend weigh it: [2024-01-31, 2024-02-29) is the month the fixed start point {@code --01-31} lays, and it is
 * one month. A part of such a period counts its days over the days of the period
 * (spec/proposals/IMPLICIT-TIME.md 12a), and the calendar's grid lays other walls.
 *
 * <p><b>Red before the change</b> (measured 2026-11-04 on a956aaa45, card total-over-partial-unit step B): the runtime
 * counted the whole months from the start of a segment, whatever its grid, so a part of a period added 0, the 28 days
 * of [2024-01-31, 2024-02-28) and the 364 days of [2024-02-29, 2025-02-27) among them.
 *
 * <p>The duration keeps its own rule: "tijdsduur van 31-01-2024 tot 29-02-2024" is 0 whole months (ch. 3.8,
 * "Clamp, not inverse"), and the last test holds the two apart.
 */
class TimeTotalClampWitnessTest {

    @Test void a_month_between_clamped_walls_weighs_a_month() {
        assertEquals(exact("10"), total("MAAND", "--01-31", "2024-01-31", "2024-02-29", "10"),
                "the --01-31 month grid's February 2024 period");
        assertEquals(exact("10"), total("MAAND", "--01-31", "2025-01-31", "2025-02-28", "10"),
                "the same period in a year without 29 February");
        assertEquals(exact("10"), total("MAAND", "--01-31", "2024-04-30", "2024-05-31", "10"),
                "a period starting on a clamped wall and ending on the 31st");
    }

    @Test void a_quarter_and_a_year_between_clamped_walls_weigh_a_unit() {
        assertEquals(exact("30"), total("KWARTAAL", "--05-31", "2024-11-30", "2025-02-28", "30"),
                "the --05-31 quarter grid's period around the turn of the year");
        assertEquals(exact("100"), total("JAAR", "--02-29", "2024-02-29", "2025-02-28", "100"),
                "the --02-29 year grid's period from the leap day");
    }

    @Test void a_part_of_a_period_counts_its_days() {
        assertEquals(q(280, 29), total("MAAND", "--01-31", "2024-01-31", "2024-02-28", "10"),
                "28 of the 29 days of the --01-31 grid's February 2024 period");
        assertEquals(q(7280, 73), total("JAAR", "--02-29", "2024-02-29", "2025-02-27", "100"),
                "364 of the 365 days of the --02-29 grid's period from the leap day");
        assertEquals(exact("20"), total("MAAND", "--01-31", "2024-01-31", "2024-03-31", "10"),
                "two periods of the --01-31 grid");
        assertEquals(q(8970, 899), total("MAAND", null, "2024-01-31", "2024-02-29", "10"),
                "the calendar's grid: one day of January and 28 of the 29 days of February 2024");
    }

    @Test void the_duration_keeps_the_rule_of_the_taken_month() {
        assertEquals(BigDecimal.ZERO, RegelSpraakRuntime.rsTijdsduur(LocalDate.parse("2024-01-31"),
                LocalDate.parse("2024-02-29"), "MONTHS", false), "ch. 3.8: whole-unit counting is not the inverse"
                + " of date-plus-unit arithmetic");
        assertEquals(exact("10"), total("MAAND", "--01-31", "2024-01-31", "2024-02-29", "10"),
                "the temporal total over the same two dates");
    }

    private static Breuk total(String granularity, String fixed, String from, String to, String value) {
        return Breuk.van(PeriodArithmetic.weighed(List.of(new Period(LocalDate.parse(from), LocalDate.parse(to),
                new BigDecimal(value))), granularity, fixed, null));
    }

    private static Breuk exact(String value) {
        return Breuk.van(new BigDecimal(value));
    }

    private static Breuk q(long num, long den) {
        return Breuk.van(BigInteger.valueOf(num), BigInteger.valueOf(den));
    }
}
