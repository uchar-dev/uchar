package dev.uchar.regelspraak;

import dev.uchar.regelspraak.model.Unit;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The fail-fast guard on {@link Unit}'s conversion factor: asking for a factor between two units
 * that do not inter-convert is a fault, never the silent 1 it used to answer. That fallback is what
 * turned a name mismatch into a WRONG MAGNITUDE — measured on a reference-type copy that lied only
 * in case ("KG" against the declared "kg"), where one and the same case read 3,75 in the reference
 * interpreter against 3.750,00 down the Drools path.
 *
 * <p>The witnesses split in three, and the split is the point:
 * <ul>
 *   <li>SIX refusals, one per shape a mismatch can take, each carrying BOTH unit names in its
 *       message — a fault a reader can act on rather than a bare stack trace;</li>
 *   <li>the wire-shape guard ({@code canoniek} filled, {@code factorToCanonical} absent — a shape
 *       the schema admits and the outer convertibility gate lets through) plus the absent target;</li>
 *   <li>what the card deliberately leaves alone: the three surviving factors, and the predicate
 *       {@code requiredConversion}, which stays FALSE on every mismatch. Eight transpiler sites ask
 *       that question bare — "is this a cross-unit site?" — on operand pairs whose convertibility is
 *       not established, and a later "throw there too" would break them. This test pins that
 *       contract so the breakage cannot happen quietly.</li>
 * </ul>
 *
 * <p>Mutation-measured: with {@code return BigDecimal.ONE} put back at the three throw sites, EIGHT
 * of the twelve go red — the six refusals, the wire shape and the absent target — while the three
 * preserved factors and the predicate stay green. No OTHER test in the suite moves, which is the
 * same zero the call-site probe measured: nothing else covered this branch.
 */
class UnitFactorTest {

    // The declarations the six mismatch shapes are built from. Each is well-formed on its own;
    // what fails is the PAIRING — two declared units need not inter-convert.
    private static final Unit EUR = new Unit("EUR", "Valuta");
    private static final Unit KG = new Unit("kg", "Gewicht");
    private static final Unit TON = new Unit("t", "Gewicht", "kg", new BigDecimal("1000"));
    /** The case-lie of the measured divergence: a copy repeating kg/Gewicht/kg in a different case. */
    private static final Unit CASE = new Unit("KG", "GEWICHT", "KG", BigDecimal.ONE, null);
    private static final Unit JR = new Unit("jr", "Tijd", "mnd", new BigDecimal("12"));
    private static final Unit MND = new Unit("mnd", "Tijd", "mnd", BigDecimal.ONE);
    /** Same system as jr/mnd, a DIFFERENT canonical — the appel-vs-peer pair numeratorConvertible names. */
    private static final Unit DG = new Unit("dg", "Tijd", "dg", BigDecimal.ONE);
    private static final Unit M = new Unit("m", "Lengte", "m", BigDecimal.ONE);
    /** §4.4's opaque product: no canonical, so convertible only to an identical product. */
    private static final Unit M_TIMES_M = Unit.product(M, M);
    /** The bare corpus shape (WF-038 tijdsduurparameter): no system, no canonical, no factor. */
    private static final Unit BARE_CORPUS_FORM = new Unit("jaar", null, null, null, null);
    private static final Unit EUR_PER_JR = Unit.compound(EUR, JR);
    private static final Unit EUR_PER_MND = Unit.compound(EUR, MND);
    private static final Unit EUR_PER_KG = Unit.compound(EUR, KG);
    /** The wire shape: a canonical WITHOUT the factor that reaches it. Both fields are nullable in
     *  the schema and no WF rule forbids the combination, so this survives verification. */
    private static final Unit CANONICAL_WITHOUT_FACTOR = new Unit("x", "Lengte", "m", null, null);

    /** Every refusal must name BOTH units: a fault that says only "not convertible" leaves the
     *  reader to guess which of the two declarations is the offending one. */
    private static void refusesWithBothNames(Unit van, Unit to) {
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> van.conversionFactorTo(to),
                () -> "de factorvraag " + van.rendering() + " -> " + to.rendering() + " moet weigeren, niet 1 geven");
        String message = ex.getMessage();
        assertTrue(message.contains("'" + van.rendering() + "'"),
                () -> "de melding noemt de broneenheid '" + van.rendering() + "' niet: " + message);
        assertTrue(message.contains("'" + to.rendering() + "'"),
                () -> "de melding noemt de doeleenheid '" + to.rendering() + "' niet: " + message);
    }

    // ---- de zes weigervormen -------------------------------------------------------------------

    /** (1) The measured motor behind the divergence: a copy that differs from the declaration only
     *  in case matches neither on the abbreviation nor on the canonical. */
    @Test void a_case_lie_refuses_the_factor_question() {
        refusesWithBothNames(CASE, TON);
    }

    /** (2) Two units from two systems: nothing ties them, so there is no ratio to give. */
    @Test void two_systems_refuse_the_factor_question() {
        refusesWithBothNames(EUR, KG);
    }

    /** (3) One system, two canonicals: sharing an eenheidstelsel is NOT convertibility. */
    @Test void two_canonicals_within_a_system_refuse_the_factor_question() {
        refusesWithBothNames(JR, DG);
    }

    /** (4) A plain unit against a rate: the numerators agree, the SHAPES do not. */
    @Test void bare_unit_against_rate_refuses_the_factor_question() {
        refusesWithBothNames(EUR, EUR_PER_JR);
    }

    /** (5) The opaque product against one of its own factors — no canonical on either side of the
     *  question. The bare corpus shape (canoniek null, 28 occurrences on the wire) refuses through
     *  the same branch, so it is pinned here too rather than in a witness of its own. */
    @Test void opaque_product_refuses_the_factor_question() {
        refusesWithBothNames(M_TIMES_M, M);
        refusesWithBothNames(BARE_CORPUS_FORM, MND);
    }

    /** (6) Two rates whose numerators agree and whose DENOMINATORS do not. */
    @Test void rate_against_another_rate_refuses_the_factor_question() {
        refusesWithBothNames(EUR_PER_JR, EUR_PER_KG);
    }

    // ---- de klem op de wirevorm en op het ontbrekende doel --------------------------------------

    /** The one shape the outer convertibility gate does NOT catch: same canonical, no declared
     *  factor. It used to answer 1 — a factor nobody declared. It now names itself, through the
     *  factor question AND through the predicate, which is the single public answer this card
     *  changes (it was false). */
    @Test void canonical_without_factor_clamps_also_via_the_predicate() {
        assertTrue(CANONICAL_WITHOUT_FACTOR.convertibleTo(M), "de buitenste poortwacht laat deze vorm door");
        IllegalStateException direct = assertThrows(IllegalStateException.class,
                () -> CANONICAL_WITHOUT_FACTOR.conversionFactorTo(M));
        assertTrue(direct.getMessage().contains("zonder gedeclareerde factorNaarCanoniek"),
                () -> "de melding moet de ontbrekende factor noemen, niet de inwisselbaarheid: " + direct.getMessage());
        IllegalStateException viaPredicate = assertThrows(IllegalStateException.class,
                () -> CANONICAL_WITHOUT_FACTOR.requiredConversion(M));
        assertEquals(direct.getMessage(), viaPredicate.getMessage(),
                "het predikaat moet dezelfde fout doorlaten, niet stil false antwoorden");
    }

    /** No target is no factor either. Every caller in the repo passes a non-null target (measured);
     *  the precondition keeps that path dead instead of assuming it is. */
    @Test void missing_target_refuses_the_factor_question() {
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> EUR.conversionFactorTo(null));
        assertTrue(ex.getMessage().contains("'EUR'") && ex.getMessage().contains("(geen eenheid)"),
                () -> "de melding moet de bron en het ontbrekende doel noemen: " + ex.getMessage());
    }

    // ---- wat ongemoeid blijft: de factoren zelf -------------------------------------------------

    /** jr -> mnd is 12: the declared factors divide, they do not collapse to the identity. */
    @Test void year_to_month_stays_twelve() {
        assertEquals(0, JR.conversionFactorTo(MND).compareTo(new BigDecimal("12")),
                () -> "jr -> mnd moet 12 blijven, kreeg " + JR.conversionFactorTo(MND));
        assertTrue(JR.requiredConversion(MND), "jr -> mnd is een echte omrekening");
    }

    /** mnd -> jr is the non-terminating inverse, pre-rounded on DECIMAL64 — the single-ratio path
     *  the exact {num, den} pair exists to avoid, and it must keep its value. */
    @Test void month_to_year_stays_a_twelfth() {
        assertEquals("0.08333333333333333", MND.conversionFactorTo(JR).toPlainString(),
                "mnd -> jr moet 1/12 op DECIMAL64 blijven");
        BigDecimal[] exact = MND.exactRestateFactors(JR);
        assertEquals(0, exact[0].compareTo(BigDecimal.ONE), "exacte teller");
        assertEquals(0, exact[1].compareTo(new BigDecimal("12")), "exacte noemer");
    }

    /** The denominator INVERTS: EUR/mnd -> EUR/jr is 12, because the jr that replaces the mnd is
     *  twelve of it. The numerators share an abbreviation, so a numerator-only reading would give 1
     *  here — the shape a careless guard loses first. */
    @Test void denominator_inversion_stays_standing() {
        assertEquals(0, EUR_PER_MND.conversionFactorTo(EUR_PER_JR).compareTo(new BigDecimal("12")),
                () -> "EUR/mnd -> EUR/jr moet 12 blijven, kreeg " + EUR_PER_MND.conversionFactorTo(EUR_PER_JR));
        assertTrue(EUR_PER_MND.requiredConversion(EUR_PER_JR), "EUR/mnd -> EUR/jr is een echte omrekening");
    }

    // ---- wat ongemoeid blijft: het predikaat ----------------------------------------------------

    /** The contract the eight bare predicate sites in the transpilers lean on: on a pair with no
     *  common unit there is no conversion to require, so the answer is FALSE — explicitly now,
     *  rather than read off an invented factor of 1. Turning any of these into a throw would fault
     *  a §8.2 timeline bound, an additive pair or a begrenzing bound that today merely asks. */
    @Test void required_conversion_stays_false_on_every_mismatch_and_on_nothing() {
        Unit[][] mismatches = {
                {CASE, TON}, {EUR, KG}, {JR, DG}, {EUR, EUR_PER_JR}, {M_TIMES_M, M}, {EUR_PER_JR, EUR_PER_KG},
        };
        for (Unit[] pair : mismatches) {
            assertFalse(pair[0].requiredConversion(pair[1]),
                    () -> "vereistOmrekening moet false blijven op " + pair[0].rendering() + " -> " + pair[1].rendering());
            assertFalse(pair[0].convertibleTo(pair[1]),
                    () -> "en convertibleNaar blijft de manier om 'geen gedeelde eenheid' van 'zelfde eenheid' te "
                            + "onderscheiden: " + pair[0].rendering() + " -> " + pair[1].rendering());
        }
        assertFalse(EUR.requiredConversion(null), "een afwezig doel vraagt geen omrekening, het weigert er een");
    }
}
