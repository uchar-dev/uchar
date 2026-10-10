package dev.uchar.regelspraak.transpiler.tms;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * One recipient of one allocation of the exec-model branch, for one source, inserted logically:
 * {@code ontvanger} is an instance of the allocation's {@code targetType} that its connecting relation links to the
 * source {@code bron} (RUNTIME-SEMANTICS ch. 4.1), and {@code gewicht}, {@code sleutel} and {@code maximum} are its
 * weight, ordering key and cap as they stand, each null where it is leeg or where the allocation declares no such
 * attribute. A rule of the allocation inserts it per recipient, with every one of those reads bound in its left-hand
 * side, a derived one through its standing; the allocation's own rule collects these facts per source in an accumulate
 * and computes the distribution from them (ch. 4.2). A recipient that arrives or leaves, or whose weight, key or cap
 * changes, gives up its fact and inserts the new one, and the whole distribution of its source follows, the rest
 * included (ch. 4.3).
 *
 * <p>Compared by the allocation, the source and the recipient by identity, and the three values by value (a number by
 * {@code compareTo}), so that a recipient whose values are inserted again unchanged stays one fact.
 */
public record Ontvanger(String verdeling, Object bron, Object ontvanger, Object gewicht, Object sleutel,
                        Object maximum) {

    public Ontvanger {
        Objects.requireNonNull(verdeling, "verdeling");
        Objects.requireNonNull(bron, "bron");
        Objects.requireNonNull(ontvanger, "ontvanger");
    }

    @Override public boolean equals(Object o) {
        return o instanceof Ontvanger x && x.bron == bron && x.ontvanger == ontvanger && x.verdeling.equals(verdeling)
                && gelijk(x.gewicht, gewicht) && gelijk(x.sleutel, sleutel) && gelijk(x.maximum, maximum);
    }

    @Override public int hashCode() {
        return Objects.hash(verdeling, System.identityHashCode(bron), System.identityHashCode(ontvanger), hash(gewicht),
                hash(sleutel), hash(maximum));
    }

    private static boolean gelijk(Object a, Object b) {
        if (a instanceof BigDecimal x && b instanceof BigDecimal y) return x.compareTo(y) == 0;
        return Objects.equals(a, b);
    }

    private static Object hash(Object w) {
        return w instanceof BigDecimal d ? d.stripTrailingZeros() : w;
    }

    @Override public String toString() {
        return "Ontvanger(" + verdeling + ", " + Feitnamen.object(bron) + ", " + Feitnamen.object(ontvanger)
                + ", gewicht " + (gewicht == null ? "leeg" : gewicht) + ", sleutel " + (sleutel == null ? "leeg" : sleutel)
                + ", maximum " + (maximum == null ? "leeg" : maximum) + ")";
    }
}
