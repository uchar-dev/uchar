package io.github.yhavinga.regelspraak.transpiler.model;

import io.github.yhavinga.regelspraak.ir.Conditie;
import io.github.yhavinga.regelspraak.ir.Default;
import io.github.yhavinga.regelspraak.ir.ExceptionGroup;
import io.github.yhavinga.regelspraak.ir.Expr;
import io.github.yhavinga.regelspraak.ir.PrioritizedDefaults;
import io.github.yhavinga.regelspraak.ir.ResolvedRegel;
import io.github.yhavinga.regelspraak.ir.ResolvedVerdeling;
import io.github.yhavinga.regelspraak.model.Datatype;
import io.github.yhavinga.regelspraak.model.Eenheid;
import io.github.yhavinga.regelspraak.model.RegelSpraakModel;
import io.github.yhavinga.regelspraak.model.RegelSpraakModel.Objecttype;
import io.github.yhavinga.regelspraak.model.RegelSpraakModel.Parameter;
import io.github.yhavinga.regelspraak.model.Rationaal;
import io.github.yhavinga.regelspraak.runtime.Accessors;
import io.github.yhavinga.regelspraak.runtime.CreationMarker;
import io.github.yhavinga.regelspraak.runtime.RuleFired;
import io.github.yhavinga.regelspraak.runtime.RegelSpraakRuntime;
import io.github.yhavinga.regelspraak.schema.FactModelGenerator;
import io.github.yhavinga.regelspraak.transpiler.kern.Dagsoorten;
import io.github.yhavinga.regelspraak.transpiler.kern.Dekking;
import io.github.yhavinga.regelspraak.transpiler.kern.Gastheer;
import io.github.yhavinga.regelspraak.transpiler.kern.Eenheidsconflict;
import io.github.yhavinga.regelspraak.kern.Literalen;
import io.github.yhavinga.regelspraak.transpiler.kern.Namen;
import io.github.yhavinga.regelspraak.transpiler.kern.Operatoren;
import io.github.yhavinga.regelspraak.transpiler.kern.Tijdlijnen;
import io.github.yhavinga.regelspraak.transpiler.kern.Vensterpositie;
import io.github.yhavinga.regelspraak.kern.Tijdrekening;

import org.drools.model.DSL;
import org.drools.model.Global;
import org.drools.model.Model;
import org.drools.model.PatternDSL;
import org.drools.model.Rule;
import org.drools.model.RuleItemBuilder;
import org.drools.model.Variable;
import org.drools.model.functions.Block2;
import org.drools.model.functions.Block3;
import org.drools.model.functions.Block4;
import org.drools.model.functions.Block5;
import org.drools.model.functions.Block6;
import org.drools.model.functions.Block7;
import org.drools.model.functions.Block8;
import org.drools.model.functions.Block9;
import org.drools.model.functions.Block10;
import org.drools.model.functions.Function1;
import org.drools.model.functions.Function2;
import org.drools.model.functions.Predicate1;
import org.drools.model.functions.Predicate2;
import org.drools.model.functions.Predicate3;
import org.drools.model.functions.Predicate4;
import org.drools.model.functions.Predicate5;
import org.drools.model.impl.ModelImpl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

/**
 * The EXECUTABLE-MODEL back-end (migration roadmap 20260624) — produces an {@link org.drools.model.Model}
 * (programmatic rules, Java lambdas) from the SAME resolved IR {@code DrlTranspiler} consumes
 * ({@code model.resolvedRegels} etc.). Replaces emitting DRL text. {@code DrlTranspiler} stays the GOLDEN
 * oracle; the differential harness fires both and asserts agreement.
 *
 * <p><b>Phase A scope</b> (the core rule spine): an assignment/kenmerk rule whose voorwaarde is null or a single
 * scalar {@link Conditie.Vergelijking} (own numeric attribute vs a numeric Parameter-global or Literal), and
 * whose value is a kenmerk's {@code true}, a Literal, an own-attribute read, or a {@code +} of those. Anything
 * outside scope is SKIPPED and recorded in {@link #overgeslagen()} — never silently mis-rendered (the marker
 * analog). Later phases (conditions/aggregations/fout/verdeling/temporal/creatie) extend this dispatch.
 */
public final class ExecModelTranspiler {

    public static final String PKG = "io.github.yhavinga.regelspraak.generated";
    static final String FACTS = "io.github.yhavinga.regelspraak.facts";

    private final RegelSpraakModel model;
    /** §9.2: the salience derivation over the declared kenmerk graph, shared with DrlTranspiler. */
    private final io.github.yhavinga.regelspraak.transpiler.Stratificatie strat;
    /** The classloader the bean fact classes ({@code io.github.yhavinga.regelspraak.facts.*}, Segment, Inconsistentie) are
     *  resolved against (card fase 1). Defaults to this transpiler's OWN loader; the {@link ScenarioRunner}/
     *  BeslisserKern shape (FactModelGenerator + BeanCompiler) threads a CHILD loader so child-loaded beans are
     *  visible at model-build time (a zero-arg {@code Class.forName} would miss them → a "geen fact-class" skip). */
    private final ClassLoader beanLoader;
    private final Map<String, Global<BigDecimal>> globals = new LinkedHashMap<>();
    /** §9.8 dagsoortdefinities compiled to per-date predicates by the SHARED {@link Dagsoorten} compiler
     *  (single-runtime principle: one implementation for both back-ends). Compiled lazily on first reference (§8.1.5 / §6
     *  duration); cross-dagsoort references and (spec-forbidden, §9.10) cycles are handled inside the compiler. */
    private final Dagsoorten dagsoorten;
    /** §13.3.6 temporal parameters: a tijdlijn parameter is a {@code List<Segment>} global (only a §7.3.2
     *  tijdsevenredig base reads it), kept apart from the scalar BigDecimal {@link #globals}. */
    private final Map<String, Global<java.util.List>> listGlobals = new LinkedHashMap<>();
    /** §13.3.8 DATUM (Datum-in-dagen) parameters: a {@code LocalDate} global, kept apart from the scalar BigDecimal
     *  {@link #globals} (the date-comparison-vs-global path binds it as a pattern variable). */
    private final Map<String, Global<java.time.LocalDate>> datumGlobals = new LinkedHashMap<>();
    /** §13.3.8 DATUM-EN-TIJD parameters: a {@code LocalDateTime} global (the datum-tijd-comparison-vs-global path binds it). */
    private final Map<String, Global<java.time.LocalDateTime>> datumTijdGlobals = new LinkedHashMap<>();
    /** §13.3.8 TEKST parameters: a {@code String} global (the tekst-equality-vs-global path binds it as a pattern var). */
    private final Map<String, Global<String>> tekstGlobals = new LinkedHashMap<>();
    /** §4.2 the Rekendatum global (a {@code LocalDate}) — registered iff ANY construct is time-bounded (a dated
     *  {@code geldig vanaf/tot} version), read by the per-construct {@link #geldigGuard} window guard. null when
     *  every construct is {@code geldig altijd} (no guard needed). Mirrors DrlTranspiler's {@code rekendatum} global. */
    private Global<java.time.LocalDate> rekendatumGlobal;
    /** §6.1 (Phase 4) the ch. 7 prioritized-defaults extension list — a top-level core-layer section the v0
     *  {@link RegelSpraakModel} does not carry, so it is threaded in beside the model (see {@link #build}).
     *  Empty by default (a flat program); lowered to reduce-rules in {@link #defaultsGroepen} / {@link #defaultsGroep}. */
    private List<PrioritizedDefaults> prioritizedDefaults = List.of();
    /** The wire's symbolic (feittype, rol) bindings per IR node, riding beside the model
     *  ({@code IrLoader.Loaded.rolBindingen}) because the vendored records cannot carry them: an objectcreatie's
     *  IR-SPEC ch. 3.3 {@code verbindendeRol} and an aggregation Functie's {@code bronRol}. Keyed by IDENTITY (the
     *  nodes are records with value equality). Empty by default — every node then uses the by-pair derivation,
     *  which is exactly the pre-2026-08-30 behaviour. */
    private Map<Object, RegelSpraakModel.RolBindingRef> rolBindingen = new java.util.IdentityHashMap<>();
    /** IR-SPEC ch. 4.3 / WF-102: the fold-source RANGE as a PATH per {@link Expr.Functie} node
     *  ({@code IrLoader.Loaded.bronPaden}), riding beside the model for the same vendored-record reason and for a
     *  sharper one — a path is the one extension the model cannot RE-DERIVE, so dropping it folds a coincidental
     *  direct relation instead. Keyed by IDENTITY. Empty by default (every fold then carries the one-step range). */
    private Map<Object, List<RegelSpraakModel.RolBindingRef>> bronPaden = new java.util.IdentityHashMap<>();
    /** IR-SPEC ch. 4.1a: the navigation bindings per AttribuutRef / Kenmerk node ({@code IrLoader.Loaded.navigatieBindingen}),
     *  by IDENTITY. Where a node carries one it decides the walk ({@link FactModelGenerator#navigatiewandeling}); only a
     *  bindingless node is resolved from its written navigatieRol/navigatiePad. */
    private Map<Object, List<RegelSpraakModel.RolBindingRef>> navigatieBindingen = new java.util.IdentityHashMap<>();
    /** RUNTIME-SEMANTICS ch. 2.10: the §4 EFFECTIVE-unit derivation — declarations plus the ch. 2.6 join rules.
     *  Every unit restatement below takes its datatypes from here; a bare {@code e.type()} at a restatement site
     *  reads the wire's REPETITION of a declaration instead of the declaration, which is what made this backend
     *  answer 3.750,00 where ref and clingo answer 3,75 on a unit-lying aggregation result type. */
    private io.github.yhavinga.regelspraak.transpiler.kern.EffectieveEenheid eff =
            new io.github.yhavinga.regelspraak.transpiler.kern.EffectieveEenheid(java.util.List.of());
    private final List<String> overgeslagen = new ArrayList<>();
    /** Build-time-only gate: true WHILE lowering a scalar consequence value (the THEN-waarde), the single
     *  context where a §13.4.1 multi-hop navigation chain may lower ({@link #navKetenReader}) — mirroring
     *  DrlTranspiler.waarde's KETEN branch. A chain reaching any CONDITION lowering (which also passes a
     *  non-null valueSubject into scalarWaarde) must keep the symmetric marker the DRL back-end emits. */
    private boolean ketenWaardePad;
    /** Monotonic within ONE rule (reset per rule) — keeps each rule's declaration NAMES ({@code $e_0}, {@code $inc_1})
     *  unique. Declaration names are per-rule scoped, so they may repeat across rules. */
    private int seq;
    /** Model-wide, NEVER reset — the source of {@link #nextId} constraint ids. Drools shares {@code .expr} constraints
     *  by id across the WHOLE package, so two DIFFERENT rules emitting the same id (e.g. the {@code (leeg gewicht)} and
     *  {@code (leeg of negatief maximum)} fout companions both starting at {@code fp_1}) would collapse to ONE
     *  constraint and silently use the first rule's predicate for both — the cross-rule analog of the within-rule
     *  id-collision. A global counter makes every constraint id unique, so no rule ever inherits another's predicate. */
    private int idSeq;
    /** The ch. 7.8 / ch. 7.7 target classification — the SHARED derivation
     *  ({@link io.github.yhavinga.regelspraak.transpiler.kern.StaandeDoelen}) the DRL-text branch reads too, so
     *  neither branch can drift on WHERE a leeg write is suppressed. Built lazily: the ch. 7 defaults list it
     *  consults is threaded in after construction (see {@link #build}). */
    private io.github.yhavinga.regelspraak.transpiler.kern.StaandeDoelen staande;

    /** A lowered voorwaarde: the subject pattern with its in-pattern constraints, plus zero or more SEPARATE
     *  rule items (the §8.1.7 far-side {@code exists}/{@code not} joins) appended after it — the model analog of
     *  {@code DrlTranspiler.LoweredConditie}'s inline + extra split. */
    private record Gate(PatternDSL.PatternDef pattern, List<RuleItemBuilder<?>> extra) {}

    private ExecModelTranspiler(RegelSpraakModel m, ClassLoader beanLoader) {
        this.model = m;
        this.dagsoorten = new Dagsoorten(m);
        this.strat = io.github.yhavinga.regelspraak.transpiler.Stratificatie.bereken(m.resolvedRegels);
        this.beanLoader = beanLoader != null ? beanLoader : ExecModelTranspiler.class.getClassLoader();
    }

    /** §9.10 fase-1 namespacing: the ENGINE-VISIBLE rule-name header for an emitted rule name — "<groep>: <naam>"
     *  for a §9.10 Regelgroep member, else the bare name (identity for ungrouped rules). Applied ONLY at the
     *  {@code PatternDSL.rule(...)} header; every data key (Inconsistentie/CreationMarker/RuleFired {@code regel})
     *  keeps the bare name, so all name-keyed mechanisms are unaffected. Mirrors DrlTranspiler's header prefix. */
    private String kop(String geëmitteerdeNaam) { return model.geëmitteerdeRegelNaam(geëmitteerdeNaam); }

    /** The §4 restatement datatype of {@code e}: its annotation with the EFFECTIVE unit grafted on
     *  ({@link io.github.yhavinga.regelspraak.transpiler.kern.EffectieveEenheid}). Every restatement argument in
     *  this file goes through here — a bare {@code e.type()} feeding {@code herschaald} / {@code herschaalWaarde} /
     *  {@code restatement} / {@code eenheidOf} is the defect this replaces. Null-safe (null in, null out). */
    private Datatype effType(Expr e) { return eff.van(e); }

    /** Build the exec {@link Model} AND expose the skipped-construct reasons (for the differential harness to
     *  decide a fixture is "not yet migrated" rather than a divergence). Fact beans resolve against
     *  {@code beanLoader}; null routes to this transpiler's own loader. */
    public static ExecModelTranspiler build(RegelSpraakModel m, ClassLoader beanLoader) {
        return build(m, beanLoader, List.of(), java.util.Map.of(), List.of(), java.util.Map.of());
    }
    /** The pre-WF-102 arity, kept for the callers that carry no path (every witness that builds a one-step model). */
    public static ExecModelTranspiler build(RegelSpraakModel m, ClassLoader beanLoader, List<PrioritizedDefaults> defaults,
            Map<Object, RegelSpraakModel.RolBindingRef> rolBindingen, List<Expr.Let> lets) {
        return build(m, beanLoader, defaults, rolBindingen, lets, java.util.Map.of());
    }
    /** As {@link #build(RegelSpraakModel, ClassLoader)} but also consumes what the loader carries BESIDE the model
     *  (see {@code IrLoader.Loaded}): the ch. 7 prioritized-defaults extension — each group lowers to one
     *  reduce-rule, a port of ref's {@code run_defaults_group} — the symbolic role bindings, and the ch. 4.10 Let
     *  index. An empty/null defaults list is the flat v0 case; an empty {@code rolBindingen} is the
     *  by-pair-derivation-only case. */
    public static ExecModelTranspiler build(RegelSpraakModel m, ClassLoader beanLoader, List<PrioritizedDefaults> defaults,
            Map<Object, RegelSpraakModel.RolBindingRef> rolBindingen, List<Expr.Let> lets,
            Map<Object, List<RegelSpraakModel.RolBindingRef>> bronPaden) {
        return build(m, beanLoader, defaults, rolBindingen, lets, bronPaden, java.util.Map.of());
    }
    /** As above, plus the ch. 4.1a navigation bindings ({@code IrLoader.Loaded.navigatieBindingen}) — the arity the
     *  replay API builds with. */
    public static ExecModelTranspiler build(RegelSpraakModel m, ClassLoader beanLoader, List<PrioritizedDefaults> defaults,
            Map<Object, RegelSpraakModel.RolBindingRef> rolBindingen, List<Expr.Let> lets,
            Map<Object, List<RegelSpraakModel.RolBindingRef>> bronPaden,
            Map<Object, List<RegelSpraakModel.RolBindingRef>> navigatieBindingen) {
        ExecModelTranspiler t = new ExecModelTranspiler(m, beanLoader);
        t.prioritizedDefaults = defaults == null ? List.of() : defaults;
        if (rolBindingen != null && !rolBindingen.isEmpty()) {
            // copied into an IdentityHashMap: the caller's map is identity-keyed, and a plain copy into a
            // value-keyed map would merge two structurally identical nodes into one binding.
            t.rolBindingen = new java.util.IdentityHashMap<>();
            t.rolBindingen.putAll(rolBindingen);
        }
        if (bronPaden != null && !bronPaden.isEmpty()) {
            t.bronPaden = new java.util.IdentityHashMap<>();   // identity, for rolBindingen's reason
            t.bronPaden.putAll(bronPaden);
        }
        if (navigatieBindingen != null && !navigatieBindingen.isEmpty()) {
            t.navigatieBindingen = new java.util.IdentityHashMap<>();   // identity, for rolBindingen's reason
            t.navigatieBindingen.putAll(navigatieBindingen);
        }
        t.eff = new io.github.yhavinga.regelspraak.transpiler.kern.EffectieveEenheid(
                lets == null ? List.of() : lets);
        t.model0 = t.run();
        return t;
    }
    private Model model0;
    public Model model() { return model0; }
    public List<String> overgeslagen() { return overgeslagen; }
    /** The coverage manifest — one {@link Dekking} verdict per construct, per soort in model-list
     *  order (comparable per soort with the DRL back-end's manifest). */
    public List<Dekking> dekking() { return dekking; }
    private final List<Dekking> dekking = new ArrayList<>();

    /** Run one construct's emitter and record its manifest verdict. This back-end's non-lowered
     *  discipline is the recorded skip ({@link #skip}/{@link #skipList} append to
     *  {@link #overgeslagen}), so "the emitter grew the skip list" IS the SKIP verdict — the wrapper
     *  measures that growth instead of re-deriving scope decisions. A construct that emits no rule
     *  WITHOUT recording would stay LOWERED here, which is why no emitter does: the one that did —
     *  the feitcreatie's inert voorwaarde — refuses by name since 2026-09-19, so emitting nothing and
     *  saying nothing is now unreachable rather than merely discouraged. A SKIP also raises a
     *  deduplicated WARNING (never silent). */
    private <T> T meet(String soort, String naam, java.util.function.Supplier<T> emitter) {
        int voor = overgeslagen.size();
        navReden = null;   // a reason belongs to the construct that produced it; never carried into the next one
        T res = emitter.get();
        Dekking d = overgeslagen.size() == voor ? Dekking.lowered(soort, naam)
                : Dekking.skip(soort, naam, String.join("; ", overgeslagen.subList(voor, overgeslagen.size())));
        dekking.add(d);
        d.meld(model);
        return res;
    }

    @SuppressWarnings("rawtypes")
    private Model run() {
        ModelImpl m = new ModelImpl(PKG);
        for (Parameter p : model.parameters.values()) {
            if (p.granulariteit() != null) {
                // §13.3.6 a TEMPORAL parameter is a List<Segment> global — only a §7.3.2 tijdsevenredig base reads it.
                Global<java.util.List> g = DSL.globalOf(java.util.List.class, PKG, globalNaam(p.naam()));
                listGlobals.put(globalNaam(p.naam()), g);
                m.addGlobal(g);
                continue;
            }
            // §13.3.8 a scalar parameter is a global of its DECLARED Java type (mirrors DrlTranspiler.javaType), so the
            // session setGlobal supplies a value of the right class. Only a NUMERIC (BigDecimal) parameter is TRACKED in
            // `globals` — the only type the condition/value binding paths consume (they cast the bound value to
            // BigDecimal). A date/tekst/booleaan parameter is registered (so setGlobal works) but NOT tracked, so a
            // construct reading it stays an honest skip rather than a fire-time "Illegal class for global" crash.
            if (p.datatype().isNumeriek() || p.datatype().isPercentage()) {
                Global<BigDecimal> g = DSL.globalOf(BigDecimal.class, PKG, globalNaam(p.naam()));
                globals.put(globalNaam(p.naam()), g);
                m.addGlobal(g);
            } else if (p.datatype() instanceof Datatype.DatumInDagen) {
                // §13.3.8 a Datum-in-dagen parameter is a LocalDate global — TRACKED so a datum-vs-global comparison binds it.
                Global<java.time.LocalDate> g = DSL.globalOf(java.time.LocalDate.class, PKG, globalNaam(p.naam()));
                datumGlobals.put(globalNaam(p.naam()), g);
                m.addGlobal(g);
            } else if (p.datatype() instanceof Datatype.DatumTijd) {
                // §13.3.8 a Datum-en-tijd parameter is a LocalDateTime global — TRACKED for a datum-tijd-vs-global comparison.
                Global<java.time.LocalDateTime> g = DSL.globalOf(java.time.LocalDateTime.class, PKG, globalNaam(p.naam()));
                datumTijdGlobals.put(globalNaam(p.naam()), g);
                m.addGlobal(g);
            } else if (p.datatype().isTekst() || p.datatype() instanceof Datatype.Enumeratie) {
                // §13.3.8 a Tekst OR §13.2.14 Enumeratie parameter is a String global (an enumeratiewaarde is stored as its
                // String label) — TRACKED so a tekst/enum equality-vs-global comparison binds it.
                Global<String> g = DSL.globalOf(String.class, PKG, globalNaam(p.naam()));
                tekstGlobals.put(globalNaam(p.naam()), g);
                m.addGlobal(g);
            } else {
                m.addGlobal(DSL.globalOf(globalKlasse(p.datatype()), PKG, globalNaam(p.naam())));
            }
        }
        // §4.2 register the Rekendatum global iff any construct of any family is time-bounded (mirrors
        // DrlTranspiler's `tijdgebonden` test); each dated construct then guards its pattern via geldigGuard.
        if (heeftTijdgebonden()) {
            rekendatumGlobal = DSL.globalOf(java.time.LocalDate.class, PKG, "rekendatum");
            m.addGlobal(rekendatumGlobal);
        }
        // Every construct family runs through meet(), which records the Dekking verdict around the
        // UNCHANGED emitter call — the emit order, rule-add order and all lowering behavior are as
        // before; only the measurement is new. (The manifest's inter-family order differs from the
        // DRL back-end's; consumers compare per soort, where both follow the same model lists.)
        for (ResolvedRegel r : model.resolvedRegels) {
            List<Rule> rules = meet("regel", r.naam(), () -> {
                Rule rule = regel(r);
                if (rule == null) return List.<Rule>of();
                List<Rule> alle = new ArrayList<>();
                alle.add(rule);
                alle.addAll(delingCompanions(r));   // §6.5 deling-door-leeg fout (Phase D)
                alle.addAll(wortelCompanions(r));   // §6.6 wortel-van-negatief fout (Phase D)
                alle.addAll(startpuntCompanions(r)); // §6.8 leeg startpunt-anker fout
                alle.addAll(gevuurdCompanions(r));  // §8.1.9 "is gevuurd" RuleFired marker (opt-in)
                return alle;
            });
            for (Rule rule : rules) m.addRule(rule);
        }
        for (io.github.yhavinga.regelspraak.ir.ResolvedConsistentie c : model.consistenties) {   // §9.5 / §13.4.8 (Phase D)
            Rule rule = meet("consistentie", c.naam(), () -> consistentie(c));
            if (rule != null) m.addRule(rule);
        }
        for (io.github.yhavinga.regelspraak.ir.ResolvedUniciteit u : model.uniciteiten) {        // §8.1.6 (Phase D)
            Rule rule = meet("uniciteit", u.naam(), () -> uniciteit(u));
            if (rule != null) m.addRule(rule);
        }
        for (io.github.yhavinga.regelspraak.ir.ResolvedVerdeling v : model.verdelingen) {        // §9.7 / §13.4.10 (Phase E)
            for (Rule rule : meet("verdeling", v.naam(), () -> verdeling(v))) m.addRule(rule);
        }
        for (io.github.yhavinga.regelspraak.ir.ResolvedObjectCreatie oc : model.objectCreaties) {  // §13.4.6 (Phase G)
            Rule rule = meet("objectcreatie", oc.naam(), () -> objectCreatie(oc));
            if (rule != null) m.addRule(rule);
        }
        for (io.github.yhavinga.regelspraak.ir.ResolvedFeitCreatie fc : model.feitCreaties) {       // §13.4.7 (Phase G)
            Rule rule = meet("feitcreatie", fc.naam(), () -> feitCreatie(fc));
            if (rule != null) m.addRule(rule);
        }
        for (io.github.yhavinga.regelspraak.ir.ResolvedUniciteitUnie u : model.uniciteitUnies)  // §8.1.6 "verenigd met" (Phase H)
            for (Rule rule : meet("uniciteit-unie", u.naam(), () -> uniciteitUnie(u))) m.addRule(rule);
        // §6.1 / ch. 7 prioritized defaults: one reduce-rule per (group, subject), a port of ref's run_defaults_group.
        // The composition itself can refuse (ch. 7.10 corner (a), a declared-unordered SET across a
        // supertype boundary). That is a per-form NOT-CARRIED, not a crash: route it through the manifest
        // like every other one, so the gate reports it in the auto-excluded channel with its reason
        // instead of as an unclassified ERROR.
        List<EffectieveStapel> stapels;
        try {
            stapels = defaultsGroepen();
        } catch (DefaultsOnvertaalbaar e) {
            stapels = List.of();
            meet("defaults", "effectieve stapels", () -> skipList("defaults: " + e.getMessage()));
        }
        for (EffectieveStapel groep : stapels)
            for (Rule rule : meet("defaults", defaultsConstructNaam(groep), () -> defaultsGroep(groep))) m.addRule(rule);
        return m;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private Rule regel(ResolvedRegel r) {
        if (!(r.doel() instanceof Expr.AttribuutRef doel) || !ongenavigeerd(doel) || doel.objecttype() == null) {
            return skip("regel (doel geen eigen attribuut/kenmerk): " + r.naam());
        }
        Class<?> factClass = factClass(doel.objecttype());
        if (factClass == null) return skip("regel (geen fact-class " + doel.objecttype().naam + "): " + r.naam());
        // §13.3.6.7 a MULTI-PERIOD temporal doel ("voor elke N maanden") is a List<Segment> timeline (gran set AND a
        // period count above 1) whose WRITE producer semantics are under-specified. Skip it honestly BEFORE the
        // doelSetter — never fall to the scalar value path below, which would silently emit a scalar setter on a
        // List<Segment> field. Mirrors the DRL multi-period marker.
        if (Namen.opNaam(doel.objecttype().granulariteitLeden(), doel.attribuut()) != null
                && Tijdlijnen.meerPeriodiek(doel.objecttype().tijdlijnAantalLeden(), doel.attribuut()))
            return skip("regel (meer-periodieke temporele doel — §13.3.6.7, geen producent): " + r.naam());

        String doelProp = doel.attribuut();
        BiConsumer<Object, Object> doelSetter = Accessors.setter(factClass, doelProp);   // typed write (Phase-H codegen)
        java.util.function.Function<Object, Object> doelGetter = Accessors.getter(factClass, doelProp);   // read-back for the idempotent update
        // ch. 7.8 (the timeline arm): on a STANDING temporal target an empty fold result decides nothing and the
        // case-supplied input timeline stands whole. The domain holds only ATTRIBUTE targets, but the lookup key
        // carries no member kind while WF-002 admits a kenmerk sharing an attribute's name — so the kenmerk
        // callsite below passes false explicitly (ref structurally cannot apply the arm to a Kenmerktoekenning).
        final boolean staandDoel = staande().staandTemporeel(doel);
        // ch. 7.8 (the scalar arm), together with the ch. 7.7 desugaring domain ref's scalar arm consults beside it:
        // on such a target a LEEG (null) computed value decides nothing and the case-supplied input — or, in the
        // desugaring domain, the initialisatie — stands. Only a computing rule is in ref's arm: a KENMERKTOEKENNING
        // must NEVER inherit it (the key carries no member kind while WF-002 admits a kenmerk sharing an attribute's
        // name, and ref reaches its arm structurally only from the Gelijkstelling/Berekening branch), and an
        // INITIALISATIE keeps its own §9.6 leeg-guard (ref's Initialisatie branch carries no standing arm either).
        final boolean staandScalairDoel = staande().staandScalair(r);
        seq = 0;
        Variable sVar = DSL.declarationOf(factClass, "$subject");
        PatternDSL.PatternDef pattern = PatternDSL.pattern(sVar);
        // SUBTYPING §4.1 rules 1 and 4: on the concrete types where inheritance made this rule a
        // defaults BASE, the composed stack writes it and the rule must not — with native `extends` a
        // pattern on the ancestor's class matches the descendant's facts, so without this the two would
        // both write. The set mirrors the SHARED predicate ref and clingo read, so no two executors
        // absorb on different instances; it is empty on every target whose chain declares no group,
        // which is every rule in a flat model and every rule the DRL-text branch ever sees (that branch
        // refuses a document carrying defaults at the door).
        Set<Class<?>> geabsorbeerd = new LinkedHashSet<>();
        if (r.soort() != ResolvedRegel.Soort.KENMERKTOEKENNING)
            for (Objecttype t : geabsorbeerdOp(doel.objecttype(), doel.attribuut())) {
                Class<?> c = factClass(t);
                if (c != null) geabsorbeerd.add(c);
            }
        if (!geabsorbeerd.isEmpty())
            pattern = pattern.expr(nextId("absorb"),
                    (Predicate1) (o -> !geabsorbeerd.contains(o.getClass())));
        List<RuleItemBuilder<?>> extra = new ArrayList<>();

        // §8.4/§13.4.5 a TEMPORAL KENMERK doel (kenmerkGranulariteit — a List<Segment> TRUE-period timeline, not a
        // boolean) is written the timeline of its voorwaarde's TRUE-periods: the doel-kenmerk holds exactly where the
        // gating kenmerk-tijdlijn does (§3.8 kenmerk-tijdlijn = boolean-tijdlijn), default-onwaar elsewhere (§3.4 —
        // monotone). The producer is conditieTijdreeks (the SAME lowering the §7.1 gedurende READ uses), so the WRITE
        // and READ of a kenmerk-tijdlijn are symmetric. DECIDABLE only with geldig altijd and a voorwaarde that lowers
        // to a finite timeline: an EMPTY voorwaarde is unbounded and a geldigheids-WINDOW needs a timeline×window clip
        // (§8.4 leaves that under-specified) — both stay a skip below. Mirrors the DRL back-end.
        boolean temporeelKenmerkDoel = r.soort() == ResolvedRegel.Soort.KENMERKTOEKENNING
                && Namen.bevatNaam(doel.objecttype().kenmerkGranulariteitLeden(), doel.attribuut())
                && !Tijdlijnen.meerPeriodiek(doel.objecttype().kenmerkTijdlijnAantalLeden(), doel.attribuut());
        java.util.function.Function<Object, List<?>> kenmerkTl =
                temporeelKenmerkDoel && r.voorwaarde() != null && r.geldigheid().altijd()
                        ? conditieTijdreeks(r.voorwaarde(), doel.objecttype()) : null;
        // A decidable temporal-kenmerk write ignores the scalar voorwaarde gate (the voorwaarde IS the write timeline)
        // and keeps the pattern class-reactive so it re-fires when the source kenmerk lands.
        if (r.voorwaarde() != null && kenmerkTl == null) {
            // §8.4.3 a periodevergelijking on a TIJDSAFHANKELIJK doel should CLIP the written timeline to the
            // period, not all-or-nothing gate it on the rekendatum — that clip is not built yet, so skip
            // honestly (mirrors the DRL marker; a scalar doel lowers via the voorwaarde() Periode branch).
            if (Conditie.bevatPeriode(r.voorwaarde())
                    && (Namen.bevatNaam(doel.objecttype().granulariteitLeden(), doel.attribuut())
                        || Namen.bevatNaam(doel.objecttype().kenmerkGranulariteitLeden(), doel.attribuut())))
                return skip("regel (periodevergelijking op tijdsafhankelijk doel — §8.4.3 clip): " + r.naam());
            Gate gate = voorwaarde(r.voorwaarde(), pattern, doel.objecttype(), sVar);
            // the sharpened §13.3.9 reason where a navigated hop refused (navReden), else the honest scope note
            if (gate == null) return skip("regel (" + (navReden != null ? navReden
                    : voorwaardeSkipReden(r.voorwaarde())) + "): " + r.naam());
            pattern = gate.pattern();
            extra.addAll(gate.extra());
        }
        // §9.6 initialisatie assigns only when the doel holds NO value yet — the same leeg-guard the DRL
        // back-end emits, so a berekening on the same doel wins whenever it applies, order-independently.
        if (r.soort() == ResolvedRegel.Soort.INITIALISATIE)
            pattern = pattern.expr(nextId("init"), (Predicate1) (s -> doelGetter.apply(s) == null));
        pattern = geldigGuard(pattern, r.geldigheid());   // §4.2 version validity window
        // Freshness (the DRL @watch mirrored): a pattern WITHOUT constraints has an EMPTY reactivity mask,
        // so an unconditioned chained berekening computed once (possibly against leeg) and never recomputed
        // when a sibling wrote its input — the differential chain fixture only passed on agenda-order luck.
        // Watch the OWN attributes the value reads; a pattern WITH any opaque expr is already class-reactive
        // (an explicit watch there would NARROW it — never add one). The idempotent werkBij keeps the
        // recompute chain at a fixpoint; a read-write CYCLE (resolver-WARNed) hits the engine's fire cap.
        // §8.4/§13.4.5 a decidable temporal-kenmerk write has NO scalar voorwaarde gate but still reads its source
        // kenmerk-tijdlijn — force class-reactivity (via the opaque freshness expr below) so it rebuilds when it lands.
        boolean klasseReactief = (r.voorwaarde() != null && kenmerkTl == null) || r.soort() == ResolvedRegel.Soort.INITIALISATIE
                || (r.geldigheid() != null && !r.geldigheid().altijd() && rekendatumGlobal != null);
        if (kenmerkTl != null) pattern = pattern.expr(nextId("fresh"), (Predicate1) (s -> true));
        // §9.9: an anchored temporal write reads the startpunt attribute (injected at lowering, invisible to
        // eigenReads) — count it as an own read, so the write re-fires when the startpunt rule lands.
        String startpuntAnker = ongenavigeerd(doel)
                ? model.startpuntAnkerLid(doel.objecttype(), doel.attribuut()) : null;
        if (!klasseReactief) {
            java.util.Set<String> reads = eigenReads(r.waarde(), doel.objecttype());
            // a watch on an EXPR-LESS pattern is inert (empirically: no reactivity registers), so anchor
            // freshness with an opaque always-true constraint — the black-box lambda leaves the pattern
            // CLASS-reactive (any subject update re-evaluates it), the exec's established idiom.
            if (!reads.isEmpty() || startpuntAnker != null)
                pattern = pattern.expr(nextId("fresh"), (Predicate1) (s -> true));
        }

        RuleItemBuilder<?> consequence;
        // §5.1.4 a single-period TEMPORAL doel (a List<Segment> timeline attribute) is WRITTEN a timeline, not a
        // scalar: the value must be a §13.4.17.57/.58 timeline producer (aantal-dagen-in / tijdsevenredig deel).
        RegelSpraakModel.Granulariteit doelGran = Namen.opNaam(doel.objecttype().granulariteitLeden(), doel.attribuut());
        boolean temporalDoel = doelGran != null && !Tijdlijnen.meerPeriodiek(doel.objecttype().tijdlijnAantalLeden(), doel.attribuut());
        // §8.4/§13.4.5 a temporal KENMERK doel (kenmerkGranulariteit — a List<Segment> true-period timeline) cannot
        // be written a scalar boolean: setX(true) on a List field throws at fire time. The DECIDABLE case writes the
        // voorwaarde's TRUE-period timeline (kenmerkTl, the SAME conditieTijdreeks lowering the §7.1 gedurende READ
        // uses): the doel-kenmerk holds exactly where the gating kenmerk-tijdlijn does (§3.8), default-onwaar elsewhere
        // (§3.4 — monotone, no complement seed). IDEMPOTENT (rsTijdreeksGelijk, mirroring the anchored-attribuut write
        // and the DRL guard): the freshness expr made the pattern class-reactive, so the guard stops a same-valued
        // rebuild from re-propagating. An EMPTY / geldigheids-windowed / negated / non-kenmerk voorwaarde left kenmerkTl
        // null (under-specified periodization) -> an honest skip (never a mis-typed boolean into the List field). The
        // DRL back-end MARKERS the same split, keeping the two symmetric.
        if (temporeelKenmerkDoel) {
            if (kenmerkTl == null)
                return skip("regel (temporele kenmerktoekenning — §8.4/§13.4.5 onbepaalde write-producer): " + r.naam());
            final java.util.function.Function<Object, List<?>> tl = kenmerkTl;
            // NEVER the standing arm here: this is a KENMERK write, and the standing domain holds attribute
            // targets only — a name-sharing temporal kenmerk (WF-002) must not inherit the attribute's status.
            consequence = DSL.on(sVar).execute((Block2<org.drools.model.Drools, Object>) (drools, s) ->
                    werkBijTijdlijn(drools, s, doelGetter, doelSetter, tl.apply(s), false));
            List<RuleItemBuilder<?>> items = new ArrayList<>();
            items.add(pattern);
            items.addAll(extra);
            items.add(consequence);
            return PatternDSL.rule(kop(r.naam()))
                    .attribute(Rule.Attribute.NO_LOOP, true)
                    .build(items.toArray(new RuleItemBuilder[0]));
        }
        // §5.8/§13.4.17 AGGREGATION value (SOM/TOTAAL/AANTAL/MIN/MAX over related elements): an accumulate CE binds
        // the result in the when, read in the consequence — distinct from a scalar fact-local value.
        WaardeFn tijdTotaal = !temporalDoel && isAggregatieWaarde(r) && r.waarde() instanceof Expr.Functie ftt
                && ftt.kind() == Expr.FunctieKind.TOTAAL ? tijdreeksTotaal(ftt, doel.objecttype()) : null;
        WaardeFn dimAgg = !temporalDoel && isAggregatieWaarde(r) && r.waarde() instanceof Expr.Functie fda
                && fda.dimensieSelectie() != null ? dimensieAggregatie(fda, doel.objecttype()) : null;
        if (temporalDoel) {
            // §13.4.17.57/.58 lower the timeline-producing RHS to a (subject -> List<Segment>) function and set it.
            // A producer we cannot lower is an honest skip (never a scalar setter on a List field, which would not
            // even reflect cleanly). NO_LOOP guards the modify; the List property is read by no pattern, so nothing
            // reacts to the write (Oracle-confirmed).
            // §3.8/§9.9: a doel riding a tijdlijn met VARIABEL startpunt buckets at the per-object anchor;
            // with NO startpuntbepaling resolved the timeline has no periodization -> honest skip (mirror DRL).
            if (startpuntAnker == null && model.heeftVariabelStartpunt(doel.objecttype(), doel.attribuut()))
                return skip("regel (tijdlijn met variabel startpunt zonder startpuntbepaling §9.9): " + r.naam());
            // §5.8.2/§5.1.4 "de som van <temporeel attr> van alle <naar-veel-rol>" into a TEMPORAL doel: a PER-PERIOD
            // sommatie over the navigated SET (kept temporal — §5.1.4; only the §7.1 "totaal van" collapses to scalar).
            // A collectList accumulate binds the element timelines; rsTijdSomAlle folds them per merged-knip period with
            // the §5.8.2 leeg rule. Anchored doelen stay a skip (set-fold × per-object bucket out of scope — mirror DRL).
            if (startpuntAnker == null) {
                // ch. 2.10: the doel is a REFERENCE position — restate into the DECLARED attribute datatype.
                SomAlleSet sas = tijdreeksSomAlleSet(r.waarde(), doel.objecttype(), sVar, effType(doel), doelGran);
                if (sas != null) {
                    extra.add(sas.accItem());
                    final BiConsumer<Object, Object> setter = doelSetter;
                    final java.util.function.Function<Object, Object> getr = doelGetter;
                    final boolean fbNul = sas.fallbackNul();
                    RuleItemBuilder<?> cons = DSL.on(sVar, sas.tijdlijnenVar()).execute((Block3<org.drools.model.Drools, Object, Object>) (drools, s, tls) ->
                            werkBijTijdlijn(drools, s, getr, setter,
                                    RegelSpraakRuntime.rsTijdSomAlle((List<? extends List<?>>) tls, fbNul, segmentClass()), staandDoel));
                    List<RuleItemBuilder<?>> items = new ArrayList<>();
                    items.add(pattern);
                    items.addAll(extra);
                    items.add(cons);
                    return PatternDSL.rule(kop(r.naam()))
                            .attribute(Rule.Attribute.NO_LOOP, true)
                            .build(items.toArray(new RuleItemBuilder[0]));
                }
            }
            final String ankerLid = startpuntAnker;
            java.util.function.Function<Object, Object> ankerReader =
                    ankerLid == null ? null : s -> RegelSpraakRuntime.get(s, ankerLid);
            TemporeleWaarde tw = temporeleWaarde(r.waarde(), doel.objecttype(), doelGran, effType(doel), ankerReader);
            // the sharpened §13.3.9 reason where the set fold's join refused (navReden), else the honest scope note —
            // the same distinction the scalar arms above and below make, extended here because the temporal set
            // sommatie is one of the readers that can now refuse for the bean-model reason (2026-09-11).
            if (tw == null) return skip("regel (" + (navReden != null ? navReden
                    : "temporele toekenning buiten Phase-F scope") + "): " + r.naam());
            BiFunction<Object, Object, Object> twf = tw.fn();
            // IDEMPOTENT timeline write (mirror DRL): the §9.9 freshness expr above makes this pattern class-reactive
            // whenever the value reads an own attribute, and an unconditional update ping-pongs with a sibling temporal
            // writer on the same subject (NO_LOOP blocks only self-retrigger) — ANCHORED OR NOT. werkBijTijdlijn holds
            // both the per-segment idempotency guard and the ch. 7.8 standing arm.
            if (tw.global() == null) {
                consequence = DSL.on(sVar).execute((Block2<org.drools.model.Drools, Object>) (drools, s) ->
                        werkBijTijdlijn(drools, s, doelGetter, doelSetter, twf.apply(s, null), staandDoel));
            } else {
                // §7.3.2 the tijdsevenredig base is a TEMPORAL PARAMETER (a List<Segment> global) — bind it.
                Global<java.util.List> g = listGlobals.get(tw.global());
                if (g == null) return skip("regel (temporele basis-parameter niet geregistreerd " + tw.global() + "): " + r.naam());
                consequence = DSL.on(sVar, (Variable) g).execute((Block3<org.drools.model.Drools, Object, Object>) (drools, s, gv) ->
                        werkBijTijdlijn(drools, s, doelGetter, doelSetter, twf.apply(s, gv), staandDoel));
            }
        } else if (dimAgg != null) {
            // §5.8.5 a dimensie-fold (SOM/MIN/MAX over a gedimensioneerd attribuut's selected labels) collapses a
            // Map<label,value> to a SCALAR — a fixed fold, NOT an instance accumulate, reading no globals.
            dimAgg = dimAgg.herschaald(effType(r.waarde()), effType(doel), "een dimensie-fold naar het doel");   // §4 EXACT restate the fold into the doel unit
            BiFunction<Object, Map<String, BigDecimal>, Object> fn = dimAgg.fn();
            consequence = DSL.on(sVar).execute((Block2<org.drools.model.Drools, Object>) (drools, s) ->
                    werkBij(drools, s, doelGetter, doelSetter, fn.apply(s, Map.of()), staandScalairDoel));
        } else if (tijdTotaal != null) {
            // §7.1 a TOTAAL over a TEMPORAL attribute is a SCALAR timeline fold (rsTijdTotaal over the subject's own/
            // navigated List<Segment>, optionally gedurende-clipped) — NOT an instance accumulate. It reads no globals
            // EXCEPT a §7.1 EUR71 gedurende-comparison vs a Parameter grens ("... groter is dan de grens"), which the
            // WaardeFn surfaces; bind those like the scalar path so the consequence reads them from the global map.
            tijdTotaal = tijdTotaal.herschaald(effType(r.waarde()), effType(doel), "een §7.1 tijdlijn-TOTAAL naar het doel");   // §4 EXACT restate the totaal into the doel unit
            BiFunction<Object, Map<String, BigDecimal>, Object> fn = tijdTotaal.fn();
            List<String> gnames = new ArrayList<>(tijdTotaal.globals());
            List<Variable> vars = new ArrayList<>();
            vars.add(sVar);
            for (String gn : gnames) {
                Global<?> g = waardeGlobal(gn);   // numeric Parameter or the §5.3 rekendatum pseudo-slot
                if (g == null) return skip("regel (totaal-grens refereert niet-geregistreerde global " + gn + "): " + r.naam());
                vars.add((Variable) g);
            }
            final List<String> fgn = gnames;
            consequence = consequenceOf(vars.toArray(new Variable[0]), (drools, args) -> {
                Object s = args[0];
                Map<String, BigDecimal> gm = new LinkedHashMap<>();
                vulGlobalMap(gm, fgn, args, 1);
                werkBij(drools, s, doelGetter, doelSetter, fn.apply(s, gm), staandScalairDoel);
            });
            if (consequence == null) return skip("regel (totaal: te veel gebonden globals): " + r.naam());
        } else if (isAggregatieWaarde(r) && r.waarde() instanceof Expr.Functie fx && !isGelisteFold(fx)) {
            // (a ≥2-operand MINIMUM/MAXIMUM is a §5.8.3 LISTED concatenation fold, not an instance accumulate — it
            //  flows to the scalar path / concatenatieFold below.)
            Agg agg = aggregatie(fx, doel.objecttype(), sVar, false);   // value side: bare fold (empty -> no-op)
            // the sharpened §13.3.9 reason where there is one (read immediately, see the field's contract), else
            // the generic Phase-C note — a bean-MODEL limit and an unbuilt arm are different things to whoever
            // reads the gate's exclusion line and decides what to card next.
            if (agg == null) return skip("regel (" + (aggregatieReden != null ? aggregatieReden
                    : "aggregatie buiten Phase-C scope") + "): " + r.naam());
            extra.add(agg.accItem());
            final Datatype aggVan = effType(fx), aggNaar = effType(doel);   // §4 declaration-derived, once at build
            if (agg.isSom()) {
                // §5.8.2 sommatie: an empty sum (0 non-empty contributors) is the leegwaarde fallback, not 0.
                Variable cntVar = agg.countVar(), sumVar = agg.resultVar();
                WaardeFn fallback = agg.somFallback();
                Block4<org.drools.model.Drools, Object, Object, Object> body = (drools, s, cnt, sum) -> {
                    BigDecimal v0 = ((Number) cnt).longValue() == 0L
                            ? (fallback == null ? null : (BigDecimal) fallback.fn().apply(s, Map.of()))
                            : RegelSpraakRuntime.toBd(sum);
                    Object v = herschaalWaarde(v0, aggVan, aggNaar);   // §4 EXACT restate the aggregate into the doel unit
                    werkBij(drools, s, doelGetter, doelSetter, v, staandScalairDoel);
                };
                consequence = DSL.on(sVar, (Variable) cntVar, (Variable) sumVar).execute(body);
            } else {
                Variable resVar = agg.resultVar();
                Block3<org.drools.model.Drools, Object, Object> body = (drools, s, res) -> {
                    Object v = herschaalWaarde(RegelSpraakRuntime.toBd(res), aggVan, aggNaar);   // §4 EXACT restate the aggregate into the doel unit
                    werkBij(drools, s, doelGetter, doelSetter, v, staandScalairDoel);
                };
                consequence = DSL.on(sVar, (Variable) resVar).execute(body);
            }
        } else if (isScalarSoort(r.soort()) && containsAggregatie(r.waarde())) {
            // §5.8 AGGREGATION INSIDE ARITHMETIC ("het aantal elementen maal de factor", "de totaal van X plus Y"):
            // the value is an arithmetic tree whose operand is an aggregation. Lower each embedded aggregation to its
            // own accumulate CE (binding a result var), then the consequence combines the bound results + own-attr
            // reads + a (≤1) global. Mirrors DrlTranspiler.waarde recursing through binairTerm into accumulate.
            Set<String> aggGlobals = new LinkedHashSet<>();
            List<RuleItemBuilder<?>> accItems = new ArrayList<>();
            List<Variable> accVars = new ArrayList<>();
            Eval eval = lowerVal(r.waarde(), doel.objecttype(), sVar, aggGlobals, accItems, accVars);
            if (eval == null) return skip("regel (aggregatie-in-arithmetiek buiten scope): " + r.naam());
            List<String> gnames = new ArrayList<>(aggGlobals);
            // consequence vars: [subject, g0, g1, ..., acc0, acc1, ...]; the body rebuilds the global map + agg array.
            List<Variable> vars = new ArrayList<>();
            vars.add(sVar);
            for (String gn : gnames) {
                Global<?> g = waardeGlobal(gn);   // numeric Parameter or the §5.3 rekendatum pseudo-slot
                if (g == null) return skip("regel (aggregatie-waarde refereert niet-geregistreerde global " + gn + "): " + r.naam());
                vars.add((Variable) g);
            }
            vars.addAll(accVars);
            final List<String> fgn = gnames; final int nG = gnames.size(); final int nAgg = accVars.size();
            final Datatype mixVan = effType(r.waarde()), mixNaar = effType(doel);   // §4 declaration-derived, once at build
            consequence = consequenceOf(vars.toArray(new Variable[0]), (drools, args) -> {
                Object s = args[0];
                Map<String, BigDecimal> gm = new LinkedHashMap<>();
                vulGlobalMap(gm, fgn, args, 1);
                Object[] aggs = new Object[nAgg];
                for (int i = 0; i < nAgg; i++) aggs[i] = args[1 + nG + i];
                Object v = eval.apply(s, gm, aggs);
                v = herschaalWaarde(v, mixVan, mixNaar);   // §4 EXACT restate the whole value into the doel unit
                werkBij(drools, s, doelGetter, doelSetter, v, staandScalairDoel);
            });
            if (consequence == null) return skip("regel (aggregatie-waarde: te veel gebonden variabelen): " + r.naam());
            extra.addAll(accItems);
        } else if (isScalarSoort(r.soort()) && r.waarde() instanceof Expr.ParameterRef tpr
                && nietNumeriekeGlobal(globalNaam(tpr.parameter().naam())) != null) {
            // §13.4.6 assign a single NON-NUMERIC Parameter-GLOBAL verbatim into the attribute — a Tekst/Enumeratie String
            // ("De naam wordt gesteld op de STANDAARDNAAM"), a Datum LocalDate or a Datum-en-tijd LocalDateTime
            // ("De datum wordt gesteld op de PARAMETERDATUM"). The numeric value-path below binds globals as BigDecimal, so
            // a non-numeric global would skip there. Bind the typed global and set it directly. (DRL declares the global
            // with its concrete type so the setter resolves — both back-ends used to decline this: DRL build-error / exec skip.)
            Global<?> g = nietNumeriekeGlobal(globalNaam(tpr.parameter().naam()));
            consequence = DSL.on(sVar, (Variable) g).execute((Block3<org.drools.model.Drools, Object, Object>) (drools, s, gv) ->
                    werkBij(drools, s, doelGetter, doelSetter, gv, staandScalairDoel));
        } else {
            // scalar value path: a WaardeFn(subject, global-values -> value) plus the Parameter-globals it reads.
            // the doel's objecttype rides along as the VALUE-path subject (no-mutable-build-state: a parameter, no field),
            // so a to-one navigated value read can resolve its role.
            WaardeFn wf;
            try {
                // §13.4.1 multi-hop chains lower only here (the consequence VALUE), mirroring DrlTranspiler.waarde's
                // KETEN branch; the flag is build-time-only and strictly scoped so no condition lowering sees it.
                ketenWaardePad = true;
                wf = switch (r.soort()) {
                    case KENMERKTOEKENNING -> WaardeFn.constant(Boolean.TRUE);      // "is <kenmerk>" -> kenmerk == true
                    case GELIJKSTELLING, BEREKENING, INITIALISATIE -> scalarWaarde(doel.objecttype(), r.waarde());
                    default -> null;
                };
            } finally {
                ketenWaardePad = false;
            }
            if (wf == null) return skip("regel (" + (navReden != null ? navReden : "waarde buiten scope") + "): " + r.naam());
            // Navigated-read reactivity: a to-one navigated value read makes the rule stale on the FAR fact's update
            // (property/class reactivity is per fact TYPE). Add a far-side JOIN per distinct navigation so the spiegel
            // re-fires when the navigated fact lands (the order-fragility fix). The consequence's read stays as-is.
            List<RuleItemBuilder<?>> navWatch = navReactiviteit(r.waarde(), doel.objecttype(), sVar);
            if (navWatch == null) return skip("regel (" + navReden + "): " + r.naam());
            extra.addAll(navWatch);
            // §4 value-side unit restatement, EXACT (a kenmerk value is Boolean, no unit). Mirrors DrlTranspiler.omrekenWaarde.
            if (r.soort() != ResolvedRegel.Soort.KENMERKTOEKENNING)
                wf = wf.herschaald(effType(r.waarde()), effType(doel), "een §4 schrijfgrens naar het doel");
            // resolve the referenced globals; an unregistered one (e.g. a temporal parameter) -> honest skip. The
            // consequence binds [subject, g0, g1, ...] via consequenceOf (up to Block10 = 9 vars); the body rebuilds
            // the name->value map. >1 distinct global is now supported (e.g. a value of two parameters).
            List<String> gnames = new ArrayList<>(wf.globals());
            List<Variable> vars = new ArrayList<>();
            vars.add(sVar);
            for (String gn : gnames) {
                Global<?> g = waardeGlobal(gn);   // numeric Parameter or the §5.3 rekendatum pseudo-slot
                if (g == null) return skip("regel (THEN-waarde refereert niet-geregistreerde global " + gn + "): " + r.naam());
                vars.add((Variable) g);
            }
            BiFunction<Object, Map<String, BigDecimal>, Object> fn = wf.fn();
            final List<String> fgn = gnames;
            consequence = consequenceOf(vars.toArray(new Variable[0]), (drools, args) -> {
                Object s = args[0];
                Map<String, BigDecimal> gm = new LinkedHashMap<>();
                vulGlobalMap(gm, fgn, args, 1);
                werkBij(drools, s, doelGetter, doelSetter, fn.apply(s, gm), staandScalairDoel);
            });
            if (consequence == null) return skip("regel (THEN-waarde: te veel gebonden globals): " + r.naam());
        }

        // The subject pattern first (binds $subject), then far-side exists/not joins and any accumulate (which
        // reference $subject), then the consequence. NO_LOOP is mandatory (the modify must not re-fire this rule).
        List<RuleItemBuilder<?>> items = new ArrayList<>();
        items.add(pattern);
        items.addAll(extra);
        items.add(consequence);
        int salience = stratificatieSalience(r);   // §9.2 negation-stratification: producers of a negated-read kenmerk first
        var bouwer = PatternDSL.rule(kop(r.naam())).attribute(Rule.Attribute.NO_LOOP, true);
        if (salience != 0) bouwer = bouwer.attribute(Rule.Attribute.SALIENCE, salience);
        return bouwer.build(items.toArray(new RuleItemBuilder[0]));
    }

    /** Write {@code v} to the doel via {@code setter}, then {@code drools.update} ONLY when the value actually CHANGED
     *  (read-back via {@code getter}). The conditional update is the model-side analogue of DRL's property reactivity:
     *  a rule whose write does not change the doel must NOT re-propagate the subject, else two modify-rules over the
     *  same subject ping-pong forever — a navigated-comparison constraint is a black-box lambda Drools re-evaluates on
     *  EVERY update of the subject, so NO_LOOP (which blocks only the rule's OWN re-trigger) + class reactivity is not
     *  enough to reach a fixpoint. {@link java.util.Objects#equals} only ever OVER-updates (a same-valued but different-
     *  scale BigDecimal re-propagates harmlessly); it never UNDER-updates, so the fixpoint outcome still matches DRL.
     *
     *  <p>{@code staandDoel} carries the ch. 7.8 STANDING arm (the scalar twin of {@link #werkBijTijdlijn}'s): on a
     *  standing scalar target or a ch. 7.7 desugaring-domain target — the SHARED classification
     *  ({@link io.github.yhavinga.regelspraak.transpiler.kern.StaandeDoelen#staandScalair}) the DRL-text branch
     *  reads too — a LEEG computed value decides nothing, so NOTHING is written and what the fact
     *  base holds — the case-supplied input, or the initialisatie's value — stands (ref: the {@code Value.Empty}
     *  arm of {@code ref/lib/eval.ml}'s Gelijkstelling/Berekening branch). Scalar leeg is {@code null}, tested
     *  AFTER {@link #collapseBreuk} so the write boundary is one place, not two. */
    private static void werkBij(org.drools.model.Drools drools, Object s,
            java.util.function.Function<Object, Object> getter, BiConsumer<Object, Object> setter, Object v,
            boolean staandDoel) {
        v = collapseBreuk(v);   // §6.1.2 write boundary: a remaining exact rational collapses to its decimal value
        if (staandDoel && v == null) return;
        Object oud = getter.apply(s);
        setter.accept(s, v);
        if (!java.util.Objects.equals(oud, v)) drools.update(s);
    }

    /** §6.1.2 write-boundary collapse: a value at rest is decimal-typed, so any exact {@link io.github.yhavinga.regelspraak.runtime.Breuk}
     *  that survived the computation (unrounded terminating result, or — impossible-by-resolver-guard — a
     *  non-terminating one) is reduced to its {@link io.github.yhavinga.regelspraak.runtime.Breuk#naarDecimal()} value here:
     *  exact when it terminates, a defensive DECIMAL64 collapse otherwise (never a throw). A non-Breuk passes through. */
    private static Object collapseBreuk(Object v) {
        return v instanceof io.github.yhavinga.regelspraak.runtime.Breuk b ? b.naarDecimal() : v;
    }

    /** Write a TIMELINE doel ({@code List<Segment>}) — the timeline analogue of {@link #werkBij}, holding the two
     *  rules a timeline write obeys that a scalar write does not.
     *
     *  <p>(1) The ch. 7.8 STANDING arm: on a standing temporal target — the SHARED classification
     *  ({@link io.github.yhavinga.regelspraak.transpiler.kern.StaandeDoelen#staandTemporeel}) the DRL-text branch
     *  reads too — an EMPTY result is the temporal leeg and decides nothing, so NOTHING is written and the
     *  case-supplied input timeline stands whole (ref: {@code ref/lib/eval.ml}'s pro-rata and day-count arms).
     *  Emptiness itself is {@link RegelSpraakRuntime#rsTijdreeksGevuld}, the same predicate the DRL text imports.
     *
     *  <p>(2) IDEMPOTENCY: {@link java.util.Objects#equals} — the guard {@link #werkBij} uses — is ALWAYS unequal on
     *  two timelines ({@code Segment} declares no {@code equals}), so a timeline write would re-propagate its subject
     *  on every recompute. A temporal write's pattern is class-reactive whenever its value reads an own attribute
     *  (the §9.9 freshness expr), so two temporal rules over one subject would then re-activate each other past the
     *  engine's fire cap. {@link RegelSpraakRuntime#rsTijdreeksGelijk} compares per segment instead, which reaches
     *  the same fixpoint the DRL back-end reaches through its narrow {@code @watch}: it never UNDER-updates (an
     *  unequal timeline always writes+propagates), so no computed value moves. */
    // package-private, not private: the §5.8.2 rsTijdSomAlle write reaches this helper on a shape NO corpus golden
    // carries (every corpus temporal target is a pro-rata or day-count fold), so the model-level replay cannot
    // witness it — TijdlijnWerkBijWitnessTest pins the helper's behaviour directly instead.
    static void werkBijTijdlijn(org.drools.model.Drools drools, Object s,
            java.util.function.Function<Object, Object> getter, BiConsumer<Object, Object> setter,
            Object nieuw, boolean staandDoel) {
        if (staandDoel && !RegelSpraakRuntime.rsTijdreeksGevuld((List<?>) nieuw)) return;
        if (RegelSpraakRuntime.rsTijdreeksGelijk((List<?>) getter.apply(s), (List<?>) nieuw)) return;
        setter.accept(s, nieuw);
        drools.update(s);
    }

    /** The ch. 7.8 / ch. 7.7 target classification, built on first use — {@link #prioritizedDefaults} (which
     *  its {@code other_writer} clause consults) is threaded in after construction, so the derivation cannot be
     *  built in the constructor. */
    private io.github.yhavinga.regelspraak.transpiler.kern.StaandeDoelen staande() {
        if (staande == null)
            staande = io.github.yhavinga.regelspraak.transpiler.kern.StaandeDoelen.van(model, prioritizedDefaults);
        return staande;
    }

    /** ref's {@code Checks.attr_temporal}: the attribuut carries a declared granulariteit. Delegates to the
     *  SHARED name contract ({@link Namen#bevatNaam}), which the DRL back-end's key sites call too — one
     *  implementation, so the two back-ends can no longer drift apart on what a written key names. */
    private static boolean temporeelAttribuut(Objecttype ot, String attr) {
        return ot != null && Namen.bevatNaam(ot.granulariteitLeden(), attr);
    }

    /** A written attribute name brought back to its DECLARED spelling on {@code ot} — the same shared name
     *  contract ({@link Namen#gedeclareerdeNaam}). Every reflective read below hands the name to the
     *  runtime's pascal-based property resolver, and the bean member was generated from the DECLARATION
     *  ({@link io.github.yhavinga.regelspraak.schema.FactModelGenerator}), so a key written in another
     *  capitalisation must be resolved here or it looks for a getter that does not exist. */
    private static String gedeclareerdAttribuut(Objecttype ot, String attr) {
        return ot == null ? attr : Namen.gedeclareerdeNaam(ot.attribuutLeden().keySet(), attr);
    }

    /** Reflective analogue of {@link #werkBij} for the verdeling / feitcreatie consequences, which read+write a
     *  recipient/relation attribute by NAME via the runtime's property resolver (a multi-word property the typed
     *  Accessors path does not pre-build): set {@code attr} on {@code fact}, then {@code drools.update} ONLY when the
     *  value actually CHANGED. These consequences join through a BLACK-BOX (reflective) lambda that Drools cannot
     *  property-analyse, so the pattern is CLASS-reactive — it re-fires on EVERY update of the fact. With an
     *  unconditional {@code update}, two such rules over the same fact type (e.g. two verdelingen writing different
     *  recipient attributes, empirically a 1e6-fire run-away) ping-pong forever (NO_LOOP blocks only self-retrigger).
     *  Updating only on a real change reaches the same fixpoint the DRL back-end reaches via mvel property reactivity. */
    private static void werkBijReflectief(org.drools.model.Drools drools, Object fact, String attr, Object v) {
        v = collapseBreuk(v);   // §6.1.2 write boundary: collapse a remaining exact rational to its decimal value
        Object oud = RegelSpraakRuntime.get(fact, attr);
        RegelSpraakRuntime.set(fact, attr, v);
        if (!java.util.Objects.equals(oud, v)) drools.update(fact);
    }

    private static boolean isAggregatieWaarde(ResolvedRegel r) {
        return (r.soort() == ResolvedRegel.Soort.GELIJKSTELLING || r.soort() == ResolvedRegel.Soort.BEREKENING
                || r.soort() == ResolvedRegel.Soort.INITIALISATIE)
                && r.waarde() instanceof Expr.Functie fx && isAggregatieKind(fx.kind());
    }
    private static boolean isAggregatieKind(Expr.FunctieKind k) {
        return k == Expr.FunctieKind.SOM || k == Expr.FunctieKind.TOTAAL || k == Expr.FunctieKind.AANTAL
                || k == Expr.FunctieKind.MINIMUM || k == Expr.FunctieKind.MAXIMUM
                || k == Expr.FunctieKind.AANTAL_GEVULDE;
    }
    /** §5.8.3-.4 a LISTED concatenation fold ("de minimale/maximale/eerste/laatste van A en B …") — ≥2 listed scalar
     *  operands, no restriction — handled on the scalar path by {@link #concatenatieFold}, NOT an instance aggregation. */
    private static boolean isGelisteFold(Expr.Functie f) {
        return (f.kind() == Expr.FunctieKind.MINIMUM || f.kind() == Expr.FunctieKind.MAXIMUM
                || f.kind() == Expr.FunctieKind.EERSTE || f.kind() == Expr.FunctieKind.LAATSTE)
                && f.argumenten().size() >= 2 && f.filter() == null;
    }

    /** A lowered aggregation: the accumulate rule item, the result Variable, and (for §5.8.2 SOM) the contributor
     *  count Variable + the empty-sum fallback. */
    private record Agg(RuleItemBuilder<?> accItem, Variable resultVar, Variable countVar, WaardeFn somFallback, boolean isSom) {}

    /** Whether the bean model derives the scalar REFERENCE field for role {@code rol} on {@code ot}, pointing at
     *  {@code doel} — asked of {@link io.github.yhavinga.regelspraak.schema.FactModelGenerator#heeftReferentieVeld},
     *  which reads the ONE member derivation the generator renders and the ContractValidator demands, so a lowered
     *  read can never drift from what the beans carry.
     *  A derived ROLE is not yet a derived FIELD: a to-many role's storage is the far end's field, and where BOTH
     *  ends are to-many there is no field at all. Nor is a derived NAME yet a derived reference: leden()'s
     *  name-dedup lets a declared member of the same name displace it, which is why the TYPE is part of the
     *  question (see that method). */
    private boolean heeftReferentieVeld(Objecttype ot, String rol, Objecttype doel) {
        return io.github.yhavinga.regelspraak.schema.FactModelGenerator.heeftReferentieVeld(model, ot, rol, doel);
    }

    /** The same question on the READ side: whether a hop from {@code ot} over {@code rol} to {@code doel} has a bean
     *  field to read at all, asked by every reader that TOUCHES that field — {@link #navReader}'s GENAVIGEERD case,
     *  {@link #navKetenReader}'s hops, {@link #navComparisonRole}, {@link #navReactiviteit}'s far-side joins, and
     *  the §8.1.7 near-side rolcheck, which does not navigate but null-TESTS the very same field. {@code lezer}
     *  names which one, because the reasons end up side by side in the gate report. On a refusal it records
     *  {@link #navReden}; the caller returns null and the construct's skip carries that reason instead of a scope
     *  note.
     *
     *  <p>Why it must be asked HERE and not left to the getter: a displaced field still EXISTS, holding a
     *  {@code BigDecimal} or a {@code boolean}. A navigated hop then reads a non-null NUMBER where the relation is
     *  absent, the chain's leeg short-circuit does not fire, and the attribute read off that number yields null —
     *  which the arithmetic treats as an unset value rather than as ch. 1.2's absent relation. Measured on
     *  verifier-clean, ref-clean models (2026-09-10): {@code 20% van het tarief van de werf} answered LEEG where
     *  ref and clingo write 0; the same read on the comparison side aborted the fire
     *  ({@code ConstraintEvaluationException}); and the rolcheck's {@code veld != null} read that displacing number
     *  as PROOF OF MEMBERSHIP and fired a rule ref and clingo leave unfired ("verwacht leeg, werkelijk 8"). All
     *  three are now per-form refusals. */
    private boolean heeftVeld(String lezer, Objecttype ot, String rol, Objecttype doel) {
        if (heeftReferentieVeld(ot, rol, doel)) return true;
        navReden = lezer + ": " + geenReferentieveldReden(ot, rol, doel) + " (§13.3.9 opslagrichting)";
        return false;
    }

    /** Why a navigated READ did not lower, when the cause is the §13.3.9 bean-model limit rather than an arm nobody
     *  has built yet — the {@link #aggregatieReden} distinction, on the read side. Set by {@link #navVeld} and RESET
     *  at every construct entry ({@link #meet}), so a reason can only ever be attached to the construct whose own
     *  lowering produced it. It SHARPENS an otherwise generic skip note and never invents one: it is set only where
     *  a hop this construct walks really has no field, which is by itself enough to make the construct not-carried. */
    private String navReden;

    /** Why {@code ot}'s bean carries no reference field for the to-ONE role {@code rol} toward {@code doel} — the
     *  precise §13.3.9 reason, so a per-form refusal names the bean-model limit instead of the generic scope note
     *  (the {@code schendingReden} pattern, and the defaults arm's kenmerktarget SKIP, applied to the reference
     *  fields). The sentence itself lives beside the derivation it explains
     *  ({@link io.github.yhavinga.regelspraak.schema.FactModelGenerator#geenReferentieveldReden}), so this branch
     *  and the DRL-text branch cannot drift into two wordings for one limit. */
    private static String geenReferentieveldReden(Objecttype ot, String rol, Objecttype doel) {
        return io.github.yhavinga.regelspraak.schema.FactModelGenerator.geenReferentieveldReden(ot, rol, doel);
    }

    /** Why the last {@link #aggregatie} call returned null, when the cause is a §13.3.9 bean-MODEL limit rather
     *  than an arm nobody has built yet — the {@code schendingReden} distinction, on the fold side. Set at the two
     *  storage-direction gates and RESET at every entry, so a caller may read it only immediately after a null
     *  return; a null field means the generic Phase-C scope note stands. */
    private String aggregatieReden;

    /** §5.8/§13.4.17 lower an aggregation over the subject's related elements to a {@code DSL.accumulate} CE: the
     *  source pattern joins the element to {@code $subject} (the §9.4 backref role), an optional §13.4.2 die-filter
     *  becomes a source constraint, and the accumulate function (count / BigDecimal sum / min / max) binds the
     *  result. Mirrors DrlTranspiler.accumulate. null -> skip (unresolvable join, unstructured filter, a temporal /
     *  multi-operand fold, or a non-literal SOM fallback) — never a silently-wrong aggregate over EVERY instance. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private Agg aggregatie(Expr.Functie f, Objecttype subject, Variable subjectVar, boolean leegCount) {
        aggregatieReden = null;                      // a stale reason must never be attached to another skip
        // §5.7 / IR-SPEC ch. 4.3 / WF-110: the count over an INSTANCE-SET UNION. Read first and
        // returning, because a union is a whole source of its own: every derivation below reads
        // elementObjecttype and the one-step bronRol, which this node deliberately does not carry.
        if (f.kind() == Expr.FunctieKind.AANTAL && f.argumenten().size() == 1
                && f.argumenten().get(0) instanceof Expr.InstanceSetUnion unie)
            return unieTelling(unie, subject, subjectVar);
        Objecttype elem = f.elementObjecttype();
        if (elem == null) return null;
        Class<?> elemClass = factClass(elem);
        if (elemClass == null) return null;
        boolean count = f.kind() == Expr.FunctieKind.AANTAL;
        // §5.8.3 a multi-operand min/max, or a multi-operand count without a resolved union filter, drops operands -> skip.
        if ((f.kind() == Expr.FunctieKind.MINIMUM || f.kind() == Expr.FunctieKind.MAXIMUM) && f.argumenten().size() > 1) return null;
        if (count && f.argumenten().size() > 1 && f.filter() == null) return null;
        Expr.AttribuutRef arg = !f.argumenten().isEmpty() && f.argumenten().get(0) instanceof Expr.AttribuutRef a ? a : null;
        boolean temporeel = arg != null && arg.objecttype() != null && Namen.bevatNaam(arg.objecttype().granulariteitLeden(), arg.attribuut());
        // §7.1 a SOM/TOTAAL over a temporal attribute of a set is a PER-ELEMENT timeline fold (rsTijdTotaal per element,
        // then sum); MIN/MAX over a temporal attribute is ambiguous -> skip (mirror the DRL marker). ("de som van het
        // totaal van X van alle" attaches the set to the inner totaal -> the outer SOM is element-less -> markers.)
        // A FILLED count lands in the same skip and for a reason the spec now states rather than leaves to the
        // executors: a count has no defined MOMENT at which to ask a timeline whether it is filled
        // (RUNTIME-SEMANTICS ch. 3.4c). Both OCaml executors refuse that form by name; refusing it here keeps the
        // three in step instead of answering from whichever bucket the getter happens to hand over.
        if (temporeel && f.kind() != Expr.FunctieKind.SOM && f.kind() != Expr.FunctieKind.TOTAAL) return null;
        if (!count && arg == null) return null;   // sum/min/max/filled-count need an attribute to fold
        // The VALUE fold below accumulates in BigDecimal, and a member that is not a number has no
        // place in it — it used to CRASH there rather than decline (a ClassCastException on the
        // som/totaal bind, an IllegalStateException out of the min/max accumulate cast). A crash
        // names no form and reaches the gate as an ERROR instead of as a channel with a reason, so
        // the question is asked here, before anything lowers, through the reason BOTH branches share.
        String vw = io.github.yhavinga.regelspraak.transpiler.kern.Vouwwaarde.weigering(f, elem, arg);
        if (vw != null) { aggregatieReden = vw; return null; }

        // IR-SPEC ch. 4.3 / WF-102: a RANGE that is a PATH replaces the whole one-step join derivation below. It is
        // read first and returns, so a document carrying a path never falls through to the by-pair search — that
        // fallback would bind a coincidental DIRECT subject-to-element relation and fold the wrong set, which is
        // the misbinding the field closes. The fold KIND, its leegwaarde and every §4.12/§5.8.2 guard stay exactly
        // where they are: only the SOURCE changes, so there is no MIN-specific (or any other kind-specific) arm in
        // the path code.
        List<RegelSpraakModel.RolBindingRef> pad = bronPaden.get(f);
        if (pad != null) {
            io.github.yhavinga.regelspraak.transpiler.kern.Bronpad.Plan plan =
                    io.github.yhavinga.regelspraak.transpiler.kern.Bronpad.ontleed(model, subject, pad, elem);
            if (plan.weigering() != null) { aggregatieReden = plan.weigering(); return null; }
            PadBron pb = padBron(plan, elem, elemClass, subjectVar);
            if (pb == null) return null;                  // aggregatieReden set by padBron
            return vouw(f, elem, arg, count, temporeel, leegCount, pb.elemVar(), pb.src(), pb.extra());
        }
        // §9.4 the join role binding the element to the subject (a count may name its element role explicitly).
        String rol = null;
        if (subject != null && arg != null && count) rol = model.verbindendeRolViaElementnaam(subject, arg.attribuut());
        if (rol == null && subject != null) rol = model.verbindendeRol(elem, subject);
        if (rol == null) {
            // The wire's symbolic bronRol, LAST — so every join the model already derives stays exactly as it was.
            // The only route for a SELF-TYPED aggregation ("de som van de rang van alle opvolgers van de Wachter"),
            // whose by-pair search counts two same-typed sibling roles and derives null by construction, and for a
            // REPEATED type pair. The derivation AND the to-many-sibling refusal are FactModelGenerator's, shared
            // with the DRL-text branch.
            FactModelGenerator.Bereikjoin bj = FactModelGenerator.bereikjoin(model, rolBindingen.get(f));
            rol = bj.rol();
            if (rol == null && bj.weigering() != null) { aggregatieReden = bj.weigering(); return null; }
        }
        if (rol == null) return null;
        final String backref = rol;
        // §13.3.9 STORAGE DIRECTION. ref reads an aggregation's set as the SUBJECT's own links under the ELEMENT's
        // role key (role_links self elem_role); the bean model stores that same set on whichever end is the ONE
        // side (FactModelGenerator.leden). So:
        //   element role TO-MANY  -> the field lives on each element, named after the subject's role: the backref
        //                            join below, unchanged.
        //   element role TO-ONE   -> the relation's field lives on the SUBJECT, named after the element's role, and
        //                            the join must read THAT. The backref join would probe the element's own
        //                            reference — a DIFFERENT field, filled only when the case (or a creation)
        //                            happened to supply the link from the element's side — and fold the wrong set.
        // A to-one element role is only FAITHFUL, though, when its sibling is to-one as well: then each direction
        // owns a field and a one-sided supply stays one-sided, exactly as ref stores it. With a TO-MANY sibling the
        // relation has exactly ONE field, which necessarily serves both directions, so a link supplied from the far
        // end reads here as this end's own. That shape is refused, not silently counted.
        //
        // WHY THE REFUSAL STILL STANDS — and on which ground (updated 2026-09-10). It used to rest on ref reading
        // the same link as NOTHING: measured on jutterswet-vondsttelling (element role `vondst` to-many, the single
        // field Vondst.aanmelder), a case supplying from the ELEMENT end (V1{aanmelder:[J1]}, V2{aanmelder:[J1]})
        // read ref 0 / 0 against Drools 2 / 3.75 — a divergence with no corpus witness, since every golden supplies
        // such a relation from the ONE side where the two agreed.
        // RUNTIME-SEMANTICS ch. 5.7a WITHDREW that ground: a supplied link establishes the relation and both roles
        // read it, so ref now answers 2 / 3.75 on that very case (remeasured) — the divergence is gone, and the
        // backend's numbers were the right ones all along. What remains is therefore NOT a correctness claim but a
        // CONSERVATIVE one: this arm keeps refusing, because deciding it is now expressible is a measured change
        // with gate-12 channel consequences and belongs to its own card, not to a comment.
        // The sister arm above (to-many backref) never refused and needs no revisiting: it computed the reading
        // ch. 5.7a decided.
        // NB the by-NAME lookup only for a COUNT: there arg.attribuut() IS the written element role (the same
        // reading verbindendeRolViaElementnaam applies above); for a sum/min/max it is an ATTRIBUTE name, which
        // must never be matched against role names.
        String elemRol = count && arg != null ? model.verbindendeRolViaNaam(subject, elem, arg.attribuut()) : null;
        if (elemRol == null) elemRol = model.verbindendeRol(subject, elem);
        if (elemRol == null) {
            RegelSpraakModel.Rol[] paar = FactModelGenerator.gebondenRolpaar(model, rolBindingen.get(f));
            if (paar != null) elemRol = paar[0].naam();
        }
        boolean voorwaarts = elemRol != null && !model.naarVeelRol(subject, elem, elemRol);
        if (voorwaarts && model.naarVeelRol(elem, subject, backref)) {              // one shared field, both directions
            aggregatieReden = "aggregatie over de enkelvoudige elementrol '" + elemRol + "' wiens zusterrol '"
                    + backref + "' meervoudig is: dat feittype heeft ÉÉN beanveld en dat draagt noodzakelijk BEIDE"
                    + " richtingen. Deze weigering staat CONSERVATIEF: haar oorspronkelijke grond — de gerichte"
                    + " lezing van de case-invoer — is per RUNTIME-SEMANTICS ch. 5.7a ingetrokken, en of ze"
                    + " daarmee overbodig is geworden vergt een eigen, gemeten kaart (het opheffen verschuift"
                    + " gate-12-kanalen) (§13.3.9 opslagrichting)";
            return null;
        }
        // ...and the join field must actually EXIST. Both arms above derive a ROLE, which is not the same as a bean
        // MEMBER: the generator derives a scalar reference only where the role is to-one AND uniquely reachable
        // (FactModelGenerator.leden — the by-pair loop, plus zelfpaarRollen for a self-typed pair) AND its name is
        // not taken by a declared member (leden()'s dedup displaces the reference — hence the TYPED question). A
        // self-typed relation whose BOTH roles are to-many (jutterswet-erflijn's `erflijn`: erflater and erfvolger
        // both meervoudig) therefore has NO field at either end, and lowering its join produced a rule that built
        // fine and died at first fire ("geen getter voor 'erflater'") — an ERROR gate 12 rightly refuses to excuse.
        // Asked of the ONE shared member derivation, so this can never drift from what the beans carry.
        Objecttype joinDrager = voorwaarts ? subject : elem, joinDoel = voorwaarts ? elem : subject;
        String joinRol = voorwaarts ? elemRol : backref;
        if (!heeftReferentieVeld(joinDrager, joinRol, joinDoel)) {
            // the cardinality half moved to FactModelGenerator.geenJoinveldReden (2026-09-18) so the DRL-text
            // branch diagnoses this same failure in the same words; the sentence itself is unchanged.
            aggregatieReden = "de aggregatie-join heeft geen beanveld — "
                    + io.github.yhavinga.regelspraak.schema.FactModelGenerator
                        .geenJoinveldReden(model, joinDrager, joinRol, joinDoel)
                    + " (§13.3.9 opslagrichting)";
            return null;
        }
        final String fwdVeld = elemRol;

        Variable elemVar = DSL.declarationOf(elemClass, "$e_" + (seq++));
        PatternDSL.PatternDef src = PatternDSL.pattern(elemVar).expr(nextId("join"), (Variable) subjectVar,
                voorwaarts ? (Predicate2) (e, subj) -> RegelSpraakRuntime.get(subj, fwdVeld) == e
                           : (Predicate2) (e, subj) -> RegelSpraakRuntime.get(e, backref) == subj);
        return vouw(f, elem, arg, count, temporeel, leegCount, elemVar, src, List.of());
    }

    /** §5.7 / IR-SPEC ch. 4.3 / WF-110 — the instance count over a UNION of selections, semantics
     *  RUNTIME-SEMANTICS ch. 3.4d. One accumulate over ONE source pattern whose constraint is the
     *  DISJUNCTION of the selections, each selection being its own join AND its own die/dat filter.
     *
     *  <p><b>One pattern is what deduplicates, and it is the whole design.</b> A source pattern
     *  matches an element once whatever makes it match, so an instance two selections both select
     *  contributes ONE tuple and the count answers |A ∪ B| — the operation ch. 3.4d defines, against
     *  the sum of the parts. Neither obvious alternative has that property: {@code DSL.or} over two
     *  patterns binding the same variable produces one match PER satisfied branch, and two
     *  accumulates added together count the shared element twice. That is why every selection is
     *  lowered to a composable {@link Predicate2}/{@link Predicate1} pair here rather than through
     *  {@link #filterSource}, which ANDs its constraint onto the pattern and cannot be OR-ed.
     *
     *  <p>null -> skip, with {@link #aggregatieReden} naming the form, never a silently wrong count. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private Agg unieTelling(Expr.InstanceSetUnion u, Objecttype subject, Variable subjectVar) {
        if (subject == null || u.takken().isEmpty()) return null;
        Objecttype elem = u.takken().get(0).elementType();
        for (Expr.InstanceSetUnion.Tak t : u.takken())
            if (t.elementType() != elem) {          // WF-110 (d), re-asked here: one accumulate has one pattern class
                aggregatieReden = "instantie-unie over twee populaties ('" + (elem == null ? "?" : elem.naam)
                        + "' en '" + (t.elementType() == null ? "?" : t.elementType().naam) + "'): één"
                        + " telling antwoordt een aantal over ÉÉN gedeclareerd type (WF-110 (d))";
                return null;
            }
        Class<?> elemClass = elem == null ? null : factClass(elem);
        if (elemClass == null) return null;
        List<Predicate2<Object, Object>> selecties = new ArrayList<>();
        for (Expr.InstanceSetUnion.Tak t : u.takken()) {
            Predicate2<Object, Object> join = unieJoin(t.bronRol(), subject, elem);
            if (join == null) return null;          // aggregatieReden set by unieJoin
            Predicate1<Object> filter = null;
            if (t.filter() != null) {
                filter = t.filter() instanceof Conditie.Samengesteld sg ? leafCompound(sg, elem)
                        : leafPredicate(t.filter(), elem);
                if (filter == null) {
                    aggregatieReden = "de deelselectie van een instantie-unie-selectie over '" + elem.naam
                            + "' is geen feit-only predicaat; een weggelaten filter zou te veel tellen en"
                            + " de unie zou dat opslokken (§13.4.2)";
                    return null;
                }
            }
            final Predicate1<Object> f = filter;
            final Predicate2<Object, Object> j = join;
            selecties.add((e, subj) -> j.test(e, subj) && (f == null || f.test(e)));
        }
        final List<Predicate2<Object, Object>> takken = List.copyOf(selecties);
        Variable elemVar = DSL.declarationOf(elemClass, "$e_" + (seq++));
        PatternDSL.PatternDef src = PatternDSL.pattern(elemVar).expr(nextId("unie"), (Variable) subjectVar,
                (Predicate2) (e, subj) -> {
                    for (Predicate2<Object, Object> p : takken) if (p.test(e, subj)) return true;
                    return false;
                });
        Variable resultVar = DSL.declarationOf(Object.class, "$acc_" + (seq++));
        return new Agg(DSL.accumulate(accBron(src, List.of()),
                        DSL.accFunction(accFn("CountAccumulateFunction"), elemVar).as(resultVar)),
                resultVar, null, null, false);
    }

    /** The join of ONE union selection: does {@code e} stand in the selection's relation to
     *  {@code subj}? Derived from the wire binding ALONE — never from the type pair — which is
     *  WF-110 (e)'s ground read as code: every selection of one union shares one pair, so a by-pair
     *  search would give them all the same range and the union would restate one of them. The
     *  §13.3.9 storage-direction question and its two refusals are the fold's, asked here at the new
     *  position through the SAME shared derivations, so both branches keep one wording per limit. */
    private Predicate2<Object, Object> unieJoin(RegelSpraakModel.RolBindingRef binding, Objecttype subject,
            Objecttype elem) {
        FactModelGenerator.Bereikjoin bj = FactModelGenerator.bereikjoin(model, binding);
        String backref = bj.rol();
        if (backref == null) {
            aggregatieReden = bj.weigering() != null ? bj.weigering()
                    : "de relatie van een instantie-unie-selectie is niet af te leiden uit haar binding (WF-110 (e))";
            return null;
        }
        RegelSpraakModel.Rol[] paar = FactModelGenerator.gebondenRolpaar(model, binding);
        String elemRol = paar != null ? paar[0].naam() : null;
        if (elemRol == null) return null;
        boolean voorwaarts = !model.naarVeelRol(subject, elem, elemRol);
        if (voorwaarts && model.naarVeelRol(elem, subject, backref)) {
            aggregatieReden = "instantie-unie over de enkelvoudige elementrol '" + elemRol + "' wiens zusterrol '"
                    + backref + "' meervoudig is: dat feittype heeft ÉÉN beanveld en dat draagt noodzakelijk"
                    + " BEIDE richtingen (§13.3.9 opslagrichting)";
            return null;
        }
        Objecttype joinDrager = voorwaarts ? subject : elem, joinDoel = voorwaarts ? elem : subject;
        String joinRol = voorwaarts ? elemRol : backref;
        if (!heeftReferentieVeld(joinDrager, joinRol, joinDoel)) {
            aggregatieReden = "de join van een instantie-unie-selectie heeft geen beanveld — "
                    + io.github.yhavinga.regelspraak.schema.FactModelGenerator
                        .geenJoinveldReden(model, joinDrager, joinRol, joinDoel)
                    + " (§13.3.9 opslagrichting)";
            return null;
        }
        final String veld = voorwaarts ? elemRol : backref;
        return voorwaarts ? (e, subj) -> RegelSpraakRuntime.get(subj, veld) == e
                          : (e, subj) -> RegelSpraakRuntime.get(e, veld) == subj;
    }

    /** <b>Every count carries the ELEMENT VARIABLE</b>, which is not decoration. A source-LESS count is legal
     *  over a plain pattern and the fold used to write one, but over a SUBNETWORK — which is what a WF-102 path
     *  with an existential intermediate builds — the model compiler locates the accumulate's source pattern
     *  through the FIRST function's source variable, and a function without one leaves every binding unresolved:
     *  measured, a {@code SubnetworkTuple} arrives where a {@code BigDecimal} is expected and the fire dies.
     *  Naming the element is semantics-free over a plain pattern (the function counts matches and never reads
     *  the value), so both source shapes take the same lowering rather than two.
     *
     *  <p>The fold itself, over a source pattern whose JOIN is already decided — the one-step backref/forward join of
     *  {@link #aggregatie} or the WF-102 path of {@link #padBron}. Everything kind-specific lives HERE and nothing
     *  range-specific does: §13.4.2's die-filter, §5.8.2's empty-SOM count+fallback, §4.12's {@code != null} source
     *  guard, §7.1's per-element timeline collapse and §5.8.3's leegCount companion read the same {@code src}
     *  whichever range produced it. {@code extra} carries the path's EXISTENTIAL intermediates (empty for every
     *  other source): they join the accumulate SOURCE, not the fold, which is what makes one element reached along
     *  two routes contribute once. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private Agg vouw(Expr.Functie f, Objecttype elem, Expr.AttribuutRef arg, boolean count, boolean temporeel,
            boolean leegCount, Variable elemVar, PatternDSL.PatternDef src, List<PatternDSL.PatternDef> extra) {
        if (f.filter() != null) {                       // §13.4.2 die/dat <predicaat> on the element
            src = filterSource(src, f.filter(), elem);
            if (src == null) return null;               // unstructured / unbindable filter -> skip (mirror the DRL marker)
        }

        Variable resultVar = DSL.declarationOf(Object.class, "$acc_" + (seq++));
        if (count)
            return new Agg(DSL.accumulate(accBron(src, extra),
                            DSL.accFunction(accFn("CountAccumulateFunction"), elemVar).as(resultVar)),
                    resultVar, null, null, false);

        final String attr = arg.attribuut();
        // IR-SPEC ch. 4.3 / RUNTIME-SEMANTICS ch. 3.4c — the FILLED-VALUE count: the same CountAccumulateFunction
        // the instance count uses, over a source narrowed to the members whose value EXISTS. The `!= null` guard is
        // the whole difference between the two kinds, and it is the same guard the sum/min/max folds below already
        // place for §4.12 — so this arm adds a kind, not a mechanism.
        // It counts the ELEMENT VARIABLE and never binds the value: over a WF-102 path the intermediates ride the
        // accumulate source as an `exists`, so an element reached along two routes yields ONE tuple and contributes
        // once (the flat-union reading of ch. 3.4b, pinned by jutterswet-gevuldepad's shared-bergplaats case).
        // An EMPTY source is 0 and not leeg: a count accumulate always binds a row, which is exactly the answer
        // ch. 1.2e demands and the reason no countVar/fallback pair is needed here as it is for the SOM.
        if (f.kind() == Expr.FunctieKind.AANTAL_GEVULDE) {
            PatternDSL.PatternDef gevuld =
                    src.expr(nextId("nn"), (Predicate1) (e) -> RegelSpraakRuntime.get(e, attr) != null);
            return new Agg(DSL.accumulate(accBron(gevuld, extra),
                            DSL.accFunction(accFn("CountAccumulateFunction"), elemVar).as(resultVar)),
                    resultVar, null, null, false);
        }
        // §7.1 per-element timeline fold: a temporal element value is summed over time (rsTijdTotaal at the attribute's
        // granularity) to a scalar BEFORE the set fold, instead of the bare List<Segment> getter.
        final String tijdGran = temporeel ? Namen.opNaam(arg.objecttype().granulariteitLeden(), attr).name() : null;
        final Function1<Object, Object> elemWaarde = temporeel
                ? (e) -> RegelSpraakRuntime.rsTijdTotaal((List<?>) RegelSpraakRuntime.get(e, attr), tijdGran)
                : (e) -> RegelSpraakRuntime.get(e, attr);
        // The fold always uses the BigDecimal accumulate variant: a Numeriek attribute maps to a BigDecimal getter
        // (the §6 exact-precision invariant), and BigDecimalSum/Min/Max skip nulls — matching what the DRL `sum`/
        // `min`/`max` keyword resolves to for a BigDecimal arg. (Seam, adversary-noted: if a future objecttype ever
        // lowered a Numeriek to a Double/Integer getter, DRL's keyword would fall back to the generic null-NPEing
        // Double sum while this stays BigDecimal — re-introduce an `attr != null` source guard if that invariant breaks.)
        Variable amtVar = DSL.declarationOf(BigDecimal.class, "$amt_" + (seq++));
        if (f.kind() == Expr.FunctieKind.SOM) {
            // §5.8.2: empty SOM -> the leegwaarde (null, or a literal "of N als die er niet zijn"), not 0. Filter to
            // non-empty contributors, count them, and sum; the consequence branches on the count.
            // §5.8.2 "of <expr> als die er niet zijn": the fallback may be a numeric literal OR a fact-local
            // expression (an own attribute / arithmetic) read from the subject — but not one reading a global.
            WaardeFn fallback = null;
            if (f.leegwaarde() != null) {
                fallback = scalarWaarde(null, f.leegwaarde());
                if (fallback == null || !fallback.globals().isEmpty()) return null;   // unlowerable / global fallback -> skip
            }
            PatternDSL.PatternDef src2 = src.expr(nextId("nn"), (Predicate1) (e) -> RegelSpraakRuntime.get(e, attr) != null)
                    .bind(amtVar, (Function1) elemWaarde);
            Variable cntVar = DSL.declarationOf(Object.class, "$cnt_" + (seq++));
            RuleItemBuilder acc = DSL.accumulate(accBron(src2, extra),
                    DSL.accFunction(accFn("CountAccumulateFunction"), elemVar).as(cntVar),
                    DSL.accFunction(accFn("BigDecimalSumAccumulateFunction"), amtVar).as(resultVar));
            return new Agg(acc, resultVar, cntVar, fallback, true);
        }
        // TOTAAL (empty -> 0) / MIN / MAX: bind the per-element value and fold it.
        Class<?> accClass = switch (f.kind()) {
            case TOTAAL  -> accFn("BigDecimalSumAccumulateFunction");
            case MINIMUM -> accFn("MinAccumulateFunction");
            case MAXIMUM -> accFn("MaxAccumulateFunction");
            default      -> null;
        };
        if (accClass == null) return null;
        // §4.12 a leegwaarde (null) contributor is not counted — AND (crucially for a source DERIVED in the same
        // fireAllRules pass) an element must not enter the accumulate window until its value exists: an element joined
        // with a null value, then modify-fired once another rule of the cycle derives it, triggers a phreak REVERSE of
        // the null prior contribution, which crashes ("Reversing a not existing accumulated object"). The `!= null`
        // source guard makes that transition an INSERT (null -> present) instead of a reverse. (SOM already guards; this
        // extends it to TOTAAL/MIN/MAX. A temporal per-element fold reads a List<Segment> getter, never null here.)
        PatternDSL.PatternDef src2 = (temporeel ? src : src.expr(nextId("nn"), (Predicate1) (e) -> RegelSpraakRuntime.get(e, attr) != null))
                .bind(amtVar, (Function1) elemWaarde);
        if (leegCount && (f.kind() == Expr.FunctieKind.MINIMUM || f.kind() == Expr.FunctieKind.MAXIMUM)) {
            // §5.8.3 an empty MIN/MAX is LEEG (§5.3 Tabel 37 / §5.6 Tabel 44), but MinAccumulateFunction/MaxAccumulateFunction
            // bind NO result row over an EMPTY source, so a condition compare would never run (the rule silently reads onwaar).
            // In a condition context bind count too (a row always exists); aggregatieVoorwaarde then treats it like a SOM
            // (count==0 -> a null aggregate, no fallback) and rsAggVergelijk applies the leegwaarde cells. The VALUE side
            // (leegCount=false) keeps the bare fold -> an empty value-aggregation simply does not fire (unchanged).
            Variable cntVar = DSL.declarationOf(Object.class, "$cnt_" + (seq++));
            RuleItemBuilder acc = DSL.accumulate(accBron(src2, extra),
                    DSL.accFunction(accFn("CountAccumulateFunction"), elemVar).as(cntVar),
                    DSL.accFunction(accClass, amtVar).as(resultVar));
            return new Agg(acc, resultVar, cntVar, null, false);   // countVar set, no fallback, not SOM
        }
        return new Agg(DSL.accumulate(accBron(src2, extra), DSL.accFunction(accClass, amtVar).as(resultVar)), resultVar, null, null, false);
    }

    /** The accumulate SOURCE: the element pattern alone, or — for a WF-102 path whose walk alternates — that
     *  pattern AND an {@code exists} over the intermediates. The {@code exists} is what makes the flat-union
     *  reading true in this branch: joined into the source instead, two intermediates reaching one element would
     *  produce two tuples and the element would contribute twice. */
    private org.drools.model.view.ViewItem<?> accBron(PatternDSL.PatternDef src, List<PatternDSL.PatternDef> extra) {
        if (extra.isEmpty()) return src;
        org.drools.model.view.ViewItemBuilder<?>[] rest =
                extra.subList(1, extra.size()).toArray(new org.drools.model.view.ViewItemBuilder<?>[0]);
        return DSL.and(src, DSL.exists(extra.get(0), rest));
    }

    /** A WF-102 path lowered to an accumulate source: the element pattern plus whatever the walk needs. */
    private record PadBron(Variable elemVar, PatternDSL.PatternDef src, List<PatternDSL.PatternDef> extra) {}

    /** Apply a chain of to-one reference fields, leeg-safely: a broken link anywhere reads as null rather than
     *  faulting, so an intermediate that reaches nothing simply drops out of the union (ch. 3.4b). */
    private static Object wandel(Object start, List<String> velden) {
        Object cur = start;
        for (String v : velden) { if (cur == null) return null; cur = RegelSpraakRuntime.get(cur, v); }
        return cur;
    }

    /**
     * IR-SPEC ch. 4.3 / WF-102 — the fold-source RANGE as a path, lowered to an accumulate source.
     *
     * <p>Each hop reads the §13.3.9 bean field {@link io.github.yhavinga.regelspraak.transpiler.kern.Bronpad}
     * derived for it, and the shape of the lowering follows from WHICH end stores it. A backward hop is functional
     * FROM the element (one scalar getter), a forward hop functional FROM the subject, so:
     *
     * <ul>
     *   <li>a path that is a forward run followed by a backward run is ONE constraint on the element pattern —
     *       walk down from {@code $subject}, walk up from the element, and demand the two meet. No intermediate is
     *       bound, no element is reachable twice, and the accumulate source stays a single pattern;</li>
     *   <li>otherwise a BACKWARD hop is followed by a FORWARD one, and there the intermediate genuinely matters:
     *       two intermediates may reach the SAME element. Each such node becomes a pattern inside one
     *       {@code exists}, which is precisely the dedup the flat union asks for — the element pattern remains the
     *       only tuple generator, so the element contributes once however many routes reach it.</li>
     * </ul>
     *
     * <p>Every constraint binds at most two variables and is attached to the LATER-bound of them, so the
     * intermediates may be declared in path order. null -> skip, with {@link #aggregatieReden} set.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private PadBron padBron(io.github.yhavinga.regelspraak.transpiler.kern.Bronpad.Plan plan, Objecttype elem,
            Class<?> elemClass, Variable subjectVar) {
        Variable elemVar = DSL.declarationOf(elemClass, "$e_" + (seq++));
        if (plan.ontmoet()) {
            int m = plan.ontmoeting();
            final List<String> vanafElement = plan.ketenVanafElement(m);
            final List<String> vanafOnderwerp = plan.ketenVanafOnderwerp(m);
            PatternDSL.PatternDef src = PatternDSL.pattern(elemVar).expr(nextId("pad"), (Variable) subjectVar,
                    (Predicate2) (e, subj) -> {
                        Object a = wandel(e, vanafElement), b = wandel(subj, vanafOnderwerp);
                        return a != null && a == b;
                    });
            return new PadBron(elemVar, src, List.of());
        }
        // the alternating middle: one existential pattern per node neither walk determines
        java.util.Map<Integer, Variable> tussen = new java.util.LinkedHashMap<>();
        for (int i : plan.tussenknopen()) {
            Class<?> c = factClass(plan.knoopTypen().get(i));
            if (c == null) {
                aggregatieReden = "bronPad-tussentype '" + plan.knoopTypen().get(i).naam
                        + "' heeft geen factklasse (§13.3.9 opslagrichting)";
                return null;
            }
            tussen.put(i, DSL.declarationOf(c, "$pm_" + (seq++)));
        }
        List<PatternDSL.PatternDef> extra = new ArrayList<>();
        int laatsteTussen = plan.achterwaartsVanaf() - 1;
        for (java.util.Map.Entry<Integer, Variable> e : tussen.entrySet()) {
            int i = e.getKey();
            PatternDSL.PatternDef p = PatternDSL.pattern(e.getValue());
            p = koppel(p, plan, i, i, tussen, elemVar, subjectVar);                   // the hop INTO this node
            if (i == laatsteTussen) p = koppel(p, plan, plan.achterwaartsVanaf(), i, tussen, elemVar, subjectVar);
            extra.add(p);
        }
        return new PadBron(elemVar, PatternDSL.pattern(elemVar), extra);
    }

    /** Constrain {@code p} — the pattern of node {@code dit} — with hop {@code stap} (which connects nodes
     *  {@code stap-1} and {@code stap}), reading the OTHER end through whichever root determines it: the subject
     *  (a forward prefix), the element (a backward suffix) or an already-declared intermediate. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private PatternDSL.PatternDef koppel(PatternDSL.PatternDef p,
            io.github.yhavinga.regelspraak.transpiler.kern.Bronpad.Plan plan, int stap, int dit,
            java.util.Map<Integer, Variable> tussen, Variable elemVar, Variable subjectVar) {
        int ander = stap - 1 == dit ? stap : stap - 1;
        final boolean ditIsDoel = ander == stap - 1;          // this node is the hop's TARGET, the other its source
        final List<String> anderKeten = ander <= plan.voorwaarts() ? plan.ketenVanafOnderwerp(ander)
                : ander >= plan.achterwaartsVanaf() ? plan.ketenVanafElement(ander) : List.of();
        Variable anderVar = ander <= plan.voorwaarts() ? subjectVar
                : ander >= plan.achterwaartsVanaf() ? elemVar : tussen.get(ander);
        final String veld = plan.stappen().get(stap - 1).veld();
        final boolean achterwaarts = plan.stappen().get(stap - 1).achterwaarts();
        return p.expr(nextId("pad"), anderVar, (Predicate2) (t, o) -> {
            Object x = wandel(o, anderKeten);
            if (x == null || t == null) return false;
            Object bron = ditIsDoel ? x : t, doel = ditIsDoel ? t : x;
            return achterwaarts ? RegelSpraakRuntime.get(doel, veld) == bron
                                : RegelSpraakRuntime.get(bron, veld) == doel;
        });
    }

    /** A §5.8.2 to-many temporal sommatie: the collectList accumulate CE (binding the element {@code List<Segment>}
     *  timelines into {@code tijdlijnenVar}) and the §5.8.2 "of 0 als die er niet zijn" flag. */
    private record SomAlleSet(RuleItemBuilder<?> accItem, Variable tijdlijnenVar, boolean fallbackNul) {}

    /** §5.8.2/§5.1.4 "de som van &lt;temporeel attr&gt; van alle &lt;naar-veel-rol&gt;" into a TEMPORAL doel — a per-period
     *  sommatie over the navigated SET. Builds a {@code CollectListAccumulateFunction} accumulate binding a
     *  {@code List<List<Segment>>} of the element timelines (each restated §4.1 into the doel unit), which the consequence
     *  folds via {@link RegelSpraakRuntime#rsTijdSomAlle}. null when it is not such a set fold (SOM over a to-many
     *  single-period temporal numeric attribute, resolvable join, structured die-filter, unit-convertible). Mirrors
     *  DrlTranspiler.tijdreeksSomAlleSet. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private SomAlleSet tijdreeksSomAlleSet(Expr w, Objecttype subject, Variable subjectVar, Datatype doelType,
            RegelSpraakModel.Granulariteit doelGran) {
        if (!(w instanceof Expr.Functie f) || f.kind() != Expr.FunctieKind.SOM || f.argumenten().size() != 1) return null;
        if (!(f.argumenten().get(0) instanceof Expr.AttribuutRef a) || !a.set() || a.objecttype() == null) return null;
        RegelSpraakModel.Granulariteit eg = Namen.opNaam(a.objecttype().granulariteitLeden(), a.attribuut());
        if (eg == null || Tijdlijnen.meerPeriodiek(a.objecttype().tijdlijnAantalLeden(), a.attribuut())) return null;   // non/multi-period -> not this fold
        if (eg != doelGran) return null;   // §5.1.4 element tijdlijn must match the doel's (no set-fold gran promotion yet) — mirror DRL
        if (a.type() == null || !a.type().isNumeriek()) return null;
        Objecttype elem = f.elementObjecttype();
        if (elem == null || subject == null) return null;
        Class<?> elemClass = factClass(elem);
        if (elemClass == null) return null;
        String rol = model.verbindendeRol(elem, subject);
        if (rol == null) {
            // ch. 4.3, the same LAST arm the instance fold takes, from the same shared derivation — see the
            // DRL-text twin for the 2026-09-19 measurement that put it here.
            FactModelGenerator.Bereikjoin bj = FactModelGenerator.bereikjoin(model, rolBindingen.get(f));
            rol = bj.rol();
            if (rol == null) {
                if (bj.weigering() != null) navReden = bj.weigering();
                return null;
            }
        }
        // §13.3.9: this fold has its OWN join, resolved apart from the one {@link #aggregatie} gates, so it must ask
        // the shared question apart too. Measured 2026-09-11 on a verifier-clean model whose element role is to-many
        // (so the bean field lives at the far end and this end derives none): ERROR ConstraintEvaluationException at
        // first fire in this branch, a bare "Unable to Analyse Expression bewarendeJutter == $subject" in the DRL
        // one. The shape reaches no differential either way — ref and clingo refuse a TEMPORAL target outright
        // ("temporal writes are not implemented") — so, as with the far-side rolcheck, the gate buys the diagnosis.
        if (!heeftVeld("temporele set-sommatie", elem, rol, subject)) return null;
        // §4.1 the per-element unit restatement factor (equal unit -> no scale; incompatible -> not this fold).
        Datatype aType = effType(a);
        Eenheid eu = aType.eenheidOf(), du = doelType == null ? null : doelType.eenheidOf();
        BigDecimal factor = null; boolean deel = false;
        if (!java.util.Objects.equals(eu, du)) {
            if (eu == null || du == null || !eu.convertibleNaar(du)) return null;
            if (eu.vereistOmrekening(du)) {
                BigDecimal fwd = valueFactor(aType, doelType), inv = valueFactor(doelType, aType);
                deel = inv != null && inv.precision() < fwd.precision();
                factor = deel ? inv : fwd;
            }
        }
        final String backref = rol, attr = a.attribuut();
        final BigDecimal fFactor = factor; final boolean fDeel = deel;
        Variable elemVar = DSL.declarationOf(elemClass, "$se_" + (seq++));
        PatternDSL.PatternDef src = PatternDSL.pattern(elemVar).expr(nextId("join"), (Variable) subjectVar,
                (Predicate2) (e, subj) -> RegelSpraakRuntime.get(e, backref) == subj);
        if (f.filter() != null) {                       // §13.4.2 die/dat <predicaat> on the element
            src = filterSource(src, f.filter(), elem);
            if (src == null) return null;
        }
        // the accumulate function reads each element's timeline (restated to the doel unit) — collectList gathers them.
        Function1<Object, Object> elemTijdlijn = fFactor == null
                ? (e) -> RegelSpraakRuntime.get(e, attr)
                : (e) -> RegelSpraakRuntime.rsTijdSchaal((List<?>) RegelSpraakRuntime.get(e, attr), fFactor, fDeel);
        Variable elemTlVar = DSL.declarationOf(List.class, "$setl_" + (seq++));   // per-element timeline (accumulate source)
        Variable tlVar = DSL.declarationOf(List.class, "$stl_" + (seq++));        // collected List<List<Segment>> (accumulate result)
        PatternDSL.PatternDef src2 = src.bind(elemTlVar, (Function1) elemTijdlijn);
        RuleItemBuilder acc = DSL.accumulate(src2, DSL.accFunction(accFn("CollectListAccumulateFunction"), elemTlVar).as(tlVar));
        return new SomAlleSet(acc, tlVar, f.leegwaarde() != null);
    }

    /** §5.8/§13.4.14 an AGGREGATION (count / sum / totaal / min / max) as one side of a condition comparison ("het aantal
     *  / de som van / de hoogste ... van alle &lt;elem&gt; van &lt;subject&gt; &lt;op&gt; N") -> a {@link Gate} whose
     *  {@code extra} is the accumulate CE (from {@link #aggregatie}) PLUS a constraint pattern on its result var (the
     *  agg value compared to the literal N via {@link RegelSpraakRuntime#rsAggVergelijk}, a leeg fold reading onwaar).
     *  A SOM binds the count var too for the §5.8.2 empty-sum leegwaarde. The exec mirror of the DRL accumulate + eval.
     *  Scoped to a literal / own-attribute / numeric-scalar-global threshold and any comparison operator (ordering, GELIJK,
     *  ONGELIJK); null otherwise (a navigated / multi- or non-numeric-global RHS, agg-vs-agg -> caller falls through). */
    private Gate aggregatieVoorwaarde(Conditie.Vergelijking v, PatternDSL.PatternDef pattern, Objecttype subject, Variable subjectVar) {
        return aggregatieVoorwaarde(v, pattern, subject, subjectVar, false);
    }
    /** {@code negeer} negates the boolean RESULT of the comparison (not the operator) — for a single aggregation bullet
     *  under a GEEN quantifier. Result-negation is leegwaarde-correct (NOT(SOM(leeg) > 10)=WAAR); operator-flip would
     *  read onwaar over an empty SOM. Mirrors the DRL eval-level {@code !( … )}. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private Gate aggregatieVoorwaarde(Conditie.Vergelijking v, PatternDSL.PatternDef pattern, Objecttype subject, Variable subjectVar, boolean negeer) {
        if (subject == null || subjectVar == null) return null;
        final boolean neg = negeer;
        boolean linksAgg = v.links() instanceof Expr.Functie fl && isAggregatieKind(fl.kind());
        boolean rechtsAgg = v.rechts() instanceof Expr.Functie fr && isAggregatieKind(fr.kind());
        if (!linksAgg && !rechtsAgg) return null;               // no aggregation operand
        if (linksAgg && rechtsAgg) return aggVsAgg(v, pattern, subject, subjectVar, neg);   // §5.8 agg-vs-agg
        Expr.Functie f = (Expr.Functie) (linksAgg ? v.links() : v.rechts());
        Expr ander = linksAgg ? v.rechts() : v.links();
        // scope: a LITERAL, an OWN (non-navigated) attribute, a numeric scalar Parameter-GLOBAL, a numeric to-ONE
        // NAVIGATED attribute threshold, or a numeric arithmetic EXPRESSION reading 2 DISTINCT scalar numeric globals
        // ("... groter is dan global A plus global B"). The own-attr containsKey clause is load-bearing: a navigation
        // the resolver could NOT encode ("de afstand van de locatie van de houder") can surface as an AttribuutRef
        // with navRol==null and objecttype==subject yet a name NOT on the subject — scalarWaarde would build a reader for
        // a non-existent getter that THROWS at fireAllRules (review wf_900d3055-a62 F2). Only a PROPER navigation (navRol
        // != null) is admitted, read null-safely via navReader (absent relation -> null; to-many/unresolvable -> navReader
        // null -> marker). The global/navigated attr must be numeric (isNumeriek/isPercentage) and non-temporal; a global
        // is scalar. The multi-global EXPRESSION arm is admitted below (aggMultiGlobalRhs), keyed on scalarWaarde's globals.
        boolean multiGlobalRhs = !(ander instanceof Expr.AttribuutRef) && !(ander instanceof Expr.Literal)
                && !(ander instanceof Expr.ParameterRef) && aggRhsGlobalCount(subject, ander) >= 2;
        if (!(ander instanceof Expr.Literal)
                && !(ander instanceof Expr.AttribuutRef ar && ongenavigeerd(ar) && ar.objecttype() == subject
                     && subject.attribuutLeden().containsKey(ar.attribuut()))
                && !(ander instanceof Expr.ParameterRef pr && pr.parameter().granulariteit() == null
                     && (pr.parameter().datatype().isNumeriek() || pr.parameter().datatype().isPercentage()))
                && !(ander instanceof Expr.AttribuutRef nav && nav.objecttype() != null
                     && (!ongenavigeerd(nav) || nav.objecttype() != subject) && nav.objecttype().heeftLid(nav.attribuut())
                     && nav.type() != null && (nav.type().isNumeriek() || nav.type().isPercentage())
                     && !Namen.bevatNaam(nav.objecttype().granulariteitLeden(), nav.attribuut()))
                && !multiGlobalRhs) return null;
        Conditie.Operator op = linksAgg ? v.op() : Operatoren.omkeren(v.op());   // orient the aggregation on the LEFT (Operatoren.omkeren(ONGELIJK)=ONGELIJK, != symmetric)
        final String opStr = Operatoren.symbool(op);
        Agg agg = aggregatie(f, subject, subjectVar, true);   // condition side: bind count for MIN/MAX so an empty set fires the leeg compare
        if (agg == null) return null;
        // §8.1.1 the RHS is a numeric arithmetic expression reading 2 DISTINCT scalar Parameter-globals — bind each into
        // its OWN Optional from-declaration (the nGlobalGate idiom: two global Variables in ONE expr collapse to the last
        // in the exec-model, so distinct decls are mandatory), then the accumulate-result comparison references BOTH. A
        // LEEG global arrives as null through the Optional carrier -> the scalarWaarde fns apply the §6/§5 leegwaarde
        // rules. Capped at 2 distinct globals (Predicate5 is the arity ceiling for a count-bound SOM/MIN/MAX: result +
        // count + subject + 2 globals); 3+ stays a symmetric skip, deliberately matching the DRL cap.
        if (multiGlobalRhs) return aggMultiGlobalRhs(v, f, ander, op, opStr, agg, pattern, subject, subjectVar, neg);
        // The threshold reader rfn(subj, globalsMap) + at most one bound Global. A to-ONE navigated RHS reads null-safely via
        // navReader (absent relation -> null leeg; to-many/unresolvable -> null -> marker). A single numeric Parameter-GLOBAL
        // binds the Global directly into the result constraint (the null-tolerant single-global idiom, like §8.2's whole-period
        // grens at ~L1692); a LEEG global arrives as null. >1 global / a non-numeric global stays a symmetric marker.
        final BiFunction<Object, Map<String, BigDecimal>, Object> rfn;
        Global<BigDecimal> gVar = null; String gNaam = null;
        if (ander instanceof Expr.AttribuutRef nar && (!ongenavigeerd(nar) || nar.objecttype() != subject)) {
            java.util.function.Function<Object, Object> reader = navReader(nar, subject);
            if (reader == null) return null;   // AFWEZIG / ONOPGELOST / NAAR_VEEL -> marker (symmetric with the DRL)
            rfn = (subj, gm) -> reader.apply(subj);
        } else {
            WaardeFn rhs = scalarWaarde(subject, ander);   // the value-path subject rides as a parameter (no-mutable-build-state)
            if (rhs == null) return null;
            if (!rhs.globals().isEmpty()) {
                if (rhs.globals().size() != 1) return null;
                gNaam = rhs.globals().iterator().next();
                gVar = globals.get(gNaam);
                if (gVar == null) return null;   // non-numeric global -> not bindable here -> skip
            }
            rfn = rhs.fn();   // the threshold, read per subject (+ bound global)
        }
        final Global<BigDecimal> global = gVar; final String globalNaam = gNaam;
        List<RuleItemBuilder<?>> extra = new ArrayList<>();
        extra.add(agg.accItem());
        PatternDSL.PatternDef resultPat;
        if (agg.countVar() != null) {
            // A count-bound aggregate: a SOM (empty -> the leegwaarde/fallback, not 0) OR a MIN/MAX (empty -> leeg, no
            // fallback — the count forces a row for the empty set). Bind the result, the count, and the subject; the
            // predicate reads count==0 -> the fallback (null for MIN/MAX) else the fold value.
            final WaardeFn fb = agg.somFallback();
            if (global == null) {
                Predicate3<Object, Object, Object> pred = (sum, cnt, subj) -> {   // (resultVar=sum, countVar=count, subjectVar)
                    Object aggVal = ((Number) cnt).longValue() == 0L ? (fb == null ? null : fb.fn().apply(subj, Map.of())) : sum;
                    return neg != RegelSpraakRuntime.rsAggVergelijk(aggVal, RegelSpraakRuntime.toBd(rfn.apply(subj, Map.of())), opStr);
                };
                resultPat = PatternDSL.pattern(agg.resultVar()).expr(nextId("aggcmp"), (Variable) agg.countVar(), (Variable) subjectVar, (Predicate3) pred);
            } else {
                Predicate4<Object, Object, Object, Object> pred = (sum, cnt, subj, gv) -> {   // + the bound global
                    Object aggVal = ((Number) cnt).longValue() == 0L ? (fb == null ? null : fb.fn().apply(subj, Map.of())) : sum;
                    return neg != RegelSpraakRuntime.rsAggVergelijk(aggVal, RegelSpraakRuntime.toBd(rfn.apply(subj, bindGlobal(globalNaam, gv))), opStr);
                };
                resultPat = PatternDSL.pattern(agg.resultVar()).expr(nextId("aggcmp"), (Variable) agg.countVar(), (Variable) subjectVar, (Variable) global, (Predicate4) pred);
            }
        } else {                                                // count / totaal / min / max: the result var IS the agg value
            if (global == null) {
                Predicate2<Object, Object> pred = (res, subj) ->
                        neg != RegelSpraakRuntime.rsAggVergelijk(res, RegelSpraakRuntime.toBd(rfn.apply(subj, Map.of())), opStr);
                resultPat = PatternDSL.pattern(agg.resultVar()).expr(nextId("aggcmp"), (Variable) subjectVar, (Predicate2) pred);
            } else {
                Predicate3<Object, Object, Object> pred = (res, subj, gv) ->
                        neg != RegelSpraakRuntime.rsAggVergelijk(res, RegelSpraakRuntime.toBd(rfn.apply(subj, bindGlobal(globalNaam, gv))), opStr);
                resultPat = PatternDSL.pattern(agg.resultVar()).expr(nextId("aggcmp"), (Variable) subjectVar, (Variable) global, (Predicate3) pred);
            }
        }
        extra.add(resultPat);
        return new Gate(pattern, extra);
    }

    /** The number of DISTINCT scalar Parameter-globals an aggregation-condition RHS expression reads (via
     *  {@link #scalarWaarde}), or 0 if it does not lower / any global is non-numeric. Used by
     *  {@link #aggregatieVoorwaarde} to detect the 2-distinct-global expression RHS ("global A plus global B"). */
    private int aggRhsGlobalCount(Objecttype subject, Expr ander) {
        WaardeFn rhs = scalarWaarde(subject, ander);
        if (rhs == null || rhs.globals().isEmpty()) return 0;
        for (String gn : rhs.globals()) if (globals.get(gn) == null) return 0;   // a non-numeric global -> not bindable
        return rhs.globals().size();
    }

    /** §8.1.1 an aggregation-condition whose RHS is a numeric arithmetic expression reading 2 DISTINCT scalar
     *  Parameter-globals ("het aantal elementen van de houder groter is dan global A plus global B"). Bind each
     *  distinct global into its OWN {@link java.util.Optional} from-declaration (the {@link #nGlobalGate} idiom: two
     *  global {@link Variable}s in ONE exec-model expr collapse to the LAST global's value, so distinct decls are
     *  mandatory), then the accumulate-result comparison references the aggregate + count + subject + BOTH global
     *  Optionals via {@link RegelSpraakRuntime#rsAggVergelijk}. A LEEG global arrives as null through the Optional
     *  carrier -> the scalarWaarde fns apply the §6/§5 leegwaarde rules (leeg-as-0 inside +/-). Capped at 2 distinct
     *  globals (Predicate5 is the arity ceiling for a count-bound SOM/MIN/MAX: result + count + subject + 2 globals);
     *  a 3rd distinct global returns null -> a symmetric skip matching the DRL cap. DRL reads each global as a free name. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private Gate aggMultiGlobalRhs(Conditie.Vergelijking v, Expr.Functie f, Expr ander, Conditie.Operator op, String opStr,
                                   Agg agg, PatternDSL.PatternDef pattern, Objecttype subject, Variable subjectVar, boolean neg) {
        WaardeFn rhs = scalarWaarde(subject, ander);
        if (rhs == null) return null;
        List<String> gns = new ArrayList<>(rhs.globals());
        if (gns.size() != 2) return null;   // >2 distinct globals -> symmetric skip (arity ceiling); 0/1 handled above
        Global<BigDecimal> g0 = globals.get(gns.get(0)), g1 = globals.get(gns.get(1));
        if (g0 == null || g1 == null) return null;
        final BiFunction<Object, Map<String, BigDecimal>, Object> rfn = rhs.fn();
        // Two distinct Optional from-declarations, anchored on the (always-present) subject so a LEEG global still yields
        // one element (unwrapping to null), exactly like nGlobalGate.
        Variable<java.util.Optional> dv0 = DSL.declarationOf(java.util.Optional.class, "$agg_gg_" + (seq++),
                DSL.from((Variable) subjectVar, (Variable) g0, (Function2) (subj, gv) -> java.util.Optional.ofNullable(gv)));
        Variable<java.util.Optional> dv1 = DSL.declarationOf(java.util.Optional.class, "$agg_gg_" + (seq++),
                DSL.from((Variable) subjectVar, (Variable) g1, (Function2) (subj, gv) -> java.util.Optional.ofNullable(gv)));
        final String n0 = gns.get(0), n1 = gns.get(1);
        List<RuleItemBuilder<?>> extra = new ArrayList<>();
        // The two global Optionals bind FIRST (upstream of the accumulate node) so the result-var constraint — which
        // lives ON the accumulate node — can reference them as cached-left. Binding them AFTER the accumulate crashes
        // PhreakAccumulateNode.evaluateResultConstraints (the same upstream-vs-downstream rule as aggVsAgg).
        extra.add(PatternDSL.pattern(dv0));   // bare binders for both global Optionals (a from-decl needs a pattern to bind)
        extra.add(PatternDSL.pattern(dv1));
        extra.add(agg.accItem());
        PatternDSL.PatternDef resultPat;
        if (agg.countVar() != null) {
            final WaardeFn fb = agg.somFallback();
            Predicate5<Object, Object, Object, Object, Object> pred = (sum, cnt, subj, o0, o1) -> {
                Object aggVal = ((Number) cnt).longValue() == 0L ? (fb == null ? null : fb.fn().apply(subj, Map.of())) : sum;
                return neg != RegelSpraakRuntime.rsAggVergelijk(aggVal,
                        RegelSpraakRuntime.toBd(rfn.apply(subj, gmap(List.of(n0, n1), unwrapGlobal(o0), unwrapGlobal(o1)))), opStr);
            };
            resultPat = PatternDSL.pattern(agg.resultVar()).expr(nextId("aggcmp"), (Variable) agg.countVar(),
                    (Variable) subjectVar, (Variable) dv1, (Variable) dv0, (Predicate5) pred);
        } else {
            Predicate4<Object, Object, Object, Object> pred = (res, subj, o0, o1) ->
                    neg != RegelSpraakRuntime.rsAggVergelijk(res,
                            RegelSpraakRuntime.toBd(rfn.apply(subj, gmap(List.of(n0, n1), unwrapGlobal(o0), unwrapGlobal(o1)))), opStr);
            resultPat = PatternDSL.pattern(agg.resultVar()).expr(nextId("aggcmp"), (Variable) subjectVar,
                    (Variable) dv1, (Variable) dv0, (Predicate4) pred);
        }
        extra.add(resultPat);
        return new Gate(pattern, extra);
    }

    /** §5.8 agg-vs-agg ("het aantal X &lt;op&gt; het aantal Y"): both operands are aggregations over the subject. Two
     *  accumulate CEs, then a result-var constraint comparing both folds via rsAggVergelijk (the right coerced to
     *  BigDecimal). A count-bound side (SOM / empty MIN·MAX) resolves count==0 -&gt; its leegwaarde (fallback or null); the
     *  predicate arity flexes with how many sides carry a count var. The DRL mirror rides one eval over two accumulates. */
    private Gate aggVsAgg(Conditie.Vergelijking v, PatternDSL.PatternDef pattern, Objecttype subject, Variable subjectVar) {
        return aggVsAgg(v, pattern, subject, subjectVar, false);
    }
    @SuppressWarnings({"unchecked", "rawtypes"})
    private Gate aggVsAgg(Conditie.Vergelijking v, PatternDSL.PatternDef pattern, Objecttype subject, Variable subjectVar, boolean negeer) {
        Agg a1 = aggregatie((Expr.Functie) v.links(), subject, subjectVar, true);
        if (a1 == null) return null;
        Agg a2 = aggregatie((Expr.Functie) v.rechts(), subject, subjectVar, true);
        if (a2 == null) return null;
        final String opStr = Operatoren.symbool(v.op());
        final boolean neg = negeer;
        final boolean c1 = a1.countVar() != null, c2 = a2.countVar() != null;
        final WaardeFn fb1 = a1.somFallback(), fb2 = a2.somFallback();
        List<RuleItemBuilder<?>> extra = new ArrayList<>();
        extra.add(a1.accItem());
        extra.add(a2.accItem());
        // The compare rides the SECOND accumulate's result (r2) as the pattern self and references the FIRST (r1) + the
        // count vars + subject as bound Variables — r1 is UPSTREAM of r2's accumulate node, so it is available as
        // cached-left. (Riding r1 and referencing the downstream r2 instead crashes PhreakAccumulateNode.) res1/res2 are
        // the LEFT/RIGHT fold values; a count-bound side resolves count==0 -> its leegwaarde via foldLeeg.
        Variable r1 = a1.resultVar(), r2 = a2.resultVar();
        PatternDSL.PatternDef rp;
        if (!c1 && !c2) {                                       // count / totaal both sides: the result var IS each value
            Predicate3<Object, Object, Object> p = (res2, res1, subj) ->
                    neg != RegelSpraakRuntime.rsAggVergelijk(res1, RegelSpraakRuntime.toBd(res2), opStr);
            rp = PatternDSL.pattern(r2).expr(nextId("aggcmp"), (Variable) r1, (Variable) subjectVar, (Predicate3) p);
        } else if (c1 && !c2) {
            Predicate4<Object, Object, Object, Object> p = (res2, res1, cnt1, subj) ->
                    neg != RegelSpraakRuntime.rsAggVergelijk(foldLeeg(res1, cnt1, fb1, subj), RegelSpraakRuntime.toBd(res2), opStr);
            rp = PatternDSL.pattern(r2).expr(nextId("aggcmp"), (Variable) r1, (Variable) a1.countVar(), (Variable) subjectVar, (Predicate4) p);
        } else if (!c1 && c2) {
            Predicate4<Object, Object, Object, Object> p = (res2, res1, cnt2, subj) ->
                    neg != RegelSpraakRuntime.rsAggVergelijk(res1, RegelSpraakRuntime.toBd(foldLeeg(res2, cnt2, fb2, subj)), opStr);
            rp = PatternDSL.pattern(r2).expr(nextId("aggcmp"), (Variable) r1, (Variable) a2.countVar(), (Variable) subjectVar, (Predicate4) p);
        } else {                                                // both count-bound (SOM / empty MIN·MAX): resolve each leeg
            Predicate5<Object, Object, Object, Object, Object> p = (res2, res1, cnt1, cnt2, subj) ->
                    neg != RegelSpraakRuntime.rsAggVergelijk(foldLeeg(res1, cnt1, fb1, subj), RegelSpraakRuntime.toBd(foldLeeg(res2, cnt2, fb2, subj)), opStr);
            rp = PatternDSL.pattern(r2).expr(nextId("aggcmp"), (Variable) r1, (Variable) a1.countVar(), (Variable) a2.countVar(), (Variable) subjectVar, (Predicate5) p);
        }
        extra.add(rp);
        return new Gate(pattern, extra);
    }

    /** A count-bound aggregate's leeg-resolved value: count==0 -&gt; the leegwaarde (the SOM fallback, or null for MIN/MAX),
     *  else the fold value. Mirrors the single-agg count-branch predicate. */
    private static Object foldLeeg(Object res, Object cnt, WaardeFn fb, Object subj) {
        if (((Number) cnt).longValue() == 0L) return fb == null ? null : fb.fn().apply(subj, Map.of());
        return res;
    }

    /** Does the comparison have an aggregation (count/sum/totaal/min/max) as either operand? Such a comparison is routed
     *  EXCLUSIVELY through {@link #aggregatieVoorwaarde} (skip if unlowerable) — never the scalar/navigated handlers. */
    private static boolean heeftAggregatieOperand(Conditie.Vergelijking v) {
        return (v.links() instanceof Expr.Functie fl && isAggregatieKind(fl.kind()))
                || (v.rechts() instanceof Expr.Functie fr && isAggregatieKind(fr.kind()));
    }


    /** §13.4.2/§5.6 apply a die/dat-subselectie filter to the accumulate SOURCE pattern. A §5.6 comparison whose RHS is
     *  a NUMERIC Parameter-global binds that global on the source pattern (a 2-var expr over the element + the global) —
     *  the exec analog of DRL reading the global as a free name in the source constraint ("prijs &lt; grens"); the LHS is
     *  the element's folded attribute. Every other supported filter is a fact-only leaf via {@link #leafPredicate}.
     *  null -&gt; the filter is unstructured, or its global is non-numeric / unregistered -&gt; the caller skips
     *  (symmetric with the DRL marker — DRL also needs the global declared). */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private PatternDSL.PatternDef filterSource(PatternDSL.PatternDef src, Conditie filter, Objecttype elem) {
        // §13.4.13.4 a COMPOUND subselectie on the element ("die aan alle volgende voorwaarden voldoen: …"): ALLE/DE
        // ANDs each bullet onto the source pattern as a successive .expr (mirroring DRL's `&&`); a counting quantifier
        // (geen/ten-minste/…) folds the bullets into ONE count-vs-N Predicate1. A bullet that cannot be lowered (null)
        // skips the WHOLE compound — never a partial filter (symmetric with DRL markering an un-lowerable child).
        if (filter instanceof Conditie.Samengesteld s) {
            if (s.kwantor() == Conditie.Kwantor.ALLE || s.kwantor() == Conditie.Kwantor.DE) {
                for (Conditie deel : s.onderdelen()) {
                    src = filterSource(src, deel, elem);   // recurse: each bullet is a leaf OR a param-global comparison
                    if (src == null) return null;
                }
                return src;
            }
            // §13.4.14.4 counting quantifier. Each bullet is either a fact-only Predicate1 ({@link #leafPredicate})
            // or a §5.6 numeric/percentage Parameter-GLOBAL comparison ("zijn leeftijd is kleiner dan de grens"). A
            // SINGLE distinct source-global across the bullets binds as a 2-var expr (element + the global), folding the
            // count INSIDE a Predicate2 — the exec analog of DRL reading the global as a free name regardless of
            // quantifier. 0 globals -> a plain Predicate1 fold. An un-lowerable leaf OR a SECOND distinct global skips
            // the whole compound (symmetric with RuleChecker's parity guard, so DRL also declines exactly those).
            final Conditie.Kwantor kw = s.kwantor(); final int n = s.drempel();
            Global<BigDecimal> bound = null;
            List<Predicate2<Object, BigDecimal>> tellers = new ArrayList<>();
            for (Conditie deel : s.onderdelen()) {
                GlobalVergelijking gb = globalBullet(deel, elem);   // a single numeric-vs-global comparison, or null
                if (gb != null) {
                    if (bound != null && bound != gb.global()) return null;   // >1 distinct global -> skip
                    bound = gb.global();
                    tellers.add(gb.test());
                } else {
                    // a fact-only leaf, OR a NESTED PURE-LEAF compound bullet ("hij voldoet aan alle volgende
                    // voorwaarden: …") folded into ONE Predicate1 (leafCompound). Either ignores the source-global.
                    Predicate1<Object> lp = deel instanceof Conditie.Samengesteld sub
                            ? leafCompound(sub, elem) : leafPredicate(deel, elem);
                    if (lp == null) return null;
                    tellers.add((e, gv) -> lp.test(e));
                }
            }
            final List<Predicate2<Object, BigDecimal>> fTellers = tellers;
            Predicate2<Object, BigDecimal> fold = (e, gv) -> {   // ch. 5.10 short circuit (see vastgesteld)
                int t = 0;
                for (int i = 0; i < fTellers.size(); i++) {
                    Boolean vast = vastgesteld(kw, n, t, i, fTellers.size() - i);
                    if (vast != null) return vast;
                    if (fTellers.get(i).test(e, gv)) t++;
                }
                return switch (kw) {
                    case GEEN -> t == 0; case TEN_MINSTE -> t >= n; case TEN_HOOGSTE -> t <= n; case PRECIES -> t == n;
                    default -> false;   // ALLE/DE handled above
                };
            };
            if (bound == null) {
                Predicate1<Object> p1 = e -> fold.test(e, null);   // no global -> a plain fact-only count fold
                return src.expr(nextId("filter"), (Predicate1) p1);
            }
            Predicate2<Object, Object> p2 = (e, gv) -> fold.test(e, (BigDecimal) gv);
            return src.expr(nextId("filter"), (Variable) bound, (Predicate2) p2);
        }
        GlobalVergelijking gb = globalBullet(filter, elem);   // §5.6 a getalachtig Parameter-global comparison source filter
        if (gb != null) {
            Predicate2<Object, Object> p2 = (e, gv) -> gb.test().test(e, (BigDecimal) gv);
            return src.expr(nextId("filter"), (Variable) gb.global(), (Predicate2) p2);
        }
        Predicate1<Object> fp = leafPredicate(filter, elem);
        return fp == null ? null : src.expr(nextId("filter"), (Predicate1) fp);
    }

    /** A §5.6/§13.4.13.4 numeric/percentage Parameter-GLOBAL comparison bullet ("zijn leeftijd is kleiner dan de
     *  grens"): the bound global {@link Variable} + a (element, globalValue) -&gt; boolean test, unit-restated like the
     *  §8.1.1 numeric-vs-global path. null when the bullet is not such a comparison, or its global is non-numeric /
     *  unregistered, or the element attribute is navigated / temporal / dimensioned (the caller then treats it as a
     *  fact-only leaf, or skips). Shared by {@link #filterSource}'s standalone branch and its counting-quantifier fold,
     *  so a single source-global binds identically in both — the only quantifier shape the ALLE/DE arm cannot reach
     *  (it recurses, binding each bullet's global on its own {@code .expr}). */
    /** §13.4.13.4 fold a nested PURE-LEAF compound bullet (a Samengesteld whose children are all fact-only leafPredicates
     *  — no Parameter-global reads) into ONE Predicate1 over the element: ALLE/DE -&gt; every child true, a counting
     *  quantifier -&gt; the count switch (geen/ten-minste/ten-hoogste/precies). Returns null when ANY child is a global
     *  comparison (the exec binds at most one source-global, already at the top-level fold) or is otherwise un-lowerable,
     *  so the caller skips the WHOLE compound (symmetric with DRL, gated by RuleChecker.execKanCountingFold). Mirrors
     *  DRL conditieInline's nested &amp;&amp;/|| on the accumulate source. Folds a SINGLE LEVEL of nesting only — a DEEPER
     *  compound child is declined (a parser bullet-merge can flatten 2+ sibling nested bullets into a depth-2 compound;
     *  declining keeps it a symmetric marker, never a fired-wrong fold). */
    private Predicate1<Object> leafCompound(Conditie.Samengesteld s, Objecttype elem) {
        final Conditie.Kwantor kw = s.kwantor(); final int n = s.drempel();
        List<Predicate1<Object>> kids = new ArrayList<>();
        for (Conditie deel : s.onderdelen()) {
            if (globalBullet(deel, elem) != null) return null;   // a global bullet inside a nested compound -> can't fold
            if (deel instanceof Conditie.Samengesteld) return null;   // a DEEPER nested compound -> decline (keep a marker)
            Predicate1<Object> lp = leafPredicate(deel, elem);
            if (lp == null) return null;
            kids.add(lp);
        }
        final List<Predicate1<Object>> fk = kids;
        // ch. 5.10 short circuit, here for ALL FIVE quantifiers: this fold is a plain lambda, so even ALLE/DE
        // must stop explicitly (the top-level conjunctive arm gets it from the alpha chain instead).
        return e -> {
            int t = 0;
            for (int i = 0; i < fk.size(); i++) {
                Boolean vast = vastgesteld(kw, n, t, i, fk.size() - i);
                if (vast != null) return vast;
                if (fk.get(i).test(e)) t++;
            }
            return switch (kw) {
                case ALLE, DE -> t == fk.size();
                case GEEN -> t == 0; case TEN_MINSTE -> t >= n; case TEN_HOOGSTE -> t <= n; case PRECIES -> t == n;
            };
        };
    }

    private record GlobalVergelijking(Global<BigDecimal> global, Predicate2<Object, BigDecimal> test) {}
    private GlobalVergelijking globalBullet(Conditie deel, Objecttype elem) {
        if (!(deel instanceof Conditie.Vergelijking v) || !(v.links() instanceof Expr.AttribuutRef la)
                || !ongenavigeerd(la) || !la.type().isGetalachtig() || !isNumeriekOp(v.op())   // §4.8 numeriek OR percentage
                || !(v.rechts() instanceof Expr.ParameterRef pr)) return null;
        if (elem == null || !elem.heeftLid(la.attribuut())) return null;
        if (Namen.bevatNaam(elem.granulariteitLeden(), la.attribuut())) return null;   // temporal element attr -> skip
        if (!elem.dimensiesVan(la.attribuut()).isEmpty()) return null;              // §3.6 gedimensioneerd -> skip
        Global<BigDecimal> g = globals.get(globalNaam(pr.parameter().naam()));
        if (g == null) return null;   // non-numeric / unregistered global -> skip (DRL needs the global declared too)
        String attr = la.attribuut();
        Conditie.Operator op = v.op();
        BigDecimal[] ff = restatement(effType(la), pr.parameter().datatype());        // §5.1 Tabel 32 unit restatement
        return new GlobalVergelijking(g, (e, gv) -> vergelijk(scale(num(e, attr), ff[0]), scale(gv, ff[1]), op));
    }

    // ---------------- §7.1 temporal totaal (Phase F) ----------------
    /** §7.1 "het totaal van &lt;temporeel attribuut&gt;": a TOTAAL over a single OWN (or to-one navigated) TEMPORAL
     *  attribute is a SCALAR fold of its {@code List<Segment>} timeline ({@link RegelSpraakRuntime#rsTijdTotaal}
     *  weighting each tijdvak by its granularity-period count), optionally clipped to a §7.1 "gedurende de tijd dat
     *  &lt;temporeel kenmerk&gt;" condition ({@code rsTijdKnip}, or {@code rsTijdKnipNiet} for a negated membership).
     *  Returns a (subject -&gt; BigDecimal) {@link WaardeFn} that reads no globals, or null for any other operand —
     *  a non-temporal attribute (-&gt; the instance {@link #aggregatie}), a §13.3.6.7 multi-period / set / unresolvable
     *  navigation, or a compound / unstructured filter (-&gt; honest skip; §8.4 compounds are a later increment).
     *  Mirrors DrlTranspiler.tijdreeksAggregatie — the only temporal expression that collapses to a scalar (§5.1.1). */
    private WaardeFn tijdreeksTotaal(Expr.Functie f, Objecttype subject) {
        if (f.kind() != Expr.FunctieKind.TOTAAL || f.argumenten().size() != 1) return null;
        if (f.filter() instanceof Conditie.Onopgelost) return null;   // unstructured restriction -> skip (never an unconditioned total)
        if (!(f.argumenten().get(0) instanceof Expr.AttribuutRef a) || a.objecttype() == null) return null;
        if (a.set()) return null;     // "het totaal van <attr> van een SET" is an instance aggregation, not an own timeline
        RegelSpraakModel.Granulariteit g = Namen.opNaam(a.objecttype().granulariteitLeden(), a.attribuut());
        if (g == null) return null;   // not temporal -> fall through to the instance accumulate
        if (Tijdlijnen.meerPeriodiek(a.objecttype().tijdlijnAantalLeden(), a.attribuut())) return null;   // §13.3.6.7 multi-period -> skip
        java.util.function.Function<Object, Object> reader = navReader(a, subject);   // own or to-one navigated, root-guarded
        if (reader == null) return null;
        final String gran = g.name();
        // §7.1/§8.4 optional gedurende clip: a single temporal kenmerk (positive intersect / negated complement), or a
        // §8.4.2 pure-kenmerk compound (ALLE intersect / ten-minste-1 union). A mixed-static compound -> skip (next inc).
        String kenmerk = null; boolean negatie = false; Class<?> segCls = null; String navRol = null;
        BiFunction<Object, List<?>, List<?>> compoundClip = null;
        VergClip vergClip = null;                                  // §7.1 EUR71 a temporal-comparison clip
        java.util.function.Predicate<Object> staticGuard = null;   // §8.4 mixed-static value-side gate
        boolean orWiden = false;                                   // ten-minste-1: a TRUE static member widens to the full span
        if (f.filter() instanceof Conditie.Kenmerk k && k.navigatie() == null
                && subject != null && Namen.bevatNaam(subject.kenmerkGranulariteitLeden(), k.kenmerk())) {
            segCls = segmentClass();
            if (segCls == null) return null;
            kenmerk = k.kenmerk();
            negatie = k.negatie();
        } else if (f.filter() instanceof Conditie.Kenmerk kn && kn.navigatie() != null && subject != null) {
            // §7.1 a NAVIGATED temporal kenmerk ("gedurende de tijd dat zijn partner een recht op korting heeft"): clip
            // to the to-one role's kenmerk timeline, read root-guarded (a null reference -> leeg). Mirrors DRL.
            segCls = segmentClass();
            if (segCls == null) return null;
            KenmerkHop hop = kenmerkHop(kn, subject);
            if (hop == null) return null;   // the bound hop does not fit this subject
            Objecttype doel = hop.doel();
            if (doel == null || kn.negatie() || !Namen.bevatNaam(doel.kenmerkGranulariteitLeden(), kn.kenmerk())
                    || Tijdlijnen.meerPeriodiek(doel.kenmerkTijdlijnAantalLeden(), kn.kenmerk())) return null;   // negated / to-many / non-temporal -> skip
            navRol = hop.rol();
            kenmerk = kn.kenmerk();
        } else if (f.filter() instanceof Conditie.Vergelijking vc) {
            // §7.1 EUR71 "gedurende de tijd dat <temporeel attr> <op> <scalar>": clip the value timeline to the
            // comparison's OWN boolean timeline (rsTijdConditie over the comparison attribute's segments), then total.
            segCls = segmentClass();
            if (segCls == null) return null;
            vergClip = vergelijkingClip(vc, subject);
            if (vergClip == null) return null;   // non-temporal / cross-unit / datum / navigated comparison -> skip
        } else if (f.filter() instanceof Conditie.Samengesteld sc) {
            segCls = segmentClass();
            if (segCls == null) return null;
            // §8.4.2 partition: temporal kenmerks (the clip) + static comparisons (the value-side guard). Any other
            // member -> skip; no temporal member -> not a temporal restriction (skip).
            List<Conditie.Kenmerk> kenmerken = new ArrayList<>();
            List<Conditie.Vergelijking> statisch = new ArrayList<>();
            for (Conditie c : sc.onderdelen()) {
                if (c instanceof Conditie.Kenmerk kk) kenmerken.add(kk);
                else if (c instanceof Conditie.Vergelijking vv) statisch.add(vv);
                else return null;
            }
            if (kenmerken.isEmpty()) return null;
            compoundClip = kenmerkClip(kenmerken, sc.kwantor(), sc.drempel(), subject, segCls);
            if (compoundClip == null) return null;
            if (!statisch.isEmpty()) {
                boolean alle = sc.kwantor() == Conditie.Kwantor.ALLE;
                staticGuard = statischeGuard(statisch, subject, alle);
                if (staticGuard == null) return null;   // a static member we cannot lower -> skip, never an ungated totaal
                orWiden = !alle;   // ten-minste-1: a TRUE static member widens the result to the full (unclipped) span
            }
        } else if (f.filter() != null) {
            return null;   // any other restriction -> honest skip
        }
        final String fKenmerk = kenmerk; final boolean fNeg = negatie; final Class<?> fSeg = segCls; final String fNavRol = navRol;
        final BiFunction<Object, List<?>, List<?>> fClip = compoundClip; final VergClip fVerg = vergClip;
        final java.util.function.Predicate<Object> fGuard = staticGuard; final boolean fOrWiden = orWiden;
        BiFunction<Object, Map<String, BigDecimal>, Object> fn = (s, gm) -> {
            List<?> tl = (List<?>) reader.apply(s);
            List<?> clipped;
            if (fKenmerk != null) {
                List<?> cond;
                if (fNavRol != null) {   // §7.1 navigated kenmerk: read the to-one role's kenmerk timeline, root-guarded
                    Object rel = RegelSpraakRuntime.get(s, fNavRol);
                    cond = rel == null ? null : (List<?>) RegelSpraakRuntime.get(rel, fKenmerk);
                } else {
                    cond = (List<?>) RegelSpraakRuntime.get(s, fKenmerk);
                }
                clipped = fNeg ? RegelSpraakRuntime.rsTijdKnipNiet(tl, cond, fSeg) : RegelSpraakRuntime.rsTijdKnip(tl, cond, fSeg);
            } else if (fVerg != null) {
                BigDecimal grens = RegelSpraakRuntime.toBd(fVerg.grens().fn().apply(s, gm));
                List<?> cond = RegelSpraakRuntime.rsTijdConditie((List<?>) fVerg.timeline().apply(s), fVerg.toets(), grens, fSeg);
                clipped = RegelSpraakRuntime.rsTijdKnip(tl, cond, fSeg);
            } else if (fClip != null) {
                clipped = fClip.apply(s, tl);
            } else {
                clipped = tl;
            }
            if (fGuard != null) {
                boolean guardHeld = fGuard.test(s);
                if (fOrWiden) return RegelSpraakRuntime.rsTijdTotaal(guardHeld ? tl : clipped, gran);   // ten-minste-1: TRUE -> full span
                if (!guardHeld) return java.math.BigDecimal.ZERO;                                       // ALLE: FALSE -> 0
            }
            return RegelSpraakRuntime.rsTijdTotaal(clipped, gran);
        };
        return new WaardeFn(fn, vergClip != null ? vergClip.grens().globals() : Set.of());
    }

    /** §7.1 EUR71 the temporal-attribute comparison's condition reader: the timeline of the comparison attribute, the
     *  toets code, and the scalar grens (a {@link WaardeFn} carrying its globals). null for a non-temporal /
     *  cross-unit / datum / navigated comparison (the caller skips). Mirrors DrlTranspiler.vergelijkingTijdreeks /
     *  geheelPeriode82's comparison branch. */
    private VergClip vergelijkingClip(Conditie.Vergelijking v, Objecttype subject) {
        Expr.AttribuutRef tijd; Expr ander; boolean linksTemp;
        if (v.links() instanceof Expr.AttribuutRef la && isTemporeel82(la))       { tijd = la; ander = v.rechts(); linksTemp = true; }
        else if (v.rechts() instanceof Expr.AttribuutRef ra && isTemporeel82(ra)) { tijd = ra; ander = v.links();  linksTemp = false; }
        else return null;
        if (!(tijd.type().isNumeriek() || tijd.type().isPercentage())) return null;
        if (ander instanceof Expr.AttribuutRef ar && isTemporeel82(ar)) return null;   // both-temporal (timeline vs timeline) -> skip
        io.github.yhavinga.regelspraak.model.Eenheid el = eff.eenheidVan(tijd), er = eff.eenheidVan(ander);
        if (el != null && er != null && el.vereistOmrekening(er)) return null;   // cross-unit -> skip (raw-magnitude would be wrong)
        Integer code = Tijdrekening.toets(linksTemp ? v.op() : Operatoren.omkeren(v.op()));        // temporal operand on the left (REVERSE)
        if (code == null) return null;                                           // datum ordering -> skip
        java.util.function.Function<Object, Object> tl = temporeelTijdlijnReader(tijd, subject);
        if (tl == null) return null;                                             // navigated/unresolvable -> skip
        // the grens is read value-side (own attr / to-one navigated / literal / global): the subject rides as
        // the VALUE-path parameter so a navigated grens ("... groter is dan de inkomen van zijn partner")
        // lowers — symmetric with DRL's waarde() (no-mutable-build-state).
        WaardeFn g = scalarWaarde(subject, ander);
        if (g == null) return null;                                              // unlowerable grens -> skip
        return new VergClip(tl, code, g);
    }
    private record VergClip(java.util.function.Function<Object, Object> timeline, int toets, WaardeFn grens) {}

    /** §5.8.5/§13.4.17.51-.54 a SOM / MINIMUM / MAXIMUM over a gedimensioneerd attribuut, restricted to a
     *  {@link Expr.DimensieSelectie}'s labels — a FIXED fold of the attribute's {@code Map<label,value>} over the
     *  selected keys ({@link RegelSpraakRuntime#rsDimSom}/{@code rsDimMin}/{@code rsDimMax}, null-skipping empties),
     *  NOT an instance accumulate. §5.8.2: an all-empty SOM yields the declared numeric-literal "of … als die er niet
     *  zijn" fallback (else null — an empty sum is a leegwaarde, not 0). Returns a (subject -> BigDecimal) WaardeFn
     *  reading no globals, or null (no selectie / unresolvable map / non-literal SOM fallback). Mirrors
     *  DrlTranspiler.dimensieAggregatie. */
    private WaardeFn dimensieAggregatie(Expr.Functie f, Objecttype subject) {
        if (f.dimensieSelectie() == null) return null;
        Expr.FunctieKind kind = f.kind();
        if (kind != Expr.FunctieKind.SOM && kind != Expr.FunctieKind.MINIMUM && kind != Expr.FunctieKind.MAXIMUM) return null;
        if (f.argumenten().size() != 1 || !(f.argumenten().get(0) instanceof Expr.AttribuutRef a)) return null;
        java.util.function.Function<Object, Object> reader = navReader(a, subject);   // the dimensioned attribute's Map field
        if (reader == null) return null;
        final List<String> labels = new ArrayList<>(f.dimensieSelectie().labels());
        BigDecimal fallback = null;
        if (kind == Expr.FunctieKind.SOM && f.leegwaarde() != null) {
            if (!(f.leegwaarde() instanceof Expr.Literal lit) || !lit.type().isNumeriek()) return null;
            fallback = bd(lit);
            if (fallback == null) return null;
        }
        final BigDecimal fFallback = fallback; final Expr.FunctieKind fKind = kind;
        BiFunction<Object, Map<String, BigDecimal>, Object> vf = (s, gm) -> {
            Map<?, ?> map = (Map<?, ?>) reader.apply(s);
            BigDecimal r = switch (fKind) {
                case SOM     -> RegelSpraakRuntime.rsDimSom(map, labels);
                case MINIMUM -> RegelSpraakRuntime.rsDimMin(map, labels);
                case MAXIMUM -> RegelSpraakRuntime.rsDimMax(map, labels);
                default      -> null;
            };
            return fKind == Expr.FunctieKind.SOM && r == null ? fFallback : r;
        };
        return new WaardeFn(vf, Set.of());
    }

    /** Resolve an {@link Expr.AttribuutRef} to a {@code (subject -> value)} reader: the subject's OWN attribute, or a
     *  to-ONE navigated read (root-guarded so a null relation reads as leeg, never an NPE). null for an absent /
     *  to-many / unresolvable navigation — the model analog of DrlTranspiler.naviResolve's EIGEN / GENAVIGEERD vs the
     *  marker cases. (The self-anaphora "van de &lt;subject-role&gt;" is resolved the same way DRL does, so the two
     *  back-ends agree.) */
    private java.util.function.Function<Object, Object> navReader(Expr.AttribuutRef a, Objecttype subject) {
        if (a.objecttype() == null || !a.objecttype().heeftLid(a.attribuut())) return null;   // AFWEZIG
        if (!a.navigatiePad().isEmpty()) return null;   // §13.4.1 multi-hop chain: ONLY navKetenReader (value path) lowers it
        final String attr = a.attribuut();
        String rol = null;
        if (navigatieBindingen.containsKey(a)) {
            rol = wireRol(a, subject);
            if (rol == null) return null;   // ONOPGELOST: the bound walk does not fit this subject
        } else if (a.navigatieRol() != null) rol = model.verbindendeRolViaNaam(subject, a.objecttype(), a.navigatieRol());
        if (rol == null) {
            if (a.objecttype() == subject) return typedGetter(factClass(subject), attr);   // EIGEN
            rol = subject != null ? model.verbindendeRol(subject, a.objecttype()) : null;
            if (rol == null) return null;   // ONOPGELOST
        }
        if (subject != null && model.naarVeelRol(subject, a.objecttype(), rol)) return null;   // NAAR_VEEL
        if (!heeftVeld("genavigeerde lezing", subject, rol, a.objecttype())) return null;   // §13.3.9 no bean field to read (name-dedup)
        // GENAVIGEERD: read the relation field on the subject, then the attribute on the (known) target type — both
        // typed (Accessors LMF) when the fact class is known at build time. Null relation -> leeg (null-safe).
        java.util.function.Function<Object, Object> roleGet = typedGetter(factClass(subject), rol);
        java.util.function.Function<Object, Object> attrGet = typedGetter(factClass(a.objecttype()), attr);
        return s -> { Object rel = roleGet.apply(s); return rel == null ? null : attrGet.apply(rel); };
    }

    /** The §13.4.1 MULTI-hop to-one chain reader ("de X van de Y van de Z", the ref's navigatiePad): one typed
     *  getter per declared role hop, short-circuiting to null (leeg) on ANY intervening null relation — the chain
     *  generalisation of {@link #navReader}'s single-hop root-guard, mirroring DrlTranspiler's KETEN value read.
     *  The pad is re-validated against THIS subject (volgKeten), so a chain resolved against another subject skips.
     *  Deliberately reached ONLY from the consequence VALUE path (scalarWaarde's chain branch, gated by
     *  {@code ketenWaardePad}) — the condition side keeps the symmetric marker both back-ends emit. */
    private java.util.function.Function<Object, Object> navKetenReader(Expr.AttribuutRef a, Objecttype subject) {
        if (a.objecttype() == null || !a.objecttype().heeftLid(a.attribuut())) return null;   // AFWEZIG
        FactModelGenerator.Navigatiewandeling w = keten(a, subject);
        if (w == null) return null;   // ONOPGELOST / to-many
        List<java.util.function.Function<Object, Object>> hops = new ArrayList<>();
        Objecttype cur = subject;
        for (int i = 0; i < w.rollen().size(); i++) {
            // EVERY hop, not just the first: a chain is only readable where each of its links has a field.
            if (!heeftVeld("genavigeerde lezing", cur, w.rollen().get(i), w.bereikt().get(i))) return null;   // §13.3.9 name-dedup
            hops.add(typedGetter(factClass(cur), w.rollen().get(i)));
            cur = w.bereikt().get(i);
        }
        java.util.function.Function<Object, Object> attrGet = typedGetter(factClass(a.objecttype()), a.attribuut());
        return s -> {
            Object o = s;
            for (java.util.function.Function<Object, Object> h : hops) {
                o = h.apply(o);
                if (o == null) return null;   // §5.5.5 an intervening leeg relation reads the operand as leeg
            }
            return attrGet.apply(o);
        };
    }

    /** §13.4.1/§13.3.9 REACTIVITY over a to-ONE navigated value read: property/class reactivity is per fact TYPE,
     *  so a subject pattern re-fires only on the SUBJECT's update — never on the FAR fact's. A spiegel-regel whose
     *  RHS navigates to another object's attribute ("de X van een A ... als de Y van de B van A") therefore computed
     *  once against a still-leeg Y and kept the stale value when a sibling rule of the SAME fire-cycle wrote B.Y (the
     *  order-fragility of navigated reads). For each DISTINCT to-one navigation the value reads, add a far-side
     *  reactivity item {@code OR( FarType(this == reachFar(subject)), eval(reachFar(subject) == null) )}: the JOIN
     *  alternative binds the reached fact so its modify re-activates the rule; the {@code eval} alternative fires the
     *  NULL-relation subject (which reads leeg and never goes stale, but MUST still fire, §6.9 — e.g. a broken chain
     *  whose value still adds an OWN attribute). The two alternatives are mutually exclusive, so no subject
     *  double-fires. The consequence keeps its OWN null-safe navigated read. Unresolvable / to-many navigations are
     *  skipped (they lower elsewhere / stay a marker). Mirrors the DRL back-end navReactiviteitClausules.
     *  <p>null (never an empty list) when a hop has NO bean field: the join would probe a displaced member and bind
     *  the wrong fact, so the whole construct is refused with {@link #navReden}. A {@code continue} would drop the
     *  watch silently and leave the rule stale — the one direction this method exists to prevent. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private List<RuleItemBuilder<?>> navReactiviteit(Expr waarde, Objecttype subject, Variable subjectVar) {
        List<Expr.AttribuutRef> refs = new ArrayList<>();
        navRefs(waarde, subject, refs);
        if (refs.isEmpty()) return List.of();
        // Distinct by the reached FAR type + full role path (subject -> reader): the same relation read twice needs
        // one item. Key on the ordered role hops that reach the fact whose attribute is read.
        java.util.LinkedHashMap<String, java.util.function.Function<Object, Object>> joins = new java.util.LinkedHashMap<>();
        java.util.LinkedHashMap<String, Objecttype> farTypes = new java.util.LinkedHashMap<>();
        for (Expr.AttribuutRef a : refs) {
            if (a.objecttype() == null || !a.objecttype().heeftLid(a.attribuut())) continue;   // AFWEZIG -> no join
            java.util.function.Function<Object, Object> reachFar; String key; Objecttype far = a.objecttype();
            if (!a.navigatiePad().isEmpty()) {
                // §13.4.1 multi-hop chain: walk the declared role hops to the fact declaring the read attribute.
                FactModelGenerator.Navigatiewandeling w = keten(a, subject);
                if (w == null) continue;
                List<java.util.function.Function<Object, Object>> hops = new ArrayList<>();
                Objecttype cur = subject;
                for (int i = 0; i < w.rollen().size(); i++) {
                    if (!heeftVeld("genavigeerde lezing", cur, w.rollen().get(i), w.bereikt().get(i))) return null;   // §13.3.9 name-dedup
                    hops.add(typedGetter(factClass(cur), w.rollen().get(i)));
                    cur = w.bereikt().get(i);
                }
                reachFar = s -> { Object o = s; for (var h : hops) { o = h.apply(o); if (o == null) return null; } return o; };
                key = far.naam + "|" + String.join(".", w.rollen());
            } else {
                // single-hop to-one: resolve the connecting role exactly as navReader does (named, self-anaphora, or
                // the unique connecting role); a to-many / unresolvable navigation is left to its own lowering.
                String rol;
                if (navigatieBindingen.containsKey(a)) {
                    rol = wireRol(a, subject);
                } else {
                    rol = a.navigatieRol() != null ? model.verbindendeRolViaNaam(subject, far, a.navigatieRol()) : null;
                    if (rol == null && far == subject) continue;   // EIGEN read (own-attr) — the eigenReads watch covers it
                    if (rol == null) rol = model.verbindendeRol(subject, far);
                }
                if (rol == null) continue;                     // ONOPGELOST
                if (model.naarVeelRol(subject, far, rol)) continue;   // NAAR_VEEL -> aggregate path, not a scalar join
                if (!heeftVeld("genavigeerde lezing", subject, rol, far)) return null;   // §13.3.9 no bean field to join on (name-dedup)
                java.util.function.Function<Object, Object> roleGet = typedGetter(factClass(subject), rol);
                reachFar = roleGet;
                key = far.naam + "|" + rol;
            }
            if (factClass(far) == null) continue;
            joins.putIfAbsent(key, reachFar);
            farTypes.putIfAbsent(key, far);
        }
        List<RuleItemBuilder<?>> uit = new ArrayList<>();
        for (var entry : joins.entrySet()) {
            final java.util.function.Function<Object, Object> reachFar = entry.getValue();
            Variable farVar = DSL.declarationOf(factClass(farTypes.get(entry.getKey())), "$nav_" + (seq++));
            PatternDSL.PatternDef joinPat = PatternDSL.pattern(farVar).expr(nextId("navjoin"), (Variable) subjectVar,
                    (Predicate2) (far, subj) -> reachFar.apply(subj) == far);
            org.drools.model.view.ViewItem nullGuard = DSL.expr(nextId("navnull"), (Variable) subjectVar,
                    (Predicate1) (subj) -> reachFar.apply(subj) == null);
            uit.add(DSL.or(joinPat, nullGuard));
        }
        return uit;
    }

    /** A getter for {@code prop} on {@code cls}: a TYPED {@link Accessors} LMF accessor when the fact class is known
     *  at model-build time, else the cached reflective bridge. Both are null-owner-safe (null -> null). */
    private static java.util.function.Function<Object, Object> typedGetter(Class<?> cls, String prop) {
        return cls != null ? Accessors.getter(cls, prop) : (s -> RegelSpraakRuntime.get(s, prop));
    }

    /** Resolve the to-ONE navigation role of a navigated AttribuutRef (navigatieRol != null), mirroring
     *  {@link #navReader}'s GENAVIGEERD case — or null when absent / unresolvable / to-many. The navigated
     *  COMPARISON branches use this (rather than {@link #navReader}) so they can read the RELATION field and,
     *  per §5.5.5, treat a NULL relation as a LEEGWAARDE — the SAME as a present relation with a null ATTRIBUTE
     *  value — and then let {@link #vergelijk}'s leegwaarde semantics decide (Tabel 33-44: a leeg operand makes
     *  ordering/GELIJK onwaar but ONGELIJK WAAR, Tabel 44). The earlier "a null relation is false for EVERY
     *  operator" reading mirrored mvel's {@code rol!.attr} short-circuit but was spec-WRONG for ONGELIJK; the DRL
     *  voorwaarde now also reads a navigated comparison leegwaarde-faithfully (genavigeerdeVergelijkingVoorwaarde). */
    private String navComparisonRole(Expr.AttribuutRef a, Objecttype subject) {
        if (a.objecttype() == null || ongenavigeerd(a) || !a.objecttype().heeftLid(a.attribuut())) return null;
        if (!a.navigatiePad().isEmpty()) return null;   // §13.4.1 multi-hop chain: comparisons keep the symmetric marker
        String rol = navigatieBindingen.containsKey(a) ? wireRol(a, subject)
                : model.verbindendeRolViaNaam(subject, a.objecttype(), a.navigatieRol());
        if (rol == null) return null;
        if (subject != null && model.naarVeelRol(subject, a.objecttype(), rol)) return null;   // NAAR_VEEL
        if (!heeftVeld("genavigeerde lezing", subject, rol, a.objecttype())) return null;   // §13.3.9 no bean field to read (name-dedup)
        return rol;
    }

    /** THE own-read shape test: the reference carries neither a written step nor a wire binding. Every arm that
     *  reads a reference as the subject's (or the element's) own attribute asks this, never {@code navigatieRol() == null}
     *  alone — a BOUND by-pair read has no written step either, and an own-read arm that fired on it read the
     *  subject's same-named attribute instead of walking the binding (V1-WIREMIGRATIE P1, lens B1). */
    private boolean ongenavigeerd(Expr.AttribuutRef a) {
        return a.navigatieRol() == null && !navigatieBindingen.containsKey(a);
    }

    /** The single hop of a BOUND to-one navigation: the reached role the wire names, or null when the walk does not
     *  fit {@code subject} or is not one hop. The single-hop readers above decide on it instead of the written name. */
    private String wireRol(Expr.AttribuutRef a, Objecttype subject) {
        FactModelGenerator.Navigatiewandeling w =
                FactModelGenerator.navigatiewandeling(model, subject, navigatieBindingen.get(a), a.objecttype());
        return w == null || w.rollen().size() != 1 ? null : w.rollen().get(0);
    }

    /** The §13.4.1 chain walk of a pad-carrying AttribuutRef: the wire's bindings where the node carries them,
     *  else the written navigatiePad re-validated from {@code subject}. Null when unresolved or any hop is to-many —
     *  the two cases the chain readers refuse alike. */
    private FactModelGenerator.Navigatiewandeling keten(Expr.AttribuutRef a, Objecttype subject) {
        if (navigatieBindingen.containsKey(a)) {
            FactModelGenerator.Navigatiewandeling w =
                    FactModelGenerator.navigatiewandeling(model, subject, navigatieBindingen.get(a), a.objecttype());
            return w == null || w.naarVeel() ? null : w;
        }
        List<Objecttype> bereikt = model.volgKeten(subject, a.navigatiePad());
        if (bereikt == null || bereikt.get(bereikt.size() - 1) != a.objecttype()) return null;
        return new FactModelGenerator.Navigatiewandeling(a.navigatiePad(), bereikt, false);
    }

    /** The one hop of a navigated kenmerk check: the reached role (the bean field on the subject) and the type it
     *  reaches. From the wire's {@code navigatieBinding} where the check carries one — null when that hop does not
     *  fit {@code subject} — else the written role, as before the port. */
    private record KenmerkHop(String rol, Objecttype doel) {}
    private KenmerkHop kenmerkHop(Conditie.Kenmerk k, Objecttype subject) {
        if (navigatieBindingen.containsKey(k)) {
            FactModelGenerator.Navigatiewandeling w =
                    FactModelGenerator.navigatiewandeling(model, subject, navigatieBindingen.get(k), null);
            return w == null || w.rollen().size() != 1 ? null : new KenmerkHop(w.rollen().get(0), w.bereikt().get(0));
        }
        return new KenmerkHop(k.navigatie(), subject == null ? null : model.rolDoeltypeNaarSubject(subject, k.navigatie()));
    }

    /** The fact {@code Segment} class (a runtime objecttype) reflectively, or null when absent — needed by the
     *  timeline-constructing helpers (rsTijdKnip/rsTijdUnie/rsAantalDagenIn/rsTijdsevenredig) to build new segments. */
    Class<?> segmentClass() {
        try { return Class.forName(FACTS + ".Segment", true, beanLoader); }
        catch (ClassNotFoundException e) { return null; }
    }

    // ---------------- §13.4.17.57/.58 temporal WRITE producers (Phase F) ----------------
    /** A lowered timeline producer: {@code fn(subject, globalValue)} -> the {@code List<Segment>} to write, plus the
     *  name of the (≤1) List-global it reads (a §7.3.2 tijdsevenredig PARAMETER base), or null. */
    private record TemporeleWaarde(BiFunction<Object, Object, Object> fn, String global) {}

    /** §5.1.4 lower a RHS that PRODUCES a timeline written to a single-period temporal doel attribute, into a
     *  {@link TemporeleWaarde} — or null when it is not a supported producer (caller skips). Two writers:
     *  §13.4.17.57 "het aantal dagen in &lt;de maand|het jaar&gt; dat &lt;temporeel kenmerk&gt;" ->
     *  {@link RegelSpraakRuntime#rsAantalDagenIn} over the kenmerk's true-period timeline; §13.4.17.58 "het
     *  tijdsevenredig deel per &lt;maand|jaar&gt; van &lt;basis&gt; gedurende ..." -> {@link
     *  RegelSpraakRuntime#rsTijdsevenredig} prorating a base value-timeline (the subject's own temporal attribute, or
     *  a temporal PARAMETER's List global). Both bucket by the DOEL's granularity (the operator's "per/in
     *  &lt;maand|jaar&gt;" MUST match the doel tijdlijn, §7.2 — a mismatch skips). For tijdsevenredig the base must be
     *  numeriek, COARSER-OR-EQUAL to the bucket (§7.3.2 reads it "voor de hele maand/jaar"), and in the SAME unit as
     *  the doel (no rate conversion — §7.3.2 ex.3 markers). Mirrors DrlTranspiler.temporeleWaarde. */
    private TemporeleWaarde temporeleWaarde(Expr w, Objecttype subject,
            RegelSpraakModel.Granulariteit gran, Datatype doelType,
            java.util.function.Function<Object, Object> anker) {
        if (w instanceof Expr.Functie f && f.kind() == Expr.FunctieKind.AANTAL_DAGEN_IN
                && f.granulariteit() == gran && subject != null) {
            java.util.function.Function<Object, List<?>> cond = conditieTijdreeks(f.filter(), subject);   // single kenmerk / §8.4 compound
            java.util.function.Predicate<Object> guard = cond == null ? null : mixedGuardWrite(f.filter(), subject);   // §8.4 static gate
            if (cond != null && guard != null) {
                Class<?> segCls = segmentClass();
                if (segCls != null) {
                    final String granName = gran.name();
                    final java.util.function.Predicate<Object> fGuard = guard;
                    // §3.8 anchored buckets for a variable-startpunt doel (rsAantalDagenInVanaf; a leeg
                    // anchor yields no periods — the §6.8 fout rides the startpunt rule's companion). Mirrors DRL.
                    if (anker != null)
                        return new TemporeleWaarde((s, gv) -> fGuard.test(s)
                                ? RegelSpraakRuntime.rsAantalDagenInVanaf(cond.apply(s), granName,
                                        (java.time.LocalDate) anker.apply(s), segCls) : new ArrayList<>(), null);
                    return new TemporeleWaarde((s, gv) -> fGuard.test(s)
                            ? RegelSpraakRuntime.rsAantalDagenIn(cond.apply(s), granName, segCls) : new ArrayList<>(), null);
                }
            }
        }
        if (w instanceof Expr.Functie f && f.kind() == Expr.FunctieKind.TIJDSEVENREDIG_DEEL
                && f.granulariteit() == gran && f.argumenten().size() == 1 && subject != null) {
            Expr base = f.argumenten().get(0);
            RegelSpraakModel.Granulariteit baseGran = null; Datatype baseType = null; String baseGlobal = null;
            BiFunction<Object, Object, List<?>> baseReader = null;   // (subject, globalValue) -> the base List<Segment>
            if (base instanceof Expr.AttribuutRef ba && ongenavigeerd(ba) && !ba.set() && ba.objecttype() == subject
                    && Namen.bevatNaam(subject.granulariteitLeden(), ba.attribuut())
                    && !Tijdlijnen.meerPeriodiek(subject.tijdlijnAantalLeden(), ba.attribuut())) {
                baseGran = Namen.opNaam(subject.granulariteitLeden(), ba.attribuut());
                baseType = effType(ba);
                String baseAttr = ba.attribuut();
                baseReader = (s, gv) -> (List<?>) RegelSpraakRuntime.get(s, baseAttr);
            } else if (base instanceof Expr.AttribuutRef ba && !ongenavigeerd(ba) && !ba.set()
                    && ba.objecttype() != null && Namen.bevatNaam(ba.objecttype().granulariteitLeden(), ba.attribuut())
                    && !Tijdlijnen.meerPeriodiek(ba.objecttype().tijdlijnAantalLeden(), ba.attribuut())) {
                // §7.3.2 a TO-ONE NAVIGATED base ("de X van zijn partner"): read the partner's temporal attribute root-guarded
                // (navReader: null relation -> leeg). The gedurende condition stays over the SUBJECT. Mirrors DRL.
                java.util.function.Function<Object, Object> nav = navReader(ba, subject);
                if (nav != null) {
                    baseGran = Namen.opNaam(ba.objecttype().granulariteitLeden(), ba.attribuut());
                    baseType = effType(ba);
                    baseReader = (s, gv) -> (List<?>) nav.apply(s);
                }
            } else if (base instanceof Expr.ParameterRef bp && bp.parameter().granulariteit() != null) {
                baseGran = bp.parameter().granulariteit();
                baseType = bp.parameter().datatype();
                baseGlobal = globalNaam(bp.parameter().naam());
                baseReader = (s, gv) -> (List<?>) gv;   // the List<Segment> global the runtime supplies
            }
            if (baseReader != null && baseGran.ordinal() >= gran.ordinal() && baseType.isNumeriek()) {
                // §7.3.2 ex.3 rate conversion: the base's per-tijdseenheid unit may DIFFER from the doel's (€/jr base
                // into a €/mnd doel). §7.3.1 restates the base FIRST (÷12 via rsTijdSchaal) and THEN takes the day
                // fraction. Same unit -> factor null (no scale); convertible-different -> a factor; not convertible ->
                // marker. Mirrors DRL.
                boolean zelfdeEenheid = java.util.Objects.equals(baseType.eenheidOf(), doelType == null ? null : doelType.eenheidOf());
                // §4.22 the operand must be a RATE (eenheid per tijdseenheid): a unit mismatch is a §7.3.2 rate
                // conversion only when the base unit has a noemer; a non-rate mismatch (cm -> m) stays a marker. Mirrors DRL.
                boolean baseIsRate = baseType.eenheidOf() != null && baseType.eenheidOf().noemer() != null;
                BigDecimal fwd = (zelfdeEenheid || !baseIsRate) ? null : valueFactor(baseType, doelType);
                // §7.3.1 DIVISION: pick the EXACT integer direction (fewer sig digits) and divide by it — never the
                // pre-rounded reciprocal (1/12), which would diverge from ÷12 for ~13% of bases. Mirrors DrlTranspiler.
                BigDecimal inv = fwd == null ? null : valueFactor(doelType, baseType);
                boolean deel = inv != null && inv.precision() < fwd.precision();
                BigDecimal factor = fwd == null ? null : (deel ? inv : fwd);
                java.util.function.Function<Object, List<?>> cond = (zelfdeEenheid || factor != null) ? conditieTijdreeks(f.filter(), subject) : null;
                java.util.function.Predicate<Object> guard = cond == null ? null : mixedGuardWrite(f.filter(), subject);   // §8.4 static gate
                // §8.4.2 OR-mixed (ten minste 1: a temporal kenmerk OR a static comparison): conditieTijdreeks markers
                // the mixed OR, but tijdsevenredig's base bounds the static-true span. Mirrors DrlTranspiler.orMixedTijdsevenredig.
                OrMixed orMixed = (anker == null && cond == null && (zelfdeEenheid || factor != null)) ? orMixedTijdsevenredig(f.filter(), subject) : null;   // §3.8: OR-mixed stays a marker on an anchored doel (mirror DRL)
                if ((cond != null && guard != null) || orMixed != null) {
                    Class<?> segCls = segmentClass();
                    if (segCls != null) {
                        final String granName = gran.name();
                        final BiFunction<Object, Object, List<?>> br0 = baseReader;
                        final BigDecimal fFactor = factor;
                        final boolean fDeel = deel;
                        // scale the base by the unit factor when a rate conversion is needed (§7.3.1), else read it raw.
                        final BiFunction<Object, Object, List<?>> br = fFactor == null ? br0
                                : (s, gv) -> RegelSpraakRuntime.rsTijdSchaal(br0.apply(s, gv), fFactor, fDeel);
                        if (orMixed != null) {
                            // static TRUE -> prorate the base over its FULL extent (base AS its own all-true condition,
                            // rsTijdsevenredig reading the base's segments as true periods); static FALSE -> the temporal
                            // union only. Mirrors DrlTranspiler.orMixedTijdsevenredig.
                            final OrMixed om = orMixed;
                            return new TemporeleWaarde((s, gv) -> {
                                List<?> baseList = br.apply(s, gv);
                                return om.orGuard().test(s)
                                        ? RegelSpraakRuntime.rsTijdsevenredig(baseList, baseList, granName, segCls)
                                        : RegelSpraakRuntime.rsTijdsevenredig(om.unionCond().apply(s), baseList, granName, segCls);
                            }, baseGlobal);
                        }
                        final java.util.function.Predicate<Object> fGuard = guard;
                        if (anker != null)   // §3.8 anchored buckets (rsTijdsevenredigVanaf) — mirror DRL
                            return new TemporeleWaarde(
                                    (s, gv) -> fGuard.test(s)
                                            ? RegelSpraakRuntime.rsTijdsevenredigVanaf(cond.apply(s), br.apply(s, gv), granName,
                                                    (java.time.LocalDate) anker.apply(s), segCls)
                                            : new ArrayList<>(),
                                    baseGlobal);
                        return new TemporeleWaarde(
                                (s, gv) -> fGuard.test(s)
                                        ? RegelSpraakRuntime.rsTijdsevenredig(cond.apply(s), br.apply(s, gv), granName, segCls)
                                        : new ArrayList<>(),
                                baseGlobal);
                    }
                }
            }
        }
        // §5.1.4 PLUS/MIN over two temporal operands -> rsTijdReken per merged-knip period. §5.1.4.2/§4.1: SUM in the
        // LEFT operand's unit (restate the right into it), THEN restate the SUM ONCE to the doel unit — scaling each
        // operand to the doel FIRST double-rounds a non-terminating factor (review wf_115473d5-eee F1). Mirrors DRL.
        if (w instanceof Expr.Binair bin && (bin.op() == Expr.BinOp.PLUS || bin.op() == Expr.BinOp.MIN) && subject != null) {
            Datatype sumType = effType(bin.links());
            java.util.function.Function<Object, List<?>> l = temporeelRekenOperand(bin.links(), subject, gran, sumType);
            java.util.function.Function<Object, List<?>> rr = temporeelRekenOperand(bin.rechts(), subject, gran, sumType);
            Class<?> segCls = segmentClass();
            if (l != null && rr != null && segCls != null) {
                final java.util.function.Function<Object, List<?>> fl = l, fr = rr;
                final String opName = bin.op().name();
                java.util.function.Function<Object, List<?>> sum = s -> RegelSpraakRuntime.rsTijdReken(fl.apply(s), fr.apply(s), opName, segCls);
                java.util.function.Function<Object, List<?>> scaled = schaalTijd(sum, sumType, doelType);   // §4.1 restate the sum once
                if (scaled != null) return new TemporeleWaarde((s, gv) -> scaled.apply(s), null);
            }
        }
        return null;
    }

    /** §5.1.4 a temporal operand reader for a tijdsafhankelijke arithmetic expression — an own or to-one navigated
     *  SINGLE-period numeric temporal attribute COARSER-OR-EQUAL to the doel granularity, CONVERTIBLE to {@code unitTarget}
     *  (the sum unit). Returns a (subject -&gt; List&lt;Segment&gt;) reader (restated to unitTarget when the unit differs),
     *  or null (incompatible unit / finer granularity / not such an operand -> marker). Mirrors DRL temporeelRekenOperand. */
    private java.util.function.Function<Object, List<?>> temporeelRekenOperand(Expr e, Objecttype subject,
            RegelSpraakModel.Granulariteit gran, Datatype unitTarget) {
        // §5.1.4 an N-operand chain ("a plus b plus c" = Binair(Binair(a,b), c)): the operand is itself a temporal sub-sum.
        // Recurse — sum in the LEFT sub-operand's unit, then restate the sub-sum ONCE to unitTarget. Mirrors DRL.
        if (e instanceof Expr.Binair bin && (bin.op() == Expr.BinOp.PLUS || bin.op() == Expr.BinOp.MIN)) {
            Datatype subSum = effType(bin.links());
            java.util.function.Function<Object, List<?>> l = temporeelRekenOperand(bin.links(), subject, gran, subSum);
            java.util.function.Function<Object, List<?>> r = temporeelRekenOperand(bin.rechts(), subject, gran, subSum);
            Class<?> segCls = segmentClass();
            if (l == null || r == null || segCls == null) return null;
            final java.util.function.Function<Object, List<?>> fl = l, fr = r; final String opName = bin.op().name();
            java.util.function.Function<Object, List<?>> sum = s -> RegelSpraakRuntime.rsTijdReken(fl.apply(s), fr.apply(s), opName, segCls);
            return schaalTijd(sum, subSum, unitTarget);
        }
        if (!(e instanceof Expr.AttribuutRef a) || a.set() || a.type() == null || !a.type().isNumeriek()) return null;
        Objecttype ot; java.util.function.Function<Object, Object> reader;
        if (ongenavigeerd(a) && a.objecttype() == subject) {
            ot = subject; String attr = a.attribuut(); reader = s -> RegelSpraakRuntime.get(s, attr);
        } else if (!ongenavigeerd(a) && a.objecttype() != null) {
            java.util.function.Function<Object, Object> nav = navReader(a, subject);
            if (nav == null) return null;
            ot = a.objecttype(); reader = nav;
        } else return null;
        // §5.1.4.2 mixed granularity: the operand must be COARSER-OR-EQUAL to the doel (DAG<...<JAAR ordinal). Mirrors DRL.
        RegelSpraakModel.Granulariteit og = Namen.opNaam(ot.granulariteitLeden(), a.attribuut());
        if (og == null || og.ordinal() < gran.ordinal()) return null;
        if (Tijdlijnen.meerPeriodiek(ot.tijdlijnAantalLeden(), a.attribuut())) return null;   // multi-period -> out of scope
        final java.util.function.Function<Object, Object> fr = reader;
        return schaalTijd(s -> (List<?>) fr.apply(s), effType(a), unitTarget);   // §4.1 restate into the sum unit (null -> marker)
    }

    /** §4.1/§5.1.4.2 wrap a (subject -&gt; List&lt;Segment&gt;) timeline producer to restate it from {@code van}'s unit into
     *  {@code naar}'s (rsTijdSchaal by the EXACT-integer direction, ÷ when the inverse has fewer sig digits — like §7.3.1).
     *  Returns the producer UNCHANGED when the units are equal / no conversion is required; null when INCOMPATIBLE (caller
     *  markers). Mirrors DrlTranspiler.schaalTijd. */
    private java.util.function.Function<Object, List<?>> schaalTijd(java.util.function.Function<Object, List<?>> prod, Datatype van, Datatype naar) {
        Eenheid eu = van == null ? null : van.eenheidOf(), du = naar == null ? null : naar.eenheidOf();
        if (java.util.Objects.equals(eu, du)) return prod;
        if (eu == null || du == null || !eu.convertibleNaar(du)) return null;
        if (!eu.vereistOmrekening(du)) return prod;
        BigDecimal fwd = valueFactor(van, naar), inv = valueFactor(naar, van);
        boolean deel = inv != null && inv.precision() < fwd.precision();
        final BigDecimal factor = deel ? inv : fwd; final boolean fd = deel;
        return s -> RegelSpraakRuntime.rsTijdSchaal(prod.apply(s), factor, fd);
    }

    /** The TRUE-period {@code (subject -> List<Segment>)} timeline of a temporal WRITE-producer's condition. A single
     *  positive single-period subject kenmerk is its getter; a §8.4.2 pure-kenmerk compound is their combined
     *  timeline (ALLE = intersection via rsTijdKnip, "ten minste 1" = union via rsTijdUnie, both over the members'
     *  getters). null when unsupported (a negated / navigated / multi-period kenmerk, or a mixed-static / non-kenmerk
     *  / other-kwantor compound): a negated member has no standalone complement here (unlike the totaal CLIP, where
     *  the value bounds it), so the caller markers rather than mis-count. Mirrors DrlTranspiler.conditieTijdreeks. */
    private java.util.function.Function<Object, List<?>> conditieTijdreeks(Conditie filter, Objecttype subject) {
        if (filter instanceof Conditie.Kenmerk k) return temporeelKenmerkTijdreeks(k, subject);
        if (filter instanceof Conditie.Samengesteld s
                && (s.kwantor() == Conditie.Kwantor.ALLE
                    || (s.kwantor() == Conditie.Kwantor.TEN_MINSTE && s.drempel() == 1))) {
            boolean alle = s.kwantor() == Conditie.Kwantor.ALLE;
            List<java.util.function.Function<Object, List<?>>> regios = new ArrayList<>();
            for (Conditie c : s.onderdelen()) {
                // §8.4 a static comparison member is the value-side guard (the mixedGuardWrite gate), not part of the
                // timeline — admitted only in an ALLE compound (an OR-mixed write widens the span, which the producers
                // do not express, so it markers).
                if (c instanceof Conditie.Vergelijking) { if (!alle) return null; continue; }
                if (!(c instanceof Conditie.Kenmerk k)) return null;
                java.util.function.Function<Object, List<?>> g = temporeelKenmerkTijdreeks(k, subject);
                if (g == null) return null;   // a negated / navigated / multi-period member -> marker (no standalone complement)
                regios.add(g);
            }
            if (regios.isEmpty()) return null;
            if (regios.size() == 1) return regios.get(0);
            Class<?> segCls = segmentClass();
            if (segCls == null) return null;
            return s2 -> {
                List<?> acc = regios.get(0).apply(s2);
                for (int i = 1; i < regios.size(); i++)
                    acc = alle ? RegelSpraakRuntime.rsTijdKnip(acc, regios.get(i).apply(s2), segCls)
                               : RegelSpraakRuntime.rsTijdUnie(acc, regios.get(i).apply(s2), segCls);
                return acc;
            };
        }
        return null;
    }

    /** §8.4.2 the clip a (pure-kenmerk) compound gedurende applies to a value timeline: ALLE nests the per-kenmerk
     *  rsTijdKnip (negation per member); "ten minste 1" (drempel 1) clips the value to the UNION of each member's
     *  true-region (a negated member's region is the value's rsTijdKnipNiet complement, value-bounded). Returns a
     *  {@code (subject, value) -> clipped-value} function, or null when a member is not a single-period subject kenmerk
     *  or the kwantor is unsupported. Mirrors DrlTranspiler.samengesteldeTijdKnip. */
    private BiFunction<Object, List<?>, List<?>> kenmerkClip(List<Conditie.Kenmerk> leden, Conditie.Kwantor kw, int drempel,
                                                             Objecttype subject, Class<?> segCls) {
        if (leden.isEmpty()) return null;
        for (Conditie.Kenmerk k : leden)
            if (k.navigatie() != null || subject == null || !Namen.bevatNaam(subject.kenmerkGranulariteitLeden(), k.kenmerk())) return null;
        if (kw == Conditie.Kwantor.ALLE) {
            return (sub, value) -> {
                List<?> r = value;
                for (Conditie.Kenmerk k : leden) {
                    List<?> g = (List<?>) RegelSpraakRuntime.get(sub, k.kenmerk());
                    r = k.negatie() ? RegelSpraakRuntime.rsTijdKnipNiet(r, g, segCls)
                                    : RegelSpraakRuntime.rsTijdKnip(r, g, segCls);
                }
                return r;
            };
        }
        if (kw == Conditie.Kwantor.TEN_MINSTE && drempel == 1) {
            return (sub, value) -> {
                List<?> unie = lidSteun(sub, value, leden.get(0), segCls);
                for (int i = 1; i < leden.size(); i++)
                    unie = RegelSpraakRuntime.rsTijdUnie(unie, lidSteun(sub, value, leden.get(i), segCls), segCls);
                return RegelSpraakRuntime.rsTijdKnip(value, unie, segCls);
            };
        }
        return null;
    }

    /** One OR-member's true-region for the union: a positive member is its kenmerk getter; a negated member is the
     *  value clipped to the kenmerk's FALSE periods (value-bounded complement). Mirrors DrlTranspiler.lidSteun. */
    private static List<?> lidSteun(Object sub, List<?> value, Conditie.Kenmerk k, Class<?> segCls) {
        List<?> g = (List<?>) RegelSpraakRuntime.get(sub, k.kenmerk());
        return k.negatie() ? RegelSpraakRuntime.rsTijdKnipNiet(value, g, segCls) : g;
    }

    /** The two pieces of a lowered OR-mixed tijdsevenredig: the temporal union's true-period timeline (the static-FALSE
     *  condition) and the static members' OR gate (true -> widen to the base's full extent). */
    private record OrMixed(java.util.function.Function<Object, List<?>> unionCond, java.util.function.Predicate<Object> orGuard) {}

    /** §8.4.2 lower an OR-mixed ("ten minste 1": a temporal kenmerk OR a static comparison) gating the §13.4.17.58
     *  tijdsevenredig WRITE producer. A TRUE static member satisfies the OR for the WHOLE span; tijdsevenredig's base
     *  bounds that span, so static TRUE prorates the base over its full extent (base AS its own all-true condition) and
     *  static FALSE prorates over the temporal union only. Returns the (union-cond, OR-guard) pair, or null when the
     *  compound is not a temporal+static OR-mix or a member is not lowerable. ONLY tijdsevenredig lowers an OR-mix:
     *  §7.2 aantal-dagen-in has no value base, so its result is bounded only by the condition's true-periods (§7.2 line
     *  3005's "zeer groot aantal resultaten" caveat) -> a static-TRUE OR is unbounded and stays an honest marker.
     *  Mirrors DrlTranspiler.orMixedTijdsevenredig. */
    private OrMixed orMixedTijdsevenredig(Conditie filter, Objecttype subject) {
        if (!(filter instanceof Conditie.Samengesteld s) || s.kwantor() != Conditie.Kwantor.TEN_MINSTE || s.drempel() != 1)
            return null;
        List<Conditie> temporeel = new ArrayList<>();
        List<Conditie.Vergelijking> statisch = new ArrayList<>();
        for (Conditie c : s.onderdelen()) {
            if (c instanceof Conditie.Kenmerk) temporeel.add(c);
            else if (c instanceof Conditie.Vergelijking v) statisch.add(v);
            else return null;   // an unsupported member type -> marker
        }
        if (temporeel.isEmpty() || statisch.isEmpty()) return null;   // not a temporal+static mixed compound
        Conditie.Samengesteld temporeelDeel = new Conditie.Samengesteld(s.kwantor(), s.drempel(), temporeel, s.bron());
        java.util.function.Function<Object, List<?>> unionCond = conditieTijdreeks(temporeelDeel, subject);
        if (unionCond == null) return null;   // a negated / navigated / multi-period temporal member -> marker
        java.util.function.Predicate<Object> orGuard = statischeGuard(statisch, subject, false);   // OR
        if (orGuard == null) return null;     // a static member we cannot lower -> marker
        return new OrMixed(unionCond, orGuard);
    }

    /** §8.4 the value-side static guard for a temporal-WRITE compound: the AND of the non-temporal comparison members
     *  (only an ALLE compound admits statics on the write side). Returns a {@code (subject -> Boolean)} gate (a
     *  constant-true gate when there are no statics), or null when a static member is not lowerable (caller skips). */
    private java.util.function.Predicate<Object> mixedGuardWrite(Conditie filter, Objecttype subject) {
        if (!(filter instanceof Conditie.Samengesteld s)) return o -> true;
        List<Conditie.Vergelijking> statisch = new ArrayList<>();
        for (Conditie c : s.onderdelen()) if (c instanceof Conditie.Vergelijking v) statisch.add(v);
        if (statisch.isEmpty()) return o -> true;
        return statischeGuard(statisch, subject, true);   // AND (an ALLE compound — the OR-mixed write markers in conditieTijdreeks)
    }

    /** §8.4 combine the static comparison members into one {@code (subject -> Boolean)} gate — the AND ({@code and}) for
     *  an ALLE compound, the OR for a "ten minste 1". null when any member is not lowerable. Mirrors DrlTranspiler.statischeGuard. */
    private java.util.function.Predicate<Object> statischeGuard(List<Conditie.Vergelijking> leden, Objecttype subject, boolean and) {
        List<java.util.function.Predicate<Object>> ps = new ArrayList<>();
        for (Conditie.Vergelijking v : leden) {
            java.util.function.Predicate<Object> p = statischeVergelijking(v, subject);
            if (p == null) return null;
            ps.add(p);
        }
        return s -> {
            for (java.util.function.Predicate<Object> p : ps) {
                boolean r = p.test(s);
                if (and && !r) return false;
                if (!and && r) return true;
            }
            return and;
        };
    }

    /** §8.4 one non-temporal comparison member as a value-side {@code (subject -> Boolean)} predicate, with the §5.1
     *  comparison-matrix leegwaarde semantics. A NUMERIC comparison reuses {@link #vergelijk} (which already matches
     *  the value-side rule: GELIJK both-empty true, ONGELIJK gevuld-vs-empty true, ordering onwaar if either empty). A
     *  DATUM ordering (eerder/later) is true only when both operands are present (an empty operand -> onwaar). null
     *  when the operand is temporal / navigated / not an own attribute vs a literal/own-attribute, or the op/operand is
     *  out of scope. Mirrors DrlTranspiler.statischeVergelijkingGuard. */
    private java.util.function.Predicate<Object> statischeVergelijking(Conditie.Vergelijking v, Objecttype subject) {
        if (!(v.links() instanceof Expr.AttribuutRef la) || !ongenavigeerd(la)) return null;
        if (subject != null && Namen.bevatNaam(subject.granulariteitLeden(), la.attribuut())) return null;   // a temporal operand is no static member
        String lattr = la.attribuut();
        if (isNumeriekOp(v.op())) {
            Conditie.Operator op = v.op();
            if (v.rechts() instanceof Expr.Literal lit && lit.type().isNumeriek()) {
                BigDecimal rhs = bd(lit);
                if (rhs == null) return null;
                BigDecimal[] f = restatement(effType(la), effType(lit));
                BigDecimal rhsS = scale(rhs, f[1]);
                return s -> vergelijk(scale(num(s, lattr), f[0]), rhsS, op);
            }
            if (v.rechts() instanceof Expr.AttribuutRef ra && ongenavigeerd(ra)) {
                if (subject != null && Namen.bevatNaam(subject.granulariteitLeden(), ra.attribuut())) return null;
                String rattr = ra.attribuut();
                BigDecimal[] f = restatement(effType(la), effType(ra));
                return s -> vergelijk(scale(num(s, lattr), f[0]), scale(num(s, rattr), f[1]), op);
            }
            return null;
        }
        if (isDatumOp(v.op()) && la.type().isDatum() && v.rechts() instanceof Expr.Literal lit && lit.type().isDatum()) {
            if (!sameDatumKind(la.type(), lit.type())) return null;   // mixed datum/datum-tijd -> §5.3 Tabel 36 mismatch, skip
            Object rhs = datumLiteraal(lit);
            if (rhs == null) return null;
            Conditie.Operator op = v.op();
            // read the raw Temporal (LocalDate OR LocalDateTime); a present Datum-en-tijd now compares via compareTo (was
            // `instanceof LocalDate` -> read as leeg -> wrong no-fire). An empty datum operand -> ordering no-match.
            return s -> datumVergelijkTemporal(datumWaarde(RegelSpraakRuntime.get(s, lattr)), rhs, op);
        }
        return null;
    }
    private static boolean isDatumOp(Conditie.Operator op) {
        return switch (op) {
            case EERDER, EERDER_OF_GELIJK, LATER, LATER_OF_GELIJK -> true;
            default -> false;
        };
    }
    /** Parse a §13.2.19 date literal to a LocalDate — the shared {@link Literalen} reader (single shared implementation). */
    private static java.time.LocalDate parseDatum(String waarde) { return Literalen.parseDatum(waarde); }
    /** Parse a §13.2.21 datum-tijd literal to a LocalDateTime — the shared {@link Literalen} reader (single shared implementation). */
    private static java.time.LocalDateTime parseDatumTijd(String waarde) { return Literalen.parseDatumTijd(waarde); }

    /** §13.3.8 the Java class a scalar parameter's global is declared with — the {@link org.drools.model.Global} type
     *  must match the value the session setGlobal supplies, else Drools throws "Illegal class for global" at fire time.
     *  Mirrors DrlTranspiler.javaType (Datum -> LocalDate, DatumTijd -> LocalDateTime, Tekst -> String, Booleaan -> Boolean). */
    private static Class<?> globalKlasse(Datatype t) {
        if (t instanceof Datatype.DatumInDagen) return java.time.LocalDate.class;
        if (t instanceof Datatype.DatumTijd) return java.time.LocalDateTime.class;
        if (t.isTekst()) return String.class;
        if (t instanceof Datatype.Booleaan) return Boolean.class;
        return Object.class;
    }

    /** A single-entry global binding map that — unlike {@link Map#of}, which rejects a null value with an NPE — TOLERATES
     *  a null: an unset / §13.3.8 leeg numeric Parameter binds to a null global value, and the downstream value-fns
     *  (rsDatumReken treats a leeg duration as 0, rsGeheelPeriode null-guards its grens, ...) already carry the §6/§8
     *  leegwaarde rules. {@code Map.of(k, null)} would throw a ConstraintEvaluationException out of fireAllRules — the
     *  DRL constraint passes the null global straight through, so the model must too. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Map<String, BigDecimal> bindGlobal(String naam, Object waarde) {
        // no BigDecimal cast: the §5.3 "rekendatum" pseudo-slot carries a LocalDate through the (erased)
        // map; every numeric reader casts per name, so the loose value type is safe.
        return (Map) java.util.Collections.singletonMap(naam, waarde);
    }

    /** The Global a value/condition fn's named slot binds to: the §4.2 rekendatum LocalDate global for the
     *  §5.3 "rekendatum" pseudo-name, else the numeric Parameter global. null -> honest skip. */
    private Global<?> waardeGlobal(String gn) {
        return "rekendatum".equals(gn) ? rekendatumGlobal : globals.get(gn);
    }

    /** Fill a consequence/eval body's name->value map WITHOUT the BigDecimal cast (see {@link #bindGlobal}). */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void vulGlobalMap(Map<String, BigDecimal> gm, List<String> namen, Object[] args, int offset) {
        for (int i = 0; i < namen.size(); i++) ((Map) gm).put(namen.get(i), args[offset + i]);
    }

    /** §8.1.1 a constraint reading 2..3 DISTINCT numeric Parameter-globals — the case the single-global pattern.expr
     *  cannot host: two global {@link Variable}s bound in ONE exec-model expr collapse to the LAST global's value (a
     *  Drools exec-model quirk). Work around it by binding EACH distinct global into its OWN declaration, sourced via
     *  {@link DSL#from} from the (always-present) SUBJECT crossed with the global, mapping the value to an
     *  {@link Optional}. Distinct declarations do NOT collapse, and the subject anchor keeps the source NON-EMPTY even
     *  when the global is LEEG (unset/null) — a bare {@code from(global)} yields nothing for a null global, which would
     *  wrongly drop the rule (the single-global path is null-tolerant, so this must be too). The comparison rides the
     *  LAST from-declaration's pattern, referencing the already-bound subject + the earlier globals; every item goes in
     *  {@code extra} (after the subject pattern), so no pre-ordering is needed. Returns null for &lt;2 / &gt;3 distinct
     *  globals (the single-global path / an honest skip) or any non-numeric global. {@code eval} takes (subject,
     *  globalName-&gt;value map, null for a leeg global) and decides the comparison via the same scalarWaarde fns as the
     *  single-global path (which carry the §6/§5 leegwaarde rules). DRL reads each global as a free mvel name. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private Gate nGlobalGate(List<String> gns, PatternDSL.PatternDef subjectPattern, Variable subjectVar,
                             java.util.function.BiFunction<Object, Map<String, BigDecimal>, Boolean> eval) {
        int n = gns.size();
        if (n < 2 || n > 3) return null;   // 1 -> single-global path; >3 -> honest skip (rare)
        List<Variable> dvs = new ArrayList<>();
        List<RuleItemBuilder<?>> extra = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            Global<?> g = waardeGlobal(gns.get(i));   // numeric Parameter or the §5.3 rekendatum pseudo-slot
            if (g == null) return null;   // a non-numeric (date/tekst) global -> not bindable here -> skip
            Variable<java.util.Optional> dv = DSL.declarationOf(java.util.Optional.class, "$gg_" + (seq++),
                    DSL.from((Variable) subjectVar, (Variable) g,
                            (Function2) (subj, gv) -> java.util.Optional.ofNullable(gv)));
            dvs.add(dv);
            if (i < n - 1) extra.add(PatternDSL.pattern(dv));   // bare binders for all but the last
        }
        Variable last = dvs.get(n - 1);
        if (n == 2) {
            Variable d0 = dvs.get(0);
            extra.add(PatternDSL.pattern(last).expr(nextId("c"), (Variable) subjectVar, (Variable) d0,
                (Predicate3) (lo, subj, o0) -> eval.apply(subj, gmap(gns, unwrapGlobal(o0), unwrapGlobal(lo)))));
        } else {   // n == 3
            Variable d0 = dvs.get(0), d1 = dvs.get(1);
            extra.add(PatternDSL.pattern(last).expr(nextId("c"), (Variable) subjectVar, (Variable) d0, (Variable) d1,
                (Predicate4) (lo, subj, o0, o1) -> eval.apply(subj, gmap(gns, unwrapGlobal(o0), unwrapGlobal(o1), unwrapGlobal(lo)))));
        }
        return new Gate(subjectPattern, extra);
    }
    /** Unwrap an {@link Optional} bound global value, or null for a LEEG (empty) global. Loosely typed (Object):
     *  the numeric readers cast per name, and the §5.3 "rekendatum" pseudo-slot carries a LocalDate. */
    private static Object unwrapGlobal(Object opt) {
        return opt == null ? null : ((java.util.Optional<?>) opt).orElse(null);
    }
    /** Build the globalName-&gt;value map for {@link #nGlobalGate}: {@code gns} are the global names in declaration
     *  order, {@code vals} the matching (possibly-null/leeg) values — the LAST value is the comparison-pattern's own
     *  variable. A null value is a §13.3.8 leeg global, which the scalarWaarde fns then handle per §6/§5. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Map<String, BigDecimal> gmap(List<String> gns, Object... vals) {
        Map m = new java.util.LinkedHashMap<>();
        for (int i = 0; i < gns.size(); i++) m.put(gns.get(i), vals[i]);
        return m;
    }

    /** §6/§13.4.16 date arithmetic as a value: a datum/datumtijd ± a numeric-with-time-unit -> a new date(time) via
     *  {@link RegelSpraakRuntime#rsDatumReken} (optellen=PLUS=later; MIN/VERMINDERD_MET=earlier; aantal 0 -> unchanged;
     *  leeg operand -> leeg). Mirrors DrlTranspiler.binairTerm's datum branch and its guards: only +/-, no datum-datum,
     *  a time unit (uur/…) needs a datum-tijd operand, kwartaal lowers (3×MONTHS via rsDatumReken), and a
     *  dagsoort/unresolvable operand -> null (skip). */
    private WaardeFn datumRekenWaarde(Objecttype valueSubject, Expr.Binair b) {
        Expr.BinOp op = b.op();
        if (op != Expr.BinOp.PLUS && op != Expr.BinOp.MIN && op != Expr.BinOp.VERMINDERD_MET) return null;
        boolean datumLinks = b.links().type().isDatum();
        Expr datumExpr = datumLinks ? b.links() : b.rechts();
        Expr durExpr   = datumLinks ? b.rechts() : b.links();
        boolean optellen = op == Expr.BinOp.PLUS;
        if (durExpr.type().isDatum() || (!optellen && !datumLinks)) return null;   // datum-datum / duur-min-datum
        // §6.11 datum ± N <dagsoort> ("5 werkdagen"): step day-by-day counting the §9.8 dagsoort days. A dagsoort is
        // day-granular -> a Datum in dagen (LocalDate) operand only. Both back-ends call rsDatumPlusDagsoort (single-runtime principle).
        // ANNOTATION BY DESIGN (WF-035, RUNTIME-SEMANTICS ch. 2.10 exception 2): the duration operand's calendar
        // unit is what routes date arithmetic, and ref routes on the same static type — deriving it here would
        // split the two. scripts/check-reftype-derivation.sh leaves this position untwisted for that reason.
        io.github.yhavinga.regelspraak.model.Eenheid durEenheid = durExpr.type().eenheidOf();
        // ch. 3.8 puts the CALENDAR TABLE first — see Tijdrekening.dagsoortVoorEenheid for the order and why.
        String dagsoortUnit = Tijdrekening.dagsoortVoorEenheid(model, durEenheid);
        if (dagsoortUnit != null) {
            // §4.1/§4.2 Tabel 2/5 rule 2: a datum-EN-tijd left operand yields a datum-en-tijd result — step over the date
            // part and re-attach the time-of-day (rsDatumTijdPlusDagsoort). A Datum-in-dagen keeps the LocalDate path.
            boolean datumTijd = datumExpr.type() instanceof Datatype.DatumTijd;
            java.util.function.Predicate<java.time.LocalDate> pred = dagsoortPredikaat(dagsoortUnit);
            WaardeFn df0 = scalarWaarde(valueSubject, datumExpr), nf0 = scalarWaarde(valueSubject, durExpr);
            if (pred == null || df0 == null || nf0 == null) return null;
            BiFunction<Object, Map<String, BigDecimal>, Object> dfn0 = df0.fn(), nfn0 = nf0.fn();
            Set<String> g0 = new LinkedHashSet<>(df0.globals()); g0.addAll(nf0.globals());
            if (datumTijd)
                return new WaardeFn((s, gm) -> RegelSpraakRuntime.rsDatumTijdPlusDagsoort(
                        (java.time.LocalDateTime) dfn0.apply(s, gm), (BigDecimal) nfn0.apply(s, gm), pred, optellen), g0);
            return new WaardeFn((s, gm) -> RegelSpraakRuntime.rsDatumPlusDagsoort(
                    (java.time.LocalDate) dfn0.apply(s, gm), (BigDecimal) nfn0.apply(s, gm), pred, optellen), g0);
        }
        String unit = Tijdrekening.chronoUnit(durExpr.type());
        if (unit == null) return null;
        boolean tijdEenheid = switch (unit) { case "HOURS", "MINUTES", "SECONDS", "MILLIS" -> true; default -> false; };
        if (tijdEenheid && !(datumExpr.type() instanceof Datatype.DatumTijd)) return null;
        WaardeFn df = scalarWaarde(valueSubject, datumExpr), nf = scalarWaarde(valueSubject, durExpr);
        if (df == null || nf == null) return null;
        BiFunction<Object, Map<String, BigDecimal>, Object> dfn = df.fn(), nfn = nf.fn();
        Set<String> g = new LinkedHashSet<>(df.globals()); g.addAll(nf.globals());
        return new WaardeFn((s, gm) -> RegelSpraakRuntime.rsDatumReken(
                (java.time.temporal.Temporal) dfn.apply(s, gm), (BigDecimal) nfn.apply(s, gm), unit, optellen), g);
    }

    /** A single positive single-period subject kenmerk's TRUE-period reader ({@code s -> the kenmerk's List<Segment>}),
     *  or null when not lowerable as a standalone true-period timeline (a navigated / negated / multi-period /
     *  non-subject kenmerk — a negated member has no standalone complement here, so the caller markers). Mirrors
     *  DrlTranspiler.temporeelKenmerkTijdreeks. */
    private java.util.function.Function<Object, List<?>> temporeelKenmerkTijdreeks(Conditie.Kenmerk k, Objecttype subject) {
        if (k.navigatie() != null || k.negatie() || subject == null
                || !Namen.bevatNaam(subject.kenmerkGranulariteitLeden(), k.kenmerk())
                || Tijdlijnen.meerPeriodiek(subject.kenmerkTijdlijnAantalLeden(), k.kenmerk())) return null;
        String kenmerk = k.kenmerk();
        return s -> (List<?>) RegelSpraakRuntime.get(s, kenmerk);
    }

    /** §8.2/§7.1 the boolean-timeline reader for a to-ONE NAVIGATED temporal kenmerk ("zijn partner gedurende het gehele
     *  jaar actief is"): the role's kenmerk {@code List<Segment>}, read ROOT-GUARDED (a null relation -&gt; leeg timeline
     *  -&gt; rsGeheelPeriode vacuously true, matching the DRL `root == null ? null : root.getKenmerk()`). null for a
     *  negated / to-many / non-temporal navigated kenmerk. Mirrors DrlTranspiler.navigatieKenmerkTijdreeks. */
    private java.util.function.Function<Object, List<?>> navigatieKenmerkTijdreeks(Conditie.Kenmerk k, Objecttype subject) {
        if (k.navigatie() == null || k.negatie() || subject == null) return null;
        KenmerkHop hop = kenmerkHop(k, subject);
        if (hop == null) return null;   // the bound hop does not fit this subject
        Objecttype doel = hop.doel();
        if (doel == null || !Namen.bevatNaam(doel.kenmerkGranulariteitLeden(), k.kenmerk())
                || Tijdlijnen.meerPeriodiek(doel.kenmerkTijdlijnAantalLeden(), k.kenmerk())) return null;
        // §13.3.9: the hop is a two-step read like navReader's, on the timeline arm and with its own resolution —
        // so it needs the same question. Measured 2026-09-11 on a verifier-clean, ref-clean model (a Schuit with a
        // Numeriek `werf` beside the to-one role of that name, a §8.2 "de werf van de Schuit gedurende het gehele
        // jaar bedrijvig is"): ref and clingo both PASS with toeslag leeg, while this branch died at first fire
        // (ERROR ConstraintEvaluationException — the kenmerk getter read off a BigDecimal) and the DRL branch sank
        // the package on "Unable to Analyse Expression rsGeheelPeriode(($subject.getWerf() == null ? …".
        if (!heeftVeld("genavigeerde kenmerktijdreeks", subject, hop.rol(), doel)) return null;
        String rol = hop.rol(), kenmerk = k.kenmerk();
        return s -> { Object rel = RegelSpraakRuntime.get(s, rol); return rel == null ? null : (List<?>) RegelSpraakRuntime.get(rel, kenmerk); };
    }

    /** §8.4.3 a periodevergelijking bound as a subject→LocalDate reader — a §13.2.19 datum literal (constant) or
     *  the subject's own scalar datum attribute (a leeg attribute reads null → the period test fails, matching the
     *  DRL null-guard). null for an absent bound OR an unsupported shape (the shared
     *  {@link Tijdrekening#periodeGrensLowerbaar} gate — the caller distinguishes by checking the Expr). */
    private java.util.function.Function<Object, java.time.LocalDate> periodeGrensLezer(Expr grens, Objecttype subject) {
        if (grens == null || !Tijdrekening.periodeGrensLowerbaar(grens, subject)) return null;
        if (grens instanceof Expr.Literal lit) {
            java.time.LocalDate d = Literalen.parseDatum(lit.waarde());
            return d == null ? null : s -> d;
        }
        String attr = ((Expr.AttribuutRef) grens).attribuut();
        return s -> RegelSpraakRuntime.get(s, attr) instanceof java.time.LocalDate d ? d : null;
    }

    /** RUNTIME-SEMANTICS ch. 3.10's scalar gate as a (subject, rekendatum) test: vanaf inclusive, tot exclusive,
     *  "tot en met" inclusive, an absent bound open, an unset rekendatum or a leeg bound never matching. ONE
     *  reading for every position that carries a window — the rule's condition, and the toets and filter of an
     *  ALLE quantifier fold, where {@code subject} is the ELEMENT and an attribute bound is therefore read on each
     *  contributor. null when a bound shape is not carried ({@link #periodeGrensLezer}). */
    private Predicate2<Object, Object> vensterToets(Conditie.Periode per, Objecttype subject) {
        java.util.function.Function<Object, java.time.LocalDate> vanF = periodeGrensLezer(per.vanaf(), subject);
        java.util.function.Function<Object, java.time.LocalDate> totF = periodeGrensLezer(per.tot(), subject);
        if ((per.vanaf() != null && vanF == null) || (per.tot() != null && totF == null)) return null;
        boolean inclusief = per.totInclusief();
        return (s, rd) -> {
            if (!(rd instanceof java.time.LocalDate d)) return false;   // unset rekendatum -> no match
            if (vanF != null) { java.time.LocalDate v = vanF.apply(s); if (v == null || d.isBefore(v)) return false; }
            if (totF != null) { java.time.LocalDate t = totF.apply(s); if (t == null) return false;
                                if (inclusief ? d.isAfter(t) : !d.isBefore(t)) return false; }
            return true;
        };
    }

    // ---------------- §8.2 / §13.4.15.70 gehele-periodevergelijking (mirrors DrlTranspiler.geheelPeriodeCall) ------
    /** The §8.2 whole-period qualifier on a predicate, or null when it carries none. */
    private static Conditie.GeheelPeriode geheelPeriodeVan(Conditie c) {
        if (c instanceof Conditie.Vergelijking v) return v.periode();
        if (c instanceof Conditie.Predicaat p)    return p.periode();
        if (c instanceof Conditie.Kenmerk k)      return k.periode();
        return null;
    }

    /** Whether an AttribuutRef reads a tijdsafhankelijk attribuut (its field is a List&lt;Segment&gt; timeline). */
    private static boolean isTemporeel82(Expr.AttribuutRef a) {
        return a.objecttype() != null && Namen.bevatNaam(a.objecttype().granulariteitLeden(), a.attribuut());
    }

    /** The §8.2 timeline reader for a temporal attribute — the subject's own List&lt;Segment&gt;, or a to-ONE
     *  navigated partner's (null reference -> null timeline -> rsGeheelPeriode returns true). null for a to-many /
     *  unresolvable navigation. Mirrors DrlTranspiler.temporeelTijdlijnGetter. */
    private java.util.function.Function<Object, Object> temporeelTijdlijnReader(Expr.AttribuutRef a, Objecttype subject) {
        if (ongenavigeerd(a)) {
            if (subject == null || !subject.heeftLid(a.attribuut())) return null;
            String attr = a.attribuut();
            return s -> RegelSpraakRuntime.get(s, attr);
        }
        String rol = navComparisonRole(a, subject);
        if (rol == null) return null;
        String attr = a.attribuut();
        return s -> { Object rel = RegelSpraakRuntime.get(s, rol); return rel == null ? null : RegelSpraakRuntime.get(rel, attr); };
    }

    /** A resolved §8.2 whole-period predicate: the subject-&gt;timeline reader, the toets code, the grenswaarde
     *  (null for gevuld/kenmerk), the granularity, and the NIET flag. */
    private record Gp82(java.util.function.Function<Object, Object> timeline, int toets, WaardeFn grens, String gran, boolean niet) {}

    /** Build the §8.2 model representation of a whole-period-qualified predicate, or null when outside the supported
     *  set (mirrors DrlTranspiler.geheelPeriodeCall exactly): a numeric comparison of a temporal attribute vs a scalar
     *  grens, a GEVULD over a temporal attribute, or a temporal kenmerkcheck; a mixed-unit comparison / DATUM ordering
     *  / LEEG / ELFPROEF / NUMERIEK-met-exact whole-period form -> null (an honest skip, symmetric with the DRL marker). */
    private Gp82 geheelPeriode82(Conditie c, Conditie.GeheelPeriode gp, Objecttype subject) {
        String gran = gp.jaar() ? "JAAR" : "MAAND";
        if (c instanceof Conditie.Vergelijking v) {
            Expr.AttribuutRef tijd; Expr ander; boolean linksTemp;
            if (v.links() instanceof Expr.AttribuutRef la && isTemporeel82(la))       { tijd = la; ander = v.rechts(); linksTemp = true; }
            else if (v.rechts() instanceof Expr.AttribuutRef ra && isTemporeel82(ra)) { tijd = ra; ander = v.links();  linksTemp = false; }
            else return null;
            if (!(tijd.type().isNumeriek() || tijd.type().isPercentage())) return null;
            io.github.yhavinga.regelspraak.model.Eenheid el = eff.eenheidVan(tijd), er = eff.eenheidVan(ander);
            // §8.2 cross-unit grens: restate it from er into the timeline's unit el via the EXACT factor (mirrors DRL
            // herschaalExact); an OPAQUE product unit (no exact factors) stays a marker (symmetric with DRL).
            boolean crossUnit = el != null && er != null && el.vereistOmrekening(er);
            if (crossUnit && er.exacteHerschaalFactoren(el) == null) return null;
            Integer code = Tijdrekening.toets(linksTemp ? v.op() : Operatoren.omkeren(v.op()));   // REVERSE (operand swap), not negate
            if (code == null) return null;
            java.util.function.Function<Object, Object> tl = temporeelTijdlijnReader(tijd, subject);
            if (tl == null) return null;
            WaardeFn g = scalarWaarde(null, ander);
            if (g == null) {
                // scalarWaarde's navigated branch is value-path-only (valueSubject is null here), so a to-ONE navigated
                // grenswaarde ("… gelijk is aan de drempel van zijn partner") would skip while the DRL oracle FIRES it via
                // partner!.attr. Lower it with the same condition-aware null-safe role reader the timeline side uses, so
                // both back-ends agree (a null relation -> null grens -> the §8.2 check fails, matching mvel's partner!.attr).
                if (ander instanceof Expr.AttribuutRef ar && !ongenavigeerd(ar) && (ar.type().isNumeriek() || ar.type().isPercentage())) {
                    String rol = navComparisonRole(ar, subject);
                    if (rol != null) {
                        String gattr = ar.attribuut();
                        g = new WaardeFn((s, gm) -> { Object rel = RegelSpraakRuntime.get(s, rol); return rel == null ? null : RegelSpraakRuntime.get(rel, gattr); }, java.util.Set.of());
                    }
                }
                if (g == null) return null;
            }
            if (crossUnit) g = g.herschaald(effType(ander), effType(tijd), "een §8.2 tijdlijngrens");   // §4 EXACT restate the grens into the timeline's unit
            return new Gp82(tl, code, g, gran, gp.niet());
        }
        if (c instanceof Conditie.Predicaat p && p.soort() == Conditie.Predicaatsoort.GEVULD
                && p.operand() instanceof Expr.AttribuutRef pa && isTemporeel82(pa)) {
            java.util.function.Function<Object, Object> tl = temporeelTijdlijnReader(pa, subject);
            if (tl == null) return null;
            return new Gp82(tl, 6, null, gran, gp.niet());
        }
        if (c instanceof Conditie.Predicaat p && p.soort() == Conditie.Predicaatsoort.LEEG
                && p.operand() instanceof Expr.AttribuutRef pa && isTemporeel82(pa)) {
            // §8.2 line 3468 "... gedurende de gehele maand leeg is": the inverse of GEVULD (toets 8 in rsGeheelPeriode).
            java.util.function.Function<Object, Object> tl = temporeelTijdlijnReader(pa, subject);
            if (tl == null) return null;
            return new Gp82(tl, 8, null, gran, gp.niet());
        }
        if (c instanceof Conditie.Kenmerk k) {
            java.util.function.Function<Object, List<?>> tl = temporeelKenmerkTijdreeks(k, subject);
            if (tl == null) tl = navigatieKenmerkTijdreeks(k, subject);   // §8.2 over a to-one NAVIGATED temporal kenmerk
            if (tl == null) return null;
            final java.util.function.Function<Object, List<?>> ftl = tl;
            return new Gp82(s -> ftl.apply(s), 7, null, gran, gp.niet());
        }
        return null;
    }

    /** §8.2 over a NAVIGATED to-many temporal SET: "de &lt;temporeel attr&gt; van alle &lt;naar-veel-rol&gt; ... gedurende
     *  het gehele jaar|maand &lt;op&gt; &lt;scalar grens&gt;" (or GEVULD/LEEG over the set) — the check holds iff it holds
     *  for EVERY element's timeline (a forall). A collectList accumulate binds a {@code List<List<Segment>>}; an
     *  rsGeheelPeriodeAlle result-var eval decides it. Returns a Gate when fully lowered, or null (fall through: not a set
     *  form, or an unlowerable set form the caller then skips via geheelPeriode82). A timeline-vs-timeline set comparison
     *  is unlowerable -> null -> skip (symmetric with the DRL marker). Mirrors DrlTranspiler.geheelPeriodeAlleVoorwaarde. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private Gate geheelPeriodeAlleVoorwaarde(Conditie c, Conditie.GeheelPeriode gp, PatternDSL.PatternDef pattern,
            Objecttype subject, Variable subjectVar) {
        String gran = gp.jaar() ? "JAAR" : "MAAND";
        Expr.AttribuutRef tijd; int toets; WaardeFn grensFn = null; Datatype tijdType;
        if (c instanceof Conditie.Vergelijking v) {
            Expr ander; boolean linksTemp;
            if (v.links() instanceof Expr.AttribuutRef la && la.set() && isTemporeelSet82(la))      { tijd = la; ander = v.rechts(); linksTemp = true; }
            else if (v.rechts() instanceof Expr.AttribuutRef ra && ra.set() && isTemporeelSet82(ra)) { tijd = ra; ander = v.links();  linksTemp = false; }
            else return null;   // not a temporal-set comparison -> scalar path
            if (!(tijd.type().isNumeriek() || tijd.type().isPercentage())) return null;
            if (ander instanceof Expr.AttribuutRef ar && isTemporeel82(ar)) return null;   // timeline-vs-timeline -> skip (symmetric marker)
            Eenheid el = eff.eenheidVan(tijd), er = eff.eenheidVan(ander);
            boolean crossUnit = el != null && er != null && el.vereistOmrekening(er);
            if (crossUnit && er.exacteHerschaalFactoren(el) == null) return null;
            Integer code = Tijdrekening.toets(linksTemp ? v.op() : Operatoren.omkeren(v.op()));
            if (code == null) return null;
            // The grens rides an eval AFTER the collectList accumulate (no $subject bound on the collected-list pattern),
            // so restrict to a LITERAL or a scalar Parameter-GLOBAL — an own/navigated grens stays a symmetric marker
            // (matching the DRL set §8.2 guard, which also declines a $subject-qualified grens here).
            if (!(ander instanceof Expr.Literal) && !(ander instanceof Expr.ParameterRef pr && pr.parameter().granulariteit() == null))
                return null;
            WaardeFn g = scalarWaarde(null, ander);
            if (g == null) return null;   // unlowerable grens -> skip (symmetric marker)
            if (crossUnit) g = g.herschaald(effType(ander), effType(tijd), "een §8.2 tijdlijngrens");
            toets = code; grensFn = g; tijdType = tijd.type();
        } else if (c instanceof Conditie.Predicaat p && p.soort() == Conditie.Predicaatsoort.GEVULD
                && p.operand() instanceof Expr.AttribuutRef pa && pa.set() && isTemporeelSet82(pa)) {
            tijd = pa; toets = 6; tijdType = pa.type();
        } else if (c instanceof Conditie.Predicaat p && p.soort() == Conditie.Predicaatsoort.LEEG
                && p.operand() instanceof Expr.AttribuutRef pa && pa.set() && isTemporeelSet82(pa)) {
            tijd = pa; toets = 8; tijdType = pa.type();
        } else {
            return null;   // not a set §8.2 form
        }
        Objecttype elem = tijd.objecttype();
        Class<?> elemClass = factClass(elem);
        if (elemClass == null || subject == null) return null;
        String rol = model.verbindendeRol(elem, subject);
        if (rol == null) return null;
        // §13.3.9: this join gets NO storage-direction gate, and the reason is that it is UNREACHABLE through a
        // well-formed document — measured 2026-09-11, not assumed. Every arm above enters on `a.set()`
        // (isTemporeelSet82), and WF-096 (2026-09-09) refuses a set reading at every position except the SOURCE of a
        // single-argument instance fold; a §8.2 comparison operand and a gevuld/leeg operand are ordinary read
        // positions. Built anyway and pushed through the four executors, the shape reads:
        //     verifier: WF-096 FAIL /resolvedRegels/0/voorwaarde/links: set reading at an ordinary read position
        //     ref     : EVALUATION FAULT: whole-period comparison is ch. 3 scope
        // so the document the gate would protect cannot exist. Adding one would be an unmeasurable branch; if WF-096
        // ever admits a set operand under a periode qualifier, THIS is the line that has to grow a heeftVeld call.
        final String backref = rol, attr = tijd.attribuut();
        Variable elemVar = DSL.declarationOf(elemClass, "$ge_" + (seq++));
        PatternDSL.PatternDef src = PatternDSL.pattern(elemVar).expr(nextId("join"), (Variable) subjectVar,
                (Predicate2) (e, subj) -> RegelSpraakRuntime.get(e, backref) == subj);
        Variable elemTlVar = DSL.declarationOf(List.class, "$getl_" + (seq++));
        Variable tlVar = DSL.declarationOf(List.class, "$gtl_" + (seq++));
        PatternDSL.PatternDef src2 = src.bind(elemTlVar, (Function1) (e) -> RegelSpraakRuntime.get(e, attr));
        RuleItemBuilder accItem = DSL.accumulate(src2, DSL.accFunction(accFn("CollectListAccumulateFunction"), elemTlVar).as(tlVar));
        final int fToets = toets; final String fGran = gran; final boolean niet = gp.niet();
        final WaardeFn gf = grensFn;
        // the eval reads the collected list + (≤1) grens global; the grens is restated to the element unit above.
        java.util.Set<String> gGlobals = gf == null ? java.util.Set.of() : gf.globals();
        if (gGlobals.isEmpty()) {
            Predicate1<Object> ev = tls -> {
                BigDecimal grens = gf == null ? null : RegelSpraakRuntime.toBd(gf.fn().apply(null, java.util.Map.of()));
                boolean b = RegelSpraakRuntime.rsGeheelPeriodeAlle((List<? extends List<?>>) tls, fGran, fToets, grens);
                return niet ? !b : b;
            };
            PatternDSL.PatternDef check = PatternDSL.pattern(tlVar).expr(nextId("c"), (Predicate1) ev);
            return new Gate(pattern, List.of(accItem, check));
        }
        if (gGlobals.size() == 1) {
            String gn = gGlobals.iterator().next();
            Global<BigDecimal> gg = globals.get(gn);
            if (gg == null) return null;
            Predicate2<Object, Object> ev = (tls, gv) -> {
                BigDecimal grens = RegelSpraakRuntime.toBd(gf.fn().apply(null, bindGlobal(gn, gv)));
                boolean b = RegelSpraakRuntime.rsGeheelPeriodeAlle((List<? extends List<?>>) tls, fGran, fToets, grens);
                return niet ? !b : b;
            };
            PatternDSL.PatternDef check = PatternDSL.pattern(tlVar).expr(nextId("c"), (Variable) gg, (Predicate2) ev);
            return new Gate(pattern, List.of(accItem, check));
        }
        return null;   // >1 global grens -> skip (symmetric marker)
    }

    /** Whether {@code a} is a temporal (tijdlijn) attribute of its (set-element) objecttype — the set analogue of
     *  {@link #isTemporeel82}, admitting a to-many set element whose objecttype != subject; multi-period excluded. */
    private static boolean isTemporeelSet82(Expr.AttribuutRef a) {
        return a.objecttype() != null && Namen.bevatNaam(a.objecttype().granulariteitLeden(), a.attribuut())
                && !Tijdlijnen.meerPeriodiek(a.objecttype().tijdlijnAantalLeden(), a.attribuut());
    }

    /** A fact-only Predicate1 for "the §8.2 check holds" (the NIET prefix complements the rsGeheelPeriode result), or
     *  null when the grens reads a Parameter-global (the caller binds it as a pattern variable). */
    private Predicate1<Object> geheelPeriodeLeaf(Gp82 g) {
        if (g == null) return null;
        if (g.grens() != null && !g.grens().globals().isEmpty()) return null;   // needs a global -> 2-var expr
        java.util.function.Function<Object, Object> tl = g.timeline();
        WaardeFn gf = g.grens(); int toets = g.toets(); String gran = g.gran(); boolean niet = g.niet();
        return s -> {
            BigDecimal grens = gf == null ? null : RegelSpraakRuntime.toBd(gf.fn().apply(s, java.util.Map.of()));
            boolean b = RegelSpraakRuntime.rsGeheelPeriode((List<?>) tl.apply(s), gran, toets, grens);
            return niet ? !b : b;
        };
    }

    // ---------------- IR-SPEC ch. 4.5 / RUNTIME-SEMANTICS ch. 3.9a the QUANTIFIER FOLD ----------------

    /** Record WHY a quantifier fold did not lower and decline it. Unlike the other condition arms — which return a
     *  bare null and let the construct's own {@code skip} attach the generic scope note — this one writes its reason
     *  into {@link #overgeslagen} itself, naming the FORM. The node has no corpus witness yet, so "the rule skipped"
     *  without a reason would leave the gate-12 report unable to say WHICH of a dozen guards below refused; and
     *  {@code meet} joins the delta of this list into the construct's one Dekking reason, so the named line and the
     *  caller's note arrive together rather than competing. Every refusal below goes through here — there is no
     *  silent decline in this arm. */
    private Gate kvSkip(Conditie.Kwantorvouw kv, String reden) {
        String snip = kv.bron() == null ? null : kv.bron().snippet();
        overgeslagen.add("kwantorvouw " + kv.kwantor() + " over "
                + (kv.elementObjecttype() == null ? "<onopgelost elementtype>" : kv.elementObjecttype().naam)
                + " (" + reden + ")" + (snip == null || snip.isEmpty() ? "" : ": " + snip));
        return null;
    }

    /** WHY a non-temporal matrix LEAF was declined, on the axis it was actually declined on. The node CLASS alone
     *  is too coarse and was measured misleading on 2026-09-16: a {@code Vergelijking} is carried in general, and a
     *  comparison against a Parameter is declined for its OPERAND, not for being a comparison. So a node that has
     *  operands is named by its operands; one that does not falls back to its class. A COMPOSITE matrix does not
     *  reach here: {@link #matrixVertaling} refuses it on the bullet that actually declined, and that bullet is
     *  named through this method with the composite path in front of it. */
    private String matrixAs(Conditie c) {
        if (c instanceof Conditie.Vergelijking v)
            return "een vergelijking van " + operandAs(v.links()) + " met " + operandAs(v.rechts());
        if (c instanceof Conditie.Predicaat p)
            return "een " + p.soort() + "-predicaat over " + operandAs(p.operand());
        return "een conditie van soort " + c.getClass().getSimpleName();
    }

    /** One operand of {@link #matrixAs}, named the way the leaf translation distinguishes them. */
    private String operandAs(Expr e) {
        if (e == null) return "een ontbrekend operand";
        if (e instanceof Expr.AttribuutRef a)
            return (ongenavigeerd(a) ? "het eigen attribuut '" : "het genavigeerde attribuut '")
                    + a.attribuut() + "'";
        if (e instanceof Expr.ParameterRef p) return "de parameter '" + p.parameter().naam() + "'";
        if (e instanceof Expr.Literal l) return "de literal '" + l.waarde() + "'";
        if (e instanceof Expr.Functie f) return "een " + f.kind() + "-functie";
        return "een operand van soort " + e.getClass().getSimpleName();
    }

    /** A lowered NON-temporal fold matrix: a {@code (contributor, boundValue) -> boolean} test plus the ONE
     *  Parameter-global it reads, or {@code bound == null} when it reads none. A REFUSAL is the same record with
     *  a null test and {@code weigering} naming the axis — carried in the value rather than signalled by null, so
     *  the reason is written where the decline happens and a composite hands its bullet's reason up unchanged.
     *  The bound arrives as an argument because the eval that asks this rides AFTER the {@code collectList}
     *  accumulate and sees the contributors only through the collected list: a global reaches the matrix as a
     *  pattern variable of that eval or not at all. The same holds for the rekendatum a ch. 3.10 window reads,
     *  which is the test's THIRD argument and is bound only when {@code rekendatum} says the matrix reads it. */
    private record Matrix(Global<BigDecimal> bound, boolean rekendatum,
            Predicate3<Object, BigDecimal, Object> test, String weigering) {
        static Matrix nee(String reden) { return new Matrix(null, false, null, reden); }
        boolean ok() { return test != null; }
    }

    /** RUNTIME-SEMANTICS ch. 3.9a's {@code test} lowered per contributor, with the ELEMENT as subject. Three
     *  shapes, in this order:
     *  <ul>
     *    <li>a {@code Samengesteld} — §13.4.14's composite, RECURSIVELY and at any depth: each bullet is lowered
     *        by this same method and the results are folded into ONE test by the composite's own quantifier and
     *        threshold, read with ch. 5.10's stop triggers ({@link #vastgesteld}) and settled by the shared
     *        {@link #telVerdict}. The composite's quantifier and the FOLD's are therefore evaluated at their own
     *        levels and never collapse into one another;</li>
     *    <li>a §5.6 comparison against a numeric Parameter, written on EITHER side — the bound arrives as a
     *        pattern variable ({@link #globalBullet}, shared with {@link #filterSource}), which is why this
     *        method returns a multi-argument test at all;</li>
     *    <li>a ch. 3.10 time window — {@link #vensterToets}, the reading the rule-level window uses, with the
     *        ELEMENT as its subject, so an attribute bound is read on each contributor and never on the fold's
     *        own subject; the rekendatum arrives as the third argument. WF-112 admits it only where nothing
     *        above it counts or complements, and {@link #kwantorvouwVoorwaarde} refuses the rest before this
     *        method runs;</li>
     *    <li>every other shape — the fact-only {@link #leafPredicate}, the per-subject leaf translation every
     *        other condition of this backend uses, widened to the same arity by ignoring the bound.</li>
     *  </ul>
     *
     *  <p><b>At most ONE distinct Parameter-global over the WHOLE matrix.</b> The eval that asks the matrix rides
     *  after the accumulate and binds the collected list plus one Parameter variable (and the rekendatum, in its
     *  own slot, when a window reads it), so a second, different bound has nowhere to land and the matrix is
     *  refused by name. This is the SAME restriction {@link #filterSource}'s
     *  counting arm carries for the same structural reason — and it is why the filter position keeps its own
     *  fold ({@link #leafCompound}) rather than calling this one: there the bullets are constraints on the
     *  accumulate SOURCE pattern, where a conjunctive arm can bind each bullet's own global on its own
     *  {@code .expr}, and depth is refused to stay symmetric with the DRL-text branch's parity guard.
     *
     *  <p>Never returns null: a refusal is a {@link Matrix} carrying its reason, so the caller can name the axis
     *  it was declined on rather than reporting the node class. */
    private Matrix matrixVertaling(Conditie c, Objecttype elem) {
        if (c instanceof Conditie.Samengesteld s) {
            final Conditie.Kwantor kw = s.kwantor();
            final int drempel = s.drempel();
            Global<BigDecimal> bound = null;
            boolean rekendatum = false;
            List<Predicate3<Object, BigDecimal, Object>> delen = new ArrayList<>();
            for (Conditie deel : s.onderdelen()) {
                Matrix m = matrixVertaling(deel, elem);
                if (!m.ok()) return Matrix.nee("in de " + kw + "-samenstelling: " + m.weigering());
                if (m.bound() != null) {
                    if (bound != null && bound != m.bound())
                        return Matrix.nee("een " + kw + "-samenstelling die twee verschillende parameters leest:"
                                + " de eval na de accumulate bindt er één");
                    bound = m.bound();
                }
                rekendatum |= m.rekendatum();
                delen.add(m.test());
            }
            final List<Predicate3<Object, BigDecimal, Object>> fd = delen;
            return new Matrix(bound, rekendatum, (e, gv, rd) -> {
                int t = 0;
                for (int i = 0; i < fd.size(); i++) {
                    Boolean vast = vastgesteld(kw, drempel, t, i, fd.size() - i);   // ch. 5.10 short circuit
                    if (vast != null) return vast;
                    if (fd.get(i).test(e, gv, rd)) t++;
                }
                return telVerdict(kw, drempel, t, fd.size());
            }, null);
        }
        if (c instanceof Conditie.Periode per) {
            if (rekendatumGlobal == null)   // defensive: heeftTijdgebonden registers it wherever a window can sit
                return Matrix.nee("een periodevenster zonder rekendatum-global");
            Predicate2<Object, Object> venster = vensterToets(per, elem);
            if (venster == null)
                return Matrix.nee("een periodevenster met een grens die geen datumliteral en geen eigen scalair"
                        + " datumattribuut van het element is (§8.4.3)");
            return new Matrix(null, true, (e, gv, rd) -> venster.test(e, rd), null);
        }
        // §8.1.1 the bound written on the LEFT ("de keurgrens kleiner is dan zijn ervaring"): swap the operands
        // and REVERSE the operator, the same normalisation leafPredicate applies to a literal-LHS comparison and
        // for the same reason — every recogniser below keys on an attribute LHS, so the unswapped form would
        // decline a sentence that means exactly the swapped one. Only a form that would otherwise decline is
        // rewritten, and after the swap the parameter sits on the right, so it cannot re-trigger.
        if (c instanceof Conditie.Vergelijking pv && pv.links() instanceof Expr.ParameterRef
                && !(pv.rechts() instanceof Expr.ParameterRef))
            c = new Conditie.Vergelijking(Operatoren.omkeren(pv.op()), pv.rechts(), pv.links(),
                    pv.periode(), pv.bron());
        GlobalVergelijking gb = globalBullet(c, elem);   // §5.6 a getalachtig Parameter bound
        if (gb != null) return new Matrix(gb.global(), false, (e, gv, rd) -> gb.test().test(e, gv), null);
        Predicate1<Object> lp = leafPredicate(c, elem);
        if (lp != null) return new Matrix(null, false, (e, gv, rd) -> lp.test(e), null);
        return Matrix.nee(matrixAs(c));
    }

    /** The fold's die/dat {@code filter} on the accumulate source: {@link #filterSource}, plus the ch. 3.10
     *  window, which that shared method does not bind. The window becomes one more conjunct of the SOURCE
     *  pattern, reading the rekendatum global beside the element — the element is the subject, so an attribute
     *  bound is read on each candidate. An ALLE/DE conjunction holding a window is split here so each bullet
     *  lands on its own {@code .expr}, as filterSource splits one; under a fold quantifier a window can sit
     *  nowhere else, because {@link Vensterpositie} has already refused every counting or complementing
     *  position (WF-112). Every filter without a window goes to filterSource unchanged, and an aggregation's
     *  filter never comes here. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private PatternDSL.PatternDef vouwFilter(PatternDSL.PatternDef src, Conditie filter, Objecttype elem) {
        if (filter instanceof Conditie.Periode per) {
            Predicate2<Object, Object> venster = rekendatumGlobal == null ? null : vensterToets(per, elem);
            return venster == null ? null
                    : src.expr(nextId("filter"), (Variable) rekendatumGlobal, (Predicate2) venster);
        }
        if (filter instanceof Conditie.Samengesteld s && Conditie.bevatPeriode(s)
                && (s.kwantor() == Conditie.Kwantor.ALLE || s.kwantor() == Conditie.Kwantor.DE)) {
            for (Conditie deel : s.onderdelen()) {
                src = vouwFilter(src, deel, elem);
                if (src == null) return null;
            }
            return src;
        }
        return filterSource(src, filter, elem);
    }

    /** Is {@code a} a read of a SINGLE-PERIOD tijdlijn member of {@code elem} written with the ELEMENT as subject?
     *  The matrix of a fold is evaluated per contributor with the element as subject (ch. 3.9a), so a navigated
     *  read is a further hop this arm does not walk, and the member is looked up CHAIN-CLOSED (a contributor may be
     *  a subtype that inherits the timeline). Multi-period (§13.3.6.7) is excluded, as everywhere in §8.2. */
    private static boolean elementTijdlijnLid(Objecttype elem, String lid) {
        return Namen.bevatNaam(elem.granulariteitLeden(), lid)
                && !Tijdlijnen.meerPeriodiek(elem.tijdlijnAantalLeden(), lid);
    }

    /** RUNTIME-SEMANTICS ch. 3.9a: lower a QUANTIFIER FOLD to a {@code collectList} accumulate over the contributor
     *  timelines plus a result-var eval that folds them with {@link RegelSpraakRuntime#rsGeheelPeriodeAlle} /
     *  {@link RegelSpraakRuntime#rsGeheelPeriodeGeen}. Three pieces, each taken from the machinery that already
     *  exists rather than rebuilt:
     *  <ul>
     *    <li>the CONTRIBUTORS are the instance fold's — the by-pair join first and the wire's {@code bronRol} as the
     *        fallback for the self-typed pair no by-pair search can disambiguate, with the SAME §13.3.9
     *        storage-direction questions {@link #aggregatie} asks (a derived role is not yet a bean field);</li>
     *    <li>the {@code filter} is applied to the accumulate SOURCE through {@link #filterSource}, which is exactly
     *        how {@code Expr.Functie.filter} is applied — the field has the same reading and the same machinery —
     *        with one addition of this position only, the ch. 3.10 window ({@link #vouwFilter});</li>
     *    <li>the {@code toets} takes one of two routes. A §8.2 whole-period predicate over the contributor's own
     *        timeline folds the collected {@code List<List<Segment>>} through the ch. 3.9 helper per contributor;
     *        every OTHER matrix is an ordinary condition of the element, so the CONTRIBUTORS THEMSELVES are
     *        collected and {@link #matrixVertaling} — the per-subject leaf translation, widened with §13.4.14's
     *        composite, a §5.6 Parameter bound and a ch. 3.10 window — is asked of each, with the element as the
     *        subject.</li>
     *  </ul>
     *
     *  <p><b>Where the NIET prefix binds.</b> ch. 3.9a makes {@code toets} an ORDINARY condition evaluated with the
     *  element as subject, so its §8.2 "niet gedurende …" prefix is part of the MATRIX and negates per contributor —
     *  not the quantifier's verdict. That is the one place this arm differs from
     *  {@link #geheelPeriodeAlleVoorwaarde}, which complements the whole forall because there the qualifier sits on
     *  the outer comparison. A per-contributor negation needs no third runtime helper: ∀c.¬P = ¬∃c.P and
     *  ¬∃c.¬P = ∀c.P, so a NIET matrix simply FLIPS ALLE into GEEN and back. On the NON-temporal route not even
     *  that happens: there the negation is a field of the condition node ({@code Kenmerk.negatie}, or the
     *  comparison's own operator) and {@link #matrixVertaling} reads it, so the quantifier is untouched.
     *
     *  <p>An EMPTY contributor set is WAAR for both quantifiers (ch. 3.9a), which both helpers give directly.
     *  Every shape outside this scope goes to {@link #kvSkip} with its own reason — never a silently wrong verdict. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private Gate kwantorvouwVoorwaarde(Conditie.Kwantorvouw kv, PatternDSL.PatternDef pattern,
            Objecttype subject, Variable subjectVar) {
        // WF-112 before anything else: this arm now lowers a window in the matrix and the filter, and under
        // GEEN or a counting quantifier that lowering would answer the counted or complemented verdict no
        // reading specifies. The verifier refuses such a document, but not every document goes through the verifier.
        String vensterWeigering = Vensterpositie.weigering(kv);
        if (vensterWeigering != null) return kvSkip(kv, vensterWeigering);
        if (subject == null || subjectVar == null)
            return kvSkip(kv, "geen subjectpatroon om het bereik aan te hangen");
        // The loader refuses DE and every unknown token by name (WF-108 (a)); this is the defensive twin,
        // because a fold reached through any other route must never be lowered as if it were a forall.
        if (kv.kwantor() == Conditie.Kwantor.DE)
            return kvSkip(kv, "kwantor DE (WF-108 (a)): de bepaalde lidwoordvorm noemt ÉÉN lid en is geen "
                    + "kwantificatie over een verzameling");
        if (!kv.bronPad().isEmpty())
            return kvSkip(kv, "een meerstaps bereik (WF-108 (c)); de accumulate-source draagt één join-rol");
        Objecttype elem = kv.elementObjecttype();
        if (elem == null) return kvSkip(kv, "onopgelost elementObjecttype");
        Class<?> elemClass = factClass(elem);
        if (elemClass == null) return kvSkip(kv, "geen fact-class voor elementtype " + elem.naam);
        if (kv.toets() == null) return kvSkip(kv, "een kwantorvouw zonder matrix (WF-108 (d))");

        // ---- the MATRIX, two routes. ch. 3.9a states `toets` as an ORDINARY condition evaluated per
        // contributor with the ELEMENT as subject, so the fold's shape does not depend on which condition it
        // is — only on how one member is asked. A §8.2 whole-period predicate over ONE tijdlijn member is
        // asked through the ch. 3.9 runtime helpers (the long branch below, which has to pick a field, a
        // check code and a grens); every OTHER matrix is asked through matrixVertaling — the per-subject leaf
        // translation every other condition of this backend uses, carrying §13.4.14's composite over those
        // leaves and a §5.6 Parameter as the bound — with the element as that subject. A form it does not
        // carry is named on the axis it declined on, never silently skipped.
        Conditie.GeheelPeriode gp = geheelPeriodeVan(kv.toets());
        Matrix statischeMatrix = null;   // the NON-temporal route: the lowered matrix, asked per contributor
        String gran = null;
        String tlVeld = null;   // the contributor field holding the List<Segment> the matrix reads
        int toetsCode = 0;      // the rsGeheelPeriode check encoding
        WaardeFn grensFn = null;
        if (gp == null) {
            statischeMatrix = matrixVertaling(kv.toets(), elem);
            if (!statischeMatrix.ok())
                return kvSkip(kv, "een niet-temporele matrix die de per-subject bladvertaling niet draagt: "
                        + statischeMatrix.weigering());
        } else {
            gran = gp.jaar() ? "JAAR" : "MAAND";
            if (kv.toets() instanceof Conditie.Vergelijking v) {
                Expr.AttribuutRef tijd; Expr ander; boolean linksTemp;
                if (v.links() instanceof Expr.AttribuutRef la && ongenavigeerd(la) && elementTijdlijnLid(elem, la.attribuut()))
                    { tijd = la; ander = v.rechts(); linksTemp = true; }
                else if (v.rechts() instanceof Expr.AttribuutRef ra && ongenavigeerd(ra) && elementTijdlijnLid(elem, ra.attribuut()))
                    { tijd = ra; ander = v.links(); linksTemp = false; }
                else return kvSkip(kv, "een matrixvergelijking zonder enkelperiodiek tijdlijnattribuut van het elementtype");
                if (!(tijd.type().isNumeriek() || tijd.type().isPercentage()))
                    return kvSkip(kv, "een niet-numerieke heelperiodevergelijking in de matrix");
                if (ander instanceof Expr.AttribuutRef ar && ongenavigeerd(ar) && elementTijdlijnLid(elem, ar.attribuut()))
                    return kvSkip(kv, "een tijdlijn-vs-tijdlijnvergelijking in de matrix");
                Eenheid el = eff.eenheidVan(tijd), er = eff.eenheidVan(ander);
                boolean crossUnit = el != null && er != null && el.vereistOmrekening(er);
                if (crossUnit && er.exacteHerschaalFactoren(el) == null)
                    return kvSkip(kv, "een matrixgrens in een eenheid zonder exacte herschaalfactoren");
                Integer code = Tijdrekening.toets(linksTemp ? v.op() : Operatoren.omkeren(v.op()));   // REVERSE (operand swap), not negate
                if (code == null) return kvSkip(kv, "een datumvergelijking als heelperiodematrix");
                // The eval rides AFTER the collectList accumulate, on a pattern that binds no $subject — so the grens
                // must be subject-free: a literal or a scalar Parameter-global. The same restriction the set §8.2 arm
                // carries, for the same structural reason.
                if (!(ander instanceof Expr.Literal)
                        && !(ander instanceof Expr.ParameterRef pr && pr.parameter().granulariteit() == null))
                    return kvSkip(kv, "een niet-scalaire matrixgrens (de eval na de accumulate bindt geen $subject)");
                WaardeFn g = scalarWaarde(null, ander);
                if (g == null) return kvSkip(kv, "een matrixgrens buiten de scalaire scope");
                if (crossUnit) g = g.herschaald(effType(ander), effType(tijd), "een §8.2 tijdlijngrens");
                tlVeld = tijd.attribuut(); toetsCode = code; grensFn = g;
            } else if (kv.toets() instanceof Conditie.Predicaat p && p.operand() instanceof Expr.AttribuutRef pa
                    && ongenavigeerd(pa) && elementTijdlijnLid(elem, pa.attribuut())
                    && (p.soort() == Conditie.Predicaatsoort.GEVULD || p.soort() == Conditie.Predicaatsoort.LEEG)) {
                tlVeld = pa.attribuut();
                toetsCode = p.soort() == Conditie.Predicaatsoort.GEVULD ? 6 : 8;   // §8.2 gevuld / leeg over every moment
            } else if (kv.toets() instanceof Conditie.Kenmerk k && k.navigatie() == null && !k.negatie()
                    && k.kenmerk() != null && Namen.bevatNaam(elem.kenmerkGranulariteitLeden(), k.kenmerk())
                    && !Tijdlijnen.meerPeriodiek(elem.kenmerkTijdlijnAantalLeden(), k.kenmerk())) {
                tlVeld = k.kenmerk(); toetsCode = 7;                                // §8.2 the kenmerk's TRUE-period timeline
            } else {
                return kvSkip(kv, "een matrixvorm buiten de §8.2 heelperiodeverzameling (vergelijking / gevuld / leeg /"
                        + " tijdsafhankelijk kenmerk van het element)");
            }
        }

        // ---- the CONTRIBUTORS: the instance fold's range, with the instance fold's §13.3.9 questions ----
        RegelSpraakModel.Rol[] paar = FactModelGenerator.gebondenRolpaar(model, kv.bronRol());
        String backref = model.verbindendeRol(elem, subject);    // the role the SUBJECT plays: the field on the element
        if (backref == null) {
            // the to-many-sibling arm this seam carried inline is FactModelGenerator.bereikjoin's, shared with
            // the DRL-text branch since 2026-09-19 — one cardinality refusal, one sentence
            FactModelGenerator.Bereikjoin bj = FactModelGenerator.bereikjoin(model, kv.bronRol());
            backref = bj.rol();
            if (backref == null)
                return kvSkip(kv, bj.weigering() != null ? bj.weigering()
                        : "geen verbindende rol tussen " + subject.naam + " en " + elem.naam);
        }
        String elemRol = model.verbindendeRol(subject, elem);    // the role the ELEMENT plays: the field on the subject
        if (elemRol == null && paar != null) elemRol = paar[0].naam();
        boolean voorwaarts = elemRol != null && !model.naarVeelRol(subject, elem, elemRol);
        if (voorwaarts && model.naarVeelRol(elem, subject, backref))
            return kvSkip(kv, "een enkelvoudige elementrol '" + elemRol + "' wiens zusterrol '" + backref
                    + "' meervoudig is: dat feittype heeft ÉÉN beanveld en dat draagt noodzakelijk BEIDE richtingen"
                    + " — dezelfde conservatieve weigering die de aggregatie draagt (§13.3.9 opslagrichting)");
        Objecttype joinDrager = voorwaarts ? subject : elem, joinDoel = voorwaarts ? elem : subject;
        String joinRol = voorwaarts ? elemRol : backref;
        if (!heeftReferentieVeld(joinDrager, joinRol, joinDoel))
            return kvSkip(kv, "de bereik-join heeft geen beanveld — "
                    + geenReferentieveldReden(joinDrager, joinRol, joinDoel) + " (§13.3.9 opslagrichting)");
        final String fwdVeld = elemRol, bwdVeld = backref;
        Variable elemVar = DSL.declarationOf(elemClass, "$kv_" + (seq++));
        PatternDSL.PatternDef src = PatternDSL.pattern(elemVar).expr(nextId("kvjoin"), (Variable) subjectVar,
                voorwaarts ? (Predicate2) (e, subj) -> RegelSpraakRuntime.get(subj, fwdVeld) == e
                           : (Predicate2) (e, subj) -> RegelSpraakRuntime.get(e, bwdVeld) == subj);
        // ch. 3.9a: the filter selects WHICH contributors are quantified over, per contributor with the element as
        // subject — the same field, the same reading and the same machinery as Functie.filter (ch. 3.4a).
        if (kv.filter() != null) {
            src = vouwFilter(src, kv.filter(), elem);
            if (src == null) return kvSkip(kv, "een die/dat-subselectie die deze tak niet op de source kan binden");
        }

        // ---- the FOLD, NON-temporal matrix: collect the CONTRIBUTORS and ask the leaf of each ----
        // The same three pieces as the temporal fold below — join+filter source, collectList accumulate,
        // result-var eval — with the ELEMENT collected instead of its timeline, because here the matrix reads
        // the whole member and not one field of it. An empty collected list satisfies BOTH quantifiers, so
        // ch. 3.9a's vacuous rule needs nothing arranged for it. And the NIET prefix stays INSIDE the matrix
        // (leafPredicate reads Kenmerk.negatie; a comparison spells its own operator), so unlike the fold
        // below there is no quantifier to flip: ¬∃ over a negated matrix IS what the wire asked for.
        if (statischeMatrix != null) {
            Variable elemsVar = DSL.declarationOf(List.class, "$kvel_" + (seq++));   // the collected contributors
            RuleItemBuilder accStatisch = DSL.accumulate(src,
                    DSL.accFunction(accFn("CollectListAccumulateFunction"), elemVar).as(elemsVar));
            final Predicate3<Object, BigDecimal, Object> matrix = statischeMatrix.test();
            final Conditie.Kwantor kwS = kv.kwantor();
            final int dS = kv.drempel();
            // ch. 3.9a's count over the collected contributors, read with ch. 5.10's stop triggers — the same
            // {@link #vastgesteld} table the composite uses, with the contributors as its members. ALLE is
            // hits == n and GEEN is hits == 0, so all five quantifiers are one comparison. The matrix takes the
            // bound as its second argument and the rekendatum as its third; each is null exactly when the matrix
            // does not read it.
            Predicate3<Object, BigDecimal, Object> tel = (leden, grens, rd) -> {
                List<?> ls = (List<?>) leden;
                int n = ls.size(), hits = 0;
                for (int i = 0; i < n; i++) {
                    Boolean vast = vastgesteld(kwS, dS, hits, i, n - i);
                    if (vast != null) return vast;
                    if (matrix.test(ls.get(i), grens, rd)) hits++;
                }
                return telVerdict(kwS, dS, hits, n);
            };
            // The §5.6 bound and the window's rekendatum ride as pattern variables on the SAME eval, exactly as
            // the bound does on the §8.2 timeline route below — each global bound once, read per contributor
            // inside the fold, and only when the matrix reads it.
            Global<BigDecimal> grensGlobal = statischeMatrix.bound();
            Global<java.time.LocalDate> rdGlobal = statischeMatrix.rekendatum() ? rekendatumGlobal : null;
            PatternDSL.PatternDef checkStatisch;
            if (grensGlobal == null && rdGlobal == null) {
                Predicate1<Object> evStatisch = leden -> tel.test(leden, null, null);
                checkStatisch = PatternDSL.pattern(elemsVar).expr(nextId("kv"), (Predicate1) evStatisch);
            } else if (rdGlobal == null) {
                Predicate2<Object, Object> evStatisch = (leden, gv) -> tel.test(leden, (BigDecimal) gv, null);
                checkStatisch = PatternDSL.pattern(elemsVar)
                        .expr(nextId("kv"), (Variable) grensGlobal, (Predicate2) evStatisch);
            } else if (grensGlobal == null) {
                Predicate2<Object, Object> evStatisch = (leden, rd) -> tel.test(leden, null, rd);
                checkStatisch = PatternDSL.pattern(elemsVar)
                        .expr(nextId("kv"), (Variable) rdGlobal, (Predicate2) evStatisch);
            } else {
                Predicate3<Object, Object, Object> evStatisch =
                        (leden, gv, rd) -> tel.test(leden, (BigDecimal) gv, rd);
                checkStatisch = PatternDSL.pattern(elemsVar)
                        .expr(nextId("kv"), (Variable) grensGlobal, (Variable) rdGlobal, (Predicate3) evStatisch);
            }
            return new Gate(pattern, List.of(accStatisch, checkStatisch));
        }

        // ---- the FOLD: collect each contributor's timeline, then quantify over the collected list ----
        final String attr = tlVeld;
        Variable elemTlVar = DSL.declarationOf(List.class, "$kvtl_" + (seq++));   // per-contributor timeline
        Variable tlVar = DSL.declarationOf(List.class, "$kvtls_" + (seq++));      // the collected List<List<Segment>>
        PatternDSL.PatternDef src2 = src.bind(elemTlVar, (Function1) (e) -> RegelSpraakRuntime.get(e, attr));
        RuleItemBuilder accItem = DSL.accumulate(src2, DSL.accFunction(accFn("CollectListAccumulateFunction"), elemTlVar).as(tlVar));
        // the NIET prefix is the MATRIX's, so it flips the quantifier rather than the verdict (see the javadoc).
        final boolean alle = (kv.kwantor() == Conditie.Kwantor.ALLE) != gp.niet();
        // ch. 3.9a's counting quantifiers over this same matrix: what they need is not a witness but the SIZE
        // of the satisfier set, so they read the counting TWIN of the two helpers above and compare. The NIET
        // prefix cannot flip a threshold, so here it is applied to the count instead: ¬P holds of exactly the
        // members P does not, and rsGeheelPeriode is total, so count(¬P) = n − count(P) exactly.
        final Conditie.Kwantor kwT = kv.kwantor();
        final int dT = kv.drempel();
        final boolean nietPrefix = gp.niet();
        final boolean telt = kwT == Conditie.Kwantor.TEN_MINSTE || kwT == Conditie.Kwantor.TEN_HOOGSTE
                || kwT == Conditie.Kwantor.PRECIES;
        final int fToets = toetsCode; final String fGran = gran; final WaardeFn gf = grensFn;
        java.util.Set<String> gGlobals = gf == null ? java.util.Set.of() : gf.globals();
        if (gGlobals.isEmpty()) {
            Predicate1<Object> ev = tls -> {
                BigDecimal grens = gf == null ? null : RegelSpraakRuntime.toBd(gf.fn().apply(null, java.util.Map.of()));
                if (telt) {
                    List<? extends List<?>> ls = (List<? extends List<?>>) tls;
                    int n = ls == null ? 0 : ls.size();
                    int hits = RegelSpraakRuntime.rsGeheelPeriodeTelling(ls, fGran, fToets, grens);
                    return telVerdict(kwT, dT, nietPrefix ? n - hits : hits, n);
                }
                return alle ? RegelSpraakRuntime.rsGeheelPeriodeAlle((List<? extends List<?>>) tls, fGran, fToets, grens)
                            : RegelSpraakRuntime.rsGeheelPeriodeGeen((List<? extends List<?>>) tls, fGran, fToets, grens);
            };
            PatternDSL.PatternDef check = PatternDSL.pattern(tlVar).expr(nextId("kv"), (Predicate1) ev);
            return new Gate(pattern, List.of(accItem, check));
        }
        if (gGlobals.size() == 1) {
            String gn = gGlobals.iterator().next();
            Global<BigDecimal> gg = globals.get(gn);
            if (gg == null) return kvSkip(kv, "een matrixgrens op de niet-geregistreerde global " + gn);
            Predicate2<Object, Object> ev = (tls, gv) -> {
                BigDecimal grens = RegelSpraakRuntime.toBd(gf.fn().apply(null, bindGlobal(gn, gv)));
                if (telt) {
                    List<? extends List<?>> ls = (List<? extends List<?>>) tls;
                    int n = ls == null ? 0 : ls.size();
                    int hits = RegelSpraakRuntime.rsGeheelPeriodeTelling(ls, fGran, fToets, grens);
                    return telVerdict(kwT, dT, nietPrefix ? n - hits : hits, n);
                }
                return alle ? RegelSpraakRuntime.rsGeheelPeriodeAlle((List<? extends List<?>>) tls, fGran, fToets, grens)
                            : RegelSpraakRuntime.rsGeheelPeriodeGeen((List<? extends List<?>>) tls, fGran, fToets, grens);
            };
            PatternDSL.PatternDef check = PatternDSL.pattern(tlVar).expr(nextId("kv"), (Variable) gg, (Predicate2) ev);
            return new Gate(pattern, List.of(accItem, check));
        }
        return kvSkip(kv, "een matrixgrens die meer dan één parameter-global leest");
    }

    /** Lower a voorwaarde into the subject pattern's in-pattern constraints (plus far-side exists/not joins as
     *  extra rule items). Returns a {@link Gate}, or null when ANY part is outside scope (caller skips the whole
     *  rule — never a partial gate). A constraint that needs a pattern variable (a comparison vs a Parameter-global)
     *  or hosts an exists-join lives HERE; every fact-only leaf flows through {@link #leafPredicate}, shared with
     *  the counting-quantifier path. A temporal (List<Segment>) operand SKIPS (Phase F) — never a List in a scalar lambda.
     *  Coverage: §13.4.14 ALLE/DE (AND), GEEN/TEN_MINSTE/TEN_HOOGSTE/PRECIES (counting), §8.1.7 near- and far-side
     *  rolcheck, §8.1.8 own + to-one navigated kenmerkcheck, §8.1.2 LEEG/GEVULD, §8.1.1 comparison vs global/literal. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    /** RUNTIME-SEMANTICS ch. 5.10, the composite's stop triggers: the verdict once {@code gelezen} members have
     *  been read of which {@code hits} hold and {@code rest} are still unread, or {@code null} while the unread
     *  members can still change it. ALLE/DE stop at the first member that does not hold; GEEN at the first that
     *  does; TEN MINSTE once the threshold is reached OR out of reach; TEN HOOGSTE once it is exceeded OR can no
     *  longer be exceeded (the TRUE side, maintainer decision 2026-09-07); PRECIES on either side. One rule, no
     *  exception: each arm marks the moment the outcome can no longer change, and a threshold that already covers
     *  the whole member list settles on the EMPTY prefix — so a TEN HOOGSTE with d &gt;= n reads nothing at all,
     *  exactly as TEN MINSTE and PRECIES above n do. Shared by both places that fold members into ONE lambda — the counting
     *  quantifiers of {@link #voorwaarde} and the nested {@link #leafCompound} (where ALLE/DE fold too, unlike
     *  the top-level conjunctive arm, which is an alpha chain and short-circuits by construction). */
    private static Boolean vastgesteld(Conditie.Kwantor kw, int drempel, int hits, int gelezen, int rest) {
        return switch (kw) {
            case ALLE, DE    -> hits < gelezen ? Boolean.FALSE : null;
            case GEEN        -> hits > 0 ? Boolean.FALSE : null;
            case TEN_MINSTE  -> hits >= drempel ? Boolean.TRUE
                              : (hits + rest < drempel ? Boolean.FALSE : null);
            case TEN_HOOGSTE -> hits > drempel ? Boolean.FALSE
                              : (hits + rest <= drempel ? Boolean.TRUE : null);
            case PRECIES     -> (hits > drempel || hits + rest < drempel) ? Boolean.FALSE : null;
        };
    }

    /** The verdict once every member has been read: {@code hits} of {@code n}, against {@code drempel}. The
     *  full-read half of {@link #vastgesteld} and the one place the five quantifiers are one comparison —
     *  ALLE is hits == n, GEEN is hits == 0, and the counting three are the threshold comparisons of
     *  RUNTIME-SEMANTICS ch. 3.9a. Used by the quantifier fold, over CONTRIBUTORS instead of bullets. */
    private static boolean telVerdict(Conditie.Kwantor kw, int drempel, int hits, int n) {
        return switch (kw) {
            case ALLE, DE    -> hits == n;
            case GEEN        -> hits == 0;
            case TEN_MINSTE  -> hits >= drempel;
            case TEN_HOOGSTE -> hits <= drempel;
            case PRECIES     -> hits == drempel;
        };
    }

    private Gate voorwaarde(Conditie c, PatternDSL.PatternDef pattern, Objecttype subject, Variable subjectVar) {
        if (c instanceof Conditie.Samengesteld s) {
            // §13.4.14 ALLE / DE: AND its parts as successive in-pattern constraints; carry up far-side joins.
            if (s.kwantor() == Conditie.Kwantor.ALLE || s.kwantor() == Conditie.Kwantor.DE) {
                List<RuleItemBuilder<?>> extra = new ArrayList<>();
                for (Conditie sub : s.onderdelen()) {
                    Gate g = voorwaarde(sub, pattern, subject, subjectVar);
                    if (g == null) return null;
                    pattern = g.pattern();
                    extra.addAll(g.extra());
                }
                return new Gate(pattern, extra);
            }
            // WF-112's composite arm: a window under GEEN or a counting quantifier, at any depth. The fact-only
            // fold below cannot bind the rekendatum and would decline anyway, but only by the generic scope note;
            // the position is ill-formed, and says so in the sentence the DRL-text branch gives too.
            String vensterWeigering = Vensterpositie.weigering(s);
            if (vensterWeigering != null) {
                String snip = s.bron() == null ? null : s.bron().snippet();
                overgeslagen.add(vensterWeigering + (snip == null || snip.isEmpty() ? "" : ": " + snip));
                return null;
            }
            // §13.4.14.4 a SINGLE aggregation bullet under GEEN / "ten minste 1": the quantifier reduces to a plain
            // boolean transform of the one leaf (GEEN = NOT the leaf, ten minste 1 = the leaf). The counting fold below
            // is fact-only (Predicate1) and CANNOT host an aggregation leaf (which needs an accumulate CE), so route it
            // to aggregatieVoorwaarde with a boolean-NEGATION flag for GEEN (leegwaarde-correct, unlike an operator-flip).
            // >1 bullet / ten hoogste / precies / an unlowerable RHS fall through to the fact-only fold below -> skip.
            if ((s.kwantor() == Conditie.Kwantor.GEEN
                 || (s.kwantor() == Conditie.Kwantor.TEN_MINSTE && s.drempel() == 1))
                    && s.onderdelen().size() == 1
                    && s.onderdelen().get(0) instanceof Conditie.Vergelijking av && heeftAggregatieOperand(av))
                return aggregatieVoorwaarde(av, pattern, subject, subjectVar, s.kwantor() == Conditie.Kwantor.GEEN);
            // §13.4.14.4 counting quantifiers: GEEN = none hold, TEN_MINSTE/TEN_HOOGSTE/PRECIES = count vs N. The
            // children fold with ||/!/count, so a hoisted exists/not (or any non-leaf) cannot ride along — every
            // onderdeel must reduce to a fact-only Predicate1, else the WHOLE compound skips (never a partial gate).
            List<Predicate1<Object>> kinderen = new ArrayList<>();
            for (Conditie sub : s.onderdelen()) {
                Predicate1<Object> lp = leafPredicate(sub, subject);
                if (lp == null) return null;
                kinderen.add(lp);
            }
            final Conditie.Kwantor kw = s.kwantor();
            final int n = s.drempel();
            final int leden = kinderen.size();
            // RUNTIME-SEMANTICS ch. 5.10: read the members left to right in WIRE order and stop at
            // the first one after which the outcome stands. The conjunctive arm above gets this for
            // free (successive in-pattern constraints are an alpha chain), so the counting fold is
            // the only place the backend has to spell it out — and it must, because a member the
            // outcome no longer reads may be exactly the one that would throw (a division by the
            // zero an earlier member tested for). Skipping a settled member can never move the
            // verdict: hits only grows and hits+rest only shrinks, so every trigger below already
            // decides the comparison. Witness: corpus golden jutterswet-wachtdeling.
            Predicate1<Object> combined = sObj -> {
                int telling = 0;
                for (int i = 0; i < leden; i++) {
                    Boolean vast = vastgesteld(kw, n, telling, i, leden - i);
                    if (vast != null) return vast;
                    if (kinderen.get(i).test(sObj)) telling++;
                }
                return switch (kw) {
                    case GEEN        -> telling == 0;
                    case TEN_MINSTE  -> telling >= n;
                    case TEN_HOOGSTE -> telling <= n;
                    case PRECIES     -> telling == n;
                    default          -> false;   // unreachable (alle/de handled above)
                };
            };
            return new Gate(pattern.expr(nextId("q"), (Predicate1) combined), List.of());
        }
        // IR-SPEC ch. 4.5 / RUNTIME-SEMANTICS ch. 3.9a the QUANTIFIER FOLD — Samengesteld's first-order sibling,
        // quantifying over INSTANCES instead of over the bullets of one subject. Placed here so the two read
        // together; it cannot be reached by the branches below (they all key on other node types).
        if (c instanceof Conditie.Kwantorvouw kv)
            return kwantorvouwVoorwaarde(kv, pattern, subject, subjectVar);
        // §8.4.3 / §13.4.15.68-.69 periodevergelijking: the rule applies during the period; the engine evaluates
        // scalar rules as of the §4.2 Rekendatum, so bind the rekendatum global as a pattern variable and test
        // "rekendatum inside [vanaf, tot)" (§5.1.3: vanaf inclusive, tot exclusive, tot en met inclusive) — the
        // expression-bound analogue of geldigGuard. Bounds share Tijdrekening.periodeGrensLowerbaar with the DRL
        // back-end (a Datum-in-dagen literal or the subject's own scalar datum attribute); an unsupported bound /
        // leeg bound -> skip / no match, symmetric with the DRL marker & null-guards. Under GEEN or a COUNTING
        // quantifier the window never gets here: that position is WF-112's, refused by name at the composite and
        // at the fold (kern.Vensterpositie, the sentence the DRL side gives too). The reading itself is
        // vensterToets, shared with an ALLE fold's toets and filter.
        if (c instanceof Conditie.Periode per) {
            if (rekendatumGlobal == null) return null;   // defensive: heeftTijdgebonden registers it
            Predicate2<Object, Object> venster = vensterToets(per, subject);
            if (venster == null) return null;
            return new Gate(pattern.expr(nextId("per"), (Variable) rekendatumGlobal, (Predicate2) venster), List.of());
        }
        // §8.2 / §13.4.15.70 "gedurende de/het gehele jaar|maand": a whole-period check over a tijdsafhankelijk
        // onderwerp. A literal/own-attr grens (0 globals) is a fact-only leaf (handled in leafPredicate, reached by
        // the fallthrough); a Parameter-global grens binds the global as a pattern variable (a 2-var expr), like the
        // §6 datum-arith branch. An unsupported form -> skip the whole rule (symmetric with the DRL marker -> no fire).
        Conditie.GeheelPeriode gp82 = geheelPeriodeVan(c);
        if (gp82 != null) {
            // §8.2 over a NAVIGATED to-many temporal SET: a forall across the accumulated element timelines — a
            // collectList accumulate binding List<List<Segment>> + an rsGeheelPeriodeAlle result-var eval. Mirrors DRL.
            Gate alle = geheelPeriodeAlleVoorwaarde(c, gp82, pattern, subject, subjectVar);
            if (alle != null) return alle;
            Gp82 g = geheelPeriode82(c, gp82, subject);
            if (g == null) return null;
            Predicate1<Object> leaf = geheelPeriodeLeaf(g);
            if (leaf != null) return new Gate(pattern.expr(nextId("c"), (Predicate1) leaf), List.of());
            if (g.grens() != null && g.grens().globals().size() == 1) {
                String gn = g.grens().globals().iterator().next();
                Global<BigDecimal> gg = globals.get(gn);
                if (gg == null) return null;
                java.util.function.Function<Object, Object> tl = g.timeline(); WaardeFn gf = g.grens();
                int toets = g.toets(); String gran = g.gran(); boolean niet = g.niet();
                return new Gate(pattern.expr(nextId("c"), (Variable) gg, (Predicate2) (s, gv) -> {
                    BigDecimal grens = RegelSpraakRuntime.toBd(gf.fn().apply(s, bindGlobal(gn, gv)));
                    boolean b = RegelSpraakRuntime.rsGeheelPeriode((List<?>) tl.apply(s), gran, toets, grens);
                    return niet ? !b : b;
                }), List.of());
            }
            return null;   // >1 global / unresolved -> skip
        }
        // §5.8/§13.4.14 an AGGREGATION (count / sum / totaal / min / max) as one side of a condition comparison -> a
        // result-constrained accumulate CE hoisted after the subject pattern (Gate.extra), the exec mirror of the DRL
        // accumulate + eval. A comparison that HAS an aggregation operand is routed EXCLUSIVELY here: aggregatieVoorwaarde
        // returns the Gate, or null when it cannot lower it (a navigated / multi- or non-numeric-global RHS, agg-vs-agg) -> the whole
        // rule SKIPS. It must NOT fall through to the scalar / §5.5.5 navigated-comparison handlers below, which would
        // mis-build the aggregation LEFT into a LIVE crashing rule (review wf_900d3055-a62 F2). The DRL fall-through
        // markers safely, so only the exec needs this guard.
        if (c instanceof Conditie.Vergelijking cv && heeftAggregatieOperand(cv))
            return aggregatieVoorwaarde(cv, pattern, subject, subjectVar);
        // §8.1.9 / §13.4.15.66 regelpredicaat "regelversie <X> is gevuurd|inconsistent". "is inconsistent"
        // mirrors the DRL exists-join on the §9.5 Inconsistentie flag fact of the named consistentie/
        // uniciteit/verdeling rule (a separate rule item, carried up through ALLE/DE like the far-side
        // rolcheck below). "is gevuurd" (no runtime record) and an unknown producer name skip the whole
        // rule — symmetric with the DRL marker, never a silent never-matching exists.
        if (c instanceof Conditie.Regelversie rv) {
            String rvNaam = rv.regel();
            // §8.1.9 "is inconsistent": exists on the §9.5 Inconsistentie flag fact of the named producer.
            if (rv.inconsistent() && model.inconsistentieProducent(rvNaam)) {
                Class<?> incClass = inconsistentieClass();
                if (incClass == null) return null;
                Variable iVar = DSL.declarationOf(incClass, "$rv_inc_" + (seq++));
                PatternDSL.PatternDef ip = PatternDSL.pattern(iVar)
                        .expr(nextId("rv"), (Predicate1) i -> rvNaam.equals(RegelSpraakRuntime.get(i, "regel")));
                return new Gate(pattern, List.of(DSL.exists(ip)));
            }
            // §8.1.9 "is gevuurd" over an instrumented main-regel() producer: exists on the RuleFired marker its
            // gevuurdCompanion inserts (mirrors the DRL exists-join; re-activated when the producer fires later).
            if (!rv.inconsistent() && model.gevuurdProducent(rvNaam)) {
                Variable fVar = DSL.declarationOf(RuleFired.class, "$rv_fired_" + (seq++));
                PatternDSL.PatternDef fp = PatternDSL.pattern(fVar)
                        .expr(nextId("rvf"), (Predicate1) f -> rvNaam.equals(((RuleFired) f).getRegel()));
                return new Gate(pattern, List.of(DSL.exists(fp)));
            }
            return null;   // un-instrumented / unknown -> skip (symmetric with the DRL marker)
        }
        // §8.1.7 far-side "heeft een <rol>": the reference lives on the to-many filler, so this is an exists-join —
        // a SEPARATE rule item that cannot live inside the subject pattern. It binds the filler against $subject.
        if (c instanceof Conditie.VerreRollidmaatschap vr) {
            Class<?> fillerClass = factClass(vr.filler());
            if (fillerClass == null) return null;
            String backref = vr.backref();
            // §13.3.9: the exists-join READS the filler's reference field back at the subject, so it must ask the
            // same question the near-side rolcheck asks of its own end — the backref is a role NAME, and a name is
            // not yet a field. Measured 2026-09-11 on a verifier-clean model whose ligplaats has BOTH roles to-many
            // (no field at either end): the exec branch built the join and died at first fire
            // (ERROR ConstraintEvaluationException), the DRL branch sank the whole package on a bare
            // "Unable to Analyse Expression werf == $subject". Neither names the limit; both must.
            // What this reader can NOT be measured against is a value: ref and clingo carry no far-side rolcheck at
            // all ("condition kind not implemented in this card" / "condition kind not translated"), so no golden of
            // this shape can enter the corpus and the differential square can never arbitrate it. The refusal is
            // therefore about the DIAGNOSIS, not about a divergence — which is exactly why it must carry one.
            if (subject == null || !heeftVeld("verre rollidmaatschapscheck", vr.filler(), backref, subject)) return null;
            Variable fVar = DSL.declarationOf(fillerClass, "$filler_" + (seq++));
            PatternDSL.PatternDef fp = PatternDSL.pattern(fVar).expr(nextId("br"), (Variable) subjectVar,
                    (Predicate2) (f, subj) -> RegelSpraakRuntime.get(f, backref) == subj);
            RuleItemBuilder<?> item = vr.negatie() ? DSL.not(fp) : DSL.exists(fp);
            return new Gate(pattern, List.of(item));
        }
        // §8.1.1 a comparison written with the Parameter-GLOBAL on the LEFT ("de DREMPEL groter is dan zijn prijs", "de
        // PARAMETERDATUM later is dan zijn datum", "de STANDAARDDATUM gelijk is aan zijn datum") — DRL reads the
        // global as a free name and fires, but every exec vs-global branch keys on an AttribuutRef/expressie LHS + a
        // ParameterRef RHS, so a global-LHS skipped. Normalise: swap the operands and REVERSE the operator (omkeren),
        // then the existing vs-global branches lower it (numeric / datum / tekst, own + navigated + expr). Safe by
        // construction — the rewrite triggers ONLY when the LHS is a ParameterRef and the RHS is not (after the swap the
        // ParameterRef sits on the RIGHT, so it cannot re-trigger). A global-vs-literal (rare) stays a skip either way.
        if (c instanceof Conditie.Vergelijking gv && gv.links() instanceof Expr.ParameterRef
                && !(gv.rechts() instanceof Expr.ParameterRef))
            return voorwaarde(new Conditie.Vergelijking(Operatoren.omkeren(gv.op()), gv.rechts(), gv.links(), gv.periode(), gv.bron()),
                    pattern, subject, subjectVar);
        // §5.3 the same normalisation for a Rekendatum/Rekenjaar CONSTANT on the LEFT ("Rekendatum eerder is dan
        // zijn einddatum") — swap+reverse so the vs-Rekendatum / expression branches below see it on the RIGHT.
        if (c instanceof Conditie.Vergelijking gv && gv.links() instanceof Expr.Constante lrc && Expr.isRekenConstante(lrc)
                && !(gv.rechts() instanceof Expr.Constante))
            return voorwaarde(new Conditie.Vergelijking(Operatoren.omkeren(gv.op()), gv.rechts(), gv.links(), gv.periode(), gv.bron()),
                    pattern, subject, subjectVar);
        // §8.1.1 a DATUM comparison of an OWN or to-one NAVIGATED datum attribute vs a Datum-in-dagen Parameter-GLOBAL
        // ("zijn datum eerder is dan de PARAMETERDATUM", "de datum van zijn partner gelijk is aan de PARAMETERDATUM").
        // BEFORE the numeric vs-global branch (whose `!isNumeriekOp -> return null` would otherwise swallow a datum op).
        // DRL reads the date global as a free name; the exec binds the LocalDate global as a pattern variable and
        // compares via datumVergelijkTemporal (§5 leegwaarde tables). A mixed Datum-in-dagen/Datum-en-tijd pair is a
        // §5.3 Tabel 36 mismatch -> skip; a Datum-en-tijd global (untracked, datumGlobals.get == null) -> honest skip.
        if (c instanceof Conditie.Vergelijking v && isDatumVergelijkOp(v.op())
                && v.links() instanceof Expr.AttribuutRef la && la.type() != null && la.type().isDatum()
                && v.rechts() instanceof Expr.ParameterRef pr && pr.parameter().datatype() != null
                && pr.parameter().datatype().isDatum()) {
            if (!sameDatumKind(la.type(), pr.parameter().datatype())) return null;
            java.util.function.Function<Object, Object> lf = datumTemporeelLezer(la, subject);
            // Datum-in-dagen -> the LocalDate global; Datum-en-tijd -> the LocalDateTime global. datumVergelijkTemporal
            // compares any Temporal, so the bound value's runtime type is irrelevant to the comparison.
            Variable g = pr.parameter().datatype() instanceof Datatype.DatumTijd
                    ? (Variable) datumTijdGlobals.get(globalNaam(pr.parameter().naam()))
                    : (Variable) datumGlobals.get(globalNaam(pr.parameter().naam()));
            if (lf != null && g != null) {
                Conditie.Operator op = v.op();
                return new Gate(pattern.expr(nextId("c"), g, (Predicate2) (s, gv) ->
                        datumVergelijkTemporal(lf.apply(s), gv, op)), List.of());
            }
            return null;   // unresolvable nav -> honest skip (symmetric with DRL marker)
        }
        // §5.3 a DATUM comparison of an OWN or to-one NAVIGATED Datum-in-dagen attribute vs REKENDATUM ("zijn
        // datum eerder is dan Rekendatum"): bind the §4.2 rekendatum global as the pattern variable and
        // compare via datumVergelijkTemporal (ordering AND gelijk/ongelijk — the §5 leegwaarde tables; an unset
        // rekendatum reads leeg). DRL reads the global as a free name in the same constraint.
        if (c instanceof Conditie.Vergelijking v
                && (isDatumVergelijkOp(v.op()) || v.op() == Conditie.Operator.GELIJK || v.op() == Conditie.Operator.ONGELIJK)
                && v.links() instanceof Expr.AttribuutRef la && la.type() instanceof Datatype.DatumInDagen
                && v.rechts() instanceof Expr.Constante rc && "Rekendatum".equals(rc.naam())) {
            java.util.function.Function<Object, Object> lf = datumTemporeelLezer(la, subject);
            if (lf == null || rekendatumGlobal == null) return null;   // unresolvable nav -> honest skip
            Conditie.Operator op = v.op();
            return new Gate(pattern.expr(nextId("c"), (Variable) rekendatumGlobal, (Predicate2) (s, rd) ->
                    datumVergelijkTemporal(lf.apply(s), rd instanceof java.time.LocalDate d ? d : null, op)), List.of());
        }
        // §5.3 a TEKST/enumeratiewaarde equality (GELIJK/ONGELIJK) of an OWN or to-one NAVIGATED attribute vs a Tekst
        // Parameter-GLOBAL ("zijn naam gelijk is aan de STANDAARDNAAM"). BEFORE the numeric vs-global branch (whose
        // globals.get == null for a tekst global would `return null` -> skip). DRL reads the tekst global as a free name;
        // the exec binds the String global as a pattern variable. §5.5.5 a null relation / leeg attr reads LEEG, then
        // string equality with the §5 leegwaarde tables (GELIJK Tabel 38 one/both-leeg onwaar; ONGELIJK Tabel 44 one-leeg WAAR).
        if (c instanceof Conditie.Vergelijking v && (v.op() == Conditie.Operator.GELIJK || v.op() == Conditie.Operator.ONGELIJK)
                && v.links() instanceof Expr.AttribuutRef la && tekstachtig(la.type())
                && v.rechts() instanceof Expr.ParameterRef pr && tekstachtig(pr.parameter().datatype())) {
            if (la.objecttype() == null || Namen.bevatNaam(la.objecttype().granulariteitLeden(), la.attribuut())) return null;  // temporal -> Phase F
            if (!la.objecttype().dimensiesVan(la.attribuut()).isEmpty()) return null;                                       // §3.6 -> marker
            Global<String> g = tekstGlobals.get(globalNaam(pr.parameter().naam()));
            if (g == null) return null;
            String attr = la.attribuut();
            boolean gelijk = v.op() == Conditie.Operator.GELIJK;
            java.util.function.UnaryOperator<Object> n = gelijkheidsLezing(la.type(), pr.parameter().datatype());
            if (ongenavigeerd(la)) {
                if (subject == null || !subject.heeftLid(attr)) return null;
                return new Gate(pattern.expr(nextId("c"), (Variable) g, (Predicate2) (s, gv) ->
                        tekstVergelijk(n.apply(RegelSpraakRuntime.get(s, attr)), n.apply(gv), gelijk)), List.of());
            }
            String rol = navComparisonRole(la, subject);
            if (rol == null) return null;
            return new Gate(pattern.expr(nextId("c"), (Variable) g, (Predicate2) (s, gv) -> {
                Object rel = RegelSpraakRuntime.get(s, rol);
                return tekstVergelijk(n.apply(rel == null ? null : RegelSpraakRuntime.get(rel, attr)), n.apply(gv), gelijk);   // §5.5.5 null relation -> leeg
            }), List.of());
        }
        // §8.1.1 a comparison vs a numeric Parameter-GLOBAL needs a 2-var expr (the global as a pattern variable),
        // so it stays here rather than in leafPredicate (a vs-literal comparison IS a fact-only leaf).
        if (c instanceof Conditie.Vergelijking v && v.links() instanceof Expr.AttribuutRef la && ongenavigeerd(la)
                && v.rechts() instanceof Expr.ParameterRef pr) {
            if (subject == null || !subject.heeftLid(la.attribuut())) return null;   // not the subject's own member (feitcreatie anker-first probe)
            if (Namen.bevatNaam(subject.granulariteitLeden(), la.attribuut())) return null;  // temporal -> Phase F
            if (!subject.dimensiesVan(la.attribuut()).isEmpty()) return null;   // §3.6 gedimensioneerd (a Map) -> marker
            if (!isNumeriekOp(v.op())) return null;   // EERDER/LATER (datum) -> Phase later
            Global<BigDecimal> g = globals.get(globalNaam(pr.parameter().naam()));
            if (g == null) return null;
            String attr = la.attribuut();
            Conditie.Operator op = v.op();
            BigDecimal[] f = restatement(effType(la), pr.parameter().datatype());  // §5.1 Tabel 32 unit restatement
            return new Gate(pattern.expr(nextId("c"), (Variable) g,
                    (Predicate2) (s, gv) -> vergelijk(scale(num(s, attr), f[0]), scale((BigDecimal) gv, f[1]), op)), List.of());
        }
        // §8.1.1 a to-ONE NAVIGATED numeric comparison vs a Parameter-GLOBAL — the §12 "de duur van
        // zijn partner groter is dan <de bovengrens schijf>" shape. The global is a pattern variable (2-var expr),
        // navReader reads the navigated value null-safely. Mirrors DRL `partner!.x op global`.
        if (c instanceof Conditie.Vergelijking v && v.links() instanceof Expr.AttribuutRef la && !ongenavigeerd(la)
                && la.type().isGetalachtig() && isNumeriekOp(v.op()) && v.rechts() instanceof Expr.ParameterRef pr) {   // §4.8 numeriek OR percentage
            if (la.objecttype() == null || Namen.bevatNaam(la.objecttype().granulariteitLeden(), la.attribuut())) return null;  // temporal -> Phase F
            if (!la.objecttype().dimensiesVan(la.attribuut()).isEmpty()) return null;                                       // §3.6 gedimensioneerd -> marker
            String rol = navComparisonRole(la, subject);
            if (rol == null) return null;
            Global<BigDecimal> g = globals.get(globalNaam(pr.parameter().naam()));
            if (g == null) return null;
            String attr = la.attribuut();
            Conditie.Operator op = v.op();
            BigDecimal[] f = restatement(effType(la), pr.parameter().datatype());  // §5.1 Tabel 32 unit restatement
            return new Gate(pattern.expr(nextId("c"), (Variable) g, (Predicate2) (s, gv) -> {
                // §5.5.5 an ABSENT relation reads the navigated attribute as LEEG (null), then vergelijk's §5 leegwaarde
                // tables decide — NOT a blanket short-circuit-to-false (which was WRONG for ONGELIJK: leeg != global is
                // WAAR, §5.6 Tabel 44). Ordering / GELIJK of a leeg operand stay onwaar (Tabel 33-42); only ONGELIJK
                // flips to WAAR. Mirrors the standalone navigated comparison (3eb94d0) and DRL's leegwaarde-faithful form.
                Object rel = RegelSpraakRuntime.get(s, rol);
                BigDecimal lv = rel == null ? null : RegelSpraakRuntime.toBd(RegelSpraakRuntime.get(rel, attr));
                return vergelijk(scale(lv, f[0]), scale((BigDecimal) gv, f[1]), op);
            }), List.of());
        }
        // §8.1.1 a NUMERIC comparison where an operand is an EXPRESSION (arithmetic Binair / Functie / begrenzing /
        // afronding) and the comparison reads EXACTLY ONE numeric/percentage Parameter-GLOBAL — "zijn prijs plus de
        // toeslag groter is dan de drempel", "zijn prijs maal 2 groter is dan de grens", "de waarde van zijn partner
        // plus de toeslag groter is dan zijn prijs". Bind that one global as a pattern variable (a 2-var expr), lowering
        // BOTH sides via scalarWaarde. DRL reads the global as a free name in the constraint operand. A NAVIGATED operand
        // inside the expr lowers via scalarWaarde's navigated read — the subject rides as the value-path parameter (no-mutable-build-state,
        // like the leaf expr-comparison branch) so navReader resolves; an absent relation reads leeg -> leeg-as-0 in the
        // arithmetic (DRL's rsRelGetal mirror). 0 globals -> falls through to leafPredicate's fact-only expr-comparison
        // branch; >1 distinct globals -> falls through -> skip (a condition expr binds at most one global -> the N-var
        // PatternDSL.expr is a carded follow-up). parityOnveilig (a §3.6 dimensiecel / cross-unit +/-) -> skip (DRL marker).
        // Mirrors the date-arith-vs-global branch below.
        if (c instanceof Conditie.Vergelijking v && isNumeriekOp(v.op())
                && getalachtigVergelijkbaar(v.links())
                && getalachtigVergelijkbaar(v.rechts())
                && (numeriekeExpressie(v.links()) || numeriekeExpressie(v.rechts()))
                && !(parityOnveilig(v.links()) || parityOnveilig(v.rechts()))) {
            WaardeFn lw = scalarWaarde(subject, v.links()), rw = scalarWaarde(subject, v.rechts());
            if (lw != null && rw != null) {
                Set<String> gset = new LinkedHashSet<>(lw.globals()); gset.addAll(rw.globals());
                if (gset.size() == 1) {
                    String gn = gset.iterator().next();
                    Global<?> g = waardeGlobal(gn);   // numeric Parameter or the §5.3 rekendatum pseudo-slot
                    if (g != null) {   // (a date/tekst Parameter-global -> null -> fall through -> skip)
                        BigDecimal[] f = restatement(effType(v.links()), effType(v.rechts()));   // §5.1 Tabel 32 unit restatement
                        BiFunction<Object, Map<String, BigDecimal>, Object> lfn = lw.fn(), rfn = rw.fn();
                        Conditie.Operator op = v.op();
                        return new Gate(pattern.expr(nextId("c"), (Variable) g, (Predicate2) (s, gv) ->
                                vergelijk(scale((BigDecimal) lfn.apply(s, bindGlobal(gn, gv)), f[0]),
                                          scale((BigDecimal) rfn.apply(s, bindGlobal(gn, gv)), f[1]), op)), List.of());
                    }
                } else if (gset.size() >= 2) {
                    // §8.1.1 a comparison reading 2..3 DISTINCT numeric globals ("zijn basistarief plus de toeslag groter
                    // is dan de grens") — bind each via DSL.from into its own declaration (no exec-model collapse), the
                    // comparison rides the last from-declaration's pattern. DRL reads each global as a free name.
                    BigDecimal[] f = restatement(effType(v.links()), effType(v.rechts()));
                    BiFunction<Object, Map<String, BigDecimal>, Object> lfn = lw.fn(), rfn = rw.fn();
                    Conditie.Operator op = v.op();
                    Gate g = nGlobalGate(new ArrayList<>(gset), pattern, subjectVar, (s, gm) ->
                            vergelijk(scale((BigDecimal) lfn.apply(s, gm), f[0]), scale((BigDecimal) rfn.apply(s, gm), f[1]), op));
                    if (g != null) return g;   // >3 globals / a non-numeric global -> fall through -> skip
                }
            }
            // 0 globals -> leafPredicate's expr-comparison branch; unresolved / non-numeric global -> falls through -> skip
        }
        // §8.1.1 a DATUM comparison where an operand is a §6/§13.4.16 date-arithmetic expression that reads EXACTLY
        // ONE Parameter-GLOBAL (the duration is a parameter, e.g. "de datum plus de DUURPARAMETER
        // eerder is dan de PARAMETERDATUM"): bind that global as a pattern variable (a 2-var expr) and evaluate
        // datumVergelijkTemporal. DRL binds the global directly in the rsDatumReken constraint. 0 globals -> the
        // fact-only leafPredicate below; >1 global -> skip (a condition expr binds at most one global, like the
        // numeric vs-global branches above).
        if (c instanceof Conditie.Vergelijking v && isDatumVergelijkOp(v.op())
                && v.links().type().isDatum() && v.rechts().type().isDatum()
                && (isDatumRekenExpr(v.links()) || isDatumRekenExpr(v.rechts()))) {
            WaardeFn lw = scalarWaarde(null, v.links()), rw = scalarWaarde(null, v.rechts());
            if (lw != null && rw != null) {
                Set<String> gset = new LinkedHashSet<>(lw.globals()); gset.addAll(rw.globals());
                if (gset.size() == 1) {
                    String gn = gset.iterator().next();
                    Global<?> g = waardeGlobal(gn);   // numeric Parameter or the §5.3 rekendatum pseudo-slot
                    if (g != null) {
                        BiFunction<Object, Map<String, BigDecimal>, Object> lfn = lw.fn(), rfn = rw.fn();
                        Conditie.Operator op = v.op();
                        return new Gate(pattern.expr(nextId("c"), (Variable) g, (Predicate2) (s, gv) ->
                                datumVergelijkTemporal(lfn.apply(s, bindGlobal(gn, gv)), rfn.apply(s, bindGlobal(gn, gv)), op)), List.of());
                    }
                }
            }
            // 0 globals -> falls through to leafPredicate (covered); >1 / unresolved -> falls through -> skip
        }
        // §8.1.5 dagsoortcontrole "<datum> is een <dagsoort>" whose datum operand reads EXACTLY ONE NUMERIC Parameter-
        // GLOBAL (a §6 date-arithmetic expression whose duration is a numeric parameter, "de datum plus de DUURPARAMETER
        // een werkdag is"): bind that global as a pattern variable (a 2-var expr), mirroring the date-arith-vs-global
        // comparison branch above. DRL calls the rsDagsoort_<N> predicate GLOBAL on the free-name global directly; the
        // exec pattern needs it as a Variable. A fact-only (0-global) operand falls through to leafPredicate. The cases
        // this branch does NOT take — a >1-global operand, a NAVIGATED operand (scalarWaarde's nav read needs the
        // value-path subject, null on the condition path), or a non-numeric date-typed global (globals.get == null) —
        // fall through to an HONEST RECORDED skip (overgeslagen). Those are NOT byte-parity with DRL (DRL fires them);
        // the differential EXCLUDES an overgeslagen fixture, so each is a documented asymmetry, never a SILENT
        // divergence — close-list follow-ups, not silent wrongs. §5.15: an absent/non-date value is ONWAAR.
        if (c instanceof Conditie.Predicaat dp && dp.soort() == Conditie.Predicaatsoort.DAGSOORT && dp.operand().type().isDatum()) {
            WaardeFn wf = scalarWaarde(null, dp.operand());
            if (wf != null && wf.globals().size() == 1) {
                String gn = wf.globals().iterator().next();
                Global<BigDecimal> g = globals.get(gn);   // null for a non-numeric (date/tekst) global -> honest skip below
                java.util.function.Predicate<java.time.LocalDate> pred = dagsoortPredikaat(dp.dagsoort());
                if (g != null && pred != null) {
                    BiFunction<Object, Map<String, BigDecimal>, Object> fn = wf.fn();
                    boolean neg = dp.negatie();   // §8.1.5 + Tabel 16 "is geen <dagsoort>"
                    return new Gate(pattern.expr(nextId("c"), (Variable) g, (Predicate2) (s, gv) -> {
                            java.time.LocalDate ld = asDag(fn.apply(s, bindGlobal(gn, gv))); return neg != (ld != null && pred.test(ld)); }), List.of());
                }
            }
            // 0 globals -> the fact-only leafPredicate below; >1 / navigated / date-typed -> fall through -> honest skip
        }
        // every other supported condition is a fact-only leaf.
        Predicate1<Object> lp = leafPredicate(c, subject);
        if (lp != null) return new Gate(pattern.expr(nextId("c"), (Predicate1) lp), List.of());
        return null;
    }

    /** §5.3/§5.6 TEKST equality with the leegwaarde tables (the String analogue of the datum equality): GELIJK is Tabel 38
     *  (both present and equal; one/both-leeg -> onwaar); ONGELIJK is Tabel 44 (exactly-one-leeg -> WAAR, both-leeg ->
     *  onwaar). Matches DRL mvel's null-asymmetric string {@code ==}/{@code !=}. */
    private static boolean tekstVergelijk(Object a, Object b, boolean gelijk) {
        return gelijk ? (java.util.Objects.equals(a, b) && !(a == null && b == null))
                      : !java.util.Objects.equals(a, b);
    }
    /** §13.3.3.4 Tekst OR §13.2.14 Enumeratie — both compare by String-label equality (an enumeratiewaarde is stored as
     *  its label), so an enum-vs-global equality lowers through the same tekst-vs-global branch + {@link #tekstVergelijk}. */
    private static boolean tekstachtig(Datatype t) { return t != null && (t.isTekst() || t instanceof Datatype.Enumeratie); }
    /** The read an equality applies to each of its operand VALUES: an enumeration label is a DECLARED NAME, so when
     *  either side is an enumeration both are read through {@link RegelSpraakRuntime#rsEnumNaam} — the identity ref
     *  and clingo compare by, and the one the DRL-text branch emits — while a Tekst or Booleaan keeps exact equality.
     *  "Either side", because the RegelSpraak-source path can still hand over an Onbekend-typed quoted literal
     *  whose only enumeration evidence is the attribute across from it. */
    private static java.util.function.UnaryOperator<Object> gelijkheidsLezing(Datatype l, Datatype r) {
        return l instanceof Datatype.Enumeratie || r instanceof Datatype.Enumeratie
                ? RegelSpraakRuntime::rsEnumNaam : java.util.function.UnaryOperator.identity();
    }
    /** A NON-NUMERIC Parameter-global (Tekst/Enumeratie String, Datum LocalDate, or Datum-en-tijd LocalDateTime) by its
     *  ident, or null. Used to assign such a global verbatim into an attribute (§13.4.6) — the numeric value-path binds
     *  globals as BigDecimal, so these need a directly-typed bind. (rekendatum is engine-internal, never a value source.) */
    private Global<?> nietNumeriekeGlobal(String gn) {
        if (tekstGlobals.containsKey(gn)) return tekstGlobals.get(gn);
        if (datumGlobals.containsKey(gn)) return datumGlobals.get(gn);
        if (datumTijdGlobals.containsKey(gn)) return datumTijdGlobals.get(gn);
        return null;
    }

    /** A condition that reduces to a fact-only {@code Predicate1<subject>} (no pattern variable, no exists-join), or
     *  null when it is outside scope / needs a global / hoists a join. Shared by the single-constraint path and the
     *  counting-quantifier fold, so the two stay in lock-step. Covers §8.1.8 own + to-one navigated kenmerkcheck,
     *  §8.1.2 LEEG/GEVULD on an own attribute, §8.1.1 comparison of an own attribute vs a numeric literal, and the
     *  §8.1.7 near-side rolcheck. A temporal (List<Segment>) operand returns null (Phase F). */
    private Predicate1<Object> leafPredicate(Conditie c, Objecttype subject) {
        // §8.2 a whole-period-qualified predicate over a tijdsafhankelijk onderwerp, with a literal/own-attr grens
        // (0 globals) -> a fact-only "the check holds" predicate. A grens reading a Parameter-global returns null here
        // (handled with a bound global in voorwaarde); inside a counting quantifier that null skips the whole compound.
        Conditie.GeheelPeriode gp82 = geheelPeriodeVan(c);
        if (gp82 != null) return geheelPeriodeLeaf(geheelPeriode82(c, gp82, subject));
        // §8.1.1 a comparison written with the CONSTANT on the LEFT ("100 groter is dan zijn waarde", "1-1-2020
        // eerder is dan zijn datum", "1-1-2021 gelijk is aan zijn datum") — DRL fires `100 > attr` /
        // `literal < attr` directly, but every exec leaf branch keys on an AttribuutRef/expressie LHS, so a literal-LHS
        // comparison SKIPPED. Normalise it: swap the operands and REVERSE the operator (omkeren), then the existing
        // attr-on-LHS branches lower it. Safe by construction — only a literal-LHS comparison (which otherwise skips) is
        // rewritten, so no firing path can regress; after the swap the literal sits on the RIGHT (no re-trigger).
        if (c instanceof Conditie.Vergelijking vlit && vlit.links() instanceof Expr.Literal
                && !(vlit.rechts() instanceof Expr.Literal))
            return leafPredicate(new Conditie.Vergelijking(Operatoren.omkeren(vlit.op()), vlit.rechts(), vlit.links(),
                    vlit.periode(), vlit.bron()), subject);
        // §8.1.8 kenmerkcheck — an own kenmerk (boolean property == !negatie) or a to-one navigated one
        // ("zijn partner heeft een <k>"), read null-safely through the reference (a null relation never matches).
        if (c instanceof Conditie.Kenmerk k) {
            // WF-109: the host is satisfied EXTENSIONALLY (subject ⊑ host), not by name identity — a
            // supertype host is this pattern's too. Shared with the DRL-text branch so the two cannot drift.
            if (!Gastheer.hostOp(k, subject)) return null;   // a feitcreatie doel-kenmerk hosts elsewhere
            KenmerkHop hop = k.navigatie() == null ? null : kenmerkHop(k, subject);
            if (k.navigatie() != null && hop == null) return null;   // the bound hop does not fit this subject
            Objecttype kHouder = k.navigatie() == null ? subject : hop.doel();
            if (kHouder != null && Namen.bevatNaam(kHouder.kenmerkGranulariteitLeden(), k.kenmerk())) return null;  // temporal -> Phase F
            String kenmerk = k.kenmerk();
            boolean wanted = !k.negatie();
            if (k.navigatie() == null)
                return s -> Boolean.TRUE.equals(RegelSpraakRuntime.get(s, kenmerk)) == wanted;
            String nav = hop.rol();
            return s -> {
                Object rel = RegelSpraakRuntime.get(s, nav);
                return rel != null && Boolean.TRUE.equals(RegelSpraakRuntime.get(rel, kenmerk)) == wanted;
            };
        }
        // §8.1.2 LEEG/GEVULD on an own attribute -> a null / non-null constraint.
        if (c instanceof Conditie.Predicaat p
                && (p.soort() == Conditie.Predicaatsoort.LEEG || p.soort() == Conditie.Predicaatsoort.GEVULD)
                && p.operand() instanceof Expr.AttribuutRef pa && ongenavigeerd(pa)) {
            if (subject == null || !subject.heeftLid(pa.attribuut())) return null;   // not the subject's own member (e.g. a feitcreatie anker-first probe of a doel condition)
            if (Namen.bevatNaam(subject.granulariteitLeden(), pa.attribuut())) return null;  // temporal -> Phase F
            if (!subject.dimensiesVan(pa.attribuut()).isEmpty()) return null;   // §3.6 gedimensioneerd (a Map) -> marker
            String attr = pa.attribuut();
            boolean leeg = p.soort() == Conditie.Predicaatsoort.LEEG;
            return s -> (RegelSpraakRuntime.get(s, attr) == null) == leeg;
        }
        // §8.1.2 LEEG/GEVULD over a to-ONE NAVIGATED operand ("de woonplaats van zijn partner is leeg"). Unlike a
        // comparison, this is NOT the mvel `!.` short-circuit: an ABSENT reference reads the value as EMPTY (typeringen
        // Tabel 53/54 — an absent value IS leeg -> waar), so DRL guards the reference explicitly (genavigeerdLeegGevuld).
        // leeg = (rol == null || rol.attr == null); gevuld = (rol != null && rol.attr != null).
        if (c instanceof Conditie.Predicaat p
                && (p.soort() == Conditie.Predicaatsoort.LEEG || p.soort() == Conditie.Predicaatsoort.GEVULD)
                && p.operand() instanceof Expr.AttribuutRef pa && !ongenavigeerd(pa)) {
            if (pa.objecttype() == null || Namen.bevatNaam(pa.objecttype().granulariteitLeden(), pa.attribuut())) return null;  // temporal -> Phase F
            if (!pa.objecttype().dimensiesVan(pa.attribuut()).isEmpty()) return null;   // §3.6 gedimensioneerd -> marker
            String rol = navComparisonRole(pa, subject);
            if (rol == null) return null;
            String attr = pa.attribuut();
            boolean leeg = p.soort() == Conditie.Predicaatsoort.LEEG;
            return s -> {
                Object rel = RegelSpraakRuntime.get(s, rol);
                boolean isLeeg = rel == null || RegelSpraakRuntime.get(rel, attr) == null;
                return isLeeg == leeg;
            };
        }
        // §8.1.3 elfproef / §8.1.4 numeriek-met-exact over an OWN or to-one NAVIGATED operand. Read the value (a Tekst
        // String or a Numeriek BigDecimal) and apply the runtime predicate XOR the §8.1 Tabel-16 negatie. A navigated
        // null relation makes the WHOLE constraint a non-match (onwaar for both polarities), mirroring the mvel
        // `rol!.attr` short-circuit in the DRL. The runtime helpers are byte-identical to the DRL rsElfproef /
        // rsNumeriekMetExact (the differential gate).
        if (c instanceof Conditie.Predicaat p
                && (p.soort() == Conditie.Predicaatsoort.ELFPROEF || p.soort() == Conditie.Predicaatsoort.NUMERIEK_MET_EXACT)
                && p.operand() instanceof Expr.AttribuutRef pa) {
            if (pa.objecttype() != null && Namen.bevatNaam(pa.objecttype().granulariteitLeden(), pa.attribuut())) return null;  // temporal -> Phase F
            if (pa.objecttype() != null && !pa.objecttype().dimensiesVan(pa.attribuut()).isEmpty()) return null;            // §3.6 gedimensioneerd -> marker
            boolean elf = p.soort() == Conditie.Predicaatsoort.ELFPROEF;
            int n = p.cijfers();
            boolean wanted = !p.negatie();
            String attr = pa.attribuut();
            if (ongenavigeerd(pa)) {
                if (subject == null || !subject.heeftLid(attr)) return null;   // not the subject's own member
                return s -> (elf ? RegelSpraakRuntime.rsElfproef(RegelSpraakRuntime.get(s, attr))
                                 : RegelSpraakRuntime.rsNumeriekMetExact(RegelSpraakRuntime.get(s, attr), n)) == wanted;
            }
            String rol = navComparisonRole(pa, subject);
            if (rol == null) return null;
            return s -> {
                Object rel = RegelSpraakRuntime.get(s, rol);
                if (rel == null) return false;   // mvel `rol!.attr` short-circuits the WHOLE constraint to non-match on a
                                                 // null reference -> onwaar for BOTH polarities (the navigated null house rule)
                return (elf ? RegelSpraakRuntime.rsElfproef(RegelSpraakRuntime.get(rel, attr))
                            : RegelSpraakRuntime.rsNumeriekMetExact(RegelSpraakRuntime.get(rel, attr), n)) == wanted;
            };
        }
        // §8.1.1 an own-attribute scalar comparison vs a numeric literal.
        if (c instanceof Conditie.Vergelijking v && v.links() instanceof Expr.AttribuutRef la && ongenavigeerd(la)
                && v.rechts() instanceof Expr.Literal lit && lit.type().isNumeriek()) {
            if (subject == null || !subject.heeftLid(la.attribuut())) return null;   // not the subject's own member (feitcreatie anker-first probe)
            if (Namen.bevatNaam(subject.granulariteitLeden(), la.attribuut())) return null;  // temporal -> Phase F
            if (!subject.dimensiesVan(la.attribuut()).isEmpty()) return null;   // §3.6 gedimensioneerd (a Map) -> marker
            if (!isNumeriekOp(v.op())) return null;   // EERDER/LATER (datum) -> Phase later
            BigDecimal rhs = bd(lit);
            if (rhs == null) return null;
            String attr = la.attribuut();
            Conditie.Operator op = v.op();
            BigDecimal[] f = restatement(effType(la), effType(lit));   // §5.1 Tabel 32 unit restatement (e.g. mnd vs jr)
            BigDecimal rhsScaled = scale(rhs, f[1]);
            return s -> vergelijk(scale(num(s, attr), f[0]), rhsScaled, op);
        }
        // §8.1.1 a to-ONE NAVIGATED numeric comparison vs a numeric literal ("de duur van zijn partner groter is dan
        // 60"): read the navigated value null-safely (mvel's `partner!.x`), restate units, and vergelijk. navReader
        // resolves the to-one role and root-guards a null relation (-> leeg -> ordering no-match / GELIJK-both-leeg,
        // matching mvel `null op x`); a to-many / unresolvable navigation -> null (skip). Mirrors DRL `partner!.x op lit`.
        if (c instanceof Conditie.Vergelijking v && v.links() instanceof Expr.AttribuutRef la && !ongenavigeerd(la)
                && la.type().isNumeriek() && isNumeriekOp(v.op())
                && v.rechts() instanceof Expr.Literal lit && lit.type().isNumeriek()) {
            if (la.objecttype() == null || Namen.bevatNaam(la.objecttype().granulariteitLeden(), la.attribuut())) return null;  // temporal -> Phase F
            if (!la.objecttype().dimensiesVan(la.attribuut()).isEmpty()) return null;                                       // §3.6 gedimensioneerd (Map) -> marker
            String rol = navComparisonRole(la, subject);
            if (rol == null) return null;
            BigDecimal rhs = bd(lit);
            if (rhs == null) return null;
            String attr = la.attribuut();
            Conditie.Operator op = v.op();
            BigDecimal[] f = restatement(effType(la), effType(lit));
            BigDecimal rhsScaled = scale(rhs, f[1]);
            return s -> {
                Object rel = RegelSpraakRuntime.get(s, rol);
                // §5.5.5 a null to-one relation reads LEEG (like a present relation with a null attribute), then vergelijk's
                // leegwaarde semantics decide (Tabel 33-44): ordering/GELIJK of a leeg operand -> onwaar, but ONGELIJK ->
                // WAAR (Tabel 44, one-leeg). NOT a blanket short-circuit (which would make a leeg-vs-X ONGELIJK wrongly false).
                Object attrVal = rel == null ? null : RegelSpraakRuntime.get(rel, attr);
                return vergelijk(scale(RegelSpraakRuntime.toBd(attrVal), f[0]), rhsScaled, op);
            };
        }
        // §8.1.1 a to-ONE NAVIGATED numeric comparison vs an OWN attribute ("de duur van zijn partner groter is dan
        // zijn drempel"): the navigated LHS read null-safely (a null relation short-circuits the whole term), the own
        // RHS fact-local. Mirrors DRL `rol!.x op ownAttr`.
        if (c instanceof Conditie.Vergelijking v && v.links() instanceof Expr.AttribuutRef la && !ongenavigeerd(la)
                && la.type().isNumeriek() && isNumeriekOp(v.op())
                && v.rechts() instanceof Expr.AttribuutRef ra && ongenavigeerd(ra) && ra.type().isNumeriek()) {
            if (la.objecttype() == null || Namen.bevatNaam(la.objecttype().granulariteitLeden(), la.attribuut())) return null;  // temporal -> Phase F
            if (!la.objecttype().dimensiesVan(la.attribuut()).isEmpty()) return null;                                       // §3.6 -> marker
            if (subject == null || !subject.heeftLid(ra.attribuut())) return null;                                          // own RHS member
            if (Namen.bevatNaam(subject.granulariteitLeden(), ra.attribuut())) return null;                                    // temporal RHS -> Phase F
            if (!subject.dimensiesVan(ra.attribuut()).isEmpty()) return null;                                               // §3.6 RHS -> marker
            String rol = navComparisonRole(la, subject);
            if (rol == null) return null;
            String lattr = la.attribuut(), rattr = ra.attribuut();
            Conditie.Operator op = v.op();
            BigDecimal[] f = restatement(effType(la), effType(ra));
            return s -> {
                Object rel = RegelSpraakRuntime.get(s, rol);
                Object lval = rel == null ? null : RegelSpraakRuntime.get(rel, lattr);   // §5.5.5 null relation -> leeg -> vergelijk (Tabel 44)
                return vergelijk(scale(RegelSpraakRuntime.toBd(lval), f[0]), scale(num(s, rattr), f[1]), op);
            };
        }
        // §8.1.1 an OWN-attribute numeric comparison vs ANOTHER own attribute ("de prijs groter is dan de drempel"):
        // both fact-local reads, unit-restated, vergelijk (mvel's null semantics — ordering no-match on a leeg
        // operand, GELIJK true only both-leeg). Mirrors DRL's `attrA op attrB`. (vs-literal handled above; a global
        // or navigated operand lives in voorwaarde / the navigated branch.)
        if (c instanceof Conditie.Vergelijking v && v.links() instanceof Expr.AttribuutRef la && ongenavigeerd(la)
                && la.type().isNumeriek() && isNumeriekOp(v.op())
                && v.rechts() instanceof Expr.AttribuutRef ra && ongenavigeerd(ra) && ra.type().isNumeriek()) {
            if (subject == null || !subject.heeftLid(la.attribuut()) || !subject.heeftLid(ra.attribuut())) return null;
            if (Namen.bevatNaam(subject.granulariteitLeden(), la.attribuut())
                    || Namen.bevatNaam(subject.granulariteitLeden(), ra.attribuut())) return null;          // temporal -> Phase F
            if (!subject.dimensiesVan(la.attribuut()).isEmpty() || !subject.dimensiesVan(ra.attribuut()).isEmpty()) return null;  // §3.6 -> marker
            String lattr = la.attribuut(), rattr = ra.attribuut();
            Conditie.Operator op = v.op();
            BigDecimal[] f = restatement(effType(la), effType(ra));
            return s -> vergelijk(scale(num(s, lattr), f[0]), scale(num(s, rattr), f[1]), op);
        }
        // §8.1.1 a DATUM ordering comparison (eerder/later/eerder-of-gelijk/later-of-gelijk) of an OWN or to-one
        // NAVIGATED datum attribute vs a §13.2.19/.21 datum literal OR another same-kind own/navigated datum attribute.
        // DRL lowers `attr < LocalDate.of(...)` / `attr < otherAttr` (Comparable); an absent datum / absent navigation
        // reads leegwaarde -> ordering no-match (datumVergelijkTemporal: a null operand is false), matching mvel. The
        // RHS must be a Literal or AttribuutRef (a §6/§13.4.16 date-ARITHMETIC Binair operand is the branch below).
        if (c instanceof Conditie.Vergelijking v && v.links() instanceof Expr.AttribuutRef la && la.type().isDatum()
                && isDatumOp(v.op()) && (v.rechts() instanceof Expr.Literal || v.rechts() instanceof Expr.AttribuutRef)
                && v.rechts().type() != null && v.rechts().type().isDatum()) {
            java.util.function.Function<Object, Object> lf = datumTemporeelLezer(la, subject);
            if (lf == null) return null;
            Conditie.Operator op = v.op();
            // a present Datum-en-tijd (LocalDateTime) compares via compareTo, not `instanceof LocalDate` (which read it as
            // leeg -> a wrong no-fire). A mixed Datum-in-dagen/Datum-en-tijd pair is a §5.3 Tabel 36 mismatch -> skip.
            if (v.rechts() instanceof Expr.Literal lit) {
                if (!sameDatumKind(la.type(), lit.type())) return null;
                Object rhs = datumLiteraal(lit);
                if (rhs == null) return null;
                return s -> datumVergelijkTemporal(lf.apply(s), rhs, op);
            }
            Expr.AttribuutRef ra = (Expr.AttribuutRef) v.rechts();
            if (!sameDatumKind(la.type(), ra.type())) return null;
            java.util.function.Function<Object, Object> rf = datumTemporeelLezer(ra, subject);
            if (rf == null) return null;
            return s -> datumVergelijkTemporal(lf.apply(s), rf.apply(s), op);
        }
        // §8.1.1 a DATUM ordering comparison where an operand is a §6/§13.4.16 date-arithmetic EXPRESSION ("de
        // daadwerkelijke datum later is dan de datum plus de DUURPARAMETER"). The simple attr-vs-literal form
        // is handled above; here at least one side is a date-arith Binair, so BOTH sides lower via scalarWaarde
        // (-> a Temporal) and compare via datumVergelijkTemporal. A fact-only reader (0 globals). DRL lowers the
        // same expression as the constraint operand (`attr > ((LocalDate) rsDatumReken(...))`).
        if (c instanceof Conditie.Vergelijking v && isDatumVergelijkOp(v.op())
                && v.links().type().isDatum() && v.rechts().type().isDatum()
                && (isDatumRekenExpr(v.links()) || isDatumRekenExpr(v.rechts()))) {
            WaardeFn lw = scalarWaarde(null, v.links()), rw = scalarWaarde(null, v.rechts());
            if (lw == null || rw == null || !lw.globals().isEmpty() || !rw.globals().isEmpty()) return null;
            BiFunction<Object, Map<String, BigDecimal>, Object> lfn = lw.fn(), rfn = rw.fn();
            Conditie.Operator op = v.op();
            return s -> datumVergelijkTemporal(lfn.apply(s, Map.of()), rfn.apply(s, Map.of()), op);
        }
        // §5.3/§5.6 a DATUM EQUALITY (gelijk/ongelijk) of an OWN or to-one NAVIGATED datum attribute vs a §13.2.19 datum
        // LITERAL or another own/navigated datum attribute. An absent navigation / leeg datum reads LEEGWAARDE (§5.5.5),
        // then the §5 leegwaarde tables decide (datumVergelijkTemporal): GELIJK both-leeg -> fout/onwaar (Tabel 38, NON-numeric — NOT
        // Tabel 37's WAAR) and one-leeg -> onwaar; ONGELIJK one-leeg -> WAAR (Tabel 44) and both-leeg -> onwaar. Mirrors
        // DrlTranspiler's genavigeerdeVergelijkingVoorwaarde (navigated) / non-numeriek schending complement (own). The
        // date-ARITHMETIC equality (an operand is a §6 Binair) is handled by the branch above (isDatumRekenExpr).
        if (c instanceof Conditie.Vergelijking v && (v.op() == Conditie.Operator.GELIJK || v.op() == Conditie.Operator.ONGELIJK)
                && v.links() instanceof Expr.AttribuutRef la && la.type() != null && la.type().isDatum()) {
            java.util.function.Function<Object, Object> lf = datumTemporeelLezer(la, subject);
            if (lf == null) return null;
            Conditie.Operator op = v.op();
            // a mixed Datum-in-dagen / Datum-en-tijd pair is a §5.3 Tabel 36 TYPE MISMATCH (left/right must be the SAME
            // datatype) -> skip (no lossy date-part coercion). Same-kind: read each as its Temporal and apply the leegwaarde
            // tables (datumVergelijkTemporal: GELIJK Tabel 38 one/both-leeg onwaar, ONGELIJK Tabel 44 one-leeg WAAR / both
            // onwaar). Now FIRES Datum-en-tijd equality too (was a LocalDate-only honest skip). DRL fires the same shapes.
            if (v.rechts() instanceof Expr.Literal lit && lit.type().isDatum()) {
                if (!sameDatumKind(la.type(), lit.type())) return null;
                Object rhs = datumLiteraal(lit);
                if (rhs == null) return null;
                return s -> datumVergelijkTemporal(lf.apply(s), rhs, op);
            }
            if (v.rechts() instanceof Expr.AttribuutRef ra && ra.type() != null && ra.type().isDatum()) {
                if (!sameDatumKind(la.type(), ra.type())) return null;
                java.util.function.Function<Object, Object> rf = datumTemporeelLezer(ra, subject);
                if (rf == null) return null;
                return s -> datumVergelijkTemporal(lf.apply(s), rf.apply(s), op);
            }
            // §5.3 the RHS is a datum-typed EXPRESSION (a §13.4 builtin like "de eerste paasdag van (...)", a date-arith
            // Binair, a date-valued function) rather than a literal/attribute. Lower it via scalarWaarde (which covers
            // EERSTE_PAASDAG et al.); passing `subject` lets any navigation inside the RHS resolve. A fact-only leaf
            // (0 globals) — a global-reading RHS falls through to skip, symmetric with the frontier. DRL lowers the same
            // `attr == <expr>` via its operand path (the builtin lowers to rsEerstePaasdag(...)).
            if (v.rechts().type() != null && v.rechts().type().isDatum() && sameDatumKind(la.type(), v.rechts().type())) {
                WaardeFn rw = scalarWaarde(subject, v.rechts());
                if (rw != null && rw.globals().isEmpty()) {
                    BiFunction<Object, Map<String, BigDecimal>, Object> rfn = rw.fn();
                    return s -> datumVergelijkTemporal(lf.apply(s), rfn.apply(s, Map.of()), op);
                }
            }
            return null;   // datum-vs-(global/other) -> not a fact-only datum-equality leaf
        }
        // §8.1.1 a comparison whose LHS is a NUMERIC EXPRESSION — a builtin Functie ("de maand uit (...)", abs), an
        // arithmetic Binair ("de prijs plus 10"), a §6.1.4 begrenzing or a §13.4.17.25 afronding — vs a numeric
        // literal. Lower the LHS via scalarWaarde (which now covers these); a fact-only reader (0 globals) makes it a
        // Predicate1, then vergelijk + restatement. DRL lowers the same expression as a constraint operand. A NAVIGATED
        // operand inside the LHS ("de waarde van zijn partner plus 1 > 100") lowers via scalarWaarde's navigated read —
        // the subject passed as the value-path parameter; an ABSENT relation reads leeg -> leeg-as-0 in the arithmetic
        // (see the expr-vs-expr branch below for the rsRelGetal parity). A param-reading LHS keeps globals non-empty ->
        // skipped (the global path binds it in voorwaarde).
        if (c instanceof Conditie.Vergelijking v && isNumeriekOp(v.op()) && getalachtigVergelijkbaar(v.links())
                && (v.links() instanceof Expr.Functie || v.links() instanceof Expr.Binair
                    || v.links() instanceof Expr.Begrensd || v.links() instanceof Expr.Afgerond)
                && v.rechts() instanceof Expr.Literal lit && lit.type().isNumeriek()) {
            if (parityOnveilig(v.links())) return null;   // §3.6 dimensiecel / cross-unit +/- -> DRL markers -> skip (symmetric)
            WaardeFn lw = scalarWaarde(subject, v.links());   // the value-path subject rides as a parameter (no-mutable-build-state)
            if (lw == null || !lw.globals().isEmpty()) return null;   // unlowerable / unresolvable navigation / needs a global -> skip
            BigDecimal[] f = restatement(effType(v.links()), effType(lit));
            BiFunction<Object, Map<String, BigDecimal>, Object> lfn = lw.fn();
            Conditie.Operator op = v.op();
            // §389/§486: a Breuk-capable left operand (a non-terminating division/percentage or a rational literal)
            // compares EXACTLY via rsAggVergelijk (single-runtime; a (BigDecimal) cast would throw on a Breuk and
            // DECIMAL64 would decide "basis maal 1/3 == 1" wrong). Its RHS is the EXACT literal (numeriekLit ->
            // Breuk when non-terminating). No unit restatement applies to an arithmetic comparison here.
            if (kanBreukOpleveren(v.links())) {
                Object rhsExact = numeriekLit(lit);
                String opStr = Operatoren.symbool(op);
                return s -> RegelSpraakRuntime.rsAggVergelijk(lfn.apply(s, Map.of()), rhsExact, opStr);
            }
            BigDecimal rhs = bd(lit);
            if (rhs == null) return null;
            BigDecimal rhsScaled = scale(rhs, f[1]);
            return s -> vergelijk(scale((BigDecimal) lfn.apply(s, Map.of()), f[0]), rhsScaled, op);
        }
        // §8.1.1 a comparison where at least one side is a NUMERIC EXPRESSION (arithmetic Binair / builtin Functie /
        // begrenzing / afronding) and the OTHER side is a numeric attribute OR also an expression — NOT a literal (the
        // branch above). Lower BOTH sides via scalarWaarde (the value-path lowering, at parity with DrlTranspiler.binairTerm
        // on the value side), then vergelijk + §5.1 restatement. parityOnveilig skips a §3.6 dimensiecel and a cross-unit
        // +/- (DRL markers those). A NAVIGATED operand inside the expr ("de prijs van zijn partner plus 1 groter is dan zijn
        // drempel") lowers via scalarWaarde's navigated read — the subject rides as the value-path parameter (like the
        // §8.2 grens path) so navReader resolves. An ABSENT relation reads LEEG, which the arithmetic treats per the §4/§6
        // leegwaarde tables (leeg-as-0 in +/-/*, §4.1 Tabel 3 etc.) and then vergelijk's leegwaarde semantics — NOT a
        // short-circuit-to-false (DRL's mvel `rol!.attr` short-circuit was a latent bug; DrlTranspiler now reads a navigated
        // arithmetic leg null-safely via rsRelGetal, so both back-ends compute leeg-as-0). A GLOBAL operand keeps globals
        // non-empty -> skip (the expr-vs-global path binds at most one global, in voorwaarde); an unresolvable / to-many
        // navigation -> scalarWaarde null -> skip. Closes the masked DRL-fires/exec-skips for "de a plus de b is gelijk aan
        // de c" / "de prijs van zijn partner plus 1 groter is dan zijn drempel".
        if (c instanceof Conditie.Vergelijking v && isNumeriekOp(v.op())
                && getalachtigVergelijkbaar(v.links())
                && getalachtigVergelijkbaar(v.rechts())
                && (numeriekeExpressie(v.links()) || numeriekeExpressie(v.rechts()))) {
            if (parityOnveilig(v.links()) || parityOnveilig(v.rechts())) return null;   // §3.6 dimensiecel / cross-unit +/- -> DRL markers -> skip (symmetric)
            WaardeFn lw = scalarWaarde(subject, v.links()), rw = scalarWaarde(subject, v.rechts());
            if (lw == null || rw == null || !lw.globals().isEmpty() || !rw.globals().isEmpty()) return null;   // unlowerable / unresolvable navigation / needs a global -> skip
            BigDecimal[] f = restatement(effType(v.links()), effType(v.rechts()));
            BiFunction<Object, Map<String, BigDecimal>, Object> lfn = lw.fn(), rfn = rw.fn();
            Conditie.Operator op = v.op();
            // §389/§486: when an operand can carry an exact Breuk (a non-terminating division/percentage or a
            // rational literal), compare EXACTLY via the Breuk-aware rsAggVergelijk (single-runtime, shared with the
            // DRL back-end) — a (BigDecimal) cast would throw on a Breuk, and DECIMAL64 would decide the comparison
            // wrong (1/3·3 == 1). No unit restatement applies here (a cross-unit +/- is parityOnveilig-skipped), so
            // pass the raw operand values. The ordinary numeric comparison keeps the exact BigDecimal path.
            if (kanBreukOpleveren(v.links()) || kanBreukOpleveren(v.rechts())) {
                String opStr = Operatoren.symbool(op);
                return s -> RegelSpraakRuntime.rsAggVergelijk(lfn.apply(s, Map.of()), rfn.apply(s, Map.of()), opStr);
            }
            return s -> vergelijk(scale((BigDecimal) lfn.apply(s, Map.of()), f[0]), scale((BigDecimal) rfn.apply(s, Map.of()), f[1]), op);
        }
        // §5.3 an own-attribute equality vs a §13.3.3.4 Tekst literal OR a §13.2.14 enumeratiewaarde 'X' (the §12
        // beslistabel "is gelijk aan X" cell, and the §12 OR-list of them). An enumeratie compares by VALUE equality
        // on its label, so it lowers exactly like a Tekst. mvel string '==' is VALUE equality (.equals); ONGELIJK is
        // null-asymmetric (mvel reads `null != "x"` as TRUE) — mirror DrlTranspiler's `prop == "lit"`.
        if (c instanceof Conditie.Vergelijking v && v.links() instanceof Expr.AttribuutRef la && ongenavigeerd(la)
                && v.rechts() instanceof Expr.Literal lit && Literalen.tekstOfEnumLabel(lit) != null
                && (v.op() == Conditie.Operator.GELIJK || v.op() == Conditie.Operator.ONGELIJK)) {
            if (subject == null || !subject.heeftLid(la.attribuut())) return null;            // not the subject's own member
            if (Namen.bevatNaam(subject.granulariteitLeden(), la.attribuut())) return null;      // temporal -> Phase F
            if (!subject.dimensiesVan(la.attribuut()).isEmpty()) return null;                 // §3.6 gedimensioneerd -> marker
            String attr = la.attribuut();
            java.util.function.UnaryOperator<Object> n = gelijkheidsLezing(la.type(), lit.type());
            Object rhs = n.apply(Literalen.tekstOfEnumLabel(lit));   // the SHARED reader: Tekst verbatim, enum labels unwrapped
            boolean gelijk = v.op() == Conditie.Operator.GELIJK;
            return s -> rhs.equals(n.apply(RegelSpraakRuntime.get(s, attr))) == gelijk;   // attr may be null; rhs non-null
        }
        // §5.3 the to-ONE NAVIGATED Tekst/enumeratiewaarde equality ("de woonplaats van zijn partner is gelijk aan
        // 'X'"): §5.5.5 a null relation reads LEEG (the SAME as a present relation with a null attribute), then string
        // equality decides — GELIJK of leeg vs the literal -> onwaar, ONGELIJK of leeg vs the literal -> WAAR (Tabel 44).
        // NOT a blanket short-circuit. Mirrors the navigated numeric comparison's null-relation handling.
        if (c instanceof Conditie.Vergelijking v && v.links() instanceof Expr.AttribuutRef la && !ongenavigeerd(la)
                && v.rechts() instanceof Expr.Literal lit && Literalen.tekstOfEnumLabel(lit) != null
                && (v.op() == Conditie.Operator.GELIJK || v.op() == Conditie.Operator.ONGELIJK)) {
            if (la.objecttype() == null || Namen.bevatNaam(la.objecttype().granulariteitLeden(), la.attribuut())) return null;  // temporal -> Phase F
            if (!la.objecttype().dimensiesVan(la.attribuut()).isEmpty()) return null;                                       // §3.6 gedimensioneerd -> marker
            String rol = navComparisonRole(la, subject);
            if (rol == null) return null;
            String attr = la.attribuut();
            java.util.function.UnaryOperator<Object> n = gelijkheidsLezing(la.type(), lit.type());
            Object rhs = n.apply(Literalen.tekstOfEnumLabel(lit));
            boolean gelijk = v.op() == Conditie.Operator.GELIJK;
            return s -> {
                Object rel = RegelSpraakRuntime.get(s, rol);
                Object val = rel == null ? null : RegelSpraakRuntime.get(rel, attr);   // §5.5.5 null relation -> leeg
                return rhs.equals(n.apply(val)) == gelijk;   // leeg != literal -> ONGELIJK WAAR (Tabel 44); leeg == literal -> GELIJK onwaar
            };
        }
        // §5.3 / typeringen Tabel 38 (GELIJK) + Tabel 44 (ONGELIJK): a NON-numeriek (Tekst / §13.2.14 enumeratiewaarde /
        // Booleaan) equality where BOTH operands are the subject's OWN attributes (attr-vs-attr). Mirrors the vs-literal
        // branches above but with the RHS read from the second own attribute; the leeg/gelijk truth table (tekstAttrGelijk)
        // is derived from the SAME DRL mvel the vs-literal `rhs.equals(prop) == gelijk` form uses, extended for a leeg RHS
        // (DrlTranspiler L2958-2966: both-leeg GELIJK -> onwaar, one-leeg ONGELIJK -> WAAR). Excludes DATUM (its own
        // ordering/equality lower elsewhere) + numeriek (the arithmetic-comparison branch above). A navigated operand on
        // either side is NOT handled here (own-vs-own suffices for parity; a navigated attr-vs-attr falls through -> skip).
        if (c instanceof Conditie.Vergelijking v && v.links() instanceof Expr.AttribuutRef la && ongenavigeerd(la)
                && v.rechts() instanceof Expr.AttribuutRef ra && ongenavigeerd(ra)
                && (v.op() == Conditie.Operator.GELIJK || v.op() == Conditie.Operator.ONGELIJK)
                && la.type() != null && !la.type().isNumeriek() && !la.type().isPercentage() && !la.type().isDatum()
                && ra.type() != null && !ra.type().isNumeriek() && !ra.type().isPercentage() && !ra.type().isDatum()) {
            if (subject == null || !subject.heeftLid(la.attribuut()) || !subject.heeftLid(ra.attribuut())) return null;   // both must be own members
            if (Namen.bevatNaam(subject.granulariteitLeden(), la.attribuut())
                    || Namen.bevatNaam(subject.granulariteitLeden(), ra.attribuut())) return null;                          // temporal -> Phase F
            if (!subject.dimensiesVan(la.attribuut()).isEmpty() || !subject.dimensiesVan(ra.attribuut()).isEmpty()) return null;   // §3.6 gedimensioneerd -> marker
            String lAttr = la.attribuut(), rAttr = ra.attribuut();
            boolean gelijk = v.op() == Conditie.Operator.GELIJK;
            java.util.function.UnaryOperator<Object> n = gelijkheidsLezing(la.type(), ra.type());
            return s -> tekstAttrGelijk(n.apply(RegelSpraakRuntime.get(s, lAttr)), n.apply(RegelSpraakRuntime.get(s, rAttr)), gelijk);
        }
        // §5.3 an own-attribute BOOLEAN equality vs a §13.2.14 boolean literal ("<attr> is gelijk aan waar|onwaar").
        // Mirrors the own Tekst/enum branch above but for a Booleaan attribute: read the literal's boolean value with
        // Literalen.waar, then compare `rhs.equals(prop) == gelijk`. A null Boolean prop reads leeg -> rhs.equals(null)
        // is false, so GELIJK -> onwaar / ONGELIJK -> WAAR (Tabel 44), matching DrlTranspiler's mvel `prop == true|false`.
        if (c instanceof Conditie.Vergelijking v && v.links() instanceof Expr.AttribuutRef la && ongenavigeerd(la)
                && v.rechts() instanceof Expr.Literal lit && lit.type() instanceof io.github.yhavinga.regelspraak.model.Datatype.Booleaan
                && (v.op() == Conditie.Operator.GELIJK || v.op() == Conditie.Operator.ONGELIJK)) {
            if (subject == null || !subject.heeftLid(la.attribuut())) return null;            // not the subject's own member
            if (Namen.bevatNaam(subject.granulariteitLeden(), la.attribuut())) return null;      // temporal -> Phase F
            if (!subject.dimensiesVan(la.attribuut()).isEmpty()) return null;                 // §3.6 gedimensioneerd -> marker
            String attr = la.attribuut();
            Boolean rhs = io.github.yhavinga.regelspraak.kern.Literalen.waar(lit.waarde());
            boolean gelijk = v.op() == Conditie.Operator.GELIJK;
            return s -> rhs.equals(RegelSpraakRuntime.get(s, attr)) == gelijk;   // prop may be null; rhs non-null
        }
        // §5.3 the to-ONE NAVIGATED BOOLEAN equality ("<attr> van zijn rol is gelijk aan waar|onwaar"): §5.5.5 a null
        // relation reads LEEG (same as a present relation with a null Boolean attribute), then boolean equality decides —
        // GELIJK of leeg vs the literal -> onwaar, ONGELIJK of leeg vs the literal -> WAAR (Tabel 44). Mirrors the
        // navigated Tekst/enum branch above.
        if (c instanceof Conditie.Vergelijking v && v.links() instanceof Expr.AttribuutRef la && !ongenavigeerd(la)
                && v.rechts() instanceof Expr.Literal lit && lit.type() instanceof io.github.yhavinga.regelspraak.model.Datatype.Booleaan
                && (v.op() == Conditie.Operator.GELIJK || v.op() == Conditie.Operator.ONGELIJK)) {
            if (la.objecttype() == null || Namen.bevatNaam(la.objecttype().granulariteitLeden(), la.attribuut())) return null;  // temporal -> Phase F
            if (!la.objecttype().dimensiesVan(la.attribuut()).isEmpty()) return null;                                       // §3.6 gedimensioneerd -> marker
            String rol = navComparisonRole(la, subject);
            if (rol == null) return null;
            String attr = la.attribuut();
            Boolean rhs = io.github.yhavinga.regelspraak.kern.Literalen.waar(lit.waarde());
            boolean gelijk = v.op() == Conditie.Operator.GELIJK;
            return s -> {
                Object rel = RegelSpraakRuntime.get(s, rol);
                Object val = rel == null ? null : RegelSpraakRuntime.get(rel, attr);   // §5.5.5 null relation -> leeg
                return rhs.equals(val) == gelijk;   // leeg != literal -> ONGELIJK WAAR (Tabel 44); leeg == literal -> GELIJK onwaar
            };
        }
        // §8.1.5 dagsoortcontrole "<datum> is een <dagsoort>": test an OWN or to-one NAVIGATED datum attribute
        // against the dagsoort's §9.8 definitie predicate (the mirror of DrlTranspiler's `attr != null &&
        // rsDagsoort_<Naam>.test(attr)`). §5.15: een leegwaarde is nooit een dagsoort -> an absent datum is ONWAAR.
        if (c instanceof Conditie.Predicaat p && p.soort() == Conditie.Predicaatsoort.DAGSOORT
                && p.operand() instanceof Expr.AttribuutRef la && la.type().isDatum()) {
            if (la.objecttype() == null || Namen.bevatNaam(la.objecttype().granulariteitLeden(), la.attribuut())) return null;  // temporal -> skip
            java.util.function.Predicate<java.time.LocalDate> pred = dagsoortPredikaat(p.dagsoort());
            if (pred == null) return null;
            String attr = la.attribuut();
            boolean neg = p.negatie();   // §8.1.5 + Tabel 16 "is geen <dagsoort>": a leeg/non-dagsoort datum is WAAR
            if (ongenavigeerd(la)) {
                if (!subject.heeftLid(attr)) return null;
                return s -> { java.time.LocalDate ld = asDag(RegelSpraakRuntime.get(s, attr)); return neg != (ld != null && pred.test(ld)); };
            }
            String rol = navComparisonRole(la, subject);
            if (rol == null) return null;
            return s -> {
                // §5.15 + Tabel 16: an absent relation (or a leeg datum) reads a leegwaarde, which is NEVER a dagsoort.
                // Under two-valued negation "is een <dagsoort>" -> ONWAAR (neg=false), "is geen <dagsoort>" -> WAAR
                // (neg=true). Mirrors DRL `(neg?"!":"") (partner!.x != null && rsDagsoort(partner!.x))` (the `!.` short-circuit).
                Object rel = RegelSpraakRuntime.get(s, rol);
                java.time.LocalDate ld = rel != null ? asDag(RegelSpraakRuntime.get(rel, attr)) : null;
                return neg != (ld != null && pred.test(ld));
            };
        }
        // §8.1.5 dagsoortcontrole over a NON-attribute datum operand (a §6 date-arithmetic expression "de datum
        // plus 1 dag is een kerstdag", a date-valued function) — lower it via scalarWaarde, mirroring DrlTranspiler's
        // generic operand path so the back-ends stay at parity. Fact-only (no globals); a global-reading operand is not
        // a fact-only leaf -> skip. §5.15: an absent/non-date value is ONWAAR.
        if (c instanceof Conditie.Predicaat p && p.soort() == Conditie.Predicaatsoort.DAGSOORT && p.operand().type().isDatum()) {
            java.util.function.Predicate<java.time.LocalDate> pred = dagsoortPredikaat(p.dagsoort());
            WaardeFn wf = scalarWaarde(null, p.operand());
            if (pred == null || wf == null || !wf.globals().isEmpty()) return null;
            BiFunction<Object, Map<String, BigDecimal>, Object> fn = wf.fn();
            boolean neg = p.negatie();   // §8.1.5 + Tabel 16 "is geen <dagsoort>"
            return s -> { java.time.LocalDate ld = asDag(fn.apply(s, Map.of())); return neg != (ld != null && pred.test(ld)); };
        }
        // §8.1.7 near-side rolcheck: the subject fills the role iff its to-ONE forward reference is set. The far-side
        // (the reference lives on the MANY side) is not a fact-only leaf -> null (handled as an exists-join above).
        if (c instanceof Conditie.Rollidmaatschap r) {
            String veld = subject != null ? model.verbindendeRol(subject, r.anderTeFiller()) : null;
            if (veld == null || model.naarVeelRol(subject, r.anderTeFiller(), veld)) return null;
            // ...and the field must be the REFERENCE, not a declared member that took its name. This reader does not
            // navigate — it null-TESTS the field — which is exactly why it was missed with the four navigated ones:
            // a displaced BigDecimal/boolean is non-null, so the check reads the attribute's VALUE as proof that the
            // subject fills the role. Measured 2026-09-10 on a verifier-clean, ref-clean model, both branches:
            // "FAIL verwacht leeg, werkelijk 8" — the rule fired where ref and clingo leave it unfired.
            if (!heeftVeld("rollidmaatschapscheck", subject, veld, r.anderTeFiller())) return null;
            boolean gevuldWanted = !r.negatie();
            return s -> (RegelSpraakRuntime.get(s, veld) != null) == gevuldWanted;
        }
        return null;
    }

    // ---------------- §9.8 dagsoort definitie -> a per-date Predicate<LocalDate> ----------------

    /** Compile (idempotently, cycle-safe) the §9.8 definitie of {@code naam} to a Predicate&lt;LocalDate&gt;; null
     *  when no such definitie exists or its condition is not a pure date predicate. A nested dagsoort reference
     *  resolves via a DEFERRED map lookup, so cross-references work regardless of compilation order and a
     *  (spec-forbidden, §9.10) cyclic definitie cannot infinite-loop — inside the shared kern.Dagsoorten (single-runtime principle). */
    /** §8.1.5/§5.15/Tabel 57: the day-granular date of a dagsoort operand — a Datum-in-dagen (LocalDate) directly, a
     *  Datum-en-tijd (LocalDateTime) via its date part (a dagsoort is day-granular, so the time portion is irrelevant).
     *  null for a leeg / non-date value (a leegwaarde is nooit een dagsoort). Mirrors the DRL `.toLocalDate()` wrap. */
    private static java.time.LocalDate asDag(Object o) {
        return o instanceof java.time.LocalDate d ? d
                : o instanceof java.time.LocalDateTime ldt ? ldt.toLocalDate() : null;
    }

    private java.util.function.Predicate<java.time.LocalDate> dagsoortPredikaat(String naam) {
        return dagsoorten.predikaat(naam);   // the SHARED §9.8 compiler (kern.Dagsoorten) — one implementation
    }

    /** A globally-unique constraint id (see {@link #idSeq}) — unique across the whole model, not just one rule. */
    private String nextId(String prefix) { return prefix + "_" + (idSeq++); }

    /** §4.2: true when ANY construct of ANY family carries a time-bounded (dated) version — the trigger for
     *  registering the Rekendatum global. Mirrors DrlTranspiler's `tijdgebonden` disjunction over every family. */
    private boolean heeftTijdgebonden() {
        java.util.function.Predicate<io.github.yhavinga.regelspraak.ir.Geldigheid> dated = g -> g != null && !g.altijd();
        return model.resolvedRegels.stream().anyMatch(r -> dated.test(r.geldigheid()))
                || model.objectCreaties.stream().anyMatch(o -> dated.test(o.geldigheid()))
                || model.verdelingen.stream().anyMatch(v -> dated.test(v.geldigheid()))
                || model.consistenties.stream().anyMatch(c -> dated.test(c.geldigheid()))
                || model.uniciteiten.stream().anyMatch(u -> dated.test(u.geldigheid()))
                || model.uniciteitUnies.stream().anyMatch(u -> dated.test(u.geldigheid()))
                || model.feitCreaties.stream().anyMatch(f -> dated.test(f.geldigheid()))
                // §8.4.3 a periodevergelijking-voorwaarde and §5.3 a Rekendatum/Rekenjaar value operand both
                // read the Rekendatum global. DRL sets a flag exactly where it emits the text; the exec model
                // must register the Global BEFORE rules build, so scan the IR surfaces (over-inclusion — a
                // shape that ends up skipped — merely declares an unused global, which is harmless).
                || model.resolvedRegels.stream().anyMatch(r -> leestRekendatum(r.voorwaarde()) || Expr.bevat(r.waarde(), Expr::isRekenConstante))
                || model.objectCreaties.stream().anyMatch(o -> leestRekendatum(o.voorwaarde())
                        || o.toekenningen().stream().anyMatch(t -> Expr.bevat(t.waarde(), Expr::isRekenConstante)))
                || model.feitCreaties.stream().anyMatch(f -> leestRekendatum(f.voorwaarde()))
                || model.verdelingen.stream().anyMatch(v -> leestRekendatum(v.voorwaarde()))
                || model.consistenties.stream().anyMatch(cs -> leestRekendatum(cs.assertie()));
    }

    /** Whether a condition contains a §8.4.3 periode-window or a §5.3 Rekendatum/Rekenjaar operand. */
    private static boolean leestRekendatum(Conditie c) {
        return c != null && (Conditie.bevatPeriode(c) || Conditie.bevatExpr(c, Expr::isRekenConstante));
    }

    /** §4.2 add the validity-window guard to a construct's pattern: an {@code .expr} over the Rekendatum global
     *  matching only when the rekendatum is inside the version's inclusive {@code [vanaf, tot]} window (an unset
     *  rekendatum never matches, so the dated rule simply does not fire) — the exec-model mirror of
     *  DrlTranspiler.geldigheidGuard ({@code eval( rekendatum != null && !rekendatum.isBefore(vanaf) &&
     *  !rekendatum.isAfter(tot) )}). A no-op (returns the pattern unchanged) for an {@code altijd} version. The
     *  predicate ignores the subject — it reads only the global — so it composes with any pattern (subject, anker,
     *  Pot source). The global is constant per session, so the guard introduces no extra reactivity. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private PatternDSL.PatternDef geldigGuard(PatternDSL.PatternDef pattern, io.github.yhavinga.regelspraak.ir.Geldigheid g) {
        if (g == null || g.altijd() || rekendatumGlobal == null) return pattern;
        java.time.LocalDate vanaf = g.vanaf(), tot = g.tot();
        return pattern.expr(nextId("geldig"), (Variable) rekendatumGlobal, (Predicate2) (s, rd) -> {
            if (!(rd instanceof java.time.LocalDate d)) return false;   // unset rekendatum -> no match (no NPE)
            if (vanaf != null && d.isBefore(vanaf)) return false;
            return tot == null || !d.isAfter(tot);
        });
    }

    /** A lowered scalar value: a {@code (subject, globalValuesByName) -> value} function plus the set of
     *  Parameter-global names it reads (so {@link #regel} can bind them on the consequence). */
    private record WaardeFn(BiFunction<Object, Map<String, BigDecimal>, Object> fn, Set<String> globals) {
        static WaardeFn constant(Object v) { return new WaardeFn((s, gm) -> v, Set.of()); }
        /** A copy whose NUMERIC result is multiplied by a unit factor (null stays null — a leegwaarde; a non-number,
         *  e.g. a Tekst/enumeratiewaarde String value, passes through unscaled). */
        WaardeFn scaledBy(BigDecimal f) {
            BiFunction<Object, Map<String, BigDecimal>, Object> inner = fn;
            return new WaardeFn((s, gm) -> { Object v = inner.apply(s, gm); return v instanceof BigDecimal bd ? bd.multiply(f) : v; }, globals);
        }
        /** A copy whose NUMERIC result is restated from unit {@code van} into {@code naar} via the §4 EXACT-or-lossy
         *  {@link #herschaalWaarde} (a non-number passes through; a leeg stays leeg). Self-noops when no conversion is due. */
        WaardeFn herschaald(Datatype van, Datatype naar, String plek) {
            // Asked HERE, where the pair is known at build time, so this site's fault is raised while the KieBase is
            // assembled rather than from the lambda below, which reaches herschaalWaarde only at fire.
            Eenheidsconflict.eisHerschaalbaar(van.eenheidOf(), naar.eenheidOf(), plek);
            BiFunction<Object, Map<String, BigDecimal>, Object> inner = fn;
            return new WaardeFn((s, gm) -> { Object v = inner.apply(s, gm);
                return (v instanceof BigDecimal || v instanceof io.github.yhavinga.regelspraak.runtime.Breuk) ? herschaalWaarde(v, van, naar) : v; }, globals);
        }
    }

    /** Scalar value: Literal | own-attr read | Parameter-global read | a {@code +} of those (rsNul leegwaarde).
     *  null -> skip. The global read pulls its value from the consequence-bound map (see {@link #regel}). */
    private WaardeFn scalarWaarde(Objecttype valueSubject, Expr e) {
        // IR-SPEC ch. 4.3 / WF-110 (a): the §5.7 instance-set union denotes INSTANCES and is admissible as the
        // single argument of a count alone — a count consumes it in aggregatie() and never reaches here. One
        // that DOES reach here stands where a VALUE is written, which is ill-formed input. Named through
        // navReden rather than left to the generic "waarde buiten scope", for the reason the DRL-text branch
        // was given the same arm on 2026-09-22: the two branches must say ONE thing about one limit, and a
        // producer reading the gate report should not have to know which branch answered.
        if (e instanceof Expr.InstanceSetUnion) {
            navReden = "instantie-unie op een waardeplek: zij duidt de INSTANTIES van een §5.7-concatenatie aan"
                    + " en alleen een instantietelling verbruikt ze (WF-110 (a))";
            return null;
        }
        // IR-SPEC ch. 4.10: the Let cluster — all three union nodes.
        if (isLetKnoop(e)) return letWaarde(valueSubject, e);
        if (e instanceof Expr.Literal lit && lit.type().isNumeriek()) {
            // §13.2.5/§389: a non-terminating rational literal (1/3) becomes an EXACT Breuk so downstream +,-,×,÷
            // stay exact (9 maal 1/3 = 3); a terminating one (4/2, 3/2) stays a plain BigDecimal (fast path).
            Object val = numeriekLit(lit);
            return val == null ? null : WaardeFn.constant(val);
        }
        // §13.2.16 waar|onwaar literal as a VALUE (the §13.4.6.4 kenmerkwaardetoekenning RHS, or a Booleaan
        // attribute init) -> its Boolean constant; DRL renders the same true/false.
        if (e instanceof Expr.Literal bl && bl.type() instanceof Datatype.Booleaan)
            return WaardeFn.constant(Literalen.waar(bl.waarde()));
        // §5.3 Rekendatum/Rekenjaar as a VALUE operand (§13.4.17.34): the §4.2 rekendatum global, bound into the
        // globals map under its own name (the map's values are BigDecimal-typed but ERASED; only this reader
        // touches the "rekendatum" slot and it instanceof-guards, so the LocalDate rides through safely — the
        // binder side uses the raw-put vulGlobalMap / cast-free bindGlobal). An unset rekendatum reads leeg.
        // DRL lowers the same shapes as the free global name / rsRekenjaar(rekendatum).
        if (e instanceof Expr.Constante rc && Expr.isRekenConstante(rc)) {
            boolean jaar = "Rekenjaar".equals(rc.naam());
            return new WaardeFn((s, gm) -> {
                Object rd = ((Map<?, ?>) gm).get("rekendatum");
                if (!(rd instanceof java.time.LocalDate d)) return null;
                return jaar ? RegelSpraakRuntime.rsRekenjaar(d) : d;
            }, java.util.Set.of("rekendatum"));
        }
        if (e instanceof Expr.Literal dl && dl.type() instanceof Datatype.DatumInDagen) {
            // §13.2.19 a date literal as a VALUE (e.g. a datum-arithmetic operand) -> its LocalDate constant; DRL
            // lowers the same LocalDate.of(...).
            java.time.LocalDate d = parseDatum(dl.waarde());
            return d == null ? null : WaardeFn.constant(d);
        }
        if (e instanceof Expr.Literal dtl && dtl.type() instanceof Datatype.DatumTijd) {
            // §13.2.21 a datum-tijd literal as a VALUE -> its LocalDateTime constant; DRL lowers the same
            // LocalDateTime.of(...) (date + HH:mm:ss[.SSS], millis -> nanoOfSecond).
            java.time.LocalDateTime dt = parseDatumTijd(dtl.waarde());
            return dt == null ? null : WaardeFn.constant(dt);
        }
        // §13.3.3.4 Tekst / §13.2.14 enumeratiewaarde literal value -> its String (an enumeratie/Tekst field is a
        // String; the §12 beslistabel may SET such an attribute). The SAME shared reader the comparisons use, so a
        // written value and a compared value are the same string; DRL renders that string through drlString, so both
        // back-ends store it identically. The value-side unit restatement is a no-op (scaledBy passes a non-number through).
        String tekstLabel = e instanceof Expr.Literal tlit ? Literalen.tekstOfEnumLabel(tlit) : null;
        if (tekstLabel != null) return WaardeFn.constant(tekstLabel);
        // §5.4/§13.4.17.9 tekstreeks "de reeks van <tekst> en <waarde> …": concatenate each part's text — a literal
        // segment verbatim, an inserted expression via rsTekst (leeg -> ""). Mirrors DrlTranspiler.tekstReeks.
        if (e instanceof Expr.TekstReeks tr) {
            List<BiFunction<Object, Map<String, BigDecimal>, Object>> parts = new ArrayList<>();
            Set<String> g = new LinkedHashSet<>();
            for (Expr.TekstReeks.Deel d : tr.delen()) {
                if (d instanceof Expr.TekstReeks.Letterlijk lit) { String t = lit.tekst(); parts.add((s, gm) -> t); }
                else if (d instanceof Expr.TekstReeks.Ingevoegd ing) {
                    WaardeFn wf = scalarWaarde(valueSubject, ing.expr());
                    if (wf == null) return null;
                    g.addAll(wf.globals());
                    parts.add(wf.fn());
                } else return null;
            }
            return new WaardeFn((s, gm) -> {
                StringBuilder sb = new StringBuilder();
                for (BiFunction<Object, Map<String, BigDecimal>, Object> p : parts) sb.append(RegelSpraakRuntime.rsTekst(p.apply(s, gm)));
                return sb.toString();
            }, g);
        }
        if (e instanceof Expr.AttribuutRef a && ongenavigeerd(a)) {
            // §5.8 a SET operand ("de prijs van ALLE elementen", set()==true) is an instance-AGGREGATION SOURCE, not the
            // subject's own scalar attribute — reading it as RegelSpraakRuntime.get(subject, attr) would look the
            // attribute up on the WRONG fact (the subject, which need not declare it) and THROW at fire time, aborting the
            // WHOLE session (an asymmetric, masked crash: DRL honestly markers a bare SET-in-scalar read). Skip it here so
            // the expr-comparison branches stay symmetric with the DRL marker. Mirrors tijdreeksTotaal's a.set() guard.
            if (a.set()) return null;
            // §8.2 a tijdsafhankelijk (List<Segment>) attribute read in a SCALAR slot is NOT a scalar — only the §7.1
            // "het totaal van" collapses a timeline to a scalar (handled in regel() before this path). Splicing the
            // List into a scalar setter would throw at fire time (setX(List) on a BigDecimal field); skip it instead
            // (DRL honestly markers the same read). Mirrors DrlTranspiler.isTemporeelAttribuut / lowerVal's guard.
            if (a.objecttype() != null && Namen.bevatNaam(a.objecttype().granulariteitLeden(), a.attribuut())) return null;
            // §3.6 a bare read of a gedimensioneerd attribuut (its field is a Map<label,value>) is under-specified —
            // a scalar slot needs a pinned label (a DimensieCel) or a dimensie-fold. Skip (DRL markers it likewise).
            if (a.objecttype() != null && !a.objecttype().dimensiesVan(a.attribuut()).isEmpty()) return null;
            // §13.3.9 a BY-PAIR navigation: when the written noun NAMES the objecttype ("zijn vliegtuig", type
            // Vliegtuig) the resolver records navigatieRol=null with objecttype != subject — NOT an own read.
            // Route it through navReader (which resolves the unique by-pair role, null-safe), mirroring
            // DrlTranspiler.naviRol's by-pair fallback; reading it as get(subject, attr) THREW at fire time
            // (no such getter on the subject — surfaced by the keten objectcreatie review fixture).
            if (valueSubject != null && a.objecttype() != null && a.objecttype() != valueSubject) {
                java.util.function.Function<Object, Object> reader = navReader(a, valueSubject);
                if (reader == null) return null;
                return new WaardeFn((s, gm) -> reader.apply(s), Set.of());
            }
            String attr = a.attribuut();
            return new WaardeFn((s, gm) -> RegelSpraakRuntime.get(s, attr), Set.of());
        }
        if (e instanceof Expr.AttribuutRef a && !ongenavigeerd(a) && valueSubject != null) {
            // §6.x a to-ONE NAVIGATED value read ("de X van zijn partner"). DRL renders a PLAIN getter chain
            // ($subject.getRol().getX()) — NOT null-safe — so a null relation NPEs and aborts the session (a latent
            // DRL bug, like the §6.9 DimensieCel null-Map). navReader is null-safe (-> leeg), the CORRECT side: the
            // non-null read AGREES with DRL, the null-relation read is a model-is-better divergence (not differentially
            // testable — DRL crashes). Only reached on the VALUE path (valueSubject set); the condition path skips it.
            if (a.objecttype() != null && Namen.bevatNaam(a.objecttype().granulariteitLeden(), a.attribuut())) return null;   // temporal -> Phase F
            if (a.objecttype() != null && !a.objecttype().dimensiesVan(a.attribuut()).isEmpty()) return null;             // §3.6 gedimensioneerd -> marker
            // §13.4.1 a MULTI-hop chain lowers ONLY on the consequence value path (ketenWaardePad, set around the
            // scalar THEN-waarde) — a chain in a CONDITION keeps the symmetric marker DRL emits (operand/attribuutOperand
            // marker KETEN), so neither back-end fires ahead of the other.
            java.util.function.Function<Object, Object> reader = a.navigatiePad().isEmpty() ? navReader(a, valueSubject)
                    : ketenWaardePad ? navKetenReader(a, valueSubject) : null;
            if (reader == null) return null;
            return new WaardeFn((s, gm) -> reader.apply(s), Set.of());
        }
        if (e instanceof Expr.DimensieCel c && c.attribuut() instanceof Expr.AttribuutRef ca && ongenavigeerd(ca)
                && c.labels().size() == 1) {
            // §3.6 a single labeled cell read of a gedimensioneerd attribuut: ((Map) map).get(label). A multi-label
            // (multi-dimension) or navigated cell stays a skip (DRL markers it). Mirrors DrlTranspiler.waarde(DimensieCel).
            String attr = ca.attribuut();
            String label = c.labels().get(0);
            return new WaardeFn((s, gm) -> { Object m = RegelSpraakRuntime.get(s, attr); return m == null ? null : ((Map<?, ?>) m).get(label); }, Set.of());
        }
        if (e instanceof Expr.ParameterRef pr) {
            String gn = globalNaam(pr.parameter().naam());
            return new WaardeFn((s, gm) -> gm.get(gn), Set.of(gn));
        }
        if (e instanceof Expr.Binair b && b.op() == Expr.BinOp.TOT_DE_MACHT) {
            // §13.4.17.14 power: an INTEGER literal exponent -> exact BigDecimal.pow (rsMacht); else Math.pow
            // (rsMachtReeel). §6.7 a leeg base/operand -> leeg. No unit restatement. Mirrors DrlTranspiler.binairTerm.
            if (b.links().type().isDatum()) return null;
            WaardeFn base = scalarWaarde(valueSubject, b.links());
            if (base == null) return null;
            BiFunction<Object, Map<String, BigDecimal>, Object> bfn = base.fn();
            Integer n = geheelExponent(b.rechts());
            if (n != null) {
                int exp = n;
                return new WaardeFn((s, gm) -> RegelSpraakRuntime.rsMacht(bfn.apply(s, gm), exp), base.globals());
            }
            WaardeFn rf = scalarWaarde(valueSubject, b.rechts());
            if (rf == null) return null;
            BiFunction<Object, Map<String, BigDecimal>, Object> rfn = rf.fn();
            Set<String> g = new LinkedHashSet<>(base.globals()); g.addAll(rf.globals());
            return new WaardeFn((s, gm) -> RegelSpraakRuntime.rsMachtReeel(bfn.apply(s, gm), rfn.apply(s, gm)), g);
        }
        if (e instanceof Expr.Begrensd bg) {
            // §6.1.4 begrenzing (clamp): restate the bounds into the basis unit (§4, like +/-) then rsBegrens
            // (min-then-max, a leeg basis stays leeg). Mirrors DrlTranspiler's value-side clamp ternaries.
            WaardeFn baseF = scalarWaarde(valueSubject, bg.basis());
            if (baseF == null) return null;
            Datatype bt = effType(bg.basis());
            WaardeFn minF = null, maxF = null;
            if (bg.minimum() != null) {
                minF = scalarWaarde(valueSubject, bg.minimum());
                if (minF == null) return null;
                minF = minF.herschaald(effType(bg.minimum()), bt, "een §6.1.4 begrenzing-ondergrens");   // §4 EXACT restate the bound into the basis unit
            }
            if (bg.maximum() != null) {
                maxF = scalarWaarde(valueSubject, bg.maximum());
                if (maxF == null) return null;
                maxF = maxF.herschaald(effType(bg.maximum()), bt, "een §6.1.4 begrenzing-bovengrens");   // §4 EXACT restate the bound into the basis unit
            }
            BiFunction<Object, Map<String, BigDecimal>, Object> bfn = baseF.fn();
            BiFunction<Object, Map<String, BigDecimal>, Object> mnfn = minF == null ? null : minF.fn();
            BiFunction<Object, Map<String, BigDecimal>, Object> mxfn = maxF == null ? null : maxF.fn();
            Set<String> g = new LinkedHashSet<>(baseF.globals());
            if (minF != null) g.addAll(minF.globals());
            if (maxF != null) g.addAll(maxF.globals());
            return new WaardeFn((s, gm) -> {
                // Evaluation ORDER mirrors ref, measured (ref/lib/eval.ml, the Begrensd arm: a let-bound basis,
                // then `Arith.clamp ~min_:(bound minimum) ~max_:(bound maximum) v`, whose labelled arguments OCaml
                // applies right to left): basis, then MAXIMUM, then minimum. Under a ch. 4.10 node that order
                // decides whether a binder read in one bound raises the barrier before a §6.5 site in the other
                // bound runs (CompanionPadWitnessTest (i)) — and, on the value side, whether a division by numeric
                // zero there is reached at all. Java's left-to-right argument order would invert it.
                Object basis = bfn.apply(s, gm);
                Object maximum = mxfn == null ? null : mxfn.apply(s, gm);
                Object minimum = mnfn == null ? null : mnfn.apply(s, gm);
                return RegelSpraakRuntime.rsBegrens(basis, minimum, maximum);
            }, g);
        }
        if (e instanceof Expr.Binair b && arithmeticOp(b.op())) {
            // §6.2-6.5 binary arithmetic with the leegwaarde tables. Datum arithmetic (datum ± tijdsduur) lowers
            // via datumRekenWaarde (.plus/.minus, not "+"). +/- restate the right operand into the LEFT's unit (§4);
            // MAAL does not (units multiply). The op's empty-operand rule: PLUS/MIN/MAAL = leeg counts as 0;
            // VERMINDERD_MET = empty LEFT -> leeg; GEDEELD_DOOR = empty LEFT -> 0, value/empty -> leeg.
            if (b.links().type().isDatum() || b.rechts().type().isDatum()) return datumRekenWaarde(valueSubject, b);
            WaardeFn lf = scalarWaarde(valueSubject, b.links()), rf = scalarWaarde(valueSubject, b.rechts());
            if (lf == null || rf == null) return null;
            Expr.BinOp op = b.op();
            if (op == Expr.BinOp.PLUS || op == Expr.BinOp.MIN || op == Expr.BinOp.VERMINDERD_MET)
                rf = rf.herschaald(effType(b.rechts()), effType(b.links()), "een additieve operandgrens");   // §4 EXACT restate rhs into lhs unit (self-noops if same)
            BiFunction<Object, Map<String, BigDecimal>, Object> lfn = lf.fn(), rfn = rf.fn();
            Set<String> g = new LinkedHashSet<>(lf.globals()); g.addAll(rf.globals());
            return new WaardeFn((s, gm) -> {
                Object lv = lfn.apply(s, gm), rv = rfn.apply(s, gm);
                // §389/§486 route ALL arithmetic through the Breuk-aware rs* helpers (the single-runtime principle,
                // shared with the DRL import functions): +,-,× propagate exactness when an operand is a Breuk and
                // otherwise stay on the fast DECIMAL128 BigDecimal path; ÷ and percentage keep the exact rational
                // when it does not terminate. No host-language infix ever sees a Breuk.
                return switch (op) {
                    case PLUS           -> RegelSpraakRuntime.rsPlus(lv, rv);
                    case MIN            -> RegelSpraakRuntime.rsMin(lv, rv);
                    case VERMINDERD_MET -> RegelSpraakRuntime.rsVerminderd(lv, rv);
                    case MAAL           -> RegelSpraakRuntime.rsMaal(lv, rv);
                    // §6.5 Tabel 12: a filled dividend over a leeg divisor is the Fout the value side degrades to
                    // leeg — reported to the path probe of the site's companion (a no-op without one, see Sonde).
                    case GEDEELD_DOOR   -> { if (lv != null && rv == null) Sonde.meld(gm); yield RegelSpraakRuntime.rsGedeeld(lv, rv); }
                    case GEDEELD_DOOR_ABS -> { if (lv != null && rv == null) Sonde.meld(gm); yield RegelSpraakRuntime.rsGedeeldAbs(lv, rv); }   // §6.5 (5 dec, toward zero)
                    case PERCENTAGE_VAN -> RegelSpraakRuntime.rsPercentageVan(lv, rv);
                    default             -> null;   // unreachable (arithmeticOp gates)
                };
            }, g);
        }
        if (e instanceof Expr.Functie fnc) {
            WaardeFn sf = scalarFunctie(valueSubject, fnc);   // §13.4.17 abs / date-component / datum-met / paasdag
            if (sf != null) return sf;
        }
        return scalarWrapper(valueSubject, e);   // §13.4.17.25 afronding / §13.4.17.13 wortel
    }

    // --- IR-SPEC ch. 4.10 / RUNTIME-SEMANTICS ch. 10.6 + 10.2: the Let cluster -----------------------------

    /**
     * The binder ENVIRONMENT slot for {@code binder} inside the (name -&gt; value) map every {@link WaardeFn}
     * already threads. Reusing that map is what keeps the binder environment PURELY FUNCTIONAL: a {@code Let}
     * evaluation copies the incoming map, adds its own slot and hands the copy to the body, so no cell, field
     * or thread-local is shared — a KieBase lambda may be fired by any number of sessions and threads at once.
     *
     * <p>The slot name carries a SPACE, which {@code Namen.ident} can never produce, so it cannot collide with a
     * Parameter global (nor with the {@code "rekendatum"} pseudo-slot). Lower-cased, mirroring ref's {@code norm}
     * over {@code locals}, so binder resolution is case-insensitive on both sides.
     */
    private static String binderSleutel(String binder) {
        return "binder " + (binder == null ? "" : binder.toLowerCase(java.util.Locale.ROOT));
    }

    /**
     * The §6.5/§6.6 fout-companion's <b>path probe</b>. A division or wortel that sits under a ch. 4.10 node
     * (a {@link Expr.Let} or an {@link Expr.IfThenElse}) faults only if its site is actually EVALUATED, and under ch. 10.2 (exactly one
     * branch runs) and ch. 10.6 (call-by-need; the leeg-barrier at the read) that depends on which branch the
     * guard takes, which reads force which needs, and in what ORDER — a read left of the site can end the Let
     * before the site is reached, a read right of it cannot. No static predicate over the operands can say
     * that; the value lowering can, because it IS that operational semantics ({@link #letWaarde}: the
     * {@link Binderslot} thunk, the per-evaluation {@link Barriere}, the branch guard). So such a companion
     * re-evaluates the OUTERMOST such node's own {@link WaardeFn} on the subject with a probe planted in the
     * environment map under {@link #SLEUTEL}, and every §6.5/§6.6 site that faults while running reports
     * itself here (the division arm of {@link #scalarWaarde}, the wortel arm of {@link #scalarWrapper}). A site
     * on a path that does not run — the branch not taken, a value no read forces, anything behind a read that
     * raised a barrier first — never executes, so it never reports: the companion's condition is exactly
     * ref's ({@code ref/lib/eval.ml}) — PROVIDED this lowering evaluates each form's children in ref's order.
     * That order is not inherited automatically: it is measured per form and mirrored here (a Binair left
     * before right; a Begrensd basis, then MAXIMUM, then minimum — the arm above, corrected in fix round 1 of
     * the companion-onder-drager card, where the Java argument order had it minimum-first and the probe both
     * fabricated and missed on the two Begrensd flanks). A form whose child order differs from ref's makes the
     * probe wrong on exactly the flank where a barrier read and a site sit in different children.
     *
     * <p>The outermost ch. 4.10 node is the right unit to probe: every other form the companion walks descend
     * (Binair, Afgerond, Begrensd, a Functie's arguments) evaluates ALL its children unconditionally, here and
     * in ref, and a barrier never escapes the Let that raised it — so a ch. 4.10 node at the top of its path runs
     * exactly when the host value runs, and a site with no such node above it keeps its static leeg/negatief
     * predicate (unchanged v0 behaviour, DRL parity). The aggregation-arithmetic path ({@link #lowerVal})
     * lowers no ch. 4.10 node at all (a host with one there skips, and skips its companions with it), so every
     * path-probed site is under {@link #scalarWaarde}, which is what the probe re-lowers.
     *
     * <p><b>What the gate asks, and what it therefore does and does not guarantee.</b> The companion fires when
     * SOME §6.5/§6.6 site under this node ran and faulted — not when the one syntactic site the companion is
     * named after did. Measured, fix round 2 of the companion-onder-drager card: on
     * {@code Let d = deler in ((inkomen ÷ tweede) plus (5,00 ÷ leegding))} with {@code leegding} absent, ref says
     * INCONSISTENT and a per-site gate says consistent in SILENCE — the second site has a Literal dividend, so it
     * qualifies for no companion of its own ({@link #kwalificeertOperand}), yet it runs and faults, and
     * {@link #meld} sits in the operand-blind division arm, so the probe SEES it (witness (a'')). Neither object
     * nor structural site identity recovers that: the reported site is a different site by either notion
     * (both candidates measured; the structural one leaves the flank red and would additionally conflate two
     * structurally equal sites). What the wide gate keeps is the no-fabrication half — every report is a real
     * fault on a path that really ran, so a barrier that cuts the sites off fires nothing (the twin of (a'')).
     * What it gives up is per-site attribution: two qualifying sites under one node make two companions that
     * both fire on either one's fault. That is not observable — the inserted Inconsistentie carries the HOST
     * rule name and subject, never the companion name, and {@link #companionRule}'s §9.5 idempotency guard
     * collapses them to one fact — but a case pin cannot see it either, so it is stated here rather than
     * witnessed. The remaining coverage limit is unchanged: a ch. 4.10 node with NO qualifying site gets no companion
     * at all, so a lone non-qualifying site under it still misses its fault.
     *
     * <p>Session-safe for the same reason {@link Binderslot} is: created inside the companion's consequence,
     * planted in that invocation's own map (which a {@code Let} copies, carrying the reference along), and
     * discarded with it. The host rule's own evaluation carries no probe, so a report there is a null-checked
     * no-op.
     */
    private static final class Sonde {
        /** The environment slot the probe rides in — it carries a space, which {@code Namen.ident} can never
         *  produce, so no Parameter global can collide with it, and it lacks the {@code "binder "} prefix every
         *  {@link #binderSleutel binder slot} carries, so no binder slot can either. */
        static final String SLEUTEL = "sonde fout";
        private boolean gemeld;

        /** Report a §6.5/§6.6 fault at the site being evaluated — a no-op when no probe rides this evaluation. */
        static void meld(Map<?, ?> gm) {
            if (gm.get(SLEUTEL) instanceof Sonde sonde) sonde.gemeld = true;
        }

        /** Whether a §6.5/§6.6 site under the probed node ran and faulted (see the class comment on why this
         *  is not per-site). */
        boolean heeftFoutGemeld() { return gemeld; }
    }

    /**
     * ch. 10.6 clause 2's <b>leeg-barrier</b> as control flow: a read of a binder whose value is leeg ends the
     * {@code Let} that bound it, wherever in the body that read sits. One INSTANCE is created per {@code Let}
     * EVALUATION — the local-exception discipline {@code ref/lib/eval.ml} gets from OCaml's {@code let
     * exception} — and a handler rethrows anything that is not its own instance, so a nested {@code Let}'s
     * barrier passes an enclosing handler by: the barrier belongs to the Let that bound the binder that was
     * read, never to an enclosing one. Stackless: it is a signal, not a diagnosis.
     */
    private static final class Barriere extends RuntimeException {
        Barriere() { super(null, null, false, false); }
    }

    /**
     * ch. 10.6 clause 1's <b>need</b>: what a binder slot carries under call-by-need. The value is evaluated at
     * the FIRST {@link #lees()} and that one result serves every further read, so a value whose evaluation
     * deposits a §9.5 channel reason deposits it once; a body that never reads the binder never evaluates the
     * value at all (clause 3 — no leeg, no fault, no channel reason).
     *
     * <p><b>Why this mutable cell is session-safe.</b> It is created INSIDE the {@link WaardeFn} invocation that
     * binds it and reaches only that invocation's own copy of the environment map, which is discarded when the
     * call returns — it is never a field, never a capture of the KieBase lambda, never shared. The lambdas
     * themselves stay stateless (they close over the two compiled fns and the slot name only), so one KieBase
     * may be fired by any number of sessions and threads at once, exactly as before this went lazy.
     */
    private static final class Binderslot {
        private final java.util.function.Supplier<Object> waardeFn;
        private final Barriere barriere;
        private boolean geforceerd;
        private Object waarde;

        Binderslot(java.util.function.Supplier<Object> waardeFn, Barriere barriere) {
            this.waardeFn = waardeFn;
            this.barriere = barriere;
        }

        /** The need, forced at most once; a leeg value raises the OWNING Let's barrier instead of answering null. */
        Object lees() {
            if (!geforceerd) {
                waarde = waardeFn.get();
                geforceerd = true;
            }
            if (waarde == null) throw barriere;
            return waarde;
        }
    }

    /**
     * Lower one ch. 4.10 Let-cluster node to the ordinary {@link WaardeFn}, so the whole existing value machinery
     * (the leegwaarde tables, the §4 unit restatement, the Breuk-aware rs* helpers) applies to a Let body
     * unchanged — the arbiter is {@code ref/lib/eval.ml}, never a second arithmetic written here.
     *
     * <p><b>Globals hygiene.</b> A {@code WaardeFn}'s globals set is what {@link #regel} (and the ch. 7 defaults
     * lowering) binds on the consequence; a forgotten name would read null and answer a silent leeg. So the sets
     * are UNIONED across value, body, condition and both branches — and a {@code Let} REMOVES its own binder slot
     * from what it reports, since it binds that slot itself. A binder slot that somehow escaped its Let would
     * therefore surface as an unregistered global and become a loud skip, never a null read. Under ch. 10.6's
     * call-by-need the value's OWN globals stay in that union even though the value may never run: the thunk is
     * built from the enclosing map, so every name it could read must still be bound on the consequence.
     */
    private WaardeFn letWaarde(Objecttype valueSubject, Expr e) {
        if (e instanceof Expr.BinderRef lz) {
            // WF-064: the nearest enclosing binder of this name. "Nearest" is exactly what the map gives —
            // an inner Let's put SHADOWS the outer one in the copy the body sees. ch. 10.6 clause 1+2: the read
            // FORCES the need (once) and, if that value is leeg, raises the owning Let's barrier rather than
            // answering leeg — which is what keeps a bound binder never-leeg (WF-077's condition operands total).
            String sleutel = binderSleutel(lz.naam());
            return new WaardeFn((s, gm) -> {
                Object slot = ((Map<?, ?>) gm).get(sleutel);
                if (!(slot instanceof Binderslot need)) {
                    // unreachable by the globals hygiene above (an escaped binder slot is refused at build
                    // time, loudly); fail fast rather than answer a silent leeg if it ever is reached.
                    throw new IllegalStateException("Let-binder '" + sleutel + "' is niet gebonden bij het lezen");
                }
                return need.lees();
            }, Set.of(sleutel));
        }
        if (e instanceof Expr.Let b) {
            // ch. 10.6: [value] is evaluated in the ENCLOSING scope (WF-076 — the binder is not visible in its
            // own value), so it compiles against the untouched incoming map.
            WaardeFn vf = scalarWaarde(valueSubject, b.value());
            if (vf == null) return null;
            WaardeFn bf = scalarWaarde(valueSubject, b.body());
            if (bf == null) return null;
            String sleutel = binderSleutel(b.binder());
            BiFunction<Object, Map<String, BigDecimal>, Object> vfn = vf.fn(), bfn = bf.fn();
            Set<String> g = new LinkedHashSet<>(vf.globals());
            for (String n : bf.globals()) if (!n.equals(sleutel)) g.add(n);
            return new WaardeFn((s, gm) -> {
                // ch. 10.6 call-by-need: the slot carries the THUNK, not the value. The body ALWAYS runs (clause
                // 3: an unread binder evaluates nothing — no leeg, no fault, no §9.5 reason, and the Let's
                // outcome IS the body's); a read forces the need (clause 1, once) and a leeg need raises THIS
                // Let's barrier (clause 2), caught here — and only here, since the token is this evaluation's own.
                Barriere barriere = new Barriere();
                Binderslot need = new Binderslot(() -> vfn.apply(s, gm), barriere);
                Map<String, BigDecimal> omgeving = new LinkedHashMap<>(gm);
                // no BigDecimal cast — the map is erased (see bindGlobal/vulGlobalMap); a binder value is whatever
                // its own expression computed (a BigDecimal, an exact Breuk, a date, a label) and only this slot's
                // reader touches it, through the cell.
                ((Map<String, Object>) (Map) omgeving).put(sleutel, need);
                try {
                    return bfn.apply(s, omgeving);
                } catch (Barriere raised) {
                    if (raised != barriere) throw raised;   // an enclosing Let's barrier, only passing through
                    return null;                            // the read emptied THIS Let
                }
            }, g);
        }
        Expr.IfThenElse k = (Expr.IfThenElse) e;
        LetConditie c = letConditie(valueSubject, k.conditie());
        if (c == null) return null;
        WaardeFn dan = scalarWaarde(valueSubject, k.dan());
        if (dan == null) return null;
        WaardeFn anders = scalarWaarde(valueSubject, k.anders());
        if (anders == null) return null;
        BiFunction<Object, Map<String, BigDecimal>, Object> dfn = dan.fn(), afn = anders.fn();
        java.util.function.BiPredicate<Object, Map<String, BigDecimal>> test = c.test();
        Set<String> g = new LinkedHashSet<>(c.globals());
        g.addAll(dan.globals());
        g.addAll(anders.globals());
        // ch. 10.2: evaluate EXACTLY the chosen branch — strict, no coalesce, no evaluation of the other arm.
        return new WaardeFn((s, gm) -> test.test(s, gm) ? dfn.apply(s, gm) : afn.apply(s, gm), g);
    }

    /** A lowered ch. 10.2 branch guard: a (subject, globals+binders) -&gt; waar/onwaar test plus the global names
     *  its operands read (a Let binder slot among them — {@link #letWaarde} strips those at its own node). */
    private record LetConditie(java.util.function.BiPredicate<Object, Map<String, BigDecimal>> test, Set<String> globals) {}

    /**
     * Lower an {@code IfThenElse} condition (ch. 10.2, its operands disciplined to bound-non-leeg reads by
     * WF-077). Both sides lower through {@link #scalarWaarde} — so a binder, a parameter, a literal and the
     * arithmetic combinators over them all work — and the verdict comes from
     * {@link RegelSpraakRuntime#rsAggVergelijk}, the SAME shared helper the aggregation-comparison path uses.
     * That helper mirrors ref's Tabel 33/37/44 leeg semantics AND compares an exact {@link
     * io.github.yhavinga.regelspraak.runtime.Breuk} exactly, so no cast can throw at fire.
     *
     * <p>null (an honest, reason-carrying skip at the caller) for anything else: a non-comparison condition, a
     * datum operator, a non-numeric operand, an operand that does not lower — and for a comparison that would
     * need a §5.1 Tabel 32 UNIT restatement, which {@code rsAggVergelijk} does not apply. No half lowering.
     */
    private LetConditie letConditie(Objecttype valueSubject, Conditie c) {
        if (!(c instanceof Conditie.Vergelijking v) || !isNumeriekOp(v.op())) return null;
        if (!getalachtigVergelijkbaar(v.links()) || !getalachtigVergelijkbaar(v.rechts())) return null;
        BigDecimal[] f = restatement(effType(v.links()), effType(v.rechts()));
        if (f[0].compareTo(BigDecimal.ONE) != 0 || f[1].compareTo(BigDecimal.ONE) != 0) return null;   // cross-unit guard
        WaardeFn lw = scalarWaarde(valueSubject, v.links()), rw = scalarWaarde(valueSubject, v.rechts());
        if (lw == null || rw == null) return null;
        BiFunction<Object, Map<String, BigDecimal>, Object> lfn = lw.fn(), rfn = rw.fn();
        String opStr = Operatoren.symbool(v.op());
        Set<String> g = new LinkedHashSet<>(lw.globals());
        g.addAll(rw.globals());
        return new LetConditie((s, gm) -> RegelSpraakRuntime.rsAggVergelijk(lfn.apply(s, gm), rfn.apply(s, gm), opStr), g);
    }

    /**
     * ALL {@link Expr} children a ch. 4.10 node carries for the tree walks in this file: a {@link Expr.Let}'s
     * value+body, and an {@link Expr.IfThenElse}'s two branches plus EVERY Expr operand of its condition.
     * Empty for anything else. This is the
     * POSSIBLY-evaluated set — which children actually run is a fire-time question (ch. 10.2/10.6), and the one
     * walk whose output is an ANSWER rather than a refusal or registration (the §6.5/§6.6 companions) settles it
     * at fire time through the path probe ({@link Sonde}), never by seeing less here.
     *
     * <p>The walks below are hand-written over concrete v0 node shapes, so they descend a Let only by taking this
     * accessor — a walk that does not silently under-reports and answers wrong. The full inventory, so the next
     * addition sees it:
     *
     * <p><b>(a) Descend, output a REFUSAL or a REGISTRATION</b>, where seeing more than actually runs is still a
     * correct answer: {@link #foutplekSoort} (the ch. 7 defaults division/wortel fence — refuses more often),
     * {@link #navRefs} (far-side reactivity — watches more), {@link #exprLeesnamen} (defaults salience — sinks
     * further), {@link #kanBreukOpleveren} (exact-comparison routing — at worst takes the exact path where the
     * plain one would also have served; blind, it takes a {@code (BigDecimal)} cast that throws).
     *
     * <p><b>(b) Descend, output an ANSWER</b> — a §6.5/§6.6 fout-companion, which is WRONG (inconsistent where ref
     * says consistent) if built off a path that never runs: {@link #verzamelDelingen} and {@link #verzamelWortels}.
     * They see every site that MAY run, and a companion of a site under a Let or an IfThenElse carries the PATH: its
     * condition re-evaluates the outermost such node on the subject with a probe ({@link Sonde}) and fires only
     * when a §6.5/§6.6 site under it actually ran and faulted.
     *
     * <p><b>(c) Deliberately do NOT descend</b>, each for a checked reason: {@link #startpuntCompanions} (§6.8 —
     * ref carries no leeg-anchor check at all, so descending would ADD a ref-divergence; recorded at that method);
     * {@code parityOnveilig}, {@code containsAggregatie}, {@code isDatumRekenExpr} and {@link #lowerVal}, which
     * pattern-match concrete v0 node shapes, so a Let matches none and the answer is "not that shape" — none can
     * fabricate a fault, because what a Let holds is lowered, or refused loudly, by {@link #scalarWaarde}, and
     * {@code lowerVal}'s own fall-through is a null, i.e. an honest skip; {@code companionOperand}, the STATIC
     * leeg/vol predicate over a site's operands, which stays a walk over {@code AttribuutRef} only (DRL parity) —
     * the scope question moved one level up, to {@link #kwalificeertOperand}; and
     * {@code transpiler.Stratificatie.verzamelReads} (§9.2), a {@code Conditie} walk over a rule's voorwaarde only,
     * which an IfThenElse's guard (living in the VALUE) never reaches.
     */
    private List<Expr> letKinderen(Expr e) {
        if (e instanceof Expr.Let b) return List.of(b.value(), b.body());
        if (!(e instanceof Expr.IfThenElse k)) return List.of();   // a BinderRef reads only the environment
        List<Expr> uit = new ArrayList<>(List.of(k.dan(), k.anders()));
        conditieOperanden(k.conditie(), uit);
        return uit;
    }

    /**
     * Every {@link Expr} operand a {@link Conditie} carries, recursively through a Samengesteld. Deliberately
     * form-COMPLETE rather than restricted to the {@link #letConditie} shapes the ch. 10.2 guard actually
     * lowers: coupling the two would make a structural collector depend, silently, on which condition forms a
     * VALUE lowering happens to accept today — the next widening of letConditie would leave every walk blind
     * again with nothing to notice it.
     *
     * <p>Being form-complete here is SAFE because of what a refused form does: a condition shape letConditie
     * will not lower makes its IfThenElse lower to null, which skips the whole rule LOUDLY — so operands collected
     * from such a shape never reach a rule that plays. It is emphatically NOT an "extra fault is harmless"
     * argument: an extra §6.5/§6.6 companion is a WRONG ANSWER (inconsistent where ref says consistent), not a
     * refusal. The two companion walks therefore never decide from this collection alone: a site they find
     * under a ch. 4.10 node gets a companion whose condition is the path probe ({@link Sonde}), which fires only
     * when the site actually ran.
     */
    private static void conditieOperanden(Conditie c, List<Expr> uit) {
        if (c instanceof Conditie.Vergelijking v) {
            if (v.links() != null) uit.add(v.links());
            if (v.rechts() != null) uit.add(v.rechts());
        } else if (c instanceof Conditie.Predicaat p) {
            if (p.operand() != null) uit.add(p.operand());
        } else if (c instanceof Conditie.Periode p) {
            if (p.vanaf() != null) uit.add(p.vanaf());
            if (p.tot() != null) uit.add(p.tot());
        } else if (c instanceof Conditie.Samengesteld s) {
            for (Conditie onderdeel : s.onderdelen()) conditieOperanden(onderdeel, uit);
        }
        // Kenmerk / Rollidmaatschap / VerreRollidmaatschap / Regelversie / Onopgelost carry NO Expr operand.
    }

    /** §13.4.17 single-value builtin Functies as a value: §13.4.17.17 ABSOLUTE_WAARDE (rsAbs), §13.4.17.18-.24
     *  date/time component (rsDatumDeel via a ChronoField — a time part off a plain Datum markers), §13.4.17.35/.36
     *  DATUM_MET/DATUMTIJD_MET (rsDatumMet/rsDatumtijdMet), §13.4.17.37 EERSTE_PAASDAG. Each argument lowers through
     *  scalarWaarde; null for an unsupported kind/arity or an unlowerable argument. WORTEL is handled by scalarWrapper
     *  (its mandatory afronding); the aggregations/tijdsduur go through other paths. Mirrors DrlTranspiler.scalarFunctie. */
    private WaardeFn scalarFunctie(Objecttype valueSubject, Expr.Functie f) {
        List<Expr> a = f.argumenten();
        WaardeFn fold = concatenatieFold(valueSubject, f);   // §4.12-4.15/§5.8.3 "de minimale/maximale/eerste/laatste van A en B …"
        if (fold != null) return fold;
        java.time.temporal.ChronoField cf = switch (f.kind()) {
            case JAAR_UIT         -> java.time.temporal.ChronoField.YEAR;
            case MAAND_UIT        -> java.time.temporal.ChronoField.MONTH_OF_YEAR;
            case DAG_UIT          -> java.time.temporal.ChronoField.DAY_OF_MONTH;
            case UUR_UIT          -> java.time.temporal.ChronoField.HOUR_OF_DAY;
            case MINUUT_UIT       -> java.time.temporal.ChronoField.MINUTE_OF_HOUR;
            case SECONDE_UIT      -> java.time.temporal.ChronoField.SECOND_OF_MINUTE;
            case MILLISECONDE_UIT -> java.time.temporal.ChronoField.MILLI_OF_SECOND;
            default               -> null;
        };
        if (cf != null) {
            if (a.size() != 1) return null;
            boolean tijdDeel = switch (f.kind()) { case UUR_UIT, MINUUT_UIT, SECONDE_UIT, MILLISECONDE_UIT -> true; default -> false; };
            if (tijdDeel && !(a.get(0).type() instanceof Datatype.DatumTijd)) return null;   // a time part off a plain Datum markers
            WaardeFn arg = scalarWaarde(valueSubject, a.get(0));
            if (arg == null) return null;
            BiFunction<Object, Map<String, BigDecimal>, Object> fn = arg.fn();
            return new WaardeFn((s, gm) -> RegelSpraakRuntime.rsDatumDeel(fn.apply(s, gm), cf), arg.globals());
        }
        switch (f.kind()) {
            case ABSOLUTE_WAARDE -> {
                if (a.size() != 1) return null;
                WaardeFn arg = scalarWaarde(valueSubject, a.get(0));
                if (arg == null) return null;
                BiFunction<Object, Map<String, BigDecimal>, Object> fn = arg.fn();
                return new WaardeFn((s, gm) -> RegelSpraakRuntime.rsAbs(fn.apply(s, gm)), arg.globals());
            }
            case EERSTE_PAASDAG -> {
                if (a.size() != 1) return null;
                WaardeFn arg = scalarWaarde(valueSubject, a.get(0));
                if (arg == null) return null;
                BiFunction<Object, Map<String, BigDecimal>, Object> fn = arg.fn();
                return new WaardeFn((s, gm) -> RegelSpraakRuntime.rsEerstePaasdag(fn.apply(s, gm)), arg.globals());
            }
            case DATUM_MET     -> { return datumConstructor(valueSubject, a, 3); }
            case DATUMTIJD_MET -> { return datumConstructor(valueSubject, a, 7); }
            case TIJDSDUUR, ABSOLUTE_TIJDSDUUR -> {
                // §6.10 / §13.4.17.32 "de [absolute] tijdsduur van A tot B in [hele] <tijdseenheid>" — the number of
                // COMPLETE units between two dates. Mirrors DrlTranspiler.tijdsduur exactly: a DAGSOORT eenheid counts
                // the §9.8 dagsoort days in the half-open interval (van, tot] (rsTijdsduurDagsoort, day-granular ->
                // Datum operands); any OTHER unit is ChronoUnit.<U>.between via rsTijdsduur (a kwartaal = 3×MONTHS), with
                // a TIME unit (uur/.../ms) demanding datum-tijd operands. A kwartaal/unrecognised non-dagsoort unit -> skip.
                if (a.size() != 2) return null;
                boolean absoluut = f.kind() == Expr.FunctieKind.ABSOLUTE_TIJDSDUUR;
                // ANNOTATION BY DESIGN (WF-038, ch. 2.10 exception 3): a tijdsduur's own unit IS the operator's
                // parameter ("in jaren"), not a derived result type — ref reads the same node type here.
                io.github.yhavinga.regelspraak.model.Eenheid e = f.type().eenheidOf();
                // ch. 3.8 puts the CALENDAR TABLE first — see Tijdrekening.dagsoortVoorEenheid for the order.
                String dagsoortUnit = Tijdrekening.dagsoortVoorEenheid(model, e);
                if (dagsoortUnit != null) {
                    // §4.16 L676: both operands must be the SAME tijd-precisie. Both datum-EN-tijd is legal — count over the
                    // DATE parts (rsTijdsduurTijdDagsoort, no time carried). Mixed precision is spec-illegal -> honest skip.
                    boolean dt0 = a.get(0).type() instanceof Datatype.DatumTijd, dt1 = a.get(1).type() instanceof Datatype.DatumTijd;
                    if (dt0 != dt1) return null;
                    java.util.function.Predicate<java.time.LocalDate> pred = dagsoortPredikaat(dagsoortUnit);
                    WaardeFn vanF = scalarWaarde(valueSubject, a.get(0)), totF = scalarWaarde(valueSubject, a.get(1));
                    if (pred == null || vanF == null || totF == null) return null;
                    BiFunction<Object, Map<String, BigDecimal>, Object> vfn = vanF.fn(), tfn = totF.fn();
                    Set<String> g = new LinkedHashSet<>(vanF.globals()); g.addAll(totF.globals());
                    if (dt0)
                        return new WaardeFn((s, gm) -> RegelSpraakRuntime.rsTijdsduurTijdDagsoort(
                                (java.time.LocalDateTime) vfn.apply(s, gm), (java.time.LocalDateTime) tfn.apply(s, gm), pred, absoluut), g);
                    return new WaardeFn((s, gm) -> RegelSpraakRuntime.rsTijdsduurDagsoort(
                            (java.time.LocalDate) vfn.apply(s, gm), (java.time.LocalDate) tfn.apply(s, gm), pred, absoluut), g);
                }
                // general (non-dagsoort) tijdseenheid: ChronoUnit.<U>.between (kwartaal -> 3×MONTHS) via rsTijdsduur.
                String unit = Tijdrekening.chronoUnit(f.type());
                if (unit == null) return null;   // unrecognised tijdseenheid -> honest skip (symmetric with the DRL marker)
                boolean tijdEenheid = switch (unit) { case "HOURS", "MINUTES", "SECONDS", "MILLIS" -> true; default -> false; };
                if (tijdEenheid && (!(a.get(0).type() instanceof Datatype.DatumTijd) || !(a.get(1).type() instanceof Datatype.DatumTijd)))
                    return null;   // a time-unit tijdsduur needs datum-tijd operands (ChronoUnit.HOURS.between throws on a LocalDate)
                WaardeFn vanF = scalarWaarde(valueSubject, a.get(0)), totF = scalarWaarde(valueSubject, a.get(1));
                if (vanF == null || totF == null) return null;
                BiFunction<Object, Map<String, BigDecimal>, Object> vfn = vanF.fn(), tfn = totF.fn();
                Set<String> g = new LinkedHashSet<>(vanF.globals()); g.addAll(totF.globals());
                return new WaardeFn((s, gm) -> RegelSpraakRuntime.rsTijdsduur(
                        (java.time.temporal.Temporal) vfn.apply(s, gm), (java.time.temporal.Temporal) tfn.apply(s, gm), unit, absoluut), g);
            }
            default            -> { return null; }
        }
    }

    /** §13.4.17.35/.36 lower a DATUM_MET (3 parts) / DATUMTIJD_MET (7 parts) constructor to a WaardeFn over its
     *  numeric component value-fns (rsDatumMet/rsDatumtijdMet, §6.9 any empty part -> leeg). null on arity/argument. */
    private WaardeFn datumConstructor(Objecttype valueSubject, List<Expr> a, int n) {
        if (a.size() != n) return null;
        List<BiFunction<Object, Map<String, BigDecimal>, Object>> fns = new ArrayList<>();
        Set<String> g = new LinkedHashSet<>();
        for (Expr e : a) {
            WaardeFn p = scalarWaarde(valueSubject, e);
            if (p == null) return null;
            fns.add(p.fn());
            g.addAll(p.globals());
        }
        if (n == 3) return new WaardeFn((s, gm) ->
                RegelSpraakRuntime.rsDatumMet(fns.get(0).apply(s, gm), fns.get(1).apply(s, gm), fns.get(2).apply(s, gm)), g);
        return new WaardeFn((s, gm) -> RegelSpraakRuntime.rsDatumtijdMet(
                fns.get(0).apply(s, gm), fns.get(1).apply(s, gm), fns.get(2).apply(s, gm), fns.get(3).apply(s, gm),
                fns.get(4).apply(s, gm), fns.get(5).apply(s, gm), fns.get(6).apply(s, gm)), g);
    }

    /** §4.12-4.15/§5.8.3-.4 the concatenation-of-expressies fold "de minimale/maximale/eerste/laatste van A en B
     *  [en C …]" over ≥2 LISTED scalar operands (distinct from a single-operand instance aggregation). A left fold:
     *  MINIMUM/EERSTE keep the smaller/earlier (Comparable compareTo — BigDecimal value-wise, LocalDate(Time)
     *  natural), MAXIMUM/LAATSTE the larger/later; §4.12-4.15 leegwaarde — a null operand is SKIPPED, all-null yields
     *  null. Numeric operands 1..n are restated into the first operand's unit (§Tabel 22/23, the result unit). null
     *  for <2 operands / a restricted (filter) fold / an unlowerable operand. Mirrors DrlTranspiler.concatenatieAggregatie. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private WaardeFn concatenatieFold(Objecttype valueSubject, Expr.Functie f) {
        boolean kleinste = f.kind() == Expr.FunctieKind.MINIMUM || f.kind() == Expr.FunctieKind.EERSTE;
        boolean grootste = f.kind() == Expr.FunctieKind.MAXIMUM || f.kind() == Expr.FunctieKind.LAATSTE;
        if ((!kleinste && !grootste) || f.argumenten().size() < 2 || f.filter() != null) return null;
        Datatype doel = effType(f.argumenten().get(0));   // Eenheid1: result carries the first operand's unit
        List<BiFunction<Object, Map<String, BigDecimal>, Object>> fns = new ArrayList<>();
        Set<String> g = new LinkedHashSet<>();
        for (int i = 0; i < f.argumenten().size(); i++) {
            Expr arg = f.argumenten().get(i);
            WaardeFn wf = scalarWaarde(valueSubject, arg);
            if (wf == null) return null;   // a set/instance-aggregation or unlowerable operand -> skip (DRL markers too)
            if (i > 0) wf = wf.herschaald(effType(arg), doel, "een §4.6 Eenheid1 fold-operand");   // §4 EXACT restate operand i into the result unit (Eenheid1)
            fns.add(wf.fn());
            g.addAll(wf.globals());
        }
        final boolean fKlein = kleinste;
        return new WaardeFn((s, gm) -> {
            Comparable acc = null;
            for (BiFunction<Object, Map<String, BigDecimal>, Object> fn : fns) {
                Object x = fn.apply(s, gm);
                if (x == null) continue;                       // §4.12-4.15: skip a leeg operand
                if (acc == null) { acc = (Comparable) x; continue; }
                int c = acc.compareTo(x);                      // ties take x, mirroring DRL's `acc < x ? acc : x`
                acc = fKlein ? (c < 0 ? acc : (Comparable) x) : (c > 0 ? acc : (Comparable) x);
            }
            return acc;
        }, g);
    }
    private static boolean arithmeticOp(Expr.BinOp op) {
        return op == Expr.BinOp.PLUS || op == Expr.BinOp.MIN || op == Expr.BinOp.VERMINDERD_MET
                || op == Expr.BinOp.MAAL || op == Expr.BinOp.GEDEELD_DOOR
                || op == Expr.BinOp.GEDEELD_DOOR_ABS || op == Expr.BinOp.PERCENTAGE_VAN;
    }
    /** A non-negative INTEGER literal exponent for §13.4.17.14 "tot de macht" (exact BigDecimal.pow(int)), or null for
     *  a fractional/negative/runtime exponent (-> the Math.pow rsMachtReeel path). Mirrors DrlTranspiler.geheelExponent
     *  (via bd() — a fraction-literal exponent therefore falls through to scalarWaarde(valueSubject, rechts)). */
    private static Integer geheelExponent(Expr e) {
        if (!(e instanceof Expr.Literal lit) || !lit.type().isNumeriek()) return null;
        BigDecimal v = bd(lit);
        if (v == null || v.signum() < 0 || v.stripTrailingZeros().scale() > 0) return null;
        try { int n = v.intValueExact(); return n <= 999_999_999 ? n : null; }
        catch (ArithmeticException ignore) { return null; }
    }

    // ---------------- §5.8 aggregation inside arithmetic (Phase C follow-up) ----------------
    /** A lowered value-tree node: given the subject, the resolved global values, and the bound aggregation results
     *  (index-aligned with the accVars the lowering collected), produce the value. */
    @FunctionalInterface
    interface Eval { Object apply(Object subject, Map<String, BigDecimal> globals, Object[] aggResults); }

    /** §13.4.6 one lowered objectcreatie toekenning value: EITHER a scalar {@link WaardeFn#fn()} ({@code eval==null},
     *  no aggregation — restates inside scalarWaarde) OR an aggregation {@link Eval} ({@code fn==null}) reading its slice
     *  {@code [aggOff, aggOff+aggCnt)} of the consequence's aggsAll[], then §4-restated from {@code van} (the value type)
     *  into {@code naar} (the new object's attribuut unit). */
    private record TkLower(String attr, BiFunction<Object, Map<String, BigDecimal>, Object> fn, Eval eval,
                           int aggOff, int aggCnt, Datatype van, Datatype naar, boolean kenmerk) {}

    private static boolean isScalarSoort(ResolvedRegel.Soort s) {
        return s == ResolvedRegel.Soort.GELIJKSTELLING || s == ResolvedRegel.Soort.BEREKENING || s == ResolvedRegel.Soort.INITIALISATIE;
    }

    /** True when the value CONTAINS an aggregation Functie somewhere in its arithmetic tree (a BARE aggregation at the
     *  root is the separate {@link #isAggregatieWaarde} path) — the trigger for the embedded-aggregation value path. */
    private static boolean containsAggregatie(Expr e) {
        if (e instanceof Expr.Functie f) {
            // a §5.8.3 LISTED MIN/MAX/EERSTE/LAATSTE fold (≥2 operands) is NOT an instance aggregation — it lowers via
            // concatenatieFold on the scalar path; only recurse into its listed operands.
            if (isGelisteFold(f)) { for (Expr a : f.argumenten()) if (containsAggregatie(a)) return true; return false; }
            if (isAggregatieKind(f.kind())) return true;
            for (Expr a : f.argumenten()) if (containsAggregatie(a)) return true;
            return false;
        }
        if (e instanceof Expr.Binair b) return containsAggregatie(b.links()) || containsAggregatie(b.rechts());
        if (e instanceof Expr.Afgerond a) return containsAggregatie(a.basis());
        if (e instanceof Expr.Begrensd g) return containsAggregatie(g.basis());
        return false;
    }

    /** Recursively lower a value tree to an {@link Eval}, collecting each embedded aggregation as its own accumulate CE
     *  (accItem + result Variable, index-aligned with accVarsOut) and the Parameter-globals it reads. null -> out of
     *  scope (caller skips). Mirrors {@link #scalarWaarde}/{@link #scalarWrapper} for the scalar leaves, but threads
     *  aggregation result vars through the {@code aggResults[]} the consequence binds. A SOM (count+fallback two-var
     *  fold) inside arithmetic is not lowered here -> null (honest skip). */
    private Eval lowerVal(Expr e, Objecttype subject, Variable sVar, Set<String> globalsOut,
                          List<RuleItemBuilder<?>> accItemsOut, List<Variable> accVarsOut) {
        if (e instanceof Expr.Literal lit && lit.type().isNumeriek()) {
            BigDecimal val = bd(lit);
            return val == null ? null : (s, g, a) -> val;
        }
        if (e instanceof Expr.AttribuutRef ar && ongenavigeerd(ar)) {
            if (subject != null && Namen.bevatNaam(subject.granulariteitLeden(), ar.attribuut())) return null;   // temporal -> Phase F
            String attr = ar.attribuut();
            return (s, g, a) -> RegelSpraakRuntime.get(s, attr);
        }
        if (e instanceof Expr.ParameterRef pr) {
            String gn = globalNaam(pr.parameter().naam());
            globalsOut.add(gn);
            return (s, g, a) -> g.get(gn);
        }
        if (e instanceof Expr.Functie f && isAggregatieKind(f.kind())) {
            Agg agg = aggregatie(f, subject, sVar, false);   // value/arith side: bare fold
            if (agg == null) return null;
            if (agg.isSom()) {
                // §5.8.2 SOM inside arithmetic: ONE accumulate CE binds BOTH the contributor count and the sum;
                // the eval branches count==0 -> the leegwaarde fallback, else toBd(sum).
                accItemsOut.add(agg.accItem());
                int idxCnt = accVarsOut.size(); accVarsOut.add(agg.countVar());
                int idxSum = accVarsOut.size(); accVarsOut.add(agg.resultVar());
                WaardeFn fallback = agg.somFallback();
                return (s, g, a) -> ((Number) a[idxCnt]).longValue() == 0L
                        ? (fallback == null ? null : (BigDecimal) fallback.fn().apply(s, Map.of()))
                        : RegelSpraakRuntime.toBd(a[idxSum]);
            }
            int idx = accVarsOut.size();
            accItemsOut.add(agg.accItem());
            accVarsOut.add(agg.resultVar());
            return (s, g, a) -> RegelSpraakRuntime.toBd(a[idx]);
        }
        if (e instanceof Expr.Binair b && arithmeticOp(b.op())) {
            if (b.links().type().isDatum() || b.rechts().type().isDatum()) return null;   // datum arithmetic -> Phase F
            Eval l = lowerVal(b.links(), subject, sVar, globalsOut, accItemsOut, accVarsOut);
            Eval r = lowerVal(b.rechts(), subject, sVar, globalsOut, accItemsOut, accVarsOut);
            if (l == null || r == null) return null;
            Expr.BinOp op = b.op();
            if (op == Expr.BinOp.PLUS || op == Expr.BinOp.MIN || op == Expr.BinOp.VERMINDERD_MET) {
                Datatype rt = effType(b.rechts()), lt = effType(b.links());   // ch. 2.10: derived, never the repetition
                if (valueFactor(rt, lt) != null || exactHerschaalFactoren(rt, lt) != null) {   // a real conversion is due
                    Eval inner = r; r = (s, g, a) -> { Object x = inner.apply(s, g, a); return x == null ? null : herschaalWaarde(x, rt, lt); };
                }
            }
            Eval lf = l, rf = r;
            return (s, g, a) -> {
                Object lv = lf.apply(s, g, a), rv = rf.apply(s, g, a);
                java.math.MathContext mc = java.math.MathContext.DECIMAL128;   // mirror mvel infix +/-/* (DECIMAL128)
                return switch (op) {
                    case PLUS           -> RegelSpraakRuntime.rsNul(lv).add(RegelSpraakRuntime.rsNul(rv), mc);
                    case MIN            -> RegelSpraakRuntime.rsNul(lv).subtract(RegelSpraakRuntime.rsNul(rv), mc);
                    case VERMINDERD_MET -> RegelSpraakRuntime.rsVerminderd(lv, rv);
                    case MAAL           -> RegelSpraakRuntime.rsNul(lv).multiply(RegelSpraakRuntime.rsNul(rv), mc);
                    case GEDEELD_DOOR   -> RegelSpraakRuntime.rsGedeeld(lv, rv);
                    default             -> null;
                };
            };
        }
        if (e instanceof Expr.Afgerond af) {
            Eval base = lowerVal(af.basis(), subject, sVar, globalsOut, accItemsOut, accVarsOut);
            if (base == null) return null;
            int dec = af.decimalen();
            java.math.RoundingMode mode = afrondMode(af.modus());
            return (s, g, a) -> RegelSpraakRuntime.rsAfronding(base.apply(s, g, a), dec, mode);
        }
        if (e instanceof Expr.Functie f && f.kind() == Expr.FunctieKind.WORTEL && f.argumenten().size() == 1) {
            Eval rad = lowerVal(f.argumenten().get(0), subject, sVar, globalsOut, accItemsOut, accVarsOut);
            if (rad == null) return null;
            return (s, g, a) -> RegelSpraakRuntime.rsWortel(rad.apply(s, g, a));
        }
        return null;
    }

    /** Build the consequence binding {@code v} (subject, then optional global, then the accumulate result vars) and
     *  invoking {@code body} with the matched fact array. Switches on arity (Block2..Block10); null if too many vars. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private RuleItemBuilder<?> consequenceOf(Variable[] v, java.util.function.BiConsumer<org.drools.model.Drools, Object[]> body) {
        return switch (v.length) {
            case 1 -> DSL.on(v[0]).execute((Block2<org.drools.model.Drools, Object>) (d, a0) -> body.accept(d, new Object[]{a0}));
            case 2 -> DSL.on(v[0], v[1]).execute((Block3<org.drools.model.Drools, Object, Object>) (d, a0, a1) -> body.accept(d, new Object[]{a0, a1}));
            case 3 -> DSL.on(v[0], v[1], v[2]).execute((Block4<org.drools.model.Drools, Object, Object, Object>) (d, a0, a1, a2) -> body.accept(d, new Object[]{a0, a1, a2}));
            case 4 -> DSL.on(v[0], v[1], v[2], v[3]).execute((Block5<org.drools.model.Drools, Object, Object, Object, Object>) (d, a0, a1, a2, a3) -> body.accept(d, new Object[]{a0, a1, a2, a3}));
            case 5 -> DSL.on(v[0], v[1], v[2], v[3], v[4]).execute((Block6<org.drools.model.Drools, Object, Object, Object, Object, Object>) (d, a0, a1, a2, a3, a4) -> body.accept(d, new Object[]{a0, a1, a2, a3, a4}));
            case 6 -> DSL.on(v[0], v[1], v[2], v[3], v[4], v[5]).execute((Block7<org.drools.model.Drools, Object, Object, Object, Object, Object, Object>) (d, a0, a1, a2, a3, a4, a5) -> body.accept(d, new Object[]{a0, a1, a2, a3, a4, a5}));
            case 7 -> DSL.on(v[0], v[1], v[2], v[3], v[4], v[5], v[6]).execute((Block8<org.drools.model.Drools, Object, Object, Object, Object, Object, Object, Object>) (d, a0, a1, a2, a3, a4, a5, a6) -> body.accept(d, new Object[]{a0, a1, a2, a3, a4, a5, a6}));
            case 8 -> DSL.on(v[0], v[1], v[2], v[3], v[4], v[5], v[6], v[7]).execute((Block9<org.drools.model.Drools, Object, Object, Object, Object, Object, Object, Object, Object>) (d, a0, a1, a2, a3, a4, a5, a6, a7) -> body.accept(d, new Object[]{a0, a1, a2, a3, a4, a5, a6, a7}));
            case 9 -> DSL.on(v[0], v[1], v[2], v[3], v[4], v[5], v[6], v[7], v[8]).execute((Block10<org.drools.model.Drools, Object, Object, Object, Object, Object, Object, Object, Object, Object>) (d, a0, a1, a2, a3, a4, a5, a6, a7, a8) -> body.accept(d, new Object[]{a0, a1, a2, a3, a4, a5, a6, a7, a8}));
            default -> null;
        };
    }

    /** Extends {@link #scalarWaarde} with §13.4.17.25 afronding (an Afgerond wrapper) and §13.4.17.13 wortel (a scalar
     *  WORTEL Functie, leeg/negatief -> leeg) — the wortel ALWAYS sits under its mandatory afronding. */
    private WaardeFn scalarWrapper(Objecttype valueSubject, Expr e) {
        if (e instanceof Expr.Afgerond af) {
            WaardeFn base = scalarWaarde(valueSubject, af.basis());
            if (base == null) return null;
            BiFunction<Object, Map<String, BigDecimal>, Object> bfn = base.fn();
            int dec = af.decimalen();
            java.math.RoundingMode mode = afrondMode(af.modus());
            return new WaardeFn((s, gm) -> RegelSpraakRuntime.rsAfronding(bfn.apply(s, gm), dec, mode), base.globals());
        }
        if (e instanceof Expr.Functie f && f.kind() == Expr.FunctieKind.WORTEL && f.argumenten().size() == 1) {
            WaardeFn rad = scalarWaarde(valueSubject, f.argumenten().get(0));
            if (rad == null) return null;
            BiFunction<Object, Map<String, BigDecimal>, Object> rfn = rad.fn();
            return new WaardeFn((s, gm) -> {
                Object radicand = rfn.apply(s, gm);
                // §6.6: a negative radicand is the Fout the value side degrades to leeg — reported to the path
                // probe of the site's companion (a no-op without one, see Sonde). toBd reduces a Breuk exactly.
                BigDecimal getal = RegelSpraakRuntime.toBd(radicand);
                if (getal != null && getal.signum() < 0) Sonde.meld(gm);
                return RegelSpraakRuntime.rsWortel(radicand);
            }, rad.globals());
        }
        return null;
    }
    private static java.math.RoundingMode afrondMode(Expr.AfrondModus m) {
        return switch (m) {
            case NAAR_BENEDEN -> java.math.RoundingMode.FLOOR;
            case NAAR_BOVEN   -> java.math.RoundingMode.CEILING;
            case REKENKUNDIG  -> java.math.RoundingMode.HALF_UP;
            case RICHTING_NUL -> java.math.RoundingMode.DOWN;
            case WEG_VAN_NUL  -> java.math.RoundingMode.UP;
        };
    }

    /** §4 value-side unit conversion factor (van -> naar), or null when no conversion applies — the mirror of
     *  DrlTranspiler.omrekenFactor. Restates the value expression in the doel attribute's unit before assignment. */
    private static BigDecimal valueFactor(Datatype van, Datatype naar) {
        Eenheid ev = van.eenheidOf(), en = naar.eenheidOf();
        Eenheidsconflict.eisHerschaalbaar(ev, en, "een §4-waardeherschaling (enkelvoudige factor)");
        if (ev == null || en == null || !ev.convertibleNaar(en) || !ev.vereistOmrekening(en)) return null;
        return ev.omrekenfactorNaar(en);
    }

    /** Whether a cross-unit +/- between {@code a} and {@code b} can be restated EXACTLY (both SIMPLE convertible units
     *  with known integer factorNaarCanoniek) — the §4 multiply/divide canonical path (mirrors DRL herschaalConditiePlus).
     *  A COMPOSITE (rate) unit or an unknown factor is NOT exactly restatable here: the constraint side then SKIPS
     *  symmetrically with the DRL marker (see {@link #parityOnveilig}). Order-independent (convertibility is symmetric). */
    private static boolean eenheidExactHerschaalbaar(Datatype a, Datatype b) {
        Eenheid ea = a == null ? null : a.eenheidOf(), eb = b == null ? null : b.eenheidOf();
        return ea != null && eb != null && ea.convertibleNaar(eb) && ea.exacteHerschaalFactoren(eb) != null;
    }

    /** Whether a begrenzing {@code grens} (min/max) is parity-UNSAFE to restate into {@code basisType}'s unit inside a
     *  CONSTRAINT: it needs a unit restatement (a different convertible unit) that is NOT exact (an opaque composite —
     *  DRL markers it, the exec must skip symmetrically). A null/same-unit/exactly-restatable bound is safe (FIRES). */
    private boolean grensOnveilig(Expr grens, Datatype basisType) {
        if (grens == null || grens.type() == null || basisType == null) return false;
        Datatype gt = effType(grens);   // the DECLARED unit, never the node's repetition of it
        Eenheid eb = basisType.eenheidOf(), eg = gt.eenheidOf();
        return eb != null && eg != null && eb.vereistOmrekening(eg) && !eenheidExactHerschaalbaar(gt, basisType);
    }

    /** The EXACT canonical-unit restatement factors {fv, fn} to restate a value in {@code van} into {@code naar}
     *  (×fv ÷fn at DECIMAL64), or null when no exact restatement applies (no conversion due, or a rate/unknown-factor
     *  unit — the caller then falls back to the lossy {@link #valueFactor}). Mirrors DRL herschaalExact. */
    private static BigDecimal[] exactHerschaalFactoren(Datatype van, Datatype naar) {
        Eenheid ev = van.eenheidOf(), en = naar.eenheidOf();
        Eenheidsconflict.eisHerschaalbaar(ev, en, "een §4-waardeherschaling (exacte factoren)");
        if (ev == null || en == null || !ev.convertibleNaar(en) || !ev.vereistOmrekening(en)) return null;
        return ev.exacteHerschaalFactoren(en);   // {num, den}: simple OR rate (the inverted denominator) — exact ×÷
    }

    /** §4 restate a scalar value from {@code van} into {@code naar}: EXACT ×fv ÷fn (DECIMAL64) for simple convertible
     *  units (mirrors DRL herschaalExact / §7.3.1 rsTijdSchaal), the lossy single ratio for a rate, identity otherwise.
     *  A leeg (null) value stays leeg. */
    private static Object herschaalWaarde(Object v, Datatype van, Datatype naar) {
        if (v == null) return null;
        BigDecimal[] ex = exactHerschaalFactoren(van, naar);
        // A Breuk value keeps its exact restatement (×fv ÷fn as exact rationals); a BigDecimal value takes the
        // original DECIMAL64 path unchanged. The single-runtime rs* helpers already return Breuk/BigDecimal, and a
        // unit restatement must not force a premature decimal collapse of a non-terminating intermediate.
        if (v instanceof io.github.yhavinga.regelspraak.runtime.Breuk) {
            if (ex != null) return RegelSpraakRuntime.rsGedeeld(RegelSpraakRuntime.rsMaal(v, ex[0]), ex[1]);
            BigDecimal vf = valueFactor(van, naar);
            return vf == null ? v : RegelSpraakRuntime.rsMaal(v, vf);
        }
        BigDecimal bd = (BigDecimal) v;
        if (ex != null) return bd.multiply(ex[0]).divide(ex[1], java.math.MathContext.DECIMAL64);
        BigDecimal vf = valueFactor(van, naar);
        return vf == null ? bd : bd.multiply(vf);
    }

    // ---- small helpers ----
    private static BigDecimal num(Object bean, String attr) { return (BigDecimal) RegelSpraakRuntime.get(bean, attr); }
    /** Parse a numeric literal's value, ignoring any trailing unit ("18 jr" -> 18, "3,5 EUR" -> 3.5; comma is the
     *  RegelSpraak decimal mark), or null for a non-numeric literal. Reads the §13.2.5 number via the SHARED
     *  {@link Rationaal#van} parser — the same one {@link io.github.yhavinga.regelspraak.transpiler.DrlTranspiler} uses — so a
     *  rational form ("4/2" -> 2, "3/2" -> 1.5, "1_1/2" -> 1.5) lowers to its EXACT value when the quotient
     *  terminates, else to a DECIMAL64 division (e.g. 1/3). A leading-integer regex would drop the denominator
     *  ("4/2" -> 4) and DIVERGE from the DRL oracle (grondslag maal 4/2 = 40 in exec vs 20 in DRL). */
    private static BigDecimal bd(Expr.Literal lit) {
        return Literalen.getal(lit);   // the shared literal reader (single shared implementation)
    }
    /** §13.2.5/§389 a numeric literal as an EXACT value: a §13.2.16 rational whose quotient does NOT terminate
     *  (1/3) becomes a {@link io.github.yhavinga.regelspraak.runtime.Breuk} (teller/noemer), so subsequent arithmetic keeps its
     *  precision; a terminating literal (4/2, 3/2, 2,5) stays a plain BigDecimal (the fast path, pins unchanged).
     *  null for a non-numeric literal. */
    private static Object numeriekLit(Expr.Literal lit) {
        BigDecimal[] r = Rationaal.van(lit.waarde());
        if (r == null) return null;
        io.github.yhavinga.regelspraak.runtime.Breuk b = io.github.yhavinga.regelspraak.runtime.Breuk.van(r[0]).delen(io.github.yhavinga.regelspraak.runtime.Breuk.van(r[1]));
        return b.isTerminerend() ? b.naarDecimal() : b;
    }
    /** A numeric EXPRESSION operand (an arithmetic {@link Expr.Binair}, a builtin {@link Expr.Functie}, a §6.1.4
     *  begrenzing or a §13.4.17.25 afronding) — i.e. NOT a plain attribute/literal/parameter leaf. Used to route an
     *  expr-vs-attr / expr-vs-expr comparison through the {@link #scalarWaarde} value-path lowering on both sides. */
    private static boolean numeriekeExpressie(Expr e) {
        // a §5.3 Rekenjaar constant counts as a numeric expression so a bare "Rekenjaar groter is dan 2020"
        // routes to the vs-global branch (it binds the rekendatum pseudo-global; the fact-only leaf skips it).
        return e instanceof Expr.Binair || e instanceof Expr.Functie || e instanceof Expr.Begrensd
                || e instanceof Expr.Afgerond || Expr.isRekenConstante(e);
    }
    /** An operand admissible to a NUMERIC comparison branch: a genuinely numeric/percentage type, OR a numeric-domain
     *  arithmetic EXPRESSION the resolver could not unit-type (Onbekend, e.g. a §4 composite/rate unit from Bedrag×km) —
     *  scalarWaarde still lowers it to a BigDecimal. Datum expressions are typed isDatum (NOT Onbekend), so they are
     *  excluded; a Tekst/Boolean is not a numeriekeExpressie. Mirrors DRL, which structurally descends with no isNumeriek gate. */
    private static boolean getalachtigVergelijkbaar(Expr e) {
        return e != null && e.type() != null
            && (e.type().isNumeriek() || (e.type().isOnbekend() && numeriekeExpressie(e)));
    }
    /** Whether an expression operand contains a construct the DRL CONDITION path MARKERS (so the exec must also skip, to
     *  stay symmetric) or that restates LOSSILY: a §3.6 DimensieCel (DrlTranspiler.operand markers a dimensioned read in a
     *  constraint — review F1), or a CROSS-UNIT +/- (DrlTranspiler.omrekenConditie markers it; the right operand restated
     *  into the left's unit truncates a non-terminating ratio mnd→jr = 1/12, a wrong firing — review F2). MAAL/GEDEELD do
     *  NOT restate units (§6.4), so they are unit-safe. Recurses through arithmetic / begrenzing / afronding / functie args. */
    private boolean parityOnveilig(Expr e) {
        if (e instanceof Expr.DimensieCel) return true;
        if (e instanceof Expr.Binair b) {
            if (b.op() == Expr.BinOp.PLUS || b.op() == Expr.BinOp.MIN || b.op() == Expr.BinOp.VERMINDERD_MET) {
                Eenheid el = eff.eenheidVan(b.links());
                Eenheid er = eff.eenheidVan(b.rechts());
                // A SIMPLE convertible cross-unit +/- now restates EXACTLY (×÷ canonical) and FIRES at parity with DRL's
                // rsHerschaalExact; only a COMPOSITE (rate) / unknown-factor cross-unit +/- still restates lossily -> the
                // exec keeps the symmetric skip (DRL herschaalConditiePlus markers it).
                if (el != null && er != null && el.vereistOmrekening(er)
                        && !eenheidExactHerschaalbaar(effType(b.links()), effType(b.rechts()))) return true;
            }
            return parityOnveilig(b.links()) || parityOnveilig(b.rechts());
        }
        if (e instanceof Expr.Begrensd bg) {
            // A CROSS-UNIT begrenzing BOUND (a min/max unit ≠ the basis unit) is now restated EXACTLY into the basis
            // unit by BOTH back-ends — DRL herschaalConditie -> rsHerschaalExact, exec scalarWaarde's .herschaald —
            // (×÷ the integer factorNaarCanoniek, one DECIMAL64 rounding, like the +/- path), so the begrenzing
            // CONSTRAINT FIRES at parity. Only an OPAQUE composite bound (no exact factors) would restate lossily and
            // must SKIP symmetrically with DRL's marker — but two such units are never inter-convertible, so a
            // convertible cross-unit bound never hits that branch (grensOnveilig). The VALUE-side begrenzing is
            // unaffected (it never reaches parityOnveilig).
            Datatype bt = effType(bg.basis());
            return grensOnveilig(bg.minimum(), bt) || grensOnveilig(bg.maximum(), bt)
                    || parityOnveilig(bg.basis())
                    || (bg.minimum() != null && parityOnveilig(bg.minimum())) || (bg.maximum() != null && parityOnveilig(bg.maximum()));
        }
        if (e instanceof Expr.Afgerond af) return parityOnveilig(af.basis());
        if (e instanceof Expr.Functie f) return f.argumenten().stream().anyMatch(this::parityOnveilig);
        return false;
    }
    /** The six numeric comparison operators the model lowers; EERDER/LATER (datum) are a later phase (caller skips). */
    private static boolean isNumeriekOp(Conditie.Operator op) {
        return switch (op) {
            case KLEINER, KLEINER_OF_GELIJK, GROTER, GROTER_OF_GELIJK, GELIJK, ONGELIJK -> true;
            default -> false;
        };
    }
    /** Compare two (already unit-restated) numbers with the SAME null semantics the DRL/mvel constraint has —
     *  the subtle case being {@code !=}: mvel reads {@code null != 18} as TRUE (a leeg value DIFFERS from a number),
     *  so ONGELIJK must be true when exactly one operand is leeg, while {@code ==} is false there and BOTH-leeg makes
     *  {@code ==} true / {@code !=} false. The ordering ops do NOT match when either operand is leeg (mvel returns
     *  false, no NPE). Getting this wrong flips firing decisions under the negated quantifiers (geen/precies/ten hoogste). */
    /** Whether a value expression can compute to an exact {@link io.github.yhavinga.regelspraak.runtime.Breuk} — the trigger for
     *  routing a numeric comparison through the Breuk-aware rsAggVergelijk (mirrors DrlTranspiler.kanBreukOpleveren).
     *  A KNOWN non-numeric result (datum/text) is never a Breuk; arithmetic / begrenzing / afronding / abs / a
     *  NON-terminating rational literal can be. */
    private boolean kanBreukOpleveren(Expr e) {
        if (e == null) return false;
        // ch. 4.10: descend THROUGH a Let (and an IfThenElse) first. Neither matches the concrete
        // pattern matches below, so a Breuk-producing division behind a Let read "cannot be a Breuk" and the
        // caller took the (BigDecimal) cast path — which THROWS a ConstraintEvaluationException on the Breuk
        // the value side actually computes (an unexplained crash, not a refusal). See letKinderen.
        for (Expr k : letKinderen(e)) if (kanBreukOpleveren(k)) return true;
        if (e instanceof Expr.Binair b) {
            if (b.op() == Expr.BinOp.GEDEELD_DOOR || b.op() == Expr.BinOp.PERCENTAGE_VAN) return true;
            return kanBreukOpleveren(b.links()) || kanBreukOpleveren(b.rechts());
        }
        if (e instanceof Expr.Begrensd bg) return kanBreukOpleveren(bg.basis()) || kanBreukOpleveren(bg.minimum()) || kanBreukOpleveren(bg.maximum());
        if (e instanceof Expr.Afgerond) return false;   // an afronding collapses any Breuk to a BigDecimal
        if (e instanceof Expr.Functie f) { for (Expr a : f.argumenten()) if (kanBreukOpleveren(a)) return true; return false; }
        if (e instanceof Expr.Literal lit && lit.type() != null && lit.type().isNumeriek()) {
            BigDecimal[] r = Rationaal.van(lit.waarde());
            if (r == null) return false;
            try { r[0].divide(r[1]); return false; }
            catch (ArithmeticException nietEindig) { return true; }
        }
        return false;
    }

    private static boolean vergelijk(BigDecimal a, BigDecimal b, Conditie.Operator op) {
        boolean aNull = a == null, bNull = b == null;
        return switch (op) {
            case GELIJK            -> aNull || bNull ? (aNull && bNull) : a.compareTo(b) == 0;
            case ONGELIJK          -> aNull || bNull ? !(aNull && bNull) : a.compareTo(b) != 0;
            case KLEINER           -> !aNull && !bNull && a.compareTo(b) < 0;
            case KLEINER_OF_GELIJK -> !aNull && !bNull && a.compareTo(b) <= 0;
            case GROTER            -> !aNull && !bNull && a.compareTo(b) > 0;
            case GROTER_OF_GELIJK  -> !aNull && !bNull && a.compareTo(b) >= 0;
            default                -> false;  // EERDER/LATER — never reached (isNumeriekOp gates the caller)
        };
    }
    /** True iff both datatypes are the SAME datum subtype (both §13.2.19 Datum-in-dagen, or both §13.2.21 Datum-en-tijd).
     *  A mixed pair is a §5.3 Tabel 36 type mismatch — the comparison must SKIP, never lossily coerce one to the other. */
    private static boolean sameDatumKind(Datatype a, Datatype b) {
        if (a instanceof Datatype.DatumTijd) return b instanceof Datatype.DatumTijd;
        return a instanceof Datatype.DatumInDagen && b instanceof Datatype.DatumInDagen;
    }
    /** Parse a datum/datum-tijd literal to its Temporal: a §13.2.21 Datum-en-tijd literal -> LocalDateTime, else a
     *  §13.2.19 Datum-in-dagen literal -> LocalDate. null when the literal lacks the parts the parser requires. */
    private static Object datumLiteraal(Expr.Literal lit) {
        return lit.type() instanceof Datatype.DatumTijd ? parseDatumTijd(lit.waarde()) : parseDatum(lit.waarde());
    }
    /** Narrow a reflectively-read value to a date/datetime Temporal (LocalDate or LocalDateTime), else null (leegwaarde). */
    private static Object datumWaarde(Object v) {
        return (v instanceof java.time.LocalDate || v instanceof java.time.LocalDateTime) ? v : null;
    }
    /** A reader for an OWN or to-ONE NAVIGATED datum/datum-tijd attribute as its raw {@link java.time.temporal.Temporal}
     *  (LocalDate for §13.2.19 Datum-in-dagen, LocalDateTime for §13.2.21 Datum-en-tijd; null = leegwaarde): an absent
     *  navigation reads leeg (§5.5.5), a present relation reads the attribute (which may itself be leeg). null when the
     *  attribute is temporal (Phase F), §3.6 gedimensioneerd, not the subject's own member, or an unresolvable / to-many
     *  navigation. The Temporal generalisation of the old LocalDate-only reader (which read a present Datum-en-tijd as
     *  leeg -> a wrong/no fire). Feeds {@link #datumVergelijkTemporal}, which encodes the §5 leegwaarde tables for any
     *  Temporal (ordering EERDER/LATER, equality GELIJK Tabel 38 / ONGELIJK Tabel 44). */
    private java.util.function.Function<Object, Object> datumTemporeelLezer(Expr.AttribuutRef a, Objecttype subject) {
        if (a.type() == null || !a.type().isDatum()) return null;
        if (a.objecttype() == null || Namen.bevatNaam(a.objecttype().granulariteitLeden(), a.attribuut())) return null;   // temporal -> Phase F
        if (!a.objecttype().dimensiesVan(a.attribuut()).isEmpty()) return null;                                        // §3.6 -> marker
        String attr = a.attribuut();
        if (ongenavigeerd(a)) {
            if (subject == null || !subject.heeftLid(attr)) return null;
            return s -> datumWaarde(RegelSpraakRuntime.get(s, attr));
        }
        String rol = navComparisonRole(a, subject);
        if (rol == null) return null;
        return s -> { Object rel = RegelSpraakRuntime.get(s, rol); return rel == null ? null : datumWaarde(RegelSpraakRuntime.get(rel, attr)); };
    }
    /** §5.1 Tabel 32: the multipliers that restate the left/right comparison operands into a common unit before
     *  comparing (so "24 mnd > 2 jr" compares 24 vs 24, not 24 vs 2) — the BigDecimal mirror of the DRL oracle's
     *  {@code vergelijkingsOperanden}: no conversion when either side is unitless / not convertible / already the
     *  same unit; both sides to their canonical unit for simple units; the rate (noemer) unit restates only the
     *  right in the left's unit. The factors are the DECLARED exact factors, so the multiply is lossless. */
    private static BigDecimal[] restatement(Datatype lt, Datatype rt) {
        Eenheid el = lt.eenheidOf(), er = rt.eenheidOf();
        Eenheidsconflict.eisHerschaalbaar(el, er, "een §5.1 vergelijking");
        if (el == null || er == null || !el.convertibleNaar(er) || !el.vereistOmrekening(er))
            return new BigDecimal[]{BigDecimal.ONE, BigDecimal.ONE};
        if (el.noemer() == null && er.noemer() == null)
            return new BigDecimal[]{nz(el.factorNaarCanoniek()), nz(er.factorNaarCanoniek())};
        return new BigDecimal[]{BigDecimal.ONE, nz(er.omrekenfactorNaar(el))};  // composite (rate) unit: restate rhs in lhs unit
    }
    private static BigDecimal nz(BigDecimal f) { return f == null ? BigDecimal.ONE : f; }
    /** Restate one operand by its unit factor; a leeg (null) operand stays leeg (no NPE on .multiply). */
    private static BigDecimal scale(BigDecimal x, BigDecimal f) { return x == null ? null : x.multiply(f); }
    /** §8.1.1 datum comparison over ANY Temporal (LocalDate or LocalDateTime), for a date-arithmetic EXPRESSION
     *  operand whose runtime type the caller does not pin. Mirrors DRL's mvel rendering exactly: ORDERING
     *  (EERDER/LATER) compares two present operands via {@link Comparable#compareTo}, a leeg operand is the no-match
     *  (false); GELIJK is {@code a==b && !(both null)} (two leeg dates are NOT equal); ONGELIJK is {@code a!=b ||
     *  both null} (mvel's null-asymmetric {@code !=}). */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static boolean datumVergelijkTemporal(Object a, Object b, Conditie.Operator op) {
        switch (op) {
            // §5.3 Tabel 38 (datum gelijk): both-leeg is "fout" -> onwaar as a guard; one-leeg -> onwaar; else .equals.
            case GELIJK:   return java.util.Objects.equals(a, b) && !(a == null && b == null);
            // §5.6 Tabel 44 (ongelijk): exactly-one-leeg -> WAAR, BOTH-leeg -> onwaar (NOT waar — mvel `null != null` is
            // false too). !Objects.equals gives exactly that: equals(null,null)=true -> !true=false; equals(null,x)=false
            // -> !false=true. (An earlier `|| (a==null && b==null)` wrongly fired both-leeg ONGELIJK -> diverged from DRL.)
            case ONGELIJK: return !java.util.Objects.equals(a, b);
            default: break;
        }
        if (!(a instanceof Comparable) || b == null) return false;   // ordering: a leeg operand -> no-match (mvel)
        int c = ((Comparable) a).compareTo(b);
        return switch (op) {
            case EERDER           -> c < 0;
            case EERDER_OF_GELIJK -> c <= 0;
            case LATER            -> c > 0;
            case LATER_OF_GELIJK  -> c >= 0;
            default               -> false;
        };
    }
    /** The datum comparison ops the {@link #datumVergelijkTemporal} date-expression branch covers: the EERDER/LATER
     *  ordering family plus GELIJK/ONGELIJK (date equality). */
    private static boolean isDatumVergelijkOp(Conditie.Operator op) {
        return isDatumOp(op) || op == Conditie.Operator.GELIJK || op == Conditie.Operator.ONGELIJK;
    }
    /** True for a §6/§13.4.16 date-arithmetic Binair (a +/- whose result is a datum because one operand is a datum):
     *  the operand form the leafPredicate datum branch handles via scalarWaarde rather than a bare attribute/literal. */
    private static boolean isDatumRekenExpr(Expr e) {
        return e instanceof Expr.Binair b && (b.links().type().isDatum() || b.rechts().type().isDatum());
    }

    // ---------------- §9.5 / §13.4.8 consistentie (Phase D) ----------------
    /** §13.4.8 consistentieregel: the assertie MUST hold, so the rule fires on its VIOLATION (the negated
     *  assertie) and INSERTS an Inconsistentie(naam, subject). Insert-only -> NO no-loop; an idempotency guard
     *  (not Inconsistentie(regel==naam, subject==$s)) keeps it from re-flagging an already-flagged subject.
     *  Mirrors DrlTranspiler.consistentie + schendingConstraint. null -> skip (no faithful violation predicate). */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private Rule consistentie(io.github.yhavinga.regelspraak.ir.ResolvedConsistentie c) {
        Objecttype subject = c.subject();
        if (subject == null) return skip("consistentie (geen subject): " + c.naam());
        Class<?> factClass = factClass(subject);
        if (factClass == null) return skip("consistentie (geen fact-class " + subject.naam + "): " + c.naam());
        Class<?> incClass = inconsistentieClass();
        if (incClass == null) return skip("consistentie (geen Inconsistentie-fact): " + c.naam());
        seq = 0;
        String naam = c.naam();
        String fqcn = FACTS + ".Inconsistentie";
        Variable sVar = DSL.declarationOf(factClass, "$subject");
        PatternDSL.PatternDef pattern;
        Predicate1<Object> schending = schending(c.assertie(), subject);
        if (schending != null) {
            pattern = PatternDSL.pattern(sVar).expr(nextId("schending"), (Predicate1) schending);
        } else {
            // §9.5 a vs-PARAMETER assertie ("X moet kleiner zijn dan de <parameter>") needs the global bound as a
            // pattern variable (a 2-var expr) — schending returns a fact-only Predicate1, so it cannot carry it.
            SchendingGlobal sg = schendingMetGlobal(c.assertie(), subject);
            if (sg == null) return skip("consistentie (" + schendingReden(c.assertie(), subject) + "): " + c.naam());
            Global<BigDecimal> g = globals.get(sg.global());
            java.util.function.BiPredicate<Object, BigDecimal> pred = sg.pred();
            pattern = PatternDSL.pattern(sVar).expr(nextId("schending"), (Variable) g,
                    (Predicate2) (s, gv) -> pred.test(s, (BigDecimal) gv));
        }
        pattern = geldigGuard(pattern, c.geldigheid());   // §4.2 window
        // idempotency guard: not Inconsistentie( regel == naam, subject == $subject )
        Variable incVar = DSL.declarationOf(incClass, "$inc_" + (seq++));
        PatternDSL.PatternDef guard = PatternDSL.pattern(incVar)
                .expr(nextId("ig"), (Predicate1) i -> naam.equals(RegelSpraakRuntime.get(i, "regel")))
                .expr(nextId("ig"), (Variable) sVar, (Predicate2) (i, s) -> RegelSpraakRuntime.get(i, "subject") == s);
        Block2<org.drools.model.Drools, Object> body = (drools, s) ->
                drools.insert(RegelSpraakRuntime.newInconsistentie(fqcn, naam, s));
        return PatternDSL.rule(kop(naam)).build(pattern, DSL.not(guard), DSL.on(sVar).execute(body));
    }

    /** The §9.5 VIOLATION predicate of an assertie: the negation that fires the Inconsistentie. Phase D scope = a
     *  single own-attribute numeric comparison (vs another own attribute or a numeric literal), unit-restated:
     *  equality (gelijk/ongelijk) flips the operator (vergelijk's mvel null semantics make an empty operand a
     *  violation of "gelijk aan", per typeringen Tabel 37); an ordering comparison is ALSO violated when either
     *  operand is leeg (the typeringen leegwaarde tables make it onwaar -> inconsistent). null -> skip. */
    private Predicate1<Object> schending(Conditie assertie, Objecttype subject) {
        // §13.4.8: "alle criteria moeten gelden" -> inconsistent when ANY criterion is violated (OR the schendingen).
        if (assertie instanceof Conditie.Samengesteld s && s.kwantor() == Conditie.Kwantor.ALLE) {
            List<Predicate1<Object>> schendingen = new ArrayList<>();
            for (Conditie deel : s.onderdelen()) {
                Predicate1<Object> sp = schending(deel, subject);
                if (sp == null) return null;                                // an un-negatable criterion no-ops the rule
                schendingen.add(sp);
            }
            if (schendingen.isEmpty()) return null;
            return obj -> { for (Predicate1<Object> sp : schendingen) if (sp.test(obj)) return true; return false; };
        }
        // §8.2 a whole-period-qualified criterium ("X moet gedurende het gehele jaar gelijk zijn aan Y"): the criterium
        // is satisfied iff the §8.2 check holds, so the VIOLATION is its negation. A literal/own-attr grens (0 globals)
        // is fact-only; a Parameter-global grens is handled by schendingMetGlobal. Symmetric with DrlTranspiler.
        Conditie.GeheelPeriode gp82 = geheelPeriodeVan(assertie);
        if (gp82 != null) {
            Predicate1<Object> hold = geheelPeriodeLeaf(geheelPeriode82(assertie, gp82, subject));
            return hold == null ? null : s -> !hold.test(s);
        }
        // §9.5 a LEEG/GEVULD assertie criterion ("X moet gevuld/leeg zijn", also as a nested ALLE criterion): the
        // violation is its negation — a GEVULD assertie is violated when the operand is leeg, a LEEG one when it is
        // gevuld. Own or to-one navigated operand (consistentieReader; navReader's null == leeg). Mirrors negeer(LEEG/
        // GEVULD) + DRL's genavigeerdLeegGevuld (absent reference reads leeg).
        if (assertie instanceof Conditie.Predicaat p
                && (p.soort() == Conditie.Predicaatsoort.LEEG || p.soort() == Conditie.Predicaatsoort.GEVULD)
                && p.operand() instanceof Expr.AttribuutRef pa) {
            java.util.function.Function<Object, Object> read = consistentieReader(pa, subject);
            if (read == null) return null;
            boolean assertGevuld = p.soort() == Conditie.Predicaatsoort.GEVULD;
            return s -> (read.apply(s) == null) == assertGevuld;   // leeg violates GEVULD; gevuld violates LEEG
        }
        // §9.5 a KENMERK assertie ("elke Penning is uitgereikt"): the violation is EXACTLY its negation. A mark is
        // TWO-VALUED under the closed-world reading the whole back-end applies (§13.4.6.4: an absent
        // kenmerkinitialisatie IS onwaar; the bean field is a primitive boolean with no leeg state), so unlike a
        // numeric ordering — where Tabel 37/38's leeg flank makes BOTH the criterion and its negation a violation,
        // which is why those arms are written out longhand below — the kenmerkcheck has no third flank and
        // !leafPredicate IS the schending. Reusing the §8.1.8 condition lowering itself (own or to-one navigated,
        // negatie honoured, temporal refused) keeps assertion and condition from ever drifting apart.
        if (assertie instanceof Conditie.Kenmerk kc) {
            if (kc.periode() != null) return null;                           // §8.2-qualified -> the geheelPeriode arm above
            Predicate1<Object> houdt = leafPredicate(kc, subject);
            return houdt == null ? null : s -> !houdt.test(s);
        }
        if (!(assertie instanceof Conditie.Vergelijking v)) return null;     // other compound: later
        // §9.5 an AANTAL-over-a-role assertie ("het aantal houder van de Penning is groter of gelijk aan 1"): the
        // count of a TO-ONE role is its PRESENCE count — 0 or 1 — read straight off the subject's own scalar
        // reference field. Checked before the AttribuutRef-LHS forms below, which this LHS is not.
        if (v.links() instanceof Expr.Functie af && af.kind() == Expr.FunctieKind.AANTAL
                && isNumeriekOp(v.op()) && v.rechts() instanceof Expr.Literal cnt && cnt.type() != null
                && cnt.type().isNumeriek()) {
            java.util.function.Function<Object, BigDecimal> telling = toOneAantalReader(af, subject).lezer();
            BigDecimal grens = bd(cnt);
            if (telling != null && grens != null) {
                // the count is never leeg (0 is a count, not an absence), so the violation is the plain negation:
                // vergelijk against the FLIPPED operator. No Tabel-37 leeg flank applies.
                Conditie.Operator flipA = Operatoren.flip(v.op());
                return s -> vergelijk(telling.apply(s), grens, flipA);
            }
        }
        // §13.4.8 a DATUM comparison assertie where an operand is a §6/§13.4.16 date-arithmetic EXPRESSION ("X moet
        // later zijn dan Y plus Z"): the violation is the negation of the assertion. datumVergelijkTemporal already
        // encodes mvel's leeg/ordering/equality semantics, so !it IS the schending (either operand leeg, or the
        // flipped comparison) — matching DRL's negated constraint. (Checked before the bare-AttribuutRef-LHS forms.)
        if (isDatumVergelijkOp(v.op()) && v.links().type().isDatum() && v.rechts().type().isDatum()
                && (isDatumRekenExpr(v.links()) || isDatumRekenExpr(v.rechts()))) {
            WaardeFn lw = scalarWaarde(null, v.links()), rw = scalarWaarde(null, v.rechts());
            if (lw == null || rw == null || !lw.globals().isEmpty() || !rw.globals().isEmpty()) return null;
            BiFunction<Object, Map<String, BigDecimal>, Object> lfn = lw.fn(), rfn = rw.fn();
            Conditie.Operator op = v.op();
            return s -> !datumVergelijkTemporal(lfn.apply(s, Map.of()), rfn.apply(s, Map.of()), op);
        }
        if (!(v.links() instanceof Expr.AttribuutRef la)) return null;
        // OWN or to-ONE NAVIGATED operand readers (navReader's null == DRL's `l.leeg` = rol==null || attr==null, so
        // the explicit leeg/vol semantics of DrlTranspiler.genavigeerdeVergelijkingSchending fall out of a null check).
        java.util.function.Function<Object, Object> lRead = consistentieReader(la, subject);
        if (lRead == null) return null;

        // §13.4.8 DATUM ordering assertie ("X moet eerder/later zijn dan Y"): violated when either operand is empty
        // OR the flipped (negated) ordering holds (own or navigated operands).
        if (la.type().isDatum() && isDatumOp(v.op())) {
            Conditie.Operator flipOp = Operatoren.flip(v.op());
            // violated when EITHER operand is empty OR the flipped (negated) ordering holds. Read each as its raw Temporal
            // (LocalDate OR LocalDateTime) — the old `instanceof LocalDate` read a present Datum-en-tijd as empty, wrongly
            // flagging a satisfied datum-tijd ordering as a violation (a masked exec over-flag). datumVergelijkTemporal
            // compares any Temporal. A mixed datum/datum-tijd pair is a §5.3 Tabel 36 mismatch -> skip.
            if (v.rechts() instanceof Expr.Literal dl && dl.type().isDatum()) {
                if (!sameDatumKind(la.type(), dl.type())) return null;
                Object rhs = datumLiteraal(dl);
                if (rhs == null) return null;
                return s -> { Object a = datumWaarde(lRead.apply(s));
                    return a == null || datumVergelijkTemporal(a, rhs, flipOp); };
            }
            if (v.rechts() instanceof Expr.AttribuutRef ra && ra.type().isDatum()) {
                if (!sameDatumKind(la.type(), ra.type())) return null;
                java.util.function.Function<Object, Object> rRead = consistentieReader(ra, subject);
                if (rRead == null) return null;
                return s -> { Object a = datumWaarde(lRead.apply(s)), b = datumWaarde(rRead.apply(s));
                    return a == null || b == null || datumVergelijkTemporal(a, b, flipOp); };
            }
            return null;
        }
        // §13.4.8 / typeringen Tabel 38 a NON-numeriek (Tekst / §13.2.14 enumeratiewaarde) equality assertie vs a
        // literal ("X moet gelijk/ongelijk zijn aan 'lit'"): the criterion is satisfied when (attr equals the label)
        // == (op is GELIJK); a violation otherwise. A leeg operand never equals the non-null literal, so a GELIJK
        // assertie over a leeg operand IS a violation (Tabel 38). Own or navigated LHS.
        if ((v.op() == Conditie.Operator.GELIJK || v.op() == Conditie.Operator.ONGELIJK)
                && v.rechts() instanceof Expr.Literal tl && Literalen.tekstOfEnumLabel(tl) != null) {
            java.util.function.UnaryOperator<Object> n = gelijkheidsLezing(la.type(), tl.type());
            Object rhs = n.apply(Literalen.tekstOfEnumLabel(tl));
            boolean assertGelijk = v.op() == Conditie.Operator.GELIJK;
            return s -> rhs.equals(n.apply(lRead.apply(s))) != assertGelijk;   // violation = criterion not satisfied
        }
        // §13.4.8 / typeringen Tabel 38 (GELIJK) + Tabel 44 (ONGELIJK): a NON-numeriek (Tekst / §13.2.14
        // enumeratiewaarde / Booleaan) equality assertie where v.rechts() is ALSO an ATTRIBUTE (attr-vs-attr, own or
        // navigated). DATUM equality is handled by the ordering/equality branch above (isDatumOp GELIJK/ONGELIJK), so
        // exclude it here. The satisfied/violated truth table is derived EXACTLY from DrlTranspiler's emitted mvel
        // constraints (genavigeerdeVergelijkingSchending L563-573, and the voorwaarde complement L2958-2966): mvel `==`
        // is VALUE equality and `null == null` is WAAR, so GELIJK is WAAR only when BOTH present and equal (both-leeg or
        // one-leeg -> onwaar/fout -> a VIOLATION of "moet gelijk"); ONGELIJK is WAAR for exactly-one-leeg OR both-present-
        // unequal (both-leeg -> onwaar -> a VIOLATION of "moet ongelijk"). See tekstAttrGelijk below.
        if ((v.op() == Conditie.Operator.GELIJK || v.op() == Conditie.Operator.ONGELIJK)
                && v.rechts() instanceof Expr.AttribuutRef ra && ra.type() != null
                && !ra.type().isNumeriek() && !ra.type().isPercentage()
                && la.type() != null && !la.type().isNumeriek() && !la.type().isPercentage()
                && !la.type().isDatum()) {   // datum handled by the ordering/equality branches above; this is Tekst/enum/boolean
            java.util.function.Function<Object, Object> rRead = consistentieReader(ra, subject);
            if (rRead == null) return null;
            boolean assertGelijk = v.op() == Conditie.Operator.GELIJK;
            java.util.function.UnaryOperator<Object> n = gelijkheidsLezing(la.type(), ra.type());
            return s -> !tekstAttrGelijk(n.apply(lRead.apply(s)), n.apply(rRead.apply(s)), assertGelijk);   // violation = criterion not satisfied
        }

        if (!isNumeriekOp(v.op())) return null;                              // remaining non-numeric op (EERDER/LATER datum): later
        // §5.3 a GELIJK/ONGELIJK assertie over NON-numeric operands (Tekst/datum/enum/boolean — attr-vs-attr or navigated-
        // vs-attr) reaching the toBd() coercion below would call RegelSpraakRuntime.toBd on a non-number and CRASH the whole
        // session at fire time (a leeg/String value -> IllegalStateException -> ConstraintEvaluationException). Skip it ->
        // an HONEST exec TODO (overgeslagen), never a crash (DRL flags it via the non-numeric schending form, so it is a
        // masked DRL-flags/exec-skips, not a silent divergence). The Tekst/enum vs-LITERAL consistentie is handled above;
        // this catches the attr-vs-attr / navigated-vs-attr Tekst/datum-equality form.
        if (la.type() != null && !la.type().isNumeriek() && !la.type().isPercentage()) return null;
        Conditie.Operator flip = Operatoren.flip(v.op());
        boolean gelijkheid = v.op() == Conditie.Operator.GELIJK || v.op() == Conditie.Operator.ONGELIJK;
        if (v.rechts() instanceof Expr.AttribuutRef ra) {
            java.util.function.Function<Object, Object> rRead = consistentieReader(ra, subject);
            if (rRead == null) return null;
            BigDecimal[] f = restatement(effType(la), effType(ra));
            return s -> {
                BigDecimal a = scale(RegelSpraakRuntime.toBd(lRead.apply(s)), f[0]), b = scale(RegelSpraakRuntime.toBd(rRead.apply(s)), f[1]);
                return gelijkheid ? vergelijk(a, b, flip) : (a == null || b == null || vergelijk(a, b, flip));
            };
        }
        if (v.rechts() instanceof Expr.Literal lit && lit.type().isNumeriek()) {
            BigDecimal rhs = bd(lit);
            if (rhs == null) return null;
            BigDecimal[] f = restatement(effType(la), effType(lit));
            BigDecimal rhsS = scale(rhs, f[1]);
            return s -> {
                BigDecimal a = scale(RegelSpraakRuntime.toBd(lRead.apply(s)), f[0]);
                return gelijkheid ? vergelijk(a, rhsS, flip) : (a == null || rhsS == null || vergelijk(a, rhsS, flip));
            };
        }
        return null;
    }

    /** Why a §9.5 assertie did not lower: the generic Phase-D scope note, SHARPENED for the AANTAL shapes, whose
     *  refusal is a §13.3.9 bean-MODEL limit rather than an arm nobody has built yet — the difference matters to
     *  whoever reads the gate's exclusion line and decides what to card next. */
    private String schendingReden(Conditie assertie, Objecttype subject) {
        if (assertie instanceof Conditie.Vergelijking v && v.links() instanceof Expr.Functie af
                && af.kind() == Expr.FunctieKind.AANTAL) {
            String reden = toOneAantalReader(af, subject).reden();
            if (reden != null) return reden;
        }
        return "schending buiten Phase-D scope";
    }

    /** A §9.5 consistentie violation predicate that reads a Parameter-GLOBAL: the global name + a {@code (subject,
     *  globalValue) -> violated} test. */
    private record SchendingGlobal(String global, java.util.function.BiPredicate<Object, BigDecimal> pred) {}

    /** §9.5 the violation predicate of a single numeric comparison assertie whose RHS is a scalar Parameter ("X moet
     *  kleiner zijn dan de volwassenleeftijd") — the global is bound as a pattern variable by the caller. Own or
     *  navigated LHS (consistentieReader); null when not this shape. The leeg/violation semantics match the own-attr
     *  numeric schending (an ordering with an empty operand is a violation). */
    private SchendingGlobal schendingMetGlobal(Conditie assertie, Objecttype subject) {
        // §8.2 a whole-period-qualified criterium whose grens reads EXACTLY ONE Parameter-global ("X moet gedurende
        // het gehele jaar gelijk zijn aan <de parameter>"): the global binds as a pattern variable, the violation is
        // !(§8.2 check holds). Mirrors DrlTranspiler.schendingConstraint lowering the global grens via operand().
        Conditie.GeheelPeriode gp82 = geheelPeriodeVan(assertie);
        if (gp82 != null) {
            Gp82 g = geheelPeriode82(assertie, gp82, subject);
            if (g == null || g.grens() == null || g.grens().globals().size() != 1) return null;
            String gn = g.grens().globals().iterator().next();
            java.util.function.Function<Object, Object> tl = g.timeline(); WaardeFn gf = g.grens();
            int toets = g.toets(); String gran = g.gran(); boolean niet = g.niet();
            return new SchendingGlobal(gn, (s, gv) -> {
                BigDecimal grens = RegelSpraakRuntime.toBd(gf.fn().apply(s, bindGlobal(gn, gv)));
                boolean b = RegelSpraakRuntime.rsGeheelPeriode((List<?>) tl.apply(s), gran, toets, grens);
                return !(niet ? !b : b);
            });
        }
        if (!(assertie instanceof Conditie.Vergelijking v)) return null;
        // §13.4.8 a DATUM comparison assertie where an operand is a §6/§13.4.16 date-arithmetic expression reading
        // EXACTLY ONE Parameter-GLOBAL (the duration is a parameter): the violation is !datumVergelijkTemporal; the
        // global is bound as a pattern variable by the caller. Mirrors DRL's negated rsDatumReken constraint.
        if (isDatumVergelijkOp(v.op()) && v.links().type().isDatum() && v.rechts().type().isDatum()
                && (isDatumRekenExpr(v.links()) || isDatumRekenExpr(v.rechts()))) {
            WaardeFn lw = scalarWaarde(null, v.links()), rw = scalarWaarde(null, v.rechts());
            if (lw != null && rw != null) {
                Set<String> gset = new LinkedHashSet<>(lw.globals()); gset.addAll(rw.globals());
                if (gset.size() == 1) {
                    String gn = gset.iterator().next();
                    if (globals.containsKey(gn)) {
                        BiFunction<Object, Map<String, BigDecimal>, Object> lfn = lw.fn(), rfn = rw.fn();
                        Conditie.Operator op = v.op();
                        return new SchendingGlobal(gn, (s, gv) ->
                                !datumVergelijkTemporal(lfn.apply(s, bindGlobal(gn, gv)), rfn.apply(s, bindGlobal(gn, gv)), op));
                    }
                }
            }
            return null;   // 0 globals -> handled by schending; >1 global -> skip
        }
        if (!(v.links() instanceof Expr.AttribuutRef la) || !isNumeriekOp(v.op())) return null;
        if (!(v.rechts() instanceof Expr.ParameterRef pr) || pr.parameter().granulariteit() != null) return null;
        java.util.function.Function<Object, Object> lRead = consistentieReader(la, subject);
        if (lRead == null) return null;
        String gn = globalNaam(pr.parameter().naam());
        if (!globals.containsKey(gn)) return null;
        Conditie.Operator flip = Operatoren.flip(v.op());
        boolean gelijkheid = v.op() == Conditie.Operator.GELIJK || v.op() == Conditie.Operator.ONGELIJK;
        BigDecimal[] f = restatement(effType(la), pr.parameter().datatype());
        return new SchendingGlobal(gn, (s, gv) -> {
            BigDecimal a = scale(RegelSpraakRuntime.toBd(lRead.apply(s)), f[0]), b = scale(gv, f[1]);
            return gelijkheid ? vergelijk(a, b, flip) : (a == null || b == null || vergelijk(a, b, flip));
        });
    }

    /** The §5.3 / typeringen Tabel 38 (GELIJK) + Tabel 44 (ONGELIJK) satisfied truth value of a NON-numeriek
     *  (Tekst / enumeratiewaarde / Booleaan) equality between two operand VALUES ({@code null} == leeg), mirroring the
     *  mvel that DrlTranspiler emits: {@code ==} is VALUE equality and mvel reads {@code null == null} as WAAR, so a
     *  GELIJK is WAAR only when BOTH operands are present and equal (both-leeg / one-leeg -> onwaar/fout); an ONGELIJK is
     *  WAAR for exactly-one-leeg OR both-present-and-unequal (both-leeg -> onwaar). Both lifts (the §9.5 schending and the
     *  condition leafPredicate) share this so they stay in lock-step with the DRL back-end. */
    static boolean tekstAttrGelijk(Object lv, Object rv, boolean gelijk) {
        boolean bothLeeg = lv == null && rv == null;
        boolean gelijkeWaarden = java.util.Objects.equals(lv, rv);   // true when both null OR both equal-value
        return gelijk ? (gelijkeWaarden && !bothLeeg)                 // Tabel 38: WAAR only both-present-and-equal
                      : (!gelijkeWaarden && !bothLeeg);               // Tabel 44: WAAR for one-leeg OR both-present-unequal
    }

    /** §5.8/§9.5 the PRESENCE count of a TO-ONE role read as an AANTAL ("het aantal houder van de Penning"):
     *  {@code 1} when the subject's scalar reference field is filled, {@code 0} when it is empty — never null, so
     *  the comparison that consumes it has no leeg flank. The role is resolved through the same authority
     *  {@link FactModelGenerator#leden} derives the field from: the WRITTEN role name first (the §13.4.1 reading —
     *  the AANTAL's argument NAMES the role), the by-pair search second.
     *
     *  <p>null for every other shape, deliberately:
     *  <ul>
     *    <li>a TO-MANY role's count is an accumulate over the element type, which the {@link #aggregatie} machinery
     *        builds and a fact-only {@code Predicate1} cannot carry (the §9.5 violation predicate has no rule items
     *        to hang a CE on) — an honest skip;</li>
     *    <li>a to-one role whose SIBLING is to-many — the §13.3.9 storage-direction limit {@link #aggregatie}
     *        states in full: such a relation has exactly ONE bean field, so it necessarily serves BOTH directions
     *        and a link supplied from the far end reads here as this end's own, where ref (which stores links per
     *        instance per role key) reads nothing. A presence count over a shared field is not a directional count;
     *        refuse it rather than assert a violation that is not there — or, worse, miss one that is;</li>
     *    <li>a filter, a navigated argument, a multi-operand count, or a role whose bean field the model does not
     *        carry — asked of {@link #heeftReferentieVeld}, the ONE shared question, AFTER the role resolves.</li>
     *  </ul> */
    private Telling toOneAantalReader(Expr.Functie f, Objecttype subject) {
        String generiek = "AANTAL-assertie buiten Phase-D scope";
        if (subject == null || f.filter() != null || f.argumenten().size() != 1) return new Telling(null, generiek);
        if (!(f.argumenten().get(0) instanceof Expr.AttribuutRef a) || !ongenavigeerd(a))
            return new Telling(null, generiek);
        Objecttype elem = f.elementObjecttype();
        if (elem == null || a.attribuut() == null) return new Telling(null, generiek);
        String rol = model.verbindendeRolViaNaam(subject, elem, a.attribuut());
        if (rol == null) rol = model.verbindendeRol(subject, elem);
        if (rol == null) return new Telling(null, generiek);
        if (model.naarVeelRol(subject, elem, rol))
            return new Telling(null, "AANTAL over de meervoudige rol '" + rol + "' vergt een accumulate-CE, die een"
                    + " fact-only schendingpredicaat niet kan dragen");
        String zuster = model.verbindendeRolViaElementnaam(subject, a.attribuut());
        if (zuster == null) zuster = model.verbindendeRol(elem, subject);
        if (zuster == null) return new Telling(null, generiek);
        if (model.naarVeelRol(elem, subject, zuster))
            return new Telling(null, "AANTAL over de enkelvoudige rol '" + rol + "' wiens zusterrol '" + zuster
                    + "' meervoudig is: dat feittype heeft ÉÉN beanveld en dat draagt noodzakelijk BEIDE richtingen."
                    + " Deze weigering staat CONSERVATIEF: haar oorspronkelijke grond — de gerichte telling van de"
                    + " case-invoer — is per RUNTIME-SEMANTICS ch. 5.7a ingetrokken, en of ze daarmee overbodig is"
                    + " geworden vergt een eigen, gemeten kaart (het opheffen verschuift gate-12-kanalen)"
                    + " (§13.3.9 opslagrichting)");
        // §13.3.9: and the field this counter null-tests must really be the REFERENCE. Until 2026-09-11 this reader
        // derived that itself — `subject.heeftLid(a.attribuut())`, asked BEFORE the role resolved — which is a second
        // reading of leden()'s law and reads the WRONG NAME whenever the two differ. They differ in the ordinary case:
        // the AANTAL's argument is the ELEMENT role as WRITTEN ("het aantal penningen"), which resolves through the
        // role's meervoud to the field name ("penning"). Measured on a verifier-clean, ref-clean model — a 1:1
        // penningbezit, a Jutter declaring a Numeriek `penning` beside it, a consistentie "het aantal penningen van de
        // Jutter is groter of gelijk aan 1", no link supplied:
        //     verifier: PASS   ref: PASS   clingo: PASS       (the consistentie is VIOLATED — J1 holds no penning)
        //     Drools  : FAIL  verwacht inconsistent, werkelijk consistent
        // The copy passed (Jutter declares no `penningen`) while the read took the attribute's 5 for a filled
        // reference and reported a violated consistentie as satisfied. The shared question, asked HERE, sees it.
        if (!heeftReferentieVeld(subject, rol, elem))
            return new Telling(null, geenReferentieveldReden(subject, rol, elem) + " (§13.3.9 opslagrichting)");
        final String veld = rol;
        return new Telling(s -> RegelSpraakRuntime.get(s, veld) == null ? BigDecimal.ZERO : BigDecimal.ONE, null);
    }

    /** Either the §9.5 presence-count reader, or the precise reason there is none — so the transpiler's SKIP names
     *  the shape instead of the generic Phase-D note (the gate prints it as the golden's exclusion reason). */
    private record Telling(java.util.function.Function<Object, BigDecimal> lezer, String reden) {}

    /** A §13.4.8 consistentie comparison-operand reader: the subject's OWN attribute (get) or a to-ONE NAVIGATED read
     *  (navReader — its null IS the operand's leeg, since DRL's {@code l.leeg} = {@code rol == null || rol.attr ==
     *  null}, exactly what navReader collapses). null for a temporal / dimensioned / unresolvable / to-many operand. */
    private java.util.function.Function<Object, Object> consistentieReader(Expr.AttribuutRef a, Objecttype subject) {
        if (a.objecttype() == null) return null;
        if (Namen.bevatNaam(a.objecttype().granulariteitLeden(), a.attribuut())) return null;   // temporal -> Phase F
        if (!a.objecttype().dimensiesVan(a.attribuut()).isEmpty()) return null;               // §3.6 gedimensioneerd -> later
        if (ongenavigeerd(a)) {
            if (subject == null || !subject.heeftLid(a.attribuut())) return null;
            String attr = a.attribuut();
            return s -> RegelSpraakRuntime.get(s, attr);
        }
        return navReader(a, subject);   // to-one navigated; null == leeg
    }

    /** The ch. 5.9 ever-uniqueness key-SHAPE violation, worded as ref ({@code Eval.ever_tuples}) and clingo
     *  ({@code uni_key_guards}) word it, so the red fence pins ONE fragment across all three executors. */
    private static final String TE_VEEL_TEMPORELE_SLEUTELS =
            "a uniqueness key list carries at most one temporal key component "
            + "(the same-moment tuple join has no corpus case; ch. 5.9)";

    /** How many of {@code sleutels} are temporal on {@code ot} — a declared attribuutGranulariteit, the same
     *  notion the key-join lowering below branches on, tested through the same ref-true mirror
     *  ({@link #temporeelAttribuut}) so the guard and the join it guards can never read one key list
     *  differently. */
    private static int temporeleSleutels(Objecttype ot, List<String> sleutels) {
        return (int) sleutels.stream().filter(k -> temporeelAttribuut(ot, k)).count();
    }

    /** A construct whose key SHAPE is invalid, lowered to a rule that FAULTS as soon as a subject instance
     *  reaches it. Fire is the mirroring moment: ref raises the same form error per instance DURING evaluation
     *  (so a type without instances never reaches it), and a transpile-time throw would surface as a backend
     *  crash — an ERROR — where the red fence must observe a genuine runtime FAULT. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private Rule vormFoutRegel(String regelNaam, Class<?> factClass,
                               io.github.yhavinga.regelspraak.ir.Geldigheid g, String melding) {
        Variable sVar = DSL.declarationOf(factClass, "$subject");
        PatternDSL.PatternDef pat = geldigGuard(PatternDSL.pattern(sVar), g);   // §4.2 window, as the real rule
        Block2<org.drools.model.Drools, Object> body = (drools, s) -> { throw new IllegalStateException(melding); };
        return PatternDSL.rule(regelNaam).build(pat, DSL.on(sVar).execute(body));
    }

    /** §8.1.6 uniciteitscontrole: "is uniek" is violated when ≥2 DISTINCT instances share a (non-null) key. The
     *  subject binds each non-null key part (+ a parent scope field), an accumulate COUNTS the instances whose key
     *  matches, and the rule fires (inserts an Inconsistentie) when that count > 1 — flagging every instance in the
     *  duplicate group. A null key cannot establish a duplicate (§8.1.6) so it is filtered out. Mirrors
     *  DrlTranspiler.uniciteit. null -> skip (no fact class / a temporal key). */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private Rule uniciteit(io.github.yhavinga.regelspraak.ir.ResolvedUniciteit u) {
        Objecttype subject = u.subject();
        if (subject == null || u.sleutels().isEmpty()) return skip("uniciteit (geen subject/sleutel): " + u.naam());
        Class<?> factClass = factClass(subject);
        if (factClass == null) return skip("uniciteit (geen fact-class " + subject.naam + "): " + u.naam());
        // ch. 5.9 key-shape arity, the guard ref and clingo already hold: at most ONE temporal key component.
        // Placed at the earliest point the fault rule is constructible (it needs no Inconsistentie fact) —
        // the form error screams before any duplicate is looked for, as it does in ref.
        if (temporeleSleutels(subject, u.sleutels()) > 1)
            return vormFoutRegel(kop(u.naam()), factClass, u.geldigheid(),
                    TE_VEEL_TEMPORELE_SLEUTELS + ": " + u.naam());
        Class<?> incClass = inconsistentieClass();
        if (incClass == null) return skip("uniciteit (geen Inconsistentie-fact): " + u.naam());
        // the scope field comes from scopeRol, never from the wire's scopeVeld (V1-WIREMIGRATIE P2, gone at B8)
        final String sv = FactModelGenerator.scopeVeld(model, u);
        if (sv == null && (u.scopeRol() != null || u.scopeVeld() != null))
            return skip("uniciteit (scopebinding onopgelost): " + u.naam());

        seq = 0;
        String naam = u.naam();
        String fqcn = FACTS + ".Inconsistentie";
        Variable sVar = DSL.declarationOf(factClass, "$subject");
        PatternDSL.PatternDef subjPat = geldigGuard(PatternDSL.pattern(sVar), u.geldigheid());   // §4.2 window
        // bind + non-null each key part (and the parent scope field, if any).
        List<Variable> keyVars = new ArrayList<>();
        for (String geschreven : u.sleutels()) {
            String key = gedeclareerdAttribuut(subject, geschreven);
            Variable kVar = DSL.declarationOf(Object.class, "$k_" + (seq++));
            subjPat = subjPat.bind(kVar, (Function1) s -> RegelSpraakRuntime.get(s, key))
                    .expr(nextId("knn"), (Predicate1) s -> RegelSpraakRuntime.get(s, key) != null);
            keyVars.add(kVar);
        }
        Variable scopeVar = null;
        if (sv != null) {
            scopeVar = DSL.declarationOf(Object.class, "$scope_" + (seq++));
            subjPat = subjPat.bind(scopeVar, (Function1) s -> RegelSpraakRuntime.get(s, sv))
                    .expr(nextId("snn"), (Predicate1) s -> RegelSpraakRuntime.get(s, sv) != null);
        }
        // accumulate source: every instance whose key (and scope) matches the subject's; count them.
        Variable oVar = DSL.declarationOf(factClass, "$o_" + (seq++));
        PatternDSL.PatternDef src = PatternDSL.pattern(oVar);
        for (int i = 0; i < u.sleutels().size(); i++) {
            String key = gedeclareerdAttribuut(subject, u.sleutels().get(i));
            if (temporeelAttribuut(subject, key))
                // ever-uniqueness (uchar RS ch. 5.9, both forms): the shared ever-value predicate
                src = src.expr(nextId("kj"), (Variable) keyVars.get(i),
                        (Predicate2) (o, k) -> RegelSpraakRuntime.rsOoitBotst(RegelSpraakRuntime.get(o, key), k));
            else {
                // an enumeration key is a declared name: two spellings of one label are ONE key (ref, clingo)
                java.util.function.UnaryOperator<Object> n = gelijkheidsLezing(subject.attribuut(key), null);
                src = src.expr(nextId("kj"), (Variable) keyVars.get(i),
                        (Predicate2) (o, k) -> RegelSpraakRuntime.gelijkWaarde(n.apply(RegelSpraakRuntime.get(o, key)), n.apply(k)));
            }
        }
        if (scopeVar != null) {
            src = src.expr(nextId("sj"), (Variable) scopeVar,
                    (Predicate2) (o, sc) -> RegelSpraakRuntime.gelijkWaarde(RegelSpraakRuntime.get(o, sv), sc));
        }
        Variable cntVar = DSL.declarationOf(Object.class, "$cnt_" + (seq++));
        RuleItemBuilder<?> accItem = DSL.accumulate(src, DSL.accFunction(accFn("CountAccumulateFunction")).as(cntVar));
        PatternDSL.PatternDef cntPat = PatternDSL.pattern(cntVar).expr(nextId("gt1"), (Predicate1) c -> ((Number) c).intValue() > 1);
        // idempotency guard + insert (insert-only -> NO no-loop).
        Variable incVar = DSL.declarationOf(incClass, "$inc_" + (seq++));
        PatternDSL.PatternDef guard = PatternDSL.pattern(incVar)
                .expr(nextId("ig"), (Predicate1) i -> naam.equals(RegelSpraakRuntime.get(i, "regel")))
                .expr(nextId("ig"), (Variable) sVar, (Predicate2) (i, s) -> RegelSpraakRuntime.get(i, "subject") == s);
        Block2<org.drools.model.Drools, Object> body = (drools, s) ->
                drools.insert(RegelSpraakRuntime.newInconsistentie(fqcn, naam, s));
        return PatternDSL.rule(kop(naam)).build(subjPat, accItem, cntPat, DSL.not(guard), DSL.on(sVar).execute(body));
    }

    /** §8.1.6 "verenigd met" cross-type union uniqueness ("de namen van alle Cirkels verenigd met de namen van alle
     *  Vierkanten moeten uniek zijn"): the key (or key tuple) must be unique across the UNION of every source's key
     *  space. One rule per source — a subject of that source binds its non-null key parts, an accumulate per source
     *  counts the instances (of that source) whose key matches, and the rule flags the subject when the SUM of the
     *  counts over all sources exceeds 1. The Inconsistentie carries the SHARED union name (so the flagged-set spans
     *  the sources); the rule NAME is disambiguated by the 1-based source index (rule names must be unique). The sum
     *  is gated in the consequence (insert iff >1) — same observable as DRL's {@code eval(sum>1)} when-gate, but
     *  general over any source count. Mirrors DrlTranspiler.uniciteitUnie. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private List<Rule> uniciteitUnie(io.github.yhavinga.regelspraak.ir.ResolvedUniciteitUnie u) {
        var bronnen = u.bronnen();
        if (bronnen.isEmpty()) return skipList("uniciteit-unie (geen bronnen): " + u.naam());
        int arity = bronnen.get(0).sleutels().size();
        Class<?> incClass = inconsistentieClass();
        if (incClass == null) return skipList("uniciteit-unie (geen Inconsistentie-fact): " + u.naam());
        for (var b : bronnen) {
            if (b.sleutels().size() != arity) return skipList("uniciteit-unie (sleutel-ariteit verschilt): " + u.naam());
            if (factClass(b.type()) == null) return skipList("uniciteit-unie (geen fact-class " + b.type().naam + "): " + u.naam());
        }
        String naam = u.naam();
        String fqcn = FACTS + ".Inconsistentie";
        List<Rule> rules = new ArrayList<>();
        for (int s = 0; s < bronnen.size(); s++) {
            var subj = bronnen.get(s);
            Class<?> subjClass = factClass(subj.type());
            seq = 0;
            // the same ch. 5.9 arity guard, per SOURCE: ref runs ever_tuples over each source's OWN key list,
            // so a source whose list carries two temporal components faults on ITS instances
            if (temporeleSleutels(subj.type(), subj.sleutels()) > 1) {
                rules.add(vormFoutRegel(kop(naam + " (" + subj.type().naam + " " + (s + 1) + ")"),
                        subjClass, u.geldigheid(), TE_VEEL_TEMPORELE_SLEUTELS + ": " + naam));
                continue;
            }
            Variable sVar = DSL.declarationOf(subjClass, "$subject");
            PatternDSL.PatternDef subjPat = geldigGuard(PatternDSL.pattern(sVar), u.geldigheid());   // §4.2 window
            List<Variable> keyVars = new ArrayList<>();
            for (int i = 0; i < arity; i++) {
                String key = gedeclareerdAttribuut(subj.type(), subj.sleutels().get(i));
                Variable kVar = DSL.declarationOf(Object.class, "$k_" + (seq++));
                subjPat = subjPat.bind(kVar, (Function1) o -> RegelSpraakRuntime.get(o, key))
                        .expr(nextId("uknn"), (Predicate1) o -> RegelSpraakRuntime.get(o, key) != null);   // null key cannot collide
                keyVars.add(kVar);
            }
            List<RuleItemBuilder<?>> items = new ArrayList<>();
            items.add(subjPat);
            List<Variable> countVars = new ArrayList<>();
            for (int j = 0; j < bronnen.size(); j++) {                 // one count per source, matched by key value
                var b = bronnen.get(j);
                Variable oVar = DSL.declarationOf(factClass(b.type()), "$o" + j + "_" + (seq++));
                PatternDSL.PatternDef src = PatternDSL.pattern(oVar);
                for (int i = 0; i < arity; i++) {
                    String key = gedeclareerdAttribuut(b.type(), b.sleutels().get(i));
                    // each source declares its OWN key list, so a component's temporality is a property of the
                    // PAIR (this source's key, the subject source's key) — all four combinations occur, and
                    // ref plays every one of them (ever_tuples runs per source).
                    boolean elementTemporeel = temporeelAttribuut(b.type(), key);
                    boolean subjectTemporeel = temporeelAttribuut(subj.type(), subj.sleutels().get(i));
                    if (elementTemporeel && subjectTemporeel)
                        // ever-uniqueness (uchar RS ch. 5.9): a temporal key joins on the EVER-value
                        // intersection — the same shared-runtime predicate the DRL route calls
                        src = src.expr(nextId("ukj"), (Variable) keyVars.get(i),
                                (Predicate2) (e, k) -> RegelSpraakRuntime.rsOoitBotst(RegelSpraakRuntime.get(e, key), k));
                    else if (elementTemporeel)
                        // MIXED: this source's timeline against the subject's plain value
                        src = src.expr(nextId("ukj"), (Variable) keyVars.get(i),
                                (Predicate2) (e, k) -> RegelSpraakRuntime.rsOoitBevat(RegelSpraakRuntime.get(e, key), k));
                    else if (subjectTemporeel)
                        // MIXED, the mirror: this source's plain value against the subject's timeline
                        src = src.expr(nextId("ukj"), (Variable) keyVars.get(i),
                                (Predicate2) (e, k) -> RegelSpraakRuntime.rsOoitBevat(k, RegelSpraakRuntime.get(e, key)));
                    else {
                        // WF-030 keeps a position's datatype kind equal across the sources, so either side decides
                        java.util.function.UnaryOperator<Object> n = gelijkheidsLezing(b.type().attribuut(key),
                                subj.type().attribuut(gedeclareerdAttribuut(subj.type(), subj.sleutels().get(i))));
                        src = src.expr(nextId("ukj"), (Variable) keyVars.get(i),
                                (Predicate2) (e, k) -> RegelSpraakRuntime.gelijkWaarde(n.apply(RegelSpraakRuntime.get(e, key)), n.apply(k)));
                    }
                }
                Variable cntVar = DSL.declarationOf(Object.class, "$c" + j + "_" + (seq++));
                items.add(DSL.accumulate(src, DSL.accFunction(accFn("CountAccumulateFunction")).as(cntVar)));
                countVars.add(cntVar);
            }
            // idempotency guard: not Inconsistentie( regel == naam, subject == $subject )
            Variable incVar = DSL.declarationOf(incClass, "$inc_" + (seq++));
            PatternDSL.PatternDef guard = PatternDSL.pattern(incVar)
                    .expr(nextId("uig"), (Predicate1) i -> naam.equals(RegelSpraakRuntime.get(i, "regel")))
                    .expr(nextId("uig"), (Variable) sVar, (Predicate2) (i, ss) -> RegelSpraakRuntime.get(i, "subject") == ss);
            items.add(DSL.not(guard));
            // consequence: sum the per-source counts; insert iff > 1 (insert-only -> NO no-loop). consequenceOf binds
            // at most 9 vars (Block10), so [subject + one count per source] overflows past 8 sources. To stay general
            // over ANY source count (DRL's string sum has no ceiling — keeping the differential faithful) WITHOUT
            // changing the ≤8-source form, FOLD the counts pairwise into one running total via DSL.from: each fold var
            // is a from-sourced declaration ($sum = from($prev, $cj, (p,c) -> p+c)) matched by a bare pattern, so the
            // consequence then binds only [subject, total]. from(...) gives the fold var a real binding source the
            // consequence can resolve (a re-bound accumulate result cannot).
            List<Variable> vars = new ArrayList<>();
            vars.add(sVar);
            RuleItemBuilder<?> consequence;
            if (countVars.size() + 1 <= 9) {
                vars.addAll(countVars);
                final int nC = countVars.size();
                consequence = consequenceOf(vars.toArray(new Variable[0]), (drools, args) -> {
                    int sum = 0;
                    for (int c = 0; c < nC; c++) sum += ((Number) args[1 + c]).intValue();
                    if (sum > 1) drools.insert(RegelSpraakRuntime.newInconsistentie(fqcn, naam, args[0]));
                });
            } else {
                Variable runningSum = countVars.get(0);            // seed with the first count (already a bound Number)
                for (int j = 1; j < countVars.size(); j++) {
                    final Variable prev = runningSum;
                    Variable sumVar = DSL.declarationOf(Object.class,
                            DSL.from((Variable) prev, (Variable) countVars.get(j),
                                    (Function2) (p, c) -> ((Number) p).intValue() + ((Number) c).intValue()));
                    items.add(PatternDSL.pattern(sumVar));
                    runningSum = sumVar;
                }
                vars.add(runningSum);
                consequence = consequenceOf(vars.toArray(new Variable[0]), (drools, args) -> {
                    if (((Number) args[1]).intValue() > 1) drools.insert(RegelSpraakRuntime.newInconsistentie(fqcn, naam, args[0]));
                });
            }
            if (consequence == null) { overgeslagen.add("uniciteit-unie (te veel bronnen): " + u.naam()); return rules; }
            items.add(consequence);
            rules.add(PatternDSL.rule(kop(naam + " (" + subj.type().naam + " " + (s + 1) + ")")).build(items.toArray(new RuleItemBuilder[0])));
        }
        return rules;
    }

    // ---------------- §9.7 / §13.4.10 verdeling (Phase E) ----------------
    /** §13.4.10 verdeling: a source amount ({@code bronAttribuut} of {@code bronType}) is distributed over a
     *  recipient attribute ({@code doelAttribuut} of the related {@code doelType}) by a method. GELIJKE_DELEN
     *  (total/count) and NAAR_RATO (total·weight/Σweight) each fold the recipients in one accumulate share rule;
     *  OP_VOLGORDE (§9.7.2) collects the recipients and lowers the whole ordered group-by-group allocation through
     *  {@link RegelSpraakRuntime#rsVerdeelOpVolgorde}. The §9.7.3 cap / §9.7.4 floor ride on every fireable method;
     *  the §9.7.6 empty-weight and leeg/negatief-maximum Fouten are SEPARATE insert-only Inconsistentie companions.
     *  Mirrors DrlTranspiler.verdeling. Returns the rules, or an empty list (recording an honest skip) for an
     *  unresolvable join / non-numeric target/weight / non-orderable ordering key / a §9.7.5 onverdeelde-rest
     *  modifier (the rest rule is a source-modify that forms a share↔rest cycle DRL breaks via property-reactive
     *  {@code modify}; the reflective model's opaque lambdas are class-reactive, so it lands as a focused follow-up).
     *  The join is {@link FactModelGenerator#verdelingsjoin}'s: the wire's recipient binding decides it, the type
     *  pair only where the verdeling carries none (V1-WIREMIGRATIE P3). */
    private List<Rule> verdeling(ResolvedVerdeling v) {
        Objecttype bron = v.bronType(), doel = v.doelType();
        FactModelGenerator.Verdelingsjoin vj = FactModelGenerator.verdelingsjoin(model, v, rolBindingen.get(v));
        if (vj.weigering() != null) return skipList("verdeling (" + vj.weigering() + "): " + v.naam());
        String join = vj.rol();
        ResolvedVerdeling.Methode methode = v.methode();
        Datatype doelType = doel != null && v.doelAttribuut() != null ? doel.attribuut(v.doelAttribuut()) : null;
        boolean doelOk = doelType != null && doelType.isGetalachtig();
        Datatype gewichtType = doel != null && v.ratoAttribuut() != null ? doel.attribuut(v.ratoAttribuut()) : null;
        boolean gelijk = methode == ResolvedVerdeling.Methode.GELIJKE_DELEN && join != null && doelOk;
        boolean rato = methode == ResolvedVerdeling.Methode.NAAR_RATO && join != null && doelOk
                && gewichtType != null && gewichtType.isGetalachtig();
        Datatype groepType = methode == ResolvedVerdeling.Methode.OP_VOLGORDE && doel != null && v.groepeerAttribuut() != null
                ? doel.attribuut(v.groepeerAttribuut()) : null;
        boolean opVolgorde = methode == ResolvedVerdeling.Methode.OP_VOLGORDE && join != null && doelOk
                && groepType != null && groepType.isOrdenbaar()
                && (v.ratoAttribuut() == null || (gewichtType != null && gewichtType.isGetalachtig()));
        if (!gelijk && !rato && !opVolgorde)
            return skipList("verdeling (onopgelost " + (bron == null || doel == null ? "referentie" : methode.name().toLowerCase()) + "): " + v.naam());
        // §13.3.9: `join` is the recipient's reference back at the source, and every one of the six reads below
        // ({@code get(recipient, join) == $src} in the share, rest, ordering and Fout rules) touches THAT field.
        // verbindendeRol derives a ROLE; leden() derives a FIELD only where the role is to-one AND its name is not
        // taken by a declared member — the two differ, and the difference is reachable. Measured 2026-09-11 on a
        // verifier-clean, ref-clean model (bergingsdeelname flipped so the recipient role is to-many, i.e. the
        // storage sits at the source end and the recipient derives nothing):
        //     verifier: PASS   ref: PASS (2 expectations)   clingo: PASS (2 expectations)
        //     Drools exec-model : ERROR ConstraintEvaluationException          (unclassified, at first fire)
        //     Drools DRL-tekst  : EXCLUDE drl compile: Unable to Analyse Expression berging == $src
        // Both loud, neither a diagnosis. The name-DISPLACED half of the same limit is not reachable with a live
        // relation — CaseRunner.naamdelingsPoort refuses to write the link at all — so the allocation then simply
        // finds no recipients, which is what ref computes for a relation that is not there; that half was measured
        // too and agrees.
        if (!heeftReferentieVeld(doel, join, bron))
            return skipList("verdeling (" + geenReferentieveldReden(doel, join, bron) + "): " + v.naam());
        // TWO verdelingen writing the SAME recipient (doelType, doelAttribuut) infinite-loop in the model — the
        // reflective share rules are class-reactive, so each update(r) re-activates the SIBLING (NO_LOOP blocks only
        // self-retrigger). Adding .watch(join) to the Deelnemer patterns DOES break the loop (empirically verified),
        // but then the back-ends DIVERGE on the VALUE: which verdeling "wins" the shared attribute is agenda-order
        // dependent, and DRL (KieHelper) and the model (createKieBaseFromModel) order the rules differently — e.g. 100
        // over 1:3 gave DRL a1=25/a2=75 (naar-rato won) vs model a1=50/a2=50 (gelijke-delen won). The construct is
        // DEGENERATE (two distribution rules conflicting on one attribute is a modeling error; RegelSpraak defines no
        // value for it), so the principled choice is an HONEST skip — not a fragile agenda-order parity. (Single-writer
        // — every real fixture — is unaffected.)
        long writers = model.verdelingen.stream().filter(o -> o.doelType() != null && doel != null
                && o.doelType().naam.equals(doel.naam) && java.util.Objects.equals(o.doelAttribuut(), v.doelAttribuut())).count();
        if (writers > 1) return skipList("verdeling (>1 verdeling schrijft hetzelfde doel-attribuut — agenda-order-afhankelijke waarde, principiële skip): " + v.naam());
        Class<?> bronClass = factClass(bron), doelClass = factClass(doel);
        if (bronClass == null || doelClass == null) return skipList("verdeling (geen fact-class): " + v.naam());
        // §13.4.10: the verdeling voorwaarde gates the WHOLE lowered family (shares, §9.7.5 rest, §9.7.6 Fout
        // companions) — a false gate must write NOTHING. Each sub-rule applies it to its $src pattern (gateVoorwaarde);
        // probe its exec-scope once here. A voorwaarde outside scope, or one that needs a hoisted join/aggregation
        // (an in-pattern form has none), makes the whole verdeling an honest skip rather than distribute
        // unconditionally (mirrors DrlTranspiler's no-op marker).
        if (v.voorwaarde() != null) {
            Variable probe = DSL.declarationOf(bronClass, "$src");
            Gate g = voorwaarde(v.voorwaarde(), PatternDSL.pattern(probe), bron, probe);
            if (g == null || !g.extra().isEmpty())
                return skipList("verdeling (voorwaarde buiten exec-model scope): " + v.naam());
        }

        List<Rule> rules = new ArrayList<>();
        rules.add(opVolgorde ? opVolgordeRegel(v, join, bronClass, doelClass)
                             : gelijkOfRatoRegel(v, join, gelijk, bronClass, doelClass));
        // §9.7.5 onverdeelde rest: the source keeps total − Σ(assigned shares). naar-rato's recipient filter excludes
        // the weightless (they get no share); gelijke/op-volgorde collect every recipient.
        if (v.restantAttribuut() != null)
            rules.add(restantRegel(v, join, rato, bronClass, doelClass));
        // §9.7.6 Tabel 19 Fouten: surface an empty weight (whenever a naar-rato split is present) or a leeg/negatief
        // maximum on each offending recipient as a SEPARATE insert-only Inconsistentie companion (the value side
        // already degrades a bad cap to uncapped via rsPlafond — never a negative share). Under naar rato a cap is
        // only read for a weighted recipient of a split whose weight sum is non-zero, as in ref.
        Class<?> incClass = inconsistentieClass();
        boolean tieNaarRato = rato || (opVolgorde && v.ratoAttribuut() != null);
        if (tieNaarRato && incClass != null) {
            String w = v.ratoAttribuut();
            rules.add(ontvangerFoutRegel(v.naam() + " (leeg gewicht)", v.naam(), bronClass, doelClass, join, incClass,
                    d -> RegelSpraakRuntime.get(d, w) == null, null, v));
        }
        if (v.maximumAttribuut() != null && incClass != null) {
            String mx = v.maximumAttribuut();
            rules.add(ontvangerFoutRegel(v.naam() + " (leeg of negatief maximum)", v.naam(), bronClass, doelClass, join, incClass,
                    d -> { Object m = RegelSpraakRuntime.get(d, mx); return m == null || ((BigDecimal) m).signum() < 0; },
                    rato ? v.ratoAttribuut() : null, v));
        }
        return rules;
    }

    /** §13.4.10: gate a verdeling sub-rule's {@code $src} pattern on the verdeling voorwaarde so the whole lowered
     *  family fires together (mirrors DrlTranspiler's splice of the voorwaarde into every bron pattern). {@link
     *  #verdeling} has already checked the voorwaarde is in exec-scope with no hoisted CE, so the gate is non-null
     *  with empty extra; an empty voorwaarde leaves the pattern unchanged. */
    private PatternDSL.PatternDef gateVoorwaarde(PatternDSL.PatternDef srcPat, Variable srcVar, ResolvedVerdeling v) {
        if (v.voorwaarde() == null) return srcPat;
        return voorwaarde(v.voorwaarde(), srcPat, v.bronType(), srcVar).pattern();
    }

    /** §13.4.10 GELIJKE_DELEN / NAAR_RATO: one accumulate folds the recipients joined to {@code $src} (count, or the
     *  Σ of the present weights); each recipient {@code $r} gets total/count or total·weight/Σweight, then the §9.7.3
     *  cap (naar rato) and §9.7.4 floor. A non-zero weight-sum guards the naar-rato divide (a weightless recipient is
     *  excluded — left unset — and the §9.7.6 empty-weight Fout is the separate companion). Mirrors gelijkOfRatoRegel. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private Rule gelijkOfRatoRegel(ResolvedVerdeling v, String join, boolean gelijk, Class<?> bronClass, Class<?> doelClass) {
        seq = 0;
        String bronAttr = v.bronAttribuut(), doelAttr = v.doelAttribuut(), gewAttr = v.ratoAttribuut(), maxAttr = v.maximumAttribuut();
        Integer dec = v.afrondingDecimalen();
        // §9.7.5 cycle break: the SOURCE pattern watches ONLY the source amount, NOT the restant a rest rule writes —
        // so a rest rule's update(src) (mask=restant) does not re-wake this share rule (the share↔rest Pot cycle).
        // The Deelnemer accumulate/recipient patterns stay class-reactive (they must react to recipient inserts); they
        // never drive a loop here (NO_LOOP blocks the share's own aandeel-write re-trigger, and a rest writes the Pot,
        // not a Deelnemer). The multi-writer-same-attribute loop is separately GUARDED above. watch(...) is honoured by
        // the default model base (empirically verified, Phase E review).
        Variable srcVar = DSL.declarationOf(bronClass, "$src");
        PatternDSL.PatternDef srcPat = PatternDSL.pattern(srcVar).watch(prop(bronAttr));
        srcPat = geldigGuard(srcPat, v.geldigheid());   // §4.2 window (Pot source; the rekendatum global is constant, no extra reactivity)
        srcPat = gateVoorwaarde(srcPat, srcVar, v);     // §13.4.10 voorwaarde gate (whole family)

        // accumulate the recipients joined to $src: count (gelijke delen) or sum of the present weights (naar rato).
        Variable cVar = DSL.declarationOf(doelClass, "$c_" + (seq++));
        PatternDSL.PatternDef accSrc = PatternDSL.pattern(cVar).expr(nextId("join"), (Variable) srcVar,
                (Predicate2) (c, s) -> RegelSpraakRuntime.get(c, join) == s);
        Variable foldVar = DSL.declarationOf(Object.class, "$fold_" + (seq++));
        RuleItemBuilder<?> acc;
        if (gelijk) {
            acc = DSL.accumulate(accSrc, DSL.accFunction(accFn("CountAccumulateFunction")).as(foldVar));
        } else {
            accSrc = accSrc.expr(nextId("wnn"), (Predicate1) c -> RegelSpraakRuntime.get(c, gewAttr) != null);
            Variable wVar = DSL.declarationOf(BigDecimal.class, "$w_" + (seq++));
            accSrc = accSrc.bind(wVar, (Function1) c -> RegelSpraakRuntime.get(c, gewAttr));
            acc = DSL.accumulate(accSrc, DSL.accFunction(accFn("BigDecimalSumAccumulateFunction"), wVar).as(foldVar));
        }

        // the recipient pattern: $r joined to $src (naar rato additionally requires a present weight).
        Variable rVar = DSL.declarationOf(doelClass, "$r");
        PatternDSL.PatternDef rPat = PatternDSL.pattern(rVar).expr(nextId("rjoin"), (Variable) srcVar,
                (Predicate2) (r, s) -> RegelSpraakRuntime.get(r, join) == s);
        if (!gelijk) rPat = rPat.expr(nextId("rwnn"), (Predicate1) r -> RegelSpraakRuntime.get(r, gewAttr) != null);

        List<RuleItemBuilder<?>> items = new ArrayList<>();
        items.add(srcPat); items.add(acc); items.add(rPat);
        if (!gelijk)   // a zero weight-sum would divide-by-zero (the spec leaves it undefined): guard it out.
            items.add(PatternDSL.pattern(foldVar).expr(nextId("sumnz"),
                    (Predicate1) s -> s != null && ((BigDecimal) s).signum() != 0));

        final boolean gelijkF = gelijk;
        Block4<org.drools.model.Drools, Object, Object, Object> body = (drools, s, r, fold) -> {
            BigDecimal totaal = (BigDecimal) RegelSpraakRuntime.get(s, bronAttr);
            BigDecimal aandeel;
            if (gelijkF) {
                aandeel = totaal.divide(RegelSpraakRuntime.toBd(fold), java.math.MathContext.DECIMAL64);
            } else {
                BigDecimal gewicht = (BigDecimal) RegelSpraakRuntime.get(r, gewAttr);
                aandeel = totaal.multiply(gewicht).divide((BigDecimal) fold, java.math.MathContext.DECIMAL64);
            }
            if (maxAttr != null) aandeel = RegelSpraakRuntime.rsPlafond(aandeel, (BigDecimal) RegelSpraakRuntime.get(r, maxAttr));
            if (dec != null) aandeel = RegelSpraakRuntime.rsAfronding(aandeel, dec, java.math.RoundingMode.FLOOR);
            werkBijReflectief(drools, r, doelAttr, aandeel);   // idempotent: no re-propagate on a no-op re-write (anti-loop)
        };
        items.add(DSL.on(srcVar, (Variable) rVar, (Variable) foldVar).execute(body));
        return PatternDSL.rule(kop(v.naam())).attribute(Rule.Attribute.NO_LOOP, true).build(items.toArray(new RuleItemBuilder[0]));
    }

    /** §9.7.2 "op volgorde van": one share rule whose accumulate collects every recipient joined to {@code $src} into
     *  a list, then the consequence recomputes the whole ordered allocation via {@link RegelSpraakRuntime#rsVerdeelOpVolgorde}
     *  (reading the ordering criterium / tie weight / cap reflectively off the collected facts) and assigns each
     *  recipient {@code $r} its identity-keyed share. The cap+floor are applied inside the helper, group by group, so
     *  a future onverdeelde-rest reflects the carried-down pot (§9.7.5 Tabel 18). Mirrors opVolgordeRegel. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private Rule opVolgordeRegel(ResolvedVerdeling v, String join, Class<?> bronClass, Class<?> doelClass) {
        seq = 0;
        String bronAttr = v.bronAttribuut(), doelAttr = v.doelAttribuut(), groepAttr = v.groepeerAttribuut(),
                gewAttr = v.ratoAttribuut(), maxAttr = v.maximumAttribuut();
        Integer dec = v.afrondingDecimalen();
        boolean toenemend = !v.afnemend();
        // cycle break: the SOURCE pattern watches only the source amount (NOT restant); see gelijkOfRatoRegel.
        Variable srcVar = DSL.declarationOf(bronClass, "$src");
        PatternDSL.PatternDef srcPat = PatternDSL.pattern(srcVar).watch(prop(bronAttr));
        srcPat = geldigGuard(srcPat, v.geldigheid());   // §4.2 window (Pot source; the rekendatum global is constant, no extra reactivity)
        srcPat = gateVoorwaarde(srcPat, srcVar, v);     // §13.4.10 voorwaarde gate (whole family)

        // collect every recipient joined to $src into one index-stable list.
        Variable pVar = DSL.declarationOf(doelClass, "$p_" + (seq++));
        Variable idVar = DSL.declarationOf(Object.class, "$id_" + (seq++));
        PatternDSL.PatternDef accSrc = PatternDSL.pattern(pVar)
                .expr(nextId("join"), (Variable) srcVar, (Predicate2) (p, s) -> RegelSpraakRuntime.get(p, join) == s)
                .bind(idVar, (Function1) p -> p);
        Variable factsVar = DSL.declarationOf(java.util.List.class, "$facts_" + (seq++));
        RuleItemBuilder<?> acc = DSL.accumulate(accSrc, DSL.accFunction(accFn("CollectListAccumulateFunction"), idVar).as(factsVar));

        Variable rVar = DSL.declarationOf(doelClass, "$r");
        PatternDSL.PatternDef rPat = PatternDSL.pattern(rVar).expr(nextId("rjoin"), (Variable) srcVar,
                (Predicate2) (r, s) -> RegelSpraakRuntime.get(r, join) == s);

        Block4<org.drools.model.Drools, Object, Object, Object> body = (drools, s, r, facts) -> {
            List<Object> fl = (List<Object>) facts;
            BigDecimal totaal = (BigDecimal) RegelSpraakRuntime.get(s, bronAttr);
            List<Object> groepL = new ArrayList<>();
            for (Object f : fl) groepL.add(RegelSpraakRuntime.get(f, groepAttr));
            List<Object> gewL = null, maxL = null;
            if (gewAttr != null) { gewL = new ArrayList<>(); for (Object f : fl) gewL.add(RegelSpraakRuntime.get(f, gewAttr)); }
            if (maxAttr != null) { maxL = new ArrayList<>(); for (Object f : fl) maxL.add(RegelSpraakRuntime.get(f, maxAttr)); }
            Map<Object, BigDecimal> shares = RegelSpraakRuntime.rsVerdeelOpVolgorde(totaal, fl, groepL, toenemend, gewL, maxL, dec);
            werkBijReflectief(drools, r, doelAttr, shares.get(r));   // idempotent: no re-propagate on a no-op re-write (anti-loop)
        };
        return PatternDSL.rule(kop(v.naam())).attribute(Rule.Attribute.NO_LOOP, true)
                .build(srcPat, acc, rPat, DSL.on(srcVar, (Variable) rVar, (Variable) factsVar).execute(body));
    }

    /** §9.7.6 build a verdeling Fout companion: an insert-only rule (NO no-loop, idempotency-guarded) firing once per
     *  recipient matching {@code recipientFout} (an empty weight, or a leeg/negatief maximum), inserting an
     *  Inconsistentie carrying the verdeling's name on THAT recipient — rule × subject, ch. 5.6, as ref and clingo key
     *  it (RUNTIME-SEMANTICS ch. 4.2/4.3); until 2026-09-23 it landed on the source, once per bron. The recipient set is
     *  ref's: an empty source amount flags nothing, and a non-null {@code gewogenAttr} (a naar-rato cap) restricts it
     *  to weighted recipients of a split whose weight sum is non-zero. The distribution still runs (the §9.7.6 "degrade +
     *  flag" representation); this only surfaces the Fout. Mirrors the DRL "(leeg gewicht)"/"(leeg of negatief maximum)" rules. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private Rule ontvangerFoutRegel(String compNaam, String hostNaam, Class<?> bronClass, Class<?> doelClass, String join,
                                    Class<?> incClass, Predicate1<Object> recipientFout, String gewogenAttr,
                                    ResolvedVerdeling v) {
        seq = 0;
        String fqcn = FACTS + ".Inconsistentie";
        String bronAttr = v.bronAttribuut();
        Variable srcVar = DSL.declarationOf(bronClass, "$src");
        PatternDSL.PatternDef srcPat = PatternDSL.pattern(srcVar)
                .expr(nextId("fbnn"), (Predicate1) s -> RegelSpraakRuntime.get(s, bronAttr) != null);
        srcPat = geldigGuard(srcPat, v.geldigheid());   // §4.2 window
        srcPat = gateVoorwaarde(srcPat, srcVar, v);   // §13.4.10 voorwaarde gate (whole family)
        List<RuleItemBuilder<?>> items = new ArrayList<>();
        items.add(srcPat);
        if (gewogenAttr != null) {
            Variable cVar = DSL.declarationOf(doelClass, "$c_" + (seq++));
            Variable wVar = DSL.declarationOf(BigDecimal.class, "$w_" + (seq++));
            PatternDSL.PatternDef accSrc = PatternDSL.pattern(cVar)
                    .expr(nextId("fsj"), (Variable) srcVar, (Predicate2) (c, s) -> RegelSpraakRuntime.get(c, join) == s)
                    .expr(nextId("fsw"), (Predicate1) c -> RegelSpraakRuntime.get(c, gewogenAttr) != null)
                    .bind(wVar, (Function1) c -> RegelSpraakRuntime.get(c, gewogenAttr));
            Variable sumVar = DSL.declarationOf(Object.class, "$sum_" + (seq++));
            items.add(DSL.accumulate(accSrc, DSL.accFunction(accFn("BigDecimalSumAccumulateFunction"), wVar).as(sumVar)));
            items.add(PatternDSL.pattern(sumVar).expr(nextId("fsnz"),
                    (Predicate1) x -> x != null && ((BigDecimal) x).signum() != 0));
        }
        Variable dVar = DSL.declarationOf(doelClass, "$d_" + (seq++));
        PatternDSL.PatternDef dPat = PatternDSL.pattern(dVar)
                .expr(nextId("fj"), (Variable) srcVar, (Predicate2) (d, s) -> RegelSpraakRuntime.get(d, join) == s)
                .expr(nextId("fp"), (Predicate1) recipientFout);
        if (gewogenAttr != null)
            dPat = dPat.expr(nextId("fw"), (Predicate1) d -> RegelSpraakRuntime.get(d, gewogenAttr) != null);
        items.add(dPat);
        Variable incVar = DSL.declarationOf(incClass, "$inc_" + (seq++));
        items.add(DSL.not(PatternDSL.pattern(incVar)
                .expr(nextId("ig"), (Predicate1) i -> hostNaam.equals(RegelSpraakRuntime.get(i, "regel")))
                .expr(nextId("ig"), (Variable) dVar, (Predicate2) (i, d) -> RegelSpraakRuntime.get(i, "subject") == d)));
        Block2<org.drools.model.Drools, Object> body = (drools, d) ->
                drools.insert(RegelSpraakRuntime.newInconsistentie(fqcn, hostNaam, d));
        items.add(DSL.on(dVar).execute(body));
        return PatternDSL.rule(kop(compNaam)).build(items.toArray(new RuleItemBuilder[0]));
    }

    /** The §9.7.5 recipient filter shared by the rest rule's {@code not} guard and its sum accumulate: the join,
     *  plus (naar rato) a present weight — the SAME set the share rule distributed over. Left class-reactive (the
     *  caller adds the {@code doelAttr ==/!= null} constraint); it writes no Deelnemer, so it cannot loop. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private PatternDSL.PatternDef recipientFilterPattern(Variable var, Variable srcVar, String join, boolean vereisGewicht,
                                                         String gewAttr, String doelAttr) {
        PatternDSL.PatternDef p = PatternDSL.pattern(var).expr(nextId("rfj"), (Variable) srcVar,
                (Predicate2) (d, s) -> RegelSpraakRuntime.get(d, join) == s);
        if (vereisGewicht) p = p.expr(nextId("rfw"), (Predicate1) d -> RegelSpraakRuntime.get(d, gewAttr) != null);
        return p;
    }

    /** §9.7.5 onverdeelde rest: the source keeps {@code total − Σ(assigned shares)}. A SEPARATE rule that modifies the
     *  SOURCE (so it cannot cancel the per-recipient share activations); method-agnostic and RECOMPUTABLE — it re-fires
     *  whenever the share set changes (a §9.4 rule-derived recipient may arrive late, so a fire-once freeze would lock
     *  in a partial sum). The {@code not Doel(filter, doelAttr == null)} guard keeps it from summing a recipient
     *  mid-assignment. Termination: the source pattern watches the source amount only (NOT the restant it writes), so
     *  its own update does not re-wake it (and the share rule's source pattern likewise does not watch restant); the
     *  recipient patterns watch the join + the assigned share, so the rest recomputes exactly when a share lands.
     *  Mirrors the DRL "(onverdeelde rest)" rule. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private Rule restantRegel(ResolvedVerdeling v, String join, boolean vereisGewicht, Class<?> bronClass, Class<?> doelClass) {
        seq = 0;
        String bronAttr = v.bronAttribuut(), doelAttr = v.doelAttribuut(), restAttr = v.restantAttribuut(), gewAttr = v.ratoAttribuut();
        Variable srcVar = DSL.declarationOf(bronClass, "$src");
        PatternDSL.PatternDef srcPat = PatternDSL.pattern(srcVar)
                .expr(nextId("bnn"), (Predicate1) s -> RegelSpraakRuntime.get(s, bronAttr) != null)
                .watch(prop(bronAttr));   // NOT prop(restAttr): the rest's own write must not re-wake it / the share rule
        srcPat = geldigGuard(srcPat, v.geldigheid());   // §4.2 window
        srcPat = gateVoorwaarde(srcPat, srcVar, v);     // §13.4.10 voorwaarde gate (whole family)
        // not Doel( filter, doelAttr == null ) — no recipient is mid-assignment. The Deelnemer patterns stay
        // class-reactive (they MUST react to aandeel becoming set); they write no Deelnemer, so they cannot loop.
        Variable notVar = DSL.declarationOf(doelClass, "$u0_" + (seq++));
        PatternDSL.PatternDef notPat = recipientFilterPattern(notVar, srcVar, join, vereisGewicht, gewAttr, doelAttr)
                .expr(nextId("unset"), (Predicate1) d -> RegelSpraakRuntime.get(d, doelAttr) == null);
        // accumulate Σ of the assigned shares.
        Variable uVar = DSL.declarationOf(doelClass, "$u_" + (seq++));
        PatternDSL.PatternDef accSrc = recipientFilterPattern(uVar, srcVar, join, vereisGewicht, gewAttr, doelAttr)
                .expr(nextId("set"), (Predicate1) d -> RegelSpraakRuntime.get(d, doelAttr) != null);
        Variable amtVar = DSL.declarationOf(BigDecimal.class, "$amt_" + (seq++));
        accSrc = accSrc.bind(amtVar, (Function1) d -> RegelSpraakRuntime.get(d, doelAttr));
        Variable sumVar = DSL.declarationOf(Object.class, "$uit_" + (seq++));
        RuleItemBuilder<?> acc = DSL.accumulate(accSrc, DSL.accFunction(accFn("BigDecimalSumAccumulateFunction"), amtVar).as(sumVar));
        Block3<org.drools.model.Drools, Object, Object> body = (drools, s, uit) -> {
            BigDecimal totaal = (BigDecimal) RegelSpraakRuntime.get(s, bronAttr);
            BigDecimal sum = RegelSpraakRuntime.toBd(uit);
            // idempotent (anti-loop): the body still RECOMPUTES the rest every fire (so a late share recomputes it),
            // only the no-op re-propagation is suppressed — the source pattern already watches bronAttr (not restAttr).
            werkBijReflectief(drools, s, restAttr, totaal.subtract(sum == null ? BigDecimal.ZERO : sum));
        };
        // §9.7.5: require ≥1 recipient in the distribution set — a verdeling with NO recipients distributes nothing, so
        // its rest stays leeg (ref/clingo pin it so); without this the accumulate Σ over the empty set is 0 and the rule
        // would write restant = totaal. Mirrors the DRL "exists <Doel>(<filter>)" guard.
        Variable exVar = DSL.declarationOf(doelClass, "$e_" + (seq++));
        PatternDSL.PatternDef exPat = recipientFilterPattern(exVar, srcVar, join, vereisGewicht, gewAttr, doelAttr);
        return PatternDSL.rule(kop(v.naam() + " (onverdeelde rest)")).attribute(Rule.Attribute.NO_LOOP, true)
                .build(srcPat, DSL.exists(exPat), DSL.not(notPat), acc, DSL.on(srcVar, (Variable) sumVar).execute(body));
    }

    private List<Rule> skipList(String reason) { overgeslagen.add(reason); return List.of(); }

    /** How many fact types RELATE {@code van} and {@code naar} in ch. 5.5's sense — the same count ref's
     *  {@code connecting_pair} takes: a TWO-role fact type whose two roles are filled by the two types (in either
     *  order), plus, when both types are the same, a §13.3.9.2 wederkerig ONE-role fact type. 0 is the LINKLESS
     *  creation shape (ref mints without a link, no fault); 1 is derivable; more than one is ch. 5.5's AMBIGUITY,
     *  which ref faults on. Deliberately NOT {@code verbindendeRol}, which collapses 0 and >1 into one null. */
    private int verbindendeFeittypen(Objecttype van, Objecttype naar) {
        if (van == null || naar == null) return 0;
        int n = 0;
        for (RegelSpraakModel.Feittype ft : model.feittypen.values()) {
            List<RegelSpraakModel.Rol> r = ft.rollen;
            if (r.size() == 2) {
                boolean ab = van.naam.equalsIgnoreCase(r.get(0).objecttypeNaam()) && naar.naam.equalsIgnoreCase(r.get(1).objecttypeNaam());
                boolean ba = van.naam.equalsIgnoreCase(r.get(1).objecttypeNaam()) && naar.naam.equalsIgnoreCase(r.get(0).objecttypeNaam());
                if (ab || ba) n++;
            } else if (r.size() == 1 && ft.wederkerig && van == naar
                    && van.naam.equalsIgnoreCase(r.get(0).objecttypeNaam())) {
                n++;
            }
        }
        return n;
    }

    // ---------------- §13.4.6 objectcreatie / §13.4.7 feitcreatie (Phase G) ----------------
    /** §13.4.6 objectcreatie: the subject creates a NEW object of {@code nieuwType}, sets each toekenning value, links
     *  new object and subject over the back-link relation, and inserts it. Gated by the voorwaarde (incl. a §8.1.7
     *  far-side exists). null -> skip (unresolved type / no fact class / NEITHER link end the bean model can hold /
     *  a toekenning value out of the scalar scope / >1 global).
     *
     *  <p>Three deliberate departures from {@code DrlTranspiler.objectCreatie} (the ported production semantics),
     *  each implementing {@code RUNTIME-SEMANTICS.md} ch. 5.5 where production does not — the spec is the product:
     *  <ul>
     *    <li><b>Both ends</b> of the back-link are written — each end the bean model carries a field for.
     *        Production writes only the created object's back-reference, so a fold over the created type finds the
     *        mint but a to-one navigation FROM the subject reads leeg. The forward field is written and the
     *        subject updated. A to-many end has no field at all (it is stored as the other end's), so it is left
     *        alone; only a pair with NO writable end is refused.</li>
     *    <li>The <b>1:1 idempotence guard</b> reads the SUBJECT-side stored link (the forward field) rather than
     *        the created object's back-reference — the field that answers ch. 5.5's question, "is the
     *        created-side role already FILLED TOWARD the subject" (the subject fills the other role), and the
     *        one the interpreter reads. Since ch. 5.7a the two
     *        ends carry the same relation (the case-input layer writes both on a 1:1 shape), so this is a choice
     *        of field, no longer a choice of reading.</li>
     *    <li>A <b>kenmerkwaardetoekenning</b> (§13.4.6.4) sets the mark only when its value is true; false or
     *        empty leaves it unset, instead of unboxing a leeg into the primitive boolean field.</li>
     *  </ul>
     *
     *  <p>§9 idempotence: no-loop does not stop the rule re-firing when ANOTHER rule later modifies a matched subject
     *  (which would mint a duplicate), so the LHS carries an absence guard — a 1:1 target keys on the subject's
     *  forward link being empty; a to-many target (or a missing forward field) keys on a per-rule per-subject
     *  {@link CreationMarker} (inserted in the consequence). */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private Rule objectCreatie(io.github.yhavinga.regelspraak.ir.ResolvedObjectCreatie oc) {
        Objecttype nieuw = oc.nieuwType(), subject = oc.subject();
        if (subject == null || nieuw == null) return skip("objectcreatie (onopgelost type): " + oc.naam());
        Class<?> subjClass = factClass(subject), nieuwClass = factClass(nieuw);
        if (subjClass == null || nieuwClass == null) return skip("objectcreatie (geen fact-class): " + oc.naam());
        // ch. 5.5's two link roles, resolved by ONE of two routes.
        //   Route 1 — the creation's own symbolic verbindendeRol binding (IR-SPEC ch. 3.3, riding beside the model
        //   because the vendored record cannot carry it). It names the role the CREATED object plays; the SIBLING
        //   role in the same feittype is the one the subject plays. AUTHORITATIVE, exactly as ref reads it
        //   (eval.ml run_object_creatie: "the explicit back-link binding wins over the type-pair derivation — the
        //   only route for a self-typed pair"). For a SELF-TYPED pair it is the only route there is: the by-pair
        //   search counts two same-typed sibling roles and derives null by construction, and
        //   FactModelGenerator.zelfpaarRollen deliberately leaves that search ambiguous while deriving the FIELDS.
        //   Route 2 — the by-pair derivation, for a creation whose wire record carries no binding (v0's shape).
        String backRol, fwdRol;                                  // subject-side (field on $new) / created-side (field on $subject)
        boolean backVeel, fwdVeel;
        RegelSpraakModel.RolBindingRef vr = rolBindingen.get(oc);
        if (vr != null) {
            RegelSpraakModel.Rol[] paar = FactModelGenerator.gebondenRolpaar(model, vr);
            if (paar == null)
                return skip("objectcreatie (de verbindendeRol-binding '" + vr.feittype() + "." + vr.rol()
                        + "' wijst geen rolpaar aan): " + oc.naam());
            fwdRol = paar[0].naam(); fwdVeel = paar[0].meervoudig();
            backRol = paar[1].naam(); backVeel = paar[1].meervoudig();
        } else {
            backRol = model.verbindendeRol(nieuw, subject);
            fwdRol = model.verbindendeRol(subject, nieuw);
            backVeel = backRol != null && model.naarVeelRol(nieuw, subject, backRol);
            fwdVeel = fwdRol != null && model.naarVeelRol(subject, nieuw, fwdRol);
        }
        // Only a to-ONE role has a bean field at all: FactModelGenerator.leden derives the scalar reference on
        // the ONE side, so a to-many role is stored solely as the OTHER end's field, and writing it anyway would
        // reach a setter no bean carries (an unclassified "geen setter voor" ERROR at fire, never a valid
        // not-carried outcome under the gate-12 regime). BOTH ends get that filter, SYMMETRICALLY: the end whose
        // role is meervoudig is simply NOT written; the other end still carries ch. 5.5's link, and a creation
        // with one writable end is played, not refused — a refusal there would withhold a value the backend can
        // produce (checked: on a verifier-clean model whose SUBJECT-side role is meervoudig, ref plays the
        // creation and the navigated read from the subject reaches the mint).
        String backRef = backRol != null && !backVeel ? backRol : null;
        String fwdRef = fwdRol != null && !fwdVeel ? fwdRol : null;
        // ...and a to-ONE role is only a WRITABLE end when the bean carries its reference field TYPED. leden()'s
        // name-dedup (attribuut > kenmerk > referentie) lets a declared member of the same name displace it — a
        // Wachter whose self-pair role `opvolger` is also a Numeriek attribute keeps the PROPERTY and loses the
        // REFERENCE — and the writes below would then push a fact into a BigDecimal field: ClassCastException at
        // fire, an unclassified ERROR where main gave a clean per-form EXCLUDE (lens 2, 2026-08-30). Refused per
        // creation, with the displacing member named, never guessed and never crashed. NOT nulled-out like a
        // to-many end either: that end's link is stored elsewhere BY DESIGN, while this one cannot be stored at
        // all, so writing only the other half would leave ch. 5.5's link silently one-sided.
        if (backRef != null && !heeftReferentieVeld(nieuw, backRef, subject))
            return skip("objectcreatie (" + geenReferentieveldReden(nieuw, backRef, subject) + "): " + oc.naam());
        if (fwdRef != null && !heeftReferentieVeld(subject, fwdRef, nieuw))
            return skip("objectcreatie (" + geenReferentieveldReden(subject, fwdRef, nieuw) + "): " + oc.naam());
        // Only when NEITHER end carries a bean field is the mint truly an ORPHAN — inserted but reachable from
        // nothing, so no navigated read or fold could ever see it. RUNTIME-SEMANTICS ch. 5.5 links both ends; a
        // link the bean model cannot hold ANYWHERE is NOT-CARRIED, refused loudly, never a silently wrong value.
        if (backRef == null && fwdRef == null && vr == null && subject != nieuw
                && verbindendeFeittypen(subject, nieuw) > 1)
            // ch. 5.5's REFUSAL TO GUESS — and the card's lowering-vs-fire question, answered ONCE for both sides.
            // The MOMENT already differs across the square (ref raises it during evaluation, the ASP backend at
            // translation), so it is a phase-structure detail. The CATEGORY is not: an ambiguous back-link is a
            // property of the PROGRAM, not a gap in this backend — the §13.4.6 form IS carried, every other
            // creation lowers and fires — so reporting it as "not carried" would file a faulty program under
            // "unsupported". It therefore lowers to a rule that FAULTS at fire (the shape uniciteit's invalid key
            // shape already uses), worded as ref words it so the red fence pins ONE fragment across all three
            // executors. NARROWING, recorded: this rule needs a subject instance to fire, where ref faults even on
            // an empty world. The neighbouring shapes below stay honest lowering REFUSALS, because those really are
            // backend gaps — a link no bean field can hold anywhere, which ref does not fault on either (a
            // self-typed pair without a binding mints unlinked there; a to-many pair links through role lists).
            return vormFoutRegel(kop(oc.naam()), subjClass, oc.geldigheid(), "more than one fact type relates \""
                    + subject.naam + "\" to \"" + nieuw.naam + "\"; the created object's back-link is ambiguous (ch. 5.5)");
        if (backRef == null && fwdRef == null)
            // The three causes are DIFFERENT and are named apart: an ambiguous pair is ch. 5.5's refusal to guess
            // (the interpreter faults there), a self-typed pair is a bean-model gap (the reference field is
            // derivable only from the creation's symbolic role binding, which this bean model does not carry),
            // and a pair whose BOTH roles are meervoudig resolves fine but derives no field on either side.
            return skip("objectcreatie (" + (vr == null && subject == nieuw
                    ? "zelfgetypeerd rollenpaar zonder verbindendeRol-binding: uit twee gelijkgetypeerde rollen leidt"
                      + " de bean geen referentieveld af en de draad wijst geen rol aan"
                    : vr == null && (backRol == null || fwdRol == null)
                    ? "geen uniek verbindend feittype: de terugkoppeling is niet af te leiden en ch. 5.5 gist niet"
                    : "beide rollen zijn meervoudig ('" + backRol + "' en '" + fwdRol + "'), dus FactModelGenerator"
                      + " leidt aan geen van beide zijden een referentieveld af")
                    + ", dus het gecreëerde object zou onbereikbaar zijn): " + oc.naam());
        // §9 guard shape (see doc). With a BINDING the cardinality is read off the bound role itself — the same
        // reading ref applies (`Some (_, new_role, _) when not new_role.rol_meervoudig`); without one, the by-pair
        // subjectNaarVeelCreatie stands. INVARIANT (either route): !to-many(created-side role) ≡ fwdRef != null, so
        // the 1:1 arm below always HAS the forward field its guard reads. Checked, not assumed: on the by-pair route
        // the two derivations sit in different classes and the guard would otherwise read a null field name.
        boolean eenOpEen = vr != null ? !fwdVeel : !model.subjectNaarVeelCreatie(subject, nieuw);
        if (eenOpEen != (fwdRef != null))
            throw new IllegalStateException("objectcreatie " + oc.naam() + ": subjectNaarVeelCreatie(" + eenOpEen
                    + ") en het voorwaartse veld (" + fwdRef + ") spreken elkaar tegen");

        // Lower each toekenning value to a §13.4.6.2 value producer (TkLower). The runtime receiver is the anchor
        // $subject (body below applies it to args[0]), so the anchor objecttype rides as the VALUE-path subject — a
        // to-one navigated or §13.4.1 multi-hop chain value resolves against it, mirroring DrlTranspiler.objectCreatie's
        // waarde(acc, t.waarde(), subject). The chain gate (ketenWaardePad) is set here for the same reason as the
        // scalar THEN-waarde: a toekenning value IS a consequence value.
        //   - A NON-aggregation value stays on the existing scalarWaarde WaardeFn path (navigated chains / builtins /
        //     dimensiecel that lowerVal does not cover; it also restates into the target unit internally).
        //   - A §5.8 AGGREGATION-valued toekenning ("het aantal elementen van de houder maal <param>", the
        //     count is NOT an attribute) cannot host its accumulate CE inside a WaardeFn, so it lowers via lowerVal to
        //     an Eval + its own accumulate CE(s) + result Variable(s) threaded through the consequence's aggsAll[],
        //     exactly like the regel aggregatie-in-arithmetiek path (L454-486). DrlTranspiler.objectCreatie likewise
        //     collects an embedded aggregation into the "when" via its Acc context.
        seq = 0;                                                     // aggregatie() names its patterns off seq
        Variable sVar = DSL.declarationOf(subjClass, "$subject");
        List<TkLower> tks = new ArrayList<>();
        Set<String> gNames = new LinkedHashSet<>();
        List<RuleItemBuilder<?>> accItems = new ArrayList<>();       // all aggregation accumulate CEs, across toekenningen
        List<Variable> accVars = new ArrayList<>();                  // all aggregation result vars, in order (aggsAll[])
        try {
            ketenWaardePad = true;
            for (var t : oc.toekenningen()) {
                Datatype naar = nieuw.attribuut(t.attribuut());     // the new object's attribute unit — the §4 restate target
                if (containsAggregatie(t.waarde())) {
                    List<Variable> tAccVars = new ArrayList<>();     // THIS toekenning's slice of the shared accVars
                    Eval eval = lowerVal(t.waarde(), subject, sVar, gNames, accItems, tAccVars);
                    if (eval == null) return skip("objectcreatie (aggregatie-toekenning buiten scope): " + oc.naam());
                    int off = accVars.size(); accVars.addAll(tAccVars);   // remember offset BEFORE appending
                    tks.add(new TkLower(t.attribuut(), null, eval, off, tAccVars.size(), effType(t.waarde()), naar, t.kenmerk()));
                } else {
                    WaardeFn wf = scalarWaarde(subject, t.waarde());
                    if (wf == null) return skip("objectcreatie ("
                            + (navReden != null ? navReden : "toekenning-waarde buiten scope") + "): " + oc.naam());
                    gNames.addAll(wf.globals());
                    tks.add(new TkLower(t.attribuut(), wf.fn(), null, 0, 0, effType(t.waarde()), naar, t.kenmerk()));
                }
            }
        } finally {
            ketenWaardePad = false;
        }
        List<String> gList = new ArrayList<>(gNames);

        PatternDSL.PatternDef pattern = PatternDSL.pattern(sVar);
        List<RuleItemBuilder<?>> extra = new ArrayList<>();
        if (oc.voorwaarde() != null) {
            Gate gate = voorwaarde(oc.voorwaarde(), pattern, subject, sVar);
            if (gate == null) return skip("objectcreatie ("
                    + (navReden != null ? navReden : "voorwaarde buiten scope") + "): " + oc.naam());
            pattern = gate.pattern();
            extra.addAll(gate.extra());
        }
        pattern = geldigGuard(pattern, oc.geldigheid());   // §4.2 version validity window

        // §9 idempotence guard: 1:1 -> not <NieuwType>(backRef == $subject); to-many/orphan -> not CreationMarker(
        // regel == naam, subject == $subject). The marker case ALSO inserts the marker in the consequence body.
        final String ruleNaam = oc.naam();
        RuleItemBuilder<?> idemGuard = null;
        if (eenOpEen) {
            // ch. 5.5, the 1:1 linked-instance guard read as the interpreter reads it: the SUBJECT-side stored
            // link on the created-side role (ref's role_links over new_role), i.e. the forward field the
            // consequence below writes. That field answers ch. 5.5's actual question — is the created-side
            // role already FILLED TOWARD the subject (the subject itself fills the OTHER role, §8.1.7's sense)
            // — and reading it also makes the write below safe: it can never hand an already-filled to-one
            // role a second filler.
            // NB this is a choice of FIELD, not of reading. Until 2026-09-10 it was also the latter: a case
            // delivering the link solely on the created side did not block, recorded in the spec as the
            // storage-direction narrowing. ch. 5.7a WITHDREW that narrowing — a supplied link establishes the
            // relation and the case-input layer writes both fields on a 1:1 shape — so both ends now carry the
            // same relation and either field would answer the same. The anchor jutterswet-rekenkamer case 4
            // pins it: a created-side delivery blocks, in ref, clingo and here.
            // Only the DIRECTION follows ref; the MOMENT does not. RUNTIME-SEMANTICS.md r1673-1680 notes the two
            // narrowings apart: ref reads the world0 SNAPSHOT (a link written during the run is invisible to its
            // guard), this predicate reads the LIVE fact base. Deliberate — a Drools LHS has no world0 to read.
            final String fFwdRef1 = fwdRef;
            pattern = pattern.expr(nextId("idem"), (Predicate1) o -> RegelSpraakRuntime.get(o, fFwdRef1) == null);
        } else {
            Variable markVar = DSL.declarationOf(CreationMarker.class, "$mark_" + (seq++));
            PatternDSL.PatternDef markPat = PatternDSL.pattern(markVar)
                    .expr(nextId("idem"), (Predicate1) mk -> ruleNaam.equals(((CreationMarker) mk).getRegel()))
                    .expr(nextId("idem"), (Variable) sVar, (Predicate2) (mk, s) -> ((CreationMarker) mk).getSubject() == s);
            idemGuard = DSL.not(markPat);
        }
        final boolean fMarker = !eenOpEen;

        final List<TkLower> fTks = tks;
        final String fBackRef = backRef; final String fFwdRef = fwdRef; final Class<?> fNieuwClass = nieuwClass;
        final List<String> fG = gList; final int nG = gList.size(); final int nAgg = accVars.size();
        java.util.function.BiConsumer<org.drools.model.Drools, Object[]> body = (drools, args) -> {
            Object s = args[0];
            Map<String, BigDecimal> gm = new LinkedHashMap<>();
            for (int i = 0; i < nG; i++) gm.put(fG.get(i), (BigDecimal) args[1 + i]);
            Object[] aggsAll = new Object[nAgg];                     // the aggregation result vars, bound after [subject, globals...]
            for (int i = 0; i < nAgg; i++) aggsAll[i] = args[1 + nG + i];
            Object newObj = RegelSpraakRuntime.newInstance(fNieuwClass);
            for (TkLower tk : fTks) {
                Object v;
                if (tk.fn() != null) {
                    v = tk.fn().apply(s, gm);                        // scalar path already restates inside scalarWaarde — do NOT double-restate
                } else {
                    Object[] slice = java.util.Arrays.copyOfRange(aggsAll, tk.aggOff(), tk.aggOff() + tk.aggCnt());
                    v = herschaalWaarde(tk.eval().apply(s, gm, slice), tk.van(), tk.naar());   // §4 EXACT restate the aggregate into the attribuut unit
                }
                if (tk.kenmerk()) {
                    // §13.4.6.4 kenmerkwaardetoekenning, ch. 5.5's reading: the mark is SET when the value
                    // evaluates true; false OR EMPTY leaves it unset — equivalent to an absent initialisation
                    // under the closed-world reading a characteristic check applies. The bean field is a
                    // primitive boolean whose default IS unset, so "leave it" is literally a skipped write;
                    // writing the leeg through would unbox null (the intValue() NPE this arm replaces).
                    if (Boolean.TRUE.equals(v)) RegelSpraakRuntime.set(newObj, tk.attr(), Boolean.TRUE);
                    else if (v != null && !(v instanceof Boolean))
                        throw new IllegalStateException("kenmerkinitialisatie van '" + tk.attr()
                                + "' is niet booleaans: " + v + " (WF-027)");
                    continue;
                }
                // §6.1.2 write boundary: collapse a remaining exact Breuk (a computed met-clause value) to its decimal
                // form before the reflective set, so it never reaches a BigDecimal field (mirrors werkBij).
                RegelSpraakRuntime.set(newObj, tk.attr(), collapseBreuk(v));
            }
            if (fBackRef != null) RegelSpraakRuntime.set(newObj, fBackRef, s);
            drools.insert(newObj);
            if (fMarker) drools.insert(new CreationMarker(ruleNaam, s));   // §9 mark this (rule, subject) created
            if (fFwdRef != null) {
                // ch. 5.5: new object and subject are linked over the back-link relation — BOTH ends, so a
                // navigated read from the subject reaches the mint. The production toolchain writes only the
                // created object's end (a fold over the created type still finds it, a to-one navigation never
                // does); this backend implements the spec. The update is required, not cosmetic: the subject is
                // an already-matched fact, so without it every rule that navigates to the mint keeps its
                // pre-creation match. It cannot re-trigger this rule — the guard above reads exactly the field
                // just filled (1:1), and the marker cancels the activation otherwise.
                RegelSpraakRuntime.set(s, fFwdRef, newObj);
                drools.update(s);
            }
        };
        List<Variable> vars = new ArrayList<>();
        vars.add(sVar);
        for (String gn : gList) {
            Global<BigDecimal> g = globals.get(gn);
            if (g == null) return skip("objectcreatie (niet-geregistreerde global): " + oc.naam());
            vars.add((Variable) g);
        }
        vars.addAll(accVars);                                       // [subject, globals..., accVars...] — aggsAll[] reads from args[1+nG..]
        RuleItemBuilder<?> consequence = consequenceOf(vars.toArray(new Variable[0]), body);
        if (consequence == null) return skip("objectcreatie (te veel gebonden variabelen): " + oc.naam());

        // accumulate CEs join the related elements to $subject, so they follow the subject pattern + idempotence guard
        // (like the regel path's extra.addAll(accItems) after the doel pattern). Order: pattern; extra; idemGuard; accItems; consequence.
        // idemGuard is null on the 1:1 path — that guard now rides the subject pattern itself (see above).
        List<RuleItemBuilder<?>> items = new ArrayList<>();
        items.add(pattern); items.addAll(extra);
        if (idemGuard != null) items.add(idemGuard);
        items.addAll(accItems); items.add(consequence);
        return PatternDSL.rule(kop(oc.naam())).build(items.toArray(new RuleItemBuilder[0]));
    }

    /** §13.4.7 feitcreatie: for each {@code anker} and each {@code doel} ALREADY related to it (the §9.4 path: the
     *  optional multi-hop {@code tussenstappen} then the final {@code joinVeld}), CREATE the new relation by setting
     *  {@code zetVeld} on the doel -> {@code modify($doel){ set zetVeld($subject) }}, NO_LOOP. The voorwaarde gates the
     *  anker (its pattern) or the doel (its pattern); a guard lowerable to NEITHER is REFUSED by name, never lowered
     *  to a silent never-fire. Mirrors DrlTranspiler.feitCreatie. null -> skip (unresolved). */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private Rule feitCreatie(io.github.yhavinga.regelspraak.ir.ResolvedFeitCreatie fc) {
        Objecttype anker = fc.anker(), doel = fc.doel();
        // The bean fields come from the role bindings, never from the wire's zetVeld/joinVeld/tussenstappen
        // (V1-WIREMIGRATIE P2): those go at B7, and ref executes the bindings alone.
        FactModelGenerator.Creatievelden velden = FactModelGenerator.creatievelden(model, fc);
        if (anker == null || doel == null || velden == null)
            return skip("feitcreatie (onopgelost): " + fc.naam());
        Class<?> ankerClass = factClass(anker), doelClass = factClass(doel);
        if (ankerClass == null || doelClass == null) return skip("feitcreatie (geen fact-class): " + fc.naam());

        seq = 0;
        Variable sVar = DSL.declarationOf(ankerClass, "$subject");
        PatternDSL.PatternDef ankerPat = geldigGuard(PatternDSL.pattern(sVar), fc.geldigheid());   // §4.2 window (on the anker)
        List<RuleItemBuilder<?>> ankerExtra = new ArrayList<>();
        // §8.1.7 the voorwaarde: try the ANKER side first (its constraints + any far-side exists ride the anker
        // pattern); if it cannot lower there, try the DOEL side (an in-pattern predicate on $doel); else INERT.
        Predicate1<Object> doelGuard = null;
        boolean inert = false;
        if (fc.voorwaarde() != null) {
            Gate g = voorwaarde(fc.voorwaarde(), ankerPat, anker, sVar);
            if (g != null) {
                ankerPat = g.pattern();
                ankerExtra.addAll(g.extra());
            } else if (!Gastheer.doelpogingToegestaan(fc.voorwaarde())) {
                // Under a whole-period qualifier the doel is not a bound object (WF-109), so the doel reading this
                // branch used to give — the sister branch sank at compile on the same guard — is refused by name.
                io.github.yhavinga.regelspraak.ir.Bron gb = fc.voorwaarde().bron();
                String snip = gb == null ? null : gb.snippet();
                return skip("feitcreatie (" + Gastheer.DOELPOGING_GEHEELPERIODE
                        + (snip == null || snip.isBlank() ? "" : ": " + snip) + "): " + fc.naam());
            } else {
                doelGuard = leafPredicate(fc.voorwaarde(), doel);   // a fact-only predicate on the doel
                if (doelGuard == null) inert = true;                // lowerable to NEITHER -> never-fire, never over-create
            }
        }
        // A guard that lowers to NEITHER side is a REFUSAL, not a nul-creatie. Until 2026-09-19 it emitted no rule
        // and recorded nothing, which the manifest reads as LOWERED: the document PLAYED with the creation's count
        // at zero. But "created nothing" is also the honest answer for a guard that is merely false, and the two are
        // not the same document. The forms that reach here are the UNREADABLE ones — measured, both verifier-clean on
        // jutterswet-postwacht: an Onopgelost guard (the shape the frontend emits when doel and anker are the same
        // type; ref EVALUATION FAULT WF-044, clingo TRANSLATION FAULT) and a kenmerkcheck hosted on a THIRD type
        // (ref and clingo both WF-100). A fault that becomes an ordinary zero in the third executor is exactly what
        // the differential exists to catch, and the played channel counted it as agreement.
        // The DRL-text branch has refused it by name since its guard arm was written (an eval(false) carrying the
        // marker, hence a MARKER verdict); this records the SAME sentence, plus the guard's own source text, so the
        // gate report can tell the two forms apart. The over-creation the old comment worried about is not at stake:
        // a refused construct creates nothing either.
        if (inert) {
            io.github.yhavinga.regelspraak.ir.Bron gb = fc.voorwaarde().bron();
            String snip = gb == null ? null : gb.snippet();
            return skip("feitcreatie (voorwaarde niet naar anker of doel te lowereren"
                    + (snip == null || snip.isBlank() ? "" : ": " + snip) + "): " + fc.naam());
        }

        // §9.4 multi-hop: bind each intermediate object, joining to the previous; $prev starts at the anker.
        // §13.3.9 STORAGE DIRECTION, at all THREE positions this construct touches a role field (2026-09-11): each
        // hop's join, the doel's join onto the last hop, and the zetVeld the consequence WRITES. The objectcreatie
        // arm has asked this since 2026-08-30; the feitcreatie asked nothing, and each position was measured on a
        // verifier-clean, ref-clean model:
        //     zetVeld displaced by a declared member -> ERROR ConsequenceException: ClassCastException: Cannot cast
        //         …facts.Kustvak to java.math.BigDecimal      (DRL branch: "Unable to Analyse Expression
        //         $doel.setSeinvak( $subject )")
        //     a hop whose field the parent's bean does not carry -> ERROR ConstraintEvaluationException at first
        //         fire  (DRL branch: "Unable to Analyse Expression this == $subject.gemeenteVanInschrijving")
        //     joinVeld displaced -> the join never matches, so the creation makes nothing; ref makes nothing there
        //         either, because the only routes that could establish the relation it reads (the case input, a
        //         sibling creation) are themselves refused. Gated with the other two all the same: it is the same
        //         field, the same question, and the UNDERIVED half of it is the crashing one.
        List<RuleItemBuilder<?>> items = new ArrayList<>();
        items.add(ankerPat);
        items.addAll(ankerExtra);
        Variable vorige = sVar;
        Objecttype vorigeType = anker;
        for (FactModelGenerator.Creatiehop stap : velden.hops()) {
            Class<?> stapClass = factClass(stap.type());
            if (stapClass == null) return skip("feitcreatie (geen fact-class tussenstap): " + fc.naam());
            String veld = stap.veld();
            boolean viaOuder = stap.viaOuderVeld();
            // the hop's field lives on the PARENT for a to-one hop (viaOuderVeld) and on the CHILD otherwise —
            // exactly the two sides the predicate below reads, so the question is asked of the side that is read.
            Objecttype hopDrager = viaOuder ? vorigeType : stap.type(), hopDoel = viaOuder ? stap.type() : vorigeType;
            if (!heeftReferentieVeld(hopDrager, veld, hopDoel))
                return skip("feitcreatie (" + geenReferentieveldReden(hopDrager, veld, hopDoel) + "): " + fc.naam());
            Variable stapVar = DSL.declarationOf(stapClass, "$t_" + (seq++));
            Variable prev = vorige;
            // a to-one hop reads the PARENT's forward ref (this == $prev.veld); a to-many hop reads the CHILD's backref (veld == $prev).
            PatternDSL.PatternDef stapPat = PatternDSL.pattern(stapVar).expr(nextId("hop"), (Variable) prev,
                    (Predicate2) (o, p) -> viaOuder ? RegelSpraakRuntime.get(p, veld) == o : RegelSpraakRuntime.get(o, veld) == p);
            items.add(stapPat);
            vorige = stapVar;
            vorigeType = stap.type();
        }

        String joinVeld = velden.joinVeld(), zetVeld = velden.zetVeld();
        if (!heeftReferentieVeld(doel, joinVeld, vorigeType))
            return skip("feitcreatie (" + geenReferentieveldReden(doel, joinVeld, vorigeType) + "): " + fc.naam());
        if (!heeftReferentieVeld(doel, zetVeld, anker))
            return skip("feitcreatie (" + geenReferentieveldReden(doel, zetVeld, anker) + "): " + fc.naam());
        Variable doelVar = DSL.declarationOf(doelClass, "$doel");
        Variable laatste = vorige;
        PatternDSL.PatternDef doelPat = PatternDSL.pattern(doelVar).expr(nextId("djoin"), (Variable) laatste,
                (Predicate2) (d, p) -> RegelSpraakRuntime.get(d, joinVeld) == p);
        if (doelGuard != null) doelPat = doelPat.expr(nextId("dg"), (Predicate1) doelGuard);
        items.add(doelPat);

        // create the relation: set the anker-role reference on $doel; NO_LOOP (the modify must not re-fire the rule),
        // and idempotent (anti-loop): re-setting an already-established ref must not re-propagate $doel through a
        // co-resident black-box rule over the same type.
        Block3<org.drools.model.Drools, Object, Object> body = (drools, subj, d) ->
            werkBijReflectief(drools, d, zetVeld, subj);
        items.add(DSL.on(sVar, (Variable) doelVar).execute(body));
        return PatternDSL.rule(kop(fc.naam())).attribute(Rule.Attribute.NO_LOOP, true).build(items.toArray(new RuleItemBuilder[0]));
    }

    /** A null-safe {@code (subject -> value)} reader for a §6.5/§6.6 fout-companion OPERAND, or null when the operand
     *  has no re-readable leeg/vol guard. Accepts the subject's OWN attribute and a to-ONE NAVIGATED attribute
     *  (root-guarded -> a null relation reads leeg), mirroring DrlTranspiler.companionDeel's EIGEN / GENAVIGEERD
     *  acceptance. Null for a literal / parameter / to-many / unresolvable operand and for a TEMPORAL attribute (whose
     *  List leeg/vol would test the timeline's nullness, not per-moment leegte — DRL markers it too), so such a
     *  division/wortel raises no Inconsistentie in EITHER back-end. */
    private java.util.function.Function<Object, Object> companionOperand(Expr e, Objecttype subject) {
        if (!(e instanceof Expr.AttribuutRef a) || a.objecttype() == null) return null;
        if (Namen.bevatNaam(a.objecttype().granulariteitLeden(), a.attribuut())) return null;   // temporal -> no re-readable guard
        if (!a.objecttype().dimensiesVan(a.attribuut()).isEmpty()) return null;              // §3.6 gedimensioneerd (a Map) -> DRL markers it too
        return navReader(a, subject);   // own attribute or to-one navigated (null for to-many / absent / unresolvable)
    }

    /** Whether a §6.5/§6.6 site's operand lets the site QUALIFY for a companion. Outside a ch. 4.10 node that is
     *  {@link #companionOperand} — a re-readable own or to-one attribute for the static predicate (DRL parity).
     *  UNDER one the companion's condition is the path probe, which observes the site's evaluation instead
     *  of re-reading its operands, so a ch. 4.10 binder READ qualifies too: the "Daarbij geldt" variabelendeel
     *  shape the RegelSpraak emitter produces ({@code Let n = teller in n gedeeld door deler}) — a read that,
     *  under ch. 10.6, either answers the bound value or raises the barrier before the site runs, exactly as ref
     *  evaluates it (CompanionPadWitnessTest (j)). Everything else stays out, bare and under a ch. 4.10 node alike, and
     *  raises no Inconsistentie in this backend where ref may: a Literal, a ParameterRef, a to-MANY navigation, a
     *  temporal or dimensioned attribute, and any compound operand (a nested Binair, a Functie, a Let node as the
     *  operand of a site that itself sits ABOVE every ch. 4.10 node — there the site has no probe unit). */
    private boolean kwalificeertOperand(Expr e, Expr drager, Objecttype subject) {
        if (companionOperand(e, subject) != null) return true;
        return drager != null && e instanceof Expr.BinderRef;
    }

    /** Whether {@code e} is a ch. 4.10 node: a {@link Expr.Let}, a {@link Expr.BinderRef} or an
     *  {@link Expr.IfThenElse}. Two callers — {@link #scalarWaarde}, which routes it to {@link #letWaarde}, and the two companion
     *  walks, which take the OUTERMOST such node as their §6.5/§6.6 path-probe host. For the probe the BinderRef
     *  arm is inert (a BinderRef has no {@link #letKinderen}, so no site is ever collected under one); it is named
     *  for completeness of the cluster, and because scalarWaarde does need it. */
    private boolean isLetKnoop(Expr e) {
        return e instanceof Expr.Let || e instanceof Expr.BinderRef || e instanceof Expr.IfThenElse;
    }

    /** A §6.5 division or §6.6 wortel SITE in a rule value, with the OUTERMOST ch. 4.10 node on the path from
     *  the value root down to it — null when there is none, in which case the site runs whenever the value does
     *  and its companion keeps the static leeg/negatief predicate; otherwise the companion's condition is the
     *  path probe over that node ({@link Sonde}). */
    private record Plek<T extends Expr>(T site, Expr drager) {}

    /** §6.5 Tabel 12 "waarde gedeeld door leeg" companion: the value side already degrades the division to leeg
     *  (rsGedeeld), so this surfaces the Fout via the §9.5 Inconsistentie channel — a SEPARATE insert-only rule per
     *  division that fires when the dividend is present and the divisor is empty (and the host voorwaarde holds),
     *  and — for a division under a ch. 4.10 node — only when the division's path actually RAN ({@link Sonde}).
     *  Scope: each operand is the subject's OWN attribute or one reached by a to-ONE navigation (companionOperand),
     *  or — under such a node only — a binder read ({@link #kwalificeertOperand}). Mirrors DrlTranspiler.deelDoorLeegFout. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private List<Rule> delingCompanions(ResolvedRegel r) {
        if (!(r.doel() instanceof Expr.AttribuutRef doel) || doel.objecttype() == null) return List.of();
        Objecttype subject = doel.objecttype();
        Class<?> factClass = factClass(subject);
        Class<?> incClass = inconsistentieClass();
        if (factClass == null || incClass == null) return List.of();
        List<Plek<Expr.Binair>> delingen = new ArrayList<>();
        verzamelDelingen(r.waarde(), null, delingen);
        // First collect the QUALIFYING divisions (both operands a re-readable own OR to-one navigated attribute),
        // mirroring DRL's `patronen` (companionDeel accepts EIGEN and GENAVIGEERD), so the companion-name suffix
        // indexes over the SAME filtered list in BOTH back-ends. Indexing over the raw `delingen` count diverged from
        // DRL when a rule mixes qualifying and non-qualifying divisions (e.g. a §11 division variable inlined next to a
        // parameter division): DRL named the lone qualifier "(deling door leeg)", the model "(deling door leeg 1)".
        // A path-probed division counts in the SAME index, which BREAKS that suffix parity for a rule mixing
        // bare and path-probed qualifiers. Measured with both transpilers on one model (fix round 2 of the
        // companion-onder-drager card), "(inkomen ÷ deler) plus (Let d = leegding in d plus (inkomen ÷ tweede))":
        // DRL emits one companion, "(deling door leeg)", this back-end two, "(deling door leeg 1)" and
        // "(deling door leeg 2)". DRL lowers no Let, true — but DrlTranspiler.deelDoorLeegFout returns early only
        // on a voorwaardeMarker, so it still emits the companion of the BARE site while its own verzamelDelingen
        // never descends into the Let; there IS a name to keep in step, and this does not keep it. Accepted, not
        // worked around: the companion NAME is not observable — the Inconsistentie both back-ends insert carries
        // the HOST rule name and the subject — and a separate suffix space for path-probed sites would make a
        // bare site's name depend on which OTHER sites lower. Recorded as an open point on the card.
        List<Plek<Expr.Binair>> kwalificeren = new ArrayList<>();
        for (Plek<Expr.Binair> p : delingen)
            if (kwalificeertOperand(p.site().links(), p.drager(), subject) && kwalificeertOperand(p.site().rechts(), p.drager(), subject))
                kwalificeren.add(p);
        List<Rule> companions = new ArrayList<>();
        String naam = r.naam();
        for (int i = 0; i < kwalificeren.size(); i++) {
            Plek<Expr.Binair> plek = kwalificeren.get(i);
            Expr.Binair d = plek.site();
            // null-safe readers for the dividend/divisor — own attribute or a to-one navigated read (a null relation
            // collapses to leeg, exactly as DRL's root-guarded "(partner != null && partner.x != null)" / "partner.x" forms).
            // Consulted by the STATIC predicate only, i.e. for a bare site, where both are non-null by
            // kwalificeertOperand; a path-probed site (a binder operand has no reader) decides by the probe.
            java.util.function.Function<Object, Object> dividend = companionOperand(d.links(), subject);
            java.util.function.Function<Object, Object> divisor = companionOperand(d.rechts(), subject);
            // dividend present AND divisor empty (an empty LEFT short-circuits to 0 -> no fout, so guard the dividend too).
            String compNaam = naam + " (deling door leeg" + (kwalificeren.size() > 1 ? " " + (i + 1) : "") + ")";
            Rule companion = padCompanion(r, subject, factClass, incClass, compNaam, plek,
                    pattern -> pattern.expr(nextId("dvd"), (Predicate1) s -> dividend.apply(s) != null)
                            .expr(nextId("dvs"), (Predicate1) s -> divisor.apply(s) == null));
            if (companion != null) companions.add(companion);
        }
        return companions;
    }

    /** §6.6 "wortel van een negatief getal" companion: the value side degrades a negative-radicand root to leeg
     *  (rsWortel), so this surfaces the Fout via the §9.5 Inconsistentie channel — a SEPARATE insert-only rule per
     *  wortel whose radicand (own attribute or a to-ONE navigation, companionOperand; under a ch. 4.10 node also a
     *  binder read, {@link #kwalificeertOperand}) is &lt; 0 (and the host voorwaarde holds), and — for a wortel
     *  under a ch. 4.10 node — only when its path actually RAN ({@link Sonde}). Mirrors
     *  DrlTranspiler.wortelNegatiefFout. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private List<Rule> wortelCompanions(ResolvedRegel r) {
        if (!(r.doel() instanceof Expr.AttribuutRef doel) || doel.objecttype() == null) return List.of();
        Objecttype subject = doel.objecttype();
        Class<?> factClass = factClass(subject);
        Class<?> incClass = inconsistentieClass();
        if (factClass == null || incClass == null) return List.of();
        List<Plek<Expr.Functie>> wortels = new ArrayList<>();
        verzamelWortels(r.waarde(), null, wortels);
        // Collect the QUALIFYING wortels (radicand a re-readable own OR to-one navigated attribute), so the
        // companion-name suffix indexes over the SAME filtered list as DRL's wortelNegatiefFout `guards`.
        List<Plek<Expr.Functie>> kwalificeren = new ArrayList<>();
        for (Plek<Expr.Functie> p : wortels)
            if (p.site().argumenten().size() == 1 && kwalificeertOperand(p.site().argumenten().get(0), p.drager(), subject))
                kwalificeren.add(p);
        List<Rule> companions = new ArrayList<>();
        String naam = r.naam();
        for (int i = 0; i < kwalificeren.size(); i++) {
            Plek<Expr.Functie> plek = kwalificeren.get(i);
            // null-safe reader for the radicand — own attribute or a to-one navigated read (null relation -> leeg);
            // consulted by the STATIC predicate only (a bare site), see delingCompanions.
            java.util.function.Function<Object, Object> radicand = companionOperand(plek.site().argumenten().get(0), subject);
            // radicand < 0 (a null radicand is leeg, not negative -> no fout; signum guards null out).
            String compNaam = naam + " (wortel van negatief" + (kwalificeren.size() > 1 ? " " + (i + 1) : "") + ")";
            Rule companion = padCompanion(r, subject, factClass, incClass, compNaam, plek,
                    pattern -> pattern.expr(nextId("rad"), (Predicate1) s -> {
                        Object v = radicand.apply(s);
                        return v != null && ((BigDecimal) v).signum() < 0;
                    }));
            if (companion != null) companions.add(companion);
        }
        return companions;
    }
    /** §6.8: a leeg §9.9 startpunt-anker is een Fout — surfaced via the §9.5 Inconsistentie channel by a
     *  separate insert-only companion on every startpunt write whose anchor decomposes to a re-readable
     *  leeg guard (companionOperand: own attribute or a to-ONE navigation). A datum-arithmetic anchor is
     *  leeg exactly when its leftmost datum operand is (Tabel 15: a leeg duration is 0/unchanged); a
     *  parameter or function anchor still degrades to a leeg write but raises no Inconsistentie (the same
     *  scope limit as the §6.5/§6.6 companions). Mirrors DrlTranspiler.startpuntLeegFout.
     *
     *  <p><b>Why the leftmost peel below does NOT descend through a ch. 4.10 node</b> (unlike
     *  {@link #verzamelDelingen} / {@link #verzamelWortels} / {@link #kanBreukOpleveren}). Measured, not
     *  assumed: on a two-flank pair — a startpunt-lid rule whose anchor is a leeg own attribute, once bare and
     *  once wrapped in {@code Let d = basis in d} — ref answers CONSISTENT on BOTH (it has no §6.8
     *  leeg-anchor check at all; the fault is production-RegelSpraak semantics this port carries and the spec
     *  does not). The bare flank therefore already diverges — Drools inconsistent, ref consistent — and the
     *  ch.-4.10-blindness of the peel is what makes the WRAPPED flank agree with ref. So this walk does NOT
     *  share the §6.5/§6.6/§Breuk gap class (blind walk → missing fault → silently wrong); descending would
     *  ADD a ref-divergence rather than remove one. Left as is deliberately: the §6.8-vs-ref question is a
     *  spec matter, out of this card's scope, and no corpus golden writes a startpunt lid by rule. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private List<Rule> startpuntCompanions(ResolvedRegel r) {
        if (!(r.doel() instanceof Expr.AttribuutRef doel) || doel.objecttype() == null
                || !model.isStartpuntLid(doel.objecttype(), doel.attribuut())) return List.of();
        Objecttype subject = doel.objecttype();
        Class<?> factClass = factClass(subject);
        Class<?> incClass = inconsistentieClass();
        if (factClass == null || incClass == null) return List.of();
        Expr wortel = r.waarde();
        while (wortel instanceof Expr.Binair b) wortel = b.links();
        java.util.function.Function<Object, Object> anker = companionOperand(wortel, subject);
        if (anker == null) return List.of();
        String naam = r.naam();
        seq = 0;
        Variable sVar = DSL.declarationOf(factClass, "$subject");
        PatternDSL.PatternDef pattern = PatternDSL.pattern(sVar);
        List<RuleItemBuilder<?>> gateExtra = List.of();
        if (r.voorwaarde() != null) {                       // §9.9: the fout only applies under the write's voorwaarde
            Gate gate = voorwaarde(r.voorwaarde(), pattern, subject, sVar);
            if (gate == null) return List.of();             // unlowerable host voorwaarde -> no companion (DRL: voorwaardeMarker -> return)
            pattern = gate.pattern();
            gateExtra = gate.extra();
        }
        pattern = geldigGuard(pattern, r.geldigheid());     // §4.2 the companion only flags inside the host's window
        pattern = pattern.expr(nextId("anker"), (Predicate1) s -> anker.apply(s) == null);
        return List.of(companionRule(naam + " (leeg startpunt)", naam, incClass, sVar, pattern, gateExtra));
    }

    /** §8.1.9 emit a RuleFired-inserting companion for a main-regel() rule whose firing is referenced by a live
     *  "regelversie X is gevuurd" predicate (opt-in — an unreferenced rule marks nothing). The companion shares
     *  the host rule's firing conditions (subject pattern + voorwaarde gate + §9.6 initialisatie leeg-guard +
     *  §4.2 window) and inserts one {@code RuleFired(naam)} per matching subject — the model analog of
     *  DrlTranspiler.markeerGevuurd's UNCONDITIONAL top-of-consequence insert. It is insert-only (NO no-loop, no
     *  idempotency guard): a RuleFired insert does not touch the subject pattern, so it never re-activates
     *  itself, and per §8.1.9 design the reader's exists collapses the duplicate markers a rule firing for N
     *  subjects produces (a not-exists guard here would change the HOST's firing semantics — deliberately
     *  omitted; bounded bloat: #referenced-rules × #subjects). Scoped to the same main-regel() instrumented
     *  kinds {@link RegelSpraakModel#gevuurdProducent} covers — an unlowerable host voorwaarde yields no
     *  companion (symmetric with the reader staying a marker). */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private List<Rule> gevuurdCompanions(ResolvedRegel r) {
        if (!model.gevuurdProducent(r.naam()) || !model.gevuurdReferenties().contains(r.naam())) return List.of();
        if (!(r.doel() instanceof Expr.AttribuutRef doel) || !ongenavigeerd(doel) || doel.objecttype() == null)
            return List.of();
        Objecttype subject = doel.objecttype();
        Class<?> factClass = factClass(subject);
        if (factClass == null) return List.of();
        final String naam = r.naam();
        seq = 0;
        Variable sVar = DSL.declarationOf(factClass, "$subject");
        PatternDSL.PatternDef pattern = PatternDSL.pattern(sVar);
        List<RuleItemBuilder<?>> extra = new ArrayList<>();
        if (r.voorwaarde() != null) {
            Gate gate = voorwaarde(r.voorwaarde(), pattern, subject, sVar);
            if (gate == null) return List.of();             // unlowerable host voorwaarde -> no companion (reader stays a marker)
            pattern = gate.pattern();
            extra.addAll(gate.extra());
        }
        // §9.6: the initialisatie leeg-guard is part of the host's firing condition — mirror it so the marker
        // records "gevuurd" exactly when the initialisatie would actually write (a pre-filled doel does not fire).
        if (r.soort() == ResolvedRegel.Soort.INITIALISATIE) {
            java.util.function.Function<Object, Object> doelGetter = Accessors.getter(factClass, doel.attribuut());
            pattern = pattern.expr(nextId("init"), (Predicate1) (s -> doelGetter.apply(s) == null));
        }
        pattern = geldigGuard(pattern, r.geldigheid());     // §4.2 the marker only records inside the host's window
        Block2<org.drools.model.Drools, Object> body = (drools, s) -> drools.insert(new RuleFired(naam));
        List<RuleItemBuilder<?>> items = new ArrayList<>();
        items.add(pattern);
        items.addAll(extra);
        items.add(DSL.on(sVar).execute(body));
        return List.of(PatternDSL.rule(kop(naam + " (gevuurd)")).build(items.toArray(new RuleItemBuilder[0])));
    }

    /** Collect the WORTEL nodes through the arithmetic-wrapper layers of a value (for the §6.6 fout), each with
     *  the outermost ch. 4.10 node above it. Mirrors DrlTranspiler.verzamelWortels over the shared v0 forms; the
     *  ch. 4.10 descent is additional — the DRL back-end lowers no Let: it writes a TODO-expressie marker for the
     *  node INSIDE the host rule's value and still emits the companions of the sites OUTSIDE it (measured;
     *  the companion-name consequence is at {@link #delingCompanions}). Without the descent a Let-wrapped
     *  wortel loses its §9.5 companion while ref still raises the fault: silently consistent. The descent takes the FULL
     *  {@link #letKinderen} — every child that MAY run — because a companion is an ANSWER and which child DID
     *  run is decided at fire time by the path probe over that node ({@link Sonde}), never by this walk. */
    private void verzamelWortels(Expr e, Expr drager, List<Plek<Expr.Functie>> uit) {
        // the OUTERMOST ch. 4.10 node on the path is fixed once entered; a nested one stays under it
        Expr onder = drager != null ? drager : (isLetKnoop(e) ? e : null);
        for (Expr k : letKinderen(e)) verzamelWortels(k, onder, uit);
        if (e instanceof Expr.Functie f) {
            if (f.kind() == Expr.FunctieKind.WORTEL) uit.add(new Plek<>(f, drager));
            for (Expr a : f.argumenten()) verzamelWortels(a, drager, uit);
        } else if (e instanceof Expr.Binair b) {
            verzamelWortels(b.links(), drager, uit); verzamelWortels(b.rechts(), drager, uit);
        } else if (e instanceof Expr.Afgerond a) {
            verzamelWortels(a.basis(), drager, uit);
        } else if (e instanceof Expr.Begrensd g) {
            verzamelWortels(g.basis(), drager, uit);
            verzamelWortels(g.minimum(), drager, uit);   // mirror DRL verzamelWortels — a wortel inside a min/max bound
            verzamelWortels(g.maximum(), drager, uit);
        }
    }

    /** One §6.5/§6.6 companion rule over the host's own gate (voorwaarde + §4.2 window). A site OUTSIDE every
     *  node gets {@code statisch} — the static leeg/negatief predicate on its re-readable operands, the
     *  unchanged v0 companion (DRL parity); a site UNDER one gets the path probe instead: the companion
     *  fires when re-evaluating that node on the subject reports a §6.5/§6.6 fault ({@link Sonde}), which is
     *  ref's own condition under ch. 10.2/10.6. null when the host gate does not lower (DRL: voorwaardeMarker
     *  -> no companion). */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private Rule padCompanion(ResolvedRegel r, Objecttype subject, Class<?> factClass, Class<?> incClass,
                              String compNaam, Plek<? extends Expr> plek,
                              java.util.function.UnaryOperator<PatternDSL.PatternDef> statisch) {
        seq = 0;
        Variable sVar = DSL.declarationOf(factClass, "$subject");
        PatternDSL.PatternDef pattern = PatternDSL.pattern(sVar);
        List<RuleItemBuilder<?>> gateExtra = List.of();
        if (r.voorwaarde() != null) {                       // the site only fires when the host gate holds
            Gate gate = voorwaarde(r.voorwaarde(), pattern, subject, sVar);
            if (gate == null) return null;                  // unlowerable host voorwaarde -> no companion (DRL: voorwaardeMarker -> return)
            pattern = gate.pattern();
            gateExtra = gate.extra();                       // a §8.1.7 far-side membership exists/not -> thread it into the companion (mirror DRL lc.extra())
        }
        pattern = geldigGuard(pattern, r.geldigheid());   // §4.2 the companion only flags inside the host's window
        String naam = r.naam();
        if (plek.drager() == null)
            return companionRule(compNaam, naam, incClass, sVar, statisch.apply(pattern), gateExtra);
        // The probe decides in the CONSEQUENCE, which binds the node's Parameter globals the way the host
        // binds its own (a pattern expr hosts one global at most). The opaque always-true expr keeps the pattern
        // class-reactive (the freshness idiom), so the probe re-runs whenever the subject is updated and the
        // insert-only companion still lands once a producer fills what the path reads; the §9.5 idempotency
        // guard in companionRule stops a second insert.
        pattern = pattern.expr(nextId("fresh"), (Predicate1) (s -> true));
        return companionRule(compNaam, naam, incClass, sVar, pattern, gateExtra,
                sondeConsequentie(compNaam, naam, subject, sVar, plek));
    }

    /** The consequence of a path-probed companion: re-evaluate the outermost ch. 4.10 node on the subject with a
     *  {@link Sonde} planted in the environment, and insert the host's Inconsistentie iff the probe reports a
     *  §6.5/§6.6 fault under that node (not per site — see {@link Sonde}). The node lowers through the same
     *  {@link #scalarWaarde} the host lowered it with (under the same
     *  §13.4.1 keten flag), so its globals are a subset of the host's; a node that does not lower here, or a
     *  global the host could not have bound, is an invariant violation and fails fast — never a silently missing
     *  companion. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private RuleItemBuilder<?> sondeConsequentie(String compNaam, String hostNaam, Objecttype subject, Variable sVar,
                                                 Plek<? extends Expr> plek) {
        WaardeFn wf;
        boolean prev = ketenWaardePad;
        try { ketenWaardePad = true; wf = scalarWaarde(subject, plek.drager()); }
        finally { ketenWaardePad = prev; }
        if (wf == null)
            throw new IllegalStateException(compNaam + ": de drager van de §6.5/§6.6-plek lowert niet, terwijl de gastregel"
                    + " lowerde — bron: " + plek.drager().bron());
        List<String> gnames = new ArrayList<>(wf.globals());
        List<Variable> vars = new ArrayList<>();
        vars.add(sVar);
        for (String gn : gnames) {
            Global<?> g = waardeGlobal(gn);   // numeric Parameter or the §5.3 rekendatum pseudo-slot
            if (g == null)
                throw new IllegalStateException(compNaam + ": de drager leest de niet-geregistreerde global " + gn
                        + " die de gastregel wel bond");
            vars.add((Variable) g);
        }
        BiFunction<Object, Map<String, BigDecimal>, Object> fn = wf.fn();
        String fqcn = FACTS + ".Inconsistentie";
        RuleItemBuilder<?> consequence = consequenceOf(vars.toArray(new Variable[0]), (drools, args) -> {
            Object s = args[0];
            Map<String, BigDecimal> gm = new LinkedHashMap<>();
            vulGlobalMap(gm, gnames, args, 1);
            Sonde sonde = new Sonde();
            ((Map<String, Object>) (Map) gm).put(Sonde.SLEUTEL, sonde);
            fn.apply(s, gm);   // the value is discarded: only whether the path faulted matters
            if (sonde.heeftFoutGemeld()) drools.insert(RegelSpraakRuntime.newInconsistentie(fqcn, hostNaam, s));
        });
        if (consequence == null)
            throw new IllegalStateException(compNaam + ": te veel gebonden globals voor de sonde (" + gnames.size()
                    + "), meer dan de gastregel kon binden");
        return consequence;
    }

    /** Build a §9.5 fout-companion rule: an insert-only rule (NO no-loop) on the given subject pattern that inserts
     *  an Inconsistentie carrying the HOST rule name, idempotency-guarded against re-flagging the same subject. The
     *  {@code gateExtra} items are the host rule's §8.1.7 FAR-side membership exists/not joins (a non-empty
     *  {@link Gate#extra()}): a fout companion under a far-side voorwaarde must carry that gate too, else it loses the
     *  membership and over-fires for subjects the host rule never acts on (mirror of DrlTranspiler's extraClausules
     *  threading). The companion is insert-only, so an extra exists/not clause is safe (it cancels no sibling). */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private Rule companionRule(String compNaam, String hostNaam, Class<?> incClass, Variable sVar,
                              PatternDSL.PatternDef pattern, List<RuleItemBuilder<?>> gateExtra) {
        String fqcn = FACTS + ".Inconsistentie";
        Block2<org.drools.model.Drools, Object> body = (drools, s) ->
                drools.insert(RegelSpraakRuntime.newInconsistentie(fqcn, hostNaam, s));
        return companionRule(compNaam, hostNaam, incClass, sVar, pattern, gateExtra, DSL.on(sVar).execute(body));
    }

    /** {@link #companionRule(String, String, Class, Variable, PatternDSL.PatternDef, List)} with the consequence
     *  supplied — the path-probe companion decides its insert there ({@link #sondeConsequentie}); the guard and
     *  the far-side gate are the same. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private Rule companionRule(String compNaam, String hostNaam, Class<?> incClass, Variable sVar,
                              PatternDSL.PatternDef pattern, List<RuleItemBuilder<?>> gateExtra,
                              RuleItemBuilder<?> consequence) {
        Variable incVar = DSL.declarationOf(incClass, "$inc_" + (seq++));
        PatternDSL.PatternDef guard = PatternDSL.pattern(incVar)
                .expr(nextId("ig"), (Predicate1) i -> hostNaam.equals(RegelSpraakRuntime.get(i, "regel")))
                .expr(nextId("ig"), (Variable) sVar, (Predicate2) (i, s) -> RegelSpraakRuntime.get(i, "subject") == s);
        List<RuleItemBuilder<?>> items = new ArrayList<>();
        items.add(pattern);
        items.addAll(gateExtra);                          // the host rule's far-side membership gate, if any
        items.add(DSL.not(guard));
        items.add(consequence);
        return PatternDSL.rule(kop(compNaam)).build(items.toArray(new RuleItemBuilder[0]));
    }
    /** Collect the GEDEELD_DOOR / GEDEELD_DOOR_ABS nodes through the arithmetic-wrapper layers of a value (for the
     *  §6.5 fout), each with the outermost ch. 4.10 node above it — byte-identical to DrlTranspiler.verzamelDelingen
     *  over the shared v0 forms; the ch. 4.10 descent is additional — the DRL back-end lowers no Let: it writes a
     *  TODO-expressie marker for the node INSIDE the host rule's value and still emits the companions of the
     *  sites OUTSIDE it (measured; the companion-name consequence is at {@link #delingCompanions}). Without the
     *  descent a Let-wrapped division loses its §6.5/§9.5 companion while ref still raises the fault: silently
     *  consistent. The descent
     *  takes the FULL {@link #letKinderen} — every child that MAY run — because a companion is an ANSWER and which
     *  child DID run is decided at fire time by the path probe over that node ({@link Sonde}), never by this walk. */
    private void verzamelDelingen(Expr e, Expr drager, List<Plek<Expr.Binair>> uit) {
        // the OUTERMOST ch. 4.10 node on the path is fixed once entered; a nested one stays under it
        Expr onder = drager != null ? drager : (isLetKnoop(e) ? e : null);
        for (Expr k : letKinderen(e)) verzamelDelingen(k, onder, uit);
        if (e instanceof Expr.Binair b) {
            // §6.5 "gedeeld door" AND "gedeeld door (ABS)" both lower to a leeg-divisor fout (mirror DRL).
            if (b.op() == Expr.BinOp.GEDEELD_DOOR || b.op() == Expr.BinOp.GEDEELD_DOOR_ABS) uit.add(new Plek<>(b, drager));
            verzamelDelingen(b.links(), drager, uit); verzamelDelingen(b.rechts(), drager, uit);
        } else if (e instanceof Expr.Afgerond a) {
            verzamelDelingen(a.basis(), drager, uit);
        } else if (e instanceof Expr.Begrensd g) {
            verzamelDelingen(g.basis(), drager, uit);
            verzamelDelingen(g.minimum(), drager, uit);   // mirror DRL — a division inside a min/max bound
            verzamelDelingen(g.maximum(), drager, uit);
        }
    }

    private Class<?> inconsistentieClass() {
        try { return Class.forName(FACTS + ".Inconsistentie", true, beanLoader); }
        catch (ClassNotFoundException e) { return null; }
    }

    /** A built-in Drools accumulate function ({@code org.drools.core.base.accumulators.*}) by simple name, resolved
     *  at MODEL-BUILD time via {@link Class#forName} rather than a compile-time {@code .class} literal. This keeps
     *  {@code src/main} compiling against {@code drools-canonical-model} ALONE (the Model-building DSL) — the
     *  accumulator impls live in {@code drools-core}, which only needs to be on the RUNTIME classpath of whoever
     *  fires the Model (the consumer engine), not the compile classpath of the transpiler. {@code DSL.accFunction}
     *  accepts an unbounded {@code Class<?>}, so the runtime-resolved class flows through unchanged.
     *
     *  <p>VERSION-PINNED to Drools 8.44: this package is a drools-core-INTERNAL FQN (not {@code kie-api}). On later
     *  Drools the same classes relocate to {@code org.drools.base.accumulators} (a separate {@code drools-base}
     *  module). Centralized here behind this one constant + a fail-fast {@link ClassNotFoundException} guard, so an
     *  engine upgrade surfaces as a clear model-build error naming the missing class — never a silent miscompile. */
    private static final String ACC_PKG = "org.drools.core.base.accumulators.";
    static Class<?> accFn(String simpleName) {
        try { return Class.forName(ACC_PKG + simpleName); }
        catch (ClassNotFoundException e) {
            throw new IllegalStateException("Drools accumulate-functie " + simpleName + " ontbreekt op het classpath"
                    + " (drools-core is een RUNTIME-afhankelijkheid van de uitvoerende engine)", e);
        }
    }

    /** Why a voorwaarde did not lower. "buiten Phase-B scope" is the honest default, but it was not the only
     *  cause: an UNREADABLE datum-tijd lexeme (IR-SPEC ch. 4.1) also lowers to nothing, and blaming a construct
     *  scope for it sent one earlier session hunting a millisecond bug that did not exist. The DRL branch labels
     *  the same cause in the same words, so a producer reads one diagnosis from either branch. */
    private static String voorwaardeSkipReden(Conditie c) {
        if (Conditie.bevatOperand(c, Literalen::onleesbaarDatumTijdLexeem)) return "voorwaarde met onleesbaar datum-tijd-lexeem";
        if (Conditie.bevatOperand(c, Literalen::onleesbaarDatumLexeem)) return "voorwaarde met onleesbaar datum-lexeem";
        return "voorwaarde buiten Phase-B scope";
    }

    private Rule skip(String reason) { overgeslagen.add(reason); return null; }
    Class<?> factClass(Objecttype ot) {
        try { return Class.forName(FACTS + "." + RegelSpraakRuntime.pascal(ot.naam), true, beanLoader); }
        catch (ClassNotFoundException e) { return null; }
    }
    /** The parameter-GLOBAL name — kern.Namen.ident(.., false) (the shared naming contract), so the model's global name
     *  equals the DRL's and the differential session sets ONE global by that single shared name. */
    static String globalNaam(String naam) { return Namen.ident(naam, false); }
    /** The JAVABEAN property name Drools watches for property reactivity (the lowercase-first form of {@link #ident}):
     *  "de aandeel" -> "aandeel", "het saldo" -> "saldo". Matches the get/set the reflective bridge uses. */
    static String prop(String naam) {
        String p = RegelSpraakRuntime.pascal(naam);
        return p.isEmpty() ? p : Character.toLowerCase(p.charAt(0)) + p.substring(1);
    }

    /** §9.2 the stratification salience for a rule. A §13.4.5 kenmerktoekenning is MONOTONIC (spec §3.4 l.795: a
     *  kenmerk is default-onwaar and a toekenning only ever makes it WAAR — never un-sets it), so a rule that reads
     *  "niet &lt;kenmerk&gt;" must be evaluated at the FIXPOINT, after every producer of that kenmerk (and their
     *  own computation chains) has run. Forward-chaining gives no such guarantee: a negation-consumer eligible in
     *  the transient window fires and its once-true write persists. We push every rule whose voorwaarde reads a
     *  NEGATED kenmerk to the BOTTOM of the agenda (salience -1) so it fires only after the default-0 positive
     *  computation has quiesced (the producer set the kenmerk). This is stronger than lifting the direct producer:
     *  the producer may itself depend on a berekening (drempelwaarde) that must fire first — sinking the consumer
     *  keeps the whole positive chain ahead of it. Every non-negation rule keeps salience 0 (unchanged agenda).
     *  DERIVED (no longer the one-level heuristic): the shared {@link Stratificatie} computes the stratum over
     *  the declared kenmerk graph, so a chain of negation-consumers sinks level by level instead of collapsing
     *  into one -1 band where source order decides (the StratificatieKetenTest red case). Both back-ends read
     *  the SAME derivation — the single-runtime principle for the agenda. */
    private int stratificatieSalience(ResolvedRegel r) {
        return strat.salience(r);
    }

    // ================= §6.1 / ch. 7 prioritized defaults (reduce-rule port of ref's run_defaults_group) =========

    /** Group the ch. 7 defaults bundles by (kind, subject, target) — the ref grouping key (eval.ml:2212): a
     *  name-sharing attribute and characteristic (WF-002) stay in separate groups, so the discriminant leads.
     *  First-seen order is preserved (a LinkedHashMap), which is stable and deterministic. Each group becomes
     *  ONE reduce-rule. */
    /** One effective stack per (CONCRETE type, target) — the composition of RUNTIME-SEMANTICS ch. 7.10,
     *  ported from {@code Ir_v0.Desugar.effective_defaults} so the three executors cannot drift on the
     *  block order. A subtype's own declarations fold over its supertype's EFFECTIVE stack, highest
     *  first: own exceptions, inherited exceptions, the base (the own one REPLACES the inherited one),
     *  and below it the initialisation default, never replaced.
     *
     *  Why per concrete type and not per declaration: with native {@code extends} a pattern on the
     *  supertype's class matches subtype facts, so a supertype group and a subtype group would BOTH
     *  reduce over one Wrakrecht and write in turn — no fixpoint. The composition spends the
     *  extensional quantification of the defaults subject once, here, and {@link #defaultsGroep} then
     *  binds its pattern to exactly one class. In a model without a supertype edge this is the
     *  declared grouping, unchanged. */
    private record EffectieveStapel(Objecttype eigenaar, List<PrioritizedDefaults> leden) {}

    private List<EffectieveStapel> defaultsGroepen() {
        List<EffectieveStapel> uit = new ArrayList<>();
        for (Objecttype ot : model.objecttypen.values())
            for (var e : effectieveStapels(ot).entrySet())
                uit.add(new EffectieveStapel(ot, e.getValue()));
        return uit;
    }

    /** The effective stacks of {@code type}, keyed by kind-discriminated target. */
    private LinkedHashMap<String, List<PrioritizedDefaults>> effectieveStapels(Objecttype type) {
        return effectieveStapels(type, ketenGroepSleutels(type));
    }

    /** {@code absorb} is the key set on which an ordinary rule counts as a base — a property of the
     *  stack's OWNER, threaded down so the answer does not change with the recursion depth. */
    private LinkedHashMap<String, List<PrioritizedDefaults>> effectieveStapels(
            Objecttype type, Set<String> absorb) {
        LinkedHashMap<String, List<PrioritizedDefaults>> eigen = new LinkedHashMap<>();
        for (PrioritizedDefaults l : prioritizedDefaults)
            if (defaultsNorm(l.onderwerp()).equals(defaultsNorm(type.naam)))
                eigen.computeIfAbsent(defaultsDoelSleutel(l), k -> new ArrayList<>()).add(l);
        // SUBTYPING §4.1 rules 1 and 4: a level that declares no group on an ABSORBED key contributes
        // its ORDINARY RULE as that level's base instead. The Java twin of Desugar.rule_member — same
        // shape, same gate — so the three executors compose one stack.
        for (String k : absorb)
            if (!eigen.containsKey(k)) {
                PrioritizedDefaults m = regelLid(type, k);
                if (m != null) eigen.put(k, List.of(m));
            }
        LinkedHashMap<String, List<PrioritizedDefaults>> geerfd =
                type.supertype == null ? new LinkedHashMap<>() : effectieveStapels(type.supertype, absorb);
        LinkedHashMap<String, List<PrioritizedDefaults>> uit = new LinkedHashMap<>(eigen);
        geerfd.forEach((k, ouderLeden) -> {
            List<PrioritizedDefaults> eigenLeden = eigen.get(k);
            if (eigenLeden == null) { uit.put(k, ouderLeden); return; }
            // ONE own group over an inherited declared-unordered SET is DEFINED — SUBTYPING §4.1
            // rule 3: the inherited block stays internally unordered and moves as one, above the
            // base and below the own exceptions. It is not lowered here, and the reason is a
            // property of the wire rather than a gap in the design: that reading is a THREE-LAYER
            // priority (own exceptions > the unordered inherited block > the own base) while a
            // defaults group's members reconcile as EQUALS with no order between them. The own
            // base's justification is unconditional, so wherever it sat it would decide whenever an
            // inherited member decides, and "an inherited exception outranks the subtype's base"
            // (§4.4) could not be told from "the base decides when nothing else does".
            if (eigenLeden.size() == 1 && ouderLeden.size() > 1)
                throw new DefaultsOnvertaalbaar("doel " + k + " op '" + type.naam + "' composes one own"
                        + " group over an inherited declared-unordered set of " + ouderLeden.size()
                        + ": SUBTYPING §4.1 rule 3 defines it (the inherited block moves whole, above the"
                        + " base and below the own exceptions) but it is not lowered in this release — the"
                        + " reading is a three-layer priority and a group's members reconcile as equals"
                        + " (RUNTIME-SEMANTICS ch. 7.10)");
            // A declared-unordered set on the OWN level, over anything inherited: here the design
            // itself is silent, so this one is genuinely undefined rather than merely un-lowered.
            if (eigenLeden.size() != 1 || ouderLeden.size() != 1)
                throw new DefaultsOnvertaalbaar("doel " + k + " op '" + type.naam + "' draagt "
                        + eigenLeden.size() + " eigen en " + ouderLeden.size() + " geërfde groep(en): a fold"
                        + " between a declared-unordered set of its own and an inherited stack has no"
                        + " defined reading (RUNTIME-SEMANTICS ch. 7.10)");
            uit.put(k, List.of(voegSamen(eigenLeden.get(0), ouderLeden.get(0))));
        });
        return uit;
    }

    /** The vacuously true justification an unconditional rule gets when it becomes a base — the same
     *  empty-ALLE the ch. 7.7 desugaring uses, so the two readings are one shape. */
    private static final Conditie ALTIJD =
            new Conditie.Samengesteld(Conditie.Kwantor.ALLE, 0, List.of(), null);

    /** The Java twin of {@code Ir_v0.Desugar.rule_member}: {@code level}'s own contribution to
     *  {@code key} when it declares no defaults group — its ordinary rules read as the ch. 7.7 shape,
     *  the computing rule as the base and the initialisation below it. Null when the level writes the
     *  target with no ordinary rule either; two computing rules on one target are agenda order, which
     *  prioritized defaults exist to replace, so those refuse. */
    private PrioritizedDefaults regelLid(Objecttype level, String key) {
        // EVERY ordinary writer on the target — the filter is on the target and nothing else. An
        // earlier revision skipped the forms it could not fold (a validity window, an absent value),
        // which was a silent-answer bug rather than a narrowing: geabsorbeerdOp takes the rule out of
        // the rule circuit per (type, attribute), so a rule the fold skipped was silenced without ever
        // becoming a base and its writer vanished. Collect everything; refuse by name below.
        List<ResolvedRegel> comps = new ArrayList<>(), inits = new ArrayList<>(), alle = new ArrayList<>();
        for (ResolvedRegel r : model.resolvedRegels) {
            if (!(r.doel() instanceof Expr.AttribuutRef d) || d.attribuut() == null) continue;
            if (d.objecttype() != level || !("A:" + defaultsNorm(d.attribuut())).equals(key)) continue;
            switch (r.soort()) {
                case GELIJKSTELLING, BEREKENING -> { comps.add(r); alle.add(r); }
                case INITIALISATIE -> { inits.add(r); alle.add(r); }
                default -> { }
            }
        }
        if (alle.isEmpty()) return null;
        for (ResolvedRegel r : alle)
            // NOT lowered, deliberately: a windowed base would put version selection inside the
            // stack, and ch. 7.4 keeps that apart from defeasibility on purpose.
            if (r.geldigheid() != null && !r.geldigheid().altijd())
                throw new DefaultsOnvertaalbaar("target " + key + " op '" + level.naam + "' is written"
                        + " by a rule with a VALIDITY WINDOW beside an inherited defaults stack: a"
                        + " windowed base would put version selection inside the stack, which ch. 7.4"
                        + " keeps apart from defeasibility, and the block order has no reading for it"
                        + " (RUNTIME-SEMANTICS ch. 7.10)");
        for (ResolvedRegel r : alle)
            if (r.waarde() == null)
                throw new DefaultsOnvertaalbaar("target " + key + " op '" + level.naam + "' is written"
                        + " by a rule without a value beside an inherited defaults stack: a base with no"
                        + " consequence decides nothing and cannot be folded (RUNTIME-SEMANTICS ch. 7.10)");
        if (comps.size() > 1 || inits.size() > 1)
            throw new DefaultsOnvertaalbaar("doel " + key + " op '" + level.naam + "' is written by "
                    + comps.size() + " computing and " + inits.size() + " initialisation rule(s) beside an"
                    + " inherited defaults stack: the v0 meaning of two writers on one target is agenda"
                    + " order, which prioritized defaults exist to replace (RUNTIME-SEMANTICS ch. 7.10)");
        List<Default> ds = new ArrayList<>();
        String attr = null;
        for (ResolvedRegel r : comps) {
            attr = ((Expr.AttribuutRef) r.doel()).attribuut();
            ds.add(new Default(null, r.voorwaarde() != null ? r.voorwaarde() : ALTIJD,
                    r.waarde(), false, null, r.naam()));
        }
        for (ResolvedRegel r : inits) {
            attr = ((Expr.AttribuutRef) r.doel()).attribuut();
            ds.add(new Default(null, null, r.waarde(), true, null, r.naam()));
        }
        return new PrioritizedDefaults("regel als basis: " + attr + " van " + level.naam, level.naam,
                null, attr, false, List.copyOf(ds),
                "SUBTYPING §4.1 rules 1/4: an ordinary rule is the base of its level", null);
    }

    /** The Java twin of {@code Ir_v0.Desugar.absorbed_on}: the CONCRETE types on which an ordinary
     *  rule writing ({@code otype}, {@code attr}) has been folded into an effective defaults stack as
     *  its base, and must therefore not fire as a rule as well. Empty unless the type's own chain
     *  declares a defaults group on that target. */
    private List<Objecttype> geabsorbeerdOp(Objecttype otype, String attr) {
        String key = "A:" + defaultsNorm(attr);
        // the rule's OWN level declaring a group on the target is not inheritance but WF-050's
        // single-writer lift, refused at the document. The synthesis skips such a level, so the
        // suppression must skip it too — otherwise the rule is silenced without ever becoming a base.
        for (PrioritizedDefaults l : prioritizedDefaults)
            if (model.objecttypeOpNaam(l.onderwerp()) == otype && defaultsDoelSleutel(l).equals(key))
                return List.of();
        List<Objecttype> uit = new ArrayList<>();
        for (Objecttype t : model.objecttypen.values())
            if (t.isEen(otype) && ketenGroepSleutels(t).contains(key)) uit.add(t);
        return uit;
    }

    /** The target keys on which {@code t}'s own chain declares a defaults GROUP. */
    private Set<String> ketenGroepSleutels(Objecttype t) {
        Set<String> uit = new LinkedHashSet<>();
        for (PrioritizedDefaults l : prioritizedDefaults) {
            Objecttype onderwerp = model.objecttypeOpNaam(l.onderwerp());
            if (onderwerp != null && t.isEen(onderwerp)) uit.add(defaultsDoelSleutel(l));
        }
        return uit;
    }

    /** The block order of ch. 7.10, applied to one own member over one inherited member. */
    private static PrioritizedDefaults voegSamen(PrioritizedDefaults eigen, PrioritizedDefaults geerfd) {
        List<Default> uit = new ArrayList<>();
        uit.addAll(uitzonderingen(eigen));
        uit.addAll(uitzonderingen(geerfd));
        Default basis = basisVan(eigen) != null ? basisVan(eigen) : basisVan(geerfd);
        if (basis != null) uit.add(basis);
        uit.addAll(initialisaties(geerfd));
        uit.addAll(initialisaties(eigen));
        return new PrioritizedDefaults(eigen.naam(), eigen.onderwerp(), eigen.kenmerk(), eigen.attribuut(),
                eigen.ongeordend(), List.copyOf(uit), eigen.bron(), eigen.geldigheid());
    }

    /** The LOWEST ordinary default, and only when it decides something itself: a group-default delegates
     *  to its exception groups and is not a base (ch. 7.10 rule 2), and a declared-unordered member has
     *  no "lowest" at all, so its whole block travels as one (rule 3). */
    private static Default basisVan(PrioritizedDefaults l) {
        if (l.ongeordend()) return null;
        Default laatste = null;
        for (Default d : l.defaults()) if (!d.initialisatie()) laatste = d;
        return laatste != null && laatste.heeftEigenInhoud() ? laatste : null;
    }
    private static List<Default> uitzonderingen(PrioritizedDefaults l) {
        Default basis = basisVan(l);
        List<Default> uit = new ArrayList<>();
        for (Default d : l.defaults()) if (!d.initialisatie() && d != basis) uit.add(d);
        return uit;
    }
    private static List<Default> initialisaties(PrioritizedDefaults l) {
        List<Default> uit = new ArrayList<>();
        for (Default d : l.defaults()) if (d.initialisatie()) uit.add(d);
        return uit;
    }

    private static String defaultsDoelSleutel(PrioritizedDefaults l) {
        return (l.isKenmerk() ? "K:" : "A:") + defaultsNorm(l.doel());
    }
    /** The manifest/rule identity of a defaults group — the kind-discriminated target form on its OWNER
     *  type, so two types sharing one inherited target still name two rules. */
    private static String defaultsConstructNaam(EffectieveStapel stapel) {
        PrioritizedDefaults l0 = stapel.leden().get(0);
        return "defaults " + (l0.isKenmerk() ? "K:" : "A:") + stapel.eigenaar().naam + "." + l0.doel();
    }
    /** Delegates to the SHARED name contract: a second, byte-identical copy of ref's {@code Checks.norm}
     *  here is how the doel/defaults keys and the uniqueness keys would come to disagree the moment one of
     *  the two is corrected. */
    private static String defaultsNorm(String s) {
        return Namen.norm(s);
    }

    /** Lower one defaults group to its single reduce-rule (a port of ref's {@code run_defaults_group},
     *  eval.ml:1630-1771): LHS the class-reactive subject pattern (freshness idiom), RHS the top-down walk over the
     *  defaults with leeg fall-through, recursion into {@code excepties}, member reconciliation, and one idempotent
     *  write or one §9.5 {@code Inconsistentie}. Loud, reason-carrying {@code skipList} fences (the exec-back-end's
     *  skip discipline, surfaced by {@link #meet} as a SKIP): a temporal defaults target (ref-parity), a
     *  channel-depositing consequence (division), a justification/consequence not reducible to the subject-local
     *  lambda machinery. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private List<Rule> defaultsGroep(EffectieveStapel stapel) {
        List<PrioritizedDefaults> members = stapel.leden();
        // The conflict path inserts one Inconsistentie and never un-writes a target, and the idempotency
        // guard blocks re-insertion; that is only sound because defaultsSalience holds the reduce-rule
        // strictly below every producer of every read of every default — a premature fire cannot happen. Keep
        // that invariant in mind before weakening the salience derivation.
        PrioritizedDefaults l0 = members.get(0);
        // the stack's OWNER, not the declaring subject of its members: inheritance has already folded
        // the supertype's declarations in, so this is the type whose instances the stack runs on
        Objecttype onderwerp = stapel.eigenaar();
        Class<?> factClass = factClass(onderwerp);
        if (factClass == null) return skipList("defaults (geen fact-class " + onderwerp.naam + ")");

        boolean isChar = l0.isKenmerk();
        String doel = l0.doel();
        // the ch. 7.2 conflict key names the type that DECLARES the target, never the concrete type:
        // one target reports under one name however many subtypes inherit it
        Objecttype declarant = onderwerp.declarantVan(doel);
        String targetForm = (declarant != null ? declarant.naam : l0.onderwerp()) + "." + doel;

        // fence: a characteristic target that NAME-SHARES an attribute (WF-002) — the exec-model bean carries a
        // single field per name and the attribute wins (FactModelGenerator: attribute > kenmerk > referentie), so
        // the boolean kenmerk has no bean field to write and the attribute/kenmerk cannot be told apart on read-back.
        // A pre-existing v0 bean-model limitation, orthogonal to defaults lowering — a loud, reason-carrying fence.
        if (isChar && onderwerp.attribuut(doel) != null)
            return skipList("defaults (kenmerktarget " + targetForm + " deelt zijn naam met een attribuut — de exec-model "
                    + "bean draagt één veld per naam (attribuut wint), dus het boolean kenmerk kan niet worden geschreven "
                    + "of onderscheiden; v0 bean-model-limiet, geen defaults-divergentie)");
        // fence: a temporal defaults target — temporal writes are not implemented (ref faults too; both fence).
        if (!isChar && Namen.bevatNaam(onderwerp.granulariteitLeden(), doel))
            return skipList("defaults (temporeel defaults-target " + targetForm + " — temporele writes niet gedragen, ref-pariteit)");
        // fence: a §3.6 gedimensioneerd (Map) target — out of the scalar-first scope.
        if (!isChar && !onderwerp.dimensiesVan(doel).isEmpty())
            return skipList("defaults (gedimensioneerd defaults-target " + targetForm + " — buiten scalar-first scope)");
        // fence: a dated validity window — the flippable set is all `altijd`; a per-member rekendatum gate is not built.
        for (PrioritizedDefaults l : members)
            if (l.geldigheid() != null && !l.geldigheid().altijd())
                return skipList("defaults (gedateerde geldigheid op " + targetForm + " — defaults-vensters niet gedragen)");

        // compile every member chain; a non-lowerable default throws DefaultsOnvertaalbaar (one loud fence for the group).
        List<List<GecompileerdeDefault>> ledenDefaults = new ArrayList<>();
        Set<String> globalNamen = new LinkedHashSet<>();
        try {
            for (PrioritizedDefaults l : members) {
                List<GecompileerdeDefault> ds = compileDefaults(l.defaults(), onderwerp, isChar, doel);
                verzamelDefaultsGlobals(ds, globalNamen);
                ledenDefaults.add(ds);
            }
        } catch (DefaultsOnvertaalbaar e) {
            return skipList("defaults (" + targetForm + "): " + e.getMessage());
        }

        java.util.function.Function<Object, Object> doelGetter = Accessors.getter(factClass, doel);
        BiConsumer<Object, Object> doelSetter = Accessors.setter(factClass, doel);

        seq = 0;
        Variable sVar = DSL.declarationOf(factClass, "$subject");
        // class-reactive: an opaque always-true expr keeps the pattern reactive to EVERY subject property change, so
        // the reduce-rule re-fires when a producer writes any read of any default (the freshness idiom). WF-051 keeps
        // the target off the read set, so the group's own write never cycles it (NO_LOOP guards self-retrigger).
        // A stack belongs to EXACTLY ONE type. With native `extends` a pattern on the supertype's class
        // matches subtype facts too, and the subtype has a stack of its own — so the subject is pinned
        // to the concrete class here, and only where the model actually has a hierarchy (a flat model
        // keeps the always-true freshness expr byte for byte).
        boolean heeftHierarchie = model.objecttypen.values().stream().anyMatch(t -> t.supertype != null);
        PatternDSL.PatternDef pattern = heeftHierarchie
                ? PatternDSL.pattern(sVar).expr(nextId("lfresh"), (Predicate1) (s -> s.getClass() == factClass))
                : PatternDSL.pattern(sVar).expr(nextId("lfresh"), (Predicate1) (s -> true));

        // resolve the globals the default consequences read (empty for the scalar-first flip set).
        List<String> gnames = new ArrayList<>(globalNamen);
        List<Variable> vars = new ArrayList<>();
        vars.add(sVar);
        for (String gn : gnames) {
            Global<?> g = waardeGlobal(gn);
            if (g == null) return skipList("defaults (" + targetForm + "): consequentie leest niet-geregistreerde global " + gn);
            vars.add((Variable) g);
        }

        boolean kanConflicteren = ledenDefaults.size() >= 2 || bevatMeervoudigeExcepties(ledenDefaults);
        final String fqcn = FACTS + ".Inconsistentie";
        final List<List<GecompileerdeDefault>> ledenDefaultsF = ledenDefaults;
        final boolean isCharF = isChar;
        final String targetFormF = targetForm;
        final List<String> gnamesF = gnames;
        final java.util.function.Function<Object, Object> getterF = doelGetter;
        final BiConsumer<Object, Object> setterF = doelSetter;

        RuleItemBuilder<?> consequence = consequenceOf(vars.toArray(new Variable[0]), (drools, args) -> {
            Object s = args[0];
            Map<String, BigDecimal> gm = new LinkedHashMap<>();
            vulGlobalMap(gm, gnamesF, args, 1);
            DefaultsRun run = new DefaultsRun(s, gm, getterF, isCharF, targetFormF);
            List<Uitkomst> memberResults = new ArrayList<>();
            for (List<GecompileerdeDefault> member : ledenDefaultsF) memberResults.add(run.decide(member, List.of()));
            Uitkomst res = run.reconcile(memberResults, List.of());
            switch (res.soort()) {
                case Uitkomst.CONFLICT ->
                    // over-determination (ch. 7.2/7.9): no write, one §9.5 fact — bare-keyed at the root,
                    // path-keyed for a nested exception-group conflict.
                    drools.insert(RegelSpraakRuntime.newInconsistentie(fqcn, res.conflictSleutel(), s));
                case Uitkomst.DECIDED -> {
                    Beslissing b = res.beslissing();
                    // NEVER the standing arm here: this is a ch. 7 DEFAULTS-group write, not a computing rule — a
                    // deciding default writes its consequence as it stands (ref's run_defaults_group likewise
                    // carries no ch. 7.8 arm; a defaults group also takes its target OUT of the standing domain).
                    if (isCharF) { if (b.charBesluit()) werkBij(drools, s, getterF, setterF, Boolean.TRUE, false); }
                    else werkBij(drools, s, getterF, setterF, b.attrWaarde(), false);
                }
                default -> { /* UNDECIDED: no write — the target keeps what the fact base holds */ }
            }
        });
        if (consequence == null) return skipList("defaults (" + targetForm + "): te veel gebonden globals");

        List<RuleItemBuilder<?>> items = new ArrayList<>();
        items.add(pattern);
        // §9.5 idempotency guard: a conflict-capable group inserts at most one Inconsistentie per subject — block a
        // re-insert of the same (regel-family, subject). The family is targetForm (+/<path> for a nested conflict);
        // only a conflict-capable group installs the guard, so a name-sharing non-conflicting group (a characteristic
        // sharing an attribute's target form) is never blocked by the other's conflict fact.
        if (kanConflicteren) {
            Class<?> incClass = inconsistentieClass();
            if (incClass != null) {
                Variable incVar = DSL.declarationOf(incClass, "$linc_" + (seq++));
                final String tf = targetForm;
                PatternDSL.PatternDef guard = PatternDSL.pattern(incVar)
                        .expr(nextId("lig"), (Predicate1) i -> {
                            Object rg = RegelSpraakRuntime.get(i, "regel");
                            return rg instanceof String s2 && (s2.equals(tf) || s2.startsWith(tf + "/"));
                        })
                        .expr(nextId("lig"), (Variable) sVar, (Predicate2) (i, s) -> RegelSpraakRuntime.get(i, "subject") == s);
                items.add(DSL.not(guard));
            }
        }
        items.add(consequence);

        // group-local salience (§5.1 of the design): strictly below every PRODUCER of every default's
        // justification/consequence read (recursed through excepties), so a monotone characteristic write cannot
        // decide before its reads have settled. Derived locally from the group's reads, NOT the full dep_forms graph.
        int salience = defaultsSalience(members, onderwerp);
        var bouwer = PatternDSL.rule(kop(defaultsConstructNaam(stapel))).attribute(Rule.Attribute.NO_LOOP, true);
        if (salience != 0) bouwer = bouwer.attribute(Rule.Attribute.SALIENCE, salience);
        return List.of(bouwer.build(items.toArray(new RuleItemBuilder[0])));
    }

    private List<GecompileerdeDefault> compileDefaults(List<Default> defaults, Objecttype onderwerp, boolean isChar, String doel) {
        List<GecompileerdeDefault> uit = new ArrayList<>();
        for (Default r : defaults) uit.add(compileDefault(r, onderwerp, isChar, doel));
        return uit;
    }

    /** Compile one default to its build-time closures. A form the scalar-first back-end does not carry raises a loud
     *  {@link DefaultsOnvertaalbaar} (caught in {@link #defaultsGroep} as the group's fence reason). */
    private GecompileerdeDefault compileDefault(Default r, Objecttype onderwerp, boolean isChar, String doel) {
        Predicate1<Object> just;
        boolean init = r.initialisatie();
        if (init) {
            if (isChar) throw new DefaultsOnvertaalbaar("initialisatie-default op kenmerk-defaults (WF-075/ref-fault)");
            just = null;   // justified via target-empty at fire time (ch. 7.3)
        } else if (r.voorwaarde() == null) {
            if (r.heeftEigenInhoud()) throw new DefaultsOnvertaalbaar("deciding default zonder justificatie");
            just = null;   // a contentless group-default never evaluates justified
        } else {
            just = subjectConditie(r.voorwaarde(), onderwerp);
            if (just == null)
                throw new DefaultsOnvertaalbaar("niet-reduceerbare justificatie (far-side/aggregatie/global): " + defaultsReden(r));
        }
        Boolean besluit = isChar ? r.besluit() : null;
        WaardeFn conseq = null;
        if (!isChar && r.consequentie() != null) {
            String foutplek = foutplekSoort(r.consequentie());
            if (foutplek != null)
                throw new DefaultsOnvertaalbaar("kanaal-deponerende consequentie (" + foutplek + ") op defaults-target: " + defaultsReden(r));
            WaardeFn wf;
            boolean prev = ketenWaardePad;
            try { ketenWaardePad = true; wf = scalarWaarde(onderwerp, r.consequentie()); }
            finally { ketenWaardePad = prev; }
            if (wf == null)
                throw new DefaultsOnvertaalbaar("niet-reduceerbare consequentie (far-side/aggregatie): " + defaultsReden(r));
            // §4 EXACT restate into the target unit. The TARGET side already reads the declaration
            // (onderwerp.attribuut); the SOURCE side is a consequence VALUE — an ordinary composite position, so it
            // takes the ch. 2.10 derivation like every other restatement site in this file.
            wf = wf.herschaald(effType(r.consequentie()), onderwerp.attribuut(doel), "een §4 consequentie naar het doel");
            conseq = wf;
        }
        List<List<GecompileerdeDefault>> exc = null;
        if (r.excepties() != null) {
            exc = new ArrayList<>();
            for (ExceptionGroup sl : r.excepties().leden()) exc.add(compileDefaults(sl.defaults(), onderwerp, isChar, doel));
        }
        return new GecompileerdeDefault(just, init, besluit, conseq, exc, r.heeftEigenInhoud());
    }
    private static String defaultsReden(Default r) { return r.bron() == null ? "<default>" : r.bron(); }

    /** A subject-local justification predicate ({@code subject -> boolean}): a compound recurses per its kwantor,
     *  every leaf goes through the shared {@link #leafPredicate} (kenmerk / own-or-navigated comparison / predicaat).
     *  null when ANY part is not reducible (a global-reading bullet, a deeper form leafPredicate declines) — the
     *  caller then fences the whole defaults group. Generalises {@link #leafCompound} to arbitrary nesting. */
    private Predicate1<Object> subjectConditie(Conditie c, Objecttype subject) {
        if (c == null) return null;
        if (c instanceof Conditie.Samengesteld sam) {
            List<Predicate1<Object>> kids = new ArrayList<>();
            for (Conditie deel : sam.onderdelen()) {
                Predicate1<Object> k = subjectConditie(deel, subject);
                if (k == null) return null;
                kids.add(k);
            }
            final Conditie.Kwantor kw = sam.kwantor();
            final int n = sam.drempel();
            final List<Predicate1<Object>> fk = kids;
            return s -> {   // ch. 5.10 short circuit, all five quantifiers (see vastgesteld)
                int t = 0;
                for (int i = 0; i < fk.size(); i++) {
                    Boolean vast = vastgesteld(kw, n, t, i, fk.size() - i);
                    if (vast != null) return vast;
                    if (fk.get(i).test(s)) t++;
                }
                return switch (kw) {
                    case ALLE, DE -> t == fk.size();
                    case GEEN -> t == 0; case TEN_MINSTE -> t >= n; case TEN_HOOGSTE -> t <= n; case PRECIES -> t == n;
                };
            };
        }
        return leafPredicate(c, subject);
    }

    /** The kind ({@code "deling"} / {@code "wortel"}) of the first §9.5 channel-depositing site in an expression —
     *  a §6.5 deling-door-leeg or a §6.6 wortel-van-negatief raises the Fout on the channel — or null when there
     *  is none: the speculative-deposit case the scalar-first port fences instead of building. A defaults
     *  consequence lowers no companion (static or probe), so a site here would fault in SILENCE; measured with
     *  ref on a taken default "wortel(saldo) plus 0" with saldo -4: ref toeslag 0.0000 + INCONSISTENT, this
     *  backend without the fence 0.0000 + consistent (fix round 1 of the companion-onder-drager card). */
    private String foutplekSoort(Expr e) {
        if (e == null) return null;
        // ch. 4.10: the fence must see a site hidden inside a Let value/body or an IfThenElse branch —
        // otherwise a Let-wrapped deling-door-leeg would deposit on the channel speculatively.
        for (Expr k : letKinderen(e)) { String r = foutplekSoort(k); if (r != null) return r; }
        if (e instanceof Expr.Binair b) {
            if (b.op() == Expr.BinOp.GEDEELD_DOOR || b.op() == Expr.BinOp.GEDEELD_DOOR_ABS) return "deling";
            String r = foutplekSoort(b.links());
            return r != null ? r : foutplekSoort(b.rechts());
        }
        if (e instanceof Expr.Afgerond af) return foutplekSoort(af.basis());
        if (e instanceof Expr.Begrensd bg) {
            String r = foutplekSoort(bg.basis());
            if (r == null) r = foutplekSoort(bg.minimum());
            return r != null ? r : foutplekSoort(bg.maximum());
        }
        if (e instanceof Expr.Functie f) {
            if (f.kind() == Expr.FunctieKind.WORTEL) return "wortel";
            for (Expr a : f.argumenten()) { String r = foutplekSoort(a); if (r != null) return r; }
            // leegwaarde is the §5.8.2 empty-set fallback; unreachable on a built defaults group today (an
            // aggregation consequence already declines via scalarWaarde) — checked anyway so a site
            // can never hide in the fallback if that fence ever moves.
            return foutplekSoort(f.leegwaarde());
        }
        if (e instanceof Expr.TekstReeks tr) {
            for (Expr.TekstReeks.Deel d : tr.delen())
                if (d instanceof Expr.TekstReeks.Ingevoegd i) { String r = foutplekSoort(i.expr()); if (r != null) return r; }
            return null;
        }
        if (e instanceof Expr.DimensieCel dc) return foutplekSoort(dc.attribuut());
        return null;
    }

    private static void verzamelDefaultsGlobals(List<GecompileerdeDefault> defaults, Set<String> out) {
        for (GecompileerdeDefault r : defaults) {
            if (r.consequentie() != null) out.addAll(r.consequentie().globals());
            if (r.excepties() != null) for (List<GecompileerdeDefault> sub : r.excepties()) verzamelDefaultsGlobals(sub, out);
        }
    }

    /** Whether the group can reach a conflict outcome — it has ≥2 members (a root declared-unordered group) or
     *  carries anywhere a default whose excepties holds ≥2 exception groups (a nested group, ch. 7.9). */
    private static boolean bevatMeervoudigeExcepties(List<List<GecompileerdeDefault>> leden) {
        for (List<GecompileerdeDefault> m : leden) if (bevatMeervoudigeExceptiesR(m)) return true;
        return false;
    }
    private static boolean bevatMeervoudigeExceptiesR(List<GecompileerdeDefault> defaults) {
        for (GecompileerdeDefault r : defaults) {
            if (r.excepties() != null) {
                if (r.excepties().size() >= 2) return true;
                for (List<GecompileerdeDefault> sub : r.excepties()) if (bevatMeervoudigeExceptiesR(sub)) return true;
            }
        }
        return false;
    }

    /** Group-local salience: strictly below every producer of every read of every default (§5.1). Collect the
     *  kenmerk/attribute names the group reads (justifications + consequences, recursed through excepties), find the
     *  resolved rules that WRITE them on this subject type, and sit one below the minimum of their salience — so a
     *  monotone characteristic write decides only at the fixpoint of its reads. No producers (input-only reads) -> 0. */
    private int defaultsSalience(List<PrioritizedDefaults> members, Objecttype onderwerp) {
        Set<String> reads = new LinkedHashSet<>();
        for (PrioritizedDefaults l : members) verzamelDefaultsLeesnamen(l.defaults(), reads);
        int minProd = 0; boolean any = false;
        for (ResolvedRegel r : model.resolvedRegels) {
            if (!(r.doel() instanceof Expr.AttribuutRef d) || d.attribuut() == null) continue;
            if (!reads.contains(d.attribuut())) continue;
            // a producer declared on an ANCESTOR also writes this type's facts (ch. 2.2), so the
            // stack must sit below it too — otherwise the reduce-rule can fire before its reads settle
            if (d.objecttype() == null || !onderwerp.isEen(d.objecttype()) && !gelijkeType(d.objecttype(), onderwerp))
                continue;
            any = true;
            minProd = Math.min(minProd, strat.salience(r));
        }
        return any ? minProd - 1 : 0;
    }
    private static boolean gelijkeType(Objecttype a, Objecttype b) {
        return a == b || (a != null && b != null && a.naam != null && a.naam.equalsIgnoreCase(b.naam));
    }
    private void verzamelDefaultsLeesnamen(List<Default> defaults, Set<String> uit) {
        for (Default r : defaults) {
            conditieLeesnamen(r.voorwaarde(), uit);
            exprLeesnamen(r.consequentie(), uit);
            if (r.excepties() != null) for (ExceptionGroup sl : r.excepties().leden()) verzamelDefaultsLeesnamen(sl.defaults(), uit);
        }
    }
    private void conditieLeesnamen(Conditie c, Set<String> uit) {
        if (c == null) return;
        if (c instanceof Conditie.Kenmerk k) { if (k.kenmerk() != null) uit.add(k.kenmerk()); }
        else if (c instanceof Conditie.Samengesteld sam) { for (Conditie d : sam.onderdelen()) conditieLeesnamen(d, uit); }
        else if (c instanceof Conditie.Vergelijking v) { exprLeesnamen(v.links(), uit); exprLeesnamen(v.rechts(), uit); }
        else if (c instanceof Conditie.Predicaat p) { exprLeesnamen(p.operand(), uit); }
        else if (c instanceof Conditie.Periode pd) { exprLeesnamen(pd.vanaf(), uit); exprLeesnamen(pd.tot(), uit); }
    }
    private void exprLeesnamen(Expr e, Set<String> uit) {
        if (e == null) return;
        // ch. 4.10: a Let-wrapped read is still a read — the group's salience must sit below its producer.
        for (Expr k : letKinderen(e)) exprLeesnamen(k, uit);
        if (e instanceof Expr.AttribuutRef a) { if (a.attribuut() != null) uit.add(a.attribuut()); }
        else if (e instanceof Expr.Binair b) { exprLeesnamen(b.links(), uit); exprLeesnamen(b.rechts(), uit); }
        else if (e instanceof Expr.Afgerond af) { exprLeesnamen(af.basis(), uit); }
        else if (e instanceof Expr.Begrensd bg) { exprLeesnamen(bg.basis(), uit); exprLeesnamen(bg.minimum(), uit); exprLeesnamen(bg.maximum(), uit); }
        else if (e instanceof Expr.Functie f) { for (Expr a : f.argumenten()) exprLeesnamen(a, uit); }
        else if (e instanceof Expr.DimensieCel dc) { exprLeesnamen(dc.attribuut(), uit); }
        else if (e instanceof Expr.TekstReeks tr) {
            for (Expr.TekstReeks.Deel d : tr.delen()) if (d instanceof Expr.TekstReeks.Ingevoegd i) exprLeesnamen(i.expr(), uit);
        }
    }

    /** A single defaults decision (ch. 7.1): a characteristic besluit XOR an attribute value. */
    private record Beslissing(boolean isChar, boolean charBesluit, Object attrWaarde) {}

    /** A chain/reconcile result (ch. 7.1/7.2/7.9): UNDECIDED (fall-through), DECIDED (the winning decision), or
     *  CONFLICT (a differing declared-unordered group, keyed by its default-index path). */
    private record Uitkomst(int soort, Beslissing beslissing, String conflictSleutel) {
        static final int UNDECIDED = 0, DECIDED = 1, CONFLICT = 2;
        static Uitkomst undecided() { return new Uitkomst(UNDECIDED, null, null); }
        static Uitkomst decided(Beslissing b) { return new Uitkomst(DECIDED, b, null); }
        static Uitkomst conflict(String key) { return new Uitkomst(CONFLICT, null, key); }
    }

    /** A compiled default: the subject-local justification (null on the initialisation default — justified iff the
     *  target is empty), the characteristic decision (or null), the attribute value fn (or null), and the nested
     *  exception-group members (or null). {@code heeftEigenInhoud} separates a form-a default from a contentless
     *  group-default. */
    private record GecompileerdeDefault(Predicate1<Object> justificatie, boolean initialisatie, Boolean besluit,
            WaardeFn consequentie, List<List<GecompileerdeDefault>> excepties, boolean heeftEigenInhoud) {}

    /** A loud, reason-carrying fence raised while compiling defaults the scalar-first back-end does not carry. */
    private static final class DefaultsOnvertaalbaar extends RuntimeException {
        DefaultsOnvertaalbaar(String reden) { super(reden); }
    }

    /** One fire of a defaults group over one subject — the RHS port of ref's {@code run_defaults_group} per-subject
     *  body (eval.ml:1645-1771): the top-down walk over the defaults with leeg fall-through, recursion into exception
     *  groups, and member reconciliation. The flippable corpus has no channel-depositing consequence, so a default's
     *  own decision evaluates its consequence directly (no speculative buffer — that case is fenced, not built). */
    private static final class DefaultsRun {
        private final Object s;
        private final Map<String, BigDecimal> gm;
        private final java.util.function.Function<Object, Object> doelGetter;
        private final boolean isChar;
        private final String targetForm;
        DefaultsRun(Object s, Map<String, BigDecimal> gm, java.util.function.Function<Object, Object> doelGetter,
                    boolean isChar, String targetForm) {
            this.s = s; this.gm = gm; this.doelGetter = doelGetter; this.isChar = isChar; this.targetForm = targetForm;
        }

        private boolean justified(GecompileerdeDefault r) {
            if (r.initialisatie()) return doelGetter.apply(s) == null;   // ch. 7.3: justified iff target empty in the input
            if (r.justificatie() == null) return false;
            try { return r.justificatie().test(s); }              // Drools Predicate1.test declares a checked Exception
            catch (RuntimeException e) { throw e; }
            catch (Exception e) { throw new RuntimeException(e); }
        }

        /** A default's OWN decision (ch. 7.1), exception group aside — a Beslissing when it decides, null when it
         *  falls through. */
        private Beslissing ownDecision(GecompileerdeDefault r) {
            if (isChar) {
                if (r.besluit() == null) return null;   // a group-default has no own besluit
                return justified(r) ? new Beslissing(true, r.besluit(), null) : null;
            }
            if (r.consequentie() == null || !justified(r)) return null;
            Object v = collapseBreuk(r.consequentie().fn().apply(s, gm));
            if (v == null) return null;   // ch. 7.1: a leeg consequence falls through
            return new Beslissing(false, false, v);
        }

        Uitkomst decide(List<GecompileerdeDefault> defaults, List<Integer> path) {
            for (int i = 0; i < defaults.size(); i++) {
                GecompileerdeDefault r = defaults.get(i);
                if (r.excepties() != null) {
                    // the exception group goes first (ch. 7.9): reconcile it at this default's path; a settling
                    // exception group (deciding OR conflicting) displaces every lower default.
                    List<Integer> subPath = new ArrayList<>(path); subPath.add(i);
                    List<Uitkomst> results = new ArrayList<>();
                    for (List<GecompileerdeDefault> sub : r.excepties()) results.add(decide(sub, subPath));
                    Uitkomst reconciled = reconcile(results, subPath);
                    if (reconciled.soort() != Uitkomst.UNDECIDED) return reconciled;
                    // an empty exception group falls back: a form-a default to its own decision, a group-default to
                    // the next default.
                    if (r.heeftEigenInhoud()) {
                        Beslissing d = ownDecision(r);
                        if (d != null) return Uitkomst.decided(d);
                    }
                } else {
                    Beslissing d = ownDecision(r);
                    if (d != null) return Uitkomst.decided(d);
                }
            }
            return Uitkomst.undecided();
        }

        /** Reconcile a group's member results (ch. 7.2, applied recursively by ch. 7.9): a deeper conflict
         *  propagates unchanged; otherwise the deciding members must agree — equal decisions are one decision,
         *  differing decisions are a conflict keyed at {@code path}; members deciding nothing drop out. */
        Uitkomst reconcile(List<Uitkomst> results, List<Integer> path) {
            for (Uitkomst u : results) if (u.soort() == Uitkomst.CONFLICT) return u;
            List<Beslissing> decided = new ArrayList<>();
            for (Uitkomst u : results) if (u.soort() == Uitkomst.DECIDED) decided.add(u.beslissing());
            if (decided.isEmpty()) return Uitkomst.undecided();
            Beslissing d0 = decided.get(0);
            for (int i = 1; i < decided.size(); i++)
                if (!beslissingGelijk(d0, decided.get(i))) return Uitkomst.conflict(pathKey(path));
            return Uitkomst.decided(d0);
        }

        private String pathKey(List<Integer> path) {
            if (path.isEmpty()) return targetForm;
            StringBuilder sb = new StringBuilder(targetForm);
            for (int i : path) sb.append('/').append(i);
            return sb.toString();
        }

        private static boolean beslissingGelijk(Beslissing a, Beslissing b) {
            if (a.isChar() != b.isChar()) return false;
            if (a.isChar()) return a.charBesluit() == b.charBesluit();
            return waardeGelijk(a.attrWaarde(), b.attrWaarde());
        }
        private static boolean waardeGelijk(Object x, Object y) {
            if (x == null) return y == null;
            if (x instanceof BigDecimal bx && y instanceof BigDecimal by) return bx.compareTo(by) == 0;
            return x.equals(y);
        }
    }

    /** The subject's OWN declared attributes a value expression READS, as bean property names — the
     *  {@code .watch} set for an otherwise constraint-less pattern (mirrors the DRL's verzamelEigenReads). */
    private java.util.Set<String> eigenReads(Expr e, Objecttype subject) {
        java.util.LinkedHashSet<String> uit = new java.util.LinkedHashSet<>();
        eigenReads(e, subject, uit);
        return uit;
    }
    private void eigenReads(Expr e, Objecttype subject, java.util.Set<String> uit) {
        // ch. 4.10: a Let and an IfThenElse hide their reads from this walk (it matches concrete v0 shapes only).
        // Missing them would leave a chained
        // berekening ("het gewogen inkomen is (Let ink = inkomen in ink maal de factor)") with an EMPTY
        // reactivity mask — computed once against a leeg factor and never recomputed. See letKinderen.
        for (Expr k : letKinderen(e)) eigenReads(k, subject, uit);
        if (e instanceof Expr.AttribuutRef a) {
            if (ongenavigeerd(a) && a.objecttype() == subject && subject.attribuut(a.attribuut()) != null)
                uit.add(prop(a.attribuut()));
        } else if (e instanceof Expr.Binair b) {
            eigenReads(b.links(), subject, uit);
            eigenReads(b.rechts(), subject, uit);
        } else if (e instanceof Expr.Functie f) {
            for (Expr a : f.argumenten()) eigenReads(a, subject, uit);
        } else if (e instanceof Expr.Afgerond af) {
            eigenReads(af.basis(), subject, uit);
        } else if (e instanceof Expr.Begrensd bg) {
            eigenReads(bg.basis(), subject, uit);
            eigenReads(bg.minimum(), subject, uit);
            eigenReads(bg.maximum(), subject, uit);
        } else if (e instanceof Expr.TekstReeks tr) {
            for (Expr.TekstReeks.Deel d : tr.delen())
                if (d instanceof Expr.TekstReeks.Ingevoegd i) eigenReads(i.expr(), subject, uit);
        } else if (e instanceof Expr.DimensieCel dc) {
            eigenReads(dc.attribuut(), subject, uit);
        }
    }

    /** The to-ONE NAVIGATED AttribuutRefs a value expression READS (a ref with a navigatieRol, or one whose
     *  objecttype differs from {@code subject} — the self-anaphora "van de &lt;rol&gt;" form) — the far-side reads
     *  the freshness watch CANNOT cover: property/class reactivity is per fact TYPE, so a subject pattern never
     *  re-fires on the FAR fact's update. Each is turned into a far-side JOIN pattern (see {@link #navReactiviteit})
     *  so the rule reacts to the navigated fact's mutation. Walks the SAME tree shape as {@link #eigenReads}. */
    private void navRefs(Expr e, Objecttype subject, java.util.List<Expr.AttribuutRef> uit) {
        for (Expr k : letKinderen(e)) navRefs(k, subject, uit);   // ch. 4.10: see into a Let / IfThenElse
        if (e instanceof Expr.AttribuutRef a) {
            if (!ongenavigeerd(a) || (a.objecttype() != null && a.objecttype() != subject)) uit.add(a);
        } else if (e instanceof Expr.Binair b) {
            navRefs(b.links(), subject, uit);
            navRefs(b.rechts(), subject, uit);
        } else if (e instanceof Expr.Functie f) {
            for (Expr a : f.argumenten()) navRefs(a, subject, uit);
        } else if (e instanceof Expr.Afgerond af) {
            navRefs(af.basis(), subject, uit);
        } else if (e instanceof Expr.Begrensd bg) {
            navRefs(bg.basis(), subject, uit);
            navRefs(bg.minimum(), subject, uit);
            navRefs(bg.maximum(), subject, uit);
        } else if (e instanceof Expr.TekstReeks tr) {
            for (Expr.TekstReeks.Deel d : tr.delen())
                if (d instanceof Expr.TekstReeks.Ingevoegd i) navRefs(i.expr(), subject, uit);
        } else if (e instanceof Expr.DimensieCel dc) {
            navRefs(dc.attribuut(), subject, uit);
        }
    }
}
