package dev.uchar.regelspraak.transpiler.tms;

import java.util.function.Function;

/**
 * An evaluation fault of the exec-model branch: a form reads the case input of an attribute on a timeline in the
 * one convention, and the case gives it in the other (RUNTIME-SEMANTICS ch. 8.4: "An executor that needs the one and
 * finds the other faults — never guesses"). The asOfDate-bucket scalar ({@link Emmerscalair}) serves the subject's own
 * total without a clip (ch. 3.6); the full timeline serves the clip of a total and the timeline a comparison clip
 * compares (ch. 3.6), the base of a pro-rata part (ch. 3.7), a member of a per-element fold (ch. 3.6, §7.1) and an
 * ever-unique key (ch. 5.9).
 *
 * <p>The sentence is the reference interpreter's where that executor names the fault, and it names the object whose
 * input it is: by its case label where the write-back is given one ({@link #benoemd}), as the reference interpreter
 * names an instance, and by type and identity where none is known, as for a consumer that fires a KieBase itself or for
 * an object a creation made. Like a {@link Rekenfout} it never leaves the fire: the consequence or the fault rule that
 * meets it inserts it logically as a {@link Fout}, which counts at the fixpoint.
 */
public final class Invoervormfout extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final String voor;
    private final transient Object object;
    private final String objectnaam;
    private final String na;
    private final String regel;

    /** The fault whose sentence is {@code voor}, the object {@code object}, then {@code na}. */
    Invoervormfout(String voor, Object object, String na) {
        this(voor, object, "\"" + Feitnamen.object(object) + "\"", na, null);
    }

    private Invoervormfout(String voor, Object object, String objectnaam, String na, String regel) {
        super(regel == null ? voor + objectnaam + na : "fout in de regel \"" + regel + "\": " + voor + objectnaam + na);
        this.voor = voor;
        this.object = object;
        this.objectnaam = objectnaam;
        this.na = na;
        this.regel = regel;
    }

    /** The same fault named after the rule {@code regel} it ran in, as {@link Rekenfout#inDeRegel} names one. */
    public Invoervormfout inDeRegel(String regel) {
        return new Invoervormfout(voor, object, objectnaam, na, regel);
    }

    /** The same fault with the object named by {@code label}, which gives the case label of an input object and null
     *  for one a creation made, which keeps its name by type and identity. */
    public Invoervormfout benoemd(Function<Object, String> label) {
        String l = label.apply(object);
        return l == null ? this : new Invoervormfout(voor, object, "\"" + l + "\"", na, regel);
    }

    /** The sentence of the fault, without the rule. */
    public String zin() {
        return voor + objectnaam + na;
    }

    /** The rule a named fault ran in; null where nothing named it yet. */
    public String regel() {
        return regel;
    }

    /** The object whose input the form could not read. */
    public Object object() {
        return object;
    }
}
