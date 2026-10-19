package dev.uchar.regelspraak.transpiler.tms;

import dev.uchar.regelspraak.cases.DroolsBackend;
import dev.uchar.regelspraak.transpiler.tms.plan.LhsItem;
import dev.uchar.regelspraak.transpiler.tms.plan.RulePlan;
import dev.uchar.regelspraak.transpiler.tms.plan.Term;
import dev.uchar.regelspraak.transpiler.tms.plan.TestItem;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The conditions of the exec-model branch's rules are planned before they are rendered (card
 * drl-teksttak-op-truth-maintenance, step 1b2): the condition compilers of {@code TmsTranspiler} give a tree of calls
 * whose value is a truth value ({@link Term}), and only {@link ClosureRendering} makes the predicate a test or a
 * consequence calls of it. Every decision a compiled condition held in its lambda is a node or a field of a node: the
 * helper that compares, by name; the operator, the polarity and the comparand as constants; how leeg and a fault work
 * through, by the function and by the division, the fold read and the full-timeline read it names; the members, the
 * quantifier and the threshold of a composite, which stops at the first point from which its verdict stands
 * (RUNTIME-SEMANTICS ch. 5.10); the whole-period fold with its grid and test code; and the probe of a deposit. This
 * witness pins those trees for the tests of two goldens, in two lists per golden:
 * <ul>
 *   <li>per rule of a form, in the order of the model: its form, the role of each test, the probe slot of a test
 *       of deposits, and the condition as a tree text ({@link TermTree#tree}): every node by its kind with the fields
 *       the analysis decided, the slots it reads, and the value of each constant;</li>
 *   <li>per test, every node of its condition in pre-order with its position, its kind, its source fragment
 *       where it carries one (the IR's {@code source.snippet}, which a rendering into DRL text puts above the text),
 *       and of a constant the class of its value and its whole IR type ({@link TermTree#fields}). A test whose list
 *       equals, line for line, that of the test before it of the same form is named with "zoals de vorige toets"
 *       instead of repeating the list.</li>
 * </ul>
 * Not pinned: the predicate of a day kind ({@link Term.DayKind}), which neither golden has. That the rendering of
 * these trees tests what the compiled conditions tested before the step is what the harness {@code FireTrace} shows, a
 * dump of every firing and every fact of the corpus and of the suite equal to the one before the step.
 *
 * <ul>
 *   <li>{@code jutterswet-filterwacht}: "drukte" is a composite under all, a characteristic and then a comparison of a
 *       count fold, which stops where the characteristic does not hold, so the fold, whose filter divides by a numeric
 *       zero for J1's second vondst, is not read there; the fold's read carries its deposit, its filter fault and its
 *       probe ({@code FoldRead}). The filter of that fold and of the fold of "zware vondsten" is a composite of one
 *       comparison of a quotient: a leeg divisor makes the quotient leeg with a deposit to the probe in slot 2
 *       ({@code Deel}), and the comparison takes a leeg operand as onwaar ({@code RS_AGG_COMPARE}). Only the test of
 *       deposits fills a probe, so in the other roles the probe of the quotient and of the fold's read is -1 (step 1e
 *       of the card).</li>
 *   <li>{@code jutterswet-vast-startpunt}: the gevuld test and a comparison under the whole-period qualifier, each over
 *       the timeline read whole, which faults on an asOfDate-bucket input ({@code VOLLEDIG}), the gevuld test without a
 *       bound and the comparison with the bound 0.</li>
 * </ul>
 *
 * <p>Since step 1d the witness reads the rule plans of the analysis ({@code Modelplan}). Until then it read the
 * planned left-hand sides in the order they were built; the trees are the same, and the contribution rules of the
 * folds now stand after the rules of the forms.
 *
 * <p><b>Red</b> before step 1b2: the condition compilers made the predicates themselves, so every planned test was a
 * {@code GecompileerdeToets}.
 */
class ConditionTermWitnessTest {

    private static final Path GOLDEN = Path.of("..", "..", "corpus", "golden");

    @Test void a_compound_condition_is_a_tree_with_its_short_circuit() throws Exception {
        List<RulePlan> plans = rulePlans("jutterswet-filterwacht");
        assertEquals(List.of(
                "vondstental: geen toets",
                "zware vondsten CONDITION: KENMERK(Read 0, Const true)",
                "drukte WITHOUT_FAULT: Compound ALLE 0(KENMERK(Read 0, Const true), RS_AGG_COMPARE(FoldRead 3"
                        + " -1 2 -1 (COUNT(Read 1)), Const 0, Const >))",
                "drukte FAULT: Compound ALLE 0(KENMERK(Read 0, Const true), RS_AGG_COMPARE(FoldRead 3 -1 2"
                        + " -1 (COUNT(Read 1)), Const 0, Const >))",
                "drukte DEPOSIT probe 4: Compound ALLE 0(KENMERK(Read 0, Const true), "
                        + "RS_AGG_COMPARE(FoldRead 3 4 2 -1 (COUNT(Read 1)), Const 0, Const >))",
                "vondstental: geen toets",
                "zware vondsten WITHOUT_FAULT: Compound DE 0(RS_AGG_COMPARE(Division DIVIDED probe -1 (Read 0, Read"
                        + " 1), Const 2, Const >))",
                "zware vondsten FAULT: Compound DE 0(RS_AGG_COMPARE(Division DIVIDED probe -1 (Read 2, Read 3),"
                        + " Const 2, Const >))",
                "zware vondsten DEPOSIT probe 2: Compound DE 0(RS_AGG_COMPARE(Division DIVIDED probe 2 (Read "
                        + "0, Read 1), Const 2, Const >))",
                "drukte WITHOUT_FAULT: Compound DE 0(RS_AGG_COMPARE(Division DIVIDED probe -1 (Read 0, Read 1),"
                        + " Const 2, Const >))",
                "drukte FAULT: Compound DE 0(RS_AGG_COMPARE(Division DIVIDED probe -1 (Read 2, Read 3), Const 2,"
                        + " Const >))",
                "drukte DEPOSIT probe 2: Compound DE 0(RS_AGG_COMPARE(Division DIVIDED probe 2 (Read 0, Read "
                        + "1), Const 2, Const >))"), trees(plans));
        assertEquals(List.of(
                "zware vondsten CONDITION 0 Call source 'hij actief is'",
                "zware vondsten CONDITION 0.0 SlotValue",
                "zware vondsten CONDITION 0.1 Const Boolean",
                "drukte WITHOUT_FAULT 0 Compound source 'hij aan alle volgende voorwaarden voldoet: - hij is actief "
                        + "- het aantal vondsten van de Jutter die aan de volgende voorwaarde voldoen: zijn gewicht "
                        + "gedeeld door zijn deelnemers is groter dan 2 is groter dan 0.'",
                "drukte WITHOUT_FAULT 0.0 Call source 'hij is actief'",
                "drukte WITHOUT_FAULT 0.0.0 SlotValue",
                "drukte WITHOUT_FAULT 0.0.1 Const Boolean",
                "drukte WITHOUT_FAULT 0.1 Call source 'het aantal vondsten van de Jutter die aan de volgende "
                        + "voorwaarde voldoen: zijn gewicht gedeeld door zijn deelnemers is groter dan 2 is groter "
                        + "dan 0'",
                "drukte WITHOUT_FAULT 0.1.0 FoldRead source 'het aantal vondsten van de Jutter die aan de volgende "
                        + "voorwaarde voldoen: zijn gewicht gedeeld door zijn deelnemers is groter dan 2'",
                "drukte WITHOUT_FAULT 0.1.0.0 Call source 'het aantal vondsten van de Jutter die aan de volgende "
                        + "voorwaarde voldoen: zijn gewicht gedeeld door zijn deelnemers is groter dan 2'",
                "drukte WITHOUT_FAULT 0.1.0.0.0 SlotValue",
                "drukte WITHOUT_FAULT 0.1.1 Const BigDecimal type Numeric[wholeNumber=true, decimals=0, sign=ONBEPAALD, "
                        + "unit=null] source '0'",
                "drukte WITHOUT_FAULT 0.1.2 Const String",
                "drukte FAULT: zoals de vorige toets",
                "drukte DEPOSIT probe 4: zoals de vorige toets",
                "zware vondsten WITHOUT_FAULT 0 Compound source 'aan de volgende voorwaarde voldoen: zijn gewicht "
                        + "gedeeld door zijn deelnemers is groter dan 2'",
                "zware vondsten WITHOUT_FAULT 0.0 Call source 'zijn gewicht gedeeld door zijn deelnemers is groter "
                        + "dan 2'",
                "zware vondsten WITHOUT_FAULT 0.0.0 Division source 'zijn gewicht gedeeld door zijn deelnemers'",
                "zware vondsten WITHOUT_FAULT 0.0.0.0 SlotValue source 'zijn gewicht'",
                "zware vondsten WITHOUT_FAULT 0.0.0.1 SlotValue source 'zijn deelnemers'",
                "zware vondsten WITHOUT_FAULT 0.0.1 Const BigDecimal type Numeric[wholeNumber=true, decimals=0, "
                        + "sign=ONBEPAALD, unit=null] source '2'",
                "zware vondsten WITHOUT_FAULT 0.0.2 Const String",
                "zware vondsten FAULT: zoals de vorige toets",
                "zware vondsten DEPOSIT probe 2: zoals de vorige toets",
                "drukte WITHOUT_FAULT 0 Compound source 'aan de volgende voorwaarde voldoen: zijn gewicht gedeeld "
                        + "door zijn deelnemers is groter dan 2'",
                "drukte WITHOUT_FAULT 0.0 Call source 'zijn gewicht gedeeld door zijn deelnemers is groter dan 2'",
                "drukte WITHOUT_FAULT 0.0.0 Division source 'zijn gewicht gedeeld door zijn deelnemers'",
                "drukte WITHOUT_FAULT 0.0.0.0 SlotValue source 'zijn gewicht'",
                "drukte WITHOUT_FAULT 0.0.0.1 SlotValue source 'zijn deelnemers'",
                "drukte WITHOUT_FAULT 0.0.1 Const BigDecimal type Numeric[wholeNumber=true, decimals=0, sign=ONBEPAALD, "
                        + "unit=null] source '2'",
                "drukte WITHOUT_FAULT 0.0.2 Const String",
                "drukte FAULT: zoals de vorige toets",
                "drukte DEPOSIT probe 2: zoals de vorige toets"), fields(plans));
    }

    @Test void a_whole_period_test_is_a_call_over_the_full_timeline() throws Exception {
        List<RulePlan> plans = rulePlans("jutterswet-vast-startpunt");
        assertEquals(List.of(
                "stormwachtdagen van een jutter: geen toets",
                "maandwachtdagen van een jutter: geen toets",
                "toelagesom van een jutter: geen toets",
                "maandtoelagesom van een jutter: geen toets",
                "stormpremie van een jutter WITHOUT_FAULT: WHOLE_PERIOD(COMPLETE(Read 0, Const the whole-period "
                        + "qualifier over \"stormwachtdagen\" needs the full timeline; , Read 1, Const  supplies only "
                        + "the asOfDate-bucket scalar (ch. 3.9, ch. 8.4)), Const null, Const JAAR, Const 6, Const "
                        + "false)",
                "stormpremie van een jutter FAULT: WHOLE_PERIOD(COMPLETE(Read 0, Const the whole-period qualifier "
                        + "over \"stormwachtdagen\" needs the full timeline; , Read 1, Const  supplies only the "
                        + "asOfDate-bucket scalar (ch. 3.9, ch. 8.4)), Const null, Const JAAR, Const 6, Const false)",
                "maandpremie van een jutter WITHOUT_FAULT: WHOLE_PERIOD(COMPLETE(Read 0, Const the whole-period "
                        + "qualifier over \"maandwachtdagen\" needs the full timeline; , Read 1, Const  supplies only "
                        + "the asOfDate-bucket scalar (ch. 3.9, ch. 8.4)), Const 0, Const MAAND, Const 2, Const "
                        + "false)",
                "maandpremie van een jutter FAULT: WHOLE_PERIOD(COMPLETE(Read 0, Const the whole-period qualifier "
                        + "over \"maandwachtdagen\" needs the full timeline; , Read 1, Const  supplies only the "
                        + "asOfDate-bucket scalar (ch. 3.9, ch. 8.4)), Const 0, Const MAAND, Const 2, Const false)",
                "weektoelagesom van een jutter: geen toets",
                "kwartaaltoelagesom van een jutter: geen toets"), trees(plans));
        assertEquals(List.of(
                "stormpremie van een jutter WITHOUT_FAULT 0 Call source 'zijn stormwachtdagen gedurende het gehele "
                        + "jaar gevuld is'",
                "stormpremie van een jutter WITHOUT_FAULT 0.0 Call source 'zijn stormwachtdagen'",
                "stormpremie van een jutter WITHOUT_FAULT 0.0.0 SlotValue source 'zijn stormwachtdagen'",
                "stormpremie van een jutter WITHOUT_FAULT 0.0.1 Const String",
                "stormpremie van een jutter WITHOUT_FAULT 0.0.2 SlotValue",
                "stormpremie van een jutter WITHOUT_FAULT 0.0.3 Const String",
                "stormpremie van een jutter WITHOUT_FAULT 0.1 Const null",
                "stormpremie van een jutter WITHOUT_FAULT 0.2 Const String",
                "stormpremie van een jutter WITHOUT_FAULT 0.3 Const Integer",
                "stormpremie van een jutter WITHOUT_FAULT 0.4 Const Boolean",
                "stormpremie van een jutter FAULT: zoals de vorige toets",
                "maandpremie van een jutter WITHOUT_FAULT 0 Call source 'zijn maandwachtdagen gedurende de gehele "
                        + "maand groter is dan 0'",
                "maandpremie van een jutter WITHOUT_FAULT 0.0 Call source 'zijn maandwachtdagen'",
                "maandpremie van een jutter WITHOUT_FAULT 0.0.0 SlotValue source 'zijn maandwachtdagen'",
                "maandpremie van een jutter WITHOUT_FAULT 0.0.1 Const String",
                "maandpremie van een jutter WITHOUT_FAULT 0.0.2 SlotValue",
                "maandpremie van een jutter WITHOUT_FAULT 0.0.3 Const String",
                "maandpremie van een jutter WITHOUT_FAULT 0.1 Const BigDecimal type Numeric[wholeNumber=true, "
                        + "decimals=0, sign=ONBEPAALD, unit=null] source '0'",
                "maandpremie van een jutter WITHOUT_FAULT 0.2 Const String",
                "maandpremie van een jutter WITHOUT_FAULT 0.3 Const Integer",
                "maandpremie van een jutter WITHOUT_FAULT 0.4 Const Boolean",
                "maandpremie van een jutter FAULT: zoals de vorige toets"), fields(plans));
    }

    /** The plans of the rules of the forms of {@code golden}, in the order of the model. */
    private static List<RulePlan> rulePlans(String golden) throws Exception {
        return DroolsBackend.build(GOLDEN.resolve(golden).resolve("ir.json"), DroolsBackend.Branch.EXEC_MODEL)
                .execModelTranspiler().modelplan().rules().stream().filter(p -> p.form() != null).toList();
    }

    /** The tests of {@code p}, in the order of its left-hand side. */
    private static List<TestItem> tests(RulePlan p) {
        List<TestItem> out = new ArrayList<>();
        for (LhsItem item : p.leftSide()) if (item instanceof TestItem t) out.add(t);
        return out;
    }

    /** Per rule of a form, in the order of the model: its form, the role of each of its tests and the test's
     *  condition as a tree ({@link TermTree#tree}), or that it tests nothing. */
    private static List<String> trees(List<RulePlan> plans) {
        List<String> out = new ArrayList<>();
        for (RulePlan p : plans) {
            List<TestItem> tests = tests(p);
            if (tests.isEmpty()) out.add(p.form() + ": geen toets");
            for (TestItem t : tests) out.add(name(p, t) + ": " + TermTree.tree(t.test()));
        }
        return out;
    }

    /** Per test, every node of its condition with what the tree text leaves out ({@link TermTree#fields}); a test of
     *  the same form as the test before it, whose list equals that test's, is named once instead. */
    private static List<String> fields(List<RulePlan> plans) {
        List<String> out = new ArrayList<>();
        String previousForm = null;
        List<String> previous = null;
        for (RulePlan p : plans)
            for (TestItem t : tests(p)) {
                List<String> rules = new ArrayList<>();
                TermTree.fields("", t.test(), "0", rules);
                if (p.form().equals(previousForm) && rules.equals(previous))
                    out.add(name(p, t) + ": zoals de vorige toets");
                else for (String r : rules) out.add(name(p, t) + r);
                previousForm = p.form();
                previous = rules;
            }
        return out;
    }

    private static String name(RulePlan p, TestItem t) {
        return p.form() + " " + t.role() + (t.depositFlag() < 0 ? "" : " probe " + t.depositFlag());
    }
}
