package dev.uchar.regelspraak.transpiler.tms;

import dev.uchar.regelspraak.graph.ProducerConsumer.ElementKey;

import java.util.Objects;

/**
 * A member that is open on its object (RUNTIME-SEMANTICS ch. 1.5): neither filled nor settled leeg. A {@link State}, a
 * {@link CharacteristicState} and a {@link PathState} carry it in place of a value or a truth value, so a rule binds it as
 * it binds any read, and the evaluation that reaches the read breaks off there ({@link ComputeHelpers#read},
 * {@link OpenRead}). It names the object and the member read open, as the reading of a
 * condition names them (ch. 5.12). Compared as the standing facts are: the subject by identity.
 */
public record OpenMember(Object subject, ElementKey member) {

    public OpenMember {
        Objects.requireNonNull(subject, "subject");
        Objects.requireNonNull(member, "member");
    }

    @Override public boolean equals(Object o) {
        return o instanceof OpenMember m && m.subject == subject && m.member.equals(member);
    }

    @Override public int hashCode() {
        return 31 * System.identityHashCode(subject) + member.hashCode();
    }

    @Override public String toString() {
        return "open " + FactNames.object(subject) + " " + FactNames.member(member);
    }
}
