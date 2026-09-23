package io.github.yhavinga.regelspraak.transpiler.kern;

import io.github.yhavinga.regelspraak.ir.Conditie;
import io.github.yhavinga.regelspraak.ir.Expr;

/**
 * The time window's well-formed POSITION ({@code spec/IR-SPEC.md} ch. 4.6; {@code spec/RUNTIME-SEMANTICS.md}
 * ch. 3.10), as both Drools branches refuse it.
 *
 * <p>WF-112: a window never sits under GEEN or a counting quantifier — not as a member of a composite at any
 * depth, and not in the filter or toets of a quantifier fold. ALLE and DE carry it.
 *
 * <p>The verifier rejects such a document, but gate 12 and the replay API load {@code ir.json} directly, so a
 * document that never went through the verifier (an older wire, a hand-made one) reaches both branches as it is.
 * Measured 2026-09-23 on such documents: the DRL-text branch lowered a window in a fold under all five
 * quantifiers, and under GEEN in a time-free session — where the window never matches — it fired the rule
 * for every subject, the complement reading nothing specifies. So each branch refuses the position itself,
 * and both give the sentence built here.
 *
 * <p>The walk is the verifier's: every composite and every fold is judged by its OWN quantifier over the
 * conditions beneath it, and a window counts only where it is a condition, never inside an operand (the
 * verifier's position test does not enter expressions either). The walk DOES go on into operands to find
 * further positions: a composite in the die/dat filter of an aggregation is a position of its own. The
 * sentence names the node that is ill-formed, not the one around it, so a GEEN fold nested in an ALLE fold is
 * reported with the inner fold's element type.
 */
public final class Vensterpositie {
    private Vensterpositie() {}

    /** The refusal for the first WF-112 position in {@code c}, or null when it has none. Null-safe. */
    public static String weigering(Conditie c) {
        if (c instanceof Conditie.Samengesteld s) {
            if (telt(s.kwantor()) && s.onderdelen().stream().anyMatch(Conditie::bevatPeriode))
                return zin("een " + s.kwantor() + "-samenstelling", TELT);
            for (Conditie deel : s.onderdelen()) {
                String w = weigering(deel);
                if (w != null) return w;
            }
            return null;
        }
        if (c instanceof Conditie.Kwantorvouw kv) {
            if (telt(kv.kwantor())) {
                boolean inFilter = Conditie.bevatPeriode(kv.filter()), inToets = Conditie.bevatPeriode(kv.toets());
                if (inFilter || inToets) {
                    String plek = inFilter && inToets ? "het filter en de toets" : inFilter ? "het filter" : "de toets";
                    return zin(plek + " van een kwantorvouw onder " + kv.kwantor() + " over "
                            + (kv.elementObjecttype() == null ? "<onopgelost elementtype>" : kv.elementObjecttype().naam),
                            inToets ? TELT : KIEST);
                }
            }
            String w = weigering(kv.filter());
            return w != null ? w : weigering(kv.toets());
        }
        if (c instanceof Conditie.Vergelijking v) {
            String w = inExpressie(v.links());
            return w != null ? w : inExpressie(v.rechts());
        }
        if (c instanceof Conditie.Predicaat p) return inExpressie(p.operand());
        return null;
    }

    /** The refusal for the first WF-112 position in the die/dat filter of an aggregation inside {@code e}, or
     *  null. Null-safe. */
    public static String inExpressie(Expr e) {
        String[] eerste = { null };
        Expr.bevat(e, x -> x instanceof Expr.Functie f && (eerste[0] = weigering(f.filter())) != null);
        return eerste[0];
    }

    /** GEEN complements the window's verdict and the three counting quantifiers count it; ALLE and DE only
     *  conjoin it, which is the reading ch. 3.10 gives. */
    private static boolean telt(Conditie.Kwantor k) {
        return k != Conditie.Kwantor.ALLE && k != Conditie.Kwantor.DE;
    }

    private static final String TELT = "de kwantor telt of ontkent het vensterverdict";
    private static final String KIEST = "het venster kiest de leden waarover de kwantor telt of ontkent";

    private static String zin(String plek, String waarom) {
        return "periodevenster in " + plek + " (WF-112): " + waarom + ", terwijl het venster de regel poort"
                + " (RUNTIME-SEMANTICS ch. 3.10), en voor die lezing is niets gespecificeerd";
    }
}
