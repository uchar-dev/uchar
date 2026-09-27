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
class ScenarioParametersetTest {

    private static final String JAREN = """
        Parameterset jaren:
        de parameter de vondstdrempel is 12 van 01-01-2025 tot 01-01-2026
        de parameter het penningbedrag is 250 van 01-01-2025 tot 01-01-2026
        de parameter de vondstdrempel is 14 van 01-01-2026 tot 01-01-2027
        de parameter het penningbedrag is 275 van 01-01-2026 tot 01-01-2027
        """;

    /** A scenario named {@code naam} on one Jutter, with header lines {@code kop} and expectation {@code verwacht}. */
    private static String scenario(String naam, String kop, String verwacht) {
        return "Scenario " + naam + "\n" + kop + "Gegeven is de volgende situatie:\neen Jutter J\n"
                + "- het vondstgewicht is 12,00 kg\nMet als verwacht resultaat:\n" + verwacht;
    }

    private static ResolvedScenario enig(ScenarioBundel b) {
        assertTrue(b.isOk(), () -> b.model().errors().toString());
        assertEquals(1, b.scenarios().size());
        return b.scenarios().get(0);
    }

    private static List<String> parameters(ResolvedScenario s) {
        List<String> uit = new ArrayList<>();
        for (ParameterWaarde p : s.situatie().parameters())
            uit.add(p.parameter() + "=" + (p.tijdlijn() != null ? "tijdlijn" + p.tijdlijn().size() : p.waarde().getal()));
        return uit;
    }

    private static void fout(String scenarioTekst, String merker, String fragment) {
        ScenarioBundel b = resolve(scenarioTekst);
        foutOpRegel(b, MODEL + scenarioTekst, merker, fragment);
        assertFalse(b.isOk());
    }

    // ---------------- the grammar ----------------

    private static int syntaxfouten(String bron, Function<RegelSpraak, ParseTree> start) {
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
        ParseTree boom = start.apply(parser);
        boolean alles = parser.getInputStream().LA(1) == Token.EOF;
        return meldingen.size() + foutknopen(boom) + (alles ? 0 : 1);
    }

    private static int foutknopen(ParseTree t) {
        if (t instanceof ErrorNode) return 1;
        int n = 0;
        for (int i = 0; i < t.getChildCount(); i++) n += foutknopen(t.getChild(i));
        return n;
    }

    private static List<String> tokens(String bron) {
        RegelSpraakLexer lexer = new RegelSpraakLexer(CharStreams.fromString(bron));
        List<String> uit = new ArrayList<>();
        for (Token t : lexer.getAllTokens())
            if (t.getChannel() == Token.DEFAULT_CHANNEL) uit.add(RegelSpraakLexer.VOCABULARY.getSymbolicName(t.getType()));
        return uit;
    }

    @Test void de_set_en_de_keuze_parseren_onder_testDocument() {
        String bron = JAREN + scenario("oudejaar", "met peildatum 31-12-2025\nmet parameterset jaren\n",
                "- de kustwachtbonus van J is 12\n");
        assertEquals(0, syntaxfouten(bron, RegelSpraak::testDocument));
    }

    @Test void een_parametersetblok_is_geen_productiedocument() {
        assertNotEquals(0, syntaxfouten(JAREN, RegelSpraak::regelSpraakDocument),
                "een parametersetblok hoort onder regelSpraakDocument te falen: de productieroute ziet de extensie niet");
        assertEquals(0, syntaxfouten(JAREN, RegelSpraak::testDocument));
    }

    @Test void de_twee_tokens_en_hun_grenzen() {
        assertEquals(List.of("PARAMETERSET", "LOWER_ID", "COLON"), tokens("Parameterset tarieven:"));
        assertEquals(List.of("MET_PARAMETERSET", "LOWER_ID"), tokens("met parameterset tarieven"));
        assertEquals(List.of("UPPER_ID"), tokens("Parametersets"));
        assertEquals(List.of("PARAMETER"), tokens("Parameter"));
        assertEquals(List.of("MET", "LOWER_ID"), tokens("met parameters"));
    }

    /** Why the header is a token: an expectation that ends in a characteristic name ends in a run that takes an
     *  UPPER_ID, and a number written without a unit takes one as its unit, so a header word that lexed as an UPPER_ID
     *  would be read into either. */
    @Test void een_setkop_na_een_kenmerknaam_of_een_getal_zonder_eenheid_opent_een_nieuw_blok() {
        String bron = "Situatie zwaar:\neen Jutter J\n- het vondstgewicht is 12,00 kg\n"
                + "Parameterset reeks noord:\nde parameter de vondstdrempel is 3\n"
                + "Parameterset reeks zuid:\nde parameter de vondstdrempel is 4\n"
                + "Scenario strandrecht\nmet peildatum 01-06-2025\nmet parameterset reeks noord\n"
                + "gebaseerd op de situatie zwaar\nMet als verwacht resultaat:\n- J heeft strandrecht\n"
                + "Parameterset reeks oost:\nde parameter de vondstdrempel is 5\n";
        assertEquals(0, syntaxfouten(bron, RegelSpraak::testDocument));
        assertEquals(List.of("vondstdrempel=3"), parameters(enig(resolve(bron))));
    }

    @Test void de_keuze_beeindigt_de_scenarionaam_en_de_naam_van_een_enkele_regel() {
        ResolvedScenario s = enig(resolve(JAREN + scenario("oudejaar met een staart",
                "toetst de regel bonus bij drempel\nmet peildatum 31-12-2025\nmet parameterset jaren\n",
                "- de kustwachtbonus van J is 12\n")));
        assertEquals("oudejaar met een staart", s.naam());
        assertEquals(List.of("bonus bij drempel"), s.scope().vormen());
        ResolvedScenario zonderPeildatum = enig(resolve("Parameterset vast:\nde parameter de vondstdrempel is 3\n"
                + scenario("vast", "toetst de regel bonus bij drempel\nmet parameterset vast\n",
                "- de kustwachtbonus van J is 3\n")));
        assertEquals(List.of("bonus bij drempel"), zonderPeildatum.scope().vormen());
    }

    // ---------------- the lowering ----------------

    private static final String CASEBESTAND = """
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

    @Test void de_peildatum_kiest_de_jaargang_en_een_tijdlijn_komt_heel_mee() {
        ResolvedScenario s = enig(resolve(JAREN + scenario("oudejaar", "met peildatum 31-12-2025\nmet parameterset jaren\n",
                "- de kustwachtbonus van J is 12\n")));
        assertEquals(CASEBESTAND, CaseEmitter.emit(s));
    }

    @Test void de_periode_van_een_jaargang_telt_tot_en_niet_tot_en_met() {
        ScenarioBundel b = resolve(JAREN
                + scenario("oudejaar", "met peildatum 31-12-2025\nmet parameterset jaren\n", "- de kustwachtbonus van J is 12\n")
                + scenario("nieuwjaar", "met peildatum 01-01-2026\nmet parameterset jaren\n", "- de kustwachtbonus van J is 14\n"));
        assertTrue(b.isOk(), () -> b.model().errors().toString());
        assertEquals(List.of("vondstdrempel=12", "penningbedrag=tijdlijn2"), parameters(b.scenarios().get(0)));
        assertEquals(List.of("vondstdrempel=14", "penningbedrag=tijdlijn2"), parameters(b.scenarios().get(1)));
    }

    @Test void jaargangen_staan_in_elke_volgorde() {
        String set = "Parameterset omgekeerd:\n"
                + "de parameter de vondstdrempel is 14 van 01-01-2026 tot 01-01-2027\n"
                + "de parameter de vondstdrempel is 12 van 01-01-2025 tot 01-01-2026\n";
        ResolvedScenario s = enig(resolve(set + scenario("zomer", "met peildatum 01-07-2025\nmet parameterset omgekeerd\n",
                "- de kustwachtbonus van J is 12\n")));
        assertEquals(List.of("vondstdrempel=12"), parameters(s));
    }

    @Test void een_regel_zonder_periode_geldt_op_elke_dag_en_vraagt_geen_peildatum() {
        ResolvedScenario s = enig(resolve("Parameterset vast:\nde parameter de vondstdrempel is 3\n"
                + scenario("vast", "toetst de regel bonus bij drempel\nmet parameterset vast\n",
                "- de kustwachtbonus van J is 3\n")));
        assertNull(s.peildatum());
        assertEquals(List.of("vondstdrempel=3"), parameters(s));
    }

    @Test void de_waarden_van_de_set_staan_voor_die_van_de_situatie() {
        String set = "Parameterset bedragen:\nde parameter het penningbedrag is 250 van 01-01-2025 tot 01-01-2026\n";
        String s = set + "Scenario samen\nmet peildatum 01-06-2025\nmet parameterset bedragen\n"
                + "Gegeven is de volgende situatie:\neen Jutter J\nde parameter de vondstdrempel is 3\n"
                + "Met als verwacht resultaat:\n- de kustwachtbonus van J is 3\n";
        assertEquals(List.of("penningbedrag=tijdlijn1", "vondstdrempel=3"), parameters(enig(resolve(s))));
    }

    @Test void een_waarde_in_een_set_wordt_getypeerd_als_in_de_situatie() {
        String set = "Parameterset eenheid:\nde parameter de vondstdrempel is 6/8 van 01-01-2025 tot 01-01-2026\n";
        ScenarioBundel b = resolve(set + scenario("breuk", "met peildatum 01-06-2025\nmet parameterset eenheid\n",
                "- de kustwachtbonus van J is 1\n"));
        // the parameter is a whole number, so a fraction is refused in the typing's own sentence, as in a situation
        foutOpRegel(b, MODEL + set, "6/8", "de parameter 'vondstdrempel': het toe te kennen resultaat heeft meer "
                + "decimalen (2) dan het attribuut toelaat (0)");
    }

    // ---------------- the refusals ----------------

    @Test void overlappende_jaargangen() {
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

    @Test void een_regel_zonder_periode_naast_een_jaargang_overlapt_hem() {
        String set = "Parameterset los:\n"
                + "de parameter de vondstdrempel is 12 van 01-01-2025 tot 01-01-2026\n"
                + "de parameter de vondstdrempel is 13\n";
        fout(set, "vondstdrempel is 13", "de parameterset 'los' geeft de parameter 'vondstdrempel' twee jaargangen die "
                + "elkaar overlappen (van 2025-01-01 tot 2026-01-01 en zonder periode)");
    }

    @Test void geen_jaargang_op_de_peildatum() {
        String tekst = JAREN + scenario("te laat", "met peildatum 01-06-2027\nmet parameterset jaren\n",
                "- de kustwachtbonus van J is 14\n");
        fout(tekst, "met parameterset jaren", "de parameterset 'jaren' heeft voor de parameter 'vondstdrempel' geen "
                + "jaargang die de peildatum 2027-06-01 van het scenario 'te laat' bevat");
        // a timeline is carried whole and the executors hold its coverage: no gap is reported for it here
        assertTrue(resolve(tekst).model().errors().stream().noneMatch(d -> d.message().contains("'penningbedrag'")));
    }

    @Test void conflict_met_de_situatie() {
        String tekst = JAREN + "Situatie met drempel:\neen Jutter J\nde parameter de vondstdrempel is 13\n"
                + "Scenario dubbel\nmet peildatum 01-06-2025\nmet parameterset jaren\ngebaseerd op de situatie met drempel\n"
                + "Met als verwacht resultaat:\n- de kustwachtbonus van J is 13\n";
        fout(tekst, "met parameterset jaren", "de parameter 'vondstdrempel' krijgt een waarde uit de parameterset 'jaren' "
                + "en uit de situatie van het scenario 'dubbel'; een parameter krijgt zijn waarde uit één van beide");
    }

    @Test void scenario_zonder_peildatum() {
        fout(JAREN + scenario("tijdloos", "toetst de regel bonus bij drempel\nmet parameterset jaren\n",
                        "- de kustwachtbonus van J is 12\n"),
                "met parameterset jaren", "de parameter 'vondstdrempel' heeft in de parameterset 'jaren' jaargangen, maar "
                        + "het scenario 'tijdloos' heeft geen peildatum die er één kiest (voeg 'met peildatum <datum>' toe)");
    }

    @Test void onbekende_parameterset() {
        String tekst = scenario("wees", "met peildatum 01-06-2025\nmet parameterset nergens\n",
                "- de kustwachtbonus van J is 12\n");
        fout(tekst, "met parameterset nergens", "de parameterset 'nergens' is nergens in het document gedeclareerd");
        // what the missing set would have given cannot be said, so the parameter gate stays quiet
        assertEquals(1, resolve(tekst).model().errors().size(), () -> resolve(tekst).model().errors().toString());
    }

    @Test void dubbele_parameterset() {
        // the names compare as the scenario layer compares names, so 'Jaren' is 'jaren'
        fout(JAREN + "Parameterset Jaren:\nde parameter de vondstdrempel is 3\n", "Parameterset Jaren",
                "de parameterset 'Jaren' is meer dan eens gedeclareerd");
    }

    @Test void de_parameterpoort_noemt_de_set() {
        String set = "Parameterset bedragen:\nde parameter het penningbedrag is 250 van 01-01-2025 tot 01-01-2026\n";
        fout(set + scenario("zonder drempel", "met peildatum 01-06-2025\nmet parameterset bedragen\n",
                        "- de kustwachtbonus van J is 3\n"),
                "Scenario zonder drempel", "de regel 'bonus bij drempel' leest de parameter 'vondstdrempel', die in de "
                        + "situatie van het scenario 'zonder drempel' en in de parameterset 'bedragen' geen waarde heeft");
    }

    @Test void een_tijdlijn_in_een_set_volgt_de_regels_van_de_situatie() {
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
    @Test void een_onvolledige_periode_is_alleen_een_syntaxfout() {
        for (String tekst : List.of("Parameterset p:\nde parameter de vondstdrempel is 3 van 01-01-2025\n",
                "Situatie s:\nde parameter het penningbedrag is 7 van 01-01-2024\n")) {
            ScenarioBundel b = resolve(tekst);
            assertFalse(b.isOk());
            assertTrue(b.model().errors().stream().allMatch(d -> d.message().startsWith("syntaxfout: ")),
                    () -> b.model().errors().toString());
        }
    }

    @Test void een_ongekozen_set_wordt_toch_gecontroleerd() {
        fout("Parameterset ongebruikt:\nde parameter de getijdendrempel is 4\n", "getijdendrempel",
                "de parameter 'de getijdendrempel' is niet gedeclareerd");
    }
}
