package dev.uchar.regelspraak;

import dev.uchar.regelspraak.emit.IrEmitter;
import dev.uchar.regelspraak.model.RegelSpraakModel;
import dev.uchar.regelspraak.resolver.Resolver;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-end proof of the unit identity (besluit 16Y + 17b): the dimensionless "1" is the group
 * identity and absorbs under multiplication (1 × E = E), a unitless number divided by E has the unit
 * E⁻¹, while a unit the {@link dev.uchar.regelspraak.model.Unit} model cannot spell is a LOUD
 * resolver error — never an Onbekend that rides a Binary onto the wire.
 */
class UnitIdentityTest {

    /** (a) A unitless ATTRIBUTE times a unit-bearing attribute now carries the real unit on the
     *  wire (§4.4 Tabel 11 rij 4 + Tabel 13), not the old Onbekend("samengestelde eenheid"). */
    @Test void unitless_attribute_times_unit_emits_the_real_type() {
        String src = """
            Eenheidsysteem Munt
                de euro (mv: euros) EUR €
            Objecttype de Verkoop
                het stuktal Numeriek (geheel getal);
                de stukprijs Numeriek (getal met 2 decimalen) met eenheid €;
                de opbrengst Numeriek (getal met 2 decimalen) met eenheid €;
            Regel opbrengst
                geldig altijd
                    De opbrengst van een Verkoop moet berekend worden als het stuktal van de Verkoop maal de stukprijs van de Verkoop.
            """;
        RegelSpraakModel m = Resolver.resolve(src);
        assertTrue(m.errors().isEmpty(), () -> "onverwachte fouten: " + m.errors());
        String c = IrEmitter.emit(m).replaceAll("\\s+", "");
        // a derived result the frontend cannot type goes onto the wire as null, so a null type here would be
        // the old degradation
        assertFalse(c.contains("\"type\":null"),
                () -> "het product mag niet meer degraderen naar een ontypeerbaar resultaat:\n" + c);
        // the times product carries its own number type with the EUR unit (2 = 0 + 2 decimals, Tabel 13).
        // NOTE emitCompact strips ALL whitespace, also inside the source snippet.
        assertTrue(c.contains("\"op\":\"times\","), () -> "het times-product ontbreekt:\n" + c);
        assertTrue(c.contains(
                "\"type\":{\"node\":\"Number\",\"whole\":false,\"decimals\":2,\"sign\":\"unspecified\","
                + "\"unit\":{\"node\":\"Unit\",\"abbreviation\":\"EUR\",\"system\":\"Munt\",\"canonical\":\"EUR\","
                + "\"factorToCanonical\":{\"num\":1,\"den\":1}}},"
                + "\"source\":{\"snippet\":\"hetstuktalvandeVerkoopmaaldestukprijsvandeVerkoop\"}"),
                () -> "het product moet het echte EUR-type (2 decimalen) dragen:\n" + c);
    }

    private static final String TIME = """
            Eenheidsysteem Tijd
                de dag (mv: dagen) dg
                de week (mv: weken) wk = 7 dg
            Eenheidsysteem Munt
                de euro (mv: euros) EUR €
            Eenheidsysteem Lengte
                de meter m
            """;

    private static RegelSpraakModel withAttributes(String attributes, String rule) {
        return Resolver.resolve(TIME + "Objecttype de Verkoop\n" + attributes + "Regel r\n    geldig altijd\n        " + rule + "\n");
    }

    private static void assertClean(RegelSpraakModel m) {
        assertTrue(m.errors().isEmpty(), () -> "onverwachte fouten: " + m.errors());
    }

    private static boolean refused(RegelSpraakModel m, String code) {
        return m.errors().stream().anyMatch(d -> code.equals(d.code()));
    }

    /** (b) A unitless numerator divided by a unit-bearing denominator is the reciprocal 1/E, which the Unit model
     *  spells and the wire writes — besluit 17b diverges from the (unsound) Tabel 14 rij 4, which gives E. */
    @Test void unitless_divided_by_unit_has_the_reciprocal_type() {
        String src = """
            Eenheidsysteem Tijdsduur
                de maand mnd
            Objecttype de Verkoop
                het stuktal Numeriek (geheel getal);
                de looptijd Numeriek (geheel getal) met eenheid mnd;
                het tempo Numeriek (getal met 2 decimalen) met eenheid 1/mnd;
            Regel tempo
                geldig altijd
                    Het tempo van een Verkoop moet berekend worden als het stuktal van de Verkoop gedeeld door de looptijd van de Verkoop.
            """;
        RegelSpraakModel m = Resolver.resolve(src);
        assertClean(m);
        String c = IrEmitter.emit(m).replaceAll("\\s+", "");
        assertTrue(c.contains("\"unit\":{\"node\":\"Unit\",\"abbreviation\":\"1\",\"denominator\":{\"node\":\"Unit\","
                + "\"abbreviation\":\"mnd\",\"system\":\"Tijdsduur\",\"canonical\":\"mnd\","
                + "\"factorToCanonical\":{\"num\":1,\"den\":1}}}"),
                () -> "de breuk draagt de eenheid 1/mnd op de wire:\n" + c);
    }

    /** (e) The form of a bill divided by days without a unit on the target: refused for the target's unit. */
    @Test void a_reciprocal_does_not_fit_a_target_without_a_unit() {
        String src = """
            Eenheidsysteem Tijd
                de dag dg
            Objecttype de Jutter (mv: Jutters) (bezield)
                het penningbedrag Numeriek (getal met 2 decimalen);
                de borgdagen Numeriek (geheel getal) met eenheid dg;
                het dagbedrag Numeriek (getal met 2 decimalen);
            Regel dagbedrag van een jutter
                geldig altijd
                    Het dagbedrag van een Jutter moet berekend worden als zijn penningbedrag gedeeld door zijn borgdagen.
            """;
        RegelSpraakModel m = Resolver.resolve(src);
        assertTrue(refused(m, "RGS-359"), () -> "verwachtte de melding over het doel, kreeg: " + m.errors());
        String message = m.errors().stream().filter(d -> "RGS-359".equals(d.code())).findFirst().orElseThrow().message();
        assertTrue(message.contains("1/dg") && message.contains("attribuut zonder eenheid"), message);
        assertFalse(m.errors().stream().anyMatch(d -> d.message().contains("niet uitdrukken")),
                () -> "de eenheid is uitdrukbaar, en de melding mag niet anders zeggen: " + m.errors());
    }

    @Test void a_reciprocal_times_a_duration_has_no_unit_also_across_a_conversion() {
        String attributes = """
                het tempo Numeriek (getal met 2 decimalen) met eenheid 1/dg;
                de looptijd Numeriek (geheel getal) met eenheid wk;
                het resultaat Numeriek (getal met 2 decimalen);
            """;
        assertClean(withAttributes(attributes,
                "Het resultaat van een Verkoop moet berekend worden als het tempo van de Verkoop maal de looptijd van de Verkoop."));
    }

    @Test void a_rate_is_a_factor_times_a_reciprocal() {
        String attributes = """
                het tempo Numeriek (getal met 2 decimalen) met eenheid 1/dg;
                het bedrag Numeriek (getal met 2 decimalen) met eenheid €;
                het dagbedrag Numeriek (getal met 4 decimalen) met eenheid €/dg;
            """;
        assertClean(withAttributes(attributes,
                "Het dagbedrag van een Verkoop moet berekend worden als het tempo van de Verkoop maal het bedrag van de Verkoop."));
    }

    @Test void comparing_and_adding_reciprocals_needs_the_same_dimension() {
        String attributes = """
                het tempo Numeriek (getal met 2 decimalen) met eenheid 1/dg;
                het weektempo Numeriek (getal met 2 decimalen) met eenheid 1/wk;
                de looptijd Numeriek (geheel getal) met eenheid dg;
                het dagbedrag Numeriek (getal met 2 decimalen) met eenheid €/dg;
                het totaal Numeriek (getal met 2 decimalen) met eenheid 1/dg;
                is snel kenmerk (bijvoeglijk);
            """;
        assertClean(withAttributes(attributes, "Een Verkoop is snel indien zijn tempo groter is dan zijn weektempo."));
        assertClean(withAttributes(attributes,
                "Het totaal van een Verkoop moet berekend worden als het tempo van de Verkoop plus het weektempo van de Verkoop."));
        assertFalse(withAttributes(attributes, "Een Verkoop is snel indien zijn tempo groter is dan zijn looptijd.").errors().isEmpty(),
                "1/dg tegen dg is geen zelfde dimensie");
        assertFalse(withAttributes(attributes, "Een Verkoop is snel indien zijn tempo groter is dan zijn dagbedrag.").errors().isEmpty(),
                "1/dg tegen €/dg is geen zelfde dimensie");
    }

    /** dg^-1 and 1/dg are one unit: an attribute in the one takes a value in the other, without any restatement. */
    @Test void a_negative_exponent_spells_the_reciprocal() {
        String attributes = """
                het stuktal Numeriek (geheel getal);
                de looptijd Numeriek (geheel getal) met eenheid dg;
                het tempo Numeriek (getal met 2 decimalen) met eenheid dg^-1;
            """;
        RegelSpraakModel m = withAttributes(attributes,
                "Het tempo van een Verkoop moet berekend worden als het stuktal van de Verkoop gedeeld door de looptijd van de Verkoop.");
        assertClean(m);
        assertTrue(IrEmitter.emit(m).replaceAll("\\s+", "").contains("\"abbreviation\":\"1\",\"denominator\""));
    }

    /** m^2 is read and refused, and it does not degrade to m: no golden can carry the product through the executors. */
    @Test void a_power_is_refused_and_is_not_the_unit() {
        for (String spelling : java.util.List.of("m^2", "m^3")) {
            RegelSpraakModel m = withAttributes("""
                    de lengte Numeriek (geheel getal) met eenheid m;
                    de oppervlakte Numeriek (geheel getal) met eenheid %s;
                """.formatted(spelling),
                    "De oppervlakte van een Verkoop moet berekend worden als de lengte van de Verkoop maal de lengte van de Verkoop.");
            assertTrue(refused(m, "RGS-360"), () -> spelling + ": verwachtte RGS-360, kreeg " + m.errors());
        }
        assertClean(withAttributes("""
                de lengte Numeriek (geheel getal) met eenheid m^1;
                de omtrek Numeriek (geheel getal) met eenheid m;
            """, "De omtrek van een Verkoop moet berekend worden als de lengte van de Verkoop."));
    }

    /** What has no normal form is an error by name and no unit, also the two spellings that once lost a part in silence. */
    @Test void a_spelling_without_a_normal_form_is_refused_by_name() {
        for (String spelling : java.util.List.of("€/dg/m", "m^0", "m^-2", "m^2", "€/dg^2", "m^2/dg", "2/dg", "1/dg/m", "1/dg^2", "m^1/2")) {
            RegelSpraakModel m = withAttributes("    het cijfer Numeriek (getal met 2 decimalen) met eenheid " + spelling + ";\n",
                    "Het cijfer van een Verkoop moet berekend worden als 1.");
            assertTrue(refused(m, "RGS-360"), () -> spelling + ": verwachtte RGS-360, kreeg " + m.errors());
        }
    }

    /** A scenario input in the other reciprocal restates exactly, by the inverted factor of the denominator. */
    @Test void a_scenario_value_in_a_reciprocal_restates_into_the_declared_reciprocal() {
        String source = TIME + """
            Objecttype de Jutter (mv: Jutters) (bezield)
                het tempo Numeriek (getal met 2 decimalen) met eenheid 1/dg;
                het weektempo Numeriek (getal met 2 decimalen) met eenheid 1/wk;
            Situatie tempo:
            een Jutter J
            - het tempo is 14 1/wk
            - het weektempo is 2 1/dg
            Scenario x
            gebaseerd op de situatie tempo
            Met als verwacht resultaat:
            - de gegevens zijn consistent
            """;
        var b = dev.uchar.regelspraak.resolver.ScenarioResolver.resolve(source);
        assertTrue(b.isOk(), () -> b.model().errors().toString());
        var j = b.scenarios().get(0).situation().instance("J");
        assertEquals("2", j.attributes().get(0).value().number(), "14 per week is 2 per dag");
        assertEquals("14", j.attributes().get(1).value().number(), "2 per dag is 14 per week");
    }

    @Test void a_literal_in_a_reciprocal_unit_is_compared_with_the_attribute() {
        String attributes = """
                het tempo Numeriek (getal met 2 decimalen) met eenheid 1/dg;
                is snel kenmerk (bijvoeglijk);
            """;
        assertClean(withAttributes(attributes, "Een Verkoop is snel indien zijn tempo groter is dan 30 1/wk."));
    }

    /** (c) A rate factor (€/jr maal m = €·m/jr) is a product unit with a denominator the model cannot
     *  spell — besluit 17c makes it a LOUD error instead of the old Onbekend. */
    @Test void unspellable_rate_factor_is_refused_loudly() {
        String src = """
            Eenheidsysteem Munt
                de euro (mv: euros) EUR €
            Eenheidsysteem Tijdsduur
                het jaar jr
            Eenheidsysteem Lengte
                de meter m
            Objecttype de Verkoop
                het bedrag Numeriek (getal met 2 decimalen) met eenheid €;
                de looptijd Numeriek (geheel getal) met eenheid jr;
                de afstand Numeriek (geheel getal) met eenheid m;
                het resultaat Numeriek (getal met 2 decimalen) met eenheid €;
            Regel resultaat
                geldig altijd
                    Het resultaat van een Verkoop moet berekend worden als (het bedrag van de Verkoop gedeeld door de looptijd van de Verkoop) maal de afstand van de Verkoop.
            """;
        RegelSpraakModel m = Resolver.resolve(src);
        assertTrue(m.errors().stream().anyMatch(d -> d.message().contains("niet uitdrukken")),
                () -> "verwachtte een luide onspelbare-samengestelde-eenheid-fout, kreeg: " + m.errors());
    }
}
