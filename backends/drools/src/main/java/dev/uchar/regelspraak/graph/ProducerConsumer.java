package dev.uchar.regelspraak.graph;

import dev.uchar.regelspraak.ir.Condition;
import dev.uchar.regelspraak.ir.Default;
import dev.uchar.regelspraak.ir.ExceptionGroup;
import dev.uchar.regelspraak.ir.Expr;
import dev.uchar.regelspraak.ir.PrioritizedDefaults;
import dev.uchar.regelspraak.ir.ResolvedConsistency;
import dev.uchar.regelspraak.ir.ResolvedFactCreation;
import dev.uchar.regelspraak.ir.ResolvedObjectCreation;
import dev.uchar.regelspraak.ir.ResolvedRule;
import dev.uchar.regelspraak.ir.ResolvedUniqueness;
import dev.uchar.regelspraak.ir.ResolvedUniquenessUnion;
import dev.uchar.regelspraak.ir.ResolvedAllocation;
import dev.uchar.regelspraak.loader.IrLoader;
import dev.uchar.regelspraak.model.RegelSpraakModel;
import dev.uchar.regelspraak.transpiler.core.Namen;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The pure-IR producer/consumer analysis over a resolved {@link RegelSpraakModel}: which form of the rule layer
 * WRITES which kenmerk/attribuut ({@link #producerIndex}) and which kenmerken/attributen a form READS
 * ({@link #forms}, and {@link #consumed} for one rule). The forms are those of IR-SPEC ch. 5.1: rules, object
 * creations, allocations, consistencies, uniquenesses, fact creations and defaults groups.
 *
 * <p>It walks the IR records only (no Drools), so a model the loader rebuilt from {@code ir.json} and a model a
 * resolver built from source give the same answers, once each route supplies what rides beside its model: the
 * defaults-group list, and where a navigated kenmerk check leads ({@link Navigation}). It is the one authority for
 * its two readers: the {@link RuleGraph} dataflow graph, and a scope-completeness check that asks whether a
 * consumed element has a producer outside a rule scope. Names are compared as the verifier compares them
 * ({@link Namen#norm}), so a case-only spelling difference still matches producer to consumer and the element
 * identity is the node identity of IR-SPEC ch. 5.1.
 */
public final class ProducerConsumer {

    private ProducerConsumer() {}

    /** The identity of a consumed/produced model element: the objecttype it lives on (canonical name, or ""
     *  when the owner is unknown), its kenmerk/attribuut naam, and whether it is a kenmerk (§13.4.5 boolean
     *  property) rather than an ordinary attribuut. Objecttype and naam are held in {@link Namen#norm} form.
     *
     *  <p><b>The objecttype is the member's DECLARING type</b> (IR-SPEC ch. 2.2), not the type the form that
     *  touched it happens to range over — see {@link #elementOf}. A member is declared once and inherited, so
     *  a write on the supertype and a read on the subtype touch ONE element and must key alike. */
    public record ElementKey(String objectType, String name, boolean characteristic) {
        public static ElementKey of(String objectType, String name, boolean characteristic) {
            return new ElementKey(Namen.norm(objectType), Namen.norm(name), characteristic);
        }
    }

    /**
     * The element identity of member {@code naam} touched through {@code holder} — keyed by the type on
     * {@code holder}'s chain that DECLARES it (IR-SPEC ch. 2.2), so producer and consumer meet on one key.
     *
     * <p>This is the whole ⊑-awareness of this analysis, and it sits HERE rather than in each reader because
     * both readers ask the same question of the same index: the scope check matches a consumed element against
     * the producer index, and {@link RuleGraph} decides INVOER / eind-output by the same membership. Keying on
     * the rule's own subject type instead makes a producer on the supertype and a consumer on the subtype two
     * different elements: the scope check then concludes "no producer at all, so this is a genuine request
     * input" and lets a scope through whose kenmerk silently evaluates false, and the graph draws the same
     * member twice — once as INVOER nobody writes, once as an eind-output nobody reads. Both claims are false.
     *
     * <p>Both directions collapse onto the declarant, deliberately. Producer-on-supertype / consumer-on-subtype
     * is the full-coverage case. Producer-on-subtype / consumer-on-supertype is PARTIAL coverage (that producer
     * only serves subtype instances), and it still keys the same: the scope check's question is whether a
     * producer EXISTS that fell outside the scope, and a partial producer outside the scope is one. Chain
     * awareness here can therefore only ever report MORE than the flat reading, never silence it.
     *
     * <p>The chain walk reads the OWN declaration maps per type on purpose, because the question is WHICH type
     * declares the member, and it is kind-aware: a kenmerk and an attribuut may legally share a name on
     * different types of one chain, and the model's own {@code declarantOf} answers for both kinds at once. A
     * member nothing on the chain declares keeps its holder, which is the reading from before this rule existed.
     */
    public static ElementKey elementOf(RegelSpraakModel.ObjectType holder, String name, boolean characteristic) {
        if (holder == null) return ElementKey.of("", name, characteristic);
        return ElementKey.of(declarant(holder, name, characteristic).name, name, characteristic);
    }

    private static RegelSpraakModel.ObjectType declarant(RegelSpraakModel.ObjectType holder, String name,
                                                         boolean characteristic) {
        if (name == null) return holder;
        for (RegelSpraakModel.ObjectType t : holder.chain())
            if (characteristic ? t.characteristics.stream().anyMatch(k -> sameName(k, name))
                        : t.attributes.keySet().stream().anyMatch(a -> sameName(a, name)))
                return t;
        return holder;
    }

    /**
     * The model-wide producer index: every element a form WRITES ({@link #forms}), mapped to the base names of
     * the forms that write it. A kenmerk and an attribuut of the same spelling are distinct keys. {@code defaults}
     * is the defaults-group list, which a resolver's model holds ({@code model.prioritizedDefaults}) and a model the
     * loader rebuilt carries beside it ({@code IrLoader.Loaded.prioritizedDefaults()}).
     */
    public static Map<ElementKey, Set<String>> producerIndex(RegelSpraakModel model,
                                                                  List<PrioritizedDefaults> defaults) {
        Map<ElementKey, Set<String>> out = new LinkedHashMap<>();
        for (Form v : read(model, defaults, null))
            for (ElementKey el : v.writes()) out.computeIfAbsent(el, k -> new LinkedHashSet<>()).add(v.name());
        return out;
    }

    /** The kind of a form of the rule layer (IR-SPEC ch. 2, Rules), one per list the forms sit in: a rule (an
     *  assignment, initialisation or characteristic attribution), an object creation, an allocation, a
     *  consistency, a uniqueness (single-source or a union), a fact creation, a defaults group. */
    public enum FormKind { REGEL, OBJECT_CREATION, ALLOCATION, CONSISTENCY, UNIQUENESS, FACT_CREATION, DEFAULTS_GROUP }

    /** One form as this analysis reads it: its kind, its base name ({@link #baseName}; a defaults group without a
     *  name is named by its pointer in the document), the elements it WRITES and the elements it READS. */
    public record Form(FormKind kind, String name, Set<ElementKey> writes, Set<ElementKey> reads) {}

    /**
     * Every form of the rule layer with the attributes and characteristics it writes and reads, per IR-SPEC ch. 5.1
     * and the producer-consumer table of RUNTIME-SEMANTICS ch. 6.1. The forms come in the order of the document's
     * lists (the rules, the object creations, the allocations, the consistencies, the uniquenesses with the
     * single-source ones first, the fact creations), and the defaults groups of {@code defaults} last.
     *
     * <ul>
     *   <li>a rule writes its target and reads its condition and value ({@link #consumed});</li>
     *   <li>an object creation writes the members its assignments set on the created type, and reads its condition
     *       and the assigned values, evaluated against the creation's subject;</li>
     *   <li>an allocation writes its target attribute and its remainder attribute, and reads its source amount,
     *       the ratio, maximum and ordering attributes of the recipients, and its condition;</li>
     *   <li>a consistency reads its assertion, a uniqueness its key attributes (a union those of every source);
     *       what either writes is its channel, no member;</li>
     *   <li>a fact creation reads its condition; what it writes is a population and a relation, no member;</li>
     *   <li>a defaults group writes its target and reads the justification and consequence of every default,
     *       nested exception groups included.</li>
     * </ul>
     *
     * A population, a relation's link set and a channel are nodes of ch. 5.1 too; this analysis does not key them,
     * so a form's reads and writes of those are not here.
     */
    public static List<Form> forms(RegelSpraakModel model, List<PrioritizedDefaults> defaults, Navigation navigation) {
        return read(model, defaults, java.util.Objects.requireNonNull(navigation, "navigation"));
    }

    /** The reading behind {@link #forms} and {@link #producerIndex}. Without {@code navigation} every read set is
     *  empty: the producer index asks what a form writes, and no write depends on where a check navigates. */
    private static List<Form> read(RegelSpraakModel model, List<PrioritizedDefaults> defaults, Navigation navigation) {
        java.util.Objects.requireNonNull(defaults, "defaults");
        List<Form> out = new ArrayList<>();
        for (ResolvedRule r : model.resolvedRules) {
            Set<ElementKey> writes = new LinkedHashSet<>();
            if (r.target() instanceof Expr.AttributeReference ref)
                writes.add(elementOf(ref.objectType(), ref.attribute(),
                        r.kind() == ResolvedRule.Kind.KENMERKTOEKENNING));
            out.add(new Form(FormKind.REGEL, baseName(r.name()), writes,
                    navigation == null ? Set.of() : consumed(r, navigation)));
        }
        for (ResolvedObjectCreation o : model.objectCreations) {
            Set<ElementKey> writes = new LinkedHashSet<>();
            for (ResolvedObjectCreation.MemberInit t : o.initialisations())
                if (t.attribute() != null) writes.add(elementOf(o.createdType(), t.attribute(), t.characteristic()));
            Reader l = new Reader(navigation, new LinkedHashSet<>());
            l.condition(o.condition(), o.subject());
            for (ResolvedObjectCreation.MemberInit t : o.initialisations()) l.expr(t.value(), o.subject());
            out.add(new Form(FormKind.OBJECT_CREATION, baseName(o.name()), writes, l.out()));
        }
        for (ResolvedAllocation v : model.allocations) {
            Set<ElementKey> writes = new LinkedHashSet<>();
            if (v.targetAttribute() != null) writes.add(elementOf(v.targetType(), v.targetAttribute(), false));
            if (v.remainderAttribute() != null) writes.add(elementOf(v.sourceType(), v.remainderAttribute(), false));
            Reader l = new Reader(navigation, new LinkedHashSet<>());
            l.member(v.sourceType(), v.sourceAttribute(), false);
            for (String a : new String[] { v.ratioAttribute(), v.maximumAttribute(), v.orderingAttribute() })
                l.member(v.targetType(), a, false);
            l.condition(v.condition(), v.sourceType());
            out.add(new Form(FormKind.ALLOCATION, baseName(v.name()), writes, l.out()));
        }
        for (ResolvedConsistency c : model.consistencies) {
            Reader l = new Reader(navigation, new LinkedHashSet<>());
            l.condition(c.assertion(), c.subject());
            out.add(new Form(FormKind.CONSISTENCY, baseName(c.name()), Set.of(), l.out()));
        }
        for (ResolvedUniqueness u : model.uniquenesses) {
            Reader l = new Reader(navigation, new LinkedHashSet<>());
            for (String key : u.keys()) l.member(u.subject(), key, false);
            out.add(new Form(FormKind.UNIQUENESS, baseName(u.name()), Set.of(), l.out()));
        }
        for (ResolvedUniquenessUnion u : model.uniquenessUnions) {
            Reader l = new Reader(navigation, new LinkedHashSet<>());
            for (ResolvedUniquenessUnion.Source b : u.sources())
                if (b.type() != null) for (String key : b.keys()) l.member(b.type(), key, false);
            out.add(new Form(FormKind.UNIQUENESS, baseName(u.name()), Set.of(), l.out()));
        }
        for (ResolvedFactCreation f : model.factCreations) {
            Reader l = new Reader(navigation, new LinkedHashSet<>());
            l.condition(f.condition(), f.anchor());
            out.add(new Form(FormKind.FACT_CREATION, baseName(f.name()), Set.of(), l.out()));
        }
        for (int i = 0; i < defaults.size(); i++) {
            PrioritizedDefaults p = defaults.get(i);
            RegelSpraakModel.ObjectType subjectType = model.objectTypeByName(p.subject());
            Set<ElementKey> writes = new LinkedHashSet<>();
            if (p.target() != null) writes.add(elementOf(subjectType, p.target(), p.isCharacteristic()));
            // the group reads what its defaults read, so the per-default reading and this one cannot drift apart
            Set<ElementKey> reads = new LinkedHashSet<>();
            for (DefaultRead d : readDefaults(model, p, navigation)) {
                reads.addAll(d.justification());
                reads.addAll(d.consequence());
            }
            out.add(new Form(FormKind.DEFAULTS_GROUP,
                    p.name() != null ? baseName(p.name()) : "/rules/prioritizedDefaults/" + i, writes, reads));
        }
        return out;
    }

    /** What one default of a defaults group reads. {@code path} is its place in the tree: [i] for the i-th default of
     *  the group, [i, g, j] for the j-th default of the g-th exception group of default i, and so on. {@code
     *  rechtvaardiging} is what its justification reads, {@code consequence} what its consequence reads; a
     *  group-default has neither (RUNTIME-SEMANTICS ch. 7.9). */
    public record DefaultRead(List<Integer> path, Default dflt, Set<ElementKey> justification,
                                Set<ElementKey> consequence) {}

    /**
     * What every default of {@code group} reads, in the order of the group (highest priority first) and per default the
     * default itself before the defaults of its exception groups, all against the subject of the group (RUNTIME-SEMANTICS
     * ch. 7.9: a descendant's read is the group's read). The walk is that of {@link #forms}, which takes the group's
     * reads from here, so the union of every justification and consequence set is exactly that group's
     * {@link Form#reads()}. A consumer that names the default where a read falls outside a scope reads the default's own
     * source text ({@code Default.source()}, or that of its justification or consequence) beside the path.
     */
    public static List<DefaultRead> readings(RegelSpraakModel model, PrioritizedDefaults group, Navigation navigation) {
        return readDefaults(model, group, java.util.Objects.requireNonNull(navigation, "navigation"));
    }

    /** The reading behind {@link #readings}; without {@code navigation} every set is empty, as in {@link #read}. */
    private static List<DefaultRead> readDefaults(RegelSpraakModel model, PrioritizedDefaults group, Navigation navigation) {
        RegelSpraakModel.ObjectType subjectType = model.objectTypeByName(group.subject());
        List<DefaultRead> out = new ArrayList<>();
        for (int i = 0; i < group.defaults().size(); i++)
            readings(group.defaults().get(i), List.of(i), subjectType, navigation, out);
        return out;
    }

    private static void readings(Default d, List<Integer> path, RegelSpraakModel.ObjectType subjectType,
                                 Navigation navigation, List<DefaultRead> out) {
        Reader justification = new Reader(navigation, new LinkedHashSet<>());
        justification.condition(d.condition(), subjectType);
        Reader consequence = new Reader(navigation, new LinkedHashSet<>());
        consequence.expr(d.consequence(), subjectType);
        out.add(new DefaultRead(path, d, java.util.Collections.unmodifiableSet(justification.out()),
                java.util.Collections.unmodifiableSet(consequence.out())));
        if (d.exceptions() == null) return;
        List<ExceptionGroup> groups = d.exceptions().members();
        for (int g = 0; g < groups.size(); g++) {
            List<Default> members = groups.get(g).defaults();
            for (int j = 0; j < members.size(); j++) {
                List<Integer> sub = new ArrayList<>(path);
                sub.add(g);
                sub.add(j);
                readings(members.get(j), List.copyOf(sub), subjectType, navigation, out);
            }
        }
    }

    /**
     * The object type a navigated kenmerk check reaches, as one route to a model records it. The check reads the
     * kenmerk of the object its hop reaches (IR-SPEC ch. 4.5), and its record does not name that type: a model the
     * loader rebuilt from the wire carries the hop beside the model, and a resolver's record names the role the
     * hop walks. The analysis cannot tell which route built its input, so the caller answers with the route's own
     * {@link #sourceNavigation} or {@link #wireNavigation}; the answer of the other route reads every navigated check
     * as the subject's own kenmerk.
     */
    @FunctionalInterface
    public interface Navigation {
        /** The type {@code check}'s hop reaches from {@code subjectType}, or null when the check does not navigate. */
        RegelSpraakModel.ObjectType reaches(Condition.CharacteristicCheck check, RegelSpraakModel.ObjectType subjectType);
    }

    /** The answer of a resolver's model: the record names the role its hop walks, and the reached type is the
     *  filler of the role of that name in a relation the subject takes part in, found as the emitter finds the
     *  binding it writes for the hop. A navigated check whose role reaches no unique type fails loudly, as the
     *  emitter refuses to write it. */
    public static Navigation sourceNavigation(RegelSpraakModel model) {
        return (k, subjectType) -> {
            if (k.navigation() == null) return null;
            RegelSpraakModel.ObjectType reaches = model.roleTargetTypeToSubject(subjectType, k.navigation());
            if (reaches == null)
                throw new IllegalStateException("the characteristic check '" + k.characteristic() + "' navigates by the role '"
                        + k.navigation() + "', which reaches no unique object type from "
                        + (subjectType == null ? "an unknown subject" : "'" + subjectType.name + "'"));
            return reaches;
        };
    }

    /** The answer of a model the loader rebuilt: the hop rides beside the model ({@code path}), and
     *  its binding names the role the reached object plays, whose declared filler is the reached type. A binding
     *  that names no declared role with a declared filler fails loudly; the verifier refuses such a document
     *  (WF-053). */
    public static Navigation wireNavigation(IrLoader.Loaded loaded) {
        RegelSpraakModel model = loaded.model();
        Map<Object, List<RegelSpraakModel.RoleBindingRef>> hops = loaded.navigationBindings();
        return (k, subjectType) -> {
            List<RegelSpraakModel.RoleBindingRef> path = hops.get(k);
            if (path == null || path.isEmpty()) return null;
            RegelSpraakModel.RoleBindingRef hop = path.get(path.size() - 1);
            for (RegelSpraakModel.FactType ft : model.factTypes.values())
                if (sameName(ft.name, hop.factType()))
                    for (RegelSpraakModel.Role role : ft.roles)
                        if (role.name() != null && sameName(role.name(), hop.role())) {
                            RegelSpraakModel.ObjectType reaches = model.objectTypeByName(role.objectTypeName());
                            if (reaches != null) return reaches;
                        }
            throw new IllegalStateException("the characteristic check '" + k.characteristic() + "' navigates by the role '"
                    + hop.role() + "' of '" + hop.factType() + "', which names no declared role with a declared filler");
        };
    }

    /** The kenmerken/attributen a resolved rule CONSUMES: every kenmerk check and attribuut read in its
     *  voorwaarde and in its RHS {@code waarde}, at any depth — the conditions inside an expression included (an
     *  aggregation's filter, a branch of an instance-set union, the guard of an if-then-else), each with the
     *  subject it is evaluated against. The rule's OWN write target ({@code target}) is not scanned — a rule
     *  reading what it writes is not a cross-rule dependency. The walk reads what IR-SPEC ch. 5.1 lets a rule
     *  read of attributes and characteristics, node kind by node kind, and each switch below is exhaustive, so a
     *  node kind the IR records gain later fails to compile here until someone decides what it reads.
     *
     *  <p>A navigated kenmerk check reads the kenmerk of the type its hop reaches, which is {@code navigation}'s
     *  answer (see {@link Navigation}). */
    public static Set<ElementKey> consumed(ResolvedRule r, Navigation navigation) {
        Set<ElementKey> out = new LinkedHashSet<>();
        Reader reader = new Reader(java.util.Objects.requireNonNull(navigation, "navigation"), out);
        RegelSpraakModel.ObjectType subject = r.target() instanceof Expr.AttributeReference ref ? ref.objectType() : null;
        reader.condition(r.condition(), subject);
        reader.expr(r.value(), subject);
        return out;
    }

    /** The one walk behind {@link #consumed}, {@link #forms} and {@link #readings}, over a condition or an
     *  expression with the subject a kenmerk check without an owner of its own is evaluated against, and from which a
     *  navigated check's hop starts. Only a kenmerk check asks for the subject; an attribuut read names its type
     *  itself. Without a {@code navigation} it reads nothing (see {@link #read}). */
    private record Reader(Navigation navigation, Set<ElementKey> out) {

        /** A member a form names outright rather than through an expression: an allocation's source amount or
         *  recipient attribute, a key of a uniqueness. */
        void member(RegelSpraakModel.ObjectType holder, String name, boolean characteristic) {
            if (navigation == null || name == null) return;
            out.add(elementOf(holder, name, characteristic));
        }

        void condition(Condition c, RegelSpraakModel.ObjectType subject) {
            if (c == null || navigation == null) return;
            switch (c) {
                case Condition.Comparison v -> { expr(v.left(), subject); expr(v.right(), subject); }
                case Condition.Predicate p -> expr(p.operand(), subject);
                case Condition.TimeWindow p -> { expr(p.from(), subject); expr(p.until(), subject); }
                // A navigated check lives on the object its hop reaches; any other check lives on its host, or on
                // the subject when it names none.
                case Condition.CharacteristicCheck k -> {
                    RegelSpraakModel.ObjectType reaches = navigation.reaches(k, subject);
                    RegelSpraakModel.ObjectType carrier =
                            reaches != null ? reaches : k.owner() != null ? k.owner() : subject;
                    out.add(elementOf(carrier, k.characteristic(), true));
                }
                case Condition.Compound s -> { for (Condition d : s.components()) condition(d, subject); }
                // IR-SPEC ch. 4.5: a quantifier fold's filter and matrix are evaluated with the ELEMENT as subject
                // (RUNTIME-SEMANTICS ch. 3.9a), hence the element type rather than the enclosing subject.
                case Condition.QuantifierFold kv -> {
                    condition(kv.filter(), kv.elementObjectType());
                    condition(kv.test(), kv.elementObjectType());
                }
                // A role check reads a relation's link set and a rule-version test a channel: nodes of IR-SPEC
                // ch. 5.1 this graph does not draw, so neither contributes an element.
                case Condition.RoleMembership ignored -> { }
                case Condition.FarRoleMembership ignored -> { }
                case Condition.RuleStatusCheck ignored -> { }
                case Condition.Unresolved ignored -> { }
            }
        }

        void expr(Expr e, RegelSpraakModel.ObjectType subject) {
            if (e == null || navigation == null) return;
            switch (e) {
                case Expr.AttributeReference ref -> out.add(elementOf(ref.objectType(), ref.attribute(), false));
                case Expr.Binary b -> { expr(b.left(), subject); expr(b.right(), subject); }
                case Expr.FunctionCall f -> {
                    // A count's source names the relation's ROLE, never a member of the element type (the loader
                    // refuses a member there): it reads the link set, not an attribuut of each contributor.
                    if (!isCountSource(f)) for (Expr a : f.arguments()) expr(a, subject);
                    expr(f.emptyFallback(), subject);
                    // the filter is evaluated per contributor, with the element as subject
                    condition(f.filter(), f.elementObjectType() != null ? f.elementObjectType() : subject);
                }
                // each selection's filter is evaluated per contributor, as a fold's filter is
                case Expr.InstanceSetUnion u -> {
                    for (Expr.InstanceSetUnion.Branch t : u.branches()) condition(t.filter(), t.elementType());
                }
                case Expr.Rounding a -> expr(a.base(), subject);
                case Expr.Clamp b -> { expr(b.base(), subject); expr(b.minimum(), subject); expr(b.maximum(), subject); }
                case Expr.DimensionCellRead c -> expr(c.attribute(), subject);
                case Expr.TextComposition t -> {
                    for (Expr.TextComposition.Part d : t.parts())
                        if (d instanceof Expr.TextComposition.EmbeddedExpression i) expr(i.expr(), subject);
                }
                // Call-by-need does not narrow this: a value that is never evaluated is still a read the author
                // wrote, and IR-SPEC ch. 5 draws the edge for it as well (a static over-approximation).
                case Expr.Let l -> { expr(l.value(), subject); expr(l.body(), subject); }
                case Expr.IfThenElse i -> { condition(i.condition(), subject); expr(i.then(), subject); expr(i.otherwise(), subject); }
                case Expr.Literal ignored -> { }
                case Expr.ParameterRef ignored -> { }
                case Expr.VariableRef ignored -> { }
                case Expr.BinderRef ignored -> { }
                case Expr.Constant ignored -> { }
                case Expr.Unresolved ignored -> { }
            }
        }
    }

    /** A count over one source whose argument names no member of the element type: the count source of IR-SPEC
     *  ch. 4.3, which names the relation's role. */
    private static boolean isCountSource(Expr.FunctionCall f) {
        return f.kind() == Expr.FunctionKind.AANTAL && f.elementObjectType() != null && f.arguments().size() == 1
                && f.arguments().get(0) instanceof Expr.AttributeReference source && source.attribute() != null
                && f.elementObjectType().attribute(source.attribute()) == null
                && f.elementObjectType().characteristicName(source.attribute()) == null;
    }

    private static boolean sameName(String a, String b) {
        return Namen.norm(a).equals(Namen.norm(b));
    }

    /** Strip a §9.10 "&lt;groep&gt;: " prefix and a §13.4.2.3 " #k" version suffix (and a beslistabel " (n)"
     *  multi-conclusie suffix) off an emitted rule name — the base name the index, a rule scope, and the
     *  transpiler dekking all agree on. */
    public static String baseName(String name) {
        if (name == null) return "";
        String n = name;
        int hash = n.indexOf(" #");
        if (hash > 0) n = n.substring(0, hash);
        int colon = n.indexOf(": ");
        if (colon > 0) n = n.substring(colon + 2);
        n = n.replaceFirst("\\s*\\(\\d+\\)\\s*$", "");
        return n.trim();
    }
}
