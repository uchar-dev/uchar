package dev.uchar.regelspraak.transpiler.tms;

import dev.uchar.regelspraak.graph.ProducerConsumer;
import dev.uchar.regelspraak.graph.ProducerConsumer.ElementKey;
import dev.uchar.regelspraak.ir.Condition;
import dev.uchar.regelspraak.ir.Default;
import dev.uchar.regelspraak.ir.ExceptionGroup;
import dev.uchar.regelspraak.ir.Expr;
import dev.uchar.regelspraak.ir.PrioritizedDefaults;
import dev.uchar.regelspraak.ir.ResolvedAllocation;
import dev.uchar.regelspraak.ir.ResolvedConsistency;
import dev.uchar.regelspraak.ir.ResolvedFactCreation;
import dev.uchar.regelspraak.ir.ResolvedObjectCreation;
import dev.uchar.regelspraak.ir.ResolvedRule;
import dev.uchar.regelspraak.loader.IrLoader;
import dev.uchar.regelspraak.model.RegelSpraakModel;
import dev.uchar.regelspraak.model.RegelSpraakModel.ObjectType;
import dev.uchar.regelspraak.model.RegelSpraakModel.RoleBindingRef;
import dev.uchar.regelspraak.transpiler.core.Namen;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The members of a document that can be open on an object (RUNTIME-SEMANTICS ch. 1.5), an upper bound: none in a
 * document without {@code inferFromEmpty: false}, so that its lowering asks nowhere whether something is open. In a document
 * with it a member can be open
 * <ul>
 *   <li>as an input that is not complete ({@link #openInput}): a member that holds one value, on the objects of a type on
 *       which nothing but an initialisation writes it, unless its declaration says that its input is complete. An
 *       initialisation, a rule or the initialisation default of a defaults group without an exception group, is the
 *       lowest writer of its target and makes no member derived (ch. 1.5, "The three states");</li>
 *   <li>by a writer ({@link #leftOpen}): a member a form writes that reads a member that can be open, at any depth of
 *       its condition and value ({@link ProducerConsumer#forms}).</li>
 * </ul>
 *
 * <p>A population and the link set of a relation, which {@link ProducerConsumer#forms} does not key, are complete but
 * where a creation's condition is undetermined (ch. 1.5, "A creation"): where a creation reads a member that can be open,
 * every form counts as reading one.
 */
public final class OpenableMembers {

    /** No member. */
    static final OpenableMembers NONE = new OpenableMembers(Map.of(), Set.of(), Set.of());

    private final Map<ObjectType, Set<ElementKey>> inputOf;
    private final Set<ElementKey> openInput, leftOpen;

    private OpenableMembers(Map<ObjectType, Set<ElementKey>> inputOf, Set<ElementKey> openInput, Set<ElementKey> leftOpen) {
        this.inputOf = inputOf;
        this.openInput = openInput;
        this.leftOpen = leftOpen;
    }

    /** The members of {@code loaded} that can be open. */
    static OpenableMembers of(IrLoader.Loaded loaded) {
        return of(loaded.model(), loaded.prioritizedDefaults(), ProducerConsumer.wireNavigation(loaded),
                loaded.navigationBindings(), loaded.roleBindings(), loaded.sourcePaths());
    }

    /** The members of {@code model} with its defaults groups {@code defaults} and role side tables that can be open.
     *  This is the one place where the lowering reads the document's {@code inferFromEmpty}. */
    static OpenableMembers of(RegelSpraakModel model, List<PrioritizedDefaults> defaults,
                              ProducerConsumer.Navigation navigation,
                              Map<Object, List<RoleBindingRef>> navigationBindings,
                              Map<Object, RoleBindingRef> roleBindings,
                              Map<Object, List<RoleBindingRef>> sourcePaths) {
        if (model.inferFromEmpty()) return NONE;
        Map<ObjectType, Set<ElementKey>> inputOf = new IdentityHashMap<>();
        Set<ElementKey> openInput = new HashSet<>();
        for (ObjectType type : model.objectTypes.values()) {
            Set<ElementKey> of = incomplete(model, defaults, roleBindings, type);
            inputOf.put(type, Collections.unmodifiableSet(of));
            openInput.addAll(of);
        }
        List<ProducerConsumer.Form> forms = ProducerConsumer.forms(model, defaults, navigation);
        List<Set<ElementKey>> roleReads = roleReadsOfForms(model, defaults, navigationBindings, roleBindings, sourcePaths);
        Set<ElementKey> openable = new HashSet<>(openInput), leftOpen = new HashSet<>();
        boolean relations = false, grown = true;
        while (grown) {
            grown = false;
            for (int i = 0; i < forms.size(); i++) {
                ProducerConsumer.Form f = forms.get(i);
                boolean readsOpenRole = roleReads.get(i).stream().anyMatch(openable::contains);
                if (!relations && !readsOpenRole && f.reads().stream().noneMatch(openable::contains)) continue;
                boolean creation = f.kind() == ProducerConsumer.FormKind.OBJECT_CREATION
                        || f.kind() == ProducerConsumer.FormKind.FACT_CREATION;
                boolean opensMember = leftOpen.addAll(f.writes()), opensRelations = creation && !relations;
                grown |= opensMember || opensRelations;
                relations |= creation;
                openable.addAll(f.writes());
            }
        }
        return new OpenableMembers(inputOf, openInput, leftOpen);
    }

    /** Whether no member can be open. */
    public boolean isEmpty() {
        return openInput.isEmpty() && leftOpen.isEmpty();
    }

    /** Whether {@code member} can be open on an object. */
    boolean contains(ElementKey member) {
        return openInput.contains(member) || leftOpen.contains(member);
    }

    /** Whether {@code member} can be open as an input that is not complete. */
    boolean openInput(ElementKey member) {
        return openInput.contains(member);
    }

    /** Whether a writer can leave {@code member} open. */
    boolean leftOpen(ElementKey member) {
        return leftOpen.contains(member);
    }

    /** The members of an object of {@code type}, its own class, that are input there and not complete, in the order of
     *  their declarations from the type up: each is open where the case gives it no value or does not name it. */
    public Set<ElementKey> incompleteInput(ObjectType type) {
        return inputOf.getOrDefault(type, Set.of());
    }

    private static Set<ElementKey> incomplete(RegelSpraakModel model, List<PrioritizedDefaults> defaults,
                                              Map<Object, RoleBindingRef> roleBindings, ObjectType type) {
        Set<ElementKey> out = new LinkedHashSet<>();
        for (ObjectType t : type.chain()) {
            for (String a : t.attributes.keySet())
                if (t.attributeGranularity.get(a) == null && !t.attributeDimensions.containsKey(a)
                        && !t.completeAttributes.contains(a) && !written(model, defaults, type, a, false))
                    out.add(ProducerConsumer.elementOf(t, a, false));
            for (String k : t.characteristics)
                if (t.characteristicGranularity.get(k) == null && !t.completeCharacteristics.contains(k)
                        && !written(model, defaults, type, k, true))
                    out.add(ProducerConsumer.elementOf(t, k, true));
        }
        for (RegelSpraakModel.FactType ft : model.factTypes.values()) {
            if (ft.reciprocal || ft.roles.size() != 2) continue;
            for (int farIdx = 0; farIdx < 2; farIdx++) {
                RegelSpraakModel.Role far = ft.roles.get(farIdx), near = ft.roles.get(1 - farIdx);
                if (far.many() || far.complete() || far.name() == null) continue;
                ObjectType nearFiller = model.objectTypeByName(near.objectTypeName());
                if (nearFiller != null && type.isA(nearFiller)
                        && !writtenRoleOn(model, roleBindings, type, ft, farIdx))
                    out.add(ElementKey.role(ft.name, farIdx, far.name()));
            }
        }
        return out;
    }

    /** Whether an object creation or fact creation can lay a link on {@code ft} placing an instance of {@code otype}
     *  in the sister role opposite {@code farIdx} (RUNTIME-SEMANTICS ch. 1.5, {@code Eval.written_role_on}). */
    private static boolean writtenRoleOn(RegelSpraakModel model, Map<Object, RoleBindingRef> roleBindings,
                                         ObjectType otype, RegelSpraakModel.FactType ft, int farIdx) {
        RegelSpraakModel.Role far = ft.roles.get(farIdx);
        for (ResolvedObjectCreation oc : model.objectCreations) {
            if (oc.subject() == null || oc.createdType() == null || !otype.isA(oc.subject())) continue;
            RoleBindingRef rb = roleBindings.get(oc);
            if (rb != null) {
                RegelSpraakModel.FactType ocFt = factType(model, rb.factType());
                if (ocFt == ft && roleIndexByDeclaredName(ocFt, rb.role()) == farIdx) return true;
            } else {
                RegelSpraakModel.FactType uniqueFt = null;
                int uniqueIdx = -1, count = 0;
                for (RegelSpraakModel.FactType cand : model.factTypes.values()) {
                    if (!cand.reciprocal && cand.roles.size() == 2) {
                        for (int idx = 0; idx < 2; idx++) {
                            ObjectType nearType = model.objectTypeByName(cand.roles.get(1 - idx).objectTypeName());
                            ObjectType farType = model.objectTypeByName(cand.roles.get(idx).objectTypeName());
                            if (nearType != null && farType != null
                                    && oc.subject().isA(nearType) && oc.createdType().isA(farType)) {
                                uniqueFt = cand;
                                uniqueIdx = idx;
                                count++;
                            }
                        }
                    } else if (cand.reciprocal && cand.roles.size() == 1) {
                        ObjectType rType = model.objectTypeByName(cand.roles.get(0).objectTypeName());
                        if (rType != null && oc.subject().isA(rType) && oc.createdType().isA(rType)) count++;
                    }
                }
                if (count == 1 && uniqueFt == ft && uniqueIdx == farIdx) return true;
            }
        }
        for (ResolvedFactCreation fc : model.factCreations) {
            if (fc.factType() == null || !Namen.norm(fc.factType()).equals(Namen.norm(ft.name))) continue;
            boolean matchesTarget = matchesNameOrPlural(far, fc.targetRole());
            boolean matchesAnchor = matchesNameOrPlural(far, fc.anchorRole());
            ObjectType targetOt = fc.target() != null ? fc.target() : targetTypeFromRole(model, ft, fc.targetRole());
            if ((matchesTarget && fc.anchor() != null && otype.isA(fc.anchor()))
                    || (matchesAnchor && targetOt != null && otype.isA(targetOt)))
                return true;
        }
        return false;
    }

    private static ObjectType targetTypeFromRole(RegelSpraakModel model, RegelSpraakModel.FactType ft, String roleName) {
        if (roleName == null || ft.roles.size() != 2) return null;
        boolean m0 = ft.roles.get(0).name() != null && Namen.norm(ft.roles.get(0).name()).equals(Namen.norm(roleName));
        boolean m1 = ft.roles.get(1).name() != null && Namen.norm(ft.roles.get(1).name()).equals(Namen.norm(roleName));
        if (m0 && !m1) return model.objectTypeByName(ft.roles.get(0).objectTypeName());
        if (m1 && !m0) return model.objectTypeByName(ft.roles.get(1).objectTypeName());
        return null;
    }

    private static boolean matchesNameOrPlural(RegelSpraakModel.Role role, String candidate) {
        if (candidate == null) return false;
        String norm = Namen.norm(candidate);
        return role.name() != null && Namen.norm(role.name()).equals(norm)
                || role.plural() != null && Namen.norm(role.plural()).equals(norm);
    }

    private static RegelSpraakModel.FactType factType(RegelSpraakModel model, String name) {
        if (name == null) return null;
        String norm = Namen.norm(name);
        for (RegelSpraakModel.FactType ft : model.factTypes.values())
            if (Namen.norm(ft.name).equals(norm)) return ft;
        return null;
    }

    private static int roleIndexByDeclaredName(RegelSpraakModel.FactType ft, String roleName) {
        if (ft == null || roleName == null) return -1;
        String norm = Namen.norm(roleName);
        for (int i = 0; i < ft.roles.size(); i++) {
            String n = ft.roles.get(i).name();
            if (n != null && Namen.norm(n).equals(norm)) return i;
        }
        return -1;
    }

    /** Per form in the order of {@link ProducerConsumer#forms}, the non-reciprocal single-role keys it reads via role
     *  checks, navigated reads or checks, or fold ranges ({@code sourceRole} / {@code sourcePath}). */
    private static List<Set<ElementKey>> roleReadsOfForms(RegelSpraakModel model, List<PrioritizedDefaults> defaults,
                                                          Map<Object, List<RoleBindingRef>> navigationBindings,
                                                          Map<Object, RoleBindingRef> roleBindings,
                                                          Map<Object, List<RoleBindingRef>> sourcePaths) {
        List<Set<ElementKey>> out = new ArrayList<>();
        for (ResolvedRule r : model.resolvedRules) {
            RoleReader rr = new RoleReader(model, navigationBindings, roleBindings, sourcePaths, new LinkedHashSet<>());
            rr.condition(r.condition());
            rr.expr(r.value());
            out.add(rr.out);
        }
        for (ResolvedObjectCreation o : model.objectCreations) {
            RoleReader rr = new RoleReader(model, navigationBindings, roleBindings, sourcePaths, new LinkedHashSet<>());
            rr.condition(o.condition());
            for (ResolvedObjectCreation.MemberAssignment t : o.assignments()) rr.expr(t.value());
            out.add(rr.out);
        }
        for (ResolvedAllocation v : model.allocations) {
            RoleReader rr = new RoleReader(model, navigationBindings, roleBindings, sourcePaths, new LinkedHashSet<>());
            rr.condition(v.condition());
            out.add(rr.out);
        }
        for (ResolvedConsistency c : model.consistencies) {
            RoleReader rr = new RoleReader(model, navigationBindings, roleBindings, sourcePaths, new LinkedHashSet<>());
            rr.condition(c.assertion());
            out.add(rr.out);
        }
        for (int i = 0; i < model.uniquenesses.size(); i++) out.add(Set.of());
        for (int i = 0; i < model.uniquenessUnions.size(); i++) out.add(Set.of());
        for (ResolvedFactCreation f : model.factCreations) {
            RoleReader rr = new RoleReader(model, navigationBindings, roleBindings, sourcePaths, new LinkedHashSet<>());
            rr.condition(f.condition());
            out.add(rr.out);
        }
        for (PrioritizedDefaults p : defaults) {
            RoleReader rr = new RoleReader(model, navigationBindings, roleBindings, sourcePaths, new LinkedHashSet<>());
            for (Default d : p.defaults()) rr.defaultEntry(d);
            out.add(rr.out);
        }
        return out;
    }

    private record RoleReader(RegelSpraakModel model, Map<Object, List<RoleBindingRef>> navigationBindings,
                              Map<Object, RoleBindingRef> roleBindings, Map<Object, List<RoleBindingRef>> sourcePaths,
                              Set<ElementKey> out) {

        void defaultEntry(Default d) {
            condition(d.condition());
            expr(d.consequence());
            if (d.exceptions() != null)
                for (ExceptionGroup g : d.exceptions().members())
                    for (Default m : g.defaults()) defaultEntry(m);
        }

        private void addFarRole(RoleBindingRef rb) {
            if (rb == null) return;
            RegelSpraakModel.FactType ft = factType(model, rb.factType());
            if (ft == null || ft.reciprocal || ft.roles.size() != 2) return;
            int idx = roleIndexByDeclaredName(ft, rb.role());
            if (idx >= 0 && !ft.roles.get(idx).many() && ft.roles.get(idx).name() != null)
                out.add(ElementKey.role(ft.name, idx, ft.roles.get(idx).name()));
        }

        private void addOppositeRole(RoleBindingRef rb) {
            if (rb == null) return;
            RegelSpraakModel.FactType ft = factType(model, rb.factType());
            if (ft == null || ft.reciprocal || ft.roles.size() != 2) return;
            int nearIdx = roleIndexByDeclaredName(ft, rb.role());
            if (nearIdx < 0) return;
            int farIdx = 1 - nearIdx;
            if (!ft.roles.get(farIdx).many() && ft.roles.get(farIdx).name() != null)
                out.add(ElementKey.role(ft.name, farIdx, ft.roles.get(farIdx).name()));
        }

        private void addPath(List<RoleBindingRef> path) {
            if (path != null) for (RoleBindingRef hop : path) addFarRole(hop);
        }

        void condition(Condition c) {
            if (c == null) return;
            switch (c) {
                case Condition.Comparison v -> { expr(v.left()); expr(v.right()); }
                case Condition.Predicate p -> expr(p.operand());
                case Condition.TimeWindow p -> { expr(p.from()); expr(p.until()); }
                case Condition.CharacteristicCheck k -> {
                    addPath(navigationBindings.get(k));
                    if (k.navigation() != null) addFarRole(k.navigation());
                }
                case Condition.Compound s -> { for (Condition d : s.components()) condition(d); }
                case Condition.QuantifierFold kv -> {
                    if (!kv.sourcePath().isEmpty()) addPath(kv.sourcePath());
                    else addFarRole(kv.sourceRole());
                    condition(kv.filter());
                    condition(kv.test());
                }
                case Condition.RoleMembership r -> addOppositeRole(roleBindings.get(r));
                case Condition.FarRoleMembership ignored -> { }
                case Condition.RuleStatusCheck ignored -> { }
                case Condition.IdentityCheck ignored -> { }
                case Condition.Unresolved ignored -> { }
            }
        }

        void expr(Expr e) {
            if (e == null) return;
            switch (e) {
                case Expr.AttributeReference ref -> addPath(navigationBindings.get(ref));
                case Expr.Binary b -> { expr(b.left()); expr(b.right()); }
                case Expr.FunctionCall f -> {
                    if (sourcePaths.containsKey(f)) addPath(sourcePaths.get(f));
                    else if (roleBindings.containsKey(f)) addFarRole(roleBindings.get(f));
                    for (Expr a : f.arguments()) expr(a);
                    expr(f.emptyFallback());
                    condition(f.filter());
                }
                case Expr.InstanceSetUnion u -> {
                    for (Expr.InstanceSetUnion.Branch t : u.branches()) {
                        addFarRole(t.sourceRole());
                        condition(t.filter());
                    }
                }
                case Expr.Rounding a -> expr(a.base());
                case Expr.Clamp b -> { expr(b.base()); expr(b.minimum()); expr(b.maximum()); }
                case Expr.DimensionCellRead c -> {
                    if (c.attribute() != null) addPath(navigationBindings.get(c.attribute()));
                }
                case Expr.TextComposition t -> {
                    for (Expr.TextComposition.Part d : t.parts())
                        if (d instanceof Expr.TextComposition.EmbeddedExpression i) expr(i.expr());
                }
                case Expr.Let l -> { expr(l.value()); expr(l.body()); }
                case Expr.IfThenElse i -> { condition(i.condition()); expr(i.then()); expr(i.otherwise()); }
                case Expr.Construct c -> expr(c.argument());
                case Expr.Literal ignored -> { }
                case Expr.ParameterRef ignored -> { }
                case Expr.VariableRef ignored -> { }
                case Expr.BinderRef ignored -> { }
                case Expr.Constant ignored -> { }
                case Expr.Unresolved ignored -> { }
            }
        }
    }

    /** Whether a form other than an initialisation writes the member {@code name}, a characteristic where
     *  {@code characteristic}, on the objects of {@code type}, one on {@code type} or on a type above it: a rule other
     *  than an initialisation, or a defaults group with a default other than its initialisation default or with an
     *  exception group, whose members decide above the input (ch. 7.9). */
    private static boolean written(RegelSpraakModel model, List<PrioritizedDefaults> defaults, ObjectType type, String name,
                                   boolean characteristic) {
        for (ResolvedRule r : model.resolvedRules) {
            Expr target = r.target() instanceof Expr.DimensionCellRead c ? c.attribute() : r.target();
            if (r.kind() != ResolvedRule.Kind.INITIALISATIE
                    && target instanceof Expr.AttributeReference a && a.objectType() != null && a.attribute() != null
                    && (r.kind() == ResolvedRule.Kind.KENMERKTOEKENNING) == characteristic
                    && Namen.norm(a.attribute()).equals(Namen.norm(name)) && type.isA(a.objectType()))
                return true;
        }
        for (PrioritizedDefaults p : defaults) {
            ObjectType subject = model.objectTypeByName(p.subject());
            if (subject != null && p.target() != null && p.isCharacteristic() == characteristic
                    && Namen.norm(p.target()).equals(Namen.norm(name)) && type.isA(subject)
                    && p.defaults().stream().anyMatch(d -> !d.initialisation() || d.exceptions() != null))
                return true;
        }
        return false;
    }
}
