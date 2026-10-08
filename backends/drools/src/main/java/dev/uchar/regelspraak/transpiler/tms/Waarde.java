package dev.uchar.regelspraak.transpiler.tms;

import dev.uchar.regelspraak.graaf.ProducentConsument.ElementSleutel;

import java.util.Objects;

/**
 * The value a computing rule of the truth-maintenance branch derived for one attribute of one object, inserted
 * logically: it stands as long as the condition of the rule and every value the rule read hold, and is replaced when
 * one of them changes. {@code schrijver} is the name of the rule's form, so a write-back that meets two different
 * values on one attribute of one object can name both writers. The value is the one the write boundary leaves: an
 * exact rational collapsed to its decimal, never leeg, since a computed leeg writes nothing (RUNTIME-SEMANTICS ch. 7.8).
 *
 * <p>Keyed and compared as {@link HeeftKenmerk} is: the attribute on its declaring type, the subject by identity, and
 * the value and the writer by their own equality.
 */
public record Waarde(Object onderwerp, ElementSleutel attribuut, Object waarde, String schrijver) {

    public Waarde {
        Objects.requireNonNull(onderwerp, "onderwerp");
        Objects.requireNonNull(attribuut, "attribuut");
        Objects.requireNonNull(waarde, "waarde");
        Objects.requireNonNull(schrijver, "schrijver");
    }

    @Override public boolean equals(Object o) {
        return o instanceof Waarde w && w.onderwerp == onderwerp && w.attribuut.equals(attribuut)
                && w.waarde.equals(waarde) && w.schrijver.equals(schrijver);
    }

    @Override public int hashCode() {
        return Objects.hash(System.identityHashCode(onderwerp), attribuut, waarde, schrijver);
    }

    @Override public String toString() {
        return "Waarde(" + Feitnamen.object(onderwerp) + ", " + Feitnamen.lid(attribuut) + ", " + waarde + ", "
                + schrijver + ")";
    }
}
