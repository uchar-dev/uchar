package dev.uchar.regelspraak;

import dev.uchar.regelspraak.model.RationalParser;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/** §13.2.5: the shared numeric-literal -> {numerator, denominator} parser used by both the resolver
 *  (precision) and the transpiler (lowering). Pins each form and the -0_b/c sign subtlety. */
class RationalTest {

    private static void assertRational(String bron, String numerator, String denominator) {
        BigDecimal[] r = RationalParser.van(bron);
        assertNotNull(r, () -> "geen getal in: " + bron);
        assertEquals(0, new BigDecimal(numerator).compareTo(r[0]), () -> "teller van " + bron + " = " + r[0]);
        assertEquals(0, new BigDecimal(denominator).compareTo(r[1]), () -> "noemer van " + bron + " = " + r[1]);
    }

    @Test void integer_decimal_rational_and_mixed() {
        assertRational("5", "5", "1");                 // bare integer
        assertRational("-12", "-12", "1");             // negative integer
        assertRational("0,50", "0.50", "1");           // a,b decimal (comma)
        assertRational("3.25", "3.25", "1");           // a.b decimal (dot)
        assertRational("3/2", "3", "2");               // a/c fraction
        assertRational("1_1/2", "3", "2");             // mixed a_b/c = 1 + 1/2 = 3/2
    }

    @Test void the_minus_zero_rational_keeps_its_sign() {
        // -0_1/2 = -0.5: the whole part BigDecimal("-0") has signum 0, so the sign MUST come from the leading
        // '-' in the text, not whole.signum() (which would yield +0.5).
        assertRational("-0_1/2", "-1", "2");
    }

    @Test void no_number_is_null() {
        assertNull(RationalParser.van("geen cijfer hier"));
    }

    @Test void leading_words_are_skipped() {
        // find() (not matches()) so a number embedded in a phrase is still read.
        assertRational("maal 4/2", "4", "2");
    }
}
