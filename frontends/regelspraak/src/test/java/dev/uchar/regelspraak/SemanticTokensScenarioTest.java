package dev.uchar.regelspraak;

import dev.uchar.regelspraak.model.RegelSpraakModel.Diagnostic;
import dev.uchar.regelspraak.resolver.Resolver;
import dev.uchar.regelspraak.resolver.ScenarioResolver;
import dev.uchar.regelspraak.resolver.SemanticTokens;
import dev.uchar.regelspraak.resolver.SemanticTokens.Kind;
import dev.uchar.regelspraak.resolver.SemanticTokens.Span;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The semantic tokens of a test document of the scenario layer (grammar/DIVERGENCES.md, "Semantic tokens", "A test
 * document"): the declarations and rules in one file, the parameter set, the situations and the scenarios in the
 * other, read as one chain as {@code --cases} reads it. The scenarios write every form of the table "case expectation"
 * (grammar/DIVERGENCES.md, Scenariotoetsen), both scope forms that carry the same rules, a situation based on another,
 * a label opened again in a later block, and one name that names nothing.
 */
class SemanticTokensScenarioTest {

    static final String MODEL = """
            Eenheidsysteem Munt
                de euro (mv: euros) EUR €

            Domein Vondstsoort is van het type Enumeratie
                'wrakhout'
                'zeewier'

            Objecttype de Jutter (mv: Jutters) (bezield)
                de naam Tekst;
                het aantal vondsten Numeriek (niet-negatief geheel getal);
                de uitkering Numeriek (niet-negatief geheel getal) met eenheid €;
                de borgdagen Numeriek (niet-negatief geheel getal) voor elke maand;
                is penninggerechtigd kenmerk (bijvoeglijk);
                is borgplichtig kenmerk (bijvoeglijk) voor elke maand;
                het strandrecht kenmerk (bezittelijk);

            Objecttype de Vondst (mv: Vondsten)
                de soort Vondstsoort;
                de vinddatum Datum in dagen;

            Feittype vondstaanmelding
                de aanmelder Jutter
                de vondst (mv: vondsten) Vondst
            Eén aanmelder heeft meerdere vondsten

            Parameter de vondstdrempel : Numeriek (geheel getal)
            Parameter het penningbedrag : Numeriek (niet-negatief geheel getal) met eenheid €

            Regelgroep penningregels
            Regel penningrecht
                geldig altijd
                    Een Jutter is penninggerechtigd
                    indien zijn aantal vondsten groter of gelijk is aan de vondstdrempel.

            Regel uitkering
                geldig altijd
                    De uitkering van een Jutter moet gesteld worden op het penningbedrag
                    indien hij penninggerechtigd is.

            Regel borgdagen van een jutter
                geldig altijd
                    De borgdagen van een Jutter moet berekend worden als het aantal dagen in de maand dat hij borgplichtig is.
            """;

    static final String TEST = """
            Parameterset jaarlijkse vaststelling:
            de parameter de vondstdrempel is 12 van 01-01-2025 tot 01-01-2026
            de parameter het penningbedrag is 250 € van 01-01-2025 tot 01-01-2026
            de parameter de vondstdrempel is 14 van 01-01-2026 tot 01-01-2027
            de parameter het penningbedrag is 275 € van 01-01-2026 tot 01-01-2027

            Situatie twee jutters:
            een Jutter J1
            - de naam is "Aukje"
            - het aantal vondsten is 13
            - de vondsten zijn V1 en V2
            - heeft strandrecht
            een Vondst V1
            - de soort is 'wrakhout'
            een Vondst V2
            - de vinddatum is 03-01-2025
            een Jutter J2
            - het aantal vondsten is 15
            een Vondst V3
            - de aanmelder is J2

            Situatie met borg gebaseerd op de situatie twee jutters:
            een Jutter J2
            - is borgplichtig van 01-01-2025 tot 11-01-2025
            de parameter de vondstdrempel is 3
            de parameter het penningbedrag is 7 €

            Scenario oudejaarsdag
            toetst de regelgroep penningregels
            met peildatum 31-12-2025
            met parameterset jaarlijkse vaststelling
            gebaseerd op de situatie twee jutters
            Gegeven is de volgende situatie:
            een Jutter J2
            - de naam is "Bart"
            Met als verwacht resultaat:
            - J1 is penninggerechtigd
            - de uitkering van J1 is 250 €
            - J1 heeft het strandrecht
            - J2 heeft geen strandrecht
            - de vinddatum van V3 is leeg
            - de borgdagen van J1 is altijd leeg
            - de gegevens zijn consistent

            Scenario borg
            toetst de regels penningrecht, uitkering en borgdagen van een jutter
            met peildatum 01-06-2025
            gebaseerd op de situatie met borg
            Met als verwacht resultaat:
            - J1 is niet penninggerechtigd
            - de borgdagen van J2 is 10 van 01-01-2025 tot 01-02-2025
            - de spaarpot van J2 is 3
            - de gegevens zijn inconsistent volgens "uitkering" voor J1
            """;

    static final List<SemanticTokens.SourceFile> FILES = List.of(
            new SemanticTokens.SourceFile("jutters.rgs", MODEL),
            new SemanticTokens.SourceFile("toets.rgs", TEST));

    private static SemanticTokens.Document document() {
        return Resolver.tokens(FILES);
    }

    private static String source(String file) {
        return file.equals("jutters.rgs") ? MODEL : TEST;
    }

    /** The place of {@code text}'s {@code n}-th occurrence in {@code source}: line from 1, column from 0. */
    private static int[] place(String source, String text, int n) {
        int at = -1;
        for (int i = 0; i <= n; i++) at = source.indexOf(text, at + 1);
        assertTrue(at >= 0, () -> "'" + text + "' does not occur " + (n + 1) + " times");
        int line = 1, lineStart = 0;
        for (int i = 0; i < at; i++) if (source.charAt(i) == '\n') { line++; lineStart = i + 1; }
        return new int[]{line, at - lineStart};
    }

    /** The span of {@code file} that starts where {@code anchor}'s {@code n}-th occurrence starts. */
    private static Span at(SemanticTokens.Document d, String file, String anchor, int n) {
        int[] p = place(source(file), anchor, n);
        for (SemanticTokens.FileSpans fs : d.files())
            if (fs.file().equals(file))
                for (Span s : fs.spans()) if (s.line() == p[0] && s.column() == p[1]) return s;
        return fail("no span at '" + anchor + "' (" + n + ") in " + file);
    }

    /** {@code text} is a declaration of {@code kind} where {@code anchor}'s {@code n}-th occurrence begins in the test
     *  file. */
    private static void declaration(SemanticTokens.Document d, String anchor, int n, String text, Kind kind) {
        Span s = at(d, "toets.rgs", anchor, n);
        assertEquals(text, s.text());
        assertEquals(kind, s.kind(), () -> "'" + text + "' at '" + anchor + "' (" + n + ")");
        assertTrue(s.declaration(), () -> "'" + text + "' at '" + anchor + "' (" + n + ") is a declaration");
    }

    /** The span where {@code anchor}'s {@code n}-th occurrence begins in the test file is {@code text}, a reference of
     *  {@code kind} to the declaration where {@code declared}'s {@code m}-th occurrence begins in {@code file}. */
    private static void reference(SemanticTokens.Document d, String anchor, int n, String text, Kind kind,
                                  String file, String declared, int m) {
        Span s = at(d, "toets.rgs", anchor, n);
        assertEquals(text, s.text());
        assertEquals(kind, s.kind(), () -> "'" + text + "' at '" + anchor + "' (" + n + ")");
        assertFalse(s.declaration(), () -> "'" + text + "' at '" + anchor + "' (" + n + ") is no declaration");
        assertNotNull(s.declaredAt(), () -> "'" + text + "' at '" + anchor + "' (" + n + ") points at its declaration");
        assertEquals(file, s.declaredAt().file());
        assertArrayEquals(place(source(file), declared, m), new int[]{s.declaredAt().line(), s.declaredAt().column()},
                () -> "'" + text + "' at '" + anchor + "' (" + n + ") points at '" + declared + "' (" + m + ") in " + file);
    }

    private static void kind(SemanticTokens.Document d, String anchor, int n, String text, Kind kind) {
        Span s = at(d, "toets.rgs", anchor, n);
        assertEquals(text, s.text());
        assertEquals(kind, s.kind(), () -> "'" + text + "' at '" + anchor + "' (" + n + ")");
        assertNull(s.declaredAt());
    }

    @Test void the_test_document_is_covered_without_a_syntax_error_and_every_reference_points_at_a_declaration() {
        SemanticTokens.Document d = document();
        assertTrue(d.diagnostics().stream().noneMatch(x -> "RGS-091".equals(x.code()) || "RGS-216".equals(x.code())),
                () -> "no syntax error: " + d.diagnostics());
        SemanticTokensCheck.check(FILES, d);
        for (SemanticTokens.FileSpans fs : d.files())
            for (Span s : fs.spans()) {
                assertFalse(SemanticTokensCheck.unlinked(s), () -> "a reference without its declaration: " + s);
                assertNotEquals(Kind.WORD, s.kind(), () -> "a word outside every name: " + s);
            }
    }

    @Test void the_diagnostics_are_those_of_the_scenario_route() {
        SemanticTokens.Document d = document();
        String chain = MODEL + "\n" + TEST + "\n";
        List<Diagnostic> cases = ScenarioResolver.resolve(chain).model().diagnostics;
        int firstTestLine = MODEL.split("\n", -1).length + 1;
        List<String> expected = new ArrayList<>();
        for (Diagnostic x : cases)
            expected.add(x.severity() + " " + x.code() + " "
                    + (x.line() < firstTestLine ? "jutters.rgs:" + x.line() : "toets.rgs:" + (x.line() - firstTestLine + 1)));
        List<String> actual = new ArrayList<>();
        for (SemanticTokens.FileDiagnostic x : d.diagnostics()) actual.add(x.severity() + " " + x.code() + " " + x.file() + ":" + x.line());
        assertEquals(expected, actual);
        int line = place(TEST, "spaarpot", 0)[0];
        assertEquals(List.of("ERROR RGS-236 toets.rgs:" + line), actual, "the one name that names nothing, and nothing else");
    }

    @Test void the_names_of_the_scenario_layer_are_declared_where_they_are_written() {
        SemanticTokens.Document d = document();
        declaration(d, "jaarlijkse vaststelling:", 0, "jaarlijkse vaststelling", Kind.PARAMETER_SET);
        declaration(d, "twee jutters:", 0, "twee jutters", Kind.SITUATION);
        declaration(d, "met borg gebaseerd", 0, "met borg", Kind.SITUATION);
        declaration(d, "oudejaarsdag", 0, "oudejaarsdag", Kind.SCENARIO);
        declaration(d, "borg\ntoetst", 0, "borg", Kind.SCENARIO);
        declaration(d, "J1\n", 0, "J1", Kind.LABEL);
        declaration(d, "V1\n", 0, "V1", Kind.LABEL);
        declaration(d, "V2\n", 1, "V2", Kind.LABEL);
        declaration(d, "J2\n", 0, "J2", Kind.LABEL);
        declaration(d, "V3\n", 0, "V3", Kind.LABEL);
    }

    @Test void a_name_in_the_scenario_layer_points_at_its_declaration() {
        SemanticTokens.Document d = document();
        // the header of a scenario
        reference(d, "penningregels", 0, "penningregels", Kind.RULE_GROUP, "jutters.rgs", "penningregels", 0);
        reference(d, "penningrecht,", 0, "penningrecht", Kind.NAMED_FORM, "jutters.rgs", "penningrecht\n", 0);
        reference(d, "uitkering en", 0, "uitkering", Kind.NAMED_FORM, "jutters.rgs", "uitkering\n", 0);
        reference(d, "borgdagen van een jutter\nmet", 0, "borgdagen van een jutter", Kind.NAMED_FORM,
                "jutters.rgs", "borgdagen van een jutter", 0);
        reference(d, "jaarlijkse vaststelling\n", 0, "jaarlijkse vaststelling", Kind.PARAMETER_SET,
                "toets.rgs", "jaarlijkse vaststelling:", 0);
        reference(d, "twee jutters\n", 0, "twee jutters", Kind.SITUATION, "toets.rgs", "twee jutters:", 0);
        reference(d, "twee jutters:", 1, "twee jutters", Kind.SITUATION, "toets.rgs", "twee jutters:", 0);
        reference(d, "met borg\nMet", 0, "met borg", Kind.SITUATION, "toets.rgs", "met borg gebaseerd", 0);
        // a situation block
        reference(d, "Jutter J1", 0, "Jutter", Kind.OBJECT_TYPE, "jutters.rgs", "Jutter (mv:", 0);
        reference(d, "naam is", 0, "naam", Kind.ATTRIBUTE, "jutters.rgs", "naam Tekst", 0);
        reference(d, "aantal vondsten is", 0, "aantal vondsten", Kind.ATTRIBUTE, "jutters.rgs", "aantal vondsten Numeriek", 0);
        reference(d, "vondsten zijn", 0, "vondsten", Kind.ROLE, "jutters.rgs", "vondst (mv:", 0);
        reference(d, "V1 en", 0, "V1", Kind.LABEL, "toets.rgs", "V1\n", 0);
        reference(d, "V2\n", 0, "V2", Kind.LABEL, "toets.rgs", "V2\n", 1);
        reference(d, "strandrecht\n", 0, "strandrecht", Kind.CHARACTERISTIC, "jutters.rgs", "strandrecht kenmerk", 0);
        reference(d, "soort is", 0, "soort", Kind.ATTRIBUTE, "jutters.rgs", "soort Vondstsoort", 0);
        reference(d, "'wrakhout'", 0, "'wrakhout'", Kind.ENUMERATION_VALUE, "jutters.rgs", "'wrakhout'", 0);
        reference(d, "aanmelder is", 0, "aanmelder", Kind.ROLE, "jutters.rgs", "aanmelder Jutter", 0);
        reference(d, "J2\n\n", 0, "J2", Kind.LABEL, "toets.rgs", "J2\n", 0);
        reference(d, "borgplichtig van", 0, "borgplichtig", Kind.CHARACTERISTIC, "jutters.rgs", "borgplichtig kenmerk", 0);
        reference(d, "vondstdrempel is 12", 0, "vondstdrempel", Kind.PARAMETER, "jutters.rgs", "vondstdrempel :", 0);
        reference(d, "penningbedrag is 7", 0, "penningbedrag", Kind.PARAMETER, "jutters.rgs", "penningbedrag :", 0);
        reference(d, "€ van", 0, "€", Kind.UNIT, "jutters.rgs", "euro (mv:", 0);
        kind(d, "de vondstdrempel is 12", 0, "de", Kind.KEYWORD);
        // a label opened again: in a situation based on the one that opened it, and in a scenario's own situation
        reference(d, "J2\n- is borgplichtig", 0, "J2", Kind.LABEL, "toets.rgs", "J2\n", 0);
        reference(d, "J2\n- de naam is \"Bart\"", 0, "J2", Kind.LABEL, "toets.rgs", "J2\n", 0);
        // the expectations
        reference(d, "J1 is penninggerechtigd", 0, "J1", Kind.LABEL, "toets.rgs", "J1\n", 0);
        reference(d, "penninggerechtigd\n- de uitkering", 0, "penninggerechtigd", Kind.CHARACTERISTIC,
                "jutters.rgs", "penninggerechtigd kenmerk", 0);
        reference(d, "uitkering van J1", 0, "uitkering", Kind.ATTRIBUTE, "jutters.rgs", "uitkering Numeriek", 0);
        reference(d, "strandrecht\n- J2", 0, "strandrecht", Kind.CHARACTERISTIC, "jutters.rgs", "strandrecht kenmerk", 0);
        kind(d, "geen strandrecht", 0, "geen", Kind.KEYWORD);
        reference(d, "strandrecht\n- de vinddatum", 0, "strandrecht", Kind.CHARACTERISTIC,
                "jutters.rgs", "strandrecht kenmerk", 0);
        reference(d, "vinddatum van V3", 0, "vinddatum", Kind.ATTRIBUTE, "jutters.rgs", "vinddatum Datum", 0);
        reference(d, "V3 is leeg", 0, "V3", Kind.LABEL, "toets.rgs", "V3\n", 0);
        reference(d, "borgdagen van J1", 0, "borgdagen", Kind.ATTRIBUTE, "jutters.rgs", "borgdagen Numeriek", 0);
        kind(d, "niet penninggerechtigd", 0, "niet", Kind.KEYWORD);
        reference(d, "penninggerechtigd\n- de borgdagen", 0, "penninggerechtigd", Kind.CHARACTERISTIC,
                "jutters.rgs", "penninggerechtigd kenmerk", 0);
        // in the scenario based on 'met borg', J2 is the instance 'twee jutters' opened
        reference(d, "J2 is 10", 0, "J2", Kind.LABEL, "toets.rgs", "J2\n", 0);
        kind(d, "volgens", 0, "volgens", Kind.KEYWORD);
        kind(d, "\"uitkering\"", 0, "\"uitkering\"", Kind.STRING);
        reference(d, "J1\n", 1, "J1", Kind.LABEL, "toets.rgs", "J1\n", 0);
    }

    @Test void a_name_that_names_nothing_is_unresolved() {
        SemanticTokens.Document d = document();
        kind(d, "spaarpot", 0, "spaarpot", Kind.UNRESOLVED);
    }

    @Test void a_chain_without_a_block_of_the_scenario_layer_is_read_as_a_production_document() {
        // the model alone: no scenario block, so the frontend reads it as --ir reads it, and nothing names a label
        SemanticTokens.Document d = Resolver.tokens(List.of(new SemanticTokens.SourceFile("jutters.rgs", MODEL)));
        assertTrue(d.diagnostics().stream().noneMatch(x -> x.code().equals("RGS-095")),
                () -> "no scenario is asked of a production document: " + d.diagnostics());
        // a scenario block in a chain read as a production document would be a syntax error; here it is none
        SemanticTokens.Document t = document();
        assertTrue(t.toJson().contains("\"kind\": \"label\""));
        assertTrue(t.toJson().contains("\"kind\": \"situation\""));
        assertTrue(t.toJson().contains("\"kind\": \"parameterSet\""));
        assertTrue(t.toJson().contains("\"kind\": \"scenario\""));
    }
}
