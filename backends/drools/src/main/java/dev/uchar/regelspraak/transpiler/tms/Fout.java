package dev.uchar.regelspraak.transpiler.tms;

import java.util.Objects;

/**
 * An evaluation fault a rule of the truth-maintenance branch raised on one object, inserted logically by the activation
 * that raised it: a division by a numeric zero (RUNTIME-SEMANTICS ch. 1.4, point 4), in a rule's value or in a
 * condition. The branch has no strata and passes intermediate states the reference interpreter does not see, and a
 * fault thrown out of the fire cannot be withdrawn; as a logical fact the fault stands while every read it rested on
 * stands, and goes when one of them changes. No rule reads it. The session reports the faults that stand at its
 * fixpoint ({@code DroolsBackend.zetAfgeleideWaarden}), so a fault, like a value, is a property of the fixpoint
 * (ch. 5.2).
 *
 * <p>{@code regel} is the name of the form that raised the fault, and {@code fout} the fault named as the exec-model
 * branch names it out of its fire: the rule and the operands. Compared as the other facts of the branch are: the subject
 * by identity, the form, the kind of fault and its sentence by value, so that two activations that raise the same fault
 * on one object merge into one fact with a justification each.
 */
public record Fout(Object onderwerp, String regel, RuntimeException fout) {

    public Fout {
        Objects.requireNonNull(onderwerp, "onderwerp");
        Objects.requireNonNull(regel, "regel");
        Objects.requireNonNull(fout, "fout");
        Objects.requireNonNull(fout.getMessage(), "de zin van de fout");
    }

    /** The sentence of the fault. */
    public String melding() {
        return fout.getMessage();
    }

    @Override public boolean equals(Object o) {
        return o instanceof Fout f && f.onderwerp == onderwerp && f.regel.equals(regel)
                && f.fout.getClass() == fout.getClass() && f.melding().equals(melding());
    }

    @Override public int hashCode() {
        return Objects.hash(System.identityHashCode(onderwerp), regel, fout.getClass().getName(), melding());
    }

    @Override public String toString() {
        return "Fout(" + Feitnamen.object(onderwerp) + ", " + regel + ", " + melding() + ")";
    }
}
