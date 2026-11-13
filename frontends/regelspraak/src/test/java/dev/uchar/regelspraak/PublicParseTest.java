package dev.uchar.regelspraak;

import dev.uchar.regelspraak.parser.RegelSpraak;
import dev.uchar.regelspraak.parser.RegelSpraakLexer;
import dev.uchar.regelspraak.resolver.Resolver;
import dev.uchar.regelspraak.resolver.Resolver.SyntaxError;
import org.antlr.v4.runtime.Token;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The public parse ({@link Resolver#parse(String)}) is the parse the resolver reads: a caller that shows a syntax
 * error with its column and the expected tokens, or reads the tree and the comments in the token stream, needs no
 * parse of its own. An error of the lexer is a syntax error like any other, with no expected tokens, and never an
 * exception; a document whose dimensions declare a label with the word 'en' is the second parse, in which the label
 * parses inside a name.
 */
class PublicParseTest {

    private static final String JUTTER = """
        Objecttype de Jutter (mv: Jutters) (bezield)
            het vondstbedrag Numeriek (getal met 2 decimalen);
            is premiewaardig kenmerk (bijvoeglijk);
        """;

    /** A character no token begins with: the lexer reports it, and its follow set is no question to ask a lexer
     *  (ANTLR's {@code getExpectedTokens} on a lexer's exception throws "Invalid state number"). */
    @Test
    void aLexerErrorIsASyntaxErrorWithoutExpectedTokens() {
        Resolver.Parse p = Resolver.parse(JUTTER + """

            Regel premie \\ toets
                geldig altijd
                    Een Jutter is premiewaardig indien zijn vondstbedrag groter is dan 10.
            """);
        assertEquals(List.of(new SyntaxError(5, 13, "token recognition error at: '\\'", null, List.of())),
                p.errors());
        assertFalse(p.testDocument());
        assertInstanceOf(RegelSpraak.RegelSpraakDocumentContext.class, p.document());
    }

    /** A parser error carries the offending token's text, its line and column, and the tokens the parser expected
     *  there by their grammar names. */
    @Test
    void aParserErrorCarriesItsColumnAndTheExpectedTokens() {
        Resolver.Parse p = Resolver.parse("Parameter het premietarief : Numeriek (geheel getal;\n");
        assertEquals(List.of(new SyntaxError(1, 51, "missing RPAREN at ';'", ";", List.of("RPAREN"))), p.errors());
    }

    private static final String TIDE = """
        Dimensie het getij, bestaande uit de getijden (na het attribuut met voorzetsel bij):
            1. eb
            2. vloed
            3. eb en vloed

        Objecttype de Jutter (mv: Jutters) (bezield)
            het getijgewicht Numeriek (getal met 2 decimalen) gedimensioneerd met getij;
            de uitkomst Numeriek (getal met 2 decimalen);

        Regel r
            geldig altijd
                De uitkomst van een Jutter moet berekend worden als zijn getijgewicht bij eb en vloed.
        """;

    /** The label "eb en vloed" inside the rule's name: the first parse reads 'en' as the conjunction and reports
     *  "extraneous input 'en'"; the public parse is the second one, with that 'en' a name word, as the resolver reads
     *  it. */
    @Test
    void aLabelWithEnParsesInsideAName() {
        Resolver.Parse p = Resolver.parse(TIDE);
        assertEquals(List.of(), p.errors());
        List<Token> en = p.tokens().getTokens().stream()
                .filter(t -> t.getLine() == 12 && t.getText().equals("en")).toList();
        assertEquals(1, en.size());
        assertEquals(RegelSpraakLexer.LOWER_ID, en.get(0).getType());
        assertTrue(Resolver.resolve(TIDE).isOk());
    }

    /** A source that opens a block of the scenario layer is a test document, parsed under {@code testDocument} as the
     *  scenario route parses it. */
    @Test
    void aSourceWithAScenarioIsATestDocument() {
        Resolver.Parse p = Resolver.parse("""
            Objecttype het Sein (mv: Seinen)
                de hoogte Numeriek (geheel getal);
                is half stok kenmerk (bijvoeglijk);

            Regel halfstok van een sein
                geldig altijd
                    Een Sein is half stok
                    indien de hoogte van het Sein kleiner is dan 5.

            Scenario laag
            Gegeven is de volgende situatie:
            een Sein S1
            - de hoogte is 3
            Met als verwacht resultaat:
            - S1 is half stok
            """);
        assertEquals(List.of(), p.errors());
        assertTrue(p.testDocument());
        assertInstanceOf(RegelSpraak.TestDocumentContext.class, p.document());
    }
}
