package dev.uchar.regelspraak.runtime;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The whole units the temporal total weighs a segment by (RUNTIME-SEMANTICS ch. 3.2 job 1, ch. 3.6): a segment
 * holds k whole months when its start plus k months, clamped to the end of the target month (ch. 3.8), does not
 * pass its end. A period between clamped walls is therefore one whole unit, as the reference interpreter and the
 * ASP backend weigh it: [2024-01-31, 2024-02-29) is the month a fixed start point on the 31st lays, and it is one
 * month.
 *
 * <p><b>Red before the fix</b> (measured 2026-09-26 on the union e6844fba): the runtime counted with
 * {@code ChronoUnit.MONTHS/YEARS.between}, which is the packed-month rule of the duration (ch. 3.8), and answered
 * 0 for each of the three clamped periods below, where ref and clingo answer one unit. The same count reached the
 * clip total on the calendar grid before the fixed start point existed (a gedurende fragment ending on a clamped
 * day), so the defect was older than the grid that made it easy to reach.
 *
 * <p>The duration keeps its own rule: "tijdsduur van 31-01-2024 tot 29-02-2024" is 0 whole months (ch. 3.8,
 * "Clamp, not inverse"), and the last test holds the two apart.
 */
class TijdtotaalKlemWitnessTest {

    @Test void een_maand_tussen_geklemde_muren_weegt_een_maand() {
        assertEquals(new BigDecimal("10"), totaal("MAAND", "2024-01-31", "2024-02-29", "10"),
                "the --01-31 month grid's February 2024 period");
        assertEquals(new BigDecimal("10"), totaal("MAAND", "2025-01-31", "2025-02-28", "10"),
                "the same period in a year without 29 February");
        assertEquals(new BigDecimal("10"), totaal("MAAND", "2024-04-30", "2024-05-31", "10"),
                "a period starting on a clamped wall and ending on the 31st");
    }

    @Test void een_kwartaal_en_een_jaar_tussen_geklemde_muren_wegen_een_eenheid() {
        assertEquals(new BigDecimal("30"), totaal("KWARTAAL", "2024-11-30", "2025-02-28", "30"),
                "the --05-31 quarter grid's period around the turn of the year");
        assertEquals(new BigDecimal("100"), totaal("JAAR", "2024-02-29", "2025-02-28", "100"),
                "the --02-29 year grid's period from the leap day");
    }

    @Test void een_dag_te_kort_is_geen_hele_eenheid() {
        assertEquals(BigDecimal.ZERO, totaal("MAAND", "2024-01-31", "2024-02-28", "10"));
        assertEquals(BigDecimal.ZERO, totaal("JAAR", "2024-02-29", "2025-02-27", "100"));
        assertEquals(new BigDecimal("20"), totaal("MAAND", "2024-01-31", "2024-03-31", "10"),
                "two periods of the --01-31 grid");
    }

    @Test void de_tijdsduur_houdt_de_regel_van_de_gepakte_maand() {
        assertEquals(BigDecimal.ZERO, RegelSpraakRuntime.rsTijdsduur(LocalDate.parse("2024-01-31"),
                LocalDate.parse("2024-02-29"), "MONTHS", false), "ch. 3.8: whole-unit counting is not the inverse"
                + " of date-plus-unit arithmetic");
        assertEquals(new BigDecimal("10"), totaal("MAAND", "2024-01-31", "2024-02-29", "10"),
                "the temporal total over the same two dates");
    }

    private static BigDecimal totaal(String granulariteit, String van, String tot, String waarde) {
        return RegelSpraakRuntime.rsTijdTotaal(List.of(new Seg(van, tot, waarde)), granulariteit);
    }

    /** Shape-faithful segment stand-in for the runtime fold: the {@code van}/{@code tot}/{@code waarde} properties
     *  it reads reflectively. */
    public static final class Seg {
        private final LocalDate van, tot; private final BigDecimal waarde;
        Seg(String van, String tot, String waarde) {
            this.van = LocalDate.parse(van); this.tot = LocalDate.parse(tot); this.waarde = new BigDecimal(waarde);
        }
        public LocalDate getVan() { return van; }
        public LocalDate getTot() { return tot; }
        public BigDecimal getWaarde() { return waarde; }
    }
}
