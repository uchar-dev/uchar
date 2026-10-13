package dev.uchar.regelspraak.transpiler.tms;

import dev.uchar.regelspraak.cases.CaseRunner;
import dev.uchar.regelspraak.cases.DroolsBackend;
import dev.uchar.regelspraak.loader.IrLoader;
import dev.uchar.regelspraak.transpiler.tms.plan.Modelplan;
import dev.uchar.regelspraak.transpiler.tms.plan.Regelplan;

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
 * branch plays, that is, every golden with cases that it builds, {@link DrlWeergave} renders the plan, or refuses a
 * kind of consequence it does not carry yet, by name; a text it renders compiles with {@code KieHelper} in
 * {@code EQUALITY}, under the golden's bean loader as the context class loader, as {@code DroolsBackend} builds the
 * text of the other DRL branch, without a single error. The text of no golden holds {@code import function},
 * {@code salience}, {@code no-loop}, {@code modify}, {@code update(} or {@code !.}: acceptance (c) of the card. With
 * {@code -Duchar.drlvorm=<map>} the witness writes the text of every golden to {@code <map>/<golden>.drl}, a slash in
 * the golden's name as an underscore, and the text of the plan of every form, {@link Vormenplan}, to
 * {@code <map>/_vormenplan.drl}: the measurement {@code drlvorm}, which shows a change of the rendering as a change of
 * text.
 *
 * <p><b>Red</b> before the step, on {@code a39778f1}: {@code DrlWeergave} did not exist, and this class did not
 * compile.
 */
class DrlWeergaveWitnessTest {

    private static final Path GOLDEN = Path.of("..", "..", "corpus", "golden");

    /** Floors, measured 2026-10-13 at step 2a: the goldens the exec-model branch plays, and those whose text the
     *  rendering gives and Drools compiles, raised at step 3a, which carries the rules of a fold, from 117 to the 165
     *  measured then, at step 3b, which carries those of a uniqueness and of the channel, to the 170 measured then, and
     *  at step 4, which carries those of a defaults stack, to the 207 measured then. A deliberate removal lowers a floor
     *  in the same commit. */
    private static final int EXPECTED_MIN_GOLDENS = 250;
    private static final int EXPECTED_MIN_GECOMPILEERD = 207;

    /** What the text of no golden holds. */
    private static final List<String> VERBODEN = List.of("import function", "salience", "no-loop", "modify",
            "update(", "!.");

    @Test void de_tekst_van_elke_golden_compileert_of_de_weergave_weigert_een_soort() throws Exception {
        String dump = System.getProperty("uchar.drlvorm");
        Map<String, List<String>> geweigerd = new TreeMap<>();
        List<String> fouten = new ArrayList<>(), verboden = new ArrayList<>();
        int goldens = 0, gecompileerd = 0;
        long nanos = 0;
        for (Path g : goldens()) {
            DroolsBackend.Build b;
            try {
                b = DroolsBackend.build(g.resolve("ir.json"), DroolsBackend.Branch.EXEC_MODEL);
            } catch (IrLoader.Refusal | IrLoader.Documentfout | CaseRunner.Excluded nietGespeeld) {
                continue;
            }
            goldens++;
            String naam = GOLDEN.relativize(g).toString();
            String tekst;
            try {
                tekst = DrlWeergave.tekst(b.execModelTranspiler().modelplan());
            } catch (DrlWeergave.Weigering w) {
                geweigerd.computeIfAbsent(w.soort(), k -> new ArrayList<>()).add(naam);
                continue;
            }
            if (dump != null) Files.writeString(Path.of(dump).resolve(naam.replace('/', '_') + ".drl"), tekst);
            for (String v : VERBODEN) if (tekst.contains(v)) verboden.add(naam + ": " + v);
            long start = System.nanoTime();
            List<String> f = compileer(tekst, b.beanLoader());
            nanos += System.nanoTime() - start;
            if (f.isEmpty()) gecompileerd++;
            else fouten.add(naam + ": " + f.size() + " fouten, de eerste " + f.get(0));
        }
        StringBuilder overzicht = new StringBuilder("drlvorm: " + goldens + " goldens, " + gecompileerd
                + " gecompileerd in " + nanos / 1_000_000 + " ms; geweigerd per soort:");
        for (Map.Entry<String, List<String>> e : geweigerd.entrySet())
            overzicht.append("\n  ").append(e.getKey()).append(" ").append(e.getValue().size()).append(": ")
                    .append(e.getValue());
        System.out.println(overzicht);
        assertEquals(List.of(), fouten, "teksten die niet compileren");
        assertEquals(List.of(), verboden, "teksten met een verboden vorm");
        int n = goldens, c = gecompileerd;
        assertTrue(n >= EXPECTED_MIN_GOLDENS, () -> n + " goldens, onder de vloer " + EXPECTED_MIN_GOLDENS);
        assertTrue(c >= EXPECTED_MIN_GECOMPILEERD, () -> c + " gecompileerde teksten, onder de vloer "
                + EXPECTED_MIN_GECOMPILEERD + "\n" + overzicht);
    }

    /** Floors, measured 2026-10-13 at step 2a: the rules of the goldens the branch plays whose consequence the rendering
     *  carries, raised at step 3a from 1221 to the 1433 measured then, at step 3b to the 1467 measured then and at step 4
     *  to the 1684 measured then, and the goldens that have at least one, which stayed 224 until step 4 raised them to
     *  the 239 measured then. A deliberate removal lowers a floor in the same commit. */
    private static final int EXPECTED_MIN_GEDRAGEN_REGELS = 1684;
    private static final int EXPECTED_MIN_GOLDENS_MET_REGELS = 239;

    /** The rules whose consequence the rendering carries compile in every golden, also in one whose text it refuses for
     *  the consequence of another rule: their left-hand sides and terms are those of the whole corpus but for the rules
     *  of the refused kinds. A rule's text depends on the rule alone, so the rules a golden carries make a text of
     *  their own, with the globals of the plan. */
    @Test void de_gedragen_regels_van_elke_golden_compileren() throws Exception {
        List<String> fouten = new ArrayList<>();
        int regels = 0, metRegels = 0, geweigerd = 0;
        for (Path g : goldens()) {
            DroolsBackend.Build b;
            try {
                b = DroolsBackend.build(g.resolve("ir.json"), DroolsBackend.Branch.EXEC_MODEL);
            } catch (IrLoader.Refusal | IrLoader.Documentfout | CaseRunner.Excluded nietGespeeld) {
                continue;
            }
            Modelplan plan = b.execModelTranspiler().modelplan();
            List<Regelplan> gedragen = new ArrayList<>();
            for (Regelplan r : plan.regels()) {
                try {
                    DrlWeergave.tekst(new Modelplan(plan.globals(), List.of(r)));
                    gedragen.add(r);
                } catch (DrlWeergave.Weigering w) {
                    geweigerd++;
                }
            }
            if (gedragen.isEmpty()) continue;
            metRegels++;
            regels += gedragen.size();
            List<String> f = compileer(DrlWeergave.tekst(new Modelplan(plan.globals(), gedragen)), b.beanLoader());
            if (!f.isEmpty()) fouten.add(GOLDEN.relativize(g) + ": " + f.size() + " fouten, de eerste " + f.get(0));
        }
        System.out.println("drlvorm, gedragen regels: " + regels + " in " + metRegels + " goldens; " + geweigerd
                + " regels geweigerd");
        assertEquals(List.of(), fouten, "gedragen regels die niet compileren");
        int r = regels, m = metRegels;
        assertTrue(r >= EXPECTED_MIN_GEDRAGEN_REGELS, () -> r + " gedragen regels, onder de vloer "
                + EXPECTED_MIN_GEDRAGEN_REGELS);
        assertTrue(m >= EXPECTED_MIN_GOLDENS_MET_REGELS, () -> m + " goldens met gedragen regels, onder de vloer "
                + EXPECTED_MIN_GOLDENS_MET_REGELS);
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
    @Test void elke_vorm_van_de_weergave_compileert() throws Exception {
        DroolsBackend.Build b = DroolsBackend.build(GOLDEN.resolve("anchor/jutterswet-kustwacht").resolve("ir.json"),
                DroolsBackend.Branch.EXEC_MODEL);
        Class<?> wachter = b.beanLoader().loadClass("dev.uchar.regelspraak.facts.Wachter");
        Class<?> post = b.beanLoader().loadClass("dev.uchar.regelspraak.facts.Post");
        Class<?> kustvak = b.beanLoader().loadClass("dev.uchar.regelspraak.facts.Kustvak");
        Modelplan plan = Vormenplan.plan(wachter, post, kustvak);
        String tekst = DrlWeergave.tekst(plan);
        assertEquals(tekst, DrlWeergave.tekst(plan), "één plan geeft één tekst");
        String dump = System.getProperty("uchar.drlvorm");
        if (dump != null) Files.writeString(Path.of(dump).resolve("_vormenplan.drl"), tekst);
        assertEquals(List.of(), compileer(tekst, b.beanLoader()), () -> "de vormentekst compileert niet:\n" + tekst);
        for (String v : VERBODEN) assertTrue(!tekst.contains(v), () -> v + " in de vormentekst");
        for (String vorm : Vormenplan.VORMEN)
            assertTrue(tekst.contains(vorm), () -> "de vormentekst mist " + vorm + ":\n" + tekst);
    }

    /**
     * Every form of the rules of a fold compiles (step 3a), also where no golden has it: a plan made here over the same
     * beans ({@link Vormenplan#vouwen}), with a contribution of every kind, the place of a member over no step and over
     * two, in the second selection, with and without a probe, the accumulates over the places through the constraint of
     * a place and of a meeting, the deposit of a filter, the fault of a filter and the fault of a member. One plan gives
     * one text, written to {@code <map>/_vouwenplan.drl} under {@code -Duchar.drlvorm}.
     *
     * <p><b>Red</b> before step 3a, on {@code 4c8a687b}: the rendering refused the plan, "de DRL-weergave draagt
     * Bijdrage nog niet".
     */
    @Test void elke_vorm_van_een_vouw_compileert() throws Exception {
        DroolsBackend.Build b = DroolsBackend.build(GOLDEN.resolve("anchor/jutterswet-kustwacht").resolve("ir.json"),
                DroolsBackend.Branch.EXEC_MODEL);
        Class<?> wachter = b.beanLoader().loadClass("dev.uchar.regelspraak.facts.Wachter");
        Class<?> post = b.beanLoader().loadClass("dev.uchar.regelspraak.facts.Post");
        Class<?> kustvak = b.beanLoader().loadClass("dev.uchar.regelspraak.facts.Kustvak");
        Modelplan plan = Vormenplan.vouwen(wachter, post, kustvak);
        String tekst = DrlWeergave.tekst(plan);
        assertEquals(tekst, DrlWeergave.tekst(plan), "één plan geeft één tekst");
        String dump = System.getProperty("uchar.drlvorm");
        if (dump != null) Files.writeString(Path.of(dump).resolve("_vouwenplan.drl"), tekst);
        assertEquals(List.of(), compileer(tekst, b.beanLoader()), () -> "de vouwentekst compileert niet:\n" + tekst);
        for (String v : VERBODEN) assertTrue(!tekst.contains(v), () -> v + " in de vouwentekst");
        for (String vorm : Vormenplan.VOUWVORMEN)
            assertTrue(tekst.contains(vorm), () -> "de vouwentekst mist " + vorm + ":\n" + tekst);
    }

    /**
     * Every form of the rules of a uniqueness and of the §9.5 channel compiles (step 3b), also where no golden has it: a
     * plan made here over the same beans ({@link Vormenplan#kanaal}), with the keys under a form without a scope, over a
     * component that is null and one through a function, and under a form with a scope, over a component on a
     * timeline; the violation, the fault of the form of an input, and the standing of the channel, inconsistent and
     * consistent. One plan gives one text, written to {@code <map>/_kanaalplan.drl} under {@code -Duchar.drlvorm}.
     *
     * <p><b>Red</b> before step 3b, on {@code 0458b2d0}: the rendering refused the plan, "de DRL-weergave draagt
     * Uniciteitssleutel nog niet".
     */
    @Test void elke_vorm_van_een_uniciteit_en_van_het_kanaal_compileert() throws Exception {
        DroolsBackend.Build b = DroolsBackend.build(GOLDEN.resolve("anchor/jutterswet-kustwacht").resolve("ir.json"),
                DroolsBackend.Branch.EXEC_MODEL);
        Class<?> wachter = b.beanLoader().loadClass("dev.uchar.regelspraak.facts.Wachter");
        Class<?> post = b.beanLoader().loadClass("dev.uchar.regelspraak.facts.Post");
        Class<?> inconsistentie = b.beanLoader().loadClass("dev.uchar.regelspraak.facts.Inconsistentie");
        Modelplan plan = Vormenplan.kanaal(wachter, post, inconsistentie);
        String tekst = DrlWeergave.tekst(plan);
        assertEquals(tekst, DrlWeergave.tekst(plan), "één plan geeft één tekst");
        String dump = System.getProperty("uchar.drlvorm");
        if (dump != null) Files.writeString(Path.of(dump).resolve("_kanaalplan.drl"), tekst);
        assertEquals(List.of(), compileer(tekst, b.beanLoader()), () -> "de kanaaltekst compileert niet:\n" + tekst);
        for (String v : VERBODEN) assertTrue(!tekst.contains(v), () -> v + " in de kanaaltekst");
        for (String vorm : Vormenplan.KANAALVORMEN)
            assertTrue(tekst.contains(vorm), () -> "de kanaaltekst mist " + vorm + ":\n" + tekst);
    }

    /**
     * Every form of the rules of a defaults stack compiles (step 4), also where no golden has it: a plan made here over
     * the same beans ({@link Vormenplan#defaults}), with the decision of a default computed and collapsed with its fault,
     * the attribution of a characteristic and the decision not to attribute it, each with its control fact, the decision
     * of a reconciliation bound off an outcome, and the conflict under a constant key and under a bound one, as an
     * outcome and as the §9.5 inconsistency. One plan gives one text, written to {@code <map>/_defaultsplan.drl} under
     * {@code -Duchar.drlvorm}.
     *
     * <p><b>Red</b> before step 4, on {@code e2bd685e}: the rendering refused the plan, "de DRL-weergave draagt Afloop
     * nog niet".
     */
    @Test void elke_vorm_van_de_defaults_compileert() throws Exception {
        DroolsBackend.Build b = DroolsBackend.build(GOLDEN.resolve("anchor/jutterswet-kustwacht").resolve("ir.json"),
                DroolsBackend.Branch.EXEC_MODEL);
        Class<?> wachter = b.beanLoader().loadClass("dev.uchar.regelspraak.facts.Wachter");
        Modelplan plan = Vormenplan.defaults(wachter);
        String tekst = DrlWeergave.tekst(plan);
        assertEquals(tekst, DrlWeergave.tekst(plan), "één plan geeft één tekst");
        String dump = System.getProperty("uchar.drlvorm");
        if (dump != null) Files.writeString(Path.of(dump).resolve("_defaultsplan.drl"), tekst);
        assertEquals(List.of(), compileer(tekst, b.beanLoader()), () -> "de defaultstekst compileert niet:\n" + tekst);
        for (String v : VERBODEN) assertTrue(!tekst.contains(v), () -> v + " in de defaultstekst");
        for (String vorm : Vormenplan.DEFAULTSVORMEN)
            assertTrue(tekst.contains(vorm), () -> "de defaultstekst mist " + vorm + ":\n" + tekst);
    }

    /**
     * Every form of a creation and of a link compiles (step 5), also where no golden has it: a plan made here over the
     * same beans ({@link Vormenplan#creaties}), with an object creation in its agenda group that sets an attribute
     * initialisation on the new bean and inserts a characteristic initialisation with an insertion of its own, one
     * without initialisations whose new object is the first end of its link, the link of the input and that of a fact
     * creation, and the fault of a second filling. One plan gives one text, written to {@code <map>/_creatieplan.drl}
     * under {@code -Duchar.drlvorm}.
     *
     * <p><b>Red</b> before step 5, on {@code b4d83a69}: the rendering refused the plan, "de DRL-weergave draagt Creatie
     * nog niet".
     */
    @Test void elke_vorm_van_een_creatie_en_een_koppeling_compileert() throws Exception {
        DroolsBackend.Build b = DroolsBackend.build(GOLDEN.resolve("anchor/jutterswet-kustwacht").resolve("ir.json"),
                DroolsBackend.Branch.EXEC_MODEL);
        Class<?> wachter = b.beanLoader().loadClass("dev.uchar.regelspraak.facts.Wachter");
        Class<?> post = b.beanLoader().loadClass("dev.uchar.regelspraak.facts.Post");
        Modelplan plan = Vormenplan.creaties(wachter, post);
        String tekst = DrlWeergave.tekst(plan);
        assertEquals(tekst, DrlWeergave.tekst(plan), "één plan geeft één tekst");
        String dump = System.getProperty("uchar.drlvorm");
        if (dump != null) Files.writeString(Path.of(dump).resolve("_creatieplan.drl"), tekst);
        assertEquals(List.of(), compileer(tekst, b.beanLoader()), () -> "de creatietekst compileert niet:\n" + tekst);
        for (String v : VERBODEN) assertTrue(!tekst.contains(v), () -> v + " in de creatietekst");
        for (String vorm : Vormenplan.CREATIEVORMEN)
            assertTrue(tekst.contains(vorm), () -> "de creatietekst mist " + vorm + ":\n" + tekst);
    }

    /**
     * Every form of an allocation compiles (step 5), also where no golden has it: a plan made here over the same beans
     * ({@link Vormenplan#verdelingen}), with a recipient with and without its weight, key and cap, and the distribution of
     * each method, with the decimals of its floor and a rest and without them. One plan gives one text, written to
     * {@code <map>/_verdelingsplan.drl} under {@code -Duchar.drlvorm}.
     *
     * <p><b>Red</b> before the allocations of step 5, on {@code abe891c1}: the rendering refused the plan, "de
     * DRL-weergave draagt Ontvanger nog niet".
     */
    @Test void elke_vorm_van_een_verdeling_compileert() throws Exception {
        DroolsBackend.Build b = DroolsBackend.build(GOLDEN.resolve("anchor/jutterswet-kustwacht").resolve("ir.json"),
                DroolsBackend.Branch.EXEC_MODEL);
        Class<?> wachter = b.beanLoader().loadClass("dev.uchar.regelspraak.facts.Wachter");
        Class<?> post = b.beanLoader().loadClass("dev.uchar.regelspraak.facts.Post");
        Modelplan plan = Vormenplan.verdelingen(wachter, post);
        String tekst = DrlWeergave.tekst(plan);
        assertEquals(tekst, DrlWeergave.tekst(plan), "één plan geeft één tekst");
        String dump = System.getProperty("uchar.drlvorm");
        if (dump != null) Files.writeString(Path.of(dump).resolve("_verdelingsplan.drl"), tekst);
        assertEquals(List.of(), compileer(tekst, b.beanLoader()), () -> "de verdelingstekst compileert niet:\n" + tekst);
        for (String v : VERBODEN) assertTrue(!tekst.contains(v), () -> v + " in de verdelingstekst");
        for (String vorm : Vormenplan.VERDELINGSVORMEN)
            assertTrue(tekst.contains(vorm), () -> "de verdelingstekst mist " + vorm + ":\n" + tekst);
    }

    /** Every kind of consequence of the plan stands in a plan of {@link Vormenplan}, so that every kind compiles, also
     *  where no golden has it. Since step 5 the rendering carries every kind: its switch over the consequences has no
     *  default arm, so a new kind does not compile until the rendering spells it, and this witness asks for its form. */
    @Test void elk_soort_gevolg_staat_in_een_vormenplan() throws Exception {
        DroolsBackend.Build b = DroolsBackend.build(GOLDEN.resolve("anchor/jutterswet-kustwacht").resolve("ir.json"),
                DroolsBackend.Branch.EXEC_MODEL);
        Class<?> wachter = b.beanLoader().loadClass("dev.uchar.regelspraak.facts.Wachter");
        Class<?> post = b.beanLoader().loadClass("dev.uchar.regelspraak.facts.Post");
        Class<?> kustvak = b.beanLoader().loadClass("dev.uchar.regelspraak.facts.Kustvak");
        Class<?> inconsistentie = b.beanLoader().loadClass("dev.uchar.regelspraak.facts.Inconsistentie");
        java.util.Set<String> soorten = new java.util.TreeSet<>();
        for (Modelplan p : List.of(Vormenplan.plan(wachter, post, kustvak), Vormenplan.vouwen(wachter, post, kustvak),
                Vormenplan.kanaal(wachter, post, inconsistentie), Vormenplan.defaults(wachter),
                Vormenplan.creaties(wachter, post), Vormenplan.verdelingen(wachter, post)))
            for (Regelplan r : p.regels()) soorten.add(r.gevolg().getClass().getSimpleName());
        java.util.Set<String> alle = new java.util.TreeSet<>();
        for (Class<?> k : dev.uchar.regelspraak.transpiler.tms.plan.Gevolg.class.getPermittedSubclasses())
            alle.add(k.getSimpleName());
        assertEquals(alle, soorten);
    }

    /** The errors of compiling {@code tekst} with {@code KieHelper} under the bean loader {@code bonen}, which is the
     *  context class loader meanwhile, as {@code DroolsBackend} has it; where there is none, the text builds a KieBase
     *  in {@code EQUALITY}. */
    static List<String> compileer(String tekst, ClassLoader bonen) {
        ClassLoader vorige = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(bonen);
        try {
            KieHelper helper = new KieHelper();
            helper.setClassLoader(bonen);
            helper.addContent(tekst, DrlWeergave.PAD);
            Results uitslag = helper.verify();
            List<String> fouten = new ArrayList<>();
            for (Message m : uitslag.getMessages(Message.Level.ERROR))
                fouten.add("r. " + m.getLine() + ": " + m.getText().replace('\n', ' '));
            if (fouten.isEmpty()) assertNotNull(helper.build(EqualityBehaviorOption.EQUALITY));
            return fouten;
        } finally {
            Thread.currentThread().setContextClassLoader(vorige);
        }
    }

    /** The goldens with cases, in order: those the replay can play. */
    private static List<Path> goldens() throws Exception {
        List<Path> uit = new ArrayList<>();
        try (Stream<Path> s = Files.walk(GOLDEN)) {
            for (Path ir : s.filter(p -> p.getFileName().toString().equals("ir.json")).sorted().toList())
                try (Stream<Path> zaken = Files.list(ir.getParent())) {
                    if (zaken.anyMatch(f -> f.getFileName().toString().matches("case.*\\.json"))) uit.add(ir.getParent());
                }
        }
        return uit;
    }
}
