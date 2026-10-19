package dev.uchar.regelspraak.transpiler.tms;

import dev.uchar.regelspraak.runtime.Breuk;

import java.util.List;
import java.util.Objects;

/**
 * A member of a fold's range for which the fold's filter faults, inserted logically by the filter fault rule of the
 * fold: {@code element} is a member of the range of the fold {@code fold}, reached from the fold's subject
 * {@code subject}, and the filter raises an evaluation fault for it ({@code fault}, unnamed): a division by a numeric
 * zero as {@link Rational#delen} threw it, or a {@link ComputeFault}. It is no fault by itself. The reference interpreter
 * evaluates a filter only where the evaluation of a form reaches the fold: where the form's condition holds, or within
 * its condition up to the member after which a composite's verdict stands (RUNTIME-SEMANTICS ch. 5.10), and only there
 * does the fault happen (ch. 1.4, point 4; ch. 3.8). The rule that reads the fold therefore binds these facts beside
 * the contributions, and where its own evaluation reaches the fold while one stands, that evaluation faults as the same
 * fault in it would; the {@link Fault} is that rule's, justified by its activation.
 *
 * <p>{@code place} is the member's place in the fold order (RUNTIME-SEMANTICS ch. 5.11: the filter per member, members
 * in fold order): the number of the selection of an instance-set union, 0 for any other fold, and then, per to-many
 * step of the route from the fold's subject, the place of the step's member in its list ({@link ListPlace}). The
 * reference interpreter walks a multi-step range step by step, each instance's list in its order, and keeps the first
 * reach of a member reached twice (ch. 3.4b): the least place of a member over its routes is that first reach, and
 * the least place over the members whose filter faults is the member at which the reference interpreter stops. Ordered
 * by the place, compared element by element; the reading rule names the least, whatever the order of the firings.
 *
 * <p>Compared as {@link Contribution} is, with the place beside it: the fold's key by value, the subject and the member by
 * identity, the fault by its kind and its sentence, and the place by value. One member reached along two routes is
 * therefore one fact per route, each with that route's place, so the least of them stays the member's first reach.
 */
public final class FilterFault implements Comparable<FilterFault> {

    private final String fold;
    private final Object subject;
    private final Object element;
    private final RuntimeException fault;
    private final List<Integer> place;
    private final String sentence;

    public FilterFault(String fold, Object subject, Object element, RuntimeException fault, List<Integer> place) {
        this.fold = Objects.requireNonNull(fold, "vouw");
        this.subject = Objects.requireNonNull(subject, "subject");
        this.element = Objects.requireNonNull(element, "element");
        this.fault = Objects.requireNonNull(fault, "fout");
        this.place = List.copyOf(place);
        if (this.place.isEmpty()) throw new IllegalArgumentException("een plaats in de vouwvolgorde begint met de selectie");
        // the fault named after the fold: a division's sentence carries the dividend, which the exception does not expose
        this.sentence = ComputeFault.designate(fault, fold).getMessage();
    }

    public String fold() { return fold; }

    public Object subject() { return subject; }

    public Object element() { return element; }

    /** The fault as the filter raised it, unnamed: the rule whose evaluation reaches the fold names it. */
    public RuntimeException fault() { return fault; }

    /** The member's place in the fold order: the selection, then its place per to-many step of its route. */
    public List<Integer> place() { return place; }

    @Override public int compareTo(FilterFault o) {
        for (int i = 0; i < Math.min(place.size(), o.place.size()); i++) {
            int c = Integer.compare(place.get(i), o.place.get(i));
            if (c != 0) return c;
        }
        // two routes of one selection have equally many to-many steps, so a tie here is one member along one route,
        // whose filter gives one fault; the sentence only keeps the order total
        int c = Integer.compare(place.size(), o.place.size());
        return c != 0 ? c : sentence.compareTo(o.sentence);
    }

    @Override public boolean equals(Object o) {
        return o instanceof FilterFault f && f.subject == subject && f.element == element && f.fold.equals(fold)
                && f.fault.getClass() == fault.getClass() && f.sentence.equals(sentence) && f.place.equals(place);
    }

    @Override public int hashCode() {
        return Objects.hash(fold, System.identityHashCode(subject), System.identityHashCode(element),
                fault.getClass().getName(), sentence, place);
    }

    @Override public String toString() {
        return "Filterfout(" + FactNames.object(subject) + ", " + FactNames.object(element) + ", " + place + ", "
                + sentence + ")";
    }
}
