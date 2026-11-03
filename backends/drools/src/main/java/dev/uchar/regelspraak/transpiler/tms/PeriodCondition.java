package dev.uchar.regelspraak.transpiler.tms;

import dev.uchar.regelspraak.ir.Condition;
import dev.uchar.regelspraak.runtime.RegelSpraakRuntime;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;

/**
 * A condition read per period on a rule with a timeline target (RUNTIME-SEMANTICS ch. 3.10, the clip generalised;
 * spec/proposals/IMPLICIT-TIME.md points 3 and 4), and the two writers of one timeline target per day (ch. 5.7, point
 * 5), as functions of the values a rule binds. The periods of a condition are a {@code List<Period>} without values,
 * sorted and maximal: two adjacent periods in which the condition holds are one, so the restriction of a timeline to
 * them cuts a segment only where the condition changes. An open bound is {@link Period#OPEN_FROM} or
 * {@link Period#OPEN_TO}, and the periods range over all time (ch. 3.1).
 *
 * <p>Public and free of the rule DSL, a function of plain values, so that a rendering of the branch as DRL text can call
 * it as the exec-model rendering does ({@link PeriodArithmetic} is the precedent); no function is overloaded, since the
 * DRL-text rendering finds one by its name.
 */
public final class PeriodCondition {
    private PeriodCondition() {}

    /** The periods in which a check of the subject's own characteristic on a timeline holds: its true periods
     *  {@code timeline} as maximal periods, none where it has no timeline (ch. 3.1). */
    public static List<Period> truePeriods(Object timeline) {
        return merged(periodsOf(timeline));
    }

    /** The periods of all time outside the true periods {@code timeline}: where a negated check of a characteristic
     *  holds, all time where the characteristic has no timeline. */
    public static List<Period> outside(Object timeline) {
        return complement(merged(periodsOf(timeline)));
    }

    /**
     * The periods of all time on which the comparison {@code operator} of a timeline with a bound holds, read per day by
     * the comparison of ch. 1.2 for a number: a gap day of the timeline reads leeg, so "ongelijk aan" holds on it
     * against a filled bound and "gelijk aan" against a leeg one, and an ordering holds on no leeg side. The timeline
     * and the bound come in the order the comparison names them, the timeline first where {@code timelineFirst}, so a
     * fault of the one that comes first is the one raised; {@code operator} is spelled as {@code Operators.symbol}
     * spells it, with the timeline on its left.
     */
    public static List<Period> where(Object first, Object second, String operator, boolean timelineFirst) {
        Object timeline = timelineFirst ? first : second, bound = timelineFirst ? second : first;
        List<Period> segments = segmentsOf(timeline);
        List<Period> out = new ArrayList<>();
        LocalDate previous = null;
        for (LocalDate c : cuts(List.of(segments))) {
            if (previous != null && RegelSpraakRuntime.rsAggVergelijk(read(segments, previous), bound, operator))
                out.add(new Period(previous, c, null));
            previous = c;
        }
        return merged(out);
    }

    /**
     * The reading of a comparison read per period (RUNTIME-SEMANTICS ch. 5.12): the filled segments of its timeline
     * {@code timeline} cut to the periods {@code periods} in which it holds, with their values, and the gaps of the
     * timeline within those periods, where a leeg side satisfies it, each without a value, in the order of their days.
     */
    public static List<Period> comparisonReading(Object timeline, Object periods) {
        List<Period> filled = new ArrayList<>(), spans = new ArrayList<>();
        for (Period p : segmentsOf(timeline))
            if (p.value() != null) {
                filled.add(p);
                spans.add(new Period(p.from(), p.to(), null));
            }
        List<Period> held = periodsOf(periods);
        List<Period> out = new ArrayList<>();
        for (Period s : filled)
            for (Period h : held) {
                LocalDate from = later(s.from(), h.from()), to = earlier(s.to(), h.to());
                if (from.isBefore(to)) out.add(new Period(from, to, s.value()));
            }
        for (Period g : complement(merged(spans)))
            for (Period h : held) {
                LocalDate from = later(g.from(), h.from()), to = earlier(g.to(), h.to());
                if (from.isBefore(to)) out.add(new Period(from, to, null));
            }
        out.sort(Comparator.comparing(Period::from));
        return List.copyOf(out);
    }

    /**
     * The whole-period qualifier on a timeline target, a truth per bucket of its own grid {@code grid}, "MAAND" or
     * "JAAR" (RUNTIME-SEMANTICS ch. 3.9, "Per bucket on a timeline target"): the buckets of the calendar every day of
     * which lies in {@code periods}, where the test holds; where {@code negated}, every other bucket of all time, also a
     * bucket without a day in {@code periods}. A period keeps the buckets between its first wall and its last one; an
     * open bound keeps its side open, since every bucket beyond the last cut lies in it.
     */
    public static List<Period> wholeBuckets(Object periods, String grid, boolean negated) {
        boolean year = switch (grid) {
            case "JAAR" -> true;
            case "MAAND" -> false;
            default -> throw new IllegalArgumentException("de gehele-periodekwalificator kent geen raster " + grid);
        };
        List<Period> out = new ArrayList<>();
        for (Period p : merged(periodsOf(periods))) {
            LocalDate from = p.from(), to = p.to();
            if (!from.equals(Period.OPEN_FROM)) {
                LocalDate start = bucketStart(from, year);
                if (!start.equals(from)) from = year ? start.plusYears(1) : start.plusMonths(1);
            }
            if (!to.equals(Period.OPEN_TO)) to = bucketStart(to, year);
            if (from.isBefore(to)) out.add(new Period(from, to, null));
        }
        List<Period> buckets = merged(out);
        return negated ? complement(buckets) : buckets;
    }

    /** The members' periods read so far, {@code members}, null before the first, with {@code periods} after them: one
     *  step of the collection of a composite's members, each member read, in the wire order ({@link #composite}). */
    public static List<Object> member(Object members, Object periods) {
        List<Object> out = new ArrayList<>();
        if (members != null) out.addAll((List<?>) members);
        out.add(periodsOf(periods));
        return out;
    }

    /**
     * The periods of all time in which a composite read per period holds: on every day on which its quantifier holds of
     * the members that hold that day, {@code members} the periods of each ({@link #member}), {@code threshold} the
     * threshold of a counting quantifier. Every member is read, without the short circuit of ch. 5.10, which orders the
     * members of one verdict where here each day has its own (spec/proposals/IMPLICIT-TIME.md VB-C3).
     */
    public static List<Period> composite(Object members, Condition.Quantifier quantifier, Integer threshold) {
        List<List<Period>> parts = new ArrayList<>();
        for (Object m : (List<?>) members) parts.add(periodsOf(m));
        List<Period> out = new ArrayList<>();
        LocalDate previous = null;
        for (LocalDate c : cuts(parts)) {
            if (previous != null) {
                long hits = 0;
                for (List<Period> part : parts) if (covers(part, previous)) hits++;
                if (ComputeHelpers.endVerdict(quantifier, threshold, hits, parts.size()))
                    out.add(new Period(previous, c, null));
            }
            previous = c;
        }
        return merged(out);
    }

    /** The periods of a node of a condition read per period that reads no timeline: all time where it holds, none
     *  where it does not (RUNTIME-SEMANTICS ch. 3.10). */
    public static List<Period> always(Object holds) {
        return (Boolean) holds ? List.of(new Period(Period.OPEN_FROM, Period.OPEN_TO, null)) : List.of();
    }

    // ---------------- two writers of one timeline target ----------------

    /** The first day on which two writers of one timeline target both decide and differ (RUNTIME-SEMANTICS ch. 5.7, "Two
     *  writers per day"): the day, the two writers' places in the list of values, and their values on that day. */
    record Clash(LocalDate day, int first, int second, Object firstValue, Object secondValue) {}

    /**
     * The clash of the timelines {@code values}, the {@link Value} facts of one target on one object, in the order of the
     * document: of all pairs of segments of two writers that overlap with different values, the one on the earliest day,
     * and on one day the pair of the writers the document names first; null where no two decide a day differently. A
     * writer decides the days of its segments, and a day its timeline reads leeg on it does not (spec/proposals/
     * IMPLICIT-TIME.md VB 5.1).
     */
    static Clash clash(List<Value> values) {
        Clash out = null;
        for (int i = 0; i < values.size(); i++)
            for (int j = i + 1; j < values.size(); j++)
                for (Period a : segmentsOf(values.get(i).value()))
                    for (Period b : segmentsOf(values.get(j).value())) {
                        if (a.value() == null || b.value() == null || a.value().compareTo(b.value()) == 0) continue;
                        LocalDate from = later(a.from(), b.from());
                        if (!from.isBefore(earlier(a.to(), b.to()))) continue;
                        if (out == null || from.isBefore(out.day())) out = new Clash(from, i, j, a.value(), b.value());
                    }
        return out;
    }

    /**
     * The timeline of a target of two or more writers (RUNTIME-SEMANTICS ch. 5.7, "Two writers per day"): every
     * writer's segments as it wrote them, {@code parts} in the order of the document, a day an earlier writer decides
     * left out of a later one's, so that a shared day of one value lies in the segment of the writer the document names
     * first; and on the days none of them decides, the target's input {@code input}, null where it has none. In the order
     * of their days.
     */
    static List<Period> union(List<List<Period>> parts, List<Period> input) {
        List<Period> covered = new ArrayList<>(), out = new ArrayList<>();
        for (List<Period> part : parts) {
            List<Period> free = complement(merged(covered));
            out.addAll(clip(part, free));
            for (Period p : part) covered.add(new Period(p.from(), p.to(), null));
        }
        if (input != null) out.addAll(clip(input, complement(merged(covered))));
        out.sort(Comparator.comparing(Period::from));
        return List.copyOf(out);
    }

    // ---------------- periods ----------------

    /** {@code segments} cut to {@code periods}, one fragment per overlap of a segment and a period, with the segment's
     *  value, segment by segment. */
    private static List<Period> clip(List<Period> segments, List<Period> periods) {
        List<Period> out = new ArrayList<>();
        for (Period s : segments)
            for (Period p : periods) {
                LocalDate from = later(s.from(), p.from()), to = earlier(s.to(), p.to());
                if (from.isBefore(to)) out.add(new Period(from, to, s.value()));
            }
        return out;
    }

    /** The segments of a timeline of working memory, an absent one (null) none. */
    private static List<Period> segmentsOf(Object timeline) {
        return timeline == null ? List.of() : Period.list((List<?>) timeline);
    }

    /** The segments of {@code timeline} as periods, each without a value; none for an absent timeline. */
    private static List<Period> periodsOf(Object timeline) {
        List<Period> out = new ArrayList<>();
        for (Period p : segmentsOf(timeline)) out.add(new Period(p.from(), p.to(), null));
        return out;
    }

    /** {@code periods} sorted, with overlapping and touching periods joined: the maximal periods. */
    private static List<Period> merged(List<Period> periods) {
        List<Period> sorted = new ArrayList<>();
        for (Period p : periods) if (p.from().isBefore(p.to())) sorted.add(p);
        sorted.sort(Comparator.comparing(Period::from).thenComparing(Period::to));
        List<Period> out = new ArrayList<>();
        for (Period p : sorted) {
            Period last = out.isEmpty() ? null : out.get(out.size() - 1);
            if (last != null && !p.from().isAfter(last.to()))
                out.set(out.size() - 1, new Period(last.from(), later(last.to(), p.to()), null));
            else out.add(new Period(p.from(), p.to(), null));
        }
        return List.copyOf(out);
    }

    /** The periods of all time outside the maximal periods {@code merged}. */
    private static List<Period> complement(List<Period> merged) {
        List<Period> out = new ArrayList<>();
        LocalDate at = Period.OPEN_FROM;
        for (Period p : merged) {
            if (p.from().isAfter(at)) out.add(new Period(at, p.from(), null));
            at = later(at, p.to());
        }
        if (at.isBefore(Period.OPEN_TO)) out.add(new Period(at, Period.OPEN_TO, null));
        return List.copyOf(out);
    }

    /** The bounds of every segment of {@code timelines} and the two open ends, ordered. */
    private static TreeSet<LocalDate> cuts(List<List<Period>> timelines) {
        TreeSet<LocalDate> cuts = new TreeSet<>();
        cuts.add(Period.OPEN_FROM);
        cuts.add(Period.OPEN_TO);
        for (List<Period> t : timelines)
            for (Period p : t) {
                cuts.add(p.from());
                cuts.add(p.to());
            }
        return cuts;
    }

    /** The value of {@code segments} on day {@code d} (ch. 3.1), leeg where no segment covers it. */
    private static Object read(List<Period> segments, LocalDate d) {
        for (Period p : segments) if (!p.from().isAfter(d) && p.to().isAfter(d)) return p.value();
        return null;
    }

    /** Whether a period of {@code periods} covers day {@code d}. */
    private static boolean covers(List<Period> periods, LocalDate d) {
        for (Period p : periods) if (!p.from().isAfter(d) && p.to().isAfter(d)) return true;
        return false;
    }

    /** The first day of the calendar month or year {@code d} lies in. */
    private static LocalDate bucketStart(LocalDate d, boolean year) {
        return year ? d.withDayOfYear(1) : d.withDayOfMonth(1);
    }

    private static LocalDate later(LocalDate a, LocalDate b) { return a.isAfter(b) ? a : b; }

    private static LocalDate earlier(LocalDate a, LocalDate b) { return a.isBefore(b) ? a : b; }
}
