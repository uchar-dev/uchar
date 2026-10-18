package dev.uchar.regelspraak.transpiler.tms;

import dev.uchar.regelspraak.cases.DroolsBackend;
import dev.uchar.regelspraak.transpiler.tms.plan.StaticFunction;
import dev.uchar.regelspraak.transpiler.tms.plan.Consequence;
import dev.uchar.regelspraak.transpiler.tms.plan.RulePlan;
import dev.uchar.regelspraak.transpiler.tms.plan.Term;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The values of the exec-model branch's rules are planned before they are rendered (card
 * drl-teksttak-op-truth-maintenance, step 1b): the value compilers of {@code TmsTranspiler} give a tree of calls
 * ({@link Term}), and only {@link ClosureRendering} makes the functions a consequence calls of it. That tree is what a
 * rendering into DRL text will take, so this witness pins it node by node, for the values of two goldens, in two
 * lists per golden:
 * <ul>
 *   <li>a tree text: the kind of every node, the function each call, restatement, division, square root and listed
 *       fold names, the unit factors and the two units of a restatement, the probe slot of a division, the value of a
 *       constant and the unit of its type, the slots, the binders, and whether a {@code Let} may be evaluated
 *       eagerly;</li>
 *   <li>per node in pre-order, with its position in the tree: its kind, its source fragment where it carries one (the
 *       IR's {@code source.snippet}, which a rendering into DRL text puts above the text), and of a constant the class
 *       of its value and its whole IR type, as the type's record prints it: kind, wholeness, decimals, sign and unit
 *       with all its fields.</li>
 * </ul>
 * Not pinned: what a node holds as a Java object rather than as a decision, the predicate of a day kind and of a
 * compiled condition. That the rendering of these trees computes what the compilers computed before the step is what
 * the harness {@code FireTrace} shows, a dump of every firing and every fact of the corpus and of the suite equal to
 * the one before the step.
 *
 * <p>Two goldens, since no golden with cases has both a {@code Let} and a runtime call whose operand is restated into
 * another unit (measured over the 250 goldens the branch plays):
 * <ul>
 *   <li>{@code jutterswet-sleepbaan}: {@code 2,5 km plus de aanloop}, the aanloop in m, written into a target in m.
 *       The sum restates its right operand into the left one's unit, by the exact factors 1/1000, and the write
 *       boundary restates the sum into the target's unit, by 1000/1 (RUNTIME-SEMANTICS ch. 2.6).</li>
 *   <li>{@code anchor/jutterswet-tariefstelling}: three nested {@code Let}s around an {@code IfThenElse}, a division
 *       with its probe, and a {@code Let} whose body reads its binder first. The outermost {@code Let} is not eager:
 *       its binder is read only in the value of an inner one, which the condition of the choice forces, and the
 *       analysis counts a choice as reading something else first. Since step 1b2 the condition is a planned
 *       comparison of two binders.</li>
 * </ul>
 *
 * <p>Since step 1d the witness reads the rule plans of the analysis ({@code Modelplan}), in the order of the model:
 * the value the consequence of a rule of a form writes, of a {@link Consequence.Value}, of each initialisation of a
 * {@link Consequence.Creation} and of a {@link Consequence.Decision}. Until then the analysis recorded those values apart, in the
 * order the rules were built, without the decision of a characteristic default and the agreed decision of a defaults
 * stack, which neither golden has; for both goldens the lists are the same.
 *
 * <p><b>Red</b> before the step: the value compilers made the functions themselves and planned nothing, so the
 * transpiler gave no planned value. Before the fix round of step 1b, a restatement and a listed fold named no function,
 * so a rendering had to choose it again, and the source fragments below the root and the type of a constant beyond its
 * unit went unpinned.
 */
class ValueTermWitnessTest {

    private static final Path GOLDEN = Path.of("..", "..", "corpus", "golden");

    @Test void a_runtime_call_with_unit_rescaling_is_a_tree_of_named_calls() throws Exception {
        List<RulePlan> plans = plans("jutterswet-sleepbaan");
        assertEquals(List.of(
                "sleeplengte van een sleepbaan: Herschaal RESTATE_EXACT km->m 1000/1 (RS_PLUS(Const 2.5 km, "
                        + "Herschaal RESTATE_EXACT m->km 1/1000 (Lees 0)))"), trees(plans));
        // the write boundary has no node of its own in the IR: it carries the fragment of the value it restates, and so
        // does the restatement of the right operand
        assertEquals(List.of(
                "sleeplengte van een sleepbaan 0 Restate bron '2,5 km plus de aanloop van de Sleepbaan'",
                "sleeplengte van een sleepbaan 0.0 Call bron '2,5 km plus de aanloop van de Sleepbaan'",
                "sleeplengte van een sleepbaan 0.0.0 Const BigDecimal type Numeriek[geheel=false, decimalen=1, "
                        + "teken=ONBEPAALD, eenheid=Eenheid[afkorting=km, stelsel=Afstand, canoniek=m, "
                        + "factorNaarCanoniek=1000, denominator=null]] bron '2,5 km'",
                "sleeplengte van een sleepbaan 0.0.1 Restate bron 'de aanloop van de Sleepbaan'",
                "sleeplengte van een sleepbaan 0.0.1.0 SlotValue bron 'de aanloop van de Sleepbaan'"), fields(plans));
    }

    @Test void a_let_is_a_node_with_its_binder_and_its_eagerness() throws Exception {
        List<RulePlan> plans = plans("anchor/jutterswet-tariefstelling");
        assertEquals(List.of(
                "aanslag van een jutter: Let 1 ink [Lees 0] in [Let 3 aftrek [RS_TIMES(Lees 2, Const 250)] in "
                        + "[Let 4 belasting [RS_ROUNDING(RS_TIMES(Binder 1 ink, Const 0.21), Const 2, Const HALF_UP)] in "
                        + "[Keuze(RS_AGG_COMPARE(Binder 3 aftrek, Binder 4 belasting, Const >), Const 0, "
                        + "RS_MIN(Binder 4 belasting, Binder 3 aftrek))]]]",
                "factor van een jutter: RS_PLUS(Const 1, Deel DIVIDED sonde 1 (Lees 0, Const 2))",
                "gewogen inkomen van een jutter: RS_ROUNDING(Let 1 ink2 gretig [Lees 0] in "
                        + "[RS_TIMES(Binder 1 ink2, Lees 2)], Const 2, Const HALF_UP)"), trees(plans));
        // the constants of the analysis, the decimals and the rounding mode, have no type and no node in the IR
        assertEquals(List.of(
                "aanslag van een jutter 0 Let bron 'ink is het inkomen van de Jutter'",
                "aanslag van een jutter 0.0 SlotValue bron 'het inkomen van de Jutter'",
                "aanslag van een jutter 0.1 Let bron 'aftrek is het kindertal van de Jutter maal 250,00'",
                "aanslag van een jutter 0.1.0 Call bron 'het kindertal van de Jutter maal 250,00'",
                "aanslag van een jutter 0.1.0.0 SlotValue bron 'het kindertal van de Jutter'",
                "aanslag van een jutter 0.1.0.1 Const BigDecimal type "
                        + "Numeriek[geheel=false, decimalen=2, teken=ONBEPAALD, eenheid=null] bron '250,00'",
                "aanslag van een jutter 0.1.1 Let bron 'belasting is ink maal 0,2100 rekenkundig afgerond op 2 "
                        + "decimalen'",
                "aanslag van een jutter 0.1.1.0 Call bron 'ink maal 0,2100 rekenkundig afgerond op 2 decimalen'",
                "aanslag van een jutter 0.1.1.0.0 Call bron 'ink maal 0,2100'",
                "aanslag van een jutter 0.1.1.0.0.0 Binder bron 'ink'",
                "aanslag van een jutter 0.1.1.0.0.1 Const BigDecimal type "
                        + "Numeriek[geheel=false, decimalen=4, teken=ONBEPAALD, eenheid=null] bron '0,2100'",
                "aanslag van een jutter 0.1.1.0.1 Const Integer",
                "aanslag van een jutter 0.1.1.0.2 Const RoundingMode",
                "aanslag van een jutter 0.1.1.1 Choice bron 'als aftrek groter dan belasting dan 0,00 anders belasting "
                        + "min aftrek'",
                "aanslag van een jutter 0.1.1.1.0 Call bron 'aftrek groter dan belasting'",
                "aanslag van een jutter 0.1.1.1.0.0 Binder bron 'aftrek'",
                "aanslag van een jutter 0.1.1.1.0.1 Binder bron 'belasting'",
                "aanslag van een jutter 0.1.1.1.0.2 Const String",
                "aanslag van een jutter 0.1.1.1.1 Const BigDecimal type "
                        + "Numeriek[geheel=false, decimalen=2, teken=ONBEPAALD, eenheid=null] bron '0,00'",
                "aanslag van een jutter 0.1.1.1.2 Call bron 'belasting min aftrek'",
                "aanslag van een jutter 0.1.1.1.2.0 Binder bron 'belasting'",
                "aanslag van een jutter 0.1.1.1.2.1 Binder bron 'aftrek'",
                "factor van een jutter 0 Call bron '1,0000 plus het kindertal van de Jutter gedeeld door 2,0000'",
                "factor van een jutter 0.0 Const BigDecimal type "
                        + "Numeriek[geheel=false, decimalen=4, teken=ONBEPAALD, eenheid=null] bron '1,0000'",
                "factor van een jutter 0.1 Division bron 'het kindertal van de Jutter gedeeld door 2,0000'",
                "factor van een jutter 0.1.0 SlotValue bron 'het kindertal van de Jutter'",
                "factor van een jutter 0.1.1 Const BigDecimal type "
                        + "Numeriek[geheel=false, decimalen=4, teken=ONBEPAALD, eenheid=null] bron '2,0000'",
                "gewogen inkomen van een jutter 0 Call bron 'ink2 maal de factor van de Jutter rekenkundig "
                        + "afgerond op 2 decimalen'",
                "gewogen inkomen van een jutter 0.0 Let bron 'ink2 is het inkomen van de Jutter'",
                "gewogen inkomen van een jutter 0.0.0 SlotValue bron 'het inkomen van de Jutter'",
                "gewogen inkomen van een jutter 0.0.1 Call bron 'ink2 maal de factor van de Jutter'",
                "gewogen inkomen van een jutter 0.0.1.0 Binder bron 'ink2'",
                "gewogen inkomen van een jutter 0.0.1.1 SlotValue bron 'de factor van de Jutter'",
                "gewogen inkomen van een jutter 0.1 Const Integer",
                "gewogen inkomen van een jutter 0.2 Const RoundingMode"), fields(plans));
    }

    /** A rendering into DRL text names every function of the plan in an {@code import function}: each constant names a
     *  public static method of its class. */
    @Test void every_function_of_the_plan_is_a_public_static_method() {
        List<String> fout = new ArrayList<>();
        for (StaticFunction f : StaticFunction.values()) {
            boolean found = false;
            for (Method m : f.javaClass().getMethods())
                if (m.getName().equals(f.methode()) && Modifier.isStatic(m.getModifiers())) found = true;
            if (!found || !Modifier.isPublic(f.javaClass().getModifiers())) fout.add(f + " " + f.javaClass().getName() + "."
                    + f.methode());
        }
        assertEquals(List.of(), fout);
    }

    /** The plans of the rules of the forms of {@code golden}, in the order of the model. */
    private static List<RulePlan> plans(String golden) throws Exception {
        return DroolsBackend.build(GOLDEN.resolve(golden).resolve("ir.json"), DroolsBackend.Branch.EXEC_MODEL)
                .execModelTranspiler().modelplan().rules().stream().filter(p -> p.vorm() != null).toList();
    }

    /** The values the consequence {@code g} writes: of a value, of each initialisation of a creation, of a decision. */
    private static List<Term> waarden(Consequence g) {
        return switch (g) {
            case Consequence.Value w -> List.of(w.waarde());
            case Consequence.Creation c -> c.initialisations().stream().map(Consequence.Initialisation::waarde).toList();
            case Consequence.Decision a -> List.of(a.waarde());
            default -> List.of();
        };
    }

    private static List<String> trees(List<RulePlan> plans) {
        List<String> out = new ArrayList<>();
        for (RulePlan p : plans) for (Term w : waarden(p.consequence())) out.add(p.vorm() + ": " + TermTree.tree(w));
        return out;
    }

    /** Per value, every node of its tree in pre-order, with what the tree text leaves out ({@link TermTree#fields}). */
    private static List<String> fields(List<RulePlan> plans) {
        List<String> out = new ArrayList<>();
        for (RulePlan p : plans) for (Term w : waarden(p.consequence())) TermTree.fields(p.vorm(), w, "0", out);
        return out;
    }
}
