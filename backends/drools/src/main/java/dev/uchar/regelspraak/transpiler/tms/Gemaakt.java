package dev.uchar.regelspraak.transpiler.tms;

import java.util.Objects;

/**
 * The stated mark of one object creation: the creation named {@code regel} made {@code object} for {@code onderwerp} in
 * creation phase {@code fase} ({@link Creatiefase}). A creation rule fires only where its mark for the subject is
 * absent, so it makes one object per subject, also when its condition holds again later (RUNTIME-SEMANTICS ch. 5.5), and
 * it does not take as its subject an object some creation made in the phase it fires in. The mark is stated, as the
 * object is: neither lapses when the condition that made them stops holding, as in the reference interpreter, which
 * keeps a minted instance under a fixed label.
 *
 * <p>Compared by the creation's name, the two objects by identity and the phase: one creation makes one mark.
 */
public record Gemaakt(String regel, Object onderwerp, Object object, int fase) {

    public Gemaakt {
        Objects.requireNonNull(regel, "regel");
        Objects.requireNonNull(onderwerp, "onderwerp");
        Objects.requireNonNull(object, "object");
        if (fase < 1) throw new IllegalArgumentException("fase " + fase + " < 1");
    }

    @Override public boolean equals(Object o) {
        return o instanceof Gemaakt g && g.onderwerp == onderwerp && g.object == object && g.fase == fase
                && g.regel.equals(regel);
    }

    @Override public int hashCode() {
        return Objects.hash(regel, System.identityHashCode(onderwerp), System.identityHashCode(object), fase);
    }

    @Override public String toString() {
        return "Gemaakt(" + regel + ", " + Feitnamen.object(onderwerp) + ", " + Feitnamen.object(object) + ", fase "
                + fase + ")";
    }
}
