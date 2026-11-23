package dev.uchar.regelspraak.transpiler.tms;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * The matrix of a quantifier fold evaluated for one contributor, the value of that contributor's {@link Contribution}
 * where the matrix can fault or deposit (RUNTIME-SEMANTICS ch. 3.9a): whether it holds ({@code holds}), or the
 * evaluation fault it raised ({@code fault}, unnamed: the form that reads the fold names it), or where it can be
 * undetermined (ch. 1.5) the {@link OpenRead} or the {@link Undetermined} it ended in, and whether its
 * evaluation reached a place that deposits ({@code deposited}: a division of a filled dividend by a leeg divisor or
 * the square root of a negative number), with the EF codes of what it deposited ({@code codes}, RUNTIME-SEMANTICS
 * ch. 5.7b; none for the square root). It is no outcome by itself. The reference interpreter reads the matrix in
 * the contributors' order and stops at the first contributor after which the verdict stands (ch. 5.10), so a
 * contributor it does not reach raises no fault and deposits nothing; the rule that reads the fold replays that order
 * over these values, and only a contributor the replay reaches has its fault or its deposit heard.
 *
 * <p>{@code place} is the contributor's place in that order, as {@link FilterFault#place} gives it: 0 for the one
 * selection, then the place of the member in the list of the fold's subject ({@link ListPlace}), absent for a range
 * that reaches at most one member. Ordered by the place, compared element by element. Compared by value: the place,
 * the verdict, the fault by its kind and its sentence, and the deposit with its codes, so the {@link Contribution} that carries it merges
 * with an equal one as its other values do.
 *
 * <p>Where the filter of the fold can be undetermined (ch. 1.5, "A quantifier fold"), every contributor has such a test,
 * also of a matrix that can neither fault nor deposit, and it carries the judgement of the filter on the member where that
 * is undetermined ({@code filter}, null where the filter is true) and the {@code element} itself
 * ({@link #filtered}): in a build without readings such a matrix shows no order and the place is that of the selection
 * alone, and in a build with readings a member that two routes reach has a test per route and is read at its first.
 */
public final class MemberTest implements Comparable<MemberTest> {

    private final List<Integer> place;
    private final Boolean holds;
    private final RuntimeException fault;
    private final boolean deposited;
    private final List<String> codes;
    private final Object element;
    private final Undetermined filter;

    public MemberTest(List<Integer> place, Boolean holds, RuntimeException fault, boolean deposited,
                      List<String> codes) {
        this(place, holds, fault, deposited, codes, null, null);
    }

    private MemberTest(List<Integer> place, Boolean holds, RuntimeException fault, boolean deposited,
                       List<String> codes, Object element, Undetermined filter) {
        this.place = List.copyOf(place);
        if (this.place.isEmpty()) throw new IllegalArgumentException("a place in the order begins with the selection");
        if ((holds == null) == (fault == null))
            throw new IllegalArgumentException("a member test holds or fails, never both or neither");
        this.holds = holds;
        this.fault = fault;
        this.deposited = deposited;
        this.codes = List.copyOf(codes);
        this.element = element;
        this.filter = filter;
    }

    /** This test of the member {@code element}, on which the fold's filter has the judgement {@code judgement}
     *  ({@link ComputeHelpers#judge}), which is not false. */
    MemberTest filtered(Object judgement, Object element) {
        return new MemberTest(place, holds, fault, deposited, codes, element,
                judgement instanceof Undetermined u ? u : null);
    }

    /** The judgement of the fold's filter on the member where it is undetermined; null where the filter is true. */
    Undetermined filter() { return filter; }

    /** The tests {@code contributions}, what a rule bound of a fold's contributors, in the contributors' order, each member
     *  once, at its first reach. */
    static List<MemberTest> inOrder(Iterable<?> contributions) {
        List<MemberTest> out = new ArrayList<>();
        for (Object t : contributions) out.add((MemberTest) t);
        out.sort(null);
        Set<Object> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        out.removeIf(t -> t.element != null && !seen.add(t.element));
        return out;
    }

    /** The open read of the first of {@code members} whose filter is undetermined, which makes every matrix
     *  of the fold a provisional read (ch. 1.5); null where every filter stands. */
    static OpenMember past(List<MemberTest> members) {
        for (MemberTest t : members) if (t.filter != null) return t.filter.read();
        return null;
    }

    public List<Integer> place() { return place; }

    /** Whether the matrix holds for the contributor; null where it faulted. */
    public Boolean holds() { return holds; }

    /** The fault the matrix raised for the contributor, unnamed; null where it answered. */
    public RuntimeException fault() { return fault; }

    /** The matrix read for the contributor, as the evaluation of it went: its deposit heard by {@code depositFlag}, a probe
     *  or nothing, then its fault raised, or else its truth. */
    boolean replayed(Object depositFlag) {
        if (deposited) DepositFlag.report(depositFlag, codes);
        if (fault != null) throw fault;
        return holds;
    }

    public boolean deposited() { return deposited; }

    /** The EF codes of what the matrix deposited, ascending; empty where it deposited nothing or nothing with a code. */
    public List<String> codes() { return codes; }

    @Override public int compareTo(MemberTest o) {
        for (int i = 0; i < Math.min(place.size(), o.place.size()); i++) {
            int c = Integer.compare(place.get(i), o.place.get(i));
            if (c != 0) return c;
        }
        return Integer.compare(place.size(), o.place.size());
    }

    @Override public boolean equals(Object o) {
        return o instanceof MemberTest t && t.place.equals(place) && Objects.equals(t.holds, holds)
                && t.deposited == deposited && t.codes.equals(codes) && (fault == null ? t.fault == null
                : t.fault != null && t.fault.getClass() == fault.getClass()
                  && Objects.equals(t.fault.getMessage(), fault.getMessage()))
                && t.element == element && (filter == null ? t.filter == null : t.filter != null
                && t.filter.read().equals(filter.read()));
    }

    @Override public int hashCode() {
        return Objects.hash(place, holds, deposited, codes, fault == null ? null : fault.getClass().getName(),
                fault == null ? null : fault.getMessage());
    }

    @Override public String toString() {
        return "MemberTest(" + place + ", " + (fault != null ? "fault " + fault.getMessage() : holds ? "true" : "false")
                + (deposited ? ", deposited" : "") + (filter == null ? "" : ", filter " + filter.getMessage()) + ")";
    }
}
