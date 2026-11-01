package dev.uchar.regelspraak.model;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A unit (§13.3.5, IR-SPEC ch. 2.5): a non-empty list of {@link Factor factors}, each a declared unit with a whole
 * exponent. No factor at all is no unit, never a {@code Unit}. "€" is one factor, "€/jr" is € and jr with exponent -1,
 * "1/dg" is dg with exponent -1, and "€·m" is € and m.
 *
 * <p>A factor names its abbreviation ("jr", "km", "mnd", "€"), the unit system that declares it, and — when the system
 * declares conversions (§13.3.5.2, "het jaar jr = 12 mnd") — the system's canonical unit it reduces to plus the factor
 * that expresses one of this unit in that canonical unit.
 *
 * <p>The record speaks of a unit as its numerator and its denominator: {@link #abbreviation}, {@link #system},
 * {@link #canonical} and {@link #factorToCanonical} describe the numerator, the factors with a positive exponent, and
 * {@link #denominator} is the factors with a negative exponent, or {@code null} where there are none. Converting a rate
 * restates the numerator AND inverts the denominator: "120 €/jr" is "10 €/mnd" (÷12), so the rate scales by the
 * numerator factor times the inverse of the denominator factor.
 *
 * <p>Two units are inter-convertible when their numerators reduce to the same canonical unit (or share an abbreviation)
 * AND their denominators are likewise convertible (or both absent). A numerator of more than one atom is a product,
 * which carries no canonical and is convertible only to the same product. {@link #conversionFactorTo} gives the
 * multiplier to restate a quantity in this unit as a quantity in a convertible target unit, so arithmetic and comparison
 * across units (jr vs mnd, €/jr vs €/mnd) are scaled rather than computed on raw magnitudes.
 */
public record Unit(List<Factor> factors) {

    /** One factor of a unit: a declared unit and its whole exponent, never 0. */
    public record Factor(String abbreviation, String system, String canonical, BigDecimal factorToCanonical, int exponent) {

        Factor inverted() { return new Factor(abbreviation, system, canonical, factorToCanonical, -exponent); }

        private String rendering() {
            return exponent == 1 || exponent == -1 ? abbreviation : abbreviation + "^" + Math.abs(exponent);
        }
    }

    /** WF-124 (d): positive exponents before negative ones, within a sign by abbreviation in code point order, then by
     *  system with an absent system first. {@code String.compareTo} compares UTF-16 units, which is another order. */
    private static final Comparator<String> CODE_POINTS = Comparator.nullsFirst(
            (a, b) -> Arrays.compare(a.codePoints().toArray(), b.codePoints().toArray()));

    private static final Comparator<Factor> CANONICAL_ORDER = Comparator
            .comparing((Factor f) -> f.exponent() < 0)
            .thenComparing(Factor::abbreviation, CODE_POINTS)
            .thenComparing(Factor::system, CODE_POINTS);

    /** The spelling of the numerator of a reciprocal, which holds no factor. */
    private static final String ONE_ABBREVIATION = "1";

    public Unit {
        if (factors.isEmpty()) throw new IllegalArgumentException("a unit has at least one factor");
        factors = java.util.Collections.unmodifiableList(new ArrayList<>(factors));
    }

    /** A simple unit with no declared conversion: it is its own canonical (factor 1). */
    public Unit(String abbreviation, String system) { this(abbreviation, system, abbreviation, BigDecimal.ONE); }

    /** A simple unit with a declared canonical + factor. */
    public Unit(String abbreviation, String system, String canonical, BigDecimal factorToCanonical) {
        this(List.of(new Factor(abbreviation, system, canonical, factorToCanonical, 1)));
    }

    private static Unit sorted(List<Factor> factors) {
        return new Unit(factors.stream().sorted(CANONICAL_ORDER).toList());
    }

    private List<Factor> positives() { return factors.stream().filter(f -> f.exponent() > 0).toList(); }

    private List<Factor> negatives() { return factors.stream().filter(f -> f.exponent() < 0).toList(); }

    /** A composite (rate) unit: the numerator of one unit over another ("€/jr"). */
    public static Unit compound(Unit numerator, Unit denominator) {
        List<Factor> all = new ArrayList<>(numerator.positives());
        for (Factor f : denominator.factors) all.add(f.inverted());
        return sorted(all);
    }

    /** E⁻¹, the unit of a unitless number divided by E: "dg" gives "1/dg", "1/dg" gives "dg" and "€/dg" gives "dg/€".
     *  Null for a unit with a product in it. */
    public static Unit reciprocal(Unit e) {
        Unit denominator = e.denominator();
        if (e.isProduct() || (denominator != null && denominator.isProduct())) return null;
        return sorted(e.factors.stream().map(Factor::inverted).toList());
    }

    /** Whether this is E⁻¹: a denominator over no factor at all. */
    public boolean isReciprocal() { return positives().isEmpty(); }

    /** Whether the numerator is a product of units ("€·m", or m with exponent 2). */
    public boolean isProduct() { return positives().stream().mapToInt(Factor::exponent).sum() > 1; }

    /** §4.4 the PRODUCT of two simple units ("€·m") — an opaque composite carrying NO canonical, so it is
     *  convertible only to an identical product (any assignment/comparison against a concrete unit then flags a
     *  §4 unit mismatch instead of silently passing as it did when the product degraded to Onbekend). The factors of
     *  both operands stand in the canonical order and an atom named twice is one factor with its exponents added, so
     *  every grouping and order of the same factors yields ONE unit — making the product commutative AND associative
     *  (§4.4 Tabel 11): (€·m)·km == €·(m·km) == €·km·m. A self-product (€ maal €) is € with exponent 2. The model does
     *  not (yet) cancel a factor of a product (€·m gedeeld door m): that stays an opaque product or Onbekend. */
    public static Unit product(Unit a, Unit b) {
        Map<String, Integer> exponents = new LinkedHashMap<>();
        for (Factor f : a.positives()) exponents.merge(f.abbreviation(), f.exponent(), Integer::sum);
        for (Factor f : b.positives()) exponents.merge(f.abbreviation(), f.exponent(), Integer::sum);
        List<Factor> all = new ArrayList<>();
        exponents.forEach((name, exponent) -> all.add(new Factor(name, a.system(), null, null, exponent)));
        return sorted(all);
    }

    /** The spelling of the numerator: the abbreviation of its one factor, its atoms joined by "·" where it is a
     *  product, "1" where it holds no factor. */
    public String abbreviation() {
        List<Factor> numerator = positives();
        if (numerator.isEmpty()) return ONE_ABBREVIATION;
        if (numerator.size() == 1 && numerator.get(0).exponent() == 1) return numerator.get(0).abbreviation();
        List<String> atoms = new ArrayList<>();
        for (Factor f : numerator)
            for (int i = 0; i < f.exponent(); i++) atoms.add(f.abbreviation());
        return String.join("·", atoms);
    }

    /** The unit system of the numerator's first factor; null where the numerator holds no factor. */
    public String system() {
        List<Factor> numerator = positives();
        return numerator.isEmpty() ? null : numerator.get(0).system();
    }

    /** The canonical unit of a numerator of one atom; a product and an empty numerator have none. */
    public String canonical() { return isSingleAtomNumerator() ? positives().get(0).canonical() : null; }

    /** The factor into the canonical unit of a numerator of one atom; a product and an empty numerator have none. */
    public BigDecimal factorToCanonical() { return isSingleAtomNumerator() ? positives().get(0).factorToCanonical() : null; }

    private boolean isSingleAtomNumerator() {
        List<Factor> numerator = positives();
        return numerator.size() == 1 && numerator.get(0).exponent() == 1;
    }

    /** The factors with a negative exponent, inverted, as a unit; null where there are none. */
    public Unit denominator() {
        List<Factor> below = negatives();
        return below.isEmpty() ? null : new Unit(below.stream().map(Factor::inverted).toList());
    }

    /** Human-readable spelling (IR-SPEC ch. 2.5): the positive factors joined by "·", "1" if there is none, then "/"
     *  and each negative factor, a power written where its size is not 1 ("€/jr", "1/dg", "m^2"). */
    public String rendering() {
        List<Factor> numerator = positives();
        StringBuilder b = new StringBuilder(numerator.isEmpty() ? ONE_ABBREVIATION
                : String.join("·", numerator.stream().map(Factor::rendering).toList()));
        for (Factor f : negatives()) b.append('/').append(f.rendering());
        return b.toString();
    }

    /** The rate's NUMERATOR as a stand-alone unit (denominator stripped); this unit itself when plain; null for a
     *  reciprocal, whose numerator is no unit: E⁻¹ × E is a number without a unit. */
    public Unit numerator() {
        if (isReciprocal()) return null;
        return negatives().isEmpty() ? this : new Unit(positives());
    }

    /** Convertible iff numerator and denominator are each convertible (or both denominators
     *  absent). A composite and a plain unit are never convertible. */
    public boolean convertibleTo(Unit other) {
        if (other == null) return false;
        Unit denominator = denominator(), otherDenominator = other.denominator();
        if ((denominator == null) != (otherDenominator == null)) return false;
        if (denominator != null && !denominator.convertibleTo(otherDenominator)) return false;
        return numeratorConvertible(other);
    }

    /** Numerator convertibility: the same unit (abbreviation), or two units that reduce to the SAME
     *  system canonical. Sharing only an eenheidstelsel is NOT enough — two independent base units in one
     *  system (appel vs peer) reduce to disjoint canonicals and are not inter-convertible, so §13.3.5's
     *  "equal or convertible" requirement is violated and comparing them on raw magnitudes is a
     *  silent-wrong (p44). A well-formed system reduces every unit to one canonical, so this stays true
     *  for the ordinary jr/mnd/dag case; an unresolved (null) canonical is treated as not convertible. */
    private boolean numeratorConvertible(Unit other) {
        if (abbreviation().equals(other.abbreviation())) return true;
        return canonical() != null && other.canonical() != null && canonical().equals(other.canonical());
    }

    /** Multiplier to restate a quantity in THIS unit as a quantity in {@code target}. For a plain unit
     *  jr→mnd is 12, mnd→jr is 1/12; for a rate the numerator factor is multiplied by the INVERSE denominator
     *  factor, so €/mnd→€/jr is 12 (the denominator jr is 12 of the mnd it replaces).
     *
     *  <p>The caller MUST gate on {@link #convertibleTo}: between two units that do not inter-convert there
     *  is NO factor, and answering 1 turns a name mismatch into a wrong magnitude instead of a fault. Measured:
     *  a reference-type copy that lied only in CASE ("KG" against the declared "kg") compares unequal both on
     *  the abbreviation and on the canonical, so a due restatement silently became the identity and one and the
     *  same case read 3,75 in the reference interpreter against 3.750,00 down the Drools path. Every caller in
     *  this repo gates already (measured); this precondition keeps that path dead rather than assuming it is.
     *
     *  @throws IllegalStateException when {@code target} is absent or not inter-convertible with this unit —
     *  and ALSO on the one shape the outer gate admits: an inter-convertible pair whose shared canonical
     *  carries no declared factorToCanonical (the wire form). Gating on {@link #convertibleTo} is therefore
     *  necessary but not sufficient; the wire form has no factor to give either way. */
    public BigDecimal conversionFactorTo(Unit target) {
        if (!convertibleTo(target))
            throw new IllegalStateException("geen omrekenfactor van eenheid '" + rendering() + "' naar '"
                    + (target == null ? "(geen eenheid)" : target.rendering()) + "': niet inwisselbaar");
        BigDecimal f = numeratorFactorTo(target);
        Unit denominator = denominator(), targetDenominator = target.denominator();
        if (denominator != null && targetDenominator != null)
            // denominator inverts: per-this.denominator -> per-target.denominator is target.denominator -> this.denominator
            f = f.multiply(targetDenominator.numeratorFactorTo(denominator));
        return f;
    }

    /** The EXACT restatement factors {num, den} to restate a quantity in THIS unit as one in {@code target} —
     *  multiply by num then DIVIDE by den (one rounding, at the end), the lossless alternative to
     *  {@link #conversionFactorTo}'s pre-rounded single ratio (which truncates a non-terminating quotient like mnd→jr
     *  = 1/12 = 0.0833…). For a simple unit num/den are the two declared factorToCanonical; for a RATE the inverted
     *  denominator contributes its factor too (€/jr→€/mnd: num=€·mnd factors, den=€·jr factors -> ÷12 exactly). Returns
     *  {@code null} when not exactly restatable here: not convertible, an unknown factor, or an opaque product unit
     *  (no canonical) — the caller then keeps the single-ratio fallback, which is available only on a CONVERTIBLE
     *  pair: between units with no common unit there is no ratio either, and {@link #conversionFactorTo} says so. */
    public BigDecimal[] exactRestateFactors(Unit target) {
        if (target == null || !convertibleTo(target)) return null;
        // an empty numerator carries no factor; convertibleTo gave both sides that numerator
        BigDecimal num = isReciprocal() ? BigDecimal.ONE : factorToCanonical();
        BigDecimal den = target.isReciprocal() ? BigDecimal.ONE : target.factorToCanonical();
        if (num == null || den == null) return null;
        Unit denominator = denominator(), targetDenominator = target.denominator();
        if (denominator != null && targetDenominator != null) {   // a rate: per-this.denominator -> per-target.denominator inverts
            if (denominator.factorToCanonical() == null || targetDenominator.factorToCanonical() == null) return null;
            num = num.multiply(targetDenominator.factorToCanonical());
            den = den.multiply(denominator.factorToCanonical());
        }
        return new BigDecimal[]{num, den};
    }

    /** Numerator-only conversion factor (the simple-unit rule), for a numerator pair that
     *  {@link #numeratorConvertible} has already accepted: the identity when the two abbreviations are the same,
     *  otherwise the ratio of the two declared factors. Everything else has no factor and says so — a factor
     *  the declaration left unknown (the wire admits a canonical without a factorToCanonical): answering 1 there
     *  would turn a name mismatch into a wrong magnitude instead of a fault. The
     *  DIFFERENT-canonical throw below is a defensive assertion, unreachable under the callers' precondition
     *  (numeratorConvertible already accepted the pair, so unequal abbreviations imply one shared non-null
     *  canonical — measured over 8.8M constructed pairs, zero hits; no witness pins it and none claims to). Called for the DENOMINATOR too, so
     *  it names its own pair rather than the composite the caller asked about. */
    private BigDecimal numeratorFactorTo(Unit target) {
        if (abbreviation().equals(target.abbreviation())) return BigDecimal.ONE;
        if (canonical() == null || !canonical().equals(target.canonical()))
            throw new IllegalStateException("geen omrekenfactor van '" + abbreviation() + "' (canoniek " + canonical()
                    + ") naar '" + target.abbreviation() + "' (canoniek " + target.canonical()
                    + "): ze herleiden niet tot dezelfde canonieke eenheid");
        if (factorToCanonical() == null || target.factorToCanonical() == null)
            throw new IllegalStateException("onbekende omrekenfactor van '" + abbreviation() + "' ("
                    + factorToCanonical() + ") naar '" + target.abbreviation() + "' (" + target.factorToCanonical()
                    + "): canoniek '" + canonical() + "' zonder gedeclareerde factorNaarCanoniek");
        return factorToCanonical().divide(target.factorToCanonical(), MathContext.DECIMAL64);
    }

    /** True when restating this unit in {@code target} is not the identity (factor ≠ 1) —
     *  composite-aware, so €/jr→€/mnd (factor 1/12) requires conversion even though the
     *  numerators share an abbreviation.
     *
     *  <p>Two units that do NOT inter-convert answer false, explicitly rather than through an invented
     *  factor of 1. This is the one legitimate caller of the unguarded question: the transpilers ask it as
     *  "is this a cross-unit site?" on operand pairs whose convertibility they have not established (a §8.2
     *  timeline bound, an additive pair, a begrenzing bound) — and "no common unit" is precisely where no
     *  factor exists. The one exception is the wire form: a CONVERTIBLE pair whose canonical has no declared
     *  factorToCanonical throws here too, via the factor question — the method is not total, and it sits
     *  behind every gated call site as well.
     *  @throws IllegalStateException on the wire form: convertible, but the shared canonical carries no
     *  declared factorToCanonical. A caller that must tell "same unit" from "no common unit" apart asks
     *  {@link #convertibleTo}. */
    public boolean requiredConversion(Unit target) {
        return convertibleTo(target) && conversionFactorTo(target).compareTo(BigDecimal.ONE) != 0;
    }
}
