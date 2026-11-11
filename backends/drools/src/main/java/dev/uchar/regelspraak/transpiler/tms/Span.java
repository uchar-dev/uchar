package dev.uchar.regelspraak.transpiler.tms;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

/**
 * A segment {@code [from, to)} of a timeline with its value, leeg as null (RUNTIME-SEMANTICS ch. 3.1), and the
 * operations on days that the period arithmetic, the condition read per period and the defaults per day share: the read
 * at a day, the cover of a day, the cut set, the clip to periods and the join of adjacent segments of one value. Two
 * records are segments: a {@link Period} of working memory, whose value is a decimal, a constructor value or a text, and
 * a {@link PeriodArithmetic.Exact} on its way to a written timeline, whose value may be a rational
 * ({@link dev.uchar.regelspraak.runtime.Breuk}); a period of a condition is a {@link Period} without a value. A segment
 * holds the day {@code from} and not the day {@code to}; an open bound is {@link Period#OPEN_FROM} or
 * {@link Period#OPEN_TO}, which these operations order as the two ends of the date range.
 *
 * @param <S> the record itself, so that a fragment of a segment is a segment of the same kind
 */
public interface Span<S extends Span<S>> {

    LocalDate from();

    LocalDate to();

    Object value();

    /** This segment's value on the days {@code [from, to)}, as a segment of this one's kind. */
    S between(LocalDate from, LocalDate to);

    /** The value of {@code segments} on day {@code d} (ch. 3.1), leeg where no segment covers it. */
    static Object read(List<? extends Span<?>> segments, LocalDate d) {
        Span<?> s = at(segments, d);
        return s == null ? null : s.value();
    }

    /** Whether a segment of {@code segments} covers day {@code d}. */
    static boolean covers(List<? extends Span<?>> segments, LocalDate d) {
        return at(segments, d) != null;
    }

    /** The bounds of every segment of {@code timelines} and the two open ends, ordered: the periods between two
     *  adjacent cuts are where no timeline changes its value. */
    static TreeSet<LocalDate> cuts(List<? extends List<? extends Span<?>>> timelines) {
        TreeSet<LocalDate> cuts = new TreeSet<>();
        cuts.add(Period.OPEN_FROM);
        cuts.add(Period.OPEN_TO);
        for (List<? extends Span<?>> t : timelines)
            for (Span<?> p : t) {
                cuts.add(p.from());
                cuts.add(p.to());
            }
        return cuts;
    }

    /** {@code segments} cut to {@code periods}: one fragment per overlap of a segment and a period, with the segment's
     *  value, segment by segment and within a segment period by period. */
    static <S extends Span<S>> List<S> clip(List<? extends S> segments, List<? extends Span<?>> periods) {
        List<S> out = new ArrayList<>();
        for (S s : segments)
            for (Span<?> p : periods) {
                LocalDate from = later(s.from(), p.from()), to = earlier(s.to(), p.to());
                if (from.isBefore(to)) out.add(s.between(from, to));
            }
        return out;
    }

    /** {@code segments} with adjacent segments of one value ({@link Period#sameValue}) joined into one, the value of
     *  the first kept. */
    static <S extends Span<S>> List<S> coalesced(List<? extends S> segments) {
        List<S> out = new ArrayList<>();
        for (S s : segments) {
            S last = out.isEmpty() ? null : out.get(out.size() - 1);
            if (last != null && last.to().equals(s.from()) && Period.sameValue(last.value(), s.value()))
                out.set(out.size() - 1, last.between(last.from(), s.to()));
            else out.add(s);
        }
        return List.copyOf(out);
    }

    static LocalDate later(LocalDate a, LocalDate b) {
        return a.isAfter(b) ? a : b;
    }

    static LocalDate earlier(LocalDate a, LocalDate b) {
        return a.isBefore(b) ? a : b;
    }

    /** The segment of {@code segments} that covers day {@code d}, the first where two do; null where none does. */
    private static Span<?> at(List<? extends Span<?>> segments, LocalDate d) {
        for (Span<?> s : segments) if (!s.from().isAfter(d) && s.to().isAfter(d)) return s;
        return null;
    }
}
