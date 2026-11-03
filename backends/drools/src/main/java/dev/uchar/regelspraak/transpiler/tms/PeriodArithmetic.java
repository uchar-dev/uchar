package dev.uchar.regelspraak.transpiler.tms;

import dev.uchar.regelspraak.runtime.Breuk;
import dev.uchar.regelspraak.runtime.RegelSpraakRuntime;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import java.util.function.BinaryOperator;

/**
 * The period arithmetic of RUNTIME-SEMANTICS ch. 3.4 and 3.5 over all time (ch. 3.1), as functions of the values a
 * rule binds: a timeline is a {@code List<Period>}, an open bound {@link Period#OPEN_FROM} or {@link Period#OPEN_TO},
 * and an absent timeline is null, which reads leeg on every day. Plus and min cut their operands at the union of
 * their bounds and the two open ends and apply the operator per period, a leeg operand 0 there (the period-coalescing
 * rule); the sum over the members of a set adds per period the members that are not leeg there, and is leeg where all
 * of them are, or its fallback (the contributor-skipping rule); a scalar operand holds on every day, the one segment
 * with two open bounds. Adjacent periods of one value are one segment. A rule writes the result's filled segments
 * only, a leeg period a gap ({@link #written}), and the subject's own total of such a timeline counts an open segment
 * of 0 as 0 and faults on an open segment of any other value ({@link #total}, ch. 3.6, EF-036).
 *
 * <p>Every value is a decimal: the analysis refuses an operand whose value need not terminate and a restatement
 * between two units, so a sum and a difference of two values terminate as well.
 *
 * <p>Public and free of the rule DSL, a function of plain values, so that a rendering of the branch as DRL text can call
 * it as the exec-model rendering does ({@link Allocation} is the precedent).
 */
public final class PeriodArithmetic {
    private PeriodArithmetic() {}

    /** A scalar operand as a timeline (ch. 3.1): the one segment {@code (−∞, +∞)} that carries {@code value}, and the
     *  empty timeline where it is leeg. */
    public static List<Period> scalar(Object value) {
        BigDecimal v = decimal(value);
        return v == null ? List.of() : List.of(new Period(Period.OPEN_FROM, Period.OPEN_TO, v));
    }

    /** {@code a} plus {@code b} per period of all time (ch. 3.4). */
    public static List<Period> plus(Object a, Object b) {
        return cutMerge(a, b, BigDecimal::add);
    }

    /** {@code a} min {@code b} per period of all time (ch. 3.4). */
    public static List<Period> minus(Object a, Object b) {
        return cutMerge(a, b, BigDecimal::subtract);
    }

    /**
     * The sum per period of the timelines {@code timelines}, the members' contributions in any order (ch. 3.5): on
     * every period between two of their bounds the sum of the members that are not leeg there, and leeg where all of
     * them are, or {@code fallback} where it is not null. Before the first and after the last bound every member is
     * leeg, so the fallback fills all time there, with no member at all as well, and without one that time stays a
     * gap.
     */
    public static List<Period> sum(Object timelines, Object fallback) {
        List<List<Period>> members = new ArrayList<>();
        for (Object t : (List<?>) timelines) members.add(timeline(t));
        BigDecimal fill = decimal(fallback);
        if (members.isEmpty() && fill == null) return List.of();
        TreeSet<LocalDate> cuts = cuts(members);
        if (fill != null) {
            cuts.add(Period.OPEN_FROM);
            cuts.add(Period.OPEN_TO);
        }
        List<Period> out = new ArrayList<>();
        LocalDate previous = null;
        for (LocalDate c : cuts) {
            if (previous != null) {
                BigDecimal total = null;
                for (List<Period> m : members) {
                    BigDecimal v = read(m, previous);
                    if (v != null) total = total == null ? v : total.add(v);
                }
                if (total == null) total = fill;
                if (total != null) out.add(new Period(previous, c, total));
            }
            previous = c;
        }
        return coalesced(out);
    }

    /**
     * The timeline a rule of period arithmetic writes into a target of {@code decimals} declared decimals, the
     * attribute {@code attribute} (ch. 3.1, 3.11): the filled segments of {@code timeline}, adjacent equal ones one
     * segment, each under the write boundary of ch. 2.4 point 1, a value that does not fit the {@link WriteBoundaryFault}
     * of the first such segment. {@code decimals} is null for a target that holds every decimal.
     */
    public static List<Period> written(Object timeline, Integer decimals, String attribute) {
        List<Period> out = new ArrayList<>();
        for (Period p : timeline(timeline)) {
            if (p.value() == null) continue;
            if (decimals != null) {
                WriteBoundaryFault.Misfit m = WriteBoundaryFault.misfit(null, null, attribute, p.value(), decimals);
                if (m != null) throw new WriteBoundaryFault(List.of(m));
            }
            out.add(p);
        }
        return coalesced(out);
    }

    /**
     * The temporal total of ch. 3.6 of {@code timeline}, the attribute {@code attribute} of {@code object}, on the grid
     * {@code granularity}, as {@code rsTijdTotaal} weighs a segment: a segment with an open bound holds unboundedly many
     * units, so with the value 0 it adds 0, and with any other value the total has none, the {@link OpenSegmentFault} of
     * the first such segment (EF-036). A leeg segment adds 0, and an absent timeline totals 0, the per-day reading of a
     * timeline that is leeg on every day: the routes that reach an absent timeline here are the clip, which turns
     * absence into 0, and the subject's own total of a timeline that period arithmetic writes (ch. 3.6).
     */
    public static BigDecimal total(Object timeline, String granularity, String attribute, Object object) {
        List<Period> closed = new ArrayList<>();
        for (Period p : timeline(timeline)) {
            if (p.value() == null) continue;
            if (p.open()) {
                if (p.value().signum() != 0) throw new OpenSegmentFault(attribute, object, p.value());
                continue;
            }
            closed.add(p);
        }
        return RegelSpraakRuntime.rsTijdTotaal(closed, granularity);
    }

    /** Whether {@code timeline} has a segment with an open bound whose value is not 0, whose total has no value
     *  (ch. 3.6): a member of a per-element fold with such a timeline contributes nothing, and its fault stands
     *  instead. Not for the asOfDate-bucket scalar or an absent timeline. */
    public static boolean openNonZero(Object timeline) {
        if (!(timeline instanceof List<?> l)) return false;
        for (Object o : l) {
            Period p = (Period) o;
            if (p.open() && p.value() != null && p.value().signum() != 0) return true;
        }
        return false;
    }

    /** {@code timeline} without its segments with an open bound that carry 0 or leeg, which add 0 to a total
     *  (ch. 3.6): what {@code rsTijdTotaal} weighs of a timeline without an open segment of another value. */
    public static List<Period> withoutOpenZeros(List<?> timeline) {
        List<Period> out = new ArrayList<>();
        for (Object o : timeline) {
            Period p = (Period) o;
            if (p.open() && (p.value() == null || p.value().signum() == 0)) continue;
            out.add(p);
        }
        return out;
    }

    /** The fault of the total of {@code attribute} of the member {@code member} of a per-element fold whose timeline
     *  has an open segment that is not 0 (ch. 3.6, EF-036), the first such segment's value in the sentence; named in no
     *  rule yet. */
    public static OpenSegmentFault openSegmentFault(String attribute, Object member, Object timeline) {
        for (Object o : (List<?>) timeline) {
            Period p = (Period) o;
            if (p.open() && p.value() != null && p.value().signum() != 0)
                return new OpenSegmentFault(attribute, member, p.value());
        }
        throw new IllegalStateException("de tijdlijn van " + attribute + " op " + FactNames.object(member)
                + " heeft geen open segment met een waarde ongelijk aan 0, en de lidfout noemt er een");
    }

    // ---------------- the cut and the merge ----------------

    /** {@code op} per period of all time over the cut set of {@code a} and {@code b} with both open ends, a leeg
     *  operand 0 there (ch. 3.4). */
    private static List<Period> cutMerge(Object a, Object b, BinaryOperator<BigDecimal> op) {
        List<Period> x = timeline(a), y = timeline(b);
        TreeSet<LocalDate> cuts = cuts(List.of(x, y));
        cuts.add(Period.OPEN_FROM);
        cuts.add(Period.OPEN_TO);
        List<Period> out = new ArrayList<>();
        LocalDate previous = null;
        for (LocalDate c : cuts) {
            if (previous != null)
                out.add(new Period(previous, c, op.apply(zeroWhereEmpty(read(x, previous)), zeroWhereEmpty(read(y, previous)))));
            previous = c;
        }
        return coalesced(out);
    }

    /** The bounds of every segment of {@code timelines}, ordered. */
    private static TreeSet<LocalDate> cuts(List<List<Period>> timelines) {
        TreeSet<LocalDate> cuts = new TreeSet<>();
        for (List<Period> t : timelines)
            for (Period p : t) {
                cuts.add(p.from());
                cuts.add(p.to());
            }
        return cuts;
    }

    /** The value of {@code timeline} on day {@code d} (ch. 3.1), leeg where no segment covers it. */
    private static BigDecimal read(List<Period> timeline, LocalDate d) {
        for (Period p : timeline)
            if (!p.from().isAfter(d) && p.to().isAfter(d)) return p.value();
        return null;
    }

    /** {@code segments} with adjacent segments of one value joined into one, the value of the first kept; two values
     *  are one by {@code compareTo}, as {@link Period#equals} compares them. */
    private static List<Period> coalesced(List<Period> segments) {
        List<Period> out = new ArrayList<>();
        for (Period s : segments) {
            Period last = out.isEmpty() ? null : out.get(out.size() - 1);
            if (last != null && last.to().equals(s.from()) && sameValue(last.value(), s.value()))
                out.set(out.size() - 1, new Period(last.from(), s.to(), last.value()));
            else out.add(s);
        }
        return List.copyOf(out);
    }

    private static boolean sameValue(BigDecimal a, BigDecimal b) {
        return a == null ? b == null : b != null && a.compareTo(b) == 0;
    }

    private static BigDecimal zeroWhereEmpty(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    /** {@code t} as a timeline: a {@code List<Period>}, an absent one (null) the empty one. */
    @SuppressWarnings("unchecked")
    private static List<Period> timeline(Object t) {
        return t == null ? List.of() : (List<Period>) t;
    }

    /** A scalar operand or a fallback as a decimal, leeg null. The analysis refuses an operand whose value need not
     *  terminate, so an exact rational here is one that terminates. */
    private static BigDecimal decimal(Object v) {
        if (v == null || v instanceof BigDecimal) return (BigDecimal) v;
        if (v instanceof Breuk b && b.isTerminerend()) return b.naarDecimal();
        throw new IllegalStateException("een operand van het rekenen per periode is " + v + " (" + v.getClass().getName()
                + "), geen decimaal: de analyse weigert een operand die niet eindigt");
    }
}
