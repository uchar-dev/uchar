package dev.uchar.regelspraak.transpiler.tms;

import dev.uchar.regelspraak.ir.Condition;

import java.util.concurrent.Callable;

/**
 * The reading of one composite condition (RUNTIME-SEMANTICS ch. 5.10, 1.5): its members read left to right in wire
 * order, each counted for the form itself and for another by Kleene's strong table, and the read stopping where the
 * judgement for another form stands, every unread member taken as undetermined. A rendering evaluates the members and
 * this counts them, so that both renderings run one reading: before each member {@link #stands()}, then the member's
 * truth value ({@link #read(boolean)}), or the member as a {@link Callable} this reading evaluates and counts also where
 * it throws ({@link #read(Callable)}), where its judgement can be undetermined
 * ({@code Term.Compound.openFrom}), and at the end {@link #verdict()}.
 *
 * <p>Where no member is open the two counts are one, and the stop is the stop of ch. 5.10: Kleene's table on a prefix
 * with the unread members open is established exactly where a trigger of that chapter fires. An open read in a member
 * makes that member undetermined and drops what the member deposited ({@link OpenRead}); a member that throws
 * {@link Undetermined} counts as its judgement says and keeps its deposits. A member read after one undetermined for
 * another form is read provisionally (ch. 1.5, "The judgement of a condition"): its truth counts, what it deposits is
 * dropped, and an evaluation fault leaves it undetermined for both instead of ending the evaluation. Any other
 * exception ends the evaluation, as it does without open values.
 *
 * <p>A quantifier fold is the composite of the members whose filter is not false for another form (ch. 1.5, "A quantifier
 * fold"; a member whose filter is false leaves the table as it is, ch. 3.9a), each its matrix, beside the judgement of its
 * filter where that does not stand ({@link #read(Undetermined, Callable)}). Every filter is read before every matrix, so
 * where one does not stand, {@code past} its open read, every matrix is read provisionally.
 */
public final class Composite {
    private final Condition.Quantifier kw;
    private final int threshold;
    private final int n;
    /** The probe the members report their deposits to, or null where the evaluation deposits nothing. */
    private final Object depositFlag;
    private int ownHits, ownFails, otherHits, otherFails;
    /** The open read of the first member undetermined for the form itself, and of the first undetermined for another. */
    private OpenMember first, firstOther;
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

    /** Whether the judgement for another form stands with the unread members undetermined, so that the read stops
     *  here; where not, the next member is under way. */
    public boolean stands() {
        if (truth(kw, threshold, n, otherHits, otherFails) != null) return true;
        provisional = past != null || firstOther != null;
        mark = DepositFlag.mark(depositFlag);
        return false;
    }

    /** The open read that makes the member under way a provisional one, that of the first filter that does not stand, and
     *  else of the first member read before it that is undetermined for another form; null where the member is not read
     *  provisionally. */
    OpenMember past() {
        return !provisional ? null : past != null ? past : firstOther;
    }

    /** The member under way is established, true or false, for the form itself and for another. */
    public void read(boolean holds) {
        if (provisional) DepositFlag.restore(depositFlag, mark);
        count(holds, holds, null, null);
    }

    /** The member under way, whose judgement can be undetermined: its truth value, or what its evaluation threw, an open
     *  read, an undetermined judgement, or, in a member read provisionally, an evaluation fault, each counted; anything
     *  else passes on. */
    public void read(Callable<Boolean> member) {
        read(null, member);
    }

    /** As {@link #read(Callable)}, of a member of a quantifier fold whose matrix {@code member} evaluates, and whose filter
     *  has the judgement {@code filter}, which does not stand for another form, or null where it is true: for {@code all}
     *  the member is true where its filter is false or its matrix true, for the other quantifiers where both are true, by
     *  Kleene's table, and it is undetermined by its filter where that is, else by its matrix. */
    void read(Undetermined filter, Callable<Boolean> member) {
        Boolean own = null, other = null;
        OpenMember open = null;
        try {
            own = other = ComputeHelpers.call(member);
            if (provisional) DepositFlag.restore(depositFlag, mark);
        } catch (RuntimeException e) {
            if (e instanceof Undetermined u) {
                own = u.own();
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
            count(own, other, open, open);
            return;
        }
        boolean excluded = kw == Condition.Quantifier.ALLE || kw == Condition.Quantifier.DE;
        count(filter.own() == null ? whatever(own, excluded) : filter.own() ? own : (Boolean) excluded, whatever(other, excluded),
                filter.own() == null ? filter.read() : open, filter.read());
    }

    /** What a member is whatever its filter: {@code excluded}, the truth of a member whose filter is false, where
     *  {@code matrix}, the truth of its matrix or null, is that too; else null, undetermined. */
    private static Boolean whatever(Boolean matrix, boolean excluded) {
        return matrix != null && matrix == excluded ? matrix : null;
    }

    /** The composite's truth where it is established for both forms; {@link Undetermined} where it is not. */
    public boolean verdict() {
        Boolean own = truth(kw, threshold, n, ownHits, ownFails), other = truth(kw, threshold, n, otherHits, otherFails);
        if (own == null) throw new Undetermined(null, first);
        if (other == null) throw new Undetermined(own, firstOther);
        return own;
    }

    /** Counts a member that is {@code own} for the form itself and {@code other} for another, null where undetermined, and
     *  then by the open read {@code ownRead} and {@code otherRead}. */
    private void count(Boolean own, Boolean other, OpenMember ownRead, OpenMember otherRead) {
        if (own == null) {
            if (first == null) first = ownRead;
        } else if (own) ownHits++;
        else ownFails++;
        if (other == null) {
            if (firstOther == null) firstOther = otherRead;
        } else if (other) otherHits++;
        else otherFails++;
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
