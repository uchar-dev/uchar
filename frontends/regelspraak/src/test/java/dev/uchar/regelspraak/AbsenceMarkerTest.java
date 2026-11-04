package dev.uchar.regelspraak;

import dev.uchar.regelspraak.emit.IrEmitter;
import dev.uchar.regelspraak.model.RegelSpraakModel;
import dev.uchar.regelspraak.model.RegelSpraakModel.Diagnostic;
import dev.uchar.regelspraak.model.RegelSpraakModel.Severity;
import dev.uchar.regelspraak.resolver.Resolver;
import dev.uchar.regelspraak.resolver.SemanticTokens;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The absence markers of the dialect (grammar/DIVERGENCES.md, "Afwezigheidsmarkeringen"; RUNTIME-SEMANTICS ch. 9.5):
 * "(of onbekend)", "(of niet van toepassing)" and "(of onbekend of niet van toepassing)" at the end of an attribute
 * declaration lower the attribute to a sum type of its own; a marker written is a Construct of it, a marker tested an
 * equality with it, "een waarde heeft" a gevuld test of the value, and every read of the value a PayloadAccess under
 * that test. A read of the value without the test is RGS-376, the frontend's half of WF-127. Measured on the frontend
 * before the markers: the declaration was a syntax error at "(of", the value "onbekend" an unresolved name and each test
 * an unresolved characteristic check, each with a WARNING.
 */
class AbsenceMarkerTest {

    /** The source of the golden corpus/golden/jutterswet-afwezigheidsmarkers, whose ir.json is its emission. */
    static final String GOLDEN = """
        Objecttype de Jutter (mv: Jutters)
            het getaxeerd totaal Numeriek (getal met 2 decimalen);

        Objecttype de Vondst (mv: Vondsten)
            de taxatie Numeriek (getal met 2 decimalen) (of onbekend of niet van toepassing);
            de schatting Numeriek (getal met 2 decimalen);
            het vindloon Numeriek (getal met 2 decimalen);
            de opslagkosten Numeriek (geheel getal) voor elke maand (of onbekend of niet van toepassing);
            de opslagtoeslag Numeriek (geheel getal) voor elke maand;
            is onleesbaar kenmerk (bijvoeglijk);
            is drijfhout kenmerk (bijvoeglijk);
            is kostbaar kenmerk (bijvoeglijk);
            is verkocht kenmerk (bijvoeglijk) voor elke maand;

        Feittype vangst
            de jutter Jutter
            de vondst Vondst
            één jutter heeft meerdere vondsten

        Regel taxatie uit schatting
            geldig altijd
                De taxatie van een Vondst moet gesteld worden op zijn schatting.

        Regel taxatie onbekend
            geldig altijd
                De taxatie van een Vondst moet gesteld worden op onbekend
                indien hij onleesbaar is.

        Regel taxatie niet van toepassing
            geldig altijd
                De taxatie van een Vondst moet gesteld worden op niet van toepassing
                indien hij drijfhout is.

        Regel vindloon naar taxatie
            geldig altijd
                Het vindloon van een Vondst moet berekend worden als zijn taxatie gedeeld door 10 rekenkundig afgerond op 2 decimalen
                indien zijn taxatie een waarde heeft.

        Regel forfaitair vindloon
            geldig altijd
                Het vindloon van een Vondst moet gesteld worden op 25,00
                indien zijn taxatie onbekend is.

        Regel geen vindloon
            geldig altijd
                Het vindloon van een Vondst moet gesteld worden op 0,00
                indien zijn taxatie niet van toepassing is.

        Regel kostbare vondst
            geldig altijd
                Een Vondst is kostbaar
                indien hij aan alle volgende voorwaarden voldoet:
                - zijn taxatie heeft een waarde
                - zijn taxatie is groter dan 1000,00.

        Regel getaxeerd totaal
            geldig altijd
                Het getaxeerd totaal van een Jutter moet berekend worden als de som van de taxatie van alle vondsten van de Jutter die een waarde hebben.

        Regel opslag onbekend in de winter
            geldig altijd
                De opslagkosten van een Vondst moet gesteld worden op onbekend van dd. 01-01-2025 tot dd. 01-03-2025.

        Regel opslag niet van toepassing na verkoop
            geldig altijd
                De opslagkosten van een Vondst moet gesteld worden op niet van toepassing gedurende de tijd dat hij verkocht is.

        Regel opslagtoeslag
            geldig altijd
                De opslagtoeslag van een Vondst moet gesteld worden op 7 gedurende de tijd dat zijn opslagkosten onbekend zijn.
        """;

    private static final Path GOLDEN_IR = Path.of("..", "..", "corpus", "golden", "jutterswet-afwezigheidsmarkers", "ir.json");

    /** The guarantee gate 10 gives a golden with a source.rgs, for the one golden whose source the Drools backend
     *  cannot read back yet: its ir.json is the frontend's emission of the source, byte for byte. */
    @Test void the_golden_is_the_emission_of_its_source() throws Exception {
        RegelSpraakModel m = Resolver.resolve(GOLDEN);
        assertTrue(m.diagnostics.isEmpty(), () -> "onverwachte diagnostiek: " + m.diagnostics);
        assertEquals(Files.readString(GOLDEN_IR, StandardCharsets.UTF_8), IrEmitter.emit(m));
    }

    /** Every word of the markers is a keyword of its own, so the semantic tokens of the golden leave none unresolved. */
    @Test void the_words_of_the_markers_are_keywords() {
        var files = List.of(new SemanticTokens.SourceFile("golden.rgs", GOLDEN));
        SemanticTokens.Document d = Resolver.tokens(files);
        for (SemanticTokens.Span s : d.files().get(0).spans())
            assertNotEquals(SemanticTokens.Kind.UNRESOLVED, s.kind(), () -> "onopgelost: '" + s.text() + "' op regel " + s.line());
        assertTrue(d.files().get(0).spans().stream().anyMatch(s -> s.text().equals("(of onbekend of niet van toepassing)")
                && s.kind() == SemanticTokens.Kind.KEYWORD), "de markering is een sleutelwoord");
    }

    private static final String VONDST = """
        Objecttype de Vondst (mv: Vondsten)
            de taxatie Numeriek (getal met 2 decimalen) (of niet van toepassing);
            het vindloon Numeriek (getal met 2 decimalen);
            is drijfhout kenmerk (bijvoeglijk);
        """;

    /** One marker: the sum type carries the value and that marker, and its domain is the attribute's own. */
    @Test void one_marker_is_one_constructor_beside_the_value() {
        String ir = IrEmitter.emit(Resolver.resolve(VONDST + """

            Regel geen taxatie
                geldig altijd
                    De taxatie van een Vondst moet gesteld worden op niet van toepassing
                    indien hij drijfhout is.
            """));
        String compact = ir.replaceAll("\\s+", "");
        assertTrue(compact.contains("{\"node\":\"Domain\",\"name\":\"taxatievaneenVondst\",\"datatype\":{\"node\":\"SumType\","
                + "\"constructors\":[{\"name\":\"waarde\",\"payload\":{\"node\":\"Number\",\"whole\":false,\"decimals\":2,"
                + "\"sign\":\"unspecified\"}},{\"name\":\"nietvantoepassing\",\"payload\":null}]}}"), ir);
        assertTrue(compact.contains("\"value\":{\"node\":\"Construct\""), ir);
        assertFalse(compact.contains("onbekend"), ir);
    }

    private static Diagnostic only(RegelSpraakModel m, String code) {
        List<Diagnostic> found = m.diagnostics.stream().filter(d -> code.equals(d.code())).toList();
        assertEquals(1, found.size(), () -> "verwachtte één " + code + ", kreeg: " + m.diagnostics);
        return found.get(0);
    }

    /** A read of the value in a computation without the test is refused, with the attribute, its type and the read. */
    @Test void a_read_without_the_test_is_refused() {
        RegelSpraakModel m = Resolver.resolve(VONDST + """

            Regel vindloon
                geldig altijd
                    Het vindloon van een Vondst moet berekend worden als zijn taxatie gedeeld door 10 rekenkundig afgerond op 2 decimalen.
            """);
        Diagnostic d = only(m, "RGS-376");
        assertEquals(Severity.ERROR, d.severity());
        assertEquals("taxatie", d.args().get("attribute"));
        assertEquals("Vondst", d.args().get("objectType"));
        assertEquals("zijn taxatie", String.valueOf(d.args().get("expression")));
        assertEquals(8, d.line());
    }

    /** A comparison before the test in an `alle` is read before the test (RUNTIME-SEMANTICS ch. 5.10), so it is refused;
     *  the test before the comparison is the golden's. */
    @Test void a_comparison_before_the_test_is_refused() {
        RegelSpraakModel m = Resolver.resolve(VONDST + """
                is kostbaar kenmerk (bijvoeglijk);

            Regel kostbaar
                geldig altijd
                    Een Vondst is kostbaar
                    indien hij aan alle volgende voorwaarden voldoet:
                    - zijn taxatie is groter dan 1000,00
                    - zijn taxatie heeft een waarde.
            """);
        assertEquals("taxatie", only(m, "RGS-376").args().get("attribute"));
    }

    /** A sum over members reads each member's value, so it needs the test per member: "die een waarde hebben". */
    @Test void a_sum_without_the_subselection_is_refused() {
        RegelSpraakModel m = Resolver.resolve(VONDST + """

            Objecttype de Jutter (mv: Jutters)
                het getaxeerd totaal Numeriek (getal met 2 decimalen);

            Feittype vangst
                de jutter Jutter
                de vondst Vondst
                één jutter heeft meerdere vondsten

            Regel totaal
                geldig altijd
                    Het getaxeerd totaal van een Jutter moet berekend worden als de som van de taxatie van alle vondsten van de Jutter.
            """);
        assertEquals("taxatie", only(m, "RGS-376").args().get("attribute"));
    }

    /** A test of the attribute itself reads no value: "gevuld is" holds on a marker, which is a filled value. */
    @Test void a_filled_test_of_the_attribute_needs_no_test() {
        RegelSpraakModel m = Resolver.resolve(VONDST + """

            Regel vindloon
                geldig altijd
                    Het vindloon van een Vondst moet gesteld worden op 5,00
                    indien zijn taxatie gevuld is.
            """);
        assertTrue(m.diagnostics.isEmpty(), () -> "onverwachte diagnostiek: " + m.diagnostics);
        assertFalse(IrEmitter.emit(m).contains("PayloadAccess"));
    }

    /** A marker the attribute does not declare stays the unresolved name it is without markers. */
    @Test void a_marker_the_attribute_does_not_declare_stays_unresolved() {
        RegelSpraakModel m = Resolver.resolve(VONDST + """

            Regel onbekende taxatie
                geldig altijd
                    De taxatie van een Vondst moet gesteld worden op onbekend
                    indien hij drijfhout is.
            """);
        assertEquals("RGS-092", m.diagnostics.get(0).code(), () -> "" + m.diagnostics);
        assertTrue(m.absenceMarkerSites.isEmpty());
    }

    /** The markers stand on an attribute, in the place of the opgave marker and not beside it. */
    @Test void a_marker_on_a_characteristic_or_beside_the_opgave_marker_is_a_syntax_error() {
        RegelSpraakModel k = Resolver.resolve("""
            Objecttype de Vondst (mv: Vondsten)
                is drijfhout kenmerk (bijvoeglijk) (of onbekend);
            """);
        assertEquals("RGS-091", k.errors().get(0).code(), () -> "" + k.diagnostics);
        RegelSpraakModel o = Resolver.resolve("""
            Objecttype de Vondst (mv: Vondsten)
                de taxatie Numeriek (getal met 2 decimalen) (of onbekend) (mag opgegeven worden);
            """);
        assertEquals("RGS-091", o.errors().get(0).code(), () -> "" + o.diagnostics);
    }

    /** Without the markers nothing changes: a characteristic named onbekend is checked as before, and the words lex as
     *  names. */
    @Test void without_markers_the_words_are_names() {
        RegelSpraakModel m = Resolver.resolve("""
            Objecttype de Vondst (mv: Vondsten)
                de waarde Numeriek (getal met 2 decimalen);
                is onbekend kenmerk (bijvoeglijk);

            Regel waarde
                geldig altijd
                    De waarde van een Vondst moet gesteld worden op 5,00
                    indien hij onbekend is.
            """);
        assertTrue(m.diagnostics.isEmpty(), () -> "onverwachte diagnostiek: " + m.diagnostics);
        String ir = IrEmitter.emit(m);
        assertTrue(ir.contains("\"node\": \"CharacteristicCheck\""), ir);
        assertFalse(ir.contains("SumType"), ir);
    }
}
