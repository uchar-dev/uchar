package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import dev.uchar.regelspraak.transpiler.tms.Stuurfeit;
import dev.uchar.regelspraak.transpiler.tms.TmsTranspiler;
import dev.uchar.regelspraak.transpiler.tms.Uitkomst;

import org.kie.api.KieBase;
import org.kie.api.definition.KiePackage;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;

/**
 * The measurement the truth-maintenance trial's card asks for in its acceptance (e), and the coverage of the corpus
 * that its question (f) weighs: a harness, not a witness. Wall-clock times depend on the machine and its load, so no
 * test may assert them, and the coverage is a report, which {@code TmsCorpusWitnessTest} holds to a floor. Its name
 * therefore ends without {@code Test}: surefire does not run it, and no gate does either.
 *
 * <p>For every golden of the card's proefset, on the exec-model branch and on the truth-maintenance branch: the
 * firings per case (what {@code fireAllRules} returns in the replay's fire), the wall-clock time of the fire plus the
 * write-back per case (after a warm-up, the median of the repetitions), the build of the document (the median of a few
 * builds), and the rules in each KieBase; per defaults group of the truth-maintenance branch its rules and the kinds of
 * control fact it inserts, from the transpiler, and the control facts a session holds at its end. Every case is set
 * up and fired by {@link CaseRunner#meet}, the replay's own set-up and fire, so the numbers are those of the replay.
 * Then every golden of the corpus with case files through {@link CaseRunner#viaTruthMaintenance}: played, failed, or
 * refused, the refusals grouped by family and by form. A played golden with a failing case is a fault of the branch,
 * and is listed as such.
 *
 * <p>Run it from the root of the repository, with the test classpath from the dependency plugin, which resolves
 * offline once the module has been built:
 * <pre>
 *   mvn -o -q -f backends/drools/pom.xml test-compile
 *   CP=$(mktemp -d)/cp.txt
 *   mvn -o -q -f backends/drools/pom.xml dependency:build-classpath -Dmdep.outputFile="$CP"
 *   java -cp "backends/drools/target/test-classes:backends/drools/target/classes:$(cat "$CP")" \
 *       dev.uchar.regelspraak.cases.TmsMeting [corpus/golden]
 * </pre>
 * It prints Markdown on standard output.
 */
public final class TmsMeting {

    private TmsMeting() {}

    private static final ObjectMapper JSON = new ObjectMapper();

    /** The card's proefset, in the order of its table. */
    private static final List<String> PROEFSET = List.of("jutterswet-meldingsketen", "anchor/jutterswet-kustwacht",
            "jutterswet-initialisatievolgorde", "jutterswet-voorwaardelijke-initialisatie", "jutterswet-wrakinitialisatie",
            "anchor/jutterswet-nestdiepte", "anchor/jutterswet-nestuitzondering", "jutterswet-opgave-boven-berekening");

    private static final int OPWARMEN = 20, HERHALINGEN = 31, BOUWRONDES = 5;

    private static final List<DroolsBackend.Branch> TAKKEN =
            List.of(DroolsBackend.Branch.EXEC_MODEL, DroolsBackend.Branch.TRUTH_MAINTENANCE);

    public static void main(String[] args) throws Exception {
        Path golden = Path.of(args.length > 0 ? args[0] : "corpus/golden");
        if (!Files.isDirectory(golden))
            throw new IllegalArgumentException("geen map " + golden.toAbsolutePath()
                    + ": draai vanuit de wortel van de repository, of geef de map corpus/golden mee");
        StringBuilder uit = new StringBuilder();
        kop(uit);
        proefset(golden, uit);
        corpus(golden, uit);
        System.out.print(uit);
    }

    private static void kop(StringBuilder uit) {
        Runtime rt = Runtime.getRuntime();
        uit.append("# Proeftak truth maintenance: meting (acceptatie e) en corpusdekking (beslisvraag f)\n\n");
        uit.append("- JVM: ").append(System.getProperty("java.vm.name")).append(' ')
                .append(System.getProperty("java.runtime.version")).append(" (").append(System.getProperty("java.vm.vendor"))
                .append("), ").append(System.getProperty("os.arch")).append(", ").append(rt.availableProcessors())
                .append(" processors, max heap ").append(rt.maxMemory() >> 20).append(" MB.\n");
        uit.append("- Opzet: per golden en tak één KieBase, gebouwd zoals de replay bouwt (`DroolsBackend.build`). Per case een")
                .append(" verse sessie met de invoer gezet en ingevoegd zoals de replay dat doet; getimed is alleen")
                .append(" `vuurTotFixpunt`: `fireAllRules` onder het plafond van de replay, met diens schrijfconflictluisteraar,")
                .append(" plus het terugzetten (een no-op op de exec-modeltak). Niet getimed: sessie, bonen, invoegen.\n");
        uit.append("- Herhalingen: eerst alle cases van alle goldens op beide takken ").append(OPWARMEN)
                .append(" keer (opwarmen), daarna per case ").append(HERHALINGEN)
                .append(" herhalingen en daarvan de mediaan; per golden de som van de medianen. Bouw: mediaan van ")
                .append(BOUWRONDES).append(" keer `DroolsBackend.build` (laden, bonen, verlagen, KieBase); de bonen")
                .append(" worden per JVM op inhoud gecachet, dus javac telt niet mee.\n");
        uit.append("- Vuringen: de teruggave van `fireAllRules` per case, opgeteld per golden; een bereik als ze tussen")
                .append(" herhalingen verschillen. Regels: alle regels van de KieBase. Feiten: alle feiten in de sessie na")
                .append(" het vuren, opgeteld over de cases.\n");
        uit.append("- Stuurfeiten: de feiten die `Stuurfeit` implementeren (`Beslist`, `Uitkomst`) aan het eind van de")
                .append(" sessie, per groep via `stapel()`. \"Los\": een `Uitkomst` waarop de uitkomst van de stapel niet rust,")
                .append(" in een lidketen van een default die zelf geen `Uitkomst` heeft omdat een hogere in zijn keten")
                .append(" besliste, of daaronder (`Defaultsgroep.eigenaars`). Losse stuurfeiten tellen mee in de som en")
                .append(" staan apart.\n");
        uit.append("- Wandkloktijden hangen af van machine en belasting; de verhouding tussen de takken is het gegeven.\n\n");
    }

    // ---------------- the proefset ----------------

    /** One golden on one branch: the replay's verdict, its runner and cases, and what was measured. */
    private static final class Meetpunt {
        final String golden;
        final DroolsBackend.Branch tak;
        final List<JsonNode> cases;
        CaseRunner.Result verdict;
        CaseRunner runner;
        double bouwMs;
        int regels;
        List<TmsTranspiler.Defaultsgroep> groepen = List.of();
        long vuringenMin, vuringenMax, feiten;
        double vuurMs;
        /** Per group: the control facts at the end summed over the cases, their maximum in one case, and the same for
         *  the loose ones. */
        final Map<String, long[]> stuurfeiten = new LinkedHashMap<>();

        Meetpunt(String golden, DroolsBackend.Branch tak, List<JsonNode> cases) {
            this.golden = golden;
            this.tak = tak;
            this.cases = cases;
        }

        boolean speelt() {
            return runner != null;
        }

        void bouw(Path ir) throws Exception {
            long[] t = new long[BOUWRONDES];
            DroolsBackend.Build b = null;
            for (int i = 0; i < BOUWRONDES; i++) {
                long begin = System.nanoTime();
                b = DroolsBackend.build(ir, tak);
                t[i] = System.nanoTime() - begin;
            }
            bouwMs = mediaan(t) / 1e6;
            regels = regels(b.kieBase());
            if (b.tmsTranspiler() != null) groepen = b.tmsTranspiler().defaultsgroepen();
            for (TmsTranspiler.Defaultsgroep g : groepen) stuurfeiten.put(g.naam(), new long[4]);
            runner = CaseRunner.voorMeting(ir, tak);
        }

        void meet() {
            long som = 0;
            for (JsonNode c : cases) {
                long[] t = new long[HERHALINGEN];
                int min = Integer.MAX_VALUE, max = Integer.MIN_VALUE;
                CaseRunner.Meting laatste = null;
                for (int i = 0; i < HERHALINGEN; i++) {
                    laatste = runner.meet(c);
                    t[i] = laatste.nanos();
                    min = Math.min(min, laatste.vuringen());
                    max = Math.max(max, laatste.vuringen());
                }
                som += mediaan(t);
                vuringenMin += min;
                vuringenMax += max;
                feiten += laatste.feiten().size();
                telStuurfeiten(laatste.feiten());
            }
            vuurMs = som / 1e6;
        }

        /** Counts the control facts of one session's end per group, and among them the loose ones. */
        void telStuurfeiten(List<Object> feiten) {
            Map<String, TmsTranspiler.Defaultsgroep> perNaam = new LinkedHashMap<>();
            for (TmsTranspiler.Defaultsgroep g : groepen) perNaam.put(g.naam(), g);
            Map<Object, Map<String, List<Uitkomst>>> uitkomsten = new IdentityHashMap<>();
            for (Object f : feiten)
                if (f instanceof Uitkomst u)
                    uitkomsten.computeIfAbsent(u.onderwerp(), o -> new LinkedHashMap<>())
                            .computeIfAbsent(u.stapel(), s -> new ArrayList<>()).add(u);
            Map<String, long[]> dezeCase = new LinkedHashMap<>();
            for (String g : perNaam.keySet()) dezeCase.put(g, new long[2]);
            for (Object f : feiten) {
                if (!(f instanceof Stuurfeit s)) continue;
                long[] n = dezeCase.get(s.stapel());
                if (n == null)
                    throw new IllegalStateException(golden + ": stuurfeit " + f + " van een stapel die de transpiler niet noemt "
                            + perNaam.keySet());
                n[0]++;
                if (f instanceof Uitkomst u
                        && !draagt(u, uitkomsten.get(u.onderwerp()).get(u.stapel()), perNaam.get(u.stapel()).eigenaars()))
                    n[1]++;
            }
            for (Map.Entry<String, long[]> e : dezeCase.entrySet()) {
                long[] t = stuurfeiten.get(e.getKey()), n = e.getValue();
                t[0] += n[0];
                t[1] = Math.max(t[1], n[0]);
                t[2] += n[1];
                t[3] = Math.max(t[3], n[1]);
            }
        }

        String vuringen() {
            return vuringenMin == vuringenMax ? String.valueOf(vuringenMin) : vuringenMin + "–" + vuringenMax;
        }

        /** The verdict where the branch does not play the golden, for every cell of its measurements. */
        String niet() {
            return verdict.status() + (verdict.status().equals("FAIL") ? " (" + verdict.caseFailures().size() + ")" : "");
        }
    }

    /** Whether the outcome of its stack rests on {@code u}: a root chain's outcome, which the root reconciliation reads,
     *  or the outcome of a member chain whose default settled and rests on the outcome itself. A member chain settles
     *  by its own rules, and its default carries the {@code not Uitkomst} of the positions above it; where that default
     *  settled nothing while a member did, a higher default in its chain displaced it, and what stands below is loose. */
    private static boolean draagt(Uitkomst u, List<Uitkomst> zelfde, Map<Integer, TmsTranspiler.Plaats> eigenaars) {
        TmsTranspiler.Plaats p = eigenaars.get(u.keten());
        if (p == null) return true;
        for (Uitkomst x : zelfde)
            if (x.keten() == p.keten() && x.positie() == p.positie()) return draagt(x, zelfde, eigenaars);
        return false;
    }

    private static void proefset(Path golden, StringBuilder uit) throws Exception {
        List<Meetpunt> punten = new ArrayList<>();
        for (String g : PROEFSET) {
            Path ir = golden.resolve(g).resolve("ir.json");
            List<Path> paden = cases(golden.resolve(g));
            List<JsonNode> cases = new ArrayList<>();
            for (Path p : paden) {
                JsonNode c = JSON.readTree(Files.readString(p));
                if (!c.has("expectedFault")) cases.add(c);
            }
            for (DroolsBackend.Branch tak : TAKKEN) {
                Meetpunt m = new Meetpunt(g, tak, cases);
                m.verdict = tak == DroolsBackend.Branch.EXEC_MODEL ? CaseRunner.viaExecModel(ir, paden)
                        : CaseRunner.viaTruthMaintenance(ir, paden);
                if (m.verdict.status().equals("PLAYED")) m.bouw(ir);
                punten.add(m);
            }
        }
        for (Meetpunt m : punten)
            if (m.speelt()) for (JsonNode c : m.cases) for (int i = 0; i < OPWARMEN; i++) m.runner.meet(c);
        for (Meetpunt m : punten) if (m.speelt()) m.meet();

        uit.append("## Per golden van de proefset\n\n");
        uit.append("| golden | cases | gespeeld exec / tms | vuringen exec | vuringen tms | vuren+terugzetten exec (ms)")
                .append(" | tms (ms) | tms/exec | bouw exec (ms) | bouw tms (ms) | regels exec | regels tms")
                .append(" | feiten eind exec | feiten eind tms |\n");
        uit.append("|---|---:|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|\n");
        double somExec = 0, somTms = 0;
        long vurExec = 0, vurTms = 0, cases = 0;
        boolean volledig = true;
        for (int i = 0; i < punten.size(); i += 2) {
            Meetpunt e = punten.get(i), t = punten.get(i + 1);
            cases += e.cases.size();
            uit.append("| ").append(e.golden).append(" | ").append(e.cases.size()).append(" | ")
                    .append(gespeeld(e)).append(" / ").append(gespeeld(t)).append(" | ")
                    .append(e.speelt() ? e.vuringen() : e.niet()).append(" | ")
                    .append(t.speelt() ? t.vuringen() : t.niet()).append(" | ")
                    .append(e.speelt() ? ms(e.vuurMs) : e.niet()).append(" | ")
                    .append(t.speelt() ? ms(t.vuurMs) : t.niet()).append(" | ")
                    .append(e.speelt() && t.speelt() ? String.format(Locale.ROOT, "%.2f", t.vuurMs / e.vuurMs) : "–")
                    .append(" | ").append(e.speelt() ? ms(e.bouwMs) : e.niet()).append(" | ")
                    .append(t.speelt() ? ms(t.bouwMs) : t.niet()).append(" | ")
                    .append(e.speelt() ? e.regels : e.niet()).append(" | ")
                    .append(t.speelt() ? t.regels : t.niet()).append(" | ")
                    .append(e.speelt() ? e.feiten : e.niet()).append(" | ")
                    .append(t.speelt() ? t.feiten : t.niet()).append(" |\n");
            if (e.speelt() && t.speelt()) {
                somExec += e.vuurMs;
                somTms += t.vuurMs;
                vurExec += e.vuringenMin;
                vurTms += t.vuringenMin;
            } else {
                volledig = false;
            }
        }
        uit.append("| **totaal**").append(volledig ? "" : " (alleen goldens die beide takken spelen)").append(" | ")
                .append(cases).append(" | | ").append(vurExec).append(" | ").append(vurTms).append(" | ").append(ms(somExec))
                .append(" | ").append(ms(somTms)).append(" | ")
                .append(somExec > 0 ? String.format(Locale.ROOT, "%.2f", somTms / somExec) : "–")
                .append(" | | | | | | |\n\n");

        uit.append("## Per defaults-groep (proeftak)\n\n");
        uit.append("| golden | groep | vorm | regels | soorten stuurfeiten | stuurfeiten eind: som | max per case")
                .append(" | waarvan los: som | max per case |\n");
        uit.append("|---|---|---|---:|---|---:|---:|---:|---:|\n");
        List<String> zonder = new ArrayList<>();
        for (Meetpunt t : punten) {
            if (t.tak != DroolsBackend.Branch.TRUTH_MAINTENANCE || !t.speelt()) continue;
            if (t.groepen.isEmpty()) zonder.add(t.golden);
            for (TmsTranspiler.Defaultsgroep g : t.groepen) {
                long[] n = t.stuurfeiten.get(g.naam());
                uit.append("| ").append(t.golden).append(" | ").append(g.naam()).append(" | ")
                        .append(g.algemeen() ? "algemeen" : "eenvoudig").append(" | ").append(g.regels()).append(" | ")
                        .append(g.stuurfeiten().isEmpty() ? "geen" : String.join(", ", g.stuurfeiten())).append(" | ")
                        .append(n[0]).append(" | ").append(n[1]).append(" | ").append(n[2]).append(" | ").append(n[3])
                        .append(" |\n");
            }
        }
        uit.append("\nZonder defaults-groep: ").append(zonder.isEmpty() ? "geen" : String.join(", ", zonder)).append(".\n");
        for (Meetpunt m : punten)
            if (!m.speelt())
                uit.append("\n- ").append(m.golden).append(" op ").append(m.tak).append(": ").append(m.verdict).append('\n');
        uit.append('\n');
    }

    private static String gespeeld(Meetpunt m) {
        return m.verdict.status().equals("PLAYED") ? m.verdict.casesPlayed() + "/" + m.cases.size() : m.niet();
    }

    // ---------------- the corpus ----------------

    private static void corpus(Path golden, StringBuilder uit) throws Exception {
        List<Path> mappen;
        try (Stream<Path> s = Files.walk(golden)) {
            mappen = s.filter(p -> p.getFileName().toString().equals("ir.json")).map(Path::getParent).sorted().toList();
        }
        Map<String, Integer> gespeeld = new LinkedHashMap<>();
        List<String> mislukt = new ArrayList<>();
        Map<String, Map<String, List<String>>> geweigerd = new TreeMap<>();
        int zonderCases = 0, gespeeldeCases = 0;
        for (Path g : mappen) {
            List<Path> zaken = cases(g);
            if (zaken.isEmpty()) {
                zonderCases++;
                continue;
            }
            String naam = golden.relativize(g).toString();
            CaseRunner.Result r;
            try {
                r = CaseRunner.viaTruthMaintenance(g.resolve("ir.json"), zaken);
            } catch (RuntimeException e) {
                mislukt.add(naam + ": " + e);
                continue;
            }
            switch (r.status()) {
                case "PLAYED" -> {
                    gespeeld.put(naam, r.casesPlayed());
                    gespeeldeCases += r.casesPlayed();
                }
                case "EXCLUDE" -> geweigerd.computeIfAbsent(familie(r.reason()), f -> new TreeMap<>())
                        .computeIfAbsent(vorm(r.reason()), v -> new ArrayList<>()).add(naam);
                default -> mislukt.add(naam + ": " + r.caseFailures());
            }
        }
        int uitgesloten = geweigerd.values().stream().flatMap(m -> m.values().stream()).mapToInt(List::size).sum();
        uit.append("## Corpusdekking van de proeftak\n\n");
        uit.append(mappen.size()).append(" goldens onder corpus/golden, waarvan ").append(mappen.size() - zonderCases)
                .append(" met cases: PLAYED ").append(gespeeld.size()).append(" (").append(gespeeldeCases)
                .append(" cases), FAIL ").append(mislukt.size()).append(", EXCLUDE ").append(uitgesloten)
                .append("; zonder cases ").append(zonderCases).append(".\n\n");

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

        uit.append("### Gespeeld\n\n| golden | cases |\n|---|---:|\n");
        gespeeld.forEach((naam, n) -> uit.append("| ").append(naam).append(" | ").append(n).append(" |\n"));
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

    private static String ms(double ms) {
        return String.format(Locale.ROOT, "%.3f", ms);
    }
}
