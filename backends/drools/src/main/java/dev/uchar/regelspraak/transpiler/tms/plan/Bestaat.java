package dev.uchar.regelspraak.transpiler.tms.plan;

import java.util.List;

/** The existence of objects that {@code patronen} match together, without binding them for the rule. */
public record Bestaat(List<Patroon> patronen) implements LhsItem {

    public Bestaat {
        patronen = List.copyOf(patronen);
        if (patronen.isEmpty()) throw new IllegalArgumentException("een bestaat-item heeft ten minste één patroon");
    }
}
