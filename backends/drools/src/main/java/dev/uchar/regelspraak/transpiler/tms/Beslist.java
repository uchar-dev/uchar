package dev.uchar.regelspraak.transpiler.tms;

import java.util.Objects;

/**
 * The control fact of a defaults group in its simple form, one chain of defaults without exception groups: default
 * {@code index} of the stack {@code stapel} decided for {@code onderwerp}. A default inserts it, beside its write, only
 * when a lower default of the stack has a rule; every such lower default carries {@code not Beslist} over the indices
 * above its own. A decision therefore displaces every lower one by truth maintenance alone: when a higher default
 * decides after a lower one already wrote, the lower one's match goes and its write with it, and when the higher
 * decision goes, the lower one matches again. A default whose consequence is leeg inserts nothing, so the lower ones
 * decide (RUNTIME-SEMANTICS ch. 7.1). This is the form the trial's A10 measured as variant M.
 *
 * <p>Keyed and compared as {@link HeeftKenmerk}: the subject by identity, the stack and the index by value.
 */
public record Beslist(Object onderwerp, String stapel, int index) implements Stuurfeit {

    public Beslist {
        Objects.requireNonNull(onderwerp, "onderwerp");
        Objects.requireNonNull(stapel, "stapel");
    }

    @Override public boolean equals(Object o) {
        return o instanceof Beslist b && b.onderwerp == onderwerp && b.stapel.equals(stapel) && b.index == index;
    }

    @Override public int hashCode() {
        return Objects.hash(System.identityHashCode(onderwerp), stapel, index);
    }

    @Override public String toString() {
        return "Beslist(" + Feitnamen.object(onderwerp) + ", " + stapel + ", " + index + ")";
    }
}
