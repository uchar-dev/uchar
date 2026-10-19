package dev.uchar.regelspraak.resolver;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
 * so the two can be compared line for line.
 */
final class Dependencies {

    private Dependencies() {}

    /** A violation of WF-045 or WF-051 as the verifier reports it: the rule, the pointer, its sentence, and the forms of
     *  the cycle ({@code forms}: for WF-045 the cycle in the walk's order, for WF-051 the group alone). */
    record Cycle(String wf, String at, String sentence, List<Form> forms) {}

    /** A form of the graph: its pointer, its name (a version of a form with several is "&lt;naam&gt; #i"), whether it
     *  is a creation in a recursive rule group, what it writes and what it reads; {@code category} is the list it comes
     *  from. */
    record Form(String at, String naam, String category, boolean recursiveCreation, List<String> writes, List<NodeRead> uses) {}

    /** One read: the node and its edge kind (positive, strict, negative; ch. 5.2). */
    record NodeRead(String graphNode, String category) {}

    private record Edge(int producer, int consumer, String graphNode, String category) {}

    // ---------------------------------------------------------------- the two rules

    /** The WF-045 and WF-051 violations of {@code ir}, in the order and the sentences of the verifier. */
    static List<Cycle> cycles(String ir) {
        Graph g = new Graph(Json.parse(ir));
        List<Form> forms = g.forms();
        List<Cycle> result = new ArrayList<>(wf045(forms, g));
        result.addAll(wf051(forms, g));
        return result;
    }

    /** The graph as {@code verifier --dependency-edges} prints it: one tab-separated line per edge — the kind, the
     *  producer's pointer and name, the node, the consumer's pointer and name — sorted, without repeats. */
    static List<String> edges(String ir) {
        Graph g = new Graph(Json.parse(ir));
        List<Form> forms = g.forms();
        TreeSet<String> result = new TreeSet<>(Dependencies::compareBytes);
        for (Edge r : edges(forms)) {
            Form p = forms.get(r.producer()), c = forms.get(r.consumer());
            result.add(String.join("\t", r.category(), m1Root(p.at()), p.naam(), r.graphNode(), m1Root(c.at()), c.naam()));
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

    /** {@code naam} without the " #i" of a version of a form with several ({@code strip_version}). */
    static String withoutVersion(String naam) { return Graph.withoutVersion(naam); }

    private static List<Cycle> wf045(List<Form> forms, Graph g) {
        int n = forms.size();
        List<List<int[]>> followers = opvolgers(forms);
        int[] kleur = new int[n];
        List<Cycle> result = new ArrayList<>();
        for (int i = 0; i < n; i++) if (kleur[i] == 0) deep(forms, followers, kleur, new ArrayList<>(), i, result);
        return result;
    }

    /** The verifier's walk: a node already on the path closes a cycle, which is reported from that node on. The path
     *  is kept most recent first, as the verifier's list is. */
    private static void deep(List<Form> forms, List<List<int[]>> followers, int[] kleur, List<Integer> path, int i,
                             List<Cycle> result) {
        if (forms.get(i).recursiveCreation()) return;
        if (kleur[i] == 1) {
            List<Integer> cycle = new ArrayList<>();
            for (int j : path) {
                cycle.add(0, j);
                if (j == i) break;
            }
            List<String> names = new ArrayList<>();
            List<Form> members = new ArrayList<>();
            for (int j : cycle) { names.add(forms.get(j).naam()); members.add(forms.get(j)); }
            names.add(forms.get(i).naam());
            result.add(new Cycle("WF-045", m1Root(forms.get(i).at()),
                    "dependency cycle through no recursive creation: " + String.join(" -> ", names), members));
            return;
        }
        if (kleur[i] == 2) return;
        kleur[i] = 1;
        List<Integer> further = new ArrayList<>();
        further.add(i);
        further.addAll(path);
        for (int[] j : followers.get(i)) deep(forms, followers, kleur, further, j[0], result);
        kleur[i] = 2;
    }

    /** The successors of every form, as the verifier builds them: for each edge in reading order, the consumer with the
     *  edge's kind put in FRONT of the producer's list unless that pair is there already, so a list is walked from the
     *  last edge inserted. An entry is {consumer, kind index}. */
    private static List<List<int[]>> opvolgers(List<Form> forms) {
        List<List<int[]>> followers = new ArrayList<>();
        for (int i = 0; i < forms.size(); i++) followers.add(new ArrayList<>());
        for (Edge r : edges(forms)) {
            List<int[]> l = followers.get(r.producer());
            int category = kindIndex(r.category());
            boolean already = false;
            for (int[] x : l) if (x[0] == r.consumer() && x[1] == category) { already = true; break; }
            if (!already) l.add(0, new int[] {r.consumer(), category});
        }
        return followers;
    }

    private static List<Cycle> wf051(List<Form> forms, Graph g) {
        int n = forms.size();
        Map<String, List<Integer>> writers = writers(forms);
        List<List<Integer>> followers = new ArrayList<>();
        for (int i = 0; i < n; i++) followers.add(new ArrayList<>());
        for (int ci = 0; ci < n; ci++)
            for (NodeRead l : forms.get(ci).uses())
                for (int pi : writers.getOrDefault(l.graphNode(), List.of())) followers.get(pi).add(0, ci);
        List<Cycle> result = new ArrayList<>();
        List<Map<String, Object>> groups = g.list(g.regels, "prioritizedDefaults");
        for (int i = 0; i < groups.size(); i++) {
            String at = "/prioritizedDefaults/" + i;
            for (int fi = 0; fi < n; fi++) {
                if (!forms.get(fi).at().equals(at)) continue;
                if (reached(followers, fi, fi, new boolean[n])) {
                    String naam = Graph.tekst(groups.get(i), "name");
                    result.add(new Cycle("WF-051", m1Root(at), "a dependency cycle passes through defaults group '"
                            + (naam == null ? at : naam) + "' — its displacement reads are negative edges; no strata exist",
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

    // ---------------------------------------------------------------- the graph over the document

    /** The verifier's reading of the document: the declarations it consults and the forms with their reads and writes. */
    private static final class Graph {
        final Map<String, Object> model, regels;
        final List<Map<String, Object>> objecttypen, feittypen;

        Graph(Object document) {
            Map<String, Object> doc = obj(document);
            this.model = obj(doc.get("model"));
            this.regels = obj(doc.get("rules"));
            this.objecttypen = list(model, "objectTypes");
            this.feittypen = list(model, "factTypes");
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

        static String withoutVersion(String naam) {
            int i = naam.lastIndexOf('#');
            if (i >= 2 && naam.charAt(i - 1) == ' ' && i + 1 < naam.length()) {
                for (int k = i + 1; k < naam.length(); k++) if (naam.charAt(k) < '0' || naam.charAt(k) > '9') return naam;
                return naam.substring(0, i - 1);
            }
            return naam;
        }

        static String attrNode(String ot, String a) { return "attr:" + norm(ot) + ":" + norm(a); }
        static String charNode(String ot, String k) { return "char:" + norm(ot) + ":" + norm(k); }
        static String chanNode(String regel) { return "chan:" + norm(withoutVersion(regel)); }
        static String popNode(String ot) { return "pop:" + norm(ot); }
        static String relNode(String ft) { return "rel:" + norm(ft); }

        Map<String, Object> objectType(String naam) {
            if (naam == null) return null;
            for (Map<String, Object> o : objecttypen) if (norm(tekst(o, "name")).equals(norm(naam))) return o;
            return null;
        }

        Map<String, Object> factType(String naam) {
            if (naam == null) return null;
            for (Map<String, Object> f : feittypen) if (norm(tekst(f, "name")).equals(norm(naam))) return f;
            return null;
        }

        /** The chain from {@code ot} upward, own type first, a name already on the path stopping the walk. */
        List<Map<String, Object>> chain(Map<String, Object> ot) {
            List<Map<String, Object>> result = new ArrayList<>();
            List<String> seen = new ArrayList<>();
            seen.add(norm(tekst(ot, "name")));
            Map<String, Object> here = ot;
            while (true) {
                result.add(here);
                String s = tekst(here, "supertype");
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
            for (Map<String, Object> t : chain(ot)) if (norm(tekst(t, "name")).equals(norm(sup))) return true;
            return false;
        }

        /** Whether {@code naam}, with its supertypes, declares the attribute or the characteristic {@code member}. */
        boolean carries(String naam, String member, boolean characteristic) {
            Map<String, Object> ot = objectType(naam);
            if (ot == null) return false;
            for (Map<String, Object> t : chain(ot))
                for (Map<String, Object> m : list(t, characteristic ? "characteristics" : "attributes"))
                    if (norm(tekst(m, "name")).equals(norm(member))) return true;
            return false;
        }

        /** {@code member_writes}: the member on {@code ot} and on every type whose instances the write reaches that
         *  carries it — a form that iterates a type reaches the comparable types, a creation its type and the ancestors. */
        List<String> memberWrites(String ot, String member, boolean characteristic, boolean minted) {
            List<String> reached = new ArrayList<>();
            if (minted) {
                Map<String, Object> o = objectType(ot);
                if (o != null) for (Map<String, Object> t : chain(o)) reached.add(tekst(t, "name"));
            } else {
                for (Map<String, Object> o : objecttypen) {
                    String n = tekst(o, "name");
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
            for (Map<String, Object> a : chain(ot)) result.add(popNode(tekst(a, "name")));
            return new ArrayList<>(result);
        }

        /** The role a binding binds: the declared role of that name in the fact type, null where it does not resolve. */
        Map<String, Object> boundRole(Map<String, Object> binding) {
            Map<String, Object> ft = factType(tekst(binding, "factType"));
            if (ft == null) return null;
            List<Map<String, Object>> rollen = list(ft, "roles");
            String role = tekst(binding, "role");
            if (rollen.size() == 2) {
                boolean a = norm(tekst(rollen.get(0), "name")).equals(norm(role));
                boolean b = norm(tekst(rollen.get(1), "name")).equals(norm(role));
                if (a && !b) return rollen.get(0);
                if (b && !a) return rollen.get(1);
                return null;
            }
            if (rollen.size() == 1 && Boolean.TRUE.equals(ft.get("reciprocal"))
                    && norm(tekst(rollen.get(0), "name")).equals(norm(role)))
                return rollen.get(0);
            return null;
        }

        /** {@code connecting_candidates}: the fact types relating {@code van} and {@code to}, in declaration order. */
        List<String> connectingFactTypes(String van, String to) {
            List<String> result = new ArrayList<>();
            for (Map<String, Object> ft : feittypen) {
                List<Map<String, Object>> rollen = list(ft, "roles");
                if (rollen.size() == 2) {
                    Map<String, Object> a = rollen.get(0), b = rollen.get(1);
                    if ((fills(van, a) && fills(to, b)) || (fills(van, b) && fills(to, a))) result.add(tekst(ft, "name"));
                } else if (rollen.size() == 1 && Boolean.TRUE.equals(ft.get("reciprocal"))) {
                    if (fills(van, rollen.get(0)) && norm(van).equals(norm(to))) result.add(tekst(ft, "name"));
                }
            }
            return result;
        }

        private boolean fills(String inst, Map<String, Object> role) {
            String fillerType = tekst(role, "objectType");
            return isSub(inst, fillerType == null ? "" : fillerType);
        }

        // ------------------------------------------------------------ reads (dep_expr, dep_cond)

        /** {@code dep_expr}: the reads of {@code e} in front of {@code acc}, as the verifier accumulates them. */
        List<NodeRead> readExpr(String subject, List<NodeRead> acc, Object e) {
            if (e == null) return acc;
            Map<String, Object> n = obj(e);
            String node = tekst(n, "node");
            switch (node) {
                case "AttributeReference" -> {
                    String a = tekst(n, "attribute"), ot = tekst(n, "subjectType");
                    if (a == null || ot == null) return acc;
                    List<NodeRead> result = new ArrayList<>();
                    for (Map<String, Object> hop : listOrEmpty(n, "path")) result.add(new NodeRead(relNode(tekst(hop, "factType")), POS));
                    result.add(new NodeRead(attrNode(ot, a), POS));
                    result.addAll(acc);
                    return result;
                }
                case "TextComposition" -> {
                    for (Map<String, Object> part : list(n, "parts"))
                        if ("EmbeddedExpression".equals(tekst(part, "node"))) acc = readExpr(subject, acc, part.get("expr"));
                    return acc;
                }
                case "Binary" -> { return readExpr(subject, readExpr(subject, acc, n.get("left")), n.get("right")); }
                case "Aggregation", "BuiltinCall" -> {
                    String el = tekst(n, "elementType");
                    if (el != null) acc = before(new NodeRead(popNode(el), POS), acc);
                    for (Map<String, Object> rb : listOrEmpty(n, "sourcePath")) acc = before(new NodeRead(relNode(tekst(rb, "factType")), POS), acc);
                    if (n.get("sourceRole") != null) acc = before(new NodeRead(relNode(tekst(obj(n.get("sourceRole")), "factType")), POS), acc);
                    List<Object> arguments = raw(n, "arguments");
                    boolean countSource = false;
                    if ("Aggregation".equals(node) && "count".equals(tekst(n, "kind")) && el != null && arguments.size() == 1
                            && (n.get("sourceRole") != null || n.get("sourcePath") != null)) {
                        Map<String, Object> arg = obj(arguments.get(0));
                        String a = tekst(arg, "attribute");
                        if ("AttributeReference".equals(tekst(arg, "node")) && a != null && objectType(el) != null)
                            countSource = !(carries(el, a, false) || carries(el, a, true));
                    }
                    if (!countSource) for (Object arg : arguments) acc = readExpr(subject, acc, arg);
                    acc = readExpr(subject, acc, n.get("emptyFallback"));
                    String fsubject = el != null ? el : subject;
                    if (n.get("filter") != null) acc = readCond(fsubject, false, acc, n.get("filter"));
                    return acc;
                }
                case "InstanceSetUnion" -> {
                    for (Map<String, Object> branch : list(n, "branches")) {
                        String el = tekst(branch, "elementType");
                        if (el != null) acc = before(new NodeRead(popNode(el), POS), acc);
                        acc = before(new NodeRead(relNode(tekst(obj(branch.get("sourceRole")), "factType")), POS), acc);
                        if (branch.get("filter") != null) acc = readCond(el, false, acc, branch.get("filter"));
                    }
                    return acc;
                }
                case "Rounding" -> { return readExpr(subject, acc, n.get("base")); }
                case "Clamp" -> {
                    return readExpr(subject, readExpr(subject, readExpr(subject, acc, n.get("base")), n.get("minimum")),
                            n.get("maximum"));
                }
                case "DimensionCellRead" -> { return readExpr(subject, acc, n.get("attribute")); }
                case "TemporalRead" -> { return readExpr(subject, readExpr(subject, acc, n.get("base")), n.get("date")); }
                case "Construct" -> { return readExpr(subject, acc, n.get("argument")); }
                case "Match" -> {
                    acc = readExpr(subject, acc, n.get("scrutinee"));
                    for (Map<String, Object> arm : list(n, "arms")) acc = readExpr(subject, acc, arm.get("body"));
                    return acc;
                }
                case "StructLiteral" -> {
                    for (Map<String, Object> field : list(n, "fields")) acc = readExpr(subject, acc, field.get("value"));
                    return acc;
                }
                case "FieldAccess" -> { return readExpr(subject, acc, n.get("base")); }
                case "Let" -> { return readExpr(subject, readExpr(subject, acc, n.get("value")), n.get("body")); }
                case "ListLiteral" -> {
                    for (Object x : raw(n, "elements")) acc = readExpr(subject, acc, x);
                    return acc;
                }
                case "Lambda" -> { return readExpr(subject, acc, n.get("body")); }
                case "IfThenElse" -> {
                    acc = readCond(subject, false, acc, n.get("condition"));
                    return readExpr(subject, readExpr(subject, acc, n.get("then")), n.get("else"));
                }
                case "Invoke" -> {
                    for (Object x : raw(n, "arguments")) acc = readExpr(subject, acc, x);
                    return acc;
                }
                case "Fold" -> { return readFunction(subject, readExpr(subject, readExpr(subject, acc, n.get("list")), n.get("init")), n.get("function")); }
                case "MapList" -> { return readFunction(subject, readExpr(subject, acc, n.get("list")), n.get("function")); }
                case "FilterList" -> { return readCond(subject, false, readExpr(subject, acc, n.get("list")), n.get("condition")); }
                case "ListLength" -> { return readExpr(subject, acc, n.get("list")); }
                default -> { return acc; }   // Literal, ParameterReference, VariableReference, ContextValue, BoundDay, BinderRef, Unresolved
            }
        }

        private List<NodeRead> readFunction(String subject, List<NodeRead> acc, Object f) {
            return f instanceof Map<?, ?> ? readExpr(subject, acc, obj(f).get("body")) : acc;
        }

        /** {@code dep_cond}: the reads of {@code c} in front of {@code acc}; {@code neg} marks a negative context. */
        List<NodeRead> readCond(String subject, boolean neg, List<NodeRead> acc, Object c) {
            if (c == null) return acc;
            Map<String, Object> n = obj(c);
            switch (tekst(n, "node")) {
                case "Comparison" -> { return readExpr(subject, readExpr(subject, acc, n.get("left")), n.get("right")); }
                case "Predicate" -> {
                    String category = tekst(n, "kind");
                    if ("empty".equals(category) || "filled".equals(category)) {
                        List<NodeRead> result = new ArrayList<>();
                        for (NodeRead l : readExpr(subject, List.of(), n.get("operand"))) result.add(new NodeRead(l.graphNode(), NEG));
                        result.addAll(acc);
                        return result;
                    }
                    return readExpr(subject, acc, n.get("operand"));
                }
                case "CharacteristicCheck" -> {
                    String k = tekst(n, "characteristic");
                    if (k == null) return acc;
                    String category = neg || Boolean.TRUE.equals(n.get("negated")) ? NEG : POS;
                    if (n.get("navigation") != null) {
                        Map<String, Object> rb = obj(n.get("navigation"));
                        Map<String, Object> role = boundRole(rb);
                        if (role == null) return acc;
                        String reached = tekst(role, "objectType");
                        List<NodeRead> result = new ArrayList<>();
                        result.add(new NodeRead(relNode(tekst(rb, "factType")), POS));
                        result.add(new NodeRead(charNode(reached == null ? "" : reached, k), category));
                        result.addAll(acc);
                        return result;
                    }
                    String host = tekst(n, "host") != null ? tekst(n, "host") : subject;
                    return host == null ? acc : before(new NodeRead(charNode(host, k), category), acc);
                }
                case "TimeWindow" -> { return readExpr(subject, readExpr(subject, acc, n.get("from")), n.get("to")); }
                case "Compound" -> {
                    String q = tekst(n, "quantifier");
                    boolean neg2 = "none".equals(q) || "atMost".equals(q) || "exactly".equals(q) || neg;
                    for (Object part : raw(n, "parts")) acc = readCond(subject, neg2, acc, part);
                    return acc;
                }
                case "QuantifierFold" -> {
                    String q = tekst(n, "quantifier");
                    boolean rangeNeg = neg || !"atLeast".equals(q);
                    boolean matrixNeg = neg || !("atLeast".equals(q) || "all".equals(q));
                    String el = tekst(n, "elementType");
                    List<NodeRead> result = new ArrayList<>();
                    if (n.get("sourceRole") != null)
                        result.add(new NodeRead(relNode(tekst(obj(n.get("sourceRole")), "factType")), rangeNeg ? NEG : POS));
                    for (Map<String, Object> rb : listOrEmpty(n, "sourcePath"))
                        result.add(new NodeRead(relNode(tekst(rb, "factType")), rangeNeg ? NEG : POS));
                    result.addAll(part(el, rangeNeg, n.get("filter")));
                    result.addAll(part(el, matrixNeg, n.get("test")));
                    result.addAll(acc);
                    return result;
                }
                case "Regelversie" -> {
                    String r = tekst(n, "regel");
                    return r == null ? acc : before(new NodeRead(chanNode(r), NEG), acc);
                }
                case "RoleMembership" -> {
                    // the link set of the named relation; negated, the check holds on absence
                    String category = neg || Boolean.TRUE.equals(n.get("negated")) ? NEG : POS;
                    return before(new NodeRead(relNode(tekst(n, "factType")), category), acc);
                }
                default -> { return acc; }   // Unresolved
            }
        }

        private List<NodeRead> part(String element, boolean negative, Object c) {
            if (c == null) return List.of();
            List<NodeRead> reads = readCond(element, negative, List.of(), c);
            if (!negative) return reads;
            List<NodeRead> result = new ArrayList<>();
            for (NodeRead l : reads) result.add(new NodeRead(l.graphNode(), NEG));
            return result;
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
                result.add(POS.equals(l.category()) && !l.graphNode().startsWith("char:") ? new NodeRead(l.graphNode(), STRICT) : l);
            return result;
        }

        private static List<NodeRead> negative(List<NodeRead> reads) {
            List<NodeRead> result = new ArrayList<>();
            for (NodeRead l : reads) result.add(new NodeRead(l.graphNode(), NEG));
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
        private record Version(String naam, String at, Map<String, Object> irNode) {}

        private List<Version> versions(String list) {
            List<Version> result = new ArrayList<>();
            List<Map<String, Object>> forms = list(regels, list);
            for (int f = 0; f < forms.size(); f++) {
                Map<String, Object> form = forms.get(f);
                String naam = tekst(form, "name");
                List<Map<String, Object>> vs = list(form, "versions");
                for (int v = 0; v < vs.size(); v++)
                    result.add(new Version(naam == null ? null : vs.size() >= 2 ? naam + " #" + (v + 1) : naam,
                            "/rules/" + list + "/" + f + "/versions/" + v, vs.get(v)));
            }
            return result;
        }

        private List<String> recursiveMembers() {
            List<String> result = new ArrayList<>();
            for (Map<String, Object> g : list(regels, "ruleGroups"))
                if (Boolean.TRUE.equals(g.get("recursive")))
                    for (Object member : raw(g, "members")) if (member instanceof String s) result.add(norm(withoutVersion(s)));
            return result;
        }

        private List<NodeRead> condition(String subject, Object c) {
            return c == null ? List.of() : readCond(subject, false, List.of(), c);
        }

        List<Form> forms() {
            List<String> recursive = recursiveMembers();
            List<Form> result = new ArrayList<>();
            // the rule versions
            for (Version v : versions("rules")) {
                Map<String, Object> target = obj(v.irNode().get("target"));
                String a = tekst(target, "attribute"), ot = tekst(target, "subjectType");
                if (a == null || ot == null) continue;
                boolean characteristic = !Boolean.TRUE.equals(v.irNode().get("initialisation")) && v.irNode().get("presentation") == null;
                List<String> writes = memberWrites(ot, a, characteristic, false);
                List<NodeRead> uses = readExpr(ot, condition(ot, v.irNode().get("condition")), v.irNode().get("value"));
                if (characteristic) uses = strict(uses);
                else if (!absorbingTypes(ot, a).isEmpty()) uses = negative(uses);
                result.add(new Form(v.at(), v.naam() == null ? v.at() : v.naam(), "rules", false, writes, uses));
            }
            // the allocations
            for (Version v : versions("allocations")) {
                Map<String, Object> k = v.irNode();
                String bt = value(tekst(k, "sourceType")), dt = value(tekst(k, "targetType"));
                List<String> writes = new ArrayList<>();
                if (tekst(k, "targetAttribute") != null) writes.addAll(memberWrites(dt, tekst(k, "targetAttribute"), false, false));
                if (tekst(k, "remainderAttribute") != null) writes.addAll(memberWrites(bt, tekst(k, "remainderAttribute"), false, false));
                List<NodeRead> uses = new ArrayList<>();
                uses.add(new NodeRead(popNode(dt), POS));
                uses.add(new NodeRead(relNode(tekst(obj(k.get("connectingRole")), "factType")), POS));
                if (tekst(k, "sourceAttribute") != null) uses.add(new NodeRead(attrNode(bt, tekst(k, "sourceAttribute")), POS));
                for (String field : List.of("ratioAttribute", "maximumAttribute", "orderingAttribute"))
                    if (tekst(k, field) != null) uses.add(new NodeRead(attrNode(dt, tekst(k, field)), POS));
                uses.addAll(condition(tekst(k, "sourceType"), k.get("condition")));
                result.add(new Form(v.at(), v.naam() == null ? v.at() : v.naam(), "allocations", false, writes, uses));
            }
            // the consistencies
            for (Version v : versions("consistencies")) {
                String naam = v.naam() == null ? v.at() : v.naam();
                result.add(new Form(v.at(), naam, "consistencies", false, List.of(chanNode(naam)),
                        strict(condition(tekst(v.irNode(), "subject"), v.irNode().get("assertion")))));
            }
            // the uniquenesses of one source, then those of several
            List<Version> unique = versions("uniquenesses");
            for (Version v : unique) {
                List<Map<String, Object>> sources = list(v.irNode(), "sources");
                if (sources.size() != 1) continue;
                String naam = v.naam() == null ? v.at() : v.naam();
                String subj = value(tekst(sources.get(0), "objectType"));
                List<NodeRead> uses = new ArrayList<>();
                if (v.irNode().get("scope") != null)
                    uses.add(new NodeRead(relNode(tekst(obj(v.irNode().get("scope")), "factType")), STRICT));
                for (Object key : raw(sources.get(0), "keys"))
                    uses.add(new NodeRead(attrNode(subj, key instanceof String s ? s : ""), STRICT));
                result.add(new Form(v.at(), naam, "uniquenesses", false, List.of(chanNode(naam)), uses));
            }
            for (Version v : unique) {
                List<Map<String, Object>> sources = list(v.irNode(), "sources");
                if (sources.size() == 1) continue;
                String naam = v.naam() == null ? v.at() : v.naam();
                List<NodeRead> uses = new ArrayList<>();
                for (Map<String, Object> b : sources) {
                    String t = tekst(b, "objectType");
                    if (t == null) continue;
                    for (Object key : raw(b, "keys")) uses.add(new NodeRead(attrNode(t, key instanceof String s ? s : ""), STRICT));
                }
                result.add(new Form(v.at(), naam, "uniquenesses", false, List.of(chanNode(naam)), uses));
            }
            // the object creations
            for (Version v : versions("objectCreations")) {
                Map<String, Object> k = v.irNode();
                String naam = v.naam() == null ? v.at() : v.naam();
                String nt = value(tekst(k, "createdType")), subject = tekst(k, "subject");
                List<String> writes = new ArrayList<>(populationWrites(nt));
                if (k.get("connectingRole") != null) {
                    writes.add(relNode(tekst(obj(k.get("connectingRole")), "factType")));
                } else if (subject != null) {
                    List<String> candidates = connectingFactTypes(subject, nt);
                    if (candidates.size() == 1) {
                        Map<String, Object> ft = factType(candidates.get(0));
                        if (!(ft != null && norm(subject).equals(norm(nt)) && !Boolean.TRUE.equals(ft.get("reciprocal"))))
                            writes.add(relNode(candidates.get(0)));
                    }
                }
                for (Map<String, Object> t : list(k, "initialisations"))
                    if (tekst(t, "member") != null)
                        writes.addAll(memberWrites(nt, tekst(t, "member"), Boolean.TRUE.equals(t.get("characteristic")), true));
                List<NodeRead> uses = new ArrayList<>();
                if (subject != null) uses.add(new NodeRead(popNode(subject), POS));
                List<NodeRead> waarden = condition(subject, k.get("condition"));
                for (Map<String, Object> t : list(k, "initialisations")) waarden = readExpr(subject, waarden, t.get("value"));
                uses.addAll(waarden);
                result.add(new Form(v.at(), naam, "objectCreations", recursive.contains(norm(withoutVersion(naam))), writes,
                        strict(uses)));
            }
            // the fact creations
            for (Version v : versions("factCreations")) {
                Map<String, Object> k = v.irNode();
                String naam = v.naam() == null ? v.at() : v.naam();
                List<Map<String, Object>> path = list(k, "path");
                List<NodeRead> uses = new ArrayList<>();
                for (Map<String, Object> hop : path) uses.add(new NodeRead(relNode(tekst(hop, "factType")), POS));
                uses.addAll(condition(tekst(k, "anchor"), k.get("condition")));
                // the created relation and no population: a fact creation links instances that exist
                List<String> writes = List.of(relNode(tekst(k, "factType")));
                result.add(new Form(v.at(), naam, "factCreations", recursive.contains(norm(withoutVersion(naam))), writes,
                        strict(uses)));
            }
            // the defaults groups
            List<Map<String, Object>> groups = list(regels, "prioritizedDefaults");
            for (int i = 0; i < groups.size(); i++) {
                Map<String, Object> l = groups.get(i);
                String at = "/prioritizedDefaults/" + i;
                String naam = tekst(l, "name") == null ? at : tekst(l, "name");
                String onderwerp = tekst(l, "subject");
                List<NodeRead> uses = new ArrayList<>();
                for (Map<String, Object> d : list(l, "defaults")) uses.addAll(defaultReads(onderwerp, d));
                List<String> writes = new ArrayList<>();
                if (tekst(l, "characteristic") != null)
                    writes.addAll(memberWrites(onderwerp, tekst(l, "characteristic"), true, false));
                if (tekst(l, "attribute") != null)
                    writes.addAll(memberWrites(onderwerp, tekst(l, "attribute"), false, false));
                result.add(new Form(at, naam, "prioritizedDefaults", false, writes, negative(uses)));
            }
            return result;
        }

        private List<NodeRead> defaultReads(String onderwerp, Map<String, Object> d) {
            List<NodeRead> result = new ArrayList<>(condition(onderwerp, d.get("justification")));
            if (d.get("consequence") != null) result.addAll(readExpr(onderwerp, List.of(), d.get("consequence")));
            if (d.get("exceptions") != null)
                for (Map<String, Object> eg : list(obj(d.get("exceptions")), "members"))
                    for (Map<String, Object> sub : list(eg, "defaults")) result.addAll(defaultReads(onderwerp, sub));
            return result;
        }

        /** {@code absorbing_types}: the types at or below {@code ot} whose chain declares a defaults group on the
         *  attribute, unless {@code ot} declares one itself. */
        private List<String> absorbingTypes(String ot, String attr) {
            String key = "A:" + norm(attr);
            for (Map<String, Object> l : list(regels, "prioritizedDefaults"))
                if (norm(tekst(l, "subject")).equals(norm(ot)) && targetKey(l).equals(key)) return List.of();
            List<String> result = new ArrayList<>();
            for (Map<String, Object> o : objecttypen) {
                String t = tekst(o, "name");
                if (isSub(t, ot) && groupKeys(t).contains(key)) result.add(t);
            }
            return result;
        }

        private static String targetKey(Map<String, Object> l) {
            String k = tekst(l, "characteristic"), a = tekst(l, "attribute");
            return (k != null ? "K:" : "A:") + (k != null ? norm(k) : a != null ? norm(a) : "?");
        }

        private TreeSet<String> groupKeys(String t) {
            TreeSet<String> result = new TreeSet<>();
            for (Map<String, Object> o : objecttypen) {
                String s = tekst(o, "name");
                if (!isSub(t, s)) continue;
                for (Map<String, Object> l : list(regels, "prioritizedDefaults"))
                    if (norm(tekst(l, "subject")).equals(norm(s))) result.add(targetKey(l));
            }
            return result;
        }

        // ------------------------------------------------------------ the document's shapes

        @SuppressWarnings("unchecked")
        static Map<String, Object> obj(Object x) {
            return x instanceof Map<?, ?> m ? (Map<String, Object>) m : Map.of();
        }

        static String tekst(Map<String, Object> m, String field) {
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

        static Object parse(String tekst) {
            Json j = new Json(tekst);
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
