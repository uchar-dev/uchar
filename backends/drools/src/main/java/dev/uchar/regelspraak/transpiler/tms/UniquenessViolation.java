package dev.uchar.regelspraak.transpiler.tms;

import java.util.Objects;

/**
 * An instance whose key is in a duplicate of the uniqueness {@code uniqueness}, inserted logically by the form's
 * violation rule for each of its keys that collides with another (RUNTIME-SEMANTICS ch. 5.9). An instance that two
 * sources of a union reach can collide through both keys; the equal facts merge, so the form's channel rule deposits
 * one §9.5 inconsistency per instance, as ch. 5.6 deposits one fact per form and subject however often the form finds
 * it. Compared by the form's name and the instance by identity.
 */
public record UniquenessViolation(String uniqueness, Object element) {

    public UniquenessViolation {
        Objects.requireNonNull(uniqueness, "uniciteit");
        Objects.requireNonNull(element, "element");
    }

    @Override public boolean equals(Object o) {
        return o instanceof UniquenessViolation s && s.element == element && s.uniqueness.equals(uniqueness);
    }

    @Override public int hashCode() {
        return Objects.hash(uniqueness, System.identityHashCode(element));
    }

    @Override public String toString() {
        return "Uniciteitsschending(" + uniqueness + ", " + FactNames.object(element) + ")";
    }
}
