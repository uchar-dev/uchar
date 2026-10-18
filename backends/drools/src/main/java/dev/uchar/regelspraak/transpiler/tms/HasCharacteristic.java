package dev.uchar.regelspraak.transpiler.tms;

import dev.uchar.regelspraak.graaf.ProducentConsument.ElementSleutel;

import java.util.Objects;

/**
 * A characteristic a rule of the exec-model branch derived for one object. The branch inserts it with
 * {@code drools.logicalInsert}, so it stands exactly as long as the condition of a rule that inserted it holds, and
 * it goes, with everything that rested on it, when that condition stops holding. The case input stays on the beans;
 * this fact is the derivation, and {@code DroolsBackend.zetAfgeleideWaarden} writes it onto the bean only after the
 * fire.
 *
 * <p>The characteristic is keyed on the type that declares it ({@code ProducentConsument.elementVan}), so a rule that
 * attributes it on a subtype and a check that reads it on the supertype meet on one key. The subject is compared by
 * identity: the generated beans keep Object's identity, and two objects of a case that look alike are two objects.
 * Truth maintenance merges equal logical facts by this equality, whatever the KieBase's equality mode: two rules that
 * derive the same characteristic for the same object insert one fact with a justification each, and it stays while
 * either holds.
 */
public record HasCharacteristic(Object onderwerp, ElementSleutel kenmerk) {

    public HasCharacteristic {
        Objects.requireNonNull(onderwerp, "onderwerp");
        Objects.requireNonNull(kenmerk, "kenmerk");
    }

    @Override public boolean equals(Object o) {
        return o instanceof HasCharacteristic h && h.onderwerp == onderwerp && h.kenmerk.equals(kenmerk);
    }

    @Override public int hashCode() {
        return 31 * System.identityHashCode(onderwerp) + kenmerk.hashCode();
    }

    @Override public String toString() {
        return "HeeftKenmerk(" + FactNames.object(onderwerp) + ", " + FactNames.member(kenmerk) + ")";
    }
}
