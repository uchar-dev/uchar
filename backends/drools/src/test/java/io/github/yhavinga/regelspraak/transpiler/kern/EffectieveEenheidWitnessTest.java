package io.github.yhavinga.regelspraak.transpiler.kern;

import io.github.yhavinga.regelspraak.ir.Bron;
import io.github.yhavinga.regelspraak.ir.Conditie;
import io.github.yhavinga.regelspraak.ir.Expr;
import io.github.yhavinga.regelspraak.model.Datatype;
import io.github.yhavinga.regelspraak.model.Eenheid;
import io.github.yhavinga.regelspraak.model.RegelSpraakModel.Objecttype;
import io.github.yhavinga.regelspraak.model.RegelSpraakModel.Parameter;

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
class EffectieveEenheidWitnessTest {

    // "Gewicht": kg is canonical (factor 1); t reduces to kg with factor 1000 — inter-convertible, so a
    // consumer that trusts a lying annotation restates by 1000 instead of refusing (the measured
    // 3,75 -> 3.750,00 shape).
    private static final Eenheid KG = new Eenheid("kg", "Gewicht", "kg", BigDecimal.ONE);
    private static final Eenheid TON = new Eenheid("t", "Gewicht", "kg", new BigDecimal("1000"));
    // "Geld" and "Tijd", for the §4.4 product rules: € per jaar is a RATE (a numerator over a denominator).
    private static final Eenheid EUR = new Eenheid("EUR", "Geld", "EUR", BigDecimal.ONE);
    private static final Eenheid JR = new Eenheid("jr", "Tijd", "mnd", new BigDecimal("12"));
    private static final Eenheid EUR_PER_JR = Eenheid.samengesteld(EUR, JR);

    private static Datatype num(Eenheid e) {
        return new Datatype.Numeriek(false, 2, Datatype.Teken.ONBEPAALD, e);
    }

    private static Bron bron() { return Bron.synthetisch("witness"); }

    private static EffectieveEenheid kaal() { return new EffectieveEenheid(List.of()); }

    /** A Vondst declaring "gewicht" in kg — the source every reference to it must read. */
    private static Objecttype vondst() {
        Objecttype ot = new Objecttype("Vondst");
        ot.attributen.put("gewicht", num(KG));
        return ot;
    }

    /** A reference to Vondst.gewicht whose ANNOTATION claims tonnes. */
    private static Expr.AttribuutRef liegendeRef(Objecttype ot) {
        return new Expr.AttribuutRef("gewicht", ot, "een vondst", null, num(TON), bron());
    }

    /** A Vracht declaring a RATE ("tarief" in €/jr), a plain time ("looptijd" in jr), a plain weight and a
     *  plain amount — the four declarations the §4.4 product rules need. */
    private static Objecttype vracht() {
        Objecttype ot = new Objecttype("Vracht");
        ot.attributen.put("tarief", num(EUR_PER_JR));
        ot.attributen.put("looptijd", num(JR));
        ot.attributen.put("gewicht", num(KG));
        ot.attributen.put("prijs", num(EUR));
        return ot;
    }

    /** A reference to {@code ot.attr} whose ANNOTATION lies (tonnes, always). */
    private static Expr.AttribuutRef liegendeRef(Objecttype ot, String attr) {
        return new Expr.AttribuutRef(attr, ot, "een vracht", null, num(TON), bron());
    }

    // ---------------- clause 1: reference positions read the declaration ----------------

    @Test void een_attribuutref_leest_de_declaratie_niet_haar_eigen_annotatie() {
        assertEquals(KG, kaal().eenheidVan(liegendeRef(vondst())),
                "de declaratie (kg) is de bron; de annotatie (t) is haar herhaling");
    }

    @Test void zonder_opzoekbare_declaratie_blijft_de_annotatie_staan() {
        // The resolver left the objecttype unbound: there IS no declaration to consult here, and the
        // derivation says so rather than inventing one (a null unit would silently disable every
        // restatement downstream).
        Expr.AttribuutRef losse = new Expr.AttribuutRef("gewicht", null, "iets", null, num(TON), bron());
        assertEquals(TON, kaal().eenheidVan(losse));
        Objecttype leeg = new Objecttype("Vondst");   // declares no member of that name
        assertEquals(TON, kaal().eenheidVan(new Expr.AttribuutRef("gewicht", leeg, "een vondst", null, num(TON), bron())));
    }

    @Test void een_parameterref_leest_de_parameterdeclaratie() {
        Parameter p = new Parameter("DREMPEL", num(KG));
        // the ParameterRef record derives its own type() from the parameter, so the wire annotation is
        // already unreachable there — this pins that the derivation agrees rather than diverging.
        assertEquals(KG, kaal().eenheidVan(new Expr.ParameterRef(p, bron())));
    }

    @Test void een_dimensiecel_leest_de_declaratie_van_haar_binnenreferentie() {
        Expr.DimensieCel cel = new Expr.DimensieCel(liegendeRef(vondst()), List.of("noord"), bron());
        assertEquals(KG, kaal().eenheidVan(cel));
    }

    // ---------------- clause 2: composite positions compute the effective type ----------------

    @Test void een_additieve_binair_draagt_de_eenheid_van_zijn_linkeroperand() {
        Objecttype ot = vondst();
        Expr.Binair som = new Expr.Binair(Expr.BinOp.PLUS, liegendeRef(ot), liegendeRef(ot), num(TON), bron());
        assertEquals(KG, kaal().eenheidVan(som), "ch. 2.6: de som draagt de eenheid van links, niet haar eigen claim");
    }

    @Test void een_eenheidloze_zijde_neemt_de_andere_over() {
        Objecttype ot = vondst();
        Expr.Literal getal = new Expr.Literal("2", num(null), bron());
        Expr.Binair prod = new Expr.Binair(Expr.BinOp.MAAL, getal, liegendeRef(ot), num(TON), bron());
        assertEquals(KG, kaal().eenheidVan(prod), "ch. 2.6: het factorvrije product draagt de eenheid door");
    }

    @Test void een_maal_met_twee_eenheden_valt_weg_tegen_zijn_noemer() {
        // §4.4 / ch. 2.6, the cancel clause the frontend's Typering.samengesteldeEenheid spells: a RATE times its
        // (convertible) denominator unit is the numerator — "€/jr maal jr = €". Reading left-then-right here (the
        // additive rule) would answer €/jr, a unit the result does NOT have.
        Objecttype ot = vracht();
        assertEquals(EUR, kaal().eenheidVan(new Expr.Binair(Expr.BinOp.MAAL,
                liegendeRef(ot, "tarief"), liegendeRef(ot, "looptijd"), num(TON), bron())));
        assertEquals(EUR, kaal().eenheidVan(new Expr.Binair(Expr.BinOp.MAAL,
                liegendeRef(ot, "looptijd"), liegendeRef(ot, "tarief"), num(TON), bron())),
                "de cancel-clausule is symmetrisch (jr maal €/jr = €)");
    }

    @Test void een_maal_van_twee_losse_eenheden_geeft_het_opake_product() {
        // §4.4: two plain units multiply to an OPAQUE product ("€·kg") — no canonical, so it restates against an
        // IDENTICAL product and nothing else. That opacity is this backend's only protection against a two-unit
        // product silently rescaling into a €-target (DrlTranspiler's four "an OPAQUE product unit stays an honest
        // marker" sites); handing back the LEFT unit would hand that protection away.
        Objecttype ot = vracht();
        Eenheid uit = kaal().eenheidVan(new Expr.Binair(Expr.BinOp.MAAL,
                liegendeRef(ot, "prijs"), liegendeRef(ot, "gewicht"), num(TON), bron()));
        assertEquals(Eenheid.product(EUR, KG), uit);
        assertNull(uit.canoniek(), "een opaak product draagt geen canoniek");
        assertNull(uit.exacteHerschaalFactoren(EUR), "en dus geen exacte herschaalfactoren naar een concrete eenheid");
        assertFalse(uit.convertibleNaar(EUR), "€·kg herschaalt niet naar €");
        assertFalse(uit.convertibleNaar(KG), "en niet naar kg");
        assertTrue(uit.convertibleNaar(Eenheid.product(KG, EUR)), "alleen een identiek product is converteerbaar");
    }

    @Test void een_waarde_aggregatie_draagt_de_eenheid_van_operand_nul() {
        // §4.6 Eenheid1 — and the exact class the WF-091 card left open: a fold RESULT type has no single
        // declaration behind it, so the verifier cannot cross it and only this derivation can.
        Objecttype ot = vondst();
        for (Expr.FunctieKind kind : List.of(Expr.FunctieKind.SOM, Expr.FunctieKind.TOTAAL,
                Expr.FunctieKind.MINIMUM, Expr.FunctieKind.MAXIMUM,
                Expr.FunctieKind.EERSTE, Expr.FunctieKind.LAATSTE)) {
            Expr.Functie f = new Expr.Functie(kind, List.of(liegendeRef(ot)), ot, null, null, num(TON), bron());
            assertEquals(KG, kaal().eenheidVan(f), kind + " draagt de eenheid van operand 0");
        }
    }

    @Test void afronding_en_begrenzing_dragen_de_eenheid_van_hun_basis() {
        Objecttype ot = vondst();
        assertEquals(KG, kaal().eenheidVan(
                new Expr.Afgerond(liegendeRef(ot), Expr.AfrondModus.REKENKUNDIG, 2, bron())));
        assertEquals(KG, kaal().eenheidVan(
                new Expr.Begrensd(liegendeRef(ot), null, null, bron())));
    }

    // ---------------- the ch. 4.10 Let cluster ----------------
    // All three are ORDINARY Expr union nodes, so a test builds them directly; the derivation needs the
    // Let INDEX beside them for one reason only — a BinderRef asked in isolation carries no route up to its own
    // binding (see EffectieveEenheid#lets).

    /** The derivation as {@code ExecModelTranspiler} builds it, over the given Lets. */
    private static EffectieveEenheid overLets(Expr... lets) {
        return new EffectieveEenheid(List.of(lets));
    }

    @Test void een_binderlezing_diep_in_de_body_leest_de_gebonden_waarde() {
        // Let g = <Vondst.gewicht, annotated t> in (<read g, annotated t> plus <read g, annotated t>)
        Objecttype ot = vondst();
        // two DISTINCT reads that are record-EQUAL: the table is identity-keyed, so both must be recorded
        Expr lezing1 = new Expr.BinderRef("g", num(TON), bron());
        Expr lezing2 = new Expr.BinderRef("g", num(TON), bron());
        Expr.Binair body = new Expr.Binair(Expr.BinOp.PLUS, lezing1, lezing2, num(TON), bron());
        Expr let = new Expr.Let("g", liegendeRef(ot), body, num(TON), bron());
        EffectieveEenheid eff = overLets(let);

        assertEquals(KG, eff.eenheidVan(let), "de Let draagt haar body (WF-076)");
        // the boundary the harness caught: a restatement site INSIDE the body asks about a read with no
        // binding in hand, so the derivation must have recorded it (identity table over the Let index).
        assertEquals(KG, eff.eenheidVan(lezing1), "een losse binderlezing leest haar binding");
        assertEquals(KG, eff.eenheidVan(lezing2));
    }

    @Test void een_binnenste_binder_overschaduwt_de_buitenste() {
        // Let g = <gewicht in kg> in (Let g = <literal in t> in <read g>) — WF-064: the NEAREST binder wins,
        // so the read is tonnes here and the outer kg must not leak in.
        Objecttype ot = vondst();
        Expr lezing = new Expr.BinderRef("g", num(KG), bron());
        Expr.Literal inTonnen = new Expr.Literal("2", num(TON), bron());
        Expr binnen = new Expr.Let("g", inTonnen, lezing, num(KG), bron());
        Expr buiten = new Expr.Let("g", liegendeRef(ot), binnen, num(KG), bron());
        // both, in the loader's own (post-)order: the inner Let is built before the outer one wraps it
        EffectieveEenheid eff = overLets(binnen, buiten);

        assertEquals(TON, eff.eenheidVan(lezing), "de nabijste binder (t) wint van de buitenste (kg)");
        assertEquals(TON, eff.eenheidVan(binnen));
        assertEquals(TON, eff.eenheidVan(buiten));
    }

    @Test void een_binder_op_een_eenheidloze_waarde_leest_eenheidloos() {
        // "Gebonden aan een eenheidloze waarde" and "ongebonden" are two different facts and must stay apart: the
        // first HAS a source (a unitless one) and the annotation may not speak over it; only the second falls back.
        // Let n = <2, unitless> in <read n, annotated t> — both the read AND the Let lie about tonnes.
        Expr.Literal getal = new Expr.Literal("2", num(null), bron());
        Expr lezing = new Expr.BinderRef("n", num(TON), bron());
        Expr let = new Expr.Let("n", getal, lezing, num(TON), bron());
        EffectieveEenheid eff = overLets(let);

        assertNull(eff.eenheidVan(lezing), "de binding is eenheidloos; de annotatie (t) is haar leugen");
        assertNull(eff.eenheidVan(let), "en de Let draagt haar body");
        assertNull(eff.van(lezing).eenheidOf(), "ook op de datatype-route (van) blijft de eenheid weg");
    }

    @Test void een_ongebonden_binderlezing_houdt_haar_annotatie() {
        // No Let of that name anywhere: there is nothing to derive from, so the annotation stands —
        // the same honest fallback as an AttribuutRef without a declaration.
        Expr lezing = new Expr.BinderRef("onbekend", num(TON), bron());
        assertEquals(TON, overLets().eenheidVan(lezing));
    }

    @Test void een_keuze_leest_de_tak_niet_haar_eigen_claim() {
        Objecttype ot = vondst();
        Expr keuze = new Expr.IfThenElse(null, liegendeRef(ot), liegendeRef(ot), num(TON), bron());
        assertEquals(KG, overLets().eenheidVan(keuze));
    }

    // WF-064 inside a ch. 10.2 GUARD. The binder-read table is built by walking each Let with a scope, and a
    // guard's operands are reached through the shared Conditie walk. That walk is a FULL pre-order scan, so it
    // used to hand back every node of every operand — the operand AND its descendants — and the table walk then
    // re-entered each subtree MIDWAY, with the enclosing scope instead of the one the node actually sits in. For
    // a shadowing inner Let that is not extra work but a WRONG ANSWER: the correct recording (t) is made first
    // through the Let, and the mid-tree re-entry overwrites it with the outer binding (kg). The two shapes below
    // are the two ways an operand can hold that Let; both must read t.

    @Test void een_schaduwende_let_in_een_guard_operand_houdt_haar_eigen_binding() {
        // vorm A — the shadowing Let sits DIRECTLY in the guard:
        //   Let g = <2 kg> in (als (Let g = <2 t> in <read g>) groter dan <2 kg> dan … anders …)
        // WF-064: the read sits under the INNER binder, so it is tonnes. Measured kg before the repair.
        Expr lezing = new Expr.BinderRef("g", num(KG), bron());
        Expr binnen = new Expr.Let("g", getal(TON), lezing, num(KG), bron());
        Conditie guard = new Conditie.Vergelijking(Conditie.Operator.GROTER, binnen, getal(KG), bron());
        Expr keuze = new Expr.IfThenElse(guard, getal(KG), getal(KG), num(KG), bron());
        Expr buiten = new Expr.Let("g", getal(KG), keuze, num(KG), bron());

        assertEquals(TON, overLets(binnen, buiten).eenheidVan(lezing),
                "de nabijste binder (t) wint, ook wanneer haar Let in een ch. 10.2-guard staat");
    }

    @Test void een_schaduwende_let_onder_een_geneste_keuze_in_een_guard_houdt_haar_binding() {
        // vorm B — the same Let one level deeper, in the DAN-branch of a nested IfThenElse that is itself the
        // guard operand. Before the union landing this shape read t by accident: the nested node was an opaque
        // Onopgelost carrier, so the pre-order scan could not reach into it and there was nothing to re-enter.
        // Making it a union node exposed the same break here — the one measured behaviour difference of the
        // union card, repaired at its root rather than pinned as a divergence.
        Expr lezing = new Expr.BinderRef("g", num(KG), bron());
        Expr binnen = new Expr.Let("g", getal(TON), lezing, num(KG), bron());
        Conditie binnenGuard = new Conditie.Vergelijking(Conditie.Operator.GROTER, getal(KG), getal(KG), bron());
        Expr genesteldeKeuze = new Expr.IfThenElse(binnenGuard, binnen, getal(KG), num(KG), bron());
        Conditie guard = new Conditie.Vergelijking(Conditie.Operator.GROTER, genesteldeKeuze, getal(KG), bron());
        Expr keuze = new Expr.IfThenElse(guard, getal(KG), getal(KG), num(KG), bron());
        Expr buiten = new Expr.Let("g", getal(KG), keuze, num(KG), bron());

        assertEquals(TON, overLets(binnen, buiten).eenheidVan(lezing),
                "ook onder een geneste keuze in de guard wint de nabijste binder (t)");
    }

    /** A unit-bearing literal — the value a shadowing binder binds to. */
    private static Expr getal(Eenheid e) { return new Expr.Literal("2", num(e), bron()); }

    // ---------------- the three reason-carrying exceptions ----------------

    @Test void een_literal_houdt_haar_eigen_type() {
        // ch. 2.10 exception 1: the lexeme "3,5 t" IS three-and-a-half tonnes; there is no declaration
        // behind it to be a copy of.
        Expr.Literal lit = new Expr.Literal("3,5", num(TON), bron());
        assertEquals(TON, kaal().eenheidVan(lit));
    }

    @Test void een_tijdsduur_houdt_haar_eigen_eenheid() {
        // ch. 2.10 exception 3 (WF-038): "de tijdsduur van A tot B in jaren" — the unit is the OPERATOR's
        // parameter. Deriving it from operand 0 (a date) would erase the operator's own statement.
        Eenheid jaar = new Eenheid("jr", "Tijd", "mnd", new BigDecimal("12"));
        Expr.Literal datum = new Expr.Literal("2026-01-01", new Datatype.DatumInDagen(), bron());
        for (Expr.FunctieKind kind : List.of(Expr.FunctieKind.TIJDSDUUR, Expr.FunctieKind.ABSOLUTE_TIJDSDUUR)) {
            Expr.Functie f = new Expr.Functie(kind, List.of(datum, datum), null, null, null, num(jaar), bron());
            assertEquals(jaar, kaal().eenheidVan(f), kind + " draagt haar eigen 'in <eenheid>'");
        }
    }

    // (exception 2 — the duration operand of a date +/- duration — is a property of the CALLERS, which
    //  never route that operand through this class; the two sites carry an ANNOTATION BY DESIGN comment
    //  and the harness leaves those positions untwisted. There is nothing here to assert about it.)

    // ---------------- shape preservation ----------------

    @Test void een_niet_numeriek_type_komt_ongewijzigd_terug() {
        Expr.Literal tekst = new Expr.Literal("aap", new Datatype.Tekst(), bron());
        assertSame(tekst.type(), kaal().van(tekst));
        assertNull(kaal().eenheidVan(tekst));
        assertNull(kaal().van(null));
    }

    @Test void alleen_de_eenheid_wordt_geent_de_rest_van_de_annotatie_blijft() {
        // The derivation replaces exactly one facet. Soort and precision are equally repetitions, but this
        // backend reads neither for a restatement (it reads Afgerond.decimalen, a NODE field), so grafting
        // more than the unit would move behaviour the clause does not ask to move.
        Expr.AttribuutRef ref = new Expr.AttribuutRef("gewicht", vondst(), "een vondst", null,
                new Datatype.Numeriek(true, 7, Datatype.Teken.POSITIEF, TON), bron());
        Datatype uit = kaal().van(ref);
        assertEquals(new Datatype.Numeriek(true, 7, Datatype.Teken.POSITIEF, KG), uit);
    }

    @Test void een_annotatie_die_al_klopt_levert_hetzelfde_object_op() {
        // The no-op path a verifier-clean document takes: nothing is rebuilt, so a document that obeys
        // WF-091 lowers exactly as it did before this derivation existed.
        Expr.AttribuutRef eerlijk = new Expr.AttribuutRef("gewicht", vondst(), "een vondst", null, num(KG), bron());
        assertSame(eerlijk.type(), kaal().van(eerlijk));
    }
}
