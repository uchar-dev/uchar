package dev.uchar.regelspraak.transpiler.tms;

import java.util.List;
import java.util.Objects;

/**
 * A member of the range of a per-element fold over an attribute on a timeline whose value the fold cannot read: the
 * case gives the member's timeline as the asOfDate-bucket scalar ({@link BucketScalar}), which states no period of its
 * own, where the fold collapses each member's own timeline over time (RUNTIME-SEMANTICS ch. 3.6, §7.1, ch. 8.4).
 * Inserted logically by the rule beside the fold's contribution rule for a member whose filter holds
 * ({@code TmsTranspiler.memberErrorRule}), on the fold's subject, with the fault that names the member ({@code fault},
 * unnamed); it is no fault by itself. The reference interpreter evaluates the filter of every member before it reads a
 * value, and then reads the values in fold order and faults at the first member that brings no timeline of its own. The
 * rule that reads the fold binds these facts beside its contributions and its {@link FilterFault} facts, and where its
 * evaluation reaches the fold it faults with the first filter fault, and otherwise with the least of these, so the
 * member it names does not depend on the order of the firings.
 *
 * <p>{@code place} is the member's place in the fold order, as {@link FilterFault#place} gives it. Compared as
 * {@link FilterFault} is: the fold's key by value, the subject and the member by identity, the fault by its kind and its
 * sentence, and the place by value, and ordered by the place.
 */
public final class MemberFault implements Comparable<MemberFault> {

    private final String fold;
    private final Object subject;
    private final Object element;
    private final RuntimeException fault;
    private final List<Integer> place;

    public MemberFault(String fold, Object subject, Object element, RuntimeException fault, List<Integer> place) {
        this.fold = Objects.requireNonNull(fold, "vouw");
        this.subject = Objects.requireNonNull(subject, "subject");
        this.element = Objects.requireNonNull(element, "element");
        this.fault = Objects.requireNonNull(fault, "fout");
        this.place = List.copyOf(place);
        if (this.place.isEmpty()) throw new IllegalArgumentException("een plaats in de vouwvolgorde begint met de selectie");
    }

    public String fold() { return fold; }

    public Object subject() { return subject; }

    public Object element() { return element; }

    /** The fault that names the member, unnamed: the rule whose evaluation reaches the fold names it. */
    public RuntimeException fault() { return fault; }

    /** The member's place in the fold order: the selection, then its place per to-many step of its route. */
    public List<Integer> place() { return place; }

    @Override public int compareTo(MemberFault o) {
        for (int i = 0; i < Math.min(place.size(), o.place.size()); i++) {
            int c = Integer.compare(place.get(i), o.place.get(i));
            if (c != 0) return c;
        }
        // two routes of one selection have equally many to-many steps, so a tie here is one member along one route;
        // the sentence only keeps the order total
        int c = Integer.compare(place.size(), o.place.size());
        return c != 0 ? c : fault.getMessage().compareTo(o.fault.getMessage());
    }

    @Override public boolean equals(Object o) {
        return o instanceof MemberFault f && f.subject == subject && f.element == element && f.fold.equals(fold)
                && f.fault.getClass() == fault.getClass() && f.fault.getMessage().equals(fault.getMessage())
                && f.place.equals(place);
    }

    @Override public int hashCode() {
        return Objects.hash(fold, System.identityHashCode(subject), System.identityHashCode(element),
                fault.getClass().getName(), fault.getMessage(), place);
    }

    @Override public String toString() {
        return "Lidfout(" + FactNames.object(subject) + ", " + FactNames.object(element) + ", " + place + ", "
                + fault.getMessage() + ")";
    }
}
