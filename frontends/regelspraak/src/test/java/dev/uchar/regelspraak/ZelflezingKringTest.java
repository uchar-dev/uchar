package dev.uchar.regelspraak;

import dev.uchar.regelspraak.model.RegelSpraakModel;
import dev.uchar.regelspraak.model.RegelSpraakModel.Diagnostic;
import dev.uchar.regelspraak.model.RegelSpraakModel.Severity;
import dev.uchar.regelspraak.resolver.Resolver;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * A computation of X that reads X itself is a dependency cycle of one node, and the verifier refuses it under WF-045
 * exactly as it refuses a longer one. Measured on the frontend before this change: "De x van een Ding moet berekend
 * worden als de x van de Ding plus 1" went to the wire without a word and the verifier answered "WF-045 FAIL …
 * dependency cycle through no recursive creation: ophoog x -> ophoog x", while a cycle over two attributes drew the
 * WARNING "kringverwijzing tussen berekeningen: …". The direct self-read now draws that same WARNING, the clamp idiom
 * "x = x, met een maximum van y" included: the verifier refuses it under WF-045 as well. So does a computation whose
 * CONDITION reads its own target ("… moet gesteld worden op 5 indien de x van de Ding groter is dan 3"), which the
 * verifier refuses the same way ("zet x -> zet x", measured before this change). Beyond the self-read the graph
 * mirrors the reads of RUNTIME-SEMANTICS ch. 5.1 and 6.1: what a rule's condition reads is an edge as much as what
 * its value reads, and a characteristic is a node as much as an attribute. Measured before that change, each of the
 * following was refused by the verifier under WF-045 and passed the frontend without a word: a cycle through a
 * condition over another attribute ("a -> b -> a"), a characteristic attribution reading its own characteristic
 * ("oud -> oud"), two characteristics reading each other, plain or negated ("oud -> versleten -> oud"), and a
 * characteristic and an attribute reading each other ("oud -> x -> oud").
 *
 * <p>A cycle that only the opgave marker's lowering closes (grammar/DIVERGENCES.md §7) runs through a member the
 * marker generates: the computation of X reads {@code berekende X}, which it writes once it has moved, or it reads
 * {@code afwijkende opgave X}, which the generated rule attributes from {@code berekende X}. Measured before the graph
 * covered the lowered forms, the frontend said nothing on all three witnesses below and the verifier refused each
 * under WF-045: "waardering -> waardering", "waardering -> afwijking opgave waarde -> waardering" (with WF-046 twice)
 * and "waardering -> premieregel -> waardering".
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

    private static final String DING_KENMERK = DING + """
            is oud kenmerk (bijvoeglijk);
            is versleten kenmerk (bijvoeglijk);
        """;

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

    @Test void een_zelflezing_in_de_voorwaarde_krijgt_de_melding() {
        Diagnostic d = enigeWaarschuwing(Resolver.resolve(DING + regel("zet x",
                "De x van een Ding moet gesteld worden op 5 indien de x van de Ding groter is dan 3.")));
        assertEquals("kringverwijzing tussen berekeningen: Ding.x -> Ding.x" + STAART, d.message());
        assertEquals(0, d.line());
    }

    @Test void een_zelflezing_in_een_samengestelde_voorwaarde_krijgt_de_melding() {
        Diagnostic d = enigeWaarschuwing(Resolver.resolve(DING + """

            Regel zet x
                geldig altijd
                    De x van een Ding moet gesteld worden op 5
                    indien hij aan alle volgende voorwaarden voldoet:
                    - zijn y is kleiner dan 2
                    - zijn x is groter dan 3.
            """));
        assertEquals("kringverwijzing tussen berekeningen: Ding.x -> Ding.x" + STAART, d.message());
    }

    @Test void een_voorwaarde_op_een_ander_attribuut_zonder_kring_meldt_niets() {
        RegelSpraakModel m = Resolver.resolve(DING + regel("zet x",
                "De x van een Ding moet gesteld worden op 5 indien de y van de Ding groter is dan 3."));
        assertTrue(m.diagnostics.isEmpty(), () -> "onverwachte diagnostiek: " + m.diagnostics);
    }

    @Test void een_kring_door_een_voorwaarde_over_een_ander_attribuut_wordt_gemeld() {
        Diagnostic d = enigeWaarschuwing(Resolver.resolve(DING
                + regel("a", "De x van een Ding moet gesteld worden op 5 indien de y van de Ding groter is dan 3.")
                + regel("b", "De y van een Ding moet berekend worden als de x van de Ding plus 1.")));
        assertEquals("kringverwijzing tussen berekeningen: Ding.x -> Ding.y -> Ding.x" + STAART, d.message());
    }

    @Test void een_kenmerkregel_die_haar_eigen_kenmerk_leest_wordt_gemeld() {
        Diagnostic d = enigeWaarschuwing(Resolver.resolve(DING_KENMERK
                + regel("oud", "Een Ding is oud indien hij oud is.")));
        assertEquals("kringverwijzing tussen berekeningen: Ding.oud -> Ding.oud" + STAART, d.message());
    }

    @Test void een_kring_over_twee_kenmerken_wordt_gemeld() {
        Diagnostic d = enigeWaarschuwing(Resolver.resolve(DING_KENMERK
                + regel("oud", "Een Ding is oud indien hij versleten is.")
                + regel("versleten", "Een Ding is versleten indien hij oud is.")));
        assertEquals("kringverwijzing tussen berekeningen: Ding.oud -> Ding.versleten -> Ding.oud" + STAART, d.message());
    }

    @Test void een_kring_over_twee_ontkende_kenmerken_wordt_gemeld() {
        Diagnostic d = enigeWaarschuwing(Resolver.resolve(DING_KENMERK
                + regel("oud", "Een Ding is oud indien hij niet versleten is.")
                + regel("versleten", "Een Ding is versleten indien hij niet oud is.")));
        assertEquals("kringverwijzing tussen berekeningen: Ding.oud -> Ding.versleten -> Ding.oud" + STAART, d.message());
    }

    @Test void een_kring_over_een_kenmerk_en_een_attribuut_wordt_gemeld() {
        Diagnostic d = enigeWaarschuwing(Resolver.resolve(DING_KENMERK
                + regel("oud", "Een Ding is oud indien zijn x groter is dan 3.")
                + regel("x", "De x van een Ding moet gesteld worden op 5 indien hij oud is.")));
        assertEquals("kringverwijzing tussen berekeningen: Ding.oud -> Ding.x -> Ding.oud" + STAART, d.message());
    }

    @Test void een_kenmerk_in_de_voorwaarde_zonder_kring_meldt_niets() {
        RegelSpraakModel m = Resolver.resolve(DING_KENMERK
                + regel("oud", "Een Ding is oud indien zijn x groter is dan 3.")
                + regel("y", "De y van een Ding moet gesteld worden op 1 indien hij oud is."));
        assertTrue(m.diagnostics.isEmpty(), () -> "onverwachte diagnostiek: " + m.diagnostics);
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

    private static final String VONDST_GEMARKEERD = """
        Objecttype de Vondst (mv: Vondsten)
            de taxatie Numeriek (getal met 2 decimalen);
            de waarde Numeriek (getal met 2 decimalen) (mag opgegeven worden);
            de premie Numeriek (getal met 2 decimalen);
        """;

    @Test void een_berekening_die_haar_eigen_berekende_waarde_leest_wordt_gemeld() {
        RegelSpraakModel m = Resolver.resolve(VONDST_GEMARKEERD + regel("waardering",
                "De waarde van een Vondst moet berekend worden als de berekende waarde van de Vondst plus 1."));
        Diagnostic d = enigeWaarschuwing(m);
        assertEquals("kringverwijzing tussen berekeningen: Vondst.berekende waarde -> Vondst.berekende waarde" + STAART,
                d.message());
        assertEquals(0, d.line());
        assertEquals(1, m.prioritizedDefaults.size(), "de melding is een WARNING: de markering wordt gewoon gelowerd");
    }

    @Test void een_voorwaarde_op_het_gegenereerde_kenmerk_sluit_een_kring() {
        Diagnostic d = enigeWaarschuwing(Resolver.resolve(VONDST_GEMARKEERD + regel("waardering",
                "De waarde van een Vondst moet berekend worden als de taxatie van de Vondst "
                + "indien hij geen afwijkende opgave waarde heeft.")));
        assertEquals("kringverwijzing tussen berekeningen: Vondst.berekende waarde -> Vondst.afwijkende opgave waarde "
                + "-> Vondst.berekende waarde" + STAART, d.message());
    }

    @Test void een_kring_via_een_ander_attribuut_door_de_berekende_waarde_wordt_gemeld() {
        Diagnostic d = enigeWaarschuwing(Resolver.resolve(VONDST_GEMARKEERD
                + regel("waardering", "De waarde van een Vondst moet berekend worden als de premie van de Vondst plus 1.")
                + regel("premieregel",
                        "De premie van een Vondst moet berekend worden als de berekende waarde van de Vondst.")));
        assertEquals("kringverwijzing tussen berekeningen: Vondst.berekende waarde -> Vondst.premie "
                + "-> Vondst.berekende waarde" + STAART, d.message());
    }

    @Test void een_lezing_van_de_opgave_of_van_het_doel_zonder_kring_meldt_niets() {
        RegelSpraakModel m = Resolver.resolve(VONDST_GEMARKEERD
                + regel("waardering", "De waarde van een Vondst moet berekend worden als de opgegeven waarde van de "
                        + "Vondst plus de taxatie van de Vondst.")
                + regel("premieregel", "De premie van een Vondst moet berekend worden als de waarde van de Vondst maal 2."));
        assertTrue(m.diagnostics.isEmpty(), () -> "onverwachte diagnostiek: " + m.diagnostics);
    }
}
