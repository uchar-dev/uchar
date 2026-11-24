package dev.uchar.regelspraak.transpiler.tms;

import dev.uchar.regelspraak.graph.ProducerConsumer.ElementKey;

import java.util.Objects;

/**
 * A member that is open on its object (RUNTIME-SEMANTICS ch. 1.5): neither filled nor settled leeg. A {@link State}, a
 * {@link CharacteristicState} and a {@link PathState} carry it in place of a value or a truth value, so a rule binds it as
 * it binds any read, and the evaluation that reaches the read breaks off there ({@link ComputeHelpers#read},
 * {@link OpenRead}). It names the object and the member read open, as the reading of a
 * condition names them (ch. 5.12). Compared as the standing facts are: the subject by identity.
 *
 * <p>Or, where {@code fold} is not null and {@code member} is, the open read of a fold that this branch reads without
 * the order of its members ({@link #ofFold}): the fold {@code fold} on {@code subject} is open, and which of its members
 * leaves it so, the first in the order of the fold (ch. 5.12, "Open values"), a reading names from the facts of that
 * fold ({@code SessionReaders}). Kleene's table over such a fold does not depend on that order (ch. 1.5, 5.11), so
 * neither the outcome nor a fact of the session does, also where the fold lies in the filter of another.
 */
public record OpenMember(Object subject, ElementKey member, String fold) {

    public OpenMember {
        Objects.requireNonNull(subject, "subject");
        if ((member == null) == (fold == null))
            throw new IllegalArgumentException("an open read names a member or a fold, not " + member + " and " + fold);
    }

    /** The open read of {@code member} on {@code subject}. */
    public OpenMember(Object subject, ElementKey member) {
        this(subject, member, null);
    }

    /** The open read of the fold {@code fold} on {@code subject}, whose first member that leaves it open a reading names. */
    static OpenMember ofFold(String fold, Object subject) {
        return new OpenMember(subject, null, Objects.requireNonNull(fold, "fold"));
    }

    @Override public boolean equals(Object o) {
        return o instanceof OpenMember m && m.subject == subject && Objects.equals(m.member, member)
                && Objects.equals(m.fold, fold);
    }

    @Override public int hashCode() {
        return 31 * System.identityHashCode(subject) + Objects.hash(member, fold);
    }

    @Override public String toString() {
        return member != null ? "open " + FactNames.object(subject) + " " + FactNames.member(member)
                : "open fold " + fold + " on " + FactNames.object(subject);
    }
}
