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
 * consistency, whose violation is a logical §9.5 inconsistency; and a prioritized-defaults group, as the effective
 * stack per concrete type of ch. 7.10, which decides by logical control facts ({@link Stuurfeit}) and into which an
 * ordinary rule on an ancestor's level folds, so that it does not fire on that type. Each always valid, with the §6.5
 * deposit of a division by leeg (a default carries no division at all), whose condition, assertion or justification
 * is built from characteristic checks (own or over a path, negated or not), comparisons, leeg and gevuld tests, tests
 * of the channel of a consistency or of a rule that deposits, and {@code Compound}s of those under any quantifier, and
 * whose value is a literal, a read of an attribute (own or over a path of roles), a parameter, or a sum, difference,
 * product or quotient of those, or a date plus or minus a duration. Every other form is in the coverage
 * manifest as not lowered, with a reason that names it, and {@code DroolsBackend} refuses the document on the first
 * such entry.
 */
public final class TmsTranspiler {

    /** The Drools package of this branch's rules. The rule of a form carries the form's name, as in the exec-model
     *  branch, so a consumer that loads models of two branches side by side keeps them apart by package. */
    public static final String PKG = "dev.uchar.regelspraak.tms";

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
    /** Unique per constraint across the model: the executable model shares a constraint between rules by its id, so
     *  two different tests under one id would collapse into the first. */
    private int idSeq;
    private Model model0;

    private TmsTranspiler(RegelSpraakModel model, ClassLoader beanLoader, List<PrioritizedDefaults> defaults,
                          Map<Object, List<RolBindingRef>> navigatieBindingen, EffectieveEenheid eff) {
        this.model = model;
        this.beanLoader = beanLoader != null ? beanLoader : TmsTranspiler.class.getClassLoader();
        this.defaults = defaults;
        this.navigatieBindingen = navigatieBindingen;
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
        TmsTranspiler t = new TmsTranspiler(m, beanLoader, defaults, nav, new EffectieveEenheid(lets));
        t.model0 = t.run();
        return t;
    }

    public Model model() { return model0; }

    /** The coverage manifest: one verdict per form, the rules in document order, then every other family. */
    public List<Dekking> dekking() { return dekking; }

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
     * <p>Two different values on one attribute of one object have no common fixpoint (RUNTIME-SEMANTICS ch. 5.7), and
     * this does not choose between them: it fails, naming both writers and both values. Equal values are no conflict.
     */
    public void zetAfgeleideWaarden(KieSession sessie) {
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
            regels.addAll(meet("consistentie", naamOf(c.naam()), () -> List.of(consistentieRegel(c))));
        for (ResolvedUniciteit u : model.uniciteiten)
            weiger("uniciteit", naamOf(u.naam()), "een Uniqueness: " + BUITEN);
        for (ResolvedVerdeling v : model.verdelingen)
            weiger("verdeling", naamOf(v.naam()), "een Allocation: " + BUITEN);
        for (ResolvedObjectCreatie o : model.objectCreaties)
            weiger("objectcreatie", naamOf(o.naam()), "een ObjectCreation: " + BUITEN);
        for (ResolvedFeitCreatie f : model.feitCreaties)
            weiger("feitcreatie", naamOf(f.naam()), "een FactCreation: " + BUITEN);
        for (ResolvedUniciteitUnie u : model.uniciteitUnies)
            weiger("uniciteit-unie", naamOf(u.naam()), "een Uniqueness over meer bronnen: " + BUITEN);
        if (samenstelling != null) weiger("defaults", "effectieve stapels", samenstelling);
        for (Stapels.Stapel st : effectief) regels.addAll(meet("defaults", stapelnaam(st), () -> stapelregels(st)));
        for (ResolvedStartpunt sp : model.resolvedStartpunten)
            weiger("startpuntbepaling", sp.tijdlijn(), "een StartPointDetermination: tijdlijnen zijn " + BUITEN);
        for (ResolvedDagsoort d : model.resolvedDagsoorten)
            weiger("dagsoortdefinitie", d.naam(), "een DayKindDefinition: dagsoorten zijn " + BUITEN);
        // a standing is built once per member, for every rule that reads it, and only for one a lowered rule reads: a
        // standing nothing reads would only add firings
        List<Rule> standen = new ArrayList<>();
        for (Padlezing p : padlezingen.values()) standen.addAll(padstandregels(p));
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
        return List.of(switch (r.soort()) {
            case KENMERKTOEKENNING -> kenmerkRegel(r, doel);
            case GELIJKSTELLING, BEREKENING -> rekenRegel(r, doel);
            case INITIALISATIE -> initialisatieRegel(r, doel);
        });
    }

    /**
     * A characteristic attribution: the subject pattern with the reads its condition binds there, the standing patterns
     * of what it reads elsewhere, one test over every value its condition reads, and a consequence that inserts the
     * {@link HeeftKenmerk} logically. Its justification is the whole match, so a standing the condition read that is
     * later retracted takes the characteristic with it, also when the condition read it negated.
     */
    private Rule kenmerkRegel(ResolvedRegel r, Expr.AttribuutRef doel) {
        // the loader refuses any other value (WF-022); asked again because this rule writes true whatever it says
        if (!(r.waarde() instanceof Expr.Literal l && "waar".equals(l.waarde()) && l.type() instanceof Datatype.Booleaan))
            throw new Weigering("een kenmerktoekenning met een andere waarde dan de constante waar (WF-022)");
        Lid lid = lid(doel.objecttype(), doel.attribuut(), true);
        Lezingen lez = new Lezingen(doel.objecttype());
        Set<Integer> gelezen = lez.volg();
        Toets toets = r.voorwaarde() == null ? null : voorwaarde(r.voorwaarde(), lez);
        lez.stop();
        List<RuleItemBuilder<?>> items = lez.linkerkant(toets, gelezen, r.voorwaarde());
        ElementSleutel sleutel = lid.sleutel();
        items.add(DSL.on(lez.s).execute((Block2<org.drools.model.Drools, Object>) (drools, o) ->
                drools.logicalInsert(new HeeftKenmerk(o, sleutel))));
        afgeleid.put(sleutel, lid);
        lez.vastleggen();
        return PatternDSL.rule(PKG, model.geëmitteerdeRegelNaam(r.naam())).build(items.toArray(new RuleItemBuilder[0]));
    }

    /**
     * An assignment or a computation of an attribute of the subject: the left-hand side of a characteristic attribution,
     * and a consequence that computes the value from the values it binds and inserts a {@link Waarde} logically when the
     * value is gevuld. A computed leeg writes nothing, so the input on the target stands, or the initialisation beside the
     * rule fills it (the standing norm of RUNTIME-SEMANTICS ch. 7.8 and the desugaring domain of ch. 7.7: the rule is its
     * target's only computing writer, beside at most one unconditional initialisation, which this method demands). A
     * division by a numeric zero leaves the consequence as the fault it is, a {@code ConsequenceException} that names the
     * rule. A division of a filled dividend by a leeg divisor inserts the §9.5 inconsistency of RUNTIME-SEMANTICS ch. 1.2
     * from the same consequence, for the divisions the exec-model branch gives a companion (both operands a read of an
     * attribute, of the subject or one role away), under the rule's name: the condition of the rule holds there by
     * construction.
     *
     * <p>That deposit is a logical fact like the value: it stands while the activation that made it stands, and goes when
     * a read it rested on changes. The reference interpreter keeps a deposit made on a transient value (RUNTIME-SEMANTICS
     * ch. 6.1, "Recorded residuals"), this branch withdraws it; recorded as an observation.
     */
    private Rule rekenRegel(ResolvedRegel r, Expr.AttribuutRef doel) {
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
     * writer displaces it later, so a division in its condition or its value is refused there, with the predicate of a
     * division in a default: a division by zero would fail where the reference interpreter never evaluates it
     * ({@code TmsInitialisatieWitnessTest}). Without another writer nothing displaces it and the division stays carried.
     */
    private Rule initialisatieRegel(ResolvedRegel r, Expr.AttribuutRef doel) {
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
                    + " deling door nul geeft daar een fout waar de referentie, die alleen de beslissende default"
                    + " uitrekent, geen geeft (speculatieve evaluatie, ch. 7.1)");
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
     * gevuld, beside the §6.5 deposit {@link #rekenRegel} describes. Objects of a class in {@code geabsorbeerd} do not
     * match: an effective stack writes the target there ({@link #geabsorbeerd}).
     */
    private Rule schrijfregel(ResolvedRegel r, Objecttype subject, Lid lid,
                              Function<Lezingen, RuleItemBuilder<?>> blokkade, Set<Class<?>> geabsorbeerd) {
        ElementSleutel sleutel = lid.sleutel();
        Lezingen lez = new Lezingen(subject);
        if (!geabsorbeerd.isEmpty())
            lez.onderwerp.expr(id("niet-in-een-stapel"), o -> !geabsorbeerd.contains(o.getClass()));
        Set<Integer> gelezen = lez.volg();
        Toets toets = r.voorwaarde() == null ? null : voorwaarde(r.voorwaarde(), lez);
        lez.stop();
        Set<Integer> gebruik = lez.volg();
        Waardefunctie waarde = waarde(r.waarde(), lez);
        List<Deling> delingen = new ArrayList<>();
        delingen(r.waarde(), lez, delingen);
        lez.stop();
        // the write boundary: the value in the target's declared unit (RUNTIME-SEMANTICS ch. 2.4)
        eenheden(eff.eenheidVan(r.waarde()), lid.datatype().eenheidOf(), "een §4 schrijfgrens naar het doel");
        List<RuleItemBuilder<?>> items = lez.linkerkant(toets, gelezen, r.voorwaarde());
        if (blokkade != null) items.add(blokkade.apply(lez));
        String naam = r.naam();
        int[] posities = posities(gebruik);
        int n = lez.variabelen.size();
        items.add(gevolg(lez, posities, (drools, onderwerp, w) -> {
            Object v = collapse(waarde.van(w));
            for (Deling d : delingen)
                if (w[d.deeltal()] != null && w[d.deler()] == null) {
                    drools.logicalInsert(RegelSpraakRuntime.newInconsistentie(INCONSISTENTIE, naam, onderwerp));
                    break;
                }
            if (v != null) drools.logicalInsert(new Waarde(onderwerp, sleutel, v, naam));
        }, n));
        afgeleid.put(sleutel, lid);
        lez.vastleggen();
        return PatternDSL.rule(PKG, model.geëmitteerdeRegelNaam(r.naam())).build(items.toArray(new RuleItemBuilder[0]));
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
     * never withdraws one; the fixpoint is the same.
     */
    private Rule consistentieRegel(ResolvedConsistentie c) {
        // the rule is named by the form, and the channel test reads the error facts by that name
        if (c.naam() == null) throw new Weigering("een consistentie zonder naam (WF-107)");
        if (c.subject() == null || c.assertie() == null) throw new Weigering("een consistentie zonder subject of assertie");
        if (c.geldigheid() != null && !c.geldigheid().altijd())
            throw new Weigering("een versie met een begrensde geldigheid (Validity met from of to): " + NOG_NIET);
        Lezingen lez = new Lezingen(c.subject());
        Set<Integer> gelezen = lez.volg();
        Toets houdt = voorwaarde(c.assertie(), lez);
        lez.stop();
        List<RuleItemBuilder<?>> items = lez.linkerkant(w -> !houdt.test(w), gelezen, c.assertie());
        String naam = c.naam();
        items.add(DSL.on(lez.s).execute((Block2<org.drools.model.Drools, Object>) (drools, o) ->
                drools.logicalInsert(RegelSpraakRuntime.newInconsistentie(INCONSISTENTIE, naam, o))));
        lez.vastleggen();
        return PatternDSL.rule(PKG, model.geëmitteerdeRegelNaam(naam)).build(items.toArray(new RuleItemBuilder[0]));
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
                    + " hij leest staat, ook een die een hogere default verdringt, en een deling door nul geeft daar een"
                    + " fout waar de referentie, die alleen tot de beslissende default komt, geen geeft (RUNTIME-SEMANTICS"
                    + " ch. 7.1, speculatieve evaluatie)");
    }

    /** The snippet of the first division in {@code voorwaarde} or {@code waarde}, or null: in the justification or the
     *  consequence of a default, and in the condition or the value of an initialisation beside another writer, which
     *  ch. 7.7 reads as the lowest default ({@link #initialisatieRegel}). The exec-model branch refuses a division below
     *  the top of a consequence, whose deposit would persist; here the evaluation itself is speculative, as a default is
     *  evaluated whenever its reads stand, so every division is refused. */
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
     * value computed from what the rule bound, and nothing where that value is leeg (ch. 7.1).
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
            case Expr.Functie f -> throw nietBerekend(f.elementObjecttype() != null ? "Aggregation" : "BuiltinCall",
                    f.bron(), BUITEN);
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
        return w -> RegelSpraakRuntime.rsDatumReken((java.time.temporal.Temporal) d.van(w), (BigDecimal) n.van(w),
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
     *  (RUNTIME-SEMANTICS ch. 5.10), so a member that would throw, a comparison with a division by zero, is not read
     *  where an earlier member settled the verdict. */
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

        @SuppressWarnings("unchecked")
        Lezingen(Objecttype subject) {
            this.subject = subject;
            this.s = (Variable<Object>) DSL.declarationOf(bonenklasse(subject), "$onderwerp");
            this.onderwerp = PatternDSL.pattern(s);
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
            List<RuleItemBuilder<?>> items = new ArrayList<>();
            items.add(onderwerp);
            items.addAll(parameters);
            items.addAll(standen);
            items.addAll(stuur);
            if (toets != null) items.add(test(this, posities(gelezen), toets, voorwaarde));
            return items;
        }

        Set<Integer> volg() {
            gebruik = new TreeSet<>();
            return gebruik;
        }

        void stop() { gebruik = null; }

        /** Registers what the lowered rule reads with the transpiler: the standings, path standings and channel
         *  standings it needs. */
        void vastleggen() {
            gelezen.putAll(afgeleid);
            for (Padlezing p : paden.values()) {
                padlezingen.putIfAbsent(p.sleutel(), p);
                gelezen.putIfAbsent(p.lid().sleutel(), p.lid());
            }
            kanalen.addAll(kanaalnamen);
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
            if (schrijvers.containsKey(lk)) {
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
    @SuppressWarnings("rawtypes")
    private static RuleItemBuilder<?> gevolg(Lezingen lez, int[] pos, Gevolgfunctie f, int n) {
        Variable[] v = new Variable[pos.length + 1];
        v[0] = lez.s;
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
                       List<Function<Object, Object>> stappen) {
        /** The object the path reaches from {@code o}, or null where a link on it is absent. */
        Object loop(Object o) {
            Object d = o;
            for (Function<Object, Object> stap : stappen) {
                d = stap.apply(d);
                if (d == null) return null;
            }
            return d;
        }
    }

    /** A derived member read over a path: one family of path-standing rules. */
    private record Padlezing(Pad pad, Lid lid) {
        String sleutel() { return pad.sleutel() + " " + Feitnamen.lid(lid.sleutel()); }
    }

    private Pad pad(Objecttype subject, List<RolBindingRef> bindingen, Objecttype eindpunt, String wat) {
        FactModelGenerator.Navigatiewandeling w =
                FactModelGenerator.navigatiewandeling(model, subject, bindingen, eindpunt);
        if (w == null) throw new Weigering(wat + ": de navigatie resolvet niet vanaf " + subject.naam);
        if (w.naarVeel()) throw new Weigering(wat + ": de navigatie gaat over een rol naar veel");
        List<Function<Object, Object>> stappen = new ArrayList<>();
        Objecttype cur = subject;
        for (int i = 0; i < w.rollen().size(); i++) {
            String rol = w.rollen().get(i);
            Objecttype volgend = w.bereikt().get(i);
            // a role whose name a declared member took has no reference field of its own (§13.3.9)
            if (!FactModelGenerator.heeftReferentieVeld(model, cur, rol, volgend))
                throw new Weigering(wat + ": " + cur.naam + " heeft geen referentieveld '" + rol + "' naar " + volgend.naam
                        + " (§13.3.9 opslagrichting)");
            stappen.add(Accessors.getter(bonenklasse(cur), rol));
            cur = volgend;
        }
        return new Pad(subject.naam + ":" + String.join(".", w.rollen()), subject, w.rollen(), cur, List.copyOf(stappen));
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
