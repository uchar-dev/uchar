package dev.uchar.regelspraak.model;

import java.math.BigDecimal;
import java.math.MathContext;

/**
 * A unit (§13.3.5): its abbreviation ("jr", "km", "mnd", "€"), the unit system that
 * declares it, and — when the system declares conversions (§13.3.5.2,
 * "het jaar jr = 12 mnd") — the system's canonical unit it reduces to plus the
 * factor that expresses one of this unit in that canonical unit.
 *
 * <p>A COMPOSITE (rate) unit (§13.3.3.2, "€/jr", "km/u") additionally carries a
 * denominator unit ({@code denominator}). The {@code abbreviation}/{@code canonical}/
 * {@code factorToCanonical} describe the NUMERATOR; {@code denominator} is the (simple)
 * denominator unit, or {@code null} for a plain unit. Converting a rate restates the
 * numerator AND inverts the denominator: "120 €/jr" is "10 €/mnd" (÷12), so the rate
 * scales by the numerator factor times the inverse of the denominator factor.
 *
 * <p>Two units are inter-convertible when their numerators reduce to the same canonical
 * unit (or share an abbreviation/system) AND their denominators are likewise convertible
 * (or both absent). {@link #conversionFactorTo} gives the multiplier to restate a quantity
 * in this unit as a quantity in a convertible target unit, so arithmetic and comparison
 * across units (jr vs mnd, €/jr vs €/mnd) are scaled rather than computed on raw magnitudes.
 */
public record Unit(String abbreviation, String system, String canonical, BigDecimal factorToCanonical, Unit denominator) {

    /** A simple unit with no declared conversion: it is its own canonical (factor 1). */
    public Unit(String abbreviation, String system) { this(abbreviation, system, abbreviation, BigDecimal.ONE, null); }

    /** A simple unit with a declared canonical + factor. */
    public Unit(String abbreviation, String system, String canonical, BigDecimal factorToCanonical) {
        this(abbreviation, system, canonical, factorToCanonical, null);
    }

    /** A composite (rate) unit: a numerator unit over a denominator unit ("€/jr"). */
    public static Unit compound(Unit numerator, Unit denominator) {
        return new Unit(numerator.abbreviation, numerator.system, numerator.canonical, numerator.factorToCanonical, denominator);
    }

    /** §4.4 the PRODUCT of two simple units ("€·m") — an opaque composite carrying NO canonical, so it is
     *  convertible only to an identical product (any assignment/comparison against a concrete unit then flags a
     *  §4 unit mismatch instead of silently passing as it did when the product degraded to Onbekend). Each
     *  operand's afkorting is FLATTENED into its atomic "·"-factors and the combined multiset is SORTED before
     *  re-joining, so every grouping and order of the same factors yields ONE canonical afkorting — making the
     *  product commutative AND associative (§4.4 Tabel 11): (€·m)·km == €·(m·km) == €·km·m. A self-product
     *  (€ maal €) keeps both factors (€·€, a squared unit). The model does not (yet) re-decompose a product, so
     *  cancelling a factor away (€·m gedeeld door m) is not recognised — it stays an opaque product or Onbekend. */
    public static Unit product(Unit a, Unit b) {
        java.util.List<String> factors = new java.util.ArrayList<>();
        java.util.Collections.addAll(factors, a.abbreviation.split("·"));
        java.util.Collections.addAll(factors, b.abbreviation.split("·"));
        java.util.Collections.sort(factors);
        return new Unit(String.join("·", factors), a.system, null, null, null);
    }

    /** Human-readable spelling, including the denominator for a composite ("€/jr"). */
    public String rendering() { return denominator == null ? abbreviation : abbreviation + "/" + denominator.abbreviation; }

    /** The rate's NUMERATOR as a stand-alone unit (denominator stripped); this unit itself when plain. */
    public Unit numerator() {
        return denominator == null ? this : new Unit(abbreviation, system, canonical, factorToCanonical);
    }

    /** Convertible iff numerator and denominator are each convertible (or both denominators
     *  absent). A composite and a plain unit are never convertible. */
    public boolean convertibleTo(Unit other) {
        if (other == null) return false;
        if ((denominator == null) != (other.denominator == null)) return false;
        if (denominator != null && !denominator.convertibleTo(other.denominator)) return false;
        return numeratorConvertible(other);
    }

    /** Numerator convertibility: the same unit (abbreviation), or two units that reduce to the SAME
     *  system canonical. Sharing only an eenheidstelsel is NOT enough — two independent base units in one
     *  system (appel vs peer) reduce to disjoint canonicals and are not inter-convertible, so §13.3.5's
     *  "equal or convertible" requirement is violated and comparing them on raw magnitudes is a
     *  silent-wrong (p44). A well-formed system reduces every unit to one canonical, so this stays true
     *  for the ordinary jr/mnd/dag case; an unresolved (null) canonical is treated as not convertible. */
    private boolean numeratorConvertible(Unit other) {
        if (abbreviation.equals(other.abbreviation)) return true;
        return canonical != null && other.canonical != null && canonical.equals(other.canonical);
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
        if (denominator != null && target.denominator != null)
            // denominator inverts: per-this.denominator -> per-target.denominator is target.denominator -> this.denominator
            f = f.multiply(target.denominator.numeratorFactorTo(denominator));
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
        if (target == null || !convertibleTo(target)
                || factorToCanonical == null || target.factorToCanonical == null) return null;
        BigDecimal num = factorToCanonical, den = target.factorToCanonical;
        if (denominator != null && target.denominator != null) {   // a rate: per-this.denominator -> per-target.denominator inverts
            if (denominator.factorToCanonical == null || target.denominator.factorToCanonical == null) return null;
            num = num.multiply(target.denominator.factorToCanonical);
            den = den.multiply(denominator.factorToCanonical);
        }
        return new BigDecimal[]{num, den};
    }

    /** Numerator-only conversion factor (the simple-unit rule), for a numerator pair that
     *  {@link #numeratorConvertible} has already accepted: the identity when the two abbreviations are the same,
     *  otherwise the ratio of the two declared factors. Everything else has no factor and says so — a factor
     *  the declaration left unknown (the wire admits a canonical without a factorToCanonical), which used to
     *  answer 1: that is how a name mismatch became a wrong magnitude instead of a fault. The
     *  DIFFERENT-canonical throw below is a defensive assertion, unreachable under the callers' precondition
     *  (numeratorConvertible already accepted the pair, so unequal abbreviations imply one shared non-null
     *  canonical — measured over 8.8M constructed pairs, zero hits; no witness pins it and none claims to). Called for the DENOMINATOR too, so
     *  it names its own pair rather than the composite the caller asked about. */
    private BigDecimal numeratorFactorTo(Unit target) {
        if (abbreviation.equals(target.abbreviation)) return BigDecimal.ONE;
        if (canonical == null || !canonical.equals(target.canonical))
            throw new IllegalStateException("geen omrekenfactor van '" + abbreviation + "' (canoniek " + canonical
                    + ") naar '" + target.abbreviation + "' (canoniek " + target.canonical
                    + "): ze herleiden niet tot dezelfde canonieke eenheid");
        if (factorToCanonical == null || target.factorToCanonical == null)
            throw new IllegalStateException("onbekende omrekenfactor van '" + abbreviation + "' ("
                    + factorToCanonical + ") naar '" + target.abbreviation + "' (" + target.factorToCanonical
                    + "): canoniek '" + canonical + "' zonder gedeclareerde factorNaarCanoniek");
        return factorToCanonical.divide(target.factorToCanonical, MathContext.DECIMAL64);
    }

    /** True when restating this unit in {@code target} is not the identity (factor ≠ 1) —
     *  composite-aware, so €/jr→€/mnd (factor 1/12) requires conversion even though the
     *  numerators share an abbreviation.
     *
     *  <p>Two units that do NOT inter-convert answer false, now explicitly rather than through an invented
     *  factor of 1. This is the one legitimate caller of the unguarded question: the transpilers ask it as
     *  "is this a cross-unit site?" on operand pairs whose convertibility they have not established (a §8.2
     *  timeline bound, an additive pair, a begrenzing bound) — and "no common unit" is precisely where no
     *  factor exists. On such pairs the answer is unchanged from the fallback it replaces (false either way,
     *  measured). The one exception is the wire form: a CONVERTIBLE pair whose canonical has no declared
     *  factorToCanonical now throws here too, via the factor question, where it used to answer false — the
     *  method is no longer total, and it sits behind every gated call site as well.
     *  @throws IllegalStateException on the wire form: convertible, but the shared canonical carries no
     *  declared factorToCanonical. A caller that must tell "same unit" from "no common unit" apart asks
     *  {@link #convertibleTo}. */
    public boolean requiredConversion(Unit target) {
        return convertibleTo(target) && conversionFactorTo(target).compareTo(BigDecimal.ONE) != 0;
    }
}
