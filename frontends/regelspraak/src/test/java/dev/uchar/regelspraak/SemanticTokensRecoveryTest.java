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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * The semantic tokens are written on a syntax error too (grammar/DIVERGENCES.md, "Semantic tokens"), where an author in
 * the middle of an edit needs them most, so the resolution must come through ANTLR's recovery. Seven sources broken off
 * where the recovery leaves a parse node without a child each crashed the resolution — and with it {@code --ir} — on a
 * missing token until the card semantic-tokens (2026-10-22); each now reports its syntax error and gives the tokens. A
 * sweep over every golden, cut at forty places and with each of twenty lines left out in turn, keeps the rest of the
 * resolution honest on recovered trees: no exception, and the output passes {@link SemanticTokensCheck}.
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

    /** Floors, measured at the build (card semantic-tokens, 2026-10-22): the cut sources read, and how many of them
     *  carry a syntax error, so the sweep keeps reaching the recovery. */
    private static final int MIN_RUNS = 7620;
    private static final int MIN_SYNTAX_ERRORS = 4699;

    @Test void every_golden_cut_or_with_a_line_left_out_gives_its_tokens() throws IOException {
        Path corpus = null;
        for (Path p = Path.of("").toAbsolutePath(); p != null && corpus == null; p = p.getParent())
            if (Files.isDirectory(p.resolve("corpus/golden"))) corpus = p.resolve("corpus/golden");
        if (corpus == null) fail("kon corpus/golden niet vinden vanaf " + Path.of("").toAbsolutePath());
        List<Path> sources;
        try (Stream<Path> s = Files.walk(corpus)) {
            sources = s.filter(p -> p.getFileName().toString().equals("source.rgs")).sorted().toList();
        }
        List<String> problems = new ArrayList<>();
        int runs = 0, syntaxErrors = 0;
        for (Path source : sources) {
            String text = Files.readString(source, StandardCharsets.UTF_8);
            List<String> variants = new ArrayList<>();
            for (int k = 1; k <= 40; k++) variants.add(text.substring(0, text.length() * k / 41));
            String[] lines = text.split("\n", -1);
            for (int k = 1; k <= 20; k++) {
                List<String> rest = new ArrayList<>(Arrays.asList(lines));
                rest.remove(lines.length * k / 21);
                variants.add(String.join("\n", rest));
            }
            for (String v : variants) {
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
}
