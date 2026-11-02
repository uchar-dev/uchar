package dev.uchar.regelspraak.transpiler.tms;

import dev.uchar.regelspraak.runtime.Breuk;

import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.Serializable;

/**
 * The greatest of the values a fold is given (RUNTIME-SEMANTICS ch. 3.4), compared as {@link #compare} compares them;
 * null over no value. Both renderings fold with it: the rendering into closures by its class, the DRL text through
 * {@code import accumulate} under the name {@link #NAME}.
 */
public final class ExactMaximumFunction implements org.kie.api.runtime.rule.AccumulateFunction<ExactMaximumFunction.Extreme> {

    /** The name a DRL text imports this function under. */
    public static final String NAME = "exactMax";

    /** The least or the greatest value so far. */
    public static final class Extreme implements Serializable {
        private static final long serialVersionUID = 1L;
        Object value;
    }

    /** The order of two values of a fold: numbers by their exact value, so that a decimal and a rational that does not
     *  terminate compare as the rationals they are, and any other values, dates and texts, by their own order. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    static int compare(Object a, Object b) {
        if (a instanceof Number && b instanceof Number) return Breuk.van(a).compareTo(Breuk.van(b));
        return ((Comparable) a).compareTo(b);
    }

    @Override public Extreme createContext() { return new Extreme(); }

    @Override public void init(Extreme e) { e.value = null; }

    @Override public void accumulate(Extreme e, Object value) {
        if (e.value == null || compare(value, e.value) > 0) e.value = value;
    }

    @Override public void reverse(Extreme e, Object value) {
        throw new UnsupportedOperationException("de maximumvouw keert niet om; Drools vouwt opnieuw");
    }

    @Override public Object getResult(Extreme e) { return e.value; }

    @Override public boolean supportsReverse() { return false; }

    @Override public Class<?> getResultType() { return Object.class; }

    @Override public void writeExternal(ObjectOutput out) {}

    @Override public void readExternal(ObjectInput in) {}
}
