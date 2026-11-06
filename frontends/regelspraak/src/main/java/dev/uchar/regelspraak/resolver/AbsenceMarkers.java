package dev.uchar.regelspraak.resolver;

import dev.uchar.regelspraak.ir.Condition;
import dev.uchar.regelspraak.ir.Default;
import dev.uchar.regelspraak.ir.ExceptionGroup;
import dev.uchar.regelspraak.ir.Expr;
import dev.uchar.regelspraak.ir.PrioritizedDefaults;
import dev.uchar.regelspraak.ir.ResolvedAllocation;
import dev.uchar.regelspraak.ir.ResolvedConsistency;
import dev.uchar.regelspraak.ir.ResolvedDayKindDefinition;
import dev.uchar.regelspraak.ir.ResolvedFactCreation;
import dev.uchar.regelspraak.ir.ResolvedObjectCreation;
import dev.uchar.regelspraak.ir.ResolvedRule;
import dev.uchar.regelspraak.model.Datatype;
import dev.uchar.regelspraak.model.RegelSpraakModel;
import dev.uchar.regelspraak.model.RegelSpraakModel.Diagnostic;
import dev.uchar.regelspraak.model.RegelSpraakModel.ObjectType;
import dev.uchar.regelspraak.model.RegelSpraakModel.Severity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static dev.uchar.regelspraak.model.RegelSpraakModel.Diagnostic.params;

/**
 * The attributes with absence markers lowered to their sum type (grammar/DIVERGENCES.md, "Afwezigheidsmarkeringen";
 * RUNTIME-SEMANTICS ch. 9.5) over the finished model, after every lowering of {@link RuleChecker}, and the reads of
 * their value judged. Where a rule is read, a marker written or tested becomes a Construct of the marker and "een
 * waarde heeft" a gevuld test of a PayloadAccess; this pass builds the rest. Every other read of the value — in
 * arithmetic, a comparison, an aggregation, a value written into another attribute — becomes a PayloadAccess of the
 * value constructor, typed with the declared datatype; every reference to such an attribute is typed with its sum type
 * ({@link ObjectType#sumTypeOf}); and every value a rule, a default or an object creation writes into it that is no
 * marker becomes a Construct of the value constructor, so a leeg value leaves the Construct leeg and decides nothing
 * (ch. 9.1). A reference reads no value as the target, as the operand of a leeg or gevuld test of the attribute
 * itself, and beside a Construct in a comparison. The checks of {@link RuleChecker} run before this pass and read a reference with the
 * declared datatype, so the typing of a computation reads the value as it reads every other attribute.
 *
 * <p>Then every PayloadAccess must stand after a test that the attribute holds a value — a conjunct of the condition
 * of its form before it, or of the filter of the fold it stands in; a read of a cell after a test of the same cell. One
 * without that test is refused with RGS-376, the frontend's half of WF-127: a marker would otherwise compute on as
 * leeg, which plus, min and maal count as 0.
 */
final class AbsenceMarkers {

    private final RegelSpraakModel model;
    /** The lowering of each node read as a value, and of each node read as a reference, by identity: a condition that two
     *  forms share (the conjuncts a subtype's block repeats from its base) stays one node, so its reads are judged and
     *  reported once. */
    private final Map<Object, Object> readLowered = new IdentityHashMap<>(), referenceLowered = new IdentityHashMap<>();
    private final Set<Object> reported = Collections.newSetFromMap(new IdentityHashMap<>());

    private AbsenceMarkers(RegelSpraakModel model) { this.model = model; }

    static void lower(RegelSpraakModel model) {
        if (model.objectTypes.values().stream().allMatch(t -> t.attributeMarkers.isEmpty())) return;
        AbsenceMarkers m = new AbsenceMarkers(model);
        m.lowerForms();
        m.judgeForms();
    }

    /** The read of the value of {@code base}, of the sum type {@code sum}: the payload of the value constructor. */
    static Expr.PayloadAccess valueOf(Expr base, Datatype.SumType sum) {
        return new Expr.PayloadAccess(base, RegelSpraakModel.VALUE_CONSTRUCTOR, sum.constructors().get(0).payload(),
                base.source());
    }

    // ---------------------------------------------------------------------------------------------------------------
    // The lowering. A node in which nothing changes is returned itself, so a form without markers stays the record the
    // rule check built.
    // ---------------------------------------------------------------------------------------------------------------

    private void lowerForms() {
        model.resolvedRules.replaceAll(this::rule);
        model.resolvedDayKindDefinitions.replaceAll(d -> {
            Condition c = cond(d.condition());
            return c == d.condition() ? d : new ResolvedDayKindDefinition(d.name(), c, d.validity());
        });
        model.objectCreations.replaceAll(this::objectCreation);
        model.allocations.replaceAll(v -> {
            Condition c = cond(v.condition());
            return c == v.condition() ? v : new ResolvedAllocation(v.name(), v.sourceType(), v.sourceAttribute(),
                    v.targetType(), v.targetAttribute(), v.method(), v.ratioAttribute(), v.maximumAttribute(),
                    v.roundingDecimals(), v.remainderAttribute(), v.orderingAttribute(), v.descending(), c, v.validity(),
                    v.connectingRole());
        });
        model.consistencies.replaceAll(k -> {
            Condition c = cond(k.assertion());
            return c == k.assertion() ? k : new ResolvedConsistency(k.name(), k.subject(), c, k.source(), k.line(), k.validity());
        });
        model.factCreations.replaceAll(f -> {
            Condition c = cond(f.condition());
            return c == f.condition() ? f : new ResolvedFactCreation(f.name(), f.anchor(), f.target(), f.setField(),
                    f.joinField(), f.pathSteps(), c, f.validity(), f.factType(), f.anchorRole(), f.targetRole(), f.path());
        });
        model.prioritizedDefaults.replaceAll(this::group);
    }

    private ResolvedRule rule(ResolvedRule r) {
        Expr target = reference(r.target());
        Expr value = written(targetSum(r.target()), read(r.value()));
        Condition condition = cond(r.condition());
        return target == r.target() && value == r.value() && condition == r.condition() ? r
                : new ResolvedRule(r.name(), r.kind(), target, value, condition, r.validity(), r.statuteAnnotations());
    }

    /** An object creation: a member it assigns a marker to is written as a rule writes it. */
    private ResolvedObjectCreation objectCreation(ResolvedObjectCreation oc) {
        List<ResolvedObjectCreation.MemberAssignment> assignments = new ArrayList<>();
        boolean same = true;
        for (var t : oc.assignments()) {
            Datatype.SumType sum = oc.createdType() == null || t.characteristic() ? null
                    : oc.createdType().sumTypeOf(t.attribute());
            Expr v = written(sum, read(t.value()));
            same &= v == t.value();
            assignments.add(v == t.value() ? t : new ResolvedObjectCreation.MemberAssignment(t.attribute(), v, t.characteristic(), t.labels()));
        }
        Condition c = cond(oc.condition());
        return same && c == oc.condition() ? oc : new ResolvedObjectCreation(oc.name(), oc.createdType(), oc.subject(),
                assignments, c, oc.validity(), oc.connectingRole());
    }

    /** A defaults group: the consequences of an attribute group write its target, the group's own and those of every
     *  exception below it. */
    private PrioritizedDefaults group(PrioritizedDefaults p) {
        ObjectType subject = model.objectTypes.get(p.subject());
        Datatype.SumType sum = subject == null || p.attribute() == null ? null : subject.sumTypeOf(p.attribute());
        List<Default> defaults = defaults(p.defaults(), sum);
        return defaults == p.defaults() ? p : new PrioritizedDefaults(p.name(), p.subject(), p.characteristic(),
                p.attribute(), p.unordered(), defaults, p.source(), p.validity());
    }

    private List<Default> defaults(List<Default> defaults, Datatype.SumType sum) {
        if (defaults == null) return null;
        List<Default> result = new ArrayList<>();
        boolean same = true;
        for (Default d : defaults) {
            Condition c = cond(d.condition());
            Expr consequence = written(sum, read(d.consequence()));
            Default.Exceptions exceptions = exceptions(d.exceptions(), sum);
            Default n = c == d.condition() && consequence == d.consequence() && exceptions == d.exceptions() ? d
                    : new Default(d.decision(), c, consequence, d.initialisation(), exceptions, d.source(), d.name(),
                            d.statuteAnnotations());
            same &= n == d;
            result.add(n);
        }
        return same ? defaults : result;
    }

    private Default.Exceptions exceptions(Default.Exceptions e, Datatype.SumType sum) {
        if (e == null) return null;
        List<ExceptionGroup> members = new ArrayList<>();
        boolean same = true;
        for (ExceptionGroup g : e.members()) {
            List<Default> d = defaults(g.defaults(), sum);
            same &= d == g.defaults();
            members.add(d == g.defaults() ? g : new ExceptionGroup(d, g.source()));
        }
        return same ? e : new Default.Exceptions(e.unordered(), members);
    }

    /** The sum type of the attribute a rule writes, its own or the one of the cell it writes; null for any other. */
    private static Datatype.SumType targetSum(Expr target) {
        Expr.AttributeReference a = target instanceof Expr.DimensionCellRead c ? c.attribute()
                : target instanceof Expr.AttributeReference r ? r : null;
        return a == null || a.objectType() == null ? null : a.objectType().sumTypeOf(a.attribute());
    }

    /** The value written into a target of the sum type {@code sum}: a marker stands as the Construct it is, every other
     *  value is the payload of a Construct of the value constructor. */
    private static Expr written(Datatype.SumType sum, Expr value) {
        if (sum == null || value == null || value instanceof Expr.Construct) return value;
        return new Expr.Construct(sum, RegelSpraakModel.VALUE_CONSTRUCTOR, value, value.source());
    }

    private static Datatype.SumType sumOf(Expr.AttributeReference a) {
        return a.objectType() == null ? null : a.objectType().sumTypeOf(a.attribute());
    }

    private static Expr.AttributeReference typed(Expr.AttributeReference a, Datatype.SumType sum) {
        return new Expr.AttributeReference(a.attribute(), a.objectType(), a.subjectSource(), a.navigationRole(), sum,
                a.source(), a.set(), a.navigationPath(), a.rangePath(), a.quantifier(), a.threshold(), a.anchored());
    }

    /** Whether {@code e} is a reference to an attribute with markers, or to a cell of one. */
    private static boolean marked(Expr e) {
        Expr.AttributeReference a = e instanceof Expr.DimensionCellRead c ? c.attribute()
                : e instanceof Expr.AttributeReference r ? r : null;
        return a != null && sumOf(a) != null;
    }

    /** {@code e} where it is read as a reference: typed with its sum type, no read of its value. */
    private Expr reference(Expr e) {
        if (e == null) return null;
        Object done = referenceLowered.get(e);
        if (done != null) return (Expr) done;
        Expr n = e;
        if (e instanceof Expr.AttributeReference a && sumOf(a) != null) n = typed(a, sumOf(a));
        else if (e instanceof Expr.DimensionCellRead c && sumOf(c.attribute()) != null)
            n = new Expr.DimensionCellRead(typed(c.attribute(), sumOf(c.attribute())), c.labels(), c.source());
        referenceLowered.put(e, n);
        return n;
    }

    /** {@code e} where it is read as a value. */
    private Expr read(Expr e) {
        if (e == null) return null;
        Object done = readLowered.get(e);
        if (done != null) return (Expr) done;
        Expr n = switch (e) {
            case Expr.AttributeReference a -> {
                Datatype.SumType s = sumOf(a);
                yield s == null ? a : valueOf(reference(a), s);
            }
            case Expr.DimensionCellRead c -> {
                Datatype.SumType s = sumOf(c.attribute());
                yield s == null ? c : valueOf(reference(c), s);
            }
            case Expr.PayloadAccess p -> {
                Expr b = reference(p.base());
                yield b == p.base() ? p : new Expr.PayloadAccess(b, p.constructor(), p.type(), p.source());
            }
            case Expr.Construct c -> {
                Expr a = read(c.argument());
                yield a == c.argument() ? c : new Expr.Construct(c.type(), c.constructor(), a, c.source());
            }
            case Expr.Binary b -> {
                Expr l = read(b.left()), r = read(b.right());
                yield l == b.left() && r == b.right() ? b : new Expr.Binary(b.op(), l, r, b.type(), b.source());
            }
            case Expr.FunctionCall f -> {
                List<Expr> args = new ArrayList<>();
                boolean same = true;
                for (Expr x : f.arguments()) {
                    Expr y = read(x);
                    same &= y == x;
                    args.add(y);
                }
                Expr empty = read(f.emptyFallback());
                Condition filter = cond(f.filter());
                yield same && empty == f.emptyFallback() && filter == f.filter() ? f
                        : new Expr.FunctionCall(f.kind(), args, f.elementObjectType(), empty, filter, f.dimensionSelections(),
                                f.type(), f.source(), f.granularity(), f.anchored());
            }
            case Expr.Rounding a -> {
                Expr b = read(a.base());
                yield b == a.base() ? a : new Expr.Rounding(b, a.mode(), a.decimals(), a.source());
            }
            case Expr.Clamp c -> {
                Expr b = read(c.base()), min = read(c.minimum()), max = read(c.maximum());
                yield b == c.base() && min == c.minimum() && max == c.maximum() ? c : new Expr.Clamp(b, min, max, c.source());
            }
            case Expr.TextComposition t -> {
                List<Expr.TextComposition.Part> parts = new ArrayList<>();
                boolean same = true;
                for (Expr.TextComposition.Part d : t.parts()) {
                    Expr.TextComposition.Part n2 = d;
                    if (d instanceof Expr.TextComposition.EmbeddedExpression i) {
                        Expr x = read(i.expr());
                        if (x != i.expr()) n2 = new Expr.TextComposition.EmbeddedExpression(x, i.unit());
                    }
                    same &= n2 == d;
                    parts.add(n2);
                }
                yield same ? t : new Expr.TextComposition(parts, t.source());
            }
            case Expr.Let l -> {
                Expr v = read(l.value()), b = read(l.body());
                yield v == l.value() && b == l.body() ? l : new Expr.Let(l.binder(), v, b, l.type(), l.source());
            }
            case Expr.IfThenElse k -> {
                Condition c = cond(k.condition());
                Expr then = read(k.then()), otherwise = read(k.otherwise());
                yield c == k.condition() && then == k.then() && otherwise == k.otherwise() ? k
                        : new Expr.IfThenElse(c, then, otherwise, k.type(), k.source());
            }
            case Expr.InstanceSetUnion u -> {
                List<Expr.InstanceSetUnion.Branch> branches = new ArrayList<>();
                boolean same = true;
                for (Expr.InstanceSetUnion.Branch b : u.branches()) {
                    Condition c = cond(b.filter());
                    same &= c == b.filter();
                    branches.add(c == b.filter() ? b : new Expr.InstanceSetUnion.Branch(b.elementType(), b.sourceRole(), c, b.source()));
                }
                yield same ? u : new Expr.InstanceSetUnion(branches, u.source());
            }
            case Expr.Literal ignored -> e;
            case Expr.ParameterRef ignored -> e;
            case Expr.VariableRef ignored -> e;
            case Expr.BinderRef ignored -> e;
            case Expr.Constant ignored -> e;
            case Expr.Unresolved ignored -> e;
        };
        readLowered.put(e, n);
        return n;
    }

    private Condition cond(Condition c) {
        if (c == null) return null;
        Object done = readLowered.get(c);
        if (done != null) return (Condition) done;
        Condition n = switch (c) {
            case Condition.Predicate p -> {
                // a leeg or gevuld test of the attribute itself reads no value: a marker is a filled value
                Expr o = (p.kind() == Condition.PredicateKind.LEEG || p.kind() == Condition.PredicateKind.GEVULD)
                        && marked(p.operand()) ? reference(p.operand()) : read(p.operand());
                yield o == p.operand() ? p
                        : new Condition.Predicate(p.kind(), o, p.dayKind(), p.negation(), p.digits(), p.period(), p.source());
            }
            case Condition.Comparison k -> {
                // beside a marker the attribute is compared as a sum-typed value (ch. 9.3)
                boolean beside = k.left() instanceof Expr.Construct || k.right() instanceof Expr.Construct;
                Expr l = beside && marked(k.left()) ? reference(k.left()) : read(k.left());
                Expr r = beside && marked(k.right()) ? reference(k.right()) : read(k.right());
                yield l == k.left() && r == k.right() ? k : new Condition.Comparison(k.op(), l, r, k.period(), k.source());
            }
            case Condition.Compound k -> {
                List<Condition> parts = new ArrayList<>();
                boolean same = true;
                for (Condition part : k.components()) {
                    Condition q = cond(part);
                    same &= q == part;
                    parts.add(q);
                }
                yield same ? k : new Condition.Compound(k.quantifier(), k.threshold(), parts, k.source());
            }
            case Condition.QuantifierFold q -> {
                Condition filter = cond(q.filter()), test = cond(q.test());
                yield filter == q.filter() && test == q.test() ? q
                        : new Condition.QuantifierFold(q.quantifier(), q.threshold(), q.elementObjectType(), filter, test,
                                q.sourceRole(), q.sourcePath(), q.source());
            }
            case Condition.TimeWindow w -> {
                Expr from = read(w.from()), until = read(w.until());
                yield from == w.from() && until == w.until() ? w : new Condition.TimeWindow(from, until, w.toInclusive(), w.source());
            }
            case Condition.CharacteristicCheck ignored -> c;
            case Condition.RoleMembership ignored -> c;
            case Condition.FarRoleMembership ignored -> c;
            case Condition.RuleStatusCheck ignored -> c;
            case Condition.IdentityCheck ignored -> c;
            case Condition.Unresolved ignored -> c;
        };
        readLowered.put(c, n);
        return n;
    }

    // ---------------------------------------------------------------------------------------------------------------
    // The judgement (RGS-376): a PayloadAccess stands after a test of its value.
    // ---------------------------------------------------------------------------------------------------------------

    private void judgeForms() {
        for (var r : model.resolvedRules) form(r.condition(), r.value() == null ? List.of() : List.of(r.value()));
        for (var d : model.resolvedDayKindDefinitions) form(d.condition(), List.of());
        for (var oc : model.objectCreations) {
            List<Expr> values = new ArrayList<>();
            for (var t : oc.assignments()) if (t.value() != null) values.add(t.value());
            form(oc.condition(), values);
        }
        for (var v : model.allocations) form(v.condition(), List.of());
        for (var c : model.consistencies) form(c.assertion(), List.of());
        for (var fc : model.factCreations) form(fc.condition(), List.of());
        for (var pd : model.prioritizedDefaults) judgeDefaults(pd.defaults());
    }

    private void judgeDefaults(List<Default> defaults) {
        if (defaults == null) return;
        for (Default d : defaults) {
            form(d.condition(), d.consequence() == null ? List.of() : List.of(d.consequence()));
            if (d.exceptions() != null)
                for (var group : d.exceptions().members()) judgeDefaults(group.defaults());
        }
    }

    /** A form: its condition read conjunct by conjunct, and its values after the whole condition. */
    private void form(Condition condition, List<Expr> values) {
        List<Key> tests = new ArrayList<>();
        if (condition != null) judge(condition, List.of());
        conjunctTests(condition, tests);
        for (Expr value : values) judge(value, tests);
    }

    /** The identity of a read: the attribute on its declaring type and the walk to it, and for a cell its labels, the
     *  source aside. */
    private record Key(ObjectType declaring, String attribute, String navigationRole, List<String> navigationPath,
                       boolean anchored, List<String> labels) {}

    /** The key of the value {@code p} reads, of an attribute or of a cell of one; null for a value no test can guard. */
    private static Key key(Expr.PayloadAccess p) {
        if (!RegelSpraakModel.VALUE_CONSTRUCTOR.equals(p.constructor())) return null;
        List<String> labels = null;
        Expr base = p.base();
        if (base instanceof Expr.DimensionCellRead c) {
            labels = c.labels().stream().map(l -> l == null ? null : l.toLowerCase(Locale.ROOT)).toList();
            base = c.attribute();
        }
        if (!(base instanceof Expr.AttributeReference a)) return null;
        return new Key(a.objectType().markerDeclaringType(a.attribute()), a.attribute().toLowerCase(Locale.ROOT),
                a.navigationRole(), a.navigationPath(), a.anchored(), labels);
    }

    /** The value tests among the conjuncts of {@code c}: a test is a guard only where it holds whenever {@code c}
     *  does. */
    private static void conjunctTests(Condition c, List<Key> into) {
        if (c instanceof Condition.Compound k && k.quantifier() == Condition.Quantifier.ALLE) {
            for (Condition part : k.components()) conjunctTests(part, into);
        } else if (c instanceof Condition.Predicate p && p.kind() == Condition.PredicateKind.GEVULD && !p.negation()
                && p.operand() instanceof Expr.PayloadAccess v && key(v) != null) {
            into.add(key(v));
        }
    }

    private void report(Expr.PayloadAccess p) {
        if (!reported.add(p)) return;
        Expr.AttributeReference a = p.base() instanceof Expr.DimensionCellRead c ? c.attribute() : (Expr.AttributeReference) p.base();
        model.diagnostics.add(new Diagnostic(Severity.ERROR, "RGS-376",
                params("attribute", a.attribute(), "objectType", a.objectType().markerDeclaringType(a.attribute()).name,
                        "expression", Objects.toString(p.source(), a.attribute())),
                p.source() == null ? 0 : p.source().line()));
    }

    private void judge(Expr e, List<Key> tests) {
        if (e == null) return;
        switch (e) {
            case Expr.PayloadAccess p -> {
                Key k = key(p);
                if (k == null || !tests.contains(k)) report(p);
            }
            case Expr.Binary b -> { judge(b.left(), tests); judge(b.right(), tests); }
            case Expr.FunctionCall f -> {
                // a fold over members reads each contributor under its own filter: the tests of the form do not hold
                // for the element, the conjuncts of the filter do
                boolean perMember = f.elementObjectType() != null;
                List<Key> inner = new ArrayList<>();
                if (f.filter() != null) {
                    judge(f.filter(), perMember ? List.of() : tests);
                    if (perMember) conjunctTests(f.filter(), inner);
                }
                for (Expr x : f.arguments()) judge(x, perMember ? inner : tests);
                judge(f.emptyFallback(), tests);
            }
            case Expr.Rounding a -> judge(a.base(), tests);
            case Expr.Clamp c -> { judge(c.base(), tests); judge(c.minimum(), tests); judge(c.maximum(), tests); }
            case Expr.TextComposition t -> t.parts().forEach(d -> {
                if (d instanceof Expr.TextComposition.EmbeddedExpression i) judge(i.expr(), tests);
            });
            case Expr.Let l -> { judge(l.value(), tests); judge(l.body(), tests); }
            case Expr.IfThenElse k -> {
                judge(k.condition(), tests);
                List<Key> then = new ArrayList<>(tests);
                conjunctTests(k.condition(), then);
                judge(k.then(), then);
                judge(k.otherwise(), tests);
            }
            case Expr.InstanceSetUnion u -> u.branches().forEach(b -> judge(b.filter(), List.of()));
            case Expr.Construct c -> judge(c.argument(), tests);
            // after the lowering a reference reads no value: every read of a value is a PayloadAccess
            case Expr.AttributeReference ignored -> { }
            case Expr.DimensionCellRead ignored -> { }
            case Expr.Literal ignored -> { }
            case Expr.ParameterRef ignored -> { }
            case Expr.VariableRef ignored -> { }
            case Expr.BinderRef ignored -> { }
            case Expr.Constant ignored -> { }
            case Expr.Unresolved ignored -> { }
        }
    }

    /** A condition read with the tests that hold before it; the members of an `alle` are read in order, and a member
     *  stops the rest where it fails (RUNTIME-SEMANTICS ch. 5.10), so a member is tested by the members before it. */
    private void judge(Condition c, List<Key> tests) {
        if (c == null) return;
        switch (c) {
            case Condition.Predicate p -> {
                // a leeg or gevuld test of a PayloadAccess is its own guard
                if (!((p.kind() == Condition.PredicateKind.LEEG || p.kind() == Condition.PredicateKind.GEVULD)
                        && p.operand() instanceof Expr.PayloadAccess)) judge(p.operand(), tests);
            }
            case Condition.Comparison k -> { judge(k.left(), tests); judge(k.right(), tests); }
            case Condition.Compound k -> {
                List<Key> seen = new ArrayList<>(tests);
                for (Condition part : k.components()) {
                    judge(part, k.quantifier() == Condition.Quantifier.ALLE ? seen : tests);
                    if (k.quantifier() == Condition.Quantifier.ALLE) conjunctTests(part, seen);
                }
            }
            case Condition.QuantifierFold q -> {
                judge(q.filter(), List.of());
                List<Key> inner = new ArrayList<>();
                conjunctTests(q.filter(), inner);
                judge(q.test(), inner);
            }
            case Condition.TimeWindow w -> { judge(w.from(), tests); judge(w.until(), tests); }
            case Condition.CharacteristicCheck ignored -> { }
            case Condition.RoleMembership ignored -> { }
            case Condition.FarRoleMembership ignored -> { }
            case Condition.RuleStatusCheck ignored -> { }
            case Condition.IdentityCheck ignored -> { }
            case Condition.Unresolved ignored -> { }
        }
    }
}
