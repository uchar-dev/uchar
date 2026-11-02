package dev.uchar.regelspraak.runtime;

import dev.uchar.regelspraak.message.Msg;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;

/**
 * An EXACT rational number — {@code teller/noemer} over {@link BigInteger}, kept normalized (positive
 * denominator, gcd-reduced). It carries a number whose exact value does not terminate (RUNTIME-SEMANTICS ch. 2.1):
 * a quotient, a percentage, a power with a negative exponent, and every result of {@code +, −, ×} with such an
 * operand. A number that terminates is carried as the {@link BigDecimal} it is, so the {@code rs*} arithmetic helpers
 * accept either wherever they accept a number and return a {@link Breuk} exactly where the result does not terminate.
 *
 * <p>Bean fields are {@link BigDecimal}: a value written into a target that bounds its decimals fits them or is a
 * fault (RUNTIME-SEMANTICS ch. 2.4 point 1), so it is an exact decimal there; a value of the unbounded form stays a
 * {@link Breuk} in working memory where it does not terminate, and becomes a decimal only on the bean, at the
 * write-back ({@link #naarDecimal()}). An {@code afronding} rounds the exact value once
 * ({@link #naarDecimal(int, java.math.RoundingMode)}).
 *
 * <p>Extends {@link Number} so it flows through the {@code Object}-typed value plumbing (the exec back-end's
 * {@code BiFunction<Object,…,Object>} values, the DRL mvel {@code Object}); the {@code Number} methods evaluate the
 * quotient at double/long precision, used only where a numeric primitive is genuinely wanted, never on the exact
 * path.
 */
public final class Breuk extends Number implements Comparable<Breuk> {
    private static final long serialVersionUID = 1L;

    private final BigInteger teller;   // signed
    private final BigInteger noemer;   // strictly positive

    private Breuk(BigInteger teller, BigInteger noemer) {
        this.teller = teller;
        this.noemer = noemer;
    }

    /** Normalize (sign to the numerator, gcd-reduce) and construct. A zero denominator is never produced here —
     *  the callers guard division-by-zero — but is rejected defensively. */
    public static Breuk van(BigInteger teller, BigInteger noemer) {
        if (noemer.signum() == 0) throw new ArithmeticException("Breuk met noemer 0");
        if (noemer.signum() < 0) { teller = teller.negate(); noemer = noemer.negate(); }
        BigInteger g = teller.gcd(noemer);
        if (g.signum() != 0 && !g.equals(BigInteger.ONE)) { teller = teller.divide(g); noemer = noemer.divide(g); }
        return new Breuk(teller, noemer);
    }

    /**
     * Coerce any number-like value to a {@link Breuk}: a {@link Breuk} is returned as-is (identity), a
     * {@link BigDecimal} becomes its EXACT rational ({@code unscaledValue / 10^scale}, no precision loss), and
     * any other {@link Number} goes through its {@link BigDecimal} form. {@code null} is passed through as
     * {@code null} (a leegwaarde stays leeg — the helpers apply their own leeg tables). A non-number is a bug.
     */
    public static Breuk van(Object o) {
        if (o == null) return null;
        if (o instanceof Breuk b) return b;
        if (o instanceof BigDecimal bd) return vanDecimal(bd);
        if (o instanceof BigInteger bi) return van(bi, BigInteger.ONE);
        if (o instanceof Number n) return vanDecimal(new BigDecimal(n.toString()));
        throw new IllegalArgumentException("kan geen Breuk maken van " + o.getClass().getName());
    }

    private static Breuk vanDecimal(BigDecimal bd) {
        int s = bd.scale();
        BigInteger unscaled = bd.unscaledValue();
        if (s == 0) return van(unscaled, BigInteger.ONE);
        if (s > 0) return van(unscaled, BigInteger.TEN.pow(s));
        // negative scale (e.g. 1E+2): value = unscaled * 10^(-scale)
        return van(unscaled.multiply(BigInteger.TEN.pow(-s)), BigInteger.ONE);
    }

    public BigInteger teller() { return teller; }
    public BigInteger noemer() { return noemer; }

    // ---- exact arithmetic (a/b op c/d) ----
    public Breuk optellen(Breuk a) {
        return van(teller.multiply(a.noemer).add(a.teller.multiply(noemer)), noemer.multiply(a.noemer));
    }
    public Breuk aftrekken(Breuk a) {
        return van(teller.multiply(a.noemer).subtract(a.teller.multiply(noemer)), noemer.multiply(a.noemer));
    }
    public Breuk vermenigvuldigen(Breuk a) {
        return van(teller.multiply(a.teller), noemer.multiply(a.noemer));
    }
    public Breuk delen(Breuk a) {
        if (a.teller.signum() == 0) throw new DelingDoorNul(this);
        return van(teller.multiply(a.noemer), noemer.multiply(a.teller));
    }

    /**
     * A division whose divisor is a numeric zero: the fault of v0, which this runtime mirrors. In v1 a filled dividend
     * divided by a numeric zero is leeg with a §9.5 inconsistency of kind EF-005, as a leeg divisor gives leeg with one of
     * kind EF-031 (RUNTIME-SEMANTICS ch. 1.4, point 4), and the truth-maintenance branch answers that
     * before it calls this runtime ({@code ComputeHelpers.divided}). {@link #delen} throws it with the
     * dividend and the message it always had, so whatever caught or matched the {@link ArithmeticException} still does. Both branches keep it as a fault of the fixpoint, which the write-back throws named
     * after the rule it ran in ({@link #inDeRegel}), with the first in its cause chain.
     */
    public static final class DelingDoorNul extends ArithmeticException {
        private static final long serialVersionUID = 1L;
        private final transient Breuk deeltal;
        private final String regel;

        DelingDoorNul(Breuk deeltal) {
            super(Msg.drools("EF-005.bare").text());
            this.deeltal = deeltal;
            this.regel = null;
        }

        private DelingDoorNul(String zin, String regel, Breuk deeltal, Throwable oorzaak) {
            super(zin);
            this.deeltal = deeltal;
            this.regel = regel;
            initCause(oorzaak);
        }

        /** The same division, named: the rule {@code regel} it ran in and its operands, with {@code oorzaak} (what
         *  the consequence or the condition threw) as its cause. */
        public DelingDoorNul inDeRegel(String regel, Throwable oorzaak) {
            String getal = deeltal.isTerminerend() ? deeltal.naarDecimal().toPlainString()
                    : deeltal.teller + "/" + deeltal.noemer;
            return new DelingDoorNul(Msg.drools("EF-005.in-rule", "rule", regel, "dividend", getal).text(), regel, deeltal,
                    oorzaak);
        }

        /** The rule a named division ran in; null where {@link Breuk#delen} threw it and nothing named it yet. */
        public String regel() { return regel; }
    }

    /** Exact sign-aware comparison by cross-multiplication (both denominators are positive, so no sign flip). */
    @Override public int compareTo(Breuk a) {
        return teller.multiply(a.noemer).compareTo(a.teller.multiply(noemer));
    }
    public int vergelijk(Breuk a) { return compareTo(a); }

    /** The exact-decimal test: this fraction terminates iff its reduced denominator's only prime factors are
     *  2 and 5 (a base-10 terminating decimal). */
    public boolean isTerminerend() {
        BigInteger n = noemer;
        BigInteger twee = BigInteger.TWO, vijf = BigInteger.valueOf(5);
        while (n.mod(twee).signum() == 0) n = n.divide(twee);
        while (n.mod(vijf).signum() == 0) n = n.divide(vijf);
        return n.equals(BigInteger.ONE);
    }

    /** The EXACT decimal when the fraction terminates, else the DECIMAL64 quotient, sixteen significant digits rounded
     *  half to even: the decimal a bean holds of a value of the unbounded form that does not terminate. Never throws on
     *  a non-terminating value. */
    public BigDecimal naarDecimal() {
        BigDecimal t = new BigDecimal(teller), n = new BigDecimal(noemer);
        if (isTerminerend()) return t.divide(n);           // exact
        return t.divide(n, MathContext.DECIMAL64);
    }

    /** The §6.1.2 collapse point: round the EXACT rational at {@code scale} decimals with {@code mode} — an
     *  {@code afronding} rounds the true value, never a pre-truncated DECIMAL64 approximation. */
    public BigDecimal naarDecimal(int scale, java.math.RoundingMode mode) {
        return new BigDecimal(teller).divide(new BigDecimal(noemer), scale, mode);
    }

    public boolean isNul() { return teller.signum() == 0; }
    public int signum() { return teller.signum(); }
    public Breuk abs() { return teller.signum() < 0 ? new Breuk(teller.negate(), noemer) : this; }
    public Breuk negate() { return new Breuk(teller.negate(), noemer); }

    // ---- Number contract (quotient at primitive precision; never used on the exact path) ----
    @Override public int intValue()       { return naarDecimal().intValue(); }
    @Override public long longValue()     { return naarDecimal().longValue(); }
    @Override public float floatValue()   { return (float) doubleValue(); }
    @Override public double doubleValue() { return new BigDecimal(teller).divide(new BigDecimal(noemer), MathContext.DECIMAL64).doubleValue(); }

    @Override public boolean equals(Object o) {
        return o instanceof Breuk b && teller.equals(b.teller) && noemer.equals(b.noemer);
    }
    @Override public int hashCode() { return teller.hashCode() * 31 + noemer.hashCode(); }
    @Override public String toString() { return teller + "/" + noemer; }
}
