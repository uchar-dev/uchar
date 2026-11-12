package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import dev.uchar.regelspraak.engine.ScenarioResult;
import dev.uchar.regelspraak.message.Msg;
import dev.uchar.regelspraak.refusal.DroolsCode;
import dev.uchar.regelspraak.refusal.Reason;
import dev.uchar.regelspraak.loader.IrLoader;
import dev.uchar.regelspraak.core.FixedStartPoint;
import dev.uchar.regelspraak.model.Datatype;
import dev.uchar.regelspraak.ir.Expr;
import dev.uchar.regelspraak.ir.PrioritizedDefaults;
import dev.uchar.regelspraak.ir.ResolvedRule;
import dev.uchar.regelspraak.ir.Spelling;
import dev.uchar.regelspraak.model.RegelSpraakModel;
import dev.uchar.regelspraak.model.RegelSpraakModel.Granularity;
import dev.uchar.regelspraak.model.RegelSpraakModel.ObjectType;
import dev.uchar.regelspraak.runtime.Breuk;
import dev.uchar.regelspraak.runtime.RegelSpraakRuntime;
import dev.uchar.regelspraak.schema.FactModelGenerator;
import dev.uchar.regelspraak.transpiler.core.Namen;
import dev.uchar.regelspraak.transpiler.tms.ComputeFault;
import dev.uchar.regelspraak.transpiler.tms.EvaluationFaults;
import dev.uchar.regelspraak.transpiler.tms.InputFormFault;
import dev.uchar.regelspraak.transpiler.tms.OpenSegmentFault;
import dev.uchar.regelspraak.transpiler.tms.CharacteristicPeriodsFault;
import dev.uchar.regelspraak.transpiler.tms.SecondFillingFault;
import dev.uchar.regelspraak.transpiler.tms.ConstructorValue;
import dev.uchar.regelspraak.transpiler.tms.Period;
import dev.uchar.regelspraak.transpiler.tms.WriteBoundaryFault;
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
 * values are the SAME expectations ref and clingo verify, a played pass IS the differential square. Every case of a
 * golden plays, also after one this backend does not carry, and a case that diverges makes the golden FAIL whatever
 * another case excludes.
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
        /** The sentence in {@code locale}. */
        public String message(java.util.Locale locale) { return reason.text(locale); }
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
    private String documentHash;

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
     *  characteristic {@code name} of an instance of {@code ot} when that member has no bean field of its own. */
    void ownField(ObjectType ot, String name, boolean characteristic, String action) {
        Reason cause = ot == null ? null : FactModelGenerator.displacedMember(displacements, ot, name, characteristic);
        if (cause != null) throw new Excluded(cause.framed("case-" + action));
    }

    /** Load {@code irPath}, build the KieBase, and replay each of {@code casePaths}. Any {@link Excluded}
     *  (loader refusal, transpiler marker, contract violation, unsupported form) becomes an EXCLUDE result. */
    public static Result runGolden(Path irPath, List<Path> casePaths) {
        return replay(irPath, casePaths, DroolsBackend.Branch.EXEC_MODEL);
    }

    /** The same replay through the OTHER branch: the analysis rendered as DRL text, which {@link DroolsBackend}
     *  compiles to a {@link KieBase} with the classic DRL build. A GATE-12 PATH (card gate12-drl-takteller):
     *  check-drools.sh sweeps every golden through it (a {@code --drl} run of {@link DroolsBackendMain}) and demands
     *  per golden the status line of the exec-model branch, since the text is the second rendering of the analysis
     *  (card drl-teksttak-op-truth-maintenance). Gate 13 drives it too. */
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
        CaseRunner runner;
        try {
            runner = build(irPath, branch);
        } catch (Excluded e) {
            return Result.excluded(e.code(), e.getMessage());
        } catch (IrLoader.Refusal e) {
            return Result.excluded(e.code(), e.reason().framed("loader").text());
        } catch (java.io.IOException e) {
            throw new RuntimeException("could not read " + irPath + ": " + e.getMessage(), e);
        }
        List<String> failures = new ArrayList<>();
        // the first case this backend does not carry; every case plays, so that it hides no FAIL of another
        Excluded excluded = null;
        int played = 0;
        for (Path cp : casePaths) {
            JsonNode c;
            try {
                c = readCase(cp);
            } catch (java.io.IOException e) {
                throw new RuntimeException("could not read " + cp + ": " + e.getMessage(), e);
            }
            if (c.has("expectedFault")) continue;   // red-path case in a green zone — not this gate's business
            played++;
            try {
                String fail = runner.runCase(cp.getFileName().toString(), c);
                if (fail != null) failures.add(fail);
            } catch (Excluded e) {
                if (excluded == null) excluded = e;
            }
        }
        if (played == 0)
            throw new IllegalArgumentException("no case of " + irPath + " carries expected: every case file given carries"
                    + " expectedFault, which the red path plays");
        if (failures.isEmpty() && excluded != null) return Result.excluded(excluded.code(), excluded.getMessage());
        return Result.played(played, failures);
    }

    /**
     * The readings of the cases {@code casePaths} on a build with readings of {@code irPath} on {@code branch}, in the
     * canonical text of {@link SessionDump#readingLines}, each line after {@code reading }: per case a line
     * {@code case <file>} and the readings at its fixpoint, then its end state in the text of
     * {@link SessionDump#endStateLines}, each line after {@code state }, none where an evaluation fault stands, which
     * leaves the session without an end state (RUNTIME-SEMANTICS ch. 5.12a); or {@code nofixpoint <sentence>} where the
     * fire reached none.
     * A red-path case is skipped, as the replay skips it. The entry of {@code DroolsBackendMain --readings}, the text the
     * trace of the reference interpreter is held to (card ref-trace-and-readings-gate). A form this backend does not
     * carry is refused as the replay refuses it, an {@link Excluded} or an {@link IrLoader.Refusal}.
     */
    static List<String> readings(Path irPath, List<Path> casePaths, DroolsBackend.Branch branch)
            throws java.io.IOException {
        CaseRunner runner = build(irPath, branch, true);
        List<String> out = new ArrayList<>();
        for (Path cp : casePaths) {
            JsonNode c = JSON.readTree(Files.readString(cp));
            if (c.has("expectedFault")) continue;
            out.add("case " + cp.getFileName());
            out.addAll(runner.withSession(c, (build, session, labels) -> {
                DroolsBackend.Firing firing;
                try {
                    firing = DroolsBackend.fire(build, session);
                } catch (ConsequenceException e) {
                    throw DroolsBackend.designate(e);
                }
                if (firing.noFixpoint() != null) return List.of("nofixpoint " + firing.noFixpoint());
                DroolsBackend.refuseLimit(build, session);
                List<String> lines = new ArrayList<>();
                SessionDump.readingLines(build, session, labels).forEach(l -> lines.add("reading " + l));
                // a session in which an evaluation fault stands has its readings and no end state (ch. 5.12a)
                Map<Object, String> label = new java.util.IdentityHashMap<>();
                labels.forEach((l, b) -> label.put(b, l));
                if (DroolsBackend.fixpointOf(build, session, label::get).faults().isEmpty())
                    SessionDump.endStateLines(build, session, labels).forEach(l -> lines.add("state " + l));
                return lines;
            }));
        }
        return out;
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
        return report(irPath, casePaths, null);
    }

    /** What a consumer of the scenario report takes of the session of one case ({@link #report(Path, List, Handoff)}):
     *  the session right after its fire, before the write-back, of a build with readings, with the case labels of its
     *  input beans and what the fire did. */
    @FunctionalInterface
    public interface Handoff {
        void take(String caseFile, DroolsBackend.Build build, KieSession session, Map<String, Object> labels,
                  DroolsBackend.Firing firing);
    }

    /**
     * The scenario report of {@link #report(Path, List)}, which plays every case on a build with readings and hands
     * {@code handoff} each session it fired, right after the fire and before the write-back, so that a consumer reads
     * the fixpoint, the decisions of the defaults stacks and the readings of the conditions of the very session the
     * report judged ({@link DroolsBackend#fixpointOf}, {@link DroolsBackend#decisionsOf},
     * {@link DroolsBackend#readingsOf}) instead of playing the case a second time; the input beans it is given carry
     * the derived values once the report wrote them back, where no evaluation fault stands. A case whose input or fire
     * raises is handed nothing, as it has no session to read. A consumer reads the session and changes nothing in it:
     * the report writes it back and compares it after the handoff. The reports are those of the build without readings: the
     * reading rules leave the fixpoint, the observer and the coverage alone ({@code ReadingRulesFixpointWitnessTest},
     * {@code ReadingRulesObserverWitnessTest}). A null {@code handoff} is {@link #report(Path, List)}.
     */
    public static List<ScenarioResult> report(Path irPath, List<Path> casePaths, Handoff handoff) {
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
            runner = handoff == null ? build(irPath) : build(irPath, DroolsBackend.Branch.EXEC_MODEL, true);
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
            String name = paths.get(i).getFileName().toString();
            Observation w = new Observation(new DroolsBackend.Observer(runner::beanLabel), handoff);
            String failure;
            try {
                caseFile(cases.get(i));
                failure = runner.runCase(name, cases.get(i), w);
            } catch (Excluded e) {
                out.add(marker(paths.get(i), e.getMessage()));
                continue;
            } catch (RuntimeException e) {
                // The replay calls this the golden's ERROR and stops; a report of several cases ends only this one,
                // so the cases beside it keep their verdicts.
                out.add(unplayable(paths.get(i), e));
                continue;
            }
            out.add(new ScenarioResult(name, failure == null ? ScenarioResult.Status.PASS : ScenarioResult.Status.FAIL,
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
        String name = caseFilePath.getFileName().toString();
        String message = String.valueOf(e.getMessage()).replace('\n', ' ');
        return new ScenarioResult(name, ScenarioResult.Status.FAIL, List.of(),
                new ScenarioResult.Trace(List.of()), List.of(),
                name + ": ERROR " + e.getClass().getSimpleName() + ": " + message);
    }

    /** What one case's replay reports beside its verdict: what the session fired, through the backend's entry as a
     *  consumer records it, with the case labels of this runner, and per expectation its verdict. */
    private static final class Observation {
        final DroolsBackend.Observer fireObserver;
        /** The consumer the session goes to after its fire, null for none. */
        final Handoff handoff;
        final List<ScenarioResult.ExpectationResult> expectations = new ArrayList<>();

        Observation(DroolsBackend.Observer fireObserver, Handoff handoff) {
            this.fireObserver = fireObserver;
            this.handoff = handoff;
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
        return new LinkedHashMap<>(CaseInput.of(build(irPath).backendBuild, c).beans());
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
        return runner.withSession(c, (build, session, labels) -> {
            String noFixpoint = runner.fireToFixpoint(session, null, null).noFixpoint();
            // The sister runCase path reports the cap as a case failure; this seam has no verdict to return, so
            // a run that never reached a fixpoint must not hand back a half-fired fact base as if it had settled.
            if (noFixpoint != null) throw new IllegalStateException(noFixpoint);
            return new AfterFire(new LinkedHashMap<>(labels), new ArrayList<>(session.getObjects()));
        });
    }

    /** The comparator seam of {@code backends/drools/src/test}: one authored in-memory case through the FULL
     *  replay (build, inputs, fire, compare), returning the per-case failure line — null on a match. The
     *  read-back comparator has no other directly observable surface: gate 12 pins post-fire values on corpus
     *  cases whose pinned lists happen to match segment-for-segment, so a comparator that ignores waarde or
     *  order survives the whole gate set (lens 1 proved it by mutation) — its verdicts are asserted here. */
    static String playCase(Path irPath, JsonNode c) throws java.io.IOException {
        return playCase(irPath, c, DroolsBackend.Branch.EXEC_MODEL);
    }

    /** The same comparator seam through EITHER transpiler branch, so one authored document can be replayed by
     *  both and their verdicts compared row by row. It exists for the same reason {@link #viaExecModel} does:
     *  a witness that wants both branches on ONE in-memory case would otherwise have to write the document to
     *  disk twice and read two {@link Result}s, which reports a whole golden's status where the row wants a
     *  single cell's verdict. Package-private, no production caller. */
    static String playCase(Path irPath, JsonNode c, boolean viaDrl) throws java.io.IOException {
        return playCase(irPath, c, viaDrl ? DroolsBackend.Branch.DRL_TEXT : DroolsBackend.Branch.EXEC_MODEL);
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

    /** The measuring seam on a build with readings where {@code readings} ({@link DroolsBackend#build(Path,
     *  DroolsBackend.Branch, boolean)}): the witnesses of card fixpoint-readings fire one case on the build with and the
     *  build without and read the readings of the first. Package-private, no production caller. */
    static CaseRunner forMeasurement(Path irPath, DroolsBackend.Branch branch, boolean readings)
            throws java.io.IOException {
        return build(irPath, branch, readings);
    }

    /** What a caller does with one session that {@link #withSession} set up: fire it and read or measure it as it
     *  chooses. {@code labels} maps each case label to its input bean. */
    @FunctionalInterface
    interface SessionMeasurement<T> {
        T meet(DroolsBackend.Build backendBuild, KieSession session, Map<String, Object> labels);
    }

    /** Sets up case {@code c} in a fresh session with the bean loader as the thread's context class loader: its input
     *  read through {@link CaseInput}, its globals set and its beans put in. Hands that session, not yet fired, to
     *  {@code measurement}, and disposes of it afterwards. Every replay, red path, report and reading of this class sets
     *  up its session here, and so do the measuring harnesses of the test tree, which fire through
     *  {@link DroolsBackend#fire} as a consumer does and time the fire and the write-back apart. */
    <T> T withSession(JsonNode c, SessionMeasurement<T> measurement) {
        labelToBean.clear();
        labelType.clear();
        ClassLoader previous = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(cl);
        KieSession session = kieBase.newKieSession();
        try {
            CaseInput input = readInput(c);
            input.setGlobals(session);
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
        return build(irPath, branch, false);
    }

    /** As above, on a build with readings where {@code readings}. */
    private static CaseRunner build(Path irPath, DroolsBackend.Branch branch, boolean readings)
            throws java.io.IOException {
        DroolsBackend.Build b = DroolsBackend.build(irPath, branch, readings);
        CaseRunner runner = new CaseRunner(b.model(), b.kieBase(), b.beanLoader(), b.dayKindGlobals());
        runner.backendBuild = b;
        if (SessionDump.SEAM_DIRECTORY != null) runner.documentHash = SessionDump.hash(irPath);
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
        return withSession(c, (build, session, labels) -> fireToFixpoint(session, null, null));
    }

    /** A fault kind (RUNTIME-SEMANTICS ch. 5.7b): its EF code and its parameters, as a red case pins them. */
    public record FaultKind(String code, Map<String, String> params) {}

    /** The fault kinds of the red-path fault {@code t}, in the order of its report, each code once; empty where it names
     *  no kind. Classified here, at the catch, by exception type: the backend's own fault exceptions name theirs
     *  ({@code code()}, {@code params()}); several faults at the fixpoint ({@link EvaluationFaults}) give the kind of
     *  each. In a cause chain the outermost exception that names a kind decides: a fault named after its rule wraps the
     *  same fault unnamed. The production runtime's {@link Breuk.DelingDoorNul} names no kind: a division by a numeric
     *  zero is the §9.5 deposit EF-005 since v1 (RUNTIME-SEMANTICS ch. 1.4 point 4), which this branch makes before the
     *  runtime is called ({@code ComputeHelpers.divided}), so the runtime's fault reaches no fault rule from a
     *  division of the IR. Public: a consumer that reports a fault of a session or of the case input names its kind and
     *  parameters through here, as the red path does. */
    public static List<FaultKind> faultKinds(Throwable t) {
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
            else if (c instanceof InputPrecisionFault d) kind = new FaultKind(d.code(), d.params());
            else if (c instanceof WriteBoundaryFault w) kind = new FaultKind(w.code(), w.params());
            else if (c instanceof OpenSegmentFault o) kind = new FaultKind(o.code(), o.params());
            else if (c instanceof InputFormFault i) kind = new FaultKind(i.code(), i.params());
            else if (c instanceof CharacteristicPeriodsFault k) kind = new FaultKind(k.code(), k.params());
            else if (c instanceof SecondFillingFault s) kind = new FaultKind(s.code(), s.params());
            else if (c instanceof CaseInput.LinkCycleFault l) kind = new FaultKind(l.code(), l.params());
            else if (c instanceof CaseInput.CellFault f) kind = new FaultKind(f.code(), f.params());
            else if (c instanceof CaseInput.NameFault f) kind = new FaultKind(f.code(), f.params());
            else if (c instanceof CaseInput.LinkTargetFault f) kind = new FaultKind(f.code(), f.params());
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
    private final Map<String, ObjectType> labelType = new LinkedHashMap<>();

    private String runCase(String name, JsonNode c) {
        return runCase(name, c, null);
    }

    /** One case: its verdict line, null on a match. The expectations are compared through the backend's entry
     *  ({@link DroolsBackend#compare}), with the case labels of this runner, as a consumer compares them. With a
     *  {@link Observation} the replay also reports the firings and every expectation's verdict ({@link #report}); the
     *  verdict itself does not depend on it. */
    private String runCase(String name, JsonNode c, Observation w) {
        return withSession(c, (build, session, labels) -> {
            Handoff handoff = w == null ? null : w.handoff;
            String noFixpoint = fireToFixpoint(session, w == null ? null : w.fireObserver, handoff == null ? null
                    : firing -> handoff.take(name, build, session,
                            java.util.Collections.unmodifiableMap(new LinkedHashMap<>(labels)), firing))
                    .noFixpoint();
            if (noFixpoint != null) return name + ": " + noFixpoint;
            DroolsBackend.Comparison comparison = DroolsBackend.compare(build, session, this::beanLabel, c.get("expected"));
            if (w != null) w.expectations.addAll(comparison.expectations());
            return comparison.pass() ? null : name + ": " + comparison.diagnostic();
        });
    }

    /** Fires {@code session} through the backend's entry ({@link DroolsBackend#fire}), under the cap a consumer gets, and
     *  returns the {@link DroolsBackend.Firing}, whose {@code noFixpoint()} is null at a fixpoint and else the sentence
     *  of {@code fire}, which names the cap and what it was computed from, or that of the creation budget, beside its
     *  code. {@code observer}, where it is not null, records what the session fires ({@link DroolsBackend.Observer});
     *  {@code afterFire}, where it is not null, gets the firing right after the fire and before the write-back
     *  ({@link Handoff}). A consequence that throws leaves the fire as
     *  {@link DroolsBackend#designate} names it; every replay, red path and report of this class fires here, on either
     *  branch. At a fixpoint the derivations go onto the beans before anything reads them, or the evaluation faults
     *  that stand there are thrown ({@link DroolsBackend#setDerivedValues}); without a fixpoint nothing is written
     *  back, since the replay reports the sentence and reads no bean. With the property
     *  {@value SessionDump#PROPERTY} a seam writes the dump of the session ({@link SessionDump}) right after the fire,
     *  before the write-back, also where the fire throws, on either branch. */
    private DroolsBackend.Firing fireToFixpoint(KieSession session, DroolsBackend.Observer observer,
                                                java.util.function.Consumer<DroolsBackend.Firing> afterFire) {
        SessionDump trace = documentHash == null ? null : SessionDump.op(session, labelToBean);
        DroolsBackend.Firing firing;
        try {
            firing = observer == null ? DroolsBackend.fire(backendBuild, session) : DroolsBackend.fire(backendBuild, session, observer);
        } catch (RuntimeException e) {
            if (trace != null) trace.seam(documentHash, backendBuild, e, labelToBean);
            if (e instanceof ConsequenceException c) throw DroolsBackend.designate(c);
            throw e;
        }
        if (trace != null) trace.seam(documentHash, backendBuild, firing, labelToBean);
        if (afterFire != null) afterFire.accept(firing);
        if (firing.noFixpoint() == null) DroolsBackend.setDerivedValues(backendBuild, session, this::beanLabel);
        return firing;
    }

    /** Reads case {@code c} through the backend's entry ({@link CaseInput}), as a consumer reads it, and keeps its
     *  labels for this runner's report, seam and write-back. */
    private CaseInput readInput(JsonNode c) {
        CaseInput input = CaseInput.of(backendBuild, c);
        labelToBean.putAll(input.beans());
        labelType.putAll(input.types());
        return input;
    }

    /** Whether {@code attr} on {@code ot} is a §13.3.6 temporal bean member — a {@code List<Segment>} field,
     *  the shape {@link FactModelGenerator} derives from a declared attributeGranularity. */
    boolean temporal(ObjectType ot, String attr) {
        return Namen.containsName(ot.granularityMembers(), attr);
    }

    /** Whether {@code attr} on {@code ot} is a §3.6 dimensioned bean member — a {@code Map} field of its cells by
     *  their labels ({@link FactModelGenerator#cellKey}). Checked BEFORE {@link #temporal}: the generator gives a
     *  dimensioned declaration the Map shape even when a granularity is declared beside it. */
    boolean dimensioned(ObjectType ot, String attr) {
        return !ot.dimensionsOf(attr).isEmpty();
    }

    /** The declared name of the attribute a case writes as {@code name} on {@code ot} or one of its ancestors, in its
     *  input or in an expectation, under the name equality of the reference interpreter's case reader
     *  ({@link Namen#norm}: runs of spaces and tabs as one space, A–Z as a–z, every other character as written). The
     *  generator derives a member's getter and setter from its declared name, so every read and write of a member the
     *  case names goes through here: "vondstNummer" for a declared "vondstnummer" is that member. A name that is no
     *  declared attribute is a defect of the case, which the reference interpreter refuses at load, and never the
     *  member whose Java name its spelling happens to derive, as "premie-som" derives that of "premie som". */
    static String attributeName(ObjectType ot, String name) {
        String declared = declaredAttribute(ot, name);
        if (declared == null)
            throw new IllegalArgumentException("attribute \"" + name + "\" is not declared on \"" + ot.name + "\"");
        return declared;
    }

    /** {@link #attributeName} for a characteristic. */
    static String characteristicName(ObjectType ot, String name) {
        String declared = declaredCharacteristic(ot, name);
        if (declared == null)
            throw new IllegalArgumentException("characteristic \"" + name + "\" is not declared on \"" + ot.name + "\"");
        return declared;
    }

    /** The declared name of the attribute {@code name} names on {@code ot} or one of its ancestors, or null; the case
     *  input refuses a null as EF-040 ({@code CaseInput.NameFault}), an expectation in the sentence of
     *  {@link #attributeName}. */
    static String declaredAttribute(ObjectType ot, String name) {
        return declared(ot.attributeMembers().keySet(), name);
    }

    /** {@link #declaredAttribute} for a characteristic. */
    static String declaredCharacteristic(ObjectType ot, String name) {
        List<String> chain = new ArrayList<>();
        for (ObjectType t : ot.chain()) chain.addAll(t.characteristics);
        return declared(chain, name);
    }

    /** {@link #declaredAttribute} for the object type of an instance: the declared type, or null. */
    static ObjectType declaredObjectType(RegelSpraakModel model, String name) {
        String declared = declared(model.objectTypes.keySet(), name);
        return declared == null ? null : model.objectTypes.get(declared);
    }

    /** The member of {@code declared} that is {@code name} under {@link Namen#norm}, or null. */
    private static String declared(java.util.Collection<String> declared, String name) {
        for (String d : declared) if (Namen.norm(d).equals(Namen.norm(name))) return d;
        return null;
    }

    /** The case's {@code asOfDate}, or null when it carries none. */
    static LocalDate asOfDate(JsonNode c) {
        JsonNode asOf = c.get("asOfDate");
        if (asOf == null || asOf.isNull()) return null;
        if (!asOf.isTextual()) throw new IllegalArgumentException("asOfDate: expected a string or null, got " + asOf);
        return caseWireDate(asOf.asText(), "asOfDate");
    }

    /**
     * Holds the case file {@code c} to what the case format demands of a file beside what {@link CaseInput#of} reads,
     * as the reference interpreter's case reader holds it: the format {@code uchar-case-v0}; the members
     * {@code asOfDate} (a date or null), {@code input.instances} and {@code input.parameters}; per instance its
     * {@code attributes}, {@code roles} and {@code characteristics}; and exactly one of {@code expected}, a list of at
     * least one expectation, and {@code expectedFault}. The replay and the report, which compare a case with its
     * expectations, hold every case file to it; a consumer that writes a case of its own for {@link CaseInput} leaves
     * out what it has no use for, and the red path and the readings read a case for its input alone.
     */
    static void caseFile(JsonNode c) {
        if (!c.isObject()) throw new IllegalArgumentException("<root>: expected an object, got " + c);
        String format = member(c, "<root>", "format").asText();
        if (!format.equals("uchar-case-v0")) throw new IllegalArgumentException("unsupported case format \"" + format + "\"");
        member(c, "<root>", "asOfDate");
        JsonNode input = member(c, "<root>", "input");
        member(input, "input", "parameters");
        JsonNode instances = member(input, "input", "instances");
        for (JsonNode i : instances) {
            String label = i.path("label").asText();
            for (String section : new String[]{"attributes", "roles", "characteristics"}) member(i, label, section);
        }
        boolean expected = c.has("expected"), fault = c.has("expectedFault");
        if (expected && fault) throw new IllegalArgumentException("a case carries exactly one of expected / expectedFault");
        if (!expected && !fault) throw new IllegalArgumentException("<root>: missing field \"expected\"");
        if (expected && (!c.get("expected").isArray() || c.get("expected").isEmpty()))
            throw new IllegalArgumentException("expected: a list of at least one expectation, got " + c.get("expected"));
    }

    /** The member {@code name} of the object {@code o} at position {@code at}, refused where it is missing. */
    private static JsonNode member(JsonNode o, String at, String name) {
        if (!o.isObject()) throw new IllegalArgumentException(at + ": expected an object, got " + o);
        JsonNode m = o.get(name);
        if (m == null) throw new IllegalArgumentException(at + ": missing field \"" + name + "\"");
        return m;
    }

    /** The case file {@code path}, read and held to the case format ({@link #caseFile}). */
    private static JsonNode readCase(Path path) throws java.io.IOException {
        JsonNode c = JSON.readTree(Files.readString(path));
        caseFile(c);
        return c;
    }

    /** Whether {@code w} is the timeline variant of a case value: an object whose one key is {@code timeline}. */
    static boolean isTimeline(JsonNode w) {
        return w != null && w.isObject() && w.size() == 1 && w.has("timeline");
    }

    /** The {@code number} lexeme of {@code uchar-case-v0}, matched whole: the case schema's pattern. */
    private static final java.util.regex.Pattern CASE_NUMBER =
            java.util.regex.Pattern.compile("-?[0-9]+(\\.[0-9]+)?|-?[0-9]+/[1-9][0-9]*");

    /**
     * Holds the scalar case value {@code w} at position {@code at} to the declared datatype {@code dt} as the reference
     * interpreter's case reader holds it (RUNTIME-SEMANTICS ch. 5.7 step 3): an object with one value variant, or a
     * constructor value of a sum type; the variant that of the datatype; a number in the case's number lexeme; a
     * boolean a JSON truth value; a text, date, dateTime or enumeration value a JSON string; an enumeration value a
     * label its enumeration declares. A defect of the case, so a hard error, in that reader's words. The lexeme of a
     * date or dateTime is held where it is read ({@link #valueToArg}), its precision by {@link #inputFits}.
     */
    static void typed(JsonNode w, Datatype dt, String at) {
        if (w == null || !w.isObject()) throw new IllegalArgumentException(at + ": expected an object, got " + w);
        if (w.has("constructor")) {
            if (!(dt instanceof Datatype.SumType s))
                throw new IllegalArgumentException(at + ": a constructor value against a "
                        + (dt == null ? "declaration without a datatype" : "non-sum-type declaration"));
            constructorFits(w, s, at);
            return;
        }
        if (w.size() != 1) throw new IllegalArgumentException(at + ": expected one value variant, got " + w);
        if (dt == null) throw new IllegalArgumentException(at + ": a value against a declaration without a datatype");
        String variant = w.fieldNames().next();
        JsonNode v = w.get(variant);
        boolean fits = switch (variant) {
            case "number" -> dt.isNumberLike();
            case "date" -> dt instanceof Datatype.DateInDays;
            case "dateTime" -> dt instanceof Datatype.DateTime;
            case "boolean" -> dt.isBoolean();
            case "text" -> dt.isText();
            case "enumeration" -> dt instanceof Datatype.Enumeration;
            default -> false;
        };
        if (!fits)
            throw new IllegalArgumentException(at + ": value variant \"" + variant + "\" does not match the declared datatype");
        if (variant.equals("boolean")) {
            if (!v.isBoolean()) throw new IllegalArgumentException(at + ".boolean: expected true or false, got " + v);
            return;
        }
        if (!v.isTextual()) throw new IllegalArgumentException(at + "." + variant + ": expected a string, got " + v);
        if (variant.equals("number") && !CASE_NUMBER.matcher(v.asText()).matches())
            throw new IllegalArgumentException(at + " \"" + v.asText() + "\" is not the uchar-case-v0 number lexeme"
                    + " [-]digits[.digits] or [-]digits/digits, the denominator without a leading 0");
        if (dt instanceof Datatype.Enumeration en
                && en.values().stream().noneMatch(label -> Namen.norm(label).equals(Namen.norm(v.asText()))))
            throw new IllegalArgumentException(at + ": \"" + v.asText() + "\" is not a declared enumeration value");
    }

    // ---------------- §13.3.6 temporal input (the List<Segment> bean members) ----------------

    /** The segments of a timeline value, with ref's load-time validation mirrored: at least one segment (an
     *  EXPECTATION may be empty — {@code emptyAllowed}), {@code from} before {@code to}, both boundaries on the
     *  declared granularity grid, and the list ordered and non-overlapping as written. A segment that leaves out
     *  {@code from} or {@code to} has an open bound (RUNTIME-SEMANTICS ch. 3.1), a null bound of its {@code Segment}:
     *  it sits on no grid, only the first segment can begin open and only the last can end open, which the order check
     *  holds. A malformed timeline is a hard failure, never a silent acceptance, and so is a segment without a value or
     *  with a value its datatype {@code dt} does not hold ({@link #typed}): the case format writes filled segments only
     *  (RUNTIME-SEMANTICS ch. 3.1). A grid that carries no case timeline is a
     *  not-carried form, refused where ref's loader refuses it and in its sentence, at position {@code site}
     *  ({@code J1.m2} for an input, {@code expected J1.m2} for an expectation): a grid of several units and a
     *  variable start before any segment is read, the calendar's week and quarter grid at the first segment. Every day
     *  sits on the calendar's day grid. */
    List<Object> timelineSegments(JsonNode segs, Datatype dt, String field, String site, Grid gran, boolean emptyAllowed) {
        typedSegments(segs, dt, site);
        if (segs.isEmpty() && !emptyAllowed)
            throw new IllegalArgumentException("the timeline value on '" + field + "' carries no segment");
        if (gran.count() != 1)
            throw new Excluded(DroolsCode.TIMELINE_MULTI_PERIOD_GRID.reason("site", site, "count", gran.count(), "granularity", spelling(gran.gran())));
        if (gran.variable())
            throw new Excluded(DroolsCode.TIMELINE_VARIABLE_START_GRID.reason("site", site));
        List<Object> out = new ArrayList<>();
        // the end of the segment before, null before the first; an open end leaves no room for a segment after it
        LocalDate previousTo = null;
        boolean openEnd = false;
        for (JsonNode sj : segs) {
            LocalDate from = openBound(sj, "from", field), to = openBound(sj, "to", field);
            if (from != null && to != null && !from.isBefore(to))
                throw new IllegalArgumentException("segment 'to' on '" + field + "' does not lie after 'from'");
            if (gran.fixed() == null) calendarGridForInput(gran.gran(), site);
            if (from != null && !onGrid(gran, from) || to != null && !onGrid(gran, to))
                throw new IllegalArgumentException("a segment bound on '" + field + "' does not lie on the "
                        + gran.gran() + " grid" + (gran.fixed() == null ? "" : " from " + gran.fixed()));
            if (openEnd || !out.isEmpty() && (from == null || previousTo.isAfter(from)))
                throw new IllegalArgumentException("the timeline segments on '" + field + "' are unordered or overlap");
            previousTo = to;
            openEnd = to == null;
            out.add(segment(from, to, segmentValue(sj.get("value"), field)));
        }
        return out;
    }

    /** Holds the segments {@code segs} of a timeline value at {@code site} to the case format: a list of objects, each
     *  with a value of the datatype {@code dt} ({@link #typed}). The case format writes filled segments only
     *  (RUNTIME-SEMANTICS ch. 3.1), so a segment without a value or with null is a defect of the case. An input holds
     *  its segments here before the input-precision fence ({@link #inputFits}) reads their numbers. */
    static void typedSegments(JsonNode segs, Datatype dt, String site) {
        if (segs == null || !segs.isArray())
            throw new IllegalArgumentException(site + ".timeline: expected a list, got " + segs);
        for (int k = 0; k < segs.size(); k++) {
            String at = site + ".timeline[" + k + "]";
            JsonNode sj = segs.get(k);
            if (!sj.isObject()) throw new IllegalArgumentException(at + ": expected an object, got " + sj);
            if (!sj.has("value")) throw new IllegalArgumentException(at + ": missing field \"value\"");
            typed(sj.get("value"), dt, at + ".value");
        }
    }

    /** The bound {@code field} of the segment {@code seg} of a timeline value, null where the segment leaves it out:
     *  an open bound (RUNTIME-SEMANTICS ch. 3.1, {@code uchar-case-v0}). A bound that is there is a date. */
    private static LocalDate openBound(JsonNode seg, String field, String where) {
        return seg != null && seg.has(field) ? boundDate(seg, field, where) : null;
    }

    /** The grid a temporal member's case values sit on: its facet's granularity and count, its fixed start point,
     *  and whether it rides a timeline with a variable start (RUNTIME-SEMANTICS ch. 3.2). */
    record Grid(Granularity gran, int count, String fixed, boolean variable) {}

    Grid grid(ObjectType ot, String attr) {
        Integer count = Namen.byName(ot.timelineCountMembers(), attr);
        return new Grid(Namen.byName(ot.granularityMembers(), attr), count == null ? 1 : count,
                ot.fixedStartPointOf(attr), model.hasVariableStart(ot, attr));
    }

    /** The period {@code [from, to)} of the member's grid that contains {@code d} (ch. 3.2), as ref lays it: the
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
        if (r.fixed() != null) return FixedStartPoint.period(r.gran(), r.fixed(), d);
        switch (r.gran()) {
            case JAAR:
                return new LocalDate[]{ LocalDate.of(d.getYear(), 1, 1), LocalDate.of(d.getYear() + 1, 1, 1) };
            case MAAND:
                LocalDate from = d.withDayOfMonth(1);
                return new LocalDate[]{ from, from.plusMonths(1) };
            default:
                throw new Excluded(DroolsCode.BUCKET_GRANULARITY_UNSUPPORTED.reason());
        }
    }

    /** ref's loader checks a case timeline on the calendar grid through the calendar bucketing, which has month and
     *  year buckets only (ch. 3.2), and every day sits on the day grid; it refuses the calendar's week and quarter grid
     *  as a v0 limit, in its own sentence; this is that refusal, at position {@code site}. */
    private static void calendarGridForInput(Granularity gran, String site) {
        switch (gran) {
            case JAAR, MAAND, DAG -> { }
            default -> throw new Excluded(DroolsCode.CALENDAR_GRID_UNSUPPORTED.reason("site", site, "granularity", spelling(gran), "granularity", spelling(gran)));
        }
    }

    /** Whether day {@code d} is a wall of the grid {@code gran}: every day of the calendar's day grid is one, and of
     *  another grid the first day of the period that holds it ({@link #bucket}). */
    private static boolean onGrid(Grid gran, LocalDate d) {
        if (gran.fixed() == null && gran.gran() == Granularity.DAG) return true;
        return bucket(gran, d)[0].equals(d);
    }

    /** A granularity in its wire spelling (IR-SPEC ch. 2.2), the word ref's sentences carry. */
    private static String spelling(Granularity gran) {
        return Spelling.GRANULARITY.spelling(gran);
    }

    /** The value of one segment: a number, a constructor value on a timeline of a sum type (RUNTIME-SEMANTICS
     *  ch. 9.4), or a text, which the whole-period qualifier tests per day (ch. 3.9). The analysis computes per period
     *  with numbers and compares constructor values, so a timeline of any other value has no reading here — a
     *  reason-carrying not-carried, never a coercion. */
    Object segmentValue(JsonNode w, String field) {
        Object v = valueToArg(w, field);
        if (v == null || v instanceof BigDecimal || v instanceof ConstructorValue || v instanceof String) return v;
        throw new Excluded(DroolsCode.TIMELINE_VALUE_NON_NUMERIC.reason("attribute", field));
    }

    static LocalDate boundDate(JsonNode seg, String field, String where) {
        JsonNode d = seg == null ? null : seg.get(field);
        if (d == null || !d.isTextual())
            throw new IllegalArgumentException("a segment on '" + where + "' misses a '" + field + "' date");
        // the field name carries BOTH coordinates: a case may hold many timelines and each holds two
        // boundaries, so "onderhoudstarief.from" is the only form a producer can act on
        return caseWireDate(d.asText(), where + "." + field);
    }

    /** One {@code Segment} fact of the generated bean model, built reflectively ({@link Period#bean}) — the Segment
     *  class is a runtime objecttype, unknown at compile time. */
    Object segment(LocalDate from, LocalDate to, Object value) {
        try {
            return Period.bean(Class.forName(FactModelGenerator.PACKAGE + ".Segment", true, cl), from, to, value);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("the generated Segment fact class is missing", e);
        }
    }

    // ---------------- comparison ----------------

    /** The comparison behind {@link DroolsBackend#compare}, on a runner of {@code build} that knows the beans of
     *  {@code session} by the labels of the caller: {@code perLabel} gives the bean of each label, {@code types} its object
     *  type. The runner reads no case input, so a session the caller filled itself compares as one of the replay. */
    static DroolsBackend.Comparison comparison(DroolsBackend.Build build, KieSession session,
                                                   Map<String, Object> perLabel, Map<String, ObjectType> types,
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
    private ObjectType typeOfLabel(JsonNode v, String label) {
        ObjectType ot = labelType.get(label);
        if (ot == null)
            throw new IllegalArgumentException(v.get("kind").asText() + " expectation on the unknown label '" + label
                    + "': no bean of the session carries it, so there is nothing to read");
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
            case "cellValue" -> cell(v, false);
            case "cellEmpty" -> cell(v, true);
            default -> throw new IllegalArgumentException("unknown expectation kind \"" + kind + "\"");
        };
    }

    private String attribute(JsonNode v, boolean emptyExpected) {
        String label = v.get("label").asText();
        ObjectType ot = typeOfLabel(v, label);
        String attr = attributeName(ot, v.get("attribute").asText());
        ownField(ot, attr, false, "reads");
        // a CASE defect: a dimensioned attribute has a value per cell and none of its own (RUNTIME-SEMANTICS ch. 5.8c),
        // and its read-back form is the cellValue or cellEmpty expectation
        if (dimensioned(ot, attr))
            throw new IllegalArgumentException(v.get("kind").asText() + " on the dimensioned attribute '" + attr + "' of "
                    + label + ", whose values are its cells: an expectation on it names a cell");
        // a CASE defect as well: a time-dependent attribute has a value per period and none of its own (RUNTIME-SEMANTICS
        // ch. 5.7 step 3), and its read-back form is the attributeTimeline expectation
        if (temporal(ot, attr))
            throw new IllegalArgumentException(v.get("kind").asText() + " on the time-dependent attribute '" + attr
                    + "' of " + label + ", whose value is a timeline: an expectation on it is an attributeTimeline");
        Object actual = invokeGetter(labelToBean.get(label), Namen.getter(attr));
        if (emptyExpected)
            return actual == null ? null : Msg.drools("PART.mismatch.empty-expected", "actual", actual).text();
        typed(v.get("value"), ot.attribute(attr), "expected " + label + "." + attr);
        Object expected = valueToArg(v.get("value"), attr);
        return equalValue(v.get("value"), expected, actual) ? null
                : Msg.drools("PART.mismatch.attribute", "attribute", attr, "expected", render(expected), "actual",
                        render(actual)).text();
    }

    /** The read-back of one cell of a dimensioned attribute (RUNTIME-SEMANTICS ch. 5.8c): the value the map of its bean
     *  member holds under the key of the cell the expectation's labels name ({@link FactModelGenerator#cellKey}), leeg
     *  where the map holds none. A cell expectation on an attribute without dimensions, and labels that name no cell, are
     *  defects of the case, as an expectation on an attribute the type does not declare is. */
    private String cell(JsonNode v, boolean emptyExpected) {
        String label = v.get("label").asText(), kind = v.get("kind").asText();
        ObjectType ot = typeOfLabel(v, label);
        String attr = attributeName(ot, v.get("attribute").asText());
        ownField(ot, attr, false, "reads");
        if (!dimensioned(ot, attr))
            throw new IllegalArgumentException(kind + " on the attribute '" + attr + "' of " + label
                    + ", which has no dimensions");
        Object actual = cellValue(v, labelToBean.get(label), ot);
        if (emptyExpected)
            return actual == null ? null : Msg.drools("PART.mismatch.cell-empty", "attribute", attr, "labels",
                    cellLabels(v).toString(), "actual", actual).text();
        typed(v.get("value"), ot.attribute(attr), "expected " + label + "." + attr + cellLabels(v));
        Object expected = valueToArg(v.get("value"), attr);
        return equalValue(v.get("value"), expected, actual) ? null
                : Msg.drools("PART.mismatch.cell", "attribute", attr, "labels", cellLabels(v).toString(), "expected",
                        render(expected), "actual", render(actual)).text();
    }

    /** The labels of a cell expectation as the case writes them. */
    private static List<String> cellLabels(JsonNode v) {
        List<String> labels = new ArrayList<>();
        for (JsonNode l : v.get("labels")) labels.add(l.asText());
        return labels;
    }

    /** The value on {@code bean}, of type {@code ot}, of the cell that the cell expectation {@code v} names; null for an
     *  empty cell. Labels that name no cell are a defect of the case. */
    private Object cellValue(JsonNode v, Object bean, ObjectType ot) {
        String attr = attributeName(ot, v.get("attribute").asText());
        List<String> labels = cellLabels(v);
        List<String> key = FactModelGenerator.cellKey(model, ot.dimensionsOf(attr), labels);
        if (key == null)
            throw new IllegalArgumentException(v.get("kind").asText() + " on " + v.get("label").asText() + "." + attr
                    + ": the labels " + labels + " name no cell of the dimensions " + ot.dimensionsOf(attr));
        Object map = bean == null ? null : invokeGetter(bean, Namen.getter(attr));
        return map == null ? null : ((Map<?, ?>) map).get(key);
    }

    /** The §13.3.6 timeline read-back: the doel's {@code List<Segment>} against the pinned segment list, both in
     *  the canonical form of RUNTIME-SEMANTICS ch. 3.1 ({@link #canonical}), then equal length, van/tot EXACT,
     *  waarde scale-safe ({@code compareTo == 0}) or a constructor value by its equality, compared index-for-index. The expectation pins what the
     *  timeline reads on each day, not how the backend or the case divides a run of equal values into segments.
     *  A never-written target (a null field) matches an explicitly empty expectation: both read leeg on every day
     *  (ch. 3.1), exactly as ref and clingo pin it. */
    private String timeline(JsonNode v) {
        String label = v.get("label").asText();
        ObjectType ot = labelType.get(label);
        // both are CASE defects, not uncarried forms — ref rejects the same shapes at load ("expected
        // timeline on %S, which declares no granularity"), so they are hard failures here too, and the
        // unknown-label arm would otherwise reach bucket() with a null granularity (an unnamed NPE).
        if (ot == null)
            throw new IllegalArgumentException("attributeTimeline expectation on the unknown label '" + label + "'");
        String attr = attributeName(ot, v.get("attribute").asText());
        if (!temporal(ot, attr))
            throw new IllegalArgumentException("attributeTimeline expectation on the non-temporal attribute '" + attr + "'");
        ownField(ot, attr, false, "reads");
        List<TimelineSegment> expected = canonical(segments(timelineSegments(v.get("timeline"), ot.attribute(attr), attr,
                "expected " + label + "." + attr, grid(ot, attr), true)));
        Object read = labelToBean.containsKey(label) ? invokeGetter(labelToBean.get(label), Namen.getter(attr)) : null;
        if (read == null)
            return expected.isEmpty() ? null
                    : Msg.drools("PART.mismatch.timeline-absent", "attribute", attr, "expected", renderTl(expected)).text();
        List<TimelineSegment> actual = canonical(segments((List<?>) read));
        boolean equal = actual.size() == expected.size();
        for (int k = 0; equal && k < expected.size(); k++)
            equal = segEqual(expected.get(k), actual.get(k));
        return equal ? null
                : Msg.drools("PART.mismatch.attribute", "attribute", attr, "expected", renderTl(expected), "actual",
                        renderTl(actual)).text();
    }

    private static boolean segEqual(TimelineSegment expectation, TimelineSegment actual) {
        return java.util.Objects.equals(expectation.from(), actual.from())
                && java.util.Objects.equals(expectation.to(), actual.to())
                && equal(expectation.value(), actual.value());
    }

    /** One segment of a timeline as the read-back compares it: a null bound is open, a null value leeg. */
    record TimelineSegment(LocalDate from, LocalDate to, Object value) {}

    /** The {@code Segment} facts of a timeline, in their order, as the read-back compares them. */
    static List<TimelineSegment> segments(List<?> facts) {
        List<TimelineSegment> out = new ArrayList<>();
        for (Object s : facts)
            out.add(new TimelineSegment((LocalDate) segField(s, "van"), (LocalDate) segField(s, "tot"),
                    segField(s, "waarde")));
        return out;
    }

    /** The canonical form of RUNTIME-SEMANTICS ch. 3.1: the maximal runs of equal filled values, gaps omitted, an
     *  open bound as it stands. A leeg segment reads as a gap does, so it goes before the runs are merged and never
     *  joins its two neighbours; so does a segment whose {@code tot} does not lie after its {@code van}. Two
     *  well-formed timelines read alike on every day exactly when their canonical forms are equal. Values merge by
     *  {@link #equal}, the equality the read-back compares them with. */
    static List<TimelineSegment> canonical(List<TimelineSegment> segments) {
        List<TimelineSegment> filled = new ArrayList<>();
        for (TimelineSegment s : segments)
            if (s.value() != null && (s.from() == null || s.to() == null || s.to().isAfter(s.from()))) filled.add(s);
        filled.sort(Comparator.comparing(TimelineSegment::from, Comparator.nullsFirst(Comparator.naturalOrder())));
        List<TimelineSegment> out = new ArrayList<>();
        for (TimelineSegment s : filled) {
            TimelineSegment last = out.isEmpty() ? null : out.get(out.size() - 1);
            if (last != null && last.to() != null && last.to().equals(s.from()) && equal(last.value(), s.value()))
                out.set(out.size() - 1, new TimelineSegment(last.from(), s.to(), last.value()));
            else out.add(s);
        }
        return out;
    }

    /** One field of a {@code Segment} fact, read through the runtime's property bridge (the class is a runtime
     *  objecttype — no compile-time type to cast to). */
    private static Object segField(Object seg, String field) { return RegelSpraakRuntime.get(seg, field); }

    private static String renderTl(List<TimelineSegment> segs) {
        if (segs.isEmpty()) return Msg.drools("PART.value.empty-timeline").text();
        List<String> out = new ArrayList<>();
        for (TimelineSegment s : segs)
            out.add("[" + bound(s.from(), "-inf") + "," + bound(s.to(), "+inf") + ")=" + render(s.value()));
        return String.join(" ", out);
    }

    /** A bound of a segment as a verdict line writes it, {@code open} for an open one (a null bound). */
    private static String bound(Object bound, String open) {
        return bound == null ? open : bound.toString();
    }

    private String characteristic(JsonNode v, boolean presentExpected) {
        ObjectType ot = typeOfLabel(v, v.get("label").asText());
        String characteristic = characteristicName(ot, v.get("characteristic").asText());
        // a CASE defect: a time-dependent characteristic holds per period and has no standing of its own
        // (RUNTIME-SEMANTICS ch. 5.7 step 3, 8.4)
        if (Namen.containsName(ot.characteristicGranularityMembers(), characteristic))
            throw new IllegalArgumentException(v.get("kind").asText() + " on the time-dependent characteristic '"
                    + characteristic + "' of " + v.get("label").asText() + ", which holds per period and has no"
                    + " standing of its own");
        ownField(ot, characteristic, true, "reads");
        Object read = invokeGetter(labelToBean.get(v.get("label").asText()), Namen.getter(characteristic));
        boolean present = Boolean.TRUE.equals(read);
        return present == presentExpected ? null
                : Msg.drools("PART.mismatch.characteristic", "characteristic", v.get("characteristic").asText(),
                        "expected", presence(presentExpected), "actual", presence(present)).text();
    }

    /** The §9.5 consistency check. A {@code consistent} expectation demands NO Inconsistency fact at all
     *  (unchanged presence semantics). An {@code inconsistent} expectation is verified against the pinned §9.5
     *  KEY (the {@code rule}) — a matching Inconsistency fact must exist — and, when the expectation pins a
     *  {@code label}, on that SUBJECT, for every key kind, as ref matches it ({@code ref/lib/case.ml}, the
     *  {@code Inconsistent} arm: rule and exact subject label on the same fact; card drools-consistentie-label). A
     *  rule-name key that passed on any fact from its rule would let a §9.5 deposit on the wrong subject pass, such as
     *  an allocation error laid on the source instead of each recipient ({@code jutterswet-rangmaximering}). An
     *  expectation carrying no rule and no code falls back to bare presence. A pinned {@code code} (card
     *  channel-fact-codes, RUNTIME-SEMANTICS ch. 5.6, 5.7b) must be among the codes of a fact that matches rule and
     *  label: ref keeps one fact per rule × subject with the codes deposited for it, this backend one fact per deposit
     *  with its own codes, so a code on any of the matching facts is a code of ref's one fact. */
    private String consistency(KieSession session, boolean inconsistentExpected, JsonNode v) {
        List<Object> facts = inconsistencies(session);
        if (!inconsistentExpected)
            return facts.isEmpty() ? null : Msg.drools("PART.mismatch.inconsistent-found", "facts",
                    renderIncs(facts, false)).text();
        String rule = v.has("rule") && !v.get("rule").isNull() ? v.get("rule").asText() : null;
        String label = v.has("label") && !v.get("label").isNull() ? v.get("label").asText() : null;
        String code = v.hasNonNull("code") ? v.get("code").asText() : null;
        if (rule == null && label != null)
            throw new IllegalArgumentException("an inconsistent label pin requires a rule pin: " + v);
        if (facts.isEmpty())
            return Msg.drools("PART.mismatch.consistent-found", "key", keyText(rule, label, code)).text();
        if (rule == null && code == null) return null;   // presence-only expectation, satisfied by any fact
        for (Object f : facts) {
            if (rule != null && !sameRule(rule, (String) invokeGetter(f, "getRule"))) continue;
            if (label != null && !label.equals(beanLabel(invokeGetter(f, "getSubject")))) continue;
            if (code == null || ((List<?>) invokeGetter(f, "getCodes")).contains(code)) return null;
        }
        return Msg.drools("PART.mismatch.inconsistent-not-found", "key", keyText(rule, label, code), "facts",
                renderIncs(facts, code != null)).text();
    }

    /** Every §9.5 Inconsistency fact the session holds, in the order of its working memory: {@link #consistency} looks
     *  for a fact that matches and {@link #renderIncs} sorts what it shows, so no verdict and no line depends on it. */
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
    static String withoutVersion(String name) {
        int i = name.lastIndexOf('#');
        if (i >= 2 && name.charAt(i - 1) == ' ' && i + 1 < name.length()
                && name.substring(i + 1).chars().allMatch(c -> c >= '0' && c <= '9'))
            return name.substring(0, i - 1);
        return name;
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
            out.add(invokeGetter(f, "getRule") + " @ " + beanLabel(invokeGetter(f, "getSubject"))
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
        if (v.hasNonNull("labels")) b.append(cellLabels(v));
        return b.toString();
    }

    /** The expected side, rendered as {@link #actual} renders the actual one. */
    private String expected(JsonNode v) {
        return switch (v.get("kind").asText()) {
            case "attributeValue" -> render(valueToArg(v.get("value"), v.get("attribute").asText()));
            case "attributeEmpty" -> render(null);
            case "attributeTimeline" -> {
                List<String> segs = new ArrayList<>();
                for (JsonNode s : v.get("timeline"))
                    segs.add("[" + (s.has("from") ? s.get("from").asText() : "-inf") + ","
                            + (s.has("to") ? s.get("to").asText() : "+inf") + ")="
                            + render(valueToArg(s.get("value"), v.get("attribute").asText())));
                yield segs.isEmpty() ? Msg.drools("PART.value.empty-timeline").text() : String.join(" ", segs);
            }
            case "cellValue" -> render(valueToArg(v.get("value"), v.get("attribute").asText()));
            case "cellEmpty" -> render(null);
            case "characteristicPresent" -> presence(true);
            case "characteristicAbsent" -> presence(false);
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
        ObjectType ot = label == null ? null : labelType.get(label);
        return switch (v.get("kind").asText()) {
            case "attributeValue", "attributeEmpty" -> render(bean == null ? null
                    : invokeGetter(bean, Namen.getter(attributeName(ot, v.get("attribute").asText()))));
            case "attributeTimeline" -> {
                Object read = bean == null ? null
                        : invokeGetter(bean, Namen.getter(attributeName(ot, v.get("attribute").asText())));
                yield read == null ? Msg.drools("PART.value.empty-timeline").text() : renderTl(segments((List<?>) read));
            }
            case "cellValue", "cellEmpty" -> render(cellValue(v, bean, ot));
            case "characteristicPresent", "characteristicAbsent" -> presence(bean != null && Boolean.TRUE.equals(
                    invokeGetter(bean, Namen.getter(characteristicName(ot, v.get("characteristic").asText())))));
            default -> {
                List<Object> facts = inconsistencies(session);
                yield facts.isEmpty() ? "consistent" : "inconsistent (" + renderIncs(facts, v.hasNonNull("code")) + ")";
            }
        };
    }

    // ---------------- value helpers ----------------

    /** Value equality of the read-back: numbers by value, any other value, a constructor value among them, by its
     *  equality. */
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
     *  deliberately ("Beschadigd" against a declared "beschadigd") and that is what this arm exists for (card
     *  `drools-enumlabel-lezer`). The expectation NODE decides, not the Java type: a Tekst value
     *  arrives as the same String and keeps the exact equality §13.3.3.4 gives it. */
    private static boolean equalValue(JsonNode expectedNode, Object expectation, Object actual) {
        if (expectedNode != null && expectedNode.has("enumeration"))
            return expectation instanceof String a && actual instanceof String b
                    && Namen.norm(a).equals(Namen.norm(b));
        return equal(expectation, actual);
    }

    /** The Java value a uchar case value node denotes, held to its datatype before ({@link #typed}): the scalar the bean
     *  setter or the global takes. A rational lexeme, which this backend does not carry, is a reason-carrying
     *  {@link Excluded}. */
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
        // a constructor value of a sum type (RUNTIME-SEMANTICS ch. 9.1), its tag as the case spells it; that it is a
        // declared constructor with the payload it declares is asked against the declaration (inputFits)
        if (w.has("constructor"))
            return new ConstructorValue(w.get("constructor").asText(), valueToArg(w.get("payload"), field));
        throw new IllegalStateException("valueToArg: the value " + w + " on '" + field + "' is no form the datatype check"
                + " lets through");
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
    public static final class CaseDateFault extends IllegalArgumentException {
        private static final long serialVersionUID = 1L;

        private final String field;
        private final String lexeme;

        CaseDateFault(String field, String lexeme, Msg message) {
            super(message.text());
            this.field = field;
            this.lexeme = lexeme;
        }

        public String code() {
            return "EF-003";
        }

        /** The parameters of the kind (ch. 5.7b): the field of the case input and the lexeme it holds. */
        public java.util.Map<String, String> params() {
            return java.util.Map.of("field", field, "lexeme", lexeme);
        }
    }

    /**
     * The input-precision fence (RUNTIME-SEMANTICS ch. 2.4, 5.7 step 3, EF-004): a case value {@code w} on the place
     * {@code place}, an attribute or a parameter of datatype {@code dt}, whose exact value its declared decimals do not
     * hold is refused while the case is read, as the reference interpreter refuses it, so that a bounded attribute holds
     * a decimal that fits its declaration wherever the session reads it. A timeline is held to it segment by segment. A
     * value of another datatype, the unbounded form, which holds every value (ch. 2.4 point 3), and leeg pass; a rational
     * lexeme is held to it too, before the refusal of a rational value ({@link #valueToArg}). Public, with
     * {@link InputPrecisionFault}, so that a consumer that binds a parameter or attribute itself holds the value to the
     * same fence and can tell the refusal from a defect.
     */
    public static void inputFits(JsonNode w, Datatype dt, String place) {
        if (w == null || w.isNull() || dt == null) return;
        if (dt instanceof Datatype.SumType s) {
            if (w.has("timeline")) {
                for (JsonNode segment : w.get("timeline")) inputFits(segment.get("value"), dt, place);
                return;
            }
            constructorFits(w, s, place);
            return;
        }
        if (!dt.isNumberLike() || dt.unbounded()) return;
        if (w.has("timeline")) {
            for (JsonNode segment : w.get("timeline")) inputFits(segment.get("value"), dt, place);
            return;
        }
        if (!w.has("number")) return;
        String lexeme = w.get("number").asText();
        int slash = lexeme.indexOf('/');
        Breuk q = slash < 0 ? Breuk.van(new BigDecimal(lexeme))
                : Breuk.van(new java.math.BigInteger(lexeme.substring(0, slash)), new java.math.BigInteger(lexeme.substring(slash + 1)));
        int decimals = dt instanceof Datatype.Numeric n ? n.decimals() : ((Datatype.Percentage) dt).decimals();
        if (!q.vermenigvuldigen(Breuk.van(java.math.BigInteger.TEN.pow(decimals), java.math.BigInteger.ONE)).noemer()
                .equals(java.math.BigInteger.ONE))
            throw new InputPrecisionFault(place, q.isTerminerend() ? q.naarDecimal().stripTrailingZeros().toPlainString()
                    : q.teller() + "/" + q.noemer(), decimals);
    }

    /** A constructor value of the case against the sum type {@code s} it is a value of (RUNTIME-SEMANTICS ch. 9.4, the
     *  WF-059/WF-018 backstop widened, as the reference interpreter's case loader holds it): its tag names a declared
     *  constructor, it carries a payload exactly where that constructor is unary, and the payload fits the declared
     *  payload datatype. A defect of the case, so a hard error. */
    private static void constructorFits(JsonNode w, Datatype.SumType s, String place) {
        if (!w.has("constructor"))
            throw new IllegalArgumentException(place + ": a value of the sum type '" + s.domain()
                    + "' is a constructor value, not " + w);
        String tag = w.get("constructor").asText();
        Datatype.Constructor declared = null;
        for (Datatype.Constructor c : s.constructors())
            if (Namen.norm(c.name()).equals(Namen.norm(tag))) declared = c;
        if (declared == null)
            throw new IllegalArgumentException(place + ": \"" + tag + "\" is not a declared constructor of the sum type '"
                    + s.domain() + "'");
        if ((declared.payload() == null) != (w.get("payload") == null || w.get("payload").isNull()))
            throw new IllegalArgumentException(place + ": the " + (declared.payload() == null ? "nullary" : "unary")
                    + " constructor \"" + tag + "\" " + (declared.payload() == null ? "carries a payload" : "carries no payload"));
        if (declared.payload() != null) {
            typed(w.get("payload"), declared.payload(), place + ".payload");
            inputFits(w.get("payload"), declared.payload(), place);
        }
    }

    /** The refusal of a case value its declaration cannot hold (RUNTIME-SEMANTICS ch. 5.7b, EF-004): a defect of the
     *  case, so a hard error and never an {@link Excluded}, as {@link CaseDateFault} is; a type of its own so that the
     *  red path can name its kind ({@link #faultKinds}). */
    public static final class InputPrecisionFault extends IllegalArgumentException {
        private static final long serialVersionUID = 1L;

        private final String place;
        private final String value;
        private final int decimals;

        InputPrecisionFault(String place, String value, int decimals) {
            super(Msg.drools("EF-004", "place", place, "value", value, "decimals", decimals).text());
            this.place = place;
            this.value = value;
            this.decimals = decimals;
        }

        public String code() {
            return "EF-004";
        }

        /** The parameters of the kind (ch. 5.7b): the place of the input, its exact value and the declared decimals. */
        public java.util.Map<String, String> params() {
            java.util.Map<String, String> p = new LinkedHashMap<>();
            p.put("place", place);
            p.put("value", value);
            p.put("decimals", Integer.toString(decimals));
            return p;
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
     *  milliseconds and {@code .1234} as 123.4. Without a fence the shared interpreter/ASP case reader reads
     *  those same digits differently ({@code .1} as ONE millisecond, {@code .100000} as a hundred THOUSAND), and
     *  the literal reader refuses them outright: one wire, three answers. Inside the schema's fence all three
     *  agree, so each reader holds that fence itself, and a case reaching a reader by another route (a generator, a hand run, a second producer) is
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
        if (dt.isText() || dt instanceof Datatype.Enumeration) return String.class;
        if (dt instanceof Datatype.BooleanType) return Boolean.class;
        if (dt instanceof Datatype.DateInDays) return LocalDate.class;
        if (dt instanceof Datatype.DateTime) return LocalDateTime.class;
        return Object.class;
    }

    static void call(Object bean, String method, Class<?> type, Object value) {
        try { bean.getClass().getMethod(method, type).invoke(bean, value); }
        catch (ReflectiveOperationException e) {
            throw new RuntimeException("could not call the setter " + method + "(" + type.getSimpleName() + ") on "
                    + bean.getClass().getSimpleName(), e);
        }
    }

    static Object invokeGetter(Object bean, String getter) {
        for (String mth : new String[]{ getter, "is" + getter.substring(3) }) {
            try { return bean.getClass().getMethod(mth).invoke(bean); }
            catch (NoSuchMethodException ignore) { /* try next form */ }
            catch (ReflectiveOperationException e) { throw new RuntimeException("could not read " + mth, e); }
        }
        throw new IllegalStateException("no getter " + getter + " on " + bean.getClass().getSimpleName());
    }

    private static String render(Object o) { return o == null ? Msg.drools("PART.value.empty").text() : o.toString(); }

    /** A characteristic's standing as the report and a mismatch line write it. */
    private static String presence(boolean present) {
        return Msg.drools(present ? "PART.value.present" : "PART.value.absent").text();
    }
}
