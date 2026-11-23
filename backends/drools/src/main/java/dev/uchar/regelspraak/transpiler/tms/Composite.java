package dev.uchar.regelspraak.transpiler.tms;

import dev.uchar.regelspraak.ir.Condition;

import java.util.concurrent.Callable;

/**
 * The reading of one composite condition (RUNTIME-SEMANTICS ch. 5.10, 1.5): its members read left to right in wire
 * order, each counted by Kleene's strong table, and the read stopping where the judgement stands, every unread member
 * taken as undetermined. A rendering evaluates the members and this counts them, so that both renderings run one reading:
 * before each member {@link #stands()}, then the member's truth value ({@link #read(boolean)}), or the member as a
 * {@link Callable} this reading evaluates and counts also where it throws ({@link #read(Callable)}), where its judgement
 * can be undetermined ({@code Term.Compound.openFrom}), and at the end {@link #verdict()}.
 *
 * <p>Where no member is open the stop is the stop of ch. 5.10: Kleene's table on a prefix with the unread members open is
 * established exactly where a trigger of that chapter fires. An open read in a member makes that member undetermined and
 * drops what the member deposited ({@link OpenRead}); a member that throws {@link Undetermined} counts as undetermined
 * and keeps its deposits. A member read after one that is undetermined is read provisionally (ch. 1.5, "The judgement of
 * a condition"): its truth counts, what it deposits is dropped, and an evaluation fault leaves it undetermined instead of
 * ending the evaluation. Any other exception ends the evaluation, as it does without open values.
 *
 * <p>A quantifier fold is the composite of the members whose filter is not false (ch. 1.5, "A quantifier fold"; a member
 * whose filter is false leaves the table as it is, ch. 3.9a), each its matrix, beside the judgement of its filter where
 * that is undetermined ({@link #read(Undetermined, Callable)}). Every filter is read before every matrix, so where one is
 * undetermined, {@code past} its open read, every matrix is read provisionally.
 */
public final class Composite {
    private final Condition.Quantifier kw;
    private final int threshold;
    private final int n;
    /** The probe the members report their deposits to, or null where the evaluation deposits nothing. */
    private final Object depositFlag;
    private int hits, fails;
    /** The open read of the first member that is undetermined. */
    private OpenMember first;
    /** The open read that makes every member a provisional one, or null. */
    private final OpenMember past;
    /** What the probe held before the member under way, and whether that member is read provisionally. */
    private Object mark;
    private boolean provisional;

    Composite(Condition.Quantifier kw, int threshold, int n, Object depositFlag) {
        this(kw, threshold, n, null, depositFlag);
    }

    Composite(Condition.Quantifier kw, int threshold, int n, OpenMember past, Object depositFlag) {
        this.kw = kw;
        this.threshold = threshold;
        this.n = n;
        this.past = past;
        this.depositFlag = depositFlag;
    }

    /** Whether the judgement stands with the unread members undetermined, so that the read stops here; where not, the
     *  next member is under way. */
    public boolean stands() {
        if (truth(kw, threshold, n, hits, fails) != null) return true;
        provisional = past != null || first != null;
        mark = DepositFlag.mark(depositFlag);
        return false;
    }

    /** The open read that makes the member under way a provisional one, that of the first filter that is undetermined, and
     *  else of the first member read before it that is undetermined; null where the member is not read provisionally. */
    OpenMember past() {
        return !provisional ? null : past != null ? past : first;
    }

    /** The member under way is established, true or false. */
    public void read(boolean holds) {
        if (provisional) DepositFlag.restore(depositFlag, mark);
        count(holds, null);
    }

    /** The member under way, whose judgement can be undetermined: its truth value, or what its evaluation threw, an open
     *  read, an undetermined judgement, or, in a member read provisionally, an evaluation fault, each counted; anything
     *  else passes on. */
    public void read(Callable<Boolean> member) {
        read(null, member);
    }

    /** As {@link #read(Callable)}, of a member of a quantifier fold whose matrix {@code member} evaluates, and whose filter
     *  has the judgement {@code filter}, undetermined, or null where it is true: for {@code all} the member is true where
     *  its filter is false or its matrix true, for the other quantifiers where both are true, by Kleene's table, and it is
     *  undetermined by its filter where that is, else by its matrix. */
    void read(Undetermined filter, Callable<Boolean> member) {
        Boolean holds = null;
        OpenMember open = null;
        try {
            holds = ComputeHelpers.call(member);
            if (provisional) DepositFlag.restore(depositFlag, mark);
        } catch (RuntimeException e) {
            if (e instanceof Undetermined u) {
                open = u.read();
                if (provisional) DepositFlag.restore(depositFlag, mark);
            } else {
                if (e instanceof OpenRead o) open = o.member();
                else if (provisional && ComputeHelpers.isEvaluationFault(e)) open = past();
                else throw e;
                DepositFlag.restore(depositFlag, mark);
            }
        }
        if (filter == null) {
            count(holds, open);
            return;
        }
        boolean excluded = kw == Condition.Quantifier.ALLE || kw == Condition.Quantifier.DE;
        count(holds != null && holds == excluded ? holds : null, filter.read());
    }

    /** The composite's truth where it is established; {@link Undetermined} where it is not. */
    public boolean verdict() {
        Boolean truth = truth(kw, threshold, n, hits, fails);
        if (truth == null) throw new Undetermined(first);
        return truth;
    }

    /** Counts a member that is {@code holds}, null where undetermined, then by the open read {@code read}. */
    private void count(Boolean holds, OpenMember read) {
        if (holds == null) {
            if (first == null) first = read;
        } else if (holds) hits++;
        else fails++;
    }

    /** Kleene's strong table of the quantifier {@code kw}, with the threshold {@code k} of a counting quantifier, over
     *  {@code n} members (RUNTIME-SEMANTICS ch. 1.5), {@code hits} of them true and {@code fails} false, every other one
     *  undetermined or unread: the truth where every way those could come out agrees, null where they do not. Where
     *  every member is read and none is undetermined this is the verdict of ch. 5.10. */
    static Boolean truth(Condition.Quantifier kw, int k, long n, long hits, long fails) {
        return switch (kw) {
            case ALLE, DE -> fails >= 1 ? Boolean.FALSE : hits == n ? Boolean.TRUE : null;
            case GEEN -> hits >= 1 ? Boolean.FALSE : fails == n ? Boolean.TRUE : null;
            case TEN_MINSTE -> hits >= k ? Boolean.TRUE : fails >= n - k + 1 ? Boolean.FALSE : null;
            case TEN_HOOGSTE -> fails >= n - k ? Boolean.TRUE : hits >= k + 1 ? Boolean.FALSE : null;
            case PRECIES -> hits >= k + 1 || fails >= n - k + 1 ? Boolean.FALSE
                    : hits >= k && fails >= n - k ? Boolean.TRUE : null;
        };
    }
}
