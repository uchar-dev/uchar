package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;
import dev.uchar.regelspraak.message.Msg;
import dev.uchar.regelspraak.refusal.DroolsCode;
import com.fasterxml.jackson.databind.ObjectMapper;

import dev.uchar.regelspraak.cases.CaseRunner.Excluded;
import dev.uchar.regelspraak.engine.BeanCompiler;
import dev.uchar.regelspraak.engine.ScenarioResult;
import dev.uchar.regelspraak.graph.ProducerConsumer;
import dev.uchar.regelspraak.ir.ResolvedStartPointDetermination;
import dev.uchar.regelspraak.loader.IrLoader;
import dev.uchar.regelspraak.model.RegelSpraakModel;
import dev.uchar.regelspraak.model.RegelSpraakModel.ObjectType;
import dev.uchar.regelspraak.refusal.Reason;
import dev.uchar.regelspraak.schema.ContractValidator;
import dev.uchar.regelspraak.schema.FactModelGenerator;
import dev.uchar.regelspraak.transpiler.core.Coverage;
import dev.uchar.regelspraak.transpiler.core.Namen;
import dev.uchar.regelspraak.transpiler.tms.Watch;
import dev.uchar.regelspraak.transpiler.tms.CreationBudget;
import dev.uchar.regelspraak.transpiler.tms.CreationPhase;
import dev.uchar.regelspraak.transpiler.tms.BucketInput;
import dev.uchar.regelspraak.transpiler.tms.Fixpoint;
import dev.uchar.regelspraak.transpiler.tms.Fault;
import dev.uchar.regelspraak.transpiler.tms.Fired;
import dev.uchar.regelspraak.transpiler.tms.Cycles;
import dev.uchar.regelspraak.transpiler.tms.ListPlace;
import dev.uchar.regelspraak.transpiler.tms.WrittenBack;
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
import java.util.LinkedHashSet;
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
 * <p>The backend has two branches ({@link Branch}), two renderings of one analysis of the IR ({@link TmsTranspiler}),
 * which derives with truth maintenance: a derived value is a logical fact, every read it depends on is a pattern in
 * the rule's left-hand side, and the beans hold the input until the write-back. The exec-model branch renders the
 * analysis through Drools' executable model, the DRL-text branch as DRL text ({@link TmsTranspiler#drl}), which the
 * classic DRL build compiles. Both derive over the same facts, and every entry of a session treats them alike. Gate 12
 * replays every golden through both and demands the same status line of each; the distribution builds with the
 * exec-model branch and shows the text. Until card drl-teksttak-op-truth-maintenance the DRL-text branch was a
 * lowering of its own, whose rules wrote the beans as they fired.
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
 * does not carry, an {@link IrLoader.DocumentError} for a document that is not well-formed, and a
 * {@link CaseRunner.Excluded} for a form the backend does not carry past the loader — among them every construct
 * the analysis does not lower, so no KieBase is handed out that silently lacks a rule.
 *
 * <p>A session of a build goes through four calls, as the replay makes them ({@link CaseRunner}): a fresh
 * {@link org.kie.api.runtime.KieSession} of {@link Build#kieBase()}, with {@link Build#beanLoader()} as the thread's
 * context class loader, the parameters and the evaluation date ({@code rekendatum}) as globals and on the DRL-text
 * branch also {@link Build#dayKindGlobals()}; then {@link #insert}, which puts in the fact beans and what the
 * branch reads beside them, and for a case {@link CaseInput} makes them; then {@link #fire}, which fires to the fixpoint under a cap and, for a model with a
 * creation, in generations, and with a {@link Observer} records what the session fired, from which {@link #coverage}
 * gives the coverage; then {@link #fixpointOf} or {@link #setDerivedValues}, after which {@link #compare} compares the
 * expectations of a case with the session. The beans hold the input alone until
 * the write-back, which comes once, at the end of the session, and closes it: a consumer that fires one session more
 * than once, changing input in between with {@code update}, reads every answer but the last with {@link #fixpointOf}, each
 * after a {@link #fire}: the two readings refuse a session in which something happened outside {@code fire} after the
 * last one. A session of a model with a creation fires once, since what a creation made is stated and does not lapse:
 * such a consumer opens a fresh session per input. An evaluation fault is a fact of the fixpoint, which {@link #fixpointOf}
 * gives as data and {@link #setDerivedValues} throws.
 */
public final class DroolsBackend {

    private static final ObjectMapper JSON = new ObjectMapper();

    private DroolsBackend() {}

    /** Which rendering of the analysis ({@code TmsTranspiler}) the build compiles. Both derive with truth maintenance,
     *  with logical inserts instead of writing the beans, over the same facts: the exec-model branch through Drools'
     *  executable model, the DRL-text branch as DRL text ({@link TmsTranspiler#drl}) through the classic DRL build.
     *  Gate 12 replays every golden through both and demands the same status line of each. */
    public enum Branch { EXEC_MODEL, DRL_TEXT }

    /** A built document, from which {@link #branch()} follows: the analysis ({@code execModelTranspiler}), on the
     *  exec-model branch without a text and on the DRL-text branch with the DRL text of that analysis
     *  ({@link TmsTranspiler#drl}). {@code coverage} is the coverage manifest of the analysis, every entry lowered,
     *  because the build refuses a document with any other entry. */
    public record Build(RegelSpraakModel model, ClassLoader beanLoader, TmsTranspiler execModelTranspiler, String drl,
                        List<Coverage> coverage, KieBase kieBase) {
        public Build {
            if (execModelTranspiler == null)
                throw new IllegalArgumentException("a Build carries the analysis of its document");
            if (drl != null && !drl.equals(execModelTranspiler.drl()))
                throw new IllegalArgumentException("a Build of the DRL-text branch carries the DRL text of its analysis");
        }

        public Branch branch() {
            return drl != null ? Branch.DRL_TEXT : Branch.EXEC_MODEL;
        }

        /** The §9.8 day-kind predicates the DRL text declares as globals (name -> predicate); a session must have
         *  each set before it fires. Empty on the exec-model branch, which wires them into its lambdas; on the
         *  DRL-text branch those of the analysis, whose rendering names their globals
         *  ({@link TmsTranspiler#drlDayKindGlobals}). */
        public Map<String, Predicate<LocalDate>> dayKindGlobals() {
            return drl == null ? Map.of() : execModelTranspiler.drlDayKindGlobals();
        }
    }

    /** {@link #build(JsonNode, Branch)} on the document read from {@code irPath}. */
    public static Build build(Path irPath, Branch branch) throws IOException {
        return build(JSON.readTree(Files.readString(irPath)), branch);
    }

    /** Load {@code document}, refuse the forms the backend does not carry, generate and contract-check the fact
     *  beans, analyse every construct with every side table the loader returns, render the analysis as
     *  {@code branch} has it, and build the {@link KieBase}.
     *
     *  @throws IrLoader.Refusal      a form the loader does not carry; the replay reports it as {@code loader: …}
     *  @throws IrLoader.DocumentError a document that is not well-formed {@code uchar-ir-v1}
     *  @throws CaseRunner.Excluded   a form the backend does not carry past the loader: two declarations on one
     *      Java name of the bean model, a reciprocal self-relation the bean model cannot hold, a bean-contract
     *      violation, a construct the analysis does not lower, a cycle of the dependency graph that WF-045 or WF-046
     *      forbids, two rules of one name, and on the DRL-text branch a kind its rendering does not carry
     *      ({@link TmsTranspiler.DrlRefusal})
     *  @throws IllegalStateException on the DRL-text branch a DRL text the Drools compiler rejects: a fault of the
     *      rendering, never a form the branch does not carry */
    public static Build build(JsonNode document, Branch branch) {
        Objects.requireNonNull(branch, "branch");
        IrLoader.Loaded loaded = IrLoader.loadAll(document);   // the model plus the ch. 7 prioritized-defaults extension beside it
        RegelSpraakModel model = loaded.model();
        refuseBeanNameCollision(loaded);

        // A form the v0 backend cannot REPLAY is refused up front — a LOUD per-form exclusion, never a
        // silent value-divergence in the gate. Carrying it to fire only to fault or misvalue there is
        // honestly NOT-CARRIED, not a semantic exec-model difference, so it belongs in the auto-excluded
        // channel with a reason, not the value-divergence list whose banner claims a genuine ref/clingo delta.
        // (§13.4.6 objectcreatie was refused here until 2026-08-30; the creation consequence now executes —
        // both back-link ends written, the kenmerkinitialisatie honoured — so the shapes it still cannot carry
        // are refused per creation by the transpiler, which knows WHICH one, not blanket for every model.)
        //   §13.3.9.2 name-sharing bean limit — members() ports every member through a name-dedup (attribute >
        //     kenmerk > reference), so a declared member sharing the reciprocal role's name displaces the
        //     self-pair reference field; the mirror pass would then read a Boolean or set an impossible type,
        //     an unclassified ERROR at best. Refuse the shape loudly instead (lens finding, 2026-08-30).
        for (ObjectType ot : model.objectTypes.values()) {
            if (!FactModelGenerator.namesReciprocal(model, ot)) continue;
            String selfRole = FactModelGenerator.selfPairRole(model, ot);
            //   the UNDERIVABLE half of the same shape — an ambiguous pair (two wederkerig feittypen on one
            //   objecttype) or a to-many reciprocal role: the transpilers navigate by the written role name
            //   toward a getter no bean carries, an unclassified fire-time ERROR (lens 2, 2026-08-30).
            if (selfRole == null)
                throw new Excluded(DroolsCode.SELF_RELATION_NO_FIELD.reason("objectType", ot.name));
            if (!FactModelGenerator.hasReferenceField(model, ot, selfRole, ot))
                throw new Excluded(DroolsCode.SELF_PAIR_ROLE_SHARED.reason("role", selfRole, "objectType", ot.name));
        }

        Map<String, String> sources = FactModelGenerator.generate(model);
        ClassLoader cl = BeanCompiler.compile(sources);
        List<Reason> violations = ContractValidator.validate(model, cl);
        if (!violations.isEmpty()) throw new Excluded(violations.get(0).framed("contract"));

        ClassLoader previous = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(cl);
        try {
            return switch (branch) {
                case EXEC_MODEL -> execModelBranch(loaded, cl);
                case DRL_TEXT -> drlBranch(loaded, cl);
            };
        } finally {
            Thread.currentThread().setContextClassLoader(previous);
        }
    }

    /** Two declarations on one Java name of the bean model, refused before a bean is generated, in the sentences of
     *  {@link FactModelGenerator}: an object type whose fact class another type, a class the generator writes itself
     *  or a {@code java.lang} type already names; two roles on one reference field of one type; and a member whose
     *  bean property belongs to another declaration, where a form of the rule layer reads or writes it (the forms of
     *  {@link ProducerConsumer}, and a §9.9 start-point determination, which names its anchor member). Such a member
     *  that nothing uses is left alone: its field is simply absent, and a case that names it is refused by the case
     *  reader. Here and not in the loader, because the rule graph and every other reading of the loaded model keep
     *  the two names apart; only the bean model does not. */
    private static void refuseBeanNameCollision(IrLoader.Loaded loaded) {
        RegelSpraakModel model = loaded.model();
        Reason javaClass = FactModelGenerator.classCollision(model);
        if (javaClass != null) throw new Excluded(javaClass);
        List<FactModelGenerator.Displacement> displacements = FactModelGenerator.displacements(model);
        if (displacements.isEmpty()) return;
        Reason role = FactModelGenerator.roleCollision(displacements);
        if (role != null) throw new Excluded(role);
        Map<ProducerConsumer.ElementKey, Msg> use = new java.util.HashMap<>();
        for (ProducerConsumer.Form v : ProducerConsumer.forms(model, loaded.prioritizedDefaults(),
                ProducerConsumer.wireNavigation(loaded))) {
            Msg formName = Msg.drools("PART.form-named." + v.category().name().toLowerCase(java.util.Locale.ROOT).replace('_', '-'), "name", v.name());
            for (ProducerConsumer.ElementKey e : v.writes()) use.putIfAbsent(e, formName);
            for (ProducerConsumer.ElementKey e : v.reads()) use.putIfAbsent(e, formName);
        }
        for (ResolvedStartPointDetermination sp : model.resolvedStartPointDeterminations)
            use.putIfAbsent(ProducerConsumer.elementOf(model.objectTypeByName(sp.objectType()), sp.member(), false),
                    Msg.drools("PART.desc.start-point", "timeline", sp.timeline()));
        for (FactModelGenerator.Displacement v : displacements) {
            FactModelGenerator.Origin h = v.displaced();
            // a role without a field of its own is refused where it is used, by the gate that asks the field's name
            // and type (FactModelGenerator.hasReferenceField); rolbotsing above took the one shape that gate passes
            if (h.category() == FactModelGenerator.Origin.Kind.ROL) continue;
            Msg formName = use.get(ProducerConsumer.ElementKey.van(h.source(), h.name(),
                    h.category() == FactModelGenerator.Origin.Kind.KENMERK));
            if (formName != null) throw new Excluded(DroolsCode.BEAN_NAME_COLLISION_USE.reason("reason", FactModelGenerator.displacementReason(v).msg(), "form", formName));
        }
    }

    /** Two rules of one name in one package: the KieBase keeps one rule per name, so the second would replace the first
     *  and the document would play without it (measured 2026-10-02 on two forms of one name, which WF-020 forbids and a
     *  hand-written document can still carry). The analysis refuses it in the words Drools' DRL compiler uses,
     *  "Duplicate rule name", after the label of the analysis, before a KieBase of either branch is built. A rule
     *  without a name (a nameless form, WF-107) has no name to share and is not counted; the analysis refuses a
     *  nameless creation from its manifest ({@code BranchEqualityWitnessTest}). */
    private static void refuseDuplicateRuleName(org.drools.model.Model model) {
        java.util.Set<String> names = new java.util.HashSet<>();
        for (org.drools.model.Rule r : model.getRules())
            if (r.getName() != null && !names.add(r.getFullName()))
                throw new Excluded(DroolsCode.DUPLICATE_RULE_NAME.reason("rule", r.getName(), "package", r.getPackage()));
    }

    /** The exec-model branch: {@link TmsTranspiler} lowers the forms it carries to an executable model that derives with
     *  truth maintenance, with logical inserts. The KieBase runs in EQUALITY mode, as the branch's design requires:
     *  there a fact inserted as stated takes over an equal logical fact, where IDENTITY mode keeps the two as two facts
     *  on one key and ignores a delete of the stated one in some states. Equal logical facts merge into one fact with a
     *  justification per rule in either mode; that merge rests on the equals and hashCode of the branch's fact classes.
     *  A form the transpiler does not lower is refused from its manifest, in the {@code transpiler SKIP:} sentence the
     *  toolchain reads; a cycle of the dependency graph that WF-045 or WF-046 forbids is refused before the KieBase is
     *  built, since truth maintenance gives such a cycle no single fixpoint ({@code Cycles}). */
    private static Build execModelBranch(IrLoader.Loaded loaded, ClassLoader cl) {
        TmsTranspiler t = analyse(loaded, cl);
        KieBase kieBase = KieBaseBuilder.createKieBaseFromModel(t.model(), EqualityBehaviorOption.EQUALITY);
        return new Build(loaded.model(), cl, t, null, List.copyOf(t.coverage()), kieBase);
    }

    /** The analysis of the document ({@link TmsTranspiler}) with its refusals, which both branches share in one
     *  sentence each: a form the transpiler does not lower, from its manifest, in the {@code transpiler SKIP:}
     *  sentence the toolchain reads; a cycle of the dependency graph that WF-045 or WF-046 forbids; and two rules of one
     *  name, after the label {@code exec-model:}, the name of the analysis. */
    private static TmsTranspiler analyse(IrLoader.Loaded loaded, ClassLoader cl) {
        TmsTranspiler t = TmsTranspiler.build(loaded.model(), cl, loaded.prioritizedDefaults(), loaded.roleBindings(),
                loaded.lets(), loaded.sourcePaths(), loaded.navigationBindings());
        for (Coverage d : t.coverage())
            if (!d.lowered()) throw new Excluded(new Reason(d.code(), Msg.drools("FRAME.transpiler-verdict",
                    "verdict", d.verdict(), "key", d.key(), "reasonClause", d.reason().isEmpty() ? ""
                            : Msg.drools("FRAME.verdict-reason", "reason", d.reasonText()))));
        // past the manifest every form of the document is one this branch lowers, so the graph over all forms is the
        // graph over the forms it carries
        Reason cycle = Cycles.refusal(loaded);
        if (cycle != null) throw new Excluded(cycle);
        refuseDuplicateRuleName(t.model());
        return t;
    }

    /** The DRL-text branch: the analysis, with its refusals ({@link #analyse}), rendered into DRL text
     *  ({@link TmsTranspiler#drl}) and compiled by the classic DRL build ({@code KieHelper}) in EQUALITY mode, as the
     *  executable model of the exec-model branch is. The text derives with truth maintenance over the same facts as the
     *  executable model, so every entry of a session treats the two branches alike. It refuses as well, loudly and with
     *  the reason, a kind the rendering does not carry ({@link TmsTranspiler.DrlRefusal}), after the label
     *  {@code drl:}, since the analysis does not refuse it. The predicates of the day kinds are globals of the text,
     *  which {@link Build#dayKindGlobals} gives from the analysis. A text the Drools compiler rejects is a fault of the
     *  rendering, not a form the branch does not carry: it fails with an {@link IllegalStateException}, which a caller
     *  that knows the document names. */
    private static Build drlBranch(IrLoader.Loaded loaded, ClassLoader cl) {
        TmsTranspiler t = analyse(loaded, cl);
        String drl;
        try {
            drl = t.drl();
        } catch (TmsTranspiler.DrlRefusal w) {
            throw new Excluded(w.reason().framed("drl"));
        }
        KieHelper helper = new KieHelper();
        helper.setClassLoader(cl);
        // the folders of the path follow the package of the text, as DrlRendering.PATH has them; the extension gives the type
        helper.addContent(drl, TmsTranspiler.PKG.replace('.', '/') + "/regels.drl");
        KieBase kieBase;
        try {
            kieBase = helper.build(EqualityBehaviorOption.EQUALITY);
        } catch (RuntimeException e) {
            // Drools' message opens with a header that carries the path above, 127 characters up to "text=" on the
            // first message, and only then the error itself (line, column, what failed); the bound leaves that standing
            String m = String.valueOf(e.getMessage()).replace('\n', ' ');
            throw new IllegalStateException("drl: de DRL-tekst van de weergave compileert niet, een fout van de"
                    + " weergave: " + (m.length() > 400 ? m.substring(0, 400) + "…" : m), e);
        }
        return new Build(loaded.model(), cl, t, drl, List.copyOf(t.coverage()), kieBase);
    }

    /**
     * Puts {@code input} into {@code session}, a fresh session of {@code build}: the fact beans, and what the branch
     * reads beside them and the bean model does not hold, as the branch's own facts, so that a consumer names it in
     * the words of the model and states none of them. That is an
     * {@code BucketInput} for every bucket scalar {@code input} names ({@link Input#bucketScalar}), without which the
     * branch reads an input timeline as a full timeline (RUNTIME-SEMANTICS ch. 8.4); and where a rule of the model reads
     * the order of a to-many list ({@code TmsTranspiler.listFields}), a {@code ListPlace} for every member of every
     * such list, in the order {@link Input#list} gives or else in the order of the beans.
     *
     * <p>It checks all of it before it inserts anything, and refuses by name: a session
     * that already holds a fact, since the places of a list are numbered over the beans of one input and a bean
     * inserted without this call has none; a bean that is not of the build's bean model or that comes twice; a reference
     * field of a bean that holds an object outside the input, named with the bean, the field and the object's type, since
     * the branch derives nothing for an object outside the session and every value read through the field would stay
     * empty without a word; a bucket
     * scalar on a bean outside the input or on a member its type does not declare as an attribute on a timeline; and a
     * list that names a bean outside the input, a member whose reference field {@code role} does not hold the holder, a
     * member twice, or not every bean of the input whose field of that relation holds the holder, or that comes twice. A
     * place the rules need is thus never missing at the fire, where it would stop the run with an
     * {@code IllegalStateException}.
     *
     * <p>Then it puts the watch of the entry on the session ({@code Watch}), with the relations the beans hold now,
     * and {@link #fire} refuses a session in which more happened outside it than an update of an attribute or a
     * characteristic of a bean this call put in: the places of a list and the bucket scalars are stated over the beans
     * and the relations of one input, so a bean inserted later has neither, and a relation changed later keeps the
     * place it had or has none. A consumer opens a fresh session to add or remove an object or to change a relation.
     *
     * @throws IllegalStateException    {@code session} is not fresh
     * @throws IllegalArgumentException a statement of {@code input} that does not fit its beans or the model
     */
    public static void insert(Build build, KieSession session, Input input) {
        Objects.requireNonNull(build, "build");
        Objects.requireNonNull(session, "session");
        Objects.requireNonNull(input, "input");
        List<Object> beans = input.beans();
        TmsTranspiler tms = build.execModelTranspiler();
        if (session.getFactCount() != 0)
            throw new IllegalStateException("insert vult een verse sessie, maar deze houdt al " + session.getFactCount()
                    + " feiten: de plaatsen van een lijst worden over de beans van één invoer geteld, en een bean die"
                    + " zonder insert in de sessie kwam, heeft er geen; geef alle beans in één Input aan een verse"
                    + " sessie, en verander tussen twee fires hun waarden met update");
        Map<Object, ObjectType> types = new IdentityHashMap<>();
        // a case of ten thousand objects has a handful of bean classes
        Map<Class<?>, ObjectType> perClass = new HashMap<>();
        for (Object beanObject : beans)
            if (types.put(beanObject, perClass.computeIfAbsent(beanObject.getClass(), k -> objectType(build, beanObject))) != null)
                throw new IllegalArgumentException("insert: de bean " + name(beanObject) + " staat twee keer in de invoer");
        String outsideSentence = targetOutsideInput("insert", build, beans, types, perClass);
        if (outsideSentence != null) throw new IllegalArgumentException(outsideSentence);
        List<Object> facts = new ArrayList<>(beans);
        for (Input.Bucket e : input.emmers()) facts.add(bucketInput(types, e));
        facts.addAll(listPlaces(build, tms.listFields(), beans, types, input.lists()));
        for (Object fact : facts) session.insert(fact);
        Watch.op(session, relationFields(build, beansClasses(build)), beans, "DroolsBackend.insert");
    }

    /** The sentence that refuses a reference field of a bean of {@code beans} that holds an object outside
     *  {@code beans}, with {@code refusingCall} the call that refuses, or null where every such field holds one of them or none.
     *  The branch derives only for the objects in the session, so every value of such an object, and every value read
     *  through the field, would stay empty without a word; and the link the field gives still counts, so a creation it
     *  blocks makes no object in its place. */
    private static String targetOutsideInput(String refusingCall, Build build, List<Object> beans, Map<Object, ObjectType> types,
                                             Map<Class<?>, ObjectType> perClass) {
        Map<ObjectType, List<String>> fields = new HashMap<>();
        for (Object beanObject : beans)
            for (String field : fields.computeIfAbsent(types.get(beanObject), t -> referenceFields(build.model(), t))) {
                Object target = Accessors.leesEigenschap(beanObject, field);
                if (target != null && !types.containsKey(target))
                    return refusingCall + ": het veld '" + field + "' van " + name(beanObject) + " wijst naar een "
                            + perClass.computeIfAbsent(target.getClass(), k -> objectType(build, target)).name + " ("
                            + name(target) + ") die niet als bean in de invoer staat: de tak leidt alleen af voor de"
                            + " objecten in de sessie, dus geef elk object waarnaar een bean wijst ook zelf als bean op";
            }
        return null;
    }

    /** {@link #targetOutsideInput} over the beans {@code session} holds, the check {@link #fire} repeats before it fires:
     *  between two fires an update can set a reference field to an object outside the session. It asks the session per
     *  bean class, since Drools keeps the facts of a class apart and hands out only those for a
     *  {@link ClassObjectFilter}, where a filter of another kind walks every fact the session holds. */
    private static String targetOutsideSession(Build build, KieSession session, Map<Class<?>, ObjectType> classes) {
        List<Object> beans = new ArrayList<>();
        Map<Object, ObjectType> types = new IdentityHashMap<>();
        for (Map.Entry<Class<?>, ObjectType> k : classes.entrySet())
            for (Object o : session.getObjects(new ClassObjectFilter(k.getKey())))
                // the filter of a supertype hands out the beans of its subtypes too, which their own class counts
                if (o.getClass() == k.getKey() && types.put(o, k.getValue()) == null) beans.add(o);
        return targetOutsideInput("vuur", build, beans, types, new HashMap<>(classes));
    }

    /** The bean class of every object type of the model of {@code build}, with that object type. */
    private static Map<Class<?>, ObjectType> beansClasses(Build build) {
        Map<Class<?>, ObjectType> out = new HashMap<>();
        for (ObjectType ot : build.model().objectTypes.values())
            try {
                out.put(Class.forName(FactModelGenerator.PACKAGE + "." + Namen.ident(ot.name, true), false,
                        build.beanLoader()), ot);
            } catch (ClassNotFoundException e) {
                throw new IllegalStateException("geen beanklasse voor objecttype " + ot.name, e);
            }
        return out;
    }

    /** The relations of every bean class of {@code classes}: its reference fields, each with its getter, which the
     *  watch of the entry reads to tell an update of an attribute or a characteristic from one of a relation. */
    private static Map<Class<?>, Watch.RelationFields> relationFields(Build build, Map<Class<?>, ObjectType> classes) {
        Map<Class<?>, Watch.RelationFields> out = new HashMap<>();
        for (Map.Entry<Class<?>, ObjectType> k : classes.entrySet()) {
            List<String> fields = referenceFields(build.model(), k.getValue());
            List<Function<Object, Object>> readers = new ArrayList<>();
            for (String field : fields) readers.add(Accessors.eigenschapGetter(k.getKey(), field));
            out.put(k.getKey(), new Watch.RelationFields(fields, readers));
        }
        return out;
    }

    /** The beans {@code session} holds, of the classes of {@code classes}, asked per class. */
    private static List<Object> beansInSession(KieSession session, Map<Class<?>, ObjectType> classes) {
        List<Object> out = new ArrayList<>();
        for (Class<?> k : classes.keySet())
            for (Object o : session.getObjects(new ClassObjectFilter(k)))
                // the filter of a supertype hands out the beans of its subtypes too, which their own class counts
                if (o.getClass() == k) out.add(o);
        return out;
    }

    /** The object type whose bean {@code beanObject} is, a bean of the build's bean model. */
    private static ObjectType objectType(Build build, Object beanObject) {
        for (ObjectType ot : build.model().objectTypes.values()) {
            if (!Namen.ident(ot.name, true).equals(beanObject.getClass().getSimpleName())) continue;
            try {
                if (Class.forName(FactModelGenerator.PACKAGE + "." + Namen.ident(ot.name, true), false,
                        build.beanLoader()) == beanObject.getClass()) return ot;
            } catch (ClassNotFoundException e) {
                throw new IllegalStateException("geen beanklasse voor objecttype " + ot.name, e);
            }
        }
        throw new IllegalArgumentException("insert: " + name(beanObject) + " is geen bean van dit model: een bean is een"
                + " instantie van een klasse die Build.beanLoader() laadt");
    }

    /** The {@code BucketInput} of bucket scalar {@code e}, keyed as {@code ProducerConsumer.elementOf} keys the
     *  attribute, on the type that declares it. */
    private static BucketInput bucketInput(Map<Object, ObjectType> types, Input.Bucket e) {
        ObjectType ot = types.get(e.bean());
        if (ot == null)
            throw new IllegalArgumentException("insert: emmerscalair '" + e.attribute() + "' op " + name(e.bean())
                    + ", een bean die niet in de invoer staat");
        if (ot.attribute(e.attribute()) == null)
            throw new IllegalArgumentException("insert: emmerscalair '" + e.attribute() + "' op " + name(e.bean())
                    + ", maar " + ot.name + " heeft geen attribuut van die naam");
        if (!Namen.containsName(ot.granularityMembers(), e.attribute()))
            throw new IllegalArgumentException("insert: emmerscalair '" + e.attribute() + "' op " + name(e.bean())
                    + ", maar dat attribuut van " + ot.name + " ligt niet op een tijdlijn; alleen de invoer van een"
                    + " tijdlijn kent de emmerconventie (RUNTIME-SEMANTICS ch. 8.4)");
        return new BucketInput(e.bean(), ProducerConsumer.elementOf(ot, e.attribute(), false));
    }

    /** One to-many list: the reference field {@code field} its members hold toward {@code houder}, and the relation that
     *  field stands for on the members' bean ({@link FactModelGenerator#origin}), since beans of two types can hold
     *  fields of one name toward one holder over two fact types, and a fold reads the members of one relation. The
     *  holder is compared by identity, as the beans do not override {@code equals}. */
    private record ListKey(String field, Object houder, FactModelGenerator.Origin relation) {}

    /**
     * The places of the members of every to-many list a rule reads ({@code read}, the fields the members hold toward
     * the holder): per field, holder and relation, the beans of the input whose field holds the holder, in the order of
     * the beans, or in the order of the list {@code opgegeven} gives for that holder. A given list is checked against the
     * beans whether or not a rule reads its field, and counts only where one does; split by the relations of its
     * members, the order kept, it must name every bean of the input whose field of that relation holds the holder.
     */
    private static List<ListPlace> listPlaces(Build build, Set<String> read, List<Object> beans,
                                                   Map<Object, ObjectType> types, List<Input.ToManyList> opgegeven) {
        RegelSpraakModel model = build.model();
        Set<String> fields = new HashSet<>(read);
        for (Input.ToManyList l : opgegeven) fields.add(Namen.ident(l.role(), false));
        Map<ObjectType, List<String>> referenceFields = new HashMap<>();
        Map<ObjectType, Map<String, FactModelGenerator.Origin>> relations = new HashMap<>();
        Function<Object, Function<String, FactModelGenerator.Origin>> relation = beanObject -> field ->
                relations.computeIfAbsent(types.get(beanObject), t -> new HashMap<>())
                        .computeIfAbsent(field, v -> FactModelGenerator.origin(model, types.get(beanObject), v));
        // per list, the beans whose field holds the holder, in the order of the beans
        Map<ListKey, List<Object>> actual = new LinkedHashMap<>();
        for (Object beanObject : beans)
            for (String field : referenceFields.computeIfAbsent(types.get(beanObject), t -> referenceFields(model, t))) {
                if (!fields.contains(field)) continue;
                Object houder = Accessors.leesEigenschap(beanObject, field);
                if (houder != null)
                    actual.computeIfAbsent(new ListKey(field, houder, relation.apply(beanObject).apply(field)),
                            k -> new ArrayList<>()).add(beanObject);
            }
        Map<ListKey, List<Object>> lists = new LinkedHashMap<>();
        actual.forEach((k, members) -> { if (read.contains(k.field())) lists.put(k, members); });
        Set<ListKey> given = new HashSet<>();
        for (Input.ToManyList l : opgegeven) {
            String field = Namen.ident(l.role(), false);
            String description = "insert: de lijst van " + name(l.houder()) + " onder de rol '" + l.role() + "'";
            Map<ListKey, List<Object>> parts = new LinkedHashMap<>();
            Set<Object> named = java.util.Collections.newSetFromMap(new IdentityHashMap<>());
            for (Object member : l.members()) {
                ObjectType ot = types.get(member);
                if (ot == null) throw new IllegalArgumentException(description + " noemt " + name(member) + ", dat geen bean van de invoer is");
                if (!referenceFields.computeIfAbsent(ot, t -> referenceFields(model, t)).contains(field))
                    throw new IllegalArgumentException(description + " noemt " + name(member) + ", maar een " + ot.name
                            + " heeft geen referentieveld '" + field + "'; de rol is die van de houder, de naam van het"
                            + " veld waarmee een lid naar de houder wijst");
                Object houder = Accessors.leesEigenschap(member, field);
                if (houder != l.houder())
                    throw new IllegalArgumentException(description + " noemt " + name(member) + ", maar diens veld '" + field
                            + "' wijst " + (houder == null ? "nergens heen" : "naar " + name(houder)));
                if (!named.add(member)) throw new IllegalArgumentException(description + " noemt " + name(member) + " twee keer");
                parts.computeIfAbsent(new ListKey(field, houder, relation.apply(member).apply(field)),
                        k -> new ArrayList<>()).add(member);
            }
            if (parts.isEmpty())
                for (Map.Entry<ListKey, List<Object>> e : actual.entrySet())
                    if (e.getKey().field().equals(field) && e.getKey().houder() == l.houder())
                        throw new IllegalArgumentException(description + " is leeg gegeven, maar " + name(e.getValue().get(0))
                                + " wijst met zijn veld '" + field + "' naar de houder");
            for (Map.Entry<ListKey, List<Object>> part : parts.entrySet()) {
                if (!given.add(part.getKey())) throw new IllegalArgumentException(description + " is twee keer gegeven");
                for (Object member : actual.getOrDefault(part.getKey(), List.of()))
                    if (!named.contains(member))
                        throw new IllegalArgumentException(description + " mist " + name(member) + ", dat met zijn veld '" + field
                                + "' naar de houder wijst: een lid buiten de gegeven volgorde heeft geen plaats");
                if (read.contains(field)) lists.put(part.getKey(), part.getValue());
            }
        }
        List<ListPlace> out = new ArrayList<>();
        lists.forEach((k, members) -> {
            for (int i = 0; i < members.size(); i++) out.add(new ListPlace(members.get(i), k.field(), i));
        });
        return out;
    }

    /** The reference fields of a bean of {@code ot}, its inherited ones among them: the members the bean model derives
     *  whose type is the bean class of an object type (§13.3.9, {@link FactModelGenerator#members}). */
    private static List<String> referenceFields(RegelSpraakModel model, ObjectType ot) {
        Set<String> classes = new HashSet<>();
        for (ObjectType t : model.objectTypes.values()) classes.add(Namen.ident(t.name, true));
        List<String> out = new ArrayList<>();
        for (ObjectType t : ot.chain())
            for (FactModelGenerator.Member member : FactModelGenerator.members(model, t))
                if (classes.contains(member.javaType())) out.add(member.property());
        return out;
    }

    /** An object in a sentence of {@link #insert}: its type and its identity, as the facts of the branch print it. */
    private static String name(Object o) {
        return o.getClass().getSimpleName() + "@" + Integer.toHexString(System.identityHashCode(o));
    }

    /**
     * What stands in {@code session} after a {@link #fire} that reached its fixpoint,
     * read without writing anything on a bean: per object the derived attribute values, in the form the bean field takes
     * them, a timeline as the bean model's segments, and the derived characteristics; the objects and links the
     * creations made; the evaluation faults that stand, as data and not thrown; and the §9.5 deposits, the
     * {@code Inconsistency} facts. A consumer that fires one session of a model without a creation more than once reads
     * every answer but the last here: the beans hold the input alone until {@link #setDerivedValues}, which writes
     * exactly this, and after which the session does not fire again. A fault names its object by type and identity,
     * and by its label in {@link #fixpointOf(Build, KieSession, Function)}.
     *
     * <p>It reads only a session whose last {@link #fire} reached its fixpoint and in which nothing happened since outside
     * {@code fire}: no fact inserted, updated or deleted, and no rule fired, by a plain {@code fireAllRules} among others.
     * What it gives is thus always a fixpoint, that of the input as the session knows it: Drools does not see an
     * attribute or a characteristic set without an {@code update}, and neither does this entry, which compares only
     * the reference fields with what {@link #insert} put in. After an {@code update}, a consumer fires
     * with {@code fire} before it reads again; where {@code fire} refuses the session, the sentence says to open a fresh
     * one.
     *
     * @throws IllegalStateException         a session that was not fired through {@link #fire}, among them one fired
     *                                       with a plain {@code fireAllRules}, whose last {@code fire} ended without a
     *                                       fixpoint ({@link Firing#noFixpoint}) or with an exception, or in which a
     *                                       fact was inserted, updated or deleted, or a rule fired, after the last
     *                                       {@code fire} and outside it, or a global or, without an update, a relation
     *                                       changed; the sentence says which, and what to do
     */
    public static Fixpoint fixpointOf(Build build, KieSession session) {
        return readFixpoint(build, session, null);
    }

    /** As {@link #fixpointOf(Build, KieSession)}, with {@code label} giving the case label of an input bean and null for an
     *  object a creation made, so that a fault in the {@link Fixpoint} that names its object names it as the reference
     *  interpreter does: "V, een Vondst" for two writers that disagree (RUNTIME-SEMANTICS ch. 5.7) and for a timeline
     *  input in the other convention (ch. 8.4), "een aangemaakte Vondst" for a made object, where the variant without
     *  labels gives the type and the identity. The names are those {@link #setDerivedValues(Build, KieSession,
     *  Function)} gives the faults it throws. */
    public static Fixpoint fixpointOf(Build build, KieSession session, Function<Object, String> label) {
        Objects.requireNonNull(label, "label");
        return readFixpoint(build, session, label);
    }

    private static Fixpoint readFixpoint(Build build, KieSession session, Function<Object, String> label) {
        Objects.requireNonNull(build, "build");
        Objects.requireNonNull(session, "session");
        return build.execModelTranspiler().readOf(session, label);
    }

    /**
     * Puts what the branch derived in {@code session} on the fact beans, after the last {@link #fire}
     * of the session, which reached its fixpoint: exactly what {@link #fixpointOf} gives, read by the same code. It sets each
     * derived characteristic and attribute value on its bean, and each link a creation laid on the reference fields that
     * hold it (§13.3.9). Until this call the beans hold the input alone, since the branch writes no bean while it fires.
     *
     * <p>Before anything is written, it throws the evaluation faults that stand at the fixpoint, and then writes nothing:
     * one fault as its own exception, which is a {@link Breuk.DelingDoorNul} for a division by a numeric zero (an
     * {@code ArithmeticException}, named after the rule with the operands), a {@code ComputeFault} for a fault of a
     * computation or of the evaluation date, a
     * {@code WritersConflict} for two writers of one attribute that disagree (an {@code IllegalStateException},
     * RUNTIME-SEMANTICS ch. 5.7) and an {@code InputFormFault} for a timeline input in the other convention (ch. 8.4);
     * two or more as {@code EvaluationFaults}, ordered by the name of the form and then by the sentence, with the faults
     * as {@code faults()}. {@link #fixpointOf} gives the same faults as data.
     *
     * <p>It refuses the sessions {@link #fixpointOf} refuses, with the same {@code IllegalStateException}, before anything is
     * written: one not fired through {@link #fire}, one whose last {@code fire} ended without a fixpoint, and one in which
     * something happened outside {@code fire} after the last one.
     *
     * <p>The write-back tells the engine nothing, and a bean then holds derived values beside its input, which a further
     * fire would read as input: an initialisation of a member that holds a value written back would not fire again. So
     * the write-back closes the session with a stated mark, and {@link #fire} refuses it.
     */
    public static void setDerivedValues(Build build, KieSession session) {
        Objects.requireNonNull(build, "build");
        Objects.requireNonNull(session, "session");
        build.execModelTranspiler().setDerivedValues(session);
    }

    /** As {@link #setDerivedValues(Build, KieSession)}, with {@code label} giving the case label of an input bean
     *  and null for an object a creation made, so that a fault names its object as the reference interpreter names it:
     *  "V, een Vondst" in the fault of two writers that disagree (RUNTIME-SEMANTICS ch. 5.7), where a consumer without
     *  labels gets the type and the identity. The replay writes back through it. */
    public static void setDerivedValues(Build build, KieSession session, Function<Object, String> label) {
        Objects.requireNonNull(build, "build");
        Objects.requireNonNull(session, "session");
        Objects.requireNonNull(label, "label");
        build.execModelTranspiler().setDerivedValues(session, label);
    }

    /**
     * The factor of the fire cap, {@code FACTOR × R × N0} ({@link #fire}), with N0 the
     * facts before the fire and, for a model with a creation, the facts at the start of each phase. Every firing of that
     * branch makes or renews a logical fact of one rule on one subject, and without a cycle, which the build refuses
     * ({@code Cycles}), the renewals per rule and subject are bounded by the depth of the dependency graph, so a run
     * fires a number of times that grows with the rules and the facts. It is twice the largest ratio measured, rounded
     * up, and at least 4. Measured 2026-10-10 (card {@code tms-omschakeling}, step 4): over the 248 goldens the branch
     * plays ({@code TmsMeasurement}), a case needs at most 1.167 firings per rule per fact the cap counts
     * ({@code anchor/jutterswet-maatschap}), counted per stretch of its run up to the start of the next creation phase;
     * the scale model ({@code ScaleTrial}) fires 0.563 times per rule per fact before its fire. Counted against the
     * facts before the fire alone, a chain of creations from one input object reaches 48.7
     * ({@code synth2-budgetgrens-onder}) while it uses a fiftieth of its cap, since the cap counts what each phase made;
     * twice that ratio would loosen the cap of every model 24-fold, so it does not set the factor.
     */
    static final int FACTOR = 4;

    /** The least fire cap: below it a small document would get a cap of a few hundred
     *  firings, which a fixpoint can need. */
    static final int FLOOR = 10_000;

    /** What {@link #fire} did: the firings of all its fires together; the cap it fired under, the cap of its last
     *  phase; and the sentence of a run that ends without a fixpoint, at the cap or at the
     *  creation budget ({@code CreationBudget}), and null at a fixpoint. A run that ends at the budget counts the firings
     *  of its fires before the one in which the creation rule threw. */
    public record Firing(int firings, int cap, String noFixpoint, java.util.Map<String, String> budget) {

        public Firing(int firings, int cap, String noFixpoint) {
            this(firings, cap, noFixpoint, java.util.Map.of());
        }

        /** The EF code of the fault kind of {@link #noFixpoint} (RUNTIME-SEMANTICS ch. 5.7b), beside the sentence and
         *  never inside it: EF-015 where the run ended at the creation budget; null at a fixpoint and at the cap, which
         *  is a safeguard of this backend and no fault kind. The two ends without a fixpoint are told apart by the
         *  count, not by the sentence: {@code fire} ends at the cap only with {@code firings >= cap}, and a creation
         *  rule throws at the budget only inside a phase that starts below the cap, whose firings it does not count. */
        public String code() {
            return noFixpoint != null && firings < cap ? "EF-015" : null;
        }

        /** The parameters of {@link #code}'s kind (ch. 5.7b) the sentence of the creation budget carries: the creation,
         *  its stratum and the depth the mint would reach. Empty where there is no code. */
        public java.util.Map<String, String> params() {
            return code() == null ? java.util.Map.of() : budget;
        }
    }

    /**
     * Fires {@code session} of {@code build} to its fixpoint under a cap on the firings of the run, of
     * {@code max(}{@value #FLOOR}{@code , FACTOR × R × N0)}, with {@code R} the rules of the session's KieBase and
     * {@code N0} the facts the session holds just before it fires ({@link #FACTOR}): the cap is a safeguard against a
     * fault of the lowering, not a norm, so it grows with the input. The sentence at the cap names {@code R}, {@code N0}
     * and the cap. A run that reaches the cap or the creation budget has no fixpoint, and {@link Firing#noFixpoint}
     * says so. Every run leaves a stated mark of how it ended, at its fixpoint, without
     * one, or with an exception out of the fire ({@code Fired}), and keeps a watch on the session from then on
     * ({@code Watch}), a listener that it takes off while it fires itself; {@link #fixpointOf} and
     * {@link #setDerivedValues} read a session only where the last run reached its fixpoint and the watch saw nothing
     * happen outside {@code fire} since.
     *
     * <p>It refuses a session that {@link #setDerivedValues} wrote back, whose beans
     * hold derived values beside their input: a consumer that fires more than once reads between the fires with
     * {@link #fixpointOf}, or opens a fresh session with beans that hold input alone.
     *
     * <p>It refuses, before it fires, a session in which, since {@link #insert} or the
     * last {@code fire}, more happened outside it than an update of an attribute or a characteristic of a bean
     * {@code insert} put in: a fact inserted or deleted, among them a bean, a bucket input, a list place or a fact of
     * the branch; an update of a fact that is no such bean, or one that changed a relation, a reference field; a rule
     * that fired; and a change of the agenda, a cleared group or a focus change, read from Drools' agenda where no event
     * reports it. {@code insert} states the places of a list and the bucket scalars over the beans and relations of one
     * input, the branch states its own facts, and only this fires under the cap and in generations, so the way is a
     * fresh session. It refuses as well, after its first run, a session whose globals changed since that run, since the
     * branch states a global as a fact once, at the first fire, and a session from which a consumer took the watch. It
     * repeats the reference check of {@code insert} over the beans of the session, which a reference field set without
     * an update can fail, and refuses in the sentence of {@code insert}; and it refuses a session in which a reference
     * field of a bean {@code insert} put in holds another object than it held then, set without an update, which no
     * event shows and after which Drools keeps reading the old relation.
     *
     * <p>It fires a session of a model with a creation once, and refuses it at every
     * later call. An object or a fact creation is stated and never lapses, and so are the initialisations, the links
     * and the fault of a failed initialisation it brought (RUNTIME-SEMANTICS ch. 5.5, {@code Fired}): after an input
     * change a second fire would keep them beside an input they no longer follow from, a state no fresh evaluation
     * gives. The sentence says so, and that the way is a fresh session per input.
     *
     * <p>A model with a creation fires in generations; see {@link #fire(Build, KieSession, int)}. There the cap is
     * computed again over the facts at the start of every phase, so the objects the creations made count, and never
     * lowered: a later phase may hold fewer facts than an earlier one, and the cap bounds the run as a whole.
     */
    public static Firing fire(Build build, KieSession session) {
        Objects.requireNonNull(build, "build");
        Objects.requireNonNull(session, "session");
        return fire(build, session, FACTOR, FLOOR);
    }

    /**
     * As {@link #fire(Build, KieSession)}, with {@code observer} recording what the session fires in this run. Its
     * listener goes on the session when this starts and comes off when it ends, also where this refuses the session or
     * the fire throws, so it hears this run and nothing before or after it. The run itself is the same: the same cap,
     * the same refusals, the same {@link Firing}.
     *
     * @throws IllegalArgumentException a waarnemer that observed another session ({@link Observer})
     */
    public static Firing fire(Build build, KieSession session, Observer observer) {
        Objects.requireNonNull(build, "build");
        Objects.requireNonNull(session, "session");
        Objects.requireNonNull(observer, "observer");
        observer.bind(session);
        AgendaEventListener listener = observer.listener(build.execModelTranspiler());
        session.addEventListener(listener);
        try {
            return fire(build, session, FACTOR, FLOOR);
        } finally {
            session.removeEventListener(listener);
        }
    }

    /**
     * What one session fired, as {@link #fire(Build, KieSession, Waarnemer)} records it: per firing of a rule that
     * belongs to a form of the document ({@link TmsTranspiler#formOf}) the exact name of that form, version suffix
     * included, and per form without its version suffix ({@link RegelSpraakModel#formName}) how often its rules fired
     * and the labels of the input beans their matches bound. The rows are per form, not per rule: the rules of the
     * analysis are named "&lt;form&gt;: &lt;part&gt;", and the part alone puts rules of different forms in one row, as
     * on golden jutterswet-premiestaffel the defaults of three object types under "default 0"; a form's name may itself
     * hold ": ", so only {@code formOf} gives it whole. The versions of one rule share a row, and {@link #forms()}
     * keeps them apart for the coverage ({@link DroolsBackend#coverage}). A rule of no form, a standing or another rule
     * the lowering keeps for every form alike ("kenmerkstand Jutter.uitgesloten: waar uit een afleiding"), is not
     * recorded: it is no rule of the document, and a consumer that names a row after a form of the document has none to
     * name it after.
     *
     * <p>The label of a bean depends on the case, so the caller gives the function: {@code label} returns the label of
     * an input bean and null for any other object, the facts of the branch and the objects a creation made among them,
     * which a row then does not name.
     *
     * <p>A waarnemer belongs to one session: the first {@code fire} it is given binds it to that session, and a
     * {@code fire} of another session refuses it, so sessions that fire at once, as those of a decision service do,
     * never write into one observer. Given to more than one {@code fire} of its session, it gathers what all of them
     * fired. It is read after the fire, by the thread that fired.
     */
    public static final class Observer {
        private final Function<Object, String> label;
        private final Map<String, Integer> count = new LinkedHashMap<>();
        private final Map<String, List<String>> labels = new LinkedHashMap<>();
        private final Set<String> forms = new LinkedHashSet<>();
        private KieSession session;

        public Observer(Function<Object, String> label) {
            this.label = Objects.requireNonNull(label, "label");
        }

        /** The firings per form without its version suffix, in the order a rule of the form first fired, each with
         *  the labels its matches bound, in the order they were first bound, and how often its rules fired. */
        public List<ScenarioResult.Firing> firings() {
            List<ScenarioResult.Firing> out = new ArrayList<>();
            for (Map.Entry<String, Integer> e : count.entrySet())
                out.add(new ScenarioResult.Firing(e.getKey(), List.copyOf(labels.get(e.getKey())), e.getValue()));
            return List.copyOf(out);
        }

        /** The forms a rule of which fired, by the exact name the manifest gives them ({@link Build#coverage}), version
         *  suffix included, in the order they first fired. */
        public Set<String> forms() {
            return java.util.Collections.unmodifiableSet(new LinkedHashSet<>(forms));
        }

        /** Synchronized, so that two threads that hand one fresh waarnemer to two sessions at once cannot both bind
         *  it: one of them is refused. */
        private synchronized void bind(KieSession s) {
            if (session == null) session = s;
            else if (session != s)
                throw new IllegalArgumentException("deze waarnemer nam al een andere sessie waar: een waarnemer hoort"
                        + " bij één sessie, dus geef elke sessie een eigen waarnemer");
        }

        private AgendaEventListener listener(TmsTranspiler tms) {
            return new DefaultAgendaEventListener() {
                @Override public void afterMatchFired(AfterMatchFiredEvent e) {
                    String form = tms.formOf(e.getMatch().getRule().getName());
                    if (form == null) return;
                    forms.add(form);
                    String rule = RegelSpraakModel.formName(form);
                    count.merge(rule, 1, Integer::sum);
                    List<String> ls = labels.computeIfAbsent(rule, k -> new ArrayList<>());
                    for (Object o : e.getMatch().getObjects()) {
                        String l = label.apply(o);
                        if (l != null && !ls.contains(l)) ls.add(l);
                    }
                }
            };
        }
    }

    /**
     * The coverage of {@code build} in a session that fired the forms {@code firedForms}, by their exact names
     * ({@link Observer#forms}): one row per entry of the manifest ({@link Build#coverage}), in its order, FIRED where
     * the entry's name is among them and NOT_FIRED where it is not. The name is compared exactly, so each version of
     * a rule has a verdict of its own: on golden anchor/jutterswet-jutheffing the second version of "jutheffing naar
     * jaartarief" is not reached in 2025, and a comparison on the name without the version suffix called it fired.
     *
     * @throws IllegalArgumentException a fired form that is no entry of the manifest, which no session of this build
     *                                  fires: the forms come from another build
     */
    public static List<ScenarioResult.CoverageRule> coverage(Build build, Set<String> firedForms) {
        Objects.requireNonNull(build, "build");
        Objects.requireNonNull(firedForms, "firedForms");
        Set<String> posten = new HashSet<>();
        for (Coverage d : build.coverage()) posten.add(d.name());
        for (String form : firedForms)
            if (!posten.contains(form))
                throw new IllegalArgumentException("de vorm '" + form + "' is geen post van het manifest van deze build,"
                        + " dus geen sessie van deze build vuurt haar; komen de gevuurde vormen van een andere build?");
        List<ScenarioResult.CoverageRule> out = new ArrayList<>();
        for (Coverage d : build.coverage())
            out.add(new ScenarioResult.CoverageRule(d.name(), firedForms.contains(d.name())
                    ? ScenarioResult.CoverageKind.FIRED : ScenarioResult.CoverageKind.NOT_FIRED));
        return List.copyOf(out);
    }

    /**
     * The verdict of the expectations of a case on a session ({@link #compare}): per expectation, in the order of the
     * case, its {@link ScenarioResult.ExpectationResult}, and the diagnose of the scenario report, the line of
     * every expectation that did not hold, joined by "; ", empty where every one held. The scenario report
     * ({@code CaseRunner.report}) puts the name of the case file and ": " before a diagnose that is not empty.
     */
    public record Comparison(List<ScenarioResult.ExpectationResult> expectations, String diagnostic) {
        public Comparison {
            expectations = List.copyOf(expectations);
            Objects.requireNonNull(diagnostic, "diagnostic");
        }

        /** Whether every expectation held. */
        public boolean pass() {
            return expectations.stream().allMatch(ScenarioResult.ExpectationResult::ok);
        }
    }

    /**
     * Compares {@code expectations}, the {@code expected} of a {@code uchar-case-v0} case, with {@code session} of
     * {@code build}: per expectation the verdict the scenario report gives ({@code CaseRunner.report}), which compares
     * through this, as the replay of every case does. It reads the beans of a session that {@link #setDerivedValues}
     * wrote back: they then carry its fixpoint, the input and over it what {@link #fixpointOf} gives, and they are the answer a
     * consumer hands out. Since the write-back closes the session, a session is compared once, after its last fire. A
     * label of an expectation names the bean of the session to which {@code label} gives it, the function that gives the
     * label of an input bean and null for any other object, as for {@link #fixpointOf} and {@link Observer}: the labels are
     * the caller's, the ids of a request among them, and need not be those of a case file. An expectation compares as
     * the replay compares: a number by value whatever its scale, an enumeration label up to the name equality of the
     * reference interpreter, a timeline segment for segment, and a §9.5 deposit by its rule and, where the expectation
     * names one, the label of its subject. An empty list passes, as in the replay, though {@code uchar-case-v0} asks
     * for at least one expectation.
     *
     * @throws IllegalStateException    what {@link #fixpointOf} refuses: a session not fired through {@link #fire}, whose last
     *                                  {@code fire} reached no fixpoint, or in which something happened outside
     *                                  {@code fire} since; a session whose fixpoint carries evaluation faults, which
     *                                  {@link #setDerivedValues} throws instead of writing back; and a session that
     *                                  was not written back
     * @throws IllegalArgumentException {@code expectations} that are no array; a {@code label} that gives one label to
     *                                  two beans of the session; and a defect of the case that the replay refuses as
     *                                  well, an expectation on a member with a label no bean of the session carries
     *                                  among them
     * @throws CaseRunner.Excluded      an expectation of a form this backend does not compare, in the sentence of the
     *                                  replay, which the scenario report gives as a MARKER: among them a dimensioned
     *                                  attribute, a scalar read of an attribute on a timeline, a member without a bean
     *                                  field of its own, a rational number, a timeline on a grid the case timelines
     *                                  cannot lie on, and a kind {@code uchar-case-v0} does not have
     */
    public static Comparison compare(Build build, KieSession session, Function<Object, String> label,
                                         JsonNode expectations) {
        Objects.requireNonNull(build, "build");
        Objects.requireNonNull(session, "session");
        Objects.requireNonNull(label, "label");
        Objects.requireNonNull(expectations, "expectations");
        if (!expectations.isArray())
            throw new IllegalArgumentException("vergelijk: de verwachtingen zijn de array 'expected' van een case, en dit"
                    + " is een " + expectations.getNodeType());
        List<Fault> faults = fixpointOf(build, session, label).faults();
        if (!faults.isEmpty())
            throw new IllegalStateException("vergelijk: aan het fixpunt van deze sessie staan " + faults.size()
                    + " evaluatiefout(en), de eerste van '" + faults.get(0).rule() + "': " + faults.get(0).message()
                    + "; de regels geven op deze invoer geen antwoord, en setDerivedValues werpt de fouten en zet niets"
                    + " terug, dus er is niets om met de verwachtingen te vergelijken");
        if (session.getObjects(new ClassObjectFilter(WrittenBack.class)).isEmpty())
            throw new IllegalStateException("vergelijk: deze sessie is niet teruggezet; de beans dragen het fixpunt pas na"
                    + " DroolsBackend.setDerivedValues, en tot dan alleen de invoer, dus zet eerst terug");
        Map<String, Object> perLabel = new HashMap<>();
        Map<String, ObjectType> types = new HashMap<>();
        for (Map.Entry<Class<?>, ObjectType> k : beansClasses(build).entrySet())
            for (Object beanObject : session.getObjects(new ClassObjectFilter(k.getKey()))) {
                // the filter of a supertype hands out the beans of its subtypes too, which their own class counts
                if (beanObject.getClass() != k.getKey()) continue;
                String l = label.apply(beanObject);
                if (l == null) continue;
                Object earlierBean = perLabel.putIfAbsent(l, beanObject);
                if (earlierBean != null)
                    throw new IllegalArgumentException("vergelijk: de labelfunctie geeft het label '" + l + "' aan "
                            + name(earlierBean) + " en aan " + name(beanObject) + ", dus een verwachting op '" + l + "' wijst geen"
                            + " bean aan");
                types.put(l, k.getValue());
            }
        return CaseRunner.comparison(build, session, perLabel, types, expectations);
    }

    /** As {@link #fire(Build, KieSession)} on every branch under the cap {@code max(vloer, factor × R × N0)}, rounded
     *  up: the seam a witness gives a small factor and floor to reach the cap, a factor below 1 among them. */
    static Firing fire(Build build, KieSession session, double factor, int floor) {
        if (!(factor > 0)) throw new IllegalArgumentException("vuurfactor " + factor + " is niet groter dan 0");
        return drive(build, session, new Cap(factor, floor, rules(session)));
    }

    /**
     * Fires {@code session} of {@code build} to its fixpoint, at most {@code cap} firings in all, a cap that does not
     * grow: the seam of the scale harness, which sets a cap that never cuts it off, and of a witness that sets a small
     * one. Without a lowered creation that is one fire.
     *
     * <p>With a creation a creation fires only at a fixpoint of the rule layer, and
     * what it makes is stated and never lapses, as in the reference interpreter (RUNTIME-SEMANTICS ch. 5.5): a logical
     * object could keep itself standing through its own output after the state that made it passed, and its
     * initialisations would follow later changes, where ch. 5.5 evaluates them when the object is made. So this fires
     * in generations, here and not in the rules, so that the two renderings of the analysis share it. The creation
     * rules sit in agenda groups of their own ({@code TmsTranspiler.FACT_CREATION}, {@code TmsTranspiler.OBJECT_CREATION}),
     * so a fire without their focus takes the rule layer to rest and leaves them pending. A generation is one phase: it
     * opens a new {@code CreationPhase}, puts both groups on the focus stack, the fact creations on top, and fires. That
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
     * ({@code TmsTranspiler.creationStrata}): the phases of stratum k carry k, and a creation rule fires only in a phase
     * of its own stratum or one above. A creation writes irrevocably, so it reads strictly and sits above the producers
     * of what it reads (RUNTIME-SEMANTICS ch. 6.1); the reference interpreter evaluates stratum k to its fixpoint before
     * a form of stratum k+1 fires (ch. 6.3). In one phase for all strata, a creation that reads what another creation
     * of the same generation makes would read the state before it, and its initialisation would stay fixed on that
     * value. The strata order only the creations, which are stated: the rule layer stays under truth maintenance and
     * reaches its fixpoint after every phase.
     *
     * <p>A stratum ends at its first generation that makes nothing. The creation budget (RUNTIME-SEMANTICS ch. 5.5) is
     * the depth of a chain of mints, not a count of generations: the creation rule computes the depth of what it makes
     * from the mark of its subject and throws {@link CreationBudget} at a mint past {@code TmsTranspiler.CREATION_BUDGET},
     * and this ends the run there, with its sentence, as a run without a fixpoint. The generations need no bound of
     * their own: every generation but the last makes an object or lays a link, and under the budget a stratum makes
     * finitely many of both. The rule layer has no budget here either: truth maintenance takes it to rest within the
     * fire, and the cap bounds that. Until 2026-10-09 this counted the generations of a stratum that make
     * something, at most 100, as the reference interpreter did that day: a chain whose every mint waits a generation for
     * a link, and a fact creation that lays a closure of 102 hops, ended without a fixpoint at their 101st generation.
     */
    static Firing fire(Build build, KieSession session, int cap) {
        return drive(build, session, new Cap(cap));
    }

    /** The rules of the KieBase {@code session} fires: the {@code R} of the cap. */
    private static long rules(KieSession session) {
        long n = 0;
        for (org.kie.api.definition.KiePackage p : session.getKieBase().getKiePackages()) n += p.getRules().size();
        return n;
    }

    /** The cap of one run of {@link #drive}: a fixed number of firings, or {@code max(vloer, factor × R × N)} over the
     *  facts {@code N} at the start of a phase, rounded up and never lowered within the run. */
    private static final class Cap {
        /** 0 for a fixed cap. */
        private final double factor;
        private final int floor;
        private final long rules;
        private int value;
        private long facts = -1;

        /** A fixed cap. */
        Cap(int fixed) {
            this(0, fixed, 0);
        }

        Cap(double factor, int floor, long rules) {
            this.factor = factor;
            this.floor = floor;
            this.rules = rules;
            this.value = floor;
        }

        /** Computes the growing cap over the facts {@code session} holds now, where that raises it. */
        void recompute(KieSession session) {
            if (factor == 0) return;
            long n = session.getFactCount();
            // a double holds every product below 2^53 exactly, and a cap is an int
            double recomputed = Math.min(Integer.MAX_VALUE, Math.max(floor, Math.ceil(factor * rules * n)));
            if (facts < 0 || recomputed > value) {
                value = (int) recomputed;
                facts = n;
            }
        }

        /** The sentence of a run that reached the cap. */
        String sentence() {
            return factor == 0
                    ? "geen fixpunt: het vuurplafond van " + value + " vuringen is bereikt"
                    : "geen fixpunt: het vuurplafond van " + value + " vuringen is bereikt, het grootste van " + floor
                      + " en " + java.math.BigDecimal.valueOf(factor).stripTrailingZeros().toPlainString() + " × "
                      + rules + " regels × " + facts + " feiten vlak vóór het vuren; zonder kring vuurt de tak per"
                      + " regel en feit een begrensd aantal keren, dus dit is een fout van de verlaging";
        }
    }

    /** The driver of every {@code fire}. It refuses a session of a model with a creation
     *  that it fired before, and a session that was written back, and it marks how the run ended ({@link Fired}),
     *  which {@link #fixpointOf} and {@link #setDerivedValues} read: the mark
     *  of the previous run goes before the fire, so none stands while the session fires, and a fire that throws leaves
     *  a mark with its exception, so that no reading follows a run that broke off. It runs with the watch of the session
     *  off ({@link Watch}), so that its own inserts, deletes and firings count as none, and puts the watch back
     *  afterwards with nothing kept: the two readings see what happens after this run. A refusal leaves the watch as it
     *  was, so a reading after it still sees what happened before. It refuses as well what {@link #fire(Build,
     *  KieSession)} lists: more than an update of an attribute or a characteristic of a bean of the input since
     *  {@link #insert} or the last run, a global changed since the first run, a fired session without its watch, a
     *  reference field that holds an object outside the session, and a relation changed without an update. At the first
     *  run it puts the watch on where
     *  {@code insert} did not, with the beans the session holds as the input, and keeps the globals. */
    private static Firing drive(Build build, KieSession session, Cap cap) {
        TmsTranspiler tms = build.execModelTranspiler();
        // per class: Drools keeps the facts of a class apart and hands out only those for a ClassObjectFilter, where a
        // filter of another kind walks every fact the session holds, at every vuur
        List<FactHandle> marks = List.copyOf(session.<FactHandle>getFactHandles(new ClassObjectFilter(Fired.class)));
        // what a creation made is stated and does not lapse, so a later fire would keep it beside an input it no longer
        // follows from; checked first, since a written-back session of such a model cannot be read between fires either
        if (!marks.isEmpty() && !tms.creationRules().isEmpty())
            throw new IllegalStateException("dit model heeft creatieregels, en deze sessie is al via DroolsBackend.fire"
                    + " gevuurd: wat een creatie maakte, met de initialisaties, de koppelingen en de fout van een mislukte"
                    + " initialisatie, is gesteld en vervalt niet (RUNTIME-SEMANTICS ch. 5.5), dus een volgende fire zou"
                    + " na een invoerwijziging een toestand geven die geen verse evaluatie geeft; open per invoer een"
                    + " verse sessie");
        if (!session.getObjects(new ClassObjectFilter(WrittenBack.class)).isEmpty())
            throw new IllegalStateException("deze sessie is teruggezet (setDerivedValues): haar beans dragen afgeleide"
                    + " waarden naast de invoer, en een volgende fire zou die als invoer lezen; lees tussen twee fires met"
                    + " DroolsBackend.fixpointOf en zet pas na de laatste fire terug, of open een verse sessie met beans die"
                    + " alleen invoer dragen");
        Watch installedWatch = Watch.van(session);
        // insert puts the watch on, or the first run where there was no insert, and every run puts it back, so only a
        // consumer that took it off leaves a fired session without it, and what happened since is then not to be seen
        if (installedWatch == null && !marks.isEmpty())
            throw new IllegalStateException("de bewaking die DroolsBackend.fire op deze sessie zette, staat er niet meer"
                    + " op, dus wat er na de laatste fire gebeurde, is niet te zien; open een verse sessie met alle beans"
                    + " in één Input");
        if (installedWatch != null && installedWatch.onOrOff() != null)
            throw new IllegalStateException("sinds " + installedWatch.since() + " kwam er buiten insert om invoer bij of ging er"
                    + " invoer weg (" + installedWatch.onOrOff() + "): een afnemer geeft alle beans in één Input aan"
                    + " insert en verandert daarna alleen hun attributen en kenmerken, met update, want insert legt de"
                    + " plaatsen van een lijst en de emmerscalairen vast over de beans van één invoer; een nieuw of"
                    + " verwijderd object vraagt een verse sessie");
        String agenda = Watch.agenda(session);
        String more = installedWatch == null ? agenda : installedWatch.moreThanOneUpdate() != null ? installedWatch.moreThanOneUpdate() : agenda;
        if (more != null)
            throw new IllegalStateException("sinds " + (installedWatch == null ? "het begin van deze sessie" : installedWatch.since())
                    + " gebeurde er buiten fire om meer dan een update van een attribuut of kenmerk van een bean (" + more
                    + "), en meer laat de ingang niet toe: open een verse sessie met alle beans in één Input");
        String global = installedWatch == null ? null : installedWatch.changedGlobal(session);
        if (global != null)
            throw new IllegalStateException(global + ": de tak leest een global één keer, in een regel die haar bij de"
                    + " eerste fire als feit stelt, dus een volgende fire zou met de oude waarde rekenen; zet de globals"
                    + " vóór de eerste fire, en open voor een andere waarde een verse sessie");
        Map<Class<?>, ObjectType> classes = beansClasses(build);
        String outsideSentence = targetOutsideSession(build, session, classes);
        if (outsideSentence != null) throw new IllegalStateException(outsideSentence);
        String relation = installedWatch == null ? null : installedWatch.changedRelation();
        if (relation != null)
            throw new IllegalStateException(relation + ": Drools ziet een veld pas na een update, dus een fire zou de oude"
                    + " relatie blijven lezen, en ook met een update vraagt een gewijzigde relatie een verse sessie, want"
                    + " insert legt de plaatsen van een lijst vast over de relaties van één invoer; open een verse sessie"
                    + " met alle beans in één Input");
        Watch watch = installedWatch != null ? installedWatch
                : Watch.op(session, relationFields(build, classes), beansInSession(session, classes),
                        "de eerste DroolsBackend.fire van deze sessie");
        watch.recordGlobals(session);
        watch.forget("de laatste DroolsBackend.fire van deze sessie");
        return watch.without(session, () -> {
            for (FactHandle mark : marks) session.delete(mark);
            Firing firing;
            try {
                firing = fireUnderCap(build, session, cap);
            } catch (RuntimeException e) {
                try {
                    session.insert(new Fired("de fire brak af met " + e.getClass().getSimpleName() + ": "
                            + e.getMessage()));
                } catch (RuntimeException markError) {
                    e.addSuppressed(markError);
                }
                throw e;
            }
            session.insert(new Fired(firing.noFixpoint()));
            return firing;
        });
    }

    /** Fires {@code session} as {@link #fire(Build, KieSession, int)} describes, under {@code cap}: in one go, or for
     *  a model with a creation in generations per stratum. */
    private static Firing fireUnderCap(Build build, KieSession session, Cap cap) {
        TmsTranspiler tms = build.execModelTranspiler();
        Set<String> creations = tms.creationRules();
        cap.recompute(session);
        if (creations.isEmpty()) {
            int fired = session.fireAllRules(cap.value);
            return new Firing(fired, cap.value, fired >= cap.value ? cap.sentence() : null);
        }
        // Drools makes a group on demand only in a KieBase that declares a group beside MAIN, which every creation
        // rule does; null means the KieBase lacks the creation rules the transpiler reports
        org.kie.api.runtime.rule.Agenda agenda = session.getAgenda();
        for (String group : List.of(TmsTranspiler.FACT_CREATION, TmsTranspiler.OBJECT_CREATION))
            if (agenda.getAgendaGroup(group) == null)
                throw new IllegalStateException("de KieBase kent de agendagroep " + group + " niet, terwijl het model"
                        + " creatieregels heeft: " + creations);
        int[] made = {0};
        AgendaEventListener counter = new DefaultAgendaEventListener() {
            @Override public void afterMatchFired(AfterMatchFiredEvent e) {
                if (creations.contains(e.getMatch().getRule().getName())) made[0]++;
            }
        };
        session.addEventListener(counter);
        try {
            int fired = session.fireAllRules(cap.value);
            FactHandle phase = null;
            int number = 0;
            for (int stratum : tms.creationStrata()) {
                while (fired < cap.value) {
                    made[0] = 0;
                    if (phase != null) session.delete(phase);
                    phase = session.insert(new CreationPhase(++number, stratum));
                    cap.recompute(session);
                    // the focus is a stack: the group given the focus last fires first
                    agenda.getAgendaGroup(TmsTranspiler.OBJECT_CREATION).setFocus();
                    agenda.getAgendaGroup(TmsTranspiler.FACT_CREATION).setFocus();
                    try {
                        fired += session.fireAllRules(cap.value - fired);
                    } catch (ConsequenceException e) {
                        CreationBudget budget = creationBudget(e);
                        if (budget == null) throw e;
                        return new Firing(fired, cap.value, budget.getMessage(), budget.params());
                    }
                    if (made[0] == 0) break;
                }
            }
            return new Firing(fired, cap.value, fired >= cap.value ? cap.sentence() : null);
        } finally {
            session.removeEventListener(counter);
        }
    }

    /** The {@link CreationBudget} a creation rule threw, which Drools' default handler hands on inside {@code e}, or null
     *  where {@code e} carries another fault. */
    private static CreationBudget creationBudget(ConsequenceException e) {
        for (Throwable t = e.getCause(); t != null; t = t.getCause() == t ? null : t.getCause())
            if (t instanceof CreationBudget budget) return budget;
        return null;
    }

    /** Names the fault behind {@code e}, the exception Drools' default handler throws out of the fire when a consequence
     *  throws: a {@link Breuk.DelingDoorNul} in its cause chain becomes one naming the rule, with what the consequence
     *  threw as its cause; any other {@code e} is returned unchanged. Neither branch throws an evaluation fault out of
     *  its fire, which stands as a fact at the fixpoint, so what comes here is a broken contract or a bug, and this
     *  hands it on as it is. Until card drl-teksttak-op-truth-maintenance the DRL-text branch threw a division by a
     *  numeric zero out of the fire, and this named it. {@link CaseRunner} calls it where it fires, so the replay, the
     *  red path and the scenario report read the sentence; a consumer that fires a KieBase itself can call it the same
     *  way.
     *
     *  <p>The KieBases built here keep Drools' default consequence handler on purpose. Drools takes a handler by class
     *  name and instantiates it in every new session; where the kie API and this backend are loaded by different
     *  class loaders (the distribution's Quarkus test of its decision service) that instance does not cast to the kie
     *  interface, and every {@code newKieSession} there threw a {@code ClassCastException} (2026-10-02). */
    public static RuntimeException designate(ConsequenceException e) {
        for (Throwable t = e.getCause(); t != null; t = t.getCause() == t ? null : t.getCause())
            if (t instanceof Breuk.DelingDoorNul zero) return zero.inDeRegel(e.getRule().getName(), e.getCause());
        return e;
    }
}
