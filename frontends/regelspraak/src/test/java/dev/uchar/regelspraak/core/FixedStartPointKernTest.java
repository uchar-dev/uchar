package dev.uchar.regelspraak.core;

import dev.uchar.regelspraak.model.RegelSpraakModel.Granulariteit;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The shared fixed start point (IR-SPEC ch. 2.8, WF-115; RUNTIME-SEMANTICS ch. 3.2): the chapter's worked examples on
 * the Java side, which the Drools case runner lays its grid with, and the WF-115 sentences, which the Drools loader
 * refuses in. The OCaml side pins the same examples in {@code ref/test} (WV-3.2-vast, WF-115-*); a drift between the
 * two is a drift between the executors.
 */
class FixedStartPointKernTest {

    private static LocalDate d(String iso) { return LocalDate.parse(iso); }

    private static void periode(Granulariteit g, String fixed, String day, String van, String until) {
        LocalDate[] p = FixedStartPoint.periode(g, fixed, d(day));
        assertEquals(d(van), p[0], "begin van de periode die " + day + " bevat");
        assertEquals(d(until), p[1], "einde van de periode die " + day + " bevat");
    }

    /** Consecutive walls: the period that starts at each wall ends at the next. */
    private static void walls(Granulariteit g, String fixed, String... walls) {
        for (int i = 0; i + 1 < walls.length; i++) periode(g, fixed, walls[i], walls[i], walls[i + 1]);
    }

    @Test void the_worked_examples_of_chapter_3_2() {
        periode(Granulariteit.JAAR, "--10-01", "2025-02-01", "2024-10-01", "2025-10-01");
        periode(Granulariteit.JAAR, "--10-01", "2024-09-30", "2023-10-01", "2024-10-01");
        walls(Granulariteit.MAAND, "--01-31", "2024-01-31", "2024-02-29", "2024-03-31", "2024-04-30");
        walls(Granulariteit.KWARTAAL, "--05-31", "2024-02-29", "2024-05-31", "2024-08-31", "2024-11-30", "2025-02-28");
        walls(Granulariteit.JAAR, "--02-29", "2024-02-29", "2025-02-28", "2026-02-28", "2027-02-28", "2028-02-29");
        periode(Granulariteit.WEEK, "2026-01-05", "2026-01-04", "2025-12-29", "2026-01-05");
        periode(Granulariteit.WEEK, "2026-01-05", "2026-01-05", "2026-01-05", "2026-01-12");
    }

    @Test void the_canonical_day_month() {
        assertEquals("--01-15", FixedStartPoint.canoniek(Granulariteit.MAAND, 1, 15));
        assertEquals("--01-31", FixedStartPoint.canoniek(Granulariteit.MAAND, 7, 31));
        assertEquals("--02-01", FixedStartPoint.canoniek(Granulariteit.KWARTAAL, 11, 1));
        assertEquals("--05-31", FixedStartPoint.canoniek(Granulariteit.KWARTAAL, 2, 31));
        assertEquals("--02-29", FixedStartPoint.canoniek(Granulariteit.JAAR, 2, 29));
        assertNull(FixedStartPoint.canoniek(Granulariteit.JAAR, 2, 30), "februari heeft geen dag 30");
    }

    @Test void the_sentences_of_wf_115() {
        assertEquals("fixed start point '2024-01-31' does not fit a month grid of count 1: a month, quarter or year "
                        + "grid of one unit starts on a day-month, written --mm-dd, a day of month mm in a leap year",
                FixedStartPoint.refusal(Granulariteit.MAAND, 1, "2024-01-31"));
        assertEquals("fixed start point '2026-01-05' does not fit a day grid of count 1: a day grid has a wall at "
                + "every day boundary and takes no start point", FixedStartPoint.refusal(Granulariteit.DAG, 1, "2026-01-05"));
        assertEquals("fixed start point '--01-05' does not fit a week grid of count 1: a week grid and a grid of several "
                + "units start on a date, written yyyy-mm-dd, the ISO form of a day of the calendar",
                FixedStartPoint.refusal(Granulariteit.WEEK, 1, "--01-05"));
        assertEquals("fixed start point '--02-01' does not fit a month grid of count 1: it lays the calendar's own "
                + "grid, which is written without a fixedStart", FixedStartPoint.refusal(Granulariteit.MAAND, 1, "--02-01"));
        assertEquals("fixed start point '--04-15' does not fit a quarter grid of count 1: its canonical spelling is "
                + "--01-15, with the earliest month of the year that has a wall on that day",
                FixedStartPoint.refusal(Granulariteit.KWARTAAL, 1, "--04-15"));
        assertNull(FixedStartPoint.refusal(Granulariteit.JAAR, 1, "--10-01"));
        assertNull(FixedStartPoint.refusal(Granulariteit.MAAND, 5, "2026-04-01"));
        assertEquals("timeline 'het stormjaar' has a variable start and a fixedStart '--10-01': its periods run from "
                        + "the anchor its start-point determination binds per object, and a timeline has one start",
                FixedStartPoint.refusalVariable("het stormjaar", "--10-01"));
        assertEquals("named-timeline adoption is partial: fixed start point none on the facet, '--10-01' on timeline "
                + "'stormjaar'", FixedStartPoint.refusalAdoption(null, "stormjaar", "--10-01"));
    }
}
