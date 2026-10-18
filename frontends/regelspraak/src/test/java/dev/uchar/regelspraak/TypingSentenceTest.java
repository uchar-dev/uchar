package dev.uchar.regelspraak;

import dev.uchar.regelspraak.model.RegelSpraakModel;
import dev.uchar.regelspraak.resolver.Resolver;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The sister of {@link UnitSentenceTest} for the other typing refusals the verifier shares: where the resolver refuses
 * a document the verifier refuses too, the message after the Dutch context prefix is the verifier's sentence, with
 * the kind words the verifier writes. Where RegelSpraak's typing refuses what the verifier admits, the refusal keeps
 * its Dutch words (grammar/DIVERGENCES.md, "producer refusals without a reader sentence"). Measured on the frontend
 * before this: every sentence below was Dutch.
 */
class TypingSentenceTest {

    /** The diagnostics of one rule sentence over a model with one attribute of each datatype the typing reads. */
    static List<String> meldingen(String sentence) {
        RegelSpraakModel m = Resolver.resolve("""
            Eenheidsysteem Munt
                de euro (mv: euros) EUR €

            Dagsoort de rustdag (mv: rustdagen);

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

            Objecttype de Vondst (mv: Vondsten)
                de vondstsaldo Numeriek (getal met 2 decimalen) voor elk jaar;
                de voorraad Numeriek (niet-negatief geheel getal);
                de stukprijs Numeriek (positief geheel getal);
                de korting Numeriek (negatief geheel getal);
                is gewogen kenmerk (bijvoeglijk);

            Feittype vondstmelding
                de melder Jutter
                de vondst (mv: vondsten) Vondst
            Eén melder heeft meerdere vondsten
            """.formatted(sentence));
        return m.diagnostics.stream().map(d -> d.severity() + " " + d.line() + " " + d.message()).toList();
    }

    static List<String> error(String sentence) { return List.of("ERROR 30 " + sentence); }

    @Test void window_bound_numeral_full_period_and_characteristic_value() {
        assertEquals(error("periodevergelijking: window bound is text, must be a date"),
                meldingen("De uitkomst van een Jutter moet gesteld worden op 5 vanaf zijn naam."));
        String quantifier = "Een Jutter is waardevol indien hij aan ten minste %s van de volgende voorwaarden voldoet:\n"
                + "                    - zijn telling is groter dan 1\n"
                + "                    - zijn uitkomst is groter dan 2.";
        assertEquals(error("telwoord van een kwantificatie: counting quantifier with threshold 0 < 1"),
                meldingen(quantifier.formatted("0")));
        // a decimal telwoord reaches the wire as 0, a number the author did not write
        assertEquals(error("telwoord van een kwantificatie moet een positief geheel getal (>= 1) zijn"),
                meldingen(quantifier.formatted("1,5")));
        assertEquals(List.of("ERROR 30 de check op volledige periode (gedurende de/het gehele jaar/maand): wholePeriod "
                        + "qualifier on a non-time-dependent operand"),
                meldingen("Een Jutter is waardevol indien zijn telling gedurende het gehele jaar groter is dan 5."));
        assertTrue(meldingen("Een Jutter is waardevol indien hij gedurende het gehele jaar waardevol is.").contains(
                "ERROR 30 de check op volledige periode (gedurende de/het gehele jaar/maand): wholePeriod qualifier on a "
                        + "non-time-dependent characteristic"));
        assertEquals(error("kenmerkwaardetoekenning 'gewogen': characteristic initialisation's value must be a "
                        + "boolean-typed expression"),
                meldingen("Een Jutter heeft een vondst met gewogen gelijk aan 5."));
    }

    /** WF-043 counts an operand as time-dependent only where it reads a temporal attribute or parameter itself, not
     *  through arithmetic or a rounding, which §8.2 admits. Measured on the frontend before this: no diagnostic, the
     *  document written, and {@code WF-043 FAIL /rules/rules/0/versions/0/condition: wholePeriod qualifier on a
     *  non-time-dependent operand}. */
    @Test void full_period_over_a_compound_operand() {
        String sentence = "ERROR 30 de check op volledige periode (gedurende de/het gehele jaar/maand): wholePeriod qualifier "
                + "on a non-time-dependent operand";
        assertEquals(List.of(sentence), meldingen("Een Vondst is gewogen indien zijn vondstsaldo plus 1 gedurende het gehele "
                + "jaar groter is dan 5."));
        assertEquals(List.of(sentence), meldingen("Een Vondst is gewogen indien zijn vondstsaldo naar beneden afgerond op 0 "
                + "decimalen gedurende het gehele jaar groter is dan 5."));
        assertEquals(List.of(), meldingen("Een Vondst is gewogen indien zijn vondstsaldo gedurende het gehele jaar "
                + "groter is dan 5."));
    }

    /** WF-039's target clause: a number literal written into a sign-restricted target, its sign read off its value
     *  as the verifier reads it, zero neither negative nor positive; the sentence names the written lexeme. Measured
     *  on the frontend before this: a Dutch refusal ({@code waarde -5 voldoet niet aan de teken-beperking …}). */
    @Test void sign_restriction_of_the_target() {
        String v = "De %s van een Vondst moet gesteld worden op %s.";
        assertEquals(error("teken-beperking: negative literal '-5' is written into a target under a non-negative sign "
                + "restriction"), meldingen(v.formatted("voorraad", "-5")));
        assertEquals(List.of(), meldingen(v.formatted("voorraad", "-0")));
        assertEquals(error("teken-beperking: literal '-0' is written into a target under a negative sign restriction"),
                meldingen(v.formatted("korting", "-0")));
        // the lexeme the wire writes for the written "0/5", a fraction that reduces to the whole number 0
        assertEquals(error("teken-beperking: literal '0' is written into a target under a positive sign restriction"),
                meldingen(v.formatted("stukprijs", "0/5")));
        assertEquals(error("teken-beperking: literal '0' is written into a target under a positive sign restriction"),
                meldingen(v.formatted("stukprijs", "0")));
        assertEquals(error("teken-beperking: literal '5' is written into a target under a negative sign restriction"),
                meldingen(v.formatted("korting", "5")));
        assertEquals(List.of(), meldingen(v.formatted("korting", "-5")));
    }

    /** WF-035: a sub-day unit shifts a date by part of a day, which a day-precision date cannot hold. Measured on the
     *  frontend before this: no diagnostic, the document written, and {@code WF-035 FAIL …/value: sub-day time unit
     *  on a day-precision date (millisecond precision required)}. */
    @Test void sub_day_unit_on_a_day_date() {
        String d = "De dag van een Jutter moet berekend worden als ";
        assertEquals(error("plus: sub-day time unit on a day-precision date (millisecond precision required)"),
                meldingen(d + "zijn vondstdag plus 3 uur."));
        assertEquals(error("min: sub-day time unit on a day-precision date (millisecond precision required)"),
                meldingen(d + "zijn vondstdag min 90 minuten."));
        assertEquals(List.of(), meldingen(d + "zijn vondstdag plus 3 dagen."));
        assertEquals(List.of(), meldingen("Een Jutter is waardevol indien zijn vondstmoment plus 3 uur later is dan "
                + "zijn vondstmoment."));
    }

    @Test void datePrecision() {
        assertEquals(error("voorwaarde: gelijk: equality across date precisions"),
                meldingen("Een Jutter is waardevol indien zijn vondstdag gelijk is aan zijn vondstmoment."));
        assertEquals(error("gelijkstelling: toekenning: a date-ms value is assigned to a date-day target"),
                meldingen("De dag van een Jutter moet gesteld worden op zijn vondstmoment."));
        assertEquals(error("eerste van: first orders dates of two precisions (day and millisecond), which share no order"),
                meldingen("De dag van een Jutter moet gesteld worden op de eerste van zijn vondstdag en zijn vondstmoment."));
        assertEquals(error("een tijd-component (uur/minuut/seconde/milliseconde uit): sub-day extractor operand is "
                        + "date-day, must be a millisecond-precision date"),
                meldingen("De uitkomst van een Jutter moet gesteld worden op het uur uit zijn vondstdag."));
        // the duration's own result also meets the unitless target, so only the precision line is pinned here
        assertTrue(meldingen("De uitkomst van een Jutter moet gesteld worden op de tijdsduur van zijn vondstdag tot "
                        + "zijn vondstmoment in hele dagen.")
                .contains("ERROR 30 de (absolute) tijdsduur van … tot …: duration operands mix date precisions (both "
                        + "must share one precision)"));
    }

    @Test void family_on_assigning_and_equality() {
        assertEquals(error("gelijkstelling: toekenning: a text value is assigned to a number target"),
                meldingen("De uitkomst van een Jutter moet gesteld worden op zijn naam."));
        assertEquals(error("gelijkstelling: toekenning: a date-day value is assigned to a number target"),
                meldingen("De uitkomst van een Jutter moet gesteld worden op zijn vondstdag."));
        assertEquals(error("voorwaarde: gelijk: equality across families: text vs number"),
                meldingen("Een Jutter is waardevol indien zijn naam gelijk is aan zijn telling."));
        // a percentage is a number with the unit '%' to the verifier: across a unitless number it refuses the units
        assertEquals(error("gelijkstelling: toekenning: assignment across incompatible units"),
                meldingen("Het aandeel van een Jutter moet gesteld worden op zijn telling."));
        assertEquals(error("voorwaarde: gelijk: equality operands have incompatible units"),
                meldingen("Een Jutter is waardevol indien zijn aandeel gelijk is aan zijn telling."));
    }

    @Test void number_beside_percentage_with_a_literal_keeps_its_own_sentence() {
        // the verifier admits both (the unitless literal adopts '%'), so it has no sentence for them
        assertEquals(error("gelijkstelling: kan een waarde van type Numeriek niet toekennen aan een Percentage"),
                meldingen("Het aandeel van een Jutter moet gesteld worden op 5."));
        assertEquals(error("voorwaarde: gelijk/ongelijk vereist operanden van hetzelfde datatype, kreeg Percentage en "
                        + "Numeriek"),
                meldingen("Een Jutter is waardevol indien zijn aandeel gelijk is aan 5."));
    }

    @Test void choiceTypeDomain() {
        assertEquals(error("voorwaarde: gelijk: equality across different enumeration domains"),
                meldingen("Een Jutter is waardevol indien zijn staat gelijk is aan zijn weer."));
        assertEquals(error("gelijkstelling: toekenning: a value of the enumeration domain 'Weerklasse' is assigned to "
                        + "a target of the enumeration domain 'Keurklasse'"),
                meldingen("De klasse van een Jutter moet gesteld worden op zijn weer."));
    }

    @Test void non_numeric_operands_at_computing() {
        String r = "De uitkomst van een Jutter moet berekend worden als ";
        assertEquals(error("verminderd met: operand of a numeric operator is text"),
                meldingen(r + "zijn telling verminderd met zijn naam."));
        assertEquals(error("maal: operand of a numeric operator is text"),
                meldingen(r + "zijn telling maal zijn naam naar beneden afgerond op 2 decimalen."));
        assertEquals(error("tot de macht: operand of a numeric operator is text"),
                meldingen(r + "zijn telling tot de macht zijn naam naar beneden afgerond op 2 decimalen."));
        // a percentage carries '%': the verifier refuses it under a power as a unit
        assertEquals(error("tot de macht: power operands must be unitless"),
                meldingen(r + "zijn telling tot de macht zijn aandeel naar beneden afgerond op 2 decimalen."));
    }

    @Test void dateArithmetic() {
        String d = "De dag van een Jutter moet berekend worden als ";
        assertEquals(error("plus: date ± date is ill-formed (date arithmetic takes a date and a duration)"),
                meldingen(d + "zijn vondstdag plus zijn vondstdag."));
        assertEquals(error("plus: date ± duration: the duration operand is text, must be a number carrying a time unit"),
                meldingen(d + "zijn vondstdag plus zijn naam."));
        assertEquals(error("plus: date ± duration: the duration operand carries no time unit"),
                meldingen(d + "zijn vondstdag plus zijn telling."));
        assertEquals(error("plus: date ± duration: the duration unit 'EUR' is neither a recognised time unit nor a "
                        + "declared day kind"),
                meldingen(d + "zijn vondstdag plus zijn waarde."));
        String u = "De uitkomst van een Jutter moet berekend worden als ";
        assertTrue(meldingen(u + "zijn telling min zijn vondstdag.").contains(
                "ERROR 30 min: duration − date is ill-formed (subtraction admits the date on the left only)"));
        assertTrue(meldingen(u + "zijn telling plus zijn vondstdag.").contains(
                "ERROR 30 plus: duration + date is ill-formed (the date stands on the left; date arithmetic does not "
                        + "commute)"));
    }

    @Test void non_numeric_operands_at_ordering() {
        assertEquals(error("voorwaarde: vergelijking: ordering operand is text"),
                meldingen("Een Jutter is waardevol indien zijn telling groter is dan zijn naam."));
        assertEquals(error("voorwaarde: vergelijking: date-ordering operand is number"),
                meldingen("Een Jutter is waardevol indien zijn vondstdag eerder is dan zijn telling."));
    }

    @Test void computing_that_the_verifier_admits_keeps_its_own_sentence() {
        // the verifier admits a plus over two non-dates and a product with a percentage: no sentence to carry
        String r = "De uitkomst van een Jutter moet berekend worden als ";
        assertEquals(error("operator 'plus' vereist numerieke operanden, kreeg Tekst en Numeriek"),
                meldingen(r + "zijn naam plus zijn telling."));
        assertEquals(error("maal vereist numerieke operanden, kreeg Numeriek en Percentage"),
                meldingen(r + "zijn telling maal zijn aandeel naar beneden afgerond op 2 decimalen."));
    }

    @Test void predicates() {
        assertEquals(error("voorwaarde: elfproef: elevenTest operand is date-day, must be text or a whole unitless number"),
                meldingen("Een Jutter is waardevol indien zijn vondstdag aan de elfproef voldoet."));
        assertEquals(error("voorwaarde: numeriek met exact: exactDigits operand is number, must be text"),
                meldingen("Een Jutter is waardevol indien zijn telling numeriek is met exact 4 cijfers."));
        assertEquals(error("voorwaarde: dagsoort: day-kind predicate operand is number, must be a date"),
                meldingen("Een Jutter is waardevol indien zijn telling een rustdag is."));
    }

    @Test void a_label_outside_the_domain() {
        assertEquals(error("enumeration label 'zwart' is not in the declared domain"),
                meldingen("De klasse van een Jutter moet gesteld worden op 'zwart'."));
        assertEquals(error("enumeration label 'zwart' is not in the declared domain"),
                meldingen("Een Jutter is waardevol indien zijn staat gelijk is aan 'zwart'."));
    }

    @Test void power_with_a_unit() {
        assertEquals(error("tot de macht: power operands must be unitless"),
                meldingen("De uitkomst van een Jutter moet berekend worden als zijn waarde tot de macht 2 naar beneden afgerond op 2 decimalen."));
    }
}
