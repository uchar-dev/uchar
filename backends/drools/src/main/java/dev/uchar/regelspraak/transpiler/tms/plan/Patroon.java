package dev.uchar.regelspraak.transpiler.tms.plan;

import java.util.List;

/**
 * A pattern over the objects of {@code feitklasse}, a fact class of the branch or a bean class, bound to the variable
 * {@code naam}: its constraints on the object alone ({@code alfa}), its identity joins to objects other patterns bind
 * ({@code joins}), its other constraints between its object and an object another pattern binds
 * ({@code betatoetsen}), and the values it binds ({@code bindingen}), in that order.
 */
public record Patroon(Class<?> feitklasse, String naam, List<Alfa> alfa, List<Join> joins, List<Betatoets> betatoetsen,
                      List<Binding> bindingen) implements LhsItem {

    public Patroon {
        alfa = List.copyOf(alfa);
        joins = List.copyOf(joins);
        betatoetsen = List.copyOf(betatoetsen);
        bindingen = List.copyOf(bindingen);
    }

    /** A pattern without constraints between its object and another pattern's but identity joins. */
    public Patroon(Class<?> feitklasse, String naam, List<Alfa> alfa, List<Join> joins, List<Binding> bindingen) {
        this(feitklasse, naam, alfa, joins, List.of(), bindingen);
    }
}
