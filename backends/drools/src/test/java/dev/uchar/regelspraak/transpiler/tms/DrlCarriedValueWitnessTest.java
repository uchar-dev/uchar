package dev.uchar.regelspraak.transpiler.tms;

import dev.uchar.regelspraak.cases.DroolsBackend;
import dev.uchar.regelspraak.graph.ProducerConsumer.ElementKey;
import dev.uchar.regelspraak.runtime.Breuk;
import dev.uchar.regelspraak.transpiler.tms.plan.Binding;
import dev.uchar.regelspraak.transpiler.tms.plan.Consequence;
import dev.uchar.regelspraak.transpiler.tms.plan.Modelplan;
import dev.uchar.regelspraak.transpiler.tms.plan.Read;
import dev.uchar.regelspraak.transpiler.tms.plan.ReadKind;
import dev.uchar.regelspraak.transpiler.tms.plan.ReadTable;
import dev.uchar.regelspraak.transpiler.tms.plan.RulePlan;
import dev.uchar.regelspraak.transpiler.tms.plan.Subject;
import dev.uchar.regelspraak.transpiler.tms.plan.Term;

import org.drools.modelcompiler.KieBaseBuilder;
import org.junit.jupiter.api.Test;
import org.kie.api.KieBase;
import org.kie.api.conf.EqualityBehaviorOption;
import org.kie.api.runtime.KieSession;
import org.kie.internal.utils.KieHelper;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The value of a decision and of a value consequence that computes nothing, a constant or the variable a binding binds,
 * with or without {@code collapse}, is carried by the consequence of the DRL text, without a function (card
 * drl-no-trivial-value-functions, acceptance (b)), and means there what it means in the executable model. A plan made
 * here over the Wachter of {@code anchor/jutterswet-kustwacht}: per constant of {@link #CONSTANTS}, every class a
 * constant of the plan has (the texts with what Drools's preparation of a consequence and MVEL read otherwise: a
 * backslash at the end, U+0000 beside an escape, a backslash before {@code u}, the marks of a comment), a value
 * consequence with and without {@code collapse} and a decision with and without both {@code collapse} and its fault in
 * the plan; the bound uitkijkbonus in each of those forms; and a value consequence with a probe in the plan over a
 * constant and over the bound value. Every term carries {@link #SENTENCE}, which stands as a comment line above the
 * statement that carries it, where Drools reads past it. The plan fires through the executable model, as the exec-model
 * branch builds it, and through the DRL text, as {@code DRL_TEXT} builds it, on a Wachter with an uitkijkbonus and on
 * one without: both give the same facts, and those the plan says, a value of the same class equal to the constant,
 * collapsed where the plan collapses it.
 *
 * <p><b>Red</b> on {@code 4707a183}, before the card: the text declares a function per value ({@code waarde_…} and
 * {@code afloop_…}), and its consequences ask an evaluation fault of each.
 */
class DrlCarriedValueWitnessTest {

    private static final Path KUSTWACHT = Path.of("..", "..", "corpus", "golden", "anchor", "jutterswet-kustwacht",
            "ir.json");
    private static final String O = "$onderwerp";
    private static final ElementKey TARGET = ElementKey.of("wachter", "uitkijkbonus", false);

    /** A constant of every class the plan holds: those of the values of a document, and those a function takes. */
    static final List<Object> CONSTANTS = Arrays.asList(null, "tekst", "eindigt op een backslash \\", "\\", "n\0\"z",
            "\0", "a\0\\", "\\u0041 en \\n", "h//x #y /*z*/", "regel\nend\r", "€ë日𝔘", "", Boolean.TRUE, Boolean.FALSE, 5,
            5L, new BigDecimal("1.50"), new BigInteger("12345678901234567890"),
            Breuk.van(BigInteger.ONE, BigInteger.valueOf(3)), LocalDate.parse("2024-02-29"),
            LocalDateTime.parse("2024-02-29T10:15:30"), Outcome.Kind.DECIDED, RoundingMode.HALF_UP, Period.class,
            List.of(), Arrays.asList("a\\", null, Boolean.TRUE, new BigDecimal("2")));

    /** The sentence of every term: the marks that open a string, a block, a comment or an escape, and a line break. */
    static final String SENTENCE = "zin met 'apostrof', \"aanhalingsteken\", {accolade, (haakje, [blok, \\u005c,"
            + " \\u12, end, then, #, // en /* en */\nnog een regel";

    @Test void a_value_that_computes_nothing_has_no_function_and_means_what_the_executable_model_means()
            throws Exception {
        DroolsBackend.Build b = DroolsBackend.build(KUSTWACHT, DroolsBackend.Branch.EXEC_MODEL);
        ClassLoader beans = b.beanLoader();
        Class<?> wachter = beans.loadClass("dev.uchar.regelspraak.facts.Wachter");
        List<String> expectedWith = new ArrayList<>(), expectedWithout = new ArrayList<>();
        Modelplan plan = plan(wachter, expectedWith, expectedWithout);
        String text = DrlRendering.text(plan);
        // no function and no question after an evaluation fault: the consequence carries every value
        assertFalse(text.contains("function "), text);
        assertFalse(text.contains("isEvaluationFault"), text);
        String comment = "    // zin met 'apostrof', \"aanhalingsteken\", {accolade, (haakje, [blok, \\u005c, \\u12,"
                + " end, then, #, // en /* en * / nog een regel\n    Object v = ";
        assertEquals(plan.rules().size(), text.split(java.util.regex.Pattern.quote(comment), -1).length - 1, text);
        ClassLoader previous = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(beans);
        try {
            KieBase model = KieBaseBuilder.createKieBaseFromModel(PatternDslRendering.model(plan),
                    EqualityBehaviorOption.EQUALITY);
            KieHelper helper = new KieHelper();
            helper.setClassLoader(beans);
            helper.addContent(text, DrlRendering.PATH);
            KieBase drl = helper.build(EqualityBehaviorOption.EQUALITY);
            for (BigDecimal bonus : Arrays.asList(new BigDecimal("7.25"), null)) {
                List<String> expected = bonus == null ? expectedWithout : expectedWith;
                assertEquals(expected, facts(model, wachter, bonus), "exec-model, uitkijkbonus " + bonus);
                assertEquals(expected, facts(drl, wachter, bonus), "drl-tekst, uitkijkbonus " + bonus);
            }
        } finally {
            Thread.currentThread().setContextClassLoader(previous);
        }
    }

    /**
     * The plan, and per uitkijkbonus of the Wachter, set ({@code with}) and not ({@code without}), the facts it gives
     * as {@link #facts} writes them: a value consequence writes leeg as well, and a decision decides nothing over
     * null.
     */
    private static Modelplan plan(Class<?> wachter, List<String> with, List<String> without) {
        List<RulePlan> rules = new ArrayList<>();
        Subject subject = new Subject(wachter, O, List.of(), List.of(), List.of());
        for (int i = 0; i < CONSTANTS.size(); i++) {
            Object w = CONSTANTS.get(i);
            for (boolean collapse : new boolean[] {false, true}) {
                Object v = collapse ? ComputeHelpers.collapse(w) : w;
                Term t = new Term.Const(w, null, SENTENCE);
                String value = "waarde " + i + (collapse ? " ingeklapt" : "");
                rules.add(new RulePlan(value, "proef", null, new ReadTable(List.of()), List.of(subject),
                        new Consequence.Value(O, TARGET, value, t, List.of(), 0, collapse, true, null,
                                new Consequence.FaultName("proef", value), Consequence.Insertion.LOGICAL)));
                expect(with, without, "Value " + value + " = " + describe(v));
                for (boolean fault : new boolean[] {false, true}) {
                    String decision = "afloop " + i + (collapse ? " ingeklapt" : "") + (fault ? " met fout" : "");
                    rules.add(new RulePlan(decision, "proef", null, new ReadTable(List.of()), List.of(subject),
                            decision(t, List.of(), 0, collapse, fault, decision)));
                    if (v != null) {
                        expect(with, without, "Value " + decision + " = " + describe(v));
                        expect(with, without, "Decides " + decision + " 0");
                    }
                }
            }
        }
        Subject withBonus = new Subject(wachter, O, List.of(), List.of(),
                List.of(Binding.of("$uitkijkbonus", wachter, "uitkijkbonus")));
        ReadTable bonus = new ReadTable(List.of(new Read(0, ReadKind.INPUT, "uitkijkbonus", "$uitkijkbonus")));
        Term bound = new Term.SlotValue(0, SENTENCE);
        for (boolean collapse : new boolean[] {false, true}) {
            String value = "gebonden waarde" + (collapse ? " ingeklapt" : "");
            rules.add(new RulePlan(value, "proef", null, bonus, List.of(withBonus), new Consequence.Value(O, TARGET,
                    value, bound, List.of(0), 1, collapse, true, null, new Consequence.FaultName("proef", value),
                    Consequence.Insertion.LOGICAL)));
            with.add("Value " + value + " = BigDecimal 7.25");
            without.add("Value " + value + " = null");
            for (boolean fault : new boolean[] {false, true}) {
                String decision = "gebonden afloop" + (collapse ? " ingeklapt" : "") + (fault ? " met fout" : "");
                rules.add(new RulePlan(decision, "proef", null, bonus, List.of(withBonus),
                        decision(bound, List.of(0), 1, collapse, fault, decision)));
                with.add("Value " + decision + " = BigDecimal 7.25");
                with.add("Decides " + decision + " 0");
            }
        }
        // a probe in the plan, which nothing the value evaluates reports to
        ReadTable probe = new ReadTable(List.of(new Read(0, ReadKind.INPUT, "uitkijkbonus", "$uitkijkbonus"),
                new Read(1, ReadKind.DEPOSIT_FLAG, null, null)));
        for (Term t : List.of(new Term.Const(new BigDecimal("3"), null, SENTENCE), bound)) {
            String value = "waarde met sonde " + (t == bound ? "gebonden" : "constant");
            rules.add(new RulePlan(value, "proef", null, probe, List.of(withBonus), new Consequence.Value(O, TARGET, value,
                    t, List.of(0), 2, true, true, new Consequence.Deposit(1, "proef"),
                    new Consequence.FaultName("proef", value), Consequence.Insertion.LOGICAL)));
            with.add("Value " + value + " = BigDecimal " + (t == bound ? "7.25" : "3"));
            without.add("Value " + value + " = " + (t == bound ? "null" : "BigDecimal 3"));
        }
        with.sort(null);
        without.sort(null);
        return new Modelplan(List.of(), rules);
    }

    private static void expect(List<String> with, List<String> without, String fact) {
        with.add(fact);
        without.add(fact);
    }

    /** A decision of {@code t} named {@code name}: its value, and its control fact in a stack of its own. */
    private static Consequence.Decision decision(Term t, List<Integer> slots, int size, boolean collapse, boolean fault,
                                                 String name) {
        return new Consequence.Decision(O, t, slots, size, collapse,
                fault ? new Consequence.FaultName("proef", name) : null,
                List.of(new Consequence.StackFact.Value(TARGET, name), new Consequence.StackFact.Decides(name, 0)),
                Consequence.Insertion.LOGICAL);
    }

    /** The values and control facts a session of {@code kb} gives a Wachter with uitkijkbonus {@code bonus}, sorted:
     *  {@code Value <writer> = <value>} and {@code Decides <stack> <index>}; any other fact fails. */
    private static List<String> facts(KieBase kb, Class<?> wachter, BigDecimal bonus) throws Exception {
        KieSession session = kb.newKieSession();
        try {
            Object w = wachter.getConstructor().newInstance();
            wachter.getMethod("setUitkijkbonus", BigDecimal.class).invoke(w, bonus);
            session.insert(w);
            session.fireAllRules();
            List<String> out = new ArrayList<>();
            for (Object f : session.getObjects()) {
                if (f == w) continue;
                if (f instanceof Value v) {
                    assertTrue(v.subject() == w && v.attribute().equals(TARGET), v::toString);
                    out.add("Value " + v.writer() + " = " + describe(v.value()));
                } else if (f instanceof Decides d) {
                    assertTrue(d.subject() == w, d::toString);
                    out.add("Decides " + d.stack() + " " + d.index());
                } else {
                    out.add("ander feit " + f);
                }
            }
            out.sort(null);
            return out;
        } finally {
            session.dispose();
        }
    }

    /** {@code v} by its class and its value, every character outside printable ASCII as its code point, a list by its
     *  elements. */
    private static String describe(Object v) {
        if (v == null) return "null";
        if (v instanceof List<?> l) {
            List<String> parts = new ArrayList<>();
            for (Object x : l) parts.add(describe(x));
            return "List " + parts;
        }
        StringBuilder out = new StringBuilder(v.getClass().getSimpleName()).append(' ');
        String s = v instanceof Class<?> k ? k.getName() : v.toString();
        s.codePoints().forEach(c -> out.append(c >= 32 && c < 127 ? Character.toString(c) : String.format("<U+%04X>", c)));
        return out.toString();
    }
}
