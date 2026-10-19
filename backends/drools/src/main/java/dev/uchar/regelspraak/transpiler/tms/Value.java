package dev.uchar.regelspraak.transpiler.tms;

import dev.uchar.regelspraak.graph.ProducerConsumer.ElementKey;

import java.util.Objects;

/**
 * The value a computing rule or an initialisation of the exec-model branch derived for one attribute of one
 * object, inserted logically: it stands as long as the condition of the rule and every value the rule read hold, and is
 * replaced when one of them changes. {@code writer} is the name of the rule's form, so a write-back that meets two
 * different values on one attribute of one object can name both writers, and an initialisation's guard tells another
 * writer's value from its own write (RUNTIME-SEMANTICS ch. 5.4). The value is the one the write boundary leaves: an
 * exact rational collapsed to its decimal. It is null, leeg, only for a computing rule outside the standing domain,
 * which writes a computed leeg (ch. 1.2g): it empties the target, input included, and is a value that can differ from
 * another writer's (ch. 5.7). A standing writer writes nothing for a computed leeg (ch. 7.8), and an initialisation
 * that computes leeg writes nothing either, since its target is empty where it fires.
 *
 * <p>Keyed and compared as {@link HasCharacteristic} is: the attribute on its declaring type, the subject by identity, and
 * the value and the writer by their own equality.
 */
public record Value(Object subject, ElementKey attribute, Object value, String writer) {

    public Value {
        Objects.requireNonNull(subject, "subject");
        Objects.requireNonNull(attribute, "attribuut");
        Objects.requireNonNull(writer, "schrijver");
    }

    @Override public boolean equals(Object o) {
        return o instanceof Value w && w.subject == subject && w.attribute.equals(attribute)
                && Objects.equals(w.value, value) && w.writer.equals(writer);
    }

    @Override public int hashCode() {
        return Objects.hash(System.identityHashCode(subject), attribute, value, writer);
    }

    @Override public String toString() {
        return "Waarde(" + FactNames.object(subject) + ", " + FactNames.member(attribute) + ", "
                + (value == null ? "leeg" : value) + ", " + writer + ")";
    }
}
