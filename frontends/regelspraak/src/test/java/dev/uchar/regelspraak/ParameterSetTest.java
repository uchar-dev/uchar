package dev.uchar.regelspraak;

import dev.uchar.regelspraak.ir.Validity;
import dev.uchar.regelspraak.model.ParameterSet;
import dev.uchar.regelspraak.model.ParameterSet.ValueInterval;
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

    private static Validity g(String from, String until) {
        return new Validity(from == null ? null : LocalDate.parse(from),
                              until == null ? null : LocalDate.parse(until));
    }

    private static ParameterSet set(String naam, String param, ValueInterval... intervals) {
        return new ParameterSet(naam, Map.of(param, List.of(intervals)));
    }

    // ---- overlap ----

    @Test void bounded_bounded_that_share_a_day_is_overlap() {
        var sets = List.of(set("s", "tarief",
                new ValueInterval(new BigDecimal(1), g("2025-01-01", "2025-12-31")),
                new ValueInterval(new BigDecimal(2), g("2025-12-31", "2026-06-30"))));
        List<String> faults = ParameterSet.overlapErrors(sets);
        assertEquals(1, faults.size(), faults::toString);
        assertTrue(faults.get(0).contains("tarief"), faults::toString);
    }

    @Test void adjacent_non_overlapping_is_ok() {
        var sets = List.of(set("s", "tarief",
                new ValueInterval(new BigDecimal(1), g("2025-01-01", "2025-12-31")),
                new ValueInterval(new BigDecimal(2), g("2026-01-01", "2026-12-31"))));
        assertTrue(ParameterSet.overlapErrors(sets).isEmpty());
    }

    @Test void open_end_versus_bounded_overlaps() {
        var sets = List.of(set("s", "tarief",
                new ValueInterval(new BigDecimal(1), g(null, "2025-12-31")),
                new ValueInterval(new BigDecimal(2), g("2025-06-01", "2026-06-30"))));
        assertEquals(1, ParameterSet.overlapErrors(sets).size());
    }

    @Test void overlap_across_two_sets() {
        var a = set("2025", "tarief", new ValueInterval(new BigDecimal(1), g("2025-01-01", "2025-12-31")));
        var b = set("2025b", "tarief", new ValueInterval(new BigDecimal(2), g("2025-06-01", "2026-06-30")));
        List<String> faults = ParameterSet.overlapErrors(List.of(a, b));
        assertEquals(1, faults.size(), faults::toString);
        assertTrue(faults.get(0).contains("tarief"));
    }

    // ---- valuesOn ----

    @Test void values_on_happy_chooses_the_containing_interval() {
        var sets = List.of(set("s", "tarief",
                new ValueInterval(new BigDecimal(1), g("2025-01-01", "2025-12-31")),
                new ValueInterval(new BigDecimal(2), g("2026-01-01", "2026-12-31"))));
        List<String> faults = new ArrayList<>();
        Map<String, Object> result = ParameterSet.valuesOn(sets, LocalDate.parse("2026-03-01"), faults);
        assertTrue(faults.isEmpty(), faults::toString);
        assertEquals(new BigDecimal(2), result.get("tarief"));
    }

    @Test void values_on_open_bounds_contains_everything() {
        var sets = List.of(set("s", "tarief",
                new ValueInterval(new BigDecimal(7), Validity.ALTIJD)));
        List<String> faults = new ArrayList<>();
        Map<String, Object> result = ParameterSet.valuesOn(sets, LocalDate.parse("1999-01-01"), faults);
        assertTrue(faults.isEmpty());
        assertEquals(new BigDecimal(7), result.get("tarief"));
    }

    @Test void values_on_gap_names_parameter_and_date() {
        var sets = List.of(set("s", "tarief",
                new ValueInterval(new BigDecimal(1), g("2025-01-01", "2025-12-31"))));
        List<String> faults = new ArrayList<>();
        Map<String, Object> result = ParameterSet.valuesOn(sets, LocalDate.parse("2027-05-05"), faults);
        assertTrue(result.isEmpty());
        assertEquals(1, faults.size());
        assertTrue(faults.get(0).contains("tarief"), faults::toString);
        assertTrue(faults.get(0).contains("2027-05-05"), faults::toString);
    }

    // ---- multiVintage ----

    @Test void more_vintage_single_always_is_false() {
        var sets = List.of(set("s", "tarief", new ValueInterval(new BigDecimal(1), Validity.ALTIJD)));
        assertFalse(ParameterSet.multiVintage(sets));
    }

    @Test void more_vintage_two_intervals_is_true() {
        var sets = List.of(set("s", "tarief",
                new ValueInterval(new BigDecimal(1), g("2025-01-01", "2025-12-31")),
                new ValueInterval(new BigDecimal(2), g("2026-01-01", "2026-12-31"))));
        assertTrue(ParameterSet.multiVintage(sets));
    }

    @Test void more_vintage_a_bounded_interval_is_true() {
        var sets = List.of(set("s", "tarief",
                new ValueInterval(new BigDecimal(1), g("2025-01-01", "2025-12-31"))));
        assertTrue(ParameterSet.multiVintage(sets));
    }
}
