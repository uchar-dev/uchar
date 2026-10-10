package dev.uchar.regelspraak.cases;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * The input of one session, as {@link DroolsBackend#voegIn} puts it in: the fact beans, and beside them what the bean
 * model does not hold and the exec-model branch reads, so that a consumer names it in the words of the model and
 * never states a fact class of that branch itself.
 *
 * <ul>
 *   <li>{@link #boon}: a bean of the build's bean model, its attributes, characteristics and reference fields set. The
 *       order of the beans is the default order of every to-many list (below).</li>
 *   <li>{@link #emmerscalair}: an attribute on a timeline whose input the bean holds as the one segment of the period of
 *       the attribute's grid that holds the evaluation date, the asOfDate-bucket convention of RUNTIME-SEMANTICS ch.
 *       8.4. The bean cannot tell that scalar from a full timeline of one segment, and the two are read apart: the
 *       bucket scalar serves the subject's own total without a clip, the full timeline every other read, and a form
 *       that needs the one and finds the other faults. Without it the branch reads the input as a full timeline.</li>
 *   <li>{@link #lijst}: the order of the members of a to-many list. The bean model stores a to-many role as a reference
 *       field on each member, toward the holder, and keeps no order; the order decides which member a fault names
 *       where a fold reads its members one by one and stops at the first fault (RUNTIME-SEMANTICS ch. 5.11), and
 *       never a value. Without it a list holds its members in the order of the beans.</li>
 * </ul>
 *
 * The DRL-text branch reads the beans alone and puts in nothing else.
 */
public final class Invoer {

    private final List<Object> bonen = new ArrayList<>();
    private final List<Emmer> emmers = new ArrayList<>();
    private final List<Lijst> lijsten = new ArrayList<>();

    /** A bucket scalar: attribute {@code attribuut} of {@code boon}, named as the model declares it. */
    record Emmer(Object boon, String attribuut) {}

    /** The members of a to-many list in their order: {@code leden}, each holding {@code houder} in its reference field
     *  {@code rol}. */
    record Lijst(Object houder, String rol, List<Object> leden) {}

    /** Adds {@code boon}. */
    public Invoer boon(Object boon) {
        bonen.add(Objects.requireNonNull(boon, "boon"));
        return this;
    }

    /** Adds every bean of {@code bonen}, in the collection's order. */
    public Invoer bonen(Collection<?> bonen) {
        for (Object b : Objects.requireNonNull(bonen, "bonen")) boon(b);
        return this;
    }

    /** States that the bean {@code boon} holds the input of attribute {@code attribuut}, an attribute on a timeline
     *  named as the model declares it, as the asOfDate-bucket scalar (RUNTIME-SEMANTICS ch. 8.4). */
    public Invoer emmerscalair(Object boon, String attribuut) {
        emmers.add(new Emmer(Objects.requireNonNull(boon, "boon"), Objects.requireNonNull(attribuut, "attribuut")));
        return this;
    }

    /** States the order of a to-many list: {@code leden} are the members whose reference field {@code rol} holds
     *  {@code houder}, in this order. {@code rol} is the role the holder plays in the fact type, the name of that field;
     *  the list must name every bean of this input whose field holds the holder, and each once. */
    public Invoer lijst(Object houder, String rol, List<?> leden) {
        Objects.requireNonNull(leden, "leden");
        List<Object> l = new ArrayList<>();
        for (Object lid : leden) l.add(Objects.requireNonNull(lid, "lid"));
        lijsten.add(new Lijst(Objects.requireNonNull(houder, "houder"), Objects.requireNonNull(rol, "rol"), List.copyOf(l)));
        return this;
    }

    List<Object> bonen() {
        return List.copyOf(bonen);
    }

    List<Emmer> emmers() {
        return List.copyOf(emmers);
    }

    List<Lijst> lijsten() {
        return List.copyOf(lijsten);
    }
}
