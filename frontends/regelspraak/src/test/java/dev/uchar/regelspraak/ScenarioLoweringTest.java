package dev.uchar.regelspraak;

import dev.uchar.regelspraak.emit.CaseEmitter;
import dev.uchar.regelspraak.emit.IrEmitter;
import dev.uchar.regelspraak.ir.ResolvedScenario;
import dev.uchar.regelspraak.resolver.Resolver;
import dev.uchar.regelspraak.resolver.ScenarioResolver;
import dev.uchar.regelspraak.resolver.ScenarioResolver.ScenarioBundel;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static dev.uchar.regelspraak.ScenarioFixture.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * The lowering of the scenariotoetsen extension: a resolved scenario is one case file of the format uchar-case-v0,
 * and a scope is the set of forms the IR document beside it carries. The expected case file below is written by hand
 * from the case schema — every value variant, both timelines, a parameter timeline, the corpus key order — so the
 * lowering answers to the format and not to itself. The corpus witnesses (the golden scenario documents, regenerated
 * byte-identically by their gate) answer to the pinned files.
 */
class ScenarioLoweringTest {

    private static final String SCENARIO = """
        Scenario één wrak
        met peildatum 15-03-2024
        Gegeven is de volgende situatie:
        een Jutter J
        - de naam is "Aukje"
        - het aandeel is 25 %
        - de vondstklasse is 'Strandgoed'
        - het meldtijdstip is 03-02-2024 10:15:00.000
        - de borgdagen is 4 van 01-01-2024 tot 01-02-2024
        - is borgplichtig van 01-01-2024 tot 15-01-2024
        - heeft strandrecht
        een Vondst V
        - de sleepafstand is 1 voet
        - de aanmelder is J
        de parameter de vondstdrempel is 3
        de parameter het penningbedrag is 7 van 01-01-2024 tot 01-01-2025
        Met als verwacht resultaat:
        - de kustwachtbonus van J is 3
        - de wachtbonus van J is leeg
        - de borgdagen van J is 4 van 01-01-2024 tot 01-02-2024
        - J is niet bergingsbevoegd
        - J heeft strandrecht
        - de gegevens zijn consistent
        """;

    private static final String CASEBESTAND = """
        {
          "format": "uchar-case-v0",
          "asOfDate": "2024-03-15",
          "input": {
            "instances": [
              {
                "label": "J",
                "objectType": "Jutter",
                "attributes": {
                  "naam": {
                    "text": "Aukje"
                  },
                  "aandeel": {
                    "number": "25"
                  },
                  "vondstklasse": {
                    "enumeration": "strandgoed"
                  },
                  "meldtijdstip": {
                    "dateTime": "2024-02-03T10:15:00.000"
                  },
                  "borgdagen": {
                    "timeline": [
                      {
                        "from": "2024-01-01",
                        "to": "2024-02-01",
                        "value": {
                          "number": "4"
                        }
                      }
                    ]
                  }
                },
                "roles": {},
                "characteristics": [
                  "strandrecht"
                ],
                "characteristicTimelines": {
                  "borgplichtig": [
                    {
                      "from": "2024-01-01",
                      "to": "2024-01-15"
                    }
                  ]
                }
              },
              {
                "label": "V",
                "objectType": "Vondst",
                "attributes": {
                  "sleepafstand": {
                    "number": "1/6"
                  }
                },
                "roles": {
                  "aanmelder": [
                    "J"
                  ]
                },
                "characteristics": []
              }
            ],
            "parameters": {
              "vondstdrempel": {
                "number": "3"
              },
              "penningbedrag": {
                "timeline": [
                  {
                    "from": "2024-01-01",
                    "to": "2025-01-01",
                    "value": {
                      "number": "7"
                    }
                  }
                ]
              }
            }
          },
          "expected": [
            {
              "kind": "attributeValue",
              "label": "J",
              "attribute": "kustwachtbonus",
              "value": {
                "number": "3"
              }
            },
            {
              "kind": "attributeEmpty",
              "label": "J",
              "attribute": "wachtbonus"
            },
            {
              "kind": "attributeTimeline",
              "label": "J",
              "attribute": "borgdagen",
              "timeline": [
                {
                  "from": "2024-01-01",
                  "to": "2024-02-01",
                  "value": {
                    "number": "4"
                  }
                }
              ]
            },
            {
              "kind": "characteristicAbsent",
              "label": "J",
              "characteristic": "bergingsbevoegd"
            },
            {
              "kind": "characteristicPresent",
              "label": "J",
              "characteristic": "strandrecht"
            },
            {
              "kind": "consistent"
            }
          ]
        }
        """;

    private static ResolvedScenario enig(ScenarioBundel b) {
        assertTrue(b.isOk(), () -> b.model().errors().toString());
        assertEquals(1, b.scenarios().size());
        return b.scenarios().get(0);
    }

    @Test void het_casebestand_volgt_het_caseformaat() {
        ResolvedScenario s = enig(resolve(SCENARIO));
        assertEquals(CASEBESTAND, CaseEmitter.emit(s));
        assertEquals("case1-een-wrak.json", CaseEmitter.bestandsnaam(1, s));
    }

    @Test void de_sleutels_van_een_inconsistentie() {
        String kop = "Situatie k:\neen Jutter J\nde parameter de vondstdrempel is 3\n"
                + "Scenario k\nmet peildatum 01-06-2025\ngebaseerd op de situatie k\nMet als verwacht resultaat:\n";
        assertTrue(CaseEmitter.emit(enig(resolve(kop + "- de gegevens zijn inconsistent\n")))
                .contains("  \"expected\": [\n    {\n      \"kind\": \"inconsistent\"\n    }\n  ]\n"));
        assertTrue(CaseEmitter.emit(enig(resolve(kop + "- de gegevens zijn inconsistent volgens \"geen wrakgoed\" voor J\n")))
                .contains("      \"kind\": \"inconsistent\",\n      \"rule\": \"geen wrakgoed\",\n      \"label\": \"J\"\n"));
    }

    @Test void de_bestandsnaam() {
        assertEquals("een-wrak-twee-jutters", CaseEmitter.slug("Één wrak, twee  jutters!"));
        assertEquals("y-wint", CaseEmitter.slug("y wint"));
        assertEquals("", CaseEmitter.slug("-"));
    }

    // ---------------- the scope and the IR document ----------------

    private static final String REGELS = """
        Objecttype de Jutter (mv: Jutters) (bezield)
            de vondstteller Numeriek (geheel getal);
            de bonus Numeriek (geheel getal);
            de premie Numeriek (geheel getal);
            de rubriek Tekst;
            is ervaren kenmerk (bijvoeglijk);

        Regel bonus van een jutter
            geldig altijd
                De bonus van een Jutter moet berekend worden als de vondstteller van de Jutter maal 2.

        Beslistabel rubriek van een jutter
            geldig altijd
        | | de rubriek van een Jutter moet gesteld worden op | indien zijn vondstteller groter is dan | indien zijn vondstteller kleiner of gelijk is aan |
        |---|---|---|---|
        | 1 | "veel" | 10 | n.v.t. |
        | 2 | "weinig" | n.v.t. | 10 |

        Regelgroep premies
        Regel premie van een jutter
            geldig altijd
                De premie van een Jutter moet berekend worden als de vondstteller van de Jutter plus 1.
        Regel ervaring van een jutter
            geldig altijd
                Een Jutter is ervaren
                indien zijn vondstteller groter is dan 5.
        """;

    /** The rule names and rule groups of the IR document a one-scenario document with header {@code scope} lowers to. */
    private static String ir(String scope) {
        ScenarioBundel b = ScenarioResolver.resolve(REGELS + "Scenario s\n" + scope
                + "Gegeven is de volgende situatie:\neen Jutter J\n- de vondstteller is 12\n"
                + "Met als verwacht resultaat:\n- de gegevens zijn consistent\n");
        ResolvedScenario s = enig(b);
        return IrEmitter.emit(CaseEmitter.metScope(b.model(), s.scope()));
    }

    private static List<String> regelnamen(String ir) {
        List<String> uit = new ArrayList<>();
        Matcher m = Pattern.compile("\"node\": \"Rule\",\\s*\"name\": \"([^\"]+)\"").matcher(ir);
        while (m.find()) uit.add(m.group(1));
        return uit;
    }

    @Test void zonder_scope_is_het_ir_document_dat_van_de_productieroute() {
        String productie = IrEmitter.emit(Resolver.resolve(REGELS));
        assertEquals(productie, ir(""));
        assertEquals(List.of("bonus van een jutter", "premie van een jutter", "ervaring van een jutter",
                "rubriek van een jutter 1", "rubriek van een jutter 2"), regelnamen(productie));
        assertEquals(IrEmitter.emit(Resolver.resolve(MODEL)), IrEmitter.emit(resolve(SCENARIO).model()),
                "de scenarioroute bouwt het model met de pijplijn van de productieroute");
    }

    @Test void de_scope_draagt_alleen_wat_zij_noemt() {
        assertEquals(List.of("bonus van een jutter"), regelnamen(ir("toetst de regel bonus van een jutter\n")));
        String tabel = ir("toetst de regels rubriek van een jutter en bonus van een jutter\n");
        assertEquals(List.of("bonus van een jutter", "rubriek van een jutter 1", "rubriek van een jutter 2"),
                regelnamen(tabel), "een beslistabel bij haar naam draagt al haar rijen");
        assertFalse(tabel.contains("\"node\": \"RuleGroup\""), "een groep zonder gedragen lid valt weg");
        String groep = ir("toetst de regelgroep premies\n");
        assertEquals(List.of("premie van een jutter", "ervaring van een jutter"), regelnamen(groep));
        assertTrue(groep.contains("\"node\": \"RuleGroup\""));
        String lid = ir("toetst de regel ervaring van een jutter\n");
        assertEquals(List.of("ervaring van een jutter"), regelnamen(lid));
        assertTrue(lid.contains("\"members\": [\n          \"ervaring van een jutter\"\n        ]"),
                () -> "de groep houdt alleen haar gedragen lid:\n" + lid);
    }

    @Test void een_scope_die_een_regelgroep_als_regel_noemt() {
        ScenarioBundel b = ScenarioResolver.resolve(REGELS + "Scenario s\ntoetst de regel premies\n"
                + "Gegeven is de volgende situatie:\neen Jutter J\nMet als verwacht resultaat:\n- de gegevens zijn consistent\n");
        assertTrue(b.model().errors().stream().anyMatch(d -> d.message()
                .contains("'premies' uit de scope is een regelgroep; schrijf 'toetst de regelgroep premies'")),
                () -> b.model().errors().toString());
    }

    @Test void een_document_een_ir_document() {
        ScenarioBundel b = ScenarioResolver.resolve(REGELS
                + "Scenario een\ntoetst de regel bonus van een jutter\nGegeven is de volgende situatie:\neen Jutter J\n"
                + "Met als verwacht resultaat:\n- de gegevens zijn consistent\n"
                + "Scenario twee\nGegeven is de volgende situatie:\neen Jutter J\n"
                + "Met als verwacht resultaat:\n- de gegevens zijn consistent\n");
        assertTrue(b.model().errors().stream().anyMatch(d -> d.message().contains("de scenario's van één document "
                + "lopen tegen één IR-document, maar het scenario 'twee' toetst andere regels dan het scenario 'een'")),
                () -> b.model().errors().toString());
    }

    @Test void een_scope_noemt_een_defaultsgroep_bij_haar_basisregel() {
        String dialect = """
            Objecttype de Jutter (mv: Jutters) (bezield)
                is vergunning geweigerd kenmerk (bijvoeglijk);
                is weigering herroepen kenmerk (bijvoeglijk);
                is uitgesloten kenmerk (bijvoeglijk);

            Regel uitsluiting
                geldig altijd
                    Een Jutter is uitgesloten
                    indien hij vergunning geweigerd is.

            Uitzondering op uitsluiting: herroepen weigering
                geldig altijd
                    Een Jutter is niet uitgesloten
                    indien hij weigering herroepen is.
            """;
        String staart = "Gegeven is de volgende situatie:\neen Jutter J\nMet als verwacht resultaat:\n- J is niet uitgesloten\n";
        ScenarioBundel fout = ScenarioResolver.resolve(dialect + "Scenario s\ntoetst de regel herroepen weigering\n" + staart);
        assertTrue(fout.model().errors().stream().anyMatch(d -> d.message().contains("de regel 'herroepen weigering' uit "
                + "de scope is opgegaan in een defaultsgroep; de scope noemt die groep bij de naam van haar basisregel")),
                () -> fout.model().errors().toString());
        ScenarioBundel goed = ScenarioResolver.resolve(dialect + "Scenario s\ntoetst de regel uitsluiting\n" + staart);
        ResolvedScenario s = enig(goed);
        assertEquals(IrEmitter.emit(Resolver.resolve(dialect)), IrEmitter.emit(CaseEmitter.metScope(goed.model(), s.scope())),
                "de basisregel draagt de hele defaultsgroep");
    }

    // ---------------- the CLI ----------------

    private record Uitvoer(int code, String tekst) {}

    private static Uitvoer cli(Path ir, Path map, Path... bronnen) throws IOException {
        List<String> rest = new ArrayList<>();
        rest.add(ir.toString());
        for (Path p : bronnen) rest.add(p.toString());
        PrintStream oud = System.out;
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        System.setOut(new PrintStream(buf, true, java.nio.charset.StandardCharsets.UTF_8));
        try {
            return new Uitvoer(Resolver.casesModus(rest, map), buf.toString(java.nio.charset.StandardCharsets.UTF_8));
        } finally {
            System.setOut(oud);
        }
    }

    private static List<String> bestanden(Path map) throws IOException {
        if (!Files.isDirectory(map)) return List.of();
        try (Stream<Path> s = Files.list(map)) { return s.map(p -> p.getFileName().toString()).sorted().toList(); }
    }

    @Test void de_casesmodus_schrijft_het_ir_document_en_de_casebestanden() throws IOException {
        Path d = Files.createTempDirectory("scenario-cli");
        Path bron = Files.writeString(d.resolve("bron.rgs"), MODEL);
        Path scen = Files.writeString(d.resolve("scenario.rgs"), SCENARIO);
        Uitvoer u = cli(d.resolve("ir.json"), d.resolve("cases"), bron, scen);
        assertEquals(0, u.code(), u.tekst());
        assertEquals(List.of("case1-een-wrak.json"), bestanden(d.resolve("cases")));
        assertEquals(CASEBESTAND, Files.readString(d.resolve("cases/case1-een-wrak.json")));
        assertEquals(IrEmitter.emit(Resolver.resolve(MODEL)), Files.readString(d.resolve("ir.json")));
    }

    @Test void de_casesmodus_schrijft_niets_bij_een_fout_en_noemt_bestand_en_regel() throws IOException {
        Path d = Files.createTempDirectory("scenario-cli");
        Path bron = Files.writeString(d.resolve("bron.rgs"), MODEL);
        Path scen = Files.writeString(d.resolve("scenario.rgs"), SCENARIO.replace("met peildatum 15-03-2024",
                "met peildatum 15-03-2024 12:00:00.000"));
        Uitvoer u = cli(d.resolve("ir.json"), d.resolve("cases"), bron, scen);
        assertEquals(1, u.code());
        assertTrue(u.tekst().contains("de peildatum '15-03-2024 12:00:00.000' draagt een tijd")
                && u.tekst().contains("scenario.rgs:2]"), u.tekst());
        assertFalse(Files.exists(d.resolve("ir.json")), "een weigering schrijft geen IR-document");
        assertEquals(List.of(), bestanden(d.resolve("cases")), "een weigering schrijft geen casebestand");
    }

    @Test void een_document_zonder_scenario_heeft_niets_te_loweren() throws IOException {
        Path d = Files.createTempDirectory("scenario-cli");
        Path bron = Files.writeString(d.resolve("bron.rgs"), MODEL);
        Uitvoer u = cli(d.resolve("ir.json"), d.resolve("cases"), bron);
        assertEquals(1, u.code());
        assertTrue(u.tekst().contains("het document draagt geen scenario, dus er valt geen casebestand te schrijven"),
                u.tekst());
    }
}
