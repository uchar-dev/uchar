package dev.uchar.regelspraak.transpiler.tms;

import java.util.Objects;

/**
 * The stated mark of one object creation: the creation named {@code rule} made {@code object} for {@code subject} in
 * creation phase {@code phase} ({@link CreationPhase}). A creation rule fires only where its mark for the subject is
 * absent, so it makes one object per subject, also when its condition holds again later (RUNTIME-SEMANTICS ch. 5.5), and
 * it does not take as its subject an object some creation made in the phase it fires in. The mark is stated, as the
 * object is: neither lapses when the condition that made them stops holding, as in the reference interpreter, which
 * keeps a minted instance under a fixed label.
 *
 * <p>{@code stratum} is the evaluation stratum of the creation and {@code depth} the creation depth of the object in
 * it, one more than the depth of the subject there, where an object of the input and an object a lower stratum made
 * stand at 0: the creation budget of RUNTIME-SEMANTICS ch. 5.5, which the creation rule reads off the mark of its
 * subject ({@code TmsTranspiler.depth}).
 *
 * <p>Compared by the creation's name, the two objects by identity and the phase: one creation makes one mark, and its
 * stratum and depth follow from the creation and the subject.
 */
public record CreationStamp(String rule, Object subject, Object object, int phase, int stratum, int depth) {

    public CreationStamp {
        Objects.requireNonNull(rule, "rule");
        Objects.requireNonNull(subject, "subject");
        Objects.requireNonNull(object, "object");
        if (phase < 1) throw new IllegalArgumentException("fase " + phase + " < 1");
        if (stratum < 0) throw new IllegalArgumentException("stratum " + stratum + " < 0");
        if (depth < 1) throw new IllegalArgumentException("diepte " + depth + " < 1");
    }

    @Override public boolean equals(Object o) {
        return o instanceof CreationStamp g && g.subject == subject && g.object == object && g.phase == phase
                && g.rule.equals(rule);
    }

    @Override public int hashCode() {
        return Objects.hash(rule, System.identityHashCode(subject), System.identityHashCode(object), phase);
    }

    @Override public String toString() {
        return "Gemaakt(" + rule + ", " + FactNames.object(subject) + ", " + FactNames.object(object) + ", fase "
                + phase + ", stratum " + stratum + ", diepte " + depth + ")";
    }
}
