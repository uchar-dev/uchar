package dev.uchar.regelspraak.transpiler.tms;

import dev.uchar.regelspraak.cases.DroolsBackend;
import dev.uchar.regelspraak.transpiler.core.Namen;
import dev.uchar.regelspraak.transpiler.tms.plan.Modelplan;
import dev.uchar.regelspraak.transpiler.tms.plan.RulePlan;

import org.junit.jupiter.api.Test;
import org.kie.api.KieBase;
import org.kie.api.builder.Message;
import org.kie.api.builder.Results;
import org.kie.api.conf.EqualityBehaviorOption;
import org.kie.api.definition.rule.Rule;
import org.kie.internal.utils.KieHelper;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The name of a rule in the KieBase the DRL text builds is the name of its plan (card drl-teksttak-op-truth-maintenance,
 * fix round 2a, point 1). {@code TmsTranspiler.formOf}, the scenario report of {@code CaseRunner} and the creation
 * driver look a fired rule up by the name of its plan, so a header that alters the name loses the rule. The header
 * spells the name as a string literal ({@code Namen.drlString}), which the parser of Drools 8.44 reads back as the name
 * for every character that literal escapes; two plans of one name the rendering refuses, since a KieBase holds one rule
 * per name.
 *
 * <p><b>Red</b> on {@code 175d5e7a}, where the header went through {@code Namen.escapeRuleName}: the rules read
 * {@code a/b}, {@code a/}, {@code a'b}, {@code a b} and {@code a  b} where the plans say {@code a\b}, {@code a\},
 * {@code a"b}, {@code a\nb} and {@code a\r\nb}, so that {@code a"b} and {@code a'b} share a name; and two plans of one
 * name reached the compiler, which reported "Duplicate rule name: a".
 */
class RuleNameWitnessTest {

    private static final Path IR = Path.of("..", "..", "corpus", "golden", "jutterswet-jaarvaststelling", "ir.json");

    /** Names with each character the string literal escapes, one ending in its escape character, the characters the
     *  former header substituted them by, an escape spelled out in the name, and a letter outside ASCII. */
    private static final List<String> NAMES = List.of("a\\b", "a\\", "a\"b", "a'b", "a\nb", "a\r\nb", "a\tb", "a/b",
            "a\\u0041", "İş");

    @Test void the_name_in_the_kiebase_is_that_of_the_plan() throws Exception {
        DroolsBackend.Build b = DroolsBackend.build(IR, DroolsBackend.Branch.EXEC_MODEL);
        Modelplan plan = b.execModelTranspiler().modelplan();
        // the second rule names itself in its consequence as well, as the writer of its value
        RulePlan regel = plan.rules().get(1);
        List<String> differences = new ArrayList<>();
        for (String naam : NAMES) {
            String text = DrlRendering.text(new Modelplan(plan.globals(), List.of(renamed(regel, naam))));
            List<String> read = new ArrayList<>();
            for (Rule r : build(text, b.beanLoader()).getKiePackage(TmsTranspiler.PKG).getRules())
                read.add(Namen.drlString(r.getName()));
            if (!read.equals(List.of(Namen.drlString(naam)))) differences.add(Namen.drlString(naam) + " -> " + read);
        }
        assertEquals(List.of(), differences, "regels die in de KieBase een andere naam hebben dan in het plan");
    }

    /** Two plans of one name: a KieBase holds one rule per name, so the rendering refuses them. */
    @Test void the_rendering_refuses_two_rules_with_a_name() throws Exception {
        DroolsBackend.Build b = DroolsBackend.build(IR, DroolsBackend.Branch.EXEC_MODEL);
        Modelplan plan = b.execModelTranspiler().modelplan();
        Modelplan two = new Modelplan(plan.globals(), List.of(renamed(plan.rules().get(0), "a"),
                renamed(plan.rules().get(1), "a")));
        String text;
        try {
            text = DrlRendering.text(two);
        } catch (IllegalStateException e) {
            assertEquals("twee regels van het plan heten \"a\", en een KieBase houdt één regel per naam", e.getMessage());
            return;
        }
        KieHelper helper = new KieHelper();
        helper.setClassLoader(b.beanLoader());
        helper.addContent(text, DrlRendering.PATH);
        ClassLoader previous = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(b.beanLoader());
        List<String> meldingen = new ArrayList<>();
        try {
            for (Message m : helper.verify().getMessages()) meldingen.add(m.getLevel() + " " + m.getText());
            if (meldingen.isEmpty()) for (Rule r : helper.build().getKiePackage(TmsTranspiler.PKG).getRules())
                meldingen.add("regel " + r.getName());
        } finally {
            Thread.currentThread().setContextClassLoader(previous);
        }
        throw new AssertionError("de weergave weigert twee regels met een naam niet; Drools meldt " + meldingen);
    }

    private static RulePlan renamed(RulePlan p, String naam) {
        return new RulePlan(naam, p.form(), p.agendaGroup(), p.readings(), p.leftSide(), p.consequence());
    }

    /** The KieBase of {@code text}, built as {@code DrlRenderingWitnessTest.compile} compiles it. */
    static KieBase build(String text, ClassLoader beans) {
        ClassLoader previous = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(beans);
        try {
            KieHelper helper = new KieHelper();
            helper.setClassLoader(beans);
            helper.addContent(text, DrlRendering.PATH);
            Results result = helper.verify();
            List<String> faults = new ArrayList<>();
            for (Message m : result.getMessages(Message.Level.ERROR)) faults.add(m.getText());
            assertEquals(List.of(), faults, () -> "de tekst compileert niet:\n" + text);
            return helper.build(EqualityBehaviorOption.EQUALITY);
        } finally {
            Thread.currentThread().setContextClassLoader(previous);
        }
    }
}
