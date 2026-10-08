package dev.uchar.regelspraak.transpiler.tms;

import dev.uchar.regelspraak.runtime.Breuk;

import java.util.Objects;

/**
 * A member of a fold's range for which the fold's filter faults, inserted logically by the filter fault rule of the
 * fold: {@code element} is a member of the range of the fold {@code vouw}, reached from the fold's subject
 * {@code onderwerp}, and the filter divides by a numeric zero for it ({@code fout}, as {@link Breuk#delen} threw it).
 * It is no fault by itself. The reference interpreter evaluates a filter only where the evaluation of a form reaches
 * the fold: where the form's condition holds, or within its condition up to the member after which a composite's
 * verdict stands (RUNTIME-SEMANTICS ch. 5.10), and only there does the division happen (ch. 1.4, point 4). The rule
 * that reads the fold therefore binds these facts beside the contributions, and where its own evaluation reaches the
 * fold while one stands, that evaluation faults as a division in it would; the {@link Fout} is that rule's, justified
 * by its activation.
 *
 * <p>Compared as {@link Bijdrage} is: the fold's key by value, the subject and the member by identity, and the fault by
 * its kind and its sentence, so one member reached along two routes is one fact. Ordered by the sentence, which names
 * the dividend: where the filter faults for several members, the reading rule names the least, whatever the order of
 * the firings.
 */
public final class Filterfout implements Comparable<Filterfout> {

    private final String vouw;
    private final Object onderwerp;
    private final Object element;
    private final Breuk.DelingDoorNul fout;
    private final String zin;

    public Filterfout(String vouw, Object onderwerp, Object element, Breuk.DelingDoorNul fout) {
        this.vouw = Objects.requireNonNull(vouw, "vouw");
        this.onderwerp = Objects.requireNonNull(onderwerp, "onderwerp");
        this.element = Objects.requireNonNull(element, "element");
        this.fout = Objects.requireNonNull(fout, "fout");
        // the division named after the fold: the sentence carries the dividend, which the exception does not expose
        this.zin = fout.inDeRegel(vouw, fout).getMessage();
    }

    public String vouw() { return vouw; }

    public Object onderwerp() { return onderwerp; }

    public Object element() { return element; }

    /** The division as the filter raised it, unnamed: the rule whose evaluation reaches the fold names it. */
    public Breuk.DelingDoorNul fout() { return fout; }

    @Override public int compareTo(Filterfout o) {
        return zin.compareTo(o.zin);
    }

    @Override public boolean equals(Object o) {
        return o instanceof Filterfout f && f.onderwerp == onderwerp && f.element == element && f.vouw.equals(vouw)
                && f.fout.getClass() == fout.getClass() && f.zin.equals(zin);
    }

    @Override public int hashCode() {
        return Objects.hash(vouw, System.identityHashCode(onderwerp), System.identityHashCode(element),
                fout.getClass().getName(), zin);
    }

    @Override public String toString() {
        return "Filterfout(" + Feitnamen.object(onderwerp) + ", " + Feitnamen.object(element) + ", " + zin + ")";
    }
}
