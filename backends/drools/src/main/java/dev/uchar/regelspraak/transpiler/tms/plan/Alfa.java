package dev.uchar.regelspraak.transpiler.tms.plan;

import java.util.List;

/**
 * One constraint of a pattern on its own object: the conjunction of {@code toetsen}, in order, each on one field. It is
 * one constraint and not one per field, with one {@code id}: Drools' executable model shares a constraint between rules
 * by its id, so the analysis gives every constraint an id of its own, and a rendering into that model keeps it.
 */
public record Alfa(String id, List<Veldtoets> toetsen) {

    public Alfa {
        toetsen = List.copyOf(toetsen);
        if (toetsen.isEmpty()) throw new IllegalArgumentException("een alfaconstraint toetst ten minste één veld");
    }

    /** A test of field {@code veld} of the object against {@code waarde}. */
    public record Veldtoets(String veld, Vergelijker op, Object waarde) {}

    /** How a field is tested: {@code GELIJK} by {@code equals}, {@code MINSTENS} as a whole number at least the value,
     *  {@code BINNEN} as a date the validity window {@code waarde} admits, {@code IS_EEN} as an instance of the class
     *  {@code waarde}. */
    public enum Vergelijker { GELIJK, MINSTENS, BINNEN, IS_EEN }
}
