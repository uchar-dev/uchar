package dev.uchar.regelspraak.loader;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * Where the nodes of a loaded document stand on the wire: for every node of the condition union (IR-SPEC ch. 4.5) and of
 * the expression union (IR-SPEC ch. 4) that the loader read into a record, its JSON pointer (RFC 6901), its place in
 * the document's pre-order and its kind as the wire spells it ({@code Comparison}, {@code Aggregation},
 * {@code ContextValue}, …); and for every version of a named form (IR-SPEC ch. 3, Versioning) that the loader read into
 * a record, the pointer of the version. A side table beside the model, as the loader's other tables are: the records
 * are those of the vendored frontend slice, which carry no position, and the table is keyed on them by identity, since
 * two nodes of equal content are two records. The lowering reads it only for a build with readings
 * ({@code TmsTranspiler}), which numbers the nodes of a form by their order here; beside the lowering,
 * {@code TmsTranspiler#originOf} names the fold of a rule of a fold by the pointer of its node, in every build, since
 * every build takes the table.
 */
public final class NodePositions {

    /** One node: its pointer, its place in the pre-order of the document, and its kind as the wire spells it. */
    public record Position(String pointer, int order, String kind) {}

    private final Map<Object, Position> nodes = new IdentityHashMap<>();
    private final Map<Object, String> versions = new IdentityHashMap<>();
    /** The nodes by their place in the pre-order of the document; made once, when the loader has read the whole
     *  document ({@link #complete}), never at a read: threads that explain sessions side by side read one table, and
     *  none may search the pre-order while another is still sorting it. */
    private List<Object> inOrder;

    NodePositions() {}

    void node(Object node, String pointer, int order, String kind) {
        open(pointer);
        if (nodes.put(node, new Position(pointer, order, kind)) != null)
            throw new IllegalStateException("IrLoader: the node at " + pointer + " is read twice");
    }

    void version(Object form, String pointer) {
        open(pointer);
        if (versions.put(form, pointer) != null)
            throw new IllegalStateException("IrLoader: the version at " + pointer + " is read twice");
    }

    /** Closes the table once the loader has read the whole document: its pre-order is made here, and no node or version
     *  comes after. */
    NodePositions complete() {
        if (inOrder != null) throw new IllegalStateException("IrLoader: the positions are completed twice");
        List<Object> sorted = new ArrayList<>(nodes.keySet());
        sorted.sort(Comparator.comparingInt(n -> nodes.get(n).order()));
        inOrder = List.copyOf(sorted);
        return this;
    }

    private void open(String pointer) {
        if (inOrder != null)
            throw new IllegalStateException("IrLoader: " + pointer + " is read after the positions were completed");
    }

    /** The position of {@code node}, a record of the condition or the expression union this loader read; null for a
     *  record it did not read, such as one the lowering makes itself. */
    public Position of(Object node) {
        return nodes.get(node);
    }

    /** The node this loader read at {@code pointer}, a JSON pointer into the document; null where it read none there. */
    public Object at(String pointer) {
        for (Map.Entry<Object, Position> e : nodes.entrySet())
            if (e.getValue().pointer().equals(pointer)) return e.getKey();
        return null;
    }

    /** The pointer of the version {@code form} stands for, a record of a rule, a consistency, an allocation or a creation
     *  this loader read; null for any other. */
    public String versionOf(Object form) {
        return versions.get(form);
    }

    /** {@code root}, a node this loader read, and every node it read below it on the wire, in the pre-order of the
     *  document: a node before its fields, the fields in the order the document writes them, an array in its order. */
    public List<Object> subtree(Object root) {
        Position r = nodes.get(root);
        if (r == null) throw new IllegalArgumentException("no node of this document: " + root);
        if (inOrder == null) throw new IllegalStateException("the positions are read before the loader completed them");
        int lo = 0, hi = inOrder.size() - 1;
        while (lo < hi) {
            int mid = (lo + hi) >>> 1;
            if (nodes.get(inOrder.get(mid)).order() < r.order()) lo = mid + 1;
            else hi = mid;
        }
        List<Object> out = new ArrayList<>();
        String below = r.pointer() + "/";
        for (int i = lo; i < inOrder.size(); i++) {
            Object n = inOrder.get(i);
            String p = nodes.get(n).pointer();
            if (n != root && !p.startsWith(below)) break;
            out.add(n);
        }
        return out;
    }
}
