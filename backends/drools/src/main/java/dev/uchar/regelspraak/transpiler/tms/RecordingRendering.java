package dev.uchar.regelspraak.transpiler.tms;

import dev.uchar.regelspraak.ir.Condition;
import dev.uchar.regelspraak.transpiler.tms.plan.Term;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * The third rendering of a planned condition or value ({@link Term}), beside {@link ClosureRendering} and
 * {@code DrlRendering}: an evaluation over a rule's value array that records, per node of the term, whether the
 * evaluation reached it and what it found, a value or an evaluation fault ({@code TmsTranspiler#readingsOf}, card
 * fixpoint-readings). It evaluates as the rendering into closures does, node for node: the same functions of the plan
 * ({@link ClosureRendering#implementation}), the same stop rule of a composite ({@link ComputeHelpers#established},
 * {@link ComputeHelpers#endVerdict}), the same order (RUNTIME-SEMANTICS ch. 5.10, 5.11), a {@code Let} by need
 * (ch. 10.6) and exactly the branch a choice takes (ch. 10.2). It decides nothing either. A probe in the value array is
 * the caller's, which fills each probe slot with a fresh probe and reads none: a reading deposits nothing.
 */
final class RecordingRendering {

    /** What the evaluation found at one node it reached: its value, or the evaluation fault it raised or passed on, or
     *  neither where a read of a leeg binder ended it (ch. 10.6, clause 2). */
    record Outcome(Object value, RuntimeException fault) {}

    private final Map<Term, Outcome> outcomes = new IdentityHashMap<>();

    /** The outcome of {@code t} where the evaluation reached it, null where it did not. */
    Outcome outcome(Term t) {
        return outcomes.get(t);
    }

    /** The value of {@code t} over {@code w}, every node it reaches recorded; an evaluation fault passes on, recorded at
     *  every node it passes. */
    Object value(Term t, Object[] w) {
        Object v;
        try {
            v = evaluate(t, w);
        } catch (Barrier b) {
            outcomes.put(t, new Outcome(null, null));
            throw b;
        } catch (RuntimeException e) {
            if (ComputeHelpers.isEvaluationFault(e)) outcomes.put(t, new Outcome(null, e));
            throw e;
        }
        outcomes.put(t, new Outcome(v, null));
        return v;
    }

    /** Whether {@code t}, a condition, holds over {@code w}. */
    boolean test(Term t, Object[] w) {
        return (Boolean) value(t, w);
    }

    private Object evaluate(Term t, Object[] w) {
        return switch (t) {
            case Term.Const c -> c.value();
            case Term.SlotValue l -> w[l.slot()];
            case Term.Input i -> {
                Function<Object, Object> read = PatternDslRendering.inputReader(i.field(), i.form(), null);
                yield read.apply(value(i.object(), w));
            }
            case Term.Binder b -> ((Need) w[b.slot()]).read();
            case Term.Let l -> let(l, w);
            case Term.Choice k -> test(k.condition(), w) ? value(k.then(), w) : value(k.otherwise(), w);
            case Term.Call a -> {
                Object[] x = new Object[a.arguments().size()];
                for (int i = 0; i < x.length; i++) x[i] = value(a.arguments().get(i), w);
                yield ClosureRendering.implementation(a.function()).of(x);
            }
            case Term.Restate h -> {
                Object v = value(h.term(), w);
                ClosureRendering.Implementation g = ClosureRendering.implementation(h.function());
                yield h.denominator() != null ? g.of(new Object[]{v, h.counter(), h.denominator()})
                        : g.of(new Object[]{v, h.counter()});
            }
            case Term.Division d -> {
                Object lv = value(d.left(), w), rv = value(d.right(), w);
                yield ClosureRendering.implementation(d.function()).of(new Object[]{lv, rv,
                        d.depositFlag() < 0 ? null : w[d.depositFlag()]});
            }
            case Term.SquareRoot x -> {
                Object v = value(x.radicand(), w);
                yield ClosureRendering.implementation(x.function()).of(new Object[]{v,
                        x.depositFlag() < 0 ? null : w[x.depositFlag()]});
            }
            case Term.Fail f -> ComputeHelpers.fail(f.sentence());
            case Term.Required v -> ComputeHelpers.required(value(v.value(), w), v.sentence());
            case Term.TextComposition s -> {
                List<Object> values = new ArrayList<>(s.parts().size());
                for (Term part : s.parts()) values.add(value(part, w));
                yield ComputeHelpers.textComposition(values);
            }
            case Term.ListedFold g -> {
                ClosureRendering.Implementation step = ClosureRendering.implementation(g.function());
                Object acc = null;
                for (Term op : g.operands()) acc = step.of(new Object[]{acc, value(op, w)});
                yield acc;
            }
            case Term.FoldRead v -> {
                if (v.depositFlag() >= 0) ComputeHelpers.reportFilterDeposits(w[v.depositFlag()], w[v.deposits()]);
                if (v.filterFault() >= 0 && w[v.filterFault()] != null) throw ((FilterFault) w[v.filterFault()]).fault();
                if (v.memberFault() >= 0 && w[v.memberFault()] != null) throw ((MemberFault) w[v.memberFault()]).fault();
                yield value(v.outcome(), w);
            }
            case Term.Compound s -> compound(s, w);
            case Term.QuantifierRead k -> {
                Object n = value(k.count(), w), b = value(k.contributions(), w);
                yield ClosureRendering.implementation(k.function()).of(new Object[]{n, b, k.quantifier(), k.threshold(),
                        k.depositFlag() < 0 ? null : w[k.depositFlag()]});
            }
            case Term.WithoutFault z -> {
                try {
                    yield test(z.condition(), w);
                } catch (RuntimeException e) {
                    if (!ComputeHelpers.isEvaluationFault(e)) throw e;
                    yield false;
                }
            }
            case Term.DayKind d -> d.datePredicate();
        };
    }

    /** A composite: its members read in order, the stop triggers of ch. 5.10 tested before each, as
     *  {@link ClosureRendering} reads them, so a member after the stop is not reached. */
    private boolean compound(Term.Compound s, Object[] w) {
        Condition.Quantifier kw = s.quantifier();
        int threshold = s.threshold(), n = s.members().size(), touch = 0;
        for (int i = 0; i < n; i++) {
            Boolean fixed = ComputeHelpers.established(kw, threshold, touch, i, n - i);
            if (fixed != null) return fixed;
            if (test(s.members().get(i), w)) touch++;
        }
        return ComputeHelpers.endVerdict(kw, threshold, touch, n);
    }

    /** A {@code Let} by need, as {@link ClosureRendering} evaluates it: the body over a copy of the value array with the
     *  need of the binder in its slot, the value evaluated in the enclosing scope at the first read; a read whose value is
     *  leeg ends this {@code Let} with leeg. */
    private Object let(Term.Let l, Object[] w) {
        Barrier barrier = new Barrier();
        Object[] inside = w.clone();
        inside[l.slot()] = new Need(() -> value(l.value(), w), barrier);
        try {
            return value(l.body(), inside);
        } catch (Barrier b) {
            if (b != barrier) throw b;
            return null;
        }
    }

    /** The barrier of one evaluation of a {@code Let}, as in {@link ClosureRendering}: a signal, not a diagnosis. */
    private static final class Barrier extends RuntimeException {
        private static final long serialVersionUID = 1L;

        Barrier() {
            super(null, null, false, false);
        }
    }

    /** The need of a binder in one evaluation of its {@code Let}, as in {@link ClosureRendering}. */
    private static final class Need {
        private final Supplier<Object> value;
        private final Barrier barrier;
        private boolean computed;
        private Object outcome;

        Need(Supplier<Object> value, Barrier barrier) {
            this.value = value;
            this.barrier = barrier;
        }

        Object read() {
            if (!computed) {
                outcome = value.get();
                computed = true;
            }
            if (outcome == null) throw barrier;
            return outcome;
        }
    }

    /** {@code v} as a reading gives a value: an exact rational collapsed to its decimal at the write boundary
     *  ({@link ComputeHelpers#collapse}), a whole number of the analysis's own counting as a {@code BigDecimal}, and the
     *  asOfDate-bucket scalar of a timeline as the timeline of its one period, which the evaluation reads
     *  (RUNTIME-SEMANTICS ch. 5.12, 8.4). */
    static Object given(Object v) {
        if (v instanceof BucketScalar s) return s.timeline();
        if (v instanceof Integer i) return BigDecimal.valueOf(i);
        if (v instanceof Long l) return BigDecimal.valueOf(l);
        return ComputeHelpers.collapse(v);
    }
}
