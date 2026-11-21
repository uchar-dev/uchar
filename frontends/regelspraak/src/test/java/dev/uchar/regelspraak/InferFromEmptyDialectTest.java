package dev.uchar.regelspraak;

import dev.uchar.regelspraak.model.RegelSpraakModel;
import dev.uchar.regelspraak.model.RegelSpraakModel.Diagnostic;
import dev.uchar.regelspraak.parser.RegelSpraakLexer;
import dev.uchar.regelspraak.resolver.Resolver;
import dev.uchar.regelspraak.resolver.SemanticTokens;
import dev.uchar.regelspraak.resolver.SemanticTokens.Kind;
import dev.uchar.regelspraak.resolver.SemanticTokens.Span;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.Token;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The line {@code Afleiden uit leeg: nee} / {@code Afleiden uit leeg: ja} (grammar/DIVERGENCES.md, "Afleiden uit
 * leeg"): {@code nee} writes {@code inferFromEmpty: false} on the root of the document, directly after
 * {@code format}; {@code ja} and no line write the same document byte for byte; the line holds for the whole document,
 * in whichever file of a chain it stands; a second line is RGS-462; the line has its semantic tokens; and the IR
 * document of the scenario layer keeps the property, with and without a scope. Measured before this card: every
 * document below that writes the line was a syntax error on it.
 */
class InferFromEmptyDialectTest {

    private static final Path GOLDEN = Path.of("..", "..", "corpus", "golden", "jutterswet-afleiden-uit-leeg");

    private static final String NEE = "Afleiden uit leeg: nee\n";

    private static final String MODEL = """
            Objecttype de Vondst (mv: Vondsten)
                de taxatie Numeriek (geheel getal);
                het gewicht Numeriek (geheel getal);
                het vindloon Numeriek (geheel getal);
                is kostbaar kenmerk (bijvoeglijk);

            Regel kostbaarheid
                geldig altijd
                    Een Vondst is kostbaar indien zijn taxatie groter is dan 1000.

            Regel vindloon
                geldig altijd
                    Het vindloon van een Vondst moet berekend worden als zijn gewicht plus 10.
            """;

    private static final String SCENARIO = """
            Scenario een kostbare vondst
            Gegeven is de volgende situatie:
            een Vondst V1
            - de taxatie is 1500
            - het gewicht is 3
            Met als verwacht resultaat:
            - V1 is kostbaar
            - het vindloon van V1 is 13
            """;

    private static final String ROOT_WITH = "{\n  \"format\": \"uchar-ir-v1\",\n  \"inferFromEmpty\": false,\n  \"model\": {";
    private static final String ROOT_WITHOUT = "{\n  \"format\": \"uchar-ir-v1\",\n  \"model\": {";

    private static String emitted(String source) {
        RegelSpraakModel m = Resolver.resolve(source);
        assertTrue(m.isOk(), () -> m.errors().toString());
        return m.document();
    }

    // ---------------- the document ----------------

    @Test void nee_writes_the_property_directly_after_format() throws Exception {
        String source = Files.readString(GOLDEN.resolve("source.rgs"));
        assertTrue(source.startsWith(NEE), "the golden writes the line");
        String ir = emitted(source);
        assertTrue(ir.startsWith(ROOT_WITH), () -> ir.substring(0, 120));
        assertEquals(1, ir.split("\"inferFromEmpty\"", -1).length - 1, "the property stands once, on the root");
        assertFalse(Resolver.resolve(source).inferFromEmpty());
    }

    @Test void ja_and_no_line_write_the_same_document_byte_for_byte() {
        String without = emitted(MODEL);
        assertTrue(without.startsWith(ROOT_WITHOUT), () -> without.substring(0, 120));
        assertEquals(without, emitted("Afleiden uit leeg: ja\n\n" + MODEL));
        assertEquals(without, emitted(MODEL + "\nAfleiden uit leeg: ja;\n"), "the line may close with ';'");
        assertTrue(Resolver.resolve("Afleiden uit leeg: ja\n\n" + MODEL).inferFromEmpty());
        // the property is the whole difference: the rest of the document is the one without the line
        assertEquals(without, emitted(NEE + "\n" + MODEL).replace("\n  \"inferFromEmpty\": false,", ""));
    }

    @Test void the_line_holds_for_the_document_in_whichever_file_of_a_chain_it_stands(@TempDir Path dir)
            throws Exception {
        Path model = dir.resolve("vondst.rgs"), line = dir.resolve("keuze.rgs");
        Files.writeString(model, MODEL);
        Files.writeString(line, NEE);
        Path second = dir.resolve("second.json"), first = dir.resolve("first.json");
        assertEquals(0, Resolver.irMode(List.of(second.toString(), model.toString(), line.toString())));
        assertEquals(0, Resolver.irMode(List.of(first.toString(), line.toString(), model.toString())));
        assertTrue(Files.readString(second).startsWith(ROOT_WITH));
        assertEquals(Files.readString(first), Files.readString(second));
    }

    // ---------------- one choice per document ----------------

    private static Diagnostic twice(String source) {
        RegelSpraakModel m = Resolver.resolve(source);
        List<Diagnostic> errors = m.errors();
        assertEquals(1, errors.size(), errors::toString);
        assertEquals("RGS-462", errors.get(0).code());
        return errors.get(0);
    }

    @Test void a_second_line_is_rgs_462_on_its_own_line_also_where_it_says_the_same() {
        Diagnostic d = twice(NEE + "\n" + MODEL + "\nAfleiden uit leeg: ja\n");
        assertEquals(Map.of("first", "nee", "value", "ja"), d.params());
        assertEquals(17, d.line());
        assertEquals("'Afleiden uit leeg: ja' staat een tweede keer in het document, na 'Afleiden uit leeg: nee'; een "
                + "document kiest één keer of uit een lege waarde iets volgt, ook als het uit meer bestanden bestaat",
                d.message(Locale.forLanguageTag("nl")));
        assertEquals("'Afleiden uit leeg: ja' stands in the document a second time, after 'Afleiden uit leeg: nee'; a "
                + "document chooses once whether anything follows from an empty value, also when it consists of "
                + "several files", d.message(Locale.ENGLISH));
        assertEquals(Map.of("first", "nee", "value", "nee"), twice(NEE + NEE + MODEL).params());
    }

    @Test void a_second_line_in_another_file_of_the_chain_writes_nothing(@TempDir Path dir) throws Exception {
        Path model = dir.resolve("vondst.rgs"), line = dir.resolve("keuze.rgs"), out = dir.resolve("ir.json");
        Files.writeString(model, NEE + MODEL);
        Files.writeString(line, "Afleiden uit leeg: ja\n");
        assertEquals(1, Resolver.irMode(List.of(out.toString(), model.toString(), line.toString())));
        assertFalse(Files.exists(out));
    }

    @Test void a_value_other_than_ja_or_nee_is_a_syntax_error_on_the_line() {
        for (String value : List.of("misschien", "Nee", "onwaar")) {
            RegelSpraakModel m = Resolver.resolve("Afleiden uit leeg: " + value + "\n\n" + MODEL);
            assertFalse(m.isOk(), value);
            assertTrue(m.errors().stream().allMatch(e -> e.code().equals("RGS-091") && e.line() == 1),
                    () -> value + ": " + m.errors());
        }
    }

    /** The opener is one token of three words: 'Afleiden' alone is a name word and 'uit' and 'leeg' keep their
     *  tokens, so a source that writes the words otherwise lexes as before. */
    @Test void the_opener_is_one_token_and_reserves_no_word() {
        assertEquals(List.of("AFLEIDEN_UIT_LEEG", "COLON", "LOWER_ID"), types("Afleiden uit leeg: nee"));
        assertEquals(List.of("UPPER_ID", "UIT", "LEEG"), types("Afleiden uit\n  // a comment\n leeg"));
        assertEquals(List.of("UPPER_ID", "LOWER_ID", "UIT", "LEEG"), types("Afleiden vindloon uit leeg"));
        assertEquals(List.of("LOWER_ID", "UIT", "LEEG"), types("afleiden uit leeg"));
    }

    private static List<String> types(String source) {
        RegelSpraakLexer lexer = new RegelSpraakLexer(CharStreams.fromString(source));
        List<String> result = new ArrayList<>();
        for (Token t : lexer.getAllTokens())
            if (t.getChannel() == Token.DEFAULT_CHANNEL) result.add(RegelSpraakLexer.VOCABULARY.getSymbolicName(t.getType()));
        return result;
    }

    // ---------------- the semantic tokens ----------------

    @Test void the_line_is_a_keyword_a_colon_and_a_keyword() {
        List<SemanticTokens.SourceFile> files = List.of(new SemanticTokens.SourceFile("vondst.rgs", MODEL),
                new SemanticTokens.SourceFile("keuze.rgs", "Afleiden uit leeg: nee;\n"));
        SemanticTokens.Document d = Resolver.tokens(files);
        SemanticTokensCheck.check(files, d);
        assertTrue(d.diagnostics().isEmpty(), () -> d.diagnostics().toString());
        List<Span> spans = d.files().get(1).spans();
        assertEquals(List.of("Afleiden uit leeg", ":", "nee", ";"), spans.stream().map(Span::text).toList());
        assertEquals(List.of(Kind.KEYWORD, Kind.PUNCTUATION, Kind.KEYWORD, Kind.PUNCTUATION),
                spans.stream().map(Span::kind).toList());
        assertEquals(List.of(0, 17, 19, 22), spans.stream().map(Span::column).toList());
        assertTrue(spans.stream().allMatch(s -> s.line() == 1 && !s.declaration() && s.declaredAt() == null));
    }

    // ---------------- the scenario layer ----------------

    @Test void the_ir_document_of_a_test_document_keeps_the_property_with_and_without_a_scope(@TempDir Path dir)
            throws Exception {
        Path source = dir.resolve("vondst.rgs"), scenario = dir.resolve("scenario.rgs");
        Files.writeString(source, NEE + "\n" + MODEL);
        Files.writeString(scenario, SCENARIO);
        Path whole = dir.resolve("whole.json");
        assertEquals(0, Resolver.casesMode(List.of(whole.toString(), source.toString(), scenario.toString()),
                dir.resolve("whole")));
        assertEquals(emitted(NEE + "\n" + MODEL), Files.readString(whole),
                "without a scope, the document of the production route");

        Files.writeString(scenario, SCENARIO.replace("Scenario een kostbare vondst\n",
                "Scenario een kostbare vondst\ntoetst de regel kostbaarheid\n").replace("- het vindloon van V1 is 13\n", ""));
        Path scoped = dir.resolve("scoped.json");
        assertEquals(0, Resolver.casesMode(List.of(scoped.toString(), source.toString(), scenario.toString()),
                dir.resolve("scoped")));
        String ir = Files.readString(scoped);
        assertTrue(ir.startsWith(ROOT_WITH), () -> ir.substring(0, 120));
        java.util.regex.Matcher rules = java.util.regex.Pattern.compile("\"node\": \"Rule\",\\s*\"name\": \"([^\"]+)\"")
                .matcher(ir);
        List<String> names = new ArrayList<>();
        while (rules.find()) names.add(rules.group(1));
        assertEquals(List.of("kostbaarheid"), names, "the scope carries only the rule it names");
    }
}
