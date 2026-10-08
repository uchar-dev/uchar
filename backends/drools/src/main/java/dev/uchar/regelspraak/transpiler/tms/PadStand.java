package dev.uchar.regelspraak.transpiler.tms;

import dev.uchar.regelspraak.graaf.ProducentConsument.ElementSleutel;

import java.util.Objects;

/**
 * A derived member read over a path of roles from one object, total: exactly one per (object, path, member) at the
 * fixpoint, also where a link on the path is absent. The value is the standing of the member on the object the path
 * reaches ({@link KenmerkStand}'s truth value, {@link Stand}'s value), and null where a link is absent. A read over an
 * absent link is leeg (RUNTIME-SEMANTICS ch. 5.8 rule 1), and a characteristic check over it holds in neither
 * polarity (rule 3), so for a characteristic the value has three states: waar, onwaar, and null for absent. Without
 * this fact the rule would have to join the reached object's standing directly, and a subject without the link would
 * have no match at all, where a leeg test or a negated comparison over it must still hold.
 *
 * <p>{@code pad} names the subject type and the reference fields the path walks; the subject is compared by identity.
 */
public record PadStand(Object onderwerp, String pad, ElementSleutel lid, Object waarde) {

    public PadStand {
        Objects.requireNonNull(onderwerp, "onderwerp");
        Objects.requireNonNull(pad, "pad");
        Objects.requireNonNull(lid, "lid");
    }

    @Override public boolean equals(Object o) {
        return o instanceof PadStand p && p.onderwerp == onderwerp && p.pad.equals(pad) && p.lid.equals(lid)
                && Objects.equals(p.waarde, waarde);
    }

    @Override public int hashCode() {
        return Objects.hash(System.identityHashCode(onderwerp), pad, lid, waarde);
    }

    @Override public String toString() {
        return "PadStand(" + Feitnamen.object(onderwerp) + ", " + pad + ", " + Feitnamen.lid(lid) + ", "
                + (waarde == null ? "afwezig of leeg" : waarde) + ")";
    }
}
