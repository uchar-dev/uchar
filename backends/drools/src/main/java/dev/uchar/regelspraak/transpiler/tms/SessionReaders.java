package dev.uchar.regelspraak.transpiler.tms;

import dev.uchar.regelspraak.graph.ProducerConsumer.ElementKey;
import dev.uchar.regelspraak.ir.Condition;
import dev.uchar.regelspraak.ir.ResolvedAllocation;
import dev.uchar.regelspraak.ir.ResolvedRule;
import dev.uchar.regelspraak.loader.NodePositions;
import dev.uchar.regelspraak.message.Msg;
import dev.uchar.regelspraak.model.RegelSpraakModel;
import dev.uchar.regelspraak.model.RegelSpraakModel.ObjectType;
import dev.uchar.regelspraak.refusal.DroolsCode;
import dev.uchar.regelspraak.runtime.Accessors;
import dev.uchar.regelspraak.runtime.RegelSpraakRuntime;
import dev.uchar.regelspraak.schema.FactModelGenerator;
import dev.uchar.regelspraak.transpiler.core.Namen;
import dev.uchar.regelspraak.transpiler.tms.TmsTranspiler.Derived;
import dev.uchar.regelspraak.transpiler.tms.TmsTranspiler.Member;
import dev.uchar.regelspraak.transpiler.tms.TmsTranspiler.Node;
import dev.uchar.regelspraak.transpiler.tms.TmsTranspiler.ReadingRule;
import dev.uchar.regelspraak.transpiler.tms.TmsTranspiler.Site;
import dev.uchar.regelspraak.transpiler.tms.TmsTranspiler.StackLayout;
import dev.uchar.regelspraak.transpiler.tms.plan.ReadKind;
import dev.uchar.regelspraak.transpiler.tms.plan.Term;

import org.kie.api.runtime.ClassObjectFilter;
import org.kie.api.runtime.KieSession;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.BiConsumer;
import java.util.function.Function;

import static dev.uchar.regelspraak.transpiler.tms.TmsTranspiler.INCONSISTENCY;
import static dev.uchar.regelspraak.transpiler.tms.TmsTranspiler.timelineAttribute;

/**
 * The readers of a fired session of a model {@link TmsTranspiler} planned: what stands at the fixpoint
 * ({@link #readOf}), written back onto the beans ({@link #setDerivedValues(KieSession, Function)}), and what the
 * defaults stacks decided ({@link #decisionsOf}, {@link #periodDecisionsOf}), and in a build with readings how the
 * condition of every form came out ({@link #readingsOf}). They analyse nothing: they read the facts of the session
 * through what the analysis left, the knots of its stacks, the members its rules derive, the order of the document's
 * rules, the reading rules, and ask the analysis for the pointer of a default or a rule.
 */
final class SessionReaders {

    /** The analysis whose model the session fired, asked for what it computes on request: the pointer of a default or
     *  a rule, whether writers decide a timeline target per day, a fact type, a bean class. */
    private final TmsTranspiler analysis;
    private final RegelSpraakModel model;
    private final ClassLoader beanLoader;
    /** The knots of every effective stack the analysis lowered, by its name. */
    private final Map<String, StackLayout> layouts;
    /** The members the lowered rules derive, by key. */
    private final Map<ElementKey, Member> derived;
    /** The names of the document's rules in its order, each once. */
    private final List<String> documentOrder;
    /** The names of the lowered creation rules. */
    private final Set<String> creationRules;
    /** The simple name of the bean class of each object type, at the place of the type in {@code types}. */
    private final List<String> typeClasses, types;
    /** Where the nodes of the document stand on the wire in a build with readings; null in a build without. */
    private final NodePositions positions;
    /** The reading rules of a build with readings, in the order their forms lowered. */
    private final List<ReadingRule> readingRules;

    /** The readers over the outcome of {@code analysis}: the collections are the analysis' own, which it fills while it
     *  plans and leaves alone after, so the readers see them complete once the analysis is built. */
    SessionReaders(TmsTranspiler analysis, RegelSpraakModel model, ClassLoader beanLoader,
                   Map<String, StackLayout> layouts, Map<ElementKey, Member> derived, List<String> documentOrder,
                   Set<String> creationRules, List<String> typeClasses, List<String> types, NodePositions positions,
                   List<ReadingRule> readingRules) {
        this.analysis = analysis;
        this.model = model;
        this.beanLoader = beanLoader;
        this.layouts = layouts;
        this.derived = derived;
        this.documentOrder = documentOrder;
        this.creationRules = creationRules;
        this.typeClasses = typeClasses;
        this.types = types;
        this.positions = positions;
        this.readingRules = readingRules;
    }

    /**
     * What stands in {@code session} after a fire that reached its fixpoint, read without writing anything on a bean:
     * the derived values and characteristics, the objects and links the creations made, the evaluation faults that
     * stand, as data, and the §9.5 deposits. {@code label} names the objects of the case in the sentence of a fault, as
     * {@link #setDerivedValues(KieSession, Function)} does; null names an object by type and identity.
     *
     * <p>Two different values on one attribute of one object have no common fixpoint (RUNTIME-SEMANTICS ch. 5.7). The
     * rule of {@link TmsTranspiler#differenceRule} makes that a {@link Fault} of the fixpoint where two writers can disagree; where no
     * such rule stood, as in a session filled by hand, this reads the same {@link WritersConflict} as a fault and gives
     * the attribute no value: it does not choose. Equal values are no conflict.
     *
     * <p>It reads only a session whose last run through the driver ({@code DroolsBackend.fire}) reached its fixpoint,
     * as the driver's mark says ({@link Fired}), and refuses any other with a sentence that says what is missing: a
     * session fired with a plain {@code fireAllRules} or not at all holds no fixpoint it can tell, and on a model with a
     * creation the creation rules never had the focus, so it holds no created object or link; a run that ended at the
     * cap, at the creation budget or with an exception holds a state on the way to the fixpoint. It refuses as well a
     * session in which something happened outside the driver after its last run, a fact inserted, updated or deleted or
     * a rule that fired ({@link Watch}), naming what happened and what to do: fire again through the driver, or open
     * a fresh session where the driver refuses a further fire; and one in which a global or, without an update, a
     * relation changed, which the driver refuses. What this gives is thus always a fixpoint, that of the input as the
     * session knows it: an attribute or a characteristic set without an update is not seen.
     */
    public Fixpoint readOf(KieSession session, Function<Object, String> label) {
        return readFixpoint(session, label).fixpoint();
    }

    /**
     * What every effective defaults stack on a target that holds one value decided at the fixpoint of {@code session}:
     * one record per object of the session and such stack of its concrete type, the objects being the beans of the
     * input and the objects the creations made ({@link Fixpoint#objects}), read off the control facts the stack left
     * standing
     * ({@link ControlFact}) and the analysis that placed them, without evaluating anything again. A record with neither
     * a decider nor a conflict is one on which no default decided, and the target keeps its input; like every record it
     * carries the root chains of its stack. Under truth maintenance only the end state counts: a decision that a higher
     * one displaced, or whose justification went, is not in the session any more.
     *
     * <p>The simple form leaves one {@link Decides} per object, of the highest default that decided; the decision is the
     * default's own on a characteristic and the value the stack wrote on an attribute. The general form leaves an
     * {@link Outcome} per chain, of its highest settling default, and the reading follows them from the root down to the
     * default whose own content decided: at a default with exception groups, into the members where one of them
     * settled, and else to the default itself, which fell back on its own decision (RUNTIME-SEMANTICS ch. 7.9). Members
     * are read as the reconciliation reads them (ch. 7.2): the first conflict in member order passes on unchanged;
     * deciding members that differ are a conflict; deciding members that agree decide, and the first of them in member
     * order is named, the one whose decision the reconciliation takes.
     *
     * <p>A stack on a timeline target decides per day (RUNTIME-SEMANTICS ch. 7.11), so that on every day another default
     * can decide: it has no record here, and {@link #periodDecisionsOf} gives its decisions per period.
     *
     * <p>It refuses the sessions {@link #readOf} refuses, with the same sentences. An evaluation fault that stands means
     * the session has no answer ({@link Fixpoint#faults}); this gives what the control facts say all the same, and a
     * consumer reads the faults first. The list is in no particular order.
     */
    public List<DefaultsDecision> decisionsOf(KieSession session) {
        requireFixpoint(session);
        // a stack runs on instances of exactly its owner (ch. 7.10), so the stacks of an object are those of its class;
        // a record names one default per object and stack, which a stack per day does not have
        Map<Class<?>, List<String>> stacksOf = new HashMap<>();
        layouts.forEach((name, s) -> {
            if (!s.perDay()) stacksOf.computeIfAbsent(s.javaClass(), k -> new ArrayList<>()).add(name);
        });
        Map<String, Map<Object, List<ControlFact>>> perStack = new LinkedHashMap<>();
        for (Object f : session.getObjects(o -> o instanceof ControlFact)) {
            ControlFact c = (ControlFact) f;
            if (!layouts.containsKey(c.stack()))
                throw new IllegalStateException(c + " in the session, but this model lowered no stack " + c.stack());
            if (layouts.get(c.stack()).perDay()) continue;
            perStack.computeIfAbsent(c.stack(), k -> new IdentityHashMap<>())
                    .computeIfAbsent(c.subject(), k -> new ArrayList<>()).add(c);
        }
        // the value an attribute stack in the simple form wrote, by stack and object: the decision of its default; and
        // where that decision did not fit its target, the fault the stack stated in place of the value
        Map<String, Map<Object, List<Value>>> written = new HashMap<>();
        for (Object f : session.getObjects(o -> o instanceof Value w && layouts.containsKey(w.writer()))) {
            Value w = (Value) f;
            written.computeIfAbsent(w.writer(), k -> new IdentityHashMap<>())
                    .computeIfAbsent(w.subject(), k -> new ArrayList<>()).add(w);
        }
        Map<String, Map<Object, WriteBoundaryFault>> misfits = new HashMap<>();
        for (Object f : session.getObjects(o -> o instanceof Fault x && x.fault() instanceof WriteBoundaryFault
                && layouts.containsKey(x.rule()))) {
            Fault x = (Fault) f;
            misfits.computeIfAbsent(x.rule(), k -> new IdentityHashMap<>()).put(x.subject(), (WriteBoundaryFault) x.fault());
        }
        List<DefaultsDecision> out = new ArrayList<>();
        Map<String, List<List<String>>> rootChains = new HashMap<>();
        // the beans of the input and the objects the creations made: the session holds no other object of a type
        for (Object subject : session.getObjects(o -> stacksOf.containsKey(o.getClass())))
            for (String name : stacksOf.get(subject.getClass())) {
                StackLayout s = layouts.get(name);
                Map<Object, List<ControlFact>> perSubject = perStack.get(name);
                List<ControlFact> facts = perSubject == null ? null : perSubject.remove(subject);
                Settling settled = facts == null ? null : s.general() ? settledInGeneralForm(name, s, subject, facts)
                        : settledInSimpleForm(name, s, subject, facts, written.getOrDefault(name, Map.of()),
                                misfits.getOrDefault(name, Map.of()));
                List<List<String>> chains = rootChains.computeIfAbsent(name, n -> rootChains(s));
                out.add(new DefaultsDecision(subject, s.member().name(), s.member().characteristic(), chains,
                        settled == null ? null : atRest(settled.decider()),
                        settled == null || settled.conflict() == null ? null : new DefaultsDecision.Conflict(
                                settled.conflict().pointer(), settled.conflict().key(),
                                settled.conflict().deciders().stream().map(SessionReaders::atRest).toList())));
            }
        perStack.forEach((name, rest) -> {
            if (!rest.isEmpty())
                throw new IllegalStateException("control facts of " + name + " on "
                        + rest.keySet().stream().map(FactNames::object).toList() + ", which in the session is no"
                        + " object of the type the stack runs on: " + rest.values());
        });
        return out;
    }

    /**
     * Who decided every attribute on a timeline that a defaults stack or computing rules write, per object and period,
     * at the fixpoint of {@code session} (RUNTIME-SEMANTICS ch. 3.11, 7.11): one {@link PeriodDecisions} per object of
     * the session whose type has the member and per such member, read off the facts the lowering left standing, without
     * evaluating anything again.
     *
     * <p>Where a stack decides the member on the object's concrete type ({@link TmsTranspiler#dayStackRules}), the
     * {@link Outcome} of each default and of each reconciliation of exception groups carries the days it settled ({@link PeriodDefaults.Days}),
     * and per day the reading follows them from the root down as {@link #decisionsOf} follows the outcomes of a stack on
     * a target that holds one value: the first conflict in member order passes on unchanged; deciding members that
     * differ are a conflict; deciding members that agree decide, and the first of them in member order is named; at a
     * default with exception groups the day is its members' where one of them settled it, and the default's own, its
     * fall-back, where none did. Every day it decides agrees with the {@link Value} the stack wrote, unless a fault of
     * the stack stands on the object. Elsewhere the {@link Value}s of the computing rules carry the days each decides,
     * the filled segments of its timeline. On a day no writer decides, the input stands as the standing of the target
     * reads it ({@link #union}): the bean's, and none where a {@link BucketInput} of the target stands on the object.
     *
     * <p>It refuses the sessions {@link #readOf} refuses, with the same sentences. An evaluation fault that stands means
     * the session has no answer ({@link Fixpoint#faults}); this gives what the facts say all the same, and a consumer
     * reads the faults first. The list is in no particular order.
     */
    public List<PeriodDecisions> periodDecisionsOf(KieSession session) {
        requireFixpoint(session);
        List<Member> targets = new ArrayList<>();
        for (Member m : derived.values()) {
            if (!timelineAttribute(m) || !analysis.timelineWriters(m.key())) continue;
            if (m.cell() != null)
                throw new IllegalStateException("a cell on a timeline, " + m.spelled() + ", which the model derives,"
                        + " where the lowering refuses a dimensioned attribute on a timeline");
            targets.add(m);
        }
        // a stack runs on instances of exactly its owner (ch. 7.10): the stack per day of a bean class and a member
        Map<Class<?>, Map<ElementKey, String>> stackOf = new HashMap<>();
        layouts.forEach((name, s) -> {
            if (s.perDay()) stackOf.computeIfAbsent(s.javaClass(), k -> new HashMap<>()).put(s.member().key(), name);
        });
        Map<Object, Map<ElementKey, List<Value>>> written = new IdentityHashMap<>();
        for (Object f : session.getObjects(new ClassObjectFilter(Value.class))) {
            Value w = (Value) f;
            written.computeIfAbsent(w.subject(), o -> new HashMap<>())
                    .computeIfAbsent(w.attribute(), a -> new ArrayList<>()).add(w);
        }
        Map<String, Map<Object, List<Outcome>>> outcomes = new HashMap<>();
        for (Object f : session.getObjects(new ClassObjectFilter(Outcome.class))) {
            Outcome o = (Outcome) f;
            StackLayout s = layouts.get(o.stack());
            if (s == null)
                throw new IllegalStateException(o + " in the session, but this model lowered no stack " + o.stack());
            if (!s.perDay()) continue;
            outcomes.computeIfAbsent(o.stack(), k -> new IdentityHashMap<>())
                    .computeIfAbsent(o.subject(), k -> new ArrayList<>()).add(o);
        }
        Map<Object, Set<ElementKey>> buckets = new IdentityHashMap<>();
        for (Object f : session.getObjects(new ClassObjectFilter(BucketInput.class))) {
            BucketInput b = (BucketInput) f;
            buckets.computeIfAbsent(b.subject(), o -> new HashSet<>()).add(b.attribute());
        }
        Map<String, Set<Object>> faulted = new HashMap<>();
        for (Object f : session.getObjects(new ClassObjectFilter(Fault.class))) {
            Fault x = (Fault) f;
            faulted.computeIfAbsent(x.rule(), k -> Collections.newSetFromMap(new IdentityHashMap<>())).add(x.subject());
        }
        // a Value names its rule by name; a name two rules share names neither, which ruleSpans refuses
        Map<String, ResolvedRule> rulesByName = new HashMap<>();
        Set<String> shared = new HashSet<>();
        for (ResolvedRule r : model.resolvedRules)
            if (r.name() != null && rulesByName.putIfAbsent(r.name(), r) != null) shared.add(r.name());
        shared.forEach(rulesByName::remove);
        List<PeriodDecisions> out = new ArrayList<>();
        for (Member m : targets)
            for (Object subject : session.getObjects(o -> m.javaClass().isInstance(o))) {
                List<Value> values = new ArrayList<>(written.getOrDefault(subject, Map.of()).getOrDefault(m.key(), List.of()));
                values.sort(WritersConflict.inDocumentOrder(documentOrder));
                Object onBean = buckets.getOrDefault(subject, Set.of()).contains(m.key()) ? null
                        : Accessors.leesEigenschap(subject, Namen.property(m.name()));
                List<Period> input = onBean == null ? List.of() : Period.list((List<?>) onBean);
                String stack = stackOf.getOrDefault(subject.getClass(), Map.of()).get(m.key());
                if (stack == null) {
                    out.add(new PeriodDecisions(subject, m.name(), List.of(), ruleSpans(m, values, input, rulesByName)));
                    continue;
                }
                StackLayout s = layouts.get(stack);
                out.add(new PeriodDecisions(subject, m.name(), rootChains(s), stackSpans(stack, s, subject,
                        outcomes.getOrDefault(stack, Map.of()).getOrDefault(subject, List.of()), values, input,
                        faulted.getOrDefault(stack, Set.of()).contains(subject))));
            }
        return out;
    }

    /** The pointers of the defaults at the root of the stack laid out as {@code s}, per group, highest first
     *  ({@link DefaultsDecision#rootChains}). */
    private List<List<String>> rootChains(StackLayout s) {
        return s.roots().stream().map(chain -> chain.stream().map(k -> analysis.pointerOf(k.d())).toList()).toList();
    }

    /** The spans of the member {@code m} on a timeline that computing rules write on one object: per day the rules
     *  whose {@link Value}s {@code values}, in the order of the document, read a value there, and the input
     *  {@code input} where none does (ch. 3.11). */
    private List<PeriodDecisions.Span> ruleSpans(Member m, List<Value> values, List<Period> input,
                                                 Map<String, ResolvedRule> rulesByName) {
        List<List<Period>> timelines = new ArrayList<>();
        List<String> pointers = new ArrayList<>();
        for (Value w : values) {
            ResolvedRule r = rulesByName.get(w.writer());
            if (r == null)
                throw new IllegalStateException(w + " in the session, and " + w.writer() + " names no rule of the model"
                        + " (or more than one), where only computing rules and a stack " + m.spelled() + " write on a"
                        + " timeline");
            timelines.add(Period.list((List<?>) w.value()));
            pointers.add(analysis.rulePointer(r));
        }
        List<List<Period>> all = new ArrayList<>(timelines);
        all.add(input);
        List<PeriodDecisions.Span> out = new ArrayList<>();
        LocalDate previous = null;
        for (LocalDate c : dayCuts(all)) {
            if (previous != null) {
                List<DefaultsDecision.Decider> rules = new ArrayList<>();
                for (int i = 0; i < timelines.size(); i++) {
                    Object v = valueOn(timelines.get(i), previous);
                    if (v != null) rules.add(new DefaultsDecision.Decider(pointers.get(i), v));
                }
                spanOn(out, new PeriodDecisions.Span(previous, c, null, null, rules,
                        rules.isEmpty() ? valueOn(input, previous) : null));
            }
            previous = c;
        }
        return out;
    }

    /**
     * The spans of a member on a timeline that the stack per day {@code name}, laid out as {@code s}, decides on
     * {@code subject}: per day what {@code outcomes}, the subject's outcomes of the stack, settled at the root
     * ({@link #settledOn}), and the input {@code input} where the stack decided nothing (ch. 7.11). The days the stack
     * decides agree with its {@link Value} among {@code values}, which stands unless a fault of the stack stands on the
     * object ({@code faulted}): a segment that does not fit the target's decimals writes nothing (ch. 3.11).
     */
    private List<PeriodDecisions.Span> stackSpans(String name, StackLayout s, Object subject, List<Outcome> outcomes,
                                                  List<Value> values, List<Period> input, boolean faulted) {
        Map<Integer, List<Outcome>> perChain = new HashMap<>();
        List<List<Period>> all = new ArrayList<>();
        for (Outcome o : outcomes) {
            if (o.conflict() || !(o.value() instanceof PeriodDefaults.Days days))
                throw new IllegalStateException(o + " in the session, and a stack per day puts an outcome only with the"
                        + " days it decides (" + name + ")");
            perChain.computeIfAbsent(o.chain(), k -> new ArrayList<>()).add(o);
            List<Period> bounds = new ArrayList<>();
            for (PeriodArithmetic.Exact e : days.decided()) bounds.add(new Period(e.from(), e.to(), null));
            for (PeriodDefaults.Conflicted k : days.conflicted()) bounds.addAll(k.periods());
            all.add(bounds);
        }
        List<Period> wrote = null;
        for (Value w : values) {
            if (!w.writer().equals(name) || wrote != null)
                throw new IllegalStateException(w + " in the session, and the stack " + name + " is the one writer of "
                        + FactNames.member(s.member().key()) + " on " + FactNames.object(subject) + ": " + values);
            wrote = Period.list((List<?>) w.value());
            all.add(wrote);
        }
        all.add(input);
        List<PeriodDecisions.Span> out = new ArrayList<>();
        LocalDate previous = null;
        for (LocalDate c : dayCuts(all)) {
            if (previous != null) {
                Settling settled = settledOn(name, previous, s.roots(), 0, perChain, null, s.targetForm());
                DefaultsDecision.Decider decider = settled == null ? null : settled.decider();
                // where a fault of the stack stands and it wrote nothing, its outcomes are all there is to read
                Object value = wrote == null ? null : valueOn(wrote, previous);
                boolean comparable = wrote != null || !faulted;
                if (comparable && !PeriodArithmetic.sameValue(decider == null ? null : decider.decision(), value))
                    throw new IllegalStateException("the stack " + name + " wrote on " + FactNames.object(subject)
                            + " on " + previous + " " + value + ", and its outcomes give there " + settled);
                DefaultsDecision.Conflict conflict = settled == null || settled.conflict() == null ? null
                        : new DefaultsDecision.Conflict(settled.conflict().pointer(), settled.conflict().key(),
                                settled.conflict().deciders().stream().map(SessionReaders::atRest).toList());
                spanOn(out, new PeriodDecisions.Span(previous, c, atRest(decider), conflict, List.of(),
                        decider == null ? valueOn(input, previous) : null));
            }
            previous = c;
        }
        return out;
    }

    /** What the stack {@code name} settled on day {@code d}: the reconciliation of {@code chains}, numbered from
     *  {@code first}, as {@link #reconciled} reads the outcomes of a stack on a target that holds one value, read on
     *  that day; the conflict a default {@code at} (null at the root) records under {@code key}. Null where no member
     *  settled the day. A decision is the exact value of the outcome. */
    private Settling settledOn(String name, LocalDate d, List<List<Node>> chains, int first,
                               Map<Integer, List<Outcome>> perChain, String at, String key) {
        List<DefaultsDecision.Decider> deciders = new ArrayList<>();
        for (int m = 0; m < chains.size(); m++) {
            Settling settled = chainOn(name, d, chains.get(m), perChain.getOrDefault(first + m, List.of()), perChain);
            if (settled == null) continue;
            if (settled.conflict() != null) return settled;
            deciders.add(settled.decider());
        }
        if (deciders.isEmpty()) return null;
        for (DefaultsDecision.Decider x : deciders)
            if (!PeriodArithmetic.sameValue(x.decision(), deciders.get(0).decision()))
                return new Settling(null, new DefaultsDecision.Conflict(at, key, deciders));
        return new Settling(deciders.get(0), null);
    }

    /** What one chain settled on day {@code d}: the outcome of the one position of {@code chain} that settled it, among
     *  {@code mine}, the chain's outcomes; at a default with exception groups the settling of its members where one of
     *  them settled the day, which its outcome must agree with, and else its own decision, the fall-back. */
    private Settling chainOn(String name, LocalDate d, List<Node> chain, List<Outcome> mine,
                             Map<Integer, List<Outcome>> perChain) {
        Outcome on = null;
        for (Outcome o : mine) {
            if (!settles((PeriodDefaults.Days) o.value(), d)) continue;
            // a lower default of a chain decides only the days the defaults above it leave (ch. 7.11)
            if (on != null)
                throw new IllegalStateException(on + " and " + o + " both decide " + d + " of " + name
                        + ", where a chain decides a day once");
            on = o;
        }
        if (on == null) return null;
        PeriodDefaults.Days days = (PeriodDefaults.Days) on.value();
        Node k = knot(chain, on.position(), name);
        if (k.chain() != on.chain())
            throw new IllegalStateException(on + " does not lie on chain " + k.chain() + " of " + name);
        Settling members = k.hasMembers()
                ? settledOn(name, d, k.members(), k.firstMember(), perChain, analysis.pointerOf(k.d()), k.key()) : null;
        String conflictKey = conflictOn(days, d);
        Object decided = valueOnExact(days.decided(), d);
        if (members == null) {
            if (conflictKey != null)
                throw new IllegalStateException(on + " in the session, but under default " + k.label() + " of " + name
                        + " there is no conflict on " + d);
            return new Settling(new DefaultsDecision.Decider(analysis.pointerOf(k.d()), decided), null);
        }
        boolean agrees = members.conflict() != null ? members.conflict().key().equals(conflictKey)
                : conflictKey == null && PeriodArithmetic.sameValue(members.decider().decision(), decided);
        if (!agrees)
            throw new IllegalStateException(on + " in the session, and the members of default " + k.label() + " of " + name
                    + " give " + members + " on " + d);
        return members;
    }

    /** Whether {@code days} settles day {@code d}: decides it or conflicts on it. */
    private static boolean settles(PeriodDefaults.Days days, LocalDate d) {
        return valueOnExact(days.decided(), d) != null || conflictOn(days, d) != null;
    }

    /** The key under which the members {@code days} reconciled differ on day {@code d}, null where they do not; the two
     *  sets of days of {@link PeriodDefaults.Days} are disjoint, and the keys' days too. */
    private static String conflictOn(PeriodDefaults.Days days, LocalDate d) {
        for (PeriodDefaults.Conflicted c : days.conflicted())
            for (Period p : c.periods()) if (!p.from().isAfter(d) && p.to().isAfter(d)) return c.key();
        return null;
    }

    /** The bounds of every segment of {@code timelines} and the two open ends of all time, ordered. */
    private static TreeSet<LocalDate> dayCuts(List<List<Period>> timelines) {
        TreeSet<LocalDate> cuts = new TreeSet<>(List.of(Period.OPEN_FROM, Period.OPEN_TO));
        for (List<Period> t : timelines)
            for (Period p : t) {
                cuts.add(p.from());
                cuts.add(p.to());
            }
        return cuts;
    }

    /** The value of {@code timeline} on day {@code d}, null where it is leeg there. */
    private static Object valueOn(List<Period> timeline, LocalDate d) {
        for (Period p : timeline) if (!p.from().isAfter(d) && p.to().isAfter(d)) return p.value();
        return null;
    }

    /** The exact value of {@code segments} on day {@code d}, null where none covers it. */
    private static Object valueOnExact(List<PeriodArithmetic.Exact> segments, LocalDate d) {
        for (PeriodArithmetic.Exact s : segments) if (!s.from().isAfter(d) && s.to().isAfter(d)) return s.value();
        return null;
    }

    /** {@code span} after {@code spans}, joined with the last of them where it has the same outcome: the spans are the
     *  maximal periods with one outcome. */
    private static void spanOn(List<PeriodDecisions.Span> spans, PeriodDecisions.Span span) {
        PeriodDecisions.Span last = spans.isEmpty() ? null : spans.get(spans.size() - 1);
        if (last != null && sameOutcome(last, span))
            spans.set(spans.size() - 1, new PeriodDecisions.Span(last.from(), span.to(), last.decider(), last.conflict(),
                    last.rules(), last.input()));
        else spans.add(span);
    }

    /** Whether two spans have one outcome: the same writers with the same values, the same conflict, the same input;
     *  numbers compared by value. */
    private static boolean sameOutcome(PeriodDecisions.Span a, PeriodDecisions.Span b) {
        if (!PeriodArithmetic.sameValue(a.input(), b.input()) || !sameDecider(a.decider(), b.decider())) return false;
        if (a.rules().size() != b.rules().size()) return false;
        for (int i = 0; i < a.rules().size(); i++) if (!sameDecider(a.rules().get(i), b.rules().get(i))) return false;
        if (a.conflict() == null || b.conflict() == null) return a.conflict() == b.conflict();
        if (!Objects.equals(a.conflict().pointer(), b.conflict().pointer()) || !a.conflict().key().equals(b.conflict().key())
                || a.conflict().deciders().size() != b.conflict().deciders().size())
            return false;
        for (int i = 0; i < a.conflict().deciders().size(); i++)
            if (!sameDecider(a.conflict().deciders().get(i), b.conflict().deciders().get(i))) return false;
        return true;
    }

    private static boolean sameDecider(DefaultsDecision.Decider a, DefaultsDecision.Decider b) {
        if (a == null || b == null) return a == b;
        return a.pointer().equals(b.pointer()) && PeriodArithmetic.sameValue(a.decision(), b.decision());
    }

    /** How a chain or a reconciliation came out: a default that decided, or a conflict. */
    private record Settling(DefaultsDecision.Decider decider, DefaultsDecision.Conflict conflict) {}

    /** {@code d} with its decision as the write-back puts a number on a bean ({@link ComputeHelpers#collapse}), the
     *  form {@link Fixpoint.DerivedValue} gives it; null for none. */
    private static DefaultsDecision.Decider atRest(DefaultsDecision.Decider d) {
        return d == null ? null : new DefaultsDecision.Decider(d.pointer(), ComputeHelpers.collapse(d.decision()));
    }

    /** The one {@link Decides} of the simple form, on the default it names; where its decision did not fit the target,
     *  the value in {@code misfits}, the fault the stack stated in place of its write. */
    private Settling settledInSimpleForm(String name, StackLayout s, Object subject, List<ControlFact> facts,
                                         Map<Object, List<Value>> written, Map<Object, WriteBoundaryFault> misfits) {
        if (facts.size() != 1 || !(facts.get(0) instanceof Decides d))
            throw new IllegalStateException("the simple form of " + name + " leaves one Beslist per object, and"
                    + " on " + FactNames.object(subject) + " stand " + facts);
        Node k = knot(s.roots().get(0), d.index(), name);
        Object decision;
        if (s.member().characteristic()) decision = k.d().decision();
        else {
            List<Value> values = written.getOrDefault(subject, List.of());
            WriteBoundaryFault misfit = misfits.get(subject);
            if (values.isEmpty() && misfit != null)
                return new Settling(new DefaultsDecision.Decider(analysis.pointerOf(k.d()),
                        misfit.misfits().get(0).value()), null);
            if (values.size() != 1)
                throw new IllegalStateException(d + " in the session, and " + name + " wrote on "
                        + FactNames.object(subject) + " " + values + " instead of one value");
            decision = values.get(0).value();
        }
        return new Settling(new DefaultsDecision.Decider(analysis.pointerOf(k.d()), decision), null);
    }

    /** The {@link Outcome}s of the general form, reconciled at the root as the root reconciliation reads them. */
    private Settling settledInGeneralForm(String name, StackLayout s, Object subject, List<ControlFact> facts) {
        Map<Integer, Outcome> perChain = new HashMap<>();
        for (ControlFact f : facts) {
            if (!(f instanceof Outcome o))
                throw new IllegalStateException("the general form of " + name + " inserts only Uitkomst, and on "
                        + FactNames.object(subject) + " stands " + f);
            Outcome other = perChain.put(o.chain(), o);
            if (other != null)
                throw new IllegalStateException("chain " + o.chain() + " of " + name + " leaves one outcome per object,"
                        + " that of its highest default, and on " + FactNames.object(subject) + " stand " + other
                        + " and " + o);
        }
        return reconciled(name, s.roots(), 0, perChain, null, s.targetForm());
    }

    /** The reconciliation of the member chains {@code chains}, numbered from {@code first}, as ch. 7.2 reads their
     *  outcomes: the first conflict passes on, deciding members that differ conflict under {@code key} at the default
     *  {@code at} (null at the root), and agreeing ones decide as the first of them. Null where no member settled. */
    private Settling reconciled(String name, List<List<Node>> chains, int first, Map<Integer, Outcome> perChain,
                                String at, String key) {
        List<Outcome> decided = new ArrayList<>();
        List<DefaultsDecision.Decider> deciders = new ArrayList<>();
        for (int m = 0; m < chains.size(); m++) {
            Outcome o = perChain.get(first + m);
            if (o == null) continue;
            Settling settled = descended(name, chains.get(m), o, perChain);
            if (o.conflict()) return settled;
            decided.add(o);
            deciders.add(settled.decider());
        }
        if (decided.isEmpty()) return null;
        for (Outcome o : decided)
            if (!Outcome.equal(o.value(), decided.get(0).value()))
                return new Settling(null, new DefaultsDecision.Conflict(at, key, deciders));
        return new Settling(deciders.get(0), null);
    }

    /** The default of {@code chain} that settled as {@code o} says, followed down to the default whose own content
     *  decided, or to the conflict among the exception groups of a default. */
    private Settling descended(String name, List<Node> chain, Outcome o, Map<Integer, Outcome> perChain) {
        Node k = knot(chain, o.position(), name);
        if (k.chain() != o.chain())
            throw new IllegalStateException(o + " does not lie on chain " + k.chain() + " of " + name);
        Settling settled = k.hasMembers()
                ? reconciled(name, k.members(), k.firstMember(), perChain, analysis.pointerOf(k.d()), k.key()) : null;
        if (settled == null) {
            // no member settled, or none to settle: the default's own decision, its fall-back where it has members
            if (o.conflict())
                throw new IllegalStateException(o + " in the session, but under default " + k.label() + " of " + name
                        + " there is no conflict");
            return new Settling(new DefaultsDecision.Decider(analysis.pointerOf(k.d()), o.value()), null);
        }
        boolean agrees = o.conflict()
                ? settled.conflict() != null && settled.conflict().key().equals(o.key())
                : settled.decider() != null && Outcome.equal(settled.decider().decision(), o.value());
        if (!agrees)
            throw new IllegalStateException(o + " in the session, and the members of default " + k.label() + " of " + name
                    + " give " + settled);
        return settled;
    }

    /** The knot at {@code position} of {@code chain}. */
    private static Node knot(List<Node> chain, int position, String name) {
        if (position < 0 || position >= chain.size() || chain.get(position).position() != position)
            throw new IllegalStateException("a control fact of " + name + " names place " + position + ", and the chain"
                    + " has " + chain.size());
        return chain.get(position);
    }

    /**
     * Writes what stands at the fixpoint of {@code session} onto the beans, exactly what {@link #readOf} gives, read by the
     * same code: {@code setX(true)} for each derived characteristic, the value of each derived attribute through the
     * setter of the declared attribute, a timeline as the bean model's segments, and each link a creation laid on the
     * reference field of every to-one end the bean model gives a field, the field the input of such a link is read from
     * ({@link TmsTranspiler#linkRules}). Input on a characteristic stays, since nothing writes {@code false}; input on
     * an attribute stays where no rule derived a value for it.
     *
     * <p>An evaluation fault that stands at the fixpoint ({@link Fault}) means the session has no answer, so before it
     * writes anything this throws the faults that stand: one as the exception it is, named after its rule, two or more
     * as {@link EvaluationFaults}, ordered by the name of the form and then by the sentence, so that the report does not
     * depend on the order of the rules or of the firings. A fault that is a limit of this branch
     * ({@link ComputeFault#limit}) goes before every other: the first such fault in that order is thrown alone. Then it
     * writes nothing, and the session stays as it was. It
     * refuses the sessions {@link #readOf} refuses, those without a fixpoint of the driver, likewise before it writes
     * anything.
     *
     * <p>The engine is not told of the writes, and a bean then holds derived values beside its input, which a further
     * fire would read as input. So after the writes this states a {@link WrittenBack} mark, and the driver refuses to fire
     * the session again ({@code DroolsBackend.fire}). It states the mark with the {@link Watch} off, as its own work,
     * and has the watch record the relations after the links it wrote: {@link #readOf} reads the session after the
     * write-back as before it.
     */
    public void setDerivedValues(KieSession session) {
        setDerivedValues(session, null);
    }

    /**
     * As {@link #setDerivedValues(KieSession)}, with {@code label} naming the objects of the case: a
     * {@link WritersConflict} then names its object by the case label it gives, and as made by a creation where it
     * gives null, as the reference interpreter names the instance, and an {@link InputFormFault} names its object by
     * that label. Null names an object by type and identity.
     */
    public void setDerivedValues(KieSession session, Function<Object, String> label) {
        Read readFixpoint = readFixpoint(session, label);
        List<Fault> faults = readFixpoint.fixpoint().faults();
        // a limit of this branch leaves the session without an answer of the branch, so its refusal goes before the
        // faults beside it, which may stem from the value the branch could not hold
        for (Fault f : faults) if (f.fault() instanceof ComputeFault c && c.limit() != null) throw c;
        if (faults.size() == 1) throw faults.get(0).fault();
        if (!faults.isEmpty()) throw new EvaluationFaults(faults);
        readFixpoint.writeActs().forEach(Runnable::run);
        // the entry's own insert and links: the session is read after them as before them
        Watch watch = Watch.of(session);
        watch.without(session, () -> session.insert(new WrittenBack()));
        watch.recordRelations();
    }

    /** What {@link #readOf} reads, with beside it the writes that put it on the beans, made from the same facts in the
     *  same pass so that the reading and the write-back cannot differ. */
    private record Read(Fixpoint fixpoint, List<Runnable> writeActs) {}

    /** What a consumer does where something happened outside the driver after its last run ({@link Watch}): fire
     *  again, or open a fresh session where the driver refuses a further fire ({@code DroolsBackend.fire}). */
    private Msg fireAway(KieSession session, Watch watch, Msg agenda) {
        if (!creationRules.isEmpty()) return DroolsCode.FIXPOINT_OUTSIDE_FIRE.part("creations");
        if (!session.getObjects(new ClassObjectFilter(WrittenBack.class)).isEmpty())
            return DroolsCode.FIXPOINT_OUTSIDE_FIRE.part("written-back");
        if (watch.onOrOff() != null) return DroolsCode.FIXPOINT_OUTSIDE_FIRE.part("input");
        if (watch.moreThanOneUpdate() != null || agenda != null) return DroolsCode.FIXPOINT_OUTSIDE_FIRE.part("more");
        if (watch.changedGlobal(session) != null) return DroolsCode.FIXPOINT_OUTSIDE_FIRE.part("global");
        return DroolsCode.FIXPOINT_OUTSIDE_FIRE.part("fire-again");
    }

    /** The session test of {@link #readOf}, {@link #setDerivedValues} and {@link #decisionsOf}: a session whose last run
     *  through the driver reached its fixpoint and in which nothing happened since outside the driver; any other is
     *  refused with the sentence {@link #readOf} describes. */
    private void requireFixpoint(KieSession session) {
        // per class, as the driver asks for it: a filter of another kind walks every fact the session holds
        List<Object> marks = new ArrayList<>(session.getObjects(new ClassObjectFilter(Fired.class)));
        if (marks.isEmpty())
            throw new IllegalStateException(DroolsCode.FIXPOINT_NOT_FIRED.reason("generations", creationRules.isEmpty()
                    ? "" : DroolsCode.FIXPOINT_NOT_FIRED.part("generations")).text());
        if (marks.size() > 1)
            throw new IllegalStateException("the session holds " + marks.size() + " marks of DroolsBackend.fire, where"
                    + " the driver leaves one after every run: " + marks);
        String noFixpoint = ((Fired) marks.get(0)).noFixpoint();
        if (noFixpoint != null)
            throw new IllegalStateException(DroolsCode.FIXPOINT_NOT_REACHED.reason("noFixpoint", noFixpoint).text());
        Watch watch = Watch.of(session);
        // the driver puts the watch back after every run, so only a consumer that took it off leaves a mark without it,
        // and the driver refuses such a session
        if (watch == null)
            throw new IllegalStateException(DroolsCode.FIXPOINT_WATCH_REMOVED.reason().text());
        Msg agenda = Watch.agenda(session);
        Msg description = Watch.clauses(watch.outsideClause(), agenda);
        if (description != null)
            throw new IllegalStateException(DroolsCode.FIXPOINT_OUTSIDE_FIRE.reason("change", description, "advice",
                    fireAway(session, watch, agenda)).text());
        Msg global = watch.changedGlobal(session);
        if (global != null)
            throw new IllegalStateException(DroolsCode.FIXPOINT_GLOBAL_CHANGED.reason("change", global).text());
        Msg relation = watch.changedRelation();
        if (relation != null)
            throw new IllegalStateException(DroolsCode.FIXPOINT_RELATION_CHANGED.reason("change", relation).text());
    }

    private Read readFixpoint(KieSession session, Function<Object, String> label) {
        requireFixpoint(session);
        List<Runnable> acts = new ArrayList<>();
        // a set: the fault of two writers that the rule of verschilregel stated and the one read off the values below
        // are one fault, equal as facts are
        Set<Fault> faults = new LinkedHashSet<>();
        Watch watch = Watch.of(session);
        for (Object f : session.getObjects(o -> o instanceof Fault)) {
            Fault fault = (Fault) f;
            faults.add(label != null && fault.fault() instanceof WritersConflict c
                    ? new Fault(fault.subject(), fault.rule(), c.named(label))
                    : label != null && fault.fault() instanceof InputFormFault i
                    ? new Fault(fault.subject(), fault.rule(), i.named(label))
                    : label != null && fault.fault() instanceof OpenSegmentFault o
                    ? new Fault(fault.subject(), fault.rule(), o.named(label))
                    : label != null && fault.fault() instanceof CharacteristicPeriodsFault k
                    ? new Fault(fault.subject(), fault.rule(), k.named(label))
                    : label != null && fault.fault() instanceof SecondFillingFault s
                    ? new Fault(fault.subject(), fault.rule(), s.named(label))
                    // the place of a value that does not fit is the object of the fact where the fault had none
                    : fault.fault() instanceof WriteBoundaryFault w
                    ? new Fault(fault.subject(), fault.rule(), w.named(fault.subject(), label, watch::inputPlace))
                    : fault);
        }
        List<Fixpoint.DerivedCharacteristic> characteristics = new ArrayList<>();
        for (Object f : session.getObjects(o -> o instanceof HasCharacteristic)) {
            HasCharacteristic h = (HasCharacteristic) f;
            Member member = memberOf(h, h.characteristic());
            characteristics.add(new Fixpoint.DerivedCharacteristic(h.subject(), member.name()));
            BiConsumer<Object, Object> setter = Accessors.eigenschapSetter(member.javaClass(), Namen.property(member.name()));
            acts.add(() -> setter.accept(h.subject(), Boolean.TRUE));
        }
        Map<Object, Map<ElementKey, List<Value>>> perTarget = new IdentityHashMap<>();
        for (Object f : session.getObjects(o -> o instanceof Value)) {
            Value w = (Value) f;
            perTarget.computeIfAbsent(w.subject(), o -> new LinkedHashMap<>())
                    .computeIfAbsent(w.attribute(), a -> new ArrayList<>()).add(w);
        }
        List<Fixpoint.DerivedValue> values = new ArrayList<>();
        for (Map<ElementKey, List<Value>> perMember : perTarget.values())
            for (List<Value> written : perMember.values()) {
                // the writers as WritersConflict.inDocumentOrder ranks them, the rules in the order of the
                // document, and the values of one writer by their text: of values that agree up to their scale, 5,00
                // and 5, the bean gets that of the rule that comes first, on either branch and in every run, where the
                // working memory gives them in an order that the firings and the hash of the facts decide
                written.sort(WritersConflict.inDocumentOrder(documentOrder)
                        .thenComparing(w -> String.valueOf(w.value())));
                Value earliest = written.get(0);
                // the writers of a timeline target decide the days of their own segments, and differ only on a day
                // two decide (RUNTIME-SEMANTICS ch. 3.11, 5.7, "Two writers per day")
                boolean perDay = analysis.timelineWriters(earliest.attribute());
                if (perDay ? PeriodCondition.clash(written) != null
                        : written.stream().anyMatch(w -> !equalValue(earliest.value(), w.value()))) {
                    WritersConflict c = conflict(earliest.subject(), earliest.attribute(), written);
                    faults.add(new Fault(earliest.subject(), c.writers().get(0).writer(),
                            label == null ? c : c.named(label)));
                    continue;
                }
                Member member = memberOf(earliest, earliest.attribute());
                // a number goes onto the bean as a decimal, which a value of the unbounded form that does not terminate
                // is not exactly (RUNTIME-SEMANTICS ch. 2.4); a timeline as the bean model's segments, which carry no
                // value equality: the union of its writers, with the target's input on the days none of them decides
                @SuppressWarnings("unchecked")
                Object v = member.time() == null ? ComputeHelpers.collapse(earliest.value())
                        : Period.beans(perDay ? union(session, member, earliest.subject(), written)
                                : (List<Period>) earliest.value(), segmentClass());
                values.add(new Fixpoint.DerivedValue(earliest.subject(), member.name(), member.cell(), v));
                // a cell is an entry of the map its attribute holds (RUNTIME-SEMANTICS ch. 5.8c)
                if (member.cell() != null) {
                    String property = Namen.property(member.name());
                    acts.add(() -> ComputeHelpers.setCell(earliest.subject(), property, member.cell(), v));
                    continue;
                }
                BiConsumer<Object, Object> setter = Accessors.eigenschapSetter(member.javaClass(),
                        Namen.property(member.name()));
                acts.add(() -> setter.accept(earliest.subject(), v));
            }
        List<CreationStamp> marks = new ArrayList<>();
        for (Object f : session.getObjects(o -> o instanceof CreationStamp)) marks.add((CreationStamp) f);
        // the working memory gives its facts in an order of identities, which differs between two sessions of one input,
        // so the lists a consumer writes out as they come are sorted on names of their objects, which do not
        Function<Object, String> name = orderNames(marks, label);
        List<Fixpoint.MadeObject> objects = new ArrayList<>();
        for (CreationStamp g : marks) objects.add(new Fixpoint.MadeObject(g.object(), g.rule(), g.subject()));
        objects.sort(Comparator.comparing(m -> name.apply(m.object())));
        List<Fixpoint.MadeLink> links = new ArrayList<>();
        // a link of the input carries phase 0: it is in the bean fields, or, over a relation of two to-many roles, a fact
        // the input stated
        for (Object f : session.getObjects(o -> o instanceof RelationLink k && k.phase() > 0))
            links.add(madeLink((RelationLink) f, acts));
        links.sort(Comparator.comparing(Fixpoint.MadeLink::factType).thenComparing(Fixpoint.MadeLink::firstRole)
                .thenComparing(k -> name.apply(k.first())).thenComparing(Fixpoint.MadeLink::secondRole)
                .thenComparing(k -> name.apply(k.second())));
        List<Fault> ordered = new ArrayList<>(faults);
        ordered.sort(Comparator.comparing(Fault::rule).thenComparing(Fault::message));
        List<Object> inconsistencies = new ArrayList<>(session.getObjects(o -> o.getClass().getName().equals(INCONSISTENCY)));
        Comparator<String> emptyFirst = Comparator.nullsFirst(Comparator.naturalOrder());
        inconsistencies.sort(Comparator.comparing((Object i) -> (String) RegelSpraakRuntime.get(i, "rule"), emptyFirst)
                .thenComparing(i -> name.apply(RegelSpraakRuntime.get(i, "subject")), emptyFirst)
                .thenComparing(i -> String.valueOf(RegelSpraakRuntime.get(i, "codes"))));
        return new Read(new Fixpoint(values, characteristics, objects, links, ordered, inconsistencies), acts);
    }

    /**
     * The names on which {@link #readFixpoint} sorts the made objects, the links and the deposits of a session: the same
     * in every session of one input, whatever the order in which the input went in (card fixpoint-inconsistency-order).
     * An object a creation made is named as the dump names it ({@code SessionDump}), {@code «"creation"»(subject)#k},
     * with k counting the phases in which that creation made an object for that subject; an object of the input by the
     * case label {@code label} gives it, quoted; without a label, in the read without labels, by its type and identity,
     * which differ from one session to the next, as the sentence of a fault names it there. Null for no object.
     */
    private static Function<Object, String> orderNames(List<CreationStamp> marks, Function<Object, String> label) {
        Map<Object, CreationStamp> markOf = new IdentityHashMap<>();
        for (CreationStamp g : marks) markOf.put(g.object(), g);
        Map<Object, String> made = new IdentityHashMap<>();
        Function<Object, String> nameOf = o -> {
            if (markOf.containsKey(o)) return made.get(o);
            String l = label == null ? null : label.apply(o);
            return l != null ? quoted(l) : FactNames.object(o);
        };
        List<CreationStamp> byPhase = new ArrayList<>(marks);
        byPhase.sort(Comparator.comparingInt(CreationStamp::phase));
        Map<String, Integer> count = new HashMap<>();
        for (CreationStamp g : byPhase) {
            // a creation takes no subject made in its own phase, so an earlier phase named a made subject already
            String subject = nameOf.apply(g.subject());
            if (subject == null)
                throw new IllegalStateException(g + " in the session, but its subject was made in the same or a later"
                        + " phase");
            String key = "«" + quoted(g.rule()) + "»(" + subject + ")";
            made.put(g.object(), key + "#" + count.merge(key, 1, Integer::sum));
        }
        return o -> o == null ? null : nameOf.apply(o);
    }

    /** {@code s} between double quotes, with a quote, a backslash and a line break escaped, as the dump quotes a name, so
     *  that no name of {@link #orderNames} reads as another. */
    private static String quoted(String s) {
        return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r")
                + "\"";
    }

    /** The created link {@code k} as {@link Fixpoint} gives it, with into {@code acts} the write of each of its to-one
     *  ends that the bean model gives a reference field: the field on the filler of the other role, named after the
     *  to-one role, holds the filler of that role (§13.3.9), as {@link TmsTranspiler#linkRules} reads the input of the
     *  relation back from it. A to-many end has no field of its own; where both ends are to-many no bean holds the link, and it
     *  is in {@link Fixpoint#links} only, as the links of the input over such a relation are facts only. */
    private Fixpoint.MadeLink madeLink(RelationLink k, List<Runnable> acts) {
        RegelSpraakModel.FactType ft = analysis.factType(k.relation());
        if (ft == null || ft.roles.size() != 2)
            throw new IllegalStateException(k + " in the session, but the model knows no fact type of two roles '"
                    + k.relation() + "'");
        RegelSpraakModel.Role r0 = ft.roles.get(0), r1 = ft.roles.get(1);
        ObjectType t0 = model.objectTypeByName(r0.objectTypeName()), t1 = model.objectTypeByName(r1.objectTypeName());
        if (!r1.many() && FactModelGenerator.hasReferenceField(model, t0, r1.name(), t1)) {
            BiConsumer<Object, Object> setter = Accessors.eigenschapSetter(analysis.beanClass(t0),
                    Namen.property(r1.name()));
            acts.add(() -> setter.accept(k.first(), k.second()));
        }
        if (!r0.many() && FactModelGenerator.hasReferenceField(model, t1, r0.name(), t0)) {
            BiConsumer<Object, Object> setter = Accessors.eigenschapSetter(analysis.beanClass(t1),
                    Namen.property(r0.name()));
            acts.add(() -> setter.accept(k.second(), k.first()));
        }
        return new Fixpoint.MadeLink(ft.name, r0.name(), k.first(), r1.name(), k.second());
    }

    /** The bean model's {@code Segment}, the class of a timeline's segments on a bean. */
    private Class<?> segmentClass() {
        try {
            return Class.forName(FactModelGenerator.PACKAGE + ".Segment", true, beanLoader);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("no bean class Segment for a timeline", e);
        }
    }

    private Member memberOf(Object fact, ElementKey key) {
        Member member = derived.get(key);
        if (member == null)
            throw new IllegalStateException(fact + " in the session, but no rule of this model derives "
                    + FactNames.member(key));
        return member;
    }

    /** The timeline of the target {@code member} of {@code subject} that rules write, each the days of its own segments
     *  ({@link TmsTranspiler#timelineWriters}): the union of their timelines {@code written}, in the order of the
     *  document, with the target's input on the days none of them decides ({@link PeriodCondition#union}, RUNTIME-SEMANTICS
     *  ch. 5.7). The input is the bean's, which holds the case input while the session fires, and none where the case
     *  gives it as the asOfDate-bucket scalar, which carries no period. */
    private static List<Period> union(KieSession session, Member member, Object subject, List<Value> written) {
        List<List<Period>> parts = new ArrayList<>();
        for (Value w : written) parts.add(Period.list((List<?>) w.value()));
        boolean bucket = !session.getObjects(o -> o instanceof BucketInput b && b.subject() == subject
                && b.attribute().equals(member.key())).isEmpty();
        Object input = bucket ? null : Accessors.leesEigenschap(subject, Namen.property(member.name()));
        return PeriodCondition.union(parts, input == null ? null : Period.list((List<?>) input));
    }

    /** The fault of RUNTIME-SEMANTICS ch. 5.7 for the {@code waarden} that writers put on attribute {@code key} of
     *  {@code subjectType}: the writers as {@link WritersConflict#inDocumentOrder} ranks them over the document's
     *  rules {@link #documentOrder}, an allocation by its name, the attribute as declared, and the object's own type. */
    private WritersConflict conflict(Object subjectType, ElementKey key, List<Value> values) {
        Set<String> allocations = new HashSet<>();
        for (ResolvedAllocation v : model.allocations) if (v.name() != null) allocations.add(v.name());
        return WritersConflict.of(subjectType, memberOf(subjectType, key).spelled(), values, documentOrder, typeClasses,
                types, allocations);
    }

    /** Value equality at the write boundary: numbers by value, so that 5.0 and 5.00 are one value; leeg is null. */
    private static boolean equalValue(Object a, Object b) {
        return ComputeHelpers.equalValue(a, b);
    }

    /**
     * The readings of the conditions at the fixpoint of {@code session}, a session of a build with readings: for every
     * form with a condition, or without one that computes a value, and every object of the session the form runs on, one
     * record. Where the form's reading rule stood at the fixpoint, its {@link Reading} carries the values the form's own
     * rule bound there, and the record is the evaluation of the rule's condition over them, and of its value where the
     * condition holds, by the recording rendering ({@link RecordingRendering}), which deposits nothing; a form without a
     * condition holds there, and its record is the evaluation of its value. Where it did not stand, the form was not at
     * its turn there. A default whose turn the control facts of its stack guard has a speculative reading rule without
     * them ({@link Reading.Kind#SPECULATIVE}): where its {@link Reading} stands, the default was displaced by a higher
     * default of its chain, by the exception groups of the default it is the content of, or with the default whose
     * exception group it is, and the record is {@link ConditionReading.Status#DISPLACED} with the evaluation of its
     * condition, and of its value where the condition holds, over the values that rule bound, a fault of the condition
     * at its nodes. Elsewhere the record has no nodes: {@link ConditionReading.Status#DISPLACED} where a guard of the
     * form's turn kept it, the input on the target of an initialisation, an initialisation default's among them, or the
     * 1:1 guard of an object creation; {@link ConditionReading.Status#OUTSIDE_VALIDITY} where its validity does not
     * admit the session's evaluation date; {@link ConditionReading.Status#FAULTS} where the session has no date and the
     * form a dated validity. A form without a guard of its turn inside its validity has a reading on every object it
     * runs on, and an object without one is a broken contract, refused loudly; so is a reading on an object no form
     * runs on.
     *
     * <p>A node that reads a member over a path names the object the path reached, off the slot the form's own rule
     * bound it in beside the member (a {@link PathState}'s {@code reached}, or the reference fields of the subject's
     * bean), null where a link is absent; a node that reads a member of the subject names the subject. A node that was
     * not reached or faulted names none.
     *
     * <p>A contributor of a fold is read off its {@link Contribution} facts on the object: of a quantifier fold whose
     * matrix can fault or deposit, in their order with the stop rule of RUNTIME-SEMANTICS ch. 5.10 replayed over their
     * verdicts; of a quantifier fold whose matrix can do neither, with its verdict and without an order; of a value
     * fold, with what it contributes. It refuses the sessions {@link #readOf} refuses, with the same sentences. An
     * evaluation fault that stands means the session has no answer ({@link Fixpoint#faults}); this gives what the
     * readings say all the same, and a consumer reads the faults first. The list is in no particular order.
     */
    public List<ConditionReading> readingsOf(KieSession session) {
        if (positions == null)
            throw new IllegalStateException(DroolsCode.BUILD_WITHOUT_READINGS.reason().text());
        requireFixpoint(session);
        Map<Reading.Kind, Map<String, Map<Object, Reading>>> facts = new java.util.EnumMap<>(Reading.Kind.class);
        for (Reading.Kind kind : Reading.Kind.values()) facts.put(kind, new HashMap<>());
        for (Object f : session.getObjects(new ClassObjectFilter(Reading.class))) {
            Reading r = (Reading) f;
            Reading other = facts.get(r.kind()).computeIfAbsent(r.form(), k -> new IdentityHashMap<>()).put(r.subject(), r);
            if (other != null)
                throw new IllegalStateException("two readings of " + r.form() + " on " + FactNames.object(r.subject())
                        + " (" + r.kind() + "): " + other.values() + " and " + r.values());
        }
        LocalDate date = null;
        for (Object f : session.getObjects(new ClassObjectFilter(EvaluationDate.class))) date = ((EvaluationDate) f).date();
        Map<String, Map<Object, List<Contribution>>> contributions = null;
        List<ConditionReading> out = new ArrayList<>();
        for (ReadingRule r : readingRules) {
            Map<Object, Reading> ofForm = facts.get(Reading.Kind.TURN).getOrDefault(r.pointer(), Map.of());
            // a default stands in a stack per concrete type that composes it, each with a reading rule of its own, and a
            // speculative one only where control facts guard its turn there
            Map<Object, Reading> displaced = r.speculative() == null ? Map.of()
                    : facts.get(Reading.Kind.SPECULATIVE).getOrDefault(r.pointer(), Map.of());
            for (Object o : session.getObjects(new ClassObjectFilter(r.subject().beanClass()))) {
                if (!r.inRange(o)) continue;
                Reading reading = ofForm.isEmpty() ? null : ofForm.remove(o);
                Reading speculative = displaced.isEmpty() ? null : displaced.remove(o);
                // the reading of the turn where the form is at its turn; else, of a default that a higher one displaced,
                // the speculative reading, DISPLACED with the nodes it found (RUNTIME-SEMANTICS ch. 5.12)
                Reading read = reading != null ? reading : speculative;
                if (read == null) {
                    out.add(new ConditionReading(o, r.pointer(), withoutReading(r, o, date), List.of()));
                    continue;
                }
                if (contributions == null && r.sites().stream().anyMatch(x -> x.fold() != null))
                    contributions = contributions(session);
                out.add(reading(r, o, read, reading == null, contributions));
            }
        }
        facts.forEach((kind, ofKind) -> ofKind.forEach((form, rest) -> {
            if (!rest.isEmpty())
                throw new IllegalStateException("readings (" + kind + ") of " + form + " on "
                        + rest.keySet().stream().map(FactNames::object).toList() + ", and no form of this build"
                        + " runs there");
        }));
        return out;
    }

    /** Why the form of {@code r} has no reading on {@code o}, which it runs on: its validity does not admit the session's
     *  date {@code date}, or there is none; else the guard of its turn. A form without a guard is a broken contract. */
    private static ConditionReading.Status withoutReading(ReadingRule r, Object o, LocalDate date) {
        if (r.validity() != null && !r.validity().always())
            return date == null ? ConditionReading.Status.FAULTS : r.validity().contains(date)
                    ? guardedOrRefused(r, o) : ConditionReading.Status.OUTSIDE_VALIDITY;
        return guardedOrRefused(r, o);
    }

    private static ConditionReading.Status guardedOrRefused(ReadingRule r, Object o) {
        if (r.guarded()) return ConditionReading.Status.DISPLACED;
        throw new IllegalStateException("the reading rule of " + r.pointer() + " did not stand at the fixpoint on "
                + FactNames.object(o) + ", on which the form runs without a guard on its turn and within its validity");
    }

    /** The contributions of the session, by fold and subject. */
    private static Map<String, Map<Object, List<Contribution>>> contributions(KieSession session) {
        Map<String, Map<Object, List<Contribution>>> out = new HashMap<>();
        for (Object f : session.getObjects(new ClassObjectFilter(Contribution.class))) {
            Contribution b = (Contribution) f;
            out.computeIfAbsent(b.fold(), k -> new HashMap<>()).computeIfAbsent(b.subject(), k -> new ArrayList<>()).add(b);
        }
        return out;
    }

    /** The reading of {@code r} on {@code o} over the values of {@code reading}; where {@code displaced}, a reading of
     *  the speculative reading rule, of a default that was not at its turn: evaluated as at its turn, its status
     *  {@link ConditionReading.Status#DISPLACED}, a fault of its condition at its nodes. */
    private ConditionReading reading(ReadingRule r, Object o, Reading reading, boolean displaced,
                                     Map<String, Map<Object, List<Contribution>>> contributions) {
        Object[] w = reading.values().toArray();
        List<dev.uchar.regelspraak.transpiler.tms.plan.Read> slots = r.plan().readings().slots();
        if (w.length != slots.size())
            throw new IllegalStateException("the reading of " + r.pointer() + " carries " + w.length + " values, and the rule"
                    + " has " + slots.size() + " slots");
        // a fresh probe in every probe slot, which nobody reads: a reading deposits nothing
        for (dev.uchar.regelspraak.transpiler.tms.plan.Read l : slots)
            if (l.kind() == ReadKind.DEPOSIT_FLAG) w[l.slot()] = ComputeHelpers.depositFlag();
        RecordingRendering rec = new RecordingRendering();
        // the initialisation default of a stack on a timeline target whose input fills the target on every day is not
        // at its turn on any, and has no nodes (RUNTIME-SEMANTICS ch. 5.12, 7.11)
        if (r.inputDays() != null) try {
            if (ComputeHelpers.isEmpty(new RecordingRendering().value(r.inputDays(), w)))
                return new ConditionReading(o, r.pointer(), ConditionReading.Status.DISPLACED, List.of());
        } catch (RuntimeException e) {
            if (!ComputeHelpers.isEvaluationFault(e)) throw e;
            return new ConditionReading(o, r.pointer(), ConditionReading.Status.FAULTS, List.of());
        }
        ConditionReading.Status status;
        // a condition read per period gates nothing, so the form holds, and a fault of it fails the reading before the
        // value is read (RUNTIME-SEMANTICS ch. 3.10, 5.12)
        if (r.periods() != null) try {
            rec.value(r.periods(), w);
            status = ConditionReading.Status.HOLDS;
        } catch (RuntimeException e) {
            if (!ComputeHelpers.isEvaluationFault(e)) throw e;
            status = ConditionReading.Status.FAULTS;
        }
        // a form without a condition whose reading rule stood is at its turn, and holds there
        else if (r.condition() == null) status = ConditionReading.Status.HOLDS;
        else try {
            status = rec.test(r.condition(), w) ? ConditionReading.Status.HOLDS : ConditionReading.Status.FAILS;
        } catch (RuntimeException e) {
            if (!ComputeHelpers.isEvaluationFault(e)) throw e;
            status = ConditionReading.Status.FAULTS;
        }
        if (status == ConditionReading.Status.HOLDS && r.value() != null) {
            try {
                rec.value(r.value(), w);
            } catch (RuntimeException e) {
                // the form's consequence inserts this fault in place of the value; the node carries it
                if (!ComputeHelpers.isEvaluationFault(e)) throw e;
            }
        }
        List<ConditionReading.NodeReading> nodes = new ArrayList<>();
        for (Site s : r.sites()) {
            if (s.derived() != null) {
                nodes.add(derivedReading(s, rec, o));
                continue;
            }
            RecordingRendering.Outcome oc = rec.outcome(s.term());
            List<ConditionReading.Contributor> contributors = oc == null || s.fold() == null ? List.of()
                    : contributors(s, contributions.getOrDefault(s.fold().key(), Map.of()).getOrDefault(o, List.of()),
                            rec);
            boolean found = oc != null && oc.fault() == null;
            nodes.add(new ConditionReading.NodeReading(s.node(), s.kind(), oc != null,
                    oc == null ? null : RecordingRendering.given(oc.value()), s.unit(),
                    oc == null ? null : oc.fault(), !found ? null : s.navigates() ? w[s.objectSlot()] : own(s, o),
                    contributors));
        }
        // a default of a stack on a timeline target is at its turn where the days of its turn hold one (ch. 7.11)
        if (r.turn() != null && ComputeHelpers.isEmpty(new RecordingRendering().value(r.turn(), w))) displaced = true;
        return new ConditionReading(o, r.pointer(), displaced ? ConditionReading.Status.DISPLACED : status, nodes);
    }

    /** The object a reached node {@code s} without a path reads a member of, on the object {@code o} of its reading:
     *  {@code o} itself for a read of an attribute or of a cell of one or a check of a characteristic, whose member is
     *  the subject's own there (RUNTIME-SEMANTICS ch. 5.8 rule 4, ch. 5.8b, 5.12), and null for every other node, which
     *  reads no member of one object. */
    private static Object own(Site s, Object o) {
        return s.kind().equals("AttributeReference") || s.kind().equals("DimensionCellRead")
                || s.kind().equals("CharacteristicCheck") ? o : null;
    }

    /** The reading of the node of {@code s}, whose reading follows from the outcomes of its inputs in the evaluation
     *  {@code rec} ({@link Derived}), on the object {@code o}: reached where every input was reached without a fault;
     *  where the inputs are the node's own evaluation, reached with the fault of an input that faulted, which the node
     *  raised or passed on (RUNTIME-SEMANTICS ch. 5.12). */
    private static ConditionReading.NodeReading derivedReading(Site s, RecordingRendering rec, Object o) {
        List<Object> values = new ArrayList<>();
        for (Term t : s.derived().inputs()) {
            RecordingRendering.Outcome oc = rec.outcome(t);
            if (oc != null && oc.fault() != null && s.derived().raises())
                return new ConditionReading.NodeReading(s.node(), s.kind(), true, null, s.unit(), oc.fault(), null,
                        List.of());
            if (oc == null || oc.fault() != null)
                return new ConditionReading.NodeReading(s.node(), s.kind(), false, null, s.unit(), null, null, List.of());
            values.add(oc.value());
        }
        return new ConditionReading.NodeReading(s.node(), s.kind(), true,
                RecordingRendering.given(s.derived().compute().apply(values)), s.unit(), null, own(s, o), List.of());
    }

    /**
     * Whether node {@code node} of the reading of the form {@code form} reads a member over a path of roles
     * (RUNTIME-SEMANTICS ch. 5.8), whose object the canonical text of ch. 5.12 names: an attribute read or a
     * characteristic check with a path, also where the evaluation reads it as a timeline. False for a node of the form
     * that reads a member of its subject, or none.
     *
     * @throws IllegalStateException a build without readings, and a form or a node the readings of this build do not
     *                               have
     */
    public boolean navigates(String form, int node) {
        if (positions == null)
            throw new IllegalStateException(DroolsCode.BUILD_WITHOUT_READINGS.reason().text());
        for (ReadingRule r : readingRules) {
            if (!r.pointer().equals(form)) continue;
            for (Site s : r.sites()) if (s.node() == node) return s.navigates();
        }
        throw new IllegalStateException(DroolsCode.READINGS_NO_NODE.reason("node", node, "form", form).text());
    }

    /** The contributors of the fold of {@code s}, from its {@code contributions} on the object, as the evaluation
     *  {@code rec} reached them. */
    private static List<ConditionReading.Contributor> contributors(Site s, List<Contribution> contributions,
                                                                   RecordingRendering rec) {
        List<ConditionReading.Contributor> out = new ArrayList<>();
        // a fault of the fold's filter, or of a member's value, comes before the fold's value: its contributors are
        // not settled, and the node carries none (RUNTIME-SEMANTICS ch. 3.9a, 5.12)
        if (s.term() instanceof Term.FoldRead f && rec.outcome(f.outcome()) == null) return out;
        if (s.quantifier() == null) {
            for (Contribution b : contributions)
                out.add(new ConditionReading.Contributor(b.element(), RecordingRendering.given(b.value()), null, true));
            return out;
        }
        if (!s.fold().ordered()) {
            for (Contribution b : contributions)
                out.add(new ConditionReading.Contributor(b.element(), ((BigDecimal) b.value()).signum() > 0, null, null));
            return out;
        }
        // the stop rule over the contributors in their order, as ComputeHelpers.quantifierRead replays it; every
        // contributor keeps the verdict or the fault of its matrix, the position says whether the stop reached it
        List<Contribution> ordered = new ArrayList<>(contributions);
        ordered.sort(Comparator.comparing(b -> (MemberTest) b.value()));
        Condition.QuantifierFold q = s.quantifier();
        int n = ordered.size(), touch = 0;
        boolean stopped = false;
        for (int k = 0; k < n; k++) {
            MemberTest m = (MemberTest) ordered.get(k).value();
            if (!stopped && ComputeHelpers.established(q.quantifier(), q.threshold(), touch, k, n - k) != null)
                stopped = true;
            out.add(new ConditionReading.Contributor(ordered.get(k).element(), m.holds(), m.fault(), !stopped));
            if (!stopped && m.fault() != null) stopped = true;
            else if (!stopped && m.holds()) touch++;
        }
        return out;
    }
}
