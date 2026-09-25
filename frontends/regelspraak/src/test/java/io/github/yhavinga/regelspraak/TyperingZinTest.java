package io.github.yhavinga.regelspraak;

import io.github.yhavinga.regelspraak.model.RegelSpraakModel;
import io.github.yhavinga.regelspraak.resolver.Resolver;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The sister of {@link EenheidZinTest} for the other typing refusals the verifier shares: where the resolver refuses
 * a document the verifier refuses too, the message after the Dutch context prefix is the verifier's sentence, with
 * the kind words the verifier writes. Where RegelSpraak's typing refuses what the verifier admits, the refusal keeps
 * its Dutch words (grammar/DIVERGENCES.md, "producer refusals without a reader sentence"). Measured on the frontend
 * before this: every sentence below was Dutch.
 */
class TyperingZinTest {

    /** The diagnostics of one rule sentence over a model with one attribute of each datatype the typing reads. */
    static List<String> meldingen(String zin) {
        RegelSpraakModel m = Resolver.resolve("""
            Eenheidsysteem Munt
                de euro (mv: euros) EUR €

            Domein Keurklasse is van het type Enumeratie
                'gaaf'
                'beschadigd'

            Domein Weerklasse is van het type Enumeratie
                'kalm'
                'storm'

            Objecttype de Jutter (mv: Jutters) (bezield)
                de waarde Numeriek (getal met 2 decimalen) met eenheid €;
                de telling Numeriek (getal met 2 decimalen);
                het aandeel Percentage (getal met 2 decimalen);
                de naam Tekst;
                de vondstdag Datum in dagen;
                het vondstmoment Datum en tijd in millisecondes;
                de staat Keurklasse;
                het weer Weerklasse;
                de uitkomst Numeriek (getal met 2 decimalen);
                de dag Datum in dagen;
                de klasse Keurklasse;
                is waardevol kenmerk (bijvoeglijk);

            Regel r
                geldig altijd
                    %s
            """.formatted(zin));
        return m.diagnostics.stream().map(d -> d.severity() + " " + d.line() + " " + d.message()).toList();
    }

    static List<String> fout(String zin) { return List.of("ERROR 28 " + zin); }

    @Test void datumprecisie() {
        assertEquals(fout("voorwaarde: gelijk: equality across date precisions"),
                meldingen("Een Jutter is waardevol indien zijn vondstdag gelijk is aan zijn vondstmoment."));
        assertEquals(fout("gelijkstelling: toekenning: a date-ms value is assigned to a date-day target"),
                meldingen("De dag van een Jutter moet gesteld worden op zijn vondstmoment."));
        assertEquals(fout("eerste van: first orders dates of two precisions (day and millisecond), which share no order"),
                meldingen("De dag van een Jutter moet gesteld worden op de eerste van zijn vondstdag en zijn vondstmoment."));
        assertEquals(fout("een tijd-component (uur/minuut/seconde/milliseconde uit): sub-day extractor operand is "
                        + "date-day, must be a millisecond-precision date"),
                meldingen("De uitkomst van een Jutter moet gesteld worden op het uur uit zijn vondstdag."));
        // the duration's own result also meets the unitless target, so only the precision line is pinned here
        assertTrue(meldingen("De uitkomst van een Jutter moet gesteld worden op de tijdsduur van zijn vondstdag tot "
                        + "zijn vondstmoment in hele dagen.")
                .contains("ERROR 28 de (absolute) tijdsduur van … tot …: duration operands mix date precisions (both "
                        + "must share one precision)"));
    }

    @Test void tot_de_macht_met_een_eenheid() {
        assertEquals(fout("tot de macht: power operands must be unitless"),
                meldingen("De uitkomst van een Jutter moet berekend worden als zijn waarde tot de macht 2 naar beneden afgerond op 2 decimalen."));
    }
}
