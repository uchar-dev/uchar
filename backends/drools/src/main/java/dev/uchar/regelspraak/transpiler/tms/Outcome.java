package dev.uchar.regelspraak.transpiler.tms;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * The control fact of a defaults group in its general form, with exception groups or with more than one group at the
 * root (RUNTIME-SEMANTICS ch. 7.2 and 7.9): the default at {@code positie} in chain {@code keten} of the stack
 * {@code stapel} settled for {@code onderwerp}, deciding {@code waarde} or conflicting under {@code sleutel}.
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
 * <p>Compared by value, the subject by identity, and a decided number by {@code compareTo}, so that two members that
 * decide 10.0 and 10.00 merge into one fact, as the reconciliation reads them as one decision; a characteristic's
 * decision is {@code waarde} as a Boolean.
 */
public record Outcome(Object onderwerp, String stack, int chain, int positie, Kind soort, Object waarde,
                       String sleutel) implements ControlFact {

    /** How a default settled. */
    public enum Kind { DECIDED, CONFLICT }

    public Outcome {
        Objects.requireNonNull(onderwerp, "onderwerp");
        Objects.requireNonNull(stack, "stapel");
        Objects.requireNonNull(soort, "soort");
        if (soort == Kind.DECIDED ? waarde == null || sleutel != null : waarde != null || sleutel == null)
            throw new IllegalArgumentException("een beslissing draagt een waarde, een conflict een sleutel: " + soort
                    + " met waarde " + waarde + " en sleutel " + sleutel);
    }

    public boolean conflict() { return soort == Kind.CONFLICT; }

    /** Two decisions as RUNTIME-SEMANTICS ch. 7.2 compares them: numbers by value, everything else by equality. */
    static boolean equal(Object a, Object b) {
        if (a instanceof BigDecimal x && b instanceof BigDecimal y) return x.compareTo(y) == 0;
        return Objects.equals(a, b);
    }

    @Override public boolean equals(Object o) {
        return o instanceof Outcome u && u.onderwerp == onderwerp && u.stack.equals(stack) && u.chain == chain
                && u.positie == positie && u.soort == soort && equal(u.waarde, waarde)
                && Objects.equals(u.sleutel, sleutel);
    }

    @Override public int hashCode() {
        Object w = waarde instanceof BigDecimal b ? b.stripTrailingZeros() : waarde;
        return Objects.hash(System.identityHashCode(onderwerp), stack, chain, positie, soort, w, sleutel);
    }

    @Override public String toString() {
        return "Uitkomst(" + FactNames.object(onderwerp) + ", " + stack + ", keten " + chain + ", positie " + positie
                + ", " + (soort == Kind.DECIDED ? "beslist " + waarde : "conflict " + sleutel) + ")";
    }
}
