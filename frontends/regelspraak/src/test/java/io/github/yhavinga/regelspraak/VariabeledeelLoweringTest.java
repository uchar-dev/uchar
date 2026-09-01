package io.github.yhavinga.regelspraak;

import io.github.yhavinga.regelspraak.emit.IrEmitter;
import io.github.yhavinga.regelspraak.model.RegelSpraakModel;
import io.github.yhavinga.regelspraak.resolver.Resolver;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * §13.4.2.10 the variabelendeel ("Daarbij geldt:") lowered to the IR-SPEC ch. 4.10 {@code Let} cluster.
 *
 * <p>Before this, the resolver INLINED every variable: it substituted the resolved definition subtree at each
 * use site, so sharing was lost and the block never reached the wire. Now a variable the RESULT part reads
 * becomes one {@code Let} carrying that definition once, with a {@code BinderRef} at every read. Two lowering
 * facts follow from the wire's shape rather than from the node, and both are pinned here: a variable read only
 * in the VOORWAARDE keeps the substitution route (a rule's condition and its value are separate wire fields, so
 * no Let can span them), and a variable no read reaches gets no Let at all (under call-by-need it would be a
 * guaranteed no-op).
 *
 * <p>Assertions run over the EMITTED JSON with whitespace collapsed, so they pin the wire shape itself.
 */
class VariabeledeelLoweringTest {

    private static String emitCompact(String src) {
        RegelSpraakModel m = Resolver.resolve(src);
        assertTrue(m.errors().isEmpty(), () -> "onverwachte fouten: " + m.errors());
        return IrEmitter.emit(m).replaceAll("\\s+", "");
    }

    /** How often {@code naald} occurs in {@code hooi}. */
    private static int tel(String hooi, String naald) {
        int n = 0;
        for (int i = hooi.indexOf(naald); i >= 0; i = hooi.indexOf(naald, i + 1)) n++;
        return n;
    }

    @Test void een_variabele_die_het_resultaat_tweemaal_leest_wordt_een_let_met_twee_binderrefs() {
        // The point of the lowering: the definition is carried ONCE and read twice, instead of the
        // definition subtree being duplicated at both use sites (the v0 inlining).
        String src = """
            Objecttype de Berging
                het wraktal Numeriek (geheel getal);
                het spoedtal Numeriek (geheel getal);
                het loon Numeriek (geheel getal);
            Regel loon
                geldig altijd
                    Het loon van een Berging moet gesteld worden op het grondloon plus het grondloon maal het spoedtal van de Berging.
                    Daarbij geldt:
                        het grondloon is het wraktal van de Berging maal 40.
            """;
        String c = emitCompact(src);
        assertEquals(1, tel(c, "\"node\":\"Let\""), () -> "precies één Let verwacht:\n" + c);
        assertTrue(c.contains("\"node\":\"Let\",\"type\":"), () -> "Let-veldvolgorde (node,type,binder,…):\n" + c);
        assertTrue(c.contains("\"binder\":\"grondloon\""), () -> "binder-naam ontbreekt:\n" + c);
        assertEquals(2, tel(c, "\"node\":\"BinderRef\",\"name\":\"grondloon\""),
                () -> "twee lezingen verwacht, geen duplicatie van de definitie:\n" + c);
        // the definition itself occurs once — inside the Let's value, nowhere else.
        assertEquals(1, tel(c, "\"attribuut\":\"wraktal\""), () -> "de definitie is gedupliceerd:\n" + c);
    }

    @Test void een_variabele_die_alleen_de_voorwaarde_leest_krijgt_geen_let_en_blijft_gesubstitueerd() {
        // A rule's `voorwaarde` is a Conditie and its `waarde` an Expr: no Let node spans the two wire
        // fields, so the condition read keeps the v0 substitution. And no DEAD Let is emitted around the
        // value for it — under ch. 10.6 an unread binder evaluates nothing, so it would be pure noise.
        String src = """
            Objecttype de Jutter
                de leeftijd Numeriek (geheel getal);
                is jong kenmerk (bijvoeglijk);
            Regel jong
                geldig altijd
                    Een Jutter is jong
                    indien de grens groter is dan de leeftijd van de Jutter.
                    Daarbij geldt:
                        de grens is 30.
            """;
        String c = emitCompact(src);
        assertFalse(c.contains("\"node\":\"Let\""), () -> "geen Let verwacht om een voorwaarde-variabele:\n" + c);
        assertFalse(c.contains("\"node\":\"BinderRef\""), () -> "geen BinderRef buiten een Let:\n" + c);
        // the definition IS there — substituted into the condition's left operand.
        assertTrue(c.contains("\"op\":\"GROTER\",\"links\":{\"node\":\"Literal\",\"waarde\":\"30\""),
                () -> "de voorwaarde-substitutie ontbreekt:\n" + c);
    }

    @Test void een_variabele_die_niemand_leest_krijgt_geen_dode_let() {
        String src = """
            Objecttype de Jutter
                de rang Numeriek (geheel getal);
            Regel rang
                geldig altijd
                    De rang van een Jutter moet gesteld worden op 7.
                    Daarbij geldt:
                        de ongebruikte is 3.
            """;
        String c = emitCompact(src);
        assertFalse(c.contains("\"node\":\"Let\""), () -> "een nooit gelezen variabele hoort niet op de wire:\n" + c);
    }

    @Test void twee_variabelen_nesten_in_declaratievolgorde_en_de_latere_leest_de_eerdere_als_binder() {
        // §11.1 rule 1: declaration order. The FIRST declared variable is the OUTERMOST Let, so a later
        // definition's BinderRef to an earlier one is in scope (WF-064) and the earlier binder is never
        // referenced inside its own value (WF-076).
        String src = """
            Objecttype de Jutter
                de grondslag Numeriek (geheel getal);
                de heffing Numeriek (geheel getal);
            Regel heffing
                geldig altijd
                    De heffing van een Jutter moet gesteld worden op het totaal plus het totaal.
                    Daarbij geldt:
                        de basis is de grondslag van de Jutter maal 2.
                        het totaal is de basis plus 5.
            """;
        String c = emitCompact(src);
        assertEquals(2, tel(c, "\"node\":\"Let\""), () -> "twee Lets verwacht:\n" + c);
        // outermost = first declared: "basis" opens, and "totaal" sits inside its body.
        int basis = c.indexOf("\"binder\":\"basis\"");
        int totaal = c.indexOf("\"binder\":\"totaal\"");
        assertTrue(basis >= 0 && totaal > basis, () -> "declaratievolgorde: basis moet de buitenste Let zijn:\n" + c);
        assertEquals(1, tel(c, "\"node\":\"BinderRef\",\"name\":\"basis\""),
                () -> "de latere definitie moet de eerdere als binder lezen:\n" + c);
        assertEquals(2, tel(c, "\"node\":\"BinderRef\",\"name\":\"totaal\""), () -> "twee lezingen verwacht:\n" + c);
    }

    @Test void een_variabele_die_beide_delen_leest_wordt_in_de_voorwaarde_gesubstitueerd_en_in_de_waarde_gebonden() {
        // The mixed case: one variable, both routes at once, and the two never interfere — the condition
        // keeps the substituted subtree, the value keeps the Let.
        String src = """
            Objecttype de Jutter
                de grondslag Numeriek (geheel getal);
                de heffing Numeriek (geheel getal);
            Regel heffing
                geldig altijd
                    De heffing van een Jutter moet gesteld worden op de basis plus 1
                    indien de basis groter is dan 10.
                    Daarbij geldt:
                        de basis is de grondslag van de Jutter maal 2.
            """;
        String c = emitCompact(src);
        assertEquals(1, tel(c, "\"node\":\"Let\""), () -> "één Let om de waarde:\n" + c);
        assertEquals(1, tel(c, "\"node\":\"BinderRef\",\"name\":\"basis\""),
                () -> "de waarde-lezing is een BinderRef:\n" + c);
        // the condition carries the SUBSTITUTED definition (a MAAL over the attribute), not a BinderRef.
        assertTrue(c.contains("\"bron\":\"debasisgroterisdan10\""), () -> "de voorwaarde ontbreekt:\n" + c);
        assertEquals(2, tel(c, "\"attribuut\":\"grondslag\""),
                () -> "één definitie in de Let-value, één gesubstitueerd in de voorwaarde:\n" + c);
    }

    @Test void een_variabele_schaduwt_een_gelijknamige_parameter_ook_op_de_binder_route() {
        // §11: a variable "geldt alleen binnen de regel" and shadows a global parameter of the same name.
        // The result read must therefore bind the VARIABLE (a BinderRef), never the parameter.
        String src = """
            Parameter het tarief : Numeriek (geheel getal);
            Objecttype de Jutter
                de heffing Numeriek (geheel getal);
            Regel heffing
                geldig altijd
                    De heffing van een Jutter moet gesteld worden op het tarief plus het tarief.
                    Daarbij geldt:
                        het tarief is 3.
            """;
        String c = emitCompact(src);
        assertEquals(2, tel(c, "\"node\":\"BinderRef\",\"name\":\"tarief\""),
                () -> "de variabele moet de parameter schaduwen:\n" + c);
        assertFalse(c.contains("\"node\":\"ParameterRef\",\"parameter\":\"tarief\""),
                () -> "geen parameterlezing binnen de regel:\n" + c);
    }

    @Test void de_let_draagt_het_bodytype_wf076() {
        // WF-076: the Let's type IS its body's type. Here the body is a Datum-valued read, so the Let must
        // carry DatumInDagen — not the (Numeriek) type of its own value.
        String src = """
            Objecttype de Vondst
                de vonddatum Datum in dagen;
                het jaartal Numeriek (geheel getal);
            Regel jaartal
                geldig altijd
                    Het jaartal van een Vondst moet gesteld worden op (het jaar uit (de datum)).
                    Daarbij geldt:
                        de datum is de vonddatum van de Vondst.
            """;
        String c = emitCompact(src);
        // the VALUE is a Datum read, the BODY a Numeriek year extraction: the Let must carry the body's type.
        assertTrue(c.contains("\"binder\":\"datum\",\"value\":{\"node\":\"AttribuutRef\",\"attribuut\":\"vonddatum\""),
                () -> "de Let-value is de datum-lezing:\n" + c);
        assertTrue(c.contains("\"node\":\"Let\",\"type\":{\"node\":\"Numeriek\",\"geheel\":true"),
                () -> "WF-076: het Let-type is het body-type (Numeriek), niet dat van zijn value (Datum):\n" + c);
    }

    @Test void lidwoordvarianten_van_een_verwijzing_binden_op_een_binder() {
        // §11 "Mogelijkheid van lidwoord bij naam variabele": "het grondloon" and the bare "grondloon"
        // are ONE variable, so both reads carry the same binder name (the declared name minus the article).
        String src = """
            Objecttype de Berging
                het wraktal Numeriek (geheel getal);
                het loon Numeriek (geheel getal);
            Regel loon
                geldig altijd
                    Het loon van een Berging moet gesteld worden op grondloon plus het grondloon.
                    Daarbij geldt:
                        het grondloon is het wraktal van de Berging maal 40.
            """;
        String c = emitCompact(src);
        assertEquals(1, tel(c, "\"node\":\"Let\""), () -> "één variabele, één Let:\n" + c);
        assertEquals(2, tel(c, "\"node\":\"BinderRef\",\"name\":\"grondloon\""),
                () -> "beide schrijfwijzen binden op dezelfde binder:\n" + c);
    }
}
