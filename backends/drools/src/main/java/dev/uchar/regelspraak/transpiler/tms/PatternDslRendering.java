package dev.uchar.regelspraak.transpiler.tms;

import dev.uchar.regelspraak.ir.Geldigheid;
import dev.uchar.regelspraak.runtime.Accessors;
import dev.uchar.regelspraak.transpiler.tms.plan.Accumulation;
import dev.uchar.regelspraak.transpiler.tms.plan.Alpha;
import dev.uchar.regelspraak.transpiler.tms.plan.Exists;
import dev.uchar.regelspraak.transpiler.tms.plan.BetaTest;
import dev.uchar.regelspraak.transpiler.tms.plan.Binding;
import dev.uchar.regelspraak.transpiler.tms.plan.Consequence;
import dev.uchar.regelspraak.transpiler.tms.plan.Join;
import dev.uchar.regelspraak.transpiler.tms.plan.ReadTable;
import dev.uchar.regelspraak.transpiler.tms.plan.LhsItem;
import dev.uchar.regelspraak.transpiler.tms.plan.Modelplan;
import dev.uchar.regelspraak.transpiler.tms.plan.Not;
import dev.uchar.regelspraak.transpiler.tms.plan.Subject;
import dev.uchar.regelspraak.transpiler.tms.plan.FactPattern;
import dev.uchar.regelspraak.transpiler.tms.plan.RulePlan;
import dev.uchar.regelspraak.transpiler.tms.plan.Term;
import dev.uchar.regelspraak.transpiler.tms.plan.TestItem;

import org.drools.model.DSL;
import org.drools.model.Global;
import org.drools.model.Index;
import org.drools.model.Model;
import org.drools.model.PatternDSL;
import org.drools.model.Rule;
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
import org.drools.model.impl.ModelImpl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * The rendering of the outcome of the analysis into Drools' executable model ({@link #model}): the globals, and per
 * planned rule the subject pattern, the patterns, the accumulates and the tests as {@code PatternDSL} items, with the
 * constraint ids, the names and the indexed identity joins the analysis planned, and the consequence the plan's
 * {@link Consequence} says. It decides nothing: which globals there are, what is read, how, in which order, and what a
 * firing inserts is the plan's ({@code TmsTranspiler}, {@link Modelplan}, {@link RulePlan}).
 *
 * <p>It renders a whole plan at once ({@link #regel}). The variables of the value array's slots are the variables the
 * bindings of the plan bind, by name; a value variable is bound once.
 */
final class PatternDslRendering {

    /** The slots of the rule's value array ({@link #regel}). */
    private final ReadTable table;
    /** The value variables of the rule, by name, the subject's among them. */
    private final Map<String, Variable<Object>> waarden = new HashMap<>();
    /** The value variables a binding or an accumulate function binds already and that are still in scope. */
    private final Set<String> bound = new HashSet<>();

    /** The rendering of a whole plan, whose subject pattern, if the rule has one, is an item of its left-hand side. */
    private PatternDslRendering(ReadTable table) {
        this.table = table;
    }

    /**
     * The executable model of {@code plan}: its globals declared, each once, and its rules, in the order of the plan
     * ({@link #regel}). This is the one place the model is made.
     */
    static Model model(Modelplan plan) {
        ModelImpl m = new ModelImpl(TmsTranspiler.PKG);
        Map<String, Global<?>> globals = new HashMap<>();
        for (Modelplan.Global g : plan.globals()) {
            Global<?> global = DSL.globalOf(g.javaClass(), TmsTranspiler.PKG, g.naam());
            if (globals.putIfAbsent(g.naam(), global) != null)
                throw new IllegalStateException("de global " + g.naam() + " wordt twee keer gedeclareerd");
            m.addGlobal(global);
        }
        for (RulePlan r : plan.rules())
            m.addRule(regel(r, naam -> {
                Global<?> g = globals.get(naam);
                if (g == null) throw new IllegalStateException("geen global " + naam + " in het model");
                return g;
            }));
        return m;
    }

    /**
     * The rule {@code plan} plans: its left-hand side item by item, in order, and its consequence, under its name and
     * in its agenda group. {@code globals} gives the global of a name, which a consequence that reads one binds: the
     * model declares each global once, so the rule takes that declaration.
     */
    @SuppressWarnings("rawtypes")
    static Rule regel(RulePlan plan, Function<String, Global<?>> globals) {
        PatternDslRendering w = new PatternDslRendering(plan.lezingen());
        List<RuleItemBuilder<?>> items = new ArrayList<>();
        for (LhsItem item : plan.leftSide()) items.add(w.weergave(item));
        items.add(w.consequence(plan.consequence(), globals));
        org.drools.model.impl.RuleBuilder b = PatternDSL.rule(TmsTranspiler.PKG, plan.naam());
        if (plan.agendaGroup() != null) b = b.attribute(Rule.Attribute.AGENDA_GROUP, plan.agendaGroup());
        return b.build(items.toArray(new RuleItemBuilder[0]));
    }

    /** The rendering of {@code item} of a whole plan. */
    private RuleItemBuilder<?> weergave(LhsItem item) {
        return switch (item) {
            case Subject o -> onderwerp(o);
            case FactPattern p -> {
                // a consequence may name the object a pattern binds, as the fault of a session without a date does
                Rendered w = pattern(p);
                if (waarden.putIfAbsent(p.naam(), w.variable()) != null)
                    throw new IllegalStateException("de variabele " + p.naam() + " wordt twee keer gebonden");
                yield w.def();
            }
            case Not n -> not(n);
            case Exists b -> exists(b);
            case Accumulation a -> accumulation(a);
            case TestItem t -> test(t);
        };
    }

    /** The subject pattern of a whole plan: its variable, its constraints, its joins, its constraints between the bean
     *  and another pattern's object, then its bindings. */
    @SuppressWarnings("unchecked")
    private PatternDSL.PatternDef<Object> onderwerp(Subject o) {
        Variable<Object> v = (Variable<Object>) DSL.declarationOf(o.beanClass(), o.naam());
        if (waarden.putIfAbsent(o.naam(), v) != null)
            throw new IllegalStateException("de variabele " + o.naam() + " wordt twee keer gebonden");
        PatternDSL.PatternDef<Object> def = PatternDSL.pattern(v);
        for (Alpha a : o.alpha()) def = def.expr(a.id(), predicate(o.beanClass(), a));
        for (Join j : o.joins()) def = join(def, o.beanClass(), j);
        for (BetaTest t : o.betaTests()) def = betaTest(def, t);
        for (Binding b : o.bindings()) def = def.bind(bind(b.doel()), reader(b));
        return def;
    }

    /** The variable of slot {@code slot} of the value array, null for a binder or a probe. */
    private Variable<?> variable(int slot) {
        String naam = table.slots().get(slot).naam();
        return naam == null ? null : known(naam);
    }

    // ---------------- the consequence ----------------

    /** The consequence {@code g} plans. */
    @SuppressWarnings("rawtypes")
    private RuleItemBuilder<?> consequence(Consequence g, Function<String, Global<?>> globals) {
        return switch (g) {
            case Consequence.Value x -> waarde(x);
            case Consequence.Characteristic x -> new VariadicConsequence(new Variable[]{known(x.onderwerp())}, (drools, a) ->
                    voegIn(drools, x.insertion(), new HasCharacteristic(a[0], x.kenmerk())));
            case Consequence.TestFault x -> testFault(x);
            case Consequence.Fout x -> new VariadicConsequence(new Variable[]{known(x.onderwerp())}, (drools, a) ->
                    voegIn(drools, x.insertion(), new Fout(a[0], x.fout().vorm(),
                            ComputeHelpers.computeFault(x.sentence(), x.fout().regel()))));
            case Consequence.Inconsistency x -> new VariadicConsequence(new Variable[]{known(x.onderwerp())}, (drools, a) ->
                    voegIn(drools, x.insertion(), ComputeHelpers.inconsistency(x.vorm(), a[0])));
            case Consequence.Schrijversconflict x ->
                    new VariadicConsequence(new Variable[]{known(x.onderwerp()), variable(x.waarden())}, (drools, a) ->
                            voegIn(drools, x.insertion(), ComputeHelpers.writersConflict(a[0], a[1], x.attribuut(),
                                    x.documentOrder(), x.typeClasses(), x.typen())));
            case Consequence.InsertParameterState x -> new VariadicConsequence(new Variable[]{globals.apply(x.global())}, (drools, a) ->
                    voegIn(drools, x.insertion(), new ParameterState(x.parameter(), a[0])));
            case Consequence.EvaluationDateState x -> new VariadicConsequence(new Variable[]{globals.apply(x.global())}, (drools, a) ->
                    voegIn(drools, x.insertion(), new EvaluationDate((LocalDate) a[0])));
            case Consequence.InsertCharacteristicState x -> new VariadicConsequence(new Variable[]{known(x.onderwerp())}, (drools, a) ->
                    voegIn(drools, x.insertion(), new CharacteristicState(a[0], x.kenmerk(), x.holds())));
            case Consequence.State x -> new VariadicConsequence(new Variable[]{known(x.onderwerp()), variable(x.waarde())},
                    (drools, a) -> voegIn(drools, x.insertion(), new State(a[0], x.attribuut(),
                            x.emmerscalair() ? ComputeHelpers.bucketScalarOf(a[1]) : a[1])));
            case Consequence.InsertPathState x -> x.bron() == Consequence.StateSource.MISSING_LINK
                    ? new VariadicConsequence(new Variable[]{known(x.onderwerp())}, (drools, a) ->
                            voegIn(drools, x.insertion(), new PathState(a[0], x.pad(), x.member(), null)))
                    : new VariadicConsequence(new Variable[]{known(x.onderwerp()), variable(x.waarde())}, (drools, a) ->
                            voegIn(drools, x.insertion(), new PathState(a[0], x.pad(), x.member(), a[1])));
            case Consequence.UniquenessKey x -> uniquenessKey(x);
            case Consequence.UniquenessViolation x -> new VariadicConsequence(new Variable[]{known(x.onderwerp())}, (drools, a) ->
                    voegIn(drools, x.insertion(), new UniquenessViolation(x.uniqueness(), a[0])));
            case Consequence.Invoervormfout x -> new VariadicConsequence(new Variable[]{known(x.onderwerp())}, (drools, a) ->
                    voegIn(drools, x.insertion(), new Fout(a[0], x.fout().vorm(),
                            ComputeHelpers.inputFormFault(x.before(), a[0], x.after(), x.fout().regel()))));
            case Consequence.InsertChannelState x -> new VariadicConsequence(new Variable[0], (drools, a) ->
                    voegIn(drools, x.insertion(), new ChannelState(x.producer(), x.inconsistent())));
            case Consequence.Recipient x -> consequentie(x.onderwerp(), slotsOf(x.recipient(), x.weight(), x.sleutel(),
                    x.maximum()), (drools, bron, w) -> voegIn(drools, x.insertion(), new Recipient(x.allocation(), bron,
                    w[x.recipient()], lees(w, x.weight()), lees(w, x.sleutel()), lees(w, x.maximum()))));
            case Consequence.Contribution x -> contribution(x);
            case Consequence.FilterDeposit x -> consequentie(x.onderwerp(), slotsOf(x.member()), (drools, carrier, w) ->
                    voegIn(drools, x.insertion(), new FilterDeposit(x.fold(), carrier, w[x.member()])));
            case Consequence.FilterFault x -> filterFault(x);
            case Consequence.MemberFault x -> consequentie(x.onderwerp(), slotsOf(placeSlots(x.member(), x.place())),
                    (drools, carrier, w) -> {
                        List<Integer> place = foldPlace(x.place(), x.fold(), w[x.member()], w);
                        voegIn(drools, x.insertion(), new MemberFault(x.fold(), carrier, w[x.member()],
                                ComputeHelpers.inputFormFault(x.before(), w[x.member()], x.after(), null), place));
                    });
            case Consequence.Creation x -> creatie(x);
            case Consequence.RelationLink x -> x.phase() < 0
                    ? new VariadicConsequence(new Variable[]{known(x.eerste()), known(x.tweede())}, (drools, a) ->
                            voegIn(drools, x.insertion(), new RelationLink(x.relation(), a[0], a[1], 0)))
                    : new VariadicConsequence(new Variable[]{known(x.eerste()), known(x.tweede()), variable(x.phase())},
                            (drools, a) -> voegIn(drools, x.insertion(),
                                    new RelationLink(x.relation(), a[0], a[1], (Integer) a[2])));
            case Consequence.SecondFilling x -> new VariadicConsequence(new Variable[]{known(x.onderwerp())}, (drools, a) ->
                    voegIn(drools, x.insertion(), new Fout(a[0], x.relation(), new IllegalStateException(x.sentence()))));
            case Consequence.Decision x -> decisionOf(x);
            case Consequence.Conflict x -> {
                ValueFunction sleutel = ClosureRendering.valueFunction(x.sleutel());
                yield consequentie(x.onderwerp(), x.slots(), x.size(), (drools, onderwerp, w) ->
                        voegIn(drools, x.insertion(), stackFact(x.fact(), onderwerp, sleutel.van(w))));
            }
            case Consequence.Payout x -> consequentie(x.onderwerp(), slotsOf(x.pot(), x.lijst()), (drools, bron, w) -> {
                for (Object fact : ComputeHelpers.payout(x.vorm(), bron, x.methode(), w[x.pot()], w[x.lijst()],
                        x.gewogen(), x.capped(), x.descending(), x.decimals(), x.aandeel(), x.rest(), x.writer()))
                    voegIn(drools, x.insertion(), fact);
            });
        };
    }

    /** The keys of {@link Consequence.UniquenessKey}: the components computed from the bound values, the parent bound,
     *  and the route that makes one key, or one per value a component on a timeline ever carries. */
    private RuleItemBuilder<?> uniquenessKey(Consequence.UniquenessKey g) {
        List<ValueFunction> components = new ArrayList<>();
        for (Term t : g.components())
            components.add(ClosureRendering.valueFunction(t));
        return consequentie(g.onderwerp(), g.slots(), g.size(), (drools, element, w) -> {
            List<Object> waarden = new ArrayList<>(components.size());
            for (ValueFunction c : components) waarden.add(c.van(w));
            Object parent = g.scope() < 0 ? null : w[g.scope()];
            for (UniquenessKey k : ComputeHelpers.uniquenessKeys(g.uniqueness(), g.bron(), element, parent, waarden,
                    g.timeline()))
                voegIn(drools, g.insertion(), k);
        });
    }

    /** The contribution of {@link Consequence.Contribution}: the member itself, its value through the route, or what the matrix
     *  of a quantifier fold says of it, its place first and the probe in its slot where the plan says so. */
    private RuleItemBuilder<?> contribution(Consequence.Contribution g) {
        return switch (g.input()) {
            case Consequence.Input.Count t -> consequentie(g.onderwerp(), g.slots(), g.size(), (drools, carrier, w) ->
                    voegIn(drools, g.insertion(), new Contribution(g.fold(), carrier, w[g.member()], null)));
            case Consequence.Input.MemberValue l -> consequentie(g.onderwerp(), g.slots(), g.size(), (drools, carrier, w) -> {
                Contribution b = ComputeHelpers.contribution(g.fold(), carrier, w[g.member()], w[l.waarde()], l.totaal(), l.filled());
                if (b != null) voegIn(drools, g.insertion(), b);
            });
            case Consequence.Input.Verdict o -> {
                Predicate<Object[]> matrix = ClosureRendering.test(o.matrix());
                yield consequentie(g.onderwerp(), g.slots(), g.size(), (drools, carrier, w) ->
                        voegIn(drools, g.insertion(), new Contribution(g.fold(), carrier, w[g.member()],
                                ComputeHelpers.verdict(matrix.test(w)))));
            }
            case Consequence.Input.MemberTest t -> {
                Predicate<Object[]> matrix = ClosureRendering.test(t.matrix());
                yield consequentie(g.onderwerp(), g.slots(), g.size(), (drools, carrier, w) -> {
                    List<Integer> place = foldPlace(t.place(), g.fold(), w[g.member()], w);
                    Object s = t.depositFlag() < 0 ? null : ComputeHelpers.depositFlag();
                    if (s != null) w[t.depositFlag()] = s;
                    Boolean holds = null;
                    RuntimeException fout = null;
                    try {
                        holds = matrix.test(w);
                    } catch (RuntimeException e) {
                        if (!ComputeHelpers.isEvaluationFault(e)) throw e;
                        fout = e;
                    }
                    voegIn(drools, g.insertion(), new Contribution(g.fold(), carrier, w[g.member()],
                            new MemberTest(place, holds, fout, ComputeHelpers.reported(s))));
                });
            }
        };
    }

    /** The fault of {@link Consequence.FilterFault}: the filter evaluated again over the same bound values, then the member's
     *  place. */
    private RuleItemBuilder<?> filterFault(Consequence.FilterFault g) {
        Predicate<Object[]> filter = ClosureRendering.test(g.filter());
        return consequentie(g.onderwerp(), g.slots(), g.size(), (drools, carrier, w) -> {
            RuntimeException fout = ClosureRendering.fout(filter, w);
            if (fout == null)
                throw new IllegalStateException("het filter van " + g.fold() + " faalde in de toets en niet in het gevolg,"
                        + " over dezelfde gebonden waarden");
            List<Integer> place = foldPlace(g.place(), g.fold(), w[g.member()], w);
            voegIn(drools, g.insertion(), new FilterFault(g.fold(), carrier, w[g.member()], fout, place));
        });
    }

    /** The member's place {@code p} in the order of the fold {@code fold}, from the bound values {@code w}, through the
     *  route. */
    private static List<Integer> foldPlace(Consequence.FoldPlace p, String fold, Object member, Object[] w) {
        List<String> fields = new ArrayList<>();
        List<Object> counts = new ArrayList<>(), places = new ArrayList<>();
        for (Consequence.FoldPlace.Step s : p.steps()) {
            fields.add(s.veld());
            counts.add(w[s.aantal()]);
            places.add(w[s.place()]);
        }
        return ComputeHelpers.foldPlace(p.reden(), fold, member, p.selection(), fields, counts, places);
    }

    /** The slot {@code lid} and the slots of the place {@code p}. */
    private static int[] placeSlots(int member, Consequence.FoldPlace p) {
        int[] out = new int[1 + 2 * p.steps().size()];
        out[0] = member;
        for (int i = 0; i < p.steps().size(); i++) {
            out[1 + 2 * i] = p.steps().get(i).aantal();
            out[2 + 2 * i] = p.steps().get(i).place();
        }
        return out;
    }

    /** The slots a consequence reads, each once and in ascending order, without -1, which stands for none. */
    private static List<Integer> slotsOf(int... slots) {
        return Arrays.stream(slots).filter(i -> i >= 0).distinct().sorted().boxed().toList();
    }

    /** The value in slot {@code slot} of {@code w}, null for the slot -1. */
    private static Object lees(Object[] w, int slot) {
        return slot < 0 ? null : w[slot];
    }

    /** A consequence over the object {@code onderwerp} binds and the values at {@code slots}, in a value array that
     *  reaches the last of them. */
    private RuleItemBuilder<?> consequentie(String onderwerp, List<Integer> slots, ConsequenceFunction f) {
        return consequentie(onderwerp, slots, slots.isEmpty() ? 0 : slots.get(slots.size() - 1) + 1, f);
    }

    /** The value of {@link Consequence.Value}: computed from the bound values, with its fault, its deposit and its leeg
     *  policy as the plan says. */
    private RuleItemBuilder<?> waarde(Consequence.Value g) {
        ValueFunction waarde = ClosureRendering.valueFunction(g.waarde());
        Consequence.Deposit deposit = g.deposit();
        return consequentie(g.onderwerp(), g.slots(), g.size(), (drools, onderwerp, w) -> {
            // the probe hears every §6.5 and §6.6 place the evaluation reaches, and only those (Sonde)
            Object s = deposit == null ? null : ComputeHelpers.depositFlag();
            if (s != null) w[deposit.depositFlag()] = s;
            Object v;
            try {
                v = waarde.van(w);
                if (g.collapse()) v = ComputeHelpers.collapse(v);
            } catch (RuntimeException e) {
                if (!ComputeHelpers.isEvaluationFault(e)) throw e;
                voegIn(drools, g.insertion(),
                        new Fout(onderwerp, g.fout().vorm(), ComputeHelpers.designate(e, g.fout().regel())));
                return;
            }
            if (ComputeHelpers.reported(s))
                voegIn(drools, g.insertion(), ComputeHelpers.inconsistency(deposit.vorm(), onderwerp));
            if (!ComputeHelpers.isEmpty(v) || g.writesEmpty())
                voegIn(drools, g.insertion(), new Value(onderwerp, g.doel(), v, g.writer()));
        });
    }

    /**
     * The object creation of {@link Consequence.Creation}, in the order of its steps: the depth through its route, before
     * anything is made; the values of the initialisations, the first evaluation fault being all the creation does; the
     * construction of the new bean, with the attribute initialisations whose value is gevuld set on it before it enters
     * working memory; then the insertions, in order.
     */
    private RuleItemBuilder<?> creatie(Consequence.Creation g) {
        List<ValueFunction> waarden = new ArrayList<>();
        for (Consequence.Initialisation i : g.initialisations()) waarden.add(ClosureRendering.valueFunction(i.waarde()));
        Consequence.Construction construction = g.construction();
        List<BiConsumer<Object, Object>> setters = new ArrayList<>();
        for (Consequence.Construction.Setting z : construction.settings())
            setters.add(Accessors.eigenschapSetter(z.veld().javaClass(), z.veld().property()));
        Consequence.CreationDepth d = g.depth();
        return consequentie(g.onderwerp(), g.slots(), g.size(), (drools, onderwerp, w) -> {
            int depth = ComputeHelpers.creationDepth(g.creatie(), d.stratum(), d.type(), (List<?>) w[d.origin()],
                    d.budget());
            Object[] v = new Object[waarden.size()];
            for (int i = 0; i < v.length; i++) {
                try {
                    v[i] = ComputeHelpers.collapse(waarden.get(i).van(w));
                } catch (RuntimeException e) {
                    if (!ComputeHelpers.isEvaluationFault(e)) throw e;
                    voegIn(drools, g.faultInsertion(),
                            new Fout(onderwerp, g.fout().vorm(), ComputeHelpers.designate(e, g.fout().regel())));
                    return;
                }
            }
            Object fresh = newObject(construction.javaClass());
            for (int j = 0; j < setters.size(); j++) {
                Object waarde = v[construction.settings().get(j).initialisatie()];
                if (waarde != null) setters.get(j).accept(fresh, waarde);
            }
            int phase = (Integer) w[g.phase()];
            for (Consequence.Insert i : g.insertions())
                switch (i) {
                    case Consequence.Insert.New n -> voegIn(drools, n.insertion(), fresh);
                    case Consequence.Insert.Mark m -> voegIn(drools, m.insertion(),
                            new CreationStamp(g.creatie(), onderwerp, fresh, phase, d.stratum(), depth));
                    case Consequence.Insert.Link l -> voegIn(drools, l.insertion(), l.newIsFirst()
                            ? new RelationLink(l.relation(), fresh, onderwerp, phase)
                            : new RelationLink(l.relation(), onderwerp, fresh, phase));
                    case Consequence.Insert.Characteristic k -> {
                        if (Boolean.TRUE.equals(v[k.initialisatie()]))
                            voegIn(drools, k.insertion(),
                                    new HasCharacteristic(fresh, g.initialisations().get(k.initialisatie()).member()));
                    }
                }
        });
    }

    /** The decision of {@link Consequence.Decision}: its value from the bound values, with its fault where the plan names
     *  one; nothing where the value is null; else the facts of the decision, in order. */
    private RuleItemBuilder<?> decisionOf(Consequence.Decision g) {
        ValueFunction waarde = ClosureRendering.valueFunction(g.waarde());
        return consequentie(g.onderwerp(), g.slots(), g.size(), (drools, onderwerp, w) -> {
            Object v;
            try {
                v = waarde.van(w);
                if (g.collapse()) v = ComputeHelpers.collapse(v);
            } catch (RuntimeException e) {
                if (g.fout() == null || !ComputeHelpers.isEvaluationFault(e)) throw e;
                voegIn(drools, g.insertion(),
                        new Fout(onderwerp, g.fout().vorm(), ComputeHelpers.designate(e, g.fout().regel())));
                return;
            }
            if (v == null) return;
            for (Consequence.StackFact f : g.facts()) voegIn(drools, g.insertion(), stackFact(f, onderwerp, v));
        });
    }

    /** The fact {@code f} of a defaults stack on {@code onderwerp}, with {@code v}: the decision, or the key of a
     *  conflict. */
    private static Object stackFact(Consequence.StackFact f, Object onderwerp, Object v) {
        return switch (f) {
            case Consequence.StackFact.Value x -> new Value(onderwerp, x.doel(), v, x.writer());
            case Consequence.StackFact.Characteristic x -> new HasCharacteristic(onderwerp, x.kenmerk());
            case Consequence.StackFact.Decides x -> new Decides(onderwerp, x.stack(), x.index());
            case Consequence.StackFact.Outcome x -> x.soort() == Outcome.Kind.DECIDED
                    ? new Outcome(onderwerp, x.stack(), x.chain(), x.positie(), Outcome.Kind.DECIDED, v, null)
                    : new Outcome(onderwerp, x.stack(), x.chain(), x.positie(), Outcome.Kind.CONFLICT, null,
                            (String) v);
            case Consequence.StackFact.Inconsistency x -> ComputeHelpers.inconsistency((String) v, onderwerp);
        };
    }

    /** A new bean of {@code klasse} by its constructor without arguments, every field unset. */
    private static Object newObject(Class<?> javaClass) {
        try {
            return javaClass.getConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("kon geen " + javaClass.getSimpleName() + " maken", e);
        }
    }

    /** The fault of {@link Consequence.TestFault}: the condition evaluated again over the same bound values. */
    private RuleItemBuilder<?> testFault(Consequence.TestFault g) {
        Predicate<Object[]> test = ClosureRendering.test(g.test());
        return consequentie(g.onderwerp(), g.slots(), g.size(), (drools, onderwerp, w) -> {
            RuntimeException f = ClosureRendering.fout(test, w);
            if (f == null)
                throw new IllegalStateException("de voorwaarde van \"" + g.fout().vorm() + "\" faalde in de toets en niet"
                        + " in het gevolg, over dezelfde gebonden waarden");
            voegIn(drools, g.insertion(), new Fout(onderwerp, g.fout().vorm(), ComputeHelpers.designate(f, g.fout().regel())));
        });
    }

    /** The body of a consequence: the Drools handle, the object, and the bound values at their slots. */
    @FunctionalInterface
    private interface ConsequenceFunction {
        void run(org.drools.model.Drools drools, Object onderwerp, Object[] waarden);
    }

    /** A consequence over the object {@code onderwerp} binds and the values at {@code slots}, in a value array of
     *  {@code grootte} slots. */
    @SuppressWarnings("rawtypes")
    private RuleItemBuilder<?> consequentie(String onderwerp, List<Integer> slots, int size, ConsequenceFunction f) {
        int[] pos = slots.stream().mapToInt(Integer::intValue).toArray();
        Variable[] v = new Variable[pos.length + 1];
        v[0] = known(onderwerp);
        for (int j = 0; j < pos.length; j++) v[j + 1] = variable(pos[j]);
        return new VariadicConsequence(v, (drools, args) -> {
            Object[] w = new Object[size];
            for (int j = 0; j < pos.length; j++) w[pos[j]] = args[j + 1];
            f.run(drools, args[0], w);
        });
    }

    private static void voegIn(org.drools.model.Drools drools, Consequence.Insertion insertion, Object fact) {
        if (insertion == Consequence.Insertion.LOGICAL) drools.logicalInsert(fact);
        else drools.insert(fact);
    }

    /** A consequence over any number of bound variables. Each typed {@code execute} of the DSL sets its builder's block
     *  to a {@link org.drools.model.functions.BlockN} over the Drools handle and the bound facts, and the engine calls
     *  that block whatever its arity; this builder sets the same block directly, so a consequence is not limited by the
     *  arities the DSL spells out. */
    private static final class VariadicConsequence
            extends org.drools.model.consequences.ConsequenceBuilder.AbstractValidBuilder<VariadicConsequence> {
        @SuppressWarnings("rawtypes")
        VariadicConsequence(Variable[] v, BiConsumer<org.drools.model.Drools, Object[]> body) {
            super(v);
            usingDrools = true;
            block = args -> body.accept((org.drools.model.Drools) args[0],
                    java.util.Arrays.copyOfRange(args, 1, args.length));
        }
    }

    private record Rendered(Variable<Object> variable, PatternDSL.PatternDef<Object> def) {}

    @SuppressWarnings("unchecked")
    private Rendered pattern(FactPattern p) {
        Variable<Object> v = (Variable<Object>) DSL.declarationOf(p.factClass(), p.naam());
        PatternDSL.PatternDef<Object> def = PatternDSL.pattern(v);
        for (Alpha a : p.alpha()) def = def.expr(a.id(), predicate(p.factClass(), a));
        for (Join j : p.joins()) def = join(def, p.factClass(), j);
        for (BetaTest t : p.betaTests()) def = betaTest(def, t);
        for (Alpha a : p.afterAlpha()) def = def.expr(a.id(), predicate(p.factClass(), a));
        for (Binding b : p.bindings()) def = def.bind(bind(b.doel()), reader(b));
        return new Rendered(v, def);
    }

    /** The identity join {@code j} on a pattern over {@code klasse}: the field it names of the pattern's object, or the
     *  object itself, against the object its variable binds, or the reference field it names of that object. */
    private PatternDSL.PatternDef<Object> join(PatternDSL.PatternDef<Object> def, Class<?> javaClass, Join j) {
        Function1<Object, Object> left = j.veld() == null ? x -> x : veld(javaClass, j.property());
        Function1<Object, Object> right = j.targetField() == null ? o -> o
                : veld(j.targetField().javaClass(), j.targetField().property());
        return same(def, j.id(), known(j.doel()), left, right);
    }

    /** The constraint {@code t} between the object of {@code def} and the object another pattern binds, unindexed. */
    private PatternDSL.PatternDef<Object> betaTest(PatternDSL.PatternDef<Object> def, BetaTest t) {
        return def.expr(t.id(), known(t.doel()), ClosureRendering.betaTest(t.function(), t.constants()));
    }

    /** The existence of {@code b}, in the scope its plan states ({@link #quantified}). */
    private RuleItemBuilder<?> exists(Exists b) {
        PatternDSL.PatternDef<?>[] defs = quantified(b.patterns(), b.inside(), b.outside());
        return DSL.exists(defs[0], Arrays.copyOfRange(defs, 1, defs.length));
    }

    /** The absence of {@code n}, in the scope its plan states ({@link #quantified}). */
    private RuleItemBuilder<?> not(Not n) {
        PatternDSL.PatternDef<?>[] defs = quantified(n.patterns(), n.inside(), n.outside());
        return DSL.not(defs[0], Arrays.copyOfRange(defs, 1, defs.length));
    }

    /** The patterns of an existence or a negation, rendered in the scope its plan states: what it reads from before
     *  ({@code buiten}) is bound, and what it declares ({@code binnen}), the variables of its patterns and those their
     *  bindings bind, is known to the patterns after them inside it and to nothing after it. */
    private PatternDSL.PatternDef<?>[] quantified(List<FactPattern> patterns, List<String> inside,
                                                       List<String> outside) {
        for (String naam : outside) known(naam);
        PatternDSL.PatternDef<?>[] defs = new PatternDSL.PatternDef<?>[patterns.size()];
        for (int i = 0; i < defs.length; i++) {
            FactPattern p = patterns.get(i);
            Rendered w = pattern(p);
            defs[i] = w.def();
            if (waarden.putIfAbsent(p.naam(), w.variable()) != null)
                throw new IllegalStateException("de variabele " + p.naam() + " wordt twee keer gebonden");
        }
        // a value bound inside lives only inside: a later quantification may bind its name again, as its plan allows
        for (String naam : inside) {
            waarden.remove(naam);
            bound.remove(naam);
        }
        return defs;
    }

    private RuleItemBuilder<?> accumulation(Accumulation a) {
        Rendered bron = pattern(a.bron());
        AccumulateFunction[] f = new AccumulateFunction[a.aggregations().size()];
        for (int i = 0; i < f.length; i++) {
            Accumulation.Aggregation fn = a.aggregations().get(i);
            Variable<Object> over = fn.bron() == null ? bron.variable() : known(fn.bron());
            f[i] = DSL.accFunction(function(fn.soort()), over).as(bind(fn.doel()));
        }
        return DSL.accumulate(bron.def(), f[0], Arrays.copyOfRange(f, 1, f.length));
    }

    /** Drools' own accumulate function of each fold of the plan: the sum over a statically typed {@code BigDecimal},
     *  since the plain sum over an {@code Object}-bound value gives a {@code Double}. */
    private static Class<?> function(Accumulation.Kind soort) {
        String naam = switch (soort) {
            case COUNT -> "CountAccumulateFunction";
            case SUM -> "BigDecimalSumAccumulateFunction";
            case MINIMUM -> "MinAccumulateFunction";
            case MAXIMUM -> "MaxAccumulateFunction";
            case LIST -> "CollectListAccumulateFunction";
        };
        try {
            return Class.forName("org.drools.core.base.accumulators." + naam);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("de Drools-accumulatiefunctie " + naam + " ontbreekt op het classpath", e);
        }
    }

    /** The test of the condition over the values at its slots, as a standalone test after the patterns that bind them,
     *  asking of the condition what the plan's role says ({@link ClosureRendering#test(Toetsitem)});
     *  a condition that reads nothing is a constant. The analysis refuses a condition over more than
     *  {@link TmsTranspiler#MAX_READS} values, the most the DSL's standalone test takes. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private RuleItemBuilder<?> test(TestItem test) {
        int[] pos = test.slots().stream().mapToInt(Integer::intValue).toArray();
        int n = test.size();
        Predicate<Object[]> compiled = ClosureRendering.test(test);
        Predicate<Object[]> t = args -> {
            Object[] w = new Object[n];
            for (int j = 0; j < pos.length; j++) w[pos[j]] = args[j];
            return compiled.test(w);
        };
        String id = test.id();
        Variable[] v = new Variable[pos.length];
        for (int j = 0; j < pos.length; j++) v[j] = variable(pos[j]);
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
                    + " er meer dan " + TmsTranspiler.MAX_READS);
        };
    }

    /** The value variable {@code naam}, which an item rendered before binds: a slot, a join and an accumulate name only
     *  what the plan bound already. */
    private Variable<Object> known(String naam) {
        Variable<Object> v = waarden.get(naam);
        if (v == null) throw new IllegalStateException("de variabele " + naam + " is nog niet gebonden");
        return v;
    }

    /** The value variable {@code naam} for the binding that binds it, made here: each is bound once. */
    private Variable<Object> bind(String naam) {
        if (!bound.add(naam) || waarden.containsKey(naam))
            throw new IllegalStateException("de variabele " + naam + " wordt twee keer gebonden");
        Variable<Object> v = DSL.declarationOf(Object.class, naam);
        waarden.put(naam, v);
        return v;
    }

    // ---------------- fields ----------------

    /** The read of a field of a fact of the branch, by class and name: the record accessor itself, as the analysis
     *  named it, so that the model calls it directly. A bean is read through its getter ({@link Accessors}). */
    private static final Map<Class<?>, Map<String, Function1<Object, Object>>> FIELDS = new HashMap<>();

    static {
        veld(Value.class, "onderwerp", Value::onderwerp);
        veld(Value.class, "attribuut", Value::attribuut);
        veld(Value.class, "writer", Value::writer);
        veld(Value.class, "waarde", Value::waarde);
        veld(State.class, "onderwerp", State::onderwerp);
        veld(State.class, "attribuut", State::attribuut);
        veld(State.class, "waarde", State::waarde);
        veld(CharacteristicState.class, "onderwerp", CharacteristicState::onderwerp);
        veld(CharacteristicState.class, "kenmerk", CharacteristicState::kenmerk);
        veld(CharacteristicState.class, "holds", CharacteristicState::holds);
        veld(HasCharacteristic.class, "onderwerp", HasCharacteristic::onderwerp);
        veld(HasCharacteristic.class, "kenmerk", HasCharacteristic::kenmerk);
        veld(BucketInput.class, "onderwerp", BucketInput::onderwerp);
        veld(BucketInput.class, "attribuut", BucketInput::attribuut);
        veld(PathState.class, "onderwerp", PathState::onderwerp);
        veld(PathState.class, "pad", PathState::pad);
        veld(PathState.class, "member", PathState::member);
        veld(PathState.class, "waarde", PathState::waarde);
        veld(ParameterState.class, "parameter", ParameterState::parameter);
        veld(ParameterState.class, "waarde", ParameterState::waarde);
        veld(EvaluationDate.class, "date", EvaluationDate::date);
        veld(ChannelState.class, "regel", ChannelState::regel);
        veld(ChannelState.class, "inconsistent", ChannelState::inconsistent);
        veld(CreationPhase.class, "number", CreationPhase::number);
        veld(CreationPhase.class, "stratum", CreationPhase::stratum);
        veld(Contribution.class, "fold", Contribution::fold);
        veld(Contribution.class, "onderwerp", Contribution::onderwerp);
        veld(Contribution.class, "waarde", Contribution::waarde);
        veld(FilterFault.class, "fold", FilterFault::fold);
        veld(FilterFault.class, "onderwerp", FilterFault::onderwerp);
        veld(FilterDeposit.class, "fold", FilterDeposit::fold);
        veld(FilterDeposit.class, "onderwerp", FilterDeposit::onderwerp);
        veld(MemberFault.class, "fold", MemberFault::fold);
        veld(MemberFault.class, "onderwerp", MemberFault::onderwerp);
        veld(UniquenessKey.class, "uniqueness", UniquenessKey::uniqueness);
        veld(UniquenessKey.class, "element", UniquenessKey::element);
        veld(UniquenessViolation.class, "uniqueness", UniquenessViolation::uniqueness);
        veld(UniquenessViolation.class, "element", UniquenessViolation::element);
        veld(Recipient.class, "allocation", Recipient::allocation);
        veld(Recipient.class, "bron", Recipient::bron);
        veld(CreationStamp.class, "regel", CreationStamp::regel);
        veld(CreationStamp.class, "onderwerp", CreationStamp::onderwerp);
        veld(CreationStamp.class, "object", CreationStamp::object);
        veld(Fout.class, "regel", Fout::regel);
        veld(Fout.class, "onderwerp", Fout::onderwerp);
        veld(RelationLink.class, "relation", RelationLink::relation);
        veld(RelationLink.class, "eerste", RelationLink::eerste);
        veld(RelationLink.class, "tweede", RelationLink::tweede);
        veld(ListPlace.class, "veld", ListPlace::veld);
        veld(ListPlace.class, "place", ListPlace::place);
        veld(Decides.class, "onderwerp", Decides::onderwerp);
        veld(Decides.class, "stack", Decides::stack);
        veld(Decides.class, "index", Decides::index);
        veld(Outcome.class, "onderwerp", Outcome::onderwerp);
        veld(Outcome.class, "stack", Outcome::stack);
        veld(Outcome.class, "chain", Outcome::chain);
        veld(Outcome.class, "positie", Outcome::positie);
        veld(Outcome.class, "soort", Outcome::soort);
        veld(Outcome.class, "waarde", Outcome::waarde);
        veld(Outcome.class, "sleutel", Outcome::sleutel);
    }

    @SuppressWarnings("unchecked")
    private static <T> void veld(Class<T> javaClass, String naam, Function1<T, Object> reader) {
        FIELDS.computeIfAbsent(javaClass, k -> new HashMap<>()).put(naam, (Function1<Object, Object>) (Function1<?, ?>) reader);
    }

    /** The read of field {@code naam} of an object of {@code klasse}: a component of a fact of the branch, or a bean
     *  property, read through the getter the bean generator wrote for it. */
    private static Function1<Object, Object> veld(Class<?> javaClass, String naam) {
        if (javaClass.getPackageName().equals(PatternDslRendering.class.getPackageName())) {
            Function1<Object, Object> f = FIELDS.getOrDefault(javaClass, Map.of()).get(naam);
            if (f == null) throw new IllegalStateException("geen lezer voor het veld " + naam + " van " + javaClass.getSimpleName());
            return f;
        }
        Function<Object, Object> getter = Accessors.eigenschapGetter(javaClass, naam);
        return getter::apply;
    }

    /** The test of one alpha constraint: its field tests in order, the first that fails ends it. */
    private static Predicate1<Object> predicate(Class<?> javaClass, Alpha a) {
        List<Predicate1<Object>> parts = new ArrayList<>();
        for (Alpha.FieldTest t : a.tests()) {
            Function1<Object, Object> f = reader(javaClass, t);
            Object w = t.waarde();
            parts.add(switch (t.op()) {
                case GELIJK -> x -> f.apply(x).equals(w);
                case NOT_EQUAL -> x -> !f.apply(x).equals(w);
                case AT_LEAST -> x -> ((Number) f.apply(x)).intValue() >= ((Number) w).intValue();
                case UNDER -> x -> ((Number) f.apply(x)).intValue() < ((Number) w).intValue();
                case WITHIN -> x -> ((Geldigheid) w).contains((LocalDate) f.apply(x));
                case IS_A -> x -> ((Class<?>) w).isInstance(f.apply(x));
                case LEEG -> x -> f.apply(x) == null;
                case GEVULD -> x -> f.apply(x) != null;
                case CLASS_NOT_IN -> x -> !((Set<?>) w).contains(f.apply(x).getClass());
                case CLASS_IS -> x -> f.apply(x).getClass() == w;
            });
        }
        if (parts.size() == 1) return parts.get(0);
        return x -> {
            for (Predicate1<Object> d : parts) if (!d.test(x)) return false;
            return true;
        };
    }

    /** The read a field test makes of the pattern's object ({@link Alpha.FieldTest}): a field as {@link #veld} reads
     *  it, of the declaring class the test names or else of the pattern's, or the object itself, as it is; else along
     *  the path, null where a link is absent, as the test's form reads it. */
    private static Function1<Object, Object> reader(Class<?> javaClass, Alpha.FieldTest t) {
        Function1<Object, Object> f = t.veld() == null ? x -> x
                : veld(t.declarant() != null ? t.declarant() : javaClass, t.property());
        if (t.pad().isEmpty() && t.vorm() == Binding.ReadForm.VALUE) return f;
        return along(t.pad(), vorm(f::apply, t.vorm(), null));
    }

    /** The read a binding makes: a field of a fact as its record accessor; on a bean the walk over the reference
     *  fields of the path, null where a link is absent, and then the member as the binding reads it. */
    private static Function1<Object, Object> reader(Binding b) {
        Class<?> javaClass = b.veld().javaClass();
        if (javaClass.getPackageName().equals(PatternDslRendering.class.getPackageName())) {
            if (!b.pad().isEmpty() || b.vorm() != Binding.ReadForm.VALUE)
                throw new IllegalStateException("een feit van de tak wordt gelezen zoals het is: " + b);
            return veld(javaClass, b.veld().property());
        }
        return along(b.pad(), inputReader(b.veld(), b.vorm(), b.soort()));
    }

    /** {@code end} of the object the walk over the reference fields {@code pad} reaches, null where a link on it is
     *  absent. */
    private static Function1<Object, Object> along(List<Binding.Field> pad, Function<Object, Object> end) {
        if (pad.isEmpty()) return o -> end.apply(o);
        List<Function<Object, Object>> steps = new ArrayList<>();
        for (Binding.Field step : pad) steps.add(Accessors.eigenschapGetter(step.javaClass(), step.property()));
        return o -> {
            Object d = o;
            for (Function<Object, Object> step : steps) {
                d = step.apply(d);
                if (d == null) return null;
            }
            return end.apply(d);
        };
    }

    /** The read of member {@code veld} off a bean, as {@code vorm} reads it ({@link Binding.ReadForm}). */
    static Function<Object, Object> inputReader(Binding.Field veld, Binding.ReadForm vorm, Class<?> soort) {
        return vorm(Accessors.eigenschapGetter(veld.javaClass(), veld.property()), vorm, soort);
    }

    /** {@code getter} as {@code vorm} reads what it gives: as it is, as whether it holds {@code true}, as the segments
     *  of a timeline, or as whether a reference is set, to an instance of {@code soort} where that is not null. */
    private static Function<Object, Object> vorm(Function<Object, Object> getter, Binding.ReadForm vorm,
                                                 Class<?> soort) {
        return switch (vorm) {
            case VALUE -> getter;
            case WAAR -> o -> Boolean.TRUE.equals(getter.apply(o));
            case PERIODS -> o -> Period.lijst((List<?>) getter.apply(o));
            case IS_SET -> o -> {
                Object other = getter.apply(o);
                return other != null && (soort == null || soort.isInstance(other));
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

    /** What an identity join ({@link #same}) hashes for a side that reads null: a key of that side alone, so that a
     *  null meets nothing, as in the predicate beside the index. */
    private static final Object NOTHING_LEFT = new Object(), NOTHING_RIGHT = new Object();

    /**
     * Constrains {@code p} to the facts whose {@code left} is the object that {@code right} reads off the object
     * {@code v} binds, by identity, as an indexed join under the constraint id {@code id}. Unindexed, Drools tests every
     * fact of the pattern's class against every tuple on the other side, so that a read of a derived value costs one
     * test per subject and a fire grows with the square of the input (the scale test of card tms-omschakeling,
     * {@code ScaleTrial}). Indexed, the memories of the join hash the object, and a fact meets only the tuples of its
     * own object. Drools does not evaluate an indexed constraint but trusts the hash, which compares with {@code equals}
     * and {@code hashCode}: a bean overrides neither ({@code FactModelGenerator}, held by {@code TmsIndexWitnessTest}),
     * so equal is identical there, as {@code ==} in the predicate. One extractor per side gives both the predicate and
     * the key, so the two cannot differ; Drools evaluates the predicate where it does not use the index. A side that
     * reads null meets nothing.
     */
    private static <T, U> PatternDSL.PatternDef<T> same(PatternDSL.PatternDef<T> p, String id, Variable<U> v,
                                                         Function1<T, Object> left, Function1<U, Object> right) {
        return p.expr(id, v, (x, o) -> {
            Object a = left.apply(x);
            return a != null && a == right.apply(o);
        }, PatternDSL.betaIndexedBy(Object.class, Index.ConstraintType.EQUAL, BETA_INDEX,
                x -> sleutel(left.apply(x), NOTHING_LEFT), o -> sleutel(right.apply(o), NOTHING_RIGHT)));
    }

    private static Object sleutel(Object waarde, Object nothing) {
        return waarde == null ? nothing : waarde;
    }
}
