package dev.uchar.regelspraak.transpiler.tms;

import dev.uchar.regelspraak.graaf.ProducentConsument.ElementSleutel;

import java.util.Objects;

/**
 * The standing of one derived attribute on one object, total: at the fixpoint there is exactly one per (object,
 * attribute), carrying the value of the {@link Waarde} a rule derived where there is one, and otherwise the value the
 * case input put on the bean, leeg included. A condition or a value reads a derived attribute through this fact, as
 * a positive pattern, so its read is bound in the left-hand side and truth maintenance follows it. The two sources
 * give the defaults reading of the case input without a rule of their own: a writer that decides replaces the input,
 * and one that computes leeg writes nothing and leaves the input standing (RUNTIME-SEMANTICS ch. 7.8).
 *
 * <p>An attribute on a timeline has a standing wherever a rule reads it, also where no form writes it, so that its input
 * stands in the convention the case gives it: a {@code List<Tijdvak>} for a full timeline, an {@link Emmerscalair} for
 * the asOfDate-bucket scalar (ch. 8.4).
 *
 * <p>Keyed and compared as {@link HeeftKenmerk} is; the value may be null, and then it is leeg.
 */
public record Stand(Object onderwerp, ElementSleutel attribuut, Object waarde) {

    public Stand {
        Objects.requireNonNull(onderwerp, "onderwerp");
        Objects.requireNonNull(attribuut, "attribuut");
    }

    @Override public boolean equals(Object o) {
        return o instanceof Stand s && s.onderwerp == onderwerp && s.attribuut.equals(attribuut)
                && Objects.equals(s.waarde, waarde);
    }

    @Override public int hashCode() {
        return Objects.hash(System.identityHashCode(onderwerp), attribuut, waarde);
    }

    @Override public String toString() {
        return "Stand(" + Feitnamen.object(onderwerp) + ", " + Feitnamen.lid(attribuut) + ", "
                + (waarde == null ? "leeg" : waarde) + ")";
    }
}
