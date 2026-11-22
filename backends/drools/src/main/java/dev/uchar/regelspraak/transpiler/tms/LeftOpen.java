package dev.uchar.regelspraak.transpiler.tms;

import dev.uchar.regelspraak.graph.ProducerConsumer.ElementKey;

import java.util.Objects;

/**
 * A writer that leaves {@code member} of {@code subject} open (RUNTIME-SEMANTICS ch. 1.5, "A rule"): on that object it
 * neither decides nor stands to fill nothing, since its condition is neither true for itself nor false for another, or
 * its value has an open read; a defaults stack leaves its target open through the first of its defaults that is open, or
 * through the reconciliation at its root. Inserted logically by the rule of the writer ({@code plan.Consequence.Writer},
 * {@code plan.Consequence.LeavesOpen}), so it goes when what it rested on changes. The
 * standing of the member reads it: a {@link Value} or a {@link HasCharacteristic} fills the member, else this fact leaves
 * it open, else the input stands. {@code writer} is the name of the writer's form, of a stack with the place of its
 * default, so two writers that leave one member open stand as two facts.
 *
 * <p>Keyed and compared as {@link Value} is: the member on its declaring type, the subject by identity.
 */
public record LeftOpen(Object subject, ElementKey member, String writer) {

    public LeftOpen {
        Objects.requireNonNull(subject, "subject");
        Objects.requireNonNull(member, "member");
        Objects.requireNonNull(writer, "writer");
    }

    @Override public boolean equals(Object o) {
        return o instanceof LeftOpen l && l.subject == subject && l.member.equals(member) && l.writer.equals(writer);
    }

    @Override public int hashCode() {
        return Objects.hash(System.identityHashCode(subject), member, writer);
    }

    @Override public String toString() {
        return "LeftOpen(" + FactNames.object(subject) + ", " + FactNames.member(member) + ", " + writer + ")";
    }
}
