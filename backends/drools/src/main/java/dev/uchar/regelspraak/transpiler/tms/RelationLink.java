package dev.uchar.regelspraak.transpiler.tms;

import java.util.Objects;

/**
 * One link of a relation, of whichever cardinality: {@code first} fills the first declared role of the fact type
 * {@code relation} and {@code second} the second. The branch reads a relation over these facts where a form of the
 * document creates it, or where both its roles are to-many; a relation of the input with a to-one role, which no form
 * creates, it reads off the reference field of its one side. An object creation inserts the link over its connecting
 * relation and a fact creation the link it creates, each stated, so that it never lapses (RUNTIME-SEMANTICS ch. 5.5);
 * the links the case input carries over a created relation are facts as well, inserted logically from the reference
 * fields of the beans, which do not change while the session fires, and those over a relation of two to-many roles,
 * which no bean holds, the input states ({@code DroolsBackend.insert}). A form that reads such a relation, a
 * navigation, the range of a fold or the path of a fact creation, joins over these facts, since this branch writes no
 * bean while it fires and a link that arises then is in no bean field.
 *
 * <p>{@code phase} is the creation phase that made the link ({@link CreationPhase}), and 0 for a link of the input: a fact
 * creation does not walk a link of the phase it fires in, which the rule layer has not yet seen. {@code relation} is the
 * fact type's name in its normalised form. Compared by that name and the two objects by identity, and not by
 * {@code phase}: one link is one fact, however it arose.
 */
public record RelationLink(String relation, Object first, Object second, int phase) {

    public RelationLink {
        Objects.requireNonNull(relation, "relation");
        Objects.requireNonNull(first, "first");
        Objects.requireNonNull(second, "second");
        if (phase < 0) throw new IllegalArgumentException("fase " + phase + " < 0");
    }

    @Override public boolean equals(Object o) {
        return o instanceof RelationLink k && k.first == first && k.second == second && k.relation.equals(relation);
    }

    @Override public int hashCode() {
        return Objects.hash(relation, System.identityHashCode(first), System.identityHashCode(second));
    }

    @Override public String toString() {
        return "RelationLink(" + relation + ", " + FactNames.object(first) + ", " + FactNames.object(second) + ", phase "
                + phase + ")";
    }
}
