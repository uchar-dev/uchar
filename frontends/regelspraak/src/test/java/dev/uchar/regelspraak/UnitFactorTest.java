package dev.uchar.regelspraak;

import dev.uchar.regelspraak.model.Unit;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
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
    private static final Unit CASE = new Unit("KG", "GEWICHT", "KG", BigDecimal.ONE);
    private static final Unit JR = new Unit("jr", "Tijd", "mnd", new BigDecimal("12"));
    private static final Unit MND = new Unit("mnd", "Tijd", "mnd", BigDecimal.ONE);
    /** Same system as jr/mnd, a DIFFERENT canonical — the appel-vs-peer pair numeratorConvertible names. */
    private static final Unit DG = new Unit("dg", "Tijd", "dg", BigDecimal.ONE);
    private static final Unit M = new Unit("m", "Lengte", "m", BigDecimal.ONE);
    /** §4.4's product of a unit with itself: m^2, another dimension than m. */
    private static final Unit M_TIMES_M = Unit.times(M, M);
    /** The bare corpus shape (WF-038 tijdsduurparameter): no system, no canonical, no factor. */
    private static final Unit BARE_CORPUS_FORM = new Unit("jaar", null, null, null);
    private static final Unit EUR_PER_JR = Unit.compound(EUR, JR);
    private static final Unit EUR_PER_MND = Unit.compound(EUR, MND);
    private static final Unit EUR_PER_KG = Unit.compound(EUR, KG);
    /** The wire shape: a canonical WITHOUT the factor that reaches it. Both fields are nullable in
     *  the schema and no WF rule forbids the combination, so this survives verification. */
    private static final Unit CANONICAL_WITHOUT_FACTOR = new Unit("x", "Lengte", "m", null);

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

    /** (5) A product against one of its own factors: another dimension. The bare corpus shape (canoniek null, 28 occurrences on the wire) refuses through
     *  the same branch, so it is pinned here too rather than in a witness of its own. */
    @Test void a_product_refuses_the_factor_question_into_its_own_factor() {
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

    /** IR-SPEC ch. 2.5, point 5: the spelling of a unit comes from its factors. */
    @Test void a_unit_is_spelled_from_its_factors() {
        Unit.Factor m2 = new Unit.Factor("m", "Lengte", "m", BigDecimal.ONE, 2);
        Unit.Factor s = new Unit.Factor("s", "Tijd", "s", BigDecimal.ONE, -1);
        Unit.Factor u = new Unit.Factor("u", "Tijd", "s", new BigDecimal("3600"), -1);
        Unit.Factor eur = new Unit.Factor("€", "Geld", "€", BigDecimal.ONE, 1);
        assertEquals("m", M.rendering());
        assertEquals("mnd", MND.rendering());
        assertEquals("€/s", new Unit(List.of(eur, s)).rendering());
        assertEquals("1/s", new Unit(List.of(s)).rendering());
        assertEquals("m^2", new Unit(List.of(m2)).rendering());
        assertEquals("m^2/s", new Unit(List.of(m2, s)).rendering());
        assertEquals("€/s/u", new Unit(List.of(eur, s, u)).rendering());
        assertEquals("1/m^2", new Unit(List.of(new Unit.Factor("m", "Lengte", "m", BigDecimal.ONE, -2))).rendering());
    }

    /** WF-124 (d): the canonical order is one of code points, which is not the order of {@code String.compareTo} on a
     *  supplementary character: U+FB00 lies below U+1D11E, while its UTF-16 unit lies above the lead surrogate. */
    @Test void the_factors_of_a_product_stand_in_code_point_order() {
        String low = "\uFB00", high = "\uD834\uDD1E";
        assertTrue(high.compareTo(low) < 0, "the two orders differ on this pair");
        Unit product = Unit.times(new Unit(high, "X"), new Unit(low, "X"));
        assertEquals(low, product.factors().get(0).abbreviation());
        assertEquals(product, Unit.times(new Unit(low, "X"), new Unit(high, "X")));
    }

    /** An atom named twice in a product is one factor with its exponents added, and it keeps its conversion pair. */
    @Test void a_self_product_is_one_factor_with_exponent_two() {
        Unit square = Unit.times(M, M);
        assertEquals(List.of(new Unit.Factor("m", "Lengte", "m", BigDecimal.ONE, 2)), square.factors());
        assertEquals("m^2", square.rendering());
        assertFalse(square.convertibleTo(M));
        assertTrue(square.convertibleTo(square));
        assertFalse(square.requiredConversion(square));
    }

    private static final Unit CM = new Unit("cm", "Lengte", "m", new BigDecimal("0.01"));

    private static void fraction(String expected, BigDecimal[] f) {
        assertEquals(expected, f[0].stripTrailingZeros().toPlainString() + "/" + f[1].stripTrailingZeros().toPlainString());
    }

    /** RUNTIME-SEMANTICS ch. 2.6, rule 1: a restatement is the product of the powers of the declared factors, exact. */
    @Test void a_power_restates_by_the_power_of_its_factor() {
        Unit square = Unit.times(M, M), cmSquare = Unit.times(CM, CM);
        assertTrue(square.convertibleTo(cmSquare));
        fraction("1/0.0001", square.exactRestateFactors(cmSquare));
        fraction("0.0001/1", cmSquare.exactRestateFactors(square));
        assertEquals(0, square.conversionFactorTo(cmSquare).compareTo(new BigDecimal("10000")));
        assertTrue(Unit.times(CM, M).convertibleTo(square), "cm·m heeft de dimensie van m^2, in welke spelling ook");
    }

    /** Rule 1 is about the dimension: m/s is not s/m, and m^2/s is not m^2. */
    @Test void a_dimension_is_a_class_with_its_exponent() {
        Unit perSecond = Unit.compound(M, new Unit("s", "Tijd"));
        assertFalse(perSecond.convertibleTo(Unit.reciprocal(perSecond)));
        assertFalse(Unit.divide(Unit.times(M, M), new Unit("s", "Tijd")).convertibleTo(Unit.times(M, M)));
    }

    /** Rule 2: a right factor of the class of a left one is restated into it first, on the value too. */
    @Test void a_product_takes_its_unit_from_the_left_and_its_factor_from_the_right() {
        assertEquals("cm^2", Unit.times(CM, M).rendering());
        fraction("1/0.01", Unit.timesFactors(CM, M));
        assertEquals("m^2", Unit.times(M, CM).rendering());
        fraction("0.01/1", Unit.timesFactors(M, CM));
        fraction("1/12", Unit.timesFactors(EUR_PER_JR, MND));
        assertEquals("EUR", Unit.times(EUR_PER_JR, MND).rendering());
        fraction("1/1", Unit.timesFactors(M, M));
        fraction("1/1", Unit.timesFactors(null, M));
    }

    /** Rule 3: the quotient is the product with the inverse. */
    @Test void a_quotient_is_the_product_with_the_inverse() {
        assertEquals("m", Unit.divide(Unit.times(M, M), CM).rendering());
        fraction("0.01/1", new BigDecimal[]{Unit.divideFactors(Unit.times(M, M), CM)[1], Unit.divideFactors(Unit.times(M, M), CM)[0]});
        assertNull(Unit.divide(DG, new Unit("wk", "Tijd", "dg", new BigDecimal("7"))));
        fraction("1/7", Unit.divideFactors(DG, new Unit("wk", "Tijd", "dg", new BigDecimal("7"))));
        assertEquals("1/m^2", Unit.divide(null, Unit.times(M, M)).rendering());
    }

    /** Half a conversion pair has no factor, in a product as in a restatement. */
    @Test void a_product_over_half_a_pair_refuses_the_factor() {
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> Unit.timesFactors(M, CANONICAL_WITHOUT_FACTOR));
        assertTrue(ex.getMessage().contains("zonder gedeclareerde factorNaarCanoniek"), ex.getMessage());
    }

    /** Rule 8: a fold over a timeline raises the one negative factor one power. */
    @Test void a_time_fold_raises_the_one_negative_factor() {
        assertEquals("EUR", EUR_PER_JR.afterTimeFold().rendering());
        assertNull(Unit.reciprocal(JR).afterTimeFold());
        Unit perDaySquared = Unit.divide(Unit.compound(EUR, DG), DG);
        assertEquals("EUR/dg^2", perDaySquared.rendering());
        assertEquals("EUR/dg", perDaySquared.afterTimeFold().rendering());
        assertEquals("EUR", EUR.afterTimeFold().rendering());
        Unit twoBelow = Unit.divide(Unit.compound(EUR, DG), M);
        assertEquals(twoBelow, twoBelow.afterTimeFold(), "welke factor stijgt, zegt alleen het rooster");
        assertEquals("dg", Unit.compound(EUR, DG).negativeAtom().rendering());
        assertNull(twoBelow.negativeAtom());
    }

    /** A calendar table names one factor with exponent 1. */
    @Test void a_calendar_unit_is_one_factor() {
        Unit dag = new Unit("dg", "Tijd");
        assertEquals("DAYS", dev.uchar.regelspraak.core.TimeCalculation.chronoUnit(dag));
        assertNull(dev.uchar.regelspraak.core.TimeCalculation.chronoUnit(Unit.times(dag, dag)));
        assertNull(dev.uchar.regelspraak.core.TimeCalculation.chronoUnit(Unit.compound(dag, M)));
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
