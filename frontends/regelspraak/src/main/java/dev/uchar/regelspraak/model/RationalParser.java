package dev.uchar.regelspraak.model;

import java.math.BigDecimal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * §13.2.5 numeric-literal parsing — the single home for reading a literal's leading number into an exact
 * rational. Shared by the resolver (which needs the literal's precision, {@code ExpressionBuilder}) and the
 * transpiler (which lowers the number, {@code DrlTranspiler}); the regex, the group convention, and the
 * {@code -0_b/c} sign fix all live in one place rather than in two byte-identical copies.
 */
public final class RationalParser {
    private RationalParser() {}

    /** Group 1 = whole part (signed); 2 = "a,b"/"a.b" decimal fraction; 3 = mixed-numerator "a_b/c";
     *  4 = denominator "/c". */
    private static final Pattern PATTERN = Pattern.compile("(-?\\d+)(?:[.,](\\d+)|(?:_(\\d+))?/(\\d+))?");

    /**
     * Numerator and denominator of a numeric literal's leading number (denominator ONE for a plain integer or
     * decimal), or {@code null} when there is no number. Recognises the §13.2.5 rational forms — "a/c" and the
     * mixed "a_b/c" (= a + b/c, the fraction taking the whole part's sign) — alongside a bare integer and an
     * "a,b" (or "a.b") decimal. Matching only the leading "a" would silently drop "/c", so "maal 4/2" must
     * lower to ·2 (not ·4) and "macht 4/2" to pow(2).
     */
    public static BigDecimal[] of(String source) {
        Matcher m = PATTERN.matcher(source);
        if (!m.find()) return null;
        BigDecimal whole = new BigDecimal(m.group(1));
        if (m.group(2) != null) // a,b decimal
            return new BigDecimal[]{ new BigDecimal(m.group(1) + "." + m.group(2)), BigDecimal.ONE };
        if (m.group(4) == null) // bare integer
            return new BigDecimal[]{ whole, BigDecimal.ONE };
        BigDecimal denominator = new BigDecimal(m.group(4));
        if (m.group(3) == null) // a/c
            return new BigDecimal[]{ whole, denominator };
        BigDecimal numerator = whole.abs().multiply(denominator).add(new BigDecimal(m.group(3)));
        // The sign comes from the leading '-' in the text, not whole.signum(): for "-0_1/2" the whole part is
        // BigDecimal("-0"), whose signum() is 0, so signum() would drop the minus (-0.5 -> +0.5).
        return new BigDecimal[]{ m.group(1).startsWith("-") ? numerator.negate() : numerator, denominator }; // a_b/c
    }
}
