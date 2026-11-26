package dev.uchar.regelspraak.transpiler.tms;

import java.util.List;
import java.util.Objects;

/**
 * An open single role encountered while walking a fold's {@code sourceRole} or {@code sourcePath} from the fold's
 * subject {@code subject} (RUNTIME-SEMANTICS ch. 1.5, 3.4a-b, 3.9a, 5.11): inserted logically by the range-open rule
 * of that step, and ordered by {@code place} ({@code [selection, step, ...listPlaces...]}), so the rule that reads the
 * fold meets the first open hop in breadth-first step and list order before any filter deposit, filter fault, member
 * fault, contributor or fallback.
 */
public final class RangeOpen implements Comparable<RangeOpen> {

    private final String fold;
    private final Object subject;
    private final OpenMember read;
    private final List<Integer> place;

    public RangeOpen(String fold, Object subject, OpenMember read, List<Integer> place) {
        this.fold = Objects.requireNonNull(fold, "fold");
        this.subject = Objects.requireNonNull(subject, "subject");
        this.read = Objects.requireNonNull(read, "read");
        this.place = List.copyOf(place);
        if (this.place.size() < 2)
            throw new IllegalArgumentException("a range-open place begins with selection and step: " + this.place);
    }

    public String fold() { return fold; }

    public Object subject() { return subject; }

    public OpenMember read() { return read; }

    public List<Integer> place() { return place; }

    public int selection() { return place.get(0); }

    public OpenRead openRead() { return new OpenRead(read); }

    @Override public int compareTo(RangeOpen o) {
        for (int i = 0; i < Math.min(place.size(), o.place.size()); i++) {
            int c = Integer.compare(place.get(i), o.place.get(i));
            if (c != 0) return c;
        }
        int c = Integer.compare(place.size(), o.place.size());
        return c != 0 ? c : FactNames.member(read.member()).compareTo(FactNames.member(o.read.member()));
    }

    @Override public boolean equals(Object o) {
        return o instanceof RangeOpen r && r.subject == subject && r.fold.equals(fold)
                && r.read.equals(read) && r.place.equals(place);
    }

    @Override public int hashCode() {
        return Objects.hash(fold, System.identityHashCode(subject), read, place);
    }

    @Override public String toString() {
        return "RangeOpen(" + fold + ", " + FactNames.object(subject) + ", " + read + ", " + place + ")";
    }
}
