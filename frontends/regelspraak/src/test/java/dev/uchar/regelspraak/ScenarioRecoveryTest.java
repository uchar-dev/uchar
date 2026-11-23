package dev.uchar.regelspraak;

import dev.uchar.regelspraak.model.RegelSpraakModel.Diagnostic;
import dev.uchar.regelspraak.resolver.Resolver;
import dev.uchar.regelspraak.resolver.ScenarioResolver;
import dev.uchar.regelspraak.resolver.ScenarioResolver.ScenarioBundle;
import dev.uchar.regelspraak.resolver.SemanticTokens;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * A test document with a syntax error in its scenario layer: both routes that read one, {@code --cases} and the
 * semantic tokens, report the syntax error (RGS-216) and return. ANTLR's recovery leaves a context out where a match
 * failed before it (a situation without its block, an opener without its label) or makes a token up (an error node
 * outside the token stream: a label, the 'van' of a marker); the scenario layer reads such a tree as far as it goes.
 * Measured on the frontend before: each of the four documents below ended in an uncaught exception
 * (NullPointerException, ArrayIndexOutOfBoundsException, IllegalStateException) on one route or both; the expectations
 * 'staat niet vast' after them did the same or brought a finding beside the syntax error.
 */
class ScenarioRecoveryTest {

    private static final String MODEL = """
        Objecttype de Vondst (mv: Vondsten)
            de taxatie Numeriek (getal met 2 decimalen) (of onbekend of niet van toepassing);

        """;

    private static final String SCENARIO = """
        Situatie wrak:
        een Vondst V
        - de taxatie is 3

        Scenario wrak
        gebaseerd op de situatie wrak
        Met als verwacht resultaat:
        - de taxatie van V is 3
        """;

    @Test void the_document_without_a_syntax_error_resolves_clean() {
        assertEquals(List.of(), codes(ScenarioResolver.resolve(MODEL + SCENARIO).model().errors()));
        assertEquals(List.of(), tokens(SCENARIO).diagnostics());
    }

    @Test void a_situation_header_without_its_colon_is_a_syntax_error() {
        syntaxErrorOnBothRoutes(SCENARIO.replace("Situatie wrak:", "Situatie wrak"));
    }

    @Test void an_opener_whose_label_the_recovery_leaves_out_is_a_syntax_error() {
        syntaxErrorOnBothRoutes(SCENARIO.replace("- de taxatie is 3", "de taxatie is 3"));
    }

    @Test void an_opener_whose_label_the_recovery_makes_up_is_a_syntax_error() {
        syntaxErrorOnBothRoutes(SCENARIO.replace("een Vondst V\n", "een Vondst\n"));
        syntaxErrorOnBothRoutes(SCENARIO.replace("een Vondst V\n", "een V\n"));
    }

    /** The recovery reads 'niet toepassing' as the marker 'niet van toepassing' with a made-up 'van': the syntax error
     *  is the one finding, and no refusal of a marker the declaration lacks (RGS-377) stands beside it. */
    @Test void a_marker_with_a_word_the_recovery_makes_up_is_a_syntax_error_only() {
        List<String> errors = syntaxErrorOnBothRoutes(
                SCENARIO.replace("- de taxatie is 3", "- de taxatie is niet toepassing"));
        assertEquals(List.of("RGS-216"), errors);
    }

    private static final String OPEN_MODEL = """
        Objecttype de Vondst (mv: Vondsten)
            de afhaaldatum Datum in dagen;
            is drijfhout kenmerk (bijvoeglijk);
            het strandrecht kenmerk (bezittelijk);

        """;

    private static final String OPEN_SCENARIO = """
        Scenario open
        Gegeven is de volgende situatie:
        een Vondst V1
        Met als verwacht resultaat:
        BULLET
        - de gegevens zijn consistent
        """;

    /** An expectation 'staat niet vast' that the recovery cut short or into which it took another word: the syntax
     *  error is the one finding, in a document with 'Afleiden uit leeg: nee' and in one without, where a whole one is
     *  refused (RGS-467). Measured on the frontend before: '- of' ended in a NullPointerException on both routes, '- of
     *  V1 strandrecht' brought RGS-244 beside the syntax error in both documents, and the other two RGS-467 in the
     *  document without the line. */
    @Test void an_open_expectation_the_recovery_cut_short_is_a_syntax_error_only() {
        for (String model : List.of("Afleiden uit leeg: nee\n\n" + OPEN_MODEL, OPEN_MODEL))
            for (String bullet : List.of("- of", "- of V1 strandrecht", "- de afhaaldatum van V1 staat niet zeker",
                    "- of V1 drijfhout is, blijft niet vast"))
                assertEquals(List.of("RGS-216"), syntaxErrorOnBothRoutes(model, OPEN_SCENARIO.replace("BULLET", bullet)),
                        () -> bullet + " in\n" + model);
    }

    /** Both routes return on {@code scenario} and report a syntax error; returns the codes of the ERRORs of
     *  {@code --cases}. */
    private static List<String> syntaxErrorOnBothRoutes(String scenario) {
        return syntaxErrorOnBothRoutes(MODEL, scenario);
    }

    private static List<String> syntaxErrorOnBothRoutes(String model, String scenario) {
        ScenarioBundle b = ScenarioResolver.resolve(model + scenario);
        assertFalse(b.isOk());
        List<String> errors = codes(b.model().errors());
        assertTrue(errors.contains("RGS-216"), () -> "a syntax error: " + errors);
        SemanticTokens.Document d = tokens(model, scenario);
        assertTrue(d.diagnostics().stream().anyMatch(x -> "RGS-216".equals(x.code()) && "toets.rgs".equals(x.file())),
                () -> "a syntax error in toets.rgs: " + d.diagnostics());
        return errors;
    }

    private static SemanticTokens.Document tokens(String scenario) {
        return tokens(MODEL, scenario);
    }

    private static SemanticTokens.Document tokens(String model, String scenario) {
        return Resolver.tokens(List.of(new SemanticTokens.SourceFile("model.rgs", model),
                new SemanticTokens.SourceFile("toets.rgs", scenario)));
    }

    private static List<String> codes(List<Diagnostic> diagnostics) {
        return diagnostics.stream().map(Diagnostic::code).toList();
    }
}
