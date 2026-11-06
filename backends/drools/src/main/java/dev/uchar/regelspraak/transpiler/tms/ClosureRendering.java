package dev.uchar.regelspraak.transpiler.tms;

import dev.uchar.regelspraak.ir.Condition;
import dev.uchar.regelspraak.runtime.RegelSpraakRuntime;
import dev.uchar.regelspraak.transpiler.tms.plan.StaticFunction;
import dev.uchar.regelspraak.transpiler.tms.plan.Term;
import dev.uchar.regelspraak.transpiler.tms.plan.TestItem;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoField;
import java.time.temporal.Temporal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * The rendering of a planned value or condition ({@link Term}) into a function over the value array of a rule
 * ({@link ValueFunction}) or a predicate over it, the form in which the consequences and the tests of the executable
 * model compute it. Each node becomes the lambda the
 * value compilers made before the values were planned, with the same calls in the same order, the same guards and the
 * same faults. It decides nothing: the functions, the unit factors, the probes, the faults and the order of evaluation
 * are the plan's ({@code TmsTranspiler}), and a function of the plan is called here by its constant, never chosen. The
 * same holds for the role of a rule's test ({@link TestItem.Role}), which the analysis sets and this class carries out.
 *
 * <p>A {@code Let} is rendered by need, as RUNTIME-SEMANTICS ch. 10.6 evaluates it, whatever {@link Term.Let#eager}
 * says: a lambda can carry the need, and evaluating by need is right in every case, where evaluating first is right
 * only where the plan marks it so.
 */
final class ClosureRendering {

    private ClosureRendering() {}

    /** The function over the value array that computes {@code t}. */
    static ValueFunction valueFunction(Term t) {
        return switch (t) {
            case Term.Const c -> {
                Object v = c.value();
                yield w -> v;
            }
            case Term.SlotValue l -> {
                int i = l.slot();
                yield w -> w[i];
            }
            case Term.Input i -> {
                ValueFunction object = valueFunction(i.object());
                Function<Object, Object> read = PatternDslRendering.inputReader(i.field(), i.form(), null);
                yield w -> read.apply(object.of(w));
            }
            case Term.Binder b -> {
                int i = b.slot();
                yield w -> ((Need) w[i]).read();
            }
            case Term.Let l -> let(l);
            case Term.Choice k -> {
                Predicate<Object[]> condition = test(k.condition());
                ValueFunction then = valueFunction(k.then()), otherwise = valueFunction(k.otherwise());
                yield w -> condition.test(w) ? then.of(w) : otherwise.of(w);
            }
            case Term.Call a -> call(a.function(), a.arguments());
            case Term.Restate h -> restate(h);
            case Term.Division d -> {
                ValueFunction l = valueFunction(d.left()), r = valueFunction(d.right());
                Implementation f = implementation(d.function());
                int depositFlag = d.depositFlag();
                yield w -> {
                    Object lv = l.of(w), rv = r.of(w);
                    return f.of(new Object[]{lv, rv, depositFlag < 0 ? null : w[depositFlag]});
                };
            }
            case Term.SquareRoot x -> {
                ValueFunction radicand = valueFunction(x.radicand());
                Implementation f = implementation(x.function());
                int depositFlag = x.depositFlag();
                yield w -> {
                    Object v = radicand.of(w);
                    return f.of(new Object[]{v, depositFlag < 0 ? null : w[depositFlag]});
                };
            }
            case Term.Fail f -> {
                String sentence = f.sentence();
                yield w -> ComputeHelpers.fail(sentence);
            }
            case Term.Required v -> {
                ValueFunction f = valueFunction(v.value());
                String sentence = v.sentence();
                yield w -> ComputeHelpers.required(f.of(w), sentence);
            }
            case Term.TextComposition s -> {
                List<ValueFunction> parts = s.parts().stream().map(ClosureRendering::valueFunction).toList();
                yield w -> {
                    List<Object> values = new ArrayList<>(parts.size());
                    for (ValueFunction part : parts) values.add(part.of(w));
                    return ComputeHelpers.textComposition(values);
                };
            }
            case Term.ListedFold g -> listedFold(g);
            case Term.FoldRead v -> foldRead(v);
            case Term.Compound s -> {
                Predicate<Object[]> p = compound(s);
                yield w -> p.test(w);
            }
            case Term.QuantifierRead k -> quantifierRead(k);
            case Term.WithoutFault z -> {
                Predicate<Object[]> p = test(z);
                yield w -> p.test(w);
            }
            case Term.DayKind d -> {
                Predicate<LocalDate> p = d.datePredicate();
                yield w -> p;
            }
        };
    }

    /** The test that {@code t}, a condition, holds: a composite as its fold, a condition tested without its fault as
     *  that test, any other term as its value, a truth value. */
    static Predicate<Object[]> test(Term t) {
        if (t instanceof Term.Compound s) return compound(s);
        if (t instanceof Term.WithoutFault z) return withoutFault(test(z.condition()));
        ValueFunction f = valueFunction(t);
        return w -> (Boolean) f.of(w);
    }

    /** The test of {@code t}, the condition of a rule's left-hand side, that asks {@code role} of it
     *  ({@link TestItem}). */
    static Predicate<Object[]> test(TestItem t) {
        return test(t.role(), test(t.test()), t.depositFlag());
    }

    /** The test of a rule's left-hand side that asks {@code role} of the condition {@code test}
     *  ({@link TestItem.Role}); {@code depositFlag} is the probe's slot of a test of deposits. */
    static Predicate<Object[]> test(TestItem.Role role, Predicate<Object[]> test, int depositFlag) {
        return switch (role) {
            case CONDITION, VORM -> test;
            case WITHOUT_FAULT -> withoutFault(test);
            case FAULT -> w -> fault(test, w) != null;
            case DEPOSIT -> w -> {
                Object s = ComputeHelpers.depositFlag();
                w[depositFlag] = s;
                try {
                    test.test(w);
                } catch (RuntimeException e) {
                    // a fault is the fault rule's; what the evaluation deposited before it stays heard
                    if (!ComputeHelpers.isEvaluationFault(e)) throw e;
                }
                return ComputeHelpers.reported(s);
            };
        };
    }

    /** {@code test} where it answers, and not holding where it faults: the fault is the fault rule's
     *  ({@link TestItem.Role#WITHOUT_FAULT}). */
    static Predicate<Object[]> withoutFault(Predicate<Object[]> test) {
        return w -> {
            try {
                return test.test(w);
            } catch (RuntimeException e) {
                if (!ComputeHelpers.isEvaluationFault(e)) throw e;
                return false;
            }
        };
    }

    /** The evaluation fault {@code test} raises over {@code w}, or null where it answers. An evaluation fault is a
     *  division by a numeric zero (RUNTIME-SEMANTICS ch. 1.4, point 4) or a {@link ComputeFault}: a date step or a duration
     *  count outside the range of ch. 3.8, a context value or a validity window without an evaluation date, an
     *  {@code Unresolved} node reached, and the other faults that class names. Any other exception is a broken contract
     *  or a bug and leaves the fire as it is. */
    static RuntimeException fault(Predicate<Object[]> test, Object[] w) {
        try {
            test.test(w);
            return null;
        } catch (RuntimeException e) {
            if (!ComputeHelpers.isEvaluationFault(e)) throw e;
            return e;
        }
    }

    /** The function {@code function} on the values of {@code arguments}, evaluated in order. */
    private static ValueFunction call(StaticFunction function, List<Term> arguments) {
        Implementation f = implementation(function);
        ValueFunction[] argument = new ValueFunction[arguments.size()];
        for (int i = 0; i < argument.length; i++) argument[i] = valueFunction(arguments.get(i));
        return w -> {
            Object[] x = new Object[argument.length];
            for (int i = 0; i < x.length; i++) x[i] = argument[i].of(w);
            return f.of(x);
        };
    }

    /** A restatement with the numerator and the denominator its plan carries ({@link Term.Restate}). */
    private static ValueFunction restate(Term.Restate h) {
        ValueFunction f = valueFunction(h.term());
        BigDecimal counter = h.counter(), denominator = h.denominator();
        return w -> ComputeHelpers.restateExact(f.of(w), counter, denominator);
    }

    /**
     * A {@code Let} (RUNTIME-SEMANTICS ch. 10.6): the body evaluated with the binder bound to its value as a need. The
     * value is evaluated in the enclosing scope (WF-076), at the first read of the binder, and that one result serves
     * every further read (clause 1); a read whose value is leeg ends the {@code Let} with leeg (clause 2), the barrier
     * belonging to the {@code Let} whose binder was read; a body that never reads the binder evaluates nothing of the
     * value, so no leeg, no fault and no deposit come of it (clause 3). The binder is a slot of the value array that no
     * pattern binds: the {@code Let} hands its body a copy of the array with the need in that slot, so the lambda keeps
     * no state and one KieBase serves any number of sessions.
     */
    private static ValueFunction let(Term.Let l) {
        ValueFunction value = valueFunction(l.value()), body = valueFunction(l.body());
        int slot = l.slot();
        return w -> {
            Barrier barrier = new Barrier();
            Object[] inside = w.clone();
            inside[slot] = new Need(() -> value.of(w), barrier);
            try {
                return body.of(inside);
            } catch (Barrier b) {
                if (b != barrier) throw b;
                return null;
            }
        };
    }

    /** A composite ({@link Term.Compound}): its members read in order, the stop triggers of RUNTIME-SEMANTICS
     *  ch. 5.10 tested before each, and the quantifier's verdict where none stops it, so a member after the stop is not
     *  evaluated. */
    private static Predicate<Object[]> compound(Term.Compound s) {
        List<Predicate<Object[]>> members = s.members().stream().map(ClosureRendering::test).toList();
        Condition.Quantifier kw = s.quantifier();
        int threshold = s.threshold();
        return w -> {
            int touch = 0;
            for (int i = 0; i < members.size(); i++) {
                Boolean fixed = ComputeHelpers.established(kw, threshold, touch, i, members.size() - i);
                if (fixed != null) return fixed;
                if (members.get(i).test(w)) touch++;
            }
            return ComputeHelpers.endVerdict(kw, threshold, touch, members.size());
        };
    }

    /** The verdict of a quantifier fold whose matrix can fault or deposit ({@link Term.QuantifierRead}): the count and
     *  the outcomes evaluated in that order, then the plan's function over them with the quantifier, the threshold and
     *  the probe in the node's slot, or nothing where the node has none. */
    private static ValueFunction quantifierRead(Term.QuantifierRead k) {
        ValueFunction count = valueFunction(k.count()), contributions = valueFunction(k.contributions());
        Implementation f = implementation(k.function());
        Condition.Quantifier kw = k.quantifier();
        int threshold = k.threshold(), depositFlag = k.depositFlag();
        return w -> {
            Object n = count.of(w), b = contributions.of(w);
            return f.of(new Object[]{n, b, kw, threshold, depositFlag < 0 ? null : w[depositFlag]});
        };
    }

    /** A listed fold: each operand taken by the plan's step as it is evaluated, so the order of evaluation and
     *  comparison is the value compiler's ({@link Term.ListedFold}). */
    private static ValueFunction listedFold(Term.ListedFold g) {
        Implementation step = implementation(g.function());
        List<ValueFunction> operands = g.operands().stream().map(ClosureRendering::valueFunction).toList();
        return w -> {
            Object acc = null;
            for (ValueFunction op : operands) acc = step.of(new Object[]{acc, op.of(w)});
            return acc;
        };
    }

    /** The evaluation of a fold in the rule that reads it ({@link Term.FoldRead}): its filter's deposit heard by this
     *  rule's probe, then the first filter fault, then the first member fault, then the fold's value. */
    private static ValueFunction foldRead(Term.FoldRead v) {
        ValueFunction outcome = valueFunction(v.outcome());
        int deposits = v.deposits(), depositFlag = v.depositFlag(), filterFault = v.filterFault(), memberFault = v.memberFault();
        return w -> {
            if (depositFlag >= 0) ComputeHelpers.reportFilterDeposits(w[depositFlag], w[deposits]);
            if (filterFault >= 0 && w[filterFault] != null) throw ((FilterFault) w[filterFault]).fault();
            // the reference interpreter reads the members' values once every member's filter is evaluated
            if (memberFault >= 0 && w[memberFault] != null) throw ((MemberFault) w[memberFault]).fault();
            return outcome.of(w);
        };
    }

    /** The constraint of a pattern ({@link dev.uchar.regelspraak.transpiler.tms.plan.BetaTest}) that calls
     *  {@code function} over the pattern's object and the object another pattern binds, in that order, and the values of
     *  {@code constants} after them. */
    static org.drools.model.functions.Predicate2<Object, Object> betaTest(StaticFunction function,
                                                                          List<Term.Const> constants) {
        Implementation f = implementation(function);
        Object[] c = constants.stream().map(Term.Const::value).toArray();
        return (x, o) -> {
            Object[] a = new Object[2 + c.length];
            a[0] = x;
            a[1] = o;
            System.arraycopy(c, 0, a, 2, c.length);
            return (Boolean) f.of(a);
        };
    }

    /** A function of the plan as a call on evaluated arguments; the recording rendering calls the same
     *  ({@link RecordingRendering}). */
    @FunctionalInterface
    interface Implementation {
        Object of(Object[] x);
    }

    /** The call of {@code function}: the method its constant names, with the arguments cast as its parameters take
     *  them. */
    @SuppressWarnings("unchecked")
    static Implementation implementation(StaticFunction function) {
        return switch (function) {
            case RS_PLUS -> x -> RegelSpraakRuntime.rsPlus(x[0], x[1]);
            case RS_MIN -> x -> RegelSpraakRuntime.rsMin(x[0], x[1]);
            case RS_REDUCED -> x -> RegelSpraakRuntime.rsVerminderd(x[0], x[1]);
            case RS_TIMES -> x -> RegelSpraakRuntime.rsMaal(x[0], x[1]);
            case RS_PERCENTAGE_OF -> x -> RegelSpraakRuntime.rsPercentageVan(x[0], x[1]);
            case RS_POWER -> x -> RegelSpraakRuntime.rsMacht(x[0], (Integer) x[1]);
            case RS_ROUNDING -> x -> RegelSpraakRuntime.rsAfronding(x[0], (Integer) x[1], (RoundingMode) x[2]);
            case RS_CLAMP -> x -> RegelSpraakRuntime.rsBegrens(x[0], x[1], x[2]);
            case RS_ABS -> x -> RegelSpraakRuntime.rsAbs(x[0]);
            case RS_TEXT -> x -> RegelSpraakRuntime.rsTekst(x[0], x[1], (String) x[2]);
            case RS_DURATION -> x -> RegelSpraakRuntime.rsTijdsduur((Temporal) x[0], (Temporal) x[1], (String) x[2],
                    (Boolean) x[3]);
            case RS_DURATION_DAY_KIND -> x -> RegelSpraakRuntime.rsTijdsduurDagsoort((LocalDate) x[0],
                    (LocalDate) x[1], (Predicate<LocalDate>) x[2], (Boolean) x[3]);
            case RS_DATE_PART -> x -> RegelSpraakRuntime.rsDatumDeel(x[0], (ChronoField) x[1]);
            case RS_EVALUATION_YEAR -> x -> RegelSpraakRuntime.rsRekenjaar((LocalDate) x[0]);
            case RS_DIM_SUM -> x -> RegelSpraakRuntime.rsDimSom((Map<?, ?>) x[0], (List<?>) x[1]);
            case RS_DIM_MIN -> x -> RegelSpraakRuntime.rsDimMin((Map<?, ?>) x[0], (List<?>) x[1]);
            case RS_DIM_MAX -> x -> RegelSpraakRuntime.rsDimMax((Map<?, ?>) x[0], (List<?>) x[1]);
            case RS_TIME_CLIP -> x -> RegelSpraakRuntime.rsTijdKnip((List<?>) x[0], (List<?>) x[1], (Class<?>) x[2]);
            case RS_TIME_CLIP_NOT -> x -> RegelSpraakRuntime.rsTijdKnipNiet((List<?>) x[0], (List<?>) x[1],
                    (Class<?>) x[2]);
            case RS_TIME_CONDITION -> x -> RegelSpraakRuntime.rsTijdConditie((List<?>) x[0], (Integer) x[1], x[2],
                    (List<?>) x[3], (Class<?>) x[4]);
            case PRO_RATA -> x -> ComputeHelpers.proRata(x[0], x[1], (String) x[2], (Integer) x[3], (String) x[4]);
            case RS_DAY_COUNT -> x -> RegelSpraakRuntime.rsAantalDagenIn((List<?>) x[0], (String) x[1],
                    (Class<?>) x[2]);
            case RS_DAY_COUNT_FROM -> x -> RegelSpraakRuntime.rsAantalDagenInVanaf((List<?>) x[0],
                    (String) x[1], (LocalDate) x[2], (Class<?>) x[3]);
            case PERIOD_LIST -> x -> Period.list((List<?>) x[0]);
            case PERIOD_DAY_COUNT -> x -> PeriodArithmetic.dayCount(x[0]);
            case PERIOD_SCALAR -> x -> PeriodArithmetic.scalar(x[0]);
            case PERIOD_PLUS -> x -> PeriodArithmetic.plus(x[0], x[1]);
            case PERIOD_MINUS -> x -> PeriodArithmetic.minus(x[0], x[1]);
            case PERIOD_REDUCED -> x -> PeriodArithmetic.reduced(x[0], x[1]);
            case PERIOD_TIMES -> x -> PeriodArithmetic.times(x[0], x[1]);
            case PERIOD_PERCENTAGE_OF -> x -> PeriodArithmetic.percentageOf(x[0], x[1]);
            case PERIOD_DIVIDED -> x -> PeriodArithmetic.divided(x[0], x[1], x[2]);
            case PERIOD_DIVIDED_ABS -> x -> PeriodArithmetic.dividedAbs(x[0], x[1], x[2]);
            case PERIOD_PARAMETER -> x -> PeriodArithmetic.parameter(x[0], (String) x[1]);
            case PERIOD_SUM -> x -> PeriodArithmetic.sum(x[0], x[1]);
            case PERIOD_WRITTEN -> x -> PeriodArithmetic.written(x[0], (Integer) x[1], (String) x[2]);
            case PERIOD_WRITTEN_VALUES -> x -> PeriodArithmetic.writtenValues(x[0]);
            case PERIOD_TOTAL -> x -> PeriodArithmetic.total(x[0], (String) x[1], (String) x[2], (String) x[3],
                    (String) x[4], x[5]);
            case PERIOD_CLIPPED -> x -> PeriodArithmetic.clipped(x[0], x[1]);
            case PERIOD_TRUE_PERIODS -> x -> PeriodCondition.truePeriods(x[0]);
            case PERIOD_OUTSIDE -> x -> PeriodCondition.outside(x[0]);
            case PERIOD_WHERE -> x -> PeriodCondition.where(x[0], x[1], (String) x[2], (Boolean) x[3]);
            case PERIOD_WHERE_EQUAL -> x -> PeriodCondition.whereEqual(x[0], x[1], (Boolean) x[2], (Boolean) x[3]);
            case PERIOD_WHOLE_BUCKETS -> x -> PeriodCondition.wholeBuckets(x[0], (String) x[1], (Boolean) x[2]);
            case PERIOD_MEMBER -> x -> PeriodCondition.member(x[0], x[1]);
            case PERIOD_COMPOSITE -> x -> PeriodCondition.composite(x[0], (Condition.Quantifier) x[1], (Integer) x[2]);
            case PERIOD_ALWAYS -> x -> PeriodCondition.always(x[0]);
            case PERIOD_DAY_KIND -> x -> PeriodCondition.dayKind(x[0], (Predicate<LocalDate>) x[1], (Boolean) x[2]);
            case PERIOD_ROUNDED -> x -> PeriodArithmetic.rounded(x[0], (Integer) x[1], (RoundingMode) x[2]);
            case PERIOD_WINDOW -> x -> PeriodCondition.window(x[0], (Boolean) x[1], x[2], (Boolean) x[3], (Boolean) x[4]);
            case PERIOD_CHARACTERISTIC -> x -> PeriodCondition.characteristicPeriods(x[0], x[1], (String) x[2], x[3]);
            case DAYS_JOINED -> x -> PeriodDefaults.joined(x[0], x[1]);
            case DAYS_REACH -> x -> PeriodDefaults.reach(x[0]);
            case DAYS_LEFT -> x -> PeriodDefaults.left(x[0], x[1]);
            case DAYS_AT_TURN -> x -> PeriodDefaults.atTurn(x[0]);
            case DAYS_WITHIN -> x -> PeriodDefaults.within(x[0], x[1]);
            case DAYS_DECIDED -> x -> PeriodDefaults.decided(x[0], x[1]);
            case DAYS_FILLED_WITHIN -> x -> PeriodDefaults.filledWithin(x[0], x[1]);
            case DAYS_INPUT_EMPTY -> x -> PeriodDefaults.inputEmpty(x[0], x[1], (String) x[2], x[3]);
            case DAYS_INITIALISATION -> x -> PeriodDefaults.initialisationDays(x[0], x[1], x[2], (String) x[3], x[4]);
            case DAYS_INITIALISED -> x -> PeriodDefaults.initialised(x[0], x[1], (Integer) x[2], (String) x[3]);
            case DAYS_INITIALISED_VALUES -> x -> PeriodDefaults.initialisedValues(x[0], x[1]);
            case DAYS_RECONCILED -> x -> PeriodDefaults.reconciled((String) x[0], x[1]);
            case DAYS_DECIDED_OF -> x -> PeriodDefaults.decidedOf(x[0]);
            case DAYS_CONFLICTED -> x -> PeriodDefaults.conflicted(x[0], (String) x[1]);
            case DIVIDED -> x -> ComputeHelpers.divided(x[0], x[1], x[2]);
            case DIVIDED_ABS -> x -> ComputeHelpers.dividedAbs(x[0], x[1], x[2]);
            case INVERTED_BASE -> x -> ComputeHelpers.invertedBase(x[0], x[1], x[2]);
            case RADICAND -> x -> ComputeHelpers.radicand(x[0], x[1]);
            case ROUNDED_SQUARE_ROOT -> x -> ComputeHelpers.roundedSquareRoot(x[0], (Integer) x[1], (RoundingMode) x[2]);
            case EXACT_POWER -> x -> ComputeHelpers.exactPower(x[0], x[1]);
            case ROUNDED_POWER -> x -> ComputeHelpers.roundedPower(x[0], x[1], (Integer) x[2], (RoundingMode) x[3]);
            case EXACT_SQUARE_ROOT -> x -> ComputeHelpers.exactSquareRoot(x[0], x[1]);
            case DATE_STEP -> x -> ComputeHelpers.dateStep(x[0], x[1], (String) x[2], (Boolean) x[3]);
            case DAY_KIND_STEP -> x -> ComputeHelpers.dayKindStep(x[0], x[1], (Predicate<LocalDate>) x[2],
                    (Boolean) x[3]);
            case DATE_TIME_DAY_KIND_STEP -> x -> ComputeHelpers.dateTimeDayKindStep(x[0], x[1],
                    (Predicate<LocalDate>) x[2], (Boolean) x[3]);
            case EASTER_SUNDAY -> x -> ComputeHelpers.easterSunday(x[0]);
            case DATUM_MET -> x -> ComputeHelpers.dateWith((String) x[0], x[1], x[2], x[3]);
            case DATUMTIJD_MET -> x -> ComputeHelpers.dateTimeWith((String) x[0], x[1], x[2], x[3], x[4], x[5], x[6],
                    x[7]);
            case CELL -> x -> ComputeHelpers.cell(x[0], (List<?>) x[1]);
            case CELL_FIRST -> x -> ComputeHelpers.firstCell(x[0], (List<?>) x[1]);
            case CELL_LAST -> x -> ComputeHelpers.lastCell(x[0], (List<?>) x[1]);
            case FILLED_CELLS -> x -> ComputeHelpers.filledCells(x[0], (List<?>) x[1]);
            case MAP -> x -> ComputeHelpers.map(x[0]);
            case WITH_CELL -> x -> ComputeHelpers.withCell(x[0], (List<?>) x[1], x[2]);
            case RESTATE_EXACT -> x -> ComputeHelpers.restateExact(x[0], (BigDecimal) x[1], (BigDecimal) x[2]);
            case SMALLEST -> x -> ComputeHelpers.smallest(x[0], x[1]);
            case LARGEST -> x -> ComputeHelpers.largest(x[0], x[1]);
            case FALLBACK -> x -> ComputeHelpers.fallback(x[0], x[1]);
            case CONSTRUCTOR_VALUE -> x -> ComputeHelpers.constructorValue((String) x[0]);
            case CONSTRUCT -> x -> ComputeHelpers.construct((String) x[0], x[1]);
            case PAYLOAD -> x -> ComputeHelpers.payload(x[0], (String) x[1]);
            case COUNT -> x -> ComputeHelpers.countOf(x[0]);
            case FOLD_VALUE -> x -> ComputeHelpers.foldValue(x[0], x[1], x[2]);
            case OWN_TOTAL -> x -> ComputeHelpers.ownTotal(x[0], x[1], x[2], (String) x[3], (String) x[4], (String) x[5],
                    (String) x[6]);
            case COMPLETE -> x -> ComputeHelpers.complete(x[0], (String) x[1], x[2], (String) x[3], (String) x[4],
                    (Boolean) x[5]);
            case COLLAPSE -> x -> ComputeHelpers.collapse(x[0]);
            case FIT_TARGET -> x -> ComputeHelpers.fitTarget(x[0], (Integer) x[1], (String) x[2], (String) x[3]);
            case RS_AGG_COMPARE -> x -> RegelSpraakRuntime.rsAggVergelijk(x[0], x[1], (String) x[2]);
            case DATE_COMPARE -> x -> ComputeHelpers.dateCompare(x[0], x[1], (Condition.Operator) x[2]);
            case TEXT_EQUAL -> x -> ComputeHelpers.textEqual(x[0], x[1], (Boolean) x[2]);
            case RS_ENUM_NAME -> x -> RegelSpraakRuntime.rsEnumNaam(x[0]);
            case EMPTY_TEST -> x -> ComputeHelpers.emptyTest(x[0], (Boolean) x[1]);
            case ELEVEN_TEST -> x -> ComputeHelpers.elevenTest(x[0], (Boolean) x[1]);
            case NUMERIEK_MET_EXACT -> x -> ComputeHelpers.exactDigits(x[0], (Integer) x[1], (Boolean) x[2]);
            case KENMERK -> x -> ComputeHelpers.characteristic(x[0], (Boolean) x[1]);
            case SAME_OBJECT -> x -> ComputeHelpers.sameObject(x[0], x[1], (Boolean) x[2]);
            case CHARACTERISTIC_CHECK_ALONG_PATH -> x -> ComputeHelpers.characteristicCheckAlongPath(x[0], (Boolean) x[1]);
            case ROLE_COUNT -> x -> ComputeHelpers.roleCount(x[0], (Boolean) x[1]);
            case ROLE_FIELD -> x -> ComputeHelpers.roleField(x[0], (Boolean) x[1]);
            case TIME_WINDOW -> x -> ComputeHelpers.timeWindow(x[0], (Boolean) x[1], x[2], (Boolean) x[3],
                    (Boolean) x[4], x[5]);
            case WHOLE_PERIOD -> x -> ComputeHelpers.wholePeriod(x[0], x[1], (String) x[2], (Integer) x[3],
                    (Boolean) x[4]);
            case QUANTIFIER_VERDICT -> x -> ComputeHelpers.quantifierVerdict(x[0], x[1], (Condition.Quantifier) x[2],
                    (Integer) x[3]);
            case QUANTIFIER_READ -> x -> ComputeHelpers.quantifierRead(x[0], x[1], (Condition.Quantifier) x[2], (Integer) x[3],
                    x[4]);
            case CHANNEL -> x -> ComputeHelpers.channel(x[0]);
            case NIET -> x -> ComputeHelpers.not(x[0]);
            case BUCKET_SCALAR -> x -> ComputeHelpers.bucketScalar(x[0], (Boolean) x[1]);
            case OPEN_NONZERO -> x -> PeriodArithmetic.openNonZero(x[0]);
            case DIFFER -> x -> ComputeHelpers.differ(x[0]);
            case DUPLICATE -> x -> ComputeHelpers.duplicate(x[0]);
            case COLLIDES_WITH -> x -> ComputeHelpers.collidesWith(x[0], x[1]);
            case MEETING -> x -> ComputeHelpers.meeting(x[0], x[1], (List<?>) x[2], (List<?>) x[3]);
            case PLACE_OF -> x -> ComputeHelpers.placeOf(x[0], x[1], (List<?>) x[2]);
            case IN_PHASE -> x -> ComputeHelpers.inPhase(x[0], x[1]);
            case NOT_IN_PHASE -> x -> ComputeHelpers.notInPhase(x[0], x[1]);
            case OTHER_FILLING -> x -> ComputeHelpers.otherFilling(x[0], x[1], (String) x[2]);
            case EARLIER_MEMBER -> x -> ComputeHelpers.earlierMember(x[0], x[1]);
            case OTHER_DECISION -> x -> ComputeHelpers.anotherDecision(x[0], x[1]);
        };
    }

    /** The barrier of one evaluation of a {@code Let} (RUNTIME-SEMANTICS ch. 10.6, clause 2): raised by a read of its
     *  binder whose value is leeg, and caught by that evaluation alone, since every evaluation makes its own, so a
     *  nested {@code Let} lets an enclosing one's barrier pass. A signal, not a diagnosis: no stack trace. */
    private static final class Barrier extends RuntimeException {
        private static final long serialVersionUID = 1L;

        Barrier() {
            super(null, null, false, false);
        }
    }

    /** The need of a binder in one evaluation of its {@code Let} (RUNTIME-SEMANTICS ch. 10.6, clause 1): the value,
     *  evaluated at the first read and kept for every further one; a leeg value raises the {@code Let}'s barrier at each
     *  read. Made inside that evaluation and reached only through its copy of the value array, so it is never shared
     *  between sessions or threads. */
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
}
