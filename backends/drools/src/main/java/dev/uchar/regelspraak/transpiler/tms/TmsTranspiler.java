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
import dev.uchar.regelspraak.transpiler.kern.Bronpad;
import dev.uchar.regelspraak.transpiler.kern.Dagsoorten;
import dev.uchar.regelspraak.transpiler.kern.Dekking;
import dev.uchar.regelspraak.transpiler.kern.EffectieveEenheid;
import dev.uchar.regelspraak.transpiler.kern.Eenheidsconflict;
import dev.uchar.regelspraak.transpiler.kern.Gastheer;
import dev.uchar.regelspraak.transpiler.kern.Namen;
import dev.uchar.regelspraak.transpiler.kern.Operatoren;
import dev.uchar.regelspraak.transpiler.kern.StaandeDoelen;
import dev.uchar.regelspraak.transpiler.kern.Tijdlijnen;
import dev.uchar.regelspraak.transpiler.kern.Vensterpositie;
import dev.uchar.regelspraak.transpiler.tms.plan.Accumulatie;
import dev.uchar.regelspraak.transpiler.tms.plan.Alfa;
import dev.uchar.regelspraak.transpiler.tms.plan.Bestaat;
import dev.uchar.regelspraak.transpiler.tms.plan.Binding;
import dev.uchar.regelspraak.transpiler.tms.plan.Functie;
import dev.uchar.regelspraak.transpiler.tms.plan.Gevolg;
import dev.uchar.regelspraak.transpiler.tms.plan.Join;
import dev.uchar.regelspraak.transpiler.tms.plan.Leessoort;
import dev.uchar.regelspraak.transpiler.tms.plan.Leestabel;
import dev.uchar.regelspraak.transpiler.tms.plan.LhsItem;
import dev.uchar.regelspraak.transpiler.tms.plan.Linkerkant;
import dev.uchar.regelspraak.transpiler.tms.plan.Niet;
import dev.uchar.regelspraak.transpiler.tms.plan.Onderwerp;
import dev.uchar.regelspraak.transpiler.tms.plan.Patroon;
import dev.uchar.regelspraak.transpiler.tms.plan.Regelplan;
import dev.uchar.regelspraak.transpiler.tms.plan.Term;
import dev.uchar.regelspraak.transpiler.tms.plan.Toetsitem;
import dev.uchar.regelspraak.transpiler.tms.plan.Waardeplan;

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
import org.drools.model.functions.Function1;
import org.drools.model.functions.Predicate1;
import org.drools.model.impl.ModelImpl;
import org.kie.api.runtime.ClassObjectFilter;
import org.kie.api.runtime.KieSession;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
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
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * The lowering of the exec-model branch: a uchar IR document to a Drools executable model that derives with
 * {@code drools.logicalInsert}. A derived fact stands exactly as long as the condition of a rule that inserted it
 * holds, and goes, with everything that rested on it, when that condition stops holding. The outcome of an acyclic
 * rule set then does not depend on which rule the agenda fires first, which a lowering that writes the beans secures
 * only with measures of its own (a rule attribute against re-firing, a priority per stratum, class reactivity, a
 * conditional bean update, a record of an initialisation's own write), as the DRL-text branch does and the exec-model
 * branch did until card tms-omschakeling (2026-10-10). This branch carries none of them; the fire cap of
 * {@code DroolsBackend.vuur} stays.
 *
 * <p>Truth maintenance follows only what the left-hand side of a rule binds: a value a consequence reads with a
 * getter, or looks up in working memory itself, records no dependency, so a derived fact computed from it stays when
 * that value changes. Every read a derived value depends on is therefore a bound pattern here, and a consequence
 * computes only with bound variables. A member no form writes is input, stable during a session, and is bound off the
 * bean pattern, over the reference fields where the read navigates; a derived member is read through its total
 * standing ({@link KenmerkStand}, {@link Stand}), over a path through {@link PadStand}, a positive pattern whatever
 * polarity a check tests; the §9.5 channel of a form through {@link KanaalStand}; a parameter through its
 * {@link ParameterStand}. The case input stays stated on the beans and the branch never writes a bean while it fires,
 * so an initialisation's guard reads the input of its derived target off the bean as well; {@link #zetAfgeleideWaarden}
 * writes the derivations onto the beans afterwards.
 *
 * <p>What it lowers: a characteristic attribution; an assignment or a computation of an attribute of the subject, which
 * writes nothing for a computed leeg on a standing target (RUNTIME-SEMANTICS ch. 7.8, or beside one unconditional
 * initialisation, the desugaring domain of ch. 7.7) and writes the leeg elsewhere (ch. 1.2g), any number of them on one
 * target, two that stand on one object with different values being the fault of ch. 5.7 ({@link #verschilregel}); an
 * initialisation (ch. 5.4), the only one on its target per concrete type; a
 * consistency, whose violation is a logical §9.5 inconsistency; a uniqueness, over one source or more, whose
 * duplicates are as well ({@link #uniciteitsregels}); a prioritized-defaults group, as the effective
 * stack per concrete type of ch. 7.10, which decides by logical control facts ({@link Stuurfeit}) and into which an
 * ordinary rule on an ancestor's level folds, so that it does not fire on that type; an allocation, as a computation
 * over the recipients its left-hand side binds ({@link #verdeling}); and an object creation, over its
 * {@code connectingRole} binding or else over the relation its type pair derives, and a fact creation, each over a
 * relation of two roles. Each always valid or gated on the
 * evaluation date by its validity window (RUNTIME-SEMANTICS ch. 8.2), with the §6.5 deposit of a division by leeg and
 * the one of a square root of a negative number where the evaluation reaches them (a default carries no division and no
 * root at all), whose condition, assertion or justification is built from characteristic checks (own or over a path,
 * negated or not), comparisons, leeg, gevuld, eleven and exact-digit tests, time windows, role checks
 * ({@link #rolcontrole}), quantifier folds over a linked set ({@link #kwantorvouw}), tests of the channel of a
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
 * {@link Bijdrage} facts its contribution rules insert logically, one per member of its range ({@link #vouw}); a
 * contribution rule binds the member's value and the reads of the fold's filter in its own left-hand side, so a member
 * whose value or filter reading changes takes its contribution along. A listed fold is computed from its operands'
 * bound values ({@link #gelijsteVouw}). A quantifier fold has the same contributions, each carrying its member's
 * verdict on the matrix, and where the matrix can fault or deposit also the member's place, so the rule that reads the
 * fold replays the short circuit of RUNTIME-SEMANTICS ch. 5.10 over them ({@link #kwantorvouw}).
 *
 * <p>An evaluation fault in a value or a condition — a division by a numeric zero (RUNTIME-SEMANTICS ch. 1.4, point 4),
 * or one of the faults of {@link Rekenfout}: a date step or a duration count outside the range of ch. 3.8, a context
 * value or a validity window without an evaluation date, an {@code Unresolved} node the evaluation reaches — is a
 * {@link Fout} inserted logically and never an exception out of the fire: the branch passes intermediate states the
 * reference interpreter does not see, and a fault thrown there could not be withdrawn when the state passes. The
 * consequence that computes a value inserts the fault instead of the value. A condition is tested in the left-hand
 * side, where Drools cannot take an exception as a fact, so its test answers that it does not hold where it faults,
 * and a rule of the form's own inserts the fault ({@link #voorwaardefoutregel}). A fold's filter that faults for a
 * member inserts no fault but a {@link Filterfout}: the form that reads the fold faults where its own evaluation reaches
 * the fold, as the reference interpreter evaluates the filter only there, with the fault of the first member in fold
 * order whose filter faults, a place the case input states per to-many list ({@link Lijstplaats}) ({@link #vouw}). Only
 * the faults that stand at
 * the fixpoint count: {@link #zetAfgeleideWaarden} reports them before it writes anything back. A division of a filled
 * dividend by a leeg divisor, and a square root of a negative number, deposit the §9.5 inconsistency of ch. 1.2 under
 * the form whose evaluation reaches them, on its subject: the consequence that computes a value through a probe in its
 * value array ({@link Sonde}), a condition through a rule of the form's own ({@link #deponeerregel}), and a fold's
 * filter through a {@link Filterdeponering} of the member, which the form that reads the fold hears where its own
 * evaluation reaches the fold ({@link #gelezenVouw}).
 *
 * <p>A creation is the one form whose output is stated, not logical (RUNTIME-SEMANTICS ch. 5.5): a creation fires only
 * at a fixpoint of the rule layer, and what it makes stands, as in the reference interpreter, which keeps a minted
 * instance under a fixed label. A logical object could keep itself standing through its own output after the state that
 * made it had passed, and its initialisations would follow later changes, where ch. 5.5 evaluates them when the object
 * is made. Its rules sit in agenda groups of their own ({@link #FEITCREATIE}, {@link #OBJECTCREATIE}) and the driver
 * fires them in generations ({@code DroolsBackend.vuur}): the rule layer to rest, then the fact creations once and the
 * object creations once, both on that one fixpoint, then the rule layer to rest again, and so on, until a generation
 * makes nothing; an object creation records the creation depth of what it makes ({@link Gemaakt}), and a mint at depth
 * {@link #CREATIEBUDGET} + 1 is the fault ({@link Creatiebudget}). The driver runs the generations per evaluation
 * stratum of the reference interpreter, in
 * ascending order ({@link #creatiestrata}): a creation reads strictly, so it fires only once the creations whose output
 * it reads have reached their fixpoint, as the reference interpreter evaluates stratum k to its fixpoint before a form
 * of stratum k+1 fires (RUNTIME-SEMANTICS ch. 6.1, 6.3); an initialisation evaluated earlier would be fixed on a value
 * that grows. An object creation inserts
 * the new bean, a {@link Gemaakt} mark per creation and subject, the {@link Koppeling} of its connecting relation and
 * its initialisations as {@link Waarde} and {@link HeeftKenmerk}, computed from what its rule binds
 * ({@link #objectCreatie}); a fact creation inserts the {@link Koppeling} it makes ({@link #feitCreatie}). Every relation
 * a form creates is read through {@link Koppeling} facts, its input links included ({@link #koppelingregels}), since a
 * created link is in no bean field, and a second distinct filler of a to-one role is a {@link Fout} at the fixpoint.
 * The rule layer stays under truth maintenance throughout: it reads what a creation made as it reads input.
 */
public final class TmsTranspiler {

    /** The Drools package of this branch's rules. The rule of a form carries the form's name, as in the exec-model
     *  branch, so a consumer that loads models of two branches side by side keeps them apart by package. */
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
    public static final int CREATIEBUDGET = 100;

    /** The most values one condition of this branch reads at once: the DSL's standalone test takes at most thirteen
     *  bound variables. A consequence has no such ceiling ({@link Gevolg}). */
    static final int MAX_LEZINGEN = 13;

    private static final String NOG_NIET = "deze tak draagt deze vorm nog niet";
    static final String INCONSISTENTIE = FactModelGenerator.PAKKET + ".Inconsistentie";

    private final RegelSpraakModel model;
    private final ClassLoader beanLoader;
    private final List<PrioritizedDefaults> defaults;
    private final Map<Object, List<RolBindingRef>> navigatieBindingen;
    /** The one-step range of a fold ({@code sourceRole}), by node. */
    private final Map<Object, RolBindingRef> rolBindingen;
    /** The multi-step range of a fold ({@code sourcePath}), by node. */
    private final Map<Object, List<RolBindingRef>> bronPaden;
    /** The folds lowered so far, per node and fold subject: a node read again, by a rule's fault rule or by a default
     *  that two effective stacks share, reads the contributions already made for it. */
    private final Map<Object, Map<Objecttype, Vouw>> vouwen = new IdentityHashMap<>();
    /** The contribution rules of the folds the lowered forms read, by the fold's key. */
    private final Map<String, List<Rule>> bijdrageregels = new LinkedHashMap<>();
    private int vouwteller;
    /** While the rules of a fold are built ({@link #vouwVoor}), the binders in scope at the fold, by normalised name:
     *  the rule that reads the fold binds them, its contribution rules do not ({@link Lezingen#buiten}). */
    private Set<String> vouwbinders = Set.of();
    /** The name of the form being lowered, as the manifest shows it: the key of its folds starts with it, and a fault in
     *  a fold's filter is named after it. */
    private String vorm;
    /** The datatype and unit of an expression as the other branches derive them (RUNTIME-SEMANTICS ch. 2.10). */
    private final EffectieveEenheid eff;
    /** Every member a form of the document writes, lowered or not. A member that no form writes is input. */
    private Map<ElementSleutel, Set<String>> schrijvers = Map.of();
    /** Per attribute, the assignments and computations of the document that write it. */
    private final Map<ElementSleutel, List<ResolvedRegel>> rekenregels = new LinkedHashMap<>();
    /** Per attribute, the initialisations of the document that write it. */
    private final Map<ElementSleutel, List<ResolvedRegel>> initialisaties = new LinkedHashMap<>();
    /** The members a defaults group writes, on their declaring type. */
    private final Set<ElementSleutel> defaultsdoelen = new LinkedHashSet<>();
    /** The effective stacks of RUNTIME-SEMANTICS ch. 7.10 and the fold of ordinary rules into them. */
    private Stapels stapels;
    /** The targets on which a computed leeg writes nothing (RUNTIME-SEMANTICS ch. 7.7 and 7.8), classified by the class
     *  both branches share. */
    private StaandeDoelen staandeDoelen;
    /** The place of each rule of the document in its list, by name: the sentence of two writers that disagree names
     *  them in this order, as the reference interpreter's pass runs them (RUNTIME-SEMANTICS ch. 5.7). */
    private final Map<String, Integer> documentplaats = new HashMap<>();
    /** The object type of each bean class, by the class's simple name (the one {@link #bonenklasse} loads): a sentence
     *  names an object by its own type. Keyed by name, so that building it loads no class. */
    private final Map<String, String> typeNamen = new HashMap<>();
    private final List<Dekking> dekking = new ArrayList<>();
    /** The form each rule of the model belongs to, by the rule's name ({@link #vormVan}). */
    private final Map<String, String> vormVanRegel = new HashMap<>();
    /** The rules of the model that belong to no form of the document ({@link #vormVan}). */
    private final Set<String> regelsZonderVorm = new LinkedHashSet<>();
    private final List<Defaultsgroep> defaultsgroepen = new ArrayList<>();
    /** The planned left-hand sides of the lowered rules, in the order {@link Lezingen#linkerkant} made them. */
    private final List<Linkerkant> linkerkanten = new ArrayList<>();
    /** The planned values the consequences of the lowered rules compute, in the order the rules were lowered. */
    private final List<Waardeplan> waardeplannen = new ArrayList<>();
    /** The plans of the rules the branch renders whole from their plan, in the order they were built. */
    private final List<Regelplan> regelplannen = new ArrayList<>();
    /** The members the lowered rules derive, by key. The key is normalised, and the setter a derivation is written back
     *  through is the bean property of the declared spelling, which the key no longer carries. */
    private final Map<ElementSleutel, Lid> afgeleid = new LinkedHashMap<>();
    /** The members a lowered rule reads through their standing, on its subject or at the end of a path: the derived
     *  ones, and every attribute on a timeline ({@link Lezingen#tijdlijn}). Each gets its standing rules. */
    private final Map<ElementSleutel, Lid> gelezen = new LinkedHashMap<>();
    /** The reads of a derived member over a path: each gets its two path-standing rules. */
    private final Map<String, Padlezing> padlezingen = new LinkedHashMap<>();
    /** The producers whose §9.5 channel a lowered condition reads, by the name the channel test spells: each gets the
     *  two rules of its {@link KanaalStand}. */
    private final Set<String> kanalen = new LinkedHashSet<>();
    /** The parameters a lowered rule reads, as the globals the case runner sets by name. */
    private final Map<String, Global<?>> globals = new LinkedHashMap<>();
    /** The global the case runner sets from the case's {@code asOfDate}, the session's evaluation date
     *  (RUNTIME-SEMANTICS ch. 8.1), read through the {@link Rekendatum} fact; null until a lowered form needs the date,
     *  so a model whose forms are all always valid and read no context value declares none. */
    private Global<LocalDate> rekendatumGlobal;
    /** The name of that global, the one the case runner sets for every branch. */
    static final String REKENDATUM = "rekendatum";
    /** The relations a form of the document creates, by normalised fact type name: the connecting relation of an
     *  object creation and the relation of a fact creation. Their links are {@link Koppeling} facts, and every form
     *  that reads such a relation joins over those ({@link #koppelingshop}, {@link #koppelingsbereik}). */
    private final Set<String> gecreeerdeRelaties = new LinkedHashSet<>();
    /** The names of the lowered creation rules, those of {@link #FEITCREATIE} and of {@link #OBJECTCREATIE}: the driver
     *  counts their firings to tell a generation that created something from one that did not. */
    private final Set<String> creatieregels = new LinkedHashSet<>();
    /** The evaluation stratum of every form of a document with a creation, by its node ({@link Kringen#strata}). */
    private Map<Object, Integer> strata = Map.of();
    /** The strata of the lowered creation rules ({@link #creatiestrata}). */
    private final Set<Integer> creatiestrata = new TreeSet<>();
    /** The reference fields under which a rule reads a {@link Lijstplaats} ({@link #plaats}; see {@link #lijstvelden}). */
    private final Set<String> lijstvelden = new LinkedHashSet<>();
    /** Unique per constraint across the model: the executable model shares a constraint between rules by its id, so
     *  two different tests under one id would collapse into the first. */
    private int idSeq;
    private Model model0;
    /** The day-kind definitions of the document, each compiled to its predicate over a calendar day where a form first
     *  steps or counts in its day kind ({@link #dagsoortpredikaat}). A definition reads nothing of working memory
     *  (WF-031), so its predicate is a constant of the document and no rule binds anything for it. */
    private final Dagsoorten dagsoorten;

    private TmsTranspiler(RegelSpraakModel model, ClassLoader beanLoader, List<PrioritizedDefaults> defaults,
                          Map<Object, List<RolBindingRef>> navigatieBindingen, Map<Object, RolBindingRef> rolBindingen,
                          Map<Object, List<RolBindingRef>> bronPaden, EffectieveEenheid eff) {
        this.model = model;
        this.beanLoader = beanLoader != null ? beanLoader : TmsTranspiler.class.getClassLoader();
        this.defaults = defaults;
        this.navigatieBindingen = navigatieBindingen;
        this.rolBindingen = rolBindingen;
        this.bronPaden = bronPaden;
        this.eff = eff;
        this.dagsoorten = new Dagsoorten(model);
    }

    /** Build the model and its coverage manifest from the loaded model and every side table the loader returns. Each
     *  table is required, also those no lowered form reads yet: a build that could leave one out could also lower a
     *  form that needs it as if the document carried none. Fact beans resolve against {@code beanLoader}; null routes
     *  to this transpiler's own loader. */
    public static TmsTranspiler build(RegelSpraakModel m, ClassLoader beanLoader, List<PrioritizedDefaults> defaults,
            Map<Object, RolBindingRef> rolBindingen, List<Expr.Let> lets,
            Map<Object, List<RolBindingRef>> bronPaden,
            Map<Object, List<RolBindingRef>> navigatieBindingen) {
        Objects.requireNonNull(defaults, "side table 'defaults' is null — pass an empty list for a document without one");
        Objects.requireNonNull(rolBindingen, "side table 'rolBindingen' is null — pass an empty map for a document without one");
        Objects.requireNonNull(lets, "side table 'lets' is null — pass an empty list for a document without one");
        Objects.requireNonNull(bronPaden, "side table 'bronPaden' is null — pass an empty map for a document without one");
        Objects.requireNonNull(navigatieBindingen, "side table 'navigatieBindingen' is null — pass an empty map for a document without one");
        // copied by identity: the loader keys its tables on the nodes, which are records with value equality
        Map<Object, List<RolBindingRef>> nav = new IdentityHashMap<>(navigatieBindingen);
        TmsTranspiler t = new TmsTranspiler(m, beanLoader, defaults, nav, new IdentityHashMap<>(rolBindingen),
                new IdentityHashMap<>(bronPaden), new EffectieveEenheid(lets));
        t.model0 = t.run();
        return t;
    }

    public Model model() { return model0; }

    /** The coverage manifest: one verdict per form, the rules in document order, then every other family. */
    public List<Dekking> dekking() { return dekking; }

    /**
     * The form of the document that rule {@code regelnaam} of the model belongs to, by the name the manifest gives it
     * ({@link #dekking}): the form whose lowering built the rule, with every rule that lowering adds beside the form's
     * own (its fault and deposit rules, the keys of a uniqueness, the recipients of an allocation, the chains and
     * agreements of an effective defaults stack), and for the contribution rules of a fold the form that first
     * registered the fold, also where a later form reads the same fold. Null for a rule of no form, one the lowering
     * keeps for every form alike: a standing, a path or channel standing, the standing of a parameter or of the
     * evaluation date, a link of the input over a created relation and its second filler, and the comparison of the
     * writers of one target. A name of no rule of the model is refused.
     */
    public String vormVan(String regelnaam) {
        String vorm = vormVanRegel.get(regelnaam);
        if (vorm != null || regelsZonderVorm.contains(regelnaam)) return vorm;
        throw new IllegalArgumentException("geen regel van dit model: '" + regelnaam + "'");
    }

    /** The names of the creation rules of the model, in the agenda groups {@link #FEITCREATIE} and
     *  {@link #OBJECTCREATIE}; empty for a document without a lowered creation, which the driver fires in one go. */
    public Set<String> creatieregels() { return Set.copyOf(creatieregels); }

    /** The evaluation strata of the creation rules of the model, ascending, as the reference interpreter computes them
     *  over the verifier's dependency graph (RUNTIME-SEMANTICS ch. 6.3): the driver runs the generations of each in
     *  turn, and a creation rule fires in the phases of its own stratum and of every stratum above it
     *  ({@link Creatiefase}). Empty where {@link #creatieregels} is. */
    public List<Integer> creatiestrata() { return List.copyOf(creatiestrata); }

    /** Whether a rule of the model reads a {@link Lijstplaats}: the filter fault rule of a fold whose filter can fault,
     *  the member fault rule of a per-element fold over a timeline, or the contribution rule of a quantifier fold whose
     *  matrix can fault or deposit, each over a range with a to-many step. Only then does the case input state the
     *  places of its to-many lists, and {@code DroolsBackend.voegIn} states them for a consumer. */
    public boolean leestLijstplaatsen() { return !lijstvelden.isEmpty(); }

    /** The reference fields under which a rule of the model reads a {@link Lijstplaats}: each the field a member of a
     *  to-many list holds toward the list's holder, named after the holder's role as the bean property is
     *  ({@link Lijstplaats#veld}). Empty where {@link #leestLijstplaatsen} is false. */
    public Set<String> lijstvelden() { return Set.copyOf(lijstvelden); }

    /**
     * The size of the lowering of one effective defaults stack, counted when it is built: {@code naam} as the manifest
     * and the control facts name it, whether it took the general form (exception groups, or more than one group at the
     * root) or the simple one, the number of Drools rules it consists of, and the kinds of control fact those rules
     * insert ({@link Stuurfeit}). The standing rules of the target are shared with every other reader and not counted.
     */
    public record Defaultsgroep(String naam, boolean algemeen, int regels, List<String> stuurfeiten) {}

    /** One entry per effective stack the branch lowered, in the order of the model. */
    public List<Defaultsgroep> defaultsgroepen() { return List.copyOf(defaultsgroepen); }

    /** The plan of the left-hand side of every lowered rule that reads through {@link Lezingen}, in the order the rules
     *  were lowered, of the forms that lowered: what a rendering takes. Until step 1c of card
     *  drl-teksttak-op-truth-maintenance it holds what {@link Lezingen} decides ({@link Linkerkant}). No consumer reads a
     *  plan, so it is no public entry. */
    List<Linkerkant> linkerkanten() { return List.copyOf(linkerkanten); }

    /** The planned value of every consequence of a lowered rule that computes one, in the order the rules were lowered,
     *  of the forms that lowered ({@link Waardeplan}). Until step 1c of card drl-teksttak-op-truth-maintenance they stand
     *  apart from the left-hand sides. No consumer reads a plan, so it is no public entry. */
    List<Waardeplan> waardeplannen() { return List.copyOf(waardeplannen); }

    /** The plan of every rule the branch renders whole from its plan ({@link Regelplan}), in the order the rules were
     *  built, of the forms that lowered: the ordinary rules, their fault, deposit and validity rules, the rules of two
     *  writers of one target, and the standings of parameters and the evaluation date. The rules of the other forms are
     *  built directly until step 1c of card drl-teksttak-op-truth-maintenance plans them. No consumer reads a plan, so
     *  it is no public entry. */
    List<Regelplan> regelplannen() { return List.copyOf(regelplannen); }

    /**
     * What stands in {@code sessie} after a fire that reached its fixpoint, read without writing anything on a bean:
     * the derived values and characteristics, the objects and links the creations made, the evaluation faults that
     * stand, as data, and the §9.5 deposits. {@code label} names the objects of the case in the sentence of a fault, as
     * {@link #zetAfgeleideWaarden(KieSession, Function)} does; null names an object by type and identity.
     *
     * <p>Two different values on one attribute of one object have no common fixpoint (RUNTIME-SEMANTICS ch. 5.7). The
     * rule of {@link #verschilregel} makes that a {@link Fout} of the fixpoint where two writers can disagree; where no
     * such rule stood, as in a session filled by hand, this reads the same {@link Schrijversconflict} as a fault and gives
     * the attribute no value: it does not choose. Equal values are no conflict.
     *
     * <p>It reads only a session whose last run through the driver ({@code DroolsBackend.vuur}) reached its fixpoint,
     * as the driver's mark says ({@link Gevuurd}), and refuses any other with a sentence that says what is missing: a
     * session fired with a plain {@code fireAllRules} or not at all holds no fixpoint it can tell, and on a model with a
     * creation the creation rules never had the focus, so it holds no created object or link; a run that ended at the
     * cap, at the creation budget or with an exception holds a state on the way to the fixpoint. It refuses as well a
     * session in which something happened outside the driver after its last run, a fact inserted, updated or deleted or
     * a rule that fired ({@link Bewaking}), naming what happened and what to do: fire again through the driver, or open
     * a fresh session where the driver refuses a further fire; and one in which a global or, without an update, a
     * relation changed, which the driver refuses. What this gives is thus always a fixpoint, that of the input as the
     * session knows it: an attribute or a characteristic set without an update is not seen.
     */
    public Fixpunt lees(KieSession sessie, Function<Object, String> label) {
        return lezing(sessie, label).fixpunt();
    }

    /**
     * Writes what stands at the fixpoint of {@code sessie} onto the beans, exactly what {@link #lees} gives, read by the
     * same code: {@code setX(true)} for each derived characteristic, the value of each derived attribute through the
     * setter of the declared attribute, a timeline as the bean model's segments, and each link a creation laid on the
     * reference field of every to-one end the bean model gives a field, the field the input of such a link is read from
     * ({@link #koppelingregels}). Input on a characteristic stays, since nothing writes {@code false}; input on an
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
     * fire would read as input. So after the writes this states a {@link Teruggezet} mark, and the driver refuses to fire
     * the session again ({@code DroolsBackend.vuur}). It states the mark with the {@link Bewaking} off, as its own work,
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
        Lezing lezing = lezing(sessie, label);
        List<Fout> fouten = lezing.fixpunt().fouten();
        if (fouten.size() == 1) throw fouten.get(0).fout();
        if (!fouten.isEmpty()) throw new Evaluatiefouten(fouten);
        lezing.schrijfdaden().forEach(Runnable::run);
        // the entry's own insert and links: the session is read after them as before them
        Bewaking bewaking = Bewaking.van(sessie);
        bewaking.zonder(sessie, () -> sessie.insert(new Teruggezet()));
        bewaking.legRelatiesVast();
    }

    /** What {@link #lees} reads, with beside it the writes that put it on the beans, made from the same facts in the
     *  same pass so that the reading and the write-back cannot differ. */
    private record Lezing(Fixpunt fixpunt, List<Runnable> schrijfdaden) {}

    /** What a consumer does where something happened outside the driver after its last run ({@link Bewaking}): fire
     *  again, or open a fresh session where the driver refuses a further fire ({@code DroolsBackend.vuur}). */
    private String vuurWeg(KieSession sessie, Bewaking bewaking, String agenda) {
        if (!creatieregels.isEmpty())
            return "dit model heeft creatieregels, dus DroolsBackend.vuur weigert een tweede fire: open een verse sessie";
        if (!sessie.getObjects(new ClassObjectFilter(Teruggezet.class)).isEmpty())
            return "deze sessie is teruggezet, dus DroolsBackend.vuur weigert haar: open een verse sessie met beans die"
                    + " alleen invoer dragen";
        if (bewaking.erbijOfEraf() != null)
            return "invoer die buiten DroolsBackend.voegIn om bijkwam of wegging, vraagt een verse sessie, dus"
                    + " DroolsBackend.vuur weigert haar: open een verse sessie met alle beans in één Invoer";
        if (bewaking.meerDanEenUpdate() != null || agenda != null)
            return "DroolsBackend.vuur weigert een sessie waarin buiten vuur om meer gebeurde dan een update van een"
                    + " attribuut of kenmerk van een bean: open een verse sessie met alle beans in één Invoer";
        if (bewaking.veranderdeGlobal(sessie) != null)
            return "DroolsBackend.vuur weigert een sessie waarvan een global na de eerste vuur veranderde: open een verse"
                    + " sessie";
        return "vuur haar eerst opnieuw met DroolsBackend.vuur";
    }

    private Lezing lezing(KieSession sessie, Function<Object, String> label) {
        // per class, as the driver asks for it: a filter of another kind walks every fact the session holds
        List<Object> merken = new ArrayList<>(sessie.getObjects(new ClassObjectFilter(Gevuurd.class)));
        if (merken.isEmpty())
            throw new IllegalStateException("geen fixpunt om te lezen of terug te zetten: deze sessie is niet via"
                    + " DroolsBackend.vuur gevuurd, dat tot het fixpunt vuurt"
                    + (creatieregels.isEmpty() ? "" : " en de creaties van dit model in generaties laat vuren"));
        if (merken.size() > 1)
            throw new IllegalStateException("de sessie houdt " + merken.size() + " merken van DroolsBackend.vuur, waar"
                    + " de driver er na elke run één laat: " + merken);
        String geenFixpunt = ((Gevuurd) merken.get(0)).geenFixpunt();
        if (geenFixpunt != null)
            throw new IllegalStateException("geen fixpunt om te lezen of terug te zetten: de laatste DroolsBackend.vuur"
                    + " van deze sessie bereikte er geen — " + geenFixpunt);
        Bewaking bewaking = Bewaking.van(sessie);
        // the driver puts the watch back after every run, so only a consumer that took it off leaves a mark without it,
        // and the driver refuses such a session
        if (bewaking == null)
            throw new IllegalStateException("geen fixpunt om te lezen of terug te zetten: de bewaking die"
                    + " DroolsBackend.vuur op deze sessie zet, staat er niet meer op, dus wat er na de laatste vuur"
                    + " gebeurde, is niet te zien; DroolsBackend.vuur weigert haar ook: open een verse sessie met alle"
                    + " beans in één Invoer");
        String agenda = Bewaking.agenda(sessie);
        String buitenom = bewaking.buitenom();
        String wat = buitenom == null ? agenda : agenda == null ? buitenom : buitenom + " en " + agenda;
        if (wat != null)
            throw new IllegalStateException("geen fixpunt om te lezen of terug te zetten: sinds de laatste"
                    + " DroolsBackend.vuur van deze sessie gebeurde er iets buiten vuur om, wat het fixpunt van die vuur"
                    + " kan veranderen (" + wat + "); " + vuurWeg(sessie, bewaking, agenda));
        String global = bewaking.veranderdeGlobal(sessie);
        if (global != null)
            throw new IllegalStateException("geen fixpunt om te lezen of terug te zetten: " + global + ", en het fixpunt"
                    + " hoort bij de oude waarde; DroolsBackend.vuur weigert de sessie, dus open een verse sessie");
        String relatie = bewaking.veranderdeRelatie();
        if (relatie != null)
            throw new IllegalStateException("geen fixpunt om te lezen of terug te zetten: " + relatie + ", en het fixpunt"
                    + " hoort bij de oude relatie; DroolsBackend.vuur weigert de sessie, dus open een verse sessie met"
                    + " alle beans in één Invoer");
        List<Runnable> daden = new ArrayList<>();
        // a set: the fault of two writers that the rule of verschilregel stated and the one read off the values below
        // are one fault, equal as facts are
        Set<Fout> fouten = new LinkedHashSet<>();
        for (Object f : sessie.getObjects(o -> o instanceof Fout)) {
            Fout fout = (Fout) f;
            fouten.add(label != null && fout.fout() instanceof Schrijversconflict c
                    ? new Fout(fout.onderwerp(), fout.regel(), c.benoemd(label))
                    : label != null && fout.fout() instanceof Invoervormfout i
                    ? new Fout(fout.onderwerp(), fout.regel(), i.benoemd(label)) : fout);
        }
        List<Fixpunt.AfgeleidKenmerk> kenmerken = new ArrayList<>();
        for (Object f : sessie.getObjects(o -> o instanceof HeeftKenmerk)) {
            HeeftKenmerk h = (HeeftKenmerk) f;
            Lid lid = lidVan(h, h.kenmerk());
            kenmerken.add(new Fixpunt.AfgeleidKenmerk(h.onderwerp(), lid.naam()));
            BiConsumer<Object, Object> zetter = Accessors.setter(lid.klasse(), lid.naam());
            daden.add(() -> zetter.accept(h.onderwerp(), Boolean.TRUE));
        }
        Map<Object, Map<ElementSleutel, List<Waarde>>> perDoel = new IdentityHashMap<>();
        for (Object f : sessie.getObjects(o -> o instanceof Waarde)) {
            Waarde w = (Waarde) f;
            perDoel.computeIfAbsent(w.onderwerp(), o -> new LinkedHashMap<>())
                    .computeIfAbsent(w.attribuut(), a -> new ArrayList<>()).add(w);
        }
        List<Fixpunt.AfgeleideWaarde> waarden = new ArrayList<>();
        for (Map<ElementSleutel, List<Waarde>> perLid : perDoel.values())
            for (List<Waarde> geschreven : perLid.values()) {
                Waarde eerste = geschreven.get(0);
                if (geschreven.stream().anyMatch(w -> !gelijkeWaarde(eerste.waarde(), w.waarde()))) {
                    Schrijversconflict c = conflict(eerste.onderwerp(), eerste.attribuut(), geschreven);
                    fouten.add(new Fout(eerste.onderwerp(), c.schrijvers().get(0).schrijver(),
                            label == null ? c : c.benoemd(label)));
                    continue;
                }
                Lid lid = lidVan(eerste, eerste.attribuut());
                // a timeline goes onto the bean as the bean model's segments, which carry no value equality
                @SuppressWarnings("unchecked")
                Object v = lid.tijd() == null ? eerste.waarde()
                        : Tijdvak.beans((List<Tijdvak>) eerste.waarde(), segmentklasse());
                waarden.add(new Fixpunt.AfgeleideWaarde(eerste.onderwerp(), lid.naam(), v));
                BiConsumer<Object, Object> zetter = Accessors.setter(lid.klasse(), lid.naam());
                daden.add(() -> zetter.accept(eerste.onderwerp(), v));
            }
        List<Fixpunt.GemaaktObject> objecten = new ArrayList<>();
        for (Object f : sessie.getObjects(o -> o instanceof Gemaakt)) {
            Gemaakt g = (Gemaakt) f;
            objecten.add(new Fixpunt.GemaaktObject(g.object(), g.regel(), g.onderwerp()));
        }
        List<Fixpunt.GemaakteKoppeling> koppelingen = new ArrayList<>();
        // a link of the input carries phase 0 and is in the bean fields already
        for (Object f : sessie.getObjects(o -> o instanceof Koppeling k && k.fase() > 0))
            koppelingen.add(gemaakteKoppeling((Koppeling) f, daden));
        List<Fout> geordend = new ArrayList<>(fouten);
        geordend.sort(Comparator.comparing(Fout::regel).thenComparing(Fout::melding));
        List<Object> inconsistenties = new ArrayList<>(sessie.getObjects(o -> o.getClass().getName().equals(INCONSISTENTIE)));
        return new Lezing(new Fixpunt(waarden, kenmerken, objecten, koppelingen, geordend, inconsistenties), daden);
    }

    /** The created link {@code k} as {@link Fixpunt} gives it, with into {@code daden} the write of each of its to-one
     *  ends that the bean model gives a reference field: the field on the filler of the other role, named after the
     *  to-one role, holds the filler of that role (§13.3.9), as {@link #koppelingregels} reads the input of the relation
     *  back from it. A to-many end has no field of its own; where both ends are to-many no bean holds the link, and it
     *  is in {@link Fixpunt#koppelingen} only. */
    private Fixpunt.GemaakteKoppeling gemaakteKoppeling(Koppeling k, List<Runnable> daden) {
        RegelSpraakModel.Feittype ft = feittype(k.relatie());
        if (ft == null || ft.rollen.size() != 2)
            throw new IllegalStateException(k + " in de sessie, maar het model kent geen feittype van twee rollen '"
                    + k.relatie() + "'");
        RegelSpraakModel.Rol r0 = ft.rollen.get(0), r1 = ft.rollen.get(1);
        Objecttype t0 = model.objecttypeOpNaam(r0.objecttypeNaam()), t1 = model.objecttypeOpNaam(r1.objecttypeNaam());
        if (!r1.meervoudig() && FactModelGenerator.heeftReferentieVeld(model, t0, r1.naam(), t1)) {
            BiConsumer<Object, Object> zetter = Accessors.setter(bonenklasse(t0), r1.naam());
            daden.add(() -> zetter.accept(k.eerste(), k.tweede()));
        }
        if (!r0.meervoudig() && FactModelGenerator.heeftReferentieVeld(model, t1, r0.naam(), t0)) {
            BiConsumer<Object, Object> zetter = Accessors.setter(bonenklasse(t1), r0.naam());
            daden.add(() -> zetter.accept(k.tweede(), k.eerste()));
        }
        return new Fixpunt.GemaakteKoppeling(ft.naam, r0.naam(), k.eerste(), r1.naam(), k.tweede());
    }

    /** The bean model's {@code Segment}, the class of a timeline's segments on a bean. */
    private Class<?> segmentklasse() {
        try {
            return Class.forName(FactModelGenerator.PAKKET + ".Segment", true, beanLoader);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("geen beanklasse Segment voor een tijdlijn", e);
        }
    }

    private Lid lidVan(Object feit, ElementSleutel sleutel) {
        Lid lid = afgeleid.get(sleutel);
        if (lid == null)
            throw new IllegalStateException(feit + " in de sessie, maar geen regel van dit model leidt "
                    + Feitnamen.lid(sleutel) + " af");
        return lid;
    }

    /** The fault of RUNTIME-SEMANTICS ch. 5.7 for the {@code waarden} that writers put on attribute {@code sleutel} of
     *  {@code onderwerp}: the writers in the order of the document ({@link #documentplaats}; a writer the document has
     *  no rule of, such as a defaults group, after them by name), the attribute as declared, and the object's own
     *  type. */
    private Schrijversconflict conflict(Object onderwerp, ElementSleutel sleutel, List<Waarde> waarden) {
        return Schrijversconflict.van(onderwerp, lidVan(onderwerp, sleutel).naam(), waarden, documentplaats, typeNamen);
    }

    /** Value equality at the write boundary: numbers by value, so that 5.0 and 5.00 are one value; leeg is null. */
    private static boolean gelijkeWaarde(Object a, Object b) {
        return Rekenhulpen.gelijkeWaarde(a, b);
    }

    // ---------------- the model ----------------

    private Model run() {
        schrijvers = ProducentConsument.producentIndex(model, defaults);
        staandeDoelen = StaandeDoelen.van(model, defaults);
        for (int i = 0; i < model.resolvedRegels.size(); i++) documentplaats.putIfAbsent(model.resolvedRegels.get(i).naam(), i);
        for (Objecttype t : model.objecttypen.values()) typeNamen.put(Namen.ident(t.naam, true), t.naam);
        if (!model.objectCreaties.isEmpty() || !model.feitCreaties.isEmpty())
            strata = Kringen.strata(model, defaults, navigatieBindingen, rolBindingen, bronPaden);
        // before any form lowers: a form that reads a created relation must know it while it lowers
        for (ResolvedObjectCreatie o : model.objectCreaties) {
            RolBindingRef vr = rolBindingen.get(o);
            if (vr != null && vr.feittype() != null) gecreeerdeRelaties.add(Namen.norm(vr.feittype()));
            if (vr == null) {
                RegelSpraakModel.Feittype ft = typenpaarrelatie(o);
                if (ft != null) gecreeerdeRelaties.add(Namen.norm(ft.naam));
            }
        }
        for (ResolvedFeitCreatie f : model.feitCreaties)
            if (f.feittype() != null) gecreeerdeRelaties.add(Namen.norm(f.feittype()));
        // computing rules and initialisations are counted apart: one of each on a target is a pair the branch carries
        // (the initialisation falls through below the rule), two of either kind is not
        for (ResolvedRegel r : model.resolvedRegels) {
            if (!(r.doel() instanceof Expr.AttribuutRef d) || d.objecttype() == null || d.attribuut() == null) continue;
            Map<ElementSleutel, List<ResolvedRegel>> per =
                    r.soort() == ResolvedRegel.Soort.GELIJKSTELLING || r.soort() == ResolvedRegel.Soort.BEREKENING
                            ? rekenregels
                            : r.soort() == ResolvedRegel.Soort.INITIALISATIE ? initialisaties : null;
            if (per != null)
                per.computeIfAbsent(ProducentConsument.elementVan(d.objecttype(), d.attribuut(), false),
                        k -> new ArrayList<>()).add(r);
        }
        for (PrioritizedDefaults p : defaults)
            if (p.doel() != null)
                defaultsdoelen.add(ProducentConsument.elementVan(model.objecttypeOpNaam(p.onderwerp()), p.doel(),
                        p.isKenmerk()));
        // composed before the rules, since a rule the fold takes into a stack must not fire on the stack's type
        stapels = new Stapels(model, defaults);
        List<Stapels.Stapel> effectief;
        String samenstelling = null;
        try {
            effectief = stapels.stapels();
        } catch (Stapels.Onsamenstelbaar o) {
            effectief = List.of();
            samenstelling = o.getMessage();
        }
        List<Rule> regels = new ArrayList<>();
        for (ResolvedRegel r : model.resolvedRegels) regels.addAll(meet("regel", naamOf(r.naam()), () -> regel(r)));
        for (ResolvedConsistentie c : model.consistenties)
            regels.addAll(meet("consistentie", naamOf(c.naam()), () -> consistentieRegels(c)));
        for (ResolvedUniciteit u : model.uniciteiten)
            regels.addAll(meet("uniciteit", naamOf(u.naam()), () -> uniciteitRegels(u)));
        for (ResolvedVerdeling v : model.verdelingen)
            regels.addAll(meet("verdeling", naamOf(v.naam()), () -> verdeling(v)));
        for (ResolvedObjectCreatie o : model.objectCreaties)
            regels.addAll(meet("objectcreatie", naamOf(o.naam()), () -> objectCreatie(o)));
        for (ResolvedFeitCreatie f : model.feitCreaties)
            regels.addAll(meet("feitcreatie", naamOf(f.naam()), () -> feitCreatie(f)));
        for (ResolvedUniciteitUnie u : model.uniciteitUnies)
            regels.addAll(meet("uniciteit-unie", naamOf(u.naam()), () -> uniciteitUnieRegels(u)));
        if (samenstelling != null) weiger("defaults", "effectieve stapels", samenstelling);
        for (Stapels.Stapel st : effectief) regels.addAll(meet("defaults", stapelnaam(st), () -> stapelregels(st)));
        regels.addAll(verschilregels());
        // a StartPointDetermination is a declaration that fires nothing: it says which member anchors a grid with a
        // variable start (RUNTIME-SEMANTICS ch. 3.2), and the reference interpreter faults only where a bucket of such
        // a grid is laid. Its anchor member is an ordinary date attribute that ordinary rules write, so a document that
        // declares one and reads or writes no timeline on its grid plays; a form that does read or write one is
        // refused by name where it asks for the member (tijdlid), and the case reader refuses a case value on it
        // a day-kind definition is no form that fires: the forms that step or count in its day kind compile it where
        // they lower (dagsoortpredikaat), so a definition no form uses is never compiled and refuses nothing
        // the contributions of the folds the lowered forms read; a fold of a refused form left none here
        for (List<Rule> rs : bijdrageregels.values()) regels.addAll(rs);
        for (String relatie : gecreeerdeRelaties) regels.addAll(koppelingregels(relatie));
        // a standing is built once per member, for every rule that reads it, and only for one a lowered rule reads: a
        // standing nothing reads would only add firings
        List<Rule> standen = new ArrayList<>();
        for (Padlezing p : padlezingen.values())
            standen.addAll(p.pad().gecreeerd() ? koppelingpadstandregels(p) : padstandregels(p));
        for (Lid lid : gelezen.values()) standen.addAll(lid.kenmerk() ? kenmerkstandregels(lid) : attribuutstandregels(lid));
        for (String producent : kanalen) standen.addAll(kanaalstandregels(producent));
        for (String parameter : globals.keySet()) standen.add(parameterstandregel(parameter));
        ModelImpl m = new ModelImpl(PKG);
        if (rekendatumGlobal != null) {
            // the case runner sets the evaluation date under this name after the parameters, so a parameter of the same
            // Java name would read the date, or the date the parameter
            if (globals.containsKey(REKENDATUM))
                weiger("parameter", REKENDATUM, "een parameter met de Java-naam '" + REKENDATUM + "' naast een vorm die"
                        + " de rekendatum leest: de caserunner zet de rekendatum onder die naam, zodat de twee één global"
                        + " delen");
            else {
                m.addGlobal(rekendatumGlobal);
                standen.add(rekendatumstandregel());
            }
        }
        globals.values().forEach(m::addGlobal);
        regels.forEach(m::addRule);
        standen.forEach(m::addRule);
        for (Rule r : regels) if (!vormVanRegel.containsKey(r.getName())) regelsZonderVorm.add(r.getName());
        for (Rule r : standen) regelsZonderVorm.add(r.getName());
        return m;
    }

    /** A form's name as the manifest shows it; a nameless form (WF-107 refuses it) still gets an entry. */
    private static String naamOf(String naam) {
        return naam != null ? naam : "(zonder naam)";
    }

    /** Lowers one form and records its verdict: lowered, or a skip with the reason a {@link Weigering} carries. */
    private List<Rule> meet(String soort, String naam, Supplier<List<Rule>> bouw) {
        vorm = naam;
        int plannen = linkerkanten.size(), waarden = waardeplannen.size(), regels = regelplannen.size();
        try {
            List<Rule> rules = bouw.get();
            dekking.add(Dekking.lowered(soort, naam));
            for (Rule r : rules) vormVanRegel.put(r.getName(), naam);
            return rules;
        } catch (Weigering w) {
            // a refused form leaves no rule, so the plans it made go as well
            linkerkanten.subList(plannen, linkerkanten.size()).clear();
            waardeplannen.subList(waarden, waardeplannen.size()).clear();
            regelplannen.subList(regels, regelplannen.size()).clear();
            weiger(soort, naam, w.getMessage());
            return List.of();
        }
    }

    private void weiger(String soort, String naam, String reden) {
        Dekking d = Dekking.skip(soort, naam, reden);
        dekking.add(d);
        d.meld(model);
    }

    /** A form, or a part of one, this branch does not lower; the message is the manifest's reason. */
    private static final class Weigering extends RuntimeException {
        Weigering(String reden) { super(reden); }
    }

    // ---------------- a rule ----------------

    private List<Rule> regel(ResolvedRegel r) {
        // the rule of a form is named by the form, and a KieBase keeps one rule per name
        if (r.naam() == null) throw new Weigering("een regel zonder naam (WF-107)");
        if (!(r.doel() instanceof Expr.AttribuutRef doel) || doel.objecttype() == null || doel.attribuut() == null)
            throw new Weigering("een regel zonder opgelost doel");
        if (navigatieBindingen.containsKey(doel)) throw new Weigering("een regel op een genavigeerd doel");
        // a version with a bounded validity gates on the evaluation date: its rules bind the date where the window
        // admits it (Lezingen.geldig), and beside them a rule faults the session that carries no date
        List<Rule> uit = new ArrayList<>(switch (r.soort()) {
            case KENMERKTOEKENNING -> kenmerkRegel(r, doel);
            case GELIJKSTELLING, BEREKENING -> rekenRegel(r, doel);
            case INITIALISATIE -> initialisatieRegel(r, doel);
        });
        Rule zonderDatum = geldigheidsfout(model.geëmitteerdeRegelNaam(r.naam()), r.naam(), r.geldigheid());
        if (zonderDatum != null) uit.add(zonderDatum);
        return uit;
    }

    /**
     * The rule beside a form with a bounded validity, {@code vorm}, whose Drools rule is {@code regelnaam}, that faults
     * a session without an evaluation date: such a form needs the date (RUNTIME-SEMANTICS ch. 8.1), and the reference
     * interpreter faults the session as soon as it evaluates the form, whether or not the form has a subject, in the
     * sentence {@link Rekenfout#GELDIGHEID_ZONDER_DATUM}. The fault is on the session's {@link Rekendatum}, the one fact
     * the date is, and like every evaluation fault it counts where it stands at the fixpoint. Null for a form that is
     * always valid, which needs no date.
     */
    private Rule geldigheidsfout(String regelnaam, String vorm, Geldigheid g) {
        if (g == null || g.altijd()) return null;
        rekendatumGlobal();
        Alfa zonderDatum = new Alfa(id("geen-rekendatum"),
                List.of(new Alfa.Veldtoets("datum", Alfa.Vergelijker.LEEG, null)));
        return weergeef(new Regelplan(regelnaam + ": geen rekendatum", this.vorm, null, new Leestabel(List.of()),
                List.of(new Patroon(Rekendatum.class, "$rekendatum", List.of(zonderDatum), List.of(), List.of())),
                new Gevolg.Fout("$rekendatum", Rekenfout.GELDIGHEID_ZONDER_DATUM, new Gevolg.Foutnaam(vorm, regelnaam),
                        Gevolg.Invoeging.LOGISCH)));
    }

    /** The global of the evaluation date, declared the first time a lowered form needs the date. */
    private Global<LocalDate> rekendatumGlobal() {
        if (rekendatumGlobal == null) rekendatumGlobal = DSL.globalOf(LocalDate.class, PKG, REKENDATUM);
        return rekendatumGlobal;
    }

    /** The rule that puts the session's {@link Rekendatum} in working memory, from the global the case runner sets
     *  where the case carries an {@code asOfDate} and leaves unset, null, where it carries none. Like a parameter's
     *  standing ({@link #parameterstandregel}) it matches no fact and fires once per session; the global does not
     *  change while the session fires. */
    private Rule rekendatumstandregel() {
        return weergeef(new Regelplan("rekendatumstand", null, null, new Leestabel(List.of()), List.of(),
                new Gevolg.Rekendatumstand(REKENDATUM, Gevolg.Invoeging.LOGISCH)));
    }

    /**
     * A characteristic attribution: the subject pattern with the reads its condition binds there, the standing patterns
     * of what it reads elsewhere, one test over every value its condition reads, and a consequence that inserts the
     * {@link HeeftKenmerk} logically. Its justification is the whole match, so a standing the condition read that is
     * later retracted takes the characteristic with it, also when the condition read it negated. Beside it, where the
     * condition can fault, the rule that inserts its fault ({@link #voorwaardefoutregel}).
     */
    private List<Rule> kenmerkRegel(ResolvedRegel r, Expr.AttribuutRef doel) {
        // the loader refuses any other value (WF-022); asked again because this rule writes true whatever it says
        if (!(r.waarde() instanceof Expr.Literal l && "waar".equals(l.waarde()) && l.type() instanceof Datatype.Booleaan))
            throw new Weigering("een kenmerktoekenning met een andere waarde dan de constante waar (WF-022)");
        Lid tijd = gedeclareerd(doel.objecttype(), doel.attribuut(), true);
        if (tijd.tijd() != null)
            throw new Weigering("het tijdsafhankelijke kenmerk '" + tijd.naam() + "' van " + tijd.declarant().naam
                    + " als doel: " + NOG_NIET + "; geen golden met cases schrijft er een (RUNTIME-SEMANTICS ch. 3.11)");
        Lid lid = lid(doel.objecttype(), doel.attribuut(), true);
        Supplier<Lezingen> nieuw = () -> new Lezingen(doel.objecttype(), false).geldig(r.geldigheid());
        Lezingen lez = nieuw.get();
        Set<Integer> gelezen = lez.volg();
        Term toets = r.voorwaarde() == null ? null : voorwaarde(r.voorwaarde(), lez);
        boolean kanFalen = lez.kanFalen();
        lez.stop();
        List<LhsItem> items = lez.plan(voorwaarderol(kanFalen), toets, gelezen, r.voorwaarde());
        ElementSleutel sleutel = lid.sleutel();
        afgeleid.put(sleutel, lid);
        lez.vastleggen();
        String regelnaam = model.geëmitteerdeRegelNaam(r.naam());
        List<Rule> uit = new ArrayList<>();
        uit.add(weergeef(lez.regelplan(regelnaam, items,
                new Gevolg.Kenmerk(ONDERWERP, sleutel, Gevolg.Invoeging.LOGISCH))));
        if (kanFalen) uit.add(voorwaardefoutregel(r.naam(), regelnaam, nieuw, r.voorwaarde(), null));
        Rule deponering = deponeerregel(r.naam(), regelnaam, nieuw, r.voorwaarde(), null);
        if (deponering != null) uit.add(deponering);
        return uit;
    }

    /**
     * An assignment or a computation of an attribute of the subject: the left-hand side of a characteristic attribution,
     * and a consequence that computes the value from the values it binds and inserts a {@link Waarde} logically. A
     * computed leeg writes nothing on a standing target, so the input on the target stands, or the initialisation beside
     * the rule fills it (the standing norm of RUNTIME-SEMANTICS ch. 7.8 and the desugaring domain of ch. 7.7: the rule is
     * its target's only computing writer, beside at most one unconditional initialisation). Outside that domain a
     * computed leeg is a write (ch. 1.2g): the {@link Waarde} carries leeg, which takes the target's input standing away
     * and is a value another writer can disagree with. A target may have more computing rules: each is an equation, and
     * where two of them stand at the fixpoint on one object with different values the rule of {@link #verschilregel}
     * makes that the fault of ch. 5.7; where at most one condition holds, or they write the same value, the document
     * plays. They are keyed per object, so writers on sister subtypes never meet (WF-079 and ch. 7.10 read per concrete
     * type). An evaluation fault, a division by a numeric zero or a {@link Rekenfout}, is inserted logically by the
     * consequence as a {@link Fout} in place of the value, named after the rule. A division of a filled dividend by a
     * leeg divisor, and a square root of a negative number, insert the §9.5 inconsistency of RUNTIME-SEMANTICS ch. 1.2
     * from the same consequence, under the rule's name, where the evaluation reaches them: the probe the consequence puts
     * in its value array hears exactly the places that run ({@link Sonde}), so a place in a branch of an
     * {@code IfThenElse} that is not taken, or in the value of a {@code Let} binder no read forces, deposits nothing
     * (ch. 10.2, 10.6). Every division and every root is such a place, as in the reference interpreter. The condition of
     * the rule holds there by construction; a deposit in the condition itself is the {@link #deponeerregel}'s.
     *
     * <p>That deposit is a logical fact like the value: it stands while the activation that made it stands, and goes when
     * a read it rested on changes. The reference interpreter keeps a deposit made on a transient value (RUNTIME-SEMANTICS
     * ch. 6.1, "Recorded residuals"), this branch withdraws it; recorded as an observation.
     *
     * <p>A member an object creation sets is the input of the object it makes ({@link #objectCreatie}), so a rule on it
     * displaces that value as it displaces case input, and outside the standing domain a computed leeg erases it, as in
     * the reference interpreter, which overwrites the value the creation wrote at the making (ch. 5.5, 1.2g).
     */
    private List<Rule> rekenRegel(ResolvedRegel r, Expr.AttribuutRef doel) {
        if (gedeclareerd(doel.objecttype(), doel.attribuut(), false).tijd() != null) return tijdlijnregel(r, doel);
        Lid lid = lid(doel.objecttype(), doel.attribuut(), false);
        Set<Class<?>> geabsorbeerd = geabsorbeerd(doel, lid);
        boolean schrijftLeeg = !staandeDoelen.staandScalair(r);
        return schrijfregel(r, doel.objecttype(), lid, null, geabsorbeerd, schrijftLeeg);
    }

    /**
     * A computation of an attribute on a timeline of the subject (RUNTIME-SEMANTICS ch. 3.11): the rule writes a whole
     * timeline at once, which a pro-rata part (ch. 3.7) or a day count (ch. 3.8) over the subject's own characteristic
     * produces ({@link #tijdlijnwaarde}), and inserts it as the {@link Waarde} of the target, an immutable
     * {@code List<Tijdvak>}. On a standing temporal target ({@link StaandeDoelen#staandTemporeel}: the target's one
     * version-free writer, ch. 7.8) an empty result is the temporal leeg and writes nothing, so the input timeline of the
     * target stands whole, and a result with a segment replaces it whole; elsewhere the rule writes the empty timeline
     * as well (ch. 1.2g). The versions of one rule are one writer; a version that applies writes as such a rule does,
     * and two that stand with different timelines are the fault of ch. 5.7 ({@link #verschilregel}).
     *
     * <p>Refused by name, each with no golden that has cases: any other writer of the target, a rule, an
     * initialisation, a defaults group or an allocation, since ch. 7.8 keeps a target with more writers fenced and
     * the reference interpreter answers it in pass order; and any other value, among them a sum of timelines over a
     * set (ch. 3.5) and the arithmetic of two timelines (ch. 3.4), which no golden of this branch's gap asks. Refused
     * by name as well: a time window in the condition, which on a temporal target clips the timeline instead of
     * gating the rule on the evaluation date (ch. 3.10), as the DRL-text branch refuses it; golden
     * {@code jutterswet-pachtvenster} has it.
     */
    private List<Rule> tijdlijnregel(ResolvedRegel r, Expr.AttribuutRef doel) {
        Lid lid = tijdlid(doel.objecttype(), doel.attribuut(), false, "een tijdsafhankelijk doel");
        List<String> anderen = andereSchrijvers(r, doel.objecttype(), lid.sleutel());
        if (!anderen.isEmpty())
            throw new Weigering("een tijdsafhankelijk doel met een tweede schrijver ('" + lid.naam() + "' van "
                    + lid.declarant().naam + " wordt ook geschreven door "
                    + String.join(", ", anderen.stream().map(x -> "\"" + x + "\"").toList()) + "): " + NOG_NIET
                    + "; een doel met meer schrijvers blijft omheind (RUNTIME-SEMANTICS ch. 7.8), en geen golden met"
                    + " cases heeft er een");
        if (Conditie.bevatPeriode(r.voorwaarde()))
            throw new Weigering("een periodevenster in de voorwaarde van een regel met het tijdsafhankelijke doel '"
                    + lid.naam() + "' van " + lid.declarant().naam + ": daar knipt het venster de tijdlijn"
                    + " (RUNTIME-SEMANTICS ch. 3.10), en " + NOG_NIET + ", zoals de DRL-teksttak haar niet draagt");
        if (!(r.waarde() instanceof Expr.Functie f && (f.kind() == Expr.FunctieKind.TIJDSEVENREDIG_DEEL
                || f.kind() == Expr.FunctieKind.AANTAL_DAGEN_IN)))
            throw new Weigering("een waarde van het tijdsafhankelijke doel '" + lid.naam() + "' van "
                    + lid.declarant().naam + " die geen tijdsevenredig deel en geen dagentelling is ('"
                    + (r.waarde() == null ? "zonder waarde" : snippet(r.waarde().bron())) + "'): " + NOG_NIET
                    + " (RUNTIME-SEMANTICS ch. 3.4, 3.5, 3.11); geen golden met cases schrijft er een");
        return schrijfregel(r, doel.objecttype(), lid, null, Set.of(), !staandeDoelen.staandTemporeel(doel));
    }

    /**
     * The timeline that the pro-rata part or the day count {@code f} produces for the target {@code doel} of
     * {@code subject} (RUNTIME-SEMANTICS ch. 3.7, 3.8), as a function of the values the rule binds: the true periods of
     * the subject's own characteristic in its gedurende clause and, for the pro-rata part, the timeline of the base, an
     * own attribute, each bound in the left-hand side ({@link Lezingen#tijdlijn}), so a base that a rule derives later
     * takes the result along. The runtime's helpers compute them, with {@link Tijdvak} as their
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
     * point ({@link Tijdlijnen#proRataVastRaster}).
     */
    private Term tijdlijnwaarde(Expr.Functie f, Lid doel, Objecttype subject, Lezingen lez) {
        boolean deel = f.kind() == Expr.FunctieKind.TIJDSEVENREDIG_DEEL;
        String wat = (deel ? "een tijdsevenredig deel" : "een dagentelling") + " ('" + snippet(f.bron()) + "')";
        if (f.granulariteit() != doel.tijd())
            throw new Weigering(wat + " per " + f.granulariteit() + " naar het doel '" + doel.naam() + "' met de"
                    + " granulariteit " + doel.tijd() + ": de emmer is die van het doel (" + (deel ? "WF-056" : "WF-058")
                    + ")");
        if (doel.tijd() != RegelSpraakModel.Granulariteit.MAAND && doel.tijd() != RegelSpraakModel.Granulariteit.JAAR)
            throw new Weigering(wat + " per " + doel.tijd() + ": alleen een maand en een jaar zijn emmers"
                    + " (RUNTIME-SEMANTICS ch. 3.2)");
        if (f.leegwaarde() != null || f.dimensieSelectie() != null)
            throw new Weigering(wat + " met een terugvalwaarde of een dimensieselectie: " + NOG_NIET);
        int conditie = kniptijdlijn(f.filter(), subject, lez, wat);
        String gran = doel.tijd().name();
        if (!deel) {
            if (!f.argumenten().isEmpty())
                throw new Weigering(wat + " met " + f.argumenten().size() + " argumenten: een dagentelling neemt er"
                        + " geen (WF-058)");
            // RUNTIME-SEMANTICS ch. 3.2: a target on a fixed start point buckets from the constant anchor its walls are
            // computed from
            LocalDate anker = model.vastStartpuntAnker(doel.declarant(), doel.naam());
            Term perioden = new Term.Lees(conditie, bron(f.filter().bron()));
            Term dagen = anker == null
                    ? new Term.Aanroep(Functie.RS_AANTAL_DAGEN_IN, List.of(perioden, constante(gran),
                            constante(Tijdvak.class)), null)
                    : new Term.Aanroep(Functie.RS_AANTAL_DAGEN_IN_VANAF, List.of(perioden, constante(gran),
                            constante(anker), constante(Tijdvak.class)), null);
            return new Term.Aanroep(Functie.TIJDVAK_LIJST, List.of(dagen), bron(f.bron()));
        }
        if (f.argumenten().size() != 1 || !(f.argumenten().get(0) instanceof Expr.AttribuutRef b)
                || b.attribuut() == null || b.objecttype() != subject || navigatieBindingen.containsKey(b))
            throw new Weigering(wat + " over een basis die geen eigen tijdsafhankelijk attribuut is: " + NOG_NIET
                    + "; de referentie-interpreter neemt alleen een eigen attribuut als basis (RUNTIME-SEMANTICS ch. 3.7)");
        Lid basis = tijdlid(subject, b.attribuut(), false, wat);
        if (!basis.datatype().isNumeriek())
            throw new Weigering(wat + " over de basis '" + basis.naam() + "', die geen getal is (WF-091)");
        if (basis.datatype().eenheidOf() != null)
            throw new Weigering(wat + " over de basis '" + basis.naam() + "' met de eenheid "
                    + basis.datatype().eenheidOf().weergave() + ": de herrekening van een tarief is omheind in de"
                    + " referentie-interpreter en deze tak (RUNTIME-SEMANTICS ch. 3.7); geen golden");
        if (basis.tijd().ordinal() < doel.tijd().ordinal())
            throw new Weigering(wat + " over de basis '" + basis.naam() + "' met de granulariteit " + basis.tijd()
                    + ", fijner dan de emmer " + doel.tijd() + ": de basis staat niet vast over een emmer (RUNTIME-SEMANTICS"
                    + " ch. 3.7)");
        String vast = Tijdlijnen.proRataVastRaster(subject, doel.naam(), f);
        if (vast != null) throw new Weigering(wat + ": " + vast);
        int basiswaarde = lez.tijdlijn(basis);
        int onderwerp = lez.lid();
        lez.markeerFalen();
        String basisnaam = basis.naam();
        Term evenredig = new Term.Aanroep(Functie.RS_TIJDSEVENREDIG, List.of(
                new Term.Lees(conditie, bron(f.filter().bron())),
                volledig(basiswaarde, "the pro-rata base \"" + basisnaam + "\" needs the full timeline; ", onderwerp,
                        " supplies only the asOfDate-bucket scalar (ch. 3.7, ch. 8.4)", bron(b.bron())),
                constante(gran), constante(Tijdvak.class)), null);
        return new Term.Aanroep(Functie.TIJDVAK_LIJST, List.of(evenredig), bron(f.bron()));
    }

    /**
     * The true periods of the gedurende clause {@code c} of a pro-rata part or a day count on {@code subject}, bound as a
     * timeline ({@link Lezingen#tijdlijn}): a check of the subject's own characteristic on a timeline, as the reference
     * interpreter reads that clause (RUNTIME-SEMANTICS ch. 3.7, 3.8). Refused by name: no clause, another condition, a
     * negated or navigated check, and an owner or a whole-period qualifier on the check (WF-097).
     */
    private int kniptijdlijn(Conditie c, Objecttype subject, Lezingen lez, String wat) {
        if (!(c instanceof Conditie.Kenmerk k))
            throw new Weigering(wat + " met een voorwaarde die geen kenmerk van het onderwerp is"
                    + (c == null ? " (geen voorwaarde)" : " ('" + snippet(c) + "')") + ": " + NOG_NIET
                    + "; de referentie-interpreter neemt alleen een eigen tijdsafhankelijk kenmerk (RUNTIME-SEMANTICS"
                    + " ch. 3.7, 3.8)");
        if (k.eigenaar() != null || k.periode() != null)
            throw new Weigering(wat + " met een kenmerk met een eigenaar of een geheleperiodetoets in de knip ('"
                    + snippet(k) + "', WF-097)");
        if (k.negatie() || k.navigatie() != null || navigatieBindingen.containsKey(k))
            throw new Weigering(wat + " met een ontkend of genavigeerd kenmerk ('" + snippet(k) + "'): " + NOG_NIET
                    + "; de referentie-interpreter neemt alleen een eigen tijdsafhankelijk kenmerk, niet ontkend"
                    + " (RUNTIME-SEMANTICS ch. 3.7, 3.8)");
        return lez.tijdlijn(tijdlid(subject, k.kenmerk(), true, wat));
    }

    /**
     * The forms other than the initialisation {@code r} on {@code type} that write the attribute {@code sleutel} on an
     * instance {@code r} writes too, by the names the producer index gives them, in its order: a rule, an allocation or
     * a defaults group on {@code type}, an ancestor or a descendant of it ({@link #verwant}), read per concrete type as
     * WF-079 reads the single-writer rule; forms on sister subtypes never write one instance. An object creation is no
     * writer here: the member values it sets are the input of the object it makes ({@link #objectCreatie}), which the
     * initialisation finds filled there.
     */
    private List<String> andereSchrijvers(ResolvedRegel r, Objecttype type, ElementSleutel sleutel) {
        Set<String> uit = new LinkedHashSet<>();
        for (ResolvedRegel x : model.resolvedRegels)
            if (x != r && x.soort() != ResolvedRegel.Soort.KENMERKTOEKENNING && x.doel() instanceof Expr.AttribuutRef d
                    && d.objecttype() != null && d.attribuut() != null && verwant(d.objecttype(), type)
                    && ProducentConsument.elementVan(d.objecttype(), d.attribuut(), false).equals(sleutel))
                uit.add(ProducentConsument.basisNaam(x.naam()));
        for (ResolvedVerdeling v : model.verdelingen)
            if (v.doelType() != null && v.doelAttribuut() != null && verwant(v.doelType(), type)
                    && ProducentConsument.elementVan(v.doelType(), v.doelAttribuut(), false).equals(sleutel))
                uit.add(ProducentConsument.basisNaam(v.naam()));
        for (int i = 0; i < defaults.size(); i++) {
            PrioritizedDefaults p = defaults.get(i);
            Objecttype t = model.objecttypeOpNaam(p.onderwerp());
            if (t != null && p.doel() != null && !p.isKenmerk() && verwant(t, type)
                    && ProducentConsument.elementVan(t, p.doel(), false).equals(sleutel))
                uit.add(p.naam() != null ? ProducentConsument.basisNaam(p.naam()) : "/rules/prioritizedDefaults/" + i);
        }
        uit.remove(ProducentConsument.basisNaam(r.naam()));
        return List.copyOf(uit);
    }

    /** Whether one of the two types is the other or an ancestor of it: a form on the one then writes instances of the
     *  other, where two forms on sister types never meet on one instance (IR-SPEC ch. 2.2, WF-079). */
    private static boolean verwant(Objecttype a, Objecttype b) {
        return a.keten().contains(b) || b.keten().contains(a);
    }

    /**
     * The bean classes of the concrete types on which the fold of RUNTIME-SEMANTICS ch. 7.10 took the rule writing
     * {@code doel} into an effective stack, as its base or as its initialisation default: there the stack writes the
     * target and the rule must not also fire, so its subject pattern leaves them out. Empty for a target no defaults
     * group writes. A rule beside a group its own type declares on the same target is no fold but two writers of one
     * target (WF-050), which this branch refuses by name: the fold leaves such a rule alone, and both would write.
     */
    private Set<Class<?>> geabsorbeerd(Expr.AttribuutRef doel, Lid lid) {
        if (!defaultsdoelen.contains(lid.sleutel())) return Set.of();
        if (stapels.eigenGroep(doel.objecttype(), doel.attribuut()))
            throw new Weigering("een regel naast een defaultsgroep van hetzelfde type op hetzelfde doel ('" + lid.naam()
                    + "' van " + doel.objecttype().naam + "): een doel heeft één schrijver, de groep of de regel (WF-050)");
        Set<Class<?>> uit = new LinkedHashSet<>();
        for (Objecttype t : stapels.geabsorbeerdOp(doel.objecttype(), doel.attribuut())) uit.add(bonenklasse(t));
        return uit;
    }

    /**
     * An initialisation (RUNTIME-SEMANTICS ch. 5.4): the left-hand side and the consequence of a computing rule, under a
     * guard that reads the input and the other writers' values, never its own write. The target must be empty on the
     * bean, which holds the case input and is not written while the session fires, and no {@link Waarde} of another
     * writer may stand on it, so a computed value displaces the initialisation whichever fires first, and a
     * computation that is leeg writes nothing and lets the initialisation fill the target (ch. 7.3). Its own
     * {@link Waarde} carries its name as the writer and does not match the guard: the value is an equation like any
     * attribute write, withdrawn and computed again when a value it reads changes. A guard that counted the own write
     * blocked the initialisation the moment it wrote and truth maintenance withdrew the value: on one object the target
     * ended leeg without a message (A5 of the trial of card drools-logische-afleiding, and the first case of
     * {@code jutterswet-initialisatievolgorde} on this branch), on several the session reached the fire cap (measured
     * 2026-10-08, {@code TmsInitialisatieWitnessTest}).
     *
     * <p>A computing rule beside it that is outside the standing domain writes a computed leeg (ch. 1.2g). The guard
     * then reads the target as the other writers leave it, as the reference interpreter's guard reads the running fact
     * base: a value another writer places switches it off, and a leeg another writer writes leaves the target empty,
     * input or not, so the initialisation fills it and the two writers demand different values, the fault of ch. 5.7
     * ({@link #verschilregel}).
     *
     * <p>Two initialisations on one target are ill-formed, conditional or not (IR-SPEC WF-079): where both apply,
     * nothing ranks them. The backend runs no verifier, so this refuses the pair by name, read per concrete type as
     * WF-079 reads it: two initialisations on one type, or on a type and its ancestor, write one instance, and two on
     * sister subtypes never do (ch. 7.10).
     *
     * <p>Beside another writer of its target on an instance it writes ({@link #andereSchrijvers}: per concrete type, so
     * not a writer on a sister subtype) an initialisation is the lowest default (ch. 7.7), and only the default
     * that decides is evaluated (ch. 7.1). This rule is evaluated as soon as what it reads stands, also where the other
     * writer displaces it later. A division by zero there is a fault on an intermediate state: this branch inserts it as
     * a {@link Fout}, which goes when the other writer displaces the initialisation, and answers the other writer's
     * value, where the reference interpreter faults or answers the value depending on the order of the rules. Whether
     * such a fault counts waits on a decision of the norm, so a division in the condition or the value of an
     * initialisation beside another writer stays refused, with the predicate of a division in a default
     * ({@code TmsInitialisatieWitnessTest}). Without another writer nothing displaces it and the division stays carried.
     */
    private List<Rule> initialisatieRegel(ResolvedRegel r, Expr.AttribuutRef doel) {
        Lid tijd = gedeclareerd(doel.objecttype(), doel.attribuut(), false);
        if (tijd.tijd() != null)
            throw new Weigering("een initialisatie van het tijdsafhankelijke attribuut '" + tijd.naam() + "' van "
                    + tijd.declarant().naam + ": " + NOG_NIET + "; geen golden met cases heeft er een");
        Lid lid = lid(doel.objecttype(), doel.attribuut(), false);
        ElementSleutel sleutel = lid.sleutel();
        Set<Class<?>> geabsorbeerd = geabsorbeerd(doel, lid);
        List<ResolvedRegel> schrijvend = initialisaties.get(sleutel).stream()
                .filter(x -> verwant(((Expr.AttribuutRef) x.doel()).objecttype(), doel.objecttype())).toList();
        if (schrijvend.size() > 1)
            throw new Weigering("twee of meer initialisaties op één doel ('" + lid.naam() + "' van " + lid.declarant().naam
                    + ": " + String.join(", ", schrijvend.stream().map(x -> "\"" + x.naam() + "\"").toList())
                    + "), voorwaardelijk of niet: waar ze allebei gelden rangschikt niets ze en bepaalt het fixpunt de"
                    + " waarde niet (RUNTIME-SEMANTICS ch. 5.4, WF-079)");
        // a computing rule outside the standing domain writes a computed leeg, which empties the target for the guard
        boolean leegNaast = rekenregels.getOrDefault(sleutel, List.of()).stream().anyMatch(x ->
                !staandeDoelen.staandScalair(x) && verwant(((Expr.AttribuutRef) x.doel()).objecttype(), doel.objecttype()));
        List<String> anderen = andereSchrijvers(r, doel.objecttype(), sleutel);
        String deling = deling(r.voorwaarde(), r.waarde());
        if (deling != null && !anderen.isEmpty())
            throw new Weigering("een deling in een initialisatie waarvan het doel nog een schrijver heeft ('" + deling
                    + "'; '" + lid.naam() + "' van " + lid.declarant().naam + " wordt ook geschreven door "
                    + String.join(", ", anderen.stream().map(x -> "\"" + x + "\"").toList()) + "): naast een andere"
                    + " schrijver is een initialisatie de laagste default (RUNTIME-SEMANTICS ch. 5.4, 7.7), en deze tak"
                    + " rekent haar uit zodra wat zij leest staat, ook waar de andere schrijver haar later verdringt; een"
                    + " deling door nul daar is een fout op een tussenstand, die deze tak intrekt zodra de andere"
                    + " schrijver beslist, waar de referentie afhankelijk van de volgorde van de regels een fout of de"
                    + " waarde geeft: of zo'n fout telt, wacht op een normbesluit (ch. 5.2, 7.1)");
        String naam = r.naam();
        Alfa.Veldtoets vanDoel = new Alfa.Veldtoets("attribuut", Alfa.Vergelijker.GELIJK, sleutel);
        Alfa.Veldtoets vanAnder = new Alfa.Veldtoets("schrijver", Alfa.Vergelijker.NIET_GELIJK, naam);
        if (!leegNaast)
            return schrijfregel(r, doel.objecttype(), lid, lez -> {
                lez.onderwerpAlfa(new Alfa(id("doel-leeg-in-de-invoer"),
                        List.of(new Alfa.Veldtoets(lid.naam(), Alfa.Vergelijker.LEEG, null))));
                Alfa ander = new Alfa(id("andere-schrijver"), List.of(vanDoel, vanAnder));
                return List.of(new Niet(new Patroon(Waarde.class, "$andere_schrijver", List.of(ander),
                        List.of(new Join(id("andere-schrijver-onderwerp"), "onderwerp", ONDERWERP)), List.of())));
            }, geabsorbeerd, false);
        return schrijfregel(r, doel.objecttype(), lid, lez -> {
            Alfa ander = new Alfa(id("andere-schrijver"),
                    List.of(vanDoel, vanAnder, new Alfa.Veldtoets("waarde", Alfa.Vergelijker.GEVULD, null)));
            Join anderOpOnderwerp = new Join(id("andere-schrijver-onderwerp"), "onderwerp", ONDERWERP);
            Alfa leeg = new Alfa(id("lege-schrijver"),
                    List.of(vanDoel, vanAnder, new Alfa.Veldtoets("waarde", Alfa.Vergelijker.LEEG, null)));
            Join leegOpOnderwerp = new Join(id("lege-schrijver-onderwerp"), "onderwerp", ONDERWERP);
            int legen = lez.legeSchrijvers();
            int onderwerp = lez.lid();
            // the target's input on the subject's bean, as it is, which the guard tests for leeg as the first variant's
            // constraint does; read in the test, so the executable model binds no value of it
            Term invoer = new Term.Invoer(new Term.Lees(onderwerp, null), new Binding.Veld(lid.klasse(), lid.naam()),
                    Binding.Leesvorm.WAARDE, null);
            Term doelLeeg = new Term.Aanroep(Functie.DOEL_LEEG, List.of(new Term.Lees(legen, null), invoer), null);
            return List.of(
                    new Niet(new Patroon(Waarde.class, "$andere_schrijver", List.of(ander), List.of(anderOpOnderwerp),
                            List.of())),
                    new Accumulatie(new Patroon(Waarde.class, "$lege_schrijver", List.of(leeg), List.of(leegOpOnderwerp),
                            List.of()), List.of(new Accumulatie.Functie(Accumulatie.Soort.AANTAL, null, "$lege_schrijvers"))),
                    new Toetsitem(id("doel-leeg"), Toetsitem.Rol.VORM, doelLeeg, -1, List.of(legen, onderwerp),
                            lez.grootte()));
        }, geabsorbeerd, false);
    }

    /**
     * The rule of a form that writes an attribute of its subject: the subject pattern with the reads its condition and
     * its value bind there, the standing patterns of what it reads elsewhere, one test over every value the condition
     * reads, the {@code blokkade} the form adds (null for none), and a consequence that computes the value from the
     * values it binds and inserts a {@link Waarde} logically, with the form's name as the writer, when the value is
     * gevuld, or also when it is leeg where {@code schrijftLeeg} (a computing rule outside the standing domain, ch.
     * 1.2g), beside the §6.5 deposit {@link #rekenRegel} describes. Where the value faults, the consequence inserts the
     * {@link Fout} instead, and neither the value nor a deposit. Objects of a class in {@code geabsorbeerd} do not
     * match: an effective stack writes the target there ({@link #geabsorbeerd}). Beside it, where the condition can
     * fault, the rule that inserts its fault ({@link #voorwaardefoutregel}).
     */
    private List<Rule> schrijfregel(ResolvedRegel r, Objecttype subject, Lid lid,
                                    Function<Lezingen, List<LhsItem>> blokkade, Set<Class<?>> geabsorbeerd,
                                    boolean schrijftLeeg) {
        ElementSleutel sleutel = lid.sleutel();
        Supplier<Lezingen> nieuw = () -> lezingen(subject, geabsorbeerd).geldig(r.geldigheid());
        Lezingen lez = nieuw.get();
        Set<Integer> gelezen = lez.volg();
        Term toets = r.voorwaarde() == null ? null : voorwaarde(r.voorwaarde(), lez);
        boolean kanFalen = lez.kanFalen();
        lez.stop();
        Set<Integer> gebruik = lez.volg();
        // the write boundary: the value restated into the target's declared unit (RUNTIME-SEMANTICS ch. 2.4, 2.6); a
        // timeline is written as its producer computes it, as in the reference interpreter (ch. 3.7)
        Term waarde = waardeplan(lid.tijd() != null
                ? tijdlijnwaarde((Expr.Functie) r.waarde(), lid, subject, lez)
                : herschaald(waarde(r.waarde(), lez), eff.eenheidVan(r.waarde()), lid.datatype().eenheidOf(),
                        "een §4 schrijfgrens naar het doel"));
        lez.stop();
        int sonde = lez.sondeplaats();
        List<LhsItem> items = lez.plan(voorwaarderol(kanFalen), toets, gelezen, r.voorwaarde());
        if (blokkade != null) items.addAll(blokkade.apply(lez));
        String naam = r.naam();
        String regelnaam = model.geëmitteerdeRegelNaam(naam);
        Gevolg gevolg = new Gevolg.Waarde(ONDERWERP, sleutel, naam, waarde, List.copyOf(gebruik), lez.grootte(), true,
                schrijftLeeg, sonde < 0 ? null : new Gevolg.Deponering(sonde, naam), new Gevolg.Foutnaam(naam, regelnaam),
                Gevolg.Invoeging.LOGISCH);
        afgeleid.put(sleutel, lid);
        lez.vastleggen();
        List<Rule> uit = new ArrayList<>();
        uit.add(weergeef(lez.regelplan(regelnaam, items, gevolg)));
        if (kanFalen) uit.add(voorwaardefoutregel(naam, regelnaam, nieuw, r.voorwaarde(), blokkade));
        Rule deponering = deponeerregel(naam, regelnaam, nieuw, r.voorwaarde(), blokkade);
        if (deponering != null) uit.add(deponering);
        return uit;
    }

    /** The reads of a rule on {@code subject}, whose subject pattern leaves out the objects of a class in
     *  {@code geabsorbeerd}: an effective stack writes the target there ({@link #geabsorbeerd}). */
    private Lezingen lezingen(Objecttype subject, Set<Class<?>> geabsorbeerd) {
        Lezingen lez = new Lezingen(subject, false);
        if (!geabsorbeerd.isEmpty())
            lez.onderwerpAlfa(new Alfa(id("niet-in-een-stapel"),
                    List.of(new Alfa.Veldtoets(null, Alfa.Vergelijker.KLASSE_NIET_IN, Set.copyOf(geabsorbeerd)))));
        return lez;
    }

    // ---------------- two writers of one target ----------------

    /**
     * The rules of {@link #verschilregel}, one per attribute on which two writers can stand at the fixpoint on one
     * object: two or more computing rules, or a computing rule outside the standing domain beside an initialisation,
     * whose guard a written leeg leaves open (ch. 5.4). An initialisation beside a standing rule needs none: its guard
     * keeps it from standing beside the rule's value, and the rule writes nothing for a computed leeg.
     */
    private List<Rule> verschilregels() {
        List<Rule> uit = new ArrayList<>();
        for (Map.Entry<ElementSleutel, List<ResolvedRegel>> e : rekenregels.entrySet()) {
            Lid lid = afgeleid.get(e.getKey());
            // no writer of the attribute was lowered: the manifest names the refused forms, and the build fails on them
            if (lid == null) continue;
            boolean leegNaastInitialisatie = e.getValue().stream().anyMatch(r -> !staandeDoelen.staandScalair(r))
                    && !initialisaties.getOrDefault(e.getKey(), List.of()).isEmpty();
            if (e.getValue().size() > 1 || leegNaastInitialisatie) uit.add(verschilregel(lid));
        }
        return uit;
    }

    /**
     * The fault of RUNTIME-SEMANTICS ch. 5.7 as a logical fact: per object of the declaring type of {@code lid}, an
     * accumulate over the {@link Waarde} facts on that object's attribute, and where they hold different values a
     * {@link Fout} with the {@link Schrijversconflict} that names the writers in the order of the document, under the
     * first of them. The fault rests on the values that stand, so a difference that exists on an intermediate state only
     * goes again when a writer's condition or value changes, and the session reports the differences that stand at its
     * fixpoint, as it reports every evaluation fault ({@link #zetAfgeleideWaarden}).
     */
    private Rule verschilregel(Lid lid) {
        ElementSleutel sleutel = lid.sleutel();
        Alfa vanDoel = gelijk(id("waarde"), "attribuut", sleutel);
        Join opOnderwerp = new Join(id("waarde-onderwerp"), "onderwerp", ONDERWERP);
        Toetsitem verschil = new Toetsitem(id("verschil"), Toetsitem.Rol.VORM,
                new Term.Aanroep(Functie.VERSCHILLEN, List.of(new Term.Lees(0, null)), null), -1, List.of(0), 1);
        return weergeef(new Regelplan("schrijvers " + lid.declarant().naam + "." + lid.naam() + ": verschil", null, null,
                new Leestabel(List.of(new dev.uchar.regelspraak.transpiler.tms.plan.Lezing(0, Leessoort.WAARDEN,
                        "waarden", "$waarden"))),
                List.of(new Onderwerp(lid.klasse(), ONDERWERP, List.of(), List.of(), List.of()),
                        new Accumulatie(new Patroon(Waarde.class, "$waarde", List.of(vanDoel), List.of(opOnderwerp),
                                List.of()), List.of(new Accumulatie.Functie(Accumulatie.Soort.LIJST, null, "$waarden"))),
                        verschil),
                new Gevolg.Schrijversconflict(ONDERWERP, 0, sleutel, lid.naam(), documentplaats, typeNamen,
                        Gevolg.Invoeging.LOGISCH)));
    }

    // ---------------- an evaluation fault in a condition ----------------

    /**
     * The rule that inserts the fault of a condition that can fault, beside the rule {@code regelnaam} of the form
     * {@code vorm} that tests the condition. A condition is tested in the left-hand side, and an exception there leaves
     * the fire, where truth maintenance cannot withdraw it when the reads it rested on change; the form's own rule
     * therefore tests the condition with the role {@link Toetsitem.Rol#ZONDER_FOUT}, which does not hold where it
     * faults, and this rule, whose left-hand side is that rule's own built again over fresh declarations ({@code nieuw}
     * sets up the subject pattern and what precedes it), with the same {@code blokkade} after the test and the test of
     * the role {@link Toetsitem.Rol#FOUT}, that the condition faults, inserts the named {@link Fout} logically, on the
     * subject of the form. It fires exactly where the form's rule tests the condition and the test faults, and the
     * fault goes when a read it rested on changes. The sentence
     * names the form's rule. A condition faults also where it
     * reaches a fold whose filter faulted for a member ({@link #vouw}); the filter itself inserts no fault
     * ({@link #filterfoutregel}).
     */
    private Rule voorwaardefoutregel(String vorm, String regelnaam, Supplier<Lezingen> nieuw, Conditie c,
                                     Function<Lezingen, List<LhsItem>> blokkade) {
        Lezingen lez = nieuw.get();
        Set<Integer> gelezen = lez.volg();
        Term toets = voorwaarde(c, lez);
        lez.stop();
        List<LhsItem> items = lez.plan(Toetsitem.Rol.FOUT, toets, gelezen, c);
        if (blokkade != null) items.addAll(blokkade.apply(lez));
        Gevolg fout = new Gevolg.Toetsfout(ONDERWERP, toets, Toetsitem.Rol.FOUT, List.copyOf(gelezen), lez.grootte(),
                new Gevolg.Foutnaam(vorm, model.geëmitteerdeRegelNaam(vorm)), Gevolg.Invoeging.LOGISCH);
        lez.vastleggen();
        return weergeef(lez.regelplan(regelnaam + ": fout in de voorwaarde", items, fout));
    }

    /** {@link #voorwaardefoutregel} for a default, whose rule a step of card drl-teksttak-op-truth-maintenance has not
     *  planned yet ({@link #eigenRegel}): {@code nieuw} adds constraints to the rendered subject pattern and
     *  {@code blokkade} the rendered control-fact patterns, so this rule is built directly, as before the step, until
     *  step 1c-C plans them. */
    private Rule voorwaardefoutregelRechtstreeks(String vorm, String regelnaam, Supplier<Lezingen> nieuw, Conditie c,
                                                 Function<Lezingen, List<RuleItemBuilder<?>>> blokkade) {
        Lezingen lez = nieuw.get();
        Set<Integer> gelezen = lez.volg();
        Term toets = voorwaarde(c, lez);
        lez.stop();
        List<RuleItemBuilder<?>> items = lez.linkerkant(Toetsitem.Rol.FOUT, toets, gelezen, c);
        if (blokkade != null) items.addAll(blokkade.apply(lez));
        String zinregel = model.geëmitteerdeRegelNaam(vorm);
        Toets gecompileerd = toetsweergave(toets);
        items.add(gevolg(lez, posities(gelezen), (drools, onderwerp, w) -> {
            RuntimeException f = fout(gecompileerd, w);
            if (f == null)
                throw new IllegalStateException("de voorwaarde van \"" + vorm + "\" faalde in de toets en niet in het"
                        + " gevolg, over dezelfde gebonden waarden");
            drools.logicalInsert(new Fout(onderwerp, vorm, Rekenfout.benoem(f, zinregel)));
        }, lez.variabelen.size()));
        lez.vastleggen();
        return PatternDSL.rule(PKG, regelnaam + ": fout in de voorwaarde").build(items.toArray(new RuleItemBuilder[0]));
    }

    /** The role of the test of a condition in the rule of its form: where the condition can fault it is tested without
     *  its fault, which the fault rule takes ({@link #voorwaardefoutregel}). */
    private static Toetsitem.Rol voorwaarderol(boolean kanFalen) {
        return kanFalen ? Toetsitem.Rol.ZONDER_FOUT : Toetsitem.Rol.VOORWAARDE;
    }

    /**
     * The rule that deposits the §9.5 inconsistency of RUNTIME-SEMANTICS ch. 1.2 for the condition {@code c} of
     * {@code vorm}, whose rule is {@code regelnaam}: the reference interpreter deposits wherever its evaluation of a
     * condition divides a filled dividend by a leeg divisor or takes the root of a negative number, whether the condition
     * then holds or not (ch. 5.6), and only at the places that evaluation reaches (ch. 5.10). A condition is tested in the
     * left-hand side, where Drools inserts nothing, so a rule of the form's own, with the same left-hand side
     * ({@code blokkade} included) and the test that evaluating the condition reaches such a place ({@link Sonde}),
     * inserts the deposit logically under the form and the subject; it goes when a read it rested on changes. Null where
     * {@code c} holds no division and no root.
     */
    private Rule deponeerregel(String vorm, String regelnaam, Supplier<Lezingen> nieuw, Conditie c,
                               Function<Lezingen, List<LhsItem>> blokkade) {
        if (!deponeert(c)) return null;
        Lezingen lez = nieuw.get();
        Set<Integer> gelezen = lez.volg();
        Term toets = voorwaarde(c, lez);
        lez.stop();
        int sonde = lez.sondeplaats();
        if (sonde < 0) throw new IllegalStateException("de voorwaarde van \"" + vorm + "\" deelt, maar kreeg geen sonde");
        List<LhsItem> items = lez.plan(Toetsitem.Rol.DEPONERING, toets, gelezen, c);
        if (blokkade != null) items.addAll(blokkade.apply(lez));
        lez.vastleggen();
        return weergeef(lez.regelplan(regelnaam + ": deponering in de voorwaarde", items,
                new Gevolg.Inconsistentie(ONDERWERP, vorm, Gevolg.Invoeging.LOGISCH)));
    }

    /** The evaluation fault {@code toets} raises over {@code w}, or null where it answers, as the test of the fault rule
     *  finds it ({@link ClosureWeergave#fout}): the consequence of that rule asks it again for the fault it inserts. */
    private static RuntimeException fout(Toets toets, Object[] w) {
        return ClosureWeergave.fout(toets::test, w);
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
     * the rule, where the assertion can fault, the rule that inserts its fault ({@link #voorwaardefoutregel}).
     */
    private List<Rule> consistentieRegels(ResolvedConsistentie c) {
        // the rule is named by the form, and the channel test reads the error facts by that name
        if (c.naam() == null) throw new Weigering("een consistentie zonder naam (WF-107)");
        if (c.subject() == null || c.assertie() == null) throw new Weigering("een consistentie zonder subject of assertie");
        Supplier<Lezingen> nieuw = () -> new Lezingen(c.subject(), false).geldig(c.geldigheid());
        Lezingen lez = nieuw.get();
        Set<Integer> gelezen = lez.volg();
        Term houdt = voorwaarde(c.assertie(), lez);
        boolean kanFalen = lez.kanFalen();
        lez.stop();
        Term geschonden = new Term.Aanroep(Functie.NIET, List.of(houdt), bron(c.assertie().bron()));
        List<LhsItem> items = lez.plan(voorwaarderol(kanFalen), geschonden, gelezen, c.assertie());
        String naam = c.naam();
        lez.vastleggen();
        List<Rule> uit = new ArrayList<>();
        uit.add(weergeef(lez.regelplan(model.geëmitteerdeRegelNaam(naam), items,
                new Gevolg.Inconsistentie(ONDERWERP, naam, Gevolg.Invoeging.LOGISCH))));
        if (kanFalen) uit.add(voorwaardefoutregel(naam, model.geëmitteerdeRegelNaam(naam), nieuw, c.assertie(), null));
        Rule deponering = deponeerregel(naam, model.geëmitteerdeRegelNaam(naam), nieuw, c.assertie(), null);
        if (deponering != null) uit.add(deponering);
        Rule zonderDatum = geldigheidsfout(model.geëmitteerdeRegelNaam(naam), naam, c.geldigheid());
        if (zonderDatum != null) uit.add(zonderDatum);
        return uit;
    }

    /** A uniqueness over one source (RUNTIME-SEMANTICS ch. 5.9), unscoped or scoped by a parent ({@link #uniciteitsregels}). */
    private List<Rule> uniciteitRegels(ResolvedUniciteit u) {
        if (u.subject() == null || u.sleutels().isEmpty()) throw new Weigering("een Uniqueness zonder bron of sleutel");
        return uniciteitsregels(u.naam(), List.of(new ResolvedUniciteitUnie.Bron(u.subject(), u.sleutels())),
                u.scopeRol(), u.geldigheid());
    }

    /** A uniqueness over two or more sources, the union of ch. 5.9, rule 1: one key domain, compared position by
     *  position, so every source carries as many keys (WF-030); a union has no scope (rule 3). */
    private List<Rule> uniciteitUnieRegels(ResolvedUniciteitUnie u) {
        if (u.bronnen().size() < 2)
            throw new Weigering("een Uniqueness over meer bronnen met " + u.bronnen().size() + " bron: een unie draagt er"
                    + " ten minste twee (WF-030)");
        int ariteit = u.bronnen().get(0).sleutels().size();
        for (ResolvedUniciteitUnie.Bron b : u.bronnen())
            if (b.type() == null || b.sleutels().size() != ariteit)
                throw new Weigering("een Uniqueness over meer bronnen met sleutels van ongelijke ariteit of een bron zonder"
                        + " type: de sleutels worden positie voor positie vergeleken (WF-030)");
        return uniciteitsregels(u.naam(), u.bronnen(), null, u.geldigheid());
    }

    /**
     * A uniqueness (RUNTIME-SEMANTICS ch. 5.6, 5.9): a key that more than one row carries marks every instance that
     * carries it on the §9.5 channel, under the form's name. A key rule per source, over the instances of the source's
     * type, binds the key components (a derived one through its standing) and, for a scoped form, the parent, and
     * inserts the instance's {@link Uniciteitssleutel} logically where every component is gevuld and the parent is
     * there: an empty component never collides, and an instance without a parent takes no part (rule 2). The violation
     * rule binds each key and counts the keys that collide with it, itself included, with an accumulate; where more
     * than one does it inserts the instance's {@link Uniciteitsschending}, which the channel rule turns into the §9.5
     * inconsistency, one per instance, also where two sources of a union reach it. A key belongs to its source, so an
     * instance that two sources reach with an equal key duplicates against itself (rule 1). Every fact is logical, so a
     * duplicate that arises on an intermediate state goes when the keys move on, and the channel holds what stands at
     * the fixpoint, where the reference interpreter evaluates the form (ch. 5.9). A key on a timeline reads
     * ever-uniqueness: one key per value its timeline ever carries ({@link #sleutelregel}), so two instances collide
     * where they ever carried one value, and an instance whose key the case gives as the asOfDate-bucket scalar faults
     * ({@link #sleutelvormregel}, ch. 8.4). A scope over a relation a form creates is refused.
     */
    private List<Rule> uniciteitsregels(String naam, List<ResolvedUniciteitUnie.Bron> bronnen, RolBindingRef scopeRol,
                                        Geldigheid geldigheid) {
        // the rules are named by the form, and the channel test reads the error facts by that name
        if (naam == null) throw new Weigering("een Uniqueness zonder naam (WF-107)");
        String regelnaam = model.geëmitteerdeRegelNaam(naam);
        List<Rule> uit = new ArrayList<>();
        for (int b = 0; b < bronnen.size(); b++) {
            uit.add(sleutelregel(naam, regelnaam, b, bronnen.get(b), scopeRol, geldigheid));
            Rule vorm = sleutelvormregel(naam, regelnaam, b, bronnen.get(b), geldigheid);
            if (vorm != null) uit.add(vorm);
        }
        Variable<Uniciteitssleutel> k = DSL.declarationOf(Uniciteitssleutel.class, "$sleutel");
        Variable<Object> element = DSL.declarationOf(Object.class, "$element");
        Variable<Uniciteitssleutel> ander = DSL.declarationOf(Uniciteitssleutel.class, "$ander");
        Variable<Object> botsingen = DSL.declarationOf(Object.class, "$botsingen");
        uit.add(PatternDSL.rule(PKG, regelnaam).build(
                PatternDSL.pattern(k).expr(id("uniciteit"), x -> x.uniciteit().equals(naam))
                        .bind(element, Uniciteitssleutel::element),
                DSL.accumulate(PatternDSL.pattern(ander).expr(id("uniciteit"), x -> x.uniciteit().equals(naam))
                                .expr(id("botsing"), k, Uniciteitssleutel::botstMet),
                        DSL.accFunction(accumulatiefunctie("CountAccumulateFunction"), ander).as(botsingen)),
                DSL.expr(id("dubbel"), botsingen, n -> ((Number) n).longValue() > 1),
                DSL.on(element).execute((Block2<org.drools.model.Drools, Object>) (drools, e) ->
                        drools.logicalInsert(new Uniciteitsschending(naam, e)))));
        Variable<Uniciteitsschending> s = DSL.declarationOf(Uniciteitsschending.class, "$schending");
        Variable<Object> subject = DSL.declarationOf(Object.class, "$subject");
        uit.add(PatternDSL.rule(PKG, regelnaam + ": kanaal").build(
                PatternDSL.pattern(s).expr(id("schending"), x -> x.uniciteit().equals(naam))
                        .bind(subject, Uniciteitsschending::element),
                DSL.on(subject).execute((Block2<org.drools.model.Drools, Object>) (drools, e) ->
                        drools.logicalInsert(RegelSpraakRuntime.newInconsistentie(INCONSISTENTIE, naam, e)))));
        Rule zonderDatum = geldigheidsfout(regelnaam, naam, geldigheid);
        if (zonderDatum != null) uit.add(zonderDatum);
        return uit;
    }

    /** The key rule of source {@code b} of the uniqueness {@code naam}: over the instances of the source's type, gated
     *  on the evaluation date by the form's validity, the key components and the parent bound, and the instance's
     *  {@link Uniciteitssleutel} inserted logically where it takes part ({@link #uniciteitsregels}). An enumeration
     *  component is compared as the declared name it is, up to normalisation (ch. 5.6).
     *
     *  <p>A component on a timeline reads ever-uniqueness (RUNTIME-SEMANTICS ch. 5.9, decided 2026-08-20): it claims
     *  every value its timeline ever carries, so the rule inserts one key per such value, beside the other components,
     *  and none where the timeline is absent or carries no value. The timeline is bound like any read
     *  ({@link Lezingen#tijdlijn}), so a key a rule derives later takes its keys along. The values are taken by the
     *  runtime's {@code rsOoitWaarden}: without trailing zeros and once each, so an instance whose timeline carries one
     *  value in two segments claims it once and does not duplicate against itself. A key has at most one component on a
     *  timeline, as the reference interpreter refuses more: the same-moment join of two has no corpus case. */
    private Rule sleutelregel(String naam, String regelnaam, int b, ResolvedUniciteitUnie.Bron bron, RolBindingRef scopeRol,
                              Geldigheid geldigheid) {
        Objecttype type = bron.type();
        Lezingen lez = new Lezingen(type).geldig(geldigheid);
        Set<Integer> gelezen = lez.volg();
        List<Integer> sleutels = new ArrayList<>();
        List<Boolean> enumeratie = new ArrayList<>();
        int ooit = -1;
        for (String sleutel : bron.sleutels()) {
            if (gedeclareerd(type, sleutel, false).tijd() != null) {
                String wat = "een uniciteitssleutel '" + sleutel + "' van " + type.naam + " op een tijdlijn";
                if (ooit >= 0)
                    throw new Weigering(wat + " naast een tweede: een sleutel heeft hoogstens één tijdsafhankelijk deel,"
                            + " de gelijktijdige koppeling van twee heeft geen corpuscase (RUNTIME-SEMANTICS ch. 5.9); "
                            + NOG_NIET);
                Lid lid = tijdlid(type, sleutel, false, wat);
                if (!lid.datatype().isNumeriek())
                    throw new Weigering(wat + " die geen getal is: " + NOG_NIET);
                ooit = sleutels.size();
                sleutels.add(lez.tijdlijn(lid));
                enumeratie.add(false);
                continue;
            }
            Lid lid = lid(type, sleutel, false);
            sleutels.add(lez.eigen(lid));
            enumeratie.add(lid.datatype() instanceof Datatype.Enumeratie);
        }
        int scope = scopeRol == null ? -1 : lez.scopelezing(scopeRol);
        lez.stop();
        int tijdlijn = ooit;
        // the key takes part where every component off the timeline is gevuld and the parent is set
        List<Term> deelname = new ArrayList<>();
        for (int i = 0; i < sleutels.size(); i++) if (i != tijdlijn) deelname.add(gevuld(sleutels.get(i)));
        // a key the case gives as the asOfDate-bucket scalar is the fault of the key-form rule beside this one
        if (tijdlijn >= 0) deelname.add(emmerscalair(sleutels.get(tijdlijn), false));
        if (scope >= 0) deelname.add(gevuld(scope));
        List<RuleItemBuilder<?>> items = lez.linkerkant(Toetsitem.Rol.VORM, alle(deelname), gelezen, null);
        items.add(gevolg(lez, posities(gelezen), (drools, o, w) -> {
            List<Object> waarden = new ArrayList<>(sleutels.size());
            for (int i = 0; i < sleutels.size(); i++) {
                Object v = w[sleutels.get(i)];
                waarden.add(enumeratie.get(i) ? RegelSpraakRuntime.rsEnumNaam(v) : v);
            }
            Object ouder = scope < 0 ? null : w[scope];
            if (tijdlijn < 0) {
                drools.logicalInsert(new Uniciteitssleutel(naam, b, o, ouder, waarden));
                return;
            }
            for (BigDecimal waarde : new TreeSet<>(RegelSpraakRuntime.rsOoitWaarden(waarden.get(tijdlijn)))) {
                List<Object> sleutel = new ArrayList<>(waarden);
                sleutel.set(tijdlijn, waarde);
                drools.logicalInsert(new Uniciteitssleutel(naam, b, o, ouder, sleutel));
            }
        }, lez.variabelen.size()));
        lez.vastleggen();
        return PatternDSL.rule(PKG, regelnaam + ": sleutel van " + type.naam + " " + (b + 1))
                .build(items.toArray(new RuleItemBuilder[0]));
    }

    /**
     * The rule beside the key rule of source {@code b} of the uniqueness {@code naam} where its key has a component on a
     * timeline: over the instances of the source's type, gated by the form's validity, the components up to that one
     * bound as the key rule binds them, the test that every component before it is gevuld and that the case gives the
     * timeline as the asOfDate-bucket scalar ({@link Emmerscalair}), and a consequence that inserts the fault of
     * RUNTIME-SEMANTICS ch. 5.9 logically as a {@link Fout} of the instance ({@link Invoervormfout}), in the reference
     * interpreter's words: ever-uniqueness reads every value the timeline ever carries, and a scalar would miss values.
     * The key's form is checked on every instance of the type, before the scope narrows the rows ("this form error
     * precedes the scope narrowing"), so the parent and the components after the timeline do not enter; the reference
     * interpreter builds a row's tuples component by component and stops at an empty one, so an empty component before
     * the timeline leaves the timeline unread. Null for a key without a component on a timeline.
     */
    private Rule sleutelvormregel(String naam, String regelnaam, int b, ResolvedUniciteitUnie.Bron bron,
                                  Geldigheid geldigheid) {
        Objecttype type = bron.type();
        int positie = -1;
        for (int i = 0; i < bron.sleutels().size() && positie < 0; i++)
            if (gedeclareerd(type, bron.sleutels().get(i), false).tijd() != null) positie = i;
        if (positie < 0) return null;
        Lezingen lez = new Lezingen(type).geldig(geldigheid);
        Set<Integer> gelezen = lez.volg();
        List<Integer> ervoor = new ArrayList<>();
        for (int i = 0; i < positie; i++) ervoor.add(lez.eigen(lid(type, bron.sleutels().get(i), false)));
        Lid tijdlid = tijdlid(type, bron.sleutels().get(positie), false, "een uniciteitssleutel '"
                + bron.sleutels().get(positie) + "' van " + type.naam + " op een tijdlijn");
        int tijdlijn = lez.tijdlijn(tijdlid);
        lez.stop();
        List<Term> emmer = new ArrayList<>();
        for (int i : ervoor) emmer.add(gevuld(i));
        emmer.add(emmerscalair(tijdlijn, true));
        List<RuleItemBuilder<?>> items = lez.linkerkant(Toetsitem.Rol.VORM, alle(emmer), gelezen, null);
        String sleutelnaam = tijdlid.naam();
        items.add(gevolg(lez, posities(gelezen), (drools, o, w) -> drools.logicalInsert(new Fout(o, naam,
                new Invoervormfout("the ever-unique key \"" + sleutelnaam + "\" needs the full timeline; ", o,
                        " supplies only a scalar (ch. 5.9)").inDeRegel(regelnaam))), lez.variabelen.size()));
        lez.vastleggen();
        return PatternDSL.rule(PKG, regelnaam + ": sleutelvorm van " + type.naam + " " + (b + 1))
                .build(items.toArray(new RuleItemBuilder[0]));
    }

    /**
     * A test of the §9.5 channel, "regelversie X is inconsistent" (RUNTIME-SEMANTICS ch. 5.6): an existential over the
     * error facts of the form X, with no subject of its own, read through X's {@link KanaalStand}. The producers this
     * branch carries are a consistency, a uniqueness, and a rule whose condition or value can deposit a §6.5 or §6.6
     * inconsistency ({@link #schrijfregel}, {@link #deponeerregel}); any other name is refused, since a test of a
     * channel nothing fills would never hold and say nothing. "is gevuurd" reads no channel; it is no construct of the
     * core, and is refused with that reason.
     */
    private Term kanaallezing(Conditie.Regelversie rv, Lezingen lez) {
        // decided as a predicate of the dialect over the fixpoint, never a construct of the core: the reference
        // interpreter faults on it ("not implemented") and the ASP backend does not translate it
        if (!rv.inconsistent())
            throw nietGelezen("Regelversie 'is gevuurd'", rv, "geen vorm van de kern maar een predicaat van het"
                    + " RegelSpraak-dialect over het fixpunt (RUNTIME-SEMANTICS ch. 5.6, CONSTRUCTS rij 30), dat de"
                    + " referentie-interpreter en de ASP-backend niet evalueren");
        String producent = rv.regel();
        if (!kanaalproducent(producent))
            throw new Weigering("een Regelversie 'is inconsistent' over \"" + producent + "\" in de voorwaarde ('"
                    + snippet(rv) + "'): deze tak draagt als producent van het §9.5-kanaal alleen een"
                    + " Consistency, een Uniqueness en een regel met een deling of een wortel in haar voorwaarde of haar"
                    + " waarde (§6.5, §6.6)");
        int i = lez.kanaal(producent);
        return new Term.Aanroep(Functie.KANAAL, List.of(new Term.Lees(i, null)), bron(rv.bron()));
    }

    /** Whether the form named {@code naam}, spelled as the error facts carry it, fills a channel this branch lowers. */
    private boolean kanaalproducent(String naam) {
        if (naam == null) return false;
        for (ResolvedConsistentie c : model.consistenties) if (naam.equals(c.naam())) return true;
        for (ResolvedUniciteit u : model.uniciteiten) if (naam.equals(u.naam())) return true;
        for (ResolvedUniciteitUnie u : model.uniciteitUnies) if (naam.equals(u.naam())) return true;
        for (ResolvedRegel r : model.resolvedRegels)
            if (naam.equals(r.naam()) && (deponeert(r.voorwaarde())
                    || r.soort() != ResolvedRegel.Soort.KENMERKTOEKENNING && deponeert(r.waarde())))
                return true;
        return false;
    }

    /** Whether a value holds a place the §6.5 or §6.6 deposit of {@link #schrijfregel} covers ({@link #deponeerplek}),
     *  where the rule's own evaluation of the value can reach it: through every form {@link #waarde} evaluates in the
     *  rule itself, and into the filter of a fold over a set source and of each selection of an instance-set union,
     *  which a filter deposit rule evaluates and the reading rule hears where it reaches the fold
     *  ({@link #filterdeponeerregel}, {@link #gelezenVouw}). The argument of such a fold is a read of the member's
     *  attribute or the union, nothing else (WF-037, refused by the loader), so it holds no place of its own. */
    private boolean deponeert(Expr e) {
        return switch (e) {
            case null -> false;
            case Expr.Binair b -> deponeerplek(b) || deponeert(b.links()) || deponeert(b.rechts());
            case Expr.Functie f -> deponeerplek(f) || f.argumenten().stream().anyMatch(this::deponeert)
                    || deponeert(f.leegwaarde()) || deponeert(f.filter());
            case Expr.InstanceSetUnion u -> u.takken().stream().anyMatch(t -> deponeert(t.filter()));
            case Expr.Afgerond a -> deponeert(a.basis());
            case Expr.Begrensd g -> deponeert(g.basis()) || deponeert(g.minimum()) || deponeert(g.maximum());
            case Expr.Let l -> deponeert(l.value()) || deponeert(l.body());
            case Expr.IfThenElse k -> deponeert(k.conditie()) || deponeert(k.dan()) || deponeert(k.anders());
            case Expr.TekstReeks t -> t.delen().stream()
                    .anyMatch(d -> d instanceof Expr.TekstReeks.Ingevoegd i && deponeert(i.expr()));
            default -> false;
        };
    }

    private boolean deponeert(Conditie c) {
        return switch (c) {
            case null -> false;
            case Conditie.Vergelijking v -> deponeert(v.links()) || deponeert(v.rechts());
            case Conditie.Predicaat p -> deponeert(p.operand());
            case Conditie.Samengesteld s -> s.onderdelen().stream().anyMatch(this::deponeert);
            case Conditie.Kwantorvouw kv -> deponeert(kv.filter()) || deponeert(kv.toets());
            default -> false;
        };
    }

    /** Whether {@code e} is itself a place that deposits: every division and every square root, whatever its operands,
     *  as the reference interpreter deposits wherever it divides a filled dividend by a leeg divisor or takes the root of
     *  a negative number (RUNTIME-SEMANTICS ch. 1.2, 5.6). */
    private static boolean deponeerplek(Expr e) {
        if (e instanceof Expr.Binair b) return b.op() == Expr.BinOp.GEDEELD_DOOR || b.op() == Expr.BinOp.GEDEELD_DOOR_ABS;
        return e instanceof Expr.Functie f && f.kind() == Expr.FunctieKind.WORTEL && f.argumenten().size() == 1;
    }

    /**
     * The two generic rules that keep one {@link KanaalStand} for the channel of the form named {@code producent}:
     * inconsistent while an error fact of that form stands, on any subject, and consistent while none does. The second
     * carries the negation over the channel, so a condition that reads it, in either polarity, binds the standing as a
     * positive pattern.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private List<Rule> kanaalstandregels(String producent) {
        Class<?> klasse;
        try {
            klasse = Class.forName(INCONSISTENTIE, true, beanLoader);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("geen beanklasse " + INCONSISTENTIE + " voor het kanaal van " + producent, e);
        }
        String naam = "kanaalstand " + producent + ": ";
        Variable i1 = DSL.declarationOf(klasse, "$inconsistentie");
        Rule inconsistent = PatternDSL.rule(PKG, naam + "inconsistent").build(
                DSL.exists(PatternDSL.pattern(i1).expr(id("kanaal"),
                        (Predicate1) i -> producent.equals(RegelSpraakRuntime.get(i, "regel")))),
                DSL.execute((org.drools.model.functions.Block1<org.drools.model.Drools>) drools ->
                        drools.logicalInsert(new KanaalStand(producent, true))));
        Variable i2 = DSL.declarationOf(klasse, "$inconsistentie");
        Rule consistent = PatternDSL.rule(PKG, naam + "consistent").build(
                DSL.not(PatternDSL.pattern(i2).expr(id("kanaal"),
                        (Predicate1) i -> producent.equals(RegelSpraakRuntime.get(i, "regel")))),
                DSL.execute((org.drools.model.functions.Block1<org.drools.model.Drools>) drools ->
                        drools.logicalInsert(new KanaalStand(producent, false))));
        return List.of(inconsistent, consistent);
    }

    // ---------------- an allocation ----------------

    /**
     * An allocation (IR-SPEC ch. 3.2, RUNTIME-SEMANTICS ch. 4): a computation over the set of recipients that the
     * left-hand side binds. One rule of the allocation inserts, per source and per recipient, an {@link Ontvanger}
     * logically: the recipient is an instance of {@code targetType} that the connecting relation links to the source
     * ({@link #ontvangersjoin}), so a related instance that is no {@code targetType} receives nothing and stays leeg
     * (ch. 4.1), and its weight, ordering key and cap are bound in that rule's left-hand side, a derived one through its
     * standing. The allocation's own rule binds the source, its pot, the reads of its condition and, through an
     * accumulate, the list of the source's {@link Ontvanger} facts ({@link Lezingen#ontvangers}), and its consequence
     * computes the distribution from those bound values only ({@link Verdeling}): each share and the rest are a
     * {@link Waarde} with the allocation as the writer, and each degradation of ch. 4.3 and 4.4 (an empty weight, a
     * negative proportional weight, an empty or negative cap) is the allocation's §9.5 deposit on that recipient. All of
     * it is logical, so a recipient that arrives or leaves, a weight, key, cap or pot that changes, and a condition that
     * stops holding take the whole distribution of the source along, the rest included (ch. 4.3), and a deposit made on
     * an intermediate state goes again: like a fault it counts at the fixpoint (PLAN §9, 2026-10-09). An empty pot is
     * no distribution in every method, as ch. 4.4 decides for v1: it writes nothing, deposits nothing and is no fault,
     * where the reference interpreter keeps the v0 hole of the equal-shares and the proportional method and faults
     * ({@link Verdeling}). A version with a bounded validity gates on the evaluation date and faults a session
     * without one, as a rule's version does; a condition that can fault or deposit has its own fault and deposit rules,
     * on the source, as the reference interpreter evaluates the condition per source before anything else.
     *
     * <p>A share or a rest replaces the input on its target, as the reference interpreter overwrites it, and a recipient
     * that receives nothing keeps its input; the member value an object creation sets is the input of the object it
     * makes. Every other writer of a target is refused ({@link #verdelingsschrijvers}): two allocations on one target
     * are ill-formed (WF-026), so is a defaults group beside an allocation (WF-050), and a rule beside one, a computing
     * rule or an initialisation, this branch does not carry yet.
     */
    private List<Rule> verdeling(ResolvedVerdeling v) {
        String naam = v.naam();
        // the rules of a form are named by the form, and the deposits and shares carry that name
        if (naam == null) throw new Weigering("een verdeling zonder naam (WF-107)");
        Objecttype bron = v.bronType(), doel = v.doelType();
        ResolvedVerdeling.Methode methode = v.methode();
        if (bron == null || doel == null || v.bronAttribuut() == null || v.doelAttribuut() == null || methode == null)
            throw new Weigering("een verdeling zonder opgeloste bron, doel of methode");
        RolBindingRef rb = rolBindingen.get(v);
        if (rb == null) throw new Weigering("een verdeling zonder connectingRole (IR-SPEC ch. 3.2)");
        // WF-025, which the verifier holds and this backend does not run: the fields each method carries
        boolean gelijk = methode == ResolvedVerdeling.Methode.GELIJKE_DELEN;
        if (methode == ResolvedVerdeling.Methode.NAAR_RATO && v.ratoAttribuut() == null)
            throw new Weigering("een verdeling naar rato zonder gewicht (WF-025)");
        if (gelijk && v.ratoAttribuut() != null)
            throw new Weigering("een verdeling in gelijke delen met een gewicht (WF-025)");
        if (v.maximumAttribuut() != null && v.ratoAttribuut() == null)
            throw new Weigering("een verdeling met een maximum zonder gewicht (WF-025)");
        if (methode != ResolvedVerdeling.Methode.OP_VOLGORDE && v.groepeerAttribuut() != null)
            throw new Weigering("een volgordesleutel op een verdeling die niet op volgorde verdeelt (WF-025)");
        Lid pot = getallid(bron, v.bronAttribuut(), "de pot");
        Lid aandeel = getallid(doel, v.doelAttribuut(), "het aandeel");
        Lid rest = v.restantAttribuut() == null ? null : getallid(bron, v.restantAttribuut(), "het restant");
        Lid gewicht = v.ratoAttribuut() == null ? null : getallid(doel, v.ratoAttribuut(), "het gewicht");
        Lid maximum = v.maximumAttribuut() == null ? null : getallid(doel, v.maximumAttribuut(), "het maximum");
        Lid sleutel = v.groepeerAttribuut() == null ? null : lid(doel, v.groepeerAttribuut(), false);
        if (sleutel != null && !sleutel.datatype().isOrdenbaar())
            throw new Weigering("de volgordesleutel '" + sleutel.naam() + "' van type " + typenaam(sleutel.datatype())
                    + " is niet ordenbaar (WF-025)");
        verdelingsschrijvers(v, doel, aandeel);
        if (rest != null) verdelingsschrijvers(v, bron, rest);

        String regelnaam = model.geëmitteerdeRegelNaam(naam);
        List<Rule> uit = new ArrayList<>();

        // per source and recipient: the recipient's values, bound in this rule's left-hand side
        Lezingen lid = bijdragelezingen(bron, doel, ontvangersjoin(rb, bron, doel, naam));
        Set<Integer> lidgebruik = lid.volg();
        int ontvanger = lid.lid();
        int g = gewicht == null ? -1 : lid.eigen(gewicht);
        int k = sleutel == null ? -1 : lid.eigen(sleutel);
        int m = maximum == null ? -1 : lid.eigen(maximum);
        lid.stop();
        List<RuleItemBuilder<?>> lidregel = lid.linkerkant(Toetsitem.Rol.VOORWAARDE, null, Set.of(), null);
        lidregel.add(gevolg(lid, lid.drager, posities(lidgebruik), (drools, b, w) ->
                drools.logicalInsert(new Ontvanger(naam, b, w[ontvanger], g < 0 ? null : w[g], k < 0 ? null : w[k],
                        m < 0 ? null : w[m])), lid.variabelen.size()));
        lid.vastleggen();
        uit.add(PatternDSL.rule(PKG, regelnaam + ": ontvanger").build(lidregel.toArray(new RuleItemBuilder[0])));

        // per source: the condition, the pot and the list of recipients, and the distribution computed from them
        Supplier<Lezingen> nieuw = () -> new Lezingen(bron).geldig(v.geldigheid());
        Lezingen lez = nieuw.get();
        Set<Integer> gelezen = lez.volg();
        Term toets = v.voorwaarde() == null ? null : voorwaarde(v.voorwaarde(), lez);
        boolean kanFalen = lez.kanFalen();
        lez.stop();
        Set<Integer> gebruik = lez.volg();
        int p = lez.eigen(pot);
        int lijst = lez.ontvangers(naam);
        lez.stop();
        List<RuleItemBuilder<?>> items = lez.linkerkant(voorwaarderol(kanFalen), toets, gelezen, v.voorwaarde());
        ElementSleutel aandeelSleutel = aandeel.sleutel(), restSleutel = rest == null ? null : rest.sleutel();
        boolean gewogen = gewicht != null, gemaximeerd = maximum != null, afnemend = v.afnemend();
        Integer decimalen = v.afrondingDecimalen();
        items.add(gevolg(lez, posities(gebruik), (drools, onderwerp, w) -> {
            List<Ontvanger> ontvangers = new ArrayList<>();
            for (Object o : (List<?>) w[lijst]) ontvangers.add((Ontvanger) o);
            Verdeling.Uitslag u = Verdeling.bereken(methode, w[p], ontvangers, gewogen, gemaximeerd, decimalen,
                    afnemend);
            for (Object r : u.deponeringen())
                drools.logicalInsert(RegelSpraakRuntime.newInconsistentie(INCONSISTENTIE, naam, r));
            for (Map.Entry<Object, BigDecimal> e : u.aandelen().entrySet())
                drools.logicalInsert(new Waarde(e.getKey(), aandeelSleutel, e.getValue(), naam));
            if (restSleutel != null && u.rest() != null)
                drools.logicalInsert(new Waarde(onderwerp, restSleutel, u.rest(), naam));
        }, lez.variabelen.size()));
        lez.vastleggen();
        uit.add(PatternDSL.rule(PKG, regelnaam).build(items.toArray(new RuleItemBuilder[0])));
        afgeleid.put(aandeelSleutel, aandeel);
        if (rest != null) afgeleid.put(restSleutel, rest);
        if (kanFalen) uit.add(voorwaardefoutregel(naam, regelnaam, nieuw, v.voorwaarde(), null));
        Rule deponering = deponeerregel(naam, regelnaam, nieuw, v.voorwaarde(), null);
        if (deponering != null) uit.add(deponering);
        Rule zonderDatum = geldigheidsfout(regelnaam, naam, v.geldigheid());
        if (zonderDatum != null) uit.add(zonderDatum);
        return uit;
    }

    /** Attribute {@code naam} of {@code houder} as an allocation reads or writes it as a number: the pot, a share, the
     *  rest, a weight or a cap, {@code rol} naming which. The distribution computes with numbers only, so another
     *  datatype is refused by name. */
    private Lid getallid(Objecttype houder, String naam, String rol) {
        Lid lid = lid(houder, naam, false);
        if (!lid.datatype().isGetalachtig())
            throw new Weigering("een verdeling waarvan " + rol + " het attribuut '" + lid.naam() + "' van "
                    + lid.declarant().naam + " is, van type " + typenaam(lid.datatype()) + " en geen getal: " + NOG_NIET);
        return lid;
    }

    /**
     * Refuses the allocation {@code v} where another form writes the attribute {@code lid} that it writes on
     * {@code type}, its share target or its rest, on an instance it writes too: on {@code type}, an ancestor or a
     * descendant ({@link #verwant}), read per concrete type as WF-026 and WF-050 read it, so a form on a sister subtype
     * does not count. Another allocation, not a version of {@code v}, is ill-formed beside it (WF-026), and so is a
     * defaults group (WF-050). A rule beside it is well-formed, and refused until this branch carries it: a computing
     * rule is a second equation of the target, whose value must agree at the fixpoint (RUNTIME-SEMANTICS ch. 5.7), a
     * disagreement this branch keys on two computing rules only ({@link #verschilregel}); an initialisation is the
     * lowest default beside the allocation (ch. 7.7), whose guard would read the allocation's shares as it reads every
     * other writer's value, but no witness holds that pair against the reference interpreter yet. The member value an
     * object creation sets is no writer here: it is the input of the object it makes, which a share replaces.
     */
    private void verdelingsschrijvers(ResolvedVerdeling v, Objecttype type, Lid lid) {
        ElementSleutel el = lid.sleutel();
        String eigen = ProducentConsument.basisNaam(v.naam());
        Set<String> verdelingen = new LinkedHashSet<>(), groepen = new LinkedHashSet<>(), regels = new LinkedHashSet<>();
        for (ResolvedVerdeling o : model.verdelingen) {
            if (o.naam() != null && ProducentConsument.basisNaam(o.naam()).equals(eigen)) continue;
            boolean schrijft = o.doelType() != null && o.doelAttribuut() != null && verwant(o.doelType(), type)
                    && ProducentConsument.elementVan(o.doelType(), o.doelAttribuut(), false).equals(el)
                    || o.bronType() != null && o.restantAttribuut() != null && verwant(o.bronType(), type)
                    && ProducentConsument.elementVan(o.bronType(), o.restantAttribuut(), false).equals(el);
            if (schrijft) verdelingen.add("\"" + naamOf(o.naam()) + "\"");
        }
        if (!verdelingen.isEmpty())
            throw new Weigering("twee verdelingen schrijven het attribuut '" + lid.naam() + "' van " + type.naam
                    + " (ook " + String.join(", ", verdelingen) + "): een doel heeft hoogstens één verdeling (WF-026)");
        for (int i = 0; i < defaults.size(); i++) {
            PrioritizedDefaults pd = defaults.get(i);
            Objecttype t = model.objecttypeOpNaam(pd.onderwerp());
            if (t != null && pd.doel() != null && !pd.isKenmerk() && verwant(t, type)
                    && ProducentConsument.elementVan(t, pd.doel(), false).equals(el))
                groepen.add(pd.naam() != null ? "\"" + pd.naam() + "\"" : "/rules/prioritizedDefaults/" + i);
        }
        if (!groepen.isEmpty())
            throw new Weigering("een verdeling naast de defaultsgroep " + String.join(", ", groepen) + " op het attribuut '"
                    + lid.naam() + "' van " + type.naam + ": een doel van een defaultsgroep heeft geen andere schrijver"
                    + " (WF-050)");
        for (ResolvedRegel r : model.resolvedRegels)
            if (r.soort() != ResolvedRegel.Soort.KENMERKTOEKENNING
                    && r.doel() instanceof Expr.AttribuutRef d && d.objecttype() != null && d.attribuut() != null
                    && verwant(d.objecttype(), type)
                    && ProducentConsument.elementVan(d.objecttype(), d.attribuut(), false).equals(el))
                regels.add("\"" + naamOf(r.naam()) + "\"");
        if (!regels.isEmpty())
            throw new Weigering("een verdeling naast de regel " + String.join(", ", regels) + " op het attribuut '"
                    + lid.naam() + "' van " + type.naam + ": een verdeling naast een rekenende regel of een initialisatie"
                    + " op één doel; " + NOG_NIET);
    }

    /**
     * How the rule of an allocation joins a recipient, its subject, to the source, its {@link Lezingen#drager}, over the
     * connecting relation {@code rb}, whose bound role is the recipient's and its sibling the source's (IR-SPEC ch. 3.2),
     * as a fold's one-step range joins a member ({@link #eenstapsbereik}): over a relation a form creates, through a
     * {@link Koppeling} in an {@code exists}; over an input relation, through the one scalar bean field §13.3.9 stores
     * it in, on the source where the recipient role is to-one and else on the recipient. Refused as there: a binding
     * that names no role pair, a to-one recipient role whose sibling is to-many, and a join the bean model gives no
     * field.
     */
    private Consumer<Lezingen> ontvangersjoin(RolBindingRef rb, Objecttype bron, Objecttype doel, String naam) {
        RegelSpraakModel.Rol[] paar = FactModelGenerator.gebondenRolpaar(model, rb);
        if (paar == null)
            throw new Weigering("de connectingRole van de verdeling noemt geen gedeclareerde rol (" + rb.feittype() + ", "
                    + rb.rol() + ")");
        if (rb.feittype() != null && gecreeerdeRelaties.contains(Namen.norm(rb.feittype())))
            return koppelingsbereik(rb, paar[0], "de verdeling '" + naam + "'").koppel();
        String ontvangerrol = paar[0].naam(), bronrol = paar[1].naam();
        boolean voorwaarts = !model.naarVeelRol(bron, doel, ontvangerrol);
        if (voorwaarts && model.naarVeelRol(doel, bron, bronrol))
            throw new Weigering("een verdeling over de enkelvoudige ontvangerrol '" + ontvangerrol + "' wiens zusterrol '"
                    + bronrol + "' meervoudig is: dat feittype heeft één beanveld voor beide richtingen, zoals een vouw"
                    + " over zo'n rol weigert (§13.3.9 opslagrichting)");
        Objecttype drager = voorwaarts ? bron : doel, ander = voorwaarts ? doel : bron;
        String rol = voorwaarts ? ontvangerrol : bronrol;
        if (!FactModelGenerator.heeftReferentieVeld(model, drager, rol, ander))
            throw new Weigering("de join van de verdeling heeft geen beanveld — "
                    + FactModelGenerator.geenJoinveldReden(model, drager, rol, ander) + " (§13.3.9 opslagrichting)");
        Function<Object, Object> veld = Accessors.getter(bonenklasse(drager), rol);
        return lid -> {
            if (voorwaarts) zelfde(lid.onderwerp, "verdelingsbereik", lid.drager, e -> e, veld::apply);
            else zelfde(lid.onderwerp, "verdelingsbereik", lid.drager, veld::apply);
        };
    }

    // ---------------- object and fact creation ----------------

    /** One member initialisation of an object creation: the member on the created type, and its value on the subject. */
    private record Initialisatie(Lid lid, Waardefunctie waarde) {}

    /**
     * An object creation (RUNTIME-SEMANTICS ch. 5.5, IR-SPEC ch. 3.3), a rule of the agenda group {@link #OBJECTCREATIE}:
     * the subject pattern with the reads of the condition and of the member values bound as a rule's are, the
     * {@link Creatiefase} the driver opened, no {@link Gemaakt} of this creation for the subject, no {@link Gemaakt} that
     * makes the subject itself in this phase, and the condition as the test; and a consequence that makes a new bean of
     * the created type and inserts, stated, the bean, its {@link Gemaakt}, the {@link Koppeling} of the connecting
     * relation and a {@link HeeftKenmerk} for a characteristic initialisation whose value is true. An attribute
     * initialisation whose value is gevuld is set on the new bean before it is inserted: it is the input of the made
     * object, as the reference interpreter reads it, so a writer of the member displaces it as it displaces case input,
     * and an initialisation rule on the member finds the target filled. Each value is computed from the values the rule
     * binds when it fires. The driver fires this group only at a fixpoint of the rule layer, so the condition and the
     * values are those of that fixpoint; and nothing the creation made lapses, as in the reference interpreter, which
     * keeps a minted instance under a fixed label. One object per creation per subject, then. The object is a bean of
     * the created type, so every pattern over that type sees it, and nothing is written on a bean that was there before:
     * the link toward it is the {@link Koppeling}, which the readers of the relation join over.
     *
     * <p>The 1:1 shape guards on the input world, as ch. 5.5 says: a subject whose reference field toward the created
     * type is filled in the case input creates nothing; this branch writes no bean while it fires, so the field is the
     * input. The decision of the link roles is the shared one ({@link FactModelGenerator#creatiekoppeling}): the
     * creation's {@code connectingRole} binding, or without one the derivation from the type pair that the DRL-text
     * branch takes as well. The relation of a derived pair is {@link #typenpaarrelatie}, and either way the link is the
     * same {@link Koppeling}, put on both ends at the write-back. Refused by name besides: an ambiguous back-link, which
     * ch. 5.5 makes a fault of the program; a derived pair whose roles the decision reaches only through a supertype of
     * either end, whose relation the strata of this branch do not see written; a connecting relation that is not one of
     * two roles; a characteristic initialisation whose value is no truth value (WF-027); and
     * a division in a member value, whose §6.5 deposit this branch does not make. Where the condition can fault, its
     * fault rule is an ordinary rule of the rule layer, without the marks and without the 1:1 guard: the reference
     * interpreter evaluates the condition first, in every pass.
     */
    private List<Rule> objectCreatie(ResolvedObjectCreatie oc) {
        if (oc.naam() == null) throw new Weigering("een objectcreatie zonder naam (WF-107)");
        Objecttype nieuw = oc.nieuwType(), subject = oc.subject();
        if (nieuw == null || subject == null) throw new Weigering("een objectcreatie zonder opgelost type");
        RolBindingRef vr = rolBindingen.get(oc);
        FactModelGenerator.Creatiekoppeling k = FactModelGenerator.creatiekoppeling(model, nieuw, subject, vr, oc.naam());
        if (k.dubbelzinnig() != null)
            throw new Weigering(k.dubbelzinnig() + "; " + NOG_NIET);
        if (k.weigering() != null) throw new Weigering(k.weigering());
        RegelSpraakModel.Feittype ft = vr != null ? feittype(vr.feittype()) : typenpaarrelatie(subject, nieuw, k);
        if (vr == null && ft == null)
            throw new Weigering("een objectcreatie zonder connectingRole-binding waarvan de terugkoppeling uit het typenpaar"
                    + " ('" + k.backRol() + "' en '" + k.fwdRol() + "') via een supertype van het onderwerp of van het"
                    + " gemaakte type loopt: de strata van deze tak leiden de relatie van de terugkoppeling af uit de exacte"
                    + " typen en zien haar dan niet geschreven: " + NOG_NIET);
        if (ft == null || ft.rollen.size() != 2 || ft.wederkerig)
            throw new Weigering("een objectcreatie over de relatie '" + (vr != null ? vr.feittype() : ft.naam)
                    + "', die geen relatie van twee rollen is: " + NOG_NIET);
        String relatie = Namen.norm(ft.naam);
        boolean nieuwIsEerste = Namen.norm(ft.rollen.get(0).naam()).equals(Namen.norm(k.fwdRol()));
        String naam = oc.naam();
        String regelnaam = model.geëmitteerdeRegelNaam(naam);
        Supplier<Lezingen> nieuwe = () -> new Lezingen(subject).geldig(oc.geldigheid());
        Lezingen lez = nieuwe.get();
        Set<Integer> gelezen = lez.volg();
        Term toets = oc.voorwaarde() == null ? null : voorwaarde(oc.voorwaarde(), lez);
        boolean kanFalen = lez.kanFalen();
        lez.stop();
        Set<Integer> gebruik = lez.volg();
        List<Initialisatie> inits = new ArrayList<>();
        for (ResolvedObjectCreatie.Toekenning t : oc.toekenningen()) {
            if (t.attribuut() == null || t.waarde() == null)
                throw new Weigering("een ledeninitialisatie zonder lid of waarde");
            // a characteristic is true or false (WF-027): the reference interpreter faults on a value of another type,
            // and the consequence below would read it as not true and leave the characteristic unset
            Datatype kenmerkwaarde = t.kenmerk() ? eff.typeVan(t.waarde()) : null;
            if (kenmerkwaarde != null && !kenmerkwaarde.isBoolean() && !kenmerkwaarde.isOnbekend())
                throw new Weigering("een kenmerkinitialisatie van '" + t.attribuut() + "' met een waarde van type "
                        + kenmerkwaarde.naam() + " ('" + snippet(t.waarde().bron()) + "'): een kenmerk is waar of onwaar"
                        + " (WF-027), en de referentie-interpreter faalt op zo'n waarde");
            String deling = deling(t.waarde());
            if (deling != null)
                throw new Weigering("een deling in een ledeninitialisatie ('" + deling + "'), waarvan deze tak de §6.5-"
                        + "deponering niet maakt: " + NOG_NIET);
            String wortel = wortel(t.waarde());
            if (wortel != null)
                throw new Weigering("een wortel in een ledeninitialisatie ('" + wortel + "'), waarvan deze tak de §6.6-"
                        + "deponering niet maakt: " + NOG_NIET);
            Lid lid = lid(nieuw, t.attribuut(), t.kenmerk());
            Term w = waarde(t.waarde(), lez);
            if (!t.kenmerk())
                w = herschaald(w, eff.eenheidVan(t.waarde()), lid.datatype().eenheidOf(), "een §4 schrijfgrens naar het lid");
            inits.add(new Initialisatie(lid, gevolgwaarde(w)));
        }
        lez.stop();
        int stratum = stratum(oc, naam);
        int fase = lez.creatiefase(stratum);
        gebruik.add(fase);
        int herkomst = lez.herkomst();
        gebruik.add(herkomst);
        List<RuleItemBuilder<?>> items = lez.linkerkant(voorwaarderol(kanFalen), toets, gelezen, oc.voorwaarde(),
                creatiebewaking(naam, lez, fase));
        // the fault this creation stated at a making for the subject blocks it there: the driver counts every firing of a
        // creation rule as a making, so a creation that fired again on its own fault would never let a stratum end
        Variable<Fout> eigenFout = DSL.declarationOf(Fout.class, "$eigen_fout");
        items.add(DSL.not(zelfde(PatternDSL.pattern(eigenFout).expr(id("eigen-fout"), x -> x.regel().equals(naam)),
                "eigen-fout-onderwerp", lez.s, Fout::onderwerp)));
        if (k.eenOpEen()) {
            // an instance linked toward the subject in the input world blocks the creation (ch. 5.5)
            Function<Object, Object> voorwaarts = Accessors.getter(bonenklasse(subject), k.fwdRef());
            lez.onderwerp.expr(id("geen-gekoppelde-instantie"), o -> voorwaarts.apply(o) == null);
        }
        Class<?> klasse = bonenklasse(nieuw);
        String subjecttype = subject.naam;
        // the setter of each attribute an initialisation sets, null for a characteristic
        List<BiConsumer<Object, Object>> zetters = new ArrayList<>();
        for (Initialisatie i : inits)
            zetters.add(i.lid().kenmerk() ? null : Accessors.setter(i.lid().klasse(), i.lid().naam()));
        items.add(gevolg(lez, posities(gebruik), (drools, onderwerp, w) -> {
            // the budget first: a mint past it is the fault at the making, before anything is made
            int diepte = diepte(naam, stratum, subjecttype, (List<?>) w[herkomst]);
            Object[] waarden = new Object[inits.size()];
            RuntimeException fout = null;
            for (int i = 0; i < inits.size() && fout == null; i++) {
                try {
                    waarden[i] = collapse(inits.get(i).waarde().van(w));
                } catch (RuntimeException e) {
                    if (!Rekenfout.isEvaluatiefout(e)) throw e;
                    fout = Rekenfout.benoem(e, regelnaam);
                }
            }
            // a creation fires on a fixpoint of the rule layer, so a fault in an initialisation is the session's at the
            // making, as in the reference interpreter, which then makes nothing: no object, no mark and no link that
            // another form could read and fault on. The fault is stated like everything a creation makes, and the
            // rule's guard on it keeps the creation from firing again in a later generation
            if (fout != null) {
                drools.insert(new Fout(onderwerp, naam, fout));
                return;
            }
            int f = (Integer) w[fase];
            Object object = nieuweBoon(klasse);
            // the member values are the input of the new object, as the reference interpreter reads them: they go on
            // the new bean before it enters working memory, so no rule ever saw it without them and no bean in the
            // session changes; a writer of the member then displaces them as it displaces case input (ch. 5.5, 7.8)
            for (int i = 0; i < inits.size(); i++)
                if (zetters.get(i) != null && waarden[i] != null) zetters.get(i).accept(object, waarden[i]);
            drools.insert(object);
            drools.insert(new Gemaakt(naam, onderwerp, object, f, stratum, diepte));
            drools.insert(nieuwIsEerste ? new Koppeling(relatie, object, onderwerp, f)
                    : new Koppeling(relatie, onderwerp, object, f));
            for (int i = 0; i < inits.size(); i++)
                if (zetters.get(i) == null && Boolean.TRUE.equals(waarden[i]))
                    drools.insert(new HeeftKenmerk(object, inits.get(i).lid().sleutel()));
        }, lez.variabelen.size()));
        for (Initialisatie i : inits) afgeleid.put(i.lid().sleutel(), i.lid());
        lez.vastleggen();
        creatieregels.add(regelnaam);
        creatiestrata.add(stratum);
        List<Rule> uit = new ArrayList<>();
        uit.add(PatternDSL.rule(PKG, regelnaam).attribute(Rule.Attribute.AGENDA_GROUP, OBJECTCREATIE)
                .build(items.toArray(new RuleItemBuilder[0])));
        if (kanFalen) uit.add(voorwaardefoutregel(naam, regelnaam, nieuwe, oc.voorwaarde(), null));
        Rule deponering = deponeerregel(naam, regelnaam, nieuwe, oc.voorwaarde(), null);
        if (deponering != null) uit.add(deponering);
        Rule zonderDatum = geldigheidsfout(regelnaam, naam, oc.geldigheid());
        if (zonderDatum != null) uit.add(zonderDatum);
        return uit;
    }

    /** {@link #typenpaarrelatie(Objecttype, Objecttype, FactModelGenerator.Creatiekoppeling)} for the object creation
     *  {@code oc} without a {@code connectingRole} binding, before any form lowers; null where the shared decision does
     *  not link the creation, which {@link #objectCreatie} then refuses by name. */
    private RegelSpraakModel.Feittype typenpaarrelatie(ResolvedObjectCreatie oc) {
        Objecttype nieuw = oc.nieuwType(), subject = oc.subject();
        if (oc.naam() == null || nieuw == null || subject == null) return null;
        FactModelGenerator.Creatiekoppeling k = FactModelGenerator.creatiekoppeling(model, nieuw, subject, null, oc.naam());
        return k.dubbelzinnig() != null || k.weigering() != null ? null : typenpaarrelatie(subject, nieuw, k);
    }

    /**
     * The connecting relation of an object creation without a {@code connectingRole} binding (RUNTIME-SEMANTICS ch.
     * 5.5): the fact type of the two roles that the shared decision {@code k} derived from the type pair, the role the
     * made object plays ({@code fwdRol}) filled by the created type and the other ({@code backRol}) by the subject's, and
     * for a subject of the created type a reciprocal fact type of that one role. The fillers are compared exactly, as
     * {@link Kringen} derives the back-link for the strata: where the decision reached a role through a supertype of
     * either end, the strata would not see the creation write the relation, so a creation that reads it could fire
     * before this one has made its links. Null then, and {@link #objectCreatie} refuses the creation by name. Two fact
     * types cannot match where the decision linked: its roles are unique among the roles the pair reaches through any
     * supertype, of which the exact ones are a part.
     */
    private RegelSpraakModel.Feittype typenpaarrelatie(Objecttype subject, Objecttype nieuw,
                                                        FactModelGenerator.Creatiekoppeling k) {
        RegelSpraakModel.Feittype gevonden = null;
        for (RegelSpraakModel.Feittype ft : model.feittypen.values()) {
            List<RegelSpraakModel.Rol> r = ft.rollen;
            boolean past = r.size() == 2
                    ? vult(r.get(0), k.fwdRol(), nieuw) && vult(r.get(1), k.backRol(), subject)
                      || vult(r.get(1), k.fwdRol(), nieuw) && vult(r.get(0), k.backRol(), subject)
                    : r.size() == 1 && ft.wederkerig && subject == nieuw && vult(r.get(0), k.fwdRol(), nieuw);
            if (!past) continue;
            if (gevonden != null)
                throw new IllegalStateException("exec-model: de terugkoppeling ('" + k.backRol() + "' en '" + k.fwdRol()
                        + "') van een " + subject.naam + " naar een " + nieuw.naam + " ligt in twee feittypen, '"
                        + gevonden.naam + "' en '" + ft.naam + "', waar het gedeelde besluit haar rollen uniek vond");
            gevonden = ft;
        }
        return gevonden;
    }

    /** Whether role {@code r} carries the name {@code naam} and is filled by exactly {@code type}. */
    private static boolean vult(RegelSpraakModel.Rol r, String naam, Objecttype type) {
        return naam != null && Namen.norm(r.naam()).equals(Namen.norm(naam))
                && type.naam.equalsIgnoreCase(r.objecttypeNaam());
    }

    /** The guard of an object creation beside its condition: no {@link Gemaakt} of creation {@code naam} for the
     *  subject, so that it makes one object per subject, and no {@link Gemaakt} that made the subject itself in the
     *  current phase, bound at {@code fase} ({@link Lezingen#creatiefase}), so that an object waits for the next phase,
     *  after the rule layer has reached a fixpoint over it. */
    private List<RuleItemBuilder<?>> creatiebewaking(String naam, Lezingen lez, int fase) {
        @SuppressWarnings("unchecked")
        Variable<Object> f = (Variable<Object>) lez.variabelen.get(fase);
        Variable<Gemaakt> eigen = DSL.declarationOf(Gemaakt.class, "$gemaakt_" + id("creatie"));
        Variable<Gemaakt> nieuw = DSL.declarationOf(Gemaakt.class, "$nieuw_" + id("creatie"));
        return List.of(
                DSL.not(zelfde(PatternDSL.pattern(eigen).expr(id("gemaakt"), g -> g.regel().equals(naam)),
                        "gemaakt-onderwerp", lez.s, Gemaakt::onderwerp)),
                DSL.not(zelfde(PatternDSL.pattern(nieuw), "nieuw-object", lez.s, Gemaakt::object)
                        .expr(id("nieuw-in-deze-fase"), f, (g, n) -> g.fase() == (Integer) n)));
    }

    /** The creation depth of the object that the object creation {@code naam} of stratum {@code stratum} makes for a
     *  subject of declared type {@code type}, whose own marks are {@code herkomst}, the {@link Gemaakt} facts whose object
     *  is the subject ({@link Lezingen#herkomst}): none for an object of the input, one for an object a creation made.
     *  It is one more than the subject's depth in this stratum, where an object of the input and one a lower stratum made
     *  stand at 0, since a lower stratum is complete before this one fires (RUNTIME-SEMANTICS ch. 5.5, 6.3). Past
     *  {@link #CREATIEBUDGET} it throws {@link Creatiebudget}. A subject that a higher stratum made cannot occur: a
     *  creation reads its subject's population strictly (ch. 6.1), so it sits at or above every creation that makes its
     *  subjects; such a subject, or an object with two marks, is a fault of this transpiler and throws as one. */
    static int diepte(String naam, int stratum, String type, List<?> herkomst) {
        if (herkomst.size() > 1)
            throw new IllegalStateException("exec-model: objectcreatie \"" + naam + "\" ontmoet een onderwerp met "
                    + herkomst.size() + " merktekens van een creatie, waar een object één keer gemaakt wordt: " + herkomst);
        int diepte = 1;
        if (!herkomst.isEmpty()) {
            Gemaakt g = (Gemaakt) herkomst.get(0);
            if (g.stratum() > stratum)
                throw new IllegalStateException("exec-model: objectcreatie \"" + naam + "\" van stratum " + stratum
                        + " ontmoet een onderwerp dat een creatie van stratum " + g.stratum() + " maakte, terwijl een"
                        + " creatie de populatie van haar onderwerp strikt leest en dus in het stratum van elke maker van"
                        + " haar onderwerpen staat of erboven (RUNTIME-SEMANTICS ch. 6.1, 6.3)");
            if (g.stratum() == stratum) diepte = g.diepte() + 1;
        }
        if (diepte > CREATIEBUDGET)
            throw new Creatiebudget("creation budget exhausted: objectcreatie \"" + naam + "\" van stratum " + stratum
                    + " zou een object maken op aanmaakdiepte " + diepte + ", voor een " + type + " op diepte "
                    + (diepte - 1) + " — een keten van aanmaken die niet stopt is een fout, nooit een stille afkapping"
                    + " (RUNTIME-SEMANTICS ch. 5.5)");
        return diepte;
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

    /** A new bean of {@code klasse}, with every field unset: what a creation initialises is a fact beside it. */
    private static Object nieuweBoon(Class<?> klasse) {
        try {
            return klasse.getConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("kon geen " + klasse.getSimpleName() + " maken", e);
        }
    }

    /**
     * A fact creation (RUNTIME-SEMANTICS ch. 5.5, IR-SPEC ch. 3.4), a rule of the agenda group {@link #FEITCREATIE}: the
     * anchor pattern with the reads of the guard, the {@link Creatiefase} the driver opened, the guard as the test where
     * it is about the anchor, the path from the anchor to the target as a chain of patterns ({@link #keten}), over the
     * {@link Koppeling} facts of a relation a form creates, none of the current phase, and over the reference fields of
     * an input relation, no link of the created relation between anchor and target yet, and a consequence that inserts
     * that link, stated and carrying the phase. The creation is idempotent per link, as in the reference interpreter,
     * where an already present link is a write that changes nothing: a target that two routes or two creations reach,
     * or that the input links already, gets no second link, and an anchor gets the links of targets that become
     * reachable in a later generation. A guard that is a characteristic check hosted on the target, and is the whole
     * condition, filters the targets instead (ch. 5.5, WF-109); hosted on the target anywhere else it is refused, as the
     * other branches refuse it. A second distinct filler of a to-one role is a fault ({@link #koppelingregels}).
     */
    private List<Rule> feitCreatie(ResolvedFeitCreatie fc) {
        if (fc.naam() == null) throw new Weigering("een feitcreatie zonder naam (WF-107)");
        Objecttype anker = fc.anker(), doel = fc.doel();
        if (anker == null || doel == null || fc.feittype() == null || fc.joinRol() == null || fc.ankerRol() == null)
            throw new Weigering("een feitcreatie zonder opgelost anker, doel, feittype of pad");
        RegelSpraakModel.Feittype ft = feittype(fc.feittype());
        if (ft == null || ft.rollen.size() != 2 || ft.wederkerig)
            throw new Weigering("een feitcreatie van de relatie '" + fc.feittype() + "', die geen relatie van twee rollen"
                    + " is: " + NOG_NIET);
        String relatie = Namen.norm(ft.naam);
        boolean ankerIsEerste = Namen.norm(ft.rollen.get(0).naam()).equals(Namen.norm(fc.ankerRol()));
        Conditie c = fc.voorwaarde();
        Conditie.Kenmerk doelcheck = c instanceof Conditie.Kenmerk k && k.eigenaar() != null && !Gastheer.hostOp(k, anker)
                && Gastheer.hostOp(k, doel) && !navigatieBindingen.containsKey(k) && k.periode() == null ? k : null;
        if (doelcheck == null && c != null && Gastheer.doelgastheerBuitenBereik(c, doel, navigatieBindingen::containsKey))
            throw new Weigering(Gastheer.DOELPOGING_GENEST);
        String regelnaam = model.geëmitteerdeRegelNaam(fc.naam());
        Conditie ankerguard = doelcheck == null ? c : null;
        Supplier<Lezingen> nieuw = () -> new Lezingen(anker).geldig(fc.geldigheid());
        Lezingen lez = nieuw.get();
        Set<Integer> gelezen = lez.volg();
        Term toets = ankerguard == null ? null : voorwaarde(ankerguard, lez);
        boolean kanFalen = lez.kanFalen();
        lez.stop();
        int stratum = stratum(fc, fc.naam());
        int fase = lez.creatiefase(stratum);
        List<RuleItemBuilder<?>> items = lez.linkerkant(voorwaarderol(kanFalen), toets, gelezen, ankerguard);
        List<RolBindingRef> stappen = new ArrayList<>();
        for (ResolvedFeitCreatie.Tussenstap t : fc.tussenstappen()) stappen.add(t.rolBinding());
        stappen.add(fc.joinRol());
        List<Hop> hops = new ArrayList<>();
        Objecttype cur = anker;
        for (RolBindingRef stap : stappen) {
            Hop h = stap(stap, cur, "het pad van de feitcreatie '" + fc.naam() + "'");
            hops.add(h);
            cur = h.naar();
        }
        @SuppressWarnings("unchecked")
        Variable<Object> f = (Variable<Object>) lez.variabelen.get(fase);
        PatternDSL.PatternDef<Object> doelpatroon = keten(hops, lez.s, items, f);
        Variable<Object> doelvar = doelpatroon.getFirstVariable();
        if (doelcheck != null) doelbewaking(doelcheck, doelpatroon, items);
        Variable<Koppeling> bestaand = DSL.declarationOf(Koppeling.class, "$bestaand_" + id("feitcreatie"));
        items.add(DSL.not(zelfde(zelfde(PatternDSL.pattern(bestaand).expr(id("bestaand"), x -> x.relatie().equals(relatie)),
                "bestaand-anker", lez.s, x -> ankerIsEerste ? x.eerste() : x.tweede()),
                "bestaand-doel", doelvar, x -> ankerIsEerste ? x.tweede() : x.eerste())));
        items.add(DSL.on(lez.s, doelvar, f).execute((Block4<org.drools.model.Drools, Object, Object, Object>)
                (drools, a, d, n) -> drools.insert(ankerIsEerste ? new Koppeling(relatie, a, d, (Integer) n)
                        : new Koppeling(relatie, d, a, (Integer) n))));
        lez.vastleggen();
        creatieregels.add(regelnaam);
        creatiestrata.add(stratum);
        List<Rule> uit = new ArrayList<>();
        uit.add(PatternDSL.rule(PKG, regelnaam).attribute(Rule.Attribute.AGENDA_GROUP, FEITCREATIE)
                .build(items.toArray(new RuleItemBuilder[0])));
        if (kanFalen) uit.add(voorwaardefoutregel(fc.naam(), regelnaam, nieuw, ankerguard, null));
        Rule deponering = deponeerregel(fc.naam(), regelnaam, nieuw, ankerguard, null);
        if (deponering != null) uit.add(deponering);
        Rule zonderDatum = geldigheidsfout(regelnaam, fc.naam(), fc.geldigheid());
        if (zonderDatum != null) uit.add(zonderDatum);
        return uit;
    }

    /** A step of a fact creation's path over {@code stap} from {@code van}: over a relation a form creates by its
     *  {@link Koppeling} facts, else by the reference field of the to-one end, on the near bean where the far role is
     *  to-one and on the far bean where it is to-many. Two to-many roles have no field in the bean model (§13.3.9). */
    private Hop stap(RolBindingRef stap, Objecttype van, String wat) {
        RegelSpraakModel.Rol[] paar = stap == null ? null : FactModelGenerator.gebondenRolpaar(model, stap);
        if (paar == null || paar[0] == paar[1])
            throw new Weigering(wat + ": een stap die geen rolpaar van twee rollen noemt");
        Objecttype naar = model.objecttypeOpNaam(paar[0].objecttypeNaam());
        if (naar == null) throw new Weigering(wat + ": de rol '" + paar[0].naam() + "' heeft geen gedeclareerde vuller");
        Hop gecreeerd = koppelingshop(stap, van, naar);
        if (gecreeerd != null) return gecreeerd;
        if (!paar[0].meervoudig()) {
            if (!FactModelGenerator.heeftReferentieVeld(model, van, paar[0].naam(), naar))
                throw new Weigering(wat + ": " + FactModelGenerator.geenReferentieveldReden(model, van, paar[0].naam(), naar)
                        + " (§13.3.9 opslagrichting)");
            return new Hop(van, naar, Accessors.getter(bonenklasse(van), paar[0].naam()), true, null, false);
        }
        if (!paar[1].meervoudig()) {
            if (!FactModelGenerator.heeftReferentieVeld(model, naar, paar[1].naam(), van))
                throw new Weigering(wat + ": " + FactModelGenerator.geenReferentieveldReden(model, naar, paar[1].naam(), van)
                        + " (§13.3.9 opslagrichting)");
            return new Hop(van, naar, Accessors.getter(bonenklasse(naar), paar[1].naam()), false, null, false);
        }
        throw new Weigering(wat + ": de stap over '" + stap.feittype() + "' heeft twee rollen naar veel, waarvoor het"
                + " beanmodel geen veld heeft (§13.3.9 opslagrichting)");
    }

    /** The guard of a fact creation on each target ({@code check}, hosted on a type the target satisfies): the target
     *  must be of the host's type, and its characteristic must hold as the check asks, read through its standing where a
     *  form writes it and off the bean otherwise. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private void doelbewaking(Conditie.Kenmerk check, PatternDSL.PatternDef<Object> doelpatroon,
                              List<RuleItemBuilder<?>> items) {
        Objecttype host = check.eigenaar();
        Lid lid = lid(host, check.kenmerk(), true);
        Class<?> hostklasse = bonenklasse(host);
        boolean gewenst = !check.negatie();
        doelpatroon.expr(id("doel-gastheer"), d -> hostklasse.isInstance(d));
        ElementSleutel sleutel = lid.sleutel();
        if (schrijvers.containsKey(sleutel)) {
            Variable<KenmerkStand> st = DSL.declarationOf(KenmerkStand.class, "$doelstand_" + id("doel"));
            Variable<Object> doel = doelpatroon.getFirstVariable();
            items.add(zelfde(PatternDSL.pattern(st).expr(id("doelstand"), x -> x.kenmerk().equals(sleutel)),
                    "doelstand-doel", doel, KenmerkStand::onderwerp)
                    .expr(id("doelstand-waar"), x -> x.waar() == gewenst));
            gelezen.putIfAbsent(sleutel, lid);
        } else {
            Function<Object, Object> getter = Accessors.getter(lid.klasse(), lid.naam());
            doelpatroon.expr(id("doel-kenmerk"), d -> Boolean.TRUE.equals(getter.apply(d)) == gewenst);
        }
    }

    /**
     * The rules every relation a form creates needs, whether a form reads it or not: a rule per to-one end with a
     * reference field that inserts the links the case input carries as {@link Koppeling} facts, so that the readers of
     * the relation see the input and the created links alike, and a rule per to-one role that faults a second distinct
     * filler of it: one object filling the other role with two links to different objects, the fault of ch. 5.5,
     * never an overwrite.
     */
    private List<Rule> koppelingregels(String relatie) {
        RegelSpraakModel.Feittype ft = feittype(relatie);
        if (ft == null || ft.rollen.size() != 2 || ft.wederkerig) return List.of();
        RegelSpraakModel.Rol r0 = ft.rollen.get(0), r1 = ft.rollen.get(1);
        Objecttype t0 = model.objecttypeOpNaam(r0.objecttypeNaam()), t1 = model.objecttypeOpNaam(r1.objecttypeNaam());
        if (t0 == null || t1 == null) return List.of();
        List<Rule> uit = new ArrayList<>();
        if (!r1.meervoudig() && FactModelGenerator.heeftReferentieVeld(model, t0, r1.naam(), t1))
            uit.add(invoerkoppeling(relatie, t0, r1.naam(), true));
        if (!r0.meervoudig() && FactModelGenerator.heeftReferentieVeld(model, t1, r0.naam(), t0))
            uit.add(invoerkoppeling(relatie, t1, r0.naam(), false));
        if (!r1.meervoudig()) uit.add(tweedeVulling(ft, relatie, true, r1.naam(), t0));
        if (!r0.meervoudig()) uit.add(tweedeVulling(ft, relatie, false, r0.naam(), t1));
        return uit;
    }

    /** The links of {@code relatie} the reference field {@code veld} of the beans of {@code houder} carries, the holder
     *  being the link's first end where {@code houderIsEerste}. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private Rule invoerkoppeling(String relatie, Objecttype houder, String veld, boolean houderIsEerste) {
        Class<?> klasse = bonenklasse(houder);
        Function<Object, Object> lees = Accessors.getter(klasse, veld);
        Variable<Object> h = (Variable<Object>) DSL.declarationOf(klasse, "$houder");
        Variable<Object> ander = DSL.declarationOf(Object.class, "$ander");
        return PatternDSL.rule(PKG, "koppeling " + relatie + " uit de invoer: " + houder.naam + "." + veld).build(
                PatternDSL.pattern(h).expr(id("koppeling-invoer"), o -> lees.apply(o) != null).bind(ander, o -> lees.apply(o)),
                DSL.on(h, ander).execute((Block3<org.drools.model.Drools, Object, Object>) (drools, a, b) ->
                        drools.logicalInsert(houderIsEerste ? new Koppeling(relatie, a, b, 0)
                                : new Koppeling(relatie, b, a, 0))));
    }

    /** The fault of two links of {@code relatie} from one object to two different fillers of the to-one role
     *  {@code rol}: the near object is the links' first end where {@code nabijIsEerste}. */
    private Rule tweedeVulling(RegelSpraakModel.Feittype ft, String relatie, boolean nabijIsEerste, String rol,
                               Objecttype nabijType) {
        Variable<Koppeling> een = DSL.declarationOf(Koppeling.class, "$een");
        Variable<Koppeling> twee = DSL.declarationOf(Koppeling.class, "$twee");
        Variable<Object> nabij = DSL.declarationOf(Object.class, "$nabij");
        String zin = "de naar-één-rol \"" + rol + "\" van een " + nabijType.naam + " heeft twee verschillende vullingen;"
                + " een tweede vulling is een fout, geen overschrijving (RUNTIME-SEMANTICS ch. 5.5)";
        return PatternDSL.rule(PKG, "koppeling " + relatie + ": tweede vulling van " + rol).build(
                PatternDSL.pattern(een).expr(id("vulling"), x -> x.relatie().equals(relatie))
                        .bind(nabij, x -> nabijIsEerste ? x.eerste() : x.tweede()),
                zelfde(PatternDSL.pattern(twee).expr(id("tweede-vulling"), x -> x.relatie().equals(relatie)),
                        "tweede-vulling-nabij", een, x -> nabijIsEerste ? x.eerste() : x.tweede(),
                        y -> nabijIsEerste ? y.eerste() : y.tweede())
                        .expr(id("tweede-vulling-ander"), een, (x, y) -> nabijIsEerste
                                ? x.tweede() != y.tweede() : x.eerste() != y.eerste()),
                DSL.on(nabij).execute((Block2<org.drools.model.Drools, Object>) (drools, n) ->
                        drools.logicalInsert(new Fout(n, ft.naam, new IllegalStateException(zin)))));
    }

    // ---------------- prioritized defaults ----------------

    /** The name of an effective stack, as the destructive lowering named the rule of the group: the kind-discriminated
     *  target on the stack's owner, so two types that share one inherited target name two stacks. */
    private static String stapelnaam(Stapels.Stapel st) {
        PrioritizedDefaults l0 = st.leden().get(0);
        return "defaults " + (l0.isKenmerk() ? "K:" : "A:") + st.eigenaar().naam + "." + l0.doel();
    }

    /**
     * One default of a stack where the lowering places it: the level it is read on, its chain and its position there,
     * a label for the names of its rules, the key of a conflict among its exception groups (RUNTIME-SEMANTICS ch. 7.9:
     * the target form and the default indices from the root, member indices left out), and, where it has exception
     * groups, their chains, numbered consecutively from {@code eersteLid}.
     */
    private record Knoop(Default d, Objecttype niveau, int keten, int positie, String label, String sleutel,
                         List<List<Knoop>> leden, int eersteLid, Geldigheid geldigheid) {
        boolean heeftLeden() { return leden != null; }
    }

    /** What a decision or a reconciliation does with its outcome, from the values its rule bound. */
    @FunctionalInterface
    private interface Afloop {
        void voer(org.drools.model.Drools drools, Object onderwerp, Object waarde);
    }

    /** What the rules of one stack share while they are built. */
    private static final class Stapelbouw {
        final String naam;
        final Lid lid;
        final boolean kenmerk;
        /** The bean class of the stack's owner: the stack runs on instances of exactly that type (ch. 7.10). */
        final Class<?> klasse;
        /** The bare ch. 7.2 conflict key, the target on the type that declares it. */
        final String doelvorm;
        final List<Lezingen> lezingen = new ArrayList<>();
        final Set<String> stuurfeiten = new LinkedHashSet<>();
        int volgendeKeten;

        Stapelbouw(String naam, Lid lid, boolean kenmerk, Class<?> klasse, String doelvorm, int wortelketens) {
            this.naam = naam;
            this.lid = lid;
            this.kenmerk = kenmerk;
            this.klasse = klasse;
            this.doelvorm = doelvorm;
            this.volgendeKeten = wortelketens;
        }
    }

    /**
     * The rules of one effective stack (RUNTIME-SEMANTICS ch. 7.1-7.3, 7.8-7.10), each default lowered on its own
     * level and on instances of exactly the stack's owner, deciding by logical control facts ({@link Stuurfeit}) where
     * the destructive lowering walked the stack in one consequence. One chain without exception groups
     * takes the simple form ({@link #eenvoudigeVorm}); exception groups, or more than one group at the root, the
     * general form ({@link #algemeneVorm}). A deciding default writes the target: a {@link Waarde} with the stack as the
     * writer, which replaces the input on an attribute, or a {@link HeeftKenmerk} for an attribution, beside which the
     * input on a characteristic stands; a conflict writes nothing and deposits the §9.5 inconsistency under its key.
     *
     * <p>Refused by name, as the destructive lowering refused them: a characteristic target that shares its name with
     * an attribute, a group with a dated validity, an initialisation default on a characteristic, a deciding default
     * without a justification; a target on a timeline or with dimensions through {@link #lid}. Refused besides: a
     * division anywhere in a default ({@link #deling(Conditie, Expr)}).
     */
    private List<Rule> stapelregels(Stapels.Stapel st) {
        PrioritizedDefaults l0 = st.leden().get(0);
        Objecttype eigenaar = st.eigenaar();
        boolean kenmerk = l0.isKenmerk();
        String doel = l0.doel();
        if (kenmerk && eigenaar.attribuut(doel) != null)
            throw new Weigering("een kenmerkdoel met de naam van een attribuut ('" + doel + "' van " + eigenaar.naam
                    + "): de bean draagt één veld per naam en het attribuut gaat voor, zodat het kenmerk niet te schrijven"
                    + " en niet te onderscheiden is");
        Lid lid = lid(eigenaar, doel, kenmerk);
        Objecttype declarant = eigenaar.declarantVan(doel);
        Stapelbouw b = new Stapelbouw(stapelnaam(st), lid, kenmerk, bonenklasse(eigenaar),
                (declarant != null ? declarant.naam : l0.onderwerp()) + "." + doel, st.leden().size());
        List<List<Knoop>> wortel = new ArrayList<>();
        // a group with a dated validity gates its own chain, the exception groups below its defaults included: where
        // the window does not admit the date the group decides nothing (RUNTIME-SEMANTICS ch. 8.2)
        for (int m = 0; m < st.leden().size(); m++)
            wortel.add(keten(b, st.leden().get(m).defaults(), m, null, eigenaar, List.of(),
                    st.leden().size() == 1 ? "" : m + ":", st.leden().get(m).geldigheid()));
        boolean algemeen = wortel.size() > 1 || wortel.stream().flatMap(List::stream).anyMatch(Knoop::heeftLeden);
        List<Rule> regels = new ArrayList<>(algemeen ? algemeneVorm(b, wortel) : eenvoudigeVorm(b, wortel.get(0)));
        if (st.leden().stream().anyMatch(l -> l.geldigheid() != null && !l.geldigheid().altijd()))
            regels.add(stapelGeldigheidsfout(b));
        // registered only once every default of the stack lowered, as a single rule registers its reads
        for (Lezingen lez : b.lezingen) lez.vastleggen();
        afgeleid.put(lid.sleutel(), lid);
        defaultsgroepen.add(new Defaultsgroep(b.naam, algemeen, regels.size(), List.copyOf(b.stuurfeiten)));
        return regels;
    }

    /** The rule that faults every object of the stack's owner, of exactly its type, in a session without an evaluation
     *  date, where a group of the stack has a dated validity: the reference interpreter asks the validity of a group when
     *  it evaluates the stack on an object, and faults there (RUNTIME-SEMANTICS ch. 8.1), so an owner without objects
     *  faults nothing. */
    private Rule stapelGeldigheidsfout(Stapelbouw b) {
        rekendatumGlobal();
        String naam = b.naam;
        Class<?> klasse = b.klasse;
        @SuppressWarnings("unchecked")
        Variable<Object> s = (Variable<Object>) DSL.declarationOf(klasse, "$onderwerp");
        Variable<Rekendatum> r = DSL.declarationOf(Rekendatum.class, "$rekendatum");
        return PatternDSL.rule(PKG, naam + ": geen rekendatum").build(
                PatternDSL.pattern(s).expr(id("concreet-type"), o -> o.getClass() == klasse),
                PatternDSL.pattern(r).expr(id("geen-rekendatum"), x -> x.datum() == null),
                DSL.on(s).execute((Block2<org.drools.model.Drools, Object>) (drools, o) -> drools.logicalInsert(
                        new Fout(o, naam, new Rekenfout(Rekenfout.GELDIGHEID_ZONDER_DATUM).inDeRegel(naam)))));
    }

    /** The knots of one chain, {@code id}, with its defaults' levels: a root default's own ({@link Stapels#niveau}), an
     *  exception group's defaults on the level of the default that carries them ({@code drager}); every knot carries
     *  the validity of the group at the root of its chain. */
    private List<Knoop> keten(Stapelbouw b, List<Default> ds, int id, Objecttype drager, Objecttype eigenaar,
                              List<Integer> pad, String voorvoegsel, Geldigheid geldigheid) {
        List<Knoop> uit = new ArrayList<>();
        for (int i = 0; i < ds.size(); i++) {
            Default d = ds.get(i);
            Objecttype niveau = drager != null ? drager : stapels.niveau(d, eigenaar);
            controleer(b, d);
            List<Integer> eigenPad = new ArrayList<>(pad);
            eigenPad.add(i);
            String label = voorvoegsel + i;
            StringBuilder sleutel = new StringBuilder(b.doelvorm);
            for (int p : eigenPad) sleutel.append('/').append(p);
            List<List<Knoop>> leden = null;
            int eerste = -1;
            if (d.excepties() != null) {
                List<dev.uchar.regelspraak.ir.ExceptionGroup> groepen = d.excepties().leden();
                eerste = b.volgendeKeten;
                b.volgendeKeten += groepen.size();
                leden = new ArrayList<>();
                for (int m = 0; m < groepen.size(); m++)
                    leden.add(keten(b, groepen.get(m).defaults(), eerste + m, niveau, eigenaar, eigenPad,
                            label + "." + m + ".", geldigheid));
            }
            uit.add(new Knoop(d, niveau, id, i, label, sleutel.toString(), leden, eerste, geldigheid));
        }
        return uit;
    }

    /** The refusals per default. */
    private void controleer(Stapelbouw b, Default d) {
        if (d.initialisatie() && b.kenmerk)
            throw new Weigering("een initialisatiedefault op een kenmerkdoel ('" + b.lid.naam() + "' van "
                    + b.lid.declarant().naam + "): een kenmerk kent geen initialisatie (WF-075)");
        if (!d.initialisatie() && d.voorwaarde() == null && d.heeftEigenInhoud())
            throw new Weigering("een beslissende default zonder rechtvaardiging ('" + (d.bron() != null ? d.bron()
                    : "zonder bron") + "', doel '" + b.lid.naam() + "')");
        String deling = deling(d.voorwaarde(), d.consequentie());
        if (deling != null)
            throw new Weigering("een deling in een default ('" + deling + "'): deze tak rekent een default uit zodra wat"
                    + " hij leest staat, ook een die een hogere default verdringt; een deling door nul daar is een fout op"
                    + " een tussenstand, die deze tak intrekt zodra de hogere default beslist: of zo'n fout telt, wacht op"
                    + " een normbesluit (RUNTIME-SEMANTICS ch. 5.2, 7.1)");
        String wortel = wortel(d.voorwaarde(), d.consequentie());
        if (wortel != null)
            throw new Weigering("een wortel in een default ('" + wortel + "'), waarvan deze tak de §6.6-deponering niet"
                    + " maakt: een default deponeert alleen waar hij beslist (RUNTIME-SEMANTICS ch. 7.1)");
    }

    /** The snippet of the first division in {@code voorwaarde} or {@code waarde}, or null: in the justification or the
     *  consequence of a default, and in the condition or the value of an initialisation beside another writer, which
     *  ch. 7.7 reads as the lowest default ({@link #initialisatieRegel}). The destructive lowering refused a division
     *  below the top of a consequence, whose deposit would persist; here the evaluation itself is speculative, as a
     *  default is evaluated whenever its reads stand. A division by zero in a default another one displaces is a fault
     *  on an intermediate state, which this branch would withdraw ({@link Fout}); whether such a fault counts waits on
     *  a decision of the norm, so every division is refused until then. Since none passes, the rules of a default raise
     *  no division fault ({@link #eigenRegel}). The walk enters every expression and condition the forms carry, a
     *  {@code Let}'s value and body, both branches of an {@code IfThenElse} and its condition, a rounding, a clamp's
     *  bounds and a fold's argument, fallback and filter among them, so a division under one of them is found too. */
    private static String deling(Conditie voorwaarde, Expr waarde) {
        return eerste(voorwaarde, waarde, TmsTranspiler::isDeling);
    }

    private static String deling(Expr e) {
        return eerste(null, e, TmsTranspiler::isDeling);
    }

    /** The snippet of the first square root in {@code waarde}, or null: a place of the §6.6 deposit, which a member
     *  initialisation and a default do not make. */
    private static String wortel(Expr e) {
        return eerste(null, e, TmsTranspiler::isWortel);
    }

    private static String wortel(Conditie voorwaarde, Expr waarde) {
        return eerste(voorwaarde, waarde, TmsTranspiler::isWortel);
    }

    private static boolean isDeling(Expr e) {
        return e instanceof Expr.Binair b && (b.op() == Expr.BinOp.GEDEELD_DOOR || b.op() == Expr.BinOp.GEDEELD_DOOR_ABS);
    }

    private static boolean isWortel(Expr e) {
        return e instanceof Expr.Functie f && f.kind() == Expr.FunctieKind.WORTEL;
    }

    /** The snippet of the first expression node for which {@code soort} holds, in {@code voorwaarde} and then in
     *  {@code waarde} (either may be null), each walked node before its operands, or null. */
    private static String eerste(Conditie voorwaarde, Expr waarde, java.util.function.Predicate<Expr> soort) {
        String[] gevonden = {null};
        java.util.function.Predicate<Expr> raak = x -> {
            if (!soort.test(x)) return false;
            gevonden[0] = snippet(x.bron());
            return true;
        };
        if (Conditie.bevatOperand(voorwaarde, raak) || Expr.bevat(waarde, raak)) return gevonden[0];
        return null;
    }

    /** Whether {@code d} decides anything itself: a decision on a characteristic, a consequence on an attribute. */
    private static boolean beslistZelf(Stapelbouw b, Default d) {
        return b.kenmerk ? d.besluit() != null : d.consequentie() != null;
    }

    /**
     * The simple form, variant M of A10 in the trial of card drools-logische-afleiding: one rule per default that
     * decides anything itself, with its reads bound, its justification as the test and {@code not Beslist} over every
     * higher index; the consequence computes the value from what the rule bound and, where it decides, inserts the
     * write and, where a lower default has a rule, {@link Beslist}. A leeg consequence inserts nothing, so the lower
     * defaults decide (ch. 7.1); the initialisation default matches only where the target is empty on the bean, which
     * holds the input (ch. 7.3). A do-not-attribute decision with no attribution below it has nothing to displace and
     * gets no rule.
     */
    private List<Rule> eenvoudigeVorm(Stapelbouw b, List<Knoop> keten) {
        List<Knoop> beslissend = new ArrayList<>(keten.stream().filter(k -> beslistZelf(b, k.d())).toList());
        while (b.kenmerk && !beslissend.isEmpty()
                && Boolean.FALSE.equals(beslissend.get(beslissend.size() - 1).d().besluit()))
            beslissend.remove(beslissend.size() - 1);
        String naam = b.naam;
        ElementSleutel sleutel = b.lid.sleutel();
        boolean kenmerk = b.kenmerk;
        List<Rule> uit = new ArrayList<>();
        for (int j = 0; j < beslissend.size(); j++) {
            Knoop k = beslissend.get(j);
            int i = k.positie();
            boolean hoger = j > 0, lager = j < beslissend.size() - 1;
            if (lager) b.stuurfeiten.add("Beslist");
            uit.addAll(eigenRegel(b, k, naam + ": default " + k.label(),
                    s -> hoger ? List.of(geenBeslist(naam, i, s)) : List.of(),
                    (drools, o, v) -> {
                        if (!kenmerk) drools.logicalInsert(new Waarde(o, sleutel, v, naam));
                        else if (Boolean.TRUE.equals(v)) drools.logicalInsert(new HeeftKenmerk(o, sleutel));
                        if (lager) drools.logicalInsert(new Beslist(o, naam, i));
                    }));
        }
        return uit;
    }

    /**
     * The general form: an {@link Uitkomst} per default that settles. A default without exception groups settles by
     * its own decision; a default with them reconciles the outcomes of their chains ({@link #verzoening}) and, with
     * content of its own, falls back to its own decision where no member settles. Every rule of a default carries
     * {@code not Uitkomst} over the positions above it in its chain and above each default whose exception group its
     * chain is ({@link #ketenregels}), so a lower default is displaced with whatever it would settle, a conflict
     * included (ch. 7.9), and its exception groups with it. At the root the groups of the stack reconcile once more, by
     * the same rules, under the bare key, and that reconciliation writes the target or deposits the conflict.
     */
    private List<Rule> algemeneVorm(Stapelbouw b, List<List<Knoop>> wortel) {
        List<Rule> uit = new ArrayList<>();
        for (List<Knoop> keten : wortel) ketenregels(b, keten, o -> List.of(), uit);
        String naam = b.naam;
        ElementSleutel sleutel = b.lid.sleutel();
        boolean kenmerk = b.kenmerk;
        verzoening(b, 0, wortel.size(), wortel.stream().anyMatch(TmsTranspiler::kanConflicteren), b.doelvorm, "wortel",
                o -> List.of(),
                (drools, o, s) -> drools.logicalInsert(
                        RegelSpraakRuntime.newInconsistentie(INCONSISTENTIE, (String) s, o)),
                (drools, o, v) -> drools.logicalInsert(
                        kenmerk ? new HeeftKenmerk(o, sleutel) : new Waarde(o, sleutel, v, naam)),
                kenmerk, false, uit);
        return uit;
    }

    /**
     * The rules of {@code keten} and, below each of its defaults, of their member chains. Every rule of a default waits
     * on the positions above it in its own chain and on {@code voorouders}, the positions above each default whose
     * exception group the chain is, at any depth: the reference interpreter reaches the exception groups of a default
     * only where no higher default in its chain decides ({@code decide}), so a group below a displaced default is not
     * evaluated, settles nothing and raises no fault (RUNTIME-SEMANTICS ch. 7.9; PLAN §9, 2026-10-09).
     */
    private void ketenregels(Stapelbouw b, List<Knoop> keten,
                             Function<Variable<Object>, List<RuleItemBuilder<?>>> voorouders, List<Rule> uit) {
        String naam = b.naam;
        for (Knoop k : keten) {
            int id = k.keten(), i = k.positie();
            Function<Variable<Object>, List<RuleItemBuilder<?>>> nietHoger = o -> {
                List<RuleItemBuilder<?>> wacht = new ArrayList<>(voorouders.apply(o));
                if (i > 0) wacht.add(geenUitkomst(naam, id, i, o));
                return wacht;
            };
            Afloop beslist = (drools, o, v) ->
                    drools.logicalInsert(new Uitkomst(o, naam, id, i, Uitkomst.Soort.BESLIST, v, null));
            if (!k.heeftLeden()) {
                if (!beslistZelf(b, k.d())) continue;
                b.stuurfeiten.add("Uitkomst " + Uitkomst.Soort.BESLIST);
                uit.addAll(eigenRegel(b, k, naam + ": default " + k.label(), nietHoger, beslist));
                continue;
            }
            for (List<Knoop> lid : k.leden()) ketenregels(b, lid, nietHoger, uit);
            int eerste = k.eersteLid(), tot = eerste + k.leden().size();
            verzoening(b, eerste, k.leden().size(), k.leden().stream().anyMatch(TmsTranspiler::kanConflicteren),
                    k.sleutel(), k.label(), nietHoger,
                    (drools, o, s) -> drools.logicalInsert(
                            new Uitkomst(o, naam, id, i, Uitkomst.Soort.CONFLICT, null, (String) s)),
                    beslist, false, true, uit);
            if (beslistZelf(b, k.d()))
                uit.addAll(eigenRegel(b, k, naam + ": terugval " + k.label(), o -> {
                    List<RuleItemBuilder<?>> wacht = new ArrayList<>();
                    wacht.add(DSL.not(uitkomsten(naam, eerste, tot, null, o)));
                    wacht.addAll(nietHoger.apply(o));
                    return wacht;
                }, beslist));
        }
    }

    /** Whether a chain can end in a conflict: some default in it reconciles two or more members, or a member that can. */
    private static boolean kanConflicteren(List<Knoop> keten) {
        for (Knoop k : keten)
            if (k.heeftLeden() && (k.leden().size() > 1 || k.leden().stream().anyMatch(TmsTranspiler::kanConflicteren)))
                return true;
        return false;
    }

    /**
     * The reconciliation of RUNTIME-SEMANTICS ch. 7.2, applied at every level by ch. 7.9, over the outcomes of the
     * member chains {@code [eerste, eerste + aantal)}: the first conflict
     * in member order passes on unchanged; else the first deciding member's decision, which is a conflict under
     * {@code sleutel} where another member decides differently (numbers by value), and the decision otherwise. A rule
     * per outcome, each under {@code wacht}; the rules that cannot match are left out (a conflict among members that
     * cannot conflict, a difference among one member). {@code alleenWaar} restricts the decision to an attribution, the
     * one decision on a characteristic that writes; {@code tussen} says the outcomes are control facts, not the write.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private void verzoening(Stapelbouw b, int eerste, int aantal, boolean ledenConflicteren, String sleutel, String label,
                            Function<Variable<Object>, List<RuleItemBuilder<?>>> wacht, Afloop bijConflict,
                            Afloop bijBesluit, boolean alleenWaar, boolean tussen, List<Rule> uit) {
        String naam = b.naam;
        int tot = eerste + aantal;
        if (ledenConflicteren) {
            Variable<Uitkomst> u = DSL.declarationOf(Uitkomst.class, "$conflict");
            Variable<Object> o = DSL.declarationOf(Object.class, "$onderwerp");
            Variable<Object> s = DSL.declarationOf(Object.class, "$sleutel");
            List<RuleItemBuilder<?>> items = new ArrayList<>();
            items.add(PatternDSL.pattern(u).expr(id("lid-conflict"), x -> x.stapel().equals(naam) && x.conflict()
                    && x.keten() >= eerste && x.keten() < tot).bind(o, Uitkomst::onderwerp).bind(s, Uitkomst::sleutel));
            if (aantal > 1) items.add(DSL.not(uitkomsten(naam, eerste, tot, Uitkomst.Soort.CONFLICT, o)
                    .expr(id("eerder-lid"), u, (x, y) -> x.keten() < y.keten())));
            items.addAll(wacht.apply(o));
            items.add(DSL.on(o, s).execute((Block3<org.drools.model.Drools, Object, Object>) (drools, oo, ss) ->
                    bijConflict.voer(drools, oo, ss)));
            uit.add(PatternDSL.rule(PKG, naam + ": doorgeven " + label).build(items.toArray(new RuleItemBuilder[0])));
            if (tussen) b.stuurfeiten.add("Uitkomst " + Uitkomst.Soort.CONFLICT);
        }
        if (aantal > 1) {
            Variable<Uitkomst> u = DSL.declarationOf(Uitkomst.class, "$besluit");
            Variable<Object> o = DSL.declarationOf(Object.class, "$onderwerp");
            List<RuleItemBuilder<?>> items = new ArrayList<>();
            items.add(PatternDSL.pattern(u).expr(id("lid-besluit"), x -> x.stapel().equals(naam) && !x.conflict()
                    && x.keten() >= eerste && x.keten() < tot).bind(o, Uitkomst::onderwerp));
            items.add(DSL.not(uitkomsten(naam, eerste, tot, Uitkomst.Soort.BESLIST, o)
                    .expr(id("eerder-lid"), u, (x, y) -> x.keten() < y.keten())));
            if (ledenConflicteren) items.add(DSL.not(uitkomsten(naam, eerste, tot, Uitkomst.Soort.CONFLICT, o)));
            items.add(DSL.exists(uitkomsten(naam, eerste, tot, Uitkomst.Soort.BESLIST, o)
                    .expr(id("ander-besluit"), u, (x, y) -> !Uitkomst.gelijk(x.waarde(), y.waarde()))));
            items.addAll(wacht.apply(o));
            items.add(DSL.on(o).execute((Block2<org.drools.model.Drools, Object>) (drools, oo) ->
                    bijConflict.voer(drools, oo, sleutel)));
            uit.add(PatternDSL.rule(PKG, naam + ": verschil " + label).build(items.toArray(new RuleItemBuilder[0])));
            if (tussen) b.stuurfeiten.add("Uitkomst " + Uitkomst.Soort.CONFLICT);
        }
        Variable<Uitkomst> u = DSL.declarationOf(Uitkomst.class, "$besluit");
        Variable<Object> o = DSL.declarationOf(Object.class, "$onderwerp");
        Variable<Object> v = DSL.declarationOf(Object.class, "$waarde");
        List<RuleItemBuilder<?>> items = new ArrayList<>();
        items.add(PatternDSL.pattern(u).expr(id("lid-besluit"), x -> x.stapel().equals(naam) && !x.conflict()
                && x.keten() >= eerste && x.keten() < tot && (!alleenWaar || Boolean.TRUE.equals(x.waarde())))
                .bind(o, Uitkomst::onderwerp).bind(v, Uitkomst::waarde));
        if (aantal > 1) {
            items.add(DSL.not(uitkomsten(naam, eerste, tot, Uitkomst.Soort.BESLIST, o)
                    .expr(id("eerder-lid"), u, (x, y) -> x.keten() < y.keten())));
            if (ledenConflicteren) items.add(DSL.not(uitkomsten(naam, eerste, tot, Uitkomst.Soort.CONFLICT, o)));
            items.add(DSL.not(uitkomsten(naam, eerste, tot, Uitkomst.Soort.BESLIST, o)
                    .expr(id("ander-besluit"), u, (x, y) -> !Uitkomst.gelijk(x.waarde(), y.waarde()))));
        }
        items.addAll(wacht.apply(o));
        items.add(DSL.on(o, v).execute((Block3<org.drools.model.Drools, Object, Object>) (drools, oo, vv) ->
                bijBesluit.voer(drools, oo, vv)));
        uit.add(PatternDSL.rule(PKG, naam + ": eens " + label).build(items.toArray(new RuleItemBuilder[0])));
        if (tussen) b.stuurfeiten.add("Uitkomst " + Uitkomst.Soort.BESLIST);
    }

    /** The outcomes of the chains {@code [van, tot)} of stack {@code naam} for {@code onderwerp}, of {@code soort}, or of
     *  either where it is null. */
    private PatternDSL.PatternDef<Uitkomst> uitkomsten(String naam, int van, int tot, Uitkomst.Soort soort,
                                                      Variable<Object> onderwerp) {
        Variable<Uitkomst> x = DSL.declarationOf(Uitkomst.class, "$uitkomst_" + idSeq);
        return zelfde(PatternDSL.pattern(x).expr(id("uitkomst"), y -> y.stapel().equals(naam) && y.keten() >= van
                        && y.keten() < tot && (soort == null || y.soort() == soort)),
                "uitkomst-onderwerp", onderwerp, Uitkomst::onderwerp);
    }

    /** {@code not Uitkomst} over the positions above {@code positie} in chain {@code keten}. */
    private RuleItemBuilder<?> geenUitkomst(String naam, int keten, int positie, Variable<Object> onderwerp) {
        Variable<Uitkomst> x = DSL.declarationOf(Uitkomst.class, "$hoger_" + idSeq);
        return DSL.not(zelfde(PatternDSL.pattern(x).expr(id("hoger-uitkomst"), y -> y.stapel().equals(naam)
                        && y.keten() == keten && y.positie() < positie),
                "hoger-uitkomst-onderwerp", onderwerp, Uitkomst::onderwerp));
    }

    /** {@code not Beslist} over the indices above {@code index} in the stack. */
    private RuleItemBuilder<?> geenBeslist(String naam, int index, Variable<Object> onderwerp) {
        Variable<Beslist> x = DSL.declarationOf(Beslist.class, "$hoger_" + idSeq);
        return DSL.not(zelfde(PatternDSL.pattern(x).expr(id("hoger-beslist"), y -> y.stapel().equals(naam)
                && y.index() < index), "hoger-beslist-onderwerp", onderwerp, Beslist::onderwerp));
    }

    /**
     * The rule of a default's own decision: the subject pattern on the default's level, restricted to the stack's
     * owner, with the input test of an initialisation default; the reads of its justification and its consequence bound
     * as any rule binds them; {@code wacht}, the control-fact patterns, before the test of the justification; and a
     * consequence that hands the decision to {@code besluit}: a characteristic's decision as declared, an attribute's
     * value computed from what the rule bound, and nothing where that value is leeg (ch. 7.1). The validity of the group
     * at the root of the default's chain gates the rule on the evaluation date ({@link Lezingen#geldig}).
     *
     * <p>A default carries no division and no square root ({@link #controleer}), but its justification or its
     * consequence can still fault, a date step out of range or a context value without an evaluation date
     * ({@link Rekenfout}). Such a fault is a {@link Fout} under the stack's name, as in a rule: the consequence inserts
     * it in place of the decision, and where the justification can fault, a fault rule with the rule's own left-hand
     * side, its control-fact patterns included, inserts it ({@link #voorwaardefoutregel}). A default a higher one
     * displaces loses its match, and so does every default of an exception group below it ({@link #ketenregels}), and
     * the fault it raised goes with it, so a fault counts only where its default is evaluated at the fixpoint (PLAN §9,
     * 2026-10-09).
     */
    private List<Rule> eigenRegel(Stapelbouw b, Knoop k, String regelnaam,
                                  Function<Variable<Object>, List<RuleItemBuilder<?>>> wacht, Afloop besluit) {
        Default d = k.d();
        Class<?> klasse = b.klasse;
        Supplier<Lezingen> nieuw = () -> {
            Lezingen l = new Lezingen(k.niveau());
            l.onderwerp.expr(id("concreet-type"), o -> o.getClass() == klasse);
            if (d.initialisatie()) {
                Function<Object, Object> invoer = Accessors.getter(b.lid.klasse(), b.lid.naam());
                l.onderwerp.expr(id("doel-leeg-in-de-invoer"), o -> invoer.apply(o) == null);
            }
            return l.geldig(k.geldigheid());
        };
        Lezingen lez = nieuw.get();
        Set<Integer> gelezen = lez.volg();
        Term toets = d.voorwaarde() == null ? null : voorwaarde(d.voorwaarde(), lez);
        boolean kanFalen = lez.kanFalen();
        lez.stop();
        String naam = b.naam;
        List<RuleItemBuilder<?>> items;
        if (b.kenmerk) {
            items = lez.linkerkant(voorwaarderol(kanFalen), toets, gelezen, d.voorwaarde(), wacht.apply(lez.s));
            Boolean beslissing = d.besluit();
            items.add(DSL.on(lez.s).execute((Block2<org.drools.model.Drools, Object>) (drools, o) ->
                    besluit.voer(drools, o, beslissing)));
        } else {
            Set<Integer> gebruik = lez.volg();
            Waardefunctie waarde = gevolgwaarde(herschaald(waarde(d.consequentie(), lez),
                    eff.eenheidVan(d.consequentie()), b.lid.datatype().eenheidOf(), "een §4 schrijfgrens naar het doel"));
            lez.stop();
            items = lez.linkerkant(voorwaarderol(kanFalen), toets, gelezen, d.voorwaarde(), wacht.apply(lez.s));
            int n = lez.variabelen.size();
            items.add(gevolg(lez, posities(gebruik), (drools, o, w) -> {
                Object v;
                try {
                    v = collapse(waarde.van(w));
                } catch (RuntimeException e) {
                    if (!Rekenfout.isEvaluatiefout(e)) throw e;
                    drools.logicalInsert(new Fout(o, naam, Rekenfout.benoem(e, naam)));
                    return;
                }
                if (v != null) besluit.voer(drools, o, v);
            }, n));
        }
        b.lezingen.add(lez);
        List<Rule> uit = new ArrayList<>();
        uit.add(PatternDSL.rule(PKG, regelnaam).build(items.toArray(new RuleItemBuilder[0])));
        if (kanFalen)
            uit.add(voorwaardefoutregelRechtstreeks(naam, regelnaam, nieuw, d.voorwaarde(), l -> wacht.apply(l.s)));
        return uit;
    }

    /** §6.1.2: a value at rest is a decimal, so an exact rational that survived the computation collapses here
     *  ({@link Rekenhulpen#collapse}, which a planned value calls as well). */
    private static Object collapse(Object v) {
        return Rekenhulpen.collapse(v);
    }

    // ---------------- values ----------------

    /** A value as the analysis plans it ({@link Term}): a tree of calls over the values the rule binds, indexed as
     *  {@link Lezingen} numbers them, which {@link ClosureWeergave} renders into the function a consequence or a test
     *  calls. Every expression node is named here, so a node the IR records gain later fails to compile until it is
     *  decided whether this branch computes it. */
    private Term waarde(Expr e, Lezingen lez) {
        return switch (e) {
            case Expr.Literal l -> literaal(l);
            case Expr.AttribuutRef a -> new Term.Lees(attribuutlezing(a, lez), bron(a.bron()));
            case Expr.ParameterRef p -> new Term.Lees(lez.parameter(p.parameter()), bron(p.bron()));
            case Expr.Binair b -> binair(b, lez);
            case Expr.TekstReeks t -> tekstreeks(t, lez);
            case Expr.Functie f -> functie(f, lez);
            // IR-SPEC ch. 4.3: a union denotes instances and stands only as the argument of a count, which reads it in
            // vouw; here it stands where a value is read, which the verifier refuses (WF-110 (a)), the reference
            // interpreter faults on and the DRL-text branch refuses in this sentence
            case Expr.InstanceSetUnion u -> throw nietBerekend("InstanceSetUnion", u.bron(), "zij duidt de instanties van"
                    + " een §5.7-concatenatie aan, en alleen een instantietelling verbruikt ze (WF-110 (a))");
            case Expr.Afgerond a -> afronding(a, lez);
            case Expr.Begrensd b -> begrenzing(b, lez);
            case Expr.DimensieCel c -> dimensiecel(c, lez);
            case Expr.VariabeleRef v -> throw nietBerekend("VariableReference", v.bron(), "het merkteken van een"
                    + " variabele die de frontend niet bond, dat de lader weigert (WF-034), zodat het deze tak alleen in een"
                    + " met de hand gebouwd model bereikt");
            case Expr.Let l -> let(l, lez);
            case Expr.BinderRef b -> binderlezing(b, lez);
            case Expr.IfThenElse i -> keuze(i, lez);
            case Expr.Constante c -> contextwaarde(c, lez);
            // RUNTIME-SEMANTICS ch. 5.7 and WF-044: the marker fails the rule's evaluation where the evaluation reaches it,
            // and only there, so a marker in a branch that is not taken plays
            case Expr.Onopgelost o -> {
                lez.markeerFalen();
                yield new Term.Faal(Rekenfout.ONOPGELOST, bron(o.bron()));
            }
        };
    }

    /** The function a consequence or a test calls to compute {@code t} ({@link ClosureWeergave}). */
    private static Waardefunctie weergave(Term t) {
        return ClosureWeergave.waardefunctie(t);
    }

    /** The test a consequence calls to evaluate the condition {@code t} again ({@link ClosureWeergave#toets(Term)}). */
    private static Toets toetsweergave(Term t) {
        Predicate<Object[]> p = ClosureWeergave.toets(t);
        return p::test;
    }

    /** {@code t}, the value a consequence of the form being lowered computes, recorded as its plan
     *  ({@link #waardeplannen}), and rendered into the function the consequence calls. */
    private Waardefunctie gevolgwaarde(Term t) {
        return weergave(waardeplan(t));
    }

    /** {@code t}, the value a consequence of the form being lowered computes, recorded as its plan
     *  ({@link #waardeplannen}); a rule that comes whole from its plan carries it in its consequence. */
    private Term waardeplan(Term t) {
        waardeplannen.add(new Waardeplan(vorm, t));
        return t;
    }

    /** A constant of the analysis, which an argument of a function of the plan takes ({@link Term.Const}). */
    private static Term.Const constante(Object waarde) {
        return new Term.Const(waarde, null, null);
    }

    /** A test a form composes itself ({@link Toetsitem.Rol#VORM}) of the checks {@code leden}: their conjunction,
     *  read in order up to the first that does not hold ({@link Term.Samengesteld} under all). No node of the IR
     *  stands for it, so it carries no source fragment. */
    private static Term alle(List<Term> leden) {
        return new Term.Samengesteld(Conditie.Kwantor.ALLE, 0, leden, null);
    }

    /** The check that the value bound at slot {@code slot} is gevuld, in a test a form composes itself. */
    private static Term gevuld(int slot) {
        return new Term.Aanroep(Functie.LEEGTOETS, List.of(new Term.Lees(slot, null), constante(false)), null);
    }

    /** The check that the value bound at slot {@code slot} is ({@code gewenst}) or is not the asOfDate-bucket scalar
     *  of a timeline ({@link Emmerscalair}), in a test a form composes itself. */
    private static Term emmerscalair(int slot, boolean gewenst) {
        return new Term.Aanroep(Functie.EMMERSCALAIR, List.of(new Term.Lees(slot, null), constante(gewenst)), null);
    }

    /** The source fragment of an IR node, which a term carries; null where the IR has none. */
    private static String bron(dev.uchar.regelspraak.ir.Bron bron) {
        return bron == null ? null : bron.snippet();
    }

    private static Weigering nietBerekend(String vorm, dev.uchar.regelspraak.ir.Bron bron, String reden) {
        return new Weigering("een " + vorm + " in een waarde ('" + snippet(bron) + "'): " + reden);
    }

    private static String snippet(dev.uchar.regelspraak.ir.Bron bron) {
        return bron == null || bron.snippet() == null ? "zonder bron" : bron.snippet();
    }

    private static String snippet(Conditie c) {
        return snippet(c.bron());
    }

    /** A literal: a number, and a percentage, a number in the unit %, exact where its
     *  quotient does not terminate, a date, a boolean, and a text or an enumeration label as its string. */
    private static Term literaal(Expr.Literal l) {
        Datatype t = l.type();
        String bron = bron(l.bron());
        if (t instanceof Datatype.Numeriek || t instanceof Datatype.Percentage) {
            BigDecimal[] r = Rationaal.van(l.waarde());
            if (r == null) throw new Weigering("een getal dat niet te lezen is ('" + l.waarde() + "')");
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
            if (v == null) throw new Weigering("een datum die niet te lezen is ('" + l.waarde() + "')");
            return new Term.Const(v, t, bron);
        }
        if (t instanceof Datatype.DatumTijd) {
            LocalDateTime v = Literalen.parseDatumTijd(l.waarde());
            if (v == null) throw new Weigering("een datum-tijd die niet te lezen is ('" + l.waarde() + "')");
            return new Term.Const(v, t, bron);
        }
        String label = Literalen.tekstOfEnumLabel(l);
        if (label == null) throw new Weigering("een Literal van type " + t + ": " + NOG_NIET);
        return new Term.Const(label, t, bron);
    }

    /** The arithmetic operators on numbers, a percentage being a number in the unit % (RUNTIME-SEMANTICS ch. 2.2, 2.6),
     *  through the runtime's exact helpers and with their leeg tables (ch. 1.2); a date plus or minus a duration through
     *  {@link #datumrekening}, a power through {@link #macht}. A sum or a difference restates its right operand into the
     *  left one's unit (ch. 2.6); a product, a quotient and a percentage of a value do not. A division faults on a numeric
     *  zero (ch. 1.4, point 4), and one of a filled dividend by a leeg divisor deposits where the evaluation reaches it and
     *  its operands qualify ({@link #deponeerplek}, {@link Sonde}). The operands are evaluated left before right
     *  (ch. 5.11). */
    private Term binair(Expr.Binair b, Lezingen lez) {
        Datatype lt = eff.typeVan(b.links()), rt = eff.typeVan(b.rechts());
        if (lt.isDatum() || rt.isDatum()) return datumrekening(b, lez);
        Expr.BinOp op = b.op();
        if (op == Expr.BinOp.TOT_DE_MACHT) return macht(b, lez);
        boolean deling = op == Expr.BinOp.GEDEELD_DOOR || op == Expr.BinOp.GEDEELD_DOOR_ABS;
        // a condition that holds a division carries the rule that inserts its fault (voorwaardefoutregel)
        if (deling) lez.markeerFalen();
        Term l = waarde(b.links(), lez), rechts = waarde(b.rechts(), lez);
        Term r = op == Expr.BinOp.PLUS || op == Expr.BinOp.MIN || op == Expr.BinOp.VERMINDERD_MET
                ? herschaald(rechts, eff.eenheidVan(b.rechts()), eff.eenheidVan(b.links()), "een additieve operandgrens")
                : rechts;
        int sonde = deling && deponeerplek(b) ? lez.sonde() : -1;
        String bron = bron(b.bron());
        return switch (op) {
            case PLUS -> new Term.Aanroep(Functie.RS_PLUS, List.of(l, r), bron);
            case MIN -> new Term.Aanroep(Functie.RS_MIN, List.of(l, r), bron);
            case VERMINDERD_MET -> new Term.Aanroep(Functie.RS_VERMINDERD, List.of(l, r), bron);
            case MAAL -> new Term.Aanroep(Functie.RS_MAAL, List.of(l, r), bron);
            case PERCENTAGE_VAN -> new Term.Aanroep(Functie.RS_PERCENTAGE_VAN, List.of(l, r), bron);
            case GEDEELD_DOOR -> new Term.Deel(Functie.GEDEELD, l, r, sonde, bron);
            case GEDEELD_DOOR_ABS -> new Term.Deel(Functie.GEDEELD_ABS, l, r, sonde, bron);
            case TOT_DE_MACHT -> throw new IllegalStateException("een macht gaat door macht()");
        };
    }

    /** A power with a whole exponent of at least 0 written as a literal, exact (RUNTIME-SEMANTICS ch. 2.2), a leeg base
     *  leeg (ch. 1.2), through the runtime's {@code rsMacht}. Any other exponent is refused: a
     *  negative or fractional literal, or a value read while the session fires, reaches the real power, which ch. 2.1
     *  leaves OPEN, where the reference interpreter answers an exact root or faults and the destructive lowering
     *  computed a binary double. */
    private Term macht(Expr.Binair b, Lezingen lez) {
        Integer n = b.rechts() instanceof Expr.Literal lit && lit.type().isGetalachtig() ? heleExponent(lit.waarde()) : null;
        if (n == null)
            throw new Weigering("een macht met een exponent die geen geheel getal van ten minste 0 als literal is ('"
                    + snippet(b.bron()) + "'): de reële macht is OPEN (RUNTIME-SEMANTICS ch. 2.1), waar de"
                    + " referentie-interpreter een exacte wortel geeft of faalt");
        Term basis = waarde(b.links(), lez);
        return new Term.Aanroep(Functie.RS_MACHT, List.of(basis, constante(n)), bron(b.bron()));
    }

    /** The whole exponent of at least 0 the lexeme writes, or null. */
    private static Integer heleExponent(String lexeem) {
        BigDecimal[] r = Rationaal.van(lexeem);
        if (r == null || r[1].signum() == 0) return null;
        try {
            BigDecimal v = r[0].divide(r[1]);
            if (v.signum() < 0 || v.stripTrailingZeros().scale() > 0) return null;
            int n = v.intValueExact();
            return n <= 999_999_999 ? n : null;
        } catch (ArithmeticException geenGeheelGetal) {
            return null;
        }
    }

    /** A date plus or minus a duration with a calendar or clock unit: no date minus date, no duration minus a date, a
     *  clock unit only on a date-time. A duration counted in a day kind walks the calendar ({@link #dagsoortstap}). A
     *  step or a count outside the range of RUNTIME-SEMANTICS ch. 3.8 is an evaluation fault
     *  ({@link Rekenhulpen#datumstap}); the date is evaluated before the duration (ch. 5.11). */
    private Term datumrekening(Expr.Binair b, Lezingen lez) {
        Expr.BinOp op = b.op();
        if (op != Expr.BinOp.PLUS && op != Expr.BinOp.MIN && op != Expr.BinOp.VERMINDERD_MET)
            throw new Weigering("een Binary " + Spelling.BINARY_OP.spelling(op) + " met een datum ('" + snippet(b.bron())
                    + "')");
        boolean datumLinks = eff.typeVan(b.links()).isDatum();
        Expr datumExpr = datumLinks ? b.links() : b.rechts();
        Expr duurExpr = datumLinks ? b.rechts() : b.links();
        boolean optellen = op == Expr.BinOp.PLUS;
        if (eff.typeVan(duurExpr).isDatum() || (!optellen && !datumLinks))
            throw new Weigering("een rekensom van twee datums of een duur min een datum ('" + snippet(b.bron()) + "')");
        // ch. 3.8 puts the calendar table first, as Tijdrekening.dagsoortVoorEenheid looks a spelling up for every branch
        String dagsoort = Tijdrekening.dagsoortVoorEenheid(model, eff.typeVan(duurExpr).eenheidOf());
        if (dagsoort != null) return dagsoortstap(b, datumExpr, duurExpr, optellen, dagsoort, lez);
        String eenheid = Tijdrekening.chronoUnit(eff.typeVan(duurExpr));
        if (eenheid == null) throw new Weigering("een duur zonder tijdseenheid ('" + snippet(b.bron()) + "')");
        boolean klok = switch (eenheid) { case "HOURS", "MINUTES", "SECONDS", "MILLIS" -> true; default -> false; };
        if (klok && !(eff.typeVan(datumExpr) instanceof Datatype.DatumTijd))
            throw new Weigering("een klokeenheid bij een datum zonder tijd ('" + snippet(b.bron()) + "')");
        lez.markeerFalen();
        Term d = waarde(datumExpr, lez), n = waarde(duurExpr, lez);
        return new Term.Aanroep(Functie.DATUMSTAP, List.of(d, n, constante(eenheid), constante(optellen)),
                bron(b.bron()));
    }

    /**
     * A date or a date-time plus or minus a count of days of the day kind {@code dagsoort} (RUNTIME-SEMANTICS ch. 3.8,
     * §6.11), as the runtime walks it for the other branches: from the date, which never counts, one calendar day at a
     * time to the Nth day the definition admits ({@code rsDatumPlusDagsoort}); on a date-time over its date part, the
     * clock time riding through ({@code rsDatumTijdPlusDagsoort}). A count of 0 or leeg gives the date unchanged, a leeg
     * date leeg. The definition reads nothing of working memory (WF-031), so its predicate is a constant of the document
     * ({@link #dagsoortpredikaat}), and the reads the step depends on are the date and the count, bound as any value of
     * the rule. A walk that passes the reach of ch. 3.8 without a day of the kind is an evaluation fault in the sentence
     * of the reference interpreter ({@link Rekenhulpen#dagsoortstap}), a {@link Fout} that counts only where it stands
     * at the fixpoint. The date is evaluated before the count (ch. 5.11).
     */
    private Term dagsoortstap(Expr.Binair b, Expr datumExpr, Expr duurExpr, boolean optellen, String dagsoort,
                              Lezingen lez) {
        java.util.function.Predicate<LocalDate> predikaat =
                dagsoortpredikaat(dagsoort, "een duur in dagsoorten ('" + snippet(b.bron()) + "')");
        boolean datumTijd = eff.typeVan(datumExpr) instanceof Datatype.DatumTijd;
        lez.markeerFalen();
        Term d = waarde(datumExpr, lez), n = waarde(duurExpr, lez);
        return new Term.Aanroep(datumTijd ? Functie.DATUMTIJD_DAGSOORTSTAP : Functie.DAGSOORTSTAP,
                List.of(d, n, new Term.Dagsoort(dagsoort, predikaat, null), constante(optellen)), bron(b.bron()));
    }

    /**
     * The predicate of the day kind {@code naam} over a calendar day, from its definition (IR-SPEC ch. 3.7), compiled by
     * the backend's day-kind compiler ({@code kern.Dagsoorten}) the first time a form needs it and the same object for
     * every form after: the bound day, its date parts, the first Easter day, literals and other day kinds on the bound
     * day. The form {@code wat} that steps or counts in the day kind is refused by name where the definition does not
     * compile ({@link #dagsoortweigering}); a definition that no form uses is never compiled, so it refuses nothing.
     */
    private java.util.function.Predicate<LocalDate> dagsoortpredikaat(String naam, String wat) {
        java.util.function.Predicate<LocalDate> p = dagsoorten.predikaat(naam);
        if (p == null) throw new Weigering(wat + ": " + dagsoortweigering(naam, new LinkedHashSet<>()));
        return p;
    }

    /**
     * Why the day kind {@code naam} has no predicate here, found in its definition and in the definitions it refers to
     * ({@code bezocht} holds those already looked at, the reference graph being acyclic by WF-085): no definition; a
     * parameter, which WF-031 admits and the reference interpreter reads, and which this branch does not read, since
     * the day-kind compiler binds nothing; a day-kind test on another day than the bound day, which no executor carries
     * (RUNTIME-SEMANTICS ch. 3.8, red case {@code dagsoortverwijzing-andere-dag}); and else a shape of its condition
     * outside what the compiler carries.
     */
    private String dagsoortweigering(String naam, Set<String> bezocht) {
        ResolvedDagsoort def = null;
        for (ResolvedDagsoort d : model.resolvedDagsoorten)
            if (d.naam() != null && d.naam().equalsIgnoreCase(naam)) def = d;
        if (def == null) return "de dagsoort '" + naam + "' heeft geen definitie (WF-031)";
        if (!bezocht.add(naam.toLowerCase())) return null;
        String[] parameter = {null};
        Conditie.bevatOperand(def.voorwaarde(), e -> e instanceof Expr.ParameterRef pr
                && (parameter[0] = pr.parameter() == null ? "(onopgelost)" : pr.parameter().naam()) != null);
        if (parameter[0] != null)
            return "de definitie van de dagsoort '" + naam + "' leest de parameter '" + parameter[0] + "', die deze"
                    + " tak in een dagsoortdefinitie niet leest (WF-031 laat haar toe)";
        List<Conditie.Predicaat> toetsen = new ArrayList<>();
        dagsoorttoetsen(def.voorwaarde(), toetsen);
        for (Conditie.Predicaat t : toetsen) {
            if (!(t.operand() == null || t.operand() instanceof Expr.Constante c && "de dag".equalsIgnoreCase(c.naam())))
                return "de definitie van de dagsoort '" + naam + "' toetst de dagsoort '" + t.dagsoort() + "' op een andere"
                        + " dag dan de gebonden dag ('" + snippet(t) + "'), een vorm die geen executor uitvoert"
                        + " (RUNTIME-SEMANTICS ch. 3.8)";
            if (t.dagsoort() != null && dagsoorten.predikaat(t.dagsoort()) == null) {
                String genest = dagsoortweigering(t.dagsoort(), bezocht);
                if (genest != null) return genest;
            }
        }
        return "de definitie van de dagsoort '" + naam + "' heeft een voorwaarde ('" + snippet(def.voorwaarde()) + "')"
                + " buiten wat de dagsoortcompiler van de backend draagt: vergelijkingen van de gebonden dag, zijn"
                + " datumdelen, de eerste paasdag en literalen, en andere dagsoorten op de gebonden dag";
    }

    /** The day-kind tests in {@code c}, in wire order, through its composites. */
    private static void dagsoorttoetsen(Conditie c, List<Conditie.Predicaat> uit) {
        if (c instanceof Conditie.Predicaat p && p.soort() == Conditie.Predicaatsoort.DAGSOORT) uit.add(p);
        else if (c instanceof Conditie.Samengesteld s) for (Conditie d : s.onderdelen()) dagsoorttoetsen(d, uit);
    }

    /** {@code t} restated from unit {@code van} into unit {@code naar} (RUNTIME-SEMANTICS ch. 2.6): times the exact
     *  canonical factors {@code num ÷ den} as rationals where both are known, so a restatement that does not terminate
     *  stays exact until a collapse point (ch. 2.1), as the reference interpreter restates; times the single ratio of the
     *  two units otherwise, a rate whose factors are not both known; unchanged where no restatement is due, a unitless
     *  side adopting the other's unit. A leeg value stays leeg and a value that is not a number passes through
     *  ({@link Term.Herschaal}). Two units that do not convert fault while the model is built ({@link Eenheidsconflict}). */
    private static Term herschaald(Term t, Eenheid van, Eenheid naar, String plek) {
        Eenheidsconflict.eisHerschaalbaar(van, naar, plek);
        if (van == null || naar == null || !van.convertibleNaar(naar) || !van.vereistOmrekening(naar)) return t;
        BigDecimal[] exact = van.exacteHerschaalFactoren(naar);
        if (exact != null)
            return new Term.Herschaal(Functie.HERSCHAAL_EXACT, t, van, naar, exact[0], exact[1], t.bron());
        return new Term.Herschaal(Functie.HERSCHAAL, t, van, naar, van.omrekenfactorNaar(naar), null, t.bron());
    }

    // ---------------- the forms of a value beside the arithmetic ----------------

    /** A rounding (RUNTIME-SEMANTICS ch. 2.5) of its base, a leeg base leeg (ch. 1.2), through {@code rsAfronding}, which
     *  rounds an exact rational on its true value; a square root as the base is the root under its mandatory rounding
     *  ({@link #wortelwaarde}). */
    private Term afronding(Expr.Afgerond a, Lezingen lez) {
        java.math.RoundingMode modus = afrondModus(a.modus());
        int decimalen = a.decimalen();
        Term basis = a.basis() instanceof Expr.Functie f && f.kind() == Expr.FunctieKind.WORTEL
                ? wortelwaarde(f, lez) : waarde(a.basis(), lez);
        return new Term.Aanroep(Functie.RS_AFRONDING, List.of(basis, constante(decimalen), constante(modus)),
                bron(a.bron()));
    }

    private static java.math.RoundingMode afrondModus(Expr.AfrondModus m) {
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
     *  {@link Sonde}). */
    private Term wortelwaarde(Expr.Functie f, Lezingen lez) {
        if (f.argumenten().size() != 1)
            throw new Weigering("een squareRoot met " + f.argumenten().size() + " argumenten ('" + snippet(f.bron())
                    + "'): de wortel heeft er één (WF-037)");
        Term radicand = waarde(f.argumenten().get(0), lez);
        int sonde = deponeerplek(f) ? lez.sonde() : -1;
        return new Term.Wortel(Functie.WORTEL, radicand, sonde, bron(f.bron()));
    }

    /** A clamp (RUNTIME-SEMANTICS ch. 2.7): its base, then its minimum, then its maximum, in the order of ch. 5.11 and
     *  of the bounds' application, each bound restated into the base's unit (ch. 2.6); a leeg base leeg, an absent or
     *  leeg bound no bound ({@code rsBegrens}). */
    private Term begrenzing(Expr.Begrensd g, Lezingen lez) {
        Term basis = waarde(g.basis(), lez);
        Eenheid eenheid = eff.eenheidVan(g.basis());
        Term minimum = g.minimum() == null ? constante(null)
                : herschaald(waarde(g.minimum(), lez), eff.eenheidVan(g.minimum()), eenheid, "een begrenzing (minimum)");
        Term maximum = g.maximum() == null ? constante(null)
                : herschaald(waarde(g.maximum(), lez), eff.eenheidVan(g.maximum()), eenheid, "een begrenzing (maximum)");
        return new Term.Aanroep(Functie.RS_BEGRENS, List.of(basis, minimum, maximum), bron(g.bron()));
    }

    /** A text composition (IR-SPEC ch. 4.2): the literal chunks and the inserted parts in part order (RUNTIME-SEMANTICS
     *  ch. 5.11), each as its text ({@link #tekstdeel}). */
    private Term tekstreeks(Expr.TekstReeks t, Lezingen lez) {
        List<Term> delen = new ArrayList<>();
        for (Expr.TekstReeks.Deel d : t.delen()) {
            switch (d) {
                case Expr.TekstReeks.Letterlijk l ->
                        delen.add(new Term.Const(l.tekst() == null ? "" : l.tekst(), new Datatype.Tekst(), null));
                case Expr.TekstReeks.Ingevoegd i -> delen.add(tekstdeel(i.expr(), lez, "TextComposition"));
            }
        }
        return new Term.Tekstreeks(delen, bron(t.bron()));
    }

    /** An inserted part of a text composition or an argument of a concatenation, as its text: a text or an enumeration
     *  label is its own text, and a leeg part reads "" (RUNTIME-SEMANTICS ch. 1.2). A part of another type is refused by
     *  name: the spec states the leeg rule and nothing more, and for the text of a number, a truth value or a date the
     *  reference interpreter (5,50 as "5.5", waar as "waar") and the runtime's {@code rsTekst} (5.50, true) differ. */
    private Term tekstdeel(Expr e, Lezingen lez, String waar) {
        Datatype t = eff.typeVan(e);
        if (!(t instanceof Datatype.Tekst) && !(t instanceof Datatype.Enumeratie))
            throw new Weigering("een tekstdeel van type " + typenaam(t) + " in een " + waar + " ('" + snippet(e.bron())
                    + "'): RUNTIME-SEMANTICS ch. 1.2 legt alleen vast dat een leeg deel \"\" leest; de tekst van een getal,"
                    + " een waarheidswaarde of een datum beslist de spec niet, en de referentie-interpreter en de"
                    + " runtimehulp rsTekst geven er een verschillende");
        Term f = waarde(e, lez);
        return new Term.Aanroep(Functie.RS_TEKST, List.of(f), f.bron());
    }

    /** A cell of a dimensioned attribute (§3.6): the value at its one label in the attribute's {@code Map<label, value>}
     *  bean member, leeg where the map or the label is absent. The attribute is
     *  input, read off the subject's own bean ({@link #dimensielezing}); a cell over several dimensions is refused by
     *  name, as that branch refuses it. The reference interpreter and the ASP backend do not read a cell. */
    private Term dimensiecel(Expr.DimensieCel c, Lezingen lez) {
        if (c.labels() == null || c.labels().size() != 1)
            throw new Weigering("een DimensionCellRead over " + (c.labels() == null ? 0 : c.labels().size())
                    + " dimensies ('" + snippet(c.bron()) + "'): deze tak leest alleen een cel van één dimensie");
        int i = dimensielezing(c.attribuut(), lez, "een DimensionCellRead");
        String label = c.labels().get(0);
        return new Term.Aanroep(Functie.CEL, List.of(new Term.Lees(i, bron(c.attribuut().bron())), constante(label)),
                bron(c.bron()));
    }

    /** A sum, minimum or maximum over a dimension selection (§5.8.5): the attribute's cells at the selected labels, a
     *  leeg cell skipped and an all-leeg selection leeg, through the runtime's {@code rsDimSom},
     *  {@code rsDimMin} and {@code rsDimMax}; a sum's {@code emptyFallback} is evaluated after the cells, every time
     *  (RUNTIME-SEMANTICS ch. 5.11), and stands in for an all-leeg sum. The reference interpreter and the ASP backend do
     *  not fold a dimension selection. */
    private Term dimensievouw(Expr.Functie f, Lezingen lez) {
        Expr.FunctieKind kind = f.kind();
        if (kind != Expr.FunctieKind.SOM && kind != Expr.FunctieKind.MINIMUM && kind != Expr.FunctieKind.MAXIMUM)
            throw new Weigering("een Aggregation " + kind + " over een dimensieselectie ('" + snippet(f.bron())
                    + "'): alleen een som, een minimum en een maximum vouwen een dimensieselectie (WF-037)");
        if (f.leegwaarde() != null && kind != Expr.FunctieKind.SOM)
            throw new Weigering("een emptyFallback op een Aggregation " + kind + " ('" + snippet(f.bron()) + "'): alleen"
                    + " een som heeft er een (WF-037)");
        if (f.argumenten().size() != 1 || !(f.argumenten().get(0) instanceof Expr.AttribuutRef a))
            throw new Weigering("een Aggregation over een dimensieselectie over iets anders dan één attribuut ('"
                    + snippet(f.bron()) + "')");
        int i = dimensielezing(a, lez, "een Aggregation over een dimensieselectie");
        List<String> labels = List.copyOf(f.dimensieSelectie().labels());
        Term terugval = f.leegwaarde() == null ? null : herschaald(waarde(f.leegwaarde(), lez),
                eff.eenheidVan(f.leegwaarde()), eff.eenheidVan(a), "een §5.8.2 terugvalwaarde");
        Functie vouw = switch (kind) {
            case SOM -> Functie.RS_DIM_SOM;
            case MINIMUM -> Functie.RS_DIM_MIN;
            default -> Functie.RS_DIM_MAX;
        };
        String bron = bron(f.bron());
        Term cellen = new Term.Aanroep(vouw, List.of(new Term.Aanroep(Functie.KAART,
                List.of(new Term.Lees(i, bron(a.bron()))), null), constante(labels)), bron);
        // only a sum has a fallback, evaluated after the cells
        return terugval == null ? cellen : new Term.Aanroep(Functie.TERUGVAL, List.of(cellen, terugval), bron);
    }

    /** The read of the dimensioned attribute {@code a} as its whole map: the subject's own attribute, which no form
     *  writes, bound off its bean. A dimensioned attribute over a path, or one a form writes, is refused by name. */
    private int dimensielezing(Expr.AttribuutRef a, Lezingen lez, String wat) {
        if (a.objecttype() == null || a.attribuut() == null)
            throw new Weigering("een onopgeloste AttributeReference in " + wat + " ('" + snippet(a.bron()) + "')");
        if (navigatieBindingen.containsKey(a))
            throw new Weigering(wat + " over een pad ('" + snippet(a.bron()) + "'): de tak leest een gedimensioneerd"
                    + " attribuut alleen op het eigen onderwerp");
        if (a.objecttype() != lez.subject)
            throw new Weigering("een eigen lezing die " + a.objecttype().naam + " noemt op een " + lez.subject.naam
                    + " ('" + snippet(a.bron()) + "', WF-053)");
        Lid lid = gedimensioneerdLid(lez.subject, a.attribuut());
        if (schrijvers.containsKey(lid.sleutel()))
            throw new Weigering(wat + " over het attribuut '" + lid.naam() + "', dat een vorm schrijft ('"
                    + snippet(a.bron()) + "'): de tak leest een gedimensioneerd attribuut alleen als invoer");
        return lez.eigen(lid);
    }

    /**
     * A {@code Let} (RUNTIME-SEMANTICS ch. 10.6): the body evaluated with the binder bound to its value as a need. The
     * value is evaluated in the enclosing scope (WF-076), at the first read of the binder, and that one result serves
     * every further read (clause 1); a read whose value is leeg ends the {@code Let} with leeg (clause 2), the barrier
     * belonging to the {@code Let} whose binder was read; a body that never reads the binder evaluates nothing of the
     * value, so no leeg, no fault and no deposit come of it (clause 3). The binder is a slot of the value array that no
     * pattern binds ({@link Lezingen#binder}), which the rendering fills with the need for the body
     * ({@link ClosureWeergave}). Every read the value and the body make is bound in the left-hand side all the same, read
     * or not: truth maintenance follows each value the evaluation may need, and a read through a total standing never
     * keeps the rule from matching. The plan marks the {@code Let} eager where the body reads the binder before anything
     * else of it can fault, deposit or end an evaluation ({@link #leestEerst}).
     */
    private Term let(Expr.Let l, Lezingen lez) {
        if (l.binder() == null) throw new Weigering("een Let zonder binder ('" + snippet(l.bron()) + "')");
        Term waarde = waarde(l.value(), lez);
        int slot = lez.binder();
        String sleutel = Namen.norm(l.binder());
        Integer buiten = lez.binders.put(sleutel, slot);
        Term lichaam;
        try {
            lichaam = waarde(l.body(), lez);
        } finally {
            if (buiten == null) lez.binders.remove(sleutel);
            else lez.binders.put(sleutel, buiten);
        }
        return new Term.Let(slot, l.binder(), waarde, lichaam, Boolean.TRUE.equals(leestEerst(lichaam, slot)),
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
    private static Boolean leestEerst(Term t, int slot) {
        return switch (t) {
            case Term.Const c -> null;
            case Term.Lees l -> null;
            case Term.Dagsoort d -> null;
            // a read of an input has no effect and cannot fault, so the evaluation goes on to what follows it
            case Term.Invoer i -> leestEerst(i.object(), slot);
            case Term.Binder b -> b.slot() == slot;
            case Term.Aanroep a -> {
                Boolean eerst = leestEerst(a.argumenten(), slot);
                yield eerst != null ? eerst : a.functie().kanFalen() ? Boolean.FALSE : null;
            }
            case Term.Herschaal h -> leestEerst(h.term(), slot);
            case Term.Deel d -> {
                Boolean eerst = leestEerst(List.of(d.links(), d.rechts()), slot);
                yield eerst != null ? eerst : Boolean.FALSE;
            }
            case Term.Wortel w -> {
                Boolean eerst = leestEerst(w.radicand(), slot);
                yield eerst != null ? eerst : Boolean.FALSE;
            }
            case Term.Vereist v -> {
                Boolean eerst = leestEerst(v.waarde(), slot);
                yield eerst != null ? eerst : Boolean.FALSE;
            }
            case Term.Tekstreeks s -> leestEerst(s.delen(), slot);
            case Term.GelijsteVouw g -> leestEerst(g.operanden(), slot);
            // the value of a nested Let is evaluated at the first read of its own binder, which gives FALSE above
            case Term.Let l -> leestEerst(l.lichaam(), slot);
            // the condition comes first and a branch is conditional; what the condition reads is not looked into, so the
            // choice counts as something else first, as before the conditions were planned
            case Term.Keuze k -> Boolean.FALSE;
            // a composite is a condition, which stands in a value only as the condition of a choice, answered above; its
            // members after the first are read only where no stop came before them
            case Term.Samengesteld s -> Boolean.FALSE;
            // so are the verdict of a quantifier fold that can fault and deposit, and a condition tested without its
            // fault, which stands only in a test a form composes itself
            case Term.Kwantorlezing k -> Boolean.FALSE;
            case Term.ZonderFout z -> Boolean.FALSE;
            // the analysis adds the node only where the fold's filter can deposit or a fault can stand, before the value
            case Term.Vouwlezing v -> Boolean.FALSE;
            case Term.Faal f -> Boolean.FALSE;
        };
    }

    private static Boolean leestEerst(List<Term> termen, int slot) {
        for (Term t : termen) {
            Boolean eerst = leestEerst(t, slot);
            if (eerst != null) return eerst;
        }
        return null;
    }

    /** A read of the binder of the nearest enclosing {@code Let} of that name (WF-064): it forces the need and raises
     *  the {@code Let}'s barrier where the value is leeg ({@link Term.Binder}). A binder of a {@code Let} around a
     *  fold, read in the fold's filter or matrix, is in scope but bound in another rule than this contribution rule
     *  ({@link Lezingen#buiten}), and refused as a form this branch does not carry yet. */
    private Term binderlezing(Expr.BinderRef b, Lezingen lez) {
        String naam = b.naam() == null ? null : Namen.norm(b.naam());
        Integer slot = naam == null ? null : lez.binders.get(naam);
        if (slot == null && naam != null && lez.buiten.contains(naam))
            throw new Weigering("een BinderRef '" + b.naam() + "' van een Let om een vouw, in het filter of de matrix"
                    + " van die vouw ('" + snippet(b.bron()) + "'): de bijdrageregels van de vouw rekenen het filter en"
                    + " de matrix per lid uit en binden de waarde van de Let niet: " + NOG_NIET);
        if (slot == null)
            throw new Weigering("een BinderRef '" + b.naam() + "' zonder omsluitende Let die hem bindt ('"
                    + snippet(b.bron()) + "', WF-064)");
        return new Term.Binder(slot, b.naam(), bron(b.bron()));
    }

    /** An {@code IfThenElse} (RUNTIME-SEMANTICS ch. 10.2): the condition evaluated as any condition of this branch, then
     *  exactly the branch it chooses; the other branch evaluates nothing, so it reads no binder, faults nowhere and
     *  deposits nothing. The reads of both branches are bound in the left-hand side all the same. */
    private Term keuze(Expr.IfThenElse k, Lezingen lez) {
        Term voorwaarde = voorwaarde(k.conditie(), lez);
        Term dan = waarde(k.dan(), lez), anders = waarde(k.anders(), lez);
        return new Term.Keuze(voorwaarde, dan, anders, bron(k.bron()));
    }

    /** A context value (RUNTIME-SEMANTICS ch. 8.2): evaluationDate, the session's evaluation date, and evaluationYear,
     *  its calendar year as a whole number without a unit ({@code rsRekenjaar}). Both read the session's
     *  {@link Rekendatum}, bound in the left-hand side, and fault where the session carries no date, in the reference
     *  interpreter's sentences. The bound day of a day-kind definition, read outside such a definition, faults as in that
     *  executor, since it denotes nothing there; a day-kind definition itself is outside this branch. */
    private Term contextwaarde(Expr.Constante c, Lezingen lez) {
        String naam = c.naam() == null ? "" : c.naam();
        String bron = bron(c.bron());
        switch (naam) {
            case "Rekendatum" -> {
                int i = lez.rekendatum();
                lez.markeerFalen();
                return new Term.Vereist(new Term.Lees(i, null), Rekenfout.REKENDATUM_ZONDER_DATUM, bron);
            }
            case "Rekenjaar" -> {
                int i = lez.rekendatum();
                lez.markeerFalen();
                return new Term.Aanroep(Functie.RS_REKENJAAR, List.of(new Term.Vereist(new Term.Lees(i, null),
                        RegelSpraakRuntime.REKENJAAR_ZONDER_REKENDATUM, null)), bron);
            }
            case "de dag" -> {
                lez.markeerFalen();
                return new Term.Faal(Rekenfout.DE_DAG_BUITEN_DAGSOORT, bron);
            }
            default -> throw new Weigering("de contextwaarde '" + naam + "' ('" + snippet(c.bron()) + "'): de lader laat"
                    + " alleen evaluationDate, evaluationYear en de gebonden dag toe (WF-098)");
        }
    }

    // ---------------- aggregations ----------------

    /** A fold this branch lowers: the key its contributions carry ({@link Bijdrage#vouw}), its contribution rules (one
     *  per range, beside the filter fault rule of a filter that can fault and the filter deposit rule of a filter that
     *  can deposit), the reads of those rules, registered when a form that reads the fold lowers
     *  ({@link Lezingen#vastleggen}), whether its filter can fault, in which case the rule that reads it binds its
     *  {@link Filterfout} facts as well, and whether its filter can deposit, in which case that rule counts its
     *  {@link Filterdeponering} facts; for a quantifier fold, whether its matrix can fault and whether it can deposit,
     *  in which case its contributions carry a {@link Lidtoets} ({@link #kwantorvouw}); and for a per-element fold over
     *  an attribute on a timeline, that a member's value can fault, in which case the rule that reads it binds its
     *  {@link Lidfout} facts as well ({@link #lidfoutregel}). */
    private record Vouw(String sleutel, List<Rule> regels, List<Lezingen> lezingen, boolean kanFalen, boolean deponeert,
                        boolean lidKanFalen, boolean lidDeponeert, boolean waardeKanFalen) {
        /** A fold over a set source or a union: no matrix per member. */
        Vouw(String sleutel, List<Rule> regels, List<Lezingen> lezingen, boolean kanFalen, boolean deponeert) {
            this(sleutel, regels, lezingen, kanFalen, deponeert, false, false, false);
        }

        /** A quantifier fold: a matrix per member, and no value per member. */
        Vouw(String sleutel, List<Rule> regels, List<Lezingen> lezingen, boolean kanFalen, boolean deponeert,
             boolean lidKanFalen, boolean lidDeponeert) {
            this(sleutel, regels, lezingen, kanFalen, deponeert, lidKanFalen, lidDeponeert, false);
        }

        /** Whether the rule that reads the fold replays its contributors in order: a quantifier fold whose matrix can
         *  fault or deposit ({@link Lidtoets}). */
        boolean geordend() {
            return lidKanFalen || lidDeponeert;
        }
    }

    /** The kinds of an aggregation, as against the builtins that share the node. */
    private static boolean isVouw(Expr.FunctieKind k) {
        return switch (k) {
            case SOM, TOTAAL, AANTAL, AANTAL_GEVULDE, MINIMUM, MAXIMUM, EERSTE, LAATSTE -> true;
            default -> false;
        };
    }

    /** The listed fold, "de eerste van A en B": two or more operands and no filter, as against a fold over a set
     *  source; told apart by the shape, as RUNTIME-SEMANTICS ch. 1.2e does, since the element type the producer stamps
     *  on it is vestigial. */
    private static boolean isGelisteVouw(Expr.Functie f) {
        boolean ordenend = switch (f.kind()) { case MINIMUM, MAXIMUM, EERSTE, LAATSTE -> true; default -> false; };
        return ordenend && f.argumenten().size() >= 2 && f.filter() == null;
    }

    /** An aggregation, listed, over a set source or over a dimension selection; every other function node is a builtin
     *  call ({@link #builtin}). */
    private Term functie(Expr.Functie f, Lezingen lez) {
        if (f.dimensieSelectie() != null) return dimensievouw(f, lez);
        if (!isVouw(f.kind())) return builtin(f, lez);
        return isGelisteVouw(f) ? gelijsteVouw(f, lez) : vouw(f, lez);
    }

    /**
     * A builtin call (IR-SPEC ch. 4.3), its arguments evaluated in wire order (RUNTIME-SEMANTICS ch. 5.11), through the
     * runtime's helpers: the absolute value ({@code rsAbs}); a concatenation of
     * texts ({@link #tekstdeel}); the duration and the absolute duration in a calendar unit ({@code rsTijdsduur},
     * ch. 3.8); the year, month and day of a date and the hour, minute, second and millisecond of a date-time
     * ({@code rsDatumDeel}); easterSunday of a whole year ({@link #paasdag}); a date and a date-time made of their parts
     * ({@link #datumconstructie}). A square root is computed as the base of its mandatory rounding ({@link #afronding}),
     * and without one exactly ({@link #exacteWortel}). A bucketing builtin over a timeline is the value of a temporal
     * target and nothing else ({@link #tijdlijnwaarde}), so here it is refused by name, and a duration in day kinds
     * belongs to the card of day kinds and is refused by name.
     */
    private Term builtin(Expr.Functie f, Lezingen lez) {
        Expr.FunctieKind kind = f.kind();
        String spelling = Spelling.BUILTIN_KIND.spelling(kind);
        if (kind == Expr.FunctieKind.TIJDSEVENREDIG_DEEL || kind == Expr.FunctieKind.AANTAL_DAGEN_IN)
            throw nietBerekend("BuiltinCall " + spelling, f.bron(), "deze tak rekent een tijdsevenredig"
                    + " deel en een dagentelling alleen als de tijdlijn van een tijdsafhankelijk doel (RUNTIME-SEMANTICS"
                    + " ch. 3.7, 3.8, 3.11)");
        if (f.filter() != null)
            throw new Weigering("een BuiltinCall " + spelling + " met een filter ('" + snippet(f.bron()) + "'): alleen een"
                    + " bucketing over een tijdlijn draagt er een");
        List<Expr> a = f.argumenten();
        return switch (kind) {
            case WORTEL -> exacteWortel(f, lez);
            case ABSOLUTE_WAARDE -> new Term.Aanroep(Functie.RS_ABS, List.of(waarde(eenArgument(f, spelling), lez)),
                    bron(f.bron()));
            case CONCATENATIE -> {
                List<Term> delen = new ArrayList<>();
                for (Expr arg : a) delen.add(tekstdeel(arg, lez, "concatenation"));
                yield new Term.Tekstreeks(delen, bron(f.bron()));
            }
            case TIJDSDUUR, ABSOLUTE_TIJDSDUUR -> tijdsduur(f, lez, spelling);
            case EERSTE_PAASDAG -> paasdag(f, lez, spelling);
            case JAAR_UIT -> datumdeel(f, lez, spelling, ChronoField.YEAR, false);
            case MAAND_UIT -> datumdeel(f, lez, spelling, ChronoField.MONTH_OF_YEAR, false);
            case DAG_UIT -> datumdeel(f, lez, spelling, ChronoField.DAY_OF_MONTH, false);
            case UUR_UIT -> datumdeel(f, lez, spelling, ChronoField.HOUR_OF_DAY, true);
            case MINUUT_UIT -> datumdeel(f, lez, spelling, ChronoField.MINUTE_OF_HOUR, true);
            case SECONDE_UIT -> datumdeel(f, lez, spelling, ChronoField.SECOND_OF_MINUTE, true);
            case MILLISECONDE_UIT -> datumdeel(f, lez, spelling, ChronoField.MILLI_OF_SECOND, true);
            case DATUM_MET, DATUMTIJD_MET -> datumconstructie(f, lez, spelling);
            default -> throw new IllegalStateException("de soort " + kind + " is een vouw, geen builtin");
        };
    }

    /** A square root that is not the base of a rounding, exact as the reference interpreter computes it: the root of
     *  a radicand that is the square of a rational, and otherwise the fault that executor gives, since an irrational
     *  root exists only under the rounding §13.4.17.13 mandates (RUNTIME-SEMANTICS ch. 2.2). A leeg radicand is leeg,
     *  a negative one leeg with the §6.6 deposit where the radicand qualifies and the evaluation reaches it (ch. 1.2,
     *  {@link Sonde}). The destructive lowering computed a binary approximation here. */
    private Term exacteWortel(Expr.Functie f, Lezingen lez) {
        if (f.argumenten().size() != 1)
            throw new Weigering("een squareRoot met " + f.argumenten().size() + " argumenten ('" + snippet(f.bron())
                    + "'): de wortel heeft er één (WF-037)");
        lez.markeerFalen();
        Term radicand = waarde(f.argumenten().get(0), lez);
        int sonde = deponeerplek(f) ? lez.sonde() : -1;
        return new Term.Wortel(Functie.EXACTE_WORTEL, radicand, sonde, bron(f.bron()));
    }

    /** The one argument of {@code f}; a call with another number is refused (WF-037). */
    private static Expr eenArgument(Expr.Functie f, String spelling) {
        if (f.argumenten().size() != 1)
            throw new Weigering("een " + spelling + " met " + f.argumenten().size() + " argumenten ('" + snippet(f.bron())
                    + "'): deze builtin heeft er één (WF-037)");
        return f.argumenten().get(0);
    }

    /** The duration and the absolute duration from argument 0 to argument 1 in whole units of the node's own unit
     *  (RUNTIME-SEMANTICS ch. 3.8, WF-038), the "van" evaluated before the "tot" (ch. 5.11); a leeg operand leeg. A unit
     *  that is a day kind counts the days of the kind ({@link #dagsoortduur}). */
    private Term tijdsduur(Expr.Functie f, Lezingen lez, String spelling) {
        List<Expr> a = f.argumenten();
        if (a.size() != 2)
            throw new Weigering("een " + spelling + " met " + a.size() + " argumenten ('" + snippet(f.bron()) + "'): een"
                    + " tijdsduur heeft er twee (WF-037)");
        Datatype van = eff.typeVan(a.get(0)), tot = eff.typeVan(a.get(1));
        if (!van.isDatum() || !tot.isDatum() || !zelfdeDatumsoort(van, tot))
            throw new Weigering("een " + spelling + " die niet twee datums van één precisie meet ('" + snippet(f.bron())
                    + "', WF-038)");
        Datatype type = eff.typeVan(f);
        // ch. 3.8 puts the calendar table first, as Tijdrekening.dagsoortVoorEenheid looks a spelling up for every branch
        String dagsoort = Tijdrekening.dagsoortVoorEenheid(model, type.eenheidOf());
        if (dagsoort != null) return dagsoortduur(f, lez, spelling, dagsoort, van instanceof Datatype.DatumTijd);
        String eenheid = Tijdrekening.chronoUnit(type);
        if (eenheid == null)
            throw new Weigering("een " + spelling + " zonder tijdseenheid ('" + snippet(f.bron()) + "', WF-038)");
        boolean klok = switch (eenheid) { case "HOURS", "MINUTES", "SECONDS", "MILLIS" -> true; default -> false; };
        if (klok && !(van instanceof Datatype.DatumTijd))
            throw new Weigering("een " + spelling + " in een klokeenheid tussen datums zonder tijd ('" + snippet(f.bron())
                    + "', WF-038)");
        Term vanaf = waarde(a.get(0), lez), totaan = waarde(a.get(1), lez);
        boolean absoluut = f.kind() == Expr.FunctieKind.ABSOLUTE_TIJDSDUUR;
        return new Term.Aanroep(Functie.RS_TIJDSDUUR, List.of(vanaf, totaan, constante(eenheid), constante(absoluut)),
                bron(f.bron()));
    }

    /**
     * The duration from argument 0 to argument 1 in whole days of the day kind {@code dagsoort} (RUNTIME-SEMANTICS
     * ch. 3.8, §6.10), as the runtime counts it for the other branches ({@code rsTijdsduurDagsoort}): the days of the
     * kind after the start up to and including the end, signed by the order of the two, the absolute duration without
     * the sign, a leeg operand leeg. The interval bounds the count, so it has no reach and no fault. The predicate is a
     * constant of the document ({@link #dagsoortpredikaat}). Over two date-times the form is refused by name, as the
     * reference interpreter and the ASP backend refuse it: counting whole days of a kind between two instants needs a
     * rule for the part-day at each end, which ch. 3.8 does not fix, and the refusal does not wait for a filled operand.
     */
    private Term dagsoortduur(Expr.Functie f, Lezingen lez, String spelling, String dagsoort, boolean datumTijd) {
        String wat = "een " + spelling + " in dagsoorten ('" + snippet(f.bron()) + "')";
        if (datumTijd)
            throw new Weigering(wat + " tussen twee datum-tijden: de referentie-interpreter en de ASP-backend weigeren haar"
                    + " met naam, omdat het tellen van hele dagen van een soort tussen twee tijdstippen een regel voor de"
                    + " deeldag aan elk eind vraagt, die RUNTIME-SEMANTICS ch. 3.8 niet vastlegt (ch. 3.12)");
        java.util.function.Predicate<LocalDate> predikaat = dagsoortpredikaat(dagsoort, wat);
        Term vanaf = waarde(f.argumenten().get(0), lez), totaan = waarde(f.argumenten().get(1), lez);
        boolean absoluut = f.kind() == Expr.FunctieKind.ABSOLUTE_TIJDSDUUR;
        return new Term.Aanroep(Functie.RS_TIJDSDUUR_DAGSOORT, List.of(vanaf, totaan,
                new Term.Dagsoort(dagsoort, predikaat, null), constante(absoluut)), bron(f.bron()));
    }

    /** A part of a date or a date-time, as a whole number without a unit; a leeg date leeg (RUNTIME-SEMANTICS ch. 1.2,
     *  3.8). A clock part demands a date-time. */
    private Term datumdeel(Expr.Functie f, Lezingen lez, String spelling, ChronoField veld, boolean klok) {
        Expr arg = eenArgument(f, spelling);
        Datatype t = eff.typeVan(arg);
        if (!t.isDatum() || (klok && !(t instanceof Datatype.DatumTijd)))
            throw new Weigering("een " + spelling + " uit iets anders dan een " + (klok ? "datum-tijd" : "datum") + " ('"
                    + snippet(f.bron()) + "')");
        Term d = waarde(arg, lez);
        return new Term.Aanroep(Functie.RS_DATUM_DEEL, List.of(d, constante(veld)), bron(f.bron()));
    }

    /** easterSunday of a whole year without a unit (RUNTIME-SEMANTICS ch. 3.8, WF-038), by the algorithm of that chapter
     *  ({@link Rekenhulpen#paasdag}); a leeg year leeg. A year that is not whole is a fault in the reference interpreter's
     *  sentence, never a silent truncation, and so is a year outside the years this backend's dates hold. */
    private Term paasdag(Expr.Functie f, Lezingen lez, String spelling) {
        Expr arg = eenArgument(f, spelling);
        Eenheid eenheid = eff.eenheidVan(arg);
        if (eenheid != null)
            throw new Weigering("een " + spelling + " van een jaar met de eenheid " + eenheid.weergave() + " ('"
                    + snippet(f.bron()) + "'): het jaar is een geheel getal zonder eenheid (WF-038)");
        lez.markeerFalen();
        Term jaar = waarde(arg, lez);
        return new Term.Aanroep(Functie.PAASDAG, List.of(jaar), bron(f.bron()));
    }

    /** A date of three parts or a date-time of seven, through the runtime's {@code rsDatumMet} and
     *  {@code rsDatumtijdMet}: every part evaluated in order (RUNTIME-SEMANTICS ch. 5.11), any leeg part leeg (ch.
     *  1.2). A part that is not a whole number within the range of a date part, or parts that name no day, fault: the
     *  reference interpreter makes no such date, and the runtime would truncate the part or throw past the fire
     *  ({@link Rekenhulpen#datumMet}). */
    private Term datumconstructie(Expr.Functie f, Lezingen lez, String spelling) {
        boolean tijd = f.kind() == Expr.FunctieKind.DATUMTIJD_MET;
        int aantal = tijd ? 7 : 3;
        if (f.argumenten().size() != aantal)
            throw new Weigering("een " + spelling + " met " + f.argumenten().size() + " argumenten ('" + snippet(f.bron())
                    + "'): deze builtin heeft er " + aantal + " (WF-037)");
        lez.markeerFalen();
        List<Term> argumenten = new ArrayList<>();
        // the spelling first: the sentence of a fault names the builtin
        argumenten.add(constante(spelling));
        for (Expr arg : f.argumenten()) argumenten.add(waarde(arg, lez));
        return new Term.Aanroep(tijd ? Functie.DATUMTIJD_MET : Functie.DATUM_MET, argumenten, bron(f.bron()));
    }

    /**
     * A listed fold (RUNTIME-SEMANTICS ch. 1.2e): minimum and eerste keep the least operand value, maximum and laatste
     * the greatest; a leeg operand is skipped, an all-leeg fold is leeg, and a tie takes the later operand. The operands
     * are computed from the values the rule binds, in wire order, each after the first restated into the first one's
     * unit, the unit of the result (§4.6 Eenheid1, RUNTIME-SEMANTICS ch. 2.6).
     */
    private Term gelijsteVouw(Expr.Functie f, Lezingen lez) {
        boolean kleinste = f.kind() == Expr.FunctieKind.MINIMUM || f.kind() == Expr.FunctieKind.EERSTE;
        Eenheid eerste = eff.eenheidVan(f.argumenten().get(0));
        List<Term> operanden = new ArrayList<>();
        for (int i = 0; i < f.argumenten().size(); i++) {
            Expr a = f.argumenten().get(i);
            Term op = waarde(a, lez);
            operanden.add(i == 0 ? op : herschaald(op, eff.eenheidVan(a), eerste, "een §5.8.3 operand van een gelijste vouw"));
        }
        return new Term.GelijsteVouw(kleinste ? Functie.KLEINSTE : Functie.GROOTSTE, operanden, bron(f.bron()));
    }

    /**
     * A fold over a set source (RUNTIME-SEMANTICS ch. 1.2 "Aggregations", ch. 3.4a-d): an accumulate in this rule's
     * left-hand side over the contributions ({@link Bijdrage}) that the fold's contribution rules insert for this rule's
     * subject, one per member of the range that passes the filter ({@link #bijdrageregels}). A contribution rule binds the
     * member's value and every read of the filter in its own left-hand side, so a member, or a value derived for it, that
     * changes takes its contribution along, and the accumulate here follows; this rule computes only with what the
     * accumulate binds. A value fold skips a leeg member, which therefore contributes nothing, and a count counts every
     * member. Over no contribution a sum is leeg or its fallback, which is computed whether it is used or not
     * (ch. 5.11), a total 0, both counts 0 and an ordering fold leeg; eerste and laatste are the least and the greatest
     * value. A count over an instance-set union counts the members its selections select together ({@link #unievouw}).
     *
     * <p>A filter that faults for a member is a {@link Filterfout} of the member, no fault: the reference interpreter
     * evaluates the filter only where it evaluates the fold, per member in fold order, and stops at the first fault
     * (RUNTIME-SEMANTICS ch. 5.11). This rule binds the first of those facts in fold order on its subject, and the
     * fold's value faults with that division where it is computed, before the fallback, which the reference
     * interpreter evaluates after the members ({@link #gelezenVouw}). The fault is then this rule's, as a division in its
     * own value or condition is: inserted by its consequence or by its fault rule ({@link #voorwaardefoutregel}), and
     * only where its evaluation reaches the fold. A filter that deposits for a member is a {@link Filterdeponering} of
     * the member in the same way: the reference interpreter evaluates the filter with this rule's collector, so the
     * deposit is this rule's, on its subject, where its evaluation reaches the fold ({@link #gelezenVouw}).
     *
     * <p>A total without a range reads the subject's own timeline ({@link #eigenTotaal}). A sum or a total over a member
     * on a timeline folds twice (RUNTIME-SEMANTICS ch. 3.6, the per-element fold): each member's timeline collapses over
     * time first, at its own granularity, in the contribution rule, which contributes that scalar, and a member without
     * a timeline contributes nothing; the fold over the set is the ordinary one. Every other fold over a member on a
     * timeline is refused by name, as the reference interpreter refuses it: it has no moment at which to rank, count or
     * test the timeline.
     */
    private Term vouw(Expr.Functie f, Lezingen lez) {
        Expr.FunctieKind kind = f.kind();
        String fragment = bron(f.bron());
        if (kind == Expr.FunctieKind.AANTAL && f.argumenten().size() == 1
                && f.argumenten().get(0) instanceof Expr.InstanceSetUnion u) {
            Vouw v = unievouw(f, u, lez);
            int[] i = lez.vouw(v, null);
            return gelezenVouw(v, i, lez, new Term.Aanroep(Functie.AANTAL, List.of(new Term.Lees(i[0], null)), fragment),
                    fragment);
        }
        Objecttype elem = f.elementObjecttype();
        if (elem == null) throw new Weigering("een Aggregation " + kind + " zonder elementtype ('" + snippet(f.bron()) + "')");
        if (f.argumenten().size() != 1)
            throw new Weigering("een Aggregation " + kind + " over " + f.argumenten().size() + " operanden met een"
                    + " elementtype ('" + snippet(f.bron()) + "'): een vouw over een bereik heeft één argument (WF-037)");
        if (f.leegwaarde() != null && kind != Expr.FunctieKind.SOM)
            throw new Weigering("een emptyFallback op een Aggregation " + kind + " ('" + snippet(f.bron()) + "'): alleen"
                    + " een som heeft er een (WF-037)");
        boolean telling = kind == Expr.FunctieKind.AANTAL;
        Expr.AttribuutRef arg = null;
        if (!telling) {
            if (!(f.argumenten().get(0) instanceof Expr.AttribuutRef a) || a.attribuut() == null)
                throw new Weigering("een Aggregation " + kind + " over iets anders dan een attribuut van het element ('"
                        + snippet(f.bron()) + "')");
            arg = a;
        }
        boolean metBereik = bronPaden.containsKey(f) || rolBindingen.containsKey(f);
        if (kind == Expr.FunctieKind.TOTAAL && !metBereik) return eigenTotaal(f, lez);
        if (arg != null && Namen.bevatNaam(elem.granulariteitLeden(), arg.attribuut())
                && kind != Expr.FunctieKind.SOM && kind != Expr.FunctieKind.TOTAAL)
            throw nietBerekend("Aggregation " + kind + " over het tijdsafhankelijke attribuut '" + arg.attribuut() + "'",
                    f.bron(), "zij heeft geen moment waarop zij een tijdlijn rangschikt, telt of toetst; alleen een som en"
                    + " een totaal vouwen de tijdlijn van elk lid eerst over de tijd (RUNTIME-SEMANTICS ch. 3.4c, 3.6)");
        if (!metBereik)
            throw new Weigering("een Aggregation " + kind + " zonder bereik ('" + snippet(f.bron()) + "'): IR-SPEC ch. 4.3"
                    + " eist een sourceRole of sourcePath");
        Expr.AttribuutRef lid = arg;
        // a member on a timeline the case gives as the asOfDate-bucket scalar faults, naming the first such member in
        // fold order (RUNTIME-SEMANTICS ch. 3.6, 8.4), and a made link has no place in that order
        boolean tijdlijnlid = lid != null && gedeclareerd(elem, lid.attribuut(), false).tijd() != null;
        // a member on a timeline is asked by the per-element fold, which refuses one that is no number (integreerbaar)
        if (lid != null && !tijdlijnlid) {
            String setbron = setbronweigering(kind, elem, lid);
            if (setbron != null) throw new Weigering(setbron);
        }
        RolBindingRef bron = rolBindingen.get(f);
        if (tijdlijnlid && bron != null && bron.feittype() != null
                && gecreeerdeRelaties.contains(Namen.norm(bron.feittype())))
            throw new Weigering("een " + kind + " per lid over het tijdsafhankelijke attribuut '" + lid.attribuut()
                    + "' in een vouw over de gecreëerde relatie '" + bron.feittype() + "' ('" + snippet(f.bron()) + "'): een"
                    + " gemaakte link heeft geen plaats in de vouwvolgorde, die bepaalt welk lid met een emmerscalair als"
                    + " invoer de vouw noemt (RUNTIME-SEMANTICS ch. 3.6, 5.11, 8.4); " + NOG_NIET + ", en geen golden met"
                    + " cases heeft het");
        Vouw v = vouwVoor(f, lez, (sleutel, naam) -> {
            List<Rule> regels = new ArrayList<>();
            List<Lezingen> lezingen = new ArrayList<>();
            boolean kanFalen = bijdrageregels(sleutel, naam, kind, lez.subject, elem, bereik(f, lez.subject, elem), 0,
                    f.filter(), lid, regels, lezingen);
            return new Vouw(sleutel, List.copyOf(regels), List.copyOf(lezingen), kanFalen, deponeert(f.filter()), false,
                    false, tijdlijnlid);
        });
        Accumulatie.Soort functie = switch (kind) {
            case SOM, TOTAAL -> Accumulatie.Soort.SOM;
            case MINIMUM, EERSTE -> Accumulatie.Soort.MINIMUM;
            case MAXIMUM, LAATSTE -> Accumulatie.Soort.MAXIMUM;
            default -> null;
        };
        int[] i = lez.vouw(v, functie);
        // the fallback restated into the unit of the folded member (RUNTIME-SEMANTICS ch. 2.6)
        Term terugval = f.leegwaarde() == null ? null : herschaald(waarde(f.leegwaarde(), lez),
                eff.eenheidVan(f.leegwaarde()), arg == null ? null : eff.eenheidVan(arg), "een §5.8.2 terugvalwaarde");
        Term aantal = new Term.Lees(i[0], null);
        // over no contribution a sum is its fallback or leeg, a total 0 and an ordering fold leeg
        Term uitkomst = switch (kind) {
            case AANTAL, AANTAL_GEVULDE -> new Term.Aanroep(Functie.AANTAL, List.of(aantal), fragment);
            case SOM -> vouwwaarde(aantal, i[1], terugval == null ? constante(null) : terugval, fragment);
            case TOTAAL -> vouwwaarde(aantal, i[1], constante(BigDecimal.ZERO), fragment);
            default -> vouwwaarde(aantal, i[1], constante(null), fragment);
        };
        return gelezenVouw(v, i, lez, uitkomst, fragment);
    }

    /** The value of a fold from the count {@code aantal} and the folded value at slot {@code waarde}, with
     *  {@code terugval} over no contribution ({@link Rekenhulpen#vouwwaarde}). */
    private static Term vouwwaarde(Term aantal, int waarde, Term terugval, String bron) {
        return new Term.Aanroep(Functie.VOUWWAARDE, List.of(aantal, new Term.Lees(waarde, null), terugval), bron);
    }

    /**
     * Why a value fold of {@code kind} over the members {@code arg} of {@code elem} has no reading, or null where it has
     * one: an additive fold over members that are no number (WF-091), and an ordering fold over members that are neither
     * a number nor a date (WF-038). The reference interpreter faults on the addition or the comparison itself and the
     * ASP runner declines the form, while this branch would hand the members to a BigDecimal sum, which fails at fire,
     * or to the Java order of their values, which answers. The sentence opens as the DRL-text branch opens its own
     * ({@code Vouwwaarde}), so a gate report names the same form in both.
     */
    private String setbronweigering(Expr.FunctieKind kind, Objecttype elem, Expr.AttribuutRef arg) {
        boolean optellend = kind == Expr.FunctieKind.SOM || kind == Expr.FunctieKind.TOTAAL;
        boolean ordenend = kind == Expr.FunctieKind.MINIMUM || kind == Expr.FunctieKind.MAXIMUM
                || kind == Expr.FunctieKind.EERSTE || kind == Expr.FunctieKind.LAATSTE;
        Datatype t = gedeclareerd(elem, arg.attribuut(), false).datatype();
        String kop = "vouw " + kind + " over de setbron '" + arg.attribuut() + "' van " + elem.naam
                + ": die leden zijn van type " + t.naam();
        if (optellend && !t.isGetalachtig())
            return kop + " en niet getalachtig; optellen is rekenkunde over getallen (WF-091), en de"
                    + " referentie-interpreter faalt op de optelling zelf: geen enkele uitvoerder draagt deze vorm";
        if (ordenend && !t.isOrdenbaar())
            return kop + " en kennen geen ordening; een ordenende vouw vraagt een getal of een datum (WF-038), en de"
                    + " referentie-interpreter faalt op de vergelijking zelf: geen enkele uitvoerder draagt deze vorm";
        return null;
    }

    /**
     * The temporal total of the subject's own timeline (RUNTIME-SEMANTICS ch. 3.6): "het totaal van zijn X", optionally
     * "gedurende" a clause, as a function of the values this rule binds: the timeline of X, through its standing
     * ({@link Lezingen#tijdlijn}), and every timeline and value the clause reads. The two routes of the total take the two
     * conventions of the case input, and a form that needs the one and finds the other faults ({@link Invoervormfout},
     * ch. 8.4). Without a clause the total takes the asOfDate-bucket scalar, the session's date being the horizon: it is
     * {@code rsTijdTotaal} of the one period of X's grid that holds the evaluation date ({@link Emmerscalair}), the
     * segment's value times the whole units it spans, and leeg where X has no input; a full timeline faults, in the
     * reference interpreter's words, and so does a session without an evaluation date, where the total has no bucket to
     * take, X filled or not: this route reads the date ({@link Lezingen#rekendatum}), and the reference interpreter tests
     * the full timeline first, then the date, then the value. With a clause the total reads the full timeline: it is
     * clipped to the clause's true
     * periods first ({@link #knip}) and then totalled per clipped fragment, so an absent timeline totals 0 (ch. 3.6: the
     * clip's presence turns absence into 0), and the bucket scalar faults, before the clause is evaluated, as in the
     * reference interpreter. The runtime's helpers compute this, with {@link Tijdvak} as their segment class.
     *
     * <p>Refused by name: an X over a path or of another type than the subject (WF-083 (c)), one that is not a number
     * (WF-091), a rate over a grid whose unit the document does not declare as one unit or whose denominator is not that
     * unit ({@link #integreerbaar}), and the total without a clause over a timeline a form writes: the reference
     * interpreter takes that total only of the case's scalar on the period of the evaluation date and faults on a full
     * timeline (ch. 3.6), so no executor confirms it, and no golden has it.
     */
    private Term eigenTotaal(Expr.Functie f, Lezingen lez) {
        String wat = "een totaal over de eigen tijdlijn ('" + snippet(f.bron()) + "')";
        Expr.AttribuutRef a = (Expr.AttribuutRef) f.argumenten().get(0);
        if (a.objecttype() != lez.subject || navigatieBindingen.containsKey(a))
            throw new Weigering(wat + " over een attribuut van een ander object dan het onderwerp: zonder bereik leest"
                    + " het totaal de tijdlijn van het onderwerp zelf (WF-083 (c)); " + NOG_NIET + " over een pad");
        Lid lid = integreerbaar(tijdlid(lez.subject, a.attribuut(), false, wat), wat);
        if (f.filter() == null && schrijvers.containsKey(lid.sleutel()))
            throw new Weigering(wat + " zonder knip over de tijdlijn van '" + lid.naam() + "', die een vorm schrijft:"
                    + " de referentie-interpreter neemt het totaal zonder knip alleen van de scalaire invoer op de periode"
                    + " van de rekendatum en weigert een volle tijdlijn (RUNTIME-SEMANTICS ch. 3.6); " + NOG_NIET
                    + ", en geen golden met cases heeft het");
        int tijdlijn = lez.tijdlijn(lid);
        int onderwerp = lez.lid();
        lez.markeerFalen();
        String gran = lid.tijd().name();
        String naam = lid.naam();
        String bron = bron(f.bron());
        if (f.filter() == null) {
            int datum = lez.rekendatum();
            return new Term.Aanroep(Functie.EIGEN_TOTAAL, List.of(new Term.Lees(tijdlijn, bron(a.bron())),
                    new Term.Lees(onderwerp, null), new Term.Lees(datum, null), constante(naam), constante(gran)), bron);
        }
        Term geknipt = knip(f.filter(), lez, wat, naam, volledig(tijdlijn, "the gedurende clip of \"" + naam
                + "\" needs the full timeline; ", onderwerp, " supplies only the asOfDate-bucket scalar", bron(a.bron())));
        return new Term.Aanroep(Functie.RS_TIJD_TOTAAL, List.of(geknipt, constante(gran)), bron);
    }

    /** The timeline bound at slot {@code tijdlijn}, which a form reads whole, or the fault in the sentence {@code voor},
     *  the subject bound at slot {@code onderwerp}, {@code na}, where the case gives that input as the asOfDate-bucket
     *  scalar ({@link Emmerscalair}, RUNTIME-SEMANTICS ch. 8.4; {@link Rekenhulpen#volledig}). */
    private static Term volledig(int tijdlijn, String voor, int onderwerp, String na, String bron) {
        return new Term.Aanroep(Functie.VOLLEDIG, List.of(new Term.Lees(tijdlijn, bron), constante(voor),
                new Term.Lees(onderwerp, null), constante(na)), bron);
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
     * bound off the bean ({@link Lezingen#tijdlijn}), the compared timeline through its standing, read whole
     * ({@link #vergelijkingsknip}), and the bound like any value of the rule. {@code geknipt} names the attribute whose
     * timeline is clipped, and {@code tijdlijn} computes that timeline, which the clip takes first.
     *
     * <p>Refused by name: any other clause, a compound other than {@code all}, a check navigated or with an owner or a
     * whole-period qualifier (WF-097), a comparison whose left side is not an own attribute on a timeline, a date
     * ordering, units that differ between the two sides, and the comparisons "gelijk aan" and "ongelijk aan": the
     * runtime's {@code rsTijdConditie} reads an empty side, a gap in the compared timeline or a leeg bound, as no
     * period, where ch. 3.6 and the reference interpreter apply the comparison of ch. 1.2 per period, so that two leeg
     * sides are equal and exactly one leeg side is unequal (ch. 3.6, the worked example of 500,00); no golden has
     * either.
     */
    private Term knip(Conditie c, Lezingen lez, String wat, String geknipt, Term tijdlijn) {
        if (c instanceof Conditie.Kenmerk k) {
            if (k.eigenaar() != null || k.periode() != null)
                throw new Weigering(wat + " met een kenmerk met een eigenaar of een geheleperiodetoets in de knip ('"
                        + snippet(k) + "', WF-097)");
            if (k.navigatie() != null || navigatieBindingen.containsKey(k))
                throw new Weigering(wat + " met een genavigeerd kenmerk in de knip ('" + snippet(k) + "'): " + NOG_NIET
                        + "; geen golden met cases heeft het");
            int perioden = lez.tijdlijn(tijdlid(lez.subject, k.kenmerk(), true, wat));
            return new Term.Aanroep(k.negatie() ? Functie.RS_TIJD_KNIP_NIET : Functie.RS_TIJD_KNIP,
                    List.of(tijdlijn, new Term.Lees(perioden, null), constante(Tijdvak.class)), bron(k.bron()));
        }
        if (c instanceof Conditie.Samengesteld s && s.kwantor() == Conditie.Kwantor.ALLE && !s.onderdelen().isEmpty()) {
            // each check clips what the one before it left
            Term uit = tijdlijn;
            for (Conditie deel : s.onderdelen()) {
                if (!(deel instanceof Conditie.Kenmerk))
                    throw new Weigering(wat + " met een lid van de samengestelde knip dat geen kenmerk is ('"
                            + snippet(deel) + "'): " + NOG_NIET + "; de referentie-interpreter neemt alleen kenmerken"
                            + " (RUNTIME-SEMANTICS ch. 3.6)");
                uit = knip(deel, lez, wat, geknipt, uit);
            }
            return uit;
        }
        if (c instanceof Conditie.Vergelijking v) return vergelijkingsknip(v, lez, wat, geknipt, tijdlijn);
        throw new Weigering(wat + " met de knip '" + snippet(c) + "': " + NOG_NIET + "; de referentie-interpreter"
                + " neemt een eigen tijdsafhankelijk kenmerk, een vergelijking van een eigen tijdsafhankelijk attribuut"
                + " en een 'alle' daarvan (RUNTIME-SEMANTICS ch. 3.6)");
    }

    /** The comparison clip of {@link #knip} of the timeline of {@code geknipt}: the left side an own attribute on a
     *  timeline, which the clip reads whole, segment by segment, so that the asOfDate-bucket scalar faults there
     *  ({@link Invoervormfout}, RUNTIME-SEMANTICS ch. 8.4) before the bound is evaluated; the right side a value. */
    private Term vergelijkingsknip(Conditie.Vergelijking v, Lezingen lez, String wat, String geknipt, Term tijdlijn) {
        if (v.periode() != null)
            throw new Weigering(wat + " met een geheleperiodetoets in de knip ('" + snippet(v) + "', WF-097)");
        Integer toets = Tijdrekening.toets(v.op());
        if (toets == null)
            throw new Weigering(wat + " met een datumvergelijking in de knip ('" + snippet(v) + "'): " + NOG_NIET);
        // an ordering with a leeg side holds nowhere, in ch. 1.2 and in that helper alike; the equalities differ
        if (v.op() == Conditie.Operator.GELIJK || v.op() == Conditie.Operator.ONGELIJK)
            throw new Weigering(wat + " met '" + (v.op() == Conditie.Operator.GELIJK ? "gelijk aan" : "ongelijk aan")
                    + "' in de knip ('" + snippet(v) + "'): " + NOG_NIET + "; de rekenhulp rsTijdConditie leest een"
                    + " lege kant, een gat in de vergeleken tijdlijn of een lege grens, als geen periode, waar"
                    + " RUNTIME-SEMANTICS ch. 3.6 en de referentie-interpreter de vergelijking van ch. 1.2 per periode"
                    + " toepassen: twee lege kanten zijn gelijk, en precies één lege kant is ongelijk; geen golden met"
                    + " cases heeft het");
        if (!(v.links() instanceof Expr.AttribuutRef a) || a.attribuut() == null || a.objecttype() != lez.subject
                || navigatieBindingen.containsKey(a))
            throw new Weigering(wat + " met een vergelijking in de knip waarvan de linkerkant geen eigen"
                    + " tijdsafhankelijk attribuut is ('" + snippet(v) + "'): " + NOG_NIET);
        Lid getoetst = tijdlid(lez.subject, a.attribuut(), false, wat);
        if (!getoetst.datatype().isNumeriek() || !eff.typeVan(v.rechts()).isNumeriek())
            throw new Weigering(wat + " met een vergelijking in de knip die geen getallen vergelijkt ('" + snippet(v)
                    + "'): " + NOG_NIET);
        if (!Objects.equals(eff.eenheidVan(v.links()), eff.eenheidVan(v.rechts())))
            throw new Weigering(wat + " met een vergelijking in de knip over twee eenheden ('" + snippet(v) + "'): "
                    + NOG_NIET);
        int getoetsteTijdlijn = lez.tijdlijn(getoetst);
        int onderwerp = lez.lid();
        lez.markeerFalen();
        Term grens = waarde(v.rechts(), lez);
        String vergeleken = getoetst.naam();
        Term perioden = new Term.Aanroep(Functie.RS_TIJD_CONDITIE, List.of(
                volledig(getoetsteTijdlijn, "the gedurende clip of \"" + geknipt + "\" compares \"" + vergeleken
                        + "\" per segment and needs its full timeline; ", onderwerp,
                        " supplies only the asOfDate-bucket scalar (ch. 3.6, ch. 8.4)", bron(a.bron())),
                constante(toets), new Term.Aanroep(Functie.COLLAPSE, List.of(grens), grens.bron()),
                constante(Tijdvak.class)), null);
        return new Term.Aanroep(Functie.RS_TIJD_KNIP, List.of(tijdlijn, perioden, constante(Tijdvak.class)),
                bron(v.bron()));
    }

    /**
     * {@code lid}, a member on a timeline whose total over time is taken, where that total is the bare value times the
     * count of whole units of the member's grid, as the runtime's {@code rsTijdTotaal} computes it: a number without a
     * unit, with a unit without a denominator, or a rate whose denominator is the time unit of the grid itself
     * ({@code EUR/mnd} on a grid of months), where the integration of RUNTIME-SEMANTICS ch. 3.6 cancels it one for one.
     * Refused by name: a value that is no number (WF-091); a rate over a grid whose unit the document does not declare
     * as exactly one unit ({@link #rastereenheid}), which the reference interpreter refuses to integrate (ch. 3.6) and
     * {@code rsTijdTotaal} would weigh as if it did; and a rate whose denominator is another unit, which the reference
     * interpreter integrates through the declared factors and {@code rsTijdTotaal} would weigh unit-blind (the executor
     * note of ch. 3.6); no golden has it.
     */
    private Lid integreerbaar(Lid lid, String wat) {
        if (!lid.datatype().isNumeriek())
            throw new Weigering(wat + " over '" + lid.naam() + "', dat geen getal is (WF-091)");
        Eenheid e = lid.datatype().eenheidOf();
        if (e == null || e.noemer() == null) return lid;
        String raster = switch (lid.tijd()) {
            case DAG -> "DAYS";
            case WEEK -> "WEEKS";
            case MAAND -> "MONTHS";
            case KWARTAAL -> "KWARTAAL";
            case JAAR -> "YEARS";
        };
        // the reference interpreter resolves the grid's unit before it looks at the denominator, so a document without
        // one is refused for that whatever the denominator
        String zonderRastereenheid = rastereenheid(lid.tijd(), raster);
        if (zonderRastereenheid != null)
            throw new Weigering(wat + " over '" + lid.naam() + "': the temporal total of a " + e.weergave() + " value: "
                    + zonderRastereenheid);
        if (!raster.equals(Tijdrekening.chronoUnit(e.noemer())))
            throw new Weigering(wat + " over '" + lid.naam() + "' met het tarief " + e.weergave() + " op een raster van "
                    + lid.tijd() + ": de noemer is niet de eenheid van het raster, en de tak herrekent hem niet"
                    + " (RUNTIME-SEMANTICS ch. 3.6); geen golden met cases heeft het");
        return lid;
    }

    /**
     * Whether the document declares the unit of the grid {@code g}, which a rate over that grid integrates against, as
     * the reference interpreter resolves it once per fold (RUNTIME-SEMANTICS ch. 3.6, "Units, the explicit rate
     * operand"): among the units the unit systems declare without a denominator, those whose abbreviation the calendar
     * table spells as the grid's unit ({@code raster}, as {@link Tijdrekening#chronoUnit} names it), counted once per
     * canonical chain, the canonical unit and the exact factor. Null where there is exactly one chain; otherwise the
     * reference interpreter's reason, that the document declares none or several.
     */
    private String rastereenheid(RegelSpraakModel.Granulariteit g, String raster) {
        record Keten(String canoniek, RegelSpraakModel.Eenheidstelsel.Factor factor) {}
        Set<Keten> ketens = new LinkedHashSet<>();
        for (RegelSpraakModel.Eenheidstelsel s : model.eenheidstelsels.values())
            for (Map.Entry<String, Eenheid> u : s.eenheden.entrySet())
                if (u.getValue().noemer() == null && raster.equals(Tijdrekening.chronoUnit(u.getValue())))
                    ketens.add(new Keten(u.getValue().canoniek() == null ? null : Namen.norm(u.getValue().canoniek()),
                            s.exacteFactoren.get(u.getKey())));
        if (ketens.size() == 1) return null;
        // the grid's unit in the spelling the reference interpreter's sentence gives it
        String spelling = switch (g) {
            case DAG -> "dg";
            case WEEK -> "wk";
            case MAAND -> "mnd";
            case KWARTAAL -> "kw";
            case JAAR -> "jr";
        };
        return ketens.isEmpty()
                ? "the document declares no " + spelling + " unit, so this timeline's grid has no unit to integrate"
                        + " against (ch. 3.6)"
                : "the document declares " + ketens.size() + " different " + spelling + " units, so this timeline's grid"
                        + " has no single unit to integrate against (ch. 3.6)";
    }

    /**
     * The evaluation of the fold {@code v} in the rule that reads it, bound at {@code i} ({@link Lezingen#vouw}): where
     * its filter deposited for a member, the deposit is heard by this rule's probe ({@link Sonde}), since the reference
     * interpreter evaluates the filter with the reading form's collector, only where that form's evaluation reaches the
     * fold (RUNTIME-SEMANTICS ch. 5.10, 5.11); then, where its filter faulted for a member, the fault of the first in
     * fold order ({@link Filterfout}); then, where the case gives a member's timeline as the asOfDate-bucket scalar, the
     * fault that names the first such member in fold order ({@link Lidfout}, ch. 3.6, 8.4); then {@code uitkomst}, the
     * fold's value, whose fallback the reference interpreter evaluates after the members ({@link Term.Vouwlezing}). A
     * deposit for a member after the one that faults, which the reference interpreter does not reach, is heard as well:
     * the session faults at the fixpoint, so it changes no outcome.
     */
    private static Term gelezenVouw(Vouw v, int[] i, Lezingen lez, Term uitkomst, String bron) {
        if (v.kanFalen() || v.waardeKanFalen()) lez.markeerFalen();
        int sonde = v.deponeert() ? lez.sonde() : -1;
        if (!v.kanFalen() && !v.waardeKanFalen() && sonde < 0) return uitkomst;
        return new Term.Vouwlezing(uitkomst, sonde < 0 ? -1 : i[3], sonde, v.kanFalen() ? i[2] : -1,
                v.waardeKanFalen() ? i[4] : -1, bron);
    }

    private static boolean leeg(Object telling) {
        return ((Number) telling).longValue() == 0;
    }

    private static Class<?> accumulatiefunctie(String naam) {
        try {
            return Class.forName("org.drools.core.base.accumulators." + naam);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("de Drools-accumulatiefunctie " + naam + " ontbreekt op het classpath", e);
        }
    }

    /** The fold of {@code f}, an aggregation or a quantifier fold, on the subject of {@code lez}, the rule that reads
     *  it, built by {@code bouw} from its key and its name the first time it is read. While it is built, the binders in
     *  scope in {@code lez} are those its contribution rules see from outside ({@link #vouwbinders}). */
    private Vouw vouwVoor(Object f, Lezingen lez, BiFunction<String, String, Vouw> bouw) {
        Map<Objecttype, Vouw> perOnderwerp = vouwen.computeIfAbsent(f, k -> new IdentityHashMap<>());
        Vouw v = perOnderwerp.get(lez.subject);
        if (v == null) {
            String naam = "vouw " + vouwteller++;
            Set<String> buiten = vouwbinders;
            vouwbinders = lez.binderbereik();
            try {
                v = bouw.apply(vorm + ": " + naam, naam);
            } finally {
                vouwbinders = buiten;
            }
            perOnderwerp.put(lez.subject, v);
        }
        return v;
    }

    /** The count over an instance-set union (RUNTIME-SEMANTICS ch. 3.4d): a contribution rule per selection, each with
     *  its own relation and its own filter, all under one key; a member two selections select gives two equal
     *  contributions, which merge into one fact, so the count is that of the union. */
    private Vouw unievouw(Expr.Functie f, Expr.InstanceSetUnion u, Lezingen lez) {
        Objecttype subject = lez.subject;
        // one count answers a number of instances of one declared type: two populations leave whether two selections
        // share an instance to the subtype lattice, which the reference interpreter faults on (WF-110 (d))
        Objecttype populatie = null;
        for (Expr.InstanceSetUnion.Tak t : u.takken()) {
            if (t.elementType() == null) continue;
            if (populatie == null) populatie = t.elementType();
            else if (t.elementType() != populatie)
                throw new Weigering("een InstanceSetUnion over twee populaties ('" + populatie.naam + "' en '"
                        + t.elementType().naam + "', '" + snippet(u.bron()) + "'): één telling antwoordt een aantal over"
                        + " één gedeclareerd type, en of twee selecties dan een instantie delen, is een vraag over de"
                        + " subtyperelatie (WF-110 (d))");
        }
        return vouwVoor(f, lez, (sleutel, naam) -> {
            List<Rule> regels = new ArrayList<>();
            List<Lezingen> lezingen = new ArrayList<>();
            boolean kanFalen = false, deponeert = false;
            for (int k = 0; k < u.takken().size(); k++) {
                Expr.InstanceSetUnion.Tak t = u.takken().get(k);
                if (t.elementType() == null)
                    throw new Weigering("een selectie zonder elementtype in een InstanceSetUnion ('" + snippet(t.bron()) + "')");
                kanFalen |= bijdrageregels(sleutel, naam + ", selectie " + k, Expr.FunctieKind.AANTAL, subject,
                        t.elementType(), eenstapsbereik(t.bronRol(), subject, t.elementType(), snippet(t.bron())), k,
                        t.filter(), null, regels, lezingen);
                deponeert |= deponeert(t.filter());
            }
            return new Vouw(sleutel, List.copyOf(regels), List.copyOf(lezingen), kanFalen, deponeert);
        });
    }

    /**
     * The contribution rule of one range of the fold {@code sleutel}: the fold's subject bound first, the member's
     * pattern joined to it through {@code bereik}, the member's reads, the {@code filter} as the test (the member is the
     * filter's subject, RUNTIME-SEMANTICS ch. 3.4a), and a consequence that inserts the member's {@link Bijdrage}
     * logically: for a count the member; for a filled count the member where its value is gevuld; for the other folds
     * the member's value where it is gevuld, since a leeg member is skipped (ch. 1.2). Beside it, where the filter can
     * fault, the rule that inserts the member's {@link Filterfout} ({@link #filterfoutregel}), and where the filter can
     * deposit, the rule that inserts the member's {@link Filterdeponering} ({@link #filterdeponeerregel});
     * {@code selectie} is the number of the selection of an instance-set union, 0 for any other fold. The rules go into
     * {@code uit} and their reads into {@code lezingen}, to be registered with the form; returns whether the filter can
     * fault.
     */
    private boolean bijdrageregels(String sleutel, String naam, Expr.FunctieKind kind, Objecttype subject,
                                   Objecttype elem, Bereik bereik, int selectie, Conditie filter,
                                   Expr.AttribuutRef arg, List<Rule> uit, List<Lezingen> lezingen) {
        Lezingen lez = bijdragelezingen(subject, elem, bereik.koppel());
        Set<Integer> gelezen = lez.volg();
        Term toets = filter == null ? null : voorwaarde(filter, lez);
        boolean kanFalen = lez.kanFalen();
        lez.stop();
        Set<Integer> gebruik = lez.volg();
        int lid = lez.lid();
        // a member on a timeline contributes its own total over time (RUNTIME-SEMANTICS ch. 3.6, the per-element fold);
        // vouw admits only a sum and a total over one
        Lid lidwaarde = arg == null ? null : gedeclareerd(elem, arg.attribuut(), false).tijd() == null
                ? lid(elem, arg.attribuut(), false) : integreerbaar(tijdlid(elem, arg.attribuut(), false,
                        "een totaal per lid ('" + snippet(arg.bron()) + "')"), "een totaal per lid");
        int waarde = lidwaarde == null ? -1 : lidwaarde.tijd() == null ? lez.eigen(lidwaarde) : lez.tijdlijn(lidwaarde);
        String totaal = lidwaarde == null || lidwaarde.tijd() == null ? null : lidwaarde.tijd().name();
        lez.stop();
        List<RuleItemBuilder<?>> items = lez.linkerkant(voorwaarderol(kanFalen), toets, gelezen, filter);
        boolean telling = kind == Expr.FunctieKind.AANTAL, gevuld = kind == Expr.FunctieKind.AANTAL_GEVULDE;
        items.add(gevolg(lez, lez.drager, posities(gebruik), (drools, onderwerp, w) -> {
            if (telling) {
                drools.logicalInsert(new Bijdrage(sleutel, onderwerp, w[lid], null));
                return;
            }
            // a member the case gives as the asOfDate-bucket scalar contributes nothing: its Lidfout stands instead
            if (totaal != null && w[waarde] instanceof Emmerscalair) return;
            Object v = totaal == null ? w[waarde] : RegelSpraakRuntime.rsTijdTotaal((List<?>) w[waarde], totaal);
            if (v != null) drools.logicalInsert(new Bijdrage(sleutel, onderwerp, w[lid], gevuld ? null : collapse(v)));
        }, lez.variabelen.size()));
        String vormnaam = model.geëmitteerdeRegelNaam(vorm);
        uit.add(PatternDSL.rule(PKG, vormnaam + ": bijdrage aan " + naam).build(items.toArray(new RuleItemBuilder[0])));
        lezingen.add(lez);
        if (kanFalen) uit.add(filterfoutregel(sleutel, vormnaam + ": filterfout in " + naam, subject, elem, bereik,
                selectie, filter, lezingen::add));
        if (totaal != null) uit.add(lidfoutregel(sleutel, vormnaam + ": lidfout in " + naam, subject, elem, bereik,
                selectie, filter, lidwaarde, lezingen::add));
        if (deponeert(filter)) uit.add(filterdeponeerregel(sleutel, vormnaam + ": filterdeponering in " + naam, subject,
                elem, bereik, filter, lezingen::add));
        return kanFalen;
    }

    /**
     * The rule beside a contribution rule whose filter can deposit: that rule's left-hand side built again over fresh
     * declarations, the test that evaluating the filter for the member reaches a place that deposits (a division of a
     * filled dividend by a leeg divisor or the square root of a negative number, RUNTIME-SEMANTICS ch. 1.2), heard by a
     * probe in the test as {@link #deponeerregel} hears a condition's ({@link Sonde}), so a place the filter's own
     * composite does not reach deposits nothing (ch. 5.10), and a consequence that inserts the member's
     * {@link Filterdeponering} logically, on the fold's subject. No inconsistency: the reference interpreter evaluates
     * the filter only where the evaluation of the form that reads the fold reaches it, and deposits under that form
     * ({@link #gelezenVouw}). A filter that faults after a place that deposits has deposited, as there; the fault is the
     * reading form's ({@link #filterfoutregel}). The fact goes when a read it rested on changes, as a contribution does.
     */
    private Rule filterdeponeerregel(String sleutel, String regelnaam, Objecttype subject, Objecttype elem,
                                     Bereik bereik, Conditie filter, Consumer<Lezingen> vastleggen) {
        Lezingen lez = bijdragelezingen(subject, elem, bereik.koppel());
        Set<Integer> gelezen = lez.volg();
        Term toets = voorwaarde(filter, lez);
        lez.stop();
        int sonde = lez.sondeplaats();
        if (sonde < 0) throw new IllegalStateException("het filter van " + sleutel + " deponeert, maar kreeg geen sonde");
        Set<Integer> gebruik = lez.volg();
        int lid = lez.lid();
        lez.stop();
        List<RuleItemBuilder<?>> items = lez.linkerkant(Toetsitem.Rol.DEPONERING, toets, gelezen, filter);
        items.add(gevolg(lez, lez.drager, posities(gebruik), (drools, onderwerp, w) ->
                drools.logicalInsert(new Filterdeponering(sleutel, onderwerp, w[lid])), lez.variabelen.size()));
        vastleggen.accept(lez);
        return PatternDSL.rule(PKG, regelnaam).build(items.toArray(new RuleItemBuilder[0]));
    }

    /**
     * The rule beside a contribution rule whose filter can fault: that rule's left-hand side built again over fresh
     * declarations, joined per route from the fold's subject with the member's place bound at every to-many step of the
     * route ({@link Bereik#perRoute}), the test that the filter faults for the member, and a consequence that inserts
     * the member's {@link Filterfout} logically, on the fold's subject, with its place in the fold order: the
     * {@code selectie}, then those places. No {@link Fout}: whether the division happens depends on the form that reads
     * the fold, which faults where its evaluation reaches the fold while such a fact stands ({@link #vouw}). The fact
     * goes when a read it rested on changes, as a contribution does. A member whose place the input did not state has
     * no place in the fold order, and the first fault cannot be told without it: the consequence throws, naming the
     * field.
     */
    private Rule filterfoutregel(String sleutel, String regelnaam, Objecttype subject, Objecttype elem, Bereik bereik,
                                 int selectie, Conditie filter, Consumer<Lezingen> vastleggen) {
        List<Plaatslezing> plaatsen = new ArrayList<>();
        Lezingen lez = bijdragelezingen(subject, elem, l -> plaatsen.addAll(bereik.perRoute().apply(l)));
        Set<Integer> gelezen = lez.volg();
        Term toets = voorwaarde(filter, lez);
        lez.stop();
        Set<Integer> gebruik = lez.volg();
        int lid = lez.lid();
        lez.stop();
        gebruik.addAll(gelezen);
        for (Plaatslezing p : plaatsen) {
            gebruik.add(p.aantal());
            gebruik.add(p.plaats());
        }
        List<RuleItemBuilder<?>> items = lez.linkerkant(Toetsitem.Rol.FOUT, toets, gelezen, filter);
        Toets gecompileerd = toetsweergave(toets);
        items.add(gevolg(lez, lez.drager, posities(gebruik), (drools, onderwerp, w) -> {
            RuntimeException fout = fout(gecompileerd, w);
            if (fout == null)
                throw new IllegalStateException("het filter van " + sleutel + " faalde in de toets en niet in het gevolg,"
                        + " over dezelfde gebonden waarden");
            List<Integer> plaats = new ArrayList<>(plaatsen.size() + 1);
            plaats.add(selectie);
            for (Plaatslezing p : plaatsen) {
                if (((Number) w[p.aantal()]).longValue() == 0)
                    throw new IllegalStateException("het filter van " + sleutel + " faalt voor " + Feitnamen.object(w[lid])
                            + ", maar een lid van zijn route heeft geen Lijstplaats onder het veld '" + p.veld() + "': de"
                            + " invoer stelt de volgorde van die lijst niet, en zonder haar is de eerste fout van de vouw"
                            + " niet te bepalen (RUNTIME-SEMANTICS ch. 5.11)");
                plaats.add(((Number) w[p.plaats()]).intValue());
            }
            drools.logicalInsert(new Filterfout(sleutel, onderwerp, w[lid], fout, plaats));
        }, lez.variabelen.size()));
        vastleggen.accept(lez);
        return PatternDSL.rule(PKG, regelnaam).build(items.toArray(new RuleItemBuilder[0]));
    }

    /**
     * The rule beside a contribution rule of a per-element fold over an attribute on a timeline (RUNTIME-SEMANTICS
     * ch. 3.6, §7.1): that rule's left-hand side built again over fresh declarations, joined per route from the fold's
     * subject with the member's place bound at every to-many step of the route ({@link Bereik#perRoute}), the test that
     * the member's filter holds and that the case gives the member's timeline as the asOfDate-bucket scalar
     * ({@link Emmerscalair}), and a consequence that inserts the member's {@link Lidfout} logically, on the fold's
     * subject, with its place in the fold order: the {@code selectie}, then those places. Such a member contributes
     * nothing, since a set fold needs each member's own timeline and the scalar states none (ch. 8.4); the form that
     * reads the fold faults where its evaluation reaches the fold while such a fact stands ({@link #gelezenVouw}), in the
     * reference interpreter's words. A member whose filter faults has its {@link Filterfout} instead, which the reading
     * form meets first. A member whose place the input did not state has no place in the fold order: the consequence
     * throws, naming the field, as the filter fault rule does.
     */
    private Rule lidfoutregel(String sleutel, String regelnaam, Objecttype subject, Objecttype elem, Bereik bereik,
                              int selectie, Conditie filter, Lid lidwaarde, Consumer<Lezingen> vastleggen) {
        List<Plaatslezing> plaatsen = new ArrayList<>();
        Lezingen lez = bijdragelezingen(subject, elem, l -> plaatsen.addAll(bereik.perRoute().apply(l)));
        Set<Integer> gelezen = lez.volg();
        // the member's filter holds, without its fault where it can fault: the filter fault rule takes that fault
        List<Term> emmer = new ArrayList<>();
        if (filter != null) {
            Term filtertoets = voorwaarde(filter, lez);
            emmer.add(lez.kanFalen() ? new Term.ZonderFout(filtertoets, bron(filter.bron())) : filtertoets);
        }
        int waarde = lez.tijdlijn(lidwaarde);
        lez.stop();
        Set<Integer> gebruik = lez.volg();
        int lid = lez.lid();
        lez.stop();
        for (Plaatslezing p : plaatsen) {
            gebruik.add(p.aantal());
            gebruik.add(p.plaats());
        }
        String naam = lidwaarde.naam();
        emmer.add(emmerscalair(waarde, true));
        List<RuleItemBuilder<?>> items = lez.linkerkant(Toetsitem.Rol.VORM, alle(emmer), gelezen, filter);
        items.add(gevolg(lez, lez.drager, posities(gebruik), (drools, onderwerp, w) -> {
            List<Integer> plaats = new ArrayList<>(plaatsen.size() + 1);
            plaats.add(selectie);
            for (Plaatslezing p : plaatsen) {
                if (((Number) w[p.aantal()]).longValue() == 0)
                    throw new IllegalStateException("de vouw " + sleutel + " leest de tijdlijn van " + Feitnamen.object(w[lid])
                            + ", die de case als emmerscalair geeft, maar een lid van zijn route heeft geen Lijstplaats"
                            + " onder het veld '" + p.veld() + "': de invoer stelt de volgorde van die lijst niet, en zonder"
                            + " haar is niet te bepalen welk lid de vouw noemt (RUNTIME-SEMANTICS ch. 3.6, 5.11)");
                plaats.add(((Number) w[p.plaats()]).intValue());
            }
            drools.logicalInsert(new Lidfout(sleutel, onderwerp, w[lid], new Invoervormfout("the per-element temporal"
                    + " total of \"" + naam + "\" over ", w[lid], ": the case supplies an asOfDate-bucket scalar, which"
                    + " carries no period of its own; a set fold needs each member's own timeline (ch. 3.6/§7.1,"
                    + " ch. 8.4)"), plaats));
        }, lez.variabelen.size()));
        vastleggen.accept(lez);
        return PatternDSL.rule(PKG, regelnaam).build(items.toArray(new RuleItemBuilder[0]));
    }

    /** The reads of a contribution rule: the member is the subject, the fold's subject is bound before it, and
     *  {@code bereik} joins the two. */
    @SuppressWarnings("unchecked")
    private Lezingen bijdragelezingen(Objecttype subject, Objecttype elem, Consumer<Lezingen> bereik) {
        Lezingen lez = new Lezingen(elem);
        lez.buiten = vouwbinders;
        lez.drager = (Variable<Object>) DSL.declarationOf(bonenklasse(subject), "$vouwonderwerp");
        lez.kop.add(PatternDSL.pattern(lez.drager));
        bereik.accept(lez);
        return lez;
    }

    /**
     * How a rule of a fold joins the member, its subject, to the fold's subject, its {@link Lezingen#drager}, in two
     * shapes. {@code koppel}, for the contribution rule: the join on the member's pattern, and the intermediates a
     * multi-step range needs right after it, quantified, so a member matches once. {@code perRoute}, for the filter
     * fault rule: the same join with the intermediates bound, so a member matches once per route, and per to-many step
     * of the route, in order, the place of the step's member in its list ({@link #plaats}); it returns those places, and
     * refuses a range whose members have no place.
     */
    private record Bereik(Consumer<Lezingen> koppel, Function<Lezingen, List<Plaatslezing>> perRoute) {}

    /** One to-many step's place, as the filter fault rule binds it: the reference field the step's member links through,
     *  and the indices of the count of its {@link Lijstplaats} facts and of the least place among them. */
    private record Plaatslezing(String veld, int aantal, int plaats) {}

    /** The place of a to-many step's member in its list, for the filter fault rule: an accumulate over the
     *  {@link Lijstplaats} facts of field {@code veld} on the node {@code keten} walks to from {@code knoop} (the node
     *  itself without a chain), which counts them and takes the least place. The count gives a row where the input
     *  stated no place, so the rule still fires and its consequence names the gap instead of dropping the fault. */
    private Plaatslezing plaats(Lezingen lid, String veld, Variable<Object> knoop, List<String> keten) {
        lijstvelden.add(veld);
        Variable<Lijstplaats> p = DSL.declarationOf(Lijstplaats.class, "$lijstplaats_" + id("plaats"));
        Variable<Object> waarde = DSL.declarationOf(Object.class, "$lijstplaatswaarde_" + id("plaats"));
        Variable<Object> aantal = DSL.declarationOf(Object.class, "$lijstplaatsen_" + id("plaats"));
        Variable<Object> minste = DSL.declarationOf(Object.class, "$plaats_" + id("plaats"));
        PatternDSL.PatternDef<Lijstplaats> q = PatternDSL.pattern(p).expr(id("plaats-veld"), x -> x.veld().equals(veld))
                .expr(id("plaats-lid"), knoop, (x, k) -> x.lid() == wandel(k, keten));
        q.bind(waarde, Lijstplaats::plaats);
        lid.bereik.add(DSL.accumulate(q, DSL.accFunction(accumulatiefunctie("CountAccumulateFunction"), p).as(aantal),
                DSL.accFunction(accumulatiefunctie("MinAccumulateFunction"), waarde).as(minste)));
        int n = lid.variabelen.size();
        return new Plaatslezing(veld, lid.extern(Leessoort.LIJSTPLAATSEN, "lijstplaatsen " + n, aantal),
                lid.extern(Leessoort.LIJSTPLAATS, "lijstplaats " + n, minste));
    }

    /** The range of the fold {@code f} from {@code subject} to {@code elem}: its multi-step path, or its one step. */
    private Bereik bereik(Expr.Functie f, Objecttype subject, Objecttype elem) {
        List<RolBindingRef> pad = bronPaden.get(f);
        if (pad != null) return padbereik(pad, subject, elem, snippet(f.bron()));
        return eenstapsbereik(rolBindingen.get(f), subject, elem, snippet(f.bron()));
    }

    /**
     * The one-step range of a fold (IR-SPEC ch. 4.3, {@code sourceRole}), derived from its binding as the exec-model
     * branch derives it: the bound role is the member's and its sibling the subject's, and §13.3.9 stores the relation in
     * one scalar bean field on whichever end is the one side. A to-many member role keeps it on the member, named after
     * the subject's role; a to-one member role keeps it on the subject, named after the member's role. Refused as that
     * branch refuses them: a binding that names no role pair, a to-one member role whose sibling is to-many (one field
     * carries both directions there, and that branch keeps refusing the shape until a card of its own measures it), and
     * a join the bean model gives no field. A to-many member role stores its links on the members, each in the list of
     * the subject at a place the case input states under the field ({@link Lijstplaats}); a to-one member role reaches
     * one member at most, whose place compares with nothing.
     */
    private Bereik eenstapsbereik(RolBindingRef rb, Objecttype subject, Objecttype elem, String wat) {
        RegelSpraakModel.Rol[] paar = FactModelGenerator.gebondenRolpaar(model, rb);
        if (paar == null) throw new Weigering("het bereik van '" + wat + "' noemt geen rolpaar (IR-SPEC ch. 4.3)");
        if (rb.feittype() != null && gecreeerdeRelaties.contains(Namen.norm(rb.feittype())))
            return koppelingsbereik(rb, paar[0], wat);
        String elemRol = paar[0].naam(), terug = paar[1].naam();
        boolean voorwaarts = !model.naarVeelRol(subject, elem, elemRol);
        if (voorwaarts && model.naarVeelRol(elem, subject, terug))
            throw new Weigering("een Aggregation over de enkelvoudige elementrol '" + elemRol + "' wiens zusterrol '"
                    + terug + "' meervoudig is ('" + wat + "'): dat feittype heeft één beanveld voor beide richtingen, en"
                    + " deze tak weigert de vorm voorzichtig tot een eigen kaart haar meet (§13.3.9 opslagrichting)");
        Objecttype drager = voorwaarts ? subject : elem, doel = voorwaarts ? elem : subject;
        String rol = voorwaarts ? elemRol : terug;
        if (!FactModelGenerator.heeftReferentieVeld(model, drager, rol, doel))
            throw new Weigering("de aggregatie-join van '" + wat + "' heeft geen beanveld — "
                    + FactModelGenerator.geenJoinveldReden(model, drager, rol, doel) + " (§13.3.9 opslagrichting)");
        Function<Object, Object> veld = Accessors.getter(bonenklasse(drager), rol);
        Consumer<Lezingen> koppel = lid -> {
            if (voorwaarts) zelfde(lid.onderwerp, "bereik", lid.drager, e -> e, veld::apply);
            else zelfde(lid.onderwerp, "bereik", lid.drager, veld::apply);
        };
        String lijstveld = Namen.ident(rol, false);
        return new Bereik(koppel, lid -> {
            koppel.accept(lid);
            return voorwaarts ? List.of() : List.of(plaats(lid, lijstveld, lid.s, List.of()));
        });
    }

    /** The one-step range of a fold over a relation a form creates: the member and the fold's subject joined by an
     *  {@link Koppeling} of the relation in an {@code exists}, the member filling {@code lidrol}. The links of such a
     *  relation change while the session fires and are in no bean field, so a field join would miss them. A created
     *  link has no place in the fold order, which the reference interpreter gives by the order of creation, so a filter
     *  that can fault over such a range is refused. */
    private Bereik koppelingsbereik(RolBindingRef rb, RegelSpraakModel.Rol lidrol, String wat) {
        RegelSpraakModel.Feittype ft = feittype(rb.feittype());
        if (ft == null || ft.rollen.size() != 2 || ft.wederkerig)
            throw new Weigering("het bereik van '" + wat + "' over de gecreëerde relatie '" + rb.feittype()
                    + "', die geen relatie van twee rollen is: " + NOG_NIET);
        String relatie = Namen.norm(ft.naam);
        boolean lidIsEerste = Namen.norm(ft.rollen.get(0).naam()).equals(Namen.norm(lidrol.naam()));
        return new Bereik(lid -> {
            Variable<Koppeling> k = DSL.declarationOf(Koppeling.class, "$koppeling_" + id("bereik"));
            lid.bereik.add(DSL.exists(zelfde(zelfde(PatternDSL.pattern(k)
                    .expr(id("bereik-koppeling"), x -> x.relatie().equals(relatie)),
                    "bereik-lid", lid.s, x -> lidIsEerste ? x.eerste() : x.tweede()),
                    "bereik-drager", lid.drager, x -> lidIsEerste ? x.tweede() : x.eerste())));
        }, lid -> {
            throw new Weigering("een filter dat kan falen in een vouw over de gecreëerde relatie '" + rb.feittype()
                    + "' ('" + wat + "'): een gemaakte link heeft geen plaats in de vouwvolgorde, die bepaalt welke fout"
                    + " de vouw geeft (RUNTIME-SEMANTICS ch. 5.11); " + NOG_NIET);
        });
    }

    /**
     * The multi-step range of a fold (IR-SPEC ch. 4.3, {@code sourcePath}; RUNTIME-SEMANTICS ch. 3.4b), on the plan the
     * shared {@link Bronpad} derives: where the walk down from the subject
     * and the walk up from the member meet, one constraint on the member's pattern; otherwise the intermediates neither
     * walk determines go into one {@code exists} after the member's pattern, so a member that two intermediates reach
     * still matches once. Two matches would give two equal contributions, which merge into one fact as well. The filter
     * fault rule binds the intermediates instead, one match per route, and the place of the member of every backward
     * step, the to-many ones ({@link Bronpad}): an intermediate where the step ends on one, else the node the member's
     * own walk reaches.
     */
    private Bereik padbereik(List<RolBindingRef> pad, Objecttype subject, Objecttype elem, String wat) {
        for (RolBindingRef stap : pad)
            if (stap.feittype() != null && gecreeerdeRelaties.contains(Namen.norm(stap.feittype())))
                throw new Weigering("een vouw met een bereik van meer stappen over de gecreëerde relatie '"
                        + stap.feittype() + "' ('" + wat + "'): " + NOG_NIET);
        Bronpad.Plan plan = Bronpad.ontleed(model, subject, pad, elem);
        if (plan.weigering() != null) throw new Weigering(plan.weigering() + " ('" + wat + "')");
        if (plan.ontmoet()) {
            int m = plan.ontmoeting();
            List<String> vanafLid = plan.ketenVanafElement(m), vanafOnderwerp = plan.ketenVanafOnderwerp(m);
            Consumer<Lezingen> koppel = lid -> lid.onderwerp.expr(id("pad"), lid.drager, (e, s) -> {
                Object a = wandel(e, vanafLid), b = wandel(s, vanafOnderwerp);
                return a != null && a == b;
            });
            return new Bereik(koppel, lid -> {
                koppel.accept(lid);
                return plaatsenLangs(plan, Map.of(), lid);
            });
        }
        return new Bereik(lid -> {
            List<PatternDSL.PatternDef<Object>> patronen = tussenpatronen(plan, new LinkedHashMap<>(), lid);
            lid.bereik.add(DSL.exists(patronen.get(0),
                    patronen.subList(1, patronen.size()).toArray(new org.drools.model.view.ViewItemBuilder<?>[0])));
        }, lid -> {
            Map<Integer, Variable<Object>> tussen = new LinkedHashMap<>();
            lid.bereik.addAll(tussenpatronen(plan, tussen, lid));
            return plaatsenLangs(plan, tussen, lid);
        });
    }

    /** The patterns of the intermediates of {@code plan} that neither walk determines, each declared into
     *  {@code tussen} and joined to its neighbours. */
    private List<PatternDSL.PatternDef<Object>> tussenpatronen(Bronpad.Plan plan, Map<Integer, Variable<Object>> tussen,
                                                               Lezingen lid) {
        for (int i : plan.tussenknopen()) tussen.put(i, tussenknoop(plan.knoopTypen().get(i)));
        List<PatternDSL.PatternDef<Object>> patronen = new ArrayList<>();
        int laatste = plan.achterwaartsVanaf() - 1;
        for (Map.Entry<Integer, Variable<Object>> e : tussen.entrySet()) {
            int i = e.getKey();
            PatternDSL.PatternDef<Object> p = koppel(PatternDSL.pattern(e.getValue()), plan, i, i, tussen, lid);
            if (i == laatste) p = koppel(p, plan, plan.achterwaartsVanaf(), i, tussen, lid);
            patronen.add(p);
        }
        return patronen;
    }

    /** The places along a route of {@code plan}, one per backward step in step order: the step's own member, node
     *  {@code i}, holds the field toward node {@code i - 1}, whose list it is in. A backward step never lies in the
     *  subject's forward run, so its node is a bound intermediate or a node of the member's own walk. */
    private List<Plaatslezing> plaatsenLangs(Bronpad.Plan plan, Map<Integer, Variable<Object>> tussen, Lezingen lid) {
        List<Plaatslezing> uit = new ArrayList<>();
        for (int i = 1; i <= plan.stappen().size(); i++) {
            Bronpad.Stap stap = plan.stappen().get(i - 1);
            if (!stap.achterwaarts()) continue;
            Variable<Object> knoop = tussen.get(i);
            uit.add(knoop != null ? plaats(lid, stap.beanveld(), knoop, List.of())
                    : plaats(lid, stap.beanveld(), lid.s, plan.ketenVanafElement(i)));
        }
        return uit;
    }

    @SuppressWarnings("unchecked")
    private Variable<Object> tussenknoop(Objecttype t) {
        return (Variable<Object>) DSL.declarationOf(bonenklasse(t), "$tussen_" + id("pad"));
    }

    /** Constrains {@code p}, the pattern of node {@code dit} of {@code plan}, with hop {@code stap}, which connects nodes
     *  {@code stap - 1} and {@code stap}; its other end is read through whichever walk determines it: the subject's
     *  (a forward prefix), the member's (a backward suffix) or an intermediate declared before it. */
    private PatternDSL.PatternDef<Object> koppel(PatternDSL.PatternDef<Object> p, Bronpad.Plan plan, int stap, int dit,
                                                Map<Integer, Variable<Object>> tussen, Lezingen lid) {
        int ander = stap - 1 == dit ? stap : stap - 1;
        boolean ditIsDoel = ander == stap - 1;
        List<String> keten = ander <= plan.voorwaarts() ? plan.ketenVanafOnderwerp(ander)
                : ander >= plan.achterwaartsVanaf() ? plan.ketenVanafElement(ander) : List.of();
        Variable<Object> anderVar = ander <= plan.voorwaarts() ? lid.drager
                : ander >= plan.achterwaartsVanaf() ? lid.s : tussen.get(ander);
        String veld = plan.stappen().get(stap - 1).beanveld();
        boolean achterwaarts = plan.stappen().get(stap - 1).achterwaarts();
        return p.expr(id("pad"), anderVar, (t, o) -> {
            Object x = wandel(o, keten);
            if (x == null || t == null) return false;
            Object bron = ditIsDoel ? x : t, doel = ditIsDoel ? t : x;
            return achterwaarts ? RegelSpraakRuntime.get(doel, veld) == bron : RegelSpraakRuntime.get(bron, veld) == doel;
        });
    }

    /** A chain of to-one reference fields, leeg-safe: a broken link reads as null, and the member drops out. */
    private static Object wandel(Object start, List<String> velden) {
        Object cur = start;
        for (String v : velden) {
            if (cur == null) return null;
            cur = RegelSpraakRuntime.get(cur, v);
        }
        return cur;
    }

    /** A read of an attribute: of the subject where the reference carries no path (it must name the subject's own type,
     *  WF-053), else over its path of roles. */
    private int attribuutlezing(Expr.AttribuutRef a, Lezingen lez) {
        if (a.objecttype() == null || a.attribuut() == null)
            throw new Weigering("een onopgeloste AttributeReference ('" + snippet(a.bron()) + "')");
        List<RolBindingRef> pad = navigatieBindingen.get(a);
        if (pad == null) {
            if (a.objecttype() != lez.subject)
                throw new Weigering("een eigen lezing die " + a.objecttype().naam + " noemt op een " + lez.subject.naam
                        + " ('" + snippet(a.bron()) + "', WF-053)");
            return lez.eigen(lid(lez.subject, a.attribuut(), false));
        }
        Pad p = pad(lez.subject, pad, a.objecttype(), "de lezing '" + snippet(a.bron()) + "'");
        return lez.genavigeerd(p, lid(p.bereikt(), a.attribuut(), false));
    }

    // ---------------- conditions ----------------

    /** A condition as a predicate over the values its reads bind, in the order {@link Lezingen} gives them indices: the
     *  rendering of a planned condition that a consequence calls to evaluate it again ({@link #toetsweergave}), until
     *  step 1c of card drl-teksttak-op-truth-maintenance plans the consequences. */
    @FunctionalInterface
    private interface Toets {
        boolean test(Object[] waarden);
    }

    /** The condition {@code c} as a term whose value is a truth value ({@link Term}), with the reads it needs bound in
     *  {@code lez}. Every condition node is named here, so a node the IR records gain later fails to compile until it
     *  is decided whether this branch reads it. */
    private Term voorwaarde(Conditie c, Lezingen lez) {
        return switch (c) {
            case Conditie.Kenmerk k -> kenmerkcheck(k, lez);
            case Conditie.Samengesteld s -> samengesteld(s, lez);
            case Conditie.Vergelijking v -> vergelijking(v, lez);
            case Conditie.Predicaat p -> predicaat(p, lez);
            case Conditie.Periode p -> tijdvenster(p, lez);
            case Conditie.Kwantorvouw q -> kwantorvouw(q, lez);
            case Conditie.Rollidmaatschap rol -> rolcontrole(rol, lez);
            // the v0 far-side record, which the loader no longer makes: since L2 a role check is one node (IR-SPEC
            // ch. 4.5), so only a hand-built model reaches this
            case Conditie.VerreRollidmaatschap rol -> throw nietGelezen("VerreRollidmaatschap", c, "de verre rolcontrole"
                    + " van v0, die sinds L2 de ene knoop RoleMembership is (IR-SPEC ch. 4.5) en die de lader niet maakt");
            case Conditie.Regelversie rv -> kanaallezing(rv, lez);
            // RUNTIME-SEMANTICS ch. 5.7 and WF-044: the marker fails the evaluation of the form where the evaluation
            // reaches it, so a composite that settles before it reads it plays
            case Conditie.Onopgelost o -> {
                lez.markeerFalen();
                yield new Term.Faal(Rekenfout.ONOPGELOSTE_VOORWAARDE, bron(o.bron()));
            }
        };
    }

    /**
     * A time window on a scalar target, the evaluation-date gate (RUNTIME-SEMANTICS ch. 3.10): it holds where the
     * session's evaluation date lies in the window, {@code from} inclusive, {@code to} exclusive or inclusive where the
     * source wrote "tot en met" ({@code toInclusive}), an absent bound open. An unset evaluation date never matches, nor
     * does a leeg bound; neither is a fault. Both bounds are read, {@code from} before {@code to}, before the date decides
     * (ch. 5.11). A bound is a date literal or the subject's own scalar date attribute (WF-111), read per subject, also
     * where another rule derives it; any other bound is refused by name ({@link Vensterpositie#GRENSVORM}). On a
     * time-dependent target the window would clip the timeline instead, and such a target is refused before this
     * ({@link #lid}).
     */
    private Term tijdvenster(Conditie.Periode p, Lezingen lez) {
        if (p.vanaf() == null && p.tot() == null)
            throw new Weigering("een periodevenster zonder grens ('" + snippet(p) + "'): er is geen venster om op te"
                    + " poorten");
        for (Expr grens : java.util.Arrays.asList(p.vanaf(), p.tot()))
            if (grens != null && !Vensterpositie.grensLowerbaar(grens, lez.subject, navigatieBindingen::containsKey))
                throw new Weigering(Vensterpositie.GRENSVORM + ": " + snippet(p));
        Term vanaf = p.vanaf() == null ? null : waarde(p.vanaf(), lez);
        Term tot = p.tot() == null ? null : waarde(p.tot(), lez);
        int datum = lez.rekendatum();
        // an absent bound is open where a leeg one never matches, so whether the window has the bound is a constant
        // of its own beside the bound's place
        return new Term.Aanroep(Functie.TIJDVENSTER, List.of(vanaf == null ? constante(null) : vanaf,
                constante(vanaf != null), tot == null ? constante(null) : tot, constante(tot != null),
                constante(p.totInclusief()), new Term.Lees(datum, null)), bron(p.bron()));
    }

    private static Weigering nietGelezen(String vorm, Conditie c, String reden) {
        return new Weigering("een " + vorm + " in de voorwaarde ('" + snippet(c) + "'): " + reden);
    }

    /** A characteristic check: of the subject, a bound truth value, complemented where the check is negated; over a
     *  role, the reached object's, and a check over an absent link holds in neither polarity (RUNTIME-SEMANTICS ch. 5.8
     *  rule 3). The standings are total, so no negation is needed here. Under the whole-period qualifier the check
     *  folds the characteristic's true periods instead ({@link #geheelPeriodeKenmerk}). */
    private Term kenmerkcheck(Conditie.Kenmerk k, Lezingen lez) {
        // WF-109: the host is satisfied extensionally; a host the subject is no instance of names another object, which
        // a rule condition does not bind
        if (!Gastheer.hostOp(k, lez.subject))
            throw new Weigering("een CharacteristicCheck met een host die het onderwerp niet is ('" + snippet(k) + "')");
        if (k.periode() != null) return geheelPeriodeKenmerk(k, lez);
        boolean gewenst = !k.negatie();
        List<RolBindingRef> nav = navigatieBindingen.get(k);
        if (nav == null) {
            if (k.navigatie() != null)
                throw new Weigering("een CharacteristicCheck met een navigatie zonder binding ('" + snippet(k) + "')");
            int i = lez.eigen(lid(lez.subject, k.kenmerk(), true));
            return new Term.Aanroep(Functie.KENMERK, List.of(new Term.Lees(i, null), constante(gewenst)),
                    bron(k.bron()));
        }
        Pad p = pad(lez.subject, nav, null, "de kenmerkcheck '" + snippet(k) + "'");
        int i = lez.genavigeerd(p, lid(p.bereikt(), k.kenmerk(), true));
        return new Term.Aanroep(Functie.KENMERK_LANGS_PAD, List.of(new Term.Lees(i, null), constante(gewenst)),
                bron(k.bron()));
    }

    // ---------------- the whole-period qualifier ----------------

    /**
     * A characteristic check under the whole-period qualifier (RUNTIME-SEMANTICS ch. 3.9): whether the characteristic's
     * true periods cover every day of every calendar year or month they touch ({@link #geheelPeriode}). The periods are
     * those of the subject or of the object a path of to-one roles reaches, bound as a timeline; where a link on the
     * path is absent the timeline is null, which touches no bucket and passes, as a characteristic without periods does,
     * where the unqualified check over an absent link holds in neither polarity. No executor writes a characteristic on
     * a timeline (ch. 3.11), so its periods are input. Refused by name: the negation inside the qualifier
     * ("gedurende het gehele jaar niet …"), a per-day complement of the periods, which the DRL-text branch refuses as
     * well (ch. 3.12).
     */
    private Term geheelPeriodeKenmerk(Conditie.Kenmerk k, Lezingen lez) {
        String wat = "een CharacteristicCheck met wholePeriod ('" + snippet(k) + "')";
        if (k.negatie())
            throw new Weigering(wat + " met de ontkenning binnen de kwalificatie: " + NOG_NIET + ", zoals de andere twee"
                    + " takken (RUNTIME-SEMANTICS ch. 3.9, 3.12); geen golden met cases heeft haar");
        List<RolBindingRef> nav = navigatieBindingen.get(k);
        if (nav == null) {
            if (k.navigatie() != null) throw new Weigering(wat + " met een navigatie zonder binding");
            return geheelPeriode(k.periode(), new Term.Lees(lez.tijdlijn(tijdlid(lez.subject, k.kenmerk(), true, wat)),
                    null), TOETS_KENMERK, null, bron(k.bron()));
        }
        Pad p = pad(lez.subject, nav, null, wat);
        return geheelPeriode(k.periode(), new Term.Lees(lez.genavigeerd(p, tijdlid(p.bereikt(), k.kenmerk(), true, wat)),
                null), TOETS_KENMERK, null, bron(k.bron()));
    }

    /**
     * A comparison under the whole-period qualifier (RUNTIME-SEMANTICS ch. 3.9): whether the subject's own timeline
     * satisfies the comparison with a bound on every day of every calendar year or month it touches
     * ({@link #geheelPeriode}). One side is an own attribute on a timeline, the other the bound, a value of the rule like
     * any other, evaluated whatever the timeline holds, as the reference interpreter evaluates it, and restated into the
     * timeline's unit (ch. 2.6); a leeg bound fails every day, never a fault. Where the timeline stands on the right, the
     * operator is reversed, never negated: on a leeg day the two differ. Refused by name, as the reference interpreter
     * folds none of them: a timeline read over a path (ch. 3.12: it folds no navigated comparison), two timelines, no
     * timeline, a date ordering (ch. 3.12) and a side that is no number. A bound read over a path is a value like any
     * other. The timeline is read before the bound, as the reference interpreter reads them.
     */
    private Term geheelPeriodeVergelijking(Conditie.Vergelijking v, Lezingen lez) {
        String wat = "een Comparison met wholePeriod ('" + snippet(v) + "')";
        for (Expr kant : java.util.Arrays.asList(v.links(), v.rechts()))
            if (kant instanceof Expr.AttribuutRef a && navigatieBindingen.containsKey(a) && a.objecttype() != null
                    && a.attribuut() != null && gedeclareerd(a.objecttype(), a.attribuut(), false).tijd() != null)
                throw new Weigering(wat + " over een tijdlijn langs een pad ('" + snippet(a.bron()) + "'): de"
                        + " referentie-interpreter vouwt alleen een eigen tijdsafhankelijk attribuut van het onderwerp"
                        + " (RUNTIME-SEMANTICS ch. 3.9, 3.12)");
        boolean links = eigenTijdlijn(v.links(), lez), rechts = eigenTijdlijn(v.rechts(), lez);
        if (links == rechts)
            throw new Weigering(wat + (links ? " tussen twee tijdlijnen" : " zonder eigen tijdsafhankelijk attribuut aan"
                    + " één kant") + ": de vouw toetst één tijdlijn van het onderwerp tegen een grens (RUNTIME-SEMANTICS"
                    + " ch. 3.9)");
        Expr.AttribuutRef tijd = (Expr.AttribuutRef) (links ? v.links() : v.rechts());
        Expr grensExpr = links ? v.rechts() : v.links();
        Integer toets = Tijdrekening.toets(links ? v.op() : Operatoren.omkeren(v.op()));
        if (toets == null)
            throw new Weigering(wat + " met een datumvergelijking: een datum over een gehele periode heeft geen lezing"
                    + " (RUNTIME-SEMANTICS ch. 3.9, 3.12)");
        Lid lid = tijdlid(lez.subject, tijd.attribuut(), false, wat);
        if (!lid.datatype().isGetalachtig() || !eff.typeVan(grensExpr).isGetalachtig())
            throw new Weigering(wat + " die geen getallen vergelijkt: de vouw toetst de waarde van een segment als getal"
                    + " (RUNTIME-SEMANTICS ch. 3.9)");
        int tijdlijn = lez.tijdlijn(lid);
        Term grens = herschaald(waarde(grensExpr, lez), eff.eenheidVan(grensExpr), eff.eenheidVan(tijd),
                "een §8.2 tijdlijngrens");
        return geheelPeriode(v.periode(), volledigeTijdlijn(tijdlijn, lid, null, lez, bron(tijd.bron())), toets, grens,
                bron(v.bron()));
    }

    /**
     * The leeg or the gevuld test under the whole-period qualifier (RUNTIME-SEMANTICS ch. 3.9): whether the timeline of an
     * attribute reads leeg, or a value, on every day of every calendar year or month in which it reads a value
     * ({@link #geheelPeriode}), the attribute the subject's own or that of the object a path of to-one roles reaches,
     * whose absent link gives the null timeline, which passes. The inner negation is per day, so a negated test is its
     * sister test; the loader already writes it so. A test of another kind and an operand that is no attribute on a
     * timeline are refused by name (the loader refuses a parameter as the operand, in the sentence of the other two
     * executors).
     */
    private Term geheelPeriodePredicaat(Conditie.Predicaat p, Lezingen lez) {
        String wat = "een Predicate met wholePeriod ('" + snippet(p) + "')";
        boolean gevuld = switch (p.soort()) {
            case LEEG -> p.negatie();
            case GEVULD -> !p.negatie();
            default -> throw new Weigering(wat + " van de soort " + Spelling.PREDICATE_KIND.spelling(p.soort()) + ": alleen"
                    + " de leeg- en de gevuldtoets hebben een lezing onder de kwalificatie (RUNTIME-SEMANTICS ch. 3.9)");
        };
        if (!(p.operand() instanceof Expr.AttribuutRef a) || a.objecttype() == null || a.attribuut() == null)
            throw new Weigering(wat + " over iets anders dan een attribuut: de vouw leest de tijdlijn van een"
                    + " tijdsafhankelijk attribuut (RUNTIME-SEMANTICS ch. 3.9)");
        int toets = gevuld ? TOETS_GEVULD : TOETS_LEEG;
        List<RolBindingRef> pad = navigatieBindingen.get(a);
        if (pad == null) {
            if (a.objecttype() != lez.subject)
                throw new Weigering(wat + " over een eigen lezing die " + a.objecttype().naam + " noemt op een "
                        + lez.subject.naam + " (WF-053)");
            Lid lid = tijdlid(lez.subject, a.attribuut(), false, wat);
            return geheelPeriode(p.periode(), volledigeTijdlijn(lez.tijdlijn(lid), lid, null, lez, bron(a.bron())),
                    toets, null, bron(p.bron()));
        }
        Pad q = pad(lez.subject, pad, a.objecttype(), wat);
        Lid lid = tijdlid(q.bereikt(), a.attribuut(), false, wat);
        return geheelPeriode(p.periode(), volledigeTijdlijn(lez.genavigeerd(q, lid), lid, q, lez, bron(a.bron())),
                toets, null, bron(p.bron()));
    }

    /**
     * The timeline of the attribute {@code lid} bound at slot {@code tijdlijn}, which the whole-period fold reads whole,
     * or the fault where the case gives its input as the asOfDate-bucket scalar ({@link Emmerscalair},
     * {@link Functie#VOLLEDIG}): ch. 8.4 makes a form that needs the one convention and finds the other fault, never
     * guess. The fault names the object by the subject of the rule, through the roles of {@code pad} where the attribute
     * is read over one ("the werf of "S1""). Registers that the rule's evaluation can fault. The reference interpreter
     * and the ASP backend read such an input as no timeline, which passes (measured 2026-10-09).
     */
    private static Term volledigeTijdlijn(int tijdlijn, Lid lid, Pad pad, Lezingen lez, String bron) {
        int onderwerp = lez.lid();
        lez.markeerFalen();
        StringBuilder langs = new StringBuilder();
        if (pad != null) for (int i = pad.rollen().size() - 1; i >= 0; i--)
            langs.append("the ").append(pad.rollen().get(i)).append(" of ");
        String voor = "the whole-period qualifier over \"" + lid.naam() + "\" needs the full timeline; " + langs;
        return volledig(tijdlijn, voor, onderwerp, " supplies only the asOfDate-bucket scalar (ch. 3.9, ch. 8.4)", bron);
    }

    /** Whether {@code e} reads an attribute on a timeline of this rule's subject itself: an attribute reference without
     *  a path, naming the subject's type, whose declaration on that type's chain lies on a timeline. */
    private boolean eigenTijdlijn(Expr e, Lezingen lez) {
        return e instanceof Expr.AttribuutRef a && a.objecttype() == lez.subject && a.attribuut() != null
                && !navigatieBindingen.containsKey(a) && gedeclareerd(lez.subject, a.attribuut(), false).tijd() != null;
    }

    /** The tests of {@code rsGeheelPeriode}, by its own codes: a comparison takes the code {@link Tijdrekening#toets}
     *  gives its operator. */
    private static final int TOETS_GEVULD = 6, TOETS_KENMERK = 7, TOETS_LEEG = 8;

    /**
     * The fold of the whole-period qualifier (RUNTIME-SEMANTICS ch. 3.9) over the timeline bound at {@code tijdlijn}, as
     * the runtime folds it ({@code rsGeheelPeriode}): for every calendar year or month the
     * timeline touches, the days on which the test holds must be all the days of that bucket. The grid is the
     * qualifier's own, "het gehele jaar" or "de gehele maand", never the member's grid nor the session's, so the fold
     * reads no evaluation date and a session without one answers. A leeg segment reads as a gap: it touches no bucket
     * and satisfies no test but the leeg test. A timeline without segments, and a null one, touch no bucket and pass,
     * so the qualifier's own negation ("niet gedurende …"), which complements the folded verdict, fails on them. The
     * bound of a comparison is read as a decimal ({@code toBd}), after the timeline.
     * The timeline of an attribute is read whole, so its input given as the asOfDate-bucket scalar faults
     * ({@link #volledigeTijdlijn}); a characteristic, whose input is its true periods, reads its timeline as it is.
     * {@code grens} is null where the test has no bound.
     */
    private static Term geheelPeriode(Conditie.GeheelPeriode periode, Term tijdlijn, int toets, Term grens,
                                      String bron) {
        return new Term.Aanroep(Functie.GEHEEL_PERIODE, List.of(tijdlijn, grens == null ? constante(null) : grens,
                constante(periode.jaar() ? "JAAR" : "MAAND"), constante(toets), constante(periode.niet())), bron);
    }

    /**
     * A role check (RUNTIME-SEMANTICS ch. 5.8a, IR-SPEC ch. 4.5): whether a link of the fact type holds the subject in
     * the role the check names and, where it names a counterpart type, an instance of that type at the other end, a
     * class test that a subtype passes; negated, whether no such link exists. An existence question, never a count and
     * never a pick, bound as a value the test reads, so the check stands in a composite as any condition does. A
     * relation a form creates is read over its {@link Koppeling} facts, which its creations state while the session
     * fires and which the input's links insert logically; any other relation is input and is read where the bean model
     * stores it ({@link FactModelGenerator#lidmaatschapsopslag}): a to-one other role is the subject's own reference
     * field, read off the subject's bean, and a to-many other role is the field of the other end, which names the
     * subject, counted over the beans of the counterpart's class. Both roles to-many leaves no field, and the check is
     * refused in the sentence the DRL-text branch gives.
     */
    private Term rolcontrole(Conditie.Rollidmaatschap r, Lezingen lez) {
        RolBindingRef rb = rolBindingen.get(r);
        RegelSpraakModel.Rol[] paar = FactModelGenerator.gebondenRolpaar(model, rb);
        if (paar == null)
            throw nietGelezen("RoleMembership", r, "de binding (" + (rb == null ? "geen" : rb.feittype() + ", " + rb.rol())
                    + ") noemt geen gedeclareerde rol");
        Objecttype tegenpartij = dev.uchar.regelspraak.loader.IrLoader.tegenpartij(r);
        Class<?> klasse = tegenpartij == null ? null : bonenklasse(tegenpartij);
        boolean ontkend = r.negatie();
        if (rb.feittype() != null && gecreeerdeRelaties.contains(Namen.norm(rb.feittype()))) {
            RegelSpraakModel.Feittype ft = feittype(rb.feittype());
            if (ft == null || ft.rollen.size() != 2 || ft.wederkerig)
                throw nietGelezen("RoleMembership", r, "over de gecreëerde relatie '" + rb.feittype() + "', die geen"
                        + " relatie van twee rollen is: " + NOG_NIET);
            boolean onderwerpIsEerste = Namen.norm(ft.rollen.get(0).naam()).equals(Namen.norm(paar[0].naam()));
            int i = lez.rolkoppelingen(Namen.norm(ft.naam), onderwerpIsEerste, klasse);
            return new Term.Aanroep(Functie.ROLAANTAL, List.of(new Term.Lees(i, null), constante(ontkend)),
                    bron(r.bron()));
        }
        FactModelGenerator.Lidmaatschapsopslag o = FactModelGenerator.lidmaatschapsopslag(model, rb);
        if (o.weigering() != null) throw nietGelezen("RoleMembership", r, o.weigering());
        if (o.opSubject()) {
            if (!FactModelGenerator.heeftReferentieVeld(model, lez.subject, o.beanveld(), o.ander()))
                throw nietGelezen("RoleMembership", r, FactModelGenerator.geenReferentieveldReden(model, lez.subject,
                        o.beanveld(), o.ander()) + " (§13.3.9 opslagrichting)");
            int i = lez.rolveld(o.beanveld(), klasse);
            return new Term.Aanroep(Functie.ROLVELD, List.of(new Term.Lees(i, null), constante(ontkend)),
                    bron(r.bron()));
        }
        Objecttype vuller = tegenpartij != null ? tegenpartij : o.ander();
        if (!FactModelGenerator.heeftReferentieVeld(model, vuller, o.beanveld(), lez.subject))
            throw nietGelezen("RoleMembership", r, FactModelGenerator.geenReferentieveldReden(model, vuller,
                    o.beanveld(), lez.subject) + " (§13.3.9 opslagrichting)");
        int i = lez.rolvullers(bonenklasse(vuller), o.beanveld());
        return new Term.Aanroep(Functie.ROLAANTAL, List.of(new Term.Lees(i, null), constante(ontkend)),
                bron(r.bron()));
    }

    /** A composite folds its members in wire order and stops at the first member after which the verdict stands
     *  (RUNTIME-SEMANTICS ch. 5.10), so a member that would fault, a comparison with a division by zero, is not read
     *  where an earlier member settled the verdict, and the composite faults only where it reads such a member. */
    private Term samengesteld(Conditie.Samengesteld s, Lezingen lez) {
        // a window under GEEN or a counting quantifier has no reading (WF-112), refused in the sentence both other
        // branches give
        String venster = Vensterpositie.weigering(s);
        if (venster != null) throw new Weigering(venster + ": " + snippet(s));
        List<Term> leden = new ArrayList<>();
        for (Conditie deel : s.onderdelen()) leden.add(voorwaarde(deel, lez));
        return new Term.Samengesteld(s.kwantor(), s.drempel(), leden, bron(s.bron()));
    }

    /**
     * A quantifier fold (RUNTIME-SEMANTICS ch. 3.9a, IR-SPEC ch. 4.5): its {@code test}, the matrix, asked of every
     * contributor, a member of the range that passes the {@code filter}, each with the member as the condition's
     * subject, and counted under the quantifier: {@code all} holds where every contributor holds the matrix, {@code none}
     * where none does, the three counting quantifiers on the number that do, and over no contributor all and none hold.
     * The contributors are an aggregation's ({@link #kwantorbijdragen}): a contribution rule per member that passes the
     * filter, with the member's reads of the filter and the matrix bound in its left-hand side, so a member whose derived
     * value changes takes its contribution along, and an accumulate over the contributions in the left-hand side of the
     * rule that reads the fold. Where the matrix can neither fault nor deposit, a contribution carries 1 or 0 for the
     * matrix's verdict and the accumulate counts the contributors and sums the verdicts: the order of the members is then
     * invisible, and the verdict a call over the count and the sum ({@link Functie#KWANTOROORDEEL}). Where it can, the
     * order shows (ch. 5.10, 5.11): the reference interpreter reads the matrix in the contributors' order and stops at
     * the first contributor after which the verdict stands, so a contributor it does not reach raises no fault and
     * deposits nothing. A contribution then carries the matrix's outcome and the member's place ({@link Lidtoets}), the
     * accumulate collects them, and the evaluation replays that order ({@link Term.Kwantorlezing}). The filter is settled
     * for every member before the matrix, as for any fold ({@link #gelezenVouw}). The range is one step,
     * {@code sourceRole}: the loader refuses a multi-step one (WF-108 (c)), and so does this method where a hand-built
     * model carries one.
     */
    private Term kwantorvouw(Conditie.Kwantorvouw kv, Lezingen lez) {
        // WF-112, in the sentence of the DRL-text branch: a window in the filter or the test under none or a counting
        // quantifier has no reading
        String venster = Vensterpositie.weigering(kv);
        if (venster != null) throw new Weigering(venster + ": " + snippet(kv));
        Objecttype elem = kv.elementObjecttype();
        if (elem == null) throw nietGelezen("QuantifierFold", kv, "zonder opgelost elementtype");
        if (kv.toets() == null)
            throw nietGelezen("QuantifierFold", kv, "zonder matrix: een kwantor over geen voorwaarde heeft geen oordeel"
                    + " (WF-108 (d))");
        if (kv.kwantor() == Conditie.Kwantor.DE)
            throw nietGelezen("QuantifierFold", kv, "onder de kwantor DE, die één lid noemt en geen verzameling"
                    + " kwantificeert (WF-108 (a))");
        if (kv.bronRol() == null || !kv.bronPad().isEmpty())
            throw nietGelezen("QuantifierFold", kv, "zonder bereik van één stap: de lader weigert een bereik van meer"
                    + " stappen (WF-108 (c))");
        Conditie.Kwantor kw = kv.kwantor();
        int drempel = kv.drempel();
        Vouw v = vouwVoor(kv, lez, (sleutel, naam) -> kwantorbijdragen(kv, sleutel, naam, lez.subject, elem));
        String bron = bron(kv.bron());
        if (!v.geordend()) {
            int[] i = lez.vouw(v, Accumulatie.Soort.SOM);
            return gelezenVouw(v, i, lez, new Term.Aanroep(Functie.KWANTOROORDEEL, List.of(new Term.Lees(i[0], null),
                    new Term.Lees(i[1], null), constante(kw), constante(drempel)), bron), bron);
        }
        int[] i = lez.vouw(v, Accumulatie.Soort.LIJST);
        if (v.lidKanFalen()) lez.markeerFalen();
        int sonde = v.lidDeponeert() ? lez.sonde() : -1;
        return gelezenVouw(v, i, lez, new Term.Kwantorlezing(Functie.KWANTORLEZING, new Term.Lees(i[0], null),
                new Term.Lees(i[1], null), kw, drempel, sonde, bron), bron);
    }

    /**
     * The contribution rules of the quantifier fold {@code kv} on {@code subject}: one over its range, the fold's
     * subject bound first and the member joined to it, the member's reads of the filter and of the matrix bound, the
     * filter as the test (with the role {@link Toetsitem.Rol#ZONDER_FOUT} where it can fault), and a consequence that
     * evaluates the matrix for the member and inserts the member's {@link Bijdrage} logically: 1 or 0 for its verdict,
     * or, where the matrix can fault or deposit, its {@link Lidtoets}, with the member's place bound as the filter fault
     * rule binds it ({@link Bereik#perRoute}). Beside it the filter fault rule and the filter deposit rule of a filter
     * that can fault or deposit, as for any fold ({@link #bijdrageregels}). A made link has no place in the order of the
     * members, so a matrix that can fault or deposit over a relation a form creates is refused.
     */
    private Vouw kwantorbijdragen(Conditie.Kwantorvouw kv, String sleutel, String naam, Objecttype subject,
                                  Objecttype elem) {
        String wat = snippet(kv);
        Bereik bereik = eenstapsbereik(kv.bronRol(), subject, elem, wat);
        // the member's reads first and its join after: whether the matrix can fault or deposit decides whether the join
        // binds the member's place
        Lezingen lez = bijdragelezingen(subject, elem, l -> { });
        Set<Integer> gelezen = lez.volg();
        Term filter = kv.filter() == null ? null : voorwaarde(kv.filter(), lez);
        boolean filterKanFalen = lez.kanFalen();
        lez.stop();
        Set<Integer> gebruik = lez.volg();
        int lid = lez.lid();
        Toets matrix = toetsweergave(voorwaarde(kv.toets(), lez));
        boolean lidKanFalen = lez.kanFalen();
        lez.stop();
        boolean lidDeponeert = deponeert(kv.toets());
        int sonde = lez.sondeplaats();
        boolean geordend = lidKanFalen || lidDeponeert;
        List<Plaatslezing> plaatsen = new ArrayList<>();
        if (geordend) {
            if (kv.bronRol().feittype() != null && gecreeerdeRelaties.contains(Namen.norm(kv.bronRol().feittype())))
                throw new Weigering("een lidvoorwaarde die kan falen of deponeren in een QuantifierFold over de gecreëerde"
                        + " relatie '" + kv.bronRol().feittype() + "' ('" + wat + "'): een gemaakte link heeft geen plaats"
                        + " in de volgorde van de leden, die bepaalt welk lid de kortsluiting nog bereikt"
                        + " (RUNTIME-SEMANTICS ch. 3.9a, 5.10); " + NOG_NIET);
            plaatsen.addAll(bereik.perRoute().apply(lez));
            for (Plaatslezing p : plaatsen) {
                gebruik.add(p.aantal());
                gebruik.add(p.plaats());
            }
        } else bereik.koppel().accept(lez);
        List<RuleItemBuilder<?>> items = lez.linkerkant(voorwaarderol(filterKanFalen), filter, gelezen,
                kv.filter());
        items.add(gevolg(lez, lez.drager, posities(gebruik), (drools, onderwerp, w) -> {
            if (!geordend) {
                drools.logicalInsert(new Bijdrage(sleutel, onderwerp, w[lid],
                        matrix.test(w) ? BigDecimal.ONE : BigDecimal.ZERO));
                return;
            }
            List<Integer> plaats = new ArrayList<>(plaatsen.size() + 1);
            plaats.add(0);
            for (Plaatslezing p : plaatsen) {
                if (((Number) w[p.aantal()]).longValue() == 0)
                    throw new IllegalStateException("de kwantorvouw " + sleutel + " leest de matrix van "
                            + Feitnamen.object(w[lid]) + ", maar dat lid heeft geen Lijstplaats onder het veld '"
                            + p.veld() + "': de invoer stelt de volgorde van die lijst niet, en zonder haar is niet te"
                            + " bepalen welk lid de kortsluiting bereikt (RUNTIME-SEMANTICS ch. 5.10, 5.11)");
                plaats.add(((Number) w[p.plaats()]).intValue());
            }
            Sonde s = sonde < 0 ? null : new Sonde();
            if (s != null) w[sonde] = s;
            Boolean waar = null;
            RuntimeException fout = null;
            try {
                waar = matrix.test(w);
            } catch (RuntimeException e) {
                if (!Rekenfout.isEvaluatiefout(e)) throw e;
                fout = e;
            }
            drools.logicalInsert(new Bijdrage(sleutel, onderwerp, w[lid],
                    new Lidtoets(plaats, waar, fout, s != null && s.gemeld())));
        }, lez.variabelen.size()));
        String vormnaam = model.geëmitteerdeRegelNaam(vorm);
        List<Rule> regels = new ArrayList<>();
        List<Lezingen> lezingen = new ArrayList<>();
        regels.add(PatternDSL.rule(PKG, vormnaam + ": bijdrage aan " + naam).build(items.toArray(new RuleItemBuilder[0])));
        lezingen.add(lez);
        if (filterKanFalen) regels.add(filterfoutregel(sleutel, vormnaam + ": filterfout in " + naam, subject, elem,
                bereik, 0, kv.filter(), lezingen::add));
        boolean filterDeponeert = deponeert(kv.filter());
        if (filterDeponeert) regels.add(filterdeponeerregel(sleutel, vormnaam + ": filterdeponering in " + naam,
                subject, elem, bereik, kv.filter(), lezingen::add));
        return new Vouw(sleutel, List.copyOf(regels), List.copyOf(lezingen), filterKanFalen, filterDeponeert,
                lidKanFalen, lidDeponeert);
    }

    /**
     * A comparison of two values the rule binds or computes, with the empty operand of RUNTIME-SEMANTICS ch. 1.2:
     * numbers through {@code rsAggVergelijk} (an ordering with a leeg operand is onwaar, two leeg numbers are equal,
     * exactly one leeg is unequal; exact where a value is a rational), a percentage being a number in the unit %, the
     * right operand restated into the left one's unit (RUNTIME-SEMANTICS ch. 2.6); dates of one precision through
     * {@link Rekenhulpen#datumVergelijk} (two leeg dates are not equal); a text, an enumeration label or a truth value
     * through {@link Rekenhulpen#tekstGelijk}, an enumeration up to its normalised label. The left operand is evaluated before the right
     * one (ch. 5.11). Two sides whose types allow no comparison are refused. Under the whole-period qualifier the
     * comparison folds a timeline instead ({@link #geheelPeriodeVergelijking}).
     */
    private Term vergelijking(Conditie.Vergelijking v, Lezingen lez) {
        if (v.periode() != null) return geheelPeriodeVergelijking(v, lez);
        Datatype lt = eff.typeVan(v.links()), rt = eff.typeVan(v.rechts());
        Conditie.Operator op = v.op();
        Term linkerterm = waarde(v.links(), lez), rechterterm = waarde(v.rechts(), lez);
        String bron = bron(v.bron());
        if (lt.isGetalachtig() && rt.isGetalachtig() && !isDatumOp(op)) {
            Term rechts = herschaald(rechterterm, eff.eenheidVan(v.rechts()), eff.eenheidVan(v.links()),
                    "een §5.1 vergelijking");
            return new Term.Aanroep(Functie.RS_AGG_VERGELIJK, List.of(linkerterm, rechts,
                    constante(Operatoren.symbool(op))), bron);
        }
        if (lt.isDatum() && rt.isDatum() && (isDatumOp(op) || op == Conditie.Operator.GELIJK
                || op == Conditie.Operator.ONGELIJK)) {
            if (!zelfdeDatumsoort(lt, rt))
                throw new Weigering("een Comparison van een datum met een datum-tijd ('" + snippet(v) + "', §5.3 Tabel 36)");
            return new Term.Aanroep(Functie.DATUM_VERGELIJK, List.of(linkerterm, rechterterm, constante(op)), bron);
        }
        if ((op == Conditie.Operator.GELIJK || op == Conditie.Operator.ONGELIJK) && gelijkheidstype(lt)
                && gelijkheidstype(rt) && lt.isBoolean() == rt.isBoolean()) {
            boolean enumeratie = lt instanceof Datatype.Enumeratie || rt instanceof Datatype.Enumeratie;
            Term links = enumeratie ? enumnaam(linkerterm) : linkerterm;
            Term rechts = enumeratie ? enumnaam(rechterterm) : rechterterm;
            return new Term.Aanroep(Functie.TEKST_GELIJK, List.of(links, rechts,
                    constante(op == Conditie.Operator.GELIJK)), bron);
        }
        throw new Weigering("een Comparison " + Spelling.COMPARISON_OP.spelling(op) + " tussen " + typenaam(lt) + " en "
                + typenaam(rt) + " ('" + snippet(v) + "'): de typen van de twee kanten laten deze vergelijking niet toe"
                + " (RUNTIME-SEMANTICS ch. 1.2)");
    }

    /** {@code t}, an enumeration label or leeg, up to its normalisation ({@link Functie#RS_ENUM_NAAM}); the call has no
     *  node of its own in the IR and carries the fragment of the operand it normalises, as a restatement does. */
    private static Term enumnaam(Term t) {
        return new Term.Aanroep(Functie.RS_ENUM_NAAM, List.of(t), t.bron());
    }

    private static String typenaam(Datatype t) {
        return t == null ? "onbekend" : t.getClass().getSimpleName();
    }

    private static boolean gelijkheidstype(Datatype t) {
        return t != null && (t.isTekst() || t instanceof Datatype.Enumeratie || t.isBoolean());
    }

    private static boolean isDatumOp(Conditie.Operator op) {
        return switch (op) {
            case EERDER, EERDER_OF_GELIJK, LATER, LATER_OF_GELIJK -> true;
            default -> false;
        };
    }

    private static boolean zelfdeDatumsoort(Datatype a, Datatype b) {
        if (a instanceof Datatype.DatumTijd) return b instanceof Datatype.DatumTijd;
        return a instanceof Datatype.DatumInDagen && b instanceof Datatype.DatumInDagen;
    }

    /** A leeg or gevuld test of a value; a read over an absent link is leeg (RUNTIME-SEMANTICS ch. 5.8 rule 1). The loader
     *  writes a negated test as its sister test. Under the whole-period qualifier the test folds a timeline instead
     *  ({@link #geheelPeriodePredicaat}). */
    private Term predicaat(Conditie.Predicaat p, Lezingen lez) {
        if (p.periode() != null) return geheelPeriodePredicaat(p, lez);
        switch (p.soort()) {
            case LEEG, GEVULD -> {
                Term operand = waarde(p.operand(), lez);
                return new Term.Aanroep(Functie.LEEGTOETS, List.of(operand,
                        constante(p.soort() == Conditie.Predicaatsoort.LEEG)), bron(p.bron()));
            }
            case ELFPROEF, NUMERIEK_MET_EXACT -> {
                return cijferproef(p, lez);
            }
            // a day kind is consulted as a classifier of a bare date before the rule graph, never as a read inside the
            // fixpoint (ch. 5.4); inside a day-kind definition the test is the definition's own (dagsoortpredikaat)
            case DAGSOORT -> throw new Weigering("een Predicate dayKind als voorwaarde ('" + snippet(p) + "'): de"
                    + " referentie-interpreter en de ASP-backend weigeren haar met naam buiten een dagsoortdefinitie"
                    + " (RUNTIME-SEMANTICS ch. 3.12, 5.4)");
            default -> throw new IllegalStateException("predicaatsoort " + p.soort());
        }
    }

    /** The eleven test and the exact-digit test (§8.1.3, §8.1.4), through the runtime's
     *  {@code rsElfproef} and {@code rsNumeriekMetExact}, the written negation complementing the verdict: a leeg value
     *  passes neither test. The operand is read as any value; over a path of roles that branch makes an absent link fail
     *  both polarities, which a leeg read here cannot tell from a leeg value, so such an operand is refused by name. The
     *  reference interpreter and the ASP backend do not evaluate these predicates. */
    private Term cijferproef(Conditie.Predicaat p, Lezingen lez) {
        if (p.operand() instanceof Expr.AttribuutRef a && navigatieBindingen.containsKey(a))
            throw new Weigering("een Predicate " + Spelling.PREDICATE_KIND.spelling(p.soort()) + " over een pad ('"
                    + snippet(p) + "'): de DRL-teksttak laat een afwezige link beide polariteiten onwaar maken, wat"
                    + " een lege lezing hier niet van een lege waarde onderscheidt");
        Term operand = waarde(p.operand(), lez);
        boolean gewenst = !p.negatie();
        if (p.soort() == Conditie.Predicaatsoort.ELFPROEF)
            return new Term.Aanroep(Functie.ELFPROEF, List.of(operand, constante(gewenst)), bron(p.bron()));
        return new Term.Aanroep(Functie.NUMERIEK_MET_EXACT, List.of(operand, constante(p.cijfers()), constante(gewenst)),
                bron(p.bron()));
    }

    // ---------------- the reads of one rule ----------------

    /**
     * The reads of one rule, each bound once in its left-hand side and numbered in the order they are met: a member of
     * the subject that no form writes off the subject's bean pattern, over a path through the reference fields read in
     * the same binding; a derived member of the subject off its standing; a derived member over a path off its
     * {@link PadStand}; a parameter through a declaration of its own. The numbers are the indices of the value arrays
     * the condition's test and the consequence receive. The standings and path standings a rule reads are registered
     * with the transpiler only when the rule lowers ({@link #vastleggen}).
     *
     * <p>It decides what the rule reads and plans it: a {@link dev.uchar.regelspraak.transpiler.tms.plan.Lezing} per
     * slot (the plan's record, which the record {@link Lezing} of a read of the fixpoint shadows here), and the items of
     * the left-hand side in the closed vocabulary of {@code transpiler/tms/plan} ({@link Linkerkant}). It builds no
     * pattern itself: it hands every item to the rendering of this rule ({@link PatternDslWeergave}), which makes the
     * executable model of it.
     */
    private final class Lezingen {
        final Objecttype subject;
        /** The rendering of this rule's plan into the executable model. */
        private final PatternDslWeergave weergave;
        final Variable<Object> s;
        /** The rendered subject pattern, to which the forms still add constraints of their own until step 1c. */
        final PatternDSL.PatternDef<Object> onderwerp;
        /** The variable of each slot, as the rendering binds it, null for a binder or a probe: a read-only view. */
        final List<Variable<?>> variabelen;
        /** The slots of the value array, as the plan reads them. */
        private final List<dev.uchar.regelspraak.transpiler.tms.plan.Lezing> slots = new ArrayList<>();
        /** The values bound off the subject pattern. */
        private final List<Binding> onderwerpbindingen = new ArrayList<>();
        /** The constraints of the subject pattern on the subject bean, which the form sets ({@link #onderwerpAlfa}). */
        private final List<Alfa> onderwerpalfa = new ArrayList<>();
        /** The parameter items of the left-hand side: parameters, the evaluation date, the validity gate, the phase. */
        private final List<LhsItem> parameters = new ArrayList<>();
        /** The standing items of the left-hand side: standings, folds and the other accumulates, channels. */
        private final List<LhsItem> standen = new ArrayList<>();
        private final Map<String, Integer> index = new LinkedHashMap<>();
        private final Map<ElementSleutel, Lid> afgeleid = new LinkedHashMap<>();
        private final Map<String, Padlezing> paden = new LinkedHashMap<>();
        private final Set<String> kanaalnamen = new LinkedHashSet<>();
        /** The indices the expression being compiled reads, or null outside such a compilation. */
        private Set<Integer> gebruik;
        /** Whether an expression compiled since the last {@link #volg} can raise an evaluation fault. */
        private boolean falen;
        /** In a contribution rule of a fold ({@link #bijdrageregels}), the fold's subject, bound before the subject
         *  pattern, which is the member's; null in every other rule. */
        Variable<Object> drager;
        /** The patterns before the subject pattern: the fold subject's in a contribution rule. */
        final List<RuleItemBuilder<?>> kop = new ArrayList<>();
        /** In a rule that comes whole from its plan, the planned items before the subject pattern: the pattern of the
         *  source of an allocation's recipient rule ({@link #kopPlan}). */
        private final List<LhsItem> kopplan = new ArrayList<>();
        /** In a rule that comes whole from its plan, the identity joins of the subject pattern to an object a planned
         *  item before it binds ({@link #onderwerpJoin}). */
        private final List<Join> onderwerpjoins = new ArrayList<>();
        /** In a rule that comes whole from its plan, the planned items right after the subject pattern: the range of an
         *  allocation over a relation a form creates ({@link #bereikPlan}). */
        private final List<LhsItem> bereikplan = new ArrayList<>();
        /** The patterns right after the subject pattern: the existential intermediates of a multi-step range. */
        final List<RuleItemBuilder<?>> bereik = new ArrayList<>();
        /** The folds this rule reads, registered with it ({@link #vastleggen}). */
        private final List<Vouw> vouwen = new ArrayList<>();
        /** The binders of the {@code Let}s around the expression being compiled, by normalised name, each to its slot
         *  in the value array ({@link #binder}); a nested {@code Let} of the same name shadows the outer one while its
         *  body compiles (WF-064). */
        final Map<String, Integer> binders = new HashMap<>();
        /** In a contribution rule, the binders in scope at its fold, by normalised name: those of the {@code Let}s
         *  around the fold in the rule that reads it, which bind them there, and for a fold in a fold's filter those in
         *  scope at the outer fold as well. This rule binds none of their values; empty in every other rule. */
        Set<String> buiten = Set.of();
        /** The slot of the probe of this rule's deposits ({@link #sonde}), -1 while no place that deposits compiled. */
        private int sonde = -1;

        Lezingen(Objecttype subject) {
            this(subject, true);
        }

        /** The reads of a rule on {@code subject}, rendered item by item as the analysis plans them where
         *  {@code weergeven}: a form that still builds its rule itself takes the rendered items ({@link #linkerkant}),
         *  while a rule that comes whole from its plan ({@link #regelplan}) renders nothing here, and has no rendered
         *  subject, pattern or variable. */
        Lezingen(Objecttype subject, boolean weergeven) {
            this.subject = subject;
            this.weergave = weergeven ? new PatternDslWeergave(bonenklasse(subject), ONDERWERP) : null;
            this.s = weergeven ? weergave.onderwerp() : null;
            this.onderwerp = weergeven ? weergave.onderwerpPatroon() : null;
            this.variabelen = weergeven ? weergave.slots() : null;
        }

        /** The binders in scope where the expression being compiled stands: this rule's own and {@link #buiten}. */
        Set<String> binderbereik() {
            if (binders.isEmpty()) return buiten;
            Set<String> uit = new LinkedHashSet<>(buiten);
            uit.addAll(binders.keySet());
            return uit;
        }

        /** The subject itself as a bound value, for a consequence that hands on another object beside it. */
        int lid() {
            int bekend = bestaand("onderwerp zelf");
            return bekend >= 0 ? bekend : nieuw(Leessoort.ONDERWERP, "onderwerp zelf", ONDERWERP);
        }

        /** The left-hand side of the rule, once its condition and its value are compiled: the subject pattern with its
         *  bindings, the parameter declarations, the standing patterns, and the test of {@code voorwaarde} over the
         *  values at {@code gelezen}, which asks {@code rol} of it ({@link Toetsitem.Rol}). */
        List<RuleItemBuilder<?>> linkerkant(Toetsitem.Rol rol, Term toets, Set<Integer> gelezen, Conditie voorwaarde) {
            return linkerkant(rol, toets, gelezen, voorwaarde, List.of());
        }

        /** As {@link #linkerkant(Toetsitem.Rol, Term, Set, Conditie)}, with {@code stuur} between the standing patterns
         *  and the test: the control-fact patterns of a default, so that a default a higher one displaced is not tested
         *  at all. The plan of the left-hand side goes to the transpiler's plans ({@link #linkerkanten}); the rendering
         *  gives the items, among them those a form built directly ({@code kop}, {@code bereik}, {@code stuur}). A test
         *  of deposits takes the probe of this rule ({@link #sondeplaats}). */
        List<RuleItemBuilder<?>> linkerkant(Toetsitem.Rol rol, Term toets, Set<Integer> gelezen, Conditie voorwaarde,
                                            List<RuleItemBuilder<?>> stuur) {
            Toetsitem test = toetsitem(rol, toets, gelezen, voorwaarde);
            legVast(test);
            List<RuleItemBuilder<?>> items = new ArrayList<>(kop);
            items.add(onderwerp);
            items.addAll(bereik);
            for (LhsItem p : parameters) items.add(weergave.item(p));
            for (LhsItem p : standen) items.add(weergave.item(p));
            items.addAll(stuur);
            if (test != null) items.add(weergave.item(test));
            return items;
        }

        /** As {@link #linkerkant(Toetsitem.Rol, Term, Set, Conditie)}, for a rule that comes whole from its plan: the
         *  planned items after the subject pattern, the parameter items, the standing items and the test, which
         *  {@link #regelplan} takes with what the form adds after them. */
        List<LhsItem> plan(Toetsitem.Rol rol, Term toets, Set<Integer> gelezen, Conditie voorwaarde) {
            Toetsitem test = toetsitem(rol, toets, gelezen, voorwaarde);
            legVast(test);
            List<LhsItem> uit = new ArrayList<>(parameters);
            uit.addAll(standen);
            if (test != null) uit.add(test);
            return uit;
        }

        /** The test of {@code voorwaarde}, compiled into {@code toets}, over the values at {@code gelezen}, asking
         *  {@code rol} of it; null where the rule has no condition. */
        private Toetsitem toetsitem(Toetsitem.Rol rol, Term toets, Set<Integer> gelezen, Conditie voorwaarde) {
            if (toets == null) return null;
            int[] pos = posities(gelezen);
            if (pos.length > MAX_LEZINGEN)
                throw new Weigering("een voorwaarde die " + pos.length + " waarden leest ('" + snippet(voorwaarde)
                        + "'): de toets van deze tak neemt hoogstens " + MAX_LEZINGEN + " gebonden waarden");
            return new Toetsitem(id("voorwaarde"), rol, toets, rol == Toetsitem.Rol.DEPONERING ? sonde : -1,
                    List.copyOf(gelezen), slots.size());
        }

        /** Records the plan of the left-hand side up to and with {@code test} with the transpiler's plans
         *  ({@link #linkerkanten}). */
        private void legVast(Toetsitem test) {
            List<LhsItem> plan = new ArrayList<>();
            plan.add(new Onderwerp(bonenklasse(subject), ONDERWERP, onderwerpalfa, onderwerpjoins, onderwerpbindingen));
            plan.addAll(parameters);
            plan.addAll(standen);
            if (test != null) plan.add(test);
            linkerkanten.add(new Linkerkant(vorm, new Leestabel(slots), plan));
        }

        /** The plan of this rule, {@code naam}, of the form being lowered: the planned items before the subject
         *  pattern, the subject pattern with its constraints, joins and bindings as they stand now, the planned items
         *  right after it, {@code items} after those, and {@code gevolg}. A rule that comes whole from its plan has no
         *  pattern built outside the plan before or after the subject pattern, which a contribution rule still has. */
        Regelplan regelplan(String naam, List<LhsItem> items, Gevolg gevolg) {
            if (!kop.isEmpty() || !bereik.isEmpty())
                throw new IllegalStateException("een regel uit haar plan met patronen buiten het plan: " + naam);
            List<LhsItem> lhs = new ArrayList<>(kopplan);
            lhs.add(new Onderwerp(bonenklasse(subject), ONDERWERP, onderwerpalfa, onderwerpjoins, onderwerpbindingen));
            lhs.addAll(bereikplan);
            lhs.addAll(items);
            return new Regelplan(naam, vorm, null, new Leestabel(slots), lhs, gevolg);
        }

        /** Plans {@code item} before the subject pattern, in a rule that comes whole from its plan. */
        void kopPlan(LhsItem item) {
            alleenGepland("een patroon voor het onderwerp");
            kopplan.add(item);
        }

        /** Plans the identity join {@code j} on the subject pattern, after its constraints, in a rule that comes whole
         *  from its plan. */
        void onderwerpJoin(Join j) {
            alleenGepland("een join op het onderwerp");
            onderwerpjoins.add(j);
        }

        /** Plans {@code item} right after the subject pattern, in a rule that comes whole from its plan. */
        void bereikPlan(LhsItem item) {
            alleenGepland("een patroon na het onderwerp");
            bereikplan.add(item);
        }

        /** Refuses {@code wat} in a rule whose items are rendered as they come, which renders no such item. */
        private void alleenGepland(String wat) {
            if (weergave != null) throw new IllegalStateException(wat + " in een regel die niet uit haar plan komt");
        }

        /** The number of slots of the value array so far. */
        int grootte() {
            return slots.size();
        }

        /** Adds the constraint {@code a} on the subject bean to the subject pattern, after the ones before it. */
        void onderwerpAlfa(Alfa a) {
            onderwerpalfa.add(a);
            if (weergave != null) weergave.alfaOnderwerp(bonenklasse(subject), a);
        }

        /** The number of the other writers of an initialisation's target that wrote leeg on the subject, bound off an
         *  accumulate the form plans ({@link TmsTranspiler#initialisatieRegel}). */
        int legeSchrijvers() {
            return nieuw(Leessoort.LEGE_SCHRIJVERS, "lege schrijvers", "$lege_schrijvers");
        }

        Set<Integer> volg() {
            gebruik = new TreeSet<>();
            falen = false;
            return gebruik;
        }

        void stop() { gebruik = null; }

        /** Records that the expression being compiled can raise an evaluation fault. */
        void markeerFalen() { falen = true; }

        /** Whether an expression compiled since the last {@link #volg} can raise an evaluation fault: a condition that
         *  can is tested with the role {@link Toetsitem.Rol#ZONDER_FOUT}, beside the rule that inserts its fault. */
        boolean kanFalen() { return falen; }

        /** Registers what the lowered rule reads with the transpiler: the standings, path standings and channel
         *  standings it needs, and the contribution rules of its folds with what those read. */
        void vastleggen() {
            gelezen.putAll(afgeleid);
            for (Padlezing p : paden.values()) {
                padlezingen.putIfAbsent(p.sleutel(), p);
                if (schrijvers.containsKey(p.lid().sleutel()) || tijdlijnattribuut(p.lid()))
                    gelezen.putIfAbsent(p.lid().sleutel(), p.lid());
            }
            kanalen.addAll(kanaalnamen);
            for (Vouw v : vouwen) {
                if (bijdrageregels.putIfAbsent(v.sleutel(), v.regels()) == null)
                    for (Rule r : v.regels()) vormVanRegel.put(r.getName(), vorm);
                for (Lezingen l : v.lezingen()) l.vastleggen();
            }
        }

        /**
         * A fold of this rule's subject, bound off the contributions of the fold {@code sleutel} ({@link Bijdrage}): one
         * accumulate over them that counts them and, where {@code functie} names a fold, folds their values with it;
         * where the fold's filter can fault, a second accumulate over its {@link Filterfout} facts on this subject, which
         * binds the least of them, the first in fold order, or null; where the filter can deposit, an accumulate that
         * counts its {@link Filterdeponering} facts on this subject; and where a member's value can fault, an
         * accumulate over its {@link Lidfout} facts on this subject, which binds the least of them in the same way.
         * Returns the index of the count, that of the folded value, that of the least filter fault, that of the count of
         * filter deposits and that of the least member fault, each -1 without one. An accumulate over no fact still binds
         * a row, through the count, so the rule reads an empty fold, and a fold without a filter fault, a filter deposit
         * or a member fault, as well.
         */
        int[] vouw(Vouw v, Accumulatie.Soort functie) {
            vouwen.add(v);
            String sleutel = v.sleutel();
            int bekend = bestaand("vouw " + sleutel);
            if (bekend >= 0) return new int[]{bekend, functie == null ? -1 : bestaand("vouwwaarde " + sleutel),
                    v.kanFalen() ? bestaand("filterfout " + sleutel) : -1,
                    v.deponeert() ? bestaand("filterdeponering " + sleutel) : -1,
                    v.waardeKanFalen() ? bestaand("lidfout " + sleutel) : -1};
            String b = "$bijdrage_" + id("vouw");
            Alfa vanVouw = gelijk(id("vouw"), "vouw", sleutel);
            Join opOnderwerp = opOnderwerp(id("vouw-onderwerp"));
            String aantal = "$aantal_" + id("vouw");
            int[] uit;
            if (functie == null) {
                standen(new Accumulatie(new Patroon(Bijdrage.class, b, List.of(vanVouw), List.of(opOnderwerp), List.of()),
                        List.of(new Accumulatie.Functie(Accumulatie.Soort.AANTAL, null, aantal))));
                uit = new int[]{nieuw(Leessoort.VOUW_AANTAL, "vouw " + sleutel, aantal), -1, -1, -1, -1};
            } else {
                String bijdragewaarde = "$bijdragewaarde_" + id("vouw");
                String uitkomst = "$vouw_" + id("vouw");
                standen(new Accumulatie(new Patroon(Bijdrage.class, b, List.of(vanVouw), List.of(opOnderwerp),
                        List.of(Binding.van(bijdragewaarde, Bijdrage.class, "waarde"))),
                        List.of(new Accumulatie.Functie(Accumulatie.Soort.AANTAL, null, aantal),
                                new Accumulatie.Functie(functie, bijdragewaarde, uitkomst))));
                uit = new int[]{nieuw(Leessoort.VOUW_AANTAL, "vouw " + sleutel, aantal),
                        nieuw(Leessoort.VOUW_WAARDE, "vouwwaarde " + sleutel, uitkomst), -1, -1, -1};
            }
            if (v.kanFalen()) {
                String f = "$filterfout_" + id("vouw");
                Alfa vanFilter = gelijk(id("filterfout"), "vouw", sleutel);
                Join filterOpOnderwerp = opOnderwerp(id("filterfout-onderwerp"));
                String fouten = "$filterfouten_" + id("vouw");
                String eerste = "$eerste_filterfout_" + id("vouw");
                standen(new Accumulatie(new Patroon(Filterfout.class, f, List.of(vanFilter), List.of(filterOpOnderwerp),
                        List.of()), List.of(new Accumulatie.Functie(Accumulatie.Soort.AANTAL, null, fouten),
                        new Accumulatie.Functie(Accumulatie.Soort.MINIMUM, null, eerste))));
                uit[2] = nieuw(Leessoort.FILTERFOUT, "filterfout " + sleutel, eerste);
            }
            if (v.deponeert()) {
                String d = "$filterdeponering_" + id("vouw");
                Alfa vanDeponering = gelijk(id("filterdeponering"), "vouw", sleutel);
                Join deponeringOpOnderwerp = opOnderwerp(id("filterdeponering-onderwerp"));
                String deponeringen = "$filterdeponeringen_" + id("vouw");
                standen(new Accumulatie(new Patroon(Filterdeponering.class, d, List.of(vanDeponering),
                        List.of(deponeringOpOnderwerp), List.of()),
                        List.of(new Accumulatie.Functie(Accumulatie.Soort.AANTAL, null, deponeringen))));
                uit[3] = nieuw(Leessoort.FILTERDEPONERING, "filterdeponering " + sleutel, deponeringen);
            }
            if (v.waardeKanFalen()) {
                String f = "$lidfout_" + id("vouw");
                Alfa vanLid = gelijk(id("lidfout"), "vouw", sleutel);
                Join lidOpOnderwerp = opOnderwerp(id("lidfout-onderwerp"));
                String fouten = "$lidfouten_" + id("vouw");
                String eerste = "$eerste_lidfout_" + id("vouw");
                standen(new Accumulatie(new Patroon(Lidfout.class, f, List.of(vanLid), List.of(lidOpOnderwerp),
                        List.of()), List.of(new Accumulatie.Functie(Accumulatie.Soort.AANTAL, null, fouten),
                        new Accumulatie.Functie(Accumulatie.Soort.MINIMUM, null, eerste))));
                uit[4] = nieuw(Leessoort.LIDFOUT, "lidfout " + sleutel, eerste);
            }
            return uit;
        }

        /** The session's evaluation date (RUNTIME-SEMANTICS ch. 8.1), bound off its {@link Rekendatum}: one fact for the
         *  whole session, joined to no subject, bound among the parameters; null where the case carries no date. */
        int rekendatum() {
            int bekend = bestaand("rekendatum");
            if (bekend >= 0) return bekend;
            rekendatumGlobal();
            String r = "$rekendatum_" + slots.size();
            String d = "$rekendatumwaarde_" + slots.size();
            parameters(new Patroon(Rekendatum.class, r, List.of(), List.of(),
                    List.of(Binding.van(d, Rekendatum.class, "datum"))));
            return nieuw(Leessoort.REKENDATUM, "rekendatum", d);
        }

        /** Gates this rule on the evaluation date (RUNTIME-SEMANTICS ch. 8.2): it matches only where the validity
         *  window {@code g} admits the date, both bounds inclusive and an absent one open (IR-SPEC ch. 3.10), and never
         *  in a session without a date, where the form faults instead ({@link #geldigheidsfout}). A window that is always
         *  valid adds nothing. The date is constant while the session fires, so the gate never changes a match. */
        Lezingen geldig(Geldigheid g) {
            if (g == null || g.altijd()) return this;
            rekendatumGlobal();
            String r = "$geldigheid_" + slots.size();
            parameters(new Patroon(Rekendatum.class, r, List.of(new Alfa(id("geldig"),
                    List.of(new Alfa.Veldtoets("datum", Alfa.Vergelijker.BINNEN, g)))), List.of(), List.of()));
            return this;
        }

        /** A slot of the value array for the binder of a {@code Let} ({@link TmsTranspiler#let}): no pattern binds it, so
         *  it is no position a condition reads or a consequence binds, and the array is sized past it. */
        int binder() {
            return ongebonden(Leessoort.BINDER);
        }

        /** The slot of the probe of this rule's deposits ({@link Sonde}), made at the first place that deposits; like a
         *  binder's slot no pattern binds it. */
        int sonde() {
            if (sonde < 0) sonde = ongebonden(Leessoort.SONDE);
            return sonde;
        }

        /** The slot of the probe, or -1 where no place that deposits compiled. */
        int sondeplaats() {
            return sonde;
        }

        /** The number of the {@link Creatiefase} the driver opened, for a creation rule of stratum {@code stratum}: one
         *  fact for the whole session, joined to no subject, bound among the parameters, and only a phase of this
         *  stratum or one above, as a pass of the reference interpreter over stratum k runs every form of stratum k or
         *  below (RUNTIME-SEMANTICS ch. 6.3). */
        int creatiefase(int stratum) {
            int bekend = bestaand("creatiefase");
            if (bekend >= 0) return bekend;
            parameters(new Patroon(Creatiefase.class, "$creatiefase", List.of(new Alfa(id("creatiestratum"),
                    List.of(new Alfa.Veldtoets("stratum", Alfa.Vergelijker.MINSTENS, stratum)))), List.of(),
                    List.of(Binding.van("$fase", Creatiefase.class, "nummer"))));
            return nieuw(Leessoort.CREATIEFASE, "creatiefase", "$fase");
        }

        /** The marks of the creation that made this rule's subject, the {@link Gemaakt} facts whose object is the
         *  subject, bound as a list off an accumulate: empty for an object of the input, one mark for an object a
         *  creation made. An accumulate over no fact still binds a row, so the rule reads an object of the input as
         *  well. An object creation reads the depth of its subject off it ({@link TmsTranspiler#diepte}). */
        /** The {@link Ontvanger} facts of the allocation {@code verdeling} whose source is this rule's subject, bound as
         *  a list off an accumulate: the recipients with their weights, keys and caps as their own rule bound them
         *  ({@link TmsTranspiler#verdeling}). A recipient whose fact changes changes the list, and the activation that
         *  read it fires again. An accumulate over no fact still binds a row, an empty list. */
        int ontvangers(String verdeling) {
            String sleutel = "ontvangers " + verdeling;
            int bekend = bestaand(sleutel);
            if (bekend >= 0) return bekend;
            String o = "$ontvanger_" + id("verdeling");
            String lijst = "$ontvangers_" + id("verdeling");
            Alfa vanVerdeling = gelijk(id("ontvanger"), "verdeling", verdeling);
            standen(new Accumulatie(new Patroon(Ontvanger.class, o, List.of(vanVerdeling),
                    List.of(new Join(id("ontvanger-bron"), "bron", ONDERWERP)), List.of()),
                    List.of(new Accumulatie.Functie(Accumulatie.Soort.LIJST, null, lijst))));
            return nieuw(Leessoort.ONTVANGERS, sleutel, lijst);
        }

        int herkomst() {
            int bekend = bestaand("herkomst");
            if (bekend >= 0) return bekend;
            String g = "$herkomst_" + id("creatie");
            String lijst = "$herkomsten_" + id("creatie");
            standen(new Accumulatie(new Patroon(Gemaakt.class, g, List.of(),
                    List.of(new Join(id("herkomst"), "object", ONDERWERP)), List.of()),
                    List.of(new Accumulatie.Functie(Accumulatie.Soort.LIJST, null, lijst))));
            return nieuw(Leessoort.HERKOMST, "herkomst", lijst);
        }

        /** The standing of the §9.5 channel of the form named {@code producent}, bound off its {@link KanaalStand}:
         *  one fact for the whole session, joined to no subject, since the channel test has none of its own. */
        int kanaal(String producent) {
            String sleutel = "kanaal " + producent;
            int bekend = bestaand(sleutel);
            if (bekend >= 0) return bekend;
            String w = "$lezing_" + slots.size();
            String st = "$kanaal_" + slots.size();
            standen(new Patroon(KanaalStand.class, st, List.of(gelijk(id("kanaal"), "regel", producent)), List.of(),
                    List.of(Binding.van(w, KanaalStand.class, "inconsistent"))));
            kanaalnamen.add(producent);
            return nieuw(Leessoort.KANAAL, sleutel, w);
        }

        /** Whether the reference field {@code veld} of this rule's subject is set, to an instance of {@code klasse} where
         *  a role check narrows the other end (null: any): a link of an input relation stored on the subject
         *  ({@link TmsTranspiler#rolcontrole}). Input does not change while the session fires, so it is bound off the
         *  subject's bean, as an input member is ({@link #eigen}). */
        int rolveld(String veld, Class<?> klasse) {
            String sleutel = "rolveld " + veld + " " + (klasse == null ? "" : klasse.getName());
            int bekend = bestaand(sleutel);
            if (bekend >= 0) return bekend;
            String w = "$lezing_" + slots.size();
            bindOnderwerp(new Binding(w, List.of(), new Binding.Veld(bonenklasse(subject), veld), Binding.Leesvorm.GEZET,
                    klasse));
            return nieuw(Leessoort.ROLVELD, sleutel, w);
        }

        /** The number of beans of {@code klasse} whose reference field {@code veld} names this rule's subject: the links
         *  of an input relation stored on the other end ({@link TmsTranspiler#rolcontrole}), bound off an accumulate,
         *  which over no bean still binds a row. */
        int rolvullers(Class<?> klasse, String veld) {
            String sleutel = "rolvullers " + klasse.getName() + " " + veld;
            int bekend = bestaand(sleutel);
            if (bekend >= 0) return bekend;
            String vuller = "$vuller_" + id("rol");
            String aantal = "$vullers_" + id("rol");
            standen(new Accumulatie(new Patroon(klasse, vuller, List.of(),
                    List.of(new Join(id("rolvuller"), veld, ONDERWERP)), List.of()),
                    List.of(new Accumulatie.Functie(Accumulatie.Soort.AANTAL, null, aantal))));
            return nieuw(Leessoort.ROLVULLERS, sleutel, aantal);
        }

        /** The number of links of the created relation {@code relatie} that hold this rule's subject at its first end
         *  where {@code onderwerpIsEerste}, else at its second, with an instance of {@code klasse} at the other end (null:
         *  any): the {@link Koppeling} facts, bound off an accumulate, which over no link still binds a row
         *  ({@link TmsTranspiler#rolcontrole}). */
        int rolkoppelingen(String relatie, boolean onderwerpIsEerste, Class<?> klasse) {
            String sleutel = "rolkoppelingen " + relatie + " " + onderwerpIsEerste + " "
                    + (klasse == null ? "" : klasse.getName());
            int bekend = bestaand(sleutel);
            if (bekend >= 0) return bekend;
            String k = "$rolkoppeling_" + id("rol");
            String aantal = "$rolkoppelingen_" + id("rol");
            // the test of the other end's class reads the link alone, so it goes with the relation and the join to the
            // subject stays an identity join that the memories can hash
            List<Alfa.Veldtoets> toetsen = new ArrayList<>();
            toetsen.add(new Alfa.Veldtoets("relatie", Alfa.Vergelijker.GELIJK, relatie));
            if (klasse != null)
                toetsen.add(new Alfa.Veldtoets(onderwerpIsEerste ? "tweede" : "eerste", Alfa.Vergelijker.IS_EEN, klasse));
            Alfa vanRelatie = new Alfa(id("rolkoppeling"), toetsen);
            Join opOnderwerp = new Join(id("rolkoppeling-onderwerp"), onderwerpIsEerste ? "eerste" : "tweede", ONDERWERP);
            standen(new Accumulatie(new Patroon(Koppeling.class, k, List.of(vanRelatie), List.of(opOnderwerp), List.of()),
                    List.of(new Accumulatie.Functie(Accumulatie.Soort.AANTAL, null, aantal))));
            return nieuw(Leessoort.ROLKOPPELINGEN, sleutel, aantal);
        }

        /** The parent of this rule's subject in the scope of a uniqueness ({@link TmsTranspiler#sleutelregel}): the
         *  reference field on the subject that the scope's binding names, the role the parent plays, read off the
         *  subject's bean, since the relation is input; null where the subject has no parent. A scope over a relation a
         *  form creates, whose links are in no field, is refused. */
        int scopelezing(RolBindingRef scopeRol) {
            String sleutel = "scope " + scopeRol.feittype() + " " + scopeRol.rol();
            int bekend = bestaand(sleutel);
            if (bekend >= 0) return bekend;
            RegelSpraakModel.Rol[] paar = FactModelGenerator.gebondenRolpaar(model, scopeRol);
            if (paar == null)
                throw new Weigering("de scope van een Uniqueness noemt geen gedeclareerde rol (" + scopeRol.feittype() + ", "
                        + scopeRol.rol() + ")");
            if (scopeRol.feittype() != null && gecreeerdeRelaties.contains(Namen.norm(scopeRol.feittype())))
                throw new Weigering("de scope van een Uniqueness over de gecreëerde relatie '" + scopeRol.feittype()
                        + "': " + NOG_NIET);
            Objecttype ouder = model.objecttypeOpNaam(paar[0].objecttypeNaam());
            if (!FactModelGenerator.heeftReferentieVeld(model, subject, paar[0].naam(), ouder))
                throw new Weigering("de scope van een Uniqueness: " + FactModelGenerator.geenReferentieveldReden(model,
                        subject, paar[0].naam(), ouder) + " (§13.3.9 opslagrichting)");
            String w = "$lezing_" + slots.size();
            bindOnderwerp(new Binding(w, List.of(), new Binding.Veld(bonenklasse(subject), paar[0].naam()),
                    Binding.Leesvorm.WAARDE, null));
            return nieuw(Leessoort.SCOPE, sleutel, w);
        }

        private int bestaand(String sleutel) {
            Integer i = index.get(sleutel);
            if (i != null && gebruik != null) gebruik.add(i);
            return i == null ? -1 : i;
        }

        /** A slot that a binding of the plan binds to the variable {@code naam}, read once per rule under
         *  {@code sleutel}. */
        private int nieuw(Leessoort soort, String sleutel, String naam) {
            int i = slots.size();
            dev.uchar.regelspraak.transpiler.tms.plan.Lezing l = new dev.uchar.regelspraak.transpiler.tms.plan.Lezing(i, soort, sleutel, naam);
            slots.add(l);
            if (weergave != null) weergave.lezing(l);
            index.put(sleutel, i);
            if (gebruik != null) gebruik.add(i);
            return i;
        }

        /** A slot that a pattern built outside the plan binds to {@code v}, read once per rule under {@code sleutel}:
         *  the places of a to-many step that the range of a filter fault rule binds ({@link TmsTranspiler#plaats}),
         *  until step 1c plans that range too. */
        int extern(Leessoort soort, String sleutel, Variable<Object> v) {
            int i = slots.size();
            dev.uchar.regelspraak.transpiler.tms.plan.Lezing l = new dev.uchar.regelspraak.transpiler.tms.plan.Lezing(i, soort, sleutel, v.getName());
            if (weergave == null)
                throw new IllegalStateException("een slot dat een patroon buiten het plan bindt, in een regel uit haar plan");
            slots.add(l);
            weergave.extern(l, v);
            index.put(sleutel, i);
            if (gebruik != null) gebruik.add(i);
            return i;
        }

        /** A slot that no pattern binds: the consequence fills it. */
        private int ongebonden(Leessoort soort) {
            dev.uchar.regelspraak.transpiler.tms.plan.Lezing l = new dev.uchar.regelspraak.transpiler.tms.plan.Lezing(slots.size(), soort, null, null);
            slots.add(l);
            if (weergave != null) weergave.lezing(l);
            return l.slot();
        }

        private void bindOnderwerp(Binding b) {
            onderwerpbindingen.add(b);
            if (weergave != null) weergave.bindOnderwerp(b);
        }

        private void parameters(LhsItem item) {
            parameters.add(item);
            if (weergave != null) weergave.item(item);
        }

        private void standen(LhsItem item) {
            standen.add(item);
            if (weergave != null) weergave.item(item);
        }

        /** The standing of {@code lid} of the subject, a {@link Stand} or a {@link KenmerkStand}, binding {@code veld}
         *  of it to {@code w}: a constraint on the member, the join on the subject, the binding. */
        private Patroon stand(Class<?> klasse, String naam, String lidveld, ElementSleutel lk, String veld, String w) {
            Alfa vanLid = gelijk(id("stand"), lidveld, lk);
            Join opOnderwerp = opOnderwerp(id("stand-onderwerp"));
            return new Patroon(klasse, naam, List.of(vanLid), List.of(opOnderwerp), List.of(Binding.van(w, klasse, veld)));
        }

        int eigen(Lid lid) {
            String sleutel = "eigen " + lid.sleutel();
            int bekend = bestaand(sleutel);
            if (bekend >= 0) return bekend;
            String w = "$lezing_" + slots.size();
            ElementSleutel lk = lid.sleutel();
            if (schrijvers.containsKey(lk)) {
                if (lid.kenmerk()) {
                    standen(stand(KenmerkStand.class, "$stand_" + slots.size(), "kenmerk", lk, "waar", w));
                    afgeleid.put(lk, lid);
                    return nieuw(Leessoort.KENMERKSTAND, sleutel, w);
                }
                standen(stand(Stand.class, "$stand_" + slots.size(), "attribuut", lk, "waarde", w));
                afgeleid.put(lk, lid);
                return nieuw(Leessoort.STAND, sleutel, w);
            }
            // input: no form writes it, so it does not change while the session fires and is read off the bean
            bindOnderwerp(new Binding(w, List.of(), new Binding.Veld(lid.klasse(), lid.naam()),
                    lid.kenmerk() ? Binding.Leesvorm.WAAR : Binding.Leesvorm.WAARDE, null));
            return nieuw(Leessoort.INVOER, sleutel, w);
        }

        /**
         * The timeline of a member on a timeline of this rule's subject ({@link Lid#tijd}), or null where the member
         * holds none (an absent timeline, RUNTIME-SEMANTICS ch. 3.1). An attribute is bound off its {@link Stand},
         * whether a form writes it or not: the standing carries the timeline a form derived, and otherwise the input,
         * as an immutable {@code List<Tijdvak>}, or as an {@link Emmerscalair} where the case gives the input in the
         * asOfDate-bucket convention ({@link Emmerinvoer}). The bean does not show that convention, and every reader of
         * the full timeline must tell it apart (ch. 8.4), so the input is read where the standing rules join that fact
         * ({@link #attribuutstandregels}). A characteristic on a timeline is input: no executor writes one (ch. 3.11),
         * and a case delivers it only as its true periods, so they are bound off the bean, turned into that form there
         * ({@link Tijdvak#lijst}), and never through {@link #kenmerkstandregels}, whose test of {@code true} is false on
         * a list; the bean does not change while the session fires, so the binding is stable.
         */
        int tijdlijn(Lid lid) {
            String sleutel = "tijdlijn " + lid.sleutel();
            int bekend = bestaand(sleutel);
            if (bekend >= 0) return bekend;
            String w = "$tijdlijn_" + slots.size();
            ElementSleutel lk = lid.sleutel();
            if (lid.kenmerk()) {
                if (schrijvers.containsKey(lk))
                    throw new Weigering("het tijdsafhankelijke kenmerk '" + lid.naam() + "' van " + lid.declarant().naam
                            + ", dat een vorm schrijft: " + NOG_NIET + " (RUNTIME-SEMANTICS ch. 3.11)");
                bindOnderwerp(new Binding(w, List.of(), new Binding.Veld(lid.klasse(), lid.naam()),
                        Binding.Leesvorm.TIJDVAKKEN, null));
            } else {
                standen(stand(Stand.class, "$stand_" + slots.size(), "attribuut", lk, "waarde", w));
                afgeleid.put(lk, lid);
            }
            return nieuw(Leessoort.TIJDLIJN, sleutel, w);
        }

        /** A member of the object {@code pad} reaches from this rule's subject, null where a link on the path is absent.
         *  An attribute on a timeline is read through its standing at the end of the path, as {@link #tijdlijn} reads
         *  one of the subject's own, so the read carries the case's input convention, a {@code List<Tijdvak>} or an
         *  {@link Emmerscalair}; a characteristic on a timeline, which is input only, is bound as its true periods, a
         *  {@code List<Tijdvak>} ({@link #invoerlezing}). */
        int genavigeerd(Pad pad, Lid lid) {
            String sleutel = "pad " + pad.sleutel() + " " + lid.sleutel();
            int bekend = bestaand(sleutel);
            if (bekend >= 0) return bekend;
            String w = "$lezing_" + slots.size();
            ElementSleutel lk = lid.sleutel();
            if (lid.tijd() != null && lid.kenmerk() && schrijvers.containsKey(lk))
                throw new Weigering("het tijdsafhankelijke kenmerk '" + lid.naam() + "' van " + lid.declarant().naam
                        + ", dat een vorm schrijft: " + NOG_NIET + " (RUNTIME-SEMANTICS ch. 3.11)");
            // a derived member, an attribute on a timeline, or any member at the end of a path whose links a form
            // creates: through the path's standing, whose rules bind every link and the member in their left-hand sides
            if (schrijvers.containsKey(lk) || tijdlijnattribuut(lid) || pad.gecreeerd()) {
                Padlezing p = new Padlezing(pad, lid);
                String ps = pad.sleutel();
                String st = "$padstand_" + slots.size();
                Alfa vanPad = new Alfa(id("padstand"), List.of(new Alfa.Veldtoets("pad", Alfa.Vergelijker.GELIJK, ps),
                        new Alfa.Veldtoets("lid", Alfa.Vergelijker.GELIJK, lk)));
                Join opOnderwerp = opOnderwerp(id("padstand-onderwerp"));
                standen(new Patroon(PadStand.class, st, List.of(vanPad), List.of(opOnderwerp),
                        List.of(Binding.van(w, PadStand.class, "waarde"))));
                paden.put(p.sleutel(), p);
                return nieuw(Leessoort.PADSTAND, sleutel, w);
            }
            // input over links that are input: bound off the subject's bean, null where a link is absent
            bindOnderwerp(new Binding(w, pad.velden(), new Binding.Veld(lid.klasse(), lid.naam()), invoervorm(lid),
                    null));
            return nieuw(Leessoort.INVOERPAD, sleutel, w);
        }

        /** A parameter, bound off its {@link ParameterStand}: no test or consequence names a global itself, since two
         *  globals in one constraint of the executable model read as the last one, and a source over a global was not
         *  kept apart from another parameter's in another rule ({@link ParameterStand}). A parameter the case leaves
         *  unset has a standing with a null value and reads leeg. */
        int parameter(Parameter p) {
            String naam = Namen.ident(p.naam(), false);
            String sleutel = "parameter " + naam;
            int bekend = bestaand(sleutel);
            if (bekend >= 0) return bekend;
            global(p);
            String w = "$lezing_" + slots.size();
            String st = "$parameter_" + slots.size();
            parameters(new Patroon(ParameterStand.class, st, List.of(gelijk(id("parameter"), "parameter", naam)),
                    List.of(), List.of(Binding.van(w, ParameterStand.class, "waarde"))));
            return nieuw(Leessoort.PARAMETER, sleutel, w);
        }
    }

    /** The name of the variable of a rule's subject in every plan: the name joins on the subject refer to. */
    private static final String ONDERWERP = "$onderwerp";

    /** A constraint that field {@code veld} of a fact equals {@code waarde}. */
    private static Alfa gelijk(String id, String veld, Object waarde) {
        return new Alfa(id, List.of(new Alfa.Veldtoets(veld, Alfa.Vergelijker.GELIJK, waarde)));
    }

    /** The join of a standing fact on the rule's subject: its field {@code onderwerp} is the subject. */
    private static Join opOnderwerp(String id) {
        return new Join(id, "onderwerp", ONDERWERP);
    }

    /** The rule that puts the {@link ParameterStand} of one parameter in working memory: it matches no fact, so it fires
     *  once per session, and it binds the global in its consequence, where the case runner set it before the session
     *  fired. The global does not change while the session fires, so the standing never needs to be withdrawn. */
    private Rule parameterstandregel(String naam) {
        return weergeef(new Regelplan("parameterstand " + naam, null, null, new Leestabel(List.of()), List.of(),
                new Gevolg.Parameterstand(naam, naam, Gevolg.Invoeging.LOGISCH)));
    }

    /** The global of parameter {@code p}, of the Java type of its datatype, so that the case runner sets it by name;
     *  one per parameter for the whole model. */
    private Global<?> global(Parameter p) {
        String naam = Namen.ident(p.naam(), false);
        Global<?> g = globals.get(naam);
        if (g != null) return g;
        if (p.granulariteit() != null)
            throw new Weigering("de tijdsafhankelijke parameter '" + p.naam() + "': " + NOG_NIET + "; de goldens met"
                    + " cases die er een lezen, doen dat in een TemporalRead, die de lader voor alle takken weigert");
        Datatype t = p.datatype();
        Class<?> klasse = t.isNumeriek() || t.isPercentage() ? BigDecimal.class
                : t instanceof Datatype.DatumInDagen ? java.time.LocalDate.class
                : t instanceof Datatype.DatumTijd ? java.time.LocalDateTime.class
                : t.isTekst() || t instanceof Datatype.Enumeratie ? String.class
                : t.isBoolean() ? Boolean.class : null;
        if (klasse == null) throw new Weigering("de parameter '" + p.naam() + "' van type " + typenaam(t) + ": " + NOG_NIET);
        g = DSL.globalOf(klasse, PKG, naam);
        globals.put(naam, g);
        return g;
    }

    private static int[] posities(Set<Integer> gebruik) {
        return gebruik.stream().mapToInt(Integer::intValue).toArray();
    }

    /** The body of a consequence: the Drools handle, the subject, and the bound values at their indices. */
    @FunctionalInterface
    private interface Gevolgfunctie {
        void voer(org.drools.model.Drools drools, Object onderwerp, Object[] waarden);
    }

    /** A consequence over the subject and the values at {@code pos}, of any number. */
    private static RuleItemBuilder<?> gevolg(Lezingen lez, int[] pos, Gevolgfunctie f, int n) {
        return gevolg(lez, lez.s, pos, f, n);
    }

    /** A consequence over {@code onderwerp} and the values at {@code pos}, of any number. */
    @SuppressWarnings("rawtypes")
    private static RuleItemBuilder<?> gevolg(Lezingen lez, Variable<?> onderwerp, int[] pos, Gevolgfunctie f, int n) {
        Variable[] v = new Variable[pos.length + 1];
        v[0] = onderwerp;
        for (int j = 0; j < pos.length; j++) v[j + 1] = lez.variabelen.get(pos[j]);
        return new PatternDslWeergave.Consequentie(v, (drools, args) -> {
            Object[] w = new Object[n];
            for (int j = 0; j < pos.length; j++) w[pos[j]] = args[j + 1];
            f.voer(drools, args[0], w);
        });
    }

    // ---------------- standings ----------------

    /**
     * The three generic rules that keep one {@link KenmerkStand} per object of the declaring type: waar from the input,
     * waar from a derivation, onwaar when neither holds. The onwaar rule carries the branch's only negation over a
     * characteristic, {@code not HeeftKenmerk}, and reads the input in its left-hand side, so a derivation that appears
     * later retracts the onwaar standing and every conclusion that read it. When both sources hold, the two waar
     * standings are equal, and truth maintenance keeps them as one fact with a justification each. The input is read
     * off the bean as whether it holds {@code true} ({@link Binding.Leesvorm#WAAR}), the one truth value the standing
     * holds: a characteristic on a timeline is never read through it ({@link Lezingen#tijdlijn}).
     */
    private List<Rule> kenmerkstandregels(Lid lid) {
        ElementSleutel sleutel = lid.sleutel();
        String naam = "kenmerkstand " + lid.declarant().naam + "." + lid.naam() + ": ";
        Alfa invoerWaar = invoerWaar(id("invoer"), lid, true);
        Rule invoer = weergeef(new Regelplan(naam + "waar uit de invoer", null, null, new Leestabel(List.of()),
                List.of(new Onderwerp(lid.klasse(), ONDERWERP, List.of(invoerWaar), List.of(), List.of())),
                new Gevolg.Kenmerkstand(ONDERWERP, sleutel, Gevolg.Standbron.INVOER, true, Gevolg.Invoeging.LOGISCH)));
        Rule afleiding = weergeef(new Regelplan(naam + "waar uit een afleiding", null, null, new Leestabel(List.of()),
                List.of(new Onderwerp(lid.klasse(), ONDERWERP, List.of(), List.of(), List.of()), afgeleid(sleutel)),
                new Gevolg.Kenmerkstand(ONDERWERP, sleutel, Gevolg.Standbron.AFLEIDING, true,
                        Gevolg.Invoeging.LOGISCH)));
        Alfa geenInvoer = invoerWaar(id("geen-invoer"), lid, false);
        Rule onwaar = weergeef(new Regelplan(naam + "onwaar", null, null, new Leestabel(List.of()),
                List.of(new Onderwerp(lid.klasse(), ONDERWERP, List.of(geenInvoer), List.of(), List.of()),
                        new Niet(afgeleid(sleutel))),
                new Gevolg.Kenmerkstand(ONDERWERP, sleutel, Gevolg.Standbron.GEEN, false, Gevolg.Invoeging.LOGISCH)));
        return List.of(invoer, afleiding, onwaar);
    }

    /** The test that the input of characteristic {@code lid} on the subject's bean, read as whether it holds
     *  {@code true}, is {@code waar}. */
    private static Alfa invoerWaar(String id, Lid lid, boolean waar) {
        return new Alfa(id, List.of(new Alfa.Veldtoets(List.of(), lid.naam(), Binding.Leesvorm.WAAR,
                Alfa.Vergelijker.GELIJK, waar)));
    }

    /** The pattern of the {@link HeeftKenmerk} of {@code kenmerk} that a derivation put on the subject: a constraint on
     *  the characteristic, the join on the subject. */
    private Patroon afgeleid(ElementSleutel kenmerk) {
        return new Patroon(HeeftKenmerk.class, "$afgeleid", List.of(gelijk(id("afgeleid"), "kenmerk", kenmerk)),
                List.of(opOnderwerp(id("afgeleid-onderwerp"))), List.of());
    }

    /**
     * The generic rules that keep one {@link Stand} per object of the declaring type: the derived value where a
     * {@link Waarde} exists, else the input on the bean, read in the left-hand side, leeg included. An input rule
     * carries {@code not Waarde}, so a derivation that appears later retracts the input standing and every value that
     * read it. A member no form writes has no derivation, and only an attribute on a timeline is read through its
     * standing then ({@link Lezingen#tijdlijn}).
     *
     * <p>The input timeline of a member on a timeline stands in the form a {@link Waarde} carries one,
     * {@code List<Tijdvak>} ({@link Tijdvak#lijst}), so every standing of the member holds one representation, except
     * where the case gives the input as the asOfDate-bucket scalar: then the input stands as an {@link Emmerscalair}
     * (RUNTIME-SEMANTICS ch. 8.4). The bean holds that scalar as the one segment of its period, which a full timeline of
     * one segment would be as well, so two input rules join the {@link Emmerinvoer} the case input states beside the
     * beans, the one under {@code exists} and the other under {@code not}; the fact is stated and never changes while
     * the session fires, so exactly one of them holds for each object.
     */
    private List<Rule> attribuutstandregels(Lid lid) {
        ElementSleutel sleutel = lid.sleutel();
        String naam = "stand " + lid.declarant().naam + "." + lid.naam() + ": ";
        List<Rule> uit = new ArrayList<>();

        if (schrijvers.containsKey(sleutel)) {
            Patroon waarde = new Patroon(Waarde.class, "$waarde", List.of(gelijk(id("waarde"), "attribuut", sleutel)),
                    List.of(opOnderwerp(id("waarde-onderwerp"))), List.of(Binding.van("$v", Waarde.class, "waarde")));
            uit.add(weergeef(new Regelplan(naam + "uit een afleiding", null, null,
                    eenSlot(Leessoort.AFLEIDING, "afleiding " + sleutel, "$v"),
                    List.of(new Onderwerp(lid.klasse(), ONDERWERP, List.of(), List.of(), List.of()), waarde),
                    new Gevolg.Stand(ONDERWERP, sleutel, Gevolg.Standbron.AFLEIDING, 0, false,
                            Gevolg.Invoeging.LOGISCH))));
        }

        if (lid.tijd() == null) {
            uit.add(invoerstandregel(lid, naam + "uit de invoer", null));
            return uit;
        }
        uit.add(invoerstandregel(lid, naam + "uit de invoer", false));
        uit.add(invoerstandregel(lid, naam + "uit de invoer, een emmerscalair", true));
        return uit;
    }

    /** The input rule {@code naam} of {@link #attribuutstandregels}: where no {@link Waarde} stands on the object, the
     *  {@link Stand} of the input on the bean, read as it is, or as its segments for a member on a timeline
     *  ({@link #invoervorm}); with {@code emmer} null for a member that holds one value, and for a member on a timeline
     *  only where an {@link Emmerinvoer} of the member on the object exists ({@code true}), the input then standing as
     *  its {@link Emmerscalair}, or does not ({@code false}). The bucket is taken in the consequence, from the bound
     *  timeline: a binding is computed for every object the subject pattern matches, also one whose input is a full
     *  timeline. */
    private Rule invoerstandregel(Lid lid, String naam, Boolean emmer) {
        ElementSleutel sleutel = lid.sleutel();
        List<LhsItem> items = new ArrayList<>();
        items.add(new Onderwerp(lid.klasse(), ONDERWERP, List.of(), List.of(), List.of(new Binding("$invoer", List.of(),
                new Binding.Veld(lid.klasse(), lid.naam()), invoervorm(lid), null))));
        if (emmer != null) {
            Patroon p = new Patroon(Emmerinvoer.class, "$emmerinvoer",
                    List.of(gelijk(id("emmerinvoer"), "attribuut", sleutel)),
                    List.of(opOnderwerp(id("emmerinvoer-onderwerp"))), List.of());
            items.add(emmer ? new Bestaat(List.of(p)) : new Niet(p));
        }
        items.add(new Niet(new Patroon(Waarde.class, "$waarde", List.of(gelijk(id("waarde"), "attribuut", sleutel)),
                List.of(opOnderwerp(id("waarde-onderwerp"))), List.of())));
        return weergeef(new Regelplan(naam, null, null, eenSlot(Leessoort.INVOER, "invoer " + sleutel, "$invoer"),
                items, new Gevolg.Stand(ONDERWERP, sleutel, Gevolg.Standbron.INVOER, 0, Boolean.TRUE.equals(emmer),
                        Gevolg.Invoeging.LOGISCH)));
    }

    /** The value array of a standing rule whose consequence reads one value: slot 0, of {@code soort}, which the
     *  variable {@code naam} binds. */
    private static Leestabel eenSlot(Leessoort soort, String sleutel, String naam) {
        return new Leestabel(List.of(new dev.uchar.regelspraak.transpiler.tms.plan.Lezing(0, soort, sleutel, naam)));
    }

    /**
     * The two generic rules that keep one {@link PadStand} per object of the subject type for a derived member read over
     * a path: where every link on the path is present, the standing of the member on the object the path reaches,
     * joined on that object; where a link is absent, null. The links are input, read off the bean in the left-hand
     * side, and do not change while the session fires, so exactly one of the two holds for each object. Both rules
     * read the path along its reference fields ({@link Pad#velden}): the test whether it reaches an object, and the
     * binding of that object, which walks to the last field and reads it there.
     */
    private List<Rule> padstandregels(Padlezing p) {
        Pad pad = p.pad();
        Lid lid = p.lid();
        String ps = pad.sleutel();
        ElementSleutel lk = lid.sleutel();
        Class<?> klasse = bonenklasse(pad.subject());
        String naam = "padstand " + pad.subject().naam + "." + String.join(".", pad.rollen()) + " naar "
                + lid.declarant().naam + "." + lid.naam() + ": ";
        List<Binding.Veld> velden = pad.velden();

        Alfa link = langsPad(id("link"), velden, Alfa.Vergelijker.GEVULD);
        Binding bereikt = new Binding("$bereikt", velden.subList(0, velden.size() - 1), velden.get(velden.size() - 1),
                Binding.Leesvorm.WAARDE, null);
        Class<?> standklasse = lid.kenmerk() ? KenmerkStand.class : Stand.class;
        Patroon stand = new Patroon(standklasse, "$stand",
                List.of(gelijk(id("stand"), lid.kenmerk() ? "kenmerk" : "attribuut", lk)),
                List.of(new Join(id("stand-bereikt"), "onderwerp", "$bereikt")),
                List.of(Binding.van("$v", standklasse, lid.kenmerk() ? "waar" : "waarde")));
        Rule aanwezig = weergeef(new Regelplan(naam + "link aanwezig", null, null,
                eenSlot(Leessoort.BEREIKTE_STAND, "stand " + ps + " " + lk, "$v"),
                List.of(new Onderwerp(klasse, ONDERWERP, List.of(link), List.of(), List.of(bereikt)), stand),
                new Gevolg.Padstand(ONDERWERP, ps, lk, Gevolg.Standbron.BEREIKT, 0, Gevolg.Invoeging.LOGISCH)));

        Alfa geenLink = langsPad(id("geen-link"), velden, Alfa.Vergelijker.LEEG);
        Rule afwezig = weergeef(new Regelplan(naam + "link afwezig", null, null, new Leestabel(List.of()),
                List.of(new Onderwerp(klasse, ONDERWERP, List.of(geenLink), List.of(), List.of())),
                new Gevolg.Padstand(ONDERWERP, ps, lk, Gevolg.Standbron.ONTBREKENDE_LINK, -1,
                        Gevolg.Invoeging.LOGISCH)));
        return List.of(aanwezig, afwezig);
    }

    /** The test {@code op} of the object the walk over the reference fields {@code velden} reaches from the subject,
     *  null where a link on it is absent: {@code GEVULD} where the path reaches an object, {@code LEEG} where not. */
    private static Alfa langsPad(String id, List<Binding.Veld> velden, Alfa.Vergelijker op) {
        return new Alfa(id, List.of(new Alfa.Veldtoets(velden, null, Binding.Leesvorm.WAARDE, op, null)));
    }

    /**
     * The two rules of {@link #padstandregels} for a path with a hop over a relation a form creates: such a relation
     * gains links while the session fires, so the path is a chain of patterns in the left-hand side, a hop over a
     * created relation joining its {@link Koppeling} facts and a hop over an input relation the reference field of the
     * near bean, each ending in a pattern over the reached bean ({@link #keten}). Where the chain reaches an object the
     * standing carries the member's value there, read off that bean where no form writes the member and through its
     * standing where one does, or where it is an attribute on a timeline, whose standing carries the case's input
     * convention; where it does not, the value is null, under a {@code not} over the same chain. A link that a creation
     * adds takes the standing along.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private List<Rule> koppelingpadstandregels(Padlezing p) {
        Pad pad = p.pad();
        Lid lid = p.lid();
        String ps = pad.sleutel();
        ElementSleutel lk = lid.sleutel();
        Class<?> klasse = bonenklasse(pad.subject());
        String naam = "padstand " + pad.subject().naam + "." + String.join(".", pad.rollen()) + " naar "
                + lid.declarant().naam + "." + lid.naam() + ": ";

        Variable<Object> s1 = (Variable<Object>) DSL.declarationOf(klasse, "$onderwerp");
        List<RuleItemBuilder<?>> items = new ArrayList<>();
        items.add(PatternDSL.pattern(s1));
        PatternDSL.PatternDef<Object> bereikt = keten(pad.hops(), s1, items, null);
        Variable<Object> v = DSL.declarationOf(Object.class, "$v");
        if (schrijvers.containsKey(lk) || tijdlijnattribuut(lid)) {
            Variable<Object> doel = bereikt.getFirstVariable();
            if (lid.kenmerk()) {
                Variable<KenmerkStand> st = DSL.declarationOf(KenmerkStand.class, "$stand");
                items.add(zelfde(PatternDSL.pattern(st).expr(id("stand"), x -> x.kenmerk().equals(lk)), "stand-bereikt",
                        doel, KenmerkStand::onderwerp).bind(v, KenmerkStand::waar));
            } else {
                Variable<Stand> st = DSL.declarationOf(Stand.class, "$stand");
                items.add(zelfde(PatternDSL.pattern(st).expr(id("stand"), x -> x.attribuut().equals(lk)), "stand-bereikt",
                        doel, Stand::onderwerp).bind(v, Stand::waarde));
            }
        } else {
            Function<Object, Object> lees = invoerlezing(lid);
            bereikt.bind(v, o -> lees.apply(o));
        }
        items.add(DSL.on(s1, v).execute((Block3<org.drools.model.Drools, Object, Object>) (drools, o, w) ->
                drools.logicalInsert(new PadStand(o, ps, lk, w))));
        Rule aanwezig = PatternDSL.rule(PKG, naam + "link aanwezig").build(items.toArray(new RuleItemBuilder[0]));

        Variable<Object> s2 = (Variable<Object>) DSL.declarationOf(klasse, "$onderwerp");
        List<RuleItemBuilder<?>> geen = new ArrayList<>();
        keten(pad.hops(), s2, geen, null);
        Rule afwezig = PatternDSL.rule(PKG, naam + "link afwezig").build(PatternDSL.pattern(s2),
                DSL.not((org.drools.model.view.ViewItem<?>) geen.get(0),
                        geen.subList(1, geen.size()).toArray(new org.drools.model.view.ViewItem<?>[0])),
                DSL.on(s2).execute((Block2<org.drools.model.Drools, Object>) (drools, o) ->
                        drools.logicalInsert(new PadStand(o, ps, lk, null))));
        return List.of(aanwezig, afwezig);
    }

    /**
     * The patterns that walk {@code hops} from the object {@code van} binds, into {@code uit}: per hop a pattern over the
     * bean the hop reaches, joined to the near object by the reference field of the end that holds it, or, over a
     * relation a form creates, through a {@link Koppeling} pattern whose other end the bean pattern then binds. Returns
     * the pattern over the last bean reached.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private PatternDSL.PatternDef<Object> keten(List<Hop> hops, Variable<Object> van, List<RuleItemBuilder<?>> uit,
                                                Variable<Object> fase) {
        Variable<Object> nabij = van;
        PatternDSL.PatternDef<Object> laatste = null;
        for (Hop h : hops) {
            Variable<Object> ver = (Variable<Object>) DSL.declarationOf(bonenklasse(h.naar()), "$stap_" + id("keten"));
            PatternDSL.PatternDef<Object> p = PatternDSL.pattern(ver);
            Variable<Object> n = nabij;
            if (h.relatie() != null) {
                String relatie = h.relatie();
                boolean eerste = h.nabijIsEerste();
                Variable<Koppeling> k = DSL.declarationOf(Koppeling.class, "$koppeling_" + id("keten"));
                Variable<Object> overzijde = DSL.declarationOf(Object.class, "$overzijde_" + id("keten"));
                PatternDSL.PatternDef<Koppeling> kp = zelfde(PatternDSL.pattern(k)
                        .expr(id("koppeling"), x -> x.relatie().equals(relatie)), "koppeling-nabij", n,
                        x -> eerste ? x.eerste() : x.tweede());
                // a creation rule does not walk a link of the phase it fires in: the rule layer has not seen it yet
                if (fase != null) kp.expr(id("koppeling-eerder"), fase, (x, f) -> x.fase() != (Integer) f);
                uit.add(kp.bind(overzijde, x -> eerste ? x.tweede() : x.eerste()));
                zelfde(p, "koppeling-ver", overzijde, x -> x);
            } else if (h.opNabij()) {
                Function<Object, Object> lees = h.lees();
                p.expr(id("veld-nabij"), n, (x, o) -> lees.apply(o) == x);
            } else {
                Function<Object, Object> lees = h.lees();
                p.expr(id("veld-ver"), n, (x, o) -> lees.apply(x) == o);
            }
            uit.add(p);
            nabij = ver;
            laatste = p;
        }
        return laatste;
    }

    // ---------------- members, paths and beans ----------------

    /** A member as this branch keys and reaches it: the key on its declaring type, that type, the declared spelling (the
     *  bean property), whether it is a characteristic, its declared datatype (null for a characteristic), the bean
     *  class of the declaring type, whose subtypes' beans extend it, and, for a member on a timeline, the granularity of
     *  its grid (null for a member that holds one value). A member on a timeline is read and written as a
     *  {@code List<Tijdvak>} ({@link Tijdvak}). */
    private record Lid(ElementSleutel sleutel, Objecttype declarant, String naam, boolean kenmerk, Datatype datatype,
                       Class<?> klasse, RegelSpraakModel.Granulariteit tijd) {}

    /** Whether {@code lid} is an attribute on a timeline: one the branch reads through its standing, derived or input,
     *  whose input stands in the convention the case gave it ({@link #attribuutstandregels}). */
    private static boolean tijdlijnattribuut(Lid lid) {
        return lid.tijd() != null && !lid.kenmerk();
    }

    /** The read of the input member {@code lid} off a bean of a type that declares it: a characteristic on a timeline as
     *  its true periods, a {@code List<Tijdvak>} ({@link Tijdvak#lijst}), null for an absent timeline, a characteristic
     *  that holds one truth value as that value, and any other member as its value. An attribute on a timeline is read
     *  through its standing instead ({@link #tijdlijnattribuut}). A closure, for the one rule family that still builds
     *  its rules itself ({@link #koppelingpadstandregels}, until step 1c-C of card
     *  drl-teksttak-op-truth-maintenance); a plan carries the same decision as a binding's form of reading
     *  ({@link #invoervorm}). */
    private static Function<Object, Object> invoerlezing(Lid lid) {
        return PatternDslWeergave.invoerlezer(new Binding.Veld(lid.klasse(), lid.naam()), invoervorm(lid), null);
    }

    /** How the input member {@code lid} is read off a bean: the decision a plan's binding carries
     *  ({@link Binding.Leesvorm}), which {@link #invoerlezing} takes as well. */
    private static Binding.Leesvorm invoervorm(Lid lid) {
        if (lid.tijd() != null) return Binding.Leesvorm.TIJDVAKKEN;
        if (lid.kenmerk()) return Binding.Leesvorm.WAAR;
        return Binding.Leesvorm.WAARDE;
    }

    /** Member {@code naam} as declared on {@code houder}'s chain, compared as the verifier compares names, holding one
     *  value. A member on a timeline is refused here: the positions that read or write one ask {@link #tijdlid}. A
     *  member with dimensions is refused. */
    private Lid lid(Objecttype houder, String naam, boolean kenmerk) {
        Lid lid = gedeclareerd(houder, naam, kenmerk);
        if (lid.tijd() != null)
            throw new Weigering("het tijdsafhankelijke " + (kenmerk ? "kenmerk" : "attribuut") + " '" + lid.naam()
                    + "' van " + lid.declarant().naam + " op een plek die één waarde leest of schrijft: deze"
                    + " tak leest een tijdlijn alleen in een totaal, de knip ervan, een tijdsevenredig"
                    + " deel, een dagentelling en een uniciteitssleutel, en schrijft haar alleen met een tijdsevenredig"
                    + " deel of een dagentelling (RUNTIME-SEMANTICS ch. 3.3, 3.6–3.8, 3.11, 5.9)");
        return lid;
    }

    /**
     * Member {@code naam} as declared on {@code houder}'s chain, on a timeline: {@code wat} reads or writes its timeline
     * (RUNTIME-SEMANTICS ch. 3). Refused by name: a member that holds one value there, one on a grid of more than one
     * unit, which carries no operation (ch. 3.2, OPEN in ch. 3.12), and one on a timeline with a variable start point,
     * whose grid is laid per object from an anchor a {@code StartPointDetermination} derives: no golden with cases
     * reads or writes one, and the case input lays no timeline on such a grid, so no case can confirm this branch's
     * reading of it against the reference interpreter.
     */
    private Lid tijdlid(Objecttype houder, String naam, boolean kenmerk, String wat) {
        Lid lid = gedeclareerd(houder, naam, kenmerk);
        String soort = kenmerk ? "kenmerk" : "attribuut";
        if (lid.tijd() == null)
            throw new Weigering(wat + " over het " + soort + " '" + lid.naam() + "' van " + lid.declarant().naam
                    + ", dat niet tijdsafhankelijk is");
        if (Tijdlijnen.meerPeriodiek(kenmerk ? lid.declarant().kenmerkTijdlijnAantalLeden()
                : lid.declarant().tijdlijnAantalLeden(), lid.naam()))
            throw new Weigering(wat + " over het " + soort + " '" + lid.naam() + "' van " + lid.declarant().naam
                    + " op een raster van meer perioden, dat geen bewerking draagt (RUNTIME-SEMANTICS ch. 3.2, 3.12)");
        if (!kenmerk && model.heeftVariabelStartpunt(lid.declarant(), lid.naam()))
            throw new Weigering(wat + " over het attribuut '" + lid.naam() + "' van " + lid.declarant().naam + " op een"
                    + " tijdlijn met een variabel startpunt: " + NOG_NIET + "; geen golden met cases leest of schrijft er"
                    + " een, en de caselezer legt geen tijdlijn op zo'n raster");
        return lid;
    }

    /** Member {@code naam} as declared on {@code houder}'s chain, its granularity set where it lies on a timeline. A
     *  member with dimensions is refused. */
    private Lid gedeclareerd(Objecttype houder, String naam, boolean kenmerk) {
        for (Objecttype t : houder.keten()) {
            if (kenmerk) {
                for (String k : t.kenmerken)
                    if (Namen.norm(k).equals(Namen.norm(naam)))
                        return new Lid(ProducentConsument.elementVan(houder, naam, true), t, k, true, null, bonenklasse(t),
                                t.kenmerkGranulariteit.get(k));
            } else {
                for (Map.Entry<String, Datatype> e : t.attributen.entrySet())
                    if (Namen.norm(e.getKey()).equals(Namen.norm(naam))) {
                        String a = e.getKey();
                        // every position that asks this method reads or writes the member whole, as one value or as a
                        // timeline; a dimensioned one is read only per cell, through gedimensioneerdLid
                        if (t.attribuutDimensies.containsKey(a))
                            throw new Weigering("het gedimensioneerde attribuut '" + a + "' van " + t.naam + ", gelezen"
                                    + " of geschreven buiten een DimensionCellRead of een vouw over een"
                                    + " dimensieselectie: " + NOG_NIET + "; geen golden met cases heeft het");
                        return new Lid(ProducentConsument.elementVan(houder, naam, false), t, a, false, e.getValue(),
                                bonenklasse(t), t.attribuutGranulariteit.get(a));
                    }
            }
        }
        throw new Weigering("het " + (kenmerk ? "kenmerk" : "attribuut") + " '" + naam + "' is niet gedeclareerd op "
                + houder.naam + " of een supertype");
    }

    /** The dimensioned attribute {@code naam} as declared on {@code houder}'s chain (§3.6): its bean member is a
     *  {@code Map<label, value>}, read whole by a dimension cell or a fold over a dimension selection. An attribute that
     *  is not dimensioned, or that also lies on a timeline, is refused by name. */
    private Lid gedimensioneerdLid(Objecttype houder, String naam) {
        for (Objecttype t : houder.keten())
            for (Map.Entry<String, Datatype> e : t.attributen.entrySet())
                if (Namen.norm(e.getKey()).equals(Namen.norm(naam))) {
                    String a = e.getKey();
                    if (!t.attribuutDimensies.containsKey(a))
                        throw new Weigering("een dimensielezing van het attribuut '" + a + "' van " + t.naam + ", dat niet"
                                + " gedimensioneerd is");
                    if (t.attribuutGranulariteit.containsKey(a))
                        throw new Weigering("het gedimensioneerde attribuut '" + a + "' van " + t.naam + " op een"
                                + " tijdlijn: " + NOG_NIET + "; geen golden met cases heeft er een");
                    return new Lid(ProducentConsument.elementVan(houder, naam, false), t, a, false, e.getValue(),
                            bonenklasse(t), null);
                }
        throw new Weigering("het attribuut '" + naam + "' is niet gedeclareerd op " + houder.naam + " of een supertype");
    }

    /** A path of to-one roles from a subject type over the reference fields of the beans: the walk
     *  {@link FactModelGenerator#navigatiewandeling} derives from the wire's bindings, the reference field of each hop
     *  over an input relation on its bean class ({@code velden}, which a plan reads the path along, and only for a path
     *  whose links are all input, {@link #gecreeerd}), the hops, and the type the last hop reaches. {@code sleutel}
     *  names the subject type and the fields, as the {@link PadStand} facts of the path carry it. */
    private record Pad(String sleutel, Objecttype subject, List<String> rollen, Objecttype bereikt,
                       List<Binding.Veld> velden, List<Hop> hops) {
        /** Whether a hop of the path goes over a relation a form creates, whose links change while the session fires:
         *  such a path is read over {@link Koppeling} facts, never off the beans. */
        boolean gecreeerd() {
            return hops.stream().anyMatch(h -> h.relatie() != null);
        }
    }

    /** One hop of a path or a range, from an object of type {@code van} to one of type {@code naar}: over an input
     *  relation by {@code lees} on the bean of one end ({@code opNabij}: on the near object, else on the far one), over
     *  a relation a form creates by its {@link Koppeling} facts ({@code relatie} set, {@code nabijIsEerste} saying
     *  which end of the link the near object is). */
    private record Hop(Objecttype van, Objecttype naar, Function<Object, Object> lees, boolean opNabij, String relatie,
                       boolean nabijIsEerste) {}

    /** A derived member read over a path: one family of path-standing rules. */
    private record Padlezing(Pad pad, Lid lid) {
        String sleutel() { return pad.sleutel() + " " + Feitnamen.lid(lid.sleutel()); }
    }

    private Pad pad(Objecttype subject, List<RolBindingRef> bindingen, Objecttype eindpunt, String wat) {
        FactModelGenerator.Navigatiewandeling w =
                FactModelGenerator.navigatiewandeling(model, subject, bindingen, eindpunt);
        if (w == null) throw new Weigering(wat + ": de navigatie resolvet niet vanaf " + subject.naam);
        if (w.naarVeel()) throw new Weigering(wat + ": de navigatie gaat over een rol naar veel");
        if (w.rollen().size() != bindingen.size())
            throw new IllegalStateException(wat + ": de wandeling telt " + w.rollen().size() + " stappen en de draad "
                    + bindingen.size() + " bindingen");
        List<Binding.Veld> velden = new ArrayList<>();
        List<Hop> hops = new ArrayList<>();
        Objecttype cur = subject;
        for (int i = 0; i < w.rollen().size(); i++) {
            String rol = w.rollen().get(i);
            Objecttype volgend = w.bereikt().get(i);
            Hop gecreeerd = koppelingshop(bindingen.get(i), cur, volgend);
            if (gecreeerd != null) {
                hops.add(gecreeerd);
                cur = volgend;
                continue;
            }
            // a role whose name a declared member took has no reference field of its own (§13.3.9)
            if (!FactModelGenerator.heeftReferentieVeld(model, cur, rol, volgend))
                throw new Weigering(wat + ": " + cur.naam + " heeft geen referentieveld '" + rol + "' naar " + volgend.naam
                        + " (§13.3.9 opslagrichting)");
            velden.add(new Binding.Veld(bonenklasse(cur), rol));
            hops.add(new Hop(cur, volgend, Accessors.getter(bonenklasse(cur), rol), true, null, false));
            cur = volgend;
        }
        return new Pad(subject.naam + ":" + String.join(".", w.rollen()), subject, w.rollen(), cur, List.copyOf(velden),
                List.copyOf(hops));
    }

    /** The hop over {@code binding} from {@code van} to {@code naar} where its relation is one a form creates, read over
     *  {@link Koppeling} facts; null for an input relation. The binding names the role the far end fills, so the near
     *  end is the first of the link where that role is the fact type's second. */
    private Hop koppelingshop(RolBindingRef binding, Objecttype van, Objecttype naar) {
        if (binding == null || binding.feittype() == null || !gecreeerdeRelaties.contains(Namen.norm(binding.feittype())))
            return null;
        RegelSpraakModel.Feittype ft = feittype(binding.feittype());
        if (ft == null || ft.rollen.size() != 2 || ft.wederkerig)
            throw new Weigering("een stap over de gecreëerde relatie '" + binding.feittype() + "' die geen relatie van twee"
                    + " rollen is: " + NOG_NIET);
        boolean verIsTweede = Namen.norm(ft.rollen.get(1).naam()).equals(Namen.norm(binding.rol()));
        return new Hop(van, naar, null, false, Namen.norm(ft.naam), verIsTweede);
    }

    /** The declared fact type named {@code naam}, compared as the verifier compares names, or null. */
    private RegelSpraakModel.Feittype feittype(String naam) {
        for (RegelSpraakModel.Feittype ft : model.feittypen.values())
            if (Namen.norm(ft.naam).equals(Namen.norm(naam))) return ft;
        return null;
    }

    private Class<?> bonenklasse(Objecttype ot) {
        try {
            return Class.forName(FactModelGenerator.PAKKET + "." + Namen.ident(ot.naam, true), true, beanLoader);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("geen beanklasse voor objecttype " + ot.naam, e);
        }
    }

    private String id(String soort) { return soort + "_" + (idSeq++); }

    /** The rule {@code plan} plans, rendered into the executable model ({@link PatternDslWeergave#regel}), and the plan
     *  recorded ({@link #regelplannen}). */
    private Rule weergeef(Regelplan plan) {
        regelplannen.add(plan);
        return PatternDslWeergave.regel(plan, this::global);
    }

    /** The global of the model named {@code naam}: a parameter's, or the evaluation date's, each declared once. A
     *  parameter of the date's Java name beside a form that reads the date refuses the document ({@link #run}), so the
     *  name of a parameter is looked up first and the date's standing is not built then. */
    private Global<?> global(String naam) {
        Global<?> g = globals.containsKey(naam) ? globals.get(naam) : REKENDATUM.equals(naam) ? rekendatumGlobal : null;
        if (g == null) throw new IllegalStateException("geen global " + naam + " in het model");
        return g;
    }

    /** An indexed identity join under a constraint id of its own: {@link PatternDslWeergave#zelfde}, which also
     *  renders the joins of a plan. */
    private <T, U> PatternDSL.PatternDef<T> zelfde(PatternDSL.PatternDef<T> p, String soort, Variable<U> v,
                                                  Function1<T, Object> links, Function1<U, Object> rechts) {
        return PatternDslWeergave.zelfde(p, id(soort), v, links, rechts);
    }

    /** {@link #zelfde} where the fact's {@code veld} is the object {@code v} binds. */
    private <T, U> PatternDSL.PatternDef<T> zelfde(PatternDSL.PatternDef<T> p, String soort, Variable<U> v,
                                                  Function1<T, Object> veld) {
        return zelfde(p, soort, v, veld, o -> o);
    }
}
