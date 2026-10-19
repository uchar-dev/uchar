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
class HeadLineEndTest {

    private static final String VONDST = """
        Objecttype de Vondst (mv: Vondsten)
            het gewicht Numeriek (geheel getal);

        """;

    private static RegelSpraakModel clean(String source) {
        RegelSpraakModel m = Resolver.resolve(source);
        assertEquals(List.of(), m.diagnostics.stream().map(RegelSpraakModel.Diagnostic::toString).toList());
        return m;
    }

    private static String extension(String member, String rule) {
        return VONDST + "Extensie van objecttype de Vondst\n    " + member + "\n\n" + rule;
    }

    private static final String ABOVE_THE_MAAT = "Regel kenmerk\n    geldig altijd\n        Een Vondst heeft boven de maat\n"
            + "        indien zijn gewicht groter is dan 10.\n";

    private static void onlyVondst(RegelSpraakModel m) {
        assertEquals(List.of("Vondst"), List.copyOf(m.objectTypes.keySet()));
    }

    // ---- the four forms of the reproduction

    @Test void e1_f5_a_possessive_characteristic_of_three_words_after_an_extension_head() {
        // before: ERROR "objecttype 'Vondst boven': het kenmerk 'maat' begint met 'de' op de regel waar de kop eindigt,
        // direct na 'boven'; een lid begint op een eigen regel — …", and a second object type 'Vondst boven'
        RegelSpraakModel m = clean(extension("boven de maat kenmerk (bezittelijk);", ABOVE_THE_MAAT));
        onlyVondst(m);
        assertTrue(m.objectTypes.get("Vondst").characteristics.contains("boven de maat"));
    }

    @Test void e1_f8_a_characteristic_of_two_words_after_an_extension_head() {
        // before: ERROR "objecttype 'Vondst zwaar': het kenmerk 'gewogen' begint met 'gewogen' op de regel waar de kop
        // eindigt, direct na 'zwaar'; een lid begint op een eigen regel"
        RegelSpraakModel m = clean(extension("zwaar gewogen kenmerk (bezittelijk);", "Regel kenmerk\n    geldig altijd\n"
                + "        Een Vondst heeft zwaar gewogen\n        indien zijn gewicht groter is dan 10.\n"));
        onlyVondst(m);
        assertTrue(m.objectTypes.get("Vondst").characteristics.contains("zwaar gewogen"));
    }

    @Test void e1_f10_an_attribute_without_article_after_an_extension_head() {
        // before: ERROR "objecttype 'Vondst laatste': het attribuut 'vinddag' begint met 'vinddag' op de regel waar de
        // kop eindigt, direct na 'laatste'; …" and the rule's target refused
        RegelSpraakModel m = clean(extension("laatste vinddag Datum in dagen;", "Regel laatste vinddag\n    geldig altijd\n"
                + "        De laatste vinddag van een Vondst moet gesteld worden op 01-03-2026.\n"));
        onlyVondst(m);
        assertNotNull(m.objectTypes.get("Vondst").attribute("laatste vinddag"));
    }

    @Test void o2_f6_a_characteristic_after_an_object_type_head_without_plural() {
        // before: ERROR "objecttype 'Vondst boven': het kenmerk 'maat' begint met 'de' …" and the rule's subject refused
        RegelSpraakModel m = clean("Objecttype de Vondst\n    boven de maat kenmerk;\n    het gewicht Numeriek (geheel getal);"
                + "\n\nRegel kenmerk\n    geldig altijd\n        Een Vondst is boven de maat\n"
                + "        indien zijn gewicht groter is dan 10.\n");
        onlyVondst(m);
        assertTrue(m.objectTypes.get("Vondst").characteristics.contains("boven de maat"));
    }

    // ---- what stays as it was

    @Test void e1_f4_f9_f11_and_a_head_with_plural_stay() {
        assertTrue(clean(extension("is boven de maat kenmerk (bijvoeglijk);", "Regel kenmerk\n    geldig altijd\n"
                + "        Een Vondst is boven de maat\n        indien zijn gewicht groter is dan 10.\n"))
                .objectTypes.get("Vondst").characteristics.contains("boven de maat"));
        assertTrue(clean(extension("de boven de maat kenmerk (bezittelijk);", "Regel kenmerk\n    geldig altijd\n"
                + "        Een Vondst heeft de boven de maat\n        indien zijn gewicht groter is dan 10.\n"))
                .objectTypes.get("Vondst").characteristics.contains("boven de maat"));
        assertNotNull(clean(extension("de laatste vinddag Datum in dagen;", "Regel laatste vinddag\n    geldig altijd\n"
                + "        De laatste vinddag van een Vondst moet gesteld worden op 01-03-2026.\n"))
                .objectTypes.get("Vondst").attribute("laatste vinddag"));
        assertTrue(clean("Objecttype de Vondst (mv: Vondsten)\n    boven de maat kenmerk;\n    het gewicht Numeriek "
                + "(geheel getal);\n\nRegel kenmerk\n    geldig altijd\n        Een Vondst is boven de maat\n"
                + "        indien zijn gewicht groter is dan 10.\n").objectTypes.get("Vondst").characteristics.contains("boven de maat"));
    }

    @Test void a_word_on_the_line_of_the_head_stays_with_the_name() {
        // the behaviour of the names in declaration heads: prepositions on the head's own line are name words
        RegelSpraakModel m = clean("Objecttype de Vondst bij eb\n    de waarde Numeriek (geheel getal);\n");
        assertEquals(List.of("Vondst bij eb"), List.copyOf(m.objectTypes.keySet()));
    }

    @Test void a_member_on_the_line_of_the_head_stays_refused() {
        RegelSpraakModel m = Resolver.resolve(VONDST + "Extensie van objecttype de Vondst boven de maat kenmerk (bezittelijk);\n");
        assertEquals(List.of("objecttype 'Vondst boven': het kenmerk 'maat' begint met 'de' op de regel waar de kop "
                + "eindigt, direct na 'boven'; een lid begint op een eigen regel — hoort 'de' bij de naam, verbind het dan "
                + "met een koppelteken aan het woord ervoor ('boven-de')"),
                m.errors().stream().map(RegelSpraakModel.Diagnostic::message).toList());
    }
}
