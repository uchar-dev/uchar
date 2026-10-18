package dev.uchar.regelspraak.transpiler.tms;

import dev.uchar.regelspraak.graph.ProducerConsumer.ElementKey;

import java.util.Objects;

/**
 * The standing of one derived characteristic on one object, total: at the fixpoint there is exactly one per (object,
 * characteristic), {@code waar} when the case input sets the characteristic or a {@link HasCharacteristic} for it exists,
 * {@code onwaar} otherwise. A condition reads a derived characteristic through this fact, as a positive pattern,
 * whichever polarity it tests. The only negation over a characteristic then sits in the generic rule that derives the
 * {@code onwaar} standing ({@code not HasCharacteristic}), and a check in a condition, negated or inside a composite, is a
 * predicate over a bound value. Reading {@code not HasCharacteristic} in every consumer instead would spread the negation
 * over every rule and leave the input, which a case sets on the bean, as a second source each consumer has to
 * combine with it.
 *
 * <p>The standing is a logical fact like {@link HasCharacteristic}: when the derivation of the characteristic appears or
 * disappears, the old standing is retracted, the new one inserted, and every conclusion that read the old one goes
 * with it. Keyed and compared as {@link HasCharacteristic} is: the characteristic on its declaring type, the subject by
 * identity.
 */
public record CharacteristicState(Object onderwerp, ElementKey characteristic, boolean holds) {

    public CharacteristicState {
        Objects.requireNonNull(onderwerp, "onderwerp");
        Objects.requireNonNull(characteristic, "kenmerk");
    }

    @Override public boolean equals(Object o) {
        return o instanceof CharacteristicState s && s.onderwerp == onderwerp && s.characteristic.equals(characteristic) && s.holds == holds;
    }

    @Override public int hashCode() {
        return (31 * System.identityHashCode(onderwerp) + characteristic.hashCode()) * 2 + (holds ? 1 : 0);
    }

    @Override public String toString() {
        return "KenmerkStand(" + FactNames.object(onderwerp) + ", " + FactNames.member(characteristic) + ", "
                + (holds ? "waar" : "onwaar") + ")";
    }
}
