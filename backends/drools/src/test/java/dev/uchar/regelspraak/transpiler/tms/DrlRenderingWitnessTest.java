package dev.uchar.regelspraak.transpiler.tms;

import dev.uchar.regelspraak.cases.CaseRunner;
import dev.uchar.regelspraak.cases.DroolsBackend;
import dev.uchar.regelspraak.loader.IrLoader;
import dev.uchar.regelspraak.transpiler.tms.plan.Modelplan;
import dev.uchar.regelspraak.transpiler.tms.plan.RulePlan;

import org.junit.jupiter.api.Test;
import org.kie.api.builder.Message;
import org.kie.api.builder.Results;
import org.kie.api.conf.EqualityBehaviorOption;
import org.kie.internal.utils.KieHelper;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The DRL text of the plan compiles (card drl-teksttak-op-truth-maintenance, step 2a): for every golden the exec-model
 * branch plays, that is, every golden with cases that it builds, {@link DrlRendering} renders the plan, or refuses a
 * kind of consequence it does not carry yet, by name; a text it renders compiles with {@code KieHelper} in
 * {@code EQUALITY}, under the golden's bean loader as the context class loader, as {@code DroolsBackend} builds the
 * text of the other DRL branch, without a single error. The text of no golden holds {@code import function},
 * {@code salience}, {@code no-loop}, {@code modify}, {@code update(} or {@code !.}: acceptance (c) of the card. With
 * {@code -Duchar.drlvorm=<map>} the witness writes the text of every golden to {@code <map>/<golden>.drl}, a slash in
 * the golden's name as an underscore, and the text of the plan of every form, {@link FormsPlan}, to
 * {@code <map>/_vormenplan.drl}: the measurement {@code drlvorm}, which shows a change of the rendering as a change of
 * text.
 *
 * <p><b>Red</b> before the step, on {@code a39778f1}: {@code DrlRendering} did not exist, and this class did not
 * compile.
 */
class DrlRenderingWitnessTest {

    private static final Path GOLDEN = Path.of("..", "..", "corpus", "golden");

    /** Floors, measured 2026-10-13 at step 2a: the goldens the exec-model branch plays, and those whose text the
     *  rendering gives and Drools compiles, raised at step 3a, which carries the rules of a fold, from 117 to the 165
     *  measured then, at step 3b, which carries those of a uniqueness and of the channel, to the 170 measured then, and
     *  at step 4, which carries those of a defaults stack, to the 207 measured then, and at step 5, which carries every
     *  kind of consequence, to the 250 measured then, every golden. A deliberate removal lowers a floor in the same
     *  commit. */
    private static final int EXPECTED_MIN_GOLDENS = 250;
    private static final int EXPECTED_MIN_COMPILED = 250;

    /** What the text of no golden holds. */
    private static final List<String> FORBIDDEN = List.of("import function", "salience", "no-loop", "modify",
            "update(", "!.");

    @Test void the_text_of_every_golden_compiles_or_the_rendering_refuses_a_kind() throws Exception {
        String dump = System.getProperty("uchar.drlvorm");
        Map<String, List<String>> refused = new TreeMap<>();
        List<String> faults = new ArrayList<>(), forbidden = new ArrayList<>();
        int goldens = 0, compiled = 0;
        long nanos = 0;
        for (Path g : goldens()) {
            DroolsBackend.Build b;
            try {
                b = DroolsBackend.build(g.resolve("ir.json"), DroolsBackend.Branch.EXEC_MODEL);
            } catch (IrLoader.Refusal | IrLoader.DocumentError | CaseRunner.Excluded notPlayed) {
                continue;
            }
            goldens++;
            String naam = GOLDEN.relativize(g).toString();
            String text;
            try {
                text = DrlRendering.text(b.execModelTranspiler().modelplan());
            } catch (DrlRendering.Refusal w) {
                refused.computeIfAbsent(w.category(), k -> new ArrayList<>()).add(naam);
                continue;
            }
            if (dump != null) Files.writeString(Path.of(dump).resolve(naam.replace('/', '_') + ".drl"), text);
            for (String v : FORBIDDEN) if (text.contains(v)) forbidden.add(naam + ": " + v);
            long start = System.nanoTime();
            List<String> f = compile(text, b.beanLoader());
            nanos += System.nanoTime() - start;
            if (f.isEmpty()) compiled++;
            else faults.add(naam + ": " + f.size() + " fouten, de eerste " + f.get(0));
        }
        StringBuilder overview = new StringBuilder("drlvorm: " + goldens + " goldens, " + compiled
                + " gecompileerd in " + nanos / 1_000_000 + " ms; geweigerd per soort:");
        for (Map.Entry<String, List<String>> e : refused.entrySet())
            overview.append("\n  ").append(e.getKey()).append(" ").append(e.getValue().size()).append(": ")
                    .append(e.getValue());
        System.out.println(overview);
        assertEquals(List.of(), faults, "teksten die niet compileren");
        assertEquals(List.of(), forbidden, "teksten met een verboden vorm");
        int n = goldens, c = compiled;
        assertTrue(n >= EXPECTED_MIN_GOLDENS, () -> n + " goldens, onder de vloer " + EXPECTED_MIN_GOLDENS);
        assertTrue(c >= EXPECTED_MIN_COMPILED, () -> c + " gecompileerde teksten, onder de vloer "
                + EXPECTED_MIN_COMPILED + "\n" + overview);
    }

    /** Floors, measured 2026-10-13 at step 2a: the rules of the goldens the branch plays whose consequence the rendering
     *  carries, raised at step 3a from 1221 to the 1433 measured then, at step 3b to the 1467 measured then and at step 4
     *  to the 1684 measured then, and at step 5 to the 1851 measured then, every rule of the corpus; and the goldens that
     *  have at least one, which stayed 224 until step 4 raised them to the 239 measured then, and step 5 to the 250
     *  measured then. A deliberate removal lowers a floor in the same commit. */
    private static final int EXPECTED_MIN_CARRIED_RULES = 1851;
    private static final int EXPECTED_MIN_GOLDENS_WITH_RULES = 250;

    /** The rules whose consequence the rendering carries compile in every golden, also in one whose text it refuses for
     *  the consequence of another rule: their left-hand sides and terms are those of the whole corpus but for the rules
     *  of the refused kinds. A rule's text depends on the rule alone, so the rules a golden carries make a text of
     *  their own, with the globals of the plan. */
    @Test void the_carried_rules_of_every_golden_compile() throws Exception {
        List<String> faults = new ArrayList<>();
        int rules = 0, withRules = 0, refused = 0;
        for (Path g : goldens()) {
            DroolsBackend.Build b;
            try {
                b = DroolsBackend.build(g.resolve("ir.json"), DroolsBackend.Branch.EXEC_MODEL);
            } catch (IrLoader.Refusal | IrLoader.DocumentError | CaseRunner.Excluded notPlayed) {
                continue;
            }
            Modelplan plan = b.execModelTranspiler().modelplan();
            List<RulePlan> carried = new ArrayList<>();
            for (RulePlan r : plan.rules()) {
                try {
                    DrlRendering.text(new Modelplan(plan.globals(), List.of(r)));
                    carried.add(r);
                } catch (DrlRendering.Refusal w) {
                    refused++;
                }
            }
            if (carried.isEmpty()) continue;
            withRules++;
            rules += carried.size();
            List<String> f = compile(DrlRendering.text(new Modelplan(plan.globals(), carried)), b.beanLoader());
            if (!f.isEmpty()) faults.add(GOLDEN.relativize(g) + ": " + f.size() + " fouten, de eerste " + f.get(0));
        }
        System.out.println("drlvorm, gedragen regels: " + rules + " in " + withRules + " goldens; " + refused
                + " regels geweigerd");
        assertEquals(List.of(), faults, "gedragen regels die niet compileren");
        int r = rules, m = withRules;
        assertTrue(r >= EXPECTED_MIN_CARRIED_RULES, () -> r + " gedragen regels, onder de vloer "
                + EXPECTED_MIN_CARRIED_RULES);
        assertTrue(m >= EXPECTED_MIN_GOLDENS_WITH_RULES, () -> m + " goldens met gedragen regels, onder de vloer "
                + EXPECTED_MIN_GOLDENS_WITH_RULES);
    }

    /**
     * Every form the rendering carries compiles, also where no golden has it: a plan made here, over the beans of
     * {@code anchor/jutterswet-kustwacht}, with every kind of left-hand item (a negation of two patterns, an existence,
     * an accumulate with every fold, a test in every role), every comparator of a field test, every form of reading,
     * along a path and through the declaring class, an identity join with and without its null test, a constraint
     * between two patterns, every node of a term with every kind of constant, a day kind, an agenda group, a list
     * longer than the routes take, and every consequence of this step. One plan gives one text, and a consequence of
     * another kind is refused by its name.
     */
    @Test void every_form_of_the_rendering_compiles() throws Exception {
        DroolsBackend.Build b = DroolsBackend.build(GOLDEN.resolve("anchor/jutterswet-kustwacht").resolve("ir.json"),
                DroolsBackend.Branch.EXEC_MODEL);
        Class<?> wachter = b.beanLoader().loadClass("dev.uchar.regelspraak.facts.Wachter");
        Class<?> post = b.beanLoader().loadClass("dev.uchar.regelspraak.facts.Post");
        Class<?> kustvak = b.beanLoader().loadClass("dev.uchar.regelspraak.facts.Kustvak");
        Modelplan plan = FormsPlan.plan(wachter, post, kustvak);
        String text = DrlRendering.text(plan);
        assertEquals(text, DrlRendering.text(plan), "één plan geeft één tekst");
        String dump = System.getProperty("uchar.drlvorm");
        if (dump != null) Files.writeString(Path.of(dump).resolve("_vormenplan.drl"), text);
        assertEquals(List.of(), compile(text, b.beanLoader()), () -> "de vormentekst compileert niet:\n" + text);
        for (String v : FORBIDDEN) assertTrue(!text.contains(v), () -> v + " in de vormentekst");
        for (String form : FormsPlan.FORMS)
            assertTrue(text.contains(form), () -> "de vormentekst mist " + form + ":\n" + text);
    }

    /**
     * Every form of the rules of a fold compiles (step 3a), also where no golden has it: a plan made here over the same
     * beans ({@link FormsPlan#folds}), with a contribution of every kind, the place of a member over no step and over
     * two, in the second selection, with and without a probe, the accumulates over the places through the constraint of
     * a place and of a meeting, the deposit of a filter, the fault of a filter and the fault of a member. One plan gives
     * one text, written to {@code <map>/_vouwenplan.drl} under {@code -Duchar.drlvorm}.
     *
     * <p><b>Red</b> before step 3a, on {@code 4c8a687b}: the rendering refused the plan, "de DRL-weergave draagt
     * Bijdrage nog niet".
     */
    @Test void every_form_of_a_fold_compiles() throws Exception {
        DroolsBackend.Build b = DroolsBackend.build(GOLDEN.resolve("anchor/jutterswet-kustwacht").resolve("ir.json"),
                DroolsBackend.Branch.EXEC_MODEL);
        Class<?> wachter = b.beanLoader().loadClass("dev.uchar.regelspraak.facts.Wachter");
        Class<?> post = b.beanLoader().loadClass("dev.uchar.regelspraak.facts.Post");
        Class<?> kustvak = b.beanLoader().loadClass("dev.uchar.regelspraak.facts.Kustvak");
        Modelplan plan = FormsPlan.folds(wachter, post, kustvak);
        String text = DrlRendering.text(plan);
        assertEquals(text, DrlRendering.text(plan), "één plan geeft één tekst");
        String dump = System.getProperty("uchar.drlvorm");
        if (dump != null) Files.writeString(Path.of(dump).resolve("_vouwenplan.drl"), text);
        assertEquals(List.of(), compile(text, b.beanLoader()), () -> "de vouwentekst compileert niet:\n" + text);
        for (String v : FORBIDDEN) assertTrue(!text.contains(v), () -> v + " in de vouwentekst");
        for (String form : FormsPlan.FOLD_FORMS)
            assertTrue(text.contains(form), () -> "de vouwentekst mist " + form + ":\n" + text);
    }

    /**
     * Every form of the rules of a uniqueness and of the §9.5 channel compiles (step 3b), also where no golden has it: a
     * plan made here over the same beans ({@link FormsPlan#channel}), with the keys under a form without a scope, over a
     * component that is null and one through a function, and under a form with a scope, over a component on a
     * timeline; the violation, the fault of the form of an input, and the standing of the channel, inconsistent and
     * consistent. One plan gives one text, written to {@code <map>/_kanaalplan.drl} under {@code -Duchar.drlvorm}.
     *
     * <p><b>Red</b> before step 3b, on {@code 0458b2d0}: the rendering refused the plan, "de DRL-weergave draagt
     * Uniciteitssleutel nog niet".
     */
    @Test void every_form_of_a_uniqueness_and_of_the_channel_compiles() throws Exception {
        DroolsBackend.Build b = DroolsBackend.build(GOLDEN.resolve("anchor/jutterswet-kustwacht").resolve("ir.json"),
                DroolsBackend.Branch.EXEC_MODEL);
        Class<?> wachter = b.beanLoader().loadClass("dev.uchar.regelspraak.facts.Wachter");
        Class<?> post = b.beanLoader().loadClass("dev.uchar.regelspraak.facts.Post");
        Class<?> inconsistency = b.beanLoader().loadClass("dev.uchar.regelspraak.facts.Inconsistentie");
        Modelplan plan = FormsPlan.channel(wachter, post, inconsistency);
        String text = DrlRendering.text(plan);
        assertEquals(text, DrlRendering.text(plan), "één plan geeft één tekst");
        String dump = System.getProperty("uchar.drlvorm");
        if (dump != null) Files.writeString(Path.of(dump).resolve("_kanaalplan.drl"), text);
        assertEquals(List.of(), compile(text, b.beanLoader()), () -> "de kanaaltekst compileert niet:\n" + text);
        for (String v : FORBIDDEN) assertTrue(!text.contains(v), () -> v + " in de kanaaltekst");
        for (String form : FormsPlan.CHANNEL_FORMS)
            assertTrue(text.contains(form), () -> "de kanaaltekst mist " + form + ":\n" + text);
    }

    /**
     * Every form of the rules of a defaults stack compiles (step 4), also where no golden has it: a plan made here over
     * the same beans ({@link FormsPlan#defaults}), with the decision of a default computed and collapsed with its fault,
     * the attribution of a characteristic and the decision not to attribute it, each with its control fact, the decision
     * of a reconciliation bound off an outcome, and the conflict under a constant key and under a bound one, as an
     * outcome and as the §9.5 inconsistency. One plan gives one text, written to {@code <map>/_defaultsplan.drl} under
     * {@code -Duchar.drlvorm}.
     *
     * <p><b>Red</b> before step 4, on {@code e2bd685e}: the rendering refused the plan, "de DRL-weergave draagt Afloop
     * nog niet".
     */
    @Test void every_form_of_the_defaults_compiles() throws Exception {
        DroolsBackend.Build b = DroolsBackend.build(GOLDEN.resolve("anchor/jutterswet-kustwacht").resolve("ir.json"),
                DroolsBackend.Branch.EXEC_MODEL);
        Class<?> wachter = b.beanLoader().loadClass("dev.uchar.regelspraak.facts.Wachter");
        Modelplan plan = FormsPlan.defaults(wachter);
        String text = DrlRendering.text(plan);
        assertEquals(text, DrlRendering.text(plan), "één plan geeft één tekst");
        String dump = System.getProperty("uchar.drlvorm");
        if (dump != null) Files.writeString(Path.of(dump).resolve("_defaultsplan.drl"), text);
        assertEquals(List.of(), compile(text, b.beanLoader()), () -> "de defaultstekst compileert niet:\n" + text);
        for (String v : FORBIDDEN) assertTrue(!text.contains(v), () -> v + " in de defaultstekst");
        for (String form : FormsPlan.DEFAULTS_FORMS)
            assertTrue(text.contains(form), () -> "de defaultstekst mist " + form + ":\n" + text);
    }

    /**
     * Every form of a creation and of a link compiles (step 5), also where no golden has it: a plan made here over the
     * same beans ({@link FormsPlan#creations}), with an object creation in its agenda group that sets an attribute
     * initialisation on the new bean and inserts a characteristic initialisation after it, stated as everything a
     * creation makes (round 5b; until then logically, which the plan of a creation no longer admits), one without
     * initialisations whose new object is the first end of its link, the link of the input and that of a fact creation,
     * and the fault of a second filling. One plan gives one text, written to {@code <map>/_creatieplan.drl} under
     * {@code -Duchar.drlvorm}.
     *
     * <p><b>Red</b> before step 5, on {@code b4d83a69}: the rendering refused the plan, "de DRL-weergave draagt Creatie
     * nog niet".
     */
    @Test void every_form_of_a_creation_and_a_link_compiles() throws Exception {
        DroolsBackend.Build b = DroolsBackend.build(GOLDEN.resolve("anchor/jutterswet-kustwacht").resolve("ir.json"),
                DroolsBackend.Branch.EXEC_MODEL);
        Class<?> wachter = b.beanLoader().loadClass("dev.uchar.regelspraak.facts.Wachter");
        Class<?> post = b.beanLoader().loadClass("dev.uchar.regelspraak.facts.Post");
        Modelplan plan = FormsPlan.creations(wachter, post);
        String text = DrlRendering.text(plan);
        assertEquals(text, DrlRendering.text(plan), "één plan geeft één tekst");
        String dump = System.getProperty("uchar.drlvorm");
        if (dump != null) Files.writeString(Path.of(dump).resolve("_creatieplan.drl"), text);
        assertEquals(List.of(), compile(text, b.beanLoader()), () -> "de creatietekst compileert niet:\n" + text);
        for (String v : FORBIDDEN) assertTrue(!text.contains(v), () -> v + " in de creatietekst");
        for (String form : FormsPlan.CREATION_FORMS)
            assertTrue(text.contains(form), () -> "de creatietekst mist " + form + ":\n" + text);
    }

    /**
     * Every form of an allocation compiles (step 5), also where no golden has it: a plan made here over the same beans
     * ({@link FormsPlan#allocations}), with a recipient with and without its weight, key and cap, and the distribution of
     * each method, with the decimals of its floor and a rest and without them. One plan gives one text, written to
     * {@code <map>/_verdelingsplan.drl} under {@code -Duchar.drlvorm}.
     *
     * <p><b>Red</b> before the allocations of step 5, on {@code abe891c1}: the rendering refused the plan, "de
     * DRL-weergave draagt Ontvanger nog niet".
     */
    @Test void every_form_of_an_allocation_compiles() throws Exception {
        DroolsBackend.Build b = DroolsBackend.build(GOLDEN.resolve("anchor/jutterswet-kustwacht").resolve("ir.json"),
                DroolsBackend.Branch.EXEC_MODEL);
        Class<?> wachter = b.beanLoader().loadClass("dev.uchar.regelspraak.facts.Wachter");
        Class<?> post = b.beanLoader().loadClass("dev.uchar.regelspraak.facts.Post");
        Modelplan plan = FormsPlan.allocations(wachter, post);
        String text = DrlRendering.text(plan);
        assertEquals(text, DrlRendering.text(plan), "één plan geeft één tekst");
        String dump = System.getProperty("uchar.drlvorm");
        if (dump != null) Files.writeString(Path.of(dump).resolve("_verdelingsplan.drl"), text);
        assertEquals(List.of(), compile(text, b.beanLoader()), () -> "de verdelingstekst compileert niet:\n" + text);
        for (String v : FORBIDDEN) assertTrue(!text.contains(v), () -> v + " in de verdelingstekst");
        for (String form : FormsPlan.ALLOCATION_FORMS)
            assertTrue(text.contains(form), () -> "de verdelingstekst mist " + form + ":\n" + text);
    }

    /** Every kind of consequence of the plan stands in a plan of {@link FormsPlan}, so that every kind compiles, also
     *  where no golden has it. Since step 5 the rendering carries every kind: its switch over the consequences has no
     *  default arm, so a new kind does not compile until the rendering spells it, and this witness asks for its form. */
    @Test void each_kind_of_consequence_stands_in_a_forms_plan() throws Exception {
        DroolsBackend.Build b = DroolsBackend.build(GOLDEN.resolve("anchor/jutterswet-kustwacht").resolve("ir.json"),
                DroolsBackend.Branch.EXEC_MODEL);
        Class<?> wachter = b.beanLoader().loadClass("dev.uchar.regelspraak.facts.Wachter");
        Class<?> post = b.beanLoader().loadClass("dev.uchar.regelspraak.facts.Post");
        Class<?> kustvak = b.beanLoader().loadClass("dev.uchar.regelspraak.facts.Kustvak");
        Class<?> inconsistency = b.beanLoader().loadClass("dev.uchar.regelspraak.facts.Inconsistentie");
        java.util.Set<String> kinds = new java.util.TreeSet<>();
        for (Modelplan p : List.of(FormsPlan.plan(wachter, post, kustvak), FormsPlan.folds(wachter, post, kustvak),
                FormsPlan.channel(wachter, post, inconsistency), FormsPlan.defaults(wachter),
                FormsPlan.creations(wachter, post), FormsPlan.allocations(wachter, post)))
            for (RulePlan r : p.rules()) kinds.add(r.consequence().getClass().getSimpleName());
        java.util.Set<String> all = new java.util.TreeSet<>();
        for (Class<?> k : dev.uchar.regelspraak.transpiler.tms.plan.Consequence.class.getPermittedSubclasses())
            all.add(k.getSimpleName());
        assertEquals(all, kinds);
    }

    /** The errors of compiling {@code text} with {@code KieHelper} under the bean loader {@code beans}, which is the
     *  context class loader meanwhile, as {@code DroolsBackend} has it; where there is none, the text builds a KieBase
     *  in {@code EQUALITY}. */
    static List<String> compile(String text, ClassLoader beans) {
        ClassLoader previous = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(beans);
        try {
            KieHelper helper = new KieHelper();
            helper.setClassLoader(beans);
            helper.addContent(text, DrlRendering.PATH);
            Results result = helper.verify();
            List<String> faults = new ArrayList<>();
            for (Message m : result.getMessages(Message.Level.ERROR))
                faults.add("r. " + m.getLine() + ": " + m.getText().replace('\n', ' '));
            if (faults.isEmpty()) assertNotNull(helper.build(EqualityBehaviorOption.EQUALITY));
            return faults;
        } finally {
            Thread.currentThread().setContextClassLoader(previous);
        }
    }

    /** The goldens with cases, in order: those the replay can play. */
    private static List<Path> goldens() throws Exception {
        List<Path> out = new ArrayList<>();
        try (Stream<Path> s = Files.walk(GOLDEN)) {
            for (Path ir : s.filter(p -> p.getFileName().toString().equals("ir.json")).sorted().toList())
                try (Stream<Path> cases = Files.list(ir.getParent())) {
                    if (cases.anyMatch(f -> f.getFileName().toString().matches("case.*\\.json"))) out.add(ir.getParent());
                }
        }
        return out;
    }
}
