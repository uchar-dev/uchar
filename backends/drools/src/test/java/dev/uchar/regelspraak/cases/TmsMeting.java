package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import dev.uchar.regelspraak.transpiler.tms.Creatiefase;
import dev.uchar.regelspraak.transpiler.tms.Stuurfeit;
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
 * phases to the cap the driver computed. Every case is set up as the replay sets it up ({@link CaseRunner#metSessie},
 * through {@link DroolsBackend#voegIn}), and fired and written back through the backend's entry as a consumer does:
 * {@link DroolsBackend#vuur} under the cap a consumer gets, then {@link DroolsBackend#zetAfgeleideWaarden}, each timed
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
 *       dev.uchar.regelspraak.cases.TmsMeting [corpus/golden] &gt; tabel.md
 * </pre>
 * It prints Markdown on standard output and one line per golden on standard error as it goes.
 */
public final class TmsMeting {

    private TmsMeting() {}

    private static final ObjectMapper JSON = new ObjectMapper();

    /** Per case: the fires to warm up and the measured fires, of which the median counts; per document the builds, of
     *  which the median counts; and the fires per case of the round over every measured golden before the first
     *  measurement, which warms up the engine the documents share. */
    private static final int OPWARMEN = 20, HERHALINGEN = 31, BOUWRONDES = 5, RONDE = 3;

    private static final DroolsBackend.Branch TAK = DroolsBackend.Branch.EXEC_MODEL;

    public static void main(String[] args) throws Exception {
        Path golden = Path.of(args.length > 0 ? args[0] : "corpus/golden");
        if (!Files.isDirectory(golden))
            throw new IllegalArgumentException("geen map " + golden.toAbsolutePath()
                    + ": draai vanuit de wortel van de repository, of geef de map corpus/golden mee");
        List<Path> mappen;
        try (Stream<Path> s = Files.walk(golden)) {
            mappen = s.filter(p -> p.getFileName().toString().equals("ir.json")).map(Path::getParent).sorted().toList();
        }
        List<Golden> goldens = new ArrayList<>();
        for (Path map : mappen) {
            List<Path> paden = cases(map);
            if (!paden.isEmpty()) goldens.add(new Golden(golden.relativize(map).toString(), map, paden));
        }
        for (Golden g : goldens) g.oordeel();
        List<Golden> gemeten = goldens.stream().filter(Golden::speelt).toList();
        rondeVooraf(gemeten);
        int k = 0;
        for (Golden g : gemeten) {
            g.meet();
            System.err.println("tmsmeting: " + ++k + "/" + gemeten.size() + " " + g.naam);
        }
        StringBuilder uit = new StringBuilder();
        kop(uit);
        tabel(gemeten, uit);
        samenvatting(gemeten, uit);
        defaultsgroepen(gemeten, uit);
        dekking(mappen.size(), goldens, uit);
        System.out.print(uit);
    }

    private static void kop(StringBuilder uit) {
        Runtime rt = Runtime.getRuntime();
        uit.append("# Meting van de exec-model-tak over het corpus (kaart tms-omschakeling, punt 7)\n\n");
        uit.append("- JVM: ").append(System.getProperty("java.vm.name")).append(' ')
                .append(System.getProperty("java.runtime.version")).append(" (").append(System.getProperty("java.vm.vendor"))
                .append("), ").append(System.getProperty("os.arch")).append(", ").append(rt.availableProcessors())
                .append(" processors, max heap ").append(rt.maxMemory() >> 20).append(" MB.\n");
        uit.append("- Tak: de exec-model-tak met truth maintenance (`").append(TAK).append("`). Gemeten: elke golden met")
                .append(" cases die hij speelt (het oordeel van de replay).\n");
        uit.append("- Opzet per case: een verse sessie, gevuld zoals de replay dat doet (`CaseRunner.metSessie`, via")
                .append(" `DroolsBackend.voegIn`); dan `DroolsBackend.vuur` onder het plafond dat een afnemer krijgt en")
                .append(" `DroolsBackend.zetAfgeleideWaarden`, elk apart getimed. Niet getimed: sessie, bonen, invoegen.")
                .append(" De schrijfconflictluisteraar van de replay hangt niet aan de sessie, zoals bij een afnemer.\n");
        uit.append("- Herhalingen: eerst een ronde over alle gemeten goldens, ").append(RONDE)
                .append(" keer per case (opwarmen van de gedeelde engine); dan per golden: bouw ")
                .append(BOUWRONDES).append(" keer `DroolsBackend.build` (laden, bonen, verlagen, KieBase; de mediaan; de")
                .append(" bonen worden per JVM op inhoud gecachet, dus javac telt niet mee), en per case ").append(OPWARMEN)
                .append(" opwarmers en ").append(HERHALINGEN).append(" herhalingen; per case de mediaan,")
                .append(" per golden de som van de medianen.\n");
        uit.append("- Vuringen: wat `vuur` teruggeeft, opgeteld over de cases (een bereik als herhalingen verschillen).")
                .append(" R: de regels van de KieBase. N0: de feiten in de sessie vlak vóór het vuren (ook de")
                .append(" `Emmerinvoer`- en `Lijstplaats`-feiten van `voegIn`), opgeteld over de cases. max v/(R×N0):")
                .append(" het grootste vuringen / (R × N0) van één case, want het plafond geldt per")
                .append(" sessie; \"–\" waar elke case N0 = 0 heeft. max v/(R×N) per fase: wat een case van de factor")
                .append(" van het plafond vraagt nu de driver het plafond bij elke creatiefase opnieuw uitrekent over de")
                .append(" feiten op dat moment (zie de samenvatting); gemeten in één extra run per case, niet getimed.\n");
        uit.append("- Wandkloktijden hangen af van machine en belasting.\n\n");
    }

    // ---------------- the measurement ----------------

    /** One golden: its document, its cases without the red-path ones, the replay's verdict, and what the branch
     *  measured where it plays it. */
    private static final class Golden {
        final String naam;
        final Path ir;
        final List<Path> paden;
        final List<JsonNode> cases = new ArrayList<>();
        final List<String> caseNamen = new ArrayList<>();
        CaseRunner.Result verdict;
        Tak tak;

        Golden(String naam, Path map, List<Path> paden) throws Exception {
            this.naam = naam;
            this.ir = map.resolve("ir.json");
            this.paden = paden;
            for (Path p : paden) {
                JsonNode c = JSON.readTree(Files.readString(p));
                if (c.has("expectedFault")) continue;
                cases.add(c);
                caseNamen.add(p.getFileName().toString());
            }
        }

        /** The replay's verdict; an exception out of the replay is a FAIL with its sentence. */
        void oordeel() {
            try {
                verdict = CaseRunner.viaExecModel(ir, paden);
            } catch (RuntimeException e) {
                verdict = CaseRunner.Result.played(0, List.of(String.valueOf(e)));
            }
        }

        boolean speelt() {
            return verdict.status().equals("PLAYED");
        }

        /** Builds the document, then fires every case. */
        void meet() throws Exception {
            tak = Tak.gebouwd(ir, TAK);
            for (int c = 0; c < cases.size(); c++) {
                List<Vuurmeting> ms = new ArrayList<>();
                for (int i = -OPWARMEN; i < HERHALINGEN; i++) {
                    Vuurmeting m = vuur(this, c, TAK, tak.runner, i == HERHALINGEN - 1);
                    if (i >= 0) ms.add(m);
                }
                Fasen f = fasen(this, c, TAK, tak.runner, ms.stream().mapToInt(Vuurmeting::vuringen).max().orElseThrow());
                tak.tel(this, caseNamen.get(c), ms, f);
            }
            // the KieBase goes once its golden is measured
            tak.runner = null;
        }
    }

    /** One fire of one case: its firings and the cap it fired under, the fire and the write-back in nanoseconds, the
     *  facts just before the fire, and, where asked, every fact the session held after the fire. */
    private record Vuurmeting(int vuringen, int plafond, long vuurNanos, long terugzetNanos, long n0,
                              List<Object> feiten) {}

    /** Fires case {@code c} of {@code g} on {@code tak} in a fresh session and measures it; any fault names the golden,
     *  the case and the branch. */
    private static Vuurmeting vuur(Golden g, int c, DroolsBackend.Branch tak, CaseRunner runner, boolean feiten) {
        try {
            return runner.metSessie(g.cases.get(c), (bouw, sessie, labels) -> {
                long n0 = sessie.getFactCount();
                long begin = System.nanoTime();
                DroolsBackend.Vuring vuring = DroolsBackend.vuur(bouw, sessie);
                long gevuurd = System.nanoTime();
                if (vuring.geenFixpunt() != null) throw new IllegalStateException(vuring.geenFixpunt());
                List<Object> stand = feiten ? new ArrayList<>(sessie.getObjects()) : null;
                long terug = System.nanoTime();
                DroolsBackend.zetAfgeleideWaarden(bouw, sessie);
                long teruggezet = System.nanoTime();
                return new Vuurmeting(vuring.vuringen(), vuring.plafond(), gevuurd - begin, teruggezet - terug, n0, stand);
            });
        } catch (RuntimeException e) {
            throw new IllegalStateException(g.naam + ", " + g.caseNamen.get(c) + " op " + tak + ": " + e, e);
        }
    }

    /** What the run of one case needs of the factor of the fire cap: {@code verhouding}, the largest over its stretches
     *  of the firings at the end of a stretch over R times the largest number of facts the cap has counted by then,
     *  with those firings and that number; -1 where every count is 0. */
    private record Fasen(double verhouding, long vuringen, long n) {}

    /** Fires case {@code c} of {@code g} on {@code tak} once more, through the seam with the factor 1000 and floor 1, a
     *  cap no golden reaches that the driver computes like the public one, with a listener on the firings and one on
     *  the facts that marks every {@code Creatiefase} the driver inserts, with the firings so far and the facts the
     *  session then holds: the driver computes the cap again right after that insertion, over the same count. The run
     *  must give the firings of the measured fires, its listener must count what the driver reports, and the cap it
     *  ends with must be 1000 × R times the largest of the counts; else the measurement fails. */
    private static Fasen fasen(Golden g, int c, DroolsBackend.Branch tak, CaseRunner runner, int vuringen) {
        final double factor = 1000;
        try {
            return runner.metSessie(g.cases.get(c), (bouw, sessie, labels) -> {
                long regels = regels(bouw.kieBase());
                long[] gevuurd = {0};
                List<long[]> starts = new ArrayList<>();
                starts.add(new long[] {0, sessie.getFactCount()});
                AgendaEventListener teller = new DefaultAgendaEventListener() {
                    @Override public void afterMatchFired(AfterMatchFiredEvent e) {
                        gevuurd[0]++;
                    }
                };
                RuleRuntimeEventListener fase = new RuleRuntimeEventListener() {
                    @Override public void objectInserted(ObjectInsertedEvent e) {
                        if (e.getObject() instanceof Creatiefase) starts.add(new long[] {gevuurd[0], sessie.getFactCount()});
                    }

                    @Override public void objectUpdated(ObjectUpdatedEvent e) {}

                    @Override public void objectDeleted(ObjectDeletedEvent e) {}
                };
                sessie.addEventListener(teller);
                sessie.addEventListener(fase);
                DroolsBackend.Vuring vuring = DroolsBackend.vuur(bouw, sessie, factor, 1);
                sessie.removeEventListener(teller);
                sessie.removeEventListener(fase);
                if (vuring.geenFixpunt() != null) throw new IllegalStateException(vuring.geenFixpunt());
                if (vuring.vuringen() != vuringen || gevuurd[0] != vuringen)
                    throw new IllegalStateException("de run met luisteraars vuurde " + vuring.vuringen() + " keer, de"
                            + " luisteraar telde " + gevuurd[0] + ", de gemeten runs " + vuringen);
                double hoogste = -1;
                long hoogsteVuringen = 0, hoogsteN = 0, grootste = 0;
                for (int k = 0; k < starts.size(); k++) {
                    grootste = Math.max(grootste, starts.get(k)[1]);
                    long eind = k + 1 < starts.size() ? starts.get(k + 1)[0] : vuring.vuringen();
                    if (grootste == 0) continue;
                    double v = eind / ((double) regels * grootste);
                    if (v > hoogste) {
                        hoogste = v;
                        hoogsteVuringen = eind;
                        hoogsteN = grootste;
                    }
                }
                long plafond = (long) Math.min(Integer.MAX_VALUE, Math.max(1, Math.ceil(factor * regels * grootste)));
                if (vuring.plafond() != plafond)
                    throw new IllegalStateException("het plafond van de driver is " + vuring.plafond() + ", de fasen geven "
                            + factor + " × " + regels + " × " + grootste + " = " + plafond);
                return new Fasen(hoogste, hoogsteVuringen, hoogsteN);
            });
        } catch (RuntimeException e) {
            throw new IllegalStateException(g.naam + ", " + g.caseNamen.get(c) + " op " + tak + " (fasen): " + e, e);
        }
    }

    /** The round over every measured golden before the first measurement: the engine and the build that all documents
     *  share are warm once the first golden is measured. Nothing of it is kept. */
    private static void rondeVooraf(List<Golden> gemeten) throws Exception {
        for (Golden g : gemeten) {
            CaseRunner runner = CaseRunner.voorMeting(g.ir, TAK);
            for (int c = 0; c < g.cases.size(); c++)
                for (int i = 0; i < RONDE; i++) vuur(g, c, TAK, runner, false);
        }
    }

    /** What the branch measured on one golden. */
    private static final class Tak {
        CaseRunner runner;
        double bouwMs;
        int regels;
        List<TmsTranspiler.Defaultsgroep> groepen = List.of();
        long vuringenMin, vuringenMax, n0;
        double vuurMs, terugzetMs, somMs;
        /** The case with the largest firings / (R × N0), -1 while every case has N0 = 0; and the largest firings /
         *  cap of a case. */
        double verhouding = -1, plafondDeel = -1;
        String verhoudingCase, plafondCase;
        long verhoudingVuringen, verhoudingN0, verhoudingPlafond;
        /** The case that needs the largest factor of the cap counted per phase ({@link Fasen}), -1 while none does. */
        double fasen = -1;
        String fasenCase;
        long fasenVuringen, fasenN;
        /** Per defaults group: the control facts after the fire summed over the cases, and their maximum in one. */
        final Map<String, long[]> stuurfeiten = new LinkedHashMap<>();

        static Tak gebouwd(Path ir, DroolsBackend.Branch b) throws Exception {
            Tak t = new Tak();
            long[] tijden = new long[BOUWRONDES];
            DroolsBackend.Build bouw = null;
            for (int i = 0; i < BOUWRONDES; i++) {
                long begin = System.nanoTime();
                bouw = DroolsBackend.build(ir, b);
                tijden[i] = System.nanoTime() - begin;
            }
            t.bouwMs = mediaan(tijden) / 1e6;
            t.regels = regels(bouw.kieBase());
            t.groepen = bouw.execModelTranspiler().defaultsgroepen();
            for (TmsTranspiler.Defaultsgroep g : t.groepen) t.stuurfeiten.put(g.naam(), new long[2]);
            t.runner = CaseRunner.voorMeting(ir, b);
            return t;
        }

        /** Adds the measured fires of one case and what its run needs of the factor of the cap: the medians of the
         *  times, the firings, N0, the ratios, and the control facts of its last fire. The firings and N0 must not
         *  differ between the fires but for the range the table shows; N0 differing means the set-up is not that of
         *  one case. */
        void tel(Golden g, String zaak, List<Vuurmeting> ms, Fasen f) {
            if (f.verhouding() > fasen) {
                fasen = f.verhouding();
                fasenCase = zaak;
                fasenVuringen = f.vuringen();
                fasenN = f.n();
            }
            vuurMs += mediaan(ms.stream().mapToLong(Vuurmeting::vuurNanos).toArray()) / 1e6;
            terugzetMs += mediaan(ms.stream().mapToLong(Vuurmeting::terugzetNanos).toArray()) / 1e6;
            somMs += mediaan(ms.stream().mapToLong(m -> m.vuurNanos() + m.terugzetNanos()).toArray()) / 1e6;
            int min = ms.stream().mapToInt(Vuurmeting::vuringen).min().orElseThrow();
            int max = ms.stream().mapToInt(Vuurmeting::vuringen).max().orElseThrow();
            vuringenMin += min;
            vuringenMax += max;
            long n = ms.get(0).n0();
            int plafond = ms.get(0).plafond();
            for (Vuurmeting m : ms)
                if (m.n0() != n || m.plafond() != plafond)
                    throw new IllegalStateException(g.naam + ", " + zaak + ": N0 of het plafond verschilt tussen de"
                            + " herhalingen: " + ms.stream().map(x -> x.n0() + "/" + x.plafond()).toList());
            n0 += n;
            if (n > 0) {
                double v = max / ((double) regels * n);
                if (v > verhouding) {
                    verhouding = v;
                    verhoudingCase = zaak;
                    verhoudingVuringen = max;
                    verhoudingN0 = n;
                    verhoudingPlafond = plafond;
                }
            }
            double p = max / (double) plafond;
            if (p > plafondDeel) {
                plafondDeel = p;
                plafondCase = zaak;
            }
            List<Object> feiten = ms.get(ms.size() - 1).feiten();
            if (feiten != null && !groepen.isEmpty()) telStuurfeiten(g, feiten);
        }

        void telStuurfeiten(Golden g, List<Object> feiten) {
            Map<String, Long> dezeCase = new LinkedHashMap<>();
            for (String s : stuurfeiten.keySet()) dezeCase.put(s, 0L);
            for (Object f : feiten) {
                if (!(f instanceof Stuurfeit s)) continue;
                if (!dezeCase.containsKey(s.stapel()))
                    throw new IllegalStateException(g.naam + ": stuurfeit " + f + " van een stapel die de transpiler niet"
                            + " noemt " + dezeCase.keySet());
                dezeCase.merge(s.stapel(), 1L, Long::sum);
            }
            dezeCase.forEach((s, n) -> {
                long[] t = stuurfeiten.get(s);
                t[0] += n;
                t[1] = Math.max(t[1], n);
            });
        }

        String vuringen() {
            return vuringenMin == vuringenMax ? String.valueOf(vuringenMin) : vuringenMin + "–" + vuringenMax;
        }
    }

    // ---------------- the tables ----------------

    private static void tabel(List<Golden> gemeten, StringBuilder uit) {
        uit.append("## Per golden\n\n");
        uit.append("| golden | cases | vuringen | vuren (ms) | terugzetten (ms) | bouw (ms) | R | N0 | max v/(R×N0)")
                .append(" | max v/(R×N) per fase |\n");
        uit.append("|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|\n");
        for (Golden g : gemeten) {
            Tak t = g.tak;
            uit.append("| ").append(g.naam).append(" | ").append(g.cases.size()).append(" | ").append(t.vuringen())
                    .append(" | ").append(ms(t.vuurMs)).append(" | ").append(ms(t.terugzetMs)).append(" | ")
                    .append(ms(t.bouwMs)).append(" | ").append(t.regels).append(" | ").append(t.n0).append(" | ")
                    .append(t.verhouding < 0 ? "–" : verhouding(t.verhouding)).append(" | ")
                    .append(t.fasen < 0 ? "–" : verhouding(t.fasen)).append(" |\n");
        }
        uit.append('\n');
    }

    private static void samenvatting(List<Golden> gemeten, StringBuilder uit) {
        uit.append("## Samenvatting\n\n");
        uit.append("- Gemeten: ").append(gemeten.size()).append(" goldens met ")
                .append(gemeten.stream().mapToInt(g -> g.cases.size()).sum()).append(" cases.\n\n");
        uit.append("Som en mediaan over de goldens.\n\n");
        uit.append("| grootheid | som | mediaan |\n|---|---:|---:|\n");
        rij(uit, "vuringen", gemeten, t -> t.vuringenMin, "%.0f");
        rij(uit, "vuren (ms)", gemeten, t -> t.vuurMs, "%.3f");
        rij(uit, "terugzetten (ms)", gemeten, t -> t.terugzetMs, "%.3f");
        rij(uit, "vuren+terugzetten (ms)", gemeten, t -> t.somMs, "%.3f");
        rij(uit, "bouw (ms)", gemeten, t -> t.bouwMs, "%.3f");
        rij(uit, "R", gemeten, t -> t.regels, "%.0f");
        rij(uit, "N0", gemeten, t -> t.n0, "%.0f");
        uit.append('\n');

        List<Golden> perVerhouding = gemeten.stream().filter(g -> g.tak.verhouding >= 0)
                .sorted((x, y) -> Double.compare(y.tak.verhouding, x.tak.verhouding)).toList();
        uit.append("### De hoogste vuringen/(R × N0) van één case\n\n");
        uit.append("| golden | case | vuringen | R | N0 | v/(R×N0) | plafond van `vuur` |\n|---|---|---:|---:|---:|---:|---:|\n");
        for (Golden g : perVerhouding.subList(0, Math.min(10, perVerhouding.size()))) {
            Tak t = g.tak;
            uit.append("| ").append(g.naam).append(" | ").append(t.verhoudingCase).append(" | ")
                    .append(t.verhoudingVuringen).append(" | ").append(t.regels).append(" | ").append(t.verhoudingN0)
                    .append(" | ").append(verhouding(t.verhouding)).append(" | ").append(t.verhoudingPlafond)
                    .append(" |\n");
        }
        long zonder = gemeten.stream().filter(g -> g.tak.verhouding < 0).count();
        uit.append('\n');
        if (zonder > 0)
            uit.append("Goldens waarvan elke case N0 = 0 heeft (geen verhouding): ").append(zonder).append(".\n\n");
        if (!perVerhouding.isEmpty()) {
            double hoogste = perVerhouding.get(0).tak.verhouding;
            uit.append("De hoogste is ").append(verhouding(hoogste)).append("; tweemaal, naar boven afgerond en ten minste")
                    .append(" 4: ").append(Math.max(4, (int) Math.ceil(2 * hoogste)))
                    .append(" (het schaalmodel van `Schaalproef` komt daar nog bij).\n\n");
        }
        List<Golden> perFase = gemeten.stream().filter(g -> g.tak.fasen >= 0)
                .sorted((x, y) -> Double.compare(y.tak.fasen, x.tak.fasen)).toList();
        uit.append("### De hoogste vuringen/(R × N) per fase van één case\n\n");
        uit.append("Per stuk van de run, vanaf de start of het begin van een creatiefase tot het volgende: de vuringen aan het")
                .append(" eind over R maal de grootste N die het plafond tot dan telde; per case het grootste. Zonder")
                .append(" creatie gelijk aan vuringen/(R × N0).\n\n");
        uit.append("| golden | case | vuringen | R | N | v/(R×N) | v/(R×N0) van de golden |\n|---|---|---:|---:|---:|---:|---:|\n");
        for (Golden g : perFase.subList(0, Math.min(10, perFase.size()))) {
            Tak t = g.tak;
            uit.append("| ").append(g.naam).append(" | ").append(t.fasenCase).append(" | ").append(t.fasenVuringen)
                    .append(" | ").append(t.regels).append(" | ").append(t.fasenN).append(" | ").append(verhouding(t.fasen))
                    .append(" | ").append(t.verhouding < 0 ? "–" : verhouding(t.verhouding)).append(" |\n");
        }
        if (!perFase.isEmpty()) {
            double hoogste = perFase.get(0).tak.fasen;
            uit.append("\nDe hoogste per fase is ").append(verhouding(hoogste))
                    .append("; tweemaal, naar boven afgerond en ten minste 4: ")
                    .append(Math.max(4, (int) Math.ceil(2 * hoogste))).append(".\n\n");
        }
        Golden krapst = null;
        for (Golden g : gemeten)
            if (krapst == null || g.tak.plafondDeel > krapst.tak.plafondDeel) krapst = g;
        if (krapst != null)
            uit.append("Het grootste vuringen/plafond van één case, onder het plafond van `vuur`: ")
                    .append(verhouding(krapst.tak.plafondDeel)).append(" (").append(krapst.naam)
                    .append(", ").append(krapst.tak.plafondCase).append(").\n\n");
    }

    private static void rij(StringBuilder uit, String naam, List<Golden> goldens, ToDoubleFunction<Tak> waarde,
                            String vorm) {
        double[] n = goldens.stream().mapToDouble(g -> waarde.applyAsDouble(g.tak)).toArray();
        uit.append("| ").append(naam).append(" | ").append(String.format(Locale.ROOT, vorm, Arrays.stream(n).sum()))
                .append(" | ").append(String.format(Locale.ROOT, vorm, mediaan(n))).append(" |\n");
    }

    private static void defaultsgroepen(List<Golden> gemeten, StringBuilder uit) {
        uit.append("## Per defaults-groep\n\n");
        uit.append("| golden | groep | vorm | regels | soorten stuurfeiten | stuurfeiten na het vuren: som | max per case |\n");
        uit.append("|---|---|---|---:|---|---:|---:|\n");
        int zonder = 0;
        for (Golden g : gemeten) {
            Tak t = g.tak;
            if (t.groepen.isEmpty()) zonder++;
            for (TmsTranspiler.Defaultsgroep d : t.groepen) {
                long[] n = t.stuurfeiten.get(d.naam());
                uit.append("| ").append(g.naam).append(" | ").append(d.naam()).append(" | ")
                        .append(d.algemeen() ? "algemeen" : "eenvoudig").append(" | ").append(d.regels()).append(" | ")
                        .append(d.stuurfeiten().isEmpty() ? "geen" : String.join(", ", d.stuurfeiten())).append(" | ")
                        .append(n[0]).append(" | ").append(n[1]).append(" |\n");
            }
        }
        uit.append("\nGespeelde goldens zonder defaults-groep: ").append(zonder).append(".\n\n");
    }

    // ---------------- the corpus ----------------

    private static void dekking(int mappen, List<Golden> goldens, StringBuilder uit) {
        Map<String, Integer> gespeeld = new LinkedHashMap<>();
        List<String> mislukt = new ArrayList<>();
        Map<String, Map<String, List<String>>> geweigerd = new TreeMap<>();
        int gespeeldeCases = 0;
        for (Golden g : goldens) {
            CaseRunner.Result r = g.verdict;
            switch (r.status()) {
                case "PLAYED" -> {
                    gespeeld.put(g.naam, r.casesPlayed());
                    gespeeldeCases += r.casesPlayed();
                }
                case "EXCLUDE" -> geweigerd.computeIfAbsent(familie(r.reason()), f -> new TreeMap<>())
                        .computeIfAbsent(vorm(r.reason()), v -> new ArrayList<>()).add(g.naam);
                default -> mislukt.add(g.naam + ": " + r.caseFailures());
            }
        }
        int uitgesloten = geweigerd.values().stream().flatMap(m -> m.values().stream()).mapToInt(List::size).sum();
        uit.append("## Corpusdekking van de exec-model-tak\n\n");
        uit.append(mappen).append(" goldens onder corpus/golden, waarvan ").append(goldens.size())
                .append(" met cases: PLAYED ").append(gespeeld.size()).append(" (").append(gespeeldeCases)
                .append(" cases), FAIL ").append(mislukt.size()).append(", EXCLUDE ").append(uitgesloten)
                .append("; zonder cases ").append(mappen - goldens.size()).append(".\n\n");

        uit.append("### Mislukt (FAIL, of een uitzondering uit de replay)\n\n");
        if (mislukt.isEmpty()) uit.append("Geen.\n\n");
        else {
            for (String m : mislukt) uit.append("- ").append(m).append('\n');
            uit.append('\n');
        }

        uit.append("### Geweigerd per familie\n\n| familie | goldens |\n|---|---:|\n");
        for (Map.Entry<String, Map<String, List<String>>> f : geweigerd.entrySet())
            uit.append("| ").append(f.getKey()).append(" | ")
                    .append(f.getValue().values().stream().mapToInt(List::size).sum()).append(" |\n");
        uit.append('\n');

        uit.append("### Geweigerd per vorm\n\n| familie | vorm | goldens | welke |\n|---|---|---:|---|\n");
        for (Map.Entry<String, Map<String, List<String>>> f : geweigerd.entrySet())
            for (Map.Entry<String, List<String>> v : f.getValue().entrySet())
                uit.append("| ").append(f.getKey()).append(" | ").append(v.getKey()).append(" | ")
                        .append(v.getValue().size()).append(" | ").append(String.join(", ", v.getValue())).append(" |\n");
        uit.append('\n');
    }

    private static final List<String> MANIFEST = List.of("transpiler SKIP: ", "transpiler MARKER: ");

    /** The family of a refusal: after the manifest's prefix the kind of form it refuses first, the part before the
     *  first colon; a cycle refused before the build; else the part before the first colon (the loader, the bean
     *  model, the contract, the case reader). */
    private static String familie(String reden) {
        for (String p : MANIFEST)
            if (reden.startsWith(p)) {
                String rest = reden.substring(p.length());
                int i = rest.indexOf(':');
                return i < 0 ? rest : rest.substring(0, i);
            }
        if (kring(reden)) return "kring";
        int i = reden.indexOf(':');
        return i < 0 ? reden : reden.substring(0, i);
    }

    /** The form a refusal names, without the names of the document: after the manifest's prefix the reason after the
     *  dash, up to the first parenthesis, colon or quote; a cycle as one form; the loader's form, which it names after
     *  its own sentence and spells with a colon ("Expr:TemporalRead"), up to the parenthesis; else the reason after its
     *  family. */
    private static String vorm(String reden) {
        for (String p : MANIFEST)
            if (reden.startsWith(p)) {
                int streep = reden.indexOf(" — ");
                return afgekapt(streep < 0 ? reden.substring(p.length()) : reden.substring(streep + 3));
            }
        if (kring(reden)) return "kring in de afhankelijkheden";
        if (reden.startsWith("loader: ")) {
            String rest = reden.substring(reden.lastIndexOf(": ") + 2);
            int haakje = rest.indexOf(" (");
            return (haakje < 0 ? rest : rest.substring(0, haakje)).trim().replace("|", "\\|");
        }
        int i = reden.indexOf(':');
        return afgekapt(i < 0 ? reden : reden.substring(i + 1).trim());
    }

    /** Whether {@code reden} is the refusal of a cycle: the label of the branch, which two rules of one name carry as
     *  well. */
    private static boolean kring(String reden) {
        return reden.startsWith("exec-model: ") && !reden.startsWith("exec-model: Duplicate rule name");
    }

    private static String afgekapt(String s) {
        int eind = s.length();
        for (String teken : List.of(" (", ":", " '", " \""))
            if (s.indexOf(teken) >= 0) eind = Math.min(eind, s.indexOf(teken));
        return s.substring(0, eind).trim().replace("|", "\\|");
    }

    // ---------------- helpers ----------------

    private static List<Path> cases(Path golden) throws Exception {
        try (Stream<Path> s = Files.list(golden)) {
            return s.filter(f -> f.getFileName().toString().matches("case.*\\.json")).sorted().toList();
        }
    }

    private static int regels(KieBase kieBase) {
        int n = 0;
        for (KiePackage p : kieBase.getKiePackages()) n += p.getRules().size();
        return n;
    }

    private static long mediaan(long[] t) {
        long[] s = t.clone();
        Arrays.sort(s);
        return s[s.length / 2];
    }

    /** The median of {@code t}: the middle value, or the mean of the two middle values of an even count. */
    private static double mediaan(double[] t) {
        double[] s = t.clone();
        Arrays.sort(s);
        return s.length % 2 == 1 ? s[s.length / 2] : (s[s.length / 2 - 1] + s[s.length / 2]) / 2;
    }

    private static String ms(double ms) {
        return String.format(Locale.ROOT, "%.3f", ms);
    }

    private static String verhouding(double v) {
        return String.format(Locale.ROOT, "%.3f", v);
    }
}
