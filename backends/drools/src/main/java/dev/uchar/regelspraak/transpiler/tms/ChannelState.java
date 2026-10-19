package dev.uchar.regelspraak.transpiler.tms;

import java.util.Objects;

/**
 * The standing of the §9.5 channel of one producing form, total: at the fixpoint there is exactly one per form a
 * lowered condition reads, {@code inconsistent} where an inconsistency that form produced stands, on any subject,
 * and not otherwise. "regelversie X is inconsistent" is an existential test over the channel with no subject of its
 * own (RUNTIME-SEMANTICS ch. 5.6); a condition reads it through this fact, as a positive pattern, so the negation over
 * the channel sits in one generic rule and a reading under a negating composite is a predicate over a bound value.
 *
 * <p>A logical fact like the standings of members: when the last inconsistency of the form is withdrawn, the
 * {@code inconsistent} standing goes and every conclusion that read it with it. Compared by value: the producer's name
 * as the channel test spells it, and the flag.
 */
public record ChannelState(String rule, boolean inconsistent) {

    public ChannelState {
        Objects.requireNonNull(rule, "rule");
    }

    @Override public String toString() {
        return "KanaalStand(" + rule + ", " + (inconsistent ? "inconsistent" : "consistent") + ")";
    }
}
