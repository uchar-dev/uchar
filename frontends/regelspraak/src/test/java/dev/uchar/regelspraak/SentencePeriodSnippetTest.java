package dev.uchar.regelspraak;

import dev.uchar.regelspraak.emit.IrEmitter;
import dev.uchar.regelspraak.model.RegelSpraakModel;
import dev.uchar.regelspraak.resolver.Resolver;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

/**
 * A snippet is the text of its node and stops before the period that closes the sentence. The grammar lets the last
 * bullet of a list ({@code genesteVoorwaarde}, {@code genestCriterium}) and a 'Daarbij geldt' part take that period,
 * so every node whose context ends there carried it: a fold whose filter is a bullet list, its argument and its filter,
 * a compound condition, a consistency over criteria, a variable. An explanation that puts a value after the snippet
 * then read "… hij is gemeld. (12)".
 *
 * <p><b>Red before the fix</b> (measured 2026-10-22 on the frontend of {@code e78f2ba1}): the six snippets below ended
 * with '.', and the consistency's form and assertion as well.
 */
class SentencePeriodSnippetTest {

    private static final String MODEL = """
        Objecttype de Jutter (mv: Jutters) (bezield)
            het vondsttotaal Numeriek (geheel getal);
            het zwaarste gewicht Numeriek (geheel getal);
            het vangstgetal Numeriek (geheel getal);
            is ervaren kenmerk (bijvoeglijk);

        Objecttype de Vondst (mv: Vondsten)
            het gewicht Numeriek (geheel getal);
            is zwaar kenmerk (bijvoeglijk);
            is gemeld kenmerk (bijvoeglijk);

        Feittype JutterHeeftVondst
            de jutter Jutter
            de vondst (mv: vondsten) Vondst
        Eén jutter heeft meerdere vondsten

        Regel vondsttotaal
            geldig altijd
                Het vondsttotaal van een Jutter moet berekend worden als de som van het gewicht van alle vondsten van de Jutter die aan alle volgende voorwaarden voldoen:
                - hij is zwaar
                - hij is gemeld.

        Regel ervaren
            geldig altijd
                Een Jutter is ervaren
                indien hij aan alle volgende voorwaarden voldoet:
                - zijn vondsttotaal is groter dan 10
                - zijn zwaarste gewicht is groter dan 5.

        Regel vangstgetal
            geldig altijd
                Het vangstgetal van een Jutter moet berekend worden als de vangst plus 1.
                Daarbij geldt:
                    de vangst is het vondsttotaal van de Jutter.

        Regel gewichtscriteria
            geldig altijd
                Een Vondst moet voldoen aan ten minste één van de volgende criteria:
                - zijn gewicht is groter dan 0
                - zijn gewicht is kleiner dan 1000.
        """;

    @Test void no_snippet_carries_the_period_that_closes_the_sentence() {
        RegelSpraakModel m = Resolver.resolve(MODEL);
        assertTrue(m.diagnostics.isEmpty(), () -> "onverwachte diagnostiek: " + m.diagnostics);
        List<String> snippets = new ArrayList<>();
        Matcher s = Pattern.compile("\"snippet\": \"((?:[^\"\\\\]|\\\\.)*)\"").matcher(IrEmitter.emit(m));
        while (s.find()) snippets.add(s.group(1).replace("\\u00e9", "é"));
        for (String want : List.of(
                // the fold, its argument and its filter
                "de som van het gewicht van alle vondsten van de Jutter die aan alle volgende voorwaarden voldoen: - hij "
                        + "is zwaar - hij is gemeld",
                "het gewicht van alle vondsten van de Jutter die aan alle volgende voorwaarden voldoen: - hij is zwaar - "
                        + "hij is gemeld",
                "aan alle volgende voorwaarden voldoen: - hij is zwaar - hij is gemeld",
                // the compound condition
                "hij aan alle volgende voorwaarden voldoet: - zijn vondsttotaal is groter dan 10 - zijn zwaarste gewicht "
                        + "is groter dan 5",
                // the variable
                "de vangst is het vondsttotaal van de Jutter",
                // the consistency over criteria
                "Een Vondst moet voldoen aan ten minste één van de volgende criteria: - zijn gewicht is groter dan 0 - "
                        + "zijn gewicht is kleiner dan 1000"))
            assertTrue(snippets.contains(want), () -> "verwachtte de snippet '" + want + "' in " + snippets);
        assertEquals(List.of(), snippets.stream().filter(x -> x.endsWith(".")).toList(), "snippets met de slotpunt");
    }
}
