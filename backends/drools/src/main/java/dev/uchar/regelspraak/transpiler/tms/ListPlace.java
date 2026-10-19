package dev.uchar.regelspraak.transpiler.tms;

import java.util.Objects;

/**
 * The place of {@code member}, counted from 0, in the to-many list of the case it belongs to through its reference field
 * {@code field}: stated beside the beans by the case input, never derived. The bean model stores a to-many role as one
 * reference field on each member (§13.3.9) and keeps no order, while the order of a fold's members is visible where
 * more than one of them can fault, as where its filter faults: the reference interpreter evaluates the filter per
 * member, members in fold order, and stops at the first fault (RUNTIME-SEMANTICS ch. 5.11). The list is the one the
 * reference interpreter holds: the members the case wrote under the list's holder, in the order it wrote them, and
 * after them the members whose own link the loader mirrors into it, in ascending label order (ch. 5.7a, "member
 * ORDER"). The filter fault rule of a fold, the member fault rule of a per-element fold over a timeline and the
 * contribution rule of an ordered quantifier fold read it ({@link FilterFault}, {@link MemberFault}, {@link MemberTest});
 * {@link TmsTranspiler#readsListPlaces} says whether a model has such a rule.
 *
 * <p>Compared by the member's identity, the field and the place.
 */
public record ListPlace(Object member, String field, int place) {

    public ListPlace {
        Objects.requireNonNull(member, "member");
        Objects.requireNonNull(field, "field");
        if (place < 0) throw new IllegalArgumentException("een plaats in een lijst telt vanaf 0, niet " + place);
    }

    @Override public boolean equals(Object o) {
        return o instanceof ListPlace p && p.member == member && p.field.equals(field) && p.place == place;
    }

    @Override public int hashCode() {
        return Objects.hash(System.identityHashCode(member), field, place);
    }

    @Override public String toString() {
        return "ListPlace(" + FactNames.object(member) + ", " + field + ", " + place + ")";
    }
}
