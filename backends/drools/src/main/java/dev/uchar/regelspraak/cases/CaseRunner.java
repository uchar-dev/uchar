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
import dev.uchar.regelspraak.transpiler.DrlTranspiler;
import dev.uchar.regelspraak.transpiler.kern.Dekking;
import dev.uchar.regelspraak.transpiler.kern.Namen;
import dev.uchar.regelspraak.transpiler.tms.TmsTranspiler;

import org.kie.api.KieBase;
import org.kie.api.event.rule.AfterMatchFiredEvent;
import org.kie.api.event.rule.DefaultAgendaEventListener;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.rule.ConsequenceException;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

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
     *  constructor stays package-private, since only this backend says what it does not carry. */
    public static final class Excluded extends RuntimeException {
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
    /** The exec branch's coverage manifest, every construct lowered (a build refuses any other): what the scenario
     *  report's coverage ranges over ({@link #rapport}). Empty for the DRL-text branch, which reports nothing. */
    private List<Dekking> dekking = List.of();
    /** The build this runner fires: the exec-model branch puts its derivations on the beans only after a fire
     *  ({@link DroolsBackend#zetAfgeleideWaarden}), and that needs the transpiler that made them. */
    private DroolsBackend.Build bouw;

    /** The members of the model without a bean field of their own ({@link FactModelGenerator#verdringingen}): a
     *  case that sets or reads one is refused by name, since the replay would reach the field of the declaration
     *  that took the property. */
    private final List<FactModelGenerator.Verdringing> verdringingen;

    private CaseRunner(RegelSpraakModel model, KieBase kieBase, ClassLoader cl,
                       Map<String, java.util.function.Predicate<LocalDate>> dagsoortGlobalen) {
        this.model = model; this.kieBase = kieBase; this.cl = cl; this.dagsoortGlobalen = dagsoortGlobalen;
        this.verdringingen = FactModelGenerator.verdringingen(model);
    }

    /** Refuses a case that sets or reads ({@code handeling}) the attribute or, with {@code kenmerk}, the
     *  characteristic {@code naam} of an instance of {@code ot} when that member has no bean field of its own. */
    private void eigenVeld(Objecttype ot, String naam, boolean kenmerk, String handeling) {
        String reden = ot == null ? null : FactModelGenerator.verdrongenLid(verdringingen, ot, naam, kenmerk);
        if (reden != null) throw new Excluded(reden + "; de case " + handeling + " het");
    }

    /** Load {@code irPath}, build the KieBase, and replay each of {@code casePaths}. Any {@link Excluded}
     *  (loader refusal, transpiler marker, contract violation, unsupported form) becomes an EXCLUDE result. */
    public static Result runGolden(Path irPath, List<Path> casePaths) {
        return replay(irPath, casePaths, DroolsBackend.Branch.EXEC_MODEL);
    }

    /** The same replay through the OTHER transpiler branch: {@link DrlTranspiler} emits DRL TEXT, which
     *  {@link DroolsBackend} compiles to a {@link KieBase} (the classic DRL build; the exec-model branch is untouched).
     *  SINCE 2026-09-19 THIS IS A GATE-12 PATH (card gate12-drl-takteller): check-drools.sh sweeps every
     *  golden through it (a {@code --drl} run of {@link DroolsBackendMain}) and pins the result in
     *  {@code EXPECTED_DRL_PLAYED} plus a reason-carrying non-player list, so gate 12's numbers DO move
     *  with it — alongside, and separately from, the exec-model square's own {@code EXPECTED_PLAYED}.
     *  Before that card it was the harness/measurement entry only; gate 13 has driven it since
     *  2026-09-04. */
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
            Waarneming w = new Waarneming();
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
            List<ScenarioResultaat.Vuring> vuringen = new ArrayList<>();
            for (Map.Entry<String, Integer> e : w.telling.entrySet())
                vuringen.add(new ScenarioResultaat.Vuring(e.getKey(), List.copyOf(w.labels.get(e.getKey())), e.getValue()));
            List<ScenarioResultaat.DekkingRegel> dekking = new ArrayList<>();
            for (Dekking d : runner.dekking)
                dekking.add(new ScenarioResultaat.DekkingRegel(d.naam(), gevuurd(d.naam(), w.vormen)
                        ? ScenarioResultaat.DekkingSoort.GEVUURD : ScenarioResultaat.DekkingSoort.NIET_GERAAKT));
            uit.add(new ScenarioResultaat(naam, fout == null ? ScenarioResultaat.Status.PASS : ScenarioResultaat.Status.FAIL,
                    List.copyOf(w.verwachtingen), new ScenarioResultaat.Trace(vuringen), dekking, fout == null ? "" : fout));
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

    /** Whether construct {@code construct} fired: whether a rule of that form fired, by the exact name the manifest
     *  gives it ({@link #luisteraar}). Exact, so each version of a rule has a verdict of its own, where the trace gives
     *  the versions of one rule one row; on golden anchor/jutterswet-jutheffing the second version of "jutheffing naar
     *  jaartarief" is not reached in 2025, and a comparison on the row's name called it fired. */
    private static boolean gevuurd(String construct, java.util.Set<String> gevuurdeVormen) {
        return gevuurdeVormen.contains(construct);
    }

    /** What one case's replay reports beside its verdict: the firings per rule with the labels of the input beans they
     *  bound, and per expectation its verdict. */
    private static final class Waarneming {
        final Map<String, Integer> telling = new LinkedHashMap<>();
        final Map<String, List<String>> labels = new LinkedHashMap<>();
        /** The forms a rule of which fired, by the exact name the manifest gives them, version suffix included. */
        final java.util.Set<String> vormen = new java.util.HashSet<>();
        final List<ScenarioResultaat.VerwachtingResultaat> verwachtingen = new ArrayList<>();
    }

    /** The witness seam of {@code backends/drools/src/test}: the beans one case's INPUT produces, before any
     *  rule fires. Every gate observes this layer only through post-fire expectation matches, so what an input
     *  BECAME has no other observable surface: the bucket a scalar landed in, the segments a timeline placed on
     *  a {@code List<Segment>} member, or WHICH LEG of a §13.3.9.2 reciprocal pair a role filled — each could be
     *  wrong and still fold to the right total, passing unseen. Roles are therefore set here too, in the replay's
     *  own order (attributes, then roles), so the mirrored self-pair fill is assertable bean-by-bean. Globals
     *  stay out: they live on a {@link KieSession}, which is fire's concern, not this layer's. */
    static Map<String, Object> invoerBeans(Path irPath, JsonNode c) throws java.io.IOException {
        CaseRunner runner = build(irPath);
        runner.rekendatum = asOfDatum(c);
        ClassLoader vorige = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(runner.cl);
        try {
            JsonNode instances = c.get("input").get("instances");
            for (JsonNode i : instances) runner.beanVoor(i);
            for (JsonNode i : instances) runner.zetAttributenEnKenmerken(i);
            for (JsonNode i : instances) runner.zetRollen(i);
            runner.spiegelWederkerigeRollen();
            return runner.labelNaarBean;
        } finally {
            Thread.currentThread().setContextClassLoader(vorige);
        }
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
            JsonNode instances = c.get("input").get("instances");
            for (JsonNode i : instances) runner.beanVoor(i);
            for (JsonNode i : instances) runner.zetAttributenEnKenmerken(i);
            for (JsonNode i : instances) runner.zetRollen(i);
            runner.spiegelWederkerigeRollen();
            runner.zetGlobals(sessie, c);
            runner.voegInvoerIn(sessie);
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
        lijsten.clear();
        emmers.clear();
        rekendatum = asOfDatum(c);
        ClassLoader vorige = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(cl);
        KieSession sessie = kieBase.newKieSession();
        try {
            JsonNode instances = c.get("input").get("instances");
            for (JsonNode i : instances) beanVoor(i);
            for (JsonNode i : instances) zetAttributenEnKenmerken(i);
            for (JsonNode i : instances) zetRollen(i);
            spiegelWederkerigeRollen();
            zetGlobals(sessie, c);
            voegInvoerIn(sessie);
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

    /** As above, with {@code tak} choosing WHICH transpiler branch produces the KieBase: the exec-model branch, which
     *  derives with truth maintenance, or {@link DrlTranspiler}'s DRL TEXT. The whole prelude — load,
     *  refuse, beans, contract, lowering with every side table, coverage check, KieBase — is
     *  {@link DroolsBackend#build(Path, DroolsBackend.Branch)}, the backend's public entry, so the green replay,
     *  the red probe and every witness seam build exactly as a consumer of the backend does. */
    private static CaseRunner build(Path irPath, DroolsBackend.Branch tak) throws java.io.IOException {
        DroolsBackend.Build b = DroolsBackend.build(irPath, tak);
        CaseRunner runner = new CaseRunner(b.model(), b.kieBase(), b.beanLoader(), b.dagsoortGlobalen());
        runner.bouw = b;
        // the exec branch's coverage manifest feeds the scenario report; the DRL-text branch reports nothing
        if (tak == DroolsBackend.Branch.EXEC_MODEL) runner.dekking = b.dekking();
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
     *  the write-back throws. "geen fixpunt (kringverwijzing?)" is the sentence of the DRL-text branch at its cap where
     *  no writers kept overwriting each other ({@link #vuurTotFixpunt}). Gate 8 therefore pins only
     *  this backend's own sentence, never platform text, since a pin on a JDK message would pin a crash; for a
     *  refusal while the case is read, that sentence names position and lexeme. */
    public static RoodResult runRood(Path irPath, Path casePath) {
        CaseRunner runner;
        try {
            runner = build(irPath);
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
        lijsten.clear();
        emmers.clear();
        rekendatum = asOfDatum(c);
        ClassLoader vorige = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(cl);
        KieSession sessie = kieBase.newKieSession();
        try {
            JsonNode input = c.get("input");
            JsonNode instances = input.get("instances");
            for (JsonNode i : instances) beanVoor(i);
            for (JsonNode i : instances) zetAttributenEnKenmerken(i);
            for (JsonNode i : instances) zetRollen(i);
            spiegelWederkerigeRollen();
            zetGlobals(sessie, c);
            voegInvoerIn(sessie);
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

    private final Map<String, Object> labelNaarBean = new LinkedHashMap<>();
    private final Map<String, Objecttype> labelType = new LinkedHashMap<>();
    /** The to-many lists of the case input per holder bean and per reference field of their members, as
     *  {@link #zetRollen} writes them: the order {@link #voegInvoerIn} gives each list ({@link Invoer#lijst}). */
    private final Map<Object, Map<String, Lijst>> lijsten = new IdentityHashMap<>();
    /** Every scalar input on a temporal attribute of the case, the asOfDate-bucket convention that
     *  {@link #temporeleInvoer} lays on the bean as one segment: what {@link #voegInvoerIn} names as bucket scalars
     *  ({@link Invoer#emmerscalair}). */
    private final List<Invoer.Emmer> emmers = new ArrayList<>();
    /** The case's evaluation date, read BEFORE the inputs are set: the asOfDate-bucket convention places a
     *  scalar input on a temporal attribute in the bucket this date falls in ({@link #temporeleInvoer}). */
    private LocalDate rekendatum;

    private String runCase(String naam, JsonNode c) {
        return runCase(naam, c, null);
    }

    /** One case: its verdict line, null on a match. With a {@link Waarneming} the replay also reports the firings and
     *  every expectation's verdict ({@link #rapport}); the verdict itself does not depend on it. */
    private String runCase(String naam, JsonNode c, Waarneming w) {
        labelNaarBean.clear();
        labelType.clear();
        lijsten.clear();
        emmers.clear();
        rekendatum = asOfDatum(c);
        ClassLoader vorige = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(cl);
        KieSession sessie = kieBase.newKieSession();
        try {
            JsonNode input = c.get("input");
            JsonNode instances = input.get("instances");
            for (JsonNode i : instances) beanVoor(i);
            for (JsonNode i : instances) zetAttributenEnKenmerken(i);
            for (JsonNode i : instances) zetRollen(i);
            spiegelWederkerigeRollen();
            zetGlobals(sessie, c);
            if (w != null) sessie.addEventListener(luisteraar(w));
            voegInvoerIn(sessie);
            String geenFixpunt = vuurTotFixpunt(sessie);
            if (geenFixpunt != null) return naam + ": " + geenFixpunt;
            List<String> mis = new ArrayList<>();
            int positie = 0;
            for (JsonNode v : c.get("expected")) {
                String m = vergelijk(v, sessie);
                if (m != null) mis.add(m);
                positie++;
                if (w != null) w.verwachtingen.add(new ScenarioResultaat.VerwachtingResultaat(
                        bron(v), verwacht(v), werkelijk(v, sessie), m == null, positie));
            }
            return mis.isEmpty() ? null : naam + ": " + String.join("; ", mis);
        } finally {
            sessie.dispose();
            Thread.currentThread().setContextClassLoader(vorige);
        }
    }

    /** Fires {@code sessie} through the backend's entry ({@link DroolsBackend#vuur}), under the cap a consumer gets, and
     *  returns null at a fixpoint. A run without one returns its sentence: at the cap on the DRL-text branch, whose
     *  rules write the beans as they fire, the writers of one attribute that kept overwriting each other with
     *  different values in the last firings (RUNTIME-SEMANTICS ch. 5.7, {@link Schrijfconflict}), and where those
     *  show none only that the cap was reached; on the exec-model branch, which writes no bean while it fires,
     *  the sentence of {@code vuur}, which names the cap and what it was computed from, or that of the creation budget. A
     *  consequence that throws leaves the fire as {@link DroolsBackend#benoem} names it; every replay, red path and
     *  report of this class fires here, on any branch.
     *  At a fixpoint the exec-model branch's derivations go onto the beans before anything reads them, or the
     *  evaluation faults that stand there are thrown ({@link DroolsBackend#zetAfgeleideWaarden}, a no-op on the
     *  DRL-text branch); without a fixpoint nothing is written back, since the replay reports the sentence and reads no bean. */
    private String vuurTotFixpunt(KieSession sessie) {
        // only the DRL-text branch writes the beans as it fires, so only its cap can come from writers that keep
        // overwriting each other; on the exec-model branch nothing reads the listener, and it cost every firing a call
        Schrijfconflict schrijvers = null;
        if (bouw.branch() == DroolsBackend.Branch.DRL_TEXT) {
            schrijvers = new Schrijfconflict(model, cl);
            sessie.addEventListener(schrijvers);
        }
        DroolsBackend.Vuring vuring;
        try {
            vuring = DroolsBackend.vuur(bouw, sessie);
        } catch (ConsequenceException e) {
            throw DroolsBackend.benoem(e);
        }
        if (vuring.geenFixpunt() == null) {
            DroolsBackend.zetAfgeleideWaarden(bouw, sessie, this::labelVanBean);
            return null;
        }
        if (schrijvers != null && vuring.vuringen() >= vuring.plafond()) {
            String conflict = schrijvers.conflict(this::labelVanBean);
            return conflict != null ? conflict : "geen fixpunt (kringverwijzing?)";
        }
        return vuring.geenFixpunt();
    }

    private Object beanVoor(JsonNode i) {
        String label = i.get("label").asText();
        Object bean = labelNaarBean.get(label);
        if (bean != null) return bean;
        String otNaam = i.get("objectType").asText();
        Objecttype ot = model.objecttypeOpNaam(otNaam);
        if (ot == null) throw new Excluded("case names an unknown objectType: " + otNaam);
        try {
            bean = Class.forName(FactModelGenerator.PAKKET + "." + Namen.ident(ot.naam, true), true, cl)
                    .getConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("kon bean voor label '" + label + "' niet maken", e);
        }
        labelNaarBean.put(label, bean);
        labelType.put(label, ot);
        return bean;
    }

    /** Whether {@code attr} on {@code ot} is a §13.3.6 temporal bean member — a {@code List<Segment>} field,
     *  the shape {@link FactModelGenerator} derives from a declared attribuutGranulariteit. */
    private boolean temporeel(Objecttype ot, String attr) {
        return Namen.bevatNaam(ot.granulariteitLeden(), attr);
    }

    /** Whether {@code attr} on {@code ot} is a §3.6 dimensioned bean member — a {@code Map<String, BigDecimal>}
     *  field. Checked BEFORE {@link #temporeel}: the generator gives a dimensioned declaration the Map shape
     *  even when a granularity is declared beside it. */
    private boolean gedimensioneerd(Objecttype ot, String attr) {
        return !ot.dimensiesVan(attr).isEmpty();
    }

    private void zetAttributenEnKenmerken(JsonNode i) {
        Object bean = labelNaarBean.get(i.get("label").asText());
        Objecttype ot = model.objecttypeOpNaam(i.get("objectType").asText());
        JsonNode attrs = i.get("attributes");
        if (attrs != null) for (Map.Entry<String, JsonNode> e : attrs.properties()) {
            Datatype dt = ot.attribuut(e.getKey());
            if (dt == null) throw new Excluded("case sets undeclared attribute '" + e.getKey() + "' on " + ot.naam);
            eigenVeld(ot, e.getKey(), false, "zet");
            if (gedimensioneerd(ot, e.getKey()))
                throw new Excluded("dimensioned attribute input '" + e.getKey() + "' (§3.6, a Map bean member) "
                        + "is not carried by the v0 backend");
            if (temporeel(ot, e.getKey())) {
                // the bean member was generated from the DECLARATION, so a case key spelled otherwise must be
                // brought back to it or it names a setter the bean does not have (ref reads it as the same name)
                roep(bean, Namen.setter(Namen.gedeclareerdeNaam(ot.granulariteitLeden().keySet(), e.getKey())), List.class,
                        temporeleInvoer(e.getValue(), e.getKey(), i.get("label").asText() + "." + e.getKey(),
                                rooster(ot, e.getKey())));
                if (!e.getValue().isNull() && !e.getValue().has("timeline"))
                    emmers.add(new Invoer.Emmer(bean, e.getKey()));
                continue;
            }
            roep(bean, Namen.setter(e.getKey()), argType(dt), waardeNaarArg(e.getValue(), e.getKey()));
        }
        JsonNode kenmerken = i.get("characteristics");
        if (kenmerken != null) for (JsonNode k : kenmerken) {
            // A CASE defect, not an uncarried form (maintainer decision 2026-09-09) — the sister arm below has
            // said so about the mirror-image mistake since it was written, and this arm is the same seam from
            // the other side. A TEMPORAL kenmerk's bean member is a List<Segment>, so a scalar-true assertion
            // reaches no reader here; in the interpreter and the ASP backend it reached none either, and BOTH
            // answered 0 where the golden pins 30 rather than saying anything (measured 2026-09-09 on
            // jutterswet-tegenwacht and anchor/jutterswet-dubbelwacht). All three readers refuse it at load
            // since that decision. It stayed an Excluded until then, which filed a broken case under "a form
            // the v0 backend does not carry" — a counted, reasoned refusal in the gate report for a case that
            // is simply wrong, and the one classification that could keep it out of sight.
            if (Namen.bevatNaam(ot.kenmerkGranulariteitLeden(), k.asText()))
                throw new IllegalArgumentException("scalar characteristic input '" + k.asText() + "' on the "
                        + "time-dependent kenmerk of " + ot.naam + ": deliver it as a characteristicTimelines "
                        + "entry — the static characteristics list asserts a scalar truth no temporal reader "
                        + "consults");
            eigenVeld(ot, k.asText(), true, "zet");
            roep(bean, Namen.setter(k.asText()), boolean.class, Boolean.TRUE);
        }
        JsonNode ctls = i.get("characteristicTimelines");
        if (ctls != null) for (Map.Entry<String, JsonNode> e : ctls.properties()) {
            // a CASE defect, not an uncarried form — ref refuses this shape at load (case.ml: "a
            // characteristic timeline on a non-temporal characteristic"), so it is hard here too; an
            // Excluded would file a broken corpus case under "not carried", silently green.
            if (!Namen.bevatNaam(ot.kenmerkGranulariteitLeden(), e.getKey()))
                throw new IllegalArgumentException("kenmerktijdlijn op niet-temporeel kenmerk '" + e.getKey() + "'");
            eigenVeld(ot, e.getKey(), true, "zet");
            roep(bean, Namen.setter(Namen.gedeclareerdeNaam(ot.kenmerkGranulariteitLeden().keySet(), e.getKey())), List.class,
                    kenmerkTijdreeks(e.getValue(), e.getKey()));
        }
    }

    private void zetRollen(JsonNode i) {
        Object bean = labelNaarBean.get(i.get("label").asText());
        Objecttype tI = model.objecttypeOpNaam(i.get("objectType").asText());
        JsonNode rollen = i.get("roles");
        if (rollen == null) return;
        for (Map.Entry<String, JsonNode> e : rollen.properties()) {
            for (JsonNode labelNode : e.getValue()) {
                Object doel = labelNaarBean.get(labelNode.asText());
                if (doel == null) throw new Excluded("role '" + e.getKey() + "' names unknown label " + labelNode.asText());
                Objecttype tL = objecttypeVanBean(doel);
                // §13.4.1 the WRITTEN role IS the navigation — and wherever the type pair carries more than
                // one role it is the ONLY route: the by-pair search below counts every candidate and derives
                // null by construction (RegelSpraakModel.verbindendeRol; ref BLOCKS the same derivation in
                // connecting_pair). That is the SELF-TYPED pair's two sibling roles and, since the repeated-pair
                // card, the ordinary cross-type pair related by several feittypen. Resolved through the very
                // authority FactModelGenerator.leden derives the fields from and ACCEPTED ONLY when it names one
                // of them (geldigRolveld), so a hit can only write a field the bean carries — that membership
                // test is what keeps this from being a general by-name override. Where the by-pair search already
                // succeeds the outcome is unchanged: a by-name hit is always one of the by-pair candidates, so a
                // unique pair resolves to the same role either way.
                String viaNaam = model.verbindendeRolViaNaam(tI, tL, e.getKey());
                boolean geschrevenVeld = viaNaam != null && geldigRolveld(tI, viaNaam, tL);
                // the field on THIS bean (to-one from here): the written role, else the type pair
                String rolIL = geschrevenVeld ? viaNaam : model.verbindendeRol(tI, tL);
                // The SISTER role — the one THIS instance fills — whose reference field lives on the TARGET
                // bean, and which is where a TO-MANY written role stores its links. Resolved BY NAME first,
                // exactly as rolIL is: the written role name names one feittype and its OTHER role is the
                // back-reference (verbindendeRolViaElementnaam). That is the only route wherever the by-pair
                // search counts more than one candidate — the self-typed pair's two siblings (the shape that
                // made W1{opvolgers:[…]} a per-form refusal although Wachter.voorligger exists) and the
                // repeated cross-type pair, where H1{liggers:[…]} must land on Schuit.thuishaven and not on
                // one of the three other Haven fields that bean now carries. RUNTIME-SEMANTICS ch. 5.7a makes
                // this write obligatory, not optional: the case names ONE leg of a relation that has two, and
                // the reader arriving from the other end must reach the same relation instance.
                //
                // Accepted only when the name lands on a field FactModelGenerator really derived
                // (geldigRolveld, the same membership test rolIL uses), so a written name that resolves to a
                // role WITHOUT a field keeps the honest refusal below instead of reaching a setter no bean
                // carries. Where the by-pair search already succeeds, a by-name hit is one of its candidates,
                // so a unique pair resolves to the same role either way.
                String zuster = model.verbindendeRolViaElementnaam(tI, e.getKey());
                boolean zusterVeld = zuster != null && geldigRolveld(tL, zuster, tI);
                String rolLI = zusterVeld ? zuster : model.verbindendeRol(tL, tI);
                if (rolIL != null && !model.naarVeelRol(tI, tL, rolIL)) {
                    naamdelingsPoort(tI, rolIL, tL, e.getKey());
                    zetToOne(bean, rolIL, doel);
                    lijst(doel, rolIL).gespiegeld.putIfAbsent(i.get("label").asText(), bean);
                    // RUNTIME-SEMANTICS ch. 5.7a, the 1:1 arm: a supplied link ESTABLISHES the relation and
                    // BOTH roles read it. With the sister role ALSO to-one the relation owns TWO bean fields —
                    // one per direction — so the forward write above leaves the far side reading a spuriously
                    // empty relation unless the second field is written too. That second write is the whole of
                    // the difference between a 1:1 and a 1:N shape here: at 1:N there is exactly ONE field and
                    // it already serves both directions (the else-branch below writes it), so nothing is left
                    // to mirror. ref's Case.materialise_relations makes the same write on the same role.
                    //
                    // rolLI == rolIL is the WEDERKERIG shape (§13.3.9.2: one role that is its own opposite),
                    // whose mirror is spiegelWederkerigeRollen's separate pass — untouched here, so its
                    // order-independence argument and its own conflict message keep holding.
                    if (rolLI != null && !rolLI.equalsIgnoreCase(rolIL) && !model.naarVeelRol(tL, tI, rolLI)) {
                        // The far field must really exist: a declared member of the same name displaces it
                        // (leden()'s dedup), and then this backend cannot hold the far direction at all. Under
                        // ch. 5.7a that is a per-form refusal with its reason, never a silently one-directional
                        // relation — the same standard the forward write is held to one line up.
                        naamdelingsPoort(tL, rolLI, tI, e.getKey());
                        zetToOne(doel, rolLI, bean);
                    }
                } else {
                    // the field lives on the target (this is the one side)
                    if (rolLI == null || model.naarVeelRol(tL, tI, rolLI))
                        // NOT UNIQUE and UNIQUE BUT TO-MANY are different failures and are reported apart (lens 2,
                        // 2026-08-30): the erflijn shape resolves `erfvolger` by name PERFECTLY, and what stops it
                        // is its CARDINALITY — a to-many role's storage is the far end's field — where the old
                        // single sentence blamed a non-unique derivation that had in fact succeeded.
                        throw new Excluded("role '" + e.getKey() + "' (" + tI.naam + " -> " + tL.naam + ") reaches no "
                                + "to-one reference field: " + (viaNaam == null
                                    ? "the written role name resolves to no unique role"
                                    : model.naarVeelRol(tI, tL, viaNaam)
                                    ? "the written role name resolves to '" + viaNaam + "', which is TO-MANY — its "
                                      + "storage is the far end's field, not a scalar reference here"
                                    : "the written role name resolves to the to-one role '" + viaNaam + "', which is "
                                      + "not one of the §13.3.9 reference fields the bean model derives")
                                + ", and " + (rolIL != null
                                    ? "this direction resolves the to-many role '" + rolIL + "'"
                                    : rolLI != null
                                    ? "the far direction resolves the to-many role '" + rolLI + "'"
                                    : "neither direction of the type pair derives a to-one role"));
                    naamdelingsPoort(tL, rolLI, tI, e.getKey());
                    zetToOne(doel, rolLI, bean);
                    Lijst l = lijst(bean, rolLI);
                    if (l.geschreven.stream().noneMatch(x -> x == doel)) l.geschreven.add(doel);
                }
            }
        }
    }

    /** One to-many list of the case input under the reference field of the holder's role {@code rol}: the members the
     *  case wrote under the holder, in the order it wrote them, and the members whose own link to the holder the reverse
     *  leg mirrors into it (RUNTIME-SEMANTICS ch. 5.7a), by label in ref's order, plain byte order. */
    private static final class Lijst {
        final String rol;
        final List<Object> geschreven = new ArrayList<>();
        final TreeMap<String, Object> gespiegeld = new TreeMap<>(
                Comparator.comparing((String l) -> l.getBytes(StandardCharsets.UTF_8), Arrays::compareUnsigned));

        Lijst(String rol) {
            this.rol = rol;
        }
    }

    private Lijst lijst(Object houder, String rol) {
        return lijsten.computeIfAbsent(houder, k -> new LinkedHashMap<>())
                .computeIfAbsent(Namen.ident(rol, false), k -> new Lijst(rol));
    }

    /**
     * Puts the case input into {@code sessie} through the backend's entry ({@link DroolsBackend#voegIn}), as a consumer
     * does: the beans, in the order of the case, every scalar input on a temporal attribute as a bucket scalar, and the
     * order of every to-many list. The bean shows such a scalar as the one segment of its bucket, which a full timeline
     * of one segment would be as well, while the exec-model branch reads the two conventions of
     * RUNTIME-SEMANTICS ch. 8.4 apart: the bucket scalar serves the subject's own total, the full timeline every other
     * read, and a form that needs the one and finds the other faults. The bean model keeps a to-many role as a reference
     * field on each member and no order, while that branch names the first member in fold order whose filter faults
     * (RUNTIME-SEMANTICS ch. 5.11). The order is that of the list the reference interpreter folds over: the written
     * members first, in the order the case wrote them, then the members whose own link the loader mirrors into the
     * list, in ascending label order (ch. 5.7a, "member ORDER"), a member in both counting once, where it was written.
     * The DRL-text branch reads the beans alone.
     */
    private void voegInvoerIn(KieSession sessie) {
        Invoer invoer = new Invoer().beans(labelNaarBean.values());
        for (Invoer.Emmer e : emmers) invoer.emmerscalair(e.bean(), e.attribuut());
        for (Map.Entry<Object, Map<String, Lijst>> perHouder : lijsten.entrySet())
            for (Lijst l : perHouder.getValue().values()) {
                List<Object> leden = new ArrayList<>(l.geschreven);
                for (Object lid : l.gespiegeld.values())
                    if (leden.stream().noneMatch(x -> x == lid)) leden.add(lid);
                invoer.lijst(perHouder.getKey(), l.rol, leden);
            }
        DroolsBackend.voegIn(bouw, sessie, invoer);
    }

    /** Whether {@code rol} is one of the reference fields {@link FactModelGenerator#leden} DERIVES on
     *  {@code drager} toward {@code doel} — the membership test that lets the WRITTEN role name override the
     *  by-pair search without ever reaching a setter no bean has. It asks the generator's own two derivations:
     *  {@code zelfpaarRollen} for a self-typed pair (§13.3.9.2 and the ordinary two-role self-pair) and
     *  {@code verbindendeRollen} for a cross-type one. Deliberately a NAME test, not
     *  {@link FactModelGenerator#heeftReferentieVeld}: a role whose name a declared member displaced is a
     *  DIFFERENT failure with its own sentence ({@link #naamdelingsPoort}), and collapsing the two here would
     *  send the name-sharing shape back through the by-pair route to be blamed for an ambiguity it does not
     *  have. */
    private boolean geldigRolveld(Objecttype drager, String rol, Objecttype doel) {
        return drager.naam.equalsIgnoreCase(doel.naam)
                ? FactModelGenerator.zelfpaarRollen(model, drager).contains(rol)
                : model.verbindendeRollen(drager, doel).contains(rol);
    }

    /** The §13.3.9 NAME-SHARING gate on a resolved to-one role write: {@code drager}'s bean must carry the
     *  reference field for {@code rol} TYPED (a field of {@code doel}'s class), not merely a property of that
     *  name. Both roles here come from the very authorities {@link FactModelGenerator#leden} derives the fields
     *  from ({@code zelfpaarRollen} / {@code verbindendeRollen} / {@code verbindendeRol}), so the ONE thing that
     *  can still be missing is the
     *  reference itself, displaced by a declared member of the same name (leden()'s dedup: attribute >
     *  characteristic > reference). Without this gate the write below reaches {@code set<X>(BigDecimal)} with a
     *  fact — "kon setter setOpvolger(Wachter) niet aanroepen", an unclassified ERROR the gate refuses to excuse
     *  — where the honest outcome is a loud, reason-carrying exclusion (lens 2, 2026-08-30). The §13.3.9.2
     *  wederkerig twin of this limit is refused in {@link DroolsBackend} for the whole model. */
    private void naamdelingsPoort(Objecttype drager, String rol, Objecttype doel, String geschreven) {
        if (FactModelGenerator.heeftReferentieVeld(model, drager, rol, doel)) return;
        // The cause is read off what holds the property, never assumed. Until 2026-09-28 every cause other than a
        // declared member was reported as a second role, and the commonest one was no second role at all: a target
        // of a SUBTYPE of the role's filler, which the field of the filler's class takes (IR-SPEC ch. 2.6) and which
        // the type gate then refused by equality. Two same-named roles toward different types
        // (corpus/rood/ambiguous-case-role-key) are the cause that sentence was written for, and it now names them.
        FactModelGenerator.Herkomst houder = FactModelGenerator.veldHerkomst(model, drager, rol);
        String veld = Namen.ident(drager.naam, true) + "." + Namen.ident(rol, false);
        String oorzaak;
        if (drager.heeftLid(rol)) oorzaak = "which shares its name with a declared member";
        else if (houder == null) oorzaak = "for which the bean model derives no field";
        else if (houder.soort() != FactModelGenerator.Herkomst.Soort.ROL)
            oorzaak = "whose bean property " + veld + " belongs to " + beschreven(houder);
        else if (!houder.equals(FactModelGenerator.rolHerkomst(model, drager, doel, rol)))
            oorzaak = "whose field " + veld + " another role already holds, " + beschreven(houder);
        else oorzaak = "whose field " + veld + " cannot hold a " + doel.naam;
        throw new Excluded("role '" + geschreven + "' resolves to the to-one role '" + rol + "' on " + drager.naam
                + ", " + oorzaak + " — the bean cannot carry the reference field "
                + "(name-sharing bean limit: attribute > characteristic > reference)");
    }

    /** A declaration in the words of this reader's refusals. */
    private static String beschreven(FactModelGenerator.Herkomst h) {
        return switch (h.soort()) {
            case ATTRIBUUT -> "the attribute '" + h.naam() + "' of " + h.bron();
            case KENMERK -> "the characteristic '" + h.naam() + "' of " + h.bron();
            case ROL -> "the role '" + h.naam() + "'" + (h.bron() == null ? "" : " of fact type '" + h.bron() + "'");
        };
    }

    /** One to-one role write, WF-013-guarded: a second, DIFFERENT filler on the same to-one field is a case
     *  shape ref refuses at load, so last-wins here would play a broken case green — the mirror pass's own
     *  argument, applied to the forward leg (lens 2, 2026-08-30). The same filler twice stays idempotent. */
    private void zetToOne(Object drager, String rol, Object doel) {
        Object huidig = lees(drager, Namen.getter(rol));
        if (huidig != null && huidig != doel)
            throw new IllegalArgumentException("to-one rol '" + rol + "' krijgt een tweede, ander doel — "
                    + "ref weigert die case bij het laden (WF-013)");
        roep(drager, Namen.setter(rol), veldklasse(drager, rol), doel);
    }

    /** The class of the reference field {@code rol} on {@code drager}'s bean, read off its getter: the class of the
     *  role's declared filler. The setter is looked up by that class and not by the target's own, because a target
     *  of a subtype of the filler (IR-SPEC ch. 2.6) has a class of its own that no setter names. */
    private static Class<?> veldklasse(Object drager, String rol) {
        try {
            return drager.getClass().getMethod(Namen.getter(rol)).getReturnType();
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException("geen getter " + Namen.getter(rol) + " op "
                    + drager.getClass().getSimpleName(), e);
        }
    }

    /** §13.3.9.2 mirrored: ONE reciprocal entry holds in BOTH directions, so every self-pair leg the case wrote
     *  gets its reverse materialised — exactly what {@code Case.materialise_relations} does in ref, and what
     *  RUNTIME-SEMANTICS ch. 4.1 requires ("the pinned runtime writes both to-one legs so navigation never reads
     *  a spuriously empty side"). Both goldens pin it: a partner that declares no roles of its own still reads its
     *  side, and the reverse read {@code Jutter( maat == $subject )} an aggregation lowers to needs the leg
     *  present. Run as a SEPARATE PASS over the finished links, like ref's, so the outcome cannot depend on the
     *  order instances appear in: a conflict must be seen wherever the colliding leg was written. Idempotent
     *  (a case supplying both legs means one relation), and a CONFLICTING leg on the to-one role is hard — ref
     *  refuses that case at load (WF-013), so overwriting it here would file a broken case under a green replay.
     *
     *  <p>Scoped to the WEDERKERIG shape, and since 2026-09-10 that scope is a DIVISION OF LABOUR rather than
     *  the law it used to state. RUNTIME-SEMANTICS ch. 5.7a (maintainer decision) makes EVERY supplied link
     *  establish its relation in both directions, and ref's {@code Case.materialise_relations} mirrors on the
     *  SISTER role accordingly. Three shapes, three answers, all landed:
     *  <ul>
     *    <li><b>1:N</b> — the relation has exactly ONE reference field and it necessarily serves both
     *        directions, so there is no second write to make. {@link #zetRollen} lands the case's write on that
     *        field through the sister role's NAME, and the three spellings of one relation give one fact base
     *        ({@code ZusterrolInvoerWitnessTest}, over the {@code jutterswet-ketenband} golden).</li>
     *    <li><b>1:1</b> — each direction owns its own field, so ch. 5.7a needs a SECOND write on the target
     *        bean. {@link #zetRollen} makes it, right after the forward write, on the sister role and under the
     *        same name-sharing gate. That closed the last ref ≢ Drools flank of the decision: a case writing
     *        {@code W1{opvolger:[W2]}} over a to-one/to-one self-pair now reads W1 as W2's voorganger here, as
     *        it does in ref ({@code OpslagrichtingWitnessTest}, converted at that decision).</li>
     *    <li><b>wederkerig</b> (§13.3.9.2, one role that is its own opposite) — THIS pass, and deliberately
     *        still a separate one: its order-independence argument above is the reason, and the 1:1 arm skips
     *        the shape (it fires only when the sister role is a DIFFERENT role) so the two never collide.</li>
     *  </ul> */
    private void spiegelWederkerigeRollen() {
        for (Object bean : labelNaarBean.values()) {
            // the reciprocal role of an instance of a SUBTYPE of its filler is its ancestor's (IR-SPEC ch. 2.6):
            // asked of the bean's own type alone, such a leg went unmirrored and the partner read an empty relation
            String rol = null;
            for (Objecttype t : objecttypeVanBean(bean).keten())
                if ((rol = FactModelGenerator.zelfpaarRol(model, t)) != null) break;
            if (rol == null) continue;
            Object partner = lees(bean, Namen.getter(rol));
            if (partner == null) continue;
            Object terug = lees(partner, Namen.getter(rol));
            if (terug == bean) continue;
            if (terug != null)
                throw new IllegalArgumentException("wederkerige rol '" + rol + "': het doel is al verbonden met "
                        + "een ander — de omgekeerde poot botst (WF-013 to-one)");
            roep(partner, Namen.setter(rol), veldklasse(partner, rol), bean);
        }
    }

    private Objecttype objecttypeVanBean(Object bean) {
        for (Objecttype ot : model.objecttypen.values())
            if (Namen.ident(ot.naam, true).equals(bean.getClass().getSimpleName())) return ot;
        throw new IllegalStateException("geen objecttype voor bean " + bean.getClass());
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
        // them); empty for the exec branch, so this loop is a no-op on every gate path.
        for (Map.Entry<String, java.util.function.Predicate<LocalDate>> e : dagsoortGlobalen.entrySet())
            setGlobalVeilig(sessie, e.getKey(), e.getValue());
    }

    /** The case's {@code asOfDate}, or null when it carries none. */
    private static LocalDate asOfDatum(JsonNode c) {
        JsonNode asOf = c.get("asOfDate");
        return asOf == null || asOf.isNull() ? null : datumVanCaseWire(asOf.asText(), "asOfDate");
    }

    private static void setGlobalVeilig(KieSession sessie, String naam, Object waarde) {
        try { sessie.setGlobal(naam, waarde); }
        catch (RuntimeException onbekend) { /* global not declared: no rule reads it */ }
    }

    // ---------------- §13.3.6 temporal input (the List<Segment> bean members) ----------------

    /** The {@code List<Segment>} a case value denotes for a temporal attribute at {@code gran} — the two input
     *  forms {@code ref} carries (ref/lib/case.ml), mirrored so both executors read the SAME timeline:
     *  <ul>
     *    <li>a {@code {"timeline":[…]}} value: the written segments, validated exactly as ref validates them;</li>
     *    <li>a SCALAR value: the asOfDate-bucket convention recorded in {@code corpus/golden/README.md} — the
     *        scalar IS the value of the bucket the evaluation date falls in. ref keeps that scalar aside and
     *        folds precisely this one-bucket timeline inside the ch. 3.6 collapse ({@code Eval}'s temporal
     *        total: value × whole units of its own grid); the bean model has no scalar slot beside the
     *        {@code List<Segment>} field, so the bucket is materialised here and
     *        {@link RegelSpraakRuntime#rsTijdTotaal} folds the identical segment to the identical scalar.</li>
     *  </ul>
     *  Deliberate consequence of that materialisation: the bean now carries a shape ref REFUSES to serve —
     *  ref has no point-in-time read at all (ch. 3.3: a temporal attribute in a scalar slot is refused, the
     *  collapses are the only bridges), and its no-filter ch. 3.6 total even faults on a case-supplied FULL
     *  timeline ("the asOfDate-bucket convention carries the total", eval.ml). The divergence surface is thus
     *  not a wrong value but a read Drools would answer where ref faults; such a case can never become a
     *  golden, because gate 5 replays every case through ref first — the executors agree wherever the corpus
     *  looks, by construction. That holds for the DRL-text branch, which reads the bean alone;
     *  for the exec-model branch the caller records each scalar as a bucket scalar, which
     *  {@link #voegInvoerIn} names to {@link DroolsBackend#voegIn}, and that branch reads the two conventions apart and faults
     *  where a form needs the one and finds the other (RUNTIME-SEMANTICS ch. 8.4). */
    private List<Object> temporeleInvoer(JsonNode w, String attr, String plek, Rooster gran) {
        if (w == null || w.isNull()) return null;   // an absent value is an absent timeline (leeg, ch. 3.1)
        if (w.has("timeline")) return tijdreeks(w.get("timeline"), attr, plek, gran, false);
        if (rekendatum == null)
            throw new Excluded("scalar input on temporal attribute '" + attr + "' without an asOfDate: the "
                    + "bucket convention (corpus/golden/README.md) has no evaluation date to place it in");
        LocalDate[] emmer = emmer(gran, rekendatum);
        return List.of(segment(emmer[0], emmer[1], segmentWaarde(w, attr)));
    }

    /** The segments of a timeline value, with ref's load-time validation mirrored: at least one segment (an
     *  EXPECTATION may be empty — {@code leegToegestaan}), {@code from} before {@code to}, both boundaries on the
     *  declared granularity grid, and the list ordered and non-overlapping as written. A malformed timeline is a
     *  hard failure, never a silent acceptance: the case schema (gate 4) and ref (gate 5) reject it first, so
     *  reaching it here means the corpus and this reader disagree. A grid that carries no case timeline is a
     *  not-carried form, refused where ref's loader refuses it and in its sentence, at position {@code plek}
     *  ({@code J1.m2} for an input, {@code expected J1.m2} for an expectation): a grid of several units and a
     *  variable start before any segment is read, the calendar's day, week and quarter grid at the first segment. */
    private List<Object> tijdreeks(JsonNode segs, String veld, String plek, Rooster gran, boolean leegToegestaan) {
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

    /** The TRUE-period {@code List<Segment>} a {@code characteristicTimelines} entry denotes. True periods are
     *  DAY-precise regardless of the declared granularity (ref: the granularity governs the kenmerk's bucketing,
     *  never its period grid — the ch. 3.7 pro-rata's whole point), so no grid check applies; ref sorts the
     *  periods and rejects overlap, and this does the same. The segments carry a null waarde: the runtime builds
     *  its own true-period timelines that way (rsTijdOfUnie) and every consumer reads van/tot only (rsTijdKnip). */
    private List<Object> kenmerkTijdreeks(JsonNode periodes, String kenmerk) {
        if (periodes == null || !periodes.isArray())
            throw new IllegalArgumentException("misvormde kenmerktijdlijn op '" + kenmerk + "'");
        List<LocalDate[]> gesorteerd = new ArrayList<>();
        for (JsonNode pj : periodes) {
            LocalDate van = grensDatum(pj, "from", kenmerk), tot = grensDatum(pj, "to", kenmerk);
            if (!van.isBefore(tot))
                throw new IllegalArgumentException("periode 'to' op '" + kenmerk + "' ligt niet na 'from'");
            gesorteerd.add(new LocalDate[]{ van, tot });
        }
        gesorteerd.sort((a, b) -> a[0].equals(b[0]) ? a[1].compareTo(b[1]) : a[0].compareTo(b[0]));
        List<Object> uit = new ArrayList<>();
        LocalDate vorigeTot = null;
        for (LocalDate[] p : gesorteerd) {
            if (vorigeTot != null && p[0].isBefore(vorigeTot))
                throw new IllegalArgumentException("perioden op '" + kenmerk + "' overlappen");
            vorigeTot = p[1];
            uit.add(segment(p[0], p[1], null));
        }
        return uit;
    }

    /** The grid a temporal member's case values sit on: its facet's granularity and count, its fixed start point,
     *  and whether it rides a timeline with a variable start (RUNTIME-SEMANTICS ch. 3.2). */
    private record Rooster(Granulariteit gran, int aantal, String vast, boolean variabel) {}

    private Rooster rooster(Objecttype ot, String attr) {
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
    private static LocalDate[] emmer(Rooster r, LocalDate d) {
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
    private BigDecimal segmentWaarde(JsonNode w, String veld) {
        Object v = waardeNaarArg(w, veld);
        if (v == null || v instanceof BigDecimal) return (BigDecimal) v;
        throw new Excluded("non-numeric timeline value on '" + veld + "' is not carried by the v0 backend — "
                + "the generated Segment holds a BigDecimal waarde");
    }

    private static LocalDate grensDatum(JsonNode seg, String veld, String waar) {
        JsonNode d = seg == null ? null : seg.get(veld);
        if (d == null || !d.isTextual())
            throw new IllegalArgumentException("segment op '" + waar + "' mist een '" + veld + "'-datum");
        // the field name carries BOTH coordinates: a case may hold many timelines and each holds two
        // boundaries, so "onderhoudstarief.from" is the only form a producer can act on
        return datumVanCaseWire(d.asText(), waar + "." + veld);
    }

    /** One {@code Segment} fact of the generated bean model, built through the runtime's own reflective
     *  constructor — the Segment class is a runtime objecttype, unknown at compile time. */
    private Object segment(LocalDate van, LocalDate tot, BigDecimal waarde) {
        try {
            return RegelSpraakRuntime.newSegment(
                    Class.forName(FactModelGenerator.PAKKET + ".Segment", true, cl), van, tot, waarde);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("de gegenereerde Segment-factklasse ontbreekt", e);
        }
    }

    // ---------------- comparison ----------------

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
        Objecttype ot = labelType.get(label);
        eigenVeld(ot, attr, false, "leest");
        if (ot != null && gedimensioneerd(ot, attr))
            throw new Excluded("dimensioned attribute read-back '" + attr + "' (§3.6, a Map bean member) "
                    + "is not carried by the v0 backend");
        // a temporal attribute's bean member is the List<Segment> timeline itself: the scalar comparison below
        // would compare a list against a scalar. Its read-back form is the attributeTimeline expectation.
        if (ot != null && temporeel(ot, attr))
            throw new Excluded("scalar read-back of temporal attribute '" + attr + "' is not carried by the v0 "
                    + "backend — the bean holds a List<Segment>; use an attributeTimeline expectation");
        Object bean = labelNaarBean.get(label);
        Object werkelijk = bean == null ? null : lees(bean, Namen.getter(attr));
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
        Object gelezen = labelNaarBean.containsKey(label) ? lees(labelNaarBean.get(label), Namen.getter(attr)) : null;
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
        eigenVeld(labelType.get(v.get("label").asText()), v.get("characteristic").asText(), true, "leest");
        Object bean = labelNaarBean.get(v.get("label").asText());
        Object gelezen = bean == null ? null : lees(bean, Namen.getter(v.get("characteristic").asText()));
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

    private String renderIncs(List<Object> facts) {
        List<String> uit = new ArrayList<>();
        for (Object f : facts) uit.add(lees(f, "getRegel") + " @ " + labelVanBean(lees(f, "getSubject")));
        return String.join(", ", uit);
    }

    // ---------------- the report's renderings ----------------

    /** Every firing, with the labels of the input beans the match bound, under the form of the document its rule
     *  belongs to ({@link TmsTranspiler#vormVan}), its name whole but for a version suffix
     *  ({@link RegelSpraakModel#vormnaam}): one row per form, the versions of one rule in one row, as before card
     *  tms-omschakeling, when every rule of the destructive lowering carried the name of its form. The coverage
     *  ({@link #gevuurd}) takes the exact name, version included. The rules of the exec-model branch are named "&lt;form&gt;: &lt;part&gt;", so the part alone, which the
     *  report gave until the repair, put rules of different forms in one row: on golden jutterswet-premiestaffel the
     *  defaults of three object types under "default 0". A rule of no form, a standing or another rule the lowering
     *  keeps for every form alike ("kenmerkstand Jutter.uitgesloten: waar uit een afleiding"), is not reported: it is
     *  no rule of the document, the report before the switch-over had none, and a consumer that names a row after a
     *  form of the document has none to name it after. */
    private DefaultAgendaEventListener luisteraar(Waarneming w) {
        TmsTranspiler tms = bouw.execModelTranspiler();
        if (tms == null) throw new IllegalStateException("het scenariorapport speelt alleen de exec-model-tak");
        return new DefaultAgendaEventListener() {
            @Override public void afterMatchFired(AfterMatchFiredEvent e) {
                String naam = e.getMatch().getRule().getName();
                String vorm = tms.vormVan(naam);
                if (vorm == null) return;
                w.vormen.add(vorm);
                String regel = RegelSpraakModel.vormnaam(vorm);
                w.telling.merge(regel, 1, Integer::sum);
                List<String> ls = w.labels.computeIfAbsent(regel, k -> new ArrayList<>());
                for (Object o : e.getMatch().getObjects()) {
                    String l = labelVanBean(o);
                    if (l != null && !ls.contains(l)) ls.add(l);
                }
            }
        };
    }

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

    /** The actual side after fire, read where the comparison reads it. */
    private String werkelijk(JsonNode v, KieSession sessie) {
        Object bean = v.hasNonNull("label") ? labelNaarBean.get(v.get("label").asText()) : null;
        return switch (v.get("kind").asText()) {
            case "attributeValue", "attributeEmpty" ->
                    render(bean == null ? null : lees(bean, Namen.getter(v.get("attribute").asText())));
            case "attributeTimeline" -> {
                Object gelezen = bean == null ? null : lees(bean, Namen.getter(v.get("attribute").asText()));
                yield gelezen == null ? "(leeg)" : renderTl((List<?>) gelezen);
            }
            case "characteristicPresent", "characteristicAbsent" ->
                    bean != null && Boolean.TRUE.equals(lees(bean, Namen.getter(v.get("characteristic").asText())))
                            ? "aanwezig" : "afwezig";
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
    private Object waardeNaarArg(JsonNode w, String veld) {
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

    private static Class<?> argType(Datatype dt) {
        if (dt == null) return Object.class;
        if (dt.isNumeriek() || dt.isPercentage()) return BigDecimal.class;
        if (dt.isTekst() || dt instanceof Datatype.Enumeratie) return String.class;
        if (dt instanceof Datatype.Booleaan) return Boolean.class;
        if (dt instanceof Datatype.DatumInDagen) return LocalDate.class;
        if (dt instanceof Datatype.DatumTijd) return LocalDateTime.class;
        return Object.class;
    }

    private static void roep(Object bean, String methode, Class<?> type, Object waarde) {
        try { bean.getClass().getMethod(methode, type).invoke(bean, waarde); }
        catch (ReflectiveOperationException e) {
            throw new RuntimeException("kon setter " + methode + "(" + type.getSimpleName() + ") niet aanroepen op "
                    + bean.getClass().getSimpleName(), e);
        }
    }

    private static Object lees(Object bean, String getter) {
        for (String mth : new String[]{ getter, "is" + getter.substring(3) }) {
            try { return bean.getClass().getMethod(mth).invoke(bean); }
            catch (NoSuchMethodException ignore) { /* try next form */ }
            catch (ReflectiveOperationException e) { throw new RuntimeException("kon " + mth + " niet lezen", e); }
        }
        throw new IllegalStateException("geen getter " + getter + " op " + bean.getClass().getSimpleName());
    }

    private static String render(Object o) { return o == null ? "leeg" : o.toString(); }
}
