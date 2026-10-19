package dev.uchar.regelspraak.transpiler.core;

import dev.uchar.regelspraak.ir.Source;
import dev.uchar.regelspraak.ir.Condition;
import dev.uchar.regelspraak.ir.Expr;
import dev.uchar.regelspraak.model.Datatype;
import dev.uchar.regelspraak.model.Unit;
import dev.uchar.regelspraak.model.RegelSpraakModel.ObjectType;
import dev.uchar.regelspraak.model.RegelSpraakModel.Parameter;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * RUNTIME-SEMANTICS ch. 2.10, the consumer clause, at the unit the corpus cannot reach.
 *
 * <p>{@code scripts/check-reftype-derivation.sh} proves the OUTCOME follows the declarations, over the
 * ten corpus goldens that carry a unit-bearing annotation at a twistable position. What it cannot show
 * is the derivation's own shape on forms the corpus does not (yet) hold: a reference whose declaration
 * cannot be looked up, a binder read under a SHADOWING inner Let, the three positions where the
 * annotation is the source and must be handed back untouched. Those are pinned here.
 *
 * <p>Each model is authored in this file (no corpus file is read or touched). Every annotation below
 * LIES about its unit relative to the declaration behind it — that is the point: on a verifier-clean
 * document (WF-091) the two agree, so only a lying model can tell a derivation from a repetition.
 */
class EffectiveUnitWitnessTest {

    // "Gewicht": kg is canonical (factor 1); t reduces to kg with factor 1000 — inter-convertible, so a
    // consumer that trusts a lying annotation restates by 1000 instead of refusing (the measured
    // 3,75 -> 3.750,00 shape).
    private static final Unit KG = new Unit("kg", "Gewicht", "kg", BigDecimal.ONE);
    private static final Unit TON = new Unit("t", "Gewicht", "kg", new BigDecimal("1000"));
    // "Geld" and "Tijd", for the §4.4 product rules: € per jaar is a RATE (a numerator over a denominator).
    private static final Unit EUR = new Unit("EUR", "Geld", "EUR", BigDecimal.ONE);
    private static final Unit JR = new Unit("jr", "Tijd", "mnd", new BigDecimal("12"));
    private static final Unit EUR_PER_JR = Unit.compound(EUR, JR);

    private static Datatype num(Unit e) {
        return new Datatype.Numeric(false, 2, Datatype.Sign.ONBEPAALD, e);
    }

    private static Source source() { return Source.synthetic("witness"); }

    private static EffectiveUnit bare() { return new EffectiveUnit(List.of()); }

    /** A Vondst declaring "gewicht" in kg — the source every reference to it must read. */
    private static ObjectType vondst() {
        ObjectType ot = new ObjectType("Vondst");
        ot.attributes.put("gewicht", num(KG));
        return ot;
    }

    /** A reference to Vondst.gewicht whose ANNOTATION claims tonnes. */
    private static Expr.AttributeReference lyingRef(ObjectType ot) {
        return new Expr.AttributeReference("gewicht", ot, "een vondst", null, num(TON), source());
    }

    /** A Vracht declaring a RATE ("tarief" in €/jr), a plain time ("looptijd" in jr), a plain weight and a
     *  plain amount — the four declarations the §4.4 product rules need. */
    private static ObjectType vracht() {
        ObjectType ot = new ObjectType("Vracht");
        ot.attributes.put("tarief", num(EUR_PER_JR));
        ot.attributes.put("looptijd", num(JR));
        ot.attributes.put("gewicht", num(KG));
        ot.attributes.put("prijs", num(EUR));
        return ot;
    }

    /** A reference to {@code ot.attr} whose ANNOTATION lies (tonnes, always). */
    private static Expr.AttributeReference lyingRef(ObjectType ot, String attr) {
        return new Expr.AttributeReference(attr, ot, "een vracht", null, num(TON), source());
    }

    // ---------------- clause 1: reference positions read the declaration ----------------

    @Test void an_attribute_reference_reads_the_declaration_not_its_own_annotation() {
        assertEquals(KG, bare().unitOf(lyingRef(vondst())),
                "de declaratie (kg) is de bron; de annotatie (t) is haar herhaling");
    }

    @Test void without_a_findable_declaration_the_read_claims_nothing() {
        // The resolver left the objecttype unbound, or the type declares no member of that name (a count
        // source names a role): there IS no declaration to consult. Since L2 of the v1 wire migration a
        // reference carries no annotation either — the record's type slot is never filled from the wire — so
        // the derivation claims no unit and no datatype rather than reading the slot.
        Expr.AttributeReference loose = new Expr.AttributeReference("gewicht", null, "iets", null, num(TON), source());
        assertNull(bare().unitOf(loose));
        assertTrue(bare().van(loose).isUnknown(), "geen declaratie, dus geen datatype");
        ObjectType empty = new ObjectType("Vondst");   // declares no member of that name
        assertNull(bare().unitOf(new Expr.AttributeReference("gewicht", empty, "een vondst", null, num(TON), source())));
    }

    @Test void a_parameterref_reads_the_parameter_declaration() {
        Parameter p = new Parameter("DREMPEL", num(KG));
        // the ParameterRef record derives its own type() from the parameter, so the wire annotation is
        // already unreachable there — this pins that the derivation agrees rather than diverging.
        assertEquals(KG, bare().unitOf(new Expr.ParameterRef(p, source())));
    }

    @Test void a_dimension_cell_reads_the_declaration_of_its_within_reference() {
        Expr.DimensionCellRead cell = new Expr.DimensionCellRead(lyingRef(vondst()), List.of("noord"), source());
        assertEquals(KG, bare().unitOf(cell));
    }

    // ---------------- clause 2: composite positions compute the effective type ----------------

    @Test void an_additive_binary_carries_the_unit_of_its_left_operand() {
        ObjectType ot = vondst();
        Expr.Binary sum = new Expr.Binary(Expr.BinOp.PLUS, lyingRef(ot), lyingRef(ot), num(TON), source());
        assertEquals(KG, bare().unitOf(sum), "ch. 2.6: de som draagt de eenheid van links, niet haar eigen claim");
    }

    @Test void a_unitless_side_takes_the_other_over() {
        ObjectType ot = vondst();
        Expr.Literal number = new Expr.Literal("2", num(null), source());
        Expr.Binary prod = new Expr.Binary(Expr.BinOp.MAAL, number, lyingRef(ot), num(TON), source());
        assertEquals(KG, bare().unitOf(prod), "ch. 2.6: het factorvrije product draagt de eenheid door");
    }

    @Test void a_times_with_two_units_falls_away_against_its_denominator() {
        // §4.4 / ch. 2.6, the cancel clause the frontend's Typing.compoundUnit spells: a RATE times its
        // (convertible) denominator unit is the numerator — "€/jr maal jr = €". Reading left-then-right here (the
        // additive rule) would answer €/jr, a unit the result does NOT have.
        ObjectType ot = vracht();
        assertEquals(EUR, bare().unitOf(new Expr.Binary(Expr.BinOp.MAAL,
                lyingRef(ot, "tarief"), lyingRef(ot, "looptijd"), num(TON), source())));
        assertEquals(EUR, bare().unitOf(new Expr.Binary(Expr.BinOp.MAAL,
                lyingRef(ot, "looptijd"), lyingRef(ot, "tarief"), num(TON), source())),
                "de cancel-clausule is symmetrisch (jr maal €/jr = €)");
    }

    @Test void a_times_of_two_separate_units_gives_the_opaque_product() {
        // §4.4: two plain units multiply to an OPAQUE product ("€·kg") — no canonical, so it restates against an
        // IDENTICAL product and nothing else. That opacity is this backend's only protection against a two-unit
        // product silently rescaling into a €-target (DrlTranspiler's four "an OPAQUE product unit stays an honest
        // marker" sites); handing back the LEFT unit would hand that protection away.
        ObjectType ot = vracht();
        Unit out = bare().unitOf(new Expr.Binary(Expr.BinOp.MAAL,
                lyingRef(ot, "prijs"), lyingRef(ot, "gewicht"), num(TON), source()));
        assertEquals(Unit.product(EUR, KG), out);
        assertNull(out.canonical(), "een opaak product draagt geen canoniek");
        assertNull(out.exactRestateFactors(EUR), "en dus geen exacte herschaalfactoren naar een concrete eenheid");
        assertFalse(out.convertibleTo(EUR), "€·kg herschaalt niet naar €");
        assertFalse(out.convertibleTo(KG), "en niet naar kg");
        assertTrue(out.convertibleTo(Unit.product(KG, EUR)), "alleen een identiek product is converteerbaar");
    }

    @Test void a_value_aggregation_carries_the_unit_of_operand_zero() {
        // §4.6 Eenheid1 — and the exact class the WF-091 card left open: a fold RESULT type has no single
        // declaration behind it, so the verifier cannot cross it and only this derivation can.
        ObjectType ot = vondst();
        for (Expr.FunctionKind kind : List.of(Expr.FunctionKind.SOM, Expr.FunctionKind.TOTAAL,
                Expr.FunctionKind.MINIMUM, Expr.FunctionKind.MAXIMUM,
                Expr.FunctionKind.EERSTE, Expr.FunctionKind.LAATSTE)) {
            Expr.FunctionCall f = new Expr.FunctionCall(kind, List.of(lyingRef(ot)), ot, null, null, num(TON), source());
            assertEquals(KG, bare().unitOf(f), kind + " draagt de eenheid van operand 0");
        }
    }

    @Test void rounding_and_clamp_carry_the_unit_of_their_base() {
        ObjectType ot = vondst();
        assertEquals(KG, bare().unitOf(
                new Expr.Rounding(lyingRef(ot), Expr.RoundingMode.REKENKUNDIG, 2, source())));
        assertEquals(KG, bare().unitOf(
                new Expr.Clamp(lyingRef(ot), null, null, source())));
    }

    // ---------------- the ch. 4.10 Let cluster ----------------
    // All three are ORDINARY Expr union nodes, so a test builds them directly; the derivation needs the
    // Let INDEX beside them for one reason only — a BinderRef asked in isolation carries no route up to its own
    // binding (see EffectiveUnit#lets).

    /** The derivation as the loader and {@code TmsTranspiler} build it, over the given Lets. */
    private static EffectiveUnit overLets(Expr... lets) {
        return new EffectiveUnit(List.of(lets));
    }

    @Test void a_binder_read_deep_in_the_body_reads_the_bound_value() {
        // Let g = <Vondst.gewicht, annotated t> in (<read g, annotated t> plus <read g, annotated t>)
        ObjectType ot = vondst();
        // two DISTINCT reads that are record-EQUAL: the table is identity-keyed, so both must be recorded
        Expr read1 = new Expr.BinderRef("g", num(TON), source());
        Expr read2 = new Expr.BinderRef("g", num(TON), source());
        Expr.Binary body = new Expr.Binary(Expr.BinOp.PLUS, read1, read2, num(TON), source());
        Expr let = new Expr.Let("g", lyingRef(ot), body, num(TON), source());
        EffectiveUnit eff = overLets(let);

        assertEquals(KG, eff.unitOf(let), "de Let draagt haar body (WF-076)");
        // the boundary the harness caught: a restatement site INSIDE the body asks about a read with no
        // binding in hand, so the derivation must have recorded it (identity table over the Let index).
        assertEquals(KG, eff.unitOf(read1), "een losse binderlezing leest haar binding");
        assertEquals(KG, eff.unitOf(read2));
    }

    @Test void an_innermost_binder_overshadows_the_outermost() {
        // Let g = <gewicht in kg> in (Let g = <literal in t> in <read g>) — WF-064: the NEAREST binder wins,
        // so the read is tonnes here and the outer kg must not leak in.
        ObjectType ot = vondst();
        Expr read = new Expr.BinderRef("g", num(KG), source());
        Expr.Literal inTonnes = new Expr.Literal("2", num(TON), source());
        Expr inside = new Expr.Let("g", inTonnes, read, num(KG), source());
        Expr outside = new Expr.Let("g", lyingRef(ot), inside, num(KG), source());
        // both, in the loader's own (post-)order: the inner Let is built before the outer one wraps it
        EffectiveUnit eff = overLets(inside, outside);

        assertEquals(TON, eff.unitOf(read), "de nabijste binder (t) wint van de buitenste (kg)");
        assertEquals(TON, eff.unitOf(inside));
        assertEquals(TON, eff.unitOf(outside));
    }

    @Test void a_binder_on_a_unitless_value_reads_unitless() {
        // "Gebonden aan een eenheidloze waarde" and "ongebonden" are two different facts and must stay apart: the
        // first HAS a source (a unitless one) and the annotation may not speak over it; only the second falls back.
        // Let n = <2, unitless> in <read n, annotated t> — both the read AND the Let lie about tonnes.
        Expr.Literal number = new Expr.Literal("2", num(null), source());
        Expr read = new Expr.BinderRef("n", num(TON), source());
        Expr let = new Expr.Let("n", number, read, num(TON), source());
        EffectiveUnit eff = overLets(let);

        assertNull(eff.unitOf(read), "de binding is eenheidloos; de annotatie (t) is haar leugen");
        assertNull(eff.unitOf(let), "en de Let draagt haar body");
        assertNull(eff.van(read).unitOf(), "ook op de datatype-route (van) blijft de eenheid weg");
    }

    @Test void an_unbound_binder_read_claims_nothing() {
        // No Let of that name anywhere: there is nothing to derive from, and since L2 the read carries no
        // annotation of its own, so it claims no unit — the same answer as an AttributeReference without a declaration.
        Expr read = new Expr.BinderRef("onbekend", num(TON), source());
        assertNull(overLets().unitOf(read));
        assertTrue(overLets().van(read).isUnknown(), "geen binding, dus geen datatype");
    }

    @Test void a_choice_reads_the_branch_not_its_own_claim() {
        ObjectType ot = vondst();
        Expr choice = new Expr.IfThenElse(null, lyingRef(ot), lyingRef(ot), num(TON), source());
        assertEquals(KG, overLets().unitOf(choice));
    }

    // WF-064 inside a ch. 10.2 GUARD. The binder-read table is built by walking each Let with a scope, and a
    // guard's operands are reached through the shared Conditie walk. That walk is a FULL pre-order scan, so it
    // used to hand back every node of every operand — the operand AND its descendants — and the table walk then
    // re-entered each subtree MIDWAY, with the enclosing scope instead of the one the node actually sits in. For
    // a shadowing inner Let that is not extra work but a WRONG ANSWER: the correct recording (t) is made first
    // through the Let, and the mid-tree re-entry overwrites it with the outer binding (kg). The two shapes below
    // are the two ways an operand can hold that Let; both must read t.

    @Test void a_shadowing_let_in_a_guard_operand_keeps_its_own_binding() {
        // vorm A — the shadowing Let sits DIRECTLY in the guard:
        //   Let g = <2 kg> in (als (Let g = <2 t> in <read g>) groter dan <2 kg> dan … anders …)
        // WF-064: the read sits under the INNER binder, so it is tonnes. Measured kg before the repair.
        Expr read = new Expr.BinderRef("g", num(KG), source());
        Expr inside = new Expr.Let("g", number(TON), read, num(KG), source());
        Condition guard = new Condition.Comparison(Condition.Operator.GROTER, inside, number(KG), source());
        Expr choice = new Expr.IfThenElse(guard, number(KG), number(KG), num(KG), source());
        Expr outside = new Expr.Let("g", number(KG), choice, num(KG), source());

        assertEquals(TON, overLets(inside, outside).unitOf(read),
                "de nabijste binder (t) wint, ook wanneer haar Let in een ch. 10.2-guard staat");
    }

    @Test void a_shadowing_let_under_a_nested_choice_in_a_guard_keeps_its_binding() {
        // vorm B — the same Let one level deeper, in the DAN-branch of a nested IfThenElse that is itself the
        // guard operand. Before the union landing this shape read t by accident: the nested node was an opaque
        // Onopgelost carrier, so the pre-order scan could not reach into it and there was nothing to re-enter.
        // Making it a union node exposed the same break here — the one measured behaviour difference of the
        // union card, repaired at its root rather than pinned as a divergence.
        Expr read = new Expr.BinderRef("g", num(KG), source());
        Expr inside = new Expr.Let("g", number(TON), read, num(KG), source());
        Condition withinGuard = new Condition.Comparison(Condition.Operator.GROTER, number(KG), number(KG), source());
        Expr nestedChoice = new Expr.IfThenElse(withinGuard, inside, number(KG), num(KG), source());
        Condition guard = new Condition.Comparison(Condition.Operator.GROTER, nestedChoice, number(KG), source());
        Expr choice = new Expr.IfThenElse(guard, number(KG), number(KG), num(KG), source());
        Expr outside = new Expr.Let("g", number(KG), choice, num(KG), source());

        assertEquals(TON, overLets(inside, outside).unitOf(read),
                "ook onder een geneste keuze in de guard wint de nabijste binder (t)");
    }

    /** A unit-bearing literal — the value a shadowing binder binds to. */
    private static Expr number(Unit e) { return new Expr.Literal("2", num(e), source()); }

    // ---------------- the three reason-carrying exceptions ----------------

    @Test void a_literal_keeps_its_own_type() {
        // ch. 2.10 exception 1: the lexeme "3,5 t" IS three-and-a-half tonnes; there is no declaration
        // behind it to be a copy of.
        Expr.Literal lit = new Expr.Literal("3,5", num(TON), source());
        assertEquals(TON, bare().unitOf(lit));
    }

    @Test void a_duration_keeps_its_own_unit() {
        // ch. 2.10 exception 3 (WF-038): "de tijdsduur van A tot B in jaren" — the unit is the OPERATOR's
        // parameter. Deriving it from operand 0 (a date) would erase the operator's own statement.
        Unit year = new Unit("jr", "Tijd", "mnd", new BigDecimal("12"));
        Expr.Literal date = new Expr.Literal("2026-01-01", new Datatype.DateInDays(), source());
        for (Expr.FunctionKind kind : List.of(Expr.FunctionKind.TIJDSDUUR, Expr.FunctionKind.ABSOLUTE_TIJDSDUUR)) {
            Expr.FunctionCall f = new Expr.FunctionCall(kind, List.of(date, date), null, null, null, num(year), source());
            assertEquals(year, bare().unitOf(f), kind + " draagt haar eigen 'in <eenheid>'");
        }
    }

    // (exception 2 — the duration operand of a date +/- duration — is a property of the CALLERS, which
    //  never route that operand through this class; the two sites carry an ANNOTATION BY DESIGN comment
    //  and the harness leaves those positions untwisted. There is nothing here to assert about it.)

    // ---------------- shape preservation ----------------

    @Test void a_non_numeric_type_comes_back_unchanged() {
        Expr.Literal text = new Expr.Literal("aap", new Datatype.Text(), source());
        assertSame(text.type(), bare().van(text));
        assertNull(bare().unitOf(text));
        assertNull(bare().van(null));
    }

    @Test void only_the_unit_is_grafted_the_rest_of_the_annotation_stays() {
        // The derivation replaces exactly one facet of a derived position's annotation. Soort and precision
        // are equally repetitions, but this backend reads neither for a restatement (it reads
        // Rounding.decimals, a NODE field), so grafting more than the unit would move behaviour the clause
        // does not ask to move. (A reference has no annotation to graft onto since L2: its datatype is its
        // declaration's, whole.)
        ObjectType ot = vondst();
        Expr.Binary sum = new Expr.Binary(Expr.BinOp.PLUS, lyingRef(ot), lyingRef(ot),
                new Datatype.Numeric(true, 7, Datatype.Sign.POSITIEF, TON), source());
        Datatype out = bare().van(sum);
        assertEquals(new Datatype.Numeric(true, 7, Datatype.Sign.POSITIEF, KG), out);
        assertSame(ot.attribute("gewicht"), bare().van(lyingRef(ot)), "de lezing draagt de declaratie zelf");
    }

    @Test void an_annotation_that_already_holds_yields_the_same_object() {
        // The no-op path a verifier-clean document takes: nothing is rebuilt, so a document that obeys
        // WF-091 lowers exactly as it did before this derivation existed.
        ObjectType ot = vondst();
        Expr.AttributeReference read = new Expr.AttributeReference("gewicht", ot, "een vondst", null, null, source());
        Expr.Binary honest = new Expr.Binary(Expr.BinOp.PLUS, read, read, num(KG), source());
        assertSame(honest.type(), bare().van(honest));
    }
}
