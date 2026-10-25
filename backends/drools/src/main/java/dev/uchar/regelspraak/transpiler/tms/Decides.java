package dev.uchar.regelspraak.transpiler.tms;

import java.util.Objects;

/**
 * The control fact of a defaults group in its simple form, one chain of defaults without exception groups: default
 * {@code index} of the stack {@code stack} decided for {@code subject}. Every deciding default inserts it beside its
 * write, a do-not-attribute decision in place of one; every lower default carries {@code not Beslist} over the indices
 * above its own. A decision therefore displaces every lower one by truth maintenance alone: when a higher default
 * decides after a lower one already wrote, the lower one's match goes and its write with it, and when the higher
 * decision goes, the lower one matches again. A default whose consequence is leeg inserts nothing, so the lower ones
 * decide (RUNTIME-SEMANTICS ch. 7.1). This is the form A10 of the trial of card drools-logische-afleiding measured as
 * variant M. The lowest deciding default inserts it too (card defaults-decider), which displaces nothing, so that the
 * session names every decision ({@code TmsTranspiler.decisionsOf}).
 *
 * <p>Keyed and compared as {@link HasCharacteristic}: the subject by identity, the stack and the index by value.
 */
public record Decides(Object subject, String stack, int index) implements ControlFact {

    public Decides {
        Objects.requireNonNull(subject, "subject");
        Objects.requireNonNull(stack, "stack");
    }

    @Override public boolean equals(Object o) {
        return o instanceof Decides b && b.subject == subject && b.stack.equals(stack) && b.index == index;
    }

    @Override public int hashCode() {
        return Objects.hash(System.identityHashCode(subject), stack, index);
    }

    @Override public String toString() {
        return "Decides(" + FactNames.object(subject) + ", " + stack + ", " + index + ")";
    }
}
