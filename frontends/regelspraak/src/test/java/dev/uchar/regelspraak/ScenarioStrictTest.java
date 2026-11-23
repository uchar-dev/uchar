package dev.uchar.regelspraak;

import dev.uchar.regelspraak.emit.CaseEmitter;
import dev.uchar.regelspraak.ir.ResolvedScenario;
import dev.uchar.regelspraak.model.RegelSpraakModel.Diagnostic;
import dev.uchar.regelspraak.resolver.ScenarioResolver;
import dev.uchar.regelspraak.resolver.ScenarioResolver.ScenarioBundle;
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

    /** The instance part of a case file of BASES with {@code characteristics} as given. */
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

    /** A document without 'Afleiden uit leeg: nee' has no open member, so there the expectation never holds. */
    @Test void staat_niet_vast_is_refused_in_a_document_that_infers_from_empty() {
        String scenario = MODEL + OPEN;
        assertEquals(List.of("RGS-467 {label=V1, member=het attribuut 'afhaaldatum'}"),
                errorsOn(scenario, "afhaaldatum van V1 staat"));
        assertEquals(List.of("RGS-467 {label=V1, member=het kenmerk 'afhaalbaar'}"),
                errorsOn(scenario, "of V1 afhaalbaar is"));
        assertEquals(List.of("RGS-467 {label=V1, member=het kenmerk 'strandrecht'}"),
                errorsOn(scenario, "of V1 strandrecht heeft"));
    }

    @Test void staat_niet_vast_on_a_timeline_a_cell_or_with_the_wrong_verb_is_refused() {
        String scenario = NEE + MODEL + """
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
        assertEquals(List.of("RGS-399 {attribute=opslagkosten, label=V1}"), errorsOn(scenario, "opslagkosten van V1"));
        assertEquals(List.of("RGS-400 {characteristic=aangespoeld, label=V1}"), errorsOn(scenario, "aangespoeld is"));
        assertEquals(List.of("RGS-468 {attribute=seizoensgewicht, written=seizoensgewicht in voorjaar}"),
                errorsOn(scenario, "seizoensgewicht in voorjaar"));
        assertEquals(List.of("RGS-244 {characteristic=strandrecht}"), errorsOn(scenario, "strandrecht is,"));
        // the open form takes no negation: the name is read as written
        assertEquals(List.of("RGS-241 {characteristic=niet afhaalbaar, objectType=Vondst}"),
                errorsOn(scenario, "niet afhaalbaar is"));
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
}
