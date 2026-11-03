package dev.uchar.regelspraak.transpiler.tms;

import dev.uchar.regelspraak.message.Msg;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * The evaluation fault of a temporal total that meets a segment with an open bound whose value is not 0
 * (RUNTIME-SEMANTICS ch. 3.6, EF-036): such a segment holds unboundedly many units, so the total has no value. Raised
 * by every route of the total that reaches one: the subject's own total of a timeline that period arithmetic writes
 * (ch. 3.4, 3.5), the gedurende clip, whose fragment of an open segment keeps its open bound, and the per-element fold
 * over a set (§7.1), where it names the member ({@link PeriodArithmetic#total}, {@link PeriodArithmetic#openNonZero}).
 *
 * <p>The parameters are the totalled attribute and the label of the object whose timeline it is (ch. 5.7b). The object
 * is named by its case label where the write-back is given one ({@link #named}), as the reference interpreter names an
 * instance, and by type and identity where none is known. Like a {@link ComputeFault} it never leaves the fire: the
 * consequence that meets it inserts it logically as a {@link Fault}, which counts at the fixpoint.
 */
public final class OpenSegmentFault extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final String attribute;
    private final transient Object object;
    private final String objectName;
    private final String value;
    private final String rule;

    /** The fault of the total of {@code attribute} of {@code object} over an open segment that carries {@code value}. */
    OpenSegmentFault(String attribute, Object object, BigDecimal value) {
        this(attribute, object, FactNames.object(object), text(value), null);
    }

    private OpenSegmentFault(String attribute, Object object, String objectName, String value, String rule) {
        super(rule == null ? sentence(attribute, objectName, value)
                : Msg.drools("FRAME.fault-in-rule", "rule", rule, "sentence", sentence(attribute, objectName, value)).text());
        this.attribute = attribute;
        this.object = object;
        this.objectName = objectName;
        this.value = value;
        this.rule = rule;
    }

    private static String sentence(String attribute, String objectName, String value) {
        return Msg.drools("EF-036", "attribute", attribute, "label", objectName, "value", value).text();
    }

    /** A value as the reference interpreter writes a number in this sentence: without trailing zeros. */
    private static String text(BigDecimal v) {
        return v.signum() == 0 ? "0" : v.stripTrailingZeros().toPlainString();
    }

    /** The same fault named after the rule {@code rule} it ran in, as {@link ComputeFault#inRule} names one. */
    public OpenSegmentFault inRule(String rule) {
        return new OpenSegmentFault(attribute, object, objectName, value, rule);
    }

    /** The same fault with the object named by {@code label}, which gives the case label of an input object and null
     *  for one a creation made, which keeps its name by type and identity. */
    public OpenSegmentFault named(Function<Object, String> label) {
        String l = label.apply(object);
        return l == null ? this : new OpenSegmentFault(attribute, object, l, value, rule);
    }

    /** The sentence of the fault, without the rule. */
    public String sentence() {
        return sentence(attribute, objectName, value);
    }

    /** The rule a named fault ran in; null where nothing named it yet. */
    public String rule() {
        return rule;
    }

    /** The fault kind (RUNTIME-SEMANTICS ch. 5.7b). */
    public String code() {
        return "EF-036";
    }

    /** The parameters of the kind (ch. 5.7b): the totalled attribute and the object whose timeline it is. */
    public Map<String, String> params() {
        Map<String, String> p = new LinkedHashMap<>();
        p.put("attribute", attribute);
        p.put("label", objectName);
        return p;
    }
}
