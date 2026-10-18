package dev.uchar.regelspraak;

import dev.uchar.regelspraak.emit.CaseEmitter;
import dev.uchar.regelspraak.ir.ResolvedScenario;
import dev.uchar.regelspraak.ir.ResolvedScenario.ParameterWaarde;
import dev.uchar.regelspraak.parser.RegelSpraak;
import dev.uchar.regelspraak.parser.RegelSpraakLexer;
import dev.uchar.regelspraak.resolver.ScenarioResolver.ScenarioBundel;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.ErrorNode;
import org.antlr.v4.runtime.tree.ParseTree;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import static dev.uchar.regelspraak.ScenarioFixture.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * The parameter set of the scenariotoetsen extension (grammar/DIVERGENCES.md, "Scenariotoetsen"): a block
 * {@code Parameterset <naam>:} of parameter rows, chosen in a scenario header with {@code met parameterset <naam>}.
 * The rows of a parameter without granularity are its jaargangen, and the scenario's peildatum picks the one whose
 * period holds it; the rows of a parameter with a timeline are its segments, and the case file carries them whole.
 * The case file below is written by hand from the case schema; every refusal is asserted on its line, in the sentence
 * the resolver writes. The model is {@link ScenarioFixture}'s: {@code vondstdrempel} has no granularity and the rule
 * {@code bonus bij drempel} reads it, {@code penningbedrag} has a year timeline and no rule reads it.
 */
class ScenarioParameterSetTest {

    private static final String JAREN = """
        Parameterset jaren:
        de parameter de vondstdrempel is 12 van 01-01-2025 tot 01-01-2026
        de parameter het penningbedrag is 250 van 01-01-2025 tot 01-01-2026
        de parameter de vondstdrempel is 14 van 01-01-2026 tot 01-01-2027
        de parameter het penningbedrag is 275 van 01-01-2026 tot 01-01-2027
        """;

    /** A scenario named {@code naam} on one Jutter, with header lines {@code head} and expectation {@code verwacht}. */
    private static String scenario(String naam, String head, String verwacht) {
        return "Scenario " + naam + "\n" + head + "Gegeven is de volgende situatie:\neen Jutter J\n"
                + "- het vondstgewicht is 12,00 kg\nMet als verwacht resultaat:\n" + verwacht;
    }

    private static ResolvedScenario only(ScenarioBundel b) {
        assertTrue(b.isOk(), () -> b.model().errors().toString());
        assertEquals(1, b.scenarios().size());
        return b.scenarios().get(0);
    }

    private static List<String> parameters(ResolvedScenario s) {
        List<String> result = new ArrayList<>();
        for (ParameterWaarde p : s.situatie().parameters())
            result.add(p.parameter() + "=" + (p.tijdlijn() != null ? "tijdlijn" + p.tijdlijn().size() : p.waarde().getal()));
        return result;
    }

    private static void fout(String scenarioText, String merker, String fragment) {
        ScenarioBundel b = resolve(scenarioText);
        errorOnLine(b, MODEL + scenarioText, merker, fragment);
        assertFalse(b.isOk());
    }

    // ---------------- the grammar ----------------

    private static int syntaxErrors(String bron, Function<RegelSpraak, ParseTree> start) {
        List<String> meldingen = new ArrayList<>();
        BaseErrorListener l = new BaseErrorListener() {
            @Override public void syntaxError(Recognizer<?, ?> r, Object sym, int line, int col, String msg,
                                              RecognitionException e) {
                meldingen.add(line + ":" + col + " " + msg);
            }
        };
        RegelSpraakLexer lexer = new RegelSpraakLexer(CharStreams.fromString(bron));
        lexer.removeErrorListeners(); lexer.addErrorListener(l);
        RegelSpraak parser = new RegelSpraak(new CommonTokenStream(lexer));
        parser.removeErrorListeners(); parser.addErrorListener(l);
        ParseTree tree = start.apply(parser);
        boolean everything = parser.getInputStream().LA(1) == Token.EOF;
        return meldingen.size() + errorNodes(tree) + (everything ? 0 : 1);
    }

    private static int errorNodes(ParseTree t) {
        if (t instanceof ErrorNode) return 1;
        int n = 0;
        for (int i = 0; i < t.getChildCount(); i++) n += errorNodes(t.getChild(i));
        return n;
    }

    private static List<String> tokens(String bron) {
        RegelSpraakLexer lexer = new RegelSpraakLexer(CharStreams.fromString(bron));
        List<String> result = new ArrayList<>();
        for (Token t : lexer.getAllTokens())
            if (t.getChannel() == Token.DEFAULT_CHANNEL) result.add(RegelSpraakLexer.VOCABULARY.getSymbolicName(t.getType()));
        return result;
    }

    @Test void the_set_and_the_choice_parse_under_testDocument() {
        String bron = JAREN + scenario("oudejaar", "met peildatum 31-12-2025\nmet parameterset jaren\n",
                "- de kustwachtbonus van J is 12\n");
        assertEquals(0, syntaxErrors(bron, RegelSpraak::testDocument));
    }

    @Test void a_parameter_set_block_is_no_production_document() {
        assertNotEquals(0, syntaxErrors(JAREN, RegelSpraak::regelSpraakDocument),
                "een parametersetblok hoort onder regelSpraakDocument te falen: de productieroute ziet de extensie niet");
        assertEquals(0, syntaxErrors(JAREN, RegelSpraak::testDocument));
    }

    @Test void the_two_tokens_and_their_boundaries() {
        assertEquals(List.of("PARAMETERSET", "LOWER_ID", "COLON"), tokens("Parameterset tarieven:"));
        assertEquals(List.of("MET_PARAMETERSET", "LOWER_ID"), tokens("met parameterset tarieven"));
        assertEquals(List.of("UPPER_ID"), tokens("Parametersets"));
        assertEquals(List.of("PARAMETER"), tokens("Parameter"));
        assertEquals(List.of("MET", "LOWER_ID"), tokens("met parameters"));
    }

    /** Why the header is a token: an expectation that ends in a characteristic name ends in a run that takes an
     *  UPPER_ID, and a number written without a unit takes one as its unit, so a header word that lexed as an UPPER_ID
     *  would be read into either. */
    @Test void a_set_header_after_a_characteristic_name_or_a_number_without_unit_opens_a_new_block() {
        String bron = "Situatie zwaar:\neen Jutter J\n- het vondstgewicht is 12,00 kg\n"
                + "Parameterset reeks noord:\nde parameter de vondstdrempel is 3\n"
                + "Parameterset reeks zuid:\nde parameter de vondstdrempel is 4\n"
                + "Scenario strandrecht\nmet peildatum 01-06-2025\nmet parameterset reeks noord\n"
                + "gebaseerd op de situatie zwaar\nMet als verwacht resultaat:\n- J heeft strandrecht\n"
                + "Parameterset reeks oost:\nde parameter de vondstdrempel is 5\n";
        assertEquals(0, syntaxErrors(bron, RegelSpraak::testDocument));
        assertEquals(List.of("vondstdrempel=3"), parameters(only(resolve(bron))));
    }

    @Test void the_choice_ends_the_scenario_name_and_the_name_of_a_single_rule() {
        ResolvedScenario s = only(resolve(JAREN + scenario("oudejaar met een staart",
                "toetst de regel bonus bij drempel\nmet peildatum 31-12-2025\nmet parameterset jaren\n",
                "- de kustwachtbonus van J is 12\n")));
        assertEquals("oudejaar met een staart", s.naam());
        assertEquals(List.of("bonus bij drempel"), s.scope().vormen());
        ResolvedScenario withoutAsOfDate = only(resolve("Parameterset vast:\nde parameter de vondstdrempel is 3\n"
                + scenario("vast", "toetst de regel bonus bij drempel\nmet parameterset vast\n",
                "- de kustwachtbonus van J is 3\n")));
        assertEquals(List.of("bonus bij drempel"), withoutAsOfDate.scope().vormen());
    }

    // ---------------- the lowering ----------------

    private static final String CASE_FILE = """
        {
          "format": "uchar-case-v0",
          "asOfDate": "2025-12-31",
          "input": {
            "instances": [
              {
                "label": "J",
                "objectType": "Jutter",
                "attributes": {
                  "vondstgewicht": {
                    "number": "12.00"
                  }
                },
                "roles": {},
                "characteristics": []
              }
            ],
            "parameters": {
              "vondstdrempel": {
                "number": "12"
              },
              "penningbedrag": {
                "timeline": [
                  {
                    "from": "2025-01-01",
                    "to": "2026-01-01",
                    "value": {
                      "number": "250"
                    }
                  },
                  {
                    "from": "2026-01-01",
                    "to": "2027-01-01",
                    "value": {
                      "number": "275"
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
                "number": "12"
              }
            }
          ]
        }
        """;

    @Test void the_as_of_date_chooses_the_vintage_and_a_timeline_comes_along_whole() {
        ResolvedScenario s = only(resolve(JAREN + scenario("oudejaar", "met peildatum 31-12-2025\nmet parameterset jaren\n",
                "- de kustwachtbonus van J is 12\n")));
        assertEquals(CASE_FILE, CaseEmitter.emit(s));
    }

    @Test void the_period_of_a_vintage_counts_to_and_not_up_to_and_including() {
        ScenarioBundel b = resolve(JAREN
                + scenario("oudejaar", "met peildatum 31-12-2025\nmet parameterset jaren\n", "- de kustwachtbonus van J is 12\n")
                + scenario("nieuwjaar", "met peildatum 01-01-2026\nmet parameterset jaren\n", "- de kustwachtbonus van J is 14\n"));
        assertTrue(b.isOk(), () -> b.model().errors().toString());
        assertEquals(List.of("vondstdrempel=12", "penningbedrag=tijdlijn2"), parameters(b.scenarios().get(0)));
        assertEquals(List.of("vondstdrempel=14", "penningbedrag=tijdlijn2"), parameters(b.scenarios().get(1)));
    }

    @Test void vintage_are_in_every_order() {
        String set = "Parameterset omgekeerd:\n"
                + "de parameter de vondstdrempel is 14 van 01-01-2026 tot 01-01-2027\n"
                + "de parameter de vondstdrempel is 12 van 01-01-2025 tot 01-01-2026\n";
        ResolvedScenario s = only(resolve(set + scenario("zomer", "met peildatum 01-07-2025\nmet parameterset omgekeerd\n",
                "- de kustwachtbonus van J is 12\n")));
        assertEquals(List.of("vondstdrempel=12"), parameters(s));
    }

    @Test void a_rule_without_a_period_applies_on_every_day_and_requires_no_as_of_date() {
        ResolvedScenario s = only(resolve("Parameterset vast:\nde parameter de vondstdrempel is 3\n"
                + scenario("vast", "toetst de regel bonus bij drempel\nmet parameterset vast\n",
                "- de kustwachtbonus van J is 3\n")));
        assertNull(s.peildatum());
        assertEquals(List.of("vondstdrempel=3"), parameters(s));
    }

    @Test void the_values_of_the_set_stand_for_that_of_the_situation() {
        String set = "Parameterset bedragen:\nde parameter het penningbedrag is 250 van 01-01-2025 tot 01-01-2026\n";
        String s = set + "Scenario samen\nmet peildatum 01-06-2025\nmet parameterset bedragen\n"
                + "Gegeven is de volgende situatie:\neen Jutter J\nde parameter de vondstdrempel is 3\n"
                + "Met als verwacht resultaat:\n- de kustwachtbonus van J is 3\n";
        assertEquals(List.of("penningbedrag=tijdlijn1", "vondstdrempel=3"), parameters(only(resolve(s))));
    }

    @Test void a_value_in_a_set_is_typed_as_in_the_situation() {
        String set = "Parameterset eenheid:\nde parameter de vondstdrempel is 6/8 van 01-01-2025 tot 01-01-2026\n";
        ScenarioBundel b = resolve(set + scenario("breuk", "met peildatum 01-06-2025\nmet parameterset eenheid\n",
                "- de kustwachtbonus van J is 1\n"));
        // the parameter is a whole number, so a fraction is refused in the typing's own sentence, as in a situation
        errorOnLine(b, MODEL + set, "6/8", "de parameter 'vondstdrempel': het toe te kennen resultaat heeft meer "
                + "decimalen (2) dan het attribuut toelaat (0)");
    }

    // ---------------- the refusals ----------------

    @Test void overlapping_vintage() {
        String set = "Parameterset overlap:\n"
                + "de parameter de vondstdrempel is 12 van 01-01-2025 tot 01-01-2026\n"
                + "de parameter de vondstdrempel is 14 van 31-12-2025 tot 01-01-2027\n";
        String tekst = set + scenario("nieuwjaar", "met peildatum 01-01-2026\nmet parameterset overlap\n",
                "- de kustwachtbonus van J is 14\n");
        fout(tekst, "van 31-12-2025 tot 01-01-2027", "de parameterset 'overlap' geeft de parameter 'vondstdrempel' "
                + "twee jaargangen die elkaar overlappen (van 2025-01-01 tot 2026-01-01 en van 2025-12-31 tot 2027-01-01); "
                + "de peildatum van een scenario kiest precies één jaargang");
        // the refused row is not a gap a peildatum could fall in: one ERROR, not two
        assertEquals(1, resolve(tekst).model().errors().size(), () -> resolve(tekst).model().errors().toString());
    }

    @Test void a_rule_without_a_period_beside_a_vintage_overlaps_it() {
        String set = "Parameterset los:\n"
                + "de parameter de vondstdrempel is 12 van 01-01-2025 tot 01-01-2026\n"
                + "de parameter de vondstdrempel is 13\n";
        fout(set, "vondstdrempel is 13", "de parameterset 'los' geeft de parameter 'vondstdrempel' twee jaargangen die "
                + "elkaar overlappen (van 2025-01-01 tot 2026-01-01 en zonder periode)");
    }

    @Test void no_vintage_on_the_as_of_date() {
        String tekst = JAREN + scenario("te laat", "met peildatum 01-06-2027\nmet parameterset jaren\n",
                "- de kustwachtbonus van J is 14\n");
        fout(tekst, "met parameterset jaren", "de parameterset 'jaren' heeft voor de parameter 'vondstdrempel' geen "
                + "jaargang die de peildatum 2027-06-01 van het scenario 'te laat' bevat");
        // a timeline is carried whole and the executors hold its coverage: no gap is reported for it here
        assertTrue(resolve(tekst).model().errors().stream().noneMatch(d -> d.message().contains("'penningbedrag'")));
    }

    @Test void conflict_with_the_situation() {
        String tekst = JAREN + "Situatie met drempel:\neen Jutter J\nde parameter de vondstdrempel is 13\n"
                + "Scenario dubbel\nmet peildatum 01-06-2025\nmet parameterset jaren\ngebaseerd op de situatie met drempel\n"
                + "Met als verwacht resultaat:\n- de kustwachtbonus van J is 13\n";
        fout(tekst, "met parameterset jaren", "de parameter 'vondstdrempel' krijgt een waarde uit de parameterset 'jaren' "
                + "en uit de situatie van het scenario 'dubbel'; een parameter krijgt zijn waarde uit één van beide");
    }

    @Test void scenario_without_as_of_date() {
        fout(JAREN + scenario("tijdloos", "toetst de regel bonus bij drempel\nmet parameterset jaren\n",
                        "- de kustwachtbonus van J is 12\n"),
                "met parameterset jaren", "de parameter 'vondstdrempel' heeft in de parameterset 'jaren' jaargangen, maar "
                        + "het scenario 'tijdloos' heeft geen peildatum die er één kiest (voeg 'met peildatum <datum>' toe)");
    }

    @Test void unknown_parameter_set() {
        String tekst = scenario("wees", "met peildatum 01-06-2025\nmet parameterset nergens\n",
                "- de kustwachtbonus van J is 12\n");
        fout(tekst, "met parameterset nergens", "de parameterset 'nergens' is nergens in het document gedeclareerd");
        // what the missing set would have given cannot be said, so the parameter gate stays quiet
        assertEquals(1, resolve(tekst).model().errors().size(), () -> resolve(tekst).model().errors().toString());
    }

    @Test void duplicate_parameter_set() {
        // the names compare as the scenario layer compares names, so 'Jaren' is 'jaren'
        fout(JAREN + "Parameterset Jaren:\nde parameter de vondstdrempel is 3\n", "Parameterset Jaren",
                "de parameterset 'Jaren' is meer dan eens gedeclareerd");
    }

    @Test void the_parameter_gate_names_the_set() {
        String set = "Parameterset bedragen:\nde parameter het penningbedrag is 250 van 01-01-2025 tot 01-01-2026\n";
        fout(set + scenario("zonder drempel", "met peildatum 01-06-2025\nmet parameterset bedragen\n",
                        "- de kustwachtbonus van J is 3\n"),
                "Scenario zonder drempel", "de regel 'bonus bij drempel' leest de parameter 'vondstdrempel', die in de "
                        + "situatie van het scenario 'zonder drempel' en in de parameterset 'bedragen' geen waarde heeft");
    }

    @Test void a_timeline_in_a_set_follows_the_rules_of_the_situation() {
        fout("Parameterset raster:\nde parameter het penningbedrag is 250 van 01-07-2025 tot 01-01-2026\n",
                "van 01-07-2025", "de periodegrens 2025-07-01 van de parameter 'penningbedrag' ligt niet op het jaarraster "
                        + "van de declaratie");
        fout("Parameterset volgorde:\nde parameter het penningbedrag is 275 van 01-01-2026 tot 01-01-2027\n"
                        + "de parameter het penningbedrag is 250 van 01-01-2025 tot 01-01-2026\n",
                "is 250 van", "de periode van 2025-01-01 tot 2026-01-01 begint vóór het einde van de vorige (2027-01-01)");
        fout("Parameterset gemengd:\nde parameter het penningbedrag is 250\n"
                        + "de parameter het penningbedrag is 275 van 01-01-2026 tot 01-01-2027\n",
                "is 275 van", "de parameter 'penningbedrag' krijgt in dit blok zowel een vaste waarde als perioden");
    }

    /** A period the parse left without its second date has its syntax error and nothing else — in a set, and in the
     *  situation row of a timeline parameter, which reads its period through the same method. */
    @Test void an_incomplete_period_is_only_a_syntax_error() {
        for (String tekst : List.of("Parameterset p:\nde parameter de vondstdrempel is 3 van 01-01-2025\n",
                "Situatie s:\nde parameter het penningbedrag is 7 van 01-01-2024\n")) {
            ScenarioBundel b = resolve(tekst);
            assertFalse(b.isOk());
            assertTrue(b.model().errors().stream().allMatch(d -> d.message().startsWith("syntaxfout: ")),
                    () -> b.model().errors().toString());
        }
    }

    @Test void an_unchosen_set_is_nevertheless_checked() {
        fout("Parameterset ongebruikt:\nde parameter de getijdendrempel is 4\n", "getijdendrempel",
                "de parameter 'de getijdendrempel' is niet gedeclareerd");
    }
}
