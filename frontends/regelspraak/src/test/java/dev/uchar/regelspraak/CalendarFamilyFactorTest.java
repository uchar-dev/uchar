package dev.uchar.regelspraak;

import dev.uchar.regelspraak.model.RegelSpraakModel;
import dev.uchar.regelspraak.resolver.Resolver;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The calendar arm of WF-009 (RUNTIME-SEMANTICS ch. 3.6): between the day family (dag, week) and the month family (maand,
 * kwartaal, jaar) no conversion factor is fixed, since a month or a year has no fixed number of days, so a unit of the one
 * declared as a multiple of a unit of the other is RGS-373. The unit is compared with the end of its chain of
 * declarations; a sub-day unit and a unit outside the calendar table belong to no family. The first refused source is the
 * RegelSpraak of the verifier's negative {@code corpus/negative/wf009-calendar-family-factor}; every source here gets the
 * verdict the verifier gives its IR.
 */
class CalendarFamilyFactorTest {

    private static final String DAY = "de dagfamilie (dag, week)";
    private static final String MONTH = "de maandfamilie (maand, kwartaal, jaar)";

    /** The negative's document; {@code %s} is the conversion of the year. */
    private static final String DOCUMENT = """
        Eenheidsysteem Munt
            de euro (mv: euros) EUR €

        Eenheidsysteem Tijd
            de dag (mv: dagen) dg
            het jaar (mv: jaren) jr%s

        Parameter de verzuimrentevoet : Numeriek (getal met 2 decimalen) met eenheid 1/jr voor elke maand;

        Objecttype de Afdracht (mv: Afdrachten)
            het afdrachtbedrag Numeriek (geheel getal) met eenheid €;
            de vervaldatum Datum in dagen;
            de betaaldatum Datum in dagen;
            is rentedragend kenmerk (bijvoeglijk) voor elke dag;
            de dagrente Numeriek (getal met 2 decimalen) met eenheid €/jr voor elke dag;
            de verzuimrente Numeriek (getal met 2 decimalen) met eenheid €;

        Regel dagrente
            geldig altijd
                De dagrente van een Afdracht moet berekend worden als de verzuimrentevoet maal zijn afdrachtbedrag.

        Regel verzuimrente
            geldig altijd
                De verzuimrente van een Afdracht moet berekend worden als (het totaal van zijn dagrente gedurende de tijd \
        dat hij rentedragend is) rekenkundig afgerond op 2 decimalen.
        """;

    /** The codes and parameters of the errors of {@code source}. */
    private static List<String> errors(String source) {
        RegelSpraakModel m = Resolver.resolve(source);
        return m.errors().stream().map(d -> d.code() + " " + d.params()).toList();
    }

    private static String refusal(String unit, String unitFamily, String canonical, String canonicalFamily) {
        Map<String, String> params = new java.util.LinkedHashMap<>();
        params.put("unit", unit);
        params.put("unitFamily", unitFamily);
        params.put("canonical", canonical);
        params.put("canonicalFamily", canonicalFamily);
        params.put("unitSystem", "Tijd");
        return "RGS-373 " + params;
    }

    private static String units(String... declarations) {
        return "Eenheidsysteem Tijd\n" + String.join("", java.util.Arrays.stream(declarations).map(d -> "    " + d + "\n").toList());
    }

    // wf009-calendar-family-factor
    @Test void a_year_declared_in_days_is_refused() {
        assertEquals(List.of(refusal("jr", MONTH, "dg", DAY)), errors(DOCUMENT.formatted(" = 365 dg")));
        // the anchor anchor/jutterswet-verzuimrente-dagen: the calendar integrates across the families
        assertEquals(List.of(), errors(DOCUMENT.formatted("")));
    }

    @Test void a_week_declared_in_months_is_refused() {
        assertEquals(List.of(refusal("wk", DAY, "mnd", MONTH)),
                errors(units("de maand (mv: maanden) mnd", "de week (mv: weken) wk = 1/4 mnd")));
    }

    @Test void the_unit_is_compared_with_the_end_of_its_chain() {
        // the kwartaal is three months, within its family, but the month is thirty days
        assertEquals(List.of(refusal("mnd", MONTH, "dg", DAY), refusal("kw", MONTH, "dg", DAY)),
                errors(units("de dag (mv: dagen) dg", "de maand (mv: maanden) mnd = 30 dg", "het kwartaal (mv: kwartalen) kw = 3 mnd")));
    }

    @Test void a_factor_within_a_family_or_over_a_unit_of_no_family_stands() {
        assertEquals(List.of(), errors(units("de maand (mv: maanden) mnd", "het jaar (mv: jaren) jr = 12 mnd",
                "het kwartaal (mv: kwartalen) kw = 3 mnd", "de dag (mv: dagen) dg", "de week (mv: weken) wk = 7 dg")));
        // the hour belongs to no family, so a day and a year in hours state no factor between the families
        assertEquals(List.of(), errors(units("het uur (mv: uren) u", "de dag (mv: dagen) dg = 24 u", "het jaar (mv: jaren) jr = 8760 u")));
        // a unit outside the calendar table belongs to no family either
        assertEquals(List.of(), errors(units("de dag (mv: dagen) dg", "de bouwmaand (mv: bouwmaanden) bm = 30 dg")));
    }
}
