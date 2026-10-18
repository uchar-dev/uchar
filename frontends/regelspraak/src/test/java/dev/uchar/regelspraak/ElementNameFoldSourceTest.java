package dev.uchar.regelspraak;

import dev.uchar.regelspraak.ir.Expr;
import dev.uchar.regelspraak.ir.ResolvedRegel;
import dev.uchar.regelspraak.model.RegelSpraakModel;
import dev.uchar.regelspraak.resolver.Resolver;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The element name of a fold source binds independently of the order of the fact types (grammar/DIVERGENCES.md
 * §13.4.1). The name of the elements ("alle vondsten van …") was read as an object type through the first role in
 * declaration order whose name or plural matched, a guessed plural included, apart from the subject and the steps
 * written: with a fact type StrandvondstHeeftWeging (its role 'vondst' filled by the subtype Strandvondst) declared
 * before BergingBetreftVondst (its role 'vondst (mv: vondsten)' filled by Vondst), "alle vondsten van de berging van de
 * Strandvondst" was refused as a chain that lands on Vondst where the name read Strandvondst, and the same fold one
 * step from the Berging folded over the strandvondsten alone, a wrong answer in every executor. Swapping the two fact
 * types made both right. Now a declared name or plural binds before a guessed plural, a name several roles with
 * different fillers carry chooses none of them, and where a walk over the written steps reaches the elements and the
 * name is no object type, the walk decides the element type.
 */
class ElementNameFoldSourceTest {

    private static final String TYPES = """
        Objecttype de Vondst (mv: Vondsten)
            het gewicht Numeriek (geheel getal);

        Objecttype de Strandvondst is een Vondst (mv: Strandvondsten)
            het hoogste gewicht in de berging Numeriek (geheel getal);

        Objecttype de Weging (mv: Wegingen)
            het brutogewicht Numeriek (geheel getal);

        Objecttype de Berging (mv: Bergingen)
            het zwaarste gewicht Numeriek (geheel getal);

        """;

    private static final String A = """
        Feittype StrandvondstHeeftWeging
            de vondst Strandvondst
            de weging Weging
        Eén vondst heeft één weging

        """;

    private static final String B = """
        Feittype BergingBetreftVondst
            de berging Berging
            de vondst (mv: vondsten) Vondst
        Eén berging betreft meerdere vondsten

        """;

    private static final String TWO_STEPS = """
        Regel hoogste gewicht in de berging
            geldig altijd
                Het hoogste gewicht in de berging van een Strandvondst moet berekend worden als de maximale waarde van het gewicht van alle vondsten van de berging van de Strandvondst.
        """;

    private static final String STEP = """
        Regel zwaarste gewicht
            geldig altijd
                Het zwaarste gewicht van een Berging moet berekend worden als de maximale waarde van het gewicht van alle vondsten van de Berging.
        """;

    private static RegelSpraakModel clean(String bron) {
        RegelSpraakModel m = Resolver.resolve(bron);
        assertEquals(List.of(), m.diagnostics.stream().map(RegelSpraakModel.Diagnostic::toString).toList());
        return m;
    }

    /** The fold of the rule's value: its element type, and the steps of its written range (empty for one step). */
    private static void foldsOver(RegelSpraakModel m, String regel, String element, List<String> steps) {
        ResolvedRegel r = m.resolvedRegels.stream().filter(x -> x.naam().equals(regel)).findFirst().orElseThrow();
        Expr.FunctionCall f = assertInstanceOf(Expr.FunctionCall.class, r.waarde());
        Expr.AttribuutRef a = assertInstanceOf(Expr.AttribuutRef.class, f.arguments().get(0));
        assertEquals(element, a.objecttype().naam, "het elementtype");
        assertEquals(steps, a.rangePath().stream().map(b -> b.feittype() + "." + b.rol()).toList());
    }

    private static final List<String> VIA_THE_BERGING =
            List.of("BergingBetreftVondst.berging", "BergingBetreftVondst.vondst");

    // ---- the reproduction: the order of the fact types decided

    @Test void two_steps_with_the_fact_type_of_the_subtype_first() {
        // before: WARNING "keten zonder sourcePath in een vouwbron: 'alle vondsten van de berging van de Strandvondst'
        // komt via haar stappen uit bij Vondst, maar de geschreven elementnaam is gelezen als Strandvondst — …"
        foldsOver(clean(TYPES + A + B + TWO_STEPS), "hoogste gewicht in de berging", "Vondst", VIA_THE_BERGING);
    }

    @Test void two_steps_with_both_plural_declared() {
        // before: the same WARNING
        foldsOver(clean(TYPES + A.replace("    de vondst Strandvondst", "    de vondst (mv: vondsten) Strandvondst")
                + B + TWO_STEPS), "hoogste gewicht in de berging", "Vondst", VIA_THE_BERGING);
    }

    @Test void a_step_from_the_berging_folds_over_all_vondsten() {
        // before: check exit 0 with elementType Strandvondst over BergingBetreftVondst.vondst: the fold kept the
        // strandvondsten alone, and ref, clingo and both Drools branches answered 5 where 9 is right
        foldsOver(clean(TYPES + A + B + STEP), "zwaarste gewicht", "Vondst", List.of());
    }

    @Test void a_role_name_that_names_no_object_type_gets_the_element_type_of_the_walk() {
        // the role name 'geborgen vondst' in two fact types with different fillers, and no object type of that name:
        // the walk over the berging binds the role from the Berging, whose filler is Vondst
        String a = A.replace("    de vondst Strandvondst", "    de geborgen vondst (mv: geborgen vondsten) Strandvondst")
                .replace("Eén vondst heeft", "Eén geborgen vondst heeft");
        String b = B.replace("    de vondst (mv: vondsten) Vondst", "    de geborgen vondst (mv: geborgen vondsten) Vondst")
                .replace("meerdere vondsten", "meerdere geborgen vondsten");
        foldsOver(clean(TYPES + a + b + TWO_STEPS.replace("alle vondsten van de berging",
                "alle geborgen vondsten van de berging")), "hoogste gewicht in de berging", "Vondst",
                List.of("BergingBetreftVondst.berging", "BergingBetreftVondst.geborgen vondst"));
    }

    // ---- the forms that already resolved, unchanged

    @Test void b_with_the_fact_type_of_the_set_first() {
        foldsOver(clean(TYPES + B + A + TWO_STEPS), "hoogste gewicht in de berging", "Vondst", VIA_THE_BERGING);
        foldsOver(clean(TYPES + B + A + STEP), "zwaarste gewicht", "Vondst", List.of());
    }

    @Test void c_the_role_on_the_supertype() {
        foldsOver(clean(TYPES + A.replace("    de vondst Strandvondst", "    de vondst Vondst") + B + TWO_STEPS),
                "hoogste gewicht in de berging", "Vondst", VIA_THE_BERGING);
    }

    @Test void d_another_role_name() {
        foldsOver(clean(TYPES + A.replace("    de vondst Strandvondst", "    de strandvondst Strandvondst")
                .replace("Eén vondst heeft", "Eén strandvondst heeft") + B + TWO_STEPS),
                "hoogste gewicht in de berging", "Vondst", VIA_THE_BERGING);
    }

    @Test void the_subtype_as_written_element_name_narrows_the_population() {
        // "alle strandvondsten van de Berging": the plural of the subtype, the population narrowed (ch. 2.2)
        foldsOver(clean(TYPES + A + B + STEP.replace("alle vondsten van de Berging", "alle strandvondsten van de Berging")),
                "zwaarste gewicht", "Strandvondst", List.of());
    }
}
