package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import dev.uchar.regelspraak.engine.ScenarioResult;
import dev.uchar.regelspraak.message.Msg;
import dev.uchar.regelspraak.refusal.DroolsCode;
import dev.uchar.regelspraak.refusal.Reason;
import dev.uchar.regelspraak.refusal.Reason;
import dev.uchar.regelspraak.loader.IrLoader;
import dev.uchar.regelspraak.core.FixedStartPoint;
import dev.uchar.regelspraak.model.Datatype;
import dev.uchar.regelspraak.ir.Expr;
import dev.uchar.regelspraak.ir.PrioritizedDefaults;
import dev.uchar.regelspraak.ir.ResolvedRule;
import dev.uchar.regelspraak.ir.Spelling;
import dev.uchar.regelspraak.model.RegelSpraakModel;
import dev.uchar.regelspraak.model.RegelSpraakModel.Granulariteit;
import dev.uchar.regelspraak.model.RegelSpraakModel.Objecttype;
import dev.uchar.regelspraak.runtime.Breuk;
import dev.uchar.regelspraak.runtime.RegelSpraakRuntime;
import dev.uchar.regelspraak.schema.FactModelGenerator;
import dev.uchar.regelspraak.transpiler.core.Namen;
import dev.uchar.regelspraak.transpiler.tms.ComputeFault;
import dev.uchar.regelspraak.transpiler.tms.EvaluationFaults;
import dev.uchar.regelspraak.transpiler.tms.WritersConflict;

import org.kie.api.KieBase;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.rule.ConsequenceException;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The uchar case runner for the Drools backend: it loads an {@code ir.json} off the wire (via
 * {@link IrLoader} — never the RegelSpraak source), builds the Drools {@link KieBase} through the exec-model
 * branch ({@link DroolsBackend}, the backend's public entry, the same build a consumer makes),
 * and replays every {@code case*.json} against it — a FRESH {@link KieSession} per
 * case (the pom notes Drools' KieBuilder flakiness: the KieBase is built ONCE per model, the session is
 * fresh per case). Expectations are compared with the production comparison discipline of
 * {@code ScenarioRunner.Loop}: {@code BigDecimal.compareTo == 0} (scale-safe), null &hArr; leeg, boolean
 * read, and the §9.5 Inconsistency channel for consistent/inconsistent.
 *
 * <p>A golden is EXCLUDED (not a failure) when the loader refuses a form, the exec transpiler markers a
 * construct (does not lower it), the bean contract is violated, or a case carries an input/expectation
 * form this v0 backend does not carry — each with a reason the differential gate lists. A golden that
 * loads, lowers wholly, and whose cases all match the pinned expectations is PLAYED: because those pinned
 * values are the SAME expectations ref and clingo verify, a played pass IS the differential square.
 */
public final class CaseRunner {

    private static final ObjectMapper JSON = new ObjectMapper();

    /** A golden is not carried by the v0 backend — a reason-carrying exclusion, never a silent skip. Public because
     *  {@link DroolsBackend} refuses with it as well and a consumer outside this package catches it by type; the
     *  constructor stays package-private, since only this backend says what it does not carry. Not final, so that
     *  {@link CaseInput.RoleRefusal} is one and names the role link it refuses; with the constructor package-private,
     *  no class outside this package extends it. */
    public static class Excluded extends RuntimeException {
        private final DroolsCode code;
        private final Reason reason;
        Excluded(Reason reason) {
            super(reason.text());
            this.code = reason.code();
            this.reason = reason;
        }
        /** A sentence that no catalogue holds, for a place that builds an exclusion of its own (the witness tests). */
        Excluded(DroolsCode code, String sentence) {
            super(sentence);
            this.code = code;
            this.reason = null;
        }
        /** The sentence in {@code locale}; the one it was built with where no catalogue holds it. */
        public String message(java.util.Locale locale) { return reason == null ? getMessage() : reason.text(locale); }
        /** The register code of the refusal, rendered at the head of the report beside the sentence. */
        public DroolsCode code() { return code; }
    }

    /** The outcome of one golden: PLAYED (with a per-case pass/fail list) or EXCLUDED (with a reason). */
    public record Result(String status, String reason, DroolsCode code, List<String> caseFailures, int casesPlayed) {
        public static Result excluded(DroolsCode code, String reason) {
            return new Result("EXCLUDE", reason, code, List.of(), 0);
        }
        public static Result played(int n, List<String> failures) {
            return new Result(failures.isEmpty() ? "PLAYED" : "FAIL", "", null, failures, n);
        }
    }

    private final RegelSpraakModel model;
    private final KieBase kieBase;
    private final ClassLoader cl;
    /** §9.8 the dagsoort predicates the DRL-text branch declares as globals (name -> predicate). The exec-model
     *  branch wires those predicates straight into its lambdas and needs none, so it passes an empty map. */
    private final Map<String, java.util.function.Predicate<LocalDate>> dayKindGlobals;
    /** The build this runner fires: a branch puts its derivations on the beans only after a fire
     *  ({@link DroolsBackend#setDerivedValues}), and that needs the analysis that made them. */
    private DroolsBackend.Build backendBuild;
    /** The hash of the document this runner built, which names the files of the seam in {@link #fireToFixpoint}; null
     *  without the property {@value SessionDump#PROPERTY}, and then the seam does nothing. */
    private String documenthash;

    /** The members of the model without a bean field of their own ({@link FactModelGenerator#displacements}): a
     *  case that sets or reads one is refused by name, since the replay would reach the field of the declaration
     *  that took the property. */
    private final List<FactModelGenerator.Displacement> displacements;

    private CaseRunner(RegelSpraakModel model, KieBase kieBase, ClassLoader cl,
                       Map<String, java.util.function.Predicate<LocalDate>> dayKindGlobals) {
        this.model = model; this.kieBase = kieBase; this.cl = cl; this.dayKindGlobals = dayKindGlobals;
        this.displacements = FactModelGenerator.displacements(model);
    }

    /** A runner of {@code build} that replays no case, whose readers of case values, dates, timelines and declared
     *  member names {@link CaseInput} shares with the comparison. */
    static CaseRunner reader(DroolsBackend.Build build) {
        CaseRunner runner = new CaseRunner(build.model(), build.kieBase(), build.beanLoader(), build.dayKindGlobals());
        runner.backendBuild = build;
        return runner;
    }

    /** Refuses a case that sets or reads ({@code action}) the attribute or, with {@code kenmerk}, the
     *  characteristic {@code naam} of an instance of {@code ot} when that member has no bean field of its own. */
    void ownField(Objecttype ot, String naam, boolean characteristic, String action) {
        Reason cause = ot == null ? null : FactModelGenerator.displacedMember(displacements, ot, naam, characteristic);
        if (cause != null) throw new Excluded(cause.framed("case-" + action));
    }

    /** Load {@code irPath}, build the KieBase, and replay each of {@code casePaths}. Any {@link Excluded}
     *  (loader refusal, transpiler marker, contract violation, unsupported form) becomes an EXCLUDE result. */
    public static Result runGolden(Path irPath, List<Path> casePaths) {
        return replay(irPath, casePaths, DroolsBackend.Branch.EXEC_MODEL);
    }

    /** The same replay through the OTHER branch: the analysis rendered as DRL text, which {@link DroolsBackend}
     *  compiles to a {@link KieBase} with the classic DRL build. A GATE-12 PATH since 2026-09-19 (card
     *  gate12-drl-takteller): check-drools.sh sweeps every golden through it (a {@code --drl} run of
     *  {@link DroolsBackendMain}); since card drl-teksttak-op-truth-maintenance, which made the text the second
     *  rendering of the analysis, the gate demands per golden the status line of the exec-model branch. Gate 13 has
     *  driven it since 2026-09-04. */
    public static Result viaDrlText(Path irPath, List<Path> casePaths) {
        return replay(irPath, casePaths, DroolsBackend.Branch.DRL_TEXT);
    }

    /** The exec-model branch under a name the witness tree may spell. Identical to the public gate entry
     *  above it — same replay, same {@link Result} — and it exists for one reason, stated rather than
     *  disguised: the gate entry's NAME carries vocabulary gate 1 (`scripts/check-self-contained.sh`)
     *  forbids across the repository, and that gate allows it in exactly two files, this one among them.
     *  A witness that wants both branches on ONE document therefore names the DRL branch directly and the
     *  exec branch through here, so the assertion lives in the test and the vocabulary stays in this file.
     *  Package-private, like the other three witness seams below: no production caller, and no second
     *  public way in. */
    static Result viaExecModel(Path irPath, List<Path> casePaths) {
        return replay(irPath, casePaths, DroolsBackend.Branch.EXEC_MODEL);
    }

    private static Result replay(Path irPath, List<Path> casePaths, DroolsBackend.Branch branch) {
        try {
            CaseRunner runner = build(irPath, branch);
            List<String> failures = new ArrayList<>();
            int played = 0;
            for (Path cp : casePaths) {
                JsonNode c = JSON.readTree(Files.readString(cp));
                if (c.has("expectedFault")) continue;   // red-path case in a green zone — not this gate's business
                played++;
                String fail = runner.runCase(cp.getFileName().toString(), c);
                if (fail != null) failures.add(fail);
            }
            return Result.played(played, failures);
        } catch (Excluded e) {
            return Result.excluded(e.code(), e.getMessage());
        } catch (IrLoader.Refusal e) {
            return Result.excluded(e.code(), e.reason().framed("loader").text());
        } catch (java.io.IOException e) {
            throw new RuntimeException("could not read " + irPath + ": " + e.getMessage(), e);
        }
    }

    /** The scenario report of a golden's replay: one {@link ScenarioResult} per case, from the same exec-model
     *  replay the gate plays, carrying beside the verdict what the source toolchain's scenario runner reported on top
     *  of a comparison — per expectation the expected and the actual value, the rules that fired with the labels
     *  they bound, and per lowered construct whether it fired. A document or a case form this backend does not
     *  carry (the replay's EXCLUDE, a construct the transpiler does not lower among them) is status MARKER with its
     *  reason, so accepted-but-inert never reads as a pass. A case the replay cannot play, one whose play raises an
     *  exception, is FAIL with that exception as its reason, and the report goes on with the next case. A red-path
     *  case is no scenario and is skipped, as the replay skips it. */
    public static List<ScenarioResult> report(Path irPath, List<Path> casePaths) {
        List<Path> paths = new ArrayList<>();
        List<JsonNode> cases = new ArrayList<>();
        for (Path cp : casePaths) {
            JsonNode c;
            try { c = JSON.readTree(Files.readString(cp)); }
            catch (java.io.IOException e) { throw new RuntimeException("could not read " + cp + ": " + e.getMessage(), e); }
            if (c.has("expectedFault")) continue;
            paths.add(cp);
            cases.add(c);
        }
        List<ScenarioResult> out = new ArrayList<>();
        CaseRunner runner;
        try {
            runner = build(irPath);
        } catch (Excluded e) {
            for (Path p : paths) out.add(marker(p, e.getMessage()));
            return out;
        } catch (IrLoader.Refusal e) {
            for (Path p : paths) out.add(marker(p, e.reason().framed("loader").text()));
            return out;
        } catch (java.io.IOException e) {
            throw new RuntimeException("could not read " + irPath + ": " + e.getMessage(), e);
        }
        for (int i = 0; i < paths.size(); i++) {
            String naam = paths.get(i).getFileName().toString();
            Observation w = new Observation(new DroolsBackend.Observer(runner::beanLabel));
            String failure;
            try {
                failure = runner.runCase(naam, cases.get(i), w);
            } catch (Excluded e) {
                out.add(marker(paths.get(i), e.getMessage()));
                continue;
            } catch (RuntimeException e) {
                // The replay calls this the golden's ERROR and stops; a report of several cases ends only this one,
                // so the cases beside it keep their verdicts.
                out.add(unplayable(paths.get(i), e));
                continue;
            }
            out.add(new ScenarioResult(naam, failure == null ? ScenarioResult.Status.PASS : ScenarioResult.Status.FAIL,
                    List.copyOf(w.expectations), new ScenarioResult.Trace(w.fireObserver.firings()),
                    DroolsBackend.coverage(runner.backendBuild, w.fireObserver.forms()), failure == null ? "" : failure));
        }
        return out;
    }

    private static ScenarioResult marker(Path caseFilePath, String cause) {
        return new ScenarioResult(caseFilePath.getFileName().toString(), ScenarioResult.Status.MARKER, List.of(),
                new ScenarioResult.Trace(List.of()), List.of(), cause);
    }

    /** A case whose play raised {@code e}: FAIL, with the exception in the words of the replay's ERROR line
     *  ({@link DroolsBackendMain}) on one line, and no verdict, firing or coverage, because the case did not finish. */
    private static ScenarioResult unplayable(Path caseFilePath, RuntimeException e) {
        String naam = caseFilePath.getFileName().toString();
        String message = String.valueOf(e.getMessage()).replace('\n', ' ');
        return new ScenarioResult(naam, ScenarioResult.Status.FAIL, List.of(),
                new ScenarioResult.Trace(List.of()), List.of(),
                naam + ": ERROR " + e.getClass().getSimpleName() + ": " + message);
    }

    /** What one case's replay reports beside its verdict: what the session fired, through the backend's entry as a
     *  consumer records it, with the case labels of this runner, and per expectation its verdict. */
    private static final class Observation {
        final DroolsBackend.Observer fireObserver;
        final List<ScenarioResult.ExpectationResult> expectations = new ArrayList<>();

        Observation(DroolsBackend.Observer fireObserver) {
            this.fireObserver = fireObserver;
        }
    }

    /** The witness seam of {@code backends/drools/src/test}: the beans one case's INPUT produces, before any
     *  rule fires. Every gate observes this layer only through post-fire expectation matches, so what an input
     *  BECAME has no other observable surface: the bucket a scalar landed in, the segments a timeline placed on
     *  a {@code List<Segment>} member, or WHICH LEG of a §13.3.9.2 reciprocal pair a role filled — each could be
     *  wrong and still fold to the right total, passing unseen. Roles are therefore set here too, in the replay's
     *  own order (attributes, then roles), so the mirrored self-pair fill is assertable bean-by-bean. Globals
     *  stay out: they live on a {@link KieSession}, which is fire's concern, not this layer's. The beans are those of
     *  the backend's entry ({@link CaseInput}), which every replay reads its case through. */
    static Map<String, Object> inputBeans(Path irPath, JsonNode c) throws java.io.IOException {
        return new LinkedHashMap<>(CaseInput.van(build(irPath).backendBuild, c).beans());
    }

    /** One case's input beans plus every fact the session holds AFTER fire — the §13.4.6 witness seam of
     *  {@code backends/drools/src/test}. A MINTED object carries no case label (ref mints into the reserved
     *  {@code "::"} namespace, which the case loader refuses as a label), so no expectation kind the case
     *  schema can express names it: every corpus observation of a creation is INDIRECT, through a fold or a
     *  navigated read on a labelled input. Neither other seam reaches it either — {@link #inputBeans} stops
     *  before any rule fires, {@link #playCase} returns only the comparator's verdict. So THAT the mint
     *  exists, WHICH members its toekenningen filled, and that BOTH ends of its back-link were written have
     *  no other observable surface, while each could be wrong and still leave the pinned total right (a mint
     *  reachable one way only; a member written on the subject instead of the mint). */
    record AfterFire(Map<String, Object> input, List<Object> sessionFacts) {}

    static AfterFire afterFire(Path irPath, JsonNode c) throws java.io.IOException {
        CaseRunner runner = build(irPath);
        runner.evaluationDate = asOfDate(c);
        ClassLoader previous = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(runner.cl);
        KieSession session = runner.kieBase.newKieSession();
        try {
            CaseInput input = runner.readInput(c);
            runner.setGlobals(session, c);
            DroolsBackend.insert(runner.backendBuild, session, input.inputData());
            String noFixpoint = runner.fireToFixpoint(session);
            // The sister runCase path reports the cap as a case failure; this seam has no verdict to return, so
            // a run that never reached a fixpoint must not hand back a half-fired fact base as if it had settled.
            if (noFixpoint != null) throw new IllegalStateException(noFixpoint);
            return new AfterFire(new LinkedHashMap<>(runner.labelToBean), new ArrayList<>(session.getObjects()));
        } finally {
            session.dispose();
            Thread.currentThread().setContextClassLoader(previous);
        }
    }

    /** The comparator seam of {@code backends/drools/src/test}: one authored in-memory case through the FULL
     *  replay (build, inputs, fire, compare), returning the per-case failure line — null on a match. The
     *  read-back comparator has no other directly observable surface: gate 12 pins post-fire values on corpus
     *  cases whose pinned lists happen to match segment-for-segment, so a comparator that ignores waarde or
     *  order survives the whole gate set (lens 1 proved it by mutation) — its verdicts are asserted here. */
    static String playCase(Path irPath, JsonNode c) throws java.io.IOException {
        return build(irPath).runCase("case", c);
    }

    /** The same comparator seam through EITHER transpiler branch, so one authored document can be replayed by
     *  both and their verdicts compared row by row. It exists for the same reason {@link #viaExecModel} does:
     *  a witness that wants both branches on ONE in-memory case would otherwise have to write the document to
     *  disk twice and read two {@link Result}s, which reports a whole golden's status where the row wants a
     *  single cell's verdict. Package-private, no production caller. */
    static String playCase(Path irPath, JsonNode c, boolean viaDrl) throws java.io.IOException {
        return build(irPath, viaDrl ? DroolsBackend.Branch.DRL_TEXT : DroolsBackend.Branch.EXEC_MODEL).runCase("case", c);
    }

    /** The same comparator seam with the branch named by its value. Package-private, no production caller. */
    static String playCase(Path irPath, JsonNode c, DroolsBackend.Branch branch) throws java.io.IOException {
        return build(irPath, branch).runCase("case", c);
    }

    /** The measuring seam of the harnesses and witnesses in the test tree, first made for the trial of card
     *  drools-logische-afleiding (acceptance (e)): a runner of {@code irPath} on {@code branch} whose KieBase is built
     *  once, so that a harness in the test tree can fire one case many times and time the fire alone. The replay
     *  entries cannot serve: they build per call and hand back a verdict, not the firings or the session.
     *  Package-private, no production caller, and no gate runs it. */
    static CaseRunner forMeasurement(Path irPath, DroolsBackend.Branch branch) throws java.io.IOException {
        return build(irPath, branch);
    }

    /** What a measuring harness does with one session that {@link #withSession} set up: fire it and measure it as it
     *  chooses. {@code labels} maps each case label to its input bean. */
    @FunctionalInterface
    interface SessionMeasurement<T> {
        T meet(DroolsBackend.Build backendBuild, KieSession session, Map<String, Object> labels);
    }

    /** Sets up case {@code c} as {@link #runCase} does, in a fresh session with the bean loader as the thread's context
     *  class loader, and hands that session, not yet fired, to {@code measurement}; the session is disposed afterwards. The
     *  seam of the measuring harnesses of card tms-omschakeling, the scale test (point 9) and the measurement over the
     *  corpus (point 7), which fire through {@link DroolsBackend#fire} as a consumer does and time the fire and the
     *  write-back apart, where the replay's own fire times neither. Package-private, no production caller, and no gate
     *  runs it. */
    <T> T withSession(JsonNode c, SessionMeasurement<T> measurement) {
        labelToBean.clear();
        labelType.clear();
        evaluationDate = asOfDate(c);
        ClassLoader previous = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(cl);
        KieSession session = kieBase.newKieSession();
        try {
            CaseInput input = readInput(c);
            setGlobals(session, c);
            DroolsBackend.insert(backendBuild, session, input.inputData());
            return measurement.meet(backendBuild, session, java.util.Collections.unmodifiableMap(labelToBean));
        } finally {
            session.dispose();
            Thread.currentThread().setContextClassLoader(previous);
        }
    }

    /** Build {@code irPath} on the exec-model branch. Throws {@link Excluded} / {@link IrLoader.Refusal} for any
     *  form the v0 backend does not carry. */
    private static CaseRunner build(Path irPath) throws java.io.IOException {
        return build(irPath, DroolsBackend.Branch.EXEC_MODEL);
    }

    /** As above, with {@code branch} choosing WHICH rendering of the analysis produces the KieBase: the executable
     *  model, or the DRL text. The whole prelude — load,
     *  refuse, beans, contract, lowering with every side table, coverage check, KieBase — is
     *  {@link DroolsBackend#build(Path, DroolsBackend.Branch)}, the backend's public entry, so the green replay,
     *  the red probe and every witness seam build exactly as a consumer of the backend does. */
    private static CaseRunner build(Path irPath, DroolsBackend.Branch branch) throws java.io.IOException {
        DroolsBackend.Build b = DroolsBackend.build(irPath, branch);
        CaseRunner runner = new CaseRunner(b.model(), b.kieBase(), b.beanLoader(), b.dayKindGlobals());
        runner.backendBuild = b;
        if (SessionDump.SEAM_DIRECTORY != null) runner.documenthash = SessionDump.hash(irPath);
        return runner;
    }

    // ---------------- the red path (corpus/red, gate 8's Drools leg) ----------------

    /** What the v0 Drools backend OBSERVES when it replays one {@code expectedFault} case, by exception TYPE: FAULT
     *  (any runtime exception after the build other than an {@link Excluded}, at fire or while the case is read, a
     *  crash included, with its full cause chain as the text a pinned fragment is matched against; and a run that
     *  hits the fire cap without a fixpoint, which raises nothing), EXCLUDE (a form the backend does not carry — a
     *  loader refusal, or an {@link Excluded} from the build or from the case reader — with the reason), or NOFAULT
     *  (the case ran to a model — the backend does not enforce this red-path constraint, so its form is not
     *  Drools-carried). {@code code} is the EF code of a FAULT's kind (RUNTIME-SEMANTICS ch. 5.7b), several
     *  comma-separated in the order of the report, beside the text and never inside it; null where the fault names no
     *  kind and for every other status. {@code params} holds one map per code of {@code code}, in the same order: the
     *  parameters of that kind (RUNTIME-SEMANTICS ch. 5.7b) this backend has structurally, beside the text and never
     *  inside it; empty where there is no code. */
    public record RedResult(String status, String detail, String code, DroolsCode droolsCode,
                            List<Map<String, String>> params) {
        public RedResult(String status, String detail, String code, DroolsCode droolsCode) {
            this(status, detail, code, droolsCode, List.of());
        }
    }

    /** Replay a single {@code corpus/red} case and report what the backend observes. The split is by exception
     *  TYPE, not by phase. A loader refusal, or an {@link Excluded} from the build or from a case-input form, is an
     *  EXCLUDE. An I/O failure, and any other exception while the IR is loaded and the KieBase built, leaves this
     *  method and the CLI reports it as ERROR. Every other runtime exception after the build is a FAULT: a fault
     *  AT fire (a division by zero, a fixpoint failure), a named refusal while the case is read (a date lexeme
     *  that names no day, corpus/red/as-of-date-calendar), and equally a CRASH while the case is read — measured
     *  2026-09-23: meldwaarde written as a boolean gives "IllegalArgumentException: argument type mismatch", an
     *  undeclared characteristic a failed setter call. A run without a fixpoint is a FAULT too, and raises no
     *  exception: on the exec-model branch, which this path drives, it says what {@link DroolsBackend#fire} says,
     *  "creation budget exhausted …" at the creation budget or "geen fixpunt: het vuurplafond van … vuringen is
     *  bereikt, …" at the fire cap, and two writers of one attribute that disagree are a fault at the fixpoint, which
     *  the write-back throws. Gate 8 therefore pins only
     *  this backend's own sentence, never platform text, since a pin on a JDK message would pin a crash; for a
     *  refusal while the case is read, that sentence names position and lexeme. The same red path on either branch is
     *  {@link #runRed(Path, Path, DroolsBackend.Branch)}. */
    public static RedResult runRed(Path irPath, Path casePath) {
        return runRed(irPath, casePath, DroolsBackend.Branch.EXEC_MODEL);
    }

    /** {@link #runRed(Path, Path)} on the branch {@code branch}: the red path of either rendering of the analysis, so that
     *  a witness can demand the same {@link RedResult} of both branches on every red case (card
     *  drl-teksttak-op-truth-maintenance, step 6b). Gate 8's Drools leg ({@code DroolsRedMain}) drives the exec-model
     *  branch through the entry above. */
    public static RedResult runRed(Path irPath, Path casePath, DroolsBackend.Branch branch) {
        CaseRunner runner;
        try {
            runner = build(irPath, branch);
        } catch (Excluded e) {
            return new RedResult("EXCLUDE", e.getMessage(), null, e.code());
        } catch (IrLoader.Refusal e) {
            return new RedResult("EXCLUDE", e.reason().framed("loader").text(), null, e.code());
        } catch (java.io.IOException e) {
            throw new RuntimeException("could not read " + irPath + ": " + e.getMessage(), e);
        }
        try {
            JsonNode c = JSON.readTree(Files.readString(casePath));
            DroolsBackend.Firing firing = runner.fireRed(c);
            return firing.noFixpoint() == null
                    ? new RedResult("NOFAULT", "ran to a model without faulting", null, null)
                    : new RedResult("FAULT", firing.noFixpoint(), firing.code(), null,
                            firing.code() == null ? List.of() : List.of(firing.params()));
        } catch (Excluded e) {
            return new RedResult("EXCLUDE", e.getMessage(), null, e.code());
        } catch (java.io.IOException e) {
            throw new RuntimeException("could not read " + casePath + ": " + e.getMessage(), e);
        } catch (RuntimeException e) {
            // a fault raised while loading inputs or firing rules — a division by zero surfaces here, wrapped by
            // Drools; the whole cause chain is the report so the pinned fragment can match the deepest message.
            List<FaultKind> kinds = faultKinds(e);
            return new RedResult("FAULT", faultText(e),
                    kinds.isEmpty() ? null : String.join(", ", kinds.stream().map(FaultKind::code).toList()), null,
                    kinds.stream().map(FaultKind::params).toList());
        }
    }

    /** Fire one red-path case: load its world, fire the rules, and return what the fire did: its
     *  {@link DroolsBackend.Firing#noFixpoint} is the fault text where the run does not reach a fixpoint, with its code,
     *  and null when the run completes cleanly (a NOFAULT — the backend produced a model where ref/clingo reject). A
     *  fault raised at fire propagates as an exception the caller renders. */
    private DroolsBackend.Firing fireRed(JsonNode c) {
        labelToBean.clear();
        labelType.clear();
        evaluationDate = asOfDate(c);
        ClassLoader previous = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(cl);
        KieSession session = kieBase.newKieSession();
        try {
            CaseInput input = readInput(c);
            setGlobals(session, c);
            DroolsBackend.insert(backendBuild, session, input.inputData());
            return fireToFixpointFiring(session, null);
        } finally {
            session.dispose();
            Thread.currentThread().setContextClassLoader(previous);
        }
    }

    /** A fault kind (RUNTIME-SEMANTICS ch. 5.7b) of a red-path fault with its parameters. */
    record FaultKind(String code, Map<String, String> params) {}

    /** The fault kinds of the red-path fault {@code t}, in the order of its report, each code once; empty where it names
     *  no kind. Classified here, at the catch, by exception type: the backend's own fault exceptions name theirs
     *  ({@code code()}, {@code params()}); several faults at the fixpoint ({@link EvaluationFaults}) give the kind of
     *  each. In a cause chain the outermost exception that names a kind decides: a fault named after its rule wraps the
     *  same fault unnamed. The production runtime's {@link Breuk.DelingDoorNul} names no kind: a division by a numeric
     *  zero is the §9.5 deposit EF-005 since v1 (RUNTIME-SEMANTICS ch. 1.4 point 4), which this branch makes before the
     *  runtime is called ({@code ComputeHelpers.divided}), so the runtime's fault no longer reaches a fault rule from
     *  a division of the IR. */
    static List<FaultKind> faultKinds(Throwable t) {
        List<FaultKind> kinds = new ArrayList<>();
        addKind(kinds, t);
        return kinds;
    }

    private static void addKind(List<FaultKind> kinds, Throwable t) {
        for (Throwable c = t; c != null; c = c.getCause() == c ? null : c.getCause()) {
            if (c instanceof EvaluationFaults several) {
                for (RuntimeException f : several.faults()) addKind(kinds, f);
                return;
            }
            FaultKind kind = null;
            if (c instanceof ComputeFault f && f.code() != null) kind = new FaultKind(f.code(), f.params());
            else if (c instanceof WritersConflict w) kind = new FaultKind(w.code(), w.params());
            else if (c instanceof CaseDateFault d) kind = new FaultKind(d.code(), d.params());
            if (kind != null) {
                String code = kind.code();
                if (kinds.stream().noneMatch(k -> k.code().equals(code))) kinds.add(kind);
                return;
            }
        }
    }

    /** The full cause chain of a fault as one line — {@code SimpleName: message <- SimpleName: message …} — so the
     *  pinned fragment can match the deepest wrapped message (a Drools rule fault wraps the runtime's exception). */
    private static String faultText(Throwable t) {
        StringBuilder sb = new StringBuilder();
        Throwable c = t;
        while (c != null) {
            if (sb.length() > 0) sb.append(" <- ");
            sb.append(c.getClass().getSimpleName());
            if (c.getMessage() != null) sb.append(": ").append(c.getMessage());
            Throwable next = c.getCause();
            if (next == c) break;   // guard a self-referential cause chain
            c = next;
        }
        return sb.toString();
    }

    // ---------------- one case ----------------

    /** The input beans of the case by label, and their object types, as {@link CaseInput} read them: the labels of the
     *  report, the seam and the write-back. */
    private final Map<String, Object> labelToBean = new LinkedHashMap<>();
    private final Map<String, Objecttype> labelType = new LinkedHashMap<>();
    /** The case's evaluation date, which {@link #setGlobals} sets as the global {@code rekendatum}. */
    private LocalDate evaluationDate;

    private String runCase(String naam, JsonNode c) {
        return runCase(naam, c, null);
    }

    /** One case: its verdict line, null on a match. The expectations are compared through the backend's entry
     *  ({@link DroolsBackend#compare}), with the case labels of this runner, as a consumer compares them. With a
     *  {@link Observation} the replay also reports the firings and every expectation's verdict ({@link #report}); the
     *  verdict itself does not depend on it. */
    private String runCase(String naam, JsonNode c, Observation w) {
        labelToBean.clear();
        labelType.clear();
        evaluationDate = asOfDate(c);
        ClassLoader previous = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(cl);
        KieSession session = kieBase.newKieSession();
        try {
            CaseInput input = readInput(c);
            setGlobals(session, c);
            DroolsBackend.insert(backendBuild, session, input.inputData());
            String noFixpoint = fireToFixpoint(session, w == null ? null : w.fireObserver);
            if (noFixpoint != null) return naam + ": " + noFixpoint;
            DroolsBackend.Comparison comparison =
                    DroolsBackend.compare(backendBuild, session, this::beanLabel, c.get("expected"));
            if (w != null) w.expectations.addAll(comparison.expectations());
            return comparison.pass() ? null : naam + ": " + comparison.diagnostic();
        } finally {
            session.dispose();
            Thread.currentThread().setContextClassLoader(previous);
        }
    }

    /** Fires {@code session} through the backend's entry ({@link DroolsBackend#fire}), under the cap a consumer gets, and
     *  returns null at a fixpoint. A run without one returns the sentence of {@code fire}, which names the cap and what
     *  it was computed from, or that of the creation budget. A consequence that throws leaves the fire as
     *  {@link DroolsBackend#designate} names it; every replay, red path and report of this class fires here, on either
     *  branch. At a fixpoint the derivations go onto the beans before anything reads them, or the evaluation faults
     *  that stand there are thrown ({@link DroolsBackend#setDerivedValues}); without a fixpoint nothing is written
     *  back, since the replay reports the sentence and reads no bean. With the property
     *  {@value SessionDump#PROPERTY} a seam writes the dump of the session ({@link SessionDump}) right after the fire,
     *  before the write-back, also where the fire throws, on either branch. */
    private String fireToFixpoint(KieSession session) {
        return fireToFixpoint(session, null);
    }

    /** As above, with {@code observer} recording what the session fires ({@link DroolsBackend.Observer}); null
     *  records nothing. */
    private String fireToFixpoint(KieSession session, DroolsBackend.Observer observer) {
        return fireToFixpointFiring(session, observer).noFixpoint();
    }

    /** As {@link #fireToFixpoint(KieSession, DroolsBackend.Observer)}, returning the {@link DroolsBackend.Firing} itself,
     *  so that the red path reads the code of a run without a fixpoint beside its sentence. */
    private DroolsBackend.Firing fireToFixpointFiring(KieSession session, DroolsBackend.Observer observer) {
        SessionDump trace = documenthash == null ? null : SessionDump.op(session, labelToBean);
        DroolsBackend.Firing firing;
        try {
            firing = observer == null ? DroolsBackend.fire(backendBuild, session) : DroolsBackend.fire(backendBuild, session, observer);
        } catch (RuntimeException e) {
            if (trace != null) trace.seam(documenthash, backendBuild, e, labelToBean);
            if (e instanceof ConsequenceException c) throw DroolsBackend.designate(c);
            throw e;
        }
        if (trace != null) trace.seam(documenthash, backendBuild, firing, labelToBean);
        if (firing.noFixpoint() == null) DroolsBackend.setDerivedValues(backendBuild, session, this::beanLabel);
        return firing;
    }

    /** Reads case {@code c} through the backend's entry ({@link CaseInput}), as a consumer reads it, and keeps its
     *  labels for this runner's report, seam and write-back. */
    private CaseInput readInput(JsonNode c) {
        CaseInput input = CaseInput.van(backendBuild, c);
        labelToBean.putAll(input.beans());
        labelType.putAll(input.types());
        return input;
    }

    /** Whether {@code attr} on {@code ot} is a §13.3.6 temporal bean member — a {@code List<Segment>} field,
     *  the shape {@link FactModelGenerator} derives from a declared attribuutGranulariteit. */
    boolean temporal(Objecttype ot, String attr) {
        return Namen.containsName(ot.granularityMembers(), attr);
    }

    /** Whether {@code attr} on {@code ot} is a §3.6 dimensioned bean member — a {@code Map<String, BigDecimal>}
     *  field. Checked BEFORE {@link #temporal}: the generator gives a dimensioned declaration the Map shape
     *  even when a granularity is declared beside it. */
    boolean dimensioned(Objecttype ot, String attr) {
        return !ot.dimensionsOf(attr).isEmpty();
    }

    /** The declared name of the attribute a case writes as {@code naam} on {@code ot} or one of its ancestors, under the
     *  name equality of the reference interpreter ({@link Namen#declaredName}); {@code naam} where {@code ot} is null
     *  or declares none. The generator derives a member's getter and setter from its declared name, and a case may
     *  write the name in another capitalisation, "vondstNummer" for a declared "vondstnummer", which the reference
     *  interpreter reads as the same member: a method derived from the written name would be one the bean lacks. */
    static String attributeName(Objecttype ot, String naam) {
        return ot == null ? naam : Namen.declaredName(ot.attributeMembers().keySet(), naam);
    }

    /** {@link #attributeName} for a characteristic. */
    static String characteristicName(Objecttype ot, String naam) {
        if (ot == null) return naam;
        List<String> declared = new ArrayList<>();
        for (Objecttype t : ot.chain()) declared.addAll(t.kenmerken);
        return Namen.declaredName(declared, naam);
    }

    private void setGlobals(KieSession session, JsonNode c) {
        JsonNode params = c.get("input").get("parameters");
        if (params != null) for (Map.Entry<String, JsonNode> e : params.properties()) {
            if (e.getValue().has("timeline"))
                throw new Excluded(DroolsCode.GRANULAR_PARAMETER_TIMELINE.reason("parameter", e.getKey()));
            setGlobalSafe(session, Namen.ident(e.getKey(), false), valueToArg(e.getValue(), e.getKey()));
        }
        if (evaluationDate != null) setGlobalSafe(session, "rekendatum", evaluationDate);
        // §9.8 the DRL-text branch reaches its dagsoort predicates through globals (the exec branch closes over
        // them); empty for the exec branch.
        for (Map.Entry<String, java.util.function.Predicate<LocalDate>> e : dayKindGlobals.entrySet())
            setGlobalSafe(session, e.getKey(), e.getValue());
    }

    /** The case's {@code asOfDate}, or null when it carries none. */
    static LocalDate asOfDate(JsonNode c) {
        JsonNode asOf = c.get("asOfDate");
        return asOf == null || asOf.isNull() ? null : caseWireDate(asOf.asText(), "asOfDate");
    }

    private static void setGlobalSafe(KieSession session, String naam, Object value) {
        try { session.setGlobal(naam, value); }
        catch (RuntimeException unknown) { /* global not declared: no rule reads it */ }
    }

    // ---------------- §13.3.6 temporal input (the List<Segment> bean members) ----------------

    /** The segments of a timeline value, with ref's load-time validation mirrored: at least one segment (an
     *  EXPECTATION may be empty — {@code emptyAllowed}), {@code from} before {@code to}, both boundaries on the
     *  declared granularity grid, and the list ordered and non-overlapping as written. A malformed timeline is a
     *  hard failure, never a silent acceptance: the case schema (gate 4) and ref (gate 5) reject it first, so
     *  reaching it here means the corpus and this reader disagree. A grid that carries no case timeline is a
     *  not-carried form, refused where ref's loader refuses it and in its sentence, at position {@code site}
     *  ({@code J1.m2} for an input, {@code expected J1.m2} for an expectation): a grid of several units and a
     *  variable start before any segment is read, the calendar's day, week and quarter grid at the first segment. */
    List<Object> timelineSegments(JsonNode segs, String field, String site, Grid gran, boolean emptyAllowed) {
        if (segs == null || !segs.isArray())
            throw new IllegalArgumentException("misvormde tijdlijnwaarde op '" + field + "'");
        if (segs.isEmpty() && !emptyAllowed)
            throw new IllegalArgumentException("tijdlijnwaarde op '" + field + "' draagt geen enkel segment");
        if (gran.count() != 1)
            throw new Excluded(DroolsCode.TIMELINE_MULTI_PERIOD_GRID.reason("site", site, "count", gran.count(), "granularity", spelling(gran.gran())));
        if (gran.variable())
            throw new Excluded(DroolsCode.TIMELINE_VARIABLE_START_GRID.reason("site", site));
        List<Object> out = new ArrayList<>();
        LocalDate previousTo = null;
        for (JsonNode sj : segs) {
            LocalDate van = boundDate(sj, "from", field), to = boundDate(sj, "to", field);
            if (!van.isBefore(to))
                throw new IllegalArgumentException("segment 'to' op '" + field + "' ligt niet na 'from'");
            if (gran.fixed() == null) calendarGridForInput(gran.gran(), site);
            if (!bucket(gran, van)[0].equals(van) || !bucket(gran, to)[0].equals(to))
                throw new IllegalArgumentException("segmentgrens op '" + field + "' ligt niet op het "
                        + gran.gran() + "-raster" + (gran.fixed() == null ? "" : " vanaf " + gran.fixed()));
            if (previousTo != null && previousTo.isAfter(van))
                throw new IllegalArgumentException("tijdlijnsegmenten op '" + field + "' zijn ongeordend of overlappen");
            previousTo = to;
            out.add(segment(van, to, segmentValue(sj.get("value"), field)));
        }
        return out;
    }

    /** The grid a temporal member's case values sit on: its facet's granularity and count, its fixed start point,
     *  and whether it rides a timeline with a variable start (RUNTIME-SEMANTICS ch. 3.2). */
    record Grid(Granulariteit gran, int count, String fixed, boolean variable) {}

    Grid grid(Objecttype ot, String attr) {
        Integer count = Namen.byName(ot.timelineCountMembers(), attr);
        return new Grid(Namen.byName(ot.granularityMembers(), attr), count == null ? 1 : count,
                ot.fixedStartPointOf(attr), model.hasVariableStart(ot, attr));
    }

    /** The period {@code [van, tot)} of the member's grid that contains {@code d} (ch. 3.2), as ref lays it: the
     *  period a fixed start point lays, every wall computed from it; otherwise the calendar bucket — year buckets run
     *  1 Jan → 1 Jan, month buckets first-of-month → first-of-next. A grid that lays no such period is a loud
     *  not-carried, never a guessed grid, in the sentence of ref's grid function, which the ASP backend speaks as
     *  well: a grid of several units carries no operation, a variable start is laid per object from an anchor the
     *  case does not give, and the calendar lays no day, week or quarter bucket. For a case timeline
     *  {@link #timelineSegments} refuses the same grids first, in the sentence of ref's case loader. */
    static LocalDate[] bucket(Grid r, LocalDate d) {
        if (r.count() != 1)
            throw new Excluded(DroolsCode.BUCKET_MULTI_PERIOD_GRID.reason("count", r.count(), "granularity", spelling(r.gran())));
        if (r.variable())
            throw new Excluded(DroolsCode.BUCKET_VARIABLE_START_GRID.reason());
        if (r.fixed() != null) return FixedStartPoint.periode(r.gran(), r.fixed(), d);
        switch (r.gran()) {
            case JAAR:
                return new LocalDate[]{ LocalDate.of(d.getYear(), 1, 1), LocalDate.of(d.getYear() + 1, 1, 1) };
            case MAAND:
                LocalDate van = d.withDayOfMonth(1);
                return new LocalDate[]{ van, van.plusMonths(1) };
            default:
                throw new Excluded(DroolsCode.BUCKET_GRANULARITY_UNSUPPORTED.reason());
        }
    }

    /** ref's loader checks a case timeline on the calendar grid through the calendar bucketing, which has month and
     *  year buckets only (ch. 3.2), and refuses the other three granularities as a v0 limit, each in its own
     *  sentence; this is that refusal, at position {@code site}. */
    private static void calendarGridForInput(Granulariteit gran, String site) {
        switch (gran) {
            case JAAR, MAAND -> { }
            case DAG -> throw new Excluded(DroolsCode.DAY_GRID_CALENDAR_BUCKETING.reason("site", site));
            default -> throw new Excluded(DroolsCode.CALENDAR_GRID_UNSUPPORTED.reason("site", site, "granularity", spelling(gran), "granularity", spelling(gran)));
        }
    }

    /** A granularity in its wire spelling (IR-SPEC ch. 2.2), the word ref's sentences carry. */
    private static String spelling(Granulariteit gran) {
        return Spelling.GRANULARITY.spelling(gran);
    }

    /** The numeric value of one segment. The generated {@code Segment} holds a {@code BigDecimal} waarde, so a
     *  non-numeric timeline value has no bean representation — a reason-carrying not-carried, never a coercion. */
    BigDecimal segmentValue(JsonNode w, String field) {
        Object v = valueToArg(w, field);
        if (v == null || v instanceof BigDecimal) return (BigDecimal) v;
        throw new Excluded(DroolsCode.TIMELINE_VALUE_NON_NUMERIC.reason("attribute", field));
    }

    static LocalDate boundDate(JsonNode seg, String field, String where) {
        JsonNode d = seg == null ? null : seg.get(field);
        if (d == null || !d.isTextual())
            throw new IllegalArgumentException("segment op '" + where + "' mist een '" + field + "'-datum");
        // the field name carries BOTH coordinates: a case may hold many timelines and each holds two
        // boundaries, so "onderhoudstarief.from" is the only form a producer can act on
        return caseWireDate(d.asText(), where + "." + field);
    }

    /** One {@code Segment} fact of the generated bean model, built through the runtime's own reflective
     *  constructor — the Segment class is a runtime objecttype, unknown at compile time. */
    Object segment(LocalDate van, LocalDate to, BigDecimal value) {
        try {
            return RegelSpraakRuntime.newSegment(
                    Class.forName(FactModelGenerator.PACKAGE + ".Segment", true, cl), van, to, value);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("de gegenereerde Segment-factklasse ontbreekt", e);
        }
    }

    // ---------------- comparison ----------------

    /** The comparison behind {@link DroolsBackend#compare}, on a runner of {@code build} that knows the beans of
     *  {@code session} by the labels of the caller: {@code perLabel} gives the bean of each label, {@code types} its object
     *  type. The runner reads no case input, so a session the caller filled itself compares as one of the replay. */
    static DroolsBackend.Comparison comparison(DroolsBackend.Build build, KieSession session,
                                                   Map<String, Object> perLabel, Map<String, Objecttype> types,
                                                   JsonNode expectations) {
        CaseRunner r = new CaseRunner(build.model(), build.kieBase(), build.beanLoader(), build.dayKindGlobals());
        r.labelToBean.putAll(perLabel);
        r.labelType.putAll(types);
        List<ScenarioResult.ExpectationResult> out = new ArrayList<>();
        List<String> mis = new ArrayList<>();
        int position = 0;
        for (JsonNode v : expectations) {
            String m = r.compare(v, session);
            if (m != null) mis.add(m);
            position++;
            out.add(new ScenarioResult.ExpectationResult(source(v), r.expected(v), r.actual(v, session), m == null,
                    position));
        }
        return new DroolsBackend.Comparison(out, String.join("; ", mis));
    }

    /** The object type of the bean that carries {@code label}, for expectation {@code v} on one of its members. A label
     *  no bean carries is refused: there is nothing to read, and the "leeg" or "afwezig" the replay read there until
     *  {@link DroolsBackend#compare} let attributeEmpty and characteristicAbsent hold on a label that names nothing, as
     *  when a caller gives the case labels to a session it labelled with the ids of its request. ref refuses such a case
     *  at load ("expectation references unknown label"). */
    private Objecttype typeOfLabel(JsonNode v, String label) {
        Objecttype ot = labelType.get(label);
        if (ot == null)
            throw new IllegalArgumentException(v.get("kind").asText() + "-verwachting op onbekend label '" + label
                    + "': geen bean van de sessie draagt het, dus er is niets te lezen");
        return ot;
    }

    private String compare(JsonNode v, KieSession session) {
        String kind = v.get("kind").asText();
        return switch (kind) {
            case "attributeValue" -> attribute(v, false);
            case "attributeEmpty" -> attribute(v, true);
            case "characteristicPresent" -> characteristic(v, true);
            case "characteristicAbsent" -> characteristic(v, false);
            case "consistent" -> consistency(session, false, v);
            case "inconsistent" -> consistency(session, true, v);
            case "attributeTimeline" -> timeline(v);
            default -> throw new Excluded(DroolsCode.EXPECTATION_KIND_UNKNOWN.reason("kind", kind));
        };
    }

    private String attribute(JsonNode v, boolean emptyExpected) {
        String attr = v.get("attribute").asText();
        String label = v.get("label").asText();
        Objecttype ot = typeOfLabel(v, label);
        ownField(ot, attr, false, "reads");
        if (dimensioned(ot, attr))
            throw new Excluded(DroolsCode.DIMENSIONED_ATTRIBUTE_READBACK.reason("attribute", attr));
        // a temporal attribute's bean member is the List<Segment> timeline itself: the scalar comparison below
        // would compare a list against a scalar. Its read-back form is the attributeTimeline expectation.
        if (temporal(ot, attr))
            throw new Excluded(DroolsCode.SCALAR_READBACK_TEMPORAL_ATTRIBUTE.reason("attribute", attr));
        Object actual = invokeGetter(labelToBean.get(label), Namen.getter(attributeName(ot, attr)));
        if (emptyExpected)
            return actual == null ? null : "verwacht leeg, werkelijk " + actual;
        Object expected = valueToArg(v.get("value"), attr);
        return equalValue(v.get("value"), expected, actual) ? null
                : "attribuut " + attr + ": verwacht " + render(expected) + ", werkelijk " + render(actual);
    }

    /** The §13.3.6 timeline read-back: the doel's {@code List<Segment>} against the pinned segment list, with
     *  ref's comparison discipline — equal length, van/tot EXACT, waarde scale-safe ({@code compareTo == 0}),
     *  compared index-for-index. Both sides arrive ordered (the expectation through the validator, the bean
     *  list through the runtime's ordered writes), so an order-blind comparator has no observable witness —
     *  the claim stops at what is falsifiable. A never-written target (a null field) matches an explicitly
     *  empty expectation: both read leeg on every day (ch. 3.1), exactly as ref and clingo pin it. */
    private String timeline(JsonNode v) {
        String attr = v.get("attribute").asText(), label = v.get("label").asText();
        Objecttype ot = labelType.get(label);
        // both are CASE defects, not uncarried forms — ref rejects the same shapes at load ("expected
        // timeline on %S, which declares no granularity"), so they are hard failures here too, and the
        // unknown-label arm would otherwise reach bucket() with a null granularity (an unnamed NPE).
        if (ot == null)
            throw new IllegalArgumentException("attributeTimeline-verwachting op onbekend label '" + label + "'");
        if (!temporal(ot, attr))
            throw new IllegalArgumentException("attributeTimeline-verwachting op niet-temporeel attribuut '" + attr + "'");
        ownField(ot, attr, false, "reads");
        List<Object> expected = timelineSegments(v.get("timeline"), attr, "expected " + label + "." + attr, grid(ot, attr),
                true);
        Object read = labelToBean.containsKey(label)
                ? invokeGetter(labelToBean.get(label), Namen.getter(attributeName(ot, attr))) : null;
        if (read == null)
            return expected.isEmpty() ? null
                    : "attribuut " + attr + ": draagt geen tijdreeks, verwacht " + renderTl(expected);
        List<?> actual = (List<?>) read;
        boolean equal = actual.size() == expected.size();
        for (int k = 0; equal && k < expected.size(); k++)
            equal = segEqual(expected.get(k), actual.get(k));
        return equal ? null
                : "attribuut " + attr + ": verwacht " + renderTl(expected) + ", werkelijk " + renderTl(actual);
    }

    private static boolean segEqual(Object expectation, Object actual) {
        return java.util.Objects.equals(segField(expectation, "van"), segField(actual, "van"))
                && java.util.Objects.equals(segField(expectation, "tot"), segField(actual, "tot"))
                && equal(segField(expectation, "waarde"), segField(actual, "waarde"));
    }

    /** One field of a {@code Segment} fact, read through the runtime's property bridge (the class is a runtime
     *  objecttype — no compile-time type to cast to). */
    private static Object segField(Object seg, String field) { return RegelSpraakRuntime.get(seg, field); }

    private static String renderTl(List<?> segs) {
        if (segs.isEmpty()) return "(leeg)";
        List<String> out = new ArrayList<>();
        for (Object s : segs)
            out.add("[" + segField(s, "van") + "," + segField(s, "tot") + ")=" + render(segField(s, "waarde")));
        return String.join(" ", out);
    }

    private String characteristic(JsonNode v, boolean presentExpected) {
        Objecttype ot = typeOfLabel(v, v.get("label").asText());
        ownField(ot, v.get("characteristic").asText(), true, "reads");
        Object read = invokeGetter(labelToBean.get(v.get("label").asText()),
                Namen.getter(characteristicName(ot, v.get("characteristic").asText())));
        boolean present = Boolean.TRUE.equals(read);
        return present == presentExpected ? null
                : "kenmerk " + v.get("characteristic").asText() + ": verwacht "
                + (presentExpected ? "aanwezig" : "afwezig") + ", werkelijk " + (present ? "aanwezig" : "afwezig");
    }

    /** The §9.5 consistency check. A {@code consistent} expectation demands NO Inconsistency fact at all
     *  (unchanged presence semantics). An {@code inconsistent} expectation is verified against the pinned §9.5
     *  KEY (the {@code rule}) — a matching Inconsistency fact must exist — and, when the expectation pins a
     *  {@code label}, on that SUBJECT, for every key kind, as ref matches it ({@code ref/lib/case.ml}, the
     *  {@code Inconsistent} arm: rule and exact subject label on the same fact). Until card drools-consistentie-label
     *  (2026-09-23) the subject was checked only for a default-group conflict key and a rule-name key passed on any
     *  fact from its rule, wherever it landed. That masked one real divergence: both transpilers laid the §9.5
     *  allocation error on the source instead of each recipient ({@code jutterswet-rangmaximering}), fixed in the same
     *  card. An expectation carrying no rule and no code falls back to bare presence. A pinned {@code code} (card
     *  channel-fact-codes, RUNTIME-SEMANTICS ch. 5.6, 5.7b) must be among the codes of a fact that matches rule and
     *  label: ref keeps one fact per rule × subject with the codes deposited for it, this backend one fact per deposit
     *  with its own codes, so a code on any of the matching facts is a code of ref's one fact. */
    private String consistency(KieSession session, boolean inconsistentExpected, JsonNode v) {
        List<Object> facts = inconsistencies(session);
        if (!inconsistentExpected)
            return facts.isEmpty() ? null : "verwacht consistent, werkelijk inconsistent (" + renderIncs(facts, false) + ")";
        String rule = v.has("rule") && !v.get("rule").isNull() ? v.get("rule").asText() : null;
        String label = v.has("label") && !v.get("label").isNull() ? v.get("label").asText() : null;
        String code = v.hasNonNull("code") ? v.get("code").asText() : null;
        if (facts.isEmpty())
            return "verwacht inconsistent" + keyText(rule, label, code) + ", werkelijk consistent";
        if (rule == null && code == null) return null;   // presence-only expectation, satisfied by any fact
        for (Object f : facts) {
            if (rule != null && !sameRule(rule, (String) invokeGetter(f, "getRegel"))) continue;
            if (label != null && !label.equals(beanLabel(invokeGetter(f, "getSubject")))) continue;
            if (code == null || ((List<?>) invokeGetter(f, "getCodes")).contains(code)) return null;
        }
        return "verwacht inconsistent" + keyText(rule, label, code) + ", niet gevonden onder "
                + renderIncs(facts, code != null);
    }

    /** Every §9.5 Inconsistency fact the session holds. */
    private List<Object> inconsistencies(KieSession session) {
        List<Object> facts = new ArrayList<>();
        try {
            Class<?> inc = Class.forName(FactModelGenerator.PACKAGE + ".Inconsistentie", true, cl);
            facts.addAll(session.getObjects(inc::isInstance));
        } catch (ClassNotFoundException e) { /* no Inconsistency bean -> none */ }
        return facts;
    }

    /** ref's rule-key identity on the §9.5 channel ({@code ref/lib/case.ml}, the {@code Inconsistent} arm):
     *  {@code norm (strip_version a) = norm (strip_version b)}. A rule is a declared name like any other, so a pin
     *  spelled differently from its declaration, or a pin on a version-flattened deposit, names the same rule. */
    static boolean sameRule(String a, String b) {
        return a != null && b != null && Namen.norm(withoutVersion(a)).equals(Namen.norm(withoutVersion(b)));
    }

    /** The verifier's {@code Checks.strip_version}, character for character: a trailing {@code " #<digits>"}
     *  suffix with at least one digit, whose {@code #} sits at index 2 or later, comes off together with the one
     *  space before it; anything else stays. Stripped BEFORE norm, as ref does, so "X  #2" becomes "X " and then
     *  "x". Deliberately not {@code ProducerConsumer.baseName}, which also strips a group prefix and a
     *  beslistabel suffix and would make this comparison wider than ref's. */
    static String withoutVersion(String naam) {
        int i = naam.lastIndexOf('#');
        if (i >= 2 && naam.charAt(i - 1) == ' ' && i + 1 < naam.length()
                && naam.substring(i + 1).chars().allMatch(c -> c >= '0' && c <= '9'))
            return naam.substring(0, i - 1);
        return naam;
    }

    private static String keyText(String rule, String label) {
        return rule == null ? "" : " (" + rule + (label != null ? " @ " + label : "") + ")";
    }

    /** {@link #keyText(String, String)} with the pinned code after it, where the expectation pins one. */
    private static String keyText(String rule, String label, String code) {
        return keyText(rule, label) + (code == null ? "" : " code " + code);
    }

    /** The case label whose bean IS this fact's subject (identity) — the reverse of {@link #labelToBean},
     *  so the §9.5 fact's subject can be checked against the expectation's {@code label}. null if not an input bean. */
    private String beanLabel(Object bean) {
        if (bean == null) return null;
        for (Map.Entry<String, Object> e : labelToBean.entrySet()) if (e.getValue() == bean) return e.getKey();
        return null;
    }

    /** The §9.5 facts as "regel @ label", with their codes as "[EF-025]" after it where {@code withCodes}, sorted: the
     *  session gives them in the order of its working memory, which differs between two renderings of one analysis,
     *  and a mismatch line must not. */
    private String renderIncs(List<Object> facts, boolean withCodes) {
        List<String> out = new ArrayList<>();
        for (Object f : facts)
            out.add(invokeGetter(f, "getRegel") + " @ " + beanLabel(invokeGetter(f, "getSubject"))
                    + (withCodes ? " " + invokeGetter(f, "getCodes") : ""));
        out.sort(Comparator.naturalOrder());
        return String.join(", ", out);
    }

    // ---------------- the report's renderings ----------------

    /** The expectation itself: its kind and what it is about. */
    private static String source(JsonNode v) {
        String kind = v.get("kind").asText();
        if (kind.equals("inconsistent"))
            return kind + keyText(v.hasNonNull("rule") ? v.get("rule").asText() : null,
                    v.hasNonNull("label") ? v.get("label").asText() : null,
                    v.hasNonNull("code") ? v.get("code").asText() : null);
        StringBuilder b = new StringBuilder(kind);
        for (String field : new String[]{"label", "attribute", "characteristic"})
            if (v.hasNonNull(field)) b.append(' ').append(v.get(field).asText());
        return b.toString();
    }

    /** The expected side, rendered as {@link #actual} renders the actual one. */
    private String expected(JsonNode v) {
        return switch (v.get("kind").asText()) {
            case "attributeValue" -> render(valueToArg(v.get("value"), v.get("attribute").asText()));
            case "attributeEmpty" -> "leeg";
            case "attributeTimeline" -> {
                List<String> segs = new ArrayList<>();
                for (JsonNode s : v.get("timeline"))
                    segs.add("[" + s.get("from").asText() + "," + s.get("to").asText() + ")="
                            + render(valueToArg(s.get("value"), v.get("attribute").asText())));
                yield segs.isEmpty() ? "(leeg)" : String.join(" ", segs);
            }
            case "characteristicPresent" -> "aanwezig";
            case "characteristicAbsent" -> "afwezig";
            case "consistent" -> "consistent";
            default -> "inconsistent" + keyText(v.hasNonNull("rule") ? v.get("rule").asText() : null,
                    v.hasNonNull("label") ? v.get("label").asText() : null,
                    v.hasNonNull("code") ? v.get("code").asText() : null);
        };
    }

    /** The actual side after fire, read where the comparison reads it: a member through its declared name
     *  ({@link #attributeName}, {@link #characteristicName}), since a case may write it in another capitalisation. */
    private String actual(JsonNode v, KieSession session) {
        String label = v.hasNonNull("label") ? v.get("label").asText() : null;
        Object bean = label == null ? null : labelToBean.get(label);
        Objecttype ot = label == null ? null : labelType.get(label);
        return switch (v.get("kind").asText()) {
            case "attributeValue", "attributeEmpty" -> render(bean == null ? null
                    : invokeGetter(bean, Namen.getter(attributeName(ot, v.get("attribute").asText()))));
            case "attributeTimeline" -> {
                Object read = bean == null ? null
                        : invokeGetter(bean, Namen.getter(attributeName(ot, v.get("attribute").asText())));
                yield read == null ? "(leeg)" : renderTl((List<?>) read);
            }
            case "characteristicPresent", "characteristicAbsent" -> bean != null && Boolean.TRUE.equals(
                    invokeGetter(bean, Namen.getter(characteristicName(ot, v.get("characteristic").asText())))) ? "aanwezig" : "afwezig";
            default -> {
                List<Object> facts = inconsistencies(session);
                yield facts.isEmpty() ? "consistent" : "inconsistent (" + renderIncs(facts, v.hasNonNull("code")) + ")";
            }
        };
    }

    // ---------------- value helpers ----------------

    private static boolean equal(Object expectation, Object actual) {
        if (expectation == null) return actual == null;
        if (expectation instanceof BigDecimal b) return actual instanceof BigDecimal w && b.compareTo(w) == 0;
        return expectation.equals(actual);
    }

    /** {@link #equal} with the ONE value form whose identity is not byte equality: an ENUMERATION label is a
     *  DECLARED NAME, and ref compares those up to {@code Checks.norm} ({@code arith.ml}: {@code Enum a, Enum b
     *  -> norm a = norm b}), while the ASP backend stores the normalised label outright. The bean holds the
     *  case's own spelling — ref keeps that too — so an input written in a capitalisation other than the
     *  declaration's reads back byte-different and value-equal, and a byte comparator here would report a
     *  divergence that neither other executor has. {@code corpus/golden/jutterswet-keurstaat} spells one
     *  deliberately ("Beschadigd" against a declared "beschadigd") and that is what this arm exists for; it was
     *  unreachable until the enumeration literal reader stopped refusing the whole document (card
     *  `drools-enumlabel-lezer`, 2026-09-20). The expectation NODE decides, not the Java type: a Tekst value
     *  arrives as the same String and keeps the exact equality §13.3.3.4 gives it. */
    private static boolean equalValue(JsonNode expectedNode, Object expectation, Object actual) {
        if (expectedNode != null && expectedNode.has("enumeration"))
            return expectation instanceof String a && actual instanceof String b
                    && Namen.norm(a).equals(Namen.norm(b));
        return equal(expectation, actual);
    }

    /** The Java value a uchar case value node denotes — the already-normalised scalar the bean setter /
     *  global takes. An input form the v0 backend does not carry (a list/struct/constructor value, a
     *  rational lexeme, a timeline) is a reason-carrying {@link Excluded}. */
    Object valueToArg(JsonNode w, String field) {
        if (w == null || w.isNull()) return null;
        if (w.has("number")) {
            String s = w.get("number").asText();
            if (s.contains("/")) throw new Excluded(DroolsCode.RATIONAL_VALUE.reason("field", field));
            return new BigDecimal(s);
        }
        if (w.has("date")) return caseWireDate(w.get("date").asText(), field);
        if (w.has("dateTime")) return caseWireDateTime(w.get("dateTime").asText(), field);
        if (w.has("boolean")) return w.get("boolean").asBoolean();
        if (w.has("text")) return w.get("text").asText();
        if (w.has("enumeration")) return w.get("enumeration").asText();
        if (w.has("timeline")) throw new Excluded(DroolsCode.TIMELINE_VALUE_SCALAR_POSITION.reason("field", field));
        throw new Excluded(DroolsCode.VALUE_FORM_NOT_CARRIED.reason("field", field, "value", w));
    }

    /* The uchar-case-v0 {@code date} lexeme, matched WHOLE: four-two-two digit fields, digits only, no sign
     * and no extended year. It is the schema's own pattern, held here as well, for the same reason as the
     * dateTime one below — gate 4 checks case FILES, this reader checks every case date that reaches the
     * backend by any route. */
    private static final java.util.regex.Pattern CASE_DATE =
            java.util.regex.Pattern.compile("\\d{4}-\\d{2}-\\d{2}");

    /** The calendar day a case {@code date} names — or a loud refusal naming the FORM, never a guess.
     *  <p>{@link LocalDate#parse} is laxer than the wire it reads: beside the four-digit year it also accepts
     *  the ISO forms with a SIGNED, extended year, which the schema pattern excludes and the interpreter
     *  refuses — {@code +012026-03-15} is year 12026 and {@code -0500-01-01} is year −500. Measured
     *  2026-09-10 on the three positions a date reaches this reader (a value, the case's {@code asOfDate}, a
     *  timeline segment's boundary): with such a lexeme in the {@code asOfDate} of one golden and in a
     *  segment boundary of another, this backend replayed both as PLAYED while the interpreter faulted on the
     *  same two documents. That is a divergence the differential gate cannot see, since a case evaluated at
     *  the wrong date or bucketed on the wrong grid may still satisfy every pinned expectation. Each reader
     *  holds its own fence (IR-SPEC ch. 4.1), so the fence sits here rather than only in the schema, which
     *  sees files and not the values a generator or a hand run hands this backend directly. A malformed
     *  lexeme is a document defect, not a form this backend does not carry, so it is a hard error and never
     *  an {@link Excluded}. */
    private static LocalDate caseWireDate(String lexeme, String field) {
        if (!CASE_DATE.matcher(lexeme).matches())
            throw new CaseDateFault(field, lexeme, Msg.drools("EF-003.malformed", "lexeme", lexeme, "field", field));
        try { return LocalDate.parse(lexeme); }
        catch (java.time.format.DateTimeParseException e) {
            throw new CaseDateFault(field, lexeme,
                    Msg.drools("EF-003.calendar", "lexeme", lexeme, "field", field, "detail", e.getMessage()));
        }
    }

    /** The refusal of a case date lexeme that names no calendar day (RUNTIME-SEMANTICS ch. 5.7b, EF-003). A document
     *  defect, so a hard error and never an {@link Excluded}, as before; a type of its own only so that the red path
     *  can name its kind ({@link #faultKinds}). */
    static final class CaseDateFault extends IllegalArgumentException {
        private static final long serialVersionUID = 1L;

        private final String field;
        private final String lexeme;

        CaseDateFault(String field, String lexeme, Msg message) {
            super(message.text());
            this.field = field;
            this.lexeme = lexeme;
        }

        String code() {
            return "EF-003";
        }

        /** The parameters of the kind (ch. 5.7b): the field of the case input and the lexeme it holds. */
        java.util.Map<String, String> params() {
            return java.util.Map.of("field", field, "lexeme", lexeme);
        }
    }

    /* The uchar-case-v0 {@code dateTime} lexeme, matched WHOLE: fixed field widths, {@code T} as the
     * separator, the seconds field always written, a fraction that is optional but EXACTLY three digits
     * (whole milliseconds). It is the schema's own pattern, held here as well — gate 4 checks case FILES,
     * this reader checks every case value that reaches the backend by any route. */
    private static final java.util.regex.Pattern CASE_DATE_TIME =
            java.util.regex.Pattern.compile("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(?:\\.\\d{3})?");

    /** The instant a case {@code dateTime} names — or a loud refusal naming the FORM, never a guess.
     *  <p>{@link LocalDateTime#parse} is laxer than the wire it reads: it accepts {@code …T12:00} without a
     *  seconds field, and a fraction of ANY width, which it reads POSITIONALLY — {@code .1} as a hundred
     *  milliseconds and {@code .1234} as 123.4. The shared interpreter/ASP case reader read those same three
     *  digits differently ({@code .1} as ONE millisecond, {@code .100000} as a hundred THOUSAND), and the
     *  literal reader refused them outright: one wire, three answers. Inside the schema's fence all three
     *  agree, and that fence was the only reason the divergence never showed. Each reader now holds it
     *  itself, so a case reaching a reader by another route (a generator, a hand run, a second producer) is
     *  refused rather than answered three ways. A malformed lexeme is a document defect, not a form this
     *  backend does not carry, so it is a hard error and never an {@link Excluded}. */
    private static LocalDateTime caseWireDateTime(String lexeme, String field) {
        if (!CASE_DATE_TIME.matcher(lexeme).matches())
            throw new IllegalArgumentException("malformed dateTime '" + lexeme + "' on '" + field
                    + "': the uchar-case-v0 lexeme is yyyy-mm-ddThh:mm:ss[.fff] — fixed field widths, 'T',"
                    + " the seconds field written, no zone or offset, a fraction of exactly three digits");
        try { return LocalDateTime.parse(lexeme); }
        catch (java.time.format.DateTimeParseException e) {
            throw new IllegalArgumentException("malformed dateTime '" + lexeme + "' on '" + field
                    + "': the field widths are right but the calendar is not (" + e.getMessage() + ")");
        }
    }

    static Class<?> argType(Datatype dt) {
        if (dt == null) return Object.class;
        if (dt.isNumber() || dt.isPercentage()) return BigDecimal.class;
        if (dt.isText() || dt instanceof Datatype.Enumeratie) return String.class;
        if (dt instanceof Datatype.BooleanType) return Boolean.class;
        if (dt instanceof Datatype.DatumInDagen) return LocalDate.class;
        if (dt instanceof Datatype.DateTime) return LocalDateTime.class;
        return Object.class;
    }

    static void call(Object bean, String method, Class<?> type, Object value) {
        try { bean.getClass().getMethod(method, type).invoke(bean, value); }
        catch (ReflectiveOperationException e) {
            throw new RuntimeException("kon setter " + method + "(" + type.getSimpleName() + ") niet aanroepen op "
                    + bean.getClass().getSimpleName(), e);
        }
    }

    static Object invokeGetter(Object bean, String getter) {
        for (String mth : new String[]{ getter, "is" + getter.substring(3) }) {
            try { return bean.getClass().getMethod(mth).invoke(bean); }
            catch (NoSuchMethodException ignore) { /* try next form */ }
            catch (ReflectiveOperationException e) { throw new RuntimeException("kon " + mth + " niet lezen", e); }
        }
        throw new IllegalStateException("geen getter " + getter + " op " + bean.getClass().getSimpleName());
    }

    private static String render(Object o) { return o == null ? "leeg" : o.toString(); }
}
