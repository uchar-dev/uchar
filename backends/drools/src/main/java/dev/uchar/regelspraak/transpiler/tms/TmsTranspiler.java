package dev.uchar.regelspraak.transpiler.tms;

import dev.uchar.regelspraak.graaf.ProducentConsument;
import dev.uchar.regelspraak.graaf.ProducentConsument.ElementSleutel;
import dev.uchar.regelspraak.ir.Conditie;
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
import dev.uchar.regelspraak.model.Datatype;
import dev.uchar.regelspraak.model.RegelSpraakModel;
import dev.uchar.regelspraak.model.RegelSpraakModel.Objecttype;
import dev.uchar.regelspraak.model.RegelSpraakModel.RolBindingRef;
import dev.uchar.regelspraak.runtime.Accessors;
import dev.uchar.regelspraak.schema.FactModelGenerator;
import dev.uchar.regelspraak.transpiler.kern.Dekking;
import dev.uchar.regelspraak.transpiler.kern.Gastheer;
import dev.uchar.regelspraak.transpiler.kern.Namen;

import org.drools.model.DSL;
import org.drools.model.Model;
import org.drools.model.PatternDSL;
import org.drools.model.Rule;
import org.drools.model.RuleItemBuilder;
import org.drools.model.Variable;
import org.drools.model.functions.Block2;
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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

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
 * bean pattern; a derived characteristic is read through its {@link KenmerkStand}, a positive pattern whatever
 * polarity the check tests. The case input stays stated on the beans and the branch never writes a bean while it
 * fires; {@link #zetAfgeleideWaarden} writes the derivations onto the beans afterwards.
 *
 * <p>What it lowers: a characteristic attribution ({@code Rule} with neither presentation nor initialisation) whose
 * condition is built from checks of the subject's own characteristics, negated or not, and {@code Compound}s of those
 * under any quantifier, always valid. Every other form is in the coverage manifest as not lowered, with a reason that
 * names it, and {@code DroolsBackend} refuses the document on the first such entry.
 */
public final class TmsTranspiler {

    /** The Drools package of this branch's rules. The rule of a form carries the form's name, as in the exec-model
     *  branch, so a consumer that loads models of two branches side by side keeps them apart by package. */
    public static final String PKG = "dev.uchar.regelspraak.tms";

    /** The most values one test of this branch reads at once: the DSL's standalone test takes at most thirteen bound
     *  variables. */
    static final int MAX_LEZINGEN = 13;

    private final RegelSpraakModel model;
    private final ClassLoader beanLoader;
    private final List<PrioritizedDefaults> defaults;
    private final Map<Object, List<RolBindingRef>> navigatieBindingen;
    /** Every member a form of the document writes, lowered or not. A member that no form writes is input. */
    private Map<ElementSleutel, Set<String>> schrijvers = Map.of();
    private final List<Dekking> dekking = new ArrayList<>();
    /** The characteristics the lowered rules derive, by key. The key is normalised, and the setter the derivation is
     *  written back through is the bean property of the declared spelling, which the key no longer carries. */
    private final Map<ElementSleutel, Kenmerklid> afgeleid = new LinkedHashMap<>();
    /** The derived characteristics the lowered conditions read: each gets the generic rules of its standing. */
    private final Map<ElementSleutel, Kenmerklid> gelezen = new LinkedHashMap<>();
    /** Unique per constraint across the model: the executable model shares a constraint between rules by its id, so
     *  two different tests under one id would collapse into the first. */
    private int idSeq;
    private Model model0;

    private TmsTranspiler(RegelSpraakModel model, ClassLoader beanLoader, List<PrioritizedDefaults> defaults,
                          Map<Object, List<RolBindingRef>> navigatieBindingen) {
        this.model = model;
        this.beanLoader = beanLoader != null ? beanLoader : TmsTranspiler.class.getClassLoader();
        this.defaults = defaults;
        this.navigatieBindingen = navigatieBindingen;
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
        Map<Object, List<RolBindingRef>> nav = new java.util.IdentityHashMap<>(navigatieBindingen);
        TmsTranspiler t = new TmsTranspiler(m, beanLoader, defaults, nav);
        t.model0 = t.run();
        return t;
    }

    public Model model() { return model0; }

    /** The coverage manifest: one verdict per form, the rules in document order, then every other family. */
    public List<Dekking> dekking() { return dekking; }

    /**
     * Writes every characteristic this model derived in {@code sessie} onto its bean: {@code setX(true)} for each
     * {@link HeeftKenmerk}. The engine is not told, so the session must not be fired again afterwards: a rule that
     * reads the bean pattern would see a value the network never propagated. A characteristic the case input set stays
     * set, since nothing writes {@code false}.
     */
    public void zetAfgeleideWaarden(KieSession sessie) {
        for (Object f : sessie.getObjects(o -> o instanceof HeeftKenmerk)) {
            HeeftKenmerk h = (HeeftKenmerk) f;
            Kenmerklid lid = afgeleid.get(h.kenmerk());
            if (lid == null)
                throw new IllegalStateException(h + " in de sessie, maar geen regel van dit model leidt "
                        + Feitnamen.lid(h.kenmerk()) + " af");
            Accessors.setter(lid.klasse(), lid.naam()).accept(h.onderwerp(), Boolean.TRUE);
        }
    }

    // ---------------- the model ----------------

    private Model run() {
        schrijvers = ProducentConsument.producentIndex(model, defaults);
        List<Rule> regels = new ArrayList<>();
        for (ResolvedRegel r : model.resolvedRegels) regels.addAll(meet("regel", naamOf(r.naam()), () -> regel(r)));
        for (ResolvedConsistentie c : model.consistenties)
            weiger("consistentie", naamOf(c.naam()), "een Consistency: " + NOG_NIET);
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
        for (int i = 0; i < defaults.size(); i++)
            weiger("defaults", defaults.get(i).naam() != null ? defaults.get(i).naam() : "/rules/prioritizedDefaults/" + i,
                    "een PrioritizedDefaults: " + NOG_NIET);
        for (ResolvedStartpunt sp : model.resolvedStartpunten)
            weiger("startpuntbepaling", sp.tijdlijn(), "een StartPointDetermination: tijdlijnen zijn " + BUITEN);
        for (ResolvedDagsoort d : model.resolvedDagsoorten)
            weiger("dagsoortdefinitie", d.naam(), "een DayKindDefinition: dagsoorten zijn " + BUITEN);
        // the standing of a derived characteristic is built once, for every condition that reads it, and only for one
        // that a lowered condition reads: a standing nothing reads would only add firings
        List<Rule> standen = new ArrayList<>();
        for (Kenmerklid lid : gelezen.values()) standen.addAll(standregels(lid));
        ModelImpl m = new ModelImpl(PKG);
        regels.forEach(m::addRule);
        standen.forEach(m::addRule);
        return m;
    }

    private static final String NOG_NIET = "de truth-maintenance-tak draagt deze vorm nog niet";
    private static final String BUITEN = "buiten de proef van de truth-maintenance-tak";

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
        if (r.soort() != ResolvedRegel.Soort.KENMERKTOEKENNING) {
            String doel = r.doel() instanceof Expr.AttribuutRef a ? "'" + a.attribuut() + "'"
                    + (a.objecttype() == null ? "" : " van " + a.objecttype().naam) : "een attribuut";
            throw new Weigering(r.soort() == ResolvedRegel.Soort.INITIALISATIE
                    ? "een initialisatie (van " + doel + "): de truth-maintenance-tak leidt nog alleen kenmerken af"
                    : "een attribuutdoel (" + (r.soort() == ResolvedRegel.Soort.BEREKENING ? "berekening" : "gelijkstelling")
                      + " van " + doel + "): de truth-maintenance-tak leidt nog alleen kenmerken af");
        }
        // the rule of a form is named by the form, and a KieBase keeps one rule per name
        if (r.naam() == null) throw new Weigering("een regel zonder naam (WF-107)");
        if (!(r.doel() instanceof Expr.AttribuutRef doel) || doel.objecttype() == null || doel.attribuut() == null)
            throw new Weigering("een kenmerktoekenning zonder opgelost doel");
        if (navigatieBindingen.containsKey(doel))
            throw new Weigering("een kenmerktoekenning op een genavigeerd doel");
        if (r.geldigheid() != null && !r.geldigheid().altijd())
            throw new Weigering("een versie met een begrensde geldigheid (Validity met from of to): " + NOG_NIET);
        // the loader refuses any other value (WF-022); asked again because this rule writes true whatever it says
        if (!(r.waarde() instanceof Expr.Literal l && "waar".equals(l.waarde()) && l.type() instanceof Datatype.Booleaan))
            throw new Weigering("een kenmerktoekenning met een andere waarde dan de constante waar (WF-022)");
        return List.of(kenmerkRegel(r, doel));
    }

    /**
     * A characteristic attribution: the subject pattern, a pattern per derived characteristic its condition reads, one
     * test over every value they bind, and a consequence that inserts the {@link HeeftKenmerk} logically. Its
     * justification is the whole match, so a standing the condition read that is later retracted takes the
     * characteristic with it, also when the condition read it negated.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private Rule kenmerkRegel(ResolvedRegel r, Expr.AttribuutRef doel) {
        Objecttype subject = doel.objecttype();
        Kenmerklid lid = kenmerklid(subject, doel.attribuut());
        Variable s = DSL.declarationOf(bonenklasse(subject), "$onderwerp");
        Lezingen lez = new Lezingen(s);
        Toets toets = r.voorwaarde() == null ? null : voorwaarde(r.voorwaarde(), subject, lez);
        List<RuleItemBuilder<?>> items = new ArrayList<>();
        items.add(lez.onderwerp);
        items.addAll(lez.standen);
        if (toets != null) items.add(test(lez.variabelen, toets, r.voorwaarde()));
        ElementSleutel sleutel = lid.sleutel();
        items.add(DSL.on(s).execute((Block2<org.drools.model.Drools, Object>) (drools, o) ->
                drools.logicalInsert(new HeeftKenmerk(o, sleutel))));
        afgeleid.put(sleutel, lid);
        gelezen.putAll(lez.afgeleid);
        return PatternDSL.rule(PKG, model.geëmitteerdeRegelNaam(r.naam())).build(items.toArray(new RuleItemBuilder[0]));
    }

    // ---------------- a condition ----------------

    /** A condition as a predicate over the values its reads bind, in the order {@link Lezingen} gives them indices. */
    @FunctionalInterface
    private interface Toets {
        boolean test(Object[] waarden);
    }

    /** Every condition node is named here, so a node the IR records gain later fails to compile until it is decided
     *  whether this branch reads it. */
    private Toets voorwaarde(Conditie c, Objecttype subject, Lezingen lez) {
        return switch (c) {
            case Conditie.Kenmerk k -> kenmerkcheck(k, subject, lez);
            case Conditie.Samengesteld s -> samengesteld(s, subject, lez);
            case Conditie.Vergelijking v -> throw nietGelezen("Comparison", c);
            case Conditie.Predicaat p -> throw nietGelezen("Predicate", c);
            case Conditie.Periode p -> throw nietGelezen("TimeWindow", c);
            case Conditie.Kwantorvouw q -> throw nietGelezen("QuantifierFold", c);
            case Conditie.Rollidmaatschap rol -> throw nietGelezen("RoleMembership", c);
            case Conditie.VerreRollidmaatschap rol -> throw nietGelezen("RoleMembership", c);
            case Conditie.Regelversie rv -> throw nietGelezen("Regelversie", c);
            case Conditie.Onopgelost o -> throw nietGelezen("Unresolved", c);
        };
    }

    private static Weigering nietGelezen(String vorm, Conditie c) {
        return new Weigering("een " + vorm + " in de voorwaarde ('" + snippet(c) + "'): de truth-maintenance-tak leest nog"
                + " alleen kenmerken van het eigen onderwerp, ook ontkend en in een Compound");
    }

    private static String snippet(Conditie c) {
        return c.bron() == null || c.bron().snippet() == null ? "zonder bron" : c.bron().snippet();
    }

    /** A check of the subject's own characteristic: the bound value, or its complement where the check is negated.
     *  The standing is total, so the complement of a bound onwaar is the negated check's waar and no negation is
     *  needed here. */
    private Toets kenmerkcheck(Conditie.Kenmerk k, Objecttype subject, Lezingen lez) {
        if (k.periode() != null)
            throw new Weigering("een CharacteristicCheck met wholePeriod ('" + snippet(k) + "'): tijdlijnen zijn " + BUITEN);
        if (navigatieBindingen.containsKey(k) || k.navigatie() != null)
            throw new Weigering("een CharacteristicCheck met navigation ('" + snippet(k) + "'): een genavigeerde lezing; "
                    + NOG_NIET);
        // WF-109: the host is satisfied extensionally; a host the subject is no instance of names another object, which
        // a rule condition does not bind
        if (!Gastheer.hostOp(k, subject))
            throw new Weigering("een CharacteristicCheck met een host die het onderwerp niet is ('" + snippet(k) + "')");
        int i = lez.kenmerk(kenmerklid(subject, k.kenmerk()));
        boolean gewenst = !k.negatie();
        return w -> (Boolean) w[i] == gewenst;
    }

    /** A composite folds its members in wire order and stops at the first member after which the verdict stands
     *  (RUNTIME-SEMANTICS ch. 5.10). Over bound truth values no member can throw, so the stop is not observable here;
     *  it is kept so that a member that can throw, a comparison with a division, is not read where the verdict
     *  already stands once such members are lowered. */
    private Toets samengesteld(Conditie.Samengesteld s, Objecttype subject, Lezingen lez) {
        List<Toets> leden = new ArrayList<>();
        for (Conditie deel : s.onderdelen()) leden.add(voorwaarde(deel, subject, lez));
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
     * The reads of one condition, each bound once in the rule's left-hand side and numbered in the order they are met:
     * an input characteristic off the subject's bean pattern, a derived one off its {@link KenmerkStand}. The numbers
     * are the indices of the values the condition's test receives.
     */
    private final class Lezingen {
        final Variable<?> s;
        final PatternDSL.PatternDef<?> onderwerp;
        final List<RuleItemBuilder<?>> standen = new ArrayList<>();
        final List<Variable<?>> variabelen = new ArrayList<>();
        final Map<ElementSleutel, Kenmerklid> afgeleid = new LinkedHashMap<>();
        private final Map<ElementSleutel, Integer> index = new LinkedHashMap<>();

        @SuppressWarnings({"unchecked", "rawtypes"})
        Lezingen(Variable s) {
            this.s = s;
            this.onderwerp = PatternDSL.pattern(s);
        }

        @SuppressWarnings({"unchecked", "rawtypes"})
        int kenmerk(Kenmerklid lid) {
            Integer bekend = index.get(lid.sleutel());
            if (bekend != null) return bekend;
            int i = variabelen.size();
            if (variabelen.size() == MAX_LEZINGEN)
                throw new Weigering("een voorwaarde die meer dan " + MAX_LEZINGEN + " leden leest: de toets van deze tak"
                        + " neemt hoogstens " + MAX_LEZINGEN + " gebonden waarden");
            Variable<Boolean> w = DSL.declarationOf(Boolean.class, "$lezing_" + i);
            ElementSleutel sleutel = lid.sleutel();
            if (schrijvers.containsKey(sleutel)) {
                Variable<KenmerkStand> st = DSL.declarationOf(KenmerkStand.class, "$stand_" + i);
                Variable<Object> o = (Variable<Object>) s;
                standen.add(PatternDSL.pattern(st)
                        .expr(id("stand"), x -> x.kenmerk().equals(sleutel))
                        .expr(id("stand-onderwerp"), o, (x, y) -> x.onderwerp() == y)
                        .bind(w, KenmerkStand::waar));
                afgeleid.put(sleutel, lid);
            } else {
                // input: no form writes it, so it does not change while the session fires and is read off the bean
                Function<Object, Object> getter = Accessors.getter(lid.klasse(), lid.naam());
                ((PatternDSL.PatternDef) onderwerp).bind(w, o -> Boolean.TRUE.equals(getter.apply(o)));
            }
            index.put(sleutel, i);
            variabelen.add(w);
            return i;
        }
    }

    /** The test over every value a condition binds, as a standalone test after the patterns that bind them. A
     *  condition that reads nothing is a constant. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private RuleItemBuilder<?> test(List<Variable<?>> vars, Toets t, Conditie c) {
        String id = id("voorwaarde");
        Variable[] v = vars.toArray(new Variable[0]);
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
                    (Predicate12) (a, b, d, e, f, g, h, i, j, k, l, n) ->
                            t.test(new Object[]{a, b, d, e, f, g, h, i, j, k, l, n}));
            case 13 -> DSL.expr(id, v[0], v[1], v[2], v[3], v[4], v[5], v[6], v[7], v[8], v[9], v[10], v[11], v[12],
                    (Predicate13) (a, b, d, e, f, g, h, i, j, k, l, n, p) ->
                            t.test(new Object[]{a, b, d, e, f, g, h, i, j, k, l, n, p}));
            default -> throw new IllegalStateException("'" + snippet(c) + "' bindt " + v.length + " waarden; "
                    + "Lezingen weigert er meer dan " + MAX_LEZINGEN);
        };
    }

    // ---------------- the standing of a derived characteristic ----------------

    /**
     * The three generic rules that keep one {@link KenmerkStand} per object of the declaring type: waar from the input,
     * waar from a derivation, onwaar when neither holds. The onwaar rule carries the branch's only negation, {@code not
     * HeeftKenmerk}, and reads the input in its left-hand side, so a derivation that appears later retracts the onwaar
     * standing and every conclusion that read it. When both sources hold, the two waar standings are equal, and truth
     * maintenance keeps them as one fact with a justification each.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private List<Rule> standregels(Kenmerklid lid) {
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

    // ---------------- members and beans ----------------

    /** A characteristic as this branch keys and reaches it: the key on its declaring type, that type, the declared
     *  spelling (the bean property), and the bean class of the declaring type, whose subtypes' beans extend it. */
    private record Kenmerklid(ElementSleutel sleutel, Objecttype declarant, String naam, Class<?> klasse) {}

    /** Characteristic {@code naam} as declared on {@code houder}'s chain, compared as the verifier compares names. */
    private Kenmerklid kenmerklid(Objecttype houder, String naam) {
        for (Objecttype t : houder.keten())
            for (String k : t.kenmerken)
                if (Namen.norm(k).equals(Namen.norm(naam))) {
                    if (t.kenmerkGranulariteit.containsKey(k))
                        throw new Weigering("het tijdsafhankelijke kenmerk '" + k + "' van " + t.naam + ": tijdlijnen zijn "
                                + BUITEN);
                    return new Kenmerklid(ProducentConsument.elementVan(houder, naam, true), t, k, bonenklasse(t));
                }
        throw new Weigering("het kenmerk '" + naam + "' is niet gedeclareerd op " + houder.naam + " of een supertype");
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
