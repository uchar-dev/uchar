package dev.uchar.regelspraak.transpiler.tms.plan;

import java.util.List;

/**
 * A pattern over the objects of {@code factClass}, a fact class of the branch or a bean class, bound to the variable
 * {@code naam}: its constraints on the object alone ({@code alpha}), its identity joins to objects other patterns bind
 * ({@code joins}), its other constraints between its object and an object or a value another pattern binds
 * ({@code betaTests}), its constraints on the object alone that come after those ({@code afterAlpha}), and the values it
 * binds ({@code bindings}), in that order. Drools keeps the constraints of a pattern in the order the model adds them,
 * so the place of a constraint is part of the plan: a form that guards an object which another part of the analysis
 * reached adds its guard after the constraints that reach it, as a fact creation does on the target of its path
 * ({@code TmsTranspiler.targetGuard}).
 */
public record FactPattern(Class<?> factClass, String naam, List<Alpha> alpha, List<Join> joins, List<BetaTest> betaTests,
                      List<Alpha> afterAlpha, List<Binding> bindings) implements LhsItem {

    public FactPattern {
        alpha = List.copyOf(alpha);
        joins = List.copyOf(joins);
        betaTests = List.copyOf(betaTests);
        afterAlpha = List.copyOf(afterAlpha);
        bindings = List.copyOf(bindings);
    }

    /** A pattern without constraints on its object after its joins and its other constraints. */
    public FactPattern(Class<?> factClass, String naam, List<Alpha> alpha, List<Join> joins, List<BetaTest> betaTests,
                   List<Binding> bindings) {
        this(factClass, naam, alpha, joins, betaTests, List.of(), bindings);
    }

    /** A pattern without constraints between its object and another pattern's but identity joins. */
    public FactPattern(Class<?> factClass, String naam, List<Alpha> alpha, List<Join> joins, List<Binding> bindings) {
        this(factClass, naam, alpha, joins, List.of(), List.of(), bindings);
    }
}
