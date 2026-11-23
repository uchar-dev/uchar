package dev.uchar.regelspraak.transpiler.tms;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * A member of the range of a fold for which it does not stand what it gives the fold (RUNTIME-SEMANTICS ch. 1.5, "A fold
 * over a set", "A quantifier fold"), inserted logically by the contribution rule of the fold: {@code element} is a member
 * of the range of the fold {@code fold}, reached from the fold's subject {@code subject}, and {@code read} the open read
 * that left it undetermined. The filter of a fold over a set is undetermined on the member, or the value it reads of the
 * member is open, a filled-value count's among them: the fold is open. The filter or the matrix of a quantifier fold is
 * undetermined on it: the member is.
 * The rule that reads the fold lists these facts beside the contributions, and the evaluation of the fold reads the list
 * ({@link ComputeHelpers#standingCount}, {@link ComputeHelpers#quantifierVerdictOverOpen}).
 *
 * <p>Which of several members a broken off fold names shows in a reading alone, and that is the first in the order of the
 * fold (RUNTIME-SEMANTICS ch. 5.12, "Open values"): in a build with readings the fact carries the member's {@code place} in
 * it ({@link #placed}), and the evaluation reads the facts in that order ({@link #inOrder}). In a build without readings
 * {@code place} is empty.
 *
 * <p>Compared as {@link Contribution} is: the fold's key, the read and the place by value, the subject and the
 * member by identity. A member that two routes reach has a fact per route, whose least place is its first reach.
 */
public record FoldOpen(String fold, Object subject, Object element, OpenMember read, List<Integer> place) {

    /** What of its member leaves it undetermined, in the order a fold reads them: the filter, or the matrix of a quantifier
     *  fold, of every member before the value of any. */
    enum Part { FILTER, VALUE }

    public FoldOpen {
        Objects.requireNonNull(fold, "fold");
        Objects.requireNonNull(subject, "subject");
        Objects.requireNonNull(element, "element");
        Objects.requireNonNull(read, "read");
        place = List.copyOf(place);
    }

    /** The place of a member whose {@code part} does not stand, where {@code place} is that of the member in the order of
     *  the fold ({@link ComputeHelpers#foldPlace}): the part, then that place; empty where {@code place} is null. */
    static List<Integer> placed(Part part, List<Integer> place) {
        if (place == null) return List.of();
        List<Integer> out = new ArrayList<>(place.size() + 1);
        out.add(part.ordinal());
        out.addAll(place);
        return out;
    }

    /** The facts of {@code open}, the list a rule bound, in the order of their places, compared element by element; in the
     *  order of the list where they have none. */
    static List<FoldOpen> inOrder(Object open) {
        List<FoldOpen> out = new ArrayList<>();
        for (Object o : (List<?>) open) out.add((FoldOpen) o);
        out.sort((a, b) -> Arrays.compare(a.place.toArray(new Integer[0]), b.place.toArray(new Integer[0])));
        return out;
    }

    /** As {@link #inOrder}, each member once, at its first reach: the members of a quantifier fold, of which each has one
     *  judgement. */
    static List<FoldOpen> members(Object open) {
        Set<Object> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        List<FoldOpen> out = inOrder(open);
        out.removeIf(f -> !seen.add(f.element));
        return out;
    }

    @Override public boolean equals(Object o) {
        return o instanceof FoldOpen f && f.subject == subject && f.element == element && f.fold.equals(fold)
                && f.read.equals(read) && f.place.equals(place);
    }

    @Override public int hashCode() {
        return Objects.hash(fold, System.identityHashCode(subject), System.identityHashCode(element), read, place);
    }

    @Override public String toString() {
        return "FoldOpen(" + fold + ", " + FactNames.object(subject) + ", " + FactNames.object(element) + ", "
                + read + ")";
    }
}
