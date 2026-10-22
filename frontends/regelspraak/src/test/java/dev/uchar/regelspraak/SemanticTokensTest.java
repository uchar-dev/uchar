package dev.uchar.regelspraak;

import dev.uchar.regelspraak.resolver.Resolver;
import dev.uchar.regelspraak.resolver.SemanticTokens;
import dev.uchar.regelspraak.resolver.SemanticTokens.Kind;
import dev.uchar.regelspraak.resolver.SemanticTokens.Span;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The semantic tokens of the frontend (grammar/DIVERGENCES.md, "Semantic tokens") on a Jutterswet model of two files:
 * the declarations in one, the rules in the other, so a reference crosses the file boundary. The model writes every
 * kind of name, a characteristic whose name runs over a line end, a name that names nothing, a {@code // Bron:}
 * comment, and a character outside the Basic Multilingual Plane before a name on the same line, which puts the UTF-16
 * column one unit past ANTLR's code-point column. A comment runs to the end of its line, so no name can follow one on
 * its line: the character stands in a designation and in a text literal instead, and once in a comment, whose end
 * column it moves.
 */
class SemanticTokensTest {

    static final String DECLARATIONS = """
            // Bron: Jutterswet artikel 1 𝔍
            Vocabulaire handboek (taal: nl)

            Eenheidsysteem Tijd
                het jaar (mv: jaren) jr

            Domein Bedrag is van het type Numeriek (getal met 2 decimalen) met eenheid €
            Domein Vondstsoort is van het type Enumeratie
                'wrakhout'
                'zeewier'

            Dimensie het seizoen, bestaande uit de seizoenen (na het attribuut met voorzetsel in):
                1. voorjaar
                2. najaar

            Dagsoort de springtijdag (mv: springtijdagen);

            Parameter de volwassenleeftijd : Numeriek (geheel getal) met eenheid jr

            Objecttype de Jutter (mv: Jutters) (bezield)
                de naam Tekst;
                de leeftijd Numeriek (geheel getal) met eenheid jr;
                het vondstgewicht Numeriek (getal met 2 decimalen) gedimensioneerd met seizoen;
                het voorjaarsgewicht Numeriek (getal met 2 decimalen);
                is minderjarig kenmerk (bijvoeglijk);
                het recht op premie kenmerk (bezittelijk);

            Objecttype de Vondst (mv: Vondsten)
                de soort (handboek: soort vondst 𝔙) Vondstsoort;
                de waarde Bedrag;
                de schatting Bedrag;
                de vinddatum Datum in dagen;
                is getijvondst kenmerk (bijvoeglijk);

            Feittype vondstaanmelding
                de aanmelder Jutter
                de vondst (mv: vondsten) Vondst
            Eén aanmelder heeft meerdere vondsten
            """;

    static final String RULES = """
            Regelgroep leeftijdsregels
            Regel minderjarigheid
                geldig altijd
                    Een Jutter is minderjarig
                    indien zijn leeftijd kleiner is dan de volwassenleeftijd.

            Regel premierecht
                geldig altijd
                    Een Jutter heeft het recht op
                        premie
                    indien hij een aanmelder is.

            Regel voorjaarsgewicht van een jutter
                geldig altijd
                    Het voorjaarsgewicht van een Jutter moet berekend worden als zijn vondstgewicht in voorjaar.

            Regel waarde van een vondst
                geldig altijd
                    De waarde van een Vondst moet berekend worden als het basisbedrag maal 2
                    indien de soort van de Vondst gelijk is aan 'wrakhout'.
                    Daarbij geldt:
                        het basisbedrag is 10 €.

            Regel getijvondst
                geldig altijd
                    Een Vondst is getijvondst
                    indien zijn vinddatum een springtijdag is.

            Regel naam van een jutter
                geldig altijd
                    De naam van een Jutter moet gesteld worden op "𝔍" indien zijn leeftijd groter is dan 0 jr.

            Regel schatting van een vondst
                geldig altijd
                    De schatting van een Vondst moet gesteld worden op de waarde van de Vondst maal de taxatiefactor.
            """;

    static final List<SemanticTokens.SourceFile> FILES = List.of(
            new SemanticTokens.SourceFile("jutters.rgs", DECLARATIONS),
            new SemanticTokens.SourceFile("regels.rgs", RULES));

    private static SemanticTokens.Document document() {
        return Resolver.tokens(FILES);
    }

    /** The {@code n}-th span (from 0) of {@code file} whose text is {@code text}. */
    private static Span span(SemanticTokens.Document d, String file, String text, int n) {
        int seen = 0;
        for (SemanticTokens.FileSpans fs : d.files()) {
            if (!fs.file().equals(file)) continue;
            for (Span s : fs.spans()) if (s.text().equals(text) && seen++ == n) return s;
        }
        return fail("no span '" + text + "' (" + n + ") in " + file);
    }

    /** The place of {@code text}'s {@code n}-th occurrence in {@code source}: line from 1, column from 0, both counted
     *  as Java counts a String, in UTF-16 code units. */
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
        int[] p = place(file.equals("jutters.rgs") ? DECLARATIONS : RULES, anchor, n);
        for (SemanticTokens.FileSpans fs : d.files())
            if (fs.file().equals(file))
                for (Span s : fs.spans()) if (s.line() == p[0] && s.column() == p[1]) return s;
        return fail("no span at '" + anchor + "' (" + n + ") in " + file);
    }

    /** {@code text} is a declaration of {@code kind} where {@code anchor} begins in {@code file}. */
    private static void declaration(SemanticTokens.Document d, String file, String anchor, String text, Kind kind) {
        Span s = at(d, file, anchor, 0);
        assertEquals(text, s.text());
        assertEquals(kind, s.kind(), () -> "'" + text + "' in " + file);
        assertTrue(s.declaration(), () -> "'" + text + "' is the declaration");
    }

    /** The span where {@code anchor}'s {@code n}-th occurrence begins in the rules is {@code text}, a reference of
     *  {@code kind} to the declaration where {@code declared} begins. */
    private static void reference(SemanticTokens.Document d, String anchor, int n, String text, Kind kind,
                                  String declared) {
        Span s = at(d, "regels.rgs", anchor, n);
        assertEquals(text, s.text());
        assertEquals(kind, s.kind(), () -> "'" + text + "' at '" + anchor + "' (" + n + ") in regels.rgs");
        assertFalse(s.declaration());
        assertNotNull(s.declaredAt(), () -> "'" + text + "' points at its declaration");
        String file = s.declaredAt().file();
        int[] p = place(file.equals("jutters.rgs") ? DECLARATIONS : RULES, declared, 0);
        assertArrayEquals(p, new int[]{s.declaredAt().line(), s.declaredAt().column()},
                () -> "'" + text + "' points at '" + declared + "' in " + file);
    }

    @Test void the_output_covers_both_files_and_every_reference_points_at_a_declaration() {
        SemanticTokens.Document d = document();
        SemanticTokensCheck.check(FILES, d);
        for (SemanticTokens.FileSpans fs : d.files())
            for (Span s : fs.spans())
                assertFalse(SemanticTokensCheck.unlinked(s), () -> "a reference without its declaration: " + s);
    }

    @Test void every_kind_of_name_is_declared_where_it_is_written() {
        SemanticTokens.Document d = document();
        declaration(d, "jutters.rgs", "handboek (taal", "handboek", Kind.VOCABULARY);
        declaration(d, "jutters.rgs", "Tijd\n", "Tijd", Kind.UNIT_SYSTEM);
        declaration(d, "jutters.rgs", "jaar (mv", "jaar", Kind.UNIT);
        declaration(d, "jutters.rgs", "Bedrag is", "Bedrag", Kind.DOMAIN);
        declaration(d, "jutters.rgs", "'wrakhout'", "'wrakhout'", Kind.ENUMERATION_VALUE);
        declaration(d, "jutters.rgs", "seizoen,", "seizoen", Kind.DIMENSION);
        declaration(d, "jutters.rgs", "voorjaar", "voorjaar", Kind.DIMENSION_LABEL);
        declaration(d, "jutters.rgs", "springtijdag (mv", "springtijdag", Kind.DAY_KIND);
        declaration(d, "jutters.rgs", "volwassenleeftijd", "volwassenleeftijd", Kind.PARAMETER);
        declaration(d, "jutters.rgs", "Jutter (mv", "Jutter", Kind.OBJECT_TYPE);
        declaration(d, "jutters.rgs", "leeftijd Numeriek", "leeftijd", Kind.ATTRIBUTE);
        declaration(d, "jutters.rgs", "minderjarig kenmerk", "minderjarig", Kind.CHARACTERISTIC);
        declaration(d, "jutters.rgs", "vondstaanmelding", "vondstaanmelding", Kind.FACT_TYPE);
        declaration(d, "jutters.rgs", "aanmelder Jutter", "aanmelder", Kind.ROLE);
        declaration(d, "regels.rgs", "leeftijdsregels", "leeftijdsregels", Kind.RULE_GROUP);
        declaration(d, "regels.rgs", "minderjarigheid", "minderjarigheid", Kind.NAMED_FORM);
        declaration(d, "regels.rgs", "basisbedrag is", "basisbedrag", Kind.VARIABLE);
    }

    @Test void a_name_in_a_rule_points_at_its_declaration_in_the_other_file() {
        SemanticTokens.Document d = document();
        reference(d, "Jutter is", 0, "Jutter", Kind.OBJECT_TYPE, "Jutter (mv:");
        reference(d, "minderjarig\n", 0, "minderjarig", Kind.CHARACTERISTIC, "minderjarig kenmerk");
        reference(d, "leeftijd kleiner", 0, "leeftijd", Kind.ATTRIBUTE, "leeftijd Numeriek");
        reference(d, "volwassenleeftijd", 0, "volwassenleeftijd", Kind.PARAMETER, "volwassenleeftijd :");
        reference(d, "aanmelder is", 0, "aanmelder", Kind.ROLE, "aanmelder Jutter");
        reference(d, "voorjaar.", 0, "voorjaar", Kind.DIMENSION_LABEL, "voorjaar\n");
        reference(d, "vondstgewicht in", 0, "vondstgewicht", Kind.ATTRIBUTE, "vondstgewicht Numeriek");
        reference(d, "soort van", 0, "soort", Kind.ATTRIBUTE, "soort (handboek");
        reference(d, "'wrakhout'", 0, "'wrakhout'", Kind.ENUMERATION_VALUE, "'wrakhout'");
        reference(d, "springtijdag is", 0, "springtijdag", Kind.DAY_KIND, "springtijdag (mv:");
        reference(d, "getijvondst\n", 1, "getijvondst", Kind.CHARACTERISTIC, "getijvondst kenmerk");
        reference(d, "jr.", 0, "jr", Kind.UNIT, "jaar (mv:");
        // the variable is declared in the same rule, after its use
        reference(d, "basisbedrag maal", 0, "basisbedrag", Kind.VARIABLE, "basisbedrag is");
        // in the declarations: a domain as a datatype, and the vocabulary of a designation
        Span domain = at(d, "jutters.rgs", "Vondstsoort;", 0);
        assertEquals(Kind.DOMAIN, domain.kind());
        assertArrayEquals(place(DECLARATIONS, "Vondstsoort is", 0),
                new int[]{domain.declaredAt().line(), domain.declaredAt().column()});
        Span vocabulary = at(d, "jutters.rgs", "(handboek: soort", 0);
        assertEquals("(handboek:", vocabulary.text());
        assertEquals(Kind.VOCABULARY, vocabulary.kind());
        assertArrayEquals(place(DECLARATIONS, "handboek (taal", 0),
                new int[]{vocabulary.declaredAt().line(), vocabulary.declaredAt().column()});
    }

    @Test void a_name_over_a_line_end_is_one_span() {
        SemanticTokens.Document d = document();
        Span s = span(d, "regels.rgs", "recht op\n            premie", 0);
        assertEquals(Kind.CHARACTERISTIC, s.kind());
        assertEquals(s.line() + 1, s.endLine());
        assertArrayEquals(place(DECLARATIONS, "recht op premie", 0), new int[]{s.declaredAt().line(), s.declaredAt().column()});
    }

    @Test void a_name_that_names_nothing_is_unresolved_and_the_diagnostic_names_its_file_and_line() {
        SemanticTokens.Document d = document();
        Span s = span(d, "regels.rgs", "taxatiefactor", 0);
        assertEquals(Kind.UNRESOLVED, s.kind());
        assertNull(s.declaredAt());
        int line = place(RULES, "taxatiefactor", 0)[0];
        assertTrue(d.diagnostics().stream().anyMatch(x -> "regels.rgs".equals(x.file()) && x.line() == line),
                () -> "a diagnostic on regels.rgs:" + line + ": " + d.diagnostics());
    }

    @Test void a_source_comment_is_one_comment_span_and_its_end_counts_utf16_units() {
        SemanticTokens.Document d = document();
        String comment = "// Bron: Jutterswet artikel 1 𝔍";
        Span s = span(d, "jutters.rgs", comment, 0);
        assertEquals(Kind.COMMENT, s.kind());
        assertEquals(1, s.line());
        assertEquals(0, s.column());
        assertEquals(comment.length(), s.endColumn(), "two UTF-16 units for the one character outside the BMP");
    }

    @Test void a_name_after_a_character_outside_the_bmp_has_its_utf16_column() {
        SemanticTokens.Document d = document();
        // the designation holds U+1D519 before the domain name on the same line
        Span domain = at(d, "jutters.rgs", "Vondstsoort;", 0);
        assertEquals("Vondstsoort", domain.text());
        int[] p = place(DECLARATIONS, "Vondstsoort;", 0);
        String line = DECLARATIONS.lines().toList().get(p[0] - 1);
        assertEquals(line.codePointCount(0, p[1]) + 1, p[1], "the line has one character outside the BMP before the name");
        // the text literal holds U+1D50D before the attribute name on the same line
        Span attribute = at(d, "regels.rgs", "leeftijd groter", 0);
        assertEquals("leeftijd", attribute.text());
        assertEquals(Kind.ATTRIBUTE, attribute.kind());
        int[] q = place(RULES, "leeftijd groter", 0);
        String ruleLine = RULES.lines().toList().get(q[0] - 1);
        assertEquals(ruleLine.codePointCount(0, q[1]) + 1, q[1], "the line has one character outside the BMP before the name");
    }

    @Test void the_json_carries_the_spans_and_the_diagnostics() {
        String json = document().toJson();
        Object parsed = Report.json(json);
        assertTrue(parsed instanceof java.util.Map<?, ?>, "a JSON object");
        java.util.Map<?, ?> m = (java.util.Map<?, ?>) parsed;
        assertEquals(2, ((List<?>) m.get("files")).size());
        assertFalse(((List<?>) m.get("diagnostics")).isEmpty());
        assertTrue(json.contains("\"kind\": \"attribute\", \"text\": \"leeftijd\", \"declaredAt\": {\"file\": \"jutters.rgs\""),
                () -> json.substring(0, Math.min(2000, json.length())));
    }

    @Test void the_cli_writes_the_file_also_with_an_error_and_refuses_wrong_usage() throws Exception {
        Path dir = Files.createTempDirectory("semantic-tokens");
        Path declarations = dir.resolve("jutters.rgs"), rules = dir.resolve("regels.rgs"), out = dir.resolve("tokens.json");
        Files.writeString(declarations, DECLARATIONS);
        Files.writeString(rules, RULES + "\nRegel kapot\n    geldig altijd\n        De naam van een Jutter moet\n");
        PrintStream err = System.err, stdout = System.out;
        ByteArrayOutputStream sink = new ByteArrayOutputStream();
        try {
            System.setErr(new PrintStream(sink));
            System.setOut(new PrintStream(sink));
            assertEquals(0, Resolver.tokensMode(List.of(out.toString(), declarations.toString(), rules.toString())));
            assertEquals(2, Resolver.tokensMode(List.of(out.toString())));
        } finally {
            System.setErr(err);
            System.setOut(stdout);
        }
        String json = Files.readString(out);
        assertTrue(json.contains("\"code\": \"RGS-091\""), "the syntax error is in the output");
        assertTrue(json.contains("\"file\": \"" + rules + "\""), "the files carry the names given");
    }
}
