package io.github.yhavinga.regelspraak;

import io.github.yhavinga.regelspraak.ir.Conditie;
import io.github.yhavinga.regelspraak.ir.Expr;
import io.github.yhavinga.regelspraak.ir.ResolvedConsistentie;
import io.github.yhavinga.regelspraak.model.RegelSpraakModel;
import io.github.yhavinga.regelspraak.resolver.Resolver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * §13.4.8 the SAMENGESTELDE consistentieregel — "Een Vondst moet voldoen aan &lt;kwantificatie&gt; van de volgende
 * criteria: …" — reaches the wire as a consistency whose assertion is a {@code Compound} under the written
 * quantifier and threshold (IR-SPEC ch. 4.5), its subject the onderwerp.
 *
 * <p>Measured before the card aantal-bronarm-en-drools-lader: under {@code geen van de}, {@code ten minste},
 * {@code ten hoogste} and {@code precies} the rule was parsed and left off the wire with a WARNING ("wordt nog
 * niet (volledig) naar IR/DRL omgezet"), because the carrier then had no violation rule for them; both Drools
 * branches carry them since that card, and {@code ref} and the ASP backend did already. Under {@code alle} the rule
 * reached the wire only when its criteria named the subject in full ("het gewicht van de Vondst"): the onderwerp
 * set no rule subject, so "zijn gewicht" did not resolve and the rule fell to the same WARNING.
 */
class SamengesteldeConsistentieTest {

    static final String MODEL = """
        Objecttype de Vondst (mv: Vondsten)
            het gewicht Numeriek (getal met 2 decimalen);
            de lengte Numeriek (geheel getal);
            het jaartal Numeriek (geheel getal);
        """;

    private static String regel(String kwantificatie) {
        return """

            Regel keuring
                geldig altijd
                    Een Vondst moet voldoen aan %s volgende criteria:
                    - zijn gewicht is groter dan 0
                    - zijn lengte is groter dan 10
                    - zijn jaartal is kleiner dan 1900.
            """.formatted(kwantificatie);
    }

    @ParameterizedTest
    @CsvSource({
            "'alle van de', ALLE, 0",
            "'ten minste twee van de', TEN_MINSTE, 2",
            "'ten hoogste één van de', TEN_HOOGSTE, 1",
            "'precies één van de', PRECIES, 1",
            "'geen van de', GEEN, 0"
    })
    void de_samengestelde_consistentie_gaat_naar_de_wire(String kwantificatie, Conditie.Kwantor kwantor, int drempel) {
        RegelSpraakModel m = Resolver.resolve(MODEL + regel(kwantificatie));
        assertTrue(m.diagnostics.isEmpty(), () -> "onverwachte diagnostiek: " + m.diagnostics);
        assertEquals(1, m.consistenties.size());
        ResolvedConsistentie c = m.consistenties.get(0);
        assertEquals("Vondst", c.subject().naam);
        Conditie.Samengesteld s = assertInstanceOf(Conditie.Samengesteld.class, c.assertie());
        assertEquals(kwantor, s.kwantor());
        assertEquals(drempel, s.drempel());
        assertEquals(3, s.onderdelen().size());
        for (Conditie deel : s.onderdelen()) {
            Conditie.Vergelijking v = assertInstanceOf(Conditie.Vergelijking.class, deel);
            Expr.AttribuutRef a = assertInstanceOf(Expr.AttribuutRef.class, v.links());
            assertEquals("Vondst", a.objecttype().naam, "\"zijn …\" reads the onderwerp");
        }
    }

    @Test void de_onpersoonlijke_vorm_telt_ook() {
        RegelSpraakModel m = Resolver.resolve(MODEL + """

            Regel keuring
                geldig altijd
                    Er moet worden voldaan aan ten minste twee van de volgende criteria:
                    - het gewicht van de Vondst is groter dan 0
                    - de lengte van de Vondst is groter dan 10.
            """);
        assertTrue(m.diagnostics.isEmpty(), () -> "onverwachte diagnostiek: " + m.diagnostics);
        Conditie.Samengesteld s = assertInstanceOf(Conditie.Samengesteld.class, m.consistenties.get(0).assertie());
        assertEquals(Conditie.Kwantor.TEN_MINSTE, s.kwantor());
        assertEquals(2, s.drempel());
    }

    @Test void een_criterium_dat_de_drager_niet_tilt_houdt_de_melding() {
        // a kenmerk criterion has no violation rule on this carrier under any quantifier: the rule stays off the
        // wire with its WARNING, now as before
        RegelSpraakModel m = Resolver.resolve(MODEL.replace("het jaartal Numeriek (geheel getal);",
                "het jaartal Numeriek (geheel getal);\n    is oud kenmerk (bijvoeglijk);") + """

            Regel keuring
                geldig altijd
                    Een Vondst moet voldoen aan ten minste één van de volgende criteria:
                    - zijn gewicht is groter dan 0
                    - hij is oud.
            """);
        assertTrue(m.consistenties.isEmpty());
        assertTrue(m.diagnostics.stream().anyMatch(d -> d.message().startsWith(
                "consistentieregel wordt nog niet (volledig) naar IR/DRL omgezet")), () -> "" + m.diagnostics);
    }
}
