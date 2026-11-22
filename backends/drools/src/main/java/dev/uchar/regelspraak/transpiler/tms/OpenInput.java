package dev.uchar.regelspraak.transpiler.tms;

import dev.uchar.regelspraak.graph.ProducerConsumer.ElementKey;

import java.util.Objects;

/**
 * An input member of {@code subject} whose input is not complete (RUNTIME-SEMANTICS ch. 1.5, "The three states",
 * "Complete input"): where the bean holds no value for it, an attribute without one or a characteristic that does not
 * hold, the member is open, not settled leeg. Stated beside the beans by {@code DroolsBackend.insert}, the one place
 * that reads the document's {@code inferFromEmpty}: in a document without it there is none. A member is input on an
 * object where no rule and no defaults group writes it on the object's type; a characteristic the case states not to
 * hold ({@code characteristicsAbsent}) and a member declared complete have none, and a session that treats every input
 * member as complete has none at all. The standing of the member reads it together with the bean, so a value a consumer
 * puts on the bean later fills the member ({@code TmsTranspiler#attributeStateRules}).
 *
 * <p>Keyed and compared as {@link HasCharacteristic} is: the member on its declaring type, the subject by identity.
 */
public record OpenInput(Object subject, ElementKey member) {

    public OpenInput {
        Objects.requireNonNull(subject, "subject");
        Objects.requireNonNull(member, "member");
    }

    @Override public boolean equals(Object o) {
        return o instanceof OpenInput i && i.subject == subject && i.member.equals(member);
    }

    @Override public int hashCode() {
        return 31 * System.identityHashCode(subject) + member.hashCode();
    }

    @Override public String toString() {
        return "OpenInput(" + FactNames.object(subject) + ", " + FactNames.member(member) + ")";
    }
}
