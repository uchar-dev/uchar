package dev.uchar.regelspraak.transpiler.tms.plan;

import java.util.List;

/**
 * A pattern over the objects of {@code feitklasse}, a fact class of the branch or a bean class, bound to the variable
 * {@code naam}: its constraints on the object alone ({@code alfa}), its identity joins to objects other patterns bind
 * ({@code joins}), its other constraints between its object and an object or a value another pattern binds
 * ({@code betatoetsen}), its constraints on the object alone that come after those ({@code naAlfa}), and the values it
 * binds ({@code bindingen}), in that order. Drools keeps the constraints of a pattern in the order the model adds them,
 * so the place of a constraint is part of the plan: a form that guards an object which another part of the analysis
 * reached adds its guard after the constraints that reach it, as a fact creation does on the target of its path
 * ({@code TmsTranspiler.doelbewaking}).
 */
public record Patroon(Class<?> feitklasse, String naam, List<Alfa> alfa, List<Join> joins, List<Betatoets> betatoetsen,
                      List<Alfa> naAlfa, List<Binding> bindingen) implements LhsItem {

    public Patroon {
        alfa = List.copyOf(alfa);
        joins = List.copyOf(joins);
        betatoetsen = List.copyOf(betatoetsen);
        naAlfa = List.copyOf(naAlfa);
        bindingen = List.copyOf(bindingen);
    }

    /** A pattern without constraints on its object after its joins and its other constraints. */
    public Patroon(Class<?> feitklasse, String naam, List<Alfa> alfa, List<Join> joins, List<Betatoets> betatoetsen,
                   List<Binding> bindingen) {
        this(feitklasse, naam, alfa, joins, betatoetsen, List.of(), bindingen);
    }

    /** A pattern without constraints between its object and another pattern's but identity joins. */
    public Patroon(Class<?> feitklasse, String naam, List<Alfa> alfa, List<Join> joins, List<Binding> bindingen) {
        this(feitklasse, naam, alfa, joins, List.of(), List.of(), bindingen);
    }
}
