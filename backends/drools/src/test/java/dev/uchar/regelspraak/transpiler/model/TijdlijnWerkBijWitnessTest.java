package dev.uchar.regelspraak.transpiler.model;

import org.drools.model.Drools;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The timeline WRITE helper, pinned directly. Two of the three write sites it serves (the ch. 3.7 pro-rata and
 * ch. 3.8 day-count folds) have corpus witnesses; the third — the §5.8.2 {@code rsTijdSomAlle} set-fold into a
 * temporal target — has NONE, because every temporal target in the corpus is one of the other two shapes. That
 * branch used to reach the SCALAR update guard, whose {@link Objects#equals} is ALWAYS unequal on two timelines
 * (a Segment declares no {@code equals}), so it re-propagated its subject on every recompute — the same defect
 * class as the unanchored pro-rata write, at a place no golden can show. These cases witness the helper's four
 * decisions instead: unchanged -> no update, changed -> update, and both sides of the ch. 7.8 standing arm.
 *
 * <p>The segment stand-in below is shape-faithful (the {@code van}/{@code tot}/{@code waarde} properties the
 * comparison reads reflectively) and — like the generated bean — declares no {@code equals}, so the first case
 * would pass trivially under a value-typed segment and does not here.
 */
class TijdlijnWerkBijWitnessTest {

    /** A mutable doel holder: the getter/setter pair the write helper is handed. */
    private static final class Doel { List<Object> tijdreeks; }

    /** Shape-faithful segment stand-in — no equals, exactly as the generated Segment bean. */
    public static final class Seg {
        private final LocalDate van, tot; private final BigDecimal waarde;
        Seg(String van, String tot, String waarde) {
            this.van = LocalDate.parse(van); this.tot = LocalDate.parse(tot);
            this.waarde = waarde == null ? null : new BigDecimal(waarde);
        }
        public LocalDate getVan() { return van; }
        public LocalDate getTot() { return tot; }
        public BigDecimal getWaarde() { return waarde; }
    }

    @Test void een_ongewijzigde_tijdreeks_propageert_niet() {
        Doel d = new Doel();
        d.tijdreeks = reeks(new Seg("2024-01-01", "2024-02-01", "12"));
        List<Object> herberekend = reeks(new Seg("2024-01-01", "2024-02-01", "12.00"));
        // the premise: the scalar guard would have seen a change here and updated on every recompute
        assertFalse(Objects.equals(d.tijdreeks, herberekend), "distinct segment instances are never equals-equal");

        AtomicInteger updates = new AtomicInteger();
        schrijf(d, herberekend, false, updates);
        assertEquals(0, updates.get(), "a same-valued timeline must not re-propagate the subject");
    }

    @Test void een_gewijzigde_tijdreeks_propageert_wel() {
        Doel d = new Doel();
        d.tijdreeks = reeks(new Seg("2024-01-01", "2024-02-01", "12"));
        List<Object> nieuw = reeks(new Seg("2024-01-01", "2024-02-01", "13"));

        AtomicInteger updates = new AtomicInteger();
        schrijf(d, nieuw, false, updates);
        assertEquals(1, updates.get(), "a changed timeline must be written AND propagated — never under-updated");
        assertSame(nieuw, d.tijdreeks);
    }

    @Test void een_lege_opbrengst_op_een_staand_doel_schrijft_niets() {
        Doel d = new Doel();
        List<Object> invoer = reeks(new Seg("2024-03-01", "2024-04-01", "99"));
        d.tijdreeks = invoer;

        AtomicInteger updates = new AtomicInteger();
        schrijf(d, new ArrayList<>(), true, updates);
        assertSame(invoer, d.tijdreeks, "ch. 7.8: the case-supplied input timeline stands whole");
        assertEquals(0, updates.get());
    }

    @Test void een_lege_opbrengst_buiten_het_staande_domein_wist_wel() {
        // the flank that keeps the arm from degenerating into "never write empty": off the standing domain the
        // empty result is an ordinary write and erases the target (ch. 1.2 assignment).
        Doel d = new Doel();
        d.tijdreeks = reeks(new Seg("2024-03-01", "2024-04-01", "99"));
        List<Object> leeg = new ArrayList<>();

        AtomicInteger updates = new AtomicInteger();
        schrijf(d, leeg, false, updates);
        assertSame(leeg, d.tijdreeks, "a non-standing target is erased by an empty result");
        assertEquals(1, updates.get());
    }

    @Test void een_gevulde_opbrengst_op_een_staand_doel_verdringt_de_invoer() {
        // the other side of the arm: a deciding fold displaces the input entirely, standing domain or not.
        Doel d = new Doel();
        d.tijdreeks = reeks(new Seg("2024-03-01", "2024-04-01", "99"));
        List<Object> berekend = reeks(new Seg("2024-01-01", "2024-02-01", "12"));

        AtomicInteger updates = new AtomicInteger();
        schrijf(d, berekend, true, updates);
        assertSame(berekend, d.tijdreeks, "a gevulde opbrengst decides and replaces the whole timeline");
        assertEquals(1, updates.get());
    }

    private static void schrijf(Doel d, Object nieuw, boolean staandDoel, AtomicInteger updates) {
        ExecModelTranspiler.werkBijTijdlijn(telDrools(updates), d,
                s -> ((Doel) s).tijdreeks, (s, v) -> ((Doel) s).tijdreeks = cast(v), nieuw, staandDoel);
    }

    @SuppressWarnings("unchecked")
    private static List<Object> cast(Object v) { return (List<Object>) v; }

    private static List<Object> reeks(Object... segs) { return new ArrayList<>(List.of(segs)); }

    /** A Drools stand-in that only counts {@code update} calls — the propagation the guard decides about. */
    private static Drools telDrools(AtomicInteger updates) {
        return (Drools) Proxy.newProxyInstance(Drools.class.getClassLoader(), new Class<?>[] { Drools.class },
                (p, m, args) -> {
                    if (m.getName().equals("update")) updates.incrementAndGet();
                    else assertTrue(false, "the write helper calls only update, never " + m.getName());
                    return null;
                });
    }
}
