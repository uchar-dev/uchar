package dev.uchar.regelspraak;

import dev.uchar.regelspraak.emit.IrEmitter;
import dev.uchar.regelspraak.ir.Conditie;
import dev.uchar.regelspraak.ir.Default;
import dev.uchar.regelspraak.ir.Expr;
import dev.uchar.regelspraak.ir.PrioritizedDefaults;
import dev.uchar.regelspraak.ir.ResolvedConsistentie;
import dev.uchar.regelspraak.ir.ResolvedRegel;
import dev.uchar.regelspraak.model.Datatype;
import dev.uchar.regelspraak.model.RegelSpraakModel;
import dev.uchar.regelspraak.model.RegelSpraakModel.Objecttype;
import dev.uchar.regelspraak.resolver.Resolver;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The opgave marker on a number with one unit (grammar/DIVERGENCES.md §7): "de waarde Numeriek (getal met 2
 * decimalen) met eenheid € (mag opgegeven worden)". Until 2026-10-02 the marker stood on a number without a unit and a
 * date in days only, and a number with a unit was refused as a family no golden had measured in the three executors;
 * the hand-written lowering in € played there before the frontend admitted the marker, and the golden
 * jutterswet-opgave-bedrag measures the marker itself. The lowering is unchanged: the two generated attributes carry
 * X's datatype, unit included, and an initialisation with an amount is the group's lowest default. The opgave and the
 * computation share that one unit: a writer or an input in another unit of the same system is refused by the typing
 * of an assignment, as without the marker.
 */
class OpgaveBedragTest {

    static final String VONDST_BEDRAG = """
        Eenheidsysteem Valuta
            de euro (mv: euros) EUR €

        Objecttype de Vondst (mv: Vondsten)
            de taxatie Numeriek (getal met 2 decimalen) met eenheid €;
            de bergingskosten Numeriek (getal met 2 decimalen) met eenheid €;
            de waarde Numeriek (getal met 2 decimalen) met eenheid € (mag opgegeven worden);
            de verzekerde waarde Numeriek (getal met 2 decimalen) met eenheid €;
            is ter controle kenmerk (bijvoeglijk);

        // Bron: art. 3k lid 1
        Regel waarde
            geldig altijd
                De waarde van een Vondst moet berekend worden als zijn taxatie plus zijn bergingskosten
                indien zijn taxatie gevuld is.

        // Bron: art. 3k lid 2
        Regel waarde bij onbekende taxatie
            geldig altijd
                De waarde van een Vondst moet geïnitialiseerd worden op 25,00 €.

        // Bron: art. 3k lid 4
        Regel controle bij afwijkende waarde
            geldig altijd
                Een Vondst is ter controle
                indien hij afwijkende opgave waarde heeft.

        // Bron: art. 3k lid 5
        Regel verzekerde waarde
            geldig altijd
                De verzekerde waarde van een Vondst moet berekend worden als zijn waarde plus 2,50 €.
        """;

    private static ResolvedRegel regel(RegelSpraakModel m, String naam) {
        return m.resolvedRegels.stream().filter(r -> r.naam().equals(naam)).findFirst().orElseThrow();
    }

    private static String gelezen(Expr e) { return assertInstanceOf(Expr.AttribuutRef.class, e).attribuut(); }

    @Test void op_een_bedrag_met_eenheid_bouwt_de_markering_dezelfde_vijf_stukken() {
        // before: ERROR "attribuut 'waarde' van Vondst: '(mag opgegeven worden)' staat alleen op een Numeriek attribuut
        // zonder eenheid of een Datum in dagen, en 'waarde' is Numeriek met eenheid € — …"
        RegelSpraakModel m = Resolver.resolve(VONDST_BEDRAG);
        assertTrue(m.diagnostics.isEmpty(), () -> "onverwachte diagnostiek: " + m.diagnostics);
        Objecttype v = m.objecttypen.get("Vondst");

        // 1 and 2: the two attributes right after X, each with X's datatype, the unit included
        assertEquals(List.of("taxatie", "bergingskosten", "waarde", "opgegeven waarde", "berekende waarde",
                "verzekerde waarde"), List.copyOf(v.attributen.keySet()));
        Datatype x = v.attributen.get("waarde");
        assertEquals("€", x.eenheidOf().afkorting());
        assertEquals(x, v.attributen.get("opgegeven waarde"));
        assertEquals(x, v.attributen.get("berekende waarde"));

        // 2: the computation, an addition in the same unit, keeps its name and targets berekende X
        ResolvedRegel w = regel(m, "waarde");
        assertEquals("berekende waarde", gelezen(w.doel()));
        assertEquals(Expr.BinOp.PLUS, assertInstanceOf(Expr.Binair.class, w.waarde()).op());

        // 3: the opgave, the computation, and the initialisation with its amount as the lowest default
        PrioritizedDefaults g = m.prioritizedDefaults.stream()
                .filter(p -> p.naam().equals("opgave boven berekening waarde")).findFirst().orElseThrow();
        assertEquals(3, g.defaults().size());
        assertEquals(List.of("opgegeven waarde", "berekende waarde"),
                g.defaults().subList(0, 2).stream().map(d -> gelezen(d.consequentie())).toList(), "van hoog naar laag");
        Default init = g.defaults().get(2);
        assertTrue(init.initialisatie(), "de initialisatie is de laagste default");
        Expr.Literal bedrag = assertInstanceOf(Expr.Literal.class, init.consequentie());
        assertEquals("25,00", bedrag.waarde());
        assertEquals("€", bedrag.type().eenheidOf().afkorting());

        // 4 and 5: the characteristic compares the two amounts, the consistency asserts it absent
        Conditie.Samengesteld alle = assertInstanceOf(Conditie.Samengesteld.class,
                regel(m, "afwijking opgave waarde").voorwaarde());
        Conditie.Vergelijking ongelijk = assertInstanceOf(Conditie.Vergelijking.class, alle.onderdelen().get(2));
        assertEquals(Conditie.Operator.ONGELIJK, ongelijk.op());
        assertEquals(List.of("controle opgave waarde"),
                m.consistenties.stream().map(ResolvedConsistentie::naam).toList());
        assertEquals(List.of("waarde", "controle bij afwijkende waarde", "verzekerde waarde", "afwijking opgave waarde"),
                m.resolvedRegels.stream().map(ResolvedRegel::naam).toList(),
                "de initialisatieregel is opgenomen in de groep en staat niet meer als regel");
    }

    @Test void op_een_bedrag_draagt_de_wire_de_eenheid() {
        RegelSpraakModel m = Resolver.resolve(VONDST_BEDRAG);
        assertTrue(m.diagnostics.isEmpty(), () -> "onverwachte diagnostiek: " + m.diagnostics);
        String ir = IrEmitter.emit(m);
        Verslag.verantwoord(ir);
        // the generated attributes write the datatype of X, unit included, as the declaration of X does
        java.util.Map<String, Object> datatypen = new java.util.LinkedHashMap<>();
        for (Object t : (List<?>) ((java.util.Map<?, ?>) ((java.util.Map<?, ?>) Verslag.json(ir)).get("model")).get("objectTypes"))
            for (Object a : (List<?>) ((java.util.Map<?, ?>) t).get("attributes"))
                datatypen.put(String.valueOf(((java.util.Map<?, ?>) a).get("name")), ((java.util.Map<?, ?>) a).get("datatype"));
        assertTrue(String.valueOf(datatypen.get("waarde")).contains("abbreviation=€"), () -> datatypen.toString());
        assertEquals(datatypen.get("waarde"), datatypen.get("opgegeven waarde"));
        assertEquals(datatypen.get("waarde"), datatypen.get("berekende waarde"));
        assertTrue(ir.contains("\"snippet\": \"25,00 €\""), () -> "de initialisatiedefault draagt het bedrag niet:\n" + ir);
    }

    @Test void een_samengestelde_eenheid_weigert() {
        RegelSpraakModel m = Resolver.resolve("""
            Eenheidsysteem Munt
                de euro (mv: euros) EUR €

            Objecttype de Vondst (mv: Vondsten)
                de opgave Numeriek (geheel getal) met eenheid €/jr (mag opgegeven worden);
            """);
        assertEquals(List.of("attribuut 'opgave' van Vondst: '(mag opgegeven worden)' staat alleen op een Numeriek "
                + "attribuut zonder eenheid of met één eenheid, of op een Datum in dagen, en 'opgave' is Numeriek met "
                + "eenheid €/jr — een defaults-groep op een attribuut van een ander datatype of met een samengestelde "
                + "eenheid is nog niet in alle drie de uitvoerders gemeten (dialect §7)"),
                m.errors().stream().map(RegelSpraakModel.Diagnostic::message).toList());
    }
}
