package dev.uchar.regelspraak;

import dev.uchar.regelspraak.emit.CaseEmitter;
import dev.uchar.regelspraak.ir.ResolvedScenario;
import dev.uchar.regelspraak.ir.ResolvedScenario.*;
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
import java.util.Map;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.*;

/**
 * A cell of a dimensioned attribute in a scenario (grammar/DIVERGENCES.md, "Scenariotoetsen"): a situation gives a cell
 * as an input and an expectation names one, in the forms in which the frontend reads a cell in a rule (§3.6; "§3.6 /
 * §13.4.17.51 More dimensions"): labels behind the attribute after the preposition of their dimension, side by side or
 * joined by "en" and ",", in any order, and labels of a dimension without a preposition before it. The lowering writes
 * the cells of one attribute as its {@code cells} and an expected cell as {@code cellValue} or {@code cellEmpty}, the
 * labels per position in the declared order of the attribute's dimensions (RUNTIME-SEMANTICS ch. 5.8c). A name that
 * gives no label of a dimension, that fits two cells, or a cell with a period has no case form and is refused.
 */
class ScenarioCellTest {

    private static final String MODEL = """
        Eenheidsysteem Gewicht
            het kilogram (mv: kilogrammen) kg

        Dimensie het seizoen, bestaande uit de seizoenen (na het attribuut met voorzetsel in):
            1. voorjaar
            2. late zomer
            3. najaar

        Dimensie het strand, bestaande uit de stranden (na het attribuut met voorzetsel op):
            1. noord
            2. zuid

        Dimensie de soort, bestaande uit de soorten (voor het attribuut zonder voorzetsel):
            1. natte
            2. droge

        Dimensie het getij, bestaande uit de getijden (na het attribuut met voorzetsel bij):
            1. eb
            2. vloed

        Dimensie het boekjaar, bestaande uit de boekjaren (na het attribuut met voorzetsel van):
            1. vorig boekjaar
            2. dit boekjaar

        Dimensie de herkomst, bestaande uit de herkomsten (na het attribuut met voorzetsel uit):
            1. zee
            2. rivier

        Objecttype de Jutter (mv: Jutters) (bezield)
            het vondstgewicht Numeriek (niet-negatief getal met 2 decimalen) met eenheid kg gedimensioneerd met seizoen en strand;
            de vangst Numeriek (niet-negatief getal met 2 decimalen) met eenheid kg gedimensioneerd met soort;
            de buit Numeriek (niet-negatief getal met 2 decimalen) met eenheid kg gedimensioneerd met seizoen en soort;
            de stand Numeriek (niet-negatief getal met 2 decimalen) gedimensioneerd met seizoen en strand en getij;
            de winst Numeriek (niet-negatief getal met 2 decimalen) gedimensioneerd met boekjaar en strand;
            de aanvoer Numeriek (niet-negatief getal met 2 decimalen) met eenheid kg gedimensioneerd met strand en herkomst;
            het jaargewicht Numeriek (niet-negatief getal met 2 decimalen) met eenheid kg;

        Regel jaargewicht van een jutter
            geldig altijd
                Het jaargewicht van een Jutter moet berekend worden als de som van zijn vondstgewicht over alle seizoenen over alle stranden.

        """;

    private static ScenarioBundle resolve(String scenarioText) {
        return ScenarioResolver.resolve(MODEL + scenarioText);
    }

    private static ResolvedScenario only(ScenarioBundle b) {
        assertTrue(b.isOk(), () -> b.model().errors().toString());
        assertEquals(1, b.scenarios().size());
        return b.scenarios().get(0);
    }

    /** A scenario with one Jutter J whose bullets are {@code bullets} and whose expectations are {@code expected}. */
    private static String scenario(String bullets, String expected) {
        return "Scenario s\nGegeven is de volgende situatie:\neen Jutter J\n" + bullets
                + "Met als verwacht resultaat:\n" + expected;
    }

    private static final String CONSISTENT = "- de gegevens zijn consistent\n";

    // ---------------- the case file ----------------

    private static final String CELLS = scenario("""
            - het vondstgewicht in voorjaar op noord is 2,00 kg
            - het vondstgewicht op zuid en in late zomer is 0,5 kg
            - de natte vangst is 1,25 kg
            """, """
            - het jaargewicht van J is 2,50 kg
            - het vondstgewicht in late zomer op zuid van J is 0,50 kg
            - het vondstgewicht in najaar, op noord van J is leeg
            - de droge vangst van J is leeg
            - de gegevens zijn consistent
            """);

    /** Written by hand from the case schema ({@code cellsValue}, the kinds {@code cellValue} and {@code cellEmpty}). */
    private static final String CELLS_CASE_FILE = """
        {
          "format": "uchar-case-v0",
          "asOfDate": null,
          "input": {
            "instances": [
              {
                "label": "J",
                "objectType": "Jutter",
                "attributes": {
                  "vondstgewicht": {
                    "cells": [
                      {
                        "labels": [
                          "voorjaar",
                          "noord"
                        ],
                        "value": {
                          "number": "2.00"
                        }
                      },
                      {
                        "labels": [
                          "late zomer",
                          "zuid"
                        ],
                        "value": {
                          "number": "0.5"
                        }
                      }
                    ]
                  },
                  "vangst": {
                    "cells": [
                      {
                        "labels": [
                          "natte"
                        ],
                        "value": {
                          "number": "1.25"
                        }
                      }
                    ]
                  }
                },
                "roles": {},
                "characteristics": []
              }
            ],
            "parameters": {}
          },
          "expected": [
            {
              "kind": "attributeValue",
              "label": "J",
              "attribute": "jaargewicht",
              "value": {
                "number": "2.50"
              }
            },
            {
              "kind": "cellValue",
              "label": "J",
              "attribute": "vondstgewicht",
              "labels": [
                "late zomer",
                "zuid"
              ],
              "value": {
                "number": "0.50"
              }
            },
            {
              "kind": "cellEmpty",
              "label": "J",
              "attribute": "vondstgewicht",
              "labels": [
                "najaar",
                "noord"
              ]
            },
            {
              "kind": "cellEmpty",
              "label": "J",
              "attribute": "vangst",
              "labels": [
                "droge"
              ]
            },
            {
              "kind": "consistent"
            }
          ]
        }
        """;

    @Test void a_cell_is_an_input_and_an_expectation_of_the_case_file() {
        assertEquals(CELLS_CASE_FILE, CaseEmitter.emit(only(resolve(CELLS))));
    }

    // ---------------- the forms of a cell ----------------

    /** The lowered case file of the one scenario of {@code text}, without its whitespace. */
    private static String compactCaseFile(String text) {
        return CaseEmitter.emit(only(resolve(text))).replaceAll("\\s+", "");
    }

    /** {@code labels} as the case file writes a list of labels, without whitespace. */
    private static String labelList(String labels) {
        return "[\"" + String.join("\",\"", labels.replace(" ", "").split(",")) + "\"]";
    }

    /** The input {@code attribute} of a case file without whitespace, with one cell per {@code labels=number}. */
    private static String inputCells(String attribute, String... cells) {
        List<String> written = new ArrayList<>();
        for (String c : cells) {
            String[] parts = c.split("=");
            written.add("{\"labels\":" + labelList(parts[0]) + ",\"value\":{\"number\":\"" + parts[1] + "\"}}");
        }
        return "\"" + attribute + "\":{\"cells\":[" + String.join(",", written) + "]}";
    }

    @Test void the_labels_are_read_in_every_form_of_a_cell() {
        String[][] forms = {
            // side by side, in the declared order and the other way round, a label of two words
            {"het vondstgewicht in voorjaar op zuid", "3 kg", "vondstgewicht", "voorjaar, zuid"},
            {"het vondstgewicht op zuid in late zomer", "3 kg", "vondstgewicht", "late zomer, zuid"},
            // joined as ALEF joins them: "en", ",", and an enumeration of three
            {"het vondstgewicht in voorjaar en op zuid", "3 kg", "vondstgewicht", "voorjaar, zuid"},
            {"het vondstgewicht op noord, in najaar", "3 kg", "vondstgewicht", "najaar, noord"},
            {"de stand op noord, in late zomer en bij vloed", "3", "stand", "late zomer, noord, vloed"},
            // after "van", which splits the name in the parse, and after "uit", a keyword of its own
            {"de winst van vorig boekjaar en op noord", "3", "winst", "vorig boekjaar, noord"},
            {"de winst op zuid van dit boekjaar", "3", "winst", "dit boekjaar, zuid"},
            {"de aanvoer uit zee op noord", "3 kg", "aanvoer", "noord, zee"},
            {"de aanvoer op zuid en uit rivier", "3 kg", "aanvoer", "zuid, rivier"},
            // a label of a dimension without a preposition before the attribute, alone and beside one behind it
            {"de natte vangst", "3 kg", "vangst", "natte"},
            {"de droge buit in najaar", "3 kg", "buit", "najaar, droge"},
        };
        for (String[] f : forms) {
            String input = compactCaseFile(scenario("- " + f[0] + " is " + f[1] + "\n", CONSISTENT));
            String cell = inputCells(f[2], f[3] + "=3");
            assertTrue(input.contains(cell), () -> "de invoer '" + f[0] + "' is " + cell + ":\n" + input);
            String value = compactCaseFile(scenario("", "- " + f[0] + " van J is " + f[1] + "\n"));
            String expectedValue = "{\"kind\":\"cellValue\",\"label\":\"J\",\"attribute\":\"" + f[2] + "\",\"labels\":"
                    + labelList(f[3]) + ",\"value\":{\"number\":\"3\"}}";
            assertTrue(value.contains(expectedValue), () -> "de verwachting '" + f[0] + "' is " + expectedValue + ":\n" + value);
            String empty = compactCaseFile(scenario("", "- " + f[0] + " van J is leeg\n"));
            String expectedEmpty = "{\"kind\":\"cellEmpty\",\"label\":\"J\",\"attribute\":\"" + f[2] + "\",\"labels\":"
                    + labelList(f[3]) + "}";
            assertTrue(empty.contains(expectedEmpty), () -> "de verwachting '" + f[0] + "' is " + expectedEmpty + ":\n" + empty);
        }
    }

    /** One label is one instance and one cell is one cell of it: a later block gives a cell again in place, and the
     *  other cells of the attribute stay, in first-seen order. Within one block the same holds. */
    @Test void the_cells_of_an_attribute_merge_cell_by_cell() {
        String text = """
            Situatie basis:
            een Jutter J
            - het vondstgewicht in voorjaar op noord is 1 kg
            - het vondstgewicht in najaar op zuid is 2 kg
            Scenario s
            gebaseerd op de situatie basis
            Gegeven is de volgende situatie:
            een Jutter J
            - het vondstgewicht op zuid in late zomer is 4 kg
            - het vondstgewicht op noord en in voorjaar is 3 kg
            - het vondstgewicht in late zomer op zuid is 5 kg
            Met als verwacht resultaat:
            - de gegevens zijn consistent
            """;
        String c = compactCaseFile(text);
        String cells = inputCells("vondstgewicht", "voorjaar, noord=3", "najaar, zuid=2", "late zomer, zuid=5");
        assertTrue(c.contains(cells), () -> cells + ":\n" + c);
    }

    /** A situation two scenarios are based on gives both the same cells: what the first adds stays its own. */
    @Test void a_situation_gives_its_cells_to_every_scenario_alike() {
        String text = """
            Situatie basis:
            een Jutter J
            - het vondstgewicht in voorjaar op noord is 1 kg
            Scenario een
            gebaseerd op de situatie basis
            Gegeven is de volgende situatie:
            een Jutter J
            - het vondstgewicht in najaar op zuid is 2 kg
            Met als verwacht resultaat:
            - de gegevens zijn consistent
            Scenario twee
            gebaseerd op de situatie basis
            Met als verwacht resultaat:
            - de gegevens zijn consistent
            """;
        ScenarioBundle b = resolve(text);
        assertTrue(b.isOk(), () -> b.model().errors().toString());
        String one = CaseEmitter.emit(b.scenarios().get(0)).replaceAll("\\s+", "");
        String two = CaseEmitter.emit(b.scenarios().get(1)).replaceAll("\\s+", "");
        assertTrue(one.contains(inputCells("vondstgewicht", "voorjaar, noord=1", "najaar, zuid=2")), one);
        assertTrue(two.contains(inputCells("vondstgewicht", "voorjaar, noord=1")), two);
    }

    /** Where the name reads as a cell of two attributes, the longer attribute is the one it names, as in a rule: the
     *  attribute "natte buit" in late zomer, not the cell (late zomer, natte) of "buit". */
    @Test void the_longest_attribute_wins() {
        String text = """
            Dimensie het seizoen, bestaande uit de seizoenen (na het attribuut met voorzetsel in):
                1. voorjaar
                2. late zomer

            Dimensie de soort, bestaande uit de soorten (voor het attribuut zonder voorzetsel):
                1. natte
                2. droge

            Objecttype de Jutter (mv: Jutters) (bezield)
                de buit Numeriek (geheel getal) gedimensioneerd met seizoen en soort;
                de natte buit Numeriek (geheel getal) gedimensioneerd met seizoen;

            Scenario s
            Gegeven is de volgende situatie:
            een Jutter J
            - de natte buit in late zomer is 3
            Met als verwacht resultaat:
            - de droge buit in voorjaar van J is leeg
            """;
        ScenarioBundle b = ScenarioResolver.resolve(text);
        assertTrue(b.isOk(), () -> b.model().errors().toString());
        String c = CaseEmitter.emit(b.scenarios().get(0)).replaceAll("\\s+", "");
        assertTrue(c.contains("\"attributes\":{" + inputCells("nattebuit", "late zomer=3") + "}"), c);
        assertTrue(c.contains("\"attribute\":\"buit\",\"labels\":[\"voorjaar\",\"droge\"]"), c);
    }

    // ---------------- what has no case form ----------------

    /** The ERRORs of {@code b} on the line of {@code text} that holds {@code merker}, each as its code and its
     *  parameters in the order of their names. */
    private static List<String> errorsOn(ScenarioBundle b, String source, String merker) {
        int line = ScenarioFixture.lineOf(source, merker);
        List<String> result = new ArrayList<>();
        for (Diagnostic d : b.model().errors()) if (d.line() == line) result.add(d.code() + " " + new TreeMap<>(d.args()));
        assertFalse(b.isOk());
        return result;
    }

    private static List<String> errorsOn(String text, String merker) {
        return errorsOn(resolve(text), MODEL + text, merker);
    }

    /** RGS-364: {@code written} names no label of {@code dimension} of {@code attribute}. */
    private static String missing(String written, String attribute, String dimension) {
        return "RGS-364 " + new TreeMap<>(Map.of("written", written, "attribute", attribute, "dimension", dimension));
    }

    @Test void a_dimensioned_attribute_without_its_labels_is_refused_per_dimension() {
        assertEquals(List.of(missing("vondstgewicht", "vondstgewicht", "seizoen"), missing("vondstgewicht", "vondstgewicht", "strand")),
                errorsOn(scenario("- het vondstgewicht is 3 kg\n", CONSISTENT), "- het vondstgewicht is 3 kg"));
        assertEquals(List.of(missing("vondstgewicht", "vondstgewicht", "seizoen"), missing("vondstgewicht", "vondstgewicht", "strand")),
                errorsOn(scenario("", "- het vondstgewicht van J is 3 kg\n"), "vondstgewicht van J"));
        assertEquals(List.of(missing("vangst", "vangst", "soort")),
                errorsOn(scenario("", "- de vangst van J is leeg\n"), "vangst van J"));
    }

    @Test void a_cell_without_a_label_of_every_dimension_is_refused() {
        assertEquals(List.of(missing("vondstgewicht in voorjaar", "vondstgewicht", "strand")),
                errorsOn(scenario("- het vondstgewicht in voorjaar is 3 kg\n", CONSISTENT), "in voorjaar is 3 kg"));
        assertEquals(List.of(missing("droge buit", "buit", "seizoen")),
                errorsOn(scenario("", "- de droge buit van J is leeg\n"), "buit van J"));
    }

    /** Two dimensions with one preposition and the same labels: "in noord in zuid" fits two cells. */
    @Test void a_cell_that_fits_two_ways_is_refused() {
        String text = """
            Dimensie de kust, bestaande uit de kusten (na het attribuut met voorzetsel in):
                1. noord
                2. zuid

            Dimensie de wind, bestaande uit de winden (na het attribuut met voorzetsel in):
                1. noord
                2. zuid

            Objecttype de Strandwacht (mv: Strandwachten) (bezield)
                de stand Numeriek (geheel getal) gedimensioneerd met kust en wind;

            Scenario s
            Gegeven is de volgende situatie:
            een Strandwacht W
            - de stand in noord in zuid is 3
            Met als verwacht resultaat:
            - de stand in zuid in noord van W is leeg
            """;
        ScenarioBundle b = ScenarioResolver.resolve(text);
        assertEquals(List.of("RGS-365 {cells=stand[noord, zuid]; stand[zuid, noord], written=stand in noord in zuid}"),
                errorsOn(b, text, "in noord in zuid is"));
        assertEquals(List.of("RGS-365 {cells=stand[zuid, noord]; stand[noord, zuid], written=stand in zuid in noord}"),
                errorsOn(b, text, "in zuid in noord van"));
    }

    /** A case file gives a cell one value and expects one (cells, cellValue, cellEmpty): a period or a timeline of a
     *  cell has no case form. */
    @Test void a_cell_has_no_period() {
        String cell = "RGS-380 {attribute=vondstgewicht, written=vondstgewicht in voorjaar op noord}";
        assertEquals(List.of(cell), errorsOn(scenario(
                "- het vondstgewicht in voorjaar op noord is 3 kg van 01-01-2024 tot 01-02-2024\n", CONSISTENT), "op noord is 3 kg van"));
        assertEquals(List.of(cell), errorsOn(scenario("",
                "- het vondstgewicht in voorjaar op noord van J is 3 kg van 01-01-2024 tot 01-02-2024\n"), "van J is 3 kg van"));
        assertEquals(List.of(cell), errorsOn(scenario("",
                "- het vondstgewicht in voorjaar op noord van J is altijd leeg\n"), "altijd leeg"));
    }

    @Test void the_value_of_a_cell_is_typed_against_its_attribute() {
        String text = scenario("- het vondstgewicht in voorjaar op noord is 1,125 kg\n", CONSISTENT);
        ScenarioFixture.errorOnLine(resolve(text), MODEL + text, "1,125 kg", "meer decimalen (3) dan het attribuut toelaat (2)");
    }

    /** A label that is no label of the dimension its preposition introduces makes no cell, and the name names nothing. */
    @Test void a_name_with_a_label_of_no_dimension_names_no_attribute() {
        assertEquals(List.of("RGS-236 {attribute=vondstgewicht in winter op noord, objectType=Jutter}"),
                errorsOn(scenario("- het vondstgewicht in winter op noord is 3 kg\n", CONSISTENT), "in winter op noord"));
    }

    // ---------------- the semantic tokens ----------------

    /** The span of {@code file} that starts where {@code anchor} starts in {@code source}. */
    private static Span spanAt(SemanticTokens.Document d, String file, String source, String anchor) {
        int at = source.indexOf(anchor);
        assertTrue(at >= 0, () -> "'" + anchor + "' does not occur");
        int line = 1, lineStart = 0;
        for (int i = 0; i < at; i++) if (source.charAt(i) == '\n') { line++; lineStart = i + 1; }
        for (SemanticTokens.FileSpans fs : d.files())
            if (fs.file().equals(file))
                for (Span s : fs.spans()) if (s.line() == line && s.column() == at - lineStart) return s;
        return fail("no span at '" + anchor + "' in " + file);
    }

    /** The span at {@code anchor} in the test file is {@code text}, a reference of {@code kind} to the declaration that
     *  begins where {@code declared} begins in the model. */
    private static void reference(SemanticTokens.Document d, String test, String anchor, String text, Kind kind, String declared) {
        Span s = spanAt(d, "toets.rgs", test, anchor);
        assertEquals(text, s.text(), () -> "at '" + anchor + "'");
        assertEquals(kind, s.kind(), () -> "'" + text + "' at '" + anchor + "'");
        assertNotNull(s.declaredAt(), () -> "'" + text + "' at '" + anchor + "' points at its declaration");
        Span declaration = spanAt(d, "model.rgs", MODEL, declared);
        assertEquals(new SemanticTokens.Location("model.rgs", declaration.line(), declaration.column()), s.declaredAt(),
                () -> "'" + text + "' at '" + anchor + "' points at '" + declared + "'");
    }

    /** The semantic tokens read the name of a cell in a bullet as the scenario route reads it: the attribute and each
     *  label point at their declarations, the prepositions and the joining words keep their lexical kind. */
    @Test void the_semantic_tokens_read_a_cell_as_the_scenario_route_reads_it() {
        String test = scenario("- het vondstgewicht op zuid en in late zomer is 0,5 kg\n- de natte vangst is 1,25 kg\n",
                "- het vondstgewicht in najaar, op noord van J is leeg\n- de aanvoer uit zee op noord van J is leeg\n");
        List<SemanticTokens.SourceFile> files = List.of(new SemanticTokens.SourceFile("model.rgs", MODEL),
                new SemanticTokens.SourceFile("toets.rgs", test));
        SemanticTokens.Document d = Resolver.tokens(files);
        assertTrue(d.diagnostics().isEmpty(), () -> "no diagnostic: " + d.diagnostics());
        SemanticTokensCheck.check(files, d);
        for (SemanticTokens.FileSpans fs : d.files())
            for (Span s : fs.spans()) {
                assertNotEquals(Kind.UNRESOLVED, s.kind(), () -> "unresolved: " + s);
                assertNotEquals(Kind.WORD, s.kind(), () -> "a word outside every name: " + s);
                assertFalse(SemanticTokensCheck.unlinked(s), () -> "a reference without its declaration: " + s);
            }
        reference(d, test, "vondstgewicht op zuid", "vondstgewicht", Kind.ATTRIBUTE, "vondstgewicht Numeriek");
        reference(d, test, "zuid en in late", "zuid", Kind.DIMENSION_LABEL, "zuid\n");
        reference(d, test, "late zomer is", "late zomer", Kind.DIMENSION_LABEL, "late zomer\n");
        reference(d, test, "natte vangst", "natte", Kind.DIMENSION_LABEL, "natte\n");
        reference(d, test, "vangst is", "vangst", Kind.ATTRIBUTE, "vangst Numeriek");
        reference(d, test, "najaar, op", "najaar", Kind.DIMENSION_LABEL, "najaar\n");
        reference(d, test, "noord van J is leeg\n- de", "noord", Kind.DIMENSION_LABEL, "noord\n");
        reference(d, test, "aanvoer uit", "aanvoer", Kind.ATTRIBUTE, "aanvoer Numeriek");
        reference(d, test, "zee op", "zee", Kind.DIMENSION_LABEL, "zee\n");
        assertEquals(Kind.KEYWORD, spanAt(d, "toets.rgs", test, "en in late").kind());
        assertEquals(Kind.KEYWORD, spanAt(d, "toets.rgs", test, "uit zee").kind());
    }
}
