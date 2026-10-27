package dev.uchar.regelspraak.transpiler.tms;

import dev.uchar.regelspraak.graph.ProducerConsumer.ElementKey;

import java.util.Objects;

/**
 * A derived member read over a path of roles from one object, total: exactly one per (object, path, member) at the
 * fixpoint, also where a link on the path is absent. The value is the standing of the member on the object the path
 * reaches ({@link CharacteristicState}'s truth value, {@link State}'s value), and null where a link is absent. A read over an
 * absent link is leeg (RUNTIME-SEMANTICS ch. 5.8 rule 1), and a characteristic check over it holds in neither
 * polarity (rule 3), so for a characteristic the value has three states: waar, onwaar, and null for absent. Without
 * this fact the rule would have to join the reached object's standing directly, and a subject without the link would
 * have no match at all, where a leeg test or a negated comparison over it must still hold.
 *
 * <p>{@code path} names the subject type and the reference fields the path walks; the subject is compared by identity.
 * {@code reached} is the object the path reaches, the one whose standing {@code value} carries, compared by identity,
 * and null where a link is absent: the object a reading of a condition names for a node that reads the member over
 * this path (RUNTIME-SEMANTICS ch. 5.12, {@link ConditionReading.NodeReading#object}).
 */
public record PathState(Object subject, String path, ElementKey member, Object value, Object reached) {

    public PathState {
        Objects.requireNonNull(subject, "subject");
        Objects.requireNonNull(path, "path");
        Objects.requireNonNull(member, "member");
    }

    @Override public boolean equals(Object o) {
        return o instanceof PathState p && p.subject == subject && p.path.equals(path) && p.member.equals(member)
                && Objects.equals(p.value, value) && p.reached == reached;
    }

    @Override public int hashCode() {
        return Objects.hash(System.identityHashCode(subject), path, member, value, System.identityHashCode(reached));
    }

    @Override public String toString() {
        return "PathState(" + FactNames.object(subject) + ", " + path + ", " + FactNames.member(member) + ", "
                + (value == null ? "absent or empty" : value) + ", "
                + (reached == null ? "no object reached" : FactNames.object(reached)) + ")";
    }
}
