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

    static final String STRICT_MODEL = """
            Afleiden uit leeg: nee

            Objecttype de Vondst (mv: Vondsten)
                de afhaaldatum Datum in dagen;
                is drijfhout kenmerk (bijvoeglijk);
                het strandrecht kenmerk (bezittelijk);
            """;

    /** A situation with the three forms of a characteristic that does not hold, and the expectations that a member is
     *  open; a document without 'Afleiden uit leeg: nee' refuses the second (RGS-467). */
    static final String STRICT_TEST = """
            Scenario open vondsten
            Gegeven is de volgende situatie:
            een Vondst V1
            - is niet drijfhout
            - heeft geen strandrecht
            een Vondst V2
            - heeft niet het strandrecht
            Met als verwacht resultaat:
            - de afhaaldatum van V1 staat niet vast
            - of V2 drijfhout is, staat niet vast
            - of V2 het strandrecht heeft staat niet vast
            """;

    /** The negation before a characteristic in a situation is a keyword, as in an expectation, and 'staat' and 'vast'
     *  of 'staat niet vast' are keywords; every name points at its declaration. */
    @Test void the_words_of_a_strict_scenario_are_keywords() {
        List<SemanticTokens.SourceFile> files = List.of(new SemanticTokens.SourceFile("vondsten.rgs", STRICT_MODEL),
                new SemanticTokens.SourceFile("strikt.rgs", STRICT_TEST));
        SemanticTokens.Document d = Resolver.tokens(files);
        assertTrue(d.diagnostics().isEmpty(), () -> "no diagnostic: " + d.diagnostics());
        SemanticTokensCheck.check(files, d);
        java.util.Map<Integer, List<String>> lines = new java.util.TreeMap<>();
        for (Span s : d.files().get(1).spans()) {
            assertFalse(SemanticTokensCheck.unlinked(s), () -> "a reference without its declaration: " + s);
            assertNotEquals(Kind.WORD, s.kind(), () -> "a word outside every name: " + s);
            lines.computeIfAbsent(s.line(), x -> new ArrayList<>()).add(s.text() + " " + s.kind().wire());
        }
        assertEquals(List.of("- punctuation", "is keyword", "niet keyword", "drijfhout characteristic"), lines.get(4));
        assertEquals(List.of("- punctuation", "heeft keyword", "geen keyword", "strandrecht characteristic"),
                lines.get(5));
        assertEquals(List.of("- punctuation", "heeft keyword", "niet keyword", "het keyword", "strandrecht characteristic"),
                lines.get(7));
        assertEquals(List.of("- punctuation", "de keyword", "afhaaldatum attribute", "van keyword", "V1 label",
                "staat keyword", "niet keyword", "vast keyword"), lines.get(9));
        assertEquals(List.of("- punctuation", "of keyword", "V2 label", "drijfhout characteristic", "is keyword",
                ", punctuation", "staat keyword", "niet keyword", "vast keyword"), lines.get(10));
        assertEquals(List.of("- punctuation", "of keyword", "V2 label", "het keyword", "strandrecht characteristic",
                "heeft keyword", "staat keyword", "niet keyword", "vast keyword"), lines.get(11));
    }

    @Test void attribute_is_leeg_marks_leeg_as_keyword_and_binds_declared_and_generated_attributes() {
        String model = """
                Afleiden uit leeg: nee

                Objecttype de Vondst (mv: Vondsten)
                    de afhaaldatum Datum in dagen;
                    het bruto gewicht Numeriek (geheel getal);
                    het bergloon Numeriek (geheel getal) (mag opgegeven worden);

                Regel bergloon
                    geldig altijd
                        Het bergloon van een Vondst moet berekend worden als zijn bruto gewicht maal 2.
                """;
        String test = """
                Scenario leeg in situatie
                Gegeven is de volgende situatie:
                een Vondst V1
                - de afhaaldatum is leeg
                - het bruto gewicht is leeg
                - het opgegeven bergloon is leeg
                Met als verwacht resultaat:
                - de gegevens zijn consistent
                """;
        List<SemanticTokens.SourceFile> files = List.of(new SemanticTokens.SourceFile("model.rgs", model),
                new SemanticTokens.SourceFile("toets.rgs", test));
        SemanticTokens.Document d = Resolver.tokens(files);
        assertTrue(d.diagnostics().isEmpty(), () -> "no diagnostic: " + d.diagnostics());
        SemanticTokensCheck.check(files, d);
        java.util.Map<Integer, List<String>> lines = new java.util.TreeMap<>();
        for (Span s : d.files().get(1).spans()) {
            assertFalse(SemanticTokensCheck.unlinked(s), () -> "a reference without its declaration: " + s);
            assertNotEquals(Kind.WORD, s.kind(), () -> "a word outside every name: " + s);
            SemanticTokens.Location target = s.declaredAt() != null ? s.declaredAt() : s.generatedFrom();
            lines.computeIfAbsent(s.line(), x -> new ArrayList<>()).add(s.text() + " " + s.kind().wire()
                    + (target == null ? "" : " " + target.line()));
        }
        assertEquals(List.of("- punctuation", "de keyword", "afhaaldatum attribute 4", "is keyword", "leeg keyword"),
                lines.get(4));
        assertEquals(List.of("- punctuation", "het keyword", "bruto gewicht attribute 5", "is keyword", "leeg keyword"),
                lines.get(5));
        assertEquals(List.of("- punctuation", "het keyword", "opgegeven bergloon attribute 6", "is keyword",
                "leeg keyword"), lines.get(6));
        Span generated = d.files().get(1).spans().stream().filter(s -> s.text().equals("opgegeven bergloon"))
                .findFirst().orElseThrow();
        assertNull(generated.declaredAt());
        assertNotNull(generated.generatedFrom());
        assertEquals(6, generated.generatedFrom().line());
    }

    @Test void role_is_leeg_marks_leeg_as_keyword_and_binds_role_declaration() {
        String model = """
                Afleiden uit leeg: nee

                Objecttype de Jutter (mv: Jutters) (bezield)
                Objecttype de Vondst (mv: Vondsten)
                    de afhaaldatum Datum in dagen;

                Feittype vondstmelding
                    de vinder Jutter
                    de vondst (mv: vondsten) Vondst
                Eén vinder heeft meerdere vondsten
                """;
        String test = """
                Scenario rol leeg in situatie
                Gegeven is de volgende situatie:
                een Vondst V1
                - de afhaaldatum is leeg
                - de vinder is leeg
                een Vondst V2
                een Jutter J1
                - de vondsten zijn leeg
                een Jutter J2
                - de vondsten zijn V2 (volledig gegeven)
                Met als verwacht resultaat:
                - de gegevens zijn consistent
                """;
        List<SemanticTokens.SourceFile> files = List.of(new SemanticTokens.SourceFile("model.rgs", model),
                new SemanticTokens.SourceFile("toets.rgs", test));
        SemanticTokens.Document d = Resolver.tokens(files);
        assertTrue(d.diagnostics().isEmpty(), () -> "no diagnostic: " + d.diagnostics());
        SemanticTokensCheck.check(files, d);
        java.util.Map<Integer, List<String>> lines = new java.util.TreeMap<>();
        for (Span s : d.files().get(1).spans()) {
            assertFalse(SemanticTokensCheck.unlinked(s), () -> "a reference without its declaration: " + s);
            assertNotEquals(Kind.WORD, s.kind(), () -> "a word outside every name: " + s);
            lines.computeIfAbsent(s.line(), x -> new ArrayList<>()).add(s.text() + " " + s.kind().wire()
                    + (s.declaredAt() == null ? "" : " " + s.declaredAt().line()));
        }
        assertEquals(List.of("- punctuation", "de keyword", "afhaaldatum attribute 5", "is keyword", "leeg keyword"),
                lines.get(4));
        assertEquals(List.of("- punctuation", "de keyword", "vinder role 8", "is keyword", "leeg keyword"),
                lines.get(5));
        assertEquals(List.of("- punctuation", "de keyword", "vondsten role 9", "zijn keyword", "leeg keyword"),
                lines.get(8));
        assertEquals(List.of("- punctuation", "de keyword", "vondsten role 9", "zijn keyword", "V2 label 6",
                "(volledig gegeven) keyword"), lines.get(10));
    }

    /** A characteristic declared with a leading "niet" binds its own declaration where its whole name is written, in
     *  a situation and in an expectation, as the scenario route reads it; beside it, "geen drijfhout" is the negation
     *  of "drijfhout". Each line as its spans, a name with the line of the declaration it points at. Measured on the
     *  frontend before: "niet" was a keyword and "drijfhout" bound "drijfhout", or nothing where only "niet drijfhout"
     *  was declared. */
    @Test void a_characteristic_declared_with_niet_binds_its_own_declaration() {
        String alone = """
                Objecttype de Vondst (mv: Vondsten)
                    is niet drijfhout kenmerk (bijvoeglijk);
                """;
        String beside = """
                Objecttype de Vondst (mv: Vondsten)
                    is drijfhout kenmerk (bijvoeglijk);
                    is niet drijfhout kenmerk (bijvoeglijk);
                """;
        String test = """
                Scenario naast elkaar
                Gegeven is de volgende situatie:
                een Vondst V1
                - is niet drijfhout
                een Vondst V2
                - is geen drijfhout
                Met als verwacht resultaat:
                - V1 is niet drijfhout
                - V2 is geen drijfhout
                """;
        java.util.Map<Integer, List<String>> lines = lines(alone, test.substring(0, test.indexOf("een Vondst V2"))
                + "Met als verwacht resultaat:\n- V1 is niet drijfhout\n");
        assertEquals(List.of("- punctuation", "is keyword", "niet drijfhout characteristic 2"), lines.get(4));
        assertEquals(List.of("- punctuation", "V1 label 3", "is keyword", "niet drijfhout characteristic 2"),
                lines.get(6));
        // "- is geen drijfhout" gives drijfhout as not holding in a document without 'Afleiden uit leeg: nee', where
        // that changes nothing (RGS-469); "- is niet drijfhout" names the characteristic declared so
        lines = lines(beside, test, List.of("WARNING RGS-469 toets.rgs:6"));
        assertEquals(List.of("- punctuation", "is keyword", "niet drijfhout characteristic 3"), lines.get(4));
        assertEquals(List.of("- punctuation", "is keyword", "geen keyword", "drijfhout characteristic 2"), lines.get(6));
        assertEquals(List.of("- punctuation", "V1 label 3", "is keyword", "niet drijfhout characteristic 3"),
                lines.get(8));
        assertEquals(List.of("- punctuation", "V2 label 5", "is keyword", "geen keyword", "drijfhout characteristic 2"),
                lines.get(9));
    }

    /** The spans of {@code test} read beside {@code model}, per line: text and kind, and for a reference the line of
     *  its declaration. */
    private static java.util.Map<Integer, List<String>> lines(String model, String test) {
        return lines(model, test, List.of());
    }

    /** As {@link #lines(String, String)}, where the document raises exactly {@code diagnostics}, each as its severity,
     *  code, file and line. */
    private static java.util.Map<Integer, List<String>> lines(String model, String test, List<String> diagnostics) {
        List<SemanticTokens.SourceFile> files = List.of(new SemanticTokens.SourceFile("model.rgs", model),
                new SemanticTokens.SourceFile("toets.rgs", test));
        SemanticTokens.Document d = Resolver.tokens(files);
        List<String> raised = new ArrayList<>();
        for (SemanticTokens.FileDiagnostic x : d.diagnostics())
            raised.add(x.severity() + " " + x.code() + " " + x.file() + ":" + x.line());
        assertEquals(diagnostics, raised, () -> "the diagnostics: " + d.diagnostics());
        SemanticTokensCheck.check(files, d);
        java.util.Map<Integer, List<String>> lines = new java.util.TreeMap<>();
        for (Span s : d.files().get(1).spans()) {
            assertFalse(SemanticTokensCheck.unlinked(s), () -> "a reference without its declaration: " + s);
            lines.computeIfAbsent(s.line(), x -> new ArrayList<>()).add(s.text() + " " + s.kind().wire()
                    + (s.declaredAt() == null ? "" : " " + s.declaredAt().line()));
        }
        return lines;
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
