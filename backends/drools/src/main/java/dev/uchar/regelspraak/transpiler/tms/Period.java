package dev.uchar.regelspraak.transpiler.tms;

import dev.uchar.regelspraak.runtime.RegelSpraakRuntime;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * One segment {@code [van, tot)} of a timeline as the exec-model branch carries it in a fact (RUNTIME-SEMANTICS
 * ch. 3.1): a value, or leeg (null), from day {@code van} up to, not including, day {@code tot}; a true period of a
 * time-dependent characteristic carries null. A timeline is an immutable {@code List<Period>}, ordered as its producer
 * gave it: a {@link State} or a {@link Value} holds one as its value.
 *
 * <p>Why not the bean model's {@code Segment}: the KieBase runs in {@code EQUALITY}, where two equal logical facts are
 * one fact, so a value in a fact needs value equality, and the generated {@code Segment} is mutable and declares no
 * {@code equals} or {@code hashCode}. A timeline is turned into this form where the branch binds it off a bean
 * ({@link #list}) and back where the write-back puts it on one ({@link #beans}); nothing in between sees a
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
public record Period(LocalDate van, LocalDate to, BigDecimal value) {

    public Period {
        Objects.requireNonNull(van, "van");
        Objects.requireNonNull(to, "tot");
    }

    public LocalDate getVan() { return van; }

    public LocalDate getTot() { return to; }

    public BigDecimal getWaarde() { return value; }

    @Override public boolean equals(Object o) {
        return o instanceof Period t && t.van.equals(van) && t.to.equals(to)
                && (value == null ? t.value == null : t.value != null && t.value.compareTo(value) == 0);
    }

    @Override public int hashCode() {
        return Objects.hash(van, to, value == null ? null : value.stripTrailingZeros());
    }

    @Override public String toString() {
        return "[" + van + ", " + to + ")=" + (value == null ? "leeg" : value.toPlainString());
    }

    /** The timeline {@code segments} as this branch carries it, immutable and in the given order: from the segments a
     *  bean holds, or from the list a runtime helper returns. Null, an absent timeline, stays null (ch. 3.1: an absent
     *  timeline is not an empty one, ch. 3.6). */
    public static List<Period> list(List<?> segments) {
        if (segments == null) return null;
        List<Period> out = new ArrayList<>(segments.size());
        for (Object s : segments)
            out.add(s instanceof Period t ? t : new Period((LocalDate) RegelSpraakRuntime.get(s, "van"),
                    (LocalDate) RegelSpraakRuntime.get(s, "tot"), (BigDecimal) RegelSpraakRuntime.get(s, "waarde")));
        return List.copyOf(out);
    }

    /** The timeline {@code timeline} as beans of the bean model's {@code segmentClass}, for the write-back: a new,
     *  mutable list of new segments, so no bean shares a segment with another. */
    public static List<Object> beans(List<Period> timeline, Class<?> segmentClass) {
        List<Object> out = new ArrayList<>(timeline.size());
        for (Period t : timeline) out.add(RegelSpraakRuntime.newSegment(segmentClass, t.van, t.to, t.value));
        return out;
    }
}
