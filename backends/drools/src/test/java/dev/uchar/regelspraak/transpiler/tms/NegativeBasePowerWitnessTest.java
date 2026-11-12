package dev.uchar.regelspraak.transpiler.tms;

import dev.uchar.regelspraak.runtime.RegelSpraakRuntime;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * A negative base has no fractional power (RUNTIME-SEMANTICS ch. 2.2): the base in front of the power is leeg with a
 * §9.5 deposit of the kind EF-044, as a negative radicand is under the root (ch. 1.2), with and without a rounding, also
 * where the base to the numerator of the exponent is positive. A whole exponent keeps its power. The corpus golden
 * {@code fractional-power-negative-base} carries the deposit through both branches.
 *
 * <p>Red before: the power faulted with EF-044 and broke off the session, where the root of the same negative number
 * was leeg with its deposit.
 */
class NegativeBasePowerWitnessTest {

    private static final BigDecimal MINUS_32 = new BigDecimal("-32");

    @Test void a_negative_base_is_leeg_with_the_deposit_ef_044() {
        for (BigDecimal exponent : List.of(new BigDecimal("0.4"), new BigDecimal("0.2"), new BigDecimal("-0.4"))) {
            Object flag = ComputeHelpers.depositFlag();
            assertNull(ComputeHelpers.negativeBase(MINUS_32, exponent, flag), "(−32)^" + exponent);
            assertEquals(List.of("EF-044"), ComputeHelpers.depositCodes(flag), "(−32)^" + exponent);
        }
    }

    @Test void a_leeg_power_follows_from_the_leeg_base() {
        Object flag = ComputeHelpers.depositFlag();
        Object base = ComputeHelpers.negativeBase(MINUS_32, new BigDecimal("0.4"), flag);
        assertNull(ComputeHelpers.exactPower(base, new BigDecimal("0.4")));
        assertNull(ComputeHelpers.roundedPower(base, new BigDecimal("0.4"), 2, java.math.RoundingMode.FLOOR));
    }

    @Test void a_negative_base_that_reaches_the_power_is_a_defect_of_the_lowering() {
        assertThrows(IllegalStateException.class, () -> ComputeHelpers.exactPower(MINUS_32, new BigDecimal("0.4")));
    }

    @Test void a_positive_base_and_a_whole_exponent_keep_their_power() {
        Object flag = ComputeHelpers.depositFlag();
        BigDecimal base = new BigDecimal("32");
        assertSame(base, ComputeHelpers.negativeBase(base, new BigDecimal("0.4"), flag));
        assertFalse(ComputeHelpers.reported(flag));
        assertEquals(0, new BigDecimal("4").compareTo((BigDecimal) ComputeHelpers.exactPower(base,
                new BigDecimal("0.4"))));
        assertEquals(0, new BigDecimal("-8").compareTo(new BigDecimal(String.valueOf(
                RegelSpraakRuntime.rsMacht(new BigDecimal("-2"), 3)))));
    }
}
