package dev.uchar.regelspraak.transpiler.tms;

import dev.uchar.regelspraak.cases.DroolsBackend;
import dev.uchar.regelspraak.graaf.ProducentConsument.ElementSleutel;
import dev.uchar.regelspraak.transpiler.tms.plan.Binding;
import dev.uchar.regelspraak.transpiler.tms.plan.Gevolg;
import dev.uchar.regelspraak.transpiler.tms.plan.Join;
import dev.uchar.regelspraak.transpiler.tms.plan.Leestabel;
import dev.uchar.regelspraak.transpiler.tms.plan.Modelplan;
import dev.uchar.regelspraak.transpiler.tms.plan.Onderwerp;
import dev.uchar.regelspraak.transpiler.tms.plan.Patroon;
import dev.uchar.regelspraak.transpiler.tms.plan.Regelplan;

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
class JoinMetTweeNullsWitnessTest {

    private static final Path KUSTWACHT = Path.of("..", "..", "corpus", "golden", "anchor", "jutterswet-kustwacht",
            "ir.json");
    private static final ElementSleutel RUSTWACHT = ElementSleutel.van("wachter", "rustwacht", true);

    @Test void een_join_waarvan_beide_zijden_null_lezen_ontmoet_niets_in_beide_weergaven() throws Exception {
        DroolsBackend.Build b = DroolsBackend.build(KUSTWACHT, DroolsBackend.Branch.EXEC_MODEL);
        ClassLoader bonen = b.beanLoader();
        Class<?> wachter = bonen.loadClass("dev.uchar.regelspraak.facts.Wachter");
        Class<?> post = bonen.loadClass("dev.uchar.regelspraak.facts.Post");
        Modelplan plan = plan(wachter);
        String tekst = DrlWeergave.tekst(plan);
        ClassLoader vorige = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(bonen);
        try {
            KieBase model = KieBaseBuilder.createKieBaseFromModel(PatternDslWeergave.model(plan),
                    EqualityBehaviorOption.EQUALITY);
            KieHelper helper = new KieHelper();
            helper.setClassLoader(bonen);
            helper.addContent(tekst, DrlWeergave.PAD);
            KieBase drl = helper.build(EqualityBehaviorOption.EQUALITY);
            Object p1 = post.getConstructor().newInstance();
            List<String> uitkomst = new ArrayList<>();
            for (Object[] posten : new Object[][] { { null, null }, { p1, p1 }, { p1, null } }) {
                String invoer = (posten[0] == null ? "leeg" : "P1") + "/" + (posten[1] == null ? "leeg" : "P1");
                uitkomst.add(invoer + " model " + kenmerken(model, wachter, posten));
                uitkomst.add(invoer + " drl " + kenmerken(drl, wachter, posten));
            }
            assertEquals(List.of("leeg/leeg model []", "leeg/leeg drl []", "P1/P1 model [W1, W2]", "P1/P1 drl [W1, W2]",
                    "P1/leeg model [W1]", "P1/leeg drl [W1]"), uitkomst);
        } finally {
            Thread.currentThread().setContextClassLoader(vorige);
        }
        assertTrue(tekst.contains("post != null, post == $post"), () -> "de join toetst eerst op null:\n" + tekst);
    }

    /** One rule: the Wachter {@code $onderwerp} binds its post, and a Wachter whose post is that post gives it the
     *  characteristic rustwacht. */
    private static Modelplan plan(Class<?> wachter) {
        Onderwerp onderwerp = new Onderwerp(wachter, "$onderwerp", List.of(), List.of(),
                List.of(Binding.van("$post", wachter, "post")));
        Patroon ander = new Patroon(wachter, "$ander", List.of(), List.of(new Join("ander-post", "post", "$post")),
                List.of());
        return new Modelplan(List.of(), List.of(new Regelplan("join op de post", null, null, new Leestabel(List.of()),
                List.of(onderwerp, ander), new Gevolg.Kenmerk("$onderwerp", RUSTWACHT, Gevolg.Invoeging.LOGISCH))));
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
            List<String> uit = new ArrayList<>();
            for (int i = 0; i < wachters.size(); i++)
                for (Object f : sessie.getObjects(new ClassObjectFilter(HeeftKenmerk.class)))
                    if (((HeeftKenmerk) f).onderwerp() == wachters.get(i) && ((HeeftKenmerk) f).kenmerk().equals(RUSTWACHT))
                        uit.add("W" + (i + 1));
            return uit;
        } finally {
            sessie.dispose();
        }
    }
}
