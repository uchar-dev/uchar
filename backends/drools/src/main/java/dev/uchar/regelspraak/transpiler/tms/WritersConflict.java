package dev.uchar.regelspraak.transpiler.tms;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

/**
 * Two or more writers of one attribute of one object that stand at the fixpoint with different values: no fact base
 * satisfies both equations, so the session has no fixpoint and faults (RUNTIME-SEMANTICS ch. 5.7, "Divergence by two
 * writers of one target"). A writer that fired with a leeg value wrote leeg (ch. 1.2g); a standing writer that computed
 * leeg wrote nothing and is not among them (ch. 7.8).
 *
 * <p>The sentence names the writers in the order of the document, each with its value, the attribute as declared and
 * the object, as the reference interpreter names them and in the words the replay gave the writers of the DRL-text
 * branch at its fire cap until card drl-teksttak-op-truth-maintenance, so the fragment a red case pins for Drools
 * matches it. The
 * object is named by its case label where the write-back is given one ({@link #named}), "een aangemaakte" type where
 * the object has none because a creation made it, and by type and identity where no labels are known, as for a consumer
 * that fires a KieBase itself.
 */
public final class WritersConflict extends IllegalStateException {

    private static final long serialVersionUID = 1L;

    /** One writer and the value it writes, null for leeg. */
    public record WriteAct(String writer, Object value) {}

    private final transient Object onderwerp;
    private final String attribute;
    private final String type;
    private final List<WriteAct> writers;
    private final String label;

    /** {@code writers} in the order of the document; {@code type} is the object's own type, {@code attribute} the
     *  attribute as declared. The object is named by type and identity until {@link #named} gives it a label. */
    WritersConflict(Object onderwerp, String attribute, String type, List<WriteAct> writers) {
        this(onderwerp, attribute, type, writers, "een " + type + " (" + FactNames.object(onderwerp) + ")", null);
    }

    /** The conflict of the value facts {@code waarden} on attribute {@code attribute} of {@code onderwerp}: the
     *  writers as {@link #inDocumentOrder} ranks them over the document's rules {@code documentOrder}, and the
     *  object's own type named by the type in {@code types} whose bean class's simple name stands at the same place in
     *  {@code typeClasses}, or by that simple name where none does. */
    static WritersConflict van(Object onderwerp, String attribute, List<Value> waarden, List<?> documentOrder,
                                  List<?> typeClasses, List<?> types) {
        List<Value> ordered = new ArrayList<>(waarden);
        ordered.sort(inDocumentOrder(documentOrder));
        List<WriteAct> acts = new ArrayList<>();
        for (Value w : ordered) acts.add(new WriteAct(w.writer(), w.value()));
        String javaClass = onderwerp.getClass().getSimpleName();
        int t = typeClasses.indexOf(javaClass);
        return new WritersConflict(onderwerp, attribute, t < 0 ? javaClass : (String) types.get(t), acts);
    }

    /** The value facts of one target by their writers: the rules in the order of the document's rules
     *  {@code documentOrder}, and after them, by name, a writer that is not among them, an effective defaults stack
     *  or an allocation. This is the order in which a conflict names the writers, and the order in which the reader of
     *  the fixpoint takes the value of writers that agree.
     *
     *  <p>The place by name only makes the order total; in a document the analysis lowers it decides nothing, since a
     *  stack and an allocation are there the only writer of their target on an object. WF-026 allows one allocation per
     *  target and WF-050 a defaults group no writer outside its effective stack; the analysis refuses an allocation
     *  beside any other writer and a rule beside a group of its own type, and folds the rules of the other types of a
     *  group's chain into the stack (RUNTIME-SEMANTICS ch. 7.10). The declared-unordered groups on one target of a type
     *  are members of one stack, whose reconciliation writes, where they agree, the decision of the first deciding group
     *  in the document (ch. 7.2). */
    static Comparator<Value> inDocumentOrder(List<?> documentOrder) {
        return Comparator.comparing((Value w) -> place(documentOrder, w.writer())).thenComparing(Value::writer);
    }

    /** The place of the rule {@code writer} in {@code documentOrder}; after every rule where it is none. */
    private static int place(List<?> documentOrder, String writer) {
        int i = documentOrder.indexOf(writer);
        return i < 0 ? Integer.MAX_VALUE : i;
    }

    private WritersConflict(Object onderwerp, String attribute, String type, List<WriteAct> writers,
                               String object, String label) {
        super(sentence(writers, attribute, object));
        if (writers.size() < 2) throw new IllegalArgumentException("een conflict heeft twee schrijvers: " + writers);
        this.onderwerp = onderwerp;
        this.attribute = attribute;
        this.type = type;
        this.writers = List.copyOf(writers);
        this.label = label;
    }

    /** The same conflict with the object named by {@code label}, which gives the case label of an input object and null
     *  for one a creation made. */
    public WritersConflict named(Function<Object, String> label) {
        String l = label.apply(onderwerp);
        return new WritersConflict(onderwerp, attribute, type, writers,
                l == null ? "een aangemaakte " + type : l + ", een " + type, l);
    }

    /** The fault kind (RUNTIME-SEMANTICS ch. 5.7b): two writers of one target that disagree. */
    public String code() {
        return "EF-016";
    }

    /** The parameters of the fault kind (RUNTIME-SEMANTICS ch. 5.7b), in the order of the table: the writers and their
     *  values (leeg for no value), the attribute as declared, the case label of the object where it has one, and its
     *  type. */
    public java.util.Map<String, String> params() {
        java.util.Map<String, String> p = new java.util.LinkedHashMap<>();
        p.put("rules", String.join(", ", writers.stream().map(WriteAct::writer).toList()));
        p.put("values", String.join(", ", writers.stream().map(w -> w.value() == null ? "leeg" : w.value().toString()).toList()));
        p.put("attribute", attribute);
        if (label != null) p.put("label", label);
        p.put("objectType", type);
        return p;
    }

    /** The object whose attribute the writers disagree on. */
    public Object onderwerp() {
        return onderwerp;
    }

    /** The writers with their values, in the order of the document. */
    public List<WriteAct> writers() {
        return writers;
    }

    private static String sentence(List<WriteAct> writers, String attribute, String object) {
        List<String> parts = new ArrayList<>();
        for (WriteAct d : writers)
            parts.add("de regel \"" + d.writer() + "\" schrijft " + (d.value() == null ? "leeg" : d.value()));
        String enumeration = String.join(", ", parts.subList(0, parts.size() - 1)) + " en " + parts.get(parts.size() - 1);
        return "geen fixpunt: " + count(writers.size()) + " schrijvers van één doel geven verschillende waarden — "
                + enumeration + " in het attribuut " + attribute + " van " + object
                + "; divergentie is een fout (RUNTIME-SEMANTICS ch. 5.7)";
    }

    private static String count(int n) {
        return switch (n) { case 2 -> "twee"; case 3 -> "drie"; case 4 -> "vier"; default -> String.valueOf(n); };
    }
}
