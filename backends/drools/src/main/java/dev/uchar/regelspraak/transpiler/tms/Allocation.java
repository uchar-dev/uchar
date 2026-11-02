package dev.uchar.regelspraak.transpiler.tms;

import dev.uchar.regelspraak.ir.ResolvedAllocation;
import dev.uchar.regelspraak.runtime.Breuk;
import dev.uchar.regelspraak.runtime.RegelSpraakRuntime;

import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * The distribution of one allocation over the recipients of one source (RUNTIME-SEMANTICS ch. 4.2 to 4.4), as a
 * function of the values the allocation's rule binds: the pot and the {@link Recipient} facts of the source, which
 * carry every recipient's weight, ordering key and cap. It computes on the exact rationals ({@link Breuk}): a share is
 * the exact quotient, capped, and floored once at the rounding's decimals, and the rest is the pot less every share,
 * exactly (ch. 4.2); a value is a decimal where it terminates and an exact rational where it does not, as the runtime's
 * arithmetic gives it. The ordered method computes with the runtime's helper {@code rsVerdeelOpVolgorde}, on the same
 * terms. The edge rows of ch. 4.4 are the reference interpreter's ({@code ref/lib/alloc.ml}), save the two that ch. 4.4
 * decides otherwise for v1 (below).
 *
 * <p>Where the spec and the v0 mirror differ, the spec decides. An empty pot distributes nothing in every method, as
 * ch. 4.4 decides for v1, so nothing is written and nothing deposited; in the equal-shares and the proportional method
 * the reference interpreter keeps the v0 hole there and faults. A negative weight in the proportional method counts as
 * 0 and surfaces the §9.5 deposit, as ch. 4.4 decides for v1; the reference interpreter keeps the v0 hole there, an
 * unclamped weight. In the ordered method an empty tie-break weight counts as 0, as ch. 4.2 and the reference interpreter
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
     * of the allocation (ch. 4.3, 4.4), each once, by identity, with the EF codes of its degradations (ch. 5.7b),
     * ascending: none for a negative weight in the proportional method, a kind without a code. A share and the rest are
     * a {@code BigDecimal} where they terminate and a {@link Breuk} where they do not.
     */
    public record Result(Map<Object, Object> shares, Object rest, Map<Object, Set<String>> depositors) {}

    /**
     * The distribution by {@code method} of {@code pot} over {@code recipients}, the recipients of one source:
     * {@code weighted} where the allocation declares a ratio attribute, {@code capped} where it declares a cap,
     * {@code decimals} the floor's decimals (null for none), {@code descending} the ordered method's direction. The
     * order of {@code recipients} decides nothing: every share depends only on the values, and a tie group divides the
     * pot as it stands at the group's entry.
     */
    public static Result compute(ResolvedAllocation.Method method, Object pot, List<Recipient> recipients, boolean weighted,
                           boolean capped, Integer decimals, boolean descending) {
        Map<Object, Object> shares = new IdentityHashMap<>();
        Map<Object, Set<String>> depositors = new IdentityHashMap<>();
        // ch. 4.4: zero recipients write nothing and raise nothing, whatever the pot
        if (recipients.isEmpty()) return new Result(shares, null, depositors);
        // ch. 4.4, decided for v1 and uniform across the methods: an empty pot is no distribution, so it writes no
        // share and no rest and deposits nothing, not even for an empty weight or cap, and it is no fault
        if (pot == null) return new Result(shares, null, depositors);
        return switch (method) {
            case GELIJKE_DELEN -> equalShares(pot, recipients, decimals, shares, depositors);
            case NAAR_RATO -> proportional(pot, recipients, capped, decimals, shares, depositors);
            case OP_VOLGORDE -> ordered(pot, recipients, weighted, capped, decimals, descending, shares,
                    depositors);
            case COMPLEX -> throw new IllegalStateException("een verdeling met de methode COMPLEX bereikt de berekening;"
                    + " de lader weigert die waarde sinds L4");
        };
    }

    /** One group, every recipient, unweighted (ch. 4.2); a cap is ill-formed here (WF-025) and refused before. */
    private static Result equalShares(Object pot, List<Recipient> recipients, Integer decimals,
                                        Map<Object, Object> shares, Map<Object, Set<String>> depositors) {
        Breuk a = Breuk.van(pot);
        Breuk n = Breuk.van(BigInteger.valueOf(recipients.size()), BigInteger.ONE);
        Breuk sum = ZERO;
        for (Recipient o : recipients) {
            Breuk share = floor(a.delen(n), decimals);
            shares.put(o.recipient(), atRest(share));
            sum = sum.optellen(share);
        }
        return new Result(shares, atRest(a.aftrekken(sum)), depositors);
    }

    /**
     * One group, the recipients whose weight is filled (ch. 4.2). An empty weight excludes its recipient, whose share
     * stays leeg, and surfaces the deposit; a negative weight counts as 0 and surfaces the deposit (ch. 4.4, decided
     * for v1). Eligible weights that sum to 0 distribute nothing and write no rest. A cap applies before the floor; an
     * empty or negative one degrades to uncapped and surfaces the deposit, read only for an eligible recipient of a
     * split that distributes, as the reference interpreter reads it (ch. 4.3).
     */
    private static Result proportional(Object pot, List<Recipient> recipients, boolean capped, Integer decimals,
                                    Map<Object, Object> shares, Map<Object, Set<String>> depositors) {
        Breuk a = Breuk.van(pot);
        List<Recipient> eligible = new ArrayList<>();
        List<Breuk> weights = new ArrayList<>();
        Breuk weightSum = ZERO;
        for (Recipient o : recipients) {
            if (o.weight() == null) {
                deposit(depositors, o.recipient(), "EF-028");
                continue;
            }
            Breuk w = Breuk.van(o.weight());
            if (w.signum() < 0) {
                deposit(depositors, o.recipient(), null);
                w = ZERO;
            }
            eligible.add(o);
            weights.add(w);
            weightSum = weightSum.optellen(w);
        }
        if (weightSum.signum() == 0) return new Result(shares, null, depositors);
        Breuk sum = ZERO;
        for (int i = 0; i < eligible.size(); i++) {
            Recipient o = eligible.get(i);
            Breuk share = a.vermenigvuldigen(weights.get(i)).delen(weightSum);
            if (capped) {
                Breuk max = Breuk.van(o.maximum());
                if (max == null || max.signum() < 0) deposit(depositors, o.recipient(), "EF-030");
                // a leeg or negative cap degrades to uncapped (ch. 4.3), as rsPlafond reads it
                else if (share.compareTo(max) > 0) share = max;
            }
            share = floor(share, decimals);
            shares.put(o.recipient(), atRest(share));
            sum = sum.optellen(share);
        }
        return new Result(shares, atRest(a.aftrekken(sum)), depositors);
    }

    /**
     * The groups of equal ordering keys, an empty key last, ascending or descending (ch. 4.2), through
     * {@code rsVerdeelOpVolgorde}. An empty tie-break weight counts as 0 and surfaces the deposit; a negative one
     * counts as 0 without one, as ch. 4.4 and the reference interpreter have it. An empty or negative cap degrades to
     * uncapped and surfaces the deposit, for every recipient, as the reference interpreter reads the cap of every
     * member of every group.
     */
    private static Result ordered(Object pot, List<Recipient> recipients, boolean weighted, boolean capped,
                                      Integer decimals, boolean descending, Map<Object, Object> shares,
                                      Map<Object, Set<String>> depositors) {
        Breuk a = Breuk.van(pot);
        List<Object> facts = new ArrayList<>(), orderKeys = new ArrayList<>();
        List<Object> weights = weighted ? new ArrayList<>() : null, maxima = capped ? new ArrayList<>() : null;
        for (Recipient o : recipients) {
            facts.add(o.recipient());
            // a number by its exact value, so that two keys of one value are one group whatever their scale
            orderKeys.add(o.key() instanceof Number n ? Breuk.van(n) : o.key());
            if (weighted) {
                if (o.weight() == null) deposit(depositors, o.recipient(), "EF-029");
                // a weighted tie group always splits by weight, so an empty weight is the real 0 it counts as
                weights.add(o.weight() == null ? ZERO : Breuk.van(o.weight()));
            }
            if (capped) {
                Breuk max = Breuk.van(o.maximum());
                if (max == null || max.signum() < 0) deposit(depositors, o.recipient(), "EF-030");
                maxima.add(max);
            }
        }
        Map<Object, Object> out = RegelSpraakRuntime.rsVerdeelOpVolgorde(a, facts, orderKeys, !descending, weights,
                maxima, decimals);
        Breuk sum = ZERO;
        for (Object f : facts) {
            Object share = out.get(f);
            if (share == null)
                throw new IllegalStateException("rsVerdeelOpVolgorde gaf geen aandeel voor " + FactNames.object(f));
            shares.put(f, share);
            sum = sum.optellen(Breuk.van(share));
        }
        return new Result(shares, atRest(a.aftrekken(sum)), depositors);
    }

    private static final Breuk ZERO = Breuk.van(BigInteger.ZERO, BigInteger.ONE);

    /** The §9.5 deposit of a degradation on {@code recipient}, with {@code code}, the EF code of its kind, or none (null)
     *  for a negative weight in the proportional method, decided for v1 in ch. 4.4 and witnessed by no golden. */
    private static void deposit(Map<Object, Set<String>> depositors, Object recipient, String code) {
        Set<String> codes = depositors.computeIfAbsent(recipient, r -> new TreeSet<>());
        if (code != null) codes.add(code);
    }

    /** The floor of the exact {@code x} at {@code decimals} decimals (ch. 4.3), rounded once; the identity without
     *  them. */
    private static Breuk floor(Breuk x, Integer decimals) {
        return decimals == null ? x : Breuk.van(x.naarDecimal(decimals, RoundingMode.FLOOR));
    }

    /** {@code x} as the branch carries a number: its decimal where it terminates, the exact rational where it does
     *  not. */
    private static Object atRest(Breuk x) {
        return x.isTerminerend() ? x.naarDecimal() : x;
    }
}
