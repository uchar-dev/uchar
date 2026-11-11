package dev.uchar.regelspraak.transpiler.tms;

import dev.uchar.regelspraak.runtime.RegelSpraakRuntime;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * A negative base has no fractional power (RUNTIME-SEMANTICS ch. 2.2, "and so is a negative base" for the whole row of
 * the fractional exponent): the power faults with and without its rounding, also where the base to the numerator of
 * the exponent is positive. A whole exponent keeps its power.
 *
 * <p>Red before: (−32)^0,4 = (−32)^(2/5) gave 4, the fifth root of (−32)^2 = 1024, without a rounding and under one.
 */
class NegativeBasePowerWitnessTest {

    private static final BigDecimal MINUS_32 = new BigDecimal("-32");

    @Test void an_even_numerator_does_not_hide_the_negative_base() {
        assertThrows(ComputeFault.class, () -> ComputeHelpers.exactPower(MINUS_32, new BigDecimal("0.4")));
        assertThrows(ComputeFault.class,
                () -> ComputeHelpers.roundedPower(MINUS_32, new BigDecimal("0.4"), 2, RoundingMode.FLOOR));
    }

    @Test void an_odd_numerator_faults_as_before() {
        assertThrows(ComputeFault.class, () -> ComputeHelpers.exactPower(MINUS_32, new BigDecimal("0.2")));
        assertThrows(ComputeFault.class,
                () -> ComputeHelpers.roundedPower(MINUS_32, new BigDecimal("0.2"), 2, RoundingMode.FLOOR));
    }

    @Test void a_positive_base_and_a_whole_exponent_keep_their_power() {
        assertEquals(0, new BigDecimal("4").compareTo((BigDecimal) ComputeHelpers.exactPower(new BigDecimal("32"),
                new BigDecimal("0.4"))));
        assertEquals(0, new BigDecimal("-8").compareTo(new BigDecimal(String.valueOf(
                RegelSpraakRuntime.rsMacht(new BigDecimal("-2"), 3)))));
    }
}
