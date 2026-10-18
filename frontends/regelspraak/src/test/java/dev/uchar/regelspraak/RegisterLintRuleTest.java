package dev.uchar.regelspraak;

import dev.uchar.regelspraak.model.RegelSpraakModel;
import dev.uchar.regelspraak.model.RegelSpraakModel.Severity;
import dev.uchar.regelspraak.resolver.Resolver;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The register lint of Tabel 16 on the condition side: a characteristic negated with the verb of the other kind
 * ("hij geen waardevol heeft" for an adjectival one, "hij geen drijfvermogen is" for a possessive one) gets a WARNING,
 * and it stands on the line of the sentence that writes the check; in a row of a decision table, which is parsed in a
 * stream of its own, on the line of the table. Measured on the frontend before this work: all four WARNINGs below
 * stood on line 0, the result side already on its sentence's line.
 */
class RegisterLintRuleTest {

    private static final String SOURCE = """
        Objecttype de Vondst (mv: Vondsten)
            de waarde Numeriek (geheel getal);
            de premie Numeriek (geheel getal);
            de toeslag Numeriek (geheel getal);
            is waardevol kenmerk (bijvoeglijk);
            het drijfvermogen kenmerk (bezittelijk);

        Objecttype de Jutter (mv: Jutters) (bezield)
            de leeftijd Numeriek (geheel getal);
            is ervaren kenmerk (bijvoeglijk);

        Feittype vondst van jutter
            de vinder Jutter
            de vondst Vondst
        Eén vinder heeft meerdere vondsten

        Regel waarde van een vondst
            geldig altijd
                De waarde van een Vondst moet berekend worden als 5
                indien hij geen waardevol heeft.

        Regel premie van een vondst
            geldig altijd
                De premie van een Vondst moet gesteld worden op 7
                indien zijn vinder geen ervaren heeft.

        Regel leeftijd van een jutter
            geldig altijd
                De leeftijd van een Jutter moet gesteld worden op 40
                indien alle vondsten van de Jutter niet waardevol hebben.

        Beslistabel toeslag van een vondst
            geldig altijd
        | | de toeslag van een Vondst moet gesteld worden op | indien hij geen drijfvermogen is |
        |---|---|---|
        | 1 | 2 | waar |
        """;

    private static String sentence(String category, String characteristic, String written, String required, String source) {
        return "het " + category + " kenmerk '" + characteristic + "' wordt ontkend met het werkwoord '" + written
                + "', maar Tabel 16 vereist '" + required + "' bij dit soort kenmerk (§8.1.8): " + source;
    }

    @Test void the_warning_stands_on_the_line_of_the_sentence() {
        RegelSpraakModel m = Resolver.resolve(SOURCE);
        assertTrue(m.isOk(), () -> "de lowering is gelijk, dus geen fout: " + m.errors());
        assertTrue(m.diagnostics.stream().allMatch(d -> d.severity() == Severity.WARNING), () -> "" + m.diagnostics);
        // the subject, a navigated subject, the element of a quantifier, and a row of a decision table
        assertEquals(List.of(
                "20: " + sentence("bijvoeglijk", "waardevol", "heeft", "is", "hij geen waardevol heeft"),
                "25: " + sentence("bijvoeglijk", "ervaren", "heeft", "is", "zijn vinder geen ervaren heeft"),
                "30: " + sentence("bijvoeglijk", "waardevol", "heeft", "is",
                        "alle vondsten van de Jutter niet waardevol hebben"),
                "32: " + sentence("bezittelijk", "drijfvermogen", "is", "heeft", "hij geen drijfvermogen is")),
                m.diagnostics.stream().map(d -> d.line() + ": " + d.message()).toList());
    }
}
