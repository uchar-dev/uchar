package dev.uchar.regelspraak;

import dev.uchar.regelspraak.parser.RegelSpraak;
import dev.uchar.regelspraak.parser.RegelSpraakLexer;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.ErrorNode;
import org.antlr.v4.runtime.tree.ParseTree;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The grammar layer of the scenariotoetsen extension (grammar/DIVERGENCES.md, "Scenariotoetsen"), driven on the
 * ANTLR parser alone: a form parses clean under {@code testDocument} — no syntax error, no error node, the whole token
 * stream consumed — or it does not. The forms the lowering to case files needs beyond the old surface are here: the
 * optional scope, a period on an assignment, a characteristic, a parameter and an expectation, the empty timeline,
 * the key of an inconsistency, a situation based on situations, a role list with commas. And the two fences: a
 * scenario block is no production document, and 'tot en met' is no period.
 */
class ScenarioParseTest {

    private record Uitkomst(int fouten, int foutknopen, boolean alles, List<String> meldingen) {
        boolean schoon() { return fouten == 0 && foutknopen == 0 && alles; }
    }

    private static Uitkomst parse(String bron, Function<RegelSpraak, ParseTree> start) {
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
        return new Uitkomst(meldingen.size(), foutknopen(boom), parser.getInputStream().LA(1) == Token.EOF, meldingen);
    }

    private static int foutknopen(ParseTree t) {
        if (t instanceof ErrorNode) return 1;
        int n = 0;
        for (int i = 0; i < t.getChildCount(); i++) n += foutknopen(t.getChild(i));
        return n;
    }

    private static void schoon(String bron) {
        Uitkomst u = parse(bron, RegelSpraak::testDocument);
        assertTrue(u.schoon(), () -> "hoort schoon te parseren onder testDocument: " + u + "\n" + bron);
    }

    private static void fout(String bron) {
        Uitkomst u = parse(bron, RegelSpraak::testDocument);
        assertFalse(u.schoon(), () -> "hoort NIET te parseren onder testDocument:\n" + bron);
    }

    @Test void situatie_op_situaties_en_een_rollijst_met_komma() {
        schoon("""
            Situatie kaal:
            een Jutter J
            - de naam is "Aukje"
            Situatie met vondsten gebaseerd op de situatie kaal gebaseerd op de situatie bodem:
            een Jutter J
            - de vondsten zijn V1, V2 en V3
            een Vondst V1
            - de aanmelder is J
            """);
    }

    @Test void scenario_zonder_scope_eindigt_zijn_naam_bij_elk_kopwoord() {
        schoon("""
            Scenario een naam met woorden
            Gegeven is de volgende situatie:
            een Jutter J
            Met als verwacht resultaat:
            - de gegevens zijn consistent
            Scenario met een dag
            met peildatum 01-06-2025
            gebaseerd op de situatie kaal
            Met als verwacht resultaat:
            - J is bergingsbevoegd
            Scenario alleen een verwachting
            Met als verwacht resultaat:
            - de gegevens zijn inconsistent
            """);
    }

    @Test void elke_scopevorm() {
        schoon("""
            Scenario een
            toetst de regel bonus bij drempel
            gebaseerd op de situatie kaal
            Met als verwacht resultaat:
            - de gegevens zijn consistent
            Scenario twee
            toetst de regels bonus bij drempel, wachtbonus vanaf 2025 en rubriek van een aanspoelsel
            gebaseerd op de situatie kaal
            Met als verwacht resultaat:
            - de gegevens zijn consistent
            Scenario drie
            toetst de regelgroep estafette
            gebaseerd op de situatie kaal
            Met als verwacht resultaat:
            - de gegevens zijn consistent
            """);
    }

    @Test void perioden_op_attribuut_kenmerk_parameter_en_verwachting() {
        schoon("""
            Situatie tijd:
            een Jutter J
            - de borgdagen is 4 van 01-01-2024 tot 01-02-2024
            - is borgplichtig van 01-01-2024 tot 15-01-2024
            - heeft een strandrecht van 01-01-2024 tot 01-07-2024.
            de parameter het penningbedrag is 7 van 01-01-2024 tot 01-01-2025
            Scenario tijd
            gebaseerd op de situatie tijd
            Met als verwacht resultaat:
            - de borgdagen van J is 4 van 01-01-2024 tot 01-02-2024
            - de jaardagen van J is altijd leeg
            """);
    }

    @Test void de_sleutel_van_een_inconsistentie() {
        schoon("""
            Scenario sleutel
            gebaseerd op de situatie kaal
            Met als verwacht resultaat:
            - de gegevens zijn inconsistent volgens "Jutter.vrijgesteld/0" voor M
            - de gegevens zijn inconsistent volgens "eenmalig vondstnummer"
            """);
    }

    @Test void volgens_is_het_enige_woord_voor_de_sleutel() {
        fout("""
            Scenario sleutel
            gebaseerd op de situatie kaal
            Met als verwacht resultaat:
            - de gegevens zijn inconsistent door "eenmalig vondstnummer"
            """);
    }

    @Test void tot_en_met_is_geen_periode() {
        fout("""
            Situatie tijd:
            een Jutter J
            - is borgplichtig van 01-01-2024 tot en met 14-01-2024
            """);
    }

    @Test void een_scenarioblok_is_geen_productiedocument() {
        String blok = """
            Scenario los
            Gegeven is de volgende situatie:
            een Jutter J
            Met als verwacht resultaat:
            - de gegevens zijn consistent
            """;
        assertFalse(parse(blok, RegelSpraak::regelSpraakDocument).schoon(),
                "een scenarioblok hoort onder regelSpraakDocument te falen: de productieroute ziet de extensie niet");
        schoon(blok);
    }
}
