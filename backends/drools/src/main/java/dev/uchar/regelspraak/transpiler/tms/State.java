package dev.uchar.regelspraak.transpiler.tms;

import dev.uchar.regelspraak.graph.ProducerConsumer.ElementKey;

import java.util.Objects;

/**
 * The standing of one attribute on one object, total: at the fixpoint there is exactly one per (object, attribute) a
 * rule reads through it, a derived attribute or one that can be open, carrying the value of the {@link Value} a rule
 * derived where there is one, and otherwise the value the case input put on the bean, leeg included; where the attribute
 * is open on the object (RUNTIME-SEMANTICS ch. 1.5), its {@link OpenMember}: no {@link Value} stands, and a writer leaves
 * it open ({@link LeftOpen}), or its input is not complete ({@link OpenInput}) and the bean holds none. A condition or
 * a value reads the attribute through this fact, as
 * a positive pattern, so its read is bound in the left-hand side and truth maintenance follows it. The first two sources
 * give the defaults reading of the case input without a rule of their own: a writer that decides replaces the input,
 * and one that computes leeg writes nothing and leaves the input standing (RUNTIME-SEMANTICS ch. 7.8).
 *
 * <p>An attribute on a timeline has a standing wherever a rule reads it, also where no form writes it, so that its input
 * stands in the convention the case gives it: a {@code List<Period>} for a full timeline, an {@link BucketScalar} for
 * the asOfDate-bucket scalar (ch. 8.4).
 *
 * <p>Keyed and compared as {@link HasCharacteristic} is; the value may be null, and then it is leeg.
 */
public record State(Object subject, ElementKey attribute, Object value) {

    public State {
        Objects.requireNonNull(subject, "subject");
        Objects.requireNonNull(attribute, "attribute");
    }

    @Override public boolean equals(Object o) {
        return o instanceof State s && s.subject == subject && s.attribute.equals(attribute)
                && Objects.equals(s.value, value);
    }

    @Override public int hashCode() {
        return Objects.hash(System.identityHashCode(subject), attribute, value);
    }

    @Override public String toString() {
        return "State(" + FactNames.object(subject) + ", " + FactNames.member(attribute) + ", "
                + (value == null ? "empty" : value) + ")";
    }
}
