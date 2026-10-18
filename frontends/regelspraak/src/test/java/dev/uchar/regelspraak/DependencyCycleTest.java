package dev.uchar.regelspraak;

import dev.uchar.regelspraak.model.RegelSpraakModel;
import dev.uchar.regelspraak.model.RegelSpraakModel.Diagnostic;
import dev.uchar.regelspraak.model.RegelSpraakModel.Severity;
import dev.uchar.regelspraak.resolver.Resolver;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * A dependency cycle the verifier refuses under WF-045, and under WF-051 where it passes through a defaults group, is
 * refused by the frontend too, in the verifier's sentence, so that {@code check} sees what the verifier sees. The value
 * of one object depends on the same value of another object of the same type, over a role ("de restgewicht van de
 * vorige vondst van de Vondst"): the dependency graph has one node per object type and attribute (IR-SPEC ch. 5.1), so
 * that is a cycle, whatever the objects. The frontend's own cycle warning reads only the reads on a rule's own subject
 * and never saw it.
 *
 * <p><b>Red before the fix</b> (measured 2026-10-02 on the frontend of {@code 70a60dc3}): every cycle row resolved
 * without a diagnostic, while the verifier gave {@code WF-045 FAIL /rules/rules/0/versions/0: dependency cycle through
 * no recursive creation: restgewicht -> bergingsgewicht -> restgewicht} and, through the defaults group,
 * {@code WF-051 FAIL /rules/prioritizedDefaults/0: a dependency cycle passes through defaults group 'meldtermijn' — …}.
 * A cycle on a rule's own subject is refused the same way, beside the WARNING it had ({@code RecursionTest},
 * {@code SelfReadCycleTest}); until 2026-10-06 it kept that WARNING alone. A cycle through an object creation outside
 * a recursive group is refused too since that day ({@code CreationRoleTest}); until then it passed, because a fact
 * creation wrote the population of its target type in the graph and so made a cycle that does not exist with an object
 * creation that counts members of that type (the last row, and {@code resolver.FactCreationEdgesTest}).
 */
class DependencyCycleTest {

    private static List<Diagnostic> faults(RegelSpraakModel m) {
        return m.diagnostics.stream().filter(d -> d.severity() == Severity.ERROR).toList();
    }

    private static final String VONDST_WITH_PREVIOUS = """
        Objecttype de Vondst (mv: Vondsten)
            het gewicht Numeriek (geheel getal);
            het bergingsgewicht Numeriek (geheel getal);
            het restgewicht Numeriek (geheel getal);

        Feittype vorige vondst
            de vondst Vondst
            de vorige vondst Vondst
        Eén vondst heeft één vorige vondst
        """;

    @Test void a_cycle_over_a_role_between_objects_of_a_type_is_refused() {
        RegelSpraakModel m = Resolver.resolve(VONDST_WITH_PREVIOUS + """

            Regel restgewicht
                geldig altijd
                    Het restgewicht van een Vondst moet berekend worden als zijn gewicht min zijn bergingsgewicht.

            Regel bergingsgewicht
                geldig altijd
                    Het bergingsgewicht van een Vondst moet gesteld worden op het restgewicht van de vorige vondst van de Vondst
                    indien het restgewicht van de vorige vondst van de Vondst groter is dan 0.
            """);
        assertEquals(1, m.diagnostics.size(), () -> "verwachtte één diagnose, kreeg: " + m.diagnostics);
        Diagnostic d = faults(m).get(0);
        assertEquals("regel 'restgewicht' hangt via een kring van zichzelf af; de afhankelijkheden zijn kringvrij buiten "
                + "een recursieve creatie (WF-045): dependency cycle through no recursive creation: restgewicht -> "
                + "bergingsgewicht -> restgewicht", d.message());
        assertEquals(11, d.line(), "de kop van de regel waar de kring sluit");
        assertFalse(m.isOk());
    }

    @Test void a_cycle_through_a_deviation_refuses_under_wf045_and_wf051() {
        RegelSpraakModel m = Resolver.resolve("""
            Objecttype de Vondst (mv: Vondsten)
                de meldtermijn Numeriek (geheel getal);
                de restermijn Numeriek (geheel getal);
                de verlenging Numeriek (geheel getal);

            Feittype vorige vondst
                de vondst Vondst
                de vorige vondst Vondst
            Eén vondst heeft één vorige vondst

            Regel meldtermijn
                geldig altijd
                    De meldtermijn van een Vondst moet gesteld worden op 30.

            Afwijking van meldtermijn: verlengd
                geldig altijd
                    De meldtermijn van een Vondst moet berekend worden als 30 plus zijn verlenging
                    indien zijn verlenging groter is dan 0.

            Regel restermijn
                geldig altijd
                    De restermijn van een Vondst moet berekend worden als zijn meldtermijn min 10.

            Regel verlenging
                geldig altijd
                    De verlenging van een Vondst moet gesteld worden op de restermijn van de vorige vondst van de Vondst.
            """);
        assertEquals(List.of(
                "regel 'restermijn' hangt via een kring van zichzelf af; de afhankelijkheden zijn kringvrij buiten een "
                        + "recursieve creatie (WF-045), en een kring draagt geen negatieve rand, zoals een ontkenning "
                        + "(WF-046): dependency cycle through no recursive creation: restermijn -> verlenging -> "
                        + "meldtermijn -> restermijn",
                "defaults-groep 'meldtermijn' ligt op een afhankelijkheidskring, en een kring door een defaults-groep "
                        + "heeft geen strata (WF-051): a dependency cycle passes through defaults group 'meldtermijn' — its "
                        + "displacement reads are negative edges; no strata exist"),
                faults(m).stream().map(Diagnostic::message).toList());
        assertEquals(List.of(20, 11), faults(m).stream().map(Diagnostic::line).toList());
    }

    @Test void a_cycle_through_a_count_is_refused() {
        RegelSpraakModel m = Resolver.resolve("""
            Objecttype de Jutter (mv: Jutters) (bezield)
                het zwaartetal Numeriek (geheel getal);

            Objecttype de Vondst (mv: Vondsten)
                het gewicht Numeriek (geheel getal);
                is zwaar kenmerk (bijvoeglijk);

            Feittype JutterHeeftVondst
                de jutter Jutter
                de vondst (mv: vondsten) Vondst
            Eén jutter heeft meerdere vondsten

            Regel zwaartetal
                geldig altijd
                    Het zwaartetal van een Jutter moet berekend worden als het aantal vondsten van de Jutter die zwaar zijn.

            Regel zwaar
                geldig altijd
                    Een Vondst is zwaar
                    indien zijn gewicht groter is dan het zwaartetal van de jutter van de Vondst.
            """);
        assertEquals(List.of("regel 'zwaartetal' hangt via een kring van zichzelf af; de afhankelijkheden zijn kringvrij "
                + "buiten een recursieve creatie (WF-045): dependency cycle through no recursive creation: zwaartetal -> "
                + "zwaar -> zwaartetal"), faults(m).stream().map(Diagnostic::message).toList());
    }

    @Test void a_cycle_on_its_own_subject_is_refused_beside_its_warning() {
        // the verifier refuses this document under WF-045; the frontend refuses it too, and keeps the WARNING that
        // names the cycle in the members the author wrote
        RegelSpraakModel m = Resolver.resolve("""
            Objecttype de Ding
                de x Numeriek (geheel getal);
                de y Numeriek (geheel getal);

            Regel a
                geldig altijd
                    De x van een Ding moet berekend worden als de y van de Ding plus 1.

            Regel b
                geldig altijd
                    De y van een Ding moet berekend worden als de x van de Ding plus 1.
            """);
        assertEquals(2, m.diagnostics.size(), () -> "verwachtte twee diagnoses, kreeg: " + m.diagnostics);
        assertEquals(Severity.WARNING, m.diagnostics.get(0).severity());
        assertTrue(m.diagnostics.get(0).message().startsWith("kringverwijzing tussen berekeningen: Ding.x -> Ding.y"),
                m.diagnostics.get(0).message());
        assertEquals(List.of("regel 'a' hangt via een kring van zichzelf af; de afhankelijkheden zijn kringvrij buiten "
                + "een recursieve creatie (WF-045): dependency cycle through no recursive creation: a -> b -> a"),
                faults(m).stream().map(Diagnostic::message).toList());
        assertEquals(5, faults(m).get(0).line(), "de kop van de regel waar de kring begint");
    }

    @Test void a_reading_over_a_role_without_cycle_reports_nothing() {
        RegelSpraakModel m = Resolver.resolve(VONDST_WITH_PREVIOUS + """

            Regel restgewicht
                geldig altijd
                    Het restgewicht van een Vondst moet berekend worden als zijn gewicht min 1.

            Regel bergingsgewicht
                geldig altijd
                    Het bergingsgewicht van een Vondst moet gesteld worden op het restgewicht van de vorige vondst van de Vondst.
            """);
        assertTrue(m.diagnostics.isEmpty(), () -> "onverwachte diagnostiek: " + m.diagnostics);
    }

    @Test void a_fact_creation_that_links_existing_objects_to_a_made_object_makes_no_cycle() {
        // the shape of corpus/golden/jutterswet-premiepot: the creation counts the havenjutters of a Haven, the fact
        // creation links those Jutters, which exist, to the Premiepot the creation made; it creates no Jutter
        RegelSpraakModel m = Resolver.resolve("""
            Objecttype de Haven (mv: Havens)
                het premietal Numeriek (geheel getal);

            Objecttype de Jutter (mv: Jutters) (bezield)

            Objecttype de Premiepot (mv: Premiepotten)
                de pot Numeriek (geheel getal);
                het jutteraantal Numeriek (geheel getal);

            Feittype HavenHeeftJutter
                de haven Haven
                de havenjutter (mv: havenjutters) Jutter
            Eén haven heeft meerdere havenjutters

            Feittype HavenHeeftPremiepot
                de premiehaven Haven
                de premiepot Premiepot
            Eén premiehaven heeft één premiepot

            Feittype PremiepotVoorJutters
                de verdeelde premiepot Premiepot
                de premiejutter (mv: premiejutters) Jutter
            Eén verdeelde premiepot heeft meerdere premiejutters

            Regel premiepot
                geldig altijd
                    Een Haven heeft de premiepot met pot gelijk aan het aantal havenjutters van de Haven.

            Regel premiejutters
                geldig altijd
                    Een premiejutter van een verdeelde premiepot is een havenjutter van de premiehaven van de verdeelde premiepot.

            Regel jutteraantal
                geldig altijd
                    Het jutteraantal van een Premiepot moet berekend worden als het aantal premiejutters van de Premiepot.

            Regel premietal
                geldig altijd
                    Het premietal van een Haven moet berekend worden als het jutteraantal van de premiepot van de Haven.
            """);
        assertTrue(m.diagnostics.isEmpty(), () -> "onverwachte diagnostiek: " + m.diagnostics);
    }

    @Test void a_cycle_through_a_recursive_creation_is_admitted() {
        // WF-045 admits a cycle that passes through a creation in a recursive rule group (IR-SPEC ch. 5.3)
        RegelSpraakModel m = Resolver.resolve("""
            Objecttype de Wachter (mv: Wachters) (bezield)
                de rang Numeriek (geheel getal);
                de opvolgerrang Numeriek (geheel getal);

            Feittype opvolging
                de voorganger Wachter
                de opvolger (mv: opvolgers) Wachter
            Eén voorganger heeft één opvolger

            Regel opvolgerrang van een wachter
                geldig altijd
                    De opvolgerrang van een Wachter moet berekend worden als de som van de rang van alle opvolgers van de Wachter of 0 als die er niet zijn.

            Regelgroep estafette is recursief
            Regel werf de opvolger
                geldig altijd
                    Een Wachter heeft een opvolger met rang gelijk aan de rang van de Wachter plus 1
                    indien de rang van de Wachter kleiner is dan 5.
            """);
        assertTrue(faults(m).isEmpty(), () -> "onverwachte fouten: " + m.diagnostics);
    }
}
