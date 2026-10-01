package dev.uchar.regelspraak;

import dev.uchar.regelspraak.ir.Expr;
import dev.uchar.regelspraak.ir.ResolvedRegel;
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
class PercentageLezingTest {

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

    private static String havengeld(String waarde) {
        return MODEL + "Regel havengeld\n    geldig altijd\n        Het havengeld van een Vondst moet berekend worden als ("
                + waarde + ") naar beneden afgerond op 2 decimalen.\n";
    }

    private static RegelSpraakModel schoon(String bron) {
        RegelSpraakModel m = Resolver.resolve(bron);
        assertEquals(List.of(), m.errors().stream().map(RegelSpraakModel.Diagnostic::message).toList());
        return m;
    }

    private static Expr waarde(RegelSpraakModel m) {
        ResolvedRegel r = m.resolvedRegels.stream().filter(x -> x.naam().equals("havengeld")).findFirst().orElseThrow();
        return r.waarde();
    }

    /** The percentage function of the rule's value, under the rounding (and the Let chain, where there is one). */
    private static Expr.Binair percentageVan(Expr w) {
        while (w instanceof Expr.Let l) w = l.body();
        Expr basis = assertInstanceOf(Expr.Afgerond.class, w).basis();
        Expr.Binair b = assertInstanceOf(Expr.Binair.class, basis);
        assertEquals(Expr.BinOp.PERCENTAGE_VAN, b.op());
        assertEquals("percentage havengeld", assertInstanceOf(Expr.ParameterRef.class, b.links()).parameter().naam());
        return b;
    }

    /** The read of the taxatiebedrag, one step from the Vondst over the role taxatie. */
    private static void taxatiebedragViaDeTaxatie(Expr y) {
        Expr.AttribuutRef a = assertInstanceOf(Expr.AttribuutRef.class, y);
        assertEquals("taxatiebedrag", a.attribuut());
        assertEquals("Taxatie", a.objecttype().naam);
        assertEquals(List.of("taxatie"), a.navigatiePad());
    }

    // ---- the four forms that did not resolve

    @Test void a_een_genavigeerde_grondslag() {
        // before: ERROR "genavigeerde lezing '…': het lid 'percentage havengeld van het taxatiebedrag' bestaat niet op
        // 'Taxatie', de vuller van de rol 'taxatie', en op geen ander objecttype: …"
        taxatiebedragViaDeTaxatie(percentageVan(waarde(schoon(havengeld(
                "het percentage havengeld van het taxatiebedrag van de taxatie van de Vondst")))).rechts());
    }

    @Test void b_een_grondslag_tussen_haakjes() {
        // before: ERROR "syntaxfout: no viable alternative at input 'hetpercentagehavengeldvan('"
        taxatiebedragViaDeTaxatie(percentageVan(waarde(schoon(havengeld(
                "het percentage havengeld van (het taxatiebedrag van de taxatie van de Vondst)")))).rechts());
    }

    @Test void d_een_grondslag_uit_daarbij_geldt() {
        // before: ERROR "lezing 'het percentage havengeld van het getaxeerde bedrag': 'het getaxeerde bedrag' noemt
        // vanaf 'Vondst' geen rol en geen objecttype: …"
        RegelSpraakModel m = schoon(MODEL + """
            Regel havengeld
                geldig altijd
                    Het havengeld van een Vondst moet berekend worden als (het percentage havengeld van het getaxeerde bedrag) naar beneden afgerond op 2 decimalen.
                    Daarbij geldt:
                        het getaxeerde bedrag is het taxatiebedrag van de taxatie van de Vondst.
            """);
        Expr w = waarde(m);
        Expr.Let let = assertInstanceOf(Expr.Let.class, w);
        assertEquals("getaxeerde bedrag", let.binder());
        taxatiebedragViaDeTaxatie(let.value());
        assertEquals("getaxeerde bedrag", assertInstanceOf(Expr.BinderRef.class, percentageVan(w).rechts()).naam());
    }

    @Test void h_het_procentteken_na_de_parameter() {
        // before: ERROR "genavigeerde lezing '…': het lid 'percentage havengeld % van het taxatiebedrag' bestaat niet …"
        taxatiebedragViaDeTaxatie(percentageVan(waarde(schoon(havengeld(
                "het percentage havengeld % van het taxatiebedrag van de taxatie van de Vondst")))).rechts());
    }

    @Test void h_het_procentteken_na_de_parameter_voor_een_eigen_lid() {
        // before: ERROR "lezing 'het percentage havengeld % van zijn getaxeerde waarde': 'zijn getaxeerde waarde'
        // noemt vanaf 'Vondst' geen rol en geen objecttype: …"
        Expr y = percentageVan(waarde(schoon(havengeld("het percentage havengeld % van zijn getaxeerde waarde")))).rechts();
        Expr.AttribuutRef a = assertInstanceOf(Expr.AttribuutRef.class, y);
        assertEquals("getaxeerde waarde", a.attribuut());
        assertEquals("Vondst", a.objecttype().naam);
    }

    // ---- the forms that already resolved, unchanged

    @Test void e_een_eigen_lid_blijft_de_grondslag() {
        Expr.AttribuutRef a = assertInstanceOf(Expr.AttribuutRef.class,
                percentageVan(waarde(schoon(havengeld("het percentage havengeld van zijn getaxeerde waarde")))).rechts());
        assertEquals("getaxeerde waarde", a.attribuut());
        assertEquals(List.of(), a.navigatiePad());
    }

    @Test void j_een_eigen_lid_met_de_onderwerpnaam_blijft_de_grondslag() {
        Expr.AttribuutRef a = assertInstanceOf(Expr.AttribuutRef.class, percentageVan(waarde(schoon(
                havengeld("het percentage havengeld van de getaxeerde waarde van de Vondst")))).rechts());
        assertEquals("getaxeerde waarde", a.attribuut());
        assertEquals("Vondst", a.objecttype().naam);
    }

    @Test void i_een_letterlijk_percentage_van_een_genavigeerde_grondslag_blijft() {
        Expr w = waarde(schoon(havengeld("12,5 % van het taxatiebedrag van de taxatie van de Vondst")));
        Expr.Binair b = assertInstanceOf(Expr.Binair.class, assertInstanceOf(Expr.Afgerond.class, w).basis());
        assertEquals(Expr.BinOp.PERCENTAGE_VAN, b.op());
        taxatiebedragViaDeTaxatie(b.rechts());
    }

    @Test void een_periode_tussen_haakjes_blijft_een_periode() {
        // "van (A) tot (B)" after a value is the period comparison of §13.4.15.68, as before the parenthesised
        // percentage form: that form is primaireExpressie's last alternative, so the reading it had wins
        RegelSpraakModel m = schoon(MODEL.replace("het havengeld Numeriek (getal met 2 decimalen);",
                "het havengeld Numeriek (getal met 2 decimalen);\n    de vinddag Datum in dagen;\n    de meldingsdag Datum in dagen;")
                + "Regel havengeld\n    geldig altijd\n        Het havengeld van een Vondst moet gesteld worden op 5,00"
                + " van (de vinddag van de Vondst) tot (de meldingsdag van de Vondst).\n");
        assertTrue(dev.uchar.regelspraak.emit.IrEmitter.emit(m).contains("\"node\": \"TimeWindow\""));
    }

    @Test void het_procentteken_voor_haakjes_blijft_de_vorm_met_procentteken() {
        // "P % van (Y)" and "12,5 % van (Y)" read as the '%' form, as before: the name does not take the sign
        assertInstanceOf(Expr.AttribuutRef.class, percentageVan(waarde(schoon(havengeld(
                "het percentage havengeld % van (zijn getaxeerde waarde)")))).rechts());
        Expr w = waarde(schoon(havengeld("12,5 % van (zijn getaxeerde waarde)")));
        Expr.Binair b = assertInstanceOf(Expr.Binair.class, assertInstanceOf(Expr.Afgerond.class, w).basis());
        assertEquals(Expr.BinOp.PERCENTAGE_VAN, b.op());
        assertInstanceOf(Expr.Literal.class, b.links());
    }

    @Test void haakjes_na_iets_anders_dan_een_percentage_weigeren() {
        // before: a syntax error ("no viable alternative at input 'van(zijngetaxeerdewaarde)'")
        RegelSpraakModel m = Resolver.resolve(MODEL.replace("Objecttype de Vondst",
                "Parameter de korting : Numeriek (getal met 2 decimalen)\n\nObjecttype de Vondst")
                + "Regel havengeld\n    geldig altijd\n        Het havengeld van een Vondst moet berekend worden als "
                + "de korting van (zijn getaxeerde waarde).\n");
        assertEquals(List.of("'de korting van (zijn getaxeerde waarde)' leest als de percentagefunctie zonder "
                + "procentteken (§13.4.17.12), maar 'de korting' is geen parameter of variabele van het type Percentage "
                + "— schrijf het procentteken ('… % van …') of noem een percentage"),
                m.errors().stream().map(RegelSpraakModel.Diagnostic::message).toList());
    }

    @Test void het_product_met_een_percentage_blijft_een_typefout() {
        // the product is a typing question (§4.4 lists no Percentage row), not a navigation: refused as before
        RegelSpraakModel m = Resolver.resolve(havengeld("zijn getaxeerde waarde maal het percentage havengeld"));
        assertEquals(List.of("maal vereist numerieke operanden, kreeg Numeriek en Percentage"),
                m.errors().stream().map(RegelSpraakModel.Diagnostic::message).toList());
    }
}
