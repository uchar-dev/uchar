package dev.uchar.regelspraak.transpiler.tms;

import dev.uchar.regelspraak.message.Msg;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * The evaluation fault of the true periods a rule writes into a time-dependent characteristic (RUNTIME-SEMANTICS
 * ch. 3.10, "A time-dependent characteristic within a window"; ch. 3.11; {@link PeriodCondition#characteristicPeriods}):
 * periods with an open bound, which no reader of the characteristic's buckets can walk (EF-023), and true periods the
 * case supplies for a characteristic a rule writes, a form ch. 3.12 leaves without a meaning (EF-041).
 *
 * <p>The object is named by its case label where the write-back is given one ({@link #named}), as the reference
 * interpreter names an instance, and by type and identity where none is known. Like a {@link ComputeFault} it never
 * leaves the fire: the consequence that meets it inserts it logically as a {@link Fault}, which counts at the fixpoint.
 */
public final class CharacteristicPeriodsFault extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /** The two faults: periods with an open bound, and true periods the case supplies. */
    public enum Kind { OPEN_BOUND, SUPPLIED }

    private final Kind kind;
    private final String characteristic;
    private final transient Object object;
    private final String objectName;
    private final String rule;

    /** The fault {@code kind} of the true periods a rule writes into {@code characteristic} of {@code object}. */
    CharacteristicPeriodsFault(Kind kind, String characteristic, Object object) {
        this(kind, characteristic, object, FactNames.object(object), null);
    }

    private CharacteristicPeriodsFault(Kind kind, String characteristic, Object object, String objectName, String rule) {
        super(rule == null ? sentence(kind, characteristic, objectName)
                : Msg.drools("FRAME.fault-in-rule", "rule", rule, "sentence", sentence(kind, characteristic, objectName))
                .text());
        this.kind = kind;
        this.characteristic = characteristic;
        this.object = object;
        this.objectName = objectName;
        this.rule = rule;
    }

    private static String sentence(Kind kind, String characteristic, String objectName) {
        return Msg.drools(kind == Kind.OPEN_BOUND ? "EF-023" : "PART.characteristic-periods-supplied",
                "characteristic", characteristic, "label", objectName).text();
    }

    /** The same fault named after the rule {@code rule} it ran in, as {@link ComputeFault#inRule} names one. */
    public CharacteristicPeriodsFault inRule(String rule) {
        return new CharacteristicPeriodsFault(kind, characteristic, object, objectName, rule);
    }

    /** The same fault with the object named by {@code label}, which gives the case label of an input object and null
     *  for one a creation made, which keeps its name by type and identity. */
    public CharacteristicPeriodsFault named(Function<Object, String> label) {
        String l = label.apply(object);
        return l == null ? this : new CharacteristicPeriodsFault(kind, characteristic, object, l, rule);
    }

    /** The sentence of the fault, without the rule. */
    public String sentence() {
        return sentence(kind, characteristic, objectName);
    }

    /** The rule a named fault ran in; null where nothing named it yet. */
    public String rule() {
        return rule;
    }

    /** The fault kind (RUNTIME-SEMANTICS ch. 5.7b): EF-023 for periods with an open bound, EF-041 for supplied ones. */
    public String code() {
        return kind == Kind.OPEN_BOUND ? "EF-023" : "EF-041";
    }

    /** The parameters of the kind (ch. 5.7b): the characteristic, and for supplied periods the object whose input they
     *  are. */
    public Map<String, String> params() {
        Map<String, String> p = new LinkedHashMap<>();
        p.put("characteristic", characteristic);
        if (kind == Kind.SUPPLIED) p.put("label", objectName);
        return p;
    }
}
