package dev.uchar.regelspraak;

import dev.uchar.regelspraak.model.RegelSpraakModel;
import dev.uchar.regelspraak.model.RegelSpraakModel.Diagnostic;
import dev.uchar.regelspraak.model.RegelSpraakModel.Severity;
import dev.uchar.regelspraak.parser.RegelSpraak;
import dev.uchar.regelspraak.parser.RegelSpraakLexer;
import dev.uchar.regelspraak.resolver.Resolver;
import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.misc.Interval;
import org.antlr.v4.runtime.tree.ParseTree;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * A chain carries at most one die/dat subselection, and it belongs to the set right before it (grammar/DIVERGENCES.md,
 * "One subselection per chain"; card subselection-per-chain). The grammar used to admit a second predicate on one
 * chain by left recursion, so in R1 of the card fold-in-element-condition both "die" hung on the outer anchor "de
 * Schuit": the outer fold took the inner predicate as its filter and the outer one stayed unconsumed (RGS-099).
 */
class SubselectionPerChainTest {

    static final String MODEL = """
        Eenheidsysteem Tijd
            de dag dg
            de maand mnd

        Domein Klachtsoort is van het type Enumeratie
            'Zwaar'
            'Licht'

        Objecttype de Schuit (mv: Schuiten)
            de eerste schone maand vanaf-de-eindmaand Datum in dagen;

        Objecttype de Lading in-een-maand (mv: Ladingen in-een-maand)
            de maand Datum in dagen;
            het gewicht Numeriek (geheel getal);
            de overlading kenmerk (bezittelijk);

        Objecttype de Klacht (mv: Klachten)
            de einddag Datum in dagen;
            de soort Klachtsoort;

        Feittype lading in-een-maand
            de schuit Schuit
            de lading (mv: ladingen) Lading in-een-maand
        Eén schuit heeft meerdere ladingen

        Feittype schuitklacht
            de beklaagde schuit Schuit
            de klacht (mv: klachten) Klacht
        Eén beklaagde schuit heeft meerdere klachten

        Regel overlading
            geldig altijd
                Een Lading in-een-maand heeft de overlading
                indien zijn gewicht groter is dan 100.

        """;

    static final String R1 = MODEL + """
        Regel eerste schone maand vanaf de eindmaand
            geldig altijd
                De eerste schone maand vanaf-de-eindmaand van een Schuit moet berekend worden als de eerste van de maand van alle ladingen van de Schuit die aan alle volgende voorwaarden voldoen:
                - hij heeft geen overlading
                - zijn maand plus 1 mnd is later dan de laatste van de einddag van alle klachten van de Schuit die aan de volgende voorwaarde voldoen: zijn soort is gelijk aan 'Zwaar'.
        """;

    /** R2 of the card: R1 with the inner fold in parentheses. */
    static final String R2 = MODEL + """
        Regel eerste schone maand vanaf de eindmaand
            geldig altijd
                De eerste schone maand vanaf-de-eindmaand van een Schuit moet berekend worden als de eerste van de maand van alle ladingen van de Schuit die aan alle volgende voorwaarden voldoen:
                - hij heeft geen overlading
                - zijn maand plus 1 mnd is later dan (de laatste van de einddag van alle klachten van de Schuit die aan de volgende voorwaarde voldoen: zijn soort is gelijk aan 'Zwaar').
        """;

    @Test void r1_and_r2_hang_each_subselection_on_the_set_before_it() {
        List<String> r1 = subselections(R1), r2 = subselections(R2);
        assertEquals(List.of(
                "alle ladingen van de Schuit | aan alle volgende voorwaarden voldoen: - hij heeft geen overlading - zijn "
                        + "maand plus 1 mnd is later dan de laatste van de einddag van alle klachten van de Schuit die aan de "
                        + "volgende voorwaarde voldoen: zijn soort is gelijk aan 'Zwaar'.",
                "alle klachten van de Schuit | aan de volgende voorwaarde voldoen: zijn soort is gelijk aan 'Zwaar'"), r1,
                "de binnenste deelselectie hoort bij 'alle klachten van de Schuit'");
        assertEquals(r1, r2, "R1 ontleedt als R2 met haakjes");
    }

    @Test void r1_leaves_no_subselection_unconsumed() {
        RegelSpraakModel m = Resolver.resolve(R1);
        assertTrue(m.diagnostics.stream().noneMatch(d -> "RGS-099".equals(d.code())),
                () -> "elke deelselectie van R1 heeft een vouw: " + m.diagnostics);
        assertTrue(m.diagnostics.stream().noneMatch(d -> "RGS-091".equals(d.code())),
                () -> "R1 ontleedt zonder syntaxfout: " + m.diagnostics);
    }

    /** The quantified set of a condition has no chain around it, so a second "die" on its anchor does not parse. */
    @Test void a_second_subselection_on_one_chain_is_a_syntax_error_with_a_hint() {
        RegelSpraakModel m = Resolver.resolve(MODEL + """
            Regel eerste schone maand vanaf de eindmaand
                geldig altijd
                    De eerste schone maand vanaf-de-eindmaand van een Schuit moet gesteld worden op 01-01-2026
                    indien alle ladingen van de Schuit die aan de volgende voorwaarde voldoen: zijn gewicht is groter dan 10 die aan de volgende voorwaarde voldoen: zijn gewicht is kleiner dan 90 de overlading hebben.
            """);
        assertTrue(m.diagnostics.stream().anyMatch(d -> "RGS-091".equals(d.code())),
                () -> "een tweede 'die' op één keten ontleedt niet: " + m.diagnostics);
        assertHint(m, 4 + MODEL.lines().count(), "aan de volgende voorwaarde voldoen: zijn gewicht is groter dan 10");
    }

    /** In a fold operand a chain stands around the set ("de maand van alle ladingen …"): the second "die" lands on it,
     *  no fold consumes it, and the refusal of an unconsumed subselection gets the same hint. */
    @Test void a_second_subselection_in_a_fold_operand_is_refused_with_the_hint() {
        for (String[] tail : List.of(
                new String[] {"die aan de volgende voorwaarde voldoen: zijn gewicht is groter dan 10 die aan de volgende "
                        + "voorwaarde voldoen: zijn gewicht is kleiner dan 90.",
                        "aan de volgende voorwaarde voldoen: zijn gewicht is groter dan 10"},
                new String[] {"die de overlading hebben die aan de volgende voorwaarde voldoen: zijn gewicht is kleiner dan 90.",
                        "de overlading hebben"})) {
            RegelSpraakModel m = Resolver.resolve(MODEL + """
                Regel eerste schone maand vanaf de eindmaand
                    geldig altijd
                        De eerste schone maand vanaf-de-eindmaand van een Schuit moet berekend worden als de eerste van de maand van alle ladingen van de Schuit\s""" + tail[0] + "\n");
            assertTrue(m.diagnostics.stream().anyMatch(d -> "RGS-099".equals(d.code())),
                    () -> "de tweede deelselectie wordt door geen vouw verbruikt: " + m.diagnostics);
            assertHint(m, 3 + MODEL.lines().count(), tail[1]);
        }
    }

    private static void assertHint(RegelSpraakModel m, long line, String first) {
        List<Diagnostic> hint = m.diagnostics.stream().filter(d -> "RGS-351".equals(d.code())).toList();
        assertEquals(1, hint.size(), () -> "een tweede 'die' krijgt één hint: " + m.diagnostics);
        assertEquals(Severity.ERROR, hint.get(0).severity());
        assertEquals(line, hint.get(0).line(), "de hint staat op de regel van de tweede 'die'");
        assertTrue(hint.get(0).message().contains("na de deelselectie '" + first + "'"),
                () -> "de hint noemt de eerste deelselectie: " + hint.get(0).message());
        assertTrue(hint.get(0).message().contains("aan alle volgende voorwaarden voldoen"),
                () -> "de hint wijst de lijst als uitweg: " + hint.get(0).message());
    }

    @Test void a_stray_die_after_no_subselection_gets_no_hint() {
        RegelSpraakModel m = Resolver.resolve(MODEL + """
            Regel eerste schone maand vanaf de eindmaand
                geldig altijd
                    De eerste schone maand vanaf-de-eindmaand van een Schuit moet berekend worden als de maand die 3.
            """);
        assertTrue(m.diagnostics.stream().anyMatch(d -> "RGS-091".equals(d.code())), () -> "" + m.diagnostics);
        assertTrue(m.diagnostics.stream().noneMatch(d -> "RGS-351".equals(d.code())),
                () -> "zonder eerdere deelselectie op de keten is er geen tweede: " + m.diagnostics);
    }

    /** Every subselection of {@code source}: the set it selects from, as written up to the "die", and its predicate,
     *  whitespace collapsed and parentheses dropped, in document order. */
    private static List<String> subselections(String source) {
        RegelSpraak parser = new RegelSpraak(new CommonTokenStream(new RegelSpraakLexer(CharStreams.fromString(source))));
        List<String> faults = new ArrayList<>();
        parser.removeErrorListeners();
        parser.addErrorListener(new BaseErrorListener() {
            @Override public void syntaxError(Recognizer<?, ?> r, Object sym, int line, int col, String msg,
                                              RecognitionException e) { faults.add(line + ": " + msg); }
        });
        ParseTree doc = parser.regelSpraakDocument();
        assertTrue(faults.isEmpty(), () -> "syntaxfouten: " + faults);
        List<String> result = new ArrayList<>();
        collect(doc, result);
        return result;
    }

    private static void collect(ParseTree t, List<String> result) {
        if (t instanceof RegelSpraak.PredicaatContext p && p.getParent() instanceof RegelSpraak.OnderwerpKetenContext) {
            ParserRuleContext set = p.getParent();
            while (set.getParent() instanceof RegelSpraak.OnderwerpContext o) set = o;
            String whole = text(set);
            result.add(whole.substring(0, whole.indexOf(" die ")) + " | " + text(p));
        }
        for (int i = 0; i < t.getChildCount(); i++) collect(t.getChild(i), result);
    }

    private static String text(ParserRuleContext c) {
        String s = c.getStart().getInputStream().getText(Interval.of(c.getStart().getStartIndex(), c.getStop().getStopIndex()));
        return s.replaceAll("[()]", "").replaceAll("\\s+", " ").trim();
    }
}
