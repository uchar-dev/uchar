package dev.uchar.regelspraak;

import dev.uchar.regelspraak.model.RegelSpraakModel;
import dev.uchar.regelspraak.resolver.Resolver;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * A line end bounds an object-type head (grammar/DIVERGENCES.md, "Namen in declaratiekoppen"; §13.3.1.1). The lexer
 * skips every line end, and the name of a head took lowercase words and prepositions after its capital, also from
 * the next physical line: an extension head or a head without "(mv: …)" followed by a member without an article of
 * two words or more ("boven de maat kenmerk (bezittelijk);", "laatste vinddag Datum in dagen;") read the member's
 * first word into the type's name ("Vondst boven"), and the line check refused what was left on the head's line.
 * The name now takes a word only from the line its previous word is on, so the member begins on its own line; a word
 * on the head's own line stays in the name, and a member that begins there is refused as before.
 */
class KopRegeleindeTest {

    private static final String VONDST = """
        Objecttype de Vondst (mv: Vondsten)
            het gewicht Numeriek (geheel getal);

        """;

    private static RegelSpraakModel schoon(String bron) {
        RegelSpraakModel m = Resolver.resolve(bron);
        assertEquals(List.of(), m.diagnostics.stream().map(RegelSpraakModel.Diagnostic::toString).toList());
        return m;
    }

    private static String extensie(String lid, String regel) {
        return VONDST + "Extensie van objecttype de Vondst\n    " + lid + "\n\n" + regel;
    }

    private static final String BOVEN_DE_MAAT = "Regel kenmerk\n    geldig altijd\n        Een Vondst heeft boven de maat\n"
            + "        indien zijn gewicht groter is dan 10.\n";

    private static void alleenVondst(RegelSpraakModel m) {
        assertEquals(List.of("Vondst"), List.copyOf(m.objecttypen.keySet()));
    }

    // ---- the four forms of the reproduction

    @Test void e1_f5_een_bezittelijk_kenmerk_van_drie_woorden_na_een_extensiekop() {
        // before: ERROR "objecttype 'Vondst boven': het kenmerk 'maat' begint met 'de' op de regel waar de kop eindigt,
        // direct na 'boven'; een lid begint op een eigen regel — …", and a second object type 'Vondst boven'
        RegelSpraakModel m = schoon(extensie("boven de maat kenmerk (bezittelijk);", BOVEN_DE_MAAT));
        alleenVondst(m);
        assertTrue(m.objecttypen.get("Vondst").kenmerken.contains("boven de maat"));
    }

    @Test void e1_f8_een_kenmerk_van_twee_woorden_na_een_extensiekop() {
        // before: ERROR "objecttype 'Vondst zwaar': het kenmerk 'gewogen' begint met 'gewogen' op de regel waar de kop
        // eindigt, direct na 'zwaar'; een lid begint op een eigen regel"
        RegelSpraakModel m = schoon(extensie("zwaar gewogen kenmerk (bezittelijk);", "Regel kenmerk\n    geldig altijd\n"
                + "        Een Vondst heeft zwaar gewogen\n        indien zijn gewicht groter is dan 10.\n"));
        alleenVondst(m);
        assertTrue(m.objecttypen.get("Vondst").kenmerken.contains("zwaar gewogen"));
    }

    @Test void e1_f10_een_attribuut_zonder_lidwoord_na_een_extensiekop() {
        // before: ERROR "objecttype 'Vondst laatste': het attribuut 'vinddag' begint met 'vinddag' op de regel waar de
        // kop eindigt, direct na 'laatste'; …" and the rule's target refused
        RegelSpraakModel m = schoon(extensie("laatste vinddag Datum in dagen;", "Regel laatste vinddag\n    geldig altijd\n"
                + "        De laatste vinddag van een Vondst moet gesteld worden op 01-03-2026.\n"));
        alleenVondst(m);
        assertNotNull(m.objecttypen.get("Vondst").attribuut("laatste vinddag"));
    }

    @Test void o2_f6_een_kenmerk_na_een_objecttypekop_zonder_meervoud() {
        // before: ERROR "objecttype 'Vondst boven': het kenmerk 'maat' begint met 'de' …" and the rule's subject refused
        RegelSpraakModel m = schoon("Objecttype de Vondst\n    boven de maat kenmerk;\n    het gewicht Numeriek (geheel getal);"
                + "\n\nRegel kenmerk\n    geldig altijd\n        Een Vondst is boven de maat\n"
                + "        indien zijn gewicht groter is dan 10.\n");
        alleenVondst(m);
        assertTrue(m.objecttypen.get("Vondst").kenmerken.contains("boven de maat"));
    }

    // ---- what stays as it was

    @Test void e1_f4_f9_f11_en_een_kop_met_meervoud_blijven() {
        assertTrue(schoon(extensie("is boven de maat kenmerk (bijvoeglijk);", "Regel kenmerk\n    geldig altijd\n"
                + "        Een Vondst is boven de maat\n        indien zijn gewicht groter is dan 10.\n"))
                .objecttypen.get("Vondst").kenmerken.contains("boven de maat"));
        assertTrue(schoon(extensie("de boven de maat kenmerk (bezittelijk);", "Regel kenmerk\n    geldig altijd\n"
                + "        Een Vondst heeft de boven de maat\n        indien zijn gewicht groter is dan 10.\n"))
                .objecttypen.get("Vondst").kenmerken.contains("boven de maat"));
        assertNotNull(schoon(extensie("de laatste vinddag Datum in dagen;", "Regel laatste vinddag\n    geldig altijd\n"
                + "        De laatste vinddag van een Vondst moet gesteld worden op 01-03-2026.\n"))
                .objecttypen.get("Vondst").attribuut("laatste vinddag"));
        assertTrue(schoon("Objecttype de Vondst (mv: Vondsten)\n    boven de maat kenmerk;\n    het gewicht Numeriek "
                + "(geheel getal);\n\nRegel kenmerk\n    geldig altijd\n        Een Vondst is boven de maat\n"
                + "        indien zijn gewicht groter is dan 10.\n").objecttypen.get("Vondst").kenmerken.contains("boven de maat"));
    }

    @Test void een_woord_op_de_regel_van_de_kop_blijft_bij_de_naam() {
        // the behaviour of the names in declaration heads: prepositions on the head's own line are name words
        RegelSpraakModel m = schoon("Objecttype de Vondst bij eb\n    de waarde Numeriek (geheel getal);\n");
        assertEquals(List.of("Vondst bij eb"), List.copyOf(m.objecttypen.keySet()));
    }

    @Test void een_lid_op_de_regel_van_de_kop_blijft_geweigerd() {
        RegelSpraakModel m = Resolver.resolve(VONDST + "Extensie van objecttype de Vondst boven de maat kenmerk (bezittelijk);\n");
        assertEquals(List.of("objecttype 'Vondst boven': het kenmerk 'maat' begint met 'de' op de regel waar de kop "
                + "eindigt, direct na 'boven'; een lid begint op een eigen regel — hoort 'de' bij de naam, verbind het dan "
                + "met een koppelteken aan het woord ervoor ('boven-de')"),
                m.errors().stream().map(RegelSpraakModel.Diagnostic::message).toList());
    }
}
