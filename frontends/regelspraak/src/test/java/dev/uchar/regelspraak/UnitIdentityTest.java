package dev.uchar.regelspraak;

import dev.uchar.regelspraak.emit.IrEmitter;
import dev.uchar.regelspraak.model.RegelSpraakModel;
import dev.uchar.regelspraak.resolver.Resolver;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-end proof of the unit identity (besluit 16Y + 17b): the dimensionless "1" is the group
 * identity and absorbs under multiplication (1 × E = E), a unitless number divided by E has the unit
 * E⁻¹, and a spelling that names no factor is a LOUD resolver error — never an Onbekend that rides a
 * Binary onto the wire.
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
                + "\"unit\":[{\"node\":\"Unit\",\"abbreviation\":\"EUR\",\"system\":\"Munt\",\"canonical\":\"EUR\","
                + "\"factorToCanonical\":{\"num\":1,\"den\":1}}]},"
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
        assertTrue(c.contains("\"unit\":[{\"node\":\"Unit\","
                + "\"abbreviation\":\"mnd\",\"system\":\"Tijdsduur\",\"canonical\":\"mnd\","
                + "\"factorToCanonical\":{\"num\":1,\"den\":1},\"exponent\":-1}]"),
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
        assertTrue(refused(m, "RGS-335"), () -> "verwachtte de zin van WF-023, kreeg: " + m.errors());
        String message = m.errors().stream().filter(d -> "RGS-335".equals(d.code())).findFirst().orElseThrow().message();
        assertTrue(message.contains("value unit '1/dg', target unit 'geen'"), message);
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
        assertTrue(IrEmitter.emit(m).replaceAll("\\s+", "").contains("\"exponent\":-1"));
    }

    private static final String FACTORS = TIME + """
            Eenheidsysteem Klok
                het uur u
                de seconde s
            """;

    /** The written unit of the attribute {@code cijfer} on the wire: per factor its abbreviation, and its exponent where
     *  one is written. */
    private static String wireFactors(String spelling) {
        RegelSpraakModel m = Resolver.resolve(FACTORS + "Objecttype de Verkoop\n"
                + "    het cijfer Numeriek (getal met 2 decimalen) met eenheid " + spelling + ";\n"
                + "    de uitkomst Numeriek (geheel getal);\n"
                + "Regel r\n    geldig altijd\n        De uitkomst van een Verkoop moet berekend worden als 1.\n");
        assertClean(m);
        return unitOf(IrEmitter.emit(m).replaceAll("\\s+", ""), "\"cijfer\"");
    }

    /** The factors of the first {@code "unit":[…]} after {@code anchor} in a compact document. */
    private static String unitOf(String compact, String anchor) {
        int from = compact.indexOf("\"unit\":[", compact.indexOf(anchor));
        int depth = 0, end = from + 7;
        do {
            char c = compact.charAt(end++);
            if (c == '[') depth++;
            if (c == ']') depth--;
        } while (depth > 0);
        java.util.List<String> factors = new java.util.ArrayList<>();
        for (String f : compact.substring(from, end).split("\"node\":\"Unit\"")) {
            var a = java.util.regex.Pattern.compile("\"abbreviation\":\"([^\"]+)\"").matcher(f);
            if (!a.find()) continue;
            var e = java.util.regex.Pattern.compile("\"exponent\":(-?\\d+)").matcher(f);
            factors.add(e.find() ? a.group(1) + "^" + e.group(1) : a.group(1));
        }
        return String.join(" ", factors);
    }

    /** Every spelling that names factors with a whole exponent other than 0 is a unit, in the order of WF-124 (d). */
    @Test void a_spelling_is_read_into_its_factor_list() {
        var expected = new java.util.LinkedHashMap<String, String>();
        expected.put("m", "m");
        expected.put("m^1", "m");
        expected.put("m^2", "m^2");
        expected.put("m^-2", "m^-2");
        expected.put("dg^-1", "dg^-1");
        expected.put("1/dg", "dg^-1");
        expected.put("€/dg", "EUR dg^-1");
        expected.put("€/u/s", "EUR s^-1 u^-1");
        expected.put("€/s/u", "EUR s^-1 u^-1");
        expected.put("1/dg/m", "dg^-1 m^-1");
        expected.put("1/dg^2", "dg^-2");
        expected.put("€/dg^2", "EUR dg^-2");
        expected.put("m^2/s", "m^2 s^-1");
        expected.put("m/m^2", "m^-1");
        expected.put("dg/dg/m", "m^-1");
        expected.put("€/dg^-1", "EUR dg");
        expected.forEach((spelling, wire) -> assertEquals(wire, wireFactors(spelling), spelling));
    }

    private static String refusalOf(String spelling) {
        RegelSpraakModel m = withAttributes("    het cijfer Numeriek (getal met 2 decimalen) met eenheid " + spelling + ";\n"
                + "    de uitkomst Numeriek (geheel getal);\n",
                "De uitkomst van een Verkoop moet berekend worden als 1.");
        var refusal = m.errors().stream().filter(d -> "RGS-361".equals(d.code())).toList();
        assertEquals(1, refusal.size(), () -> spelling + ": verwachtte een RGS-361, kreeg " + m.errors());
        assertEquals(spelling, refusal.get(0).params().get("spelling"));
        return refusal.get(0).params().get("part") + ": " + refusal.get(0).message();
    }

    /** A spelling that names no factor is an error by name and no unit, whatever part of it does so. */
    @Test void a_spelling_that_names_no_unit_is_refused_by_name() {
        assertTrue(refusalOf("m^0").startsWith("m^0: "), refusalOf("m^0"));
        assertTrue(refusalOf("m^-0").startsWith("m^-0: "), refusalOf("m^-0"));
        assertTrue(refusalOf("m^1/2").startsWith("m^1/2: "), refusalOf("m^1/2"));
        assertTrue(refusalOf("m^1,5").startsWith("m^1,5: "), refusalOf("m^1,5"));
        assertTrue(refusalOf("€/dg^0").startsWith("dg^0: "), refusalOf("€/dg^0"));
        assertTrue(refusalOf("m/m").startsWith("m/m: "), refusalOf("m/m"));
        assertTrue(refusalOf("2/dg").startsWith("2: "), refusalOf("2/dg"));
        assertTrue(refusalOf("1/2/dg").startsWith("1/2: "), refusalOf("1/2/dg"));
        assertTrue(refusalOf("2,5/dg").startsWith("2,5: "), refusalOf("2,5/dg"));
    }

    /** The numerator of a unit is 1: the message says the 2 is a number, and where it goes. */
    @Test void a_number_over_a_unit_points_at_the_value() {
        String message = refusalOf("2/dg");
        assertTrue(message.contains("'2/dg'") && message.contains("getal") && message.contains("'met eenheid 1/dg'")
                && message.contains("waarde"), message);
        assertTrue(refusalOf("2/dg/m").contains("'met eenheid 1/dg/m'"), refusalOf("2/dg/m"));
    }

    /** A rule over an attribute in {@code unit} with the literal as its value: the document, emitted. */
    private static String literalDocument(String unit, String literal) {
        RegelSpraakModel m = literalModel(unit, literal);
        assertClean(m);
        return IrEmitter.emit(m).replaceAll("\\s+", "");
    }

    private static RegelSpraakModel literalModel(String unit, String literal) {
        return Resolver.resolve(FACTORS + "Objecttype de Verkoop\n"
                + "    het cijfer Numeriek (getal met 2 decimalen) met eenheid " + unit + ";\n"
                + "Regel r\n    geldig altijd\n        Het cijfer van een Verkoop moet berekend worden als " + literal + ".\n");
    }

    /** The value and the unit of the one literal in a document, the source snippet aside: the snippet is the text that
     *  was written, and it differs by construction. */
    private static String literalOnTheWire(String compact) {
        int at = compact.indexOf("\"node\":\"Literal\"");
        int end = compact.indexOf("\"source\":", at);
        return compact.substring(at, end);
    }

    /** {@code 2/dg} is the value 2 with the unit 1/dg, on the wire what {@code 2 1/dg} is. */
    @Test void a_number_over_a_unit_is_a_value_in_the_inverse_unit() {
        var same = new java.util.LinkedHashMap<String, String[]>();
        same.put("2/dg", new String[]{"1/dg", "2 1/dg"});
        same.put("2,5/dg", new String[]{"1/dg", "2,5 1/dg"});
        same.put("-2/dg", new String[]{"1/dg", "-2 1/dg"});
        same.put("2/ dg", new String[]{"1/dg", "2 1/dg"});
        same.put("2/dg/m", new String[]{"1/dg/m", "2 1/dg/m"});
        same.put("2/m^2", new String[]{"1/m^2", "2 1/m^2"});
        same.put("1/dg", new String[]{"1/dg", "1 1/dg"});
        same.put("1/2/dg", new String[]{"1/dg", "1/2 1/dg"});
        same.forEach((literal, unitAndOther) -> {
            String unit = unitAndOther[0];
            String a = literalDocument(unit, literal), b = literalDocument(unit, unitAndOther[1]);
            assertEquals(literalOnTheWire(b), literalOnTheWire(a), literal);
        });
        assertEquals("dg^-1", unitOf(literalDocument("1/dg", "2/dg"), "\"node\":\"Literal\""));
        assertTrue(literalOnTheWire(literalDocument("1/dg", "2,5/dg")).contains("\"decimals\":1"), "2,5 heeft één decimaal");
    }

    /** A fraction stays one number: the lexer takes {@code 1/2} as one token, a unit name cannot start with a digit. */
    @Test void a_fraction_stays_a_number() {
        String half = literalOnTheWire(literalDocument("dg", "1/2 dg"));
        assertTrue(half.contains("\"number\":\"1/2\""), half);
        assertEquals("dg", unitOf(literalDocument("dg", "1/2 dg"), "\"node\":\"Literal\""));
        RegelSpraakModel noUnit = Resolver.resolve(FACTORS + "Objecttype de Verkoop\n"
                + "    het cijfer Numeriek (getal met 2 decimalen);\n"
                + "Regel r\n    geldig altijd\n        Het cijfer van een Verkoop moet berekend worden als 1/2.\n");
        assertClean(noUnit);
        String plain = literalOnTheWire(IrEmitter.emit(noUnit).replaceAll("\\s+", ""));
        assertTrue(plain.contains("\"number\":\"1/2\"") && !plain.contains("\"unit\""), plain);
    }

    /** A name after the slash that no system declares is no division and no bare unit: an error that names it. */
    @Test void a_name_over_a_number_that_is_no_unit_is_refused_by_name() {
        for (String[] literal : new String[][]{{"2/foo", "foo"}, {"2/dg/foo", "foo"}, {"2/foo^2", "foo"}, {"2,5/foo", "foo"}}) {
            RegelSpraakModel m = literalModel("1/dg", literal[0]);
            var refusal = m.errors().stream().filter(d -> "RGS-362".equals(d.code())).toList();
            assertEquals(1, refusal.size(), () -> literal[0] + ": verwachtte RGS-362, kreeg " + m.errors());
            assertEquals(literal[1], refusal.get(0).params().get("name"));
            assertEquals(literal[0], refusal.get(0).params().get("spelling"));
            assertTrue(refusal.get(0).message().contains("'" + literal[1] + "'"), refusal.get(0).message());
            assertFalse(m.errors().stream().anyMatch(d -> "RGS-361".equals(d.code())), literal[0]);
        }
    }

    /** What may follow the slash without a declaration: a time unit of the calendar table, and the euro sign. */
    @Test void a_calendar_unit_and_the_euro_sign_need_no_declaration_over_a_number() {
        for (String[] form : new String[][]{{"2/dag", "1/dag"}, {"2/uur", "1/uur"}, {"2/€", "1/€"}, {"2/ms", "1/ms"}}) {
            RegelSpraakModel m = Resolver.resolve("Objecttype de Verkoop\n"
                    + "    het cijfer Numeriek (getal met 2 decimalen) met eenheid " + form[1] + ";\n"
                    + "Regel r\n    geldig altijd\n        Het cijfer van een Verkoop moet berekend worden als " + form[0] + ".\n");
            assertClean(m);
        }
    }

    /** A slash after a percent sign, a slash with nothing behind it and a zero denominator stay syntax errors. */
    @Test void what_is_no_number_over_a_unit_stays_a_syntax_error() {
        for (String literal : java.util.List.of("2 %/dg", "2/%", "2/", "2/3/", "2/0", "1/2/3"))
            assertFalse(literalModel("1/dg", literal).errors().isEmpty(), literal);
    }

    /** A value over a unit in a comparison and in a scenario, each read as its reciprocal. */
    @Test void a_number_over_a_unit_reads_in_a_comparison_and_in_a_scenario() {
        assertClean(withAttributes("""
                het tempo Numeriek (getal met 2 decimalen) met eenheid 1/dg;
                is snel kenmerk (bijvoeglijk);
            """, "Een Verkoop is snel indien zijn tempo groter is dan 30/wk."));
        var b = dev.uchar.regelspraak.resolver.ScenarioResolver.resolve(TIME + """
            Objecttype de Jutter (mv: Jutters) (bezield)
                het tempo Numeriek (getal met 2 decimalen) met eenheid 1/dg;
            Situatie tempo:
            een Jutter J
            - het tempo is 14/wk
            Scenario x
            gebaseerd op de situatie tempo
            Met als verwacht resultaat:
            - de gegevens zijn consistent
            """);
        assertTrue(b.isOk(), () -> b.model().errors().toString());
        assertEquals("2", b.scenarios().get(0).situation().instance("J").attributes().get(0).value().number(),
                "14 per week is 2 per dag");
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

    /** (c) A rate factor (EUR/jr maal m = EUR·m/jr) is a unit, and it has another dimension than the target €: the sentence
     *  of WF-023, with the unit written out. */
    @Test void a_rate_factor_into_another_dimension_is_refused_by_its_unit() {
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
        assertTrue(refused(m, "RGS-335"), () -> "verwachtte de zin van WF-023, kreeg: " + m.errors());
        String message = m.errors().stream().filter(d -> "RGS-335".equals(d.code())).findFirst().orElseThrow().message();
        assertTrue(message.contains("value unit 'EUR·m/jr', target unit 'EUR'"), message);
    }
}
