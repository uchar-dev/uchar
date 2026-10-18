package dev.uchar.regelspraak.transpiler.tms;

import java.util.Objects;

/**
 * A member of a fold's range whose filter deposits, inserted logically by the filter deposit rule of the fold:
 * {@code element} is a member of the range of the fold {@code fold}, reached from the fold's subject
 * {@code onderwerp}, and evaluating the filter for it reaches a division of a filled dividend by a leeg divisor or the
 * square root of a negative number (RUNTIME-SEMANTICS ch. 1.2). It is no deposit by itself. The reference interpreter
 * evaluates a filter only where the evaluation of a form reaches the fold (ch. 5.10, 5.11), with that form's collector,
 * so the §9.5 inconsistency is the reading form's, on its subject, the fold's subject (ch. 5.6, "keyed by (rule ×
 * subject)"). The rule that reads the fold therefore counts these facts beside the contributions, and where its own
 * evaluation reaches the fold while one stands, it deposits as a division in its own value or condition would.
 *
 * <p>Compared as {@link Contribution} is, without a value: the fold's key by value, the subject and the member by identity.
 * A member reached along two routes of a multi-step range, or selected by two selections of an instance-set union
 * whose filters both deposit, is therefore one fact with a justification each; the reading rule asks only whether one
 * stands.
 */
public record FilterDeposit(String fold, Object onderwerp, Object element) {

    public FilterDeposit {
        Objects.requireNonNull(fold, "vouw");
        Objects.requireNonNull(onderwerp, "onderwerp");
        Objects.requireNonNull(element, "element");
    }

    @Override public boolean equals(Object o) {
        return o instanceof FilterDeposit d && d.onderwerp == onderwerp && d.element == element && d.fold.equals(fold);
    }

    @Override public int hashCode() {
        return Objects.hash(fold, System.identityHashCode(onderwerp), System.identityHashCode(element));
    }

    @Override public String toString() {
        return "Filterdeponering(" + fold + ", " + FactNames.object(onderwerp) + ", " + FactNames.object(element) + ")";
    }
}
