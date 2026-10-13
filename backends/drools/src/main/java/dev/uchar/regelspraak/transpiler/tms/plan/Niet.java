package dev.uchar.regelspraak.transpiler.tms.plan;

import java.util.List;

/**
 * The absence of objects that {@code patronen} match together, in that order: no combination of them exists. The scope
 * is part of the plan, as in {@link Bestaat}: {@code binnen} are the variables the negation declares, per pattern in
 * order its own and then those its bindings bind, which live only inside it, so that a pattern there may join or test
 * an object or a value a pattern before it binds, and no item after the negation can name one; {@code buiten} are the
 * variables bound before the negation that its patterns join or test, each once, in the order they are met.
 */
public record Niet(List<Patroon> patronen, List<String> binnen, List<String> buiten) implements LhsItem {
    public Niet {
        patronen = List.copyOf(patronen);
        binnen = List.copyOf(binnen);
        buiten = List.copyOf(buiten);
        Bestaat.scope("een niet-item", patronen, binnen, buiten);
    }

    /** The absence of an object that {@code patroon} matches, in the scope of that one pattern: what it declares lives
     *  inside, what it reads comes from before. */
    public Niet(Patroon patroon) {
        this(List.of(patroon), Bestaat.verklaard(List.of(patroon)), Bestaat.gelezen(List.of(patroon)));
    }
}
