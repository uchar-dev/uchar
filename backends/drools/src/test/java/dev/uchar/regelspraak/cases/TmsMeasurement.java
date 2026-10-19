package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import dev.uchar.regelspraak.transpiler.tms.CreationPhase;
import dev.uchar.regelspraak.transpiler.tms.ControlFact;
import dev.uchar.regelspraak.transpiler.tms.TmsTranspiler;

import org.kie.api.KieBase;
import org.kie.api.definition.KiePackage;
import org.kie.api.event.rule.AfterMatchFiredEvent;
import org.kie.api.event.rule.AgendaEventListener;
import org.kie.api.event.rule.DefaultAgendaEventListener;
import org.kie.api.event.rule.ObjectDeletedEvent;
import org.kie.api.event.rule.ObjectInsertedEvent;
import org.kie.api.event.rule.ObjectUpdatedEvent;
import org.kie.api.event.rule.RuleRuntimeEventListener;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.ToDoubleFunction;
import java.util.stream.Stream;

/**
 * The measurement of card tms-omschakeling, point 7: every golden that the exec-model branch plays, for the service of
 * the distribution, which fires one session per request. Until the switch it set the destructive lowering against the
 * one with truth maintenance; those numbers are in the card's {@code Result}. A harness, not a witness: wall-clock
 * times depend on the machine and its load, so no test may assert them, and the coverage of the corpus is a report,
 * which {@code TmsCorpusWitnessTest} holds to a floor. Its name therefore ends without {@code Test}: surefire does not run it,
 * and no gate does either.
 *
 * <p>Per golden: the firings; the time of the fire and that of the write-back, apart; the
 * build of the document; R, the rules in the KieBase; N0, the facts in the session just before the fire; and the
 * largest firings / (R × N0) of one case, from which the factor of the fire cap is set ({@code DroolsBackend.FACTOR}):
 * the cap holds per session, so per case. A model with a creation fires in phases, and the driver computes the cap
 * again over the facts at the start of every phase, so beside it the harness gives the factor a case needs under that
 * cap: per stretch of the run, from its start or the start of a phase to the next, the firings at its end over R times
 * the largest N the cap has counted by then, the largest of those; it equals firings / (R × N0) without a creation.
 * One extra fire per case measures it, with listeners, through the seam with a cap no golden reaches, and holds its
 * phases to the cap the driver computed. Every case is set up as the replay sets it up ({@link CaseRunner#withSession},
 * through {@link DroolsBackend#insert}), and fired and written back through the backend's entry as a consumer does:
 * {@link DroolsBackend#fire} under the cap a consumer gets, then {@link DroolsBackend#setDerivedValues}, each timed
 * on its own. The replay's listener for two writers of one attribute is not on the session, as with a consumer. Then
 * the sums and medians, the goldens with the largest ratio, and per defaults group its rules and control facts; last,
 * every golden of the corpus with case files through
 * {@link CaseRunner#viaExecModel}: played, failed, or refused, the refusals grouped by family and by form.
 *
 * <p>Run it from the root of the repository, with the test classpath from the dependency plugin, which resolves
 * offline once the module has been built:
 * <pre>
 *   mvn -o -q -f backends/drools/pom.xml test-compile
 *   CP=$(mktemp -d)/cp.txt
 *   mvn -o -q -f backends/drools/pom.xml dependency:build-classpath -Dmdep.outputFile="$CP"
 *   java -Xmx16g -cp "backends/drools/target/test-classes:backends/drools/target/classes:$(cat "$CP")" \
 *       dev.uchar.regelspraak.cases.TmsMeasurement [corpus/golden] &gt; tabel.md
 * </pre>
 * It prints Markdown on standard output and one line per golden on standard error as it goes.
 */
public final class TmsMeasurement {

    private TmsMeasurement() {}

    private static final ObjectMapper JSON = new ObjectMapper();

    /** Per case: the fires to warm up and the measured fires, of which the median counts; per document the builds, of
     *  which the median counts; and the fires per case of the round over every measured golden before the first
     *  measurement, which warms up the engine the documents share. */
    private static final int WARM_UP = 20, REPETITIONS = 31, BUILD_ROUNDS = 5, RONDE = 3;

    private static final DroolsBackend.Branch BRANCH = DroolsBackend.Branch.EXEC_MODEL;

    public static void main(String[] args) throws Exception {
        Path golden = Path.of(args.length > 0 ? args[0] : "corpus/golden");
        if (!Files.isDirectory(golden))
            throw new IllegalArgumentException("geen map " + golden.toAbsolutePath()
                    + ": draai vanuit de wortel van de repository, of geef de map corpus/golden mee");
        List<Path> directories;
        try (Stream<Path> s = Files.walk(golden)) {
            directories = s.filter(p -> p.getFileName().toString().equals("ir.json")).map(Path::getParent).sorted().toList();
        }
        List<Golden> goldens = new ArrayList<>();
        for (Path map : directories) {
            List<Path> paths = cases(map);
            if (!paths.isEmpty()) goldens.add(new Golden(golden.relativize(map).toString(), map, paths));
        }
        for (Golden g : goldens) g.judge();
        List<Golden> gemeten = goldens.stream().filter(Golden::plays).toList();
        rondeVooraf(gemeten);
        int k = 0;
        for (Golden g : gemeten) {
            g.meet();
            System.err.println("tmsmeting: " + ++k + "/" + gemeten.size() + " " + g.name);
        }
        StringBuilder out = new StringBuilder();
        head(out);
        table(gemeten, out);
        samenvatting(gemeten, out);
        defaultsGroups(gemeten, out);
        coverage(directories.size(), goldens, out);
        System.out.print(out);
    }

    private static void head(StringBuilder out) {
        Runtime rt = Runtime.getRuntime();
        out.append("# Meting van de exec-model-tak over het corpus (kaart tms-omschakeling, punt 7)\n\n");
        out.append("- JVM: ").append(System.getProperty("java.vm.name")).append(' ')
                .append(System.getProperty("java.runtime.version")).append(" (").append(System.getProperty("java.vm.vendor"))
                .append("), ").append(System.getProperty("os.arch")).append(", ").append(rt.availableProcessors())
                .append(" processors, max heap ").append(rt.maxMemory() >> 20).append(" MB.\n");
        out.append("- Tak: de exec-model-tak met truth maintenance (`").append(BRANCH).append("`). Gemeten: elke golden met")
                .append(" cases die hij speelt (het oordeel van de replay).\n");
        out.append("- Opzet per case: een verse sessie, gevuld zoals de replay dat doet (`CaseRunner.forMeasurement`, via")
                .append(" `DroolsBackend.insert`); dan `DroolsBackend.fire` onder het plafond dat een afnemer krijgt en")
                .append(" `DroolsBackend.setDerivedValues`, elk apart getimed. Niet getimed: sessie, beans, invoegen.")
                .append(" De schrijfconflictluisteraar van de replay hangt niet aan de sessie, zoals bij een afnemer.\n");
        out.append("- Herhalingen: eerst een ronde over alle gemeten goldens, ").append(RONDE)
                .append(" keer per case (opwarmen van de gedeelde engine); dan per golden: bouw ")
                .append(BUILD_ROUNDS).append(" keer `DroolsBackend.build` (laden, beanklassen, verlagen, KieBase; de mediaan; de")
                .append(" beanklassen worden per JVM op inhoud gecachet, dus javac telt niet mee), en per case ").append(WARM_UP)
                .append(" opwarmers en ").append(REPETITIONS).append(" herhalingen; per case de mediaan,")
                .append(" per golden de som van de medianen.\n");
        out.append("- Vuringen: wat `vuur` teruggeeft, opgeteld over de cases (een bereik als herhalingen verschillen).")
                .append(" R: de regels van de KieBase. N0: de feiten in de sessie vlak vóór het vuren (ook de")
                .append(" `Emmerinvoer`- en `Lijstplaats`-feiten van `insert`), opgeteld over de cases. max v/(R×N0):")
                .append(" het grootste vuringen / (R × N0) van één case, want het plafond geldt per")
                .append(" sessie; \"–\" waar elke case N0 = 0 heeft. max v/(R×N) per fase: wat een case van de factor")
                .append(" van het plafond vraagt nu de driver het plafond bij elke creatiefase opnieuw uitrekent over de")
                .append(" feiten op dat moment (zie de samenvatting); gemeten in één extra run per case, niet getimed.\n");
        out.append("- Wandkloktijden hangen af van machine en belasting.\n\n");
    }

    // ---------------- the measurement ----------------

    /** One golden: its document, its cases without the red-path ones, the replay's verdict, and what the branch
     *  measured where it plays it. */
    private static final class Golden {
        final String name;
        final Path ir;
        final List<Path> paths;
        final List<JsonNode> cases = new ArrayList<>();
        final List<String> caseNames = new ArrayList<>();
        CaseRunner.Result verdict;
        Branch branch;

        Golden(String name, Path map, List<Path> paths) throws Exception {
            this.name = name;
            this.ir = map.resolve("ir.json");
            this.paths = paths;
            for (Path p : paths) {
                JsonNode c = JSON.readTree(Files.readString(p));
                if (c.has("expectedFault")) continue;
                cases.add(c);
                caseNames.add(p.getFileName().toString());
            }
        }

        /** The replay's verdict; an exception out of the replay is a FAIL with its sentence. */
        void judge() {
            try {
                verdict = CaseRunner.viaExecModel(ir, paths);
            } catch (RuntimeException e) {
                verdict = CaseRunner.Result.played(0, List.of(String.valueOf(e)));
            }
        }

        boolean plays() {
            return verdict.status().equals("PLAYED");
        }

        /** Builds the document, then fires every case. */
        void meet() throws Exception {
            branch = Branch.built(ir, BRANCH);
            for (int c = 0; c < cases.size(); c++) {
                List<FireMeasurement> ms = new ArrayList<>();
                for (int i = -WARM_UP; i < REPETITIONS; i++) {
                    FireMeasurement m = fire(this, c, BRANCH, branch.runner, i == REPETITIONS - 1);
                    if (i >= 0) ms.add(m);
                }
                Phases f = phases(this, c, BRANCH, branch.runner, ms.stream().mapToInt(FireMeasurement::firings).max().orElseThrow());
                branch.count(this, caseNames.get(c), ms, f);
            }
            // the KieBase goes once its golden is measured
            branch.runner = null;
        }
    }

    /** One fire of one case: its firings and the cap it fired under, the fire and the write-back in nanoseconds, the
     *  facts just before the fire, and, where asked, every fact the session held after the fire. */
    private record FireMeasurement(int firings, int cap, long fireNanos, long writeBackNanos, long n0,
                              List<Object> sessionFacts) {}

    /** Fires case {@code c} of {@code g} on {@code branch} in a fresh session and measures it; any fault names the golden,
     *  the case and the branch. */
    private static FireMeasurement fire(Golden g, int c, DroolsBackend.Branch branch, CaseRunner runner, boolean sessionFacts) {
        try {
            return runner.withSession(g.cases.get(c), (build, session, labels) -> {
                long n0 = session.getFactCount();
                long begin = System.nanoTime();
                DroolsBackend.Firing firing = DroolsBackend.fire(build, session);
                long fired = System.nanoTime();
                if (firing.noFixpoint() != null) throw new IllegalStateException(firing.noFixpoint());
                List<Object> state = sessionFacts ? new ArrayList<>(session.getObjects()) : null;
                long writeBackStart = System.nanoTime();
                DroolsBackend.setDerivedValues(build, session);
                long writtenBack = System.nanoTime();
                return new FireMeasurement(firing.firings(), firing.cap(), fired - begin, writtenBack - writeBackStart, n0, state);
            });
        } catch (RuntimeException e) {
            throw new IllegalStateException(g.name + ", " + g.caseNames.get(c) + " op " + branch + ": " + e, e);
        }
    }

    /** What the run of one case needs of the factor of the fire cap: {@code ratio}, the largest over its stretches
     *  of the firings at the end of a stretch over R times the largest number of facts the cap has counted by then,
     *  with those firings and that number; -1 where every count is 0. */
    private record Phases(double ratio, long firings, long n) {}

    /** Fires case {@code c} of {@code g} on {@code branch} once more, through the seam with the factor 1000 and floor 1, a
     *  cap no golden reaches that the driver computes like the public one, with a listener on the firings and one on
     *  the facts that marks every {@code CreationPhase} the driver inserts, with the firings so far and the facts the
     *  session then holds: the driver computes the cap again right after that insertion, over the same count. The run
     *  must give the firings of the measured fires, its listener must count what the driver reports, and the cap it
     *  ends with must be 1000 × R times the largest of the counts; else the measurement fails. */
    private static Phases phases(Golden g, int c, DroolsBackend.Branch branch, CaseRunner runner, int firings) {
        final double factor = 1000;
        try {
            return runner.withSession(g.cases.get(c), (build, session, labels) -> {
                long rules = rules(build.kieBase());
                long[] fired = {0};
                List<long[]> starts = new ArrayList<>();
                starts.add(new long[] {0, session.getFactCount()});
                AgendaEventListener counter = new DefaultAgendaEventListener() {
                    @Override public void afterMatchFired(AfterMatchFiredEvent e) {
                        fired[0]++;
                    }
                };
                RuleRuntimeEventListener phase = new RuleRuntimeEventListener() {
                    @Override public void objectInserted(ObjectInsertedEvent e) {
                        if (e.getObject() instanceof CreationPhase) starts.add(new long[] {fired[0], session.getFactCount()});
                    }

                    @Override public void objectUpdated(ObjectUpdatedEvent e) {}

                    @Override public void objectDeleted(ObjectDeletedEvent e) {}
                };
                session.addEventListener(counter);
                session.addEventListener(phase);
                DroolsBackend.Firing firing = DroolsBackend.fire(build, session, factor, 1);
                session.removeEventListener(counter);
                session.removeEventListener(phase);
                if (firing.noFixpoint() != null) throw new IllegalStateException(firing.noFixpoint());
                if (firing.firings() != firings || fired[0] != firings)
                    throw new IllegalStateException("de run met luisteraars vuurde " + firing.firings() + " keer, de"
                            + " luisteraar telde " + fired[0] + ", de gemeten runs " + firings);
                double highest = -1;
                long highestFirings = 0, highestN = 0, largest = 0;
                for (int k = 0; k < starts.size(); k++) {
                    largest = Math.max(largest, starts.get(k)[1]);
                    long end = k + 1 < starts.size() ? starts.get(k + 1)[0] : firing.firings();
                    if (largest == 0) continue;
                    double v = end / ((double) rules * largest);
                    if (v > highest) {
                        highest = v;
                        highestFirings = end;
                        highestN = largest;
                    }
                }
                long cap = (long) Math.min(Integer.MAX_VALUE, Math.max(1, Math.ceil(factor * rules * largest)));
                if (firing.cap() != cap)
                    throw new IllegalStateException("het plafond van de driver is " + firing.cap() + ", de fasen geven "
                            + factor + " × " + rules + " × " + largest + " = " + cap);
                return new Phases(highest, highestFirings, highestN);
            });
        } catch (RuntimeException e) {
            throw new IllegalStateException(g.name + ", " + g.caseNames.get(c) + " op " + branch + " (fasen): " + e, e);
        }
    }

    /** The round over every measured golden before the first measurement: the engine and the build that all documents
     *  share are warm once the first golden is measured. Nothing of it is kept. */
    private static void rondeVooraf(List<Golden> gemeten) throws Exception {
        for (Golden g : gemeten) {
            CaseRunner runner = CaseRunner.forMeasurement(g.ir, BRANCH);
            for (int c = 0; c < g.cases.size(); c++)
                for (int i = 0; i < RONDE; i++) fire(g, c, BRANCH, runner, false);
        }
    }

    /** What the branch measured on one golden. */
    private static final class Branch {
        CaseRunner runner;
        double buildMs;
        int rules;
        List<TmsTranspiler.DefaultsGroup> groups = List.of();
        long firingsMin, firingsMax, n0;
        double fireMs, writeBackMs, sumMs;
        /** The case with the largest firings / (R × N0), -1 while every case has N0 = 0; and the largest firings /
         *  cap of a case. */
        double ratio = -1, capShare = -1;
        String ratioCase, capCase;
        long ratioFirings, ratioN0, ratioCap;
        /** The case that needs the largest factor of the cap counted per phase ({@link Phases}), -1 while none does. */
        double phases = -1;
        String phasesCase;
        long phasesFirings, phasesN;
        /** Per defaults group: the control facts after the fire summed over the cases, and their maximum in one. */
        final Map<String, long[]> controlFacts = new LinkedHashMap<>();

        static Branch built(Path ir, DroolsBackend.Branch b) throws Exception {
            Branch t = new Branch();
            long[] times = new long[BUILD_ROUNDS];
            DroolsBackend.Build build = null;
            for (int i = 0; i < BUILD_ROUNDS; i++) {
                long begin = System.nanoTime();
                build = DroolsBackend.build(ir, b);
                times[i] = System.nanoTime() - begin;
            }
            t.buildMs = median(times) / 1e6;
            t.rules = rules(build.kieBase());
            t.groups = build.execModelTranspiler().defaultsGroups();
            for (TmsTranspiler.DefaultsGroup g : t.groups) t.controlFacts.put(g.name(), new long[2]);
            t.runner = CaseRunner.forMeasurement(ir, b);
            return t;
        }

        /** Adds the measured fires of one case and what its run needs of the factor of the cap: the medians of the
         *  times, the firings, N0, the ratios, and the control facts of its last fire. The firings and N0 must not
         *  differ between the fires but for the range the table shows; N0 differing means the set-up is not that of
         *  one case. */
        void count(Golden g, String caseFileName, List<FireMeasurement> ms, Phases f) {
            if (f.ratio() > phases) {
                phases = f.ratio();
                phasesCase = caseFileName;
                phasesFirings = f.firings();
                phasesN = f.n();
            }
            fireMs += median(ms.stream().mapToLong(FireMeasurement::fireNanos).toArray()) / 1e6;
            writeBackMs += median(ms.stream().mapToLong(FireMeasurement::writeBackNanos).toArray()) / 1e6;
            sumMs += median(ms.stream().mapToLong(m -> m.fireNanos() + m.writeBackNanos()).toArray()) / 1e6;
            int min = ms.stream().mapToInt(FireMeasurement::firings).min().orElseThrow();
            int max = ms.stream().mapToInt(FireMeasurement::firings).max().orElseThrow();
            firingsMin += min;
            firingsMax += max;
            long n = ms.get(0).n0();
            int cap = ms.get(0).cap();
            for (FireMeasurement m : ms)
                if (m.n0() != n || m.cap() != cap)
                    throw new IllegalStateException(g.name + ", " + caseFileName + ": N0 of het plafond verschilt tussen de"
                            + " herhalingen: " + ms.stream().map(x -> x.n0() + "/" + x.cap()).toList());
            n0 += n;
            if (n > 0) {
                double v = max / ((double) rules * n);
                if (v > ratio) {
                    ratio = v;
                    ratioCase = caseFileName;
                    ratioFirings = max;
                    ratioN0 = n;
                    ratioCap = cap;
                }
            }
            double p = max / (double) cap;
            if (p > capShare) {
                capShare = p;
                capCase = caseFileName;
            }
            List<Object> sessionFacts = ms.get(ms.size() - 1).sessionFacts();
            if (sessionFacts != null && !groups.isEmpty()) countControlFacts(g, sessionFacts);
        }

        void countControlFacts(Golden g, List<Object> sessionFacts) {
            Map<String, Long> thisCase = new LinkedHashMap<>();
            for (String s : controlFacts.keySet()) thisCase.put(s, 0L);
            for (Object f : sessionFacts) {
                if (!(f instanceof ControlFact s)) continue;
                if (!thisCase.containsKey(s.stack()))
                    throw new IllegalStateException(g.name + ": stuurfeit " + f + " van een stapel die de transpiler niet"
                            + " noemt " + thisCase.keySet());
                thisCase.merge(s.stack(), 1L, Long::sum);
            }
            thisCase.forEach((s, n) -> {
                long[] t = controlFacts.get(s);
                t[0] += n;
                t[1] = Math.max(t[1], n);
            });
        }

        String firings() {
            return firingsMin == firingsMax ? String.valueOf(firingsMin) : firingsMin + "–" + firingsMax;
        }
    }

    // ---------------- the tables ----------------

    private static void table(List<Golden> gemeten, StringBuilder out) {
        out.append("## Per golden\n\n");
        out.append("| golden | cases | vuringen | vuren (ms) | terugzetten (ms) | bouw (ms) | R | N0 | max v/(R×N0)")
                .append(" | max v/(R×N) per fase |\n");
        out.append("|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|\n");
        for (Golden g : gemeten) {
            Branch t = g.branch;
            out.append("| ").append(g.name).append(" | ").append(g.cases.size()).append(" | ").append(t.firings())
                    .append(" | ").append(ms(t.fireMs)).append(" | ").append(ms(t.writeBackMs)).append(" | ")
                    .append(ms(t.buildMs)).append(" | ").append(t.rules).append(" | ").append(t.n0).append(" | ")
                    .append(t.ratio < 0 ? "–" : ratio(t.ratio)).append(" | ")
                    .append(t.phases < 0 ? "–" : ratio(t.phases)).append(" |\n");
        }
        out.append('\n');
    }

    private static void samenvatting(List<Golden> gemeten, StringBuilder out) {
        out.append("## Samenvatting\n\n");
        out.append("- Gemeten: ").append(gemeten.size()).append(" goldens met ")
                .append(gemeten.stream().mapToInt(g -> g.cases.size()).sum()).append(" cases.\n\n");
        out.append("Som en mediaan over de goldens.\n\n");
        out.append("| grootheid | som | mediaan |\n|---|---:|---:|\n");
        row(out, "vuringen", gemeten, t -> t.firingsMin, "%.0f");
        row(out, "vuren (ms)", gemeten, t -> t.fireMs, "%.3f");
        row(out, "terugzetten (ms)", gemeten, t -> t.writeBackMs, "%.3f");
        row(out, "vuren+terugzetten (ms)", gemeten, t -> t.sumMs, "%.3f");
        row(out, "bouw (ms)", gemeten, t -> t.buildMs, "%.3f");
        row(out, "R", gemeten, t -> t.rules, "%.0f");
        row(out, "N0", gemeten, t -> t.n0, "%.0f");
        out.append('\n');

        List<Golden> perRatio = gemeten.stream().filter(g -> g.branch.ratio >= 0)
                .sorted((x, y) -> Double.compare(y.branch.ratio, x.branch.ratio)).toList();
        out.append("### De hoogste vuringen/(R × N0) van één case\n\n");
        out.append("| golden | case | vuringen | R | N0 | v/(R×N0) | plafond van `vuur` |\n|---|---|---:|---:|---:|---:|---:|\n");
        for (Golden g : perRatio.subList(0, Math.min(10, perRatio.size()))) {
            Branch t = g.branch;
            out.append("| ").append(g.name).append(" | ").append(t.ratioCase).append(" | ")
                    .append(t.ratioFirings).append(" | ").append(t.rules).append(" | ").append(t.ratioN0)
                    .append(" | ").append(ratio(t.ratio)).append(" | ").append(t.ratioCap)
                    .append(" |\n");
        }
        long without = gemeten.stream().filter(g -> g.branch.ratio < 0).count();
        out.append('\n');
        if (without > 0)
            out.append("Goldens waarvan elke case N0 = 0 heeft (geen verhouding): ").append(without).append(".\n\n");
        if (!perRatio.isEmpty()) {
            double highest = perRatio.get(0).branch.ratio;
            out.append("De hoogste is ").append(ratio(highest)).append("; tweemaal, naar boven afgerond en ten minste")
                    .append(" 4: ").append(Math.max(4, (int) Math.ceil(2 * highest)))
                    .append(" (het schaalmodel van `Schaalproef` komt daar nog bij).\n\n");
        }
        List<Golden> perPhase = gemeten.stream().filter(g -> g.branch.phases >= 0)
                .sorted((x, y) -> Double.compare(y.branch.phases, x.branch.phases)).toList();
        out.append("### De hoogste vuringen/(R × N) per fase van één case\n\n");
        out.append("Per stuk van de run, vanaf de start of het begin van een creatiefase tot het volgende: de vuringen aan het")
                .append(" eind over R maal de grootste N die het plafond tot dan telde; per case het grootste. Zonder")
                .append(" creatie gelijk aan vuringen/(R × N0).\n\n");
        out.append("| golden | case | vuringen | R | N | v/(R×N) | v/(R×N0) van de golden |\n|---|---|---:|---:|---:|---:|---:|\n");
        for (Golden g : perPhase.subList(0, Math.min(10, perPhase.size()))) {
            Branch t = g.branch;
            out.append("| ").append(g.name).append(" | ").append(t.phasesCase).append(" | ").append(t.phasesFirings)
                    .append(" | ").append(t.rules).append(" | ").append(t.phasesN).append(" | ").append(ratio(t.phases))
                    .append(" | ").append(t.ratio < 0 ? "–" : ratio(t.ratio)).append(" |\n");
        }
        if (!perPhase.isEmpty()) {
            double highest = perPhase.get(0).branch.phases;
            out.append("\nDe hoogste per fase is ").append(ratio(highest))
                    .append("; tweemaal, naar boven afgerond en ten minste 4: ")
                    .append(Math.max(4, (int) Math.ceil(2 * highest))).append(".\n\n");
        }
        Golden tightest = null;
        for (Golden g : gemeten)
            if (tightest == null || g.branch.capShare > tightest.branch.capShare) tightest = g;
        if (tightest != null)
            out.append("Het grootste vuringen/plafond van één case, onder het plafond van `vuur`: ")
                    .append(ratio(tightest.branch.capShare)).append(" (").append(tightest.name)
                    .append(", ").append(tightest.branch.capCase).append(").\n\n");
    }

    private static void row(StringBuilder out, String name, List<Golden> goldens, ToDoubleFunction<Branch> value,
                            String form) {
        double[] n = goldens.stream().mapToDouble(g -> value.applyAsDouble(g.branch)).toArray();
        out.append("| ").append(name).append(" | ").append(String.format(Locale.ROOT, form, Arrays.stream(n).sum()))
                .append(" | ").append(String.format(Locale.ROOT, form, median(n))).append(" |\n");
    }

    private static void defaultsGroups(List<Golden> gemeten, StringBuilder out) {
        out.append("## Per defaults-groep\n\n");
        out.append("| golden | groep | vorm | regels | soorten stuurfeiten | stuurfeiten na het vuren: som | max per case |\n");
        out.append("|---|---|---|---:|---|---:|---:|\n");
        int without = 0;
        for (Golden g : gemeten) {
            Branch t = g.branch;
            if (t.groups.isEmpty()) without++;
            for (TmsTranspiler.DefaultsGroup d : t.groups) {
                long[] n = t.controlFacts.get(d.name());
                out.append("| ").append(g.name).append(" | ").append(d.name()).append(" | ")
                        .append(d.general() ? "algemeen" : "eenvoudig").append(" | ").append(d.rules()).append(" | ")
                        .append(d.controlFacts().isEmpty() ? "geen" : String.join(", ", d.controlFacts())).append(" | ")
                        .append(n[0]).append(" | ").append(n[1]).append(" |\n");
            }
        }
        out.append("\nGespeelde goldens zonder defaults-groep: ").append(without).append(".\n\n");
    }

    // ---------------- the corpus ----------------

    private static void coverage(int directories, List<Golden> goldens, StringBuilder out) {
        Map<String, Integer> played = new LinkedHashMap<>();
        List<String> failed = new ArrayList<>();
        Map<String, Map<String, List<String>>> refused = new TreeMap<>();
        int playedCases = 0;
        for (Golden g : goldens) {
            CaseRunner.Result r = g.verdict;
            switch (r.status()) {
                case "PLAYED" -> {
                    played.put(g.name, r.casesPlayed());
                    playedCases += r.casesPlayed();
                }
                case "EXCLUDE" -> refused.computeIfAbsent(family(r.reason()), f -> new TreeMap<>())
                        .computeIfAbsent(form(r.reason()), v -> new ArrayList<>()).add(g.name);
                default -> failed.add(g.name + ": " + r.caseFailures());
            }
        }
        int uitgesloten = refused.values().stream().flatMap(m -> m.values().stream()).mapToInt(List::size).sum();
        out.append("## Corpusdekking van de exec-model-tak\n\n");
        out.append(directories).append(" goldens onder corpus/golden, waarvan ").append(goldens.size())
                .append(" met cases: PLAYED ").append(played.size()).append(" (").append(playedCases)
                .append(" cases), FAIL ").append(failed.size()).append(", EXCLUDE ").append(uitgesloten)
                .append("; zonder cases ").append(directories - goldens.size()).append(".\n\n");

        out.append("### Mislukt (FAIL, of een uitzondering uit de replay)\n\n");
        if (failed.isEmpty()) out.append("Geen.\n\n");
        else {
            for (String m : failed) out.append("- ").append(m).append('\n');
            out.append('\n');
        }

        out.append("### Geweigerd per familie\n\n| familie | goldens |\n|---|---:|\n");
        for (Map.Entry<String, Map<String, List<String>>> f : refused.entrySet())
            out.append("| ").append(f.getKey()).append(" | ")
                    .append(f.getValue().values().stream().mapToInt(List::size).sum()).append(" |\n");
        out.append('\n');

        out.append("### Geweigerd per vorm\n\n| familie | vorm | goldens | welke |\n|---|---|---:|---|\n");
        for (Map.Entry<String, Map<String, List<String>>> f : refused.entrySet())
            for (Map.Entry<String, List<String>> v : f.getValue().entrySet())
                out.append("| ").append(f.getKey()).append(" | ").append(v.getKey()).append(" | ")
                        .append(v.getValue().size()).append(" | ").append(String.join(", ", v.getValue())).append(" |\n");
        out.append('\n');
    }

    private static final List<String> MANIFEST = List.of("transpiler SKIP: ", "transpiler MARKER: ");

    /** The family of a refusal: after the manifest's prefix the kind of form it refuses first, the part before the
     *  first colon; a cycle refused before the build; else the part before the first colon (the loader, the bean
     *  model, the contract, the case reader). */
    private static String family(String reason) {
        for (String p : MANIFEST)
            if (reason.startsWith(p)) {
                String rest = reason.substring(p.length());
                int i = rest.indexOf(':');
                return i < 0 ? rest : rest.substring(0, i);
            }
        if (cycle(reason)) return "kring";
        int i = reason.indexOf(':');
        return i < 0 ? reason : reason.substring(0, i);
    }

    /** The form a refusal names, without the names of the document: after the manifest's prefix the reason after the
     *  dash, up to the first parenthesis, colon or quote; a cycle as one form; the loader's form, which it names after
     *  its own sentence and spells with a colon ("Expr:TemporalRead"), up to the parenthesis; else the reason after its
     *  family. */
    private static String form(String reason) {
        for (String p : MANIFEST)
            if (reason.startsWith(p)) {
                int streep = reason.indexOf(" — ");
                return truncated(streep < 0 ? reason.substring(p.length()) : reason.substring(streep + 3));
            }
        if (cycle(reason)) return "kring in de afhankelijkheden";
        if (reason.startsWith("loader: ")) {
            String rest = reason.substring(reason.lastIndexOf(": ") + 2);
            int bracket = rest.indexOf(" (");
            return (bracket < 0 ? rest : rest.substring(0, bracket)).trim().replace("|", "\\|");
        }
        int i = reason.indexOf(':');
        return truncated(i < 0 ? reason : reason.substring(i + 1).trim());
    }

    /** Whether {@code reason} is the refusal of a cycle: the label of the branch, which two rules of one name carry as
     *  well. */
    private static boolean cycle(String reason) {
        return reason.startsWith("exec-model: ") && !reason.startsWith("exec-model: Duplicate rule name");
    }

    private static String truncated(String s) {
        int end = s.length();
        for (String sign : List.of(" (", ":", " '", " \""))
            if (s.indexOf(sign) >= 0) end = Math.min(end, s.indexOf(sign));
        return s.substring(0, end).trim().replace("|", "\\|");
    }

    // ---------------- helpers ----------------

    private static List<Path> cases(Path golden) throws Exception {
        try (Stream<Path> s = Files.list(golden)) {
            return s.filter(f -> f.getFileName().toString().matches("case.*\\.json")).sorted().toList();
        }
    }

    private static int rules(KieBase kieBase) {
        int n = 0;
        for (KiePackage p : kieBase.getKiePackages()) n += p.getRules().size();
        return n;
    }

    private static long median(long[] t) {
        long[] s = t.clone();
        Arrays.sort(s);
        return s[s.length / 2];
    }

    /** The median of {@code t}: the middle value, or the mean of the two middle values of an even count. */
    private static double median(double[] t) {
        double[] s = t.clone();
        Arrays.sort(s);
        return s.length % 2 == 1 ? s[s.length / 2] : (s[s.length / 2 - 1] + s[s.length / 2]) / 2;
    }

    private static String ms(double ms) {
        return String.format(Locale.ROOT, "%.3f", ms);
    }

    private static String ratio(double v) {
        return String.format(Locale.ROOT, "%.3f", v);
    }
}
