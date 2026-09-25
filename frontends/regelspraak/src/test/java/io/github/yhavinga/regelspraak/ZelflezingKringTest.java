package io.github.yhavinga.regelspraak;

import io.github.yhavinga.regelspraak.model.RegelSpraakModel;
import io.github.yhavinga.regelspraak.model.RegelSpraakModel.Diagnostic;
import io.github.yhavinga.regelspraak.model.RegelSpraakModel.Severity;
import io.github.yhavinga.regelspraak.resolver.Resolver;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * A computation of X that reads X itself is a dependency cycle of one node, and the verifier refuses it under WF-045
 * exactly as it refuses a longer one. Measured on the frontend before this change: "De x van een Ding moet berekend
 * worden als de x van de Ding plus 1" went to the wire without a word and the verifier answered "WF-045 FAIL …
 * dependency cycle through no recursive creation: ophoog x -> ophoog x", while a cycle over two attributes drew the
 * WARNING "kringverwijzing tussen berekeningen: …". The direct self-read now draws that same WARNING, the clamp idiom
 * "x = x, met een maximum van y" included: the verifier refuses it under WF-045 as well.
 */
class ZelflezingKringTest {

    private static final String DING = """
        Objecttype de Ding
            de x Numeriek (geheel getal);
            de y Numeriek (geheel getal);
        """;

    private static String regel(String naam, String zin) {
        return "\nRegel " + naam + "\n    geldig altijd\n        " + zin + "\n";
    }

    private static Diagnostic enigeWaarschuwing(RegelSpraakModel m) {
        assertEquals(1, m.diagnostics.size(), () -> "verwachtte precies één diagnose, kreeg: " + m.diagnostics);
        Diagnostic d = m.diagnostics.get(0);
        assertEquals(Severity.WARNING, d.severity(), () -> "verwachtte een WARNING: " + d);
        return d;
    }

    private static List<String> kringmeldingen(RegelSpraakModel m) {
        return m.diagnostics.stream().map(Diagnostic::message).filter(t -> t.startsWith("kringverwijzing")).toList();
    }

    private static final String STAART =
            " — herberekening bereikt mogelijk geen fixpunt (de engine begrenst het aantal activaties)";

    @Test void een_directe_zelflezing_krijgt_de_melding_van_de_kring() {
        Diagnostic d = enigeWaarschuwing(Resolver.resolve(DING
                + regel("ophoog x", "De x van een Ding moet berekend worden als de x van de Ding plus 1.")));
        assertEquals("kringverwijzing tussen berekeningen: Ding.x -> Ding.x" + STAART, d.message());
        assertEquals(0, d.line(), "zoals de melding van de kring over twee attributen: zonder regel");
    }

    @Test void een_zelflezing_door_een_variabelendeel_heen_wordt_gemeld() {
        Diagnostic d = enigeWaarschuwing(Resolver.resolve(DING + """

            Regel ophoog x
                geldig altijd
                    De x van een Ding moet gesteld worden op de hulp plus 1.
                    Daarbij geldt:
                        de hulp is de x van de Ding.
            """));
        assertEquals("kringverwijzing tussen berekeningen: Ding.x -> Ding.x" + STAART, d.message());
    }

    @Test void een_begrenzing_van_iets_anders_dan_het_doel_zelf_wordt_gemeld() {
        Diagnostic d = enigeWaarschuwing(Resolver.resolve(DING + regel("begrens x",
                "De x van een Ding moet gesteld worden op de x van de Ding plus 1, met een maximum van de y van de Ding.")));
        assertEquals("kringverwijzing tussen berekeningen: Ding.x -> Ding.x" + STAART, d.message());
    }

    @Test void een_grens_die_het_doel_leest_wordt_gemeld() {
        Diagnostic d = enigeWaarschuwing(Resolver.resolve(DING + regel("begrens x",
                "De x van een Ding moet gesteld worden op de y van de Ding, met een maximum van de x van de Ding.")));
        assertEquals("kringverwijzing tussen berekeningen: Ding.x -> Ding.x" + STAART, d.message());
    }

    @Test void het_begrenzingsidioom_krijgt_de_melding() {
        Diagnostic d = enigeWaarschuwing(Resolver.resolve(DING + regel("begrens x",
                "De x van een Ding moet gesteld worden op de x van de Ding, met een maximum van de y van de Ding.")));
        assertEquals("kringverwijzing tussen berekeningen: Ding.x -> Ding.x" + STAART, d.message());
    }

    @Test void de_kring_over_twee_attributen_houdt_haar_melding() {
        RegelSpraakModel m = Resolver.resolve(DING
                + regel("a", "De x van een Ding moet berekend worden als de y van de Ding plus 1.")
                + regel("b", "De y van een Ding moet berekend worden als de x van de Ding plus 1."));
        assertEquals(List.of("kringverwijzing tussen berekeningen: Ding.x -> Ding.y -> Ding.x" + STAART),
                kringmeldingen(m));
    }

    @Test void een_zelflezing_op_een_gemarkeerd_attribuut_wordt_gemeld_in_de_namen_van_de_auteur() {
        RegelSpraakModel m = Resolver.resolve("""
            Objecttype de Vondst (mv: Vondsten)
                de taxatie Numeriek (getal met 2 decimalen);
                de waarde Numeriek (getal met 2 decimalen) (mag opgegeven worden);

            Regel waardering
                geldig altijd
                    De waarde van een Vondst moet berekend worden als de waarde van de Vondst plus 1.
            """);
        Diagnostic d = enigeWaarschuwing(m);
        assertEquals("kringverwijzing tussen berekeningen: Vondst.waarde -> Vondst.waarde" + STAART, d.message());
        assertEquals(1, m.prioritizedDefaults.size(), "de melding is een WARNING: de markering wordt gewoon gelowerd");
    }
}
