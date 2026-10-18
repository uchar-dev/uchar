package dev.uchar.regelspraak.transpiler.tms;

import dev.uchar.regelspraak.graph.ProducerConsumer.ElementKey;

import java.util.Objects;

/**
 * The case input of the attribute {@code attribute} of {@code onderwerp}, an attribute on a timeline, given in the
 * asOfDate-bucket convention (RUNTIME-SEMANTICS ch. 8.4): one scalar, the value of the period of the attribute's grid
 * that holds the evaluation date. The bean carries that scalar as the one segment of that period, which a full timeline
 * of one segment would be as well, so the case input states this fact beside the beans, and the branch reads the input
 * in the convention the case gave it ({@link BucketScalar}). The two conventions are not interchangeable: the bucket
 * scalar serves the subject's own total without a clip, the full timeline serves every other read, and a form that
 * needs the one and finds the other faults ({@link InputFormFault}).
 *
 * <p>Stated, never derived: inserted with the beans, one per bucket scalar, and never retracted while the session fires.
 * A session that states none has every input timeline read as a full timeline. {@code attribute} is the attribute's key
 * by its declaring type, as {@code ProducerConsumer.elementOf} gives it for the instance's own type. Compared by the
 * object's identity and the key.
 */
public record BucketInput(Object onderwerp, ElementKey attribute) {

    public BucketInput {
        Objects.requireNonNull(onderwerp, "onderwerp");
        Objects.requireNonNull(attribute, "attribuut");
    }

    @Override public boolean equals(Object o) {
        return o instanceof BucketInput e && e.onderwerp == onderwerp && e.attribute.equals(attribute);
    }

    @Override public int hashCode() {
        return Objects.hash(System.identityHashCode(onderwerp), attribute);
    }

    @Override public String toString() {
        return "Emmerinvoer(" + FactNames.object(onderwerp) + ", " + FactNames.member(attribute) + ")";
    }
}
