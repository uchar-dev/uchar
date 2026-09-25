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

    @Test void tot_de_macht_met_een_eenheid() {
        assertEquals(fout("tot de macht: power operands must be unitless"),
                meldingen("De uitkomst van een Jutter moet berekend worden als zijn waarde tot de macht 2 naar beneden afgerond op 2 decimalen."));
    }
}
