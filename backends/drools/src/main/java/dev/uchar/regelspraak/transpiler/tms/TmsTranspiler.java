package dev.uchar.regelspraak.transpiler.tms;

import dev.uchar.regelspraak.graaf.ProducentConsument;
import dev.uchar.regelspraak.graaf.ProducentConsument.ElementSleutel;
import dev.uchar.regelspraak.ir.Conditie;
import dev.uchar.regelspraak.ir.Default;
import dev.uchar.regelspraak.ir.Expr;
import dev.uchar.regelspraak.ir.Geldigheid;
import dev.uchar.regelspraak.ir.PrioritizedDefaults;
import dev.uchar.regelspraak.ir.ResolvedConsistentie;
import dev.uchar.regelspraak.ir.ResolvedDagsoort;
import dev.uchar.regelspraak.ir.ResolvedFeitCreatie;
import dev.uchar.regelspraak.ir.ResolvedObjectCreatie;
import dev.uchar.regelspraak.ir.ResolvedRegel;
import dev.uchar.regelspraak.ir.ResolvedUniciteit;
import dev.uchar.regelspraak.ir.ResolvedUniciteitUnie;
import dev.uchar.regelspraak.ir.ResolvedVerdeling;
import dev.uchar.regelspraak.ir.Spelling;
import dev.uchar.regelspraak.kern.Literalen;
import dev.uchar.regelspraak.kern.Tijdrekening;
import dev.uchar.regelspraak.model.Datatype;
import dev.uchar.regelspraak.model.Eenheid;
import dev.uchar.regelspraak.model.Rationaal;
import dev.uchar.regelspraak.model.RegelSpraakModel;
import dev.uchar.regelspraak.model.RegelSpraakModel.Objecttype;
import dev.uchar.regelspraak.model.RegelSpraakModel.Parameter;
import dev.uchar.regelspraak.model.RegelSpraakModel.RolBindingRef;
import dev.uchar.regelspraak.runtime.Accessors;
import dev.uchar.regelspraak.runtime.Breuk;
import dev.uchar.regelspraak.runtime.RegelSpraakRuntime;
import dev.uchar.regelspraak.schema.FactModelGenerator;
import dev.uchar.regelspraak.transpiler.kern.SourcePath;
import dev.uchar.regelspraak.transpiler.kern.DayKinds;
import dev.uchar.regelspraak.transpiler.kern.Dekking;
import dev.uchar.regelspraak.transpiler.kern.EffectiveUnit;
import dev.uchar.regelspraak.transpiler.kern.UnitConflict;
import dev.uchar.regelspraak.transpiler.kern.Host;
import dev.uchar.regelspraak.transpiler.kern.Namen;
import dev.uchar.regelspraak.transpiler.kern.Operators;
import dev.uchar.regelspraak.transpiler.kern.StandingTargets;
import dev.uchar.regelspraak.transpiler.kern.Timelines;
import dev.uchar.regelspraak.transpiler.kern.WindowPosition;
import dev.uchar.regelspraak.transpiler.tms.plan.Accumulation;
import dev.uchar.regelspraak.transpiler.tms.plan.Alpha;
import dev.uchar.regelspraak.transpiler.tms.plan.Exists;
import dev.uchar.regelspraak.transpiler.tms.plan.BetaTest;
import dev.uchar.regelspraak.transpiler.tms.plan.Binding;
import dev.uchar.regelspraak.transpiler.tms.plan.StaticFunction;
import dev.uchar.regelspraak.transpiler.tms.plan.Consequence;
import dev.uchar.regelspraak.transpiler.tms.plan.Join;
import dev.uchar.regelspraak.transpiler.tms.plan.ReadKind;
import dev.uchar.regelspraak.transpiler.tms.plan.ReadTable;
import dev.uchar.regelspraak.transpiler.tms.plan.LhsItem;
import dev.uchar.regelspraak.transpiler.tms.plan.Modelplan;
import dev.uchar.regelspraak.transpiler.tms.plan.Not;
import dev.uchar.regelspraak.transpiler.tms.plan.Subject;
import dev.uchar.regelspraak.transpiler.tms.plan.FactPattern;
import dev.uchar.regelspraak.transpiler.tms.plan.RulePlan;
import dev.uchar.regelspraak.transpiler.tms.plan.Term;
import dev.uchar.regelspraak.transpiler.tms.plan.TestItem;

import org.drools.model.Model;
import org.kie.api.runtime.ClassObjectFilter;
import org.kie.api.runtime.KieSession;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoField;
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
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * The analysis that both branches of the backend render: a uchar IR document to the plan of its rules, which the
 * exec-model branch renders as a Drools executable model ({@link PatternDslRendering}) and the DRL-text branch as DRL text
 * ({@code DrlRendering}), each deriving with {@code drools.logicalInsert}. A derived fact stands exactly as long as the
 * condition of a rule that inserted it holds, and goes, with everything that rested on it, when that condition stops
 * holding. The outcome of an acyclic
 * rule set then does not depend on which rule the agenda fires first, which a lowering that writes the beans secures
 * only with measures of its own (a rule attribute against re-firing, a priority per stratum, class reactivity, a
 * conditional bean update, a record of an initialisation's own write), as the exec-model branch did until card
 * tms-omschakeling (2026-10-10) and the DRL-text branch until card drl-teksttak-op-truth-maintenance, which made it a
 * rendering of this analysis. Neither branch carries any of them now; the fire cap of {@code DroolsBackend.vuur}
 * stays.
 *
 * <p>Truth maintenance follows only what the left-hand side of a rule binds: a value a consequence reads with a
 * getter, or looks up in working memory itself, records no dependency, so a derived fact computed from it stays when
 * that value changes. Every read a derived value depends on is therefore a bound pattern here, and a consequence
 * computes only with bound variables. A member no form writes is input, stable during a session, and is bound off the
 * bean pattern, over the reference fields where the read navigates; a derived member is read through its total
 * standing ({@link CharacteristicState}, {@link State}), over a path through {@link PathState}, a positive pattern whatever
 * polarity a check tests; the §9.5 channel of a form through {@link ChannelState}; a parameter through its
 * {@link ParameterState}. The case input stays stated on the beans and the branch never writes a bean while it fires,
 * so an initialisation's guard reads the input of its derived target off the bean as well; {@link #zetAfgeleideWaarden}
 * writes the derivations onto the beans afterwards.
 *
 * <p>What it lowers: a characteristic attribution; an assignment or a computation of an attribute of the subject, which
 * writes nothing for a computed leeg on a standing target (RUNTIME-SEMANTICS ch. 7.8, or beside one unconditional
 * initialisation, the desugaring domain of ch. 7.7) and writes the leeg elsewhere (ch. 1.2g), any number of them on one
 * target, two that stand on one object with different values being the fault of ch. 5.7 ({@link #differenceRule}); an
 * initialisation (ch. 5.4), the only one on its target per concrete type; a
 * consistency, whose violation is a logical §9.5 inconsistency; a uniqueness, over one source or more, whose
 * duplicates are as well ({@link #unionUniquenessRules}); a prioritized-defaults group, as the effective
 * stack per concrete type of ch. 7.10, which decides by logical control facts ({@link ControlFact}) and into which an
 * ordinary rule on an ancestor's level folds, so that it does not fire on that type; an allocation, as a computation
 * over the recipients its left-hand side binds ({@link #allocation}); and an object creation, over its
 * {@code connectingRole} binding or else over the relation its type pair derives, and a fact creation, each over a
 * relation of two roles. Each always valid or gated on the
 * evaluation date by its validity window (RUNTIME-SEMANTICS ch. 8.2), with the §6.5 deposit of a division by leeg and
 * the one of a square root of a negative number where the evaluation reaches them (a default carries no division and no
 * root at all), whose condition, assertion or justification is built from characteristic checks (own or over a path,
 * negated or not), comparisons, leeg, gevuld, eleven and exact-digit tests, time windows, role checks
 * ({@link #roleCheck}), quantifier folds over a linked set ({@link #quantifierFold}), tests of the channel of a
 * consistency, a uniqueness or a rule that deposits, and {@code Compound}s of those under any quantifier, and whose
 * value is a
 * literal, a read of an attribute (own or over a path of roles, or a dimension cell), a parameter, a context value, an
 * aggregation, a {@code Let} with its binder reads, an {@code IfThenElse}, a rounding, a clamp, a builtin call, a text
 * composition of texts, or an arithmetic of those: a sum, difference, product, quotient, power with a whole exponent or
 * percentage, with the units restated where they differ (ch. 2.6), or a date plus or minus a duration. Every other form
 * is in the coverage manifest as not lowered, with a reason that names it, and {@code DroolsBackend} refuses the
 * document on the first such entry.
 *
 * <p>An aggregation over a set source is an accumulate in the left-hand side of the rule that reads it, over the
 * {@link Contribution} facts its contribution rules insert logically, one per member of its range ({@link #fold}); a
 * contribution rule binds the member's value and the reads of the fold's filter in its own left-hand side, so a member
 * whose value or filter reading changes takes its contribution along. A listed fold is computed from its operands'
 * bound values ({@link #listedFold}). A quantifier fold has the same contributions, each carrying its member's
 * verdict on the matrix, and where the matrix can fault or deposit also the member's place, so the rule that reads the
 * fold replays the short circuit of RUNTIME-SEMANTICS ch. 5.10 over them ({@link #quantifierFold}).
 *
 * <p>An evaluation fault in a value or a condition — a division by a numeric zero (RUNTIME-SEMANTICS ch. 1.4, point 4),
 * or one of the faults of {@link Rekenfout}: a date step or a duration count outside the range of ch. 3.8, a context
 * value or a validity window without an evaluation date, an {@code Unresolved} node the evaluation reaches — is a
 * {@link Fout} inserted logically and never an exception out of the fire: the branch passes intermediate states the
 * reference interpreter does not see, and a fault thrown there could not be withdrawn when the state passes. The
 * consequence that computes a value inserts the fault instead of the value. A condition is tested in the left-hand
 * side, where Drools cannot take an exception as a fact, so its test answers that it does not hold where it faults,
 * and a rule of the form's own inserts the fault ({@link #conditionFaultRule}). A fold's filter that faults for a
 * member inserts no fault but a {@link FilterFault}: the form that reads the fold faults where its own evaluation reaches
 * the fold, as the reference interpreter evaluates the filter only there, with the fault of the first member in fold
 * order whose filter faults, a place the case input states per to-many list ({@link ListPlace}) ({@link #fold}). Only
 * the faults that stand at
 * the fixpoint count: {@link #zetAfgeleideWaarden} reports them before it writes anything back. A division of a filled
 * dividend by a leeg divisor, and a square root of a negative number, deposit the §9.5 inconsistency of ch. 1.2 under
 * the form whose evaluation reaches them, on its subject: the consequence that computes a value through a probe in its
 * value array ({@link DepositFlag}), a condition through a rule of the form's own ({@link #depositRule}), and a fold's
 * filter through a {@link FilterDeposit} of the member, which the form that reads the fold hears where its own
 * evaluation reaches the fold ({@link #readFold}).
 *
 * <p>A creation is the one form whose output is stated, not logical (RUNTIME-SEMANTICS ch. 5.5): a creation fires only
 * at a fixpoint of the rule layer, and what it makes stands, as in the reference interpreter, which keeps a minted
 * instance under a fixed label. A logical object could keep itself standing through its own output after the state that
 * made it had passed, and its initialisations would follow later changes, where ch. 5.5 evaluates them when the object
 * is made. Its rules sit in agenda groups of their own ({@link #FEITCREATIE}, {@link #OBJECTCREATIE}) and the driver
 * fires them in generations ({@code DroolsBackend.vuur}): the rule layer to rest, then the fact creations once and the
 * object creations once, both on that one fixpoint, then the rule layer to rest again, and so on, until a generation
 * makes nothing; an object creation records the creation depth of what it makes ({@link CreationStamp}), and a mint at depth
 * {@link #CREATION_BUDGET} + 1 is the fault ({@link CreationBudget}). The driver runs the generations per evaluation
 * stratum of the reference interpreter, in
 * ascending order ({@link #creationStrata}): a creation reads strictly, so it fires only once the creations whose output
 * it reads have reached their fixpoint, as the reference interpreter evaluates stratum k to its fixpoint before a form
 * of stratum k+1 fires (RUNTIME-SEMANTICS ch. 6.1, 6.3); an initialisation evaluated earlier would be fixed on a value
 * that grows. An object creation inserts
 * the new bean, a {@link CreationStamp} mark per creation and subject, the {@link RelationLink} of its connecting relation and
 * its initialisations as {@link Value} and {@link HasCharacteristic}, computed from what its rule binds
 * ({@link #objectCreation}); a fact creation inserts the {@link RelationLink} it makes ({@link #factCreation}). Every relation
 * a form creates is read through {@link RelationLink} facts, its input links included ({@link #linkRules}), since a
 * created link is in no bean field, and a second distinct filler of a to-one role is a {@link Fout} at the fixpoint.
 * The rule layer stays under truth maintenance throughout: it reads what a creation made as it reads input.
 *
 * <p>This class is the analysis: it decides per rule what is read, how, in which order, and what a firing inserts, as
 * a {@link RulePlan}, and delivers every plan with the globals of the model as one {@link Modelplan}. It builds no part
 * of the executable model itself; {@link PatternDslRendering#model} renders the outcome into it ({@link #model}), and
 * {@code DrlRendering} into DRL text ({@link #drl}).
 */
public final class TmsTranspiler {

    /** The Drools package of the rules of both branches: the executable model of the exec-model branch is built in it,
     *  and the DRL text of {@code DRL_TEXT} declares it and is added at its path. The rule of a form carries the form's
     *  name on both branches, so the two builds of one document have rules of the same names in this one package. Each
     *  build has a KieBase of its own, and a consumer that loads both branches keeps them apart by their builds; the
     *  package does not tell them apart. */
    public static final String PKG = "dev.uchar.regelspraak.tms";

    /** The agenda group of the fact creations. A creation fires only at a fixpoint of the rule layer, so its rules are
     *  kept out of the main group, where every other rule of the branch sits: a plain fire never fires them, and the
     *  driver gives this group the focus once per generation ({@code DroolsBackend.vuur}). An agenda filter cannot
     *  hold them back instead: Drools 8.44 takes a match the filter rejects off the agenda for good
     *  ({@code RuleExecutor.fire}, {@code cancelAndContinue}), so a creation rejected while the rule layer fires would
     *  never fire later. */
    public static final String FEITCREATIE = "uchar-feitcreatie";

    /** The agenda group of the object creations. The driver puts it on the focus stack beneath the group of the fact
     *  creations ({@link #FEITCREATIE}), so in a generation the fact creations fire first and the object creations right
     *  after them, before the rule layer fires again: every creation of a generation reads the same settled world, and an
     *  object creation does not read what a fact creation of its generation links (RUNTIME-SEMANTICS ch. 5.5,
     *  Generations). */
    public static final String OBJECTCREATIE = "uchar-objectcreatie";

    /** The creation budget (RUNTIME-SEMANTICS ch. 5.5): the deepest an object creation makes an object in its stratum.
     *  A mint at one depth more is the fault, at the making, as in the reference interpreter and the ASP backend
     *  ({@code oc_budget}); a fact creation makes no object and has no budget. The depth follows the chain of makings,
     *  so a creation whose condition waits generations for a link or a value keeps its subject's depth, and the driver
     *  needs no bound on the generations: under this budget a stratum makes finitely many objects and links. */
    public static final int CREATION_BUDGET = 100;

    /** The most values one condition of this branch reads at once: the DSL's standalone test takes at most thirteen
     *  bound variables. A consequence has no such ceiling ({@link Consequence}). */
    static final int MAX_READS = 13;

    private static final String STILL_NOT = "deze tak draagt deze vorm nog niet";
    static final String INCONSISTENCY = FactModelGenerator.PAKKET + ".Inconsistentie";

    private final RegelSpraakModel model;
    private final ClassLoader beanLoader;
    private final List<PrioritizedDefaults> defaults;
    private final Map<Object, List<RolBindingRef>> navigationBindings;
    /** The one-step range of a fold ({@code sourceRole}), by node. */
    private final Map<Object, RolBindingRef> roleBindings;
    /** The multi-step range of a fold ({@code sourcePath}), by node. */
    private final Map<Object, List<RolBindingRef>> sourcePaths;
    /** The folds lowered so far, per node and fold subject: a node read again, by a rule's fault rule or by a default
     *  that two effective stacks share, reads the contributions already made for it. */
    private final Map<Object, Map<Objecttype, Fold>> folds = new IdentityHashMap<>();
    /** The contribution rules of the folds the lowered forms read, by the fold's key. */
    private final Map<String, List<RulePlan>> contributionRules = new LinkedHashMap<>();
    private int foldCounter;
    /** While the rules of a fold are built ({@link #foldFor}), the binders in scope at the fold, by normalised name:
     *  the rule that reads the fold binds them, its contribution rules do not ({@link Reads#outside}). */
    private Set<String> foldBinders = Set.of();
    /** The name of the form being lowered, as the manifest shows it: the key of its folds starts with it, and a fault in
     *  a fold's filter is named after it. */
    private String vorm;
    /** The datatype and unit of an expression as the other branches derive them (RUNTIME-SEMANTICS ch. 2.10). */
    private final EffectiveUnit eff;
    /** Every member a form of the document writes, lowered or not. A member that no form writes is input. */
    private Map<ElementSleutel, Set<String>> writers = Map.of();
    /** Per attribute, the assignments and computations of the document that write it. */
    private final Map<ElementSleutel, List<ResolvedRegel>> computeRules = new LinkedHashMap<>();
    /** Per attribute, the initialisations of the document that write it. */
    private final Map<ElementSleutel, List<ResolvedRegel>> initialisations = new LinkedHashMap<>();
    /** The members a defaults group writes, on their declaring type. */
    private final Set<ElementSleutel> defaultsTargets = new LinkedHashSet<>();
    /** The effective stacks of RUNTIME-SEMANTICS ch. 7.10 and the fold of ordinary rules into them. */
    private Stapels stapels;
    /** The targets on which a computed leeg writes nothing (RUNTIME-SEMANTICS ch. 7.7 and 7.8), classified by the class
     *  both branches share. */
    private StandingTargets standingTargets;
    /** The names of the document's rules in its order, each once: the sentence of two writers that disagree names them
     *  in this order, as the reference interpreter's pass runs them (RUNTIME-SEMANTICS ch. 5.7), and the reader of the
     *  fixpoint takes, of writers that agree, the value of the first ({@link Schrijversconflict#inDocumentOrder}). A
     *  list, so that a plan carries the order itself and a DRL text can spell it. */
    private final List<String> documentOrder = new ArrayList<>();
    /** The object types of the document in its order ({@link #typen}), and at the same place the simple name of each
     *  one's bean class (the one {@link #beanClass} loads): a sentence names an object by its own type. By name, so
     *  that building it loads no class. */
    private final List<String> typeClasses = new ArrayList<>(), typen = new ArrayList<>();
    private final List<Dekking> dekking = new ArrayList<>();
    /** The form each rule of the model belongs to, by the rule's name ({@link #formOf}). */
    private final Map<String, String> formOfRule = new HashMap<>();
    /** The rules of the model that belong to no form of the document ({@link #formOf}). */
    private final Set<String> rulesWithoutForm = new LinkedHashSet<>();
    private final List<DefaultsGroup> defaultsGroups = new ArrayList<>();
    /** The manifest names that name no declaration, with the element each comes from ({@link #gegenereerd}). */
    private final Map<String, RegelSpraakModel.GegenereerdUit> gegenereerd = new LinkedHashMap<>();
    /** The members the lowered rules derive, by key. The key is normalised, and the setter a derivation is written back
     *  through is the bean property of the declared spelling, which the key no longer carries. */
    private final Map<ElementSleutel, Member> derived = new LinkedHashMap<>();
    /** The members a lowered rule reads through their standing, on its subject or at the end of a path: the derived
     *  ones, and every attribute on a timeline ({@link Reads#timeline}). Each gets its standing rules. */
    private final Map<ElementSleutel, Member> read = new LinkedHashMap<>();
    /** The reads of a derived member over a path: each gets its two path-standing rules. */
    private final Map<String, PathRead> pathReads = new LinkedHashMap<>();
    /** The producers whose §9.5 channel a lowered condition reads, by the name the channel test spells: each gets the
     *  two rules of its {@link ChannelState}. */
    private final Set<String> channels = new LinkedHashSet<>();
    /** The parameters a lowered rule reads, as the globals the case runner sets by name: the Java class of each, by
     *  name, in the order they were first read. */
    private final Map<String, Class<?>> globals = new LinkedHashMap<>();
    /** Whether the model declares the global the case runner sets from the case's {@code asOfDate}, the session's
     *  evaluation date (RUNTIME-SEMANTICS ch. 8.1), read through the {@link EvaluationDate} fact: false until a lowered
     *  form needs the date, so a model whose forms are all always valid and read no context value declares none. */
    private boolean evaluationDateGlobal;
    /** The name of that global, the one the case runner sets for every branch. */
    static final String REKENDATUM = "rekendatum";
    /** The relations a form of the document creates, by normalised fact type name: the connecting relation of an
     *  object creation and the relation of a fact creation. Their links are {@link RelationLink} facts, and every form
     *  that reads such a relation joins over those ({@link #linkHop}, {@link #linkRange}). */
    private final Set<String> createdRelations = new LinkedHashSet<>();
    /** The names of the lowered creation rules, those of {@link #FEITCREATIE} and of {@link #OBJECTCREATIE}: the driver
     *  counts their firings to tell a generation that created something from one that did not. */
    private final Set<String> creationRules = new LinkedHashSet<>();
    /** The evaluation stratum of every form of a document with a creation, by its node ({@link Cycles#strata}). */
    private Map<Object, Integer> strata = Map.of();
    /** The strata of the lowered creation rules ({@link #creationStrata}). */
    private final Set<Integer> creationStrata = new TreeSet<>();
    /** The reference fields under which a rule reads a {@link ListPlace} ({@link #place}; see {@link #listFields}). */
    private final Set<String> listFields = new LinkedHashSet<>();
    /** Unique per constraint across the model: the executable model shares a constraint between rules by its id, so
     *  two different tests under one id would collapse into the first. */
    private int idSeq;
    /** The outcome of the analysis, which {@link PatternDslRendering#model} renders into {@link #model}. */
    private Modelplan modelplan;
    private Model model0;
    /** The day-kind definitions of the document, each compiled to its predicate over a calendar day where a form first
     *  steps or counts in its day kind ({@link #dayKindPredicate}). A definition reads nothing of working memory
     *  (WF-031), so its predicate is a constant of the document and no rule binds anything for it. */
    private final DayKinds dayKinds;

    private TmsTranspiler(RegelSpraakModel model, ClassLoader beanLoader, List<PrioritizedDefaults> defaults,
                          Map<Object, List<RolBindingRef>> navigationBindings, Map<Object, RolBindingRef> roleBindings,
                          Map<Object, List<RolBindingRef>> sourcePaths, EffectiveUnit eff) {
        this.model = model;
        this.beanLoader = beanLoader != null ? beanLoader : TmsTranspiler.class.getClassLoader();
        this.defaults = defaults;
        this.navigationBindings = navigationBindings;
        this.roleBindings = roleBindings;
        this.sourcePaths = sourcePaths;
        this.eff = eff;
        this.dayKinds = new DayKinds(model);
    }

    /** Build the model and its coverage manifest from the loaded model and every side table the loader returns. Each
     *  table is required, also those no lowered form reads yet: a build that could leave one out could also lower a
     *  form that needs it as if the document carried none. Fact beans resolve against {@code beanLoader}; null routes
     *  to this transpiler's own loader. */
    public static TmsTranspiler build(RegelSpraakModel m, ClassLoader beanLoader, List<PrioritizedDefaults> defaults,
            Map<Object, RolBindingRef> roleBindings, List<Expr.Let> lets,
            Map<Object, List<RolBindingRef>> sourcePaths,
            Map<Object, List<RolBindingRef>> navigationBindings) {
        Objects.requireNonNull(defaults, "side table 'defaults' is null — pass an empty list for a document without one");
        Objects.requireNonNull(roleBindings, "side table 'rolBindingen' is null — pass an empty map for a document without one");
        Objects.requireNonNull(lets, "side table 'lets' is null — pass an empty list for a document without one");
        Objects.requireNonNull(sourcePaths, "side table 'bronPaden' is null — pass an empty map for a document without one");
        Objects.requireNonNull(navigationBindings, "side table 'navigatieBindingen' is null — pass an empty map for a document without one");
        // copied by identity: the loader keys its tables on the nodes, which are records with value equality
        Map<Object, List<RolBindingRef>> nav = new IdentityHashMap<>(navigationBindings);
        TmsTranspiler t = new TmsTranspiler(m, beanLoader, defaults, nav, new IdentityHashMap<>(roleBindings),
                new IdentityHashMap<>(sourcePaths), new EffectiveUnit(lets));
        t.modelplan = t.run();
        t.model0 = PatternDslRendering.model(t.modelplan);
        return t;
    }

    public Model model() { return model0; }

    /** The coverage manifest: one verdict per form, the rules in document order, then every other family. */
    public List<Dekking> dekking() { return dekking; }

    /**
     * The names of the manifest ({@link #dekking}) that name no declaration of the document, each with the declared
     * element it comes from, in the order of the manifest: an effective defaults stack ({@code defaults A:<type>.<member>},
     * {@code defaults K:<type>.<member>}) from the member it decides, as declared on its owner's chain; a version of a
     * form with several ({@code <form> #<k>}) from that form. Every other name of the manifest is the name of a form of
     * the model. A side table: the lowering reads none of it.
     */
    public Map<String, RegelSpraakModel.GegenereerdUit> gegenereerd() {
        return java.util.Collections.unmodifiableMap(gegenereerd);
    }

    /**
     * The form of the document that rule {@code ruleName} of the model belongs to, by the name the manifest gives it
     * ({@link #dekking}): the form whose lowering built the rule, with every rule that lowering adds beside the form's
     * own (its fault and deposit rules, the keys of a uniqueness, the recipients of an allocation, the chains and
     * agreements of an effective defaults stack), and for the contribution rules of a fold the form that first
     * registered the fold, also where a later form reads the same fold. Null for a rule of no form, one the lowering
     * keeps for every form alike: a standing, a path or channel standing, the standing of a parameter or of the
     * evaluation date, a link of the input over a created relation and its second filler, and the comparison of the
     * writers of one target. A name of no rule of the model is refused.
     */
    public String formOf(String ruleName) {
        String vorm = formOfRule.get(ruleName);
        if (vorm != null || rulesWithoutForm.contains(ruleName)) return vorm;
        throw new IllegalArgumentException("geen regel van dit model: '" + ruleName + "'");
    }

    /** The names of the creation rules of the model, in the agenda groups {@link #FEITCREATIE} and
     *  {@link #OBJECTCREATIE}; empty for a document without a lowered creation, which the driver fires in one go. */
    public Set<String> creationRules() { return Set.copyOf(creationRules); }

    /** The evaluation strata of the creation rules of the model, ascending, as the reference interpreter computes them
     *  over the verifier's dependency graph (RUNTIME-SEMANTICS ch. 6.3): the driver runs the generations of each in
     *  turn, and a creation rule fires in the phases of its own stratum and of every stratum above it
     *  ({@link CreationPhase}). Empty where {@link #creationRules} is. */
    public List<Integer> creationStrata() { return List.copyOf(creationStrata); }

    /** Whether a rule of the model reads a {@link ListPlace}: the filter fault rule of a fold whose filter can fault,
     *  the member fault rule of a per-element fold over a timeline, or the contribution rule of a quantifier fold whose
     *  matrix can fault or deposit, each over a range with a to-many step. Only then does the case input state the
     *  places of its to-many lists, and {@code DroolsBackend.voegIn} states them for a consumer. */
    public boolean readsListPlaces() { return !listFields.isEmpty(); }

    /** The reference fields under which a rule of the model reads a {@link ListPlace}: each the field a member of a
     *  to-many list holds toward the list's holder, named after the holder's role as the bean property is
     *  ({@link ListPlace#veld}). Empty where {@link #readsListPlaces} is false. */
    public Set<String> listFields() { return Set.copyOf(listFields); }

    /**
     * The size of the lowering of one effective defaults stack, counted when it is built: {@code naam} as the manifest
     * and the control facts name it, whether it took the general form (exception groups, or more than one group at the
     * root) or the simple one, the number of Drools rules it consists of, and the kinds of control fact those rules
     * insert ({@link ControlFact}). The standing rules of the target are shared with every other reader and not counted.
     */
    public record DefaultsGroup(String naam, boolean general, int rules, List<String> controlFacts) {}

    /** One entry per effective stack the branch lowered, in the order of the model. */
    public List<DefaultsGroup> defaultsGroups() { return List.copyOf(defaultsGroups); }

    /** The outcome of the analysis ({@link Modelplan}): the globals the model declares and the plan of every rule of
     *  the model, in the order of the model, which {@link PatternDslRendering#model} renders. No consumer reads a plan,
     *  so it is no public entry. */
    Modelplan modelplan() { return modelplan; }

    /** A kind of the plan that the rendering into DRL text does not carry yet ({@link #drl}), refused loudly by its
     *  kind, never left out of the text: a consequence the rendering does not spell yet, by its simple name, or a member
     *  of a bean that Drools reads as something else where a pattern spells it. */
    public static final class DrlRefusal extends RuntimeException {
        private final String soort;

        DrlRefusal(String soort, String melding, Throwable cause) {
            super(melding, cause);
            this.soort = soort;
        }

        /** The kind refused, such as the simple name of a consequence. */
        public String soort() { return soort; }
    }

    /** The outcome of the one rendering into DRL text ({@link #drl}): the text with the predicates of its day-kind
     *  globals, or the refusal of the rendering; both null until the first call. */
    private String drl;
    private Map<String, java.util.function.Predicate<LocalDate>> drlDayKinds;
    private DrlRendering.Refusal drlRefused;

    /**
     * The DRL text of the outcome of the analysis, the second rendering of the plan beside the executable model
     * ({@link #model}): the rules of the plan in its order, under their names, deriving with truth maintenance over the
     * same facts. Rendered once, at the first call. A kind the rendering does not carry yet is refused at every call with
     * {@link DrlRefusal}, never left out of the text.
     */
    public synchronized String drl() {
        if (drl == null && drlRefused == null) {
            try {
                DrlRendering.Rendered g = DrlRendering.render(modelplan);
                drl = g.text();
                drlDayKinds = g.dayKinds();
            } catch (DrlRendering.Refusal w) {
                drlRefused = w;
            }
        }
        if (drlRefused != null)
            throw new DrlRefusal(drlRefused.soort(), drlRefused.getMessage(), drlRefused);
        return drl;
    }

    /** The §9.8 day-kind predicates of the DRL text ({@link #drl}), by the name of the global the text declares for
     *  each, as the analysis compiled them into its plan; a session of the text has each set before it fires. Refused
     *  as {@link #drl} refuses. */
    public synchronized Map<String, java.util.function.Predicate<LocalDate>> drlDayKindGlobals() {
        drl();
        return drlDayKinds;
    }

    /**
     * What stands in {@code sessie} after a fire that reached its fixpoint, read without writing anything on a bean:
     * the derived values and characteristics, the objects and links the creations made, the evaluation faults that
     * stand, as data, and the §9.5 deposits. {@code label} names the objects of the case in the sentence of a fault, as
     * {@link #zetAfgeleideWaarden(KieSession, Function)} does; null names an object by type and identity.
     *
     * <p>Two different values on one attribute of one object have no common fixpoint (RUNTIME-SEMANTICS ch. 5.7). The
     * rule of {@link #differenceRule} makes that a {@link Fout} of the fixpoint where two writers can disagree; where no
     * such rule stood, as in a session filled by hand, this reads the same {@link Schrijversconflict} as a fault and gives
     * the attribute no value: it does not choose. Equal values are no conflict.
     *
     * <p>It reads only a session whose last run through the driver ({@code DroolsBackend.vuur}) reached its fixpoint,
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
    public Fixpunt lees(KieSession sessie, Function<Object, String> label) {
        return readFixpoint(sessie, label).fixpunt();
    }

    /**
     * Writes what stands at the fixpoint of {@code sessie} onto the beans, exactly what {@link #lees} gives, read by the
     * same code: {@code setX(true)} for each derived characteristic, the value of each derived attribute through the
     * setter of the declared attribute, a timeline as the bean model's segments, and each link a creation laid on the
     * reference field of every to-one end the bean model gives a field, the field the input of such a link is read from
     * ({@link #linkRules}). Input on a characteristic stays, since nothing writes {@code false}; input on an
     * attribute stays where no rule derived a value for it.
     *
     * <p>An evaluation fault that stands at the fixpoint ({@link Fout}) means the session has no answer, so before it
     * writes anything this throws the faults that stand: one as the exception it is, named after its rule, two or more
     * as {@link Evaluatiefouten}, ordered by the name of the form and then by the sentence, so that the report does not
     * depend on the order of the rules or of the firings. Then it writes nothing, and the session stays as it was. It
     * refuses the sessions {@link #lees} refuses, those without a fixpoint of the driver, likewise before it writes
     * anything.
     *
     * <p>The engine is not told of the writes, and a bean then holds derived values beside its input, which a further
     * fire would read as input. So after the writes this states a {@link WrittenBack} mark, and the driver refuses to fire
     * the session again ({@code DroolsBackend.vuur}). It states the mark with the {@link Watch} off, as its own work,
     * and has the watch record the relations after the links it wrote: {@link #lees} reads the session after the
     * write-back as before it.
     */
    public void zetAfgeleideWaarden(KieSession sessie) {
        zetAfgeleideWaarden(sessie, null);
    }

    /**
     * As {@link #zetAfgeleideWaarden(KieSession)}, with {@code label} naming the objects of the case: a
     * {@link Schrijversconflict} then names its object by the case label it gives, and as made by a creation where it
     * gives null, as the reference interpreter names the instance, and an {@link Invoervormfout} names its object by
     * that label. Null names an object by type and identity.
     */
    public void zetAfgeleideWaarden(KieSession sessie, Function<Object, String> label) {
        Read readFixpoint = readFixpoint(sessie, label);
        List<Fout> fouten = readFixpoint.fixpunt().fouten();
        if (fouten.size() == 1) throw fouten.get(0).fout();
        if (!fouten.isEmpty()) throw new Evaluatiefouten(fouten);
        readFixpoint.writeActs().forEach(Runnable::run);
        // the entry's own insert and links: the session is read after them as before them
        Watch watch = Watch.van(sessie);
        watch.without(sessie, () -> sessie.insert(new WrittenBack()));
        watch.recordRelations();
    }

    /** What {@link #lees} reads, with beside it the writes that put it on the beans, made from the same facts in the
     *  same pass so that the reading and the write-back cannot differ. */
    private record Read(Fixpunt fixpunt, List<Runnable> writeActs) {}

    /** What a consumer does where something happened outside the driver after its last run ({@link Watch}): fire
     *  again, or open a fresh session where the driver refuses a further fire ({@code DroolsBackend.vuur}). */
    private String fireAway(KieSession sessie, Watch watch, String agenda) {
        if (!creationRules.isEmpty())
            return "dit model heeft creatieregels, dus DroolsBackend.vuur weigert een tweede fire: open een verse sessie";
        if (!sessie.getObjects(new ClassObjectFilter(WrittenBack.class)).isEmpty())
            return "deze sessie is teruggezet, dus DroolsBackend.vuur weigert haar: open een verse sessie met beans die"
                    + " alleen invoer dragen";
        if (watch.onOrOff() != null)
            return "invoer die buiten DroolsBackend.voegIn om bijkwam of wegging, vraagt een verse sessie, dus"
                    + " DroolsBackend.vuur weigert haar: open een verse sessie met alle beans in één Invoer";
        if (watch.moreThanOneUpdate() != null || agenda != null)
            return "DroolsBackend.vuur weigert een sessie waarin buiten vuur om meer gebeurde dan een update van een"
                    + " attribuut of kenmerk van een bean: open een verse sessie met alle beans in één Invoer";
        if (watch.changedGlobal(sessie) != null)
            return "DroolsBackend.vuur weigert een sessie waarvan een global na de eerste vuur veranderde: open een verse"
                    + " sessie";
        return "vuur haar eerst opnieuw met DroolsBackend.vuur";
    }

    private Read readFixpoint(KieSession sessie, Function<Object, String> label) {
        // per class, as the driver asks for it: a filter of another kind walks every fact the session holds
        List<Object> marks = new ArrayList<>(sessie.getObjects(new ClassObjectFilter(Fired.class)));
        if (marks.isEmpty())
            throw new IllegalStateException("geen fixpunt om te lezen of terug te zetten: deze sessie is niet via"
                    + " DroolsBackend.vuur gevuurd, dat tot het fixpunt vuurt"
                    + (creationRules.isEmpty() ? "" : " en de creaties van dit model in generaties laat vuren"));
        if (marks.size() > 1)
            throw new IllegalStateException("de sessie houdt " + marks.size() + " merken van DroolsBackend.vuur, waar"
                    + " de driver er na elke run één laat: " + marks);
        String geenFixpunt = ((Fired) marks.get(0)).geenFixpunt();
        if (geenFixpunt != null)
            throw new IllegalStateException("geen fixpunt om te lezen of terug te zetten: de laatste DroolsBackend.vuur"
                    + " van deze sessie bereikte er geen — " + geenFixpunt);
        Watch watch = Watch.van(sessie);
        // the driver puts the watch back after every run, so only a consumer that took it off leaves a mark without it,
        // and the driver refuses such a session
        if (watch == null)
            throw new IllegalStateException("geen fixpunt om te lezen of terug te zetten: de bewaking die"
                    + " DroolsBackend.vuur op deze sessie zet, staat er niet meer op, dus wat er na de laatste vuur"
                    + " gebeurde, is niet te zien; DroolsBackend.vuur weigert haar ook: open een verse sessie met alle"
                    + " beans in één Invoer");
        String agenda = Watch.agenda(sessie);
        String outsideClause = watch.outsideClause();
        String description = outsideClause == null ? agenda : agenda == null ? outsideClause : outsideClause + " en " + agenda;
        if (description != null)
            throw new IllegalStateException("geen fixpunt om te lezen of terug te zetten: sinds de laatste"
                    + " DroolsBackend.vuur van deze sessie gebeurde er iets buiten vuur om, wat het fixpunt van die vuur"
                    + " kan veranderen (" + description + "); " + fireAway(sessie, watch, agenda));
        String global = watch.changedGlobal(sessie);
        if (global != null)
            throw new IllegalStateException("geen fixpunt om te lezen of terug te zetten: " + global + ", en het fixpunt"
                    + " hoort bij de oude waarde; DroolsBackend.vuur weigert de sessie, dus open een verse sessie");
        String relation = watch.changedRelation();
        if (relation != null)
            throw new IllegalStateException("geen fixpunt om te lezen of terug te zetten: " + relation + ", en het fixpunt"
                    + " hoort bij de oude relatie; DroolsBackend.vuur weigert de sessie, dus open een verse sessie met"
                    + " alle beans in één Invoer");
        List<Runnable> acts = new ArrayList<>();
        // a set: the fault of two writers that the rule of verschilregel stated and the one read off the values below
        // are one fault, equal as facts are
        Set<Fout> fouten = new LinkedHashSet<>();
        for (Object f : sessie.getObjects(o -> o instanceof Fout)) {
            Fout fout = (Fout) f;
            fouten.add(label != null && fout.fout() instanceof Schrijversconflict c
                    ? new Fout(fout.onderwerp(), fout.regel(), c.named(label))
                    : label != null && fout.fout() instanceof Invoervormfout i
                    ? new Fout(fout.onderwerp(), fout.regel(), i.named(label)) : fout);
        }
        List<Fixpunt.AfgeleidKenmerk> kenmerken = new ArrayList<>();
        for (Object f : sessie.getObjects(o -> o instanceof HasCharacteristic)) {
            HasCharacteristic h = (HasCharacteristic) f;
            Member member = memberOf(h, h.kenmerk());
            kenmerken.add(new Fixpunt.AfgeleidKenmerk(h.onderwerp(), member.naam()));
            BiConsumer<Object, Object> setter = Accessors.eigenschapSetter(member.javaClass(), Namen.property(member.naam()));
            acts.add(() -> setter.accept(h.onderwerp(), Boolean.TRUE));
        }
        Map<Object, Map<ElementSleutel, List<Value>>> perTarget = new IdentityHashMap<>();
        for (Object f : sessie.getObjects(o -> o instanceof Value)) {
            Value w = (Value) f;
            perTarget.computeIfAbsent(w.onderwerp(), o -> new LinkedHashMap<>())
                    .computeIfAbsent(w.attribuut(), a -> new ArrayList<>()).add(w);
        }
        List<Fixpunt.AfgeleideWaarde> waarden = new ArrayList<>();
        for (Map<ElementSleutel, List<Value>> perMember : perTarget.values())
            for (List<Value> written : perMember.values()) {
                // the writers as Schrijversconflict.inDocumentOrder ranks them, the rules in the order of the
                // document, and the values of one writer by their text: of values that agree up to their scale, 5,00
                // and 5, the bean gets that of the rule that comes first, on either branch and in every run, where the
                // working memory gives them in an order that the firings and the hash of the facts decide
                written.sort(Schrijversconflict.inDocumentOrder(documentOrder)
                        .thenComparing(w -> String.valueOf(w.waarde())));
                Value eerste = written.get(0);
                if (written.stream().anyMatch(w -> !equalValue(eerste.waarde(), w.waarde()))) {
                    Schrijversconflict c = conflict(eerste.onderwerp(), eerste.attribuut(), written);
                    fouten.add(new Fout(eerste.onderwerp(), c.writers().get(0).writer(),
                            label == null ? c : c.named(label)));
                    continue;
                }
                Member member = memberOf(eerste, eerste.attribuut());
                // a timeline goes onto the bean as the bean model's segments, which carry no value equality
                @SuppressWarnings("unchecked")
                Object v = member.time() == null ? eerste.waarde()
                        : Period.beans((List<Period>) eerste.waarde(), segmentClass());
                waarden.add(new Fixpunt.AfgeleideWaarde(eerste.onderwerp(), member.naam(), v));
                BiConsumer<Object, Object> setter = Accessors.eigenschapSetter(member.javaClass(),
                        Namen.property(member.naam()));
                acts.add(() -> setter.accept(eerste.onderwerp(), v));
            }
        List<Fixpunt.GemaaktObject> objecten = new ArrayList<>();
        for (Object f : sessie.getObjects(o -> o instanceof CreationStamp)) {
            CreationStamp g = (CreationStamp) f;
            objecten.add(new Fixpunt.GemaaktObject(g.object(), g.regel(), g.onderwerp()));
        }
        List<Fixpunt.GemaakteKoppeling> koppelingen = new ArrayList<>();
        // a link of the input carries phase 0 and is in the bean fields already
        for (Object f : sessie.getObjects(o -> o instanceof RelationLink k && k.phase() > 0))
            koppelingen.add(madeLink((RelationLink) f, acts));
        List<Fout> ordered = new ArrayList<>(fouten);
        ordered.sort(Comparator.comparing(Fout::regel).thenComparing(Fout::melding));
        List<Object> inconsistenties = new ArrayList<>(sessie.getObjects(o -> o.getClass().getName().equals(INCONSISTENCY)));
        return new Read(new Fixpunt(waarden, kenmerken, objecten, koppelingen, ordered, inconsistenties), acts);
    }

    /** The created link {@code k} as {@link Fixpunt} gives it, with into {@code acts} the write of each of its to-one
     *  ends that the bean model gives a reference field: the field on the filler of the other role, named after the
     *  to-one role, holds the filler of that role (§13.3.9), as {@link #linkRules} reads the input of the relation
     *  back from it. A to-many end has no field of its own; where both ends are to-many no bean holds the link, and it
     *  is in {@link Fixpunt#koppelingen} only. */
    private Fixpunt.GemaakteKoppeling madeLink(RelationLink k, List<Runnable> acts) {
        RegelSpraakModel.Feittype ft = feittype(k.relation());
        if (ft == null || ft.rollen.size() != 2)
            throw new IllegalStateException(k + " in de sessie, maar het model kent geen feittype van twee rollen '"
                    + k.relation() + "'");
        RegelSpraakModel.Rol r0 = ft.rollen.get(0), r1 = ft.rollen.get(1);
        Objecttype t0 = model.objecttypeOpNaam(r0.objecttypeNaam()), t1 = model.objecttypeOpNaam(r1.objecttypeNaam());
        if (!r1.meervoudig() && FactModelGenerator.hasReferenceField(model, t0, r1.naam(), t1)) {
            BiConsumer<Object, Object> setter = Accessors.eigenschapSetter(beanClass(t0), Namen.property(r1.naam()));
            acts.add(() -> setter.accept(k.eerste(), k.tweede()));
        }
        if (!r0.meervoudig() && FactModelGenerator.hasReferenceField(model, t1, r0.naam(), t0)) {
            BiConsumer<Object, Object> setter = Accessors.eigenschapSetter(beanClass(t1), Namen.property(r0.naam()));
            acts.add(() -> setter.accept(k.tweede(), k.eerste()));
        }
        return new Fixpunt.GemaakteKoppeling(ft.naam, r0.naam(), k.eerste(), r1.naam(), k.tweede());
    }

    /** The bean model's {@code Segment}, the class of a timeline's segments on a bean. */
    private Class<?> segmentClass() {
        try {
            return Class.forName(FactModelGenerator.PAKKET + ".Segment", true, beanLoader);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("geen beanklasse Segment voor een tijdlijn", e);
        }
    }

    private Member memberOf(Object fact, ElementSleutel sleutel) {
        Member member = derived.get(sleutel);
        if (member == null)
            throw new IllegalStateException(fact + " in de sessie, maar geen regel van dit model leidt "
                    + FactNames.member(sleutel) + " af");
        return member;
    }

    /** The fault of RUNTIME-SEMANTICS ch. 5.7 for the {@code waarden} that writers put on attribute {@code sleutel} of
     *  {@code onderwerp}: the writers as {@link Schrijversconflict#inDocumentOrder} ranks them over the document's
     *  rules {@link #documentOrder}, the attribute as declared, and the object's own type. */
    private Schrijversconflict conflict(Object onderwerp, ElementSleutel sleutel, List<Value> waarden) {
        return Schrijversconflict.van(onderwerp, memberOf(onderwerp, sleutel).naam(), waarden, documentOrder, typeClasses,
                typen);
    }

    /** Value equality at the write boundary: numbers by value, so that 5.0 and 5.00 are one value; leeg is null. */
    private static boolean equalValue(Object a, Object b) {
        return ComputeHelpers.equalValue(a, b);
    }

    // ---------------- the model ----------------

    private Modelplan run() {
        writers = ProducentConsument.producentIndex(model, defaults);
        standingTargets = StandingTargets.van(model, defaults);
        for (ResolvedRegel r : model.resolvedRegels)
            if (r.naam() != null && !documentOrder.contains(r.naam())) documentOrder.add(r.naam());
        for (Objecttype t : model.objecttypen.values()) {
            String javaClass = Namen.ident(t.naam, true);
            int i = typeClasses.indexOf(javaClass);
            if (i < 0) {
                typeClasses.add(javaClass);
                typen.add(t.naam);
            } else {
                typen.set(i, t.naam);
            }
        }
        if (!model.objectCreaties.isEmpty() || !model.feitCreaties.isEmpty())
            strata = Cycles.strata(model, defaults, navigationBindings, roleBindings, sourcePaths);
        // before any form lowers: a form that reads a created relation must know it while it lowers
        for (ResolvedObjectCreatie o : model.objectCreaties) {
            RolBindingRef vr = roleBindings.get(o);
            if (vr != null && vr.feittype() != null) createdRelations.add(Namen.norm(vr.feittype()));
            if (vr == null) {
                RegelSpraakModel.Feittype ft = typePairRelation(o);
                if (ft != null) createdRelations.add(Namen.norm(ft.naam));
            }
        }
        for (ResolvedFeitCreatie f : model.feitCreaties)
            if (f.feittype() != null) createdRelations.add(Namen.norm(f.feittype()));
        // computing rules and initialisations are counted apart: one of each on a target is a pair the branch carries
        // (the initialisation falls through below the rule), two of either kind is not
        for (ResolvedRegel r : model.resolvedRegels) {
            if (!(r.doel() instanceof Expr.AttribuutRef d) || d.objecttype() == null || d.attribuut() == null) continue;
            Map<ElementSleutel, List<ResolvedRegel>> per =
                    r.soort() == ResolvedRegel.Soort.GELIJKSTELLING || r.soort() == ResolvedRegel.Soort.BEREKENING
                            ? computeRules
                            : r.soort() == ResolvedRegel.Soort.INITIALISATIE ? initialisations : null;
            if (per != null)
                per.computeIfAbsent(ProducentConsument.elementVan(d.objecttype(), d.attribuut(), false),
                        k -> new ArrayList<>()).add(r);
        }
        for (PrioritizedDefaults p : defaults)
            if (p.doel() != null)
                defaultsTargets.add(ProducentConsument.elementVan(model.objecttypeOpNaam(p.onderwerp()), p.doel(),
                        p.isKenmerk()));
        // composed before the rules, since a rule the fold takes into a stack must not fire on the stack's type
        stapels = new Stapels(model, defaults);
        List<Stapels.Stack> effective;
        String composition = null;
        try {
            effective = stapels.stapels();
        } catch (Stapels.Uncomposable o) {
            effective = List.of();
            composition = o.getMessage();
        }
        List<RulePlan> rules = new ArrayList<>();
        for (ResolvedRegel r : model.resolvedRegels) rules.addAll(meet("regel", nameOf(r.naam()), () -> regel(r)));
        for (ResolvedConsistentie c : model.consistenties)
            rules.addAll(meet("consistentie", nameOf(c.naam()), () -> consistencyRules(c)));
        for (ResolvedUniciteit u : model.uniciteiten)
            rules.addAll(meet("uniciteit", nameOf(u.naam()), () -> uniquenessRules(u)));
        for (ResolvedVerdeling v : model.verdelingen)
            rules.addAll(meet("verdeling", nameOf(v.naam()), () -> allocation(v)));
        for (ResolvedObjectCreatie o : model.objectCreaties)
            rules.addAll(meet("objectcreatie", nameOf(o.naam()), () -> objectCreation(o)));
        for (ResolvedFeitCreatie f : model.feitCreaties)
            rules.addAll(meet("feitcreatie", nameOf(f.naam()), () -> factCreation(f)));
        for (ResolvedUniciteitUnie u : model.uniciteitUnies)
            rules.addAll(meet("uniciteit-unie", nameOf(u.naam()), () -> uniquenessUnionRules(u)));
        if (composition != null) refuse("defaults", "effectieve stapels", composition);
        for (Stapels.Stack st : effective) {
            gegenereerd.put(stackName(st), new RegelSpraakModel.GegenereerdUit(stackTarget(st),
                    RegelSpraakModel.Herkomstvorm.DEFAULTSSTAPEL));
            rules.addAll(meet("defaults", stackName(st), () -> stackRules(st)));
        }
        rules.addAll(differenceRules());
        // a StartPointDetermination is a declaration that fires nothing: it says which member anchors a grid with a
        // variable start (RUNTIME-SEMANTICS ch. 3.2), and the reference interpreter faults only where a bucket of such
        // a grid is laid. Its anchor member is an ordinary date attribute that ordinary rules write, so a document that
        // declares one and reads or writes no timeline on its grid plays; a form that does read or write one is
        // refused by name where it asks for the member (tijdlid), and the case reader refuses a case value on it
        // a day-kind definition is no form that fires: the forms that step or count in its day kind compile it where
        // they lower (dagsoortpredikaat), so a definition no form uses is never compiled and refuses nothing
        // the contributions of the folds the lowered forms read; a fold of a refused form left none here
        for (List<RulePlan> rs : contributionRules.values()) rules.addAll(rs);
        for (String relation : createdRelations) rules.addAll(linkRules(relation));
        // a standing is built once per member, for every rule that reads it, and only for one a lowered rule reads: a
        // standing nothing reads would only add firings
        List<RulePlan> states = new ArrayList<>();
        for (PathRead p : pathReads.values())
            states.addAll(p.pad().created() ? linkPathStateRules(p) : pathStateRules(p));
        for (Member member : read.values()) states.addAll(member.kenmerk() ? characteristicStateRules(member) : attributeStateRules(member));
        for (String producer : channels) states.addAll(channelStateRules(producer));
        for (String parameter : globals.keySet()) states.add(parameterStateRule(parameter));
        List<Modelplan.Global> declarations = new ArrayList<>();
        if (evaluationDateGlobal) {
            // the case runner sets the evaluation date under this name after the parameters, so a parameter of the same
            // Java name would read the date, or the date the parameter
            if (globals.containsKey(REKENDATUM))
                refuse("parameter", REKENDATUM, "een parameter met de Java-naam '" + REKENDATUM + "' naast een vorm die"
                        + " de rekendatum leest: de caserunner zet de rekendatum onder die naam, zodat de twee één global"
                        + " delen");
            else {
                declarations.add(new Modelplan.Global(REKENDATUM, LocalDate.class));
                states.add(evaluationDateStateRule());
            }
        }
        globals.forEach((naam, javaClass) -> declarations.add(new Modelplan.Global(naam, javaClass)));
        for (RulePlan r : rules) if (!formOfRule.containsKey(r.naam())) rulesWithoutForm.add(r.naam());
        for (RulePlan r : states) rulesWithoutForm.add(r.naam());
        List<RulePlan> plans = new ArrayList<>(rules);
        plans.addAll(states);
        return new Modelplan(declarations, plans);
    }

    /** A form's name as the manifest shows it; a nameless form (WF-107 refuses it) still gets an entry. */
    private static String nameOf(String naam) {
        return naam != null ? naam : "(zonder naam)";
    }

    /** Lowers one form and records its verdict: lowered, or a skip with the reason a {@link Refusal} carries. */
    private List<RulePlan> meet(String soort, String naam, Supplier<List<RulePlan>> bouw) {
        vorm = naam;
        String own = RegelSpraakModel.vormnaam(naam);
        if (!own.equals(naam))
            gegenereerd.put(naam, new RegelSpraakModel.GegenereerdUit(RegelSpraakModel.Benoembaar.vorm(own),
                    RegelSpraakModel.Herkomstvorm.VERSIE));
        try {
            List<RulePlan> rules = bouw.get();
            dekking.add(Dekking.lowered(soort, naam));
            for (RulePlan r : rules) formOfRule.put(r.naam(), naam);
            return rules;
        } catch (Refusal w) {
            refuse(soort, naam, w.getMessage());
            return List.of();
        }
    }

    private void refuse(String soort, String naam, String reden) {
        Dekking d = Dekking.skip(soort, naam, reden);
        dekking.add(d);
        d.report(model);
    }

    /** A form, or a part of one, this branch does not lower; the message is the manifest's reason. */
    private static final class Refusal extends RuntimeException {
        Refusal(String reden) { super(reden); }
    }

    // ---------------- a rule ----------------

    private List<RulePlan> regel(ResolvedRegel r) {
        // the rule of a form is named by the form, and a KieBase keeps one rule per name
        if (r.naam() == null) throw new Refusal("een regel zonder naam (WF-107)");
        if (!(r.doel() instanceof Expr.AttribuutRef doel) || doel.objecttype() == null || doel.attribuut() == null)
            throw new Refusal("een regel zonder opgelost doel");
        if (navigationBindings.containsKey(doel)) throw new Refusal("een regel op een genavigeerd doel");
        // a version with a bounded validity gates on the evaluation date: its rules bind the date where the window
        // admits it (Reads.valid), and beside them a rule faults the session that carries no date
        List<RulePlan> out = new ArrayList<>(switch (r.soort()) {
            case KENMERKTOEKENNING -> characteristicRule(r, doel);
            case GELIJKSTELLING, BEREKENING -> computeRule(r, doel);
            case INITIALISATIE -> initialisationRule(r, doel);
        });
        RulePlan withoutDate = validityFault(model.geëmitteerdeRegelNaam(r.naam()), r.naam(), r.geldigheid());
        if (withoutDate != null) out.add(withoutDate);
        return out;
    }

    /**
     * The rule beside a form with a bounded validity, {@code vorm}, whose Drools rule is {@code ruleName}, that faults
     * a session without an evaluation date: such a form needs the date (RUNTIME-SEMANTICS ch. 8.1), and the reference
     * interpreter faults the session as soon as it evaluates the form, whether or not the form has a subject, in the
     * sentence {@link Rekenfout#VALIDITY_WITHOUT_DATE}. The fault is on the session's {@link EvaluationDate}, the one fact
     * the date is, and like every evaluation fault it counts where it stands at the fixpoint. Null for a form that is
     * always valid, which needs no date.
     */
    private RulePlan validityFault(String ruleName, String vorm, Geldigheid g) {
        if (g == null || g.altijd()) return null;
        evaluationDateNeeded();
        Alpha withoutDate = new Alpha(id("geen-rekendatum"),
                List.of(new Alpha.FieldTest("date", Alpha.Comparator.LEEG, null)));
        return new RulePlan(ruleName + ": geen rekendatum", this.vorm, null, new ReadTable(List.of()),
                List.of(new FactPattern(EvaluationDate.class, "$rekendatum", List.of(withoutDate), List.of(), List.of())),
                new Consequence.Fout("$rekendatum", Rekenfout.VALIDITY_WITHOUT_DATE, new Consequence.FaultName(vorm, ruleName),
                        Consequence.Insertion.LOGICAL));
    }

    /** Declares the global of the evaluation date, the first time a lowered form needs the date. */
    private void evaluationDateNeeded() {
        evaluationDateGlobal = true;
    }

    /** The rule that puts the session's {@link EvaluationDate} in working memory, from the global the case runner sets
     *  where the case carries an {@code asOfDate} and leaves unset, null, where it carries none. Like a parameter's
     *  standing ({@link #parameterStateRule}) it matches no fact and fires once per session; the global does not
     *  change while the session fires. */
    private RulePlan evaluationDateStateRule() {
        return new RulePlan("rekendatumstand", null, null, new ReadTable(List.of()), List.of(),
                new Consequence.EvaluationDateState(REKENDATUM, Consequence.Insertion.LOGICAL));
    }

    /**
     * A characteristic attribution: the subject pattern with the reads its condition binds there, the standing patterns
     * of what it reads elsewhere, one test over every value its condition reads, and a consequence that inserts the
     * {@link HasCharacteristic} logically. Its justification is the whole match, so a standing the condition read that is
     * later retracted takes the characteristic with it, also when the condition read it negated. Beside it, where the
     * condition can fault, the rule that inserts its fault ({@link #conditionFaultRule}).
     */
    private List<RulePlan> characteristicRule(ResolvedRegel r, Expr.AttribuutRef doel) {
        // the loader refuses any other value (WF-022); asked again because this rule writes true whatever it says
        if (!(r.waarde() instanceof Expr.Literal l && "waar".equals(l.waarde()) && l.type() instanceof Datatype.Booleaan))
            throw new Refusal("een kenmerktoekenning met een andere waarde dan de constante waar (WF-022)");
        Member time = declared(doel.objecttype(), doel.attribuut(), true);
        if (time.time() != null)
            throw new Refusal("het tijdsafhankelijke kenmerk '" + time.naam() + "' van " + time.declarant().naam
                    + " als doel: " + STILL_NOT + "; geen golden met cases schrijft er een (RUNTIME-SEMANTICS ch. 3.11)");
        Member member = member(doel.objecttype(), doel.attribuut(), true);
        Supplier<Reads> fresh = () -> new Reads(doel.objecttype()).valid(r.geldigheid());
        Reads lez = fresh.get();
        Set<Integer> read = lez.follow();
        Term testTerm = r.voorwaarde() == null ? null : voorwaarde(r.voorwaarde(), lez);
        boolean canFail = lez.canFail();
        lez.stop();
        List<LhsItem> items = lez.plan(conditionRole(canFail), testTerm, read, r.voorwaarde());
        ElementSleutel sleutel = member.sleutel();
        derived.put(sleutel, member);
        lez.recorder();
        String ruleName = model.geëmitteerdeRegelNaam(r.naam());
        List<RulePlan> out = new ArrayList<>();
        out.add(lez.rulePlan(ruleName, items,
                new Consequence.Characteristic(SUBJECT, sleutel, Consequence.Insertion.LOGICAL)));
        if (canFail) out.add(conditionFaultRule(r.naam(), ruleName, fresh, r.voorwaarde(), null));
        RulePlan deposit = depositRule(r.naam(), ruleName, fresh, r.voorwaarde(), null);
        if (deposit != null) out.add(deposit);
        return out;
    }

    /**
     * An assignment or a computation of an attribute of the subject: the left-hand side of a characteristic attribution,
     * and a consequence that computes the value from the values it binds and inserts a {@link Value} logically. A
     * computed leeg writes nothing on a standing target, so the input on the target stands, or the initialisation beside
     * the rule fills it (the standing norm of RUNTIME-SEMANTICS ch. 7.8 and the desugaring domain of ch. 7.7: the rule is
     * its target's only computing writer, beside at most one unconditional initialisation). Outside that domain a
     * computed leeg is a write (ch. 1.2g): the {@link Value} carries leeg, which takes the target's input standing away
     * and is a value another writer can disagree with. A target may have more computing rules: each is an equation, and
     * where two of them stand at the fixpoint on one object with different values the rule of {@link #differenceRule}
     * makes that the fault of ch. 5.7; where at most one condition holds, or they write the same value, the document
     * plays. They are keyed per object, so writers on sister subtypes never meet (WF-079 and ch. 7.10 read per concrete
     * type). An evaluation fault, a division by a numeric zero or a {@link Rekenfout}, is inserted logically by the
     * consequence as a {@link Fout} in place of the value, named after the rule. A division of a filled dividend by a
     * leeg divisor, and a square root of a negative number, insert the §9.5 inconsistency of RUNTIME-SEMANTICS ch. 1.2
     * from the same consequence, under the rule's name, where the evaluation reaches them: the probe the consequence puts
     * in its value array hears exactly the places that run ({@link DepositFlag}), so a place in a branch of an
     * {@code IfThenElse} that is not taken, or in the value of a {@code Let} binder no read forces, deposits nothing
     * (ch. 10.2, 10.6). Every division and every root is such a place, as in the reference interpreter. The condition of
     * the rule holds there by construction; a deposit in the condition itself is the {@link #depositRule}'s.
     *
     * <p>That deposit is a logical fact like the value: it stands while the activation that made it stands, and goes when
     * a read it rested on changes. The reference interpreter keeps a deposit made on a transient value (RUNTIME-SEMANTICS
     * ch. 6.1, "Recorded residuals"), this branch withdraws it; recorded as an observation.
     *
     * <p>A member an object creation sets is the input of the object it makes ({@link #objectCreation}), so a rule on it
     * displaces that value as it displaces case input, and outside the standing domain a computed leeg erases it, as in
     * the reference interpreter, which overwrites the value the creation wrote at the making (ch. 5.5, 1.2g).
     */
    private List<RulePlan> computeRule(ResolvedRegel r, Expr.AttribuutRef doel) {
        if (declared(doel.objecttype(), doel.attribuut(), false).time() != null) return timelineRule(r, doel);
        Member member = member(doel.objecttype(), doel.attribuut(), false);
        Set<Class<?>> absorbed = absorbed(doel, member);
        boolean writesEmpty = !standingTargets.standingScalar(r);
        return writeRule(r, doel.objecttype(), member, null, absorbed, writesEmpty);
    }

    /**
     * A computation of an attribute on a timeline of the subject (RUNTIME-SEMANTICS ch. 3.11): the rule writes a whole
     * timeline at once, which a pro-rata part (ch. 3.7) or a day count (ch. 3.8) over the subject's own characteristic
     * produces ({@link #timelineValue}), and inserts it as the {@link Value} of the target, an immutable
     * {@code List<Period>}. On a standing temporal target ({@link StandingTargets#standingTemporal}: the target's one
     * version-free writer, ch. 7.8) an empty result is the temporal leeg and writes nothing, so the input timeline of the
     * target stands whole, and a result with a segment replaces it whole; elsewhere the rule writes the empty timeline
     * as well (ch. 1.2g). The versions of one rule are one writer; a version that applies writes as such a rule does,
     * and two that stand with different timelines are the fault of ch. 5.7 ({@link #differenceRule}).
     *
     * <p>Refused by name, each with no golden that has cases: any other writer of the target, a rule, an
     * initialisation, a defaults group or an allocation, since ch. 7.8 keeps a target with more writers fenced and
     * the reference interpreter answers it in pass order; and any other value, among them a sum of timelines over a
     * set (ch. 3.5) and the arithmetic of two timelines (ch. 3.4), which no golden of this branch's gap asks. Refused
     * by name as well: a time window in the condition, which on a temporal target clips the timeline instead of
     * gating the rule on the evaluation date (ch. 3.10), as the destructive DRL-text lowering refused it as well until
     * card drl-teksttak-op-truth-maintenance; golden {@code jutterswet-pachtvenster} has it.
     */
    private List<RulePlan> timelineRule(ResolvedRegel r, Expr.AttribuutRef doel) {
        Member member = timeMember(doel.objecttype(), doel.attribuut(), false, "een tijdsafhankelijk doel");
        List<String> others = otherWriters(r, doel.objecttype(), member.sleutel());
        if (!others.isEmpty())
            throw new Refusal("een tijdsafhankelijk doel met een tweede schrijver ('" + member.naam() + "' van "
                    + member.declarant().naam + " wordt ook geschreven door "
                    + String.join(", ", others.stream().map(x -> "\"" + x + "\"").toList()) + "): " + STILL_NOT
                    + "; een doel met meer schrijvers blijft omheind (RUNTIME-SEMANTICS ch. 7.8), en geen golden met"
                    + " cases heeft er een");
        if (Conditie.bevatPeriode(r.voorwaarde()))
            throw new Refusal("een periodevenster in de voorwaarde van een regel met het tijdsafhankelijke doel '"
                    + member.naam() + "' van " + member.declarant().naam + ": daar knipt het venster de tijdlijn"
                    + " (RUNTIME-SEMANTICS ch. 3.10), en " + STILL_NOT);
        if (!(r.waarde() instanceof Expr.Functie f && (f.kind() == Expr.FunctieKind.TIJDSEVENREDIG_DEEL
                || f.kind() == Expr.FunctieKind.AANTAL_DAGEN_IN)))
            throw new Refusal("een waarde van het tijdsafhankelijke doel '" + member.naam() + "' van "
                    + member.declarant().naam + " die geen tijdsevenredig deel en geen dagentelling is ('"
                    + (r.waarde() == null ? "zonder waarde" : snippet(r.waarde().bron())) + "'): " + STILL_NOT
                    + " (RUNTIME-SEMANTICS ch. 3.4, 3.5, 3.11); geen golden met cases schrijft er een");
        return writeRule(r, doel.objecttype(), member, null, Set.of(), !standingTargets.standingTemporal(doel));
    }

    /**
     * The timeline that the pro-rata part or the day count {@code f} produces for the target {@code doel} of
     * {@code subject} (RUNTIME-SEMANTICS ch. 3.7, 3.8), as a function of the values the rule binds: the true periods of
     * the subject's own characteristic in its gedurende clause and, for the pro-rata part, the timeline of the base, an
     * own attribute, each bound in the left-hand side ({@link Reads#timeline}), so a base that a rule derives later
     * takes the result along. The runtime's helpers compute them, with {@link Period} as their
     * segment class: {@code rsTijdsevenredig} per bucket of the target's grid, the base's
     * value at the bucket's start times the share of true days, a base that is leeg there or absent giving no segment,
     * and {@code rsAantalDagenIn} the true days per touched bucket, on the calendar or from the anchor of the target's
     * fixed start point ({@code rsAantalDagenInVanaf}, ch. 3.2). Neither coalesces adjacent equal buckets (ch. 3.7). The
     * pro-rata part reads its base whole, bucket by bucket, so a base the case gives as the asOfDate-bucket scalar
     * faults ({@link Invoervormfout}, ch. 8.4), wherever the rule's evaluation reaches the value; the reference
     * interpreter and the ASP backend read such a base as no timeline today and write nothing.
     *
     * <p>Refused by name, as the reference interpreter refuses them: a stated bucket other than the target's
     * granularity (WF-056, WF-058); a clause that is not a check of an own characteristic on a timeline, or is negated
     * or navigated; a base that is not an own attribute on a timeline, is finer than the bucket or carries a unit, whose
     * rate the reference interpreter does not restate (ch. 3.7, fenced); and a pro-rata part onto or from a fixed start
     * point ({@link Timelines#proRataFixedGrid}).
     */
    private Term timelineValue(Expr.Functie f, Member doel, Objecttype subject, Reads lez) {
        boolean part = f.kind() == Expr.FunctieKind.TIJDSEVENREDIG_DEEL;
        String description = (part ? "een tijdsevenredig deel" : "een dagentelling") + " ('" + snippet(f.bron()) + "')";
        if (f.granulariteit() != doel.time())
            throw new Refusal(description + " per " + f.granulariteit() + " naar het doel '" + doel.naam() + "' met de"
                    + " granulariteit " + doel.time() + ": de emmer is die van het doel (" + (part ? "WF-056" : "WF-058")
                    + ")");
        if (doel.time() != RegelSpraakModel.Granulariteit.MAAND && doel.time() != RegelSpraakModel.Granulariteit.JAAR)
            throw new Refusal(description + " per " + doel.time() + ": alleen een maand en een jaar zijn emmers"
                    + " (RUNTIME-SEMANTICS ch. 3.2)");
        if (f.leegwaarde() != null || f.dimensieSelectie() != null)
            throw new Refusal(description + " met een terugvalwaarde of een dimensieselectie: " + STILL_NOT);
        int condition = clipTimeline(f.filter(), subject, lez, description);
        String gran = doel.time().name();
        if (!part) {
            if (!f.argumenten().isEmpty())
                throw new Refusal(description + " met " + f.argumenten().size() + " argumenten: een dagentelling neemt er"
                        + " geen (WF-058)");
            // RUNTIME-SEMANTICS ch. 3.2: a target on a fixed start point buckets from the constant anchor its walls are
            // computed from
            LocalDate anchor = model.vastStartpuntAnker(doel.declarant(), doel.naam());
            Term periods = new Term.SlotValue(condition, bron(f.filter().bron()));
            Term days = anchor == null
                    ? new Term.Call(StaticFunction.RS_DAY_COUNT, List.of(periods, constant(gran),
                            constant(Period.class)), null)
                    : new Term.Call(StaticFunction.RS_DAY_COUNT_FROM, List.of(periods, constant(gran),
                            constant(anchor), constant(Period.class)), null);
            return new Term.Call(StaticFunction.PERIOD_LIST, List.of(days), bron(f.bron()));
        }
        if (f.argumenten().size() != 1 || !(f.argumenten().get(0) instanceof Expr.AttribuutRef b)
                || b.attribuut() == null || b.objecttype() != subject || navigationBindings.containsKey(b))
            throw new Refusal(description + " over een basis die geen eigen tijdsafhankelijk attribuut is: " + STILL_NOT
                    + "; de referentie-interpreter neemt alleen een eigen attribuut als basis (RUNTIME-SEMANTICS ch. 3.7)");
        Member base = timeMember(subject, b.attribuut(), false, description);
        if (!base.datatype().isNumeriek())
            throw new Refusal(description + " over de basis '" + base.naam() + "', die geen getal is (WF-091)");
        if (base.datatype().eenheidOf() != null)
            throw new Refusal(description + " over de basis '" + base.naam() + "' met de eenheid "
                    + base.datatype().eenheidOf().weergave() + ": de herrekening van een tarief is omheind in de"
                    + " referentie-interpreter en deze tak (RUNTIME-SEMANTICS ch. 3.7); geen golden");
        if (base.time().ordinal() < doel.time().ordinal())
            throw new Refusal(description + " over de basis '" + base.naam() + "' met de granulariteit " + base.time()
                    + ", fijner dan de emmer " + doel.time() + ": de basis staat niet vast over een emmer (RUNTIME-SEMANTICS"
                    + " ch. 3.7)");
        String fixed = Timelines.proRataFixedGrid(subject, doel.naam(), f);
        if (fixed != null) throw new Refusal(description + ": " + fixed);
        int baseValue = lez.timeline(base);
        int onderwerp = lez.member();
        lez.markFail();
        String baseName = base.naam();
        Term proportional = new Term.Call(StaticFunction.RS_TIME_PROPORTIONAL, List.of(
                new Term.SlotValue(condition, bron(f.filter().bron())),
                complete(baseValue, "the pro-rata base \"" + baseName + "\" needs the full timeline; ", onderwerp,
                        " supplies only the asOfDate-bucket scalar (ch. 3.7, ch. 8.4)", bron(b.bron())),
                constant(gran), constant(Period.class)), null);
        return new Term.Call(StaticFunction.PERIOD_LIST, List.of(proportional), bron(f.bron()));
    }

    /**
     * The true periods of the gedurende clause {@code c} of a pro-rata part or a day count on {@code subject}, bound as a
     * timeline ({@link Reads#timeline}): a check of the subject's own characteristic on a timeline, as the reference
     * interpreter reads that clause (RUNTIME-SEMANTICS ch. 3.7, 3.8). Refused by name: no clause, another condition, a
     * negated or navigated check, and an owner or a whole-period qualifier on the check (WF-097).
     */
    private int clipTimeline(Conditie c, Objecttype subject, Reads lez, String description) {
        if (!(c instanceof Conditie.Kenmerk k))
            throw new Refusal(description + " met een voorwaarde die geen kenmerk van het onderwerp is"
                    + (c == null ? " (geen voorwaarde)" : " ('" + snippet(c) + "')") + ": " + STILL_NOT
                    + "; de referentie-interpreter neemt alleen een eigen tijdsafhankelijk kenmerk (RUNTIME-SEMANTICS"
                    + " ch. 3.7, 3.8)");
        if (k.eigenaar() != null || k.periode() != null)
            throw new Refusal(description + " met een kenmerk met een eigenaar of een geheleperiodetoets in de knip ('"
                    + snippet(k) + "', WF-097)");
        if (k.negatie() || k.navigatie() != null || navigationBindings.containsKey(k))
            throw new Refusal(description + " met een ontkend of genavigeerd kenmerk ('" + snippet(k) + "'): " + STILL_NOT
                    + "; de referentie-interpreter neemt alleen een eigen tijdsafhankelijk kenmerk, niet ontkend"
                    + " (RUNTIME-SEMANTICS ch. 3.7, 3.8)");
        return lez.timeline(timeMember(subject, k.kenmerk(), true, description));
    }

    /**
     * The forms other than the initialisation {@code r} on {@code type} that write the attribute {@code sleutel} on an
     * instance {@code r} writes too, by the names the producer index gives them, in its order: a rule, an allocation or
     * a defaults group on {@code type}, an ancestor or a descendant of it ({@link #related}), read per concrete type as
     * WF-079 reads the single-writer rule; forms on sister subtypes never write one instance. An object creation is no
     * writer here: the member values it sets are the input of the object it makes ({@link #objectCreation}), which the
     * initialisation finds filled there.
     */
    private List<String> otherWriters(ResolvedRegel r, Objecttype type, ElementSleutel sleutel) {
        Set<String> out = new LinkedHashSet<>();
        for (ResolvedRegel x : model.resolvedRegels)
            if (x != r && x.soort() != ResolvedRegel.Soort.KENMERKTOEKENNING && x.doel() instanceof Expr.AttribuutRef d
                    && d.objecttype() != null && d.attribuut() != null && related(d.objecttype(), type)
                    && ProducentConsument.elementVan(d.objecttype(), d.attribuut(), false).equals(sleutel))
                out.add(ProducentConsument.basisNaam(x.naam()));
        for (ResolvedVerdeling v : model.verdelingen)
            if (v.doelType() != null && v.doelAttribuut() != null && related(v.doelType(), type)
                    && ProducentConsument.elementVan(v.doelType(), v.doelAttribuut(), false).equals(sleutel))
                out.add(ProducentConsument.basisNaam(v.naam()));
        for (int i = 0; i < defaults.size(); i++) {
            PrioritizedDefaults p = defaults.get(i);
            Objecttype t = model.objecttypeOpNaam(p.onderwerp());
            if (t != null && p.doel() != null && !p.isKenmerk() && related(t, type)
                    && ProducentConsument.elementVan(t, p.doel(), false).equals(sleutel))
                out.add(p.naam() != null ? ProducentConsument.basisNaam(p.naam()) : "/rules/prioritizedDefaults/" + i);
        }
        out.remove(ProducentConsument.basisNaam(r.naam()));
        return List.copyOf(out);
    }

    /** Whether one of the two types is the other or an ancestor of it: a form on the one then writes instances of the
     *  other, where two forms on sister types never meet on one instance (IR-SPEC ch. 2.2, WF-079). */
    private static boolean related(Objecttype a, Objecttype b) {
        return a.keten().contains(b) || b.keten().contains(a);
    }

    /**
     * The bean classes of the concrete types on which the fold of RUNTIME-SEMANTICS ch. 7.10 took the rule writing
     * {@code doel} into an effective stack, as its base or as its initialisation default: there the stack writes the
     * target and the rule must not also fire, so its subject pattern leaves them out. Empty for a target no defaults
     * group writes. A rule beside a group its own type declares on the same target is no fold but two writers of one
     * target (WF-050), which this branch refuses by name: the fold leaves such a rule alone, and both would write.
     */
    private Set<Class<?>> absorbed(Expr.AttribuutRef doel, Member member) {
        if (!defaultsTargets.contains(member.sleutel())) return Set.of();
        if (stapels.ownGroup(doel.objecttype(), doel.attribuut()))
            throw new Refusal("een regel naast een defaultsgroep van hetzelfde type op hetzelfde doel ('" + member.naam()
                    + "' van " + doel.objecttype().naam + "): een doel heeft één schrijver, de groep of de regel (WF-050)");
        Set<Class<?>> out = new LinkedHashSet<>();
        for (Objecttype t : stapels.absorbedOn(doel.objecttype(), doel.attribuut())) out.add(beanClass(t));
        return out;
    }

    /**
     * An initialisation (RUNTIME-SEMANTICS ch. 5.4): the left-hand side and the consequence of a computing rule, under a
     * guard that reads the input and the other writers' values, never its own write. The target must be empty on the
     * bean, which holds the case input and is not written while the session fires, and no {@link Value} of another
     * writer may stand on it, so a computed value displaces the initialisation whichever fires first, and a
     * computation that is leeg writes nothing and lets the initialisation fill the target (ch. 7.3). Its own
     * {@link Value} carries its name as the writer and does not match the guard: the value is an equation like any
     * attribute write, withdrawn and computed again when a value it reads changes. A guard that counted the own write
     * blocked the initialisation the moment it wrote and truth maintenance withdrew the value: on one object the target
     * ended leeg without a message (A5 of the trial of card drools-logische-afleiding, and the first case of
     * {@code jutterswet-initialisatievolgorde} on this branch), on several the session reached the fire cap (measured
     * 2026-10-08, {@code TmsInitialisationWitnessTest}).
     *
     * <p>A computing rule beside it that is outside the standing domain writes a computed leeg (ch. 1.2g). The guard
     * then reads the target as the other writers leave it, as the reference interpreter's guard reads the running fact
     * base: a value another writer places switches it off, and a leeg another writer writes leaves the target empty,
     * input or not, so the initialisation fills it and the two writers demand different values, the fault of ch. 5.7
     * ({@link #differenceRule}).
     *
     * <p>Two initialisations on one target are ill-formed, conditional or not (IR-SPEC WF-079): where both apply,
     * nothing ranks them. The backend runs no verifier, so this refuses the pair by name, read per concrete type as
     * WF-079 reads it: two initialisations on one type, or on a type and its ancestor, write one instance, and two on
     * sister subtypes never do (ch. 7.10).
     *
     * <p>Beside another writer of its target on an instance it writes ({@link #otherWriters}: per concrete type, so
     * not a writer on a sister subtype) an initialisation is the lowest default (ch. 7.7), and only the default
     * that decides is evaluated (ch. 7.1). This rule is evaluated as soon as what it reads stands, also where the other
     * writer displaces it later. A division by zero there is a fault on an intermediate state: this branch inserts it as
     * a {@link Fout}, which goes when the other writer displaces the initialisation, and answers the other writer's
     * value, where the reference interpreter faults or answers the value depending on the order of the rules. Whether
     * such a fault counts waits on a decision of the norm, so a division in the condition or the value of an
     * initialisation beside another writer stays refused, with the predicate of a division in a default
     * ({@code TmsInitialisationWitnessTest}). Without another writer nothing displaces it and the division stays carried.
     */
    private List<RulePlan> initialisationRule(ResolvedRegel r, Expr.AttribuutRef doel) {
        Member time = declared(doel.objecttype(), doel.attribuut(), false);
        if (time.time() != null)
            throw new Refusal("een initialisatie van het tijdsafhankelijke attribuut '" + time.naam() + "' van "
                    + time.declarant().naam + ": " + STILL_NOT + "; geen golden met cases heeft er een");
        Member member = member(doel.objecttype(), doel.attribuut(), false);
        ElementSleutel sleutel = member.sleutel();
        Set<Class<?>> absorbed = absorbed(doel, member);
        List<ResolvedRegel> writing = initialisations.get(sleutel).stream()
                .filter(x -> related(((Expr.AttribuutRef) x.doel()).objecttype(), doel.objecttype())).toList();
        if (writing.size() > 1)
            throw new Refusal("twee of meer initialisaties op één doel ('" + member.naam() + "' van " + member.declarant().naam
                    + ": " + String.join(", ", writing.stream().map(x -> "\"" + x.naam() + "\"").toList())
                    + "), voorwaardelijk of niet: waar ze allebei gelden rangschikt niets ze en bepaalt het fixpunt de"
                    + " waarde niet (RUNTIME-SEMANTICS ch. 5.4, WF-079)");
        // a computing rule outside the standing domain writes a computed leeg, which empties the target for the guard
        boolean emptyBeside = computeRules.getOrDefault(sleutel, List.of()).stream().anyMatch(x ->
                !standingTargets.standingScalar(x) && related(((Expr.AttribuutRef) x.doel()).objecttype(), doel.objecttype()));
        List<String> others = otherWriters(r, doel.objecttype(), sleutel);
        String division = division(r.voorwaarde(), r.waarde());
        if (division != null && !others.isEmpty())
            throw new Refusal("een deling in een initialisatie waarvan het doel nog een schrijver heeft ('" + division
                    + "'; '" + member.naam() + "' van " + member.declarant().naam + " wordt ook geschreven door "
                    + String.join(", ", others.stream().map(x -> "\"" + x + "\"").toList()) + "): naast een andere"
                    + " schrijver is een initialisatie de laagste default (RUNTIME-SEMANTICS ch. 5.4, 7.7), en deze tak"
                    + " rekent haar uit zodra wat zij leest staat, ook waar de andere schrijver haar later verdringt; een"
                    + " deling door nul daar is een fout op een tussenstand, die deze tak intrekt zodra de andere"
                    + " schrijver beslist, waar de referentie afhankelijk van de volgorde van de regels een fout of de"
                    + " waarde geeft: of zo'n fout telt, wacht op een normbesluit (ch. 5.2, 7.1)");
        String naam = r.naam();
        Alpha.FieldTest ofTarget = new Alpha.FieldTest("attribuut", Alpha.Comparator.GELIJK, sleutel);
        Alpha.FieldTest ofOther = new Alpha.FieldTest("writer", Alpha.Comparator.NOT_EQUAL, naam);
        if (!emptyBeside)
            return writeRule(r, doel.objecttype(), member, lez -> {
                lez.addSubjectAlpha(new Alpha(id("doel-leeg-in-de-invoer"),
                        List.of(new Alpha.FieldTest(member.naam(), Alpha.Comparator.LEEG, null))));
                Alpha another = new Alpha(id("andere-schrijver"), List.of(ofTarget, ofOther));
                return List.of(new Not(new FactPattern(Value.class, "$andere_schrijver", List.of(another),
                        List.of(new Join(id("andere-schrijver-onderwerp"), "onderwerp", SUBJECT)), List.of())));
            }, absorbed, false);
        return writeRule(r, doel.objecttype(), member, lez -> {
            Alpha another = new Alpha(id("andere-schrijver"),
                    List.of(ofTarget, ofOther, new Alpha.FieldTest("waarde", Alpha.Comparator.GEVULD, null)));
            Join otherOnSubject = new Join(id("andere-schrijver-onderwerp"), "onderwerp", SUBJECT);
            Alpha empty = new Alpha(id("lege-schrijver"),
                    List.of(ofTarget, ofOther, new Alpha.FieldTest("waarde", Alpha.Comparator.LEEG, null)));
            Join emptyOnSubject = new Join(id("lege-schrijver-onderwerp"), "onderwerp", SUBJECT);
            int emptyWriterCount = lez.emptyWriters();
            int onderwerp = lez.member();
            // the target's input on the subject's bean, as it is, which the guard tests for leeg as the first variant's
            // constraint does; read in the test, so the executable model binds no value of it
            Term invoer = new Term.Invoer(new Term.SlotValue(onderwerp, null), new Binding.Field(member.javaClass(), member.naam()),
                    Binding.ReadForm.VALUE, null);
            Term targetEmpty = new Term.Call(StaticFunction.TARGET_EMPTY, List.of(new Term.SlotValue(emptyWriterCount, null), invoer), null);
            return List.of(
                    new Not(new FactPattern(Value.class, "$andere_schrijver", List.of(another), List.of(otherOnSubject),
                            List.of())),
                    new Accumulation(new FactPattern(Value.class, "$lege_schrijver", List.of(empty), List.of(emptyOnSubject),
                            List.of()), List.of(new Accumulation.Aggregation(Accumulation.Kind.COUNT, null, "$lege_schrijvers"))),
                    new TestItem(id("doel-leeg"), TestItem.Role.VORM, targetEmpty, -1, List.of(emptyWriterCount, onderwerp),
                            lez.size()));
        }, absorbed, false);
    }

    /**
     * The rule of a form that writes an attribute of its subject: the subject pattern with the reads its condition and
     * its value bind there, the standing patterns of what it reads elsewhere, one test over every value the condition
     * reads, the {@code blockade} the form adds (null for none), and a consequence that computes the value from the
     * values it binds and inserts a {@link Value} logically, with the form's name as the writer, when the value is
     * gevuld, or also when it is leeg where {@code writesEmpty} (a computing rule outside the standing domain, ch.
     * 1.2g), beside the §6.5 deposit {@link #computeRule} describes. Where the value faults, the consequence inserts the
     * {@link Fout} instead, and neither the value nor a deposit. Objects of a class in {@code absorbed} do not
     * match: an effective stack writes the target there ({@link #absorbed}). Beside it, where the condition can
     * fault, the rule that inserts its fault ({@link #conditionFaultRule}).
     */
    private List<RulePlan> writeRule(ResolvedRegel r, Objecttype subject, Member member,
                                         Function<Reads, List<LhsItem>> blockade, Set<Class<?>> absorbed,
                                         boolean writesEmpty) {
        ElementSleutel sleutel = member.sleutel();
        Supplier<Reads> fresh = () -> lezingen(subject, absorbed).valid(r.geldigheid());
        Reads lez = fresh.get();
        Set<Integer> read = lez.follow();
        Term testTerm = r.voorwaarde() == null ? null : voorwaarde(r.voorwaarde(), lez);
        boolean canFail = lez.canFail();
        lez.stop();
        Set<Integer> use = lez.follow();
        // the write boundary: the value restated into the target's declared unit (RUNTIME-SEMANTICS ch. 2.4, 2.6); a
        // timeline is written as its producer computes it, as in the reference interpreter (ch. 3.7)
        Term waarde = member.time() != null
                ? timelineValue((Expr.Functie) r.waarde(), member, subject, lez)
                : restated(waarde(r.waarde(), lez), eff.unitOf(r.waarde()), member.datatype().eenheidOf(),
                        "een §4 schrijfgrens naar het doel");
        lez.stop();
        int depositFlag = lez.depositFlagSlot();
        List<LhsItem> items = lez.plan(conditionRole(canFail), testTerm, read, r.voorwaarde());
        if (blockade != null) items.addAll(blockade.apply(lez));
        String naam = r.naam();
        String ruleName = model.geëmitteerdeRegelNaam(naam);
        Consequence consequence = new Consequence.Value(SUBJECT, sleutel, naam, waarde, List.copyOf(use), lez.size(), true,
                writesEmpty, depositFlag < 0 ? null : new Consequence.Deposit(depositFlag, naam), new Consequence.FaultName(naam, ruleName),
                Consequence.Insertion.LOGICAL);
        derived.put(sleutel, member);
        lez.recorder();
        List<RulePlan> out = new ArrayList<>();
        out.add(lez.rulePlan(ruleName, items, consequence));
        if (canFail) out.add(conditionFaultRule(naam, ruleName, fresh, r.voorwaarde(), blockade));
        RulePlan deposit = depositRule(naam, ruleName, fresh, r.voorwaarde(), blockade);
        if (deposit != null) out.add(deposit);
        return out;
    }

    /** The reads of a rule on {@code subject}, whose subject pattern leaves out the objects of a class in
     *  {@code absorbed}: an effective stack writes the target there ({@link #absorbed}). The plan carries the
     *  classes in the order of the document's types, as {@code absorbed} lists them: an immutable copy by
     *  {@code Set.copyOf} iterates in an order that differs per run, and a rendering into text must be the same in
     *  every run. */
    private Reads lezingen(Objecttype subject, Set<Class<?>> absorbed) {
        Reads lez = new Reads(subject);
        if (!absorbed.isEmpty())
            lez.addSubjectAlpha(new Alpha(id("niet-in-een-stapel"), List.of(new Alpha.FieldTest(null,
                    Alpha.Comparator.CLASS_NOT_IN, Collections.unmodifiableSet(new LinkedHashSet<>(absorbed))))));
        return lez;
    }

    // ---------------- two writers of one target ----------------

    /**
     * The rules of {@link #differenceRule}, one per attribute on which two writers can stand at the fixpoint on one
     * object: two or more computing rules, or a computing rule outside the standing domain beside an initialisation,
     * whose guard a written leeg leaves open (ch. 5.4). An initialisation beside a standing rule needs none: its guard
     * keeps it from standing beside the rule's value, and the rule writes nothing for a computed leeg.
     */
    private List<RulePlan> differenceRules() {
        List<RulePlan> out = new ArrayList<>();
        for (Map.Entry<ElementSleutel, List<ResolvedRegel>> e : computeRules.entrySet()) {
            Member member = derived.get(e.getKey());
            // no writer of the attribute was lowered: the manifest names the refused forms, and the build fails on them
            if (member == null) continue;
            boolean emptyBesideInitialisation = e.getValue().stream().anyMatch(r -> !standingTargets.standingScalar(r))
                    && !initialisations.getOrDefault(e.getKey(), List.of()).isEmpty();
            if (e.getValue().size() > 1 || emptyBesideInitialisation) out.add(differenceRule(member));
        }
        return out;
    }

    /**
     * The fault of RUNTIME-SEMANTICS ch. 5.7 as a logical fact: per object of the declaring type of {@code lid}, an
     * accumulate over the {@link Value} facts on that object's attribute, and where they hold different values a
     * {@link Fout} with the {@link Schrijversconflict} that names the writers in the order of the document, under the
     * first of them. The fault rests on the values that stand, so a difference that exists on an intermediate state only
     * goes again when a writer's condition or value changes, and the session reports the differences that stand at its
     * fixpoint, as it reports every evaluation fault ({@link #zetAfgeleideWaarden}).
     */
    private RulePlan differenceRule(Member member) {
        ElementSleutel sleutel = member.sleutel();
        Alpha ofTarget = equal(id("waarde"), "attribuut", sleutel);
        Join onSubject = new Join(id("waarde-onderwerp"), "onderwerp", SUBJECT);
        TestItem difference = new TestItem(id("verschil"), TestItem.Role.VORM,
                new Term.Call(StaticFunction.DIFFER, List.of(new Term.SlotValue(0, null)), null), -1, List.of(0), 1);
        return new RulePlan("schrijvers " + member.declarant().naam + "." + member.naam() + ": verschil", null, null,
                new ReadTable(List.of(new dev.uchar.regelspraak.transpiler.tms.plan.Read(0, ReadKind.VALUES,
                        "waarden", "$waarden"))),
                List.of(new Subject(member.javaClass(), SUBJECT, List.of(), List.of(), List.of()),
                        new Accumulation(new FactPattern(Value.class, "$waarde", List.of(ofTarget), List.of(onSubject),
                                List.of()), List.of(new Accumulation.Aggregation(Accumulation.Kind.LIST, null, "$waarden"))),
                        difference),
                new Consequence.Schrijversconflict(SUBJECT, 0, sleutel, member.naam(), documentOrder, typeClasses, typen,
                        Consequence.Insertion.LOGICAL));
    }

    // ---------------- an evaluation fault in a condition ----------------

    /**
     * The rule that inserts the fault of a condition that can fault, beside the rule {@code ruleName} of the form
     * {@code vorm} that tests the condition. A condition is tested in the left-hand side, and an exception there leaves
     * the fire, where truth maintenance cannot withdraw it when the reads it rested on change; the form's own rule
     * therefore tests the condition with the role {@link TestItem.Role#ZONDER_FOUT}, which does not hold where it
     * faults, and this rule, whose left-hand side is that rule's own built again over fresh declarations ({@code nieuw}
     * sets up the subject pattern and what precedes it), with the same {@code blockade} after the test and the test of
     * the role {@link TestItem.Role#FOUT}, that the condition faults, inserts the named {@link Fout} logically, on the
     * subject of the form. It fires exactly where the form's rule tests the condition and the test faults, and the
     * fault goes when a read it rested on changes. The sentence
     * names the form's rule. A condition faults also where it
     * reaches a fold whose filter faulted for a member ({@link #fold}); the filter itself inserts no fault
     * ({@link #filterFaultRule}).
     */
    private RulePlan conditionFaultRule(String vorm, String ruleName, Supplier<Reads> fresh, Conditie c,
                                          Function<Reads, List<LhsItem>> blockade) {
        Reads lez = fresh.get();
        Set<Integer> read = lez.follow();
        Term testTerm = voorwaarde(c, lez);
        lez.stop();
        List<LhsItem> items = lez.plan(TestItem.Role.FAULT, testTerm, read, c);
        if (blockade != null) items.addAll(blockade.apply(lez));
        Consequence fout = new Consequence.TestFault(SUBJECT, testTerm, TestItem.Role.FAULT, List.copyOf(read), lez.size(),
                new Consequence.FaultName(vorm, model.geëmitteerdeRegelNaam(vorm)), Consequence.Insertion.LOGICAL);
        lez.recorder();
        return lez.rulePlan(ruleName + ": fout in de voorwaarde", items, fout);
    }

    /** The role of the test of a condition in the rule of its form: where the condition can fault it is tested without
     *  its fault, which the fault rule takes ({@link #conditionFaultRule}). */
    private static TestItem.Role conditionRole(boolean canFail) {
        return canFail ? TestItem.Role.WITHOUT_FAULT : TestItem.Role.CONDITION;
    }

    /**
     * The rule that deposits the §9.5 inconsistency of RUNTIME-SEMANTICS ch. 1.2 for the condition {@code c} of
     * {@code vorm}, whose rule is {@code ruleName}: the reference interpreter deposits wherever its evaluation of a
     * condition divides a filled dividend by a leeg divisor or takes the root of a negative number, whether the condition
     * then holds or not (ch. 5.6), and only at the places that evaluation reaches (ch. 5.10). A condition is tested in the
     * left-hand side, where Drools inserts nothing, so a rule of the form's own, with the same left-hand side
     * ({@code blockade} included) and the test that evaluating the condition reaches such a place ({@link DepositFlag}),
     * inserts the deposit logically under the form and the subject; it goes when a read it rested on changes. Null where
     * {@code c} holds no division and no root.
     */
    private RulePlan depositRule(String vorm, String ruleName, Supplier<Reads> fresh, Conditie c,
                                    Function<Reads, List<LhsItem>> blockade) {
        if (!deposits(c)) return null;
        Reads lez = fresh.get();
        Set<Integer> read = lez.follow();
        Term testTerm = voorwaarde(c, lez);
        lez.stop();
        int depositFlag = lez.depositFlagSlot();
        if (depositFlag < 0) throw new IllegalStateException("de voorwaarde van \"" + vorm + "\" deelt, maar kreeg geen sonde");
        List<LhsItem> items = lez.plan(TestItem.Role.DEPOSIT, testTerm, read, c);
        if (blockade != null) items.addAll(blockade.apply(lez));
        lez.recorder();
        return lez.rulePlan(ruleName + ": deponering in de voorwaarde", items,
                new Consequence.Inconsistency(SUBJECT, vorm, Consequence.Insertion.LOGICAL));
    }

    // ---------------- a consistency and the §9.5 channel ----------------

    /**
     * A consistency (IR-SPEC ch. 3.5): the subject pattern with the reads of its assertion bound as a rule's are, one
     * test that holds where the assertion does not, and a consequence that inserts the subject's §9.5 inconsistency
     * logically, under the form's name. The violation is the negation of the assertion read with the empty operand of
     * RUNTIME-SEMANTICS ch. 1.2: the reference interpreter deposits where the assertion does not evaluate to true,
     * table by table per shape (an equality of numbers violated where exactly one is leeg, an ordering violated where
     * an operand is leeg), which the condition lowering of this branch carries. Every read is bound to a total
     * standing, so a subject has one activation and the channel one error fact per form and subject, without a guard
     * against a second firing. A violation found against a transient value goes when that value changes; the
     * destructive lowering, until card tms-omschakeling, ordered the consistency after the writers of what it reads
     * instead and never withdrew one, which reaches the same fixpoint. An assertion that faults is no violation: beside
     * the rule, where the assertion can fault, the rule that inserts its fault ({@link #conditionFaultRule}).
     */
    private List<RulePlan> consistencyRules(ResolvedConsistentie c) {
        // the rule is named by the form, and the channel test reads the error facts by that name
        if (c.naam() == null) throw new Refusal("een consistentie zonder naam (WF-107)");
        if (c.subject() == null || c.assertie() == null) throw new Refusal("een consistentie zonder subject of assertie");
        Supplier<Reads> fresh = () -> new Reads(c.subject()).valid(c.geldigheid());
        Reads lez = fresh.get();
        Set<Integer> read = lez.follow();
        Term holds = voorwaarde(c.assertie(), lez);
        boolean canFail = lez.canFail();
        lez.stop();
        Term geschonden = new Term.Call(StaticFunction.NIET, List.of(holds), bron(c.assertie().bron()));
        List<LhsItem> items = lez.plan(conditionRole(canFail), geschonden, read, c.assertie());
        String naam = c.naam();
        lez.recorder();
        List<RulePlan> out = new ArrayList<>();
        out.add(lez.rulePlan(model.geëmitteerdeRegelNaam(naam), items,
                new Consequence.Inconsistency(SUBJECT, naam, Consequence.Insertion.LOGICAL)));
        if (canFail) out.add(conditionFaultRule(naam, model.geëmitteerdeRegelNaam(naam), fresh, c.assertie(), null));
        RulePlan deposit = depositRule(naam, model.geëmitteerdeRegelNaam(naam), fresh, c.assertie(), null);
        if (deposit != null) out.add(deposit);
        RulePlan withoutDate = validityFault(model.geëmitteerdeRegelNaam(naam), naam, c.geldigheid());
        if (withoutDate != null) out.add(withoutDate);
        return out;
    }

    /** A uniqueness over one source (RUNTIME-SEMANTICS ch. 5.9), unscoped or scoped by a parent ({@link #unionUniquenessRules}). */
    private List<RulePlan> uniquenessRules(ResolvedUniciteit u) {
        if (u.subject() == null || u.sleutels().isEmpty()) throw new Refusal("een Uniqueness zonder bron of sleutel");
        return unionUniquenessRules(u.naam(), List.of(new ResolvedUniciteitUnie.Bron(u.subject(), u.sleutels())),
                u.scopeRol(), u.geldigheid());
    }

    /** A uniqueness over two or more sources, the union of ch. 5.9, rule 1: one key domain, compared position by
     *  position, so every source carries as many keys (WF-030); a union has no scope (rule 3). */
    private List<RulePlan> uniquenessUnionRules(ResolvedUniciteitUnie u) {
        if (u.bronnen().size() < 2)
            throw new Refusal("een Uniqueness over meer bronnen met " + u.bronnen().size() + " bron: een unie draagt er"
                    + " ten minste twee (WF-030)");
        int ariteit = u.bronnen().get(0).sleutels().size();
        for (ResolvedUniciteitUnie.Bron b : u.bronnen())
            if (b.type() == null || b.sleutels().size() != ariteit)
                throw new Refusal("een Uniqueness over meer bronnen met sleutels van ongelijke ariteit of een bron zonder"
                        + " type: de sleutels worden positie voor positie vergeleken (WF-030)");
        return unionUniquenessRules(u.naam(), u.bronnen(), null, u.geldigheid());
    }

    /**
     * A uniqueness (RUNTIME-SEMANTICS ch. 5.6, 5.9): a key that more than one row carries marks every instance that
     * carries it on the §9.5 channel, under the form's name. A key rule per source, over the instances of the source's
     * type, binds the key components (a derived one through its standing) and, for a scoped form, the parent, and
     * inserts the instance's {@link UniquenessKey} logically where every component is gevuld and the parent is
     * there: an empty component never collides, and an instance without a parent takes no part (rule 2). The violation
     * rule binds each key and counts the keys that collide with it, itself included, with an accumulate; where more
     * than one does it inserts the instance's {@link UniquenessViolation}, which the channel rule turns into the §9.5
     * inconsistency, one per instance, also where two sources of a union reach it. A key belongs to its source, so an
     * instance that two sources reach with an equal key duplicates against itself (rule 1). Every fact is logical, so a
     * duplicate that arises on an intermediate state goes when the keys move on, and the channel holds what stands at
     * the fixpoint, where the reference interpreter evaluates the form (ch. 5.9). A key on a timeline reads
     * ever-uniqueness: one key per value its timeline ever carries ({@link #keyRule}), so two instances collide
     * where they ever carried one value, and an instance whose key the case gives as the asOfDate-bucket scalar faults
     * ({@link #keyFormRule}, ch. 8.4). A scope over a relation a form creates is refused.
     */
    private List<RulePlan> unionUniquenessRules(String naam, List<ResolvedUniciteitUnie.Bron> sources,
                                             RolBindingRef scopeRole, Geldigheid validity) {
        // the rules are named by the form, and the channel test reads the error facts by that name
        if (naam == null) throw new Refusal("een Uniqueness zonder naam (WF-107)");
        String ruleName = model.geëmitteerdeRegelNaam(naam);
        List<RulePlan> out = new ArrayList<>();
        for (int b = 0; b < sources.size(); b++) {
            out.add(keyRule(naam, ruleName, b, sources.get(b), scopeRole, validity));
            RulePlan vorm = keyFormRule(naam, ruleName, b, sources.get(b), validity);
            if (vorm != null) out.add(vorm);
        }
        FactPattern sleutel = new FactPattern(UniquenessKey.class, "$sleutel", List.of(equal(id("uniciteit"), "uniqueness",
                naam)), List.of(), List.of(Binding.van("$element", UniquenessKey.class, "element")));
        Alpha ofUniqueness = equal(id("uniciteit"), "uniqueness", naam);
        BetaTest collision = new BetaTest(id("botsing"), StaticFunction.COLLIDES_WITH, "$sleutel");
        Accumulation collisions = new Accumulation(new FactPattern(UniquenessKey.class, "$ander", List.of(ofUniqueness),
                List.of(), List.of(collision), List.of()),
                List.of(new Accumulation.Aggregation(Accumulation.Kind.COUNT, null, "$botsingen")));
        ReadTable table = new ReadTable(List.of(new dev.uchar.regelspraak.transpiler.tms.plan.Read(0,
                ReadKind.COLLISIONS, "botsingen", "$botsingen")));
        TestItem duplicate = new TestItem(id("dubbel"), TestItem.Role.VORM,
                new Term.Call(StaticFunction.DUPLICATE, List.of(new Term.SlotValue(0, null)), null), -1, List.of(0), 1);
        out.add(new RulePlan(ruleName, vorm, null, table, List.of(sleutel, collisions, duplicate),
                new Consequence.UniquenessViolation("$element", naam, Consequence.Insertion.LOGICAL)));
        FactPattern violation = new FactPattern(UniquenessViolation.class, "$schending", List.of(equal(id("schending"),
                "uniqueness", naam)), List.of(), List.of(Binding.van("$subject", UniquenessViolation.class, "element")));
        out.add(new RulePlan(ruleName + ": kanaal", vorm, null, new ReadTable(List.of()),
                List.of(violation), new Consequence.Inconsistency("$subject", naam, Consequence.Insertion.LOGICAL)));
        RulePlan withoutDate = validityFault(ruleName, naam, validity);
        if (withoutDate != null) out.add(withoutDate);
        return out;
    }

    /** The key rule of source {@code b} of the uniqueness {@code naam}: over the instances of the source's type, gated
     *  on the evaluation date by the form's validity, the key components and the parent bound, and the instance's
     *  {@link UniquenessKey} inserted logically where it takes part ({@link #unionUniquenessRules}). An enumeration
     *  component is compared as the declared name it is, up to normalisation (ch. 5.6).
     *
     *  <p>A component on a timeline reads ever-uniqueness (RUNTIME-SEMANTICS ch. 5.9, decided 2026-08-20): it claims
     *  every value its timeline ever carries, so the rule inserts one key per such value, beside the other components,
     *  and none where the timeline is absent or carries no value. The timeline is bound like any read
     *  ({@link Reads#timeline}), so a key a rule derives later takes its keys along. The values are taken by the
     *  runtime's {@code rsOoitWaarden}: without trailing zeros and once each, so an instance whose timeline carries one
     *  value in two segments claims it once and does not duplicate against itself. A key has at most one component on a
     *  timeline, as the reference interpreter refuses more: the same-moment join of two has no corpus case. */
    private RulePlan keyRule(String naam, String ruleName, int b, ResolvedUniciteitUnie.Bron bron,
                                   RolBindingRef scopeRole, Geldigheid validity) {
        Objecttype type = bron.type();
        Reads lez = new Reads(type).valid(validity);
        Set<Integer> read = lez.follow();
        List<Integer> keySlots = new ArrayList<>();
        List<Boolean> enumeration = new ArrayList<>();
        int ever = -1;
        for (String sleutel : bron.sleutels()) {
            if (declared(type, sleutel, false).time() != null) {
                String description = "een uniciteitssleutel '" + sleutel + "' van " + type.naam + " op een tijdlijn";
                if (ever >= 0)
                    throw new Refusal(description + " naast een tweede: een sleutel heeft hoogstens één tijdsafhankelijk deel,"
                            + " de gelijktijdige koppeling van twee heeft geen corpuscase (RUNTIME-SEMANTICS ch. 5.9); "
                            + STILL_NOT);
                Member member = timeMember(type, sleutel, false, description);
                if (!member.datatype().isNumeriek())
                    throw new Refusal(description + " die geen getal is: " + STILL_NOT);
                ever = keySlots.size();
                keySlots.add(lez.timeline(member));
                enumeration.add(false);
                continue;
            }
            Member member = member(type, sleutel, false);
            keySlots.add(lez.own(member));
            enumeration.add(member.datatype() instanceof Datatype.Enumeratie);
        }
        int scope = scopeRole == null ? -1 : lez.scopeRead(scopeRole);
        lez.stop();
        int timeline = ever;
        // the key takes part where every component off the timeline is gevuld and the parent is set
        List<Term> participation = new ArrayList<>();
        for (int i = 0; i < keySlots.size(); i++) if (i != timeline) participation.add(filled(keySlots.get(i)));
        // a key the case gives as the asOfDate-bucket scalar is the fault of the key-form rule beside this one
        if (timeline >= 0) participation.add(emmerscalair(keySlots.get(timeline), false));
        if (scope >= 0) participation.add(filled(scope));
        List<LhsItem> items = lez.plan(TestItem.Role.VORM, all(participation), read, null);
        List<Term> components = new ArrayList<>();
        for (int i = 0; i < keySlots.size(); i++) {
            Term component = new Term.SlotValue(keySlots.get(i), null);
            components.add(enumeration.get(i) ? new Term.Call(StaticFunction.RS_ENUM_NAME, List.of(component), null)
                    : component);
        }
        lez.recorder();
        return lez.rulePlan(ruleName + ": sleutel van " + type.naam + " " + (b + 1), items,
                new Consequence.UniquenessKey(SUBJECT, naam, b, components, timeline, scope, List.copyOf(read),
                        lez.size(), Consequence.Insertion.LOGICAL));
    }

    /**
     * The rule beside the key rule of source {@code b} of the uniqueness {@code naam} where its key has a component on a
     * timeline: over the instances of the source's type, gated by the form's validity, the components up to that one
     * bound as the key rule binds them, the test that every component before it is gevuld and that the case gives the
     * timeline as the asOfDate-bucket scalar ({@link BucketScalar}), and a consequence that inserts the fault of
     * RUNTIME-SEMANTICS ch. 5.9 logically as a {@link Fout} of the instance ({@link Invoervormfout}), in the reference
     * interpreter's words: ever-uniqueness reads every value the timeline ever carries, and a scalar would miss values.
     * The key's form is checked on every instance of the type, before the scope narrows the rows ("this form error
     * precedes the scope narrowing"), so the parent and the components after the timeline do not enter; the reference
     * interpreter builds a row's tuples component by component and stops at an empty one, so an empty component before
     * the timeline leaves the timeline unread. Null for a key without a component on a timeline.
     */
    private RulePlan keyFormRule(String naam, String ruleName, int b, ResolvedUniciteitUnie.Bron bron,
                                       Geldigheid validity) {
        Objecttype type = bron.type();
        int positie = -1;
        for (int i = 0; i < bron.sleutels().size() && positie < 0; i++)
            if (declared(type, bron.sleutels().get(i), false).time() != null) positie = i;
        if (positie < 0) return null;
        Reads lez = new Reads(type).valid(validity);
        Set<Integer> read = lez.follow();
        List<Integer> preceding = new ArrayList<>();
        for (int i = 0; i < positie; i++) preceding.add(lez.own(member(type, bron.sleutels().get(i), false)));
        Member timeMember = timeMember(type, bron.sleutels().get(positie), false, "een uniciteitssleutel '"
                + bron.sleutels().get(positie) + "' van " + type.naam + " op een tijdlijn");
        int timeline = lez.timeline(timeMember);
        lez.stop();
        List<Term> bucket = new ArrayList<>();
        for (int i : preceding) bucket.add(filled(i));
        bucket.add(emmerscalair(timeline, true));
        List<LhsItem> items = lez.plan(TestItem.Role.VORM, all(bucket), read, null);
        lez.recorder();
        return lez.rulePlan(ruleName + ": sleutelvorm van " + type.naam + " " + (b + 1), items,
                new Consequence.Invoervormfout(SUBJECT, "the ever-unique key \"" + timeMember.naam()
                        + "\" needs the full timeline; ", " supplies only a scalar (ch. 5.9)",
                        new Consequence.FaultName(naam, ruleName), Consequence.Insertion.LOGICAL));
    }

    /**
     * A test of the §9.5 channel, "regelversie X is inconsistent" (RUNTIME-SEMANTICS ch. 5.6): an existential over the
     * error facts of the form X, with no subject of its own, read through X's {@link ChannelState}. The producers this
     * branch carries are a consistency, a uniqueness, and a rule whose condition or value can deposit a §6.5 or §6.6
     * inconsistency ({@link #writeRule}, {@link #depositRule}); any other name is refused, since a test of a
     * channel nothing fills would never hold and say nothing. "is gevuurd" reads no channel; it is no construct of the
     * core, and is refused with that reason.
     */
    private Term channelRead(Conditie.Regelversie rv, Reads lez) {
        // decided as a predicate of the dialect over the fixpoint, never a construct of the core: the reference
        // interpreter faults on it ("not implemented") and the ASP backend does not translate it
        if (!rv.inconsistent())
            throw notRead("Regelversie 'is gevuurd'", rv, "geen vorm van de kern maar een predicaat van het"
                    + " RegelSpraak-dialect over het fixpunt (RUNTIME-SEMANTICS ch. 5.6, CONSTRUCTS rij 30), dat de"
                    + " referentie-interpreter en de ASP-backend niet evalueren");
        String producer = rv.regel();
        if (!channelProducer(producer))
            throw new Refusal("een Regelversie 'is inconsistent' over \"" + producer + "\" in de voorwaarde ('"
                    + snippet(rv) + "'): deze tak draagt als producent van het §9.5-kanaal alleen een"
                    + " Consistency, een Uniqueness en een regel met een deling of een wortel in haar voorwaarde of haar"
                    + " waarde (§6.5, §6.6)");
        int i = lez.channel(producer);
        return new Term.Call(StaticFunction.CHANNEL, List.of(new Term.SlotValue(i, null)), bron(rv.bron()));
    }

    /** Whether the form named {@code naam}, spelled as the error facts carry it, fills a channel this branch lowers. */
    private boolean channelProducer(String naam) {
        if (naam == null) return false;
        for (ResolvedConsistentie c : model.consistenties) if (naam.equals(c.naam())) return true;
        for (ResolvedUniciteit u : model.uniciteiten) if (naam.equals(u.naam())) return true;
        for (ResolvedUniciteitUnie u : model.uniciteitUnies) if (naam.equals(u.naam())) return true;
        for (ResolvedRegel r : model.resolvedRegels)
            if (naam.equals(r.naam()) && (deposits(r.voorwaarde())
                    || r.soort() != ResolvedRegel.Soort.KENMERKTOEKENNING && deposits(r.waarde())))
                return true;
        return false;
    }

    /** Whether a value holds a place the §6.5 or §6.6 deposit of {@link #writeRule} covers ({@link #depositSite}),
     *  where the rule's own evaluation of the value can reach it: through every form {@link #waarde} evaluates in the
     *  rule itself, and into the filter of a fold over a set source and of each selection of an instance-set union,
     *  which a filter deposit rule evaluates and the reading rule hears where it reaches the fold
     *  ({@link #filterDepositRule}, {@link #readFold}). The argument of such a fold is a read of the member's
     *  attribute or the union, nothing else (WF-037, refused by the loader), so it holds no place of its own. */
    private boolean deposits(Expr e) {
        return switch (e) {
            case null -> false;
            case Expr.Binair b -> depositSite(b) || deposits(b.links()) || deposits(b.rechts());
            case Expr.Functie f -> depositSite(f) || f.argumenten().stream().anyMatch(this::deposits)
                    || deposits(f.leegwaarde()) || deposits(f.filter());
            case Expr.InstanceSetUnion u -> u.takken().stream().anyMatch(t -> deposits(t.filter()));
            case Expr.Afgerond a -> deposits(a.basis());
            case Expr.Begrensd g -> deposits(g.basis()) || deposits(g.minimum()) || deposits(g.maximum());
            case Expr.Let l -> deposits(l.value()) || deposits(l.body());
            case Expr.IfThenElse k -> deposits(k.conditie()) || deposits(k.dan()) || deposits(k.anders());
            case Expr.TekstReeks t -> t.delen().stream()
                    .anyMatch(d -> d instanceof Expr.TekstReeks.Ingevoegd i && deposits(i.expr()));
            default -> false;
        };
    }

    private boolean deposits(Conditie c) {
        return switch (c) {
            case null -> false;
            case Conditie.Vergelijking v -> deposits(v.links()) || deposits(v.rechts());
            case Conditie.Predicaat p -> deposits(p.operand());
            case Conditie.Samengesteld s -> s.onderdelen().stream().anyMatch(this::deposits);
            case Conditie.Kwantorvouw kv -> deposits(kv.filter()) || deposits(kv.toets());
            default -> false;
        };
    }

    /** Whether {@code e} is itself a place that deposits: every division and every square root, whatever its operands,
     *  as the reference interpreter deposits wherever it divides a filled dividend by a leeg divisor or takes the root of
     *  a negative number (RUNTIME-SEMANTICS ch. 1.2, 5.6). */
    private static boolean depositSite(Expr e) {
        if (e instanceof Expr.Binair b) return b.op() == Expr.BinOp.GEDEELD_DOOR || b.op() == Expr.BinOp.GEDEELD_DOOR_ABS;
        return e instanceof Expr.Functie f && f.kind() == Expr.FunctieKind.WORTEL && f.argumenten().size() == 1;
    }

    /**
     * The two generic rules that keep one {@link ChannelState} for the channel of the form named {@code producer}:
     * inconsistent while an error fact of that form stands, on any subject, and consistent while none does. The second
     * carries the negation over the channel, so a condition that reads it, in either polarity, binds the standing as a
     * positive pattern.
     */
    private List<RulePlan> channelStateRules(String producer) {
        // the class the fact model generates for the inconsistency is data of the plan, by which a rendering names it
        Class<?> javaClass;
        try {
            javaClass = Class.forName(INCONSISTENCY, true, beanLoader);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("geen beanklasse " + INCONSISTENCY + " voor het kanaal van " + producer, e);
        }
        String naam = "kanaalstand " + producer + ": ";
        FactPattern fout = new FactPattern(javaClass, "$inconsistentie", List.of(equal(id("kanaal"), "regel", producer)),
                List.of(), List.of());
        RulePlan inconsistent = new RulePlan(naam + "inconsistent", null, null, new ReadTable(List.of()),
                List.of(new Exists(List.of(fout), List.of(fout.naam()), List.of())),
                new Consequence.InsertChannelState(producer, true, Consequence.Insertion.LOGICAL));
        FactPattern none = new FactPattern(javaClass, "$inconsistentie", List.of(equal(id("kanaal"), "regel", producer)),
                List.of(), List.of());
        RulePlan consistent = new RulePlan(naam + "consistent", null, null, new ReadTable(List.of()),
                List.of(new Not(none)), new Consequence.InsertChannelState(producer, false, Consequence.Insertion.LOGICAL));
        return List.of(inconsistent, consistent);
    }

    // ---------------- an allocation ----------------

    /**
     * An allocation (IR-SPEC ch. 3.2, RUNTIME-SEMANTICS ch. 4): a computation over the set of recipients that the
     * left-hand side binds. One rule of the allocation inserts, per source and per recipient, an {@link Recipient}
     * logically: the recipient is an instance of {@code targetType} that the connecting relation links to the source
     * ({@link #recipientsJoin}), so a related instance that is no {@code targetType} receives nothing and stays leeg
     * (ch. 4.1), and its weight, ordering key and cap are bound in that rule's left-hand side, a derived one through its
     * standing. The allocation's own rule binds the source, its pot, the reads of its condition and, through an
     * accumulate, the list of the source's {@link Recipient} facts ({@link Reads#recipients}), and its consequence
     * computes the distribution from those bound values only ({@link Allocation}): each share and the rest are a
     * {@link Value} with the allocation as the writer, and each degradation of ch. 4.3 and 4.4 (an empty weight, a
     * negative proportional weight, an empty or negative cap) is the allocation's §9.5 deposit on that recipient. All of
     * it is logical, so a recipient that arrives or leaves, a weight, key, cap or pot that changes, and a condition that
     * stops holding take the whole distribution of the source along, the rest included (ch. 4.3), and a deposit made on
     * an intermediate state goes again: like a fault it counts at the fixpoint (PLAN §9, 2026-10-09). An empty pot is
     * no distribution in every method, as ch. 4.4 decides for v1: it writes nothing, deposits nothing and is no fault,
     * where the reference interpreter keeps the v0 hole of the equal-shares and the proportional method and faults
     * ({@link Allocation}). A version with a bounded validity gates on the evaluation date and faults a session
     * without one, as a rule's version does; a condition that can fault or deposit has its own fault and deposit rules,
     * on the source, as the reference interpreter evaluates the condition per source before anything else.
     *
     * <p>A share or a rest replaces the input on its target, as the reference interpreter overwrites it, and a recipient
     * that receives nothing keeps its input; the member value an object creation sets is the input of the object it
     * makes. Every other writer of a target is refused ({@link #allocationWriters}): two allocations on one target
     * are ill-formed (WF-026), so is a defaults group beside an allocation (WF-050), and a rule beside one, a computing
     * rule or an initialisation, this branch does not carry yet.
     */
    private List<RulePlan> allocation(ResolvedVerdeling v) {
        String naam = v.naam();
        // the rules of a form are named by the form, and the deposits and shares carry that name
        if (naam == null) throw new Refusal("een verdeling zonder naam (WF-107)");
        Objecttype bron = v.bronType(), doel = v.doelType();
        ResolvedVerdeling.Methode methode = v.methode();
        if (bron == null || doel == null || v.bronAttribuut() == null || v.doelAttribuut() == null || methode == null)
            throw new Refusal("een verdeling zonder opgeloste bron, doel of methode");
        RolBindingRef rb = roleBindings.get(v);
        if (rb == null) throw new Refusal("een verdeling zonder connectingRole (IR-SPEC ch. 3.2)");
        // WF-025, which the verifier holds and this backend does not run: the fields each method carries
        boolean equal = methode == ResolvedVerdeling.Methode.GELIJKE_DELEN;
        if (methode == ResolvedVerdeling.Methode.NAAR_RATO && v.ratoAttribuut() == null)
            throw new Refusal("een verdeling naar rato zonder gewicht (WF-025)");
        if (equal && v.ratoAttribuut() != null)
            throw new Refusal("een verdeling in gelijke delen met een gewicht (WF-025)");
        if (v.maximumAttribuut() != null && v.ratoAttribuut() == null)
            throw new Refusal("een verdeling met een maximum zonder gewicht (WF-025)");
        if (methode != ResolvedVerdeling.Methode.OP_VOLGORDE && v.groepeerAttribuut() != null)
            throw new Refusal("een volgordesleutel op een verdeling die niet op volgorde verdeelt (WF-025)");
        Member pot = numberMember(bron, v.bronAttribuut(), "de pot");
        Member aandeel = numberMember(doel, v.doelAttribuut(), "het aandeel");
        Member rest = v.restantAttribuut() == null ? null : numberMember(bron, v.restantAttribuut(), "het restant");
        Member weight = v.ratoAttribuut() == null ? null : numberMember(doel, v.ratoAttribuut(), "het gewicht");
        Member maximum = v.maximumAttribuut() == null ? null : numberMember(doel, v.maximumAttribuut(), "het maximum");
        Member sleutel = v.groepeerAttribuut() == null ? null : member(doel, v.groepeerAttribuut(), false);
        if (sleutel != null && !sleutel.datatype().isOrdenbaar())
            throw new Refusal("de volgordesleutel '" + sleutel.naam() + "' van type " + typeName(sleutel.datatype())
                    + " is niet ordenbaar (WF-025)");
        allocationWriters(v, doel, aandeel);
        if (rest != null) allocationWriters(v, bron, rest);

        String ruleName = model.geëmitteerdeRegelNaam(naam);
        List<RulePlan> out = new ArrayList<>();

        // per source and recipient: the recipient's values, bound in this rule's left-hand side, after the source
        Consumer<Reads> range = recipientsJoin(rb, bron, doel, naam);
        Reads member = new Reads(doel);
        member.outside = foldBinders;
        member.addToHeadPlan(new FactPattern(beanClass(bron), FOLD_SUBJECT, List.of(), List.of(), List.of()));
        range.accept(member);
        int recipient = member.member();
        int g = weight == null ? -1 : member.own(weight);
        int k = sleutel == null ? -1 : member.own(sleutel);
        int m = maximum == null ? -1 : member.own(maximum);
        List<LhsItem> memberRule = member.plan(TestItem.Role.CONDITION, null, Set.of(), null);
        member.recorder();
        out.add(member.rulePlan(ruleName + ": ontvanger", memberRule,
                new Consequence.Recipient(FOLD_SUBJECT, naam, recipient, g, k, m, Consequence.Insertion.LOGICAL)));

        // per source: the condition, the pot and the list of recipients, and the distribution computed from them
        Supplier<Reads> fresh = () -> new Reads(bron).valid(v.geldigheid());
        Reads lez = fresh.get();
        Set<Integer> read = lez.follow();
        Term testTerm = v.voorwaarde() == null ? null : voorwaarde(v.voorwaarde(), lez);
        boolean canFail = lez.canFail();
        lez.stop();
        int p = lez.own(pot);
        int lijst = lez.recipients(naam);
        List<LhsItem> items = lez.plan(conditionRole(canFail), testTerm, read, v.voorwaarde());
        ElementSleutel aandeelKey = aandeel.sleutel(), restKey = rest == null ? null : rest.sleutel();
        lez.recorder();
        out.add(lez.rulePlan(ruleName, items, new Consequence.Payout(SUBJECT, naam, methode, p, lijst,
                weight != null, maximum != null, v.afnemend(), v.afrondingDecimalen(), aandeelKey, restKey,
                naam, Consequence.Insertion.LOGICAL)));
        derived.put(aandeelKey, aandeel);
        if (rest != null) derived.put(restKey, rest);
        if (canFail) out.add(conditionFaultRule(naam, ruleName, fresh, v.voorwaarde(), null));
        RulePlan deposit = depositRule(naam, ruleName, fresh, v.voorwaarde(), null);
        if (deposit != null) out.add(deposit);
        RulePlan withoutDate = validityFault(ruleName, naam, v.geldigheid());
        if (withoutDate != null) out.add(withoutDate);
        return out;
    }

    /** Attribute {@code naam} of {@code houder} as an allocation reads or writes it as a number: the pot, a share, the
     *  rest, a weight or a cap, {@code rol} naming which. The distribution computes with numbers only, so another
     *  datatype is refused by name. */
    private Member numberMember(Objecttype houder, String naam, String rol) {
        Member member = member(houder, naam, false);
        if (!member.datatype().isGetalachtig())
            throw new Refusal("een verdeling waarvan " + rol + " het attribuut '" + member.naam() + "' van "
                    + member.declarant().naam + " is, van type " + typeName(member.datatype()) + " en geen getal: " + STILL_NOT);
        return member;
    }

    /**
     * Refuses the allocation {@code v} where another form writes the attribute {@code lid} that it writes on
     * {@code type}, its share target or its rest, on an instance it writes too: on {@code type}, an ancestor or a
     * descendant ({@link #related}), read per concrete type as WF-026 and WF-050 read it, so a form on a sister subtype
     * does not count. Another allocation, not a version of {@code v}, is ill-formed beside it (WF-026), and so is a
     * defaults group (WF-050). A rule beside it is well-formed, and refused until this branch carries it: a computing
     * rule is a second equation of the target, whose value must agree at the fixpoint (RUNTIME-SEMANTICS ch. 5.7), a
     * disagreement this branch keys on two computing rules only ({@link #differenceRule}); an initialisation is the
     * lowest default beside the allocation (ch. 7.7), whose guard would read the allocation's shares as it reads every
     * other writer's value, but no witness holds that pair against the reference interpreter yet. The member value an
     * object creation sets is no writer here: it is the input of the object it makes, which a share replaces.
     */
    private void allocationWriters(ResolvedVerdeling v, Objecttype type, Member member) {
        ElementSleutel el = member.sleutel();
        String own = ProducentConsument.basisNaam(v.naam());
        Set<String> allocations = new LinkedHashSet<>(), groups = new LinkedHashSet<>(), rules = new LinkedHashSet<>();
        for (ResolvedVerdeling o : model.verdelingen) {
            if (o.naam() != null && ProducentConsument.basisNaam(o.naam()).equals(own)) continue;
            boolean writes = o.doelType() != null && o.doelAttribuut() != null && related(o.doelType(), type)
                    && ProducentConsument.elementVan(o.doelType(), o.doelAttribuut(), false).equals(el)
                    || o.bronType() != null && o.restantAttribuut() != null && related(o.bronType(), type)
                    && ProducentConsument.elementVan(o.bronType(), o.restantAttribuut(), false).equals(el);
            if (writes) allocations.add("\"" + nameOf(o.naam()) + "\"");
        }
        if (!allocations.isEmpty())
            throw new Refusal("twee verdelingen schrijven het attribuut '" + member.naam() + "' van " + type.naam
                    + " (ook " + String.join(", ", allocations) + "): een doel heeft hoogstens één verdeling (WF-026)");
        for (int i = 0; i < defaults.size(); i++) {
            PrioritizedDefaults pd = defaults.get(i);
            Objecttype t = model.objecttypeOpNaam(pd.onderwerp());
            if (t != null && pd.doel() != null && !pd.isKenmerk() && related(t, type)
                    && ProducentConsument.elementVan(t, pd.doel(), false).equals(el))
                groups.add(pd.naam() != null ? "\"" + pd.naam() + "\"" : "/rules/prioritizedDefaults/" + i);
        }
        if (!groups.isEmpty())
            throw new Refusal("een verdeling naast de defaultsgroep " + String.join(", ", groups) + " op het attribuut '"
                    + member.naam() + "' van " + type.naam + ": een doel van een defaultsgroep heeft geen andere schrijver"
                    + " (WF-050)");
        for (ResolvedRegel r : model.resolvedRegels)
            if (r.soort() != ResolvedRegel.Soort.KENMERKTOEKENNING
                    && r.doel() instanceof Expr.AttribuutRef d && d.objecttype() != null && d.attribuut() != null
                    && related(d.objecttype(), type)
                    && ProducentConsument.elementVan(d.objecttype(), d.attribuut(), false).equals(el))
                rules.add("\"" + nameOf(r.naam()) + "\"");
        if (!rules.isEmpty())
            throw new Refusal("een verdeling naast de regel " + String.join(", ", rules) + " op het attribuut '"
                    + member.naam() + "' van " + type.naam + ": een verdeling naast een rekenende regel of een initialisatie"
                    + " op één doel; " + STILL_NOT);
    }

    /**
     * How the rule of an allocation joins a recipient, its subject, to the source, its {@link Reads#carrier}, over the
     * connecting relation {@code rb}, whose bound role is the recipient's and its sibling the source's (IR-SPEC ch. 3.2),
     * as a fold's one-step range joins a member ({@link #oneStepRange}): over a relation a form creates, through a
     * {@link RelationLink} in an {@code exists}; over an input relation, through the one scalar bean field §13.3.9 stores
     * it in, on the source where the recipient role is to-one and else on the recipient. Refused as there: a binding
     * that names no role pair, a to-one recipient role whose sibling is to-many, and a join the bean model gives no
     * field.
     */
    private Consumer<Reads> recipientsJoin(RolBindingRef rb, Objecttype bron, Objecttype doel, String naam) {
        RegelSpraakModel.Rol[] pair = FactModelGenerator.boundRolePair(model, rb);
        if (pair == null)
            throw new Refusal("de connectingRole van de verdeling noemt geen gedeclareerde rol (" + rb.feittype() + ", "
                    + rb.rol() + ")");
        if (rb.feittype() != null && createdRelations.contains(Namen.norm(rb.feittype()))) {
            LinkRelation relation = linkRelation(rb, pair[0], "de verdeling '" + naam + "'");
            return member -> member.addToRangePlan(linkExists(relation));
        }
        String recipientRole = pair[0].naam(), sourceRole = pair[1].naam();
        boolean forward = !model.naarVeelRol(bron, doel, recipientRole);
        if (forward && model.naarVeelRol(doel, bron, sourceRole))
            throw new Refusal("een verdeling over de enkelvoudige ontvangerrol '" + recipientRole + "' wiens zusterrol '"
                    + sourceRole + "' meervoudig is: dat feittype heeft één beanveld voor beide richtingen, zoals een vouw"
                    + " over zo'n rol weigert (§13.3.9 opslagrichting)");
        Objecttype carrier = forward ? bron : doel, another = forward ? doel : bron;
        String rol = forward ? recipientRole : sourceRole;
        if (!FactModelGenerator.hasReferenceField(model, carrier, rol, another))
            throw new Refusal("de join van de verdeling heeft geen beanveld — "
                    + FactModelGenerator.noJoinFieldReason(model, carrier, rol, another) + " (§13.3.9 opslagrichting)");
        // forward the source's reference field names the recipient, else the recipient's names the source
        Binding.Field veld = new Binding.Field(beanClass(carrier), rol);
        return member -> member.subjectJoin(forward ? new Join(id("verdelingsbereik"), null, FOLD_SUBJECT, veld)
                : new Join(id("verdelingsbereik"), rol, FOLD_SUBJECT));
    }

    // ---------------- object and fact creation ----------------

    /** One member initialisation of an object creation: the member on the created type, and its value on the subject. */
    private record Initialisation(Member member, Term waarde) {}

    /**
     * An object creation (RUNTIME-SEMANTICS ch. 5.5, IR-SPEC ch. 3.3), a rule of the agenda group {@link #OBJECTCREATIE}:
     * the subject pattern with the reads of the condition and of the member values bound as a rule's are, the
     * {@link CreationPhase} the driver opened, no {@link CreationStamp} of this creation for the subject, no {@link CreationStamp} that
     * makes the subject itself in this phase, and the condition as the test; and a consequence that makes a new bean of
     * the created type and inserts, stated, the bean, its {@link CreationStamp}, the {@link RelationLink} of the connecting
     * relation and a {@link HasCharacteristic} for a characteristic initialisation whose value is true
     * ({@link Consequence.Creation}). An attribute initialisation whose value is gevuld is set on the new bean before it is
     * inserted, as part of its construction ({@link Consequence.Construction}): it is the input of the made object, as the
     * reference interpreter reads it, so a writer of the member displaces it as it displaces case input, and an
     * initialisation rule on the member finds the target filled. Each value is computed from the values the rule binds
     * when it fires. The driver fires this group only at a fixpoint of the rule layer, so the condition and the
     * values are those of that fixpoint; and nothing the creation made lapses, as in the reference interpreter, which
     * keeps a minted instance under a fixed label. One object per creation per subject, then. The object is a bean of
     * the created type, so every pattern over that type sees it, and nothing is written on a bean that was there before:
     * the link toward it is the {@link RelationLink}, which the readers of the relation join over.
     *
     * <p>The 1:1 shape guards on the input world, as ch. 5.5 says: a subject whose reference field toward the created
     * type is filled in the case input creates nothing; this branch writes no bean while it fires, so the field is the
     * input. The decision of the link roles is the shared one ({@link FactModelGenerator#creationLink}): the
     * creation's {@code connectingRole} binding, or without one the derivation from the type pair that the DRL-text
     * branch takes as well. The relation of a derived pair is {@link #typePairRelation}, and either way the link is the
     * same {@link RelationLink}, put on both ends at the write-back. Refused by name besides: an ambiguous back-link, which
     * ch. 5.5 makes a fault of the program; a derived pair whose roles the decision reaches only through a supertype of
     * either end, whose relation the strata of this branch do not see written; a connecting relation that is not one of
     * two roles; a characteristic initialisation whose value is no truth value (WF-027); and
     * a division in a member value, whose §6.5 deposit this branch does not make. Where the condition can fault, its
     * fault rule is an ordinary rule of the rule layer, without the marks and without the 1:1 guard: the reference
     * interpreter evaluates the condition first, in every pass.
     */
    private List<RulePlan> objectCreation(ResolvedObjectCreatie oc) {
        if (oc.naam() == null) throw new Refusal("een objectcreatie zonder naam (WF-107)");
        Objecttype fresh = oc.nieuwType(), subject = oc.subject();
        if (fresh == null || subject == null) throw new Refusal("een objectcreatie zonder opgelost type");
        RolBindingRef vr = roleBindings.get(oc);
        FactModelGenerator.CreationLink k = FactModelGenerator.creationLink(model, fresh, subject, vr, oc.naam());
        if (k.ambiguous() != null)
            throw new Refusal(k.ambiguous() + "; " + STILL_NOT);
        if (k.refusal() != null) throw new Refusal(k.refusal());
        RegelSpraakModel.Feittype ft = vr != null ? feittype(vr.feittype()) : typePairRelation(subject, fresh, k);
        if (vr == null && ft == null)
            throw new Refusal("een objectcreatie zonder connectingRole-binding waarvan de terugkoppeling uit het typenpaar"
                    + " ('" + k.backRole() + "' en '" + k.fwdRole() + "') via een supertype van het onderwerp of van het"
                    + " gemaakte type loopt: de strata van deze tak leiden de relatie van de terugkoppeling af uit de exacte"
                    + " typen en zien haar dan niet geschreven: " + STILL_NOT);
        if (ft == null || ft.rollen.size() != 2 || ft.wederkerig)
            throw new Refusal("een objectcreatie over de relatie '" + (vr != null ? vr.feittype() : ft.naam)
                    + "', die geen relatie van twee rollen is: " + STILL_NOT);
        String relation = Namen.norm(ft.naam);
        boolean newIsFirst = Namen.norm(ft.rollen.get(0).naam()).equals(Namen.norm(k.fwdRole()));
        String naam = oc.naam();
        String ruleName = model.geëmitteerdeRegelNaam(naam);
        Supplier<Reads> nieuwe = () -> new Reads(subject).valid(oc.geldigheid());
        Reads lez = nieuwe.get();
        Set<Integer> read = lez.follow();
        Term testTerm = oc.voorwaarde() == null ? null : voorwaarde(oc.voorwaarde(), lez);
        boolean canFail = lez.canFail();
        lez.stop();
        Set<Integer> use = lez.follow();
        List<Initialisation> inits = new ArrayList<>();
        for (ResolvedObjectCreatie.Toekenning t : oc.toekenningen()) {
            if (t.attribuut() == null || t.waarde() == null)
                throw new Refusal("een ledeninitialisatie zonder lid of waarde");
            // a characteristic is true or false (WF-027): the reference interpreter faults on a value of another type,
            // and the consequence below would read it as not true and leave the characteristic unset
            Datatype characteristicValue = t.kenmerk() ? eff.typeOf(t.waarde()) : null;
            if (characteristicValue != null && !characteristicValue.isBoolean() && !characteristicValue.isOnbekend())
                throw new Refusal("een kenmerkinitialisatie van '" + t.attribuut() + "' met een waarde van type "
                        + characteristicValue.naam() + " ('" + snippet(t.waarde().bron()) + "'): een kenmerk is waar of onwaar"
                        + " (WF-027), en de referentie-interpreter faalt op zo'n waarde");
            String division = division(t.waarde());
            if (division != null)
                throw new Refusal("een deling in een ledeninitialisatie ('" + division + "'), waarvan deze tak de §6.5-"
                        + "deponering niet maakt: " + STILL_NOT);
            String squareRoot = squareRoot(t.waarde());
            if (squareRoot != null)
                throw new Refusal("een wortel in een ledeninitialisatie ('" + squareRoot + "'), waarvan deze tak de §6.6-"
                        + "deponering niet maakt: " + STILL_NOT);
            Member member = member(fresh, t.attribuut(), t.kenmerk());
            Term w = waarde(t.waarde(), lez);
            if (!t.kenmerk())
                w = restated(w, eff.unitOf(t.waarde()), member.datatype().eenheidOf(), "een §4 schrijfgrens naar het lid");
            inits.add(new Initialisation(member, w));
        }
        lez.stop();
        int stratum = stratum(oc, naam);
        int phase = lez.creationPhase(stratum);
        use.add(phase);
        int origin = lez.origin();
        use.add(origin);
        List<LhsItem> items = lez.plan(conditionRole(canFail), testTerm, read, oc.voorwaarde(),
                creationGuard(naam, lez, phase));
        // the fault this creation stated at a making for the subject blocks it there: the driver counts every firing of a
        // creation rule as a making, so a creation that fired again on its own fault would never let a stratum end
        items.add(new Not(new FactPattern(Fout.class, "$eigen_fout", List.of(equal(id("eigen-fout"), "regel", naam)),
                List.of(new Join(id("eigen-fout-onderwerp"), "onderwerp", SUBJECT)), List.of())));
        if (k.oneToOne())
            // an instance linked toward the subject in the input world blocks the creation (ch. 5.5)
            lez.addSubjectAlpha(new Alpha(id("geen-gekoppelde-instantie"),
                    List.of(new Alpha.FieldTest(k.fwdRef(), Alpha.Comparator.LEEG, null))));
        // every initialisation is set on the new bean in its construction where it is an attribute, or inserted as a
        // characteristic after the bean where it is one, in the order of the document
        List<Consequence.Initialisation> initialisations = new ArrayList<>();
        List<Consequence.Construction.Setting> settings = new ArrayList<>();
        List<Consequence.Insert> insertions = new ArrayList<>(List.of(new Consequence.Insert.New(Consequence.Insertion.STATED),
                new Consequence.Insert.Mark(Consequence.Insertion.STATED),
                new Consequence.Insert.Link(relation, newIsFirst, Consequence.Insertion.STATED)));
        for (int i = 0; i < inits.size(); i++) {
            Member member = inits.get(i).member();
            initialisations.add(new Consequence.Initialisation(member.sleutel(), inits.get(i).waarde()));
            if (member.kenmerk()) insertions.add(new Consequence.Insert.Characteristic(i, Consequence.Insertion.STATED));
            else settings.add(new Consequence.Construction.Setting(i, new Binding.Field(member.javaClass(), member.naam())));
        }
        // a creation fires on a fixpoint of the rule layer, so a fault in an initialisation is the session's at the
        // making, as in the reference interpreter, which then makes nothing; the fault is stated like everything a
        // creation makes, and the rule's guard on it keeps the creation from firing again in a later generation
        Consequence creatie = new Consequence.Creation(SUBJECT, naam,
                new Consequence.CreationDepth(origin, stratum, subject.naam, CREATION_BUDGET), initialisations,
                new Consequence.FaultName(naam, ruleName), Consequence.Insertion.STATED,
                new Consequence.Construction(beanClass(fresh), settings), phase, insertions, List.copyOf(use),
                lez.size());
        for (Initialisation i : inits) derived.put(i.member().sleutel(), i.member());
        lez.recorder();
        creationRules.add(ruleName);
        creationStrata.add(stratum);
        List<RulePlan> out = new ArrayList<>();
        out.add(lez.rulePlan(ruleName, OBJECTCREATIE, items, creatie));
        if (canFail) out.add(conditionFaultRule(naam, ruleName, nieuwe, oc.voorwaarde(), null));
        RulePlan deposit = depositRule(naam, ruleName, nieuwe, oc.voorwaarde(), null);
        if (deposit != null) out.add(deposit);
        RulePlan withoutDate = validityFault(ruleName, naam, oc.geldigheid());
        if (withoutDate != null) out.add(withoutDate);
        return out;
    }

    /** {@link #typePairRelation(Objecttype, Objecttype, FactModelGenerator.CreationLink)} for the object creation
     *  {@code oc} without a {@code connectingRole} binding, before any form lowers; null where the shared decision does
     *  not link the creation, which {@link #objectCreation} then refuses by name. */
    private RegelSpraakModel.Feittype typePairRelation(ResolvedObjectCreatie oc) {
        Objecttype fresh = oc.nieuwType(), subject = oc.subject();
        if (oc.naam() == null || fresh == null || subject == null) return null;
        FactModelGenerator.CreationLink k = FactModelGenerator.creationLink(model, fresh, subject, null, oc.naam());
        return k.ambiguous() != null || k.refusal() != null ? null : typePairRelation(subject, fresh, k);
    }

    /**
     * The connecting relation of an object creation without a {@code connectingRole} binding (RUNTIME-SEMANTICS ch.
     * 5.5): the fact type of the two roles that the shared decision {@code k} derived from the type pair, the role the
     * made object plays ({@code fwdRole}) filled by the created type and the other ({@code backRole}) by the subject's, and
     * for a subject of the created type a reciprocal fact type of that one role. The fillers are compared exactly, as
     * {@link Cycles} derives the back-link for the strata: where the decision reached a role through a supertype of
     * either end, the strata would not see the creation write the relation, so a creation that reads it could fire
     * before this one has made its links. Null then, and {@link #objectCreation} refuses the creation by name. Two fact
     * types cannot match where the decision linked: its roles are unique among the roles the pair reaches through any
     * supertype, of which the exact ones are a part.
     */
    private RegelSpraakModel.Feittype typePairRelation(Objecttype subject, Objecttype fresh,
                                                        FactModelGenerator.CreationLink k) {
        RegelSpraakModel.Feittype found = null;
        for (RegelSpraakModel.Feittype ft : model.feittypen.values()) {
            List<RegelSpraakModel.Rol> r = ft.rollen;
            boolean fits = r.size() == 2
                    ? fills(r.get(0), k.fwdRole(), fresh) && fills(r.get(1), k.backRole(), subject)
                      || fills(r.get(1), k.fwdRole(), fresh) && fills(r.get(0), k.backRole(), subject)
                    : r.size() == 1 && ft.wederkerig && subject == fresh && fills(r.get(0), k.fwdRole(), fresh);
            if (!fits) continue;
            if (found != null)
                throw new IllegalStateException("exec-model: de terugkoppeling ('" + k.backRole() + "' en '" + k.fwdRole()
                        + "') van een " + subject.naam + " naar een " + fresh.naam + " ligt in twee feittypen, '"
                        + found.naam + "' en '" + ft.naam + "', waar het gedeelde besluit haar rollen uniek vond");
            found = ft;
        }
        return found;
    }

    /** Whether role {@code r} carries the name {@code naam} and is filled by exactly {@code type}. */
    private static boolean fills(RegelSpraakModel.Rol r, String naam, Objecttype type) {
        return naam != null && Namen.norm(r.naam()).equals(Namen.norm(naam))
                && type.naam.equalsIgnoreCase(r.objecttypeNaam());
    }

    /** The guard of an object creation beside its condition: no {@link CreationStamp} of creation {@code naam} for the
     *  subject, so that it makes one object per subject, and no {@link CreationStamp} that made the subject itself in the
     *  current phase, bound at {@code phase} ({@link Reads#creationPhase}), so that an object waits for the next phase,
     *  after the rule layer has reached a fixpoint over it. */
    private List<LhsItem> creationGuard(String naam, Reads lez, int phase) {
        String f = lez.variable(phase);
        String own = "$gemaakt_" + id("creatie"), fresh = "$nieuw_" + id("creatie");
        return List.of(
                new Not(new FactPattern(CreationStamp.class, own, List.of(equal(id("gemaakt"), "regel", naam)),
                        List.of(new Join(id("gemaakt-onderwerp"), "onderwerp", SUBJECT)), List.of())),
                new Not(new FactPattern(CreationStamp.class, fresh, List.of(),
                        List.of(new Join(id("nieuw-object"), "object", SUBJECT)),
                        List.of(new BetaTest(id("nieuw-in-deze-fase"), StaticFunction.IN_PHASE, f)), List.of())));
    }

    /** The evaluation stratum of the creation {@code vorm}, named {@code naam}: every creation is a form of the
     *  dependency graph, so a creation without one is a fault of this transpiler. */
    private int stratum(Object vorm, String naam) {
        Integer s = strata.get(vorm);
        if (s == null)
            throw new IllegalStateException("exec-model: de creatie \"" + naam + "\" heeft geen stratum in de"
                    + " afhankelijkheidsgraaf");
        return s;
    }

    /**
     * A fact creation (RUNTIME-SEMANTICS ch. 5.5, IR-SPEC ch. 3.4), a rule of the agenda group {@link #FEITCREATIE}: the
     * anchor pattern with the reads of the guard, the {@link CreationPhase} the driver opened, the guard as the test where
     * it is about the anchor, the path from the anchor to the target as a chain of patterns ({@link #chain}), over the
     * {@link RelationLink} facts of a relation a form creates, none of the current phase, and over the reference fields of
     * an input relation, no link of the created relation between anchor and target yet, and a consequence that inserts
     * that link, stated and carrying the phase. The creation is idempotent per link, as in the reference interpreter,
     * where an already present link is a write that changes nothing: a target that two routes or two creations reach,
     * or that the input links already, gets no second link, and an anchor gets the links of targets that become
     * reachable in a later generation. A guard that is a characteristic check hosted on the target, and is the whole
     * condition, filters the targets instead (ch. 5.5, WF-109); hosted on the target anywhere else it is refused, as the
     * other branches refuse it. A second distinct filler of a to-one role is a fault ({@link #linkRules}).
     */
    private List<RulePlan> factCreation(ResolvedFeitCreatie fc) {
        if (fc.naam() == null) throw new Refusal("een feitcreatie zonder naam (WF-107)");
        Objecttype anchor = fc.anker(), doel = fc.doel();
        if (anchor == null || doel == null || fc.feittype() == null || fc.joinRol() == null || fc.ankerRol() == null)
            throw new Refusal("een feitcreatie zonder opgelost anker, doel, feittype of pad");
        RegelSpraakModel.Feittype ft = feittype(fc.feittype());
        if (ft == null || ft.rollen.size() != 2 || ft.wederkerig)
            throw new Refusal("een feitcreatie van de relatie '" + fc.feittype() + "', die geen relatie van twee rollen"
                    + " is: " + STILL_NOT);
        String relation = Namen.norm(ft.naam);
        boolean anchorIsFirst = Namen.norm(ft.rollen.get(0).naam()).equals(Namen.norm(fc.ankerRol()));
        Conditie c = fc.voorwaarde();
        Conditie.Kenmerk targetCheck = c instanceof Conditie.Kenmerk k && k.eigenaar() != null && !Host.hostOp(k, anchor)
                && Host.hostOp(k, doel) && !navigationBindings.containsKey(k) && k.periode() == null ? k : null;
        if (targetCheck == null && c != null && Host.targetHostOutsideRange(c, doel, navigationBindings::containsKey))
            throw new Refusal(Host.TARGET_ATTEMPT_NESTED);
        String ruleName = model.geëmitteerdeRegelNaam(fc.naam());
        Conditie anchorGuard = targetCheck == null ? c : null;
        Supplier<Reads> fresh = () -> new Reads(anchor).valid(fc.geldigheid());
        Reads lez = fresh.get();
        Set<Integer> read = lez.follow();
        Term testTerm = anchorGuard == null ? null : voorwaarde(anchorGuard, lez);
        boolean canFail = lez.canFail();
        lez.stop();
        int stratum = stratum(fc, fc.naam());
        int phase = lez.creationPhase(stratum);
        List<LhsItem> items = lez.plan(conditionRole(canFail), testTerm, read, anchorGuard);
        List<RolBindingRef> steps = new ArrayList<>();
        for (ResolvedFeitCreatie.Tussenstap t : fc.tussenstappen()) steps.add(t.rolBinding());
        steps.add(fc.joinRol());
        List<Hop> hops = new ArrayList<>();
        Objecttype cur = anchor;
        for (RolBindingRef step : steps) {
            Hop h = step(step, cur, "het pad van de feitcreatie '" + fc.naam() + "'");
            hops.add(h);
            cur = h.naar();
        }
        List<FactPattern> pad = new ArrayList<>(chain(hops, SUBJECT, lez.variable(phase)));
        String targetVar = pad.get(pad.size() - 1).naam();
        if (targetCheck != null) {
            TargetGuard b = targetGuard(targetCheck, targetVar);
            pad.set(pad.size() - 1, withAfterAlpha(pad.get(pad.size() - 1), b.afterAlpha()));
            items.addAll(pad);
            if (b.state() != null) items.add(b.state());
        } else {
            items.addAll(pad);
        }
        String anchorEnd = anchorIsFirst ? "eerste" : "tweede", targetEnd = anchorIsFirst ? "tweede" : "eerste";
        items.add(new Not(new FactPattern(RelationLink.class, "$bestaand_" + id("feitcreatie"),
                List.of(equal(id("bestaand"), "relation", relation)),
                List.of(new Join(id("bestaand-anker"), anchorEnd, SUBJECT),
                        new Join(id("bestaand-doel"), targetEnd, targetVar)),
                List.of())));
        Consequence relationLink = new Consequence.RelationLink(relation, anchorIsFirst ? SUBJECT : targetVar,
                anchorIsFirst ? targetVar : SUBJECT, phase, Consequence.Insertion.STATED);
        lez.recorder();
        creationRules.add(ruleName);
        creationStrata.add(stratum);
        List<RulePlan> out = new ArrayList<>();
        out.add(lez.rulePlan(ruleName, FEITCREATIE, items, relationLink));
        if (canFail) out.add(conditionFaultRule(fc.naam(), ruleName, fresh, anchorGuard, null));
        RulePlan deposit = depositRule(fc.naam(), ruleName, fresh, anchorGuard, null);
        if (deposit != null) out.add(deposit);
        RulePlan withoutDate = validityFault(ruleName, fc.naam(), fc.geldigheid());
        if (withoutDate != null) out.add(withoutDate);
        return out;
    }

    /** A step of a fact creation's path over {@code step} from {@code van}: over a relation a form creates by its
     *  {@link RelationLink} facts, else by the reference field of the to-one end, on the near bean where the far role is
     *  to-one and on the far bean where it is to-many. Two to-many roles have no field in the bean model (§13.3.9). */
    private Hop step(RolBindingRef step, Objecttype van, String description) {
        RegelSpraakModel.Rol[] pair = step == null ? null : FactModelGenerator.boundRolePair(model, step);
        if (pair == null || pair[0] == pair[1])
            throw new Refusal(description + ": een stap die geen rolpaar van twee rollen noemt");
        Objecttype naar = model.objecttypeOpNaam(pair[0].objecttypeNaam());
        if (naar == null) throw new Refusal(description + ": de rol '" + pair[0].naam() + "' heeft geen gedeclareerde vuller");
        Hop created = linkHop(step, van, naar);
        if (created != null) return created;
        if (!pair[0].meervoudig()) {
            if (!FactModelGenerator.hasReferenceField(model, van, pair[0].naam(), naar))
                throw new Refusal(description + ": " + FactModelGenerator.noReferenceFieldReason(model, van, pair[0].naam(), naar)
                        + " (§13.3.9 opslagrichting)");
            return new Hop(van, naar, pair[0].naam(), true, null, false);
        }
        if (!pair[1].meervoudig()) {
            if (!FactModelGenerator.hasReferenceField(model, naar, pair[1].naam(), van))
                throw new Refusal(description + ": " + FactModelGenerator.noReferenceFieldReason(model, naar, pair[1].naam(), van)
                        + " (§13.3.9 opslagrichting)");
            return new Hop(van, naar, pair[1].naam(), false, null, false);
        }
        throw new Refusal(description + ": de stap over '" + step.feittype() + "' heeft twee rollen naar veel, waarvoor het"
                + " beanmodel geen veld heeft (§13.3.9 opslagrichting)");
    }

    /** The guard of a fact creation on each target ({@code check}, hosted on a type the target satisfies), for the
     *  target the variable {@code doel} binds at the end of the creation's path: the target must be of the host's type,
     *  and its characteristic must hold as the check asks, read through its standing where a form writes it and off the
     *  bean otherwise, through the getter of the class that declares it. The constraints on the target come after the
     *  one that reaches it ({@link FactPattern#afterAlpha}), as do those on the standing after its join. */
    private TargetGuard targetGuard(Conditie.Kenmerk check, String doel) {
        Objecttype host = check.eigenaar();
        Member member = member(host, check.kenmerk(), true);
        Class<?> hostClass = beanClass(host);
        boolean desired = !check.negatie();
        Alpha hostConstraint = new Alpha(id("doel-gastheer"),
                List.of(new Alpha.FieldTest(null, Alpha.Comparator.IS_A, hostClass)));
        ElementSleutel sleutel = member.sleutel();
        if (writers.containsKey(sleutel)) {
            String st = "$doelstand_" + id("doel");
            FactPattern state = new FactPattern(CharacteristicState.class, st, List.of(equal(id("doelstand"), "kenmerk", sleutel)),
                    List.of(new Join(id("doelstand-doel"), "onderwerp", doel)), List.of(),
                    List.of(equal(id("doelstand-waar"), "holds", desired)), List.of());
            read.putIfAbsent(sleutel, member);
            return new TargetGuard(List.of(hostConstraint), state);
        }
        Alpha kenmerk = new Alpha(id("doel-kenmerk"), List.of(new Alpha.FieldTest(List.of(), member.naam(),
                Binding.ReadForm.WAAR, Alpha.Comparator.GELIJK, desired, member.javaClass())));
        return new TargetGuard(List.of(hostConstraint, kenmerk), null);
    }

    /** The guard of a fact creation on its target ({@link #targetGuard}): the constraints on the target's pattern after
     *  the one that reaches it, and the pattern of the target's characteristic standing, null where the guard reads the
     *  characteristic off the bean. */
    private record TargetGuard(List<Alpha> afterAlpha, FactPattern state) {}

    /** {@code p} with the constraints {@code afterAlpha} after its joins and its other constraints. */
    private static FactPattern withAfterAlpha(FactPattern p, List<Alpha> afterAlpha) {
        List<Alpha> all = new ArrayList<>(p.afterAlpha());
        all.addAll(afterAlpha);
        return new FactPattern(p.factClass(), p.naam(), p.alpha(), p.joins(), p.betaTests(), all, p.bindings());
    }

    /** {@code p} binding {@code b} as well, after its other bindings. */
    private static FactPattern withBinding(FactPattern p, Binding b) {
        List<Binding> all = new ArrayList<>(p.bindings());
        all.add(b);
        return new FactPattern(p.factClass(), p.naam(), p.alpha(), p.joins(), p.betaTests(), p.afterAlpha(), all);
    }

    /**
     * The rules every relation a form creates needs, whether a form reads it or not: a rule per to-one end with a
     * reference field that inserts the links the case input carries as {@link RelationLink} facts, so that the readers of
     * the relation see the input and the created links alike, and a rule per to-one role that faults a second distinct
     * filler of it: one object filling the other role with two links to different objects, the fault of ch. 5.5,
     * never an overwrite.
     */
    private List<RulePlan> linkRules(String relation) {
        RegelSpraakModel.Feittype ft = feittype(relation);
        if (ft == null || ft.rollen.size() != 2 || ft.wederkerig) return List.of();
        RegelSpraakModel.Rol r0 = ft.rollen.get(0), r1 = ft.rollen.get(1);
        Objecttype t0 = model.objecttypeOpNaam(r0.objecttypeNaam()), t1 = model.objecttypeOpNaam(r1.objecttypeNaam());
        if (t0 == null || t1 == null) return List.of();
        List<RulePlan> out = new ArrayList<>();
        if (!r1.meervoudig() && FactModelGenerator.hasReferenceField(model, t0, r1.naam(), t1))
            out.add(inputLink(relation, t0, r1.naam(), true));
        if (!r0.meervoudig() && FactModelGenerator.hasReferenceField(model, t1, r0.naam(), t0))
            out.add(inputLink(relation, t1, r0.naam(), false));
        if (!r1.meervoudig()) out.add(secondFilling(ft, relation, true, r1.naam(), t0));
        if (!r0.meervoudig()) out.add(secondFilling(ft, relation, false, r0.naam(), t1));
        return out;
    }

    /** The links of {@code relation} the reference field {@code veld} of the beans of {@code houder} carries, the holder
     *  being the link's first end where {@code houderIsFirst}: a rule of no form, whose subject is the holder and whose
     *  link has phase 0, the input's. */
    private RulePlan inputLink(String relation, Objecttype houder, String veld, boolean houderIsFirst) {
        Class<?> javaClass = beanClass(houder);
        Subject h = new Subject(javaClass, "$houder", List.of(new Alpha(id("koppeling-invoer"),
                List.of(new Alpha.FieldTest(veld, Alpha.Comparator.GEVULD, null)))), List.of(),
                List.of(Binding.van("$ander", javaClass, veld)));
        return new RulePlan("koppeling " + relation + " uit de invoer: " + houder.naam + "." + veld, null, null,
                new ReadTable(List.of()), List.of(h), new Consequence.RelationLink(relation, houderIsFirst ? "$houder" : "$ander",
                houderIsFirst ? "$ander" : "$houder", -1, Consequence.Insertion.LOGICAL));
    }

    /** The fault of two links of {@code relation} from one object to two different fillers of the to-one role
     *  {@code rol}: the near object is the links' first end where {@code nearIsFirst}. */
    private RulePlan secondFilling(RegelSpraakModel.Feittype ft, String relation, boolean nearIsFirst, String rol,
                                    Objecttype nearType) {
        String nearEnd = nearIsFirst ? "eerste" : "tweede", farEnd = nearIsFirst ? "tweede" : "eerste";
        String sentence = "de naar-één-rol \"" + rol + "\" van een " + nearType.naam + " heeft twee verschillende vullingen;"
                + " een tweede vulling is een fout, geen overschrijving (RUNTIME-SEMANTICS ch. 5.5)";
        FactPattern one = new FactPattern(RelationLink.class, "$een", List.of(equal(id("vulling"), "relation", relation)), List.of(),
                List.of(Binding.van("$nabij", RelationLink.class, nearEnd)));
        FactPattern two = new FactPattern(RelationLink.class, "$twee", List.of(equal(id("tweede-vulling"), "relation", relation)),
                List.of(new Join(id("tweede-vulling-nabij"), nearEnd, "$een",
                        new Binding.Field(RelationLink.class, nearEnd))),
                List.of(new BetaTest(id("tweede-vulling-ander"), StaticFunction.OTHER_FILLING, "$een",
                        List.of(constant(farEnd)))),
                List.of());
        return new RulePlan("koppeling " + relation + ": tweede vulling van " + rol, null, null,
                new ReadTable(List.of()), List.of(one, two),
                new Consequence.SecondFilling("$nabij", ft.naam, sentence, Consequence.Insertion.LOGICAL));
    }

    // ---------------- prioritized defaults ----------------

    /** The name of an effective stack, as the destructive lowering named the rule of the group: the kind-discriminated
     *  target on the stack's owner, so two types that share one inherited target name two stacks. */
    private static String stackName(Stapels.Stack st) {
        PrioritizedDefaults l0 = st.members().get(0);
        return "defaults " + (l0.isKenmerk() ? "K:" : "A:") + st.eigenaar().naam + "." + l0.doel();
    }

    /** The member a stack decides, as declared: on the type of its owner's chain that declares it, in that type's
     *  spelling, the way the model names it ({@link RegelSpraakModel#benoembaar}). */
    private static RegelSpraakModel.Benoembaar stackTarget(Stapels.Stack st) {
        PrioritizedDefaults l0 = st.members().get(0);
        for (Objecttype t : st.eigenaar().keten()) {
            if (l0.isKenmerk()) {
                for (String k : t.kenmerken)
                    if (Namen.norm(k).equals(Namen.norm(l0.doel()))) return RegelSpraakModel.Benoembaar.kenmerk(t.naam, k);
            } else {
                for (String a : t.attributen.keySet())
                    if (Namen.norm(a).equals(Namen.norm(l0.doel()))) return RegelSpraakModel.Benoembaar.attribuut(t.naam, a);
            }
        }
        throw new IllegalStateException("de stapel " + stackName(st) + " beslist een lid dat de keten van "
                + st.eigenaar().naam + " niet declareert");
    }

    /**
     * One default of a stack where the lowering places it: the level it is read on, its chain and its position there,
     * a label for the names of its rules, the key of a conflict among its exception groups (RUNTIME-SEMANTICS ch. 7.9:
     * the target form and the default indices from the root, member indices left out), and, where it has exception
     * groups, their chains, numbered consecutively from {@code firstMember}.
     */
    private record Node(Default d, Objecttype levelOf, int chain, int positie, String label, String sleutel,
                         List<List<Node>> members, int firstMember, Geldigheid validity) {
        boolean hasMembers() { return members != null; }
    }

    /** What the rules of one stack share while they are built. */
    private static final class StackBuild {
        final String naam;
        final Member member;
        final boolean kenmerk;
        /** The bean class of the stack's owner: the stack runs on instances of exactly that type (ch. 7.10). */
        final Class<?> javaClass;
        /** The bare ch. 7.2 conflict key, the target on the type that declares it. */
        final String targetForm;
        final List<Reads> lezingen = new ArrayList<>();
        final Set<String> controlFacts = new LinkedHashSet<>();
        int nextChain;

        StackBuild(String naam, Member member, boolean kenmerk, Class<?> javaClass, String targetForm, int rootChains) {
            this.naam = naam;
            this.member = member;
            this.kenmerk = kenmerk;
            this.javaClass = javaClass;
            this.targetForm = targetForm;
            this.nextChain = rootChains;
        }
    }

    /**
     * The rules of one effective stack (RUNTIME-SEMANTICS ch. 7.1-7.3, 7.8-7.10), each default lowered on its own
     * level and on instances of exactly the stack's owner, deciding by logical control facts ({@link ControlFact}) where
     * the destructive lowering walked the stack in one consequence. One chain without exception groups
     * takes the simple form ({@link #simpleForm}); exception groups, or more than one group at the root, the
     * general form ({@link #generalForm}). A deciding default writes the target: a {@link Value} with the stack as the
     * writer, which replaces the input on an attribute, or a {@link HasCharacteristic} for an attribution, beside which the
     * input on a characteristic stands; a conflict writes nothing and deposits the §9.5 inconsistency under its key.
     *
     * <p>Refused by name, as the destructive lowering refused them: a characteristic target that shares its name with
     * an attribute, a group with a dated validity, an initialisation default on a characteristic, a deciding default
     * without a justification; a target on a timeline or with dimensions through {@link #member}. Refused besides: a
     * division anywhere in a default ({@link #division(Conditie, Expr)}).
     */
    private List<RulePlan> stackRules(Stapels.Stack st) {
        PrioritizedDefaults l0 = st.members().get(0);
        Objecttype eigenaar = st.eigenaar();
        boolean kenmerk = l0.isKenmerk();
        String doel = l0.doel();
        if (kenmerk && eigenaar.attribuut(doel) != null)
            throw new Refusal("een kenmerkdoel met de naam van een attribuut ('" + doel + "' van " + eigenaar.naam
                    + "): de bean draagt één veld per naam en het attribuut gaat voor, zodat het kenmerk niet te schrijven"
                    + " en niet te onderscheiden is");
        Member member = member(eigenaar, doel, kenmerk);
        Objecttype declarant = eigenaar.declarantVan(doel);
        StackBuild b = new StackBuild(stackName(st), member, kenmerk, beanClass(eigenaar),
                (declarant != null ? declarant.naam : l0.onderwerp()) + "." + doel, st.members().size());
        List<List<Node>> squareRoot = new ArrayList<>();
        // a group with a dated validity gates its own chain, the exception groups below its defaults included: where
        // the window does not admit the date the group decides nothing (RUNTIME-SEMANTICS ch. 8.2)
        for (int m = 0; m < st.members().size(); m++)
            squareRoot.add(chain(b, st.members().get(m).defaults(), m, null, eigenaar, List.of(),
                    st.members().size() == 1 ? "" : m + ":", st.members().get(m).geldigheid()));
        boolean general = squareRoot.size() > 1 || squareRoot.stream().flatMap(List::stream).anyMatch(Node::hasMembers);
        List<RulePlan> rules = new ArrayList<>(general ? generalForm(b, squareRoot) : simpleForm(b, squareRoot.get(0)));
        if (st.members().stream().anyMatch(l -> l.geldigheid() != null && !l.geldigheid().altijd()))
            rules.add(stackValidityFault(b));
        // registered only once every default of the stack lowered, as a single rule registers its reads
        for (Reads lez : b.lezingen) lez.recorder();
        derived.put(member.sleutel(), member);
        defaultsGroups.add(new DefaultsGroup(b.naam, general, rules.size(), List.copyOf(b.controlFacts)));
        return rules;
    }

    /** The rule that faults every object of the stack's owner, of exactly its type, in a session without an evaluation
     *  date, where a group of the stack has a dated validity: the reference interpreter asks the validity of a group when
     *  it evaluates the stack on an object, and faults there (RUNTIME-SEMANTICS ch. 8.1), so an owner without objects
     *  faults nothing. */
    private RulePlan stackValidityFault(StackBuild b) {
        evaluationDateNeeded();
        String naam = b.naam;
        Alpha concrete = concreteType(b.javaClass);
        Alpha withoutDate = new Alpha(id("geen-rekendatum"),
                List.of(new Alpha.FieldTest("date", Alpha.Comparator.LEEG, null)));
        return new RulePlan(naam + ": geen rekendatum", vorm, null, new ReadTable(List.of()),
                List.of(new Subject(b.javaClass, SUBJECT, List.of(concrete), List.of(), List.of()),
                        new FactPattern(EvaluationDate.class, "$rekendatum", List.of(withoutDate), List.of(), List.of())),
                new Consequence.Fout(SUBJECT, Rekenfout.VALIDITY_WITHOUT_DATE, new Consequence.FaultName(naam, naam),
                        Consequence.Insertion.LOGICAL));
    }

    /** The constraint that keeps a rule of a stack on instances of exactly the stack's owner, the bean class
     *  {@code klasse}: the stack runs on instances of exactly that type (RUNTIME-SEMANTICS ch. 7.10,
     *  {@link StackBuild#javaClass}). */
    private Alpha concreteType(Class<?> javaClass) {
        return new Alpha(id("concreet-type"), List.of(new Alpha.FieldTest(null, Alpha.Comparator.CLASS_IS, javaClass)));
    }

    /** The knots of one chain, {@code id}, with its defaults' levels: a root default's own ({@link Stapels#levelOf}), an
     *  exception group's defaults on the level of the default that carries them ({@code drager}); every knot carries
     *  the validity of the group at the root of its chain. */
    private List<Node> chain(StackBuild b, List<Default> ds, int id, Objecttype carrier, Objecttype eigenaar,
                              List<Integer> pad, String prefix, Geldigheid validity) {
        List<Node> out = new ArrayList<>();
        for (int i = 0; i < ds.size(); i++) {
            Default d = ds.get(i);
            Objecttype levelOf = carrier != null ? carrier : stapels.levelOf(d, eigenaar);
            controleer(b, d);
            List<Integer> ownPath = new ArrayList<>(pad);
            ownPath.add(i);
            String label = prefix + i;
            StringBuilder sleutel = new StringBuilder(b.targetForm);
            for (int p : ownPath) sleutel.append('/').append(p);
            List<List<Node>> members = null;
            int eerste = -1;
            if (d.excepties() != null) {
                List<dev.uchar.regelspraak.ir.ExceptionGroup> groups = d.excepties().leden();
                eerste = b.nextChain;
                b.nextChain += groups.size();
                members = new ArrayList<>();
                for (int m = 0; m < groups.size(); m++)
                    members.add(chain(b, groups.get(m).defaults(), eerste + m, levelOf, eigenaar, ownPath,
                            label + "." + m + ".", validity));
            }
            out.add(new Node(d, levelOf, id, i, label, sleutel.toString(), members, eerste, validity));
        }
        return out;
    }

    /** The refusals per default. */
    private void controleer(StackBuild b, Default d) {
        if (d.initialisatie() && b.kenmerk)
            throw new Refusal("een initialisatiedefault op een kenmerkdoel ('" + b.member.naam() + "' van "
                    + b.member.declarant().naam + "): een kenmerk kent geen initialisatie (WF-075)");
        if (!d.initialisatie() && d.voorwaarde() == null && d.heeftEigenInhoud())
            throw new Refusal("een beslissende default zonder rechtvaardiging ('" + (d.bron() != null ? d.bron()
                    : "zonder bron") + "', doel '" + b.member.naam() + "')");
        String division = division(d.voorwaarde(), d.consequentie());
        if (division != null)
            throw new Refusal("een deling in een default ('" + division + "'): deze tak rekent een default uit zodra wat"
                    + " hij leest staat, ook een die een hogere default verdringt; een deling door nul daar is een fout op"
                    + " een tussenstand, die deze tak intrekt zodra de hogere default beslist: of zo'n fout telt, wacht op"
                    + " een normbesluit (RUNTIME-SEMANTICS ch. 5.2, 7.1)");
        String squareRoot = squareRoot(d.voorwaarde(), d.consequentie());
        if (squareRoot != null)
            throw new Refusal("een wortel in een default ('" + squareRoot + "'), waarvan deze tak de §6.6-deponering niet"
                    + " maakt: een default deponeert alleen waar hij beslist (RUNTIME-SEMANTICS ch. 7.1)");
    }

    /** The snippet of the first division in {@code voorwaarde} or {@code waarde}, or null: in the justification or the
     *  consequence of a default, and in the condition or the value of an initialisation beside another writer, which
     *  ch. 7.7 reads as the lowest default ({@link #initialisationRule}). The destructive lowering refused a division
     *  below the top of a consequence, whose deposit would persist; here the evaluation itself is speculative, as a
     *  default is evaluated whenever its reads stand. A division by zero in a default another one displaces is a fault
     *  on an intermediate state, which this branch would withdraw ({@link Fout}); whether such a fault counts waits on
     *  a decision of the norm, so every division is refused until then. Since none passes, the rules of a default raise
     *  no division fault ({@link #ownRule}). The walk enters every expression and condition the forms carry, a
     *  {@code Let}'s value and body, both branches of an {@code IfThenElse} and its condition, a rounding, a clamp's
     *  bounds and a fold's argument, fallback and filter among them, so a division under one of them is found too. */
    private static String division(Conditie voorwaarde, Expr waarde) {
        return eerste(voorwaarde, waarde, TmsTranspiler::isDivision);
    }

    private static String division(Expr e) {
        return eerste(null, e, TmsTranspiler::isDivision);
    }

    /** The snippet of the first square root in {@code waarde}, or null: a place of the §6.6 deposit, which a member
     *  initialisation and a default do not make. */
    private static String squareRoot(Expr e) {
        return eerste(null, e, TmsTranspiler::isSquareRoot);
    }

    private static String squareRoot(Conditie voorwaarde, Expr waarde) {
        return eerste(voorwaarde, waarde, TmsTranspiler::isSquareRoot);
    }

    private static boolean isDivision(Expr e) {
        return e instanceof Expr.Binair b && (b.op() == Expr.BinOp.GEDEELD_DOOR || b.op() == Expr.BinOp.GEDEELD_DOOR_ABS);
    }

    private static boolean isSquareRoot(Expr e) {
        return e instanceof Expr.Functie f && f.kind() == Expr.FunctieKind.WORTEL;
    }

    /** The snippet of the first expression node for which {@code soort} holds, in {@code voorwaarde} and then in
     *  {@code waarde} (either may be null), each walked node before its operands, or null. */
    private static String eerste(Conditie voorwaarde, Expr waarde, java.util.function.Predicate<Expr> soort) {
        String[] found = {null};
        java.util.function.Predicate<Expr> touch = x -> {
            if (!soort.test(x)) return false;
            found[0] = snippet(x.bron());
            return true;
        };
        if (Conditie.bevatOperand(voorwaarde, touch) || Expr.bevat(waarde, touch)) return found[0];
        return null;
    }

    /** Whether {@code d} decides anything itself: a decision on a characteristic, a consequence on an attribute. */
    private static boolean decidesSelf(StackBuild b, Default d) {
        return b.kenmerk ? d.besluit() != null : d.consequentie() != null;
    }

    /**
     * The simple form, variant M of A10 in the trial of card drools-logische-afleiding: one rule per default that
     * decides anything itself, with its reads bound, its justification as the test and {@code not Beslist} over every
     * higher index; the consequence computes the value from what the rule bound and, where it decides, inserts the
     * write and, where a lower default has a rule, {@link Decides}. A leeg consequence inserts nothing, so the lower
     * defaults decide (ch. 7.1); the initialisation default matches only where the target is empty on the bean, which
     * holds the input (ch. 7.3). A do-not-attribute decision with no attribution below it has nothing to displace and
     * gets no rule.
     */
    private List<RulePlan> simpleForm(StackBuild b, List<Node> chain) {
        List<Node> deciding = new ArrayList<>(chain.stream().filter(k -> decidesSelf(b, k.d())).toList());
        while (b.kenmerk && !deciding.isEmpty()
                && Boolean.FALSE.equals(deciding.get(deciding.size() - 1).d().besluit()))
            deciding.remove(deciding.size() - 1);
        String naam = b.naam;
        ElementSleutel sleutel = b.member.sleutel();
        List<RulePlan> out = new ArrayList<>();
        for (int j = 0; j < deciding.size(); j++) {
            Node k = deciding.get(j);
            int i = k.positie();
            boolean higher = j > 0, lower = j < deciding.size() - 1;
            if (lower) b.controlFacts.add("Beslist");
            // the target: an attribute's value, or the attribution of a characteristic, whose decision the default
            // declares; a do-not-attribute decision writes nothing and only displaces the defaults below it
            List<Consequence.StackFact> facts = new ArrayList<>();
            if (!b.kenmerk) facts.add(new Consequence.StackFact.Value(sleutel, naam));
            else if (Boolean.TRUE.equals(k.d().besluit())) facts.add(new Consequence.StackFact.Characteristic(sleutel));
            if (lower) facts.add(new Consequence.StackFact.Decides(naam, i));
            out.addAll(ownRule(b, k, naam + ": default " + k.label(),
                    () -> higher ? List.of(noDecides(naam, i)) : List.of(), facts));
        }
        return out;
    }

    /**
     * The general form: an {@link Outcome} per default that settles. A default without exception groups settles by
     * its own decision; a default with them reconciles the outcomes of their chains ({@link #reconciliation}) and, with
     * content of its own, falls back to its own decision where no member settles. Every rule of a default carries
     * {@code not Uitkomst} over the positions above it in its chain and above each default whose exception group its
     * chain is ({@link #chainRules}), so a lower default is displaced with whatever it would settle, a conflict
     * included (ch. 7.9), and its exception groups with it. At the root the groups of the stack reconcile once more, by
     * the same rules, under the bare key, and that reconciliation writes the target or deposits the conflict.
     */
    private List<RulePlan> generalForm(StackBuild b, List<List<Node>> squareRoot) {
        List<RulePlan> out = new ArrayList<>();
        for (List<Node> chain : squareRoot) chainRules(b, chain, List::of, out);
        ElementSleutel sleutel = b.member.sleutel();
        // at the root the reconciliation writes the target and deposits a conflict as the §9.5 inconsistency; on a
        // characteristic only an attribution reaches the write (onlyTrue), so the attribution is planned as such
        Consequence.StackFact doel = b.kenmerk ? new Consequence.StackFact.Characteristic(sleutel)
                : new Consequence.StackFact.Value(sleutel, b.naam);
        reconciliation(b, 0, squareRoot.size(), squareRoot.stream().anyMatch(TmsTranspiler::canConflict), b.targetForm, "wortel",
                List::of, new Consequence.StackFact.Inconsistency(), doel, b.kenmerk, false, out);
        return out;
    }

    /**
     * The rules of {@code keten} and, below each of its defaults, of their member chains. Every rule of a default waits
     * on the positions above it in its own chain and on {@code ancestors}, the positions above each default whose
     * exception group the chain is, at any depth: the reference interpreter reaches the exception groups of a default
     * only where no higher default in its chain decides ({@code decide}), so a group below a displaced default is not
     * evaluated, settles nothing and raises no fault (RUNTIME-SEMANTICS ch. 7.9; PLAN §9, 2026-10-09).
     */
    private void chainRules(StackBuild b, List<Node> chain, Supplier<List<LhsItem>> ancestors, List<RulePlan> out) {
        String naam = b.naam;
        for (Node k : chain) {
            int id = k.chain(), i = k.positie();
            Supplier<List<LhsItem>> notHigher = () -> {
                List<LhsItem> guard = new ArrayList<>(ancestors.get());
                if (i > 0) guard.add(noOutcome(naam, id, i));
                return guard;
            };
            Consequence.StackFact decides = new Consequence.StackFact.Outcome(naam, id, i, Outcome.Kind.DECIDED);
            if (!k.hasMembers()) {
                if (!decidesSelf(b, k.d())) continue;
                b.controlFacts.add("Uitkomst " + Outcome.Kind.DECIDED);
                out.addAll(ownRule(b, k, naam + ": default " + k.label(), notHigher, List.of(decides)));
                continue;
            }
            for (List<Node> member : k.members()) chainRules(b, member, notHigher, out);
            int eerste = k.firstMember(), to = eerste + k.members().size();
            reconciliation(b, eerste, k.members().size(), k.members().stream().anyMatch(TmsTranspiler::canConflict),
                    k.sleutel(), k.label(), notHigher,
                    new Consequence.StackFact.Outcome(naam, id, i, Outcome.Kind.CONFLICT), decides, false, true, out);
            if (decidesSelf(b, k.d()))
                out.addAll(ownRule(b, k, naam + ": terugval " + k.label(), () -> {
                    List<LhsItem> guard = new ArrayList<>();
                    guard.add(new Not(outcomes(naam, eerste, to, null)));
                    guard.addAll(notHigher.get());
                    return guard;
                }, List.of(decides)));
        }
    }

    /** Whether a chain can end in a conflict: some default in it reconciles two or more members, or a member that can. */
    private static boolean canConflict(List<Node> chain) {
        for (Node k : chain)
            if (k.hasMembers() && (k.members().size() > 1 || k.members().stream().anyMatch(TmsTranspiler::canConflict)))
                return true;
        return false;
    }

    /**
     * The reconciliation of RUNTIME-SEMANTICS ch. 7.2, applied at every level by ch. 7.9, over the outcomes of the
     * member chains {@code [eerste, eerste + aantal)}: the first conflict
     * in member order passes on unchanged; else the first deciding member's decision, which is a conflict under
     * {@code sleutel} where another member decides differently (numbers by value), and the decision otherwise. A rule
     * per outcome, each under {@code wacht}; the rules that cannot match are left out (a conflict among members that
     * cannot conflict, a difference among one member). {@code onlyTrue} restricts the decision to an attribution, the
     * one decision on a characteristic that writes; {@code between} says the outcomes are control facts, not the write.
     */
    private void reconciliation(StackBuild b, int eerste, int aantal, boolean membersConflict, String sleutel, String label,
                            Supplier<List<LhsItem>> guard, Consequence.StackFact atConflict,
                            Consequence.StackFact atDecision, boolean onlyTrue, boolean between, List<RulePlan> out) {
        String naam = b.naam;
        int to = eerste + aantal;
        Binding ofSubject = Binding.van(SUBJECT, Outcome.class, "onderwerp");
        if (membersConflict) {
            List<LhsItem> items = new ArrayList<>();
            items.add(new FactPattern(Outcome.class, "$conflict",
                    List.of(outcomeOfMember("lid-conflict", naam, Outcome.Kind.CONFLICT, eerste, to, false)), List.of(),
                    List.of(ofSubject, Binding.van("$sleutel", Outcome.class, "sleutel"))));
            if (aantal > 1) items.add(new Not(outcomes(naam, eerste, to, Outcome.Kind.CONFLICT,
                    "eerder-lid", StaticFunction.EARLIER_MEMBER, "$conflict")));
            items.addAll(guard.get());
            out.add(new RulePlan(naam + ": doorgeven " + label, vorm, null,
                    new ReadTable(List.of(new dev.uchar.regelspraak.transpiler.tms.plan.Read(0,
                            ReadKind.CONFLICT_KEY, "conflictsleutel", "$sleutel"))), items,
                    new Consequence.Conflict(SUBJECT, new Term.SlotValue(0, null), List.of(0), 1, atConflict,
                            Consequence.Insertion.LOGICAL)));
            if (between) b.controlFacts.add("Uitkomst " + Outcome.Kind.CONFLICT);
        }
        if (aantal > 1) {
            List<LhsItem> items = new ArrayList<>();
            items.add(new FactPattern(Outcome.class, "$besluit",
                    List.of(outcomeOfMember("lid-besluit", naam, Outcome.Kind.DECIDED, eerste, to, false)), List.of(),
                    List.of(ofSubject)));
            items.add(new Not(outcomes(naam, eerste, to, Outcome.Kind.DECIDED, "eerder-lid",
                    StaticFunction.EARLIER_MEMBER, "$besluit")));
            if (membersConflict) items.add(new Not(outcomes(naam, eerste, to, Outcome.Kind.CONFLICT)));
            FactPattern another = outcomes(naam, eerste, to, Outcome.Kind.DECIDED, "ander-besluit", StaticFunction.OTHER_DECISION,
                    "$besluit");
            items.add(new Exists(List.of(another), Exists.verklaard(List.of(another)), Exists.read(List.of(another))));
            items.addAll(guard.get());
            out.add(new RulePlan(naam + ": verschil " + label, vorm, null, new ReadTable(List.of()), items,
                    new Consequence.Conflict(SUBJECT, constant(sleutel), List.of(), 0, atConflict,
                            Consequence.Insertion.LOGICAL)));
            if (between) b.controlFacts.add("Uitkomst " + Outcome.Kind.CONFLICT);
        }
        List<LhsItem> items = new ArrayList<>();
        items.add(new FactPattern(Outcome.class, "$besluit",
                List.of(outcomeOfMember("lid-besluit", naam, Outcome.Kind.DECIDED, eerste, to, onlyTrue)), List.of(),
                List.of(ofSubject, Binding.van("$waarde", Outcome.class, "waarde"))));
        if (aantal > 1) {
            items.add(new Not(outcomes(naam, eerste, to, Outcome.Kind.DECIDED, "eerder-lid", StaticFunction.EARLIER_MEMBER,
                    "$besluit")));
            if (membersConflict) items.add(new Not(outcomes(naam, eerste, to, Outcome.Kind.CONFLICT)));
            items.add(new Not(outcomes(naam, eerste, to, Outcome.Kind.DECIDED, "ander-besluit",
                    StaticFunction.OTHER_DECISION, "$besluit")));
        }
        items.addAll(guard.get());
        out.add(new RulePlan(naam + ": eens " + label, vorm, null,
                new ReadTable(List.of(new dev.uchar.regelspraak.transpiler.tms.plan.Read(0, ReadKind.DECISION,
                        "besluit", "$waarde"))), items,
                new Consequence.Decision(SUBJECT, new Term.SlotValue(0, null), List.of(0), 1, false, null, List.of(atDecision),
                        Consequence.Insertion.LOGICAL)));
        if (between) b.controlFacts.add("Uitkomst " + Outcome.Kind.DECIDED);
    }

    /** The constraint {@code soort} on the outcome a reconciliation binds: of stack {@code naam}, settled as
     *  {@code uitkomst}, of a member chain in {@code [eerste, tot)}, and where {@code onlyTrue} an attribution, the one
     *  decision on a characteristic that writes. */
    private Alpha outcomeOfMember(String soort, String naam, Outcome.Kind outcome, int eerste, int to,
                                boolean onlyTrue) {
        List<Alpha.FieldTest> tests = new ArrayList<>(List.of(
                new Alpha.FieldTest("stack", Alpha.Comparator.GELIJK, naam),
                new Alpha.FieldTest("soort", Alpha.Comparator.GELIJK, outcome),
                new Alpha.FieldTest("chain", Alpha.Comparator.AT_LEAST, eerste),
                new Alpha.FieldTest("chain", Alpha.Comparator.UNDER, to)));
        if (onlyTrue) tests.add(new Alpha.FieldTest("waarde", Alpha.Comparator.GELIJK, Boolean.TRUE));
        return new Alpha(id(soort), tests);
    }

    /** The outcomes of the chains {@code [van, tot)} of stack {@code naam} for the subject, of {@code soort}, or of
     *  either where it is null, under a variable of their own. */
    private FactPattern outcomes(String naam, int van, int to, Outcome.Kind soort) {
        return outcomes(naam, van, to, soort, null, null, null);
    }

    /** As {@link #outcomes(String, int, int, Outcome.Kind)}, with the constraint {@code betaKind}, which calls
     *  {@code function}, between the outcome and the one the variable {@code doel} binds, where it is not null. */
    private FactPattern outcomes(String naam, int van, int to, Outcome.Kind soort, String betaKind, StaticFunction function,
                               String doel) {
        String variable = "$uitkomst_" + idSeq;
        List<Alpha.FieldTest> tests = new ArrayList<>(List.of(
                new Alpha.FieldTest("stack", Alpha.Comparator.GELIJK, naam),
                new Alpha.FieldTest("chain", Alpha.Comparator.AT_LEAST, van),
                new Alpha.FieldTest("chain", Alpha.Comparator.UNDER, to)));
        if (soort != null) tests.add(new Alpha.FieldTest("soort", Alpha.Comparator.GELIJK, soort));
        Alpha alpha = new Alpha(id("uitkomst"), tests);
        Join onSubject = new Join(id("uitkomst-onderwerp"), "onderwerp", SUBJECT);
        List<BetaTest> beta = betaKind == null ? List.of() : List.of(new BetaTest(id(betaKind), function, doel));
        return new FactPattern(Outcome.class, variable, List.of(alpha), List.of(onSubject), beta, List.of());
    }

    /** {@code not Uitkomst} over the positions above {@code positie} in chain {@code keten}, for the subject. */
    private LhsItem noOutcome(String naam, int chain, int positie) {
        String variable = "$hoger_" + idSeq;
        Alpha higher = new Alpha(id("hoger-uitkomst"), List.of(
                new Alpha.FieldTest("stack", Alpha.Comparator.GELIJK, naam),
                new Alpha.FieldTest("chain", Alpha.Comparator.GELIJK, chain),
                new Alpha.FieldTest("positie", Alpha.Comparator.UNDER, positie)));
        return new Not(new FactPattern(Outcome.class, variable, List.of(higher),
                List.of(new Join(id("hoger-uitkomst-onderwerp"), "onderwerp", SUBJECT)), List.of()));
    }

    /** {@code not Beslist} over the indices above {@code index} in the stack, for the subject. */
    private LhsItem noDecides(String naam, int index) {
        String variable = "$hoger_" + idSeq;
        Alpha higher = new Alpha(id("hoger-beslist"), List.of(
                new Alpha.FieldTest("stack", Alpha.Comparator.GELIJK, naam),
                new Alpha.FieldTest("index", Alpha.Comparator.UNDER, index)));
        return new Not(new FactPattern(Decides.class, variable, List.of(higher),
                List.of(new Join(id("hoger-beslist-onderwerp"), "onderwerp", SUBJECT)), List.of()));
    }

    /**
     * The rule of a default's own decision: the subject pattern on the default's level, restricted to the stack's
     * owner, with the input test of an initialisation default; the reads of its justification and its consequence bound
     * as any rule binds them; {@code wacht}, the control-fact patterns, before the test of the justification; and a
     * consequence that hands the decision to {@code besluit}: a characteristic's decision as declared, an attribute's
     * value computed from what the rule bound, and nothing where that value is leeg (ch. 7.1). The validity of the group
     * at the root of the default's chain gates the rule on the evaluation date ({@link Reads#valid}).
     *
     * <p>A default carries no division and no square root ({@link #controleer}), but its justification or its
     * consequence can still fault, a date step out of range or a context value without an evaluation date
     * ({@link Rekenfout}). Such a fault is a {@link Fout} under the stack's name, as in a rule: the consequence inserts
     * it in place of the decision, and where the justification can fault, a fault rule with the rule's own left-hand
     * side, its control-fact patterns included, inserts it ({@link #conditionFaultRule}). A default a higher one
     * displaces loses its match, and so does every default of an exception group below it ({@link #chainRules}), and
     * the fault it raised goes with it, so a fault counts only where its default is evaluated at the fixpoint (PLAN §9,
     * 2026-10-09).
     */
    private List<RulePlan> ownRule(StackBuild b, Node k, String ruleName, Supplier<List<LhsItem>> guard,
                                       List<Consequence.StackFact> facts) {
        Default d = k.d();
        Supplier<Reads> fresh = () -> {
            Reads l = new Reads(k.levelOf());
            l.addSubjectAlpha(concreteType(b.javaClass));
            // the input on the target, read through the getter of the type that declares it, as the bean holds it
            if (d.initialisatie())
                l.addSubjectAlpha(new Alpha(id("doel-leeg-in-de-invoer"), List.of(new Alpha.FieldTest(List.of(),
                        b.member.naam(), Binding.ReadForm.VALUE, Alpha.Comparator.LEEG, null, b.member.javaClass()))));
            return l.valid(k.validity());
        };
        Reads lez = fresh.get();
        Set<Integer> read = lez.follow();
        Term testTerm = d.voorwaarde() == null ? null : voorwaarde(d.voorwaarde(), lez);
        boolean canFail = lez.canFail();
        lez.stop();
        String naam = b.naam;
        List<LhsItem> items;
        Consequence decision;
        if (b.kenmerk) {
            items = lez.plan(conditionRole(canFail), testTerm, read, d.voorwaarde(), guard.get());
            decision = new Consequence.Decision(SUBJECT, constant(d.besluit()), List.of(), lez.size(), false, null, facts,
                    Consequence.Insertion.LOGICAL);
        } else {
            Set<Integer> use = lez.follow();
            Term waarde = restated(waarde(d.consequentie(), lez), eff.unitOf(d.consequentie()),
                    b.member.datatype().eenheidOf(), "een §4 schrijfgrens naar het doel");
            lez.stop();
            items = lez.plan(conditionRole(canFail), testTerm, read, d.voorwaarde(), guard.get());
            decision = new Consequence.Decision(SUBJECT, waarde, List.copyOf(use), lez.size(), true,
                    new Consequence.FaultName(naam, naam), facts, Consequence.Insertion.LOGICAL);
        }
        b.lezingen.add(lez);
        List<RulePlan> out = new ArrayList<>();
        out.add(lez.rulePlan(ruleName, items, decision));
        if (canFail) out.add(conditionFaultRule(naam, ruleName, fresh, d.voorwaarde(), l -> guard.get()));
        return out;
    }

    // ---------------- values ----------------

    /** A value as the analysis plans it ({@link Term}): a tree of calls over the values the rule binds, indexed as
     *  {@link Reads} numbers them, which {@link ClosureRendering} renders into the function a consequence or a test
     *  calls. Every expression node is named here, so a node the IR records gain later fails to compile until it is
     *  decided whether this branch computes it. */
    private Term waarde(Expr e, Reads lez) {
        return switch (e) {
            case Expr.Literal l -> literal(l);
            case Expr.AttribuutRef a -> new Term.SlotValue(attributeRead(a, lez), bron(a.bron()));
            case Expr.ParameterRef p -> new Term.SlotValue(lez.parameter(p.parameter()), bron(p.bron()));
            case Expr.Binair b -> binary(b, lez);
            case Expr.TekstReeks t -> textComposition(t, lez);
            case Expr.Functie f -> function(f, lez);
            // IR-SPEC ch. 4.3: a union denotes instances and stands only as the argument of a count, which reads it in
            // vouw; here it stands where a value is read, which the verifier refuses (WF-110 (a)) and the reference
            // interpreter faults on; both branches refuse it in this sentence, which the destructive DRL-text lowering
            // gave until card drl-teksttak-op-truth-maintenance
            case Expr.InstanceSetUnion u -> throw notComputed("InstanceSetUnion", u.bron(), "zij duidt de instanties van"
                    + " een §5.7-concatenatie aan, en alleen een instantietelling verbruikt ze (WF-110 (a))");
            case Expr.Afgerond a -> rounding(a, lez);
            case Expr.Begrensd b -> clamp(b, lez);
            case Expr.DimensieCel c -> dimensionCell(c, lez);
            case Expr.VariabeleRef v -> throw notComputed("VariableReference", v.bron(), "het merkteken van een"
                    + " variabele die de frontend niet bond, dat de lader weigert (WF-034), zodat het deze tak alleen in een"
                    + " met de hand gebouwd model bereikt");
            case Expr.Let l -> let(l, lez);
            case Expr.BinderRef b -> binderRead(b, lez);
            case Expr.IfThenElse i -> keuze(i, lez);
            case Expr.Constante c -> contextValue(c, lez);
            // RUNTIME-SEMANTICS ch. 5.7 and WF-044: the marker fails the rule's evaluation where the evaluation reaches it,
            // and only there, so a marker in a branch that is not taken plays
            case Expr.Onopgelost o -> {
                lez.markFail();
                yield new Term.Fail(Rekenfout.UNRESOLVED, bron(o.bron()));
            }
        };
    }

    /** A constant of the analysis, which an argument of a function of the plan takes ({@link Term.Const}). */
    private static Term.Const constant(Object waarde) {
        return new Term.Const(waarde, null, null);
    }

    /** A test a form composes itself ({@link TestItem.Role#VORM}) of the checks {@code leden}: their conjunction,
     *  read in order up to the first that does not hold ({@link Term.Compound} under all). No node of the IR
     *  stands for it, so it carries no source fragment. */
    private static Term all(List<Term> members) {
        return new Term.Compound(Conditie.Kwantor.ALLE, 0, members, null);
    }

    /** The check that the value bound at slot {@code slot} is gevuld, in a test a form composes itself. */
    private static Term filled(int slot) {
        return new Term.Call(StaticFunction.EMPTY_TEST, List.of(new Term.SlotValue(slot, null), constant(false)), null);
    }

    /** The check that the value bound at slot {@code slot} is ({@code desired}) or is not the asOfDate-bucket scalar
     *  of a timeline ({@link BucketScalar}), in a test a form composes itself. */
    private static Term emmerscalair(int slot, boolean desired) {
        return new Term.Call(StaticFunction.BUCKET_SCALAR, List.of(new Term.SlotValue(slot, null), constant(desired)), null);
    }

    /** The source fragment of an IR node, which a term carries; null where the IR has none. */
    private static String bron(dev.uchar.regelspraak.ir.Bron bron) {
        return bron == null ? null : bron.snippet();
    }

    private static Refusal notComputed(String vorm, dev.uchar.regelspraak.ir.Bron bron, String reden) {
        return new Refusal("een " + vorm + " in een waarde ('" + snippet(bron) + "'): " + reden);
    }

    private static String snippet(dev.uchar.regelspraak.ir.Bron bron) {
        return bron == null || bron.snippet() == null ? "zonder bron" : bron.snippet();
    }

    private static String snippet(Conditie c) {
        return snippet(c.bron());
    }

    /** A literal: a number, and a percentage, a number in the unit %, exact where its
     *  quotient does not terminate, a date, a boolean, and a text or an enumeration label as its string. */
    private static Term literal(Expr.Literal l) {
        Datatype t = l.type();
        String bron = bron(l.bron());
        if (t instanceof Datatype.Numeriek || t instanceof Datatype.Percentage) {
            BigDecimal[] r = Rationaal.van(l.waarde());
            if (r == null) throw new Refusal("een getal dat niet te lezen is ('" + l.waarde() + "')");
            Breuk b = Breuk.van(r[0]).delen(Breuk.van(r[1]));
            Object v = b.isTerminerend() ? b.naarDecimal() : b;
            return new Term.Const(v, t, bron);
        }
        if (t instanceof Datatype.Booleaan) {
            Boolean v = Literalen.waar(l.waarde());
            return new Term.Const(v, t, bron);
        }
        if (t instanceof Datatype.DatumInDagen) {
            LocalDate v = Literalen.parseDatum(l.waarde());
            if (v == null) throw new Refusal("een datum die niet te lezen is ('" + l.waarde() + "')");
            return new Term.Const(v, t, bron);
        }
        if (t instanceof Datatype.DatumTijd) {
            LocalDateTime v = Literalen.parseDatumTijd(l.waarde());
            if (v == null) throw new Refusal("een datum-tijd die niet te lezen is ('" + l.waarde() + "')");
            return new Term.Const(v, t, bron);
        }
        String label = Literalen.tekstOfEnumLabel(l);
        if (label == null) throw new Refusal("een Literal van type " + t + ": " + STILL_NOT);
        return new Term.Const(label, t, bron);
    }

    /** The arithmetic operators on numbers, a percentage being a number in the unit % (RUNTIME-SEMANTICS ch. 2.2, 2.6),
     *  through the runtime's exact helpers and with their leeg tables (ch. 1.2); a date plus or minus a duration through
     *  {@link #dateCalculation}, a power through {@link #power}. A sum or a difference restates its right operand into the
     *  left one's unit (ch. 2.6); a product, a quotient and a percentage of a value do not. A division faults on a numeric
     *  zero (ch. 1.4, point 4), and one of a filled dividend by a leeg divisor deposits where the evaluation reaches it and
     *  its operands qualify ({@link #depositSite}, {@link DepositFlag}). The operands are evaluated left before right
     *  (ch. 5.11). */
    private Term binary(Expr.Binair b, Reads lez) {
        Datatype lt = eff.typeOf(b.links()), rt = eff.typeOf(b.rechts());
        if (lt.isDatum() || rt.isDatum()) return dateCalculation(b, lez);
        Expr.BinOp op = b.op();
        if (op == Expr.BinOp.TOT_DE_MACHT) return power(b, lez);
        boolean division = op == Expr.BinOp.GEDEELD_DOOR || op == Expr.BinOp.GEDEELD_DOOR_ABS;
        // a condition that holds a division carries the rule that inserts its fault (voorwaardefoutregel)
        if (division) lez.markFail();
        Term l = waarde(b.links(), lez), right = waarde(b.rechts(), lez);
        Term r = op == Expr.BinOp.PLUS || op == Expr.BinOp.MIN || op == Expr.BinOp.VERMINDERD_MET
                ? restated(right, eff.unitOf(b.rechts()), eff.unitOf(b.links()), "een additieve operandgrens")
                : right;
        int depositFlag = division && depositSite(b) ? lez.depositFlag() : -1;
        String bron = bron(b.bron());
        return switch (op) {
            case PLUS -> new Term.Call(StaticFunction.RS_PLUS, List.of(l, r), bron);
            case MIN -> new Term.Call(StaticFunction.RS_MIN, List.of(l, r), bron);
            case VERMINDERD_MET -> new Term.Call(StaticFunction.RS_REDUCED, List.of(l, r), bron);
            case MAAL -> new Term.Call(StaticFunction.RS_TIMES, List.of(l, r), bron);
            case PERCENTAGE_VAN -> new Term.Call(StaticFunction.RS_PERCENTAGE_OF, List.of(l, r), bron);
            case GEDEELD_DOOR -> new Term.Division(StaticFunction.DIVIDED, l, r, depositFlag, bron);
            case GEDEELD_DOOR_ABS -> new Term.Division(StaticFunction.DIVIDED_ABS, l, r, depositFlag, bron);
            case TOT_DE_MACHT -> throw new IllegalStateException("een macht gaat door macht()");
        };
    }

    /** A power with a whole exponent of at least 0 written as a literal, exact (RUNTIME-SEMANTICS ch. 2.2), a leeg base
     *  leeg (ch. 1.2), through the runtime's {@code rsMacht}. Any other exponent is refused: a
     *  negative or fractional literal, or a value read while the session fires, reaches the real power, which ch. 2.1
     *  leaves OPEN, where the reference interpreter answers an exact root or faults and the destructive lowering
     *  computed a binary double. */
    private Term power(Expr.Binair b, Reads lez) {
        Integer n = b.rechts() instanceof Expr.Literal lit && lit.type().isGetalachtig() ? wholeExponent(lit.waarde()) : null;
        if (n == null)
            throw new Refusal("een macht met een exponent die geen geheel getal van ten minste 0 als literal is ('"
                    + snippet(b.bron()) + "'): de reële macht is OPEN (RUNTIME-SEMANTICS ch. 2.1), waar de"
                    + " referentie-interpreter een exacte wortel geeft of faalt");
        Term base = waarde(b.links(), lez);
        return new Term.Call(StaticFunction.RS_POWER, List.of(base, constant(n)), bron(b.bron()));
    }

    /** The whole exponent of at least 0 the lexeme writes, or null. */
    private static Integer wholeExponent(String lexeme) {
        BigDecimal[] r = Rationaal.van(lexeme);
        if (r == null || r[1].signum() == 0) return null;
        try {
            BigDecimal v = r[0].divide(r[1]);
            if (v.signum() < 0 || v.stripTrailingZeros().scale() > 0) return null;
            int n = v.intValueExact();
            return n <= 999_999_999 ? n : null;
        } catch (ArithmeticException noWholeNumber) {
            return null;
        }
    }

    /** A date plus or minus a duration with a calendar or clock unit: no date minus date, no duration minus a date, a
     *  clock unit only on a date-time. A duration counted in a day kind walks the calendar ({@link #dayKindStep}). A
     *  step or a count outside the range of RUNTIME-SEMANTICS ch. 3.8 is an evaluation fault
     *  ({@link ComputeHelpers#dateStep}); the date is evaluated before the duration (ch. 5.11). */
    private Term dateCalculation(Expr.Binair b, Reads lez) {
        Expr.BinOp op = b.op();
        if (op != Expr.BinOp.PLUS && op != Expr.BinOp.MIN && op != Expr.BinOp.VERMINDERD_MET)
            throw new Refusal("een Binary " + Spelling.BINARY_OP.spelling(op) + " met een datum ('" + snippet(b.bron())
                    + "')");
        boolean dateLeft = eff.typeOf(b.links()).isDatum();
        Expr dateExpr = dateLeft ? b.links() : b.rechts();
        Expr durationExpr = dateLeft ? b.rechts() : b.links();
        boolean optellen = op == Expr.BinOp.PLUS;
        if (eff.typeOf(durationExpr).isDatum() || (!optellen && !dateLeft))
            throw new Refusal("een rekensom van twee datums of een duur min een datum ('" + snippet(b.bron()) + "')");
        // ch. 3.8 puts the calendar table first, as Tijdrekening.dagsoortVoorEenheid looks a spelling up for every branch
        String dayKind = Tijdrekening.dagsoortVoorEenheid(model, eff.typeOf(durationExpr).eenheidOf());
        if (dayKind != null) return dayKindStep(b, dateExpr, durationExpr, optellen, dayKind, lez);
        String unit = Tijdrekening.chronoUnit(eff.typeOf(durationExpr));
        if (unit == null) throw new Refusal("een duur zonder tijdseenheid ('" + snippet(b.bron()) + "')");
        boolean clock = switch (unit) { case "HOURS", "MINUTES", "SECONDS", "MILLIS" -> true; default -> false; };
        if (clock && !(eff.typeOf(dateExpr) instanceof Datatype.DatumTijd))
            throw new Refusal("een klokeenheid bij een datum zonder tijd ('" + snippet(b.bron()) + "')");
        lez.markFail();
        Term d = waarde(dateExpr, lez), n = waarde(durationExpr, lez);
        return new Term.Call(StaticFunction.DATE_STEP, List.of(d, n, constant(unit), constant(optellen)),
                bron(b.bron()));
    }

    /**
     * A date or a date-time plus or minus a count of days of the day kind {@code dayKind} (RUNTIME-SEMANTICS ch. 3.8,
     * §6.11), as the runtime walks it for the other branches: from the date, which never counts, one calendar day at a
     * time to the Nth day the definition admits ({@code rsDatumPlusDagsoort}); on a date-time over its date part, the
     * clock time riding through ({@code rsDatumTijdPlusDagsoort}). A count of 0 or leeg gives the date unchanged, a leeg
     * date leeg. The definition reads nothing of working memory (WF-031), so its predicate is a constant of the document
     * ({@link #dayKindPredicate}), and the reads the step depends on are the date and the count, bound as any value of
     * the rule. A walk that passes the reach of ch. 3.8 without a day of the kind is an evaluation fault in the sentence
     * of the reference interpreter ({@link ComputeHelpers#dayKindStep}), a {@link Fout} that counts only where it stands
     * at the fixpoint. The date is evaluated before the count (ch. 5.11).
     */
    private Term dayKindStep(Expr.Binair b, Expr dateExpr, Expr durationExpr, boolean optellen, String dayKind,
                              Reads lez) {
        java.util.function.Predicate<LocalDate> datePredicate =
                dayKindPredicate(dayKind, "een duur in dagsoorten ('" + snippet(b.bron()) + "')");
        boolean dateTime = eff.typeOf(dateExpr) instanceof Datatype.DatumTijd;
        lez.markFail();
        Term d = waarde(dateExpr, lez), n = waarde(durationExpr, lez);
        return new Term.Call(dateTime ? StaticFunction.DATE_TIME_DAY_KIND_STEP : StaticFunction.DAY_KIND_STEP,
                List.of(d, n, new Term.DayKind(dayKind, datePredicate, null), constant(optellen)), bron(b.bron()));
    }

    /**
     * The predicate of the day kind {@code naam} over a calendar day, from its definition (IR-SPEC ch. 3.7), compiled by
     * the backend's day-kind compiler ({@code kern.DayKinds}) the first time a form needs it and the same object for
     * every form after: the bound day, its date parts, the first Easter day, literals and other day kinds on the bound
     * day. The form {@code description} that steps or counts in the day kind is refused by name where the definition does not
     * compile ({@link #dayKindRefusal}); a definition that no form uses is never compiled, so it refuses nothing.
     */
    private java.util.function.Predicate<LocalDate> dayKindPredicate(String naam, String description) {
        java.util.function.Predicate<LocalDate> p = dayKinds.datePredicate(naam);
        if (p == null) throw new Refusal(description + ": " + dayKindRefusal(naam, new LinkedHashSet<>()));
        return p;
    }

    /**
     * Why the day kind {@code naam} has no predicate here, found in its definition and in the definitions it refers to
     * ({@code visited} holds those already looked at, the reference graph being acyclic by WF-085): no definition; a
     * parameter, which WF-031 admits and the reference interpreter reads, and which this branch does not read, since
     * the day-kind compiler binds nothing; a day-kind test on another day than the bound day, which no executor carries
     * (RUNTIME-SEMANTICS ch. 3.8, red case {@code dagsoortverwijzing-andere-dag}); and else a shape of its condition
     * outside what the compiler carries.
     */
    private String dayKindRefusal(String naam, Set<String> visited) {
        ResolvedDagsoort def = null;
        for (ResolvedDagsoort d : model.resolvedDagsoorten)
            if (d.naam() != null && d.naam().equalsIgnoreCase(naam)) def = d;
        if (def == null) return "de dagsoort '" + naam + "' heeft geen definitie (WF-031)";
        if (!visited.add(naam.toLowerCase())) return null;
        String[] parameter = {null};
        Conditie.bevatOperand(def.voorwaarde(), e -> e instanceof Expr.ParameterRef pr
                && (parameter[0] = pr.parameter() == null ? "(onopgelost)" : pr.parameter().naam()) != null);
        if (parameter[0] != null)
            return "de definitie van de dagsoort '" + naam + "' leest de parameter '" + parameter[0] + "', die deze"
                    + " tak in een dagsoortdefinitie niet leest (WF-031 laat haar toe)";
        List<Conditie.Predicaat> tests = new ArrayList<>();
        dayKindTests(def.voorwaarde(), tests);
        for (Conditie.Predicaat t : tests) {
            if (!(t.operand() == null || t.operand() instanceof Expr.Constante c && "de dag".equalsIgnoreCase(c.naam())))
                return "de definitie van de dagsoort '" + naam + "' toetst de dagsoort '" + t.dagsoort() + "' op een andere"
                        + " dag dan de gebonden dag ('" + snippet(t) + "'), een vorm die geen executor uitvoert"
                        + " (RUNTIME-SEMANTICS ch. 3.8)";
            if (t.dagsoort() != null && dayKinds.datePredicate(t.dagsoort()) == null) {
                String nested = dayKindRefusal(t.dagsoort(), visited);
                if (nested != null) return nested;
            }
        }
        return "de definitie van de dagsoort '" + naam + "' heeft een voorwaarde ('" + snippet(def.voorwaarde()) + "')"
                + " buiten wat de dagsoortcompiler van de backend draagt: vergelijkingen van de gebonden dag, zijn"
                + " datumdelen, de eerste paasdag en literalen, en andere dagsoorten op de gebonden dag";
    }

    /** The day-kind tests in {@code c}, in wire order, through its composites. */
    private static void dayKindTests(Conditie c, List<Conditie.Predicaat> out) {
        if (c instanceof Conditie.Predicaat p && p.soort() == Conditie.Predicaatsoort.DAGSOORT) out.add(p);
        else if (c instanceof Conditie.Samengesteld s) for (Conditie d : s.onderdelen()) dayKindTests(d, out);
    }

    /** {@code t} restated from unit {@code van} into unit {@code naar} (RUNTIME-SEMANTICS ch. 2.6): times the exact
     *  canonical factors {@code num ÷ den} as rationals where both are known, so a restatement that does not terminate
     *  stays exact until a collapse point (ch. 2.1), as the reference interpreter restates; times the single ratio of the
     *  two units otherwise, a rate whose factors are not both known; unchanged where no restatement is due, a unitless
     *  side adopting the other's unit. A leeg value stays leeg and a value that is not a number passes through
     *  ({@link Term.Restate}). Two units that do not convert fault while the model is built ({@link UnitConflict}). */
    private static Term restated(Term t, Eenheid van, Eenheid naar, String site) {
        UnitConflict.requireRescalable(van, naar, site);
        if (van == null || naar == null || !van.convertibleNaar(naar) || !van.vereistOmrekening(naar)) return t;
        BigDecimal[] exact = van.exacteHerschaalFactoren(naar);
        if (exact != null)
            return new Term.Restate(StaticFunction.RESTATE_EXACT, t, van, naar, exact[0], exact[1], t.bron());
        return new Term.Restate(StaticFunction.RESTATE, t, van, naar, van.omrekenfactorNaar(naar), null, t.bron());
    }

    // ---------------- the forms of a value beside the arithmetic ----------------

    /** A rounding (RUNTIME-SEMANTICS ch. 2.5) of its base, a leeg base leeg (ch. 1.2), through {@code rsAfronding}, which
     *  rounds an exact rational on its true value; a square root as the base is the root under its mandatory rounding
     *  ({@link #squareRootValue}). */
    private Term rounding(Expr.Afgerond a, Reads lez) {
        java.math.RoundingMode mode = roundingMode(a.modus());
        int decimals = a.decimalen();
        Term base = a.basis() instanceof Expr.Functie f && f.kind() == Expr.FunctieKind.WORTEL
                ? squareRootValue(f, lez) : waarde(a.basis(), lez);
        return new Term.Call(StaticFunction.RS_ROUNDING, List.of(base, constant(decimals), constant(mode)),
                bron(a.bron()));
    }

    private static java.math.RoundingMode roundingMode(Expr.AfrondModus m) {
        return switch (m) {
            case NAAR_BENEDEN -> java.math.RoundingMode.FLOOR;
            case NAAR_BOVEN -> java.math.RoundingMode.CEILING;
            case REKENKUNDIG -> java.math.RoundingMode.HALF_UP;
            case RICHTING_NUL -> java.math.RoundingMode.DOWN;
            case WEG_VAN_NUL -> java.math.RoundingMode.UP;
        };
    }

    /** A square root under its rounding (§13.4.17.13): the runtime's root at 34
     *  digits ({@code rsWortel}), which the rounding above it collapses; a leeg radicand leeg, and a negative one leeg
     *  with the §6.6 deposit where the radicand qualifies and the evaluation reaches it (RUNTIME-SEMANTICS ch. 1.2,
     *  {@link DepositFlag}). */
    private Term squareRootValue(Expr.Functie f, Reads lez) {
        if (f.argumenten().size() != 1)
            throw new Refusal("een squareRoot met " + f.argumenten().size() + " argumenten ('" + snippet(f.bron())
                    + "'): de wortel heeft er één (WF-037)");
        Term radicand = waarde(f.argumenten().get(0), lez);
        int depositFlag = depositSite(f) ? lez.depositFlag() : -1;
        return new Term.SquareRoot(StaticFunction.SQUARE_ROOT, radicand, depositFlag, bron(f.bron()));
    }

    /** A clamp (RUNTIME-SEMANTICS ch. 2.7): its base, then its minimum, then its maximum, in the order of ch. 5.11 and
     *  of the bounds' application, each bound restated into the base's unit (ch. 2.6); a leeg base leeg, an absent or
     *  leeg bound no bound ({@code rsBegrens}). */
    private Term clamp(Expr.Begrensd g, Reads lez) {
        Term base = waarde(g.basis(), lez);
        Eenheid unit = eff.unitOf(g.basis());
        Term minimum = g.minimum() == null ? constant(null)
                : restated(waarde(g.minimum(), lez), eff.unitOf(g.minimum()), unit, "een begrenzing (minimum)");
        Term maximum = g.maximum() == null ? constant(null)
                : restated(waarde(g.maximum(), lez), eff.unitOf(g.maximum()), unit, "een begrenzing (maximum)");
        return new Term.Call(StaticFunction.RS_CLAMP, List.of(base, minimum, maximum), bron(g.bron()));
    }

    /** A text composition (IR-SPEC ch. 4.2): the literal chunks and the inserted parts in part order (RUNTIME-SEMANTICS
     *  ch. 5.11), each as its text ({@link #textPart}). */
    private Term textComposition(Expr.TekstReeks t, Reads lez) {
        List<Term> parts = new ArrayList<>();
        for (Expr.TekstReeks.Deel d : t.delen()) {
            switch (d) {
                case Expr.TekstReeks.Letterlijk l ->
                        parts.add(new Term.Const(l.tekst() == null ? "" : l.tekst(), new Datatype.Tekst(), null));
                case Expr.TekstReeks.Ingevoegd i -> parts.add(textPart(i.expr(), lez, "TextComposition"));
            }
        }
        return new Term.TextComposition(parts, bron(t.bron()));
    }

    /** An inserted part of a text composition or an argument of a concatenation, as its text: a text or an enumeration
     *  label is its own text, and a leeg part reads "" (RUNTIME-SEMANTICS ch. 1.2). A part of another type is refused by
     *  name: the spec states the leeg rule and nothing more, and for the text of a number, a truth value or a date the
     *  reference interpreter (5,50 as "5.5", waar as "waar") and the runtime's {@code rsTekst} (5.50, true) differ. */
    private Term textPart(Expr e, Reads lez, String where) {
        Datatype t = eff.typeOf(e);
        if (!(t instanceof Datatype.Tekst) && !(t instanceof Datatype.Enumeratie))
            throw new Refusal("een tekstdeel van type " + typeName(t) + " in een " + where + " ('" + snippet(e.bron())
                    + "'): RUNTIME-SEMANTICS ch. 1.2 legt alleen vast dat een leeg deel \"\" leest; de tekst van een getal,"
                    + " een waarheidswaarde of een datum beslist de spec niet, en de referentie-interpreter en de"
                    + " runtimehulp rsTekst geven er een verschillende");
        Term f = waarde(e, lez);
        return new Term.Call(StaticFunction.RS_TEXT, List.of(f), f.bron());
    }

    /** A cell of a dimensioned attribute (§3.6): the value at its one label in the attribute's {@code Map<label, value>}
     *  bean member, leeg where the map or the label is absent. The attribute is
     *  input, read off the subject's own bean ({@link #dimensionRead}); a cell over several dimensions is refused by
     *  name, as that branch refuses it. The reference interpreter and the ASP backend do not read a cell. */
    private Term dimensionCell(Expr.DimensieCel c, Reads lez) {
        if (c.labels() == null || c.labels().size() != 1)
            throw new Refusal("een DimensionCellRead over " + (c.labels() == null ? 0 : c.labels().size())
                    + " dimensies ('" + snippet(c.bron()) + "'): deze tak leest alleen een cel van één dimensie");
        int i = dimensionRead(c.attribuut(), lez, "een DimensionCellRead");
        String label = c.labels().get(0);
        return new Term.Call(StaticFunction.CELL, List.of(new Term.SlotValue(i, bron(c.attribuut().bron())), constant(label)),
                bron(c.bron()));
    }

    /** A sum, minimum or maximum over a dimension selection (§5.8.5): the attribute's cells at the selected labels, a
     *  leeg cell skipped and an all-leeg selection leeg, through the runtime's {@code rsDimSom},
     *  {@code rsDimMin} and {@code rsDimMax}; a sum's {@code emptyFallback} is evaluated after the cells, every time
     *  (RUNTIME-SEMANTICS ch. 5.11), and stands in for an all-leeg sum. The reference interpreter and the ASP backend do
     *  not fold a dimension selection. */
    private Term dimensionFold(Expr.Functie f, Reads lez) {
        Expr.FunctieKind kind = f.kind();
        if (kind != Expr.FunctieKind.SOM && kind != Expr.FunctieKind.MINIMUM && kind != Expr.FunctieKind.MAXIMUM)
            throw new Refusal("een Aggregation " + kind + " over een dimensieselectie ('" + snippet(f.bron())
                    + "'): alleen een som, een minimum en een maximum vouwen een dimensieselectie (WF-037)");
        if (f.leegwaarde() != null && kind != Expr.FunctieKind.SOM)
            throw new Refusal("een emptyFallback op een Aggregation " + kind + " ('" + snippet(f.bron()) + "'): alleen"
                    + " een som heeft er een (WF-037)");
        if (f.argumenten().size() != 1 || !(f.argumenten().get(0) instanceof Expr.AttribuutRef a))
            throw new Refusal("een Aggregation over een dimensieselectie over iets anders dan één attribuut ('"
                    + snippet(f.bron()) + "')");
        int i = dimensionRead(a, lez, "een Aggregation over een dimensieselectie");
        List<String> labels = List.copyOf(f.dimensieSelectie().labels());
        Term fallback = f.leegwaarde() == null ? null : restated(waarde(f.leegwaarde(), lez),
                eff.unitOf(f.leegwaarde()), eff.unitOf(a), "een §5.8.2 terugvalwaarde");
        StaticFunction fold = switch (kind) {
            case SOM -> StaticFunction.RS_DIM_SUM;
            case MINIMUM -> StaticFunction.RS_DIM_MIN;
            default -> StaticFunction.RS_DIM_MAX;
        };
        String bron = bron(f.bron());
        Term cells = new Term.Call(fold, List.of(new Term.Call(StaticFunction.MAP,
                List.of(new Term.SlotValue(i, bron(a.bron()))), null), constant(labels)), bron);
        // only a sum has a fallback, evaluated after the cells
        return fallback == null ? cells : new Term.Call(StaticFunction.FALLBACK, List.of(cells, fallback), bron);
    }

    /** The read of the dimensioned attribute {@code a} as its whole map: the subject's own attribute, which no form
     *  writes, bound off its bean. A dimensioned attribute over a path, or one a form writes, is refused by name. */
    private int dimensionRead(Expr.AttribuutRef a, Reads lez, String description) {
        if (a.objecttype() == null || a.attribuut() == null)
            throw new Refusal("een onopgeloste AttributeReference in " + description + " ('" + snippet(a.bron()) + "')");
        if (navigationBindings.containsKey(a))
            throw new Refusal(description + " over een pad ('" + snippet(a.bron()) + "'): de tak leest een gedimensioneerd"
                    + " attribuut alleen op het eigen onderwerp");
        if (a.objecttype() != lez.subject)
            throw new Refusal("een eigen lezing die " + a.objecttype().naam + " noemt op een " + lez.subject.naam
                    + " ('" + snippet(a.bron()) + "', WF-053)");
        Member member = dimensionedMember(lez.subject, a.attribuut());
        if (writers.containsKey(member.sleutel()))
            throw new Refusal(description + " over het attribuut '" + member.naam() + "', dat een vorm schrijft ('"
                    + snippet(a.bron()) + "'): de tak leest een gedimensioneerd attribuut alleen als invoer");
        return lez.own(member);
    }

    /**
     * A {@code Let} (RUNTIME-SEMANTICS ch. 10.6): the body evaluated with the binder bound to its value as a need. The
     * value is evaluated in the enclosing scope (WF-076), at the first read of the binder, and that one result serves
     * every further read (clause 1); a read whose value is leeg ends the {@code Let} with leeg (clause 2), the barrier
     * belonging to the {@code Let} whose binder was read; a body that never reads the binder evaluates nothing of the
     * value, so no leeg, no fault and no deposit come of it (clause 3). The binder is a slot of the value array that no
     * pattern binds ({@link Reads#binder}), which the rendering fills with the need for the body
     * ({@link ClosureRendering}). Every read the value and the body make is bound in the left-hand side all the same, read
     * or not: truth maintenance follows each value the evaluation may need, and a read through a total standing never
     * keeps the rule from matching. The plan marks the {@code Let} eager where the body reads the binder before anything
     * else of it can fault, deposit or end an evaluation ({@link #readsFirst}).
     */
    private Term let(Expr.Let l, Reads lez) {
        if (l.binder() == null) throw new Refusal("een Let zonder binder ('" + snippet(l.bron()) + "')");
        Term waarde = waarde(l.value(), lez);
        int slot = lez.binder();
        String sleutel = Namen.norm(l.binder());
        Integer outside = lez.binders.put(sleutel, slot);
        Term body;
        try {
            body = waarde(l.body(), lez);
        } finally {
            if (outside == null) lez.binders.remove(sleutel);
            else lez.binders.put(sleutel, outside);
        }
        return new Term.Let(slot, l.binder(), waarde, body, Boolean.TRUE.equals(readsFirst(body, slot)),
                bron(l.bron()));
    }

    /**
     * Whether evaluating {@code t} reads the binder in slot {@code slot} before anything of it can fault, deposit or end
     * an evaluation: TRUE where that read comes first; FALSE where such a step comes first, among them the read of
     * another binder, whose need can do all three, or where the read stands in a part that is evaluated only on a
     * condition; null where {@code t} only reads bound values and computes without a fault, so the evaluation goes on
     * to what follows it. A {@code Let} whose body gives TRUE can have its value evaluated before the body with the same
     * outcome, faults and deposits ({@link Term.Let#gretig}).
     */
    private static Boolean readsFirst(Term t, int slot) {
        return switch (t) {
            case Term.Const c -> null;
            case Term.SlotValue l -> null;
            case Term.DayKind d -> null;
            // a read of an input has no effect and cannot fault, so the evaluation goes on to what follows it
            case Term.Invoer i -> readsFirst(i.object(), slot);
            case Term.Binder b -> b.slot() == slot;
            case Term.Call a -> {
                Boolean first = readsFirst(a.arguments(), slot);
                yield first != null ? first : a.function().canFail() ? Boolean.FALSE : null;
            }
            case Term.Restate h -> readsFirst(h.term(), slot);
            case Term.Division d -> {
                Boolean first = readsFirst(List.of(d.left(), d.right()), slot);
                yield first != null ? first : Boolean.FALSE;
            }
            case Term.SquareRoot w -> {
                Boolean first = readsFirst(w.radicand(), slot);
                yield first != null ? first : Boolean.FALSE;
            }
            case Term.Required v -> {
                Boolean first = readsFirst(v.waarde(), slot);
                yield first != null ? first : Boolean.FALSE;
            }
            case Term.TextComposition s -> readsFirst(s.parts(), slot);
            case Term.ListedFold g -> readsFirst(g.operands(), slot);
            // the value of a nested Let is evaluated at the first read of its own binder, which gives FALSE above
            case Term.Let l -> readsFirst(l.body(), slot);
            // the condition comes first and a branch is conditional; what the condition reads is not looked into, so the
            // choice counts as something else first, as before the conditions were planned
            case Term.Choice k -> Boolean.FALSE;
            // a composite is a condition, which stands in a value only as the condition of a choice, answered above; its
            // members after the first are read only where no stop came before them
            case Term.Compound s -> Boolean.FALSE;
            // so are the verdict of a quantifier fold that can fault and deposit, and a condition tested without its
            // fault, which stands only in a test a form composes itself
            case Term.QuantifierRead k -> Boolean.FALSE;
            case Term.WithoutFault z -> Boolean.FALSE;
            // the analysis adds the node only where the fold's filter can deposit or a fault can stand, before the value
            case Term.FoldRead v -> Boolean.FALSE;
            case Term.Fail f -> Boolean.FALSE;
        };
    }

    private static Boolean readsFirst(List<Term> terms, int slot) {
        for (Term t : terms) {
            Boolean first = readsFirst(t, slot);
            if (first != null) return first;
        }
        return null;
    }

    /** A read of the binder of the nearest enclosing {@code Let} of that name (WF-064): it forces the need and raises
     *  the {@code Let}'s barrier where the value is leeg ({@link Term.Binder}). A binder of a {@code Let} around a
     *  fold, read in the fold's filter or matrix, is in scope but bound in another rule than this contribution rule
     *  ({@link Reads#outside}), and refused as a form this branch does not carry yet. */
    private Term binderRead(Expr.BinderRef b, Reads lez) {
        String naam = b.naam() == null ? null : Namen.norm(b.naam());
        Integer slot = naam == null ? null : lez.binders.get(naam);
        if (slot == null && naam != null && lez.outside.contains(naam))
            throw new Refusal("een BinderRef '" + b.naam() + "' van een Let om een vouw, in het filter of de matrix"
                    + " van die vouw ('" + snippet(b.bron()) + "'): de bijdrageregels van de vouw rekenen het filter en"
                    + " de matrix per lid uit en binden de waarde van de Let niet: " + STILL_NOT);
        if (slot == null)
            throw new Refusal("een BinderRef '" + b.naam() + "' zonder omsluitende Let die hem bindt ('"
                    + snippet(b.bron()) + "', WF-064)");
        return new Term.Binder(slot, b.naam(), bron(b.bron()));
    }

    /** An {@code IfThenElse} (RUNTIME-SEMANTICS ch. 10.2): the condition evaluated as any condition of this branch, then
     *  exactly the branch it chooses; the other branch evaluates nothing, so it reads no binder, faults nowhere and
     *  deposits nothing. The reads of both branches are bound in the left-hand side all the same. */
    private Term keuze(Expr.IfThenElse k, Reads lez) {
        Term voorwaarde = voorwaarde(k.conditie(), lez);
        Term then = waarde(k.dan(), lez), otherwise = waarde(k.anders(), lez);
        return new Term.Choice(voorwaarde, then, otherwise, bron(k.bron()));
    }

    /** A context value (RUNTIME-SEMANTICS ch. 8.2): evaluationDate, the session's evaluation date, and evaluationYear,
     *  its calendar year as a whole number without a unit ({@code rsRekenjaar}). Both read the session's
     *  {@link EvaluationDate}, bound in the left-hand side, and fault where the session carries no date, in the reference
     *  interpreter's sentences. The bound day of a day-kind definition, read outside such a definition, faults as in that
     *  executor, since it denotes nothing there; a day-kind definition itself is outside this branch. */
    private Term contextValue(Expr.Constante c, Reads lez) {
        String naam = c.naam() == null ? "" : c.naam();
        String bron = bron(c.bron());
        switch (naam) {
            case "Rekendatum" -> {
                int i = lez.rekendatum();
                lez.markFail();
                return new Term.Required(new Term.SlotValue(i, null), Rekenfout.EVALUATION_DATE_WITHOUT_DATE, bron);
            }
            case "Rekenjaar" -> {
                int i = lez.rekendatum();
                lez.markFail();
                return new Term.Call(StaticFunction.RS_EVALUATION_YEAR, List.of(new Term.Required(new Term.SlotValue(i, null),
                        RegelSpraakRuntime.REKENJAAR_ZONDER_REKENDATUM, null)), bron);
            }
            case "de dag" -> {
                lez.markFail();
                return new Term.Fail(Rekenfout.DAY_OUTSIDE_DAY_KIND, bron);
            }
            default -> throw new Refusal("de contextwaarde '" + naam + "' ('" + snippet(c.bron()) + "'): de lader laat"
                    + " alleen evaluationDate, evaluationYear en de gebonden dag toe (WF-098)");
        }
    }

    // ---------------- aggregations ----------------

    /** A fold this branch lowers: the key its contributions carry ({@link Contribution#fold}), its contribution rules (one
     *  per range, beside the filter fault rule of a filter that can fault and the filter deposit rule of a filter that
     *  can deposit), the reads of those rules, registered when a form that reads the fold lowers
     *  ({@link Reads#recorder}), whether its filter can fault, in which case the rule that reads it binds its
     *  {@link FilterFault} facts as well, and whether its filter can deposit, in which case that rule counts its
     *  {@link FilterDeposit} facts; for a quantifier fold, whether its matrix can fault and whether it can deposit,
     *  in which case its contributions carry a {@link MemberTest} ({@link #quantifierFold}); and for a per-element fold over
     *  an attribute on a timeline, that a member's value can fault, in which case the rule that reads it binds its
     *  {@link MemberFault} facts as well ({@link #memberFaultRule}). */
    private record Fold(String sleutel, List<RulePlan> rules, List<Reads> lezingen, boolean canFail,
                        boolean deposits, boolean memberCanFail, boolean memberDeposits, boolean valueCanFail) {
        /** A fold over a set source or a union: no matrix per member. */
        Fold(String sleutel, List<RulePlan> rules, List<Reads> lezingen, boolean canFail, boolean deposits) {
            this(sleutel, rules, lezingen, canFail, deposits, false, false, false);
        }

        /** A quantifier fold: a matrix per member, and no value per member. */
        Fold(String sleutel, List<RulePlan> rules, List<Reads> lezingen, boolean canFail, boolean deposits,
             boolean memberCanFail, boolean memberDeposits) {
            this(sleutel, rules, lezingen, canFail, deposits, memberCanFail, memberDeposits, false);
        }

        /** Whether the rule that reads the fold replays its contributors in order: a quantifier fold whose matrix can
         *  fault or deposit ({@link MemberTest}). */
        boolean ordered() {
            return memberCanFail || memberDeposits;
        }
    }

    /** The kinds of an aggregation, as against the builtins that share the node. */
    private static boolean isFold(Expr.FunctieKind k) {
        return switch (k) {
            case SOM, TOTAAL, AANTAL, AANTAL_GEVULDE, MINIMUM, MAXIMUM, EERSTE, LAATSTE -> true;
            default -> false;
        };
    }

    /** The listed fold, "de eerste van A en B": two or more operands and no filter, as against a fold over a set
     *  source; told apart by the shape, as RUNTIME-SEMANTICS ch. 1.2e does, since the element type the producer stamps
     *  on it is vestigial. */
    private static boolean isListedFold(Expr.Functie f) {
        boolean ordering = switch (f.kind()) { case MINIMUM, MAXIMUM, EERSTE, LAATSTE -> true; default -> false; };
        return ordering && f.argumenten().size() >= 2 && f.filter() == null;
    }

    /** An aggregation, listed, over a set source or over a dimension selection; every other function node is a builtin
     *  call ({@link #builtin}). */
    private Term function(Expr.Functie f, Reads lez) {
        if (f.dimensieSelectie() != null) return dimensionFold(f, lez);
        if (!isFold(f.kind())) return builtin(f, lez);
        return isListedFold(f) ? listedFold(f, lez) : fold(f, lez);
    }

    /**
     * A builtin call (IR-SPEC ch. 4.3), its arguments evaluated in wire order (RUNTIME-SEMANTICS ch. 5.11), through the
     * runtime's helpers: the absolute value ({@code rsAbs}); a concatenation of
     * texts ({@link #textPart}); the duration and the absolute duration in a calendar unit ({@code rsTijdsduur},
     * ch. 3.8); the year, month and day of a date and the hour, minute, second and millisecond of a date-time
     * ({@code rsDatumDeel}); easterSunday of a whole year ({@link #easterSunday}); a date and a date-time made of their parts
     * ({@link #dateConstruction}). A square root is computed as the base of its mandatory rounding ({@link #rounding}),
     * and without one exactly ({@link #exactSquareRoot}). A bucketing builtin over a timeline is the value of a temporal
     * target and nothing else ({@link #timelineValue}), so here it is refused by name, and a duration in day kinds
     * belongs to the card of day kinds and is refused by name.
     */
    private Term builtin(Expr.Functie f, Reads lez) {
        Expr.FunctieKind kind = f.kind();
        String spelling = Spelling.BUILTIN_KIND.spelling(kind);
        if (kind == Expr.FunctieKind.TIJDSEVENREDIG_DEEL || kind == Expr.FunctieKind.AANTAL_DAGEN_IN)
            throw notComputed("BuiltinCall " + spelling, f.bron(), "deze tak rekent een tijdsevenredig"
                    + " deel en een dagentelling alleen als de tijdlijn van een tijdsafhankelijk doel (RUNTIME-SEMANTICS"
                    + " ch. 3.7, 3.8, 3.11)");
        if (f.filter() != null)
            throw new Refusal("een BuiltinCall " + spelling + " met een filter ('" + snippet(f.bron()) + "'): alleen een"
                    + " bucketing over een tijdlijn draagt er een");
        List<Expr> a = f.argumenten();
        return switch (kind) {
            case WORTEL -> exactSquareRoot(f, lez);
            case ABSOLUTE_WAARDE -> new Term.Call(StaticFunction.RS_ABS, List.of(waarde(oneArgument(f, spelling), lez)),
                    bron(f.bron()));
            case CONCATENATIE -> {
                List<Term> parts = new ArrayList<>();
                for (Expr arg : a) parts.add(textPart(arg, lez, "concatenation"));
                yield new Term.TextComposition(parts, bron(f.bron()));
            }
            case TIJDSDUUR, ABSOLUTE_TIJDSDUUR -> duration(f, lez, spelling);
            case EERSTE_PAASDAG -> easterSunday(f, lez, spelling);
            case JAAR_UIT -> datePart(f, lez, spelling, ChronoField.YEAR, false);
            case MAAND_UIT -> datePart(f, lez, spelling, ChronoField.MONTH_OF_YEAR, false);
            case DAG_UIT -> datePart(f, lez, spelling, ChronoField.DAY_OF_MONTH, false);
            case UUR_UIT -> datePart(f, lez, spelling, ChronoField.HOUR_OF_DAY, true);
            case MINUUT_UIT -> datePart(f, lez, spelling, ChronoField.MINUTE_OF_HOUR, true);
            case SECONDE_UIT -> datePart(f, lez, spelling, ChronoField.SECOND_OF_MINUTE, true);
            case MILLISECONDE_UIT -> datePart(f, lez, spelling, ChronoField.MILLI_OF_SECOND, true);
            case DATUM_MET, DATUMTIJD_MET -> dateConstruction(f, lez, spelling);
            default -> throw new IllegalStateException("de soort " + kind + " is een vouw, geen builtin");
        };
    }

    /** A square root that is not the base of a rounding, exact as the reference interpreter computes it: the root of
     *  a radicand that is the square of a rational, and otherwise the fault that executor gives, since an irrational
     *  root exists only under the rounding §13.4.17.13 mandates (RUNTIME-SEMANTICS ch. 2.2). A leeg radicand is leeg,
     *  a negative one leeg with the §6.6 deposit where the radicand qualifies and the evaluation reaches it (ch. 1.2,
     *  {@link DepositFlag}). The destructive lowering computed a binary approximation here. */
    private Term exactSquareRoot(Expr.Functie f, Reads lez) {
        if (f.argumenten().size() != 1)
            throw new Refusal("een squareRoot met " + f.argumenten().size() + " argumenten ('" + snippet(f.bron())
                    + "'): de wortel heeft er één (WF-037)");
        lez.markFail();
        Term radicand = waarde(f.argumenten().get(0), lez);
        int depositFlag = depositSite(f) ? lez.depositFlag() : -1;
        return new Term.SquareRoot(StaticFunction.EXACT_SQUARE_ROOT, radicand, depositFlag, bron(f.bron()));
    }

    /** The one argument of {@code f}; a call with another number is refused (WF-037). */
    private static Expr oneArgument(Expr.Functie f, String spelling) {
        if (f.argumenten().size() != 1)
            throw new Refusal("een " + spelling + " met " + f.argumenten().size() + " argumenten ('" + snippet(f.bron())
                    + "'): deze builtin heeft er één (WF-037)");
        return f.argumenten().get(0);
    }

    /** The duration and the absolute duration from argument 0 to argument 1 in whole units of the node's own unit
     *  (RUNTIME-SEMANTICS ch. 3.8, WF-038), the "van" evaluated before the "tot" (ch. 5.11); a leeg operand leeg. A unit
     *  that is a day kind counts the days of the kind ({@link #dayKindDuration}). */
    private Term duration(Expr.Functie f, Reads lez, String spelling) {
        List<Expr> a = f.argumenten();
        if (a.size() != 2)
            throw new Refusal("een " + spelling + " met " + a.size() + " argumenten ('" + snippet(f.bron()) + "'): een"
                    + " tijdsduur heeft er twee (WF-037)");
        Datatype van = eff.typeOf(a.get(0)), to = eff.typeOf(a.get(1));
        if (!van.isDatum() || !to.isDatum() || !sameDateKind(van, to))
            throw new Refusal("een " + spelling + " die niet twee datums van één precisie meet ('" + snippet(f.bron())
                    + "', WF-038)");
        Datatype type = eff.typeOf(f);
        // ch. 3.8 puts the calendar table first, as Tijdrekening.dagsoortVoorEenheid looks a spelling up for every branch
        String dayKind = Tijdrekening.dagsoortVoorEenheid(model, type.eenheidOf());
        if (dayKind != null) return dayKindDuration(f, lez, spelling, dayKind, van instanceof Datatype.DatumTijd);
        String unit = Tijdrekening.chronoUnit(type);
        if (unit == null)
            throw new Refusal("een " + spelling + " zonder tijdseenheid ('" + snippet(f.bron()) + "', WF-038)");
        boolean clock = switch (unit) { case "HOURS", "MINUTES", "SECONDS", "MILLIS" -> true; default -> false; };
        if (clock && !(van instanceof Datatype.DatumTijd))
            throw new Refusal("een " + spelling + " in een klokeenheid tussen datums zonder tijd ('" + snippet(f.bron())
                    + "', WF-038)");
        Term from = waarde(a.get(0), lez), upTo = waarde(a.get(1), lez);
        boolean absolute = f.kind() == Expr.FunctieKind.ABSOLUTE_TIJDSDUUR;
        return new Term.Call(StaticFunction.RS_DURATION, List.of(from, upTo, constant(unit), constant(absolute)),
                bron(f.bron()));
    }

    /**
     * The duration from argument 0 to argument 1 in whole days of the day kind {@code dayKind} (RUNTIME-SEMANTICS
     * ch. 3.8, §6.10), as the runtime counts it for the other branches ({@code rsTijdsduurDagsoort}): the days of the
     * kind after the start up to and including the end, signed by the order of the two, the absolute duration without
     * the sign, a leeg operand leeg. The interval bounds the count, so it has no reach and no fault. The predicate is a
     * constant of the document ({@link #dayKindPredicate}). Over two date-times the form is refused by name, as the
     * reference interpreter and the ASP backend refuse it: counting whole days of a kind between two instants needs a
     * rule for the part-day at each end, which ch. 3.8 does not fix, and the refusal does not wait for a filled operand.
     */
    private Term dayKindDuration(Expr.Functie f, Reads lez, String spelling, String dayKind, boolean dateTime) {
        String description = "een " + spelling + " in dagsoorten ('" + snippet(f.bron()) + "')";
        if (dateTime)
            throw new Refusal(description + " tussen twee datum-tijden: de referentie-interpreter en de ASP-backend weigeren haar"
                    + " met naam, omdat het tellen van hele dagen van een soort tussen twee tijdstippen een regel voor de"
                    + " deeldag aan elk eind vraagt, die RUNTIME-SEMANTICS ch. 3.8 niet vastlegt (ch. 3.12)");
        java.util.function.Predicate<LocalDate> datePredicate = dayKindPredicate(dayKind, description);
        Term from = waarde(f.argumenten().get(0), lez), upTo = waarde(f.argumenten().get(1), lez);
        boolean absolute = f.kind() == Expr.FunctieKind.ABSOLUTE_TIJDSDUUR;
        return new Term.Call(StaticFunction.RS_DURATION_DAY_KIND, List.of(from, upTo,
                new Term.DayKind(dayKind, datePredicate, null), constant(absolute)), bron(f.bron()));
    }

    /** A part of a date or a date-time, as a whole number without a unit; a leeg date leeg (RUNTIME-SEMANTICS ch. 1.2,
     *  3.8). A clock part demands a date-time. */
    private Term datePart(Expr.Functie f, Reads lez, String spelling, ChronoField veld, boolean clock) {
        Expr arg = oneArgument(f, spelling);
        Datatype t = eff.typeOf(arg);
        if (!t.isDatum() || (clock && !(t instanceof Datatype.DatumTijd)))
            throw new Refusal("een " + spelling + " uit iets anders dan een " + (clock ? "datum-tijd" : "datum") + " ('"
                    + snippet(f.bron()) + "')");
        Term d = waarde(arg, lez);
        return new Term.Call(StaticFunction.RS_DATE_PART, List.of(d, constant(veld)), bron(f.bron()));
    }

    /** easterSunday of a whole year without a unit (RUNTIME-SEMANTICS ch. 3.8, WF-038), by the algorithm of that chapter
     *  ({@link ComputeHelpers#easterSunday}); a leeg year leeg. A year that is not whole is a fault in the reference interpreter's
     *  sentence, never a silent truncation, and so is a year outside the years this backend's dates hold. */
    private Term easterSunday(Expr.Functie f, Reads lez, String spelling) {
        Expr arg = oneArgument(f, spelling);
        Eenheid unit = eff.unitOf(arg);
        if (unit != null)
            throw new Refusal("een " + spelling + " van een jaar met de eenheid " + unit.weergave() + " ('"
                    + snippet(f.bron()) + "'): het jaar is een geheel getal zonder eenheid (WF-038)");
        lez.markFail();
        Term year = waarde(arg, lez);
        return new Term.Call(StaticFunction.EASTER_SUNDAY, List.of(year), bron(f.bron()));
    }

    /** A date of three parts or a date-time of seven, through the runtime's {@code rsDatumMet} and
     *  {@code rsDatumtijdMet}: every part evaluated in order (RUNTIME-SEMANTICS ch. 5.11), any leeg part leeg (ch.
     *  1.2). A part that is not a whole number within the range of a date part, or parts that name no day, fault: the
     *  reference interpreter makes no such date, and the runtime would truncate the part or throw past the fire
     *  ({@link ComputeHelpers#dateWith}). */
    private Term dateConstruction(Expr.Functie f, Reads lez, String spelling) {
        boolean time = f.kind() == Expr.FunctieKind.DATUMTIJD_MET;
        int aantal = time ? 7 : 3;
        if (f.argumenten().size() != aantal)
            throw new Refusal("een " + spelling + " met " + f.argumenten().size() + " argumenten ('" + snippet(f.bron())
                    + "'): deze builtin heeft er " + aantal + " (WF-037)");
        lez.markFail();
        List<Term> arguments = new ArrayList<>();
        // the spelling first: the sentence of a fault names the builtin
        arguments.add(constant(spelling));
        for (Expr arg : f.argumenten()) arguments.add(waarde(arg, lez));
        return new Term.Call(time ? StaticFunction.DATUMTIJD_MET : StaticFunction.DATUM_MET, arguments, bron(f.bron()));
    }

    /**
     * A listed fold (RUNTIME-SEMANTICS ch. 1.2e): minimum and eerste keep the least operand value, maximum and laatste
     * the greatest; a leeg operand is skipped, an all-leeg fold is leeg, and a tie takes the later operand. The operands
     * are computed from the values the rule binds, in wire order, each after the first restated into the first one's
     * unit, the unit of the result (§4.6 Eenheid1, RUNTIME-SEMANTICS ch. 2.6).
     */
    private Term listedFold(Expr.Functie f, Reads lez) {
        boolean smallest = f.kind() == Expr.FunctieKind.MINIMUM || f.kind() == Expr.FunctieKind.EERSTE;
        Eenheid eerste = eff.unitOf(f.argumenten().get(0));
        List<Term> operands = new ArrayList<>();
        for (int i = 0; i < f.argumenten().size(); i++) {
            Expr a = f.argumenten().get(i);
            Term op = waarde(a, lez);
            operands.add(i == 0 ? op : restated(op, eff.unitOf(a), eerste, "een §5.8.3 operand van een gelijste vouw"));
        }
        return new Term.ListedFold(smallest ? StaticFunction.SMALLEST : StaticFunction.LARGEST, operands, bron(f.bron()));
    }

    /**
     * A fold over a set source (RUNTIME-SEMANTICS ch. 1.2 "Aggregations", ch. 3.4a-d): an accumulate in this rule's
     * left-hand side over the contributions ({@link Contribution}) that the fold's contribution rules insert for this rule's
     * subject, one per member of the range that passes the filter ({@link #contributionRules}). A contribution rule binds the
     * member's value and every read of the filter in its own left-hand side, so a member, or a value derived for it, that
     * changes takes its contribution along, and the accumulate here follows; this rule computes only with what the
     * accumulate binds. A value fold skips a leeg member, which therefore contributes nothing, and a count counts every
     * member. Over no contribution a sum is leeg or its fallback, which is computed whether it is used or not
     * (ch. 5.11), a total 0, both counts 0 and an ordering fold leeg; eerste and laatste are the least and the greatest
     * value. A count over an instance-set union counts the members its selections select together ({@link #unionFold}).
     *
     * <p>A filter that faults for a member is a {@link FilterFault} of the member, no fault: the reference interpreter
     * evaluates the filter only where it evaluates the fold, per member in fold order, and stops at the first fault
     * (RUNTIME-SEMANTICS ch. 5.11). This rule binds the first of those facts in fold order on its subject, and the
     * fold's value faults with that division where it is computed, before the fallback, which the reference
     * interpreter evaluates after the members ({@link #readFold}). The fault is then this rule's, as a division in its
     * own value or condition is: inserted by its consequence or by its fault rule ({@link #conditionFaultRule}), and
     * only where its evaluation reaches the fold. A filter that deposits for a member is a {@link FilterDeposit} of
     * the member in the same way: the reference interpreter evaluates the filter with this rule's collector, so the
     * deposit is this rule's, on its subject, where its evaluation reaches the fold ({@link #readFold}).
     *
     * <p>A total without a range reads the subject's own timeline ({@link #ownTotal}). A sum or a total over a member
     * on a timeline folds twice (RUNTIME-SEMANTICS ch. 3.6, the per-element fold): each member's timeline collapses over
     * time first, at its own granularity, in the contribution rule, which contributes that scalar, and a member without
     * a timeline contributes nothing; the fold over the set is the ordinary one. Every other fold over a member on a
     * timeline is refused by name, as the reference interpreter refuses it: it has no moment at which to rank, count or
     * test the timeline.
     */
    private Term fold(Expr.Functie f, Reads lez) {
        Expr.FunctieKind kind = f.kind();
        String fragment = bron(f.bron());
        if (kind == Expr.FunctieKind.AANTAL && f.argumenten().size() == 1
                && f.argumenten().get(0) instanceof Expr.InstanceSetUnion u) {
            Fold v = unionFold(f, u, lez);
            int[] i = lez.fold(v, null);
            return readFold(v, i, lez, new Term.Call(StaticFunction.COUNT, List.of(new Term.SlotValue(i[0], null)), fragment),
                    fragment);
        }
        Objecttype elem = f.elementObjecttype();
        if (elem == null) throw new Refusal("een Aggregation " + kind + " zonder elementtype ('" + snippet(f.bron()) + "')");
        if (f.argumenten().size() != 1)
            throw new Refusal("een Aggregation " + kind + " over " + f.argumenten().size() + " operanden met een"
                    + " elementtype ('" + snippet(f.bron()) + "'): een vouw over een bereik heeft één argument (WF-037)");
        if (f.leegwaarde() != null && kind != Expr.FunctieKind.SOM)
            throw new Refusal("een emptyFallback op een Aggregation " + kind + " ('" + snippet(f.bron()) + "'): alleen"
                    + " een som heeft er een (WF-037)");
        boolean count = kind == Expr.FunctieKind.AANTAL;
        Expr.AttribuutRef arg = null;
        if (!count) {
            if (!(f.argumenten().get(0) instanceof Expr.AttribuutRef a) || a.attribuut() == null)
                throw new Refusal("een Aggregation " + kind + " over iets anders dan een attribuut van het element ('"
                        + snippet(f.bron()) + "')");
            arg = a;
        }
        boolean withRange = sourcePaths.containsKey(f) || roleBindings.containsKey(f);
        if (kind == Expr.FunctieKind.TOTAAL && !withRange) return ownTotal(f, lez);
        if (arg != null && Namen.containsName(elem.granulariteitLeden(), arg.attribuut())
                && kind != Expr.FunctieKind.SOM && kind != Expr.FunctieKind.TOTAAL)
            throw notComputed("Aggregation " + kind + " over het tijdsafhankelijke attribuut '" + arg.attribuut() + "'",
                    f.bron(), "zij heeft geen moment waarop zij een tijdlijn rangschikt, telt of toetst; alleen een som en"
                    + " een totaal vouwen de tijdlijn van elk lid eerst over de tijd (RUNTIME-SEMANTICS ch. 3.4c, 3.6)");
        if (!withRange)
            throw new Refusal("een Aggregation " + kind + " zonder bereik ('" + snippet(f.bron()) + "'): IR-SPEC ch. 4.3"
                    + " eist een sourceRole of sourcePath");
        Expr.AttribuutRef member = arg;
        // a member on a timeline the case gives as the asOfDate-bucket scalar faults, naming the first such member in
        // fold order (RUNTIME-SEMANTICS ch. 3.6, 8.4), and a made link has no place in that order
        boolean timelineMember = member != null && declared(elem, member.attribuut(), false).time() != null;
        // a member on a timeline is asked by the per-element fold, which refuses one that is no number (integreerbaar)
        if (member != null && !timelineMember) {
            String setSource = setSourceRefusal(kind, elem, member);
            if (setSource != null) throw new Refusal(setSource);
        }
        RolBindingRef bron = roleBindings.get(f);
        if (timelineMember && bron != null && bron.feittype() != null
                && createdRelations.contains(Namen.norm(bron.feittype())))
            throw new Refusal("een " + kind + " per lid over het tijdsafhankelijke attribuut '" + member.attribuut()
                    + "' in een vouw over de gecreëerde relatie '" + bron.feittype() + "' ('" + snippet(f.bron()) + "'): een"
                    + " gemaakte link heeft geen plaats in de vouwvolgorde, die bepaalt welk lid met een emmerscalair als"
                    + " invoer de vouw noemt (RUNTIME-SEMANTICS ch. 3.6, 5.11, 8.4); " + STILL_NOT + ", en geen golden met"
                    + " cases heeft het");
        Fold v = foldFor(f, lez, (sleutel, naam) -> {
            List<RulePlan> rules = new ArrayList<>();
            List<Reads> lezingen = new ArrayList<>();
            boolean canFail = contributionRules(sleutel, naam, kind, lez.subject, elem, range(f, lez.subject, elem), 0,
                    f.filter(), member, rules, lezingen);
            return new Fold(sleutel, List.copyOf(rules), List.copyOf(lezingen), canFail, deposits(f.filter()), false,
                    false, timelineMember);
        });
        Accumulation.Kind function = switch (kind) {
            case SOM, TOTAAL -> Accumulation.Kind.SUM;
            case MINIMUM, EERSTE -> Accumulation.Kind.MINIMUM;
            case MAXIMUM, LAATSTE -> Accumulation.Kind.MAXIMUM;
            default -> null;
        };
        int[] i = lez.fold(v, function);
        // the fallback restated into the unit of the folded member (RUNTIME-SEMANTICS ch. 2.6)
        Term fallback = f.leegwaarde() == null ? null : restated(waarde(f.leegwaarde(), lez),
                eff.unitOf(f.leegwaarde()), arg == null ? null : eff.unitOf(arg), "een §5.8.2 terugvalwaarde");
        Term aantal = new Term.SlotValue(i[0], null);
        // over no contribution a sum is its fallback or leeg, a total 0 and an ordering fold leeg
        Term outcome = switch (kind) {
            case AANTAL, AANTAL_GEVULDE -> new Term.Call(StaticFunction.COUNT, List.of(aantal), fragment);
            case SOM -> foldValue(aantal, i[1], fallback == null ? constant(null) : fallback, fragment);
            case TOTAAL -> foldValue(aantal, i[1], constant(BigDecimal.ZERO), fragment);
            default -> foldValue(aantal, i[1], constant(null), fragment);
        };
        return readFold(v, i, lez, outcome, fragment);
    }

    /** The value of a fold from the count {@code aantal} and the folded value at slot {@code waarde}, with
     *  {@code fallback} over no contribution ({@link ComputeHelpers#foldValue}). */
    private static Term foldValue(Term aantal, int waarde, Term fallback, String bron) {
        return new Term.Call(StaticFunction.FOLD_VALUE, List.of(aantal, new Term.SlotValue(waarde, null), fallback), bron);
    }

    /**
     * Why a value fold of {@code kind} over the members {@code arg} of {@code elem} has no reading, or null where it has
     * one: an additive fold over members that are no number (WF-091), and an ordering fold over members that are neither
     * a number nor a date (WF-038). The reference interpreter faults on the addition or the comparison itself and the
     * ASP runner declines the form, while this branch would hand the members to a BigDecimal sum, which fails at fire,
     * or to the Java order of their values, which answers. Both branches refuse in this sentence. It opens as the
     * refusal of the destructive DRL-text lowering opened, so that a gate report named the same form on both branches
     * while that lowering existed, until card drl-teksttak-op-truth-maintenance.
     */
    private String setSourceRefusal(Expr.FunctieKind kind, Objecttype elem, Expr.AttribuutRef arg) {
        boolean summing = kind == Expr.FunctieKind.SOM || kind == Expr.FunctieKind.TOTAAL;
        boolean ordering = kind == Expr.FunctieKind.MINIMUM || kind == Expr.FunctieKind.MAXIMUM
                || kind == Expr.FunctieKind.EERSTE || kind == Expr.FunctieKind.LAATSTE;
        Datatype t = declared(elem, arg.attribuut(), false).datatype();
        String head = "vouw " + kind + " over de setbron '" + arg.attribuut() + "' van " + elem.naam
                + ": die leden zijn van type " + t.naam();
        if (summing && !t.isGetalachtig())
            return head + " en niet getalachtig; optellen is rekenkunde over getallen (WF-091), en de"
                    + " referentie-interpreter faalt op de optelling zelf: geen enkele uitvoerder draagt deze vorm";
        if (ordering && !t.isOrdenbaar())
            return head + " en kennen geen ordening; een ordenende vouw vraagt een getal of een datum (WF-038), en de"
                    + " referentie-interpreter faalt op de vergelijking zelf: geen enkele uitvoerder draagt deze vorm";
        return null;
    }

    /**
     * The temporal total of the subject's own timeline (RUNTIME-SEMANTICS ch. 3.6): "het totaal van zijn X", optionally
     * "gedurende" a clause, as a function of the values this rule binds: the timeline of X, through its standing
     * ({@link Reads#timeline}), and every timeline and value the clause reads. The two routes of the total take the two
     * conventions of the case input, and a form that needs the one and finds the other faults ({@link Invoervormfout},
     * ch. 8.4). Without a clause the total takes the asOfDate-bucket scalar, the session's date being the horizon: it is
     * {@code rsTijdTotaal} of the one period of X's grid that holds the evaluation date ({@link BucketScalar}), the
     * segment's value times the whole units it spans, and leeg where X has no input; a full timeline faults, in the
     * reference interpreter's words, and so does a session without an evaluation date, where the total has no bucket to
     * take, X filled or not: this route reads the date ({@link Reads#rekendatum}), and the reference interpreter tests
     * the full timeline first, then the date, then the value. With a clause the total reads the full timeline: it is
     * clipped to the clause's true
     * periods first ({@link #clip}) and then totalled per clipped fragment, so an absent timeline totals 0 (ch. 3.6: the
     * clip's presence turns absence into 0), and the bucket scalar faults, before the clause is evaluated, as in the
     * reference interpreter. The runtime's helpers compute this, with {@link Period} as their segment class.
     *
     * <p>Refused by name: an X over a path or of another type than the subject (WF-083 (c)), one that is not a number
     * (WF-091), a rate over a grid whose unit the document does not declare as one unit or whose denominator is not that
     * unit ({@link #integrable}), and the total without a clause over a timeline a form writes: the reference
     * interpreter takes that total only of the case's scalar on the period of the evaluation date and faults on a full
     * timeline (ch. 3.6), so no executor confirms it, and no golden has it.
     */
    private Term ownTotal(Expr.Functie f, Reads lez) {
        String description = "een totaal over de eigen tijdlijn ('" + snippet(f.bron()) + "')";
        Expr.AttribuutRef a = (Expr.AttribuutRef) f.argumenten().get(0);
        if (a.objecttype() != lez.subject || navigationBindings.containsKey(a))
            throw new Refusal(description + " over een attribuut van een ander object dan het onderwerp: zonder bereik leest"
                    + " het totaal de tijdlijn van het onderwerp zelf (WF-083 (c)); " + STILL_NOT + " over een pad");
        Member member = integrable(timeMember(lez.subject, a.attribuut(), false, description), description);
        if (f.filter() == null && writers.containsKey(member.sleutel()))
            throw new Refusal(description + " zonder knip over de tijdlijn van '" + member.naam() + "', die een vorm schrijft:"
                    + " de referentie-interpreter neemt het totaal zonder knip alleen van de scalaire invoer op de periode"
                    + " van de rekendatum en weigert een volle tijdlijn (RUNTIME-SEMANTICS ch. 3.6); " + STILL_NOT
                    + ", en geen golden met cases heeft het");
        int timeline = lez.timeline(member);
        int onderwerp = lez.member();
        lez.markFail();
        String gran = member.time().name();
        String naam = member.naam();
        String bron = bron(f.bron());
        if (f.filter() == null) {
            int date = lez.rekendatum();
            return new Term.Call(StaticFunction.OWN_TOTAL, List.of(new Term.SlotValue(timeline, bron(a.bron())),
                    new Term.SlotValue(onderwerp, null), new Term.SlotValue(date, null), constant(naam), constant(gran)), bron);
        }
        Term cut = clip(f.filter(), lez, description, naam, complete(timeline, "the gedurende clip of \"" + naam
                + "\" needs the full timeline; ", onderwerp, " supplies only the asOfDate-bucket scalar", bron(a.bron())));
        return new Term.Call(StaticFunction.RS_TIME_TOTAL, List.of(cut, constant(gran)), bron);
    }

    /** The timeline bound at slot {@code timeline}, which a form reads whole, or the fault in the sentence {@code voor},
     *  the subject bound at slot {@code onderwerp}, {@code na}, where the case gives that input as the asOfDate-bucket
     *  scalar ({@link BucketScalar}, RUNTIME-SEMANTICS ch. 8.4; {@link ComputeHelpers#complete}). */
    private static Term complete(int timeline, String before, int onderwerp, String after, String bron) {
        return new Term.Call(StaticFunction.COMPLETE, List.of(new Term.SlotValue(timeline, bron), constant(before),
                new Term.SlotValue(onderwerp, null), constant(after)), bron);
    }

    /**
     * The gedurende clause {@code c} of a temporal total as a clip of the subject's timeline (RUNTIME-SEMANTICS
     * ch. 3.6), in the three shapes the reference interpreter carries, each through the runtime's helpers: a check of
     * an own characteristic on a timeline, which keeps the fragments of each segment within its true periods
     * ({@code rsTijdKnip}), or, negated, outside them within the segment ({@code rsTijdKnipNiet}); a flat {@code all}
     * of such checks, which clips by each in turn, the intersection of their true periods; and a comparison of an own
     * attribute on a timeline with a value, whose true periods are the segments of that timeline whose value satisfies
     * it ({@code rsTijdConditie}), where a leeg segment and a leeg bound satisfy no ordering. The fragments are kept
     * apart, never coalesced, so a clip that splits one whole unit loses it. Every characteristic's true periods are
     * bound off the bean ({@link Reads#timeline}), the compared timeline through its standing, read whole
     * ({@link #comparisonClip}), and the bound like any value of the rule. {@code cut} names the attribute whose
     * timeline is clipped, and {@code timeline} computes that timeline, which the clip takes first.
     *
     * <p>Refused by name: any other clause, a compound other than {@code all}, a check navigated or with an owner or a
     * whole-period qualifier (WF-097), a comparison whose left side is not an own attribute on a timeline, a date
     * ordering, units that differ between the two sides, and the comparisons "gelijk aan" and "ongelijk aan": the
     * runtime's {@code rsTijdConditie} reads an empty side, a gap in the compared timeline or a leeg bound, as no
     * period, where ch. 3.6 and the reference interpreter apply the comparison of ch. 1.2 per period, so that two leeg
     * sides are equal and exactly one leeg side is unequal (ch. 3.6, the worked example of 500,00); no golden has
     * either.
     */
    private Term clip(Conditie c, Reads lez, String description, String cut, Term timeline) {
        if (c instanceof Conditie.Kenmerk k) {
            if (k.eigenaar() != null || k.periode() != null)
                throw new Refusal(description + " met een kenmerk met een eigenaar of een geheleperiodetoets in de knip ('"
                        + snippet(k) + "', WF-097)");
            if (k.navigatie() != null || navigationBindings.containsKey(k))
                throw new Refusal(description + " met een genavigeerd kenmerk in de knip ('" + snippet(k) + "'): " + STILL_NOT
                        + "; geen golden met cases heeft het");
            int periods = lez.timeline(timeMember(lez.subject, k.kenmerk(), true, description));
            return new Term.Call(k.negatie() ? StaticFunction.RS_TIME_CLIP_NOT : StaticFunction.RS_TIME_CLIP,
                    List.of(timeline, new Term.SlotValue(periods, null), constant(Period.class)), bron(k.bron()));
        }
        if (c instanceof Conditie.Samengesteld s && s.kwantor() == Conditie.Kwantor.ALLE && !s.onderdelen().isEmpty()) {
            // each check clips what the one before it left
            Term out = timeline;
            for (Conditie part : s.onderdelen()) {
                if (!(part instanceof Conditie.Kenmerk))
                    throw new Refusal(description + " met een lid van de samengestelde knip dat geen kenmerk is ('"
                            + snippet(part) + "'): " + STILL_NOT + "; de referentie-interpreter neemt alleen kenmerken"
                            + " (RUNTIME-SEMANTICS ch. 3.6)");
                out = clip(part, lez, description, cut, out);
            }
            return out;
        }
        if (c instanceof Conditie.Vergelijking v) return comparisonClip(v, lez, description, cut, timeline);
        throw new Refusal(description + " met de knip '" + snippet(c) + "': " + STILL_NOT + "; de referentie-interpreter"
                + " neemt een eigen tijdsafhankelijk kenmerk, een vergelijking van een eigen tijdsafhankelijk attribuut"
                + " en een 'alle' daarvan (RUNTIME-SEMANTICS ch. 3.6)");
    }

    /** The comparison clip of {@link #clip} of the timeline of {@code cut}: the left side an own attribute on a
     *  timeline, which the clip reads whole, segment by segment, so that the asOfDate-bucket scalar faults there
     *  ({@link Invoervormfout}, RUNTIME-SEMANTICS ch. 8.4) before the bound is evaluated; the right side a value. */
    private Term comparisonClip(Conditie.Vergelijking v, Reads lez, String description, String cut, Term timeline) {
        if (v.periode() != null)
            throw new Refusal(description + " met een geheleperiodetoets in de knip ('" + snippet(v) + "', WF-097)");
        Integer testCode = Tijdrekening.toets(v.op());
        if (testCode == null)
            throw new Refusal(description + " met een datumvergelijking in de knip ('" + snippet(v) + "'): " + STILL_NOT);
        // an ordering with a leeg side holds nowhere, in ch. 1.2 and in that helper alike; the equalities differ
        if (v.op() == Conditie.Operator.GELIJK || v.op() == Conditie.Operator.ONGELIJK)
            throw new Refusal(description + " met '" + (v.op() == Conditie.Operator.GELIJK ? "gelijk aan" : "ongelijk aan")
                    + "' in de knip ('" + snippet(v) + "'): " + STILL_NOT + "; de rekenhulp rsTijdConditie leest een"
                    + " lege kant, een gat in de vergeleken tijdlijn of een lege grens, als geen periode, waar"
                    + " RUNTIME-SEMANTICS ch. 3.6 en de referentie-interpreter de vergelijking van ch. 1.2 per periode"
                    + " toepassen: twee lege kanten zijn gelijk, en precies één lege kant is ongelijk; geen golden met"
                    + " cases heeft het");
        if (!(v.links() instanceof Expr.AttribuutRef a) || a.attribuut() == null || a.objecttype() != lez.subject
                || navigationBindings.containsKey(a))
            throw new Refusal(description + " met een vergelijking in de knip waarvan de linkerkant geen eigen"
                    + " tijdsafhankelijk attribuut is ('" + snippet(v) + "'): " + STILL_NOT);
        Member tested = timeMember(lez.subject, a.attribuut(), false, description);
        if (!tested.datatype().isNumeriek() || !eff.typeOf(v.rechts()).isNumeriek())
            throw new Refusal(description + " met een vergelijking in de knip die geen getallen vergelijkt ('" + snippet(v)
                    + "'): " + STILL_NOT);
        if (!Objects.equals(eff.unitOf(v.links()), eff.unitOf(v.rechts())))
            throw new Refusal(description + " met een vergelijking in de knip over twee eenheden ('" + snippet(v) + "'): "
                    + STILL_NOT);
        int testedTimeline = lez.timeline(tested);
        int onderwerp = lez.member();
        lez.markFail();
        Term bound = waarde(v.rechts(), lez);
        String compared = tested.naam();
        Term periods = new Term.Call(StaticFunction.RS_TIME_CONDITION, List.of(
                complete(testedTimeline, "the gedurende clip of \"" + cut + "\" compares \"" + compared
                        + "\" per segment and needs its full timeline; ", onderwerp,
                        " supplies only the asOfDate-bucket scalar (ch. 3.6, ch. 8.4)", bron(a.bron())),
                constant(testCode), new Term.Call(StaticFunction.COLLAPSE, List.of(bound), bound.bron()),
                constant(Period.class)), null);
        return new Term.Call(StaticFunction.RS_TIME_CLIP, List.of(timeline, periods, constant(Period.class)),
                bron(v.bron()));
    }

    /**
     * {@code lid}, a member on a timeline whose total over time is taken, where that total is the bare value times the
     * count of whole units of the member's grid, as the runtime's {@code rsTijdTotaal} computes it: a number without a
     * unit, with a unit without a denominator, or a rate whose denominator is the time unit of the grid itself
     * ({@code EUR/mnd} on a grid of months), where the integration of RUNTIME-SEMANTICS ch. 3.6 cancels it one for one.
     * Refused by name: a value that is no number (WF-091); a rate over a grid whose unit the document does not declare
     * as exactly one unit ({@link #gridUnit}), which the reference interpreter refuses to integrate (ch. 3.6) and
     * {@code rsTijdTotaal} would weigh as if it did; and a rate whose denominator is another unit, which the reference
     * interpreter integrates through the declared factors and {@code rsTijdTotaal} would weigh unit-blind (the executor
     * note of ch. 3.6); no golden has it.
     */
    private Member integrable(Member member, String description) {
        if (!member.datatype().isNumeriek())
            throw new Refusal(description + " over '" + member.naam() + "', dat geen getal is (WF-091)");
        Eenheid e = member.datatype().eenheidOf();
        if (e == null || e.noemer() == null) return member;
        String grid = switch (member.time()) {
            case DAG -> "DAYS";
            case WEEK -> "WEEKS";
            case MAAND -> "MONTHS";
            case KWARTAAL -> "KWARTAAL";
            case JAAR -> "YEARS";
        };
        // the reference interpreter resolves the grid's unit before it looks at the denominator, so a document without
        // one is refused for that whatever the denominator
        String withoutGridUnit = gridUnit(member.time(), grid);
        if (withoutGridUnit != null)
            throw new Refusal(description + " over '" + member.naam() + "': the temporal total of a " + e.weergave() + " value: "
                    + withoutGridUnit);
        if (!grid.equals(Tijdrekening.chronoUnit(e.noemer())))
            throw new Refusal(description + " over '" + member.naam() + "' met het tarief " + e.weergave() + " op een raster van "
                    + member.time() + ": de noemer is niet de eenheid van het raster, en de tak herrekent hem niet"
                    + " (RUNTIME-SEMANTICS ch. 3.6); geen golden met cases heeft het");
        return member;
    }

    /**
     * Whether the document declares the unit of the grid {@code g}, which a rate over that grid integrates against, as
     * the reference interpreter resolves it once per fold (RUNTIME-SEMANTICS ch. 3.6, "Units, the explicit rate
     * operand"): among the units the unit systems declare without a denominator, those whose abbreviation the calendar
     * table spells as the grid's unit ({@code grid}, as {@link Tijdrekening#chronoUnit} names it), counted once per
     * canonical chain, the canonical unit and the exact factor. Null where there is exactly one chain; otherwise the
     * reference interpreter's reason, that the document declares none or several.
     */
    private String gridUnit(RegelSpraakModel.Granulariteit g, String grid) {
        record Chain(String canonical, RegelSpraakModel.Eenheidstelsel.Factor factor) {}
        Set<Chain> chains = new LinkedHashSet<>();
        for (RegelSpraakModel.Eenheidstelsel s : model.eenheidstelsels.values())
            for (Map.Entry<String, Eenheid> u : s.eenheden.entrySet())
                if (u.getValue().noemer() == null && grid.equals(Tijdrekening.chronoUnit(u.getValue())))
                    chains.add(new Chain(u.getValue().canoniek() == null ? null : Namen.norm(u.getValue().canoniek()),
                            s.exacteFactoren.get(u.getKey())));
        if (chains.size() == 1) return null;
        // the grid's unit in the spelling the reference interpreter's sentence gives it
        String spelling = switch (g) {
            case DAG -> "dg";
            case WEEK -> "wk";
            case MAAND -> "mnd";
            case KWARTAAL -> "kw";
            case JAAR -> "jr";
        };
        return chains.isEmpty()
                ? "the document declares no " + spelling + " unit, so this timeline's grid has no unit to integrate"
                        + " against (ch. 3.6)"
                : "the document declares " + chains.size() + " different " + spelling + " units, so this timeline's grid"
                        + " has no single unit to integrate against (ch. 3.6)";
    }

    /**
     * The evaluation of the fold {@code v} in the rule that reads it, bound at {@code i} ({@link Reads#fold}): where
     * its filter deposited for a member, the deposit is heard by this rule's probe ({@link DepositFlag}), since the reference
     * interpreter evaluates the filter with the reading form's collector, only where that form's evaluation reaches the
     * fold (RUNTIME-SEMANTICS ch. 5.10, 5.11); then, where its filter faulted for a member, the fault of the first in
     * fold order ({@link FilterFault}); then, where the case gives a member's timeline as the asOfDate-bucket scalar, the
     * fault that names the first such member in fold order ({@link MemberFault}, ch. 3.6, 8.4); then {@code uitkomst}, the
     * fold's value, whose fallback the reference interpreter evaluates after the members ({@link Term.FoldRead}). A
     * deposit for a member after the one that faults, which the reference interpreter does not reach, is heard as well:
     * the session faults at the fixpoint, so it changes no outcome.
     */
    private static Term readFold(Fold v, int[] i, Reads lez, Term outcome, String bron) {
        if (v.canFail() || v.valueCanFail()) lez.markFail();
        int depositFlag = v.deposits() ? lez.depositFlag() : -1;
        if (!v.canFail() && !v.valueCanFail() && depositFlag < 0) return outcome;
        return new Term.FoldRead(outcome, depositFlag < 0 ? -1 : i[3], depositFlag, v.canFail() ? i[2] : -1,
                v.valueCanFail() ? i[4] : -1, bron);
    }

    /** The fold of {@code f}, an aggregation or a quantifier fold, on the subject of {@code lez}, the rule that reads
     *  it, built by {@code bouw} from its key and its name the first time it is read. While it is built, the binders in
     *  scope in {@code lez} are those its contribution rules see from outside ({@link #foldBinders}). */
    private Fold foldFor(Object f, Reads lez, BiFunction<String, String, Fold> bouw) {
        Map<Objecttype, Fold> perSubject = folds.computeIfAbsent(f, k -> new IdentityHashMap<>());
        Fold v = perSubject.get(lez.subject);
        if (v == null) {
            String naam = "vouw " + foldCounter++;
            Set<String> outside = foldBinders;
            foldBinders = lez.binderRange();
            try {
                v = bouw.apply(vorm + ": " + naam, naam);
            } finally {
                foldBinders = outside;
            }
            perSubject.put(lez.subject, v);
        }
        return v;
    }

    /** The count over an instance-set union (RUNTIME-SEMANTICS ch. 3.4d): a contribution rule per selection, each with
     *  its own relation and its own filter, all under one key; a member two selections select gives two equal
     *  contributions, which merge into one fact, so the count is that of the union. */
    private Fold unionFold(Expr.Functie f, Expr.InstanceSetUnion u, Reads lez) {
        Objecttype subject = lez.subject;
        // one count answers a number of instances of one declared type: two populations leave whether two selections
        // share an instance to the subtype lattice, which the reference interpreter faults on (WF-110 (d))
        Objecttype population = null;
        for (Expr.InstanceSetUnion.Tak t : u.takken()) {
            if (t.elementType() == null) continue;
            if (population == null) population = t.elementType();
            else if (t.elementType() != population)
                throw new Refusal("een InstanceSetUnion over twee populaties ('" + population.naam + "' en '"
                        + t.elementType().naam + "', '" + snippet(u.bron()) + "'): één telling antwoordt een aantal over"
                        + " één gedeclareerd type, en of twee selecties dan een instantie delen, is een vraag over de"
                        + " subtyperelatie (WF-110 (d))");
        }
        return foldFor(f, lez, (sleutel, naam) -> {
            List<RulePlan> rules = new ArrayList<>();
            List<Reads> lezingen = new ArrayList<>();
            boolean canFail = false, deposits = false;
            for (int k = 0; k < u.takken().size(); k++) {
                Expr.InstanceSetUnion.Tak t = u.takken().get(k);
                if (t.elementType() == null)
                    throw new Refusal("een selectie zonder elementtype in een InstanceSetUnion ('" + snippet(t.bron()) + "')");
                canFail |= contributionRules(sleutel, naam + ", selectie " + k, Expr.FunctieKind.AANTAL, subject,
                        t.elementType(), oneStepRange(t.bronRol(), subject, t.elementType(), snippet(t.bron())), k,
                        t.filter(), null, rules, lezingen);
                deposits |= deposits(t.filter());
            }
            return new Fold(sleutel, List.copyOf(rules), List.copyOf(lezingen), canFail, deposits);
        });
    }

    /**
     * The contribution rule of one range of the fold {@code sleutel}, planned: the fold's subject bound first, the
     * member's pattern joined to it through {@code range}, the member's reads, the {@code filter} as the test (the member
     * is the filter's subject, RUNTIME-SEMANTICS ch. 3.4a), and a consequence that inserts the member's {@link Contribution}
     * logically: for a count the member; for a filled count the member where its value is gevuld; for the other folds
     * the member's value where it is gevuld, since a leeg member is skipped (ch. 1.2) ({@link Consequence.Input}). Beside
     * it, where the filter can fault, the rule that inserts the member's {@link FilterFault} ({@link #filterFaultRule}),
     * and where the filter can deposit, the rule that inserts the member's {@link FilterDeposit}
     * ({@link #filterDepositRule}); {@code selection} is the number of the selection of an instance-set union, 0 for any
     * other fold. The rules go into {@code uit} and their reads into {@code lezingen}, to be registered with the form;
     * returns whether the filter can fault.
     */
    private boolean contributionRules(String sleutel, String naam, Expr.FunctieKind kind, Objecttype subject,
                                   Objecttype elem, Range range, int selection, Conditie filter,
                                   Expr.AttribuutRef arg, List<RulePlan> out, List<Reads> lezingen) {
        Reads lez = contributionReads(subject, elem, range.join());
        Set<Integer> read = lez.follow();
        Term testTerm = filter == null ? null : voorwaarde(filter, lez);
        boolean canFail = lez.canFail();
        lez.stop();
        Set<Integer> use = lez.follow();
        int member = lez.member();
        // a member on a timeline contributes its own total over time (RUNTIME-SEMANTICS ch. 3.6, the per-element fold);
        // vouw admits only a sum and a total over one
        Member memberValue = arg == null ? null : declared(elem, arg.attribuut(), false).time() == null
                ? member(elem, arg.attribuut(), false) : integrable(timeMember(elem, arg.attribuut(), false,
                        "een totaal per lid ('" + snippet(arg.bron()) + "')"), "een totaal per lid");
        int waarde = memberValue == null ? -1 : memberValue.time() == null ? lez.own(memberValue) : lez.timeline(memberValue);
        String totaal = memberValue == null || memberValue.time() == null ? null : memberValue.time().name();
        lez.stop();
        List<LhsItem> items = lez.plan(conditionRole(canFail), testTerm, read, filter);
        Consequence.Input input = kind == Expr.FunctieKind.AANTAL ? new Consequence.Input.Count()
                : new Consequence.Input.MemberValue(waarde, totaal, kind == Expr.FunctieKind.AANTAL_GEVULDE);
        String formName = model.geëmitteerdeRegelNaam(vorm);
        out.add(lez.rulePlan(formName + ": bijdrage aan " + naam, items, new Consequence.Contribution(FOLD_SUBJECT,
                sleutel, member, input, List.copyOf(use), lez.size(), Consequence.Insertion.LOGICAL)));
        lezingen.add(lez);
        if (canFail) out.add(filterFaultRule(sleutel, formName + ": filterfout in " + naam, subject, elem, range,
                selection, filter, lezingen::add));
        if (totaal != null) out.add(memberFaultRule(sleutel, formName + ": lidfout in " + naam, subject, elem, range,
                selection, filter, memberValue, lezingen::add));
        if (deposits(filter)) out.add(filterDepositRule(sleutel, formName + ": filterdeponering in " + naam, subject,
                elem, range, filter, lezingen::add));
        return canFail;
    }

    /**
     * The rule beside a contribution rule whose filter can deposit, planned: that rule's left-hand side planned again,
     * the test that evaluating the filter for the member reaches a place that deposits (a division of a filled dividend
     * by a leeg divisor or the square root of a negative number, RUNTIME-SEMANTICS ch. 1.2), heard by a probe in the
     * test as {@link #depositRule} hears a condition's ({@link DepositFlag}), so a place the filter's own composite does not
     * reach deposits nothing (ch. 5.10), and a consequence that inserts the member's {@link FilterDeposit} logically,
     * on the fold's subject. No inconsistency: the reference interpreter evaluates the filter only where the evaluation
     * of the form that reads the fold reaches it, and deposits under that form ({@link #readFold}). A filter that
     * faults after a place that deposits has deposited, as there; the fault is the reading form's
     * ({@link #filterFaultRule}). The fact goes when a read it rested on changes, as a contribution does.
     */
    private RulePlan filterDepositRule(String sleutel, String ruleName, Objecttype subject, Objecttype elem,
                                          Range range, Conditie filter, Consumer<Reads> recorder) {
        Reads lez = contributionReads(subject, elem, range.join());
        Set<Integer> read = lez.follow();
        Term testTerm = voorwaarde(filter, lez);
        lez.stop();
        if (lez.depositFlagSlot() < 0)
            throw new IllegalStateException("het filter van " + sleutel + " deponeert, maar kreeg geen sonde");
        int member = lez.member();
        List<LhsItem> items = lez.plan(TestItem.Role.DEPOSIT, testTerm, read, filter);
        recorder.accept(lez);
        return lez.rulePlan(ruleName, items, new Consequence.FilterDeposit(FOLD_SUBJECT, sleutel, member,
                Consequence.Insertion.LOGICAL));
    }

    /**
     * The rule beside a contribution rule whose filter can fault, planned: that rule's left-hand side planned again,
     * joined per route from the fold's subject with the member's place bound at every to-many step of the route
     * ({@link Range#perRoute}), the test that the filter faults for the member, and a consequence that inserts the
     * member's {@link FilterFault} logically, on the fold's subject, with its place in the fold order: the
     * {@code selection}, then those places ({@link Consequence.FoldPlace}). No {@link Fout}: whether the division happens
     * depends on the form that reads the fold, which faults where its evaluation reaches the fold while such a fact
     * stands ({@link #fold}). The fact goes when a read it rested on changes, as a contribution does. A member whose
     * place the input did not state has no place in the fold order, and the first fault cannot be told without it: the
     * consequence throws, naming the field.
     */
    private RulePlan filterFaultRule(String sleutel, String ruleName, Objecttype subject, Objecttype elem,
                                      Range range, int selection, Conditie filter, Consumer<Reads> recorder) {
        List<Consequence.FoldPlace.Step> places = new ArrayList<>();
        Reads lez = contributionReads(subject, elem, l -> places.addAll(range.perRoute().apply(l)));
        Set<Integer> read = lez.follow();
        Term testTerm = voorwaarde(filter, lez);
        lez.stop();
        Set<Integer> use = lez.follow();
        int member = lez.member();
        lez.stop();
        use.addAll(read);
        for (Consequence.FoldPlace.Step p : places) {
            use.add(p.aantal());
            use.add(p.place());
        }
        List<LhsItem> items = lez.plan(TestItem.Role.FAULT, testTerm, read, filter);
        recorder.accept(lez);
        return lez.rulePlan(ruleName, items, new Consequence.FilterFault(FOLD_SUBJECT, sleutel, member, testTerm,
                new Consequence.FoldPlace(selection, places, Consequence.PlaceReason.FILTER_FAULT), List.copyOf(use),
                lez.size(), Consequence.Insertion.LOGICAL));
    }

    /**
     * The rule beside a contribution rule of a per-element fold over an attribute on a timeline (RUNTIME-SEMANTICS
     * ch. 3.6, §7.1), planned: that rule's left-hand side planned again, joined per route from the fold's subject with
     * the member's place bound at every to-many step of the route ({@link Range#perRoute}), the test that the member's
     * filter holds and that the case gives the member's timeline as the asOfDate-bucket scalar ({@link BucketScalar}),
     * and a consequence that inserts the member's {@link MemberFault} logically, on the fold's subject, with its place in
     * the fold order: the {@code selection}, then those places. Such a member contributes nothing, since a set fold needs
     * each member's own timeline and the scalar states none (ch. 8.4); the form that reads the fold faults where its
     * evaluation reaches the fold while such a fact stands ({@link #readFold}), in the reference interpreter's words.
     * A member whose filter faults has its {@link FilterFault} instead, which the reading form meets first. A member whose
     * place the input did not state has no place in the fold order: the consequence throws, naming the field, as the
     * filter fault rule does.
     */
    private RulePlan memberFaultRule(String sleutel, String ruleName, Objecttype subject, Objecttype elem, Range range,
                                   int selection, Conditie filter, Member memberValue, Consumer<Reads> recorder) {
        List<Consequence.FoldPlace.Step> places = new ArrayList<>();
        Reads lez = contributionReads(subject, elem, l -> places.addAll(range.perRoute().apply(l)));
        Set<Integer> read = lez.follow();
        // the member's filter holds, without its fault where it can fault: the filter fault rule takes that fault
        List<Term> bucket = new ArrayList<>();
        if (filter != null) {
            Term filterTest = voorwaarde(filter, lez);
            bucket.add(lez.canFail() ? new Term.WithoutFault(filterTest, bron(filter.bron())) : filterTest);
        }
        int waarde = lez.timeline(memberValue);
        lez.stop();
        int member = lez.member();
        bucket.add(emmerscalair(waarde, true));
        List<LhsItem> items = lez.plan(TestItem.Role.VORM, all(bucket), read, filter);
        recorder.accept(lez);
        return lez.rulePlan(ruleName, items, new Consequence.MemberFault(FOLD_SUBJECT, sleutel, member,
                new Consequence.FoldPlace(selection, places, Consequence.PlaceReason.MEMBER_FAULT),
                "the per-element temporal total of \"" + memberValue.naam() + "\" over ", ": the case supplies an"
                + " asOfDate-bucket scalar, which carries no period of its own; a set fold needs each member's own"
                + " timeline (ch. 3.6/§7.1, ch. 8.4)", Consequence.Insertion.LOGICAL));
    }

    /** The reads of a contribution rule, planned: the member is the subject, the fold's subject is bound before it
     *  ({@link #FOLD_SUBJECT}), and {@code range} joins the two. */
    private Reads contributionReads(Objecttype subject, Objecttype elem, Consumer<Reads> range) {
        Reads lez = new Reads(elem);
        lez.outside = foldBinders;
        lez.addToHeadPlan(new FactPattern(beanClass(subject), FOLD_SUBJECT, List.of(), List.of(), List.of()));
        range.accept(lez);
        return lez;
    }

    /**
     * How a rule of a fold joins the member, its subject, to the fold's subject ({@link #FOLD_SUBJECT}), in two
     * shapes, each planned into the rule's reads. {@code join}, for the contribution rule: the join on the member's
     * pattern, and the intermediates a multi-step range needs right after it, quantified, so a member matches once.
     * {@code perRoute}, for the filter fault rule: the same join with the intermediates bound, so a member matches once
     * per route, and per to-many step of the route, in order, the place of the step's member in its list
     * ({@link #place}); it returns those places, and refuses a range whose members have no place.
     */
    private record Range(Consumer<Reads> join, Function<Reads, List<Consequence.FoldPlace.Step>> perRoute) {}


    /** The place of a to-many step's member in its list, for the filter fault rule, planned after the subject pattern:
     *  an accumulate over the {@link ListPlace} facts of field {@code veld} on the node the walk over {@code keten}
     *  reaches from {@code node}, a variable a pattern before it binds (the node itself without a chain), which counts
     *  them and takes the least place. The count gives a row where the input stated no place, so the rule still fires
     *  and its consequence names the gap instead of dropping the fault. */
    private Consequence.FoldPlace.Step place(Reads member, String veld, String node, List<String> chain) {
        listFields.add(veld);
        String p = "$lijstplaats_" + id("plaats");
        String waarde = "$lijstplaatswaarde_" + id("plaats");
        String aantal = "$lijstplaatsen_" + id("plaats");
        String least = "$plaats_" + id("plaats");
        Alpha ofField = equal(id("plaats-veld"), "veld", veld);
        BetaTest ofMember = new BetaTest(id("plaats-lid"), StaticFunction.PLACE_OF, node,
                List.of(constant(List.copyOf(chain))));
        member.addToRangePlan(new Accumulation(new FactPattern(ListPlace.class, p, List.of(ofField), List.of(), List.of(ofMember),
                List.of(Binding.van(waarde, ListPlace.class, "place"))),
                List.of(new Accumulation.Aggregation(Accumulation.Kind.COUNT, null, aantal),
                        new Accumulation.Aggregation(Accumulation.Kind.MINIMUM, waarde, least))));
        int n = member.size();
        return new Consequence.FoldPlace.Step(veld, member.allocateSlot(ReadKind.LIST_PLACES, "lijstplaatsen " + n, aantal),
                member.allocateSlot(ReadKind.LIST_PLACE, "lijstplaats " + n, least));
    }

    /** The range of the fold {@code f} from {@code subject} to {@code elem}: its multi-step path, or its one step. */
    private Range range(Expr.Functie f, Objecttype subject, Objecttype elem) {
        List<RolBindingRef> pad = sourcePaths.get(f);
        if (pad != null) return pathRange(pad, subject, elem, snippet(f.bron()));
        return oneStepRange(roleBindings.get(f), subject, elem, snippet(f.bron()));
    }

    /**
     * The one-step range of a fold (IR-SPEC ch. 4.3, {@code sourceRole}), derived from its binding as the exec-model
     * branch derives it: the bound role is the member's and its sibling the subject's, and §13.3.9 stores the relation in
     * one scalar bean field on whichever end is the one side. A to-many member role keeps it on the member, named after
     * the subject's role; a to-one member role keeps it on the subject, named after the member's role. Refused as that
     * branch refuses them: a binding that names no role pair, a to-one member role whose sibling is to-many (one field
     * carries both directions there, and that branch keeps refusing the shape until a card of its own measures it), and
     * a join the bean model gives no field. A to-many member role stores its links on the members, each in the list of
     * the subject at a place the case input states under the field ({@link ListPlace}); a to-one member role reaches
     * one member at most, whose place compares with nothing. The join is planned on the member's pattern, as the
     * recipient rule of an allocation plans its join ({@link #recipientsJoin}).
     */
    private Range oneStepRange(RolBindingRef rb, Objecttype subject, Objecttype elem, String description) {
        RegelSpraakModel.Rol[] pair = FactModelGenerator.boundRolePair(model, rb);
        if (pair == null) throw new Refusal("het bereik van '" + description + "' noemt geen rolpaar (IR-SPEC ch. 4.3)");
        if (rb.feittype() != null && createdRelations.contains(Namen.norm(rb.feittype())))
            return linkRange(rb, pair[0], description);
        String elemRole = pair[0].naam(), backRoleName = pair[1].naam();
        boolean forward = !model.naarVeelRol(subject, elem, elemRole);
        if (forward && model.naarVeelRol(elem, subject, backRoleName))
            throw new Refusal("een Aggregation over de enkelvoudige elementrol '" + elemRole + "' wiens zusterrol '"
                    + backRoleName + "' meervoudig is ('" + description + "'): dat feittype heeft één beanveld voor beide richtingen, en"
                    + " deze tak weigert de vorm voorzichtig tot een eigen kaart haar meet (§13.3.9 opslagrichting)");
        Objecttype carrier = forward ? subject : elem, doel = forward ? elem : subject;
        String rol = forward ? elemRole : backRoleName;
        if (!FactModelGenerator.hasReferenceField(model, carrier, rol, doel))
            throw new Refusal("de aggregatie-join van '" + description + "' heeft geen beanveld — "
                    + FactModelGenerator.noJoinFieldReason(model, carrier, rol, doel) + " (§13.3.9 opslagrichting)");
        // forward the fold subject's reference field names the member, else the member's names the fold's subject
        Binding.Field veld = new Binding.Field(beanClass(carrier), rol);
        Consumer<Reads> join = member -> member.subjectJoin(forward
                ? new Join(id("bereik"), null, FOLD_SUBJECT, veld) : new Join(id("bereik"), rol, FOLD_SUBJECT));
        String listField = Namen.ident(rol, false);
        return new Range(join, member -> {
            join.accept(member);
            return forward ? List.of() : List.of(place(member, listField, SUBJECT, List.of()));
        });
    }

    /** The one-step range of a fold over a relation a form creates: the member and the fold's subject joined by an
     *  {@link RelationLink} of the relation in an {@code exists} ({@link #linkExists}), the member filling
     *  {@code memberRole}. The links of such a relation change while the session fires and are in no bean field, so a field
     *  join would miss them. A created link has no place in the fold order, which the reference interpreter gives by the
     *  order of creation, so a filter that can fault over such a range is refused. */
    private Range linkRange(RolBindingRef rb, RegelSpraakModel.Rol memberRole, String description) {
        LinkRelation r = linkRelation(rb, memberRole, description);
        return new Range(member -> member.addToRangePlan(linkExists(r)), member -> {
            throw new Refusal("een filter dat kan falen in een vouw over de gecreëerde relatie '" + rb.feittype()
                    + "' ('" + description + "'): een gemaakte link heeft geen plaats in de vouwvolgorde, die bepaalt welke fout"
                    + " de vouw geeft (RUNTIME-SEMANTICS ch. 5.11); " + STILL_NOT);
        });
    }

    /** A relation a form creates, as a one-step range over it walks it: by its normalised name, and whether the member
     *  fills its first role. */
    private record LinkRelation(String relation, boolean memberIsFirst) {}

    /** The relation a form creates that a one-step range over {@code rb} walks, its member filling {@code memberRole}, for
     *  {@code description}; a fact type that is no relation of two roles is refused. */
    private LinkRelation linkRelation(RolBindingRef rb, RegelSpraakModel.Rol memberRole, String description) {
        RegelSpraakModel.Feittype ft = feittype(rb.feittype());
        if (ft == null || ft.rollen.size() != 2 || ft.wederkerig)
            throw new Refusal("het bereik van '" + description + "' over de gecreëerde relatie '" + rb.feittype()
                    + "', die geen relatie van twee rollen is: " + STILL_NOT);
        return new LinkRelation(Namen.norm(ft.naam),
                Namen.norm(ft.rollen.get(0).naam()).equals(Namen.norm(memberRole.naam())));
    }

    /** The range over the relation {@code r} a form creates, planned, of a fold's contribution rule and of an
     *  allocation's recipient rule: an {@code exists} of a {@link RelationLink} of the relation that holds the subject at
     *  the member's end and the object the rule binds before the subject ({@link #FOLD_SUBJECT}) at the other. */
    private Exists linkExists(LinkRelation r) {
        String relationLink = "$koppeling_" + id("bereik");
        Alpha ofRelation = equal(id("bereik-koppeling"), "relation", r.relation());
        Join member = new Join(id("bereik-lid"), r.memberIsFirst() ? "eerste" : "tweede", SUBJECT);
        Join carrier = new Join(id("bereik-drager"), r.memberIsFirst() ? "tweede" : "eerste", FOLD_SUBJECT);
        return new Exists(List.of(new FactPattern(RelationLink.class, relationLink, List.of(ofRelation), List.of(member, carrier),
                List.of())), List.of(relationLink), List.of(SUBJECT, FOLD_SUBJECT));
    }

    /**
     * The multi-step range of a fold (IR-SPEC ch. 4.3, {@code sourcePath}; RUNTIME-SEMANTICS ch. 3.4b), on the plan the
     * shared {@link SourcePath} derives: where the walk down from the subject and the walk up from the member meet, one
     * constraint on the member's pattern that the two walks reach one object ({@link StaticFunction#MEETING}); otherwise
     * the intermediates neither walk determines go into one {@code exists} after the member's pattern, so a member that
     * two intermediates reach still matches once ({@link Exists}, whose scope holds the intermediates, and reads the
     * fold's subject and the member from outside). Two matches would give two equal contributions, which merge into one
     * fact as well. The filter fault rule binds the intermediates instead, as patterns of the rule, one match per route,
     * and the place of the member of every backward step, the to-many ones ({@link SourcePath}): an intermediate where the
     * step ends on one, else the node the member's own walk reaches.
     */
    private Range pathRange(List<RolBindingRef> pad, Objecttype subject, Objecttype elem, String description) {
        for (RolBindingRef step : pad)
            if (step.feittype() != null && createdRelations.contains(Namen.norm(step.feittype())))
                throw new Refusal("een vouw met een bereik van meer stappen over de gecreëerde relatie '"
                        + step.feittype() + "' ('" + description + "'): " + STILL_NOT);
        SourcePath.Plan plan = SourcePath.parse(model, subject, pad, elem);
        if (plan.refusal() != null) throw new Refusal(plan.refusal() + " ('" + description + "')");
        if (plan.meets()) {
            int m = plan.meeting();
            List<String> fromMember = List.copyOf(plan.chainFromElement(m)),
                    fromSubject = List.copyOf(plan.chainFromSubject(m));
            Consumer<Reads> join = member -> member.subjectBeta(new BetaTest(id("pad"), StaticFunction.MEETING,
                    FOLD_SUBJECT, List.of(constant(fromMember), constant(fromSubject))));
            return new Range(join, member -> {
                join.accept(member);
                return placesAlong(plan, Map.of(), member);
            });
        }
        return new Range(member -> {
            List<FactPattern> patterns = intermediatePatterns(plan, new LinkedHashMap<>());
            // the first intermediate hangs off the fold's subject and the last off the member: both bound before
            member.addToRangePlan(new Exists(patterns, patterns.stream().map(FactPattern::naam).toList(),
                    List.of(FOLD_SUBJECT, SUBJECT)));
        }, member -> {
            Map<Integer, String> between = new LinkedHashMap<>();
            for (FactPattern p : intermediatePatterns(plan, between)) member.addToRangePlan(p);
            return placesAlong(plan, between, member);
        });
    }

    /** The patterns of the intermediates of {@code plan} that neither walk determines, each over its node's bean class,
     *  its variable named into {@code between}, and constrained to its neighbours by the hops on either side of it
     *  ({@link #pathStep}). */
    private List<FactPattern> intermediatePatterns(SourcePath.Plan plan, Map<Integer, String> between) {
        for (int i : plan.intermediateNodes()) between.put(i, "$tussen_" + id("pad"));
        List<FactPattern> patterns = new ArrayList<>();
        int last = plan.backwardFrom() - 1;
        for (Map.Entry<Integer, String> e : between.entrySet()) {
            int i = e.getKey();
            List<BetaTest> steps = new ArrayList<>();
            steps.add(pathStep(plan, i, i, between));
            if (i == last) steps.add(pathStep(plan, plan.backwardFrom(), i, between));
            patterns.add(new FactPattern(beanClass(plan.nodeTypes().get(i)), e.getValue(), List.of(), List.of(), steps,
                    List.of()));
        }
        return patterns;
    }

    /** The places along a route of {@code plan}, one per backward step in step order: the step's own member, node
     *  {@code i}, holds the field toward node {@code i - 1}, whose list it is in. A backward step never lies in the
     *  subject's forward run, so its node is a bound intermediate or a node of the member's own walk. */
    private List<Consequence.FoldPlace.Step> placesAlong(SourcePath.Plan plan, Map<Integer, String> between, Reads member) {
        List<Consequence.FoldPlace.Step> out = new ArrayList<>();
        for (int i = 1; i <= plan.steps().size(); i++) {
            SourcePath.Step step = plan.steps().get(i - 1);
            if (!step.backward()) continue;
            String node = between.get(i);
            out.add(node != null ? place(member, step.beanField(), node, List.of())
                    : place(member, step.beanField(), SUBJECT, plan.chainFromElement(i)));
        }
        return out;
    }

    /** The constraint on the pattern of node {@code current} of {@code plan} by hop {@code step}, which connects nodes
     *  {@code stap - 1} and {@code step}: its other end is read through whichever walk determines it, the subject's (a
     *  forward prefix), the member's (a backward suffix) or an intermediate declared before it, and the hop's field on
     *  the end that holds it, the hop's source going forward and its target going backward. The two walks meet where
     *  the hop holds ({@link StaticFunction#MEETING}). */
    private BetaTest pathStep(SourcePath.Plan plan, int step, int current, Map<Integer, String> between) {
        int another = step - 1 == current ? step : step - 1;
        boolean thisIsTarget = another == step - 1;
        List<String> chain = another <= plan.forward() ? plan.chainFromSubject(another)
                : another >= plan.backwardFrom() ? plan.chainFromElement(another) : List.of();
        String otherName = another <= plan.forward() ? FOLD_SUBJECT
                : another >= plan.backwardFrom() ? SUBJECT : between.get(another);
        SourcePath.Step hop = plan.steps().get(step - 1);
        boolean fieldHere = hop.backward() == thisIsTarget;
        List<String> own = fieldHere ? List.of(hop.beanField()) : List.of();
        List<String> other = new ArrayList<>(chain);
        if (!fieldHere) other.add(hop.beanField());
        return new BetaTest(id("pad"), StaticFunction.MEETING, otherName,
                List.of(constant(own), constant(List.copyOf(other))));
    }


    /** A read of an attribute: of the subject where the reference carries no path (it must name the subject's own type,
     *  WF-053), else over its path of roles. */
    private int attributeRead(Expr.AttribuutRef a, Reads lez) {
        if (a.objecttype() == null || a.attribuut() == null)
            throw new Refusal("een onopgeloste AttributeReference ('" + snippet(a.bron()) + "')");
        List<RolBindingRef> pad = navigationBindings.get(a);
        if (pad == null) {
            if (a.objecttype() != lez.subject)
                throw new Refusal("een eigen lezing die " + a.objecttype().naam + " noemt op een " + lez.subject.naam
                        + " ('" + snippet(a.bron()) + "', WF-053)");
            return lez.own(member(lez.subject, a.attribuut(), false));
        }
        Path p = pad(lez.subject, pad, a.objecttype(), "de lezing '" + snippet(a.bron()) + "'");
        return lez.navigated(p, member(p.reaches(), a.attribuut(), false));
    }

    // ---------------- conditions ----------------

    /** The condition {@code c} as a term whose value is a truth value ({@link Term}), with the reads it needs bound in
     *  {@code lez}. Every condition node is named here, so a node the IR records gain later fails to compile until it
     *  is decided whether this branch reads it. */
    private Term voorwaarde(Conditie c, Reads lez) {
        return switch (c) {
            case Conditie.Kenmerk k -> characteristicCheck(k, lez);
            case Conditie.Samengesteld s -> compound(s, lez);
            case Conditie.Vergelijking v -> comparison(v, lez);
            case Conditie.Predicaat p -> predicate(p, lez);
            case Conditie.Periode p -> timeWindow(p, lez);
            case Conditie.Kwantorvouw q -> quantifierFold(q, lez);
            case Conditie.Rollidmaatschap rol -> roleCheck(rol, lez);
            // the v0 far-side record, which the loader no longer makes: since L2 a role check is one node (IR-SPEC
            // ch. 4.5), so only a hand-built model reaches this
            case Conditie.VerreRollidmaatschap rol -> throw notRead("VerreRollidmaatschap", c, "de verre rolcontrole"
                    + " van v0, die sinds L2 de ene knoop RoleMembership is (IR-SPEC ch. 4.5) en die de lader niet maakt");
            case Conditie.Regelversie rv -> channelRead(rv, lez);
            // RUNTIME-SEMANTICS ch. 5.7 and WF-044: the marker fails the evaluation of the form where the evaluation
            // reaches it, so a composite that settles before it reads it plays
            case Conditie.Onopgelost o -> {
                lez.markFail();
                yield new Term.Fail(Rekenfout.UNRESOLVED_CONDITION, bron(o.bron()));
            }
        };
    }

    /**
     * A time window on a scalar target, the evaluation-date gate (RUNTIME-SEMANTICS ch. 3.10): it holds where the
     * session's evaluation date lies in the window, {@code from} inclusive, {@code to} exclusive or inclusive where the
     * source wrote "tot en met" ({@code toInclusive}), an absent bound open. An unset evaluation date never matches, nor
     * does a leeg bound; neither is a fault. Both bounds are read, {@code from} before {@code to}, before the date decides
     * (ch. 5.11). A bound is a date literal or the subject's own scalar date attribute (WF-111), read per subject, also
     * where another rule derives it; any other bound is refused by name ({@link WindowPosition#BOUND_FORM}). On a
     * time-dependent target the window would clip the timeline instead, and such a target is refused before this
     * ({@link #member}).
     */
    private Term timeWindow(Conditie.Periode p, Reads lez) {
        if (p.vanaf() == null && p.tot() == null)
            throw new Refusal("een periodevenster zonder grens ('" + snippet(p) + "'): er is geen venster om op te"
                    + " poorten");
        for (Expr bound : java.util.Arrays.asList(p.vanaf(), p.tot()))
            if (bound != null && !WindowPosition.boundLowerable(bound, lez.subject, navigationBindings::containsKey))
                throw new Refusal(WindowPosition.BOUND_FORM + ": " + snippet(p));
        Term from = p.vanaf() == null ? null : waarde(p.vanaf(), lez);
        Term to = p.tot() == null ? null : waarde(p.tot(), lez);
        int date = lez.rekendatum();
        // an absent bound is open where a leeg one never matches, so whether the window has the bound is a constant
        // of its own beside the bound's place
        return new Term.Call(StaticFunction.TIME_WINDOW, List.of(from == null ? constant(null) : from,
                constant(from != null), to == null ? constant(null) : to, constant(to != null),
                constant(p.totInclusief()), new Term.SlotValue(date, null)), bron(p.bron()));
    }

    private static Refusal notRead(String vorm, Conditie c, String reden) {
        return new Refusal("een " + vorm + " in de voorwaarde ('" + snippet(c) + "'): " + reden);
    }

    /** A characteristic check: of the subject, a bound truth value, complemented where the check is negated; over a
     *  role, the reached object's, and a check over an absent link holds in neither polarity (RUNTIME-SEMANTICS ch. 5.8
     *  rule 3). The standings are total, so no negation is needed here. Under the whole-period qualifier the check
     *  folds the characteristic's true periods instead ({@link #wholePeriodCharacteristicCheck}). */
    private Term characteristicCheck(Conditie.Kenmerk k, Reads lez) {
        // WF-109: the host is satisfied extensionally; a host the subject is no instance of names another object, which
        // a rule condition does not bind
        if (!Host.hostOp(k, lez.subject))
            throw new Refusal("een CharacteristicCheck met een host die het onderwerp niet is ('" + snippet(k) + "')");
        if (k.periode() != null) return wholePeriodCharacteristicCheck(k, lez);
        boolean desired = !k.negatie();
        List<RolBindingRef> nav = navigationBindings.get(k);
        if (nav == null) {
            if (k.navigatie() != null)
                throw new Refusal("een CharacteristicCheck met een navigatie zonder binding ('" + snippet(k) + "')");
            int i = lez.own(member(lez.subject, k.kenmerk(), true));
            return new Term.Call(StaticFunction.KENMERK, List.of(new Term.SlotValue(i, null), constant(desired)),
                    bron(k.bron()));
        }
        Path p = pad(lez.subject, nav, null, "de kenmerkcheck '" + snippet(k) + "'");
        int i = lez.navigated(p, member(p.reaches(), k.kenmerk(), true));
        return new Term.Call(StaticFunction.CHARACTERISTIC_CHECK_ALONG_PATH, List.of(new Term.SlotValue(i, null), constant(desired)),
                bron(k.bron()));
    }

    // ---------------- the whole-period qualifier ----------------

    /**
     * A characteristic check under the whole-period qualifier (RUNTIME-SEMANTICS ch. 3.9): whether the characteristic's
     * true periods cover every day of every calendar year or month they touch ({@link #wholePeriod}). The periods are
     * those of the subject or of the object a path of to-one roles reaches, bound as a timeline; where a link on the
     * path is absent the timeline is null, which touches no bucket and passes, as a characteristic without periods does,
     * where the unqualified check over an absent link holds in neither polarity. No executor writes a characteristic on
     * a timeline (ch. 3.11), so its periods are input. Refused by name: the negation inside the qualifier
     * ("gedurende het gehele jaar niet …"), a per-day complement of the periods (ch. 3.12), on both branches.
     */
    private Term wholePeriodCharacteristicCheck(Conditie.Kenmerk k, Reads lez) {
        String description = "een CharacteristicCheck met wholePeriod ('" + snippet(k) + "')";
        if (k.negatie())
            throw new Refusal(description + " met de ontkenning binnen de kwalificatie: " + STILL_NOT + ", zoals de andere twee"
                    + " takken (RUNTIME-SEMANTICS ch. 3.9, 3.12); geen golden met cases heeft haar");
        List<RolBindingRef> nav = navigationBindings.get(k);
        if (nav == null) {
            if (k.navigatie() != null) throw new Refusal(description + " met een navigatie zonder binding");
            return wholePeriod(k.periode(), new Term.SlotValue(lez.timeline(timeMember(lez.subject, k.kenmerk(), true, description)),
                    null), CHARACTERISTIC_TEST, null, bron(k.bron()));
        }
        Path p = pad(lez.subject, nav, null, description);
        return wholePeriod(k.periode(), new Term.SlotValue(lez.navigated(p, timeMember(p.reaches(), k.kenmerk(), true, description)),
                null), CHARACTERISTIC_TEST, null, bron(k.bron()));
    }

    /**
     * A comparison under the whole-period qualifier (RUNTIME-SEMANTICS ch. 3.9): whether the subject's own timeline
     * satisfies the comparison with a bound on every day of every calendar year or month it touches
     * ({@link #wholePeriod}). One side is an own attribute on a timeline, the other the bound, a value of the rule like
     * any other, evaluated whatever the timeline holds, as the reference interpreter evaluates it, and restated into the
     * timeline's unit (ch. 2.6); a leeg bound fails every day, never a fault. Where the timeline stands on the right, the
     * operator is reversed, never negated: on a leeg day the two differ. Refused by name, as the reference interpreter
     * folds none of them: a timeline read over a path (ch. 3.12: it folds no navigated comparison), two timelines, no
     * timeline, a date ordering (ch. 3.12) and a side that is no number. A bound read over a path is a value like any
     * other. The timeline is read before the bound, as the reference interpreter reads them.
     */
    private Term wholePeriodComparison(Conditie.Vergelijking v, Reads lez) {
        String description = "een Comparison met wholePeriod ('" + snippet(v) + "')";
        for (Expr side : java.util.Arrays.asList(v.links(), v.rechts()))
            if (side instanceof Expr.AttribuutRef a && navigationBindings.containsKey(a) && a.objecttype() != null
                    && a.attribuut() != null && declared(a.objecttype(), a.attribuut(), false).time() != null)
                throw new Refusal(description + " over een tijdlijn langs een pad ('" + snippet(a.bron()) + "'): de"
                        + " referentie-interpreter vouwt alleen een eigen tijdsafhankelijk attribuut van het onderwerp"
                        + " (RUNTIME-SEMANTICS ch. 3.9, 3.12)");
        boolean left = ownTimeline(v.links(), lez), right = ownTimeline(v.rechts(), lez);
        if (left == right)
            throw new Refusal(description + (left ? " tussen twee tijdlijnen" : " zonder eigen tijdsafhankelijk attribuut aan"
                    + " één kant") + ": de vouw toetst één tijdlijn van het onderwerp tegen een grens (RUNTIME-SEMANTICS"
                    + " ch. 3.9)");
        Expr.AttribuutRef time = (Expr.AttribuutRef) (left ? v.links() : v.rechts());
        Expr boundExpr = left ? v.rechts() : v.links();
        Integer testCode = Tijdrekening.toets(left ? v.op() : Operators.reverse(v.op()));
        if (testCode == null)
            throw new Refusal(description + " met een datumvergelijking: een datum over een gehele periode heeft geen lezing"
                    + " (RUNTIME-SEMANTICS ch. 3.9, 3.12)");
        Member member = timeMember(lez.subject, time.attribuut(), false, description);
        if (!member.datatype().isGetalachtig() || !eff.typeOf(boundExpr).isGetalachtig())
            throw new Refusal(description + " die geen getallen vergelijkt: de vouw toetst de waarde van een segment als getal"
                    + " (RUNTIME-SEMANTICS ch. 3.9)");
        int timeline = lez.timeline(member);
        Term bound = restated(waarde(boundExpr, lez), eff.unitOf(boundExpr), eff.unitOf(time),
                "een §8.2 tijdlijngrens");
        return wholePeriod(v.periode(), fullTimeline(timeline, member, null, lez, bron(time.bron())), testCode, bound,
                bron(v.bron()));
    }

    /**
     * The leeg or the gevuld test under the whole-period qualifier (RUNTIME-SEMANTICS ch. 3.9): whether the timeline of an
     * attribute reads leeg, or a value, on every day of every calendar year or month in which it reads a value
     * ({@link #wholePeriod}), the attribute the subject's own or that of the object a path of to-one roles reaches,
     * whose absent link gives the null timeline, which passes. The inner negation is per day, so a negated test is its
     * sister test; the loader already writes it so. A test of another kind and an operand that is no attribute on a
     * timeline are refused by name (the loader refuses a parameter as the operand, in the sentence of the other two
     * executors).
     */
    private Term wholePeriodPredicate(Conditie.Predicaat p, Reads lez) {
        String description = "een Predicate met wholePeriod ('" + snippet(p) + "')";
        boolean filled = switch (p.soort()) {
            case LEEG -> p.negatie();
            case GEVULD -> !p.negatie();
            default -> throw new Refusal(description + " van de soort " + Spelling.PREDICATE_KIND.spelling(p.soort()) + ": alleen"
                    + " de leeg- en de gevuldtoets hebben een lezing onder de kwalificatie (RUNTIME-SEMANTICS ch. 3.9)");
        };
        if (!(p.operand() instanceof Expr.AttribuutRef a) || a.objecttype() == null || a.attribuut() == null)
            throw new Refusal(description + " over iets anders dan een attribuut: de vouw leest de tijdlijn van een"
                    + " tijdsafhankelijk attribuut (RUNTIME-SEMANTICS ch. 3.9)");
        int testCode = filled ? FILLED_TEST : TEST_EMPTY;
        List<RolBindingRef> pad = navigationBindings.get(a);
        if (pad == null) {
            if (a.objecttype() != lez.subject)
                throw new Refusal(description + " over een eigen lezing die " + a.objecttype().naam + " noemt op een "
                        + lez.subject.naam + " (WF-053)");
            Member member = timeMember(lez.subject, a.attribuut(), false, description);
            return wholePeriod(p.periode(), fullTimeline(lez.timeline(member), member, null, lez, bron(a.bron())),
                    testCode, null, bron(p.bron()));
        }
        Path q = pad(lez.subject, pad, a.objecttype(), description);
        Member member = timeMember(q.reaches(), a.attribuut(), false, description);
        return wholePeriod(p.periode(), fullTimeline(lez.navigated(q, member), member, q, lez, bron(a.bron())),
                testCode, null, bron(p.bron()));
    }

    /**
     * The timeline of the attribute {@code lid} bound at slot {@code timeline}, which the whole-period fold reads whole,
     * or the fault where the case gives its input as the asOfDate-bucket scalar ({@link BucketScalar},
     * {@link StaticFunction#COMPLETE}): ch. 8.4 makes a form that needs the one convention and finds the other fault, never
     * guess. The fault names the object by the subject of the rule, through the roles of {@code pad} where the attribute
     * is read over one ("the werf of "S1""). Registers that the rule's evaluation can fault. The reference interpreter
     * and the ASP backend read such an input as no timeline, which passes (measured 2026-10-09).
     */
    private static Term fullTimeline(int timeline, Member member, Path pad, Reads lez, String bron) {
        int onderwerp = lez.member();
        lez.markFail();
        StringBuilder along = new StringBuilder();
        if (pad != null) for (int i = pad.roles().size() - 1; i >= 0; i--)
            along.append("the ").append(pad.roles().get(i)).append(" of ");
        String before = "the whole-period qualifier over \"" + member.naam() + "\" needs the full timeline; " + along;
        return complete(timeline, before, onderwerp, " supplies only the asOfDate-bucket scalar (ch. 3.9, ch. 8.4)", bron);
    }

    /** Whether {@code e} reads an attribute on a timeline of this rule's subject itself: an attribute reference without
     *  a path, naming the subject's type, whose declaration on that type's chain lies on a timeline. */
    private boolean ownTimeline(Expr e, Reads lez) {
        return e instanceof Expr.AttribuutRef a && a.objecttype() == lez.subject && a.attribuut() != null
                && !navigationBindings.containsKey(a) && declared(lez.subject, a.attribuut(), false).time() != null;
    }

    /** The tests of {@code rsGeheelPeriode}, by its own codes: a comparison takes the code {@link Tijdrekening#toets}
     *  gives its operator. */
    private static final int FILLED_TEST = 6, CHARACTERISTIC_TEST = 7, TEST_EMPTY = 8;

    /**
     * The fold of the whole-period qualifier (RUNTIME-SEMANTICS ch. 3.9) over the timeline bound at {@code timeline}, as
     * the runtime folds it ({@code rsGeheelPeriode}): for every calendar year or month the
     * timeline touches, the days on which the test holds must be all the days of that bucket. The grid is the
     * qualifier's own, "het gehele jaar" or "de gehele maand", never the member's grid nor the session's, so the fold
     * reads no evaluation date and a session without one answers. A leeg segment reads as a gap: it touches no bucket
     * and satisfies no test but the leeg test. A timeline without segments, and a null one, touch no bucket and pass,
     * so the qualifier's own negation ("niet gedurende …"), which complements the folded verdict, fails on them. The
     * bound of a comparison is read as a decimal ({@code toBd}), after the timeline.
     * The timeline of an attribute is read whole, so its input given as the asOfDate-bucket scalar faults
     * ({@link #fullTimeline}); a characteristic, whose input is its true periods, reads its timeline as it is.
     * {@code grens} is null where the test has no bound.
     */
    private static Term wholePeriod(Conditie.GeheelPeriode qualifier, Term timeline, int testCode, Term bound,
                                      String bron) {
        return new Term.Call(StaticFunction.WHOLE_PERIOD, List.of(timeline, bound == null ? constant(null) : bound,
                constant(qualifier.jaar() ? "JAAR" : "MAAND"), constant(testCode), constant(qualifier.niet())), bron);
    }

    /**
     * A role check (RUNTIME-SEMANTICS ch. 5.8a, IR-SPEC ch. 4.5): whether a link of the fact type holds the subject in
     * the role the check names and, where it names a counterpart type, an instance of that type at the other end, a
     * class test that a subtype passes; negated, whether no such link exists. An existence question, never a count and
     * never a pick, bound as a value the test reads, so the check stands in a composite as any condition does. A
     * relation a form creates is read over its {@link RelationLink} facts, which its creations state while the session
     * fires and which the input's links insert logically; any other relation is input and is read where the bean model
     * stores it ({@link FactModelGenerator#membershipOpslag}): a to-one other role is the subject's own reference
     * field, read off the subject's bean, and a to-many other role is the field of the other end, which names the
     * subject, counted over the beans of the counterpart's class. Both roles to-many leaves no field, and the check is
     * refused in the sentence of {@code FactModelGenerator.noReferenceFieldReason}, on both branches.
     */
    private Term roleCheck(Conditie.Rollidmaatschap r, Reads lez) {
        RolBindingRef rb = roleBindings.get(r);
        RegelSpraakModel.Rol[] pair = FactModelGenerator.boundRolePair(model, rb);
        if (pair == null)
            throw notRead("RoleMembership", r, "de binding (" + (rb == null ? "geen" : rb.feittype() + ", " + rb.rol())
                    + ") noemt geen gedeclareerde rol");
        Objecttype counterpart = dev.uchar.regelspraak.loader.IrLoader.counterpart(r);
        Class<?> javaClass = counterpart == null ? null : beanClass(counterpart);
        boolean negated = r.negatie();
        if (rb.feittype() != null && createdRelations.contains(Namen.norm(rb.feittype()))) {
            RegelSpraakModel.Feittype ft = feittype(rb.feittype());
            if (ft == null || ft.rollen.size() != 2 || ft.wederkerig)
                throw notRead("RoleMembership", r, "over de gecreëerde relatie '" + rb.feittype() + "', die geen"
                        + " relatie van twee rollen is: " + STILL_NOT);
            boolean subjectIsFirst = Namen.norm(ft.rollen.get(0).naam()).equals(Namen.norm(pair[0].naam()));
            int i = lez.roleLinks(Namen.norm(ft.naam), subjectIsFirst, javaClass);
            return new Term.Call(StaticFunction.ROLE_COUNT, List.of(new Term.SlotValue(i, null), constant(negated)),
                    bron(r.bron()));
        }
        FactModelGenerator.MembershipStorage o = FactModelGenerator.membershipStorage(model, rb);
        if (o.refusal() != null) throw notRead("RoleMembership", r, o.refusal());
        if (o.opSubject()) {
            if (!FactModelGenerator.hasReferenceField(model, lez.subject, o.beanField(), o.otherType()))
                throw notRead("RoleMembership", r, FactModelGenerator.noReferenceFieldReason(model, lez.subject,
                        o.beanField(), o.otherType()) + " (§13.3.9 opslagrichting)");
            int i = lez.roleField(o.beanField(), javaClass);
            return new Term.Call(StaticFunction.ROLE_FIELD, List.of(new Term.SlotValue(i, null), constant(negated)),
                    bron(r.bron()));
        }
        Objecttype filler = counterpart != null ? counterpart : o.otherType();
        if (!FactModelGenerator.hasReferenceField(model, filler, o.beanField(), lez.subject))
            throw notRead("RoleMembership", r, FactModelGenerator.noReferenceFieldReason(model, filler,
                    o.beanField(), lez.subject) + " (§13.3.9 opslagrichting)");
        int i = lez.roleFillers(beanClass(filler), o.beanField());
        return new Term.Call(StaticFunction.ROLE_COUNT, List.of(new Term.SlotValue(i, null), constant(negated)),
                bron(r.bron()));
    }

    /** A composite folds its members in wire order and stops at the first member after which the verdict stands
     *  (RUNTIME-SEMANTICS ch. 5.10), so a member that would fault, a comparison with a division by zero, is not read
     *  where an earlier member settled the verdict, and the composite faults only where it reads such a member. */
    private Term compound(Conditie.Samengesteld s, Reads lez) {
        // a window under GEEN or a counting quantifier has no reading (WF-112), refused in the sentence both other
        // branches give
        String window = WindowPosition.refusal(s);
        if (window != null) throw new Refusal(window + ": " + snippet(s));
        List<Term> members = new ArrayList<>();
        for (Conditie part : s.onderdelen()) members.add(voorwaarde(part, lez));
        return new Term.Compound(s.kwantor(), s.drempel(), members, bron(s.bron()));
    }

    /**
     * A quantifier fold (RUNTIME-SEMANTICS ch. 3.9a, IR-SPEC ch. 4.5): its {@code test}, the matrix, asked of every
     * contributor, a member of the range that passes the {@code filter}, each with the member as the condition's
     * subject, and counted under the quantifier: {@code all} holds where every contributor holds the matrix, {@code none}
     * where none does, the three counting quantifiers on the number that do, and over no contributor all and none hold.
     * The contributors are an aggregation's ({@link #quantifierContributions}): a contribution rule per member that passes the
     * filter, with the member's reads of the filter and the matrix bound in its left-hand side, so a member whose derived
     * value changes takes its contribution along, and an accumulate over the contributions in the left-hand side of the
     * rule that reads the fold. Where the matrix can neither fault nor deposit, a contribution carries 1 or 0 for the
     * matrix's verdict and the accumulate counts the contributors and sums the verdicts: the order of the members is then
     * invisible, and the verdict a call over the count and the sum ({@link StaticFunction#QUANTIFIER_VERDICT}). Where it can, the
     * order shows (ch. 5.10, 5.11): the reference interpreter reads the matrix in the contributors' order and stops at
     * the first contributor after which the verdict stands, so a contributor it does not reach raises no fault and
     * deposits nothing. A contribution then carries the matrix's outcome and the member's place ({@link MemberTest}), the
     * accumulate collects them, and the evaluation replays that order ({@link Term.QuantifierRead}). The filter is settled
     * for every member before the matrix, as for any fold ({@link #readFold}). The range is one step,
     * {@code sourceRole}: the loader refuses a multi-step one (WF-108 (c)), and so does this method where a hand-built
     * model carries one.
     */
    private Term quantifierFold(Conditie.Kwantorvouw kv, Reads lez) {
        // WF-112, in the sentence of Vensterpositie, on both branches: a window in the filter or the test under none
        // or a counting quantifier has no reading
        String window = WindowPosition.refusal(kv);
        if (window != null) throw new Refusal(window + ": " + snippet(kv));
        Objecttype elem = kv.elementObjecttype();
        if (elem == null) throw notRead("QuantifierFold", kv, "zonder opgelost elementtype");
        if (kv.toets() == null)
            throw notRead("QuantifierFold", kv, "zonder matrix: een kwantor over geen voorwaarde heeft geen oordeel"
                    + " (WF-108 (d))");
        if (kv.kwantor() == Conditie.Kwantor.DE)
            throw notRead("QuantifierFold", kv, "onder de kwantor DE, die één lid noemt en geen verzameling"
                    + " kwantificeert (WF-108 (a))");
        if (kv.bronRol() == null || !kv.bronPad().isEmpty())
            throw notRead("QuantifierFold", kv, "zonder bereik van één stap: de lader weigert een bereik van meer"
                    + " stappen (WF-108 (c))");
        Conditie.Kwantor kw = kv.kwantor();
        int threshold = kv.drempel();
        Fold v = foldFor(kv, lez, (sleutel, naam) -> quantifierContributions(kv, sleutel, naam, lez.subject, elem));
        String bron = bron(kv.bron());
        if (!v.ordered()) {
            int[] i = lez.fold(v, Accumulation.Kind.SUM);
            return readFold(v, i, lez, new Term.Call(StaticFunction.QUANTIFIER_VERDICT, List.of(new Term.SlotValue(i[0], null),
                    new Term.SlotValue(i[1], null), constant(kw), constant(threshold)), bron), bron);
        }
        int[] i = lez.fold(v, Accumulation.Kind.LIST);
        if (v.memberCanFail()) lez.markFail();
        int depositFlag = v.memberDeposits() ? lez.depositFlag() : -1;
        return readFold(v, i, lez, new Term.QuantifierRead(StaticFunction.QUANTIFIER_READ, new Term.SlotValue(i[0], null),
                new Term.SlotValue(i[1], null), kw, threshold, depositFlag, bron), bron);
    }

    /**
     * The contribution rules of the quantifier fold {@code kv} on {@code subject}, planned: one over its range, the
     * fold's subject bound first and the member joined to it, the member's reads of the filter and of the matrix bound,
     * the filter as the test (with the role {@link TestItem.Role#ZONDER_FOUT} where it can fault), and a consequence
     * that evaluates the matrix for the member and inserts the member's {@link Contribution} logically: 1 or 0 for its
     * verdict ({@link Consequence.Input.Verdict}), or, where the matrix can fault or deposit, its {@link MemberTest}
     * ({@link Consequence.Input.MemberTest}), with the member's place bound as the filter fault rule binds it
     * ({@link Range#perRoute}) and the rule's probe. Beside it the filter fault rule and the filter deposit rule of a
     * filter that can fault or deposit, as for any fold ({@link #contributionRules}). A made link has no place in the order
     * of the members, so a matrix that can fault or deposit over a relation a form creates is refused.
     */
    private Fold quantifierContributions(Conditie.Kwantorvouw kv, String sleutel, String naam, Objecttype subject,
                                  Objecttype elem) {
        String description = snippet(kv);
        Range range = oneStepRange(kv.bronRol(), subject, elem, description);
        // the member's reads first and its join after: whether the matrix can fault or deposit decides whether the join
        // binds the member's place
        Reads lez = contributionReads(subject, elem, l -> { });
        Set<Integer> read = lez.follow();
        Term filter = kv.filter() == null ? null : voorwaarde(kv.filter(), lez);
        boolean filterCanFail = lez.canFail();
        lez.stop();
        Set<Integer> use = lez.follow();
        int member = lez.member();
        Term matrix = voorwaarde(kv.toets(), lez);
        boolean memberCanFail = lez.canFail();
        lez.stop();
        boolean memberDeposits = deposits(kv.toets());
        int depositFlag = lez.depositFlagSlot();
        boolean ordered = memberCanFail || memberDeposits;
        List<Consequence.FoldPlace.Step> places = new ArrayList<>();
        if (ordered) {
            if (kv.bronRol().feittype() != null && createdRelations.contains(Namen.norm(kv.bronRol().feittype())))
                throw new Refusal("een lidvoorwaarde die kan falen of deponeren in een QuantifierFold over de gecreëerde"
                        + " relatie '" + kv.bronRol().feittype() + "' ('" + description + "'): een gemaakte link heeft geen plaats"
                        + " in de volgorde van de leden, die bepaalt welk lid de kortsluiting nog bereikt"
                        + " (RUNTIME-SEMANTICS ch. 3.9a, 5.10); " + STILL_NOT);
            places.addAll(range.perRoute().apply(lez));
            for (Consequence.FoldPlace.Step p : places) {
                use.add(p.aantal());
                use.add(p.place());
            }
        } else range.join().accept(lez);
        List<LhsItem> items = lez.plan(conditionRole(filterCanFail), filter, read, kv.filter());
        Consequence.Input input = ordered
                ? new Consequence.Input.MemberTest(matrix, depositFlag, new Consequence.FoldPlace(0, places,
                        Consequence.PlaceReason.QUANTIFIER))
                : new Consequence.Input.Verdict(matrix);
        String formName = model.geëmitteerdeRegelNaam(vorm);
        List<RulePlan> rules = new ArrayList<>();
        List<Reads> lezingen = new ArrayList<>();
        rules.add(lez.rulePlan(formName + ": bijdrage aan " + naam, items, new Consequence.Contribution(FOLD_SUBJECT,
                sleutel, member, input, List.copyOf(use), lez.size(), Consequence.Insertion.LOGICAL)));
        lezingen.add(lez);
        if (filterCanFail) rules.add(filterFaultRule(sleutel, formName + ": filterfout in " + naam, subject, elem,
                range, 0, kv.filter(), lezingen::add));
        boolean filterDeposits = deposits(kv.filter());
        if (filterDeposits) rules.add(filterDepositRule(sleutel, formName + ": filterdeponering in " + naam,
                subject, elem, range, kv.filter(), lezingen::add));
        return new Fold(sleutel, List.copyOf(rules), List.copyOf(lezingen), filterCanFail, filterDeposits,
                memberCanFail, memberDeposits);
    }

    /**
     * A comparison of two values the rule binds or computes, with the empty operand of RUNTIME-SEMANTICS ch. 1.2:
     * numbers through {@code rsAggVergelijk} (an ordering with a leeg operand is onwaar, two leeg numbers are equal,
     * exactly one leeg is unequal; exact where a value is a rational), a percentage being a number in the unit %, the
     * right operand restated into the left one's unit (RUNTIME-SEMANTICS ch. 2.6); dates of one precision through
     * {@link ComputeHelpers#dateCompare} (two leeg dates are not equal); a text, an enumeration label or a truth value
     * through {@link ComputeHelpers#textEqual}, an enumeration up to its normalised label. The left operand is evaluated before the right
     * one (ch. 5.11). Two sides whose types allow no comparison are refused. Under the whole-period qualifier the
     * comparison folds a timeline instead ({@link #wholePeriodComparison}).
     */
    private Term comparison(Conditie.Vergelijking v, Reads lez) {
        if (v.periode() != null) return wholePeriodComparison(v, lez);
        Datatype lt = eff.typeOf(v.links()), rt = eff.typeOf(v.rechts());
        Conditie.Operator op = v.op();
        Term leftTerm = waarde(v.links(), lez), rightTerm = waarde(v.rechts(), lez);
        String bron = bron(v.bron());
        if (lt.isGetalachtig() && rt.isGetalachtig() && !isDateOperator(op)) {
            Term right = restated(rightTerm, eff.unitOf(v.rechts()), eff.unitOf(v.links()),
                    "een §5.1 vergelijking");
            return new Term.Call(StaticFunction.RS_AGG_COMPARE, List.of(leftTerm, right,
                    constant(Operators.symbol(op))), bron);
        }
        if (lt.isDatum() && rt.isDatum() && (isDateOperator(op) || op == Conditie.Operator.GELIJK
                || op == Conditie.Operator.ONGELIJK)) {
            if (!sameDateKind(lt, rt))
                throw new Refusal("een Comparison van een datum met een datum-tijd ('" + snippet(v) + "', §5.3 Tabel 36)");
            return new Term.Call(StaticFunction.DATE_COMPARE, List.of(leftTerm, rightTerm, constant(op)), bron);
        }
        if ((op == Conditie.Operator.GELIJK || op == Conditie.Operator.ONGELIJK) && equalityType(lt)
                && equalityType(rt) && lt.isBoolean() == rt.isBoolean()) {
            boolean enumeration = lt instanceof Datatype.Enumeratie || rt instanceof Datatype.Enumeratie;
            Term left = enumeration ? enumName(leftTerm) : leftTerm;
            Term right = enumeration ? enumName(rightTerm) : rightTerm;
            return new Term.Call(StaticFunction.TEXT_EQUAL, List.of(left, right,
                    constant(op == Conditie.Operator.GELIJK)), bron);
        }
        throw new Refusal("een Comparison " + Spelling.COMPARISON_OP.spelling(op) + " tussen " + typeName(lt) + " en "
                + typeName(rt) + " ('" + snippet(v) + "'): de typen van de twee kanten laten deze vergelijking niet toe"
                + " (RUNTIME-SEMANTICS ch. 1.2)");
    }

    /** {@code t}, an enumeration label or leeg, up to its normalisation ({@link StaticFunction#RS_ENUM_NAME}); the call has no
     *  node of its own in the IR and carries the fragment of the operand it normalises, as a restatement does. */
    private static Term enumName(Term t) {
        return new Term.Call(StaticFunction.RS_ENUM_NAME, List.of(t), t.bron());
    }

    private static String typeName(Datatype t) {
        return t == null ? "onbekend" : t.getClass().getSimpleName();
    }

    private static boolean equalityType(Datatype t) {
        return t != null && (t.isTekst() || t instanceof Datatype.Enumeratie || t.isBoolean());
    }

    private static boolean isDateOperator(Conditie.Operator op) {
        return switch (op) {
            case EERDER, EERDER_OF_GELIJK, LATER, LATER_OF_GELIJK -> true;
            default -> false;
        };
    }

    private static boolean sameDateKind(Datatype a, Datatype b) {
        if (a instanceof Datatype.DatumTijd) return b instanceof Datatype.DatumTijd;
        return a instanceof Datatype.DatumInDagen && b instanceof Datatype.DatumInDagen;
    }

    /** A leeg or gevuld test of a value; a read over an absent link is leeg (RUNTIME-SEMANTICS ch. 5.8 rule 1). The loader
     *  writes a negated test as its sister test. Under the whole-period qualifier the test folds a timeline instead
     *  ({@link #wholePeriodPredicate}). */
    private Term predicate(Conditie.Predicaat p, Reads lez) {
        if (p.periode() != null) return wholePeriodPredicate(p, lez);
        switch (p.soort()) {
            case LEEG, GEVULD -> {
                Term operand = waarde(p.operand(), lez);
                return new Term.Call(StaticFunction.EMPTY_TEST, List.of(operand,
                        constant(p.soort() == Conditie.Predicaatsoort.LEEG)), bron(p.bron()));
            }
            case ELFPROEF, NUMERIEK_MET_EXACT -> {
                return digitTrial(p, lez);
            }
            // a day kind is consulted as a classifier of a bare date before the rule graph, never as a read inside the
            // fixpoint (ch. 5.4); inside a day-kind definition the test is the definition's own (dagsoortpredikaat)
            case DAGSOORT -> throw new Refusal("een Predicate dayKind als voorwaarde ('" + snippet(p) + "'): de"
                    + " referentie-interpreter en de ASP-backend weigeren haar met naam buiten een dagsoortdefinitie"
                    + " (RUNTIME-SEMANTICS ch. 3.12, 5.4)");
            default -> throw new IllegalStateException("predicaatsoort " + p.soort());
        }
    }

    /** The eleven test and the exact-digit test (§8.1.3, §8.1.4), through the runtime's
     *  {@code rsElfproef} and {@code rsNumeriekMetExact}, the written negation complementing the verdict: a leeg value
     *  passes neither test. The operand is read as any value, in which an absent link over a path of roles is leeg
     *  (RUNTIME-SEMANTICS ch. 5.8, rule 1), so the test cannot tell an absent link from a leeg value there; such an
     *  operand is refused by name. The reference interpreter and the ASP backend do not evaluate these predicates. */
    private Term digitTrial(Conditie.Predicaat p, Reads lez) {
        if (p.operand() instanceof Expr.AttribuutRef a && navigationBindings.containsKey(a))
            throw new Refusal("een Predicate " + Spelling.PREDICATE_KIND.spelling(p.soort()) + " over een pad ('"
                    + snippet(p) + "'): de operand wordt als een waarde gelezen, waarin een afwezige link leeg is"
                    + " (RUNTIME-SEMANTICS ch. 5.8, regel 1), zodat de toets een afwezige link hier niet van een lege"
                    + " waarde kan onderscheiden");
        Term operand = waarde(p.operand(), lez);
        boolean desired = !p.negatie();
        if (p.soort() == Conditie.Predicaatsoort.ELFPROEF)
            return new Term.Call(StaticFunction.ELEVEN_TEST, List.of(operand, constant(desired)), bron(p.bron()));
        return new Term.Call(StaticFunction.NUMERIEK_MET_EXACT, List.of(operand, constant(p.cijfers()), constant(desired)),
                bron(p.bron()));
    }

    // ---------------- the reads of one rule ----------------

    /**
     * The reads of one rule, each bound once in its left-hand side and numbered in the order they are met: a member of
     * the subject that no form writes off the subject's bean pattern, over a path through the reference fields read in
     * the same binding; a derived member of the subject off its standing; a derived member over a path off its
     * {@link PathState}; a parameter through a declaration of its own. The numbers are the indices of the value arrays
     * the condition's test and the consequence receive. The standings and path standings a rule reads are registered
     * with the transpiler only when the rule lowers ({@link #recorder}).
     *
     * <p>It decides what the rule reads and plans it: a {@link dev.uchar.regelspraak.transpiler.tms.plan.Read} per
     * slot (the plan's record, which the record {@link Read} of a read of the fixpoint shadows here), and the items of
     * the left-hand side in the closed vocabulary of {@code transpiler/tms/plan} ({@link LhsItem}). It builds no
     * pattern itself: the plan of the rule ({@link #rulePlan}) carries every item, and a rendering makes the rule of it.
     */
    private final class Reads {
        final Objecttype subject;
        /** The slots of the value array, as the plan reads them. */
        private final List<dev.uchar.regelspraak.transpiler.tms.plan.Read> slots = new ArrayList<>();
        /** The values bound off the subject pattern. */
        private final List<Binding> subjectBindings = new ArrayList<>();
        /** The constraints of the subject pattern on the subject bean, which the form sets ({@link #addSubjectAlpha}). */
        private final List<Alpha> subjectAlpha = new ArrayList<>();
        /** The parameter items of the left-hand side: parameters, the evaluation date, the validity gate, the phase. */
        private final List<LhsItem> parameters = new ArrayList<>();
        /** The standing items of the left-hand side: standings, folds and the other accumulates, channels. */
        private final List<LhsItem> states = new ArrayList<>();
        private final Map<String, Integer> index = new LinkedHashMap<>();
        /** The variables of this rule's slots, so that a read name ({@link #readName}) is one no other slot has. */
        private final Set<String> names = new HashSet<>();
        private final Map<ElementSleutel, Member> derived = new LinkedHashMap<>();
        private final Map<String, PathRead> paths = new LinkedHashMap<>();
        private final Set<String> channelNames = new LinkedHashSet<>();
        /** The indices the expression being compiled reads, or null outside such a compilation. */
        private Set<Integer> use;
        /** Whether an expression compiled since the last {@link #follow} can raise an evaluation fault. */
        private boolean fail;
        /** The planned items before the subject pattern: the pattern of the source of an allocation's recipient rule,
         *  and of the fold's subject in a contribution rule ({@link #addToHeadPlan}). */
        private final List<LhsItem> headPlan = new ArrayList<>();
        /** The identity joins of the subject pattern to an object a planned item before it binds
         *  ({@link #subjectJoin}). */
        private final List<Join> subjectJoins = new ArrayList<>();
        /** The other constraints of the subject pattern on such an object: the meeting of the two walks of a fold's
         *  range of more steps ({@link #subjectBeta}). */
        private final List<BetaTest> subjectBetaTests = new ArrayList<>();
        /** The planned items right after the subject pattern: the range of an allocation over a relation a form
         *  creates, and in a contribution rule the intermediates of a range of more steps and the accumulates of the
         *  member's places ({@link #addToRangePlan}). */
        private final List<LhsItem> rangePlan = new ArrayList<>();
        /** The folds this rule reads, registered with it ({@link #recorder}). */
        private final List<Fold> folds = new ArrayList<>();
        /** The binders of the {@code Let}s around the expression being compiled, by normalised name, each to its slot
         *  in the value array ({@link #binder}); a nested {@code Let} of the same name shadows the outer one while its
         *  body compiles (WF-064). */
        final Map<String, Integer> binders = new HashMap<>();
        /** In a contribution rule, the binders in scope at its fold, by normalised name: those of the {@code Let}s
         *  around the fold in the rule that reads it, which bind them there, and for a fold in a fold's filter those in
         *  scope at the outer fold as well. This rule binds none of their values; empty in every other rule. */
        Set<String> outside = Set.of();
        /** The slot of the probe of this rule's deposits ({@link #depositFlag}), -1 while no place that deposits compiled. */
        private int depositFlag = -1;

        /** The reads of a rule on {@code subject}, which its plan carries ({@link #rulePlan}). */
        Reads(Objecttype subject) {
            this.subject = subject;
        }

        /** The binders in scope where the expression being compiled stands: this rule's own and {@link #outside}. */
        Set<String> binderRange() {
            if (binders.isEmpty()) return outside;
            Set<String> out = new LinkedHashSet<>(outside);
            out.addAll(binders.keySet());
            return out;
        }

        /** The subject itself as a bound value, for a consequence that hands on another object beside it. */
        int member() {
            int known = existing("onderwerp zelf");
            return known >= 0 ? known : allocateSlot(ReadKind.SUBJECT, "onderwerp zelf", SUBJECT);
        }

        /** The left-hand side of the rule after its subject pattern, once its condition and its value are compiled:
         *  the parameter items, the standing items, and the test of {@code voorwaarde} over the values at
         *  {@code read}, which asks {@code rol} of it ({@link TestItem.Role}); {@link #rulePlan} takes them with
         *  what the form adds after them. A test of deposits takes the probe of this rule ({@link #depositFlagSlot}). */
        List<LhsItem> plan(TestItem.Role rol, Term testTerm, Set<Integer> read, Conditie voorwaarde) {
            return plan(rol, testTerm, read, voorwaarde, List.of());
        }

        /** As {@link #plan(TestItem.Role, Term, Set, Conditie)}, with {@code control} between the standing items and the
         *  test: the guard of a creation, whose ids the analysis draws before the test's, or the control-fact patterns
         *  of a default, so that a default a higher one displaced is not tested at all. */
        List<LhsItem> plan(TestItem.Role rol, Term testTerm, Set<Integer> read, Conditie voorwaarde,
                           List<LhsItem> control) {
            TestItem test = testItem(rol, testTerm, read, voorwaarde);
            List<LhsItem> out = new ArrayList<>(parameters);
            out.addAll(states);
            out.addAll(control);
            if (test != null) out.add(test);
            return out;
        }

        /** The test of {@code voorwaarde}, compiled into {@code toets}, over the values at {@code read}, asking
         *  {@code rol} of it; null where the rule has no condition. */
        private TestItem testItem(TestItem.Role rol, Term testTerm, Set<Integer> read, Conditie voorwaarde) {
            if (testTerm == null) return null;
            int[] pos = positions(read);
            if (pos.length > MAX_READS)
                throw new Refusal("een voorwaarde die " + pos.length + " waarden leest ('" + snippet(voorwaarde)
                        + "'): de toets van deze tak neemt hoogstens " + MAX_READS + " gebonden waarden");
            return new TestItem(id("voorwaarde"), rol, testTerm, rol == TestItem.Role.DEPOSIT ? depositFlag : -1,
                    List.copyOf(read), slots.size());
        }

        /** The plan of this rule, {@code naam}, of the form being lowered: the planned items before the subject
         *  pattern, the subject pattern with its constraints, joins, other constraints and bindings as they stand now,
         *  the planned items right after it, {@code items} after those, and {@code consequence}. */
        RulePlan rulePlan(String naam, List<LhsItem> items, Consequence consequence) {
            return rulePlan(naam, null, items, consequence);
        }

        /** As {@link #rulePlan(String, List, Gevolg)}, for a rule of the agenda group {@code agendaGroup}. */
        RulePlan rulePlan(String naam, String agendaGroup, List<LhsItem> items, Consequence consequence) {
            List<LhsItem> lhs = new ArrayList<>(headPlan);
            lhs.add(new Subject(beanClass(subject), SUBJECT, subjectAlpha, subjectJoins, subjectBetaTests,
                    subjectBindings));
            lhs.addAll(rangePlan);
            lhs.addAll(items);
            return new RulePlan(naam, vorm, agendaGroup, new ReadTable(slots), withoutUnfilledDepositFlag(lhs),
                    withoutUnfilledDepositFlag(consequence));
        }

        /** The name of the variable that binds slot {@code slot}, which an item of the plan names to read it. */
        String variable(int slot) {
            String naam = slots.get(slot).naam();
            if (naam == null) throw new IllegalStateException("slot " + slot + " wordt door geen patroon gebonden");
            return naam;
        }

        /** Plans {@code item} before the subject pattern. */
        void addToHeadPlan(LhsItem item) {
            headPlan.add(item);
        }

        /** Plans the identity join {@code j} on the subject pattern, after its constraints. */
        void subjectJoin(Join j) {
            subjectJoins.add(j);
        }

        /** Plans the constraint {@code t} between the subject and an object a planned item before it binds on the
         *  subject pattern, after its joins. */
        void subjectBeta(BetaTest t) {
            subjectBetaTests.add(t);
        }

        /** Plans {@code item} right after the subject pattern. */
        void addToRangePlan(LhsItem item) {
            rangePlan.add(item);
        }

        /** The number of slots of the value array so far. */
        int size() {
            return slots.size();
        }

        /** Adds the constraint {@code a} on the subject bean to the subject pattern, after the ones before it. */
        void addSubjectAlpha(Alpha a) {
            subjectAlpha.add(a);
        }

        /** The number of the other writers of an initialisation's target that wrote leeg on the subject, bound off an
         *  accumulate the form plans ({@link TmsTranspiler#initialisationRule}). */
        int emptyWriters() {
            return allocateSlot(ReadKind.EMPTY_WRITERS, "lege schrijvers", "$lege_schrijvers");
        }

        Set<Integer> follow() {
            use = new TreeSet<>();
            fail = false;
            return use;
        }

        void stop() { use = null; }

        /** Records that the expression being compiled can raise an evaluation fault. */
        void markFail() { fail = true; }

        /** Whether an expression compiled since the last {@link #follow} can raise an evaluation fault: a condition that
         *  can is tested with the role {@link TestItem.Role#ZONDER_FOUT}, beside the rule that inserts its fault. */
        boolean canFail() { return fail; }

        /** Registers what the lowered rule reads with the transpiler: the standings, path standings and channel
         *  standings it needs, and the contribution rules of its folds with what those read. */
        void recorder() {
            read.putAll(derived);
            for (PathRead p : paths.values()) {
                pathReads.putIfAbsent(p.sleutel(), p);
                if (writers.containsKey(p.member().sleutel()) || timelineAttribute(p.member()))
                    read.putIfAbsent(p.member().sleutel(), p.member());
            }
            channels.addAll(channelNames);
            for (Fold v : folds) {
                if (contributionRules.putIfAbsent(v.sleutel(), v.rules()) == null)
                    for (RulePlan r : v.rules()) formOfRule.put(r.naam(), vorm);
                for (Reads l : v.lezingen()) l.recorder();
            }
        }

        /**
         * A fold of this rule's subject, bound off the contributions of the fold {@code sleutel} ({@link Contribution}): one
         * accumulate over them that counts them and, where {@code function} names a fold, folds their values with it;
         * where the fold's filter can fault, a second accumulate over its {@link FilterFault} facts on this subject, which
         * binds the least of them, the first in fold order, or null; where the filter can deposit, an accumulate that
         * counts its {@link FilterDeposit} facts on this subject; and where a member's value can fault, an
         * accumulate over its {@link MemberFault} facts on this subject, which binds the least of them in the same way.
         * Returns the index of the count, that of the folded value, that of the least filter fault, that of the count of
         * filter deposits and that of the least member fault, each -1 without one. An accumulate over no fact still binds
         * a row, through the count, so the rule reads an empty fold, and a fold without a filter fault, a filter deposit
         * or a member fault, as well.
         */
        int[] fold(Fold v, Accumulation.Kind function) {
            folds.add(v);
            String sleutel = v.sleutel();
            int known = existing("vouw " + sleutel);
            if (known >= 0) return new int[]{known, function == null ? -1 : existing("vouwwaarde " + sleutel),
                    v.canFail() ? existing("filterfout " + sleutel) : -1,
                    v.deposits() ? existing("filterdeponering " + sleutel) : -1,
                    v.valueCanFail() ? existing("lidfout " + sleutel) : -1};
            String b = "$bijdrage_" + id("vouw");
            Alpha ofFold = equal(id("vouw"), "fold", sleutel);
            Join onSubject = onSubject(id("vouw-onderwerp"));
            String aantal = "$aantal_" + id("vouw");
            int[] out;
            if (function == null) {
                states(new Accumulation(new FactPattern(Contribution.class, b, List.of(ofFold), List.of(onSubject), List.of()),
                        List.of(new Accumulation.Aggregation(Accumulation.Kind.COUNT, null, aantal))));
                out = new int[]{allocateSlot(ReadKind.FOLD_COUNT, "vouw " + sleutel, aantal), -1, -1, -1, -1};
            } else {
                String contributionValue = "$bijdragewaarde_" + id("vouw");
                String outcome = "$vouw_" + id("vouw");
                states(new Accumulation(new FactPattern(Contribution.class, b, List.of(ofFold), List.of(onSubject),
                        List.of(Binding.van(contributionValue, Contribution.class, "waarde"))),
                        List.of(new Accumulation.Aggregation(Accumulation.Kind.COUNT, null, aantal),
                                new Accumulation.Aggregation(function, contributionValue, outcome))));
                out = new int[]{allocateSlot(ReadKind.FOLD_COUNT, "vouw " + sleutel, aantal),
                        allocateSlot(ReadKind.FOLDED_VALUE, "vouwwaarde " + sleutel, outcome), -1, -1, -1};
            }
            if (v.canFail()) {
                String f = "$filterfout_" + id("vouw");
                Alpha ofFilter = equal(id("filterfout"), "fold", sleutel);
                Join filterOnSubject = onSubject(id("filterfout-onderwerp"));
                String fouten = "$filterfouten_" + id("vouw");
                String eerste = "$eerste_filterfout_" + id("vouw");
                states(new Accumulation(new FactPattern(FilterFault.class, f, List.of(ofFilter), List.of(filterOnSubject),
                        List.of()), List.of(new Accumulation.Aggregation(Accumulation.Kind.COUNT, null, fouten),
                        new Accumulation.Aggregation(Accumulation.Kind.MINIMUM, null, eerste))));
                out[2] = allocateSlot(ReadKind.FILTER_FAULT, "filterfout " + sleutel, eerste);
            }
            if (v.deposits()) {
                String d = "$filterdeponering_" + id("vouw");
                Alpha ofDeposit = equal(id("filterdeponering"), "fold", sleutel);
                Join depositOnSubject = onSubject(id("filterdeponering-onderwerp"));
                String depositCount = "$filterdeponeringen_" + id("vouw");
                states(new Accumulation(new FactPattern(FilterDeposit.class, d, List.of(ofDeposit),
                        List.of(depositOnSubject), List.of()),
                        List.of(new Accumulation.Aggregation(Accumulation.Kind.COUNT, null, depositCount))));
                out[3] = allocateSlot(ReadKind.FILTER_DEPOSIT, "filterdeponering " + sleutel, depositCount);
            }
            if (v.valueCanFail()) {
                String f = "$lidfout_" + id("vouw");
                Alpha ofMember = equal(id("lidfout"), "fold", sleutel);
                Join memberOnSubject = onSubject(id("lidfout-onderwerp"));
                String fouten = "$lidfouten_" + id("vouw");
                String eerste = "$eerste_lidfout_" + id("vouw");
                states(new Accumulation(new FactPattern(MemberFault.class, f, List.of(ofMember), List.of(memberOnSubject),
                        List.of()), List.of(new Accumulation.Aggregation(Accumulation.Kind.COUNT, null, fouten),
                        new Accumulation.Aggregation(Accumulation.Kind.MINIMUM, null, eerste))));
                out[4] = allocateSlot(ReadKind.MEMBER_FAULT, "lidfout " + sleutel, eerste);
            }
            return out;
        }

        /** The session's evaluation date (RUNTIME-SEMANTICS ch. 8.1), bound off its {@link EvaluationDate}: one fact for the
         *  whole session, joined to no subject, bound among the parameters; null where the case carries no date. */
        int rekendatum() {
            int known = existing("rekendatum");
            if (known >= 0) return known;
            evaluationDateNeeded();
            String r = "$rekendatum_" + slots.size();
            String d = "$rekendatumwaarde_" + slots.size();
            parameters(new FactPattern(EvaluationDate.class, r, List.of(), List.of(),
                    List.of(Binding.van(d, EvaluationDate.class, "date"))));
            return allocateSlot(ReadKind.REKENDATUM, "rekendatum", d);
        }

        /** Gates this rule on the evaluation date (RUNTIME-SEMANTICS ch. 8.2): it matches only where the validity
         *  window {@code g} admits the date, both bounds inclusive and an absent one open (IR-SPEC ch. 3.10), and never
         *  in a session without a date, where the form faults instead ({@link #validityFault}). A window that is always
         *  valid adds nothing. The date is constant while the session fires, so the gate never changes a match. */
        Reads valid(Geldigheid g) {
            if (g == null || g.altijd()) return this;
            evaluationDateNeeded();
            String r = "$geldigheid_" + slots.size();
            parameters(new FactPattern(EvaluationDate.class, r, List.of(new Alpha(id("geldig"),
                    List.of(new Alpha.FieldTest("date", Alpha.Comparator.WITHIN, g)))), List.of(), List.of()));
            return this;
        }

        /** A slot of the value array for the binder of a {@code Let} ({@link TmsTranspiler#let}): no pattern binds it, so
         *  it is no position a condition reads or a consequence binds, and the array is sized past it. */
        int binder() {
            return unbound(ReadKind.BINDER);
        }

        /** The slot of the probe of this rule's deposits ({@link DepositFlag}), made at the first place that deposits; like a
         *  binder's slot no pattern binds it. */
        int depositFlag() {
            if (depositFlag < 0) depositFlag = unbound(ReadKind.DEPOSIT_FLAG);
            return depositFlag;
        }

        /** The slot of the probe, or -1 where no place that deposits compiled. */
        int depositFlagSlot() {
            return depositFlag;
        }

        /** The number of the {@link CreationPhase} the driver opened, for a creation rule of stratum {@code stratum}: one
         *  fact for the whole session, joined to no subject, bound among the parameters, and only a phase of this
         *  stratum or one above, as a pass of the reference interpreter over stratum k runs every form of stratum k or
         *  below (RUNTIME-SEMANTICS ch. 6.3). */
        int creationPhase(int stratum) {
            int known = existing("creatiefase");
            if (known >= 0) return known;
            parameters(new FactPattern(CreationPhase.class, "$creatiefase", List.of(new Alpha(id("creatiestratum"),
                    List.of(new Alpha.FieldTest("stratum", Alpha.Comparator.AT_LEAST, stratum)))), List.of(),
                    List.of(Binding.van("$fase", CreationPhase.class, "number"))));
            return allocateSlot(ReadKind.CREATION_PHASE, "creatiefase", "$fase");
        }

        /** The marks of the creation that made this rule's subject, the {@link CreationStamp} facts whose object is the
         *  subject, bound as a list off an accumulate: empty for an object of the input, one mark for an object a
         *  creation made. An accumulate over no fact still binds a row, so the rule reads an object of the input as
         *  well. An object creation reads the depth of its subject off it ({@link ComputeHelpers#creationDepth}). */
        /** The {@link Recipient} facts of the allocation {@code verdeling} whose source is this rule's subject, bound as
         *  a list off an accumulate: the recipients with their weights, keys and caps as their own rule bound them
         *  ({@link TmsTranspiler#allocation}). A recipient whose fact changes changes the list, and the activation that
         *  read it fires again. An accumulate over no fact still binds a row, an empty list. */
        int recipients(String allocation) {
            String sleutel = "ontvangers " + allocation;
            int known = existing(sleutel);
            if (known >= 0) return known;
            String o = "$ontvanger_" + id("verdeling");
            String lijst = "$ontvangers_" + id("verdeling");
            Alpha ofAllocation = equal(id("ontvanger"), "allocation", allocation);
            states(new Accumulation(new FactPattern(Recipient.class, o, List.of(ofAllocation),
                    List.of(new Join(id("ontvanger-bron"), "bron", SUBJECT)), List.of()),
                    List.of(new Accumulation.Aggregation(Accumulation.Kind.LIST, null, lijst))));
            return allocateSlot(ReadKind.RECIPIENTS, sleutel, lijst);
        }

        int origin() {
            int known = existing("herkomst");
            if (known >= 0) return known;
            String g = "$herkomst_" + id("creatie");
            String lijst = "$herkomsten_" + id("creatie");
            states(new Accumulation(new FactPattern(CreationStamp.class, g, List.of(),
                    List.of(new Join(id("herkomst"), "object", SUBJECT)), List.of()),
                    List.of(new Accumulation.Aggregation(Accumulation.Kind.LIST, null, lijst))));
            return allocateSlot(ReadKind.ORIGIN, "herkomst", lijst);
        }

        /** The standing of the §9.5 channel of the form named {@code producer}, bound off its {@link ChannelState}:
         *  one fact for the whole session, joined to no subject, since the channel test has none of its own. */
        int channel(String producer) {
            String sleutel = "kanaal " + producer;
            int known = existing(sleutel);
            if (known >= 0) return known;
            String w = readName(producer + " inconsistent");
            String st = "$kanaal_" + slots.size();
            states(new FactPattern(ChannelState.class, st, List.of(equal(id("kanaal"), "regel", producer)), List.of(),
                    List.of(Binding.van(w, ChannelState.class, "inconsistent"))));
            channelNames.add(producer);
            return allocateSlot(ReadKind.CHANNEL, sleutel, w);
        }

        /** Whether the reference field {@code veld} of this rule's subject is set, to an instance of {@code klasse} where
         *  a role check narrows the other end (null: any): a link of an input relation stored on the subject
         *  ({@link TmsTranspiler#roleCheck}). Input does not change while the session fires, so it is bound off the
         *  subject's bean, as an input member is ({@link #own}). */
        int roleField(String veld, Class<?> javaClass) {
            String sleutel = "rolveld " + veld + " " + (javaClass == null ? "" : javaClass.getName());
            int known = existing(sleutel);
            if (known >= 0) return known;
            String w = readName(veld + " gezet");
            bindSubject(new Binding(w, List.of(), new Binding.Field(beanClass(subject), veld), Binding.ReadForm.IS_SET,
                    javaClass));
            return allocateSlot(ReadKind.ROLE_FIELD, sleutel, w);
        }

        /** The number of beans of {@code klasse} whose reference field {@code veld} names this rule's subject: the links
         *  of an input relation stored on the other end ({@link TmsTranspiler#roleCheck}), bound off an accumulate,
         *  which over no bean still binds a row. */
        int roleFillers(Class<?> javaClass, String veld) {
            String sleutel = "rolvullers " + javaClass.getName() + " " + veld;
            int known = existing(sleutel);
            if (known >= 0) return known;
            String filler = "$vuller_" + id("rol");
            String aantal = "$vullers_" + id("rol");
            states(new Accumulation(new FactPattern(javaClass, filler, List.of(),
                    List.of(new Join(id("rolvuller"), veld, SUBJECT)), List.of()),
                    List.of(new Accumulation.Aggregation(Accumulation.Kind.COUNT, null, aantal))));
            return allocateSlot(ReadKind.ROLE_FILLERS, sleutel, aantal);
        }

        /** The number of links of the created relation {@code relation} that hold this rule's subject at its first end
         *  where {@code subjectIsFirst}, else at its second, with an instance of {@code klasse} at the other end (null:
         *  any): the {@link RelationLink} facts, bound off an accumulate, which over no link still binds a row
         *  ({@link TmsTranspiler#roleCheck}). */
        int roleLinks(String relation, boolean subjectIsFirst, Class<?> javaClass) {
            String sleutel = "rolkoppelingen " + relation + " " + subjectIsFirst + " "
                    + (javaClass == null ? "" : javaClass.getName());
            int known = existing(sleutel);
            if (known >= 0) return known;
            String k = "$rolkoppeling_" + id("rol");
            String aantal = "$rolkoppelingen_" + id("rol");
            // the test of the other end's class reads the link alone, so it goes with the relation and the join to the
            // subject stays an identity join that the memories can hash
            List<Alpha.FieldTest> tests = new ArrayList<>();
            tests.add(new Alpha.FieldTest("relation", Alpha.Comparator.GELIJK, relation));
            if (javaClass != null)
                tests.add(new Alpha.FieldTest(subjectIsFirst ? "tweede" : "eerste", Alpha.Comparator.IS_A, javaClass));
            Alpha ofRelation = new Alpha(id("rolkoppeling"), tests);
            Join onSubject = new Join(id("rolkoppeling-onderwerp"), subjectIsFirst ? "eerste" : "tweede", SUBJECT);
            states(new Accumulation(new FactPattern(RelationLink.class, k, List.of(ofRelation), List.of(onSubject), List.of()),
                    List.of(new Accumulation.Aggregation(Accumulation.Kind.COUNT, null, aantal))));
            return allocateSlot(ReadKind.ROLE_LINKS, sleutel, aantal);
        }

        /** The parent of this rule's subject in the scope of a uniqueness ({@link TmsTranspiler#keyRule}): the
         *  reference field on the subject that the scope's binding names, the role the parent plays, read off the
         *  subject's bean, since the relation is input; null where the subject has no parent. A scope over a relation a
         *  form creates, whose links are in no field, is refused. */
        int scopeRead(RolBindingRef scopeRole) {
            String sleutel = "scope " + scopeRole.feittype() + " " + scopeRole.rol();
            int known = existing(sleutel);
            if (known >= 0) return known;
            RegelSpraakModel.Rol[] pair = FactModelGenerator.boundRolePair(model, scopeRole);
            if (pair == null)
                throw new Refusal("de scope van een Uniqueness noemt geen gedeclareerde rol (" + scopeRole.feittype() + ", "
                        + scopeRole.rol() + ")");
            if (scopeRole.feittype() != null && createdRelations.contains(Namen.norm(scopeRole.feittype())))
                throw new Refusal("de scope van een Uniqueness over de gecreëerde relatie '" + scopeRole.feittype()
                        + "': " + STILL_NOT);
            Objecttype parent = model.objecttypeOpNaam(pair[0].objecttypeNaam());
            if (!FactModelGenerator.hasReferenceField(model, subject, pair[0].naam(), parent))
                throw new Refusal("de scope van een Uniqueness: " + FactModelGenerator.noReferenceFieldReason(model,
                        subject, pair[0].naam(), parent) + " (§13.3.9 opslagrichting)");
            String w = readName(pair[0].naam());
            bindSubject(new Binding(w, List.of(), new Binding.Field(beanClass(subject), pair[0].naam()),
                    Binding.ReadForm.VALUE, null));
            return allocateSlot(ReadKind.SCOPE, sleutel, w);
        }

        private int existing(String sleutel) {
            Integer i = index.get(sleutel);
            if (i != null && use != null) use.add(i);
            return i == null ? -1 : i;
        }

        /** The variable of a slot that reads {@code read} ({@link TmsTranspiler#readName(String, Set)}), unique
         *  against the slots of this rule before it. */
        private String readName(String read) {
            return TmsTranspiler.readName(read, names);
        }

        /** A slot that a binding of the plan binds to the variable {@code naam}, read once per rule under
         *  {@code sleutel}. */
        private int allocateSlot(ReadKind soort, String sleutel, String naam) {
            int i = slots.size();
            dev.uchar.regelspraak.transpiler.tms.plan.Read l = new dev.uchar.regelspraak.transpiler.tms.plan.Read(i, soort, sleutel, naam);
            slots.add(l);
            index.put(sleutel, i);
            names.add(naam);
            if (use != null) use.add(i);
            return i;
        }

        /** A slot that no pattern binds: the consequence fills it. */
        private int unbound(ReadKind soort) {
            dev.uchar.regelspraak.transpiler.tms.plan.Read l = new dev.uchar.regelspraak.transpiler.tms.plan.Read(slots.size(), soort, null, null);
            slots.add(l);
            return l.slot();
        }

        private void bindSubject(Binding b) {
            subjectBindings.add(b);
        }

        private void parameters(LhsItem item) {
            parameters.add(item);
        }

        private void states(LhsItem item) {
            states.add(item);
        }

        /** The standing of {@code lid} of the subject, a {@link State} or a {@link CharacteristicState}, binding {@code veld}
         *  of it to {@code w}: a constraint on the member, the join on the subject, the binding. */
        private FactPattern state(Class<?> javaClass, String naam, String memberField, ElementSleutel lk, String veld, String w) {
            Alpha ofMember = equal(id("stand"), memberField, lk);
            Join onSubject = onSubject(id("stand-onderwerp"));
            return new FactPattern(javaClass, naam, List.of(ofMember), List.of(onSubject), List.of(Binding.van(w, javaClass, veld)));
        }

        int own(Member member) {
            String sleutel = "eigen " + member.sleutel();
            int known = existing(sleutel);
            if (known >= 0) return known;
            String w = readName(member.naam());
            ElementSleutel lk = member.sleutel();
            if (writers.containsKey(lk)) {
                if (member.kenmerk()) {
                    states(state(CharacteristicState.class, "$stand_" + slots.size(), "kenmerk", lk, "holds", w));
                    derived.put(lk, member);
                    return allocateSlot(ReadKind.CHARACTERISTIC_STATE, sleutel, w);
                }
                states(state(State.class, "$stand_" + slots.size(), "attribuut", lk, "waarde", w));
                derived.put(lk, member);
                return allocateSlot(ReadKind.STATE, sleutel, w);
            }
            // input: no form writes it, so it does not change while the session fires and is read off the bean
            bindSubject(new Binding(w, List.of(), new Binding.Field(member.javaClass(), member.naam()),
                    member.kenmerk() ? Binding.ReadForm.WAAR : Binding.ReadForm.VALUE, null));
            return allocateSlot(ReadKind.INVOER, sleutel, w);
        }

        /**
         * The timeline of a member on a timeline of this rule's subject ({@link Member#time}), or null where the member
         * holds none (an absent timeline, RUNTIME-SEMANTICS ch. 3.1). An attribute is bound off its {@link State},
         * whether a form writes it or not: the standing carries the timeline a form derived, and otherwise the input,
         * as an immutable {@code List<Period>}, or as an {@link BucketScalar} where the case gives the input in the
         * asOfDate-bucket convention ({@link BucketInput}). The bean does not show that convention, and every reader of
         * the full timeline must tell it apart (ch. 8.4), so the input is read where the standing rules join that fact
         * ({@link #attributeStateRules}). A characteristic on a timeline is input: no executor writes one (ch. 3.11),
         * and a case delivers it only as its true periods, so they are bound off the bean, turned into that form there
         * ({@link Period#lijst}), and never through {@link #characteristicStateRules}, whose test of {@code true} is false on
         * a list; the bean does not change while the session fires, so the binding is stable.
         */
        int timeline(Member member) {
            String sleutel = "tijdlijn " + member.sleutel();
            int known = existing(sleutel);
            if (known >= 0) return known;
            String w = "$tijdlijn_" + slots.size();
            ElementSleutel lk = member.sleutel();
            if (member.kenmerk()) {
                if (writers.containsKey(lk))
                    throw new Refusal("het tijdsafhankelijke kenmerk '" + member.naam() + "' van " + member.declarant().naam
                            + ", dat een vorm schrijft: " + STILL_NOT + " (RUNTIME-SEMANTICS ch. 3.11)");
                bindSubject(new Binding(w, List.of(), new Binding.Field(member.javaClass(), member.naam()),
                        Binding.ReadForm.PERIODS, null));
            } else {
                states(state(State.class, "$stand_" + slots.size(), "attribuut", lk, "waarde", w));
                derived.put(lk, member);
            }
            return allocateSlot(ReadKind.TIJDLIJN, sleutel, w);
        }

        /** A member of the object {@code pad} reaches from this rule's subject, null where a link on the path is absent.
         *  An attribute on a timeline is read through its standing at the end of the path, as {@link #timeline} reads
         *  one of the subject's own, so the read carries the case's input convention, a {@code List<Period>} or an
         *  {@link BucketScalar}; a characteristic on a timeline, which is input only, is bound as its true periods, a
         *  {@code List<Period>} ({@link TmsTranspiler#inputForm}). */
        int navigated(Path pad, Member member) {
            String sleutel = "pad " + pad.sleutel() + " " + member.sleutel();
            int known = existing(sleutel);
            if (known >= 0) return known;
            String w = readName(String.join(" ", pad.roles()) + " " + member.naam());
            ElementSleutel lk = member.sleutel();
            if (member.time() != null && member.kenmerk() && writers.containsKey(lk))
                throw new Refusal("het tijdsafhankelijke kenmerk '" + member.naam() + "' van " + member.declarant().naam
                        + ", dat een vorm schrijft: " + STILL_NOT + " (RUNTIME-SEMANTICS ch. 3.11)");
            // a derived member, an attribute on a timeline, or any member at the end of a path whose links a form
            // creates: through the path's standing, whose rules bind every link and the member in their left-hand sides
            if (writers.containsKey(lk) || timelineAttribute(member) || pad.created()) {
                PathRead p = new PathRead(pad, member);
                String ps = pad.sleutel();
                String st = "$padstand_" + slots.size();
                Alpha ofPath = new Alpha(id("padstand"), List.of(new Alpha.FieldTest("pad", Alpha.Comparator.GELIJK, ps),
                        new Alpha.FieldTest("member", Alpha.Comparator.GELIJK, lk)));
                Join onSubject = onSubject(id("padstand-onderwerp"));
                states(new FactPattern(PathState.class, st, List.of(ofPath), List.of(onSubject),
                        List.of(Binding.van(w, PathState.class, "waarde"))));
                paths.put(p.sleutel(), p);
                return allocateSlot(ReadKind.PATH_STATE, sleutel, w);
            }
            // input over links that are input: bound off the subject's bean, null where a link is absent
            bindSubject(new Binding(w, pad.fields(), new Binding.Field(member.javaClass(), member.naam()), inputForm(member),
                    null));
            return allocateSlot(ReadKind.INPUT_PATH, sleutel, w);
        }

        /** A parameter, bound off its {@link ParameterState}: no test or consequence names a global itself, since two
         *  globals in one constraint of the executable model read as the last one, and a source over a global was not
         *  kept apart from another parameter's in another rule ({@link ParameterState}). A parameter the case leaves
         *  unset has a standing with a null value and reads leeg. */
        int parameter(Parameter p) {
            String naam = Namen.ident(p.naam(), false);
            String sleutel = "parameter " + naam;
            int known = existing(sleutel);
            if (known >= 0) return known;
            global(p);
            String w = readName(p.naam());
            String st = "$parameter_" + slots.size();
            parameters(new FactPattern(ParameterState.class, st, List.of(equal(id("parameter"), "parameter", naam)),
                    List.of(), List.of(Binding.van(w, ParameterState.class, "waarde"))));
            return allocateSlot(ReadKind.PARAMETER, sleutel, w);
        }
    }

    /** The name of the variable of a rule's subject in every plan: the name joins on the subject refer to. */
    private static final String SUBJECT = "$onderwerp";
    /** The variable of the object a rule binds before its subject: a fold's subject in a contribution rule, the source
     *  in an allocation's recipient rule. */
    private static final String FOLD_SUBJECT = "$vouwonderwerp";
    /**
     * The variable of a slot that reads {@code read}, a name as the document declares it, in a rule whose slots
     * before it have the variables {@code bezet}: {@code $} and that name as an identifier ({@link Namen#ident}), so
     * that a rendering shows what the slot holds, in DRL text as well. Only the characters a Java identifier may hold
     * stay: {@code Namen.ident} keeps every letter and every number of Unicode, and a number such as {@code ²} or
     * {@code ½} is none, so "artikel 3²" reads {@code $artikel3}. A name of which nothing stays reads {@code $x}, the
     * name {@code Namen.ident} gives an empty name. It is unique within the rule by construction: the identifier has no
     * underscore, while every other variable of a rule has one ({@link TmsTranspiler#id}, the numbered names of
     * {@code Reads}) or is one of {@link #FIXED_NAMES}; where such a fixed name or a name in {@code bezet} has it
     * already, the first free number from 2 after it makes it unique. It depends only on the reads of the rule before
     * it, in their order. {@code RulePlan} refuses a rule that declares one variable twice, so a name that escaped
     * this would fail the build, and not bind two values to one variable.
     */
    static String readName(String read, Set<String> bezet) {
        StringBuilder id = new StringBuilder();
        Namen.ident(read, false).codePoints().filter(Character::isJavaIdentifierPart).forEach(id::appendCodePoint);
        String base = "$" + (id.isEmpty() ? "x" : id);
        String naam = base;
        for (int n = 2; FIXED_NAMES.contains(naam) || bezet.contains(naam); n++) naam = base + n;
        return naam;
    }

    /** Every name without an underscore that the analysis gives a variable of a rule other than a read name
     *  ({@link #readName(String, Set)}): a read name that equals one of these takes a number, so that no rule binds
     *  one name twice. {@code ReadNameWitnessTest} holds this set equal to the fixed names this source spells. */
    static final Set<String> FIXED_NAMES = Set.of("$afgeleid", "$ander", "$bereikt", "$besluit", "$botsingen",
            "$conflict", "$creatiefase", "$een", "$element", "$emmerinvoer", "$fase", "$houder", "$inconsistentie",
            "$invoer", "$nabij", "$onderwerp", "$rekendatum", "$schending", "$sleutel", "$stand", "$subject", "$twee",
            "$v", "$vouwonderwerp", "$waarde", "$waarden");

    /** The items {@code lhs} of a rule with the probe of every test that places none at -1
     *  ({@link #withoutUnfilledDepositFlag(Gevolg)}): a test of deposits places it, every other role does not. */
    private static List<LhsItem> withoutUnfilledDepositFlag(List<LhsItem> lhs) {
        List<LhsItem> out = new ArrayList<>(lhs.size());
        for (LhsItem item : lhs) {
            if (item instanceof TestItem t && t.rol() != TestItem.Role.DEPOSIT) {
                Term testTerm = withoutDepositFlag(t.test());
                if (testTerm != t.test()) item = new TestItem(t.id(), t.rol(), testTerm, t.depositFlag(), t.slots(), t.size());
            }
            out.add(item);
        }
        return out;
    }

    /**
     * {@code g} with the probe of its terms at -1 where it places none. Every place that deposits compiles with the
     * probe slot of its rule ({@link Reads#depositFlag}), whatever the role of the rule, but only three positions put a
     * probe in that slot: a test of deposits ({@link TestItem.Role#DEPONERING}), the value of a rule with a deposit
     * ({@link Consequence.Value#deposit}) and the test of a member with a probe in a quantifier fold
     * ({@link Consequence.Input.MemberTest}). Elsewhere, as in the rule of a condition's fault or a filter's fault, the
     * places would report to a slot nobody fills, which reads as nothing; the plan says so itself, so that a rendering
     * needs no rule of its own for such a slot. The slot stays in the table, and the value array keeps its size.
     */
    private static Consequence withoutUnfilledDepositFlag(Consequence g) {
        switch (g) {
            case Consequence.Value w when w.deposit() == null -> {
                Term z = withoutDepositFlag(w.waarde());
                return z == w.waarde() ? w : new Consequence.Value(w.onderwerp(), w.doel(), w.writer(), z, w.slots(),
                        w.size(), w.collapse(), w.writesEmpty(), null, w.fout(), w.insertion());
            }
            case Consequence.TestFault f -> {
                Term z = withoutDepositFlag(f.test());
                return z == f.test() ? f : new Consequence.TestFault(f.onderwerp(), z, f.rol(), f.slots(), f.size(),
                        f.fout(), f.insertion());
            }
            case Consequence.FilterFault f -> {
                Term z = withoutDepositFlag(f.filter());
                return z == f.filter() ? f : new Consequence.FilterFault(f.onderwerp(), f.fold(), f.member(), z, f.place(),
                        f.slots(), f.size(), f.insertion());
            }
            case Consequence.Contribution b when b.input() instanceof Consequence.Input.Verdict o -> {
                Term z = withoutDepositFlag(o.matrix());
                return z == o.matrix() ? b : new Consequence.Contribution(b.onderwerp(), b.fold(), b.member(),
                        new Consequence.Input.Verdict(z), b.slots(), b.size(), b.insertion());
            }
            case Consequence.Contribution b when b.input() instanceof Consequence.Input.MemberTest l && l.depositFlag() < 0 -> {
                Term z = withoutDepositFlag(l.matrix());
                return z == l.matrix() ? b : new Consequence.Contribution(b.onderwerp(), b.fold(), b.member(),
                        new Consequence.Input.MemberTest(z, -1, l.place()), b.slots(), b.size(), b.insertion());
            }
            case Consequence.Creation c -> {
                List<Consequence.Initialisation> inits = new ArrayList<>();
                boolean changed = false;
                for (Consequence.Initialisation i : c.initialisations()) {
                    Term z = withoutDepositFlag(i.waarde());
                    changed |= z != i.waarde();
                    inits.add(z == i.waarde() ? i : new Consequence.Initialisation(i.member(), z));
                }
                return !changed ? c : new Consequence.Creation(c.onderwerp(), c.creatie(), c.depth(), inits, c.fout(),
                        c.faultInsertion(), c.construction(), c.phase(), c.insertions(), c.slots(), c.size());
            }
            case Consequence.Decision a -> {
                Term z = withoutDepositFlag(a.waarde());
                return z == a.waarde() ? a : new Consequence.Decision(a.onderwerp(), z, a.slots(), a.size(), a.collapse(),
                        a.fout(), a.facts(), a.insertion());
            }
            case Consequence.Conflict c -> {
                Term z = withoutDepositFlag(c.sleutel());
                return z == c.sleutel() ? c : new Consequence.Conflict(c.onderwerp(), z, c.slots(), c.size(), c.fact(),
                        c.insertion());
            }
            default -> {
                return g;
            }
        }
    }

    /** {@code t} with the probe of every place that deposits at -1: of a division, a square root, the evaluation of a
     *  fold whose filter deposits and a quantifier fold whose matrix deposits; {@code t} itself where it has none. */
    private static Term withoutDepositFlag(Term t) {
        switch (t) {
            case null -> {
                return null;
            }
            case Term.Const c -> {
                return c;
            }
            case Term.SlotValue l -> {
                return l;
            }
            case Term.Binder b -> {
                return b;
            }
            case Term.Fail f -> {
                return f;
            }
            case Term.DayKind d -> {
                return d;
            }
            case Term.Invoer i -> {
                Term o = withoutDepositFlag(i.object());
                return o == i.object() ? i : new Term.Invoer(o, i.veld(), i.vorm(), i.bron());
            }
            case Term.Let l -> {
                Term w = withoutDepositFlag(l.waarde()), b = withoutDepositFlag(l.body());
                return w == l.waarde() && b == l.body() ? l
                        : new Term.Let(l.slot(), l.naam(), w, b, l.eager(), l.bron());
            }
            case Term.Choice k -> {
                Term v = withoutDepositFlag(k.voorwaarde()), d = withoutDepositFlag(k.then()), a = withoutDepositFlag(k.otherwise());
                return v == k.voorwaarde() && d == k.then() && a == k.otherwise() ? k : new Term.Choice(v, d, a, k.bron());
            }
            case Term.Call a -> {
                List<Term> z = withoutDepositFlag(a.arguments());
                return z == a.arguments() ? a : new Term.Call(a.function(), z, a.bron());
            }
            case Term.Restate h -> {
                Term z = withoutDepositFlag(h.term());
                return z == h.term() ? h
                        : new Term.Restate(h.function(), z, h.van(), h.naar(), h.counter(), h.denominator(), h.bron());
            }
            case Term.Division d -> {
                Term l = withoutDepositFlag(d.left()), r = withoutDepositFlag(d.right());
                return d.depositFlag() < 0 && l == d.left() && r == d.right() ? d
                        : new Term.Division(d.function(), l, r, -1, d.bron());
            }
            case Term.SquareRoot w -> {
                Term r = withoutDepositFlag(w.radicand());
                return w.depositFlag() < 0 && r == w.radicand() ? w : new Term.SquareRoot(w.function(), r, -1, w.bron());
            }
            case Term.Required v -> {
                Term z = withoutDepositFlag(v.waarde());
                return z == v.waarde() ? v : new Term.Required(z, v.sentence(), v.bron());
            }
            case Term.TextComposition r -> {
                List<Term> z = withoutDepositFlag(r.parts());
                return z == r.parts() ? r : new Term.TextComposition(z, r.bron());
            }
            case Term.ListedFold g -> {
                List<Term> z = withoutDepositFlag(g.operands());
                return z == g.operands() ? g : new Term.ListedFold(g.function(), z, g.bron());
            }
            case Term.FoldRead v -> {
                Term z = withoutDepositFlag(v.outcome());
                return v.depositFlag() < 0 && z == v.outcome() ? v
                        : new Term.FoldRead(z, v.depositCount(), -1, v.filterFault(), v.memberFault(), v.bron());
            }
            case Term.Compound s -> {
                List<Term> z = withoutDepositFlag(s.members());
                return z == s.members() ? s : new Term.Compound(s.quantifier(), s.threshold(), z, s.bron());
            }
            case Term.QuantifierRead k -> {
                Term n = withoutDepositFlag(k.aantal()), b = withoutDepositFlag(k.contributions());
                return k.depositFlag() < 0 && n == k.aantal() && b == k.contributions() ? k
                        : new Term.QuantifierRead(k.function(), n, b, k.quantifier(), k.threshold(), -1, k.bron());
            }
            case Term.WithoutFault z -> {
                Term v = withoutDepositFlag(z.voorwaarde());
                return v == z.voorwaarde() ? z : new Term.WithoutFault(v, z.bron());
            }
        }
    }

    /** {@link #withoutDepositFlag(Term)} of each term, {@code terms} itself where none changes. */
    private static List<Term> withoutDepositFlag(List<Term> terms) {
        List<Term> out = new ArrayList<>(terms.size());
        boolean otherwise = false;
        for (Term t : terms) {
            Term z = withoutDepositFlag(t);
            otherwise |= z != t;
            out.add(z);
        }
        return otherwise ? out : terms;
    }

    /** A constraint that field {@code veld} of a fact equals {@code waarde}. */
    private static Alpha equal(String id, String veld, Object waarde) {
        return new Alpha(id, List.of(new Alpha.FieldTest(veld, Alpha.Comparator.GELIJK, waarde)));
    }

    /** The join of a standing fact on the rule's subject: its field {@code onderwerp} is the subject. */
    private static Join onSubject(String id) {
        return new Join(id, "onderwerp", SUBJECT);
    }

    /** The rule that puts the {@link ParameterState} of one parameter in working memory: it matches no fact, so it fires
     *  once per session, and it binds the global in its consequence, where the case runner set it before the session
     *  fired. The global does not change while the session fires, so the standing never needs to be withdrawn. */
    private RulePlan parameterStateRule(String naam) {
        return new RulePlan("parameterstand " + naam, null, null, new ReadTable(List.of()), List.of(),
                new Consequence.InsertParameterState(naam, naam, Consequence.Insertion.LOGICAL));
    }

    /** Declares the global of parameter {@code p}, of the Java type of its datatype, so that the case runner sets it by
     *  name; one per parameter for the whole model. */
    private void global(Parameter p) {
        String naam = Namen.ident(p.naam(), false);
        if (globals.containsKey(naam)) return;
        if (p.granulariteit() != null)
            throw new Refusal("de tijdsafhankelijke parameter '" + p.naam() + "': " + STILL_NOT + "; de goldens met"
                    + " cases die er een lezen, doen dat in een TemporalRead, die de lader voor alle takken weigert");
        Datatype t = p.datatype();
        Class<?> javaClass = t.isNumeriek() || t.isPercentage() ? BigDecimal.class
                : t instanceof Datatype.DatumInDagen ? java.time.LocalDate.class
                : t instanceof Datatype.DatumTijd ? java.time.LocalDateTime.class
                : t.isTekst() || t instanceof Datatype.Enumeratie ? String.class
                : t.isBoolean() ? Boolean.class : null;
        if (javaClass == null) throw new Refusal("de parameter '" + p.naam() + "' van type " + typeName(t) + ": " + STILL_NOT);
        globals.put(naam, javaClass);
    }

    private static int[] positions(Set<Integer> use) {
        return use.stream().mapToInt(Integer::intValue).toArray();
    }

    // ---------------- standings ----------------

    /**
     * The three generic rules that keep one {@link CharacteristicState} per object of the declaring type: waar from the input,
     * waar from a derivation, onwaar when neither holds. The onwaar rule carries the branch's only negation over a
     * characteristic, {@code not HasCharacteristic}, and reads the input in its left-hand side, so a derivation that appears
     * later retracts the onwaar standing and every conclusion that read it. When both sources hold, the two waar
     * standings are equal, and truth maintenance keeps them as one fact with a justification each. The input is read
     * off the bean as whether it holds {@code true} ({@link Binding.ReadForm#WAAR}), the one truth value the standing
     * holds: a characteristic on a timeline is never read through it ({@link Reads#timeline}).
     */
    private List<RulePlan> characteristicStateRules(Member member) {
        ElementSleutel sleutel = member.sleutel();
        String naam = "kenmerkstand " + member.declarant().naam + "." + member.naam() + ": ";
        Alpha inputTrue = inputTrue(id("invoer"), member, true);
        RulePlan invoer = new RulePlan(naam + "waar uit de invoer", null, null, new ReadTable(List.of()),
                List.of(new Subject(member.javaClass(), SUBJECT, List.of(inputTrue), List.of(), List.of())),
                new Consequence.InsertCharacteristicState(SUBJECT, sleutel, Consequence.StateSource.INVOER, true, Consequence.Insertion.LOGICAL));
        RulePlan derivation = new RulePlan(naam + "waar uit een afleiding", null, null, new ReadTable(List.of()),
                List.of(new Subject(member.javaClass(), SUBJECT, List.of(), List.of(), List.of()), derived(sleutel)),
                new Consequence.InsertCharacteristicState(SUBJECT, sleutel, Consequence.StateSource.DERIVATION, true,
                        Consequence.Insertion.LOGICAL));
        Alpha noInput = inputTrue(id("geen-invoer"), member, false);
        RulePlan falsePlan = new RulePlan(naam + "onwaar", null, null, new ReadTable(List.of()),
                List.of(new Subject(member.javaClass(), SUBJECT, List.of(noInput), List.of(), List.of()),
                        new Not(derived(sleutel))),
                new Consequence.InsertCharacteristicState(SUBJECT, sleutel, Consequence.StateSource.GEEN, false, Consequence.Insertion.LOGICAL));
        return List.of(invoer, derivation, falsePlan);
    }

    /** The test that the input of characteristic {@code lid} on the subject's bean, read as whether it holds
     *  {@code true}, is {@code polarity}. */
    private static Alpha inputTrue(String id, Member member, boolean polarity) {
        return new Alpha(id, List.of(new Alpha.FieldTest(List.of(), member.naam(), Binding.ReadForm.WAAR,
                Alpha.Comparator.GELIJK, polarity)));
    }

    /** The pattern of the {@link HasCharacteristic} of {@code kenmerk} that a derivation put on the subject: a constraint on
     *  the characteristic, the join on the subject. */
    private FactPattern derived(ElementSleutel kenmerk) {
        return new FactPattern(HasCharacteristic.class, "$afgeleid", List.of(equal(id("afgeleid"), "kenmerk", kenmerk)),
                List.of(onSubject(id("afgeleid-onderwerp"))), List.of());
    }

    /**
     * The generic rules that keep one {@link State} per object of the declaring type: the derived value where a
     * {@link Value} exists, else the input on the bean, read in the left-hand side, leeg included. An input rule
     * carries {@code not Waarde}, so a derivation that appears later retracts the input standing and every value that
     * read it. A member no form writes has no derivation, and only an attribute on a timeline is read through its
     * standing then ({@link Reads#timeline}).
     *
     * <p>The input timeline of a member on a timeline stands in the form a {@link Value} carries one,
     * {@code List<Period>} ({@link Period#lijst}), so every standing of the member holds one representation, except
     * where the case gives the input as the asOfDate-bucket scalar: then the input stands as an {@link BucketScalar}
     * (RUNTIME-SEMANTICS ch. 8.4). The bean holds that scalar as the one segment of its period, which a full timeline of
     * one segment would be as well, so two input rules join the {@link BucketInput} the case input states beside the
     * beans, the one under {@code exists} and the other under {@code not}; the fact is stated and never changes while
     * the session fires, so exactly one of them holds for each object.
     */
    private List<RulePlan> attributeStateRules(Member member) {
        ElementSleutel sleutel = member.sleutel();
        String naam = "stand " + member.declarant().naam + "." + member.naam() + ": ";
        List<RulePlan> out = new ArrayList<>();

        if (writers.containsKey(sleutel)) {
            FactPattern waarde = new FactPattern(Value.class, "$waarde", List.of(equal(id("waarde"), "attribuut", sleutel)),
                    List.of(onSubject(id("waarde-onderwerp"))), List.of(Binding.van("$v", Value.class, "waarde")));
            out.add(new RulePlan(naam + "uit een afleiding", null, null,
                    oneSlot(ReadKind.DERIVATION, "afleiding " + sleutel, "$v"),
                    List.of(new Subject(member.javaClass(), SUBJECT, List.of(), List.of(), List.of()), waarde),
                    new Consequence.State(SUBJECT, sleutel, Consequence.StateSource.DERIVATION, 0, false,
                            Consequence.Insertion.LOGICAL)));
        }

        if (member.time() == null) {
            out.add(inputStateRule(member, naam + "uit de invoer", null));
            return out;
        }
        out.add(inputStateRule(member, naam + "uit de invoer", false));
        out.add(inputStateRule(member, naam + "uit de invoer, een emmerscalair", true));
        return out;
    }

    /** The input rule {@code naam} of {@link #attributeStateRules}: where no {@link Value} stands on the object, the
     *  {@link State} of the input on the bean, read as it is, or as its segments for a member on a timeline
     *  ({@link #inputForm}); with {@code bucket} null for a member that holds one value, and for a member on a timeline
     *  only where an {@link BucketInput} of the member on the object exists ({@code true}), the input then standing as
     *  its {@link BucketScalar}, or does not ({@code false}). The bucket is taken in the consequence, from the bound
     *  timeline: a binding is computed for every object the subject pattern matches, also one whose input is a full
     *  timeline. */
    private RulePlan inputStateRule(Member member, String naam, Boolean bucket) {
        ElementSleutel sleutel = member.sleutel();
        List<LhsItem> items = new ArrayList<>();
        items.add(new Subject(member.javaClass(), SUBJECT, List.of(), List.of(), List.of(new Binding("$invoer", List.of(),
                new Binding.Field(member.javaClass(), member.naam()), inputForm(member), null))));
        if (bucket != null) {
            FactPattern p = new FactPattern(BucketInput.class, "$emmerinvoer",
                    List.of(equal(id("emmerinvoer"), "attribuut", sleutel)),
                    List.of(onSubject(id("emmerinvoer-onderwerp"))), List.of());
            items.add(bucket ? new Exists(List.of(p), List.of(p.naam()), List.of(SUBJECT)) : new Not(p));
        }
        items.add(new Not(new FactPattern(Value.class, "$waarde", List.of(equal(id("waarde"), "attribuut", sleutel)),
                List.of(onSubject(id("waarde-onderwerp"))), List.of())));
        return new RulePlan(naam, null, null, oneSlot(ReadKind.INVOER, "invoer " + sleutel, "$invoer"),
                items, new Consequence.State(SUBJECT, sleutel, Consequence.StateSource.INVOER, 0, Boolean.TRUE.equals(bucket),
                        Consequence.Insertion.LOGICAL));
    }

    /** The value array of a standing rule whose consequence reads one value: slot 0, of {@code soort}, which the
     *  variable {@code naam} binds. */
    private static ReadTable oneSlot(ReadKind soort, String sleutel, String naam) {
        return new ReadTable(List.of(new dev.uchar.regelspraak.transpiler.tms.plan.Read(0, soort, sleutel, naam)));
    }

    /**
     * The two generic rules that keep one {@link PathState} per object of the subject type for a derived member read over
     * a path: where every link on the path is present, the standing of the member on the object the path reaches,
     * joined on that object; where a link is absent, null. The links are input, read off the bean in the left-hand
     * side, and do not change while the session fires, so exactly one of the two holds for each object. Both rules
     * read the path along its reference fields ({@link Path#fields}): the test whether it reaches an object, and the
     * binding of that object, which walks to the last field and reads it there.
     */
    private List<RulePlan> pathStateRules(PathRead p) {
        Path pad = p.pad();
        Member member = p.member();
        String ps = pad.sleutel();
        ElementSleutel lk = member.sleutel();
        Class<?> javaClass = beanClass(pad.subject());
        String naam = "padstand " + pad.subject().naam + "." + String.join(".", pad.roles()) + " naar "
                + member.declarant().naam + "." + member.naam() + ": ";
        List<Binding.Field> fields = pad.fields();

        Alpha link = alongPath(id("link"), fields, Alpha.Comparator.GEVULD);
        Binding reaches = new Binding("$bereikt", fields.subList(0, fields.size() - 1), fields.get(fields.size() - 1),
                Binding.ReadForm.VALUE, null);
        Class<?> stateClass = member.kenmerk() ? CharacteristicState.class : State.class;
        FactPattern state = new FactPattern(stateClass, "$stand",
                List.of(equal(id("stand"), member.kenmerk() ? "kenmerk" : "attribuut", lk)),
                List.of(new Join(id("stand-bereikt"), "onderwerp", "$bereikt")),
                List.of(Binding.van("$v", stateClass, member.kenmerk() ? "holds" : "waarde")));
        RulePlan aanwezig = new RulePlan(naam + "link aanwezig", null, null,
                oneSlot(ReadKind.REACHED_STATE, "stand " + ps + " " + lk, "$v"),
                List.of(new Subject(javaClass, SUBJECT, List.of(link), List.of(), List.of(reaches)), state),
                new Consequence.InsertPathState(SUBJECT, ps, lk, Consequence.StateSource.REACHED, 0, Consequence.Insertion.LOGICAL));

        Alpha noLink = alongPath(id("geen-link"), fields, Alpha.Comparator.LEEG);
        RulePlan absent = new RulePlan(naam + "link afwezig", null, null, new ReadTable(List.of()),
                List.of(new Subject(javaClass, SUBJECT, List.of(noLink), List.of(), List.of())),
                new Consequence.InsertPathState(SUBJECT, ps, lk, Consequence.StateSource.MISSING_LINK, -1,
                        Consequence.Insertion.LOGICAL));
        return List.of(aanwezig, absent);
    }

    /** The test {@code op} of the object the walk over the reference fields {@code fields} reaches from the subject,
     *  null where a link on it is absent: {@code GEVULD} where the path reaches an object, {@code LEEG} where not. */
    private static Alpha alongPath(String id, List<Binding.Field> fields, Alpha.Comparator op) {
        return new Alpha(id, List.of(new Alpha.FieldTest(fields, null, Binding.ReadForm.VALUE, op, null)));
    }

    /**
     * The two rules of {@link #pathStateRules} for a path with a hop over a relation a form creates: such a relation
     * gains links while the session fires, so the path is a chain of patterns in the left-hand side, a hop over a
     * created relation joining its {@link RelationLink} facts and a hop over an input relation the reference field of the
     * near bean, each ending in a pattern over the reached bean ({@link #chain}). Where the chain reaches an object the
     * standing carries the member's value there, read off that bean where no form writes the member and through its
     * standing where one does, or where it is an attribute on a timeline, whose standing carries the case's input
     * convention; where it does not, the value is null, under a {@code not} over the same chain. A link that a creation
     * adds takes the standing along.
     */
    private List<RulePlan> linkPathStateRules(PathRead p) {
        Path pad = p.pad();
        Member member = p.member();
        String ps = pad.sleutel();
        ElementSleutel lk = member.sleutel();
        Class<?> javaClass = beanClass(pad.subject());
        String naam = "padstand " + pad.subject().naam + "." + String.join(".", pad.roles()) + " naar "
                + member.declarant().naam + "." + member.naam() + ": ";

        List<LhsItem> items = new ArrayList<>();
        items.add(new Subject(javaClass, SUBJECT, List.of(), List.of(), List.of()));
        List<FactPattern> chain = chain(pad.hops(), SUBJECT, null);
        ReadTable waarde;
        if (writers.containsKey(lk) || timelineAttribute(member)) {
            items.addAll(chain);
            Class<?> stateClass = member.kenmerk() ? CharacteristicState.class : State.class;
            items.add(new FactPattern(stateClass, "$stand",
                    List.of(equal(id("stand"), member.kenmerk() ? "kenmerk" : "attribuut", lk)),
                    List.of(new Join(id("stand-bereikt"), "onderwerp", chain.get(chain.size() - 1).naam())),
                    List.of(Binding.van("$v", stateClass, member.kenmerk() ? "holds" : "waarde"))));
            waarde = oneSlot(ReadKind.REACHED_STATE, "stand " + ps + " " + lk, "$v");
        } else {
            // the input of the member off the bean the chain reaches, read as the binding's form says
            items.addAll(chain.subList(0, chain.size() - 1));
            items.add(withBinding(chain.get(chain.size() - 1), new Binding("$v", List.of(),
                    new Binding.Field(member.javaClass(), member.naam()), inputForm(member), null)));
            waarde = oneSlot(ReadKind.REACHED_INPUT, "invoer " + ps + " " + lk, "$v");
        }
        RulePlan aanwezig = new RulePlan(naam + "link aanwezig", null, null, waarde, items,
                new Consequence.InsertPathState(SUBJECT, ps, lk, Consequence.StateSource.REACHED, 0, Consequence.Insertion.LOGICAL));

        // no combination of the chain's objects: what the chain declares lives inside the negation, and it reads the
        // subject alone from before it
        List<FactPattern> none = chain(pad.hops(), SUBJECT, null);
        RulePlan absent = new RulePlan(naam + "link afwezig", null, null, new ReadTable(List.of()),
                List.of(new Subject(javaClass, SUBJECT, List.of(), List.of(), List.of()),
                        new Not(none, Exists.verklaard(none), List.of(SUBJECT))),
                new Consequence.InsertPathState(SUBJECT, ps, lk, Consequence.StateSource.MISSING_LINK, -1,
                        Consequence.Insertion.LOGICAL));
        return List.of(aanwezig, absent);
    }

    /**
     * The patterns that walk {@code hops} from the object the variable {@code van} binds, in order: per hop a pattern
     * over the bean the hop reaches, tested against the near object along the reference field of the end that holds it
     * ({@link StaticFunction#MEETING}, the field the chain of one side), or, over a relation a form creates, a
     * {@link RelationLink} pattern joined to the near object, which binds the link's other end, and a pattern over the bean
     * joined to that end. Where {@code phase} names the variable of the creation phase, a link of that phase is not
     * walked. The last pattern is the one over the last bean reached.
     */
    private List<FactPattern> chain(List<Hop> hops, String van, String phase) {
        List<FactPattern> out = new ArrayList<>();
        String near = van;
        for (Hop h : hops) {
            Class<?> javaClass = beanClass(h.naar());
            String far = "$stap_" + id("keten");
            if (h.relation() != null) {
                boolean eerste = h.nearIsFirst();
                String relationLink = "$koppeling_" + id("keten");
                String otherSide = "$overzijde_" + id("keten");
                Alpha ofRelation = equal(id("koppeling"), "relation", h.relation());
                Join onNear = new Join(id("koppeling-nabij"), eerste ? "eerste" : "tweede", near);
                // a creation rule does not walk a link of the phase it fires in: the rule layer has not seen it yet
                List<BetaTest> earlier = phase == null ? List.of()
                        : List.of(new BetaTest(id("koppeling-eerder"), StaticFunction.NOT_IN_PHASE, phase));
                out.add(new FactPattern(RelationLink.class, relationLink, List.of(ofRelation), List.of(onNear), earlier,
                        List.of(Binding.van(otherSide, RelationLink.class, eerste ? "tweede" : "eerste"))));
                out.add(new FactPattern(javaClass, far, List.of(), List.of(new Join(id("koppeling-ver"), null, otherSide)),
                        List.of()));
            } else {
                // the field on the near bean reaches the far one, or the field on the far bean the near one
                // the walk reads bean properties, as every chain of the plan holds them
                List<String> veld = List.of(Namen.property(h.veld()));
                BetaTest step = h.onNear()
                        ? new BetaTest(id("veld-nabij"), StaticFunction.MEETING, near,
                                List.of(constant(List.of()), constant(veld)))
                        : new BetaTest(id("veld-ver"), StaticFunction.MEETING, near,
                                List.of(constant(veld), constant(List.of())));
                out.add(new FactPattern(javaClass, far, List.of(), List.of(), List.of(step), List.of()));
            }
            near = far;
        }
        return out;
    }

    // ---------------- members, paths and beans ----------------

    /** A member as this branch keys and reaches it: the key on its declaring type, that type, the declared spelling (the
     *  bean property), whether it is a characteristic, its declared datatype (null for a characteristic), the bean
     *  class of the declaring type, whose subtypes' beans extend it, and, for a member on a timeline, the granularity of
     *  its grid (null for a member that holds one value). A member on a timeline is read and written as a
     *  {@code List<Period>} ({@link Period}). */
    private record Member(ElementSleutel sleutel, Objecttype declarant, String naam, boolean kenmerk, Datatype datatype,
                       Class<?> javaClass, RegelSpraakModel.Granulariteit time) {}

    /** Whether {@code lid} is an attribute on a timeline: one the branch reads through its standing, derived or input,
     *  whose input stands in the convention the case gave it ({@link #attributeStateRules}). */
    private static boolean timelineAttribute(Member member) {
        return member.time() != null && !member.kenmerk();
    }

    /** How the input member {@code lid} is read off a bean of a type that declares it, the decision a plan's binding
     *  carries ({@link Binding.ReadForm}): a characteristic on a timeline as its true periods, a {@code List<Period>}
     *  ({@link Period#lijst}), null for an absent timeline, a characteristic that holds one truth value as whether it
     *  holds {@code true}, and any other member as its value. An attribute on a timeline is read through its standing
     *  instead ({@link #timelineAttribute}). */
    private static Binding.ReadForm inputForm(Member member) {
        if (member.time() != null) return Binding.ReadForm.PERIODS;
        if (member.kenmerk()) return Binding.ReadForm.WAAR;
        return Binding.ReadForm.VALUE;
    }

    /** Member {@code naam} as declared on {@code houder}'s chain, compared as the verifier compares names, holding one
     *  value. A member on a timeline is refused here: the positions that read or write one ask {@link #timeMember}. A
     *  member with dimensions is refused. */
    private Member member(Objecttype houder, String naam, boolean kenmerk) {
        Member member = declared(houder, naam, kenmerk);
        if (member.time() != null)
            throw new Refusal("het tijdsafhankelijke " + (kenmerk ? "kenmerk" : "attribuut") + " '" + member.naam()
                    + "' van " + member.declarant().naam + " op een plek die één waarde leest of schrijft: deze"
                    + " tak leest een tijdlijn alleen in een totaal, de knip ervan, een tijdsevenredig"
                    + " deel, een dagentelling en een uniciteitssleutel, en schrijft haar alleen met een tijdsevenredig"
                    + " deel of een dagentelling (RUNTIME-SEMANTICS ch. 3.3, 3.6–3.8, 3.11, 5.9)");
        return member;
    }

    /**
     * Member {@code naam} as declared on {@code houder}'s chain, on a timeline: {@code description} reads or writes its timeline
     * (RUNTIME-SEMANTICS ch. 3). Refused by name: a member that holds one value there, one on a grid of more than one
     * unit, which carries no operation (ch. 3.2, OPEN in ch. 3.12), and one on a timeline with a variable start point,
     * whose grid is laid per object from an anchor a {@code StartPointDetermination} derives: no golden with cases
     * reads or writes one, and the case input lays no timeline on such a grid, so no case can confirm this branch's
     * reading of it against the reference interpreter.
     */
    private Member timeMember(Objecttype houder, String naam, boolean kenmerk, String description) {
        Member member = declared(houder, naam, kenmerk);
        String soort = kenmerk ? "kenmerk" : "attribuut";
        if (member.time() == null)
            throw new Refusal(description + " over het " + soort + " '" + member.naam() + "' van " + member.declarant().naam
                    + ", dat niet tijdsafhankelijk is");
        if (Timelines.morePeriodic(kenmerk ? member.declarant().kenmerkTijdlijnAantalLeden()
                : member.declarant().tijdlijnAantalLeden(), member.naam()))
            throw new Refusal(description + " over het " + soort + " '" + member.naam() + "' van " + member.declarant().naam
                    + " op een raster van meer perioden, dat geen bewerking draagt (RUNTIME-SEMANTICS ch. 3.2, 3.12)");
        if (!kenmerk && model.heeftVariabelStartpunt(member.declarant(), member.naam()))
            throw new Refusal(description + " over het attribuut '" + member.naam() + "' van " + member.declarant().naam + " op een"
                    + " tijdlijn met een variabel startpunt: " + STILL_NOT + "; geen golden met cases leest of schrijft er"
                    + " een, en de caselezer legt geen tijdlijn op zo'n raster");
        return member;
    }

    /** Member {@code naam} as declared on {@code houder}'s chain, its granularity set where it lies on a timeline. A
     *  member with dimensions is refused. */
    private Member declared(Objecttype houder, String naam, boolean kenmerk) {
        for (Objecttype t : houder.keten()) {
            if (kenmerk) {
                for (String k : t.kenmerken)
                    if (Namen.norm(k).equals(Namen.norm(naam)))
                        return new Member(ProducentConsument.elementVan(houder, naam, true), t, k, true, null, beanClass(t),
                                t.kenmerkGranulariteit.get(k));
            } else {
                for (Map.Entry<String, Datatype> e : t.attributen.entrySet())
                    if (Namen.norm(e.getKey()).equals(Namen.norm(naam))) {
                        String a = e.getKey();
                        // every position that asks this method reads or writes the member whole, as one value or as a
                        // timeline; a dimensioned one is read only per cell, through dimensionedMember
                        if (t.attribuutDimensies.containsKey(a))
                            throw new Refusal("het gedimensioneerde attribuut '" + a + "' van " + t.naam + ", gelezen"
                                    + " of geschreven buiten een DimensionCellRead of een vouw over een"
                                    + " dimensieselectie: " + STILL_NOT + "; geen golden met cases heeft het");
                        return new Member(ProducentConsument.elementVan(houder, naam, false), t, a, false, e.getValue(),
                                beanClass(t), t.attribuutGranulariteit.get(a));
                    }
            }
        }
        throw new Refusal("het " + (kenmerk ? "kenmerk" : "attribuut") + " '" + naam + "' is niet gedeclareerd op "
                + houder.naam + " of een supertype");
    }

    /** The dimensioned attribute {@code naam} as declared on {@code houder}'s chain (§3.6): its bean member is a
     *  {@code Map<label, value>}, read whole by a dimension cell or a fold over a dimension selection. An attribute that
     *  is not dimensioned, or that also lies on a timeline, is refused by name. */
    private Member dimensionedMember(Objecttype houder, String naam) {
        for (Objecttype t : houder.keten())
            for (Map.Entry<String, Datatype> e : t.attributen.entrySet())
                if (Namen.norm(e.getKey()).equals(Namen.norm(naam))) {
                    String a = e.getKey();
                    if (!t.attribuutDimensies.containsKey(a))
                        throw new Refusal("een dimensielezing van het attribuut '" + a + "' van " + t.naam + ", dat niet"
                                + " gedimensioneerd is");
                    if (t.attribuutGranulariteit.containsKey(a))
                        throw new Refusal("het gedimensioneerde attribuut '" + a + "' van " + t.naam + " op een"
                                + " tijdlijn: " + STILL_NOT + "; geen golden met cases heeft er een");
                    return new Member(ProducentConsument.elementVan(houder, naam, false), t, a, false, e.getValue(),
                            beanClass(t), null);
                }
        throw new Refusal("het attribuut '" + naam + "' is niet gedeclareerd op " + houder.naam + " of een supertype");
    }

    /** A path of to-one roles from a subject type over the reference fields of the beans: the walk
     *  {@link FactModelGenerator#navigationWalk} derives from the wire's bindings, the reference field of each hop
     *  over an input relation on its bean class ({@code fields}, which a plan reads the path along, and only for a path
     *  whose links are all input, {@link #created}), the hops, and the type the last hop reaches. {@code sleutel}
     *  names the subject type and the fields, as the {@link PathState} facts of the path carry it. */
    private record Path(String sleutel, Objecttype subject, List<String> roles, Objecttype reaches,
                       List<Binding.Field> fields, List<Hop> hops) {
        /** Whether a hop of the path goes over a relation a form creates, whose links change while the session fires:
         *  such a path is read over {@link RelationLink} facts, never off the beans. */
        boolean created() {
            return hops.stream().anyMatch(h -> h.relation() != null);
        }
    }

    /** One hop of a path or a range, from an object of type {@code van} to one of type {@code naar}: over an input
     *  relation by the reference field {@code veld} on the bean of one end ({@code onNear}: on the near object, else on
     *  the far one), over a relation a form creates by its {@link RelationLink} facts ({@code relation} set,
     *  {@code nearIsFirst} saying which end of the link the near object is, {@code veld} null). */
    private record Hop(Objecttype van, Objecttype naar, String veld, boolean onNear, String relation,
                       boolean nearIsFirst) {}

    /** A derived member read over a path: one family of path-standing rules. */
    private record PathRead(Path pad, Member member) {
        String sleutel() { return pad.sleutel() + " " + FactNames.member(member.sleutel()); }
    }

    private Path pad(Objecttype subject, List<RolBindingRef> bindings, Objecttype endpoint, String description) {
        FactModelGenerator.NavigationWalk w =
                FactModelGenerator.navigationWalk(model, subject, bindings, endpoint);
        if (w == null) throw new Refusal(description + ": de navigatie resolvet niet vanaf " + subject.naam);
        if (w.toMany()) throw new Refusal(description + ": de navigatie gaat over een rol naar veel");
        if (w.roles().size() != bindings.size())
            throw new IllegalStateException(description + ": de wandeling telt " + w.roles().size() + " stappen en de draad "
                    + bindings.size() + " bindingen");
        List<Binding.Field> fields = new ArrayList<>();
        List<Hop> hops = new ArrayList<>();
        Objecttype cur = subject;
        for (int i = 0; i < w.roles().size(); i++) {
            String rol = w.roles().get(i);
            Objecttype next = w.reaches().get(i);
            Hop created = linkHop(bindings.get(i), cur, next);
            if (created != null) {
                hops.add(created);
                cur = next;
                continue;
            }
            // a role whose name a declared member took has no reference field of its own (§13.3.9)
            if (!FactModelGenerator.hasReferenceField(model, cur, rol, next))
                throw new Refusal(description + ": " + cur.naam + " heeft geen referentieveld '" + rol + "' naar " + next.naam
                        + " (§13.3.9 opslagrichting)");
            fields.add(new Binding.Field(beanClass(cur), rol));
            hops.add(new Hop(cur, next, rol, true, null, false));
            cur = next;
        }
        return new Path(subject.naam + ":" + String.join(".", w.roles()), subject, w.roles(), cur, List.copyOf(fields),
                List.copyOf(hops));
    }

    /** The hop over {@code binding} from {@code van} to {@code naar} where its relation is one a form creates, read over
     *  {@link RelationLink} facts; null for an input relation. The binding names the role the far end fills, so the near
     *  end is the first of the link where that role is the fact type's second. */
    private Hop linkHop(RolBindingRef binding, Objecttype van, Objecttype naar) {
        if (binding == null || binding.feittype() == null || !createdRelations.contains(Namen.norm(binding.feittype())))
            return null;
        RegelSpraakModel.Feittype ft = feittype(binding.feittype());
        if (ft == null || ft.rollen.size() != 2 || ft.wederkerig)
            throw new Refusal("een stap over de gecreëerde relatie '" + binding.feittype() + "' die geen relatie van twee"
                    + " rollen is: " + STILL_NOT);
        boolean farIsSecond = Namen.norm(ft.rollen.get(1).naam()).equals(Namen.norm(binding.rol()));
        return new Hop(van, naar, null, false, Namen.norm(ft.naam), farIsSecond);
    }

    /** The declared fact type named {@code naam}, compared as the verifier compares names, or null. */
    private RegelSpraakModel.Feittype feittype(String naam) {
        for (RegelSpraakModel.Feittype ft : model.feittypen.values())
            if (Namen.norm(ft.naam).equals(Namen.norm(naam))) return ft;
        return null;
    }

    private Class<?> beanClass(Objecttype ot) {
        try {
            return Class.forName(FactModelGenerator.PAKKET + "." + Namen.ident(ot.naam, true), true, beanLoader);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("geen beanklasse voor objecttype " + ot.naam, e);
        }
    }

    private String id(String soort) { return soort + "_" + (idSeq++); }

}
