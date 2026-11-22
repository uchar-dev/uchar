package dev.uchar.regelspraak.transpiler.tms;

import dev.uchar.regelspraak.graph.ProducerConsumer.ElementKey;

import java.util.Objects;

/**
 * The standing of one characteristic on one object, total: at the fixpoint there is exactly one per (object,
 * characteristic) a rule reads, in one of the three states of RUNTIME-SEMANTICS ch. 1.5. {@code holds} is
 * {@code Boolean.TRUE} where the case input sets the characteristic or a {@link HasCharacteristic} for it exists,
 * the {@link OpenMember} of the characteristic where neither holds and a writer leaves it open ({@link LeftOpen}) or
 * the input of an input characteristic is not complete ({@link OpenInput}), and {@code Boolean.FALSE} otherwise. A
 * condition reads the characteristic through this fact, as a positive pattern, whichever polarity it tests, so the
 * negations over a characteristic sit in the generic rules that derive the false and the open standing
 * ({@code not HasCharacteristic}), and a check in a condition, negated or inside a composite, is a predicate over a
 * bound value. Reading {@code not HasCharacteristic} in every consumer instead would spread the negation over every
 * rule and leave the input, which a case sets on the bean, as a second source each consumer has to combine with it.
 *
 * <p>The standing is a logical fact like {@link HasCharacteristic}: when the derivation of the characteristic appears or
 * disappears, the old standing is retracted, the new one inserted, and every conclusion that read the old one goes
 * with it. Keyed and compared as {@link HasCharacteristic} is: the characteristic on its declaring type, the subject by
 * identity.
 */
public record CharacteristicState(Object subject, ElementKey characteristic, Object holds) {

    public CharacteristicState {
        Objects.requireNonNull(subject, "subject");
        Objects.requireNonNull(characteristic, "characteristic");
        if (!(holds instanceof Boolean) && !(holds instanceof OpenMember))
            throw new IllegalArgumentException("a characteristic holds, does not hold, or is open, not " + holds);
    }

    @Override public boolean equals(Object o) {
        return o instanceof CharacteristicState s && s.subject == subject && s.characteristic.equals(characteristic)
                && s.holds.equals(holds);
    }

    @Override public int hashCode() {
        return (31 * System.identityHashCode(subject) + characteristic.hashCode()) * 2
                + (holds instanceof Boolean b ? (b ? 1 : 0) : holds.hashCode());
    }

    @Override public String toString() {
        return "CharacteristicState(" + FactNames.object(subject) + ", " + FactNames.member(characteristic) + ", "
                + holds + ")";
    }
}
