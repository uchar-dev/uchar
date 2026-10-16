package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import dev.uchar.regelspraak.engine.ScenarioResultaat;
import dev.uchar.regelspraak.loader.IrLoader;
import dev.uchar.regelspraak.kern.VastStartpunt;
import dev.uchar.regelspraak.model.Datatype;
import dev.uchar.regelspraak.ir.Expr;
import dev.uchar.regelspraak.ir.PrioritizedDefaults;
import dev.uchar.regelspraak.ir.ResolvedRegel;
import dev.uchar.regelspraak.ir.Spelling;
import dev.uchar.regelspraak.model.RegelSpraakModel;
import dev.uchar.regelspraak.model.RegelSpraakModel.Granulariteit;
import dev.uchar.regelspraak.model.RegelSpraakModel.Objecttype;
import dev.uchar.regelspraak.runtime.RegelSpraakRuntime;
import dev.uchar.regelspraak.schema.FactModelGenerator;
import dev.uchar.regelspraak.transpiler.kern.Namen;

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
 * read, and the §9.5 Inconsistentie channel for consistent/inconsistent.
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
     *  {@link CaseInvoer.Rolweigering} is one and names the role link it refuses; with the constructor package-private,
     *  no class outside this package extends it. */
    public static class Excluded extends RuntimeException {
        Excluded(String reason) { super(reason); }
    }

    /** The outcome of one golden: PLAYED (with a per-case pass/fail list) or EXCLUDED (with a reason). */
    public record Result(String status, String reason, List<String> caseFailures, int casesPlayed) {
        public static Result excluded(String reason) { return new Result("EXCLUDE", reason, List.of(), 0); }
        public static Result played(int n, List<String> failures) {
            return new Result(failures.isEmpty() ? "PLAYED" : "FAIL", "", failures, n);
        }
    }

    private final RegelSpraakModel model;
    private final KieBase kieBase;
    private final ClassLoader cl;
    /** §9.8 the dagsoort predicates the DRL-text branch declares as globals (name -> predicate). The exec-model
     *  branch wires those predicates straight into its lambdas and needs none, so it passes an empty map. */
    private final Map<String, java.util.function.Predicate<LocalDate>> dagsoortGlobalen;
    /** The build this runner fires: a branch puts its derivations on the beans only after a fire
     *  ({@link DroolsBackend#zetAfgeleideWaarden}), and that needs the analysis that made them. */
    private DroolsBackend.Build bouw;
    /** The hash of the document this runner built, which names the files of the seam in {@link #vuurTotFixpunt}; null
     *  without the property {@value Sessiedump#EIGENSCHAP}, and then the seam does nothing. */
    private String documenthash;

    /** The members of the model without a bean field of their own ({@link FactModelGenerator#verdringingen}): a
     *  case that sets or reads one is refused by name, since the replay would reach the field of the declaration
     *  that took the property. */
    private final List<FactModelGenerator.Verdringing> verdringingen;

    private CaseRunner(RegelSpraakModel model, KieBase kieBase, ClassLoader cl,
                       Map<String, java.util.function.Predicate<LocalDate>> dagsoortGlobalen) {
        this.model = model; this.kieBase = kieBase; this.cl = cl; this.dagsoortGlobalen = dagsoortGlobalen;
        this.verdringingen = FactModelGenerator.verdringingen(model);
    }

    /** A runner of {@code build} that replays no case, whose readers of case values, dates, timelines and declared
     *  member names {@link CaseInvoer} shares with the comparison. */
    static CaseRunner lezer(DroolsBackend.Build build) {
        CaseRunner runner = new CaseRunner(build.model(), build.kieBase(), build.beanLoader(), build.dagsoortGlobalen());
        runner.bouw = build;
        return runner;
    }

    /** Refuses a case that sets or reads ({@code handeling}) the attribute or, with {@code kenmerk}, the
     *  characteristic {@code naam} of an instance of {@code ot} when that member has no bean field of its own. */
    void eigenVeld(Objecttype ot, String naam, boolean kenmerk, String handeling) {
        String reden = ot == null ? null : FactModelGenerator.verdrongenLid(verdringingen, ot, naam, kenmerk);
        if (reden != null) throw new Excluded(reden + "; de case " + handeling + " het");
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
    public static Result viaDrlTekst(Path irPath, List<Path> casePaths) {
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

    private static Result replay(Path irPath, List<Path> casePaths, DroolsBackend.Branch tak) {
        try {
            CaseRunner runner = build(irPath, tak);
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
            return Result.excluded(e.getMessage());
        } catch (IrLoader.Refusal e) {
            return Result.excluded("loader: " + e.getMessage());
        } catch (java.io.IOException e) {
            throw new RuntimeException("could not read " + irPath + ": " + e.getMessage(), e);
        }
    }

    /** The scenario report of a golden's replay: one {@link ScenarioResultaat} per case, from the same exec-model
     *  replay the gate plays, carrying beside the verdict what the source toolchain's scenario runner reported on top
     *  of a comparison — per expectation the expected and the actual value, the rules that fired with the labels
     *  they bound, and per lowered construct whether it fired. A document or a case form this backend does not
     *  carry (the replay's EXCLUDE, a construct the transpiler does not lower among them) is status MARKER with its
     *  reason, so accepted-but-inert never reads as a pass. A case the replay cannot play, one whose play raises an
     *  exception, is FAIL with that exception as its reason, and the report goes on with the next case. A red-path
     *  case is no scenario and is skipped, as the replay skips it. */
    public static List<ScenarioResultaat> rapport(Path irPath, List<Path> casePaths) {
        List<Path> paden = new ArrayList<>();
        List<JsonNode> cases = new ArrayList<>();
        for (Path cp : casePaths) {
            JsonNode c;
            try { c = JSON.readTree(Files.readString(cp)); }
            catch (java.io.IOException e) { throw new RuntimeException("could not read " + cp + ": " + e.getMessage(), e); }
            if (c.has("expectedFault")) continue;
            paden.add(cp);
            cases.add(c);
        }
        List<ScenarioResultaat> uit = new ArrayList<>();
        CaseRunner runner;
        try {
            runner = build(irPath);
        } catch (Excluded e) {
            for (Path p : paden) uit.add(marker(p, e.getMessage()));
            return uit;
        } catch (IrLoader.Refusal e) {
            for (Path p : paden) uit.add(marker(p, "loader: " + e.getMessage()));
            return uit;
        } catch (java.io.IOException e) {
            throw new RuntimeException("could not read " + irPath + ": " + e.getMessage(), e);
        }
        for (int i = 0; i < paden.size(); i++) {
            String naam = paden.get(i).getFileName().toString();
            Waarneming w = new Waarneming(new DroolsBackend.Waarnemer(runner::labelVanBean));
            String fout;
            try {
                fout = runner.runCase(naam, cases.get(i), w);
            } catch (Excluded e) {
                uit.add(marker(paden.get(i), e.getMessage()));
                continue;
            } catch (RuntimeException e) {
                // The replay calls this the golden's ERROR and stops; a report of several cases ends only this one,
                // so the cases beside it keep their verdicts.
                uit.add(onspeelbaar(paden.get(i), e));
                continue;
            }
            uit.add(new ScenarioResultaat(naam, fout == null ? ScenarioResultaat.Status.PASS : ScenarioResultaat.Status.FAIL,
                    List.copyOf(w.verwachtingen), new ScenarioResultaat.Trace(w.vuren.vuringen()),
                    DroolsBackend.dekking(runner.bouw, w.vuren.vormen()), fout == null ? "" : fout));
        }
        return uit;
    }

    private static ScenarioResultaat marker(Path casePad, String reden) {
        return new ScenarioResultaat(casePad.getFileName().toString(), ScenarioResultaat.Status.MARKER, List.of(),
                new ScenarioResultaat.Trace(List.of()), List.of(), reden);
    }

    /** A case whose play raised {@code e}: FAIL, with the exception in the words of the replay's ERROR line
     *  ({@link DroolsBackendMain}) on one line, and no verdict, firing or coverage, because the case did not finish. */
    private static ScenarioResultaat onspeelbaar(Path casePad, RuntimeException e) {
        String naam = casePad.getFileName().toString();
        String melding = String.valueOf(e.getMessage()).replace('\n', ' ');
        return new ScenarioResultaat(naam, ScenarioResultaat.Status.FAIL, List.of(),
                new ScenarioResultaat.Trace(List.of()), List.of(),
                naam + ": ERROR " + e.getClass().getSimpleName() + ": " + melding);
    }

    /** What one case's replay reports beside its verdict: what the session fired, through the backend's entry as a
     *  consumer records it, with the case labels of this runner, and per expectation its verdict. */
    private static final class Waarneming {
        final DroolsBackend.Waarnemer vuren;
        final List<ScenarioResultaat.VerwachtingResultaat> verwachtingen = new ArrayList<>();

        Waarneming(DroolsBackend.Waarnemer vuren) {
            this.vuren = vuren;
        }
    }

    /** The witness seam of {@code backends/drools/src/test}: the beans one case's INPUT produces, before any
     *  rule fires. Every gate observes this layer only through post-fire expectation matches, so what an input
     *  BECAME has no other observable surface: the bucket a scalar landed in, the segments a timeline placed on
     *  a {@code List<Segment>} member, or WHICH LEG of a §13.3.9.2 reciprocal pair a role filled — each could be
     *  wrong and still fold to the right total, passing unseen. Roles are therefore set here too, in the replay's
     *  own order (attributes, then roles), so the mirrored self-pair fill is assertable bean-by-bean. Globals
     *  stay out: they live on a {@link KieSession}, which is fire's concern, not this layer's. The beans are those of
     *  the backend's entry ({@link CaseInvoer}), which every replay reads its case through. */
    static Map<String, Object> invoerBeans(Path irPath, JsonNode c) throws java.io.IOException {
        return new LinkedHashMap<>(CaseInvoer.van(build(irPath).bouw, c).beans());
    }

    /** One case's input beans plus every fact the session holds AFTER fire — the §13.4.6 witness seam of
     *  {@code backends/drools/src/test}. A MINTED object carries no case label (ref mints into the reserved
     *  {@code "::"} namespace, which the case loader refuses as a label), so no expectation kind the case
     *  schema can express names it: every corpus observation of a creation is INDIRECT, through a fold or a
     *  navigated read on a labelled input. Neither other seam reaches it either — {@link #invoerBeans} stops
     *  before any rule fires, {@link #speelCase} returns only the comparator's verdict. So THAT the mint
     *  exists, WHICH members its toekenningen filled, and that BOTH ends of its back-link were written have
     *  no other observable surface, while each could be wrong and still leave the pinned total right (a mint
     *  reachable one way only; a member written on the subject instead of the mint). */
    record NaFire(Map<String, Object> invoer, List<Object> feiten) {}

    static NaFire naFire(Path irPath, JsonNode c) throws java.io.IOException {
        CaseRunner runner = build(irPath);
        runner.rekendatum = asOfDatum(c);
        ClassLoader vorige = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(runner.cl);
        KieSession sessie = runner.kieBase.newKieSession();
        try {
            CaseInvoer invoer = runner.leesInvoer(c);
            runner.zetGlobals(sessie, c);
            DroolsBackend.voegIn(runner.bouw, sessie, invoer.invoer());
            String geenFixpunt = runner.vuurTotFixpunt(sessie);
            // The sister runCase path reports the cap as a case failure; this seam has no verdict to return, so
            // a run that never reached a fixpoint must not hand back a half-fired fact base as if it had settled.
            if (geenFixpunt != null) throw new IllegalStateException(geenFixpunt);
            return new NaFire(new LinkedHashMap<>(runner.labelNaarBean), new ArrayList<>(sessie.getObjects()));
        } finally {
            sessie.dispose();
            Thread.currentThread().setContextClassLoader(vorige);
        }
    }

    /** The comparator seam of {@code backends/drools/src/test}: one authored in-memory case through the FULL
     *  replay (build, inputs, fire, compare), returning the per-case failure line — null on a match. The
     *  read-back comparator has no other directly observable surface: gate 12 pins post-fire values on corpus
     *  cases whose pinned lists happen to match segment-for-segment, so a comparator that ignores waarde or
     *  order survives the whole gate set (lens 1 proved it by mutation) — its verdicts are asserted here. */
    static String speelCase(Path irPath, JsonNode c) throws java.io.IOException {
        return build(irPath).runCase("case", c);
    }

    /** The same comparator seam through EITHER transpiler branch, so one authored document can be replayed by
     *  both and their verdicts compared row by row. It exists for the same reason {@link #viaExecModel} does:
     *  a witness that wants both branches on ONE in-memory case would otherwise have to write the document to
     *  disk twice and read two {@link Result}s, which reports a whole golden's status where the row wants a
     *  single cell's verdict. Package-private, no production caller. */
    static String speelCase(Path irPath, JsonNode c, boolean viaDrl) throws java.io.IOException {
        return build(irPath, viaDrl ? DroolsBackend.Branch.DRL_TEXT : DroolsBackend.Branch.EXEC_MODEL).runCase("case", c);
    }

    /** The same comparator seam with the branch named by its value. Package-private, no production caller. */
    static String speelCase(Path irPath, JsonNode c, DroolsBackend.Branch tak) throws java.io.IOException {
        return build(irPath, tak).runCase("case", c);
    }

    /** The measuring seam of the harnesses and witnesses in the test tree, first made for the trial of card
     *  drools-logische-afleiding (acceptance (e)): a runner of {@code irPath} on {@code tak} whose KieBase is built
     *  once, so that a harness in the test tree can fire one case many times and time the fire alone. The replay
     *  entries cannot serve: they build per call and hand back a verdict, not the firings or the session.
     *  Package-private, no production caller, and no gate runs it. */
    static CaseRunner voorMeting(Path irPath, DroolsBackend.Branch tak) throws java.io.IOException {
        return build(irPath, tak);
    }

    /** What a measuring harness does with one session that {@link #metSessie} set up: fire it and measure it as it
     *  chooses. {@code labels} maps each case label to its input bean. */
    @FunctionalInterface
    interface Sessiemeting<T> {
        T meet(DroolsBackend.Build bouw, KieSession sessie, Map<String, Object> labels);
    }

    /** Sets up case {@code c} as {@link #runCase} does, in a fresh session with the bean loader as the thread's context
     *  class loader, and hands that session, not yet fired, to {@code meting}; the session is disposed afterwards. The
     *  seam of the measuring harnesses of card tms-omschakeling, the scale test (point 9) and the measurement over the
     *  corpus (point 7), which fire through {@link DroolsBackend#vuur} as a consumer does and time the fire and the
     *  write-back apart, where the replay's own fire times neither. Package-private, no production caller, and no gate
     *  runs it. */
    <T> T metSessie(JsonNode c, Sessiemeting<T> meting) {
        labelNaarBean.clear();
        labelType.clear();
        rekendatum = asOfDatum(c);
        ClassLoader vorige = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(cl);
        KieSession sessie = kieBase.newKieSession();
        try {
            CaseInvoer invoer = leesInvoer(c);
            zetGlobals(sessie, c);
            DroolsBackend.voegIn(bouw, sessie, invoer.invoer());
            return meting.meet(bouw, sessie, java.util.Collections.unmodifiableMap(labelNaarBean));
        } finally {
            sessie.dispose();
            Thread.currentThread().setContextClassLoader(vorige);
        }
    }

    /** Build {@code irPath} on the exec-model branch. Throws {@link Excluded} / {@link IrLoader.Refusal} for any
     *  form the v0 backend does not carry. */
    private static CaseRunner build(Path irPath) throws java.io.IOException {
        return build(irPath, DroolsBackend.Branch.EXEC_MODEL);
    }

    /** As above, with {@code tak} choosing WHICH rendering of the analysis produces the KieBase: the executable
     *  model, or the DRL text. The whole prelude — load,
     *  refuse, beans, contract, lowering with every side table, coverage check, KieBase — is
     *  {@link DroolsBackend#build(Path, DroolsBackend.Branch)}, the backend's public entry, so the green replay,
     *  the red probe and every witness seam build exactly as a consumer of the backend does. */
    private static CaseRunner build(Path irPath, DroolsBackend.Branch tak) throws java.io.IOException {
        DroolsBackend.Build b = DroolsBackend.build(irPath, tak);
        CaseRunner runner = new CaseRunner(b.model(), b.kieBase(), b.beanLoader(), b.dagsoortGlobalen());
        runner.bouw = b;
        if (Sessiedump.NAADMAP != null) runner.documenthash = Sessiedump.hash(irPath);
        return runner;
    }

    // ---------------- the red path (corpus/rood, gate 8's Drools leg) ----------------

    /** What the v0 Drools backend OBSERVES when it replays one {@code expectedFault} case, by exception TYPE: FAULT
     *  (any runtime exception after the build other than an {@link Excluded}, at fire or while the case is read, a
     *  crash included, with its full cause chain as the text a pinned fragment is matched against; and a run that
     *  hits the fire cap without a fixpoint, which raises nothing), EXCLUDE (a form the backend does not carry — a
     *  loader refusal, or an {@link Excluded} from the build or from the case reader — with the reason), or NOFAULT
     *  (the case ran to a model — the backend does not enforce this red-path constraint, so its form is not
     *  Drools-carried). */
    public record RoodResult(String status, String detail) {}

    /** Replay a single {@code corpus/rood} case and report what the backend observes. The split is by exception
     *  TYPE, not by phase. A loader refusal, or an {@link Excluded} from the build or from a case-input form, is an
     *  EXCLUDE. An I/O failure, and any other exception while the IR is loaded and the KieBase built, leaves this
     *  method and the CLI reports it as ERROR. Every other runtime exception after the build is a FAULT: a fault
     *  AT fire (a division by zero, a fixpoint failure), a named refusal while the case is read (a date lexeme
     *  that names no day, corpus/rood/asofdate-kalender), and equally a CRASH while the case is read — measured
     *  2026-09-23: meldwaarde written as a boolean gives "IllegalArgumentException: argument type mismatch", an
     *  undeclared characteristic a failed setter call. A run without a fixpoint is a FAULT too, and raises no
     *  exception: on the exec-model branch, which this path drives, it says what {@link DroolsBackend#vuur} says,
     *  "creation budget exhausted …" at the creation budget or "geen fixpunt: het vuurplafond van … vuringen is
     *  bereikt, …" at the fire cap, and two writers of one attribute that disagree are a fault at the fixpoint, which
     *  the write-back throws. Gate 8 therefore pins only
     *  this backend's own sentence, never platform text, since a pin on a JDK message would pin a crash; for a
     *  refusal while the case is read, that sentence names position and lexeme. The same red path on either branch is
     *  {@link #runRood(Path, Path, DroolsBackend.Branch)}. */
    public static RoodResult runRood(Path irPath, Path casePath) {
        return runRood(irPath, casePath, DroolsBackend.Branch.EXEC_MODEL);
    }

    /** {@link #runRood(Path, Path)} on the branch {@code tak}: the red path of either rendering of the analysis, so that
     *  a witness can demand the same {@link RoodResult} of both branches on every red case (card
     *  drl-teksttak-op-truth-maintenance, step 6b). Gate 8's Drools leg ({@code DroolsRoodMain}) drives the exec-model
     *  branch through the entry above. */
    public static RoodResult runRood(Path irPath, Path casePath, DroolsBackend.Branch tak) {
        CaseRunner runner;
        try {
            runner = build(irPath, tak);
        } catch (Excluded e) {
            return new RoodResult("EXCLUDE", e.getMessage());
        } catch (IrLoader.Refusal e) {
            return new RoodResult("EXCLUDE", "loader: " + e.getMessage());
        } catch (java.io.IOException e) {
            throw new RuntimeException("could not read " + irPath + ": " + e.getMessage(), e);
        }
        try {
            JsonNode c = JSON.readTree(Files.readString(casePath));
            String fault = runner.fireRood(c);
            return fault == null
                    ? new RoodResult("NOFAULT", "ran to a model without faulting")
                    : new RoodResult("FAULT", fault);
        } catch (Excluded e) {
            return new RoodResult("EXCLUDE", e.getMessage());
        } catch (java.io.IOException e) {
            throw new RuntimeException("could not read " + casePath + ": " + e.getMessage(), e);
        } catch (RuntimeException e) {
            // a fault raised while loading inputs or firing rules — a division by zero surfaces here, wrapped by
            // Drools; the whole cause chain is the report so the pinned fragment can match the deepest message.
            return new RoodResult("FAULT", faultText(e));
        }
    }

    /** Fire one red-path case: load its world, fire the rules, and return the fault text if the run does not
     *  reach a fixpoint. Returns {@code null} when the run completes cleanly (a NOFAULT — the backend produced a
     *  model where ref/clingo reject). A fault raised at fire propagates as an exception the caller renders. */
    private String fireRood(JsonNode c) {
        labelNaarBean.clear();
        labelType.clear();
        rekendatum = asOfDatum(c);
        ClassLoader vorige = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(cl);
        KieSession sessie = kieBase.newKieSession();
        try {
            CaseInvoer invoer = leesInvoer(c);
            zetGlobals(sessie, c);
            DroolsBackend.voegIn(bouw, sessie, invoer.invoer());
            return vuurTotFixpunt(sessie);
        } finally {
            sessie.dispose();
            Thread.currentThread().setContextClassLoader(vorige);
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

    /** The input beans of the case by label, and their object types, as {@link CaseInvoer} read them: the labels of the
     *  report, the seam and the write-back. */
    private final Map<String, Object> labelNaarBean = new LinkedHashMap<>();
    private final Map<String, Objecttype> labelType = new LinkedHashMap<>();
    /** The case's evaluation date, which {@link #zetGlobals} sets as the global {@code rekendatum}. */
    private LocalDate rekendatum;

    private String runCase(String naam, JsonNode c) {
        return runCase(naam, c, null);
    }

    /** One case: its verdict line, null on a match. The expectations are compared through the backend's entry
     *  ({@link DroolsBackend#vergelijk}), with the case labels of this runner, as a consumer compares them. With a
     *  {@link Waarneming} the replay also reports the firings and every expectation's verdict ({@link #rapport}); the
     *  verdict itself does not depend on it. */
    private String runCase(String naam, JsonNode c, Waarneming w) {
        labelNaarBean.clear();
        labelType.clear();
        rekendatum = asOfDatum(c);
        ClassLoader vorige = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(cl);
        KieSession sessie = kieBase.newKieSession();
        try {
            CaseInvoer invoer = leesInvoer(c);
            zetGlobals(sessie, c);
            DroolsBackend.voegIn(bouw, sessie, invoer.invoer());
            String geenFixpunt = vuurTotFixpunt(sessie, w == null ? null : w.vuren);
            if (geenFixpunt != null) return naam + ": " + geenFixpunt;
            DroolsBackend.Vergelijking vergelijking =
                    DroolsBackend.vergelijk(bouw, sessie, this::labelVanBean, c.get("expected"));
            if (w != null) w.verwachtingen.addAll(vergelijking.verwachtingen());
            return vergelijking.pass() ? null : naam + ": " + vergelijking.diagnose();
        } finally {
            sessie.dispose();
            Thread.currentThread().setContextClassLoader(vorige);
        }
    }

    /** Fires {@code sessie} through the backend's entry ({@link DroolsBackend#vuur}), under the cap a consumer gets, and
     *  returns null at a fixpoint. A run without one returns the sentence of {@code vuur}, which names the cap and what
     *  it was computed from, or that of the creation budget. A consequence that throws leaves the fire as
     *  {@link DroolsBackend#benoem} names it; every replay, red path and report of this class fires here, on either
     *  branch. At a fixpoint the derivations go onto the beans before anything reads them, or the evaluation faults
     *  that stand there are thrown ({@link DroolsBackend#zetAfgeleideWaarden}); without a fixpoint nothing is written
     *  back, since the replay reports the sentence and reads no bean. With the property
     *  {@value Sessiedump#EIGENSCHAP} a seam writes the dump of the session ({@link Sessiedump}) right after the fire,
     *  before the write-back, also where the fire throws, on either branch. */
    private String vuurTotFixpunt(KieSession sessie) {
        return vuurTotFixpunt(sessie, null);
    }

    /** As above, with {@code waarnemer} recording what the session fires ({@link DroolsBackend.Waarnemer}); null
     *  records nothing. */
    private String vuurTotFixpunt(KieSession sessie, DroolsBackend.Waarnemer waarnemer) {
        Sessiedump spoor = documenthash == null ? null : Sessiedump.op(sessie, labelNaarBean);
        DroolsBackend.Vuring vuring;
        try {
            vuring = waarnemer == null ? DroolsBackend.vuur(bouw, sessie) : DroolsBackend.vuur(bouw, sessie, waarnemer);
        } catch (RuntimeException e) {
            if (spoor != null) spoor.naad(documenthash, bouw, e, labelNaarBean);
            if (e instanceof ConsequenceException c) throw DroolsBackend.benoem(c);
            throw e;
        }
        if (spoor != null) spoor.naad(documenthash, bouw, vuring, labelNaarBean);
        if (vuring.geenFixpunt() == null) {
            DroolsBackend.zetAfgeleideWaarden(bouw, sessie, this::labelVanBean);
            return null;
        }
        return vuring.geenFixpunt();
    }

    /** Reads case {@code c} through the backend's entry ({@link CaseInvoer}), as a consumer reads it, and keeps its
     *  labels for this runner's report, seam and write-back. */
    private CaseInvoer leesInvoer(JsonNode c) {
        CaseInvoer invoer = CaseInvoer.van(bouw, c);
        labelNaarBean.putAll(invoer.beans());
        labelType.putAll(invoer.typen());
        return invoer;
    }

    /** Whether {@code attr} on {@code ot} is a §13.3.6 temporal bean member — a {@code List<Segment>} field,
     *  the shape {@link FactModelGenerator} derives from a declared attribuutGranulariteit. */
    boolean temporeel(Objecttype ot, String attr) {
        return Namen.bevatNaam(ot.granulariteitLeden(), attr);
    }

    /** Whether {@code attr} on {@code ot} is a §3.6 dimensioned bean member — a {@code Map<String, BigDecimal>}
     *  field. Checked BEFORE {@link #temporeel}: the generator gives a dimensioned declaration the Map shape
     *  even when a granularity is declared beside it. */
    boolean gedimensioneerd(Objecttype ot, String attr) {
        return !ot.dimensiesVan(attr).isEmpty();
    }

    /** The declared name of the attribute a case writes as {@code naam} on {@code ot} or one of its ancestors, under the
     *  name equality of the reference interpreter ({@link Namen#gedeclareerdeNaam}); {@code naam} where {@code ot} is null
     *  or declares none. The generator derives a member's getter and setter from its declared name, and a case may
     *  write the name in another capitalisation, "vondstNummer" for a declared "vondstnummer", which the reference
     *  interpreter reads as the same member: a method derived from the written name would be one the bean lacks. */
    static String attribuutnaam(Objecttype ot, String naam) {
        return ot == null ? naam : Namen.gedeclareerdeNaam(ot.attribuutLeden().keySet(), naam);
    }

    /** {@link #attribuutnaam} for a characteristic. */
    static String kenmerknaam(Objecttype ot, String naam) {
        if (ot == null) return naam;
        List<String> gedeclareerd = new ArrayList<>();
        for (Objecttype t : ot.keten()) gedeclareerd.addAll(t.kenmerken);
        return Namen.gedeclareerdeNaam(gedeclareerd, naam);
    }

    private void zetGlobals(KieSession sessie, JsonNode c) {
        JsonNode params = c.get("input").get("parameters");
        if (params != null) for (Map.Entry<String, JsonNode> e : params.properties()) {
            if (e.getValue().has("timeline"))
                throw new Excluded("granular (timeline) parameter '" + e.getKey() + "' is not carried by the v0 backend");
            setGlobalVeilig(sessie, Namen.ident(e.getKey(), false), waardeNaarArg(e.getValue(), e.getKey()));
        }
        if (rekendatum != null) setGlobalVeilig(sessie, "rekendatum", rekendatum);
        // §9.8 the DRL-text branch reaches its dagsoort predicates through globals (the exec branch closes over
        // them); empty for the exec branch.
        for (Map.Entry<String, java.util.function.Predicate<LocalDate>> e : dagsoortGlobalen.entrySet())
            setGlobalVeilig(sessie, e.getKey(), e.getValue());
    }

    /** The case's {@code asOfDate}, or null when it carries none. */
    static LocalDate asOfDatum(JsonNode c) {
        JsonNode asOf = c.get("asOfDate");
        return asOf == null || asOf.isNull() ? null : datumVanCaseWire(asOf.asText(), "asOfDate");
    }

    private static void setGlobalVeilig(KieSession sessie, String naam, Object waarde) {
        try { sessie.setGlobal(naam, waarde); }
        catch (RuntimeException onbekend) { /* global not declared: no rule reads it */ }
    }

    // ---------------- §13.3.6 temporal input (the List<Segment> bean members) ----------------

    /** The segments of a timeline value, with ref's load-time validation mirrored: at least one segment (an
     *  EXPECTATION may be empty — {@code leegToegestaan}), {@code from} before {@code to}, both boundaries on the
     *  declared granularity grid, and the list ordered and non-overlapping as written. A malformed timeline is a
     *  hard failure, never a silent acceptance: the case schema (gate 4) and ref (gate 5) reject it first, so
     *  reaching it here means the corpus and this reader disagree. A grid that carries no case timeline is a
     *  not-carried form, refused where ref's loader refuses it and in its sentence, at position {@code plek}
     *  ({@code J1.m2} for an input, {@code expected J1.m2} for an expectation): a grid of several units and a
     *  variable start before any segment is read, the calendar's day, week and quarter grid at the first segment. */
    List<Object> tijdreeks(JsonNode segs, String veld, String plek, Rooster gran, boolean leegToegestaan) {
        if (segs == null || !segs.isArray())
            throw new IllegalArgumentException("misvormde tijdlijnwaarde op '" + veld + "'");
        if (segs.isEmpty() && !leegToegestaan)
            throw new IllegalArgumentException("tijdlijnwaarde op '" + veld + "' draagt geen enkel segment");
        if (gran.aantal() != 1)
            throw new Excluded(plek + ": the declared grid is " + gran.aantal() + " " + spelling(gran.gran())
                    + " units, a multi-period grid, which carries no operation, a case input included"
                    + " (RUNTIME-SEMANTICS ch. 3.2, OPEN in ch. 3.12): refused as a v0 limit, not as a defect of the"
                    + " case");
        if (gran.variabel())
            throw new Excluded(plek + ": the declared grid is laid per object from the anchor of a variable start,"
                    + " which this loader does not read (RUNTIME-SEMANTICS ch. 3.2): refused as a v0 limit, not as a"
                    + " defect of the case");
        List<Object> uit = new ArrayList<>();
        LocalDate vorigeTot = null;
        for (JsonNode sj : segs) {
            LocalDate van = grensDatum(sj, "from", veld), tot = grensDatum(sj, "to", veld);
            if (!van.isBefore(tot))
                throw new IllegalArgumentException("segment 'to' op '" + veld + "' ligt niet na 'from'");
            if (gran.vast() == null) kalenderRoosterVoorInvoer(gran.gran(), plek);
            if (!emmer(gran, van)[0].equals(van) || !emmer(gran, tot)[0].equals(tot))
                throw new IllegalArgumentException("segmentgrens op '" + veld + "' ligt niet op het "
                        + gran.gran() + "-raster" + (gran.vast() == null ? "" : " vanaf " + gran.vast()));
            if (vorigeTot != null && vorigeTot.isAfter(van))
                throw new IllegalArgumentException("tijdlijnsegmenten op '" + veld + "' zijn ongeordend of overlappen");
            vorigeTot = tot;
            uit.add(segment(van, tot, segmentWaarde(sj.get("value"), veld)));
        }
        return uit;
    }

    /** The grid a temporal member's case values sit on: its facet's granularity and count, its fixed start point,
     *  and whether it rides a timeline with a variable start (RUNTIME-SEMANTICS ch. 3.2). */
    record Rooster(Granulariteit gran, int aantal, String vast, boolean variabel) {}

    Rooster rooster(Objecttype ot, String attr) {
        Integer aantal = Namen.opNaam(ot.tijdlijnAantalLeden(), attr);
        return new Rooster(Namen.opNaam(ot.granulariteitLeden(), attr), aantal == null ? 1 : aantal,
                ot.vastStartpuntVan(attr), model.heeftVariabelStartpunt(ot, attr));
    }

    /** The period {@code [van, tot)} of the member's grid that contains {@code d} (ch. 3.2), as ref lays it: the
     *  period a fixed start point lays, every wall computed from it; otherwise the calendar bucket — year buckets run
     *  1 Jan → 1 Jan, month buckets first-of-month → first-of-next. A grid that lays no such period is a loud
     *  not-carried, never a guessed grid, in the sentence of ref's grid function, which the ASP backend speaks as
     *  well: a grid of several units carries no operation, a variable start is laid per object from an anchor the
     *  case does not give, and the calendar lays no day, week or quarter bucket. For a case timeline
     *  {@link #tijdreeks} refuses the same grids first, in the sentence of ref's case loader. */
    static LocalDate[] emmer(Rooster r, LocalDate d) {
        if (r.aantal() != 1)
            throw new Excluded("a multi-period grid (" + r.aantal() + " " + spelling(r.gran()) + " periods) carries"
                    + " no operation (RUNTIME-SEMANTICS ch. 3.2, OPEN in ch. 3.12)");
        if (r.variabel())
            throw new Excluded("a variable-start grid is laid per object from the anchor its start-point"
                    + " determination binds, which this interpreter does not execute (ch. 3.2)");
        if (r.vast() != null) return VastStartpunt.periode(r.gran(), r.vast(), d);
        switch (r.gran()) {
            case JAAR:
                return new LocalDate[]{ LocalDate.of(d.getYear(), 1, 1), LocalDate.of(d.getYear() + 1, 1, 1) };
            case MAAND:
                LocalDate van = d.withDayOfMonth(1);
                return new LocalDate[]{ van, van.plusMonths(1) };
            default:
                throw new Excluded("bucketing is defined for month and year only (ch. 3.2)");
        }
    }

    /** ref's loader checks a case timeline on the calendar grid through the calendar bucketing, which has month and
     *  year buckets only (ch. 3.2), and refuses the other three granularities as a v0 limit, each in its own
     *  sentence; this is that refusal, at position {@code plek}. */
    private static void kalenderRoosterVoorInvoer(Granulariteit gran, String plek) {
        switch (gran) {
            case JAAR, MAAND -> { }
            case DAG -> throw new Excluded(plek + ": the declared grid is day, which every day boundary sits on, yet"
                    + " this loader checks a timeline's grid through the calendar bucketing and v0 defines buckets"
                    + " for month and year only (RUNTIME-SEMANTICS ch. 3.2, OPEN in ch. 3.12): refused as a v0 limit,"
                    + " not as a defect of the case");
            default -> throw new Excluded(plek + ": the declared grid is " + spelling(gran) + ", and v0 defines"
                    + " calendar buckets for month and year only (RUNTIME-SEMANTICS ch. 3.2, OPEN in ch. 3.12), so"
                    + " this loader has no " + spelling(gran) + " grid to check the segment boundaries against:"
                    + " refused as a v0 limit, not as a defect of the case");
        }
    }

    /** A granularity in its wire spelling (IR-SPEC ch. 2.2), the word ref's sentences carry. */
    private static String spelling(Granulariteit gran) {
        return Spelling.GRANULARITY.spelling(gran);
    }

    /** The numeric value of one segment. The generated {@code Segment} holds a {@code BigDecimal} waarde, so a
     *  non-numeric timeline value has no bean representation — a reason-carrying not-carried, never a coercion. */
    BigDecimal segmentWaarde(JsonNode w, String veld) {
        Object v = waardeNaarArg(w, veld);
        if (v == null || v instanceof BigDecimal) return (BigDecimal) v;
        throw new Excluded("non-numeric timeline value on '" + veld + "' is not carried by the v0 backend — "
                + "the generated Segment holds a BigDecimal waarde");
    }

    static LocalDate grensDatum(JsonNode seg, String veld, String waar) {
        JsonNode d = seg == null ? null : seg.get(veld);
        if (d == null || !d.isTextual())
            throw new IllegalArgumentException("segment op '" + waar + "' mist een '" + veld + "'-datum");
        // the field name carries BOTH coordinates: a case may hold many timelines and each holds two
        // boundaries, so "onderhoudstarief.from" is the only form a producer can act on
        return datumVanCaseWire(d.asText(), waar + "." + veld);
    }

    /** One {@code Segment} fact of the generated bean model, built through the runtime's own reflective
     *  constructor — the Segment class is a runtime objecttype, unknown at compile time. */
    Object segment(LocalDate van, LocalDate tot, BigDecimal waarde) {
        try {
            return RegelSpraakRuntime.newSegment(
                    Class.forName(FactModelGenerator.PAKKET + ".Segment", true, cl), van, tot, waarde);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("de gegenereerde Segment-factklasse ontbreekt", e);
        }
    }

    // ---------------- comparison ----------------

    /** The comparison behind {@link DroolsBackend#vergelijk}, on a runner of {@code build} that knows the beans of
     *  {@code sessie} by the labels of the caller: {@code perLabel} gives the bean of each label, {@code typen} its object
     *  type. The runner reads no case input, so a session the caller filled itself compares as one of the replay. */
    static DroolsBackend.Vergelijking vergelijking(DroolsBackend.Build build, KieSession sessie,
                                                   Map<String, Object> perLabel, Map<String, Objecttype> typen,
                                                   JsonNode verwachtingen) {
        CaseRunner r = new CaseRunner(build.model(), build.kieBase(), build.beanLoader(), build.dagsoortGlobalen());
        r.labelNaarBean.putAll(perLabel);
        r.labelType.putAll(typen);
        List<ScenarioResultaat.VerwachtingResultaat> uit = new ArrayList<>();
        List<String> mis = new ArrayList<>();
        int positie = 0;
        for (JsonNode v : verwachtingen) {
            String m = r.vergelijk(v, sessie);
            if (m != null) mis.add(m);
            positie++;
            uit.add(new ScenarioResultaat.VerwachtingResultaat(bron(v), r.verwacht(v), r.werkelijk(v, sessie), m == null,
                    positie));
        }
        return new DroolsBackend.Vergelijking(uit, String.join("; ", mis));
    }

    /** The object type of the bean that carries {@code label}, for expectation {@code v} on one of its members. A label
     *  no bean carries is refused: there is nothing to read, and the "leeg" or "afwezig" the replay read there until
     *  {@link DroolsBackend#vergelijk} let attributeEmpty and characteristicAbsent hold on a label that names nothing, as
     *  when a caller gives the case labels to a session it labelled with the ids of its request. ref refuses such a case
     *  at load ("expectation references unknown label"). */
    private Objecttype typeVanLabel(JsonNode v, String label) {
        Objecttype ot = labelType.get(label);
        if (ot == null)
            throw new IllegalArgumentException(v.get("kind").asText() + "-verwachting op onbekend label '" + label
                    + "': geen bean van de sessie draagt het, dus er is niets te lezen");
        return ot;
    }

    private String vergelijk(JsonNode v, KieSession sessie) {
        String kind = v.get("kind").asText();
        return switch (kind) {
            case "attributeValue" -> attribuut(v, false);
            case "attributeEmpty" -> attribuut(v, true);
            case "characteristicPresent" -> kenmerk(v, true);
            case "characteristicAbsent" -> kenmerk(v, false);
            case "consistent" -> consistentie(sessie, false, v);
            case "inconsistent" -> consistentie(sessie, true, v);
            case "attributeTimeline" -> tijdlijn(v);
            default -> throw new Excluded("unknown expectation kind: " + kind);
        };
    }

    private String attribuut(JsonNode v, boolean leegVerwacht) {
        String attr = v.get("attribute").asText();
        String label = v.get("label").asText();
        Objecttype ot = typeVanLabel(v, label);
        eigenVeld(ot, attr, false, "leest");
        if (gedimensioneerd(ot, attr))
            throw new Excluded("dimensioned attribute read-back '" + attr + "' (§3.6, a Map bean member) "
                    + "is not carried by the v0 backend");
        // a temporal attribute's bean member is the List<Segment> timeline itself: the scalar comparison below
        // would compare a list against a scalar. Its read-back form is the attributeTimeline expectation.
        if (temporeel(ot, attr))
            throw new Excluded("scalar read-back of temporal attribute '" + attr + "' is not carried by the v0 "
                    + "backend — the bean holds a List<Segment>; use an attributeTimeline expectation");
        Object werkelijk = lees(labelNaarBean.get(label), Namen.getter(attribuutnaam(ot, attr)));
        if (leegVerwacht)
            return werkelijk == null ? null : "verwacht leeg, werkelijk " + werkelijk;
        Object verwacht = waardeNaarArg(v.get("value"), attr);
        return gelijkeWaarde(v.get("value"), verwacht, werkelijk) ? null
                : "attribuut " + attr + ": verwacht " + render(verwacht) + ", werkelijk " + render(werkelijk);
    }

    /** The §13.3.6 timeline read-back: the doel's {@code List<Segment>} against the pinned segment list, with
     *  ref's comparison discipline — equal length, van/tot EXACT, waarde scale-safe ({@code compareTo == 0}),
     *  compared index-for-index. Both sides arrive ordered (the expectation through the validator, the bean
     *  list through the runtime's ordered writes), so an order-blind comparator has no observable witness —
     *  the claim stops at what is falsifiable. A never-written target (a null field) matches an explicitly
     *  empty expectation: both read leeg on every day (ch. 3.1), exactly as ref and clingo pin it. */
    private String tijdlijn(JsonNode v) {
        String attr = v.get("attribute").asText(), label = v.get("label").asText();
        Objecttype ot = labelType.get(label);
        // both are CASE defects, not uncarried forms — ref rejects the same shapes at load ("expected
        // timeline on %S, which declares no granularity"), so they are hard failures here too, and the
        // unknown-label arm would otherwise reach emmer() with a null granularity (an unnamed NPE).
        if (ot == null)
            throw new IllegalArgumentException("attributeTimeline-verwachting op onbekend label '" + label + "'");
        if (!temporeel(ot, attr))
            throw new IllegalArgumentException("attributeTimeline-verwachting op niet-temporeel attribuut '" + attr + "'");
        eigenVeld(ot, attr, false, "leest");
        List<Object> verwacht = tijdreeks(v.get("timeline"), attr, "expected " + label + "." + attr, rooster(ot, attr),
                true);
        Object gelezen = labelNaarBean.containsKey(label)
                ? lees(labelNaarBean.get(label), Namen.getter(attribuutnaam(ot, attr))) : null;
        if (gelezen == null)
            return verwacht.isEmpty() ? null
                    : "attribuut " + attr + ": draagt geen tijdreeks, verwacht " + renderTl(verwacht);
        List<?> werkelijk = (List<?>) gelezen;
        boolean gelijk = werkelijk.size() == verwacht.size();
        for (int k = 0; gelijk && k < verwacht.size(); k++)
            gelijk = segGelijk(verwacht.get(k), werkelijk.get(k));
        return gelijk ? null
                : "attribuut " + attr + ": verwacht " + renderTl(verwacht) + ", werkelijk " + renderTl(werkelijk);
    }

    private static boolean segGelijk(Object verw, Object werkelijk) {
        return java.util.Objects.equals(segVeld(verw, "van"), segVeld(werkelijk, "van"))
                && java.util.Objects.equals(segVeld(verw, "tot"), segVeld(werkelijk, "tot"))
                && gelijk(segVeld(verw, "waarde"), segVeld(werkelijk, "waarde"));
    }

    /** One field of a {@code Segment} fact, read through the runtime's property bridge (the class is a runtime
     *  objecttype — no compile-time type to cast to). */
    private static Object segVeld(Object seg, String veld) { return RegelSpraakRuntime.get(seg, veld); }

    private static String renderTl(List<?> segs) {
        if (segs.isEmpty()) return "(leeg)";
        List<String> uit = new ArrayList<>();
        for (Object s : segs)
            uit.add("[" + segVeld(s, "van") + "," + segVeld(s, "tot") + ")=" + render(segVeld(s, "waarde")));
        return String.join(" ", uit);
    }

    private String kenmerk(JsonNode v, boolean aanwezigVerwacht) {
        Objecttype ot = typeVanLabel(v, v.get("label").asText());
        eigenVeld(ot, v.get("characteristic").asText(), true, "leest");
        Object gelezen = lees(labelNaarBean.get(v.get("label").asText()),
                Namen.getter(kenmerknaam(ot, v.get("characteristic").asText())));
        boolean aanwezig = Boolean.TRUE.equals(gelezen);
        return aanwezig == aanwezigVerwacht ? null
                : "kenmerk " + v.get("characteristic").asText() + ": verwacht "
                + (aanwezigVerwacht ? "aanwezig" : "afwezig") + ", werkelijk " + (aanwezig ? "aanwezig" : "afwezig");
    }

    /** The §9.5 consistency check. A {@code consistent} expectation demands NO Inconsistentie fact at all
     *  (unchanged presence semantics). An {@code inconsistent} expectation is verified against the pinned §9.5
     *  KEY (the {@code rule}) — a matching Inconsistentie fact must exist — and, when the expectation pins a
     *  {@code label}, on that SUBJECT, for every key kind, as ref matches it ({@code ref/lib/case.ml}, the
     *  {@code Inconsistent} arm: rule and exact subject label on the same fact). Until card drools-consistentie-label
     *  (2026-09-23) the subject was checked only for a default-group conflict key and a rule-name key passed on any
     *  fact from its rule, wherever it landed. That masked one real divergence: both transpilers laid the §9.5
     *  allocation error on the source instead of each recipient ({@code jutterswet-rangmaximering}), fixed in the same
     *  card. An expectation carrying no rule falls back to bare presence. */
    private String consistentie(KieSession sessie, boolean inconsistentVerwacht, JsonNode v) {
        List<Object> facts = inconsistenties(sessie);
        if (!inconsistentVerwacht)
            return facts.isEmpty() ? null : "verwacht consistent, werkelijk inconsistent (" + renderIncs(facts) + ")";
        String rule = v.has("rule") && !v.get("rule").isNull() ? v.get("rule").asText() : null;
        String label = v.has("label") && !v.get("label").isNull() ? v.get("label").asText() : null;
        if (facts.isEmpty())
            return "verwacht inconsistent" + sleutelTekst(rule, label) + ", werkelijk consistent";
        if (rule == null) return null;   // presence-only expectation, satisfied by any fact
        for (Object f : facts) {
            if (!zelfdeRegel(rule, (String) lees(f, "getRegel"))) continue;
            if (label == null || label.equals(labelVanBean(lees(f, "getSubject")))) return null;
        }
        return "verwacht inconsistent" + sleutelTekst(rule, label) + ", niet gevonden onder " + renderIncs(facts);
    }

    /** Every §9.5 Inconsistentie fact the session holds. */
    private List<Object> inconsistenties(KieSession sessie) {
        List<Object> facts = new ArrayList<>();
        try {
            Class<?> inc = Class.forName(FactModelGenerator.PAKKET + ".Inconsistentie", true, cl);
            facts.addAll(sessie.getObjects(inc::isInstance));
        } catch (ClassNotFoundException e) { /* no Inconsistentie bean -> none */ }
        return facts;
    }

    /** ref's rule-key identity on the §9.5 channel ({@code ref/lib/case.ml}, the {@code Inconsistent} arm):
     *  {@code norm (strip_version a) = norm (strip_version b)}. A rule is a declared name like any other, so a pin
     *  spelled differently from its declaration, or a pin on a version-flattened deposit, names the same rule. */
    static boolean zelfdeRegel(String a, String b) {
        return a != null && b != null && Namen.norm(zonderVersie(a)).equals(Namen.norm(zonderVersie(b)));
    }

    /** The verifier's {@code Checks.strip_version}, character for character: a trailing {@code " #<digits>"}
     *  suffix with at least one digit, whose {@code #} sits at index 2 or later, comes off together with the one
     *  space before it; anything else stays. Stripped BEFORE norm, as ref does, so "X  #2" becomes "X " and then
     *  "x". Deliberately not {@code ProducentConsument.basisNaam}, which also strips a group prefix and a
     *  beslistabel suffix and would make this comparison wider than ref's. */
    static String zonderVersie(String naam) {
        int i = naam.lastIndexOf('#');
        if (i >= 2 && naam.charAt(i - 1) == ' ' && i + 1 < naam.length()
                && naam.substring(i + 1).chars().allMatch(c -> c >= '0' && c <= '9'))
            return naam.substring(0, i - 1);
        return naam;
    }

    private static String sleutelTekst(String rule, String label) {
        return rule == null ? "" : " (" + rule + (label != null ? " @ " + label : "") + ")";
    }

    /** The case label whose bean IS this fact's subject (identity) — the reverse of {@link #labelNaarBean},
     *  so the §9.5 fact's subject can be checked against the expectation's {@code label}. null if not an input bean. */
    private String labelVanBean(Object bean) {
        if (bean == null) return null;
        for (Map.Entry<String, Object> e : labelNaarBean.entrySet()) if (e.getValue() == bean) return e.getKey();
        return null;
    }

    /** The §9.5 facts as "regel @ label", sorted: the session gives them in the order of its working memory, which
     *  differs between two renderings of one analysis, and a mismatch line must not. */
    private String renderIncs(List<Object> facts) {
        List<String> uit = new ArrayList<>();
        for (Object f : facts) uit.add(lees(f, "getRegel") + " @ " + labelVanBean(lees(f, "getSubject")));
        uit.sort(Comparator.naturalOrder());
        return String.join(", ", uit);
    }

    // ---------------- the report's renderings ----------------

    /** The expectation itself: its kind and what it is about. */
    private static String bron(JsonNode v) {
        String kind = v.get("kind").asText();
        if (kind.equals("inconsistent"))
            return kind + sleutelTekst(v.hasNonNull("rule") ? v.get("rule").asText() : null,
                    v.hasNonNull("label") ? v.get("label").asText() : null);
        StringBuilder b = new StringBuilder(kind);
        for (String veld : new String[]{"label", "attribute", "characteristic"})
            if (v.hasNonNull(veld)) b.append(' ').append(v.get(veld).asText());
        return b.toString();
    }

    /** The expected side, rendered as {@link #werkelijk} renders the actual one. */
    private String verwacht(JsonNode v) {
        return switch (v.get("kind").asText()) {
            case "attributeValue" -> render(waardeNaarArg(v.get("value"), v.get("attribute").asText()));
            case "attributeEmpty" -> "leeg";
            case "attributeTimeline" -> {
                List<String> segs = new ArrayList<>();
                for (JsonNode s : v.get("timeline"))
                    segs.add("[" + s.get("from").asText() + "," + s.get("to").asText() + ")="
                            + render(waardeNaarArg(s.get("value"), v.get("attribute").asText())));
                yield segs.isEmpty() ? "(leeg)" : String.join(" ", segs);
            }
            case "characteristicPresent" -> "aanwezig";
            case "characteristicAbsent" -> "afwezig";
            case "consistent" -> "consistent";
            default -> "inconsistent" + sleutelTekst(v.hasNonNull("rule") ? v.get("rule").asText() : null,
                    v.hasNonNull("label") ? v.get("label").asText() : null);
        };
    }

    /** The actual side after fire, read where the comparison reads it: a member through its declared name
     *  ({@link #attribuutnaam}, {@link #kenmerknaam}), since a case may write it in another capitalisation. */
    private String werkelijk(JsonNode v, KieSession sessie) {
        String label = v.hasNonNull("label") ? v.get("label").asText() : null;
        Object bean = label == null ? null : labelNaarBean.get(label);
        Objecttype ot = label == null ? null : labelType.get(label);
        return switch (v.get("kind").asText()) {
            case "attributeValue", "attributeEmpty" -> render(bean == null ? null
                    : lees(bean, Namen.getter(attribuutnaam(ot, v.get("attribute").asText()))));
            case "attributeTimeline" -> {
                Object gelezen = bean == null ? null
                        : lees(bean, Namen.getter(attribuutnaam(ot, v.get("attribute").asText())));
                yield gelezen == null ? "(leeg)" : renderTl((List<?>) gelezen);
            }
            case "characteristicPresent", "characteristicAbsent" -> bean != null && Boolean.TRUE.equals(
                    lees(bean, Namen.getter(kenmerknaam(ot, v.get("characteristic").asText())))) ? "aanwezig" : "afwezig";
            default -> {
                List<Object> facts = inconsistenties(sessie);
                yield facts.isEmpty() ? "consistent" : "inconsistent (" + renderIncs(facts) + ")";
            }
        };
    }

    // ---------------- value helpers ----------------

    private static boolean gelijk(Object verw, Object werkelijk) {
        if (verw == null) return werkelijk == null;
        if (verw instanceof BigDecimal b) return werkelijk instanceof BigDecimal w && b.compareTo(w) == 0;
        return verw.equals(werkelijk);
    }

    /** {@link #gelijk} with the ONE value form whose identity is not byte equality: an ENUMERATION label is a
     *  DECLARED NAME, and ref compares those up to {@code Checks.norm} ({@code arith.ml}: {@code Enum a, Enum b
     *  -> norm a = norm b}), while the ASP backend stores the normalised label outright. The bean holds the
     *  case's own spelling — ref keeps that too — so an input written in a capitalisation other than the
     *  declaration's reads back byte-different and value-equal, and a byte comparator here would report a
     *  divergence that neither other executor has. {@code corpus/golden/jutterswet-keurstaat} spells one
     *  deliberately ("Beschadigd" against a declared "beschadigd") and that is what this arm exists for; it was
     *  unreachable until the enumeration literal reader stopped refusing the whole document (card
     *  `drools-enumlabel-lezer`, 2026-09-20). The expectation NODE decides, not the Java type: a Tekst value
     *  arrives as the same String and keeps the exact equality §13.3.3.4 gives it. */
    private static boolean gelijkeWaarde(JsonNode verwachtNode, Object verw, Object werkelijk) {
        if (verwachtNode != null && verwachtNode.has("enumeration"))
            return verw instanceof String a && werkelijk instanceof String b
                    && Namen.norm(a).equals(Namen.norm(b));
        return gelijk(verw, werkelijk);
    }

    /** The Java value a uchar case value node denotes — the already-normalised scalar the bean setter /
     *  global takes. An input form the v0 backend does not carry (a list/struct/constructor value, a
     *  rational lexeme, a timeline) is a reason-carrying {@link Excluded}. */
    Object waardeNaarArg(JsonNode w, String veld) {
        if (w == null || w.isNull()) return null;
        if (w.has("number")) {
            String s = w.get("number").asText();
            if (s.contains("/")) throw new Excluded("rational value on '" + veld + "' is not carried by the v0 backend");
            return new BigDecimal(s);
        }
        if (w.has("date")) return datumVanCaseWire(w.get("date").asText(), veld);
        if (w.has("dateTime")) return datumTijdVanCaseWire(w.get("dateTime").asText(), veld);
        if (w.has("boolean")) return w.get("boolean").asBoolean();
        if (w.has("text")) return w.get("text").asText();
        if (w.has("enumeration")) return w.get("enumeration").asText();
        if (w.has("timeline")) throw new Excluded("timeline value on '" + veld + "' is not carried by the v0 backend");
        throw new Excluded("value form on '" + veld + "' is not carried by the v0 backend: " + w);
    }

    /* The uchar-case-v0 {@code date} lexeme, matched WHOLE: four-two-two digit fields, digits only, no sign
     * and no extended year. It is the schema's own pattern, held here as well, for the same reason as the
     * dateTime one below — gate 4 checks case FILES, this reader checks every case date that reaches the
     * backend by any route. */
    private static final java.util.regex.Pattern CASE_DATUM =
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
    private static LocalDate datumVanCaseWire(String lexeem, String veld) {
        if (!CASE_DATUM.matcher(lexeem).matches())
            throw new IllegalArgumentException("malformed date '" + lexeem + "' on '" + veld
                    + "': the uchar-case-v0 lexeme is yyyy-mm-dd — fixed field widths, digits only,"
                    + " no sign and no extended year");
        try { return LocalDate.parse(lexeem); }
        catch (java.time.format.DateTimeParseException e) {
            throw new IllegalArgumentException("malformed date '" + lexeem + "' on '" + veld
                    + "': the field widths are right but the calendar is not (" + e.getMessage() + ")");
        }
    }

    /* The uchar-case-v0 {@code dateTime} lexeme, matched WHOLE: fixed field widths, {@code T} as the
     * separator, the seconds field always written, a fraction that is optional but EXACTLY three digits
     * (whole milliseconds). It is the schema's own pattern, held here as well — gate 4 checks case FILES,
     * this reader checks every case value that reaches the backend by any route. */
    private static final java.util.regex.Pattern CASE_DATUM_TIJD =
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
    private static LocalDateTime datumTijdVanCaseWire(String lexeem, String veld) {
        if (!CASE_DATUM_TIJD.matcher(lexeem).matches())
            throw new IllegalArgumentException("malformed dateTime '" + lexeem + "' on '" + veld
                    + "': the uchar-case-v0 lexeme is yyyy-mm-ddThh:mm:ss[.fff] — fixed field widths, 'T',"
                    + " the seconds field written, no zone or offset, a fraction of exactly three digits");
        try { return LocalDateTime.parse(lexeem); }
        catch (java.time.format.DateTimeParseException e) {
            throw new IllegalArgumentException("malformed dateTime '" + lexeem + "' on '" + veld
                    + "': the field widths are right but the calendar is not (" + e.getMessage() + ")");
        }
    }

    static Class<?> argType(Datatype dt) {
        if (dt == null) return Object.class;
        if (dt.isNumeriek() || dt.isPercentage()) return BigDecimal.class;
        if (dt.isTekst() || dt instanceof Datatype.Enumeratie) return String.class;
        if (dt instanceof Datatype.Booleaan) return Boolean.class;
        if (dt instanceof Datatype.DatumInDagen) return LocalDate.class;
        if (dt instanceof Datatype.DatumTijd) return LocalDateTime.class;
        return Object.class;
    }

    static void roep(Object bean, String methode, Class<?> type, Object waarde) {
        try { bean.getClass().getMethod(methode, type).invoke(bean, waarde); }
        catch (ReflectiveOperationException e) {
            throw new RuntimeException("kon setter " + methode + "(" + type.getSimpleName() + ") niet aanroepen op "
                    + bean.getClass().getSimpleName(), e);
        }
    }

    static Object lees(Object bean, String getter) {
        for (String mth : new String[]{ getter, "is" + getter.substring(3) }) {
            try { return bean.getClass().getMethod(mth).invoke(bean); }
            catch (NoSuchMethodException ignore) { /* try next form */ }
            catch (ReflectiveOperationException e) { throw new RuntimeException("kon " + mth + " niet lezen", e); }
        }
        throw new IllegalStateException("geen getter " + getter + " op " + bean.getClass().getSimpleName());
    }

    private static String render(Object o) { return o == null ? "leeg" : o.toString(); }
}
