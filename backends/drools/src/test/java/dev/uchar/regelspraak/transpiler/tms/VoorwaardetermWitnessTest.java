package dev.uchar.regelspraak.transpiler.tms;

import dev.uchar.regelspraak.cases.DroolsBackend;
import dev.uchar.regelspraak.transpiler.tms.plan.LhsItem;
import dev.uchar.regelspraak.transpiler.tms.plan.Regelplan;
import dev.uchar.regelspraak.transpiler.tms.plan.Term;
import dev.uchar.regelspraak.transpiler.tms.plan.Toetsitem;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The conditions of the exec-model branch's rules are planned before they are rendered (card
 * drl-teksttak-op-truth-maintenance, step 1b2): the condition compilers of {@code TmsTranspiler} give a tree of calls
 * whose value is a truth value ({@link Term}), and only {@link ClosureWeergave} makes the predicate a test or a
 * consequence calls of it. Every decision a compiled condition held in its lambda is a node or a field of a node: the
 * helper that compares, by name; the operator, the polarity and the comparand as constants; how leeg and a fault work
 * through, by the function and by the division, the fold read and the full-timeline read it names; the members, the
 * quantifier and the threshold of a composite, which stops at the first point from which its verdict stands
 * (RUNTIME-SEMANTICS ch. 5.10); the whole-period fold with its grid and test code; and the probe of a deposit. This
 * witness pins those trees for the tests of two goldens, in two lists per golden:
 * <ul>
 *   <li>per rule of a form, in the order of the model: its form, the role of each test, the probe slot of a test
 *       of deposits, and the condition as a tree text ({@link Termboom#boom}): every node by its kind with the fields
 *       the analysis decided, the slots it reads, and the value of each constant;</li>
 *   <li>per test, every node of its condition in pre-order with its position, its kind, its source fragment
 *       where it carries one (the IR's {@code source.snippet}, which a rendering into DRL text puts above the text),
 *       and of a constant the class of its value and its whole IR type ({@link Termboom#velden}). A test whose list
 *       equals, line for line, that of the test before it of the same form is named with "zoals de vorige toets"
 *       instead of repeating the list.</li>
 * </ul>
 * Not pinned: the predicate of a day kind ({@link Term.Dagsoort}), which neither golden has. That the rendering of
 * these trees tests what the compiled conditions tested before the step is what the harness {@code Vuurspoor} shows, a
 * dump of every firing and every fact of the corpus and of the suite equal to the one before the step.
 *
 * <ul>
 *   <li>{@code jutterswet-filterwacht}: "drukte" is a composite under all, a characteristic and then a comparison of a
 *       count fold, which stops where the characteristic does not hold, so the fold, whose filter divides by a numeric
 *       zero for J1's second vondst, is not read there; the fold's read carries its deposit, its filter fault and its
 *       probe ({@code Vouwlezing}). The filter of that fold and of the fold of "zware vondsten" is a composite of one
 *       comparison of a quotient: a leeg divisor makes the quotient leeg with a deposit to the probe in slot 2
 *       ({@code Deel}), and the comparison takes a leeg operand as onwaar ({@code RS_AGG_VERGELIJK}).</li>
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
class VoorwaardetermWitnessTest {

    private static final Path GOLDEN = Path.of("..", "..", "corpus", "golden");

    @Test void een_samengestelde_voorwaarde_is_een_boom_met_haar_kortsluiting() throws Exception {
        List<Regelplan> plannen = regelplannen("jutterswet-filterwacht");
        assertEquals(List.of(
                "vondstental: geen toets",
                "zware vondsten VOORWAARDE: KENMERK(Lees 0, Const true)",
                "drukte ZONDER_FOUT: Samengesteld ALLE 0(KENMERK(Lees 0, Const true), RS_AGG_VERGELIJK(Vouwlezing 3 "
                        + "4 2 -1 (AANTAL(Lees 1)), Const 0, Const >))",
                "drukte FOUT: Samengesteld ALLE 0(KENMERK(Lees 0, Const true), RS_AGG_VERGELIJK(Vouwlezing 3 4 2 -1 "
                        + "(AANTAL(Lees 1)), Const 0, Const >))",
                "drukte DEPONERING sonde 4: Samengesteld ALLE 0(KENMERK(Lees 0, Const true), "
                        + "RS_AGG_VERGELIJK(Vouwlezing 3 4 2 -1 (AANTAL(Lees 1)), Const 0, Const >))",
                "vondstental: geen toets",
                "zware vondsten ZONDER_FOUT: Samengesteld DE 0(RS_AGG_VERGELIJK(Deel GEDEELD sonde 2 (Lees 0, Lees "
                        + "1), Const 2, Const >))",
                "zware vondsten FOUT: Samengesteld DE 0(RS_AGG_VERGELIJK(Deel GEDEELD sonde 4 (Lees 2, Lees 3), "
                        + "Const 2, Const >))",
                "zware vondsten DEPONERING sonde 2: Samengesteld DE 0(RS_AGG_VERGELIJK(Deel GEDEELD sonde 2 (Lees "
                        + "0, Lees 1), Const 2, Const >))",
                "drukte ZONDER_FOUT: Samengesteld DE 0(RS_AGG_VERGELIJK(Deel GEDEELD sonde 2 (Lees 0, Lees 1), "
                        + "Const 2, Const >))",
                "drukte FOUT: Samengesteld DE 0(RS_AGG_VERGELIJK(Deel GEDEELD sonde 4 (Lees 2, Lees 3), Const 2, "
                        + "Const >))",
                "drukte DEPONERING sonde 2: Samengesteld DE 0(RS_AGG_VERGELIJK(Deel GEDEELD sonde 2 (Lees 0, Lees "
                        + "1), Const 2, Const >))"), bomen(plannen));
        assertEquals(List.of(
                "zware vondsten VOORWAARDE 0 Aanroep bron 'hij actief is'",
                "zware vondsten VOORWAARDE 0.0 Lees",
                "zware vondsten VOORWAARDE 0.1 Const Boolean",
                "drukte ZONDER_FOUT 0 Samengesteld bron 'hij aan alle volgende voorwaarden voldoet: - hij is actief "
                        + "- het aantal vondsten van de Jutter die aan de volgende voorwaarde voldoen: zijn gewicht "
                        + "gedeeld door zijn deelnemers is groter dan 2 is groter dan 0.'",
                "drukte ZONDER_FOUT 0.0 Aanroep bron 'hij is actief'",
                "drukte ZONDER_FOUT 0.0.0 Lees",
                "drukte ZONDER_FOUT 0.0.1 Const Boolean",
                "drukte ZONDER_FOUT 0.1 Aanroep bron 'het aantal vondsten van de Jutter die aan de volgende "
                        + "voorwaarde voldoen: zijn gewicht gedeeld door zijn deelnemers is groter dan 2 is groter "
                        + "dan 0'",
                "drukte ZONDER_FOUT 0.1.0 Vouwlezing bron 'het aantal vondsten van de Jutter die aan de volgende "
                        + "voorwaarde voldoen: zijn gewicht gedeeld door zijn deelnemers is groter dan 2'",
                "drukte ZONDER_FOUT 0.1.0.0 Aanroep bron 'het aantal vondsten van de Jutter die aan de volgende "
                        + "voorwaarde voldoen: zijn gewicht gedeeld door zijn deelnemers is groter dan 2'",
                "drukte ZONDER_FOUT 0.1.0.0.0 Lees",
                "drukte ZONDER_FOUT 0.1.1 Const BigDecimal type Numeriek[geheel=true, decimalen=0, teken=ONBEPAALD, "
                        + "eenheid=null] bron '0'",
                "drukte ZONDER_FOUT 0.1.2 Const String",
                "drukte FOUT: zoals de vorige toets",
                "drukte DEPONERING sonde 4: zoals de vorige toets",
                "zware vondsten ZONDER_FOUT 0 Samengesteld bron 'aan de volgende voorwaarde voldoen: zijn gewicht "
                        + "gedeeld door zijn deelnemers is groter dan 2'",
                "zware vondsten ZONDER_FOUT 0.0 Aanroep bron 'zijn gewicht gedeeld door zijn deelnemers is groter "
                        + "dan 2'",
                "zware vondsten ZONDER_FOUT 0.0.0 Deel bron 'zijn gewicht gedeeld door zijn deelnemers'",
                "zware vondsten ZONDER_FOUT 0.0.0.0 Lees bron 'zijn gewicht'",
                "zware vondsten ZONDER_FOUT 0.0.0.1 Lees bron 'zijn deelnemers'",
                "zware vondsten ZONDER_FOUT 0.0.1 Const BigDecimal type Numeriek[geheel=true, decimalen=0, "
                        + "teken=ONBEPAALD, eenheid=null] bron '2'",
                "zware vondsten ZONDER_FOUT 0.0.2 Const String",
                "zware vondsten FOUT: zoals de vorige toets",
                "zware vondsten DEPONERING sonde 2: zoals de vorige toets",
                "drukte ZONDER_FOUT 0 Samengesteld bron 'aan de volgende voorwaarde voldoen: zijn gewicht gedeeld "
                        + "door zijn deelnemers is groter dan 2'",
                "drukte ZONDER_FOUT 0.0 Aanroep bron 'zijn gewicht gedeeld door zijn deelnemers is groter dan 2'",
                "drukte ZONDER_FOUT 0.0.0 Deel bron 'zijn gewicht gedeeld door zijn deelnemers'",
                "drukte ZONDER_FOUT 0.0.0.0 Lees bron 'zijn gewicht'",
                "drukte ZONDER_FOUT 0.0.0.1 Lees bron 'zijn deelnemers'",
                "drukte ZONDER_FOUT 0.0.1 Const BigDecimal type Numeriek[geheel=true, decimalen=0, teken=ONBEPAALD, "
                        + "eenheid=null] bron '2'",
                "drukte ZONDER_FOUT 0.0.2 Const String",
                "drukte FOUT: zoals de vorige toets",
                "drukte DEPONERING sonde 2: zoals de vorige toets"), velden(plannen));
    }

    @Test void een_geheel_periode_toets_is_een_aanroep_over_de_volledige_tijdlijn() throws Exception {
        List<Regelplan> plannen = regelplannen("jutterswet-vast-startpunt");
        assertEquals(List.of(
                "stormwachtdagen van een jutter: geen toets",
                "maandwachtdagen van een jutter: geen toets",
                "toelagesom van een jutter: geen toets",
                "maandtoelagesom van een jutter: geen toets",
                "stormpremie van een jutter ZONDER_FOUT: GEHEEL_PERIODE(VOLLEDIG(Lees 0, Const the whole-period "
                        + "qualifier over \"stormwachtdagen\" needs the full timeline; , Lees 1, Const  supplies only "
                        + "the asOfDate-bucket scalar (ch. 3.9, ch. 8.4)), Const null, Const JAAR, Const 6, Const "
                        + "false)",
                "stormpremie van een jutter FOUT: GEHEEL_PERIODE(VOLLEDIG(Lees 0, Const the whole-period qualifier "
                        + "over \"stormwachtdagen\" needs the full timeline; , Lees 1, Const  supplies only the "
                        + "asOfDate-bucket scalar (ch. 3.9, ch. 8.4)), Const null, Const JAAR, Const 6, Const false)",
                "maandpremie van een jutter ZONDER_FOUT: GEHEEL_PERIODE(VOLLEDIG(Lees 0, Const the whole-period "
                        + "qualifier over \"maandwachtdagen\" needs the full timeline; , Lees 1, Const  supplies only "
                        + "the asOfDate-bucket scalar (ch. 3.9, ch. 8.4)), Const 0, Const MAAND, Const 2, Const "
                        + "false)",
                "maandpremie van een jutter FOUT: GEHEEL_PERIODE(VOLLEDIG(Lees 0, Const the whole-period qualifier "
                        + "over \"maandwachtdagen\" needs the full timeline; , Lees 1, Const  supplies only the "
                        + "asOfDate-bucket scalar (ch. 3.9, ch. 8.4)), Const 0, Const MAAND, Const 2, Const false)",
                "weektoelagesom van een jutter: geen toets",
                "kwartaaltoelagesom van een jutter: geen toets"), bomen(plannen));
        assertEquals(List.of(
                "stormpremie van een jutter ZONDER_FOUT 0 Aanroep bron 'zijn stormwachtdagen gedurende het gehele "
                        + "jaar gevuld is'",
                "stormpremie van een jutter ZONDER_FOUT 0.0 Aanroep bron 'zijn stormwachtdagen'",
                "stormpremie van een jutter ZONDER_FOUT 0.0.0 Lees bron 'zijn stormwachtdagen'",
                "stormpremie van een jutter ZONDER_FOUT 0.0.1 Const String",
                "stormpremie van een jutter ZONDER_FOUT 0.0.2 Lees",
                "stormpremie van een jutter ZONDER_FOUT 0.0.3 Const String",
                "stormpremie van een jutter ZONDER_FOUT 0.1 Const null",
                "stormpremie van een jutter ZONDER_FOUT 0.2 Const String",
                "stormpremie van een jutter ZONDER_FOUT 0.3 Const Integer",
                "stormpremie van een jutter ZONDER_FOUT 0.4 Const Boolean",
                "stormpremie van een jutter FOUT: zoals de vorige toets",
                "maandpremie van een jutter ZONDER_FOUT 0 Aanroep bron 'zijn maandwachtdagen gedurende de gehele "
                        + "maand groter is dan 0'",
                "maandpremie van een jutter ZONDER_FOUT 0.0 Aanroep bron 'zijn maandwachtdagen'",
                "maandpremie van een jutter ZONDER_FOUT 0.0.0 Lees bron 'zijn maandwachtdagen'",
                "maandpremie van een jutter ZONDER_FOUT 0.0.1 Const String",
                "maandpremie van een jutter ZONDER_FOUT 0.0.2 Lees",
                "maandpremie van een jutter ZONDER_FOUT 0.0.3 Const String",
                "maandpremie van een jutter ZONDER_FOUT 0.1 Const BigDecimal type Numeriek[geheel=true, "
                        + "decimalen=0, teken=ONBEPAALD, eenheid=null] bron '0'",
                "maandpremie van een jutter ZONDER_FOUT 0.2 Const String",
                "maandpremie van een jutter ZONDER_FOUT 0.3 Const Integer",
                "maandpremie van een jutter ZONDER_FOUT 0.4 Const Boolean",
                "maandpremie van een jutter FOUT: zoals de vorige toets"), velden(plannen));
    }

    /** The plans of the rules of the forms of {@code golden}, in the order of the model. */
    private static List<Regelplan> regelplannen(String golden) throws Exception {
        return DroolsBackend.build(GOLDEN.resolve(golden).resolve("ir.json"), DroolsBackend.Branch.EXEC_MODEL)
                .execModelTranspiler().modelplan().regels().stream().filter(p -> p.vorm() != null).toList();
    }

    /** The tests of {@code p}, in the order of its left-hand side. */
    private static List<Toetsitem> toetsen(Regelplan p) {
        List<Toetsitem> uit = new ArrayList<>();
        for (LhsItem item : p.linkerkant()) if (item instanceof Toetsitem t) uit.add(t);
        return uit;
    }

    /** Per rule of a form, in the order of the model: its form, the role of each of its tests and the test's
     *  condition as a tree ({@link Termboom#boom}), or that it tests nothing. */
    private static List<String> bomen(List<Regelplan> plannen) {
        List<String> uit = new ArrayList<>();
        for (Regelplan p : plannen) {
            List<Toetsitem> toetsen = toetsen(p);
            if (toetsen.isEmpty()) uit.add(p.vorm() + ": geen toets");
            for (Toetsitem t : toetsen) uit.add(naam(p, t) + ": " + Termboom.boom(t.toets()));
        }
        return uit;
    }

    /** Per test, every node of its condition with what the tree text leaves out ({@link Termboom#velden}); a test of
     *  the same form as the test before it, whose list equals that test's, is named once instead. */
    private static List<String> velden(List<Regelplan> plannen) {
        List<String> uit = new ArrayList<>();
        String vorigeVorm = null;
        List<String> vorige = null;
        for (Regelplan p : plannen)
            for (Toetsitem t : toetsen(p)) {
                List<String> regels = new ArrayList<>();
                Termboom.velden("", t.toets(), "0", regels);
                if (p.vorm().equals(vorigeVorm) && regels.equals(vorige))
                    uit.add(naam(p, t) + ": zoals de vorige toets");
                else for (String r : regels) uit.add(naam(p, t) + r);
                vorigeVorm = p.vorm();
                vorige = regels;
            }
        return uit;
    }

    private static String naam(Regelplan p, Toetsitem t) {
        return p.vorm() + " " + t.rol() + (t.sonde() < 0 ? "" : " sonde " + t.sonde());
    }
}
