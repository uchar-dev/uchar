package dev.uchar.regelspraak;

import dev.uchar.regelspraak.resolver.Resolver;
import dev.uchar.regelspraak.resolver.SemanticTokens;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * The semantic tokens are written on a syntax error too (grammar/DIVERGENCES.md, "Semantic tokens"), where an author in
 * the middle of an edit needs them most, so the resolution must come through ANTLR's recovery. Seven sources broken off
 * where the recovery leaves a parse node without a child each crashed the resolution — and with it {@code --ir} — on a
 * missing token until the card semantic-tokens (2026-10-22); each now reports its syntax error and gives the tokens. A
 * sweep over every golden, cut at forty places and with each of twenty lines left out in turn, keeps the rest of the
 * resolution honest on recovered trees: no exception, and the output passes {@link SemanticTokensCheck}. A second sweep
 * does the same to the {@code scenario.rgs} of every golden test document, read beside its {@code source.rgs}, so the
 * scenario layer comes through the recovery too ({@link ScenarioRecoveryTest} pins the forms that failed there).
 */
class SemanticTokensRecoveryTest {

    /** A source broken off where the recovery leaves a node without a child, by what is missing. */
    private static final Map<String, String> BROKEN = Map.ofEntries(
            Map.entry("a_conversion_without_its_number", """
            Eenheidsysteem Tijd
                de dag dg
                de week wk =
            """),
            Map.entry("an_allocation_rounding_without_its_number", """
            Objecttype de Berging (mv: Bergingen)
                de opbrengst Numeriek (getal met 2 decimalen);

            Objecttype de Berger (mv: Bergers) (bezield)
                het bergingsaandeel Numeriek (getal met 2 decimalen);
                het geborgen gewicht Numeriek (getal met 2 decimalen);

            Feittype bergingsdeelname
                de berging Berging
                de berger (mv: bergers) Berger
            Eén berging heeft meerdere bergers

            Regel verdeling van de opbrengst
                geldig altijd
                    De opbrengst van een Berging wordt verdeeld over de bergingsaandelen van alle bergers van de berging, waarbij wordt verdeeld:
                    - naar rato van het geborgen gewicht,
                    - afgerond op
            """),
            Map.entry("a_nested_condition_without_its_bullets", """
            Objecttype de Vondst (mv: Vondsten)
                de lengte Numeriek (geheel getal);
                is keurig kenmerk (bijvoeglijk);

            Regel keurigheid van een vondst
                geldig altijd
                    Een Vondst is keurig
                    indien hij aan precies één van de volgende voorwaarden voldoet:
                    - zijn lengte is groter dan 20
                    - hij voldoet aan te
            """),
            Map.entry("a_criteria_header_without_its_bullets", """
            Objecttype de Vondst (mv: Vondsten)
                de lengte Numeriek (geheel getal);

            Regel keuring van een vondst
                geldig altijd
                    Een Vondst moet voldoen aan precies één van de volgende criteria:
                    - zijn lengte is groter dan 20
                    - er
            """),
            Map.entry("a_week_timeline_without_its_start_date", """
            Objecttype de Jutter (mv: Jutters) (bezield)
                is wachtplichtig kenmerk (bijvoeglijk) voor elke week sta
            """),
            Map.entry("a_quarter_timeline_without_its_months", """
            Objecttype de Jutter (mv: Jutters) (bezield)
                de kwartaaltoelage Numeriek (geheel getal) voor elk kwartaal startend op dag 31
            """),
            Map.entry("a_unit_without_its_word", """
            Eenheidsysteem Rentetijd
                de (mv: dagen) dg
                het jaar (mv: jaren) jr

            Parameter het rentejaar : Numeriek (geheel getal) met eenheid dg/jr;
            """),
            Map.entry("a_start_point_rule_without_its_subject", """
            Tijdlijn de bergingsperiode heeft tijdvakken van een maand met variabel startpunt;

            Objecttype de Jutter (mv: Jutters) (bezield)
                de aanmelddatum Datum in dagen;

            Regel startpunt van de bergingsperiode
                geldig altijd
                    Het startpunt van de bergingsperiode voor
            """));

    @Test void a_broken_source_reports_its_syntax_error_and_gives_its_tokens() {
        for (var e : BROKEN.entrySet()) {
            assertFalse(Resolver.resolve(e.getValue()).isOk(), () -> e.getKey() + ": the resolution reports an ERROR");
            List<SemanticTokens.SourceFile> files = List.of(new SemanticTokens.SourceFile(e.getKey() + ".rgs", e.getValue()));
            SemanticTokens.Document d = Resolver.tokens(files);
            assertTrue(d.diagnostics().stream().anyMatch(x -> "RGS-091".equals(x.code())),
                    () -> e.getKey() + ": the syntax error is in the output: " + d.diagnostics());
            SemanticTokensCheck.check(files, d);
        }
    }

    /** A unit line the recovery reads without its word: its abbreviation declares the unit, and the plural and a use
     *  point at it. */
    @Test void a_unit_line_without_its_word_is_declared_by_its_abbreviation() {
        List<SemanticTokens.SourceFile> files = List.of(
                new SemanticTokens.SourceFile("unit.rgs", BROKEN.get("a_unit_without_its_word")));
        SemanticTokens.Document d = Resolver.tokens(files);
        List<SemanticTokens.Span> spans = d.files().get(0).spans();
        SemanticTokens.Span declared = spans.stream().filter(s -> s.text().equals("dg") && s.declaration()).findFirst()
                .orElseThrow(() -> new AssertionError("no declaring span 'dg': " + spans));
        for (String name : List.of("dagen", "dg"))
            assertTrue(spans.stream().anyMatch(s -> s.text().equals(name) && !s.declaration() && s.declaredAt() != null
                            && s.declaredAt().line() == declared.line() && s.declaredAt().column() == declared.column()),
                    () -> "'" + name + "' points at the declaration of dg: " + spans);
    }

    /** An expectation 'staat niet vast' that the recovery cut short or into which it took another word: the tokens
     *  come with the syntax error, and a word that failed the predicate of 'staat' or 'vast' keeps its lexical kind
     *  where the recovery took it into the expectation. Measured on the frontend before: '- of' ended in a
     *  NullPointerException, and 'zeker' and 'blijft' were keywords. */
    @Test void an_open_expectation_the_recovery_cut_short_gives_its_tokens() {
        String model = """
                Afleiden uit leeg: nee

                Objecttype de Vondst (mv: Vondsten)
                    de afhaaldatum Datum in dagen;
                    is drijfhout kenmerk (bijvoeglijk);
                    het strandrecht kenmerk (bezittelijk);
                """;
        List<Map.Entry<String, List<String>>> words = List.of(
                Map.entry("- of", List.of("of keyword")),
                Map.entry("- of V1 strandrecht", List.of("of keyword", "V1 label", "strandrecht characteristic")),
                Map.entry("- de afhaaldatum van V1 staat niet zeker",
                        List.of("de keyword", "afhaaldatum attribute", "van keyword", "V1 label", "staat keyword",
                                "niet keyword", "zeker word")),
                Map.entry("- of V1 drijfhout is, blijft niet vast",
                        List.of("of keyword", "V1 label", "drijfhout characteristic", "is keyword", ", punctuation",
                                "blijft word", "niet keyword", "vast keyword")));
        for (var e : words) {
            String scenario = """
                    Scenario open
                    Gegeven is de volgende situatie:
                    een Vondst V1
                    Met als verwacht resultaat:
                    """ + e.getKey() + "\n- de gegevens zijn consistent\n";
            List<SemanticTokens.SourceFile> files = List.of(new SemanticTokens.SourceFile("model.rgs", model),
                    new SemanticTokens.SourceFile("toets.rgs", scenario));
            SemanticTokens.Document d = Resolver.tokens(files);
            assertTrue(d.diagnostics().stream().anyMatch(x -> "RGS-216".equals(x.code())),
                    () -> e.getKey() + ": the syntax error is in the output: " + d.diagnostics());
            SemanticTokensCheck.check(files, d);
            List<String> line = new ArrayList<>();
            for (SemanticTokens.Span s : d.files().get(1).spans())
                if (s.line() == 5 && !s.text().equals("-")) line.add(s.text() + " " + s.kind().wire());
            assertEquals(e.getValue(), line, e.getKey());
        }
    }

    @Test void an_attribute_bullet_the_recovery_cut_short_gives_its_tokens() {
        String model = """
                Afleiden uit leeg: nee

                Objecttype de Vondst (mv: Vondsten)
                    de afhaaldatum Datum in dagen;
                """;
        for (String broken : List.of("- de", "- de afhaaldatum", "- de afhaaldatum is",
                "- de afhaaldatum is onbekend", "- de afhaaldatum is leeg van")) {
            String scenario = """
                    Scenario afgebroken
                    Gegeven is de volgende situatie:
                    een Vondst V1
                    """ + broken + "\nMet als verwacht resultaat:\n- de gegevens zijn consistent\n";
            List<SemanticTokens.SourceFile> files = List.of(new SemanticTokens.SourceFile("model.rgs", model),
                    new SemanticTokens.SourceFile("toets.rgs", scenario));
            SemanticTokens.Document d = Resolver.tokens(files);
            assertTrue(d.diagnostics().stream().anyMatch(x -> "RGS-216".equals(x.code())),
                    () -> broken + ": the syntax error is in the output: " + d.diagnostics());
            SemanticTokensCheck.check(files, d);
            if (broken.contains("is leeg")) {
                SemanticTokens.Span attr = d.files().get(1).spans().stream()
                        .filter(s -> s.text().equals("afhaaldatum")).findFirst().orElseThrow();
                assertEquals(SemanticTokens.Kind.ATTRIBUTE, attr.kind(), broken);
                assertNotNull(attr.declaredAt(), broken);
            }
            if (broken.endsWith("onbekend")) {
                SemanticTokens.Span last = d.files().get(1).spans().stream()
                        .filter(s -> s.text().equals("onbekend")).findFirst().orElseThrow();
                assertEquals(SemanticTokens.Kind.WORD, last.kind());
            }
        }
    }

    /** Floors, measured at the build (card semantic-tokens, 2026-10-22): the cut sources read, and how many of them
     *  carry a syntax error, so the sweep keeps reaching the recovery. */
    private static final int MIN_RUNS = 7620;
    private static final int MIN_SYNTAX_ERRORS = 4699;

    @Test void every_golden_cut_or_with_a_line_left_out_gives_its_tokens() throws IOException {
        Path corpus = corpus();
        List<String> problems = new ArrayList<>();
        int runs = 0, syntaxErrors = 0;
        for (Path source : files(corpus, "source.rgs")) {
            String text = Files.readString(source, StandardCharsets.UTF_8);
            for (String v : variants(text)) {
                runs++;
                List<SemanticTokens.SourceFile> files = List.of(new SemanticTokens.SourceFile("cut.rgs", v));
                try {
                    SemanticTokens.Document d = Resolver.tokens(files);
                    SemanticTokensCheck.check(files, d);
                    if (d.diagnostics().stream().anyMatch(x -> "RGS-091".equals(x.code()))) syntaxErrors++;
                } catch (AssertionError | RuntimeException e) {
                    problems.add(corpus.relativize(source) + " (" + v.length() + " characters): " + e);
                }
            }
        }
        assertTrue(problems.isEmpty(), () -> problems.size() + " problem(s):\n  " + String.join("\n  ", problems));
        int r = runs, s = syntaxErrors;
        assertTrue(r >= MIN_RUNS, () -> "cut sources " + r + " < floor " + MIN_RUNS);
        assertTrue(s >= MIN_SYNTAX_ERRORS, () -> "cut sources with a syntax error " + s + " < floor " + MIN_SYNTAX_ERRORS);
    }

    /** Floors, measured at the build (card scenario-recovery, 2026-11-07): the cut test documents read, and how many of
     *  them carry a syntax error, so the sweep keeps reaching the recovery of the scenario layer. */
    private static final int MIN_SCENARIO_RUNS = 2580;
    private static final int MIN_SCENARIO_SYNTAX_ERRORS = 1715;

    @Test void every_golden_test_document_with_its_scenarios_cut_or_with_a_line_left_out_gives_its_tokens()
            throws IOException {
        Path corpus = corpus();
        List<String> problems = new ArrayList<>();
        int runs = 0, syntaxErrors = 0;
        for (Path scenario : files(corpus, "scenario.rgs")) {
            Path sourcePath = scenario.resolveSibling("source.rgs");
            if (!Files.isRegularFile(sourcePath)) fail(corpus.relativize(scenario) + " has no source.rgs beside it");
            String source = Files.readString(sourcePath, StandardCharsets.UTF_8);
            String text = Files.readString(scenario, StandardCharsets.UTF_8);
            for (String v : variants(text)) {
                runs++;
                List<SemanticTokens.SourceFile> files = List.of(new SemanticTokens.SourceFile("source.rgs", source),
                        new SemanticTokens.SourceFile("scenario.rgs", v));
                try {
                    SemanticTokens.Document d = Resolver.tokens(files);
                    SemanticTokensCheck.check(files, d);
                    if (d.diagnostics().stream().anyMatch(x -> "RGS-216".equals(x.code()))) syntaxErrors++;
                } catch (AssertionError | RuntimeException e) {
                    problems.add(corpus.relativize(scenario) + " (" + v.length() + " characters): " + e);
                }
            }
        }
        assertTrue(problems.isEmpty(), () -> problems.size() + " problem(s):\n  " + String.join("\n  ", problems));
        int r = runs, s = syntaxErrors;
        assertTrue(r >= MIN_SCENARIO_RUNS, () -> "cut test documents " + r + " < floor " + MIN_SCENARIO_RUNS);
        assertTrue(s >= MIN_SCENARIO_SYNTAX_ERRORS,
                () -> "cut test documents with a syntax error " + s + " < floor " + MIN_SCENARIO_SYNTAX_ERRORS);
    }

    /** {@code text} cut at forty places, and with each of twenty lines left out in turn. */
    private static List<String> variants(String text) {
        List<String> variants = new ArrayList<>();
        for (int k = 1; k <= 40; k++) variants.add(text.substring(0, text.length() * k / 41));
        String[] lines = text.split("\n", -1);
        for (int k = 1; k <= 20; k++) {
            List<String> rest = new ArrayList<>(Arrays.asList(lines));
            rest.remove(lines.length * k / 21);
            variants.add(String.join("\n", rest));
        }
        return variants;
    }

    private static Path corpus() {
        for (Path p = Path.of("").toAbsolutePath(); p != null; p = p.getParent())
            if (Files.isDirectory(p.resolve("corpus/golden"))) return p.resolve("corpus/golden");
        return fail("kon corpus/golden niet vinden vanaf " + Path.of("").toAbsolutePath());
    }

    private static List<Path> files(Path corpus, String name) throws IOException {
        try (Stream<Path> s = Files.walk(corpus)) {
            return s.filter(p -> p.getFileName().toString().equals(name)).sorted().toList();
        }
    }
}
