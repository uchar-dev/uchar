package dev.uchar.regelspraak.cases;

import dev.uchar.regelspraak.model.RegelSpraakModel;
import dev.uchar.regelspraak.model.RegelSpraakModel.ObjectType;
import dev.uchar.regelspraak.model.Unit;
import dev.uchar.regelspraak.runtime.RegelSpraakRuntime;
import dev.uchar.regelspraak.schema.FactModelGenerator;
import dev.uchar.regelspraak.transpiler.core.Namen;
import dev.uchar.regelspraak.transpiler.tms.Fixpoint;
import dev.uchar.regelspraak.transpiler.tms.RelationLink;

import org.kie.api.runtime.ClassObjectFilter;
import org.kie.api.runtime.KieSession;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Function;

/**
 * The end state of a session at its fixpoint (RUNTIME-SEMANTICS ch. 5.12a), read without writing anything on a bean
 * ({@link DroolsBackend#endStateOf}): every object, of the input and made by a creation, with its type and every member
 * of it that holds a value; every link; and the §9.5 facts. Where {@link Fixpoint} gives what the rules derived, this
 * gives what stands: the input of a member where nothing derived it, the derived value where something did.
 *
 * @param objects every object of the session: those of the input in the order of their labels, then the made objects in
 *                the order {@link Fixpoint#objects} gives them
 * @param links   every link, once, its objects in the order of the roles its fact type declares
 * @param channel the §9.5 facts, one per rule and subject, each with the EF codes of the kinds deposited for that pair
 */
public record EndState(List<EndObject> objects, List<Link> links, List<ChannelFact> channel) {

    public EndState {
        objects = List.copyOf(objects);
        links = List.copyOf(links);
        channel = List.copyOf(channel);
    }

    /**
     * One object of the session: its bean, its case label (null for a made object), the name of its object type, the
     * name of the creation that made it and the object it made it for (both null for an object of the input), and its
     * members that hold a value, from the root of its type chain down, each type's members in declaration order.
     */
    public record EndObject(Object object, String label, String type, String creation, Object subject,
                            List<Member> members) {
        public EndObject {
            Objects.requireNonNull(object, "object");
            Objects.requireNonNull(type, "type");
            members = List.copyOf(members);
        }
    }

    /**
     * One member of an object that holds a value. An attribute holds it in the form its bean field takes it: a number
     * as a {@code BigDecimal}, and a derived one of the unbounded form that does not terminate as the runtime's exact
     * rational ({@link Fixpoint.DerivedValue#exact}), a date, a text, a truth value, a constructor value, and a
     * timeline as the bean model's
     * {@code Segment}s; a cell of a dimensioned attribute carries its labels in {@code cell}, one per dimension in their
     * declared spelling, and every other member null there. A characteristic is listed where it holds, with
     * {@code Boolean.TRUE}, and a time-dependent one where it has true periods, with those periods as {@code Segment}s
     * without a value. {@code unit} is the unit of an attribute's declaration, null where it declares none.
     *
     * <p>{@code derived}: a rule, a default or an allocation derived the value. {@code given}: what the bean field holds
     * without the write-back of the derived values, in the same form as {@code value}: the input of an object of the
     * input, the value its creation assigned to a made object; null where the field holds none, a timeline without
     * segments included. For a characteristic it is {@code Boolean.TRUE} where the input or the creation set it, and
     * for a time-dependent one its true periods. Where nothing derived the member, {@code value} is what the field
     * holds, so a member that holds only a {@code given} and no {@code value} does not occur.
     */
    public record Member(boolean characteristic, String name, List<String> cell, Object value, Unit unit,
                         boolean derived, Object given) {
        public Member {
            Objects.requireNonNull(name, "name");
            Objects.requireNonNull(value, "value");
            cell = cell == null ? null : List.copyOf(cell);
        }
    }

    /** A link of the fact type {@code factType}: {@code first} fills its first declared role {@code firstRole} and
     *  {@code second} its second, {@code secondRole}; of a reciprocal fact type, whose one role both fill, both role
     *  names are that role's. */
    public record Link(String factType, String firstRole, Object first, String secondRole, Object second) {
        public Link {
            Objects.requireNonNull(factType, "factType");
            Objects.requireNonNull(first, "first");
            Objects.requireNonNull(second, "second");
        }
    }

    /** A §9.5 fact: the rule that deposited it, its subject, and the EF codes of the kinds deposited for that pair,
     *  ascending, each once. The backend keeps one fact per deposit; this is one per rule and subject (ch. 5.6). */
    public record ChannelFact(String rule, Object subject, List<String> codes) {
        public ChannelFact {
            codes = List.copyOf(codes);
        }
    }

    /** The reader behind {@link DroolsBackend#endStateOf}: {@code fp} is the fixpoint of {@code session}, {@code label}
     *  the case label of an input bean and null for any other, {@code classes} the bean class of every object type. */
    static EndState read(DroolsBackend.Build build, KieSession session, Fixpoint fp, Function<Object, String> label,
                         Map<Class<?>, ObjectType> classes) {
        RegelSpraakModel model = build.model();
        List<FactModelGenerator.Displacement> displacements = FactModelGenerator.displacements(model);
        // what the rules derived, per object and normalised member name: a value, or a cell per labels
        Map<Object, Map<String, Object>> derived = new IdentityHashMap<>();
        Map<Object, Map<String, Map<List<String>, Object>>> derivedCells = new IdentityHashMap<>();
        for (Fixpoint.DerivedValue v : fp.values()) {
            String key = Namen.norm(v.attribute());
            if (v.labels() != null)
                derivedCells.computeIfAbsent(v.object(), o -> new HashMap<>())
                        .computeIfAbsent(key, k -> new HashMap<>()).put(v.labels(), v.exact());
            else derived.computeIfAbsent(v.object(), o -> new HashMap<>()).put(key, v.exact());
        }
        Map<Object, Set<String>> derivedCharacteristics = new IdentityHashMap<>();
        for (Fixpoint.DerivedCharacteristic k : fp.characteristics())
            derivedCharacteristics.computeIfAbsent(k.object(), o -> new HashSet<>()).add(Namen.norm(k.characteristic()));

        Map<Object, ObjectType> typeOf = new IdentityHashMap<>();
        TreeMap<String, Object> labelled = new TreeMap<>();
        Map<Object, Fixpoint.MadeObject> made = new IdentityHashMap<>();
        for (Fixpoint.MadeObject m : fp.objects()) made.put(m.object(), m);
        for (Map.Entry<Class<?>, ObjectType> k : classes.entrySet())
            for (Object bean : session.getObjects(new ClassObjectFilter(k.getKey()))) {
                // the filter of a supertype hands out the beans of its subtypes too, which their own class counts
                if (bean.getClass() != k.getKey()) continue;
                typeOf.put(bean, k.getValue());
                String l = label.apply(bean);
                if (l != null) {
                    Object earlier = labelled.put(l, bean);
                    if (earlier != null)
                        throw new IllegalArgumentException("the label '" + l + "' names two beans of the session");
                } else if (!made.containsKey(bean))
                    throw new IllegalStateException("a bean of " + k.getValue().name + " in the session has no case label"
                            + " and no creation made it: the end state cannot name it");
            }

        List<EndObject> objects = new ArrayList<>();
        for (Map.Entry<String, Object> e : labelled.entrySet()) {
            Object bean = e.getValue();
            objects.add(new EndObject(bean, e.getKey(), typeOf.get(bean).name, null, null,
                    members(bean, typeOf.get(bean), displacements, derived, derivedCells, derivedCharacteristics)));
        }
        for (Fixpoint.MadeObject m : fp.objects()) {
            ObjectType ot = typeOf.get(m.object());
            if (ot == null)
                throw new IllegalStateException("the creation '" + m.creation() + "' made an object that is no bean of"
                        + " the session");
            objects.add(new EndObject(m.object(), null, ot.name, m.creation(), m.subject(),
                    members(m.object(), ot, displacements, derived, derivedCells, derivedCharacteristics)));
        }
        return new EndState(objects, links(model, session, typeOf), channel(session));
    }

    /** The members of {@code bean}, of type {@code ot}, that hold a value: the derived value where a rule derived one,
     *  the bean field's input elsewhere, each with what its bean field holds. A member without a bean field of its own
     *  has no value here. */
    private static List<Member> members(Object bean, ObjectType ot, List<FactModelGenerator.Displacement> displacements,
                                        Map<Object, Map<String, Object>> derived,
                                        Map<Object, Map<String, Map<List<String>, Object>>> derivedCells,
                                        Map<Object, Set<String>> derivedCharacteristics) {
        Map<String, Object> mine = derived.getOrDefault(bean, Map.of());
        Map<String, Map<List<String>, Object>> myCells = derivedCells.getOrDefault(bean, Map.of());
        Set<String> myCharacteristics = derivedCharacteristics.getOrDefault(bean, Set.of());
        List<ObjectType> chain = new ArrayList<>(ot.chain());
        java.util.Collections.reverse(chain);
        List<Member> out = new ArrayList<>();
        for (ObjectType t : chain) {
            for (Map.Entry<String, dev.uchar.regelspraak.model.Datatype> a : t.attributes.entrySet()) {
                String name = a.getKey();
                if (FactModelGenerator.displacedMember(displacements, ot, name, false) != null) continue;
                Unit unit = a.getValue() == null ? null : a.getValue().unitOf();
                String key = Namen.norm(name);
                if (!ot.dimensionsOf(name).isEmpty()) {
                    Map<List<String>, Object> given = new HashMap<>();
                    Map<List<String>, Object> cells = new LinkedHashMap<>();
                    Object field = CaseRunner.invokeGetter(bean, Namen.getter(name));
                    if (field != null)
                        for (Map.Entry<?, ?> c : ((Map<?, ?>) field).entrySet()) {
                            @SuppressWarnings("unchecked")
                            List<String> labels = (List<String>) c.getKey();
                            cells.put(labels, c.getValue());
                            given.put(labels, c.getValue());
                        }
                    Map<List<String>, Object> derivedHere = myCells.getOrDefault(key, Map.of());
                    cells.putAll(derivedHere);
                    cells.forEach((labels, v) -> {
                        if (v != null) out.add(new Member(false, name, labels, v, unit, derivedHere.containsKey(labels),
                                given.get(labels)));
                    });
                    continue;
                }
                Object field = CaseRunner.invokeGetter(bean, Namen.getter(name));
                Object v = mine.containsKey(key) ? mine.get(key) : field;
                if (v != null) out.add(new Member(false, name, null, v, unit, mine.containsKey(key), given(field)));
            }
            for (String name : t.characteristics) {
                if (FactModelGenerator.displacedMember(displacements, ot, name, true) != null) continue;
                String key = Namen.norm(name);
                if (t.characteristicGranularity.containsKey(name)) {
                    Object field = CaseRunner.invokeGetter(bean, Namen.getter(name));
                    Object v = mine.containsKey(key) ? mine.get(key) : field;
                    if (v != null && !((Collection<?>) v).isEmpty())
                        out.add(new Member(true, name, null, v, null, mine.containsKey(key), given(field)));
                    continue;
                }
                Object given = Boolean.TRUE.equals(CaseRunner.invokeGetter(bean, Namen.getter(name))) ? Boolean.TRUE : null;
                if (myCharacteristics.contains(key) || given != null)
                    out.add(new Member(true, name, null, Boolean.TRUE, null, myCharacteristics.contains(key), given));
            }
        }
        return out;
    }

    /** {@code field} as {@link Member#given} takes it: a timeline without segments holds nothing. */
    private static Object given(Object field) {
        return field instanceof Collection<?> c && c.isEmpty() ? null : field;
    }

    /** One link of a relation, compared by its fact type and its two objects by identity. */
    private record Key(String factType, Object first, Object second) {
        @Override public boolean equals(Object o) {
            return o instanceof Key k && k.factType.equals(factType) && k.first == first && k.second == second;
        }

        @Override public int hashCode() {
            return Objects.hash(factType, System.identityHashCode(first), System.identityHashCode(second));
        }
    }

    /** Every link of the session, once: those its beans hold in their reference fields, the input over a relation with
     *  a to-one role, and those it holds as {@link RelationLink} facts, the input over a relation a form creates or of
     *  two to-many roles and every link a creation laid. */
    private static List<Link> links(RegelSpraakModel model, KieSession session, Map<Object, ObjectType> typeOf) {
        Map<Key, Link> out = new LinkedHashMap<>();
        for (Map.Entry<Object, ObjectType> e : typeOf.entrySet()) {
            Object bean = e.getKey();
            ObjectType ot = e.getValue();
            for (ObjectType t : ot.chain())
                for (FactModelGenerator.Member m : FactModelGenerator.members(model, t)) {
                    FactModelGenerator.Origin origin = FactModelGenerator.origin(model, ot, m.property());
                    if (origin == null || origin.kind() != FactModelGenerator.Origin.Kind.ROL) continue;
                    if (origin.source() == null)
                        throw new IllegalStateException("the reference field '" + m.property() + "' of " + ot.name
                                + " stands for no single role");
                    Object target = CaseRunner.invokeGetter(bean, Namen.getter(origin.name()));
                    if (target == null) continue;
                    RegelSpraakModel.FactType ft = factType(model, origin.source());
                    // the target fills the role the field is named after, the bean the other one
                    boolean targetSecond = ft.roles.size() < 2 || ft.roles.get(1).name().equalsIgnoreCase(origin.name());
                    add(out, ft, targetSecond ? bean : target, targetSecond ? target : bean);
                }
        }
        for (Object f : session.getObjects(o -> o instanceof RelationLink)) {
            RelationLink k = (RelationLink) f;
            add(out, factType(model, k.relation()), k.first(), k.second());
        }
        return new ArrayList<>(out.values());
    }

    private static void add(Map<Key, Link> out, RegelSpraakModel.FactType ft, Object first, Object second) {
        String r0 = ft.roles.get(0).name(), r1 = ft.roles.size() < 2 ? r0 : ft.roles.get(1).name();
        out.putIfAbsent(new Key(ft.name, first, second), new Link(ft.name, r0, first, r1, second));
    }

    private static RegelSpraakModel.FactType factType(RegelSpraakModel model, String name) {
        for (RegelSpraakModel.FactType ft : model.factTypes.values())
            if (Namen.norm(ft.name).equals(Namen.norm(name))) return ft;
        throw new IllegalStateException("a link of the session over '" + name + "', a fact type the model does not"
                + " declare");
    }

    /** The §9.5 facts, one per rule and subject, with the codes of every deposit for that pair. */
    private static List<ChannelFact> channel(KieSession session) {
        Map<Key, TreeSet<String>> codes = new LinkedHashMap<>();
        Map<Key, ChannelFact> first = new LinkedHashMap<>();
        for (Object f : session.getObjects(o -> o.getClass().getName().equals(FactModelGenerator.PACKAGE + ".Inconsistentie"))) {
            String rule = (String) RegelSpraakRuntime.get(f, "rule");
            Object subject = RegelSpraakRuntime.get(f, "subject");
            Key k = new Key(String.valueOf(rule), subject, null);
            first.putIfAbsent(k, new ChannelFact(rule, subject, List.of()));
            TreeSet<String> mine = codes.computeIfAbsent(k, x -> new TreeSet<>());
            Object c = RegelSpraakRuntime.get(f, "codes");
            if (c != null) for (Object code : (Collection<?>) c) mine.add(String.valueOf(code));
        }
        List<ChannelFact> out = new ArrayList<>();
        first.forEach((k, f) -> out.add(new ChannelFact(f.rule(), f.subject(), new ArrayList<>(codes.get(k)))));
        out.sort(Comparator.comparing(ChannelFact::rule, Comparator.nullsFirst(Comparator.naturalOrder())));
        return out;
    }
}
