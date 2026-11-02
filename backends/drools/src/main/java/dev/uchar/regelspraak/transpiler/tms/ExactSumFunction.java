package dev.uchar.regelspraak.transpiler.tms;

import dev.uchar.regelspraak.runtime.Breuk;

import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * The sum of a fold over the values it is given, exact (RUNTIME-SEMANTICS ch. 2.1, 3.4): a decimal is added as the
 * exact {@code BigDecimal} it is and a rational that does not terminate as the {@link Breuk} it is, so three times 1/3
 * sums to 1. Over decimals alone the sum is the {@code BigDecimal} sum, at the scale it has; with a rational among them
 * it is the exact rational, a decimal where it terminates. Over no value it is 0. It reverses a value it was given,
 * as truth maintenance retracts a contribution. Both renderings fold with it: the rendering into closures by its
 * class, the DRL text through {@code import accumulate} under the name {@link #NAME}.
 */
public final class ExactSumFunction implements org.kie.api.runtime.rule.AccumulateFunction<ExactSumFunction.Sum> {

    /** The name a DRL text imports this function under. */
    public static final String NAME = "exactSum";

    /** The sum so far, its decimals and its rationals apart, so that a fold over decimals alone stays a decimal. */
    public static final class Sum implements Serializable {
        private static final long serialVersionUID = 1L;
        BigDecimal decimals = BigDecimal.ZERO;
        Breuk rationals;
    }

    @Override public Sum createContext() { return new Sum(); }

    @Override public void init(Sum s) {
        s.decimals = BigDecimal.ZERO;
        s.rationals = null;
    }

    @Override public void accumulate(Sum s, Object value) {
        if (value instanceof Breuk b) s.rationals = s.rationals == null ? b : s.rationals.optellen(b);
        else s.decimals = s.decimals.add((BigDecimal) value);
    }

    @Override public void reverse(Sum s, Object value) {
        if (value instanceof Breuk b) s.rationals = s.rationals.aftrekken(b);
        else s.decimals = s.decimals.subtract((BigDecimal) value);
    }

    @Override public Object getResult(Sum s) {
        if (s.rationals == null) return s.decimals;
        Breuk total = Breuk.van(s.decimals).optellen(s.rationals);
        return total.isTerminerend() ? total.naarDecimal() : total;
    }

    @Override public boolean supportsReverse() { return true; }

    @Override public Class<?> getResultType() { return Object.class; }

    @Override public void writeExternal(ObjectOutput out) {}

    @Override public void readExternal(ObjectInput in) {}
}
