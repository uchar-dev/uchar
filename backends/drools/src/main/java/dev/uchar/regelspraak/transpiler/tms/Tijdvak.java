package dev.uchar.regelspraak.transpiler.tms;

import dev.uchar.regelspraak.runtime.RegelSpraakRuntime;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * One segment {@code [van, tot)} of a timeline as the truth-maintenance branch carries it in a fact (RUNTIME-SEMANTICS
 * ch. 3.1): a value, or leeg (null), from day {@code van} up to, not including, day {@code tot}; a true period of a
 * time-dependent characteristic carries null. A timeline is an immutable {@code List<Tijdvak>}, ordered as its producer
 * gave it: a {@link Stand} or a {@link Waarde} holds one as its value.
 *
 * <p>Why not the bean model's {@code Segment}: the KieBase runs in {@code EQUALITY}, where two equal logical facts are
 * one fact, so a value in a fact needs value equality, and the generated {@code Segment} is mutable and declares no
 * {@code equals} or {@code hashCode}. A timeline is turned into this form where the branch binds it off a bean
 * ({@link #lijst}) and back where the write-back puts it on one ({@link #bonen}); nothing in between sees a
 * {@code Segment}.
 *
 * <p>Two segments are equal where they have the same {@code van}, the same {@code tot}, and values that are equal as
 * numbers or both leeg: 7 and 7.00 are one value, as in the reference interpreter's comparison of two timelines
 * ({@code World.write_timeline}, {@code Arith.equal}) and the change detection of ch. 3.1. The hash code is taken over
 * the value without trailing zeros, so that it agrees with that equality: a record's own hash code is that of
 * {@code BigDecimal}, which depends on the scale, and would give two facts for one value. Two timelines are equal
 * where they hold equal segments in the same order; a timeline cut differently is a different value, also where it
 * reads the same on every day, since ch. 3.1 makes the segment-wise comparison the change detection, and the
 * reference interpreter compares so.
 *
 * <p>The runtime's timeline helpers ({@code rsTijdTotaal}, {@code rsTijdKnip}, {@code rsTijdsevenredig},
 * {@code rsAantalDagenIn}, {@code rsOoitWaarden} and the others) read a segment through {@code getVan()},
 * {@code getTot()} and {@code getWaarde()} ({@link RegelSpraakRuntime#get}) and make one through the public constructor
 * of {@code (LocalDate, LocalDate, BigDecimal)} ({@link RegelSpraakRuntime#newSegment}): given this class as their
 * segment class they compute with it as with the bean model's, and nothing in {@code runtime/} changes.
 */
public record Tijdvak(LocalDate van, LocalDate tot, BigDecimal waarde) {

    public Tijdvak {
        Objects.requireNonNull(van, "van");
        Objects.requireNonNull(tot, "tot");
    }

    public LocalDate getVan() { return van; }

    public LocalDate getTot() { return tot; }

    public BigDecimal getWaarde() { return waarde; }

    @Override public boolean equals(Object o) {
        return o instanceof Tijdvak t && t.van.equals(van) && t.tot.equals(tot)
                && (waarde == null ? t.waarde == null : t.waarde != null && t.waarde.compareTo(waarde) == 0);
    }

    @Override public int hashCode() {
        return Objects.hash(van, tot, waarde == null ? null : waarde.stripTrailingZeros());
    }

    @Override public String toString() {
        return "[" + van + ", " + tot + ")=" + (waarde == null ? "leeg" : waarde.toPlainString());
    }

    /** The timeline {@code segmenten} as this branch carries it, immutable and in the given order: from the segments a
     *  bean holds, or from the list a runtime helper returns. Null, an absent timeline, stays null (ch. 3.1: an absent
     *  timeline is not an empty one, ch. 3.6). */
    public static List<Tijdvak> lijst(List<?> segmenten) {
        if (segmenten == null) return null;
        List<Tijdvak> uit = new ArrayList<>(segmenten.size());
        for (Object s : segmenten)
            uit.add(s instanceof Tijdvak t ? t : new Tijdvak((LocalDate) RegelSpraakRuntime.get(s, "van"),
                    (LocalDate) RegelSpraakRuntime.get(s, "tot"), (BigDecimal) RegelSpraakRuntime.get(s, "waarde")));
        return List.copyOf(uit);
    }

    /** The timeline {@code tijdlijn} as beans of the bean model's {@code segmentklasse}, for the write-back: a new,
     *  mutable list of new segments, so no bean shares a segment with another. */
    public static List<Object> bonen(List<Tijdvak> tijdlijn, Class<?> segmentklasse) {
        List<Object> uit = new ArrayList<>(tijdlijn.size());
        for (Tijdvak t : tijdlijn) uit.add(RegelSpraakRuntime.newSegment(segmentklasse, t.van, t.tot, t.waarde));
        return uit;
    }
}
