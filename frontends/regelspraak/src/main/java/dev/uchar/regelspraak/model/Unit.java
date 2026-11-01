package dev.uchar.regelspraak.model;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * A unit (§13.3.5, IR-SPEC ch. 2.5): a non-empty list of {@link Factor factors}, each a declared unit with a whole
 * exponent. No factor at all is no unit, never a {@code Unit}. "€" is one factor, "€/jr" is € and jr with exponent -1,
 * "1/dg" is dg with exponent -1, and "m·€" is m and €.
 *
 * <p>A factor names its abbreviation ("jr", "km", "mnd", "€"), the unit system that declares it, and — when the system
 * declares conversions (§13.3.5.2, "het jaar jr = 12 mnd") — the system's canonical unit it reduces to plus the factor
 * that expresses one of this unit in that canonical unit.
 *
 * <p>The algebra is that of a product of factors (RUNTIME-SEMANTICS ch. 2.6). {@link #times} adds the exponents of two
 * units and {@link #divide} subtracts them, a right factor restated into the left factor of its class on the way. Two
 * units are inter-convertible when they have one dimension: the same canonical classes with the same exponents.
 * {@link #exactRestateFactors} gives the multiplier to restate a quantity in this unit as a quantity in a convertible
 * target, the product of the powers of the declared factors, so arithmetic and comparison across units (jr vs mnd, €/jr
 * vs €/mnd, m^2 vs cm^2) are scaled rather than computed on raw magnitudes.
 *
 * <p>Where a caller speaks of a numerator and a denominator, {@link #abbreviation}, {@link #system}, {@link #canonical}
 * and {@link #factorToCanonical} describe the factors with a positive exponent, and {@link #denominator} the factors
 * with a negative one, or {@code null} where there are none.
 */
public record Unit(List<Factor> factors) {

    /** One factor of a unit: a declared unit and its whole exponent, never 0. */
    public record Factor(String abbreviation, String system, String canonical, BigDecimal factorToCanonical, int exponent) {

        Factor inverted() { return withExponent(-exponent); }

        Factor withExponent(int e) { return new Factor(abbreviation, system, canonical, factorToCanonical, e); }

        /** The canonical unit, or the abbreviation of a bare unit, which is the canonical target of every unit that
         *  names it. A name is compared as written: a copy that lies in case alone is another class, and a site that
         *  must restate it faults rather than guess. */
        String quantityClass() { return canonical != null ? canonical : abbreviation; }

        boolean sameAtom(Factor o) {
            return Objects.equals(abbreviation, o.abbreviation) && Objects.equals(system, o.system)
                    && Objects.equals(canonical, o.canonical)
                    && (factorToCanonical == null ? o.factorToCanonical == null
                            : o.factorToCanonical != null && factorToCanonical.compareTo(o.factorToCanonical) == 0);
        }

        /** Both fields of the conversion pair, or neither (a bare unit, factor 1). Half a pair has no factor. */
        boolean hasFactor() { return (canonical == null) == (factorToCanonical == null); }

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

    /** This unit to the power {@code e}: "m" to the power 2 is "m^2", "dg" to the power -1 is "1/dg". */
    public Unit toThePower(int e) {
        return new Unit(factors.stream().map(f -> f.withExponent(f.exponent() * e)).toList());
    }

    /** The unit a spelling names: the product of {@code parts}, one factor per atom in the order of WF-124 (d). Null
     *  where nothing is left ("m/m"). No factor is restated: "km/m" stays two factors, as written. */
    public static Unit ofParts(List<Unit> parts) {
        List<Factor> all = new ArrayList<>();
        for (Unit part : parts)
            for (Factor f : part.factors) {
                int at = indexOf(all, f::sameAtom);
                if (at < 0) all.add(f);
                else all.set(at, all.get(at).withExponent(all.get(at).exponent() + f.exponent()));
            }
        all.removeIf(f -> f.exponent() == 0);
        return all.isEmpty() ? null : sorted(all);
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

    /** E⁻¹, the unit of a unitless number divided by E: "dg" gives "1/dg", "1/dg" gives "dg", "€/dg" gives "dg/€" and
     *  "m^2" gives "1/m^2". */
    public static Unit reciprocal(Unit e) {
        return sorted(e.factors.stream().map(Factor::inverted).toList());
    }

    /** What a product leaves: the unit (null for none), and the powers of declared factors the value takes. */
    private record Product(Unit unit, List<Factor> powers) {}

    /** Rule 2 of ch. 2.6, the operands both unit-bearing: a factor of {@code v} that is an atom of {@code u} adds its
     *  exponent, one of the same class is restated into that atom first, any other joins the list. */
    private static Product multiply(Unit u, Unit v) {
        List<Factor> all = new ArrayList<>(u.factors);
        List<Factor> powers = new ArrayList<>();
        for (Factor f : v.factors) {
            int at = indexOf(all, f::sameAtom);
            if (at < 0) {
                at = indexOf(all, x -> Objects.equals(x.quantityClass(), f.quantityClass()));
                if (at >= 0) {
                    powers.add(f);
                    powers.add(all.get(at).withExponent(-f.exponent()));
                }
            }
            if (at < 0) all.add(f);
            else all.set(at, all.get(at).withExponent(all.get(at).exponent() + f.exponent()));
        }
        all.removeIf(f -> f.exponent() == 0);
        return new Product(all.isEmpty() ? null : sorted(all), powers);
    }

    private static int indexOf(List<Factor> fs, java.util.function.Predicate<Factor> test) {
        for (int i = 0; i < fs.size(); i++) if (test.test(fs.get(i))) return i;
        return -1;
    }

    /** {@code a} times {@code b} (ch. 2.6, rule 2): null is no unit, and a list that ends up empty is no unit either. A
     *  unitless operand hands the other unit through. */
    public static Unit times(Unit a, Unit b) {
        if (a == null) return b;
        return b == null ? a : multiply(a, b).unit();
    }

    /** {@code a} divided by {@code b} (ch. 2.6, rule 3): the product with the inverse. */
    public static Unit divide(Unit a, Unit b) {
        if (b == null) return a;
        return a == null ? reciprocal(b) : multiply(a, reciprocal(b)).unit();
    }

    /** The exact factor {num, den} the value of {@code a} times {@code b} takes beside the two values: 100/1 for cm
     *  times m, 7/1 for €/dg times wk, 1/1 where no factor of {@code b} is restated. */
    public static BigDecimal[] timesFactors(Unit a, Unit b) {
        return a == null || b == null ? new BigDecimal[]{BigDecimal.ONE, BigDecimal.ONE} : fraction(multiply(a, b).powers());
    }

    /** As {@link #timesFactors}, for {@code a} divided by {@code b}. */
    public static BigDecimal[] divideFactors(Unit a, Unit b) {
        return a == null || b == null ? new BigDecimal[]{BigDecimal.ONE, BigDecimal.ONE}
                : fraction(multiply(a, reciprocal(b)).powers());
    }

    /** ∏ f^e of {@code powers} as {num, den}, exactly: powers of the declared factors, each as a {@code BigDecimal}
     *  power, which rounds nothing. A bare unit has factor 1.
     *  @throws IllegalStateException on half a conversion pair, which has no factor. */
    private static BigDecimal[] fraction(List<Factor> powers) {
        BigDecimal num = BigDecimal.ONE, den = BigDecimal.ONE;
        for (Factor f : powers) {
            if (!f.hasFactor())
                throw new IllegalStateException("onbekende omrekenfactor van '" + f.abbreviation() + "' ("
                        + f.factorToCanonical() + "): canoniek '" + f.canonical() + "' zonder gedeclareerde factorNaarCanoniek");
            if (f.factorToCanonical() == null) continue;
            BigDecimal power = f.factorToCanonical().pow(Math.abs(f.exponent()));
            if (f.exponent() > 0) num = num.multiply(power);
            else den = den.multiply(power);
        }
        return new BigDecimal[]{num, den};
    }

    /** One factor with exponent 1: the only unit a calendar table or a canonical table can name. */
    public boolean isAtom() { return factors.size() == 1 && factors.get(0).exponent() == 1; }

    /** The one negative factor as a stand-alone unit of exponent 1; null where there is no negative factor or more than
     *  one. It is the unit a fold over a timeline can integrate against without the grid in hand. */
    public Unit negativeAtom() {
        List<Factor> below = negatives();
        return below.size() == 1 ? new Unit(List.of(below.get(0).withExponent(1))) : null;
    }

    /** The unit a fold over a timeline leaves (ch. 2.6, rule 8): the integration multiplies by the grid unit, so the one
     *  negative factor rises one power, "€/mnd" gives "€", "€/dg^2" gives "€/dg" and "1/dg" no unit (null). A unit with
     *  no negative factor or more than one stands unchanged: which one rises is the grid's to say, this model does not
     *  hold the grid, and the write boundary then refuses the value by name. */
    public Unit afterTimeFold() {
        List<Factor> below = negatives();
        if (below.size() != 1) return this;
        List<Factor> all = new ArrayList<>(factors);
        int at = all.indexOf(below.get(0));
        all.set(at, below.get(0).withExponent(below.get(0).exponent() + 1));
        all.removeIf(f -> f.exponent() == 0);
        return all.isEmpty() ? null : new Unit(all);
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

    /** The dimension of this unit (ch. 2.6): canonical class to the sum of its exponents, without zeros. */
    private Map<String, Integer> dimension() {
        Map<String, Integer> sums = new HashMap<>();
        for (Factor f : factors) sums.merge(f.quantityClass(), f.exponent(), Integer::sum);
        sums.values().removeIf(e -> e == 0);
        return sums;
    }

    /** Rule 1 of ch. 2.6: convertible iff the dimension is the same, whichever way the unit is spelled: "€/mnd" and
     *  "€/jr", "cm·m" and "m^2", and never two units that reduce to different canonicals. Sharing only an eenheidstelsel
     *  is NOT enough — two independent base units in one system (appel vs peer) reduce to disjoint canonicals and are not
     *  inter-convertible, so §13.3.5's "equal or convertible" requirement is violated and comparing them on raw magnitudes
     *  is a silent-wrong (p44). */
    public boolean convertibleTo(Unit other) {
        return other != null && dimension().equals(other.dimension());
    }

    /** The powers of declared factors that take a quantity in this unit to {@code target}, F(this)/F(target), without
     *  the atoms the two share: a factor that cancels is no factor to ask for. */
    private List<Factor> powersTo(Unit target) {
        List<Factor> powers = new ArrayList<>(factors);
        for (Factor t : target.factors) {
            int at = indexOf(powers, t::sameAtom);
            if (at < 0) powers.add(t.inverted());
            else powers.set(at, powers.get(at).withExponent(powers.get(at).exponent() - t.exponent()));
        }
        powers.removeIf(f -> f.exponent() == 0);
        return powers;
    }

    /** Multiplier to restate a quantity in THIS unit as a quantity in {@code target}, rounded to one ratio on DECIMAL64:
     *  jr→mnd is 12, mnd→jr is 1/12, and €/mnd→€/jr is 12 (the denominator jr is 12 of the mnd it replaces).
     *
     *  <p>The caller MUST gate on {@link #convertibleTo}: between two units that do not inter-convert there
     *  is NO factor, and answering 1 turns a name mismatch into a wrong magnitude instead of a fault. Measured:
     *  a reference-type copy that lied only in CASE ("KG" against the declared "kg") compares unequal both on
     *  the abbreviation and on the canonical, so a due restatement silently became the identity and one and the
     *  same case read 3,75 in the reference interpreter against 3.750,00 down the Drools path. Every caller in
     *  this repo gates already (measured); this precondition keeps that path dead rather than assuming it is.
     *
     *  @throws IllegalStateException when {@code target} is absent or not inter-convertible with this unit —
     *  and ALSO on the one shape the outer gate admits: an inter-convertible pair where half a conversion pair
     *  stands (a canonical without a declared factorToCanonical, the wire form). Gating on {@link #convertibleTo}
     *  is therefore necessary but not sufficient; the wire form has no factor to give either way. */
    public BigDecimal conversionFactorTo(Unit target) {
        if (!convertibleTo(target))
            throw new IllegalStateException("geen omrekenfactor van eenheid '" + rendering() + "' naar '"
                    + (target == null ? "(geen eenheid)" : target.rendering()) + "': niet inwisselbaar");
        BigDecimal[] f = fraction(powersTo(target));
        return f[0].divide(f[1], MathContext.DECIMAL64);
    }

    /** The EXACT restatement factors {num, den} to restate a quantity in THIS unit as one in {@code target} —
     *  multiply by num then DIVIDE by den (one rounding, at the end), the lossless alternative to
     *  {@link #conversionFactorTo}'s pre-rounded single ratio (which truncates a non-terminating quotient like mnd→jr
     *  = 1/12 = 0.0833…). num and den are the products of the powers of the declared factors: jr→mnd is 12/1, €/jr→€/mnd
     *  is 1/12, m^2→cm^2 is 10000/1. Returns {@code null} when not exactly restatable here: not convertible, or half a
     *  conversion pair — the caller then keeps the single-ratio fallback, which is available only on a CONVERTIBLE
     *  pair: between units with no common unit there is no ratio either, and {@link #conversionFactorTo} says so. */
    public BigDecimal[] exactRestateFactors(Unit target) {
        if (target == null || !convertibleTo(target)) return null;
        List<Factor> powers = powersTo(target);
        return powers.stream().allMatch(Factor::hasFactor) ? fraction(powers) : null;
    }

    /** True when restating this unit in {@code target} is not the identity (factor ≠ 1) —
     *  composite-aware, so €/jr→€/mnd (factor 1/12) requires conversion even though the
     *  numerators share an abbreviation.
     *
     *  <p>Two units that do NOT inter-convert answer false, explicitly rather than through an invented
     *  factor of 1. This is the one legitimate caller of the unguarded question: the transpilers ask it as
     *  "is this a cross-unit site?" on operand pairs whose convertibility they have not established (a §8.2
     *  timeline bound, an additive pair, a begrenzing bound) — and "no common unit" is precisely where no
     *  factor exists. The one exception is the wire form: a CONVERTIBLE pair with half a conversion pair in it
     *  throws here too, via the factor question — the method is not total, and it sits behind every gated call
     *  site as well.
     *  @throws IllegalStateException on the wire form: convertible, but a canonical carries no declared
     *  factorToCanonical. A caller that must tell "same unit" from "no common unit" apart asks
     *  {@link #convertibleTo}. */
    public boolean requiredConversion(Unit target) {
        return convertibleTo(target) && conversionFactorTo(target).compareTo(BigDecimal.ONE) != 0;
    }
}
