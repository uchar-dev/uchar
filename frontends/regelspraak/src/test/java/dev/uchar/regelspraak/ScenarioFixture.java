package dev.uchar.regelspraak;

import dev.uchar.regelspraak.model.RegelSpraakModel.Diagnostic;
import dev.uchar.regelspraak.resolver.ScenarioResolver;
import dev.uchar.regelspraak.resolver.ScenarioResolver.ScenarioBundle;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * The Jutterswet model the scenariotoetsen suites resolve their scenarios against: every value form the case format
 * carries (a text, a number with and without a unit, a date, a date-time, a percentage, an enumeration value, a
 * dimensioned attribute), a time-dependent attribute, characteristic and parameter, an adjectival and a possessive
 * characteristic, a to-one and a to-many role, two unit systems — one whose conversion terminates (t to kg) and one
 * whose conversion does not (voet to vadem) — and four rules: one plain, one reading a parameter, one with a validity
 * window, and a consistency rule. It resolves clean on its own ({@code ScenarioTypeGateTest}).
 */
final class ScenarioFixture {

    private ScenarioFixture() {}

    static final String MODEL = """
        Eenheidsysteem Gewicht
            de ton (mv: tonnen) t = 1000 kg
            het kilogram (mv: kilogrammen) kg

        Eenheidsysteem Lengte
            de vadem (mv: vademen) vadem = 6 voet
            de voet (mv: voeten) voet

        Domein Vondstklasse is van het type Enumeratie
            'drijfgoed'
            'strandgoed'
            'wrakgoed'

        Dimensie het seizoen, bestaande uit de seizoenen (na het attribuut met voorzetsel in):
            1. voorjaar
            2. najaar

        Objecttype de Jutter (mv: Jutters) (bezield)
            de naam Tekst;
            het vondstgewicht Numeriek (niet-negatief getal met 2 decimalen) met eenheid kg;
            het seizoensgewicht Numeriek (niet-negatief getal met 2 decimalen) met eenheid kg gedimensioneerd met seizoen;
            de bergingsdatum Datum in dagen;
            het meldtijdstip Datum en tijd in millisecondes;
            het aandeel Percentage (geheel getal);
            de vondstklasse Vondstklasse;
            de borgdagen Numeriek (niet-negatief geheel getal) voor elke maand;
            de kustwachtbonus Numeriek (geheel getal);
            de wachtbonus Numeriek (geheel getal);
            is bergingsbevoegd kenmerk (bijvoeglijk);
            is borgplichtig kenmerk (bijvoeglijk) voor elke maand;
            het strandrecht kenmerk (bezittelijk);

        Objecttype de Vondst (mv: Vondsten)
            de sleepafstand Numeriek (getal) met eenheid vadem;
            het gewicht Numeriek (niet-negatief getal met 2 decimalen) met eenheid kg;
            is gemeld kenmerk (bijvoeglijk);

        Feittype vondstaanmelding
            de aanmelder Jutter
            de vondst (mv: vondsten) Vondst
        Eén aanmelder heeft meerdere vondsten

        Parameter de vondstdrempel : Numeriek (geheel getal)
        Parameter het penningbedrag : Numeriek (geheel getal) voor elk jaar

        Regel bevoegdheid bij gewicht
            geldig altijd
                Een Jutter is bergingsbevoegd
                indien zijn vondstgewicht groter of gelijk is aan 10 kg.

        Regel bonus bij drempel
            geldig altijd
                De kustwachtbonus van een Jutter moet gesteld worden op de vondstdrempel.

        Regel wachtbonus vanaf 2025
            geldig vanaf 01-01-2025
                De wachtbonus van een Jutter moet gesteld worden op 5.

        Regel geen wrakgoed
            geldig altijd
                De vondstklasse van een Jutter moet ongelijk zijn aan 'wrakgoed'.
        """;

    static ScenarioBundle resolve(String scenarioText) {
        return ScenarioResolver.resolve(MODEL + scenarioText);
    }

    /** The 1-based line of the first line of {@code bron} that contains {@code merker}. */
    static int lineOf(String source, String merker) {
        String[] regels = source.split("\n", -1);
        for (int i = 0; i < regels.length; i++) if (regels[i].contains(merker)) return i + 1;
        return fail("merker '" + merker + "' staat niet in de bron");
    }

    /** An ERROR whose message contains {@code fragment} stands on the line of {@code bron} that holds {@code merker}. */
    static void errorOnLine(ScenarioBundle b, String source, String merker, String fragment) {
        int regel = lineOf(source, merker);
        List<Diagnostic> matching = b.model().errors().stream()
                .filter(d -> d.line() == regel && d.message().contains(fragment)).toList();
        assertFalse(matching.isEmpty(), () -> "verwacht een ERROR met '" + fragment + "' op regel " + regel
                + " (de regel met '" + merker + "'), maar de diagnoses zijn: " + b.model().errors());
    }
}
