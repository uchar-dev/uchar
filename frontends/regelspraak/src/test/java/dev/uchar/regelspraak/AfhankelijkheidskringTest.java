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
 * A cycle on a rule's own subject is refused the same way, beside the WARNING it had ({@code RecursieTest},
 * {@code ZelflezingKringTest}); until 2026-10-06 it kept that WARNING alone.
 */
class AfhankelijkheidskringTest {

    private static List<Diagnostic> fouten(RegelSpraakModel m) {
        return m.diagnostics.stream().filter(d -> d.severity() == Severity.ERROR).toList();
    }

    private static final String VONDST_MET_VORIGE = """
        Objecttype de Vondst (mv: Vondsten)
            het gewicht Numeriek (geheel getal);
            het bergingsgewicht Numeriek (geheel getal);
            het restgewicht Numeriek (geheel getal);

        Feittype vorige vondst
            de vondst Vondst
            de vorige vondst Vondst
        Eén vondst heeft één vorige vondst
        """;

    @Test void een_kring_over_een_rol_tussen_objecten_van_een_type_weigert() {
        RegelSpraakModel m = Resolver.resolve(VONDST_MET_VORIGE + """

            Regel restgewicht
                geldig altijd
                    Het restgewicht van een Vondst moet berekend worden als zijn gewicht min zijn bergingsgewicht.

            Regel bergingsgewicht
                geldig altijd
                    Het bergingsgewicht van een Vondst moet gesteld worden op het restgewicht van de vorige vondst van de Vondst
                    indien het restgewicht van de vorige vondst van de Vondst groter is dan 0.
            """);
        assertEquals(1, m.diagnostics.size(), () -> "verwachtte één diagnose, kreeg: " + m.diagnostics);
        Diagnostic d = fouten(m).get(0);
        assertEquals("regel 'restgewicht' hangt via een kring van zichzelf af; de afhankelijkheden zijn kringvrij buiten "
                + "een recursieve creatie (WF-045): dependency cycle through no recursive creation: restgewicht -> "
                + "bergingsgewicht -> restgewicht", d.message());
        assertEquals(11, d.line(), "de kop van de regel waar de kring sluit");
        assertFalse(m.isOk());
    }

    @Test void een_kring_door_een_afwijking_weigert_onder_wf045_en_wf051() {
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
                fouten(m).stream().map(Diagnostic::message).toList());
        assertEquals(List.of(20, 11), fouten(m).stream().map(Diagnostic::line).toList());
    }

    @Test void een_kring_door_een_telling_weigert() {
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
                + "zwaar -> zwaartetal"), fouten(m).stream().map(Diagnostic::message).toList());
    }

    @Test void een_kring_op_het_eigen_onderwerp_weigert_naast_haar_waarschuwing() {
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
                fouten(m).stream().map(Diagnostic::message).toList());
        assertEquals(5, fouten(m).get(0).line(), "de kop van de regel waar de kring begint");
    }

    @Test void een_lezing_over_een_rol_zonder_kring_meldt_niets() {
        RegelSpraakModel m = Resolver.resolve(VONDST_MET_VORIGE + """

            Regel restgewicht
                geldig altijd
                    Het restgewicht van een Vondst moet berekend worden als zijn gewicht min 1.

            Regel bergingsgewicht
                geldig altijd
                    Het bergingsgewicht van een Vondst moet gesteld worden op het restgewicht van de vorige vondst van de Vondst.
            """);
        assertTrue(m.diagnostics.isEmpty(), () -> "onverwachte diagnostiek: " + m.diagnostics);
    }

    @Test void een_kring_door_een_recursieve_creatie_is_toegelaten() {
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
        assertTrue(fouten(m).isEmpty(), () -> "onverwachte fouten: " + m.diagnostics);
    }
}
