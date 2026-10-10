package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import dev.uchar.regelspraak.transpiler.tms.Stuurfeit;
import dev.uchar.regelspraak.transpiler.tms.TmsTranspiler;

import org.kie.api.KieBase;
import org.kie.api.definition.KiePackage;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.ToDoubleFunction;
import java.util.stream.Stream;

/**
 * The measurement of card tms-omschakeling, point 7: every golden that one of the two exec-model branches plays, the
 * destructive lowering ({@code EXEC_MODEL}) against the one with truth maintenance ({@code TRUTH_MAINTENANCE}), for the
 * service of the distribution, which fires one session per request. A harness, not a witness: wall-clock times depend
 * on the machine and its load, so no test may assert them, and the coverage of the corpus is a report, which
 * {@code TmsCorpusWitnessTest} holds to a floor. Its name therefore ends without {@code Test}: surefire does not run it,
 * and no gate does either.
 *
 * <p>Per golden, and per branch that plays it: the firings; the time of the fire and that of the write-back, apart; the
 * build of the document; R, the rules in the KieBase; N0, the facts in the session just before the fire; and the
 * largest firings / (R × N0) of one case, from which the factor of the fire cap is set ({@code DroolsBackend.FACTOR}):
 * the cap holds per session, so per case. Every case is set up as the replay sets it up ({@link CaseRunner#metSessie},
 * through {@link DroolsBackend#voegIn}), and fired and written back through the backend's entry as a consumer does:
 * {@link DroolsBackend#vuur} under the cap a consumer gets, then {@link DroolsBackend#zetAfgeleideWaarden}, each timed
 * on its own. The replay's listener for two writers of one attribute is not on the session, as with a consumer. Then
 * the sums and medians, the ratio new / old, the goldens with the largest ratio, and per defaults group of the branch
 * with truth maintenance its rules and control facts; last, every golden of the corpus with case files through
 * {@link CaseRunner#viaTruthMaintenance}: played, failed, or refused, the refusals grouped by family and by form.
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

    private static final DroolsBackend.Branch OUD = DroolsBackend.Branch.EXEC_MODEL,
            NIEUW = DroolsBackend.Branch.TRUTH_MAINTENANCE;

    private static final List<DroolsBackend.Branch> TAKKEN = List.of(OUD, NIEUW);

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
        List<Golden> gemeten = goldens.stream().filter(g -> g.speelt(OUD) || g.speelt(NIEUW)).toList();
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
        uit.append("# Meting oud tegen nieuw over het corpus (kaart tms-omschakeling, punt 7)\n\n");
        uit.append("- JVM: ").append(System.getProperty("java.vm.name")).append(' ')
                .append(System.getProperty("java.runtime.version")).append(" (").append(System.getProperty("java.vm.vendor"))
                .append("), ").append(System.getProperty("os.arch")).append(", ").append(rt.availableProcessors())
                .append(" processors, max heap ").append(rt.maxMemory() >> 20).append(" MB.\n");
        uit.append("- Takken: oud = de destructieve exec-model-tak (`").append(OUD).append("`), nieuw = de exec-model-tak met")
                .append(" truth maintenance (`").append(NIEUW).append("`). Gemeten: elke golden met cases die een van beide")
                .append(" speelt (het oordeel van de replay), per tak die hem speelt.\n");
        uit.append("- Opzet per case: een verse sessie, gevuld zoals de replay dat doet (`CaseRunner.metSessie`, via")
                .append(" `DroolsBackend.voegIn`); dan `DroolsBackend.vuur` onder het plafond dat een afnemer krijgt en")
                .append(" `DroolsBackend.zetAfgeleideWaarden`, elk apart getimed (op de oude tak is het terugzetten een")
                .append(" no-op). Niet getimed: sessie, bonen, invoegen. De schrijfconflictluisteraar van de replay hangt")
                .append(" niet aan de sessie, zoals bij een afnemer.\n");
        uit.append("- Herhalingen: eerst een ronde over alle gemeten goldens, ").append(RONDE)
                .append(" keer per case op elke tak (opwarmen van de gedeelde engine); dan per golden: bouw ")
                .append(BOUWRONDES).append(" keer `DroolsBackend.build` (laden, bonen, verlagen, KieBase; de mediaan; de")
                .append(" bonen worden per JVM op inhoud gecachet, dus javac telt niet mee), en per case ").append(OPWARMEN)
                .append(" opwarmers en ").append(HERHALINGEN).append(" herhalingen, de takken om en om; per case de mediaan,")
                .append(" per golden de som van de medianen.\n");
        uit.append("- Vuringen: wat `vuur` teruggeeft, opgeteld over de cases (een bereik als herhalingen verschillen).")
                .append(" R: de regels van de KieBase. N0: de feiten in de sessie vlak vóór het vuren (op de nieuwe tak ook")
                .append(" de `Emmerinvoer`- en `Lijstplaats`-feiten van `voegIn`), opgeteld over de cases. max v/(R×N0):")
                .append(" het grootste vuringen / (R × N0) van één case, want het plafond geldt per")
                .append(" sessie; \"–\" waar elke case N0 = 0 heeft.\n");
        uit.append("- Wandkloktijden hangen af van machine en belasting; de verhouding tussen de takken is het gegeven.\n\n");
    }

    // ---------------- the measurement ----------------

    /** One golden: its document, its cases without the red-path ones, the replay's verdict per branch, and what each
     *  branch that plays it measured. */
    private static final class Golden {
        final String naam;
        final Path ir;
        final List<Path> paden;
        final List<JsonNode> cases = new ArrayList<>();
        final List<String> caseNamen = new ArrayList<>();
        final Map<DroolsBackend.Branch, CaseRunner.Result> verdict = new EnumMap<>(DroolsBackend.Branch.class);
        final Map<DroolsBackend.Branch, Tak> takken = new EnumMap<>(DroolsBackend.Branch.class);

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

        /** The replay's verdict on both branches; an exception out of the replay is a FAIL with its sentence. */
        void oordeel() {
            for (DroolsBackend.Branch b : TAKKEN) {
                CaseRunner.Result r;
                try {
                    r = b == OUD ? CaseRunner.viaExecModel(ir, paden) : CaseRunner.viaTruthMaintenance(ir, paden);
                } catch (RuntimeException e) {
                    r = CaseRunner.Result.played(0, List.of(String.valueOf(e)));
                }
                verdict.put(b, r);
            }
        }

        boolean speelt(DroolsBackend.Branch b) {
            return verdict.get(b).status().equals("PLAYED");
        }

        /** Builds the document on every branch that plays it, then fires every case on those branches in turn. */
        void meet() throws Exception {
            for (DroolsBackend.Branch b : TAKKEN) if (speelt(b)) takken.put(b, Tak.gebouwd(ir, b));
            for (int c = 0; c < cases.size(); c++) {
                Map<DroolsBackend.Branch, List<Vuurmeting>> metingen = new EnumMap<>(DroolsBackend.Branch.class);
                for (int i = -OPWARMEN; i < HERHALINGEN; i++)
                    for (Map.Entry<DroolsBackend.Branch, Tak> t : takken.entrySet()) {
                        Vuurmeting m = vuur(this, c, t.getKey(), t.getValue().runner, i == HERHALINGEN - 1);
                        if (i >= 0) metingen.computeIfAbsent(t.getKey(), x -> new ArrayList<>()).add(m);
                    }
                for (Map.Entry<DroolsBackend.Branch, Tak> t : takken.entrySet())
                    t.getValue().tel(this, caseNamen.get(c), metingen.get(t.getKey()));
            }
            // the KieBases go once their golden is measured
            for (Tak t : takken.values()) t.runner = null;
        }

        String status(DroolsBackend.Branch b) {
            CaseRunner.Result r = verdict.get(b);
            return r.status() + (r.caseFailures().isEmpty() ? "" : " (" + r.caseFailures().size() + ")");
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

    /** The round over every measured golden before the first measurement: the engine and the build that all documents
     *  share are warm once the first golden is measured. Nothing of it is kept. */
    private static void rondeVooraf(List<Golden> gemeten) throws Exception {
        for (Golden g : gemeten)
            for (DroolsBackend.Branch b : TAKKEN) {
                if (!g.speelt(b)) continue;
                CaseRunner runner = CaseRunner.voorMeting(g.ir, b);
                for (int c = 0; c < g.cases.size(); c++)
                    for (int i = 0; i < RONDE; i++) vuur(g, c, b, runner, false);
            }
    }

    /** What one branch measured on one golden. */
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
            if (bouw.tmsTranspiler() != null) t.groepen = bouw.tmsTranspiler().defaultsgroepen();
            for (TmsTranspiler.Defaultsgroep g : t.groepen) t.stuurfeiten.put(g.naam(), new long[2]);
            t.runner = CaseRunner.voorMeting(ir, b);
            return t;
        }

        /** Adds the measured fires of one case: the medians of the times, the firings, N0, the ratios, and the control
         *  facts of its last fire. The firings and N0 must not differ between the fires but for the range the table
         *  shows; N0 differing means the set-up is not that of one case. */
        void tel(Golden g, String zaak, List<Vuurmeting> ms) {
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
        uit.append("| golden | cases | vuringen oud | vuringen nieuw | vuren oud (ms) | vuren nieuw (ms) | terugzetten oud (ms)")
                .append(" | terugzetten nieuw (ms) | bouw oud (ms) | bouw nieuw (ms) | R oud | R nieuw | N0 oud | N0 nieuw")
                .append(" | max v/(R×N0) oud | max v/(R×N0) nieuw |\n");
        uit.append("|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|\n");
        for (Golden g : gemeten) {
            Tak o = g.takken.get(OUD), n = g.takken.get(NIEUW);
            uit.append("| ").append(g.naam).append(" | ").append(g.cases.size());
            cel(uit, g, OUD, o, Tak::vuringen);
            cel(uit, g, NIEUW, n, Tak::vuringen);
            cel(uit, g, OUD, o, t -> ms(t.vuurMs));
            cel(uit, g, NIEUW, n, t -> ms(t.vuurMs));
            cel(uit, g, OUD, o, t -> ms(t.terugzetMs));
            cel(uit, g, NIEUW, n, t -> ms(t.terugzetMs));
            cel(uit, g, OUD, o, t -> ms(t.bouwMs));
            cel(uit, g, NIEUW, n, t -> ms(t.bouwMs));
            cel(uit, g, OUD, o, t -> String.valueOf(t.regels));
            cel(uit, g, NIEUW, n, t -> String.valueOf(t.regels));
            cel(uit, g, OUD, o, t -> String.valueOf(t.n0));
            cel(uit, g, NIEUW, n, t -> String.valueOf(t.n0));
            cel(uit, g, OUD, o, t -> t.verhouding < 0 ? "–" : verhouding(t.verhouding));
            cel(uit, g, NIEUW, n, t -> t.verhouding < 0 ? "–" : verhouding(t.verhouding));
            uit.append(" |\n");
        }
        uit.append('\n');
    }

    private static void cel(StringBuilder uit, Golden g, DroolsBackend.Branch b, Tak t,
                            java.util.function.Function<Tak, String> waarde) {
        uit.append(" | ").append(t != null ? waarde.apply(t) : g.status(b));
    }

    private static void samenvatting(List<Golden> gemeten, StringBuilder uit) {
        List<Golden> beide = gemeten.stream().filter(g -> g.takken.size() == 2).toList();
        List<Golden> nieuw = gemeten.stream().filter(g -> g.takken.size() == 1 && g.takken.containsKey(NIEUW)).toList();
        List<Golden> oud = gemeten.stream().filter(g -> g.takken.size() == 1 && g.takken.containsKey(OUD)).toList();
        uit.append("## Samenvatting\n\n");
        uit.append("- Gemeten: ").append(gemeten.size()).append(" goldens met ")
                .append(gemeten.stream().mapToInt(g -> g.cases.size()).sum()).append(" cases. Beide takken spelen ")
                .append(beide.size()).append(" (").append(beide.stream().mapToInt(g -> g.cases.size()).sum())
                .append(" cases); alleen de nieuwe ").append(nieuw.size()).append(namen(nieuw)).append("; alleen de oude ")
                .append(oud.size()).append(namen(oud)).append(".\n\n");

        uit.append("### De goldens die beide takken spelen (").append(beide.size()).append(")\n\n");
        uit.append("Som en mediaan over de goldens; nieuw/oud van de sommen, en de mediaan van nieuw/oud per golden (over")
                .append(" de goldens waar beide waarden groter zijn dan 0).\n\n");
        uit.append("| grootheid | som oud | som nieuw | nieuw/oud | mediaan oud | mediaan nieuw | mediaan van nieuw/oud |\n");
        uit.append("|---|---:|---:|---:|---:|---:|---:|\n");
        rij(uit, "vuringen", beide, t -> t.vuringenMin, "%.0f", true);
        rij(uit, "vuren (ms)", beide, t -> t.vuurMs, "%.3f", true);
        rij(uit, "terugzetten (ms)", beide, t -> t.terugzetMs, "%.3f", false);
        rij(uit, "vuren+terugzetten (ms)", beide, t -> t.somMs, "%.3f", true);
        rij(uit, "bouw (ms)", beide, t -> t.bouwMs, "%.3f", true);
        rij(uit, "R", beide, t -> t.regels, "%.0f", true);
        rij(uit, "N0", beide, t -> t.n0, "%.0f", true);
        uit.append("\nOp de oude tak is het terugzetten een no-op; zijn tijd is die van de aanroep.\n\n");

        if (!nieuw.isEmpty()) {
            uit.append("### Alleen de nieuwe tak (").append(nieuw.size()).append(")\n\n");
            uit.append("| grootheid | som | mediaan |\n|---|---:|---:|\n");
            alleen(uit, "vuringen", nieuw, t -> t.vuringenMin, "%.0f");
            alleen(uit, "vuren (ms)", nieuw, t -> t.vuurMs, "%.3f");
            alleen(uit, "terugzetten (ms)", nieuw, t -> t.terugzetMs, "%.3f");
            alleen(uit, "vuren+terugzetten (ms)", nieuw, t -> t.somMs, "%.3f");
            alleen(uit, "bouw (ms)", nieuw, t -> t.bouwMs, "%.3f");
            alleen(uit, "R", nieuw, t -> t.regels, "%.0f");
            alleen(uit, "N0", nieuw, t -> t.n0, "%.0f");
            uit.append('\n');
        }

        for (DroolsBackend.Branch b : TAKKEN) {
            List<Golden> perVerhouding = gemeten.stream().filter(g -> g.takken.containsKey(b) && g.takken.get(b).verhouding >= 0)
                    .sorted((x, y) -> Double.compare(y.takken.get(b).verhouding, x.takken.get(b).verhouding)).toList();
            String tak = b == OUD ? "oude" : "nieuwe";
            int top = b == NIEUW ? 10 : 5;
            uit.append("### De hoogste vuringen/(R × N0) van één case, ").append(tak).append(" tak\n\n");
            uit.append("| golden | case | vuringen | R | N0 | v/(R×N0) | plafond van `vuur` |\n|---|---|---:|---:|---:|---:|---:|\n");
            for (Golden g : perVerhouding.subList(0, Math.min(top, perVerhouding.size()))) {
                Tak t = g.takken.get(b);
                uit.append("| ").append(g.naam).append(" | ").append(t.verhoudingCase).append(" | ")
                        .append(t.verhoudingVuringen).append(" | ").append(t.regels).append(" | ").append(t.verhoudingN0)
                        .append(" | ").append(verhouding(t.verhouding)).append(" | ").append(t.verhoudingPlafond)
                        .append(" |\n");
            }
            long zonder = gemeten.stream().filter(g -> g.takken.containsKey(b) && g.takken.get(b).verhouding < 0).count();
            uit.append('\n');
            if (zonder > 0)
                uit.append("Goldens waarvan elke case N0 = 0 heeft (geen verhouding): ").append(zonder).append(".\n\n");
            if (b == NIEUW && !perVerhouding.isEmpty()) {
                double hoogste = perVerhouding.get(0).takken.get(b).verhouding;
                uit.append("De hoogste op de nieuwe tak is ").append(verhouding(hoogste)).append("; tweemaal, naar boven")
                        .append(" afgerond en ten minste 4: ").append(Math.max(4, (int) Math.ceil(2 * hoogste)))
                        .append(" (het schaalmodel van `Schaalproef` komt daar nog bij).\n\n");
            }
        }
        Golden krapst = null;
        for (Golden g : gemeten)
            if (g.takken.containsKey(NIEUW)
                    && (krapst == null || g.takken.get(NIEUW).plafondDeel > krapst.takken.get(NIEUW).plafondDeel))
                krapst = g;
        if (krapst != null)
            uit.append("Het grootste vuringen/plafond van één case op de nieuwe tak, onder het plafond van `vuur`: ")
                    .append(verhouding(krapst.takken.get(NIEUW).plafondDeel)).append(" (").append(krapst.naam)
                    .append(", ").append(krapst.takken.get(NIEUW).plafondCase).append(").\n\n");
    }

    private static String namen(List<Golden> goldens) {
        return goldens.isEmpty() ? "" : " (" + String.join(", ", goldens.stream().map(g -> g.naam).toList()) + ")";
    }

    private static void rij(StringBuilder uit, String naam, List<Golden> goldens, ToDoubleFunction<Tak> waarde,
                            String vorm, boolean verhouding) {
        double[] o = goldens.stream().mapToDouble(g -> waarde.applyAsDouble(g.takken.get(OUD))).toArray();
        double[] n = goldens.stream().mapToDouble(g -> waarde.applyAsDouble(g.takken.get(NIEUW))).toArray();
        double so = Arrays.stream(o).sum(), sn = Arrays.stream(n).sum();
        List<Double> per = new ArrayList<>();
        for (int i = 0; i < o.length; i++) if (o[i] > 0 && n[i] > 0) per.add(n[i] / o[i]);
        uit.append("| ").append(naam).append(" | ").append(String.format(Locale.ROOT, vorm, so)).append(" | ")
                .append(String.format(Locale.ROOT, vorm, sn)).append(" | ")
                .append(verhouding && so > 0 ? String.format(Locale.ROOT, "%.2f", sn / so) : "–").append(" | ")
                .append(String.format(Locale.ROOT, vorm, mediaan(o))).append(" | ")
                .append(String.format(Locale.ROOT, vorm, mediaan(n))).append(" | ")
                .append(verhouding && !per.isEmpty()
                        ? String.format(Locale.ROOT, "%.2f", mediaan(per.stream().mapToDouble(Double::doubleValue).toArray()))
                          + (per.size() < o.length ? " (" + per.size() + " goldens)" : "")
                        : "–")
                .append(" |\n");
    }

    private static void alleen(StringBuilder uit, String naam, List<Golden> goldens, ToDoubleFunction<Tak> waarde,
                               String vorm) {
        double[] n = goldens.stream().mapToDouble(g -> waarde.applyAsDouble(g.takken.get(NIEUW))).toArray();
        uit.append("| ").append(naam).append(" | ").append(String.format(Locale.ROOT, vorm, Arrays.stream(n).sum()))
                .append(" | ").append(String.format(Locale.ROOT, vorm, mediaan(n))).append(" |\n");
    }

    private static void defaultsgroepen(List<Golden> gemeten, StringBuilder uit) {
        uit.append("## Per defaults-groep (nieuwe tak)\n\n");
        uit.append("| golden | groep | vorm | regels | soorten stuurfeiten | stuurfeiten na het vuren: som | max per case |\n");
        uit.append("|---|---|---|---:|---|---:|---:|\n");
        int zonder = 0;
        for (Golden g : gemeten) {
            Tak t = g.takken.get(NIEUW);
            if (t == null) continue;
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
            CaseRunner.Result r = g.verdict.get(NIEUW);
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
        uit.append("## Corpusdekking van de nieuwe tak\n\n");
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

    private static final List<String> MANIFEST = List.of("tms-transpiler SKIP: ", "tms-transpiler MARKER: ");

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
        if (reden.startsWith("tms: ")) return "kring";
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
        if (reden.startsWith("tms: ")) return "kring in de afhankelijkheden";
        if (reden.startsWith("loader: ")) {
            String rest = reden.substring(reden.lastIndexOf(": ") + 2);
            int haakje = rest.indexOf(" (");
            return (haakje < 0 ? rest : rest.substring(0, haakje)).trim().replace("|", "\\|");
        }
        int i = reden.indexOf(':');
        return afgekapt(i < 0 ? reden : reden.substring(i + 1).trim());
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
