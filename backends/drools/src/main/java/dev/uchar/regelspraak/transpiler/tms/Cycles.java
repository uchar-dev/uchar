package dev.uchar.regelspraak.transpiler.tms;

import dev.uchar.regelspraak.graph.ProducerConsumer;
import dev.uchar.regelspraak.refusal.DroolsCode;
import dev.uchar.regelspraak.refusal.Reason;
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
import dev.uchar.regelspraak.ir.ResolvedUniqueness;
import dev.uchar.regelspraak.ir.ResolvedUniquenessUnion;
import dev.uchar.regelspraak.ir.ResolvedAllocation;
import dev.uchar.regelspraak.loader.IrLoader;
import dev.uchar.regelspraak.message.Messages;
import dev.uchar.regelspraak.message.Msg;
import dev.uchar.regelspraak.model.RegelSpraakModel;
import dev.uchar.regelspraak.model.RegelSpraakModel.ObjectType;
import dev.uchar.regelspraak.model.RegelSpraakModel.RoleBindingRef;
import dev.uchar.regelspraak.transpiler.core.Namen;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * The cycles of the dependency graph of IR-SPEC ch. 5, which the exec-model branch refuses before it builds a
 * KieBase: WF-045, a cycle that does not pass through a creation member of a recursive rule group, and WF-046, a
 * negative edge on any cycle, one through such a member included. The verifier refuses both, but this backend does
 * not run the verifier, so a hand-written document brings its cycle here unchecked. Truth maintenance does not settle
 * such a cycle into one answer either. Measured on this branch with the refusal switched off (2026-10-08): a positive
 * cycle derived from an input kept both its characteristics after the input was taken off the bean in a running
 * session, since each was the other's justification; an even cycle of negations played, and the rule first in the
 * model decided which characteristic held; an odd one, of length one and of length two, alternated until the fire
 * cap. A cycle through a recursive creation is admitted: a creation fires only at a fixpoint of the rule layer and
 * what it makes is stated, so the chain advances one generation at a time, and the creation budget, a mint at depth 101
 * in its stratum, bounds a creation that does not stop (RUNTIME-SEMANTICS ch. 5.5, {@code TmsTranspiler.depth}).
 *
 * <p>The graph is the verifier's ({@code spec/verifier/lib/checks.ml}, {@code dep_forms}, {@code wf045_046}), node for
 * node and edge for edge, and not {@link ProducerConsumer}'s, whose member keys are the declaring type and which
 * feeds gate 15 as it is. The nodes are those of ch. 5.1: attribute and characteristic per type as written,
 * channel per rule name, population per type, relation per fact type. A read is literal; a write closes over the
 * subtype chain as ch. 5.1 decides per writer: a form that iterates a type writes the member on every type comparable
 * with it that carries the member, an object creation writes its members and its population on the created type and
 * its ancestors only. Keyed on the declaring type instead, a creation of a supertype was the producer of a read on a
 * subtype it never mints, and the three wrakboeking goldens showed a cycle the verifier does not know. Of the edge
 * kinds of ch. 5.2, WF-046 asks which edges are negative, and the evaluation strata ({@link #strata}), by which the
 * driver orders the creations, ask which are strict or negative.
 *
 * <p>One edge is drawn beyond the verifier: a rule writes the §9.5 channel of its own name. A rule whose value
 * deposits a §6.5 inconsistency fills that channel (RUNTIME-SEMANTICS ch. 5.6), and this branch carries a test of it,
 * where ch. 5.1 keys the channel of a consistency or a uniqueness only. The edge reaches a reader only where such a
 * test passes the lowering, and makes this check stricter than the verifier, never laxer: a test of a rule's channel
 * that closes a cycle is refused here and alternates until the fire cap without the edge. The strata leave the edge
 * out, so that they are the reference interpreter's.
 */
public final class Cycles {
    private Cycles() {}

    /** A read: the node, and whether the edge it draws is negative (ch. 5.2). */
    private record Read(String node, boolean negative) {}

    /** One form of the graph: its kind and name as the refusal names it, the IR node it is built from, whether it is a
     *  creation member of a recursive rule group, whether its writes are irrevocable, and the nodes it writes and
     *  reads. A form with irrevocable writes, a characteristic attribution, a consistency, a uniqueness or a creation,
     *  reads every value that is not a characteristic strictly (the verifier's {@code strict_unless_char},
     *  RUNTIME-SEMANTICS ch. 6.1): a transient it acted on would persist. */
    private record Form(ProducerConsumer.FormKind category, String name, Object source, boolean recursiveCreation,
                        boolean irrevocable, Set<String> writes, List<Read> reads) {}

    /** An edge from the form that writes {@code node} to the form that reads it; {@code strict} where ch. 5.2 makes
     *  the edge strict or negative, so that off a cycle the reader's stratum lies above the writer's. */
    private record Edge(int producer, int consumer, String node, boolean negative, boolean strict) {}

    private static final Comparator<Form> FORM_ORDER =
            Comparator.comparing((Form v) -> v.category().ordinal()).thenComparing(Form::name);

    /** The refusal of the cycles of {@code loaded}'s forms that WF-045 or WF-046 forbids, or null when there is none.
     *  A cycle names its nodes and the forms whose reads and writes close it, both sorted, so every order of the same
     *  forms gives the same sentence. */
    public static Reason refusal(IrLoader.Loaded loaded) {
        Graph g = new Graph(loaded.model(), loaded.prioritizedDefaults(), loaded.navigationBindings(),
                loaded.roleBindings(), loaded.sourcePaths());
        List<Form> graphForms = g.graphForms();
        List<Edge> edges = edges(graphForms);
        List<Msg> cycles = cycles45(g, graphForms, edges);
        if (!cycles.isEmpty())
            return DroolsCode.CYCLE_OUTSIDE_RECURSIVE_CREATION.reason("sentence", cycles.size() == 1
                    ? DroolsCode.CYCLE_OUTSIDE_RECURSIVE_CREATION.part("one", "cycle", cycles.get(0))
                    : DroolsCode.CYCLE_OUTSIDE_RECURSIVE_CREATION.part("several", "count", cycles.size(), "cycles", new Msg.Joined(cycles, "; ")));
        List<Msg> negations = negations46(g, graphForms, edges);
        if (negations.isEmpty()) return null;
        return DroolsCode.NEGATION_ON_CYCLE.reason("sentence", negations.size() == 1
                ? DroolsCode.NEGATION_ON_CYCLE.part("one", "negation", negations.get(0))
                : DroolsCode.NEGATION_ON_CYCLE.part("several", "count", negations.size(), "negations", new Msg.Joined(negations, "; ")));
    }

    /** The order of the cycles and negations of one refusal: by their Dutch text, whatever language the refusal is
     *  written in, so that every order of the same forms gives the same sentence in either language. */
    private static String sortKey(Msg m) {
        return m.text(java.util.Locale.forLanguageTag("nl"));
    }

    /**
     * The evaluation stratum of every form of the document, by its IR node, as the reference interpreter computes it
     * over the verifier's graph ({@code ref/lib/eval.ml}, RUNTIME-SEMANTICS ch. 6.3): the smallest stratum at or above
     * the writers of what the form reads positively and above the writers of what it reads strictly or negatively,
     * except along a cycle, where every edge counts as positive (a cycle WF-045 admits passes through a recursive
     * creation, and its generations settle it). The edge this branch draws beyond the verifier, a rule that writes the
     * channel of its own name, is left out, so the strata are the reference interpreter's and no higher. A document
     * with a forbidden cycle gets strata as well; {@link #refusal} refuses it.
     */
    static Map<Object, Integer> strata(RegelSpraakModel model, List<PrioritizedDefaults> defaults,
                                       Map<Object, List<RoleBindingRef>> navigation, Map<Object, RoleBindingRef> roleBindings,
                                       Map<Object, List<RoleBindingRef>> sourcePaths) {
        List<Form> graphForms = new Graph(model, defaults, navigation, roleBindings, sourcePaths).graphForms();
        int n = graphForms.size();
        List<Edge> edges = new ArrayList<>();
        for (Edge r : edges(graphForms))
            if (!(graphForms.get(r.producer()).category() == ProducerConsumer.FormKind.REGEL && r.node().startsWith("chan:")))
                edges.add(r);
        List<List<Integer>> next = new ArrayList<>();
        for (int i = 0; i < n; i++) next.add(new ArrayList<>());
        for (Edge r : edges) next.get(r.producer()).add(r.consumer());
        boolean[] rises = new boolean[edges.size()];
        for (int i = 0; i < edges.size(); i++) {
            Edge r = edges.get(i);
            rises[i] = r.strict() && !reachable(next, r.consumer(), r.producer());
        }
        int[] stratum = new int[n];
        boolean changed = true;
        for (int ronde = 1; changed; ronde++) {
            // only an edge between two strongly connected components climbs, so the strata settle within n rounds;
            // the reference interpreter faults past n + 2, and so does this
            if (ronde > n + 2) throw new IllegalStateException("exec-model: geen evaluatiestrata na " + (n + 2) + " rondes");
            changed = false;
            for (int i = 0; i < edges.size(); i++) {
                Edge r = edges.get(i);
                int needed = stratum[r.producer()] + (rises[i] ? 1 : 0);
                if (stratum[r.consumer()] < needed) {
                    stratum[r.consumer()] = needed;
                    changed = true;
                }
            }
        }
        Map<Object, Integer> out = new IdentityHashMap<>();
        for (int i = 0; i < n; i++) out.put(graphForms.get(i).source(), stratum[i]);
        return out;
    }

    /** Every edge of the graph: one for every node a form reads and another form, or the same, writes. */
    private static List<Edge> edges(List<Form> graphForms) {
        Map<String, List<Integer>> writers = new HashMap<>();
        for (int i = 0; i < graphForms.size(); i++)
            for (String k : graphForms.get(i).writes()) writers.computeIfAbsent(k, x -> new ArrayList<>()).add(i);
        List<Edge> out = new ArrayList<>();
        for (int c = 0; c < graphForms.size(); c++) {
            Form reader = graphForms.get(c);
            for (Read l : reader.reads()) {
                boolean strict = l.negative() || reader.irrevocable() && !l.node().startsWith("char:");
                for (int p : writers.getOrDefault(l.node(), List.of()))
                    out.add(new Edge(p, c, l.node(), l.negative(), strict));
            }
        }
        return out;
    }

    /** The cycles of the graph without the creation members of recursive rule groups (WF-045): every strongly connected
     *  component of two or more forms, or of one form that reads what it writes, as "over <nodes> via <forms>". */
    private static List<Msg> cycles45(Graph g, List<Form> graphForms, List<Edge> edges) {
        int n = graphForms.size();
        List<List<Edge>> out = new ArrayList<>();
        for (int i = 0; i < n; i++) out.add(new ArrayList<>());
        for (Edge r : edges)
            if (!graphForms.get(r.producer()).recursiveCreation() && !graphForms.get(r.consumer()).recursiveCreation())
                out.get(r.producer()).add(r);
        List<Msg> cycles = new ArrayList<>();
        for (Set<Integer> component : components(n, out)) {
            int first = component.iterator().next();
            boolean selfLoop = out.get(first).stream().anyMatch(r -> r.consumer() == first);
            if (component.size() == 1 && !selfLoop) continue;
            Set<Form> through = new TreeSet<>(FORM_ORDER);
            Set<String> nodes = new LinkedHashSet<>();
            for (int i : component)
                for (Edge r : out.get(i))
                    if (component.contains(r.consumer())) {
                        through.add(graphForms.get(i));
                        through.add(graphForms.get(r.consumer()));
                        nodes.add(r.node());
                    }
            List<Msg> forms = new ArrayList<>();
            for (Form v : through) forms.add(form(v));
            cycles.add(Msg.drools("PART.cycle.item", "nodes", g.nodes(nodes), "forms", enumeration(forms)));
        }
        cycles.sort(Comparator.comparing(Cycles::sortKey));
        return cycles;
    }

    /** The negative edges on a cycle of the whole graph (WF-046): an edge from producer p to consumer c on which p is
     *  reachable from c, as "<consumer> leest <node> van <producer>, dat vanuit <consumer> bereikbaar is". */
    private static List<Msg> negations46(Graph g, List<Form> graphForms, List<Edge> edges) {
        int n = graphForms.size();
        List<List<Integer>> next = new ArrayList<>();
        for (int i = 0; i < n; i++) next.add(new ArrayList<>());
        for (Edge r : edges) next.get(r.producer()).add(r.consumer());
        Map<String, Msg> out = new java.util.TreeMap<>();
        for (Edge r : edges)
            if (r.negative() && reachable(next, r.consumer(), r.producer())) {
                Msg m = Msg.drools("PART.cycle.negation", "consumer", form(graphForms.get(r.consumer())), "node", g.node(r.node()),
                        "producer", form(graphForms.get(r.producer())));
                out.put(sortKey(m), m);
            }
        return new ArrayList<>(out.values());
    }

    private static boolean reachable(List<List<Integer>> next, int van, int destination) {
        Set<Integer> seen = new HashSet<>();
        Deque<Integer> queue = new ArrayDeque<>(List.of(van));
        while (!queue.isEmpty()) {
            int i = queue.pop();
            if (i == destination) return true;
            if (seen.add(i)) queue.addAll(next.get(i));
        }
        return false;
    }

    /** The strongly connected components of the graph over {@code n} forms (Tarjan), each with its forms in index
     *  order. */
    private static List<Set<Integer>> components(int n, List<List<Edge>> out) {
        int[] index = new int[n], lowLink = new int[n];
        java.util.Arrays.fill(index, -1);
        boolean[] onStack = new boolean[n];
        Deque<Integer> stack = new ArrayDeque<>();
        List<Set<Integer>> components = new ArrayList<>();
        int[] counter = {0};
        for (int i = 0; i < n; i++)
            if (index[i] < 0) visit(i, out, index, lowLink, onStack, stack, components, counter);
        return components;
    }

    private static void visit(int k, List<List<Edge>> out, int[] index, int[] lowLink, boolean[] onStack,
                               Deque<Integer> stack, List<Set<Integer>> components, int[] counter) {
        index[k] = lowLink[k] = counter[0]++;
        stack.push(k);
        onStack[k] = true;
        for (Edge r : out.get(k)) {
            int m = r.consumer();
            if (index[m] < 0) {
                visit(m, out, index, lowLink, onStack, stack, components, counter);
                lowLink[k] = Math.min(lowLink[k], lowLink[m]);
            } else if (onStack[m]) {
                lowLink[k] = Math.min(lowLink[k], index[m]);
            }
        }
        if (lowLink[k] == index[k]) {
            Set<Integer> component = new TreeSet<>();
            int m;
            do {
                m = stack.pop();
                onStack[m] = false;
                component.add(m);
            } while (m != k);
            components.add(component);
        }
    }

    private static Msg form(Form v) {
        String category = switch (v.category()) {
            case REGEL -> "rule";
            case OBJECT_CREATION -> "object-creation";
            case ALLOCATION -> "allocation";
            case CONSISTENCY -> "consistency";
            case UNIQUENESS -> "uniqueness";
            case FACT_CREATION -> "fact-creation";
            case DEFAULTS_GROUP -> "defaults-group";
        };
        return Msg.drools("PART.cycle.form." + category, "name", v.name());
    }

    private static Msg.Enumeration enumeration(List<?> parts) {
        return new Msg.Enumeration(dev.uchar.regelspraak.message.Component.DROOLS, parts);
    }

    /** The verifier's {@code strip_version}: a trailing " #&lt;digits&gt;" comes off, with the space before it. */
    private static String withoutVersion(String name) {
        int i = name.lastIndexOf('#');
        if (i >= 2 && name.charAt(i - 1) == ' ' && i + 1 < name.length()
                && name.substring(i + 1).chars().allMatch(c -> c >= '0' && c <= '9'))
            return name.substring(0, i - 1);
        return name;
    }

    /** The graph of one document: its forms, built as the verifier's {@code dep_forms} builds them, and the names its
     *  nodes are shown by. */
    private static final class Graph {
        private final RegelSpraakModel model;
        private final List<PrioritizedDefaults> defaults;
        private final Map<Object, List<RoleBindingRef>> navigation;
        private final Map<Object, RoleBindingRef> roleBindings;
        private final Map<Object, List<RoleBindingRef>> sourcePaths;
        private final Stapels stapels;
        /** How a node is shown, by key: a member as "Type.naam", spelled as declared. */
        private final Map<String, Object> names = new HashMap<>();
        /** The order nodes are listed in: the members first, by type and name, then the other nodes. */
        private final Map<String, String> order = new HashMap<>();

        Graph(RegelSpraakModel model, List<PrioritizedDefaults> defaults, Map<Object, List<RoleBindingRef>> navigation,
              Map<Object, RoleBindingRef> roleBindings, Map<Object, List<RoleBindingRef>> sourcePaths) {
            this.model = model;
            this.defaults = defaults;
            this.navigation = navigation;
            this.roleBindings = roleBindings;
            this.sourcePaths = sourcePaths;
            this.stapels = new Stapels(model, defaults);
        }

        Object node(String k) {
            return names.getOrDefault(k, k);
        }

        /** {@code nodes} listed as the refusal lists them: "het lid X" or "de leden X en Y" where all are members, and
         *  otherwise the node itself or "de knopen X en Y". */
        Msg nodes(Set<String> nodes) {
            List<String> sorted = new ArrayList<>(nodes);
            sorted.sort(Comparator.comparing((String k) -> order.getOrDefault(k, "~" + k)));
            boolean members = sorted.stream().allMatch(k -> k.startsWith("attr:") || k.startsWith("char:"));
            List<Object> shown = sorted.stream().map(this::node).toList();
            if (shown.size() == 1)
                return members ? Msg.drools("PART.cycle.member", "name", shown.get(0)) : Msg.drools("PART.cycle.node", "name", shown.get(0));
            return Msg.drools(members ? "PART.cycle.members" : "PART.cycle.nodes", "names", enumeration(shown));
        }

        // ---------------- nodes ----------------

        private String member(ObjectType ot, String name, boolean characteristic) {
            String key = (characteristic ? "char:" : "attr:") + Namen.norm(ot.name) + ":" + Namen.norm(name);
            String declared = characteristic ? ot.characteristicName(name) : ot.attributeName(name);
            names.putIfAbsent(key, ot.name + "." + (declared != null ? declared : name));
            order.putIfAbsent(key, "0 " + Namen.norm(ot.name) + " " + Namen.norm(name) + " " + (characteristic ? 1 : 0));
            return key;
        }

        private String channel(String rule) {
            String key = "chan:" + Namen.norm(withoutVersion(rule));
            names.putIfAbsent(key, Msg.drools("PART.cycle.channel", "rule", ProducerConsumer.baseName(rule)));
            order.putIfAbsent(key, "1 " + key);
            return key;
        }

        private String population(ObjectType ot) {
            String key = "pop:" + Namen.norm(ot.name);
            names.putIfAbsent(key, Msg.drools("PART.cycle.population", "type", ot.name));
            order.putIfAbsent(key, "2 " + key);
            return key;
        }

        private String relation(String factType) {
            String key = "rel:" + Namen.norm(factType);
            names.putIfAbsent(key, Msg.drools("PART.cycle.relation", "factType", factType));
            order.putIfAbsent(key, "3 " + key);
            return key;
        }

        /** Whether {@code ot} carries member {@code naam}, declared on it or inherited. */
        private static boolean carries(ObjectType ot, String name, boolean characteristic) {
            for (ObjectType t : ot.chain())
                if (characteristic ? t.characteristics.stream().anyMatch(k -> Namen.norm(k).equals(Namen.norm(name)))
                            : t.attributes.keySet().stream().anyMatch(a -> Namen.norm(a).equals(Namen.norm(name))))
                    return true;
            return false;
        }

        /** A member write by a form that iterates {@code ot}: on {@code ot} and every type comparable with it that
         *  carries the member (the verifier's {@code member_writes ~over:`Extension}). */
        private Set<String> extensionWrite(ObjectType ot, String name, boolean characteristic) {
            Set<String> out = new LinkedHashSet<>();
            out.add(member(ot, name, characteristic));
            for (ObjectType s : model.objectTypes.values())
                if ((s.isA(ot) || ot.isA(s)) && carries(s, name, characteristic)) out.add(member(s, name, characteristic));
            return out;
        }

        /** A member write by an object creation of {@code ot}: on {@code ot} and its ancestors that carry the member
         *  (the verifier's {@code member_writes ~over:`Minted}), since creation mints exactly {@code ot}. */
        private Set<String> mintWrite(ObjectType ot, String name, boolean characteristic) {
            Set<String> out = new LinkedHashSet<>();
            out.add(member(ot, name, characteristic));
            for (ObjectType t : ot.chain()) if (carries(t, name, characteristic)) out.add(member(t, name, characteristic));
            return out;
        }

        // ---------------- the forms ----------------

        List<Form> graphForms() {
            Set<String> recursive = new HashSet<>();
            for (ResolvedRuleGroup g : model.ruleGroups)
                if (g.recursive()) for (String member : g.members()) if (member != null) recursive.add(Namen.norm(withoutVersion(member)));
            List<Form> out = new ArrayList<>();
            for (ResolvedRule r : model.resolvedRules) {
                if (!(r.target() instanceof Expr.AttributeReference d) || d.objectType() == null || d.attribute() == null) continue;
                boolean characteristic = r.category() == ResolvedRule.Kind.KENMERKTOEKENNING;
                Set<String> writes = extensionWrite(d.objectType(), d.attribute(), characteristic);
                if (r.name() != null) writes.add(channel(r.name()));
                List<Read> reads = new ArrayList<>();
                condition(r.condition(), d.objectType(), false, reads);
                expr(r.value(), d.objectType(), reads);
                if (!characteristic && !stapels.absorbedOn(d.objectType(), d.attribute()).isEmpty()) reads = negative(reads);
                out.add(new Form(ProducerConsumer.FormKind.REGEL, ProducerConsumer.baseName(r.name()), r, false,
                        characteristic, writes, reads));
            }
            for (ResolvedAllocation v : model.allocations) {
                Set<String> writes = new LinkedHashSet<>();
                if (v.targetType() != null && v.targetAttribute() != null)
                    writes.addAll(extensionWrite(v.targetType(), v.targetAttribute(), false));
                if (v.sourceType() != null && v.remainderAttribute() != null)
                    writes.addAll(extensionWrite(v.sourceType(), v.remainderAttribute(), false));
                List<Read> reads = new ArrayList<>();
                if (v.targetType() != null) reads.add(new Read(population(v.targetType()), false));
                RoleBindingRef rb = roleBindings.get(v);
                if (rb != null && rb.factType() != null) reads.add(new Read(relation(rb.factType()), false));
                if (v.sourceType() != null && v.sourceAttribute() != null)
                    reads.add(new Read(member(v.sourceType(), v.sourceAttribute(), false), false));
                if (v.targetType() != null)
                    for (String a : new String[] {v.ratioAttribute(), v.maximumAttribute(), v.orderingAttribute()})
                        if (a != null) reads.add(new Read(member(v.targetType(), a, false), false));
                condition(v.condition(), v.sourceType(), false, reads);
                out.add(new Form(ProducerConsumer.FormKind.ALLOCATION, ProducerConsumer.baseName(v.name()), v, false,
                        false, writes, reads));
            }
            for (ResolvedConsistency c : model.consistencies) {
                List<Read> reads = new ArrayList<>();
                condition(c.assertion(), c.subject(), false, reads);
                out.add(new Form(ProducerConsumer.FormKind.CONSISTENCY, ProducerConsumer.baseName(c.name()), c,
                        false, true, c.name() == null ? Set.of() : Set.of(channel(c.name())), reads));
            }
            for (ResolvedUniqueness u : model.uniquenesses) {
                List<Read> reads = new ArrayList<>();
                if (u.scopeBinding() != null && u.scopeBinding().factType() != null)
                    reads.add(new Read(relation(u.scopeBinding().factType()), false));
                if (u.subject() != null)
                    for (String k : u.keys()) if (k != null) reads.add(new Read(member(u.subject(), k, false), false));
                out.add(new Form(ProducerConsumer.FormKind.UNIQUENESS, ProducerConsumer.baseName(u.name()), u,
                        false, true, u.name() == null ? Set.of() : Set.of(channel(u.name())), reads));
            }
            for (ResolvedUniquenessUnion u : model.uniquenessUnions) {
                List<Read> reads = new ArrayList<>();
                for (ResolvedUniquenessUnion.Source b : u.sources())
                    if (b.type() != null)
                        for (String k : b.keys()) if (k != null) reads.add(new Read(member(b.type(), k, false), false));
                out.add(new Form(ProducerConsumer.FormKind.UNIQUENESS, ProducerConsumer.baseName(u.name()), u,
                        false, true, u.name() == null ? Set.of() : Set.of(channel(u.name())), reads));
            }
            for (ResolvedObjectCreation o : model.objectCreations) {
                Set<String> writes = new LinkedHashSet<>();
                if (o.createdType() != null) {
                    writes.add(population(o.createdType()));
                    for (ObjectType t : o.createdType().chain()) writes.add(population(t));
                }
                String backLinkRelation = backLink(o);
                if (backLinkRelation != null) writes.add(relation(backLinkRelation));
                if (o.createdType() != null)
                    for (ResolvedObjectCreation.MemberInit t : o.initialisations())
                        if (t.attribute() != null) writes.addAll(mintWrite(o.createdType(), t.attribute(), t.characteristic()));
                List<Read> reads = new ArrayList<>();
                if (o.subject() != null) reads.add(new Read(population(o.subject()), false));
                condition(o.condition(), o.subject(), false, reads);
                for (ResolvedObjectCreation.MemberInit t : o.initialisations()) expr(t.value(), o.subject(), reads);
                String name = o.name() == null ? "" : o.name();
                out.add(new Form(ProducerConsumer.FormKind.OBJECT_CREATION, ProducerConsumer.baseName(o.name()), o,
                        recursive.contains(Namen.norm(withoutVersion(name))), true, writes, reads));
            }
            for (ResolvedFactCreation f : model.factCreations) {
                Set<String> writes = new LinkedHashSet<>();
                if (f.factType() != null) writes.add(relation(f.factType()));
                List<Read> reads = new ArrayList<>();
                for (ResolvedFactCreation.PathStep t : f.pathSteps())
                    if (t.roleBinding() != null && t.roleBinding().factType() != null)
                        reads.add(new Read(relation(t.roleBinding().factType()), false));
                if (f.path() != null && f.path().factType() != null)
                    reads.add(new Read(relation(f.path().factType()), false));
                condition(f.condition(), f.anchor(), false, reads);
                String name = f.name() == null ? "" : f.name();
                out.add(new Form(ProducerConsumer.FormKind.FACT_CREATION, ProducerConsumer.baseName(f.name()), f,
                        recursive.contains(Namen.norm(withoutVersion(name))), true, writes, reads));
            }
            for (int i = 0; i < defaults.size(); i++) {
                PrioritizedDefaults p = defaults.get(i);
                ObjectType subjectType = model.objectTypeByName(p.subject());
                Set<String> writes = new LinkedHashSet<>();
                if (subjectType != null && p.target() != null)
                    writes.addAll(extensionWrite(subjectType, p.target(), p.isCharacteristic()));
                List<Read> reads = new ArrayList<>();
                for (Default d : p.defaults()) defaultReads(d, subjectType, reads);
                out.add(new Form(ProducerConsumer.FormKind.DEFAULTS_GROUP,
                        p.name() != null ? ProducerConsumer.baseName(p.name()) : "/rules/prioritizedDefaults/" + i, p,
                        false, false, writes, negative(reads)));
            }
            return out;
        }

        /** The relation an object creation writes as its back-link: its {@code connectingRole} binding's, else the one
         *  fact type relating its subject and created type, none for a self-typed two-role pair, which the derivation
         *  leaves unlinked (the verifier's reading of the by-pair route). */
        private String backLink(ResolvedObjectCreation o) {
            RoleBindingRef rb = roleBindings.get(o);
            if (rb != null) return rb.factType();
            ObjectType s = o.subject(), n = o.createdType();
            if (s == null || n == null) return null;
            List<RegelSpraakModel.FactType> candidates = new ArrayList<>();
            for (RegelSpraakModel.FactType ft : model.factTypes.values()) {
                List<RegelSpraakModel.Role> r = ft.roles;
                if (r.size() == 2) {
                    boolean ab = s.name.equalsIgnoreCase(r.get(0).objectTypeName())
                            && n.name.equalsIgnoreCase(r.get(1).objectTypeName());
                    boolean ba = s.name.equalsIgnoreCase(r.get(1).objectTypeName())
                            && n.name.equalsIgnoreCase(r.get(0).objectTypeName());
                    if (ab || ba) candidates.add(ft);
                } else if (r.size() == 1 && ft.reciprocal && s == n && s.name.equalsIgnoreCase(r.get(0).objectTypeName())) {
                    candidates.add(ft);
                }
            }
            if (candidates.size() != 1) return null;
            RegelSpraakModel.FactType ft = candidates.get(0);
            return s == n && !ft.reciprocal ? null : ft.name;
        }

        private void defaultReads(Default d, ObjectType subjectType, List<Read> out) {
            condition(d.condition(), subjectType, false, out);
            expr(d.consequence(), subjectType, out);
            if (d.exceptions() == null) return;
            for (ExceptionGroup g : d.exceptions().members())
                for (Default member : g.defaults()) defaultReads(member, subjectType, out);
        }

        private static List<Read> negative(List<Read> l) {
            List<Read> out = new ArrayList<>();
            for (Read x : l) out.add(new Read(x.node(), true));
            return out;
        }

        // ---------------- the reads ----------------

        /** The verifier's {@code dep_expr}: every expression node, so a node the IR records gain later fails to compile
         *  here until it is decided what it reads. */
        private void expr(Expr e, ObjectType subject, List<Read> out) {
            if (e == null) return;
            switch (e) {
                case Expr.AttributeReference a -> {
                    if (a.objectType() == null || a.attribute() == null) return;
                    for (RoleBindingRef hop : navigation.getOrDefault(a, List.of()))
                        if (hop.factType() != null) out.add(new Read(relation(hop.factType()), false));
                    out.add(new Read(member(a.objectType(), a.attribute(), false), false));
                }
                case Expr.Binary b -> { expr(b.left(), subject, out); expr(b.right(), subject, out); }
                case Expr.FunctionCall f -> {
                    ObjectType el = f.elementObjectType();
                    if (el != null) out.add(new Read(population(el), false));
                    List<RoleBindingRef> path = sourcePaths.get(f);
                    if (path != null)
                        for (RoleBindingRef hop : path) if (hop.factType() != null) out.add(new Read(relation(hop.factType()), false));
                    RoleBindingRef role = roleBindings.get(f);
                    if (role != null && role.factType() != null) out.add(new Read(relation(role.factType()), false));
                    // the count source names the relation's role, not a member: it reads the link set and the
                    // population drawn above, never a member of the contributor
                    boolean countSource = f.kind() == Expr.FunctionKind.AANTAL && el != null && f.arguments().size() == 1
                            && f.arguments().get(0) instanceof Expr.AttributeReference source && source.attribute() != null
                            && (path != null || role != null)
                            && !carries(el, source.attribute(), false) && !carries(el, source.attribute(), true);
                    if (!countSource) for (Expr a : f.arguments()) expr(a, subject, out);
                    expr(f.emptyFallback(), subject, out);
                    condition(f.filter(), el != null ? el : subject, false, out);
                }
                case Expr.InstanceSetUnion u -> {
                    for (Expr.InstanceSetUnion.Branch t : u.branches()) {
                        if (t.elementType() != null) out.add(new Read(population(t.elementType()), false));
                        if (t.sourceRole() != null && t.sourceRole().factType() != null)
                            out.add(new Read(relation(t.sourceRole().factType()), false));
                        condition(t.filter(), t.elementType(), false, out);
                    }
                }
                case Expr.Rounding a -> expr(a.base(), subject, out);
                case Expr.Clamp b -> {
                    expr(b.base(), subject, out);
                    expr(b.minimum(), subject, out);
                    expr(b.maximum(), subject, out);
                }
                case Expr.DimensionCellRead c -> expr(c.attribute(), subject, out);
                case Expr.TextComposition t -> {
                    for (Expr.TextComposition.Part d : t.parts())
                        if (d instanceof Expr.TextComposition.EmbeddedExpression i) expr(i.expr(), subject, out);
                }
                case Expr.Let l -> { expr(l.value(), subject, out); expr(l.body(), subject, out); }
                case Expr.IfThenElse i -> {
                    condition(i.condition(), subject, false, out);
                    expr(i.then(), subject, out);
                    expr(i.otherwise(), subject, out);
                }
                case Expr.Literal ignored -> { }
                case Expr.ParameterRef ignored -> { }
                case Expr.VariableRef ignored -> { }
                case Expr.BinderRef ignored -> { }
                case Expr.Constant ignored -> { }
                case Expr.Unresolved ignored -> { }
            }
        }

        /** The verifier's {@code dep_cond}: {@code neg} is the negation-like context a characteristic read under it
         *  takes; a leeg or gevuld test makes every read under it negative, in either polarity; a quantifier fold reads
         *  its range negatively under all, none, atMost and exactly and its matrix under none, atMost and exactly. */
        private void condition(Condition c, ObjectType subject, boolean neg, List<Read> out) {
            if (c == null) return;
            switch (c) {
                case Condition.Comparison v -> { expr(v.left(), subject, out); expr(v.right(), subject, out); }
                case Condition.Predicate p -> {
                    if (p.category() == Condition.PredicateKind.LEEG || p.category() == Condition.PredicateKind.GEVULD) {
                        List<Read> under = new ArrayList<>();
                        expr(p.operand(), subject, under);
                        out.addAll(negative(under));
                    } else {
                        expr(p.operand(), subject, out);
                    }
                }
                case Condition.CharacteristicCheck k -> {
                    if (k.characteristic() == null) return;
                    boolean negative = neg || k.negation();
                    List<RoleBindingRef> path = navigation.get(k);
                    if (path != null && !path.isEmpty()) {
                        for (RoleBindingRef hop : path)
                            if (hop.factType() != null) out.add(new Read(relation(hop.factType()), false));
                        ObjectType reaches = filler(path.get(path.size() - 1));
                        if (reaches != null) out.add(new Read(member(reaches, k.characteristic(), true), negative));
                        return;
                    }
                    ObjectType carrier = k.owner() != null ? k.owner() : subject;
                    if (carrier != null) out.add(new Read(member(carrier, k.characteristic(), true), negative));
                }
                case Condition.RoleMembership r -> {
                    RoleBindingRef rb = roleBindings.get(r);
                    if (rb != null && rb.factType() != null) out.add(new Read(relation(rb.factType()), neg || r.negation()));
                }
                // a role check the loader records without a binding: no relation of its own to name
                case Condition.FarRoleMembership ignored -> { }
                case Condition.TimeWindow p -> { expr(p.from(), subject, out); expr(p.until(), subject, out); }
                case Condition.Compound s -> {
                    boolean under = switch (s.quantifier()) {
                        case GEEN, TEN_HOOGSTE, PRECIES -> true;
                        default -> neg;
                    };
                    for (Condition d : s.components()) condition(d, subject, under, out);
                }
                case Condition.QuantifierFold kv -> {
                    boolean rangeNeg = neg || kv.quantifier() != Condition.Quantifier.TEN_MINSTE;
                    boolean matrixNeg = neg || !(kv.quantifier() == Condition.Quantifier.TEN_MINSTE
                            || kv.quantifier() == Condition.Quantifier.ALLE);
                    List<RoleBindingRef> range = new ArrayList<>();
                    if (kv.sourceRole() != null) range.add(kv.sourceRole());
                    if (kv.sourcePath() != null) range.addAll(kv.sourcePath());
                    for (RoleBindingRef hop : range)
                        if (hop.factType() != null) out.add(new Read(relation(hop.factType()), rangeNeg));
                    part(kv.filter(), kv.elementObjectType(), rangeNeg, out);
                    part(kv.test(), kv.elementObjectType(), matrixNeg, out);
                }
                case Condition.RuleStatusCheck rv -> {
                    if (rv.rule() != null) out.add(new Read(channel(rv.rule()), true));
                }
                case Condition.Unresolved ignored -> { }
            }
        }

        /** A part of a quantifier fold: negative throughout where {@code negative}, its own kinds otherwise. */
        private void part(Condition c, ObjectType subject, boolean negative, List<Read> out) {
            List<Read> under = new ArrayList<>();
            condition(c, subject, negative, under);
            out.addAll(negative ? negative(under) : under);
        }

        /** The declared filler of the role a hop binds: the type it reaches. */
        private ObjectType filler(RoleBindingRef hop) {
            for (RegelSpraakModel.FactType ft : model.factTypes.values())
                if (Namen.norm(ft.name).equals(Namen.norm(hop.factType())))
                    for (RegelSpraakModel.Role r : ft.roles)
                        if (r.name() != null && Namen.norm(r.name()).equals(Namen.norm(hop.role())))
                            return model.objectTypeByName(r.objectTypeName());
            return null;
        }
    }
}
