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
     *  measured then. A deliberate removal lowers a floor in the same commit. */
    private static final int EXPECTED_MIN_GOLDENS = 250;
    private static final int EXPECTED_MIN_GECOMPILEERD = 165;

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
     *  carries, raised at step 3a from 1221 to the 1433 measured then, and the goldens that have at least one, which
     *  stayed 224. A deliberate removal lowers a floor in the same commit. */
    private static final int EXPECTED_MIN_GEDRAGEN_REGELS = 1433;
    private static final int EXPECTED_MIN_GOLDENS_MET_REGELS = 224;

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
        DrlWeergave.Weigering w = org.junit.jupiter.api.Assertions.assertThrows(DrlWeergave.Weigering.class,
                () -> DrlWeergave.tekst(Vormenplan.ongedragen(wachter)));
        assertEquals("Uniciteitsschending", w.soort());
        assertEquals("de DRL-weergave draagt Uniciteitsschending nog niet", w.getMessage());
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
