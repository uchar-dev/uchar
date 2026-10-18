package dev.uchar.regelspraak;

import dev.uchar.regelspraak.ir.Geldigheid;
import dev.uchar.regelspraak.model.ParameterSet;
import dev.uchar.regelspraak.model.ParameterSet.WaardeInterval;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Puur-Java unittests voor de vintage-selectie / fail-fast semantiek van {@link ParameterSet}: overlap-detectie,
 * peildatum-selectie met gap-fout, en de meer-vintage-vraag. Geen Jackson, geen model — alleen de kern.
 */
class ParameterSetTest {

    private static Geldigheid g(String vanaf, String tot) {
        return new Geldigheid(vanaf == null ? null : LocalDate.parse(vanaf),
                              tot == null ? null : LocalDate.parse(tot));
    }

    private static ParameterSet set(String naam, String param, WaardeInterval... intervals) {
        return new ParameterSet(naam, Map.of(param, List.of(intervals)));
    }

    // ---- overlap ----

    @Test void bounded_bounded_that_share_a_day_is_overlap() {
        var sets = List.of(set("s", "tarief",
                new WaardeInterval(new BigDecimal(1), g("2025-01-01", "2025-12-31")),
                new WaardeInterval(new BigDecimal(2), g("2025-12-31", "2026-06-30"))));
        List<String> fouten = ParameterSet.overlapFouten(sets);
        assertEquals(1, fouten.size(), fouten::toString);
        assertTrue(fouten.get(0).contains("tarief"), fouten::toString);
    }

    @Test void adjacent_non_overlapping_is_ok() {
        var sets = List.of(set("s", "tarief",
                new WaardeInterval(new BigDecimal(1), g("2025-01-01", "2025-12-31")),
                new WaardeInterval(new BigDecimal(2), g("2026-01-01", "2026-12-31"))));
        assertTrue(ParameterSet.overlapFouten(sets).isEmpty());
    }

    @Test void open_end_versus_bounded_overlaps() {
        var sets = List.of(set("s", "tarief",
                new WaardeInterval(new BigDecimal(1), g(null, "2025-12-31")),
                new WaardeInterval(new BigDecimal(2), g("2025-06-01", "2026-06-30"))));
        assertEquals(1, ParameterSet.overlapFouten(sets).size());
    }

    @Test void overlap_across_two_sets() {
        var a = set("2025", "tarief", new WaardeInterval(new BigDecimal(1), g("2025-01-01", "2025-12-31")));
        var b = set("2025b", "tarief", new WaardeInterval(new BigDecimal(2), g("2025-06-01", "2026-06-30")));
        List<String> fouten = ParameterSet.overlapFouten(List.of(a, b));
        assertEquals(1, fouten.size(), fouten::toString);
        assertTrue(fouten.get(0).contains("tarief"));
    }

    // ---- waardenOp ----

    @Test void values_on_happy_chooses_the_containing_interval() {
        var sets = List.of(set("s", "tarief",
                new WaardeInterval(new BigDecimal(1), g("2025-01-01", "2025-12-31")),
                new WaardeInterval(new BigDecimal(2), g("2026-01-01", "2026-12-31"))));
        List<String> fouten = new ArrayList<>();
        Map<String, Object> result = ParameterSet.waardenOp(sets, LocalDate.parse("2026-03-01"), fouten);
        assertTrue(fouten.isEmpty(), fouten::toString);
        assertEquals(new BigDecimal(2), result.get("tarief"));
    }

    @Test void values_on_open_bounds_contains_everything() {
        var sets = List.of(set("s", "tarief",
                new WaardeInterval(new BigDecimal(7), Geldigheid.ALTIJD)));
        List<String> fouten = new ArrayList<>();
        Map<String, Object> result = ParameterSet.waardenOp(sets, LocalDate.parse("1999-01-01"), fouten);
        assertTrue(fouten.isEmpty());
        assertEquals(new BigDecimal(7), result.get("tarief"));
    }

    @Test void values_on_gap_names_parameter_and_date() {
        var sets = List.of(set("s", "tarief",
                new WaardeInterval(new BigDecimal(1), g("2025-01-01", "2025-12-31"))));
        List<String> fouten = new ArrayList<>();
        Map<String, Object> result = ParameterSet.waardenOp(sets, LocalDate.parse("2027-05-05"), fouten);
        assertTrue(result.isEmpty());
        assertEquals(1, fouten.size());
        assertTrue(fouten.get(0).contains("tarief"), fouten::toString);
        assertTrue(fouten.get(0).contains("2027-05-05"), fouten::toString);
    }

    // ---- meerVintage ----

    @Test void more_vintage_single_always_is_false() {
        var sets = List.of(set("s", "tarief", new WaardeInterval(new BigDecimal(1), Geldigheid.ALTIJD)));
        assertFalse(ParameterSet.meerVintage(sets));
    }

    @Test void more_vintage_two_intervals_is_true() {
        var sets = List.of(set("s", "tarief",
                new WaardeInterval(new BigDecimal(1), g("2025-01-01", "2025-12-31")),
                new WaardeInterval(new BigDecimal(2), g("2026-01-01", "2026-12-31"))));
        assertTrue(ParameterSet.meerVintage(sets));
    }

    @Test void more_vintage_a_bounded_interval_is_true() {
        var sets = List.of(set("s", "tarief",
                new WaardeInterval(new BigDecimal(1), g("2025-01-01", "2025-12-31"))));
        assertTrue(ParameterSet.meerVintage(sets));
    }
}
