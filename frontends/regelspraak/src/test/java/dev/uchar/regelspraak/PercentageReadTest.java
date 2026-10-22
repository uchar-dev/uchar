package dev.uchar.regelspraak;

import dev.uchar.regelspraak.ir.Expr;
import dev.uchar.regelspraak.ir.ResolvedRule;
import dev.uchar.regelspraak.model.RegelSpraakModel;
import dev.uchar.regelspraak.resolver.Resolver;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * "Het percentage P van Y" without the '%' (grammar/DIVERGENCES.md, "The percentage forms"). The grammar reads the
 * sentence as an attribute read, and the resolver re-reads it as the percentage function where the first name part
 * is a Percentage parameter. Until 2026-10-02 that re-reading took a Y of one or two name parts only: a navigated Y
 * ("het taxatiebedrag van de taxatie van de Vondst") gave an ERROR that no member "percentage havengeld van het
 * taxatiebedrag" exists, a Y between parentheses was a syntax error, a Y named by a "Daarbij geldt" variable gave an
 * ERROR that the name is no role, and a '%' after the parameter's name was read as a word of that name, in every one
 * of these forms. Each is now the percentage function over the read the sentence writes.
 */
class PercentageReadTest {

    private static final String MODEL = """
        Parameter het percentage havengeld : Percentage (getal met 1 decimalen)

        Objecttype de Vondst (mv: Vondsten)
            de getaxeerde waarde Numeriek (getal met 2 decimalen);
            het havengeld Numeriek (getal met 2 decimalen);

        Objecttype de Taxatie (mv: Taxaties)
            het taxatiebedrag Numeriek (getal met 2 decimalen);

        Feittype VondstHeeftTaxatie
            de vondst Vondst
            de taxatie Taxatie
        Eén vondst heeft één taxatie

        Regel getaxeerde waarde
            geldig altijd
                De getaxeerde waarde van een Vondst moet gesteld worden op het taxatiebedrag van de taxatie van de Vondst.

        """;

    private static String havengeld(String value) {
        return MODEL + "Regel havengeld\n    geldig altijd\n        Het havengeld van een Vondst moet berekend worden als ("
                + value + ") naar beneden afgerond op 2 decimalen.\n";
    }

    private static RegelSpraakModel clean(String source) {
        RegelSpraakModel m = Resolver.resolve(source);
        assertEquals(List.of(), m.errors().stream().map(RegelSpraakModel.Diagnostic::message).toList());
        return m;
    }

    private static Expr value(RegelSpraakModel m) {
        ResolvedRule r = m.resolvedRules.stream().filter(x -> x.name().equals("havengeld")).findFirst().orElseThrow();
        return r.value();
    }

    /** The percentage function of the rule's value, under the rounding (and the Let chain, where there is one). */
    private static Expr.Binary percentageOf(Expr w) {
        while (w instanceof Expr.Let l) w = l.body();
        Expr base = assertInstanceOf(Expr.Rounding.class, w).base();
        Expr.Binary b = assertInstanceOf(Expr.Binary.class, base);
        assertEquals(Expr.BinOp.PERCENTAGE_VAN, b.op());
        assertEquals("percentage havengeld", assertInstanceOf(Expr.ParameterRef.class, b.left()).parameter().name());
        return b;
    }

    /** The read of the taxatiebedrag, one step from the Vondst over the role taxatie. */
    private static void taxatiebedragViaTaxatie(Expr y) {
        Expr.AttributeReference a = assertInstanceOf(Expr.AttributeReference.class, y);
        assertEquals("taxatiebedrag", a.attribute());
        assertEquals("Taxatie", a.objectType().name);
        assertEquals(List.of("taxatie"), a.navigationPath());
    }

    // ---- the four forms that did not resolve

    @Test void a_a_navigated_base() {
        // before: ERROR "genavigeerde lezing '…': het lid 'percentage havengeld van het taxatiebedrag' bestaat niet op
        // 'Taxatie', de vuller van de rol 'taxatie', en op geen ander objecttype: …"
        taxatiebedragViaTaxatie(percentageOf(value(clean(havengeld(
                "het percentage havengeld van het taxatiebedrag van de taxatie van de Vondst")))).right());
    }

    @Test void b_a_base_between_parentheses() {
        // before: ERROR "syntaxfout: no viable alternative at input 'hetpercentagehavengeldvan('"
        taxatiebedragViaTaxatie(percentageOf(value(clean(havengeld(
                "het percentage havengeld van (het taxatiebedrag van de taxatie van de Vondst)")))).right());
    }

    @Test void d_a_base_from_a_local_variable() {
        // before: ERROR "lezing 'het percentage havengeld van het getaxeerde bedrag': 'het getaxeerde bedrag' noemt
        // vanaf 'Vondst' geen rol en geen objecttype: …"
        RegelSpraakModel m = clean(MODEL + """
            Regel havengeld
                geldig altijd
                    Het havengeld van een Vondst moet berekend worden als (het percentage havengeld van het getaxeerde bedrag) naar beneden afgerond op 2 decimalen.
                    Daarbij geldt:
                        het getaxeerde bedrag is het taxatiebedrag van de taxatie van de Vondst.
            """);
        Expr w = value(m);
        Expr.Let let = assertInstanceOf(Expr.Let.class, w);
        assertEquals("getaxeerde bedrag", let.binder());
        taxatiebedragViaTaxatie(let.value());
        assertEquals("getaxeerde bedrag", assertInstanceOf(Expr.BinderRef.class, percentageOf(w).right()).name());
    }

    @Test void h_the_percent_sign_after_the_parameter() {
        // before: ERROR "genavigeerde lezing '…': het lid 'percentage havengeld % van het taxatiebedrag' bestaat niet …"
        taxatiebedragViaTaxatie(percentageOf(value(clean(havengeld(
                "het percentage havengeld % van het taxatiebedrag van de taxatie van de Vondst")))).right());
    }

    @Test void h_the_percent_sign_after_the_parameter_for_an_own_member() {
        // before: ERROR "lezing 'het percentage havengeld % van zijn getaxeerde waarde': 'zijn getaxeerde waarde'
        // noemt vanaf 'Vondst' geen rol en geen objecttype: …"
        Expr y = percentageOf(value(clean(havengeld("het percentage havengeld % van zijn getaxeerde waarde")))).right();
        Expr.AttributeReference a = assertInstanceOf(Expr.AttributeReference.class, y);
        assertEquals("getaxeerde waarde", a.attribute());
        assertEquals("Vondst", a.objectType().name);
    }

    // ---- the source of each operand

    /** P and Y carry the text that writes them, the percentage function the whole sentence. Until 2026-10-22 both
     *  operands of the re-reading carried the sentence of the function, so a sentence that names the operands with
     *  their values named the whole phrase twice; the '%' form and the parenthesised form already gave each operand
     *  its own text. */
    @Test void k_each_operand_carries_its_own_source() {
        String navigated = "het taxatiebedrag van de taxatie van de Vondst";
        List<String[]> rows = List.of(
                new String[] {"het percentage havengeld van " + navigated, navigated},
                new String[] {"het percentage havengeld % van " + navigated, navigated},
                new String[] {"het percentage havengeld van zijn getaxeerde waarde", "zijn getaxeerde waarde"},
                new String[] {"het percentage havengeld van de getaxeerde waarde van de Vondst",
                        "de getaxeerde waarde van de Vondst"},
                new String[] {"het percentage havengeld van (" + navigated + ")", navigated});
        List<String> faults = new java.util.ArrayList<>();
        for (String[] row : rows) {
            Expr.Binary b = percentageOf(value(clean(havengeld(row[0]))));
            List<String> got = List.of(b.source().snippet(), b.left().source().snippet(), b.right().source().snippet());
            List<String> want = List.of(row[0], "percentage havengeld", row[1]);
            if (!got.equals(want)) faults.add(row[0] + ": " + got + ", verwacht " + want);
        }
        Expr.Binary local = percentageOf(value(clean(MODEL + """
            Regel havengeld
                geldig altijd
                    Het havengeld van een Vondst moet berekend worden als (het percentage havengeld van het getaxeerde bedrag) naar beneden afgerond op 2 decimalen.
                    Daarbij geldt:
                        het getaxeerde bedrag is het taxatiebedrag van de taxatie van de Vondst.
            """)));
        List<String> got = List.of(local.left().source().snippet(), local.right().source().snippet());
        if (!got.equals(List.of("percentage havengeld", "het getaxeerde bedrag")))
            faults.add("Daarbij geldt: " + got);
        assertEquals(List.of(), faults);
    }

    /** A reading of the sentence as the percentage function that is not taken leaves no diagnostic behind: the
     *  navigation the sentence then stays raises its own. Until 2026-10-22 an ordinal the rule cannot place was
     *  reported twice, once from the attempt to build Y and once from the navigation. */
    @Test void l_a_rejected_reading_reports_nothing_of_its_own() {
        for (String value : List.of(
                "het percentage havengeld van het taxatiebedrag van de taxatie van de als tweede genoemde Vondst",
                "het percentage havengeld van de getaxeerde waarde van de als tweede genoemde Vondst")) {
            RegelSpraakModel m = Resolver.resolve(havengeld(value));
            assertEquals(List.of("rangtelwoord: de regel noemt geen 2 onderwerpen 'Vondst' (§5.5.3)"),
                    m.diagnostics.stream().map(RegelSpraakModel.Diagnostic::message).toList(), value);
        }
    }

    // ---- the forms that already resolved, unchanged

    @Test void e_an_own_member_stays_the_base() {
        Expr.AttributeReference a = assertInstanceOf(Expr.AttributeReference.class,
                percentageOf(value(clean(havengeld("het percentage havengeld van zijn getaxeerde waarde")))).right());
        assertEquals("getaxeerde waarde", a.attribute());
        assertEquals(List.of(), a.navigationPath());
    }

    @Test void j_an_own_member_with_the_subject_name_stays_the_base() {
        Expr.AttributeReference a = assertInstanceOf(Expr.AttributeReference.class, percentageOf(value(clean(
                havengeld("het percentage havengeld van de getaxeerde waarde van de Vondst")))).right());
        assertEquals("getaxeerde waarde", a.attribute());
        assertEquals("Vondst", a.objectType().name);
    }

    @Test void i_a_literal_percentage_of_a_navigated_base_stays() {
        Expr w = value(clean(havengeld("12,5 % van het taxatiebedrag van de taxatie van de Vondst")));
        Expr.Binary b = assertInstanceOf(Expr.Binary.class, assertInstanceOf(Expr.Rounding.class, w).base());
        assertEquals(Expr.BinOp.PERCENTAGE_VAN, b.op());
        taxatiebedragViaTaxatie(b.right());
    }

    @Test void a_period_between_parentheses_stays_a_period() {
        // "van (A) tot (B)" after a value is the period comparison of §13.4.15.68, as before the parenthesised
        // percentage form: that form is primaireExpressie's last alternative, so the reading it had wins
        RegelSpraakModel m = clean(MODEL.replace("het havengeld Numeriek (getal met 2 decimalen);",
                "het havengeld Numeriek (getal met 2 decimalen);\n    de vinddag Datum in dagen;\n    de meldingsdag Datum in dagen;")
                + "Regel havengeld\n    geldig altijd\n        Het havengeld van een Vondst moet gesteld worden op 5,00"
                + " van (de vinddag van de Vondst) tot (de meldingsdag van de Vondst).\n");
        assertTrue(dev.uchar.regelspraak.emit.IrEmitter.emit(m).contains("\"node\": \"TimeWindow\""));
    }

    @Test void the_percent_sign_before_parentheses_stays_the_form_with_percent_sign() {
        // "P % van (Y)" and "12,5 % van (Y)" read as the '%' form, as before: the name does not take the sign
        assertInstanceOf(Expr.AttributeReference.class, percentageOf(value(clean(havengeld(
                "het percentage havengeld % van (zijn getaxeerde waarde)")))).right());
        Expr w = value(clean(havengeld("12,5 % van (zijn getaxeerde waarde)")));
        Expr.Binary b = assertInstanceOf(Expr.Binary.class, assertInstanceOf(Expr.Rounding.class, w).base());
        assertEquals(Expr.BinOp.PERCENTAGE_VAN, b.op());
        assertInstanceOf(Expr.Literal.class, b.left());
    }

    @Test void parentheses_after_something_else_than_a_percentage_are_refused() {
        // before: a syntax error ("no viable alternative at input 'of(zijngetaxeerdewaarde)'")
        RegelSpraakModel m = Resolver.resolve(MODEL.replace("Objecttype de Vondst",
                "Parameter de korting : Numeriek (getal met 2 decimalen)\n\nObjecttype de Vondst")
                + "Regel havengeld\n    geldig altijd\n        Het havengeld van een Vondst moet berekend worden als "
                + "de korting van (zijn getaxeerde waarde).\n");
        assertEquals(List.of("'de korting van (zijn getaxeerde waarde)' leest als de percentagefunctie zonder "
                + "procentteken (§13.4.17.12), maar 'de korting' is geen parameter of variabele van het type Percentage "
                + "— schrijf het procentteken ('… % van …') of noem een percentage"),
                m.errors().stream().map(RegelSpraakModel.Diagnostic::message).toList());
    }

    @Test void the_product_with_a_percentage_stays_a_type_error() {
        // the product is a typing question (§4.4 lists no Percentage row), not a navigation: refused as before
        RegelSpraakModel m = Resolver.resolve(havengeld("zijn getaxeerde waarde maal het percentage havengeld"));
        assertEquals(List.of("maal vereist numerieke operanden, kreeg Numeriek en Percentage"),
                m.errors().stream().map(RegelSpraakModel.Diagnostic::message).toList());
    }
}
