package dev.uchar.regelspraak.transpiler.tms;

import dev.uchar.regelspraak.runtime.Breuk;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * The control fact of a defaults group in its general form, with exception groups or with more than one group at the
 * root (RUNTIME-SEMANTICS ch. 7.2 and 7.9): the default at {@code position} in chain {@code chain} of the stack
 * {@code stack} settled for {@code subject}, deciding {@code value}, conflicting under {@code key}, or open.
 *
 * <p>A chain is the list of defaults of a root group or of one exception group; the lowering numbers them per stack,
 * the root groups first, and the exception groups of one default consecutively, so that "a member before this one" is
 * a range of numbers. A default settles in one of four ways, each a rule that carries {@code not Uitkomst} over the
 * positions above its own in its chain and above each default whose exception group its chain is, at any depth: its
 * own decision, where it has no exception groups; and, where it has them,
 * the first conflict among its members passed on unchanged, a conflict of its own where two members decide
 * differently, or the decision of the first deciding member where the deciding members agree; a default with content
 * of its own besides falls back to its own decision where no member settles. The outcome of a chain is therefore the
 * one fact of its highest settling default, and the reconciliation of a default reads the outcomes of its member chains
 * as positive and negated patterns on this fact. A lower default with exception groups is displaced with its conflict
 * (ch. 7.9): its rules, and those of its exception groups, are under the {@code not} of the higher one.
 *
 * <p>A default that neither decides nor gives way settles {@code OPEN} (RUNTIME-SEMANTICS ch. 1.5, "A stack"), and so does
 * one whose exception groups have an open member and neither conflict nor decide differently: the lower defaults of its
 * chain and its exception groups are under the {@code not} of it as of any outcome, the initialisation of the stack among
 * them, and a reconciliation over a member that is open decides nothing.
 *
 * <p>On a timeline target a default settles per day (RUNTIME-SEMANTICS ch. 7.11): its outcome is {@code DECIDED} with
 * the days it settled as its value, a {@link PeriodDefaults.Days} that holds the days it decides with their values and
 * per conflict key the days on which its members differ; a default and its fall-back each insert one, and a lower
 * default of the chain reads the days they settle off an accumulate instead of their absence
 * ({@code TmsTranspiler.dayStackRules}).
 *
 * <p>Compared by value, the subject by identity, and a decided number by {@code compareTo}, so that two members that
 * decide 10.0 and 10.00 merge into one fact, as the reconciliation reads them as one decision; a characteristic's
 * decision is {@code value} as a Boolean.
 */
public record Outcome(Object subject, String stack, int chain, int position, Kind kind, Object value,
                       String key, boolean provisionalFault) implements ControlFact {

    /** How a default or a chain settled. */
    public enum Kind { DECIDED, CONFLICT, OPEN, OPEN_ONLY }

    /** An outcome that is not a provisional evaluation fault. */
    public Outcome(Object subject, String stack, int chain, int position, Kind kind, Object value, String key) {
        this(subject, stack, chain, position, kind, value, key, false);
    }

    public Outcome {
        Objects.requireNonNull(subject, "subject");
        Objects.requireNonNull(stack, "stack");
        Objects.requireNonNull(kind, "kind");
        if ((kind == Kind.DECIDED || kind == Kind.OPEN_ONLY) != (value != null) || (kind == Kind.CONFLICT) != (key != null))
            throw new IllegalArgumentException("a decision or open-with-constant carries a value, a conflict a key, an open outcome neither: "
                    + kind + " with value " + value + " and key " + key);
        if (provisionalFault && kind != Kind.OPEN)
            throw new IllegalArgumentException("a provisional fault is an open outcome, not " + kind);
    }

    public boolean conflict() { return kind == Kind.CONFLICT; }

    /** Two decisions as RUNTIME-SEMANTICS ch. 7.2 and ch. 1.5 compare them: numbers by exact value, everything else by
     *  equality. */
    static boolean equal(Object a, Object b) {
        if (a instanceof BigDecimal x && b instanceof BigDecimal y) return x.compareTo(y) == 0;
        if ((a instanceof BigDecimal || a instanceof Breuk) && (b instanceof BigDecimal || b instanceof Breuk))
            return Breuk.van(a).compareTo(Breuk.van(b)) == 0;
        return Objects.equals(a, b);
    }

    @Override public boolean equals(Object o) {
        return o instanceof Outcome u && u.subject == subject && u.stack.equals(stack) && u.chain == chain
                && u.position == position && u.kind == kind && equal(u.value, value)
                && Objects.equals(u.key, key) && u.provisionalFault == provisionalFault;
    }

    @Override public int hashCode() {
        Object w = value instanceof BigDecimal b ? b.stripTrailingZeros()
                : value instanceof Breuk br && br.isTerminerend() ? br.naarDecimal().stripTrailingZeros() : value;
        return Objects.hash(System.identityHashCode(subject), stack, chain, position, kind, w, key, provisionalFault);
    }

    @Override public String toString() {
        return "Outcome(" + FactNames.object(subject) + ", " + stack + ", chain " + chain + ", position " + position
                + ", " + switch (kind) {
                    case DECIDED -> "decided " + value;
                    case CONFLICT -> "conflict " + key;
                    case OPEN -> provisionalFault ? "open (provisional fault)" : "open";
                    case OPEN_ONLY -> "open with " + value;
                } + ")";
    }
}
