package dev.uchar.regelspraak.transpiler.tms;

import dev.uchar.regelspraak.cases.DroolsBackend;
import dev.uchar.regelspraak.model.Eenheid;
import dev.uchar.regelspraak.transpiler.tms.plan.Functie;
import dev.uchar.regelspraak.transpiler.tms.plan.Term;
import dev.uchar.regelspraak.transpiler.tms.plan.Waardeplan;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The values of the exec-model branch's rules are planned before they are rendered (card
 * drl-teksttak-op-truth-maintenance, step 1b): the value compilers of {@code TmsTranspiler} give a tree of calls
 * ({@link Term}), and only {@link ClosureWeergave} makes the functions a consequence calls of it. That tree is what a
 * rendering into DRL text will take, so this witness pins it whole, node by node, for the values of two goldens: the
 * kind of every node, the function each call names, the unit factors, the probe slot of a division, the slots and the
 * binders, and whether a {@code Let} may be evaluated eagerly. That the rendering of these trees computes what the
 * compilers computed before the step is what the harness {@code Vuurspoor} shows, a dump of every firing and every fact
 * of the corpus and of the suite equal to the one before the step.
 *
 * <p>Two goldens, since no golden with cases has both a {@code Let} and a runtime call whose operand is restated into
 * another unit (measured over the 250 goldens the branch plays):
 * <ul>
 *   <li>{@code jutterswet-sleepbaan}: {@code 2,5 km plus de aanloop}, the aanloop in m, written into a target in m.
 *       The sum restates its right operand into the left one's unit, by the exact factors 1/1000, and the write
 *       boundary restates the sum into the target's unit, by 1000/1 (RUNTIME-SEMANTICS ch. 2.6).</li>
 *   <li>{@code anchor/jutterswet-tariefstelling}: three nested {@code Let}s around an {@code IfThenElse}, a division
 *       with its probe, and a {@code Let} whose body reads its binder first. The outermost {@code Let} is not eager:
 *       its binder is read only in the value of an inner one, which the condition forces, and the condition is still a
 *       compiled test until step 1b2.</li>
 * </ul>
 *
 * <p><b>Red</b> before the step: the value compilers made the functions themselves and planned nothing, so the
 * transpiler gave no planned value.
 */
class WaardetermWitnessTest {

    private static final Path GOLDEN = Path.of("..", "..", "corpus", "golden");

    @Test void een_runtimeaanroep_met_eenheidsherschaling_is_een_boom_van_genoemde_aanroepen() throws Exception {
        List<Waardeplan> plannen = plannen("jutterswet-sleepbaan");
        assertEquals(List.of(
                "sleeplengte van een sleepbaan: Herschaal km->m 1000/1 (RS_PLUS(Const 2.5 km, "
                        + "Herschaal m->km 1/1000 (Lees 0)))"), bomen(plannen));
        // the write boundary has no node of its own in the IR: it carries the fragment of the value it restates
        assertEquals("2,5 km plus de aanloop van de Sleepbaan", plannen.get(0).waarde().bron());
    }

    @Test void een_let_is_een_knoop_met_zijn_binder_en_zijn_gretigheid() throws Exception {
        List<Waardeplan> plannen = plannen("anchor/jutterswet-tariefstelling");
        assertEquals(List.of(
                "aanslag van een jutter: Let 1 ink [Lees 0] in [Let 3 aftrek [RS_MAAL(Lees 2, Const 250)] in "
                        + "[Let 4 belasting [RS_AFRONDING(RS_MAAL(Binder 1 ink, Const 0.21), Const 2, Const HALF_UP)] in "
                        + "[Keuze(GecompileerdeToets 'aftrek groter dan belasting', Const 0, "
                        + "RS_MIN(Binder 4 belasting, Binder 3 aftrek))]]]",
                "factor van een jutter: RS_PLUS(Const 1, Deel GEDEELD sonde 1 (Lees 0, Const 2))",
                "gewogen inkomen van een jutter: RS_AFRONDING(Let 1 ink2 gretig [Lees 0] in "
                        + "[RS_MAAL(Binder 1 ink2, Lees 2)], Const 2, Const HALF_UP)"), bomen(plannen));
        for (Waardeplan p : plannen)
            assertTrue(p.waarde().bron() != null, "de waarde van " + p.vorm() + " draagt haar bronfragment");
    }

    /** A rendering into DRL text names every function of the plan in an {@code import function}: each constant names a
     *  public static method of its class. */
    @Test void elke_functie_van_het_plan_is_een_publieke_statische_methode() {
        List<String> fout = new ArrayList<>();
        for (Functie f : Functie.values()) {
            boolean gevonden = false;
            for (Method m : f.klasse().getMethods())
                if (m.getName().equals(f.methode()) && Modifier.isStatic(m.getModifiers())) gevonden = true;
            if (!gevonden || !Modifier.isPublic(f.klasse().getModifiers())) fout.add(f + " " + f.klasse().getName() + "."
                    + f.methode());
        }
        assertEquals(List.of(), fout);
    }

    private static List<Waardeplan> plannen(String golden) throws Exception {
        return DroolsBackend.build(GOLDEN.resolve(golden).resolve("ir.json"), DroolsBackend.Branch.EXEC_MODEL)
                .execModelTranspiler().waardeplannen();
    }

    private static List<String> bomen(List<Waardeplan> plannen) {
        return plannen.stream().map(p -> p.vorm() + ": " + boom(p.waarde())).toList();
    }

    /** {@code t} as text: every node by its kind, with the fields the analysis decided. */
    private static String boom(Term t) {
        return switch (t) {
            case Term.Const c -> "Const " + waarde(c.waarde())
                    + (c.type() == null || c.type().eenheidOf() == null ? "" : " " + c.type().eenheidOf().weergave());
            case Term.Lees l -> "Lees " + l.slot();
            case Term.Binder b -> "Binder " + b.slot() + " " + b.naam();
            case Term.Let l -> "Let " + l.slot() + " " + l.naam() + (l.gretig() ? " gretig" : "") + " [" + boom(l.waarde())
                    + "] in [" + boom(l.lichaam()) + "]";
            case Term.Keuze k -> "Keuze(" + boom(k.voorwaarde()) + ", " + boom(k.dan()) + ", " + boom(k.anders()) + ")";
            case Term.Aanroep a -> a.functie() + lijst(a.argumenten());
            case Term.Herschaal h -> "Herschaal " + eenheid(h.van()) + "->" + eenheid(h.naar()) + " " + h.teller()
                    + (h.noemer() == null ? "" : "/" + h.noemer()) + " (" + boom(h.term()) + ")";
            case Term.Deel d -> "Deel " + d.functie() + " sonde " + d.sonde() + " (" + boom(d.links()) + ", "
                    + boom(d.rechts()) + ")";
            case Term.Wortel w -> "Wortel " + w.functie() + " sonde " + w.sonde() + " (" + boom(w.radicand()) + ")";
            case Term.Faal f -> "Faal '" + f.zin() + "'";
            case Term.Vereist v -> "Vereist (" + boom(v.waarde()) + ") '" + v.zin() + "'";
            case Term.Tekstreeks s -> "Tekstreeks" + lijst(s.delen());
            case Term.GelijsteVouw g -> "GelijsteVouw " + (g.kleinste() ? "kleinste" : "grootste") + lijst(g.operanden());
            case Term.Vouwlezing v -> "Vouwlezing " + v.deponeringen() + " " + v.sonde() + " " + v.filterfout() + " "
                    + v.lidfout() + " (" + boom(v.uitkomst()) + ")";
            case Term.Dagsoort d -> "Dagsoort " + d.naam();
            case Term.GecompileerdeToets g -> "GecompileerdeToets '" + g.bron() + "'";
        };
    }

    private static String lijst(List<Term> termen) {
        return termen.stream().map(WaardetermWitnessTest::boom).collect(Collectors.joining(", ", "(", ")"));
    }

    private static String waarde(Object w) {
        return w instanceof BigDecimal b ? b.toPlainString() : String.valueOf(w);
    }

    private static String eenheid(Eenheid e) {
        return e == null ? "-" : e.weergave();
    }
}
