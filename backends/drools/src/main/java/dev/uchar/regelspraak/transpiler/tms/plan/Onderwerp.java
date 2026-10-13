package dev.uchar.regelspraak.transpiler.tms.plan;

import java.util.List;

/** The pattern of the rule's subject: a bean of {@code bonenklasse}, bound to the variable {@code naam}, with its
 *  constraints on the bean alone ({@code alfa}), its identity joins to objects a pattern before it binds
 *  ({@code joins}), its other constraints between the bean and such an object ({@code betatoetsen}), and the values the
 *  rule binds off it, in that order. */
public record Onderwerp(Class<?> bonenklasse, String naam, List<Alfa> alfa, List<Join> joins,
                        List<Betatoets> betatoetsen, List<Binding> bindingen) implements LhsItem {
    public Onderwerp {
        alfa = List.copyOf(alfa);
        joins = List.copyOf(joins);
        betatoetsen = List.copyOf(betatoetsen);
        bindingen = List.copyOf(bindingen);
    }

    /** A subject pattern without constraints between the bean and another pattern's object but identity joins. */
    public Onderwerp(Class<?> bonenklasse, String naam, List<Alfa> alfa, List<Join> joins, List<Binding> bindingen) {
        this(bonenklasse, naam, alfa, joins, List.of(), bindingen);
    }
}
