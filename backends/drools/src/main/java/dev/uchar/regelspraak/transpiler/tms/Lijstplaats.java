package dev.uchar.regelspraak.transpiler.tms;

import java.util.Objects;

/**
 * The place of {@code lid}, counted from 0, in the to-many list of the case it belongs to through its reference field
 * {@code veld}: stated beside the beans by the case input, never derived. The bean model stores a to-many role as one
 * reference field on each member (§13.3.9) and keeps no order, while the order of a fold's members is visible where
 * more than one of them can fault, as where its filter faults: the reference interpreter evaluates the filter per
 * member, members in fold order, and stops at the first fault (RUNTIME-SEMANTICS ch. 5.11). The list is the one the
 * reference interpreter holds: the members the case wrote under the list's holder, in the order it wrote them, and
 * after them the members whose own link the loader mirrors into it, in ascending label order (ch. 5.7a, "member
 * ORDER"). The filter fault rule of a fold, the member fault rule of a per-element fold over a timeline and the
 * contribution rule of an ordered quantifier fold read it ({@link Filterfout}, {@link Lidfout}, {@link Lidtoets});
 * {@link TmsTranspiler#leestLijstplaatsen} says whether a model has such a rule.
 *
 * <p>Compared by the member's identity, the field and the place.
 */
public record Lijstplaats(Object lid, String veld, int plaats) {

    public Lijstplaats {
        Objects.requireNonNull(lid, "lid");
        Objects.requireNonNull(veld, "veld");
        if (plaats < 0) throw new IllegalArgumentException("een plaats in een lijst telt vanaf 0, niet " + plaats);
    }

    @Override public boolean equals(Object o) {
        return o instanceof Lijstplaats p && p.lid == lid && p.veld.equals(veld) && p.plaats == plaats;
    }

    @Override public int hashCode() {
        return Objects.hash(System.identityHashCode(lid), veld, plaats);
    }

    @Override public String toString() {
        return "Lijstplaats(" + Feitnamen.object(lid) + ", " + veld + ", " + plaats + ")";
    }
}
