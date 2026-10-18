package dev.uchar.regelspraak.transpiler.tms;

import java.util.List;
import java.util.Objects;

/**
 * The matrix of a quantifier fold evaluated for one contributor, the value of that contributor's {@link Contribution}
 * where the matrix can fault or deposit (RUNTIME-SEMANTICS ch. 3.9a): whether it holds ({@code holds}), or the
 * evaluation fault it raised ({@code fout}, unnamed: the form that reads the fold names it), and whether its
 * evaluation reached a place that deposits ({@code deposited}: a division of a filled dividend by a leeg divisor or
 * the square root of a negative number). It is no outcome by itself. The reference interpreter reads the matrix in
 * the contributors' order and stops at the first contributor after which the verdict stands (ch. 5.10), so a
 * contributor it does not reach raises no fault and deposits nothing; the rule that reads the fold replays that order
 * over these values, and only a contributor the replay reaches has its fault or its deposit heard.
 *
 * <p>{@code place} is the contributor's place in that order, as {@link FilterError#place} gives it: 0 for the one
 * selection, then the place of the member in the list of the fold's subject ({@link ListPlace}), absent for a range
 * that reaches at most one member. Ordered by the place, compared element by element. Compared by value: the place,
 * the verdict, the fault by its kind and its sentence, and the deposit, so the {@link Contribution} that carries it merges
 * with an equal one as its other values do.
 */
public final class MemberTest implements Comparable<MemberTest> {

    private final List<Integer> place;
    private final Boolean holds;
    private final RuntimeException fout;
    private final boolean deposited;

    public MemberTest(List<Integer> place, Boolean holds, RuntimeException fout, boolean deposited) {
        this.place = List.copyOf(place);
        if (this.place.isEmpty()) throw new IllegalArgumentException("een plaats in de volgorde begint met de selectie");
        if ((holds == null) == (fout == null))
            throw new IllegalArgumentException("een lidtoets houdt of faalt, nooit allebei of geen van beide");
        this.holds = holds;
        this.fout = fout;
        this.deposited = deposited;
    }

    public List<Integer> place() { return place; }

    /** Whether the matrix holds for the contributor; null where it faulted. */
    public Boolean holds() { return holds; }

    /** The fault the matrix raised for the contributor, unnamed; null where it answered. */
    public RuntimeException fout() { return fout; }

    public boolean deposited() { return deposited; }

    @Override public int compareTo(MemberTest o) {
        for (int i = 0; i < Math.min(place.size(), o.place.size()); i++) {
            int c = Integer.compare(place.get(i), o.place.get(i));
            if (c != 0) return c;
        }
        return Integer.compare(place.size(), o.place.size());
    }

    @Override public boolean equals(Object o) {
        return o instanceof MemberTest t && t.place.equals(place) && Objects.equals(t.holds, holds)
                && t.deposited == deposited && (fout == null ? t.fout == null
                : t.fout != null && t.fout.getClass() == fout.getClass()
                  && Objects.equals(t.fout.getMessage(), fout.getMessage()));
    }

    @Override public int hashCode() {
        return Objects.hash(place, holds, deposited, fout == null ? null : fout.getClass().getName(),
                fout == null ? null : fout.getMessage());
    }

    @Override public String toString() {
        return "Lidtoets(" + place + ", " + (fout != null ? "fout " + fout.getMessage() : holds ? "waar" : "onwaar")
                + (deposited ? ", gedeponeerd" : "") + ")";
    }
}
