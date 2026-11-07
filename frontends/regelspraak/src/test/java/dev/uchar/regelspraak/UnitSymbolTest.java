package dev.uchar.regelspraak;

import dev.uchar.regelspraak.emit.IrEmitter;
import dev.uchar.regelspraak.model.RegelSpraakModel;
import dev.uchar.regelspraak.parser.RegelSpraakLexer;
import dev.uchar.regelspraak.resolver.Resolver;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.Token;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The symbol of a unit (IR-SPEC ch. 2.5; grammar/DIVERGENCES.md, "De eenheidsregel: afkorting en symbool"): on a unit
 * line the token after the abbreviation is the symbol, written on the wire as {@code DeclaredUnit.symbol}. Two units may
 * carry one symbol; a reference with such a symbol names none of them (RGS-390), and one with the abbreviation always
 * works. A third token has no role (RGS-389), and a symbol equal to an abbreviation of its system never reaches its unit
 * (RGS-391). A currency sign, after letters where it writes them ({@code $}, {@code R$}, {@code £}), is one token and
 * may stand as abbreviation or symbol.
 */
class UnitSymbolTest {

    private static String emitCompact(String src) {
        RegelSpraakModel m = Resolver.resolve(src);
        assertTrue(m.diagnostics.isEmpty(), () -> "onverwachte diagnostiek: " + m.diagnostics);
        return IrEmitter.emit(m).replaceAll("\\s+", "");
    }

    private static List<String> codes(RegelSpraakModel m) {
        return m.diagnostics.stream().map(d -> d.code() + " " + d.message()).toList();
    }

    private static final String KRONEN = """
            Eenheidsysteem Munt
                de kroon (mv: kronen) SEK kr
                de krone (mv: kroner) NOK kr
            Eenheidsysteem Tijd
                het jaar (mv: jaren) jr
            """;

    @Test void the_token_after_the_abbreviation_is_the_symbol() {
        String c = emitCompact("""
            Eenheidsysteem Munt
                de euro (mv: euros) EUR €
                de cent (mv: centen) ct = 1/100 €
            Objecttype de Vondst
                de basis Numeriek (getal met 2 decimalen) met eenheid €;
            """);
        assertTrue(c.contains("{\"node\":\"Unit\",\"abbreviation\":\"EUR\",\"system\":\"Munt\",\"canonical\":\"EUR\","
                + "\"factorToCanonical\":{\"num\":1,\"den\":1},\"symbol\":\"\\u20ac\",\"spellings\":[\"euro\",\"euros\"]}"),
                () -> "het symbool staat in zijn eigen veld en niet bij de spellingen:\n" + c);
        assertTrue(c.contains("{\"node\":\"Unit\",\"abbreviation\":\"ct\",\"system\":\"Munt\",\"canonical\":\"EUR\","
                + "\"factorToCanonical\":{\"num\":1,\"den\":100},\"spellings\":[\"cent\",\"centen\"]}"),
                () -> "een omrekening naar het symbool bereikt zijn eenheid:\n" + c);
        assertTrue(c.contains("\"datatype\":{\"node\":\"Number\",\"whole\":false,\"decimals\":2,\"sign\":\"unspecified\","
                + "\"unit\":[{\"node\":\"Unit\",\"abbreviation\":\"EUR\",\"system\":\"Munt\",\"canonical\":\"EUR\","
                + "\"factorToCanonical\":{\"num\":1,\"den\":1}}]}"),
                () -> "met eenheid € verwijst naar EUR, en het voorkomen draagt geen symbool:\n" + c);
        assertEquals(1, c.split("\"symbol\"", -1).length - 1, () -> "het symbool alleen op de declaratie:\n" + c);
    }

    @Test void one_token_is_an_abbreviation_also_when_it_is_the_euro_sign() {
        String c = emitCompact("""
            Eenheidsysteem Munt
                de euro (mv: euros) €
            Objecttype de Vondst
                de basis Numeriek (getal met 2 decimalen) met eenheid €;
            """);
        assertTrue(c.contains("{\"node\":\"Unit\",\"abbreviation\":\"\\u20ac\",\"system\":\"Munt\",\"canonical\":\"\\u20ac\","
                + "\"factorToCanonical\":{\"num\":1,\"den\":1},\"spellings\":[\"euro\",\"euros\"]}"), c);
        assertFalse(c.contains("\"symbol\""), c);
    }

    @Test void a_third_token_has_no_role() {
        RegelSpraakModel m = Resolver.resolve("""
            Eenheidsysteem Munt
                de euro (mv: euros) EUR € E
            Objecttype de Vondst
                de basis Numeriek (getal met 2 decimalen) met eenheid EUR;
            """);
        assertEquals(List.of("RGS-389 de eenheid 'EUR' van het eenheidstelsel 'Munt' schrijft 'E' na haar symbool '€': een "
                        + "eenheidsregel is een woord, een meervoud, een afkorting en een symbool, en een verder token heeft "
                        + "geen rol"),
                codes(m));
        assertEquals(2, m.diagnostics.get(0).line(), "de melding staat op de eenheidsregel");
    }

    @Test void two_units_may_carry_one_symbol_and_the_abbreviation_names_each() {
        String c = emitCompact(KRONEN + """
            Objecttype de Vondst
                de opbrengst Numeriek (getal met 2 decimalen) met eenheid SEK;
                de jaaropbrengst Numeriek (getal met 2 decimalen) met eenheid NOK/jr;
            """);
        assertTrue(c.contains("{\"node\":\"Unit\",\"abbreviation\":\"SEK\",\"system\":\"Munt\",\"canonical\":\"SEK\","
                + "\"factorToCanonical\":{\"num\":1,\"den\":1},\"symbol\":\"kr\",\"spellings\":[\"kroon\",\"kronen\"]}"), c);
        assertTrue(c.contains("{\"node\":\"Unit\",\"abbreviation\":\"NOK\",\"system\":\"Munt\",\"canonical\":\"NOK\","
                + "\"factorToCanonical\":{\"num\":1,\"den\":1},\"symbol\":\"kr\",\"spellings\":[\"krone\",\"kroner\"]}"), c);
        assertTrue(c.contains("\"unit\":[{\"node\":\"Unit\",\"abbreviation\":\"SEK\",\"system\":\"Munt\""), c);
        assertTrue(c.contains("\"unit\":[{\"node\":\"Unit\",\"abbreviation\":\"NOK\",\"system\":\"Munt\",\"canonical\":"
                + "\"NOK\",\"factorToCanonical\":{\"num\":1,\"den\":1}},{\"node\":\"Unit\",\"abbreviation\":\"jr\""), c);
    }

    @Test void a_reference_with_a_shared_symbol_names_neither_unit() {
        String ambiguous = "RGS-390 de eenheid 'kr' is dubbelzinnig: in het eenheidstelsel 'Munt' dragen de eenheden 'SEK', "
                + "'NOK' het symbool 'kr'; schrijf de afkorting van de bedoelde eenheid";
        RegelSpraakModel declared = Resolver.resolve(KRONEN + """
            Objecttype de Vondst
                de opbrengst Numeriek (getal met 2 decimalen) met eenheid kr;
            """);
        assertEquals(List.of(ambiguous), codes(declared));
        assertEquals(7, declared.diagnostics.get(0).line(), "de melding staat bij de verwijzing");

        RegelSpraakModel composite = Resolver.resolve(KRONEN + """
            Objecttype de Vondst
                de jaaropbrengst Numeriek (getal met 2 decimalen) met eenheid kr/jr;
            """);
        assertEquals(List.of(ambiguous), codes(composite));

        RegelSpraakModel value = Resolver.resolve(KRONEN + """
            Objecttype de Vondst
                de opbrengst Numeriek (getal met 2 decimalen) met eenheid SEK;
            Regel opbrengst
                geldig altijd
                    De opbrengst van een Vondst moet berekend worden als 100 kr.
            """);
        assertTrue(codes(value).contains(ambiguous), () -> "de waarde 100 kr: " + codes(value));

        RegelSpraakModel conversion = Resolver.resolve("""
            Eenheidsysteem Munt
                de kroon (mv: kronen) SEK kr
                de krone (mv: kroner) NOK kr
                de ore (mv: oren) ore = 1/100 kr
            Objecttype de Vondst
                de opbrengst Numeriek (getal met 2 decimalen) met eenheid ore;
            """);
        assertEquals(List.of(ambiguous), codes(conversion));
        assertEquals(4, conversion.diagnostics.get(0).line(), "de melding staat bij de omrekening");
    }

    @Test void a_symbol_equal_to_an_abbreviation_of_its_system_is_refused() {
        RegelSpraakModel own = Resolver.resolve("""
            Eenheidsysteem Gewicht
                het kilogram kg KG
            Objecttype de Vondst
                het gewicht Numeriek (geheel getal) met eenheid kg;
            """);
        assertEquals(List.of("RGS-391 het symbool 'KG' van de eenheid 'kg' is de afkorting 'kg' in het eenheidstelsel "
                        + "'Gewicht', en een verwijzing leest een afkorting vóór een symbool: het symbool noemt zijn eenheid "
                        + "nooit"),
                codes(own));

        RegelSpraakModel other = Resolver.resolve("""
            Eenheidsysteem Gewicht
                de gram g kg
                het kilogram kg = 1000 g
            Objecttype de Vondst
                het gewicht Numeriek (geheel getal) met eenheid kg;
            """);
        assertEquals(List.of("RGS-391 het symbool 'kg' van de eenheid 'g' is de afkorting 'kg' in het eenheidstelsel "
                        + "'Gewicht', en een verwijzing leest een afkorting vóór een symbool: het symbool noemt zijn eenheid "
                        + "nooit"),
                codes(other));
        assertEquals(2, other.diagnostics.get(0).line(), "de melding staat op de declaratie met het symbool");
    }

    private static final String DOLLARS = """
            Eenheidsysteem Munt
                de VS-dollar (mv: VS-dollars) USD $
                de Canada-dollar (mv: Canada-dollars) CAD $
                de Australië-dollar (mv: Australië-dollars) AUD $
                de real (mv: reais) BRL R$
                het pond (mv: ponden) GBP £
            Eenheidsysteem Tijd
                het jaar (mv: jaren) jr
            """;

    private static final String SHARED_DOLLAR = "RGS-390 de eenheid '$' is dubbelzinnig: in het eenheidstelsel 'Munt' dragen "
            + "de eenheden 'USD', 'CAD', 'AUD' het symbool '$'; schrijf de afkorting van de bedoelde eenheid";

    /** The tokens of {@code text} on the default channel, as (symbolic name, text) pairs. */
    private static List<String> tokens(String text) {
        RegelSpraakLexer lexer = new RegelSpraakLexer(CharStreams.fromString(text));
        lexer.removeErrorListeners();
        return lexer.getAllTokens().stream().filter(t -> t.getChannel() == Token.DEFAULT_CHANNEL)
                .map(t -> lexer.getVocabulary().getSymbolicName(t.getType()) + " " + t.getText()).toList();
    }

    @Test void a_currency_sign_is_one_token_with_the_letters_before_it() {
        assertEquals(List.of("CURRENCY_SIGN $"), tokens("$"));
        assertEquals(List.of("CURRENCY_SIGN R$"), tokens("R$"));
        assertEquals(List.of("CURRENCY_SIGN US$"), tokens("US$"));
        assertEquals(List.of("CURRENCY_SIGN £"), tokens("£"));
        assertEquals(List.of("CURRENCY_SIGN €"), tokens("€"));
        assertEquals(List.of("CURRENCY_SIGN bedrag€"), tokens("bedrag€"));
        assertEquals(List.of("UPPER_ID BRL", "CURRENCY_SIGN R$"), tokens("BRL R$"));
        assertEquals(List.of("NUMBER 100", "CURRENCY_SIGN $", "SLASH /", "LOWER_ID jr"), tokens("100 $/jr"));
    }

    @Test void three_units_may_carry_the_dollar_sign_and_each_abbreviation_names_its_unit() {
        String c = emitCompact(DOLLARS + """
            Objecttype de Vondst
                de opbrengst Numeriek (getal met 2 decimalen) met eenheid USD;
                de jaaropbrengst Numeriek (getal met 2 decimalen) met eenheid CAD/jr;
            """);
        for (String a : List.of("USD", "CAD", "AUD"))
            assertTrue(c.contains("{\"node\":\"Unit\",\"abbreviation\":\"" + a + "\",\"system\":\"Munt\",\"canonical\":\""
                    + a + "\",\"factorToCanonical\":{\"num\":1,\"den\":1},\"symbol\":\"$\","), () -> a + ":\n" + c);
        assertTrue(c.contains("\"unit\":[{\"node\":\"Unit\",\"abbreviation\":\"USD\",\"system\":\"Munt\""), c);
        assertTrue(c.contains("\"unit\":[{\"node\":\"Unit\",\"abbreviation\":\"CAD\",\"system\":\"Munt\""), c);
    }

    @Test void a_reference_with_the_shared_dollar_sign_names_none_of_the_three() {
        RegelSpraakModel declared = Resolver.resolve(DOLLARS + """
            Objecttype de Vondst
                de opbrengst Numeriek (getal met 2 decimalen) met eenheid $;
            """);
        assertEquals(List.of(SHARED_DOLLAR), codes(declared));
        assertEquals(10, declared.diagnostics.get(0).line(), "de melding staat bij de verwijzing");

        RegelSpraakModel value = Resolver.resolve(DOLLARS + """
            Objecttype de Vondst
                de opbrengst Numeriek (getal met 2 decimalen) met eenheid USD;
            Regel opbrengst
                geldig altijd
                    De opbrengst van een Vondst moet berekend worden als 100 $.
            """);
        assertTrue(codes(value).contains(SHARED_DOLLAR), () -> "de waarde 100 $: " + codes(value));

        RegelSpraakModel rate = Resolver.resolve(DOLLARS + """
            Objecttype de Vondst
                de jaaropbrengst Numeriek (getal met 2 decimalen) met eenheid $/jr;
            """);
        assertEquals(List.of(SHARED_DOLLAR), codes(rate));
    }

    @Test void a_sign_with_letters_and_a_single_sign_are_the_symbols_of_their_units() {
        String c = emitCompact(DOLLARS + """
            Objecttype de Vondst
                de opbrengst Numeriek (getal met 2 decimalen) met eenheid R$;
                de waarde Numeriek (getal met 2 decimalen) met eenheid £;
            """);
        assertTrue(c.contains("{\"node\":\"Unit\",\"abbreviation\":\"BRL\",\"system\":\"Munt\",\"canonical\":\"BRL\","
                + "\"factorToCanonical\":{\"num\":1,\"den\":1},\"symbol\":\"R$\",\"spellings\":[\"real\",\"reais\"]}"), c);
        assertTrue(c.contains("{\"node\":\"Unit\",\"abbreviation\":\"GBP\",\"system\":\"Munt\",\"canonical\":\"GBP\","
                + "\"factorToCanonical\":{\"num\":1,\"den\":1},\"symbol\":\"\\u00a3\",\"spellings\":[\"pond\",\"ponden\"]}"), c);
        assertTrue(c.contains("\"unit\":[{\"node\":\"Unit\",\"abbreviation\":\"BRL\",\"system\":\"Munt\",\"canonical\":"
                + "\"BRL\",\"factorToCanonical\":{\"num\":1,\"den\":1}}]"), () -> "met eenheid R$ is BRL:\n" + c);
        assertTrue(c.contains("\"unit\":[{\"node\":\"Unit\",\"abbreviation\":\"GBP\",\"system\":\"Munt\",\"canonical\":"
                + "\"GBP\",\"factorToCanonical\":{\"num\":1,\"den\":1}}]"), () -> "met eenheid £ is GBP:\n" + c);
    }

    /** A text part with its unit over R$ and £: the part asks for the unit, and the declarations carry the symbols the
     *  part writes (RUNTIME-SEMANTICS ch. 1.2: "(R$)" and "(£)"). */
    @Test void a_text_part_with_its_unit_reaches_the_currency_symbol_of_its_declaration() {
        String c = emitCompact(DOLLARS + """
            Objecttype de Vondst
                de opbrengst Numeriek (getal met 2 decimalen) met eenheid R$;
                de waarde Numeriek (getal met 2 decimalen) met eenheid £;
                de omschrijving Tekst;
            Regel omschrijving
                geldig altijd
                    De omschrijving van een Vondst moet berekend worden als "«zijn opbrengst (eenheid)», «zijn waarde (eenheid)»".
            """);
        assertEquals(2, c.split("\"unit\":true", -1).length - 1, () -> "twee delen vragen hun eenheid:\n" + c);
        assertTrue(c.contains("\"symbol\":\"R$\""), c);
        assertTrue(c.contains("\"symbol\":\"\\u00a3\""), c);
    }

    /** The noun of a unit line is one word: of "de Amerikaanse dollar USD $" the noun is "Amerikaanse", the abbreviation
     *  "dollar" and the symbol "USD", and "$" has no role (grammar/DIVERGENCES.md, "De eenheidsregel"). */
    @Test void the_noun_of_a_unit_line_is_one_word() {
        RegelSpraakModel m = Resolver.resolve("""
            Eenheidsysteem Munt
                de Amerikaanse dollar USD $
            Objecttype de Vondst
                de opbrengst Numeriek (getal met 2 decimalen) met eenheid dollar;
            """);
        assertEquals(List.of("RGS-389 de eenheid 'dollar' van het eenheidstelsel 'Munt' schrijft '$' na haar symbool 'USD': "
                        + "een eenheidsregel is een woord, een meervoud, een afkorting en een symbool, en een verder token "
                        + "heeft geen rol"),
                codes(m));
    }
}
