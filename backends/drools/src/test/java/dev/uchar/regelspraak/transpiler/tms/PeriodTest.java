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
 * The value equality of a timeline in a fact of the exec-model branch ({@link Period}), which the KieBase in
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
 * are written out in {@link Period} for that reason, and {@link #seven_and_seven_zero_zero_are_a_value} pins them.
 */
class PeriodTest {

    private static final LocalDate JAN = LocalDate.of(2024, 1, 1), FEB = LocalDate.of(2024, 2, 1),
            MRT = LocalDate.of(2024, 3, 1);

    @Test void seven_and_seven_zero_zero_are_a_value() {
        Period a = new Period(JAN, FEB, new BigDecimal("7")), b = new Period(JAN, FEB, new BigDecimal("7.00"));
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertEquals(List.of(a), List.of(b));
        assertEquals(List.of(a).hashCode(), List.of(b).hashCode());
        Set<Object> sessionFacts = new HashSet<>(List.of(List.of(a), List.of(b)));
        assertEquals(1, sessionFacts.size(), "twee gelijke tijdlijnen zijn één feit");
        assertNotEquals(a, new Period(JAN, FEB, new BigDecimal("7.01")));
    }

    @Test void empty_is_equal_to_empty_and_not_to_zero() {
        Period empty = new Period(JAN, FEB, null);
        assertEquals(empty, new Period(JAN, FEB, null));
        assertEquals(empty.hashCode(), new Period(JAN, FEB, null).hashCode());
        assertNotEquals(empty, new Period(JAN, FEB, BigDecimal.ZERO));
        assertNotEquals(new Period(JAN, FEB, BigDecimal.ZERO), empty);
        assertEquals(List.of(), List.of(), "twee lege tijdlijnen zijn gelijk");
    }

    @Test void another_clip_is_another_value() {
        List<Period> whole = List.of(new Period(JAN, MRT, new BigDecimal("7")));
        List<Period> cut = List.of(new Period(JAN, FEB, new BigDecimal("7")), new Period(FEB, MRT, new BigDecimal("7")));
        assertNotEquals(whole, cut);
        assertNotEquals(new Period(JAN, FEB, BigDecimal.ONE), new Period(JAN, MRT, BigDecimal.ONE));
        assertNotEquals(new Period(JAN, MRT, BigDecimal.ONE), new Period(FEB, MRT, BigDecimal.ONE));
    }

    @Test void the_runtime_computes_with_a_period_as_segment_class() {
        List<Period> timeline = List.of(new Period(JAN, FEB, new BigDecimal("10")),
                new Period(FEB, MRT, new BigDecimal("11")));
        assertEquals(0, new BigDecimal("21").compareTo(RegelSpraakRuntime.rsTijdTotaal(timeline, "MAAND")));
        List<Object> clip = RegelSpraakRuntime.rsTijdKnip(timeline, List.of(new Period(FEB, MRT, null)), Period.class);
        assertEquals(List.of(new Period(FEB, MRT, new BigDecimal("11"))), clip);
        Object recomputed = RegelSpraakRuntime.newSegment(Period.class, JAN, FEB, BigDecimal.TEN);
        assertEquals(new Period(JAN, FEB, BigDecimal.TEN), recomputed);
    }

    @Test void list_makes_an_immutable_timeline_and_lets_an_absent_be_absent() {
        assertNull(Period.lijst(null));
        List<Object> segments = new ArrayList<>(List.of(new Period(JAN, FEB, BigDecimal.ONE)));
        List<Period> timeline = Period.lijst(segments);
        segments.clear();
        assertEquals(List.of(new Period(JAN, FEB, BigDecimal.ONE)), timeline);
        assertThrows(UnsupportedOperationException.class, () -> timeline.add(new Period(FEB, MRT, BigDecimal.ONE)));
    }
}
