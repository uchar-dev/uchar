package io.github.yhavinga.regelspraak.transpiler;

import io.github.yhavinga.regelspraak.runtime.RegelSpraakRuntime;

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
class DagsoortDuurTest {

    /** the chapter's own day kind: 25 or 26 December. */
    private static final Predicate<LocalDate> KERST =
            d -> d.getMonthValue() == 12 && (d.getDayOfMonth() == 25 || d.getDayOfMonth() == 26);

    private static final Predicate<LocalDate> NOOIT = d -> false;

    private static LocalDate d(String iso) {
        return LocalDate.parse(iso);
    }

    private static LocalDate plus(String from, String n) {
        return RegelSpraakRuntime.rsDatumPlusDagsoort(d(from), new BigDecimal(n), KERST, true);
    }

    private static LocalDate min(String from, String n) {
        return RegelSpraakRuntime.rsDatumPlusDagsoort(d(from), new BigDecimal(n), KERST, false);
    }

    private static long telling(String van, String tot, boolean absoluut) {
        return RegelSpraakRuntime.rsTijdsduurDagsoort(d(van), d(tot), KERST, absoluut).longValueExact();
    }

    @Test
    void telling_is_start_exclusief_en_eind_inclusief() {
        assertEquals(2, telling("2024-12-20", "2024-12-31", false));
        assertEquals(1, telling("2024-12-25", "2024-12-26", false)); // the 25th is the excluded start
        assertEquals(-2, telling("2024-12-31", "2024-12-20", false));
        assertEquals(2, telling("2024-12-31", "2024-12-20", true));
        assertEquals(0, telling("2024-12-25", "2024-12-25", false));
    }

    @Test
    void telling_over_een_onvervulbare_dagsoort_is_nul_en_geen_fout() {
        assertEquals(
                0,
                RegelSpraakRuntime.rsTijdsduurDagsoort(d("2024-12-20"), d("2024-12-31"), NOOIT, false)
                        .longValueExact());
    }

    @Test
    void telling_propageert_leeg_nooit_nul() {
        assertNull(RegelSpraakRuntime.rsTijdsduurDagsoort(null, d("2024-12-31"), KERST, false));
        assertNull(RegelSpraakRuntime.rsTijdsduurDagsoort(d("2024-12-20"), null, KERST, false));
    }

    @Test
    void stap_telt_de_startdag_nooit_mee() {
        assertEquals(d("2024-12-26"), plus("2024-12-20", "2"));
        assertEquals(d("2024-12-26"), plus("2024-12-25", "1")); // the start IS of the kind
        assertEquals(d("2025-12-25"), plus("2024-12-26", "1"));
        assertEquals(d("2024-12-26"), min("2024-12-27", "1"));
        assertEquals(d("2023-12-26"), min("2024-12-25", "1"));
    }

    @Test
    void stap_van_nul_of_leeg_geeft_de_startdag_terug() {
        assertEquals(d("2024-12-20"), plus("2024-12-20", "0"));
        assertNull(RegelSpraakRuntime.rsDatumPlusDagsoort(null, new BigDecimal("2"), KERST, true));
        assertEquals(
                d("2024-12-20"), RegelSpraakRuntime.rsDatumPlusDagsoort(d("2024-12-20"), null, KERST, true));
    }

    @Test
    void stap_keert_om_bij_een_negatief_aantal_en_kapt_een_breuk_af_naar_nul() {
        assertEquals(d("2024-12-26"), plus("2024-12-27", "-1"));
        assertEquals(d("2024-12-26"), plus("2024-12-20", "2.9"));
    }

    @Test
    void stap_valt_op_de_reikwijdte_en_loopt_niet_eindeloos() {
        IllegalStateException e =
                assertThrows(
                        IllegalStateException.class,
                        () ->
                                RegelSpraakRuntime.rsDatumPlusDagsoort(
                                        d("2024-12-20"), BigDecimal.ONE, NOOIT, true));
        // the same fragment ref and clingo pin, and corpus/rood/dagsoort-onvervulbare-wandeling
        assertEquals(
                true,
                e.getMessage()
                        .contains("no day of the kind within " + RegelSpraakRuntime.DAGSOORT_REIKWIJDTE
                                + " calendar days"));
    }

    @Test
    void de_reikwijdte_ligt_boven_de_langste_gregoriaanse_terugkeer() {
        // the century-skipped leap day: 2096-02-29 -> 2104-02-29, 2 921 days, still steps. And the case the
        // reach was RAISED for (maintainer decision 2026-09-19): a year-bound day kind more than a decade out.
        Predicate<LocalDate> schrikkeldag = x -> x.getMonthValue() == 2 && x.getDayOfMonth() == 29;
        LocalDate uit =
                RegelSpraakRuntime.rsDatumPlusDagsoort(
                        d("2096-02-29"), BigDecimal.ONE, schrikkeldag, true);
        assertEquals(d("2104-02-29"), uit);
        assertEquals(2921, uit.toEpochDay() - d("2096-02-29").toEpochDay());

        Predicate<LocalDate> in2040 =
                x -> x.getYear() == 2040 && (x.getDayOfMonth() == 1 || x.getDayOfMonth() == 15);
        assertEquals(
                d("2040-01-01"),
                RegelSpraakRuntime.rsDatumPlusDagsoort(d("2026-03-10"), BigDecimal.ONE, in2040, true));
    }

    @Test
    void datumtijd_stapt_op_het_dagdeel_en_houdt_de_kloktijd() {
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
