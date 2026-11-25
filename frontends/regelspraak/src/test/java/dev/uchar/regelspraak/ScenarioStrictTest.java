package dev.uchar.regelspraak;

import dev.uchar.regelspraak.emit.CaseEmitter;
import dev.uchar.regelspraak.emit.IrEmitter;
import dev.uchar.regelspraak.ir.ResolvedScenario;
import dev.uchar.regelspraak.model.RegelSpraakModel.Diagnostic;
import dev.uchar.regelspraak.model.RegelSpraakModel.Severity;
import dev.uchar.regelspraak.resolver.Resolver;
import dev.uchar.regelspraak.resolver.ScenarioResolver;
import dev.uchar.regelspraak.resolver.ScenarioResolver.ScenarioBundle;
import dev.uchar.regelspraak.resolver.SemanticTokens;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.*;

/**
 * A scenario for a document with {@code Afleiden uit leeg: nee} (grammar/DIVERGENCES.md, "Scenariotoetsen"): a
 * situation gives a characteristic that does not hold ("- is niet drijfhout", "- heeft geen strandrecht", "- heeft
 * niet het strandrecht"), which the case file carries in {@code characteristicsAbsent}, and an expectation expects an
 * open member ("- de afhaaldatum van V1 staat niet vast", "- of V1 afhaalbaar is, staat niet vast"), which the case
 * file carries as {@code attributeOpen} and {@code characteristicOpen} (RUNTIME-SEMANTICS ch. 1.5). The case files
 * below are written by hand from the case schema. Measured on the frontend before: a characteristic after "niet" or
 * "geen" in a situation was an unknown characteristic (RGS-241), and every "staat niet vast" a syntax error.
 */
class ScenarioStrictTest {

    private static final String NEE = "Afleiden uit leeg: nee\n\n";

    private static final String MODEL = """
            Dimensie het seizoen, bestaande uit de seizoenen (na het attribuut met voorzetsel in):
                1. voorjaar
                2. najaar

            Objecttype de Vondst (mv: Vondsten)
                de afhaaldatum Datum in dagen;
                het gewicht Numeriek (geheel getal);
                de opslagkosten Numeriek (geheel getal) voor elke maand;
                het seizoensgewicht Numeriek (geheel getal) gedimensioneerd met seizoen;
                is drijfhout kenmerk (bijvoeglijk);
                is afhaalbaar kenmerk (bijvoeglijk);
                is aangespoeld kenmerk (bijvoeglijk) voor elke maand;
                het strandrecht kenmerk (bezittelijk);

            """;

    private static final String THREE_FORMS = """
            Scenario drie vormen
            Gegeven is de volgende situatie:
            een Vondst V1
            - is niet drijfhout
            - heeft geen strandrecht
            een Vondst V2
            - heeft niet het strandrecht
            - is afhaalbaar
            een Vondst V3
            - is drijfhout
            Met als verwacht resultaat:
            - de gegevens zijn consistent
            """;

    /** Written by hand from the case schema: {@code characteristicsAbsent} after {@code characteristics}, left out
     *  where it would be empty. */
    private static final String THREE_FORMS_CASE = """
            {
              "format": "uchar-case-v0",
              "asOfDate": null,
              "input": {
                "instances": [
                  {
                    "label": "V1",
                    "objectType": "Vondst",
                    "attributes": {},
                    "roles": {},
                    "characteristics": [],
                    "characteristicsAbsent": [
                      "drijfhout",
                      "strandrecht"
                    ]
                  },
                  {
                    "label": "V2",
                    "objectType": "Vondst",
                    "attributes": {},
                    "roles": {},
                    "characteristics": [
                      "afhaalbaar"
                    ],
                    "characteristicsAbsent": [
                      "strandrecht"
                    ]
                  },
                  {
                    "label": "V3",
                    "objectType": "Vondst",
                    "attributes": {},
                    "roles": {},
                    "characteristics": [
                      "drijfhout"
                    ]
                  }
                ],
                "parameters": {}
              },
              "expected": [
                {
                  "kind": "consistent"
                }
              ]
            }
            """;

    private static List<ResolvedScenario> scenarios(String text) {
        ScenarioBundle b = ScenarioResolver.resolve(text);
        assertTrue(b.isOk(), () -> b.model().errors().toString());
        return b.scenarios();
    }

    private static ResolvedScenario only(String text) {
        List<ResolvedScenario> s = scenarios(text);
        assertEquals(1, s.size());
        return s.get(0);
    }

    /** The ERRORs of {@code text} on the line that holds {@code marker}, each as its code and its parameters. */
    private static List<String> errorsOn(String text, String marker) {
        ScenarioBundle b = ScenarioResolver.resolve(text);
        assertFalse(b.isOk(), "the document is refused");
        int line = ScenarioFixture.lineOf(text, marker);
        List<String> result = new ArrayList<>();
        for (Diagnostic d : b.model().errors()) if (d.line() == line) result.add(d.code() + " " + new TreeMap<>(d.args()));
        return result;
    }

    // ---------------- the situation ----------------

    @Test void the_three_forms_of_a_characteristic_that_does_not_hold_write_characteristicsAbsent() {
        assertEquals(THREE_FORMS_CASE, CaseEmitter.emit(only(NEE + MODEL + THREE_FORMS)));
    }

    /** characteristicsAbsent changes no outcome in a document that infers from an empty value; the frontend writes
     *  it there all the same. */
    @Test void a_document_that_infers_from_empty_writes_the_same_case_file() {
        assertEquals(THREE_FORMS_CASE, CaseEmitter.emit(only(MODEL + THREE_FORMS)));
    }

    /** The WARNINGs of the scenario layer, each as its line, its code and its parameters. */
    private static List<String> warnings(ScenarioBundle b) {
        List<String> result = new ArrayList<>();
        for (Diagnostic d : b.warnings()) {
            assertEquals(Severity.WARNING, d.severity());
            result.add(d.line() + " " + d.code() + " " + new TreeMap<>(d.args()));
        }
        return result;
    }

    /** There each of the three forms is a WARNING (RGS-469): a characteristic the situation does not name does not
     *  hold already. The warnings stand beside the model, so the IR document stays the production route's, and the
     *  semantic tokens carry them as the route prints them. */
    @Test void a_document_that_infers_from_empty_warns_that_the_sentence_changes_nothing() {
        String text = MODEL + THREE_FORMS;
        ScenarioBundle b = ScenarioResolver.resolve(text);
        assertTrue(b.isOk(), () -> b.model().errors().toString());
        List<String> expected = List.of(
                ScenarioFixture.lineOf(text, "- is niet drijfhout") + " RGS-469 {written=is niet drijfhout}",
                ScenarioFixture.lineOf(text, "- heeft geen strandrecht") + " RGS-469 {written=heeft geen strandrecht}",
                ScenarioFixture.lineOf(text, "- heeft niet het strandrecht") + " RGS-469 {written=heeft niet het strandrecht}");
        assertEquals(expected, warnings(b));
        assertTrue(b.model().diagnostics.stream().noneMatch(d -> "RGS-469".equals(d.code())), () -> b.model().diagnostics.toString());
        assertEquals(IrEmitter.emit(Resolver.resolve(MODEL)), IrEmitter.emit(b.model()));
        assertEquals("scenario: 'is niet drijfhout' verandert in een document zonder 'Afleiden uit leeg: nee' niets: daar "
                        + "geldt een kenmerk dat de situatie niet noemt al niet; de zin telt alleen in een document met "
                        + "'Afleiden uit leeg: nee'",
                b.warnings().get(0).message());
        List<String> tokens = new ArrayList<>();
        for (SemanticTokens.FileDiagnostic d : Resolver.tokens(List.of(new SemanticTokens.SourceFile("t.rgs", text))).diagnostics())
            if (d.severity() == Severity.WARNING) tokens.add(d.line() + " " + d.code() + " " + new TreeMap<>(d.params()));
        assertEquals(expected, tokens);
    }

    @Test void a_strict_document_gives_no_warning() {
        ScenarioBundle b = ScenarioResolver.resolve(NEE + MODEL + THREE_FORMS);
        assertTrue(b.isOk(), () -> b.model().errors().toString());
        assertEquals(List.of(), warnings(b));
    }

    /** Where the bullet replaces a characteristic a block merged before it gives as holding, it counts in every
     *  document and gives no warning: "Scenario niet meer" on the situation drijfhout. The bullet of the situation
     *  geen drijfhout replaces nothing (a later block replaces it), and warns. */
    @Test void a_bullet_that_replaces_an_inherited_characteristic_gives_no_warning() {
        String text = MODEL + BASES;
        ScenarioBundle b = ScenarioResolver.resolve(text);
        assertTrue(b.isOk(), () -> b.model().errors().toString());
        assertEquals(List.of(ScenarioFixture.lineOf(text, "- is niet drijfhout") + " RGS-469 {written=is niet drijfhout}"),
                warnings(b));
    }

    /** A situation based on another replaces there the characteristic it inherits as holding, also where no scenario
     *  uses it: the bullet counts in every scenario that will, and gives no warning. */
    @Test void a_bullet_that_replaces_an_inherited_characteristic_in_an_unused_situation_gives_no_warning() {
        String text = MODEL + """
                Situatie drijfhout:
                een Vondst V1
                - is drijfhout

                Situatie geen drijfhout gebaseerd op de situatie drijfhout:
                een Vondst V1
                - is niet drijfhout

                Scenario alleen de basis
                gebaseerd op de situatie drijfhout
                Met als verwacht resultaat:
                - de gegevens zijn consistent
                """;
        ScenarioBundle b = ScenarioResolver.resolve(text);
        assertTrue(b.isOk(), () -> b.model().errors().toString());
        assertEquals(List.of(), warnings(b));
    }

    /** Two bullets on one line are two sentences: the one that replaces an inherited characteristic gives no warning,
     *  the other does, and where neither replaces one each warns on that line. */
    @Test void two_bullets_on_one_line_are_judged_each_on_its_own() {
        String text = MODEL + """
                Situatie drijfhout:
                een Vondst V1
                - is drijfhout

                Scenario op een regel
                gebaseerd op de situatie drijfhout
                Gegeven is de volgende situatie:
                een Vondst V1
                - is niet drijfhout - heeft geen strandrecht
                een Vondst V2
                - heeft geen strandrecht - is niet drijfhout
                Met als verwacht resultaat:
                - de gegevens zijn consistent
                """;
        ScenarioBundle b = ScenarioResolver.resolve(text);
        assertTrue(b.isOk(), () -> b.model().errors().toString());
        int v1 = ScenarioFixture.lineOf(text, "- is niet drijfhout - heeft geen strandrecht");
        int v2 = ScenarioFixture.lineOf(text, "- heeft geen strandrecht - is niet drijfhout");
        assertEquals(List.of(v1 + " RGS-469 {written=heeft geen strandrecht}",
                        v2 + " RGS-469 {written=heeft geen strandrecht}",
                        v2 + " RGS-469 {written=is niet drijfhout}"),
                warnings(b));
    }

    @Test void a_characteristic_that_holds_and_does_not_in_one_block_is_refused() {
        String sameOpener = NEE + MODEL + """
                Scenario tegenspraak
                Gegeven is de volgende situatie:
                een Vondst V1
                - is drijfhout
                - is niet drijfhout
                Met als verwacht resultaat:
                - de gegevens zijn consistent
                """;
        assertEquals(List.of("RGS-465 {characteristic=drijfhout, label=V1}"), errorsOn(sameOpener, "- is niet drijfhout"));
        // a label opened twice in one block is one instance of that block
        String twoOpeners = NEE + MODEL + """
                Situatie tweemaal V1:
                een Vondst V1
                - heeft geen strandrecht
                een Vondst V1
                - heeft strandrecht

                Scenario tegenspraak
                gebaseerd op de situatie tweemaal V1
                Met als verwacht resultaat:
                - de gegevens zijn consistent
                """;
        assertEquals(List.of("RGS-465 {characteristic=strandrecht, label=V1}"), errorsOn(twoOpeners, "- heeft strandrecht"));
    }

    private static final String BASES = """
            Situatie drijfhout:
            een Vondst V1
            - is drijfhout
            - is afhaalbaar

            Situatie geen drijfhout:
            een Vondst V1
            - is niet drijfhout
            - is afhaalbaar

            Scenario niet meer
            gebaseerd op de situatie drijfhout
            Gegeven is de volgende situatie:
            een Vondst V1
            - is niet drijfhout
            Met als verwacht resultaat:
            - de gegevens zijn consistent

            Scenario weer wel
            gebaseerd op de situatie geen drijfhout
            Gegeven is de volgende situatie:
            een Vondst V1
            - is drijfhout
            Met als verwacht resultaat:
            - de gegevens zijn consistent
            """;

    /** A case file of one instance V1 and the expectation 'consistent', with {@code characteristics} as given. */
    private static String basesCase(String characteristics) {
        return """
                {
                  "format": "uchar-case-v0",
                  "asOfDate": null,
                  "input": {
                    "instances": [
                      {
                        "label": "V1",
                        "objectType": "Vondst",
                        "attributes": {},
                        "roles": {},
                """ + characteristics + """
                      }
                    ],
                    "parameters": {}
                  },
                  "expected": [
                    {
                      "kind": "consistent"
                    }
                  ]
                }
                """;
    }

    /** What a later block says of a characteristic replaces what it inherits, in both directions, so a case never
     *  names one characteristic in both lists (EF-047). */
    @Test void a_block_based_on_another_replaces_what_it_inherits_in_both_directions() {
        List<ResolvedScenario> s = scenarios(NEE + MODEL + BASES);
        assertEquals(2, s.size());
        assertEquals(basesCase("""
                        "characteristics": [
                          "afhaalbaar"
                        ],
                        "characteristicsAbsent": [
                          "drijfhout"
                        ]
                """), CaseEmitter.emit(s.get(0)));
        assertEquals(basesCase("""
                        "characteristics": [
                          "drijfhout",
                          "afhaalbaar"
                        ]
                """), CaseEmitter.emit(s.get(1)));
    }

    private static final String SITUATION_BASES = """
            Situatie drijfhout:
            een Vondst V1
            - is drijfhout
            - is afhaalbaar

            Situatie geen drijfhout:
            een Vondst V1
            - is niet drijfhout
            - is afhaalbaar

            Situatie drijfhout weg gebaseerd op de situatie drijfhout:
            een Vondst V1
            - is niet drijfhout

            Situatie drijfhout terug gebaseerd op de situatie geen drijfhout:
            een Vondst V1
            - is drijfhout

            Scenario situatie weg
            gebaseerd op de situatie drijfhout weg
            Met als verwacht resultaat:
            - de gegevens zijn consistent

            Scenario situatie terug
            gebaseerd op de situatie drijfhout terug
            Met als verwacht resultaat:
            - de gegevens zijn consistent

            Scenario twee bases weg
            gebaseerd op de situatie drijfhout
            gebaseerd op de situatie geen drijfhout
            Met als verwacht resultaat:
            - de gegevens zijn consistent

            Scenario twee bases terug
            gebaseerd op de situatie geen drijfhout
            gebaseerd op de situatie drijfhout
            Met als verwacht resultaat:
            - de gegevens zijn consistent
            """;

    /** As above for a situation based on another, and for a scenario with two bases, the second of which is the later
     *  block: the characteristic keeps its place among those of the instance and changes its list. */
    @Test void a_situation_based_on_another_and_a_second_basis_replace_what_they_inherit_in_both_directions() {
        List<ResolvedScenario> s = scenarios(NEE + MODEL + SITUATION_BASES);
        assertEquals(4, s.size());
        String gone = basesCase("""
                        "characteristics": [
                          "afhaalbaar"
                        ],
                        "characteristicsAbsent": [
                          "drijfhout"
                        ]
                """);
        String back = basesCase("""
                        "characteristics": [
                          "drijfhout",
                          "afhaalbaar"
                        ]
                """);
        assertEquals(gone, CaseEmitter.emit(s.get(0)));
        assertEquals(back, CaseEmitter.emit(s.get(1)));
        assertEquals(gone, CaseEmitter.emit(s.get(2)));
        assertEquals(back, CaseEmitter.emit(s.get(3)));
    }

    /** One instance with a characteristic that holds, one that does not and one with a timeline: the case schema's key
     *  order, characteristicsAbsent between characteristics and characteristicTimelines. */
    @Test void characteristicsAbsent_stands_between_characteristics_and_characteristicTimelines() {
        String scenario = NEE + MODEL + """
                Scenario drie lijsten
                Gegeven is de volgende situatie:
                een Vondst V1
                - is aangespoeld van 01-01-2025 tot 01-02-2025
                - is niet drijfhout
                - is afhaalbaar
                Met als verwacht resultaat:
                - de gegevens zijn consistent
                """;
        assertEquals(basesCase("""
                        "characteristics": [
                          "afhaalbaar"
                        ],
                        "characteristicsAbsent": [
                          "drijfhout"
                        ],
                        "characteristicTimelines": {
                          "aangespoeld": [
                            {
                              "from": "2025-01-01",
                              "to": "2025-02-01"
                            }
                          ]
                        }
                """), CaseEmitter.emit(only(scenario)));
    }

    /** A characteristic may be declared with a leading "niet" or "geen". The whole written name is read first, as a
     *  rule reads it, and only where it names no characteristic is the word read as the negation: alone, "niet
     *  drijfhout" names its own declaration; beside "drijfhout", "is niet drijfhout" still names it and "is geen
     *  drijfhout" says that "drijfhout" does not hold. Measured on the frontend before: the first was refused (RGS-241),
     *  and in the second "is niet drijfhout" gave drijfhout in characteristicsAbsent. */
    @Test void a_characteristic_declared_with_niet_reads_as_a_whole_first() {
        String alone = """
                Objecttype de Vondst (mv: Vondsten)
                    is niet drijfhout kenmerk (bijvoeglijk);

                Scenario alleen
                Gegeven is de volgende situatie:
                een Vondst V1
                - is niet drijfhout
                Met als verwacht resultaat:
                - V1 is niet drijfhout
                """;
        assertEquals("""
                {
                  "format": "uchar-case-v0",
                  "asOfDate": null,
                  "input": {
                    "instances": [
                      {
                        "label": "V1",
                        "objectType": "Vondst",
                        "attributes": {},
                        "roles": {},
                        "characteristics": [
                          "niet drijfhout"
                        ]
                      }
                    ],
                    "parameters": {}
                  },
                  "expected": [
                    {
                      "kind": "characteristicPresent",
                      "label": "V1",
                      "characteristic": "niet drijfhout"
                    }
                  ]
                }
                """, CaseEmitter.emit(only(alone)));
        String beside = NEE + """
                Objecttype de Vondst (mv: Vondsten)
                    is drijfhout kenmerk (bijvoeglijk);
                    is niet drijfhout kenmerk (bijvoeglijk);

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
        assertEquals("""
                {
                  "format": "uchar-case-v0",
                  "asOfDate": null,
                  "input": {
                    "instances": [
                      {
                        "label": "V1",
                        "objectType": "Vondst",
                        "attributes": {},
                        "roles": {},
                        "characteristics": [
                          "niet drijfhout"
                        ]
                      },
                      {
                        "label": "V2",
                        "objectType": "Vondst",
                        "attributes": {},
                        "roles": {},
                        "characteristics": [],
                        "characteristicsAbsent": [
                          "drijfhout"
                        ]
                      }
                    ],
                    "parameters": {}
                  },
                  "expected": [
                    {
                      "kind": "characteristicPresent",
                      "label": "V1",
                      "characteristic": "niet drijfhout"
                    },
                    {
                      "kind": "characteristicAbsent",
                      "label": "V2",
                      "characteristic": "drijfhout"
                    }
                  ]
                }
                """, CaseEmitter.emit(only(beside)));
    }

    /** An unknown characteristic after a negation is quoted without its negation and its article, in a situation and
     *  in an expectation alike. */
    @Test void an_unknown_characteristic_after_a_negation_is_quoted_without_its_article() {
        String scenario = NEE + MODEL + """
                Scenario onbekend
                Gegeven is de volgende situatie:
                een Vondst V1
                - heeft niet het onbekendrecht
                een Vondst V2
                Met als verwacht resultaat:
                - V2 heeft niet het onbekendrecht
                """;
        assertEquals(List.of("RGS-241 {characteristic=onbekendrecht, objectType=Vondst}"),
                errorsOn(scenario, "- heeft niet het onbekendrecht"));
        assertEquals(List.of("RGS-241 {characteristic=onbekendrecht, objectType=Vondst}"),
                errorsOn(scenario, "- V2 heeft niet het onbekendrecht"));
    }

    @Test void a_characteristic_that_does_not_hold_takes_no_period_and_no_timeline() {
        String scenario = NEE + MODEL + """
                Scenario perioden
                Gegeven is de volgende situatie:
                een Vondst V1
                - is niet drijfhout van 01-01-2025 tot 01-02-2025
                - is niet aangespoeld
                een Vondst V2
                - is niet aangespoeld van 01-01-2025 tot 01-02-2025
                Met als verwacht resultaat:
                - de gegevens zijn consistent
                """;
        assertEquals(List.of("RGS-466 {characteristic=drijfhout}"), errorsOn(scenario, "niet drijfhout van"));
        assertEquals(List.of("RGS-466 {characteristic=aangespoeld}"), errorsOn(scenario, "- is niet aangespoeld")); // the first line that holds it
        assertEquals(List.of("RGS-466 {characteristic=aangespoeld}"), errorsOn(scenario, "niet aangespoeld van"));
    }

    // ---------------- the expectation ----------------

    private static final String OPEN = """
            Scenario open
            Gegeven is de volgende situatie:
            een Vondst V1
            Met als verwacht resultaat:
            - de afhaaldatum van V1 staat niet vast
            - het gewicht van V1 staat niet vast.
            - of V1 drijfhout is, staat niet vast
            - of V1 afhaalbaar is staat niet vast
            - of V1 een drijfhout is, staat niet vast
            - of V1 strandrecht heeft, staat niet vast
            - of V1 het strandrecht heeft staat niet vast
            """;

    /** Written by hand from the case schema: attributeOpen with label and attribute, characteristicOpen with label and
     *  characteristic. */
    private static final String OPEN_CASE = """
            {
              "format": "uchar-case-v0",
              "asOfDate": null,
              "input": {
                "instances": [
                  {
                    "label": "V1",
                    "objectType": "Vondst",
                    "attributes": {},
                    "roles": {},
                    "characteristics": []
                  }
                ],
                "parameters": {}
              },
              "expected": [
                {
                  "kind": "attributeOpen",
                  "label": "V1",
                  "attribute": "afhaaldatum"
                },
                {
                  "kind": "attributeOpen",
                  "label": "V1",
                  "attribute": "gewicht"
                },
                {
                  "kind": "characteristicOpen",
                  "label": "V1",
                  "characteristic": "drijfhout"
                },
                {
                  "kind": "characteristicOpen",
                  "label": "V1",
                  "characteristic": "afhaalbaar"
                },
                {
                  "kind": "characteristicOpen",
                  "label": "V1",
                  "characteristic": "drijfhout"
                },
                {
                  "kind": "characteristicOpen",
                  "label": "V1",
                  "characteristic": "strandrecht"
                },
                {
                  "kind": "characteristicOpen",
                  "label": "V1",
                  "characteristic": "strandrecht"
                }
              ]
            }
            """;

    @Test void staat_niet_vast_expects_an_open_attribute_or_characteristic() {
        assertEquals(OPEN_CASE, CaseEmitter.emit(only(NEE + MODEL + OPEN)));
    }

    /** A document without 'Afleiden uit leeg: nee' has no open member, so there the expectation never holds; the same
     *  with the line written out as 'Afleiden uit leeg: ja'. */
    @Test void staat_niet_vast_is_refused_in_a_document_that_infers_from_empty() {
        for (String scenario : List.of(MODEL + OPEN, "Afleiden uit leeg: ja\n\n" + MODEL + OPEN)) {
            assertEquals(List.of("RGS-467 {label=V1, member=het attribuut 'afhaaldatum'}"),
                    errorsOn(scenario, "afhaaldatum van V1 staat"));
            assertEquals(List.of("RGS-467 {label=V1, member=het kenmerk 'afhaalbaar'}"),
                    errorsOn(scenario, "of V1 afhaalbaar is"));
            assertEquals(List.of("RGS-467 {label=V1, member=het kenmerk 'strandrecht'}"),
                    errorsOn(scenario, "of V1 strandrecht heeft"));
        }
    }

    /** attributeOpen names an attribute without dimensions, and ref refuses one on a dimensioned attribute; so the
     *  expectation on a dimensioned attribute is refused with a cell or without one, in either kind of document, and
     *  before the refusal of the document's kind (RGS-467) or of a cell without its labels (RGS-364). Measured on the
     *  frontend before: without labels it was RGS-364, with labels in a document that infers from an empty value
     *  RGS-467. */
    @Test void staat_niet_vast_on_a_dimensioned_attribute_is_refused_with_or_without_a_cell() {
        String bullets = """
                Scenario open seizoen
                Gegeven is de volgende situatie:
                een Vondst V1
                Met als verwacht resultaat:
                - het seizoensgewicht van V1 staat niet vast
                - het seizoensgewicht in voorjaar van V1 staat niet vast
                """;
        for (String scenario : List.of(NEE + MODEL + bullets, MODEL + bullets)) {
            assertEquals(List.of("RGS-468 {attribute=seizoensgewicht, written=seizoensgewicht}"),
                    errorsOn(scenario, "seizoensgewicht van V1"));
            assertEquals(List.of("RGS-468 {attribute=seizoensgewicht, written=seizoensgewicht in voorjaar}"),
                    errorsOn(scenario, "seizoensgewicht in voorjaar"));
        }
    }

    /** Each of these is refused in either kind of document, so a document that infers from an empty value gets the
     *  same refusal and not RGS-467, whose advice ('Afleiden uit leeg: nee') would not admit the bullet. */
    @Test void staat_niet_vast_on_a_timeline_a_cell_or_with_the_wrong_verb_is_refused() {
        String bullets = """
                Scenario open met tijdlijn
                Gegeven is de volgende situatie:
                een Vondst V1
                Met als verwacht resultaat:
                - de opslagkosten van V1 staat niet vast
                - of V1 aangespoeld is, staat niet vast
                - het seizoensgewicht in voorjaar van V1 staat niet vast
                - of V1 strandrecht is, staat niet vast
                - of V1 niet afhaalbaar is, staat niet vast
                """;
        for (String scenario : List.of(NEE + MODEL + bullets, MODEL + bullets)) {
            assertEquals(List.of("RGS-399 {attribute=opslagkosten, label=V1}"),
                    errorsOn(scenario, "opslagkosten van V1"));
            assertEquals(List.of("RGS-400 {characteristic=aangespoeld, label=V1}"), errorsOn(scenario, "aangespoeld is"));
            assertEquals(List.of("RGS-468 {attribute=seizoensgewicht, written=seizoensgewicht in voorjaar}"),
                    errorsOn(scenario, "seizoensgewicht in voorjaar"));
            assertEquals(List.of("RGS-244 {characteristic=strandrecht}"), errorsOn(scenario, "strandrecht is,"));
            // the open form takes no negation: the name is read as written
            assertEquals(List.of("RGS-241 {characteristic=niet afhaalbaar, objectType=Vondst}"),
                    errorsOn(scenario, "niet afhaalbaar is"));
        }
    }

    /** 'staat' and 'vast' are matched on their text after the label of an expectation only, and 'of' and 'niet' keep
     *  their tokens: an attribute, a characteristic and an attribute named with those words read as before, in a
     *  situation and in an expectation. */
    @Test void names_with_staat_vast_of_and_niet_read_as_before() {
        String text = NEE + """
                Objecttype de Vondst (mv: Vondsten)
                    de staat Tekst;
                    het gewicht of volume Numeriek (geheel getal);
                    de niet uitgekeerde premie Numeriek (geheel getal);
                    is vast kenmerk (bijvoeglijk);

                Scenario vaste staat
                Gegeven is de volgende situatie:
                een Vondst V1
                - de staat is "goed"
                - het gewicht of volume is 3
                - de niet uitgekeerde premie is 4
                - is vast
                een Vondst V2
                - is niet vast
                Met als verwacht resultaat:
                - de staat van V1 is "goed"
                - het gewicht of volume van V1 is 3
                - de niet uitgekeerde premie van V1 is 4
                - V1 is vast
                - V2 is niet vast
                - de staat van V2 staat niet vast
                - of V2 vast is, staat niet vast
                """;
        assertEquals("""
                {
                  "format": "uchar-case-v0",
                  "asOfDate": null,
                  "input": {
                    "instances": [
                      {
                        "label": "V1",
                        "objectType": "Vondst",
                        "attributes": {
                          "staat": {
                            "text": "goed"
                          },
                          "gewicht of volume": {
                            "number": "3"
                          },
                          "niet uitgekeerde premie": {
                            "number": "4"
                          }
                        },
                        "roles": {},
                        "characteristics": [
                          "vast"
                        ]
                      },
                      {
                        "label": "V2",
                        "objectType": "Vondst",
                        "attributes": {},
                        "roles": {},
                        "characteristics": [],
                        "characteristicsAbsent": [
                          "vast"
                        ]
                      }
                    ],
                    "parameters": {}
                  },
                  "expected": [
                    {
                      "kind": "attributeValue",
                      "label": "V1",
                      "attribute": "staat",
                      "value": {
                        "text": "goed"
                      }
                    },
                    {
                      "kind": "attributeValue",
                      "label": "V1",
                      "attribute": "gewicht of volume",
                      "value": {
                        "number": "3"
                      }
                    },
                    {
                      "kind": "attributeValue",
                      "label": "V1",
                      "attribute": "niet uitgekeerde premie",
                      "value": {
                        "number": "4"
                      }
                    },
                    {
                      "kind": "characteristicPresent",
                      "label": "V1",
                      "characteristic": "vast"
                    },
                    {
                      "kind": "characteristicAbsent",
                      "label": "V2",
                      "characteristic": "vast"
                    },
                    {
                      "kind": "attributeOpen",
                      "label": "V2",
                      "attribute": "staat"
                    },
                    {
                      "kind": "characteristicOpen",
                      "label": "V2",
                      "characteristic": "vast"
                    }
                  ]
                }
                """, CaseEmitter.emit(only(text)));
    }

    // ---------------- settled-empty attributes in a situation ----------------

    private static final String EMPTY_ATTRIBUTES = """
            Scenario lege attributen
            Gegeven is de volgende situatie:
            een Vondst V1
            - de afhaaldatum is leeg
            - het gewicht is leeg.
            een Vondst V2
            - het gewicht is 12
            Met als verwacht resultaat:
            - de gegevens zijn consistent
            """;

    private static final String EMPTY_ATTRIBUTES_CASE = """
            {
              "format": "uchar-case-v0",
              "asOfDate": null,
              "input": {
                "instances": [
                  {
                    "label": "V1",
                    "objectType": "Vondst",
                    "attributes": {},
                    "attributesEmpty": [
                      "afhaaldatum",
                      "gewicht"
                    ],
                    "roles": {},
                    "characteristics": []
                  },
                  {
                    "label": "V2",
                    "objectType": "Vondst",
                    "attributes": {
                      "gewicht": {
                        "number": "12"
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
                  "kind": "consistent"
                }
              ]
            }
            """;

    @Test void attribute_is_leeg_writes_attributesEmpty_in_strict_and_ordinary_documents() {
        assertEquals(EMPTY_ATTRIBUTES_CASE, CaseEmitter.emit(only(NEE + MODEL + EMPTY_ATTRIBUTES)));
        assertEquals(EMPTY_ATTRIBUTES_CASE, CaseEmitter.emit(only(MODEL + EMPTY_ATTRIBUTES)));
        assertEquals(List.of(), warnings(ScenarioResolver.resolve(NEE + MODEL + EMPTY_ATTRIBUTES)));
    }

    @Test void attribute_is_leeg_warns_in_an_ordinary_document_unless_it_replaces_an_inherited_value() {
        String plain = MODEL + EMPTY_ATTRIBUTES;
        ScenarioBundle b = ScenarioResolver.resolve(plain);
        assertTrue(b.isOk(), () -> b.model().errors().toString());
        List<String> expected = List.of(
                ScenarioFixture.lineOf(plain, "- de afhaaldatum is leeg") + " RGS-473 {written=de afhaaldatum is leeg}",
                ScenarioFixture.lineOf(plain, "- het gewicht is leeg.") + " RGS-473 {written=het gewicht is leeg}");
        assertEquals(expected, warnings(b));
        assertTrue(b.model().diagnostics.stream().noneMatch(d -> "RGS-473".equals(d.code())));
        assertEquals(IrEmitter.emit(Resolver.resolve(MODEL)), IrEmitter.emit(b.model()));
        assertEquals("scenario: 'de afhaaldatum is leeg' verandert in een document zonder 'Afleiden uit leeg: nee' "
                        + "niets: daar is een attribuut dat de situatie niet noemt al leeg; de zin telt alleen in een "
                        + "document met 'Afleiden uit leeg: nee'",
                b.warnings().get(0).message());
        List<String> tokens = new ArrayList<>();
        for (SemanticTokens.FileDiagnostic d : Resolver.tokens(List.of(new SemanticTokens.SourceFile("t.rgs", plain))).diagnostics())
            if (d.severity() == Severity.WARNING) tokens.add(d.line() + " " + d.code() + " " + new TreeMap<>(d.params()));
        assertEquals(expected, tokens);

        String replacing = MODEL + """
                Situatie gewogen:
                een Vondst V1
                - het gewicht is 15

                Situatie ongewogen gebaseerd op de situatie gewogen:
                een Vondst V1
                - het gewicht is leeg

                Scenario intrekken
                gebaseerd op de situatie gewogen
                Gegeven is de volgende situatie:
                een Vondst V1
                - het gewicht is leeg - de afhaaldatum is leeg
                Met als verwacht resultaat:
                - de gegevens zijn consistent
                """;
        ScenarioBundle rb = ScenarioResolver.resolve(replacing);
        assertTrue(rb.isOk(), () -> rb.model().errors().toString());
        assertEquals(List.of(ScenarioFixture.lineOf(replacing, "- het gewicht is leeg - de afhaaldatum is leeg")
                + " RGS-473 {written=de afhaaldatum is leeg}"), warnings(rb));
    }

    @Test void an_attribute_filled_and_empty_in_one_block_is_refused() {
        String filledThenEmpty = NEE + MODEL + """
                Scenario tegenspraak
                Gegeven is de volgende situatie:
                een Vondst V1
                - het gewicht is 10
                - het gewicht is leeg
                Met als verwacht resultaat:
                - de gegevens zijn consistent
                """;
        assertEquals(List.of("RGS-470 {attribute=gewicht, label=V1}"), errorsOn(filledThenEmpty, "- het gewicht is leeg"));

        String emptyThenFilled = NEE + MODEL + """
                Scenario tegenspraak
                Gegeven is de volgende situatie:
                een Vondst V1
                - het gewicht is leeg
                - het gewicht is 10
                Met als verwacht resultaat:
                - de gegevens zijn consistent
                """;
        assertEquals(List.of("RGS-470 {attribute=gewicht, label=V1}"), errorsOn(emptyThenFilled, "- het gewicht is 10"));

        String twoOpeners = NEE + MODEL + """
                Situatie tweemaal V1:
                een Vondst V1
                - de afhaaldatum is leeg
                een Vondst V1
                - de afhaaldatum is 01-06-2025

                Scenario tegenspraak
                gebaseerd op de situatie tweemaal V1
                Met als verwacht resultaat:
                - de gegevens zijn consistent
                """;
        assertEquals(List.of("RGS-470 {attribute=afhaaldatum, label=V1}"),
                errorsOn(twoOpeners, "- de afhaaldatum is 01-06-2025"));
    }

    @Test void based_on_replaces_attribute_values_and_is_leeg_in_both_directions() {
        String text = NEE + MODEL + """
                Situatie met gewicht:
                een Vondst V1
                - het gewicht is 10
                - de afhaaldatum is 01-06-2025

                Situatie zonder gewicht:
                een Vondst V1
                - het gewicht is leeg
                - de afhaaldatum is 01-06-2025

                Situatie gewicht weg gebaseerd op de situatie met gewicht:
                een Vondst V1
                - het gewicht is leeg

                Situatie gewicht terug gebaseerd op de situatie zonder gewicht:
                een Vondst V1
                - het gewicht is 25

                Scenario keten weg
                gebaseerd op de situatie gewicht weg
                Met als verwacht resultaat:
                - de gegevens zijn consistent

                Scenario keten terug
                gebaseerd op de situatie gewicht terug
                Met als verwacht resultaat:
                - de gegevens zijn consistent

                Scenario twee bases weg
                gebaseerd op de situatie met gewicht
                gebaseerd op de situatie zonder gewicht
                Met als verwacht resultaat:
                - de gegevens zijn consistent

                Scenario twee bases terug
                gebaseerd op de situatie zonder gewicht
                gebaseerd op de situatie met gewicht
                Met als verwacht resultaat:
                - de gegevens zijn consistent
                """;
        List<ResolvedScenario> s = scenarios(text);
        assertEquals(4, s.size());
        String gone = """
                {
                  "format": "uchar-case-v0",
                  "asOfDate": null,
                  "input": {
                    "instances": [
                      {
                        "label": "V1",
                        "objectType": "Vondst",
                        "attributes": {
                          "afhaaldatum": {
                            "date": "2025-06-01"
                          }
                        },
                        "attributesEmpty": [
                          "gewicht"
                        ],
                        "roles": {},
                        "characteristics": []
                      }
                    ],
                    "parameters": {}
                  },
                  "expected": [
                    {
                      "kind": "consistent"
                    }
                  ]
                }
                """;
        assertEquals(gone, CaseEmitter.emit(s.get(0)));
        assertEquals(gone, CaseEmitter.emit(s.get(2)));
        assertTrue(CaseEmitter.emit(s.get(1)).contains("\"number\": \"25\"")
                && !CaseEmitter.emit(s.get(1)).contains("attributesEmpty"));
        assertTrue(CaseEmitter.emit(s.get(3)).contains("\"number\": \"10\"")
                && !CaseEmitter.emit(s.get(3)).contains("attributesEmpty"));
    }

    @Test void attribute_is_leeg_refuses_unknown_period_timeline_and_dimensioned_attributes() {
        String bullets = """
                Scenario geweigerd
                Gegeven is de volgende situatie:
                een Vondst V1
                - de onzin is leeg
                - de afhaaldatum is leeg van 01-01-2025 tot 01-02-2025
                - de opslagkosten is leeg
                - het gewicht is leeg
                een Vondst V2
                - de opslagkosten is leeg van 01-01-2025 tot 01-02-2025
                - het seizoensgewicht is leeg
                - het seizoensgewicht in voorjaar is leeg
                Met als verwacht resultaat:
                - de gegevens zijn consistent
                """;
        for (String scenario : List.of(NEE + MODEL + bullets, MODEL + bullets)) {
            assertEquals(List.of("RGS-236 {attribute=onzin, objectType=Vondst}"),
                    errorsOn(scenario, "- de onzin is leeg"));
            assertEquals(List.of("RGS-471 {attribute=afhaaldatum}"),
                    errorsOn(scenario, "afhaaldatum is leeg van"));
            assertEquals(List.of("RGS-471 {attribute=opslagkosten}"),
                    errorsOn(scenario, "- de opslagkosten is leeg"));
            assertEquals(List.of("RGS-471 {attribute=opslagkosten}"),
                    errorsOn(scenario, "opslagkosten is leeg van"));
            assertEquals(List.of("RGS-472 {attribute=seizoensgewicht, written=seizoensgewicht}"),
                    errorsOn(scenario, "- het seizoensgewicht is leeg"));
            assertEquals(List.of("RGS-472 {attribute=seizoensgewicht, written=seizoensgewicht in voorjaar}"),
                    errorsOn(scenario, "seizoensgewicht in voorjaar is leeg"));
        }
    }

    @Test void attributesEmpty_stands_between_attributes_and_roles_and_names_with_leeg_parse_clean() {
        String keyOrder = NEE + MODEL + """
                Objecttype de Jutter (mv: Jutters) (bezield)

                Feittype vondstmelding
                    de melder Jutter
                    de vondst (mv: vondsten) Vondst
                Eén melder heeft meerdere vondsten

                Scenario sleutelvolgorde
                Gegeven is de volgende situatie:
                een Jutter J1
                een Vondst V1
                - is aangespoeld van 01-01-2025 tot 01-02-2025
                - is niet drijfhout
                - is afhaalbaar
                - de melder is J1
                - de afhaaldatum is leeg
                - het gewicht is 7
                Met als verwacht resultaat:
                - de gegevens zijn consistent
                """;
        String emitted = CaseEmitter.emit(only(keyOrder));
        int v1At = emitted.indexOf("\"label\": \"V1\"");
        String v1Slice = emitted.substring(v1At);
        List<String> keys = List.of("\"label\"", "\"objectType\"", "\"attributes\"", "\"attributesEmpty\"",
                "\"roles\"", "\"characteristics\"", "\"characteristicsAbsent\"", "\"characteristicTimelines\"");
        int prev = -1;
        for (String key : keys) {
            int pos = v1Slice.indexOf(key);
            assertTrue(pos > prev, () -> key + " out of order in:\n" + v1Slice);
            prev = pos;
        }

        String leegInName = NEE + """
                Objecttype de Vondst (mv: Vondsten)
                    het leeg gewicht Numeriek (geheel getal);
                    is vrijgesteld kenmerk (bijvoeglijk);

                Regel vrijstelling
                    geldig altijd
                        Een Vondst is vrijgesteld indien zijn leeg gewicht leeg is.

                Scenario leeg in naam
                Gegeven is de volgende situatie:
                een Vondst V1
                - het leeg gewicht is 10
                een Vondst V2
                - het leeg gewicht is leeg
                Met als verwacht resultaat:
                - het leeg gewicht van V1 is 10
                - V1 is niet vrijgesteld
                - het leeg gewicht van V2 is leeg
                - V2 is vrijgesteld
                """;
        String leegCase = CaseEmitter.emit(only(leegInName));
        assertTrue(leegCase.contains("\"leeg gewicht\": {\n            \"number\": \"10\"\n          }"), leegCase);
        assertTrue(leegCase.contains("\"attributesEmpty\": [\n          \"leeg gewicht\"\n        ]"), leegCase);
    }
}
