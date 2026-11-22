package dev.uchar.regelspraak.transpiler.tms;

import java.util.Objects;

/**
 * A member of the range of a fold for which it does not stand what it gives the fold (RUNTIME-SEMANTICS ch. 1.5, "A fold
 * over a set", "A closure inside a value", "A quantifier fold"), inserted logically by the contribution rule of the fold:
 * {@code element} is a member of the range of the fold {@code fold}, reached from the fold's subject {@code subject}.
 * {@code own} is what the member is for the form itself, and {@code read} the open read that left it so, for a closure the
 * read it closed over:
 * <ul>
 *   <li>null: undetermined. The filter of a fold over a set is undetermined on the member, or the value it reads of the
 *       member is open: the fold is open. The matrix of a quantifier fold is undetermined on it: the member is.</li>
 *   <li>false, by a closure: the member does not count. A filter is false on it, a filled-value count takes its open value
 *       as leeg, or the matrix is false on it.</li>
 *   <li>true, by a closure: the member counts, a member of a fold over a set with its {@link Contribution} beside this
 *       fact, one of a quantifier fold by this fact alone.</li>
 * </ul>
 * The rule that reads the fold lists these facts beside the contributions, and the evaluation of the fold reads the list
 * ({@link ComputeHelpers#standingCount}, {@link ComputeHelpers#quantifierVerdictOverOpen}).
 *
 * <p>Compared as {@link Contribution} is: the fold's key, {@code own} and the read by value, the subject and the member by
 * identity.
 */
public record FoldOpen(String fold, Object subject, Object element, Boolean own, OpenMember read) {

    public FoldOpen {
        Objects.requireNonNull(fold, "fold");
        Objects.requireNonNull(subject, "subject");
        Objects.requireNonNull(element, "element");
        Objects.requireNonNull(read, "read");
    }

    @Override public boolean equals(Object o) {
        return o instanceof FoldOpen f && f.subject == subject && f.element == element && f.fold.equals(fold)
                && Objects.equals(f.own, own) && f.read.equals(read);
    }

    @Override public int hashCode() {
        return Objects.hash(fold, System.identityHashCode(subject), System.identityHashCode(element), own, read);
    }

    @Override public String toString() {
        return "FoldOpen(" + fold + ", " + FactNames.object(subject) + ", " + FactNames.object(element) + ", "
                + (own == null ? "undetermined" : own) + ", " + read + ")";
    }
}
