package dev.uchar.regelspraak;

import dev.uchar.regelspraak.emit.IrEmitter;
import dev.uchar.regelspraak.ir.Condition;
import dev.uchar.regelspraak.ir.Default;
import dev.uchar.regelspraak.ir.Expr;
import dev.uchar.regelspraak.ir.PrioritizedDefaults;
import dev.uchar.regelspraak.ir.ResolvedConsistency;
import dev.uchar.regelspraak.ir.ResolvedRule;
import dev.uchar.regelspraak.model.Datatype;
import dev.uchar.regelspraak.model.RegelSpraakModel;
import dev.uchar.regelspraak.model.RegelSpraakModel.ObjectType;
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
class InputAmountTest {

    static final String VONDST_AMOUNT = """
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

    private static ResolvedRule rule(RegelSpraakModel m, String name) {
        return m.resolvedRules.stream().filter(r -> r.name().equals(name)).findFirst().orElseThrow();
    }

    private static String read(Expr e) { return assertInstanceOf(Expr.AttributeReference.class, e).attribute(); }

    @Test void on_an_amount_with_unit_the_marking_builds_the_same_five_pieces() {
        // before: ERROR "attribuut 'waarde' van Vondst: '(mag opgegeven worden)' staat alleen op een Numeriek attribuut
        // zonder eenheid of een Datum in dagen, en 'waarde' is Numeriek met eenheid € — …"
        RegelSpraakModel m = Resolver.resolve(VONDST_AMOUNT);
        assertTrue(m.diagnostics.isEmpty(), () -> "onverwachte diagnostiek: " + m.diagnostics);
        ObjectType v = m.objectTypes.get("Vondst");

        // 1 and 2: the two attributes right after X, each with X's datatype, the unit included
        assertEquals(List.of("taxatie", "bergingskosten", "waarde", "opgegeven waarde", "berekende waarde",
                "verzekerde waarde"), List.copyOf(v.attributes.keySet()));
        Datatype x = v.attributes.get("waarde");
        assertEquals("EUR", x.unitOf().abbreviation());
        assertEquals(x, v.attributes.get("opgegeven waarde"));
        assertEquals(x, v.attributes.get("berekende waarde"));

        // 2: the computation, an addition in the same unit, keeps its name and targets berekende X
        ResolvedRule w = rule(m, "waarde");
        assertEquals("berekende waarde", read(w.target()));
        assertEquals(Expr.BinOp.PLUS, assertInstanceOf(Expr.Binary.class, w.value()).op());

        // 3: the opgave, the computation, and the initialisation with its amount as the lowest default
        PrioritizedDefaults g = m.prioritizedDefaults.stream()
                .filter(p -> p.name().equals("opgave boven berekening waarde")).findFirst().orElseThrow();
        assertEquals(3, g.defaults().size());
        assertEquals(List.of("opgegeven waarde", "berekende waarde"),
                g.defaults().subList(0, 2).stream().map(d -> read(d.consequence())).toList(), "van hoog naar laag");
        Default init = g.defaults().get(2);
        assertTrue(init.initialisation(), "de initialisatie is de laagste default");
        Expr.Literal amount = assertInstanceOf(Expr.Literal.class, init.consequence());
        assertEquals("25,00", amount.valueText());
        assertEquals("EUR", amount.type().unitOf().abbreviation());

        // 4 and 5: the characteristic compares the two amounts, the consistency asserts it absent
        Condition.Compound all = assertInstanceOf(Condition.Compound.class,
                rule(m, "afwijking opgave waarde").condition());
        Condition.Comparison notEqual = assertInstanceOf(Condition.Comparison.class, all.components().get(2));
        assertEquals(Condition.Operator.ONGELIJK, notEqual.op());
        assertEquals(List.of("controle opgave waarde"),
                m.consistencies.stream().map(ResolvedConsistency::name).toList());
        assertEquals(List.of("waarde", "controle bij afwijkende waarde", "verzekerde waarde", "afwijking opgave waarde"),
                m.resolvedRules.stream().map(ResolvedRule::name).toList(),
                "de initialisatieregel is opgenomen in de groep en staat niet meer als regel");
    }

    @Test void on_an_amount_the_wire_carries_the_unit() {
        RegelSpraakModel m = Resolver.resolve(VONDST_AMOUNT);
        assertTrue(m.diagnostics.isEmpty(), () -> "onverwachte diagnostiek: " + m.diagnostics);
        String ir = IrEmitter.emit(m);
        Report.assertAccountedFor(ir);
        // the generated attributes write the datatype of X, unit included, as the declaration of X does
        java.util.Map<String, Object> dataTypes = new java.util.LinkedHashMap<>();
        for (Object t : (List<?>) ((java.util.Map<?, ?>) ((java.util.Map<?, ?>) Report.json(ir)).get("model")).get("objectTypes"))
            for (Object a : (List<?>) ((java.util.Map<?, ?>) t).get("attributes"))
                dataTypes.put(String.valueOf(((java.util.Map<?, ?>) a).get("name")), ((java.util.Map<?, ?>) a).get("datatype"));
        assertTrue(String.valueOf(dataTypes.get("waarde")).contains("abbreviation=EUR"), () -> dataTypes.toString());
        assertEquals(dataTypes.get("waarde"), dataTypes.get("opgegeven waarde"));
        assertEquals(dataTypes.get("waarde"), dataTypes.get("berekende waarde"));
        // the initialisation default's consequence is the amount with its unit
        assertTrue(ir.replaceAll("\\s+", "").contains("{\"node\":\"Literal\",\"value\":{\"number\":\"25.00\"},\"type\":"
                + "{\"node\":\"Number\",\"whole\":true,\"decimals\":0,\"sign\":\"unspecified\",\"unit\":{\"node\":\"Unit\","
                + "\"abbreviation\":\"EUR\""), () -> "de initialisatiedefault draagt het bedrag niet:\n" + ir);
    }

    @Test void a_compound_unit_is_refused() {
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
