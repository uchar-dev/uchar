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
import dev.uchar.regelspraak.ir.ResolvedStartpunt;
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
import dev.uchar.regelspraak.transpiler.kern.Dekking;
import dev.uchar.regelspraak.transpiler.kern.EffectieveEenheid;
import dev.uchar.regelspraak.transpiler.kern.Eenheidsconflict;
import dev.uchar.regelspraak.transpiler.kern.Gastheer;
import dev.uchar.regelspraak.transpiler.kern.Namen;
import dev.uchar.regelspraak.transpiler.kern.Operatoren;
import dev.uchar.regelspraak.transpiler.kern.StaandeDoelen;
import dev.uchar.regelspraak.transpiler.kern.Vensterpositie;

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
import org.drools.model.impl.ModelImpl;
import org.kie.api.runtime.KieSession;

import java.math.BigDecimal;
import java.math.BigInteger;
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
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

/**
 * The lowering of the truth-maintenance branch: a uchar IR document to a Drools executable model that derives with
 * {@code drools.logicalInsert}. A derived fact stands exactly as long as the condition of a rule that inserted it
 * holds, and goes, with everything that rested on it, when that condition stops holding. The outcome of an acyclic
 * rule set then does not depend on which rule the agenda fires first, which is what the exec-model branch secures
 * with measures of its own on top of destructive writes (a rule attribute against re-firing, a priority per stratum,
 * class reactivity, a conditional bean update, a record of an initialisation's own write). This branch carries none
 * of them; the replay's fire cap stays.
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
 * <p>What it lowers: a characteristic attribution; an assignment or a computation of an attribute of the subject that
 * is the only computing rule on a standing target (RUNTIME-SEMANTICS ch. 7.8, or beside one unconditional
 * initialisation, the desugaring domain of ch. 7.7); an initialisation (ch. 5.4), the only one on its target; a
 * consistency, whose violation is a logical §9.5 inconsistency; a prioritized-defaults group, as the effective
 * stack per concrete type of ch. 7.10, which decides by logical control facts ({@link Stuurfeit}) and into which an
 * ordinary rule on an ancestor's level folds, so that it does not fire on that type; and an object creation over a
 * {@code connectingRole} and a fact creation, each over a relation of two roles. Each always valid or gated on the
 * evaluation date by its validity window (RUNTIME-SEMANTICS ch. 8.2), with the §6.5 deposit of a division by leeg and
 * the one of a square root of a negative number where the evaluation reaches them (a default carries no division and no
 * root at all), whose condition, assertion or justification is built from characteristic checks (own or over a path,
 * negated or not), comparisons, leeg, gevuld, eleven and exact-digit tests, time windows, tests of the channel of a
 * consistency or of a rule that deposits, and {@code Compound}s of those under any quantifier, and whose value is a
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
 * bound values ({@link #gelijsteVouw}).
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

    private static final String NOG_NIET = "de truth-maintenance-tak draagt deze vorm nog niet";
    private static final String BUITEN = "buiten de proef van de truth-maintenance-tak";
    private static final String INCONSISTENTIE = FactModelGenerator.PAKKET + ".Inconsistentie";

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
    /** The targets on which a computed leeg writes nothing (RUNTIME-SEMANTICS ch. 7.7 and 7.8), classified as the
     *  other two branches classify them. */
    private StaandeDoelen staandeDoelen;
    private final List<Dekking> dekking = new ArrayList<>();
    private final List<Defaultsgroep> defaultsgroepen = new ArrayList<>();
    /** The members the lowered rules derive, by key. The key is normalised, and the setter a derivation is written back
     *  through is the bean property of the declared spelling, which the key no longer carries. */
    private final Map<ElementSleutel, Lid> afgeleid = new LinkedHashMap<>();
    /** The derived members a lowered rule reads, on its subject or at the end of a path: each gets its standing rules. */
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
    /** Whether a filter fault rule reads a {@link Lijstplaats} ({@link #plaats}). */
    private boolean lijstplaatsen;
    /** Unique per constraint across the model: the executable model shares a constraint between rules by its id, so
     *  two different tests under one id would collapse into the first. */
    private int idSeq;
    private Model model0;

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

    /** The names of the creation rules of the model, in the agenda groups {@link #FEITCREATIE} and
     *  {@link #OBJECTCREATIE}; empty for a document without a lowered creation, which the driver fires in one go. */
    public Set<String> creatieregels() { return Set.copyOf(creatieregels); }

    /** The evaluation strata of the creation rules of the model, ascending, as the reference interpreter computes them
     *  over the verifier's dependency graph (RUNTIME-SEMANTICS ch. 6.3): the driver runs the generations of each in
     *  turn, and a creation rule fires in the phases of its own stratum and of every stratum above it
     *  ({@link Creatiefase}). Empty where {@link #creatieregels} is. */
    public List<Integer> creatiestrata() { return List.copyOf(creatiestrata); }

    /** Whether a rule of the model reads a {@link Lijstplaats}: a fold whose filter can fault over a range with a
     *  to-many step. Only then does the case input state the places of its to-many lists. */
    public boolean leestLijstplaatsen() { return lijstplaatsen; }

    /**
     * The size of the lowering of one effective defaults stack, counted when it is built: {@code naam} as the manifest
     * and the control facts name it, whether it took the general form (exception groups, or more than one group at the
     * root) or the simple one, the number of Drools rules it consists of, and the kinds of control fact those rules
     * insert ({@link Stuurfeit}). The standing rules of the target are shared with every other reader and not counted.
     *
     * <p>{@code eigenaars} gives, in the general form, per member chain the place of the default whose exception group
     * it is; empty in the simple form, which has none. A member chain settles by its own rules whether that default is
     * displaced or not, so a measurement needs it to tell the {@link Uitkomst} facts the stack's outcome rests on from
     * those that stand under a displaced default.
     */
    public record Defaultsgroep(String naam, boolean algemeen, int regels, List<String> stuurfeiten,
                                Map<Integer, Plaats> eigenaars) {}

    /** The place of a default in the general form: its chain and its position there, as {@link Uitkomst} carries them. */
    public record Plaats(int keten, int positie) {}

    /** One entry per effective stack the branch lowered, in the order of the model. */
    public List<Defaultsgroep> defaultsgroepen() { return List.copyOf(defaultsgroepen); }

    /**
     * Writes every derivation of this model in {@code sessie} onto its bean: {@code setX(true)} for each
     * {@link HeeftKenmerk}, and the value of each {@link Waarde} through the setter of the declared attribute. The
     * engine is not told, so the session must not be fired again afterwards: a rule that reads the bean pattern would
     * see a value the network never propagated. Input on a characteristic stays, since nothing writes {@code false};
     * input on an attribute stays where no rule derived a value for it.
     *
     * <p>An evaluation fault that stands at the fixpoint ({@link Fout}) means the session has no answer, so before it
     * writes anything this throws the faults that stand: one as the exception it is, named after its rule as the
     * exec-model branch names it out of its fire, two or more as {@link Evaluatiefouten}, ordered by the name of the
     * form and then by the sentence, so that the report does not depend on the order of the rules or of the firings.
     *
     * <p>Two different values on one attribute of one object have no common fixpoint (RUNTIME-SEMANTICS ch. 5.7), and
     * this does not choose between them: it fails, naming both writers and both values. Equal values are no conflict.
     *
     * <p>A model with a creation is fired in generations ({@code DroolsBackend.vuur}), which leaves a
     * {@link Creatiefase} in the session. Without one the creation rules never had the focus and the session holds no
     * created object or link, so this refuses to write such a session back rather than hand out an answer without the
     * creations.
     */
    public void zetAfgeleideWaarden(KieSession sessie) {
        if (!creatieregels.isEmpty() && sessie.getObjects(o -> o instanceof Creatiefase).isEmpty())
            throw new IllegalStateException("dit model heeft creatieregels en vuurt in generaties (DroolsBackend.vuur);"
                    + " de sessie is zonder creatiefase gevuurd, dus zonder de creaties");
        List<Fout> fouten = new ArrayList<>();
        for (Object f : sessie.getObjects(o -> o instanceof Fout)) fouten.add((Fout) f);
        if (!fouten.isEmpty()) {
            fouten.sort(Comparator.comparing(Fout::regel).thenComparing(Fout::melding));
            if (fouten.size() == 1) throw fouten.get(0).fout();
            throw new Evaluatiefouten(fouten);
        }
        for (Object f : sessie.getObjects(o -> o instanceof HeeftKenmerk)) {
            HeeftKenmerk h = (HeeftKenmerk) f;
            Lid lid = lidVan(h, h.kenmerk());
            Accessors.setter(lid.klasse(), lid.naam()).accept(h.onderwerp(), Boolean.TRUE);
        }
        Map<Object, Map<ElementSleutel, List<Waarde>>> perDoel = new IdentityHashMap<>();
        for (Object f : sessie.getObjects(o -> o instanceof Waarde)) {
            Waarde w = (Waarde) f;
            perDoel.computeIfAbsent(w.onderwerp(), o -> new LinkedHashMap<>())
                    .computeIfAbsent(w.attribuut(), a -> new ArrayList<>()).add(w);
        }
        for (Map<ElementSleutel, List<Waarde>> perLid : perDoel.values())
            for (List<Waarde> waarden : perLid.values()) {
                waarden.sort(Comparator.comparing(Waarde::schrijver));
                Waarde eerste = waarden.get(0);
                for (Waarde w : waarden)
                    if (!gelijkeWaarde(eerste.waarde(), w.waarde())) throw new IllegalStateException(divergentie(eerste, w));
                Lid lid = lidVan(eerste, eerste.attribuut());
                Accessors.setter(lid.klasse(), lid.naam()).accept(eerste.onderwerp(), eerste.waarde());
            }
    }

    private Lid lidVan(Object feit, ElementSleutel sleutel) {
        Lid lid = afgeleid.get(sleutel);
        if (lid == null)
            throw new IllegalStateException(feit + " in de sessie, maar geen regel van dit model leidt "
                    + Feitnamen.lid(sleutel) + " af");
        return lid;
    }

    /** The sentence of RUNTIME-SEMANTICS ch. 5.7 for two writers of one attribute of one object. */
    private String divergentie(Waarde a, Waarde b) {
        Lid lid = afgeleid.get(a.attribuut());
        return "geen fixpunt: twee schrijvers van één doel geven verschillende waarden — de regel \"" + a.schrijver()
                + "\" schrijft " + a.waarde() + " en de regel \"" + b.schrijver() + "\" schrijft " + b.waarde()
                + " in het attribuut " + lid.naam() + " van een " + lid.declarant().naam + " ("
                + Feitnamen.object(a.onderwerp()) + "); divergentie is een fout (RUNTIME-SEMANTICS ch. 5.7)";
    }

    /** Value equality at the write boundary: numbers by value, so that 5.0 and 5.00 are one value. */
    private static boolean gelijkeWaarde(Object a, Object b) {
        if (a instanceof BigDecimal x && b instanceof BigDecimal y) return x.compareTo(y) == 0;
        return Objects.equals(a, b);
    }

    // ---------------- the model ----------------

    private Model run() {
        schrijvers = ProducentConsument.producentIndex(model, defaults);
        staandeDoelen = StaandeDoelen.van(model, defaults);
        if (!model.objectCreaties.isEmpty() || !model.feitCreaties.isEmpty())
            strata = Kringen.strata(model, defaults, navigatieBindingen, rolBindingen, bronPaden);
        // before any form lowers: a form that reads a created relation must know it while it lowers
        for (ResolvedObjectCreatie o : model.objectCreaties) {
            RolBindingRef vr = rolBindingen.get(o);
            if (vr != null && vr.feittype() != null) gecreeerdeRelaties.add(Namen.norm(vr.feittype()));
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
            weiger("verdeling", naamOf(v.naam()), "een Allocation: " + BUITEN);
        for (ResolvedObjectCreatie o : model.objectCreaties)
            regels.addAll(meet("objectcreatie", naamOf(o.naam()), () -> objectCreatie(o)));
        for (ResolvedFeitCreatie f : model.feitCreaties)
            regels.addAll(meet("feitcreatie", naamOf(f.naam()), () -> feitCreatie(f)));
        for (ResolvedUniciteitUnie u : model.uniciteitUnies)
            regels.addAll(meet("uniciteit-unie", naamOf(u.naam()), () -> uniciteitUnieRegels(u)));
        if (samenstelling != null) weiger("defaults", "effectieve stapels", samenstelling);
        for (Stapels.Stapel st : effectief) regels.addAll(meet("defaults", stapelnaam(st), () -> stapelregels(st)));
        for (ResolvedStartpunt sp : model.resolvedStartpunten)
            weiger("startpuntbepaling", sp.tijdlijn(), "een StartPointDetermination: tijdlijnen zijn " + BUITEN);
        for (ResolvedDagsoort d : model.resolvedDagsoorten)
            weiger("dagsoortdefinitie", d.naam(), "een DayKindDefinition: dagsoorten zijn " + BUITEN);
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
        for (Map.Entry<String, Global<?>> g : globals.entrySet()) standen.add(parameterstandregel(g.getKey(), g.getValue()));
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
                standen.add(rekendatumstandregel(rekendatumGlobal));
            }
        }
        globals.values().forEach(m::addGlobal);
        regels.forEach(m::addRule);
        standen.forEach(m::addRule);
        return m;
    }

    /** A form's name as the manifest shows it; a nameless form (WF-107 refuses it) still gets an entry. */
    private static String naamOf(String naam) {
        return naam != null ? naam : "(zonder naam)";
    }

    /** Lowers one form and records its verdict: lowered, or a skip with the reason a {@link Weigering} carries. */
    private List<Rule> meet(String soort, String naam, Supplier<List<Rule>> bouw) {
        vorm = naam;
        try {
            List<Rule> rules = bouw.get();
            dekking.add(Dekking.lowered(soort, naam));
            return rules;
        } catch (Weigering w) {
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
        Variable<Rekendatum> r = DSL.declarationOf(Rekendatum.class, "$rekendatum");
        return PatternDSL.rule(PKG, regelnaam + ": geen rekendatum").build(
                PatternDSL.pattern(r).expr(id("geen-rekendatum"), x -> x.datum() == null),
                DSL.on(r).execute((Block2<org.drools.model.Drools, Rekendatum>) (drools, x) -> drools.logicalInsert(
                        new Fout(x, vorm, new Rekenfout(Rekenfout.GELDIGHEID_ZONDER_DATUM).inDeRegel(regelnaam)))));
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
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Rule rekendatumstandregel(Global<LocalDate> g) {
        return PatternDSL.rule(PKG, "rekendatumstand").build(
                DSL.on((Global) g).execute((Block2<org.drools.model.Drools, Object>) (drools, v) ->
                        drools.logicalInsert(new Rekendatum((LocalDate) v))));
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
        Lid lid = lid(doel.objecttype(), doel.attribuut(), true);
        Lezingen lez = new Lezingen(doel.objecttype()).geldig(r.geldigheid());
        Set<Integer> gelezen = lez.volg();
        Toets toets = r.voorwaarde() == null ? null : voorwaarde(r.voorwaarde(), lez);
        boolean kanFalen = lez.kanFalen();
        lez.stop();
        List<RuleItemBuilder<?>> items = lez.linkerkant(kanFalen ? zonderFout(toets) : toets, gelezen, r.voorwaarde());
        ElementSleutel sleutel = lid.sleutel();
        items.add(DSL.on(lez.s).execute((Block2<org.drools.model.Drools, Object>) (drools, o) ->
                drools.logicalInsert(new HeeftKenmerk(o, sleutel))));
        afgeleid.put(sleutel, lid);
        lez.vastleggen();
        List<Rule> uit = new ArrayList<>();
        uit.add(PatternDSL.rule(PKG, model.geëmitteerdeRegelNaam(r.naam())).build(items.toArray(new RuleItemBuilder[0])));
        if (kanFalen)
            uit.add(voorwaardefoutregel(r.naam(), model.geëmitteerdeRegelNaam(r.naam()),
                    () -> new Lezingen(doel.objecttype()).geldig(r.geldigheid()), r.voorwaarde(), null));
        Rule deponering = deponeerregel(r.naam(), model.geëmitteerdeRegelNaam(r.naam()),
                () -> new Lezingen(doel.objecttype()).geldig(r.geldigheid()), r.voorwaarde(), null);
        if (deponering != null) uit.add(deponering);
        return uit;
    }

    /**
     * An assignment or a computation of an attribute of the subject: the left-hand side of a characteristic attribution,
     * and a consequence that computes the value from the values it binds and inserts a {@link Waarde} logically when the
     * value is gevuld. A computed leeg writes nothing, so the input on the target stands, or the initialisation beside the
     * rule fills it (the standing norm of RUNTIME-SEMANTICS ch. 7.8 and the desugaring domain of ch. 7.7: the rule is its
     * target's only computing writer, beside at most one unconditional initialisation, which this method demands). An
     * evaluation fault, a division by a numeric zero or a {@link Rekenfout}, is inserted logically by the consequence as
     * a {@link Fout} in place of the value, named after the rule. A division of a filled dividend by a leeg divisor, and
     * a square root of a negative number, insert the §9.5 inconsistency of RUNTIME-SEMANTICS ch. 1.2 from the same
     * consequence, under the rule's name, where the evaluation reaches them: the probe the consequence puts in its value
     * array hears exactly the places that run ({@link Sonde}), so a place in a branch of an {@code IfThenElse} that is
     * not taken, or in the value of a {@code Let} binder no read forces, deposits nothing (ch. 10.2, 10.6). Every
     * division and every root is such a place, as in the reference interpreter. The condition of the rule holds there by
     * construction; a deposit in the condition itself is the {@link #deponeerregel}'s.
     *
     * <p>That deposit is a logical fact like the value: it stands while the activation that made it stands, and goes when
     * a read it rested on changes. The reference interpreter keeps a deposit made on a transient value (RUNTIME-SEMANTICS
     * ch. 6.1, "Recorded residuals"), this branch withdraws it; recorded as an observation.
     */
    private List<Rule> rekenRegel(ResolvedRegel r, Expr.AttribuutRef doel) {
        Lid lid = lid(doel.objecttype(), doel.attribuut(), false);
        ElementSleutel sleutel = lid.sleutel();
        List<ResolvedRegel> schrijvend = rekenregels.get(sleutel);
        if (schrijvend != null && schrijvend.size() > 1)
            throw new Weigering("twee of meer rekenende regels op één doel ('" + lid.naam() + "' van " + lid.declarant().naam
                    + ": " + String.join(", ", schrijvend.stream().map(x -> "\"" + x.naam() + "\"").toList()) + "): hun"
                    + " v0-betekenis is de volgorde op de agenda (RUNTIME-SEMANTICS ch. 7.7, WF-079)");
        Set<Class<?>> geabsorbeerd = geabsorbeerd(doel, lid);
        // the consequence below writes nothing for a computed leeg, which is the semantics on a standing target only;
        // off that domain a computed leeg empties the target, and no fact of this branch says so
        if (!staandeDoelen.staandScalair(r))
            throw new Weigering("een rekenende regel op een doel buiten het staande domein ('" + lid.naam() + "' van "
                    + lid.declarant().naam + "), waar een berekende leeg het doel leeg maakt (RUNTIME-SEMANTICS ch."
                    + " 7.8): " + NOG_NIET);
        return schrijfregel(r, doel.objecttype(), lid, null, geabsorbeerd);
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
     * ended leeg without a message (the trial's A5, and the first case of {@code jutterswet-initialisatievolgorde} on
     * this branch), on several the session reached the fire cap (measured 2026-10-08,
     * {@code TmsInitialisatieWitnessTest}).
     *
     * <p>Two initialisations on one target are ill-formed, conditional or not (IR-SPEC WF-079): where both apply,
     * nothing ranks them. The backend runs no verifier, so this refuses the pair by name; the key is the declaring
     * type's, as for two computing rules, so two initialisations on sibling subtypes are refused as well, which
     * WF-079, read per concrete type, does not pair.
     *
     * <p>Beside another writer of its target an initialisation is the lowest default (ch. 7.7), and only the default
     * that decides is evaluated (ch. 7.1). This rule is evaluated as soon as what it reads stands, also where the other
     * writer displaces it later. A division by zero there is a fault on an intermediate state: this branch inserts it as
     * a {@link Fout}, which goes when the other writer displaces the initialisation, and answers the other writer's
     * value, where the reference interpreter faults or answers the value depending on the order of the rules. Whether
     * such a fault counts waits on a decision of the norm, so a division in the condition or the value of an
     * initialisation beside another writer stays refused, with the predicate of a division in a default
     * ({@code TmsInitialisatieWitnessTest}). Without another writer nothing displaces it and the division stays carried.
     */
    private List<Rule> initialisatieRegel(ResolvedRegel r, Expr.AttribuutRef doel) {
        Lid lid = lid(doel.objecttype(), doel.attribuut(), false);
        ElementSleutel sleutel = lid.sleutel();
        Set<Class<?>> geabsorbeerd = geabsorbeerd(doel, lid);
        List<ResolvedRegel> schrijvend = initialisaties.get(sleutel);
        if (schrijvend.size() > 1)
            throw new Weigering("twee of meer initialisaties op één doel ('" + lid.naam() + "' van " + lid.declarant().naam
                    + ": " + String.join(", ", schrijvend.stream().map(x -> "\"" + x.naam() + "\"").toList())
                    + "), voorwaardelijk of niet: waar ze allebei gelden rangschikt niets ze en bepaalt het fixpunt de"
                    + " waarde niet (RUNTIME-SEMANTICS ch. 5.4, WF-079)");
        Set<String> anderen = new LinkedHashSet<>(schrijvers.getOrDefault(sleutel, Set.of()));
        anderen.remove(ProducentConsument.basisNaam(r.naam()));
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
        Function<Object, Object> invoer = Accessors.getter(lid.klasse(), lid.naam());
        String naam = r.naam();
        return schrijfregel(r, doel.objecttype(), lid, lez -> {
            lez.onderwerp.expr(id("doel-leeg-in-de-invoer"), o -> invoer.apply(o) == null);
            Variable<Waarde> w = DSL.declarationOf(Waarde.class, "$andere_schrijver");
            return List.of(DSL.not(PatternDSL.pattern(w)
                    .expr(id("andere-schrijver"), x -> x.attribuut().equals(sleutel) && !x.schrijver().equals(naam))
                    .expr(id("andere-schrijver-onderwerp"), lez.s, (x, o) -> x.onderwerp() == o)));
        }, geabsorbeerd);
    }

    /**
     * The rule of a form that writes an attribute of its subject: the subject pattern with the reads its condition and
     * its value bind there, the standing patterns of what it reads elsewhere, one test over every value the condition
     * reads, the {@code blokkade} the form adds (null for none), and a consequence that computes the value from the
     * values it binds and inserts a {@link Waarde} logically, with the form's name as the writer, when the value is
     * gevuld, beside the §6.5 deposit {@link #rekenRegel} describes. Where the value faults, the consequence inserts the
     * {@link Fout} instead, and neither the value nor a deposit. Objects of a class in {@code geabsorbeerd} do not
     * match: an effective stack writes the target there ({@link #geabsorbeerd}). Beside it, where the condition can
     * fault, the rule that inserts its fault ({@link #voorwaardefoutregel}).
     */
    private List<Rule> schrijfregel(ResolvedRegel r, Objecttype subject, Lid lid,
                                    Function<Lezingen, List<RuleItemBuilder<?>>> blokkade, Set<Class<?>> geabsorbeerd) {
        ElementSleutel sleutel = lid.sleutel();
        Supplier<Lezingen> nieuw = () -> lezingen(subject, geabsorbeerd).geldig(r.geldigheid());
        Lezingen lez = nieuw.get();
        Set<Integer> gelezen = lez.volg();
        Toets toets = r.voorwaarde() == null ? null : voorwaarde(r.voorwaarde(), lez);
        boolean kanFalen = lez.kanFalen();
        lez.stop();
        Set<Integer> gebruik = lez.volg();
        // the write boundary: the value restated into the target's declared unit (RUNTIME-SEMANTICS ch. 2.4, 2.6)
        Waardefunctie waarde = herschaald(waarde(r.waarde(), lez), eff.eenheidVan(r.waarde()),
                lid.datatype().eenheidOf(), "een §4 schrijfgrens naar het doel");
        lez.stop();
        int sonde = lez.sondeplaats();
        List<RuleItemBuilder<?>> items = lez.linkerkant(kanFalen ? zonderFout(toets) : toets, gelezen, r.voorwaarde());
        if (blokkade != null) items.addAll(blokkade.apply(lez));
        String naam = r.naam();
        String regelnaam = model.geëmitteerdeRegelNaam(naam);
        int[] posities = posities(gebruik);
        int n = lez.variabelen.size();
        items.add(gevolg(lez, posities, (drools, onderwerp, w) -> {
            // the probe hears every §6.5 and §6.6 place the evaluation reaches, and only those (Sonde)
            Sonde s = sonde < 0 ? null : new Sonde();
            if (s != null) w[sonde] = s;
            Object v;
            try {
                v = collapse(waarde.van(w));
            } catch (RuntimeException e) {
                if (!Rekenfout.isEvaluatiefout(e)) throw e;
                drools.logicalInsert(new Fout(onderwerp, naam, Rekenfout.benoem(e, regelnaam)));
                return;
            }
            if (s != null && s.gemeld())
                drools.logicalInsert(RegelSpraakRuntime.newInconsistentie(INCONSISTENTIE, naam, onderwerp));
            if (v != null) drools.logicalInsert(new Waarde(onderwerp, sleutel, v, naam));
        }, n));
        afgeleid.put(sleutel, lid);
        lez.vastleggen();
        List<Rule> uit = new ArrayList<>();
        uit.add(PatternDSL.rule(PKG, regelnaam).build(items.toArray(new RuleItemBuilder[0])));
        if (kanFalen) uit.add(voorwaardefoutregel(naam, regelnaam, nieuw, r.voorwaarde(), blokkade));
        Rule deponering = deponeerregel(naam, regelnaam, nieuw, r.voorwaarde(), blokkade);
        if (deponering != null) uit.add(deponering);
        return uit;
    }

    /** The reads of a rule on {@code subject}, whose subject pattern leaves out the objects of a class in
     *  {@code geabsorbeerd}: an effective stack writes the target there ({@link #geabsorbeerd}). */
    private Lezingen lezingen(Objecttype subject, Set<Class<?>> geabsorbeerd) {
        Lezingen lez = new Lezingen(subject);
        if (!geabsorbeerd.isEmpty())
            lez.onderwerp.expr(id("niet-in-een-stapel"), o -> !geabsorbeerd.contains(o.getClass()));
        return lez;
    }

    // ---------------- an evaluation fault in a condition ----------------

    /**
     * The rule that inserts the fault of a condition that can fault, beside the rule {@code regelnaam} of the form
     * {@code vorm} that tests the condition. A condition is tested in the left-hand side, and an exception there leaves
     * the fire, where truth maintenance cannot withdraw it when the reads it rested on change; the form's own rule
     * therefore tests the condition through {@link #zonderFout}, which does not hold where it faults, and this rule,
     * whose left-hand side is that rule's own built again over fresh declarations ({@code nieuw} sets up the subject
     * pattern and what precedes it), with the same {@code blokkade} after the test and the test that the condition
     * faults, inserts the named {@link Fout} logically, on the subject of the form. It fires exactly where the form's
     * rule tests the condition and the test faults, and the fault goes when a read it rested on changes. The sentence
     * names the form's rule as the exec-model branch names a fault in a condition. A condition faults also where it
     * reaches a fold whose filter faulted for a member ({@link #vouw}); the filter itself inserts no fault
     * ({@link #filterfoutregel}).
     */
    private Rule voorwaardefoutregel(String vorm, String regelnaam, Supplier<Lezingen> nieuw, Conditie c,
                                     Function<Lezingen, List<RuleItemBuilder<?>>> blokkade) {
        Lezingen lez = nieuw.get();
        Set<Integer> gelezen = lez.volg();
        Toets toets = voorwaarde(c, lez);
        lez.stop();
        List<RuleItemBuilder<?>> items = lez.linkerkant(w -> fout(toets, w) != null, gelezen, c);
        if (blokkade != null) items.addAll(blokkade.apply(lez));
        String zinregel = model.geëmitteerdeRegelNaam(vorm);
        items.add(gevolg(lez, posities(gelezen), (drools, onderwerp, w) -> {
            RuntimeException f = fout(toets, w);
            if (f == null)
                throw new IllegalStateException("de voorwaarde van \"" + vorm + "\" faalde in de toets en niet in het"
                        + " gevolg, over dezelfde gebonden waarden");
            drools.logicalInsert(new Fout(onderwerp, vorm, Rekenfout.benoem(f, zinregel)));
        }, lez.variabelen.size()));
        lez.vastleggen();
        return PatternDSL.rule(PKG, regelnaam + ": fout in de voorwaarde").build(items.toArray(new RuleItemBuilder[0]));
    }

    /** {@code toets} where it answers, and not holding where it faults: the fault is the fault rule's
     *  ({@link #voorwaardefoutregel}). */
    private static Toets zonderFout(Toets toets) {
        return w -> {
            try {
                return toets.test(w);
            } catch (RuntimeException e) {
                if (!Rekenfout.isEvaluatiefout(e)) throw e;
                return false;
            }
        };
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
                               Function<Lezingen, List<RuleItemBuilder<?>>> blokkade) {
        if (!deponeert(c)) return null;
        Lezingen lez = nieuw.get();
        Set<Integer> gelezen = lez.volg();
        Toets toets = voorwaarde(c, lez);
        lez.stop();
        int sonde = lez.sondeplaats();
        if (sonde < 0) throw new IllegalStateException("de voorwaarde van \"" + vorm + "\" deelt, maar kreeg geen sonde");
        List<RuleItemBuilder<?>> items = lez.linkerkant(w -> {
            Sonde s = new Sonde();
            w[sonde] = s;
            try {
                toets.test(w);
            } catch (RuntimeException e) {
                // a fault is the fault rule's; what the evaluation deposited before it stays heard
                if (!Rekenfout.isEvaluatiefout(e)) throw e;
            }
            return s.gemeld();
        }, gelezen, c);
        if (blokkade != null) items.addAll(blokkade.apply(lez));
        items.add(DSL.on(lez.s).execute((Block2<org.drools.model.Drools, Object>) (drools, o) ->
                drools.logicalInsert(RegelSpraakRuntime.newInconsistentie(INCONSISTENTIE, vorm, o))));
        lez.vastleggen();
        return PatternDSL.rule(PKG, regelnaam + ": deponering in de voorwaarde").build(items.toArray(new RuleItemBuilder[0]));
    }

    /** The evaluation fault {@code toets} raises over {@code w}, or null where it answers. An evaluation fault is a
     *  division by a numeric zero (RUNTIME-SEMANTICS ch. 1.4, point 4) or a {@link Rekenfout}: a date step or a duration
     *  count outside the range of ch. 3.8, a context value or a validity window without an evaluation date, an
     *  {@code Unresolved} node reached, and the other faults that class names. Any other exception is a broken contract
     *  or a bug and leaves the fire as it is. */
    private static RuntimeException fout(Toets toets, Object[] w) {
        try {
            toets.test(w);
            return null;
        } catch (RuntimeException e) {
            if (!Rekenfout.isEvaluatiefout(e)) throw e;
            return e;
        }
    }

    // ---------------- a consistency and the §9.5 channel ----------------

    /**
     * A consistency (IR-SPEC ch. 3.5): the subject pattern with the reads of its assertion bound as a rule's are, one
     * test that holds where the assertion does not, and a consequence that inserts the subject's §9.5 inconsistency
     * logically, under the form's name. The violation is the negation of the assertion read with the empty operand of
     * RUNTIME-SEMANTICS ch. 1.2: the reference interpreter deposits where the assertion does not evaluate to true, and
     * the exec-model branch writes the same tables out per shape (an equality of numbers violated where exactly one is
     * leeg, an ordering violated where an operand is leeg), which the condition lowering of this branch carries. Every
     * read is bound to a total standing, so a subject has one activation and the channel one error fact per form and
     * subject, without the guard the exec-model branch needs. A violation found against a transient value goes when
     * that value changes, where the exec-model branch orders the consistency after the writers of what it reads and
     * never withdraws one; the fixpoint is the same. An assertion that faults is no violation: beside the rule, where the
     * assertion can fault, the rule that inserts its fault ({@link #voorwaardefoutregel}).
     */
    private List<Rule> consistentieRegels(ResolvedConsistentie c) {
        // the rule is named by the form, and the channel test reads the error facts by that name
        if (c.naam() == null) throw new Weigering("een consistentie zonder naam (WF-107)");
        if (c.subject() == null || c.assertie() == null) throw new Weigering("een consistentie zonder subject of assertie");
        Supplier<Lezingen> nieuw = () -> new Lezingen(c.subject()).geldig(c.geldigheid());
        Lezingen lez = nieuw.get();
        Set<Integer> gelezen = lez.volg();
        Toets houdt = voorwaarde(c.assertie(), lez);
        boolean kanFalen = lez.kanFalen();
        lez.stop();
        Toets geschonden = w -> !houdt.test(w);
        List<RuleItemBuilder<?>> items = lez.linkerkant(kanFalen ? zonderFout(geschonden) : geschonden, gelezen,
                c.assertie());
        String naam = c.naam();
        items.add(DSL.on(lez.s).execute((Block2<org.drools.model.Drools, Object>) (drools, o) ->
                drools.logicalInsert(RegelSpraakRuntime.newInconsistentie(INCONSISTENTIE, naam, o))));
        lez.vastleggen();
        List<Rule> uit = new ArrayList<>();
        uit.add(PatternDSL.rule(PKG, model.geëmitteerdeRegelNaam(naam)).build(items.toArray(new RuleItemBuilder[0])));
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
     * the fixpoint, where the reference interpreter evaluates the form (ch. 5.9). A key on a timeline, which reads
     * ever-uniqueness, is refused with the member ({@link #lid}), and so is a scope over a relation a form creates.
     */
    private List<Rule> uniciteitsregels(String naam, List<ResolvedUniciteitUnie.Bron> bronnen, RolBindingRef scopeRol,
                                        Geldigheid geldigheid) {
        // the rules are named by the form, and the channel test reads the error facts by that name
        if (naam == null) throw new Weigering("een Uniqueness zonder naam (WF-107)");
        String regelnaam = model.geëmitteerdeRegelNaam(naam);
        List<Rule> uit = new ArrayList<>();
        for (int b = 0; b < bronnen.size(); b++) uit.add(sleutelregel(naam, regelnaam, b, bronnen.get(b), scopeRol,
                geldigheid));
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
     *  component is compared as the declared name it is, up to normalisation (ch. 5.6). */
    private Rule sleutelregel(String naam, String regelnaam, int b, ResolvedUniciteitUnie.Bron bron, RolBindingRef scopeRol,
                              Geldigheid geldigheid) {
        Objecttype type = bron.type();
        Lezingen lez = new Lezingen(type).geldig(geldigheid);
        Set<Integer> gelezen = lez.volg();
        List<Integer> sleutels = new ArrayList<>();
        List<Boolean> enumeratie = new ArrayList<>();
        for (String sleutel : bron.sleutels()) {
            Lid lid = lid(type, sleutel, false);
            sleutels.add(lez.eigen(lid));
            enumeratie.add(lid.datatype() instanceof Datatype.Enumeratie);
        }
        int scope = scopeRol == null ? -1 : lez.scopelezing(scopeRol);
        lez.stop();
        Toets doetMee = w -> {
            for (int i : sleutels) if (w[i] == null) return false;
            return scope < 0 || w[scope] != null;
        };
        List<RuleItemBuilder<?>> items = lez.linkerkant(doetMee, gelezen, null);
        items.add(gevolg(lez, posities(gelezen), (drools, o, w) -> {
            List<Object> waarden = new ArrayList<>(sleutels.size());
            for (int i = 0; i < sleutels.size(); i++) {
                Object v = w[sleutels.get(i)];
                waarden.add(enumeratie.get(i) ? RegelSpraakRuntime.rsEnumNaam(v) : v);
            }
            drools.logicalInsert(new Uniciteitssleutel(naam, b, o, scope < 0 ? null : w[scope], waarden));
        }, lez.variabelen.size()));
        lez.vastleggen();
        return PatternDSL.rule(PKG, regelnaam + ": sleutel van " + type.naam + " " + (b + 1))
                .build(items.toArray(new RuleItemBuilder[0]));
    }

    /**
     * A test of the §9.5 channel, "regelversie X is inconsistent" (RUNTIME-SEMANTICS ch. 5.6): an existential over the
     * error facts of the form X, with no subject of its own, read through X's {@link KanaalStand}. The producers this
     * branch carries are a consistency, a uniqueness, and a rule whose condition or value can deposit a §6.5 or §6.6
     * inconsistency ({@link #schrijfregel}, {@link #deponeerregel}); any other name is refused, as the exec-model branch
     * refuses a name that produces nothing it lowers, since a test of a channel nothing fills would never hold and say
     * nothing. "is gevuurd" reads no channel; it is no construct of the core, and is refused with that reason.
     */
    private Toets kanaallezing(Conditie.Regelversie rv, Lezingen lez) {
        // decided as a predicate of the dialect over the fixpoint, never a construct of the core: the reference
        // interpreter faults on it ("not implemented") and the ASP backend does not translate it
        if (!rv.inconsistent())
            throw nietGelezen("Regelversie 'is gevuurd'", rv, "geen vorm van de kern maar een predicaat van het"
                    + " RegelSpraak-dialect over het fixpunt (RUNTIME-SEMANTICS ch. 5.6, CONSTRUCTS rij 30), dat de"
                    + " referentie-interpreter en de ASP-backend niet evalueren");
        String producent = rv.regel();
        if (!kanaalproducent(producent))
            throw new Weigering("een Regelversie 'is inconsistent' over \"" + producent + "\" in de voorwaarde ('"
                    + snippet(rv) + "'): de truth-maintenance-tak draagt als producent van het §9.5-kanaal alleen een"
                    + " Consistency, een Uniqueness en een regel met een deling of een wortel in haar voorwaarde of haar"
                    + " waarde (§6.5, §6.6)");
        int i = lez.kanaal(producent);
        return w -> Boolean.TRUE.equals(w[i]);
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
            throw new IllegalStateException("geen bonenklasse " + INCONSISTENTIE + " voor het kanaal van " + producent, e);
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

    // ---------------- object and fact creation ----------------

    /** One member initialisation of an object creation: the member on the created type, and its value on the subject. */
    private record Initialisatie(Lid lid, Waardefunctie waarde) {}

    /**
     * An object creation (RUNTIME-SEMANTICS ch. 5.5, IR-SPEC ch. 3.3), a rule of the agenda group {@link #OBJECTCREATIE}:
     * the subject pattern with the reads of the condition and of the member values bound as a rule's are, the
     * {@link Creatiefase} the driver opened, no {@link Gemaakt} of this creation for the subject, no {@link Gemaakt} that
     * makes the subject itself in this phase, and the condition as the test; and a consequence that makes a new bean of
     * the created type and inserts, stated, the bean, its {@link Gemaakt}, the {@link Koppeling} of the connecting
     * relation and the member initialisations as {@link Waarde} and {@link HeeftKenmerk} facts: a characteristic where
     * its value is true, an attribute where its value is gevuld, each computed from the values the rule binds when it
     * fires. The driver fires this group only at a fixpoint of the rule layer, so the condition and the values are those
     * of that fixpoint; and nothing the creation made lapses, as in the reference interpreter, which keeps a minted
     * instance under a fixed label. One object per creation per subject, then. The object is a bean of the created type,
     * so every pattern over that type sees it, and nothing is written on a bean that was there before: the link toward
     * it is the {@link Koppeling}, which the readers of the relation join over.
     *
     * <p>The 1:1 shape guards on the input world, as ch. 5.5 says: a subject whose reference field toward the created
     * type is filled in the case input creates nothing; this branch writes no bean while it fires, so the field is the
     * input. The decision of the link roles is the shared one ({@link FactModelGenerator#creatiekoppeling}). Refused by
     * name besides: an ambiguous back-link, which the exec-model branch faults at fire; a creation without a
     * {@code connectingRole} binding, whose relation this branch does not derive from the type pair; a connecting
     * relation that is not one of two roles; and a division in a member value, whose §6.5 deposit this branch does not
     * make. Where the condition can fault, its fault rule is an ordinary rule of the rule layer, without the marks and
     * without the 1:1 guard: the reference interpreter evaluates the condition first, in every pass.
     */
    private List<Rule> objectCreatie(ResolvedObjectCreatie oc) {
        if (oc.naam() == null) throw new Weigering("een objectcreatie zonder naam (WF-107)");
        Objecttype nieuw = oc.nieuwType(), subject = oc.subject();
        if (nieuw == null || subject == null) throw new Weigering("een objectcreatie zonder opgelost type");
        RolBindingRef vr = rolBindingen.get(oc);
        FactModelGenerator.Creatiekoppeling k = FactModelGenerator.creatiekoppeling(model, nieuw, subject, vr, oc.naam());
        if (k.dubbelzinnig() != null)
            throw new Weigering(k.dubbelzinnig() + "; de exec-modeltak faalt hier bij het vuren, deze tak weigert: " + NOG_NIET);
        if (k.weigering() != null) throw new Weigering(k.weigering());
        if (vr == null)
            throw new Weigering("een objectcreatie zonder connectingRole-binding, waarvan de terugkoppeling uit het typenpaar"
                    + " volgt: " + NOG_NIET);
        RegelSpraakModel.Feittype ft = feittype(vr.feittype());
        if (ft == null || ft.rollen.size() != 2 || ft.wederkerig)
            throw new Weigering("een objectcreatie over de relatie '" + vr.feittype() + "', die geen relatie van twee rollen"
                    + " is: " + NOG_NIET);
        String relatie = Namen.norm(ft.naam);
        boolean nieuwIsEerste = Namen.norm(ft.rollen.get(0).naam()).equals(Namen.norm(k.fwdRol()));
        String naam = oc.naam();
        String regelnaam = model.geëmitteerdeRegelNaam(naam);
        Supplier<Lezingen> nieuwe = () -> new Lezingen(subject).geldig(oc.geldigheid());
        Lezingen lez = nieuwe.get();
        Set<Integer> gelezen = lez.volg();
        Toets toets = oc.voorwaarde() == null ? null : voorwaarde(oc.voorwaarde(), lez);
        boolean kanFalen = lez.kanFalen();
        lez.stop();
        Set<Integer> gebruik = lez.volg();
        List<Initialisatie> inits = new ArrayList<>();
        for (ResolvedObjectCreatie.Toekenning t : oc.toekenningen()) {
            if (t.attribuut() == null || t.waarde() == null)
                throw new Weigering("een ledeninitialisatie zonder lid of waarde");
            String deling = deling(t.waarde());
            if (deling != null)
                throw new Weigering("een deling in een ledeninitialisatie ('" + deling + "'), waarvan deze tak de §6.5-"
                        + "deponering niet maakt: " + NOG_NIET);
            String wortel = wortel(t.waarde());
            if (wortel != null)
                throw new Weigering("een wortel in een ledeninitialisatie ('" + wortel + "'), waarvan deze tak de §6.6-"
                        + "deponering niet maakt: " + NOG_NIET);
            Lid lid = lid(nieuw, t.attribuut(), t.kenmerk());
            Waardefunctie w = waarde(t.waarde(), lez);
            if (!t.kenmerk())
                w = herschaald(w, eff.eenheidVan(t.waarde()), lid.datatype().eenheidOf(), "een §4 schrijfgrens naar het lid");
            inits.add(new Initialisatie(lid, w));
        }
        lez.stop();
        int stratum = stratum(oc, naam);
        int fase = lez.creatiefase(stratum);
        gebruik.add(fase);
        int herkomst = lez.herkomst();
        gebruik.add(herkomst);
        List<RuleItemBuilder<?>> items = lez.linkerkant(kanFalen ? zonderFout(toets) : toets, gelezen, oc.voorwaarde(),
                creatiebewaking(naam, lez, fase));
        // the fault this creation stated at a making for the subject blocks it there: the driver counts every firing of a
        // creation rule as a making, so a creation that fired again on its own fault would never let a stratum end
        Variable<Fout> eigenFout = DSL.declarationOf(Fout.class, "$eigen_fout");
        items.add(DSL.not(PatternDSL.pattern(eigenFout).expr(id("eigen-fout"), x -> x.regel().equals(naam))
                .expr(id("eigen-fout-onderwerp"), lez.s, (x, o) -> x.onderwerp() == o)));
        if (k.eenOpEen()) {
            // an instance linked toward the subject in the input world blocks the creation (ch. 5.5)
            Function<Object, Object> voorwaarts = Accessors.getter(bonenklasse(subject), k.fwdRef());
            lez.onderwerp.expr(id("geen-gekoppelde-instantie"), o -> voorwaarts.apply(o) == null);
        }
        Class<?> klasse = bonenklasse(nieuw);
        String subjecttype = subject.naam;
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
            drools.insert(object);
            drools.insert(new Gemaakt(naam, onderwerp, object, f, stratum, diepte));
            drools.insert(nieuwIsEerste ? new Koppeling(relatie, object, onderwerp, f)
                    : new Koppeling(relatie, onderwerp, object, f));
            for (int i = 0; i < inits.size(); i++) {
                Lid lid = inits.get(i).lid();
                Object v = waarden[i];
                if (lid.kenmerk()) {
                    if (Boolean.TRUE.equals(v)) drools.insert(new HeeftKenmerk(object, lid.sleutel()));
                } else if (v != null) {
                    drools.insert(new Waarde(object, lid.sleutel(), v, naam));
                }
            }
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
                DSL.not(PatternDSL.pattern(eigen).expr(id("gemaakt"), g -> g.regel().equals(naam))
                        .expr(id("gemaakt-onderwerp"), lez.s, (g, o) -> g.onderwerp() == o)),
                DSL.not(PatternDSL.pattern(nieuw).expr(id("nieuw-object"), lez.s, (g, o) -> g.object() == o)
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
            throw new IllegalStateException("tms: objectcreatie \"" + naam + "\" ontmoet een onderwerp met "
                    + herkomst.size() + " merktekens van een creatie, waar een object één keer gemaakt wordt: " + herkomst);
        int diepte = 1;
        if (!herkomst.isEmpty()) {
            Gemaakt g = (Gemaakt) herkomst.get(0);
            if (g.stratum() > stratum)
                throw new IllegalStateException("tms: objectcreatie \"" + naam + "\" van stratum " + stratum
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
            throw new IllegalStateException("tms: de creatie \"" + naam + "\" heeft geen stratum in de"
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
        Toets toets = ankerguard == null ? null : voorwaarde(ankerguard, lez);
        boolean kanFalen = lez.kanFalen();
        lez.stop();
        int stratum = stratum(fc, fc.naam());
        int fase = lez.creatiefase(stratum);
        List<RuleItemBuilder<?>> items = lez.linkerkant(kanFalen ? zonderFout(toets) : toets, gelezen, ankerguard);
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
        items.add(DSL.not(PatternDSL.pattern(bestaand).expr(id("bestaand"), x -> x.relatie().equals(relatie))
                .expr(id("bestaand-anker"), lez.s, (x, a) -> (ankerIsEerste ? x.eerste() : x.tweede()) == a)
                .expr(id("bestaand-doel"), doelvar, (x, d) -> (ankerIsEerste ? x.tweede() : x.eerste()) == d)));
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
            items.add(PatternDSL.pattern(st).expr(id("doelstand"), x -> x.kenmerk().equals(sleutel))
                    .expr(id("doelstand-doel"), doel, (x, d) -> x.onderwerp() == d)
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
                PatternDSL.pattern(twee).expr(id("tweede-vulling"), x -> x.relatie().equals(relatie))
                        .expr(id("tweede-vulling-nabij"), een, (x, y) -> nabijIsEerste
                                ? x.eerste() == y.eerste() && x.tweede() != y.tweede()
                                : x.tweede() == y.tweede() && x.eerste() != y.eerste()),
                DSL.on(nabij).execute((Block2<org.drools.model.Drools, Object>) (drools, n) ->
                        drools.logicalInsert(new Fout(n, ft.naam, new IllegalStateException(zin)))));
    }

    // ---------------- prioritized defaults ----------------

    /** The name of an effective stack, as the exec-model branch names the rule of the group: the kind-discriminated
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
     * the exec-model branch walks the stack in one consequence ({@code DefaultsRun}). One chain without exception groups
     * takes the simple form ({@link #eenvoudigeVorm}); exception groups, or more than one group at the root, the
     * general form ({@link #algemeneVorm}). A deciding default writes the target: a {@link Waarde} with the stack as the
     * writer, which replaces the input on an attribute, or a {@link HeeftKenmerk} for an attribution, beside which the
     * input on a characteristic stands; a conflict writes nothing and deposits the §9.5 inconsistency under its key.
     *
     * <p>Refused as the exec-model branch refuses them: a characteristic target that shares its name with an attribute,
     * a group with a dated validity, an initialisation default on a characteristic, a deciding default without a
     * justification; a target on a timeline or with dimensions through {@link #lid}. Refused besides: a division
     * anywhere in a default ({@link #deling(Conditie, Expr)}).
     */
    private List<Rule> stapelregels(Stapels.Stapel st) {
        PrioritizedDefaults l0 = st.leden().get(0);
        Objecttype eigenaar = st.eigenaar();
        boolean kenmerk = l0.isKenmerk();
        String doel = l0.doel();
        if (kenmerk && eigenaar.attribuut(doel) != null)
            throw new Weigering("een kenmerkdoel met de naam van een attribuut ('" + doel + "' van " + eigenaar.naam
                    + "): de boon draagt één veld per naam en het attribuut gaat voor, zodat het kenmerk niet te schrijven"
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
        Map<Integer, Plaats> eigenaars = new LinkedHashMap<>();
        for (List<Knoop> keten : wortel) eigenaars(keten, eigenaars);
        defaultsgroepen.add(new Defaultsgroep(b.naam, algemeen, regels.size(), List.copyOf(b.stuurfeiten),
                Map.copyOf(eigenaars)));
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

    /** Per member chain below {@code keten}, at any depth, the default whose exception group it is. */
    private static void eigenaars(List<Knoop> keten, Map<Integer, Plaats> uit) {
        for (Knoop k : keten) {
            if (!k.heeftLeden()) continue;
            for (int m = 0; m < k.leden().size(); m++) {
                uit.put(k.eersteLid() + m, new Plaats(k.keten(), k.positie()));
                eigenaars(k.leden().get(m), uit);
            }
        }
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
     *  ch. 7.7 reads as the lowest default ({@link #initialisatieRegel}). The exec-model branch refuses a division below
     *  the top of a consequence, whose deposit would persist; here the evaluation itself is speculative, as a default is
     *  evaluated whenever its reads stand. A division by zero in a default another one displaces is a fault on an
     *  intermediate state, which this branch would withdraw ({@link Fout}); whether such a fault counts waits on a
     *  decision of the norm, so every division is refused until then. Since none passes, the rules of a default raise
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
     * The simple form, variant M of the trial's A10: one rule per default that decides anything itself, with its reads
     * bound, its justification as the test and {@code not Beslist} over every higher index; the consequence computes
     * the value from what the rule bound and, where it decides, inserts the write and, where a lower default has a rule,
     * {@link Beslist}. A leeg consequence inserts nothing, so the lower defaults decide (ch. 7.1); the initialisation
     * default matches only where the target is empty on the bean, which holds the input (ch. 7.3). A do-not-attribute
     * decision with no attribution below it has nothing to displace and gets no rule.
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
     * {@code not Uitkomst} over the positions above it in its chain, so a lower default is displaced with whatever it
     * would settle, a conflict included (ch. 7.9). At the root the groups of the stack reconcile once more, by the same
     * rules, under the bare key, and that reconciliation writes the target or deposits the conflict.
     */
    private List<Rule> algemeneVorm(Stapelbouw b, List<List<Knoop>> wortel) {
        List<Rule> uit = new ArrayList<>();
        for (List<Knoop> keten : wortel) ketenregels(b, keten, uit);
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

    private void ketenregels(Stapelbouw b, List<Knoop> keten, List<Rule> uit) {
        String naam = b.naam;
        for (Knoop k : keten) {
            int id = k.keten(), i = k.positie();
            Function<Variable<Object>, List<RuleItemBuilder<?>>> nietHoger =
                    o -> i > 0 ? List.of(geenUitkomst(naam, id, i, o)) : List.of();
            Afloop beslist = (drools, o, v) ->
                    drools.logicalInsert(new Uitkomst(o, naam, id, i, Uitkomst.Soort.BESLIST, v, null));
            if (!k.heeftLeden()) {
                if (!beslistZelf(b, k.d())) continue;
                b.stuurfeiten.add("Uitkomst " + Uitkomst.Soort.BESLIST);
                uit.addAll(eigenRegel(b, k, naam + ": default " + k.label(), nietHoger, beslist));
                continue;
            }
            for (List<Knoop> lid : k.leden()) ketenregels(b, lid, uit);
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
     * member chains {@code [eerste, eerste + aantal)}, as {@code DefaultsRun.reconcile} reads them: the first conflict
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
        return PatternDSL.pattern(x).expr(id("uitkomst"), y -> y.stapel().equals(naam) && y.keten() >= van
                        && y.keten() < tot && (soort == null || y.soort() == soort))
                .expr(id("uitkomst-onderwerp"), onderwerp, (y, oo) -> y.onderwerp() == oo);
    }

    /** {@code not Uitkomst} over the positions above {@code positie} in chain {@code keten}. */
    private RuleItemBuilder<?> geenUitkomst(String naam, int keten, int positie, Variable<Object> onderwerp) {
        Variable<Uitkomst> x = DSL.declarationOf(Uitkomst.class, "$hoger_" + idSeq);
        return DSL.not(PatternDSL.pattern(x).expr(id("hoger-uitkomst"), y -> y.stapel().equals(naam)
                        && y.keten() == keten && y.positie() < positie)
                .expr(id("hoger-uitkomst-onderwerp"), onderwerp, (y, oo) -> y.onderwerp() == oo));
    }

    /** {@code not Beslist} over the indices above {@code index} in the stack. */
    private RuleItemBuilder<?> geenBeslist(String naam, int index, Variable<Object> onderwerp) {
        Variable<Beslist> x = DSL.declarationOf(Beslist.class, "$hoger_" + idSeq);
        return DSL.not(PatternDSL.pattern(x).expr(id("hoger-beslist"), y -> y.stapel().equals(naam) && y.index() < index)
                .expr(id("hoger-beslist-onderwerp"), onderwerp, (y, oo) -> y.onderwerp() == oo));
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
     * displaces loses its match, and the fault it raised goes with it, so a fault counts only where its default is
     * evaluated at the fixpoint (PLAN §9, 2026-10-09).
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
        Toets toets = d.voorwaarde() == null ? null : voorwaarde(d.voorwaarde(), lez);
        boolean kanFalen = lez.kanFalen();
        lez.stop();
        Toets getoetst = kanFalen ? zonderFout(toets) : toets;
        String naam = b.naam;
        List<RuleItemBuilder<?>> items;
        if (b.kenmerk) {
            items = lez.linkerkant(getoetst, gelezen, d.voorwaarde(), wacht.apply(lez.s));
            Boolean beslissing = d.besluit();
            items.add(DSL.on(lez.s).execute((Block2<org.drools.model.Drools, Object>) (drools, o) ->
                    besluit.voer(drools, o, beslissing)));
        } else {
            Set<Integer> gebruik = lez.volg();
            Waardefunctie waarde = herschaald(waarde(d.consequentie(), lez), eff.eenheidVan(d.consequentie()),
                    b.lid.datatype().eenheidOf(), "een §4 schrijfgrens naar het doel");
            lez.stop();
            items = lez.linkerkant(getoetst, gelezen, d.voorwaarde(), wacht.apply(lez.s));
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
            uit.add(voorwaardefoutregel(naam, regelnaam, nieuw, d.voorwaarde(), l -> wacht.apply(l.s)));
        return uit;
    }

    /** §6.1.2: a value at rest is a decimal, so an exact rational that survived the computation collapses here, as at
     *  the exec-model branch's write boundary. */
    private static Object collapse(Object v) {
        return v instanceof Breuk b ? b.naarDecimal() : v;
    }

    // ---------------- values ----------------

    /** A value as a function of the values the rule binds, indexed as {@link Lezingen} numbers them. */
    @FunctionalInterface
    private interface Waardefunctie {
        Object van(Object[] w);
    }

    /** Every expression node is named here, so a node the IR records gain later fails to compile until it is decided
     *  whether this branch computes it. */
    private Waardefunctie waarde(Expr e, Lezingen lez) {
        return switch (e) {
            case Expr.Literal l -> literaal(l);
            case Expr.AttribuutRef a -> { int i = attribuutlezing(a, lez); yield w -> w[i]; }
            case Expr.ParameterRef p -> { int i = lez.parameter(p.parameter()); yield w -> w[i]; }
            case Expr.Binair b -> binair(b, lez);
            case Expr.TekstReeks t -> tekstreeks(t, lez);
            case Expr.Functie f -> functie(f, lez);
            // IR-SPEC ch. 4.3: a union denotes instances and stands only as the argument of a count, which reads it in
            // vouw; here it stands where a value is read, which the verifier refuses (WF-110 (a)), the reference
            // interpreter faults on and the other two branches refuse in this sentence
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
                yield w -> { throw new Rekenfout(Rekenfout.ONOPGELOST); };
            }
        };
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

    /** A literal as the exec-model branch reads it: a number, and a percentage, a number in the unit %, exact where its
     *  quotient does not terminate, a date, a boolean, and a text or an enumeration label as its string. */
    private static Waardefunctie literaal(Expr.Literal l) {
        Datatype t = l.type();
        if (t instanceof Datatype.Numeriek || t instanceof Datatype.Percentage) {
            BigDecimal[] r = Rationaal.van(l.waarde());
            if (r == null) throw new Weigering("een getal dat niet te lezen is ('" + l.waarde() + "')");
            Breuk b = Breuk.van(r[0]).delen(Breuk.van(r[1]));
            Object v = b.isTerminerend() ? b.naarDecimal() : b;
            return w -> v;
        }
        if (t instanceof Datatype.Booleaan) {
            Boolean v = Literalen.waar(l.waarde());
            return w -> v;
        }
        if (t instanceof Datatype.DatumInDagen) {
            LocalDate v = Literalen.parseDatum(l.waarde());
            if (v == null) throw new Weigering("een datum die niet te lezen is ('" + l.waarde() + "')");
            return w -> v;
        }
        if (t instanceof Datatype.DatumTijd) {
            LocalDateTime v = Literalen.parseDatumTijd(l.waarde());
            if (v == null) throw new Weigering("een datum-tijd die niet te lezen is ('" + l.waarde() + "')");
            return w -> v;
        }
        String label = Literalen.tekstOfEnumLabel(l);
        if (label == null) throw new Weigering("een Literal van type " + t + ": " + NOG_NIET);
        return w -> label;
    }

    /** The arithmetic operators on numbers, a percentage being a number in the unit % (RUNTIME-SEMANTICS ch. 2.2, 2.6),
     *  through the runtime's exact helpers and with their leeg tables (ch. 1.2); a date plus or minus a duration through
     *  {@link #datumrekening}, a power through {@link #macht}. A sum or a difference restates its right operand into the
     *  left one's unit (ch. 2.6); a product, a quotient and a percentage of a value do not. A division faults on a numeric
     *  zero (ch. 1.4, point 4), and one of a filled dividend by a leeg divisor deposits where the evaluation reaches it and
     *  its operands qualify ({@link #deponeerplek}, {@link Sonde}). The operands are evaluated left before right
     *  (ch. 5.11). */
    private Waardefunctie binair(Expr.Binair b, Lezingen lez) {
        Datatype lt = eff.typeVan(b.links()), rt = eff.typeVan(b.rechts());
        if (lt.isDatum() || rt.isDatum()) return datumrekening(b, lez);
        Expr.BinOp op = b.op();
        if (op == Expr.BinOp.TOT_DE_MACHT) return macht(b, lez);
        boolean deling = op == Expr.BinOp.GEDEELD_DOOR || op == Expr.BinOp.GEDEELD_DOOR_ABS;
        // a condition that holds a division carries the rule that inserts its fault (voorwaardefoutregel)
        if (deling) lez.markeerFalen();
        Waardefunctie l = waarde(b.links(), lez), rechts = waarde(b.rechts(), lez);
        Waardefunctie r = op == Expr.BinOp.PLUS || op == Expr.BinOp.MIN || op == Expr.BinOp.VERMINDERD_MET
                ? herschaald(rechts, eff.eenheidVan(b.rechts()), eff.eenheidVan(b.links()), "een additieve operandgrens")
                : rechts;
        int sonde = deling && deponeerplek(b) ? lez.sonde() : -1;
        return switch (op) {
            case PLUS -> w -> RegelSpraakRuntime.rsPlus(l.van(w), r.van(w));
            case MIN -> w -> RegelSpraakRuntime.rsMin(l.van(w), r.van(w));
            case VERMINDERD_MET -> w -> RegelSpraakRuntime.rsVerminderd(l.van(w), r.van(w));
            case MAAL -> w -> RegelSpraakRuntime.rsMaal(l.van(w), r.van(w));
            case PERCENTAGE_VAN -> w -> RegelSpraakRuntime.rsPercentageVan(l.van(w), r.van(w));
            case GEDEELD_DOOR -> w -> {
                Object lv = l.van(w), rv = r.van(w);
                if (lv != null && rv == null) Sonde.meld(w, sonde);
                return RegelSpraakRuntime.rsGedeeld(lv, rv);
            };
            case GEDEELD_DOOR_ABS -> w -> {
                Object lv = l.van(w), rv = r.van(w);
                if (lv != null && rv == null) Sonde.meld(w, sonde);
                return RegelSpraakRuntime.rsGedeeldAbs(lv, rv);
            };
            case TOT_DE_MACHT -> throw new IllegalStateException("een macht gaat door macht()");
        };
    }

    /** A power with a whole exponent of at least 0 written as a literal, exact (RUNTIME-SEMANTICS ch. 2.2), a leeg base
     *  leeg (ch. 1.2), as the exec-model branch computes that shape ({@code rsMacht}). Any other exponent is refused: a
     *  negative or fractional literal, or a value read while the session fires, reaches the real power, which ch. 2.1
     *  leaves OPEN, where the reference interpreter answers an exact root or faults and the exec-model branch computes a
     *  binary double. */
    private Waardefunctie macht(Expr.Binair b, Lezingen lez) {
        Integer n = b.rechts() instanceof Expr.Literal lit && lit.type().isGetalachtig() ? heleExponent(lit.waarde()) : null;
        if (n == null)
            throw new Weigering("een macht met een exponent die geen geheel getal van ten minste 0 als literal is ('"
                    + snippet(b.bron()) + "'): de reële macht is OPEN (RUNTIME-SEMANTICS ch. 2.1), waar de"
                    + " referentie-interpreter een exacte wortel geeft of faalt en de exec-modeltak een binaire double rekent");
        Waardefunctie basis = waarde(b.links(), lez);
        int exponent = n;
        return w -> RegelSpraakRuntime.rsMacht(basis.van(w), exponent);
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

    /** A date plus or minus a duration with a calendar or clock unit, as the exec-model branch computes it
     *  ({@code datumRekenWaarde}): no date minus date, no duration minus a date, a clock unit only on a date-time. A
     *  duration counted in a day kind is refused: day kinds are outside the proef. A step or a count outside the range of
     *  RUNTIME-SEMANTICS ch. 3.8 is an evaluation fault ({@link #datumfout}); the date is evaluated before the duration
     *  (ch. 5.11). */
    private Waardefunctie datumrekening(Expr.Binair b, Lezingen lez) {
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
        if (Tijdrekening.dagsoortVoorEenheid(model, eff.typeVan(duurExpr).eenheidOf()) != null)
            throw new Weigering("een duur in dagsoorten ('" + snippet(b.bron()) + "'): dagsoorten zijn " + BUITEN);
        String eenheid = Tijdrekening.chronoUnit(eff.typeVan(duurExpr));
        if (eenheid == null) throw new Weigering("een duur zonder tijdseenheid ('" + snippet(b.bron()) + "')");
        boolean klok = switch (eenheid) { case "HOURS", "MINUTES", "SECONDS", "MILLIS" -> true; default -> false; };
        if (klok && !(eff.typeVan(datumExpr) instanceof Datatype.DatumTijd))
            throw new Weigering("een klokeenheid bij een datum zonder tijd ('" + snippet(b.bron()) + "')");
        lez.markeerFalen();
        Waardefunctie d = waarde(datumExpr, lez), n = waarde(duurExpr, lez);
        return w -> {
            java.time.temporal.Temporal datum = (java.time.temporal.Temporal) d.van(w);
            Object aantal = n.van(w);
            try {
                return RegelSpraakRuntime.rsDatumReken(datum, aantal, eenheid, optellen);
            } catch (IllegalStateException e) {
                throw datumfout(e);
            }
        };
    }

    /** The fault of a date step the runtime raised as {@code e}, as an evaluation fault in its own sentence, which is
     *  the reference interpreter's: a step that leaves the representable range of a datum-tijd, a duration count outside
     *  that range (RUNTIME-SEMANTICS ch. 3.8), and a step on a date beyond the years the runtime's dates hold. Any other
     *  {@code IllegalStateException} out of the date step is no evaluation fault and is thrown as it is. */
    private static RuntimeException datumfout(IllegalStateException e) {
        String zin = String.valueOf(e.getMessage());
        boolean bereik = zin.equals(RegelSpraakRuntime.DATUMTIJD_BUITEN_BEREIK)
                || zin.equals(RegelSpraakRuntime.DATUM_BUITEN_JAREN)
                || (zin.startsWith("duration count ") && zin.endsWith(" is out of range"));
        return bereik ? new Rekenfout(zin, e) : e;
    }

    /** {@code f} restated from unit {@code van} into unit {@code naar} (RUNTIME-SEMANTICS ch. 2.6): times the exact
     *  canonical factors {@code num ÷ den} as rationals where both are known, so a restatement that does not terminate
     *  stays exact until a collapse point (ch. 2.1), as the reference interpreter restates; times the single ratio of the
     *  two units otherwise, a rate whose factors are not both known; unchanged where no restatement is due, a unitless
     *  side adopting the other's unit. A leeg value stays leeg and a value that is not a number passes through. Two units
     *  that do not convert fault while the model is built ({@link Eenheidsconflict}), as at the exec-model branch. */
    private static Waardefunctie herschaald(Waardefunctie f, Eenheid van, Eenheid naar, String plek) {
        Eenheidsconflict.eisHerschaalbaar(van, naar, plek);
        if (van == null || naar == null || !van.convertibleNaar(naar) || !van.vereistOmrekening(naar)) return f;
        BigDecimal[] exact = van.exacteHerschaalFactoren(naar);
        if (exact != null) {
            BigDecimal num = exact[0], den = exact[1];
            return w -> {
                Object v = f.van(w);
                return v instanceof BigDecimal || v instanceof Breuk
                        ? RegelSpraakRuntime.rsGedeeld(RegelSpraakRuntime.rsMaal(v, num), den) : v;
            };
        }
        BigDecimal factor = van.omrekenfactorNaar(naar);
        return w -> {
            Object v = f.van(w);
            return v instanceof BigDecimal || v instanceof Breuk ? RegelSpraakRuntime.rsMaal(v, factor) : v;
        };
    }

    // ---------------- the forms of a value beside the arithmetic ----------------

    /** A rounding (RUNTIME-SEMANTICS ch. 2.5) of its base, a leeg base leeg (ch. 1.2), through {@code rsAfronding}, which
     *  rounds an exact rational on its true value; a square root as the base is the root under its mandatory rounding
     *  ({@link #wortelwaarde}). */
    private Waardefunctie afronding(Expr.Afgerond a, Lezingen lez) {
        java.math.RoundingMode modus = afrondModus(a.modus());
        int decimalen = a.decimalen();
        Waardefunctie basis = a.basis() instanceof Expr.Functie f && f.kind() == Expr.FunctieKind.WORTEL
                ? wortelwaarde(f, lez) : waarde(a.basis(), lez);
        return w -> RegelSpraakRuntime.rsAfronding(basis.van(w), decimalen, modus);
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

    /** A square root under its rounding (§13.4.17.13), as the exec-model branch computes it: the runtime's root at 34
     *  digits ({@code rsWortel}), which the rounding above it collapses; a leeg radicand leeg, and a negative one leeg
     *  with the §6.6 deposit where the radicand qualifies and the evaluation reaches it (RUNTIME-SEMANTICS ch. 1.2,
     *  {@link Sonde}). */
    private Waardefunctie wortelwaarde(Expr.Functie f, Lezingen lez) {
        if (f.argumenten().size() != 1)
            throw new Weigering("een squareRoot met " + f.argumenten().size() + " argumenten ('" + snippet(f.bron())
                    + "'): de wortel heeft er één (WF-037)");
        Waardefunctie radicand = waarde(f.argumenten().get(0), lez);
        int sonde = deponeerplek(f) ? lez.sonde() : -1;
        return w -> {
            Object x = radicand.van(w);
            BigDecimal getal = RegelSpraakRuntime.toBd(x);
            if (getal != null && getal.signum() < 0) Sonde.meld(w, sonde);
            return RegelSpraakRuntime.rsWortel(x);
        };
    }

    /** A clamp (RUNTIME-SEMANTICS ch. 2.7): its base, then its minimum, then its maximum, in the order of ch. 5.11 and
     *  of the bounds' application, each bound restated into the base's unit (ch. 2.6); a leeg base leeg, an absent or
     *  leeg bound no bound ({@code rsBegrens}). */
    private Waardefunctie begrenzing(Expr.Begrensd g, Lezingen lez) {
        Waardefunctie basis = waarde(g.basis(), lez);
        Eenheid eenheid = eff.eenheidVan(g.basis());
        Waardefunctie minimum = g.minimum() == null ? null
                : herschaald(waarde(g.minimum(), lez), eff.eenheidVan(g.minimum()), eenheid, "een begrenzing (minimum)");
        Waardefunctie maximum = g.maximum() == null ? null
                : herschaald(waarde(g.maximum(), lez), eff.eenheidVan(g.maximum()), eenheid, "een begrenzing (maximum)");
        return w -> {
            Object x = basis.van(w);
            Object laag = minimum == null ? null : minimum.van(w);
            Object hoog = maximum == null ? null : maximum.van(w);
            return RegelSpraakRuntime.rsBegrens(x, laag, hoog);
        };
    }

    /** A text composition (IR-SPEC ch. 4.2): the literal chunks and the inserted parts in part order (RUNTIME-SEMANTICS
     *  ch. 5.11), each as its text ({@link #tekstdeel}). */
    private Waardefunctie tekstreeks(Expr.TekstReeks t, Lezingen lez) {
        List<Waardefunctie> delen = new ArrayList<>();
        for (Expr.TekstReeks.Deel d : t.delen()) {
            switch (d) {
                case Expr.TekstReeks.Letterlijk l -> {
                    String tekst = l.tekst() == null ? "" : l.tekst();
                    delen.add(w -> tekst);
                }
                case Expr.TekstReeks.Ingevoegd i -> delen.add(tekstdeel(i.expr(), lez, "TextComposition"));
            }
        }
        return w -> {
            StringBuilder uit = new StringBuilder();
            for (Waardefunctie deel : delen) uit.append((String) deel.van(w));
            return uit.toString();
        };
    }

    /** An inserted part of a text composition or an argument of a concatenation, as its text: a text or an enumeration
     *  label is its own text, and a leeg part reads "" (RUNTIME-SEMANTICS ch. 1.2). A part of another type is refused by
     *  name: the spec states the leeg rule and nothing more, and for the text of a number, a truth value or a date the
     *  reference interpreter (5,50 as "5.5", waar as "waar") and the runtime's {@code rsTekst} (5.50, true) differ. */
    private Waardefunctie tekstdeel(Expr e, Lezingen lez, String waar) {
        Datatype t = eff.typeVan(e);
        if (!(t instanceof Datatype.Tekst) && !(t instanceof Datatype.Enumeratie))
            throw new Weigering("een tekstdeel van type " + typenaam(t) + " in een " + waar + " ('" + snippet(e.bron())
                    + "'): RUNTIME-SEMANTICS ch. 1.2 legt alleen vast dat een leeg deel \"\" leest; de tekst van een getal,"
                    + " een waarheidswaarde of een datum beslist de spec niet, en de referentie-interpreter en de"
                    + " runtimehulp rsTekst geven er een verschillende");
        Waardefunctie f = waarde(e, lez);
        return w -> {
            Object v = f.van(w);
            return v == null ? "" : v.toString();
        };
    }

    /** A cell of a dimensioned attribute (§3.6): the value at its one label in the attribute's {@code Map<label, value>}
     *  bean member, leeg where the map or the label is absent, as the exec-model branch reads it. The attribute is
     *  input, read off the subject's own bean ({@link #dimensielezing}); a cell over several dimensions is refused by
     *  name, as that branch refuses it. The reference interpreter and the ASP backend do not read a cell. */
    private Waardefunctie dimensiecel(Expr.DimensieCel c, Lezingen lez) {
        if (c.labels() == null || c.labels().size() != 1)
            throw new Weigering("een DimensionCellRead over " + (c.labels() == null ? 0 : c.labels().size())
                    + " dimensies ('" + snippet(c.bron()) + "'): de exec-modeltak en deze tak lezen alleen een cel van"
                    + " één dimensie");
        int i = dimensielezing(c.attribuut(), lez, "een DimensionCellRead");
        String label = c.labels().get(0);
        return w -> w[i] instanceof Map<?, ?> m ? m.get(label) : null;
    }

    /** A sum, minimum or maximum over a dimension selection (§5.8.5): the attribute's cells at the selected labels, a
     *  leeg cell skipped and an all-leeg selection leeg, as the exec-model branch folds them ({@code rsDimSom},
     *  {@code rsDimMin}, {@code rsDimMax}); a sum's {@code emptyFallback} is evaluated after the cells, every time
     *  (RUNTIME-SEMANTICS ch. 5.11), and stands in for an all-leeg sum. The reference interpreter and the ASP backend do
     *  not fold a dimension selection. */
    private Waardefunctie dimensievouw(Expr.Functie f, Lezingen lez) {
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
        Waardefunctie terugval = f.leegwaarde() == null ? null : herschaald(waarde(f.leegwaarde(), lez),
                eff.eenheidVan(f.leegwaarde()), eff.eenheidVan(a), "een §5.8.2 terugvalwaarde");
        return w -> {
            Map<?, ?> m = w[i] instanceof Map<?, ?> x ? x : null;
            BigDecimal r = switch (kind) {
                case SOM -> RegelSpraakRuntime.rsDimSom(m, labels);
                case MINIMUM -> RegelSpraakRuntime.rsDimMin(m, labels);
                default -> RegelSpraakRuntime.rsDimMax(m, labels);
            };
            Object t = terugval == null ? null : terugval.van(w);
            return kind == Expr.FunctieKind.SOM && r == null ? t : r;
        };
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
     * pattern binds ({@link Lezingen#binder}): the {@code Let} hands its body a copy of the array with the need in that
     * slot, so the lambda keeps no state and one KieBase serves any number of sessions. Every read the value and the body
     * make is bound in the left-hand side all the same, read or not: truth maintenance follows each value the evaluation
     * may need, and a read through a total standing never keeps the rule from matching.
     */
    private Waardefunctie let(Expr.Let l, Lezingen lez) {
        if (l.binder() == null) throw new Weigering("een Let zonder binder ('" + snippet(l.bron()) + "')");
        Waardefunctie waarde = waarde(l.value(), lez);
        int slot = lez.binder();
        String sleutel = Namen.norm(l.binder());
        Integer buiten = lez.binders.put(sleutel, slot);
        Waardefunctie lichaam;
        try {
            lichaam = waarde(l.body(), lez);
        } finally {
            if (buiten == null) lez.binders.remove(sleutel);
            else lez.binders.put(sleutel, buiten);
        }
        return w -> {
            Barriere barriere = new Barriere();
            Object[] binnen = w.clone();
            binnen[slot] = new Behoefte(() -> waarde.van(w), barriere);
            try {
                return lichaam.van(binnen);
            } catch (Barriere b) {
                if (b != barriere) throw b;
                return null;
            }
        };
    }

    /** A read of the binder of the nearest enclosing {@code Let} of that name (WF-064): it forces the need and raises
     *  the {@code Let}'s barrier where the value is leeg ({@link Behoefte#lees}). */
    private Waardefunctie binderlezing(Expr.BinderRef b, Lezingen lez) {
        Integer slot = b.naam() == null ? null : lez.binders.get(Namen.norm(b.naam()));
        if (slot == null)
            throw new Weigering("een BinderRef '" + b.naam() + "' zonder omsluitende Let die hem bindt ('"
                    + snippet(b.bron()) + "', WF-064)");
        int i = slot;
        return w -> ((Behoefte) w[i]).lees();
    }

    /** An {@code IfThenElse} (RUNTIME-SEMANTICS ch. 10.2): the condition evaluated as any condition of this branch, then
     *  exactly the branch it chooses; the other branch evaluates nothing, so it reads no binder, faults nowhere and
     *  deposits nothing. The reads of both branches are bound in the left-hand side all the same. */
    private Waardefunctie keuze(Expr.IfThenElse k, Lezingen lez) {
        Toets voorwaarde = voorwaarde(k.conditie(), lez);
        Waardefunctie dan = waarde(k.dan(), lez), anders = waarde(k.anders(), lez);
        return w -> voorwaarde.test(w) ? dan.van(w) : anders.van(w);
    }

    /** A context value (RUNTIME-SEMANTICS ch. 8.2): evaluationDate, the session's evaluation date, and evaluationYear,
     *  its calendar year as a whole number without a unit ({@code rsRekenjaar}). Both read the session's
     *  {@link Rekendatum}, bound in the left-hand side, and fault where the session carries no date, in the reference
     *  interpreter's sentences. The bound day of a day-kind definition, read outside such a definition, faults as in that
     *  executor, since it denotes nothing there; a day-kind definition itself is outside this branch. */
    private Waardefunctie contextwaarde(Expr.Constante c, Lezingen lez) {
        String naam = c.naam() == null ? "" : c.naam();
        switch (naam) {
            case "Rekendatum" -> {
                int i = lez.rekendatum();
                lez.markeerFalen();
                return w -> {
                    if (w[i] == null) throw new Rekenfout(Rekenfout.REKENDATUM_ZONDER_DATUM);
                    return w[i];
                };
            }
            case "Rekenjaar" -> {
                int i = lez.rekendatum();
                lez.markeerFalen();
                return w -> {
                    if (w[i] == null) throw new Rekenfout(RegelSpraakRuntime.REKENJAAR_ZONDER_REKENDATUM);
                    return RegelSpraakRuntime.rsRekenjaar((LocalDate) w[i]);
                };
            }
            case "de dag" -> {
                lez.markeerFalen();
                return w -> { throw new Rekenfout(Rekenfout.DE_DAG_BUITEN_DAGSOORT); };
            }
            default -> throw new Weigering("de contextwaarde '" + naam + "' ('" + snippet(c.bron()) + "'): de lader laat"
                    + " alleen evaluationDate, evaluationYear en de gebonden dag toe (WF-098)");
        }
    }

    /** The barrier of one evaluation of a {@code Let} (RUNTIME-SEMANTICS ch. 10.6, clause 2): raised by a read of its
     *  binder whose value is leeg, and caught by that evaluation alone, since every evaluation makes its own, so a
     *  nested {@code Let} lets an enclosing one's barrier pass. A signal, not a diagnosis: no stack trace. */
    private static final class Barriere extends RuntimeException {
        private static final long serialVersionUID = 1L;

        Barriere() {
            super(null, null, false, false);
        }
    }

    /** The need of a binder in one evaluation of its {@code Let} (RUNTIME-SEMANTICS ch. 10.6, clause 1): the value,
     *  evaluated at the first read and kept for every further one; a leeg value raises the {@code Let}'s barrier at each
     *  read. Made inside that evaluation and reached only through its copy of the value array, so it is never shared
     *  between sessions or threads. */
    private static final class Behoefte {
        private final Supplier<Object> waarde;
        private final Barriere barriere;
        private boolean berekend;
        private Object uitkomst;

        Behoefte(Supplier<Object> waarde, Barriere barriere) {
            this.waarde = waarde;
            this.barriere = barriere;
        }

        Object lees() {
            if (!berekend) {
                uitkomst = waarde.get();
                berekend = true;
            }
            if (uitkomst == null) throw barriere;
            return uitkomst;
        }
    }

    /**
     * The probe of the §6.5 and §6.6 deposit (RUNTIME-SEMANTICS ch. 1.2): a division of a filled dividend by a leeg
     * divisor, and a square root of a negative number, deposit a §9.5 inconsistency where the evaluation reaches them.
     * Which places run depends on the evaluation itself — the branch an {@code IfThenElse} takes, the binders a body reads
     * (ch. 10.2, 10.6) — so a static test of the operands cannot tell. The consequence of a computing rule or an
     * initialisation puts a fresh probe in its slot of the value array ({@link Lezingen#sonde}) before it computes the
     * value, every place ({@link #deponeerplek}) reports to it where it runs, and the consequence deposits where one did.
     * A condition gets its probe in the test of its deposit rule ({@link #deponeerregel}), and a fold's filter in the
     * test of its filter deposit rule ({@link #filterdeponeerregel}), whose {@link Filterdeponering} the evaluation of
     * the fold in the reading rule reports to that rule's probe ({@link #gelezenVouw}); elsewhere the slot is empty and
     * a place reports to nothing.
     */
    private static final class Sonde {
        private boolean gemeld;

        /** A place that qualifies ran and deposits; {@code plaats} is its probe's slot, -1 where the place does not
         *  qualify. */
        static void meld(Object[] w, int plaats) {
            if (plaats >= 0 && w[plaats] instanceof Sonde s) s.gemeld = true;
        }

        boolean gemeld() {
            return gemeld;
        }
    }

    // ---------------- aggregations ----------------

    /** A fold this branch lowers: the key its contributions carry ({@link Bijdrage#vouw}), its contribution rules (one
     *  per range, beside the filter fault rule of a filter that can fault and the filter deposit rule of a filter that
     *  can deposit), the reads of those rules, registered when a form that reads the fold lowers
     *  ({@link Lezingen#vastleggen}), whether its filter can fault, in which case the rule that reads it binds its
     *  {@link Filterfout} facts as well, and whether its filter can deposit, in which case that rule counts its
     *  {@link Filterdeponering} facts; for a quantifier fold, whether its matrix can fault and whether it can deposit,
     *  in which case its contributions carry a {@link Lidtoets} ({@link #kwantorvouw}). */
    private record Vouw(String sleutel, List<Rule> regels, List<Lezingen> lezingen, boolean kanFalen, boolean deponeert,
                        boolean lidKanFalen, boolean lidDeponeert) {
        /** A fold over a set source or a union: no matrix per member. */
        Vouw(String sleutel, List<Rule> regels, List<Lezingen> lezingen, boolean kanFalen, boolean deponeert) {
            this(sleutel, regels, lezingen, kanFalen, deponeert, false, false);
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
    private Waardefunctie functie(Expr.Functie f, Lezingen lez) {
        if (f.dimensieSelectie() != null) return dimensievouw(f, lez);
        if (!isVouw(f.kind())) return builtin(f, lez);
        return isGelisteVouw(f) ? gelijsteVouw(f, lez) : vouw(f, lez);
    }

    /**
     * A builtin call (IR-SPEC ch. 4.3), its arguments evaluated in wire order (RUNTIME-SEMANTICS ch. 5.11), through the
     * runtime's helpers where the exec-model branch uses them: the absolute value ({@code rsAbs}); a concatenation of
     * texts ({@link #tekstdeel}); the duration and the absolute duration in a calendar unit ({@code rsTijdsduur},
     * ch. 3.8); the year, month and day of a date and the hour, minute, second and millisecond of a date-time
     * ({@code rsDatumDeel}); easterSunday of a whole year ({@link #paasdag}); a date and a date-time made of their parts
     * ({@link #datumconstructie}). A square root is computed as the base of its mandatory rounding ({@link #afronding}),
     * and without one exactly ({@link #exacteWortel}). A bucketing builtin over a timeline and a duration in day kinds
     * belong to the card of timelines and day kinds, and are refused by name.
     */
    private Waardefunctie builtin(Expr.Functie f, Lezingen lez) {
        Expr.FunctieKind kind = f.kind();
        String spelling = Spelling.BUILTIN_KIND.spelling(kind);
        if (kind == Expr.FunctieKind.TIJDSEVENREDIG_DEEL || kind == Expr.FunctieKind.AANTAL_DAGEN_IN)
            throw nietBerekend("BuiltinCall " + spelling, f.bron(), "tijdlijnen zijn " + BUITEN);
        if (f.filter() != null)
            throw new Weigering("een BuiltinCall " + spelling + " met een filter ('" + snippet(f.bron()) + "'): alleen een"
                    + " bucketing over een tijdlijn draagt er een");
        List<Expr> a = f.argumenten();
        return switch (kind) {
            case WORTEL -> exacteWortel(f, lez);
            case ABSOLUTE_WAARDE -> {
                Waardefunctie x = waarde(eenArgument(f, spelling), lez);
                yield w -> RegelSpraakRuntime.rsAbs(x.van(w));
            }
            case CONCATENATIE -> {
                List<Waardefunctie> delen = new ArrayList<>();
                for (Expr arg : a) delen.add(tekstdeel(arg, lez, "concatenation"));
                yield w -> {
                    StringBuilder uit = new StringBuilder();
                    for (Waardefunctie deel : delen) uit.append((String) deel.van(w));
                    return uit.toString();
                };
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
     *  {@link Sonde}). The exec-model branch computes a binary approximation here instead. */
    private Waardefunctie exacteWortel(Expr.Functie f, Lezingen lez) {
        if (f.argumenten().size() != 1)
            throw new Weigering("een squareRoot met " + f.argumenten().size() + " argumenten ('" + snippet(f.bron())
                    + "'): de wortel heeft er één (WF-037)");
        lez.markeerFalen();
        Waardefunctie radicand = waarde(f.argumenten().get(0), lez);
        int sonde = deponeerplek(f) ? lez.sonde() : -1;
        return w -> {
            Breuk q = Breuk.van(radicand.van(w));
            if (q == null) return null;
            if (q.signum() < 0) {
                Sonde.meld(w, sonde);
                return null;
            }
            BigInteger teller = q.teller().sqrt(), noemer = q.noemer().sqrt();
            if (!teller.multiply(teller).equals(q.teller()) || !noemer.multiply(noemer).equals(q.noemer()))
                // the reference interpreter writes the radicand as a rational here (Q.to_string), not as a decimal
                throw new Rekenfout("irrational square root of " + (q.noemer().equals(BigInteger.ONE) ? q.teller()
                        : q.teller() + "/" + q.noemer()) + " reached without an explicit rounding (§13.4.17.13 mandates"
                        + " one)");
            Breuk r = Breuk.van(teller, noemer);
            return r.isTerminerend() ? r.naarDecimal() : r;
        };
    }

    /** The one argument of {@code f}; a call with another number is refused (WF-037). */
    private static Expr eenArgument(Expr.Functie f, String spelling) {
        if (f.argumenten().size() != 1)
            throw new Weigering("een " + spelling + " met " + f.argumenten().size() + " argumenten ('" + snippet(f.bron())
                    + "'): deze builtin heeft er één (WF-037)");
        return f.argumenten().get(0);
    }

    /** The duration and the absolute duration from argument 0 to argument 1 in whole units of the node's own unit
     *  (RUNTIME-SEMANTICS ch. 3.8, WF-038), the "van" evaluated before the "tot" (ch. 5.11); a leeg operand leeg. */
    private Waardefunctie tijdsduur(Expr.Functie f, Lezingen lez, String spelling) {
        List<Expr> a = f.argumenten();
        if (a.size() != 2)
            throw new Weigering("een " + spelling + " met " + a.size() + " argumenten ('" + snippet(f.bron()) + "'): een"
                    + " tijdsduur heeft er twee (WF-037)");
        Datatype van = eff.typeVan(a.get(0)), tot = eff.typeVan(a.get(1));
        if (!van.isDatum() || !tot.isDatum() || !zelfdeDatumsoort(van, tot))
            throw new Weigering("een " + spelling + " die niet twee datums van één precisie meet ('" + snippet(f.bron())
                    + "', WF-038)");
        Datatype type = eff.typeVan(f);
        if (Tijdrekening.dagsoortVoorEenheid(model, type.eenheidOf()) != null)
            throw new Weigering("een " + spelling + " in dagsoorten ('" + snippet(f.bron()) + "'): dagsoorten zijn " + BUITEN);
        String eenheid = Tijdrekening.chronoUnit(type);
        if (eenheid == null)
            throw new Weigering("een " + spelling + " zonder tijdseenheid ('" + snippet(f.bron()) + "', WF-038)");
        boolean klok = switch (eenheid) { case "HOURS", "MINUTES", "SECONDS", "MILLIS" -> true; default -> false; };
        if (klok && !(van instanceof Datatype.DatumTijd))
            throw new Weigering("een " + spelling + " in een klokeenheid tussen datums zonder tijd ('" + snippet(f.bron())
                    + "', WF-038)");
        Waardefunctie vanaf = waarde(a.get(0), lez), totaan = waarde(a.get(1), lez);
        boolean absoluut = f.kind() == Expr.FunctieKind.ABSOLUTE_TIJDSDUUR;
        return w -> {
            Object v = vanaf.van(w), t = totaan.van(w);
            return RegelSpraakRuntime.rsTijdsduur((java.time.temporal.Temporal) v, (java.time.temporal.Temporal) t,
                    eenheid, absoluut);
        };
    }

    /** A part of a date or a date-time, as a whole number without a unit; a leeg date leeg (RUNTIME-SEMANTICS ch. 1.2,
     *  3.8). A clock part demands a date-time, as the exec-model branch demands it. */
    private Waardefunctie datumdeel(Expr.Functie f, Lezingen lez, String spelling, ChronoField veld, boolean klok) {
        Expr arg = eenArgument(f, spelling);
        Datatype t = eff.typeVan(arg);
        if (!t.isDatum() || (klok && !(t instanceof Datatype.DatumTijd)))
            throw new Weigering("een " + spelling + " uit iets anders dan een " + (klok ? "datum-tijd" : "datum") + " ('"
                    + snippet(f.bron()) + "')");
        Waardefunctie d = waarde(arg, lez);
        return w -> RegelSpraakRuntime.rsDatumDeel(d.van(w), veld);
    }

    /** easterSunday of a whole year without a unit (RUNTIME-SEMANTICS ch. 3.8, WF-038), by the algorithm of that chapter
     *  ({@code rsEerstePaasdag}); a leeg year leeg. A year that is not whole is a fault in the reference interpreter's
     *  sentence, never a silent truncation, and so is a year outside the years this backend's dates hold. */
    private Waardefunctie paasdag(Expr.Functie f, Lezingen lez, String spelling) {
        Expr arg = eenArgument(f, spelling);
        Eenheid eenheid = eff.eenheidVan(arg);
        if (eenheid != null)
            throw new Weigering("een " + spelling + " van een jaar met de eenheid " + eenheid.weergave() + " ('"
                    + snippet(f.bron()) + "'): het jaar is een geheel getal zonder eenheid (WF-038)");
        lez.markeerFalen();
        Waardefunctie jaar = waarde(arg, lez);
        return w -> {
            Object v = jaar.van(w);
            if (v == null) return null;
            Breuk q = Breuk.van(v);
            if (!q.noemer().equals(BigInteger.ONE))
                throw new Rekenfout("easterSunday: expected a whole unitless year, got " + getaltekst(q));
            if (q.teller().abs().compareTo(BigInteger.valueOf(999_999_999)) > 0)
                throw new Rekenfout("easterSunday: the year " + q.teller() + " lies outside the years -999999999 to"
                        + " 999999999 that this backend's dates hold");
            return RegelSpraakRuntime.rsEerstePaasdag(new BigDecimal(q.teller()));
        };
    }

    /** A date of three parts or a date-time of seven, as the exec-model branch makes them ({@code rsDatumMet},
     *  {@code rsDatumtijdMet}): every part evaluated in order (RUNTIME-SEMANTICS ch. 5.11), any leeg part leeg (ch. 1.2).
     *  A part that is not a whole number within the range of a date part, or parts that name no day, fault: the
     *  reference interpreter makes no such date, and the runtime would truncate the part or throw past the fire. */
    private Waardefunctie datumconstructie(Expr.Functie f, Lezingen lez, String spelling) {
        boolean tijd = f.kind() == Expr.FunctieKind.DATUMTIJD_MET;
        int aantal = tijd ? 7 : 3;
        if (f.argumenten().size() != aantal)
            throw new Weigering("een " + spelling + " met " + f.argumenten().size() + " argumenten ('" + snippet(f.bron())
                    + "'): deze builtin heeft er " + aantal + " (WF-037)");
        lez.markeerFalen();
        List<Waardefunctie> delen = new ArrayList<>();
        for (Expr arg : f.argumenten()) delen.add(waarde(arg, lez));
        return w -> {
            Object[] v = new Object[aantal];
            for (int i = 0; i < aantal; i++) v[i] = delen.get(i).van(w);
            for (Object deel : v) if (deel == null) return null;
            for (Object deel : v) {
                Breuk q = Breuk.van(deel);
                if (!q.noemer().equals(BigInteger.ONE) || q.teller().bitLength() > 31)
                    throw new Rekenfout(spelling + ": the part " + getaltekst(q) + " is no whole number within the range"
                            + " of a date part");
            }
            try {
                return tijd ? RegelSpraakRuntime.rsDatumtijdMet(v[0], v[1], v[2], v[3], v[4], v[5], v[6])
                        : RegelSpraakRuntime.rsDatumMet(v[0], v[1], v[2]);
            } catch (java.time.DateTimeException e) {
                throw new Rekenfout(spelling + ": " + e.getMessage(), e);
            }
        };
    }

    /** A number as the reference interpreter writes it in a sentence: a terminating one as its plain decimal, another as
     *  numerator/denominator. */
    private static String getaltekst(Breuk q) {
        return q.isTerminerend() ? q.naarDecimal().toPlainString() : q.teller() + "/" + q.noemer();
    }

    /**
     * A listed fold (RUNTIME-SEMANTICS ch. 1.2e): minimum and eerste keep the least operand value, maximum and laatste
     * the greatest; a leeg operand is skipped, an all-leeg fold is leeg, and a tie takes the later operand. The operands
     * are computed from the values the rule binds, in wire order, each after the first restated into the first one's
     * unit, the unit of the result (§4.6 Eenheid1, RUNTIME-SEMANTICS ch. 2.6).
     */
    private Waardefunctie gelijsteVouw(Expr.Functie f, Lezingen lez) {
        boolean kleinste = f.kind() == Expr.FunctieKind.MINIMUM || f.kind() == Expr.FunctieKind.EERSTE;
        Eenheid eerste = eff.eenheidVan(f.argumenten().get(0));
        List<Waardefunctie> operanden = new ArrayList<>();
        for (int i = 0; i < f.argumenten().size(); i++) {
            Expr a = f.argumenten().get(i);
            Waardefunctie op = waarde(a, lez);
            operanden.add(i == 0 ? op : herschaald(op, eff.eenheidVan(a), eerste, "een §5.8.3 operand van een gelijste vouw"));
        }
        return w -> {
            Object acc = null;
            for (Waardefunctie op : operanden) {
                Object x = op.van(w);
                if (x == null) continue;
                if (acc == null) { acc = x; continue; }
                int c = vergelijk(acc, x);
                acc = kleinste ? (c < 0 ? acc : x) : (c > 0 ? acc : x);
            }
            return acc;
        };
    }

    /** The order of two values a fold compares: numbers exactly, a rational among them included; dates as they order. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static int vergelijk(Object a, Object b) {
        if (a instanceof Number x && b instanceof Number y) return Breuk.van(x).compareTo(Breuk.van(y));
        return ((Comparable) a).compareTo(b);
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
     * interpreter evaluates after the members ({@link #filterfout}). The fault is then this rule's, as a division in its
     * own value or condition is: inserted by its consequence or by its fault rule ({@link #voorwaardefoutregel}), and
     * only where its evaluation reaches the fold. A filter that deposits for a member is a {@link Filterdeponering} of
     * the member in the same way: the reference interpreter evaluates the filter with this rule's collector, so the
     * deposit is this rule's, on its subject, where its evaluation reaches the fold ({@link #gelezenVouw}).
     *
     * <p>Refused by name: a total without a range, which reads the subject's own timeline, and a fold over a member on
     * a timeline; the timelines are of a later card.
     */
    private Waardefunctie vouw(Expr.Functie f, Lezingen lez) {
        Expr.FunctieKind kind = f.kind();
        if (kind == Expr.FunctieKind.AANTAL && f.argumenten().size() == 1
                && f.argumenten().get(0) instanceof Expr.InstanceSetUnion u) {
            Vouw v = unievouw(f, u, lez.subject);
            int[] i = lez.vouw(v, null);
            return gelezenVouw(v, i, lez, w -> aantal(w[i[0]]));
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
        if (kind == Expr.FunctieKind.TOTAAL && !metBereik)
            throw nietBerekend("Aggregation total over de eigen tijdlijn", f.bron(), "tijdlijnen zijn " + BUITEN);
        if (arg != null && Namen.bevatNaam(elem.granulariteitLeden(), arg.attribuut()))
            throw nietBerekend("Aggregation " + kind + " over het tijdsafhankelijke attribuut '" + arg.attribuut() + "'",
                    f.bron(), "tijdlijnen zijn " + BUITEN);
        if (!metBereik)
            throw new Weigering("een Aggregation " + kind + " zonder bereik ('" + snippet(f.bron()) + "'): IR-SPEC ch. 4.3"
                    + " eist een sourceRole of sourcePath");
        Expr.AttribuutRef lid = arg;
        Vouw v = vouwVoor(f, lez.subject, (sleutel, naam) -> {
            List<Rule> regels = new ArrayList<>();
            List<Lezingen> lezingen = new ArrayList<>();
            boolean kanFalen = bijdrageregels(sleutel, naam, kind, lez.subject, elem, bereik(f, lez.subject, elem), 0,
                    f.filter(), lid, regels, lezingen);
            return new Vouw(sleutel, List.copyOf(regels), List.copyOf(lezingen), kanFalen, deponeert(f.filter()));
        });
        Class<?> functie = switch (kind) {
            case SOM, TOTAAL -> accumulatiefunctie("BigDecimalSumAccumulateFunction");
            case MINIMUM, EERSTE -> accumulatiefunctie("MinAccumulateFunction");
            case MAXIMUM, LAATSTE -> accumulatiefunctie("MaxAccumulateFunction");
            default -> null;
        };
        int[] i = lez.vouw(v, functie);
        // the fallback restated into the unit of the folded member (RUNTIME-SEMANTICS ch. 2.6)
        Waardefunctie terugval = f.leegwaarde() == null ? null : herschaald(waarde(f.leegwaarde(), lez),
                eff.eenheidVan(f.leegwaarde()), arg == null ? null : eff.eenheidVan(arg), "een §5.8.2 terugvalwaarde");
        Waardefunctie uitkomst = switch (kind) {
            case AANTAL, AANTAL_GEVULDE -> w -> aantal(w[i[0]]);
            case SOM -> w -> {
                Object t = terugval == null ? null : terugval.van(w);
                return leeg(w[i[0]]) ? t : w[i[1]];
            };
            case TOTAAL -> w -> leeg(w[i[0]]) ? BigDecimal.ZERO : w[i[1]];
            default -> w -> leeg(w[i[0]]) ? null : w[i[1]];
        };
        return gelezenVouw(v, i, lez, uitkomst);
    }

    /**
     * The evaluation of the fold {@code v} in the rule that reads it, bound at {@code i} ({@link Lezingen#vouw}): where
     * its filter deposited for a member, the deposit is heard by this rule's probe ({@link Sonde}), since the reference
     * interpreter evaluates the filter with the reading form's collector, only where that form's evaluation reaches the
     * fold (RUNTIME-SEMANTICS ch. 5.10, 5.11); then, where its filter faulted for a member, the fault of the first in
     * fold order ({@link #filterfout}); then {@code uitkomst}, the fold's value, whose fallback the reference interpreter
     * evaluates after the members. A deposit for a member after the one that faults, which the reference interpreter
     * does not reach, is heard as well: the session faults at the fixpoint, so it changes no outcome.
     */
    private static Waardefunctie gelezenVouw(Vouw v, int[] i, Lezingen lez, Waardefunctie uitkomst) {
        if (v.kanFalen()) lez.markeerFalen();
        int sonde = v.deponeert() ? lez.sonde() : -1;
        if (!v.kanFalen() && sonde < 0) return uitkomst;
        return w -> {
            if (sonde >= 0 && ((Number) w[i[3]]).longValue() > 0) Sonde.meld(w, sonde);
            if (v.kanFalen()) filterfout(w[i[2]]);
            return uitkomst.van(w);
        };
    }

    /** The evaluation of a fold whose filter faulted for a member: {@code eerste} is the first {@link Filterfout} in
     *  fold order that the reading rule bound on its subject, or null where none stands. */
    private static void filterfout(Object eerste) {
        if (eerste != null) throw ((Filterfout) eerste).fout();
    }

    private static BigDecimal aantal(Object telling) {
        return BigDecimal.valueOf(((Number) telling).longValue());
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

    /** The fold of {@code f}, an aggregation or a quantifier fold, on {@code subject}, built by {@code bouw} from its key
     *  and its name the first time it is read. */
    private Vouw vouwVoor(Object f, Objecttype subject, BiFunction<String, String, Vouw> bouw) {
        Map<Objecttype, Vouw> perOnderwerp = vouwen.computeIfAbsent(f, k -> new IdentityHashMap<>());
        Vouw v = perOnderwerp.get(subject);
        if (v == null) {
            String naam = "vouw " + vouwteller++;
            v = bouw.apply(vorm + ": " + naam, naam);
            perOnderwerp.put(subject, v);
        }
        return v;
    }

    /** The count over an instance-set union (RUNTIME-SEMANTICS ch. 3.4d): a contribution rule per selection, each with
     *  its own relation and its own filter, all under one key; a member two selections select gives two equal
     *  contributions, which merge into one fact, so the count is that of the union. */
    private Vouw unievouw(Expr.Functie f, Expr.InstanceSetUnion u, Objecttype subject) {
        return vouwVoor(f, subject, (sleutel, naam) -> {
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
        Toets toets = filter == null ? null : voorwaarde(filter, lez);
        boolean kanFalen = lez.kanFalen();
        lez.stop();
        Set<Integer> gebruik = lez.volg();
        int lid = lez.lid();
        int waarde = arg == null ? -1 : lez.eigen(lid(elem, arg.attribuut(), false));
        lez.stop();
        List<RuleItemBuilder<?>> items = lez.linkerkant(kanFalen ? zonderFout(toets) : toets, gelezen, filter);
        boolean telling = kind == Expr.FunctieKind.AANTAL, gevuld = kind == Expr.FunctieKind.AANTAL_GEVULDE;
        items.add(gevolg(lez, lez.drager, posities(gebruik), (drools, onderwerp, w) -> {
            if (telling) {
                drools.logicalInsert(new Bijdrage(sleutel, onderwerp, w[lid], null));
                return;
            }
            Object v = w[waarde];
            if (v != null) drools.logicalInsert(new Bijdrage(sleutel, onderwerp, w[lid], gevuld ? null : collapse(v)));
        }, lez.variabelen.size()));
        String vormnaam = model.geëmitteerdeRegelNaam(vorm);
        uit.add(PatternDSL.rule(PKG, vormnaam + ": bijdrage aan " + naam).build(items.toArray(new RuleItemBuilder[0])));
        lezingen.add(lez);
        if (kanFalen) uit.add(filterfoutregel(sleutel, vormnaam + ": filterfout in " + naam, subject, elem, bereik,
                selectie, filter, lezingen::add));
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
        Toets toets = voorwaarde(filter, lez);
        lez.stop();
        int sonde = lez.sondeplaats();
        if (sonde < 0) throw new IllegalStateException("het filter van " + sleutel + " deponeert, maar kreeg geen sonde");
        Set<Integer> gebruik = lez.volg();
        int lid = lez.lid();
        lez.stop();
        List<RuleItemBuilder<?>> items = lez.linkerkant(w -> {
            Sonde s = new Sonde();
            w[sonde] = s;
            try {
                toets.test(w);
            } catch (RuntimeException e) {
                if (!Rekenfout.isEvaluatiefout(e)) throw e;
            }
            return s.gemeld();
        }, gelezen, filter);
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
        Toets toets = voorwaarde(filter, lez);
        lez.stop();
        Set<Integer> gebruik = lez.volg();
        int lid = lez.lid();
        lez.stop();
        gebruik.addAll(gelezen);
        for (Plaatslezing p : plaatsen) {
            gebruik.add(p.aantal());
            gebruik.add(p.plaats());
        }
        List<RuleItemBuilder<?>> items = lez.linkerkant(w -> fout(toets, w) != null, gelezen, filter);
        items.add(gevolg(lez, lez.drager, posities(gebruik), (drools, onderwerp, w) -> {
            RuntimeException fout = fout(toets, w);
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

    /** The reads of a contribution rule: the member is the subject, the fold's subject is bound before it, and
     *  {@code bereik} joins the two. */
    @SuppressWarnings("unchecked")
    private Lezingen bijdragelezingen(Objecttype subject, Objecttype elem, Consumer<Lezingen> bereik) {
        Lezingen lez = new Lezingen(elem);
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
        lijstplaatsen = true;
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
        return new Plaatslezing(veld, lid.nieuw("lijstplaatsen " + n, aantal), lid.nieuw("lijstplaats " + n, minste));
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
                    + " de exec-modeltak weigert deze vorm voorzichtig tot een eigen kaart haar meet (§13.3.9"
                    + " opslagrichting)");
        Objecttype drager = voorwaarts ? subject : elem, doel = voorwaarts ? elem : subject;
        String rol = voorwaarts ? elemRol : terug;
        if (!FactModelGenerator.heeftReferentieVeld(model, drager, rol, doel))
            throw new Weigering("de aggregatie-join van '" + wat + "' heeft geen beanveld — "
                    + FactModelGenerator.geenJoinveldReden(model, drager, rol, doel) + " (§13.3.9 opslagrichting)");
        Function<Object, Object> veld = Accessors.getter(bonenklasse(drager), rol);
        Consumer<Lezingen> koppel = lid -> lid.onderwerp.expr(id("bereik"), lid.drager, voorwaarts
                ? (e, s) -> veld.apply(s) == e
                : (e, s) -> veld.apply(e) == s);
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
            lid.bereik.add(DSL.exists(PatternDSL.pattern(k).expr(id("bereik-koppeling"), x -> x.relatie().equals(relatie))
                    .expr(id("bereik-lid"), lid.s, (x, e) -> (lidIsEerste ? x.eerste() : x.tweede()) == e)
                    .expr(id("bereik-drager"), lid.drager, (x, d) -> (lidIsEerste ? x.tweede() : x.eerste()) == d)));
        }, lid -> {
            throw new Weigering("een filter dat kan falen in een vouw over de gecreëerde relatie '" + rb.feittype()
                    + "' ('" + wat + "'): een gemaakte link heeft geen plaats in de vouwvolgorde, die bepaalt welke fout"
                    + " de vouw geeft (RUNTIME-SEMANTICS ch. 5.11); " + NOG_NIET);
        });
    }

    /**
     * The multi-step range of a fold (IR-SPEC ch. 4.3, {@code sourcePath}; RUNTIME-SEMANTICS ch. 3.4b), on the plan the
     * shared {@link Bronpad} derives, lowered as the exec-model branch lowers it: where the walk down from the subject
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

    /** A condition as a predicate over the values its reads bind, in the order {@link Lezingen} gives them indices. */
    @FunctionalInterface
    private interface Toets {
        boolean test(Object[] waarden);
    }

    /** Every condition node is named here, so a node the IR records gain later fails to compile until it is decided
     *  whether this branch reads it. */
    private Toets voorwaarde(Conditie c, Lezingen lez) {
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
                yield w -> { throw new Rekenfout(Rekenfout.ONOPGELOSTE_VOORWAARDE); };
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
    private Toets tijdvenster(Conditie.Periode p, Lezingen lez) {
        if (p.vanaf() == null && p.tot() == null)
            throw new Weigering("een periodevenster zonder grens ('" + snippet(p) + "'): er is geen venster om op te"
                    + " poorten");
        for (Expr grens : java.util.Arrays.asList(p.vanaf(), p.tot()))
            if (grens != null && !Vensterpositie.grensLowerbaar(grens, lez.subject, navigatieBindingen::containsKey))
                throw new Weigering(Vensterpositie.GRENSVORM + ": " + snippet(p));
        Waardefunctie vanaf = p.vanaf() == null ? null : waarde(p.vanaf(), lez);
        Waardefunctie tot = p.tot() == null ? null : waarde(p.tot(), lez);
        int datum = lez.rekendatum();
        boolean totInclusief = p.totInclusief();
        return w -> {
            Object van = vanaf == null ? null : vanaf.van(w);
            Object eind = tot == null ? null : tot.van(w);
            if (!(w[datum] instanceof LocalDate dag)) return false;
            if (vanaf != null && !(van instanceof LocalDate v && !dag.isBefore(v))) return false;
            if (tot != null && !(eind instanceof LocalDate t && (totInclusief ? !dag.isAfter(t) : dag.isBefore(t))))
                return false;
            return true;
        };
    }

    private static Weigering nietGelezen(String vorm, Conditie c, String reden) {
        return new Weigering("een " + vorm + " in de voorwaarde ('" + snippet(c) + "'): " + reden);
    }

    /** A characteristic check: of the subject, a bound truth value, complemented where the check is negated; over a
     *  role, the reached object's, and a check over an absent link holds in neither polarity (RUNTIME-SEMANTICS ch. 5.8
     *  rule 3). The standings are total, so no negation is needed here. */
    private Toets kenmerkcheck(Conditie.Kenmerk k, Lezingen lez) {
        if (k.periode() != null)
            throw new Weigering("een CharacteristicCheck met wholePeriod ('" + snippet(k) + "'): tijdlijnen zijn " + BUITEN);
        // WF-109: the host is satisfied extensionally; a host the subject is no instance of names another object, which
        // a rule condition does not bind
        if (!Gastheer.hostOp(k, lez.subject))
            throw new Weigering("een CharacteristicCheck met een host die het onderwerp niet is ('" + snippet(k) + "')");
        boolean gewenst = !k.negatie();
        List<RolBindingRef> nav = navigatieBindingen.get(k);
        if (nav == null) {
            if (k.navigatie() != null)
                throw new Weigering("een CharacteristicCheck met een navigatie zonder binding ('" + snippet(k) + "')");
            int i = lez.eigen(lid(lez.subject, k.kenmerk(), true));
            return w -> (Boolean) w[i] == gewenst;
        }
        Pad p = pad(lez.subject, nav, null, "de kenmerkcheck '" + snippet(k) + "'");
        int i = lez.genavigeerd(p, lid(p.bereikt(), k.kenmerk(), true));
        return w -> w[i] != null && (Boolean) w[i] == gewenst;
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
     * refused in the sentence the other two branches give.
     */
    private Toets rolcontrole(Conditie.Rollidmaatschap r, Lezingen lez) {
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
            return w -> (((Number) w[i]).longValue() > 0) != ontkend;
        }
        FactModelGenerator.Lidmaatschapsopslag o = FactModelGenerator.lidmaatschapsopslag(model, rb);
        if (o.weigering() != null) throw nietGelezen("RoleMembership", r, o.weigering());
        if (o.opSubject()) {
            if (!FactModelGenerator.heeftReferentieVeld(model, lez.subject, o.beanveld(), o.ander()))
                throw nietGelezen("RoleMembership", r, FactModelGenerator.geenReferentieveldReden(model, lez.subject,
                        o.beanveld(), o.ander()) + " (§13.3.9 opslagrichting)");
            int i = lez.rolveld(o.beanveld(), klasse);
            return w -> (Boolean) w[i] != ontkend;
        }
        Objecttype vuller = tegenpartij != null ? tegenpartij : o.ander();
        if (!FactModelGenerator.heeftReferentieVeld(model, vuller, o.beanveld(), lez.subject))
            throw nietGelezen("RoleMembership", r, FactModelGenerator.geenReferentieveldReden(model, vuller,
                    o.beanveld(), lez.subject) + " (§13.3.9 opslagrichting)");
        int i = lez.rolvullers(bonenklasse(vuller), o.beanveld());
        return w -> (((Number) w[i]).longValue() > 0) != ontkend;
    }

    /** A composite folds its members in wire order and stops at the first member after which the verdict stands
     *  (RUNTIME-SEMANTICS ch. 5.10), so a member that would fault, a comparison with a division by zero, is not read
     *  where an earlier member settled the verdict, and the composite faults only where it reads such a member. */
    private Toets samengesteld(Conditie.Samengesteld s, Lezingen lez) {
        // a window under GEEN or a counting quantifier has no reading (WF-112), refused in the sentence both other
        // branches give
        String venster = Vensterpositie.weigering(s);
        if (venster != null) throw new Weigering(venster + ": " + snippet(s));
        List<Toets> leden = new ArrayList<>();
        for (Conditie deel : s.onderdelen()) leden.add(voorwaarde(deel, lez));
        Conditie.Kwantor kw = s.kwantor();
        int drempel = s.drempel();
        return w -> {
            int raak = 0;
            for (int i = 0; i < leden.size(); i++) {
                Boolean vast = vastgesteld(kw, drempel, raak, i, leden.size() - i);
                if (vast != null) return vast;
                if (leden.get(i).test(w)) raak++;
            }
            return switch (kw) {
                case ALLE, DE -> raak == leden.size();
                case GEEN -> raak == 0;
                case TEN_MINSTE -> raak >= drempel;
                case TEN_HOOGSTE -> raak <= drempel;
                case PRECIES -> raak == drempel;
            };
        };
    }

    /** The stop triggers of RUNTIME-SEMANTICS ch. 5.10, as the exec-model branch reads them
     *  ({@code ExecModelTranspiler.vastgesteld}, private there): the verdict once {@code gelezen} members are read, of
     *  which {@code raak} hold and {@code rest} are unread, or null while the unread members can still change it. */
    private static Boolean vastgesteld(Conditie.Kwantor kw, int drempel, int raak, int gelezen, int rest) {
        return switch (kw) {
            case ALLE, DE -> raak < gelezen ? Boolean.FALSE : null;
            case GEEN -> raak > 0 ? Boolean.FALSE : null;
            case TEN_MINSTE -> raak >= drempel ? Boolean.TRUE : (raak + rest < drempel ? Boolean.FALSE : null);
            case TEN_HOOGSTE -> raak > drempel ? Boolean.FALSE : (raak + rest <= drempel ? Boolean.TRUE : null);
            case PRECIES -> (raak > drempel || raak + rest < drempel) ? Boolean.FALSE : null;
        };
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
     * invisible. Where it can, the order shows (ch. 5.10, 5.11): the reference interpreter reads the matrix in the
     * contributors' order and stops at the first contributor after which the verdict stands, so a contributor it does not
     * reach raises no fault and deposits nothing. A contribution then carries the matrix's outcome and the member's place
     * ({@link Lidtoets}), the accumulate collects them, and the evaluation replays that order ({@link #kwantorlezing}).
     * The filter is settled for every member before the matrix, as for any fold ({@link #gelezenVouw}). The range is one
     * step, {@code sourceRole}: the loader refuses a multi-step one (WF-108 (c)), and so does this method where a
     * hand-built model carries one.
     */
    private Toets kwantorvouw(Conditie.Kwantorvouw kv, Lezingen lez) {
        // WF-112, in the sentence of the other two branches: a window in the filter or the test under none or a counting
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
        Vouw v = vouwVoor(kv, lez.subject, (sleutel, naam) -> kwantorbijdragen(kv, sleutel, naam, lez.subject, elem));
        if (!v.geordend()) {
            int[] i = lez.vouw(v, accumulatiefunctie("BigDecimalSumAccumulateFunction"));
            Waardefunctie oordeel = gelezenVouw(v, i, lez, w -> {
                long n = ((Number) w[i[0]]).longValue();
                return eindoordeel(kw, drempel, n == 0 ? 0 : ((BigDecimal) w[i[1]]).longValueExact(), n);
            });
            return w -> (Boolean) oordeel.van(w);
        }
        int[] i = lez.vouw(v, accumulatiefunctie("CollectListAccumulateFunction"));
        if (v.lidKanFalen()) lez.markeerFalen();
        int sonde = v.lidDeponeert() ? lez.sonde() : -1;
        Waardefunctie oordeel = gelezenVouw(v, i, lez, w -> kwantorlezing(kw, drempel,
                leeg(w[i[0]]) ? List.of() : (List<?>) w[i[1]], w, sonde));
        return w -> (Boolean) oordeel.van(w);
    }

    /**
     * The contribution rules of the quantifier fold {@code kv} on {@code subject}: one over its range, the fold's
     * subject bound first and the member joined to it, the member's reads of the filter and of the matrix bound, the
     * filter as the test (through {@link #zonderFout} where it can fault), and a consequence that evaluates the matrix
     * for the member and inserts the member's {@link Bijdrage} logically: 1 or 0 for its verdict, or, where the matrix
     * can fault or deposit, its {@link Lidtoets}, with the member's place bound as the filter fault rule binds it
     * ({@link Bereik#perRoute}). Beside it the filter fault rule and the filter deposit rule of a filter that can fault
     * or deposit, as for any fold ({@link #bijdrageregels}). A made link has no place in the order of the members, so a
     * matrix that can fault or deposit over a relation a form creates is refused.
     */
    private Vouw kwantorbijdragen(Conditie.Kwantorvouw kv, String sleutel, String naam, Objecttype subject,
                                  Objecttype elem) {
        String wat = snippet(kv);
        Bereik bereik = eenstapsbereik(kv.bronRol(), subject, elem, wat);
        // the member's reads first and its join after: whether the matrix can fault or deposit decides whether the join
        // binds the member's place
        Lezingen lez = bijdragelezingen(subject, elem, l -> { });
        Set<Integer> gelezen = lez.volg();
        Toets filter = kv.filter() == null ? null : voorwaarde(kv.filter(), lez);
        boolean filterKanFalen = lez.kanFalen();
        lez.stop();
        Set<Integer> gebruik = lez.volg();
        int lid = lez.lid();
        Toets matrix = voorwaarde(kv.toets(), lez);
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
        List<RuleItemBuilder<?>> items = lez.linkerkant(filterKanFalen ? zonderFout(filter) : filter, gelezen,
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

    /** The verdict of a quantifier over {@code n} members of which {@code raak} hold, every member read (RUNTIME-SEMANTICS
     *  ch. 5.10): all where all hold, none where none does, the counting quantifiers on the threshold. */
    private static boolean eindoordeel(Conditie.Kwantor kw, int drempel, long raak, long n) {
        return switch (kw) {
            case ALLE, DE -> raak == n;
            case GEEN -> raak == 0;
            case TEN_MINSTE -> raak >= drempel;
            case TEN_HOOGSTE -> raak <= drempel;
            case PRECIES -> raak == drempel;
        };
    }

    /**
     * The matrix of a quantifier fold read as the reference interpreter reads it, over the {@link Lidtoets} of each
     * contributor (RUNTIME-SEMANTICS ch. 3.9a, 5.10): in the contributors' order, with the stop triggers of ch. 5.10
     * tested before each contributor, the empty prefix included. A contributor that is reached deposits where its
     * matrix deposited, heard by the probe at {@code sonde}, and faults where its matrix faulted; one that is not reached
     * does neither.
     */
    private static Boolean kwantorlezing(Conditie.Kwantor kw, int drempel, List<?> bijdragen, Object[] w, int sonde) {
        List<Lidtoets> leden = new ArrayList<>(bijdragen.size());
        for (Object b : bijdragen) leden.add((Lidtoets) b);
        leden.sort(null);
        int n = leden.size(), raak = 0;
        for (int k = 0; k < n; k++) {
            Boolean vast = vastgesteld(kw, drempel, raak, k, n - k);
            if (vast != null) return vast;
            Lidtoets lid = leden.get(k);
            if (lid.gedeponeerd()) Sonde.meld(w, sonde);
            if (lid.fout() != null) throw lid.fout();
            if (lid.waar()) raak++;
        }
        return eindoordeel(kw, drempel, raak, n);
    }

    /**
     * A comparison of two values the rule binds or computes, with the empty operand of RUNTIME-SEMANTICS ch. 1.2 as the
     * exec-model branch reads it: numbers through {@code rsAggVergelijk} (an ordering with a leeg operand is onwaar, two
     * leeg numbers are equal, exactly one leeg is unequal; exact where a value is a rational), a percentage being a
     * number in the unit %, the right operand restated into the left one's unit (RUNTIME-SEMANTICS ch. 2.6); dates of
     * one precision through {@link #datumVergelijk} (two leeg dates are not equal); a text, an enumeration label or a
     * truth value through {@link #tekstGelijk}, an enumeration up to its normalised label. The left operand is evaluated
     * before the right one (ch. 5.11). Two sides whose types allow no comparison are refused.
     */
    private Toets vergelijking(Conditie.Vergelijking v, Lezingen lez) {
        if (v.periode() != null)
            throw new Weigering("een Comparison met wholePeriod ('" + snippet(v) + "'): tijdlijnen zijn " + BUITEN);
        Datatype lt = eff.typeVan(v.links()), rt = eff.typeVan(v.rechts());
        Conditie.Operator op = v.op();
        Waardefunctie l = waarde(v.links(), lez), r = waarde(v.rechts(), lez);
        if (lt.isGetalachtig() && rt.isGetalachtig() && !isDatumOp(op)) {
            Waardefunctie rechts = herschaald(r, eff.eenheidVan(v.rechts()), eff.eenheidVan(v.links()),
                    "een §5.1 vergelijking");
            String symbool = Operatoren.symbool(op);
            return w -> {
                Object links = l.van(w);
                return RegelSpraakRuntime.rsAggVergelijk(links, rechts.van(w), symbool);
            };
        }
        if (lt.isDatum() && rt.isDatum() && (isDatumOp(op) || op == Conditie.Operator.GELIJK
                || op == Conditie.Operator.ONGELIJK)) {
            if (!zelfdeDatumsoort(lt, rt))
                throw new Weigering("een Comparison van een datum met een datum-tijd ('" + snippet(v) + "', §5.3 Tabel 36)");
            return w -> datumVergelijk(datumWaarde(l.van(w)), datumWaarde(r.van(w)), op);
        }
        if ((op == Conditie.Operator.GELIJK || op == Conditie.Operator.ONGELIJK) && gelijkheidstype(lt)
                && gelijkheidstype(rt) && lt.isBoolean() == rt.isBoolean()) {
            UnaryOperator<Object> lezing = lt instanceof Datatype.Enumeratie || rt instanceof Datatype.Enumeratie
                    ? RegelSpraakRuntime::rsEnumNaam : UnaryOperator.identity();
            boolean gelijk = op == Conditie.Operator.GELIJK;
            return w -> tekstGelijk(lezing.apply(l.van(w)), lezing.apply(r.van(w)), gelijk);
        }
        throw new Weigering("een Comparison " + Spelling.COMPARISON_OP.spelling(op) + " tussen " + typenaam(lt) + " en "
                + typenaam(rt) + " ('" + snippet(v) + "'): de typen van de twee kanten laten deze vergelijking niet toe"
                + " (RUNTIME-SEMANTICS ch. 1.2)");
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

    private static Object datumWaarde(Object v) {
        return v instanceof java.time.LocalDate || v instanceof java.time.LocalDateTime ? v : null;
    }

    /** A date comparison with the exec-model branch's leeg tables ({@code datumVergelijkTemporal}, private there): an
     *  ordering with a leeg operand is onwaar, equality of two leeg dates is onwaar (§5.3 Tabel 38), inequality holds
     *  where exactly one is leeg (§5.6 Tabel 44). */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static boolean datumVergelijk(Object a, Object b, Conditie.Operator op) {
        switch (op) {
            case GELIJK: return Objects.equals(a, b) && !(a == null && b == null);
            case ONGELIJK: return !Objects.equals(a, b);
            default: break;
        }
        if (!(a instanceof Comparable) || b == null) return false;
        int c = ((Comparable) a).compareTo(b);
        return switch (op) {
            case EERDER -> c < 0;
            case EERDER_OF_GELIJK -> c <= 0;
            case LATER -> c > 0;
            case LATER_OF_GELIJK -> c >= 0;
            default -> false;
        };
    }

    /** The equality of a text, an enumeration label or a truth value with the exec-model branch's leeg tables
     *  ({@code tekstAttrGelijk}): equal only where both are filled and the same, unequal where exactly one is leeg or
     *  the two differ. */
    private static boolean tekstGelijk(Object a, Object b, boolean gelijk) {
        boolean beideLeeg = a == null && b == null;
        boolean zelfde = Objects.equals(a, b);
        return gelijk ? zelfde && !beideLeeg : !zelfde && !beideLeeg;
    }

    /** A leeg or gevuld test of a value; a read over an absent link is leeg (RUNTIME-SEMANTICS ch. 5.8 rule 1). The loader
     *  writes a negated test as its sister test. */
    private Toets predicaat(Conditie.Predicaat p, Lezingen lez) {
        if (p.periode() != null)
            throw new Weigering("een Predicate met wholePeriod ('" + snippet(p) + "'): tijdlijnen zijn " + BUITEN);
        switch (p.soort()) {
            case LEEG, GEVULD -> {
                Waardefunctie f = waarde(p.operand(), lez);
                boolean leeg = p.soort() == Conditie.Predicaatsoort.LEEG;
                return w -> (f.van(w) == null) == leeg;
            }
            case ELFPROEF, NUMERIEK_MET_EXACT -> {
                return cijferproef(p, lez);
            }
            case DAGSOORT -> throw new Weigering("een Predicate dayKind ('" + snippet(p) + "'): dagsoorten zijn " + BUITEN);
            default -> throw new IllegalStateException("predicaatsoort " + p.soort());
        }
    }

    /** The eleven test and the exact-digit test (§8.1.3, §8.1.4), as the exec-model branch tests them
     *  ({@code rsElfproef}, {@code rsNumeriekMetExact}), the written negation complementing the verdict: a leeg value
     *  passes neither test. The operand is read as any value; over a path of roles that branch makes an absent link fail
     *  both polarities, which a leeg read here cannot tell from a leeg value, so such an operand is refused by name. The
     *  reference interpreter and the ASP backend do not evaluate these predicates. */
    private Toets cijferproef(Conditie.Predicaat p, Lezingen lez) {
        if (p.operand() instanceof Expr.AttribuutRef a && navigatieBindingen.containsKey(a))
            throw new Weigering("een Predicate " + Spelling.PREDICATE_KIND.spelling(p.soort()) + " over een pad ('"
                    + snippet(p) + "'): de exec-modeltak laat een afwezige link beide polariteiten onwaar maken, wat"
                    + " een lege lezing hier niet van een lege waarde onderscheidt");
        Waardefunctie f = waarde(p.operand(), lez);
        boolean elf = p.soort() == Conditie.Predicaatsoort.ELFPROEF;
        int cijfers = p.cijfers();
        boolean gewenst = !p.negatie();
        return w -> {
            Object v = f.van(w);
            return (elf ? RegelSpraakRuntime.rsElfproef(v) : RegelSpraakRuntime.rsNumeriekMetExact(v, cijfers)) == gewenst;
        };
    }

    // ---------------- the reads of one rule ----------------

    /**
     * The reads of one rule, each bound once in its left-hand side and numbered in the order they are met: a member of
     * the subject that no form writes off the subject's bean pattern, over a path through the reference fields read in
     * the same binding; a derived member of the subject off its standing; a derived member over a path off its
     * {@link PadStand}; a parameter through a declaration of its own. The numbers are the indices of the value arrays
     * the condition's test and the consequence receive. The standings and path standings a rule reads are registered
     * with the transpiler only when the rule lowers ({@link #vastleggen}).
     */
    private final class Lezingen {
        final Objecttype subject;
        final Variable<Object> s;
        final PatternDSL.PatternDef<Object> onderwerp;
        final List<RuleItemBuilder<?>> parameters = new ArrayList<>();
        final List<RuleItemBuilder<?>> standen = new ArrayList<>();
        final List<Variable<?>> variabelen = new ArrayList<>();
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
        /** The patterns right after the subject pattern: the existential intermediates of a multi-step range. */
        final List<RuleItemBuilder<?>> bereik = new ArrayList<>();
        /** The folds this rule reads, registered with it ({@link #vastleggen}). */
        private final List<Vouw> vouwen = new ArrayList<>();
        /** The binders of the {@code Let}s around the expression being compiled, by normalised name, each to its slot
         *  in the value array ({@link #binder}); a nested {@code Let} of the same name shadows the outer one while its
         *  body compiles (WF-064). */
        final Map<String, Integer> binders = new HashMap<>();
        /** The slot of the probe of this rule's deposits ({@link #sonde}), -1 while no place that deposits compiled. */
        private int sonde = -1;

        @SuppressWarnings("unchecked")
        Lezingen(Objecttype subject) {
            this.subject = subject;
            this.s = (Variable<Object>) DSL.declarationOf(bonenklasse(subject), "$onderwerp");
            this.onderwerp = PatternDSL.pattern(s);
        }

        /** The subject itself as a bound value, for a consequence that hands on another object beside it. */
        int lid() {
            int bekend = bestaand("onderwerp zelf");
            return bekend >= 0 ? bekend : nieuw("onderwerp zelf", s);
        }

        /** The left-hand side of the rule, once its condition and its value are compiled: the subject pattern with its
         *  bindings, the parameter declarations, the standing patterns, and the test of {@code voorwaarde} over the
         *  values at {@code gelezen}. */
        List<RuleItemBuilder<?>> linkerkant(Toets toets, Set<Integer> gelezen, Conditie voorwaarde) {
            return linkerkant(toets, gelezen, voorwaarde, List.of());
        }

        /** As {@link #linkerkant(Toets, Set, Conditie)}, with {@code stuur} between the standing patterns and the test:
         *  the control-fact patterns of a default, so that a default a higher one displaced is not tested at all. */
        List<RuleItemBuilder<?>> linkerkant(Toets toets, Set<Integer> gelezen, Conditie voorwaarde,
                                            List<RuleItemBuilder<?>> stuur) {
            List<RuleItemBuilder<?>> items = new ArrayList<>(kop);
            items.add(onderwerp);
            items.addAll(bereik);
            items.addAll(parameters);
            items.addAll(standen);
            items.addAll(stuur);
            if (toets != null) items.add(test(this, posities(gelezen), toets, voorwaarde));
            return items;
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
         *  can is tested through {@link #zonderFout}, beside the rule that inserts its fault. */
        boolean kanFalen() { return falen; }

        /** Registers what the lowered rule reads with the transpiler: the standings, path standings and channel
         *  standings it needs, and the contribution rules of its folds with what those read. */
        void vastleggen() {
            gelezen.putAll(afgeleid);
            for (Padlezing p : paden.values()) {
                padlezingen.putIfAbsent(p.sleutel(), p);
                if (schrijvers.containsKey(p.lid().sleutel())) gelezen.putIfAbsent(p.lid().sleutel(), p.lid());
            }
            kanalen.addAll(kanaalnamen);
            for (Vouw v : vouwen) {
                bijdrageregels.putIfAbsent(v.sleutel(), v.regels());
                for (Lezingen l : v.lezingen()) l.vastleggen();
            }
        }

        /**
         * A fold of this rule's subject, bound off the contributions of the fold {@code sleutel} ({@link Bijdrage}): one
         * accumulate over them that counts them and, where {@code functie} names an accumulate function, folds their
         * values with it; where the fold's filter can fault, a second accumulate over its {@link Filterfout} facts
         * on this subject, which binds the least of them, the first in fold order, or null; and where the filter can
         * deposit, an accumulate that counts its {@link Filterdeponering} facts on this subject. Returns the index of
         * the count, that of the folded value, that of the least filter fault and that of the count of filter deposits,
         * each -1 without one. An accumulate over no fact still binds a row, through the count, so the rule reads an
         * empty fold, and a fold without a filter fault or a filter deposit, as well.
         */
        int[] vouw(Vouw v, Class<?> functie) {
            vouwen.add(v);
            String sleutel = v.sleutel();
            int bekend = bestaand("vouw " + sleutel);
            if (bekend >= 0) return new int[]{bekend, functie == null ? -1 : bestaand("vouwwaarde " + sleutel),
                    v.kanFalen() ? bestaand("filterfout " + sleutel) : -1,
                    v.deponeert() ? bestaand("filterdeponering " + sleutel) : -1};
            Variable<Bijdrage> b = DSL.declarationOf(Bijdrage.class, "$bijdrage_" + id("vouw"));
            PatternDSL.PatternDef<Bijdrage> p = PatternDSL.pattern(b).expr(id("vouw"), x -> x.vouw().equals(sleutel))
                    .expr(id("vouw-onderwerp"), s, (x, o) -> x.onderwerp() == o);
            Variable<Object> aantal = DSL.declarationOf(Object.class, "$aantal_" + id("vouw"));
            int[] uit;
            if (functie == null) {
                standen.add(DSL.accumulate(p, DSL.accFunction(accumulatiefunctie("CountAccumulateFunction"), b)
                        .as(aantal)));
                uit = new int[]{nieuw("vouw " + sleutel, aantal), -1, -1, -1};
            } else {
                Variable<Object> bijdragewaarde = DSL.declarationOf(Object.class, "$bijdragewaarde_" + id("vouw"));
                Variable<Object> uitkomst = DSL.declarationOf(Object.class, "$vouw_" + id("vouw"));
                p.bind(bijdragewaarde, Bijdrage::waarde);
                standen.add(DSL.accumulate(p, DSL.accFunction(accumulatiefunctie("CountAccumulateFunction"), b)
                        .as(aantal), DSL.accFunction(functie, bijdragewaarde).as(uitkomst)));
                uit = new int[]{nieuw("vouw " + sleutel, aantal), nieuw("vouwwaarde " + sleutel, uitkomst), -1, -1};
            }
            if (v.kanFalen()) {
                Variable<Filterfout> f = DSL.declarationOf(Filterfout.class, "$filterfout_" + id("vouw"));
                PatternDSL.PatternDef<Filterfout> q = PatternDSL.pattern(f)
                        .expr(id("filterfout"), x -> x.vouw().equals(sleutel))
                        .expr(id("filterfout-onderwerp"), s, (x, o) -> x.onderwerp() == o);
                Variable<Object> fouten = DSL.declarationOf(Object.class, "$filterfouten_" + id("vouw"));
                Variable<Object> eerste = DSL.declarationOf(Object.class, "$eerste_filterfout_" + id("vouw"));
                standen.add(DSL.accumulate(q, DSL.accFunction(accumulatiefunctie("CountAccumulateFunction"), f)
                        .as(fouten), DSL.accFunction(accumulatiefunctie("MinAccumulateFunction"), f).as(eerste)));
                uit[2] = nieuw("filterfout " + sleutel, eerste);
            }
            if (v.deponeert()) {
                Variable<Filterdeponering> d = DSL.declarationOf(Filterdeponering.class, "$filterdeponering_" + id("vouw"));
                PatternDSL.PatternDef<Filterdeponering> q = PatternDSL.pattern(d)
                        .expr(id("filterdeponering"), x -> x.vouw().equals(sleutel))
                        .expr(id("filterdeponering-onderwerp"), s, (x, o) -> x.onderwerp() == o);
                Variable<Object> deponeringen = DSL.declarationOf(Object.class, "$filterdeponeringen_" + id("vouw"));
                standen.add(DSL.accumulate(q, DSL.accFunction(accumulatiefunctie("CountAccumulateFunction"), d)
                        .as(deponeringen)));
                uit[3] = nieuw("filterdeponering " + sleutel, deponeringen);
            }
            return uit;
        }

        /** The session's evaluation date (RUNTIME-SEMANTICS ch. 8.1), bound off its {@link Rekendatum}: one fact for the
         *  whole session, joined to no subject, bound among the parameters; null where the case carries no date. */
        int rekendatum() {
            int bekend = bestaand("rekendatum");
            if (bekend >= 0) return bekend;
            rekendatumGlobal();
            Variable<Rekendatum> r = DSL.declarationOf(Rekendatum.class, "$rekendatum_" + variabelen.size());
            Variable<Object> d = DSL.declarationOf(Object.class, "$rekendatumwaarde_" + variabelen.size());
            parameters.add(PatternDSL.pattern(r).bind(d, Rekendatum::datum));
            return nieuw("rekendatum", d);
        }

        /** Gates this rule on the evaluation date (RUNTIME-SEMANTICS ch. 8.2): it matches only where the validity
         *  window {@code g} admits the date, both bounds inclusive and an absent one open (IR-SPEC ch. 3.10), and never
         *  in a session without a date, where the form faults instead ({@link #geldigheidsfout}). A window that is always
         *  valid adds nothing. The date is constant while the session fires, so the gate never changes a match. */
        Lezingen geldig(Geldigheid g) {
            if (g == null || g.altijd()) return this;
            rekendatumGlobal();
            Variable<Rekendatum> r = DSL.declarationOf(Rekendatum.class, "$geldigheid_" + variabelen.size());
            parameters.add(PatternDSL.pattern(r).expr(id("geldig"), x -> g.bevat(x.datum())));
            return this;
        }

        /** A slot of the value array for the binder of a {@code Let} ({@link TmsTranspiler#let}): no pattern binds it, so
         *  it is no position a condition reads or a consequence binds, and the array is sized past it. */
        int binder() {
            variabelen.add(null);
            return variabelen.size() - 1;
        }

        /** The slot of the probe of this rule's deposits ({@link Sonde}), made at the first place that deposits; like a
         *  binder's slot no pattern binds it. */
        int sonde() {
            if (sonde < 0) {
                variabelen.add(null);
                sonde = variabelen.size() - 1;
            }
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
            Variable<Creatiefase> f = DSL.declarationOf(Creatiefase.class, "$creatiefase");
            Variable<Object> n = DSL.declarationOf(Object.class, "$fase");
            parameters.add(PatternDSL.pattern(f).expr(id("creatiestratum"), c -> c.stratum() >= stratum)
                    .bind(n, Creatiefase::nummer));
            return nieuw("creatiefase", n);
        }

        /** The marks of the creation that made this rule's subject, the {@link Gemaakt} facts whose object is the
         *  subject, bound as a list off an accumulate: empty for an object of the input, one mark for an object a
         *  creation made. An accumulate over no fact still binds a row, so the rule reads an object of the input as
         *  well. An object creation reads the depth of its subject off it ({@link TmsTranspiler#diepte}). */
        int herkomst() {
            int bekend = bestaand("herkomst");
            if (bekend >= 0) return bekend;
            Variable<Gemaakt> g = DSL.declarationOf(Gemaakt.class, "$herkomst_" + id("creatie"));
            Variable<Object> lijst = DSL.declarationOf(Object.class, "$herkomsten_" + id("creatie"));
            standen.add(DSL.accumulate(PatternDSL.pattern(g).expr(id("herkomst"), s, (x, o) -> x.object() == o),
                    DSL.accFunction(accumulatiefunctie("CollectListAccumulateFunction"), g).as(lijst)));
            return nieuw("herkomst", lijst);
        }

        /** The standing of the §9.5 channel of the form named {@code producent}, bound off its {@link KanaalStand}:
         *  one fact for the whole session, joined to no subject, since the channel test has none of its own. */
        @SuppressWarnings("unchecked")
        int kanaal(String producent) {
            String sleutel = "kanaal " + producent;
            int bekend = bestaand(sleutel);
            if (bekend >= 0) return bekend;
            Variable<Object> w = DSL.declarationOf(Object.class, "$lezing_" + variabelen.size());
            Variable<KanaalStand> st = DSL.declarationOf(KanaalStand.class, "$kanaal_" + variabelen.size());
            standen.add(PatternDSL.pattern(st).expr(id("kanaal"), k -> k.regel().equals(producent))
                    .bind(w, KanaalStand::inconsistent));
            kanaalnamen.add(producent);
            return nieuw(sleutel, w);
        }

        /** Whether the reference field {@code veld} of this rule's subject is set, to an instance of {@code klasse} where
         *  a role check narrows the other end (null: any): a link of an input relation stored on the subject
         *  ({@link TmsTranspiler#rolcontrole}). Input does not change while the session fires, so it is bound off the
         *  subject's bean, as an input member is ({@link #eigen}). */
        int rolveld(String veld, Class<?> klasse) {
            String sleutel = "rolveld " + veld + " " + (klasse == null ? "" : klasse.getName());
            int bekend = bestaand(sleutel);
            if (bekend >= 0) return bekend;
            Variable<Object> w = DSL.declarationOf(Object.class, "$lezing_" + variabelen.size());
            Function<Object, Object> getter = Accessors.getter(bonenklasse(subject), veld);
            onderwerp.bind(w, o -> {
                Object ander = getter.apply(o);
                return ander != null && (klasse == null || klasse.isInstance(ander));
            });
            return nieuw(sleutel, w);
        }

        /** The number of beans of {@code klasse} whose reference field {@code veld} names this rule's subject: the links
         *  of an input relation stored on the other end ({@link TmsTranspiler#rolcontrole}), bound off an accumulate,
         *  which over no bean still binds a row. */
        @SuppressWarnings("unchecked")
        int rolvullers(Class<?> klasse, String veld) {
            String sleutel = "rolvullers " + klasse.getName() + " " + veld;
            int bekend = bestaand(sleutel);
            if (bekend >= 0) return bekend;
            Variable<Object> vuller = (Variable<Object>) DSL.declarationOf(klasse, "$vuller_" + id("rol"));
            Variable<Object> aantal = DSL.declarationOf(Object.class, "$vullers_" + id("rol"));
            Function<Object, Object> getter = Accessors.getter(klasse, veld);
            standen.add(DSL.accumulate(PatternDSL.pattern(vuller).expr(id("rolvuller"), s, (x, o) -> getter.apply(x) == o),
                    DSL.accFunction(accumulatiefunctie("CountAccumulateFunction"), vuller).as(aantal)));
            return nieuw(sleutel, aantal);
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
            Variable<Koppeling> k = DSL.declarationOf(Koppeling.class, "$rolkoppeling_" + id("rol"));
            Variable<Object> aantal = DSL.declarationOf(Object.class, "$rolkoppelingen_" + id("rol"));
            standen.add(DSL.accumulate(PatternDSL.pattern(k).expr(id("rolkoppeling"), x -> x.relatie().equals(relatie))
                    .expr(id("rolkoppeling-onderwerp"), s, (x, o) -> (onderwerpIsEerste ? x.eerste() : x.tweede()) == o
                            && (klasse == null || klasse.isInstance(onderwerpIsEerste ? x.tweede() : x.eerste()))),
                    DSL.accFunction(accumulatiefunctie("CountAccumulateFunction"), k).as(aantal)));
            return nieuw(sleutel, aantal);
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
            Variable<Object> w = DSL.declarationOf(Object.class, "$lezing_" + variabelen.size());
            Function<Object, Object> getter = Accessors.getter(bonenklasse(subject), paar[0].naam());
            onderwerp.bind(w, o -> getter.apply(o));
            return nieuw(sleutel, w);
        }

        private int bestaand(String sleutel) {
            Integer i = index.get(sleutel);
            if (i != null && gebruik != null) gebruik.add(i);
            return i == null ? -1 : i;
        }

        private int nieuw(String sleutel, Variable<?> v) {
            int i = variabelen.size();
            variabelen.add(v);
            index.put(sleutel, i);
            if (gebruik != null) gebruik.add(i);
            return i;
        }

        @SuppressWarnings({"unchecked", "rawtypes"})
        int eigen(Lid lid) {
            String sleutel = "eigen " + lid.sleutel();
            int bekend = bestaand(sleutel);
            if (bekend >= 0) return bekend;
            Variable<Object> w = DSL.declarationOf(Object.class, "$lezing_" + variabelen.size());
            ElementSleutel lk = lid.sleutel();
            if (schrijvers.containsKey(lk)) {
                if (lid.kenmerk()) {
                    Variable<KenmerkStand> st = DSL.declarationOf(KenmerkStand.class, "$stand_" + variabelen.size());
                    standen.add(PatternDSL.pattern(st).expr(id("stand"), x -> x.kenmerk().equals(lk))
                            .expr(id("stand-onderwerp"), s, (x, o) -> x.onderwerp() == o)
                            .bind(w, KenmerkStand::waar));
                } else {
                    Variable<Stand> st = DSL.declarationOf(Stand.class, "$stand_" + variabelen.size());
                    standen.add(PatternDSL.pattern(st).expr(id("stand"), x -> x.attribuut().equals(lk))
                            .expr(id("stand-onderwerp"), s, (x, o) -> x.onderwerp() == o)
                            .bind(w, Stand::waarde));
                }
                afgeleid.put(lk, lid);
            } else {
                // input: no form writes it, so it does not change while the session fires and is read off the bean
                Function<Object, Object> getter = Accessors.getter(lid.klasse(), lid.naam());
                if (lid.kenmerk()) onderwerp.bind(w, o -> Boolean.TRUE.equals(getter.apply(o)));
                else onderwerp.bind(w, o -> getter.apply(o));
            }
            return nieuw(sleutel, w);
        }

        @SuppressWarnings({"unchecked", "rawtypes"})
        int genavigeerd(Pad pad, Lid lid) {
            String sleutel = "pad " + pad.sleutel() + " " + lid.sleutel();
            int bekend = bestaand(sleutel);
            if (bekend >= 0) return bekend;
            Variable<Object> w = DSL.declarationOf(Object.class, "$lezing_" + variabelen.size());
            ElementSleutel lk = lid.sleutel();
            // a derived member, or any member at the end of a path whose links a form creates: through the path's
            // standing, whose rules bind every link and the member in their left-hand sides
            if (schrijvers.containsKey(lk) || pad.gecreeerd()) {
                Padlezing p = new Padlezing(pad, lid);
                String ps = pad.sleutel();
                Variable<PadStand> st = DSL.declarationOf(PadStand.class, "$padstand_" + variabelen.size());
                standen.add(PatternDSL.pattern(st).expr(id("padstand"), x -> x.pad().equals(ps) && x.lid().equals(lk))
                        .expr(id("padstand-onderwerp"), s, (x, o) -> x.onderwerp() == o)
                        .bind(w, PadStand::waarde));
                paden.put(p.sleutel(), p);
            } else {
                // input over links that are input: bound off the subject's bean, null where a link is absent
                Function<Object, Object> getter = Accessors.getter(lid.klasse(), lid.naam());
                boolean kenmerk = lid.kenmerk();
                onderwerp.bind(w, o -> {
                    Object d = pad.loop(o);
                    if (d == null) return null;
                    return kenmerk ? Boolean.TRUE.equals(getter.apply(d)) : getter.apply(d);
                });
            }
            return nieuw(sleutel, w);
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
            Variable<Object> w = DSL.declarationOf(Object.class, "$lezing_" + variabelen.size());
            Variable<ParameterStand> st = DSL.declarationOf(ParameterStand.class, "$parameter_" + variabelen.size());
            parameters.add(PatternDSL.pattern(st).expr(id("parameter"), x -> x.parameter().equals(naam))
                    .bind(w, ParameterStand::waarde));
            return nieuw(sleutel, w);
        }
    }

    /** The rule that puts the {@link ParameterStand} of one parameter in working memory: it matches no fact, so it fires
     *  once per session, and it binds the global in its consequence, where the case runner set it before the session
     *  fired. The global does not change while the session fires, so the standing never needs to be withdrawn. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Rule parameterstandregel(String naam, Global<?> g) {
        return PatternDSL.rule(PKG, "parameterstand " + naam).build(
                DSL.on((Global) g).execute((Block2<org.drools.model.Drools, Object>) (drools, v) ->
                        drools.logicalInsert(new ParameterStand(naam, v))));
    }

    /** The global of parameter {@code p}, of the Java type the exec-model branch declares it with, so that the case
     *  runner sets it by name; one per parameter for the whole model. */
    private Global<?> global(Parameter p) {
        String naam = Namen.ident(p.naam(), false);
        Global<?> g = globals.get(naam);
        if (g != null) return g;
        if (p.granulariteit() != null)
            throw new Weigering("de tijdsafhankelijke parameter '" + p.naam() + "': tijdlijnen zijn " + BUITEN);
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

    /** The test over the values a condition reads, as a standalone test after the patterns that bind them. A condition
     *  that reads nothing is a constant. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private RuleItemBuilder<?> test(Lezingen lez, int[] pos, Toets toets, Conditie c) {
        if (pos.length > MAX_LEZINGEN)
            throw new Weigering("een voorwaarde die " + pos.length + " waarden leest ('" + snippet(c) + "'): de toets van"
                    + " deze tak neemt hoogstens " + MAX_LEZINGEN + " gebonden waarden");
        int n = lez.variabelen.size();
        Toets t = args -> {
            Object[] w = new Object[n];
            for (int j = 0; j < pos.length; j++) w[pos[j]] = args[j];
            return toets.test(w);
        };
        String id = id("voorwaarde");
        Variable[] v = new Variable[pos.length];
        for (int j = 0; j < pos.length; j++) v[j] = lez.variabelen.get(pos[j]);
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
            default -> throw new IllegalStateException("'" + snippet(c) + "' bindt " + v.length + " waarden; "
                    + "hierboven geweigerd boven " + MAX_LEZINGEN);
        };
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
        return new Gevolg(v, (drools, args) -> {
            Object[] w = new Object[n];
            for (int j = 0; j < pos.length; j++) w[pos[j]] = args[j + 1];
            f.voer(drools, args[0], w);
        });
    }

    /** A consequence over any number of bound variables. Each typed {@code execute} of the DSL sets its builder's block
     *  to a {@link org.drools.model.functions.BlockN} over the Drools handle and the bound facts, and the engine calls
     *  that block whatever its arity; this builder sets the same block directly, as the exec-model branch's
     *  {@code GebondenN} does, so a consequence is not limited by the arities the DSL spells out. */
    private static final class Gevolg extends org.drools.model.consequences.ConsequenceBuilder.AbstractValidBuilder<Gevolg> {
        @SuppressWarnings("rawtypes")
        Gevolg(Variable[] v, BiConsumer<org.drools.model.Drools, Object[]> body) {
            super(v);
            usingDrools = true;
            block = args -> body.accept((org.drools.model.Drools) args[0],
                    java.util.Arrays.copyOfRange(args, 1, args.length));
        }
    }

    // ---------------- standings ----------------

    /**
     * The three generic rules that keep one {@link KenmerkStand} per object of the declaring type: waar from the input,
     * waar from a derivation, onwaar when neither holds. The onwaar rule carries the branch's only negation over a
     * characteristic, {@code not HeeftKenmerk}, and reads the input in its left-hand side, so a derivation that appears
     * later retracts the onwaar standing and every conclusion that read it. When both sources hold, the two waar
     * standings are equal, and truth maintenance keeps them as one fact with a justification each.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private List<Rule> kenmerkstandregels(Lid lid) {
        Function<Object, Object> getter = Accessors.getter(lid.klasse(), lid.naam());
        ElementSleutel sleutel = lid.sleutel();
        String naam = "kenmerkstand " + lid.declarant().naam + "." + lid.naam() + ": ";

        Variable s1 = DSL.declarationOf(lid.klasse(), "$onderwerp");
        Rule invoer = PatternDSL.rule(PKG, naam + "waar uit de invoer").build(
                PatternDSL.pattern(s1).expr(id("invoer"), (Predicate1) o -> Boolean.TRUE.equals(getter.apply(o))),
                DSL.on(s1).execute((Block2<org.drools.model.Drools, Object>) (drools, o) ->
                        drools.logicalInsert(new KenmerkStand(o, sleutel, true))));

        Variable s2 = DSL.declarationOf(lid.klasse(), "$onderwerp");
        Variable<HeeftKenmerk> h2 = DSL.declarationOf(HeeftKenmerk.class, "$afgeleid");
        Rule afleiding = PatternDSL.rule(PKG, naam + "waar uit een afleiding").build(
                PatternDSL.pattern(s2),
                PatternDSL.pattern(h2).expr(id("afgeleid"), x -> x.kenmerk().equals(sleutel))
                        .expr(id("afgeleid-onderwerp"), s2, (x, o) -> x.onderwerp() == o),
                DSL.on(s2).execute((Block2<org.drools.model.Drools, Object>) (drools, o) ->
                        drools.logicalInsert(new KenmerkStand(o, sleutel, true))));

        Variable s3 = DSL.declarationOf(lid.klasse(), "$onderwerp");
        Variable<HeeftKenmerk> h3 = DSL.declarationOf(HeeftKenmerk.class, "$afgeleid");
        Rule onwaar = PatternDSL.rule(PKG, naam + "onwaar").build(
                PatternDSL.pattern(s3).expr(id("geen-invoer"), (Predicate1) o -> !Boolean.TRUE.equals(getter.apply(o))),
                DSL.not(PatternDSL.pattern(h3).expr(id("afgeleid"), x -> x.kenmerk().equals(sleutel))
                        .expr(id("afgeleid-onderwerp"), s3, (x, o) -> x.onderwerp() == o)),
                DSL.on(s3).execute((Block2<org.drools.model.Drools, Object>) (drools, o) ->
                        drools.logicalInsert(new KenmerkStand(o, sleutel, false))));
        return List.of(invoer, afleiding, onwaar);
    }

    /**
     * The two generic rules that keep one {@link Stand} per object of the declaring type: the derived value where a
     * {@link Waarde} exists, else the input on the bean, read in the left-hand side, leeg included. The input rule
     * carries {@code not Waarde}, so a derivation that appears later retracts the input standing and every value that
     * read it.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private List<Rule> attribuutstandregels(Lid lid) {
        Function<Object, Object> getter = Accessors.getter(lid.klasse(), lid.naam());
        ElementSleutel sleutel = lid.sleutel();
        String naam = "stand " + lid.declarant().naam + "." + lid.naam() + ": ";

        Variable<Object> s1 = (Variable<Object>) DSL.declarationOf(lid.klasse(), "$onderwerp");
        Variable<Waarde> w1 = DSL.declarationOf(Waarde.class, "$waarde");
        Variable<Object> v1 = DSL.declarationOf(Object.class, "$v");
        Rule afleiding = PatternDSL.rule(PKG, naam + "uit een afleiding").build(
                PatternDSL.pattern(s1),
                PatternDSL.pattern(w1).expr(id("waarde"), x -> x.attribuut().equals(sleutel))
                        .expr(id("waarde-onderwerp"), s1, (x, o) -> x.onderwerp() == o)
                        .bind(v1, Waarde::waarde),
                DSL.on(s1, v1).execute((Block3<org.drools.model.Drools, Object, Object>) (drools, o, v) ->
                        drools.logicalInsert(new Stand(o, sleutel, v))));

        Variable s2 = DSL.declarationOf(lid.klasse(), "$onderwerp");
        Variable<Object> v2 = DSL.declarationOf(Object.class, "$invoer");
        Variable<Waarde> w2 = DSL.declarationOf(Waarde.class, "$waarde");
        Rule invoer = PatternDSL.rule(PKG, naam + "uit de invoer").build(
                ((PatternDSL.PatternDef) PatternDSL.pattern(s2)).bind(v2, o -> getter.apply(o)),
                DSL.not(PatternDSL.pattern(w2).expr(id("waarde"), x -> x.attribuut().equals(sleutel))
                        .expr(id("waarde-onderwerp"), s2, (x, o) -> x.onderwerp() == o)),
                DSL.on(s2, v2).execute((Block3<org.drools.model.Drools, Object, Object>) (drools, o, v) ->
                        drools.logicalInsert(new Stand(o, sleutel, v))));
        return List.of(afleiding, invoer);
    }

    /**
     * The two generic rules that keep one {@link PadStand} per object of the subject type for a derived member read over
     * a path: where every link on the path is present, the standing of the member on the object the path reaches,
     * joined on that object; where a link is absent, null. The links are input, read off the bean in the left-hand
     * side, and do not change while the session fires, so exactly one of the two holds for each object.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private List<Rule> padstandregels(Padlezing p) {
        Pad pad = p.pad();
        Lid lid = p.lid();
        String ps = pad.sleutel();
        ElementSleutel lk = lid.sleutel();
        Class<?> klasse = bonenklasse(pad.subject());
        String naam = "padstand " + pad.subject().naam + "." + String.join(".", pad.rollen()) + " naar "
                + lid.declarant().naam + "." + lid.naam() + ": ";

        Variable s1 = DSL.declarationOf(klasse, "$onderwerp");
        Variable<Object> bereikt = DSL.declarationOf(Object.class, "$bereikt");
        Variable<Object> v1 = DSL.declarationOf(Object.class, "$v");
        PatternDSL.PatternDef bron = ((PatternDSL.PatternDef) PatternDSL.pattern(s1))
                .expr(id("link"), (Predicate1) o -> pad.loop(o) != null)
                .bind(bereikt, o -> pad.loop(o));
        RuleItemBuilder<?> stand;
        if (lid.kenmerk()) {
            Variable<KenmerkStand> st = DSL.declarationOf(KenmerkStand.class, "$stand");
            stand = PatternDSL.pattern(st).expr(id("stand"), x -> x.kenmerk().equals(lk))
                    .expr(id("stand-bereikt"), bereikt, (x, b) -> x.onderwerp() == b).bind(v1, KenmerkStand::waar);
        } else {
            Variable<Stand> st = DSL.declarationOf(Stand.class, "$stand");
            stand = PatternDSL.pattern(st).expr(id("stand"), x -> x.attribuut().equals(lk))
                    .expr(id("stand-bereikt"), bereikt, (x, b) -> x.onderwerp() == b).bind(v1, Stand::waarde);
        }
        Rule aanwezig = PatternDSL.rule(PKG, naam + "link aanwezig").build(bron, stand,
                DSL.on(s1, v1).execute((Block3<org.drools.model.Drools, Object, Object>) (drools, o, v) ->
                        drools.logicalInsert(new PadStand(o, ps, lk, v))));

        Variable s2 = DSL.declarationOf(klasse, "$onderwerp");
        Rule afwezig = PatternDSL.rule(PKG, naam + "link afwezig").build(
                ((PatternDSL.PatternDef) PatternDSL.pattern(s2)).expr(id("geen-link"), (Predicate1) o -> pad.loop(o) == null),
                DSL.on(s2).execute((Block2<org.drools.model.Drools, Object>) (drools, o) ->
                        drools.logicalInsert(new PadStand(o, ps, lk, null))));
        return List.of(aanwezig, afwezig);
    }

    /**
     * The two rules of {@link #padstandregels} for a path with a hop over a relation a form creates: such a relation
     * gains links while the session fires, so the path is a chain of patterns in the left-hand side, a hop over a
     * created relation joining its {@link Koppeling} facts and a hop over an input relation the reference field of the
     * near bean, each ending in a pattern over the reached bean ({@link #keten}). Where the chain reaches an object the
     * standing carries the member's value there, read off that bean where no form writes the member and through its
     * standing where one does; where it does not, the value is null, under a {@code not} over the same chain. A link that
     * a creation adds takes the standing along.
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
        if (schrijvers.containsKey(lk)) {
            Variable<Object> doel = bereikt.getFirstVariable();
            if (lid.kenmerk()) {
                Variable<KenmerkStand> st = DSL.declarationOf(KenmerkStand.class, "$stand");
                items.add(PatternDSL.pattern(st).expr(id("stand"), x -> x.kenmerk().equals(lk))
                        .expr(id("stand-bereikt"), doel, (x, b) -> x.onderwerp() == b).bind(v, KenmerkStand::waar));
            } else {
                Variable<Stand> st = DSL.declarationOf(Stand.class, "$stand");
                items.add(PatternDSL.pattern(st).expr(id("stand"), x -> x.attribuut().equals(lk))
                        .expr(id("stand-bereikt"), doel, (x, b) -> x.onderwerp() == b).bind(v, Stand::waarde));
            }
        } else {
            Function<Object, Object> getter = Accessors.getter(lid.klasse(), lid.naam());
            boolean kenmerk = lid.kenmerk();
            bereikt.bind(v, o -> kenmerk ? Boolean.TRUE.equals(getter.apply(o)) : getter.apply(o));
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
                PatternDSL.PatternDef<Koppeling> kp = PatternDSL.pattern(k)
                        .expr(id("koppeling"), x -> x.relatie().equals(relatie))
                        .expr(id("koppeling-nabij"), n, (x, o) -> (eerste ? x.eerste() : x.tweede()) == o);
                // a creation rule does not walk a link of the phase it fires in: the rule layer has not seen it yet
                if (fase != null) kp.expr(id("koppeling-eerder"), fase, (x, f) -> x.fase() != (Integer) f);
                uit.add(kp.bind(overzijde, x -> eerste ? x.tweede() : x.eerste()));
                p.expr(id("koppeling-ver"), overzijde, (x, o) -> x == o);
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
     *  bean property), whether it is a characteristic, its declared datatype (null for a characteristic), and the bean
     *  class of the declaring type, whose subtypes' beans extend it. */
    private record Lid(ElementSleutel sleutel, Objecttype declarant, String naam, boolean kenmerk, Datatype datatype,
                       Class<?> klasse) {}

    /** Member {@code naam} as declared on {@code houder}'s chain, compared as the verifier compares names. A member on a
     *  timeline or with dimensions is refused. */
    private Lid lid(Objecttype houder, String naam, boolean kenmerk) {
        for (Objecttype t : houder.keten()) {
            if (kenmerk) {
                for (String k : t.kenmerken)
                    if (Namen.norm(k).equals(Namen.norm(naam))) {
                        if (t.kenmerkGranulariteit.containsKey(k))
                            throw new Weigering("het tijdsafhankelijke kenmerk '" + k + "' van " + t.naam + ": tijdlijnen"
                                    + " zijn " + BUITEN);
                        return new Lid(ProducentConsument.elementVan(houder, naam, true), t, k, true, null, bonenklasse(t));
                    }
            } else {
                for (Map.Entry<String, Datatype> e : t.attributen.entrySet())
                    if (Namen.norm(e.getKey()).equals(Namen.norm(naam))) {
                        String a = e.getKey();
                        if (t.attribuutGranulariteit.containsKey(a))
                            throw new Weigering("het tijdsafhankelijke attribuut '" + a + "' van " + t.naam + ": tijdlijnen"
                                    + " zijn " + BUITEN);
                        if (t.attribuutDimensies.containsKey(a))
                            throw new Weigering("het gedimensioneerde attribuut '" + a + "' van " + t.naam + ": " + BUITEN);
                        return new Lid(ProducentConsument.elementVan(houder, naam, false), t, a, false, e.getValue(),
                                bonenklasse(t));
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
                        throw new Weigering("het tijdsafhankelijke attribuut '" + a + "' van " + t.naam + ": tijdlijnen"
                                + " zijn " + BUITEN);
                    return new Lid(ProducentConsument.elementVan(houder, naam, false), t, a, false, e.getValue(),
                            bonenklasse(t));
                }
        throw new Weigering("het attribuut '" + naam + "' is niet gedeclareerd op " + houder.naam + " of een supertype");
    }

    /** A path of to-one roles from a subject type over the reference fields of the beans: the walk
     *  {@link FactModelGenerator#navigatiewandeling} derives from the wire's bindings, a getter per hop, and the type
     *  the last hop reaches. {@code sleutel} names the subject type and the fields, as the {@link PadStand} facts of
     *  the path carry it. */
    private record Pad(String sleutel, Objecttype subject, List<String> rollen, Objecttype bereikt,
                       List<Function<Object, Object>> stappen, List<Hop> hops) {
        /** The object the path reaches from {@code o}, or null where a link on it is absent. Only for a path whose
         *  links are all input ({@link #gecreeerd}). */
        Object loop(Object o) {
            Object d = o;
            for (Function<Object, Object> stap : stappen) {
                d = stap.apply(d);
                if (d == null) return null;
            }
            return d;
        }

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
        List<Function<Object, Object>> stappen = new ArrayList<>();
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
            Function<Object, Object> lees = Accessors.getter(bonenklasse(cur), rol);
            stappen.add(lees);
            hops.add(new Hop(cur, volgend, lees, true, null, false));
            cur = volgend;
        }
        return new Pad(subject.naam + ":" + String.join(".", w.rollen()), subject, w.rollen(), cur, List.copyOf(stappen),
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
            throw new IllegalStateException("geen bonenklasse voor objecttype " + ot.naam, e);
        }
    }

    private String id(String soort) { return soort + "_" + (idSeq++); }
}
