package dev.uchar.regelspraak.transpiler.tms;

import java.util.List;
import java.util.Objects;

/**
 * A member of a fold's range whose filter deposits, inserted logically by the filter deposit rule of the fold:
 * {@code element} is a member of the range of the fold {@code fold}, reached from the fold's subject
 * {@code subject}, and evaluating the filter for it reaches a division of a filled dividend by a leeg divisor or by a
 * numeric zero, or the square root of a negative number (RUNTIME-SEMANTICS ch. 1.2, 1.4 point 4); {@code codes} are the
 * EF codes of the kinds it deposits (ch. 5.7b), ascending, none for the square root. It is no deposit by itself. The
 * reference interpreter evaluates a filter only where the evaluation of a form reaches the fold (ch. 5.10, 5.11), with
 * that form's collector, so the §9.5 inconsistency is the reading form's, on its subject, the fold's subject (ch. 5.6,
 * "keyed by (rule × subject)"). The rule that reads the fold therefore lists these facts beside the contributions, and
 * where its own evaluation reaches the fold while one stands, it deposits their codes as a division in its own value or
 * condition would.
 *
 * <p>Compared as {@link Contribution} is, without a value: the fold's key and the codes by value, the subject and the
 * member by identity. A member reached along two routes of a multi-step range, or selected by two selections of an
 * instance-set union whose filters both deposit the same kinds, is therefore one fact with a justification each; the
 * reading rule asks only which codes stand.
 */
public record FilterDeposit(String fold, Object subject, Object element, List<String> codes) {

    public FilterDeposit {
        Objects.requireNonNull(fold, "fold");
        Objects.requireNonNull(subject, "subject");
        Objects.requireNonNull(element, "element");
        codes = List.copyOf(codes);
    }

    @Override public boolean equals(Object o) {
        return o instanceof FilterDeposit d && d.subject == subject && d.element == element && d.fold.equals(fold)
                && d.codes.equals(codes);
    }

    @Override public int hashCode() {
        return Objects.hash(fold, System.identityHashCode(subject), System.identityHashCode(element), codes);
    }

    @Override public String toString() {
        return "FilterDeposit(" + fold + ", " + FactNames.object(subject) + ", " + FactNames.object(element) + ")";
    }
}
