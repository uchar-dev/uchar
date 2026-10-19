package dev.uchar.regelspraak.transpiler.core;

import dev.uchar.regelspraak.ir.Condition;
import dev.uchar.regelspraak.ir.Expr;
import dev.uchar.regelspraak.refusal.DroolsCode;
import dev.uchar.regelspraak.refusal.Reason;
import dev.uchar.regelspraak.model.Datatype;
import dev.uchar.regelspraak.model.RegelSpraakModel.ObjectType;

import java.util.function.Predicate;

/**
 * The time window's well-formed POSITION and BOUND ({@code spec/IR-SPEC.md} ch. 4.6;
 * {@code spec/RUNTIME-SEMANTICS.md} ch. 3.10), as both Drools branches refuse them.
 *
 * <p>WF-112: a window never sits under GEEN or a counting quantifier — not as a member of a composite at any
 * depth, and not in the filter or toets of a quantifier fold. ALLE and DE carry it. WF-111: each bound is a date
 * literal or the subject's own scalar date attribute.
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
public final class WindowPosition {
    private WindowPosition() {}

    /** The refusal for a window whose bound is neither shape WF-111 admits. */
    public static final Reason BOUND_FORM = DroolsCode.WINDOW_BOUND_NOT_DATE.reason();

    /** The refusal for the first WF-112 position in {@code c}, or null when it has none. Null-safe. */
    public static Reason refusal(Condition c) {
        if (c instanceof Condition.Compound s) {
            if (counts(s.quantifier()) && s.components().stream().anyMatch(Condition::containsTimeWindow))
                return DroolsCode.COMPOUND_COUNTING_WINDOW.reason("quantifier", s.quantifier());
            for (Condition part : s.components()) {
                Reason w = refusal(part);
                if (w != null) return w;
            }
            return null;
        }
        if (c instanceof Condition.QuantifierFold kv) {
            if (counts(kv.quantifier())) {
                boolean inFilter = Condition.containsTimeWindow(kv.filter()), inTest = Condition.containsTimeWindow(kv.test());
                if (inFilter || inTest) {
                    return DroolsCode.QUANTIFIER_FOLD_WINDOW_POSITION.reason(
                            "site", DroolsCode.QUANTIFIER_FOLD_WINDOW_POSITION.part(
                                    inFilter && inTest ? "filter-and-test" : inFilter ? "filter" : "test"),
                            "quantifier", kv.quantifier(),
                            "elementType", kv.elementObjectType() == null
                                    ? DroolsCode.QUANTIFIER_FOLD_WINDOW_POSITION.part("unresolved-element-type")
                                    : kv.elementObjectType().name,
                            "reason", DroolsCode.QUANTIFIER_FOLD_WINDOW_POSITION.part(inTest ? "counts" : "chooses"));
                }
            }
            Reason w = refusal(kv.filter());
            return w != null ? w : refusal(kv.test());
        }
        if (c instanceof Condition.Comparison v) {
            Reason w = inExpression(v.left());
            return w != null ? w : inExpression(v.right());
        }
        if (c instanceof Condition.Predicate p) return inExpression(p.operand());
        return null;
    }

    /** The refusal for the first WF-112 position in the die/dat filter of an aggregation inside {@code e}, or
     *  null. Null-safe. */
    public static Reason inExpression(Expr e) {
        Reason[] first = { null };
        Expr.contains(e, x -> x instanceof Expr.FunctionCall f && (first[0] = refusal(f.filter())) != null);
        return first[0];
    }

    /** Whether a window in {@code c} — as a condition, through composites and folds, not into operands — has a
     *  bound neither WF-111 shape describes for {@code subject}: the shape both branches lower, shared through
     *  {@link #boundLowerable}. */
    public static boolean uncarriableBound(Condition c, ObjectType subject, Predicate<Expr.AttributeReference> navigated) {
        if (c instanceof Condition.TimeWindow per)
            return (per.from() != null && !boundLowerable(per.from(), subject, navigated))
                    || (per.until() != null && !boundLowerable(per.until(), subject, navigated));
        if (c instanceof Condition.Compound s)
            return s.components().stream().anyMatch(d -> uncarriableBound(d, subject, navigated));
        return false;
    }

    /** WF-111's two bound shapes, as both branches lower them: a {@code Datum in dagen} literal, or the subject's own
     *  scalar {@code Datum in dagen} attribute — a read without bindings (IR-SPEC ch. 4.1a) of the subject's type,
     *  with no timeline and no dimension. The read's type is its declaration's (IR-SPEC ch. 2.10). A bound date
     *  computed, navigated or held in a parameter is neither. */
    public static boolean boundLowerable(Expr bound, ObjectType subject, Predicate<Expr.AttributeReference> navigated) {
        if (!(EffectiveUnit.typeWithoutBinder(bound) instanceof Datatype.DateInDays)) return false;
        if (bound instanceof Expr.Literal) return true;
        return bound instanceof Expr.AttributeReference a && !navigated.test(a)
                && a.objectType() != null && a.objectType() == subject
                && subject.granularityOf(a.attribute()) == null
                && subject.dimensionsOf(a.attribute()).isEmpty();
    }

    /** GEEN complements the window's verdict and the three counting quantifiers count it; ALLE and DE only
     *  conjoin it, which is the reading ch. 3.10 gives. */
    private static boolean counts(Condition.Quantifier k) {
        return k != Condition.Quantifier.ALLE && k != Condition.Quantifier.DE;
    }
}
