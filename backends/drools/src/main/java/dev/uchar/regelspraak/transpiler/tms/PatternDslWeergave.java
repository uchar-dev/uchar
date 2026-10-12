package dev.uchar.regelspraak.transpiler.tms;

import dev.uchar.regelspraak.ir.Geldigheid;
import dev.uchar.regelspraak.runtime.Accessors;
import dev.uchar.regelspraak.transpiler.tms.plan.Accumulatie;
import dev.uchar.regelspraak.transpiler.tms.plan.Alfa;
import dev.uchar.regelspraak.transpiler.tms.plan.Bestaat;
import dev.uchar.regelspraak.transpiler.tms.plan.Binding;
import dev.uchar.regelspraak.transpiler.tms.plan.Join;
import dev.uchar.regelspraak.transpiler.tms.plan.Lezing;
import dev.uchar.regelspraak.transpiler.tms.plan.LhsItem;
import dev.uchar.regelspraak.transpiler.tms.plan.Niet;
import dev.uchar.regelspraak.transpiler.tms.plan.Onderwerp;
import dev.uchar.regelspraak.transpiler.tms.plan.Patroon;
import dev.uchar.regelspraak.transpiler.tms.plan.Toetsitem;

import org.drools.model.DSL;
import org.drools.model.Index;
import org.drools.model.PatternDSL;
import org.drools.model.RuleItemBuilder;
import org.drools.model.Variable;
import org.drools.model.functions.Function1;
import org.drools.model.functions.Predicate1;
import org.drools.model.functions.Predicate10;
import org.drools.model.functions.Predicate11;
import org.drools.model.functions.Predicate12;
import org.drools.model.functions.Predicate13;
import org.drools.model.functions.Predicate2;
import org.drools.model.functions.Predicate3;
import org.drools.model.functions.Predicate4;
import org.drools.model.functions.Predicate5;
import org.drools.model.functions.Predicate6;
import org.drools.model.functions.Predicate7;
import org.drools.model.functions.Predicate8;
import org.drools.model.functions.Predicate9;
import org.drools.model.functions.accumulate.AccumulateFunction;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * The rendering of the planned left-hand side of one rule into Drools' executable model: the subject pattern, the
 * patterns, the accumulates and the test of the condition as {@code PatternDSL} items, with the constraint ids, the
 * names and the indexed identity joins the analysis planned. It decides nothing: what is read, how, and in which order
 * is the plan's ({@code TmsTranspiler.Lezingen}).
 *
 * <p>It renders each item when the analysis hands it over, and keeps the rendering, since until step 1c of card
 * drl-teksttak-op-truth-maintenance the forms still add constraints to the rendered subject pattern themselves, and a
 * rule and the fault rule of its condition take the same rendered items. The variables of the value array's slots are
 * the variables the bindings of the plan bind, by name; a value variable is bound once.
 */
final class PatternDslWeergave {

    private final Variable<Object> onderwerp;
    private final PatternDSL.PatternDef<Object> onderwerpPatroon;
    /** The value variables of the rule, by name, the subject's among them. */
    private final Map<String, Variable<Object>> waarden = new HashMap<>();
    /** The value variables a binding or an accumulate function binds already. */
    private final Set<String> gebonden = new HashSet<>();
    private final List<Variable<?>> slots = new ArrayList<>();
    private final List<Variable<?>> slotsLezen = Collections.unmodifiableList(slots);
    private final Map<LhsItem, RuleItemBuilder<?>> weergegeven = new IdentityHashMap<>();

    @SuppressWarnings("unchecked")
    PatternDslWeergave(Class<?> bonenklasse, String naam) {
        onderwerp = (Variable<Object>) DSL.declarationOf(bonenklasse, naam);
        onderwerpPatroon = PatternDSL.pattern(onderwerp);
        waarden.put(naam, onderwerp);
    }

    Variable<Object> onderwerp() { return onderwerp; }

    PatternDSL.PatternDef<Object> onderwerpPatroon() { return onderwerpPatroon; }

    /** The variable of each slot of the value array, null for a binder or a probe: a view that grows with the plan. */
    List<Variable<?>> slots() { return slotsLezen; }

    /** Registers slot {@code l}: the variable an item rendered before binds under its name, the subject's for the
     *  subject itself, and none for a binder or a probe, which no pattern binds. */
    void lezing(Lezing l) {
        if (l.slot() != slots.size())
            throw new IllegalStateException("slot " + l.slot() + " na " + slots.size() + " slots");
        slots.add(l.naam() == null ? null : bekend(l.naam()));
    }

    /** Registers slot {@code l}, which a pattern built outside the plan binds to {@code v}: the variable is that
     *  pattern's, so this rendering does not bind it again. */
    void extern(Lezing l, Variable<Object> v) {
        if (l.slot() != slots.size())
            throw new IllegalStateException("slot " + l.slot() + " na " + slots.size() + " slots");
        if (waarden.putIfAbsent(l.naam(), v) != null || !gebonden.add(l.naam()))
            throw new IllegalStateException("de variabele " + l.naam() + " wordt twee keer gebonden");
        slots.add(v);
    }

    /** Binds {@code b} off the subject pattern. */
    void bindOnderwerp(Binding b) {
        onderwerpPatroon.bind(bind(b.doel()), lezer(b));
    }

    /** The rendering of {@code item}, made at the first call and the same object at every later one. */
    RuleItemBuilder<?> item(LhsItem item) {
        // the subject pattern exists from the start and takes its bindings as they come (bindOnderwerp); the plan's
        // subject item is a copy of those bindings per planned left-hand side, so it renders as that one pattern
        if (item instanceof Onderwerp) return onderwerpPatroon;
        RuleItemBuilder<?> r = weergegeven.get(item);
        if (r == null) {
            r = switch (item) {
                case Onderwerp o -> onderwerpPatroon;
                case Patroon p -> patroon(p).def();
                case Niet n -> DSL.not(patroon(n.patroon()).def());
                case Bestaat b -> bestaat(b);
                case Accumulatie a -> accumulatie(a);
                case Toetsitem t -> toets(t);
            };
            weergegeven.put(item, r);
        }
        return r;
    }

    private record Weergegeven(Variable<Object> variabele, PatternDSL.PatternDef<Object> def) {}

    @SuppressWarnings("unchecked")
    private Weergegeven patroon(Patroon p) {
        Variable<Object> v = (Variable<Object>) DSL.declarationOf(p.feitklasse(), p.naam());
        PatternDSL.PatternDef<Object> def = PatternDSL.pattern(v);
        for (Alfa a : p.alfa()) def = def.expr(a.id(), predicaat(p.feitklasse(), a));
        for (Join j : p.joins()) def = zelfde(def, j.id(), bekend(j.doel()), veld(p.feitklasse(), j.veld()), o -> o);
        for (Binding b : p.bindingen()) def = def.bind(bind(b.doel()), lezer(b));
        return new Weergegeven(v, def);
    }

    private RuleItemBuilder<?> bestaat(Bestaat b) {
        PatternDSL.PatternDef<?>[] rest = new PatternDSL.PatternDef<?>[b.patronen().size() - 1];
        for (int i = 1; i < b.patronen().size(); i++) rest[i - 1] = patroon(b.patronen().get(i)).def();
        return DSL.exists(patroon(b.patronen().get(0)).def(), rest);
    }

    private RuleItemBuilder<?> accumulatie(Accumulatie a) {
        Weergegeven bron = patroon(a.bron());
        AccumulateFunction[] f = new AccumulateFunction[a.functies().size()];
        for (int i = 0; i < f.length; i++) {
            Accumulatie.Functie fn = a.functies().get(i);
            Variable<Object> over = fn.bron() == null ? bron.variabele() : bekend(fn.bron());
            f[i] = DSL.accFunction(functie(fn.soort()), over).as(bind(fn.doel()));
        }
        return DSL.accumulate(bron.def(), f[0], Arrays.copyOfRange(f, 1, f.length));
    }

    /** Drools' own accumulate function of each fold of the plan: the sum over a statically typed {@code BigDecimal},
     *  since the plain sum over an {@code Object}-bound value gives a {@code Double}. */
    private static Class<?> functie(Accumulatie.Soort soort) {
        String naam = switch (soort) {
            case AANTAL -> "CountAccumulateFunction";
            case SOM -> "BigDecimalSumAccumulateFunction";
            case MINIMUM -> "MinAccumulateFunction";
            case MAXIMUM -> "MaxAccumulateFunction";
            case LIJST -> "CollectListAccumulateFunction";
        };
        try {
            return Class.forName("org.drools.core.base.accumulators." + naam);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("de Drools-accumulatiefunctie " + naam + " ontbreekt op het classpath", e);
        }
    }

    /** The test of the condition over the values at its slots, as a standalone test after the patterns that bind them;
     *  a condition that reads nothing is a constant. The analysis refuses a condition over more than
     *  {@link TmsTranspiler#MAX_LEZINGEN} values, the most the DSL's standalone test takes. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private RuleItemBuilder<?> toets(Toetsitem toets) {
        int[] pos = toets.slots().stream().mapToInt(Integer::intValue).toArray();
        int n = toets.grootte();
        Predicate<Object[]> gecompileerd = toets.gecompileerd();
        Predicate<Object[]> t = args -> {
            Object[] w = new Object[n];
            for (int j = 0; j < pos.length; j++) w[pos[j]] = args[j];
            return gecompileerd.test(w);
        };
        String id = toets.id();
        Variable[] v = new Variable[pos.length];
        for (int j = 0; j < pos.length; j++) v[j] = slots.get(pos[j]);
        return switch (v.length) {
            case 0 -> DSL.expr(id, t.test(new Object[0]));
            case 1 -> DSL.expr(id, v[0], (Predicate1) a -> t.test(new Object[]{a}));
            case 2 -> DSL.expr(id, v[0], v[1], (Predicate2) (a, b) -> t.test(new Object[]{a, b}));
            case 3 -> DSL.expr(id, v[0], v[1], v[2], (Predicate3) (a, b, d) -> t.test(new Object[]{a, b, d}));
            case 4 -> DSL.expr(id, v[0], v[1], v[2], v[3],
                    (Predicate4) (a, b, d, e) -> t.test(new Object[]{a, b, d, e}));
            case 5 -> DSL.expr(id, v[0], v[1], v[2], v[3], v[4],
                    (Predicate5) (a, b, d, e, f) -> t.test(new Object[]{a, b, d, e, f}));
            case 6 -> DSL.expr(id, v[0], v[1], v[2], v[3], v[4], v[5],
                    (Predicate6) (a, b, d, e, f, g) -> t.test(new Object[]{a, b, d, e, f, g}));
            case 7 -> DSL.expr(id, v[0], v[1], v[2], v[3], v[4], v[5], v[6],
                    (Predicate7) (a, b, d, e, f, g, h) -> t.test(new Object[]{a, b, d, e, f, g, h}));
            case 8 -> DSL.expr(id, v[0], v[1], v[2], v[3], v[4], v[5], v[6], v[7],
                    (Predicate8) (a, b, d, e, f, g, h, i) -> t.test(new Object[]{a, b, d, e, f, g, h, i}));
            case 9 -> DSL.expr(id, v[0], v[1], v[2], v[3], v[4], v[5], v[6], v[7], v[8],
                    (Predicate9) (a, b, d, e, f, g, h, i, j) -> t.test(new Object[]{a, b, d, e, f, g, h, i, j}));
            case 10 -> DSL.expr(id, v[0], v[1], v[2], v[3], v[4], v[5], v[6], v[7], v[8], v[9],
                    (Predicate10) (a, b, d, e, f, g, h, i, j, k) -> t.test(new Object[]{a, b, d, e, f, g, h, i, j, k}));
            case 11 -> DSL.expr(id, v[0], v[1], v[2], v[3], v[4], v[5], v[6], v[7], v[8], v[9], v[10],
                    (Predicate11) (a, b, d, e, f, g, h, i, j, k, l) ->
                            t.test(new Object[]{a, b, d, e, f, g, h, i, j, k, l}));
            case 12 -> DSL.expr(id, v[0], v[1], v[2], v[3], v[4], v[5], v[6], v[7], v[8], v[9], v[10], v[11],
                    (Predicate12) (a, b, d, e, f, g, h, i, j, k, l, m) ->
                            t.test(new Object[]{a, b, d, e, f, g, h, i, j, k, l, m}));
            case 13 -> DSL.expr(id, v[0], v[1], v[2], v[3], v[4], v[5], v[6], v[7], v[8], v[9], v[10], v[11], v[12],
                    (Predicate13) (a, b, d, e, f, g, h, i, j, k, l, m, p) ->
                            t.test(new Object[]{a, b, d, e, f, g, h, i, j, k, l, m, p}));
            default -> throw new IllegalStateException("een toets over " + v.length + " waarden; de analyse weigert"
                    + " er meer dan " + TmsTranspiler.MAX_LEZINGEN);
        };
    }

    /** The value variable {@code naam}, which an item rendered before binds: a slot, a join and an accumulate name only
     *  what the plan bound already. */
    private Variable<Object> bekend(String naam) {
        Variable<Object> v = waarden.get(naam);
        if (v == null) throw new IllegalStateException("de variabele " + naam + " is nog niet gebonden");
        return v;
    }

    /** The value variable {@code naam} for the binding that binds it, made here: each is bound once. */
    private Variable<Object> bind(String naam) {
        if (!gebonden.add(naam) || waarden.containsKey(naam))
            throw new IllegalStateException("de variabele " + naam + " wordt twee keer gebonden");
        Variable<Object> v = DSL.declarationOf(Object.class, naam);
        waarden.put(naam, v);
        return v;
    }

    // ---------------- fields ----------------

    /** The read of a field of a fact of the branch, by class and name: the record accessor itself, as the analysis
     *  named it, so that the model calls it directly. A bean is read through its getter ({@link Accessors}). */
    private static final Map<Class<?>, Map<String, Function1<Object, Object>>> VELDEN = new HashMap<>();

    static {
        veld(Stand.class, "onderwerp", Stand::onderwerp);
        veld(Stand.class, "attribuut", Stand::attribuut);
        veld(Stand.class, "waarde", Stand::waarde);
        veld(KenmerkStand.class, "onderwerp", KenmerkStand::onderwerp);
        veld(KenmerkStand.class, "kenmerk", KenmerkStand::kenmerk);
        veld(KenmerkStand.class, "waar", KenmerkStand::waar);
        veld(PadStand.class, "onderwerp", PadStand::onderwerp);
        veld(PadStand.class, "pad", PadStand::pad);
        veld(PadStand.class, "lid", PadStand::lid);
        veld(PadStand.class, "waarde", PadStand::waarde);
        veld(ParameterStand.class, "parameter", ParameterStand::parameter);
        veld(ParameterStand.class, "waarde", ParameterStand::waarde);
        veld(Rekendatum.class, "datum", Rekendatum::datum);
        veld(KanaalStand.class, "regel", KanaalStand::regel);
        veld(KanaalStand.class, "inconsistent", KanaalStand::inconsistent);
        veld(Creatiefase.class, "nummer", Creatiefase::nummer);
        veld(Creatiefase.class, "stratum", Creatiefase::stratum);
        veld(Bijdrage.class, "vouw", Bijdrage::vouw);
        veld(Bijdrage.class, "onderwerp", Bijdrage::onderwerp);
        veld(Bijdrage.class, "waarde", Bijdrage::waarde);
        veld(Filterfout.class, "vouw", Filterfout::vouw);
        veld(Filterfout.class, "onderwerp", Filterfout::onderwerp);
        veld(Filterdeponering.class, "vouw", Filterdeponering::vouw);
        veld(Filterdeponering.class, "onderwerp", Filterdeponering::onderwerp);
        veld(Lidfout.class, "vouw", Lidfout::vouw);
        veld(Lidfout.class, "onderwerp", Lidfout::onderwerp);
        veld(Ontvanger.class, "verdeling", Ontvanger::verdeling);
        veld(Ontvanger.class, "bron", Ontvanger::bron);
        veld(Gemaakt.class, "object", Gemaakt::object);
        veld(Koppeling.class, "relatie", Koppeling::relatie);
        veld(Koppeling.class, "eerste", Koppeling::eerste);
        veld(Koppeling.class, "tweede", Koppeling::tweede);
    }

    @SuppressWarnings("unchecked")
    private static <T> void veld(Class<T> klasse, String naam, Function1<T, Object> lezer) {
        VELDEN.computeIfAbsent(klasse, k -> new HashMap<>()).put(naam, (Function1<Object, Object>) (Function1<?, ?>) lezer);
    }

    /** The read of field {@code naam} of an object of {@code klasse}. */
    private static Function1<Object, Object> veld(Class<?> klasse, String naam) {
        if (klasse.getPackageName().equals(PatternDslWeergave.class.getPackageName())) {
            Function1<Object, Object> f = VELDEN.getOrDefault(klasse, Map.of()).get(naam);
            if (f == null) throw new IllegalStateException("geen lezer voor het veld " + naam + " van " + klasse.getSimpleName());
            return f;
        }
        Function<Object, Object> getter = Accessors.getter(klasse, naam);
        return getter::apply;
    }

    /** The test of one alpha constraint: its field tests in order, the first that fails ends it. */
    private static Predicate1<Object> predicaat(Class<?> klasse, Alfa a) {
        List<Predicate1<Object>> delen = new ArrayList<>();
        for (Alfa.Veldtoets t : a.toetsen()) {
            Function1<Object, Object> f = veld(klasse, t.veld());
            Object w = t.waarde();
            delen.add(switch (t.op()) {
                case GELIJK -> x -> f.apply(x).equals(w);
                case MINSTENS -> x -> ((Number) f.apply(x)).intValue() >= ((Number) w).intValue();
                case BINNEN -> x -> ((Geldigheid) w).bevat((LocalDate) f.apply(x));
                case IS_EEN -> x -> ((Class<?>) w).isInstance(f.apply(x));
            });
        }
        if (delen.size() == 1) return delen.get(0);
        return x -> {
            for (Predicate1<Object> d : delen) if (!d.test(x)) return false;
            return true;
        };
    }

    /** The read a binding makes: a field of a fact as its record accessor; on a bean the walk over the reference
     *  fields of the path, null where a link is absent, and then the member as the binding reads it. */
    private static Function1<Object, Object> lezer(Binding b) {
        Class<?> klasse = b.veld().klasse();
        if (klasse.getPackageName().equals(PatternDslWeergave.class.getPackageName())) {
            if (!b.pad().isEmpty() || b.vorm() != Binding.Leesvorm.WAARDE)
                throw new IllegalStateException("een feit van de tak wordt gelezen zoals het is: " + b);
            return veld(klasse, b.veld().naam());
        }
        Function<Object, Object> lid = invoerlezer(b.veld(), b.vorm(), b.soort());
        if (b.pad().isEmpty()) return o -> lid.apply(o);
        List<Function<Object, Object>> stappen = new ArrayList<>();
        for (Binding.Veld stap : b.pad()) stappen.add(Accessors.getter(stap.klasse(), stap.naam()));
        return o -> {
            Object d = o;
            for (Function<Object, Object> stap : stappen) {
                d = stap.apply(d);
                if (d == null) return null;
            }
            return lid.apply(d);
        };
    }

    /** The read of member {@code veld} off a bean, as {@code vorm} reads it ({@link Binding.Leesvorm}). */
    static Function<Object, Object> invoerlezer(Binding.Veld veld, Binding.Leesvorm vorm, Class<?> soort) {
        Function<Object, Object> getter = Accessors.getter(veld.klasse(), veld.naam());
        return switch (vorm) {
            case WAARDE -> getter;
            case WAAR -> o -> Boolean.TRUE.equals(getter.apply(o));
            case TIJDVAKKEN -> o -> Tijdvak.lijst((List<?>) getter.apply(o));
            case GEZET -> o -> {
                Object ander = getter.apply(o);
                return ander != null && (soort == null || soort.isInstance(ander));
            };
        };
    }

    // ---------------- the identity join ----------------

    /**
     * The {@code indexId} of every beta index. In Drools 8.44 a beta index hands it to the field reader of the pattern's
     * own fact ({@code LambdaReadAccessor}), and in a beta node that number only seeds the hash of the node's tuple
     * memories ({@code TupleIndexHashTable}), with the same seed on both sides of the node, so any value is correct.
     *
     * <p>The alpha constraints carry no index, since an alpha index uses its number otherwise. The object type node
     * puts the alpha nodes of one number in one hash map, keyed by the number and the constant
     * ({@code CompositeObjectSinkAdapter}), so two alpha nodes on one constant replace each other there unless Drools
     * shares them as one node, which it does only for constraints with equal ids. Every constraint of this branch has
     * an id of its own ({@code TmsTranspiler.id}): two rules that read one attribute each have an alpha node on it, and
     * with an alpha index one of the two would never see a fact. An alpha index therefore waits for equal ids on equal
     * constraints.
     */
    private static final int BETA_INDEX = 0;

    /** What an identity join ({@link #zelfde}) hashes for a side that reads null: a key of that side alone, so that a
     *  null meets nothing, as in the predicate beside the index. */
    private static final Object NIETS_LINKS = new Object(), NIETS_RECHTS = new Object();

    /**
     * Constrains {@code p} to the facts whose {@code links} is the object that {@code rechts} reads off the object
     * {@code v} binds, by identity, as an indexed join under the constraint id {@code id}. Unindexed, Drools tests every
     * fact of the pattern's class against every tuple on the other side, so that a read of a derived value costs one
     * test per subject and a fire grows with the square of the input (the scale test of card tms-omschakeling,
     * {@code Schaalproef}). Indexed, the memories of the join hash the object, and a fact meets only the tuples of its
     * own object. Drools does not evaluate an indexed constraint but trusts the hash, which compares with {@code equals}
     * and {@code hashCode}: a bean overrides neither ({@code FactModelGenerator}, held by {@code TmsIndexWitnessTest}),
     * so equal is identical there, as {@code ==} in the predicate. One extractor per side gives both the predicate and
     * the key, so the two cannot differ; Drools evaluates the predicate where it does not use the index. A side that
     * reads null meets nothing.
     */
    static <T, U> PatternDSL.PatternDef<T> zelfde(PatternDSL.PatternDef<T> p, String id, Variable<U> v,
                                                 Function1<T, Object> links, Function1<U, Object> rechts) {
        return p.expr(id, v, (x, o) -> {
            Object a = links.apply(x);
            return a != null && a == rechts.apply(o);
        }, PatternDSL.betaIndexedBy(Object.class, Index.ConstraintType.EQUAL, BETA_INDEX,
                x -> sleutel(links.apply(x), NIETS_LINKS), o -> sleutel(rechts.apply(o), NIETS_RECHTS)));
    }

    private static Object sleutel(Object waarde, Object niets) {
        return waarde == null ? niets : waarde;
    }
}
