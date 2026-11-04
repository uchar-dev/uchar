package dev.uchar.regelspraak.transpiler.tms;

import dev.uchar.regelspraak.runtime.Breuk;
import dev.uchar.regelspraak.transpiler.core.Namen;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * A constructor value of a sum type (RUNTIME-SEMANTICS ch. 9.1): the tag {@code constructor} and, for a unary
 * constructor, its payload, never leeg; null for a nullary one. A value of this branch in working memory, on a bean and
 * in a segment of a timeline ({@link Period}), as a case and a {@code Construct} give it. An absence marker of ch. 9.5
 * is such a value, {@code onbekend} or {@code niet van toepassing}, and the value of an attribute with markers is one of
 * the constructor {@code waarde}.
 *
 * <p>Two constructor values are equal where their tags are one declared name (WF-001, the identity the reference
 * interpreter compares tags by) and their payloads are equal values, numbers by value, so that 300 and 300.00 are one
 * payload, as the write boundary and the change detection of ch. 3.1 read a number (ch. 9.3). The hash code agrees with
 * that equality, over the normalised tag and a number without trailing zeros: the KieBase runs in {@code EQUALITY}, where
 * two equal logical facts are one fact. The tag keeps the spelling it was given, which a reading prints.
 */
public record ConstructorValue(String constructor, Object payload) {

    public ConstructorValue {
        Objects.requireNonNull(constructor, "constructor");
    }

    @Override public boolean equals(Object o) {
        return o instanceof ConstructorValue c && Namen.norm(c.constructor).equals(Namen.norm(constructor))
                && samePayload(c.payload, payload);
    }

    @Override public int hashCode() {
        return Objects.hash(Namen.norm(constructor), payloadKey(payload));
    }

    @Override public String toString() {
        return payload == null ? constructor : constructor + "(" + (payload instanceof BigDecimal d
                ? d.toPlainString() : payload) + ")";
    }

    /** Two payloads as ch. 9.3 compares them: numbers by value, a rational among them; any other by its equality. */
    private static boolean samePayload(Object a, Object b) {
        if (a instanceof Number) return b instanceof Number && Breuk.van(a).compareTo(Breuk.van(b)) == 0;
        return Objects.equals(a, b);
    }

    private static Object payloadKey(Object p) {
        if (p instanceof BigDecimal d) return d.signum() == 0 ? BigDecimal.ZERO : d.stripTrailingZeros();
        if (p instanceof Breuk b) return b.isTerminerend() ? payloadKey(b.naarDecimal()) : b;
        return p;
    }
}
