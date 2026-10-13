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
 * the object, as the reference interpreter names them and in the words the replay gives the writers of the DRL-text
 * branch at its fire cap ({@code cases.Schrijfconflict}), so the fragment a red case pins for Drools matches it. The
 * object is named by its case label where the write-back is given one ({@link #benoemd}), "een aangemaakte" type where
 * the object has none because a creation made it, and by type and identity where no labels are known, as for a consumer
 * that fires a KieBase itself.
 */
public final class Schrijversconflict extends IllegalStateException {

    private static final long serialVersionUID = 1L;

    /** One writer and the value it writes, null for leeg. */
    public record Schrijfdaad(String schrijver, Object waarde) {}

    private final transient Object onderwerp;
    private final String attribuut;
    private final String type;
    private final List<Schrijfdaad> schrijvers;

    /** {@code schrijvers} in the order of the document; {@code type} is the object's own type, {@code attribuut} the
     *  attribute as declared. The object is named by type and identity until {@link #benoemd} gives it a label. */
    Schrijversconflict(Object onderwerp, String attribuut, String type, List<Schrijfdaad> schrijvers) {
        this(onderwerp, attribuut, type, schrijvers, "een " + type + " (" + Feitnamen.object(onderwerp) + ")");
    }

    /** The conflict of the value facts {@code waarden} on attribute {@code attribuut} of {@code onderwerp}: the
     *  writers in the order of the document's rules {@code documentvolgorde}, a writer the document has no rule of,
     *  such as a defaults group, after them by name, and the object's own type named by the type in {@code typen} whose
     *  bean class's simple name stands at the same place in {@code typeklassen}, or by that simple name where none
     *  does. */
    static Schrijversconflict van(Object onderwerp, String attribuut, List<Waarde> waarden, List<?> documentvolgorde,
                                  List<?> typeklassen, List<?> typen) {
        List<Waarde> geordend = new ArrayList<>(waarden);
        geordend.sort(Comparator.comparing((Waarde w) -> plaats(documentvolgorde, w.schrijver()))
                .thenComparing(Waarde::schrijver));
        List<Schrijfdaad> daden = new ArrayList<>();
        for (Waarde w : geordend) daden.add(new Schrijfdaad(w.schrijver(), w.waarde()));
        String klasse = onderwerp.getClass().getSimpleName();
        int t = typeklassen.indexOf(klasse);
        return new Schrijversconflict(onderwerp, attribuut, t < 0 ? klasse : (String) typen.get(t), daden);
    }

    /** The place of the rule {@code schrijver} in {@code documentvolgorde}; after every rule where it is none. */
    private static int plaats(List<?> documentvolgorde, String schrijver) {
        int i = documentvolgorde.indexOf(schrijver);
        return i < 0 ? Integer.MAX_VALUE : i;
    }

    private Schrijversconflict(Object onderwerp, String attribuut, String type, List<Schrijfdaad> schrijvers,
                               String object) {
        super(zin(schrijvers, attribuut, object));
        if (schrijvers.size() < 2) throw new IllegalArgumentException("een conflict heeft twee schrijvers: " + schrijvers);
        this.onderwerp = onderwerp;
        this.attribuut = attribuut;
        this.type = type;
        this.schrijvers = List.copyOf(schrijvers);
    }

    /** The same conflict with the object named by {@code label}, which gives the case label of an input object and null
     *  for one a creation made. */
    public Schrijversconflict benoemd(Function<Object, String> label) {
        String l = label.apply(onderwerp);
        return new Schrijversconflict(onderwerp, attribuut, type, schrijvers,
                l == null ? "een aangemaakte " + type : l + ", een " + type);
    }

    /** The object whose attribute the writers disagree on. */
    public Object onderwerp() {
        return onderwerp;
    }

    /** The writers with their values, in the order of the document. */
    public List<Schrijfdaad> schrijvers() {
        return schrijvers;
    }

    private static String zin(List<Schrijfdaad> schrijvers, String attribuut, String object) {
        List<String> delen = new ArrayList<>();
        for (Schrijfdaad d : schrijvers)
            delen.add("de regel \"" + d.schrijver() + "\" schrijft " + (d.waarde() == null ? "leeg" : d.waarde()));
        String opsomming = String.join(", ", delen.subList(0, delen.size() - 1)) + " en " + delen.get(delen.size() - 1);
        return "geen fixpunt: " + aantal(schrijvers.size()) + " schrijvers van één doel geven verschillende waarden — "
                + opsomming + " in het attribuut " + attribuut + " van " + object
                + "; divergentie is een fout (RUNTIME-SEMANTICS ch. 5.7)";
    }

    private static String aantal(int n) {
        return switch (n) { case 2 -> "twee"; case 3 -> "drie"; case 4 -> "vier"; default -> String.valueOf(n); };
    }
}
