package dev.uchar.regelspraak.transpiler.tms.plan;

import java.util.List;

/** The pattern of the rule's subject: a bean of {@code beanClass}, bound to the variable {@code name}, with its
 *  constraints on the bean alone ({@code alpha}), its identity joins to objects a pattern before it binds
 *  ({@code joins}), its other constraints between the bean and such an object ({@code betaTests}), and the values the
 *  rule binds off it, in that order. */
public record Subject(Class<?> beanClass, String name, List<Alpha> alpha, List<Join> joins,
                        List<BetaTest> betaTests, List<Binding> bindings) implements LhsItem {
    public Subject {
        alpha = List.copyOf(alpha);
        joins = List.copyOf(joins);
        betaTests = List.copyOf(betaTests);
        bindings = List.copyOf(bindings);
    }

    /** A subject pattern without constraints between the bean and another pattern's object but identity joins. */
    public Subject(Class<?> beanClass, String name, List<Alpha> alpha, List<Join> joins, List<Binding> bindings) {
        this(beanClass, name, alpha, joins, List.of(), bindings);
    }
}
