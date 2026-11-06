package dev.uchar.regelspraak.transpiler.tms;

import dev.uchar.regelspraak.runtime.Breuk;

import java.util.List;
import java.util.Objects;

/**
 * An evaluation fault a rule of the exec-model branch raised on one object, inserted logically by the activation
 * that raised it. The branch has no strata and passes intermediate states the reference interpreter does not see, and
 * a fault thrown out of the fire cannot be withdrawn; as a logical fact the fault stands while every read it rested on
 * stands, and goes when one of them changes. No rule reads it. The session reports the faults that stand at its
 * fixpoint ({@code DroolsBackend.setDerivedValues}), so a fault, like a value, is a property of the fixpoint
 * (ch. 5.2).
 *
 * <p>{@code rule} is the name of the form that raised the fault, and for a {@link SecondFillingFault}, which the
 * links of a relation raise and no form, the declared name of the fact type. {@code fault} is the fault named as the
 * exec-model branch names it out of its fire, one of {@link #KINDS}. Compared as the other facts of the branch are: the
 * subject by identity, the form, the kind of fault and its sentence by value, so that two activations that raise the
 * same fault on one object merge into one fact with a justification each.
 */
public record Fault(Object subject, String rule, RuntimeException fault) {

    /** The exceptions a fault carries: the evaluation faults of the branch, each named in the section "Faults" of the
     *  backend's README ({@code FaultKindsWitnessTest} holds the two together). Any other exception is a broken
     *  contract, which leaves the fire as it is and is no fault of the fixpoint. */
    public static final List<Class<? extends RuntimeException>> KINDS = List.of(ComputeFault.class,
            WritersConflict.class, WriteBoundaryFault.class, InputFormFault.class, OpenSegmentFault.class,
            CharacteristicPeriodsFault.class, SecondFillingFault.class, Breuk.DelingDoorNul.class);

    public Fault {
        Objects.requireNonNull(subject, "subject");
        Objects.requireNonNull(rule, "rule");
        Objects.requireNonNull(fault, "fault");
        Objects.requireNonNull(fault.getMessage(), "the sentence of the fault");
        if (!KINDS.contains(fault.getClass()))
            throw new IllegalArgumentException("geen evaluatiefout van deze tak (Fault.KINDS): " + fault.getClass().getName()
                    + " in de regel \"" + rule + "\"", fault);
    }

    /** The sentence of the fault. */
    public String message() {
        return fault.getMessage();
    }

    @Override public boolean equals(Object o) {
        return o instanceof Fault f && f.subject == subject && f.rule.equals(rule)
                && f.fault.getClass() == fault.getClass() && f.message().equals(message());
    }

    @Override public int hashCode() {
        return Objects.hash(System.identityHashCode(subject), rule, fault.getClass().getName(), message());
    }

    @Override public String toString() {
        return "Fault(" + FactNames.object(subject) + ", " + rule + ", " + message() + ")";
    }
}
