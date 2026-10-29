package dev.uchar.regelspraak;

import dev.uchar.regelspraak.resolver.Resolver;
import dev.uchar.regelspraak.resolver.SemanticTokens;
import dev.uchar.regelspraak.resolver.SemanticTokens.Kind;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * The semantic tokens over every {@code corpus/golden/**}{@code /source.rgs} (grammar/DIVERGENCES.md, "Semantic
 * tokens"), each read as one document: the coverage and the positions of {@link SemanticTokensCheck}; every reference
 * points at a declaration of its kind, and every reference whose kind has declarations carries one; a document without
 * diagnostics has no span {@code unresolved}; and a span {@code word}, which only ANTLR's recovery leaves, stands only
 * in a document with a syntax error. A golden that may break the third rule stands in {@link #UNRESOLVED_KNOWN} with
 * its reason, and the list fails when an entry no longer applies. All problems are gathered before the test fails, so
 * one run names every one of them.
 */
class SemanticTokensCorpusTest {

    /** Goldens without diagnostics that keep a span {@code unresolved}, each with the reason. */
    private static final Map<String, String> UNRESOLVED_KNOWN = Map.of();

    /** Floors, measured at the build (card semantic-tokens, 2026-10-22): the goldens read, the spans written, and the
     *  name spans per kind. A floor catches shrink, not growth: a corpus card adds goldens without touching them. A
     *  change that moves names from one kind to another lowers the floor it falls under in the same commit. */
    private static final int MIN_FILES = 127;
    private static final int MIN_SPANS = 18813;
    private static final Map<Kind, Integer> MIN_NAMES = new EnumMap<>(Map.ofEntries(
            Map.entry(Kind.OBJECT_TYPE, 1441), Map.entry(Kind.ATTRIBUTE, 1332), Map.entry(Kind.CHARACTERISTIC, 351),
            Map.entry(Kind.ROLE, 733), Map.entry(Kind.FACT_TYPE, 98), Map.entry(Kind.PARAMETER, 43),
            Map.entry(Kind.DOMAIN, 8), Map.entry(Kind.ENUMERATION_VALUE, 17), Map.entry(Kind.UNIT_SYSTEM, 33),
            Map.entry(Kind.UNIT, 243), Map.entry(Kind.DIMENSION, 9), Map.entry(Kind.DIMENSION_LABEL, 13),
            Map.entry(Kind.TIMELINE, 5), Map.entry(Kind.DAY_KIND, 28), Map.entry(Kind.VOCABULARY, 48),
            Map.entry(Kind.NAMED_FORM, 489), Map.entry(Kind.RULE_GROUP, 4), Map.entry(Kind.VARIABLE, 31)));

    private static Path corpus() {
        for (Path p = Path.of("").toAbsolutePath(); p != null; p = p.getParent()) {
            Path candidate = p.resolve("corpus/golden");
            if (Files.isDirectory(candidate)) return candidate;
        }
        return fail("kon corpus/golden niet vinden vanaf " + Path.of("").toAbsolutePath()
                + " — deze getuige leest het corpus en mag niet overslaan als hij het niet vindt");
    }

    @Test void every_golden_source_is_covered_and_resolved() throws IOException {
        Path corpus = corpus();
        List<Path> sources;
        try (Stream<Path> s = Files.walk(corpus)) {
            sources = s.filter(p -> p.getFileName().toString().equals("source.rgs")).sorted().toList();
        }
        List<String> problems = new ArrayList<>();
        Map<String, Integer> unresolvedSeen = new TreeMap<>();
        Map<Kind, Integer> names = new EnumMap<>(Kind.class);
        int spans = 0;
        for (Path source : sources) {
            String golden = corpus.relativize(source.getParent()).toString();
            List<SemanticTokens.SourceFile> files = List.of(new SemanticTokens.SourceFile(golden + "/source.rgs",
                    Files.readString(source, StandardCharsets.UTF_8)));
            SemanticTokens.Document d;
            try {
                d = Resolver.tokens(files);
                SemanticTokensCheck.check(files, d);
            } catch (AssertionError | RuntimeException e) {
                problems.add(golden + ": " + e);
                continue;
            }
            boolean syntaxError = d.diagnostics().stream().anyMatch(x -> "RGS-091".equals(x.code()));
            for (SemanticTokens.Span s : d.files().get(0).spans()) {
                spans++;
                if (s.kind().names()) names.merge(s.kind(), 1, Integer::sum);
                String where = golden + ":" + s.line() + ":" + s.column() + " '" + s.text() + "'";
                if (SemanticTokensCheck.unlinked(s)) problems.add(where + ": a " + s.kind().wire() + " without its declaration");
                if (s.kind() == Kind.WORD && !syntaxError) problems.add(where + ": a word outside every name, without a syntax error");
                if (s.kind() == Kind.UNRESOLVED && d.diagnostics().isEmpty()) {
                    unresolvedSeen.merge(golden, 1, Integer::sum);
                    if (!UNRESOLVED_KNOWN.containsKey(golden)) problems.add(where + ": unresolved in a document without diagnostics");
                }
            }
        }
        for (String known : UNRESOLVED_KNOWN.keySet())
            if (!unresolvedSeen.containsKey(known))
                problems.add("UNRESOLVED_KNOWN is stale: " + known + " has no span unresolved (any more)");
        System.out.println("semantic tokens over the corpus: files=" + sources.size() + " spans=" + spans + " names=" + names);
        assertTrue(problems.isEmpty(), () -> problems.size() + " problem(s):\n  " + String.join("\n  ", problems));
        assertTrue(sources.size() >= MIN_FILES, () -> "goldens read " + sources.size() + " < floor " + MIN_FILES);
        int total = spans;
        assertTrue(total >= MIN_SPANS, () -> "spans " + total + " < floor " + MIN_SPANS);
        for (var floor : MIN_NAMES.entrySet()) {
            int n = names.getOrDefault(floor.getKey(), 0);
            assertTrue(n >= floor.getValue(), () -> "name spans " + floor.getKey().wire() + " " + n + " < floor " + floor.getValue());
        }
    }

    /** Floors of the test documents, measured at the build (card semantic-tokens-scenario-layer, 2026-10-29): the
     *  goldens with a {@code scenario.rgs}, and the name spans of the kinds of the scenario layer in them. */
    private static final int MIN_TEST_DOCUMENTS = 36;
    private static final Map<Kind, Integer> MIN_SCENARIO_NAMES = new EnumMap<>(Map.ofEntries(
            Map.entry(Kind.LABEL, 1558), Map.entry(Kind.SITUATION, 6), Map.entry(Kind.PARAMETER_SET, 8),
            Map.entry(Kind.SCENARIO, 126)));

    /** Every golden {@code scenario.rgs} with its {@code source.rgs}, read as one test document as gate 16 reads the
     *  two ({@code --cases}): the properties of a production document above, no ERROR (the goldens lower to their
     *  case files), and so no span {@code unresolved} and no {@code word}. */
    @Test void every_golden_test_document_is_covered_and_resolved() throws IOException {
        Path corpus = corpus();
        List<Path> scenarios;
        try (Stream<Path> s = Files.walk(corpus)) {
            scenarios = s.filter(p -> p.getFileName().toString().equals("scenario.rgs")).sorted().toList();
        }
        List<String> problems = new ArrayList<>();
        Map<Kind, Integer> names = new EnumMap<>(Kind.class);
        int spans = 0;
        for (Path scenario : scenarios) {
            String golden = corpus.relativize(scenario.getParent()).toString();
            List<SemanticTokens.SourceFile> files = List.of(
                    new SemanticTokens.SourceFile(golden + "/source.rgs",
                            Files.readString(scenario.resolveSibling("source.rgs"), StandardCharsets.UTF_8)),
                    new SemanticTokens.SourceFile(golden + "/scenario.rgs", Files.readString(scenario, StandardCharsets.UTF_8)));
            SemanticTokens.Document d;
            try {
                d = Resolver.tokens(files);
                SemanticTokensCheck.check(files, d);
            } catch (AssertionError | RuntimeException e) {
                problems.add(golden + ": " + e);
                continue;
            }
            for (SemanticTokens.FileDiagnostic x : d.diagnostics())
                if (x.severity() == dev.uchar.regelspraak.model.RegelSpraakModel.Severity.ERROR)
                    problems.add(golden + ": an ERROR " + x.code() + " " + x.file() + ":" + x.line() + ": " + x.message());
            for (SemanticTokens.FileSpans fs : d.files())
                for (SemanticTokens.Span s : fs.spans()) {
                    spans++;
                    if (s.kind().names()) names.merge(s.kind(), 1, Integer::sum);
                    String where = fs.file() + ":" + s.line() + ":" + s.column() + " '" + s.text() + "'";
                    if (SemanticTokensCheck.unlinked(s)) problems.add(where + ": a " + s.kind().wire() + " without its declaration");
                    if (s.kind() == Kind.WORD) problems.add(where + ": a word outside every name");
                    if (s.kind() == Kind.UNRESOLVED) problems.add(where + ": unresolved in a golden test document");
                }
        }
        System.out.println("semantic tokens over the test documents: goldens=" + scenarios.size() + " spans=" + spans
                + " names=" + names);
        assertTrue(problems.isEmpty(), () -> problems.size() + " problem(s):\n  " + String.join("\n  ", problems));
        assertTrue(scenarios.size() >= MIN_TEST_DOCUMENTS,
                () -> "test documents read " + scenarios.size() + " < floor " + MIN_TEST_DOCUMENTS);
        for (var floor : MIN_SCENARIO_NAMES.entrySet()) {
            int n = names.getOrDefault(floor.getKey(), 0);
            assertTrue(n >= floor.getValue(), () -> "name spans " + floor.getKey().wire() + " " + n + " < floor " + floor.getValue());
        }
    }
}
