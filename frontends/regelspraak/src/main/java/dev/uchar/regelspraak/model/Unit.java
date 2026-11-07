package dev.uchar.regelspraak.model;

import dev.uchar.regelspraak.message.Component;
import dev.uchar.regelspraak.message.Msg;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static dev.uchar.regelspraak.model.RegelSpraakModel.Diagnostic.params;

/**
 * A unit (§13.3.5, IR-SPEC ch. 2.5): a non-empty list of {@link Factor factors}, each a declared unit with a whole
 * exponent. No factor at all is no unit, never a {@code Unit}. "€" is one factor, "€/jr" is € and jr with exponent -1,
 * "1/dg" is dg with exponent -1, and "m·€" is m and €.
 *
 * <p>A factor names its abbreviation ("jr", "km", "mnd", "€"), the unit system that declares it, and — when the system
 * declares conversions (§13.3.5.2, "het jaar jr = 12 mnd") — the system's canonical unit it reduces to plus the factor
 * that expresses one of this unit in that canonical unit, an exact {@link Fraction} ("1/3" stays 1/3).
 *
 * <p>The algebra is that of a product of factors (RUNTIME-SEMANTICS ch. 2.6). {@link #times} adds the exponents of two
 * units and {@link #divide} subtracts them, a right factor restated into the left factor of its class on the way. Two
 * units are inter-convertible when they have one dimension: the same canonical classes with the same exponents.
 * {@link #exactRestateFactors} gives the multiplier to restate a quantity in this unit as a quantity in a convertible
 * target, the product of the powers of the declared factors, so arithmetic and comparison across units (jr vs mnd, €/jr
 * vs €/mnd, m^2 vs cm^2) are scaled rather than computed on raw magnitudes, and no factor is rounded on the way to a
 * collapse point (RUNTIME-SEMANTICS ch. 2.1).
 *
 * <p>{@link #abbreviation}, {@link #system}, {@link #canonical} and {@link #factorToCanonical} are the fields of a
 * declared unit, one factor with exponent 1 ({@link #isAtom}), and refuse any other unit: a spelling comes from
 * {@link #rendering}, and every other question about a unit is a method here. {@link #denominator} is the factors with
 * a negative exponent, or {@code null} where there are none.
 */
public record Unit(List<Factor> factors) {

    /** An exact fraction in lowest terms, its sign on the numerator. A declared factor may be zero or negative; the
     *  resolver reports those, because the wire writes a factor as a positive fraction. */
    public record Fraction(BigInteger num, BigInteger den) {
        public static final Fraction ONE = new Fraction(BigInteger.ONE, BigInteger.ONE);

        public Fraction {
            if (den.signum() == 0) throw new IllegalArgumentException(new Msg(Component.FRONTEND, "RGS-382", params()).text());
            if (den.signum() < 0) { num = num.negate(); den = den.negate(); }
            BigInteger g = num.gcd(den);
            num = num.divide(g);
            den = den.divide(g);
        }

        public static Fraction of(long num, long den) { return new Fraction(BigInteger.valueOf(num), BigInteger.valueOf(den)); }

        public Fraction times(Fraction o) { return new Fraction(num.multiply(o.num), den.multiply(o.den)); }

        public Fraction inverted() { return new Fraction(den, num); }

        public boolean isWhole() { return den.equals(BigInteger.ONE); }

        Fraction toThePower(int e) {
            int size = Math.abs(e);
            return e >= 0 ? new Fraction(num.pow(size), den.pow(size)) : new Fraction(den.pow(size), num.pow(size));
        }

        @Override public String toString() { return isWhole() ? num.toString() : num + "/" + den; }
    }

    /** One factor of a unit: a declared unit and its whole exponent, never 0. */
    public record Factor(String abbreviation, String system, String canonical, Fraction factorToCanonical, int exponent) {

        Factor inverted() { return withExponent(-exponent); }

        Factor withExponent(int e) { return new Factor(abbreviation, system, canonical, factorToCanonical, e); }

        /** The canonical unit, or the abbreviation of a bare unit, which is the canonical target of every unit that
         *  names it. A name is compared as written: a copy that lies in case alone is another class, and a site that
         *  must restate it faults rather than guess. */
        public String quantityClass() { return canonical != null ? canonical : abbreviation; }

        boolean sameAtom(Factor o) {
            return Objects.equals(abbreviation, o.abbreviation) && Objects.equals(system, o.system)
                    && Objects.equals(canonical, o.canonical) && Objects.equals(factorToCanonical, o.factorToCanonical);
        }

        /** Both fields of the conversion pair, or neither (a bare unit, factor 1). Half a pair has no factor. */
        boolean hasFactor() { return (canonical == null) == (factorToCanonical == null); }

        private String rendering(java.util.function.Function<Factor, String> name) {
            String n = name.apply(this);
            return exponent == 1 || exponent == -1 ? n : n + "^" + Math.abs(exponent);
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

    private static final String PERCENT_ABBREVIATION = "%";

    public Unit {
        if (factors.isEmpty()) throw new IllegalArgumentException("a unit has at least one factor");
        factors = java.util.Collections.unmodifiableList(new ArrayList<>(factors));
    }

    /** A simple unit with no declared conversion: it is its own canonical (factor 1). */
    public Unit(String abbreviation, String system) { this(abbreviation, system, abbreviation, Fraction.ONE); }

    /** A simple unit with a declared canonical + factor. */
    public Unit(String abbreviation, String system, String canonical, Fraction factorToCanonical) {
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

    /** E⁻¹, the unit of a unitless number divided by E: "dg" gives "1/dg", "1/dg" gives "dg", "€/dg" gives "dg/€" and
     *  "m^2" gives "1/m^2". */
    private static Unit reciprocal(Unit e) {
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

    /** The exact factor the value of {@code a} times {@code b} takes beside the two values: 100/1 for cm times m, 7/1 for
     *  €/dg times wk, 1/1 where no factor of {@code b} is restated. */
    public static Fraction timesFactors(Unit a, Unit b) {
        return a == null || b == null ? Fraction.ONE : fraction(multiply(a, b).powers());
    }

    /** As {@link #timesFactors}, for {@code a} divided by {@code b}. */
    public static Fraction divideFactors(Unit a, Unit b) {
        return a == null || b == null ? Fraction.ONE : fraction(multiply(a, reciprocal(b)).powers());
    }

    /** ∏ f^e of {@code powers}, exactly. A bare unit has factor 1.
     *  @throws IllegalStateException on half a conversion pair, which has no factor. */
    private static Fraction fraction(List<Factor> powers) {
        Fraction product = Fraction.ONE;
        for (Factor f : powers) {
            if (!f.hasFactor())
                throw new IllegalStateException(new Msg(Component.FRONTEND, "RGS-383", params("abbreviation",
                        f.abbreviation(), "factor", f.factorToCanonical(), "canonical", f.canonical())).text());
            if (f.factorToCanonical() != null) product = product.times(f.factorToCanonical().toThePower(f.exponent()));
        }
        return product;
    }

    /** One factor with exponent 1: the only unit a calendar table or a canonical table can name. */
    public boolean isAtom() { return factors.size() == 1 && factors.get(0).exponent() == 1; }

    /** The one positive factor is "%", with or without a denominator: the reading that makes a number a percentage. */
    public boolean isPercentage() {
        List<Factor> numerator = positives();
        return numerator.size() == 1 && numerator.get(0).exponent() == 1 && PERCENT_ABBREVIATION.equals(numerator.get(0).abbreviation());
    }

    /** The factors with a negative exponent, in the order of the list. */
    public List<Factor> negativeFactors() { return negatives(); }

    /** The one negative factor as a stand-alone unit of exponent 1; null where there is no negative factor or more than
     *  one. */
    public Unit negativeAtom() {
        List<Factor> below = negatives();
        return below.size() == 1 ? new Unit(List.of(below.get(0).withExponent(1))) : null;
    }

    /** The unit a fold over a timeline leaves where the reader does not hold the grid (ch. 2.6, rule 8): the one negative
     *  factor rises one power, "€/mnd" gives "€", "€/dg^2" gives "€/dg" and "1/dg" no unit (null). A unit with no negative
     *  factor or more than one stands unchanged: which one rises is the grid's to say, and the write boundary then
     *  refuses the value by name. {@link dev.uchar.regelspraak.core.TimeCalculation#afterTimeFold} holds the grid. */
    public Unit afterTimeFold() {
        List<Factor> below = negatives();
        return below.size() == 1 ? risen(below.get(0)) : this;
    }

    /** This unit with the negative factor {@code f} one power higher: null where no factor is left. */
    public Unit risen(Factor f) {
        List<Factor> all = new ArrayList<>(factors);
        all.set(all.indexOf(f), f.withExponent(f.exponent() + 1));
        all.removeIf(x -> x.exponent() == 0);
        return all.isEmpty() ? null : new Unit(all);
    }

    /** The one factor of a declared unit. Any other unit has no abbreviation, system or canonical of its own: asking
     *  would read a part of it as the whole. */
    private Factor atom() {
        if (!isAtom())
            throw new IllegalStateException(new Msg(Component.FRONTEND, "RGS-384", params("unit", rendering())).text());
        return factors.get(0);
    }

    /** The abbreviation of a declared unit.
     *  @throws IllegalStateException on any other unit, as {@link #system}, {@link #canonical} and
     *  {@link #factorToCanonical} do. */
    public String abbreviation() { return atom().abbreviation(); }

    /** The unit system that declares this unit; see {@link #abbreviation}. */
    public String system() { return atom().system(); }

    /** The canonical unit this declared unit reduces to; see {@link #abbreviation}. */
    public String canonical() { return atom().canonical(); }

    /** The factor into the canonical unit; see {@link #abbreviation}. */
    public Fraction factorToCanonical() { return atom().factorToCanonical(); }

    /** The factors with a negative exponent, inverted, as a unit; null where there are none. */
    public Unit denominator() {
        List<Factor> below = negatives();
        return below.isEmpty() ? null : new Unit(below.stream().map(Factor::inverted).toList());
    }

    /** Human-readable spelling (IR-SPEC ch. 2.5): the positive factors joined by "·", "1" if there is none, then "/"
     *  and each negative factor, a power written where its size is not 1 ("€/jr", "1/dg", "m^2"). */
    public String rendering() { return rendering(Factor::abbreviation); }

    /** {@link #rendering()} with each factor written as {@code name} gives it: a text part writes a factor by the symbol
     *  of its declaration (RUNTIME-SEMANTICS ch. 1.2), every other reader by its abbreviation. */
    public String rendering(java.util.function.Function<Factor, String> name) {
        List<Factor> numerator = positives();
        StringBuilder b = new StringBuilder(numerator.isEmpty() ? ONE_ABBREVIATION
                : String.join("·", numerator.stream().map(f -> f.rendering(name)).toList()));
        for (Factor f : negatives()) b.append('/').append(f.rendering(name));
        return b.toString();
    }

    /** The spelling of {@link #dimension}: "€/mnd" for "€/jr" with "jr = 12 mnd", "1" where no class is left. */
    public String dimensionRendering() {
        List<Factor> classes = new ArrayList<>();
        dimension().forEach((c, e) -> classes.add(new Factor(c, null, c, Fraction.ONE, e)));
        return classes.isEmpty() ? ONE_ABBREVIATION : sorted(classes).rendering();
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

    /** The exact factor to restate a quantity in THIS unit as one in {@code target}: multiply by its numerator, then
     *  divide by its denominator, with one rounding at the end. It is the product of the powers of the factors the units
     *  carry: jr→mnd is 12/1, €/jr→€/mnd is 1/12, m^2→cm^2 is 10000/1.
     *
     *  <p>Between two units that do not inter-convert there is NO factor, and answering 1 turns a name mismatch into a
     *  wrong magnitude instead of a fault. Measured: a reference-type copy that lied only in CASE ("KG" against the
     *  declared "kg") compares unequal both on the abbreviation and on the canonical, so a due restatement silently
     *  became the identity and one and the same case read 3,75 in the reference interpreter against 3.750,00 down the
     *  Drools path.
     *
     *  @throws IllegalStateException when {@code target} is absent or not inter-convertible with this unit, and ALSO on
     *  the one shape {@link #convertibleTo} admits: half a conversion pair (a canonical without a declared
     *  factorToCanonical, the wire form), which has no factor to give either way. */
    public Fraction exactRestateFactors(Unit target) {
        if (!convertibleTo(target))
            throw new IllegalStateException(new Msg(Component.FRONTEND, "RGS-385", params("unit", rendering(),
                    "target", target == null ? Msg.frontend("PART.no-unit") : target.rendering())).text());
        return fraction(powersTo(target));
    }

    /** True when restating this unit in {@code target} is not the identity (factor ≠ 1). €/jr→€/mnd (factor 1/12)
     *  requires conversion even though the numerators share an abbreviation.
     *
     *  <p>Two units that do NOT inter-convert answer false, explicitly rather than through an invented factor of 1. This
     *  is the one legitimate caller of the unguarded question: the transpilers ask it as "is this a cross-unit site?" on
     *  operand pairs whose convertibility they have not established (a §8.2 timeline bound, an additive pair, a
     *  begrenzing bound), and "no common unit" is precisely where no factor exists.
     *  @throws IllegalStateException on the wire form: convertible, but a canonical carries no declared
     *  factorToCanonical. A caller that must tell "same unit" from "no common unit" apart asks {@link #convertibleTo}. */
    public boolean requiredConversion(Unit target) {
        return convertibleTo(target) && !exactRestateFactors(target).equals(Fraction.ONE);
    }
}
