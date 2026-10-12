package dev.uchar.regelspraak.transpiler.tms.plan;

import java.util.List;

/** The pattern of the rule's subject: a bean of {@code bonenklasse}, bound to the variable {@code naam}, with the values
 *  the rule binds off it. */
public record Onderwerp(Class<?> bonenklasse, String naam, List<Binding> bindingen) implements LhsItem {

    public Onderwerp {
        bindingen = List.copyOf(bindingen);
    }
}
