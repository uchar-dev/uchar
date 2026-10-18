package dev.uchar.regelspraak.transpiler.tms.plan;

import java.util.List;

/**
 * The absence of objects that {@code patterns} match together, in that order: no combination of them exists. The scope
 * is part of the plan, as in {@link Exists}: {@code binnen} are the variables the negation declares, per pattern in
 * order its own and then those its bindings bind, which live only inside it, so that a pattern there may join or test
 * an object or a value a pattern before it binds, and no item after the negation can name one; {@code buiten} are the
 * variables bound before the negation that its patterns join or test, each once, in the order they are met.
 */
public record Not(List<FactPattern> patterns, List<String> inside, List<String> outside) implements LhsItem {
    public Not {
        patterns = List.copyOf(patterns);
        inside = List.copyOf(inside);
        outside = List.copyOf(outside);
        Exists.scope("een niet-item", patterns, inside, outside);
    }

    /** The absence of an object that {@code pattern} matches, in the scope of that one pattern: what it declares lives
     *  inside, what it reads comes from before. */
    public Not(FactPattern pattern) {
        this(List.of(pattern), Exists.verklaard(List.of(pattern)), Exists.read(List.of(pattern)));
    }
}
