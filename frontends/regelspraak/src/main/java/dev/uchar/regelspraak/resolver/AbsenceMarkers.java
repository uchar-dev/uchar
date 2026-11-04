package dev.uchar.regelspraak.resolver;

import dev.uchar.regelspraak.ir.Condition;
import dev.uchar.regelspraak.ir.Default;
import dev.uchar.regelspraak.ir.Expr;
import dev.uchar.regelspraak.model.RegelSpraakModel;
import dev.uchar.regelspraak.model.RegelSpraakModel.AbsenceMarkerSite;
import dev.uchar.regelspraak.model.RegelSpraakModel.Diagnostic;
import dev.uchar.regelspraak.model.RegelSpraakModel.ObjectType;
import dev.uchar.regelspraak.model.RegelSpraakModel.Severity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

import static dev.uchar.regelspraak.model.RegelSpraakModel.Diagnostic.params;

/**
 * The reads of an attribute with absence markers (grammar/DIVERGENCES.md, "Afwezigheidsmarkeringen"; RUNTIME-SEMANTICS
 * ch. 9.5), judged over the finished model, after every lowering of {@link RuleChecker}. A read of such an attribute is
 * one of three: the operand of a leeg or gevuld test; a side of a comparison with a marker; or a read of its value,
 * which the emitter writes as a PayloadAccess and which stands after a test that the attribute holds a value — a
 * conjunct of the condition of its form before it, or of the filter of the fold it stands in. A read of the value
 * without such a test is refused with RGS-376, the frontend's half of WF-127: a marker would otherwise compute on as
 * leeg, which plus, min and maal count as 0. The reads of the value are recorded in
 * {@link RegelSpraakModel#absenceMarkerSites}.
 */
final class AbsenceMarkers {

    private final RegelSpraakModel model;
    private final Set<Object> reported = Collections.newSetFromMap(new IdentityHashMap<>());

    private AbsenceMarkers(RegelSpraakModel model) { this.model = model; }

    static void check(RegelSpraakModel model) {
        if (model.objectTypes.values().stream().allMatch(t -> t.attributeMarkers.isEmpty())) return;
        AbsenceMarkers m = new AbsenceMarkers(model);
        for (var r : model.resolvedRules) m.form(r.condition(), r.value() == null ? List.of() : List.of(r.value()));
        for (var d : model.resolvedDayKindDefinitions) m.form(d.condition(), List.of());
        for (var oc : model.objectCreations) {
            List<Expr> values = new ArrayList<>();
            for (var t : oc.initialisations()) if (t.value() != null) values.add(t.value());
            m.form(oc.condition(), values);
        }
        for (var v : model.allocations) m.form(v.condition(), List.of());
        for (var c : model.consistencies) m.form(c.assertion(), List.of());
        for (var fc : model.factCreations) m.form(fc.condition(), List.of());
        for (var pd : model.prioritizedDefaults) m.defaults(pd.defaults());
    }

    private void defaults(List<Default> defaults) {
        if (defaults == null) return;
        for (Default d : defaults) {
            form(d.condition(), d.consequence() == null ? List.of() : List.of(d.consequence()));
            if (d.exceptions() != null)
                for (var group : d.exceptions().members()) defaults(group.defaults());
        }
    }

    /** A form: its condition read conjunct by conjunct, and its values after the whole condition. */
    private void form(Condition condition, List<Expr> values) {
        List<Key> tests = new ArrayList<>();
        if (condition != null) cond(condition, List.of());
        conjunctTests(condition, tests);
        for (Expr value : values) expr(value, tests);
    }

    /** The identity of a read: the attribute on its declaring type and the walk to it, the source aside. */
    private record Key(ObjectType declaring, String attribute, String navigationRole, List<String> navigationPath,
                       boolean anchored) {}

    private static Key key(Expr.AttributeReference a) {
        ObjectType declaring = a.objectType().markerDeclaringType(a.attribute());
        return new Key(declaring, a.attribute().toLowerCase(Locale.ROOT), a.navigationRole(), a.navigationPath(),
                a.anchored());
    }

    private boolean marked(Expr e) {
        return e instanceof Expr.AttributeReference a && a.objectType() != null
                && a.objectType().markersOf(a.attribute()) != null;
    }

    /** The value tests among the conjuncts of {@code c}: a test is a guard only where it holds whenever {@code c}
     *  does. */
    private void conjunctTests(Condition c, List<Key> into) {
        if (c instanceof Condition.Compound k && k.quantifier() == Condition.Quantifier.ALLE) {
            for (Condition part : k.components()) conjunctTests(part, into);
        } else if (c instanceof Condition.Predicate p && valueTest(p)) {
            into.add(key((Expr.AttributeReference) p.operand()));
        }
    }

    private boolean valueTest(Condition.Predicate p) {
        AbsenceMarkerSite site = model.absenceMarkerSites.get(p);
        return site != null && RegelSpraakModel.VALUE_CONSTRUCTOR.equals(site.constructor())
                && p.operand() instanceof Expr.AttributeReference;
    }

    private void read(Expr.AttributeReference a, List<Key> tests) {
        if (tests.contains(key(a))) {
            record(a);
            return;
        }
        if (!reported.add(a)) return;
        model.diagnostics.add(new Diagnostic(Severity.ERROR, "RGS-376",
                params("attribute", a.attribute(), "objectType", a.objectType().markerDeclaringType(a.attribute()).name,
                        "expression", Objects.toString(a.source(), a.attribute())),
                a.source() == null ? 0 : a.source().line()));
    }

    private void record(Expr.AttributeReference a) {
        ObjectType declaring = a.objectType().markerDeclaringType(a.attribute());
        model.absenceMarkerSites.put(a, new AbsenceMarkerSite(declaring, declaring.attributeName(a.attribute()),
                RegelSpraakModel.VALUE_CONSTRUCTOR));
    }

    private void expr(Expr e, List<Key> tests) {
        if (e == null) return;
        switch (e) {
            case Expr.AttributeReference a -> { if (marked(a)) read(a, tests); }
            case Expr.Binary b -> { expr(b.left(), tests); expr(b.right(), tests); }
            case Expr.FunctionCall f -> {
                // a fold over members reads each contributor under its own filter: the tests of the form do not hold
                // for the element, the conjuncts of the filter do
                boolean perMember = f.elementObjectType() != null;
                List<Key> inner = new ArrayList<>();
                if (f.filter() != null) {
                    cond(f.filter(), perMember ? List.of() : tests);
                    if (perMember) conjunctTests(f.filter(), inner);
                }
                for (Expr x : f.arguments()) expr(x, perMember ? inner : tests);
                expr(f.emptyFallback(), tests);
            }
            case Expr.Rounding a -> expr(a.base(), tests);
            case Expr.Clamp c -> { expr(c.base(), tests); expr(c.minimum(), tests); expr(c.maximum(), tests); }
            case Expr.TextComposition t -> t.parts().forEach(d -> {
                if (d instanceof Expr.TextComposition.EmbeddedExpression i) expr(i.expr(), tests);
            });
            case Expr.DimensionCellRead c -> expr(c.attribute(), tests);
            case Expr.Let l -> { expr(l.value(), tests); expr(l.body(), tests); }
            case Expr.IfThenElse k -> {
                cond(k.condition(), tests);
                List<Key> then = new ArrayList<>(tests);
                conjunctTests(k.condition(), then);
                expr(k.then(), then);
                expr(k.otherwise(), tests);
            }
            case Expr.InstanceSetUnion u -> u.branches().forEach(b -> cond(b.filter(), List.of()));
            case Expr.Literal ignored -> { }
            case Expr.ParameterRef ignored -> { }
            case Expr.VariableRef ignored -> { }
            case Expr.BinderRef ignored -> { }
            case Expr.Constant ignored -> { }
            case Expr.Unresolved ignored -> { }
            // the resolver builds neither: the emitter writes them from the sites this pass records
            case Expr.Construct c -> expr(c.argument(), tests);
            case Expr.PayloadAccess p -> expr(p.base(), tests);
        }
    }

    /** A condition read with the tests that hold before it; the members of an `alle` are read in order, and a member
     *  stops the rest where it fails (RUNTIME-SEMANTICS ch. 5.10), so a member is tested by the members before it. */
    private void cond(Condition c, List<Key> tests) {
        if (c == null) return;
        switch (c) {
            case Condition.Predicate p -> {
                if ((p.kind() == Condition.PredicateKind.LEEG || p.kind() == Condition.PredicateKind.GEVULD)
                        && marked(p.operand())) {
                    // a test of the attribute itself: a value test reads the value, which is its own guard
                    if (valueTest(p)) record((Expr.AttributeReference) p.operand());
                } else expr(p.operand(), tests);
            }
            case Condition.Comparison k -> {
                boolean leftMarker = isMarker(k.left()), rightMarker = isMarker(k.right());
                if (leftMarker && marked(k.right()) || rightMarker && marked(k.left())) return;
                expr(k.left(), tests);
                expr(k.right(), tests);
            }
            case Condition.Compound k -> {
                List<Key> seen = new ArrayList<>(tests);
                for (Condition part : k.components()) {
                    cond(part, k.quantifier() == Condition.Quantifier.ALLE ? seen : tests);
                    if (k.quantifier() == Condition.Quantifier.ALLE) conjunctTests(part, seen);
                }
            }
            case Condition.QuantifierFold q -> {
                cond(q.filter(), List.of());
                List<Key> inner = new ArrayList<>();
                conjunctTests(q.filter(), inner);
                cond(q.test(), inner);
            }
            case Condition.TimeWindow w -> { expr(w.from(), tests); expr(w.until(), tests); }
            case Condition.CharacteristicCheck ignored -> { }
            case Condition.RoleMembership ignored -> { }
            case Condition.FarRoleMembership ignored -> { }
            case Condition.RuleStatusCheck ignored -> { }
            case Condition.IdentityCheck ignored -> { }
            case Condition.Unresolved ignored -> { }
        }
    }

    private boolean isMarker(Expr e) {
        AbsenceMarkerSite site = model.absenceMarkerSites.get(e);
        return e instanceof Expr.Literal && site != null && !RegelSpraakModel.VALUE_CONSTRUCTOR.equals(site.constructor());
    }
}
