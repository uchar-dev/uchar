package dev.uchar.regelspraak.transpiler.tms;

import dev.uchar.regelspraak.graaf.ProducentConsument;
import dev.uchar.regelspraak.ir.Conditie;
import dev.uchar.regelspraak.ir.Default;
import dev.uchar.regelspraak.ir.ExceptionGroup;
import dev.uchar.regelspraak.ir.Expr;
import dev.uchar.regelspraak.ir.PrioritizedDefaults;
import dev.uchar.regelspraak.ir.ResolvedConsistency;
import dev.uchar.regelspraak.ir.ResolvedFactCreation;
import dev.uchar.regelspraak.ir.ResolvedObjectCreation;
import dev.uchar.regelspraak.ir.ResolvedRegel;
import dev.uchar.regelspraak.ir.ResolvedRegelGroep;
import dev.uchar.regelspraak.ir.ResolvedUniqueness;
import dev.uchar.regelspraak.ir.ResolvedUniquenessUnion;
import dev.uchar.regelspraak.ir.ResolvedVerdeling;
import dev.uchar.regelspraak.loader.IrLoader;
import dev.uchar.regelspraak.model.RegelSpraakModel;
import dev.uchar.regelspraak.model.RegelSpraakModel.Objecttype;
import dev.uchar.regelspraak.model.RegelSpraakModel.RoleBindingRef;
import dev.uchar.regelspraak.transpiler.kern.Namen;

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
 * node and edge for edge, and not {@link ProducentConsument}'s, whose member keys are the declaring type and which
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
    private record Vorm(ProducentConsument.Vormsoort soort, String naam, Object bron, boolean recursiveCreation,
                        boolean irrevocable, Set<String> writes, List<Read> leest) {}

    /** An edge from the form that writes {@code node} to the form that reads it; {@code strict} where ch. 5.2 makes
     *  the edge strict or negative, so that off a cycle the reader's stratum lies above the writer's. */
    private record Edge(int producer, int consumer, String node, boolean negative, boolean strict) {}

    private static final Comparator<Vorm> FORM_ORDER =
            Comparator.comparing((Vorm v) -> v.soort().ordinal()).thenComparing(Vorm::naam);

    /** The refusal of the cycles of {@code loaded}'s forms that WF-045 or WF-046 forbids, or null when there is none.
     *  A cycle names its nodes and the forms whose reads and writes close it, both sorted, so every order of the same
     *  forms gives the same sentence. */
    public static String refusal(IrLoader.Loaded loaded) {
        Graph g = new Graph(loaded.model(), loaded.prioritizedDefaults(), loaded.navigationBindings(),
                loaded.roleBindings(), loaded.sourcePaths());
        List<Vorm> vormen = g.vormen();
        List<Edge> edges = edges(vormen);
        List<String> cycles = cycles45(g, vormen, edges);
        if (!cycles.isEmpty())
            return "exec-model: " + (cycles.size() == 1 ? "kring in de afhankelijkheden " + cycles.get(0)
                    : cycles.size() + " kringen in de afhankelijkheden: " + String.join("; ", cycles))
                    + " — een kring die niet door een creatielid van een recursieve regelgroep loopt, is onwelgevormd"
                    + " (WF-045), en truth maintenance geeft er geen eenduidig fixpunt voor: een positieve kring houdt"
                    + " zichzelf in stand nadat haar ingang vervalt, een even ontkenningskring beslist op de volgorde van"
                    + " de regels in het model, en een oneven ontkenningskring wisselt tot het vuurplafond";
        List<String> negations = negations46(g, vormen, edges);
        if (negations.isEmpty()) return null;
        return "exec-model: " + (negations.size() == 1 ? "een ontkennende afhankelijkheid op een kring: " + negations.get(0)
                : negations.size() + " ontkennende afhankelijkheden op een kring: " + String.join("; ", negations))
                + " — geen kring mag een ontkenning dragen, ook niet een kring door een creatielid van een recursieve"
                + " regelgroep (WF-046): de creatie handelt dan op een afwezigheid die de kring daarna vult";
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
        List<Vorm> vormen = new Graph(model, defaults, navigation, roleBindings, sourcePaths).vormen();
        int n = vormen.size();
        List<Edge> edges = new ArrayList<>();
        for (Edge r : edges(vormen))
            if (!(vormen.get(r.producer()).soort() == ProducentConsument.Vormsoort.REGEL && r.node().startsWith("chan:")))
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
        for (int i = 0; i < n; i++) out.put(vormen.get(i).bron(), stratum[i]);
        return out;
    }

    /** Every edge of the graph: one for every node a form reads and another form, or the same, writes. */
    private static List<Edge> edges(List<Vorm> vormen) {
        Map<String, List<Integer>> writers = new HashMap<>();
        for (int i = 0; i < vormen.size(); i++)
            for (String k : vormen.get(i).writes()) writers.computeIfAbsent(k, x -> new ArrayList<>()).add(i);
        List<Edge> out = new ArrayList<>();
        for (int c = 0; c < vormen.size(); c++) {
            Vorm reader = vormen.get(c);
            for (Read l : reader.leest()) {
                boolean strict = l.negative() || reader.irrevocable() && !l.node().startsWith("char:");
                for (int p : writers.getOrDefault(l.node(), List.of()))
                    out.add(new Edge(p, c, l.node(), l.negative(), strict));
            }
        }
        return out;
    }

    /** The cycles of the graph without the creation members of recursive rule groups (WF-045): every strongly connected
     *  component of two or more forms, or of one form that reads what it writes, as "over <nodes> via <forms>". */
    private static List<String> cycles45(Graph g, List<Vorm> vormen, List<Edge> edges) {
        int n = vormen.size();
        List<List<Edge>> out = new ArrayList<>();
        for (int i = 0; i < n; i++) out.add(new ArrayList<>());
        for (Edge r : edges)
            if (!vormen.get(r.producer()).recursiveCreation() && !vormen.get(r.consumer()).recursiveCreation())
                out.get(r.producer()).add(r);
        List<String> cycles = new ArrayList<>();
        for (Set<Integer> component : components(n, out)) {
            int eerste = component.iterator().next();
            boolean selfLoop = out.get(eerste).stream().anyMatch(r -> r.consumer() == eerste);
            if (component.size() == 1 && !selfLoop) continue;
            Set<Vorm> through = new TreeSet<>(FORM_ORDER);
            Set<String> nodes = new LinkedHashSet<>();
            for (int i : component)
                for (Edge r : out.get(i))
                    if (component.contains(r.consumer())) {
                        through.add(vormen.get(i));
                        through.add(vormen.get(r.consumer()));
                        nodes.add(r.node());
                    }
            List<String> forms = new ArrayList<>();
            for (Vorm v : through) forms.add(vorm(v));
            cycles.add("over " + g.nodes(nodes) + " via " + enumeration(forms));
        }
        cycles.sort(null);
        return cycles;
    }

    /** The negative edges on a cycle of the whole graph (WF-046): an edge from producer p to consumer c on which p is
     *  reachable from c, as "<consumer> leest <node> van <producer>, dat vanuit <consumer> bereikbaar is". */
    private static List<String> negations46(Graph g, List<Vorm> vormen, List<Edge> edges) {
        int n = vormen.size();
        List<List<Integer>> next = new ArrayList<>();
        for (int i = 0; i < n; i++) next.add(new ArrayList<>());
        for (Edge r : edges) next.get(r.producer()).add(r.consumer());
        Set<String> out = new TreeSet<>();
        for (Edge r : edges)
            if (r.negative() && reachable(next, r.consumer(), r.producer())) {
                String c = vorm(vormen.get(r.consumer())), p = vorm(vormen.get(r.producer()));
                out.add(c + " leest " + g.node(r.node()) + " ontkennend van " + p + ", dat vanuit " + c
                        + " bereikbaar is");
            }
        return new ArrayList<>(out);
    }

    private static boolean reachable(List<List<Integer>> next, int van, int naar) {
        Set<Integer> seen = new HashSet<>();
        Deque<Integer> queue = new ArrayDeque<>(List.of(van));
        while (!queue.isEmpty()) {
            int i = queue.pop();
            if (i == naar) return true;
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

    private static String vorm(Vorm v) {
        String soort = switch (v.soort()) {
            case REGEL -> "de regel";
            case OBJECTCREATIE -> "de objectcreatie";
            case VERDELING -> "de verdeling";
            case CONSISTENTIE -> "de consistentieregel";
            case UNICITEIT -> "de uniciteitsregel";
            case FEITCREATIE -> "de feitcreatie";
            case DEFAULTSGROEP -> "de defaultsgroep";
        };
        return soort + " \"" + v.naam() + "\"";
    }

    private static String enumeration(List<String> parts) {
        if (parts.size() == 1) return parts.get(0);
        return String.join(", ", parts.subList(0, parts.size() - 1)) + " en " + parts.get(parts.size() - 1);
    }

    /** The verifier's {@code strip_version}: a trailing " #&lt;digits&gt;" comes off, with the space before it. */
    private static String withoutVersion(String naam) {
        int i = naam.lastIndexOf('#');
        if (i >= 2 && naam.charAt(i - 1) == ' ' && i + 1 < naam.length()
                && naam.substring(i + 1).chars().allMatch(c -> c >= '0' && c <= '9'))
            return naam.substring(0, i - 1);
        return naam;
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
        private final Map<String, String> names = new HashMap<>();
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

        String node(String k) {
            return names.getOrDefault(k, k);
        }

        /** {@code nodes} listed as the refusal lists them: "het lid X" or "de leden X en Y" where all are members, and
         *  otherwise the node itself or "de knopen X en Y". */
        String nodes(Set<String> nodes) {
            List<String> sorted = new ArrayList<>(nodes);
            sorted.sort(Comparator.comparing((String k) -> order.getOrDefault(k, "~" + k)));
            boolean members = sorted.stream().allMatch(k -> k.startsWith("attr:") || k.startsWith("char:"));
            List<String> shown = sorted.stream().map(this::node).toList();
            if (shown.size() == 1) return (members ? "het lid " : "") + shown.get(0);
            return (members ? "de leden " : "de knopen ") + enumeration(shown);
        }

        // ---------------- nodes ----------------

        private String member(Objecttype ot, String naam, boolean kenmerk) {
            String sleutel = (kenmerk ? "char:" : "attr:") + Namen.norm(ot.naam) + ":" + Namen.norm(naam);
            String declared = kenmerk ? ot.kenmerkNaam(naam) : ot.attributeName(naam);
            names.putIfAbsent(sleutel, ot.naam + "." + (declared != null ? declared : naam));
            order.putIfAbsent(sleutel, "0 " + Namen.norm(ot.naam) + " " + Namen.norm(naam) + " " + (kenmerk ? 1 : 0));
            return sleutel;
        }

        private String channel(String regel) {
            String sleutel = "chan:" + Namen.norm(withoutVersion(regel));
            names.putIfAbsent(sleutel, "het kanaal van \"" + ProducentConsument.basisNaam(regel) + "\"");
            order.putIfAbsent(sleutel, "1 " + sleutel);
            return sleutel;
        }

        private String population(Objecttype ot) {
            String sleutel = "pop:" + Namen.norm(ot.naam);
            names.putIfAbsent(sleutel, "de populatie van " + ot.naam);
            order.putIfAbsent(sleutel, "2 " + sleutel);
            return sleutel;
        }

        private String relation(String feittype) {
            String sleutel = "rel:" + Namen.norm(feittype);
            names.putIfAbsent(sleutel, "de relatie " + feittype);
            order.putIfAbsent(sleutel, "3 " + sleutel);
            return sleutel;
        }

        /** Whether {@code ot} carries member {@code naam}, declared on it or inherited. */
        private static boolean carries(Objecttype ot, String naam, boolean kenmerk) {
            for (Objecttype t : ot.keten())
                if (kenmerk ? t.kenmerken.stream().anyMatch(k -> Namen.norm(k).equals(Namen.norm(naam)))
                            : t.attributen.keySet().stream().anyMatch(a -> Namen.norm(a).equals(Namen.norm(naam))))
                    return true;
            return false;
        }

        /** A member write by a form that iterates {@code ot}: on {@code ot} and every type comparable with it that
         *  carries the member (the verifier's {@code member_writes ~over:`Extension}). */
        private Set<String> extensionWrite(Objecttype ot, String naam, boolean kenmerk) {
            Set<String> out = new LinkedHashSet<>();
            out.add(member(ot, naam, kenmerk));
            for (Objecttype s : model.objecttypen.values())
                if ((s.isEen(ot) || ot.isEen(s)) && carries(s, naam, kenmerk)) out.add(member(s, naam, kenmerk));
            return out;
        }

        /** A member write by an object creation of {@code ot}: on {@code ot} and its ancestors that carry the member
         *  (the verifier's {@code member_writes ~over:`Minted}), since creation mints exactly {@code ot}. */
        private Set<String> mintWrite(Objecttype ot, String naam, boolean kenmerk) {
            Set<String> out = new LinkedHashSet<>();
            out.add(member(ot, naam, kenmerk));
            for (Objecttype t : ot.keten()) if (carries(t, naam, kenmerk)) out.add(member(t, naam, kenmerk));
            return out;
        }

        // ---------------- the forms ----------------

        List<Vorm> vormen() {
            Set<String> recursive = new HashSet<>();
            for (ResolvedRegelGroep g : model.regelGroepen)
                if (g.recursive()) for (String member : g.leden()) if (member != null) recursive.add(Namen.norm(withoutVersion(member)));
            List<Vorm> out = new ArrayList<>();
            for (ResolvedRegel r : model.resolvedRegels) {
                if (!(r.doel() instanceof Expr.AttribuutRef d) || d.objecttype() == null || d.attribuut() == null) continue;
                boolean kenmerk = r.soort() == ResolvedRegel.Soort.KENMERKTOEKENNING;
                Set<String> writes = extensionWrite(d.objecttype(), d.attribuut(), kenmerk);
                if (r.naam() != null) writes.add(channel(r.naam()));
                List<Read> leest = new ArrayList<>();
                condition(r.voorwaarde(), d.objecttype(), false, leest);
                expr(r.waarde(), d.objecttype(), leest);
                if (!kenmerk && !stapels.absorbedOn(d.objecttype(), d.attribuut()).isEmpty()) leest = negative(leest);
                out.add(new Vorm(ProducentConsument.Vormsoort.REGEL, ProducentConsument.basisNaam(r.naam()), r, false,
                        kenmerk, writes, leest));
            }
            for (ResolvedVerdeling v : model.verdelingen) {
                Set<String> writes = new LinkedHashSet<>();
                if (v.targetType() != null && v.targetAttribute() != null)
                    writes.addAll(extensionWrite(v.targetType(), v.targetAttribute(), false));
                if (v.sourceType() != null && v.remainderAttribute() != null)
                    writes.addAll(extensionWrite(v.sourceType(), v.remainderAttribute(), false));
                List<Read> leest = new ArrayList<>();
                if (v.targetType() != null) leest.add(new Read(population(v.targetType()), false));
                RoleBindingRef rb = roleBindings.get(v);
                if (rb != null && rb.feittype() != null) leest.add(new Read(relation(rb.feittype()), false));
                if (v.sourceType() != null && v.sourceAttribute() != null)
                    leest.add(new Read(member(v.sourceType(), v.sourceAttribute(), false), false));
                if (v.targetType() != null)
                    for (String a : new String[] {v.ratioAttribute(), v.maximumAttribute(), v.orderingAttribute()})
                        if (a != null) leest.add(new Read(member(v.targetType(), a, false), false));
                condition(v.voorwaarde(), v.sourceType(), false, leest);
                out.add(new Vorm(ProducentConsument.Vormsoort.VERDELING, ProducentConsument.basisNaam(v.naam()), v, false,
                        false, writes, leest));
            }
            for (ResolvedConsistency c : model.consistenties) {
                List<Read> leest = new ArrayList<>();
                condition(c.assertie(), c.subject(), false, leest);
                out.add(new Vorm(ProducentConsument.Vormsoort.CONSISTENTIE, ProducentConsument.basisNaam(c.naam()), c,
                        false, true, c.naam() == null ? Set.of() : Set.of(channel(c.naam())), leest));
            }
            for (ResolvedUniqueness u : model.uniciteiten) {
                List<Read> leest = new ArrayList<>();
                if (u.scopeBinding() != null && u.scopeBinding().feittype() != null)
                    leest.add(new Read(relation(u.scopeBinding().feittype()), false));
                if (u.subject() != null)
                    for (String k : u.keys()) if (k != null) leest.add(new Read(member(u.subject(), k, false), false));
                out.add(new Vorm(ProducentConsument.Vormsoort.UNICITEIT, ProducentConsument.basisNaam(u.naam()), u,
                        false, true, u.naam() == null ? Set.of() : Set.of(channel(u.naam())), leest));
            }
            for (ResolvedUniquenessUnion u : model.uniciteitUnies) {
                List<Read> leest = new ArrayList<>();
                for (ResolvedUniquenessUnion.Bron b : u.sources())
                    if (b.type() != null)
                        for (String k : b.keys()) if (k != null) leest.add(new Read(member(b.type(), k, false), false));
                out.add(new Vorm(ProducentConsument.Vormsoort.UNICITEIT, ProducentConsument.basisNaam(u.naam()), u,
                        false, true, u.naam() == null ? Set.of() : Set.of(channel(u.naam())), leest));
            }
            for (ResolvedObjectCreation o : model.objectCreaties) {
                Set<String> writes = new LinkedHashSet<>();
                if (o.createdType() != null) {
                    writes.add(population(o.createdType()));
                    for (Objecttype t : o.createdType().keten()) writes.add(population(t));
                }
                String backLinkRelation = backLink(o);
                if (backLinkRelation != null) writes.add(relation(backLinkRelation));
                if (o.createdType() != null)
                    for (ResolvedObjectCreation.MemberInit t : o.initialisations())
                        if (t.attribuut() != null) writes.addAll(mintWrite(o.createdType(), t.attribuut(), t.kenmerk()));
                List<Read> leest = new ArrayList<>();
                if (o.subject() != null) leest.add(new Read(population(o.subject()), false));
                condition(o.voorwaarde(), o.subject(), false, leest);
                for (ResolvedObjectCreation.MemberInit t : o.initialisations()) expr(t.waarde(), o.subject(), leest);
                String naam = o.naam() == null ? "" : o.naam();
                out.add(new Vorm(ProducentConsument.Vormsoort.OBJECTCREATIE, ProducentConsument.basisNaam(o.naam()), o,
                        recursive.contains(Namen.norm(withoutVersion(naam))), true, writes, leest));
            }
            for (ResolvedFactCreation f : model.feitCreaties) {
                Set<String> writes = new LinkedHashSet<>();
                if (f.feittype() != null) writes.add(relation(f.feittype()));
                List<Read> leest = new ArrayList<>();
                for (ResolvedFactCreation.PathStep t : f.pathSteps())
                    if (t.roleBinding() != null && t.roleBinding().feittype() != null)
                        leest.add(new Read(relation(t.roleBinding().feittype()), false));
                if (f.path() != null && f.path().feittype() != null)
                    leest.add(new Read(relation(f.path().feittype()), false));
                condition(f.voorwaarde(), f.anchor(), false, leest);
                String naam = f.naam() == null ? "" : f.naam();
                out.add(new Vorm(ProducentConsument.Vormsoort.FEITCREATIE, ProducentConsument.basisNaam(f.naam()), f,
                        recursive.contains(Namen.norm(withoutVersion(naam))), true, writes, leest));
            }
            for (int i = 0; i < defaults.size(); i++) {
                PrioritizedDefaults p = defaults.get(i);
                Objecttype onderwerp = model.objecttypeOpNaam(p.onderwerp());
                Set<String> writes = new LinkedHashSet<>();
                if (onderwerp != null && p.doel() != null)
                    writes.addAll(extensionWrite(onderwerp, p.doel(), p.isKenmerk()));
                List<Read> leest = new ArrayList<>();
                for (Default d : p.defaults()) defaultReads(d, onderwerp, leest);
                out.add(new Vorm(ProducentConsument.Vormsoort.DEFAULTSGROEP,
                        p.naam() != null ? ProducentConsument.basisNaam(p.naam()) : "/rules/prioritizedDefaults/" + i, p,
                        false, false, writes, negative(leest)));
            }
            return out;
        }

        /** The relation an object creation writes as its back-link: its {@code connectingRole} binding's, else the one
         *  fact type relating its subject and created type, none for a self-typed two-role pair, which the derivation
         *  leaves unlinked (the verifier's reading of the by-pair route). */
        private String backLink(ResolvedObjectCreation o) {
            RoleBindingRef rb = roleBindings.get(o);
            if (rb != null) return rb.feittype();
            Objecttype s = o.subject(), n = o.createdType();
            if (s == null || n == null) return null;
            List<RegelSpraakModel.Feittype> candidates = new ArrayList<>();
            for (RegelSpraakModel.Feittype ft : model.feittypen.values()) {
                List<RegelSpraakModel.Rol> r = ft.rollen;
                if (r.size() == 2) {
                    boolean ab = s.naam.equalsIgnoreCase(r.get(0).objecttypeNaam())
                            && n.naam.equalsIgnoreCase(r.get(1).objecttypeNaam());
                    boolean ba = s.naam.equalsIgnoreCase(r.get(1).objecttypeNaam())
                            && n.naam.equalsIgnoreCase(r.get(0).objecttypeNaam());
                    if (ab || ba) candidates.add(ft);
                } else if (r.size() == 1 && ft.wederkerig && s == n && s.naam.equalsIgnoreCase(r.get(0).objecttypeNaam())) {
                    candidates.add(ft);
                }
            }
            if (candidates.size() != 1) return null;
            RegelSpraakModel.Feittype ft = candidates.get(0);
            return s == n && !ft.wederkerig ? null : ft.naam;
        }

        private void defaultReads(Default d, Objecttype onderwerp, List<Read> out) {
            condition(d.voorwaarde(), onderwerp, false, out);
            expr(d.consequentie(), onderwerp, out);
            if (d.exceptions() == null) return;
            for (ExceptionGroup g : d.exceptions().leden())
                for (Default member : g.defaults()) defaultReads(member, onderwerp, out);
        }

        private static List<Read> negative(List<Read> l) {
            List<Read> out = new ArrayList<>();
            for (Read x : l) out.add(new Read(x.node(), true));
            return out;
        }

        // ---------------- the reads ----------------

        /** The verifier's {@code dep_expr}: every expression node, so a node the IR records gain later fails to compile
         *  here until it is decided what it reads. */
        private void expr(Expr e, Objecttype subject, List<Read> out) {
            if (e == null) return;
            switch (e) {
                case Expr.AttribuutRef a -> {
                    if (a.objecttype() == null || a.attribuut() == null) return;
                    for (RoleBindingRef hop : navigation.getOrDefault(a, List.of()))
                        if (hop.feittype() != null) out.add(new Read(relation(hop.feittype()), false));
                    out.add(new Read(member(a.objecttype(), a.attribuut(), false), false));
                }
                case Expr.Binair b -> { expr(b.links(), subject, out); expr(b.rechts(), subject, out); }
                case Expr.FunctionCall f -> {
                    Objecttype el = f.elementObjectType();
                    if (el != null) out.add(new Read(population(el), false));
                    List<RoleBindingRef> pad = sourcePaths.get(f);
                    if (pad != null)
                        for (RoleBindingRef hop : pad) if (hop.feittype() != null) out.add(new Read(relation(hop.feittype()), false));
                    RoleBindingRef rol = roleBindings.get(f);
                    if (rol != null && rol.feittype() != null) out.add(new Read(relation(rol.feittype()), false));
                    // the count source names the relation's role, not a member: it reads the link set and the
                    // population drawn above, never a member of the contributor
                    boolean countSource = f.kind() == Expr.FunctionKind.AANTAL && el != null && f.arguments().size() == 1
                            && f.arguments().get(0) instanceof Expr.AttribuutRef bron && bron.attribuut() != null
                            && (pad != null || rol != null)
                            && !carries(el, bron.attribuut(), false) && !carries(el, bron.attribuut(), true);
                    if (!countSource) for (Expr a : f.arguments()) expr(a, subject, out);
                    expr(f.emptyFallback(), subject, out);
                    condition(f.filter(), el != null ? el : subject, false, out);
                }
                case Expr.InstanceSetUnion u -> {
                    for (Expr.InstanceSetUnion.Branch t : u.branches()) {
                        if (t.elementType() != null) out.add(new Read(population(t.elementType()), false));
                        if (t.sourceRole() != null && t.sourceRole().feittype() != null)
                            out.add(new Read(relation(t.sourceRole().feittype()), false));
                        condition(t.filter(), t.elementType(), false, out);
                    }
                }
                case Expr.Afgerond a -> expr(a.basis(), subject, out);
                case Expr.Begrensd b -> {
                    expr(b.basis(), subject, out);
                    expr(b.minimum(), subject, out);
                    expr(b.maximum(), subject, out);
                }
                case Expr.DimensionCellRead c -> expr(c.attribuut(), subject, out);
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
                case Expr.Constante ignored -> { }
                case Expr.Unresolved ignored -> { }
            }
        }

        /** The verifier's {@code dep_cond}: {@code neg} is the negation-like context a characteristic read under it
         *  takes; a leeg or gevuld test makes every read under it negative, in either polarity; a quantifier fold reads
         *  its range negatively under all, none, atMost and exactly and its matrix under none, atMost and exactly. */
        private void condition(Conditie c, Objecttype subject, boolean neg, List<Read> out) {
            if (c == null) return;
            switch (c) {
                case Conditie.Vergelijking v -> { expr(v.links(), subject, out); expr(v.rechts(), subject, out); }
                case Conditie.Predicaat p -> {
                    if (p.soort() == Conditie.PredicateKind.LEEG || p.soort() == Conditie.PredicateKind.GEVULD) {
                        List<Read> under = new ArrayList<>();
                        expr(p.operand(), subject, under);
                        out.addAll(negative(under));
                    } else {
                        expr(p.operand(), subject, out);
                    }
                }
                case Conditie.Kenmerk k -> {
                    if (k.kenmerk() == null) return;
                    boolean negative = neg || k.negatie();
                    List<RoleBindingRef> pad = navigation.get(k);
                    if (pad != null && !pad.isEmpty()) {
                        for (RoleBindingRef hop : pad)
                            if (hop.feittype() != null) out.add(new Read(relation(hop.feittype()), false));
                        Objecttype reaches = filler(pad.get(pad.size() - 1));
                        if (reaches != null) out.add(new Read(member(reaches, k.kenmerk(), true), negative));
                        return;
                    }
                    Objecttype carrier = k.eigenaar() != null ? k.eigenaar() : subject;
                    if (carrier != null) out.add(new Read(member(carrier, k.kenmerk(), true), negative));
                }
                case Conditie.Rollidmaatschap r -> {
                    RoleBindingRef rb = roleBindings.get(r);
                    if (rb != null && rb.feittype() != null) out.add(new Read(relation(rb.feittype()), neg || r.negatie()));
                }
                // a role check the loader records without a binding: no relation of its own to name
                case Conditie.FarRoleMembership ignored -> { }
                case Conditie.Periode p -> { expr(p.vanaf(), subject, out); expr(p.tot(), subject, out); }
                case Conditie.Samengesteld s -> {
                    boolean under = switch (s.kwantor()) {
                        case GEEN, TEN_HOOGSTE, PRECIES -> true;
                        default -> neg;
                    };
                    for (Conditie d : s.onderdelen()) condition(d, subject, under, out);
                }
                case Conditie.QuantifierFold kv -> {
                    boolean rangeNeg = neg || kv.kwantor() != Conditie.Kwantor.TEN_MINSTE;
                    boolean matrixNeg = neg || !(kv.kwantor() == Conditie.Kwantor.TEN_MINSTE
                            || kv.kwantor() == Conditie.Kwantor.ALLE);
                    List<RoleBindingRef> range = new ArrayList<>();
                    if (kv.sourceRole() != null) range.add(kv.sourceRole());
                    if (kv.sourcePath() != null) range.addAll(kv.sourcePath());
                    for (RoleBindingRef hop : range)
                        if (hop.feittype() != null) out.add(new Read(relation(hop.feittype()), rangeNeg));
                    part(kv.filter(), kv.elementObjectType(), rangeNeg, out);
                    part(kv.test(), kv.elementObjectType(), matrixNeg, out);
                }
                case Conditie.RuleStatusCheck rv -> {
                    if (rv.regel() != null) out.add(new Read(channel(rv.regel()), true));
                }
                case Conditie.Unresolved ignored -> { }
            }
        }

        /** A part of a quantifier fold: negative throughout where {@code negative}, its own kinds otherwise. */
        private void part(Conditie c, Objecttype subject, boolean negative, List<Read> out) {
            List<Read> under = new ArrayList<>();
            condition(c, subject, negative, under);
            out.addAll(negative ? negative(under) : under);
        }

        /** The declared filler of the role a hop binds: the type it reaches. */
        private Objecttype filler(RoleBindingRef hop) {
            for (RegelSpraakModel.Feittype ft : model.feittypen.values())
                if (Namen.norm(ft.naam).equals(Namen.norm(hop.feittype())))
                    for (RegelSpraakModel.Rol r : ft.rollen)
                        if (r.naam() != null && Namen.norm(r.naam()).equals(Namen.norm(hop.rol())))
                            return model.objecttypeOpNaam(r.objecttypeNaam());
            return null;
        }
    }
}
