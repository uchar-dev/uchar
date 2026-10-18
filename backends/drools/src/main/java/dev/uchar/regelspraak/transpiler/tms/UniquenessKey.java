package dev.uchar.regelspraak.transpiler.tms;

import dev.uchar.regelspraak.runtime.RegelSpraakRuntime;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * The key of one instance in one source of a uniqueness of the exec-model branch, inserted logically by the
 * source's key rule (RUNTIME-SEMANTICS ch. 5.9): {@code element} carries {@code waarden}, its key components in the
 * order the source lists them, each gevuld, an enumeration label in its normalised form, and, for a uniqueness scoped
 * by a parent, the {@code scope} it is linked to (null where the form has no scope). An instance with an empty
 * component, or without a parent where the form is scoped, gets no key: it takes no part (ch. 5.9, rule 2). The key
 * rule binds the components in its left-hand side, so a key whose value changes gives way to the new one.
 *
 * <p>{@code bron} is the number of the source in the form: an instance that two sources of a union reach has a key in
 * each, and they count apart, so an instance whose two keys are equal duplicates against itself (ch. 5.9, rule 1).
 * Compared by the form's name, the source, the instance and the parent by identity, and the components by value, a
 * number by {@code compareTo}; two keys of different instances collide where {@link #collidesWith} holds.
 */
public final class UniquenessKey {

    private final String uniqueness;
    private final int source;
    private final Object element;
    private final Object scope;
    private final List<Object> waarden;

    public UniquenessKey(String uniqueness, int source, Object element, Object scope, List<Object> waarden) {
        this.uniqueness = Objects.requireNonNull(uniqueness, "uniciteit");
        this.source = source;
        this.element = Objects.requireNonNull(element, "element");
        this.scope = scope;
        this.waarden = new ArrayList<>(waarden);
        for (Object w : this.waarden)
            if (w == null) throw new IllegalArgumentException("een lege sleutelcomponent neemt niet deel (ch. 5.9)");
    }

    public String uniqueness() { return uniqueness; }

    public Object element() { return element; }

    /** Whether this key and {@code o} are the same key of one form: the same parent, and the components equal one by
     *  one under the value equality of RUNTIME-SEMANTICS ch. 1.2d. Every key collides with itself, so a key is in a
     *  duplicate where more than one key collides with it. */
    public boolean collidesWith(UniquenessKey o) {
        if (!o.uniqueness.equals(uniqueness) || o.scope != scope || o.waarden.size() != waarden.size()) return false;
        for (int i = 0; i < waarden.size(); i++)
            if (!RegelSpraakRuntime.gelijkWaarde(waarden.get(i), o.waarden.get(i))) return false;
        return true;
    }

    @Override public boolean equals(Object o) {
        return o instanceof UniquenessKey k && k.element == element && k.source == source && collidesWith(k);
    }

    @Override public int hashCode() {
        List<Object> h = new ArrayList<>(waarden.size());
        for (Object w : waarden) h.add(w instanceof BigDecimal d ? d.stripTrailingZeros() : w);
        return Objects.hash(uniqueness, source, System.identityHashCode(element), System.identityHashCode(scope), h);
    }

    @Override public String toString() {
        return "Uniciteitssleutel(" + uniqueness + " " + source + ", " + FactNames.object(element)
                + (scope == null ? "" : ", " + FactNames.object(scope)) + ", " + waarden + ")";
    }
}
