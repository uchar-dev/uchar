package dev.uchar.regelspraak;

import dev.uchar.regelspraak.emit.CaseEmitter;
import dev.uchar.regelspraak.emit.IrEmitter;
import dev.uchar.regelspraak.ir.ResolvedScenario;
import dev.uchar.regelspraak.model.RegelSpraakModel;
import dev.uchar.regelspraak.resolver.Resolver;
import dev.uchar.regelspraak.resolver.ScenarioResolver;
import dev.uchar.regelspraak.resolver.ScenarioResolver.ScenarioBundle;
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

    private static final String CASE_FILE = """
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

    private static ResolvedScenario only(ScenarioBundle b) {
        assertTrue(b.isOk(), () -> b.model().errors().toString());
        assertEquals(1, b.scenarios().size());
        return b.scenarios().get(0);
    }

    @Test void the_case_file_follows_the_case_format() {
        ResolvedScenario s = only(resolve(SCENARIO));
        assertEquals(CASE_FILE, CaseEmitter.emit(s));
        assertEquals("case1-een-wrak.json", CaseEmitter.fileName(1, s));
    }

    @Test void the_keys_of_an_inconsistency() {
        String head = "Situatie k:\neen Jutter J\nde parameter de vondstdrempel is 3\n"
                + "Scenario k\nmet peildatum 01-06-2025\ngebaseerd op de situatie k\nMet als verwacht resultaat:\n";
        assertTrue(CaseEmitter.emit(only(resolve(head + "- de gegevens zijn inconsistent\n")))
                .contains("  \"expected\": [\n    {\n      \"kind\": \"inconsistent\"\n    }\n  ]\n"));
        assertTrue(CaseEmitter.emit(only(resolve(head + "- de gegevens zijn inconsistent volgens \"geen wrakgoed\" voor J\n")))
                .contains("      \"kind\": \"inconsistent\",\n      \"rule\": \"geen wrakgoed\",\n      \"label\": \"J\"\n"));
    }

    @Test void the_file_name() {
        assertEquals("een-wrak-twee-jutters", CaseEmitter.slug("Één wrak, twee  jutters!"));
        assertEquals("y-wint", CaseEmitter.slug("y wint"));
        assertEquals("", CaseEmitter.slug("-"));
    }

    // ---------------- the scope and the IR document ----------------

    private static final String RULES = """
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
        ScenarioBundle b = ScenarioResolver.resolve(RULES + "Scenario s\n" + scope
                + "Gegeven is de volgende situatie:\neen Jutter J\n- de vondstteller is 12\n"
                + "Met als verwacht resultaat:\n- de gegevens zijn consistent\n");
        ResolvedScenario s = only(b);
        return IrEmitter.emit(CaseEmitter.withScope(b.model(), s.scope()));
    }

    private static List<String> ruleNames(String ir) {
        List<String> result = new ArrayList<>();
        Matcher m = Pattern.compile("\"node\": \"Rule\",\\s*\"name\": \"([^\"]+)\"").matcher(ir);
        while (m.find()) result.add(m.group(1));
        return result;
    }

    @Test void without_scope_is_the_ir_document_that_is_of_the_production_route() {
        String production = IrEmitter.emit(Resolver.resolve(RULES));
        assertEquals(production, ir(""));
        assertEquals(List.of("bonus van een jutter", "premie van een jutter", "ervaring van een jutter",
                "rubriek van een jutter 1", "rubriek van een jutter 2"), ruleNames(production));
        assertEquals(IrEmitter.emit(Resolver.resolve(MODEL)), IrEmitter.emit(resolve(SCENARIO).model()),
                "de scenarioroute bouwt het model met de pijplijn van de productieroute");
    }

    @Test void the_scope_carries_only_what_it_names() {
        assertEquals(List.of("bonus van een jutter"), ruleNames(ir("toetst de regel bonus van een jutter\n")));
        String table = ir("toetst de regels rubriek van een jutter en bonus van een jutter\n");
        assertEquals(List.of("bonus van een jutter", "rubriek van een jutter 1", "rubriek van een jutter 2"),
                ruleNames(table), "een beslistabel bij haar naam draagt al haar rijen");
        assertFalse(table.contains("\"node\": \"RuleGroup\""), "een groep zonder gedragen lid valt weg");
        String group = ir("toetst de regelgroep premies\n");
        assertEquals(List.of("premie van een jutter", "ervaring van een jutter"), ruleNames(group));
        assertTrue(group.contains("\"node\": \"RuleGroup\""));
        String member = ir("toetst de regel ervaring van een jutter\n");
        assertEquals(List.of("ervaring van een jutter"), ruleNames(member));
        assertTrue(member.contains("\"members\": [\n          \"ervaring van een jutter\"\n        ]"),
                () -> "de groep houdt alleen haar gedragen lid:\n" + member);
    }

    @Test void a_scope_that_names_a_rule_group_as_rule() {
        ScenarioBundle b = ScenarioResolver.resolve(RULES + "Scenario s\ntoetst de regel premies\n"
                + "Gegeven is de volgende situatie:\neen Jutter J\nMet als verwacht resultaat:\n- de gegevens zijn consistent\n");
        assertTrue(b.model().errors().stream().anyMatch(d -> d.message()
                .contains("'premies' uit de scope is een regelgroep; schrijf 'toetst de regelgroep premies'")),
                () -> b.model().errors().toString());
    }

    @Test void a_document_an_ir_document() {
        ScenarioBundle b = ScenarioResolver.resolve(RULES
                + "Scenario een\ntoetst de regel bonus van een jutter\nGegeven is de volgende situatie:\neen Jutter J\n"
                + "Met als verwacht resultaat:\n- de gegevens zijn consistent\n"
                + "Scenario twee\nGegeven is de volgende situatie:\neen Jutter J\n"
                + "Met als verwacht resultaat:\n- de gegevens zijn consistent\n");
        assertTrue(b.model().errors().stream().anyMatch(d -> d.message().contains("de scenario's van één document "
                + "lopen tegen één IR-document, maar het scenario 'twee' toetst andere regels dan het scenario 'een'")),
                () -> b.model().errors().toString());
    }

    @Test void a_scope_names_a_defaults_group_at_its_base_rule() {
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
        String suffix = "Gegeven is de volgende situatie:\neen Jutter J\nMet als verwacht resultaat:\n- J is niet uitgesloten\n";
        ScenarioBundle error = ScenarioResolver.resolve(dialect + "Scenario s\ntoetst de regel herroepen weigering\n" + suffix);
        assertTrue(error.model().errors().stream().anyMatch(d -> d.message().contains("de regel 'herroepen weigering' uit "
                + "de scope is opgegaan in een defaultsgroep; de scope noemt die groep bij de naam van haar basisregel")),
                () -> error.model().errors().toString());
        ScenarioBundle goed = ScenarioResolver.resolve(dialect + "Scenario s\ntoetst de regel uitsluiting\n" + suffix);
        ResolvedScenario s = only(goed);
        assertEquals(IrEmitter.emit(Resolver.resolve(dialect)), IrEmitter.emit(CaseEmitter.withScope(goed.model(), s.scope())),
                "de basisregel draagt de hele defaultsgroep");
    }

    // ---------------- the scoped document is a copy of the whole model ----------------

    /** A document that fills every collection of the model: each wire section, a WARNING among the diagnostics. */
    private static final String RIJK = """
        Vocabulaire handboek

        Eenheidsysteem Munt
            de euro (mv: euros) EUR €

        Domein Vondstklasse is van het type Enumeratie
            'drijfgoed'
            'wrakgoed'

        Dimensie het seizoen, bestaande uit de seizoenen (na het attribuut met voorzetsel in):
            1. voorjaar
            2. najaar

        Tijdlijn het wachtjaar heeft tijdvakken van een jaar met variabel startpunt;

        Dagsoort de havendag (mv: havendagen);

        Parameter de vondstdrempel : Numeriek (geheel getal)

        Objecttype de Haven (mv: Havens) (handboek: aanlegplaats)
            de naam Tekst;
            de opbrengst Numeriek (geheel getal) met eenheid €;
            de onverdeelde rest Numeriek (geheel getal) met eenheid €;
            de vangst Numeriek (geheel getal);
            is veilig kenmerk (bijvoeglijk);

        Objecttype de Schuit (mv: Schuiten)
            de naam Tekst;
            het nummer Numeriek (geheel getal);
            het aandeel Numeriek (geheel getal) met eenheid €;
            de vondstklasse Vondstklasse;
            de seizoensvangst Numeriek (geheel getal) gedimensioneerd met seizoen;
            de aanvangsdatum Datum in dagen;
            de wachtdagen Numeriek (geheel getal) voor elke wachtjaar;
            de romplengte Numeriek (geheel getal);
            is gekeurd kenmerk (bijvoeglijk);
            is gemeld kenmerk (bijvoeglijk);

        Feittype thuisligging
            de thuishaven Haven
            de ligger (mv: liggers) Schuit
        Eén thuishaven telt meerdere liggers

        Feittype winterberging
            de winterhaven Haven
            de winterschuit (mv: winterschuiten) Schuit
        Eén winterhaven telt meerdere winterschuiten

        Regel Dagsoortdefinitie havendag
            geldig altijd
                Een dag is een havendag
                indien de dag uit de dag gelijk is aan 1.

        Regel startpunt wachtjaar
            geldig altijd
                Het startpunt van het wachtjaar voor een Schuit wordt bepaald door zijn aanvangsdatum.

        Regel romplengte
            geldig altijd
                De romplengte van een Schuit moet gesteld worden op de vondstdrempel.

        Regel keuring
            geldig altijd
                Een Schuit is gekeurd
                indien zijn romplengte groter is dan 3.

        Uitzondering op keuring: geen keuring
            geldig altijd
                Een Schuit is niet gekeurd
                indien zijn nummer groter is dan 9.

        Regelgroep meldingen
        Regel melding
            geldig altijd
                Een Schuit is gemeld
                indien zijn nummer groter is dan 1.

        Regel nieuwe ligger
            geldig altijd
                Een Haven heeft een ligger met romplengte gelijk aan 3
                indien de Haven veilig is.

        Regel winterligging
            geldig altijd
                Een winterschuit van een Haven is een ligger van de Haven
                indien de Haven veilig is.

        Regel verdeel de opbrengst
            geldig altijd
                De opbrengst van een Haven wordt verdeeld over de aandelen van alle liggers van de Haven, waarbij wordt verdeeld:
                - in gelijke delen,
                - afgerond op 0 decimalen naar beneden.
                Als onverdeelde rest blijft de onverdeelde rest van de Haven over.

        Regel nummer positief
            geldig altijd
                Het nummer van een Schuit moet groter zijn dan 0.

        Regel nummers uniek
            geldig altijd
                De nummers van alle Schuiten moeten uniek zijn.

        Regel namen uniek
            geldig altijd
                De namen van alle Havens verenigd met de namen van alle Schuiten moeten uniek zijn.

        Regel vangst
            geldig altijd
                De vangst van een Haven moet gesteld worden op de som van het maximum van de romplengte van alle liggers van de Haven.
        """;

    @SuppressWarnings("unchecked")
    private static List<java.lang.reflect.Field> sets() {
        List<java.lang.reflect.Field> result = new ArrayList<>();
        for (java.lang.reflect.Field f : RegelSpraakModel.class.getFields())
            if (!java.lang.reflect.Modifier.isStatic(f.getModifiers())) result.add(f);
        return result;
    }

    private static boolean empty(Object v) {
        return v instanceof java.util.Collection<?> c ? c.isEmpty() : ((java.util.Map<?, ?>) v).isEmpty();
    }

    /** Every element of every collection reaches the copy the scoped document starts from. Before: a copy field by
     *  field, from which a mutant could leave eight declaration collections out with every test green. */
    @SuppressWarnings("unchecked")
    @Test void the_copy_carries_every_set_of_the_model() throws IllegalAccessException {
        RegelSpraakModel m = new RegelSpraakModel();
        Object mark = new Object();
        for (java.lang.reflect.Field f : sets()) {
            if (f.get(m) instanceof List<?> l) ((List<Object>) l).add(mark);
            else ((java.util.Map<Object, Object>) f.get(m)).put(mark, mark);
        }
        RegelSpraakModel k = m.copy();
        for (java.lang.reflect.Field f : sets()) {
            Object v = f.get(k);
            assertTrue(v instanceof List<?> l ? l.contains(mark) : ((java.util.Map<?, ?>) v).containsKey(mark),
                    () -> "de kopie mist het veld '" + f.getName() + "'");
        }
        assertTrue(sets().size() >= 22, "het model telt minder verzamelingen dan gemeten (22)");
    }

    /** A scope that names every form of a model that fills every collection leaves the production document, byte for
     *  byte. The model carries a start point determination, which the scenario route refuses beside a scope, so the
     *  scope is built here from the model's own form names. */
    @Test void a_scope_that_names_everything_is_the_production_document() throws IllegalAccessException {
        RegelSpraakModel m = Resolver.resolve(RIJK);
        assertTrue(m.errors().isEmpty(), () -> "het rijke document hoort schoon te resolven: " + m.errors());
        for (java.lang.reflect.Field f : sets())
            assertFalse(empty(f.get(m)), () -> "het rijke document vult '" + f.getName() + "' niet");
        List<String> names = new ArrayList<>();
        m.resolvedRegels.forEach(r -> names.add(r.naam()));
        m.objectCreations.forEach(x -> names.add(x.naam()));
        m.allocations.forEach(x -> names.add(x.naam()));
        m.consistenties.forEach(x -> names.add(x.naam()));
        m.uniquenesses.forEach(x -> names.add(x.naam()));
        m.uniquenessUnions.forEach(x -> names.add(x.naam()));
        m.factCreations.forEach(x -> names.add(x.naam()));
        m.prioritizedDefaults.forEach(x -> names.add(x.naam()));
        ResolvedScenario.Scope everything = new ResolvedScenario.Scope(names);
        assertEquals(IrEmitter.emit(m), IrEmitter.emit(CaseEmitter.withScope(m, everything)));
    }

    // ---------------- the diagnostics a scoped document carries ----------------

    /** A declaration that draws a WARNING, and a marker WARNING in a rule, a rule the scope leaves out, the exception
     *  block of a defaults group it leaves out, and the member of a group it keeps one other member of. */
    private static final String WARNINGS = """
        Dagsoort de rustdag (mv: wk);

        Objecttype de Jutter (mv: Jutters) (bezield)
            het totaalgewicht Numeriek (getal met 2 decimalen);
            het restgewicht Numeriek (getal met 2 decimalen);
            het vangsttotaal Numeriek (getal met 2 decimalen);
            is bevoegd kenmerk (bijvoeglijk);
            is ervaren kenmerk (bijvoeglijk);
            is gemeld kenmerk (bijvoeglijk);

        Objecttype de Vondst (mv: Vondsten)
            het gewicht Numeriek (getal met 2 decimalen);

        Feittype vondstaanmelding
            de aanmelder Jutter
            de aangemelde vondst (mv: aangemelde vondsten) Vondst
        Eén aanmelder meldt meerdere aangemelde vondsten

        Regel totaalgewicht
            geldig altijd
                Het totaalgewicht van een Jutter moet berekend worden als de som van de schelpen van alle aangemelde vondsten van de Jutter.

        Regel bevoegdheid
            geldig altijd
                Een Jutter is bevoegd
                indien de som van de kiezels van alle aangemelde vondsten van de Jutter groter is dan 5.

        Regel ervaring
            geldig altijd
                Een Jutter is ervaren
                indien zijn vangsttotaal groter is dan 5.

        Uitzondering op ervaring: geen ervaring
            geldig altijd
                Een Jutter is niet ervaren
                indien de som van de keien van alle aangemelde vondsten van de Jutter groter is dan 9.

        Regelgroep meldingen
        Regel melding
            geldig altijd
                Een Jutter is gemeld
                indien zijn vangsttotaal groter is dan 1.
        Regel restgewicht
            geldig altijd
                Het restgewicht van een Jutter moet berekend worden als de som van de stenen van alle aangemelde vondsten van de Jutter.
        """;

    /** Before: the scoped document kept every diagnostic of the whole model, also the WARNINGs of the rules it left
     *  out (open point of the scenario card, measured again by the lens: a WARNING about a left-out rule in a
     *  document without it). */
    @Test void a_scoped_document_carries_the_diagnostics_of_its_forms_and_declarations() {
        ScenarioBundle b = ScenarioResolver.resolve(WARNINGS + "Scenario s\n"
                + "toetst de regels bevoegdheid en melding\nGegeven is de volgende situatie:\neen Jutter J\n"
                + "Met als verwacht resultaat:\n- de gegevens zijn consistent\n");
        assertTrue(b.isOk(), () -> b.model().errors().toString());
        assertEquals(5, b.model().diagnostics.size(), () -> "het hele model: " + b.model().diagnostics);
        List<String> scoped = CaseEmitter.withScope(b.model(), only(b).scope()).diagnostics.stream()
                .map(d -> d.line() + " " + d.message().substring(0, d.message().indexOf(' '))).toList();
        assertEquals(List.of("1 dagsoort-eenheid", "26 'kiezels'"), scoped,
                "de declaratie en de gedragen regel houden hun WARNING; de weggelaten regel, het uitzonderingsblok "
                        + "van de weggelaten defaultsgroep en het weggelaten groepslid niet");
    }

    // ---------------- the CLI ----------------

    private record Output(int code, String tekst) {}

    private static Output cli(Path ir, Path map, Path... sources) throws IOException {
        List<String> rest = new ArrayList<>();
        rest.add(ir.toString());
        for (Path p : sources) rest.add(p.toString());
        PrintStream oud = System.out;
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        System.setOut(new PrintStream(buf, true, java.nio.charset.StandardCharsets.UTF_8));
        try {
            return new Output(Resolver.casesMode(rest, map), buf.toString(java.nio.charset.StandardCharsets.UTF_8));
        } finally {
            System.setOut(oud);
        }
    }

    private static List<String> files(Path map) throws IOException {
        if (!Files.isDirectory(map)) return List.of();
        try (Stream<Path> s = Files.list(map)) { return s.map(p -> p.getFileName().toString()).sorted().toList(); }
    }

    @Test void the_cases_mode_writes_the_ir_document_and_the_case_files() throws IOException {
        Path d = Files.createTempDirectory("scenario-cli");
        Path source = Files.writeString(d.resolve("bron.rgs"), MODEL);
        Path scen = Files.writeString(d.resolve("scenario.rgs"), SCENARIO);
        Output u = cli(d.resolve("ir.json"), d.resolve("cases"), source, scen);
        assertEquals(0, u.code(), u.tekst());
        assertEquals(List.of("case1-een-wrak.json"), files(d.resolve("cases")));
        assertEquals(CASE_FILE, Files.readString(d.resolve("cases/case1-een-wrak.json")));
        assertEquals(IrEmitter.emit(Resolver.resolve(MODEL)), Files.readString(d.resolve("ir.json")));
    }

    @Test void the_cases_mode_writes_nothing_at_an_error_and_names_file_and_line() throws IOException {
        Path d = Files.createTempDirectory("scenario-cli");
        Path source = Files.writeString(d.resolve("bron.rgs"), MODEL);
        Path scen = Files.writeString(d.resolve("scenario.rgs"), SCENARIO.replace("met peildatum 15-03-2024",
                "met peildatum 15-03-2024 12:00:00.000"));
        Output u = cli(d.resolve("ir.json"), d.resolve("cases"), source, scen);
        assertEquals(1, u.code());
        // one line number, the one in the file: before, the line in the chain of files came first ("(line 64)")
        assertTrue(u.tekst().contains("  ERROR (" + scen + ":2): scenario: de peildatum '15-03-2024 12:00:00.000' "
                + "draagt een tijd"), u.tekst());
        assertFalse(u.tekst().contains("(line "), u.tekst());
        assertFalse(Files.exists(d.resolve("ir.json")), "een weigering schrijft geen IR-document");
        assertEquals(List.of(), files(d.resolve("cases")), "een weigering schrijft geen casebestand");
    }

    @Test void the_cases_mode_refuses_a_directory_with_a_case_file_that_it_does_not_write() throws IOException {
        // before: the old file stayed beside the new ones, and the run wrote them and exited 0
        Path d = Files.createTempDirectory("scenario-cli");
        Path source = Files.writeString(d.resolve("bron.rgs"), MODEL);
        Path scen = Files.writeString(d.resolve("scenario.rgs"), SCENARIO);
        Files.createDirectories(d.resolve("cases"));
        Files.writeString(d.resolve("cases/case9-oud.json"), "{}\n");
        Output u = cli(d.resolve("ir.json"), d.resolve("cases"), source, scen);
        assertEquals(1, u.code(), u.tekst());
        assertTrue(u.tekst().contains("draagt al casebestanden die dit document niet schrijft (case9-oud.json)"),
                u.tekst());
        assertEquals(List.of("case9-oud.json"), files(d.resolve("cases")), "de weigering schrijft geen casebestand");
        assertFalse(Files.exists(d.resolve("ir.json")), "de weigering schrijft geen IR-document");
        // a case file the run writes itself is overwritten
        Files.delete(d.resolve("cases/case9-oud.json"));
        Files.writeString(d.resolve("cases/case1-een-wrak.json"), "{}\n");
        assertEquals(0, cli(d.resolve("ir.json"), d.resolve("cases"), source, scen).code());
        assertEquals(CASE_FILE, Files.readString(d.resolve("cases/case1-een-wrak.json")));
    }

    @Test void a_document_without_scenario_has_nothing_to_lower() throws IOException {
        Path d = Files.createTempDirectory("scenario-cli");
        Path source = Files.writeString(d.resolve("bron.rgs"), MODEL);
        Output u = cli(d.resolve("ir.json"), d.resolve("cases"), source);
        assertEquals(1, u.code());
        assertTrue(u.tekst().contains("het document draagt geen scenario, dus er valt geen casebestand te schrijven"),
                u.tekst());
    }
}
