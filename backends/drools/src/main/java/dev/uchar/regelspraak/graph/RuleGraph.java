package dev.uchar.regelspraak.graph;

import dev.uchar.regelspraak.graph.ProducerConsumer.ElementKey;
import dev.uchar.regelspraak.graph.ProducerConsumer.Navigation;
import dev.uchar.regelspraak.graph.ProducerConsumer.Form;
import dev.uchar.regelspraak.graph.ProducerConsumer.FormKind;
import dev.uchar.regelspraak.ir.PrioritizedDefaults;
import dev.uchar.regelspraak.model.RegelSpraakModel;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The regelgraaf: the bipartite dataflow graph between the forms of the rule layer and the data they write and read,
 * extracted from a resolved {@link RegelSpraakModel}. A form is a rule, an object creation, an allocation, a
 * consistency, a uniqueness, a fact creation or a defaults group ({@link ProducerConsumer.FormKind}). A form node
 * connects to a data node it PRODUCES (form → data: a rule's or a defaults group's target, an allocation's target and
 * remainder, the members an object creation assigns) and a data node connects to a form that CONSUMES it (data →
 * form: what its conditions, values, keys and justifications read). Direction is therefore dataflow: a request-input
 * (a datum no form produces) feeds forms, forms produce data, that data feeds downstream forms, and a datum no form
 * consumes is a product-less eind-output.
 *
 * <p>The producer/consumer analysis is the SHARED {@link ProducerConsumer} core (also the authority behind a
 * scope-completeness check), so the graph is coloured by the very facts that check uses:
 * <ul>
 *   <li>{@link NodeKind#RULE} — a form of the rule layer, of whichever kind;</li>
 *   <li>{@link NodeKind#ATTRIBUTE} — an attribuut some form writes;</li>
 *   <li>{@link NodeKind#CHARACTERISTIC} — a §13.4.5 boolean kenmerk some form writes;</li>
 *   <li>{@link NodeKind#INPUT} — a datum CONSUMED but produced by no form (a request/koppelvlak input);</li>
 * </ul>
 * and a produced-but-never-consumed datum is flagged as a product-less eind-output. A data node is one member keyed
 * on the type that declares it, so a member written on a supertype and read on a subtype is one node, and a
 * navigated kenmerk check reads the kenmerk of the type its hop reaches, which each route to a model answers
 * ({@link ProducerConsumer.Navigation}).
 *
 * <p>The graph is a reading of the dependency graph of IR-SPEC ch. 5 over its attribute and characteristic nodes:
 * every path form → data → form is an edge producer → consumer there ({@link #dependencies}). The other nodes of
 * ch. 5.1 (a population, a relation's link set, a channel) are not drawn, and neither are the edges that run over
 * them. Keying a member on its declaring type draws more than ch. 5.1 in two known places, where the verifier closes
 * a write over the instances it reaches: between two sibling subtypes that share an inherited member, and from an
 * object creation that assigns an inherited member to a read of it on a strict subtype of the created type, which
 * the creation never mints. The graph is built from the records the loader rebuilds from {@code ir.json}, with the
 * defaults groups and the navigation bindings it returns beside the model, so a consumer without sources gets the
 * graph a resolver's model gives. {@code scripts/check-graaf.sh} holds both claims: the dependencies against the
 * verifier's, over every golden, and the two routes against each other.
 *
 * <p>The render is jar-only: {@link #mermaid()} is the bare Mermaid flowchart source (offline-usable), and
 * {@link #html()} wraps it in a self-contained page that loads Mermaid from a CDN (needs the network to render).
 */
public final class RuleGraph {

    public enum NodeKind { RULE, ATTRIBUTE, CHARACTERISTIC, INPUT }

    /** One graph node. {@code id} is a Mermaid-safe stable identifier; {@code label} is the human text;
     *  {@code endOutput} marks a produced-but-never-consumed datum (a product-less eind-output). */
    public record Node(String id, String label, NodeKind kind, boolean endOutput) {}

    /** A directed dataflow edge from {@code van} to {@code naar} (both node ids). */
    public record Edge(String van, String destination) {}

    /** One dependency the graph draws: form {@code producer}, of kind {@code producerForm}, writes {@code element},
     *  and form {@code consumer}, of kind {@code consumerForm}, reads it — a path producent → element → consument
     *  over two of its edges. This is the reading IR-SPEC ch. 5.2 calls an edge (producer → consumer), over the
     *  nodes this graph draws: attributes and characteristics. */
    public record Dependency(FormKind producerForm, String producer, ElementKey element,
                                  FormKind consumerForm, String consumer) {}

    private final List<Node> nodes;
    private final List<Edge> edges;
    /** The element behind every element node, by node id: the key a node label only spells for a reader. */
    private final Map<String, ElementKey> elements;
    /** The kind of form behind every form node, by node id. */
    private final Map<String, FormKind> formKinds;

    private RuleGraph(List<Node> nodes, List<Edge> edges, Map<String, ElementKey> elements,
                       Map<String, FormKind> formKinds) {
        this.nodes = nodes;
        this.edges = edges;
        this.elements = elements;
        this.formKinds = formKinds;
    }

    public List<Node> nodes() { return nodes; }
    public List<Edge> edges() { return edges; }

    /** Every {@link Dependency} the graph draws, read off its own edges (never off the analysis behind
     *  them), so an edge the builder dropped or invented shows here as well: each producer edge into an element
     *  node paired with each consumer edge out of it. */
    public List<Dependency> dependencies() {
        Map<String, String> label = new LinkedHashMap<>();
        for (Node n : nodes) label.put(n.id(), n.label());
        // the consumer edges run element -> form, so they leave an element node
        Map<String, List<String>> readers = new LinkedHashMap<>();
        for (Edge e : edges)
            if (elements.containsKey(e.van())) readers.computeIfAbsent(e.van(), k -> new ArrayList<>()).add(e.destination());
        List<Dependency> out = new ArrayList<>();
        for (Edge in : edges) {
            ElementKey el = elements.get(in.destination());
            if (el == null) continue;                           // not a producer edge
            for (String reader : readers.getOrDefault(in.destination(), List.of()))
                out.add(new Dependency(formKinds.get(in.van()), label.get(in.van()), el,
                        formKinds.get(reader), label.get(reader)));
        }
        return out;
    }

    /** Build the graph of a resolved model: every form of the rule layer ({@link ProducerConsumer#forms}) and
     *  the attributes and characteristics it writes and reads. A data element is INPUT when no form writes it, and
     *  a written element is marked eind-output when no form reads it. {@code defaults} is the defaults-group list
     *  (a resolver's {@code model.prioritizedDefaults}, the loader's {@code Loaded.prioritizedDefaults()}), and
     *  {@code navigation} the route's answer to which type a navigated kenmerk check reaches (see
     *  {@link Navigation}). */
    public static RuleGraph build(RegelSpraakModel model, List<PrioritizedDefaults> defaults, Navigation navigation) {
        List<Form> forms = ProducerConsumer.forms(model, defaults, navigation);
        // A form node is one form under its kind and base name, so the versions of a form are one node.
        record FormKey(FormKind category, String naam) {}
        Map<ElementKey, Set<FormKey>> producers = new LinkedHashMap<>();
        for (Form v : forms)
            for (ElementKey el : v.writes())
                producers.computeIfAbsent(el, k -> new LinkedHashSet<>()).add(new FormKey(v.category(), v.naam()));
        // Every element that is consumed by at least one form, so we can tell producties zonder afnemer apart.
        Set<ElementKey> consumedElements = new LinkedHashSet<>();
        for (Form v : forms) consumedElements.addAll(v.reads());

        // Stable id allocation. Form ids by kind and base name; element ids by sleutel.
        Map<FormKey, String> formId = new LinkedHashMap<>();
        Map<String, FormKind> formKinds = new LinkedHashMap<>();
        Map<ElementKey, String> elemId = new LinkedHashMap<>();
        List<Node> nodes = new ArrayList<>();
        Set<Edge> edgeSet = new LinkedHashSet<>();

        // 1. element nodes: the union of everything produced and everything consumed.
        Set<ElementKey> allElements = new LinkedHashSet<>();
        allElements.addAll(producers.keySet());
        allElements.addAll(consumedElements);
        for (ElementKey el : allElements) {
            boolean produced = producers.containsKey(el);
            boolean consumed = consumedElements.contains(el);
            NodeKind kind = !produced ? NodeKind.INPUT
                    : (el.characteristic() ? NodeKind.CHARACTERISTIC : NodeKind.ATTRIBUTE);
            boolean endOutput = produced && !consumed;
            String id = "e" + elemId.size();
            elemId.put(el, id);
            nodes.add(new Node(id, elementLabel(el), kind, endOutput));
        }

        // 2. form nodes: every form, whatever it writes or reads.
        for (Form v : forms)
            formId.computeIfAbsent(new FormKey(v.category(), v.naam()), s -> {
                String id = "r" + formId.size();
                nodes.add(new Node(id, s.naam(), NodeKind.RULE, false));
                formKinds.put(id, s.category());
                return id;
            });

        // 3. producer edges: form -> element it writes.
        for (var e : producers.entrySet())
            for (FormKey s : e.getValue()) edgeSet.add(new Edge(formId.get(s), elemId.get(e.getKey())));
        // 4. consumer edges: element -> form that reads it.
        for (Form v : forms) {
            String to = formId.get(new FormKey(v.category(), v.naam()));
            for (ElementKey el : v.reads()) edgeSet.add(new Edge(elemId.get(el), to));
        }
        Map<String, ElementKey> elements = new LinkedHashMap<>();
        elemId.forEach((el, id) -> elements.put(id, el));
        return new RuleGraph(nodes, new ArrayList<>(edgeSet), elements, formKinds);
    }

    /** The label of an element node: "attribuut van Objecttype" (or just the naam when the owner is unknown). */
    private static String elementLabel(ElementKey el) {
        return el.objectType().isEmpty() ? el.naam() : el.naam() + " · " + el.objectType();
    }

    // ---------------- render ----------------

    /** The bare Mermaid flowchart source (left-to-right, dataflow direction). Offline-usable: paste into any
     *  Mermaid renderer. Node shapes/classes encode the kind; an eind-output rule-target is dashed-outlined. */
    public String mermaid() {
        StringBuilder b = new StringBuilder();
        b.append("flowchart LR\n");
        for (Node n : nodes) {
            String open, close;
            switch (n.kind()) {
                case RULE   -> { open = "[[";  close = "]]"; }       // subroutine shape: a rule
                case INPUT  -> { open = "[/";  close = "/]"; }       // parallelogram: an external input
                case CHARACTERISTIC -> { open = "([";  close = "])"; }       // stadium: a boolean kenmerk
                default      -> { open = "(";   close = ")"; }        // rounded: an attribuut
            }
            b.append("  ").append(n.id()).append(open).append('"').append(escapeMermaid(n.label()))
                    .append('"').append(close).append('\n');
        }
        b.append('\n');
        for (Edge e : edges) b.append("  ").append(e.van()).append(" --> ").append(e.destination()).append('\n');
        b.append('\n');
        // class definitions + assignments
        b.append("  classDef regel fill:#dbeafe,stroke:#2563eb,color:#1e3a8a;\n");
        b.append("  classDef attribuut fill:#dcfce7,stroke:#16a34a,color:#14532d;\n");
        b.append("  classDef kenmerk fill:#fef9c3,stroke:#ca8a04,color:#713f12;\n");
        b.append("  classDef invoer fill:#fae8ff,stroke:#a21caf,color:#701a75;\n");
        b.append("  classDef eindoutput stroke-width:3px,stroke-dasharray:4 3;\n");
        appendClass(b, "regel", NodeKind.RULE);
        appendClass(b, "attribuut", NodeKind.ATTRIBUTE);
        appendClass(b, "kenmerk", NodeKind.CHARACTERISTIC);
        appendClass(b, "invoer", NodeKind.INPUT);
        List<String> end = nodes.stream().filter(Node::endOutput).map(Node::id).toList();
        if (!end.isEmpty()) b.append("  class ").append(String.join(",", end)).append(" eindoutput;\n");
        return b.toString();
    }

    private void appendClass(StringBuilder b, String javaClass, NodeKind kind) {
        List<String> ids = nodes.stream().filter(n -> n.kind() == kind).map(Node::id).toList();
        if (!ids.isEmpty()) b.append("  class ").append(String.join(",", ids)).append(' ').append(javaClass).append(";\n");
    }

    /** A self-contained HTML page embedding the Mermaid source and loading the Mermaid runtime from a CDN. Works
     *  ONLY with a network (that is the deliberate trade for a single portable file); the {@link #mermaid()}
     *  companion is the offline artefact. The counts + a legend are rendered in plain HTML so the page is still
     *  informative if the script fails to load. */
    public String html() {
        long nRule = nodes.stream().filter(n -> n.kind() == NodeKind.RULE).count();
        long nAttr  = nodes.stream().filter(n -> n.kind() == NodeKind.ATTRIBUTE).count();
        long nCharacteristic  = nodes.stream().filter(n -> n.kind() == NodeKind.CHARACTERISTIC).count();
        long nInput = nodes.stream().filter(n -> n.kind() == NodeKind.INPUT).count();
        long nEnd  = nodes.stream().filter(Node::endOutput).count();
        String mmd = mermaid();
        return """
            <!DOCTYPE html>
            <html lang="nl">
            <head>
            <meta charset="utf-8">
            <meta name="viewport" content="width=device-width, initial-scale=1">
            <title>RegelSpraak regelgraaf</title>
            <style>
              body { font-family: system-ui, sans-serif; margin: 0; padding: 1.5rem; color: #1f2937; }
              h1 { font-size: 1.25rem; margin: 0 0 .5rem; }
              .telling { color: #4b5563; margin: 0 0 1rem; font-size: .9rem; }
              .telling b { color: #111827; }
              .legenda { display: flex; gap: 1rem; flex-wrap: wrap; margin: 0 0 1rem; font-size: .85rem; }
              .legenda span { padding: .15rem .5rem; border-radius: .3rem; border: 1px solid; }
              .lg-regel { background:#dbeafe; border-color:#2563eb; color:#1e3a8a; }
              .lg-attr  { background:#dcfce7; border-color:#16a34a; color:#14532d; }
              .lg-kenm  { background:#fef9c3; border-color:#ca8a04; color:#713f12; }
              .lg-inv   { background:#fae8ff; border-color:#a21caf; color:#701a75; }
              .lg-eind  { border-style:dashed; border-width:2px; border-color:#111827; }
              .graaf { border:1px solid #e5e7eb; border-radius:.5rem; padding:1rem; overflow:auto; }
            </style>
            <script src="https://cdn.jsdelivr.net/npm/mermaid@11/dist/mermaid.min.js"></script>
            <script>
              window.addEventListener('load', function () {
                if (window.mermaid) { mermaid.initialize({ startOnLoad: false, maxTextSize: 500000 });
                  mermaid.run({ querySelector: '.mermaid' }); }
              });
            </script>
            </head>
            <body>
            <h1>RegelSpraak regelgraaf — dataflow regels &harr; gegevens</h1>
            <p class="telling"><b>__NREGEL__</b> regels, <b>__NATTR__</b> attributen,
              <b>__NKENM__</b> kenmerken, <b>__NINVOER__</b> invoer-bronnen (nergens geproduceerd),
              <b>__NEIND__</b> productloze eind-outputs.</p>
            <div class="legenda">
              <span class="lg-regel">regel</span>
              <span class="lg-attr">attribuut</span>
              <span class="lg-kenm">kenmerk</span>
              <span class="lg-inv">invoer (request/koppelvlak)</span>
              <span class="lg-eind">productloze eind-output</span>
            </div>
            <div class="graaf"><pre class="mermaid">
            __MERMAID__
            </pre></div>
            </body>
            </html>
            """
            .replace("__NREGEL__", Long.toString(nRule))
            .replace("__NATTR__", Long.toString(nAttr))
            .replace("__NKENM__", Long.toString(nCharacteristic))
            .replace("__NINVOER__", Long.toString(nInput))
            .replace("__NEIND__", Long.toString(nEnd))
            .replace("__MERMAID__", mmd);
    }

    /** Escape a label for a Mermaid quoted string: double-quotes break the "…" node text, so entity-encode them. */
    private static String escapeMermaid(String s) {
        return s.replace("\"", "&quot;");
    }
}
