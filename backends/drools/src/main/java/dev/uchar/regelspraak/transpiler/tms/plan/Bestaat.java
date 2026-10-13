package dev.uchar.regelspraak.transpiler.tms.plan;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * The existence of objects that {@code patronen} match together, in that order, without binding them for the rule. The
 * scope is part of the plan, since a rendering cannot tell it from the patterns: {@code binnen} are the variables the
 * quantification declares, per pattern in order its own and then those its bindings bind ({@link #verklaard}), which
 * live only inside it, so that a pattern there may join or test an object or a value a pattern before it binds, and no
 * item after the quantification can name one; {@code buiten} are the variables bound before the quantification that
 * its patterns join or test, each once, in the order they are met. The same patterns outside a quantification bind
 * their objects for the rule, which then match once per combination of them.
 */
public record Bestaat(List<Patroon> patronen, List<String> binnen, List<String> buiten) implements LhsItem {
    public Bestaat {
        patronen = List.copyOf(patronen);
        binnen = List.copyOf(binnen);
        buiten = List.copyOf(buiten);
        scope("een bestaat-item", patronen, binnen, buiten);
    }

    /** The variables a quantification over {@code patronen} declares, in order: per pattern its own, then those its
     *  bindings bind. */
    public static List<String> verklaard(List<Patroon> patronen) {
        List<String> uit = new ArrayList<>();
        for (Patroon p : patronen) {
            uit.add(p.naam());
            for (Binding b : p.bindingen()) uit.add(b.doel());
        }
        return uit;
    }

    /** The variables bound before a quantification over {@code patronen} that its patterns join or test, each once, in
     *  the order they are met: those no pattern before the reading one declares. */
    public static List<String> gelezen(List<Patroon> patronen) {
        Set<String> gezien = new LinkedHashSet<>(), gelezen = new LinkedHashSet<>();
        for (Patroon p : patronen) {
            List<String> doelen = new ArrayList<>();
            for (Join j : p.joins()) doelen.add(j.doel());
            for (Betatoets t : p.betatoetsen()) doelen.add(t.doel());
            for (String d : doelen) if (!gezien.contains(d)) gelezen.add(d);
            gezien.add(p.naam());
            for (Binding b : p.bindingen()) gezien.add(b.doel());
        }
        return List.copyOf(gelezen);
    }

    /** Refuses, as a fault of the analysis, the scope {@code binnen} and {@code buiten} of a quantification over
     *  {@code patronen}, the {@code item}, where it is not the one the patterns give: {@code binnen} what they declare
     *  ({@link #verklaard}), each once, and {@code buiten} what they read from before the quantification
     *  ({@link #gelezen}). */
    static void scope(String item, List<Patroon> patronen, List<String> binnen, List<String> buiten) {
        if (patronen.isEmpty()) throw new IllegalArgumentException(item + " heeft ten minste één patroon");
        List<String> verklaard = verklaard(patronen);
        if (!binnen.equals(verklaard))
            throw new IllegalArgumentException(item + " declareert de variabelen van zijn patronen en van hun"
                    + " bindingen, " + verklaard + ", niet " + binnen);
        if (new LinkedHashSet<>(binnen).size() != binnen.size())
            throw new IllegalArgumentException(item + " declareert een variabele twee keer: " + binnen);
        List<String> gelezen = gelezen(patronen);
        if (!buiten.equals(gelezen))
            throw new IllegalArgumentException(item + " leest van buiten " + gelezen + ", niet " + buiten);
        // a pattern reads an object or a value of the quantification only after the pattern that declares it
        for (String b : buiten)
            if (binnen.contains(b))
                throw new IllegalArgumentException("een patroon van " + item + " leest " + b + " voor het patroon"
                        + " dat die variabele declareert");
    }
}
