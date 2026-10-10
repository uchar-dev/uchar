package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import dev.uchar.regelspraak.cases.CaseRunner.Excluded;
import dev.uchar.regelspraak.engine.BeanCompiler;
import dev.uchar.regelspraak.graaf.ProducentConsument;
import dev.uchar.regelspraak.ir.ResolvedStartpunt;
import dev.uchar.regelspraak.loader.IrLoader;
import dev.uchar.regelspraak.model.RegelSpraakModel;
import dev.uchar.regelspraak.model.RegelSpraakModel.Objecttype;
import dev.uchar.regelspraak.schema.ContractValidator;
import dev.uchar.regelspraak.schema.FactModelGenerator;
import dev.uchar.regelspraak.transpiler.DrlTranspiler;
import dev.uchar.regelspraak.transpiler.kern.Dekking;
import dev.uchar.regelspraak.transpiler.kern.Namen;
import dev.uchar.regelspraak.transpiler.tms.Bewaking;
import dev.uchar.regelspraak.transpiler.tms.Creatiebudget;
import dev.uchar.regelspraak.transpiler.tms.Creatiefase;
import dev.uchar.regelspraak.transpiler.tms.Emmerinvoer;
import dev.uchar.regelspraak.transpiler.tms.Fixpunt;
import dev.uchar.regelspraak.transpiler.tms.Gevuurd;
import dev.uchar.regelspraak.transpiler.tms.Kringen;
import dev.uchar.regelspraak.transpiler.tms.Lijstplaats;
import dev.uchar.regelspraak.transpiler.tms.Teruggezet;
import dev.uchar.regelspraak.transpiler.tms.TmsTranspiler;

import dev.uchar.regelspraak.runtime.Accessors;
import dev.uchar.regelspraak.runtime.Breuk;

import org.drools.modelcompiler.KieBaseBuilder;
import org.kie.api.KieBase;
import org.kie.api.conf.EqualityBehaviorOption;
import org.kie.api.event.rule.AfterMatchFiredEvent;
import org.kie.api.event.rule.AgendaEventListener;
import org.kie.api.event.rule.DefaultAgendaEventListener;
import org.kie.api.runtime.ClassObjectFilter;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.rule.ConsequenceException;
import org.kie.api.runtime.rule.FactHandle;
import org.kie.internal.utils.KieHelper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * The public entry of the Drools backend: one {@code uchar-ir-v1} document in, a built {@link KieBase} out, along
 * the path gate 12 replays. {@link CaseRunner} builds every golden through {@link #build(JsonNode, Branch)}, so what
 * a consumer fires is what the gate plays; there is no second copy of this path to drift from it.
 *
 * <p>The consumer it exists for is the toolchain distribution — CLI, decision service, scenario runner — that
 * builds this repository as a git submodule and fires rule sets through it: a source goes through the frontend
 * ({@code Resolver.resolve}, then {@code IrEmitter.emit}) and the emitted document comes here. What the loader
 * returns beside the model — the ch. 7 prioritized defaults, the role bindings, the Let index, the fold-source
 * paths, the navigation bindings — reaches the transpiler here and nowhere else. A consumer that built the
 * transpiler itself could leave one out and still get a KieBase: a defaults family then fires nowhere and a
 * navigated read is answered as the subject's own, with no marker and no error.
 *
 * <p>The refusals are the replay's own, sentence for sentence: an {@link IrLoader.Refusal} for a form the loader
 * does not carry, an {@link IrLoader.Documentfout} for a document that is not well-formed, and a
 * {@link CaseRunner.Excluded} for a form the backend does not carry past the loader — among them every construct
 * the chosen branch does not lower, so no KieBase is handed out that silently lacks a rule.
 *
 * <p>A session of a build goes through four calls, as the replay makes them ({@link CaseRunner}): a fresh
 * {@link org.kie.api.runtime.KieSession} of {@link Build#kieBase()}, with {@link Build#beanLoader()} as the thread's
 * context class loader, the parameters and the evaluation date ({@code rekendatum}) as globals and on the DRL-text
 * branch also {@link Build#dagsoortGlobalen()}; then {@link #voegIn}, which puts in the fact beans and, on the
 * exec-model branch, what that branch reads beside them; then {@link #vuur}, which fires to the fixpoint under a cap
 * and, for a model with a creation, in generations; then {@link #lees} or {@link #zetAfgeleideWaarden}. On the
 * exec-model branch, which derives with truth maintenance, the beans hold the input alone until the write-back, which
 * comes once, at the end of the session, and closes it: a consumer that fires one session more than once, changing
 * input in between with {@code update}, reads every answer but the last with {@link #lees}, each after a
 * {@link #vuur}: the two readings refuse a session in which something happened outside {@code vuur} after the last
 * one. A session of a model with a creation fires once, since what a creation made is stated and does not lapse: such
 * a consumer opens a fresh session per input. On the DRL-text branch the rules write the beans as they fire, the
 * write-back does nothing and {@link #lees} refuses. A {@code ConsequenceException} out of the fire is named by
 * {@link #benoem}.
 */
public final class DroolsBackend {

    private static final ObjectMapper JSON = new ObjectMapper();

    private DroolsBackend() {}

    /** Which transpiler lowers the document. The exec-model branch ({@code TmsTranspiler}) is the one gate 12's square
     *  replays: it derives with truth maintenance, with logical inserts instead of writing the beans. The DRL-text
     *  branch is narrower — no ch. 7 defaults lowering, more markers — and the gate counts it apart. */
    public enum Branch { EXEC_MODEL, DRL_TEXT }

    /** A built document. Exactly one transpiler is set, the one {@link #branch()} names; {@code dekking} is its
     *  coverage manifest, every entry lowered, because the build refuses a document with any other entry. */
    public record Build(RegelSpraakModel model, ClassLoader beanLoader, TmsTranspiler execModelTranspiler,
                        DrlTranspiler drlTranspiler, List<Dekking> dekking, KieBase kieBase) {
        public Build {
            if ((execModelTranspiler == null) == (drlTranspiler == null))
                throw new IllegalArgumentException("a Build carries exactly one transpiler, got "
                        + (execModelTranspiler == null ? "none" : 2));
        }

        public Branch branch() {
            return execModelTranspiler != null ? Branch.EXEC_MODEL : Branch.DRL_TEXT;
        }

        /** The §9.8 day-kind predicates the DRL text declares as globals (name -> predicate); a session must have
         *  each set before it fires. Empty on the exec-model branch, which wires them into its lambdas. */
        public Map<String, Predicate<LocalDate>> dagsoortGlobalen() {
            return drlTranspiler == null ? Map.of() : drlTranspiler.dagsoortGlobalen();
        }
    }

    /** {@link #build(JsonNode, Branch)} on the document read from {@code irPath}. */
    public static Build build(Path irPath, Branch branch) throws IOException {
        return build(JSON.readTree(Files.readString(irPath)), branch);
    }

    /** Load {@code document}, refuse the forms the backend does not carry, generate and contract-check the fact
     *  beans, lower every construct through {@code branch}'s transpiler with every side table the loader returns,
     *  and build the {@link KieBase}.
     *
     *  @throws IrLoader.Refusal      a form the loader does not carry; the replay reports it as {@code loader: …}
     *  @throws IrLoader.Documentfout a document that is not well-formed {@code uchar-ir-v1}
     *  @throws CaseRunner.Excluded   a form the backend does not carry past the loader: two declarations on one
     *      Java name of the bean model, a reciprocal self-relation the bean model cannot hold, a bean-contract
     *      violation, a construct the branch does not lower, on the DRL-text branch a prioritized-defaults
     *      extension or a DRL text the Drools compiler rejects, and on the exec-model branch a cycle of the
     *      dependency graph that WF-045 or WF-046 forbids */
    public static Build build(JsonNode document, Branch branch) {
        Objects.requireNonNull(branch, "branch");
        IrLoader.Loaded loaded = IrLoader.loadAll(document);   // the model plus the ch. 7 prioritized-defaults extension beside it
        RegelSpraakModel model = loaded.model();
        weigerBeannaambotsing(loaded);

        // A form the v0 backend cannot REPLAY is refused up front — a LOUD per-form exclusion, never a
        // silent value-divergence in the gate. Carrying it to fire only to fault or misvalue there is
        // honestly NOT-CARRIED, not a semantic exec-model difference, so it belongs in the auto-excluded
        // channel with a reason, not the value-divergence list whose banner claims a genuine ref/clingo delta.
        // (§13.4.6 objectcreatie was refused here until 2026-08-30; the creation consequence now executes —
        // both back-link ends written, the kenmerkinitialisatie honoured — so the shapes it still cannot carry
        // are refused per creation by the transpiler, which knows WHICH one, not blanket for every model.)
        //   §13.3.9.2 name-sharing bean limit — leden() ports every member through a name-dedup (attribute >
        //     kenmerk > reference), so a declared member sharing the reciprocal role's name displaces the
        //     self-pair reference field; the mirror pass would then read a Boolean or set an impossible type,
        //     an unclassified ERROR at best. Refuse the shape loudly instead (lens finding, 2026-08-30).
        for (Objecttype ot : model.objecttypen.values()) {
            if (!FactModelGenerator.noemtWederkerig(model, ot)) continue;
            String zelf = FactModelGenerator.zelfpaarRol(model, ot);
            //   the UNDERIVABLE half of the same shape — an ambiguous pair (two wederkerig feittypen on one
            //   objecttype) or a to-many reciprocal role: the transpilers navigate by the written role name
            //   toward a getter no bean carries, an unclassified fire-time ERROR (lens 2, 2026-08-30).
            if (zelf == null)
                throw new Excluded("wederkerig self-relation (§13.3.9.2) on " + ot.naam + " derives no scalar "
                        + "reference field (an ambiguous or to-many self-pair role) — not carried by the v0 backend");
            if (!FactModelGenerator.heeftReferentieVeld(model, ot, zelf, ot))
                throw new Excluded("wederkerig self-pair role '" + zelf + "' on " + ot.naam + " shares its name "
                        + "with a declared member — the bean cannot carry the self-typed reference field "
                        + "(name-sharing bean limit)");
        }

        Map<String, String> bronnen = FactModelGenerator.genereer(model);
        ClassLoader cl = BeanCompiler.compileer(bronnen);
        List<String> schendingen = ContractValidator.valideer(model, cl);
        if (!schendingen.isEmpty()) throw new Excluded("contract: " + schendingen.get(0));

        ClassLoader vorige = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(cl);
        try {
            return switch (branch) {
                case EXEC_MODEL -> execModelTak(loaded, cl);
                case DRL_TEXT -> drlTak(loaded, cl);
            };
        } finally {
            Thread.currentThread().setContextClassLoader(vorige);
        }
    }

    /** Two declarations on one Java name of the bean model, refused before a bean is generated, in the sentences of
     *  {@link FactModelGenerator}: an object type whose fact class another type, a class the generator writes itself
     *  or a {@code java.lang} type already names; two roles on one reference field of one type; and a member whose
     *  bean property belongs to another declaration, where a form of the rule layer reads or writes it (the forms of
     *  {@link ProducentConsument}, and a §9.9 start-point determination, which names its anchor member). Such a member
     *  that nothing uses is left alone: its field is simply absent, and a case that names it is refused by the case
     *  reader. Here and not in the loader, because the rule graph and every other reading of the loaded model keep
     *  the two names apart; only the bean model does not. */
    private static void weigerBeannaambotsing(IrLoader.Loaded loaded) {
        RegelSpraakModel model = loaded.model();
        String klasse = FactModelGenerator.klassebotsing(model);
        if (klasse != null) throw new Excluded(klasse);
        List<FactModelGenerator.Verdringing> verdringingen = FactModelGenerator.verdringingen(model);
        if (verdringingen.isEmpty()) return;
        String rol = FactModelGenerator.rolbotsing(verdringingen);
        if (rol != null) throw new Excluded(rol);
        Map<ProducentConsument.ElementSleutel, String> gebruik = new java.util.HashMap<>();
        for (ProducentConsument.Vorm v : ProducentConsument.vormen(model, loaded.prioritizedDefaults(),
                ProducentConsument.wireNavigatie(loaded))) {
            String wie = v.soort().name().toLowerCase(java.util.Locale.ROOT) + " '" + v.naam() + "'";
            for (ProducentConsument.ElementSleutel e : v.schrijft()) gebruik.putIfAbsent(e, wie);
            for (ProducentConsument.ElementSleutel e : v.leest()) gebruik.putIfAbsent(e, wie);
        }
        for (ResolvedStartpunt sp : model.resolvedStartpunten)
            gebruik.putIfAbsent(ProducentConsument.elementVan(model.objecttypeOpNaam(sp.objecttype()), sp.lid(), false),
                    "de startpuntbepaling van tijdlijn '" + sp.tijdlijn() + "'");
        for (FactModelGenerator.Verdringing v : verdringingen) {
            FactModelGenerator.Herkomst h = v.verdrongen();
            // a role without a field of its own is refused where it is used, by the gate that asks the field's name
            // and type (FactModelGenerator.heeftReferentieVeld); rolbotsing above took the one shape that gate passes
            if (h.soort() == FactModelGenerator.Herkomst.Soort.ROL) continue;
            String wie = gebruik.get(ProducentConsument.ElementSleutel.van(h.bron(), h.naam(),
                    h.soort() == FactModelGenerator.Herkomst.Soort.KENMERK));
            if (wie != null) throw new Excluded(FactModelGenerator.verdringingReden(v) + "; " + wie + " leest of schrijft het");
        }
    }

    /** Two rules of one name in one package: the KieBase keeps one rule per name, so the second would replace the first
     *  and the document would play without it (measured 2026-10-02 on two forms of one name, which WF-020 forbids and a
     *  hand-written document can still carry). The DRL-text branch's compiler refuses the same document with
     *  "Duplicate rule name"; the exec-model branch refuses it in those words, after its {@code tak}, before the
     *  KieBase is built. A rule without a name (a nameless form, WF-107) has no name to share and is not counted; the
     *  exec-model branch refuses a nameless creation from its manifest ({@code TakgelijkheidWitnessTest}). */
    private static void weigerDubbeleRegelnaam(org.drools.model.Model model, String tak) {
        java.util.Set<String> namen = new java.util.HashSet<>();
        for (org.drools.model.Rule r : model.getRules())
            if (r.getName() != null && !namen.add(r.getFullName()))
                throw new Excluded(tak + ": Duplicate rule name: " + r.getName() + " — twee regels van één naam in "
                        + "pakket " + r.getPackage() + "; de KieBase houdt één regel per naam, zodat de tweede de eerste "
                        + "zou vervangen (vormnamen zijn uniek, WF-020)");
    }

    /** The exec-model branch: {@link TmsTranspiler} lowers the forms it carries to an executable model that derives with
     *  truth maintenance, with logical inserts. The KieBase runs in EQUALITY mode, as the branch's design requires:
     *  there a fact inserted as stated takes over an equal logical fact, where IDENTITY mode keeps the two as two facts
     *  on one key and ignores a delete of the stated one in some states. Equal logical facts merge into one fact with a
     *  justification per rule in either mode; that merge rests on the equals and hashCode of the branch's fact classes.
     *  A form the transpiler does not lower is refused from its manifest, in the {@code transpiler SKIP:} sentence the
     *  toolchain reads; a cycle of the dependency graph that WF-045 or WF-046 forbids is refused before the KieBase is
     *  built, since truth maintenance gives such a cycle no single fixpoint ({@code Kringen}). */
    private static Build execModelTak(IrLoader.Loaded loaded, ClassLoader cl) {
        TmsTranspiler t = TmsTranspiler.build(loaded.model(), cl, loaded.prioritizedDefaults(), loaded.rolBindingen(),
                loaded.lets(), loaded.bronPaden(), loaded.navigatieBindingen());
        for (Dekking d : t.dekking())
            if (!d.lowered()) throw new Excluded("transpiler " + d.verdict() + ": " + d.sleutel()
                    + (d.reden().isEmpty() ? "" : " — " + d.reden()));
        // past the manifest every form of the document is one this branch lowers, so the graph over all forms is the
        // graph over the forms it carries
        String kring = Kringen.weigering(loaded);
        if (kring != null) throw new Excluded(kring);
        weigerDubbeleRegelnaam(t.model(), "exec-model");
        KieBase kieBase = KieBaseBuilder.createKieBaseFromModel(t.model(), EqualityBehaviorOption.EQUALITY);
        return new Build(loaded.model(), cl, t, null, List.copyOf(t.dekking()), kieBase);
    }

    /**
     * Puts {@code invoer} into {@code sessie}, a fresh session of {@code build}: the fact beans, and on the
     * exec-model branch what that branch reads beside them and the bean model does not hold, as that branch's own
     * facts, so that a consumer names it in the words of the model and states none of them. That is an
     * {@code Emmerinvoer} for every bucket scalar {@code invoer} names ({@link Invoer#emmerscalair}), without which the
     * branch reads an input timeline as a full timeline (RUNTIME-SEMANTICS ch. 8.4); and where a rule of the model reads
     * the order of a to-many list ({@code TmsTranspiler.lijstvelden}), a {@code Lijstplaats} for every member of every
     * such list, in the order {@link Invoer#lijst} gives or else in the order of the beans. On the DRL-text branch it
     * inserts the beans alone, since that branch reads nothing else.
     *
     * <p>On the exec-model branch it checks all of it before it inserts anything, and refuses by name: a session
     * that already holds a fact, since the places of a list are numbered over the beans of one input and a bean
     * inserted without this call has none; a bean that is not of the build's bean model or that comes twice; a reference
     * field of a bean that holds an object outside the input, named with the bean, the field and the object's type, since
     * the branch derives nothing for an object outside the session and every value read through the field would stay
     * empty without a word; a bucket
     * scalar on a bean outside the input or on a member its type does not declare as an attribute on a timeline; and a
     * list that names a bean outside the input, a member whose reference field {@code rol} does not hold the holder, a
     * member twice, or not every bean of the input whose field of that relation holds the holder, or that comes twice. A
     * place the rules need is thus never missing at the fire, where it would stop the run with an
     * {@code IllegalStateException}.
     *
     * <p>Then it puts the watch of the entry on the session ({@code Bewaking}), with the relations the beans hold now,
     * and {@link #vuur} refuses a session in which more happened outside it than an update of an attribute or a
     * characteristic of a bean this call put in: the places of a list and the bucket scalars are stated over the beans
     * and the relations of one input, so a bean inserted later has neither, and a relation changed later keeps the
     * place it had or has none. A consumer opens a fresh session to add or remove an object or to change a relation.
     *
     * @throws IllegalStateException    {@code sessie} is not fresh, on the exec-model branch
     * @throws IllegalArgumentException a statement of {@code invoer} that does not fit its beans or the model, on the
     *                                  exec-model branch
     */
    public static void voegIn(Build build, KieSession sessie, Invoer invoer) {
        Objects.requireNonNull(build, "build");
        Objects.requireNonNull(sessie, "sessie");
        Objects.requireNonNull(invoer, "invoer");
        List<Object> bonen = invoer.beans();
        if (build.branch() == Branch.DRL_TEXT) {
            for (Object boon : bonen) sessie.insert(boon);
            return;
        }
        TmsTranspiler tms = build.execModelTranspiler();
        if (sessie.getFactCount() != 0)
            throw new IllegalStateException("voegIn vult een verse sessie, maar deze houdt al " + sessie.getFactCount()
                    + " feiten: de plaatsen van een lijst worden over de beans van één invoer geteld, en een bean die"
                    + " zonder voegIn in de sessie kwam, heeft er geen; geef alle beans in één Invoer aan een verse"
                    + " sessie, en verander tussen twee fires hun waarden met update");
        Map<Object, Objecttype> typen = new IdentityHashMap<>();
        // a case of ten thousand objects has a handful of bean classes
        Map<Class<?>, Objecttype> perKlasse = new HashMap<>();
        for (Object boon : bonen)
            if (typen.put(boon, perKlasse.computeIfAbsent(boon.getClass(), k -> objecttype(build, boon))) != null)
                throw new IllegalArgumentException("voegIn: de bean " + naam(boon) + " staat twee keer in de invoer");
        String buiten = doelBuitenDeInvoer("voegIn", build, bonen, typen, perKlasse);
        if (buiten != null) throw new IllegalArgumentException(buiten);
        List<Object> feiten = new ArrayList<>(bonen);
        for (Invoer.Emmer e : invoer.emmers()) feiten.add(emmerinvoer(typen, e));
        feiten.addAll(lijstplaatsen(build, tms.lijstvelden(), bonen, typen, invoer.lijsten()));
        for (Object feit : feiten) sessie.insert(feit);
        Bewaking.op(sessie, relatievelden(build, bonenklassen(build)), bonen, "DroolsBackend.voegIn");
    }

    /** The sentence that refuses a reference field of a bean of {@code bonen} that holds an object outside
     *  {@code bonen}, with {@code wie} the call that refuses, or null where every such field holds one of them or none.
     *  The branch derives only for the objects in the session, so every value of such an object, and every value read
     *  through the field, would stay empty without a word; and the link the field gives still counts, so a creation it
     *  blocks makes no object in its place. */
    private static String doelBuitenDeInvoer(String wie, Build build, List<Object> bonen, Map<Object, Objecttype> typen,
                                             Map<Class<?>, Objecttype> perKlasse) {
        Map<Objecttype, List<String>> velden = new HashMap<>();
        for (Object boon : bonen)
            for (String veld : velden.computeIfAbsent(typen.get(boon), t -> referentievelden(build.model(), t))) {
                Object doel = Accessors.getter(boon.getClass(), veld).apply(boon);
                if (doel != null && !typen.containsKey(doel))
                    return wie + ": het veld '" + veld + "' van " + naam(boon) + " wijst naar een "
                            + perKlasse.computeIfAbsent(doel.getClass(), k -> objecttype(build, doel)).naam + " ("
                            + naam(doel) + ") die niet als bean in de invoer staat: de tak leidt alleen af voor de"
                            + " objecten in de sessie, dus geef elk object waarnaar een bean wijst ook zelf als bean op";
            }
        return null;
    }

    /** {@link #doelBuitenDeInvoer} over the beans {@code sessie} holds, the check {@link #vuur} repeats before it fires:
     *  between two fires an update can set a reference field to an object outside the session. It asks the session per
     *  bean class, since Drools keeps the facts of a class apart and hands out only those for a
     *  {@link ClassObjectFilter}, where a filter of another kind walks every fact the session holds. */
    private static String doelBuitenDeSessie(Build build, KieSession sessie, Map<Class<?>, Objecttype> klassen) {
        List<Object> bonen = new ArrayList<>();
        Map<Object, Objecttype> typen = new IdentityHashMap<>();
        for (Map.Entry<Class<?>, Objecttype> k : klassen.entrySet())
            for (Object o : sessie.getObjects(new ClassObjectFilter(k.getKey())))
                // the filter of a supertype hands out the beans of its subtypes too, which their own class counts
                if (o.getClass() == k.getKey() && typen.put(o, k.getValue()) == null) bonen.add(o);
        return doelBuitenDeInvoer("vuur", build, bonen, typen, new HashMap<>(klassen));
    }

    /** The bean class of every object type of the model of {@code build}, with that object type. */
    private static Map<Class<?>, Objecttype> bonenklassen(Build build) {
        Map<Class<?>, Objecttype> uit = new HashMap<>();
        for (Objecttype ot : build.model().objecttypen.values())
            try {
                uit.put(Class.forName(FactModelGenerator.PAKKET + "." + Namen.ident(ot.naam, true), false,
                        build.beanLoader()), ot);
            } catch (ClassNotFoundException e) {
                throw new IllegalStateException("geen beanklasse voor objecttype " + ot.naam, e);
            }
        return uit;
    }

    /** The relations of every bean class of {@code klassen}: its reference fields, each with its getter, which the
     *  watch of the entry reads to tell an update of an attribute or a characteristic from one of a relation. */
    private static Map<Class<?>, Bewaking.Relatievelden> relatievelden(Build build, Map<Class<?>, Objecttype> klassen) {
        Map<Class<?>, Bewaking.Relatievelden> uit = new HashMap<>();
        for (Map.Entry<Class<?>, Objecttype> k : klassen.entrySet()) {
            List<String> velden = referentievelden(build.model(), k.getValue());
            List<Function<Object, Object>> lezers = new ArrayList<>();
            for (String veld : velden) lezers.add(Accessors.getter(k.getKey(), veld));
            uit.put(k.getKey(), new Bewaking.Relatievelden(velden, lezers));
        }
        return uit;
    }

    /** The beans {@code sessie} holds, of the classes of {@code klassen}, asked per class. */
    private static List<Object> bonenInDeSessie(KieSession sessie, Map<Class<?>, Objecttype> klassen) {
        List<Object> uit = new ArrayList<>();
        for (Class<?> k : klassen.keySet())
            for (Object o : sessie.getObjects(new ClassObjectFilter(k)))
                // the filter of a supertype hands out the beans of its subtypes too, which their own class counts
                if (o.getClass() == k) uit.add(o);
        return uit;
    }

    /** The object type whose bean {@code boon} is, a bean of the build's bean model. */
    private static Objecttype objecttype(Build build, Object boon) {
        for (Objecttype ot : build.model().objecttypen.values()) {
            if (!Namen.ident(ot.naam, true).equals(boon.getClass().getSimpleName())) continue;
            try {
                if (Class.forName(FactModelGenerator.PAKKET + "." + Namen.ident(ot.naam, true), false,
                        build.beanLoader()) == boon.getClass()) return ot;
            } catch (ClassNotFoundException e) {
                throw new IllegalStateException("geen beanklasse voor objecttype " + ot.naam, e);
            }
        }
        throw new IllegalArgumentException("voegIn: " + naam(boon) + " is geen bean van dit model: een bean is een"
                + " instantie van een klasse die Build.beanLoader() laadt");
    }

    /** The {@code Emmerinvoer} of bucket scalar {@code e}, keyed as {@code ProducentConsument.elementVan} keys the
     *  attribute, on the type that declares it. */
    private static Emmerinvoer emmerinvoer(Map<Object, Objecttype> typen, Invoer.Emmer e) {
        Objecttype ot = typen.get(e.bean());
        if (ot == null)
            throw new IllegalArgumentException("voegIn: emmerscalair '" + e.attribuut() + "' op " + naam(e.bean())
                    + ", een bean die niet in de invoer staat");
        if (ot.attribuut(e.attribuut()) == null)
            throw new IllegalArgumentException("voegIn: emmerscalair '" + e.attribuut() + "' op " + naam(e.bean())
                    + ", maar " + ot.naam + " heeft geen attribuut van die naam");
        if (!Namen.bevatNaam(ot.granulariteitLeden(), e.attribuut()))
            throw new IllegalArgumentException("voegIn: emmerscalair '" + e.attribuut() + "' op " + naam(e.bean())
                    + ", maar dat attribuut van " + ot.naam + " ligt niet op een tijdlijn; alleen de invoer van een"
                    + " tijdlijn kent de emmerconventie (RUNTIME-SEMANTICS ch. 8.4)");
        return new Emmerinvoer(e.bean(), ProducentConsument.elementVan(ot, e.attribuut(), false));
    }

    /** One to-many list: the reference field {@code veld} its members hold toward {@code houder}, and the relation that
     *  field stands for on the members' bean ({@link FactModelGenerator#herkomst}), since beans of two types can hold
     *  fields of one name toward one holder over two fact types, and a fold reads the members of one relation. The
     *  holder is compared by identity, as the beans do not override {@code equals}. */
    private record Lijstsleutel(String veld, Object houder, FactModelGenerator.Herkomst relatie) {}

    /**
     * The places of the members of every to-many list a rule reads ({@code gelezen}, the fields the members hold toward
     * the holder): per field, holder and relation, the beans of the input whose field holds the holder, in the order of
     * the beans, or in the order of the list {@code opgegeven} gives for that holder. A given list is checked against the
     * beans whether or not a rule reads its field, and counts only where one does; split by the relations of its
     * members, the order kept, it must name every bean of the input whose field of that relation holds the holder.
     */
    private static List<Lijstplaats> lijstplaatsen(Build build, Set<String> gelezen, List<Object> bonen,
                                                   Map<Object, Objecttype> typen, List<Invoer.Lijst> opgegeven) {
        RegelSpraakModel model = build.model();
        Set<String> velden = new HashSet<>(gelezen);
        for (Invoer.Lijst l : opgegeven) velden.add(Namen.ident(l.rol(), false));
        Map<Objecttype, List<String>> referentievelden = new HashMap<>();
        Map<Objecttype, Map<String, FactModelGenerator.Herkomst>> relaties = new HashMap<>();
        Function<Object, Function<String, FactModelGenerator.Herkomst>> relatie = boon -> veld ->
                relaties.computeIfAbsent(typen.get(boon), t -> new HashMap<>())
                        .computeIfAbsent(veld, v -> FactModelGenerator.herkomst(model, typen.get(boon), v));
        // per list, the beans whose field holds the holder, in the order of the beans
        Map<Lijstsleutel, List<Object>> werkelijk = new LinkedHashMap<>();
        for (Object boon : bonen)
            for (String veld : referentievelden.computeIfAbsent(typen.get(boon), t -> referentievelden(model, t))) {
                if (!velden.contains(veld)) continue;
                Object houder = Accessors.getter(boon.getClass(), veld).apply(boon);
                if (houder != null)
                    werkelijk.computeIfAbsent(new Lijstsleutel(veld, houder, relatie.apply(boon).apply(veld)),
                            k -> new ArrayList<>()).add(boon);
            }
        Map<Lijstsleutel, List<Object>> lijsten = new LinkedHashMap<>();
        werkelijk.forEach((k, leden) -> { if (gelezen.contains(k.veld())) lijsten.put(k, leden); });
        Set<Lijstsleutel> gegeven = new HashSet<>();
        for (Invoer.Lijst l : opgegeven) {
            String veld = Namen.ident(l.rol(), false);
            String wat = "voegIn: de lijst van " + naam(l.houder()) + " onder de rol '" + l.rol() + "'";
            Map<Lijstsleutel, List<Object>> delen = new LinkedHashMap<>();
            Set<Object> genoemd = java.util.Collections.newSetFromMap(new IdentityHashMap<>());
            for (Object lid : l.leden()) {
                Objecttype ot = typen.get(lid);
                if (ot == null) throw new IllegalArgumentException(wat + " noemt " + naam(lid) + ", dat geen bean van de invoer is");
                if (!referentievelden.computeIfAbsent(ot, t -> referentievelden(model, t)).contains(veld))
                    throw new IllegalArgumentException(wat + " noemt " + naam(lid) + ", maar een " + ot.naam
                            + " heeft geen referentieveld '" + veld + "'; de rol is die van de houder, de naam van het"
                            + " veld waarmee een lid naar de houder wijst");
                Object houder = Accessors.getter(lid.getClass(), veld).apply(lid);
                if (houder != l.houder())
                    throw new IllegalArgumentException(wat + " noemt " + naam(lid) + ", maar diens veld '" + veld
                            + "' wijst " + (houder == null ? "nergens heen" : "naar " + naam(houder)));
                if (!genoemd.add(lid)) throw new IllegalArgumentException(wat + " noemt " + naam(lid) + " twee keer");
                delen.computeIfAbsent(new Lijstsleutel(veld, houder, relatie.apply(lid).apply(veld)),
                        k -> new ArrayList<>()).add(lid);
            }
            if (delen.isEmpty())
                for (Map.Entry<Lijstsleutel, List<Object>> e : werkelijk.entrySet())
                    if (e.getKey().veld().equals(veld) && e.getKey().houder() == l.houder())
                        throw new IllegalArgumentException(wat + " is leeg gegeven, maar " + naam(e.getValue().get(0))
                                + " wijst met zijn veld '" + veld + "' naar de houder");
            for (Map.Entry<Lijstsleutel, List<Object>> deel : delen.entrySet()) {
                if (!gegeven.add(deel.getKey())) throw new IllegalArgumentException(wat + " is twee keer gegeven");
                for (Object lid : werkelijk.getOrDefault(deel.getKey(), List.of()))
                    if (!genoemd.contains(lid))
                        throw new IllegalArgumentException(wat + " mist " + naam(lid) + ", dat met zijn veld '" + veld
                                + "' naar de houder wijst: een lid buiten de gegeven volgorde heeft geen plaats");
                if (gelezen.contains(veld)) lijsten.put(deel.getKey(), deel.getValue());
            }
        }
        List<Lijstplaats> uit = new ArrayList<>();
        lijsten.forEach((k, leden) -> {
            for (int i = 0; i < leden.size(); i++) uit.add(new Lijstplaats(leden.get(i), k.veld(), i));
        });
        return uit;
    }

    /** The reference fields of a bean of {@code ot}, its inherited ones among them: the members the bean model derives
     *  whose type is the bean class of an object type (§13.3.9, {@link FactModelGenerator#leden}). */
    private static List<String> referentievelden(RegelSpraakModel model, Objecttype ot) {
        Set<String> klassen = new HashSet<>();
        for (Objecttype t : model.objecttypen.values()) klassen.add(Namen.ident(t.naam, true));
        List<String> uit = new ArrayList<>();
        for (Objecttype t : ot.keten())
            for (FactModelGenerator.Lid lid : FactModelGenerator.leden(model, t))
                if (klassen.contains(lid.javaType())) uit.add(lid.eigenschap());
        return uit;
    }

    /** An object in a sentence of {@link #voegIn}: its type and its identity, as the facts of the branch print it. */
    private static String naam(Object o) {
        return o.getClass().getSimpleName() + "@" + Integer.toHexString(System.identityHashCode(o));
    }

    /**
     * What stands in {@code sessie} of the exec-model branch after a {@link #vuur} that reached its fixpoint,
     * read without writing anything on a bean: per object the derived attribute values, in the form the bean field takes
     * them, a timeline as the bean model's segments, and the derived characteristics; the objects and links the
     * creations made; the evaluation faults that stand, as data and not thrown; and the §9.5 deposits, the
     * {@code Inconsistentie} facts. A consumer that fires one session of a model without a creation more than once reads
     * every answer but the last here: the beans hold the input alone until {@link #zetAfgeleideWaarden}, which writes
     * exactly this, and after which the session does not fire again. A fault names its object by type and identity,
     * and by its label in {@link #lees(Build, KieSession, Function)}.
     *
     * <p>It reads only a session whose last {@link #vuur} reached its fixpoint and in which nothing happened since outside
     * {@code vuur}: no fact inserted, updated or deleted, and no rule fired, by a plain {@code fireAllRules} among others.
     * What it gives is thus always a fixpoint, that of the input as it stands. After an {@code update}, a consumer fires
     * with {@code vuur} before it reads again; where {@code vuur} refuses the session, the sentence says to open a fresh
     * one.
     *
     * @throws UnsupportedOperationException on the DRL-text branch, whose rules write the derived values on the beans
     *                                       as they fire
     * @throws IllegalStateException         a session that was not fired through {@link #vuur}, among them one fired
     *                                       with a plain {@code fireAllRules}, whose last {@code vuur} ended without a
     *                                       fixpoint ({@link Vuring#geenFixpunt}) or with an exception, or in which a
     *                                       fact was inserted, updated or deleted, or a rule fired, after the last
     *                                       {@code vuur} and outside it, or a global or, without an update, a relation
     *                                       changed; the sentence says which, and what to do
     */
    public static Fixpunt lees(Build build, KieSession sessie) {
        return lezing(build, sessie, null);
    }

    /** As {@link #lees(Build, KieSession)}, with {@code label} giving the case label of an input bean and null for an
     *  object a creation made, so that a fault in the {@link Fixpunt} that names its object names it as the reference
     *  interpreter does: "V, een Vondst" for two writers that disagree (RUNTIME-SEMANTICS ch. 5.7) and for a timeline
     *  input in the other convention (ch. 8.4), "een aangemaakte Vondst" for a made object, where the variant without
     *  labels gives the type and the identity. The names are those {@link #zetAfgeleideWaarden(Build, KieSession,
     *  Function)} gives the faults it throws. */
    public static Fixpunt lees(Build build, KieSession sessie, Function<Object, String> label) {
        Objects.requireNonNull(label, "label");
        return lezing(build, sessie, label);
    }

    private static Fixpunt lezing(Build build, KieSession sessie, Function<Object, String> label) {
        Objects.requireNonNull(build, "build");
        Objects.requireNonNull(sessie, "sessie");
        if (build.branch() != Branch.EXEC_MODEL)
            throw new UnsupportedOperationException("lees leest wat de tak met truth maintenance afleidde; op de "
                    + build.branch() + "-tak schrijven de regels de afgeleide waarden al tijdens het vuren op de beans");
        return build.execModelTranspiler().lees(sessie, label);
    }

    /**
     * Puts what the exec-model branch derived in {@code sessie} on the fact beans, after the last {@link #vuur}
     * of the session, which reached its fixpoint: exactly what {@link #lees} gives, read by the same code. It sets each
     * derived characteristic and attribute value on its bean, and each link a creation laid on the reference fields that
     * hold it (§13.3.9). Until this call the beans hold the input alone, since that branch writes no bean while it fires.
     * On the DRL-text branch it does nothing, since its rules write the beans as they fire.
     *
     * <p>Before anything is written, it throws the evaluation faults that stand at the fixpoint, and then writes nothing:
     * one fault as its own exception, which is a {@link Breuk.DelingDoorNul} for a division by a numeric zero (an
     * {@code ArithmeticException}, named after the rule with the operands, as the DRL-text branch's fire gives it
     * through {@link #benoem}), a {@code Rekenfout} for a fault of a computation or of the evaluation date, a
     * {@code Schrijversconflict} for two writers of one attribute that disagree (an {@code IllegalStateException},
     * RUNTIME-SEMANTICS ch. 5.7) and an {@code Invoervormfout} for a timeline input in the other convention (ch. 8.4);
     * two or more as {@code Evaluatiefouten}, ordered by the name of the form and then by the sentence, with the faults
     * as {@code fouten()}. {@link #lees} gives the same faults as data.
     *
     * <p>It refuses the sessions {@link #lees} refuses, with the same {@code IllegalStateException}, before anything is
     * written: one not fired through {@link #vuur}, one whose last {@code vuur} ended without a fixpoint, and one in which
     * something happened outside {@code vuur} after the last one.
     *
     * <p>The write-back tells the engine nothing, and a bean then holds derived values beside its input, which a further
     * fire would read as input: an initialisation of a member that holds a value written back would not fire again. So
     * the write-back closes the session with a stated mark, and {@link #vuur} refuses it.
     */
    public static void zetAfgeleideWaarden(Build build, KieSession sessie) {
        Objects.requireNonNull(build, "build");
        Objects.requireNonNull(sessie, "sessie");
        if (build.branch() == Branch.EXEC_MODEL) build.execModelTranspiler().zetAfgeleideWaarden(sessie);
    }

    /** As {@link #zetAfgeleideWaarden(Build, KieSession)}, with {@code label} giving the case label of an input bean
     *  and null for an object a creation made, so that a fault names its object as the reference interpreter names it:
     *  "V, een Vondst" in the fault of two writers that disagree (RUNTIME-SEMANTICS ch. 5.7), where a consumer without
     *  labels gets the type and the identity. The replay writes back through it. */
    public static void zetAfgeleideWaarden(Build build, KieSession sessie, Function<Object, String> label) {
        Objects.requireNonNull(build, "build");
        Objects.requireNonNull(sessie, "sessie");
        Objects.requireNonNull(label, "label");
        if (build.branch() == Branch.EXEC_MODEL) build.execModelTranspiler().zetAfgeleideWaarden(sessie, label);
    }

    /** The fixed fire cap of the DRL-text branch: its rules write the beans as they fire, and the cap ends a run whose
     *  rules keep overwriting each other. */
    static final int VAST_PLAFOND = 100_000;

    /**
     * The factor of the fire cap of the exec-model branch, {@code FACTOR × R × N0} ({@link #vuur}), with N0 the
     * facts before the fire and, for a model with a creation, the facts at the start of each phase. Every firing of that
     * branch makes or renews a logical fact of one rule on one subject, and without a cycle, which the build refuses
     * ({@code Kringen}), the renewals per rule and subject are bounded by the depth of the dependency graph, so a run
     * fires a number of times that grows with the rules and the facts. It is twice the largest ratio measured, rounded
     * up, and at least 4. Measured 2026-10-10 (card {@code tms-omschakeling}, step 4): over the 248 goldens the branch
     * plays ({@code TmsMeting}), a case needs at most 1.167 firings per rule per fact the cap counts
     * ({@code anchor/jutterswet-maatschap}), counted per stretch of its run up to the start of the next creation phase;
     * the scale model ({@code Schaalproef}) fires 0.563 times per rule per fact before its fire. Counted against the
     * facts before the fire alone, a chain of creations from one input object reaches 48.7
     * ({@code synth2-budgetgrens-onder}) while it uses a fiftieth of its cap, since the cap counts what each phase made;
     * twice that ratio would loosen the cap of every model 24-fold, so it does not set the factor.
     */
    static final int FACTOR = 4;

    /** The least fire cap of the exec-model branch: below it a small document would get a cap of a few hundred
     *  firings, which a fixpoint can need. */
    static final int VLOER = 10_000;

    /** What {@link #vuur} did: the firings of all its fires together; the cap it fired under, on the exec-model branch
     *  the cap of its last phase; and the sentence of a run that ends without a fixpoint, at the cap or at the
     *  creation budget ({@code Creatiebudget}), and null at a fixpoint. A run that ends at the budget counts the firings
     *  of its fires before the one in which the creation rule threw. */
    public record Vuring(int vuringen, int plafond, String geenFixpunt) {}

    /**
     * Fires {@code sessie} of {@code build} to its fixpoint under a cap on the firings of the run. On the DRL-text
     * branch the cap is a fixed {@value #VAST_PLAFOND}. On the exec-model branch it is
     * {@code max(}{@value #VLOER}{@code , FACTOR × R × N0)}, with {@code R} the rules of the session's KieBase and
     * {@code N0} the facts the session holds just before it fires ({@link #FACTOR}): the cap is a safeguard against a
     * fault of the lowering, not a norm, so it grows with the input. The sentence at the cap names {@code R}, {@code N0}
     * and the cap. A run that reaches the cap or the creation budget has no fixpoint, and {@link Vuring#geenFixpunt}
     * says so. On the exec-model branch every run leaves a stated mark of how it ended, at its fixpoint, without
     * one, or with an exception out of the fire ({@code Gevuurd}), and keeps a watch on the session from then on
     * ({@code Bewaking}), a listener that it takes off while it fires itself; {@link #lees} and
     * {@link #zetAfgeleideWaarden} read a session only where the last run reached its fixpoint and the watch saw nothing
     * happen outside {@code vuur} since.
     *
     * <p>On the exec-model branch it refuses a session that {@link #zetAfgeleideWaarden} wrote back, whose beans
     * hold derived values beside their input: a consumer that fires more than once reads between the fires with
     * {@link #lees}, or opens a fresh session with beans that hold input alone.
     *
     * <p>On the exec-model branch it refuses, before it fires, a session in which, since {@link #voegIn} or the
     * last {@code vuur}, more happened outside it than an update of an attribute or a characteristic of a bean
     * {@code voegIn} put in: a fact inserted or deleted, among them a bean, a bucket input, a list place or a fact of
     * the branch; an update of a fact that is no such bean, or one that changed a relation, a reference field; a rule
     * that fired; and a change of the agenda, a cleared group or a focus change, read from Drools' agenda where no event
     * reports it. {@code voegIn} states the places of a list and the bucket scalars over the beans and relations of one
     * input, the branch states its own facts, and only this fires under the cap and in generations, so the way is a
     * fresh session. It refuses as well, after its first run, a session whose globals changed since that run, since the
     * branch states a global as a fact once, at the first fire, and a session from which a consumer took the watch. It
     * repeats the reference check of {@code voegIn} over the beans of the session, which a reference field set without
     * an update can fail, and refuses in the sentence of {@code voegIn}; and it refuses a session in which a reference
     * field of a bean {@code voegIn} put in holds another object than it held then, set without an update, which no
     * event shows and after which Drools keeps reading the old relation.
     *
     * <p>On the exec-model branch it fires a session of a model with a creation once, and refuses it at every
     * later call. An object or a fact creation is stated and never lapses, and so are the initialisations, the links
     * and the fault of a failed initialisation it brought (RUNTIME-SEMANTICS ch. 5.5, {@code Gevuurd}): after an input
     * change a second fire would keep them beside an input they no longer follow from, a state no fresh evaluation
     * gives. The sentence says so, and that the way is a fresh session per input.
     *
     * <p>A model with a creation fires in generations; see {@link #vuur(Build, KieSession, int)}. There the cap is
     * computed again over the facts at the start of every phase, so the objects the creations made count, and never
     * lowered: a later phase may hold fewer facts than an earlier one, and the cap bounds the run as a whole.
     */
    public static Vuring vuur(Build build, KieSession sessie) {
        Objects.requireNonNull(build, "build");
        Objects.requireNonNull(sessie, "sessie");
        return build.branch() == Branch.DRL_TEXT ? vuur(build, sessie, VAST_PLAFOND)
                : vuur(build, sessie, FACTOR, VLOER);
    }

    /** As {@link #vuur(Build, KieSession)} on every branch under the cap {@code max(vloer, factor × R × N0)}, rounded
     *  up: the seam a witness gives a small factor and floor to reach the cap, a factor below 1 among them. */
    static Vuring vuur(Build build, KieSession sessie, double factor, int vloer) {
        if (!(factor > 0)) throw new IllegalArgumentException("vuurfactor " + factor + " is niet groter dan 0");
        return drijf(build, sessie, new Plafond(factor, vloer, regels(sessie)));
    }

    /**
     * Fires {@code sessie} of {@code build} to its fixpoint, at most {@code plafond} firings in all, a cap that does not
     * grow: the seam of the scale harness, which sets a cap that never cuts it off, and of a witness that sets a small
     * one. On the DRL-text branch, and on the exec-model branch without a lowered creation, that is one fire.
     *
     * <p>On the exec-model branch with a creation a creation fires only at a fixpoint of the rule layer, and
     * what it makes is stated and never lapses, as in the reference interpreter (RUNTIME-SEMANTICS ch. 5.5): a logical
     * object could keep itself standing through its own output after the state that made it passed, and its
     * initialisations would follow later changes, where ch. 5.5 evaluates them when the object is made. So this fires
     * in generations, here and not in the rules, so that a second rendering of the branch can share it. The creation
     * rules sit in agenda groups of their own ({@code TmsTranspiler.FEITCREATIE}, {@code TmsTranspiler.OBJECTCREATIE}),
     * so a fire without their focus takes the rule layer to rest and leaves them pending. A generation is one phase: it
     * opens a new {@code Creatiefase}, puts both groups on the focus stack, the fact creations on top, and fires. That
     * fires the fact creations that hold at the fixpoint, then the object creations, and once both groups are empty it
     * takes the rule layer to rest again in the same fire. Between the two groups the rule layer does not fire, and
     * Drools evaluates a rule only when the agenda takes it from the group that has the focus, so every creation of a
     * generation reads the same settled world, as in the reference interpreter (RUNTIME-SEMANTICS ch. 5.5,
     * Generations): an object creation does not read what a fact creation of its own generation links. What a phase
     * made carries its number, and a creation rule does not take an object or a link of the phase it fires in, so a
     * recursive chain advances one generation per phase. Until 2026-10-09 a generation was a phase of fact creations
     * and a phase of object creations with the rule layer at rest between them, so an object creation read the links
     * the fact creations of its own generation had laid: an initialisation that counts the maatgevers of a Wachter,
     * over the relation a fact creation in the same recursive group lays, gave 1 where the reference interpreter
     * gives 0.
     *
     * <p>The generations run per evaluation stratum of the reference interpreter, in ascending order
     * ({@code TmsTranspiler.creatiestrata}): the phases of stratum k carry k, and a creation rule fires only in a phase
     * of its own stratum or one above. A creation writes irrevocably, so it reads strictly and sits above the producers
     * of what it reads (RUNTIME-SEMANTICS ch. 6.1); the reference interpreter evaluates stratum k to its fixpoint before
     * a form of stratum k+1 fires (ch. 6.3). In one phase for all strata, a creation that reads what another creation
     * of the same generation makes would read the state before it, and its initialisation would stay fixed on that
     * value. The strata order only the creations, which are stated: the rule layer stays under truth maintenance and
     * reaches its fixpoint after every phase.
     *
     * <p>A stratum ends at its first generation that makes nothing. The creation budget (RUNTIME-SEMANTICS ch. 5.5) is
     * the depth of a chain of mints, not a count of generations: the creation rule computes the depth of what it makes
     * from the mark of its subject and throws {@link Creatiebudget} at a mint past {@code TmsTranspiler.CREATIEBUDGET},
     * and this ends the run there, with its sentence, as a run without a fixpoint. The generations need no bound of
     * their own: every generation but the last makes an object or lays a link, and under the budget a stratum makes
     * finitely many of both. The rule layer has no budget here either: truth maintenance takes it to rest within the
     * fire, and the cap bounds that. Until 2026-10-09 this counted the generations of a stratum that make
     * something, at most 100, as the reference interpreter did that day: a chain whose every mint waits a generation for
     * a link, and a fact creation that lays a closure of 102 hops, ended without a fixpoint at their 101st generation.
     */
    static Vuring vuur(Build build, KieSession sessie, int plafond) {
        return drijf(build, sessie, new Plafond(plafond));
    }

    /** The rules of the KieBase {@code sessie} fires: the {@code R} of the cap. */
    private static long regels(KieSession sessie) {
        long n = 0;
        for (org.kie.api.definition.KiePackage p : sessie.getKieBase().getKiePackages()) n += p.getRules().size();
        return n;
    }

    /** The cap of one run of {@link #drijf}: a fixed number of firings, or {@code max(vloer, factor × R × N)} over the
     *  facts {@code N} at the start of a phase, rounded up and never lowered within the run. */
    private static final class Plafond {
        /** 0 for a fixed cap. */
        private final double factor;
        private final int vloer;
        private final long regels;
        private int waarde;
        private long feiten = -1;

        /** A fixed cap. */
        Plafond(int vast) {
            this(0, vast, 0);
        }

        Plafond(double factor, int vloer, long regels) {
            this.factor = factor;
            this.vloer = vloer;
            this.regels = regels;
            this.waarde = vloer;
        }

        /** Computes the growing cap over the facts {@code sessie} holds now, where that raises it. */
        void herreken(KieSession sessie) {
            if (factor == 0) return;
            long n = sessie.getFactCount();
            // a double holds every product below 2^53 exactly, and a cap is an int
            double nieuw = Math.min(Integer.MAX_VALUE, Math.max(vloer, Math.ceil(factor * regels * n)));
            if (feiten < 0 || nieuw > waarde) {
                waarde = (int) nieuw;
                feiten = n;
            }
        }

        /** The sentence of a run that reached the cap. */
        String zin() {
            return factor == 0
                    ? "geen fixpunt: het vuurplafond van " + waarde + " vuringen is bereikt"
                    : "geen fixpunt: het vuurplafond van " + waarde + " vuringen is bereikt, het grootste van " + vloer
                      + " en " + java.math.BigDecimal.valueOf(factor).stripTrailingZeros().toPlainString() + " × "
                      + regels + " regels × " + feiten + " feiten vlak vóór het vuren; zonder kring vuurt de tak per"
                      + " regel en feit een begrensd aantal keren, dus dit is een fout van de verlaging";
        }
    }

    /** The driver of every {@code vuur}. On the exec-model branch it refuses a session of a model with a creation
     *  that it fired before, and a session that was written back, and it marks how the run ended ({@link Gevuurd}),
     *  which {@link #lees} and {@link #zetAfgeleideWaarden} read: the mark
     *  of the previous run goes before the fire, so none stands while the session fires, and a fire that throws leaves
     *  a mark with its exception, so that no reading follows a run that broke off. It runs with the watch of the session
     *  off ({@link Bewaking}), so that its own inserts, deletes and firings count as none, and puts the watch back
     *  afterwards with nothing kept: the two readings see what happens after this run. A refusal leaves the watch as it
     *  was, so a reading after it still sees what happened before. It refuses as well what {@link #vuur(Build,
     *  KieSession)} lists: more than an update of an attribute or a characteristic of a bean of the input since
     *  {@link #voegIn} or the last run, a global changed since the first run, a fired session without its watch, a
     *  reference field that holds an object outside the session, and a relation changed without an update. At the first
     *  run it puts the watch on where
     *  {@code voegIn} did not, with the beans the session holds as the input, and keeps the globals. */
    private static Vuring drijf(Build build, KieSession sessie, Plafond plafond) {
        if (build.branch() == Branch.DRL_TEXT) return vuurOnderPlafond(build, sessie, plafond);
        TmsTranspiler tms = build.execModelTranspiler();
        // per class: Drools keeps the facts of a class apart and hands out only those for a ClassObjectFilter, where a
        // filter of another kind walks every fact the session holds, at every vuur
        List<FactHandle> merken = List.copyOf(sessie.<FactHandle>getFactHandles(new ClassObjectFilter(Gevuurd.class)));
        // what a creation made is stated and does not lapse, so a later fire would keep it beside an input it no longer
        // follows from; checked first, since a written-back session of such a model cannot be read between fires either
        if (!merken.isEmpty() && !tms.creatieregels().isEmpty())
            throw new IllegalStateException("dit model heeft creatieregels, en deze sessie is al via DroolsBackend.vuur"
                    + " gevuurd: wat een creatie maakte, met de initialisaties, de koppelingen en de fout van een mislukte"
                    + " initialisatie, is gesteld en vervalt niet (RUNTIME-SEMANTICS ch. 5.5), dus een volgende fire zou"
                    + " na een invoerwijziging een toestand geven die geen verse evaluatie geeft; open per invoer een"
                    + " verse sessie");
        if (!sessie.getObjects(new ClassObjectFilter(Teruggezet.class)).isEmpty())
            throw new IllegalStateException("deze sessie is teruggezet (zetAfgeleideWaarden): haar beans dragen afgeleide"
                    + " waarden naast de invoer, en een volgende fire zou die als invoer lezen; lees tussen twee fires met"
                    + " DroolsBackend.lees en zet pas na de laatste fire terug, of open een verse sessie met beans die"
                    + " alleen invoer dragen");
        Bewaking gezet = Bewaking.van(sessie);
        // voegIn puts the watch on, or the first run where there was no voegIn, and every run puts it back, so only a
        // consumer that took it off leaves a fired session without it, and what happened since is then not to be seen
        if (gezet == null && !merken.isEmpty())
            throw new IllegalStateException("de bewaking die DroolsBackend.vuur op deze sessie zette, staat er niet meer"
                    + " op, dus wat er na de laatste vuur gebeurde, is niet te zien; open een verse sessie met alle beans"
                    + " in één Invoer");
        if (gezet != null && gezet.erbijOfEraf() != null)
            throw new IllegalStateException("sinds " + gezet.sinds() + " kwam er buiten voegIn om invoer bij of ging er"
                    + " invoer weg (" + gezet.erbijOfEraf() + "): een afnemer geeft alle beans in één Invoer aan"
                    + " voegIn en verandert daarna alleen hun attributen en kenmerken, met update, want voegIn legt de"
                    + " plaatsen van een lijst en de emmerscalairen vast over de beans van één invoer; een nieuw of"
                    + " verwijderd object vraagt een verse sessie");
        String agenda = Bewaking.agenda(sessie);
        String meer = gezet == null ? agenda : gezet.meerDanEenUpdate() != null ? gezet.meerDanEenUpdate() : agenda;
        if (meer != null)
            throw new IllegalStateException("sinds " + (gezet == null ? "het begin van deze sessie" : gezet.sinds())
                    + " gebeurde er buiten vuur om meer dan een update van een attribuut of kenmerk van een bean (" + meer
                    + "), en meer laat de ingang niet toe: open een verse sessie met alle beans in één Invoer");
        String global = gezet == null ? null : gezet.veranderdeGlobal(sessie);
        if (global != null)
            throw new IllegalStateException(global + ": de tak leest een global één keer, in een regel die haar bij de"
                    + " eerste fire als feit stelt, dus een volgende fire zou met de oude waarde rekenen; zet de globals"
                    + " vóór de eerste vuur, en open voor een andere waarde een verse sessie");
        Map<Class<?>, Objecttype> klassen = bonenklassen(build);
        String buiten = doelBuitenDeSessie(build, sessie, klassen);
        if (buiten != null) throw new IllegalStateException(buiten);
        String relatie = gezet == null ? null : gezet.veranderdeRelatie();
        if (relatie != null)
            throw new IllegalStateException(relatie + ": Drools ziet een veld pas na een update, dus een fire zou de oude"
                    + " relatie blijven lezen, en ook met een update vraagt een gewijzigde relatie een verse sessie, want"
                    + " voegIn legt de plaatsen van een lijst vast over de relaties van één invoer; open een verse sessie"
                    + " met alle beans in één Invoer");
        Bewaking bewaking = gezet != null ? gezet
                : Bewaking.op(sessie, relatievelden(build, klassen), bonenInDeSessie(sessie, klassen),
                        "de eerste DroolsBackend.vuur van deze sessie");
        bewaking.legGlobalenVast(sessie);
        bewaking.vergeet("de laatste DroolsBackend.vuur van deze sessie");
        return bewaking.zonder(sessie, () -> {
            for (FactHandle merk : merken) sessie.delete(merk);
            Vuring vuring;
            try {
                vuring = vuurOnderPlafond(build, sessie, plafond);
            } catch (RuntimeException e) {
                try {
                    sessie.insert(new Gevuurd("de fire brak af met " + e.getClass().getSimpleName() + ": "
                            + e.getMessage()));
                } catch (RuntimeException merkfout) {
                    e.addSuppressed(merkfout);
                }
                throw e;
            }
            sessie.insert(new Gevuurd(vuring.geenFixpunt()));
            return vuring;
        });
    }

    /** Fires {@code sessie} as {@link #vuur(Build, KieSession, int)} describes, under {@code plafond}: in one go, or for
     *  a model with a creation on the exec-model branch in generations per stratum. */
    private static Vuring vuurOnderPlafond(Build build, KieSession sessie, Plafond plafond) {
        TmsTranspiler tms = build.execModelTranspiler();
        Set<String> creaties = build.branch() == Branch.DRL_TEXT ? Set.of() : tms.creatieregels();
        plafond.herreken(sessie);
        if (creaties.isEmpty()) {
            int gevuurd = sessie.fireAllRules(plafond.waarde);
            return new Vuring(gevuurd, plafond.waarde, gevuurd >= plafond.waarde ? plafond.zin() : null);
        }
        // Drools makes a group on demand only in a KieBase that declares a group beside MAIN, which every creation
        // rule does; null means the KieBase lacks the creation rules the transpiler reports
        org.kie.api.runtime.rule.Agenda agenda = sessie.getAgenda();
        for (String groep : List.of(TmsTranspiler.FEITCREATIE, TmsTranspiler.OBJECTCREATIE))
            if (agenda.getAgendaGroup(groep) == null)
                throw new IllegalStateException("de KieBase kent de agendagroep " + groep + " niet, terwijl het model"
                        + " creatieregels heeft: " + creaties);
        int[] gemaakt = {0};
        AgendaEventListener teller = new DefaultAgendaEventListener() {
            @Override public void afterMatchFired(AfterMatchFiredEvent e) {
                if (creaties.contains(e.getMatch().getRule().getName())) gemaakt[0]++;
            }
        };
        sessie.addEventListener(teller);
        try {
            int gevuurd = sessie.fireAllRules(plafond.waarde);
            FactHandle fase = null;
            int nummer = 0;
            for (int stratum : tms.creatiestrata()) {
                while (gevuurd < plafond.waarde) {
                    gemaakt[0] = 0;
                    if (fase != null) sessie.delete(fase);
                    fase = sessie.insert(new Creatiefase(++nummer, stratum));
                    plafond.herreken(sessie);
                    // the focus is a stack: the group given the focus last fires first
                    agenda.getAgendaGroup(TmsTranspiler.OBJECTCREATIE).setFocus();
                    agenda.getAgendaGroup(TmsTranspiler.FEITCREATIE).setFocus();
                    try {
                        gevuurd += sessie.fireAllRules(plafond.waarde - gevuurd);
                    } catch (ConsequenceException e) {
                        Creatiebudget budget = creatiebudget(e);
                        if (budget == null) throw e;
                        return new Vuring(gevuurd, plafond.waarde, budget.getMessage());
                    }
                    if (gemaakt[0] == 0) break;
                }
            }
            return new Vuring(gevuurd, plafond.waarde, gevuurd >= plafond.waarde ? plafond.zin() : null);
        } finally {
            sessie.removeEventListener(teller);
        }
    }

    /** The {@link Creatiebudget} a creation rule threw, which Drools' default handler hands on inside {@code e}, or null
     *  where {@code e} carries another fault. */
    private static Creatiebudget creatiebudget(ConsequenceException e) {
        for (Throwable t = e.getCause(); t != null; t = t.getCause() == t ? null : t.getCause())
            if (t instanceof Creatiebudget budget) return budget;
        return null;
    }

    /** The DRL-TEXT branch: {@link DrlTranspiler} emits DRL, which is compiled to a {@link KieBase} by the classic
     *  DRL build ({@code KieHelper}, mvel dialect). Three forms are refused here, each loudly and with the reason,
     *  because the text branch is NARROWER than the exec-model one:
     *  <ul>
     *    <li>a ch. 7 prioritizedDefaults extension — that lowering exists only in the exec branch, so a DRL
     *        KieBase would silently omit the default and answer with an unwritten attribute;</li>
     *    <li>any construct the text branch markers (its {@code TODO} discipline) — the DRL still compiles,
     *        which is exactly why the marker must be refused rather than fired;</li>
     *    <li>a DRL the Drools compiler rejects — a text-branch limit, not a semantic divergence.</li>
     *  </ul> */
    private static Build drlTak(IrLoader.Loaded loaded, ClassLoader cl) {
        if (!loaded.prioritizedDefaults().isEmpty())
            throw new Excluded("drl: ch. 7 prioritizedDefaults — the DRL-text branch has no defaults lowering");
        // ch. 4.10: the DRL branch lowers the §11 variabelendeel's Let chain, so it gets the document's Let INDEX
        // beside the model — the ch. 2.10 unit derivation needs it to answer a BinderRef read, which carries no
        // route back up to its own binding (the exec branch has taken the same index since the Let-node card).
        // The defaults list is empty by the refusal above; it is passed rather than defaulted because the
        // ch. 7.8 standing classification's other_writer clause reads it (see DrlTranspiler.build).
        // ch. 4.3: and the symbolic role bindings, for the same reason the exec branch has taken them since
        // 2026-08-30 — a fold whose argument is an ATTRIBUTE name has no written role to resolve its range
        // from, so on a repeated or self-typed pair the wire's sourceRole is the only route there is. Withheld,
        // this branch markered exactly the folds its sister played.
        DrlTranspiler t = DrlTranspiler.build(loaded.model(), loaded.lets(), loaded.prioritizedDefaults(),
                loaded.bronPaden(), loaded.rolBindingen(), loaded.navigatieBindingen());
        for (Dekking d : t.dekking())
            if (!d.lowered()) throw new Excluded("drl-transpiler " + d.verdict() + ": " + d.sleutel()
                    + (d.reden().isEmpty() ? "" : " — " + d.reden()));
        KieHelper helper = new KieHelper();
        helper.setClassLoader(cl);
        // Without a name KieHelper files the text as file0.drl in the resource root, and KieBuilder warns that a
        // file in folder '' declares DRL_PACKAGE. The path's folders follow the package; the extension gives the type.
        helper.addContent(t.drl(), DrlTranspiler.DRL_PACKAGE.replace('.', '/') + "/regels.drl");
        KieBase kieBase;
        try {
            kieBase = helper.build();
        } catch (RuntimeException e) {
            // Drools' message opens with a header that carries the path above, 127 characters up to "text=" on the
            // first message, and only then the error itself (line, column, what failed). The bound has to leave that
            // text standing; at 200, 73 characters of it remained.
            String m = String.valueOf(e.getMessage()).replace('\n', ' ');
            throw new Excluded("drl compile: " + (m.length() > 400 ? m.substring(0, 400) + "…" : m));
        }
        return new Build(loaded.model(), cl, null, t, List.copyOf(t.dekking()), kieBase);
    }

    /** Names the fault behind {@code e}, the exception Drools' default handler throws out of the fire when a consequence
     *  throws: a division by a numeric zero in a consequence (a rule's value, a default's justification or consequence)
     *  becomes {@link Breuk.DelingDoorNul} naming the rule and the operands, with what the consequence threw as its
     *  cause; any other {@code e} is returned unchanged. {@link CaseRunner} calls it where it fires, so the replay, the
     *  red path and the scenario report read the sentence; a consumer that fires a KieBase itself gets Drools'
     *  {@code ConsequenceException}, which names the rule, and can call this the same way. A division in a condition
     *  is evaluated outside a consequence and does not come here.
     *
     *  <p>The KieBases built here keep Drools' default consequence handler on purpose. Drools takes a handler by class
     *  name and instantiates it in every new session; where the kie API and this backend are loaded by different
     *  class loaders (the distribution's Quarkus test of its decision service) that instance does not cast to the kie
     *  interface, and every {@code newKieSession} there threw a {@code ClassCastException} (2026-10-02). */
    public static RuntimeException benoem(ConsequenceException e) {
        for (Throwable t = e.getCause(); t != null; t = t.getCause() == t ? null : t.getCause())
            if (t instanceof Breuk.DelingDoorNul nul) return nul.inDeRegel(e.getRule().getName(), e.getCause());
        return e;
    }
}
