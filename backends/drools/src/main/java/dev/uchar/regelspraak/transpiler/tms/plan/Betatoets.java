package dev.uchar.regelspraak.transpiler.tms.plan;

import java.util.List;

/**
 * A constraint of a pattern between its own object and the object the variable {@code doel} binds: the predicate
 * {@code functie} over the two, in that order, then the constants of the analysis {@code constanten}, such as the
 * chains of reference fields a walk from either object follows. It is unindexed, since it compares by value or at the
 * end of a walk, and not the two objects by an identity an index can hash. {@code id} is the constraint's own id (see
 * {@link Alfa}).
 */
public record Betatoets(String id, Functie functie, String doel, List<Term.Const> constanten) {
    public Betatoets {
        constanten = List.copyOf(constanten);
    }

    /** A constraint whose function takes the two objects alone. */
    public Betatoets(String id, Functie functie, String doel) {
        this(id, functie, doel, List.of());
    }
}
