package dev.uchar.regelspraak.transpiler.tms;

import java.util.List;
import java.util.Objects;

/**
 * A member of the range of a per-element fold over an attribute on a timeline whose value the fold cannot read: the
 * case gives the member's timeline as the asOfDate-bucket scalar ({@link Emmerscalair}), which states no period of its
 * own, where the fold collapses each member's own timeline over time (RUNTIME-SEMANTICS ch. 3.6, §7.1, ch. 8.4).
 * Inserted logically by the rule beside the fold's contribution rule for a member whose filter holds
 * ({@code TmsTranspiler.lidfoutregel}), on the fold's subject, with the fault that names the member ({@code fout},
 * unnamed); it is no fault by itself. The reference interpreter evaluates the filter of every member before it reads a
 * value, and then reads the values in fold order and faults at the first member that brings no timeline of its own. The
 * rule that reads the fold binds these facts beside its contributions and its {@link Filterfout} facts, and where its
 * evaluation reaches the fold it faults with the first filter fault, and otherwise with the least of these, so the
 * member it names does not depend on the order of the firings.
 *
 * <p>{@code plaats} is the member's place in the fold order, as {@link Filterfout#plaats} gives it. Compared as
 * {@link Filterfout} is: the fold's key by value, the subject and the member by identity, the fault by its kind and its
 * sentence, and the place by value, and ordered by the place.
 */
public final class Lidfout implements Comparable<Lidfout> {

    private final String vouw;
    private final Object onderwerp;
    private final Object element;
    private final RuntimeException fout;
    private final List<Integer> plaats;

    public Lidfout(String vouw, Object onderwerp, Object element, RuntimeException fout, List<Integer> plaats) {
        this.vouw = Objects.requireNonNull(vouw, "vouw");
        this.onderwerp = Objects.requireNonNull(onderwerp, "onderwerp");
        this.element = Objects.requireNonNull(element, "element");
        this.fout = Objects.requireNonNull(fout, "fout");
        this.plaats = List.copyOf(plaats);
        if (this.plaats.isEmpty()) throw new IllegalArgumentException("een plaats in de vouwvolgorde begint met de selectie");
    }

    public String vouw() { return vouw; }

    public Object onderwerp() { return onderwerp; }

    public Object element() { return element; }

    /** The fault that names the member, unnamed: the rule whose evaluation reaches the fold names it. */
    public RuntimeException fout() { return fout; }

    /** The member's place in the fold order: the selection, then its place per to-many step of its route. */
    public List<Integer> plaats() { return plaats; }

    @Override public int compareTo(Lidfout o) {
        for (int i = 0; i < Math.min(plaats.size(), o.plaats.size()); i++) {
            int c = Integer.compare(plaats.get(i), o.plaats.get(i));
            if (c != 0) return c;
        }
        // two routes of one selection have equally many to-many steps, so a tie here is one member along one route;
        // the sentence only keeps the order total
        int c = Integer.compare(plaats.size(), o.plaats.size());
        return c != 0 ? c : fout.getMessage().compareTo(o.fout.getMessage());
    }

    @Override public boolean equals(Object o) {
        return o instanceof Lidfout f && f.onderwerp == onderwerp && f.element == element && f.vouw.equals(vouw)
                && f.fout.getClass() == fout.getClass() && f.fout.getMessage().equals(fout.getMessage())
                && f.plaats.equals(plaats);
    }

    @Override public int hashCode() {
        return Objects.hash(vouw, System.identityHashCode(onderwerp), System.identityHashCode(element),
                fout.getClass().getName(), fout.getMessage(), plaats);
    }

    @Override public String toString() {
        return "Lidfout(" + Feitnamen.object(onderwerp) + ", " + Feitnamen.object(element) + ", " + plaats + ", "
                + fout.getMessage() + ")";
    }
}
