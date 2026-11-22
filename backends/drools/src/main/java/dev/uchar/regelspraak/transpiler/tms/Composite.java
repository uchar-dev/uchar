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
    /** What the probe held before the member under way, and whether that member is read provisionally. */
    private Object mark;
    private boolean provisional;

    Composite(Condition.Quantifier kw, int threshold, int n, Object depositFlag) {
        this.kw = kw;
        this.threshold = threshold;
        this.n = n;
        this.depositFlag = depositFlag;
    }

    /** Whether the judgement for another form stands with the unread members undetermined, so that the read stops
     *  here; where not, the next member is under way. */
    public boolean stands() {
        if (truth(kw, threshold, n, otherHits, otherFails) != null) return true;
        provisional = firstOther != null;
        mark = DepositFlag.mark(depositFlag);
        return false;
    }

    /** The member under way is established, true or false, for the form itself and for another. */
    public void read(boolean holds) {
        if (provisional) DepositFlag.restore(depositFlag, mark);
        count(holds, holds);
    }

    /** The member under way, whose judgement can be undetermined: its truth value, or what its evaluation threw, an open
     *  read, an undetermined judgement, or, in a member read provisionally, an evaluation fault, each counted; anything
     *  else passes on. */
    public void read(Callable<Boolean> member) {
        boolean holds;
        try {
            holds = ComputeHelpers.call(member);
        } catch (RuntimeException e) {
            threw(e);
            return;
        }
        read(holds);
    }

    private void threw(RuntimeException e) {
        if (e instanceof OpenRead o) {
            DepositFlag.restore(depositFlag, mark);
            undetermined(o.member());
        } else if (e instanceof Undetermined u) {
            if (provisional) DepositFlag.restore(depositFlag, mark);
            if (u.own() == null) undetermined(u.read());
            else {
                count(u.own(), null);
                if (firstOther == null) firstOther = u.read();
            }
        } else if (provisional && ComputeHelpers.isEvaluationFault(e)) {
            DepositFlag.restore(depositFlag, mark);
            undetermined(firstOther);
        } else throw e;
    }

    /** The composite's truth where it is established for both forms; {@link Undetermined} where it is not. */
    public boolean verdict() {
        Boolean own = truth(kw, threshold, n, ownHits, ownFails), other = truth(kw, threshold, n, otherHits, otherFails);
        if (own == null) throw new Undetermined(null, first);
        if (other == null) throw new Undetermined(own, firstOther);
        return own;
    }

    private void undetermined(OpenMember read) {
        count(null, null);
        if (first == null) first = read;
        if (firstOther == null) firstOther = read;
    }

    private void count(Boolean own, Boolean other) {
        if (own != null) {
            if (own) ownHits++;
            else ownFails++;
        }
        if (other != null) {
            if (other) otherHits++;
            else otherFails++;
        }
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
