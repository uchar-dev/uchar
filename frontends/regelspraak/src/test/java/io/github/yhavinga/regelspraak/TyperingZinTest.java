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
            """.formatted(zin));
        return m.diagnostics.stream().map(d -> d.severity() + " " + d.line() + " " + d.message()).toList();
    }

    static List<String> fout(String zin) { return List.of("ERROR 30 " + zin); }

    @Test void datumprecisie() {
        assertEquals(fout("voorwaarde: gelijk: equality across date precisions"),
                meldingen("Een Jutter is waardevol indien zijn vondstdag gelijk is aan zijn vondstmoment."));
        assertEquals(fout("gelijkstelling: toekenning: a date-ms value is assigned to a date-day target"),
                meldingen("De dag van een Jutter moet gesteld worden op zijn vondstmoment."));
        assertEquals(fout("eerste van: first orders dates of two precisions (day and millisecond), which share no order"),
                meldingen("De dag van een Jutter moet gesteld worden op de eerste van zijn vondstdag en zijn vondstmoment."));
        assertEquals(fout("een tijd-component (uur/minuut/seconde/milliseconde uit): sub-day extractor operand is "
                        + "date-day, must be a millisecond-precision date"),
                meldingen("De uitkomst van een Jutter moet gesteld worden op het uur uit zijn vondstdag."));
        // the duration's own result also meets the unitless target, so only the precision line is pinned here
        assertTrue(meldingen("De uitkomst van een Jutter moet gesteld worden op de tijdsduur van zijn vondstdag tot "
                        + "zijn vondstmoment in hele dagen.")
                .contains("ERROR 30 de (absolute) tijdsduur van … tot …: duration operands mix date precisions (both "
                        + "must share one precision)"));
    }

    @Test void familie_bij_toekennen_en_gelijkheid() {
        assertEquals(fout("gelijkstelling: toekenning: a text value is assigned to a number target"),
                meldingen("De uitkomst van een Jutter moet gesteld worden op zijn naam."));
        assertEquals(fout("gelijkstelling: toekenning: a date-day value is assigned to a number target"),
                meldingen("De uitkomst van een Jutter moet gesteld worden op zijn vondstdag."));
        assertEquals(fout("voorwaarde: gelijk: equality across families: text vs number"),
                meldingen("Een Jutter is waardevol indien zijn naam gelijk is aan zijn telling."));
        // a percentage is a number with the unit '%' to the verifier: across a unitless number it refuses the units
        assertEquals(fout("gelijkstelling: toekenning: assignment across incompatible units"),
                meldingen("Het aandeel van een Jutter moet gesteld worden op zijn telling."));
        assertEquals(fout("voorwaarde: gelijk: equality operands have incompatible units"),
                meldingen("Een Jutter is waardevol indien zijn aandeel gelijk is aan zijn telling."));
    }

    @Test void numeriek_naast_percentage_met_een_literal_houdt_de_eigen_zin() {
        // the verifier admits both (the unitless literal adopts '%'), so it has no sentence for them
        assertEquals(fout("gelijkstelling: kan een waarde van type Numeriek niet toekennen aan een Percentage"),
                meldingen("Het aandeel van een Jutter moet gesteld worden op 5."));
        assertEquals(fout("voorwaarde: gelijk/ongelijk vereist operanden van hetzelfde datatype, kreeg Percentage en "
                        + "Numeriek"),
                meldingen("Een Jutter is waardevol indien zijn aandeel gelijk is aan 5."));
    }

    @Test void keuzetypedomein() {
        assertEquals(fout("voorwaarde: gelijk: equality across different enumeration domains"),
                meldingen("Een Jutter is waardevol indien zijn staat gelijk is aan zijn weer."));
        assertEquals(fout("gelijkstelling: toekenning: a value of the enumeration domain 'Weerklasse' is assigned to "
                        + "a target of the enumeration domain 'Keurklasse'"),
                meldingen("De klasse van een Jutter moet gesteld worden op zijn weer."));
    }

    @Test void niet_numerieke_operanden_bij_rekenen() {
        String r = "De uitkomst van een Jutter moet berekend worden als ";
        assertEquals(fout("verminderd met: operand of a numeric operator is text"),
                meldingen(r + "zijn telling verminderd met zijn naam."));
        assertEquals(fout("maal: operand of a numeric operator is text"),
                meldingen(r + "zijn telling maal zijn naam naar beneden afgerond op 2 decimalen."));
        assertEquals(fout("tot de macht: operand of a numeric operator is text"),
                meldingen(r + "zijn telling tot de macht zijn naam naar beneden afgerond op 2 decimalen."));
        // a percentage carries '%': the verifier refuses it under a power as a unit
        assertEquals(fout("tot de macht: power operands must be unitless"),
                meldingen(r + "zijn telling tot de macht zijn aandeel naar beneden afgerond op 2 decimalen."));
    }

    @Test void datumrekenkunde() {
        String d = "De dag van een Jutter moet berekend worden als ";
        assertEquals(fout("plus: date ± date is ill-formed (date arithmetic takes a date and a duration)"),
                meldingen(d + "zijn vondstdag plus zijn vondstdag."));
        assertEquals(fout("plus: date ± duration: the duration operand is text, must be a number carrying a time unit"),
                meldingen(d + "zijn vondstdag plus zijn naam."));
        assertEquals(fout("plus: date ± duration: the duration operand carries no time unit"),
                meldingen(d + "zijn vondstdag plus zijn telling."));
        assertEquals(fout("plus: 'EUR' is not a recognised time unit or declared day kind"),
                meldingen(d + "zijn vondstdag plus zijn waarde."));
        String u = "De uitkomst van een Jutter moet berekend worden als ";
        assertTrue(meldingen(u + "zijn telling min zijn vondstdag.").contains(
                "ERROR 30 min: duration − date is ill-formed (subtraction admits the date on the left only)"));
    }

    @Test void niet_numerieke_operanden_bij_ordenen() {
        assertEquals(fout("voorwaarde: vergelijking: ordering operand is text"),
                meldingen("Een Jutter is waardevol indien zijn telling groter is dan zijn naam."));
        assertEquals(fout("voorwaarde: vergelijking: date-ordering operand is number"),
                meldingen("Een Jutter is waardevol indien zijn vondstdag eerder is dan zijn telling."));
    }

    @Test void rekenen_dat_de_verifier_toelaat_houdt_de_eigen_zin() {
        // the verifier admits a plus over two non-dates and a product with a percentage: no sentence to carry
        String r = "De uitkomst van een Jutter moet berekend worden als ";
        assertEquals(fout("operator 'plus' vereist numerieke operanden, kreeg Tekst en Numeriek"),
                meldingen(r + "zijn naam plus zijn telling."));
        assertEquals(fout("maal vereist numerieke operanden, kreeg Numeriek en Percentage"),
                meldingen(r + "zijn telling maal zijn aandeel naar beneden afgerond op 2 decimalen."));
    }

    @Test void predicaten() {
        assertEquals(fout("voorwaarde: elfproef: elevenTest operand is date-day, must be text or a whole unitless number"),
                meldingen("Een Jutter is waardevol indien zijn vondstdag aan de elfproef voldoet."));
        assertEquals(fout("voorwaarde: numeriek met exact: exactDigits operand is number, must be text"),
                meldingen("Een Jutter is waardevol indien zijn telling numeriek is met exact 4 cijfers."));
        assertEquals(fout("voorwaarde: dagsoort: day-kind predicate operand is number, must be a date"),
                meldingen("Een Jutter is waardevol indien zijn telling een rustdag is."));
    }

    @Test void een_label_buiten_het_domein() {
        assertEquals(fout("enumeration label 'zwart' is not in the declared domain"),
                meldingen("De klasse van een Jutter moet gesteld worden op 'zwart'."));
        assertEquals(fout("enumeration label 'zwart' is not in the declared domain"),
                meldingen("Een Jutter is waardevol indien zijn staat gelijk is aan 'zwart'."));
    }

    @Test void tot_de_macht_met_een_eenheid() {
        assertEquals(fout("tot de macht: power operands must be unitless"),
                meldingen("De uitkomst van een Jutter moet berekend worden als zijn waarde tot de macht 2 naar beneden afgerond op 2 decimalen."));
    }
}
