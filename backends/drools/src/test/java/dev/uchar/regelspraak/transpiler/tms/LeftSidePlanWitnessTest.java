package dev.uchar.regelspraak.transpiler.tms;

import dev.uchar.regelspraak.cases.DroolsBackend;
import dev.uchar.regelspraak.transpiler.tms.plan.Accumulation;
import dev.uchar.regelspraak.transpiler.tms.plan.Exists;
import dev.uchar.regelspraak.transpiler.tms.plan.Read;
import dev.uchar.regelspraak.transpiler.tms.plan.LhsItem;
import dev.uchar.regelspraak.transpiler.tms.plan.Not;
import dev.uchar.regelspraak.transpiler.tms.plan.Subject;
import dev.uchar.regelspraak.transpiler.tms.plan.FactPattern;
import dev.uchar.regelspraak.transpiler.tms.plan.RulePlan;
import dev.uchar.regelspraak.transpiler.tms.plan.TestItem;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The left-hand side of the exec-model branch's rules is planned before it is rendered (card
 * drl-teksttak-op-truth-maintenance, step 1a): {@code TmsTranspiler.Reads} decides what a rule reads and gives the
 * slots of its value array ({@code ReadTable}) and the items that bind them ({@code LhsItem}), and only
 * {@link PatternDslRendering} makes the executable model of them. That plan is what a rendering into DRL text will take,
 * so this witness pins its shape for one golden: per form, how many plans, slots of each kind and items of each kind
 * (constraints, joins, bindings, folds). It counts; it does not pin the constraint ids, the fields, the join targets or
 * the order of the items, which the harness covers through the rules they render to. Since step 1d it counts over the
 * rule plans of the analysis ({@code Modelplan}), every rule of a form and every item of its left-hand side: the
 * carrier pattern a contribution rule binds before its subject, which step 1c planned, is counted as well. Until then
 * it counted the planned left-hand sides, which held only what {@code Reads} planned.
 *
 * <p>The golden {@code jutterswet-premierecht} has the reads the step planned in one small document: an input
 * member bound off the subject ({@code toekenning van een recht op premie}), a derived characteristic and a parameter
 * through their standing facts ({@code premie van een recht op premie}), a derived attribute
 * ({@code premiegerechtigdheid}), and two folds, a sum and a count with a filter, each with its contribution rule, which
 * reads the member itself and its value or its filter ({@code premiebedrag van een jutter},
 * {@code aantal toegekende rechten}). The counts follow from the document: a condition is one test, a derived member
 * one standing pattern with a constraint on the member, a join on the subject and a binding of the value, a parameter
 * one pattern without a join, a fold one accumulate whose count is always bound beside the folded value, and its
 * contribution rule one pattern of the fold's subject, the {@code Jutter}, before its own subject pattern.
 *
 * <p>The witness reads the analysis's plans, not the rules: that the rendering of the plans builds the same rules as
 * before the step is what the harness {@code FireTrace} shows, a dump of every firing and every fact of the corpus and
 * of the suite equal to the one before the step.
 *
 * <p><b>Red</b> before step 1a: {@code Reads} built the patterns itself and planned nothing, so the transpiler gave
 * no plan for any form.
 */
class LeftSidePlanWitnessTest {

    private static final Path GOLDEN = Path.of("..", "..", "corpus", "golden", "jutterswet-premierecht", "ir.json");

    @Test void the_left_side_of_every_form_is_a_plan_out_of_the_closed_vocabulary() throws Exception {
        DroolsBackend.Build build = DroolsBackend.build(GOLDEN, DroolsBackend.Branch.EXEC_MODEL);
        Map<String, Map<String, Integer>> expected = new TreeMap<>();
        expected.put("toekenning van een recht op premie", count(
                "plan", 1, "reading INPUT", 1, "subject", 1, "binding on the subject", 1, "test", 1));
        expected.put("premie van een recht op premie", count(
                "plan", 1, "reading CHARACTERISTIC_STATE", 1, "reading INPUT", 1, "reading PARAMETER", 1,
                "subject", 1, "binding on the subject", 1, "pattern ParameterState", 1, "pattern CharacteristicState", 1,
                "alpha", 2, "join", 1, "binding", 2, "test", 1));
        expected.put("premiebedrag van een jutter", count(
                "plan", 2, "reading SUBJECT", 1, "reading STATE", 1, "reading FOLD_COUNT", 1, "reading FOLDED_VALUE", 1,
                "subject", 2, "pattern Jutter", 1, "pattern State", 1, "accumulation Contribution", 1, "function COUNT", 1,
                "function SUM", 1, "alpha", 2, "join", 2, "binding", 2));
        expected.put("aantal toegekende rechten", count(
                "plan", 2, "reading CHARACTERISTIC_STATE", 1, "reading SUBJECT", 1, "reading FOLD_COUNT", 1,
                "subject", 2, "pattern Jutter", 1, "pattern CharacteristicState", 1, "accumulation Contribution", 1,
                "function COUNT", 1, "alpha", 2, "join", 2, "binding", 1, "test", 1));
        expected.put("premiegerechtigdheid", count(
                "plan", 1, "reading STATE", 1, "subject", 1, "pattern State", 1, "alpha", 1, "join", 1, "binding", 1,
                "test", 1));
        assertEquals(expected, perForm(build.execModelTranspiler().modelplan().rules()));
    }

    /** Per form, the plans of its rules and the kinds of their slots and items. */
    private static Map<String, Map<String, Integer>> perForm(List<RulePlan> plans) {
        Map<String, Map<String, Integer>> out = new TreeMap<>();
        for (RulePlan r : plans) {
            if (r.form() == null) continue;
            Map<String, Integer> t = out.computeIfAbsent(r.form(), v -> new TreeMap<>());
            tally(t, "plan");
            for (Read l : r.readings().slots()) tally(t, "reading " + l.kind());
            for (LhsItem item : r.leftSide()) {
                switch (item) {
                    case Subject o -> {
                        tally(t, "subject");
                        o.bindings().forEach(b -> tally(t, "binding on the subject"));
                    }
                    case FactPattern p -> pattern(t, "pattern", p);
                    case Not n -> n.patterns().forEach(p -> pattern(t, "not", p));
                    case Exists b -> b.patterns().forEach(p -> pattern(t, "exists", p));
                    case Accumulation a -> {
                        pattern(t, "accumulation", a.source());
                        a.aggregations().forEach(f -> tally(t, "function " + f.kind()));
                    }
                    case TestItem x -> tally(t, "test");
                }
            }
        }
        return out;
    }

    private static void pattern(Map<String, Integer> t, String category, FactPattern p) {
        tally(t, category + " " + p.factClass().getSimpleName());
        p.alpha().forEach(a -> tally(t, "alpha"));
        p.joins().forEach(j -> tally(t, "join"));
        p.bindings().forEach(b -> tally(t, "binding"));
    }

    private static void tally(Map<String, Integer> t, String category) {
        t.merge(category, 1, Integer::sum);
    }

    private static Map<String, Integer> count(Object... pairs) {
        Map<String, Integer> out = new TreeMap<>();
        for (int i = 0; i < pairs.length; i += 2) out.put((String) pairs[i], (Integer) pairs[i + 1]);
        return out;
    }
}
