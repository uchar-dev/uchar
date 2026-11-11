package dev.uchar.regelspraak.transpiler.tms;

import dev.uchar.regelspraak.runtime.RegelSpraakRuntime;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * One segment {@code [from, to)} of a timeline as the exec-model branch carries it in a fact (RUNTIME-SEMANTICS
 * ch. 3.1): a value, or leeg (null), from day {@code from} up to, not including, day {@code to}; a true period of a
 * time-dependent characteristic carries null. A timeline is an immutable {@code List<Period>}, ordered as its producer
 * gave it: a {@link State} or a {@link Value} holds one as its value.
 *
 * <p>Why not the bean model's {@code Segment}: the KieBase runs in {@code EQUALITY}, where two equal logical facts are
 * one fact, so a value in a fact needs value equality, and the generated {@code Segment} is mutable and declares no
 * {@code equals} or {@code hashCode}. A timeline is turned into this form where the branch binds it off a bean
 * ({@link #list}) and back where the write-back puts it on one ({@link #beans}); nothing in between sees a
 * {@code Segment}.
 *
 * <p>A value is a number, a constructor value ({@link ConstructorValue}) on a timeline of a sum type (ch. 9.4), or a
 * text on a timeline of a text, which the whole-period qualifier reads per day (ch. 3.9). Two
 * segments are equal where they have the same {@code from}, the same {@code to}, and values that are equal as numbers,
 * equal constructor values, equal texts or both leeg: 7 and 7.00 are one value, as in the reference interpreter's comparison of two
 * timelines ({@code World.write_timeline}, {@code Arith.equal}) and the change detection of ch. 3.1. The hash code is
 * taken over a number without trailing zeros, so that it agrees with that equality: a record's own hash code is that
 * of {@code BigDecimal}, which depends on the scale, and would give two facts for one value. Two timelines are equal
 * where they hold equal segments in the same order; a timeline cut differently is a different value, also where it
 * reads the same on every day, since ch. 3.1 makes the segment-wise comparison the change detection, and the
 * reference interpreter compares so.
 *
 * <p>The runtime's timeline helpers ({@code rsTijdKnip}, {@code rsAantalDagenIn}, {@code rsOoitWaarden} and the
 * others) read a segment through {@code getVan()},
 * {@code getTot()} and {@code getWaarde()} ({@link RegelSpraakRuntime#get}) and make one through the public constructor
 * of {@code (LocalDate, LocalDate, BigDecimal)} ({@link RegelSpraakRuntime#newSegment}): given this class as their
 * segment class they compute with it as with the bean model's, and nothing in {@code runtime/} changes. They compute
 * with numbers only, so a timeline of constructor values never reaches them.
 *
 * <p>A timeline divides all time (ch. 3.1), so a segment may have an open bound: an open {@code from} is
 * {@link #OPEN_FROM}, before every day, and an open {@code to} is {@link #OPEN_TO}, past every day. The two ends of the
 * date range keep a clip, a complement and a comparison of two bounds what they are on a closed segment, as the
 * reference interpreter keeps them with the ends of its integer range; only a fold that walks the buckets of a period
 * or counts its units has to ask for one ({@link #open}). On a bean an open bound is null, as the case format leaves
 * the bound out.
 */
public record Period(LocalDate from, LocalDate to, Object value) {

    /** The open {@code from} of a segment that begins before every day. */
    public static final LocalDate OPEN_FROM = LocalDate.MIN;
    /** The open {@code to} of a segment that runs past every day. */
    public static final LocalDate OPEN_TO = LocalDate.MAX;

    public Period {
        Objects.requireNonNull(from, "van");
        Objects.requireNonNull(to, "to");
        if (value != null && !(value instanceof BigDecimal) && !(value instanceof ConstructorValue)
                && !(value instanceof String))
            throw new IllegalArgumentException("a segment holds a decimal, a constructor value or a text, not a "
                    + value.getClass().getName());
    }

    /** The constructor the runtime's timeline helpers make a segment through ({@link RegelSpraakRuntime#newSegment}). */
    public Period(LocalDate from, LocalDate to, BigDecimal value) {
        this(from, to, (Object) value);
    }

    /** The value of this segment as a number, which a segment of a numeric timeline holds; leeg is null. */
    public BigDecimal number() {
        return (BigDecimal) value;
    }

    /** Whether this segment has an open bound (ch. 3.1). */
    public boolean open() {
        return from.equals(OPEN_FROM) || to.equals(OPEN_TO);
    }

    public LocalDate getVan() { return from; }

    public LocalDate getTot() { return to; }

    public Object getWaarde() { return value; }

    @Override public boolean equals(Object o) {
        return o instanceof Period t && t.from.equals(from) && t.to.equals(to) && sameValue(t.value, value);
    }

    @Override public int hashCode() {
        return Objects.hash(from, to, value instanceof BigDecimal d ? d.stripTrailingZeros() : value);
    }

    /** Whether two values of segments are one value: numbers by value, constructor values and texts by their equality,
     *  leeg only with leeg. */
    public static boolean sameValue(Object a, Object b) {
        if (a instanceof BigDecimal x) return b instanceof BigDecimal y && x.compareTo(y) == 0;
        return Objects.equals(a, b);
    }

    @Override public String toString() {
        return "[" + (from.equals(OPEN_FROM) ? "-inf" : from) + ", " + (to.equals(OPEN_TO) ? "+inf" : to) + ")="
                + (value == null ? "empty" : value instanceof BigDecimal d ? d.toPlainString() : value);
    }

    /** The timeline {@code segments} as this branch carries it, immutable and in the given order: from the segments a
     *  bean holds, a null bound there an open one, or from the list a runtime helper returns. Null, an absent timeline,
     *  stays null (ch. 3.1: an absent timeline is not an empty one, ch. 3.6). */
    public static List<Period> list(List<?> segments) {
        if (segments == null) return null;
        List<Period> out = new ArrayList<>(segments.size());
        for (Object s : segments) {
            if (s instanceof Period t) {
                out.add(t);
                continue;
            }
            LocalDate from = (LocalDate) RegelSpraakRuntime.get(s, "van"), to = (LocalDate) RegelSpraakRuntime.get(s, "tot");
            out.add(new Period(from == null ? OPEN_FROM : from, to == null ? OPEN_TO : to,
                    RegelSpraakRuntime.get(s, "waarde")));
        }
        return List.copyOf(out);
    }

    /** The timeline {@code timeline} as beans of the bean model's {@code segmentClass}, for the write-back: a new,
     *  mutable list of new segments, so no bean shares a segment with another; an open bound is null there. */
    public static List<Object> beans(List<Period> timeline, Class<?> segmentClass) {
        List<Object> out = new ArrayList<>(timeline.size());
        for (Period t : timeline)
            out.add(bean(segmentClass, t.from.equals(OPEN_FROM) ? null : t.from, t.to.equals(OPEN_TO) ? null : t.to,
                    t.value));
        return out;
    }

    /** One segment of the bean model's {@code segmentClass}: a number through the runtime's constructor, and a
     *  constructor value or a text through the one of {@code (LocalDate, LocalDate, Object)} the bean model generates
     *  beside it. */
    public static Object bean(Class<?> segmentClass, LocalDate from, LocalDate to, Object value) {
        if (value == null || value instanceof BigDecimal)
            return RegelSpraakRuntime.newSegment(segmentClass, from, to, (BigDecimal) value);
        try {
            return segmentClass.getConstructor(LocalDate.class, LocalDate.class, Object.class).newInstance(from, to, value);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("no segment of a constructor value on " + segmentClass.getName(), e);
        }
    }
}
