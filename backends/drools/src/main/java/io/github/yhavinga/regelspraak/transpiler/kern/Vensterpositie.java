package io.github.yhavinga.regelspraak.transpiler.kern;

import io.github.yhavinga.regelspraak.ir.Conditie;
import io.github.yhavinga.regelspraak.ir.Expr;

/**
 * WF-112 ({@code spec/IR-SPEC.md} ch. 4.6; {@code spec/RUNTIME-SEMANTICS.md} ch. 3.10): a time window never
 * sits under GEEN or a counting quantifier — not as a member of a composite at any depth, and not in the
 * filter or toets of a quantifier fold. ALLE and DE carry it.
 *
 * <p>The verifier rejects such a document, but gate 12 and the replay API load {@code ir.json} directly, so a
 * document that never went through the verifier (an older wire, a hand-made one) reaches both branches as it is.
 * Measured 2026-09-23 on such documents: the DRL-text branch lowered a window in a fold under all five
 * quantifiers, and under GEEN in a time-free session — where the window never matches — it fired the rule
 * for every subject, the complement reading nothing specifies. So each branch refuses the position itself,
 * and both give the sentence built here.
 *
 * <p>The walk is the verifier's: every composite and every fold is judged by its OWN quantifier over the
 * conditions beneath it, so a GEEN composite inside an ALLE fold's toets is caught on the composite. A window
 * counts only where it is a condition, never inside an operand (the verifier's position test does not enter
 * expressions either). The walk DOES go on into operands to find further positions: a composite in the
 * die/dat filter of an aggregation is a position of its own.
 */
public final class Vensterpositie {
    private Vensterpositie() {}

    /** The refusal for the first WF-112 position in {@code c}, or null when it has none. Null-safe. */
    public static String weigering(Conditie c) {
        if (c instanceof Conditie.Samengesteld s) {
            if (telt(s.kwantor()) && s.onderdelen().stream().anyMatch(Conditie::bevatPeriode))
                return zin("een " + s.kwantor() + "-samenstelling");
            for (Conditie deel : s.onderdelen()) {
                String w = weigering(deel);
                if (w != null) return w;
            }
            return null;
        }
        if (c instanceof Conditie.Kwantorvouw kv) {
            if (telt(kv.kwantor()) && (Conditie.bevatPeriode(kv.filter()) || Conditie.bevatPeriode(kv.toets())))
                return zin("filter of toets van een kwantorvouw onder " + kv.kwantor());
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

    /** The refusal for the first die/dat filter inside {@code e} — an aggregation's subselection, read in an
     *  operand or a value — that places a window under GEEN or a counting quantifier, or null. Null-safe. */
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

    private static String zin(String plek) {
        return "periodevenster in " + plek + " (WF-112): de kwantor telt of ontkent het vensterverdict, terwijl"
                + " het venster de regel poort (RUNTIME-SEMANTICS ch. 3.10), en voor die lezing is niets gespecificeerd";
    }
}
