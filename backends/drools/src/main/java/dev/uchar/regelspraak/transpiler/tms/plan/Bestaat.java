package dev.uchar.regelspraak.transpiler.tms.plan;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * The existence of objects that {@code patronen} match together, in that order, without binding them for the rule. The
 * scope is part of the plan, since a rendering cannot tell it from the patterns: {@code binnen} are the variables the
 * quantification declares, its patterns' own in their order, which live only inside it, so that a pattern there may
 * join or test an object a pattern before it binds, and no item after the quantification can name one; {@code buiten}
 * are the variables bound before the quantification that its patterns join or test, each once, in the order they are
 * met. The same patterns outside a quantification bind their objects for the rule, which then match once per
 * combination of them.
 */
public record Bestaat(List<Patroon> patronen, List<String> binnen, List<String> buiten) implements LhsItem {
    public Bestaat {
        patronen = List.copyOf(patronen);
        binnen = List.copyOf(binnen);
        buiten = List.copyOf(buiten);
        if (patronen.isEmpty()) throw new IllegalArgumentException("een bestaat-item heeft ten minste één patroon");
        if (!binnen.equals(patronen.stream().map(Patroon::naam).toList()))
            throw new IllegalArgumentException("een bestaat-item declareert de variabelen van zijn patronen, "
                    + patronen.stream().map(Patroon::naam).toList() + ", niet " + binnen);
        Set<String> gezien = new LinkedHashSet<>(), gelezen = new LinkedHashSet<>();
        for (Patroon p : patronen) {
            List<String> doelen = new ArrayList<>();
            for (Join j : p.joins()) doelen.add(j.doel());
            for (Betatoets t : p.betatoetsen()) doelen.add(t.doel());
            for (String d : doelen) if (!gezien.contains(d)) gelezen.add(d);
            gezien.add(p.naam());
        }
        if (!buiten.equals(List.copyOf(gelezen)))
            throw new IllegalArgumentException("een bestaat-item leest van buiten " + gelezen + ", niet " + buiten);
        // a pattern reads an object of the quantification only after the pattern that declares it
        for (String b : buiten)
            if (binnen.contains(b))
                throw new IllegalArgumentException("een patroon van een bestaat-item leest " + b + " voor het patroon"
                        + " dat die variabele declareert");
    }
}
