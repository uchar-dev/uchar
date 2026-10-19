package dev.uchar.regelspraak.transpiler.tms.plan;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * The existence of objects that {@code patterns} match together, in that order, without binding them for the rule. The
 * scope is part of the plan, since a rendering cannot tell it from the patterns: {@code inside} are the variables the
 * quantification declares, per pattern in order its own and then those its bindings bind ({@link #verklaard}), which
 * live only inside it, so that a pattern there may join or test an object or a value a pattern before it binds, and no
 * item after the quantification can name one; {@code outside} are the variables bound before the quantification that
 * its patterns join or test, each once, in the order they are met. The same patterns outside a quantification bind
 * their objects for the rule, which then match once per combination of them.
 */
public record Exists(List<FactPattern> patterns, List<String> inside, List<String> outside) implements LhsItem {
    public Exists {
        patterns = List.copyOf(patterns);
        inside = List.copyOf(inside);
        outside = List.copyOf(outside);
        scope("een bestaat-item", patterns, inside, outside);
    }

    /** The variables a quantification over {@code patterns} declares, in order: per pattern its own, then those its
     *  bindings bind. */
    public static List<String> verklaard(List<FactPattern> patterns) {
        List<String> out = new ArrayList<>();
        for (FactPattern p : patterns) {
            out.add(p.naam());
            for (Binding b : p.bindings()) out.add(b.target());
        }
        return out;
    }

    /** The variables bound before a quantification over {@code patterns} that its patterns join or test, each once, in
     *  the order they are met: those no pattern before the reading one declares. */
    public static List<String> read(List<FactPattern> patterns) {
        Set<String> seen = new LinkedHashSet<>(), read = new LinkedHashSet<>();
        for (FactPattern p : patterns) {
            List<String> targets = new ArrayList<>();
            for (Join j : p.joins()) targets.add(j.target());
            for (BetaTest t : p.betaTests()) targets.add(t.target());
            for (String d : targets) if (!seen.contains(d)) read.add(d);
            seen.add(p.naam());
            for (Binding b : p.bindings()) seen.add(b.target());
        }
        return List.copyOf(read);
    }

    /** Refuses, as a fault of the analysis, the scope {@code inside} and {@code outside} of a quantification over
     *  {@code patterns}, the {@code item}, where it is not the one the patterns give: {@code inside} what they declare
     *  ({@link #verklaard}), each once, and {@code outside} what they read from before the quantification
     *  ({@link #read}). */
    static void scope(String item, List<FactPattern> patterns, List<String> inside, List<String> outside) {
        if (patterns.isEmpty()) throw new IllegalArgumentException(item + " heeft ten minste één patroon");
        List<String> verklaard = verklaard(patterns);
        if (!inside.equals(verklaard))
            throw new IllegalArgumentException(item + " declareert de variabelen van zijn patronen en van hun"
                    + " bindingen, " + verklaard + ", niet " + inside);
        if (new LinkedHashSet<>(inside).size() != inside.size())
            throw new IllegalArgumentException(item + " declareert een variabele twee keer: " + inside);
        List<String> read = read(patterns);
        if (!outside.equals(read))
            throw new IllegalArgumentException(item + " leest van buiten " + read + ", niet " + outside);
        // a pattern reads an object or a value of the quantification only after the pattern that declares it
        for (String b : outside)
            if (inside.contains(b))
                throw new IllegalArgumentException("een patroon van " + item + " leest " + b + " voor het patroon"
                        + " dat die variabele declareert");
    }
}
