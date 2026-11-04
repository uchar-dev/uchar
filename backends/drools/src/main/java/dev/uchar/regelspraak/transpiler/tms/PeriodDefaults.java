package dev.uchar.regelspraak.transpiler.tms;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * A defaults group on a timeline target, decided per day (RUNTIME-SEMANTICS ch. 7.11), as functions of the values the
 * rules of its stack bind. A default settles the days of its turn, the days on which no strictly higher default of its
 * chain settled, within those of the default whose exception group its chain is; it decides the days of its turn on
 * which its justification holds and its consequence reads a value. A default with exception groups reconciles them
 * per day, and the group at the root of the stack reconciles its members per day (ch. 7.2, 7.9). What a default or a
 * reconciliation settles is the value of its {@link Outcome}, a {@link Days}, which the lower defaults of its chain read
 * as the days of their turn that it takes.
 *
 * <p>Periods are a {@code List<Period>} without values, maximal and in the order of their days, as
 * {@link PeriodCondition} gives them; a timeline holds exact segments ({@link PeriodArithmetic.Exact}) or segments of
 * working memory ({@link Period}). Public and free of the rule DSL, a function of plain values, so that a rendering of
 * the branch as DRL text calls it as the exec-model rendering does; no function is overloaded, since the DRL-text
 * rendering finds one by its name.
 */
public final class PeriodDefaults {
    private PeriodDefaults() {}

    /** What a default or a reconciliation settled per day: the segments of the days it decides, with their exact
     *  values, in the order of their days, and per conflict key the days on which the members it reconciles differ
     *  (ch. 7.2, 7.9), the keys in their order. The two sets of days are disjoint. Compared by value, so that truth
     *  maintenance keeps one outcome for two equal ones. */
    public record Days(List<PeriodArithmetic.Exact> decided, List<Conflicted> conflicted) {
        public Days {
            decided = List.copyOf(decided);
            conflicted = List.copyOf(conflicted);
        }
    }

    /** The days {@code periods} on which the members under the conflict key {@code key} differ. */
    public record Conflicted(String key, List<Period> periods) {
        public Conflicted {
            periods = List.copyOf(periods);
        }
    }

    /** The outcomes {@code a} and {@code b}, two lists of {@link Outcome} facts that accumulates bound, as one list. */
    public static List<Object> joined(Object a, Object b) {
        List<Object> out = new ArrayList<>((List<?>) a);
        out.addAll((List<?>) b);
        return out;
    }

    /** The days of all time that none of {@code above}, the outcomes of the defaults above a default, settled: the days
     *  of its turn; all time where it has none above it (null). */
    public static List<Period> reach(Object above) {
        return PeriodCondition.complement(above == null ? List.of() : settled(above));
    }

    /** The days of {@code reach} that none of {@code members}, the outcomes of the exception groups of a default,
     *  settled: the days of the turn of the default's own content (ch. 7.9). */
    public static List<Period> left(Object reach, Object members) {
        return PeriodCondition.intersection(PeriodCondition.periodsOf(reach),
                PeriodCondition.complement(settled(members)));
    }

    /** Whether {@code periods} hold a day: a default whose turn holds none is displaced on every day, and the reading of
     *  a default whose justification holds on none does not reach its consequence (ch. 5.12). */
    public static boolean atTurn(Object periods) {
        return periods != null && !((List<?>) periods).isEmpty();
    }

    /** The days {@code a} and {@code b} share, null where they share none: a read of the binder that holds them ends
     *  its {@code Let} with leeg, so that a default whose justification holds on no day of its turn decides nothing
     *  and its consequence is not evaluated (ch. 7.11). */
    public static List<Period> within(Object a, Object b) {
        List<Period> out = PeriodCondition.intersection(PeriodCondition.periodsOf(a), PeriodCondition.periodsOf(b));
        return out.isEmpty() ? null : out;
    }

    /** What a default decides on the days {@code days}: the filled segments of its consequence's timeline
     *  {@code timeline} cut to them; null where it decides none, so that it inserts no outcome. */
    public static Days decided(Object days, Object timeline) {
        List<PeriodArithmetic.Exact> out = cut(timeline, PeriodCondition.periodsOf(days));
        return out.isEmpty() ? null : new Days(out, List.of());
    }

    /** The timeline a default would write, the root of its consequence in a reading (ch. 5.12): the filled segments of
     *  {@code timeline} cut to the periods {@code periods} of its justification, all time where it has none (null). */
    public static List<PeriodArithmetic.Exact> filledWithin(Object periods, Object timeline) {
        return cut(timeline, periods == null ? List.of(new Period(Period.OPEN_FROM, Period.OPEN_TO, null))
                : PeriodCondition.periodsOf(periods));
    }

    /**
     * The days on which the input on the bean, {@code input}, leaves the timeline target {@code attribute} of
     * {@code subject} empty: the justification of the initialisation default per day (ch. 7.3, 7.11), all time where
     * there is none. Where the case gives the input as the asOfDate-bucket scalar ({@code bucket}, the number of
     * {@link BucketInput} facts of the target on the subject, above 0), it carries no period, and the default faults
     * with EF-037 in the reference interpreter's words.
     */
    public static List<Period> inputEmpty(Object input, Object bucket, String attribute, Object subject) {
        if (((Number) bucket).longValue() > 0)
            throw InputFormFault.bucketScalar(attribute, true, "the initialisation default of the timeline target \""
                    + attribute + "\" reads the input per day; the case supplies only the asOfDate-bucket scalar of ",
                    subject, ", which carries no period");
        List<Period> filled = new ArrayList<>();
        for (Period p : input == null ? List.<Period>of() : Period.list((List<?>) input))
            if (p.value() != null) filled.add(new Period(p.from(), p.to(), null));
        return PeriodCondition.complement(PeriodCondition.merged(filled));
    }

    /**
     * The reconciliation per day of the chains of {@code outcomes}, the outcomes of the member chains of a default or of
     * the groups at the root of a stack (ch. 7.2, 7.9, 7.11): per chain, in the order of the chains, what its defaults
     * settled; and on every period between two of their bounds, the first conflict of a chain, in that order, where
     * one stands; else the value the chains that decide there agree on, or a conflict under {@code key} where they
     * differ. Null where it settles no day.
     */
    public static Days reconciled(String key, Object outcomes) {
        Map<Integer, List<Days>> perChain = new TreeMap<>();
        for (Object o : (List<?>) outcomes) {
            Outcome u = (Outcome) o;
            perChain.computeIfAbsent(u.chain(), c -> new ArrayList<>()).add((Days) u.value());
        }
        List<Days> chains = new ArrayList<>();
        for (List<Days> c : perChain.values()) chains.add(join(c));
        TreeSet<LocalDate> cuts = new TreeSet<>(List.of(Period.OPEN_FROM, Period.OPEN_TO));
        for (Days d : chains) {
            for (PeriodArithmetic.Exact s : d.decided()) {
                cuts.add(s.from());
                cuts.add(s.to());
            }
            for (Conflicted c : d.conflicted())
                for (Period p : c.periods()) {
                    cuts.add(p.from());
                    cuts.add(p.to());
                }
        }
        List<PeriodArithmetic.Exact> decided = new ArrayList<>();
        Map<String, List<Period>> conflicted = new TreeMap<>();
        LocalDate previous = null;
        for (LocalDate c : cuts) {
            if (previous != null) {
                String passed = null;
                for (Days d : chains) {
                    for (Conflicted k : d.conflicted())
                        if (covers(k.periods(), previous)) {
                            passed = k.key();
                            break;
                        }
                    if (passed != null) break;
                }
                if (passed != null) conflicted.computeIfAbsent(passed, k -> new ArrayList<>())
                        .add(new Period(previous, c, null));
                else {
                    List<Object> values = new ArrayList<>();
                    for (Days d : chains) {
                        Object v = read(d.decided(), previous);
                        if (v != null) values.add(v);
                    }
                    if (!values.isEmpty()) {
                        boolean agree = true;
                        for (Object v : values) agree &= PeriodArithmetic.sameValue(values.get(0), v);
                        if (agree) decided.add(new PeriodArithmetic.Exact(previous, c, values.get(0)));
                        else conflicted.computeIfAbsent(key, k -> new ArrayList<>()).add(new Period(previous, c, null));
                    }
                }
            }
            previous = c;
        }
        if (decided.isEmpty() && conflicted.isEmpty()) return null;
        List<Conflicted> keyed = new ArrayList<>();
        conflicted.forEach((k, ps) -> keyed.add(new Conflicted(k, PeriodCondition.merged(ps))));
        return new Days(PeriodArithmetic.coalesced(decided), keyed);
    }

    /** The segments {@code days} decides, what the group writes on its target (ch. 7.11); none where it is null. */
    public static List<PeriodArithmetic.Exact> decidedOf(Object days) {
        return days == null ? List.of() : ((Days) days).decided();
    }

    /** Whether the members {@code days} reconciled differ under the conflict key {@code key} on some day: the §9.5
     *  channel then carries one fact under that key on the subject, whatever the number of days (ch. 5.6, 7.11). */
    public static boolean conflicted(Object days, String key) {
        if (days == null) return false;
        for (Conflicted c : ((Days) days).conflicted()) if (c.key().equals(key)) return true;
        return false;
    }

    /** The days {@code outcomes} settled, where they decide or conflict, as maximal periods. */
    private static List<Period> settled(Object outcomes) {
        List<Period> out = new ArrayList<>();
        for (Object o : (List<?>) outcomes) {
            Days d = (Days) ((Outcome) o).value();
            for (PeriodArithmetic.Exact s : d.decided()) out.add(new Period(s.from(), s.to(), null));
            for (Conflicted c : d.conflicted()) out.addAll(c.periods());
        }
        return PeriodCondition.merged(out);
    }

    /** What the defaults of one chain settled, on disjoint days, as one: the decided segments in the order of their days,
     *  and per key its days. */
    private static Days join(List<Days> parts) {
        List<PeriodArithmetic.Exact> decided = new ArrayList<>();
        Map<String, List<Period>> conflicted = new TreeMap<>();
        for (Days d : parts) {
            decided.addAll(d.decided());
            for (Conflicted c : d.conflicted())
                conflicted.computeIfAbsent(c.key(), k -> new ArrayList<>()).addAll(c.periods());
        }
        decided.sort(Comparator.comparing(PeriodArithmetic.Exact::from));
        List<Conflicted> keyed = new ArrayList<>();
        conflicted.forEach((k, ps) -> keyed.add(new Conflicted(k, PeriodCondition.merged(ps))));
        return new Days(decided, keyed);
    }

    /** The filled segments of {@code timeline} cut to {@code periods}, segment by segment, period by period. */
    private static List<PeriodArithmetic.Exact> cut(Object timeline, List<Period> periods) {
        List<PeriodArithmetic.Exact> out = new ArrayList<>();
        for (PeriodArithmetic.Exact s : PeriodArithmetic.pieces(timeline)) {
            if (s.value() == null) continue;
            for (Period p : periods) {
                LocalDate from = s.from().isAfter(p.from()) ? s.from() : p.from();
                LocalDate to = s.to().isBefore(p.to()) ? s.to() : p.to();
                if (from.isBefore(to)) out.add(new PeriodArithmetic.Exact(from, to, s.value()));
            }
        }
        return List.copyOf(out);
    }

    /** The value of {@code segments} on day {@code d}, leeg where no segment covers it. */
    private static Object read(List<PeriodArithmetic.Exact> segments, LocalDate d) {
        for (PeriodArithmetic.Exact s : segments) if (!s.from().isAfter(d) && s.to().isAfter(d)) return s.value();
        return null;
    }

    /** Whether a period of {@code periods} covers day {@code d}. */
    private static boolean covers(List<Period> periods, LocalDate d) {
        for (Period p : periods) if (!p.from().isAfter(d) && p.to().isAfter(d)) return true;
        return false;
    }
}
