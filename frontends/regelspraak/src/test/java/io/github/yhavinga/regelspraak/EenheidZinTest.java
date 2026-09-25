package io.github.yhavinga.regelspraak;

import io.github.yhavinga.regelspraak.model.RegelSpraakModel;
import io.github.yhavinga.regelspraak.resolver.Resolver;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Where the resolver refuses a unit pair or a non-numeric clamp that the verifier refuses too, it says so in the
 * verifier's sentence: WF-035 for an additive pair and a clamp's numeric-ness, WF-036 for an ordering and an equality
 * comparison, WF-023 for an assignment (IR-SPEC ch. 4.6). The one pair the verifier admits is a unit beside a
 * unitless numeric LITERAL, which adopts the unit; RegelSpraak's typing lets a literal adopt in a comparison only,
 * so in arithmetic and in an assignment that pair is refused for the language's own reason and keeps the
 * language's own words. Measured on the frontend before this: every sentence below was Dutch.
 */
class EenheidZinTest {

    private static List<String> meldingen(String eenheid, String zin) {
        RegelSpraakModel m = Resolver.resolve("""
            Eenheidsysteem Munt
                de euro (mv: euros) EUR €

            Eenheidsysteem Gewicht
                het kilogram (mv: kilogrammen) kg

            Objecttype de Jutter (mv: Jutters) (bezield)
                de vondstwaarde Numeriek (getal met 2 decimalen) met eenheid €;
                de ruilwaarde Numeriek (getal met 2 decimalen) met eenheid kg;
                de telling Numeriek (getal met 2 decimalen);
                de naam Tekst;
                de vondstdatum Datum in dagen;
                de uitkomst Numeriek (getal met 2 decimalen)%s;
                de naamkopie Tekst;
                de datumkopie Datum in dagen;
                is waardevol kenmerk (bijvoeglijk);

            Regel r
                geldig altijd
                    %s
            """.formatted(eenheid, zin));
        return m.diagnostics.stream().map(d -> d.severity() + " " + d.line() + " " + d.message()).toList();
    }

    private static List<String> fout(String zin) { return List.of("ERROR 20 " + zin); }

    @Test void rekenen_krijgt_de_zin_van_wf035() {
        String zin = "plus: additive operands have incompatible units";
        assertEquals(fout(zin), meldingen(" met eenheid €",
                "De uitkomst van een Jutter moet berekend worden als zijn vondstwaarde plus zijn telling."));
        assertEquals(fout(zin), meldingen(" met eenheid €",
                "De uitkomst van een Jutter moet berekend worden als zijn vondstwaarde plus zijn ruilwaarde."));
    }

    @Test void vergelijken_krijgt_de_zinnen_van_wf036() {
        String ordening = "voorwaarde: vergelijking: ordering operands have incompatible units";
        assertEquals(fout(ordening), meldingen("", "Een Jutter is waardevol indien zijn vondstwaarde groter is dan zijn telling."));
        assertEquals(fout(ordening), meldingen("", "Een Jutter is waardevol indien zijn vondstwaarde groter is dan zijn ruilwaarde."));
        String gelijkheid = "voorwaarde: gelijk: equality operands have incompatible units";
        assertEquals(fout(gelijkheid), meldingen("", "Een Jutter is waardevol indien zijn vondstwaarde gelijk is aan zijn telling."));
        assertEquals(fout(gelijkheid), meldingen("", "Een Jutter is waardevol indien zijn vondstwaarde gelijk is aan zijn ruilwaarde."));
    }

    @Test void toekennen_krijgt_de_zin_van_wf023() {
        String zin = "gelijkstelling: toekenning: assignment across incompatible units";
        assertEquals(fout(zin), meldingen(" met eenheid €", "De uitkomst van een Jutter moet gesteld worden op zijn telling."));
        assertEquals(fout(zin), meldingen(" met eenheid €", "De uitkomst van een Jutter moet gesteld worden op zijn ruilwaarde."));
    }

    @Test void een_eenheidloze_literal_naast_een_eenheid_houdt_de_eigen_zin_van_regelspraak() {
        // the verifier admits this pair (the literal adopts the unit), so it has no sentence for it
        assertEquals(fout("plus: als één operand een eenheid heeft, moeten beide er een hebben"),
                meldingen(" met eenheid €", "De uitkomst van een Jutter moet berekend worden als zijn vondstwaarde plus 5."));
        assertEquals(fout("gelijkstelling: toekenning: als één operand een eenheid heeft, moeten beide er een hebben"),
                meldingen(" met eenheid €", "De uitkomst van een Jutter moet gesteld worden op 5."));
        assertEquals(List.of(), meldingen("", "Een Jutter is waardevol indien zijn vondstwaarde groter is dan 5."));
    }

    @Test void een_niet_numerieke_begrenzing_krijgt_de_zin_van_wf035() {
        assertEquals(fout("clamp base is text, must be numeric"), meldingen("",
                "De naamkopie van een Jutter moet gesteld worden op zijn naam, met een minimum van 0."));
        assertEquals(fout("clamp base is date-day, must be numeric"), meldingen("",
                "De datumkopie van een Jutter moet gesteld worden op zijn vondstdatum, met een minimum van 0."));
        assertEquals(fout("clamp minimum is text, must be numeric"), meldingen("",
                "De uitkomst van een Jutter moet gesteld worden op zijn telling, met een minimum van zijn naam."));
        // the base is one position, reported once as the verifier reports it, however many bounds the clamp has
        assertEquals(fout("clamp base is text, must be numeric"), meldingen("",
                "De naamkopie van een Jutter moet gesteld worden op zijn naam, met een minimum van 0 en een maximum van 5."));
    }
}
