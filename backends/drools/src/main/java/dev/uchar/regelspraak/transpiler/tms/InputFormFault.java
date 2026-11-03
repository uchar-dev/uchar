package dev.uchar.regelspraak.transpiler.tms;

import dev.uchar.regelspraak.message.Msg;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * An evaluation fault of the exec-model branch: a form reads the case input of an attribute on a timeline in the
 * one convention, and the case gives it in the other (RUNTIME-SEMANTICS ch. 8.4: "An executor that needs the one and
 * finds the other faults — never guesses"). The asOfDate-bucket scalar ({@link BucketScalar}) serves the subject's own
 * total without a clip (ch. 3.6); the full timeline serves the clip of a total and the timeline a comparison clip
 * compares (ch. 3.6), the base of a pro-rata part (ch. 3.7), a member of a per-element fold (ch. 3.6, §7.1), the
 * period arithmetic and a condition per period (ch. 3.4, 3.5, 3.10), the whole-period qualifier (ch. 3.9) and an
 * ever-unique key (ch. 5.9).
 *
 * <p>The sentence is the reference interpreter's where that executor names the fault, and it names the object whose
 * input it is: by its case label where the write-back is given one ({@link #named}), as the reference interpreter
 * names an instance, and by type and identity where none is known, as for a consumer that fires a KieBase itself or for
 * an object a creation made. Like a {@link ComputeFault} it never leaves the fire: the consequence or the fault rule that
 * meets it inserts it logically as a {@link Fault}, which counts at the fixpoint.
 *
 * <p>A form that reads a timeline whole and finds the asOfDate-bucket scalar is the fault kind EF-037 (ch. 5.7b,
 * {@link #bucketScalar}): its parameters are the attribute and the label of the object whose input it is, the latter
 * only where the sentence names that object itself and not the subject of the rule along a path. The other faults of
 * this class have no code.
 */
public final class InputFormFault extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final String code;
    private final String attribute;
    private final boolean owner;
    private final String before;
    private final transient Object object;
    private final String objectName;
    private final String after;
    private final String rule;

    /** The fault without a code whose sentence is {@code before}, the object {@code object}, then {@code after}. */
    InputFormFault(String before, Object object, String after) {
        this(null, null, false, before, object, FactNames.object(object), after, null);
    }

    private InputFormFault(String code, String attribute, boolean owner, String before, Object object,
                           String objectName, String after, String rule) {
        super(rule == null ? sentence(before, objectName, after)
                : Msg.drools("FRAME.fault-in-rule", "rule", rule, "sentence", sentence(before, objectName, after)).text());
        this.code = code;
        this.attribute = attribute;
        this.owner = owner;
        this.before = before;
        this.object = object;
        this.objectName = objectName;
        this.after = after;
        this.rule = rule;
    }

    /** The fault EF-037 of a form that reads the timeline of {@code attribute} whole and finds the asOfDate-bucket
     *  scalar, in the sentence {@code before}, the object {@code object}, {@code after}; {@code owner} says whether
     *  {@code object} is the object whose input it is, which the parameter {@code label} then names. */
    static InputFormFault bucketScalar(String attribute, boolean owner, String before, Object object, String after) {
        return new InputFormFault("EF-037", attribute, owner, before, object, FactNames.object(object), after, null);
    }

    private static String sentence(String before, String objectName, String after) {
        return before + "\"" + objectName + "\"" + after;
    }

    /** The same fault named after the rule {@code rule} it ran in, as {@link ComputeFault#inRule} names one. */
    public InputFormFault inRule(String rule) {
        return new InputFormFault(code, attribute, owner, before, object, objectName, after, rule);
    }

    /** The same fault with the object named by {@code label}, which gives the case label of an input object and null
     *  for one a creation made, which keeps its name by type and identity. */
    public InputFormFault named(Function<Object, String> label) {
        String l = label.apply(object);
        return l == null ? this : new InputFormFault(code, attribute, owner, before, object, l, after, rule);
    }

    /** The sentence of the fault, without the rule. */
    public String sentence() {
        return sentence(before, objectName, after);
    }

    /** The rule a named fault ran in; null where nothing named it yet. */
    public String rule() {
        return rule;
    }

    /** The object whose input the form could not read, or, along a path, the subject of the rule. */
    public Object object() {
        return object;
    }

    /** The fault kind (RUNTIME-SEMANTICS ch. 5.7b): EF-037 for the asOfDate-bucket scalar, null for the others. */
    public String code() {
        return code;
    }

    /** The parameters of the kind (ch. 5.7b): the attribute, and the object whose input it is where the sentence
     *  names that object itself. Empty for a fault without a code. */
    public Map<String, String> params() {
        Map<String, String> p = new LinkedHashMap<>();
        if (code == null) return p;
        p.put("attribute", attribute);
        if (owner) p.put("label", objectName);
        return p;
    }
}
