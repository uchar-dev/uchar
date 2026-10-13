package dev.uchar.regelspraak.transpiler.tms;

import dev.uchar.regelspraak.ir.Geldigheid;
import dev.uchar.regelspraak.runtime.Accessors;
import dev.uchar.regelspraak.transpiler.tms.plan.Accumulatie;
import dev.uchar.regelspraak.transpiler.tms.plan.Alfa;
import dev.uchar.regelspraak.transpiler.tms.plan.Bestaat;
import dev.uchar.regelspraak.transpiler.tms.plan.Betatoets;
import dev.uchar.regelspraak.transpiler.tms.plan.Binding;
import dev.uchar.regelspraak.transpiler.tms.plan.Gevolg;
import dev.uchar.regelspraak.transpiler.tms.plan.Join;
import dev.uchar.regelspraak.transpiler.tms.plan.Leestabel;
import dev.uchar.regelspraak.transpiler.tms.plan.LhsItem;
import dev.uchar.regelspraak.transpiler.tms.plan.Modelplan;
import dev.uchar.regelspraak.transpiler.tms.plan.Niet;
import dev.uchar.regelspraak.transpiler.tms.plan.Onderwerp;
import dev.uchar.regelspraak.transpiler.tms.plan.Patroon;
import dev.uchar.regelspraak.transpiler.tms.plan.Regelplan;
import dev.uchar.regelspraak.transpiler.tms.plan.Term;
import dev.uchar.regelspraak.transpiler.tms.plan.Toetsitem;

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
 * {@link Gevolg} says. It decides nothing: which globals there are, what is read, how, in which order, and what a
 * firing inserts is the plan's ({@code TmsTranspiler}, {@link Modelplan}, {@link Regelplan}).
 *
 * <p>It renders a whole plan at once ({@link #regel}). The variables of the value array's slots are the variables the
 * bindings of the plan bind, by name; a value variable is bound once.
 */
final class PatternDslWeergave {

    /** The slots of the rule's value array ({@link #regel}). */
    private final Leestabel tabel;
    /** The value variables of the rule, by name, the subject's among them. */
    private final Map<String, Variable<Object>> waarden = new HashMap<>();
    /** The value variables a binding or an accumulate function binds already and that are still in scope. */
    private final Set<String> gebonden = new HashSet<>();

    /** The rendering of a whole plan, whose subject pattern, if the rule has one, is an item of its left-hand side. */
    private PatternDslWeergave(Leestabel tabel) {
        this.tabel = tabel;
    }

    /**
     * The executable model of {@code plan}: its globals declared, each once, and its rules, in the order of the plan
     * ({@link #regel}). This is the one place the model is made.
     */
    static Model model(Modelplan plan) {
        ModelImpl m = new ModelImpl(TmsTranspiler.PKG);
        Map<String, Global<?>> globals = new HashMap<>();
        for (Modelplan.Globaal g : plan.globals()) {
            Global<?> global = DSL.globalOf(g.klasse(), TmsTranspiler.PKG, g.naam());
            if (globals.putIfAbsent(g.naam(), global) != null)
                throw new IllegalStateException("de global " + g.naam() + " wordt twee keer gedeclareerd");
            m.addGlobal(global);
        }
        for (Regelplan r : plan.regels())
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
    static Rule regel(Regelplan plan, Function<String, Global<?>> globals) {
        PatternDslWeergave w = new PatternDslWeergave(plan.lezingen());
        List<RuleItemBuilder<?>> items = new ArrayList<>();
        for (LhsItem item : plan.linkerkant()) items.add(w.weergave(item));
        items.add(w.gevolg(plan.gevolg(), globals));
        org.drools.model.impl.RuleBuilder b = PatternDSL.rule(TmsTranspiler.PKG, plan.naam());
        if (plan.agendagroep() != null) b = b.attribute(Rule.Attribute.AGENDA_GROUP, plan.agendagroep());
        return b.build(items.toArray(new RuleItemBuilder[0]));
    }

    /** The rendering of {@code item} of a whole plan. */
    private RuleItemBuilder<?> weergave(LhsItem item) {
        return switch (item) {
            case Onderwerp o -> onderwerp(o);
            case Patroon p -> {
                // a consequence may name the object a pattern binds, as the fault of a session without a date does
                Weergegeven w = patroon(p);
                if (waarden.putIfAbsent(p.naam(), w.variabele()) != null)
                    throw new IllegalStateException("de variabele " + p.naam() + " wordt twee keer gebonden");
                yield w.def();
            }
            case Niet n -> niet(n);
            case Bestaat b -> bestaat(b);
            case Accumulatie a -> accumulatie(a);
            case Toetsitem t -> toets(t);
        };
    }

    /** The subject pattern of a whole plan: its variable, its constraints, its joins, its constraints between the bean
     *  and another pattern's object, then its bindings. */
    @SuppressWarnings("unchecked")
    private PatternDSL.PatternDef<Object> onderwerp(Onderwerp o) {
        Variable<Object> v = (Variable<Object>) DSL.declarationOf(o.bonenklasse(), o.naam());
        if (waarden.putIfAbsent(o.naam(), v) != null)
            throw new IllegalStateException("de variabele " + o.naam() + " wordt twee keer gebonden");
        PatternDSL.PatternDef<Object> def = PatternDSL.pattern(v);
        for (Alfa a : o.alfa()) def = def.expr(a.id(), predicaat(o.bonenklasse(), a));
        for (Join j : o.joins()) def = join(def, o.bonenklasse(), j);
        for (Betatoets t : o.betatoetsen()) def = betatoets(def, t);
        for (Binding b : o.bindingen()) def = def.bind(bind(b.doel()), lezer(b));
        return def;
    }

    /** The variable of slot {@code slot} of the value array, null for a binder or a probe. */
    private Variable<?> variabele(int slot) {
        String naam = tabel.slots().get(slot).naam();
        return naam == null ? null : bekend(naam);
    }

    // ---------------- the consequence ----------------

    /** The consequence {@code g} plans. */
    @SuppressWarnings("rawtypes")
    private RuleItemBuilder<?> gevolg(Gevolg g, Function<String, Global<?>> globals) {
        return switch (g) {
            case Gevolg.Waarde x -> waarde(x);
            case Gevolg.Kenmerk x -> new Consequentie(new Variable[]{bekend(x.onderwerp())}, (drools, a) ->
                    voegIn(drools, x.invoeging(), new HeeftKenmerk(a[0], x.kenmerk())));
            case Gevolg.Toetsfout x -> toetsfout(x);
            case Gevolg.Fout x -> new Consequentie(new Variable[]{bekend(x.onderwerp())}, (drools, a) ->
                    voegIn(drools, x.invoeging(), new Fout(a[0], x.fout().vorm(),
                            Rekenhulpen.rekenfout(x.zin(), x.fout().regel()))));
            case Gevolg.Inconsistentie x -> new Consequentie(new Variable[]{bekend(x.onderwerp())}, (drools, a) ->
                    voegIn(drools, x.invoeging(), Rekenhulpen.inconsistentie(x.vorm(), a[0])));
            case Gevolg.Schrijversconflict x ->
                    new Consequentie(new Variable[]{bekend(x.onderwerp()), variabele(x.waarden())}, (drools, a) -> {
                        List<Waarde> waarden = new ArrayList<>();
                        for (Object o : (List<?>) a[1]) waarden.add((Waarde) o);
                        Schrijversconflict c = Schrijversconflict.van(a[0], x.attribuut(), waarden, x.documentplaats(),
                                x.typenamen());
                        voegIn(drools, x.invoeging(), new Fout(a[0], c.schrijvers().get(0).schrijver(), c));
                    });
            case Gevolg.Parameterstand x -> new Consequentie(new Variable[]{globals.apply(x.global())}, (drools, a) ->
                    voegIn(drools, x.invoeging(), new ParameterStand(x.parameter(), a[0])));
            case Gevolg.Rekendatumstand x -> new Consequentie(new Variable[]{globals.apply(x.global())}, (drools, a) ->
                    voegIn(drools, x.invoeging(), new Rekendatum((LocalDate) a[0])));
            case Gevolg.Kenmerkstand x -> new Consequentie(new Variable[]{bekend(x.onderwerp())}, (drools, a) ->
                    voegIn(drools, x.invoeging(), new KenmerkStand(a[0], x.kenmerk(), x.waar())));
            case Gevolg.Stand x -> new Consequentie(new Variable[]{bekend(x.onderwerp()), variabele(x.waarde())},
                    (drools, a) -> voegIn(drools, x.invoeging(), new Stand(a[0], x.attribuut(),
                            x.emmerscalair() ? Rekenhulpen.emmerscalairVan(a[1]) : a[1])));
            case Gevolg.Padstand x -> x.bron() == Gevolg.Standbron.ONTBREKENDE_LINK
                    ? new Consequentie(new Variable[]{bekend(x.onderwerp())}, (drools, a) ->
                            voegIn(drools, x.invoeging(), new PadStand(a[0], x.pad(), x.lid(), null)))
                    : new Consequentie(new Variable[]{bekend(x.onderwerp()), variabele(x.waarde())}, (drools, a) ->
                            voegIn(drools, x.invoeging(), new PadStand(a[0], x.pad(), x.lid(), a[1])));
            case Gevolg.Uniciteitssleutel x -> uniciteitssleutel(x);
            case Gevolg.Uniciteitsschending x -> new Consequentie(new Variable[]{bekend(x.onderwerp())}, (drools, a) ->
                    voegIn(drools, x.invoeging(), new Uniciteitsschending(x.uniciteit(), a[0])));
            case Gevolg.Invoervormfout x -> new Consequentie(new Variable[]{bekend(x.onderwerp())}, (drools, a) ->
                    voegIn(drools, x.invoeging(), new Fout(a[0], x.fout().vorm(),
                            Rekenhulpen.invoervormfout(x.voor(), a[0], x.na(), x.fout().regel()))));
            case Gevolg.Kanaalstand x -> new Consequentie(new Variable[0], (drools, a) ->
                    voegIn(drools, x.invoeging(), new KanaalStand(x.producent(), x.inconsistent())));
            case Gevolg.Ontvanger x -> consequentie(x.onderwerp(), slotsVan(x.ontvanger(), x.gewicht(), x.sleutel(),
                    x.maximum()), (drools, bron, w) -> voegIn(drools, x.invoeging(), new Ontvanger(x.verdeling(), bron,
                    w[x.ontvanger()], lees(w, x.gewicht()), lees(w, x.sleutel()), lees(w, x.maximum()))));
            case Gevolg.Bijdrage x -> bijdrage(x);
            case Gevolg.Filterdeponering x -> consequentie(x.onderwerp(), slotsVan(x.lid()), (drools, drager, w) ->
                    voegIn(drools, x.invoeging(), new Filterdeponering(x.vouw(), drager, w[x.lid()])));
            case Gevolg.Filterfout x -> filterfout(x);
            case Gevolg.Lidfout x -> consequentie(x.onderwerp(), slotsVan(plaatsslots(x.lid(), x.plaats())),
                    (drools, drager, w) -> {
                        List<Integer> plaats = vouwplaats(x.plaats(), x.vouw(), w[x.lid()], w);
                        voegIn(drools, x.invoeging(), new Lidfout(x.vouw(), drager, w[x.lid()],
                                Rekenhulpen.invoervormfout(x.voor(), w[x.lid()], x.na(), null), plaats));
                    });
            case Gevolg.Creatie x -> creatie(x);
            case Gevolg.Koppeling x -> x.fase() < 0
                    ? new Consequentie(new Variable[]{bekend(x.eerste()), bekend(x.tweede())}, (drools, a) ->
                            voegIn(drools, x.invoeging(), new Koppeling(x.relatie(), a[0], a[1], 0)))
                    : new Consequentie(new Variable[]{bekend(x.eerste()), bekend(x.tweede()), variabele(x.fase())},
                            (drools, a) -> voegIn(drools, x.invoeging(),
                                    new Koppeling(x.relatie(), a[0], a[1], (Integer) a[2])));
            case Gevolg.TweedeVulling x -> new Consequentie(new Variable[]{bekend(x.onderwerp())}, (drools, a) ->
                    voegIn(drools, x.invoeging(), new Fout(a[0], x.relatie(), new IllegalStateException(x.zin()))));
            case Gevolg.Afloop x -> afloop(x);
            case Gevolg.Conflict x -> {
                Waardefunctie sleutel = ClosureWeergave.waardefunctie(x.sleutel());
                yield consequentie(x.onderwerp(), x.slots(), x.grootte(), (drools, onderwerp, w) ->
                        voegIn(drools, x.invoeging(), stapelfeit(x.feit(), onderwerp, sleutel.van(w))));
            }
            case Gevolg.Uitdeling x -> consequentie(x.onderwerp(), slotsVan(x.pot(), x.lijst()), (drools, bron, w) -> {
                for (Object feit : Rekenhulpen.uitdeling(x.vorm(), bron, x.methode(), w[x.pot()], w[x.lijst()],
                        x.gewogen(), x.gemaximeerd(), x.afnemend(), x.decimalen(), x.aandeel(), x.rest(), x.schrijver()))
                    voegIn(drools, x.invoeging(), feit);
            });
        };
    }

    /** The keys of {@link Gevolg.Uniciteitssleutel}: the components computed from the bound values, the parent bound,
     *  and the route that makes one key, or one per value a component on a timeline ever carries. */
    private RuleItemBuilder<?> uniciteitssleutel(Gevolg.Uniciteitssleutel g) {
        List<Waardefunctie> componenten = new ArrayList<>();
        for (Term t : g.componenten())
            componenten.add(ClosureWeergave.waardefunctie(t));
        return consequentie(g.onderwerp(), g.slots(), g.grootte(), (drools, element, w) -> {
            List<Object> waarden = new ArrayList<>(componenten.size());
            for (Waardefunctie c : componenten) waarden.add(c.van(w));
            Object ouder = g.scope() < 0 ? null : w[g.scope()];
            for (Uniciteitssleutel k : Rekenhulpen.uniciteitssleutels(g.uniciteit(), g.bron(), element, ouder, waarden,
                    g.tijdlijn()))
                voegIn(drools, g.invoeging(), k);
        });
    }

    /** The contribution of {@link Gevolg.Bijdrage}: the member itself, its value through the route, or what the matrix
     *  of a quantifier fold says of it, its place first and the probe in its slot where the plan says so. */
    private RuleItemBuilder<?> bijdrage(Gevolg.Bijdrage g) {
        return switch (g.inbreng()) {
            case Gevolg.Inbreng.Telling t -> consequentie(g.onderwerp(), g.slots(), g.grootte(), (drools, drager, w) ->
                    voegIn(drools, g.invoeging(), new Bijdrage(g.vouw(), drager, w[g.lid()], null)));
            case Gevolg.Inbreng.Lidwaarde l -> consequentie(g.onderwerp(), g.slots(), g.grootte(), (drools, drager, w) -> {
                Bijdrage b = Rekenhulpen.bijdrage(g.vouw(), drager, w[g.lid()], w[l.waarde()], l.totaal(), l.gevuld());
                if (b != null) voegIn(drools, g.invoeging(), b);
            });
            case Gevolg.Inbreng.Oordeel o -> {
                Predicate<Object[]> matrix = ClosureWeergave.toets(o.matrix());
                yield consequentie(g.onderwerp(), g.slots(), g.grootte(), (drools, drager, w) ->
                        voegIn(drools, g.invoeging(), new Bijdrage(g.vouw(), drager, w[g.lid()],
                                matrix.test(w) ? BigDecimal.ONE : BigDecimal.ZERO)));
            }
            case Gevolg.Inbreng.Lidtoets t -> {
                Predicate<Object[]> matrix = ClosureWeergave.toets(t.matrix());
                yield consequentie(g.onderwerp(), g.slots(), g.grootte(), (drools, drager, w) -> {
                    List<Integer> plaats = vouwplaats(t.plaats(), g.vouw(), w[g.lid()], w);
                    Object s = t.sonde() < 0 ? null : Rekenhulpen.sonde();
                    if (s != null) w[t.sonde()] = s;
                    Boolean waar = null;
                    RuntimeException fout = null;
                    try {
                        waar = matrix.test(w);
                    } catch (RuntimeException e) {
                        if (!Rekenfout.isEvaluatiefout(e)) throw e;
                        fout = e;
                    }
                    voegIn(drools, g.invoeging(), new Bijdrage(g.vouw(), drager, w[g.lid()],
                            new Lidtoets(plaats, waar, fout, Rekenhulpen.gemeld(s))));
                });
            }
        };
    }

    /** The fault of {@link Gevolg.Filterfout}: the filter evaluated again over the same bound values, then the member's
     *  place. */
    private RuleItemBuilder<?> filterfout(Gevolg.Filterfout g) {
        Predicate<Object[]> filter = ClosureWeergave.toets(g.filter());
        return consequentie(g.onderwerp(), g.slots(), g.grootte(), (drools, drager, w) -> {
            RuntimeException fout = ClosureWeergave.fout(filter, w);
            if (fout == null)
                throw new IllegalStateException("het filter van " + g.vouw() + " faalde in de toets en niet in het gevolg,"
                        + " over dezelfde gebonden waarden");
            List<Integer> plaats = vouwplaats(g.plaats(), g.vouw(), w[g.lid()], w);
            voegIn(drools, g.invoeging(), new Filterfout(g.vouw(), drager, w[g.lid()], fout, plaats));
        });
    }

    /** The member's place {@code p} in the order of the fold {@code vouw}, from the bound values {@code w}, through the
     *  route. */
    private static List<Integer> vouwplaats(Gevolg.Vouwplaats p, String vouw, Object lid, Object[] w) {
        List<String> velden = new ArrayList<>();
        List<Object> aantallen = new ArrayList<>(), plaatsen = new ArrayList<>();
        for (Gevolg.Vouwplaats.Stap s : p.stappen()) {
            velden.add(s.veld());
            aantallen.add(w[s.aantal()]);
            plaatsen.add(w[s.plaats()]);
        }
        return Rekenhulpen.vouwplaats(p.reden(), vouw, lid, p.selectie(), velden, aantallen, plaatsen);
    }

    /** The slot {@code lid} and the slots of the place {@code p}. */
    private static int[] plaatsslots(int lid, Gevolg.Vouwplaats p) {
        int[] uit = new int[1 + 2 * p.stappen().size()];
        uit[0] = lid;
        for (int i = 0; i < p.stappen().size(); i++) {
            uit[1 + 2 * i] = p.stappen().get(i).aantal();
            uit[2 + 2 * i] = p.stappen().get(i).plaats();
        }
        return uit;
    }

    /** The slots a consequence reads, each once and in ascending order, without -1, which stands for none. */
    private static List<Integer> slotsVan(int... slots) {
        return Arrays.stream(slots).filter(i -> i >= 0).distinct().sorted().boxed().toList();
    }

    /** The value in slot {@code slot} of {@code w}, null for the slot -1. */
    private static Object lees(Object[] w, int slot) {
        return slot < 0 ? null : w[slot];
    }

    /** A consequence over the object {@code onderwerp} binds and the values at {@code slots}, in a value array that
     *  reaches the last of them. */
    private RuleItemBuilder<?> consequentie(String onderwerp, List<Integer> slots, Gevolgfunctie f) {
        return consequentie(onderwerp, slots, slots.isEmpty() ? 0 : slots.get(slots.size() - 1) + 1, f);
    }

    /** The value of {@link Gevolg.Waarde}: computed from the bound values, with its fault, its deposit and its leeg
     *  policy as the plan says. */
    private RuleItemBuilder<?> waarde(Gevolg.Waarde g) {
        Waardefunctie waarde = ClosureWeergave.waardefunctie(g.waarde());
        Gevolg.Deponering deponering = g.deponering();
        return consequentie(g.onderwerp(), g.slots(), g.grootte(), (drools, onderwerp, w) -> {
            // the probe hears every §6.5 and §6.6 place the evaluation reaches, and only those (Sonde)
            Object s = deponering == null ? null : Rekenhulpen.sonde();
            if (s != null) w[deponering.sonde()] = s;
            Object v;
            try {
                v = waarde.van(w);
                if (g.collapse()) v = Rekenhulpen.collapse(v);
            } catch (RuntimeException e) {
                if (!Rekenfout.isEvaluatiefout(e)) throw e;
                voegIn(drools, g.invoeging(),
                        new Fout(onderwerp, g.fout().vorm(), Rekenfout.benoem(e, g.fout().regel())));
                return;
            }
            if (Rekenhulpen.gemeld(s))
                voegIn(drools, g.invoeging(), Rekenhulpen.inconsistentie(deponering.vorm(), onderwerp));
            if (!Rekenhulpen.isLeeg(v) || g.schrijftLeeg())
                voegIn(drools, g.invoeging(), new Waarde(onderwerp, g.doel(), v, g.schrijver()));
        });
    }

    /**
     * The object creation of {@link Gevolg.Creatie}, in the order of its steps: the depth through its route, before
     * anything is made; the values of the initialisations, the first evaluation fault being all the creation does; the
     * construction of the new bean, with the attribute initialisations whose value is gevuld set on it before it enters
     * working memory; then the insertions, in order.
     */
    private RuleItemBuilder<?> creatie(Gevolg.Creatie g) {
        List<Waardefunctie> waarden = new ArrayList<>();
        for (Gevolg.Initialisatie i : g.initialisaties()) waarden.add(ClosureWeergave.waardefunctie(i.waarde()));
        Gevolg.Constructie constructie = g.constructie();
        List<BiConsumer<Object, Object>> zetters = new ArrayList<>();
        for (Gevolg.Constructie.Zetting z : constructie.zettingen())
            zetters.add(Accessors.setter(z.veld().klasse(), z.veld().eigenschap()));
        Gevolg.Creatiediepte d = g.diepte();
        return consequentie(g.onderwerp(), g.slots(), g.grootte(), (drools, onderwerp, w) -> {
            int diepte = Rekenhulpen.creatiediepte(g.creatie(), d.stratum(), d.type(), (List<?>) w[d.herkomst()],
                    d.budget());
            Object[] v = new Object[waarden.size()];
            for (int i = 0; i < v.length; i++) {
                try {
                    v[i] = Rekenhulpen.collapse(waarden.get(i).van(w));
                } catch (RuntimeException e) {
                    if (!Rekenfout.isEvaluatiefout(e)) throw e;
                    voegIn(drools, g.foutinvoeging(),
                            new Fout(onderwerp, g.fout().vorm(), Rekenfout.benoem(e, g.fout().regel())));
                    return;
                }
            }
            Object nieuw = nieuwObject(constructie.klasse());
            for (int j = 0; j < zetters.size(); j++) {
                Object waarde = v[constructie.zettingen().get(j).initialisatie()];
                if (waarde != null) zetters.get(j).accept(nieuw, waarde);
            }
            int fase = (Integer) w[g.fase()];
            for (Gevolg.Invoegsel i : g.invoegingen())
                switch (i) {
                    case Gevolg.Invoegsel.Nieuw n -> voegIn(drools, n.invoeging(), nieuw);
                    case Gevolg.Invoegsel.Merk m -> voegIn(drools, m.invoeging(),
                            new Gemaakt(g.creatie(), onderwerp, nieuw, fase, d.stratum(), diepte));
                    case Gevolg.Invoegsel.Link l -> voegIn(drools, l.invoeging(), l.nieuwIsEerste()
                            ? new Koppeling(l.relatie(), nieuw, onderwerp, fase)
                            : new Koppeling(l.relatie(), onderwerp, nieuw, fase));
                    case Gevolg.Invoegsel.Kenmerk k -> {
                        if (Boolean.TRUE.equals(v[k.initialisatie()]))
                            voegIn(drools, k.invoeging(),
                                    new HeeftKenmerk(nieuw, g.initialisaties().get(k.initialisatie()).lid()));
                    }
                }
        });
    }

    /** The decision of {@link Gevolg.Afloop}: its value from the bound values, with its fault where the plan names
     *  one; nothing where the value is null; else the facts of the decision, in order. */
    private RuleItemBuilder<?> afloop(Gevolg.Afloop g) {
        Waardefunctie waarde = ClosureWeergave.waardefunctie(g.waarde());
        return consequentie(g.onderwerp(), g.slots(), g.grootte(), (drools, onderwerp, w) -> {
            Object v;
            try {
                v = waarde.van(w);
                if (g.collapse()) v = Rekenhulpen.collapse(v);
            } catch (RuntimeException e) {
                if (g.fout() == null || !Rekenfout.isEvaluatiefout(e)) throw e;
                voegIn(drools, g.invoeging(),
                        new Fout(onderwerp, g.fout().vorm(), Rekenfout.benoem(e, g.fout().regel())));
                return;
            }
            if (v == null) return;
            for (Gevolg.Stapelfeit f : g.feiten()) voegIn(drools, g.invoeging(), stapelfeit(f, onderwerp, v));
        });
    }

    /** The fact {@code f} of a defaults stack on {@code onderwerp}, with {@code v}: the decision, or the key of a
     *  conflict. */
    private static Object stapelfeit(Gevolg.Stapelfeit f, Object onderwerp, Object v) {
        return switch (f) {
            case Gevolg.Stapelfeit.Waarde x -> new Waarde(onderwerp, x.doel(), v, x.schrijver());
            case Gevolg.Stapelfeit.Kenmerk x -> new HeeftKenmerk(onderwerp, x.kenmerk());
            case Gevolg.Stapelfeit.Beslist x -> new Beslist(onderwerp, x.stapel(), x.index());
            case Gevolg.Stapelfeit.Uitkomst x -> x.soort() == Uitkomst.Soort.BESLIST
                    ? new Uitkomst(onderwerp, x.stapel(), x.keten(), x.positie(), Uitkomst.Soort.BESLIST, v, null)
                    : new Uitkomst(onderwerp, x.stapel(), x.keten(), x.positie(), Uitkomst.Soort.CONFLICT, null,
                            (String) v);
            case Gevolg.Stapelfeit.Inconsistentie x -> Rekenhulpen.inconsistentie((String) v, onderwerp);
        };
    }

    /** A new bean of {@code klasse} by its constructor without arguments, every field unset. */
    private static Object nieuwObject(Class<?> klasse) {
        try {
            return klasse.getConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("kon geen " + klasse.getSimpleName() + " maken", e);
        }
    }

    /** The fault of {@link Gevolg.Toetsfout}: the condition evaluated again over the same bound values. */
    private RuleItemBuilder<?> toetsfout(Gevolg.Toetsfout g) {
        Predicate<Object[]> toets = ClosureWeergave.toets(g.toets());
        return consequentie(g.onderwerp(), g.slots(), g.grootte(), (drools, onderwerp, w) -> {
            RuntimeException f = ClosureWeergave.fout(toets, w);
            if (f == null)
                throw new IllegalStateException("de voorwaarde van \"" + g.fout().vorm() + "\" faalde in de toets en niet"
                        + " in het gevolg, over dezelfde gebonden waarden");
            voegIn(drools, g.invoeging(), new Fout(onderwerp, g.fout().vorm(), Rekenfout.benoem(f, g.fout().regel())));
        });
    }

    /** The body of a consequence: the Drools handle, the object, and the bound values at their slots. */
    @FunctionalInterface
    private interface Gevolgfunctie {
        void voer(org.drools.model.Drools drools, Object onderwerp, Object[] waarden);
    }

    /** A consequence over the object {@code onderwerp} binds and the values at {@code slots}, in a value array of
     *  {@code grootte} slots. */
    @SuppressWarnings("rawtypes")
    private RuleItemBuilder<?> consequentie(String onderwerp, List<Integer> slots, int grootte, Gevolgfunctie f) {
        int[] pos = slots.stream().mapToInt(Integer::intValue).toArray();
        Variable[] v = new Variable[pos.length + 1];
        v[0] = bekend(onderwerp);
        for (int j = 0; j < pos.length; j++) v[j + 1] = variabele(pos[j]);
        return new Consequentie(v, (drools, args) -> {
            Object[] w = new Object[grootte];
            for (int j = 0; j < pos.length; j++) w[pos[j]] = args[j + 1];
            f.voer(drools, args[0], w);
        });
    }

    private static void voegIn(org.drools.model.Drools drools, Gevolg.Invoeging invoeging, Object feit) {
        if (invoeging == Gevolg.Invoeging.LOGISCH) drools.logicalInsert(feit);
        else drools.insert(feit);
    }

    /** A consequence over any number of bound variables. Each typed {@code execute} of the DSL sets its builder's block
     *  to a {@link org.drools.model.functions.BlockN} over the Drools handle and the bound facts, and the engine calls
     *  that block whatever its arity; this builder sets the same block directly, so a consequence is not limited by the
     *  arities the DSL spells out. */
    private static final class Consequentie
            extends org.drools.model.consequences.ConsequenceBuilder.AbstractValidBuilder<Consequentie> {
        @SuppressWarnings("rawtypes")
        Consequentie(Variable[] v, BiConsumer<org.drools.model.Drools, Object[]> body) {
            super(v);
            usingDrools = true;
            block = args -> body.accept((org.drools.model.Drools) args[0],
                    java.util.Arrays.copyOfRange(args, 1, args.length));
        }
    }

    private record Weergegeven(Variable<Object> variabele, PatternDSL.PatternDef<Object> def) {}

    @SuppressWarnings("unchecked")
    private Weergegeven patroon(Patroon p) {
        Variable<Object> v = (Variable<Object>) DSL.declarationOf(p.feitklasse(), p.naam());
        PatternDSL.PatternDef<Object> def = PatternDSL.pattern(v);
        for (Alfa a : p.alfa()) def = def.expr(a.id(), predicaat(p.feitklasse(), a));
        for (Join j : p.joins()) def = join(def, p.feitklasse(), j);
        for (Betatoets t : p.betatoetsen()) def = betatoets(def, t);
        for (Alfa a : p.naAlfa()) def = def.expr(a.id(), predicaat(p.feitklasse(), a));
        for (Binding b : p.bindingen()) def = def.bind(bind(b.doel()), lezer(b));
        return new Weergegeven(v, def);
    }

    /** The identity join {@code j} on a pattern over {@code klasse}: the field it names of the pattern's object, or the
     *  object itself, against the object its variable binds, or the reference field it names of that object. */
    private PatternDSL.PatternDef<Object> join(PatternDSL.PatternDef<Object> def, Class<?> klasse, Join j) {
        Function1<Object, Object> links = j.veld() == null ? x -> x : veld(klasse, j.eigenschap());
        Function1<Object, Object> rechts = j.doelveld() == null ? o -> o
                : veld(j.doelveld().klasse(), j.doelveld().eigenschap());
        return zelfde(def, j.id(), bekend(j.doel()), links, rechts);
    }

    /** The constraint {@code t} between the object of {@code def} and the object another pattern binds, unindexed. */
    private PatternDSL.PatternDef<Object> betatoets(PatternDSL.PatternDef<Object> def, Betatoets t) {
        return def.expr(t.id(), bekend(t.doel()), ClosureWeergave.betatoets(t.functie(), t.constanten()));
    }

    /** The existence of {@code b}, in the scope its plan states ({@link #gekwantificeerd}). */
    private RuleItemBuilder<?> bestaat(Bestaat b) {
        PatternDSL.PatternDef<?>[] defs = gekwantificeerd(b.patronen(), b.binnen(), b.buiten());
        return DSL.exists(defs[0], Arrays.copyOfRange(defs, 1, defs.length));
    }

    /** The absence of {@code n}, in the scope its plan states ({@link #gekwantificeerd}). */
    private RuleItemBuilder<?> niet(Niet n) {
        PatternDSL.PatternDef<?>[] defs = gekwantificeerd(n.patronen(), n.binnen(), n.buiten());
        return DSL.not(defs[0], Arrays.copyOfRange(defs, 1, defs.length));
    }

    /** The patterns of an existence or a negation, rendered in the scope its plan states: what it reads from before
     *  ({@code buiten}) is bound, and what it declares ({@code binnen}), the variables of its patterns and those their
     *  bindings bind, is known to the patterns after them inside it and to nothing after it. */
    private PatternDSL.PatternDef<?>[] gekwantificeerd(List<Patroon> patronen, List<String> binnen,
                                                       List<String> buiten) {
        for (String naam : buiten) bekend(naam);
        PatternDSL.PatternDef<?>[] defs = new PatternDSL.PatternDef<?>[patronen.size()];
        for (int i = 0; i < defs.length; i++) {
            Patroon p = patronen.get(i);
            Weergegeven w = patroon(p);
            defs[i] = w.def();
            if (waarden.putIfAbsent(p.naam(), w.variabele()) != null)
                throw new IllegalStateException("de variabele " + p.naam() + " wordt twee keer gebonden");
        }
        // a value bound inside lives only inside: a later quantification may bind its name again, as its plan allows
        for (String naam : binnen) {
            waarden.remove(naam);
            gebonden.remove(naam);
        }
        return defs;
    }

    private RuleItemBuilder<?> accumulatie(Accumulatie a) {
        Weergegeven bron = patroon(a.bron());
        AccumulateFunction[] f = new AccumulateFunction[a.functies().size()];
        for (int i = 0; i < f.length; i++) {
            Accumulatie.Functie fn = a.functies().get(i);
            Variable<Object> over = fn.bron() == null ? bron.variabele() : bekend(fn.bron());
            f[i] = DSL.accFunction(functie(fn.soort()), over).as(bind(fn.doel()));
        }
        return DSL.accumulate(bron.def(), f[0], Arrays.copyOfRange(f, 1, f.length));
    }

    /** Drools' own accumulate function of each fold of the plan: the sum over a statically typed {@code BigDecimal},
     *  since the plain sum over an {@code Object}-bound value gives a {@code Double}. */
    private static Class<?> functie(Accumulatie.Soort soort) {
        String naam = switch (soort) {
            case AANTAL -> "CountAccumulateFunction";
            case SOM -> "BigDecimalSumAccumulateFunction";
            case MINIMUM -> "MinAccumulateFunction";
            case MAXIMUM -> "MaxAccumulateFunction";
            case LIJST -> "CollectListAccumulateFunction";
        };
        try {
            return Class.forName("org.drools.core.base.accumulators." + naam);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("de Drools-accumulatiefunctie " + naam + " ontbreekt op het classpath", e);
        }
    }

    /** The test of the condition over the values at its slots, as a standalone test after the patterns that bind them,
     *  asking of the condition what the plan's role says ({@link ClosureWeergave#toets(Toetsitem)});
     *  a condition that reads nothing is a constant. The analysis refuses a condition over more than
     *  {@link TmsTranspiler#MAX_LEZINGEN} values, the most the DSL's standalone test takes. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private RuleItemBuilder<?> toets(Toetsitem toets) {
        int[] pos = toets.slots().stream().mapToInt(Integer::intValue).toArray();
        int n = toets.grootte();
        Predicate<Object[]> gecompileerd = ClosureWeergave.toets(toets);
        Predicate<Object[]> t = args -> {
            Object[] w = new Object[n];
            for (int j = 0; j < pos.length; j++) w[pos[j]] = args[j];
            return gecompileerd.test(w);
        };
        String id = toets.id();
        Variable[] v = new Variable[pos.length];
        for (int j = 0; j < pos.length; j++) v[j] = variabele(pos[j]);
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
                    + " er meer dan " + TmsTranspiler.MAX_LEZINGEN);
        };
    }

    /** The value variable {@code naam}, which an item rendered before binds: a slot, a join and an accumulate name only
     *  what the plan bound already. */
    private Variable<Object> bekend(String naam) {
        Variable<Object> v = waarden.get(naam);
        if (v == null) throw new IllegalStateException("de variabele " + naam + " is nog niet gebonden");
        return v;
    }

    /** The value variable {@code naam} for the binding that binds it, made here: each is bound once. */
    private Variable<Object> bind(String naam) {
        if (!gebonden.add(naam) || waarden.containsKey(naam))
            throw new IllegalStateException("de variabele " + naam + " wordt twee keer gebonden");
        Variable<Object> v = DSL.declarationOf(Object.class, naam);
        waarden.put(naam, v);
        return v;
    }

    // ---------------- fields ----------------

    /** The read of a field of a fact of the branch, by class and name: the record accessor itself, as the analysis
     *  named it, so that the model calls it directly. A bean is read through its getter ({@link Accessors}). */
    private static final Map<Class<?>, Map<String, Function1<Object, Object>>> VELDEN = new HashMap<>();

    static {
        veld(Waarde.class, "onderwerp", Waarde::onderwerp);
        veld(Waarde.class, "attribuut", Waarde::attribuut);
        veld(Waarde.class, "schrijver", Waarde::schrijver);
        veld(Waarde.class, "waarde", Waarde::waarde);
        veld(Stand.class, "onderwerp", Stand::onderwerp);
        veld(Stand.class, "attribuut", Stand::attribuut);
        veld(Stand.class, "waarde", Stand::waarde);
        veld(KenmerkStand.class, "onderwerp", KenmerkStand::onderwerp);
        veld(KenmerkStand.class, "kenmerk", KenmerkStand::kenmerk);
        veld(KenmerkStand.class, "waar", KenmerkStand::waar);
        veld(HeeftKenmerk.class, "onderwerp", HeeftKenmerk::onderwerp);
        veld(HeeftKenmerk.class, "kenmerk", HeeftKenmerk::kenmerk);
        veld(Emmerinvoer.class, "onderwerp", Emmerinvoer::onderwerp);
        veld(Emmerinvoer.class, "attribuut", Emmerinvoer::attribuut);
        veld(PadStand.class, "onderwerp", PadStand::onderwerp);
        veld(PadStand.class, "pad", PadStand::pad);
        veld(PadStand.class, "lid", PadStand::lid);
        veld(PadStand.class, "waarde", PadStand::waarde);
        veld(ParameterStand.class, "parameter", ParameterStand::parameter);
        veld(ParameterStand.class, "waarde", ParameterStand::waarde);
        veld(Rekendatum.class, "datum", Rekendatum::datum);
        veld(KanaalStand.class, "regel", KanaalStand::regel);
        veld(KanaalStand.class, "inconsistent", KanaalStand::inconsistent);
        veld(Creatiefase.class, "nummer", Creatiefase::nummer);
        veld(Creatiefase.class, "stratum", Creatiefase::stratum);
        veld(Bijdrage.class, "vouw", Bijdrage::vouw);
        veld(Bijdrage.class, "onderwerp", Bijdrage::onderwerp);
        veld(Bijdrage.class, "waarde", Bijdrage::waarde);
        veld(Filterfout.class, "vouw", Filterfout::vouw);
        veld(Filterfout.class, "onderwerp", Filterfout::onderwerp);
        veld(Filterdeponering.class, "vouw", Filterdeponering::vouw);
        veld(Filterdeponering.class, "onderwerp", Filterdeponering::onderwerp);
        veld(Lidfout.class, "vouw", Lidfout::vouw);
        veld(Lidfout.class, "onderwerp", Lidfout::onderwerp);
        veld(Uniciteitssleutel.class, "uniciteit", Uniciteitssleutel::uniciteit);
        veld(Uniciteitssleutel.class, "element", Uniciteitssleutel::element);
        veld(Uniciteitsschending.class, "uniciteit", Uniciteitsschending::uniciteit);
        veld(Uniciteitsschending.class, "element", Uniciteitsschending::element);
        veld(Ontvanger.class, "verdeling", Ontvanger::verdeling);
        veld(Ontvanger.class, "bron", Ontvanger::bron);
        veld(Gemaakt.class, "regel", Gemaakt::regel);
        veld(Gemaakt.class, "onderwerp", Gemaakt::onderwerp);
        veld(Gemaakt.class, "object", Gemaakt::object);
        veld(Fout.class, "regel", Fout::regel);
        veld(Fout.class, "onderwerp", Fout::onderwerp);
        veld(Koppeling.class, "relatie", Koppeling::relatie);
        veld(Koppeling.class, "eerste", Koppeling::eerste);
        veld(Koppeling.class, "tweede", Koppeling::tweede);
        veld(Lijstplaats.class, "veld", Lijstplaats::veld);
        veld(Lijstplaats.class, "plaats", Lijstplaats::plaats);
        veld(Beslist.class, "onderwerp", Beslist::onderwerp);
        veld(Beslist.class, "stapel", Beslist::stapel);
        veld(Beslist.class, "index", Beslist::index);
        veld(Uitkomst.class, "onderwerp", Uitkomst::onderwerp);
        veld(Uitkomst.class, "stapel", Uitkomst::stapel);
        veld(Uitkomst.class, "keten", Uitkomst::keten);
        veld(Uitkomst.class, "positie", Uitkomst::positie);
        veld(Uitkomst.class, "soort", Uitkomst::soort);
        veld(Uitkomst.class, "waarde", Uitkomst::waarde);
        veld(Uitkomst.class, "sleutel", Uitkomst::sleutel);
    }

    @SuppressWarnings("unchecked")
    private static <T> void veld(Class<T> klasse, String naam, Function1<T, Object> lezer) {
        VELDEN.computeIfAbsent(klasse, k -> new HashMap<>()).put(naam, (Function1<Object, Object>) (Function1<?, ?>) lezer);
    }

    /** The read of field {@code naam} of an object of {@code klasse}. */
    private static Function1<Object, Object> veld(Class<?> klasse, String naam) {
        if (klasse.getPackageName().equals(PatternDslWeergave.class.getPackageName())) {
            Function1<Object, Object> f = VELDEN.getOrDefault(klasse, Map.of()).get(naam);
            if (f == null) throw new IllegalStateException("geen lezer voor het veld " + naam + " van " + klasse.getSimpleName());
            return f;
        }
        Function<Object, Object> getter = Accessors.getter(klasse, naam);
        return getter::apply;
    }

    /** The test of one alpha constraint: its field tests in order, the first that fails ends it. */
    private static Predicate1<Object> predicaat(Class<?> klasse, Alfa a) {
        List<Predicate1<Object>> delen = new ArrayList<>();
        for (Alfa.Veldtoets t : a.toetsen()) {
            Function1<Object, Object> f = lezer(klasse, t);
            Object w = t.waarde();
            delen.add(switch (t.op()) {
                case GELIJK -> x -> f.apply(x).equals(w);
                case NIET_GELIJK -> x -> !f.apply(x).equals(w);
                case MINSTENS -> x -> ((Number) f.apply(x)).intValue() >= ((Number) w).intValue();
                case ONDER -> x -> ((Number) f.apply(x)).intValue() < ((Number) w).intValue();
                case BINNEN -> x -> ((Geldigheid) w).bevat((LocalDate) f.apply(x));
                case IS_EEN -> x -> ((Class<?>) w).isInstance(f.apply(x));
                case LEEG -> x -> f.apply(x) == null;
                case GEVULD -> x -> f.apply(x) != null;
                case KLASSE_NIET_IN -> x -> !((Set<?>) w).contains(f.apply(x).getClass());
                case KLASSE_IS -> x -> f.apply(x).getClass() == w;
            });
        }
        if (delen.size() == 1) return delen.get(0);
        return x -> {
            for (Predicate1<Object> d : delen) if (!d.test(x)) return false;
            return true;
        };
    }

    /** The read a field test makes of the pattern's object ({@link Alfa.Veldtoets}): a field as {@link #veld} reads
     *  it, of the declaring class the test names or else of the pattern's, or the object itself, as it is; else along
     *  the path, null where a link is absent, as the test's form reads it. */
    private static Function1<Object, Object> lezer(Class<?> klasse, Alfa.Veldtoets t) {
        Function1<Object, Object> f = t.veld() == null ? x -> x
                : veld(t.declarant() != null ? t.declarant() : klasse, t.eigenschap());
        if (t.pad().isEmpty() && t.vorm() == Binding.Leesvorm.WAARDE) return f;
        return langs(t.pad(), vorm(f::apply, t.vorm(), null));
    }

    /** The read a binding makes: a field of a fact as its record accessor; on a bean the walk over the reference
     *  fields of the path, null where a link is absent, and then the member as the binding reads it. */
    private static Function1<Object, Object> lezer(Binding b) {
        Class<?> klasse = b.veld().klasse();
        if (klasse.getPackageName().equals(PatternDslWeergave.class.getPackageName())) {
            if (!b.pad().isEmpty() || b.vorm() != Binding.Leesvorm.WAARDE)
                throw new IllegalStateException("een feit van de tak wordt gelezen zoals het is: " + b);
            return veld(klasse, b.veld().eigenschap());
        }
        return langs(b.pad(), invoerlezer(b.veld(), b.vorm(), b.soort()));
    }

    /** {@code eind} of the object the walk over the reference fields {@code pad} reaches, null where a link on it is
     *  absent. */
    private static Function1<Object, Object> langs(List<Binding.Veld> pad, Function<Object, Object> eind) {
        if (pad.isEmpty()) return o -> eind.apply(o);
        List<Function<Object, Object>> stappen = new ArrayList<>();
        for (Binding.Veld stap : pad) stappen.add(Accessors.getter(stap.klasse(), stap.eigenschap()));
        return o -> {
            Object d = o;
            for (Function<Object, Object> stap : stappen) {
                d = stap.apply(d);
                if (d == null) return null;
            }
            return eind.apply(d);
        };
    }

    /** The read of member {@code veld} off a bean, as {@code vorm} reads it ({@link Binding.Leesvorm}). */
    static Function<Object, Object> invoerlezer(Binding.Veld veld, Binding.Leesvorm vorm, Class<?> soort) {
        return vorm(Accessors.getter(veld.klasse(), veld.eigenschap()), vorm, soort);
    }

    /** {@code getter} as {@code vorm} reads what it gives: as it is, as whether it holds {@code true}, as the segments
     *  of a timeline, or as whether a reference is set, to an instance of {@code soort} where that is not null. */
    private static Function<Object, Object> vorm(Function<Object, Object> getter, Binding.Leesvorm vorm,
                                                 Class<?> soort) {
        return switch (vorm) {
            case WAARDE -> getter;
            case WAAR -> o -> Boolean.TRUE.equals(getter.apply(o));
            case TIJDVAKKEN -> o -> Tijdvak.lijst((List<?>) getter.apply(o));
            case GEZET -> o -> {
                Object ander = getter.apply(o);
                return ander != null && (soort == null || soort.isInstance(ander));
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

    /** What an identity join ({@link #zelfde}) hashes for a side that reads null: a key of that side alone, so that a
     *  null meets nothing, as in the predicate beside the index. */
    private static final Object NIETS_LINKS = new Object(), NIETS_RECHTS = new Object();

    /**
     * Constrains {@code p} to the facts whose {@code links} is the object that {@code rechts} reads off the object
     * {@code v} binds, by identity, as an indexed join under the constraint id {@code id}. Unindexed, Drools tests every
     * fact of the pattern's class against every tuple on the other side, so that a read of a derived value costs one
     * test per subject and a fire grows with the square of the input (the scale test of card tms-omschakeling,
     * {@code Schaalproef}). Indexed, the memories of the join hash the object, and a fact meets only the tuples of its
     * own object. Drools does not evaluate an indexed constraint but trusts the hash, which compares with {@code equals}
     * and {@code hashCode}: a bean overrides neither ({@code FactModelGenerator}, held by {@code TmsIndexWitnessTest}),
     * so equal is identical there, as {@code ==} in the predicate. One extractor per side gives both the predicate and
     * the key, so the two cannot differ; Drools evaluates the predicate where it does not use the index. A side that
     * reads null meets nothing.
     */
    private static <T, U> PatternDSL.PatternDef<T> zelfde(PatternDSL.PatternDef<T> p, String id, Variable<U> v,
                                                         Function1<T, Object> links, Function1<U, Object> rechts) {
        return p.expr(id, v, (x, o) -> {
            Object a = links.apply(x);
            return a != null && a == rechts.apply(o);
        }, PatternDSL.betaIndexedBy(Object.class, Index.ConstraintType.EQUAL, BETA_INDEX,
                x -> sleutel(links.apply(x), NIETS_LINKS), o -> sleutel(rechts.apply(o), NIETS_RECHTS)));
    }

    private static Object sleutel(Object waarde, Object niets) {
        return waarde == null ? niets : waarde;
    }
}
