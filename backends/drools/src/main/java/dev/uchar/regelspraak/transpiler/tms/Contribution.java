package dev.uchar.regelspraak.transpiler.tms;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * One member's contribution to one aggregation of the exec-model branch, inserted logically: {@code element} is
 * a member of the range of the fold {@code fold}, reached from the fold's subject {@code onderwerp} and passing the
 * fold's filter, and {@code waarde} the value it contributes, or null for a count, where the member itself is what
 * counts. A rule per range of a fold inserts it, with the member's value and every read of the filter bound in its
 * left-hand side; the fold is an accumulate over these facts in the left-hand side of the rule that reads it. A member
 * whose value or filter reading changes gives its contribution up and inserts the new one, and the accumulate follows.
 *
 * <p>Compared by the fold's key, the subject and the member by identity, and the value by value (a number by
 * {@code compareTo}). One member reached along two routes of a multi-step range (RUNTIME-SEMANTICS ch. 3.4b), or
 * selected by two selections of an instance-set union (ch. 3.4d), therefore merges into one fact with a justification
 * per route, and contributes once.
 */
public record Contribution(String fold, Object onderwerp, Object element, Object value) {

    public Contribution {
        Objects.requireNonNull(fold, "vouw");
        Objects.requireNonNull(onderwerp, "onderwerp");
        Objects.requireNonNull(element, "element");
    }

    @Override public boolean equals(Object o) {
        return o instanceof Contribution b && b.onderwerp == onderwerp && b.element == element && b.fold.equals(fold)
                && equal(b.value, value);
    }

    @Override public int hashCode() {
        Object w = value instanceof BigDecimal d ? d.stripTrailingZeros() : value;
        return Objects.hash(fold, System.identityHashCode(onderwerp), System.identityHashCode(element), w);
    }

    private static boolean equal(Object a, Object b) {
        if (a instanceof BigDecimal x && b instanceof BigDecimal y) return x.compareTo(y) == 0;
        return Objects.equals(a, b);
    }

    @Override public String toString() {
        return "Bijdrage(" + fold + ", " + FactNames.object(onderwerp) + ", " + FactNames.object(element)
                + (value == null ? "" : ", " + value) + ")";
    }
}
