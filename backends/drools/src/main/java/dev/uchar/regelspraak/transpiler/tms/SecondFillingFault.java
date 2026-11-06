package dev.uchar.regelspraak.transpiler.tms;

import dev.uchar.regelspraak.message.Msg;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * The evaluation fault of a to-one role with two different fillers at the fixpoint (RUNTIME-SEMANTICS ch. 5.5, EF-017):
 * one object holds two links of a created relation to two different objects over a role it may fill once, and a second
 * filling is a fault, never an overwrite. The rule that finds it belongs to no form: it reads the links of the
 * relation, whichever form or input laid them, so the {@link Fault} that carries it names the fact type of the relation
 * in its {@code rule}, and {@link EvaluationFaults} names it as a fact type.
 *
 * <p>The parameters are the role and the label of the object that holds it (ch. 5.7b); the two fillers are not named,
 * since the rule meets them as a pair in either order and the fault must be one fact. The object is named by its case
 * label where the write-back is given one ({@link #named}), as the reference interpreter names an instance, and by type
 * and identity where none is known. Like a {@link WritersConflict} it never leaves the fire: the rule that finds it
 * inserts it logically as a {@link Fault}, which counts at the fixpoint.
 */
public final class SecondFillingFault extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final String role;
    private final transient Object object;
    private final String objectName;

    /** The fault of the to-one role {@code role} of {@code object}, which holds two different fillers. Public, since
     *  the DRL text the DRL-text branch generates constructs it. */
    public SecondFillingFault(String role, Object object) {
        this(role, object, FactNames.object(object));
    }

    private SecondFillingFault(String role, Object object, String objectName) {
        super(Msg.drools("EF-017", "role", role, "label", objectName).text());
        this.role = role;
        this.object = object;
        this.objectName = objectName;
    }

    /** The same fault with the object named by {@code label}, which gives the case label of an input object and null
     *  for one a creation made, which keeps its name by type and identity. */
    public SecondFillingFault named(Function<Object, String> label) {
        String l = label.apply(object);
        return l == null ? this : new SecondFillingFault(role, object, l);
    }

    /** The fault kind (RUNTIME-SEMANTICS ch. 5.7b). */
    public String code() {
        return "EF-017";
    }

    /** The parameters of the kind (ch. 5.7b): the role and the object that holds it. */
    public Map<String, String> params() {
        Map<String, String> p = new LinkedHashMap<>();
        p.put("role", role);
        p.put("label", objectName);
        return p;
    }
}
