package dev.uchar.regelspraak.transpiler.tms.plan;

import java.util.List;

/**
 * The outcome of the analysis of one document: what a rendering into Drools' executable model or into DRL text takes
 * to make the whole model, deciding nothing itself.
 *
 * @param globals the globals the model declares, which the case runner sets by name before the session fires: the
 *                evaluation date first where a lowered form reads it, then the parameters the lowered rules read, in
 *                the order they were first read
 * @param regels  the plan of every rule of the model, in the order of the model: the rules of the forms that lowered,
 *                the rules that compare the writers of one target, the contribution rules of the folds and the rules
 *                of the links over a created relation, then the standings
 */
public record Modelplan(List<Global> globals, List<RulePlan> rules) {

    public Modelplan {
        globals = List.copyOf(globals);
        rules = List.copyOf(rules);
    }

    /**
     * A global the model declares.
     *
     * @param naam   the name the case runner sets it under, and the one a consequence that binds it names
     * @param klasse the Java class of its value
     */
    public record Global(String name, Class<?> javaClass) {}
}
