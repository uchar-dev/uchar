package dev.uchar.regelspraak.transpiler.tms.plan;

import java.util.List;

/**
 * A pattern over the objects of {@code feitklasse}, a fact class of the branch or a bean class, bound to the variable
 * {@code naam}: its constraints on the object alone ({@code alfa}), its identity joins to objects other patterns bind
 * ({@code joins}), and the values it binds ({@code bindingen}), in that order.
 */
public record Patroon(Class<?> feitklasse, String naam, List<Alfa> alfa, List<Join> joins, List<Binding> bindingen)
        implements LhsItem {

    public Patroon {
        alfa = List.copyOf(alfa);
        joins = List.copyOf(joins);
        bindingen = List.copyOf(bindingen);
    }
}
