package dev.uchar.regelspraak.transpiler.tms;

import dev.uchar.regelspraak.ir.ResolvedVerdeling;
import dev.uchar.regelspraak.runtime.RegelSpraakRuntime;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The distribution of one allocation over the recipients of one source (RUNTIME-SEMANTICS ch. 4.2 to 4.4), as a
 * function of the values the allocation's rule binds: the pot and the {@link Recipient} facts of the source, which
 * carry every recipient's weight, ordering key and cap. It computes with the runtime's helpers ({@code rsPlafond},
 * {@code rsAfronding} and, for the ordered method, {@code rsVerdeelOpVolgorde}), so a share is the quotient at 16
 * significant digits, capped and then floored, the v0 residual ch. 4.2 records beside the exact fractions; the edge
 * rows of ch. 4.4 are the reference interpreter's ({@code ref/lib/alloc.ml}), save the two that ch. 4.4 decides
 * otherwise for v1 (below).
 *
 * <p>Where the spec and the destructive lowering of before card tms-omschakeling differed, the spec decides. An empty
 * pot distributes nothing in every method, as ch. 4.4 decides for v1, so nothing is written and nothing deposited; in
 * the equal-shares and the proportional method the reference interpreter keeps the v0 hole there and faults, and the
 * destructive lowering threw. A negative weight in the proportional method counts as 0 and surfaces the §9.5 deposit,
 * as ch. 4.4 decides for v1; the reference interpreter keeps the v0 hole there, an unclamped weight, as the destructive
 * lowering did. In the ordered method an empty tie-break weight counts as 0, as ch. 4.2 and the reference interpreter
 * read it, where {@code rsVerdeelOpVolgorde} splits a group whose weights are all empty in equal parts; and two
 * ordering keys of one value are one group whatever their scale, as the reference interpreter compares them, where that
 * helper groups by {@code equals}. The weights and keys are handed to the helper read so.
 *
 * <p>Public and free of the rule DSL, a function of plain values, so that a rendering of the branch as DRL text can call
 * it from a consequence as the exec-model rendering does.
 */
public final class Allocation {
    private Allocation() {}

    /**
     * What the distribution of one source gives: the share of every recipient that receives one, by identity; the rest,
     * the pot less every share, or null where nothing is distributed and nothing written (ch. 4.4: no recipients, an
     * empty pot, eligible weights that sum to 0); and the recipients on which a degradation surfaces the §9.5 deposit
     * of the allocation (ch. 4.3, 4.4), each once.
     */
    public record Result(Map<Object, BigDecimal> shares, BigDecimal rest, Set<Object> depositors) {}

    /**
     * The distribution by {@code methode} of {@code pot} over {@code ontvangers}, the recipients of one source:
     * {@code gewogen} where the allocation declares a ratio attribute, {@code capped} where it declares a cap,
     * {@code decimals} the floor's decimals (null for none), {@code descending} the ordered method's direction. The
     * order of {@code ontvangers} decides nothing: every share depends only on the values, and a tie group divides the
     * pot as it stands at the group's entry.
     */
    public static Result bereken(ResolvedVerdeling.Methode methode, Object pot, List<Recipient> recipients, boolean gewogen,
                           boolean capped, Integer decimals, boolean descending) {
        Map<Object, BigDecimal> shares = new IdentityHashMap<>();
        Set<Object> depositors = Collections.newSetFromMap(new IdentityHashMap<>());
        // ch. 4.4: zero recipients write nothing and raise nothing, whatever the pot
        if (recipients.isEmpty()) return new Result(shares, null, depositors);
        // ch. 4.4, decided for v1 and uniform across the methods: an empty pot is no distribution, so it writes no
        // share and no rest and deposits nothing, not even for an empty weight or cap, and it is no fault
        if (pot == null) return new Result(shares, null, depositors);
        return switch (methode) {
            case GELIJKE_DELEN -> equalShares(pot, recipients, decimals, shares, depositors);
            case NAAR_RATO -> proportional(pot, recipients, capped, decimals, shares, depositors);
            case OP_VOLGORDE -> ordered(pot, recipients, gewogen, capped, decimals, descending, shares,
                    depositors);
            case COMPLEX -> throw new IllegalStateException("een verdeling met de methode COMPLEX bereikt de berekening;"
                    + " de lader weigert die waarde sinds L4");
        };
    }

    /** One group, every recipient, unweighted (ch. 4.2); a cap is ill-formed here (WF-025) and refused before. */
    private static Result equalShares(Object pot, List<Recipient> recipients, Integer decimals,
                                        Map<Object, BigDecimal> shares, Set<Object> depositors) {
        BigDecimal a = RegelSpraakRuntime.toBd(pot);
        BigDecimal n = BigDecimal.valueOf(recipients.size());
        BigDecimal sum = BigDecimal.ZERO;
        for (Recipient o : recipients) {
            BigDecimal aandeel = floor(a.divide(n, MathContext.DECIMAL64), decimals);
            shares.put(o.recipient(), aandeel);
            sum = sum.add(aandeel);
        }
        return new Result(shares, a.subtract(sum), depositors);
    }

    /**
     * One group, the recipients whose weight is filled (ch. 4.2). An empty weight excludes its recipient, whose share
     * stays leeg, and surfaces the deposit; a negative weight counts as 0 and surfaces the deposit (ch. 4.4, decided
     * for v1). Eligible weights that sum to 0 distribute nothing and write no rest. A cap applies before the floor; an
     * empty or negative one degrades to uncapped and surfaces the deposit, read only for an eligible recipient of a
     * split that distributes, as the reference interpreter reads it (ch. 4.3).
     */
    private static Result proportional(Object pot, List<Recipient> recipients, boolean capped, Integer decimals,
                                    Map<Object, BigDecimal> shares, Set<Object> depositors) {
        BigDecimal a = RegelSpraakRuntime.toBd(pot);
        List<Recipient> gerechtigd = new ArrayList<>();
        List<BigDecimal> gewichten = new ArrayList<>();
        BigDecimal weightSum = BigDecimal.ZERO;
        for (Recipient o : recipients) {
            if (o.weight() == null) {
                depositors.add(o.recipient());
                continue;
            }
            BigDecimal w = RegelSpraakRuntime.toBd(o.weight());
            if (w.signum() < 0) {
                depositors.add(o.recipient());
                w = BigDecimal.ZERO;
            }
            gerechtigd.add(o);
            gewichten.add(w);
            weightSum = weightSum.add(w);
        }
        if (weightSum.signum() == 0) return new Result(shares, null, depositors);
        BigDecimal sum = BigDecimal.ZERO;
        for (int i = 0; i < gerechtigd.size(); i++) {
            Recipient o = gerechtigd.get(i);
            BigDecimal aandeel = a.multiply(gewichten.get(i)).divide(weightSum, MathContext.DECIMAL64);
            if (capped) {
                BigDecimal max = RegelSpraakRuntime.toBd(o.maximum());
                if (max == null || max.signum() < 0) depositors.add(o.recipient());
                aandeel = RegelSpraakRuntime.rsPlafond(aandeel, max);
            }
            aandeel = floor(aandeel, decimals);
            shares.put(o.recipient(), aandeel);
            sum = sum.add(aandeel);
        }
        return new Result(shares, a.subtract(sum), depositors);
    }

    /**
     * The groups of equal ordering keys, an empty key last, ascending or descending (ch. 4.2), through
     * {@code rsVerdeelOpVolgorde}. An empty tie-break weight counts as 0 and surfaces the deposit; a negative one
     * counts as 0 without one, as ch. 4.4 and the reference interpreter have it. An empty or negative cap degrades to
     * uncapped and surfaces the deposit, for every recipient, as the reference interpreter reads the cap of every
     * member of every group.
     */
    private static Result ordered(Object pot, List<Recipient> recipients, boolean gewogen, boolean capped,
                                      Integer decimals, boolean descending, Map<Object, BigDecimal> shares,
                                      Set<Object> depositors) {
        BigDecimal a = RegelSpraakRuntime.toBd(pot);
        List<Object> facts = new ArrayList<>(), orderKeys = new ArrayList<>();
        List<Object> gewichten = gewogen ? new ArrayList<>() : null, maxima = capped ? new ArrayList<>() : null;
        for (Recipient o : recipients) {
            facts.add(o.recipient());
            orderKeys.add(o.sleutel() instanceof BigDecimal d ? d.stripTrailingZeros() : o.sleutel());
            if (gewogen) {
                if (o.weight() == null) depositors.add(o.recipient());
                // a weighted tie group always splits by weight, so an empty weight is the real 0 it counts as
                gewichten.add(o.weight() == null ? BigDecimal.ZERO : RegelSpraakRuntime.toBd(o.weight()));
            }
            if (capped) {
                BigDecimal max = RegelSpraakRuntime.toBd(o.maximum());
                if (max == null || max.signum() < 0) depositors.add(o.recipient());
                maxima.add(max);
            }
        }
        Map<Object, BigDecimal> out = RegelSpraakRuntime.rsVerdeelOpVolgorde(a, facts, orderKeys, !descending, gewichten,
                maxima, decimals);
        BigDecimal sum = BigDecimal.ZERO;
        for (Object f : facts) {
            BigDecimal aandeel = out.get(f);
            if (aandeel == null)
                throw new IllegalStateException("rsVerdeelOpVolgorde gaf geen aandeel voor " + FactNames.object(f));
            shares.put(f, aandeel);
            sum = sum.add(aandeel);
        }
        return new Result(shares, a.subtract(sum), depositors);
    }

    /** The floor at {@code decimals} decimals (ch. 4.3), the identity without them. */
    private static BigDecimal floor(BigDecimal x, Integer decimals) {
        return decimals == null ? x : RegelSpraakRuntime.rsAfronding(x, decimals, RoundingMode.FLOOR);
    }
}
