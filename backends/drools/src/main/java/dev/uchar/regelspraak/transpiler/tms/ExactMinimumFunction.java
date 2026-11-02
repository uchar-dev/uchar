package dev.uchar.regelspraak.transpiler.tms;

import java.io.ObjectInput;
import java.io.ObjectOutput;

/**
 * The least of the values a fold is given (RUNTIME-SEMANTICS ch. 3.4): two numbers compared exactly, a decimal and a
 * rational that does not terminate among them ({@link ExactMaximumFunction#compare}), and any other values by their
 * own order; null over no value. Both renderings fold with it: the rendering into closures by its class, the DRL text
 * through {@code import accumulate} under the name {@link #NAME}.
 */
public final class ExactMinimumFunction
        implements org.kie.api.runtime.rule.AccumulateFunction<ExactMaximumFunction.Extreme> {

    /** The name a DRL text imports this function under. */
    public static final String NAME = "exactMin";

    @Override public ExactMaximumFunction.Extreme createContext() { return new ExactMaximumFunction.Extreme(); }

    @Override public void init(ExactMaximumFunction.Extreme e) { e.value = null; }

    @Override public void accumulate(ExactMaximumFunction.Extreme e, Object value) {
        if (e.value == null || ExactMaximumFunction.compare(value, e.value) < 0) e.value = value;
    }

    @Override public void reverse(ExactMaximumFunction.Extreme e, Object value) {
        throw new UnsupportedOperationException("de minimumvouw keert niet om; Drools vouwt opnieuw");
    }

    @Override public Object getResult(ExactMaximumFunction.Extreme e) { return e.value; }

    @Override public boolean supportsReverse() { return false; }

    @Override public Class<?> getResultType() { return Object.class; }

    @Override public void writeExternal(ObjectOutput out) {}

    @Override public void readExternal(ObjectInput in) {}
}
