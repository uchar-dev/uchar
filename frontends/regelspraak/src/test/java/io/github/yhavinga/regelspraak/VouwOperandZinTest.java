package io.github.yhavinga.regelspraak;

import io.github.yhavinga.regelspraak.model.RegelSpraakModel;
import io.github.yhavinga.regelspraak.resolver.Resolver;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The operand checks of the folds and builtins, in the verifier's sentences where it refuses the same document: an
 * ordering fold (WF-038), an arithmetic fold (WF-091's numeric-operand arm), the easter day and the duration (WF-038).
 * Where RegelSpraak's tables are stricter than the verifier — a percentage or a date in a minimum or maximum, numbers
 * in a first or last, a percentage in a sum, any operand of a square root or an absolute value — the refusal keeps its
 * Dutch words. Measured on the frontend before this: every sentence below was Dutch.
 */
class VouwOperandZinTest {

    static List<String> meldingen(String zin) {
        RegelSpraakModel m = Resolver.resolve("""
            Objecttype de Jutter (mv: Jutters) (bezield)
                de telling Numeriek (getal met 2 decimalen);
                de hulp Numeriek (getal met 2 decimalen);
                het aandeel Percentage (getal met 2 decimalen);
                de naam Tekst;
                de vondstdag Datum in dagen;
                het vondstmoment Datum en tijd in millisecondes;
                de uitkomst Numeriek (getal met 2 decimalen);
                de dag Datum in dagen;
                is waardevol kenmerk (bijvoeglijk);

            Objecttype de Vondst (mv: Vondsten)
                de vondstwaarde Numeriek (getal met 2 decimalen);
                de vondstnaam Tekst;
                de vondstdatum Datum in dagen;

            Feittype vondstmelding
                de melder Jutter
                de vondst (mv: vondsten) Vondst
            Eén melder heeft meerdere vondsten

            Parameter de havennaam : Tekst;

            Regel r
                geldig altijd
                    %s

            Parameter de havenprijs : Numeriek (getal met 2 decimalen);

            Parameter de havendag : Datum in dagen;
            """.formatted(zin));
        return m.diagnostics.stream().map(d -> d.severity() + " " + d.line() + " " + d.message()).toList();
    }

    static List<String> fout(String zin) { return List.of("ERROR 26 " + zin); }

    @Test void ordeningsvouwen() {
        String u = "De uitkomst van een Jutter moet berekend worden als ";
        assertEquals(fout("maximale waarde van: operand of the maximum ordering fold is text, must be a number or a date"),
                meldingen(u + "de maximale waarde van zijn telling en zijn naam."));
        assertEquals(fout("maximale waarde van: maximum orders operands of two comparable families, numbers and dates, "
                        + "which share no ordering"),
                meldingen(u + "de maximale waarde van zijn telling en zijn vondstdag."));
        assertEquals(fout("eerste van: operand of the first ordering fold is text, must be a number or a date"),
                meldingen("De dag van een Jutter moet berekend worden als de eerste van zijn vondstdag en zijn naam."));
        // RegelSpraak admits no date in a maximum at all; the verifier refuses only the mix of two precisions
        assertTrue(meldingen(u + "de maximale waarde van zijn vondstdag en zijn vondstmoment.").contains(
                "ERROR 26 maximale waarde van: maximum orders dates of two precisions (day and millisecond), which "
                        + "share no order"));
    }

    @Test void rekenvouwen() {
        // the fold's own type is its operand's, so the assignment to the number target is refused beside it
        String o = "De uitkomst van een Jutter moet berekend worden als ";
        assertTrue(meldingen(o + "de som van de vondstnaam van alle vondsten van de Jutter.").contains(
                "ERROR 26 som van: the sum of 'vondstnaam' on 'Vondst' folds Text values: a total, sum or pro-rata "
                        + "part is arithmetic over a Number, and Text values are not amounts"));
        assertTrue(meldingen(o + "de som van de vondstdatum van alle vondsten van de Jutter.").contains(
                "ERROR 26 som van: the sum of 'vondstdatum' on 'Vondst' folds Date values: a total, sum or pro-rata "
                        + "part is arithmetic over a Number, and Date values are calendar positions"));
        assertTrue(meldingen(o + "het totaal van de vondstnaam van alle vondsten van de Jutter.").contains(
                "ERROR 26 totaal van: the total of 'vondstnaam' on 'Vondst' folds Text values: a total, sum or "
                        + "pro-rata part is arithmetic over a Number, and Text values are not amounts"));
        assertEquals(fout("leegwaarde-alternatief van een sommatie: sum fallback is text, must be numeric"),
                meldingen(o + "de som van de vondstwaarde van alle vondsten van de Jutter of zijn naam als die er niet "
                        + "zijn."));
        // the verifier names a parameter by its name and any other operand by its position
        assertTrue(meldingen(o + "de som van de havennaam.").contains(
                "ERROR 26 som van: the sum of the parameter 'havennaam' folds Text values: a total, sum or pro-rata "
                        + "part is arithmetic over a Number, and Text values are not amounts"));
        assertTrue(meldingen(o + "de som van de eerste paasdag van 2024.").contains(
                "ERROR 26 som van: the sum of its operand 0 folds Date values: a total, sum or pro-rata part is "
                        + "arithmetic over a Number, and Date values are calendar positions"));
    }

    @Test void paasdag_en_tijdsduur() {
        String d = "De dag van een Jutter moet berekend worden als ";
        assertEquals(fout("eerste paasdag van: easterSunday operand is number, must be a whole unitless number"),
                meldingen(d + "de eerste paasdag van zijn telling."));
        assertEquals(fout("eerste paasdag van: easterSunday operand is text, must be a whole unitless number"),
                meldingen(d + "de eerste paasdag van zijn naam."));
        assertTrue(meldingen("Een Jutter is waardevol indien de tijdsduur van zijn vondstdag tot zijn telling in hele "
                        + "dagen groter is dan 5.")
                .contains("ERROR 26 de (absolute) tijdsduur van … tot …: duration operand is number, must be a date"));
    }

    /** WF-037: a value fold without an element type and without a dimension selection is the listed form, and that
     *  form has two or more operands. Measured on the frontend before this: no diagnostic, the document written, and
     *  {@code WF-037 FAIL /rules/rules/0/versions/0/value: value fold with fewer than two operands} for a sum, a total,
     *  a maximum and a last over one parameter and for a sum over one literal. */
    @Test void een_waardevouw_zonder_bereik_met_een_operand() {
        String u = "De uitkomst van een Jutter moet berekend worden als ";
        assertEquals(fout("som van: value fold with fewer than two operands"), meldingen(u + "de som van de havenprijs."));
        assertEquals(fout("totaal van: value fold with fewer than two operands"),
                meldingen(u + "het totaal van de havenprijs."));
        assertEquals(fout("maximale waarde van: value fold with fewer than two operands"),
                meldingen(u + "de maximale waarde van de havenprijs."));
        assertEquals(fout("laatste van: value fold with fewer than two operands"),
                meldingen("De dag van een Jutter moet berekend worden als de laatste van de havendag."));
        assertEquals(fout("som van: value fold with fewer than two operands"), meldingen(u + "de som van 5."));
        // a fold with an element type ranges over instances and is no listed form
        assertEquals(List.of(), meldingen(u + "de som van de vondstwaarde van alle vondsten van de Jutter."));
        assertEquals(List.of(), meldingen(u + "het totaal van zijn telling."));
    }

    @Test void een_gebrek_in_een_voorwaarde_eenmaal_gemeld() {
        // the condition's type check builds its operands a second time; the verifier reports each defect once, and
        // the same defect on both sides of one comparison twice, at two pointers
        assertEquals(fout("maximale waarde van: operand of the maximum ordering fold is text, must be a number or a date"),
                meldingen("Een Jutter is waardevol indien de maximale waarde van zijn telling en zijn naam groter is dan 5."));
        String duur = "de (absolute) tijdsduur van … tot …: duration operand is number, must be a date";
        assertEquals(fout(duur), meldingen("Een Jutter is waardevol indien de tijdsduur van zijn vondstdag tot zijn "
                + "telling in hele dagen groter is dan 5."));
        assertEquals(List.of("ERROR 26 " + duur, "ERROR 26 " + duur), meldingen("Een Jutter is waardevol indien de "
                + "tijdsduur van zijn vondstdag tot zijn telling in hele dagen groter is dan de tijdsduur van zijn "
                + "vondstdag tot zijn hulp in hele dagen."));
    }

    @Test void wat_de_verifier_toelaat_houdt_de_eigen_zin() {
        String u = "De uitkomst van een Jutter moet berekend worden als ";
        assertEquals(fout("maximale waarde van vereist numerieke operanden, kreeg Percentage"),
                meldingen(u + "de maximale waarde van zijn telling en zijn aandeel."));
        assertEquals(fout("eerste van vereist datumexpressies, kreeg Numeriek"),
                meldingen(u + "de eerste van zijn telling en zijn hulp."));
        assertEquals(fout("leegwaarde-alternatief van een sommatie moet numeriek zijn, kreeg Percentage (§5.8.2)"),
                meldingen(u + "de som van de vondstwaarde van alle vondsten van de Jutter of zijn aandeel als die er "
                        + "niet zijn."));
        assertEquals(fout("de wortel van vereist numerieke operanden, kreeg Tekst"),
                meldingen(u + "de wortel van zijn naam naar beneden afgerond op 2 decimalen."));
    }
}
