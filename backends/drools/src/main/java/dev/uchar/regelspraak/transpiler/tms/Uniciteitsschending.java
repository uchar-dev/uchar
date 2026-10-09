package dev.uchar.regelspraak.transpiler.tms;

import java.util.Objects;

/**
 * An instance whose key is in a duplicate of the uniqueness {@code uniciteit}, inserted logically by the form's
 * violation rule for each of its keys that collides with another (RUNTIME-SEMANTICS ch. 5.9). An instance that two
 * sources of a union reach can collide through both keys; the equal facts merge, so the form's channel rule deposits
 * one §9.5 inconsistency per instance, as ch. 5.6 deposits one fact per form and subject however often the form finds
 * it. Compared by the form's name and the instance by identity.
 */
public record Uniciteitsschending(String uniciteit, Object element) {

    public Uniciteitsschending {
        Objects.requireNonNull(uniciteit, "uniciteit");
        Objects.requireNonNull(element, "element");
    }

    @Override public boolean equals(Object o) {
        return o instanceof Uniciteitsschending s && s.element == element && s.uniciteit.equals(uniciteit);
    }

    @Override public int hashCode() {
        return Objects.hash(uniciteit, System.identityHashCode(element));
    }

    @Override public String toString() {
        return "Uniciteitsschending(" + uniciteit + ", " + Feitnamen.object(element) + ")";
    }
}
