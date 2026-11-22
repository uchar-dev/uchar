package dev.uchar.regelspraak.cases;

import dev.uchar.regelspraak.ir.Condition;
import dev.uchar.regelspraak.ir.Default;
import dev.uchar.regelspraak.ir.ExceptionGroup;
import dev.uchar.regelspraak.ir.Expr;
import dev.uchar.regelspraak.ir.PrioritizedDefaults;
import dev.uchar.regelspraak.ir.ResolvedConsistency;
import dev.uchar.regelspraak.ir.ResolvedFactCreation;
import dev.uchar.regelspraak.ir.ResolvedObjectCreation;
import dev.uchar.regelspraak.ir.ResolvedRule;
import dev.uchar.regelspraak.ir.ResolvedRuleGroup;
import dev.uchar.regelspraak.model.RegelSpraakModel;
import dev.uchar.regelspraak.transpiler.core.Namen;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The forms for which this backend does not carry the reading of a document with {@code inferFromEmpty: false}
 * (RUNTIME-SEMANTICS ch. 1.5, "Forms without the reading"), the one place that lists them: the build refuses such a
 * document before it builds anything, naming the first form of {@link #SLUGS} the document holds
 * ({@code DroolsBackend.build}, {@code DROOLS-370}). The list opens with the table of ch. 1.5, in its order, the forms
 * whose reading is not defined; then come the forms whose reading ch. 1.5 defines and this backend does not carry
 * yet, under slugs of its own; and last {@code reading}, the build with readings ({@code DroolsBackend.build(…,
 * readings)}), which reads no such document yet. A document whose forms are all outside the list is built and played
 * with the reading of ch. 1.5.
 *
 * <p>The loader refuses some forms of the table in every document, before this list is read: a function definition,
 * a variable reference, a temporal read, a match, a record, the list family and an invoke ({@code IrLoader}). Their
 * slugs stand in the list all the same, so that it is the table of ch. 1.5.
 */
final class UncarriedForms {
    private UncarriedForms() {}

    /** The slugs, in the order of which the first a document holds is named. */
    static final List<String> SLUGS = List.of(
            // the table of RUNTIME-SEMANTICS ch. 1.5, in its order
            "allocation", "creation-cycle", "uniqueness", "start-point", "day-kind", "function-def", "timeline-member",
            "dimension-cell", "dimension-fold", "time-proportional-part", "temporal-total", "builtin-filter",
            "variable-reference", "unresolved", "temporal-read", "match", "record", "list-family", "invoke",
            "whole-period", "time-window", "rule-status-check", "let-in-composite",
            // what ch. 1.5 defines and this backend does not carry yet
            "defaults-group", "quantifier-fold", "set-fold", "if-then-else", "object-creation", "fact-creation",
            // the build with readings
            "reading");

    /** The number of slugs of the table of ch. 1.5 at the head of {@link #SLUGS}. */
    static final int TABLE = 23;

    /** The first slug of {@link #SLUGS} whose form {@code model} holds, with its defaults groups {@code defaults}, or
     *  {@code reading} where it holds none and {@code readings} asks a build with readings; null where it holds none
     *  and is built without readings. */
    static String first(RegelSpraakModel model, List<PrioritizedDefaults> defaults, boolean readings) {
        Set<String> found = new HashSet<>();
        Walk w = new Walk(found, Map.of());
        if (!model.allocations.isEmpty()) found.add("allocation");
        Set<String> recursive = new HashSet<>();
        for (ResolvedRuleGroup g : model.ruleGroups)
            if (g.recursive()) for (String m : g.members()) recursive.add(formOf(m));
        for (ResolvedObjectCreation o : model.objectCreations)
            if (o.name() != null && recursive.contains(formOf(o.name()))) found.add("creation-cycle");
        for (ResolvedFactCreation f : model.factCreations)
            if (f.name() != null && recursive.contains(formOf(f.name()))) found.add("creation-cycle");
        if (!model.uniquenesses.isEmpty() || !model.uniquenessUnions.isEmpty()) found.add("uniqueness");
        if (!model.resolvedStartPointDeterminations.isEmpty()) found.add("start-point");
        if (!model.dayKinds.isEmpty() || !model.resolvedDayKindDefinitions.isEmpty()) found.add("day-kind");
        // a member with a timeline has no state per day in this reading
        for (RegelSpraakModel.ObjectType t : model.objectTypes.values())
            if (t.attributeGranularity.values().stream().anyMatch(g -> g != null)
                    || t.characteristicGranularity.values().stream().anyMatch(g -> g != null))
                found.add("timeline-member");
        for (RegelSpraakModel.Parameter p : model.parameters.values())
            if (p.granularity() != null) found.add("timeline-member");
        for (ResolvedRule r : model.resolvedRules) {
            if (r.target() instanceof Expr.DimensionCellRead) found.add("dimension-cell");
            w.expr(r.value());
            w.condition(r.condition());
        }
        for (ResolvedConsistency c : model.consistencies) w.condition(c.assertion());
        if (!defaults.isEmpty()) found.add("defaults-group");
        for (PrioritizedDefaults p : defaults) for (Default d : p.defaults()) w.defaultOf(d);
        if (!model.objectCreations.isEmpty()) found.add("object-creation");
        for (ResolvedObjectCreation o : model.objectCreations) {
            w.condition(o.condition());
            for (ResolvedObjectCreation.MemberAssignment a : o.assignments()) w.expr(a.value());
        }
        if (!model.factCreations.isEmpty()) found.add("fact-creation");
        for (ResolvedFactCreation f : model.factCreations) w.condition(f.condition());
        if (readings) found.add("reading");
        for (String slug : SLUGS) if (found.contains(slug)) return slug;
        return null;
    }

    /** A form's name without its version, as a recursive group names its members. */
    private static String formOf(String name) {
        return Namen.norm(RegelSpraakModel.formName(name));
    }

    /** The walk over the conditions and expressions of a document that notes the slugs of the forms it meets.
     *  {@code lets} holds the binders of the Lets around, each with whether the place is, within the body of its Let, in
     *  a member of a composite other than its first or in the filter or the matrix of a quantifier fold: a read there is
     *  {@code let-in-composite}. */
    private record Walk(Set<String> found, Map<String, Boolean> lets) {

        /** The walk in the body of a Let, whose binder takes the place of an outer one of the same name. */
        private Walk binding(String binder) {
            Map<String, Boolean> inner = new HashMap<>(lets);
            inner.put(Namen.norm(binder), false);
            return new Walk(found, inner);
        }

        /** The walk in a later member of a composite, or in the filter or the matrix of a quantifier fold. */
        private Walk past() {
            Map<String, Boolean> inner = new HashMap<>(lets);
            inner.replaceAll((binder, late) -> true);
            return new Walk(found, inner);
        }

        void defaultOf(Default d) {
            condition(d.condition());
            expr(d.consequence());
            if (d.exceptions() != null)
                for (ExceptionGroup g : d.exceptions().members()) for (Default e : g.defaults()) defaultOf(e);
        }

        void expr(Expr e) {
            switch (e) {
                case null -> { }
                case Expr.Literal x -> { }
                case Expr.ParameterRef x -> { }
                case Expr.BinderRef x -> {
                    if (lets.getOrDefault(Namen.norm(x.name()), false)) found.add("let-in-composite");
                }
                case Expr.AttributeReference x -> { }
                // the bound day of a day-kind definition; any other constant is a value of the session
                case Expr.Constant c -> {
                    if (c.name().equals("de dag")) found.add("day-kind");
                }
                case Expr.TextComposition t -> {
                    for (Expr.TextComposition.Part p : t.parts())
                        if (p instanceof Expr.TextComposition.EmbeddedExpression x) expr(x.expr());
                }
                case Expr.Binary b -> {
                    expr(b.left());
                    expr(b.right());
                }
                case Expr.Rounding r -> expr(r.base());
                case Expr.Clamp c -> {
                    expr(c.base());
                    expr(c.minimum());
                    expr(c.maximum());
                }
                case Expr.Construct c -> expr(c.argument());
                case Expr.PayloadAccess p -> expr(p.base());
                case Expr.Let l -> {
                    expr(l.value());
                    binding(l.binder()).expr(l.body());
                }
                case Expr.FunctionCall f -> function(f);
                case Expr.InstanceSetUnion u -> {
                    for (Expr.InstanceSetUnion.Branch b : u.branches()) condition(b.filter());
                }
                case Expr.DimensionCellRead c -> found.add("dimension-cell");
                case Expr.VariableRef v -> found.add("variable-reference");
                case Expr.Unresolved u -> found.add("unresolved");
                case Expr.IfThenElse k -> {
                    found.add("if-then-else");
                    condition(k.condition());
                    expr(k.then());
                    expr(k.otherwise());
                }
            }
        }

        private void function(Expr.FunctionCall f) {
            boolean ranged = f.elementObjectType() != null || f.sourceRole() != null || !f.sourcePath().isEmpty();
            boolean fold = switch (f.kind()) {
                case SOM, TOTAAL, AANTAL, AANTAL_GEVULDE, MINIMUM, MAXIMUM, EERSTE, LAATSTE -> true;
                default -> false;
            };
            boolean union = f.kind() == Expr.FunctionKind.AANTAL && f.filter() == null && f.arguments().size() == 1
                    && f.arguments().get(0) instanceof Expr.InstanceSetUnion;
            if (f.dimensionSelections() != null) found.add("dimension-fold");
            else if (f.kind() == Expr.FunctionKind.TIJDSEVENREDIG_DEEL) found.add("time-proportional-part");
            else if (f.kind() == Expr.FunctionKind.AANTAL_DAGEN_IN) found.add("day-kind");
            else if (fold && ranged || union) found.add("set-fold");
            else if (switch (f.kind()) {
                case SOM, TOTAAL, AANTAL, AANTAL_GEVULDE -> true;
                default -> false;
            }) found.add("temporal-total");
            else if (f.filter() != null || f.granularity() != null) found.add("builtin-filter");
            condition(f.filter());
            for (Expr a : f.arguments()) expr(a);
            expr(f.emptyFallback());
        }

        void condition(Condition c) {
            switch (c) {
                case null -> { }
                case Condition.Comparison v -> {
                    if (v.period() != null) found.add("whole-period");
                    expr(v.left());
                    expr(v.right());
                }
                case Condition.Predicate p -> {
                    if (p.period() != null) found.add("whole-period");
                    if (p.kind() == Condition.PredicateKind.DAGSOORT) found.add("day-kind");
                    expr(p.operand());
                }
                case Condition.CharacteristicCheck k -> {
                    if (k.period() != null) found.add("whole-period");
                }
                case Condition.RoleMembership r -> { }
                case Condition.FarRoleMembership r -> { }
                case Condition.IdentityCheck i -> { }
                case Condition.Compound s -> {
                    for (int i = 0; i < s.components().size(); i++)
                        (i == 0 ? this : past()).condition(s.components().get(i));
                }
                case Condition.QuantifierFold q -> {
                    found.add("quantifier-fold");
                    past().condition(q.filter());
                    past().condition(q.test());
                }
                case Condition.TimeWindow t -> found.add("time-window");
                case Condition.RuleStatusCheck r -> found.add("rule-status-check");
                case Condition.Unresolved u -> found.add("unresolved");
            }
        }
    }
}
