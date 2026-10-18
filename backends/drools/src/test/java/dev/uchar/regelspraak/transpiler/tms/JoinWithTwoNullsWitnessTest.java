package dev.uchar.regelspraak.transpiler.tms;

import dev.uchar.regelspraak.cases.DroolsBackend;
import dev.uchar.regelspraak.graaf.ProducentConsument.ElementSleutel;
import dev.uchar.regelspraak.transpiler.tms.plan.Binding;
import dev.uchar.regelspraak.transpiler.tms.plan.Consequence;
import dev.uchar.regelspraak.transpiler.tms.plan.Join;
import dev.uchar.regelspraak.transpiler.tms.plan.ReadTable;
import dev.uchar.regelspraak.transpiler.tms.plan.Modelplan;
import dev.uchar.regelspraak.transpiler.tms.plan.Subject;
import dev.uchar.regelspraak.transpiler.tms.plan.FactPattern;
import dev.uchar.regelspraak.transpiler.tms.plan.RulePlan;

import org.drools.modelcompiler.KieBaseBuilder;
import org.junit.jupiter.api.Test;
import org.kie.api.KieBase;
import org.kie.api.conf.EqualityBehaviorOption;
import org.kie.api.runtime.ClassObjectFilter;
import org.kie.api.runtime.KieSession;
import org.kie.internal.utils.KieHelper;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * An identity join of which both sides read null meets nothing in either rendering (decision 23; card
 * drl-teksttak-op-truth-maintenance, step 2b): the executable model joins through {@code zelfde}, which meets nothing
 * where a side reads null, and DRL finds two nulls equal, so the DRL rendering tests the pattern's field for null first
 * where the other side can read null. No form the branch carries at this step lets both sides read null: the one join
 * with that test in the corpus is that of a path standing, whose other side the same pattern tests for a link first.
 * So the witness fires a plan of its own, over the beans of {@code anchor/jutterswet-kustwacht}: one rule that gives a
 * Wachter a characteristic where some Wachter has its post ({@code $post : post}, then a Wachter with
 * {@code post == $post}), through the executable model, as the exec-model branch builds it, and through the DRL text, as
 * {@code DRL_TEXT} builds it. Three inputs: two Wachters without a post, where both sides of every join read null and
 * neither rendering gives a characteristic; two with one post, which both get it; and one with a post beside one
 * without, of which only the first gets it.
 *
 * <p><b>Red</b> without the null test (the DRL rendering of decision 23 less its first constraint, measured on a copy of
 * {@code 582a1d7f}): the DRL text gives both Wachters without a post the characteristic, and in the third input the
 * Wachter without one as well, where the executable model gives none.
 */
class JoinWithTwoNullsWitnessTest {

    private static final Path KUSTWACHT = Path.of("..", "..", "corpus", "golden", "anchor", "jutterswet-kustwacht",
            "ir.json");
    private static final ElementSleutel RUSTWACHT = ElementSleutel.van("wachter", "rustwacht", true);

    @Test void a_join_whose_both_sides_read_null_meets_nothing_in_both_renderings() throws Exception {
        DroolsBackend.Build b = DroolsBackend.build(KUSTWACHT, DroolsBackend.Branch.EXEC_MODEL);
        ClassLoader beans = b.beanLoader();
        Class<?> wachter = beans.loadClass("dev.uchar.regelspraak.facts.Wachter");
        Class<?> post = beans.loadClass("dev.uchar.regelspraak.facts.Post");
        Modelplan plan = plan(wachter);
        String text = DrlRendering.text(plan);
        ClassLoader previous = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(beans);
        try {
            KieBase model = KieBaseBuilder.createKieBaseFromModel(PatternDslRendering.model(plan),
                    EqualityBehaviorOption.EQUALITY);
            KieHelper helper = new KieHelper();
            helper.setClassLoader(beans);
            helper.addContent(text, DrlRendering.PATH);
            KieBase drl = helper.build(EqualityBehaviorOption.EQUALITY);
            Object p1 = post.getConstructor().newInstance();
            List<String> outcome = new ArrayList<>();
            for (Object[] posten : new Object[][] { { null, null }, { p1, p1 }, { p1, null } }) {
                String invoer = (posten[0] == null ? "leeg" : "P1") + "/" + (posten[1] == null ? "leeg" : "P1");
                outcome.add(invoer + " model " + kenmerken(model, wachter, posten));
                outcome.add(invoer + " drl " + kenmerken(drl, wachter, posten));
            }
            assertEquals(List.of("leeg/leeg model []", "leeg/leeg drl []", "P1/P1 model [W1, W2]", "P1/P1 drl [W1, W2]",
                    "P1/leeg model [W1]", "P1/leeg drl [W1]"), outcome);
        } finally {
            Thread.currentThread().setContextClassLoader(previous);
        }
        assertTrue(text.contains("post != null, post == $post"), () -> "de join toetst eerst op null:\n" + text);
    }

    /** One rule: the Wachter {@code $onderwerp} binds its post, and a Wachter whose post is that post gives it the
     *  characteristic rustwacht. */
    private static Modelplan plan(Class<?> wachter) {
        Subject onderwerp = new Subject(wachter, "$onderwerp", List.of(), List.of(),
                List.of(Binding.van("$post", wachter, "post")));
        FactPattern other = new FactPattern(wachter, "$ander", List.of(), List.of(new Join("ander-post", "post", "$post")),
                List.of());
        return new Modelplan(List.of(), List.of(new RulePlan("join op de post", null, null, new ReadTable(List.of()),
                List.of(onderwerp, other), new Consequence.Characteristic("$onderwerp", RUSTWACHT, Consequence.Insertion.LOGICAL))));
    }

    /** The labels of the Wachters W1 and W2, with the posts {@code posten}, that have the characteristic at the fixpoint
     *  of a session of {@code kb}. */
    private static List<String> kenmerken(KieBase kb, Class<?> wachter, Object[] posten) throws Exception {
        KieSession sessie = kb.newKieSession();
        try {
            List<Object> wachters = new ArrayList<>();
            for (Object p : posten) {
                Object w = wachter.getConstructor().newInstance();
                if (p != null) wachter.getMethod("setPost", p.getClass()).invoke(w, p);
                wachters.add(w);
                sessie.insert(w);
            }
            sessie.fireAllRules();
            List<String> out = new ArrayList<>();
            for (int i = 0; i < wachters.size(); i++)
                for (Object f : sessie.getObjects(new ClassObjectFilter(HasCharacteristic.class)))
                    if (((HasCharacteristic) f).onderwerp() == wachters.get(i) && ((HasCharacteristic) f).kenmerk().equals(RUSTWACHT))
                        out.add("W" + (i + 1));
            return out;
        } finally {
            sessie.dispose();
        }
    }
}
