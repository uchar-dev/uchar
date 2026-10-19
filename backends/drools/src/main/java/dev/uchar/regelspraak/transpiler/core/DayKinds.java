package dev.uchar.regelspraak.transpiler.core;

import dev.uchar.regelspraak.ir.Condition;
import dev.uchar.regelspraak.ir.Expr;
import dev.uchar.regelspraak.ir.ResolvedDayKindDefinition;
import dev.uchar.regelspraak.core.Literals;
import dev.uchar.regelspraak.model.Datatype;
import dev.uchar.regelspraak.model.RegelSpraakModel;
import dev.uchar.regelspraak.runtime.RegelSpraakRuntime;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * THE §9.8 dagsoortdefinitie compiler — the one implementation both back-ends share (single shared implementation).
 * A definitie's condition over "de dag" compiles to a {@code Predicate<LocalDate>}: the exec model
 * captures it directly in its lambdas, and the DRL back-end declares it as a session {@code global}
 * ({@code rsDagsoort_<Naam>}) that the session builder supplies from {@link #datePredicate} — so the
 * predicate SEMANTICS exist exactly once, and the generated DRL carries no {@code function} bodies.
 *
 * <p>Compilation is idempotent and cycle-safe: a nested dagsoort reference (a feestdag tests
 * kerstdag/paasdag) resolves via a DEFERRED map lookup, so cross-references work regardless of
 * compilation order and a (spec-forbidden, §9.10) cyclic definitie cannot infinite-loop the compile.
 *
 * <p>The admitted condition shapes are deliberately narrow and identical for both back-ends — a
 * Vergelijking of same-comparable-type date operands, a (negated) dagsoort membership, a
 * Samengesteld with any §13.4.14 kwantor — over the §9.8-realistic operands: "de dag" itself, a
 * date-component of it (§13.4.17.18-.24, via {@link RegelSpraakRuntime#rsDatumDeel}), "de eerste
 * paasdag van" (§13.4.17.37), or a fact-free literal. Anything else returns null and the calling
 * back-end markers/skips honestly, so the per-date predicate never diverges between back-ends.
 */
public final class DayKinds {
    private final RegelSpraakModel model;
    private final Map<String, Predicate<LocalDate>> predicates = new LinkedHashMap<>();
    private final java.util.Set<String> busy = new java.util.HashSet<>();

    public DayKinds(RegelSpraakModel model) { this.model = model; }

    /** Compile (idempotently, cycle-safe) the §9.8 definitie of {@code naam}; null when no such definitie
     *  exists or its condition is not a pure date predicate (the caller then markers/skips). */
    public Predicate<LocalDate> datePredicate(String name) {
        String key = name.toLowerCase();
        Predicate<LocalDate> have = predicates.get(key);
        if (have != null) return have;
        if (busy.contains(key))   // cyclic reference: defer to the eventual map entry
            return d -> { var p = predicates.get(key); return p != null && p.test(d); };
        ResolvedDayKindDefinition def = null;
        for (var d : model.resolvedDayKindDefinitions) if (d.name().equalsIgnoreCase(name)) { def = d; break; }
        if (def == null) return null;
        busy.add(key);
        Predicate<LocalDate> body = bool(def.condition());
        busy.remove(key);
        if (body != null) predicates.put(key, body);
        return body;
    }

    /** Lower a §9.8 condition-over-the-day; null when any leaf is not a pure date predicate. */
    private Predicate<LocalDate> bool(Condition c) {
        if (c instanceof Condition.Comparison v) {
            // a type-mismatched comparison (datum vs numeric date-component) can never compare via
            // Comparable.compareTo -> null, so both back-ends marker it symmetrically.
            // a day-kind definition binds no Let, so its operands' types are their declarations' or their own
            Datatype lt = EffectiveUnit.typeWithoutBinder(v.left()), rt = EffectiveUnit.typeWithoutBinder(v.right());
            boolean compat = (lt.isNumber() && rt.isNumber()) || (lt.isDate() && rt.isDate());
            if (!compat) return null;
            Function<LocalDate, Object> lo = value(v.left()), ro = value(v.right());
            if (lo == null || ro == null) return null;
            Condition.Operator op = v.op();
            return d -> compare(lo.apply(d), ro.apply(d), op);
        }
        if (c instanceof Condition.Predicate p && p.kind() == Condition.PredicateKind.DAGSOORT) {
            // The §9.8 composition is carried ON THE BOUND DAY ONLY (uchar RUNTIME-SEMANTICS ch. 3.8, the narrow
            // route of 2026-09-19): a nested test on some OTHER date is a recognised, unexecuted form in `ref` and
            // in the ASP backend, so this backend does not answer it either — one executor computing what two
            // refuse is exactly the asymmetry the day-kind card set out to end. Uncompiled, so the form leaves
            // through this compiler's ordinary refusal channel and the transpiler names it.
            Expr od = p.operand();
            if (!(od == null || (od instanceof Expr.Constant oc && "de dag".equalsIgnoreCase(oc.name()))))
                return null;
            Function<LocalDate, Object> o = value(p.operand());
            Predicate<LocalDate> inner = datePredicate(p.dayKind());
            if (o == null || inner == null) return null;
            boolean neg = p.negation();   // §8.1.5 + Tabel 16 "(de dag) is geen <dagsoort>" inside a §9.8 definitie
            return d -> neg != (o.apply(d) instanceof LocalDate ld && inner.test(ld));
        }
        if (c instanceof Condition.Compound s) {
            List<Predicate<LocalDate>> parts = new ArrayList<>();
            for (Condition part : s.components()) { var d = bool(part); if (d == null) return null; parts.add(d); }
            if (parts.isEmpty()) return null;
            int n = s.threshold();
            return switch (s.quantifier()) {
                case ALLE, DE    -> d -> parts.stream().allMatch(q -> q.test(d));
                case GEEN        -> d -> parts.stream().noneMatch(q -> q.test(d));
                case TEN_MINSTE  -> d -> parts.stream().filter(q -> q.test(d)).count() >= n;
                case TEN_HOOGSTE -> d -> parts.stream().filter(q -> q.test(d)).count() <= n;
                case PRECIES     -> d -> parts.stream().filter(q -> q.test(d)).count() == n;
            };
        }
        return null;   // any other condition shape is not a pure date predicate
    }

    /** A §9.8 operand over "de dag" as a {@code Function<LocalDate,Object>} (the day itself, a date-component
     *  of it via rsDatumDeel, "de eerste paasdag van", or a fact-free literal); null when not a pure date operand. */
    private Function<LocalDate, Object> value(Expr e) {
        if (e instanceof Expr.Constant c && "de dag".equalsIgnoreCase(c.name())) return d -> d;
        if (e instanceof Expr.FunctionCall f && f.arguments().size() == 1) {
            java.time.temporal.ChronoField cf = switch (f.kind()) {
                case JAAR_UIT         -> java.time.temporal.ChronoField.YEAR;
                case MAAND_UIT        -> java.time.temporal.ChronoField.MONTH_OF_YEAR;
                case DAG_UIT          -> java.time.temporal.ChronoField.DAY_OF_MONTH;
                case UUR_UIT          -> java.time.temporal.ChronoField.HOUR_OF_DAY;
                case MINUUT_UIT       -> java.time.temporal.ChronoField.MINUTE_OF_HOUR;
                case SECONDE_UIT      -> java.time.temporal.ChronoField.SECOND_OF_MINUTE;
                case MILLISECONDE_UIT -> java.time.temporal.ChronoField.MILLI_OF_SECOND;
                default               -> null;
            };
            Function<LocalDate, Object> inner = value(f.arguments().get(0));
            if (inner == null) return null;
            if (cf != null) return d -> RegelSpraakRuntime.rsDatumDeel(inner.apply(d), cf);
            // §13.4.17.37 "de eerste paasdag van <jaar>" — the paasdag/feestdag building block.
            if (f.kind() == Expr.FunctionKind.EERSTE_PAASDAG) return d -> RegelSpraakRuntime.rsEerstePaasdag(inner.apply(d));
            return null;
        }
        if (e instanceof Expr.Literal lit) {
            if (lit.type().isDate()) { LocalDate ld = Literals.parseDate(lit.valueText()); return ld == null ? null : d -> ld; }
            BigDecimal n = Literals.number(lit); return n == null ? null : d -> n;
        }
        return null;
    }

    /** Compare two §9.8 operands (BigDecimal date-components or LocalDate values) by Comparable.compareTo. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static boolean compare(Object a, Object b, Condition.Operator op) {
        if (!(a instanceof Comparable) || b == null) return false;
        int c = ((Comparable) a).compareTo(b);
        return switch (op) {
            case GELIJK -> c == 0;  case ONGELIJK -> c != 0;
            case GROTER, LATER -> c > 0;  case KLEINER, EERDER -> c < 0;
            case GROTER_OF_GELIJK, LATER_OF_GELIJK -> c >= 0;  case KLEINER_OF_GELIJK, EERDER_OF_GELIJK -> c <= 0;
        };
    }
}
