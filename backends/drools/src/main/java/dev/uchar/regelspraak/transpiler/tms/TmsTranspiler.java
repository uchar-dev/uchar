package dev.uchar.regelspraak.transpiler.tms;

import dev.uchar.regelspraak.graaf.ProducentConsument;
import dev.uchar.regelspraak.graaf.ProducentConsument.ElementSleutel;
import dev.uchar.regelspraak.ir.Conditie;
import dev.uchar.regelspraak.ir.Default;
import dev.uchar.regelspraak.ir.Expr;
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
import java.util.ArrayList;
import java.util.Comparator;
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
 * {@code connectingRole} and a fact creation, each over a relation of two roles. Each always valid, with the §6.5
 * deposit of a division by leeg (a default carries no division at all), whose condition, assertion or justification
 * is built from characteristic checks (own or over a path, negated or not), comparisons, leeg and gevuld tests, tests
 * of the channel of a consistency or of a rule that deposits, and {@code Compound}s of those under any quantifier, and
 * whose value is a literal, a read of an attribute (own or over a path of roles), a parameter, an aggregation, or a
 * sum, difference, product or quotient of those, or a date plus or minus a duration. Every other form is in the
 * coverage manifest as not lowered, with a reason that names it, and {@code DroolsBackend} refuses the document on the
 * first such entry.
 *
 * <p>An aggregation over a set source is an accumulate in the left-hand side of the rule that reads it, over the
 * {@link Bijdrage} facts its contribution rules insert logically, one per member of its range ({@link #vouw}); a
 * contribution rule binds the member's value and the reads of the fold's filter in its own left-hand side, so a member
 * whose value or filter reading changes takes its contribution along. A listed fold is computed from its operands'
 * bound values ({@link #gelijsteVouw}).
 *
 * <p>An evaluation fault, a division by a numeric zero in a value or a condition (RUNTIME-SEMANTICS ch. 1.4, point 4),
 * is a {@link Fout} inserted logically and never an exception out of the fire: the branch passes intermediate states
 * the reference interpreter does not see, and a fault thrown there could not be withdrawn when the state passes. The
 * consequence that computes a value inserts the fault instead of the value. A condition is tested in the left-hand
 * side, where Drools cannot take an exception as a fact, so its test answers that it does not hold where it faults,
 * and a rule of the form's own inserts the fault ({@link #voorwaardefoutregel}). A fold's filter that faults for a
 * member inserts no fault but a {@link Filterfout}: the form that reads the fold faults where its own evaluation reaches
 * the fold, as the reference interpreter evaluates the filter only there, with the fault of the first member in fold
 * order whose filter faults, a place the case input states per to-many list ({@link Lijstplaats}) ({@link #vouw}). Only
 * the faults that stand at
 * the fixpoint count: {@link #zetAfgeleideWaarden} reports them before it writes anything back.
 *
 * <p>A creation is the one form whose output is stated, not logical (RUNTIME-SEMANTICS ch. 5.5): a creation fires only
 * at a fixpoint of the rule layer, and what it makes stands, as in the reference interpreter, which keeps a minted
 * instance under a fixed label. A logical object could keep itself standing through its own output after the state that
 * made it had passed, and its initialisations would follow later changes, where ch. 5.5 evaluates them when the object
 * is made. Its rules sit in agenda groups of their own ({@link #FEITCREATIE}, {@link #OBJECTCREATIE}) and the driver
 * fires them in generations ({@code DroolsBackend.vuur}): the rule layer to rest, then the fact creations once and the
 * object creations once, both on that one fixpoint, then the rule layer to rest again, and so on, at most 100
 * generations of one stratum that make something. It does so per evaluation stratum of the reference interpreter, in
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
    private final Map<Expr.Functie, Map<Objecttype, Vouw>> vouwen = new IdentityHashMap<>();
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
            weiger("uniciteit", naamOf(u.naam()), "een Uniqueness: " + BUITEN);
        for (ResolvedVerdeling v : model.verdelingen)
            weiger("verdeling", naamOf(v.naam()), "een Allocation: " + BUITEN);
        for (ResolvedObjectCreatie o : model.objectCreaties)
            regels.addAll(meet("objectcreatie", naamOf(o.naam()), () -> objectCreatie(o)));
        for (ResolvedFeitCreatie f : model.feitCreaties)
            regels.addAll(meet("feitcreatie", naamOf(f.naam()), () -> feitCreatie(f)));
        for (ResolvedUniciteitUnie u : model.uniciteitUnies)
            weiger("uniciteit-unie", naamOf(u.naam()), "een Uniqueness over meer bronnen: " + BUITEN);
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
        if (r.geldigheid() != null && !r.geldigheid().altijd())
            throw new Weigering("een versie met een begrensde geldigheid (Validity met from of to): " + NOG_NIET);
        return switch (r.soort()) {
            case KENMERKTOEKENNING -> kenmerkRegel(r, doel);
            case GELIJKSTELLING, BEREKENING -> rekenRegel(r, doel);
            case INITIALISATIE -> initialisatieRegel(r, doel);
        };
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
        Lezingen lez = new Lezingen(doel.objecttype());
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
                    () -> new Lezingen(doel.objecttype()), r.voorwaarde(), null));
        return uit;
    }

    /**
     * An assignment or a computation of an attribute of the subject: the left-hand side of a characteristic attribution,
     * and a consequence that computes the value from the values it binds and inserts a {@link Waarde} logically when the
     * value is gevuld. A computed leeg writes nothing, so the input on the target stands, or the initialisation beside the
     * rule fills it (the standing norm of RUNTIME-SEMANTICS ch. 7.8 and the desugaring domain of ch. 7.7: the rule is its
     * target's only computing writer, beside at most one unconditional initialisation, which this method demands). A
     * division by a numeric zero is an evaluation fault: the consequence inserts it logically as a {@link Fout} in place
     * of the value, named after the rule. A division of a filled dividend by a leeg divisor inserts the §9.5
     * inconsistency of RUNTIME-SEMANTICS ch. 1.2 from the same consequence, for the divisions the exec-model branch gives
     * a companion (both operands a read of an attribute, of the subject or one role away), under the rule's name: the
     * condition of the rule holds there by construction.
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
            return DSL.not(PatternDSL.pattern(w)
                    .expr(id("andere-schrijver"), x -> x.attribuut().equals(sleutel) && !x.schrijver().equals(naam))
                    .expr(id("andere-schrijver-onderwerp"), lez.s, (x, o) -> x.onderwerp() == o));
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
                                    Function<Lezingen, RuleItemBuilder<?>> blokkade, Set<Class<?>> geabsorbeerd) {
        ElementSleutel sleutel = lid.sleutel();
        Lezingen lez = lezingen(subject, geabsorbeerd);
        Set<Integer> gelezen = lez.volg();
        Toets toets = r.voorwaarde() == null ? null : voorwaarde(r.voorwaarde(), lez);
        boolean kanFalen = lez.kanFalen();
        lez.stop();
        Set<Integer> gebruik = lez.volg();
        Waardefunctie waarde = waarde(r.waarde(), lez);
        List<Deling> delingen = new ArrayList<>();
        delingen(r.waarde(), lez, delingen);
        lez.stop();
        // the write boundary: the value in the target's declared unit (RUNTIME-SEMANTICS ch. 2.4)
        eenheden(eff.eenheidVan(r.waarde()), lid.datatype().eenheidOf(), "een §4 schrijfgrens naar het doel");
        List<RuleItemBuilder<?>> items = lez.linkerkant(kanFalen ? zonderFout(toets) : toets, gelezen, r.voorwaarde());
        if (blokkade != null) items.add(blokkade.apply(lez));
        String naam = r.naam();
        String regelnaam = model.geëmitteerdeRegelNaam(naam);
        int[] posities = posities(gebruik);
        int n = lez.variabelen.size();
        items.add(gevolg(lez, posities, (drools, onderwerp, w) -> {
            Object v;
            try {
                v = collapse(waarde.van(w));
            } catch (Breuk.DelingDoorNul nul) {
                drools.logicalInsert(new Fout(onderwerp, naam, nul.inDeRegel(regelnaam, nul)));
                return;
            }
            for (Deling d : delingen)
                if (w[d.deeltal()] != null && w[d.deler()] == null) {
                    drools.logicalInsert(RegelSpraakRuntime.newInconsistentie(INCONSISTENTIE, naam, onderwerp));
                    break;
                }
            if (v != null) drools.logicalInsert(new Waarde(onderwerp, sleutel, v, naam));
        }, n));
        afgeleid.put(sleutel, lid);
        lez.vastleggen();
        List<Rule> uit = new ArrayList<>();
        uit.add(PatternDSL.rule(PKG, regelnaam).build(items.toArray(new RuleItemBuilder[0])));
        if (kanFalen)
            uit.add(voorwaardefoutregel(naam, regelnaam, () -> lezingen(subject, geabsorbeerd), r.voorwaarde(),
                    blokkade));
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
                                     Function<Lezingen, RuleItemBuilder<?>> blokkade) {
        Lezingen lez = nieuw.get();
        Set<Integer> gelezen = lez.volg();
        Toets toets = voorwaarde(c, lez);
        lez.stop();
        List<RuleItemBuilder<?>> items = lez.linkerkant(w -> fout(toets, w) != null, gelezen, c);
        if (blokkade != null) items.add(blokkade.apply(lez));
        String zinregel = model.geëmitteerdeRegelNaam(vorm);
        items.add(gevolg(lez, posities(gelezen), (drools, onderwerp, w) -> {
            Breuk.DelingDoorNul nul = fout(toets, w);
            if (nul == null)
                throw new IllegalStateException("de voorwaarde van \"" + vorm + "\" faalde in de toets en niet in het"
                        + " gevolg, over dezelfde gebonden waarden");
            drools.logicalInsert(new Fout(onderwerp, vorm, nul.inDeRegel(zinregel, nul)));
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
            } catch (Breuk.DelingDoorNul nul) {
                return false;
            }
        };
    }

    /** The evaluation fault {@code toets} raises over {@code w}, or null where it answers. The one evaluation fault the
     *  forms this branch lowers can raise is a division by a numeric zero (RUNTIME-SEMANTICS ch. 1.4, point 4); any
     *  other exception is a broken contract or a bug and leaves the fire as it is. */
    private static Breuk.DelingDoorNul fout(Toets toets, Object[] w) {
        try {
            toets.test(w);
            return null;
        } catch (Breuk.DelingDoorNul nul) {
            return nul;
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
        if (c.geldigheid() != null && !c.geldigheid().altijd())
            throw new Weigering("een versie met een begrensde geldigheid (Validity met from of to): " + NOG_NIET);
        Lezingen lez = new Lezingen(c.subject());
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
        if (kanFalen)
            uit.add(voorwaardefoutregel(naam, model.geëmitteerdeRegelNaam(naam), () -> new Lezingen(c.subject()),
                    c.assertie(), null));
        return uit;
    }

    /**
     * A test of the §9.5 channel, "regelversie X is inconsistent" (RUNTIME-SEMANTICS ch. 5.6): an existential over the
     * error facts of the form X, with no subject of its own, read through X's {@link KanaalStand}. The producers this
     * branch carries are a consistency and a computing rule or initialisation whose value deposits a §6.5
     * inconsistency ({@link #schrijfregel}); any other name is refused, as the exec-model branch refuses a name that
     * produces nothing it lowers, since a test of a channel nothing fills would never hold and say nothing. "is
     * gevuurd" reads no channel and is refused as well.
     */
    private Toets kanaallezing(Conditie.Regelversie rv, Lezingen lez) {
        if (!rv.inconsistent()) throw nietGelezen("Regelversie 'is gevuurd'", rv, BUITEN);
        String producent = rv.regel();
        if (!kanaalproducent(producent))
            throw new Weigering("een Regelversie 'is inconsistent' over \"" + producent + "\" in de voorwaarde ('"
                    + snippet(rv) + "'): de truth-maintenance-tak draagt als producent van het §9.5-kanaal alleen een"
                    + " Consistency en een rekenende regel of initialisatie met een deling tussen twee gelezen attributen"
                    + " (§6.5)");
        int i = lez.kanaal(producent);
        return w -> Boolean.TRUE.equals(w[i]);
    }

    /** Whether the form named {@code naam}, spelled as the error facts carry it, fills a channel this branch lowers. */
    private boolean kanaalproducent(String naam) {
        if (naam == null) return false;
        for (ResolvedConsistentie c : model.consistenties) if (naam.equals(c.naam())) return true;
        for (ResolvedRegel r : model.resolvedRegels)
            if (naam.equals(r.naam()) && r.soort() != ResolvedRegel.Soort.KENMERKTOEKENNING && deponeert(r.waarde()))
                return true;
        return false;
    }

    /** Whether a value holds a division the §6.5 deposit of {@link #schrijfregel} covers, as {@link #delingen} finds
     *  them. */
    private boolean deponeert(Expr e) {
        if (!(e instanceof Expr.Binair b)) return false;
        if ((b.op() == Expr.BinOp.GEDEELD_DOOR || b.op() == Expr.BinOp.GEDEELD_DOOR_ABS) && telt(b.links())
                && telt(b.rechts()))
            return true;
        return deponeert(b.links()) || deponeert(b.rechts());
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
        if (oc.geldigheid() != null && !oc.geldigheid().altijd())
            throw new Weigering("een versie met een begrensde geldigheid (Validity met from of to): " + NOG_NIET);
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
        Lezingen lez = new Lezingen(subject);
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
            Lid lid = lid(nieuw, t.attribuut(), t.kenmerk());
            Waardefunctie w = waarde(t.waarde(), lez);
            if (!t.kenmerk())
                eenheden(eff.eenheidVan(t.waarde()), lid.datatype().eenheidOf(), "een §4 schrijfgrens naar het lid");
            inits.add(new Initialisatie(lid, w));
        }
        lez.stop();
        int stratum = stratum(oc, naam);
        int fase = lez.creatiefase(stratum);
        gebruik.add(fase);
        List<RuleItemBuilder<?>> items = lez.linkerkant(kanFalen ? zonderFout(toets) : toets, gelezen, oc.voorwaarde(),
                creatiebewaking(naam, lez, fase));
        if (k.eenOpEen()) {
            // an instance linked toward the subject in the input world blocks the creation (ch. 5.5)
            Function<Object, Object> voorwaarts = Accessors.getter(bonenklasse(subject), k.fwdRef());
            lez.onderwerp.expr(id("geen-gekoppelde-instantie"), o -> voorwaarts.apply(o) == null);
        }
        Class<?> klasse = bonenklasse(nieuw);
        items.add(gevolg(lez, posities(gebruik), (drools, onderwerp, w) -> {
            Object[] waarden = new Object[inits.size()];
            for (int i = 0; i < inits.size(); i++) waarden[i] = collapse(inits.get(i).waarde().van(w));
            int f = (Integer) w[fase];
            Object object = nieuweBoon(klasse);
            drools.insert(object);
            drools.insert(new Gemaakt(naam, onderwerp, object, f));
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
        if (kanFalen) uit.add(voorwaardefoutregel(naam, regelnaam, () -> new Lezingen(subject), oc.voorwaarde(), null));
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
        if (fc.geldigheid() != null && !fc.geldigheid().altijd())
            throw new Weigering("een versie met een begrensde geldigheid (Validity met from of to): " + NOG_NIET);
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
        Lezingen lez = new Lezingen(anker);
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
        if (kanFalen) uit.add(voorwaardefoutregel(fc.naam(), regelnaam, () -> new Lezingen(anker), ankerguard, null));
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
                         List<List<Knoop>> leden, int eersteLid) {
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
        for (PrioritizedDefaults l : st.leden())
            if (l.geldigheid() != null && !l.geldigheid().altijd())
                throw new Weigering("een defaultsgroep met een gedateerde geldigheid ('" + naamOf(l.naam()) + "'): "
                        + NOG_NIET);
        Lid lid = lid(eigenaar, doel, kenmerk);
        Objecttype declarant = eigenaar.declarantVan(doel);
        Stapelbouw b = new Stapelbouw(stapelnaam(st), lid, kenmerk, bonenklasse(eigenaar),
                (declarant != null ? declarant.naam : l0.onderwerp()) + "." + doel, st.leden().size());
        List<List<Knoop>> wortel = new ArrayList<>();
        for (int m = 0; m < st.leden().size(); m++)
            wortel.add(keten(b, st.leden().get(m).defaults(), m, null, eigenaar, List.of(),
                    st.leden().size() == 1 ? "" : m + ":"));
        boolean algemeen = wortel.size() > 1 || wortel.stream().flatMap(List::stream).anyMatch(Knoop::heeftLeden);
        List<Rule> regels = algemeen ? algemeneVorm(b, wortel) : eenvoudigeVorm(b, wortel.get(0));
        // registered only once every default of the stack lowered, as a single rule registers its reads
        for (Lezingen lez : b.lezingen) lez.vastleggen();
        afgeleid.put(lid.sleutel(), lid);
        Map<Integer, Plaats> eigenaars = new LinkedHashMap<>();
        for (List<Knoop> keten : wortel) eigenaars(keten, eigenaars);
        defaultsgroepen.add(new Defaultsgroep(b.naam, algemeen, regels.size(), List.copyOf(b.stuurfeiten),
                Map.copyOf(eigenaars)));
        return regels;
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
     *  exception group's defaults on the level of the default that carries them ({@code drager}). */
    private List<Knoop> keten(Stapelbouw b, List<Default> ds, int id, Objecttype drager, Objecttype eigenaar,
                              List<Integer> pad, String voorvoegsel) {
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
                            label + "." + m + "."));
            }
            uit.add(new Knoop(d, niveau, id, i, label, sleutel.toString(), leden, eerste));
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
    }

    /** The snippet of the first division in {@code voorwaarde} or {@code waarde}, or null: in the justification or the
     *  consequence of a default, and in the condition or the value of an initialisation beside another writer, which
     *  ch. 7.7 reads as the lowest default ({@link #initialisatieRegel}). The exec-model branch refuses a division below
     *  the top of a consequence, whose deposit would persist; here the evaluation itself is speculative, as a default is
     *  evaluated whenever its reads stand. A division by zero in a default another one displaces is a fault on an
     *  intermediate state, which this branch would withdraw ({@link Fout}); whether such a fault counts waits on a
     *  decision of the norm, so every division is refused until then. Since none passes, the rules of a default raise
     *  no evaluation fault ({@link #eigenRegel}). */
    private static String deling(Conditie voorwaarde, Expr waarde) {
        String c = voorwaarde == null ? null : deling(voorwaarde);
        return c != null ? c : deling(waarde);
    }

    private static String deling(Conditie c) {
        return switch (c) {
            case Conditie.Vergelijking v -> deling(v.links()) != null ? deling(v.links()) : deling(v.rechts());
            case Conditie.Predicaat p -> deling(p.operand());
            case Conditie.Samengesteld s -> s.onderdelen().stream().map(TmsTranspiler::deling)
                    .filter(Objects::nonNull).findFirst().orElse(null);
            default -> null;
        };
    }

    private static String deling(Expr e) {
        if (e instanceof Expr.Functie f) {
            for (Expr a : f.argumenten()) {
                String d = deling(a);
                if (d != null) return d;
            }
            String d = f.leegwaarde() == null ? null : deling(f.leegwaarde());
            return d != null || f.filter() == null ? d : deling(f.filter());
        }
        if (e instanceof Expr.InstanceSetUnion u) {
            for (Expr.InstanceSetUnion.Tak t : u.takken()) {
                String d = t.filter() == null ? null : deling(t.filter());
                if (d != null) return d;
            }
            return null;
        }
        if (!(e instanceof Expr.Binair b)) return null;
        if (b.op() == Expr.BinOp.GEDEELD_DOOR || b.op() == Expr.BinOp.GEDEELD_DOOR_ABS) return snippet(b.bron());
        String l = deling(b.links());
        return l != null ? l : deling(b.rechts());
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
            uit.add(eigenRegel(b, k, naam + ": default " + k.label(),
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
                uit.add(eigenRegel(b, k, naam + ": default " + k.label(), nietHoger, beslist));
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
                uit.add(eigenRegel(b, k, naam + ": terugval " + k.label(), o -> {
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
     * value computed from what the rule bound, and nothing where that value is leeg (ch. 7.1). A default carries no
     * division ({@link #controleer}), so neither its test nor its consequence raises an evaluation fault, and neither
     * catches one as a rule does ({@link #schrijfregel}, {@link #voorwaardefoutregel}).
     */
    private Rule eigenRegel(Stapelbouw b, Knoop k, String regelnaam,
                            Function<Variable<Object>, List<RuleItemBuilder<?>>> wacht, Afloop besluit) {
        Default d = k.d();
        Lezingen lez = new Lezingen(k.niveau());
        Class<?> klasse = b.klasse;
        lez.onderwerp.expr(id("concreet-type"), o -> o.getClass() == klasse);
        if (d.initialisatie()) {
            Function<Object, Object> invoer = Accessors.getter(b.lid.klasse(), b.lid.naam());
            lez.onderwerp.expr(id("doel-leeg-in-de-invoer"), o -> invoer.apply(o) == null);
        }
        Set<Integer> gelezen = lez.volg();
        Toets toets = d.voorwaarde() == null ? null : voorwaarde(d.voorwaarde(), lez);
        lez.stop();
        List<RuleItemBuilder<?>> items;
        if (b.kenmerk) {
            items = lez.linkerkant(toets, gelezen, d.voorwaarde(), wacht.apply(lez.s));
            Boolean beslissing = d.besluit();
            items.add(DSL.on(lez.s).execute((Block2<org.drools.model.Drools, Object>) (drools, o) ->
                    besluit.voer(drools, o, beslissing)));
        } else {
            Set<Integer> gebruik = lez.volg();
            Waardefunctie waarde = waarde(d.consequentie(), lez);
            lez.stop();
            eenheden(eff.eenheidVan(d.consequentie()), b.lid.datatype().eenheidOf(), "een §4 schrijfgrens naar het doel");
            items = lez.linkerkant(toets, gelezen, d.voorwaarde(), wacht.apply(lez.s));
            int n = lez.variabelen.size();
            items.add(gevolg(lez, posities(gebruik), (drools, o, w) -> {
                Object v = collapse(waarde.van(w));
                if (v != null) besluit.voer(drools, o, v);
            }, n));
        }
        b.lezingen.add(lez);
        return PatternDSL.rule(PKG, regelnaam).build(items.toArray(new RuleItemBuilder[0]));
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
            case Expr.TekstReeks t -> throw nietBerekend("TextComposition", t.bron(), NOG_NIET);
            case Expr.Functie f -> functie(f, lez);
            case Expr.InstanceSetUnion u -> throw nietBerekend("InstanceSetUnion", u.bron(), BUITEN);
            case Expr.Afgerond a -> throw nietBerekend("Rounding", a.bron(), NOG_NIET);
            case Expr.Begrensd b -> throw nietBerekend("Clamp", b.bron(), NOG_NIET);
            case Expr.DimensieCel c -> throw nietBerekend("DimensionCellRead", c.bron(), BUITEN);
            case Expr.VariabeleRef v -> throw nietBerekend("VariableReference", v.bron(), BUITEN);
            case Expr.Let l -> throw nietBerekend("Let", l.bron(), BUITEN);
            case Expr.BinderRef b -> throw nietBerekend("BinderRef", b.bron(), BUITEN);
            case Expr.IfThenElse i -> throw nietBerekend("IfThenElse", i.bron(), BUITEN);
            case Expr.Constante c -> throw nietBerekend("contextwaarde (" + c.naam() + ")", c.bron(), BUITEN);
            case Expr.Onopgelost o -> throw nietBerekend("Unresolved", o.bron(), BUITEN);
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

    /** A literal as the exec-model branch reads it: a number exact where its quotient does not terminate, a date, a
     *  boolean, and a text or an enumeration label as its string. */
    private static Waardefunctie literaal(Expr.Literal l) {
        Datatype t = l.type();
        if (t instanceof Datatype.Numeriek) {
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
            java.time.LocalDate v = Literalen.parseDatum(l.waarde());
            if (v == null) throw new Weigering("een datum die niet te lezen is ('" + l.waarde() + "')");
            return w -> v;
        }
        if (t instanceof Datatype.DatumTijd) {
            java.time.LocalDateTime v = Literalen.parseDatumTijd(l.waarde());
            if (v == null) throw new Weigering("een datum-tijd die niet te lezen is ('" + l.waarde() + "')");
            return w -> v;
        }
        if (t instanceof Datatype.Percentage)
            throw new Weigering("een Literal van een percentage ('" + l.waarde() + "'): " + NOG_NIET);
        String label = Literalen.tekstOfEnumLabel(l);
        if (label == null) throw new Weigering("een Literal van type " + t + ": " + NOG_NIET);
        return w -> label;
    }

    /** The five operators of the proef on numbers, through the runtime's exact helpers and with their leeg tables
     *  (RUNTIME-SEMANTICS ch. 1.2); a date plus or minus a duration through {@link #datumrekening}. */
    private Waardefunctie binair(Expr.Binair b, Lezingen lez) {
        Datatype lt = eff.typeVan(b.links()), rt = eff.typeVan(b.rechts());
        if (lt.isDatum() || rt.isDatum()) return datumrekening(b, lez);
        if (lt.isPercentage() || rt.isPercentage())
            throw new Weigering("een Binary over een percentage ('" + snippet(b.bron()) + "'): " + NOG_NIET);
        Expr.BinOp op = b.op();
        switch (op) {
            case PLUS, MIN, VERMINDERD_MET, MAAL, GEDEELD_DOOR, GEDEELD_DOOR_ABS -> { }
            default -> throw new Weigering("een Binary " + Spelling.BINARY_OP.spelling(op) + " ('" + snippet(b.bron())
                    + "'): " + NOG_NIET);
        }
        // a division faults on a numeric zero (RUNTIME-SEMANTICS ch. 1.4, point 4): a condition that holds one carries the
        // rule that inserts its fault (voorwaardefoutregel)
        if (op == Expr.BinOp.GEDEELD_DOOR || op == Expr.BinOp.GEDEELD_DOOR_ABS) lez.markeerFalen();
        Waardefunctie l = waarde(b.links(), lez), r = waarde(b.rechts(), lez);
        // a sum or a difference restates its right operand into the left one's unit; a product and a quotient do not
        if (op == Expr.BinOp.PLUS || op == Expr.BinOp.MIN || op == Expr.BinOp.VERMINDERD_MET)
            eenheden(eff.eenheidVan(b.rechts()), eff.eenheidVan(b.links()), "een additieve operandgrens");
        return switch (op) {
            case PLUS -> w -> RegelSpraakRuntime.rsPlus(l.van(w), r.van(w));
            case MIN -> w -> RegelSpraakRuntime.rsMin(l.van(w), r.van(w));
            case VERMINDERD_MET -> w -> RegelSpraakRuntime.rsVerminderd(l.van(w), r.van(w));
            case MAAL -> w -> RegelSpraakRuntime.rsMaal(l.van(w), r.van(w));
            case GEDEELD_DOOR -> w -> RegelSpraakRuntime.rsGedeeld(l.van(w), r.van(w));
            case GEDEELD_DOOR_ABS -> w -> RegelSpraakRuntime.rsGedeeldAbs(l.van(w), r.van(w));
            default -> throw new IllegalStateException("operator " + op + " na de controle hierboven");
        };
    }

    /** A date plus or minus a duration with a calendar or clock unit, as the exec-model branch computes it
     *  ({@code datumRekenWaarde}): no date minus date, no duration minus a date, a clock unit only on a date-time. A
     *  duration counted in a day kind is refused: day kinds are outside the proef. */
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
        Waardefunctie d = waarde(datumExpr, lez), n = waarde(duurExpr, lez);
        return w -> RegelSpraakRuntime.rsDatumReken((java.time.temporal.Temporal) d.van(w), n.van(w),
                eenheid, optellen);
    }

    /** A restatement between two units is not built in this branch: two units that need one are refused, and two that
     *  cannot be compared at all fault as at the exec-model branch ({@link Eenheidsconflict}). */
    private static void eenheden(Eenheid van, Eenheid naar, String plek) {
        Eenheidsconflict.eisHerschaalbaar(van, naar, plek);
        if (van != null && naar != null && van.convertibleNaar(naar) && van.vereistOmrekening(naar))
            throw new Weigering(plek + " over eenheden die herrekend moeten worden (" + van.afkorting() + " naar "
                    + naar.afkorting() + "): " + NOG_NIET);
    }

    // ---------------- aggregations ----------------

    /** A fold this branch lowers: the key its contributions carry ({@link Bijdrage#vouw}), its contribution rules (one
     *  per range, beside the filter fault rule of a filter that can fault), the reads of those rules, registered when a
     *  form that reads the fold lowers ({@link Lezingen#vastleggen}), and whether its filter can fault, in which case
     *  the rule that reads it binds its {@link Filterfout} facts as well. */
    private record Vouw(String sleutel, List<Rule> regels, List<Lezingen> lezingen, boolean kanFalen) {}

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

    /** An aggregation, listed or over a set source; every other function node is a builtin, which this branch does not
     *  compute yet, and so is a fold over a dimension selection, which reads a dimensioned attribute. */
    private Waardefunctie functie(Expr.Functie f, Lezingen lez) {
        if (!isVouw(f.kind())) throw nietBerekend("BuiltinCall", f.bron(), BUITEN);
        if (f.dimensieSelectie() != null)
            throw nietBerekend("Aggregation over een dimensieselectie", f.bron(), "gedimensioneerde attributen zijn " + BUITEN);
        return isGelisteVouw(f) ? gelijsteVouw(f, lez) : vouw(f, lez);
    }

    /**
     * A listed fold (RUNTIME-SEMANTICS ch. 1.2e): minimum and eerste keep the least operand value, maximum and laatste
     * the greatest; a leeg operand is skipped, an all-leeg fold is leeg, and a tie takes the later operand. The operands
     * are computed from the values the rule binds, in wire order; an operand that would need a restatement into the
     * first one's unit is refused, as everywhere in this branch.
     */
    private Waardefunctie gelijsteVouw(Expr.Functie f, Lezingen lez) {
        boolean kleinste = f.kind() == Expr.FunctieKind.MINIMUM || f.kind() == Expr.FunctieKind.EERSTE;
        Eenheid eerste = eff.eenheidVan(f.argumenten().get(0));
        List<Waardefunctie> operanden = new ArrayList<>();
        for (int i = 0; i < f.argumenten().size(); i++) {
            Expr a = f.argumenten().get(i);
            if (i > 0) eenheden(eff.eenheidVan(a), eerste, "een §5.8.3 operand van een gelijste vouw");
            operanden.add(waarde(a, lez));
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
     * only where its evaluation reaches the fold.
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
            if (!v.kanFalen()) return w -> aantal(w[i[0]]);
            lez.markeerFalen();
            return w -> {
                filterfout(w[i[2]]);
                return aantal(w[i[0]]);
            };
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
            return new Vouw(sleutel, List.copyOf(regels), List.copyOf(lezingen), kanFalen);
        });
        if (arg != null && f.leegwaarde() != null)
            eenheden(eff.eenheidVan(f.leegwaarde()), eff.eenheidVan(arg), "een §5.8.2 terugvalwaarde");
        Class<?> functie = switch (kind) {
            case SOM, TOTAAL -> accumulatiefunctie("BigDecimalSumAccumulateFunction");
            case MINIMUM, EERSTE -> accumulatiefunctie("MinAccumulateFunction");
            case MAXIMUM, LAATSTE -> accumulatiefunctie("MaxAccumulateFunction");
            default -> null;
        };
        int[] i = lez.vouw(v, functie);
        if (v.kanFalen()) lez.markeerFalen();
        Waardefunctie terugval = f.leegwaarde() == null ? null : waarde(f.leegwaarde(), lez);
        Waardefunctie uitkomst = switch (kind) {
            case AANTAL, AANTAL_GEVULDE -> w -> aantal(w[i[0]]);
            case SOM -> w -> {
                Object t = terugval == null ? null : terugval.van(w);
                return leeg(w[i[0]]) ? t : w[i[1]];
            };
            case TOTAAL -> w -> leeg(w[i[0]]) ? BigDecimal.ZERO : w[i[1]];
            default -> w -> leeg(w[i[0]]) ? null : w[i[1]];
        };
        if (!v.kanFalen()) return uitkomst;
        return w -> {
            filterfout(w[i[2]]);
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

    /** The fold of {@code f} on {@code subject}, built by {@code bouw} from its key and its name the first time it is
     *  read. */
    private Vouw vouwVoor(Expr.Functie f, Objecttype subject, BiFunction<String, String, Vouw> bouw) {
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
            boolean kanFalen = false;
            for (int k = 0; k < u.takken().size(); k++) {
                Expr.InstanceSetUnion.Tak t = u.takken().get(k);
                if (t.elementType() == null)
                    throw new Weigering("een selectie zonder elementtype in een InstanceSetUnion ('" + snippet(t.bron()) + "')");
                kanFalen |= bijdrageregels(sleutel, naam + ", selectie " + k, Expr.FunctieKind.AANTAL, subject,
                        t.elementType(), eenstapsbereik(t.bronRol(), subject, t.elementType(), snippet(t.bron())), k,
                        t.filter(), null, regels, lezingen);
            }
            return new Vouw(sleutel, List.copyOf(regels), List.copyOf(lezingen), kanFalen);
        });
    }

    /**
     * The contribution rule of one range of the fold {@code sleutel}: the fold's subject bound first, the member's
     * pattern joined to it through {@code bereik}, the member's reads, the {@code filter} as the test (the member is the
     * filter's subject, RUNTIME-SEMANTICS ch. 3.4a), and a consequence that inserts the member's {@link Bijdrage}
     * logically: for a count the member; for a filled count the member where its value is gevuld; for the other folds
     * the member's value where it is gevuld, since a leeg member is skipped (ch. 1.2). Beside it, where the filter can
     * fault, the rule that inserts the member's {@link Filterfout} ({@link #filterfoutregel}); {@code selectie} is the
     * number of the selection of an instance-set union, 0 for any other fold. The rules go into {@code uit} and their
     * reads into {@code lezingen}, to be registered with the form; returns whether the filter can fault.
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
        return kanFalen;
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
            Breuk.DelingDoorNul nul = fout(toets, w);
            if (nul == null)
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
            drools.logicalInsert(new Filterfout(sleutel, onderwerp, w[lid], nul, plaats));
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

    /** A division whose dividend and divisor are bound reads: the indices of the two. */
    private record Deling(int deeltal, int deler) {}

    /** The divisions in a value the exec-model branch gives a §6.5 companion ({@code delingCompanions}): each operand a
     *  read of an attribute, of the subject or one role away; a literal, a parameter, a nested expression and a read
     *  over two or more roles do not count there, and so not here. */
    private void delingen(Expr e, Lezingen lez, List<Deling> uit) {
        if (!(e instanceof Expr.Binair b)) return;
        if ((b.op() == Expr.BinOp.GEDEELD_DOOR || b.op() == Expr.BinOp.GEDEELD_DOOR_ABS)
                && telt(b.links()) && telt(b.rechts()))
            uit.add(new Deling(attribuutlezing((Expr.AttribuutRef) b.links(), lez),
                    attribuutlezing((Expr.AttribuutRef) b.rechts(), lez)));
        delingen(b.links(), lez, uit);
        delingen(b.rechts(), lez, uit);
    }

    private boolean telt(Expr e) {
        if (!(e instanceof Expr.AttribuutRef a)) return false;
        List<RolBindingRef> pad = navigatieBindingen.get(a);
        return pad == null || pad.size() == 1;
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
            case Conditie.Periode p -> throw nietGelezen("TimeWindow", c, BUITEN);
            case Conditie.Kwantorvouw q -> throw nietGelezen("QuantifierFold", c, BUITEN);
            case Conditie.Rollidmaatschap rol -> throw nietGelezen("RoleMembership", c, BUITEN);
            case Conditie.VerreRollidmaatschap rol -> throw nietGelezen("RoleMembership", c, BUITEN);
            case Conditie.Regelversie rv -> kanaallezing(rv, lez);
            case Conditie.Onopgelost o -> throw nietGelezen("Unresolved", c, BUITEN);
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

    /** A composite folds its members in wire order and stops at the first member after which the verdict stands
     *  (RUNTIME-SEMANTICS ch. 5.10), so a member that would fault, a comparison with a division by zero, is not read
     *  where an earlier member settled the verdict, and the composite faults only where it reads such a member. */
    private Toets samengesteld(Conditie.Samengesteld s, Lezingen lez) {
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
     * A comparison of two values the rule binds or computes, with the empty operand of RUNTIME-SEMANTICS ch. 1.2 as the
     * exec-model branch reads it: numbers through {@code rsAggVergelijk} (an ordering with a leeg operand is onwaar, two
     * leeg numbers are equal, exactly one leeg is unequal; exact where a value is a rational); dates of one precision
     * through {@link #datumVergelijk} (two leeg dates are not equal); a text, an enumeration label or a truth value
     * through {@link #tekstGelijk}, an enumeration up to its normalised label. Units that need a restatement are refused.
     */
    private Toets vergelijking(Conditie.Vergelijking v, Lezingen lez) {
        if (v.periode() != null)
            throw new Weigering("een Comparison met wholePeriod ('" + snippet(v) + "'): tijdlijnen zijn " + BUITEN);
        Datatype lt = eff.typeVan(v.links()), rt = eff.typeVan(v.rechts());
        Conditie.Operator op = v.op();
        Waardefunctie l = waarde(v.links(), lez), r = waarde(v.rechts(), lez);
        if (lt.isNumeriek() && rt.isNumeriek() && !isDatumOp(op)) {
            eenheden(eff.eenheidVan(v.links()), eff.eenheidVan(v.rechts()), "een §5.1 vergelijking");
            String symbool = Operatoren.symbool(op);
            return w -> RegelSpraakRuntime.rsAggVergelijk(l.van(w), r.van(w), symbool);
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
                + typenaam(rt) + " ('" + snippet(v) + "'): " + NOG_NIET);
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
        if (p.soort() != Conditie.Predicaatsoort.LEEG && p.soort() != Conditie.Predicaatsoort.GEVULD)
            throw new Weigering("een Predicate " + Spelling.PREDICATE_KIND.spelling(p.soort()) + " ('" + snippet(p)
                    + "'): " + NOG_NIET);
        Waardefunctie f = waarde(p.operand(), lez);
        boolean leeg = p.soort() == Conditie.Predicaatsoort.LEEG;
        return w -> (f.van(w) == null) == leeg;
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
         * values with it; and where the fold's filter can fault, a second accumulate over its {@link Filterfout} facts
         * on this subject, which binds the least of them, the first in fold order, or null. Returns the index of the count, that of the folded
         * value and that of the least filter fault, each -1 without one. An accumulate over no fact still binds a row,
         * through the count, so the rule reads an empty fold, and a fold without a filter fault, as well.
         */
        int[] vouw(Vouw v, Class<?> functie) {
            vouwen.add(v);
            String sleutel = v.sleutel();
            int bekend = bestaand("vouw " + sleutel);
            if (bekend >= 0) return new int[]{bekend, functie == null ? -1 : bestaand("vouwwaarde " + sleutel),
                    v.kanFalen() ? bestaand("filterfout " + sleutel) : -1};
            Variable<Bijdrage> b = DSL.declarationOf(Bijdrage.class, "$bijdrage_" + id("vouw"));
            PatternDSL.PatternDef<Bijdrage> p = PatternDSL.pattern(b).expr(id("vouw"), x -> x.vouw().equals(sleutel))
                    .expr(id("vouw-onderwerp"), s, (x, o) -> x.onderwerp() == o);
            Variable<Object> aantal = DSL.declarationOf(Object.class, "$aantal_" + id("vouw"));
            int[] uit;
            if (functie == null) {
                standen.add(DSL.accumulate(p, DSL.accFunction(accumulatiefunctie("CountAccumulateFunction"), b)
                        .as(aantal)));
                uit = new int[]{nieuw("vouw " + sleutel, aantal), -1, -1};
            } else {
                Variable<Object> bijdragewaarde = DSL.declarationOf(Object.class, "$bijdragewaarde_" + id("vouw"));
                Variable<Object> uitkomst = DSL.declarationOf(Object.class, "$vouw_" + id("vouw"));
                p.bind(bijdragewaarde, Bijdrage::waarde);
                standen.add(DSL.accumulate(p, DSL.accFunction(accumulatiefunctie("CountAccumulateFunction"), b)
                        .as(aantal), DSL.accFunction(functie, bijdragewaarde).as(uitkomst)));
                uit = new int[]{nieuw("vouw " + sleutel, aantal), nieuw("vouwwaarde " + sleutel, uitkomst), -1};
            }
            if (!v.kanFalen()) return uit;
            Variable<Filterfout> f = DSL.declarationOf(Filterfout.class, "$filterfout_" + id("vouw"));
            PatternDSL.PatternDef<Filterfout> q = PatternDSL.pattern(f)
                    .expr(id("filterfout"), x -> x.vouw().equals(sleutel))
                    .expr(id("filterfout-onderwerp"), s, (x, o) -> x.onderwerp() == o);
            Variable<Object> fouten = DSL.declarationOf(Object.class, "$filterfouten_" + id("vouw"));
            Variable<Object> eerste = DSL.declarationOf(Object.class, "$eerste_filterfout_" + id("vouw"));
            standen.add(DSL.accumulate(q, DSL.accFunction(accumulatiefunctie("CountAccumulateFunction"), f).as(fouten),
                    DSL.accFunction(accumulatiefunctie("MinAccumulateFunction"), f).as(eerste)));
            uit[2] = nieuw("filterfout " + sleutel, eerste);
            return uit;
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
