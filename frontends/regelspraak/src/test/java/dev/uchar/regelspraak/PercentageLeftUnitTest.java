package dev.uchar.regelspraak;

import dev.uchar.regelspraak.model.RegelSpraakModel;
import dev.uchar.regelspraak.model.RegelSpraakModel.Diagnostic;
import dev.uchar.regelspraak.resolver.Resolver;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * WF-035: the left operand of the percentage of a value is a number without a unit or a percentage in '%'. With € on
 * the left the core algebra gives €² and the unit law of the percentage €, and no clause says which; a rate such as
 * '%/jr' has no reading either. The frontend wrote such a form without a word.
 */
class PercentageLeftUnitTest {

    private static final String MODEL = """
        Eenheidsysteem Munt
            de euro (mv: euros) EUR €

        Eenheidsysteem Tijd
            het jaar (mv: jaren) jr

        Parameter het kortingsbedrag : Numeriek (geheel getal) met eenheid €
        Parameter het kortingsgetal : Numeriek (geheel getal)
        Parameter het percentage korting : Percentage (geheel getal)
        Parameter de rente : Percentage (getal met 2 decimalen) met eenheid %/jr

        Objecttype de Vondst (mv: Vondsten)
            de prijs Numeriek (getal met 2 decimalen) met eenheid €;
            de korting Numeriek (getal met 2 decimalen) met eenheid €;

        Regel korting
            geldig altijd
                De korting van een Vondst moet berekend worden als (VALUE) naar beneden afgerond op 2 decimalen.
        """;

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "(het kortingsbedrag) % van de prijs van de Vondst | Numeriek (EUR)",
            "12 € % van de prijs van de Vondst                 | Numeriek (EUR)",
            "(de rente) % van de prijs van de Vondst           | Percentage (%/jr)",
            "de rente van (de prijs van de Vondst)             | Percentage (%/jr)",
    })
    void a_unit_on_the_left_is_refused(String value, String type) {
        RegelSpraakModel m = Resolver.resolve(MODEL.replace("VALUE", value));
        List<Diagnostic> errors = m.errors();
        assertEquals(1, errors.size(), () -> "verwachtte precies één fout, kreeg: " + m.diagnostics);
        assertEquals("RGS-424", errors.get(0).code(), errors.get(0).message());
        assertEquals(18, errors.get(0).line(), errors.get(0).message());
        assertTrue(errors.get(0).message().contains("van het type " + type), errors.get(0).message());
    }

    /** The form without '%' and a number on its left is RGS-063's, which speaks alone. */
    @org.junit.jupiter.api.Test void the_form_without_percent_sign_keeps_its_own_refusal() {
        RegelSpraakModel m = Resolver.resolve(MODEL.replace("VALUE", "het kortingsbedrag van (de prijs van de Vondst)"));
        assertEquals(List.of("RGS-063"), m.errors().stream().map(Diagnostic::code).toList(), m.diagnostics::toString);
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "(het kortingsgetal) % van de prijs van de Vondst",
            "(het percentage korting) % van de prijs van de Vondst",
            "10 % van de prijs van de Vondst",
            "het percentage korting van (de prijs van de Vondst)",
    })
    void a_number_or_a_percentage_in_percent_stays_clean(String value) {
        RegelSpraakModel m = Resolver.resolve(MODEL.replace("VALUE", value));
        assertTrue(m.errors().isEmpty(), () -> "onverwachte fouten: " + m.errors());
    }
}
