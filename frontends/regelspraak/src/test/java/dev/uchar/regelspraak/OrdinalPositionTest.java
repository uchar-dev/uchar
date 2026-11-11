package dev.uchar.regelspraak;

import dev.uchar.regelspraak.model.RegelSpraakModel;
import dev.uchar.regelspraak.resolver.Resolver;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The rangtelwoord "als &lt;N&gt;e genoemde" (§13.2.12, §5.5.3) names the position of a subject in the rule, so its
 * number is a positive whole number. The lexer's NUMBER also admits 0, a negative, a decimal and a rational number;
 * such an N names no position and is refused (RGS-393). A position beyond the subjects the rule names is RGS-089.
 */
class OrdinalPositionTest {

    private static RegelSpraakModel resolve(String ordinal) {
        return Resolver.resolve("""
            Objecttype de Vondst (mv: Vondsten)
                het gewicht Numeriek (geheel getal);
                de premie Numeriek (geheel getal);

            Regel r
                geldig altijd
                    De premie van een Vondst moet berekend worden als het gewicht van de %s Vondst.
            """.formatted(ordinal));
    }

    @Test void a_number_that_is_no_position_is_refused() {
        for (String ordinal : List.of("als 0e genoemde", "als -1e genoemde", "als 2,5e genoemde", "als 1/2e genoemde",
                "als 99999999999e genoemde")) {
            RegelSpraakModel m = resolve(ordinal);
            assertEquals(1, m.diagnostics.size(), () -> ordinal + ": verwachtte precies één diagnose, kreeg: " + m.diagnostics);
            assertEquals("RGS-393", m.diagnostics.get(0).code(), m.diagnostics.get(0)::toString);
            assertEquals(ordinal, m.diagnostics.get(0).args().get("ordinal"));
        }
    }

    @Test void the_first_position_resolves_and_a_later_one_is_counted() {
        RegelSpraakModel first = resolve("als 1e genoemde");
        assertTrue(first.diagnostics.isEmpty(), () -> "onverwachte diagnostiek: " + first.diagnostics);
        RegelSpraakModel third = resolve("als 3e genoemde");
        assertEquals(List.of("RGS-089"), third.diagnostics.stream().map(RegelSpraakModel.Diagnostic::code).toList());
    }
}
