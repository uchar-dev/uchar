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
 * other WF rules in their sentences. {@link #randen} prints the graph in the form of {@code verifier --dependency-edges},
 * so the two can be compared line for line.
 */
final class Afhankelijkheden {

    private Afhankelijkheden() {}

    /** A violation of WF-045 or WF-051 as the verifier reports it: the rule, the pointer, its sentence, and the forms of
     *  the cycle ({@code vormen}: for WF-045 the cycle in the walk's order, for WF-051 the group alone). */
    record Kring(String wf, String at, String zin, List<Vorm> vormen) {}

    /** A form of the graph: its pointer, its name (a version of a form with several is "&lt;naam&gt; #i"), whether it
     *  is a creation in a recursive rule group, what it writes and what it reads; {@code soort} is the list it comes
     *  from. */
    record Vorm(String at, String naam, String soort, boolean recursieveCreatie, List<String> schrijft, List<Lezing> leest) {}

    /** One read: the node and its edge kind (positive, strict, negative; ch. 5.2). */
    record Lezing(String knoop, String soort) {}

    private record Rand(int producent, int consument, String knoop, String soort) {}

    // ---------------------------------------------------------------- the two rules

    /** The WF-045 and WF-051 violations of {@code ir}, in the order and the sentences of the verifier. */
    static List<Kring> kringen(String ir) {
        Graaf g = new Graaf(Json.lees(ir));
        List<Vorm> vormen = g.vormen();
        List<Kring> uit = new ArrayList<>(wf045(vormen, g));
        uit.addAll(wf051(vormen, g));
        return uit;
    }

    /** The graph as {@code verifier --dependency-edges} prints it: one tab-separated line per edge — the kind, the
     *  producer's pointer and name, the node, the consumer's pointer and name — sorted, without repeats. */
    static List<String> randen(String ir) {
        Graaf g = new Graaf(Json.lees(ir));
        List<Vorm> vormen = g.vormen();
        TreeSet<String> uit = new TreeSet<>(Afhankelijkheden::vergelijkBytes);
        for (Rand r : randen(vormen)) {
            Vorm p = vormen.get(r.producent()), c = vormen.get(r.consument());
            uit.add(String.join("\t", r.soort(), m1Root(p.at()), p.naam(), r.knoop(), m1Root(c.at()), c.naam()));
        }
        return new ArrayList<>(uit);
    }

    /** Whether a negative edge (ch. 5.2) runs from one form of the cycle {@code k} to another, or from a form to itself.
     *  Every form of a cycle reaches every other, so such an edge lies on a cycle and the verifier refuses it under
     *  WF-046 as well ({@code wf045_046}); a negative edge from a form outside {@code k} is not looked for. */
    static boolean draagtNegatieveRand(Kring k) {
        for (Vorm c : k.vormen())
            for (Lezing l : c.leest())
                if (NEG.equals(l.soort()) && k.vormen().stream().anyMatch(p -> p.schrijft().contains(l.knoop())))
                    return true;
        return false;
    }

    /** {@code naam} without the " #i" of a version of a form with several ({@code strip_version}). */
    static String zonderVersie(String naam) { return Graaf.zonderVersie(naam); }

    private static List<Kring> wf045(List<Vorm> vormen, Graaf g) {
        int n = vormen.size();
        List<List<int[]>> volgers = opvolgers(vormen);
        int[] kleur = new int[n];
        List<Kring> uit = new ArrayList<>();
        for (int i = 0; i < n; i++) if (kleur[i] == 0) diep(vormen, volgers, kleur, new ArrayList<>(), i, uit);
        return uit;
    }

    /** The verifier's walk: a node already on the path closes a cycle, which is reported from that node on. The path
     *  is kept most recent first, as the verifier's list is. */
    private static void diep(List<Vorm> vormen, List<List<int[]>> volgers, int[] kleur, List<Integer> pad, int i,
                             List<Kring> uit) {
        if (vormen.get(i).recursieveCreatie()) return;
        if (kleur[i] == 1) {
            List<Integer> kring = new ArrayList<>();
            for (int j : pad) {
                kring.add(0, j);
                if (j == i) break;
            }
            List<String> namen = new ArrayList<>();
            List<Vorm> leden = new ArrayList<>();
            for (int j : kring) { namen.add(vormen.get(j).naam()); leden.add(vormen.get(j)); }
            namen.add(vormen.get(i).naam());
            uit.add(new Kring("WF-045", m1Root(vormen.get(i).at()),
                    "dependency cycle through no recursive creation: " + String.join(" -> ", namen), leden));
            return;
        }
        if (kleur[i] == 2) return;
        kleur[i] = 1;
        List<Integer> verder = new ArrayList<>();
        verder.add(i);
        verder.addAll(pad);
        for (int[] j : volgers.get(i)) diep(vormen, volgers, kleur, verder, j[0], uit);
        kleur[i] = 2;
    }

    /** The successors of every form, as the verifier builds them: for each edge in reading order, the consumer with the
     *  edge's kind put in FRONT of the producer's list unless that pair is there already, so a list is walked from the
     *  last edge inserted. An entry is {consumer, kind index}. */
    private static List<List<int[]>> opvolgers(List<Vorm> vormen) {
        List<List<int[]>> volgers = new ArrayList<>();
        for (int i = 0; i < vormen.size(); i++) volgers.add(new ArrayList<>());
        for (Rand r : randen(vormen)) {
            List<int[]> l = volgers.get(r.producent());
            int soort = soortIndex(r.soort());
            boolean al = false;
            for (int[] x : l) if (x[0] == r.consument() && x[1] == soort) { al = true; break; }
            if (!al) l.add(0, new int[] {r.consument(), soort});
        }
        return volgers;
    }

    private static List<Kring> wf051(List<Vorm> vormen, Graaf g) {
        int n = vormen.size();
        Map<String, List<Integer>> schrijvers = schrijvers(vormen);
        List<List<Integer>> volgers = new ArrayList<>();
        for (int i = 0; i < n; i++) volgers.add(new ArrayList<>());
        for (int ci = 0; ci < n; ci++)
            for (Lezing l : vormen.get(ci).leest())
                for (int pi : schrijvers.getOrDefault(l.knoop(), List.of())) volgers.get(pi).add(0, ci);
        List<Kring> uit = new ArrayList<>();
        List<Map<String, Object>> groepen = g.lijst(g.regels, "prioritizedDefaults");
        for (int i = 0; i < groepen.size(); i++) {
            String at = "/prioritizedDefaults/" + i;
            for (int fi = 0; fi < n; fi++) {
                if (!vormen.get(fi).at().equals(at)) continue;
                if (bereikt(volgers, fi, fi, new boolean[n])) {
                    String naam = Graaf.tekst(groepen.get(i), "name");
                    uit.add(new Kring("WF-051", m1Root(at), "a dependency cycle passes through defaults group '"
                            + (naam == null ? at : naam) + "' — its displacement reads are negative edges; no strata exist",
                            List.of(vormen.get(fi))));
                }
                break;
            }
        }
        return uit;
    }

    private static boolean bereikt(List<List<Integer>> volgers, int i, int doel, boolean[] gezien) {
        for (int j : volgers.get(i)) {
            if (j == doel) return true;
            if (!gezien[j]) {
                gezien[j] = true;
                if (bereikt(volgers, j, doel, gezien)) return true;
            }
        }
        return false;
    }

    /** Every writer of every node, the last one written first, as the verifier's table returns them. */
    private static Map<String, List<Integer>> schrijvers(List<Vorm> vormen) {
        Map<String, List<Integer>> uit = new LinkedHashMap<>();
        for (int i = 0; i < vormen.size(); i++)
            for (String knoop : vormen.get(i).schrijft()) uit.computeIfAbsent(knoop, k -> new ArrayList<>()).add(0, i);
        return uit;
    }

    /** The edges in reading order: consumer by consumer, each read in turn, its writers as the table answers them. */
    private static List<Rand> randen(List<Vorm> vormen) {
        Map<String, List<Integer>> schrijvers = schrijvers(vormen);
        List<Rand> uit = new ArrayList<>();
        for (int ci = 0; ci < vormen.size(); ci++)
            for (Lezing l : vormen.get(ci).leest())
                for (int pi : schrijvers.getOrDefault(l.knoop(), List.of())) uit.add(new Rand(pi, ci, l.knoop(), l.soort()));
        return uit;
    }

    private static int soortIndex(String soort) {
        return switch (soort) { case POS -> 0; case STRIKT -> 1; default -> 2; };
    }

    /** The rule lists the verifier addresses from the root and maps under {@code /rules} ({@code m1_rule_lists}). */
    private static final List<String> REGELLIJSTEN = List.of("dayKindDefinitions", "startPointDeterminations",
            "ruleGroups", "prioritizedDefaults");

    /** The verifier's pointer of a form in the document ({@code m1_root}): a defaults group's sits under
     *  {@code /rules}. */
    private static String m1Root(String at) {
        String[] d = at.split("/", -1);
        if (d.length >= 2 && d[0].isEmpty() && REGELLIJSTEN.contains(d[1])) return "/rules" + at;
        return at;
    }

    /** OCaml's string order, byte by byte over UTF-8, which for these names is code point order. */
    private static int vergelijkBytes(String a, String b) {
        return a.compareTo(b);
    }

    static final String POS = "positive", STRIKT = "strict", NEG = "negative";

    // ---------------------------------------------------------------- the graph over the document

    /** The verifier's reading of the document: the declarations it consults and the forms with their reads and writes. */
    private static final class Graaf {
        final Map<String, Object> model, regels;
        final List<Map<String, Object>> objecttypen, feittypen;

        Graaf(Object document) {
            Map<String, Object> doc = obj(document);
            this.model = obj(doc.get("model"));
            this.regels = obj(doc.get("rules"));
            this.objecttypen = lijst(model, "objectTypes");
            this.feittypen = lijst(model, "factTypes");
        }

        // ------------------------------------------------------------ names and types

        static String norm(String s) {
            StringBuilder b = new StringBuilder();
            for (String deel : s.split("[ \t]")) {
                if (deel.isEmpty()) continue;
                if (b.length() > 0) b.append(' ');
                b.append(deel);
            }
            StringBuilder uit = new StringBuilder(b.length());
            for (int i = 0; i < b.length(); i++) {
                char c = b.charAt(i);
                uit.append(c >= 'A' && c <= 'Z' ? (char) (c + 32) : c);
            }
            return uit.toString();
        }

        static String zonderVersie(String naam) {
            int i = naam.lastIndexOf('#');
            if (i >= 2 && naam.charAt(i - 1) == ' ' && i + 1 < naam.length()) {
                for (int k = i + 1; k < naam.length(); k++) if (naam.charAt(k) < '0' || naam.charAt(k) > '9') return naam;
                return naam.substring(0, i - 1);
            }
            return naam;
        }

        static String attrKnoop(String ot, String a) { return "attr:" + norm(ot) + ":" + norm(a); }
        static String charKnoop(String ot, String k) { return "char:" + norm(ot) + ":" + norm(k); }
        static String chanKnoop(String regel) { return "chan:" + norm(zonderVersie(regel)); }
        static String popKnoop(String ot) { return "pop:" + norm(ot); }
        static String relKnoop(String ft) { return "rel:" + norm(ft); }

        Map<String, Object> objecttype(String naam) {
            if (naam == null) return null;
            for (Map<String, Object> o : objecttypen) if (norm(tekst(o, "name")).equals(norm(naam))) return o;
            return null;
        }

        Map<String, Object> feittype(String naam) {
            if (naam == null) return null;
            for (Map<String, Object> f : feittypen) if (norm(tekst(f, "name")).equals(norm(naam))) return f;
            return null;
        }

        /** The chain from {@code ot} upward, own type first, a name already on the path stopping the walk. */
        List<Map<String, Object>> keten(Map<String, Object> ot) {
            List<Map<String, Object>> uit = new ArrayList<>();
            List<String> gezien = new ArrayList<>();
            gezien.add(norm(tekst(ot, "name")));
            Map<String, Object> hier = ot;
            while (true) {
                uit.add(hier);
                String s = tekst(hier, "supertype");
                if (s == null || gezien.contains(norm(s))) break;
                Map<String, Object> p = objecttype(s);
                if (p == null) break;
                gezien.add(norm(s));
                hier = p;
            }
            return uit;
        }

        boolean isSub(String sub, String sup) {
            if (norm(sub).equals(norm(sup))) return true;
            Map<String, Object> ot = objecttype(sub);
            if (ot == null) return false;
            for (Map<String, Object> t : keten(ot)) if (norm(tekst(t, "name")).equals(norm(sup))) return true;
            return false;
        }

        /** Whether {@code naam}, with its supertypes, declares the attribute or the characteristic {@code lid}. */
        boolean draagt(String naam, String lid, boolean kenmerk) {
            Map<String, Object> ot = objecttype(naam);
            if (ot == null) return false;
            for (Map<String, Object> t : keten(ot))
                for (Map<String, Object> m : lijst(t, kenmerk ? "characteristics" : "attributes"))
                    if (norm(tekst(m, "name")).equals(norm(lid))) return true;
            return false;
        }

        /** {@code member_writes}: the member on {@code ot} and on every type whose instances the write reaches that
         *  carries it — a form that iterates a type reaches the comparable types, a creation its type and the ancestors. */
        List<String> lidSchrijft(String ot, String lid, boolean kenmerk, boolean gemunt) {
            List<String> bereikt = new ArrayList<>();
            if (gemunt) {
                Map<String, Object> o = objecttype(ot);
                if (o != null) for (Map<String, Object> t : keten(o)) bereikt.add(tekst(t, "name"));
            } else {
                for (Map<String, Object> o : objecttypen) {
                    String n = tekst(o, "name");
                    if (isSub(n, ot) || isSub(ot, n)) bereikt.add(n);
                }
            }
            TreeSet<String> uit = new TreeSet<>();
            uit.add(kenmerk ? charKnoop(ot, lid) : attrKnoop(ot, lid));
            for (String n : bereikt) if (draagt(n, lid, kenmerk)) uit.add(kenmerk ? charKnoop(n, lid) : attrKnoop(n, lid));
            return new ArrayList<>(uit);
        }

        /** {@code pop_writes}: a creation grows the population of its type and of every ancestor. */
        List<String> populatieSchrijft(String t) {
            Map<String, Object> ot = objecttype(t);
            if (ot == null) return List.of(popKnoop(t));
            TreeSet<String> uit = new TreeSet<>();
            uit.add(popKnoop(t));
            for (Map<String, Object> a : keten(ot)) uit.add(popKnoop(tekst(a, "name")));
            return new ArrayList<>(uit);
        }

        /** The role a binding binds: the declared role of that name in the fact type, null where it does not resolve. */
        Map<String, Object> gebondenRol(Map<String, Object> binding) {
            Map<String, Object> ft = feittype(tekst(binding, "factType"));
            if (ft == null) return null;
            List<Map<String, Object>> rollen = lijst(ft, "roles");
            String rol = tekst(binding, "role");
            if (rollen.size() == 2) {
                boolean a = norm(tekst(rollen.get(0), "name")).equals(norm(rol));
                boolean b = norm(tekst(rollen.get(1), "name")).equals(norm(rol));
                if (a && !b) return rollen.get(0);
                if (b && !a) return rollen.get(1);
                return null;
            }
            if (rollen.size() == 1 && Boolean.TRUE.equals(ft.get("reciprocal"))
                    && norm(tekst(rollen.get(0), "name")).equals(norm(rol)))
                return rollen.get(0);
            return null;
        }

        /** {@code connecting_candidates}: the fact types relating {@code van} and {@code naar}, in declaration order. */
        List<String> verbindendeFeittypen(String van, String naar) {
            List<String> uit = new ArrayList<>();
            for (Map<String, Object> ft : feittypen) {
                List<Map<String, Object>> rollen = lijst(ft, "roles");
                if (rollen.size() == 2) {
                    Map<String, Object> a = rollen.get(0), b = rollen.get(1);
                    if ((vult(van, a) && vult(naar, b)) || (vult(van, b) && vult(naar, a))) uit.add(tekst(ft, "name"));
                } else if (rollen.size() == 1 && Boolean.TRUE.equals(ft.get("reciprocal"))) {
                    if (vult(van, rollen.get(0)) && norm(van).equals(norm(naar))) uit.add(tekst(ft, "name"));
                }
            }
            return uit;
        }

        private boolean vult(String inst, Map<String, Object> rol) {
            String vuller = tekst(rol, "objectType");
            return isSub(inst, vuller == null ? "" : vuller);
        }

        // ------------------------------------------------------------ reads (dep_expr, dep_cond)

        /** {@code dep_expr}: the reads of {@code e} in front of {@code acc}, as the verifier accumulates them. */
        List<Lezing> leesExpr(String subject, List<Lezing> acc, Object e) {
            if (e == null) return acc;
            Map<String, Object> n = obj(e);
            String node = tekst(n, "node");
            switch (node) {
                case "AttributeReference" -> {
                    String a = tekst(n, "attribute"), ot = tekst(n, "subjectType");
                    if (a == null || ot == null) return acc;
                    List<Lezing> uit = new ArrayList<>();
                    for (Map<String, Object> hop : lijstOfLeeg(n, "path")) uit.add(new Lezing(relKnoop(tekst(hop, "factType")), POS));
                    uit.add(new Lezing(attrKnoop(ot, a), POS));
                    uit.addAll(acc);
                    return uit;
                }
                case "TextComposition" -> {
                    for (Map<String, Object> deel : lijst(n, "parts"))
                        if ("EmbeddedExpression".equals(tekst(deel, "node"))) acc = leesExpr(subject, acc, deel.get("expr"));
                    return acc;
                }
                case "Binary" -> { return leesExpr(subject, leesExpr(subject, acc, n.get("left")), n.get("right")); }
                case "Aggregation", "BuiltinCall" -> {
                    String el = tekst(n, "elementType");
                    if (el != null) acc = voor(new Lezing(popKnoop(el), POS), acc);
                    for (Map<String, Object> rb : lijstOfLeeg(n, "sourcePath")) acc = voor(new Lezing(relKnoop(tekst(rb, "factType")), POS), acc);
                    if (n.get("sourceRole") != null) acc = voor(new Lezing(relKnoop(tekst(obj(n.get("sourceRole")), "factType")), POS), acc);
                    List<Object> argumenten = rauw(n, "arguments");
                    boolean telbron = false;
                    if ("Aggregation".equals(node) && "count".equals(tekst(n, "kind")) && el != null && argumenten.size() == 1
                            && (n.get("sourceRole") != null || n.get("sourcePath") != null)) {
                        Map<String, Object> arg = obj(argumenten.get(0));
                        String a = tekst(arg, "attribute");
                        if ("AttributeReference".equals(tekst(arg, "node")) && a != null && objecttype(el) != null)
                            telbron = !(draagt(el, a, false) || draagt(el, a, true));
                    }
                    if (!telbron) for (Object arg : argumenten) acc = leesExpr(subject, acc, arg);
                    acc = leesExpr(subject, acc, n.get("emptyFallback"));
                    String fsubject = el != null ? el : subject;
                    if (n.get("filter") != null) acc = leesCond(fsubject, false, acc, n.get("filter"));
                    return acc;
                }
                case "InstanceSetUnion" -> {
                    for (Map<String, Object> tak : lijst(n, "branches")) {
                        String el = tekst(tak, "elementType");
                        if (el != null) acc = voor(new Lezing(popKnoop(el), POS), acc);
                        acc = voor(new Lezing(relKnoop(tekst(obj(tak.get("sourceRole")), "factType")), POS), acc);
                        if (tak.get("filter") != null) acc = leesCond(el, false, acc, tak.get("filter"));
                    }
                    return acc;
                }
                case "Rounding" -> { return leesExpr(subject, acc, n.get("base")); }
                case "Clamp" -> {
                    return leesExpr(subject, leesExpr(subject, leesExpr(subject, acc, n.get("base")), n.get("minimum")),
                            n.get("maximum"));
                }
                case "DimensionCellRead" -> { return leesExpr(subject, acc, n.get("attribute")); }
                case "TemporalRead" -> { return leesExpr(subject, leesExpr(subject, acc, n.get("base")), n.get("date")); }
                case "Construct" -> { return leesExpr(subject, acc, n.get("argument")); }
                case "Match" -> {
                    acc = leesExpr(subject, acc, n.get("scrutinee"));
                    for (Map<String, Object> arm : lijst(n, "arms")) acc = leesExpr(subject, acc, arm.get("body"));
                    return acc;
                }
                case "StructLiteral" -> {
                    for (Map<String, Object> veld : lijst(n, "fields")) acc = leesExpr(subject, acc, veld.get("value"));
                    return acc;
                }
                case "FieldAccess" -> { return leesExpr(subject, acc, n.get("base")); }
                case "Let" -> { return leesExpr(subject, leesExpr(subject, acc, n.get("value")), n.get("body")); }
                case "ListLiteral" -> {
                    for (Object x : rauw(n, "elements")) acc = leesExpr(subject, acc, x);
                    return acc;
                }
                case "Lambda" -> { return leesExpr(subject, acc, n.get("body")); }
                case "IfThenElse" -> {
                    acc = leesCond(subject, false, acc, n.get("condition"));
                    return leesExpr(subject, leesExpr(subject, acc, n.get("then")), n.get("else"));
                }
                case "Invoke" -> {
                    for (Object x : rauw(n, "arguments")) acc = leesExpr(subject, acc, x);
                    return acc;
                }
                case "Fold" -> { return leesFunctie(subject, leesExpr(subject, leesExpr(subject, acc, n.get("list")), n.get("init")), n.get("function")); }
                case "MapList" -> { return leesFunctie(subject, leesExpr(subject, acc, n.get("list")), n.get("function")); }
                case "FilterList" -> { return leesCond(subject, false, leesExpr(subject, acc, n.get("list")), n.get("condition")); }
                case "ListLength" -> { return leesExpr(subject, acc, n.get("list")); }
                default -> { return acc; }   // Literal, ParameterReference, VariableReference, ContextValue, BoundDay, BinderRef, Unresolved
            }
        }

        private List<Lezing> leesFunctie(String subject, List<Lezing> acc, Object f) {
            return f instanceof Map<?, ?> ? leesExpr(subject, acc, obj(f).get("body")) : acc;
        }

        /** {@code dep_cond}: the reads of {@code c} in front of {@code acc}; {@code neg} marks a negative context. */
        List<Lezing> leesCond(String subject, boolean neg, List<Lezing> acc, Object c) {
            if (c == null) return acc;
            Map<String, Object> n = obj(c);
            switch (tekst(n, "node")) {
                case "Comparison" -> { return leesExpr(subject, leesExpr(subject, acc, n.get("left")), n.get("right")); }
                case "Predicate" -> {
                    String soort = tekst(n, "kind");
                    if ("empty".equals(soort) || "filled".equals(soort)) {
                        List<Lezing> uit = new ArrayList<>();
                        for (Lezing l : leesExpr(subject, List.of(), n.get("operand"))) uit.add(new Lezing(l.knoop(), NEG));
                        uit.addAll(acc);
                        return uit;
                    }
                    return leesExpr(subject, acc, n.get("operand"));
                }
                case "CharacteristicCheck" -> {
                    String k = tekst(n, "characteristic");
                    if (k == null) return acc;
                    String soort = neg || Boolean.TRUE.equals(n.get("negated")) ? NEG : POS;
                    if (n.get("navigation") != null) {
                        Map<String, Object> rb = obj(n.get("navigation"));
                        Map<String, Object> rol = gebondenRol(rb);
                        if (rol == null) return acc;
                        String bereikt = tekst(rol, "objectType");
                        List<Lezing> uit = new ArrayList<>();
                        uit.add(new Lezing(relKnoop(tekst(rb, "factType")), POS));
                        uit.add(new Lezing(charKnoop(bereikt == null ? "" : bereikt, k), soort));
                        uit.addAll(acc);
                        return uit;
                    }
                    String gastheer = tekst(n, "host") != null ? tekst(n, "host") : subject;
                    return gastheer == null ? acc : voor(new Lezing(charKnoop(gastheer, k), soort), acc);
                }
                case "TimeWindow" -> { return leesExpr(subject, leesExpr(subject, acc, n.get("from")), n.get("to")); }
                case "Compound" -> {
                    String q = tekst(n, "quantifier");
                    boolean neg2 = "none".equals(q) || "atMost".equals(q) || "exactly".equals(q) || neg;
                    for (Object deel : rauw(n, "parts")) acc = leesCond(subject, neg2, acc, deel);
                    return acc;
                }
                case "QuantifierFold" -> {
                    String q = tekst(n, "quantifier");
                    boolean bereikNeg = neg || !"atLeast".equals(q);
                    boolean matrixNeg = neg || !("atLeast".equals(q) || "all".equals(q));
                    String el = tekst(n, "elementType");
                    List<Lezing> uit = new ArrayList<>();
                    if (n.get("sourceRole") != null)
                        uit.add(new Lezing(relKnoop(tekst(obj(n.get("sourceRole")), "factType")), bereikNeg ? NEG : POS));
                    for (Map<String, Object> rb : lijstOfLeeg(n, "sourcePath"))
                        uit.add(new Lezing(relKnoop(tekst(rb, "factType")), bereikNeg ? NEG : POS));
                    uit.addAll(deel(el, bereikNeg, n.get("filter")));
                    uit.addAll(deel(el, matrixNeg, n.get("test")));
                    uit.addAll(acc);
                    return uit;
                }
                case "Regelversie" -> {
                    String r = tekst(n, "regel");
                    return r == null ? acc : voor(new Lezing(chanKnoop(r), NEG), acc);
                }
                case "RoleMembership" -> {
                    // the link set of the named relation; negated, the check holds on absence
                    String soort = neg || Boolean.TRUE.equals(n.get("negated")) ? NEG : POS;
                    return voor(new Lezing(relKnoop(tekst(n, "factType")), soort), acc);
                }
                default -> { return acc; }   // Unresolved
            }
        }

        private List<Lezing> deel(String element, boolean negatief, Object c) {
            if (c == null) return List.of();
            List<Lezing> lezingen = leesCond(element, negatief, List.of(), c);
            if (!negatief) return lezingen;
            List<Lezing> uit = new ArrayList<>();
            for (Lezing l : lezingen) uit.add(new Lezing(l.knoop(), NEG));
            return uit;
        }

        private static List<Lezing> voor(Lezing l, List<Lezing> acc) {
            List<Lezing> uit = new ArrayList<>(acc.size() + 1);
            uit.add(l);
            uit.addAll(acc);
            return uit;
        }

        private static List<Lezing> strikt(List<Lezing> lezingen) {
            List<Lezing> uit = new ArrayList<>();
            for (Lezing l : lezingen)
                uit.add(POS.equals(l.soort()) && !l.knoop().startsWith("char:") ? new Lezing(l.knoop(), STRIKT) : l);
            return uit;
        }

        private static List<Lezing> negatief(List<Lezing> lezingen) {
            List<Lezing> uit = new ArrayList<>();
            for (Lezing l : lezingen) uit.add(new Lezing(l.knoop(), NEG));
            return uit;
        }

        private static List<Lezing> plus(List<Lezing> a, List<Lezing> b) {
            List<Lezing> uit = new ArrayList<>(a);
            uit.addAll(b);
            return uit;
        }

        // ------------------------------------------------------------ the forms (dep_forms)

        /** A named form's versions: per version its in-memory name ("&lt;naam&gt; #i" where there are several), its
         *  pointer and its node. */
        private record Versie(String naam, String at, Map<String, Object> knoop) {}

        private List<Versie> versies(String lijst) {
            List<Versie> uit = new ArrayList<>();
            List<Map<String, Object>> vormen = lijst(regels, lijst);
            for (int f = 0; f < vormen.size(); f++) {
                Map<String, Object> vorm = vormen.get(f);
                String naam = tekst(vorm, "name");
                List<Map<String, Object>> vs = lijst(vorm, "versions");
                for (int v = 0; v < vs.size(); v++)
                    uit.add(new Versie(naam == null ? null : vs.size() >= 2 ? naam + " #" + (v + 1) : naam,
                            "/rules/" + lijst + "/" + f + "/versions/" + v, vs.get(v)));
            }
            return uit;
        }

        private List<String> recursieveLeden() {
            List<String> uit = new ArrayList<>();
            for (Map<String, Object> g : lijst(regels, "ruleGroups"))
                if (Boolean.TRUE.equals(g.get("recursive")))
                    for (Object lid : rauw(g, "members")) if (lid instanceof String s) uit.add(norm(zonderVersie(s)));
            return uit;
        }

        private List<Lezing> conditie(String subject, Object c) {
            return c == null ? List.of() : leesCond(subject, false, List.of(), c);
        }

        List<Vorm> vormen() {
            List<String> recursief = recursieveLeden();
            List<Vorm> uit = new ArrayList<>();
            // the rule versions
            for (Versie v : versies("rules")) {
                Map<String, Object> doel = obj(v.knoop().get("target"));
                String a = tekst(doel, "attribute"), ot = tekst(doel, "subjectType");
                if (a == null || ot == null) continue;
                boolean kenmerk = !Boolean.TRUE.equals(v.knoop().get("initialisation")) && v.knoop().get("presentation") == null;
                List<String> schrijft = lidSchrijft(ot, a, kenmerk, false);
                List<Lezing> leest = leesExpr(ot, conditie(ot, v.knoop().get("condition")), v.knoop().get("value"));
                if (kenmerk) leest = strikt(leest);
                else if (!absorberendeTypen(ot, a).isEmpty()) leest = negatief(leest);
                uit.add(new Vorm(v.at(), v.naam() == null ? v.at() : v.naam(), "rules", false, schrijft, leest));
            }
            // the allocations
            for (Versie v : versies("allocations")) {
                Map<String, Object> k = v.knoop();
                String bt = waarde(tekst(k, "sourceType")), dt = waarde(tekst(k, "targetType"));
                List<String> schrijft = new ArrayList<>();
                if (tekst(k, "targetAttribute") != null) schrijft.addAll(lidSchrijft(dt, tekst(k, "targetAttribute"), false, false));
                if (tekst(k, "remainderAttribute") != null) schrijft.addAll(lidSchrijft(bt, tekst(k, "remainderAttribute"), false, false));
                List<Lezing> leest = new ArrayList<>();
                leest.add(new Lezing(popKnoop(dt), POS));
                leest.add(new Lezing(relKnoop(tekst(obj(k.get("connectingRole")), "factType")), POS));
                if (tekst(k, "sourceAttribute") != null) leest.add(new Lezing(attrKnoop(bt, tekst(k, "sourceAttribute")), POS));
                for (String veld : List.of("ratioAttribute", "maximumAttribute", "orderingAttribute"))
                    if (tekst(k, veld) != null) leest.add(new Lezing(attrKnoop(dt, tekst(k, veld)), POS));
                leest.addAll(conditie(tekst(k, "sourceType"), k.get("condition")));
                uit.add(new Vorm(v.at(), v.naam() == null ? v.at() : v.naam(), "allocations", false, schrijft, leest));
            }
            // the consistencies
            for (Versie v : versies("consistencies")) {
                String naam = v.naam() == null ? v.at() : v.naam();
                uit.add(new Vorm(v.at(), naam, "consistencies", false, List.of(chanKnoop(naam)),
                        strikt(conditie(tekst(v.knoop(), "subject"), v.knoop().get("assertion")))));
            }
            // the uniquenesses of one source, then those of several
            List<Versie> uniek = versies("uniquenesses");
            for (Versie v : uniek) {
                List<Map<String, Object>> bronnen = lijst(v.knoop(), "sources");
                if (bronnen.size() != 1) continue;
                String naam = v.naam() == null ? v.at() : v.naam();
                String subj = waarde(tekst(bronnen.get(0), "objectType"));
                List<Lezing> leest = new ArrayList<>();
                if (v.knoop().get("scope") != null)
                    leest.add(new Lezing(relKnoop(tekst(obj(v.knoop().get("scope")), "factType")), STRIKT));
                for (Object sleutel : rauw(bronnen.get(0), "keys"))
                    leest.add(new Lezing(attrKnoop(subj, sleutel instanceof String s ? s : ""), STRIKT));
                uit.add(new Vorm(v.at(), naam, "uniquenesses", false, List.of(chanKnoop(naam)), leest));
            }
            for (Versie v : uniek) {
                List<Map<String, Object>> bronnen = lijst(v.knoop(), "sources");
                if (bronnen.size() == 1) continue;
                String naam = v.naam() == null ? v.at() : v.naam();
                List<Lezing> leest = new ArrayList<>();
                for (Map<String, Object> b : bronnen) {
                    String t = tekst(b, "objectType");
                    if (t == null) continue;
                    for (Object sleutel : rauw(b, "keys")) leest.add(new Lezing(attrKnoop(t, sleutel instanceof String s ? s : ""), STRIKT));
                }
                uit.add(new Vorm(v.at(), naam, "uniquenesses", false, List.of(chanKnoop(naam)), leest));
            }
            // the object creations
            for (Versie v : versies("objectCreations")) {
                Map<String, Object> k = v.knoop();
                String naam = v.naam() == null ? v.at() : v.naam();
                String nt = waarde(tekst(k, "createdType")), subject = tekst(k, "subject");
                List<String> schrijft = new ArrayList<>(populatieSchrijft(nt));
                if (k.get("connectingRole") != null) {
                    schrijft.add(relKnoop(tekst(obj(k.get("connectingRole")), "factType")));
                } else if (subject != null) {
                    List<String> kandidaten = verbindendeFeittypen(subject, nt);
                    if (kandidaten.size() == 1) {
                        Map<String, Object> ft = feittype(kandidaten.get(0));
                        if (!(ft != null && norm(subject).equals(norm(nt)) && !Boolean.TRUE.equals(ft.get("reciprocal"))))
                            schrijft.add(relKnoop(kandidaten.get(0)));
                    }
                }
                for (Map<String, Object> t : lijst(k, "initialisations"))
                    if (tekst(t, "member") != null)
                        schrijft.addAll(lidSchrijft(nt, tekst(t, "member"), Boolean.TRUE.equals(t.get("characteristic")), true));
                List<Lezing> leest = new ArrayList<>();
                if (subject != null) leest.add(new Lezing(popKnoop(subject), POS));
                List<Lezing> waarden = conditie(subject, k.get("condition"));
                for (Map<String, Object> t : lijst(k, "initialisations")) waarden = leesExpr(subject, waarden, t.get("value"));
                leest.addAll(waarden);
                uit.add(new Vorm(v.at(), naam, "objectCreations", recursief.contains(norm(zonderVersie(naam))), schrijft,
                        strikt(leest)));
            }
            // the fact creations
            for (Versie v : versies("factCreations")) {
                Map<String, Object> k = v.knoop();
                String naam = v.naam() == null ? v.at() : v.naam();
                List<Map<String, Object>> pad = lijst(k, "path");
                List<Lezing> leest = new ArrayList<>();
                for (Map<String, Object> hop : pad) leest.add(new Lezing(relKnoop(tekst(hop, "factType")), POS));
                leest.addAll(conditie(tekst(k, "anchor"), k.get("condition")));
                // the created relation and no population: a fact creation links instances that exist
                List<String> schrijft = List.of(relKnoop(tekst(k, "factType")));
                uit.add(new Vorm(v.at(), naam, "factCreations", recursief.contains(norm(zonderVersie(naam))), schrijft,
                        strikt(leest)));
            }
            // the defaults groups
            List<Map<String, Object>> groepen = lijst(regels, "prioritizedDefaults");
            for (int i = 0; i < groepen.size(); i++) {
                Map<String, Object> l = groepen.get(i);
                String at = "/prioritizedDefaults/" + i;
                String naam = tekst(l, "name") == null ? at : tekst(l, "name");
                String onderwerp = tekst(l, "subject");
                List<Lezing> leest = new ArrayList<>();
                for (Map<String, Object> d : lijst(l, "defaults")) leest.addAll(standaardLezingen(onderwerp, d));
                List<String> schrijft = new ArrayList<>();
                if (tekst(l, "characteristic") != null)
                    schrijft.addAll(lidSchrijft(onderwerp, tekst(l, "characteristic"), true, false));
                if (tekst(l, "attribute") != null)
                    schrijft.addAll(lidSchrijft(onderwerp, tekst(l, "attribute"), false, false));
                uit.add(new Vorm(at, naam, "prioritizedDefaults", false, schrijft, negatief(leest)));
            }
            return uit;
        }

        private List<Lezing> standaardLezingen(String onderwerp, Map<String, Object> d) {
            List<Lezing> uit = new ArrayList<>(conditie(onderwerp, d.get("justification")));
            if (d.get("consequence") != null) uit.addAll(leesExpr(onderwerp, List.of(), d.get("consequence")));
            if (d.get("exceptions") != null)
                for (Map<String, Object> eg : lijst(obj(d.get("exceptions")), "members"))
                    for (Map<String, Object> sub : lijst(eg, "defaults")) uit.addAll(standaardLezingen(onderwerp, sub));
            return uit;
        }

        /** {@code absorbing_types}: the types at or below {@code ot} whose chain declares a defaults group on the
         *  attribute, unless {@code ot} declares one itself. */
        private List<String> absorberendeTypen(String ot, String attr) {
            String sleutel = "A:" + norm(attr);
            for (Map<String, Object> l : lijst(regels, "prioritizedDefaults"))
                if (norm(tekst(l, "subject")).equals(norm(ot)) && doelsleutel(l).equals(sleutel)) return List.of();
            List<String> uit = new ArrayList<>();
            for (Map<String, Object> o : objecttypen) {
                String t = tekst(o, "name");
                if (isSub(t, ot) && groepsleutels(t).contains(sleutel)) uit.add(t);
            }
            return uit;
        }

        private static String doelsleutel(Map<String, Object> l) {
            String k = tekst(l, "characteristic"), a = tekst(l, "attribute");
            return (k != null ? "K:" : "A:") + (k != null ? norm(k) : a != null ? norm(a) : "?");
        }

        private TreeSet<String> groepsleutels(String t) {
            TreeSet<String> uit = new TreeSet<>();
            for (Map<String, Object> o : objecttypen) {
                String s = tekst(o, "name");
                if (!isSub(t, s)) continue;
                for (Map<String, Object> l : lijst(regels, "prioritizedDefaults"))
                    if (norm(tekst(l, "subject")).equals(norm(s))) uit.add(doelsleutel(l));
            }
            return uit;
        }

        // ------------------------------------------------------------ the document's shapes

        @SuppressWarnings("unchecked")
        static Map<String, Object> obj(Object x) {
            return x instanceof Map<?, ?> m ? (Map<String, Object>) m : Map.of();
        }

        static String tekst(Map<String, Object> m, String veld) {
            return m.get(veld) instanceof String s ? s : null;
        }

        private static String waarde(String s) { return s == null ? "" : s; }

        static List<Object> rauw(Map<String, Object> m, String veld) {
            return m.get(veld) instanceof List<?> l ? Collections.unmodifiableList(l) : List.of();
        }

        List<Map<String, Object>> lijst(Map<String, Object> m, String veld) { return lijstOfLeeg(m, veld); }

        static List<Map<String, Object>> lijstOfLeeg(Map<String, Object> m, String veld) {
            List<Map<String, Object>> uit = new ArrayList<>();
            for (Object x : rauw(m, veld)) uit.add(obj(x));
            return uit;
        }
    }

    // ---------------------------------------------------------------- reading the document

    /** A reader for the JSON this frontend's emitter writes: objects keep their key order, a number is a BigDecimal. */
    static final class Json {
        private final String s;
        private int i;

        private Json(String s) { this.s = s; }

        static Object lees(String tekst) {
            Json j = new Json(tekst);
            Object v = j.waarde();
            j.wit();
            if (j.i != j.s.length()) throw new IllegalArgumentException("JSON: tekst na het document op " + j.i);
            return v;
        }

        private void wit() {
            while (i < s.length() && (s.charAt(i) == ' ' || s.charAt(i) == '\n' || s.charAt(i) == '\r' || s.charAt(i) == '\t')) i++;
        }

        private Object waarde() {
            wit();
            if (i >= s.length()) throw new IllegalArgumentException("JSON: einde zonder waarde");
            char c = s.charAt(i);
            switch (c) {
                case '{' -> {
                    i++;
                    Map<String, Object> m = new LinkedHashMap<>();
                    wit();
                    if (s.charAt(i) == '}') { i++; return m; }
                    while (true) {
                        wit();
                        String k = tekenreeks();
                        wit();
                        verwacht(':');
                        m.put(k, waarde());
                        wit();
                        if (s.charAt(i) == ',') { i++; continue; }
                        verwacht('}');
                        return m;
                    }
                }
                case '[' -> {
                    i++;
                    List<Object> l = new ArrayList<>();
                    wit();
                    if (s.charAt(i) == ']') { i++; return l; }
                    while (true) {
                        l.add(waarde());
                        wit();
                        if (s.charAt(i) == ',') { i++; continue; }
                        verwacht(']');
                        return l;
                    }
                }
                case '"' -> { return tekenreeks(); }
                case 't' -> { woord("true"); return Boolean.TRUE; }
                case 'f' -> { woord("false"); return Boolean.FALSE; }
                case 'n' -> { woord("null"); return null; }
                default -> {
                    int b = i;
                    while (i < s.length() && "+-0123456789.eE".indexOf(s.charAt(i)) >= 0) i++;
                    if (b == i) throw new IllegalArgumentException("JSON: onverwacht teken '" + c + "' op " + i);
                    return new BigDecimal(s.substring(b, i));
                }
            }
        }

        private void woord(String w) {
            if (!s.startsWith(w, i)) throw new IllegalArgumentException("JSON: '" + w + "' verwacht op " + i);
            i += w.length();
        }

        private void verwacht(char c) {
            if (i >= s.length() || s.charAt(i) != c) throw new IllegalArgumentException("JSON: '" + c + "' verwacht op " + i);
            i++;
        }

        private String tekenreeks() {
            verwacht('"');
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
