package dev.uchar.regelspraak;

import dev.uchar.regelspraak.model.RegelSpraakModel;
import dev.uchar.regelspraak.resolver.Resolver;
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
class FoldOperandSentenceTest {

    static List<String> meldingen(String sentence) {
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
            """.formatted(sentence));
        return m.diagnostics.stream().map(d -> d.severity() + " " + d.line() + " " + d.message()).toList();
    }

    static List<String> error(String sentence) { return List.of("ERROR 26 " + sentence); }

    @Test void orderingFolds() {
        String u = "De uitkomst van een Jutter moet berekend worden als ";
        assertEquals(error("maximale waarde van: operand of the maximum ordering fold is text, must be a number or a date"),
                meldingen(u + "de maximale waarde van zijn telling en zijn naam."));
        assertEquals(error("maximale waarde van: maximum orders operands of two comparable families, numbers and dates, "
                        + "which share no ordering"),
                meldingen(u + "de maximale waarde van zijn telling en zijn vondstdag."));
        assertEquals(error("eerste van: operand of the first ordering fold is text, must be a number or a date"),
                meldingen("De dag van een Jutter moet berekend worden als de eerste van zijn vondstdag en zijn naam."));
        // RegelSpraak admits no date in a maximum at all; the verifier refuses only the mix of two precisions
        assertTrue(meldingen(u + "de maximale waarde van zijn vondstdag en zijn vondstmoment.").contains(
                "ERROR 26 maximale waarde van: maximum orders dates of two precisions (day and millisecond), which "
                        + "share no order"));
    }

    @Test void arithmeticFolds() {
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
        assertEquals(error("leegwaarde-alternatief van een sommatie: sum fallback is text, must be numeric"),
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

    @Test void easter_sunday_and_duration() {
        String d = "De dag van een Jutter moet berekend worden als ";
        assertEquals(error("eerste paasdag van: easterSunday operand is number, must be a whole unitless number"),
                meldingen(d + "de eerste paasdag van zijn telling."));
        assertEquals(error("eerste paasdag van: easterSunday operand is text, must be a whole unitless number"),
                meldingen(d + "de eerste paasdag van zijn naam."));
        assertTrue(meldingen("Een Jutter is waardevol indien de tijdsduur van zijn vondstdag tot zijn telling in hele "
                        + "dagen groter is dan 5.")
                .contains("ERROR 26 de (absolute) tijdsduur van … tot …: duration operand is number, must be a date"));
    }

    /** WF-037: a value fold without an element type and without a dimension selection is the listed form, and that
     *  form has two or more operands. Measured on the frontend before this: no diagnostic, the document written, and
     *  {@code WF-037 FAIL /rules/rules/0/versions/0/value: value fold with fewer than two operands} for a sum, a total,
     *  a maximum and a last over one parameter and for a sum over one literal. */
    @Test void a_value_fold_without_range_with_an_operand() {
        String u = "De uitkomst van een Jutter moet berekend worden als ";
        assertEquals(error("som van: value fold with fewer than two operands"), meldingen(u + "de som van de havenprijs."));
        assertEquals(error("totaal van: value fold with fewer than two operands"),
                meldingen(u + "het totaal van de havenprijs."));
        assertEquals(error("maximale waarde van: value fold with fewer than two operands"),
                meldingen(u + "de maximale waarde van de havenprijs."));
        assertEquals(error("laatste van: value fold with fewer than two operands"),
                meldingen("De dag van een Jutter moet berekend worden als de laatste van de havendag."));
        assertEquals(error("som van: value fold with fewer than two operands"), meldingen(u + "de som van 5."));
        // a fold with an element type ranges over instances and is no listed form
        assertEquals(List.of(), meldingen(u + "de som van de vondstwaarde van alle vondsten van de Jutter."));
        assertEquals(List.of(), meldingen(u + "het totaal van zijn telling."));
    }

    @Test void a_defect_in_a_condition_is_reported_once() {
        // the condition's type check builds its operands a second time; the verifier reports each defect once, and
        // the same defect on both sides of one comparison twice, at two pointers
        assertEquals(error("maximale waarde van: operand of the maximum ordering fold is text, must be a number or a date"),
                meldingen("Een Jutter is waardevol indien de maximale waarde van zijn telling en zijn naam groter is dan 5."));
        String duration = "de (absolute) tijdsduur van … tot …: duration operand is number, must be a date";
        assertEquals(error(duration), meldingen("Een Jutter is waardevol indien de tijdsduur van zijn vondstdag tot zijn "
                + "telling in hele dagen groter is dan 5."));
        assertEquals(List.of("ERROR 26 " + duration, "ERROR 26 " + duration), meldingen("Een Jutter is waardevol indien de "
                + "tijdsduur van zijn vondstdag tot zijn telling in hele dagen groter is dan de tijdsduur van zijn "
                + "vondstdag tot zijn hulp in hele dagen."));
    }

    /** WF-038 on the counted unit of a duration, the unit of its node type: a recognised time unit or a declared day
     *  kind, and a sub-day unit only between millisecond-precision dates. Measured on the frontend before this: no
     *  diagnostic, the document written, and {@code WF-038 FAIL …: duration: the counted unit 'kopjes' is neither a
     *  recognised time unit nor a declared day kind} and {@code … sub-day duration unit on day-precision operands
     *  (millisecond precision required)}. */
    @Test void the_unit_of_a_duration() {
        String w = "Een Jutter is waardevol indien de tijdsduur van %s in hele %s groter is dan 5.";
        assertEquals(error("de (absolute) tijdsduur van … tot …: duration: the counted unit 'kopjes' is neither a "
                        + "recognised time unit nor a declared day kind"),
                meldingen(w.formatted("zijn vondstdag tot zijn dag", "kopjes")));
        assertEquals(error("de (absolute) tijdsduur van … tot …: sub-day duration unit on day-precision operands "
                        + "(millisecond precision required)"),
                meldingen(w.formatted("zijn vondstdag tot zijn dag", "uren")));
        assertEquals(List.of(), meldingen(w.formatted("zijn vondstdag tot zijn dag", "dagen")));
        assertEquals(List.of(), meldingen(w.formatted("zijn vondstmoment tot zijn vondstmoment", "uren")));
        // the verifier names the builtin's own kind: the absolute duration speaks its own sentence
        assertEquals(error("de (absolute) tijdsduur van … tot …: sub-day absoluteDuration unit on day-precision operands "
                        + "(millisecond precision required)"),
                meldingen("Een Jutter is waardevol indien de absolute tijdsduur van zijn vondstdag tot zijn dag in hele "
                        + "uren groter is dan 5."));
    }

    @Test void what_the_verifier_admits_keeps_its_own_sentence() {
        String u = "De uitkomst van een Jutter moet berekend worden als ";
        assertEquals(error("maximale waarde van vereist numerieke operanden, kreeg Percentage"),
                meldingen(u + "de maximale waarde van zijn telling en zijn aandeel."));
        assertEquals(error("eerste van vereist datumexpressies, kreeg Numeriek"),
                meldingen(u + "de eerste van zijn telling en zijn hulp."));
        assertEquals(error("leegwaarde-alternatief van een sommatie moet numeriek zijn, kreeg Percentage (§5.8.2)"),
                meldingen(u + "de som van de vondstwaarde van alle vondsten van de Jutter of zijn aandeel als die er "
                        + "niet zijn."));
        // a percentage is a number to the verifier, which admits it under a square root and an absolute value
        assertEquals(error("de wortel van vereist numerieke operanden, kreeg Percentage"),
                meldingen(u + "de wortel van zijn aandeel naar beneden afgerond op 2 decimalen."));
    }

    @Test void square_root_and_absolute_value() {
        String u = "De uitkomst van een Jutter moet berekend worden als ";
        assertEquals(error("de wortel van: squareRoot operand is text, must be a number"),
                meldingen(u + "de wortel van zijn naam naar beneden afgerond op 2 decimalen."));
        assertEquals(error("de absolute waarde van: absoluteValue operand is date-day, must be a number"),
                meldingen("De dag van een Jutter moet berekend worden als de absolute waarde van (zijn vondstdag)."));
    }
}
