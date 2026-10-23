package dev.uchar.regelspraak.resolver;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * The dependency graph of IR-SPEC ch. 5 over a written {@code uchar-ir-v1} document, and its two cycle rules WF-045 and
 * WF-051, in the verifier's own reading: the forms of {@code dep_forms} in its order (the rule versions, allocations,
 * consistencies, uniquenesses, unions of uniquenesses, object creations, fact creations and defaults groups), what each
 * reads and writes ({@code dep_expr}, {@code dep_cond}, {@code member_writes}, {@code pop_writes}), the edges in reading
 * order ({@code dep_edges}), the depth-first walk of WF-045 that names the first cycle it meets, and the reach test of
 * WF-051, each with the verifier's sentence. It reads the document the emitter writes and not the model, so it
 * judges what the verifier judges; {@code check} has no verifier to call, so the frontend repeats it, as it repeats
 * other WF rules in their sentences. {@link #edges} prints the graph in the form of {@code verifier --dependency-edges},
 * so the two can be compared line for line. Since 2026-10-22 a read carries its path, and the instance-ordered parts
 * of IR-SPEC ch. 5.3 are found as the verifier's {@code instance_ordering} finds them: their shifted edges leave the
 * graph both walks read, and a part over a switch relation that falls on a clause of ch. 5.3 names its reads outside
 * ({@link #find}).
 */
final class Dependencies {

    private Dependencies() {}

    /** A violation of WF-045 or WF-051 as the verifier reports it: the rule, the pointer, its sentence, and the forms of
     *  the cycle ({@code forms}: for WF-045 the cycle in the walk's order, for WF-051 the group alone). */
    record Cycle(String wf, String at, String sentence, List<Form> forms) {}

    /** A form of the graph: its pointer, its name (a version of a form with several is "&lt;naam&gt; #i"), whether it
     *  is a creation in a recursive rule group, what it writes and what it reads; {@code category} is the list it comes
     *  from. */
    record Form(String at, String name, String category, boolean recursiveCreation, List<String> writes, List<NodeRead> uses) {}

    /** One read: the node, its edge kind (positive, strict, negative; ch. 5.2) and, for a read of an attribute or a
     *  characteristic by a rule or a defaults group, its path: the hops from the form's subject to the object it lands
     *  on, null where no written path reaches it (the verifier's {@code dep_expr_at}). The instance-ordered cycle
     *  (IR-SPEC ch. 5.3) reads the path; the walks read node and kind. */
    record NodeRead(String graphNode, String category, List<Hop> path) {
        NodeRead(String graphNode, String category) {
            this(graphNode, category, null);
        }
    }

    /** A hop of a read path: a role binding as the document spells it. */
    record Hop(String factType, String role) {}

    /** A read behind an edge inside a strongly connected part over a switch relation that is neither shifted nor local
     *  (IR-SPEC ch. 5.3), as the verifier names it under {@code WF-045.instance-read-outside}: the form that reads, the
     *  node, the path ({@code factType/role; …}, "(none)" where no written path reaches the object) and the clause it
     *  falls on; beside them the switch relation the part walks, as the document spells it. */
    record ReadOutside(Form form, String node, String path, String clause, String factType) {}

    /** What the cycle rules find in a document: the WF-045 and WF-051 violations, and the reads that keep a part over a
     *  switch relation from being instance-ordered. */
    record Findings(List<Cycle> cycles, List<ReadOutside> readsOutside) {}

    private record Edge(int producer, int consumer, String graphNode, String category) {}

    /** An edge without its kind: the verifier's {@code shifted (producer, consumer, node)}. */
    private record EdgeKey(int producer, int consumer, String graphNode) {}

    // ---------------------------------------------------------------- the two rules

    /** The WF-045 and WF-051 violations of {@code ir}, in the order and the sentences of the verifier. */
    static List<Cycle> cycles(String ir) {
        return find(ir).cycles();
    }

    /** The WF-045 and WF-051 violations of {@code ir} and its reads outside an instance-ordered part. The shifted edges
     *  of the instance-ordered parts leave the graph both walks read, as they leave the verifier's: such a cycle is
     *  admitted (IR-SPEC ch. 5.3). */
    static Findings find(String ir) {
        Graph g = new Graph(Json.parse(ir));
        List<Form> forms = g.forms();
        List<Edge> edges = edges(forms);
        InstanceOrdering ordering = new InstanceOrdering(g, forms, edges);
        List<Cycle> result = new ArrayList<>(wf045(forms, edges, ordering.shifted));
        result.addAll(wf051(forms, g, ordering.shifted));
        return new Findings(result, ordering.outside);
    }

    /** The graph as {@code verifier --dependency-edges} prints it: one tab-separated line per edge — the kind, the
     *  producer's pointer and name, the node, the consumer's pointer and name — sorted, without repeats. */
    static List<String> edges(String ir) {
        Graph g = new Graph(Json.parse(ir));
        List<Form> forms = g.forms();
        TreeSet<String> result = new TreeSet<>(Dependencies::compareBytes);
        for (Edge r : edges(forms)) {
            Form p = forms.get(r.producer()), c = forms.get(r.consumer());
            result.add(String.join("\t", r.category(), m1Root(p.at()), p.name(), r.graphNode(), m1Root(c.at()), c.name()));
        }
        return new ArrayList<>(result);
    }

    /** Whether a negative edge (ch. 5.2) runs from one form of the cycle {@code k} to another, or from a form to itself.
     *  Every form of a cycle reaches every other, so such an edge lies on a cycle and the verifier refuses it under
     *  WF-046 as well ({@code wf045_046}); a negative edge from a form outside {@code k} is not looked for. */
    static boolean carriesNegativeEdge(Cycle k) {
        for (Form c : k.forms())
            for (NodeRead l : c.uses())
                if (NEG.equals(l.category()) && k.forms().stream().anyMatch(p -> p.writes().contains(l.graphNode())))
                    return true;
        return false;
    }

    /** {@code name} without the " #i" of a version of a form with several ({@code strip_version}). */
    static String withoutVersion(String name) { return Graph.withoutVersion(name); }

    private static List<Cycle> wf045(List<Form> forms, List<Edge> edges, Set<EdgeKey> shifted) {
        int n = forms.size();
        List<List<int[]>> followers = followersOf(n, edges, shifted);
        int[] color = new int[n];
        List<Cycle> result = new ArrayList<>();
        for (int i = 0; i < n; i++) if (color[i] == 0) deep(forms, followers, color, new ArrayList<>(), i, result);
        return result;
    }

    /** The verifier's walk: a node already on the path closes a cycle, which is reported from that node on. The path
     *  is kept most recent first, as the verifier's list is. */
    private static void deep(List<Form> forms, List<List<int[]>> followers, int[] color, List<Integer> path, int i,
                             List<Cycle> result) {
        if (forms.get(i).recursiveCreation()) return;
        if (color[i] == 1) {
            List<Integer> cycle = new ArrayList<>();
            for (int j : path) {
                cycle.add(0, j);
                if (j == i) break;
            }
            List<String> names = new ArrayList<>();
            List<Form> members = new ArrayList<>();
            for (int j : cycle) { names.add(forms.get(j).name()); members.add(forms.get(j)); }
            names.add(forms.get(i).name());
            result.add(new Cycle("WF-045", m1Root(forms.get(i).at()),
                    "dependency cycle through no recursive creation: " + String.join(" -> ", names), members));
            return;
        }
        if (color[i] == 2) return;
        color[i] = 1;
        List<Integer> further = new ArrayList<>();
        further.add(i);
        further.addAll(path);
        for (int[] j : followers.get(i)) deep(forms, followers, color, further, j[0], result);
        color[i] = 2;
    }

    /** The successors of every form, as the verifier builds them: for each edge in reading order, the consumer with the
     *  edge's kind put in FRONT of the producer's list unless that pair is there already, so a list is walked from the
     *  last edge inserted, a shifted edge left out. An entry is {consumer, kind index}. */
    private static List<List<int[]>> followersOf(int n, List<Edge> edges, Set<EdgeKey> shifted) {
        List<List<int[]>> followers = new ArrayList<>();
        for (int i = 0; i < n; i++) followers.add(new ArrayList<>());
        for (Edge r : edges) {
            if (shifted.contains(new EdgeKey(r.producer(), r.consumer(), r.graphNode()))) continue;
            List<int[]> l = followers.get(r.producer());
            int category = kindIndex(r.category());
            boolean already = false;
            for (int[] x : l) if (x[0] == r.consumer() && x[1] == category) { already = true; break; }
            if (!already) l.add(0, new int[] {r.consumer(), category});
        }
        return followers;
    }

    private static List<Cycle> wf051(List<Form> forms, Graph g, Set<EdgeKey> shifted) {
        int n = forms.size();
        Map<String, List<Integer>> writers = writers(forms);
        List<List<Integer>> followers = new ArrayList<>();
        for (int i = 0; i < n; i++) followers.add(new ArrayList<>());
        for (int ci = 0; ci < n; ci++)
            for (NodeRead l : forms.get(ci).uses())
                for (int pi : writers.getOrDefault(l.graphNode(), List.of()))
                    if (!shifted.contains(new EdgeKey(pi, ci, l.graphNode()))) followers.get(pi).add(0, ci);
        List<Cycle> result = new ArrayList<>();
        List<Map<String, Object>> groups = g.list(g.rules, "prioritizedDefaults");
        for (int i = 0; i < groups.size(); i++) {
            String at = "/prioritizedDefaults/" + i;
            for (int fi = 0; fi < n; fi++) {
                if (!forms.get(fi).at().equals(at)) continue;
                if (reached(followers, fi, fi, new boolean[n])) {
                    String name = Graph.text(groups.get(i), "name");
                    result.add(new Cycle("WF-051", m1Root(at), "a dependency cycle passes through defaults group '"
                            + (name == null ? at : name) + "' — its displacement reads are negative edges; no strata exist",
                            List.of(forms.get(fi))));
                }
                break;
            }
        }
        return result;
    }

    private static boolean reached(List<List<Integer>> followers, int i, int target, boolean[] seen) {
        for (int j : followers.get(i)) {
            if (j == target) return true;
            if (!seen[j]) {
                seen[j] = true;
                if (reached(followers, j, target, seen)) return true;
            }
        }
        return false;
    }

    /** Every writer of every node, the last one written first, as the verifier's table returns them. */
    private static Map<String, List<Integer>> writers(List<Form> forms) {
        Map<String, List<Integer>> result = new LinkedHashMap<>();
        for (int i = 0; i < forms.size(); i++)
            for (String graphNode : forms.get(i).writes()) result.computeIfAbsent(graphNode, k -> new ArrayList<>()).add(0, i);
        return result;
    }

    /** The edges in reading order: consumer by consumer, each read in turn, its writers as the table answers them. */
    private static List<Edge> edges(List<Form> forms) {
        Map<String, List<Integer>> writers = writers(forms);
        List<Edge> result = new ArrayList<>();
        for (int ci = 0; ci < forms.size(); ci++)
            for (NodeRead l : forms.get(ci).uses())
                for (int pi : writers.getOrDefault(l.graphNode(), List.of())) result.add(new Edge(pi, ci, l.graphNode(), l.category()));
        return result;
    }

    private static int kindIndex(String category) {
        return switch (category) { case POS -> 0; case STRICT -> 1; default -> 2; };
    }

    /** The rule lists the verifier addresses from the root and maps under {@code /rules} ({@code m1_rule_lists}). */
    private static final List<String> RULE_LISTS = List.of("dayKindDefinitions", "startPointDeterminations",
            "ruleGroups", "prioritizedDefaults");

    /** The verifier's pointer of a form in the document ({@code m1_root}): a defaults group's sits under
     *  {@code /rules}. */
    private static String m1Root(String at) {
        String[] d = at.split("/", -1);
        if (d.length >= 2 && d[0].isEmpty() && RULE_LISTS.contains(d[1])) return "/rules" + at;
        return at;
    }

    /** OCaml's string order, byte by byte over UTF-8, which for these names is code point order. */
    private static int compareBytes(String a, String b) {
        return a.compareTo(b);
    }

    static final String POS = "positive", STRICT = "strict", NEG = "negative";

    // ---------------------------------------------------------------- the instance-ordered cycle (IR-SPEC ch. 5.3)

    /**
     * The instance-ordered parts of the graph, as the verifier's {@code instance_ordering} finds them, and the reads that
     * keep a part over a switch relation from being one. A strongly connected part is instance-ordered when it holds no
     * creation of a recursive group and only rules and defaults groups, its shifted reads all walk one switch relation
     * to one role, every read behind an internal edge is shifted or local, every satellite type it reads is reached
     * through one satellite relation, at least one edge is shifted, and the edges that remain close no cycle.
     * Its shifted edges then leave the graph that WF-045 and WF-051 walk. Where a part walks a switch relation and a
     * read falls on a clause of ch. 5.3, that read comes back with the clause, as the verifier names it under
     * {@code WF-045.instance-read-outside}: the cycle names the forms, this names the read the author has to change.
     */
    private static final class InstanceOrdering {
        /** The shifted edges of the instance-ordered parts. */
        final Set<EdgeKey> shifted = new HashSet<>();
        /** The reads outside, part by part in the order the verifier's components come, sorted within a part. */
        final List<ReadOutside> outside = new ArrayList<>();

        private final Graph g;
        private final List<Form> forms;
        private final Set<String> written = new HashSet<>();

        /** A hop over a switch relation: the relation and the role it reaches, both normalised, the role's filler (the
         *  type of the cycle), and the hop as written. */
        private record Walk(String relation, String role, String type, Hop hop) {}

        /** A satellite relation a path walks: the relation, normalised, and the satellite type. */
        private record Satellite(String relation, String type) {}

        /** A hop over a satellite relation: whether it goes back to the owner, and the relation walked. */
        private record Step(boolean toOwner, Satellite satellite) {}

        /** The class of a read path: {@code kind} "shifted" or "local" with the satellite relations it walks, or
         *  {@code kind} null with the clause it falls on. */
        private record PathClass(String kind, String clause, List<Satellite> satellites) {
            static PathClass outside(String clause) { return new PathClass(null, clause, List.of()); }
        }

        /** A binding resolved ({@code resolve_binding}): its fact type, the role it binds and the other role. */
        private record Binding(Map<String, Object> factType, Map<String, Object> bound, Map<String, Object> other) {}

        /** A read of a consumer: the form and the node. */
        private record Reader(int consumer, String graphNode) {}

        /** A read outside, before it is spelled. */
        private record Entry(int consumer, String graphNode, List<Hop> path, String clause, String factType) {}

        private enum State { SUBJECT, OWNER_FIRST, PREDECESSOR, HOUSEHOLD }

        InstanceOrdering(Graph g, List<Form> forms, List<Edge> edges) {
            this.g = g;
            this.forms = forms;
            for (Form f : forms) written.addAll(f.writes());
            int n = forms.size();
            List<List<Integer>> succ = new ArrayList<>();
            for (int i = 0; i < n; i++) succ.add(new ArrayList<>());
            for (Edge e : edges)
                if (!succ.get(e.producer()).contains(e.consumer())) succ.get(e.producer()).add(0, e.consumer());
            for (List<Integer> part : components(n, succ)) part(part, edges);
        }

        /** The rule and the defaults group write per subject of their own type: the only forms such a part may hold. */
        private static boolean perSubject(Form f) {
            return "rules".equals(f.category()) || "prioritizedDefaults".equals(f.category());
        }

        private void part(List<Integer> part, List<Edge> edges) {
            List<Edge> internal = new ArrayList<>();
            for (Edge e : edges) if (part.contains(e.producer()) && part.contains(e.consumer())) internal.add(e);
            if (internal.isEmpty()) return;
            for (int i : part) if (forms.get(i).recursiveCreation() || !perSubject(forms.get(i))) return;
            // the reads behind the internal edges, one entry per consumer and node
            TreeSet<Reader> reads = new TreeSet<>(Comparator.comparingInt(Reader::consumer)
                    .thenComparing(Reader::graphNode, Dependencies::compareBytes));
            for (Edge e : internal) reads.add(new Reader(e.consumer(), e.graphNode()));
            TreeSet<Walk> walks = new TreeSet<>(Comparator.comparing(Walk::relation, Dependencies::compareBytes)
                    .thenComparing(Walk::role, Dependencies::compareBytes)
                    .thenComparing(Walk::type, Dependencies::compareBytes)
                    .thenComparing(w -> w.hop().factType(), Dependencies::compareBytes)
                    .thenComparing(w -> w.hop().role(), Dependencies::compareBytes));
            for (Reader r : reads)
                for (List<Hop> path : positions(r)) {
                    Walk w = walkOf(path);
                    if (w != null) walks.add(w);
                }
            TreeSet<String> pairs = new TreeSet<>();
            for (Walk w : walks) pairs.add(w.relation() + "\u0000" + w.role());
            if (pairs.isEmpty()) return;
            TreeSet<Entry> found = new TreeSet<>(ENTRY_ORDER);
            if (pairs.size() > 1) {
                // one switch relation, one role: every read that walks one is named
                for (Reader r : reads)
                    for (List<Hop> path : positions(r)) {
                        Walk w = walkOf(path);
                        if (w != null) found.add(new Entry(r.consumer(), r.graphNode(), path, "one-walk", w.hop().factType()));
                    }
                spell(found);
                return;
            }
            Walk first = walks.first();
            String relation = first.relation(), role = first.role(), t = first.type();
            Map<Reader, List<PathClass>> classes = new LinkedHashMap<>();
            List<Satellite> satellites = new ArrayList<>();
            for (Reader r : reads) {
                List<PathClass> cs = new ArrayList<>();
                for (List<Hop> path : positions(r)) {
                    PathClass c = classify(relation, role, t, path);
                    cs.add(c);
                    if (c.kind() != null) satellites.addAll(c.satellites());
                }
                classes.put(r, cs);
            }
            // one satellite relation per satellite type: two would give a satellite two owners, and the order two depths
            Set<Satellite> shared = new HashSet<>();
            for (Satellite a : satellites)
                for (Satellite b : satellites)
                    if (!a.relation().equals(b.relation()) && compatible(a.type(), b.type())) shared.add(a);
            for (Reader r : reads) {
                List<List<Hop>> paths = positions(r);
                for (int k = 0; k < paths.size(); k++) {
                    PathClass c = classes.get(r).get(k);
                    String clause = c.kind() == null ? c.clause()
                            : c.satellites().stream().anyMatch(shared::contains) ? "one-satellite-relation" : null;
                    if (clause != null) found.add(new Entry(r.consumer(), r.graphNode(), paths.get(k), clause, first.hop().factType()));
                }
            }
            if (!found.isEmpty()) {
                spell(found);
                return;
            }
            Set<Reader> shiftedReads = new HashSet<>();
            for (Map.Entry<Reader, List<PathClass>> e : classes.entrySet())
                if (e.getValue().stream().allMatch(c -> "shifted".equals(c.kind()))) shiftedReads.add(e.getKey());
            List<Edge> remaining = new ArrayList<>();
            for (Edge e : internal) if (!shiftedReads.contains(new Reader(e.consumer(), e.graphNode()))) remaining.add(e);
            if (shiftedReads.isEmpty() || !acyclic(forms.size(), remaining) || !settles(part, remaining)) return;
            for (Edge e : internal)
                if (shiftedReads.contains(new Reader(e.consumer(), e.graphNode())))
                    shifted.add(new EdgeKey(e.producer(), e.consumer(), e.graphNode()));
        }

        private void spell(TreeSet<Entry> found) {
            for (Entry e : found) {
                String path = "(none)";
                if (e.path() != null) {
                    List<String> hops = new ArrayList<>();
                    for (Hop h : e.path()) hops.add(h.factType() + "/" + h.role());
                    path = String.join("; ", hops);
                }
                outside.add(new ReadOutside(forms.get(e.consumer()), e.graphNode(), path, e.clause(), e.factType()));
            }
        }

        /** The verifier's order of its reads outside ({@code sort_uniq compare}): consumer, node, path (none first, then
         *  hop by hop, a prefix first), clause. */
        private static final Comparator<Entry> ENTRY_ORDER = Comparator.comparingInt(Entry::consumer)
                .thenComparing(Entry::graphNode, Dependencies::compareBytes)
                .thenComparing(Entry::path, InstanceOrdering::comparePaths)
                .thenComparing(Entry::clause, Dependencies::compareBytes);

        private static int comparePaths(List<Hop> a, List<Hop> b) {
            if (a == null || b == null) return a == null ? (b == null ? 0 : -1) : 1;
            for (int i = 0; i < Math.min(a.size(), b.size()); i++) {
                int c = compareBytes(a.get(i).factType(), b.get(i).factType());
                if (c == 0) c = compareBytes(a.get(i).role(), b.get(i).role());
                if (c != 0) return c;
            }
            return Integer.compare(a.size(), b.size());
        }

        /** The paths of the reads of a node by its consumer, one per read. */
        private List<List<Hop>> positions(Reader r) {
            List<List<Hop>> result = new ArrayList<>();
            for (NodeRead l : forms.get(r.consumer()).uses()) if (l.graphNode().equals(r.graphNode())) result.add(l.path());
            return result;
        }

        /** Whether the edges that remain close no cycle (the verifier's {@code acyclic}): a part they leave cyclic is no
         *  instance-ordered part (IR-SPEC ch. 5.3), and the levels do not see every such cycle, since a cycle of
         *  positive edges raises no level. */
        private static boolean acyclic(int n, List<Edge> remaining) {
            List<List<Integer>> rest = new ArrayList<>();
            for (int i = 0; i < n; i++) rest.add(new ArrayList<>());
            for (Edge e : remaining) rest.get(e.producer()).add(e.consumer());
            for (List<Integer> c : components(n, rest))
                if (c.size() > 1 || rest.get(c.get(0)).contains(c.get(0))) return false;
            return true;
        }

        /** The levels over the edges that remain, a strict or negative edge counting one: whether they settle within as
         *  many rounds as the part has forms (the verifier's {@code settle}). */
        private static boolean settles(List<Integer> part, List<Edge> remaining) {
            Map<Integer, Integer> level = new HashMap<>();
            for (int i : part) level.put(i, 0);
            for (int round = 0; ; round++) {
                boolean changed = false;
                for (Edge e : remaining) {
                    int need = level.get(e.producer()) + (POS.equals(e.category()) ? 0 : 1);
                    if (level.get(e.consumer()) < need) {
                        level.put(e.consumer(), need);
                        changed = true;
                    }
                }
                if (!changed) return true;
                if (round > part.size()) return false;
            }
        }

        /** The strongly connected parts over {@code succ} (Tarjan, the verifier's {@code components}), in the order
         *  they complete, each with its forms in ascending order. */
        private static List<List<Integer>> components(int n, List<List<Integer>> succ) {
            int[] index = new int[n], low = new int[n];
            java.util.Arrays.fill(index, -1);
            boolean[] onStack = new boolean[n];
            java.util.ArrayDeque<Integer> stack = new java.util.ArrayDeque<>();
            int[] counter = {0};
            List<List<Integer>> out = new ArrayList<>();
            for (int k = 0; k < n; k++) if (index[k] < 0) visit(k, succ, index, low, onStack, stack, counter, out);
            return out;
        }

        private static void visit(int k, List<List<Integer>> succ, int[] index, int[] low, boolean[] onStack,
                                  java.util.ArrayDeque<Integer> stack, int[] counter, List<List<Integer>> out) {
            index[k] = counter[0];
            low[k] = counter[0];
            counter[0]++;
            stack.push(k);
            onStack[k] = true;
            for (int m : succ.get(k)) {
                if (index[m] < 0) {
                    visit(m, succ, index, low, onStack, stack, counter, out);
                    low[k] = Math.min(low[k], low[m]);
                } else if (onStack[m]) low[k] = Math.min(low[k], index[m]);
            }
            if (low[k] == index[k]) {
                List<Integer> part = new ArrayList<>();
                while (true) {
                    int m = stack.pop();
                    onStack[m] = false;
                    part.add(m);
                    if (m == k) break;
                }
                Collections.sort(part);
                out.add(part);
            }
        }

        // ------------------------------------------------------------ the hops of a path

        private boolean compatible(String a, String b) {
            return g.isSub(a, b) || g.isSub(b, a);
        }

        private static boolean many(Map<String, Object> role) {
            return Boolean.TRUE.equals(role.get("many"));
        }

        private static boolean reciprocal(Map<String, Object> factType) {
            return Boolean.TRUE.equals(factType.get("reciprocal"));
        }

        private boolean inputOnly(String factType) {
            return !written.contains(Graph.relNode(factType));
        }

        private static boolean names(Map<String, Object> role, String name) {
            String r = Graph.text(role, "name");
            return r != null && Graph.norm(r).equals(Graph.norm(name));
        }

        private Binding resolve(Hop h) {
            Map<String, Object> ft = g.factType(h.factType());
            if (ft == null || h.role() == null) return null;
            List<Map<String, Object>> roles = Graph.listOrEmpty(ft, "roles");
            if (roles.size() == 2) {
                boolean a = names(roles.get(0), h.role()), b = names(roles.get(1), h.role());
                if (a && !b) return new Binding(ft, roles.get(0), roles.get(1));
                if (b && !a) return new Binding(ft, roles.get(1), roles.get(0));
                return null;
            }
            if (roles.size() == 1 && reciprocal(ft) && names(roles.get(0), h.role()))
                return new Binding(ft, roles.get(0), roles.get(0));
            return null;
        }

        /** A hop over a switch relation ({@code switch_hop}): two roles on comparable types, the role it reaches
         *  to-one, no form writing its link set; null otherwise. */
        private Walk switchHop(Hop h) {
            Binding b = resolve(h);
            if (b == null || reciprocal(b.factType()) || many(b.bound()) || !inputOnly(h.factType())) return null;
            String bound = Graph.text(b.bound(), "objectType"), other = Graph.text(b.other(), "objectType");
            if (bound == null || other == null || !compatible(bound, other)) return null;
            return new Walk(Graph.norm(h.factType()), Graph.norm(h.role()), bound, h);
        }

        /** A hop over a satellite relation of {@code t} other than {@code relation} ({@code satellite_hop}): the owner's
         *  role to-one and filled by a type comparable with {@code t}, the other filled by a type that is not; to the
         *  satellite it may reach a set. Null otherwise. */
        private Step satelliteHop(String relation, String t, Hop h) {
            Binding b = resolve(h);
            if (b == null || reciprocal(b.factType()) || Graph.norm(h.factType()).equals(relation) || !inputOnly(h.factType()))
                return null;
            String bound = Graph.text(b.bound(), "objectType"), other = Graph.text(b.other(), "objectType");
            if (bound == null || other == null) return null;
            if (Graph.text(b.other(), "name") != null && compatible(other, t) && !compatible(bound, t) && !many(b.other()))
                return new Step(false, new Satellite(Graph.norm(h.factType()), bound));
            if (compatible(bound, t) && !compatible(other, t) && !many(b.bound()))
                return new Step(true, new Satellite(Graph.norm(h.factType()), other));
            return null;
        }

        /** The clause of ch. 5.3 a hop falls on that is neither the step to the predecessor nor a household hop
         *  ({@code clause_of_hop}). */
        private String clauseOfHop(String t, Hop h) {
            Binding b = resolve(h);
            if (b == null) return "shifted-or-local";
            String bound = Graph.text(b.bound(), "objectType"), other = Graph.text(b.other(), "objectType");
            if (bound == null || other == null) return "shifted-or-local";
            boolean bc = compatible(bound, t), oc = compatible(other, t);
            if (bc && oc) return many(b.bound()) ? "set-of-predecessors" : "shifted-or-local";
            if (oc) return many(b.other()) ? "hub" : "shifted-or-local";
            if (bc) return many(b.bound()) ? "hub" : "shifted-or-local";
            return "satellite-of-satellite";
        }

        /** The hop over a switch relation a path starts with: its first hop, or its second after one that is none
         *  ({@code walk_of}); null where there is neither. */
        private Walk walkOf(List<Hop> path) {
            if (path == null || path.isEmpty()) return null;
            Walk w = switchHop(path.get(0));
            if (w != null) return w;
            return path.size() > 1 ? switchHop(path.get(1)) : null;
        }

        /** The class of a path within a part over ({@code relation}, {@code role}, {@code t}), hop by hop
         *  ({@code classify}): local, household hops from the subject; shifted, at most one hop to the owner first, one
         *  hop over the relation to the predecessor's role, then household hops. */
        private PathClass classify(String relation, String role, String t, List<Hop> path) {
            if (path == null) return PathClass.outside("shifted-or-local");
            State state = State.SUBJECT;
            List<Satellite> satellites = new ArrayList<>();
            for (Hop h : path) {
                Walk w = switchHop(h);
                if (w != null) {
                    if (!(w.relation().equals(relation) && w.role().equals(role)))
                        return PathClass.outside(state == State.PREDECESSOR ? "two-switch-steps" : "one-walk");
                    switch (state) {
                        case SUBJECT, OWNER_FIRST -> state = State.PREDECESSOR;
                        case PREDECESSOR -> { return PathClass.outside("two-switch-steps"); }
                        case HOUSEHOLD -> { return PathClass.outside("shifted-or-local"); }
                    }
                    continue;
                }
                Step s = satelliteHop(relation, t, h);
                if (s == null) return PathClass.outside(clauseOfHop(t, h));
                state = state == State.SUBJECT && s.toOwner() ? State.OWNER_FIRST
                        : state == State.PREDECESSOR ? State.PREDECESSOR : State.HOUSEHOLD;
                satellites.add(s.satellite());
            }
            return new PathClass(state == State.PREDECESSOR ? "shifted" : "local", null, satellites);
        }
    }

    // ---------------------------------------------------------------- the graph over the document

    /** The verifier's reading of the document: the declarations it consults and the forms with their reads and writes. */
    private static final class Graph {
        final Map<String, Object> model, rules;
        final List<Map<String, Object>> objectTypes, factTypes;

        Graph(Object document) {
            Map<String, Object> doc = obj(document);
            this.model = obj(doc.get("model"));
            this.rules = obj(doc.get("rules"));
            this.objectTypes = list(model, "objectTypes");
            this.factTypes = list(model, "factTypes");
        }

        // ------------------------------------------------------------ names and types

        static String norm(String s) {
            StringBuilder b = new StringBuilder();
            for (String part : s.split("[ \t]")) {
                if (part.isEmpty()) continue;
                if (b.length() > 0) b.append(' ');
                b.append(part);
            }
            StringBuilder result = new StringBuilder(b.length());
            for (int i = 0; i < b.length(); i++) {
                char c = b.charAt(i);
                result.append(c >= 'A' && c <= 'Z' ? (char) (c + 32) : c);
            }
            return result.toString();
        }

        static String withoutVersion(String name) {
            int i = name.lastIndexOf('#');
            if (i >= 2 && name.charAt(i - 1) == ' ' && i + 1 < name.length()) {
                for (int k = i + 1; k < name.length(); k++) if (name.charAt(k) < '0' || name.charAt(k) > '9') return name;
                return name.substring(0, i - 1);
            }
            return name;
        }

        static String attrNode(String ot, String a) { return "attr:" + norm(ot) + ":" + norm(a); }
        static String charNode(String ot, String k) { return "char:" + norm(ot) + ":" + norm(k); }
        static String chanNode(String rule) { return "chan:" + norm(withoutVersion(rule)); }
        static String popNode(String ot) { return "pop:" + norm(ot); }
        static String relNode(String ft) { return "rel:" + norm(ft); }

        Map<String, Object> objectType(String name) {
            if (name == null) return null;
            for (Map<String, Object> o : objectTypes) if (norm(text(o, "name")).equals(norm(name))) return o;
            return null;
        }

        Map<String, Object> factType(String name) {
            if (name == null) return null;
            for (Map<String, Object> f : factTypes) if (norm(text(f, "name")).equals(norm(name))) return f;
            return null;
        }

        /** The chain from {@code ot} upward, own type first, a name already on the path stopping the walk. */
        List<Map<String, Object>> chain(Map<String, Object> ot) {
            List<Map<String, Object>> result = new ArrayList<>();
            List<String> seen = new ArrayList<>();
            seen.add(norm(text(ot, "name")));
            Map<String, Object> here = ot;
            while (true) {
                result.add(here);
                String s = text(here, "supertype");
                if (s == null || seen.contains(norm(s))) break;
                Map<String, Object> p = objectType(s);
                if (p == null) break;
                seen.add(norm(s));
                here = p;
            }
            return result;
        }

        boolean isSub(String sub, String sup) {
            if (norm(sub).equals(norm(sup))) return true;
            Map<String, Object> ot = objectType(sub);
            if (ot == null) return false;
            for (Map<String, Object> t : chain(ot)) if (norm(text(t, "name")).equals(norm(sup))) return true;
            return false;
        }

        /** Whether {@code name}, with its supertypes, declares the attribute or the characteristic {@code member}. */
        boolean carries(String name, String member, boolean characteristic) {
            Map<String, Object> ot = objectType(name);
            if (ot == null) return false;
            for (Map<String, Object> t : chain(ot))
                for (Map<String, Object> m : list(t, characteristic ? "characteristics" : "attributes"))
                    if (norm(text(m, "name")).equals(norm(member))) return true;
            return false;
        }

        /** {@code member_writes}: the member on {@code ot} and on every type whose instances the write reaches that
         *  carries it — a form that iterates a type reaches the comparable types, a creation its type and the ancestors. */
        List<String> memberWrites(String ot, String member, boolean characteristic, boolean minted) {
            List<String> reached = new ArrayList<>();
            if (minted) {
                Map<String, Object> o = objectType(ot);
                if (o != null) for (Map<String, Object> t : chain(o)) reached.add(text(t, "name"));
            } else {
                for (Map<String, Object> o : objectTypes) {
                    String n = text(o, "name");
                    if (isSub(n, ot) || isSub(ot, n)) reached.add(n);
                }
            }
            TreeSet<String> result = new TreeSet<>();
            result.add(characteristic ? charNode(ot, member) : attrNode(ot, member));
            for (String n : reached) if (carries(n, member, characteristic)) result.add(characteristic ? charNode(n, member) : attrNode(n, member));
            return new ArrayList<>(result);
        }

        /** {@code pop_writes}: a creation grows the population of its type and of every ancestor. */
        List<String> populationWrites(String t) {
            Map<String, Object> ot = objectType(t);
            if (ot == null) return List.of(popNode(t));
            TreeSet<String> result = new TreeSet<>();
            result.add(popNode(t));
            for (Map<String, Object> a : chain(ot)) result.add(popNode(text(a, "name")));
            return new ArrayList<>(result);
        }

        /** The role a binding binds: the declared role of that name in the fact type, null where it does not resolve. */
        Map<String, Object> boundRole(Map<String, Object> binding) {
            Map<String, Object> ft = factType(text(binding, "factType"));
            if (ft == null) return null;
            List<Map<String, Object>> roles = list(ft, "roles");
            String role = text(binding, "role");
            if (roles.size() == 2) {
                boolean a = norm(text(roles.get(0), "name")).equals(norm(role));
                boolean b = norm(text(roles.get(1), "name")).equals(norm(role));
                if (a && !b) return roles.get(0);
                if (b && !a) return roles.get(1);
                return null;
            }
            if (roles.size() == 1 && Boolean.TRUE.equals(ft.get("reciprocal"))
                    && norm(text(roles.get(0), "name")).equals(norm(role)))
                return roles.get(0);
            return null;
        }

        /** {@code connecting_candidates}: the fact types relating {@code van} and {@code to}, in declaration order. */
        List<String> connectingFactTypes(String van, String to) {
            List<String> result = new ArrayList<>();
            for (Map<String, Object> ft : factTypes) {
                List<Map<String, Object>> roles = list(ft, "roles");
                if (roles.size() == 2) {
                    Map<String, Object> a = roles.get(0), b = roles.get(1);
                    if ((fills(van, a) && fills(to, b)) || (fills(van, b) && fills(to, a))) result.add(text(ft, "name"));
                } else if (roles.size() == 1 && Boolean.TRUE.equals(ft.get("reciprocal"))) {
                    if (fills(van, roles.get(0)) && norm(van).equals(norm(to))) result.add(text(ft, "name"));
                }
            }
            return result;
        }

        private boolean fills(String inst, Map<String, Object> role) {
            String fillerType = text(role, "objectType");
            return isSub(inst, fillerType == null ? "" : fillerType);
        }

        // ------------------------------------------------------------ reads (dep_expr_at, dep_cond_at)

        /** The hops of a list of role bindings, as the document spells them. */
        static List<Hop> hops(List<Map<String, Object>> bindings) {
            List<Hop> result = new ArrayList<>();
            for (Map<String, Object> b : bindings) result.add(hop(b));
            return result;
        }

        static Hop hop(Map<String, Object> binding) {
            return new Hop(text(binding, "factType"), text(binding, "role"));
        }

        /** {@code at} followed by {@code hops}; null where no written path reaches {@code at}. */
        static List<Hop> then(List<Hop> at, List<Hop> hops) {
            if (at == null) return null;
            List<Hop> result = new ArrayList<>(at);
            result.addAll(hops);
            return result;
        }

        /** The subject of the form whose reads are collected: an anchored node is read on it (IR-SPEC ch. 4.1a, 5.1). */
        private String formSubject;

        /** {@code unanchored_expr}/{@code unanchored_cond}: {@code n} without its form anchor, read as the same node on the
         *  form's subject, at its own steps (the verifier's {@code ~at:(Some [])}). */
        private static Map<String, Object> unanchored(Map<String, Object> n) {
            Map<String, Object> plain = new LinkedHashMap<>(n);
            plain.remove("anchor");
            return plain;
        }

        /** {@code dep_expr_at}: the reads of {@code e} in front of {@code acc}, as the verifier accumulates them;
         *  {@code at} is the path of the object {@code e} is read on (empty on the subject, null where no written path
         *  reaches it). */
        List<NodeRead> readExpr(String subject, List<Hop> at, List<NodeRead> acc, Object e) {
            if (e == null) return acc;
            Map<String, Object> n = obj(e);
            String node = text(n, "node");
            if (n.get("anchor") != null) return readExpr(formSubject, List.of(), acc, unanchored(n));
            switch (node) {
                case "AttributeReference" -> {
                    String a = text(n, "attribute"), ot = text(n, "subjectType");
                    if (a == null || ot == null) return acc;
                    List<NodeRead> result = new ArrayList<>();
                    for (Map<String, Object> hop : listOrEmpty(n, "path")) result.add(new NodeRead(relNode(text(hop, "factType")), POS));
                    result.add(new NodeRead(attrNode(ot, a), POS, then(at, hops(listOrEmpty(n, "path")))));
                    result.addAll(acc);
                    return result;
                }
                case "TextComposition" -> {
                    for (Map<String, Object> part : list(n, "parts"))
                        if ("EmbeddedExpression".equals(text(part, "node"))) acc = readExpr(subject, at, acc, part.get("expr"));
                    return acc;
                }
                case "Binary" -> { return readExpr(subject, at, readExpr(subject, at, acc, n.get("left")), n.get("right")); }
                case "Aggregation", "BuiltinCall" -> {
                    String el = text(n, "elementType");
                    if (el != null) acc = before(new NodeRead(popNode(el), POS), acc);
                    for (Map<String, Object> rb : listOrEmpty(n, "sourcePath")) acc = before(new NodeRead(relNode(text(rb, "factType")), POS), acc);
                    if (n.get("sourceRole") != null) acc = before(new NodeRead(relNode(text(obj(n.get("sourceRole")), "factType")), POS), acc);
                    // the argument and the filter of an instance fold are read on its elements, at the end of its range
                    List<Hop> elementAt = n.get("sourceRole") != null ? then(at, List.of(hop(obj(n.get("sourceRole")))))
                            : n.get("sourcePath") != null ? then(at, hops(listOrEmpty(n, "sourcePath"))) : at;
                    List<Object> arguments = raw(n, "arguments");
                    boolean countSource = false;
                    if ("Aggregation".equals(node) && "count".equals(text(n, "kind")) && el != null && arguments.size() == 1
                            && (n.get("sourceRole") != null || n.get("sourcePath") != null)) {
                        Map<String, Object> arg = obj(arguments.get(0));
                        String a = text(arg, "attribute");
                        if ("AttributeReference".equals(text(arg, "node")) && a != null && objectType(el) != null)
                            countSource = !(carries(el, a, false) || carries(el, a, true));
                    }
                    if (!countSource) for (Object arg : arguments) acc = readExpr(subject, elementAt, acc, arg);
                    acc = readExpr(subject, at, acc, n.get("emptyFallback"));
                    String fsubject = el != null ? el : subject;
                    if (n.get("filter") != null) acc = readCond(fsubject, elementAt, false, acc, n.get("filter"));
                    return acc;
                }
                case "InstanceSetUnion" -> {
                    for (Map<String, Object> branch : list(n, "branches")) {
                        String el = text(branch, "elementType");
                        if (el != null) acc = before(new NodeRead(popNode(el), POS), acc);
                        acc = before(new NodeRead(relNode(text(obj(branch.get("sourceRole")), "factType")), POS), acc);
                        if (branch.get("filter") != null)
                            acc = readCond(el, then(at, List.of(hop(obj(branch.get("sourceRole"))))), false, acc, branch.get("filter"));
                    }
                    return acc;
                }
                case "Rounding" -> { return readExpr(subject, at, acc, n.get("base")); }
                case "Clamp" -> {
                    return readExpr(subject, at, readExpr(subject, at, readExpr(subject, at, acc, n.get("base")), n.get("minimum")),
                            n.get("maximum"));
                }
                case "DimensionCellRead" -> { return readExpr(subject, at, acc, n.get("attribute")); }
                case "TemporalRead" -> { return readExpr(subject, at, readExpr(subject, at, acc, n.get("base")), n.get("date")); }
                case "Construct" -> { return readExpr(subject, at, acc, n.get("argument")); }
                case "Match" -> {
                    acc = readExpr(subject, at, acc, n.get("scrutinee"));
                    for (Map<String, Object> arm : list(n, "arms")) acc = readExpr(subject, at, acc, arm.get("body"));
                    return acc;
                }
                case "StructLiteral" -> {
                    for (Map<String, Object> field : list(n, "fields")) acc = readExpr(subject, at, acc, field.get("value"));
                    return acc;
                }
                case "FieldAccess" -> { return readExpr(subject, at, acc, n.get("base")); }
                case "Let" -> { return readExpr(subject, at, readExpr(subject, at, acc, n.get("value")), n.get("body")); }
                case "ListLiteral" -> {
                    for (Object x : raw(n, "elements")) acc = readExpr(subject, at, acc, x);
                    return acc;
                }
                case "Lambda" -> { return readExpr(subject, at, acc, n.get("body")); }
                case "IfThenElse" -> {
                    acc = readCond(subject, at, false, acc, n.get("condition"));
                    return readExpr(subject, at, readExpr(subject, at, acc, n.get("then")), n.get("else"));
                }
                case "Invoke" -> {
                    for (Object x : raw(n, "arguments")) acc = readExpr(subject, at, acc, x);
                    return acc;
                }
                case "Fold" -> { return readFunction(subject, at, readExpr(subject, at, readExpr(subject, at, acc, n.get("list")), n.get("init")), n.get("function")); }
                case "MapList" -> { return readFunction(subject, at, readExpr(subject, at, acc, n.get("list")), n.get("function")); }
                case "FilterList" -> { return readCond(subject, at, false, readExpr(subject, at, acc, n.get("list")), n.get("condition")); }
                case "ListLength" -> { return readExpr(subject, at, acc, n.get("list")); }
                default -> { return acc; }   // Literal, ParameterReference, VariableReference, ContextValue, BoundDay, BinderRef, Unresolved
            }
        }

        private List<NodeRead> readFunction(String subject, List<Hop> at, List<NodeRead> acc, Object f) {
            return f instanceof Map<?, ?> ? readExpr(subject, at, acc, obj(f).get("body")) : acc;
        }

        /** {@code dep_cond_at}: the reads of {@code c} in front of {@code acc}; {@code neg} marks a negative context. */
        List<NodeRead> readCond(String subject, List<Hop> at, boolean neg, List<NodeRead> acc, Object c) {
            if (c == null) return acc;
            Map<String, Object> n = obj(c);
            // the identity check reads no node (IR-SPEC ch. 5.1); every other anchored node is read on the form's subject
            if (n.get("anchor") != null && !"IdentityCheck".equals(text(n, "node")))
                return readCond(formSubject, List.of(), neg, acc, unanchored(n));
            switch (text(n, "node")) {
                case "Comparison" -> { return readExpr(subject, at, readExpr(subject, at, acc, n.get("left")), n.get("right")); }
                case "Predicate" -> {
                    String category = text(n, "kind");
                    if ("empty".equals(category) || "filled".equals(category)) {
                        List<NodeRead> result = new ArrayList<>();
                        for (NodeRead l : readExpr(subject, at, List.of(), n.get("operand"))) result.add(new NodeRead(l.graphNode(), NEG, l.path()));
                        result.addAll(acc);
                        return result;
                    }
                    return readExpr(subject, at, acc, n.get("operand"));
                }
                case "CharacteristicCheck" -> {
                    String k = text(n, "characteristic");
                    if (k == null) return acc;
                    String category = neg || Boolean.TRUE.equals(n.get("negated")) ? NEG : POS;
                    if (n.get("navigation") != null) {
                        Map<String, Object> rb = obj(n.get("navigation"));
                        Map<String, Object> role = boundRole(rb);
                        if (role == null) return acc;
                        String reached = text(role, "objectType");
                        List<NodeRead> result = new ArrayList<>();
                        result.add(new NodeRead(relNode(text(rb, "factType")), POS));
                        result.add(new NodeRead(charNode(reached == null ? "" : reached, k), category, then(at, List.of(hop(rb)))));
                        result.addAll(acc);
                        return result;
                    }
                    // a holder other than the subject is the other object in scope of a fact creation, which no written
                    // path reaches
                    String written = text(n, "host");
                    List<Hop> path = written == null || (subject != null && norm(written).equals(norm(subject))) ? at : null;
                    String host = written != null ? written : subject;
                    return host == null ? acc : before(new NodeRead(charNode(host, k), category, path), acc);
                }
                case "TimeWindow" -> { return readExpr(subject, at, readExpr(subject, at, acc, n.get("from")), n.get("to")); }
                case "Compound" -> {
                    String q = text(n, "quantifier");
                    boolean neg2 = "none".equals(q) || "atMost".equals(q) || "exactly".equals(q) || neg;
                    for (Object part : raw(n, "parts")) acc = readCond(subject, at, neg2, acc, part);
                    return acc;
                }
                case "QuantifierFold" -> {
                    String q = text(n, "quantifier");
                    boolean rangeNeg = neg || !"atLeast".equals(q);
                    boolean matrixNeg = neg || !("atLeast".equals(q) || "all".equals(q));
                    String el = text(n, "elementType");
                    List<NodeRead> result = new ArrayList<>();
                    List<Hop> reach = new ArrayList<>();
                    if (n.get("sourceRole") != null) {
                        result.add(new NodeRead(relNode(text(obj(n.get("sourceRole")), "factType")), rangeNeg ? NEG : POS));
                        reach.add(hop(obj(n.get("sourceRole"))));
                    }
                    for (Map<String, Object> rb : listOrEmpty(n, "sourcePath"))
                        result.add(new NodeRead(relNode(text(rb, "factType")), rangeNeg ? NEG : POS));
                    reach.addAll(hops(listOrEmpty(n, "sourcePath")));
                    // filter and test are read on the members, at the end of the range
                    List<Hop> memberAt = then(at, reach);
                    result.addAll(part(el, memberAt, rangeNeg, n.get("filter")));
                    result.addAll(part(el, memberAt, matrixNeg, n.get("test")));
                    result.addAll(acc);
                    return result;
                }
                case "Regelversie" -> {
                    String r = text(n, "regel");
                    return r == null ? acc : before(new NodeRead(chanNode(r), NEG), acc);
                }
                case "RoleMembership" -> {
                    // the link set of the named relation; negated, the check holds on absence
                    String category = neg || Boolean.TRUE.equals(n.get("negated")) ? NEG : POS;
                    return before(new NodeRead(relNode(text(n, "factType")), category), acc);
                }
                default -> { return acc; }   // Unresolved
            }
        }

        private List<NodeRead> part(String element, List<Hop> at, boolean negative, Object c) {
            if (c == null) return List.of();
            List<NodeRead> reads = readCond(element, at, negative, List.of(), c);
            if (!negative) return reads;
            return negative(reads);
        }

        private static List<NodeRead> before(NodeRead l, List<NodeRead> acc) {
            List<NodeRead> result = new ArrayList<>(acc.size() + 1);
            result.add(l);
            result.addAll(acc);
            return result;
        }

        private static List<NodeRead> strict(List<NodeRead> reads) {
            List<NodeRead> result = new ArrayList<>();
            for (NodeRead l : reads)
                result.add(POS.equals(l.category()) && !l.graphNode().startsWith("char:")
                        ? new NodeRead(l.graphNode(), STRICT, l.path()) : l);
            return result;
        }

        private static List<NodeRead> negative(List<NodeRead> reads) {
            List<NodeRead> result = new ArrayList<>();
            for (NodeRead l : reads) result.add(new NodeRead(l.graphNode(), NEG, l.path()));
            return result;
        }

        private static List<NodeRead> plus(List<NodeRead> a, List<NodeRead> b) {
            List<NodeRead> result = new ArrayList<>(a);
            result.addAll(b);
            return result;
        }

        // ------------------------------------------------------------ the forms (dep_forms)

        /** A named form's versions: per version its in-memory name ("&lt;naam&gt; #i" where there are several), its
         *  pointer and its node. */
        private record Version(String name, String at, Map<String, Object> irNode) {}

        private List<Version> versions(String list) {
            List<Version> result = new ArrayList<>();
            List<Map<String, Object>> forms = list(rules, list);
            for (int f = 0; f < forms.size(); f++) {
                Map<String, Object> form = forms.get(f);
                String name = text(form, "name");
                List<Map<String, Object>> vs = list(form, "versions");
                for (int v = 0; v < vs.size(); v++)
                    result.add(new Version(name == null ? null : vs.size() >= 2 ? name + " #" + (v + 1) : name,
                            "/rules/" + list + "/" + f + "/versions/" + v, vs.get(v)));
            }
            return result;
        }

        private List<String> recursiveMembers() {
            List<String> result = new ArrayList<>();
            for (Map<String, Object> g : list(rules, "ruleGroups"))
                if (Boolean.TRUE.equals(g.get("recursive")))
                    for (Object member : raw(g, "members")) if (member instanceof String s) result.add(norm(withoutVersion(s)));
            return result;
        }

        /** The reads of a condition read on the object {@code at} names. */
        private List<NodeRead> condition(String subject, List<Hop> at, Object c) {
            return c == null ? List.of() : readCond(subject, at, false, List.of(), c);
        }

        List<Form> forms() {
            List<String> recursive = recursiveMembers();
            List<Form> result = new ArrayList<>();
            // the rule versions
            for (Version v : versions("rules")) {
                Map<String, Object> target = obj(v.irNode().get("target"));
                String a = text(target, "attribute"), ot = text(target, "subjectType");
                if (a == null || ot == null) continue;
                boolean characteristic = !Boolean.TRUE.equals(v.irNode().get("initialisation")) && v.irNode().get("presentation") == null;
                List<String> writes = memberWrites(ot, a, characteristic, false);
                // a rule writes per subject of its type: its reads carry their paths from the subject (ch. 5.3)
                formSubject = ot;
                List<NodeRead> uses = readExpr(ot, List.of(), condition(ot, List.of(), v.irNode().get("condition")),
                        v.irNode().get("value"));
                if (characteristic) uses = strict(uses);
                else if (!absorbingTypes(ot, a).isEmpty()) uses = negative(uses);
                result.add(new Form(v.at(), v.name() == null ? v.at() : v.name(), "rules", false, writes, uses));
            }
            // the allocations
            for (Version v : versions("allocations")) {
                Map<String, Object> k = v.irNode();
                String bt = value(text(k, "sourceType")), dt = value(text(k, "targetType"));
                List<String> writes = new ArrayList<>();
                if (text(k, "targetAttribute") != null) writes.addAll(memberWrites(dt, text(k, "targetAttribute"), false, false));
                if (text(k, "remainderAttribute") != null) writes.addAll(memberWrites(bt, text(k, "remainderAttribute"), false, false));
                List<NodeRead> uses = new ArrayList<>();
                uses.add(new NodeRead(popNode(dt), POS));
                uses.add(new NodeRead(relNode(text(obj(k.get("connectingRole")), "factType")), POS));
                if (text(k, "sourceAttribute") != null) uses.add(new NodeRead(attrNode(bt, text(k, "sourceAttribute")), POS));
                for (String field : List.of("ratioAttribute", "maximumAttribute", "orderingAttribute"))
                    if (text(k, field) != null) uses.add(new NodeRead(attrNode(dt, text(k, field)), POS));
                formSubject = text(k, "sourceType");
                uses.addAll(condition(text(k, "sourceType"), null, k.get("condition")));
                result.add(new Form(v.at(), v.name() == null ? v.at() : v.name(), "allocations", false, writes, uses));
            }
            // the consistencies
            for (Version v : versions("consistencies")) {
                String name = v.name() == null ? v.at() : v.name();
                formSubject = text(v.irNode(), "subject");
                result.add(new Form(v.at(), name, "consistencies", false, List.of(chanNode(name)),
                        strict(condition(text(v.irNode(), "subject"), null, v.irNode().get("assertion")))));
            }
            // the uniquenesses of one source, then those of several
            List<Version> unique = versions("uniquenesses");
            for (Version v : unique) {
                List<Map<String, Object>> sources = list(v.irNode(), "sources");
                if (sources.size() != 1) continue;
                String name = v.name() == null ? v.at() : v.name();
                String subj = value(text(sources.get(0), "objectType"));
                List<NodeRead> uses = new ArrayList<>();
                if (v.irNode().get("scope") != null)
                    uses.add(new NodeRead(relNode(text(obj(v.irNode().get("scope")), "factType")), STRICT));
                for (Object key : raw(sources.get(0), "keys"))
                    uses.add(new NodeRead(attrNode(subj, key instanceof String s ? s : ""), STRICT));
                result.add(new Form(v.at(), name, "uniquenesses", false, List.of(chanNode(name)), uses));
            }
            for (Version v : unique) {
                List<Map<String, Object>> sources = list(v.irNode(), "sources");
                if (sources.size() == 1) continue;
                String name = v.name() == null ? v.at() : v.name();
                List<NodeRead> uses = new ArrayList<>();
                for (Map<String, Object> b : sources) {
                    String t = text(b, "objectType");
                    if (t == null) continue;
                    for (Object key : raw(b, "keys")) uses.add(new NodeRead(attrNode(t, key instanceof String s ? s : ""), STRICT));
                }
                result.add(new Form(v.at(), name, "uniquenesses", false, List.of(chanNode(name)), uses));
            }
            // the object creations
            for (Version v : versions("objectCreations")) {
                Map<String, Object> k = v.irNode();
                String name = v.name() == null ? v.at() : v.name();
                String nt = value(text(k, "createdType")), subject = text(k, "subject");
                List<String> writes = new ArrayList<>(populationWrites(nt));
                if (k.get("connectingRole") != null) {
                    writes.add(relNode(text(obj(k.get("connectingRole")), "factType")));
                } else if (subject != null) {
                    List<String> candidates = connectingFactTypes(subject, nt);
                    if (candidates.size() == 1) {
                        Map<String, Object> ft = factType(candidates.get(0));
                        if (!(ft != null && norm(subject).equals(norm(nt)) && !Boolean.TRUE.equals(ft.get("reciprocal"))))
                            writes.add(relNode(candidates.get(0)));
                    }
                }
                for (Map<String, Object> t : list(k, "initialisations"))
                    if (text(t, "member") != null)
                        writes.addAll(memberWrites(nt, text(t, "member"), Boolean.TRUE.equals(t.get("characteristic")), true));
                List<NodeRead> uses = new ArrayList<>();
                if (subject != null) uses.add(new NodeRead(popNode(subject), POS));
                formSubject = subject;
                List<NodeRead> values = condition(subject, null, k.get("condition"));
                for (Map<String, Object> t : list(k, "initialisations")) values = readExpr(subject, null, values, t.get("value"));
                uses.addAll(values);
                result.add(new Form(v.at(), name, "objectCreations", recursive.contains(norm(withoutVersion(name))), writes,
                        strict(uses)));
            }
            // the fact creations
            for (Version v : versions("factCreations")) {
                Map<String, Object> k = v.irNode();
                String name = v.name() == null ? v.at() : v.name();
                List<Map<String, Object>> path = list(k, "path");
                List<NodeRead> uses = new ArrayList<>();
                for (Map<String, Object> hop : path) uses.add(new NodeRead(relNode(text(hop, "factType")), POS));
                formSubject = text(k, "anchor");
                uses.addAll(condition(text(k, "anchor"), null, k.get("condition")));
                // the created relation and no population: a fact creation links instances that exist
                List<String> writes = List.of(relNode(text(k, "factType")));
                result.add(new Form(v.at(), name, "factCreations", recursive.contains(norm(withoutVersion(name))), writes,
                        strict(uses)));
            }
            // the defaults groups
            List<Map<String, Object>> groups = list(rules, "prioritizedDefaults");
            for (int i = 0; i < groups.size(); i++) {
                Map<String, Object> l = groups.get(i);
                String at = "/prioritizedDefaults/" + i;
                String name = text(l, "name") == null ? at : text(l, "name");
                String subjectType = text(l, "subject");
                List<NodeRead> uses = new ArrayList<>();
                formSubject = subjectType;
                for (Map<String, Object> d : list(l, "defaults")) uses.addAll(defaultReads(subjectType, d));
                List<String> writes = new ArrayList<>();
                if (text(l, "characteristic") != null)
                    writes.addAll(memberWrites(subjectType, text(l, "characteristic"), true, false));
                if (text(l, "attribute") != null)
                    writes.addAll(memberWrites(subjectType, text(l, "attribute"), false, false));
                result.add(new Form(at, name, "prioritizedDefaults", false, writes, negative(uses)));
            }
            return result;
        }

        private List<NodeRead> defaultReads(String subjectType, Map<String, Object> d) {
            List<NodeRead> result = new ArrayList<>(condition(subjectType, List.of(), d.get("justification")));
            if (d.get("consequence") != null) result.addAll(readExpr(subjectType, List.of(), List.of(), d.get("consequence")));
            if (d.get("exceptions") != null)
                for (Map<String, Object> eg : list(obj(d.get("exceptions")), "members"))
                    for (Map<String, Object> sub : list(eg, "defaults")) result.addAll(defaultReads(subjectType, sub));
            return result;
        }

        /** {@code absorbing_types}: the types at or below {@code ot} whose chain declares a defaults group on the
         *  attribute, unless {@code ot} declares one itself. */
        private List<String> absorbingTypes(String ot, String attr) {
            String key = "A:" + norm(attr);
            for (Map<String, Object> l : list(rules, "prioritizedDefaults"))
                if (norm(text(l, "subject")).equals(norm(ot)) && targetKey(l).equals(key)) return List.of();
            List<String> result = new ArrayList<>();
            for (Map<String, Object> o : objectTypes) {
                String t = text(o, "name");
                if (isSub(t, ot) && groupKeys(t).contains(key)) result.add(t);
            }
            return result;
        }

        private static String targetKey(Map<String, Object> l) {
            String k = text(l, "characteristic"), a = text(l, "attribute");
            return (k != null ? "K:" : "A:") + (k != null ? norm(k) : a != null ? norm(a) : "?");
        }

        private TreeSet<String> groupKeys(String t) {
            TreeSet<String> result = new TreeSet<>();
            for (Map<String, Object> o : objectTypes) {
                String s = text(o, "name");
                if (!isSub(t, s)) continue;
                for (Map<String, Object> l : list(rules, "prioritizedDefaults"))
                    if (norm(text(l, "subject")).equals(norm(s))) result.add(targetKey(l));
            }
            return result;
        }

        // ------------------------------------------------------------ the document's shapes

        @SuppressWarnings("unchecked")
        static Map<String, Object> obj(Object x) {
            return x instanceof Map<?, ?> m ? (Map<String, Object>) m : Map.of();
        }

        static String text(Map<String, Object> m, String field) {
            return m.get(field) instanceof String s ? s : null;
        }

        private static String value(String s) { return s == null ? "" : s; }

        static List<Object> raw(Map<String, Object> m, String field) {
            return m.get(field) instanceof List<?> l ? Collections.unmodifiableList(l) : List.of();
        }

        List<Map<String, Object>> list(Map<String, Object> m, String field) { return listOrEmpty(m, field); }

        static List<Map<String, Object>> listOrEmpty(Map<String, Object> m, String field) {
            List<Map<String, Object>> result = new ArrayList<>();
            for (Object x : raw(m, field)) result.add(obj(x));
            return result;
        }
    }

    // ---------------------------------------------------------------- reading the document

    /** A reader for the JSON this frontend's emitter writes: objects keep their key order, a number is a BigDecimal. */
    static final class Json {
        private final String s;
        private int i;

        private Json(String s) { this.s = s; }

        static Object parse(String text) {
            Json j = new Json(text);
            Object v = j.value();
            j.whitespace();
            if (j.i != j.s.length()) throw new IllegalArgumentException("JSON: tekst na het document op " + j.i);
            return v;
        }

        private void whitespace() {
            while (i < s.length() && (s.charAt(i) == ' ' || s.charAt(i) == '\n' || s.charAt(i) == '\r' || s.charAt(i) == '\t')) i++;
        }

        private Object value() {
            whitespace();
            if (i >= s.length()) throw new IllegalArgumentException("JSON: einde zonder waarde");
            char c = s.charAt(i);
            switch (c) {
                case '{' -> {
                    i++;
                    Map<String, Object> m = new LinkedHashMap<>();
                    whitespace();
                    if (s.charAt(i) == '}') { i++; return m; }
                    while (true) {
                        whitespace();
                        String k = string();
                        whitespace();
                        expected(':');
                        m.put(k, value());
                        whitespace();
                        if (s.charAt(i) == ',') { i++; continue; }
                        expected('}');
                        return m;
                    }
                }
                case '[' -> {
                    i++;
                    List<Object> l = new ArrayList<>();
                    whitespace();
                    if (s.charAt(i) == ']') { i++; return l; }
                    while (true) {
                        l.add(value());
                        whitespace();
                        if (s.charAt(i) == ',') { i++; continue; }
                        expected(']');
                        return l;
                    }
                }
                case '"' -> { return string(); }
                case 't' -> { word("true"); return Boolean.TRUE; }
                case 'f' -> { word("false"); return Boolean.FALSE; }
                case 'n' -> { word("null"); return null; }
                default -> {
                    int b = i;
                    while (i < s.length() && "+-0123456789.eE".indexOf(s.charAt(i)) >= 0) i++;
                    if (b == i) throw new IllegalArgumentException("JSON: onverwacht teken '" + c + "' op " + i);
                    return new BigDecimal(s.substring(b, i));
                }
            }
        }

        private void word(String w) {
            if (!s.startsWith(w, i)) throw new IllegalArgumentException("JSON: '" + w + "' verwacht op " + i);
            i += w.length();
        }

        private void expected(char c) {
            if (i >= s.length() || s.charAt(i) != c) throw new IllegalArgumentException("JSON: '" + c + "' verwacht op " + i);
            i++;
        }

        private String string() {
            expected('"');
            StringBuilder b = new StringBuilder();
            while (true) {
                char c = s.charAt(i++);
                if (c == '"') return b.toString();
                if (c != '\\') { b.append(c); continue; }
                char e = s.charAt(i++);
                switch (e) {
                    case '"' -> b.append('"');
                    case '\\' -> b.append('\\');
                    case '/' -> b.append('/');
                    case 'b' -> b.append('\b');
                    case 'f' -> b.append('\f');
                    case 'n' -> b.append('\n');
                    case 'r' -> b.append('\r');
                    case 't' -> b.append('\t');
                    case 'u' -> { b.append((char) Integer.parseInt(s.substring(i, i + 4), 16)); i += 4; }
                    default -> throw new IllegalArgumentException("JSON: onbekende escape '\\" + e + "' op " + i);
                }
            }
        }
    }
}
