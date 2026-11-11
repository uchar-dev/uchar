package dev.uchar.regelspraak.transpiler.tms;

import dev.uchar.regelspraak.core.FixedStartPoint;
import dev.uchar.regelspraak.model.RegelSpraakModel;
import dev.uchar.regelspraak.runtime.Breuk;
import dev.uchar.regelspraak.runtime.RegelSpraakRuntime;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import java.util.function.BinaryOperator;

/**
 * The period arithmetic of RUNTIME-SEMANTICS ch. 3.4 and 3.5 over all time (ch. 3.1), as functions of the values a
 * rule binds: a timeline is a {@code List<Period>}, an open bound {@link Period#OPEN_FROM} or {@link Period#OPEN_TO},
 * and an absent timeline is null, which reads leeg on every day. An operator of the arithmetic table of ch. 1.2 other
 * than the power cuts its operands at the union of their bounds and the two open ends and applies its scalar operator
 * of the runtime per period, with that operator's own leeg rule (the period-coalescing rule of plus and min, the left
 * propagation of verminderd met, the divisor rule of a division); the sum over the members of a set adds per period
 * the members that are not leeg there, and is leeg where all of them are, or its fallback (the contributor-skipping
 * rule); a scalar operand holds on every day, the one segment with two open bounds. Adjacent periods of one value are
 * one segment. A rule writes the result's filled segments only, a leeg period a gap ({@link #written}), and the total of
 * such a timeline counts an open segment of 0 as 0 and faults on an open segment of any other value ({@link #total},
 * ch. 3.6, EF-036).
 *
 * <p>The values between the operators are exact, a decimal or a rational ({@link Breuk}), as the scalar operators of
 * the runtime give them, so that a quotient that does not terminate reaches the next operator as it is: the timeline
 * an operator returns holds such values, and only the written timeline holds decimals, the segment class of this
 * branch ({@link Period}).
 *
 * <p>Public and free of the rule DSL, a function of plain values, so that a rendering of the branch as DRL text can call
 * it as the exec-model rendering does ({@link Allocation} is the precedent).
 */
public final class PeriodArithmetic {
    private PeriodArithmetic() {}

    /** One segment of a timeline on its way to a written one: from day {@code from} up to {@code to}, its value exact
     *  (a decimal or a {@link Breuk}) or leeg (null). Public so that a reading can name the timeline an operator
     *  computes (RUNTIME-SEMANTICS ch. 5.12). */
    public record Exact(LocalDate from, LocalDate to, Object value) {}

    /** A scalar operand as a timeline (ch. 3.1): the one segment {@code (−∞, +∞)} that carries {@code value}, and the
     *  empty timeline where it is leeg. */
    public static List<?> scalar(Object value) {
        return value == null ? List.of() : List.of(new Exact(Period.OPEN_FROM, Period.OPEN_TO, value));
    }

    /** {@code a} plus {@code b} per period of all time, a leeg operand 0 (ch. 3.4). */
    public static List<?> plus(Object a, Object b) {
        return cutMerge(a, b, RegelSpraakRuntime::rsPlus);
    }

    /** {@code a} min {@code b} per period of all time, a leeg operand 0 (ch. 3.4). */
    public static List<?> minus(Object a, Object b) {
        return cutMerge(a, b, RegelSpraakRuntime::rsMin);
    }

    /** {@code a} verminderd met {@code b} per period of all time: leeg where {@code a} is, a leeg {@code b} 0 (ch. 1.2,
     *  3.4). */
    public static List<?> reduced(Object a, Object b) {
        return cutMerge(a, b, RegelSpraakRuntime::rsVerminderd);
    }

    /** {@code a} maal {@code b} per period of all time, a leeg operand 0 (ch. 1.2, 3.4). */
    public static List<?> times(Object a, Object b) {
        return cutMerge(a, b, RegelSpraakRuntime::rsMaal);
    }

    /** {@code a} percent of {@code b} per period of all time, a leeg operand 0 (ch. 1.2, 3.4). */
    public static List<?> percentageOf(Object a, Object b) {
        return cutMerge(a, b, RegelSpraakRuntime::rsPercentageVan);
    }

    /** {@code a} gedeeld door {@code b} per period of all time (ch. 1.2, 3.4), as {@link ComputeHelpers#divided} divides
     *  one value: a leeg dividend 0, and a leeg or numeric-zero divisor under a filled dividend leeg in that period,
     *  heard by {@code depositFlag} with its code; the probe keeps each code once, so the rule × subject carries one
     *  §9.5 fact however many periods divide so (ch. 5.6). */
    public static List<?> divided(Object a, Object b, Object depositFlag) {
        return cutMerge(a, b, (x, y) -> ComputeHelpers.divided(x, y, depositFlag));
    }

    /** As {@link #divided}, each quotient truncated toward zero at 5 decimals ({@link ComputeHelpers#dividedAbs}). */
    public static List<?> dividedAbs(Object a, Object b, Object depositFlag) {
        return cutMerge(a, b, (x, y) -> ComputeHelpers.dividedAbs(x, y, depositFlag));
    }

    /**
     * The timeline of the time-dependent parameter {@code name} as an operand (ch. 3.4, 8.3): the timeline the case
     * gives it, read whole, and where the case gives none the fault of an unbound parameter in the reference
     * interpreter's words (EF-014, ch. 5.7): a parameter's timeline is its binding.
     */
    public static Object parameter(Object timeline, String name) {
        if (timeline == null) throw new ComputeFault(ComputeFault.unboundParameter(name));
        return timeline;
    }

    /**
     * The sum per period of the timelines {@code timelines}, the members' contributions in any order (ch. 3.5): on
     * every period between two of their bounds the sum of the members that are not leeg there, and leeg where all of
     * them are, or {@code fallback} where it is not null. Before the first and after the last bound every member is
     * leeg, so the fallback fills all time there, with no member at all as well, and without one that time stays a
     * gap.
     */
    public static List<?> sum(Object timelines, Object fallback) {
        List<List<Exact>> members = new ArrayList<>();
        for (Object t : (List<?>) timelines) members.add(pieces(t));
        if (members.isEmpty() && fallback == null) return List.of();
        TreeSet<LocalDate> cuts = cuts(members);
        if (fallback != null) {
            cuts.add(Period.OPEN_FROM);
            cuts.add(Period.OPEN_TO);
        }
        List<Exact> out = new ArrayList<>();
        LocalDate previous = null;
        for (LocalDate c : cuts) {
            if (previous != null) {
                Object total = null;
                for (List<Exact> m : members) {
                    Object v = read(m, previous);
                    if (v != null) total = total == null ? v : RegelSpraakRuntime.rsPlus(total, v);
                }
                if (total == null) total = fallback;
                if (total != null) out.add(new Exact(previous, c, total));
            }
            previous = c;
        }
        return coalesced(out);
    }

    /**
     * The timeline {@code timeline} restricted to the periods {@code periods} in which its rule's condition holds
     * (RUNTIME-SEMANTICS ch. 3.10, the clip generalised; {@link PeriodCondition}): one fragment per overlap of a segment
     * and a period, with the segment's value, segment by segment, none for an absent timeline. A segment of working
     * memory stays a {@link Period}, one an operator returned stays exact. The periods come first, since the condition
     * is read before the value.
     */
    public static List<?> clipped(Object periods, Object timeline) {
        List<Object> out = new ArrayList<>();
        if (timeline == null) return out;
        for (Object o : (List<?>) timeline) {
            LocalDate from = o instanceof Exact e ? e.from() : ((Period) o).from();
            LocalDate to = o instanceof Exact e ? e.to() : ((Period) o).to();
            for (Object c : (List<?>) periods) {
                Period p = (Period) c;
                LocalDate f = from.isAfter(p.from()) ? from : p.from(), t = to.isBefore(p.to()) ? to : p.to();
                if (!f.isBefore(t)) continue;
                out.add(o instanceof Exact e ? new Exact(f, t, e.value()) : new Period(f, t, ((Period) o).value()));
            }
        }
        return List.copyOf(out);
    }

    /** {@code timeline} rounded per segment (ch. 2.5) to {@code decimals} decimals by {@code mode}, a leeg segment leeg,
     *  and adjacent segments that round to one value one segment (spec/proposals/IMPLICIT-TIME.md VB-C19): the rounding of
     *  a value per day, which a segment holds on each of its days. */
    public static List<?> rounded(Object timeline, Integer decimals, java.math.RoundingMode mode) {
        List<Exact> out = new ArrayList<>();
        for (Exact p : pieces(timeline))
            out.add(new Exact(p.from(), p.to(), RegelSpraakRuntime.rsAfronding(p.value(), decimals, mode)));
        return coalesced(out);
    }

    /**
     * The day count of ch. 3.8 over all time, from {@code counted}, the segments a runtime helper returns for it, one
     * per bucket with a true day, in the order of their days: those buckets as they are, and 0 on every period of all
     * time outside them, each run of buckets without a true day one segment, open where it reaches before the first
     * counted bucket or past the last, and the one segment (−∞, +∞) where no bucket has a true day (maintainer decision
     * 2026-11-03, spec/proposals/IMPLICIT-TIME.md 7a). The counted buckets stay one segment each, also where two
     * adjacent ones count alike.
     */
    public static List<Period> dayCount(Object counted) {
        List<Period> out = new ArrayList<>();
        LocalDate at = Period.OPEN_FROM;
        for (Period b : Period.list((List<?>) counted)) {
            if (b.from().isAfter(at)) out.add(new Period(at, b.from(), BigDecimal.ZERO));
            out.add(b);
            at = b.to();
        }
        if (at.isBefore(Period.OPEN_TO)) out.add(new Period(at, Period.OPEN_TO, BigDecimal.ZERO));
        return List.copyOf(out);
    }

    /**
     * The timeline a rule of period arithmetic writes into a target of {@code decimals} declared decimals, the
     * attribute {@code attribute} (ch. 3.1, 3.11): the filled segments of {@code timeline}, adjacent equal ones one
     * segment, each under the write boundary of ch. 2.4 point 1, a value that does not fit the {@link WriteBoundaryFault}
     * of the first such segment. {@code decimals} is null for a target of the unbounded form, which holds every value;
     * a segment of this branch holds a decimal, so a value there that does not terminate faults by name as a limit of
     * this branch ({@link ComputeFault#nonTerminatingSegment}).
     */
    public static List<Period> written(Object timeline, Integer decimals, String attribute) {
        List<Period> out = new ArrayList<>();
        for (Exact p : pieces(timeline)) {
            if (p.value() == null) continue;
            Breuk q = Breuk.van(p.value());
            if (decimals != null) {
                WriteBoundaryFault.Misfit m = WriteBoundaryFault.misfit(null, null, attribute, p.value(), decimals);
                if (m != null) throw new WriteBoundaryFault(List.of(m));
            } else if (!q.isTerminerend())
                throw ComputeFault.nonTerminatingSegment(attribute, WriteBoundaryFault.text(q), bound(p.from()),
                        bound(p.to()));
            out.add(new Period(p.from(), p.to(), q.naarDecimal()));
        }
        List<Period> joined = new ArrayList<>();
        for (Period s : out) {
            Period last = joined.isEmpty() ? null : joined.get(joined.size() - 1);
            if (last != null && last.to().equals(s.from()) && last.number().compareTo(s.number()) == 0)
                joined.set(joined.size() - 1, new Period(last.from(), s.to(), last.value()));
            else joined.add(s);
        }
        return List.copyOf(joined);
    }

    /**
     * The timeline a rule writes into a target of a sum type (RUNTIME-SEMANTICS ch. 9.4, 3.11): the filled segments of
     * {@code timeline}, constructor values, adjacent equal ones one segment. A constructor value has no decimals, so no
     * write boundary applies.
     */
    public static List<Period> writtenValues(Object timeline) {
        List<Period> joined = new ArrayList<>();
        for (Exact p : pieces(timeline)) {
            if (p.value() == null) continue;
            Period last = joined.isEmpty() ? null : joined.get(joined.size() - 1);
            if (last != null && last.to().equals(p.from()) && Period.sameValue(last.value(), p.value()))
                joined.set(joined.size() - 1, new Period(last.from(), p.to(), last.value()));
            else joined.add(new Period(p.from(), p.to(), p.value()));
        }
        return List.copyOf(joined);
    }

    /**
     * The temporal total of ch. 3.6 of {@code timeline}, the attribute {@code attribute} of {@code object}, on the grid
     * {@code granularity} laid from the fixed start point {@code fixed} (null: the calendar's grid), each closed segment
     * weighed as {@link #weighed} weighs it, by the calendar where {@code calendar} names the time unit of the rate's
     * denominator: a segment with an open bound holds unboundedly many units, so with the value 0 it adds 0, and with
     * any other value the total has none, the {@link OpenSegmentFault} of the first such segment (EF-036). A leeg
     * segment adds 0, and an absent timeline totals 0, the per-day reading of a timeline that is leeg on every day
     * (ch. 3.6, spec/proposals/IMPLICIT-TIME.md 12c).
     */
    public static Object total(Object timeline, String granularity, String fixed, String calendar, String attribute,
                               Object object) {
        List<Period> closed = new ArrayList<>();
        for (Object o : timeline == null ? List.of() : (List<?>) timeline) {
            Period p = (Period) o;
            if (p.value() == null) continue;
            if (p.open()) {
                if (p.number().signum() != 0) throw new OpenSegmentFault(attribute, object, p.number());
                continue;
            }
            closed.add(p);
        }
        return weighed(closed, granularity, fixed, calendar);
    }

    /**
     * The total over time of the closed segments {@code segments} on the grid {@code granularity} laid from the fixed
     * start point {@code fixed} (null: the calendar's grid) (ch. 3.6): each segment's value times its length in units
     * of the grid ({@link #gridMeasure}), a part of a unit by its days (spec/proposals/IMPLICIT-TIME.md 12a). Where
     * {@code calendar} names a time unit of the other family of the calendar than the grid's ("DAG" or "WEEK" against
     * "MAAND", "KWARTAAL" or "JAAR"), the segment is measured in that unit by the actual length of the calendar unit each
     * day lies in ({@link #measure}): a rate per year over a day grid weighs a day by 1/365 or 1/366. The total is the
     * exact rational the lengths give, as a decimal where it terminates.
     */
    public static Object weighed(List<?> segments, String granularity, String fixed, String calendar) {
        Breuk sum = Breuk.van(BigInteger.ZERO, BigInteger.ONE);
        for (Period p : Period.list(segments)) {
            if (p.value() == null || !p.from().isBefore(p.to())) continue;
            // an open bound holds unboundedly many units; the callers take such a segment out or fault on it first
            if (p.open())
                throw new IllegalStateException("the total over time meets no segment with an open bound: " + p);
            Breuk length = calendar == null ? gridMeasure(granularity, fixed, p.from(), p.to())
                    : measure(calendar, p.from(), p.to());
            sum = sum.optellen(Breuk.van(p.value()).vermenigvuldigen(length));
        }
        return sum.isTerminerend() ? sum.naarDecimal() : sum;
    }

    /** How many units of the grid {@code granularity} laid from the fixed start point {@code fixed} the days of
     *  {@code [from, upTo)} make (ch. 3.6): a day counts 1/len of the period of the grid that holds it, len that period's
     *  length in days, so a whole period counts 1 and a part of one its share of the days. On the calendar's grid
     *  ({@code fixed} null) that period is the calendar unit ({@link #measure}); a fixed start point lays the periods
     *  of its own grid ({@link FixedStartPoint#period}), as the case input lays the bucket of the evaluation date. */
    static Breuk gridMeasure(String granularity, String fixed, LocalDate from, LocalDate upTo) {
        if (fixed == null) return measure(granularity, from, upTo);
        RegelSpraakModel.Granularity g = RegelSpraakModel.Granularity.valueOf(granularity);
        Breuk acc = Breuk.van(BigInteger.ZERO, BigInteger.ONE);
        LocalDate d = from;
        while (d.isBefore(upTo)) {
            LocalDate[] period = FixedStartPoint.period(g, fixed, d);
            LocalDate e = period[1].isBefore(upTo) ? period[1] : upTo;
            acc = acc.optellen(Breuk.van(BigInteger.valueOf(ChronoUnit.DAYS.between(d, e)),
                    BigInteger.valueOf(ChronoUnit.DAYS.between(period[0], period[1]))));
            d = e;
        }
        return acc;
    }

    /** How many {@code unit} the days of {@code [from, upTo)} make by the calendar (ch. 3.6): a day is one dag and a
     *  seventh of a week, and 1/len of the calendar month, quarter or year it lies in, len its actual length in days
     *  (28 to 31, 90 to 92, 365 or 366). */
    static Breuk measure(String unit, LocalDate from, LocalDate upTo) {
        if (!from.isBefore(upTo)) return Breuk.van(BigInteger.ZERO, BigInteger.ONE);
        long days = ChronoUnit.DAYS.between(from, upTo);
        if (unit.equals("DAG")) return Breuk.van(BigInteger.valueOf(days), BigInteger.ONE);
        if (unit.equals("WEEK")) return Breuk.van(BigInteger.valueOf(days), BigInteger.valueOf(7));
        Breuk acc = Breuk.van(BigInteger.ZERO, BigInteger.ONE);
        LocalDate d = from;
        while (d.isBefore(upTo)) {
            LocalDate start = switch (unit) {
                case "MAAND" -> d.withDayOfMonth(1);
                case "KWARTAAL" -> LocalDate.of(d.getYear(), (d.getMonthValue() - 1) / 3 * 3 + 1, 1);
                case "JAAR" -> d.withDayOfYear(1);
                default -> throw new IllegalArgumentException("the calendar gives " + unit + " no length in days");
            };
            LocalDate next = start.plusMonths(unit.equals("MAAND") ? 1 : unit.equals("KWARTAAL") ? 3 : 12);
            LocalDate e = next.isBefore(upTo) ? next : upTo;
            acc = acc.optellen(Breuk.van(BigInteger.valueOf(ChronoUnit.DAYS.between(d, e)),
                    BigInteger.valueOf(ChronoUnit.DAYS.between(start, next))));
            d = e;
        }
        return acc;
    }

    /** Whether {@code timeline} has a segment with an open bound whose value is not 0, whose total has no value
     *  (ch. 3.6): a member of a per-element fold with such a timeline contributes nothing, and its fault stands
     *  instead. Not for the asOfDate-bucket scalar or an absent timeline. */
    public static boolean openNonZero(Object timeline) {
        if (!(timeline instanceof List<?> l)) return false;
        for (Object o : l) {
            Period p = (Period) o;
            if (p.open() && p.value() != null && p.number().signum() != 0) return true;
        }
        return false;
    }

    /** {@code timeline} without its segments with an open bound that carry 0 or leeg, which add 0 to a total
     *  (ch. 3.6): what {@link #weighed} weighs of a timeline without an open segment of another value. */
    public static List<Period> withoutOpenZeros(List<?> timeline) {
        List<Period> out = new ArrayList<>();
        for (Object o : timeline) {
            Period p = (Period) o;
            if (p.open() && (p.value() == null || p.number().signum() == 0)) continue;
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
            if (p.open() && p.value() != null && p.number().signum() != 0)
                return new OpenSegmentFault(attribute, member, p.number());
        }
        throw new IllegalStateException("the timeline of " + attribute + " on " + FactNames.object(member)
                + " has no open segment with a value other than 0, and the member fault names one");
    }

    // ---------------- the cut and the merge ----------------

    /** {@code op} per period of all time over the cut set of {@code a} and {@code b} with both open ends, on the values
     *  the two read there, leeg where a timeline has no segment (ch. 3.4): the operator applies its own leeg rule. */
    private static List<Exact> cutMerge(Object a, Object b, BinaryOperator<Object> op) {
        List<Exact> x = pieces(a), y = pieces(b);
        TreeSet<LocalDate> cuts = cuts(List.of(x, y));
        cuts.add(Period.OPEN_FROM);
        cuts.add(Period.OPEN_TO);
        List<Exact> out = new ArrayList<>();
        LocalDate previous = null;
        for (LocalDate c : cuts) {
            if (previous != null) out.add(new Exact(previous, c, op.apply(read(x, previous), read(y, previous))));
            previous = c;
        }
        return coalesced(out);
    }

    /** The bounds of every segment of {@code timelines}, ordered. */
    private static TreeSet<LocalDate> cuts(List<List<Exact>> timelines) {
        TreeSet<LocalDate> cuts = new TreeSet<>();
        for (List<Exact> t : timelines)
            for (Exact p : t) {
                cuts.add(p.from());
                cuts.add(p.to());
            }
        return cuts;
    }

    /** The value of {@code timeline} on day {@code d} (ch. 3.1), leeg where no segment covers it. */
    private static Object read(List<Exact> timeline, LocalDate d) {
        for (Exact p : timeline)
            if (!p.from().isAfter(d) && p.to().isAfter(d)) return p.value();
        return null;
    }

    /** {@code segments} with adjacent segments of one value ({@link Period#sameValue}) joined into one, the value of
     *  the first kept. */
    static List<Exact> coalesced(List<Exact> segments) {
        List<Exact> out = new ArrayList<>();
        for (Exact s : segments) {
            Exact last = out.isEmpty() ? null : out.get(out.size() - 1);
            if (last != null && last.to().equals(s.from()) && Period.sameValue(last.value(), s.value()))
                out.set(out.size() - 1, new Exact(last.from(), s.to(), last.value()));
            else out.add(s);
        }
        return List.copyOf(out);
    }

    /** {@code t} as exact segments: a timeline of working memory ({@link Period}), one an operator returned, or an
     *  absent one (null), the empty one. */
    static List<Exact> pieces(Object t) {
        if (t == null) return List.of();
        List<Exact> out = new ArrayList<>();
        for (Object o : (List<?>) t)
            out.add(o instanceof Exact e ? e : new Exact(((Period) o).from(), ((Period) o).to(), ((Period) o).value()));
        return out;
    }

    /** A bound as a sentence writes it, an open one {@code -inf} or {@code +inf}. */
    private static String bound(LocalDate d) {
        return d.equals(Period.OPEN_FROM) ? "-inf" : d.equals(Period.OPEN_TO) ? "+inf" : d.toString();
    }
}
