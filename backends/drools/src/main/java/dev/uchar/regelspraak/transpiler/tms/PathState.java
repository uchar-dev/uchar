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
 */
public record PathState(Object subject, String path, ElementKey member, Object value) {

    public PathState {
        Objects.requireNonNull(subject, "subject");
        Objects.requireNonNull(path, "path");
        Objects.requireNonNull(member, "member");
    }

    @Override public boolean equals(Object o) {
        return o instanceof PathState p && p.subject == subject && p.path.equals(path) && p.member.equals(member)
                && Objects.equals(p.value, value);
    }

    @Override public int hashCode() {
        return Objects.hash(System.identityHashCode(subject), path, member, value);
    }

    @Override public String toString() {
        return "PathState(" + FactNames.object(subject) + ", " + path + ", " + FactNames.member(member) + ", "
                + (value == null ? "absent or empty" : value) + ")";
    }
}
