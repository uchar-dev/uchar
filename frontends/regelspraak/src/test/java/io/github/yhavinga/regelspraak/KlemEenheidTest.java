package io.github.yhavinga.regelspraak;

import io.github.yhavinga.regelspraak.model.RegelSpraakModel;
import io.github.yhavinga.regelspraak.model.RegelSpraakModel.Diagnostic;
import io.github.yhavinga.regelspraak.model.RegelSpraakModel.Severity;
import io.github.yhavinga.regelspraak.resolver.Resolver;
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
class KlemEenheidTest {

    private static String bron(String eenheid, String grens, String extraLid) {
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
            """.formatted(eenheid, grens, extraLid);
    }

    private static List<String> meldingen(String src) {
        RegelSpraakModel m = Resolver.resolve(src);
        assertTrue(m.errors().isEmpty(), () -> "onverwachte fouten: " + m.errors());
        return m.diagnostics.stream().map(d -> d.severity() + " " + d.line() + " " + d.message()).toList();
    }

    @Test void een_grens_met_een_eenheid_over_een_eenheidloze_basis() {
        assertEquals(List.of("WARNING 11 clamp minimum and base have incompatible units"),
                meldingen(bron("", "met een minimum van 0 €", "")));
        assertEquals(List.of("WARNING 11 clamp maximum and base have incompatible units"),
                meldingen(bron("", "met een maximum van 100 €", "")));
        assertEquals(List.of("WARNING 11 clamp minimum and base have incompatible units",
                        "WARNING 11 clamp maximum and base have incompatible units"),
                meldingen(bron("", "met een minimum van 0 € en een maximum van 100 €", "")));
    }

    @Test void een_eenheidloze_attribuutgrens_over_een_basis_met_eenheid() {
        // only a LITERAL adopts the other side's unit; an attribute declared without one does not
        assertEquals(List.of("WARNING 12 clamp minimum and base have incompatible units"),
                meldingen(bron(" met eenheid €", "met een minimum van zijn ondergrens",
                        "\n    de ondergrens Numeriek (getal met 2 decimalen);")));
    }

    @Test void een_eenheidloze_literal_neemt_de_eenheid_over_en_gelijke_eenheden_passen() {
        assertEquals(List.of(), meldingen(bron("", "met een minimum van 0", "")));
        assertEquals(List.of(), meldingen(bron(" met eenheid €", "met een minimum van 0", "")));
        assertEquals(List.of(), meldingen(bron(" met eenheid €", "met een minimum van 0 €", "")));
    }

    @Test void de_waarschuwing_houdt_het_document() {
        RegelSpraakModel m = Resolver.resolve(bron("", "met een minimum van 0 €", ""));
        assertTrue(m.isOk(), () -> "een WARNING blokkeert het document niet: " + m.diagnostics);
        assertEquals(Severity.WARNING, m.diagnostics.stream().map(Diagnostic::severity).findFirst().orElseThrow());
    }
}
