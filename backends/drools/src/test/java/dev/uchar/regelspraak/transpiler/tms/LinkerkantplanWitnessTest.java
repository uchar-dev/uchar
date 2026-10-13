package dev.uchar.regelspraak.transpiler.tms;

import dev.uchar.regelspraak.cases.DroolsBackend;
import dev.uchar.regelspraak.transpiler.tms.plan.Accumulatie;
import dev.uchar.regelspraak.transpiler.tms.plan.Bestaat;
import dev.uchar.regelspraak.transpiler.tms.plan.Lezing;
import dev.uchar.regelspraak.transpiler.tms.plan.LhsItem;
import dev.uchar.regelspraak.transpiler.tms.plan.Niet;
import dev.uchar.regelspraak.transpiler.tms.plan.Onderwerp;
import dev.uchar.regelspraak.transpiler.tms.plan.Patroon;
import dev.uchar.regelspraak.transpiler.tms.plan.Regelplan;
import dev.uchar.regelspraak.transpiler.tms.plan.Toetsitem;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The left-hand side of the exec-model branch's rules is planned before it is rendered (card
 * drl-teksttak-op-truth-maintenance, step 1a): {@code TmsTranspiler.Lezingen} decides what a rule reads and gives the
 * slots of its value array ({@code Leestabel}) and the items that bind them ({@code LhsItem}), and only
 * {@link PatternDslWeergave} makes the executable model of them. That plan is what a rendering into DRL text will take,
 * so this witness pins its shape for one golden: per form, how many plans, slots of each kind and items of each kind
 * (constraints, joins, bindings, folds). It counts; it does not pin the constraint ids, the fields, the join targets or
 * the order of the items, which the harness covers through the rules they render to. Since step 1d it counts over the
 * rule plans of the analysis ({@code Modelplan}), every rule of a form and every item of its left-hand side: the
 * carrier pattern a contribution rule binds before its subject, which step 1c planned, is counted as well. Until then
 * it counted the planned left-hand sides, which held only what {@code Lezingen} planned.
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
 * before the step is what the harness {@code Vuurspoor} shows, a dump of every firing and every fact of the corpus and
 * of the suite equal to the one before the step.
 *
 * <p><b>Red</b> before step 1a: {@code Lezingen} built the patterns itself and planned nothing, so the transpiler gave
 * no plan for any form.
 */
class LinkerkantplanWitnessTest {

    private static final Path GOLDEN = Path.of("..", "..", "corpus", "golden", "jutterswet-premierecht", "ir.json");

    @Test void de_linkerkant_van_elke_vorm_is_een_plan_uit_de_gesloten_woordenschat() throws Exception {
        DroolsBackend.Build bouw = DroolsBackend.build(GOLDEN, DroolsBackend.Branch.EXEC_MODEL);
        Map<String, Map<String, Integer>> verwacht = new TreeMap<>();
        verwacht.put("toekenning van een recht op premie", telling(
                "plan", 1, "lezing INVOER", 1, "onderwerp", 1, "binding op het onderwerp", 1, "toets", 1));
        verwacht.put("premie van een recht op premie", telling(
                "plan", 1, "lezing KENMERKSTAND", 1, "lezing INVOER", 1, "lezing PARAMETER", 1,
                "onderwerp", 1, "binding op het onderwerp", 1, "patroon ParameterStand", 1, "patroon KenmerkStand", 1,
                "alfa", 2, "join", 1, "binding", 2, "toets", 1));
        verwacht.put("premiebedrag van een jutter", telling(
                "plan", 2, "lezing ONDERWERP", 1, "lezing STAND", 1, "lezing VOUW_AANTAL", 1, "lezing VOUW_WAARDE", 1,
                "onderwerp", 2, "patroon Jutter", 1, "patroon Stand", 1, "accumulatie Bijdrage", 1, "functie AANTAL", 1,
                "functie SOM", 1, "alfa", 2, "join", 2, "binding", 2));
        verwacht.put("aantal toegekende rechten", telling(
                "plan", 2, "lezing KENMERKSTAND", 1, "lezing ONDERWERP", 1, "lezing VOUW_AANTAL", 1,
                "onderwerp", 2, "patroon Jutter", 1, "patroon KenmerkStand", 1, "accumulatie Bijdrage", 1,
                "functie AANTAL", 1, "alfa", 2, "join", 2, "binding", 1, "toets", 1));
        verwacht.put("premiegerechtigdheid", telling(
                "plan", 1, "lezing STAND", 1, "onderwerp", 1, "patroon Stand", 1, "alfa", 1, "join", 1, "binding", 1,
                "toets", 1));
        assertEquals(verwacht, perVorm(bouw.execModelTranspiler().modelplan().regels()));
    }

    /** Per form, the plans of its rules and the kinds of their slots and items. */
    private static Map<String, Map<String, Integer>> perVorm(List<Regelplan> plannen) {
        Map<String, Map<String, Integer>> uit = new TreeMap<>();
        for (Regelplan r : plannen) {
            if (r.vorm() == null) continue;
            Map<String, Integer> t = uit.computeIfAbsent(r.vorm(), v -> new TreeMap<>());
            tel(t, "plan");
            for (Lezing l : r.lezingen().slots()) tel(t, "lezing " + l.soort());
            for (LhsItem item : r.linkerkant()) {
                switch (item) {
                    case Onderwerp o -> {
                        tel(t, "onderwerp");
                        o.bindingen().forEach(b -> tel(t, "binding op het onderwerp"));
                    }
                    case Patroon p -> patroon(t, "patroon", p);
                    case Niet n -> n.patronen().forEach(p -> patroon(t, "niet", p));
                    case Bestaat b -> b.patronen().forEach(p -> patroon(t, "bestaat", p));
                    case Accumulatie a -> {
                        patroon(t, "accumulatie", a.bron());
                        a.functies().forEach(f -> tel(t, "functie " + f.soort()));
                    }
                    case Toetsitem x -> tel(t, "toets");
                }
            }
        }
        return uit;
    }

    private static void patroon(Map<String, Integer> t, String soort, Patroon p) {
        tel(t, soort + " " + p.feitklasse().getSimpleName());
        p.alfa().forEach(a -> tel(t, "alfa"));
        p.joins().forEach(j -> tel(t, "join"));
        p.bindingen().forEach(b -> tel(t, "binding"));
    }

    private static void tel(Map<String, Integer> t, String soort) {
        t.merge(soort, 1, Integer::sum);
    }

    private static Map<String, Integer> telling(Object... paren) {
        Map<String, Integer> uit = new TreeMap<>();
        for (int i = 0; i < paren.length; i += 2) uit.put((String) paren[i], (Integer) paren[i + 1]);
        return uit;
    }
}
