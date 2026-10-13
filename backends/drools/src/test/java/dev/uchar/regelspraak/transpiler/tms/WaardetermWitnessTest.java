package dev.uchar.regelspraak.transpiler.tms;

import dev.uchar.regelspraak.cases.DroolsBackend;
import dev.uchar.regelspraak.transpiler.tms.plan.Functie;
import dev.uchar.regelspraak.transpiler.tms.plan.Waardeplan;

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
 * ({@link Term}), and only {@link ClosureWeergave} makes the functions a consequence calls of it. That tree is what a
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
 * the harness {@code Vuurspoor} shows, a dump of every firing and every fact of the corpus and of the suite equal to
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
 * <p><b>Red</b> before the step: the value compilers made the functions themselves and planned nothing, so the
 * transpiler gave no planned value. Before the fix round of step 1b, a restatement and a listed fold named no function,
 * so a rendering had to choose it again, and the source fragments below the root and the type of a constant beyond its
 * unit went unpinned.
 */
class WaardetermWitnessTest {

    private static final Path GOLDEN = Path.of("..", "..", "corpus", "golden");

    @Test void een_runtimeaanroep_met_eenheidsherschaling_is_een_boom_van_genoemde_aanroepen() throws Exception {
        List<Waardeplan> plannen = plannen("jutterswet-sleepbaan");
        assertEquals(List.of(
                "sleeplengte van een sleepbaan: Herschaal HERSCHAAL_EXACT km->m 1000/1 (RS_PLUS(Const 2.5 km, "
                        + "Herschaal HERSCHAAL_EXACT m->km 1/1000 (Lees 0)))"), bomen(plannen));
        // the write boundary has no node of its own in the IR: it carries the fragment of the value it restates, and so
        // does the restatement of the right operand
        assertEquals(List.of(
                "sleeplengte van een sleepbaan 0 Herschaal bron '2,5 km plus de aanloop van de Sleepbaan'",
                "sleeplengte van een sleepbaan 0.0 Aanroep bron '2,5 km plus de aanloop van de Sleepbaan'",
                "sleeplengte van een sleepbaan 0.0.0 Const BigDecimal type Numeriek[geheel=false, decimalen=1, "
                        + "teken=ONBEPAALD, eenheid=Eenheid[afkorting=km, stelsel=Afstand, canoniek=m, "
                        + "factorNaarCanoniek=1000, noemer=null]] bron '2,5 km'",
                "sleeplengte van een sleepbaan 0.0.1 Herschaal bron 'de aanloop van de Sleepbaan'",
                "sleeplengte van een sleepbaan 0.0.1.0 Lees bron 'de aanloop van de Sleepbaan'"), velden(plannen));
    }

    @Test void een_let_is_een_knoop_met_zijn_binder_en_zijn_gretigheid() throws Exception {
        List<Waardeplan> plannen = plannen("anchor/jutterswet-tariefstelling");
        assertEquals(List.of(
                "aanslag van een jutter: Let 1 ink [Lees 0] in [Let 3 aftrek [RS_MAAL(Lees 2, Const 250)] in "
                        + "[Let 4 belasting [RS_AFRONDING(RS_MAAL(Binder 1 ink, Const 0.21), Const 2, Const HALF_UP)] in "
                        + "[Keuze(RS_AGG_VERGELIJK(Binder 3 aftrek, Binder 4 belasting, Const >), Const 0, "
                        + "RS_MIN(Binder 4 belasting, Binder 3 aftrek))]]]",
                "factor van een jutter: RS_PLUS(Const 1, Deel GEDEELD sonde 1 (Lees 0, Const 2))",
                "gewogen inkomen van een jutter: RS_AFRONDING(Let 1 ink2 gretig [Lees 0] in "
                        + "[RS_MAAL(Binder 1 ink2, Lees 2)], Const 2, Const HALF_UP)"), bomen(plannen));
        // the constants of the analysis, the decimals and the rounding mode, have no type and no node in the IR
        assertEquals(List.of(
                "aanslag van een jutter 0 Let bron 'ink is het inkomen van de Jutter'",
                "aanslag van een jutter 0.0 Lees bron 'het inkomen van de Jutter'",
                "aanslag van een jutter 0.1 Let bron 'aftrek is het kindertal van de Jutter maal 250,00'",
                "aanslag van een jutter 0.1.0 Aanroep bron 'het kindertal van de Jutter maal 250,00'",
                "aanslag van een jutter 0.1.0.0 Lees bron 'het kindertal van de Jutter'",
                "aanslag van een jutter 0.1.0.1 Const BigDecimal type "
                        + "Numeriek[geheel=false, decimalen=2, teken=ONBEPAALD, eenheid=null] bron '250,00'",
                "aanslag van een jutter 0.1.1 Let bron 'belasting is ink maal 0,2100 rekenkundig afgerond op 2 "
                        + "decimalen'",
                "aanslag van een jutter 0.1.1.0 Aanroep bron 'ink maal 0,2100 rekenkundig afgerond op 2 decimalen'",
                "aanslag van een jutter 0.1.1.0.0 Aanroep bron 'ink maal 0,2100'",
                "aanslag van een jutter 0.1.1.0.0.0 Binder bron 'ink'",
                "aanslag van een jutter 0.1.1.0.0.1 Const BigDecimal type "
                        + "Numeriek[geheel=false, decimalen=4, teken=ONBEPAALD, eenheid=null] bron '0,2100'",
                "aanslag van een jutter 0.1.1.0.1 Const Integer",
                "aanslag van een jutter 0.1.1.0.2 Const RoundingMode",
                "aanslag van een jutter 0.1.1.1 Keuze bron 'als aftrek groter dan belasting dan 0,00 anders belasting "
                        + "min aftrek'",
                "aanslag van een jutter 0.1.1.1.0 Aanroep bron 'aftrek groter dan belasting'",
                "aanslag van een jutter 0.1.1.1.0.0 Binder bron 'aftrek'",
                "aanslag van een jutter 0.1.1.1.0.1 Binder bron 'belasting'",
                "aanslag van een jutter 0.1.1.1.0.2 Const String",
                "aanslag van een jutter 0.1.1.1.1 Const BigDecimal type "
                        + "Numeriek[geheel=false, decimalen=2, teken=ONBEPAALD, eenheid=null] bron '0,00'",
                "aanslag van een jutter 0.1.1.1.2 Aanroep bron 'belasting min aftrek'",
                "aanslag van een jutter 0.1.1.1.2.0 Binder bron 'belasting'",
                "aanslag van een jutter 0.1.1.1.2.1 Binder bron 'aftrek'",
                "factor van een jutter 0 Aanroep bron '1,0000 plus het kindertal van de Jutter gedeeld door 2,0000'",
                "factor van een jutter 0.0 Const BigDecimal type "
                        + "Numeriek[geheel=false, decimalen=4, teken=ONBEPAALD, eenheid=null] bron '1,0000'",
                "factor van een jutter 0.1 Deel bron 'het kindertal van de Jutter gedeeld door 2,0000'",
                "factor van een jutter 0.1.0 Lees bron 'het kindertal van de Jutter'",
                "factor van een jutter 0.1.1 Const BigDecimal type "
                        + "Numeriek[geheel=false, decimalen=4, teken=ONBEPAALD, eenheid=null] bron '2,0000'",
                "gewogen inkomen van een jutter 0 Aanroep bron 'ink2 maal de factor van de Jutter rekenkundig "
                        + "afgerond op 2 decimalen'",
                "gewogen inkomen van een jutter 0.0 Let bron 'ink2 is het inkomen van de Jutter'",
                "gewogen inkomen van een jutter 0.0.0 Lees bron 'het inkomen van de Jutter'",
                "gewogen inkomen van een jutter 0.0.1 Aanroep bron 'ink2 maal de factor van de Jutter'",
                "gewogen inkomen van een jutter 0.0.1.0 Binder bron 'ink2'",
                "gewogen inkomen van een jutter 0.0.1.1 Lees bron 'de factor van de Jutter'",
                "gewogen inkomen van een jutter 0.1 Const Integer",
                "gewogen inkomen van een jutter 0.2 Const RoundingMode"), velden(plannen));
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
        return plannen.stream().map(p -> p.vorm() + ": " + Termboom.boom(p.waarde())).toList();
    }

    /** Per plan, every node of its tree in pre-order, with what the tree text leaves out ({@link Termboom#velden}). */
    private static List<String> velden(List<Waardeplan> plannen) {
        List<String> uit = new ArrayList<>();
        for (Waardeplan p : plannen) Termboom.velden(p.vorm(), p.waarde(), "0", uit);
        return uit;
    }
}
