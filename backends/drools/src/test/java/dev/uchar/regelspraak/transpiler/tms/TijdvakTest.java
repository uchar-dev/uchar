package dev.uchar.regelspraak.transpiler.tms;

import dev.uchar.regelspraak.runtime.RegelSpraakRuntime;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * The value equality of a timeline in a fact of the truth-maintenance branch ({@link Tijdvak}), which the KieBase in
 * {@code EQUALITY} rests on: two equal logical facts are one fact, found through {@code hashCode} and confirmed through
 * {@code equals}, so the two must agree.
 *
 * <p>The equality is that of RUNTIME-SEMANTICS ch. 3.1 and of the reference interpreter's comparison of two timelines
 * ({@code ref/lib/world.ml}, {@code write_timeline}: the same length, and per segment the same {@code van}, the same
 * {@code tot} and values equal under {@code Arith.equal}): 7 and 7.00 are one value, two leeg segments are equal, and two
 * timelines that read the same on every day but are cut differently are two values.
 *
 * <p><b>Red</b> before the change: the class did not exist. A record's own {@code hashCode} would take
 * {@code BigDecimal.hashCode}, which depends on the scale, and its own {@code equals} {@code BigDecimal.equals}; both
 * are written out in {@link Tijdvak} for that reason, and {@link #zeven_en_zeven_nul_nul_zijn_een_waarde} pins them.
 */
class TijdvakTest {

    private static final LocalDate JAN = LocalDate.of(2024, 1, 1), FEB = LocalDate.of(2024, 2, 1),
            MRT = LocalDate.of(2024, 3, 1);

    @Test void zeven_en_zeven_nul_nul_zijn_een_waarde() {
        Tijdvak a = new Tijdvak(JAN, FEB, new BigDecimal("7")), b = new Tijdvak(JAN, FEB, new BigDecimal("7.00"));
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertEquals(List.of(a), List.of(b));
        assertEquals(List.of(a).hashCode(), List.of(b).hashCode());
        Set<Object> feiten = new HashSet<>(List.of(List.of(a), List.of(b)));
        assertEquals(1, feiten.size(), "twee gelijke tijdlijnen zijn één feit");
        assertNotEquals(a, new Tijdvak(JAN, FEB, new BigDecimal("7.01")));
    }

    @Test void leeg_is_gelijk_aan_leeg_en_niet_aan_nul() {
        Tijdvak leeg = new Tijdvak(JAN, FEB, null);
        assertEquals(leeg, new Tijdvak(JAN, FEB, null));
        assertEquals(leeg.hashCode(), new Tijdvak(JAN, FEB, null).hashCode());
        assertNotEquals(leeg, new Tijdvak(JAN, FEB, BigDecimal.ZERO));
        assertNotEquals(new Tijdvak(JAN, FEB, BigDecimal.ZERO), leeg);
        assertEquals(List.of(), List.of(), "twee lege tijdlijnen zijn gelijk");
    }

    @Test void een_andere_knip_is_een_andere_waarde() {
        List<Tijdvak> heel = List.of(new Tijdvak(JAN, MRT, new BigDecimal("7")));
        List<Tijdvak> geknipt = List.of(new Tijdvak(JAN, FEB, new BigDecimal("7")), new Tijdvak(FEB, MRT, new BigDecimal("7")));
        assertNotEquals(heel, geknipt);
        assertNotEquals(new Tijdvak(JAN, FEB, BigDecimal.ONE), new Tijdvak(JAN, MRT, BigDecimal.ONE));
        assertNotEquals(new Tijdvak(JAN, MRT, BigDecimal.ONE), new Tijdvak(FEB, MRT, BigDecimal.ONE));
    }

    @Test void de_runtime_rekent_met_een_tijdvak_als_segmentklasse() {
        List<Tijdvak> tijdlijn = List.of(new Tijdvak(JAN, FEB, new BigDecimal("10")),
                new Tijdvak(FEB, MRT, new BigDecimal("11")));
        assertEquals(0, new BigDecimal("21").compareTo(RegelSpraakRuntime.rsTijdTotaal(tijdlijn, "MAAND")));
        List<Object> knip = RegelSpraakRuntime.rsTijdKnip(tijdlijn, List.of(new Tijdvak(FEB, MRT, null)), Tijdvak.class);
        assertEquals(List.of(new Tijdvak(FEB, MRT, new BigDecimal("11"))), knip);
        Object nieuw = RegelSpraakRuntime.newSegment(Tijdvak.class, JAN, FEB, BigDecimal.TEN);
        assertEquals(new Tijdvak(JAN, FEB, BigDecimal.TEN), nieuw);
    }

    @Test void lijst_maakt_een_onveranderlijke_tijdlijn_en_laat_een_afwezige_afwezig() {
        assertNull(Tijdvak.lijst(null));
        List<Object> segmenten = new ArrayList<>(List.of(new Tijdvak(JAN, FEB, BigDecimal.ONE)));
        List<Tijdvak> tijdlijn = Tijdvak.lijst(segmenten);
        segmenten.clear();
        assertEquals(List.of(new Tijdvak(JAN, FEB, BigDecimal.ONE)), tijdlijn);
        assertThrows(UnsupportedOperationException.class, () -> tijdlijn.add(new Tijdvak(FEB, MRT, BigDecimal.ONE)));
    }
}
