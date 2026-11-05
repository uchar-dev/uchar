package dev.uchar.regelspraak;

import dev.uchar.regelspraak.emit.CaseEmitter;
import dev.uchar.regelspraak.ir.ResolvedScenario;
import dev.uchar.regelspraak.model.RegelSpraakModel.Diagnostic;
import dev.uchar.regelspraak.resolver.Resolver;
import dev.uchar.regelspraak.resolver.ScenarioResolver;
import dev.uchar.regelspraak.resolver.ScenarioResolver.ScenarioBundle;
import dev.uchar.regelspraak.resolver.SemanticTokens;
import dev.uchar.regelspraak.resolver.SemanticTokens.Kind;
import dev.uchar.regelspraak.resolver.SemanticTokens.Span;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.*;

/**
 * An absence marker in a scenario (grammar/DIVERGENCES.md, "Afwezigheidsmarkeringen" and "Scenariotoetsen"): a
 * situation gives an attribute with markers a marker or a value, and an expectation expects one, in the words of the
 * rules ("de taxatie is onbekend", "… is niet van toepassing"). The lowering writes every value of such an attribute as
 * the constructor value of the case format: a marker as {@code {"constructor": "onbekend"}}, a value as
 * {@code {"constructor": "waarde", "payload": …}}, per segment on a timeline. A marker the attribute does not declare,
 * a marker on an attribute without markers and a marker for a parameter have no case form and are refused (RGS-377).
 * Measured on the frontend before: every marker as a value was a syntax error (RGS-216).
 */
class ScenarioAbsenceMarkerTest {

    private static final String MODEL = """
        Objecttype de Vondst (mv: Vondsten)
            de taxatie Numeriek (getal met 2 decimalen) (of onbekend of niet van toepassing);
            de keurwaarde Numeriek (geheel getal) (of niet van toepassing);
            de schatting Numeriek (getal met 2 decimalen);
            de opslagkosten Numeriek (geheel getal) voor elke maand (of onbekend);
            is onleesbaar kenmerk (bijvoeglijk);

        Parameter de drempel : Numeriek (geheel getal)

        Regel taxatie onbekend
            geldig altijd
                De taxatie van een Vondst moet gesteld worden op onbekend
                indien hij onleesbaar is.

        """;

    private static final String SCENARIO = """
        Scenario markeringen
        Gegeven is de volgende situatie:
        een Vondst V
        - de taxatie is niet van toepassing
        - de keurwaarde is 7
        - de opslagkosten is onbekend van 01-01-2025 tot 01-02-2025
        - de opslagkosten is 3 van 01-02-2025 tot 01-03-2025
        de parameter de drempel is 5
        Met als verwacht resultaat:
        - de taxatie van V is niet van toepassing
        - de keurwaarde van V is 7
        - de schatting van V is leeg
        - de opslagkosten van V is onbekend van 01-01-2025 tot 01-02-2025
        - de opslagkosten van V is 3 vanaf 01-02-2025
        - de gegevens zijn consistent
        """;

    /** Written by hand from the case schema: the constructor variant of a value, with its payload for the value. */
    private static final String CASE_FILE = """
        {
          "format": "uchar-case-v0",
          "asOfDate": null,
          "input": {
            "instances": [
              {
                "label": "V",
                "objectType": "Vondst",
                "attributes": {
                  "taxatie": {
                    "constructor": "niet van toepassing"
                  },
                  "keurwaarde": {
                    "constructor": "waarde",
                    "payload": {
                      "number": "7"
                    }
                  },
                  "opslagkosten": {
                    "timeline": [
                      {
                        "from": "2025-01-01",
                        "to": "2025-02-01",
                        "value": {
                          "constructor": "onbekend"
                        }
                      },
                      {
                        "from": "2025-02-01",
                        "to": "2025-03-01",
                        "value": {
                          "constructor": "waarde",
                          "payload": {
                            "number": "3"
                          }
                        }
                      }
                    ]
                  }
                },
                "roles": {},
                "characteristics": []
              }
            ],
            "parameters": {
              "drempel": {
                "number": "5"
              }
            }
          },
          "expected": [
            {
              "kind": "attributeValue",
              "label": "V",
              "attribute": "taxatie",
              "value": {
                "constructor": "niet van toepassing"
              }
            },
            {
              "kind": "attributeValue",
              "label": "V",
              "attribute": "keurwaarde",
              "value": {
                "constructor": "waarde",
                "payload": {
                  "number": "7"
                }
              }
            },
            {
              "kind": "attributeEmpty",
              "label": "V",
              "attribute": "schatting"
            },
            {
              "kind": "attributeTimeline",
              "label": "V",
              "attribute": "opslagkosten",
              "timeline": [
                {
                  "from": "2025-01-01",
                  "to": "2025-02-01",
                  "value": {
                    "constructor": "onbekend"
                  }
                },
                {
                  "from": "2025-02-01",
                  "value": {
                    "constructor": "waarde",
                    "payload": {
                      "number": "3"
                    }
                  }
                }
              ]
            },
            {
              "kind": "consistent"
            }
          ]
        }
        """;

    private static ResolvedScenario only(ScenarioBundle b) {
        assertTrue(b.isOk(), () -> b.model().errors().toString());
        assertEquals(1, b.scenarios().size());
        return b.scenarios().get(0);
    }

    @Test void a_marker_and_a_value_are_constructor_values_of_the_case_file() {
        assertEquals(CASE_FILE, CaseEmitter.emit(only(ScenarioResolver.resolve(MODEL + SCENARIO))));
    }

    /** The ERRORs of {@code text} on the line that holds {@code marker}, each as its code and its parameters. */
    private static List<String> errorsOn(String scenario, String marker) {
        ScenarioBundle b = ScenarioResolver.resolve(MODEL + scenario);
        assertFalse(b.isOk());
        int line = ScenarioFixture.lineOf(MODEL + scenario, marker);
        List<String> result = new ArrayList<>();
        for (Diagnostic d : b.model().errors()) if (d.line() == line) result.add(d.code() + " " + new TreeMap<>(d.args()));
        return result;
    }

    private static final String ONLY_NOT_APPLICABLE = "de declaratie laat alleen 'niet van toepassing' toe";
    private static final String NONE = "de declaratie laat geen afwezigheidsmarkering toe";

    @Test void a_marker_the_attribute_does_not_declare_is_refused() {
        assertEquals(List.of("RGS-377 {admitted=" + ONLY_NOT_APPLICABLE + ", marker=onbekend, what=het attribuut 'keurwaarde'}"),
                errorsOn(SCENARIO.replace("- de keurwaarde is 7", "- de keurwaarde is onbekend"), "keurwaarde is onbekend"));
        assertEquals(List.of("RGS-377 {admitted=" + ONLY_NOT_APPLICABLE + ", marker=onbekend, what=de verwachting voor 'keurwaarde'}"),
                errorsOn(SCENARIO.replace("- de keurwaarde van V is 7", "- de keurwaarde van V is onbekend"),
                        "keurwaarde van V is onbekend"));
    }

    @Test void a_marker_on_an_attribute_without_markers_is_refused() {
        assertEquals(List.of("RGS-377 {admitted=" + NONE + ", marker=niet van toepassing, what=de verwachting voor 'schatting'}"),
                errorsOn(SCENARIO.replace("- de schatting van V is leeg", "- de schatting van V is niet van toepassing"),
                        "schatting van V is niet"));
        assertEquals(List.of("RGS-377 {admitted=" + NONE + ", marker=onbekend, what=de parameter 'drempel'}"),
                errorsOn(SCENARIO.replace("de parameter de drempel is 5", "de parameter de drempel is onbekend"),
                        "drempel is onbekend"));
    }

    /** Only the words of a marker read as one on the place of a value: any other word there stays the syntax error it
     *  was, so no word of a document changes its reading. */
    @Test void a_word_that_is_no_marker_stays_a_syntax_error() {
        List<String> errors = errorsOn(SCENARIO.replace("- de keurwaarde is 7", "- de keurwaarde is onbekende"),
                "keurwaarde is onbekende");
        assertTrue(errors.stream().anyMatch(e -> e.startsWith("RGS-216 ")), () -> "" + errors);
        assertTrue(errors.stream().noneMatch(e -> e.startsWith("RGS-377 ")), () -> "" + errors);
    }

    /** The words of a marker are keywords of the value, as in a rule; no word stays outside a name. */
    @Test void the_words_of_a_marker_value_are_keywords() {
        List<SemanticTokens.SourceFile> files = List.of(new SemanticTokens.SourceFile("model.rgs", MODEL),
                new SemanticTokens.SourceFile("toets.rgs", SCENARIO));
        SemanticTokens.Document d = Resolver.tokens(files);
        assertTrue(d.diagnostics().isEmpty(), () -> "no diagnostic: " + d.diagnostics());
        SemanticTokensCheck.check(files, d);
        List<String> markers = new ArrayList<>();
        for (Span s : d.files().get(1).spans()) {
            assertNotEquals(Kind.WORD, s.kind(), () -> "a word outside every name: " + s);
            assertNotEquals(Kind.UNRESOLVED, s.kind(), () -> "unresolved: " + s);
            if (s.kind() == Kind.KEYWORD && (s.text().contains("onbekend") || s.text().contains("toepassing")))
                markers.add(s.text());
        }
        assertEquals(List.of("niet van toepassing", "onbekend", "niet van toepassing", "onbekend"), markers);
    }
}
