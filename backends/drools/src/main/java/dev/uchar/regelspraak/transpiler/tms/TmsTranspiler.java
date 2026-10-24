package dev.uchar.regelspraak.transpiler.tms;

import dev.uchar.regelspraak.graph.ProducerConsumer;
import dev.uchar.regelspraak.message.Msg;
import dev.uchar.regelspraak.refusal.DroolsCode;
import dev.uchar.regelspraak.refusal.Reason;
import dev.uchar.regelspraak.graph.ProducerConsumer.ElementKey;
import dev.uchar.regelspraak.ir.Condition;
import dev.uchar.regelspraak.ir.Default;
import dev.uchar.regelspraak.ir.Expr;
import dev.uchar.regelspraak.ir.Validity;
import dev.uchar.regelspraak.ir.PrioritizedDefaults;
import dev.uchar.regelspraak.ir.ResolvedConsistency;
import dev.uchar.regelspraak.ir.ResolvedDayKindDefinition;
import dev.uchar.regelspraak.ir.ResolvedFactCreation;
import dev.uchar.regelspraak.ir.ResolvedObjectCreation;
import dev.uchar.regelspraak.ir.ResolvedRule;
import dev.uchar.regelspraak.ir.ResolvedUniqueness;
import dev.uchar.regelspraak.ir.ResolvedUniquenessUnion;
import dev.uchar.regelspraak.ir.ResolvedAllocation;
import dev.uchar.regelspraak.ir.Spelling;
import dev.uchar.regelspraak.core.Literals;
import dev.uchar.regelspraak.core.TimeCalculation;
import dev.uchar.regelspraak.model.Datatype;
import dev.uchar.regelspraak.model.Unit;
import dev.uchar.regelspraak.model.RationalParser;
import dev.uchar.regelspraak.model.RegelSpraakModel;
import dev.uchar.regelspraak.model.RegelSpraakModel.ObjectType;
import dev.uchar.regelspraak.model.RegelSpraakModel.Parameter;
import dev.uchar.regelspraak.model.RegelSpraakModel.RoleBindingRef;
import dev.uchar.regelspraak.runtime.Accessors;
import dev.uchar.regelspraak.runtime.Breuk;
import dev.uchar.regelspraak.runtime.RegelSpraakRuntime;
import dev.uchar.regelspraak.schema.FactModelGenerator;
import dev.uchar.regelspraak.transpiler.core.SourcePath;
import dev.uchar.regelspraak.transpiler.core.DayKinds;
import dev.uchar.regelspraak.transpiler.core.Coverage;
import dev.uchar.regelspraak.transpiler.core.EffectiveUnit;
import dev.uchar.regelspraak.transpiler.core.UnitConflict;
import dev.uchar.regelspraak.transpiler.core.Host;
import dev.uchar.regelspraak.transpiler.core.Namen;
import dev.uchar.regelspraak.transpiler.core.Operators;
import dev.uchar.regelspraak.transpiler.core.StandingTargets;
import dev.uchar.regelspraak.transpiler.core.Timelines;
import dev.uchar.regelspraak.transpiler.core.WindowPosition;
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
import java.util.Collection;
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
 * rendering of this analysis. Neither branch carries any of them now; the fire cap of {@code DroolsBackend.fire}
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
 * so an initialisation's guard reads the input of its derived target off the bean as well; {@link #setDerivedValues}
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
 * or one of the faults of {@link ComputeFault}: a date step or a duration count outside the range of ch. 3.8, a context
 * value or a validity window without an evaluation date, an {@code Unresolved} node the evaluation reaches — is a
 * {@link Fault} inserted logically and never an exception out of the fire: the branch passes intermediate states the
 * reference interpreter does not see, and a fault thrown there could not be withdrawn when the state passes. The
 * consequence that computes a value inserts the fault instead of the value. A condition is tested in the left-hand
 * side, where Drools cannot take an exception as a fact, so its test answers that it does not hold where it faults,
 * and a rule of the form's own inserts the fault ({@link #conditionFaultRule}). A fold's filter that faults for a
 * member inserts no fault but a {@link FilterFault}: the form that reads the fold faults where its own evaluation reaches
 * the fold, as the reference interpreter evaluates the filter only there, with the fault of the first member in fold
 * order whose filter faults, a place the case input states per to-many list ({@link ListPlace}) ({@link #fold}). Only
 * the faults that stand at
 * the fixpoint count: {@link #setDerivedValues} reports them before it writes anything back. A division of a filled
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
 * is made. Its rules sit in agenda groups of their own ({@link #FACT_CREATION}, {@link #OBJECT_CREATION}) and the driver
 * fires them in generations ({@code DroolsBackend.fire}): the rule layer to rest, then the fact creations once and the
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
 * created link is in no bean field, and a second distinct filler of a to-one role is a {@link Fault} at the fixpoint.
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
     *  driver gives this group the focus once per generation ({@code DroolsBackend.fire}). An agenda filter cannot
     *  hold them back instead: Drools 8.44 takes a match the filter rejects off the agenda for good
     *  ({@code RuleExecutor.fire}, {@code cancelAndContinue}), so a creation rejected while the rule layer fires would
     *  never fire later. */
    public static final String FACT_CREATION = "uchar-feitcreatie";

    /** The agenda group of the object creations. The driver puts it on the focus stack beneath the group of the fact
     *  creations ({@link #FACT_CREATION}), so in a generation the fact creations fire first and the object creations right
     *  after them, before the rule layer fires again: every creation of a generation reads the same settled world, and an
     *  object creation does not read what a fact creation of its generation links (RUNTIME-SEMANTICS ch. 5.5,
     *  Generations). */
    public static final String OBJECT_CREATION = "uchar-objectcreatie";

    /** The creation budget (RUNTIME-SEMANTICS ch. 5.5): the deepest an object creation makes an object in its stratum.
     *  A mint at one depth more is the fault, at the making, as in the reference interpreter and the ASP backend
     *  ({@code oc_budget}); a fact creation makes no object and has no budget. The depth follows the chain of makings,
     *  so a creation whose condition waits generations for a link or a value keeps its subject's depth, and the driver
     *  needs no bound on the generations: under this budget a stratum makes finitely many objects and links. */
    public static final int CREATION_BUDGET = 100;

    /** The most values one condition of this branch reads at once: the DSL's standalone test takes at most thirteen
     *  bound variables. A consequence has no such ceiling ({@link Consequence}). */
    static final int MAX_READS = 13;

    static final String INCONSISTENCY = FactModelGenerator.PACKAGE + ".Inconsistentie";

    private final RegelSpraakModel model;
    private final ClassLoader beanLoader;
    /** The switch relations of the instance-ordered parts ({@link #instanceWalks}), computed on first use. */
    private List<RoleBindingRef> instanceWalks;
    private final List<PrioritizedDefaults> defaults;
    private final Map<Object, List<RoleBindingRef>> navigationBindings;
    /** The nodes that carry the form anchor (IR-SPEC ch. 4.1a, WF-122), by identity: each is read on the subject of the
     *  form, where the filter or the test around it moved the subject to an element ({@link Reads#atForm}). */
    private final Set<Object> anchored;
    /** The one-step range of a fold ({@code sourceRole}), by node. */
    private final Map<Object, RoleBindingRef> roleBindings;
    /** The multi-step range of a fold ({@code sourcePath}), by node. */
    private final Map<Object, List<RoleBindingRef>> sourcePaths;
    /** The folds lowered so far, per node and fold subject: a node read again, by a rule's fault rule or by a default
     *  that two effective stacks share, reads the contributions already made for it. */
    private final Map<Object, Map<ObjectType, Fold>> folds = new IdentityHashMap<>();
    /** The contribution rules of the folds the lowered forms read, by the fold's key. */
    private final Map<String, List<RulePlan>> contributionRules = new LinkedHashMap<>();
    private int foldCounter;
    /** While the rules of a fold are built ({@link #foldFor}), the binders in scope at the fold, by normalised name:
     *  the rule that reads the fold binds them, its contribution rules do not ({@link Reads#outside}). */
    private Set<String> foldBinders = Set.of();
    /** The name of the form being lowered, as the manifest shows it: the key of its folds starts with it, and a fault in
     *  a fold's filter is named after it. */
    private String form;
    /** The datatype and unit of an expression as the other branches derive them (RUNTIME-SEMANTICS ch. 2.10). */
    private final EffectiveUnit eff;
    /** Every member a form of the document writes, lowered or not. A member that no form writes is input. */
    private Map<ElementKey, Set<String>> writers = Map.of();
    /** Per attribute, the assignments and computations of the document that write it. */
    private final Map<ElementKey, List<ResolvedRule>> computeRules = new LinkedHashMap<>();
    /** Per attribute, the initialisations of the document that write it. */
    private final Map<ElementKey, List<ResolvedRule>> initialisations = new LinkedHashMap<>();
    /** The members a defaults group writes, on their declaring type. */
    private final Set<ElementKey> defaultsTargets = new LinkedHashSet<>();
    /** The effective stacks of RUNTIME-SEMANTICS ch. 7.10 and the fold of ordinary rules into them. */
    private Stacks stacks;
    /** The targets on which a computed leeg writes nothing (RUNTIME-SEMANTICS ch. 7.7 and 7.8), classified by the class
     *  both branches share. */
    private StandingTargets standingTargets;
    /** The names of the document's rules in its order, each once: the sentence of two writers that disagree names them
     *  in this order, as the reference interpreter's pass runs them (RUNTIME-SEMANTICS ch. 5.7), and the reader of the
     *  fixpoint takes, of writers that agree, the value of the first ({@link WritersConflict#inDocumentOrder}). A
     *  list, so that a plan carries the order itself and a DRL text can spell it. */
    private final List<String> documentOrder = new ArrayList<>();
    /** The object types of the document in its order ({@link #types}), and at the same place the simple name of each
     *  one's bean class (the one {@link #beanClass} loads): a sentence names an object by its own type. By name, so
     *  that building it loads no class. */
    private final List<String> typeClasses = new ArrayList<>(), types = new ArrayList<>();
    private final List<Coverage> coverage = new ArrayList<>();
    /** The form each rule of the model belongs to, by the rule's name ({@link #formOf}). */
    private final Map<String, String> formOfRule = new HashMap<>();
    /** The rules of the model that belong to no form of the document ({@link #formOf}). */
    private final Set<String> rulesWithoutForm = new LinkedHashSet<>();
    /** The default each rule of an effective stack lowers, by the rule's name, the contribution rules of the folds the
     *  default reads among them ({@link #originOf}). */
    private final Map<String, Default> defaultOfRule = new HashMap<>();
    /** The rules of an effective stack that lower the stack itself, no default of it ({@link #originOf}). */
    private final Set<String> stackOwnRules = new HashSet<>();
    /** The rule each reading rule of a build with readings reads, by the reading rule's name ({@link #originOf}). */
    private final Map<String, String> readingRuleOf = new HashMap<>();
    /** The role each rule of a form has in the lowering of its form, default or stack, by the rule's name, noted where
     *  the lowering builds the rule ({@link #as}, {@link #originOf}); a rule of no form has none. */
    private final Map<String, RuleOrigin.Role> roleOfRule = new HashMap<>();
    /** The form whose lowering noted the role of each rule first ({@link #as}), by the rule's name. */
    private final Map<String, String> notedBy = new HashMap<>();
    /** The names of rules that the lowerings of two forms of the document both build, with those forms in the order
     *  they noted the name: a form named like a helper rule of another one ({@code R} beside
     *  {@code R: fout in de voorwaarde}), which WF-020 lets pass. Such a name has no one role and no one origin. Where
     *  both forms lower, the model holds two rules of it, which {@code DroolsBackend} refuses by name (DROOLS-055), and
     *  where one does not, the backend refuses that form from the manifest; so the lowering does not fail on it first
     *  ({@link #as}, {@link #run}). */
    private final Map<String, List<String>> sharedNames = new LinkedHashMap<>();
    /** The fold each rule of a fold belongs to, by the rule's name: the node of the document the fold is, an
     *  aggregation or a quantifier fold, noted where {@link #foldFor} builds the fold's rules ({@link #originOf}). */
    private final Map<String, Object> foldOfRule = new HashMap<>();
    /** While a default of a stack lowers, and while the stack registers that default's reads: the default, which the
     *  contribution rules of the folds registered meanwhile lower ({@link Reads#recorder}); null at every other
     *  moment. */
    private Default lowering;
    private final List<DefaultsGroup> defaultsGroups = new ArrayList<>();
    /** The knots of every effective stack the branch lowered, by its name, which {@link #decisionsOf} lays the control
     *  facts of a session on. */
    private final Map<String, StackLayout> layouts = new LinkedHashMap<>();
    /** The JSON pointer of every default the document declares, by identity ({@link #pointerOf}); made at the first
     *  call. */
    private Map<Default, String> pointers;
    /** The manifest names that name no declaration, with the element each comes from ({@link #generated}). */
    private final Map<String, RegelSpraakModel.GeneratedFrom> generated = new LinkedHashMap<>();
    /** Every effective stack with its type and its composition, by the stack's name ({@link #stacks}). */
    private final Map<String, Stack> effectiveStacks = new LinkedHashMap<>();
    /** The members the lowered rules derive, by key. The key is normalised, and the setter a derivation is written back
     *  through is the bean property of the declared spelling, which the key no longer carries. */
    private final Map<ElementKey, Member> derived = new LinkedHashMap<>();
    /** The members a lowered rule reads through their standing, on its subject or at the end of a path: the derived
     *  ones, and every attribute on a timeline ({@link Reads#timeline}). Each gets its standing rules. */
    private final Map<ElementKey, Member> read = new LinkedHashMap<>();
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
    /** The names of the lowered creation rules, those of {@link #FACT_CREATION} and of {@link #OBJECT_CREATION}: the driver
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
    /** Where the nodes of the document stand on the wire, in a build with readings (card fixpoint-readings), which
     *  plans a reading rule beside the rule of every form with a condition, and of every form without one that computes
     *  a value (card readings-of-unconditional-forms) ({@link #readingsOf}); null in a build without, whose plan is that
     *  of a build before the card. */
    private final dev.uchar.regelspraak.loader.NodePositions positions;
    /** Where the nodes of the document stand on the wire, for {@link #originOf} alone, which names the fold of a rule of
     *  a fold by it: the positions the loader gave with the document, which every build takes. The lowering reads none
     *  of it, so a build without readings plans what it planned before it had them. */
    private final dev.uchar.regelspraak.loader.NodePositions origins;
    /** While the condition and the value of a form's own rule compile in a build with readings: the term the analysis
     *  plans for each node of the IR it compiles there, by identity ({@link #note}); null at every other moment. */
    private Map<Object, Term> noted;
    /** Beside {@link #noted}: the unit of each expression node, by the effective-type law (RUNTIME-SEMANTICS ch. 2.10). */
    private Map<Object, Unit> notedUnits;
    /** Beside {@link #noted}: the nodes whose reading follows from the outcomes of terms of the rule ({@link Derived},
     *  {@link #noteDerived}), by identity. */
    private Map<Object, Derived> notedDerived;
    /** While the rules of a fold are built ({@link #foldFor}): whether the fold's subject is the subject of the form being
     *  lowered, which its rules then bind as {@link #FOLD_SUBJECT}, so that an anchored node reads there. */
    private boolean foldOnForm;
    /** Above zero while the rules of a fold are built ({@link #foldFor}): a node compiled there is evaluated per member
     *  in those rules, not in the rule that reads the fold, so it is not noted. */
    private int memberwise;
    /** The reading rules of a build with readings, in the order their forms lowered. */
    private final List<ReadingRule> readingRules = new ArrayList<>();

    private TmsTranspiler(RegelSpraakModel model, ClassLoader beanLoader, List<PrioritizedDefaults> defaults,
                          Map<Object, List<RoleBindingRef>> navigationBindings, Set<Object> anchored,
                          Map<Object, RoleBindingRef> roleBindings,
                          Map<Object, List<RoleBindingRef>> sourcePaths, EffectiveUnit eff,
                          dev.uchar.regelspraak.loader.NodePositions positions,
                          dev.uchar.regelspraak.loader.NodePositions origins) {
        this.positions = positions;
        this.origins = origins;
        this.model = model;
        this.beanLoader = beanLoader != null ? beanLoader : TmsTranspiler.class.getClassLoader();
        this.defaults = defaults;
        this.navigationBindings = navigationBindings;
        this.anchored = anchored;
        this.roleBindings = roleBindings;
        this.sourcePaths = sourcePaths;
        this.eff = eff;
        this.dayKinds = new DayKinds(model);
    }

    /** The switch relations of the document's instance-ordered parts, as the hops that reach the predecessor
     *  ({@link Cycles#instanceWalks}): a case reader refuses a cycle in their links (EF-033, RUNTIME-SEMANTICS
     *  ch. 6.2). */
    public synchronized List<RoleBindingRef> instanceWalks() {
        if (instanceWalks == null)
            instanceWalks = Cycles.instanceWalks(model, defaults, navigationBindings, anchored, roleBindings, sourcePaths);
        return instanceWalks;
    }

    /** Build the model and its coverage manifest from the loaded model and every side table the loader returns. Each
     *  table is required, also those no lowered form reads yet: a build that could leave one out could also lower a
     *  form that needs it as if the document carried none. So are the positions of the document's nodes
     *  ({@code IrLoader.loadWithPositions}), by which {@link #originOf} names the fold of a rule of a fold: no build
     *  leaves a rule without the origin every other build gives it. The lowering reads none of them. Fact beans
     *  resolve against {@code beanLoader}; null routes to this transpiler's own loader. */
    public static TmsTranspiler build(RegelSpraakModel m, ClassLoader beanLoader, List<PrioritizedDefaults> defaults,
            Map<Object, RoleBindingRef> roleBindings, List<Expr.Let> lets,
            Map<Object, List<RoleBindingRef>> sourcePaths,
            Map<Object, List<RoleBindingRef>> navigationBindings, Set<Object> anchored,
            dev.uchar.regelspraak.loader.NodePositions positions) {
        return build(m, beanLoader, defaults, roleBindings, lets, sourcePaths, navigationBindings, anchored, null,
                Objects.requireNonNull(positions, "positions"));
    }

    /** The build without readings of the loaded {@code document}: the build of
     *  {@link #build(RegelSpraakModel, ClassLoader, List, Map, List, Map, Map, Set, dev.uchar.regelspraak.loader.NodePositions)} with every side table the loader
     *  returns and the positions of the document's nodes, which {@link #originOf} alone reads, to name the fold of a
     *  rule of a fold ({@link RuleOrigin#fold}). */
    public static TmsTranspiler build(dev.uchar.regelspraak.loader.IrLoader.Positioned document,
                                      ClassLoader beanLoader) {
        Objects.requireNonNull(document, "document");
        dev.uchar.regelspraak.loader.IrLoader.Loaded l = document.loaded();
        return build(l.model(), beanLoader, l.prioritizedDefaults(), l.roleBindings(), l.lets(), l.sourcePaths(),
                l.navigationBindings(), l.anchored(), null, Objects.requireNonNull(document.positions(), "positions"));
    }

    /** A build with readings (card fixpoint-readings) of the loaded {@code document}: the build of
     *  {@link #build(RegelSpraakModel, ClassLoader, List, Map, List, Map, Map, Set, dev.uchar.regelspraak.loader.NodePositions)}, and beside the rule of every form with a
     *  condition, or without one that computes a value, a reading rule, whose facts {@link #readingsOf} reads, numbering
     *  the nodes of a form by the positions the loader gives with the document, by which {@link #originOf} names the
     *  fold of a rule of a fold as well. It takes the loaded document whole, so it leaves no side table out. */
    public static TmsTranspiler withReadings(dev.uchar.regelspraak.loader.IrLoader.Positioned document,
                                             ClassLoader beanLoader) {
        Objects.requireNonNull(document, "document");
        dev.uchar.regelspraak.loader.IrLoader.Loaded l = document.loaded();
        dev.uchar.regelspraak.loader.NodePositions positions = Objects.requireNonNull(document.positions(), "positions");
        return build(l.model(), beanLoader, l.prioritizedDefaults(), l.roleBindings(), l.lets(), l.sourcePaths(),
                l.navigationBindings(), l.anchored(), positions, positions);
    }

    /** The build, with {@code positions} for a build with readings and null for one without, and {@code origins}, the
     *  positions of the document's nodes by which {@link #originOf} names the folds, in every build. */
    private static TmsTranspiler build(RegelSpraakModel m, ClassLoader beanLoader, List<PrioritizedDefaults> defaults,
            Map<Object, RoleBindingRef> roleBindings, List<Expr.Let> lets,
            Map<Object, List<RoleBindingRef>> sourcePaths,
            Map<Object, List<RoleBindingRef>> navigationBindings, Set<Object> anchored,
            dev.uchar.regelspraak.loader.NodePositions positions,
            dev.uchar.regelspraak.loader.NodePositions origins) {
        Objects.requireNonNull(defaults, "side table 'defaults' is null — pass an empty list for a document without one");
        Objects.requireNonNull(roleBindings, "side table 'roleBindings' is null — pass an empty map for a document without one");
        Objects.requireNonNull(lets, "side table 'lets' is null — pass an empty list for a document without one");
        Objects.requireNonNull(sourcePaths, "side table 'sourcePaths' is null — pass an empty map for a document without one");
        Objects.requireNonNull(navigationBindings, "side table 'navigationBindings' is null — pass an empty map for a document without one");
        Objects.requireNonNull(anchored, "side table 'anchored' is null — pass an empty set for a document without one");
        Objects.requireNonNull(origins, "origins");
        // copied by identity: the loader keys its tables on the nodes, which are records with value equality
        Map<Object, List<RoleBindingRef>> nav = new IdentityHashMap<>(navigationBindings);
        Set<Object> anchors = Collections.newSetFromMap(new IdentityHashMap<>());
        anchors.addAll(anchored);
        TmsTranspiler t = new TmsTranspiler(m, beanLoader, defaults, nav, anchors, new IdentityHashMap<>(roleBindings),
                new IdentityHashMap<>(sourcePaths), new EffectiveUnit(lets), positions, origins);
        t.modelplan = t.run();
        t.model0 = PatternDslRendering.model(t.modelplan);
        return t;
    }

    public Model model() { return model0; }

    /** Whether this is a build with readings ({@link #withReadings}), whose sessions {@link #readingsOf} reads. */
    public boolean readings() { return positions != null; }

    /** The coverage manifest: one verdict per form, the rules in document order, then every other family. */
    public List<Coverage> coverage() { return coverage; }

    /**
     * The names of the manifest ({@link #coverage}) that name no declaration of the document, each with the declared
     * element it comes from, in the order of the manifest: an effective defaults stack ({@code defaults A:<type>.<member>},
     * {@code defaults K:<type>.<member>}) from the member it decides, as declared on its owner's chain; a version of a
     * form with several ({@code <form> #<k>}) from that form. Every other name of the manifest is the name of a form of
     * the model. A side table: the lowering reads none of it. The type a stack runs for and its composition are beside
     * it, in {@link #stacks}.
     */
    public Map<String, RegelSpraakModel.GeneratedFrom> generated() {
        return java.util.Collections.unmodifiableMap(generated);
    }

    /**
     * Every effective defaults stack of {@link #generated}, by the stack's name, in the order of the manifest, with the
     * type it runs for and its composition ({@link Stack}), a refused stack too. A name of the manifest that is no
     * stack, a version of a form, has no entry: {@code get} gives null there, as {@code generated} gives another form.
     * Beside {@code generated} and not in it, since {@link RegelSpraakModel.GeneratedFrom} is the frontend's record,
     * which the wire carries as {@code origin} (IR-SPEC ch. 2.13). Filled while the analysis is built, as each stack has
     * lowered or been refused, so a stack whose defaults cannot be named by their pointer fails every build, as a rule
     * without a role does. A side table: the lowering reads none of it.
     */
    public Map<String, Stack> stacks() {
        return java.util.Collections.unmodifiableMap(effectiveStacks);
    }

    /**
     * One effective defaults stack of {@link #stacks}.
     *
     * @param type        the type the stack runs for: the owner of the effective stack (RUNTIME-SEMANTICS ch. 7.10), on
     *                    whose instances of exactly that type its rules run, as an element of the model
     *                    ({@link RegelSpraakModel.Designatable#objectType}). It is the type that declares the member
     *                    {@code generated} gives, or a subtype of it, so two stacks that decide one inherited member for
     *                    two types have one entry each in {@code generated} with the same member, and each its own type
     *                    here. A refused stack has one too
     * @param composition its composition ({@link StackComposition}): its defaults in the order of priority, across the
     *                    groups of the types ch. 7.10 composes it from, with the exception groups below each default
     *                    (ch. 7.9), each by the pointer {@link #decisionsOf} and {@link #originOf} name a default by.
     *                    Read off the knots the lowering laid for the stack, on which {@link #decisionsOf} lays the
     *                    control facts of a session, so its chains at the root are the {@link DefaultsDecision#rootChains}
     *                    of every record of the stack; never off the labels in the names of the rules or the order of the
     *                    DRL text. Null for a stack the lowering refused, which laid no knots; the reason is in the
     *                    manifest ({@link #coverage}). {@code DroolsBackend.build} refuses a document with a refused form
     *                    whole, so in a build it returns every stack has a composition
     */
    public record Stack(RegelSpraakModel.Designatable type, StackComposition composition) {
        public Stack {
            Objects.requireNonNull(type, "type");
        }
    }

    /** The chains {@code chains} of knots, each knot by the pointer of its default and its members below it. */
    private StackComposition composition(List<List<Node>> chains) {
        return new StackComposition(chains.stream().map(chain -> chain.stream()
                .map(k -> new StackComposition.Entry(pointerOf(k.d()), k.hasMembers() ? composition(k.members()) : null))
                .toList()).toList());
    }

    /**
     * The walk of {@code read}, a read of the model this analysis was built from: the role bindings the loader read
     * beside it (IR-SPEC ch. 4.1a, the wire's {@code path}), one per hop from the subject outward, the walk the
     * branch reads it along. Empty for a read of the subject's own member. A read the loader of this model did not
     * make has none either, since the table is keyed on the node: the walk of a read of a model the frontend
     * resolved is the one its emitter writes, {@code IrEmitter.navigationBindings}. A consumer of the written-back
     * beans reaches the object of the read with {@code DroolsBackend.reached}.
     */
    public List<RoleBindingRef> navigationBindings(Expr.AttributeReference read) {
        Objects.requireNonNull(read, "read");
        return navigationBindings.getOrDefault(read, List.of());
    }

    /**
     * The form of the document that rule {@code ruleName} of the model belongs to, by the name the manifest gives it
     * ({@link #coverage}): the form whose lowering built the rule, with every rule that lowering adds beside the form's
     * own (its fault and deposit rules, the keys of a uniqueness, the recipients of an allocation, the chains and
     * agreements of an effective defaults stack), and for the contribution rules of a fold the form that first
     * registered the fold, also where a later form reads the same fold. Null for a rule of no form, one the lowering
     * keeps for every form alike: a standing, a path or channel standing, the standing of a parameter or of the
     * evaluation date, a link of the input over a created relation and its second filler, and the comparison of the
     * writers of one target. A name of no rule of the model is refused.
     */
    public String formOf(String ruleName) {
        String form = formOfRule.get(ruleName);
        if (form != null || rulesWithoutForm.contains(ruleName)) return form;
        throw new IllegalArgumentException("geen regel van dit model: '" + ruleName + "'");
    }

    /**
     * Where one rule of the model comes from ({@link TmsTranspiler#originOf}): the form of the document whose lowering
     * built it, in an effective defaults stack the default the rule lowers, the role the rule has in that lowering, and
     * for a rule of a fold the fold it belongs to. The name of a rule of a stack does not say the default: its label
     * ({@code default 0.1.2}) is the lowering's own numbering of the knots of the effective stack, which ch. 7.10
     * composes from the groups of several types, and a contribution rule of a fold names its fold by the lowering's
     * own number ({@code vouw 3}), not the node of the document nor the default whose condition or consequence reads
     * it. The role is a fixed key where the name has the lowering's wording.
     *
     * @param form    the form, by the name the manifest gives it, as {@link TmsTranspiler#formOf} gives it; for a reading
     *                rule, which {@code formOf} leaves out so that no observer counts its firing as one of a form, the
     *                form of the rule it reads. Null only for {@link Kind#NO_FORM}
     * @param kind    what of its form the rule lowers
     * @param role    what the rule does in the lowering of its form, its default or its stack; null only for
     *                {@link Kind#NO_FORM}, and one of the roles {@link Role#kinds} admits for {@code kind}
     * @param pointer for {@link Kind#DEFAULT} the default, as {@link DefaultsDecision} names one: the JSON pointer of the
     *                {@code Default} node, {@code /rules/prioritizedDefaults/3/defaults/1/exceptions/members/0/defaults/0},
     *                or for an ordinary rule a subtype's stack takes in (ch. 7.10) the version of that rule,
     *                {@code /rules/rules/0/versions/0}; null for every other kind
     * @param fold    for a role of a fold ({@link Role#inFold}) the fold, as the JSON pointer of its node in the document,
     *                in the form {@code NodePositions} gives it: an {@code Aggregation},
     *                {@code /rules/prioritizedDefaults/3/defaults/0/justification/left}, or a {@code QuantifierFold}; the
     *                one node every form that reads the fold reads, below the form or the default that
     *                {@code form} and {@code pointer} name. Null for every other role
     */
    public record RuleOrigin(String form, Kind kind, Role role, String pointer, String fold) {

        public RuleOrigin {
            Objects.requireNonNull(kind, "kind");
            if ((form == null) != (kind == Kind.NO_FORM) || (role == null) != (kind == Kind.NO_FORM)
                    || (pointer == null) == (kind == Kind.DEFAULT) || (fold != null) != (role != null && role.inFold())
                    || role != null && !role.kinds().contains(kind))
                throw new IllegalArgumentException("de herkomst " + kind + " met vorm " + form + ", rol " + role
                        + ", pointer " + pointer + " en vouw " + fold);
        }

        public enum Kind {
            /** A rule of no form, one the lowering keeps for every form alike ({@link TmsTranspiler#formOf} gives
             *  null). It has no role. */
            NO_FORM,
            /** A rule of a form that is no effective defaults stack: the form's own rule and every rule its lowering
             *  adds beside it, the rules of the folds it registered among them. */
            FORM,
            /** A rule of an effective stack that lowers one default of it: the rule of the default's own decision, its
             *  fall-back where it has exception groups, the fault rule of its justification, the pass-on, difference and
             *  agreement rules that reconcile its exception groups (ch. 7.9), the rules of the folds its justification
             *  or consequence reads, and its reading rule. */
            DEFAULT,
            /** A rule of an effective stack that lowers the stack itself, no default of it: the pass-on, difference and
             *  agreement rules that reconcile the groups at its root (ch. 7.2), and the fault of a session without an
             *  evaluation date where a group of the stack has a dated validity. */
            STACK
        }

        /**
         * What a rule does in the lowering of its form, its default or its stack, as the lowering noted it when it
         * built the rule: a fixed key, which {@link #key} spells for a consumer that writes it out, with the kinds of
         * origin it occurs with ({@link #kinds}). Where a form and a default have the same kind of rule, the role is
         * the same: the fault rule of a condition and of a justification, the rules of a fold, the reading rule.
         */
        public enum Role {
            /** The rule of the form itself: of a rule the rule that writes its target or attributes its characteristic,
             *  of a consistency the rule that deposits its inconsistency, of a uniqueness the rule that marks the
             *  instances whose key collides, of an allocation the rule that distributes, of a creation the rule that
             *  makes; and of a default the rule of its own decision, an ordinary rule a subtype's stack takes in
             *  among them (ch. 7.10). */
            OWN("own", Kind.FORM, Kind.DEFAULT),
            /** The rule of a default's own decision where none of its exception groups settles (RUNTIME-SEMANTICS
             *  ch. 7.9). */
            FALLBACK("fallback", Kind.DEFAULT),
            /** The rule that inserts the fault of a condition that can fault: a form's condition or assertion, a
             *  default's justification. */
            CONDITION_FAULT("condition-fault", Kind.FORM, Kind.DEFAULT),
            /** The rule that deposits the §9.5 inconsistency where the evaluation of a form's condition divides a filled
             *  dividend by a leeg divisor or takes the root of a negative number (ch. 1.2); a default holds neither. */
            CONDITION_DEPOSIT("condition-deposit", Kind.FORM),
            /** The rule that faults a session without an evaluation date: beside a form with a bounded validity, and
             *  for a stack where a group of it has a dated validity (ch. 8.1). */
            NO_EVALUATION_DATE("no-evaluation-date", Kind.FORM, Kind.STACK),
            /** The reading rule of a build with readings (ch. 5.12) beside the rule it reads: the form's own rule, or
             *  the rule of a default's own decision or of its fall-back. */
            READING("reading", Kind.FORM, Kind.DEFAULT),
            /** Of the reconciliation of a default's exception groups (ch. 7.9), or of the groups at the root of a stack
             *  (ch. 7.2): the rule that passes on the first conflict among the members. */
            RECONCILE_PASS_ON("reconcile-pass-on", Kind.DEFAULT, Kind.STACK),
            /** Of that reconciliation: the rule that makes a conflict where the first deciding member and another one
             *  decide differently. */
            RECONCILE_DIFFERENCE("reconcile-difference", Kind.DEFAULT, Kind.STACK),
            /** Of that reconciliation: the rule that takes the decision of the first deciding member where no member
             *  conflicts and no other one decides differently. */
            RECONCILE_AGREEMENT("reconcile-agreement", Kind.DEFAULT, Kind.STACK),
            /** Of a fold (ch. 3.4): the rule that inserts the contribution of a member of the range that passes the
             *  filter; one per range, and one per selection of an instance-set union. */
            FOLD_CONTRIBUTION("fold-contribution", Kind.FORM, Kind.DEFAULT),
            /** Of a fold: the rule that notes the member for which the filter faults (ch. 5.11). */
            FOLD_FILTER_FAULT("fold-filter-fault", Kind.FORM, Kind.DEFAULT),
            /** Of a per-element fold over a timeline: the rule that notes the member whose timeline the case gives as
             *  the asOfDate-bucket scalar (ch. 3.6, 8.4). */
            FOLD_MEMBER_FAULT("fold-member-fault", Kind.FORM, Kind.DEFAULT),
            /** Of a fold: the rule that notes the member for which the filter deposits (ch. 5.10, 5.11). */
            FOLD_FILTER_DEPOSIT("fold-filter-deposit", Kind.FORM, Kind.DEFAULT),
            /** Of a uniqueness: the rule that inserts the key of an instance of one source (ch. 5.9). */
            UNIQUENESS_KEY("uniqueness-key", Kind.FORM),
            /** Of a uniqueness: the rule that faults an instance whose key component on a timeline the case gives as
             *  the asOfDate-bucket scalar (ch. 5.9, 8.4). */
            UNIQUENESS_KEY_FORM_FAULT("uniqueness-key-form-fault", Kind.FORM),
            /** Of a uniqueness: the rule that puts the violation of an instance on the §9.5 channel (ch. 5.6). */
            UNIQUENESS_CHANNEL("uniqueness-channel", Kind.FORM),
            /** Of an allocation: the rule that inserts a recipient of a source, with its weight, its ordering key and
             *  its cap (ch. 4). */
            ALLOCATION_RECIPIENT("allocation-recipient", Kind.FORM);

            private final String key;
            private final Set<Kind> kinds;

            Role(String key, Kind first, Kind... rest) {
                this.key = key;
                this.kinds = Collections.unmodifiableSet(java.util.EnumSet.of(first, rest));
            }

            /** The fixed key of this role, as a consumer writes it out. */
            public String key() { return key; }

            /** The kinds of origin a rule of this role has. */
            public Set<Kind> kinds() { return kinds; }

            /** Whether a rule of this role belongs to a fold, which {@link RuleOrigin#fold} then names. */
            public boolean inFold() {
                return this == FOLD_CONTRIBUTION || this == FOLD_FILTER_FAULT || this == FOLD_MEMBER_FAULT
                        || this == FOLD_FILTER_DEPOSIT;
            }
        }
    }

    /**
     * Where rule {@code ruleName} of the model comes from ({@link RuleOrigin}), for every rule of the model: its form as
     * {@link #formOf} gives it and, for a rule of an effective defaults stack, the default it lowers, as the pointer
     * {@link #decisionsOf} names a decider by, or that it lowers the stack itself; its role in that lowering; and for a
     * rule of a fold the node of the fold. A rule the stack adds to lower a default, a rule of a fold that the
     * default's justification or consequence reads among them, has that default; a rule of a fold that an ordinary
     * rule registered first has that rule's form, also where a stack that took the rule in reads the same fold. Read
     * off what the lowering noted while it built the rule, never off the rule's name; the fold by the position of its
     * node, which every build is given. A name of no rule of the model
     * is refused, as {@link #formOf} refuses it, and so is a name the lowerings of two forms both build, which has no one
     * origin ({@link #sharedNames}).
     */
    public RuleOrigin originOf(String ruleName) {
        String form = formOf(ruleName);
        List<String> sharing = sharedNames.get(ruleName);
        if (sharing != null)
            throw new IllegalStateException("de regel '" + ruleName + "' wordt gebouwd door de verlaging van de vormen "
                    + String.join(" en ", sharing) + ", en heeft dus geen herkomst; DroolsBackend weigert dit model"
                    + " (DROOLS-055)");
        if (form == null) {
            String own = readingRuleOf.get(ruleName);
            if (own == null) return new RuleOrigin(null, RuleOrigin.Kind.NO_FORM, null, null, null);
            RuleOrigin read = originOf(own);
            return new RuleOrigin(read.form(), read.kind(), roleOf(ruleName), read.pointer(), null);
        }
        RuleOrigin.Role role = roleOf(ruleName);
        String fold = role.inFold() ? foldOf(ruleName) : null;
        Default d = defaultOfRule.get(ruleName);
        if (d != null) return new RuleOrigin(form, RuleOrigin.Kind.DEFAULT, role, pointerOf(d), fold);
        if (stackOwnRules.contains(ruleName)) return new RuleOrigin(form, RuleOrigin.Kind.STACK, role, null, fold);
        if (layouts.containsKey(form))
            throw new IllegalStateException("de regel '" + ruleName + "' hoort bij de stapel " + form + ", en de"
                    + " verlaging noteerde niet van welke default of dat hij van de stapel zelf is");
        return new RuleOrigin(form, RuleOrigin.Kind.FORM, role, null, fold);
    }

    /** The role the lowering noted for {@code ruleName}, a rule of a form ({@link #as}). */
    private RuleOrigin.Role roleOf(String ruleName) {
        RuleOrigin.Role role = roleOfRule.get(ruleName);
        if (role == null)
            throw new IllegalStateException("de regel '" + ruleName + "' hoort bij een vorm, en de verlaging noteerde"
                    + " niet welke rol hij in haar verlaging heeft");
        return role;
    }

    /** The pointer of the fold of {@code ruleName}, a rule of a fold: the place of its node in the document. */
    private String foldOf(String ruleName) {
        Object node = foldOfRule.get(ruleName);
        if (node == null)
            throw new IllegalStateException("de regel '" + ruleName + "' is een regel van een vouw, en de verlaging"
                    + " noteerde niet van welke");
        dev.uchar.regelspraak.loader.NodePositions.Position at = origins.of(node);
        if (at == null)
            throw new IllegalStateException("de vouw van de regel '" + ruleName + "' is geen knoop die de lader las");
        return at.pointer();
    }

    /** Notes that the lowering builds {@code rule} in the role {@code role} ({@link #originOf}), and returns it. A rule
     *  of one name that one form builds in two roles is a fault of this class; a name that two forms build comes from
     *  the document's names, and is noted as shared ({@link #sharedNames}). */
    private RulePlan as(RuleOrigin.Role role, RulePlan rule) {
        String first = notedBy.putIfAbsent(rule.name(), form);
        if (first != null && !first.equals(form)) {
            List<String> sharing = sharedNames.computeIfAbsent(rule.name(), k -> new ArrayList<>(List.of(first)));
            if (!sharing.contains(form)) sharing.add(form);
            return rule;
        }
        RuleOrigin.Role before = roleOfRule.put(rule.name(), role);
        if (before != null && before != role)
            throw new IllegalStateException("de regel '" + rule.name() + "' is gebouwd als " + before + " en als "
                    + role);
        return rule;
    }

    /** The names of the creation rules of the model, in the agenda groups {@link #FACT_CREATION} and
     *  {@link #OBJECT_CREATION}; empty for a document without a lowered creation, which the driver fires in one go. */
    public Set<String> creationRules() { return Set.copyOf(creationRules); }

    /** The evaluation strata of the creation rules of the model, ascending, as the reference interpreter computes them
     *  over the verifier's dependency graph (RUNTIME-SEMANTICS ch. 6.3): the driver runs the generations of each in
     *  turn, and a creation rule fires in the phases of its own stratum and of every stratum above it
     *  ({@link CreationPhase}). Empty where {@link #creationRules} is. */
    public List<Integer> creationStrata() { return List.copyOf(creationStrata); }

    /** Whether a rule of the model reads a {@link ListPlace}: the filter fault rule of a fold whose filter can fault,
     *  the member fault rule of a per-element fold over a timeline, or the contribution rule of a quantifier fold whose
     *  matrix can fault or deposit, each over a range with a to-many step. Only then does the case input state the
     *  places of its to-many lists, and {@code DroolsBackend.insert} states them for a consumer. */
    public boolean readsListPlaces() { return !listFields.isEmpty(); }

    /** The reference fields under which a rule of the model reads a {@link ListPlace}: each the field a member of a
     *  to-many list holds toward the list's holder, named after the holder's role as the bean property is
     *  ({@link ListPlace#field}). Empty where {@link #readsListPlaces} is false. */
    public Set<String> listFields() { return Set.copyOf(listFields); }

    /**
     * The size of the lowering of one effective defaults stack, counted when it is built: {@code name} as the manifest
     * and the control facts name it, whether it took the general form (exception groups, or more than one group at the
     * root) or the simple one, the number of Drools rules it consists of, and the kinds of control fact those rules
     * insert ({@link ControlFact}). The standing rules of the target are shared with every other reader and not counted.
     */
    public record DefaultsGroup(String name, boolean general, int rules, List<String> controlFacts) {}

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
        private final Reason reason;

        DrlRefusal(Reason reason, Throwable cause) {
            super(reason.text(), cause);
            this.reason = reason;
        }

        /** The register code of the refusal of the rendering. */
        public DroolsCode code() { return reason.code(); }

        public Reason reason() { return reason; }
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
            throw new DrlRefusal(drlRefused.reason(), drlRefused);
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
     * What stands in {@code session} after a fire that reached its fixpoint, read without writing anything on a bean:
     * the derived values and characteristics, the objects and links the creations made, the evaluation faults that
     * stand, as data, and the §9.5 deposits. {@code label} names the objects of the case in the sentence of a fault, as
     * {@link #setDerivedValues(KieSession, Function)} does; null names an object by type and identity.
     *
     * <p>Two different values on one attribute of one object have no common fixpoint (RUNTIME-SEMANTICS ch. 5.7). The
     * rule of {@link #differenceRule} makes that a {@link Fault} of the fixpoint where two writers can disagree; where no
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
     * What every effective defaults stack decided at the fixpoint of {@code session}: one record per object of the
     * session and effective stack of its concrete type, the objects being the beans of the input and the objects the
     * creations made ({@link Fixpoint#objects}), read off the control facts the stack left standing
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
     * <p>It refuses the sessions {@link #readOf} refuses, with the same sentences. An evaluation fault that stands means
     * the session has no answer ({@link Fixpoint#faults}); this gives what the control facts say all the same, and a
     * consumer reads the faults first. The list is in no particular order.
     */
    public List<DefaultsDecision> decisionsOf(KieSession session) {
        requireFixpoint(session);
        // a stack runs on instances of exactly its owner (ch. 7.10), so the stacks of an object are those of its class
        Map<Class<?>, List<String>> stacksOf = new HashMap<>();
        layouts.forEach((name, s) -> stacksOf.computeIfAbsent(s.javaClass(), k -> new ArrayList<>()).add(name));
        Map<String, Map<Object, List<ControlFact>>> perStack = new LinkedHashMap<>();
        for (Object f : session.getObjects(o -> o instanceof ControlFact)) {
            ControlFact c = (ControlFact) f;
            if (!layouts.containsKey(c.stack()))
                throw new IllegalStateException(c + " in de sessie, maar dit model verlaagde geen stapel " + c.stack());
            perStack.computeIfAbsent(c.stack(), k -> new IdentityHashMap<>())
                    .computeIfAbsent(c.subject(), k -> new ArrayList<>()).add(c);
        }
        // the value an attribute stack in the simple form wrote, by stack and object: the decision of its default
        Map<String, Map<Object, List<Value>>> written = new HashMap<>();
        for (Object f : session.getObjects(o -> o instanceof Value w && layouts.containsKey(w.writer()))) {
            Value w = (Value) f;
            written.computeIfAbsent(w.writer(), k -> new IdentityHashMap<>())
                    .computeIfAbsent(w.subject(), k -> new ArrayList<>()).add(w);
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
                        : settledInSimpleForm(name, s, subject, facts, written.getOrDefault(name, Map.of()));
                List<List<String>> chains = rootChains.computeIfAbsent(name, n -> s.roots().stream()
                        .map(chain -> chain.stream().map(k -> pointerOf(k.d())).toList()).toList());
                out.add(new DefaultsDecision(subject, s.member().name(), s.member().characteristic(), chains,
                        settled == null ? null : settled.decider(), settled == null ? null : settled.conflict()));
            }
        perStack.forEach((name, rest) -> {
            if (!rest.isEmpty())
                throw new IllegalStateException("controlefeiten van " + name + " op "
                        + rest.keySet().stream().map(FactNames::object).toList() + ", en dat is in de sessie geen"
                        + " object van het type waarop de stapel loopt: " + rest.values());
        });
        return out;
    }

    /** The lowered form of one effective stack: its target, its bare conflict key (the target on the type that declares
     *  it), whether it took the general form, the knots of its chains at the root, and the bean class of its owner, the
     *  type it runs on ({@link StackBuild#javaClass}). */
    private record StackLayout(Member member, String targetForm, boolean general, List<List<Node>> roots,
                               Class<?> javaClass) {}

    /** How a chain or a reconciliation came out: a default that decided, or a conflict. */
    private record Settling(DefaultsDecision.Decider decider, DefaultsDecision.Conflict conflict) {}

    /** The one {@link Decides} of the simple form, on the default it names. */
    private Settling settledInSimpleForm(String name, StackLayout s, Object subject, List<ControlFact> facts,
                                         Map<Object, List<Value>> written) {
        if (facts.size() != 1 || !(facts.get(0) instanceof Decides d))
            throw new IllegalStateException("de eenvoudige vorm van " + name + " laat per object één Beslist staan, en"
                    + " op " + FactNames.object(subject) + " staan " + facts);
        Node k = knot(s.roots().get(0), d.index(), name);
        Object decision;
        if (s.member().characteristic()) decision = k.d().decision();
        else {
            List<Value> values = written.getOrDefault(subject, List.of());
            if (values.size() != 1)
                throw new IllegalStateException(d + " in de sessie, en " + name + " schreef op "
                        + FactNames.object(subject) + " " + values + " in plaats van één waarde");
            decision = values.get(0).value();
        }
        return new Settling(new DefaultsDecision.Decider(pointerOf(k.d()), decision), null);
    }

    /** The {@link Outcome}s of the general form, reconciled at the root as the root reconciliation reads them. */
    private Settling settledInGeneralForm(String name, StackLayout s, Object subject, List<ControlFact> facts) {
        Map<Integer, Outcome> perChain = new HashMap<>();
        for (ControlFact f : facts) {
            if (!(f instanceof Outcome o))
                throw new IllegalStateException("de algemene vorm van " + name + " zet alleen Uitkomst in, en op "
                        + FactNames.object(subject) + " staat " + f);
            Outcome other = perChain.put(o.chain(), o);
            if (other != null)
                throw new IllegalStateException("keten " + o.chain() + " van " + name + " laat per object één uitkomst"
                        + " staan, die van haar hoogste default, en op " + FactNames.object(subject) + " staan " + other
                        + " en " + o);
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
            throw new IllegalStateException(o + " ligt niet op keten " + k.chain() + " van " + name);
        Settling settled = k.hasMembers()
                ? reconciled(name, k.members(), k.firstMember(), perChain, pointerOf(k.d()), k.key()) : null;
        if (settled == null) {
            // no member settled, or none to settle: the default's own decision, its fall-back where it has members
            if (o.conflict())
                throw new IllegalStateException(o + " in de sessie, maar onder default " + k.label() + " van " + name
                        + " staat geen conflict");
            return new Settling(new DefaultsDecision.Decider(pointerOf(k.d()), o.value()), null);
        }
        boolean agrees = o.conflict()
                ? settled.conflict() != null && settled.conflict().key().equals(o.key())
                : settled.decider() != null && Outcome.equal(settled.decider().decision(), o.value());
        if (!agrees)
            throw new IllegalStateException(o + " in de sessie, en de leden van default " + k.label() + " van " + name
                    + " geven " + settled);
        return settled;
    }

    /** The knot at {@code position} of {@code chain}. */
    private static Node knot(List<Node> chain, int position, String name) {
        if (position < 0 || position >= chain.size() || chain.get(position).position() != position)
            throw new IllegalStateException("een controlefeit van " + name + " noemt plaats " + position + ", en de keten"
                    + " heeft er " + chain.size());
        return chain.get(position);
    }

    /** The JSON pointer of default {@code d} into the document ({@link DefaultsDecision}): the {@code Default} node, or
     *  for a default the fold of ch. 7.10 made of an ordinary rule the version of that rule. */
    private String pointerOf(Default d) {
        if (pointers == null) {
            pointers = new IdentityHashMap<>();
            for (int i = 0; i < defaults.size(); i++)
                pointersOf(defaults.get(i).defaults(), "/rules/prioritizedDefaults/" + i);
        }
        String p = pointers.get(d);
        if (p != null) return p;
        ResolvedRule r = stacks.ruleOf(d);
        if (r == null) throw new IllegalStateException("een default van een stapel die het document niet declareert en"
                + " die geen regel van het document is: " + d);
        return rulePointer(r);
    }

    private void pointersOf(List<Default> chain, String holder) {
        for (int j = 0; j < chain.size(); j++) {
            Default d = chain.get(j);
            String p = holder + "/defaults/" + j;
            pointers.put(d, p);
            if (d.exceptions() != null)
                for (int m = 0; m < d.exceptions().members().size(); m++)
                    pointersOf(d.exceptions().members().get(m).defaults(), p + "/exceptions/members/" + m);
        }
    }

    /** The version of rule {@code r} as a pointer, {@code /rules/rules/<form>/versions/<version>}. The model holds one
     *  rule per version, in the order of the document, and a form with two or more versions names them "&lt;naam&gt;
     *  #1" … "#n" in a row (the loader's naming, IR-SPEC ch. 3, Versioning), so a version continues a form where it
     *  follows the version before it by name; a rule without a name says nothing of its form, and one before {@code r}
     *  leaves the place of {@code r} open, which is refused. */
    private String rulePointer(ResolvedRule r) {
        int form = -1, version = 0;
        String previous = null;
        for (ResolvedRule x : model.resolvedRules) {
            if (x.name() == null)
                throw new IllegalStateException("de regel " + r.name() + " is de basis van een stapel, en een regel"
                        + " zonder naam ervoor laat open bij welke vorm van het document hij hoort");
            if (previous != null && nextVersion(previous, x.name())) version++;
            else {
                form++;
                version = 0;
            }
            previous = x.name();
            if (x == r) return "/rules/rules/" + form + "/versions/" + version;
        }
        throw new IllegalStateException("de regel " + r.name() + " is de basis van een stapel en staat niet in het model");
    }

    private static final java.util.regex.Pattern VERSION = java.util.regex.Pattern.compile("([\\s\\S]*) #([0-9]+)");

    /** Whether {@code name} names the version after the one {@code previous} names, of one form. */
    private static boolean nextVersion(String previous, String name) {
        java.util.regex.Matcher a = VERSION.matcher(previous), b = VERSION.matcher(name);
        return a.matches() && b.matches() && a.group(1).equals(b.group(1))
                && Integer.parseInt(b.group(2)) == Integer.parseInt(a.group(2)) + 1;
    }

    /**
     * Writes what stands at the fixpoint of {@code session} onto the beans, exactly what {@link #readOf} gives, read by the
     * same code: {@code setX(true)} for each derived characteristic, the value of each derived attribute through the
     * setter of the declared attribute, a timeline as the bean model's segments, and each link a creation laid on the
     * reference field of every to-one end the bean model gives a field, the field the input of such a link is read from
     * ({@link #linkRules}). Input on a characteristic stays, since nothing writes {@code false}; input on an
     * attribute stays where no rule derived a value for it.
     *
     * <p>An evaluation fault that stands at the fixpoint ({@link Fault}) means the session has no answer, so before it
     * writes anything this throws the faults that stand: one as the exception it is, named after its rule, two or more
     * as {@link EvaluationFaults}, ordered by the name of the form and then by the sentence, so that the report does not
     * depend on the order of the rules or of the firings. Then it writes nothing, and the session stays as it was. It
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
    private String fireAway(KieSession session, Watch watch, String agenda) {
        if (!creationRules.isEmpty())
            return "dit model heeft creatieregels, dus DroolsBackend.fire weigert een tweede fire: open een verse sessie";
        if (!session.getObjects(new ClassObjectFilter(WrittenBack.class)).isEmpty())
            return "deze sessie is teruggezet, dus DroolsBackend.fire weigert haar: open een verse sessie met beans die"
                    + " alleen invoer dragen";
        if (watch.onOrOff() != null)
            return "invoer die buiten DroolsBackend.insert om bijkwam of wegging, vraagt een verse sessie, dus"
                    + " DroolsBackend.fire weigert haar: open een verse sessie met alle beans in één Input";
        if (watch.moreThanOneUpdate() != null || agenda != null)
            return "DroolsBackend.fire weigert een sessie waarin buiten fire om meer gebeurde dan een update van een"
                    + " attribuut of kenmerk van een bean: open een verse sessie met alle beans in één Input";
        if (watch.changedGlobal(session) != null)
            return "DroolsBackend.fire weigert een sessie waarvan een global na de eerste fire veranderde: open een verse"
                    + " sessie";
        return "vuur haar eerst opnieuw met DroolsBackend.fire";
    }

    /** The session test of {@link #readOf}, {@link #setDerivedValues} and {@link #decisionsOf}: a session whose last run
     *  through the driver reached its fixpoint and in which nothing happened since outside the driver; any other is
     *  refused with the sentence {@link #readOf} describes. */
    private void requireFixpoint(KieSession session) {
        // per class, as the driver asks for it: a filter of another kind walks every fact the session holds
        List<Object> marks = new ArrayList<>(session.getObjects(new ClassObjectFilter(Fired.class)));
        if (marks.isEmpty())
            throw new IllegalStateException("geen fixpunt om te lezen of terug te zetten: deze sessie is niet via"
                    + " DroolsBackend.fire gevuurd, dat tot het fixpunt vuurt"
                    + (creationRules.isEmpty() ? "" : " en de creaties van dit model in generaties laat vuren"));
        if (marks.size() > 1)
            throw new IllegalStateException("de sessie houdt " + marks.size() + " merken van DroolsBackend.fire, waar"
                    + " de driver er na elke run één laat: " + marks);
        String noFixpoint = ((Fired) marks.get(0)).noFixpoint();
        if (noFixpoint != null)
            throw new IllegalStateException("geen fixpunt om te lezen of terug te zetten: de laatste DroolsBackend.fire"
                    + " van deze sessie bereikte er geen — " + noFixpoint);
        Watch watch = Watch.of(session);
        // the driver puts the watch back after every run, so only a consumer that took it off leaves a mark without it,
        // and the driver refuses such a session
        if (watch == null)
            throw new IllegalStateException("geen fixpunt om te lezen of terug te zetten: de bewaking die"
                    + " DroolsBackend.fire op deze sessie zet, staat er niet meer op, dus wat er na de laatste fire"
                    + " gebeurde, is niet te zien; DroolsBackend.fire weigert haar ook: open een verse sessie met alle"
                    + " beans in één Input");
        String agenda = Watch.agenda(session);
        String outsideClause = watch.outsideClause();
        String description = outsideClause == null ? agenda : agenda == null ? outsideClause : outsideClause + " en " + agenda;
        if (description != null)
            throw new IllegalStateException("geen fixpunt om te lezen of terug te zetten: sinds de laatste"
                    + " DroolsBackend.fire van deze sessie gebeurde er iets buiten fire om, wat het fixpunt van die fire"
                    + " kan veranderen (" + description + "); " + fireAway(session, watch, agenda));
        String global = watch.changedGlobal(session);
        if (global != null)
            throw new IllegalStateException("geen fixpunt om te lezen of terug te zetten: " + global + ", en het fixpunt"
                    + " hoort bij de oude waarde; DroolsBackend.fire weigert de sessie, dus open een verse sessie");
        String relation = watch.changedRelation();
        if (relation != null)
            throw new IllegalStateException("geen fixpunt om te lezen of terug te zetten: " + relation + ", en het fixpunt"
                    + " hoort bij de oude relatie; DroolsBackend.fire weigert de sessie, dus open een verse sessie met"
                    + " alle beans in één Input");
    }

    private Read readFixpoint(KieSession session, Function<Object, String> label) {
        requireFixpoint(session);
        List<Runnable> acts = new ArrayList<>();
        // a set: the fault of two writers that the rule of verschilregel stated and the one read off the values below
        // are one fault, equal as facts are
        Set<Fault> faults = new LinkedHashSet<>();
        for (Object f : session.getObjects(o -> o instanceof Fault)) {
            Fault fault = (Fault) f;
            faults.add(label != null && fault.fault() instanceof WritersConflict c
                    ? new Fault(fault.subject(), fault.rule(), c.named(label))
                    : label != null && fault.fault() instanceof InputFormFault i
                    ? new Fault(fault.subject(), fault.rule(), i.named(label)) : fault);
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
                if (written.stream().anyMatch(w -> !equalValue(earliest.value(), w.value()))) {
                    WritersConflict c = conflict(earliest.subject(), earliest.attribute(), written);
                    faults.add(new Fault(earliest.subject(), c.writers().get(0).writer(),
                            label == null ? c : c.named(label)));
                    continue;
                }
                Member member = memberOf(earliest, earliest.attribute());
                // a timeline goes onto the bean as the bean model's segments, which carry no value equality
                @SuppressWarnings("unchecked")
                Object v = member.time() == null ? earliest.value()
                        : Period.beans((List<Period>) earliest.value(), segmentClass());
                values.add(new Fixpoint.DerivedValue(earliest.subject(), member.name(), v));
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
        // a link of the input carries phase 0 and is in the bean fields already
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
                throw new IllegalStateException(g + " in de sessie, maar haar onderwerp is in dezelfde of een latere fase"
                        + " gemaakt");
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
     *  to-one role, holds the filler of that role (§13.3.9), as {@link #linkRules} reads the input of the relation
     *  back from it. A to-many end has no field of its own; where both ends are to-many no bean holds the link, and it
     *  is in {@link Fixpoint#links} only. */
    private Fixpoint.MadeLink madeLink(RelationLink k, List<Runnable> acts) {
        RegelSpraakModel.FactType ft = factType(k.relation());
        if (ft == null || ft.roles.size() != 2)
            throw new IllegalStateException(k + " in de sessie, maar het model kent geen feittype van twee rollen '"
                    + k.relation() + "'");
        RegelSpraakModel.Role r0 = ft.roles.get(0), r1 = ft.roles.get(1);
        ObjectType t0 = model.objectTypeByName(r0.objectTypeName()), t1 = model.objectTypeByName(r1.objectTypeName());
        if (!r1.many() && FactModelGenerator.hasReferenceField(model, t0, r1.name(), t1)) {
            BiConsumer<Object, Object> setter = Accessors.eigenschapSetter(beanClass(t0), Namen.property(r1.name()));
            acts.add(() -> setter.accept(k.first(), k.second()));
        }
        if (!r0.many() && FactModelGenerator.hasReferenceField(model, t1, r0.name(), t0)) {
            BiConsumer<Object, Object> setter = Accessors.eigenschapSetter(beanClass(t1), Namen.property(r0.name()));
            acts.add(() -> setter.accept(k.second(), k.first()));
        }
        return new Fixpoint.MadeLink(ft.name, r0.name(), k.first(), r1.name(), k.second());
    }

    /** The bean model's {@code Segment}, the class of a timeline's segments on a bean. */
    private Class<?> segmentClass() {
        try {
            return Class.forName(FactModelGenerator.PACKAGE + ".Segment", true, beanLoader);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("geen beanklasse Segment voor een tijdlijn", e);
        }
    }

    private Member memberOf(Object fact, ElementKey key) {
        Member member = derived.get(key);
        if (member == null)
            throw new IllegalStateException(fact + " in de sessie, maar geen regel van dit model leidt "
                    + FactNames.member(key) + " af");
        return member;
    }

    /** The fault of RUNTIME-SEMANTICS ch. 5.7 for the {@code waarden} that writers put on attribute {@code key} of
     *  {@code subjectType}: the writers as {@link WritersConflict#inDocumentOrder} ranks them over the document's
     *  rules {@link #documentOrder}, the attribute as declared, and the object's own type. */
    private WritersConflict conflict(Object subjectType, ElementKey key, List<Value> values) {
        return WritersConflict.of(subjectType, memberOf(subjectType, key).name(), values, documentOrder, typeClasses,
                types);
    }

    /** Value equality at the write boundary: numbers by value, so that 5.0 and 5.00 are one value; leeg is null. */
    private static boolean equalValue(Object a, Object b) {
        return ComputeHelpers.equalValue(a, b);
    }

    // ---------------- the model ----------------

    private Modelplan run() {
        writers = ProducerConsumer.producerIndex(model, defaults);
        standingTargets = StandingTargets.of(model, defaults);
        for (ResolvedRule r : model.resolvedRules)
            if (r.name() != null && !documentOrder.contains(r.name())) documentOrder.add(r.name());
        for (ObjectType t : model.objectTypes.values()) {
            String javaClass = Namen.ident(t.name, true);
            int i = typeClasses.indexOf(javaClass);
            if (i < 0) {
                typeClasses.add(javaClass);
                types.add(t.name);
            } else {
                types.set(i, t.name);
            }
        }
        if (!model.objectCreations.isEmpty() || !model.factCreations.isEmpty())
            strata = Cycles.strata(model, defaults, navigationBindings, anchored, roleBindings, sourcePaths);
        // before any form lowers: a form that reads a created relation must know it while it lowers
        for (ResolvedObjectCreation o : model.objectCreations) {
            RoleBindingRef vr = roleBindings.get(o);
            if (vr != null && vr.factType() != null) createdRelations.add(Namen.norm(vr.factType()));
            if (vr == null) {
                RegelSpraakModel.FactType ft = typePairRelation(o);
                if (ft != null) createdRelations.add(Namen.norm(ft.name));
            }
        }
        for (ResolvedFactCreation f : model.factCreations)
            if (f.factType() != null) createdRelations.add(Namen.norm(f.factType()));
        // computing rules and initialisations are counted apart: one of each on a target is a pair the branch carries
        // (the initialisation falls through below the rule), two of either kind is not
        for (ResolvedRule r : model.resolvedRules) {
            if (!(r.target() instanceof Expr.AttributeReference d) || d.objectType() == null || d.attribute() == null) continue;
            Map<ElementKey, List<ResolvedRule>> per =
                    r.kind() == ResolvedRule.Kind.GELIJKSTELLING || r.kind() == ResolvedRule.Kind.BEREKENING
                            ? computeRules
                            : r.kind() == ResolvedRule.Kind.INITIALISATIE ? initialisations : null;
            if (per != null)
                per.computeIfAbsent(ProducerConsumer.elementOf(d.objectType(), d.attribute(), false),
                        k -> new ArrayList<>()).add(r);
        }
        for (PrioritizedDefaults p : defaults)
            if (p.target() != null)
                defaultsTargets.add(ProducerConsumer.elementOf(model.objectTypeByName(p.subject()), p.target(),
                        p.isCharacteristic()));
        // composed before the rules, since a rule the fold takes into a stack must not fire on the stack's type
        stacks = new Stacks(model, defaults);
        List<Stacks.Stack> effective;
        Stacks.Uncomposable composition = null;
        try {
            effective = stacks.stacks();
        } catch (Stacks.Uncomposable o) {
            effective = List.of();
            composition = o;
        }
        List<RulePlan> rules = new ArrayList<>();
        for (ResolvedRule r : model.resolvedRules) rules.addAll(meet("regel", nameOf(r.name()), () -> rule(r)));
        for (ResolvedConsistency c : model.consistencies)
            rules.addAll(meet("consistentie", nameOf(c.name()), () -> consistencyRules(c)));
        for (ResolvedUniqueness u : model.uniquenesses)
            rules.addAll(meet("uniciteit", nameOf(u.name()), () -> uniquenessRules(u)));
        for (ResolvedAllocation v : model.allocations)
            rules.addAll(meet("verdeling", nameOf(v.name()), () -> allocation(v)));
        for (ResolvedObjectCreation o : model.objectCreations)
            rules.addAll(meet("objectcreatie", nameOf(o.name()), () -> objectCreation(o)));
        for (ResolvedFactCreation f : model.factCreations)
            rules.addAll(meet("feitcreatie", nameOf(f.name()), () -> factCreation(f)));
        for (ResolvedUniquenessUnion u : model.uniquenessUnions)
            rules.addAll(meet("uniciteit-unie", nameOf(u.name()), () -> uniquenessUnionRules(u)));
        if (composition != null) refuse(composition.reason(), "defaults", "effectieve stapels");
        for (Stacks.Stack st : effective) {
            String name = stackName(st);
            generated.put(name, new RegelSpraakModel.GeneratedFrom(stackTarget(st),
                    RegelSpraakModel.OriginForm.DEFAULTS_STACK));
            rules.addAll(meet("defaults", name, () -> stackRules(st)));
            // a refused stack laid no knots: it has its type, and no composition
            StackLayout laid = layouts.get(name);
            effectiveStacks.put(name, new Stack(RegelSpraakModel.Designatable.objectType(st.owner().name),
                    laid == null ? null : composition(laid.roots())));
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
        // the reading rules of a build with readings belong to no form, so the observer and the coverage pass them by
        for (ReadingRule r : readingRules) {
            rules.add(r.plan());
            readingRuleOf.put(r.plan().name(), r.ownRule());
        }
        // a standing is built once per member, for every rule that reads it, and only for one a lowered rule reads: a
        // standing nothing reads would only add firings
        List<RulePlan> states = new ArrayList<>();
        for (PathRead p : pathReads.values())
            states.addAll(p.path().created() ? linkPathStateRules(p) : pathStateRules(p));
        for (Member member : read.values()) states.addAll(member.characteristic() ? characteristicStateRules(member) : attributeStateRules(member));
        for (String producer : channels) states.addAll(channelStateRules(producer));
        for (String parameter : globals.keySet()) states.add(parameterStateRule(parameter));
        List<Modelplan.Global> declarations = new ArrayList<>();
        if (evaluationDateGlobal) {
            // the case runner sets the evaluation date under this name after the parameters, so a parameter of the same
            // Java name would read the date, or the date the parameter
            if (globals.containsKey(REKENDATUM))
                refuse(DroolsCode.PARAMETER_EVALUATION_DATE_NAME.reason(), "parameter", REKENDATUM);
            else {
                declarations.add(new Modelplan.Global(REKENDATUM, LocalDate.class));
                states.add(evaluationDateStateRule());
            }
        }
        globals.forEach((name, javaClass) -> declarations.add(new Modelplan.Global(name, javaClass)));
        for (RulePlan r : rules) if (!formOfRule.containsKey(r.name())) rulesWithoutForm.add(r.name());
        for (RulePlan r : states) rulesWithoutForm.add(r.name());
        // every rule of a form, a reading rule among them, has the role its lowering noted, and a rule of a fold its
        // fold, so that originOf answers for every rule of every document, not only for those a witness asks
        for (RulePlan r : rules) {
            if (rulesWithoutForm.contains(r.name()) && !readingRuleOf.containsKey(r.name())) continue;
            // a name two forms build has no one role or fold; the model holds both rules, which the backend refuses
            if (sharedNames.containsKey(r.name())) continue;
            RuleOrigin.Role role = roleOf(r.name());
            if (role.inFold() != foldOfRule.containsKey(r.name()))
                throw new IllegalStateException("de regel '" + r.name() + "' heeft de rol " + role + ", en de verlaging"
                        + (role.inFold() ? " noteerde zijn vouw niet" : " noteerde een vouw bij hem"));
        }
        List<RulePlan> plans = new ArrayList<>(rules);
        plans.addAll(states);
        return new Modelplan(declarations, plans);
    }

    /** A form's name as the manifest shows it; a nameless form (WF-107 refuses it) still gets an entry. */
    private static String nameOf(String name) {
        return name != null ? name : "(zonder naam)";
    }

    /** Lowers one form and records its verdict: lowered, or a skip with the reason a {@link Refusal} carries. */
    private List<RulePlan> meet(String category, String name, Supplier<List<RulePlan>> build) {
        form = name;
        String own = RegelSpraakModel.formName(name);
        if (!own.equals(name))
            generated.put(name, new RegelSpraakModel.GeneratedFrom(RegelSpraakModel.Designatable.form(own),
                    RegelSpraakModel.OriginForm.VERSION));
        int readingRulesBefore = readingRules.size();
        try {
            List<RulePlan> rules = build.get();
            coverage.add(Coverage.lowered(category, name));
            for (RulePlan r : rules) formOfRule.put(r.name(), name);
            return rules;
        } catch (Refusal w) {
            // a refusal part-way through the form leaves its reading open, and the next form would open its own on it
            abandonReading(readingRulesBefore);
            refuse(w.reason(), category, name);
            return List.of();
        }
    }

    private void refuse(Reason reason, String category, String name) {
        Coverage d = Coverage.skip(reason, category, name);
        coverage.add(d);
        d.report(model);
    }

    /** A form, or a part of one, this branch does not lower; the message is the manifest's reason. */
    private static final class Refusal extends RuntimeException {
        private final Reason reason;
        Refusal(Reason reason) {
            super(reason.text());
            this.reason = reason;
        }
        DroolsCode code() { return reason.code(); }
        Reason reason() { return reason; }
    }

    // ---------------- a rule ----------------

    private List<RulePlan> rule(ResolvedRule r) {
        // the rule of a form is named by the form, and a KieBase keeps one rule per name
        if (r.name() == null) throw new Refusal(DroolsCode.RULE_WITHOUT_NAME.reason());
        if (!(r.target() instanceof Expr.AttributeReference target) || target.objectType() == null || target.attribute() == null)
            throw new Refusal(DroolsCode.RULE_TARGET_UNRESOLVED.reason());
        if (navigationBindings.containsKey(target)) throw new Refusal(DroolsCode.RULE_NAVIGATED_TARGET.reason());
        // a version with a bounded validity gates on the evaluation date: its rules bind the date where the window
        // admits it (Reads.valid), and beside them a rule faults the session that carries no date
        List<RulePlan> out = new ArrayList<>(switch (r.kind()) {
            case KENMERKTOEKENNING -> characteristicRule(r, target);
            case GELIJKSTELLING, BEREKENING -> computeRule(r, target);
            case INITIALISATIE -> initialisationRule(r, target);
        });
        RulePlan withoutDate = validityFault(model.emittedRuleName(r.name()), r.name(), r.validity());
        if (withoutDate != null) out.add(withoutDate);
        return out;
    }

    /**
     * The rule beside a form with a bounded validity, {@code form}, whose Drools rule is {@code ruleName}, that faults
     * a session without an evaluation date: such a form needs the date (RUNTIME-SEMANTICS ch. 8.1), and the reference
     * interpreter faults the session as soon as it evaluates the form, whether or not the form has a subject, in the
     * sentence {@link ComputeFault#VALIDITY_WITHOUT_DATE}. The fault is on the session's {@link EvaluationDate}, the one fact
     * the date is, and like every evaluation fault it counts where it stands at the fixpoint. Null for a form that is
     * always valid, which needs no date.
     */
    private RulePlan validityFault(String ruleName, String form, Validity g) {
        if (g == null || g.always()) return null;
        evaluationDateNeeded();
        Alpha withoutDate = new Alpha(id("geen-rekendatum"),
                List.of(new Alpha.FieldTest("date", Alpha.Comparator.LEEG, null)));
        return as(RuleOrigin.Role.NO_EVALUATION_DATE, new RulePlan(ruleName + ": geen rekendatum", this.form, null,
                new ReadTable(List.of()),
                List.of(new FactPattern(EvaluationDate.class, "$rekendatum", List.of(withoutDate), List.of(), List.of())),
                new Consequence.Fault("$rekendatum", ComputeFault.VALIDITY_WITHOUT_DATE, new Consequence.FaultName(form, ruleName),
                        Consequence.Insertion.LOGICAL)));
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
    private List<RulePlan> characteristicRule(ResolvedRule r, Expr.AttributeReference target) {
        // the loader refuses any other value (WF-022); asked again because this rule writes true whatever it says
        if (!(r.value() instanceof Expr.Literal l && "waar".equals(l.valueText()) && l.type() instanceof Datatype.BooleanType))
            throw new Refusal(DroolsCode.CHARACTERISTIC_ASSIGNMENT_NOT_TRUE.reason());
        Member time = declared(target.objectType(), target.attribute(), true);
        if (time.time() != null)
            throw new Refusal(DroolsCode.TIME_CHARACTERISTIC_TARGET.reason("attribute", time.name(), "declarant", time.declarant().name));
        Member member = member(target.objectType(), target.attribute(), true);
        Supplier<Reads> fresh = () -> new Reads(target.objectType()).valid(r.validity());
        Reads reads = fresh.get();
        Set<Integer> read = reads.follow();
        beginReading();
        Term testTerm = r.condition() == null ? null : conditionTerm(r.condition(), reads);
        Noted noted = endReading();
        boolean canFail = reads.canFail();
        reads.stop();
        List<LhsItem> items = reads.plan(conditionRole(canFail), testTerm, read, r.condition());
        ElementKey key = member.key();
        derived.put(key, member);
        reads.recorder();
        String ruleName = model.emittedRuleName(r.name());
        List<RulePlan> out = new ArrayList<>();
        out.add(as(RuleOrigin.Role.OWN, reads.rulePlan(ruleName, items,
                new Consequence.Characteristic(SUBJECT, key, Consequence.Insertion.LOGICAL))));
        if (noted != null && testTerm != null)
            readingRule(ruleName, positions.versionOf(r), reads, reads.readingPlan(List.of(), false), testTerm, null,
                    r.condition(), null, noted, r.validity(), false);
        if (canFail) out.add(conditionFaultRule(r.name(), ruleName, fresh, r.condition(), null));
        RulePlan deposit = depositRule(r.name(), ruleName, fresh, r.condition(), null);
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
     * type). An evaluation fault, a division by a numeric zero or a {@link ComputeFault}, is inserted logically by the
     * consequence as a {@link Fault} in place of the value, named after the rule. A division of a filled dividend by a
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
    private List<RulePlan> computeRule(ResolvedRule r, Expr.AttributeReference target) {
        if (declared(target.objectType(), target.attribute(), false).time() != null) return timelineRule(r, target);
        Member member = member(target.objectType(), target.attribute(), false);
        Set<Class<?>> absorbed = absorbed(target, member);
        boolean writesEmpty = !standingTargets.standingScalar(r);
        return writeRule(r, target.objectType(), member, null, absorbed, writesEmpty);
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
    private List<RulePlan> timelineRule(ResolvedRule r, Expr.AttributeReference target) {
        Member member = timeMember(target.objectType(), target.attribute(), false, Msg.drools("PART.desc.time-dependent-target"));
        List<String> others = otherWriters(r, target.objectType(), member.key());
        if (!others.isEmpty())
            throw new Refusal(DroolsCode.TIME_TARGET_SECOND_WRITER.reason("attribute", member.name(), "declarant", member.declarant().name, "writers", String.join(", ", others.stream().map(x -> "\"" + x + "\"").toList())));
        if (Condition.containsTimeWindow(r.condition()))
            throw new Refusal(DroolsCode.PERIOD_WINDOW_TIME_TARGET.reason("attribute", member.name(), "declarant", member.declarant().name));
        if (!(r.value() instanceof Expr.FunctionCall f && (f.kind() == Expr.FunctionKind.TIJDSEVENREDIG_DEEL
                || f.kind() == Expr.FunctionKind.AANTAL_DAGEN_IN)))
            throw new Refusal(DroolsCode.TIME_TARGET_UNSUPPORTED_VALUE.reason("attribute", member.name(), "declarant", member.declarant().name, "snippet", r.value() == null ? DroolsCode.TIME_TARGET_UNSUPPORTED_VALUE.part("without-value")
                    : snippet(r.value().source())));
        return writeRule(r, target.objectType(), member, null, Set.of(), !standingTargets.standingTemporal(target));
    }

    /**
     * The timeline that the pro-rata part or the day count {@code f} produces for the target {@code target} of
     * {@code subject} (RUNTIME-SEMANTICS ch. 3.7, 3.8), as a function of the values the rule binds: the true periods of
     * the subject's own characteristic in its gedurende clause and, for the pro-rata part, the timeline of the base, an
     * own attribute, each bound in the left-hand side ({@link Reads#timeline}), so a base that a rule derives later
     * takes the result along. The runtime's helpers compute them, with {@link Period} as their
     * segment class: {@code rsTijdsevenredig} per bucket of the target's grid, the base's
     * value at the bucket's start times the share of true days, a base that is leeg there or absent giving no segment,
     * and {@code rsAantalDagenIn} the true days per touched bucket, on the calendar or from the anchor of the target's
     * fixed start point ({@code rsAantalDagenInVanaf}, ch. 3.2). Neither coalesces adjacent equal buckets (ch. 3.7). The
     * pro-rata part reads its base whole, bucket by bucket, so a base the case gives as the asOfDate-bucket scalar
     * faults ({@link InputFormFault}, ch. 8.4), wherever the rule's evaluation reaches the value; the reference
     * interpreter and the ASP backend read such a base as no timeline today and write nothing.
     *
     * <p>Refused by name, as the reference interpreter refuses them: a stated bucket other than the target's
     * granularity (WF-056, WF-058); a clause that is not a check of an own characteristic on a timeline, or is negated
     * or navigated; a base that is not an own attribute on a timeline, is finer than the bucket or carries a unit, whose
     * rate the reference interpreter does not restate (ch. 3.7, fenced); and a pro-rata part onto or from a fixed start
     * point ({@link Timelines#proRataFixedGrid}).
     */
    private Term timelineValue(Expr.FunctionCall f, Member target, ObjectType subject, Reads reads) {
        boolean part = f.kind() == Expr.FunctionKind.TIJDSEVENREDIG_DEEL;
        Msg description = Msg.drools(part ? "PART.desc.pro-rata-part" : "PART.desc.day-count", "snippet", snippet(f.source()));
        if (f.granularity() != target.time())
            throw new Refusal(DroolsCode.TIMELINE_VALUE_GRANULARITY_MISMATCH.reason("description", description, "sourceGranularity", f.granularity(), "attribute", target.name(), "targetGranularity", target.time(), "ruleReference", part ? "WF-056" : "WF-058"));
        if (target.time() != RegelSpraakModel.Granularity.MAAND && target.time() != RegelSpraakModel.Granularity.JAAR)
            throw new Refusal(DroolsCode.TIMELINE_VALUE_NOT_BUCKET.reason("description", description, "targetGranularity", target.time()));
        if (f.emptyFallback() != null || f.dimensionSelection() != null)
            throw new Refusal(DroolsCode.TIMELINE_VALUE_FALLBACK_SELECTION.reason("description", description));
        int condition = clipTimeline(f.filter(), subject, reads, description);
        String gran = target.time().name();
        if (!part) {
            if (!f.arguments().isEmpty())
                throw new Refusal(DroolsCode.DAY_COUNT_WITH_ARGUMENTS.reason("description", description, "argumentCount", f.arguments().size()));
            // RUNTIME-SEMANTICS ch. 3.2: a target on a fixed start point buckets from the constant anchor its walls are
            // computed from
            LocalDate anchor = model.fixedStartPointAnchor(target.declarant(), target.name());
            Term periods = new Term.SlotValue(condition, source(f.filter().source()));
            // the reading of the clause is its true periods, none where the characteristic has no timeline
            noteDerived(f.filter(), List.of(periods), x -> periodsOf(x.get(0)));
            Term days = anchor == null
                    ? new Term.Call(StaticFunction.RS_DAY_COUNT, List.of(periods, constant(gran),
                            constant(Period.class)), null)
                    : new Term.Call(StaticFunction.RS_DAY_COUNT_FROM, List.of(periods, constant(gran),
                            constant(anchor), constant(Period.class)), null);
            return new Term.Call(StaticFunction.PERIOD_LIST, List.of(days), source(f.source()));
        }
        if (f.arguments().size() != 1 || !(f.arguments().get(0) instanceof Expr.AttributeReference b)
                || b.attribute() == null || b.objectType() != subject || navigationBindings.containsKey(b))
            throw new Refusal(DroolsCode.TIME_BASE_NOT_OWN.reason("description", description));
        Member base = timeMember(subject, b.attribute(), false, description);
        if (!base.datatype().isNumber())
            throw new Refusal(DroolsCode.TIME_BASE_NOT_NUMBER.reason("description", description, "attribute", base.name()));
        if (base.datatype().unitOf() != null)
            throw new Refusal(DroolsCode.RATE_RECALCULATION_WITH_UNIT.reason("description", description, "attribute", base.name(), "unit", base.datatype().unitOf().rendering()));
        if (base.time().ordinal() < target.time().ordinal())
            throw new Refusal(DroolsCode.TIME_BASE_FINER_BUCKET.reason("description", description, "attribute", base.name(), "baseGranularity", base.time(), "targetGranularity", target.time()));
        String fixed = Timelines.proRataFixedGrid(subject, target.name(), f);
        if (fixed != null) throw new Refusal(DroolsCode.PRO_RATA_FIXED_GRID.reason("description", description, "reason", fixed));
        int baseValue = reads.timeline(base);
        int subjectType = reads.member();
        reads.markFail();
        String baseName = base.name();
        Term periods = new Term.SlotValue(condition, source(f.filter().source()));
        // the reading of the clause is its true periods, none where the characteristic has no timeline
        noteDerived(f.filter(), List.of(periods), x -> periodsOf(x.get(0)));
        Term whole = complete(baseValue, "the pro-rata base \"" + baseName + "\" needs the full timeline; ", subjectType,
                " supplies only the asOfDate-bucket scalar (ch. 3.7, ch. 8.4)", source(b.source()));
        note(b, whole);
        Term proportional = new Term.Call(StaticFunction.RS_TIME_PROPORTIONAL, List.of(periods, whole,
                constant(gran), constant(Period.class)), null);
        return new Term.Call(StaticFunction.PERIOD_LIST, List.of(proportional), source(f.source()));
    }

    /**
     * The true periods of the gedurende clause {@code c} of a pro-rata part or a day count on {@code subject}, bound as a
     * timeline ({@link Reads#timeline}): a check of the subject's own characteristic on a timeline, as the reference
     * interpreter reads that clause (RUNTIME-SEMANTICS ch. 3.7, 3.8). Refused by name: no clause, another condition, a
     * negated or navigated check, and an owner or a whole-period qualifier on the check (WF-097).
     */
    private int clipTimeline(Condition c, ObjectType subject, Reads reads, Object description) {
        if (!(c instanceof Condition.CharacteristicCheck k))
            throw new Refusal(DroolsCode.CLIP_CONDITION_NOT_CHARACTERISTIC.reason("description", description, "condition", c == null ? DroolsCode.CLIP_CONDITION_NOT_CHARACTERISTIC.part("no-condition")
                    : DroolsCode.CLIP_CONDITION_NOT_CHARACTERISTIC.part("condition", "snippet", snippet(c))));
        if (k.owner() != null || k.period() != null)
            throw new Refusal(DroolsCode.CLIP_OWNED_WHOLE_PERIOD.reason("description", description, "snippet", snippet(k)));
        if (k.negation() || k.navigation() != null || navigationBindings.containsKey(k))
            throw new Refusal(DroolsCode.CLIP_NEGATED_OR_NAVIGATED.reason("description", description, "characteristic", snippet(k)));
        return reads.timeline(timeMember(subject, k.characteristic(), true, description));
    }

    /**
     * The forms other than the initialisation {@code r} on {@code type} that write the attribute {@code key} on an
     * instance {@code r} writes too, by the names the producer index gives them, in its order: a rule, an allocation or
     * a defaults group on {@code type}, an ancestor or a descendant of it ({@link #related}), read per concrete type as
     * WF-079 reads the single-writer rule; forms on sister subtypes never write one instance. An object creation is no
     * writer here: the member values it sets are the input of the object it makes ({@link #objectCreation}), which the
     * initialisation finds filled there.
     */
    private List<String> otherWriters(ResolvedRule r, ObjectType type, ElementKey key) {
        Set<String> out = new LinkedHashSet<>();
        for (ResolvedRule x : model.resolvedRules)
            if (x != r && x.kind() != ResolvedRule.Kind.KENMERKTOEKENNING && x.target() instanceof Expr.AttributeReference d
                    && d.objectType() != null && d.attribute() != null && related(d.objectType(), type)
                    && ProducerConsumer.elementOf(d.objectType(), d.attribute(), false).equals(key))
                out.add(ProducerConsumer.baseName(x.name()));
        for (ResolvedAllocation v : model.allocations)
            if (v.targetType() != null && v.targetAttribute() != null && related(v.targetType(), type)
                    && ProducerConsumer.elementOf(v.targetType(), v.targetAttribute(), false).equals(key))
                out.add(ProducerConsumer.baseName(v.name()));
        for (int i = 0; i < defaults.size(); i++) {
            PrioritizedDefaults p = defaults.get(i);
            ObjectType t = model.objectTypeByName(p.subject());
            if (t != null && p.target() != null && !p.isCharacteristic() && related(t, type)
                    && ProducerConsumer.elementOf(t, p.target(), false).equals(key))
                out.add(p.name() != null ? ProducerConsumer.baseName(p.name()) : "/rules/prioritizedDefaults/" + i);
        }
        out.remove(ProducerConsumer.baseName(r.name()));
        return List.copyOf(out);
    }

    /** Whether one of the two types is the other or an ancestor of it: a form on the one then writes instances of the
     *  other, where two forms on sister types never meet on one instance (IR-SPEC ch. 2.2, WF-079). */
    private static boolean related(ObjectType a, ObjectType b) {
        return a.chain().contains(b) || b.chain().contains(a);
    }

    /**
     * The bean classes of the concrete types on which the fold of RUNTIME-SEMANTICS ch. 7.10 took the rule writing
     * {@code target} into an effective stack, as its base or as its initialisation default: there the stack writes the
     * target and the rule must not also fire, so its subject pattern leaves them out. Empty for a target no defaults
     * group writes. A rule beside a group its own type declares on the same target is no fold but two writers of one
     * target (WF-050), which this branch refuses by name: the fold leaves such a rule alone, and both would write.
     */
    private Set<Class<?>> absorbed(Expr.AttributeReference target, Member member) {
        if (!defaultsTargets.contains(member.key())) return Set.of();
        if (stacks.ownGroup(target.objectType(), target.attribute()))
            throw new Refusal(DroolsCode.RULE_BESIDE_DEFAULTS_GROUP.reason("attribute", member.name(), "objectType", target.objectType().name));
        Set<Class<?>> out = new LinkedHashSet<>();
        for (ObjectType t : stacks.absorbedOn(target.objectType(), target.attribute())) out.add(beanClass(t));
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
     * a {@link Fault}, which goes when the other writer displaces the initialisation, and answers the other writer's
     * value, where the reference interpreter faults or answers the value depending on the order of the rules. Whether
     * such a fault counts waits on a decision of the norm, so a division in the condition or the value of an
     * initialisation beside another writer stays refused, with the predicate of a division in a default
     * ({@code TmsInitialisationWitnessTest}). Without another writer nothing displaces it and the division stays carried.
     */
    private List<RulePlan> initialisationRule(ResolvedRule r, Expr.AttributeReference target) {
        Member time = declared(target.objectType(), target.attribute(), false);
        if (time.time() != null)
            throw new Refusal(DroolsCode.INITIALISATION_OF_TIME_ATTRIBUTE.reason("attribute", time.name(), "declarant", time.declarant().name));
        Member member = member(target.objectType(), target.attribute(), false);
        ElementKey key = member.key();
        Set<Class<?>> absorbed = absorbed(target, member);
        List<ResolvedRule> writing = initialisations.get(key).stream()
                .filter(x -> related(((Expr.AttributeReference) x.target()).objectType(), target.objectType())).toList();
        if (writing.size() > 1)
            throw new Refusal(DroolsCode.MULTIPLE_INITIALISATIONS_ON_TARGET.reason("attribute", member.name(), "declarant", member.declarant().name, "rules", String.join(", ", writing.stream().map(x -> "\"" + x.name() + "\"").toList())));
        // a computing rule outside the standing domain writes a computed leeg, which empties the target for the guard
        boolean emptyBeside = computeRules.getOrDefault(key, List.of()).stream().anyMatch(x ->
                !standingTargets.standingScalar(x) && related(((Expr.AttributeReference) x.target()).objectType(), target.objectType()));
        List<String> others = otherWriters(r, target.objectType(), key);
        String division = division(r.condition(), r.value());
        if (division != null && !others.isEmpty())
            throw new Refusal(DroolsCode.INITIALISATION_DIVISION_SHARED_TARGET.reason("division", division, "attribute", member.name(), "declarant", member.declarant().name, "writers", String.join(", ", others.stream().map(x -> "\"" + x + "\"").toList())));
        String name = r.name();
        Alpha.FieldTest ofTarget = new Alpha.FieldTest("attribute", Alpha.Comparator.GELIJK, key);
        Alpha.FieldTest ofOther = new Alpha.FieldTest("writer", Alpha.Comparator.NOT_EQUAL, name);
        if (!emptyBeside)
            return writeRule(r, target.objectType(), member, reads -> {
                reads.addSubjectAlpha(new Alpha(id("doel-leeg-in-de-invoer"),
                        List.of(new Alpha.FieldTest(member.name(), Alpha.Comparator.LEEG, null))));
                Alpha another = new Alpha(id("andere-schrijver"), List.of(ofTarget, ofOther));
                return List.of(new Not(new FactPattern(Value.class, "$andere_schrijver", List.of(another),
                        List.of(new Join(id("andere-schrijver-onderwerp"), "subject", SUBJECT)), List.of())));
            }, absorbed, false);
        return writeRule(r, target.objectType(), member, reads -> {
            Alpha another = new Alpha(id("andere-schrijver"),
                    List.of(ofTarget, ofOther, new Alpha.FieldTest("value", Alpha.Comparator.GEVULD, null)));
            Join otherOnSubject = new Join(id("andere-schrijver-onderwerp"), "subject", SUBJECT);
            Alpha empty = new Alpha(id("lege-schrijver"),
                    List.of(ofTarget, ofOther, new Alpha.FieldTest("value", Alpha.Comparator.LEEG, null)));
            Join emptyOnSubject = new Join(id("lege-schrijver-onderwerp"), "subject", SUBJECT);
            int emptyWriterCount = reads.emptyWriters();
            int subjectType = reads.member();
            // the target's input on the subject's bean, as it is, which the guard tests for leeg as the first variant's
            // constraint does; read in the test, so the executable model binds no value of it
            Term inputData = new Term.Input(new Term.SlotValue(subjectType, null), new Binding.Field(member.javaClass(), member.name()),
                    Binding.ReadForm.VALUE, null);
            Term targetEmpty = new Term.Call(StaticFunction.TARGET_EMPTY, List.of(new Term.SlotValue(emptyWriterCount, null), inputData), null);
            return List.of(
                    new Not(new FactPattern(Value.class, "$andere_schrijver", List.of(another), List.of(otherOnSubject),
                            List.of())),
                    new Accumulation(new FactPattern(Value.class, "$lege_schrijver", List.of(empty), List.of(emptyOnSubject),
                            List.of()), List.of(new Accumulation.Aggregation(Accumulation.Kind.COUNT, null, "$lege_schrijvers"))),
                    new TestItem(id("doel-leeg"), TestItem.Role.VORM, targetEmpty, -1, List.of(emptyWriterCount, subjectType),
                            reads.size()));
        }, absorbed, false);
    }

    /**
     * The rule of a form that writes an attribute of its subject: the subject pattern with the reads its condition and
     * its value bind there, the standing patterns of what it reads elsewhere, one test over every value the condition
     * reads, the {@code blockade} the form adds (null for none), and a consequence that computes the value from the
     * values it binds and inserts a {@link Value} logically, with the form's name as the writer, when the value is
     * gevuld, or also when it is leeg where {@code writesEmpty} (a computing rule outside the standing domain, ch.
     * 1.2g), beside the §6.5 deposit {@link #computeRule} describes. Where the value faults, the consequence inserts the
     * {@link Fault} instead, and neither the value nor a deposit. Objects of a class in {@code absorbed} do not
     * match: an effective stack writes the target there ({@link #absorbed}). Beside it, where the condition can
     * fault, the rule that inserts its fault ({@link #conditionFaultRule}).
     */
    private List<RulePlan> writeRule(ResolvedRule r, ObjectType subject, Member member,
                                         Function<Reads, List<LhsItem>> blockade, Set<Class<?>> absorbed,
                                         boolean writesEmpty) {
        ElementKey key = member.key();
        Supplier<Reads> fresh = () -> readings(subject, absorbed).valid(r.validity());
        Reads reads = fresh.get();
        Set<Integer> read = reads.follow();
        beginReading();
        Term testTerm = r.condition() == null ? null : conditionTerm(r.condition(), reads);
        boolean canFail = reads.canFail();
        reads.stop();
        Set<Integer> use = reads.follow();
        // the write boundary: the value restated into the target's declared unit (RUNTIME-SEMANTICS ch. 2.4, 2.6); a
        // timeline is written as its producer computes it, as in the reference interpreter (ch. 3.7)
        Term value = member.time() != null
                ? timelineValue((Expr.FunctionCall) r.value(), member, subject, reads)
                : restated(value(r.value(), reads), eff.unitOf(r.value()), member.datatype().unitOf(),
                        "een §4 schrijfgrens naar het doel");
        if (member.time() != null) note(r.value(), value);
        Noted noted = endReading();
        reads.stop();
        int depositFlag = reads.depositFlagSlot();
        List<LhsItem> items = reads.plan(conditionRole(canFail), testTerm, read, r.condition());
        List<LhsItem> blockadeItems = blockade == null ? List.of() : blockade.apply(reads);
        items.addAll(blockadeItems);
        String name = r.name();
        String ruleName = model.emittedRuleName(name);
        Consequence consequence = new Consequence.Value(SUBJECT, key, name, value, List.copyOf(use), reads.size(), true,
                writesEmpty, depositFlag < 0 ? null : new Consequence.Deposit(depositFlag, name), new Consequence.FaultName(name, ruleName),
                Consequence.Insertion.LOGICAL);
        derived.put(key, member);
        reads.recorder();
        List<RulePlan> out = new ArrayList<>();
        out.add(as(RuleOrigin.Role.OWN, reads.rulePlan(ruleName, items, consequence)));
        // every form this writes computes a value, so it has a reading also without a condition (RUNTIME-SEMANTICS
        // ch. 5.12): the reading of its value, where it is at its turn
        if (noted != null) {
            // an initialisation's guard stays: where another writer's value or the input fills the target, the form is
            // not at its turn (RUNTIME-SEMANTICS ch. 5.4)
            List<LhsItem> readingItems = reads.readingPlan(List.of(), false);
            readingItems.addAll(blockadeItems);
            readingRule(ruleName, positions.versionOf(r), reads, readingItems, testTerm, value, r.condition(), r.value(),
                    noted, r.validity(), blockade != null);
        }
        if (canFail) out.add(conditionFaultRule(name, ruleName, fresh, r.condition(), blockade));
        RulePlan deposit = depositRule(name, ruleName, fresh, r.condition(), blockade);
        if (deposit != null) out.add(deposit);
        return out;
    }

    /** The reads of a rule on {@code subject}, whose subject pattern leaves out the objects of a class in
     *  {@code absorbed}: an effective stack writes the target there ({@link #absorbed}). The plan carries the
     *  classes in the order of the document's types, as {@code absorbed} lists them: an immutable copy by
     *  {@code Set.copyOf} iterates in an order that differs per run, and a rendering into text must be the same in
     *  every run. */
    private Reads readings(ObjectType subject, Set<Class<?>> absorbed) {
        Reads reads = new Reads(subject);
        if (!absorbed.isEmpty())
            reads.addSubjectAlpha(new Alpha(id("niet-in-een-stapel"), List.of(new Alpha.FieldTest(null,
                    Alpha.Comparator.CLASS_NOT_IN, Collections.unmodifiableSet(new LinkedHashSet<>(absorbed))))));
        return reads;
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
        for (Map.Entry<ElementKey, List<ResolvedRule>> e : computeRules.entrySet()) {
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
     * The fault of RUNTIME-SEMANTICS ch. 5.7 as a logical fact: per object of the declaring type of {@code member}, an
     * accumulate over the {@link Value} facts on that object's attribute, and where they hold different values a
     * {@link Fault} with the {@link WritersConflict} that names the writers in the order of the document, under the
     * first of them. The fault rests on the values that stand, so a difference that exists on an intermediate state only
     * goes again when a writer's condition or value changes, and the session reports the differences that stand at its
     * fixpoint, as it reports every evaluation fault ({@link #setDerivedValues}).
     */
    private RulePlan differenceRule(Member member) {
        ElementKey key = member.key();
        Alpha ofTarget = equal(id("waarde"), "attribute", key);
        Join onSubject = new Join(id("waarde-onderwerp"), "subject", SUBJECT);
        TestItem difference = new TestItem(id("verschil"), TestItem.Role.VORM,
                new Term.Call(StaticFunction.DIFFER, List.of(new Term.SlotValue(0, null)), null), -1, List.of(0), 1);
        return new RulePlan("schrijvers " + member.declarant().name + "." + member.name() + ": verschil", null, null,
                new ReadTable(List.of(new dev.uchar.regelspraak.transpiler.tms.plan.Read(0, ReadKind.VALUES,
                        "waarden", "$waarden"))),
                List.of(new Subject(member.javaClass(), SUBJECT, List.of(), List.of(), List.of()),
                        new Accumulation(new FactPattern(Value.class, "$waarde", List.of(ofTarget), List.of(onSubject),
                                List.of()), List.of(new Accumulation.Aggregation(Accumulation.Kind.LIST, null, "$waarden"))),
                        difference),
                new Consequence.WritersConflict(SUBJECT, 0, key, member.name(), documentOrder, typeClasses, types,
                        Consequence.Insertion.LOGICAL));
    }

    // ---------------- an evaluation fault in a condition ----------------

    /**
     * The rule that inserts the fault of a condition that can fault, beside the rule {@code ruleName} of the form
     * {@code form} that tests the condition. A condition is tested in the left-hand side, and an exception there leaves
     * the fire, where truth maintenance cannot withdraw it when the reads it rested on change; the form's own rule
     * therefore tests the condition with the role {@link TestItem.Role#WITHOUT_FAULT}, which does not hold where it
     * faults, and this rule, whose left-hand side is that rule's own built again over fresh declarations ({@code fresh}
     * sets up the subject pattern and what precedes it), with the same {@code blockade} after the test and the test of
     * the role {@link TestItem.Role#FAULT}, that the condition faults, inserts the named {@link Fault} logically, on the
     * subject of the form. It fires exactly where the form's rule tests the condition and the test faults, and the
     * fault goes when a read it rested on changes. The sentence
     * names the form's rule. A condition faults also where it
     * reaches a fold whose filter faulted for a member ({@link #fold}); the filter itself inserts no fault
     * ({@link #filterFaultRule}).
     */
    private RulePlan conditionFaultRule(String form, String ruleName, Supplier<Reads> fresh, Condition c,
                                          Function<Reads, List<LhsItem>> blockade) {
        Reads reads = fresh.get();
        Set<Integer> read = reads.follow();
        Term testTerm = conditionTerm(c, reads);
        reads.stop();
        List<LhsItem> items = reads.plan(TestItem.Role.FAULT, testTerm, read, c);
        if (blockade != null) items.addAll(blockade.apply(reads));
        Consequence fault = new Consequence.TestFault(SUBJECT, testTerm, TestItem.Role.FAULT, List.copyOf(read), reads.size(),
                new Consequence.FaultName(form, model.emittedRuleName(form)), Consequence.Insertion.LOGICAL);
        reads.recorder();
        return as(RuleOrigin.Role.CONDITION_FAULT, reads.rulePlan(ruleName + ": fout in de voorwaarde", items, fault));
    }

    /** The role of the test of a condition in the rule of its form: where the condition can fault it is tested without
     *  its fault, which the fault rule takes ({@link #conditionFaultRule}). */
    private static TestItem.Role conditionRole(boolean canFail) {
        return canFail ? TestItem.Role.WITHOUT_FAULT : TestItem.Role.CONDITION;
    }

    /**
     * The rule that deposits the §9.5 inconsistency of RUNTIME-SEMANTICS ch. 1.2 for the condition {@code c} of
     * {@code form}, whose rule is {@code ruleName}: the reference interpreter deposits wherever its evaluation of a
     * condition divides a filled dividend by a leeg divisor or takes the root of a negative number, whether the condition
     * then holds or not (ch. 5.6), and only at the places that evaluation reaches (ch. 5.10). A condition is tested in the
     * left-hand side, where Drools inserts nothing, so a rule of the form's own, with the same left-hand side
     * ({@code blockade} included) and the test that evaluating the condition reaches such a place ({@link DepositFlag}),
     * inserts the deposit logically under the form and the subject; it goes when a read it rested on changes. Null where
     * {@code c} holds no division and no root.
     */
    private RulePlan depositRule(String form, String ruleName, Supplier<Reads> fresh, Condition c,
                                    Function<Reads, List<LhsItem>> blockade) {
        if (!deposits(c)) return null;
        refuseMixedDeposits("'" + form + "'", List.of(c));
        Reads reads = fresh.get();
        Set<Integer> read = reads.follow();
        Term testTerm = conditionTerm(c, reads);
        reads.stop();
        int depositFlag = reads.depositFlagSlot();
        if (depositFlag < 0) throw new IllegalStateException("de voorwaarde van \"" + form + "\" deelt, maar kreeg geen sonde");
        List<LhsItem> items = reads.plan(TestItem.Role.DEPOSIT, testTerm, read, c);
        if (blockade != null) items.addAll(blockade.apply(reads));
        reads.recorder();
        return as(RuleOrigin.Role.CONDITION_DEPOSIT, reads.rulePlan(ruleName + ": deponering in de voorwaarde", items,
                new Consequence.ConditionDeposit(SUBJECT, form, testTerm, depositFlag, List.copyOf(read), reads.size(),
                        Consequence.Insertion.LOGICAL)));
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
    private List<RulePlan> consistencyRules(ResolvedConsistency c) {
        // the rule is named by the form, and the channel test reads the error facts by that name
        if (c.name() == null) throw new Refusal(DroolsCode.CONSISTENCY_WITHOUT_NAME.reason());
        if (c.subject() == null || c.assertion() == null) throw new Refusal(DroolsCode.CONSISTENCY_MISSING_SUBJECT_ASSERTION.reason());
        Supplier<Reads> fresh = () -> new Reads(c.subject()).valid(c.validity());
        Reads reads = fresh.get();
        Set<Integer> read = reads.follow();
        beginReading();
        Term holds = conditionTerm(c.assertion(), reads);
        Noted noted = endReading();
        boolean canFail = reads.canFail();
        reads.stop();
        Term violated = new Term.Call(StaticFunction.NIET, List.of(holds), source(c.assertion().source()));
        List<LhsItem> items = reads.plan(conditionRole(canFail), violated, read, c.assertion());
        String name = c.name();
        reads.recorder();
        List<RulePlan> out = new ArrayList<>();
        // EF-025: the consistency does not hold (RUNTIME-SEMANTICS ch. 5.6, 5.7b)
        out.add(as(RuleOrigin.Role.OWN, reads.rulePlan(model.emittedRuleName(name), items,
                new Consequence.Inconsistency(SUBJECT, name, List.of("EF-025"), Consequence.Insertion.LOGICAL))));
        // the reading is of the assertion, which the rule's test negates
        if (noted != null)
            readingRule(model.emittedRuleName(name), positions.versionOf(c), reads, reads.readingPlan(List.of(), false),
                    holds, null, c.assertion(), null, noted, c.validity(), false);
        if (canFail) out.add(conditionFaultRule(name, model.emittedRuleName(name), fresh, c.assertion(), null));
        RulePlan deposit = depositRule(name, model.emittedRuleName(name), fresh, c.assertion(), null);
        if (deposit != null) out.add(deposit);
        RulePlan withoutDate = validityFault(model.emittedRuleName(name), name, c.validity());
        if (withoutDate != null) out.add(withoutDate);
        return out;
    }

    /** A uniqueness over one source (RUNTIME-SEMANTICS ch. 5.9), unscoped or scoped by a parent ({@link #unionUniquenessRules}). */
    private List<RulePlan> uniquenessRules(ResolvedUniqueness u) {
        if (u.subject() == null || u.keys().isEmpty()) throw new Refusal(DroolsCode.UNIQUENESS_MISSING_SOURCE_KEY.reason());
        return unionUniquenessRules(u.name(), List.of(new ResolvedUniquenessUnion.Source(u.subject(), u.keys())),
                u.scopeBinding(), u.validity());
    }

    /** A uniqueness over two or more sources, the union of ch. 5.9, rule 1: one key domain, compared position by
     *  position, so every source carries as many keys (WF-030); a union has no scope (rule 3). */
    private List<RulePlan> uniquenessUnionRules(ResolvedUniquenessUnion u) {
        if (u.sources().size() < 2)
            throw new Refusal(DroolsCode.UNION_UNIQUENESS_SINGLE_SOURCE.reason("sourceCount", u.sources().size()));
        int arity = u.sources().get(0).keys().size();
        for (ResolvedUniquenessUnion.Source b : u.sources())
            if (b.type() == null || b.keys().size() != arity)
                throw new Refusal(DroolsCode.UNION_UNIQUENESS_UNEQUAL_ARITY.reason());
        return unionUniquenessRules(u.name(), u.sources(), null, u.validity());
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
    private List<RulePlan> unionUniquenessRules(String name, List<ResolvedUniquenessUnion.Source> sources,
                                             RoleBindingRef scopeRole, Validity validity) {
        // the rules are named by the form, and the channel test reads the error facts by that name
        if (name == null) throw new Refusal(DroolsCode.UNIQUENESS_WITHOUT_NAME.reason());
        String ruleName = model.emittedRuleName(name);
        List<RulePlan> out = new ArrayList<>();
        for (int b = 0; b < sources.size(); b++) {
            out.add(keyRule(name, ruleName, b, sources.get(b), scopeRole, validity));
            RulePlan form = keyFormRule(name, ruleName, b, sources.get(b), validity);
            if (form != null) out.add(form);
        }
        FactPattern key = new FactPattern(UniquenessKey.class, "$sleutel", List.of(equal(id("uniciteit"), "uniqueness",
                name)), List.of(), List.of(Binding.of("$element", UniquenessKey.class, "element")));
        Alpha ofUniqueness = equal(id("uniciteit"), "uniqueness", name);
        BetaTest collision = new BetaTest(id("botsing"), StaticFunction.COLLIDES_WITH, "$sleutel");
        Accumulation collisions = new Accumulation(new FactPattern(UniquenessKey.class, "$ander", List.of(ofUniqueness),
                List.of(), List.of(collision), List.of()),
                List.of(new Accumulation.Aggregation(Accumulation.Kind.COUNT, null, "$botsingen")));
        ReadTable table = new ReadTable(List.of(new dev.uchar.regelspraak.transpiler.tms.plan.Read(0,
                ReadKind.COLLISIONS, "botsingen", "$botsingen")));
        TestItem duplicate = new TestItem(id("dubbel"), TestItem.Role.VORM,
                new Term.Call(StaticFunction.DUPLICATE, List.of(new Term.SlotValue(0, null)), null), -1, List.of(0), 1);
        out.add(as(RuleOrigin.Role.OWN, new RulePlan(ruleName, form, null, table, List.of(key, collisions, duplicate),
                new Consequence.UniquenessViolation("$element", name, Consequence.Insertion.LOGICAL))));
        FactPattern violation = new FactPattern(UniquenessViolation.class, "$schending", List.of(equal(id("schending"),
                "uniqueness", name)), List.of(), List.of(Binding.of("$subject", UniquenessViolation.class, "element")));
        out.add(as(RuleOrigin.Role.UNIQUENESS_CHANNEL, new RulePlan(ruleName + ": kanaal", form, null,
                new ReadTable(List.of()),
                // EF-026: a key of the uniqueness occurs more than once (RUNTIME-SEMANTICS ch. 5.6, 5.9, 5.7b)
                List.of(violation), new Consequence.Inconsistency("$subject", name, List.of("EF-026"),
                        Consequence.Insertion.LOGICAL))));
        RulePlan withoutDate = validityFault(ruleName, name, validity);
        if (withoutDate != null) out.add(withoutDate);
        return out;
    }

    /** The key rule of source {@code b} of the uniqueness {@code name}: over the instances of the source's type, gated
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
    private RulePlan keyRule(String name, String ruleName, int b, ResolvedUniquenessUnion.Source source,
                                   RoleBindingRef scopeRole, Validity validity) {
        ObjectType type = source.type();
        Reads reads = new Reads(type).valid(validity);
        Set<Integer> read = reads.follow();
        List<Integer> keySlots = new ArrayList<>();
        List<Boolean> enumeration = new ArrayList<>();
        int ever = -1;
        for (String key : source.keys()) {
            if (declared(type, key, false).time() != null) {
                Msg description = Msg.drools("PART.desc.unique-key", "key", key, "type", type.name);
                if (ever >= 0)
                    throw new Refusal(DroolsCode.TIME_KEY_BESIDE_SECOND.reason("description", description));
                Member member = timeMember(type, key, false, description);
                if (!member.datatype().isNumber())
                    throw new Refusal(DroolsCode.TIME_KEY_NOT_NUMBER.reason("description", description));
                ever = keySlots.size();
                keySlots.add(reads.timeline(member));
                enumeration.add(false);
                continue;
            }
            Member member = member(type, key, false);
            keySlots.add(reads.own(member));
            enumeration.add(member.datatype() instanceof Datatype.Enumeration);
        }
        int scope = scopeRole == null ? -1 : reads.scopeRead(scopeRole);
        reads.stop();
        int timeline = ever;
        // the key takes part where every component off the timeline is gevuld and the parent is set
        List<Term> participation = new ArrayList<>();
        for (int i = 0; i < keySlots.size(); i++) if (i != timeline) participation.add(filled(keySlots.get(i)));
        // a key the case gives as the asOfDate-bucket scalar is the fault of the key-form rule beside this one
        if (timeline >= 0) participation.add(bucketScalar(keySlots.get(timeline), false));
        if (scope >= 0) participation.add(filled(scope));
        List<LhsItem> items = reads.plan(TestItem.Role.VORM, all(participation), read, null);
        List<Term> components = new ArrayList<>();
        for (int i = 0; i < keySlots.size(); i++) {
            Term component = new Term.SlotValue(keySlots.get(i), null);
            components.add(enumeration.get(i) ? new Term.Call(StaticFunction.RS_ENUM_NAME, List.of(component), null)
                    : component);
        }
        reads.recorder();
        return as(RuleOrigin.Role.UNIQUENESS_KEY, reads.rulePlan(ruleName + ": sleutel van " + type.name + " "
                + (b + 1), items, new Consequence.UniquenessKey(SUBJECT, name, b, components, timeline, scope,
                        List.copyOf(read), reads.size(), Consequence.Insertion.LOGICAL)));
    }

    /**
     * The rule beside the key rule of source {@code b} of the uniqueness {@code name} where its key has a component on a
     * timeline: over the instances of the source's type, gated by the form's validity, the components up to that one
     * bound as the key rule binds them, the test that every component before it is gevuld and that the case gives the
     * timeline as the asOfDate-bucket scalar ({@link BucketScalar}), and a consequence that inserts the fault of
     * RUNTIME-SEMANTICS ch. 5.9 logically as a {@link Fault} of the instance ({@link InputFormFault}), in the reference
     * interpreter's words: ever-uniqueness reads every value the timeline ever carries, and a scalar would miss values.
     * The key's form is checked on every instance of the type, before the scope narrows the rows ("this form error
     * precedes the scope narrowing"), so the parent and the components after the timeline do not enter; the reference
     * interpreter builds a row's tuples component by component and stops at an empty one, so an empty component before
     * the timeline leaves the timeline unread. Null for a key without a component on a timeline.
     */
    private RulePlan keyFormRule(String name, String ruleName, int b, ResolvedUniquenessUnion.Source source,
                                       Validity validity) {
        ObjectType type = source.type();
        int position = -1;
        for (int i = 0; i < source.keys().size() && position < 0; i++)
            if (declared(type, source.keys().get(i), false).time() != null) position = i;
        if (position < 0) return null;
        Reads reads = new Reads(type).valid(validity);
        Set<Integer> read = reads.follow();
        List<Integer> preceding = new ArrayList<>();
        for (int i = 0; i < position; i++) preceding.add(reads.own(member(type, source.keys().get(i), false)));
        Member timeMember = timeMember(type, source.keys().get(position), false,
                Msg.drools("PART.desc.unique-key", "key", source.keys().get(position), "type", type.name));
        int timeline = reads.timeline(timeMember);
        reads.stop();
        List<Term> bucket = new ArrayList<>();
        for (int i : preceding) bucket.add(filled(i));
        bucket.add(bucketScalar(timeline, true));
        List<LhsItem> items = reads.plan(TestItem.Role.VORM, all(bucket), read, null);
        reads.recorder();
        return as(RuleOrigin.Role.UNIQUENESS_KEY_FORM_FAULT, reads.rulePlan(ruleName + ": sleutelvorm van "
                + type.name + " " + (b + 1), items,
                new Consequence.InputFormFault(SUBJECT, "the ever-unique key \"" + timeMember.name()
                        + "\" needs the full timeline; ", " supplies only a scalar (ch. 5.9)",
                        new Consequence.FaultName(name, ruleName), Consequence.Insertion.LOGICAL)));
    }

    /**
     * A test of the §9.5 channel, "regelversie X is inconsistent" (RUNTIME-SEMANTICS ch. 5.6): an existential over the
     * error facts of the form X, with no subject of its own, read through X's {@link ChannelState}. The producers this
     * branch carries are a consistency, a uniqueness, and a rule whose condition or value can deposit a §6.5 or §6.6
     * inconsistency ({@link #writeRule}, {@link #depositRule}); any other name is refused, since a test of a
     * channel nothing fills would never hold and say nothing. "is gevuurd" reads no channel; it is no construct of the
     * core, and is refused with that reason.
     */
    private Term channelRead(Condition.RuleStatusCheck rv, Reads reads) {
        // decided as a predicate of the dialect over the fixpoint, never a construct of the core: the reference
        // interpreter faults on it ("not implemented") and the ASP backend does not translate it
        if (!rv.inconsistent())
            throw new Refusal(DroolsCode.DIALECT_FIRED_PREDICATE.reason("snippet", snippet(rv)));
        String producer = rv.rule();
        if (!channelProducer(producer))
            throw new Refusal(DroolsCode.INCONSISTENT_PRODUCER_NOT_CARRIED.reason("producer", producer, "snippet", snippet(rv)));
        int i = reads.channel(producer);
        return new Term.Call(StaticFunction.CHANNEL, List.of(new Term.SlotValue(i, null)), source(rv.source()));
    }

    /** Whether the form named {@code name}, spelled as the error facts carry it, fills a channel this branch lowers. */
    private boolean channelProducer(String name) {
        if (name == null) return false;
        for (ResolvedConsistency c : model.consistencies) if (name.equals(c.name())) return true;
        for (ResolvedUniqueness u : model.uniquenesses) if (name.equals(u.name())) return true;
        for (ResolvedUniquenessUnion u : model.uniquenessUnions) if (name.equals(u.name())) return true;
        for (ResolvedRule r : model.resolvedRules)
            if (name.equals(r.name()) && (deposits(r.condition())
                    || r.kind() != ResolvedRule.Kind.KENMERKTOEKENNING && deposits(r.value())))
                return true;
        return false;
    }

    /** Whether a value holds a place the §6.5 or §6.6 deposit of {@link #writeRule} covers ({@link #depositSite}),
     *  where the rule's own evaluation of the value can reach it: through every form {@link #value} evaluates in the
     *  rule itself, and into the filter of a fold over a set source and of each selection of an instance-set union,
     *  which a filter deposit rule evaluates and the reading rule hears where it reaches the fold
     *  ({@link #filterDepositRule}, {@link #readFold}). The argument of such a fold is a read of the member's
     *  attribute or the union, nothing else (WF-037, refused by the loader), so it holds no place of its own. */
    private boolean deposits(Expr e) {
        return deposits(e, TmsTranspiler::depositSite);
    }

    /** {@link #deposits(Expr)} for the places {@code site} admits. */
    private boolean deposits(Expr e, java.util.function.Predicate<Expr> site) {
        return switch (e) {
            case null -> false;
            case Expr.Binary b -> site.test(b) || deposits(b.left(), site) || deposits(b.right(), site);
            case Expr.FunctionCall f -> site.test(f) || f.arguments().stream().anyMatch(a -> deposits(a, site))
                    || deposits(f.emptyFallback(), site) || deposits(f.filter(), site);
            case Expr.InstanceSetUnion u -> u.branches().stream().anyMatch(t -> deposits(t.filter(), site));
            case Expr.Rounding a -> deposits(a.base(), site);
            case Expr.Clamp g -> deposits(g.base(), site) || deposits(g.minimum(), site) || deposits(g.maximum(), site);
            case Expr.Let l -> deposits(l.value(), site) || deposits(l.body(), site);
            case Expr.IfThenElse k -> deposits(k.condition(), site) || deposits(k.then(), site)
                    || deposits(k.otherwise(), site);
            case Expr.TextComposition t -> t.parts().stream()
                    .anyMatch(d -> d instanceof Expr.TextComposition.EmbeddedExpression i && deposits(i.expr(), site));
            default -> false;
        };
    }

    private boolean deposits(Condition c) {
        return deposits(c, TmsTranspiler::depositSite);
    }

    /** {@link #deposits(Condition)} for the places {@code site} admits. */
    private boolean deposits(Condition c, java.util.function.Predicate<Expr> site) {
        return switch (c) {
            case null -> false;
            case Condition.Comparison v -> deposits(v.left(), site) || deposits(v.right(), site);
            case Condition.Predicate p -> deposits(p.operand(), site);
            case Condition.Compound s -> s.components().stream().anyMatch(d -> deposits(d, site));
            case Condition.QuantifierFold kv -> deposits(kv.filter(), site) || deposits(kv.test(), site);
            default -> false;
        };
    }

    /** Whether {@code e} is itself a place that deposits: every division and every square root, whatever its operands,
     *  as the reference interpreter deposits wherever it divides a filled dividend by a leeg divisor or takes the root of
     *  a negative number (RUNTIME-SEMANTICS ch. 1.2, 5.6). */
    private static boolean depositSite(Expr e) {
        return divisionSite(e) || squareRootSite(e);
    }

    /** A division, which deposits over a leeg divisor (§6.5, EF-031) and over a numeric zero (EF-005). */
    private static boolean divisionSite(Expr e) {
        return e instanceof Expr.Binary b && (b.op() == Expr.BinOp.GEDEELD_DOOR || b.op() == Expr.BinOp.GEDEELD_DOOR_ABS);
    }

    /** A square root, whose deposit (§6.6) has no code (RUNTIME-SEMANTICS ch. 5.7b). */
    private static boolean squareRootSite(Expr e) {
        return e instanceof Expr.FunctionCall f && f.kind() == Expr.FunctionKind.WORTEL && f.arguments().size() == 1;
    }

    /**
     * Refuses {@code conditions}, the condition of a form or the filters of a fold, where their deposits mix a division and
     * a square root. Until the card zero-division-channel (2026-10-19) the plan gave a deposit of a condition or a filter
     * its codes, and could not tell those two kinds apart; since then the consequence hears them again with a probe
     * ({@link Consequence.ConditionDeposit}, {@link Consequence.FilterDeposit}), which could, so the refusal stands until
     * its code is retired; no golden has such a condition.
     */
    private void refuseMixedDeposits(String what, List<Condition> conditions) {
        boolean division = false, squareRoot = false;
        for (Condition c : conditions) {
            division |= deposits(c, TmsTranspiler::divisionSite);
            squareRoot |= deposits(c, TmsTranspiler::squareRootSite);
        }
        if (division && squareRoot)
            throw new Refusal(DroolsCode.DEPOSIT_KINDS_MIXED.reason("form", what));
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
        String name = "kanaalstand " + producer + ": ";
        FactPattern fault = new FactPattern(javaClass, "$inconsistentie", List.of(equal(id("kanaal"), "rule", producer)),
                List.of(), List.of());
        RulePlan inconsistent = new RulePlan(name + "inconsistent", null, null, new ReadTable(List.of()),
                List.of(new Exists(List.of(fault), List.of(fault.name()), List.of())),
                new Consequence.InsertChannelState(producer, true, Consequence.Insertion.LOGICAL));
        FactPattern none = new FactPattern(javaClass, "$inconsistentie", List.of(equal(id("kanaal"), "rule", producer)),
                List.of(), List.of());
        RulePlan consistent = new RulePlan(name + "consistent", null, null, new ReadTable(List.of()),
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
    private List<RulePlan> allocation(ResolvedAllocation v) {
        String name = v.name();
        // the rules of a form are named by the form, and the deposits and shares carry that name
        if (name == null) throw new Refusal(DroolsCode.ALLOCATION_WITHOUT_NAME.reason());
        ObjectType source = v.sourceType(), target = v.targetType();
        ResolvedAllocation.Method method = v.method();
        if (source == null || target == null || v.sourceAttribute() == null || v.targetAttribute() == null || method == null)
            throw new Refusal(DroolsCode.ALLOCATION_UNRESOLVED_OPERAND.reason());
        RoleBindingRef rb = roleBindings.get(v);
        if (rb == null) throw new Refusal(DroolsCode.ALLOCATION_WITHOUT_CONNECTING_ROLE.reason());
        // WF-025, which the verifier holds and this backend does not run: the fields each method carries
        boolean equal = method == ResolvedAllocation.Method.GELIJKE_DELEN;
        if (method == ResolvedAllocation.Method.NAAR_RATO && v.ratioAttribute() == null)
            throw new Refusal(DroolsCode.RATIO_ALLOCATION_WITHOUT_WEIGHT.reason());
        if (equal && v.ratioAttribute() != null)
            throw new Refusal(DroolsCode.EQUAL_ALLOCATION_WITH_WEIGHT.reason());
        if (v.maximumAttribute() != null && v.ratioAttribute() == null)
            throw new Refusal(DroolsCode.ALLOCATION_MAXIMUM_WITHOUT_WEIGHT.reason());
        if (method != ResolvedAllocation.Method.OP_VOLGORDE && v.orderingAttribute() != null)
            throw new Refusal(DroolsCode.ORDERING_KEY_WITHOUT_METHOD.reason());
        Member pot = numberMember(source, v.sourceAttribute(), Msg.drools("PART.allocation-role.pot"));
        Member share = numberMember(target, v.targetAttribute(), Msg.drools("PART.allocation-role.share"));
        Member rest = v.remainderAttribute() == null ? null : numberMember(source, v.remainderAttribute(), Msg.drools("PART.allocation-role.remainder"));
        Member weight = v.ratioAttribute() == null ? null : numberMember(target, v.ratioAttribute(), Msg.drools("PART.allocation-role.weight"));
        Member maximum = v.maximumAttribute() == null ? null : numberMember(target, v.maximumAttribute(), Msg.drools("PART.allocation-role.maximum"));
        Member key = v.orderingAttribute() == null ? null : member(target, v.orderingAttribute(), false);
        if (key != null && !key.datatype().isOrderable())
            throw new Refusal(DroolsCode.ORDERING_KEY_NOT_ORDERABLE.reason("attribute", key.name(), "datatype", typeName(key.datatype())));
        allocationWriters(v, target, share);
        if (rest != null) allocationWriters(v, source, rest);

        String ruleName = model.emittedRuleName(name);
        List<RulePlan> out = new ArrayList<>();

        // per source and recipient: the recipient's values, bound in this rule's left-hand side, after the source
        Consumer<Reads> range = recipientsJoin(rb, source, target, name);
        Reads member = new Reads(target);
        member.outside = foldBinders;
        member.addToHeadPlan(new FactPattern(beanClass(source), FOLD_SUBJECT, List.of(), List.of(), List.of()));
        range.accept(member);
        int recipient = member.member();
        int g = weight == null ? -1 : member.own(weight);
        int k = key == null ? -1 : member.own(key);
        int m = maximum == null ? -1 : member.own(maximum);
        List<LhsItem> memberRule = member.plan(TestItem.Role.CONDITION, null, Set.of(), null);
        member.recorder();
        out.add(as(RuleOrigin.Role.ALLOCATION_RECIPIENT, member.rulePlan(ruleName + ": ontvanger", memberRule,
                new Consequence.Recipient(FOLD_SUBJECT, name, recipient, g, k, m, Consequence.Insertion.LOGICAL))));

        // per source: the condition, the pot and the list of recipients, and the distribution computed from them
        Supplier<Reads> fresh = () -> new Reads(source).valid(v.validity());
        Reads reads = fresh.get();
        Set<Integer> read = reads.follow();
        beginReading();
        Term testTerm = v.condition() == null ? null : conditionTerm(v.condition(), reads);
        Noted noted = endReading();
        boolean canFail = reads.canFail();
        reads.stop();
        int p = reads.own(pot);
        int list = reads.recipients(name);
        List<LhsItem> items = reads.plan(conditionRole(canFail), testTerm, read, v.condition());
        ElementKey shareKey = share.key(), restKey = rest == null ? null : rest.key();
        reads.recorder();
        out.add(as(RuleOrigin.Role.OWN, reads.rulePlan(ruleName, items, new Consequence.Payout(SUBJECT, name, method,
                p, list, weight != null, maximum != null, v.descending(), v.roundingDecimals(), shareKey, restKey,
                name, Consequence.Insertion.LOGICAL))));
        if (noted != null && testTerm != null)
            readingRule(ruleName, positions.versionOf(v), reads, reads.readingPlan(List.of(), false), testTerm, null,
                    v.condition(), null, noted, v.validity(), false);
        derived.put(shareKey, share);
        if (rest != null) derived.put(restKey, rest);
        if (canFail) out.add(conditionFaultRule(name, ruleName, fresh, v.condition(), null));
        RulePlan deposit = depositRule(name, ruleName, fresh, v.condition(), null);
        if (deposit != null) out.add(deposit);
        RulePlan withoutDate = validityFault(ruleName, name, v.validity());
        if (withoutDate != null) out.add(withoutDate);
        return out;
    }

    /** Attribute {@code name} of {@code holder} as an allocation reads or writes it as a number: the pot, a share, the
     *  rest, a weight or a cap, {@code role} naming which. The distribution computes with numbers only, so another
     *  datatype is refused by name. */
    private Member numberMember(ObjectType holder, String name, Msg role) {
        Member member = member(holder, name, false);
        if (!member.datatype().isNumberLike())
            throw new Refusal(DroolsCode.ALLOCATION_NON_NUMERIC_MEMBER.reason("role", role, "attribute", member.name(), "declarant", member.declarant().name, "datatype", typeName(member.datatype())));
        return member;
    }

    /**
     * Refuses the allocation {@code v} where another form writes the attribute {@code member} that it writes on
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
    private void allocationWriters(ResolvedAllocation v, ObjectType type, Member member) {
        ElementKey el = member.key();
        String own = ProducerConsumer.baseName(v.name());
        Set<String> allocations = new LinkedHashSet<>(), groups = new LinkedHashSet<>(), rules = new LinkedHashSet<>();
        for (ResolvedAllocation o : model.allocations) {
            if (o.name() != null && ProducerConsumer.baseName(o.name()).equals(own)) continue;
            boolean writes = o.targetType() != null && o.targetAttribute() != null && related(o.targetType(), type)
                    && ProducerConsumer.elementOf(o.targetType(), o.targetAttribute(), false).equals(el)
                    || o.sourceType() != null && o.remainderAttribute() != null && related(o.sourceType(), type)
                    && ProducerConsumer.elementOf(o.sourceType(), o.remainderAttribute(), false).equals(el);
            if (writes) allocations.add("\"" + nameOf(o.name()) + "\"");
        }
        if (!allocations.isEmpty())
            throw new Refusal(DroolsCode.TWO_ALLOCATIONS_WRITE_TARGET.reason("attribute", member.name(), "objectType", type.name, "allocations", String.join(", ", allocations)));
        for (int i = 0; i < defaults.size(); i++) {
            PrioritizedDefaults pd = defaults.get(i);
            ObjectType t = model.objectTypeByName(pd.subject());
            if (t != null && pd.target() != null && !pd.isCharacteristic() && related(t, type)
                    && ProducerConsumer.elementOf(t, pd.target(), false).equals(el))
                groups.add(pd.name() != null ? "\"" + pd.name() + "\"" : "/rules/prioritizedDefaults/" + i);
        }
        if (!groups.isEmpty())
            throw new Refusal(DroolsCode.ALLOCATION_BESIDE_DEFAULTS_GROUP.reason("groups", String.join(", ", groups), "attribute", member.name(), "objectType", type.name));
        for (ResolvedRule r : model.resolvedRules)
            if (r.kind() != ResolvedRule.Kind.KENMERKTOEKENNING
                    && r.target() instanceof Expr.AttributeReference d && d.objectType() != null && d.attribute() != null
                    && related(d.objectType(), type)
                    && ProducerConsumer.elementOf(d.objectType(), d.attribute(), false).equals(el))
                rules.add("\"" + nameOf(r.name()) + "\"");
        if (!rules.isEmpty())
            throw new Refusal(DroolsCode.ALLOCATION_BESIDE_COMPUTING_RULE.reason("rules", String.join(", ", rules), "attribute", member.name(), "objectType", type.name));
    }

    /**
     * How the rule of an allocation joins a recipient, its subject, to the source, its source, over the
     * connecting relation {@code rb}, whose bound role is the recipient's and its sibling the source's (IR-SPEC ch. 3.2),
     * as a fold's one-step range joins a member ({@link #oneStepRange}): over a relation a form creates, through a
     * {@link RelationLink} in an {@code exists}; over an input relation, through the one scalar bean field §13.3.9 stores
     * it in, on the source where the recipient role is to-one and else on the recipient. Refused as there: a binding
     * that names no role pair, a to-one recipient role whose sibling is to-many, and a join the bean model gives no
     * field.
     */
    private Consumer<Reads> recipientsJoin(RoleBindingRef rb, ObjectType source, ObjectType target, String name) {
        RegelSpraakModel.Role[] pair = FactModelGenerator.boundRolePair(model, rb);
        if (pair == null)
            throw new Refusal(DroolsCode.CONNECTING_ROLE_UNDECLARED.reason("factType", rb.factType(), "role", rb.role()));
        if (rb.factType() != null && createdRelations.contains(Namen.norm(rb.factType()))) {
            LinkRelation relation = linkRelation(rb, pair[0], Msg.drools("PART.desc.allocation", "name", name));
            return member -> member.addToRangePlan(linkExists(relation));
        }
        String recipientRole = pair[0].name(), sourceRole = pair[1].name();
        boolean forward = !model.toManyRole(source, target, recipientRole);
        if (forward && model.toManyRole(target, source, sourceRole))
            throw new Refusal(DroolsCode.ALLOCATION_SHARED_BEAN_FIELD.reason("recipientRole", recipientRole, "sourceRole", sourceRole));
        ObjectType carrier = forward ? source : target, another = forward ? target : source;
        String role = forward ? recipientRole : sourceRole;
        if (!FactModelGenerator.hasReferenceField(model, carrier, role, another))
            throw new Refusal(DroolsCode.ROLE_WITHOUT_BEAN_FIELD.reason("reason", FactModelGenerator.noJoinFieldReason(model, carrier, role, another).msg()));
        // forward the source's reference field names the recipient, else the recipient's names the source
        Binding.Field field = new Binding.Field(beanClass(carrier), role);
        return member -> member.subjectJoin(forward ? new Join(id("verdelingsbereik"), null, FOLD_SUBJECT, field)
                : new Join(id("verdelingsbereik"), role, FOLD_SUBJECT));
    }

    // ---------------- object and fact creation ----------------

    /** One member initialisation of an object creation: the member on the created type, and its value on the subject. */
    private record Initialisation(Member member, Term value) {}

    /**
     * An object creation (RUNTIME-SEMANTICS ch. 5.5, IR-SPEC ch. 3.3), a rule of the agenda group {@link #OBJECT_CREATION}:
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
    private List<RulePlan> objectCreation(ResolvedObjectCreation oc) {
        if (oc.name() == null) throw new Refusal(DroolsCode.OBJECT_CREATION_WITHOUT_NAME.reason());
        ObjectType fresh = oc.createdType(), subject = oc.subject();
        if (fresh == null || subject == null) throw new Refusal(DroolsCode.OBJECT_CREATION_UNRESOLVED_TYPE.reason());
        RoleBindingRef vr = roleBindings.get(oc);
        FactModelGenerator.CreationLink k = FactModelGenerator.creationLink(model, fresh, subject, vr, oc.name());
        if (k.ambiguous() != null)
            throw new Refusal(k.ambiguous().framed("still-not"));
        if (k.refusal() != null) throw new Refusal(k.refusal());
        RegelSpraakModel.FactType ft = vr != null ? factType(vr.factType()) : typePairRelation(subject, fresh, k);
        if (vr == null && ft == null)
            throw new Refusal(DroolsCode.CREATION_BACK_LINK_SUPERTYPE.reason("backRole", k.backRole(), "fwdRole", k.fwdRole()));
        if (ft == null || ft.roles.size() != 2 || ft.reciprocal)
            throw new Refusal(DroolsCode.CREATION_RELATION_NOT_BINARY.reason("factType", vr != null ? vr.factType() : ft.name));
        String relation = Namen.norm(ft.name);
        boolean newIsFirst = Namen.norm(ft.roles.get(0).name()).equals(Namen.norm(k.fwdRole()));
        String name = oc.name();
        String ruleName = model.emittedRuleName(name);
        Supplier<Reads> newReads = () -> new Reads(subject).valid(oc.validity());
        Reads reads = newReads.get();
        Set<Integer> read = reads.follow();
        beginReading();
        Term testTerm = oc.condition() == null ? null : conditionTerm(oc.condition(), reads);
        Noted noted = endReading();
        boolean canFail = reads.canFail();
        reads.stop();
        Set<Integer> use = reads.follow();
        List<Initialisation> inits = new ArrayList<>();
        for (ResolvedObjectCreation.MemberInit t : oc.initialisations()) {
            if (t.attribute() == null || t.value() == null)
                throw new Refusal(DroolsCode.MEMBER_INITIALISATION_INCOMPLETE.reason());
            // a characteristic is true or false (WF-027): the reference interpreter faults on a value of another type,
            // and the consequence below would read it as not true and leave the characteristic unset
            Datatype characteristicValue = t.characteristic() ? eff.typeOf(t.value()) : null;
            if (characteristicValue != null && !characteristicValue.isBoolean() && !characteristicValue.isUnknown())
                throw new Refusal(DroolsCode.CHARACTERISTIC_INIT_NON_BOOLEAN.reason("attribute", t.attribute(), "datatype", characteristicValue.name(), "snippet", snippet(t.value().source())));
            String division = division(t.value());
            if (division != null)
                throw new Refusal(DroolsCode.MEMBER_INIT_DIVISION.reason("division", division));
            String squareRoot = squareRoot(t.value());
            if (squareRoot != null)
                throw new Refusal(DroolsCode.MEMBER_INIT_SQUARE_ROOT.reason("squareRoot", squareRoot));
            Member member = member(fresh, t.attribute(), t.characteristic());
            Term w = value(t.value(), reads);
            if (!t.characteristic())
                w = restated(w, eff.unitOf(t.value()), member.datatype().unitOf(), "een §4 schrijfgrens naar het lid");
            inits.add(new Initialisation(member, w));
        }
        reads.stop();
        int stratum = stratum(oc, name);
        int phase = reads.creationPhase(stratum);
        use.add(phase);
        int origin = reads.origin();
        use.add(origin);
        List<LhsItem> items = reads.plan(conditionRole(canFail), testTerm, read, oc.condition(),
                creationGuard(name, reads, phase));
        // the fault this creation stated at a making for the subject blocks it there: the driver counts every firing of a
        // creation rule as a making, so a creation that fired again on its own fault would never let a stratum end
        items.add(new Not(new FactPattern(Fault.class, "$eigen_fout", List.of(equal(id("eigen-fout"), "rule", name)),
                List.of(new Join(id("eigen-fout-onderwerp"), "subject", SUBJECT)), List.of())));
        if (k.oneToOne())
            // an instance linked toward the subject in the input world blocks the creation (ch. 5.5)
            reads.addSubjectAlpha(new Alpha(id("geen-gekoppelde-instantie"),
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
            initialisations.add(new Consequence.Initialisation(member.key(), inits.get(i).value()));
            if (member.characteristic()) insertions.add(new Consequence.Insert.Characteristic(i, Consequence.Insertion.STATED));
            else settings.add(new Consequence.Construction.Setting(i, new Binding.Field(member.javaClass(), member.name())));
        }
        // a creation fires on a fixpoint of the rule layer, so a fault in an initialisation is the session's at the
        // making, as in the reference interpreter, which then makes nothing; the fault is stated like everything a
        // creation makes, and the rule's guard on it keeps the creation from firing again in a later generation
        Consequence creation = new Consequence.Creation(SUBJECT, name,
                new Consequence.CreationDepth(origin, stratum, subject.name, CREATION_BUDGET), initialisations,
                new Consequence.FaultName(name, ruleName), Consequence.Insertion.STATED,
                new Consequence.Construction(beanClass(fresh), settings), phase, insertions, List.copyOf(use),
                reads.size());
        for (Initialisation i : inits) derived.put(i.member().key(), i.member());
        reads.recorder();
        creationRules.add(ruleName);
        creationStrata.add(stratum);
        List<RulePlan> out = new ArrayList<>();
        out.add(as(RuleOrigin.Role.OWN, reads.rulePlan(ruleName, OBJECT_CREATION, items, creation)));
        // the condition on every subject at the end world, in the main group: without the creation's guard, its phase
        // and its own fault, which keep it from making twice; the guard of the 1:1 shape stays, under which a subject
        // linked in the input is not the creation's to make for (RUNTIME-SEMANTICS ch. 5.5)
        if (noted != null && testTerm != null)
            readingRule(ruleName, positions.versionOf(oc), reads, reads.readingPlan(List.of(), true), testTerm, null,
                    oc.condition(), null, noted, oc.validity(), k.oneToOne());
        if (canFail) out.add(conditionFaultRule(name, ruleName, newReads, oc.condition(), null));
        RulePlan deposit = depositRule(name, ruleName, newReads, oc.condition(), null);
        if (deposit != null) out.add(deposit);
        RulePlan withoutDate = validityFault(ruleName, name, oc.validity());
        if (withoutDate != null) out.add(withoutDate);
        return out;
    }

    /** {@link #typePairRelation(Objecttype, Objecttype, FactModelGenerator.CreationLink)} for the object creation
     *  {@code oc} without a {@code connectingRole} binding, before any form lowers; null where the shared decision does
     *  not link the creation, which {@link #objectCreation} then refuses by name. */
    private RegelSpraakModel.FactType typePairRelation(ResolvedObjectCreation oc) {
        ObjectType fresh = oc.createdType(), subject = oc.subject();
        if (oc.name() == null || fresh == null || subject == null) return null;
        FactModelGenerator.CreationLink k = FactModelGenerator.creationLink(model, fresh, subject, null, oc.name());
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
    private RegelSpraakModel.FactType typePairRelation(ObjectType subject, ObjectType fresh,
                                                        FactModelGenerator.CreationLink k) {
        RegelSpraakModel.FactType found = null;
        for (RegelSpraakModel.FactType ft : model.factTypes.values()) {
            List<RegelSpraakModel.Role> r = ft.roles;
            boolean fits = r.size() == 2
                    ? fills(r.get(0), k.fwdRole(), fresh) && fills(r.get(1), k.backRole(), subject)
                      || fills(r.get(1), k.fwdRole(), fresh) && fills(r.get(0), k.backRole(), subject)
                    : r.size() == 1 && ft.reciprocal && subject == fresh && fills(r.get(0), k.fwdRole(), fresh);
            if (!fits) continue;
            if (found != null)
                throw new IllegalStateException("exec-model: de terugkoppeling ('" + k.backRole() + "' en '" + k.fwdRole()
                        + "') van een " + subject.name + " naar een " + fresh.name + " ligt in twee feittypen, '"
                        + found.name + "' en '" + ft.name + "', waar het gedeelde besluit haar rollen uniek vond");
            found = ft;
        }
        return found;
    }

    /** Whether role {@code r} carries the name {@code name} and is filled by exactly {@code type}. */
    private static boolean fills(RegelSpraakModel.Role r, String name, ObjectType type) {
        return name != null && Namen.norm(r.name()).equals(Namen.norm(name))
                && type.name.equalsIgnoreCase(r.objectTypeName());
    }

    /** The guard of an object creation beside its condition: no {@link CreationStamp} of creation {@code name} for the
     *  subject, so that it makes one object per subject, and no {@link CreationStamp} that made the subject itself in the
     *  current phase, bound at {@code phase} ({@link Reads#creationPhase}), so that an object waits for the next phase,
     *  after the rule layer has reached a fixpoint over it. */
    private List<LhsItem> creationGuard(String name, Reads reads, int phase) {
        String f = reads.variable(phase);
        String own = "$gemaakt_" + id("creatie"), fresh = "$nieuw_" + id("creatie");
        return List.of(
                new Not(new FactPattern(CreationStamp.class, own, List.of(equal(id("gemaakt"), "rule", name)),
                        List.of(new Join(id("gemaakt-onderwerp"), "subject", SUBJECT)), List.of())),
                new Not(new FactPattern(CreationStamp.class, fresh, List.of(),
                        List.of(new Join(id("nieuw-object"), "object", SUBJECT)),
                        List.of(new BetaTest(id("nieuw-in-deze-fase"), StaticFunction.IN_PHASE, f)), List.of())));
    }

    /** The evaluation stratum of the creation {@code form}, named {@code name}: every creation is a form of the
     *  dependency graph, so a creation without one is a fault of this transpiler. */
    private int stratum(Object form, String name) {
        Integer s = strata.get(form);
        if (s == null)
            throw new IllegalStateException("exec-model: de creatie \"" + name + "\" heeft geen stratum in de"
                    + " afhankelijkheidsgraaf");
        return s;
    }

    /**
     * A fact creation (RUNTIME-SEMANTICS ch. 5.5, IR-SPEC ch. 3.4), a rule of the agenda group {@link #FACT_CREATION}: the
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
    private List<RulePlan> factCreation(ResolvedFactCreation fc) {
        if (fc.name() == null) throw new Refusal(DroolsCode.FACT_CREATION_WITHOUT_NAME.reason());
        ObjectType anchor = fc.anchor(), target = fc.target();
        if (anchor == null || target == null || fc.factType() == null || fc.path() == null || fc.anchorRole() == null)
            throw new Refusal(DroolsCode.FACT_CREATION_UNRESOLVED_OPERAND.reason());
        RegelSpraakModel.FactType ft = factType(fc.factType());
        if (ft == null || ft.roles.size() != 2 || ft.reciprocal)
            throw new Refusal(DroolsCode.FACT_CREATION_NOT_BINARY.reason("factType", fc.factType()));
        String relation = Namen.norm(ft.name);
        boolean anchorIsFirst = Namen.norm(ft.roles.get(0).name()).equals(Namen.norm(fc.anchorRole()));
        Condition c = fc.condition();
        Condition.CharacteristicCheck targetCheck = c instanceof Condition.CharacteristicCheck k && k.owner() != null && !Host.hostOp(k, anchor)
                && Host.hostOp(k, target) && !navigationBindings.containsKey(k) && k.period() == null ? k : null;
        if (targetCheck == null && c != null && Host.targetHostOutsideRange(c, target, navigationBindings::containsKey))
            throw new Refusal(DroolsCode.TARGET_CONDITION_NESTED_HOST.reason("reason", Host.TARGET_ATTEMPT_NESTED));
        String ruleName = model.emittedRuleName(fc.name());
        Condition anchorGuard = targetCheck == null ? c : null;
        Supplier<Reads> fresh = () -> new Reads(anchor).valid(fc.validity());
        Reads reads = fresh.get();
        Set<Integer> read = reads.follow();
        beginReading();
        Term testTerm = anchorGuard == null ? null : conditionTerm(anchorGuard, reads);
        Noted noted = endReading();
        boolean canFail = reads.canFail();
        reads.stop();
        int stratum = stratum(fc, fc.name());
        int phase = reads.creationPhase(stratum);
        List<LhsItem> items = reads.plan(conditionRole(canFail), testTerm, read, anchorGuard);
        List<RoleBindingRef> steps = new ArrayList<>();
        for (ResolvedFactCreation.PathStep t : fc.pathSteps()) steps.add(t.roleBinding());
        steps.add(fc.path());
        List<Hop> hops = new ArrayList<>();
        ObjectType cur = anchor;
        for (RoleBindingRef step : steps) {
            Hop h = step(step, cur, Msg.drools("PART.desc.fact-creation-path", "name", fc.name()));
            hops.add(h);
            cur = h.destination();
        }
        List<FactPattern> path = new ArrayList<>(chain(hops, SUBJECT, reads.variable(phase)));
        String targetVar = path.get(path.size() - 1).name();
        if (targetCheck != null) {
            TargetGuard b = targetGuard(targetCheck, targetVar);
            path.set(path.size() - 1, withAfterAlpha(path.get(path.size() - 1), b.afterAlpha()));
            items.addAll(path);
            if (b.state() != null) items.add(b.state());
        } else {
            items.addAll(path);
        }
        String anchorEnd = anchorIsFirst ? "first" : "second", targetEnd = anchorIsFirst ? "second" : "first";
        items.add(new Not(new FactPattern(RelationLink.class, "$bestaand_" + id("feitcreatie"),
                List.of(equal(id("bestaand"), "relation", relation)),
                List.of(new Join(id("bestaand-anker"), anchorEnd, SUBJECT),
                        new Join(id("bestaand-doel"), targetEnd, targetVar)),
                List.of())));
        Consequence relationLink = new Consequence.RelationLink(relation, anchorIsFirst ? SUBJECT : targetVar,
                anchorIsFirst ? targetVar : SUBJECT, phase, Consequence.Insertion.STATED);
        reads.recorder();
        creationRules.add(ruleName);
        creationStrata.add(stratum);
        List<RulePlan> out = new ArrayList<>();
        out.add(as(RuleOrigin.Role.OWN, reads.rulePlan(ruleName, FACT_CREATION, items, relationLink)));
        // the guard on the anchor at the end world, in the main group, without the creation's phase and the path to
        // its targets, so one reading per anchor
        if (noted != null && testTerm != null)
            readingRule(ruleName, positions.versionOf(fc), reads, reads.readingPlan(List.of(), true), testTerm, null,
                    anchorGuard, null, noted, fc.validity(), false);
        if (canFail) out.add(conditionFaultRule(fc.name(), ruleName, fresh, anchorGuard, null));
        RulePlan deposit = depositRule(fc.name(), ruleName, fresh, anchorGuard, null);
        if (deposit != null) out.add(deposit);
        RulePlan withoutDate = validityFault(ruleName, fc.name(), fc.validity());
        if (withoutDate != null) out.add(withoutDate);
        return out;
    }

    /** A step of a fact creation's path over {@code step} from {@code van}: over a relation a form creates by its
     *  {@link RelationLink} facts, else by the reference field of the to-one end, on the near bean where the far role is
     *  to-one and on the far bean where it is to-many. Two to-many roles have no field in the bean model (§13.3.9). */
    private Hop step(RoleBindingRef step, ObjectType van, Object description) {
        RegelSpraakModel.Role[] pair = step == null ? null : FactModelGenerator.boundRolePair(model, step);
        if (pair == null || pair[0] == pair[1])
            throw new Refusal(DroolsCode.STEP_NOT_ROLE_PAIR.reason("description", description));
        ObjectType destination = model.objectTypeByName(pair[0].objectTypeName());
        if (destination == null) throw new Refusal(DroolsCode.STEP_ROLE_WITHOUT_FILLER.reason("description", description, "role", pair[0].name()));
        Hop created = linkHop(step, van, destination);
        if (created != null) return created;
        if (!pair[0].many()) {
            if (!FactModelGenerator.hasReferenceField(model, van, pair[0].name(), destination))
                throw new Refusal(DroolsCode.ONE_HOP_NO_FIELD.reason("description", description, "reason", FactModelGenerator.noReferenceFieldReason(model, van, pair[0].name(), destination).msg()));
            return new Hop(van, destination, pair[0].name(), true, null, false);
        }
        if (!pair[1].many()) {
            if (!FactModelGenerator.hasReferenceField(model, destination, pair[1].name(), van))
                throw new Refusal(DroolsCode.ONE_HOP_NO_FIELD.reason("description", description, "reason", FactModelGenerator.noReferenceFieldReason(model, destination, pair[1].name(), van).msg()));
            return new Hop(van, destination, pair[1].name(), false, null, false);
        }
        throw new Refusal(DroolsCode.MANY_PAIR_NO_FIELD.reason("description", description, "factType", step.factType()));
    }

    /** The guard of a fact creation on each target ({@code check}, hosted on a type the target satisfies), for the
     *  target the variable {@code target} binds at the end of the creation's path: the target must be of the host's type,
     *  and its characteristic must hold as the check asks, read through its standing where a form writes it and off the
     *  bean otherwise, through the getter of the class that declares it. The constraints on the target come after the
     *  one that reaches it ({@link FactPattern#afterAlpha}), as do those on the standing after its join. */
    private TargetGuard targetGuard(Condition.CharacteristicCheck check, String target) {
        ObjectType host = check.owner();
        Member member = member(host, check.characteristic(), true);
        Class<?> hostClass = beanClass(host);
        boolean desired = !check.negation();
        Alpha hostConstraint = new Alpha(id("doel-gastheer"),
                List.of(new Alpha.FieldTest(null, Alpha.Comparator.IS_A, hostClass)));
        ElementKey key = member.key();
        if (writers.containsKey(key)) {
            String st = "$doelstand_" + id("doel");
            FactPattern state = new FactPattern(CharacteristicState.class, st, List.of(equal(id("doelstand"), "characteristic", key)),
                    List.of(new Join(id("doelstand-doel"), "subject", target)), List.of(),
                    List.of(equal(id("doelstand-waar"), "holds", desired)), List.of());
            read.putIfAbsent(key, member);
            return new TargetGuard(List.of(hostConstraint), state);
        }
        Alpha characteristic = new Alpha(id("doel-kenmerk"), List.of(new Alpha.FieldTest(List.of(), member.name(),
                Binding.ReadForm.WAAR, Alpha.Comparator.GELIJK, desired, member.javaClass())));
        return new TargetGuard(List.of(hostConstraint, characteristic), null);
    }

    /** The guard of a fact creation on its target ({@link #targetGuard}): the constraints on the target's pattern after
     *  the one that reaches it, and the pattern of the target's characteristic standing, null where the guard reads the
     *  characteristic off the bean. */
    private record TargetGuard(List<Alpha> afterAlpha, FactPattern state) {}

    /** {@code p} with the constraints {@code afterAlpha} after its joins and its other constraints. */
    private static FactPattern withAfterAlpha(FactPattern p, List<Alpha> afterAlpha) {
        List<Alpha> all = new ArrayList<>(p.afterAlpha());
        all.addAll(afterAlpha);
        return new FactPattern(p.factClass(), p.name(), p.alpha(), p.joins(), p.betaTests(), all, p.bindings());
    }

    /** {@code p} binding {@code b} as well, after its other bindings. */
    private static FactPattern withBinding(FactPattern p, Binding b) {
        List<Binding> all = new ArrayList<>(p.bindings());
        all.add(b);
        return new FactPattern(p.factClass(), p.name(), p.alpha(), p.joins(), p.betaTests(), p.afterAlpha(), all);
    }

    /**
     * The rules every relation a form creates needs, whether a form reads it or not: a rule per to-one end with a
     * reference field that inserts the links the case input carries as {@link RelationLink} facts, so that the readers of
     * the relation see the input and the created links alike, and a rule per to-one role that faults a second distinct
     * filler of it: one object filling the other role with two links to different objects, the fault of ch. 5.5,
     * never an overwrite.
     */
    private List<RulePlan> linkRules(String relation) {
        RegelSpraakModel.FactType ft = factType(relation);
        if (ft == null || ft.roles.size() != 2 || ft.reciprocal) return List.of();
        RegelSpraakModel.Role r0 = ft.roles.get(0), r1 = ft.roles.get(1);
        ObjectType t0 = model.objectTypeByName(r0.objectTypeName()), t1 = model.objectTypeByName(r1.objectTypeName());
        if (t0 == null || t1 == null) return List.of();
        List<RulePlan> out = new ArrayList<>();
        if (!r1.many() && FactModelGenerator.hasReferenceField(model, t0, r1.name(), t1))
            out.add(inputLink(relation, t0, r1.name(), true));
        if (!r0.many() && FactModelGenerator.hasReferenceField(model, t1, r0.name(), t0))
            out.add(inputLink(relation, t1, r0.name(), false));
        if (!r1.many()) out.add(secondFilling(ft, relation, true, r1.name(), t0));
        if (!r0.many()) out.add(secondFilling(ft, relation, false, r0.name(), t1));
        return out;
    }

    /** The links of {@code relation} the reference field {@code field} of the beans of {@code holder} carries, the holder
     *  being the link's first end where {@code holderIsFirst}: a rule of no form, whose subject is the holder and whose
     *  link has phase 0, the input's. */
    private RulePlan inputLink(String relation, ObjectType holder, String field, boolean holderIsFirst) {
        Class<?> javaClass = beanClass(holder);
        Subject h = new Subject(javaClass, "$houder", List.of(new Alpha(id("koppeling-invoer"),
                List.of(new Alpha.FieldTest(field, Alpha.Comparator.GEVULD, null)))), List.of(),
                List.of(Binding.of("$ander", javaClass, field)));
        return new RulePlan("koppeling " + relation + " uit de invoer: " + holder.name + "." + field, null, null,
                new ReadTable(List.of()), List.of(h), new Consequence.RelationLink(relation, holderIsFirst ? "$houder" : "$ander",
                holderIsFirst ? "$ander" : "$houder", -1, Consequence.Insertion.LOGICAL));
    }

    /** The fault of two links of {@code relation} from one object to two different fillers of the to-one role
     *  {@code role}: the near object is the links' first end where {@code nearIsFirst}. */
    private RulePlan secondFilling(RegelSpraakModel.FactType ft, String relation, boolean nearIsFirst, String role,
                                    ObjectType nearType) {
        String nearEnd = nearIsFirst ? "first" : "second", farEnd = nearIsFirst ? "second" : "first";
        String sentence = "de naar-één-rol \"" + role + "\" van een " + nearType.name + " heeft twee verschillende vullingen;"
                + " een tweede vulling is een fout, geen overschrijving (RUNTIME-SEMANTICS ch. 5.5)";
        FactPattern one = new FactPattern(RelationLink.class, "$een", List.of(equal(id("vulling"), "relation", relation)), List.of(),
                List.of(Binding.of("$nabij", RelationLink.class, nearEnd)));
        FactPattern two = new FactPattern(RelationLink.class, "$twee", List.of(equal(id("tweede-vulling"), "relation", relation)),
                List.of(new Join(id("tweede-vulling-nabij"), nearEnd, "$een",
                        new Binding.Field(RelationLink.class, nearEnd))),
                List.of(new BetaTest(id("tweede-vulling-ander"), StaticFunction.OTHER_FILLING, "$een",
                        List.of(constant(farEnd)))),
                List.of());
        return new RulePlan("koppeling " + relation + ": tweede vulling van " + role, null, null,
                new ReadTable(List.of()), List.of(one, two),
                new Consequence.SecondFilling("$nabij", ft.name, sentence, Consequence.Insertion.LOGICAL));
    }

    // ---------------- prioritized defaults ----------------

    /** The name of an effective stack, as the destructive lowering named the rule of the group: the kind-discriminated
     *  target on the stack's owner, so two types that share one inherited target name two stacks. */
    private static String stackName(Stacks.Stack st) {
        PrioritizedDefaults l0 = st.members().get(0);
        return "defaults " + (l0.isCharacteristic() ? "K:" : "A:") + st.owner().name + "." + l0.target();
    }

    /** The member a stack decides, as declared: on the type of its owner's chain that declares it, in that type's
     *  spelling, the way the model names it ({@link RegelSpraakModel#designatable}). */
    private static RegelSpraakModel.Designatable stackTarget(Stacks.Stack st) {
        PrioritizedDefaults l0 = st.members().get(0);
        for (ObjectType t : st.owner().chain()) {
            if (l0.isCharacteristic()) {
                for (String k : t.characteristics)
                    if (Namen.norm(k).equals(Namen.norm(l0.target()))) return RegelSpraakModel.Designatable.characteristic(t.name, k);
            } else {
                for (String a : t.attributes.keySet())
                    if (Namen.norm(a).equals(Namen.norm(l0.target()))) return RegelSpraakModel.Designatable.attribute(t.name, a);
            }
        }
        throw new IllegalStateException("de stapel " + stackName(st) + " beslist een lid dat de keten van "
                + st.owner().name + " niet declareert");
    }

    /**
     * One default of a stack where the lowering places it: the level it is read on, its chain and its position there,
     * a label for the names of its rules, the key of a conflict among its exception groups (RUNTIME-SEMANTICS ch. 7.9:
     * the target form and the default indices from the root, member indices left out), and, where it has exception
     * groups, their chains, numbered consecutively from {@code firstMember}.
     */
    private record Node(Default d, ObjectType levelOf, int chain, int position, String label, String key,
                         List<List<Node>> members, int firstMember, Validity validity) {
        boolean hasMembers() { return members != null; }
    }

    /** What the rules of one stack share while they are built. */
    private static final class StackBuild {
        final String name;
        final Member member;
        final boolean characteristic;
        /** The bean class of the stack's owner: the stack runs on instances of exactly that type (ch. 7.10). */
        final Class<?> javaClass;
        /** The bare ch. 7.2 conflict key, the target on the type that declares it. */
        final String targetForm;
        final List<Reads> readings = new ArrayList<>();
        /** The default each of {@link #readings} belongs to. */
        final Map<Reads, Default> defaultOf = new IdentityHashMap<>();
        final Set<String> controlFacts = new LinkedHashSet<>();
        int nextChain;

        StackBuild(String name, Member member, boolean characteristic, Class<?> javaClass, String targetForm, int rootChains) {
            this.name = name;
            this.member = member;
            this.characteristic = characteristic;
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
    private List<RulePlan> stackRules(Stacks.Stack st) {
        PrioritizedDefaults l0 = st.members().get(0);
        ObjectType owner = st.owner();
        boolean characteristic = l0.isCharacteristic();
        String target = l0.target();
        if (characteristic && owner.attribute(target) != null)
            throw new Refusal(DroolsCode.CHARACTERISTIC_TARGET_NAME_CLASH.reason("attribute", target, "objectType", owner.name));
        Member member = member(owner, target, characteristic);
        ObjectType declarant = owner.declarantOf(target);
        StackBuild b = new StackBuild(stackName(st), member, characteristic, beanClass(owner),
                (declarant != null ? declarant.name : l0.subject()) + "." + target, st.members().size());
        List<List<Node>> squareRoot = new ArrayList<>();
        // a group with a dated validity gates its own chain, the exception groups below its defaults included: where
        // the window does not admit the date the group decides nothing (RUNTIME-SEMANTICS ch. 8.2)
        for (int m = 0; m < st.members().size(); m++)
            squareRoot.add(chain(b, st.members().get(m).defaults(), m, null, owner, List.of(),
                    st.members().size() == 1 ? "" : m + ":", st.members().get(m).validity()));
        boolean general = squareRoot.size() > 1 || squareRoot.stream().flatMap(List::stream).anyMatch(Node::hasMembers);
        List<RulePlan> rules = new ArrayList<>(general ? generalForm(b, squareRoot) : simpleForm(b, squareRoot.get(0)));
        if (st.members().stream().anyMatch(l -> l.validity() != null && !l.validity().always()))
            rules.add(stackValidityFault(b));
        // registered only once every default of the stack lowered, as a single rule registers its reads
        for (Reads reads : b.readings) {
            lowering = b.defaultOf.get(reads);
            try {
                reads.recorder();
            } finally {
                lowering = null;
            }
        }
        // every rule of the stack lowers a default of it or the stack itself, and originOf says which: the stack's own
        // rules, and the contribution rules of the folds registered under its name, which do not stand in that list and
        // which a fault rule of a justification registers on its own, before the loop above
        List<String> ofStack = new ArrayList<>();
        for (RulePlan r : rules) ofStack.add(r.name());
        formOfRule.forEach((rule, of) -> { if (of.equals(b.name)) ofStack.add(rule); });
        for (String rule : ofStack)
            if (!defaultOfRule.containsKey(rule) && !stackOwnRules.contains(rule))
                throw new IllegalStateException("de regel '" + rule + "' van de stapel " + b.name + " verlaagt"
                        + " geen default en niet de stapel zelf");
        derived.put(member.key(), member);
        defaultsGroups.add(new DefaultsGroup(b.name, general, rules.size(), List.copyOf(b.controlFacts)));
        layouts.put(b.name, new StackLayout(member, b.targetForm, general, squareRoot, b.javaClass));
        return rules;
    }

    /** The rule that faults every object of the stack's owner, of exactly its type, in a session without an evaluation
     *  date, where a group of the stack has a dated validity: the reference interpreter asks the validity of a group when
     *  it evaluates the stack on an object, and faults there (RUNTIME-SEMANTICS ch. 8.1), so an owner without objects
     *  faults nothing. */
    private RulePlan stackValidityFault(StackBuild b) {
        evaluationDateNeeded();
        String name = b.name;
        Alpha concrete = concreteType(b.javaClass);
        Alpha withoutDate = new Alpha(id("geen-rekendatum"),
                List.of(new Alpha.FieldTest("date", Alpha.Comparator.LEEG, null)));
        String rule = name + ": geen rekendatum";
        stackOwnRules.add(rule);
        return as(RuleOrigin.Role.NO_EVALUATION_DATE, new RulePlan(rule, form, null, new ReadTable(List.of()),
                List.of(new Subject(b.javaClass, SUBJECT, List.of(concrete), List.of(), List.of()),
                        new FactPattern(EvaluationDate.class, "$rekendatum", List.of(withoutDate), List.of(), List.of())),
                new Consequence.Fault(SUBJECT, ComputeFault.VALIDITY_WITHOUT_DATE, new Consequence.FaultName(name, name),
                        Consequence.Insertion.LOGICAL)));
    }

    /** The constraint that keeps a rule of a stack on instances of exactly the stack's owner, the bean class
     *  {@code javaClass}: the stack runs on instances of exactly that type (RUNTIME-SEMANTICS ch. 7.10,
     *  {@link StackBuild#javaClass}). */
    private Alpha concreteType(Class<?> javaClass) {
        return new Alpha(id("concreet-type"), List.of(new Alpha.FieldTest(null, Alpha.Comparator.CLASS_IS, javaClass)));
    }

    /** The knots of one chain, {@code id}, with its defaults' levels: a root default's own ({@link Stacks#levelOf}), an
     *  exception group's defaults on the level of the default that carries them ({@code carrier}); every knot carries
     *  the validity of the group at the root of its chain. */
    private List<Node> chain(StackBuild b, List<Default> ds, int id, ObjectType carrier, ObjectType owner,
                              List<Integer> path, String prefix, Validity validity) {
        List<Node> out = new ArrayList<>();
        for (int i = 0; i < ds.size(); i++) {
            Default d = ds.get(i);
            ObjectType levelOf = carrier != null ? carrier : stacks.levelOf(d, owner);
            verifyDefault(b, d);
            List<Integer> ownPath = new ArrayList<>(path);
            ownPath.add(i);
            String label = prefix + i;
            StringBuilder key = new StringBuilder(b.targetForm);
            for (int p : ownPath) key.append('/').append(p);
            List<List<Node>> members = null;
            int earliest = -1;
            if (d.exceptions() != null) {
                List<dev.uchar.regelspraak.ir.ExceptionGroup> groups = d.exceptions().members();
                earliest = b.nextChain;
                b.nextChain += groups.size();
                members = new ArrayList<>();
                for (int m = 0; m < groups.size(); m++)
                    members.add(chain(b, groups.get(m).defaults(), earliest + m, levelOf, owner, ownPath,
                            label + "." + m + ".", validity));
            }
            out.add(new Node(d, levelOf, id, i, label, key.toString(), members, earliest, validity));
        }
        return out;
    }

    /** The refusals per default. */
    private void verifyDefault(StackBuild b, Default d) {
        if (d.initialisation() && b.characteristic)
            throw new Refusal(DroolsCode.DEFAULT_INITIALISATION_ON_CHARACTERISTIC.reason("attribute", b.member.name(), "declarant", b.member.declarant().name));
        if (!d.initialisation() && d.condition() == null && d.hasOwnContent())
            throw new Refusal(DroolsCode.DEFAULT_WITHOUT_JUSTIFICATION.reason("source", d.source() != null ? d.source() : DroolsCode.DEFAULT_WITHOUT_JUSTIFICATION.part("without-source"),
                    "attribute", b.member.name()));
        String division = division(d.condition(), d.consequence());
        if (division != null)
            throw new Refusal(DroolsCode.DEFAULT_DIVISION_INTERMEDIATE.reason("division", division));
        String squareRoot = squareRoot(d.condition(), d.consequence());
        if (squareRoot != null)
            throw new Refusal(DroolsCode.DEFAULT_SQUARE_ROOT.reason("squareRoot", squareRoot));
    }

    /** The snippet of the first division in {@code conditionTerm} or {@code value}, or null: in the justification or the
     *  consequence of a default, and in the condition or the value of an initialisation beside another writer, which
     *  ch. 7.7 reads as the lowest default ({@link #initialisationRule}). The destructive lowering refused a division
     *  below the top of a consequence, whose deposit would persist; here the evaluation itself is speculative, as a
     *  default is evaluated whenever its reads stand. A division by zero in a default another one displaces is a fault
     *  on an intermediate state, which this branch would withdraw ({@link Fault}); whether such a fault counts waits on
     *  a decision of the norm, so every division is refused until then. Since none passes, the rules of a default raise
     *  no division fault ({@link #ownRule}). The walk enters every expression and condition the forms carry, a
     *  {@code Let}'s value and body, both branches of an {@code IfThenElse} and its condition, a rounding, a clamp's
     *  bounds and a fold's argument, fallback and filter among them, so a division under one of them is found too. */
    private static String division(Condition conditionTerm, Expr value) {
        return earliest(conditionTerm, value, TmsTranspiler::isDivision);
    }

    private static String division(Expr e) {
        return earliest(null, e, TmsTranspiler::isDivision);
    }

    /** The snippet of the first square root in {@code value}, or null: a place of the §6.6 deposit, which a member
     *  initialisation and a default do not make. */
    private static String squareRoot(Expr e) {
        return earliest(null, e, TmsTranspiler::isSquareRoot);
    }

    private static String squareRoot(Condition conditionTerm, Expr value) {
        return earliest(conditionTerm, value, TmsTranspiler::isSquareRoot);
    }

    private static boolean isDivision(Expr e) {
        return e instanceof Expr.Binary b && (b.op() == Expr.BinOp.GEDEELD_DOOR || b.op() == Expr.BinOp.GEDEELD_DOOR_ABS);
    }

    private static boolean isSquareRoot(Expr e) {
        return e instanceof Expr.FunctionCall f && f.kind() == Expr.FunctionKind.WORTEL;
    }

    /** The snippet of the first expression node for which {@code category} holds, in {@code conditionTerm} and then in
     *  {@code value} (either may be null), each walked node before its operands, or null. */
    private static String earliest(Condition conditionTerm, Expr value, java.util.function.Predicate<Expr> category) {
        String[] found = {null};
        java.util.function.Predicate<Expr> touch = x -> {
            if (!category.test(x)) return false;
            found[0] = snippet(x.source());
            return true;
        };
        if (Condition.containsOperand(conditionTerm, touch) || Expr.contains(value, touch)) return found[0];
        return null;
    }

    /** Whether {@code d} decides anything itself: a decision on a characteristic, a consequence on an attribute. */
    private static boolean decidesSelf(StackBuild b, Default d) {
        return b.characteristic ? d.decision() != null : d.consequence() != null;
    }

    /**
     * The simple form, variant M of A10 in the trial of card drools-logische-afleiding: one rule per default that
     * decides anything itself, with its reads bound, its justification as the test and {@code not Beslist} over every
     * higher index; the consequence computes the value from what the rule bound and, where it decides, inserts the
     * write and {@link Decides}. A leeg consequence inserts nothing, so the lower defaults decide (ch. 7.1); the
     * initialisation default matches only where the target is empty on the bean, which holds the input (ch. 7.3). Every
     * decision stands as its {@code Decides}, the lowest one's and a do-not-attribute decision's at the bottom included,
     * which displace nothing: the session says which default decided ({@link #decisionsOf}), where until card
     * defaults-decider those two left nothing, and the do-not-attribute decision at the bottom had no rule at all.
     */
    private List<RulePlan> simpleForm(StackBuild b, List<Node> chain) {
        List<Node> deciding = chain.stream().filter(k -> decidesSelf(b, k.d())).toList();
        String name = b.name;
        ElementKey key = b.member.key();
        List<RulePlan> out = new ArrayList<>();
        if (!deciding.isEmpty()) b.controlFacts.add("Beslist");
        for (int j = 0; j < deciding.size(); j++) {
            Node k = deciding.get(j);
            int i = k.position();
            boolean higher = j > 0;
            // the target: an attribute's value, or the attribution of a characteristic, whose decision the default
            // declares; a do-not-attribute decision writes nothing and only displaces the defaults below it
            List<Consequence.StackFact> facts = new ArrayList<>();
            if (!b.characteristic) facts.add(new Consequence.StackFact.Value(key, name));
            else if (Boolean.TRUE.equals(k.d().decision())) facts.add(new Consequence.StackFact.Characteristic(key));
            facts.add(new Consequence.StackFact.Decides(name, i));
            out.addAll(ownRule(b, k, RuleOrigin.Role.OWN, name + ": default " + k.label(),
                    () -> higher ? List.of(noDecides(name, i)) : List.of(), facts));
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
        ElementKey key = b.member.key();
        // at the root the reconciliation writes the target and deposits a conflict as the §9.5 inconsistency; on a
        // characteristic only an attribution reaches the write (onlyTrue), so the attribution is planned as such
        Consequence.StackFact target = b.characteristic ? new Consequence.StackFact.Characteristic(key)
                : new Consequence.StackFact.Value(key, b.name);
        int root = out.size();
        reconciliation(b, 0, squareRoot.size(), squareRoot.stream().anyMatch(TmsTranspiler::canConflict), b.targetForm, "wortel",
                List::of, new Consequence.StackFact.Inconsistency(), target, b.characteristic, false, out);
        for (RulePlan r : out.subList(root, out.size())) stackOwnRules.add(r.name());
        return out;
    }

    /**
     * The rules of {@code chain} and, below each of its defaults, of their member chains. Every rule of a default waits
     * on the positions above it in its own chain and on {@code ancestors}, the positions above each default whose
     * exception group the chain is, at any depth: the reference interpreter reaches the exception groups of a default
     * only where no higher default in its chain decides ({@code decide}), so a group below a displaced default is not
     * evaluated, settles nothing and raises no fault (RUNTIME-SEMANTICS ch. 7.9; PLAN §9, 2026-10-09).
     */
    private void chainRules(StackBuild b, List<Node> chain, Supplier<List<LhsItem>> ancestors, List<RulePlan> out) {
        String name = b.name;
        for (Node k : chain) {
            int id = k.chain(), i = k.position();
            Supplier<List<LhsItem>> notHigher = () -> {
                List<LhsItem> guard = new ArrayList<>(ancestors.get());
                if (i > 0) guard.add(noOutcome(name, id, i));
                return guard;
            };
            Consequence.StackFact decides = new Consequence.StackFact.Outcome(name, id, i, Outcome.Kind.DECIDED);
            if (!k.hasMembers()) {
                if (!decidesSelf(b, k.d())) continue;
                b.controlFacts.add("Uitkomst " + Outcome.Kind.DECIDED);
                out.addAll(ownRule(b, k, RuleOrigin.Role.OWN, name + ": default " + k.label(), notHigher,
                        List.of(decides)));
                continue;
            }
            for (List<Node> member : k.members()) chainRules(b, member, notHigher, out);
            int earliest = k.firstMember(), to = earliest + k.members().size();
            int reconciling = out.size();
            reconciliation(b, earliest, k.members().size(), k.members().stream().anyMatch(TmsTranspiler::canConflict),
                    k.key(), k.label(), notHigher,
                    new Consequence.StackFact.Outcome(name, id, i, Outcome.Kind.CONFLICT), decides, false, true, out);
            for (RulePlan r : out.subList(reconciling, out.size())) defaultOfRule.put(r.name(), k.d());
            if (decidesSelf(b, k.d()))
                out.addAll(ownRule(b, k, RuleOrigin.Role.FALLBACK, name + ": terugval " + k.label(), () -> {
                    List<LhsItem> guard = new ArrayList<>();
                    guard.add(new Not(outcomes(name, earliest, to, null)));
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
     * {@code key} where another member decides differently (numbers by value), and the decision otherwise. A rule
     * per outcome, each under {@code guard}; the rules that cannot match are left out (a conflict among members that
     * cannot conflict, a difference among one member). {@code onlyTrue} restricts the decision to an attribution, the
     * one decision on a characteristic that writes; {@code between} says the outcomes are control facts, not the write.
     */
    private void reconciliation(StackBuild b, int earliest, int itemCount, boolean membersConflict, String key, String label,
                            Supplier<List<LhsItem>> guard, Consequence.StackFact atConflict,
                            Consequence.StackFact atDecision, boolean onlyTrue, boolean between, List<RulePlan> out) {
        String name = b.name;
        int to = earliest + itemCount;
        Binding ofSubject = Binding.of(SUBJECT, Outcome.class, "subject");
        if (membersConflict) {
            List<LhsItem> items = new ArrayList<>();
            items.add(new FactPattern(Outcome.class, "$conflict",
                    List.of(outcomeOfMember("lid-conflict", name, Outcome.Kind.CONFLICT, earliest, to, false)), List.of(),
                    List.of(ofSubject, Binding.of("$sleutel", Outcome.class, "key"))));
            if (itemCount > 1) items.add(new Not(outcomes(name, earliest, to, Outcome.Kind.CONFLICT,
                    "eerder-lid", StaticFunction.EARLIER_MEMBER, "$conflict")));
            items.addAll(guard.get());
            out.add(as(RuleOrigin.Role.RECONCILE_PASS_ON, new RulePlan(name + ": doorgeven " + label, form, null,
                    new ReadTable(List.of(new dev.uchar.regelspraak.transpiler.tms.plan.Read(0,
                            ReadKind.CONFLICT_KEY, "conflictsleutel", "$sleutel"))), items,
                    new Consequence.Conflict(SUBJECT, new Term.SlotValue(0, null), List.of(0), 1, atConflict,
                            Consequence.Insertion.LOGICAL))));
            if (between) b.controlFacts.add("Uitkomst " + Outcome.Kind.CONFLICT);
        }
        if (itemCount > 1) {
            List<LhsItem> items = new ArrayList<>();
            items.add(new FactPattern(Outcome.class, "$besluit",
                    List.of(outcomeOfMember("lid-besluit", name, Outcome.Kind.DECIDED, earliest, to, false)), List.of(),
                    List.of(ofSubject)));
            items.add(new Not(outcomes(name, earliest, to, Outcome.Kind.DECIDED, "eerder-lid",
                    StaticFunction.EARLIER_MEMBER, "$besluit")));
            if (membersConflict) items.add(new Not(outcomes(name, earliest, to, Outcome.Kind.CONFLICT)));
            FactPattern another = outcomes(name, earliest, to, Outcome.Kind.DECIDED, "ander-besluit", StaticFunction.OTHER_DECISION,
                    "$besluit");
            items.add(new Exists(List.of(another), Exists.declared(List.of(another)), Exists.read(List.of(another))));
            items.addAll(guard.get());
            out.add(as(RuleOrigin.Role.RECONCILE_DIFFERENCE, new RulePlan(name + ": verschil " + label, form, null,
                    new ReadTable(List.of()), items,
                    new Consequence.Conflict(SUBJECT, constant(key), List.of(), 0, atConflict,
                            Consequence.Insertion.LOGICAL))));
            if (between) b.controlFacts.add("Uitkomst " + Outcome.Kind.CONFLICT);
        }
        List<LhsItem> items = new ArrayList<>();
        items.add(new FactPattern(Outcome.class, "$besluit",
                List.of(outcomeOfMember("lid-besluit", name, Outcome.Kind.DECIDED, earliest, to, onlyTrue)), List.of(),
                List.of(ofSubject, Binding.of("$waarde", Outcome.class, "value"))));
        if (itemCount > 1) {
            items.add(new Not(outcomes(name, earliest, to, Outcome.Kind.DECIDED, "eerder-lid", StaticFunction.EARLIER_MEMBER,
                    "$besluit")));
            if (membersConflict) items.add(new Not(outcomes(name, earliest, to, Outcome.Kind.CONFLICT)));
            items.add(new Not(outcomes(name, earliest, to, Outcome.Kind.DECIDED, "ander-besluit",
                    StaticFunction.OTHER_DECISION, "$besluit")));
        }
        items.addAll(guard.get());
        out.add(as(RuleOrigin.Role.RECONCILE_AGREEMENT, new RulePlan(name + ": eens " + label, form, null,
                new ReadTable(List.of(new dev.uchar.regelspraak.transpiler.tms.plan.Read(0, ReadKind.DECISION,
                        "besluit", "$waarde"))), items,
                new Consequence.Decision(SUBJECT, new Term.SlotValue(0, null), List.of(0), 1, false, null, List.of(atDecision),
                        Consequence.Insertion.LOGICAL))));
        if (between) b.controlFacts.add("Uitkomst " + Outcome.Kind.DECIDED);
    }

    /** The constraint {@code category} on the outcome a reconciliation binds: of stack {@code name}, settled as
     *  {@code outcome}, of a member chain in {@code [eerste, tot)}, and where {@code onlyTrue} an attribution, the one
     *  decision on a characteristic that writes. */
    private Alpha outcomeOfMember(String category, String name, Outcome.Kind outcome, int earliest, int to,
                                boolean onlyTrue) {
        List<Alpha.FieldTest> tests = new ArrayList<>(List.of(
                new Alpha.FieldTest("stack", Alpha.Comparator.GELIJK, name),
                new Alpha.FieldTest("kind", Alpha.Comparator.GELIJK, outcome),
                new Alpha.FieldTest("chain", Alpha.Comparator.AT_LEAST, earliest),
                new Alpha.FieldTest("chain", Alpha.Comparator.UNDER, to)));
        if (onlyTrue) tests.add(new Alpha.FieldTest("value", Alpha.Comparator.GELIJK, Boolean.TRUE));
        return new Alpha(id(category), tests);
    }

    /** The outcomes of the chains {@code [van, tot)} of stack {@code name} for the subject, of {@code category}, or of
     *  either where it is null, under a variable of their own. */
    private FactPattern outcomes(String name, int van, int to, Outcome.Kind category) {
        return outcomes(name, van, to, category, null, null, null);
    }

    /** As {@link #outcomes(String, int, int, Outcome.Kind)}, with the constraint {@code betaKind}, which calls
     *  {@code function}, between the outcome and the one the variable {@code target} binds, where it is not null. */
    private FactPattern outcomes(String name, int van, int to, Outcome.Kind category, String betaKind, StaticFunction function,
                               String target) {
        String variable = "$uitkomst_" + idSeq;
        List<Alpha.FieldTest> tests = new ArrayList<>(List.of(
                new Alpha.FieldTest("stack", Alpha.Comparator.GELIJK, name),
                new Alpha.FieldTest("chain", Alpha.Comparator.AT_LEAST, van),
                new Alpha.FieldTest("chain", Alpha.Comparator.UNDER, to)));
        if (category != null) tests.add(new Alpha.FieldTest("kind", Alpha.Comparator.GELIJK, category));
        Alpha alpha = new Alpha(id("uitkomst"), tests);
        Join onSubject = new Join(id("uitkomst-onderwerp"), "subject", SUBJECT);
        List<BetaTest> beta = betaKind == null ? List.of() : List.of(new BetaTest(id(betaKind), function, target));
        return new FactPattern(Outcome.class, variable, List.of(alpha), List.of(onSubject), beta, List.of());
    }

    /** {@code not Uitkomst} over the positions above {@code position} in chain {@code chain}, for the subject. */
    private LhsItem noOutcome(String name, int chain, int position) {
        String variable = "$hoger_" + idSeq;
        Alpha higher = new Alpha(id("hoger-uitkomst"), List.of(
                new Alpha.FieldTest("stack", Alpha.Comparator.GELIJK, name),
                new Alpha.FieldTest("chain", Alpha.Comparator.GELIJK, chain),
                new Alpha.FieldTest("position", Alpha.Comparator.UNDER, position)));
        return new Not(new FactPattern(Outcome.class, variable, List.of(higher),
                List.of(new Join(id("hoger-uitkomst-onderwerp"), "subject", SUBJECT)), List.of()));
    }

    /** {@code not Beslist} over the indices above {@code index} in the stack, for the subject. */
    private LhsItem noDecides(String name, int index) {
        String variable = "$hoger_" + idSeq;
        Alpha higher = new Alpha(id("hoger-beslist"), List.of(
                new Alpha.FieldTest("stack", Alpha.Comparator.GELIJK, name),
                new Alpha.FieldTest("index", Alpha.Comparator.UNDER, index)));
        return new Not(new FactPattern(Decides.class, variable, List.of(higher),
                List.of(new Join(id("hoger-beslist-onderwerp"), "subject", SUBJECT)), List.of()));
    }

    /**
     * The rule of a default's own decision: the subject pattern on the default's level, restricted to the stack's
     * owner, with the input test of an initialisation default; the reads of its justification and its consequence bound
     * as any rule binds them; {@code guard}, the control-fact patterns, before the test of the justification; and a
     * consequence that hands the decision to {@code decision}: a characteristic's decision as declared, an attribute's
     * value computed from what the rule bound, and nothing where that value is leeg (ch. 7.1). The validity of the group
     * at the root of the default's chain gates the rule on the evaluation date ({@link Reads#valid}).
     *
     * <p>A default carries no division and no square root ({@link #verifyDefault}), but its justification or its
     * consequence can still fault, a date step out of range or a context value without an evaluation date
     * ({@link ComputeFault}). Such a fault is a {@link Fault} under the stack's name, as in a rule: the consequence inserts
     * it in place of the decision, and where the justification can fault, a fault rule with the rule's own left-hand
     * side, its control-fact patterns included, inserts it ({@link #conditionFaultRule}). A default a higher one
     * displaces loses its match, and so does every default of an exception group below it ({@link #chainRules}), and
     * the fault it raised goes with it, so a fault counts only where its default is evaluated at the fixpoint (PLAN §9,
     * 2026-10-09).
     */
    private List<RulePlan> ownRule(StackBuild b, Node k, RuleOrigin.Role role, String ruleName,
                                   Supplier<List<LhsItem>> guard, List<Consequence.StackFact> facts) {
        // the rules built here lower the default, and so do the contributions of the folds registered meanwhile, those
        // its justification's fault rule reads
        lowering = k.d();
        try {
            List<RulePlan> out = lowerOwnRule(b, k, role, ruleName, guard, facts);
            for (RulePlan r : out) defaultOfRule.put(r.name(), k.d());
            return out;
        } finally {
            lowering = null;
        }
    }

    /** The rules of {@link #ownRule}, built while {@link #lowering} is the default: the rule of the default's own
     *  decision in the role {@code role}, the default's own or its fall-back, and the fault rule of its justification. */
    private List<RulePlan> lowerOwnRule(StackBuild b, Node k, RuleOrigin.Role role, String ruleName,
                                        Supplier<List<LhsItem>> guard, List<Consequence.StackFact> facts) {
        Default d = k.d();
        Supplier<Reads> fresh = () -> {
            Reads l = new Reads(k.levelOf());
            l.addSubjectAlpha(concreteType(b.javaClass));
            // the input on the target, read through the getter of the type that declares it, as the bean holds it
            if (d.initialisation())
                l.addSubjectAlpha(new Alpha(id("doel-leeg-in-de-invoer"), List.of(new Alpha.FieldTest(List.of(),
                        b.member.name(), Binding.ReadForm.VALUE, Alpha.Comparator.LEEG, null, b.member.javaClass()))));
            return l.valid(k.validity());
        };
        Reads reads = fresh.get();
        Set<Integer> read = reads.follow();
        beginReading();
        Term testTerm = d.condition() == null ? null : conditionTerm(d.condition(), reads);
        boolean canFail = reads.canFail();
        reads.stop();
        String name = b.name;
        List<LhsItem> items, control;
        Consequence decision;
        Term value = null;
        Noted noted;
        if (b.characteristic) {
            noted = endReading();
            control = guard.get();
            items = reads.plan(conditionRole(canFail), testTerm, read, d.condition(), control);
            decision = new Consequence.Decision(SUBJECT, constant(d.decision()), List.of(), reads.size(), false, null, facts,
                    Consequence.Insertion.LOGICAL);
        } else {
            Set<Integer> use = reads.follow();
            value = restated(value(d.consequence(), reads), eff.unitOf(d.consequence()),
                    b.member.datatype().unitOf(), "een §4 schrijfgrens naar het doel");
            reads.stop();
            noted = endReading();
            control = guard.get();
            items = reads.plan(conditionRole(canFail), testTerm, read, d.condition(), control);
            decision = new Consequence.Decision(SUBJECT, value, List.copyOf(use), reads.size(), true,
                    new Consequence.FaultName(name, name), facts, Consequence.Insertion.LOGICAL);
        }
        b.readings.add(reads);
        b.defaultOf.put(reads, d);
        List<RulePlan> out = new ArrayList<>();
        out.add(as(role, reads.rulePlan(ruleName, items, decision)));
        // the control facts stay: a default a higher one displaced is not at its turn (RUNTIME-SEMANTICS ch. 7.1, 7.9).
        // A default without a justification, and an ordinary rule without a condition that the fold of ch. 7.10 took in
        // (its justification the fold's vacuous one, which stands nowhere in the document), has no condition to read:
        // on an attribute it has the reading of its consequence, which holds where it is at its turn; a characteristic
        // default without one has nothing to read
        ResolvedRule madeFrom = stacks.ruleOf(d);
        boolean unconditional = testTerm == null || (madeFrom != null && madeFrom.condition() == null);
        if (noted != null && !(unconditional && b.characteristic))
            readingRule(ruleName, pointerOf(d), reads, reads.readingPlan(control, false), unconditional ? null : testTerm,
                    value, unconditional ? null : d.condition(), b.characteristic ? null : d.consequence(), noted,
                    k.validity(), !control.isEmpty() || d.initialisation());
        if (canFail) out.add(conditionFaultRule(name, ruleName, fresh, d.condition(), l -> guard.get()));
        return out;
    }

    // ---------------- the readings of a build with readings ----------------

    /** What the analysis noted while the condition and the value of one form's own rule compiled ({@link #note}): the
     *  term of each node and the unit of each expression node, by identity of the node. */
    private record Noted(Map<Object, Term> terms, Map<Object, Unit> units, Map<Object, Derived> derived) {}

    /**
     * The reading of a node that no term of the rule computes, and that follows from the outcomes of {@code inputs},
     * terms the rule evaluates, by {@code compute}: the part of a timeline in which a gedurende clause of a temporal
     * total holds (RUNTIME-SEMANTICS ch. 3.6, 5.12), where the clip computes only the timeline it leaves. The node is
     * reached where every input is reached without a fault; it evaluates nothing of the session again.
     */
    private record Derived(List<Term> inputs, Function<List<Object>, Object> compute) {}

    /** Starts noting the terms of the nodes the condition and the value of a form's own rule compile, in a build with
     *  readings; nothing in a build without. */
    private void beginReading() {
        if (positions == null) return;
        if (noted != null) throw new IllegalStateException("de lezing van een vorm begint terwijl die van een andere loopt");
        noted = new IdentityHashMap<>();
        notedUnits = new IdentityHashMap<>();
        notedDerived = new IdentityHashMap<>();
    }

    /** What {@link #beginReading} noted since it started, and the end of it; null in a build without readings. */
    private Noted endReading() {
        if (positions == null) return null;
        Noted n = new Noted(noted, notedUnits, notedDerived);
        noted = null;
        notedUnits = null;
        notedDerived = null;
        return n;
    }

    /** Ends the reading {@link #beginReading} started for a form whose lowering {@link #meet} refused part-way, without
     *  reading what it noted, and drops the reading rules past the first {@code keep}, those the form planned before
     *  the refusal (a stack plans one per default): the refused form has no rule, and so no reading rule. */
    private void abandonReading(int keep) {
        noted = null;
        notedUnits = null;
        notedDerived = null;
        readingRules.subList(keep, readingRules.size()).clear();
    }

    /** One node of a form's reading: its number, its kind as the wire spells it, the term that computes it in the
     *  form's own rule, its unit, and for the node of a fold over a set the fold, whose contributions are its
     *  contributors, with the quantifier fold the node is where it is one. */
    private record Site(int node, String kind, Term term, Unit unit, Fold fold, Condition.QuantifierFold quantifier,
                        Derived derived) {}

    /**
     * The reading rule of one form in a build with readings, with what {@link #readingsOf} needs to read its facts:
     * the name of the form's own rule, which it reads ({@link #originOf}), the form's pointer, its validity, whether the
     * form has a guard of its turn (control facts of a defaults stack, the guard of an initialisation, the 1:1 guard of
     * an object creation), the planned condition and value of the form's own rule, the very terms its test and its
     * consequence evaluate, the condition null for a form without one, and the nodes of the reading in their order, a
     * node evaluated per member of a fold left out.
     */
    private record ReadingRule(RulePlan plan, String ownRule, String pointer, Validity validity, boolean guarded,
                               Term condition, Term value, List<Site> sites) {

        /** The subject pattern of the reading rule. */
        Subject subject() {
            for (LhsItem i : plan.leftSide()) if (i instanceof Subject o) return o;
            throw new IllegalStateException("de leesregel " + plan.name() + " heeft geen onderwerppatroon");
        }

        /** Whether the form runs on {@code o}: an instance of its subject's bean class that the class constraints of
         *  the subject pattern admit, the concrete type of a stack and the types a stack takes a rule from. Every other
         *  constraint of that pattern is a guard of the form's turn. */
        boolean inRange(Object o) {
            Subject s = subject();
            if (!s.beanClass().isInstance(o)) return false;
            for (Alpha a : s.alpha())
                for (Alpha.FieldTest t : a.tests()) {
                    if (t.field() != null || !t.path().isEmpty()) continue;
                    if (t.op() == Alpha.Comparator.CLASS_IS && o.getClass() != t.value()) return false;
                    if (t.op() == Alpha.Comparator.CLASS_NOT_IN && ((Collection<?>) t.value()).contains(o.getClass()))
                        return false;
                }
            return true;
        }
    }

    /**
     * Plans the reading rule of the form {@code pointer}, whose own rule is {@code ruleName} and reads through
     * {@code reads} (card fixpoint-readings): its left-hand side is that rule's with the planned items {@code items}
     * after the subject pattern, the rule's own without its test, and its consequence inserts the {@link Reading} of
     * the subject; beside it the terms {@code condition} and {@code value} of the rule's test and consequence, and the
     * nodes of {@code irCondition} and {@code irValue} numbered in the pre-order of the document, each with the term the
     * analysis noted for it ({@code noted}). A form without a condition, {@code condition} and {@code irCondition} null,
     * numbers the nodes of its value from 0. A node without a term below the node of a fold over a set is evaluated per
     * member of the fold; any other node without one is a place of the analysis that does not note its node, a fault
     * of this class, refused loudly.
     */
    private void readingRule(String ruleName, String pointer, Reads reads, List<LhsItem> items, Term condition,
                             Term value, Condition irCondition, Expr irValue, Noted noted, Validity validity,
                             boolean guarded) {
        if (pointer == null)
            throw new IllegalStateException("de vorm van de regel " + ruleName + " staat niet in het document dat de lader"
                    + " las: een lezing noemt haar vorm naar haar plaats");
        RulePlan plan = as(RuleOrigin.Role.READING, reads.readingRulePlan(ruleName + ": lezing", pointer, items));
        if ((condition == null) != (irCondition == null))
            throw new IllegalStateException("de leesregel van " + pointer + " heeft een voorwaarde zonder haar knopen, of"
                    + " knopen zonder haar voorwaarde");
        List<Object> nodes = new ArrayList<>(irCondition == null ? List.of() : positions.subtree(irCondition));
        if (irValue != null) nodes.addAll(positions.subtree(irValue));
        List<String> foldsAt = new ArrayList<>();
        for (Object n : nodes)
            if (folds.containsKey(n) && noted.terms().containsKey(n)) foldsAt.add(positions.of(n).pointer() + "/");
        List<Site> sites = new ArrayList<>();
        for (int i = 0; i < nodes.size(); i++) {
            Object n = nodes.get(i);
            dev.uchar.regelspraak.loader.NodePositions.Position at = positions.of(n);
            Term t = noted.terms().get(n);
            Derived derived = noted.derived().get(n);
            if (t != null && derived != null)
                throw new IllegalStateException("de lezing van " + pointer + ": knoop " + i + " (" + at.kind() + " op "
                        + at.pointer() + ") heeft een term en een afgeleide lezing");
            if (t == null && derived == null) {
                if (foldsAt.stream().noneMatch(at.pointer()::startsWith))
                    throw new IllegalStateException("de lezing van " + pointer + ": de analyse noteerde geen term voor"
                            + " knoop " + i + " (" + at.kind() + " op " + at.pointer() + ")");
                continue;
            }
            Map<ObjectType, Fold> fold = folds.get(n);
            sites.add(new Site(i, at.kind(), t, noted.units().get(n), fold == null ? null : fold.get(reads.subject),
                    n instanceof Condition.QuantifierFold q ? q : null, derived));
        }
        readingRules.add(new ReadingRule(plan, ruleName, pointer, validity, guarded, condition, value, sites));
    }

    /** The variables the top level of {@code lhs} declares, which a consequence can name: the variable of each pattern
     *  and those its bindings bind, and the results of each accumulate; not those a negation or an existence
     *  declares, which live only inside it. */
    private static Set<String> declaredVariables(List<LhsItem> lhs) {
        Set<String> out = new HashSet<>();
        for (LhsItem item : lhs)
            switch (item) {
                case Subject o -> {
                    out.add(o.name());
                    for (Binding b : o.bindings()) out.add(b.target());
                }
                case FactPattern p -> {
                    out.add(p.name());
                    for (Binding b : p.bindings()) out.add(b.target());
                }
                case Accumulation a -> {
                    for (Accumulation.Aggregation f : a.aggregations()) out.add(f.target());
                }
                case Not n -> { }
                case Exists e -> { }
                case TestItem t -> { }
            }
        return out;
    }

    /**
     * The readings of the conditions at the fixpoint of {@code session}, a session of a build with readings: for every
     * form with a condition, or without one that computes a value, and every object of the session the form runs on, one
     * record. Where the form's reading rule stood at the fixpoint, its {@link Reading} carries the values the form's own
     * rule bound there, and the record is the evaluation of the rule's condition over them, and of its value where the
     * condition holds, by the recording rendering ({@link RecordingRendering}), which deposits nothing; a form without a
     * condition holds there, and its record is the evaluation of its value; else the form was not at its turn there,
     * {@link ConditionReading.Status#DISPLACED}, or its validity does not admit the session's evaluation date,
     * {@link ConditionReading.Status#OUTSIDE_VALIDITY}, or the session has no date and the form a dated validity,
     * {@link ConditionReading.Status#FAULTS}, each without nodes. A form without a guard of its turn inside its
     * validity has a reading on every object it runs on, and an object without one is a broken contract, refused
     * loudly; so is a reading on an object no form runs on.
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
            throw new IllegalStateException("deze build heeft geen leesregels: bouw met lezingen"
                    + " (DroolsBackend.build met readings) om de lezingen van de voorwaarden te lezen");
        requireFixpoint(session);
        Map<String, Map<Object, Reading>> facts = new HashMap<>();
        for (Object f : session.getObjects(new ClassObjectFilter(Reading.class))) {
            Reading r = (Reading) f;
            Reading other = facts.computeIfAbsent(r.form(), k -> new IdentityHashMap<>()).put(r.subject(), r);
            if (other != null)
                throw new IllegalStateException("twee lezingen van " + r.form() + " op " + FactNames.object(r.subject())
                        + ": " + other.values() + " en " + r.values());
        }
        LocalDate date = null;
        for (Object f : session.getObjects(new ClassObjectFilter(EvaluationDate.class))) date = ((EvaluationDate) f).date();
        Map<String, Map<Object, List<Contribution>>> contributions = null;
        List<ConditionReading> out = new ArrayList<>();
        for (ReadingRule r : readingRules) {
            Map<Object, Reading> ofForm = facts.getOrDefault(r.pointer(), Map.of());
            for (Object o : session.getObjects(new ClassObjectFilter(r.subject().beanClass()))) {
                if (!r.inRange(o)) continue;
                Reading reading = ofForm.isEmpty() ? null : ofForm.remove(o);
                if (reading == null) {
                    out.add(new ConditionReading(o, r.pointer(), withoutReading(r, o, date), List.of()));
                    continue;
                }
                if (contributions == null && r.sites().stream().anyMatch(x -> x.fold() != null))
                    contributions = contributions(session);
                out.add(reading(r, o, reading, contributions));
            }
        }
        facts.forEach((form, rest) -> {
            if (!rest.isEmpty())
                throw new IllegalStateException("lezingen van " + form + " op "
                        + rest.keySet().stream().map(FactNames::object).toList() + ", en daar loopt geen vorm van deze"
                        + " build");
        });
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
        throw new IllegalStateException("de leesregel van " + r.pointer() + " stond aan het fixpunt niet op "
                + FactNames.object(o) + ", waarop de vorm loopt zonder wacht op haar beurt en binnen haar geldigheid");
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

    /** The reading of {@code r} on {@code o} over the values of {@code reading}. */
    private ConditionReading reading(ReadingRule r, Object o, Reading reading,
                                     Map<String, Map<Object, List<Contribution>>> contributions) {
        Object[] w = reading.values().toArray();
        List<dev.uchar.regelspraak.transpiler.tms.plan.Read> slots = r.plan().readings().slots();
        if (w.length != slots.size())
            throw new IllegalStateException("de lezing van " + r.pointer() + " draagt " + w.length + " waarden, en de regel"
                    + " heeft " + slots.size() + " slots");
        // a fresh probe in every probe slot, which nobody reads: a reading deposits nothing
        for (dev.uchar.regelspraak.transpiler.tms.plan.Read l : slots)
            if (l.kind() == ReadKind.DEPOSIT_FLAG) w[l.slot()] = ComputeHelpers.depositFlag();
        RecordingRendering rec = new RecordingRendering();
        ConditionReading.Status status;
        // a form without a condition whose reading rule stood is at its turn, and holds there
        if (r.condition() == null) status = ConditionReading.Status.HOLDS;
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
                nodes.add(derivedReading(s, rec));
                continue;
            }
            RecordingRendering.Outcome oc = rec.outcome(s.term());
            List<ConditionReading.Contributor> contributors = oc == null || s.fold() == null ? List.of()
                    : contributors(s, contributions.getOrDefault(s.fold().key(), Map.of()).getOrDefault(o, List.of()),
                            rec);
            nodes.add(new ConditionReading.NodeReading(s.node(), s.kind(), oc != null,
                    oc == null ? null : RecordingRendering.given(oc.value()), s.unit(),
                    oc == null ? null : oc.fault(), contributors));
        }
        return new ConditionReading(o, r.pointer(), status, nodes);
    }

    /** The reading of the node of {@code s}, whose reading follows from the outcomes of its inputs in the evaluation
     *  {@code rec} ({@link Derived}): reached where every input was reached without a fault. */
    private static ConditionReading.NodeReading derivedReading(Site s, RecordingRendering rec) {
        List<Object> values = new ArrayList<>();
        for (Term t : s.derived().inputs()) {
            RecordingRendering.Outcome o = rec.outcome(t);
            if (o == null || o.fault() != null)
                return new ConditionReading.NodeReading(s.node(), s.kind(), false, null, s.unit(), null, List.of());
            values.add(o.value());
        }
        return new ConditionReading.NodeReading(s.node(), s.kind(), true,
                RecordingRendering.given(s.derived().compute().apply(values)), s.unit(), null, List.of());
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

    // ---------------- values ----------------

    /** A value as the analysis plans it ({@link Term}): a tree of calls over the values the rule binds, indexed as
     *  {@link Reads} numbers them, which {@link ClosureRendering} renders into the function a consequence or a test
     *  calls. Every expression node is named here, so a node the IR records gain later fails to compile until it is
     *  decided whether this branch computes it. */
    private Term value(Expr e, Reads reads) {
        // IR-SPEC ch. 4.1a: an anchored node is read on the subject of the form, as in the form's own condition or value
        if (anchored.contains(e) && !reads.atForm) return reads.atForm(e, () -> value(e, reads));
        Term planned = switch (e) {
            case Expr.Literal l -> literal(l);
            case Expr.AttributeReference a -> new Term.SlotValue(attributeRead(a, reads), source(a.source()));
            case Expr.ParameterRef p -> new Term.SlotValue(reads.parameter(p.parameter()), source(p.source()));
            case Expr.Binary b -> binary(b, reads);
            case Expr.TextComposition t -> textComposition(t, reads);
            case Expr.FunctionCall f -> function(f, reads);
            // IR-SPEC ch. 4.3: a union denotes instances and stands only as the argument of a count, which reads it in
            // vouw; here it stands where a value is read, which the verifier refuses (WF-110 (a)) and the reference
            // interpreter faults on; both branches refuse it in this sentence, which the destructive DRL-text lowering
            // gave until card drl-teksttak-op-truth-maintenance
            case Expr.InstanceSetUnion u -> throw new Refusal(DroolsCode.INSTANCE_UNION_IN_VALUE.reason("snippet", snippet(u.source())));
            case Expr.Rounding a -> rounding(a, reads);
            case Expr.Clamp b -> clamp(b, reads);
            case Expr.DimensionCellRead c -> dimensionCell(c, reads);
            case Expr.VariableRef v -> throw new Refusal(DroolsCode.UNBOUND_VARIABLE_REFERENCE.reason("snippet", snippet(v.source())));
            case Expr.Let l -> let(l, reads);
            case Expr.BinderRef b -> binderRead(b, reads);
            case Expr.IfThenElse i -> choice(i, reads);
            case Expr.Constant c -> contextValue(c, reads);
            // RUNTIME-SEMANTICS ch. 5.7 and WF-044: the marker fails the rule's evaluation where the evaluation reaches it,
            // and only there, so a marker in a branch that is not taken plays
            case Expr.Unresolved o -> {
                reads.markFail();
                yield new Term.Fail(ComputeFault.UNRESOLVED, source(o.source()));
            }
        };
        note(e, planned);
        return planned;
    }

    /** Notes that {@code t} computes the node {@code node} of the IR, a condition or an expression, where the condition
     *  and the value of a form's own rule compile in a build with readings ({@link #noted}); nothing at any other moment.
     *  The term is the one the evaluation of the rule reaches for the node, before a restatement or a normalisation
     *  that the analysis puts around it at its use. */
    private void note(Object node, Term t) {
        if (noted == null || memberwise > 0) return;
        noted.put(node, t);
        if (node instanceof Expr e) notedUnits.put(node, eff.unitOf(e));
    }

    /** Notes that the reading of {@code node}, a condition, follows from the outcomes of {@code inputs} by
     *  {@code compute} ({@link Derived}), as {@link #note} notes a term; nothing outside a build with readings. */
    private void noteDerived(Object node, List<Term> inputs, Function<List<Object>, Object> compute) {
        if (noted == null || memberwise > 0) return;
        notedDerived.put(node, new Derived(List.copyOf(inputs), compute));
    }

    /** A constant of the analysis, which an argument of a function of the plan takes ({@link Term.Const}). */
    private static Term.Const constant(Object value) {
        return new Term.Const(value, null, null);
    }

    /** A test a form composes itself ({@link TestItem.Role#VORM}) of the checks {@code members}: their conjunction,
     *  read in order up to the first that does not hold ({@link Term.Compound} under all). No node of the IR
     *  stands for it, so it carries no source fragment. */
    private static Term all(List<Term> members) {
        return new Term.Compound(Condition.Quantifier.ALLE, 0, members, null);
    }

    /** The check that the value bound at slot {@code slot} is gevuld, in a test a form composes itself. */
    private static Term filled(int slot) {
        return new Term.Call(StaticFunction.EMPTY_TEST, List.of(new Term.SlotValue(slot, null), constant(false)), null);
    }

    /** The check that the value bound at slot {@code slot} is ({@code desired}) or is not the asOfDate-bucket scalar
     *  of a timeline ({@link BucketScalar}), in a test a form composes itself. */
    private static Term bucketScalar(int slot, boolean desired) {
        return new Term.Call(StaticFunction.BUCKET_SCALAR, List.of(new Term.SlotValue(slot, null), constant(desired)), null);
    }

    /** The source fragment of an IR node, which a term carries; null where the IR has none. */
    private static String source(dev.uchar.regelspraak.ir.Source source) {
        return source == null ? null : source.snippet();
    }

    private static String snippet(dev.uchar.regelspraak.ir.Source source) {
        return source == null || source.snippet() == null ? "zonder bron" : source.snippet();
    }

    private static String snippet(Condition c) {
        return snippet(c.source());
    }

    /** The source fragment of {@code node}, a condition or an expression of the IR, or the node as text otherwise. */
    private static String snippetOf(Object node) {
        if (node instanceof Condition c) return snippet(c);
        if (node instanceof Expr e) return snippet(e.source());
        return String.valueOf(node);
    }

    /** A literal: a number, and a percentage, a number in the unit %, exact where its
     *  quotient does not terminate, a date, a boolean, and a text or an enumeration label as its string. */
    private static Term literal(Expr.Literal l) {
        Datatype t = l.type();
        String source = source(l.source());
        if (t instanceof Datatype.Numeric || t instanceof Datatype.Percentage) {
            BigDecimal[] r = RationalParser.of(l.valueText());
            if (r == null) throw new Refusal(DroolsCode.NUMERIC_LITERAL_UNREADABLE.reason("valueText", l.valueText()));
            Breuk b = Breuk.van(r[0]).delen(Breuk.van(r[1]));
            Object v = b.isTerminerend() ? b.naarDecimal() : b;
            return new Term.Const(v, t, source);
        }
        if (t instanceof Datatype.BooleanType) {
            Boolean v = Literals.parseBoolean(l.valueText());
            return new Term.Const(v, t, source);
        }
        if (t instanceof Datatype.DateInDays) {
            LocalDate v = Literals.parseDate(l.valueText());
            if (v == null) throw new Refusal(DroolsCode.DATE_LITERAL_UNREADABLE.reason("valueText", l.valueText()));
            return new Term.Const(v, t, source);
        }
        if (t instanceof Datatype.DateTime) {
            LocalDateTime v = Literals.parseDateTime(l.valueText());
            if (v == null) throw new Refusal(DroolsCode.DATETIME_LITERAL_UNREADABLE.reason("valueText", l.valueText()));
            return new Term.Const(v, t, source);
        }
        String label = Literals.textOrEnumLabel(l);
        if (label == null) throw new Refusal(DroolsCode.LITERAL_DATATYPE_NOT_CARRIED.reason("datatype", t));
        return new Term.Const(label, t, source);
    }

    /** The arithmetic operators on numbers, a percentage being a number in the unit % (RUNTIME-SEMANTICS ch. 2.2, 2.6),
     *  through the runtime's exact helpers and with their leeg tables (ch. 1.2); a date plus or minus a duration through
     *  {@link #dateCalculation}, a power through {@link #power}. A sum or a difference restates its right operand into the
     *  left one's unit (ch. 2.6); a product, a quotient and a percentage of a value do not. A division faults on a numeric
     *  zero (ch. 1.4, point 4), and one of a filled dividend by a leeg divisor deposits where the evaluation reaches it and
     *  its operands qualify ({@link #depositSite}, {@link DepositFlag}). The operands are evaluated left before right
     *  (ch. 5.11). */
    private Term binary(Expr.Binary b, Reads reads) {
        Datatype lt = eff.typeOf(b.left()), rt = eff.typeOf(b.right());
        if (lt.isDate() || rt.isDate()) return dateCalculation(b, reads);
        Expr.BinOp op = b.op();
        if (op == Expr.BinOp.TOT_DE_MACHT) return power(b, reads);
        boolean division = op == Expr.BinOp.GEDEELD_DOOR || op == Expr.BinOp.GEDEELD_DOOR_ABS;
        // a condition that holds a division carries the rule that inserts its fault (voorwaardefoutregel)
        if (division) reads.markFail();
        Term l = value(b.left(), reads), right = value(b.right(), reads);
        Term r = op == Expr.BinOp.PLUS || op == Expr.BinOp.MIN || op == Expr.BinOp.VERMINDERD_MET
                ? restated(right, eff.unitOf(b.right()), eff.unitOf(b.left()), "een additieve operandgrens")
                : right;
        int depositFlag = division && depositSite(b) ? reads.depositFlag() : -1;
        String source = source(b.source());
        return switch (op) {
            case PLUS -> new Term.Call(StaticFunction.RS_PLUS, List.of(l, r), source);
            case MIN -> new Term.Call(StaticFunction.RS_MIN, List.of(l, r), source);
            case VERMINDERD_MET -> new Term.Call(StaticFunction.RS_REDUCED, List.of(l, r), source);
            case MAAL -> new Term.Call(StaticFunction.RS_TIMES, List.of(l, r), source);
            case PERCENTAGE_VAN -> new Term.Call(StaticFunction.RS_PERCENTAGE_OF, List.of(l, r), source);
            case GEDEELD_DOOR -> new Term.Division(StaticFunction.DIVIDED, l, r, depositFlag, source);
            case GEDEELD_DOOR_ABS -> new Term.Division(StaticFunction.DIVIDED_ABS, l, r, depositFlag, source);
            case TOT_DE_MACHT -> throw new IllegalStateException("een macht gaat door macht()");
        };
    }

    /** A power with a whole exponent of at least 0 written as a literal, exact (RUNTIME-SEMANTICS ch. 2.2), a leeg base
     *  leeg (ch. 1.2), through the runtime's {@code rsMacht}. Any other exponent is refused: a
     *  negative or fractional literal, or a value read while the session fires, reaches the real power, which ch. 2.1
     *  leaves OPEN, where the reference interpreter answers an exact root or faults and the destructive lowering
     *  computed a binary double. */
    private Term power(Expr.Binary b, Reads reads) {
        Integer n = b.right() instanceof Expr.Literal lit && lit.type().isNumberLike() ? wholeExponent(lit.valueText()) : null;
        if (n == null)
            throw new Refusal(DroolsCode.POWER_NON_INTEGER_EXPONENT.reason("snippet", snippet(b.source())));
        Term base = value(b.left(), reads);
        Term exponent = constant(n);
        note(b.right(), exponent);
        return new Term.Call(StaticFunction.RS_POWER, List.of(base, exponent), source(b.source()));
    }

    /** The whole exponent of at least 0 the lexeme writes, or null. */
    private static Integer wholeExponent(String lexeme) {
        BigDecimal[] r = RationalParser.of(lexeme);
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
    private Term dateCalculation(Expr.Binary b, Reads reads) {
        Expr.BinOp op = b.op();
        if (op != Expr.BinOp.PLUS && op != Expr.BinOp.MIN && op != Expr.BinOp.VERMINDERD_MET)
            throw new Refusal(DroolsCode.DATE_BINARY_OPERATOR_UNSUPPORTED.reason("operator", Spelling.BINARY_OP.spelling(op), "snippet", snippet(b.source())));
        boolean dateLeft = eff.typeOf(b.left()).isDate();
        Expr dateExpr = dateLeft ? b.left() : b.right();
        Expr durationExpr = dateLeft ? b.right() : b.left();
        boolean adding = op == Expr.BinOp.PLUS;
        if (eff.typeOf(durationExpr).isDate() || (!adding && !dateLeft))
            throw new Refusal(DroolsCode.DATE_ARITHMETIC_INVALID_OPERANDS.reason("snippet", snippet(b.source())));
        // ch. 3.8 puts the calendar table first, as TimeCalculation.dayKindForUnit looks a spelling up for every branch
        String dayKind = TimeCalculation.dayKindForUnit(model, eff.typeOf(durationExpr).unitOf());
        if (dayKind != null) return dayKindStep(b, dateExpr, durationExpr, adding, dayKind, reads);
        String unit = TimeCalculation.chronoUnit(eff.typeOf(durationExpr));
        if (unit == null) throw new Refusal(DroolsCode.DURATION_WITHOUT_TIME_UNIT.reason("snippet", snippet(b.source())));
        boolean clock = switch (unit) { case "HOURS", "MINUTES", "SECONDS", "MILLIS" -> true; default -> false; };
        if (clock && !(eff.typeOf(dateExpr) instanceof Datatype.DateTime))
            throw new Refusal(DroolsCode.CLOCK_UNIT_ON_DATE.reason("snippet", snippet(b.source())));
        reads.markFail();
        Term d = value(dateExpr, reads), n = value(durationExpr, reads);
        return new Term.Call(StaticFunction.DATE_STEP, List.of(d, n, constant(unit), constant(adding)),
                source(b.source()));
    }

    /**
     * A date or a date-time plus or minus a count of days of the day kind {@code dayKind} (RUNTIME-SEMANTICS ch. 3.8,
     * §6.11), as the runtime walks it for the other branches: from the date, which never counts, one calendar day at a
     * time to the Nth day the definition admits ({@code rsDatumPlusDagsoort}); on a date-time over its date part, the
     * clock time riding through ({@code rsDatumTijdPlusDagsoort}). A count of 0 or leeg gives the date unchanged, a leeg
     * date leeg. The definition reads nothing of working memory (WF-031), so its predicate is a constant of the document
     * ({@link #dayKindPredicate}), and the reads the step depends on are the date and the count, bound as any value of
     * the rule. A walk that passes the reach of ch. 3.8 without a day of the kind is an evaluation fault in the sentence
     * of the reference interpreter ({@link ComputeHelpers#dayKindStep}), a {@link Fault} that counts only where it stands
     * at the fixpoint. The date is evaluated before the count (ch. 5.11).
     */
    private Term dayKindStep(Expr.Binary b, Expr dateExpr, Expr durationExpr, boolean adding, String dayKind,
                              Reads reads) {
        java.util.function.Predicate<LocalDate> datePredicate =
                dayKindPredicate(dayKind, Msg.drools("PART.desc.day-kind-duration", "snippet", snippet(b.source())));
        boolean dateTime = eff.typeOf(dateExpr) instanceof Datatype.DateTime;
        reads.markFail();
        Term d = value(dateExpr, reads), n = value(durationExpr, reads);
        return new Term.Call(dateTime ? StaticFunction.DATE_TIME_DAY_KIND_STEP : StaticFunction.DAY_KIND_STEP,
                List.of(d, n, new Term.DayKind(dayKind, datePredicate, null), constant(adding)), source(b.source()));
    }

    /**
     * The predicate of the day kind {@code name} over a calendar day, from its definition (IR-SPEC ch. 3.7), compiled by
     * the backend's day-kind compiler ({@code core.DayKinds}) the first time a form needs it and the same object for
     * every form after: the bound day, its date parts, the first Easter day, literals and other day kinds on the bound
     * day. The form {@code description} that steps or counts in the day kind is refused by name where the definition does not
     * compile ({@link #dayKindRefusal}); a definition that no form uses is never compiled, so it refuses nothing.
     */
    private java.util.function.Predicate<LocalDate> dayKindPredicate(String name, Object description) {
        java.util.function.Predicate<LocalDate> p = dayKinds.datePredicate(name);
        if (p == null) {
            Reason why = dayKindRefusal(name, new LinkedHashSet<>());
            throw new Refusal(why.framed("described", "description", description));
        }
        return p;
    }

    /**
     * Why the day kind {@code name} has no predicate here, found in its definition and in the definitions it refers to
     * ({@code visited} holds those already looked at, the reference graph being acyclic by WF-085): no definition; a
     * parameter, which WF-031 admits and the reference interpreter reads, and which this branch does not read, since
     * the day-kind compiler binds nothing; a day-kind test on another day than the bound day, which no executor carries
     * (RUNTIME-SEMANTICS ch. 3.8, red case {@code day-kind-reference-other-day}); and else a shape of its condition
     * outside what the compiler carries.
     */
    private Reason dayKindRefusal(String name, Set<String> visited) {
        ResolvedDayKindDefinition def = null;
        for (ResolvedDayKindDefinition d : model.resolvedDayKindDefinitions)
            if (d.name() != null && d.name().equalsIgnoreCase(name)) def = d;
        if (def == null)
            return DroolsCode.DAY_KIND_WITHOUT_DEFINITION.reason("dayKind", name);
        if (!visited.add(name.toLowerCase())) return null;
        String[] parameter = {null};
        Condition.containsOperand(def.condition(), e -> e instanceof Expr.ParameterRef pr
                && (parameter[0] = pr.parameter() == null ? "(onopgelost)" : pr.parameter().name()) != null);
        if (parameter[0] != null)
            return DroolsCode.DAY_KIND_READS_PARAMETER.reason("dayKind", name, "parameter", parameter[0]);
        List<Condition.Predicate> tests = new ArrayList<>();
        dayKindTests(def.condition(), tests);
        for (Condition.Predicate t : tests) {
            if (!(t.operand() == null || t.operand() instanceof Expr.Constant c && "de dag".equalsIgnoreCase(c.name())))
                return DroolsCode.DAY_KIND_TESTS_OTHER.reason("dayKind", name, "testedDayKind", t.dayKind(), "snippet", snippet(t));
            if (t.dayKind() != null && dayKinds.datePredicate(t.dayKind()) == null) {
                Reason nested = dayKindRefusal(t.dayKind(), visited);
                if (nested != null) return nested;
            }
        }
        return DroolsCode.DAY_KIND_CONDITION_UNSUPPORTED.reason("dayKind", name, "snippet", snippet(def.condition()));
    }

    /** The day-kind tests in {@code c}, in wire order, through its composites. */
    private static void dayKindTests(Condition c, List<Condition.Predicate> out) {
        if (c instanceof Condition.Predicate p && p.kind() == Condition.PredicateKind.DAGSOORT) out.add(p);
        else if (c instanceof Condition.Compound s) for (Condition d : s.components()) dayKindTests(d, out);
    }

    /** {@code t} restated from unit {@code van} into unit {@code destination} (RUNTIME-SEMANTICS ch. 2.6): times the exact
     *  canonical factors {@code num ÷ den} as rationals where both are known, so a restatement that does not terminate
     *  stays exact until a collapse point (ch. 2.1), as the reference interpreter restates; times the single ratio of the
     *  two units otherwise, a rate whose factors are not both known; unchanged where no restatement is due, a unitless
     *  side adopting the other's unit. A leeg value stays leeg and a value that is not a number passes through
     *  ({@link Term.Restate}). Two units that do not convert fault while the model is built ({@link UnitConflict}). */
    private static Term restated(Term t, Unit van, Unit destination, String site) {
        UnitConflict.requireRescalable(van, destination, site);
        if (van == null || destination == null || !van.convertibleTo(destination) || !van.requiredConversion(destination)) return t;
        BigDecimal[] exact = van.exactRestateFactors(destination);
        if (exact != null)
            return new Term.Restate(StaticFunction.RESTATE_EXACT, t, van, destination, exact[0], exact[1], t.source());
        return new Term.Restate(StaticFunction.RESTATE, t, van, destination, van.conversionFactorTo(destination), null, t.source());
    }

    // ---------------- the forms of a value beside the arithmetic ----------------

    /** A rounding (RUNTIME-SEMANTICS ch. 2.5) of its base, a leeg base leeg (ch. 1.2), through {@code rsAfronding}, which
     *  rounds an exact rational on its true value; a square root as the base is the root under its mandatory rounding
     *  ({@link #squareRootValue}). */
    private Term rounding(Expr.Rounding a, Reads reads) {
        java.math.RoundingMode mode = roundingMode(a.mode());
        int decimals = a.decimals();
        Term base;
        if (a.base() instanceof Expr.FunctionCall f && f.kind() == Expr.FunctionKind.WORTEL) {
            base = squareRootValue(f, reads);
            note(f, base);
        } else base = value(a.base(), reads);
        return new Term.Call(StaticFunction.RS_ROUNDING, List.of(base, constant(decimals), constant(mode)),
                source(a.source()));
    }

    private static java.math.RoundingMode roundingMode(Expr.RoundingMode m) {
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
    private Term squareRootValue(Expr.FunctionCall f, Reads reads) {
        if (f.arguments().size() != 1)
            throw new Refusal(DroolsCode.SQUARE_ROOT_ARGUMENT_COUNT.reason("argumentCount", f.arguments().size(), "snippet", snippet(f.source())));
        Term radicand = value(f.arguments().get(0), reads);
        int depositFlag = depositSite(f) ? reads.depositFlag() : -1;
        return new Term.SquareRoot(StaticFunction.SQUARE_ROOT, radicand, depositFlag, source(f.source()));
    }

    /** A clamp (RUNTIME-SEMANTICS ch. 2.7): its base, then its minimum, then its maximum, in the order of ch. 5.11 and
     *  of the bounds' application, each bound restated into the base's unit (ch. 2.6); a leeg base leeg, an absent or
     *  leeg bound no bound ({@code rsBegrens}). */
    private Term clamp(Expr.Clamp g, Reads reads) {
        Term base = value(g.base(), reads);
        Unit unit = eff.unitOf(g.base());
        Term minimum = g.minimum() == null ? constant(null)
                : restated(value(g.minimum(), reads), eff.unitOf(g.minimum()), unit, "een begrenzing (minimum)");
        Term maximum = g.maximum() == null ? constant(null)
                : restated(value(g.maximum(), reads), eff.unitOf(g.maximum()), unit, "een begrenzing (maximum)");
        return new Term.Call(StaticFunction.RS_CLAMP, List.of(base, minimum, maximum), source(g.source()));
    }

    /** A text composition (IR-SPEC ch. 4.2): the literal chunks and the inserted parts in part order (RUNTIME-SEMANTICS
     *  ch. 5.11), each as its text ({@link #textPart}). */
    private Term textComposition(Expr.TextComposition t, Reads reads) {
        List<Term> parts = new ArrayList<>();
        for (Expr.TextComposition.Part d : t.parts()) {
            switch (d) {
                case Expr.TextComposition.LiteralChunk l ->
                        parts.add(new Term.Const(l.text() == null ? "" : l.text(), new Datatype.Text(), null));
                case Expr.TextComposition.EmbeddedExpression i -> parts.add(textPart(i.expr(), reads, "TextComposition"));
            }
        }
        return new Term.TextComposition(parts, source(t.source()));
    }

    /** An inserted part of a text composition or an argument of a concatenation, as its text: a text or an enumeration
     *  label is its own text, and a leeg part reads "" (RUNTIME-SEMANTICS ch. 1.2). A part of another type is refused by
     *  name: the spec states the leeg rule and nothing more, and for the text of a number, a truth value or a date the
     *  reference interpreter (5,50 as "5.5", waar as "waar") and the runtime's {@code rsTekst} (5.50, true) differ. */
    private Term textPart(Expr e, Reads reads, String where) {
        Datatype t = eff.typeOf(e);
        if (!(t instanceof Datatype.Text) && !(t instanceof Datatype.Enumeration))
            throw new Refusal(DroolsCode.TEXT_PART_NON_TEXT.reason("datatype", typeName(t), "where", where, "snippet", snippet(e.source())));
        Term f = value(e, reads);
        return new Term.Call(StaticFunction.RS_TEXT, List.of(f), f.source());
    }

    /** A cell of a dimensioned attribute (§3.6): the value at its one label in the attribute's {@code Map<label, value>}
     *  bean member, leeg where the map or the label is absent. The attribute is
     *  input, read off the subject's own bean ({@link #dimensionRead}); a cell over several dimensions is refused by
     *  name, as that branch refuses it. The reference interpreter and the ASP backend do not read a cell. */
    private Term dimensionCell(Expr.DimensionCellRead c, Reads reads) {
        if (c.labels() == null || c.labels().size() != 1)
            throw new Refusal(DroolsCode.DIMENSION_CELL_MULTI_DIMENSION.reason("dimensionCount", c.labels() == null ? 0 : c.labels().size(), "snippet", snippet(c.source())));
        int i = dimensionRead(c.attribute(), reads, Msg.drools("PART.desc.dimension-cell-read"));
        String label = c.labels().get(0);
        Term map = new Term.SlotValue(i, source(c.attribute().source()));
        note(c.attribute(), map);
        return new Term.Call(StaticFunction.CELL, List.of(map, constant(label)), source(c.source()));
    }

    /** A sum, minimum or maximum over a dimension selection (§5.8.5): the attribute's cells at the selected labels, a
     *  leeg cell skipped and an all-leeg selection leeg, through the runtime's {@code rsDimSom},
     *  {@code rsDimMin} and {@code rsDimMax}; a sum's {@code emptyFallback} is evaluated after the cells, every time
     *  (RUNTIME-SEMANTICS ch. 5.11), and stands in for an all-leeg sum. The reference interpreter and the ASP backend do
     *  not fold a dimension selection. */
    private Term dimensionFold(Expr.FunctionCall f, Reads reads) {
        Expr.FunctionKind kind = f.kind();
        if (kind != Expr.FunctionKind.SOM && kind != Expr.FunctionKind.MINIMUM && kind != Expr.FunctionKind.MAXIMUM)
            throw new Refusal(DroolsCode.DIMENSION_FOLD_UNSUPPORTED_KIND.reason("kind", kind, "snippet", snippet(f.source())));
        if (f.emptyFallback() != null && kind != Expr.FunctionKind.SOM)
            throw new Refusal(DroolsCode.EMPTY_FALLBACK_NON_SUM.reason("kind", kind, "snippet", snippet(f.source())));
        if (f.arguments().size() != 1 || !(f.arguments().get(0) instanceof Expr.AttributeReference a))
            throw new Refusal(DroolsCode.DIMENSION_FOLD_NON_ATTRIBUTE.reason("snippet", snippet(f.source())));
        int i = dimensionRead(a, reads, Msg.drools("PART.desc.dimension-aggregation"));
        List<String> labels = List.copyOf(f.dimensionSelection().labels());
        Term fallback = f.emptyFallback() == null ? null : restated(value(f.emptyFallback(), reads),
                eff.unitOf(f.emptyFallback()), eff.unitOf(a), "een §5.8.2 terugvalwaarde");
        StaticFunction fold = switch (kind) {
            case SOM -> StaticFunction.RS_DIM_SUM;
            case MINIMUM -> StaticFunction.RS_DIM_MIN;
            default -> StaticFunction.RS_DIM_MAX;
        };
        String source = source(f.source());
        Term map = new Term.SlotValue(i, source(a.source()));
        note(a, map);
        Term cells = new Term.Call(fold, List.of(new Term.Call(StaticFunction.MAP, List.of(map), null), constant(labels)),
                source);
        // only a sum has a fallback, evaluated after the cells
        return fallback == null ? cells : new Term.Call(StaticFunction.FALLBACK, List.of(cells, fallback), source);
    }

    /** The read of the dimensioned attribute {@code a} as its whole map: the subject's own attribute, which no form
     *  writes, bound off its bean. A dimensioned attribute over a path, or one a form writes, is refused by name. */
    private int dimensionRead(Expr.AttributeReference a, Reads reads, Object description) {
        if (a.objectType() == null || a.attribute() == null)
            throw new Refusal(DroolsCode.DIMENSION_READ_UNRESOLVED_ATTRIBUTE.reason("description", description, "snippet", snippet(a.source())));
        if (navigationBindings.containsKey(a))
            throw new Refusal(DroolsCode.DIMENSION_READ_OVER_PATH.reason("description", description, "snippet", snippet(a.source())));
        if (a.objectType() != reads.subject)
            throw new Refusal(DroolsCode.OWN_READ_FOREIGN_SUBJECT.reason("objectType", a.objectType().name, "subject", reads.subject.name, "snippet", snippet(a.source())));
        Member member = dimensionedMember(reads.subject, a.attribute());
        if (writers.containsKey(member.key()))
            throw new Refusal(DroolsCode.DIMENSION_READ_WRITTEN_ATTRIBUTE.reason("description", description, "attribute", member.name(), "snippet", snippet(a.source())));
        return reads.own(member);
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
    private Term let(Expr.Let l, Reads reads) {
        if (l.binder() == null) throw new Refusal(DroolsCode.LET_WITHOUT_BINDER.reason("snippet", snippet(l.source())));
        Term value = value(l.value(), reads);
        int slot = reads.binder();
        String key = Namen.norm(l.binder());
        Integer outside = reads.binders.put(key, slot);
        Term body;
        try {
            body = value(l.body(), reads);
        } finally {
            if (outside == null) reads.binders.remove(key);
            else reads.binders.put(key, outside);
        }
        return new Term.Let(slot, l.binder(), value, body, Boolean.TRUE.equals(readsFirst(body, slot)),
                source(l.source()));
    }

    /**
     * Whether evaluating {@code t} reads the binder in slot {@code slot} before anything of it can fault, deposit or end
     * an evaluation: TRUE where that read comes first; FALSE where such a step comes first, among them the read of
     * another binder, whose need can do all three, or where the read stands in a part that is evaluated only on a
     * condition; null where {@code t} only reads bound values and computes without a fault, so the evaluation goes on
     * to what follows it. A {@code Let} whose body gives TRUE can have its value evaluated before the body with the same
     * outcome, faults and deposits ({@link Term.Let#eager}).
     */
    private static Boolean readsFirst(Term t, int slot) {
        return switch (t) {
            case Term.Const c -> null;
            case Term.SlotValue l -> null;
            case Term.DayKind d -> null;
            // a read of an input has no effect and cannot fault, so the evaluation goes on to what follows it
            case Term.Input i -> readsFirst(i.object(), slot);
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
                Boolean first = readsFirst(v.value(), slot);
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
    private Term binderRead(Expr.BinderRef b, Reads reads) {
        String name = b.name() == null ? null : Namen.norm(b.name());
        Integer slot = name == null ? null : reads.binders.get(name);
        if (slot == null && name != null && reads.outside.contains(name))
            throw new Refusal(DroolsCode.BINDER_READ_FOLD_FILTER.reason("binder", b.name(), "snippet", snippet(b.source())));
        if (slot == null)
            throw new Refusal(DroolsCode.BINDER_WITHOUT_ENCLOSING_LET.reason("binder", b.name(), "snippet", snippet(b.source())));
        return new Term.Binder(slot, b.name(), source(b.source()));
    }

    /** An {@code IfThenElse} (RUNTIME-SEMANTICS ch. 10.2): the condition evaluated as any condition of this branch, then
     *  exactly the branch it chooses; the other branch evaluates nothing, so it reads no binder, faults nowhere and
     *  deposits nothing. The reads of both branches are bound in the left-hand side all the same. */
    private Term choice(Expr.IfThenElse k, Reads reads) {
        Term conditionTerm = conditionTerm(k.condition(), reads);
        Term then = value(k.then(), reads), otherwise = value(k.otherwise(), reads);
        return new Term.Choice(conditionTerm, then, otherwise, source(k.source()));
    }

    /** A context value (RUNTIME-SEMANTICS ch. 8.2): evaluationDate, the session's evaluation date, and evaluationYear,
     *  its calendar year as a whole number without a unit ({@code rsRekenjaar}). Both read the session's
     *  {@link EvaluationDate}, bound in the left-hand side, and fault where the session carries no date, in the reference
     *  interpreter's sentences. The bound day of a day-kind definition, read outside such a definition, faults as in that
     *  executor, since it denotes nothing there; a day-kind definition itself is outside this branch. */
    private Term contextValue(Expr.Constant c, Reads reads) {
        String name = c.name() == null ? "" : c.name();
        String source = source(c.source());
        switch (name) {
            case "Rekendatum" -> {
                int i = reads.evaluationDate();
                reads.markFail();
                return new Term.Required(new Term.SlotValue(i, null), ComputeFault.EVALUATION_DATE_WITHOUT_DATE, source);
            }
            case "Rekenjaar" -> {
                int i = reads.evaluationDate();
                reads.markFail();
                return new Term.Call(StaticFunction.RS_EVALUATION_YEAR, List.of(new Term.Required(new Term.SlotValue(i, null),
                        RegelSpraakRuntime.REKENJAAR_ZONDER_REKENDATUM, null)), source);
            }
            case "de dag" -> {
                reads.markFail();
                return new Term.Fail(ComputeFault.DAY_OUTSIDE_DAY_KIND, source);
            }
            default -> throw new Refusal(DroolsCode.CONTEXT_VALUE_UNSUPPORTED.reason("name", name, "snippet", snippet(c.source())));
        }
    }

    // ---------------- aggregations ----------------

    /** A fold this branch lowers: the key its contributions carry ({@link Contribution#fold}), its contribution rules (one
     *  per range, beside the filter fault rule of a filter that can fault and the filter deposit rule of a filter that
     *  can deposit), the reads of those rules, registered when a form that reads the fold lowers
     *  ({@link Reads#recorder}), whether its filter can fault, in which case the rule that reads it binds its
     *  {@link FilterFault} facts as well, and whether its filter can deposit, in which case that rule lists its
     *  {@link FilterDeposit} facts, which carry the EF codes of their deposits; for a quantifier
     *  fold, whether its matrix can fault and whether it can deposit,
     *  in which case its contributions carry a {@link MemberTest} ({@link #quantifierFold}); and for a per-element fold over
     *  an attribute on a timeline, that a member's value can fault, in which case the rule that reads it binds its
     *  {@link MemberFault} facts as well ({@link #memberFaultRule}). */
    private record Fold(String key, List<RulePlan> rules, List<Reads> readings, boolean canFail,
                        boolean deposits, boolean memberCanFail, boolean memberDeposits, boolean valueCanFail) {
        /** A fold over a set source or a union: no matrix per member. */
        Fold(String key, List<RulePlan> rules, List<Reads> readings, boolean canFail, boolean deposits) {
            this(key, rules, readings, canFail, deposits, false, false, false);
        }

        /** A quantifier fold: a matrix per member, and no value per member. */
        Fold(String key, List<RulePlan> rules, List<Reads> readings, boolean canFail, boolean deposits,
             boolean memberCanFail, boolean memberDeposits) {
            this(key, rules, readings, canFail, deposits, memberCanFail, memberDeposits, false);
        }

        /** Whether the rule that reads the fold replays its contributors in order: a quantifier fold whose matrix can
         *  fault or deposit ({@link MemberTest}). */
        boolean ordered() {
            return memberCanFail || memberDeposits;
        }
    }

    /** The kinds of an aggregation, as against the builtins that share the node. */
    private static boolean isFold(Expr.FunctionKind k) {
        return switch (k) {
            case SOM, TOTAAL, AANTAL, AANTAL_GEVULDE, MINIMUM, MAXIMUM, EERSTE, LAATSTE -> true;
            default -> false;
        };
    }

    /** The listed fold, "de eerste van A en B": two or more operands and no filter, as against a fold over a set
     *  source; told apart by the shape, as RUNTIME-SEMANTICS ch. 1.2e does, since the element type the producer stamps
     *  on it is vestigial. */
    private static boolean isListedFold(Expr.FunctionCall f) {
        boolean ordering = switch (f.kind()) { case MINIMUM, MAXIMUM, EERSTE, LAATSTE -> true; default -> false; };
        return ordering && f.arguments().size() >= 2 && f.filter() == null;
    }

    /** An aggregation, listed, over a set source or over a dimension selection; every other function node is a builtin
     *  call ({@link #builtin}). */
    private Term function(Expr.FunctionCall f, Reads reads) {
        if (f.dimensionSelection() != null) return dimensionFold(f, reads);
        if (!isFold(f.kind())) return builtin(f, reads);
        return isListedFold(f) ? listedFold(f, reads) : fold(f, reads);
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
    private Term builtin(Expr.FunctionCall f, Reads reads) {
        Expr.FunctionKind kind = f.kind();
        String spelling = Spelling.BUILTIN_KIND.spelling(kind);
        if (kind == Expr.FunctionKind.TIJDSEVENREDIG_DEEL || kind == Expr.FunctionKind.AANTAL_DAGEN_IN)
            throw new Refusal(DroolsCode.PROPORTIONAL_DAY_COUNT_UNTIMED.reason("spelling", spelling, "snippet", snippet(f.source())));
        if (f.filter() != null)
            throw new Refusal(DroolsCode.BUILTIN_FILTER_WITHOUT_TIMELINE.reason("spelling", spelling, "snippet", snippet(f.source())));
        List<Expr> a = f.arguments();
        return switch (kind) {
            case WORTEL -> exactSquareRoot(f, reads);
            case ABSOLUTE_WAARDE -> new Term.Call(StaticFunction.RS_ABS, List.of(value(oneArgument(f, spelling), reads)),
                    source(f.source()));
            case CONCATENATIE -> {
                List<Term> parts = new ArrayList<>();
                for (Expr arg : a) parts.add(textPart(arg, reads, "concatenation"));
                yield new Term.TextComposition(parts, source(f.source()));
            }
            case TIJDSDUUR, ABSOLUTE_TIJDSDUUR -> duration(f, reads, spelling);
            case EERSTE_PAASDAG -> easterSunday(f, reads, spelling);
            case JAAR_UIT -> datePart(f, reads, spelling, ChronoField.YEAR, false);
            case MAAND_UIT -> datePart(f, reads, spelling, ChronoField.MONTH_OF_YEAR, false);
            case DAG_UIT -> datePart(f, reads, spelling, ChronoField.DAY_OF_MONTH, false);
            case UUR_UIT -> datePart(f, reads, spelling, ChronoField.HOUR_OF_DAY, true);
            case MINUUT_UIT -> datePart(f, reads, spelling, ChronoField.MINUTE_OF_HOUR, true);
            case SECONDE_UIT -> datePart(f, reads, spelling, ChronoField.SECOND_OF_MINUTE, true);
            case MILLISECONDE_UIT -> datePart(f, reads, spelling, ChronoField.MILLI_OF_SECOND, true);
            case DATUM_MET, DATUMTIJD_MET -> dateConstruction(f, reads, spelling);
            default -> throw new IllegalStateException("de soort " + kind + " is een vouw, geen builtin");
        };
    }

    /** A square root that is not the base of a rounding, exact as the reference interpreter computes it: the root of
     *  a radicand that is the square of a rational, and otherwise the fault that executor gives, since an irrational
     *  root exists only under the rounding §13.4.17.13 mandates (RUNTIME-SEMANTICS ch. 2.2). A leeg radicand is leeg,
     *  a negative one leeg with the §6.6 deposit where the radicand qualifies and the evaluation reaches it (ch. 1.2,
     *  {@link DepositFlag}). The destructive lowering computed a binary approximation here. */
    private Term exactSquareRoot(Expr.FunctionCall f, Reads reads) {
        if (f.arguments().size() != 1)
            throw new Refusal(DroolsCode.SQUARE_ROOT_ARGUMENT_COUNT.reason("argumentCount", f.arguments().size(), "snippet", snippet(f.source())));
        reads.markFail();
        Term radicand = value(f.arguments().get(0), reads);
        int depositFlag = depositSite(f) ? reads.depositFlag() : -1;
        return new Term.SquareRoot(StaticFunction.EXACT_SQUARE_ROOT, radicand, depositFlag, source(f.source()));
    }

    /** The one argument of {@code f}; a call with another number is refused (WF-037). */
    private static Expr oneArgument(Expr.FunctionCall f, String spelling) {
        if (f.arguments().size() != 1)
            throw new Refusal(DroolsCode.ONE_ARGUMENT_BUILTIN_ARITY.reason("spelling", spelling, "count", f.arguments().size(), "snippet", snippet(f.source())));
        return f.arguments().get(0);
    }

    /** The duration and the absolute duration from argument 0 to argument 1 in whole units of the node's own unit
     *  (RUNTIME-SEMANTICS ch. 3.8, WF-038), the "van" evaluated before the "tot" (ch. 5.11); a leeg operand leeg. A unit
     *  that is a day kind counts the days of the kind ({@link #dayKindDuration}). */
    private Term duration(Expr.FunctionCall f, Reads reads, String spelling) {
        List<Expr> a = f.arguments();
        if (a.size() != 2)
            throw new Refusal(DroolsCode.DURATION_ARGUMENT_COUNT.reason("spelling", spelling, "count", a.size(), "snippet", snippet(f.source())));
        Datatype van = eff.typeOf(a.get(0)), to = eff.typeOf(a.get(1));
        if (!van.isDate() || !to.isDate() || !sameDateKind(van, to))
            throw new Refusal(DroolsCode.DURATION_MIXED_DATE_PRECISION.reason("spelling", spelling, "snippet", snippet(f.source())));
        Datatype type = eff.typeOf(f);
        // ch. 3.8 puts the calendar table first, as TimeCalculation.dayKindForUnit looks a spelling up for every branch
        String dayKind = TimeCalculation.dayKindForUnit(model, type.unitOf());
        if (dayKind != null) return dayKindDuration(f, reads, spelling, dayKind, van instanceof Datatype.DateTime);
        String unit = TimeCalculation.chronoUnit(type);
        if (unit == null)
            throw new Refusal(DroolsCode.DURATION_SPELLING_NO_UNIT.reason("spelling", spelling, "snippet", snippet(f.source())));
        boolean clock = switch (unit) { case "HOURS", "MINUTES", "SECONDS", "MILLIS" -> true; default -> false; };
        if (clock && !(van instanceof Datatype.DateTime))
            throw new Refusal(DroolsCode.CLOCK_UNIT_WITHOUT_DATETIME.reason("spelling", spelling, "snippet", snippet(f.source())));
        Term from = value(a.get(0), reads), upTo = value(a.get(1), reads);
        boolean absolute = f.kind() == Expr.FunctionKind.ABSOLUTE_TIJDSDUUR;
        return new Term.Call(StaticFunction.RS_DURATION, List.of(from, upTo, constant(unit), constant(absolute)),
                source(f.source()));
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
    private Term dayKindDuration(Expr.FunctionCall f, Reads reads, String spelling, String dayKind, boolean dateTime) {
        Msg description = Msg.drools("PART.desc.day-kind-spelling", "spelling", spelling, "snippet", snippet(f.source()));
        if (dateTime)
            throw new Refusal(DroolsCode.DAY_KIND_COUNT_DATETIMES.reason("description", description));
        java.util.function.Predicate<LocalDate> datePredicate = dayKindPredicate(dayKind, description);
        Term from = value(f.arguments().get(0), reads), upTo = value(f.arguments().get(1), reads);
        boolean absolute = f.kind() == Expr.FunctionKind.ABSOLUTE_TIJDSDUUR;
        return new Term.Call(StaticFunction.RS_DURATION_DAY_KIND, List.of(from, upTo,
                new Term.DayKind(dayKind, datePredicate, null), constant(absolute)), source(f.source()));
    }

    /** A part of a date or a date-time, as a whole number without a unit; a leeg date leeg (RUNTIME-SEMANTICS ch. 1.2,
     *  3.8). A clock part demands a date-time. */
    private Term datePart(Expr.FunctionCall f, Reads reads, String spelling, ChronoField field, boolean clock) {
        Expr arg = oneArgument(f, spelling);
        Datatype t = eff.typeOf(arg);
        if (!t.isDate() || (clock && !(t instanceof Datatype.DateTime)))
            throw new Refusal(DroolsCode.DATE_PART_NON_DATE.reason("spelling", spelling, "dateType", DroolsCode.DATE_PART_NON_DATE.part(clock ? "date-time" : "date"), "snippet", snippet(f.source())));
        Term d = value(arg, reads);
        return new Term.Call(StaticFunction.RS_DATE_PART, List.of(d, constant(field)), source(f.source()));
    }

    /** easterSunday of a whole year without a unit (RUNTIME-SEMANTICS ch. 3.8, WF-038), by the algorithm of that chapter
     *  ({@link ComputeHelpers#easterSunday}); a leeg year leeg. A year that is not whole is a fault in the reference interpreter's
     *  sentence, never a silent truncation, and so is a year outside the years this backend's dates hold. */
    private Term easterSunday(Expr.FunctionCall f, Reads reads, String spelling) {
        Expr arg = oneArgument(f, spelling);
        Unit unit = eff.unitOf(arg);
        if (unit != null)
            throw new Refusal(DroolsCode.EASTER_SUNDAY_YEAR_UNIT.reason("spelling", spelling, "unit", unit.rendering(), "snippet", snippet(f.source())));
        reads.markFail();
        Term year = value(arg, reads);
        return new Term.Call(StaticFunction.EASTER_SUNDAY, List.of(year), source(f.source()));
    }

    /** A date of three parts or a date-time of seven, through the runtime's {@code rsDatumMet} and
     *  {@code rsDatumtijdMet}: every part evaluated in order (RUNTIME-SEMANTICS ch. 5.11), any leeg part leeg (ch.
     *  1.2). A part that is not a whole number within the range of a date part, or parts that name no day, fault: the
     *  reference interpreter makes no such date, and the runtime would truncate the part or throw past the fire
     *  ({@link ComputeHelpers#dateWith}). */
    private Term dateConstruction(Expr.FunctionCall f, Reads reads, String spelling) {
        boolean time = f.kind() == Expr.FunctionKind.DATUMTIJD_MET;
        int itemCount = time ? 7 : 3;
        if (f.arguments().size() != itemCount)
            throw new Refusal(DroolsCode.DATE_CONSTRUCTION_ARGUMENT_COUNT.reason("spelling", spelling, "count", f.arguments().size(), "snippet", snippet(f.source()), "itemCount", itemCount));
        reads.markFail();
        List<Term> arguments = new ArrayList<>();
        // the spelling first: the sentence of a fault names the builtin
        arguments.add(constant(spelling));
        for (Expr arg : f.arguments()) arguments.add(value(arg, reads));
        return new Term.Call(time ? StaticFunction.DATUMTIJD_MET : StaticFunction.DATUM_MET, arguments, source(f.source()));
    }

    /**
     * A listed fold (RUNTIME-SEMANTICS ch. 1.2e): minimum and eerste keep the least operand value, maximum and laatste
     * the greatest; a leeg operand is skipped, an all-leeg fold is leeg, and a tie takes the later operand. The operands
     * are computed from the values the rule binds, in wire order, each after the first restated into the first one's
     * unit, the unit of the result (§4.6 Eenheid1, RUNTIME-SEMANTICS ch. 2.6).
     */
    private Term listedFold(Expr.FunctionCall f, Reads reads) {
        boolean smallest = f.kind() == Expr.FunctionKind.MINIMUM || f.kind() == Expr.FunctionKind.EERSTE;
        Unit earliest = eff.unitOf(f.arguments().get(0));
        List<Term> operands = new ArrayList<>();
        for (int i = 0; i < f.arguments().size(); i++) {
            Expr a = f.arguments().get(i);
            Term op = value(a, reads);
            operands.add(i == 0 ? op : restated(op, eff.unitOf(a), earliest, "een §5.8.3 operand van een gelijste vouw"));
        }
        return new Term.ListedFold(smallest ? StaticFunction.SMALLEST : StaticFunction.LARGEST, operands, source(f.source()));
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
    private Term fold(Expr.FunctionCall f, Reads reads) {
        Expr.FunctionKind kind = f.kind();
        String fragment = source(f.source());
        if (kind == Expr.FunctionKind.AANTAL && f.arguments().size() == 1
                && f.arguments().get(0) instanceof Expr.InstanceSetUnion u) {
            Fold v = unionFold(f, u, reads);
            int[] i = reads.fold(v, null);
            return readFold(v, i, reads, new Term.Call(StaticFunction.COUNT, List.of(new Term.SlotValue(i[0], null)), fragment),
                    fragment);
        }
        ObjectType elem = f.elementObjectType();
        if (elem == null) throw new Refusal(DroolsCode.AGGREGATION_WITHOUT_ELEMENT_TYPE.reason("kind", kind, "snippet", snippet(f.source())));
        if (f.arguments().size() != 1)
            throw new Refusal(DroolsCode.AGGREGATION_OPERAND_COUNT.reason("kind", kind, "count", f.arguments().size(), "snippet", snippet(f.source())));
        if (f.emptyFallback() != null && kind != Expr.FunctionKind.SOM)
            throw new Refusal(DroolsCode.EMPTY_FALLBACK_NON_SUM.reason("kind", kind, "snippet", snippet(f.source())));
        boolean count = kind == Expr.FunctionKind.AANTAL;
        Expr.AttributeReference arg = null;
        if (!count) {
            if (!(f.arguments().get(0) instanceof Expr.AttributeReference a) || a.attribute() == null)
                throw new Refusal(DroolsCode.AGGREGATION_NON_ATTRIBUTE_OPERAND.reason("kind", kind, "snippet", snippet(f.source())));
            arg = a;
        }
        boolean withRange = sourcePaths.containsKey(f) || roleBindings.containsKey(f);
        if (kind == Expr.FunctionKind.TOTAAL && !withRange) return ownTotal(f, reads);
        if (arg != null && Namen.containsName(elem.granularityMembers(), arg.attribute())
                && kind != Expr.FunctionKind.SOM && kind != Expr.FunctionKind.TOTAAL)
            throw new Refusal(DroolsCode.TIMELINE_AGGREGATION_NON_SUM.reason("kind", kind, "attribute", arg.attribute(), "snippet", snippet(f.source())));
        if (!withRange)
            throw new Refusal(DroolsCode.AGGREGATION_WITHOUT_SOURCE_PATH.reason("kind", kind, "snippet", snippet(f.source())));
        Expr.AttributeReference member = arg;
        // a member on a timeline the case gives as the asOfDate-bucket scalar faults, naming the first such member in
        // fold order (RUNTIME-SEMANTICS ch. 3.6, 8.4), and a made link has no place in that order
        boolean timelineMember = member != null && declared(elem, member.attribute(), false).time() != null;
        // a member on a timeline is asked by the per-element fold, which refuses one that is no number (integreerbaar)
        if (member != null && !timelineMember) {
            Reason setSource = setSourceRefusal(kind, elem, member);
            if (setSource != null) throw new Refusal(setSource);
        }
        RoleBindingRef source = roleBindings.get(f);
        if (timelineMember && source != null && source.factType() != null
                && createdRelations.contains(Namen.norm(source.factType())))
            throw new Refusal(DroolsCode.CREATED_LINK_FOLD_ORDER.reason("kind", kind, "attribute", member.attribute(), "factType", source.factType(), "snippet", snippet(f.source())));
        Fold v = foldFor(f, reads, (key, name) -> {
            List<RulePlan> rules = new ArrayList<>();
            List<Reads> readings = new ArrayList<>();
            boolean canFail = contributionRules(key, name, kind, reads.subject, elem, range(f, reads.subject, elem), 0,
                    f.filter(), member, rules, readings);
            refuseMixedDeposits(name, Collections.singletonList(f.filter()));
            return new Fold(key, List.copyOf(rules), List.copyOf(readings), canFail, deposits(f.filter()), false, false,
                    timelineMember);
        });
        Accumulation.Kind function = switch (kind) {
            case SOM, TOTAAL -> Accumulation.Kind.SUM;
            case MINIMUM, EERSTE -> Accumulation.Kind.MINIMUM;
            case MAXIMUM, LAATSTE -> Accumulation.Kind.MAXIMUM;
            default -> null;
        };
        int[] i = reads.fold(v, function);
        // the fallback restated into the unit of the folded member (RUNTIME-SEMANTICS ch. 2.6)
        Term fallback = f.emptyFallback() == null ? null : restated(value(f.emptyFallback(), reads),
                eff.unitOf(f.emptyFallback()), arg == null ? null : eff.unitOf(arg), "een §5.8.2 terugvalwaarde");
        Term itemCount = new Term.SlotValue(i[0], null);
        // over no contribution a sum is its fallback or leeg, a total 0 and an ordering fold leeg
        Term outcome = switch (kind) {
            case AANTAL, AANTAL_GEVULDE -> new Term.Call(StaticFunction.COUNT, List.of(itemCount), fragment);
            case SOM -> foldValue(itemCount, i[1], fallback == null ? constant(null) : fallback, fragment);
            case TOTAAL -> foldValue(itemCount, i[1], constant(BigDecimal.ZERO), fragment);
            default -> foldValue(itemCount, i[1], constant(null), fragment);
        };
        return readFold(v, i, reads, outcome, fragment);
    }

    /** The value of a fold from the count {@code count} and the folded value at slot {@code value}, with
     *  {@code fallback} over no contribution ({@link ComputeHelpers#foldValue}). */
    private static Term foldValue(Term itemCount, int value, Term fallback, String source) {
        return new Term.Call(StaticFunction.FOLD_VALUE, List.of(itemCount, new Term.SlotValue(value, null), fallback), source);
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
    private Reason setSourceRefusal(Expr.FunctionKind kind, ObjectType elem, Expr.AttributeReference arg) {
        boolean summing = kind == Expr.FunctionKind.SOM || kind == Expr.FunctionKind.TOTAAL;
        boolean ordering = kind == Expr.FunctionKind.MINIMUM || kind == Expr.FunctionKind.MAXIMUM
                || kind == Expr.FunctionKind.EERSTE || kind == Expr.FunctionKind.LAATSTE;
        Datatype t = declared(elem, arg.attribute(), false).datatype();
        Msg head = Msg.drools("PART.desc.set-source-head", "kind", kind, "attribute", arg.attribute(), "element", elem.name,
                "type", t.name());
        if (summing && !t.isNumberLike())
            return DroolsCode.SUM_NON_NUMERIC_SETSOURCE.reason("head", head);
        if (ordering && !t.isOrderable())
            return DroolsCode.ORDERING_NON_ORDERABLE_SETSOURCE.reason("head", head);
        return null;
    }

    /**
     * The temporal total of the subject's own timeline (RUNTIME-SEMANTICS ch. 3.6): "het totaal van zijn X", optionally
     * "gedurende" a clause, as a function of the values this rule binds: the timeline of X, through its standing
     * ({@link Reads#timeline}), and every timeline and value the clause reads. The two routes of the total take the two
     * conventions of the case input, and a form that needs the one and finds the other faults ({@link InputFormFault},
     * ch. 8.4). Without a clause the total takes the asOfDate-bucket scalar, the session's date being the horizon: it is
     * {@code rsTijdTotaal} of the one period of X's grid that holds the evaluation date ({@link BucketScalar}), the
     * segment's value times the whole units it spans, and leeg where X has no input; a full timeline faults, in the
     * reference interpreter's words, and so does a session without an evaluation date, where the total has no bucket to
     * take, X filled or not: this route reads the date ({@link Reads#evaluationDate}), and the reference interpreter tests
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
    private Term ownTotal(Expr.FunctionCall f, Reads reads) {
        Msg description = Msg.drools("PART.desc.own-total", "snippet", snippet(f.source()));
        Expr.AttributeReference a = (Expr.AttributeReference) f.arguments().get(0);
        if (a.objectType() != reads.subject || navigationBindings.containsKey(a))
            throw new Refusal(DroolsCode.OWN_TOTAL_FOREIGN_ATTRIBUTE.reason("description", description));
        Member member = integrable(timeMember(reads.subject, a.attribute(), false, description), description);
        if (f.filter() == null && writers.containsKey(member.key()))
            throw new Refusal(DroolsCode.TOTAL_NO_CUT_WRITTEN.reason("description", description, "member", member.name()));
        int timeline = reads.timeline(member);
        int subjectType = reads.member();
        reads.markFail();
        String gran = member.time().name();
        String name = member.name();
        String source = source(f.source());
        if (f.filter() == null) {
            int date = reads.evaluationDate();
            Term input = new Term.SlotValue(timeline, source(a.source()));
            note(a, input);
            return new Term.Call(StaticFunction.OWN_TOTAL, List.of(input, new Term.SlotValue(subjectType, null),
                    new Term.SlotValue(date, null), constant(name), constant(gran)), source);
        }
        Term whole = complete(timeline, "the gedurende clip of \"" + name + "\" needs the full timeline; ", subjectType,
                " supplies only the asOfDate-bucket scalar", source(a.source()));
        note(a, whole);
        Term cut = clip(f.filter(), reads, description, name, whole, whole);
        return new Term.Call(StaticFunction.RS_TIME_TOTAL, List.of(cut, constant(gran)), source);
    }

    /** The timeline bound at slot {@code timeline}, which a form reads whole, or the fault in the sentence {@code voor},
     *  the subject bound at slot {@code subjectType}, {@code na}, where the case gives that input as the asOfDate-bucket
     *  scalar ({@link BucketScalar}, RUNTIME-SEMANTICS ch. 8.4; {@link ComputeHelpers#complete}). */
    private static Term complete(int timeline, String before, int subjectType, String after, String source) {
        return new Term.Call(StaticFunction.COMPLETE, List.of(new Term.SlotValue(timeline, source), constant(before),
                new Term.SlotValue(subjectType, null), constant(after)), source);
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
     * timeline is clipped, and {@code timeline} computes that timeline, which the clip takes first; {@code base} computes
     * the whole timeline of {@code cut}, which a member of a composite clip reads its periods within.
     *
     * <p>In a build with readings the reading of the clause is the part of the timeline in which it holds (ch. 5.12),
     * not the timeline the clip leaves: the true periods of a characteristic, or, negated, the periods outside them
     * within the segments of {@code base}; for a composite the intersection of its members' periods, each member read
     * on its own, each a period without a value; for a comparison the segments of the compared timeline where it
     * holds, with their values, cut where the segments of {@code base} are cut. The clip computes none of these, so the
     * reading follows from terms it does compute ({@link #noteDerived}), and the rule's terms stay those of a build
     * without readings.
     *
     * <p>Refused by name: any other clause, a compound other than {@code all}, a check navigated or with an owner or a
     * whole-period qualifier (WF-097), a comparison whose left side is not an own attribute on a timeline, a date
     * ordering, units that differ between the two sides, and the comparisons "gelijk aan" and "ongelijk aan": the
     * runtime's {@code rsTijdConditie} reads an empty side, a gap in the compared timeline or a leeg bound, as no
     * period, where ch. 3.6 and the reference interpreter apply the comparison of ch. 1.2 per period, so that two leeg
     * sides are equal and exactly one leeg side is unequal (ch. 3.6, the worked example of 500,00); no golden has
     * either.
     */
    private Term clip(Condition c, Reads reads, Object description, String cut, Term timeline, Term base) {
        if (c instanceof Condition.CharacteristicCheck k) return checkClip(k, reads, description, timeline, base, null);
        if (c instanceof Condition.Compound s && s.quantifier() == Condition.Quantifier.ALLE && !s.components().isEmpty()) {
            // each check clips what the one before it left
            Term out = timeline;
            List<Term> inputs = new ArrayList<>(List.of(base));
            List<Boolean> negated = new ArrayList<>();
            for (Condition part : s.components()) {
                if (!(part instanceof Condition.CharacteristicCheck k))
                    throw new Refusal(DroolsCode.CLIP_COMPOSITE_NON_CHARACTERISTIC.reason("description", description, "snippet", snippet(part)));
                out = checkClip(k, reads, description, out, base, inputs);
                negated.add(k.negation());
            }
            // the intersection of the members' periods, each member read within the segments of the whole timeline
            noteDerived(s, inputs, x -> {
                List<Period> held = null;
                for (int i = 0; i < negated.size(); i++) {
                    List<Period> member = negated.get(i) ? outside(x.get(0), x.get(i + 1)) : periodsOf(x.get(i + 1));
                    held = held == null ? member : intersection(held, member);
                }
                return held;
            });
            return out;
        }
        if (c instanceof Condition.Comparison v) return comparisonClip(v, reads, description, cut, timeline);
        throw new Refusal(DroolsCode.CLIP_UNSUPPORTED_CUT_FORM.reason("description", description, "snippet", snippet(c)));
    }

    /** The clip of {@code timeline} by the check {@code k} of a characteristic of the subject on a timeline, or outside it
     *  where the check is negated ({@link #clip}); the term of the characteristic's true periods goes into
     *  {@code truePeriods} where it is not null, for the reading of the composite the check is a member of. */
    private Term checkClip(Condition.CharacteristicCheck k, Reads reads, Object description, Term timeline, Term base,
                           List<Term> truePeriods) {
        if (k.owner() != null || k.period() != null)
            throw new Refusal(DroolsCode.CLIP_OWNED_WHOLE_PERIOD.reason("description", description, "snippet", snippet(k)));
        if (k.navigation() != null || navigationBindings.containsKey(k))
            throw new Refusal(DroolsCode.CLIP_NAVIGATED_CHARACTERISTIC.reason("description", description, "snippet", snippet(k)));
        int periods = reads.timeline(timeMember(reads.subject, k.characteristic(), true, description));
        Term held = new Term.SlotValue(periods, null);
        if (truePeriods != null) truePeriods.add(held);
        if (k.negation()) noteDerived(k, List.of(base, held), x -> outside(x.get(0), x.get(1)));
        else noteDerived(k, List.of(held), x -> periodsOf(x.get(0)));
        return new Term.Call(k.negation() ? StaticFunction.RS_TIME_CLIP_NOT : StaticFunction.RS_TIME_CLIP,
                List.of(timeline, held, constant(Period.class)), source(k.source()));
    }

    /** The comparison clip of {@link #clip} of the timeline of {@code cut}: the left side an own attribute on a
     *  timeline, which the clip reads whole, segment by segment, so that the asOfDate-bucket scalar faults there
     *  ({@link InputFormFault}, RUNTIME-SEMANTICS ch. 8.4) before the bound is evaluated; the right side a value. */
    private Term comparisonClip(Condition.Comparison v, Reads reads, Object description, String cut, Term timeline) {
        if (v.period() != null)
            throw new Refusal(DroolsCode.CLIP_WHOLE_PERIOD_TEST.reason("description", description, "snippet", snippet(v)));
        Integer testCode = TimeCalculation.test(v.op());
        if (testCode == null)
            throw new Refusal(DroolsCode.CLIP_DATE_COMPARISON_UNSUPPORTED.reason("description", description, "snippet", snippet(v)));
        // an ordering with a leeg side holds nowhere, in ch. 1.2 and in that helper alike; the equalities differ
        if (v.op() == Condition.Operator.GELIJK || v.op() == Condition.Operator.ONGELIJK)
            throw new Refusal(DroolsCode.CLIP_EQUALITY_EMPTY_SIDE.reason("description", description, "operator", DroolsCode.CLIP_EQUALITY_EMPTY_SIDE.part(v.op() == Condition.Operator.GELIJK ? "equal" : "unequal"), "snippet", snippet(v)));
        if (!(v.left() instanceof Expr.AttributeReference a) || a.attribute() == null || a.objectType() != reads.subject
                || navigationBindings.containsKey(a))
            throw new Refusal(DroolsCode.CLIP_LEFT_NOT_TIMED.reason("description", description, "snippet", snippet(v)));
        Member tested = timeMember(reads.subject, a.attribute(), false, description);
        if (!tested.datatype().isNumber() || !eff.typeOf(v.right()).isNumber())
            throw new Refusal(DroolsCode.CLIP_COMPARISON_NON_NUMERIC.reason("description", description, "snippet", snippet(v)));
        if (!Objects.equals(eff.unitOf(v.left()), eff.unitOf(v.right())))
            throw new Refusal(DroolsCode.CLIP_COMPARISON_TWO_UNITS.reason("description", description, "snippet", snippet(v)));
        int testedTimeline = reads.timeline(tested);
        int subjectType = reads.member();
        reads.markFail();
        Term bound = value(v.right(), reads);
        String compared = tested.name();
        Term whole = complete(testedTimeline, "the gedurende clip of \"" + cut + "\" compares \"" + compared
                + "\" per segment and needs its full timeline; ", subjectType,
                " supplies only the asOfDate-bucket scalar (ch. 3.6, ch. 8.4)", source(a.source()));
        note(a, whole);
        Term periods = new Term.Call(StaticFunction.RS_TIME_CONDITION, List.of(whole,
                constant(testCode), new Term.Call(StaticFunction.COLLAPSE, List.of(bound), bound.source()),
                constant(Period.class)), null);
        // the segments of the compared timeline where the comparison holds, cut where the clipped timeline's segments
        // are cut (ch. 3.6: the comparison is read per period of their common cuts)
        noteDerived(v, List.of(periods, timeline), x -> cutAt(x.get(0), x.get(1)));
        return new Term.Call(StaticFunction.RS_TIME_CLIP, List.of(timeline, periods, constant(Period.class)),
                source(v.source()));
    }

    /** The segments of {@code timeline} as true periods, each without a value; none for an absent timeline. */
    private static List<Period> periodsOf(Object timeline) {
        List<Period> out = new ArrayList<>();
        if (timeline != null) for (Object o : (List<?>) timeline) {
            Period p = (Period) o;
            out.add(new Period(p.from(), p.to(), null));
        }
        return out;
    }

    /** The periods outside the true periods {@code truePeriods} within each segment of {@code timeline}, each without a
     *  value, per segment and never joined across two: where a negated check of a characteristic holds over that
     *  timeline (RUNTIME-SEMANTICS ch. 3.6). */
    private static List<Period> outside(Object timeline, Object truePeriods) {
        List<Period> out = new ArrayList<>();
        List<Period> held = periodsOf(truePeriods);
        for (Period segment : periodsOf(timeline)) {
            List<Period> inside = new ArrayList<>();
            for (Period p : held) {
                LocalDate from = later(p.from(), segment.from()), to = earlier(p.to(), segment.to());
                if (from.isBefore(to)) inside.add(new Period(from, to, null));
            }
            inside.sort(Comparator.comparing(Period::from).thenComparing(Period::to));
            LocalDate at = segment.from();
            for (Period p : inside) {
                if (p.from().isAfter(at)) out.add(new Period(at, p.from(), null));
                at = p.to();
            }
            if (segment.to().isAfter(at)) out.add(new Period(at, segment.to(), null));
        }
        return out;
    }

    /** The overlaps of every period of {@code a} with every period of {@code b}, in the order of their days: where both
     *  members of a composite clip hold (RUNTIME-SEMANTICS ch. 3.6). */
    private static List<Period> intersection(List<Period> a, List<Period> b) {
        List<Period> out = new ArrayList<>();
        for (Period p : a)
            for (Period q : b) {
                LocalDate from = later(p.from(), q.from()), to = earlier(p.to(), q.to());
                if (from.isBefore(to)) out.add(new Period(from, to, null));
            }
        out.sort(Comparator.comparing(Period::from).thenComparing(Period::to));
        return out;
    }

    /** The segments of {@code segments} cut at every day inside them where a segment of {@code timeline} begins or
     *  ends, each piece with the value of its segment. */
    private static List<Period> cutAt(Object segments, Object timeline) {
        java.util.TreeSet<LocalDate> cuts = new java.util.TreeSet<>();
        for (Period p : periodsOf(timeline)) {
            cuts.add(p.from());
            cuts.add(p.to());
        }
        List<Period> out = new ArrayList<>();
        if (segments != null) for (Object o : (List<?>) segments) {
            Period p = (Period) o;
            LocalDate at = p.from();
            for (LocalDate c : cuts.subSet(p.from(), false, p.to(), false)) {
                out.add(new Period(at, c, p.value()));
                at = c;
            }
            out.add(new Period(at, p.to(), p.value()));
        }
        return out;
    }

    private static LocalDate later(LocalDate a, LocalDate b) { return a.isAfter(b) ? a : b; }

    private static LocalDate earlier(LocalDate a, LocalDate b) { return a.isBefore(b) ? a : b; }

    /**
     * {@code member}, a member on a timeline whose total over time is taken, where that total is the bare value times the
     * count of whole units of the member's grid, as the runtime's {@code rsTijdTotaal} computes it: a number without a
     * unit, with a unit without a denominator, or a rate whose denominator is the time unit of the grid itself
     * ({@code EUR/mnd} on a grid of months), where the integration of RUNTIME-SEMANTICS ch. 3.6 cancels it one for one.
     * Refused by name: a value that is no number (WF-091); a rate over a grid whose unit the document does not declare
     * as exactly one unit ({@link #gridUnit}), which the reference interpreter refuses to integrate (ch. 3.6) and
     * {@code rsTijdTotaal} would weigh as if it did; and a rate whose denominator is another unit, which the reference
     * interpreter integrates through the declared factors and {@code rsTijdTotaal} would weigh unit-blind (the executor
     * note of ch. 3.6); no golden has it.
     */
    private Member integrable(Member member, Object description) {
        if (!member.datatype().isNumber())
            throw new Refusal(DroolsCode.TEMPORAL_TOTAL_NON_NUMBER.reason("description", description, "member", member.name()));
        Unit e = member.datatype().unitOf();
        if (e == null || e.denominator() == null) return member;
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
            throw new Refusal(DroolsCode.TEMPORAL_TOTAL_RATE_UNIT.reason("description", description, "member", member.name(), "unit", e.rendering(), "reason", withoutGridUnit));
        if (!grid.equals(TimeCalculation.chronoUnit(e.denominator())))
            throw new Refusal(DroolsCode.RATE_DENOMINATOR_NOT_GRID.reason("description", description, "member", member.name(), "rate", e.rendering(), "grid", member.time()));
        return member;
    }

    /**
     * Whether the document declares the unit of the grid {@code g}, which a rate over that grid integrates against, as
     * the reference interpreter resolves it once per fold (RUNTIME-SEMANTICS ch. 3.6, "Units, the explicit rate
     * operand"): among the units the unit systems declare without a denominator, those whose abbreviation the calendar
     * table spells as the grid's unit ({@code grid}, as {@link TimeCalculation#chronoUnit} names it), counted once per
     * canonical chain, the canonical unit and the exact factor. Null where there is exactly one chain; otherwise the
     * reference interpreter's reason, that the document declares none or several.
     */
    private String gridUnit(RegelSpraakModel.Granularity g, String grid) {
        record Chain(String canonical, RegelSpraakModel.UnitSystem.Factor factor) {}
        Set<Chain> chains = new LinkedHashSet<>();
        for (RegelSpraakModel.UnitSystem s : model.unitSystems.values())
            for (Map.Entry<String, Unit> u : s.units.entrySet())
                if (u.getValue().denominator() == null && grid.equals(TimeCalculation.chronoUnit(u.getValue())))
                    chains.add(new Chain(u.getValue().canonical() == null ? null : Namen.norm(u.getValue().canonical()),
                            s.exactFactors.get(u.getKey())));
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
     * fault that names the first such member in fold order ({@link MemberFault}, ch. 3.6, 8.4); then {@code outcome}, the
     * fold's value, whose fallback the reference interpreter evaluates after the members ({@link Term.FoldRead}). A
     * deposit for a member after the one that faults, which the reference interpreter does not reach, is heard as well:
     * the session faults at the fixpoint, so it changes no outcome.
     */
    private static Term readFold(Fold v, int[] i, Reads reads, Term outcome, String source) {
        if (v.canFail() || v.valueCanFail()) reads.markFail();
        int depositFlag = v.deposits() ? reads.depositFlag() : -1;
        if (!v.canFail() && !v.valueCanFail() && depositFlag < 0) return outcome;
        return new Term.FoldRead(outcome, depositFlag < 0 ? -1 : i[3], depositFlag, v.canFail() ? i[2] : -1,
                v.valueCanFail() ? i[4] : -1, source);
    }

    /** The fold of {@code f}, an aggregation or a quantifier fold, on the subject of {@code reads}, the rule that reads
     *  it, built by {@code build} from its key and its name the first time it is read. While it is built, the binders in
     *  scope in {@code reads} are those its contribution rules see from outside ({@link #foldBinders}). */
    private Fold foldFor(Object f, Reads reads, BiFunction<String, String, Fold> build) {
        Map<ObjectType, Fold> perSubject = folds.computeIfAbsent(f, k -> new IdentityHashMap<>());
        Fold v = perSubject.get(reads.subject);
        if (v == null) {
            String name = "vouw " + foldCounter++;
            Set<String> outside = foldBinders;
            boolean outerOnForm = foldOnForm;
            foldBinders = reads.binderRange();
            // the fold's subject is the form's where the rule that reads it runs on the form's subject, or reads it in an
            // anchored node (IR-SPEC ch. 4.1a)
            foldOnForm = !reads.contribution || reads.atForm;
            memberwise++;
            try {
                v = build.apply(form + ": " + name, name);
            } finally {
                memberwise--;
                foldBinders = outside;
                foldOnForm = outerOnForm;
            }
            perSubject.put(reads.subject, v);
            for (RulePlan r : v.rules()) foldOfRule.put(r.name(), f);
        }
        return v;
    }

    /** The count over an instance-set union (RUNTIME-SEMANTICS ch. 3.4d): a contribution rule per selection, each with
     *  its own relation and its own filter, all under one key; a member two selections select gives two equal
     *  contributions, which merge into one fact, so the count is that of the union. */
    private Fold unionFold(Expr.FunctionCall f, Expr.InstanceSetUnion u, Reads reads) {
        ObjectType subject = reads.subject;
        // one count answers a number of instances of one declared type: two populations leave whether two selections
        // share an instance to the subtype lattice, which the reference interpreter faults on (WF-110 (d))
        ObjectType population = null;
        for (Expr.InstanceSetUnion.Branch t : u.branches()) {
            if (t.elementType() == null) continue;
            if (population == null) population = t.elementType();
            else if (t.elementType() != population)
                throw new Refusal(DroolsCode.UNION_TWO_POPULATIONS.reason("population", population.name, "elementType", t.elementType().name, "snippet", snippet(u.source())));
        }
        return foldFor(f, reads, (key, name) -> {
            List<RulePlan> rules = new ArrayList<>();
            List<Reads> readings = new ArrayList<>();
            boolean canFail = false, deposits = false;
            List<Condition> filters = new ArrayList<>();
            for (int k = 0; k < u.branches().size(); k++) {
                Expr.InstanceSetUnion.Branch t = u.branches().get(k);
                if (t.elementType() == null)
                    throw new Refusal(DroolsCode.UNION_BRANCH_WITHOUT_TYPE.reason("snippet", snippet(t.source())));
                canFail |= contributionRules(key, name + ", selectie " + k, Expr.FunctionKind.AANTAL, subject,
                        t.elementType(), oneStepRange(t.sourceRole(), subject, t.elementType(), snippet(t.source())), k,
                        t.filter(), null, rules, readings);
                deposits |= deposits(t.filter());
                filters.add(t.filter());
            }
            refuseMixedDeposits(name, filters);
            return new Fold(key, List.copyOf(rules), List.copyOf(readings), canFail, deposits);
        });
    }

    /**
     * The contribution rule of one range of the fold {@code key}, planned: the fold's subject bound first, the
     * member's pattern joined to it through {@code range}, the member's reads, the {@code filter} as the test (the member
     * is the filter's subject, RUNTIME-SEMANTICS ch. 3.4a), and a consequence that inserts the member's {@link Contribution}
     * logically: for a count the member; for a filled count the member where its value is gevuld; for the other folds
     * the member's value where it is gevuld, since a leeg member is skipped (ch. 1.2) ({@link Consequence.Input}). Beside
     * it, where the filter can fault, the rule that inserts the member's {@link FilterFault} ({@link #filterFaultRule}),
     * and where the filter can deposit, the rule that inserts the member's {@link FilterDeposit}
     * ({@link #filterDepositRule}); {@code selection} is the number of the selection of an instance-set union, 0 for any
     * other fold. The rules go into {@code out} and their reads into {@code readings}, to be registered with the form;
     * returns whether the filter can fault.
     */
    private boolean contributionRules(String key, String name, Expr.FunctionKind kind, ObjectType subject,
                                   ObjectType elem, Range range, int selection, Condition filter,
                                   Expr.AttributeReference arg, List<RulePlan> out, List<Reads> readings) {
        Reads reads = contributionReads(subject, elem, range.join());
        Set<Integer> read = reads.follow();
        Term testTerm = filter == null ? null : conditionTerm(filter, reads);
        boolean canFail = reads.canFail();
        reads.stop();
        Set<Integer> use = reads.follow();
        int member = reads.member();
        // a member on a timeline contributes its own total over time (RUNTIME-SEMANTICS ch. 3.6, the per-element fold);
        // vouw admits only a sum and a total over one
        Member memberValue = arg == null ? null : declared(elem, arg.attribute(), false).time() == null
                ? member(elem, arg.attribute(), false) : integrable(timeMember(elem, arg.attribute(), false,
                        Msg.drools("PART.desc.member-total", "snippet", snippet(arg.source()))), Msg.drools("PART.desc.member-total-plain"));
        int value = memberValue == null ? -1 : memberValue.time() == null ? reads.own(memberValue) : reads.timeline(memberValue);
        String total = memberValue == null || memberValue.time() == null ? null : memberValue.time().name();
        reads.stop();
        List<LhsItem> items = reads.plan(conditionRole(canFail), testTerm, read, filter);
        Consequence.Input input = kind == Expr.FunctionKind.AANTAL ? new Consequence.Input.Count()
                : new Consequence.Input.MemberValue(value, total, kind == Expr.FunctionKind.AANTAL_GEVULDE);
        String formName = model.emittedRuleName(form);
        out.add(as(RuleOrigin.Role.FOLD_CONTRIBUTION, reads.rulePlan(formName + ": bijdrage aan " + name, items,
                new Consequence.Contribution(FOLD_SUBJECT, key, member, input, List.copyOf(use), reads.size(),
                        Consequence.Insertion.LOGICAL))));
        readings.add(reads);
        if (canFail) out.add(filterFaultRule(key, formName + ": filterfout in " + name, subject, elem, range,
                selection, filter, readings::add));
        if (total != null) out.add(memberFaultRule(key, formName + ": lidfout in " + name, subject, elem, range,
                selection, filter, memberValue, readings::add));
        if (deposits(filter)) out.add(filterDepositRule(key, formName + ": filterdeponering in " + name, subject,
                elem, range, filter, readings::add));
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
    private RulePlan filterDepositRule(String key, String ruleName, ObjectType subject, ObjectType elem,
                                          Range range, Condition filter, Consumer<Reads> recorder) {
        Reads reads = contributionReads(subject, elem, range.join());
        Set<Integer> read = reads.follow();
        Term testTerm = conditionTerm(filter, reads);
        reads.stop();
        if (reads.depositFlagSlot() < 0)
            throw new IllegalStateException("het filter van " + key + " deponeert, maar kreeg geen sonde");
        int member = reads.member();
        List<LhsItem> items = reads.plan(TestItem.Role.DEPOSIT, testTerm, read, filter);
        recorder.accept(reads);
        Set<Integer> use = new TreeSet<>(read);
        use.add(member);
        return as(RuleOrigin.Role.FOLD_FILTER_DEPOSIT, reads.rulePlan(ruleName, items,
                new Consequence.FilterDeposit(FOLD_SUBJECT, key, member, testTerm, reads.depositFlagSlot(),
                        List.copyOf(use), reads.size(), Consequence.Insertion.LOGICAL)));
    }

    /**
     * The rule beside a contribution rule whose filter can fault, planned: that rule's left-hand side planned again,
     * joined per route from the fold's subject with the member's place bound at every to-many step of the route
     * ({@link Range#perRoute}), the test that the filter faults for the member, and a consequence that inserts the
     * member's {@link FilterFault} logically, on the fold's subject, with its place in the fold order: the
     * {@code selection}, then those places ({@link Consequence.FoldPlace}). No {@link Fault}: whether the division happens
     * depends on the form that reads the fold, which faults where its evaluation reaches the fold while such a fact
     * stands ({@link #fold}). The fact goes when a read it rested on changes, as a contribution does. A member whose
     * place the input did not state has no place in the fold order, and the first fault cannot be told without it: the
     * consequence throws, naming the field.
     */
    private RulePlan filterFaultRule(String key, String ruleName, ObjectType subject, ObjectType elem,
                                      Range range, int selection, Condition filter, Consumer<Reads> recorder) {
        List<Consequence.FoldPlace.Step> places = new ArrayList<>();
        Reads reads = contributionReads(subject, elem, l -> places.addAll(range.perRoute().apply(l)));
        Set<Integer> read = reads.follow();
        Term testTerm = conditionTerm(filter, reads);
        reads.stop();
        Set<Integer> use = reads.follow();
        int member = reads.member();
        reads.stop();
        use.addAll(read);
        for (Consequence.FoldPlace.Step p : places) {
            use.add(p.count());
            use.add(p.place());
        }
        List<LhsItem> items = reads.plan(TestItem.Role.FAULT, testTerm, read, filter);
        recorder.accept(reads);
        return as(RuleOrigin.Role.FOLD_FILTER_FAULT, reads.rulePlan(ruleName, items,
                new Consequence.FilterFault(FOLD_SUBJECT, key, member, testTerm,
                        new Consequence.FoldPlace(selection, places, Consequence.PlaceReason.FILTER_FAULT),
                        List.copyOf(use), reads.size(), Consequence.Insertion.LOGICAL)));
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
    private RulePlan memberFaultRule(String key, String ruleName, ObjectType subject, ObjectType elem, Range range,
                                   int selection, Condition filter, Member memberValue, Consumer<Reads> recorder) {
        List<Consequence.FoldPlace.Step> places = new ArrayList<>();
        Reads reads = contributionReads(subject, elem, l -> places.addAll(range.perRoute().apply(l)));
        Set<Integer> read = reads.follow();
        // the member's filter holds, without its fault where it can fault: the filter fault rule takes that fault
        List<Term> bucket = new ArrayList<>();
        if (filter != null) {
            Term filterTest = conditionTerm(filter, reads);
            bucket.add(reads.canFail() ? new Term.WithoutFault(filterTest, source(filter.source())) : filterTest);
        }
        int value = reads.timeline(memberValue);
        reads.stop();
        int member = reads.member();
        bucket.add(bucketScalar(value, true));
        List<LhsItem> items = reads.plan(TestItem.Role.VORM, all(bucket), read, filter);
        recorder.accept(reads);
        return as(RuleOrigin.Role.FOLD_MEMBER_FAULT, reads.rulePlan(ruleName, items,
                new Consequence.MemberFault(FOLD_SUBJECT, key, member,
                        new Consequence.FoldPlace(selection, places, Consequence.PlaceReason.MEMBER_FAULT),
                        "the per-element temporal total of \"" + memberValue.name() + "\" over ", ": the case supplies an"
                        + " asOfDate-bucket scalar, which carries no period of its own; a set fold needs each member's own"
                        + " timeline (ch. 3.6/§7.1, ch. 8.4)", Consequence.Insertion.LOGICAL)));
    }

    /** The reads of a contribution rule, planned: the member is the subject, the fold's subject is bound before it
     *  ({@link #FOLD_SUBJECT}), and {@code range} joins the two. */
    private Reads contributionReads(ObjectType subject, ObjectType elem, Consumer<Reads> range) {
        Reads reads = new Reads(elem);
        reads.outside = foldBinders;
        reads.contribution = true;
        reads.formType = foldOnForm ? subject : null;
        reads.addToHeadPlan(new FactPattern(beanClass(subject), FOLD_SUBJECT, List.of(), List.of(), List.of()));
        range.accept(reads);
        return reads;
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
     *  an accumulate over the {@link ListPlace} facts of field {@code field} on the node the walk over {@code chain}
     *  reaches from {@code node}, a variable a pattern before it binds (the node itself without a chain), which counts
     *  them and takes the least place. The count gives a row where the input stated no place, so the rule still fires
     *  and its consequence names the gap instead of dropping the fault. */
    private Consequence.FoldPlace.Step place(Reads member, String field, String node, List<String> chain) {
        listFields.add(field);
        String p = "$lijstplaats_" + id("plaats");
        String value = "$lijstplaatswaarde_" + id("plaats");
        String itemCount = "$lijstplaatsen_" + id("plaats");
        String least = "$plaats_" + id("plaats");
        Alpha ofField = equal(id("plaats-veld"), "field", field);
        BetaTest ofMember = new BetaTest(id("plaats-lid"), StaticFunction.PLACE_OF, node,
                List.of(constant(List.copyOf(chain))));
        member.addToRangePlan(new Accumulation(new FactPattern(ListPlace.class, p, List.of(ofField), List.of(), List.of(ofMember),
                List.of(Binding.of(value, ListPlace.class, "place"))),
                List.of(new Accumulation.Aggregation(Accumulation.Kind.COUNT, null, itemCount),
                        new Accumulation.Aggregation(Accumulation.Kind.MINIMUM, value, least))));
        int n = member.size();
        return new Consequence.FoldPlace.Step(field, member.allocateSlot(ReadKind.LIST_PLACES, "lijstplaatsen " + n, itemCount),
                member.allocateSlot(ReadKind.LIST_PLACE, "lijstplaats " + n, least));
    }

    /** The range of the fold {@code f} from {@code subject} to {@code elem}: its multi-step path, or its one step. */
    private Range range(Expr.FunctionCall f, ObjectType subject, ObjectType elem) {
        List<RoleBindingRef> path = sourcePaths.get(f);
        if (path != null) return pathRange(path, subject, elem, snippet(f.source()));
        return oneStepRange(roleBindings.get(f), subject, elem, snippet(f.source()));
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
    private Range oneStepRange(RoleBindingRef rb, ObjectType subject, ObjectType elem, Object description) {
        RegelSpraakModel.Role[] pair = FactModelGenerator.boundRolePair(model, rb);
        if (pair == null) throw new Refusal(DroolsCode.RANGE_WITHOUT_ROLE_PAIR.reason("description", description));
        if (rb.factType() != null && createdRelations.contains(Namen.norm(rb.factType())))
            return linkRange(rb, pair[0], description);
        String elemRole = pair[0].name(), backRoleName = pair[1].name();
        boolean forward = !model.toManyRole(subject, elem, elemRole);
        if (forward && model.toManyRole(elem, subject, backRoleName))
            throw new Refusal(DroolsCode.AGGREGATION_SINGLE_ROLE_SIBLING.reason("role", elemRole, "siblingRole", backRoleName, "description", description));
        ObjectType carrier = forward ? subject : elem, target = forward ? elem : subject;
        String role = forward ? elemRole : backRoleName;
        if (!FactModelGenerator.hasReferenceField(model, carrier, role, target))
            throw new Refusal(DroolsCode.JOIN_WITHOUT_BEAN_FIELD.reason("description", description, "reason", FactModelGenerator.noJoinFieldReason(model, carrier, role, target).msg()));
        // forward the fold subject's reference field names the member, else the member's names the fold's subject
        Binding.Field field = new Binding.Field(beanClass(carrier), role);
        Consumer<Reads> join = member -> member.subjectJoin(forward
                ? new Join(id("bereik"), null, FOLD_SUBJECT, field) : new Join(id("bereik"), role, FOLD_SUBJECT));
        String listField = Namen.ident(role, false);
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
    private Range linkRange(RoleBindingRef rb, RegelSpraakModel.Role memberRole, Object description) {
        LinkRelation r = linkRelation(rb, memberRole, description);
        return new Range(member -> member.addToRangePlan(linkExists(r)), member -> {
            throw new Refusal(DroolsCode.CREATED_RELATION_FAILING_FILTER.reason("factType", rb.factType(), "description", description));
        });
    }

    /** A relation a form creates, as a one-step range over it walks it: by its normalised name, and whether the member
     *  fills its first role. */
    private record LinkRelation(String relation, boolean memberIsFirst) {}

    /** The relation a form creates that a one-step range over {@code rb} walks, its member filling {@code memberRole}, for
     *  {@code description}; a fact type that is no relation of two roles is refused. */
    private LinkRelation linkRelation(RoleBindingRef rb, RegelSpraakModel.Role memberRole, Object description) {
        RegelSpraakModel.FactType ft = factType(rb.factType());
        if (ft == null || ft.roles.size() != 2 || ft.reciprocal)
            throw new Refusal(DroolsCode.CREATED_RELATION_NOT_BINARY.reason("description", description, "factType", rb.factType()));
        return new LinkRelation(Namen.norm(ft.name),
                Namen.norm(ft.roles.get(0).name()).equals(Namen.norm(memberRole.name())));
    }

    /** The range over the relation {@code r} a form creates, planned, of a fold's contribution rule and of an
     *  allocation's recipient rule: an {@code exists} of a {@link RelationLink} of the relation that holds the subject at
     *  the member's end and the object the rule binds before the subject ({@link #FOLD_SUBJECT}) at the other. */
    private Exists linkExists(LinkRelation r) {
        String relationLink = "$koppeling_" + id("bereik");
        Alpha ofRelation = equal(id("bereik-koppeling"), "relation", r.relation());
        Join member = new Join(id("bereik-lid"), r.memberIsFirst() ? "first" : "second", SUBJECT);
        Join carrier = new Join(id("bereik-drager"), r.memberIsFirst() ? "second" : "first", FOLD_SUBJECT);
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
    private Range pathRange(List<RoleBindingRef> path, ObjectType subject, ObjectType elem, Object description) {
        for (RoleBindingRef step : path)
            if (step.factType() != null && createdRelations.contains(Namen.norm(step.factType())))
                throw new Refusal(DroolsCode.CREATED_RELATION_MULTI_STEP.reason("factType", step.factType(), "description", description));
        SourcePath.Plan plan = SourcePath.parse(model, subject, path, elem);
        if (plan.refusal() != null) throw new Refusal(plan.refusal().framed("with-description", "description", description));
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
            member.addToRangePlan(new Exists(patterns, patterns.stream().map(FactPattern::name).toList(),
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
    private int attributeRead(Expr.AttributeReference a, Reads reads) {
        if (a.objectType() == null || a.attribute() == null)
            throw new Refusal(DroolsCode.UNRESOLVED_ATTRIBUTE_REFERENCE.reason("snippet", snippet(a.source())));
        List<RoleBindingRef> path = navigationBindings.get(a);
        if (path == null) {
            if (a.objectType() != reads.subject)
                throw new Refusal(DroolsCode.OWN_READ_FOREIGN_SUBJECT.reason("objectType", a.objectType().name, "subject", reads.subject.name, "snippet", snippet(a.source())));
            return reads.own(member(reads.subject, a.attribute(), false));
        }
        Path p = path(reads.subject, path, a.objectType(), Msg.drools("PART.desc.read", "snippet", snippet(a.source())));
        return reads.navigated(p, member(p.reaches(), a.attribute(), false));
    }

    // ---------------- conditions ----------------

    /** The condition {@code c} as a term whose value is a truth value ({@link Term}), with the reads it needs bound in
     *  {@code reads}. Every condition node is named here, so a node the IR records gain later fails to compile until it
     *  is decided whether this branch reads it. */
    private Term conditionTerm(Condition c, Reads reads) {
        if (anchored.contains(c) && !reads.atForm) return reads.atForm(c, () -> conditionTerm(c, reads));
        Term t = switch (c) {
            case Condition.CharacteristicCheck k -> characteristicCheck(k, reads);
            case Condition.IdentityCheck i -> identityCheck(i, reads);
            case Condition.Compound s -> compound(s, reads);
            case Condition.Comparison v -> comparison(v, reads);
            case Condition.Predicate p -> predicate(p, reads);
            case Condition.TimeWindow p -> timeWindow(p, reads);
            case Condition.QuantifierFold q -> quantifierFold(q, reads);
            case Condition.RoleMembership role -> roleCheck(role, reads);
            // the v0 far-side record, which the loader no longer makes: since L2 a role check is one node (IR-SPEC
            // ch. 4.5), so only a hand-built model reaches this
            case Condition.FarRoleMembership role -> throw new Refusal(DroolsCode.FAR_ROLE_CHECK_UNMADE.reason("snippet", snippet(c)));
            case Condition.RuleStatusCheck rv -> channelRead(rv, reads);
            // RUNTIME-SEMANTICS ch. 5.7 and WF-044: the marker fails the evaluation of the form where the evaluation
            // reaches it, so a composite that settles before it reads it plays
            case Condition.Unresolved o -> {
                reads.markFail();
                yield new Term.Fail(ComputeFault.UNRESOLVED_CONDITION, source(o.source()));
            }
        };
        note(c, t);
        return t;
    }

    /** The identity check (IR-SPEC ch. 4.5, WF-122; RUNTIME-SEMANTICS ch. 3.4a): whether the member, the subject of the
     *  filter or the test, is the subject of the form, which the rule of a fold on the subject of the form binds as the
     *  fold's subject, complemented where the check is negated. Neither object is ever leeg, so the check is never leeg
     *  and never a fault. */
    private Term identityCheck(Condition.IdentityCheck i, Reads reads) {
        int form = reads.formSubject(i);
        int member = reads.member();
        return new Term.Call(StaticFunction.SAME_OBJECT, List.of(new Term.SlotValue(member, null),
                new Term.SlotValue(form, null), constant(i.negated())), source(i.source()));
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
    private Term timeWindow(Condition.TimeWindow p, Reads reads) {
        if (p.from() == null && p.until() == null)
            throw new Refusal(DroolsCode.PERIOD_WINDOW_WITHOUT_BOUND.reason("snippet", snippet(p)));
        for (Expr bound : java.util.Arrays.asList(p.from(), p.until()))
            if (bound != null && !WindowPosition.boundLowerable(bound, reads.subject, navigationBindings::containsKey))
                throw new Refusal(WindowPosition.BOUND_FORM.framed("with-snippet", "snippet", snippet(p)));
        Term from = p.from() == null ? null : value(p.from(), reads);
        Term to = p.until() == null ? null : value(p.until(), reads);
        int date = reads.evaluationDate();
        // an absent bound is open where a leeg one never matches, so whether the window has the bound is a constant
        // of its own beside the bound's place
        return new Term.Call(StaticFunction.TIME_WINDOW, List.of(from == null ? constant(null) : from,
                constant(from != null), to == null ? constant(null) : to, constant(to != null),
                constant(p.toInclusive()), new Term.SlotValue(date, null)), source(p.source()));
    }

    /** A characteristic check: of the subject, a bound truth value, complemented where the check is negated; over a
     *  role, the reached object's, and a check over an absent link holds in neither polarity (RUNTIME-SEMANTICS ch. 5.8
     *  rule 3). The standings are total, so no negation is needed here. Under the whole-period qualifier the check
     *  folds the characteristic's true periods instead ({@link #wholePeriodCharacteristicCheck}). */
    private Term characteristicCheck(Condition.CharacteristicCheck k, Reads reads) {
        // WF-109: the host is satisfied extensionally; a host the subject is no instance of names another object, which
        // a rule condition does not bind
        if (!Host.hostOp(k, reads.subject))
            throw new Refusal(DroolsCode.CHARACTERISTIC_HOST_NOT_SUBJECT.reason("snippet", snippet(k)));
        if (k.period() != null) {
            refuseAnchoredUnderWholePeriod(k, reads);
            return wholePeriodCharacteristicCheck(k, reads);
        }
        boolean desired = !k.negation();
        List<RoleBindingRef> nav = navigationBindings.get(k);
        if (nav == null) {
            if (k.navigation() != null)
                throw new Refusal(DroolsCode.NAVIGATION_WITHOUT_BINDING.reason("snippet", snippet(k)));
            int i = reads.own(member(reads.subject, k.characteristic(), true));
            return new Term.Call(StaticFunction.KENMERK, List.of(new Term.SlotValue(i, null), constant(desired)),
                    source(k.source()));
        }
        Path p = path(reads.subject, nav, null, Msg.drools("PART.desc.characteristic-check", "snippet", snippet(k)));
        int i = reads.navigated(p, member(p.reaches(), k.characteristic(), true));
        return new Term.Call(StaticFunction.CHARACTERISTIC_CHECK_ALONG_PATH, List.of(new Term.SlotValue(i, null), constant(desired)),
                source(k.source()));
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
    private Term wholePeriodCharacteristicCheck(Condition.CharacteristicCheck k, Reads reads) {
        Msg description = Msg.drools("PART.desc.whole-period-characteristic-check", "snippet", snippet(k));
        if (k.negation())
            throw new Refusal(DroolsCode.WHOLE_PERIOD_CHARACTERISTIC_NEGATION.reason("description", description));
        List<RoleBindingRef> nav = navigationBindings.get(k);
        if (nav == null) {
            if (k.navigation() != null) throw new Refusal(DroolsCode.WHOLE_PERIOD_UNBOUND_NAVIGATION.reason("description", description));
            return wholePeriod(k.period(), new Term.SlotValue(reads.timeline(timeMember(reads.subject, k.characteristic(), true, description)),
                    null), CHARACTERISTIC_TEST, null, source(k.source()));
        }
        Path p = path(reads.subject, nav, null, description);
        return wholePeriod(k.period(), new Term.SlotValue(reads.navigated(p, timeMember(p.reaches(), k.characteristic(), true, description)),
                null), CHARACTERISTIC_TEST, null, source(k.source()));
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
    private Term wholePeriodComparison(Condition.Comparison v, Reads reads) {
        Msg description = Msg.drools("PART.desc.whole-period-comparison", "snippet", snippet(v));
        for (Expr side : java.util.Arrays.asList(v.left(), v.right()))
            if (side instanceof Expr.AttributeReference a && navigationBindings.containsKey(a) && a.objectType() != null
                    && a.attribute() != null && declared(a.objectType(), a.attribute(), false).time() != null)
                throw new Refusal(DroolsCode.PERIOD_TIMELINE_ALONG_PATH.reason("description", description, "snippet", snippet(a.source())));
        boolean left = ownTimeline(v.left(), reads), right = ownTimeline(v.right(), reads);
        if (left == right)
            throw new Refusal(DroolsCode.PERIOD_COMPARISON_ONE_SIDE.reason("description", description, "sideDescription", DroolsCode.PERIOD_COMPARISON_ONE_SIDE.part(left ? "between-timelines" : "one-sided")));
        Expr.AttributeReference time = (Expr.AttributeReference) (left ? v.left() : v.right());
        Expr boundExpr = left ? v.right() : v.left();
        Integer testCode = TimeCalculation.test(left ? v.op() : Operators.reverse(v.op()));
        if (testCode == null)
            throw new Refusal(DroolsCode.WHOLE_PERIOD_DATE_COMPARISON.reason("description", description));
        Member member = timeMember(reads.subject, time.attribute(), false, description);
        if (!member.datatype().isNumberLike() || !eff.typeOf(boundExpr).isNumberLike())
            throw new Refusal(DroolsCode.PERIOD_NON_NUMERIC_COMPARE.reason("description", description));
        int timeline = reads.timeline(member);
        Term bound = restated(value(boundExpr, reads), eff.unitOf(boundExpr), eff.unitOf(time),
                "een §8.2 tijdlijngrens");
        // after the bound, where it always stood: it takes the slot of the subject, and the slots keep their numbers
        Term whole = fullTimeline(timeline, member, null, reads, source(time.source()));
        note(time, whole);
        return wholePeriod(v.period(), whole, testCode, bound, source(v.source()));
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
    private Term wholePeriodPredicate(Condition.Predicate p, Reads reads) {
        Msg description = Msg.drools("PART.desc.whole-period-predicate", "snippet", snippet(p));
        boolean filled = switch (p.kind()) {
            case LEEG -> p.negation();
            case GEVULD -> !p.negation();
            default -> throw new Refusal(DroolsCode.WHOLE_PERIOD_PREDICATE_KIND.reason("description", description, "spelling", Spelling.PREDICATE_KIND.spelling(p.kind())));
        };
        if (!(p.operand() instanceof Expr.AttributeReference a) || a.objectType() == null || a.attribute() == null)
            throw new Refusal(DroolsCode.PERIOD_PREDICATE_NON_ATTRIBUTE.reason("description", description));
        int testCode = filled ? FILLED_TEST : TEST_EMPTY;
        List<RoleBindingRef> path = navigationBindings.get(a);
        if (path == null) {
            if (a.objectType() != reads.subject)
                throw new Refusal(DroolsCode.PERIOD_PREDICATE_FOREIGN_READ.reason("description", description, "objectType", a.objectType().name, "subject", reads.subject.name));
            Member member = timeMember(reads.subject, a.attribute(), false, description);
            Term whole = fullTimeline(reads.timeline(member), member, null, reads, source(a.source()));
            note(a, whole);
            return wholePeriod(p.period(), whole, testCode, null, source(p.source()));
        }
        Path q = path(reads.subject, path, a.objectType(), description);
        Member member = timeMember(q.reaches(), a.attribute(), false, description);
        Term whole = fullTimeline(reads.navigated(q, member), member, q, reads, source(a.source()));
        note(a, whole);
        return wholePeriod(p.period(), whole, testCode, null, source(p.source()));
    }

    /**
     * The timeline of the attribute {@code member} bound at slot {@code timeline}, which the whole-period fold reads whole,
     * or the fault where the case gives its input as the asOfDate-bucket scalar ({@link BucketScalar},
     * {@link StaticFunction#COMPLETE}): ch. 8.4 makes a form that needs the one convention and finds the other fault, never
     * guess. The fault names the object by the subject of the rule, through the roles of {@code path} where the attribute
     * is read over one ("the werf of "S1""). Registers that the rule's evaluation can fault. The reference interpreter
     * and the ASP backend read such an input as no timeline, which passes (measured 2026-10-09).
     */
    private static Term fullTimeline(int timeline, Member member, Path path, Reads reads, String source) {
        int subjectType = reads.member();
        reads.markFail();
        StringBuilder along = new StringBuilder();
        if (path != null) for (int i = path.roles().size() - 1; i >= 0; i--)
            along.append("the ").append(path.roles().get(i)).append(" of ");
        String before = "the whole-period qualifier over \"" + member.name() + "\" needs the full timeline; " + along;
        return complete(timeline, before, subjectType, " supplies only the asOfDate-bucket scalar (ch. 3.9, ch. 8.4)", source);
    }

    /** Whether {@code e} reads an attribute on a timeline of this rule's subject itself: an attribute reference without
     *  a path, naming the subject's type, whose declaration on that type's chain lies on a timeline. */
    private boolean ownTimeline(Expr e, Reads reads) {
        return e instanceof Expr.AttributeReference a && a.objectType() == reads.subject && a.attribute() != null
                && !navigationBindings.containsKey(a) && declared(reads.subject, a.attribute(), false).time() != null;
    }

    /** Refuses the whole-period qualifier on {@code node} where it stands in an anchored node or one of its
     *  {@code operands} is anchored (IR-SPEC ch. 4.1a): the forms of the qualifier read the timeline of the rule's own
     *  subject by the shape of the operand, which an anchor moves to the subject of the form; as in the ASP backend this
     *  branch does not carry it. */
    private void refuseAnchoredUnderWholePeriod(Condition node, Reads reads, Expr... operands) {
        boolean anchoredOperand = false;
        for (Expr o : operands) anchoredOperand |= anchored.contains(o);
        if (reads.atForm || anchoredOperand)
            throw new Refusal(DroolsCode.ANCHORED_FORM_NOT_CARRIED.reason("snippet", snippet(node)));
    }

    /** The tests of {@code rsGeheelPeriode}, by its own codes: a comparison takes the code {@link TimeCalculation#test}
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
     * {@code bound} is null where the test has no bound.
     */
    private static Term wholePeriod(Condition.WholePeriod qualifier, Term timeline, int testCode, Term bound,
                                      String source) {
        return new Term.Call(StaticFunction.WHOLE_PERIOD, List.of(timeline, bound == null ? constant(null) : bound,
                constant(qualifier.year() ? "JAAR" : "MAAND"), constant(testCode), constant(qualifier.negated())), source);
    }

    /**
     * A role check (RUNTIME-SEMANTICS ch. 5.8a, IR-SPEC ch. 4.5): whether a link of the fact type holds the subject in
     * the role the check names and, where it names a counterpart type, an instance of that type at the other end, a
     * class test that a subtype passes; negated, whether no such link exists. An existence question, never a count and
     * never a pick, bound as a value the test reads, so the check stands in a composite as any condition does. A
     * relation a form creates is read over its {@link RelationLink} facts, which its creations state while the session
     * fires and which the input's links insert logically; any other relation is input and is read where the bean model
     * stores it ({@link FactModelGenerator#membershipStorage}): a to-one other role is the subject's own reference
     * field, read off the subject's bean, and a to-many other role is the field of the other end, which names the
     * subject, counted over the beans of the counterpart's class. Both roles to-many leaves no field, and the check is
     * refused in the sentence of {@link FactModelGenerator#noReferenceFieldReason}, on both branches.
     */
    private Term roleCheck(Condition.RoleMembership r, Reads reads) {
        RoleBindingRef rb = roleBindings.get(r);
        RegelSpraakModel.Role[] pair = FactModelGenerator.boundRolePair(model, rb);
        if (pair == null)
            throw new Refusal(DroolsCode.ROLE_MEMBERSHIP_UNDECLARED_ROLE.reason("snippet", snippet(r), "binding", rb == null ? DroolsCode.ROLE_MEMBERSHIP_UNDECLARED_ROLE.part("none") : rb.factType() + ", " + rb.role()));
        ObjectType counterpart = dev.uchar.regelspraak.loader.IrLoader.counterpart(r);
        Class<?> javaClass = counterpart == null ? null : beanClass(counterpart);
        boolean negated = r.negation();
        if (rb.factType() != null && createdRelations.contains(Namen.norm(rb.factType()))) {
            RegelSpraakModel.FactType ft = factType(rb.factType());
            if (ft == null || ft.roles.size() != 2 || ft.reciprocal)
                throw new Refusal(DroolsCode.ROLE_MEMBERSHIP_NOT_BINARY.reason("snippet", snippet(r), "factType", rb.factType()));
            boolean subjectIsFirst = Namen.norm(ft.roles.get(0).name()).equals(Namen.norm(pair[0].name()));
            int i = reads.roleLinks(Namen.norm(ft.name), subjectIsFirst, javaClass);
            return new Term.Call(StaticFunction.ROLE_COUNT, List.of(new Term.SlotValue(i, null), constant(negated)),
                    source(r.source()));
        }
        FactModelGenerator.MembershipStorage o = FactModelGenerator.membershipStorage(model, rb);
        if (o.refusal() != null) throw new Refusal(DroolsCode.ROLE_MEMBERSHIP_STORAGE_REFUSED.reason("snippet", snippet(r), "reason", o.refusal()));
        if (o.opSubject()) {
            if (!FactModelGenerator.hasReferenceField(model, reads.subject, o.beanField(), o.otherType()))
                throw new Refusal(DroolsCode.ROLE_MEMBERSHIP_NO_FIELD.reason("snippet", snippet(r), "reason", FactModelGenerator.noReferenceFieldReason(model, reads.subject, o.beanField(), o.otherType()).msg()));
            int i = reads.roleField(o.beanField(), javaClass);
            return new Term.Call(StaticFunction.ROLE_FIELD, List.of(new Term.SlotValue(i, null), constant(negated)),
                    source(r.source()));
        }
        ObjectType filler = counterpart != null ? counterpart : o.otherType();
        if (!FactModelGenerator.hasReferenceField(model, filler, o.beanField(), reads.subject))
            throw new Refusal(DroolsCode.ROLE_MEMBERSHIP_NO_FIELD.reason("snippet", snippet(r), "reason", FactModelGenerator.noReferenceFieldReason(model, filler, o.beanField(), reads.subject).msg()));
        int i = reads.roleFillers(beanClass(filler), o.beanField());
        return new Term.Call(StaticFunction.ROLE_COUNT, List.of(new Term.SlotValue(i, null), constant(negated)),
                source(r.source()));
    }

    /** A composite folds its members in wire order and stops at the first member after which the verdict stands
     *  (RUNTIME-SEMANTICS ch. 5.10), so a member that would fault, a comparison with a division by zero, is not read
     *  where an earlier member settled the verdict, and the composite faults only where it reads such a member. */
    private Term compound(Condition.Compound s, Reads reads) {
        // a window under GEEN or a counting quantifier has no reading (WF-112), refused in the sentence both other
        // branches give
        Reason window = WindowPosition.refusal(s);
        if (window != null) throw new Refusal(window.framed("with-snippet", "snippet", snippet(s)));
        List<Term> members = new ArrayList<>();
        for (Condition part : s.components()) members.add(conditionTerm(part, reads));
        return new Term.Compound(s.quantifier(), s.threshold(), members, source(s.source()));
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
     * {@code sourceRole}, or a path, {@code sourcePath}, planned as a value fold's ({@link #pathRange}): a member that
     * two routes reach contributes once (RUNTIME-SEMANTICS ch. 3.4b), which is what the counting quantifiers count.
     */
    private Term quantifierFold(Condition.QuantifierFold kv, Reads reads) {
        // WF-112, in the sentence of WindowPosition, on both branches: a window in the filter or the test under none
        // or a counting quantifier has no reading
        Reason window = WindowPosition.refusal(kv);
        if (window != null) throw new Refusal(window.framed("with-snippet", "snippet", snippet(kv)));
        ObjectType elem = kv.elementObjectType();
        if (elem == null) throw new Refusal(DroolsCode.QUANTIFIER_FOLD_UNRESOLVED_ELEMENT.reason("snippet", snippet(kv)));
        if (kv.test() == null)
            throw new Refusal(DroolsCode.QUANTIFIER_FOLD_WITHOUT_MATRIX.reason("snippet", snippet(kv)));
        if (kv.quantifier() == Condition.Quantifier.DE)
            throw new Refusal(DroolsCode.QUANTIFIER_DE_SINGLE_MEMBER.reason("snippet", snippet(kv)));
        Condition.Quantifier kw = kv.quantifier();
        int threshold = kv.threshold();
        Fold v = foldFor(kv, reads, (key, name) -> quantifierContributions(kv, key, name, reads.subject, elem));
        String source = source(kv.source());
        if (!v.ordered()) {
            int[] i = reads.fold(v, Accumulation.Kind.SUM);
            return readFold(v, i, reads, new Term.Call(StaticFunction.QUANTIFIER_VERDICT, List.of(new Term.SlotValue(i[0], null),
                    new Term.SlotValue(i[1], null), constant(kw), constant(threshold)), source), source);
        }
        int[] i = reads.fold(v, Accumulation.Kind.LIST);
        if (v.memberCanFail()) reads.markFail();
        int depositFlag = v.memberDeposits() ? reads.depositFlag() : -1;
        return readFold(v, i, reads, new Term.QuantifierRead(StaticFunction.QUANTIFIER_READ, new Term.SlotValue(i[0], null),
                new Term.SlotValue(i[1], null), kw, threshold, depositFlag, source), source);
    }

    /**
     * The contribution rules of the quantifier fold {@code kv} on {@code subject}, planned: one over its range, the
     * fold's subject bound first and the member joined to it, the member's reads of the filter and of the matrix bound,
     * the filter as the test (with the role {@link TestItem.Role#WITHOUT_FAULT} where it can fault), and a consequence
     * that evaluates the matrix for the member and inserts the member's {@link Contribution} logically: 1 or 0 for its
     * verdict ({@link Consequence.Input.Verdict}), or, where the matrix can fault or deposit, its {@link MemberTest}
     * ({@link Consequence.Input.MemberTest}), with the member's place bound as the filter fault rule binds it
     * ({@link Range#perRoute}) and the rule's probe. Beside it the filter fault rule and the filter deposit rule of a
     * filter that can fault or deposit, as for any fold ({@link #contributionRules}). A made link has no place in the order
     * of the members, so a matrix that can fault or deposit over a relation a form creates is refused. Over a path the
     * range is {@link #pathRange}'s; where a member can be reached along more than one route, the contribution rule
     * of a matrix that can fault or deposit would bind the member once per route, each at its own place, and the
     * replay ({@link ComputeHelpers#quantifierRead}) would read it as often, where the reference interpreter reads it
     * once, at its first route (RUNTIME-SEMANTICS ch. 3.4b, 5.11): that shape is refused.
     */
    private Fold quantifierContributions(Condition.QuantifierFold kv, String key, String name, ObjectType subject,
                                  ObjectType elem) {
        String description = snippet(kv);
        boolean path = !kv.sourcePath().isEmpty();
        Range range = path ? pathRange(kv.sourcePath(), subject, elem, description)
                : oneStepRange(kv.sourceRole(), subject, elem, description);
        // the member's reads first and its join after: whether the matrix can fault or deposit decides whether the join
        // binds the member's place
        Reads reads = contributionReads(subject, elem, l -> { });
        Set<Integer> read = reads.follow();
        Term filter = kv.filter() == null ? null : conditionTerm(kv.filter(), reads);
        boolean filterCanFail = reads.canFail();
        reads.stop();
        Set<Integer> use = reads.follow();
        int member = reads.member();
        Term matrix = conditionTerm(kv.test(), reads);
        boolean memberCanFail = reads.canFail();
        reads.stop();
        boolean memberDeposits = deposits(kv.test());
        int depositFlag = reads.depositFlagSlot();
        boolean ordered = memberCanFail || memberDeposits;
        List<Consequence.FoldPlace.Step> places = new ArrayList<>();
        if (ordered) {
            if (path && !SourcePath.parse(model, subject, kv.sourcePath(), elem).meets())
                throw new Refusal(DroolsCode.QUANTIFIER_MEMBER_TWO_ROUTES.reason("description", description));
            if (!path && kv.sourceRole().factType() != null
                    && createdRelations.contains(Namen.norm(kv.sourceRole().factType())))
                throw new Refusal(DroolsCode.QUANTIFIER_MEMBER_CREATED_RELATION.reason("factType", kv.sourceRole().factType(), "description", description));
            places.addAll(range.perRoute().apply(reads));
            for (Consequence.FoldPlace.Step p : places) {
                use.add(p.count());
                use.add(p.place());
            }
        } else range.join().accept(reads);
        List<LhsItem> items = reads.plan(conditionRole(filterCanFail), filter, read, kv.filter());
        Consequence.Input input = ordered
                ? new Consequence.Input.MemberTest(matrix, depositFlag, new Consequence.FoldPlace(0, places,
                        Consequence.PlaceReason.QUANTIFIER))
                : new Consequence.Input.Verdict(matrix);
        String formName = model.emittedRuleName(form);
        List<RulePlan> rules = new ArrayList<>();
        List<Reads> readings = new ArrayList<>();
        rules.add(as(RuleOrigin.Role.FOLD_CONTRIBUTION, reads.rulePlan(formName + ": bijdrage aan " + name, items,
                new Consequence.Contribution(FOLD_SUBJECT, key, member, input, List.copyOf(use), reads.size(),
                        Consequence.Insertion.LOGICAL))));
        readings.add(reads);
        if (filterCanFail) rules.add(filterFaultRule(key, formName + ": filterfout in " + name, subject, elem,
                range, 0, kv.filter(), readings::add));
        boolean filterDeposits = deposits(kv.filter());
        if (filterDeposits) rules.add(filterDepositRule(key, formName + ": filterdeponering in " + name,
                subject, elem, range, kv.filter(), readings::add));
        refuseMixedDeposits(name, Collections.singletonList(kv.filter()));
        return new Fold(key, List.copyOf(rules), List.copyOf(readings), filterCanFail, filterDeposits, memberCanFail,
                memberDeposits);
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
    private Term comparison(Condition.Comparison v, Reads reads) {
        if (v.period() != null) {
            refuseAnchoredUnderWholePeriod(v, reads, v.left(), v.right());
            return wholePeriodComparison(v, reads);
        }
        Datatype lt = eff.typeOf(v.left()), rt = eff.typeOf(v.right());
        Condition.Operator op = v.op();
        Term leftTerm = value(v.left(), reads), rightTerm = value(v.right(), reads);
        String source = source(v.source());
        if (lt.isNumberLike() && rt.isNumberLike() && !isDateOperator(op)) {
            Term right = restated(rightTerm, eff.unitOf(v.right()), eff.unitOf(v.left()),
                    "een §5.1 vergelijking");
            return new Term.Call(StaticFunction.RS_AGG_COMPARE, List.of(leftTerm, right,
                    constant(Operators.symbol(op))), source);
        }
        if (lt.isDate() && rt.isDate() && (isDateOperator(op) || op == Condition.Operator.GELIJK
                || op == Condition.Operator.ONGELIJK)) {
            if (!sameDateKind(lt, rt))
                throw new Refusal(DroolsCode.COMPARISON_DATE_WITH_DATETIME.reason("snippet", snippet(v)));
            return new Term.Call(StaticFunction.DATE_COMPARE, List.of(leftTerm, rightTerm, constant(op)), source);
        }
        if ((op == Condition.Operator.GELIJK || op == Condition.Operator.ONGELIJK) && equalityType(lt)
                && equalityType(rt) && lt.isBoolean() == rt.isBoolean()) {
            boolean enumeration = lt instanceof Datatype.Enumeration || rt instanceof Datatype.Enumeration;
            Term left = enumeration ? enumName(leftTerm) : leftTerm;
            Term right = enumeration ? enumName(rightTerm) : rightTerm;
            return new Term.Call(StaticFunction.TEXT_EQUAL, List.of(left, right,
                    constant(op == Condition.Operator.GELIJK)), source);
        }
        throw new Refusal(DroolsCode.COMPARISON_INCOMPATIBLE_TYPES.reason("spelling", Spelling.COMPARISON_OP.spelling(op), "leftType", typeName(lt), "rightType", typeName(rt), "snippet", snippet(v)));
    }

    /** {@code t}, an enumeration label or leeg, up to its normalisation ({@link StaticFunction#RS_ENUM_NAME}); the call has no
     *  node of its own in the IR and carries the fragment of the operand it normalises, as a restatement does. */
    private static Term enumName(Term t) {
        return new Term.Call(StaticFunction.RS_ENUM_NAME, List.of(t), t.source());
    }

    private static String typeName(Datatype t) {
        return t == null ? "onbekend" : t.getClass().getSimpleName();
    }

    private static boolean equalityType(Datatype t) {
        return t != null && (t.isText() || t instanceof Datatype.Enumeration || t.isBoolean());
    }

    private static boolean isDateOperator(Condition.Operator op) {
        return switch (op) {
            case EERDER, EERDER_OF_GELIJK, LATER, LATER_OF_GELIJK -> true;
            default -> false;
        };
    }

    private static boolean sameDateKind(Datatype a, Datatype b) {
        if (a instanceof Datatype.DateTime) return b instanceof Datatype.DateTime;
        return a instanceof Datatype.DateInDays && b instanceof Datatype.DateInDays;
    }

    /** A leeg or gevuld test of a value; a read over an absent link is leeg (RUNTIME-SEMANTICS ch. 5.8 rule 1). The loader
     *  writes a negated test as its sister test. Under the whole-period qualifier the test folds a timeline instead
     *  ({@link #wholePeriodPredicate}). */
    private Term predicate(Condition.Predicate p, Reads reads) {
        if (p.period() != null) {
            refuseAnchoredUnderWholePeriod(p, reads, p.operand());
            return wholePeriodPredicate(p, reads);
        }
        switch (p.kind()) {
            case LEEG, GEVULD -> {
                Term operand = value(p.operand(), reads);
                return new Term.Call(StaticFunction.EMPTY_TEST, List.of(operand,
                        constant(p.kind() == Condition.PredicateKind.LEEG)), source(p.source()));
            }
            case ELFPROEF, NUMERIEK_MET_EXACT -> {
                return digitTrial(p, reads);
            }
            // a day kind is consulted as a classifier of a bare date before the rule graph, never as a read inside the
            // fixpoint (ch. 5.4); inside a day-kind definition the test is the definition's own (dagsoortpredikaat)
            case DAGSOORT -> throw new Refusal(DroolsCode.DAY_KIND_PREDICATE_OUTSIDE.reason("snippet", snippet(p)));
            default -> throw new IllegalStateException("predicaatsoort " + p.kind());
        }
    }

    /** The eleven test and the exact-digit test (§8.1.3, §8.1.4), through the runtime's
     *  {@code rsElfproef} and {@code rsNumeriekMetExact}, the written negation complementing the verdict: a leeg value
     *  passes neither test. The operand is read as any value, in which an absent link over a path of roles is leeg
     *  (RUNTIME-SEMANTICS ch. 5.8, rule 1), so the test cannot tell an absent link from a leeg value there; such an
     *  operand is refused by name. The reference interpreter and the ASP backend do not evaluate these predicates. */
    private Term digitTrial(Condition.Predicate p, Reads reads) {
        if (p.operand() instanceof Expr.AttributeReference a && navigationBindings.containsKey(a))
            throw new Refusal(DroolsCode.PREDICATE_OVER_NAVIGATED_PATH.reason("spelling", Spelling.PREDICATE_KIND.spelling(p.kind()), "snippet", snippet(p)));
        Term operand = value(p.operand(), reads);
        boolean desired = !p.negation();
        if (p.kind() == Condition.PredicateKind.ELFPROEF)
            return new Term.Call(StaticFunction.ELEVEN_TEST, List.of(operand, constant(desired)), source(p.source()));
        return new Term.Call(StaticFunction.NUMERIEK_MET_EXACT, List.of(operand, constant(p.digits()), constant(desired)),
                source(p.source()));
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
        /** The rule's subject; while an anchored node compiles ({@link #atForm}) the subject of the form. */
        ObjectType subject;
        /** Whether this is the reads of a rule of a fold ({@link #contributionReads}), whose subject is a member and which
         *  binds the fold's subject before it ({@link #FOLD_SUBJECT}). */
        private boolean contribution;
        /** In a rule of a fold on the subject of the form, which {@link #FOLD_SUBJECT} binds, the type of that subject:
         *  an anchored node is read there (IR-SPEC ch. 4.1a, WF-122). Null in every other rule. */
        private ObjectType formType;
        /** Whether an anchored node compiles: its reads join or bind {@link #FOLD_SUBJECT} instead of the subject. */
        private boolean atForm;
        /** While an anchored node compiles, that node, which a refusal of a form inside it names. */
        private Object formNode;
        /** The values bound off the bean of the form's subject ({@link #FOLD_SUBJECT}) while an anchored node compiles:
         *  its input, which does not change while the session fires, on the pattern that binds it. */
        private final List<Binding> formBindings = new ArrayList<>();
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
        private final Map<ElementKey, Member> derived = new LinkedHashMap<>();
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
        Reads(ObjectType subject) {
            this.subject = subject;
        }

        /** The binders in scope where the expression being compiled stands: this rule's own and {@link #outside}. */
        Set<String> binderRange() {
            if (binders.isEmpty()) return outside;
            Set<String> out = new LinkedHashSet<>(outside);
            out.addAll(binders.keySet());
            return out;
        }

        /** The subject itself as a bound value, for a consequence that hands on another object beside it. Refused by
         *  name in an anchored node: a form that takes the subject itself, such as the total over the own timeline,
         *  reads it off this rule's subject only, and a fold on the subject of the form binds that subject as
         *  {@link #FOLD_SUBJECT}. */
        int member() {
            if (atForm) throw new Refusal(DroolsCode.ANCHORED_FORM_NOT_CARRIED.reason("snippet", snippetOf(formNode)));
            int known = existing("onderwerp zelf");
            return known >= 0 ? known : allocateSlot(ReadKind.SUBJECT, "onderwerp zelf", SUBJECT);
        }

        /** The subject of the form itself as a bound value, for the identity check (IR-SPEC ch. 4.5, WF-122): the fold's
         *  subject, which this rule of a fold on the subject of the form binds. Refused, naming {@code node}, in a rule
         *  that binds no subject of the form. */
        int formSubject(Object node) {
            if (formType == null || atForm) throw new Refusal(DroolsCode.ANCHOR_FORM_SUBJECT_UNBOUND.reason("snippet", snippetOf(node)));
            int known = existing("vormonderwerp zelf");
            return known >= 0 ? known : allocateSlot(ReadKind.FORM_SUBJECT, "vormonderwerp zelf", FOLD_SUBJECT);
        }

        /** The term {@code compile} plans for the anchored node {@code node} (IR-SPEC ch. 4.1a, WF-122): planned with the
         *  subject of the form as the subject, as in the form's own condition or value, its reads joined to or bound off
         *  {@link #FOLD_SUBJECT}, under their own keys. Only a rule of a fold on the subject of the form binds it, so an
         *  anchored node in a fold that is itself read per member of another fold is refused by name. */
        Term atForm(Object node, Supplier<Term> compile) {
            if (atForm) return compile.get();
            if (formType == null) throw new Refusal(DroolsCode.ANCHOR_FORM_SUBJECT_UNBOUND.reason("snippet", snippetOf(node)));
            ObjectType outer = subject;
            subject = formType;
            atForm = true;
            formNode = node;
            try {
                return compile.get();
            } finally {
                subject = outer;
                atForm = false;
                formNode = null;
            }
        }

        /** Refuses, naming {@code node}, a read this branch takes only off the rule's own subject, where an anchored node
         *  compiles. */
        private void notAtForm(Object node) {
            if (atForm) throw new Refusal(DroolsCode.ANCHORED_FORM_NOT_CARRIED.reason("snippet", snippetOf(node)));
        }

        /** The variable of the object this rule reads its subject's members off: the fold's subject while an anchored node
         *  compiles, else the subject. */
        private String subjectVariable() {
            return atForm ? FOLD_SUBJECT : SUBJECT;
        }

        /** The join of a standing fact on the object {@link #subjectVariable} names. */
        private Join onSubjectHere(String id) {
            return new Join(id, "subject", subjectVariable());
        }

        /** The planned items before the subject pattern, the pattern of {@link #FOLD_SUBJECT} with the bindings of
         *  {@link #formBindings}. */
        private List<LhsItem> head() {
            if (formBindings.isEmpty()) return headPlan;
            List<LhsItem> out = new ArrayList<>();
            for (LhsItem item : headPlan) {
                if (item instanceof FactPattern f && f.name().equals(FOLD_SUBJECT)) {
                    List<Binding> b = new ArrayList<>(f.bindings());
                    b.addAll(formBindings);
                    out.add(new FactPattern(f.factClass(), f.name(), f.alpha(), f.joins(), f.betaTests(), f.afterAlpha(), b));
                } else out.add(item);
            }
            return out;
        }

        /** The left-hand side of the rule after its subject pattern, once its condition and its value are compiled:
         *  the parameter items, the standing items, and the test of {@code conditionTerm} over the values at
         *  {@code read}, which asks {@code role} of it ({@link TestItem.Role}); {@link #rulePlan} takes them with
         *  what the form adds after them. A test of deposits takes the probe of this rule ({@link #depositFlagSlot}). */
        List<LhsItem> plan(TestItem.Role role, Term testTerm, Set<Integer> read, Condition conditionTerm) {
            return plan(role, testTerm, read, conditionTerm, List.of());
        }

        /** As {@link #plan(TestItem.Role, Term, Set, Conditie)}, with {@code control} between the standing items and the
         *  test: the guard of a creation, whose ids the analysis draws before the test's, or the control-fact patterns
         *  of a default, so that a default a higher one displaced is not tested at all. */
        List<LhsItem> plan(TestItem.Role role, Term testTerm, Set<Integer> read, Condition conditionTerm,
                           List<LhsItem> control) {
            TestItem test = testItem(role, testTerm, read, conditionTerm);
            List<LhsItem> out = new ArrayList<>(parameters);
            out.addAll(states);
            out.addAll(control);
            if (test != null) out.add(test);
            return out;
        }

        /** The test of {@code conditionTerm}, compiled into {@code test}, over the values at {@code read}, asking
         *  {@code role} of it; null where the rule has no condition. */
        private TestItem testItem(TestItem.Role role, Term testTerm, Set<Integer> read, Condition conditionTerm) {
            if (testTerm == null) return null;
            int[] pos = positions(read);
            if (pos.length > MAX_READS)
                throw new Refusal(DroolsCode.TEST_ITEM_MANY_READS.reason("count", pos.length, "snippet", snippet(conditionTerm), "limit", MAX_READS));
            return new TestItem(id("voorwaarde"), role, testTerm, role == TestItem.Role.DEPOSIT ? depositFlag : -1,
                    List.copyOf(read), slots.size());
        }

        /** The plan of this rule, {@code name}, of the form being lowered: the planned items before the subject
         *  pattern, the subject pattern with its constraints, joins, other constraints and bindings as they stand now,
         *  the planned items right after it, {@code items} after those, and {@code consequence}. */
        RulePlan rulePlan(String name, List<LhsItem> items, Consequence consequence) {
            return rulePlan(name, null, items, consequence);
        }

        /** As {@link #rulePlan(String, List, Gevolg)}, for a rule of the agenda group {@code agendaGroup}. */
        RulePlan rulePlan(String name, String agendaGroup, List<LhsItem> items, Consequence consequence) {
            List<LhsItem> lhs = new ArrayList<>(head());
            lhs.add(new Subject(beanClass(subject), SUBJECT, subjectAlpha, subjectJoins, subjectBetaTests,
                    subjectBindings));
            lhs.addAll(rangePlan);
            lhs.addAll(items);
            return new RulePlan(name, form, agendaGroup, new ReadTable(slots), withoutUnfilledDepositFlag(lhs),
                    withoutUnfilledDepositFlag(consequence));
        }

        /** The left-hand side of the reading rule after its subject pattern ({@link TmsTranspiler#readingRule}): the
         *  parameter items, the standing items and {@code control}, as {@link #plan} lists them, without the test; and
         *  where {@code withoutPhase}, without the {@link CreationPhase} of a creation, whose condition reads no phase,
         *  so that the reading rule runs in the main group. */
        List<LhsItem> readingPlan(List<LhsItem> control, boolean withoutPhase) {
            List<LhsItem> out = new ArrayList<>();
            for (LhsItem p : parameters)
                if (!(withoutPhase && p instanceof FactPattern f && f.factClass() == CreationPhase.class)) out.add(p);
            out.addAll(states);
            out.addAll(control);
            return out;
        }

        /** The reading rule {@code name} of the form {@code pointer}: the planned items before the subject pattern, the
         *  subject pattern as it stands now, the planned items right after it and {@code items}, as {@link #rulePlan}
         *  assembles them, in the main group and of no form, and a consequence that inserts the {@link Reading} of the
         *  subject with every slot this left-hand side binds. */
        RulePlan readingRulePlan(String name, String pointer, List<LhsItem> items) {
            List<LhsItem> lhs = new ArrayList<>(head());
            lhs.add(new Subject(beanClass(subject), SUBJECT, subjectAlpha, subjectJoins, subjectBetaTests,
                    subjectBindings));
            lhs.addAll(rangePlan);
            lhs.addAll(items);
            Set<String> declared = declaredVariables(lhs);
            List<Integer> bound = new ArrayList<>();
            for (dev.uchar.regelspraak.transpiler.tms.plan.Read l : slots)
                if (l.name() != null && declared.contains(l.name())) bound.add(l.slot());
            return new RulePlan(name, null, null, new ReadTable(slots), withoutUnfilledDepositFlag(lhs),
                    new Consequence.Reading(SUBJECT, pointer, bound, slots.size(), Consequence.Insertion.LOGICAL));
        }

        /** The name of the variable that binds slot {@code slot}, which an item of the plan names to read it. */
        String variable(int slot) {
            String name = slots.get(slot).name();
            if (name == null) throw new IllegalStateException("slot " + slot + " wordt door geen patroon gebonden");
            return name;
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
         *  can is tested with the role {@link TestItem.Role#WITHOUT_FAULT}, beside the rule that inserts its fault. */
        boolean canFail() { return fail; }

        /** Registers what the lowered rule reads with the transpiler: the standings, path standings and channel
         *  standings it needs, and the contribution rules of its folds with what those read. */
        void recorder() {
            read.putAll(derived);
            for (PathRead p : paths.values()) {
                pathReads.putIfAbsent(p.key(), p);
                if (writers.containsKey(p.member().key()) || timelineAttribute(p.member()))
                    read.putIfAbsent(p.member().key(), p.member());
            }
            channels.addAll(channelNames);
            for (Fold v : folds) {
                if (contributionRules.putIfAbsent(v.key(), v.rules()) == null)
                    for (RulePlan r : v.rules()) {
                        formOfRule.put(r.name(), form);
                        if (lowering != null) defaultOfRule.put(r.name(), lowering);
                    }
                for (Reads l : v.readings()) l.recorder();
            }
        }

        /**
         * A fold of this rule's subject, bound off the contributions of the fold {@code key} ({@link Contribution}): one
         * accumulate over them that counts them and, where {@code function} names a fold, folds their values with it;
         * where the fold's filter can fault, a second accumulate over its {@link FilterFault} facts on this subject, which
         * binds the least of them, the first in fold order, or null; where the filter can deposit, an accumulate that
         * lists its {@link FilterDeposit} facts on this subject; and where a member's value can fault, an
         * accumulate over its {@link MemberFault} facts on this subject, which binds the least of them in the same way.
         * Returns the index of the count, that of the folded value, that of the least filter fault, that of the list of
         * filter deposits and that of the least member fault, each -1 without one. An accumulate over no fact still binds
         * a row, through the count, so the rule reads an empty fold, and a fold without a filter fault, a filter deposit
         * or a member fault, as well.
         */
        int[] fold(Fold v, Accumulation.Kind function) {
            folds.add(v);
            String key = v.key();
            int known = existing("vouw " + key);
            if (known >= 0) return new int[]{known, function == null ? -1 : existing("vouwwaarde " + key),
                    v.canFail() ? existing("filterfout " + key) : -1,
                    v.deposits() ? existing("filterdeponering " + key) : -1,
                    v.valueCanFail() ? existing("lidfout " + key) : -1};
            String b = "$bijdrage_" + id("vouw");
            Alpha ofFold = equal(id("vouw"), "fold", key);
            Join onSubject = onSubjectHere(id("vouw-onderwerp"));
            String itemCount = "$aantal_" + id("vouw");
            int[] out;
            if (function == null) {
                states(new Accumulation(new FactPattern(Contribution.class, b, List.of(ofFold), List.of(onSubject), List.of()),
                        List.of(new Accumulation.Aggregation(Accumulation.Kind.COUNT, null, itemCount))));
                out = new int[]{allocateSlot(ReadKind.FOLD_COUNT, "vouw " + key, itemCount), -1, -1, -1, -1};
            } else {
                String contributionValue = "$bijdragewaarde_" + id("vouw");
                String outcome = "$vouw_" + id("vouw");
                states(new Accumulation(new FactPattern(Contribution.class, b, List.of(ofFold), List.of(onSubject),
                        List.of(Binding.of(contributionValue, Contribution.class, "value"))),
                        List.of(new Accumulation.Aggregation(Accumulation.Kind.COUNT, null, itemCount),
                                new Accumulation.Aggregation(function, contributionValue, outcome))));
                out = new int[]{allocateSlot(ReadKind.FOLD_COUNT, "vouw " + key, itemCount),
                        allocateSlot(ReadKind.FOLDED_VALUE, "vouwwaarde " + key, outcome), -1, -1, -1};
            }
            if (v.canFail()) {
                String f = "$filterfout_" + id("vouw");
                Alpha ofFilter = equal(id("filterfout"), "fold", key);
                Join filterOnSubject = onSubjectHere(id("filterfout-onderwerp"));
                String faults = "$filterfouten_" + id("vouw");
                String earliest = "$eerste_filterfout_" + id("vouw");
                states(new Accumulation(new FactPattern(FilterFault.class, f, List.of(ofFilter), List.of(filterOnSubject),
                        List.of()), List.of(new Accumulation.Aggregation(Accumulation.Kind.COUNT, null, faults),
                        new Accumulation.Aggregation(Accumulation.Kind.MINIMUM, null, earliest))));
                out[2] = allocateSlot(ReadKind.FILTER_FAULT, "filterfout " + key, earliest);
            }
            if (v.deposits()) {
                String d = "$filterdeponering_" + id("vouw");
                Alpha ofDeposit = equal(id("filterdeponering"), "fold", key);
                Join depositOnSubject = onSubjectHere(id("filterdeponering-onderwerp"));
                String deposits = "$filterdeponeringen_" + id("vouw");
                states(new Accumulation(new FactPattern(FilterDeposit.class, d, List.of(ofDeposit),
                        List.of(depositOnSubject), List.of()),
                        List.of(new Accumulation.Aggregation(Accumulation.Kind.LIST, null, deposits))));
                out[3] = allocateSlot(ReadKind.FILTER_DEPOSIT, "filterdeponering " + key, deposits);
            }
            if (v.valueCanFail()) {
                String f = "$lidfout_" + id("vouw");
                Alpha ofMember = equal(id("lidfout"), "fold", key);
                Join memberOnSubject = onSubjectHere(id("lidfout-onderwerp"));
                String faults = "$lidfouten_" + id("vouw");
                String earliest = "$eerste_lidfout_" + id("vouw");
                states(new Accumulation(new FactPattern(MemberFault.class, f, List.of(ofMember), List.of(memberOnSubject),
                        List.of()), List.of(new Accumulation.Aggregation(Accumulation.Kind.COUNT, null, faults),
                        new Accumulation.Aggregation(Accumulation.Kind.MINIMUM, null, earliest))));
                out[4] = allocateSlot(ReadKind.MEMBER_FAULT, "lidfout " + key, earliest);
            }
            return out;
        }

        /** The session's evaluation date (RUNTIME-SEMANTICS ch. 8.1), bound off its {@link EvaluationDate}: one fact for the
         *  whole session, joined to no subject, bound among the parameters; null where the case carries no date. */
        int evaluationDate() {
            // one fact for the whole session, read alike on the subject and on the form's subject
            if (atForm) return unscoped(this::evaluationDate);
            int known = existing("rekendatum");
            if (known >= 0) return known;
            evaluationDateNeeded();
            String r = "$rekendatum_" + slots.size();
            String d = "$rekendatumwaarde_" + slots.size();
            parameters(new FactPattern(EvaluationDate.class, r, List.of(), List.of(),
                    List.of(Binding.of(d, EvaluationDate.class, "date"))));
            return allocateSlot(ReadKind.REKENDATUM, "rekendatum", d);
        }

        /** Gates this rule on the evaluation date (RUNTIME-SEMANTICS ch. 8.2): it matches only where the validity
         *  window {@code g} admits the date, both bounds inclusive and an absent one open (IR-SPEC ch. 3.10), and never
         *  in a session without a date, where the form faults instead ({@link #validityFault}). A window that is always
         *  valid adds nothing. The date is constant while the session fires, so the gate never changes a match. */
        Reads valid(Validity g) {
            if (g == null || g.always()) return this;
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
                    List.of(Binding.of("$fase", CreationPhase.class, "number"))));
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
            notAtForm(allocation);
            String key = "ontvangers " + allocation;
            int known = existing(key);
            if (known >= 0) return known;
            String o = "$ontvanger_" + id("verdeling");
            String list = "$ontvangers_" + id("verdeling");
            Alpha ofAllocation = equal(id("ontvanger"), "allocation", allocation);
            states(new Accumulation(new FactPattern(Recipient.class, o, List.of(ofAllocation),
                    List.of(new Join(id("ontvanger-bron"), "source", SUBJECT)), List.of()),
                    List.of(new Accumulation.Aggregation(Accumulation.Kind.LIST, null, list))));
            return allocateSlot(ReadKind.RECIPIENTS, key, list);
        }

        int origin() {
            notAtForm("herkomst");
            int known = existing("herkomst");
            if (known >= 0) return known;
            String g = "$herkomst_" + id("creatie");
            String list = "$herkomsten_" + id("creatie");
            states(new Accumulation(new FactPattern(CreationStamp.class, g, List.of(),
                    List.of(new Join(id("herkomst"), "object", SUBJECT)), List.of()),
                    List.of(new Accumulation.Aggregation(Accumulation.Kind.LIST, null, list))));
            return allocateSlot(ReadKind.ORIGIN, "herkomst", list);
        }

        /** The standing of the §9.5 channel of the form named {@code producer}, bound off its {@link ChannelState}:
         *  one fact for the whole session, joined to no subject, since the channel test has none of its own. */
        int channel(String producer) {
            String key = "kanaal " + producer;
            int known = existing(key);
            if (known >= 0) return known;
            String w = readName(producer + " inconsistent");
            String st = "$kanaal_" + slots.size();
            states(new FactPattern(ChannelState.class, st, List.of(equal(id("kanaal"), "rule", producer)), List.of(),
                    List.of(Binding.of(w, ChannelState.class, "inconsistent"))));
            channelNames.add(producer);
            return allocateSlot(ReadKind.CHANNEL, key, w);
        }

        /** Whether the reference field {@code field} of this rule's subject is set, to an instance of {@code javaClass} where
         *  a role check narrows the other end (null: any): a link of an input relation stored on the subject
         *  ({@link TmsTranspiler#roleCheck}). Input does not change while the session fires, so it is bound off the
         *  subject's bean, as an input member is ({@link #own}). */
        int roleField(String field, Class<?> javaClass) {
            notAtForm(field);
            String key = "rolveld " + field + " " + (javaClass == null ? "" : javaClass.getName());
            int known = existing(key);
            if (known >= 0) return known;
            String w = readName(field + " gezet");
            bindSubject(new Binding(w, List.of(), new Binding.Field(beanClass(subject), field), Binding.ReadForm.IS_SET,
                    javaClass));
            return allocateSlot(ReadKind.ROLE_FIELD, key, w);
        }

        /** The number of beans of {@code javaClass} whose reference field {@code field} names this rule's subject: the links
         *  of an input relation stored on the other end ({@link TmsTranspiler#roleCheck}), bound off an accumulate,
         *  which over no bean still binds a row. */
        int roleFillers(Class<?> javaClass, String field) {
            notAtForm(field);
            String key = "rolvullers " + javaClass.getName() + " " + field;
            int known = existing(key);
            if (known >= 0) return known;
            String filler = "$vuller_" + id("rol");
            String itemCount = "$vullers_" + id("rol");
            states(new Accumulation(new FactPattern(javaClass, filler, List.of(),
                    List.of(new Join(id("rolvuller"), field, SUBJECT)), List.of()),
                    List.of(new Accumulation.Aggregation(Accumulation.Kind.COUNT, null, itemCount))));
            return allocateSlot(ReadKind.ROLE_FILLERS, key, itemCount);
        }

        /** The number of links of the created relation {@code relation} that hold this rule's subject at its first end
         *  where {@code subjectIsFirst}, else at its second, with an instance of {@code javaClass} at the other end (null:
         *  any): the {@link RelationLink} facts, bound off an accumulate, which over no link still binds a row
         *  ({@link TmsTranspiler#roleCheck}). */
        int roleLinks(String relation, boolean subjectIsFirst, Class<?> javaClass) {
            notAtForm(relation);
            String key = "rolkoppelingen " + relation + " " + subjectIsFirst + " "
                    + (javaClass == null ? "" : javaClass.getName());
            int known = existing(key);
            if (known >= 0) return known;
            String k = "$rolkoppeling_" + id("rol");
            String itemCount = "$rolkoppelingen_" + id("rol");
            // the test of the other end's class reads the link alone, so it goes with the relation and the join to the
            // subject stays an identity join that the memories can hash
            List<Alpha.FieldTest> tests = new ArrayList<>();
            tests.add(new Alpha.FieldTest("relation", Alpha.Comparator.GELIJK, relation));
            if (javaClass != null)
                tests.add(new Alpha.FieldTest(subjectIsFirst ? "second" : "first", Alpha.Comparator.IS_A, javaClass));
            Alpha ofRelation = new Alpha(id("rolkoppeling"), tests);
            Join onSubject = new Join(id("rolkoppeling-onderwerp"), subjectIsFirst ? "first" : "second", SUBJECT);
            states(new Accumulation(new FactPattern(RelationLink.class, k, List.of(ofRelation), List.of(onSubject), List.of()),
                    List.of(new Accumulation.Aggregation(Accumulation.Kind.COUNT, null, itemCount))));
            return allocateSlot(ReadKind.ROLE_LINKS, key, itemCount);
        }

        /** The parent of this rule's subject in the scope of a uniqueness ({@link TmsTranspiler#keyRule}): the
         *  reference field on the subject that the scope's binding names, the role the parent plays, read off the
         *  subject's bean, since the relation is input; null where the subject has no parent. A scope over a relation a
         *  form creates, whose links are in no field, is refused. */
        int scopeRead(RoleBindingRef scopeRole) {
            notAtForm(scopeRole.role());
            String key = "scope " + scopeRole.factType() + " " + scopeRole.role();
            int known = existing(key);
            if (known >= 0) return known;
            RegelSpraakModel.Role[] pair = FactModelGenerator.boundRolePair(model, scopeRole);
            if (pair == null)
                throw new Refusal(DroolsCode.UNIQUENESS_SCOPE_ROLE_UNDECLARED.reason("factType", scopeRole.factType(), "role", scopeRole.role()));
            if (scopeRole.factType() != null && createdRelations.contains(Namen.norm(scopeRole.factType())))
                throw new Refusal(DroolsCode.UNIQUENESS_SCOPE_CREATED_RELATION.reason("factType", scopeRole.factType()));
            ObjectType parent = model.objectTypeByName(pair[0].objectTypeName());
            if (!FactModelGenerator.hasReferenceField(model, subject, pair[0].name(), parent))
                throw new Refusal(DroolsCode.UNIQUENESS_SCOPE_NO_FIELD.reason("reason", FactModelGenerator.noReferenceFieldReason(model, subject, pair[0].name(), parent).msg()));
            String w = readName(pair[0].name());
            bindSubject(new Binding(w, List.of(), new Binding.Field(beanClass(subject), pair[0].name()),
                    Binding.ReadForm.VALUE, null));
            return allocateSlot(ReadKind.SCOPE, key, w);
        }

        private int existing(String key) {
            Integer i = index.get(scoped(key));
            if (i != null && use != null) use.add(i);
            return i == null ? -1 : i;
        }

        /** The variable of a slot that reads {@code read} ({@link TmsTranspiler#readName(String, Set)}), unique
         *  against the slots of this rule before it. */
        private String readName(String read) {
            return TmsTranspiler.readName(read, names);
        }

        /** A slot that a binding of the plan binds to the variable {@code name}, read once per rule under
         *  {@code key}. */
        private int allocateSlot(ReadKind category, String key, String name) {
            int i = slots.size();
            dev.uchar.regelspraak.transpiler.tms.plan.Read l = new dev.uchar.regelspraak.transpiler.tms.plan.Read(i, category, scoped(key), name);
            slots.add(l);
            index.put(scoped(key), i);
            names.add(name);
            if (use != null) use.add(i);
            return i;
        }

        /** A slot that no pattern binds: the consequence fills it. */
        private int unbound(ReadKind category) {
            dev.uchar.regelspraak.transpiler.tms.plan.Read l = new dev.uchar.regelspraak.transpiler.tms.plan.Read(slots.size(), category, null, null);
            slots.add(l);
            return l.slot();
        }

        private void bindSubject(Binding b) {
            (atForm ? formBindings : subjectBindings).add(b);
        }

        /** The slot {@code read} gives for a read that does not depend on the subject, under its own key while an
         *  anchored node compiles as well. */
        private int unscoped(java.util.function.IntSupplier read) {
            boolean was = atForm;
            atForm = false;
            try {
                return read.getAsInt();
            } finally {
                atForm = was;
            }
        }

        /** The key {@code key} of a read under which this rule knows it: apart for a read of the form's subject, which
         *  can read a member the subject reads as well. */
        private String scoped(String key) {
            return atForm ? "vormonderwerp " + key : key;
        }

        private void parameters(LhsItem item) {
            parameters.add(item);
        }

        private void states(LhsItem item) {
            states.add(item);
        }

        /** The standing of {@code member} of the subject, a {@link State} or a {@link CharacteristicState}, binding {@code field}
         *  of it to {@code w}: a constraint on the member, the join on the subject, the binding. */
        private FactPattern state(Class<?> javaClass, String name, String memberField, ElementKey lk, String field, String w) {
            Alpha ofMember = equal(id("stand"), memberField, lk);
            Join onSubject = onSubjectHere(id("stand-onderwerp"));
            return new FactPattern(javaClass, name, List.of(ofMember), List.of(onSubject), List.of(Binding.of(w, javaClass, field)));
        }

        int own(Member member) {
            String key = "eigen " + member.key();
            int known = existing(key);
            if (known >= 0) return known;
            String w = readName(member.name());
            ElementKey lk = member.key();
            if (writers.containsKey(lk)) {
                if (member.characteristic()) {
                    states(state(CharacteristicState.class, "$stand_" + slots.size(), "characteristic", lk, "holds", w));
                    derived.put(lk, member);
                    return allocateSlot(ReadKind.CHARACTERISTIC_STATE, key, w);
                }
                states(state(State.class, "$stand_" + slots.size(), "attribute", lk, "value", w));
                derived.put(lk, member);
                return allocateSlot(ReadKind.STATE, key, w);
            }
            // input: no form writes it, so it does not change while the session fires and is read off the bean
            bindSubject(new Binding(w, List.of(), new Binding.Field(member.javaClass(), member.name()),
                    member.characteristic() ? Binding.ReadForm.WAAR : Binding.ReadForm.VALUE, null));
            return allocateSlot(ReadKind.INPUT, key, w);
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
         * ({@link Period#list}), and never through {@link #characteristicStateRules}, whose test of {@code true} is false on
         * a list; the bean does not change while the session fires, so the binding is stable.
         */
        int timeline(Member member) {
            String key = "tijdlijn " + member.key();
            int known = existing(key);
            if (known >= 0) return known;
            String w = "$tijdlijn_" + slots.size();
            ElementKey lk = member.key();
            if (member.characteristic()) {
                if (writers.containsKey(lk))
                    throw new Refusal(DroolsCode.TIMELINE_CHARACTERISTIC_WRITTEN.reason("member", member.name(), "objectType", member.declarant().name));
                bindSubject(new Binding(w, List.of(), new Binding.Field(member.javaClass(), member.name()),
                        Binding.ReadForm.PERIODS, null));
            } else {
                states(state(State.class, "$stand_" + slots.size(), "attribute", lk, "value", w));
                derived.put(lk, member);
            }
            return allocateSlot(ReadKind.TIJDLIJN, key, w);
        }

        /** A member of the object {@code path} reaches from this rule's subject, null where a link on the path is absent.
         *  An attribute on a timeline is read through its standing at the end of the path, as {@link #timeline} reads
         *  one of the subject's own, so the read carries the case's input convention, a {@code List<Period>} or an
         *  {@link BucketScalar}; a characteristic on a timeline, which is input only, is bound as its true periods, a
         *  {@code List<Period>} ({@link TmsTranspiler#inputForm}). */
        int navigated(Path path, Member member) {
            String key = "pad " + path.key() + " " + member.key();
            int known = existing(key);
            if (known >= 0) return known;
            String w = readName(String.join(" ", path.roles()) + " " + member.name());
            ElementKey lk = member.key();
            if (member.time() != null && member.characteristic() && writers.containsKey(lk))
                throw new Refusal(DroolsCode.TIMELINE_CHARACTERISTIC_WRITTEN.reason("member", member.name(), "objectType", member.declarant().name));
            // a derived member, an attribute on a timeline, or any member at the end of a path whose links a form
            // creates: through the path's standing, whose rules bind every link and the member in their left-hand sides
            if (writers.containsKey(lk) || timelineAttribute(member) || path.created()) {
                PathRead p = new PathRead(path, member);
                String ps = path.key();
                String st = "$padstand_" + slots.size();
                Alpha ofPath = new Alpha(id("padstand"), List.of(new Alpha.FieldTest("path", Alpha.Comparator.GELIJK, ps),
                        new Alpha.FieldTest("member", Alpha.Comparator.GELIJK, lk)));
                Join onSubject = onSubjectHere(id("padstand-onderwerp"));
                states(new FactPattern(PathState.class, st, List.of(ofPath), List.of(onSubject),
                        List.of(Binding.of(w, PathState.class, "value"))));
                paths.put(p.key(), p);
                return allocateSlot(ReadKind.PATH_STATE, key, w);
            }
            // input over links that are input: bound off the subject's bean, null where a link is absent
            bindSubject(new Binding(w, path.fields(), new Binding.Field(member.javaClass(), member.name()), inputForm(member),
                    null));
            return allocateSlot(ReadKind.INPUT_PATH, key, w);
        }

        /** A parameter, bound off its {@link ParameterState}: no test or consequence names a global itself, since two
         *  globals in one constraint of the executable model read as the last one, and a source over a global was not
         *  kept apart from another parameter's in another rule ({@link ParameterState}). A parameter the case leaves
         *  unset has a standing with a null value and reads leeg. */
        int parameter(Parameter p) {
            if (atForm) return unscoped(() -> parameter(p));
            String name = Namen.ident(p.name(), false);
            String key = "parameter " + name;
            int known = existing(key);
            if (known >= 0) return known;
            global(p);
            String w = readName(p.name());
            String st = "$parameter_" + slots.size();
            parameters(new FactPattern(ParameterState.class, st, List.of(equal(id("parameter"), "parameter", name)),
                    List.of(), List.of(Binding.of(w, ParameterState.class, "value"))));
            return allocateSlot(ReadKind.PARAMETER, key, w);
        }
    }

    /** The name of the variable of a rule's subject in every plan: the name joins on the subject refer to. */
    private static final String SUBJECT = "$onderwerp";
    /** The variable of the object a rule binds before its subject: a fold's subject in a contribution rule, the source
     *  in an allocation's recipient rule. */
    private static final String FOLD_SUBJECT = "$vouwonderwerp";
    /**
     * The variable of a slot that reads {@code read}, a name as the document declares it, in a rule whose slots
     * before it have the variables {@code occupied}: {@code $} and that name as an identifier ({@link Namen#ident}), so
     * that a rendering shows what the slot holds, in DRL text as well. Only the characters a Java identifier may hold
     * stay: {@link Namen#ident} keeps every letter and every number of Unicode, and a number such as {@code ²} or
     * {@code ½} is none, so "artikel 3²" reads {@code $artikel3}. A name of which nothing stays reads {@code $x}, the
     * name {@link Namen#ident} gives an empty name. It is unique within the rule by construction: the identifier has no
     * underscore, while every other variable of a rule has one ({@link TmsTranspiler#id}, the numbered names of
     * {@code Reads}) or is one of {@link #FIXED_NAMES}; where such a fixed name or a name in {@code occupied} has it
     * already, the first free number from 2 after it makes it unique. It depends only on the reads of the rule before
     * it, in their order. {@code RulePlan} refuses a rule that declares one variable twice, so a name that escaped
     * this would fail the build, and not bind two values to one variable.
     */
    static String readName(String read, Set<String> occupied) {
        StringBuilder id = new StringBuilder();
        Namen.ident(read, false).codePoints().filter(Character::isJavaIdentifierPart).forEach(id::appendCodePoint);
        String base = "$" + (id.isEmpty() ? "x" : id);
        String name = base;
        for (int n = 2; FIXED_NAMES.contains(name) || occupied.contains(name); n++) name = base + n;
        return name;
    }

    /** Every name without an underscore that the analysis gives a variable of a rule other than a read name
     *  ({@link #readName(String, Set)}): a read name that equals one of these takes a number, so that no rule binds
     *  one name twice. {@code ReadNameWitnessTest} holds this set equal to the fixed names this source spells. */
    static final Set<String> FIXED_NAMES = Set.of("$afgeleid", "$ander", "$bereikt", "$besluit", "$botsingen",
            "$conflict", "$creatiefase", "$een", "$element", "$emmerinvoer", "$fase", "$houder", "$inconsistentie",
            "$invoer", "$nabij", "$onderwerp", "$rekendatum", "$schending", "$sleutel", "$stand", "$subject", "$twee",
            "$v", "$vouwonderwerp", "$waarde", "$waarden");

    /** The items {@code lhs} of a rule with the probe of every test that places none at -1
     *  ({@link #withoutUnfilledDepositFlag(Consequence)}): a test of deposits places it, every other role does not. */
    private static List<LhsItem> withoutUnfilledDepositFlag(List<LhsItem> lhs) {
        List<LhsItem> out = new ArrayList<>(lhs.size());
        for (LhsItem item : lhs) {
            if (item instanceof TestItem t && t.role() != TestItem.Role.DEPOSIT) {
                Term testTerm = withoutDepositFlag(t.test());
                if (testTerm != t.test()) item = new TestItem(t.id(), t.role(), testTerm, t.depositFlag(), t.slots(), t.size());
            }
            out.add(item);
        }
        return out;
    }

    /**
     * {@code g} with the probe of its terms at -1 where it places none. Every place that deposits compiles with the
     * probe slot of its rule ({@link Reads#depositFlag}), whatever the role of the rule, but only three positions put a
     * probe in that slot: a test of deposits ({@link TestItem.Role#DEPOSIT}), the value of a rule with a deposit
     * ({@link Consequence.Value#deposit}) and the test of a member with a probe in a quantifier fold
     * ({@link Consequence.Input.MemberTest}). Elsewhere, as in the rule of a condition's fault or a filter's fault, the
     * places would report to a slot nobody fills, which reads as nothing; the plan says so itself, so that a rendering
     * needs no rule of its own for such a slot. The slot stays in the table, and the value array keeps its size.
     */
    private static Consequence withoutUnfilledDepositFlag(Consequence g) {
        switch (g) {
            case Consequence.Value w when w.deposit() == null -> {
                Term z = withoutDepositFlag(w.value());
                return z == w.value() ? w : new Consequence.Value(w.subject(), w.target(), w.writer(), z, w.slots(),
                        w.size(), w.collapse(), w.writesEmpty(), null, w.fault(), w.insertion());
            }
            case Consequence.TestFault f -> {
                Term z = withoutDepositFlag(f.test());
                return z == f.test() ? f : new Consequence.TestFault(f.subject(), z, f.role(), f.slots(), f.size(),
                        f.fault(), f.insertion());
            }
            case Consequence.FilterFault f -> {
                Term z = withoutDepositFlag(f.filter());
                return z == f.filter() ? f : new Consequence.FilterFault(f.subject(), f.fold(), f.member(), z, f.place(),
                        f.slots(), f.size(), f.insertion());
            }
            case Consequence.Contribution b when b.input() instanceof Consequence.Input.Verdict o -> {
                Term z = withoutDepositFlag(o.matrix());
                return z == o.matrix() ? b : new Consequence.Contribution(b.subject(), b.fold(), b.member(),
                        new Consequence.Input.Verdict(z), b.slots(), b.size(), b.insertion());
            }
            case Consequence.Contribution b when b.input() instanceof Consequence.Input.MemberTest l && l.depositFlag() < 0 -> {
                Term z = withoutDepositFlag(l.matrix());
                return z == l.matrix() ? b : new Consequence.Contribution(b.subject(), b.fold(), b.member(),
                        new Consequence.Input.MemberTest(z, -1, l.place()), b.slots(), b.size(), b.insertion());
            }
            case Consequence.Creation c -> {
                List<Consequence.Initialisation> inits = new ArrayList<>();
                boolean changed = false;
                for (Consequence.Initialisation i : c.initialisations()) {
                    Term z = withoutDepositFlag(i.value());
                    changed |= z != i.value();
                    inits.add(z == i.value() ? i : new Consequence.Initialisation(i.member(), z));
                }
                return !changed ? c : new Consequence.Creation(c.subject(), c.creation(), c.depth(), inits, c.fault(),
                        c.faultInsertion(), c.construction(), c.phase(), c.insertions(), c.slots(), c.size());
            }
            case Consequence.Decision a -> {
                Term z = withoutDepositFlag(a.value());
                return z == a.value() ? a : new Consequence.Decision(a.subject(), z, a.slots(), a.size(), a.collapse(),
                        a.fault(), a.facts(), a.insertion());
            }
            case Consequence.Conflict c -> {
                Term z = withoutDepositFlag(c.key());
                return z == c.key() ? c : new Consequence.Conflict(c.subject(), z, c.slots(), c.size(), c.fact(),
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
            case Term.Input i -> {
                Term o = withoutDepositFlag(i.object());
                return o == i.object() ? i : new Term.Input(o, i.field(), i.form(), i.source());
            }
            case Term.Let l -> {
                Term w = withoutDepositFlag(l.value()), b = withoutDepositFlag(l.body());
                return w == l.value() && b == l.body() ? l
                        : new Term.Let(l.slot(), l.name(), w, b, l.eager(), l.source());
            }
            case Term.Choice k -> {
                Term v = withoutDepositFlag(k.condition()), d = withoutDepositFlag(k.then()), a = withoutDepositFlag(k.otherwise());
                return v == k.condition() && d == k.then() && a == k.otherwise() ? k : new Term.Choice(v, d, a, k.source());
            }
            case Term.Call a -> {
                List<Term> z = withoutDepositFlag(a.arguments());
                return z == a.arguments() ? a : new Term.Call(a.function(), z, a.source());
            }
            case Term.Restate h -> {
                Term z = withoutDepositFlag(h.term());
                return z == h.term() ? h
                        : new Term.Restate(h.function(), z, h.from(), h.destination(), h.counter(), h.denominator(), h.source());
            }
            case Term.Division d -> {
                Term l = withoutDepositFlag(d.left()), r = withoutDepositFlag(d.right());
                return d.depositFlag() < 0 && l == d.left() && r == d.right() ? d
                        : new Term.Division(d.function(), l, r, -1, d.source());
            }
            case Term.SquareRoot w -> {
                Term r = withoutDepositFlag(w.radicand());
                return w.depositFlag() < 0 && r == w.radicand() ? w : new Term.SquareRoot(w.function(), r, -1, w.source());
            }
            case Term.Required v -> {
                Term z = withoutDepositFlag(v.value());
                return z == v.value() ? v : new Term.Required(z, v.sentence(), v.source());
            }
            case Term.TextComposition r -> {
                List<Term> z = withoutDepositFlag(r.parts());
                return z == r.parts() ? r : new Term.TextComposition(z, r.source());
            }
            case Term.ListedFold g -> {
                List<Term> z = withoutDepositFlag(g.operands());
                return z == g.operands() ? g : new Term.ListedFold(g.function(), z, g.source());
            }
            case Term.FoldRead v -> {
                Term z = withoutDepositFlag(v.outcome());
                return v.depositFlag() < 0 && z == v.outcome() ? v
                        : new Term.FoldRead(z, v.deposits(), -1, v.filterFault(), v.memberFault(), v.source());
            }
            case Term.Compound s -> {
                List<Term> z = withoutDepositFlag(s.members());
                return z == s.members() ? s : new Term.Compound(s.quantifier(), s.threshold(), z, s.source());
            }
            case Term.QuantifierRead k -> {
                Term n = withoutDepositFlag(k.count()), b = withoutDepositFlag(k.contributions());
                return k.depositFlag() < 0 && n == k.count() && b == k.contributions() ? k
                        : new Term.QuantifierRead(k.function(), n, b, k.quantifier(), k.threshold(), -1, k.source());
            }
            case Term.WithoutFault z -> {
                Term v = withoutDepositFlag(z.condition());
                return v == z.condition() ? z : new Term.WithoutFault(v, z.source());
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

    /** A constraint that field {@code field} of a fact equals {@code value}. */
    private static Alpha equal(String id, String field, Object value) {
        return new Alpha(id, List.of(new Alpha.FieldTest(field, Alpha.Comparator.GELIJK, value)));
    }

    /** The join of a standing fact on the rule's subject: its field {@code subjectType} is the subject. */
    private static Join onSubject(String id) {
        return new Join(id, "subject", SUBJECT);
    }

    /** The rule that puts the {@link ParameterState} of one parameter in working memory: it matches no fact, so it fires
     *  once per session, and it binds the global in its consequence, where the case runner set it before the session
     *  fired. The global does not change while the session fires, so the standing never needs to be withdrawn. */
    private RulePlan parameterStateRule(String name) {
        return new RulePlan("parameterstand " + name, null, null, new ReadTable(List.of()), List.of(),
                new Consequence.InsertParameterState(name, name, Consequence.Insertion.LOGICAL));
    }

    /** Declares the global of parameter {@code p}, of the Java type of its datatype, so that the case runner sets it by
     *  name; one per parameter for the whole model. */
    private void global(Parameter p) {
        String name = Namen.ident(p.name(), false);
        if (globals.containsKey(name)) return;
        if (p.granularity() != null)
            throw new Refusal(DroolsCode.TEMPORAL_PARAMETER_GRANULARITY.reason("parameter", p.name()));
        Datatype t = p.datatype();
        Class<?> javaClass = t.isNumber() || t.isPercentage() ? BigDecimal.class
                : t instanceof Datatype.DateInDays ? java.time.LocalDate.class
                : t instanceof Datatype.DateTime ? java.time.LocalDateTime.class
                : t.isText() || t instanceof Datatype.Enumeration ? String.class
                : t.isBoolean() ? Boolean.class : null;
        if (javaClass == null) throw new Refusal(DroolsCode.PARAMETER_DATATYPE_NOT_CARRIED.reason("parameter", p.name(), "typeName", typeName(t)));
        globals.put(name, javaClass);
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
        ElementKey key = member.key();
        String name = "kenmerkstand " + member.declarant().name + "." + member.name() + ": ";
        Alpha inputTrue = inputTrue(id("invoer"), member, true);
        RulePlan inputData = new RulePlan(name + "waar uit de invoer", null, null, new ReadTable(List.of()),
                List.of(new Subject(member.javaClass(), SUBJECT, List.of(inputTrue), List.of(), List.of())),
                new Consequence.InsertCharacteristicState(SUBJECT, key, Consequence.StateSource.INPUT, true, Consequence.Insertion.LOGICAL));
        RulePlan derivation = new RulePlan(name + "waar uit een afleiding", null, null, new ReadTable(List.of()),
                List.of(new Subject(member.javaClass(), SUBJECT, List.of(), List.of(), List.of()), derived(key)),
                new Consequence.InsertCharacteristicState(SUBJECT, key, Consequence.StateSource.DERIVATION, true,
                        Consequence.Insertion.LOGICAL));
        Alpha noInput = inputTrue(id("geen-invoer"), member, false);
        RulePlan falsePlan = new RulePlan(name + "onwaar", null, null, new ReadTable(List.of()),
                List.of(new Subject(member.javaClass(), SUBJECT, List.of(noInput), List.of(), List.of()),
                        new Not(derived(key))),
                new Consequence.InsertCharacteristicState(SUBJECT, key, Consequence.StateSource.NONE, false, Consequence.Insertion.LOGICAL));
        return List.of(inputData, derivation, falsePlan);
    }

    /** The test that the input of characteristic {@code member} on the subject's bean, read as whether it holds
     *  {@code true}, is {@code polarity}. */
    private static Alpha inputTrue(String id, Member member, boolean polarity) {
        return new Alpha(id, List.of(new Alpha.FieldTest(List.of(), member.name(), Binding.ReadForm.WAAR,
                Alpha.Comparator.GELIJK, polarity)));
    }

    /** The pattern of the {@link HasCharacteristic} of {@code characteristic} that a derivation put on the subject: a constraint on
     *  the characteristic, the join on the subject. */
    private FactPattern derived(ElementKey characteristic) {
        return new FactPattern(HasCharacteristic.class, "$afgeleid", List.of(equal(id("afgeleid"), "characteristic", characteristic)),
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
     * {@code List<Period>} ({@link Period#list}), so every standing of the member holds one representation, except
     * where the case gives the input as the asOfDate-bucket scalar: then the input stands as an {@link BucketScalar}
     * (RUNTIME-SEMANTICS ch. 8.4). The bean holds that scalar as the one segment of its period, which a full timeline of
     * one segment would be as well, so two input rules join the {@link BucketInput} the case input states beside the
     * beans, the one under {@code exists} and the other under {@code not}; the fact is stated and never changes while
     * the session fires, so exactly one of them holds for each object.
     */
    private List<RulePlan> attributeStateRules(Member member) {
        ElementKey key = member.key();
        String name = "stand " + member.declarant().name + "." + member.name() + ": ";
        List<RulePlan> out = new ArrayList<>();

        if (writers.containsKey(key)) {
            FactPattern value = new FactPattern(Value.class, "$waarde", List.of(equal(id("waarde"), "attribute", key)),
                    List.of(onSubject(id("waarde-onderwerp"))), List.of(Binding.of("$v", Value.class, "value")));
            out.add(new RulePlan(name + "uit een afleiding", null, null,
                    oneSlot(ReadKind.DERIVATION, "afleiding " + key, "$v"),
                    List.of(new Subject(member.javaClass(), SUBJECT, List.of(), List.of(), List.of()), value),
                    new Consequence.State(SUBJECT, key, Consequence.StateSource.DERIVATION, 0, false,
                            Consequence.Insertion.LOGICAL)));
        }

        if (member.time() == null) {
            out.add(inputStateRule(member, name + "uit de invoer", null));
            return out;
        }
        out.add(inputStateRule(member, name + "uit de invoer", false));
        out.add(inputStateRule(member, name + "uit de invoer, een emmerscalair", true));
        return out;
    }

    /** The input rule {@code name} of {@link #attributeStateRules}: where no {@link Value} stands on the object, the
     *  {@link State} of the input on the bean, read as it is, or as its segments for a member on a timeline
     *  ({@link #inputForm}); with {@code bucket} null for a member that holds one value, and for a member on a timeline
     *  only where an {@link BucketInput} of the member on the object exists ({@code true}), the input then standing as
     *  its {@link BucketScalar}, or does not ({@code false}). The bucket is taken in the consequence, from the bound
     *  timeline: a binding is computed for every object the subject pattern matches, also one whose input is a full
     *  timeline. */
    private RulePlan inputStateRule(Member member, String name, Boolean bucket) {
        ElementKey key = member.key();
        List<LhsItem> items = new ArrayList<>();
        items.add(new Subject(member.javaClass(), SUBJECT, List.of(), List.of(), List.of(new Binding("$invoer", List.of(),
                new Binding.Field(member.javaClass(), member.name()), inputForm(member), null))));
        if (bucket != null) {
            FactPattern p = new FactPattern(BucketInput.class, "$emmerinvoer",
                    List.of(equal(id("emmerinvoer"), "attribute", key)),
                    List.of(onSubject(id("emmerinvoer-onderwerp"))), List.of());
            items.add(bucket ? new Exists(List.of(p), List.of(p.name()), List.of(SUBJECT)) : new Not(p));
        }
        items.add(new Not(new FactPattern(Value.class, "$waarde", List.of(equal(id("waarde"), "attribute", key)),
                List.of(onSubject(id("waarde-onderwerp"))), List.of())));
        return new RulePlan(name, null, null, oneSlot(ReadKind.INPUT, "invoer " + key, "$invoer"),
                items, new Consequence.State(SUBJECT, key, Consequence.StateSource.INPUT, 0, Boolean.TRUE.equals(bucket),
                        Consequence.Insertion.LOGICAL));
    }

    /** The value array of a standing rule whose consequence reads one value: slot 0, of {@code category}, which the
     *  variable {@code name} binds. */
    private static ReadTable oneSlot(ReadKind category, String key, String name) {
        return new ReadTable(List.of(new dev.uchar.regelspraak.transpiler.tms.plan.Read(0, category, key, name)));
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
        Path path = p.path();
        Member member = p.member();
        String ps = path.key();
        ElementKey lk = member.key();
        Class<?> javaClass = beanClass(path.subject());
        String name = "padstand " + path.subject().name + "." + String.join(".", path.roles()) + " naar "
                + member.declarant().name + "." + member.name() + ": ";
        List<Binding.Field> fields = path.fields();

        Alpha link = alongPath(id("link"), fields, Alpha.Comparator.GEVULD);
        Binding reaches = new Binding("$bereikt", fields.subList(0, fields.size() - 1), fields.get(fields.size() - 1),
                Binding.ReadForm.VALUE, null);
        Class<?> stateClass = member.characteristic() ? CharacteristicState.class : State.class;
        FactPattern state = new FactPattern(stateClass, "$stand",
                List.of(equal(id("stand"), member.characteristic() ? "characteristic" : "attribute", lk)),
                List.of(new Join(id("stand-bereikt"), "subject", "$bereikt")),
                List.of(Binding.of("$v", stateClass, member.characteristic() ? "holds" : "value")));
        RulePlan present = new RulePlan(name + "link aanwezig", null, null,
                oneSlot(ReadKind.REACHED_STATE, "stand " + ps + " " + lk, "$v"),
                List.of(new Subject(javaClass, SUBJECT, List.of(link), List.of(), List.of(reaches)), state),
                new Consequence.InsertPathState(SUBJECT, ps, lk, Consequence.StateSource.REACHED, 0, Consequence.Insertion.LOGICAL));

        Alpha noLink = alongPath(id("geen-link"), fields, Alpha.Comparator.LEEG);
        RulePlan absent = new RulePlan(name + "link afwezig", null, null, new ReadTable(List.of()),
                List.of(new Subject(javaClass, SUBJECT, List.of(noLink), List.of(), List.of())),
                new Consequence.InsertPathState(SUBJECT, ps, lk, Consequence.StateSource.MISSING_LINK, -1,
                        Consequence.Insertion.LOGICAL));
        return List.of(present, absent);
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
        Path path = p.path();
        Member member = p.member();
        String ps = path.key();
        ElementKey lk = member.key();
        Class<?> javaClass = beanClass(path.subject());
        String name = "padstand " + path.subject().name + "." + String.join(".", path.roles()) + " naar "
                + member.declarant().name + "." + member.name() + ": ";

        List<LhsItem> items = new ArrayList<>();
        items.add(new Subject(javaClass, SUBJECT, List.of(), List.of(), List.of()));
        List<FactPattern> chain = chain(path.hops(), SUBJECT, null);
        ReadTable value;
        if (writers.containsKey(lk) || timelineAttribute(member)) {
            items.addAll(chain);
            Class<?> stateClass = member.characteristic() ? CharacteristicState.class : State.class;
            items.add(new FactPattern(stateClass, "$stand",
                    List.of(equal(id("stand"), member.characteristic() ? "characteristic" : "attribute", lk)),
                    List.of(new Join(id("stand-bereikt"), "subject", chain.get(chain.size() - 1).name())),
                    List.of(Binding.of("$v", stateClass, member.characteristic() ? "holds" : "value"))));
            value = oneSlot(ReadKind.REACHED_STATE, "stand " + ps + " " + lk, "$v");
        } else {
            // the input of the member off the bean the chain reaches, read as the binding's form says
            items.addAll(chain.subList(0, chain.size() - 1));
            items.add(withBinding(chain.get(chain.size() - 1), new Binding("$v", List.of(),
                    new Binding.Field(member.javaClass(), member.name()), inputForm(member), null)));
            value = oneSlot(ReadKind.REACHED_INPUT, "invoer " + ps + " " + lk, "$v");
        }
        RulePlan present = new RulePlan(name + "link aanwezig", null, null, value, items,
                new Consequence.InsertPathState(SUBJECT, ps, lk, Consequence.StateSource.REACHED, 0, Consequence.Insertion.LOGICAL));

        // no combination of the chain's objects: what the chain declares lives inside the negation, and it reads the
        // subject alone from before it
        List<FactPattern> none = chain(path.hops(), SUBJECT, null);
        RulePlan absent = new RulePlan(name + "link afwezig", null, null, new ReadTable(List.of()),
                List.of(new Subject(javaClass, SUBJECT, List.of(), List.of(), List.of()),
                        new Not(none, Exists.declared(none), List.of(SUBJECT))),
                new Consequence.InsertPathState(SUBJECT, ps, lk, Consequence.StateSource.MISSING_LINK, -1,
                        Consequence.Insertion.LOGICAL));
        return List.of(present, absent);
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
            Class<?> javaClass = beanClass(h.destination());
            String far = "$stap_" + id("keten");
            if (h.relation() != null) {
                boolean earliest = h.nearIsFirst();
                String relationLink = "$koppeling_" + id("keten");
                String otherSide = "$overzijde_" + id("keten");
                Alpha ofRelation = equal(id("koppeling"), "relation", h.relation());
                Join onNear = new Join(id("koppeling-nabij"), earliest ? "first" : "second", near);
                // a creation rule does not walk a link of the phase it fires in: the rule layer has not seen it yet
                List<BetaTest> earlier = phase == null ? List.of()
                        : List.of(new BetaTest(id("koppeling-eerder"), StaticFunction.NOT_IN_PHASE, phase));
                out.add(new FactPattern(RelationLink.class, relationLink, List.of(ofRelation), List.of(onNear), earlier,
                        List.of(Binding.of(otherSide, RelationLink.class, earliest ? "second" : "first"))));
                out.add(new FactPattern(javaClass, far, List.of(), List.of(new Join(id("koppeling-ver"), null, otherSide)),
                        List.of()));
            } else {
                // the field on the near bean reaches the far one, or the field on the far bean the near one
                // the walk reads bean properties, as every chain of the plan holds them
                List<String> field = List.of(Namen.property(h.field()));
                BetaTest step = h.onNear()
                        ? new BetaTest(id("veld-nabij"), StaticFunction.MEETING, near,
                                List.of(constant(List.of()), constant(field)))
                        : new BetaTest(id("veld-ver"), StaticFunction.MEETING, near,
                                List.of(constant(field), constant(List.of())));
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
    private record Member(ElementKey key, ObjectType declarant, String name, boolean characteristic, Datatype datatype,
                       Class<?> javaClass, RegelSpraakModel.Granularity time) {}

    /** Whether {@code member} is an attribute on a timeline: one the branch reads through its standing, derived or input,
     *  whose input stands in the convention the case gave it ({@link #attributeStateRules}). */
    private static boolean timelineAttribute(Member member) {
        return member.time() != null && !member.characteristic();
    }

    /** How the input member {@code member} is read off a bean of a type that declares it, the decision a plan's binding
     *  carries ({@link Binding.ReadForm}): a characteristic on a timeline as its true periods, a {@code List<Period>}
     *  ({@link Period#list}), null for an absent timeline, a characteristic that holds one truth value as whether it
     *  holds {@code true}, and any other member as its value. An attribute on a timeline is read through its standing
     *  instead ({@link #timelineAttribute}). */
    private static Binding.ReadForm inputForm(Member member) {
        if (member.time() != null) return Binding.ReadForm.PERIODS;
        if (member.characteristic()) return Binding.ReadForm.WAAR;
        return Binding.ReadForm.VALUE;
    }

    /** Member {@code name} as declared on {@code holder}'s chain, compared as the verifier compares names, holding one
     *  value. A member on a timeline is refused here: the positions that read or write one ask {@link #timeMember}. A
     *  member with dimensions is refused. */
    private Member member(ObjectType holder, String name, boolean characteristic) {
        Member member = declared(holder, name, characteristic);
        if (member.time() != null)
            throw new Refusal(DroolsCode.TIMED_MEMBER_SINGLE_PLACE.reason("category", Msg.drools(characteristic ? "PART.member-kind.characteristic-kind" : "PART.member-kind.attribute"), "member", member.name(), "objectType", member.declarant().name));
        return member;
    }

    /**
     * Member {@code name} as declared on {@code holder}'s chain, on a timeline: {@code description} reads or writes its timeline
     * (RUNTIME-SEMANTICS ch. 3). Refused by name: a member that holds one value there, one on a grid of more than one
     * unit, which carries no operation (ch. 3.2, OPEN in ch. 3.12), and one on a timeline with a variable start point,
     * whose grid is laid per object from an anchor a {@code StartPointDetermination} derives: no golden with cases
     * reads or writes one, and the case input lays no timeline on such a grid, so no case can confirm this branch's
     * reading of it against the reference interpreter.
     */
    private Member timeMember(ObjectType holder, String name, boolean characteristic, Object description) {
        Member member = declared(holder, name, characteristic);
        Msg category = Msg.drools(characteristic ? "PART.member-kind.characteristic-kind" : "PART.member-kind.attribute");
        if (member.time() == null)
            throw new Refusal(DroolsCode.TIMED_MEMBER_NOT_TEMPORAL.reason("description", description, "category", category, "member", member.name(), "objectType", member.declarant().name));
        if (Timelines.morePeriodic(characteristic ? member.declarant().characteristicTimelineCountMembers()
                : member.declarant().timelineCountMembers(), member.name()))
            throw new Refusal(DroolsCode.TIMED_MEMBER_PERIOD_GRID.reason("description", description, "category", category, "member", member.name(), "objectType", member.declarant().name));
        if (!characteristic && model.hasVariableStart(member.declarant(), member.name()))
            throw new Refusal(DroolsCode.TIMED_ATTRIBUTE_VARIABLE_START.reason("description", description, "member", member.name(), "objectType", member.declarant().name));
        return member;
    }

    /** Member {@code name} as declared on {@code holder}'s chain, its granularity set where it lies on a timeline. A
     *  member with dimensions is refused. */
    private Member declared(ObjectType holder, String name, boolean characteristic) {
        for (ObjectType t : holder.chain()) {
            if (characteristic) {
                for (String k : t.characteristics)
                    if (Namen.norm(k).equals(Namen.norm(name)))
                        return new Member(ProducerConsumer.elementOf(holder, name, true), t, k, true, null, beanClass(t),
                                t.characteristicGranularity.get(k));
            } else {
                for (Map.Entry<String, Datatype> e : t.attributes.entrySet())
                    if (Namen.norm(e.getKey()).equals(Namen.norm(name))) {
                        String a = e.getKey();
                        // every position that asks this method reads or writes the member whole, as one value or as a
                        // timeline; a dimensioned one is read only per cell, through dimensionedMember
                        if (t.attributeDimensions.containsKey(a))
                            throw new Refusal(DroolsCode.DIMENSIONED_READ_OUTSIDE_CELL.reason("attribute", a, "objectType", t.name));
                        return new Member(ProducerConsumer.elementOf(holder, name, false), t, a, false, e.getValue(),
                                beanClass(t), t.attributeGranularity.get(a));
                    }
            }
        }
        throw new Refusal(DroolsCode.MEMBER_NOT_DECLARED.reason("category", Msg.drools(characteristic ? "PART.member-kind.characteristic-kind" : "PART.member-kind.attribute"), "member", name, "objectType", holder.name));
    }

    /** The dimensioned attribute {@code name} as declared on {@code holder}'s chain (§3.6): its bean member is a
     *  {@code Map<label, value>}, read whole by a dimension cell or a fold over a dimension selection. An attribute that
     *  is not dimensioned, or that also lies on a timeline, is refused by name. */
    private Member dimensionedMember(ObjectType holder, String name) {
        for (ObjectType t : holder.chain())
            for (Map.Entry<String, Datatype> e : t.attributes.entrySet())
                if (Namen.norm(e.getKey()).equals(Namen.norm(name))) {
                    String a = e.getKey();
                    if (!t.attributeDimensions.containsKey(a))
                        throw new Refusal(DroolsCode.DIMENSION_READ_NOT_DIMENSIONED.reason("attribute", a, "objectType", t.name));
                    if (t.attributeGranularity.containsKey(a))
                        throw new Refusal(DroolsCode.DIMENSIONED_ATTRIBUTE_TIMELINE.reason("attribute", a, "objectType", t.name));
                    return new Member(ProducerConsumer.elementOf(holder, name, false), t, a, false, e.getValue(),
                            beanClass(t), null);
                }
        throw new Refusal(DroolsCode.ATTRIBUTE_NOT_DECLARED.reason("attribute", name, "objectType", holder.name));
    }

    /** A path of to-one roles from a subject type over the reference fields of the beans: the walk
     *  {@link FactModelGenerator#navigationWalk} derives from the wire's bindings, the reference field of each hop
     *  over an input relation on its bean class ({@code fields}, which a plan reads the path along, and only for a path
     *  whose links are all input, {@link #created}), the hops, and the type the last hop reaches. {@code key}
     *  names the subject type and the fields, as the {@link PathState} facts of the path carry it. */
    private record Path(String key, ObjectType subject, List<String> roles, ObjectType reaches,
                       List<Binding.Field> fields, List<Hop> hops) {
        /** Whether a hop of the path goes over a relation a form creates, whose links change while the session fires:
         *  such a path is read over {@link RelationLink} facts, never off the beans. */
        boolean created() {
            return hops.stream().anyMatch(h -> h.relation() != null);
        }
    }

    /** One hop of a path or a range, from an object of type {@code van} to one of type {@code destination}: over an input
     *  relation by the reference field {@code field} on the bean of one end ({@code onNear}: on the near object, else on
     *  the far one), over a relation a form creates by its {@link RelationLink} facts ({@code relation} set,
     *  {@code nearIsFirst} saying which end of the link the near object is, {@code field} null). */
    private record Hop(ObjectType van, ObjectType destination, String field, boolean onNear, String relation,
                       boolean nearIsFirst) {}

    /** A derived member read over a path: one family of path-standing rules. */
    private record PathRead(Path path, Member member) {
        String key() { return path.key() + " " + FactNames.member(member.key()); }
    }

    private Path path(ObjectType subject, List<RoleBindingRef> bindings, ObjectType endpoint, Object description) {
        FactModelGenerator.NavigationWalk w =
                FactModelGenerator.navigationWalk(model, subject, bindings, endpoint);
        if (w == null) throw new Refusal(DroolsCode.NAVIGATION_DOES_NOT_RESOLVE.reason("description", description, "subject", subject.name));
        if (w.toMany()) throw new Refusal(DroolsCode.NAVIGATION_TO_MANY_ROLE.reason("description", description));
        if (w.roles().size() != bindings.size())
            throw new IllegalStateException(description + ": de wandeling telt " + w.roles().size() + " stappen en de draad "
                    + bindings.size() + " bindingen");
        List<Binding.Field> fields = new ArrayList<>();
        List<Hop> hops = new ArrayList<>();
        ObjectType cur = subject;
        for (int i = 0; i < w.roles().size(); i++) {
            String role = w.roles().get(i);
            ObjectType next = w.reaches().get(i);
            Hop created = linkHop(bindings.get(i), cur, next);
            if (created != null) {
                hops.add(created);
                cur = next;
                continue;
            }
            // a role whose name a declared member took has no reference field of its own (§13.3.9)
            if (!FactModelGenerator.hasReferenceField(model, cur, role, next))
                throw new Refusal(DroolsCode.NAVIGATION_ROLE_NO_FIELD.reason("description", description, "objectType", cur.name, "role", role, "targetType", next.name));
            fields.add(new Binding.Field(beanClass(cur), role));
            hops.add(new Hop(cur, next, role, true, null, false));
            cur = next;
        }
        return new Path(subject.name + ":" + String.join(".", w.roles()), subject, w.roles(), cur, List.copyOf(fields),
                List.copyOf(hops));
    }

    /** The hop over {@code binding} from {@code van} to {@code destination} where its relation is one a form creates, read over
     *  {@link RelationLink} facts; null for an input relation. The binding names the role the far end fills, so the near
     *  end is the first of the link where that role is the fact type's second. */
    private Hop linkHop(RoleBindingRef binding, ObjectType van, ObjectType destination) {
        if (binding == null || binding.factType() == null || !createdRelations.contains(Namen.norm(binding.factType())))
            return null;
        RegelSpraakModel.FactType ft = factType(binding.factType());
        if (ft == null || ft.roles.size() != 2 || ft.reciprocal)
            throw new Refusal(DroolsCode.CREATED_RELATION_STEP_NONBINARY.reason("factType", binding.factType()));
        boolean farIsSecond = Namen.norm(ft.roles.get(1).name()).equals(Namen.norm(binding.role()));
        return new Hop(van, destination, null, false, Namen.norm(ft.name), farIsSecond);
    }

    /** The declared fact type named {@code name}, compared as the verifier compares names, or null. */
    private RegelSpraakModel.FactType factType(String name) {
        for (RegelSpraakModel.FactType ft : model.factTypes.values())
            if (Namen.norm(ft.name).equals(Namen.norm(name))) return ft;
        return null;
    }

    private Class<?> beanClass(ObjectType ot) {
        try {
            return Class.forName(FactModelGenerator.PACKAGE + "." + Namen.ident(ot.name, true), true, beanLoader);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("geen beanklasse voor objecttype " + ot.name, e);
        }
    }

    private String id(String category) { return category + "_" + (idSeq++); }

}
