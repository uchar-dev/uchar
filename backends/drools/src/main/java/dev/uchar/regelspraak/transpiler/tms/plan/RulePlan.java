package dev.uchar.regelspraak.transpiler.tms.plan;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * One rule as the analysis plans it, complete: what a rendering into Drools' executable model or into DRL text takes,
 * deciding nothing itself.
 *
 * <p>A variable is declared once in its scope, and every slot has a variable of its own: a rendering spells a variable
 * by its name, so a second declaration of a name in scope would bind two values to one variable, or fail to build. The
 * plan refuses that at its construction, for every document. A declaration of the rule's top level (a pattern's
 * variable, the subject pattern's among them, a value a binding binds, the source of an accumulate and its results) is
 * in scope for every item after it; one inside a negation or an existence lives only inside it ({@link Exists}), so
 * two quantifications may each declare one name, while neither may declare a name in scope where it stands.
 *
 * @param naam        the rule's name, by which a KieBase keeps one rule
 * @param vorm        the form of the document the rule belongs to, as the manifest names it ({@code formOf}); null for
 *                    a rule of no form, such as a standing or the rule of two writers of one target
 * @param agendagroep the agenda group the rule fires in, null for the main group
 * @param lezingen    the slots of the rule's value array, which the test and the consequence read
 * @param linkerkant  the left-hand side in order: the subject pattern with its constraints and bindings where the rule
 *                    has a subject, the patterns, the negations, the accumulates and the tests, each constraint with
 *                    its id
 * @param gevolg      what the rule does when it fires
 */
public record RulePlan(String name, String form, String agendaGroup, ReadTable readings, List<LhsItem> leftSide,
                        Consequence consequence) {

    public RulePlan {
        leftSide = List.copyOf(leftSide);
        Set<String> inScope = new HashSet<>();
        for (LhsItem item : leftSide) {
            switch (item) {
                case Subject o -> {
                    declare(name, o.name(), inScope);
                    for (Binding b : o.bindings()) declare(name, b.target(), inScope);
                }
                case FactPattern p -> declare(name, p, inScope);
                case Accumulation a -> {
                    // the source's variables stay in scope, as the rendering into the executable model keeps them
                    declare(name, a.source(), inScope);
                    for (Accumulation.Aggregation f : a.aggregations()) declare(name, f.target(), inScope);
                }
                case Not n -> inside(name, n.inside(), inScope);
                case Exists b -> inside(name, b.inside(), inScope);
                case TestItem t -> { }
            }
        }
        Set<String> slotNames = new HashSet<>();
        for (Read l : readings.slots())
            if (l.name() != null && !slotNames.add(l.name()))
                throw new IllegalArgumentException("de regel " + name + " geeft twee slots de variabele " + l.name());
    }

    private static void declare(String rule, FactPattern p, Set<String> inScope) {
        declare(rule, p.name(), inScope);
        for (Binding b : p.bindings()) declare(rule, b.target(), inScope);
    }

    private static void declare(String rule, String variable, Set<String> inScope) {
        if (!inScope.add(variable))
            throw new IllegalArgumentException("de regel " + rule + " declareert de variabele " + variable
                    + " twee keer");
    }

    /** The variables a quantification declares, which live only inside it: none may be in scope where it stands
     *  ({@link Exists} holds them distinct among themselves). */
    private static void inside(String rule, List<String> inside, Set<String> inScope) {
        for (String v : inside)
            if (inScope.contains(v))
                throw new IllegalArgumentException("de regel " + rule + " declareert de variabele " + v
                        + " twee keer");
    }
}
