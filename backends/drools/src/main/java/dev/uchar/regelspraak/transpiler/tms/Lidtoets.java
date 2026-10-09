package dev.uchar.regelspraak.transpiler.tms;

import java.util.List;
import java.util.Objects;

/**
 * The matrix of a quantifier fold evaluated for one contributor, the value of that contributor's {@link Bijdrage}
 * where the matrix can fault or deposit (RUNTIME-SEMANTICS ch. 3.9a): whether it holds ({@code waar}), or the
 * evaluation fault it raised ({@code fout}, unnamed: the form that reads the fold names it), and whether its
 * evaluation reached a place that deposits ({@code gedeponeerd}: a division of a filled dividend by a leeg divisor or
 * the square root of a negative number). It is no outcome by itself. The reference interpreter reads the matrix in
 * the contributors' order and stops at the first contributor after which the verdict stands (ch. 5.10), so a
 * contributor it does not reach raises no fault and deposits nothing; the rule that reads the fold replays that order
 * over these values, and only a contributor the replay reaches has its fault or its deposit heard.
 *
 * <p>{@code plaats} is the contributor's place in that order, as {@link Filterfout#plaats} gives it: 0 for the one
 * selection, then the place of the member in the list of the fold's subject ({@link Lijstplaats}), absent for a range
 * that reaches at most one member. Ordered by the place, compared element by element. Compared by value: the place,
 * the verdict, the fault by its kind and its sentence, and the deposit, so the {@link Bijdrage} that carries it merges
 * with an equal one as its other values do.
 */
public final class Lidtoets implements Comparable<Lidtoets> {

    private final List<Integer> plaats;
    private final Boolean waar;
    private final RuntimeException fout;
    private final boolean gedeponeerd;

    public Lidtoets(List<Integer> plaats, Boolean waar, RuntimeException fout, boolean gedeponeerd) {
        this.plaats = List.copyOf(plaats);
        if (this.plaats.isEmpty()) throw new IllegalArgumentException("een plaats in de volgorde begint met de selectie");
        if ((waar == null) == (fout == null))
            throw new IllegalArgumentException("een lidtoets houdt of faalt, nooit allebei of geen van beide");
        this.waar = waar;
        this.fout = fout;
        this.gedeponeerd = gedeponeerd;
    }

    public List<Integer> plaats() { return plaats; }

    /** Whether the matrix holds for the contributor; null where it faulted. */
    public Boolean waar() { return waar; }

    /** The fault the matrix raised for the contributor, unnamed; null where it answered. */
    public RuntimeException fout() { return fout; }

    public boolean gedeponeerd() { return gedeponeerd; }

    @Override public int compareTo(Lidtoets o) {
        for (int i = 0; i < Math.min(plaats.size(), o.plaats.size()); i++) {
            int c = Integer.compare(plaats.get(i), o.plaats.get(i));
            if (c != 0) return c;
        }
        return Integer.compare(plaats.size(), o.plaats.size());
    }

    @Override public boolean equals(Object o) {
        return o instanceof Lidtoets t && t.plaats.equals(plaats) && Objects.equals(t.waar, waar)
                && t.gedeponeerd == gedeponeerd && (fout == null ? t.fout == null
                : t.fout != null && t.fout.getClass() == fout.getClass()
                  && Objects.equals(t.fout.getMessage(), fout.getMessage()));
    }

    @Override public int hashCode() {
        return Objects.hash(plaats, waar, gedeponeerd, fout == null ? null : fout.getClass().getName(),
                fout == null ? null : fout.getMessage());
    }

    @Override public String toString() {
        return "Lidtoets(" + plaats + ", " + (fout != null ? "fout " + fout.getMessage() : waar ? "waar" : "onwaar")
                + (gedeponeerd ? ", gedeponeerd" : "") + ")";
    }
}
