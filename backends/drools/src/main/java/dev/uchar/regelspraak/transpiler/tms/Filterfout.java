package dev.uchar.regelspraak.transpiler.tms;

import dev.uchar.regelspraak.runtime.Breuk;

import java.util.List;
import java.util.Objects;

/**
 * A member of a fold's range for which the fold's filter faults, inserted logically by the filter fault rule of the
 * fold: {@code element} is a member of the range of the fold {@code vouw}, reached from the fold's subject
 * {@code onderwerp}, and the filter raises an evaluation fault for it ({@code fout}, unnamed): a division by a numeric
 * zero as {@link Breuk#delen} threw it, or a {@link Rekenfout}. It is no fault by itself. The reference interpreter
 * evaluates a filter only where the evaluation of a form reaches the fold: where the form's condition holds, or within
 * its condition up to the member after which a composite's verdict stands (RUNTIME-SEMANTICS ch. 5.10), and only there
 * does the fault happen (ch. 1.4, point 4; ch. 3.8). The rule that reads the fold therefore binds these facts beside
 * the contributions, and where its own evaluation reaches the fold while one stands, that evaluation faults as the same
 * fault in it would; the {@link Fout} is that rule's, justified by its activation.
 *
 * <p>{@code plaats} is the member's place in the fold order (RUNTIME-SEMANTICS ch. 5.11: the filter per member, members
 * in fold order): the number of the selection of an instance-set union, 0 for any other fold, and then, per to-many
 * step of the route from the fold's subject, the place of the step's member in its list ({@link Lijstplaats}). The
 * reference interpreter walks a multi-step range step by step, each instance's list in its order, and keeps the first
 * reach of a member reached twice (ch. 3.4b): the least place of a member over its routes is that first reach, and
 * the least place over the members whose filter faults is the member at which the reference interpreter stops. Ordered
 * by the place, compared element by element; the reading rule names the least, whatever the order of the firings.
 *
 * <p>Compared as {@link Bijdrage} is, with the place beside it: the fold's key by value, the subject and the member by
 * identity, the fault by its kind and its sentence, and the place by value. One member reached along two routes is
 * therefore one fact per route, each with that route's place, so the least of them stays the member's first reach.
 */
public final class Filterfout implements Comparable<Filterfout> {

    private final String vouw;
    private final Object onderwerp;
    private final Object element;
    private final RuntimeException fout;
    private final List<Integer> plaats;
    private final String zin;

    public Filterfout(String vouw, Object onderwerp, Object element, RuntimeException fout, List<Integer> plaats) {
        this.vouw = Objects.requireNonNull(vouw, "vouw");
        this.onderwerp = Objects.requireNonNull(onderwerp, "onderwerp");
        this.element = Objects.requireNonNull(element, "element");
        this.fout = Objects.requireNonNull(fout, "fout");
        this.plaats = List.copyOf(plaats);
        if (this.plaats.isEmpty()) throw new IllegalArgumentException("een plaats in de vouwvolgorde begint met de selectie");
        // the fault named after the fold: a division's sentence carries the dividend, which the exception does not expose
        this.zin = Rekenfout.benoem(fout, vouw).getMessage();
    }

    public String vouw() { return vouw; }

    public Object onderwerp() { return onderwerp; }

    public Object element() { return element; }

    /** The fault as the filter raised it, unnamed: the rule whose evaluation reaches the fold names it. */
    public RuntimeException fout() { return fout; }

    /** The member's place in the fold order: the selection, then its place per to-many step of its route. */
    public List<Integer> plaats() { return plaats; }

    @Override public int compareTo(Filterfout o) {
        for (int i = 0; i < Math.min(plaats.size(), o.plaats.size()); i++) {
            int c = Integer.compare(plaats.get(i), o.plaats.get(i));
            if (c != 0) return c;
        }
        // two routes of one selection have equally many to-many steps, so a tie here is one member along one route,
        // whose filter gives one fault; the sentence only keeps the order total
        int c = Integer.compare(plaats.size(), o.plaats.size());
        return c != 0 ? c : zin.compareTo(o.zin);
    }

    @Override public boolean equals(Object o) {
        return o instanceof Filterfout f && f.onderwerp == onderwerp && f.element == element && f.vouw.equals(vouw)
                && f.fout.getClass() == fout.getClass() && f.zin.equals(zin) && f.plaats.equals(plaats);
    }

    @Override public int hashCode() {
        return Objects.hash(vouw, System.identityHashCode(onderwerp), System.identityHashCode(element),
                fout.getClass().getName(), zin, plaats);
    }

    @Override public String toString() {
        return "Filterfout(" + Feitnamen.object(onderwerp) + ", " + Feitnamen.object(element) + ", " + plaats + ", "
                + zin + ")";
    }
}
