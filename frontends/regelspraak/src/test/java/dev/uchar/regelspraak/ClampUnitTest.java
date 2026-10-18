package dev.uchar.regelspraak;

import dev.uchar.regelspraak.model.RegelSpraakModel;
import dev.uchar.regelspraak.model.RegelSpraakModel.Diagnostic;
import dev.uchar.regelspraak.model.RegelSpraakModel.Severity;
import dev.uchar.regelspraak.resolver.Resolver;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * A clamp restates each bound into its base's unit before it compares (WF-099), so a unit on one side alone
 * leaves nothing to restate: the verifier refuses such a clamp, and only a unitless numeric literal adopts the
 * other side's unit. The frontend admitted the one-sided pair in silence (it tolerates a unit-bearing bound over a
 * unitless base for a known casus shape), so it wrote a document the verifier refuses without a word to the
 * author. It now says so in the verifier's sentence, as a WARNING: the tolerated casus shape has to keep its
 * document. Measured on the frontend before this: every sentence below resolved without a diagnostic.
 */
class ClampUnitTest {

    private static String source(String eenheid, String limit, String extraMember) {
        return """
            Eenheidsysteem Munt
                de euro (mv: euros) EUR €

            Objecttype de Jutter (mv: Jutters) (bezield)
                de vondstwaarde Numeriek (getal met 2 decimalen)%1$s;
                de bergingskosten Numeriek (getal met 2 decimalen)%1$s;
                het bergloon Numeriek (getal met 2 decimalen)%1$s;%3$s

            Regel bergloon van een jutter
                geldig altijd
                    Het bergloon van een Jutter moet berekend worden als zijn vondstwaarde min zijn bergingskosten, %2$s.
            """.formatted(eenheid, limit, extraMember);
    }

    private static List<String> meldingen(String src) {
        RegelSpraakModel m = Resolver.resolve(src);
        assertTrue(m.errors().isEmpty(), () -> "onverwachte fouten: " + m.errors());
        return m.diagnostics.stream().map(d -> d.severity() + " " + d.line() + " " + d.message()).toList();
    }

    @Test void a_bound_with_a_unit_over_a_unitless_base() {
        assertEquals(List.of("WARNING 11 clamp minimum and base have incompatible units"),
                meldingen(source("", "met een minimum van 0 €", "")));
        assertEquals(List.of("WARNING 11 clamp maximum and base have incompatible units"),
                meldingen(source("", "met een maximum van 100 €", "")));
        assertEquals(List.of("WARNING 11 clamp minimum and base have incompatible units",
                        "WARNING 11 clamp maximum and base have incompatible units"),
                meldingen(source("", "met een minimum van 0 € en een maximum van 100 €", "")));
    }

    @Test void a_unitless_attribute_bound_over_a_base_with_a_unit() {
        // only a LITERAL adopts the other side's unit; an attribute declared without one does not
        assertEquals(List.of("WARNING 12 clamp minimum and base have incompatible units"),
                meldingen(source(" met eenheid €", "met een minimum van zijn ondergrens",
                        "\n    de ondergrens Numeriek (getal met 2 decimalen);")));
    }

    @Test void a_unitless_literal_takes_over_the_unit_and_equal_units_fit() {
        assertEquals(List.of(), meldingen(source("", "met een minimum van 0", "")));
        assertEquals(List.of(), meldingen(source(" met eenheid €", "met een minimum van 0", "")));
        assertEquals(List.of(), meldingen(source(" met eenheid €", "met een minimum van 0 €", "")));
    }

    /** Two units that do not convert are the same WF-099 defect as a unit on one side alone, and the resolver has
     *  always refused them; since the card frontend-emitterrestanten in the verifier's sentence as well, where it
     *  used to say "begrenzing: eenheden 'EUR' en 'kg' zijn niet in elkaar om te rekenen". */
    @Test void two_incompatible_units_get_the_same_sentence_as_error() {
        RegelSpraakModel m = Resolver.resolve("""
            Eenheidsysteem Munt
                de euro (mv: euros) EUR €

            Eenheidsysteem Gewicht
                het kilogram (mv: kilogrammen) kg

            Objecttype de Jutter (mv: Jutters) (bezield)
                de vondstwaarde Numeriek (getal met 2 decimalen) met eenheid €;
                het bergloon Numeriek (getal met 2 decimalen) met eenheid €;

            Regel bergloon van een jutter
                geldig altijd
                    Het bergloon van een Jutter moet berekend worden als zijn vondstwaarde, met een minimum van 0 kg en een maximum van 5 kg.
            """);
        assertEquals(List.of("ERROR 13 clamp minimum and base have incompatible units",
                        "ERROR 13 clamp maximum and base have incompatible units"),
                m.diagnostics.stream().map(d -> d.severity() + " " + d.line() + " " + d.message()).toList());
    }

    @Test void the_warning_keeps_the_document() {
        RegelSpraakModel m = Resolver.resolve(source("", "met een minimum van 0 €", ""));
        assertTrue(m.isOk(), () -> "een WARNING blokkeert het document niet: " + m.diagnostics);
        assertEquals(Severity.WARNING, m.diagnostics.stream().map(Diagnostic::severity).findFirst().orElseThrow());
    }
}
