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
 *   <li>{@link #bean}: a bean of the build's bean model, its attributes, characteristics and reference fields set. The
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
 */
public final class Invoer {

    private final List<Object> beans = new ArrayList<>();
    private final List<Bucket> emmers = new ArrayList<>();
    private final List<ToManyList> lists = new ArrayList<>();

    /** A bucket scalar: attribute {@code attribuut} of {@code bean}, named as the model declares it. */
    record Bucket(Object bean, String attribuut) {}

    /** The members of a to-many list in their order: {@code leden}, each holding {@code houder} in its reference field
     *  {@code rol}. */
    record ToManyList(Object houder, String rol, List<Object> members) {}

    /** Adds {@code bean}. */
    public Invoer bean(Object bean) {
        beans.add(Objects.requireNonNull(bean, "bean"));
        return this;
    }

    /** Adds every bean of {@code beans}, in the collection's order. */
    public Invoer beans(Collection<?> beans) {
        for (Object b : Objects.requireNonNull(beans, "beans")) bean(b);
        return this;
    }

    /** States that the bean {@code bean} holds the input of attribute {@code attribuut}, an attribute on a timeline
     *  named as the model declares it, as the asOfDate-bucket scalar (RUNTIME-SEMANTICS ch. 8.4). */
    public Invoer emmerscalair(Object bean, String attribuut) {
        emmers.add(new Bucket(Objects.requireNonNull(bean, "bean"), Objects.requireNonNull(attribuut, "attribuut")));
        return this;
    }

    /** States the order of a to-many list: {@code leden} are the members whose reference field {@code rol} holds
     *  {@code houder}, in this order. {@code rol} is the role the holder plays in the fact type, the name of that field;
     *  the list must name every bean of this input whose field holds the holder, and each once. */
    public Invoer lijst(Object houder, String rol, List<?> members) {
        Objects.requireNonNull(members, "leden");
        List<Object> l = new ArrayList<>();
        for (Object member : members) l.add(Objects.requireNonNull(member, "lid"));
        lists.add(new ToManyList(Objects.requireNonNull(houder, "houder"), Objects.requireNonNull(rol, "rol"), List.copyOf(l)));
        return this;
    }

    List<Object> beans() {
        return List.copyOf(beans);
    }

    List<Bucket> emmers() {
        return List.copyOf(emmers);
    }

    List<ToManyList> lists() {
        return List.copyOf(lists);
    }
}
