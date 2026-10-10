package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import dev.uchar.regelspraak.loader.IrLoader;
import dev.uchar.regelspraak.runtime.RegelSpraakRuntime;

import org.kie.api.KieBase;
import org.kie.api.definition.KiePackage;
import org.kie.api.event.rule.ObjectDeletedEvent;
import org.kie.api.event.rule.ObjectInsertedEvent;
import org.kie.api.event.rule.ObjectUpdatedEvent;
import org.kie.api.event.rule.RuleRuntimeEventListener;
import org.kie.api.runtime.KieSession;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The scale test of card tms-omschakeling (point 9): a harness, not a witness. Wall-clock times depend on the machine
 * and its load, so no test may assert them; its name therefore ends without {@code Test}, and neither surefire nor a
 * gate runs it.
 *
 * <p><b>The model</b> is {@code src/test/resources/schaal/schaal.rgs}, written in RegelSpraak so that it uses the forms
 * the frontend gives a rule set of the distribution, and its {@code ir.json} beside it is what
 * {@code frontends/regelspraak} emits for it (the verifier passes it). One Kustwacht, Wachters, and Vondsten per
 * Wachter, with the card's forms: (a) a rule per Vondst from its input times a parameter ("waarde"); (b) a
 * prioritized-defaults group on the Vondst, an unconditional lower default and a higher one that reads (a)
 * ("beloning"); (c) a sum ("wachtsom") and (d) a maximum ("hoogste beloning") per Wachter over (b); (e) a sum over all
 * Wachters on the Kustwacht ("kusttotaal"); (f) a consistency per Wachter on (c) ("wachtsomgrens"); (g) a recursive
 * creation chain of depth 3 per Wachter ("werf de opvolger": every Wachter of rang below 3 makes an opvolger one rang
 * up, so an input Wachter of rang 0 gets three). No gate regenerates the {@code ir.json}; after a change of the source,
 * emit it again from the root of the repository:
 * <pre>
 *   make -C frontends/regelspraak ir FILE=$PWD/backends/drools/src/test/resources/schaal/schaal.rgs \
 *       OUT=$PWD/backends/drools/src/test/resources/schaal/ir.json
 * </pre>
 *
 * <p><b>The case</b> ({@link #casus}): one Kustwacht {@code K}; N Wachters {@code W1}…{@code WN} of rang 0, each a
 * Wachter of {@code K}; ten Vondsten {@code Vi_j} per Wachter, of gewicht 5 + ((7i + 11j) mod 36), so between 5 and 40;
 * the parameter vondstfactor 2. Both sides of the conditions are reached: the higher default of (b) holds where the
 * gewicht is above 25 (about 42% of the Vondsten), and (f) fails where the wachtsom reaches 300 (W7 among the first
 * eight, about 22% of the Wachters).
 *
 * <p><b>A point</b> is one branch at one N, in a JVM of its own with {@code -Xmx16g}: the build of the document
 * ({@link DroolsBackend#build}: load, beans, lowering, KieBase), once to warm up and then the median of three, the beans
 * cached per JVM by content so that javac counts in the warm-up only; R, the rules in the KieBase; and one warm-up fire
 * and three measured ones, each in a fresh session that {@link CaseRunner#metSessie} sets up as the replay does. Of a
 * fire it measures the firings, the time of {@link DroolsBackend#vuur} and that of
 * {@link DroolsBackend#zetAfgeleideWaarden} apart, N0 (the facts just before the fire), the peak of the facts during
 * the fire (a listener that counts insertions and deletions from N0 on and keeps the running maximum; at the end its
 * count must equal the session's), and the heap in use after {@code System.gc()} with the session open. The cap is
 * {@code Integer.MAX_VALUE}, so the measurement is never cut off: the cap that grows with the input is step 3 of the
 * card, and this harness reports the firings and firings / (R × N0) beside it. The kusttotaal and the number of
 * inconsistencies go with every point, so that both branches can be seen to give the same answer. The table gives the
 * median per column, and per branch the slope of fire plus write-back in log-log from N = 1 000 to N = 10 000.
 *
 * <p>Run it from the root of the repository; without arguments it measures every point, each in a child JVM, and
 * prints Markdown on standard output:
 * <pre>
 *   mvn -o -q -f backends/drools/pom.xml test-compile
 *   CP=$(mktemp -d)/cp.txt
 *   mvn -o -q -f backends/drools/pom.xml dependency:build-classpath -Dmdep.outputFile="$CP"
 *   java -cp "backends/drools/target/test-classes:backends/drools/target/classes:$(cat "$CP")" \
 *       dev.uchar.regelspraak.cases.Schaalproef
 * </pre>
 * With branch names as arguments it measures those branches only; with {@code punt <branch> <N>} it measures one
 * point in its own JVM and prints its line; with {@code controle <N>} it replays a case of N Wachters with its expected
 * values through both branches; with {@code case <N> <file>} it writes that case, for the reference interpreter and
 * clingo.
 *
 * <p>The second fire after one input change (card point 9) is not measured yet: it needs the public fire of step 3 of
 * the card, and step 4 adds it where {@link #punt} says.
 */
public final class Schaalproef {

    private Schaalproef() {}

    private static final ObjectMapper JSON = new ObjectMapper();

    static final List<Integer> NS = List.of(100, 1_000, 10_000);

    static final List<DroolsBackend.Branch> TAKKEN =
            List.of(DroolsBackend.Branch.EXEC_MODEL, DroolsBackend.Branch.TRUTH_MAINTENANCE);

    private static final int OPWARMEN = 1, HERHALINGEN = 3;

    static final int VONDSTEN_PER_WACHTER = 10;

    private static final BigDecimal VONDSTFACTOR = BigDecimal.valueOf(2), DREMPEL = BigDecimal.valueOf(50),
            WACHTSOMGRENS = BigDecimal.valueOf(300), LAGERE_BELONING = new BigDecimal("1.00");

    public static void main(String[] args) throws Exception {
        if (args.length == 0 || !List.of("punt", "controle", "case").contains(args[0])) {
            System.exit(alles(args.length == 0 ? TAKKEN
                    : Arrays.stream(args).map(DroolsBackend.Branch::valueOf).toList()));
            return;
        }
        switch (args[0]) {
            case "punt" -> System.exit(punt(DroolsBackend.Branch.valueOf(args[1]), Integer.parseInt(args[2])));
            case "controle" -> System.exit(controle(Integer.parseInt(args[1])));
            case "case" -> Files.writeString(Path.of(args[2]),
                    JSON.writerWithDefaultPrettyPrinter().writeValueAsString(casus(Integer.parseInt(args[1]), true)));
            default -> throw new IllegalArgumentException("onbekende opdracht " + args[0]
                    + "; gebruik: Schaalproef [<tak>... | punt <tak> <N> | controle <N> | case <N> <bestand>]");
        }
    }

    // ---------------- the model and the case ----------------

    static Path model() throws Exception {
        java.net.URL url = Schaalproef.class.getResource("/schaal/ir.json");
        if (url == null)
            throw new IllegalStateException("schaal/ir.json niet op het klassepad: draai eerst test-compile");
        return Path.of(url.toURI());
    }

    static int gewicht(int wachter, int vondst) {
        return 5 + ((7 * wachter + 11 * vondst) % 36);
    }

    /** The case of {@code n} Wachters; with {@code verwacht} also every value the model derives on an input object, and
     *  the inconsistency of every Wachter whose wachtsom reaches the bound, as the case schema spells them. */
    static ObjectNode casus(int n, boolean verwacht) {
        JsonNodeFactory f = JsonNodeFactory.instance;
        ObjectNode doc = f.objectNode();
        doc.put("format", "uchar-case-v0");
        doc.putNull("asOfDate");
        ObjectNode input = doc.putObject("input");
        ArrayNode instanties = input.putArray("instances");
        ArrayNode verwachtingen = f.arrayNode();
        instanties.add(instantie(f, "K", "Kustwacht", f.objectNode(), f.objectNode()));
        BigDecimal kusttotaal = new BigDecimal("0.00");
        boolean inconsistent = false;
        for (int i = 1; i <= n; i++) {
            String wachter = "W" + i;
            ObjectNode attributen = f.objectNode();
            attributen.putObject("rang").put("number", "0");
            ObjectNode rollen = f.objectNode();
            rollen.putArray("kustwacht").add("K");
            instanties.add(instantie(f, wachter, "Wachter", attributen, rollen));
            BigDecimal wachtsom = new BigDecimal("0.00"), hoogste = null;
            for (int j = 1; j <= VONDSTEN_PER_WACHTER; j++) {
                String vondst = "V" + i + "_" + j;
                BigDecimal gewicht = BigDecimal.valueOf(gewicht(i, j)).setScale(2);
                ObjectNode va = f.objectNode();
                va.putObject("gewicht").put("number", gewicht.toPlainString());
                ObjectNode vr = f.objectNode();
                vr.putArray("beheerder").add(wachter);
                instanties.add(instantie(f, vondst, "Vondst", va, vr));
                BigDecimal waarde = gewicht.multiply(VONDSTFACTOR);
                BigDecimal beloning = waarde.compareTo(DREMPEL) > 0 ? waarde : LAGERE_BELONING;
                wachtsom = wachtsom.add(beloning);
                hoogste = hoogste == null || beloning.compareTo(hoogste) > 0 ? beloning : hoogste;
                waarde(verwachtingen, vondst, "waarde", waarde);
                waarde(verwachtingen, vondst, "beloning", beloning);
            }
            waarde(verwachtingen, wachter, "wachtsom", wachtsom);
            waarde(verwachtingen, wachter, "hoogste beloning", hoogste);
            if (wachtsom.compareTo(WACHTSOMGRENS) >= 0) {
                inconsistent = true;
                verwachtingen.addObject().put("kind", "inconsistent").put("rule", "wachtsomgrens").put("label", wachter);
            }
            kusttotaal = kusttotaal.add(wachtsom);
        }
        waarde(verwachtingen, "K", "kusttotaal", kusttotaal);
        if (!inconsistent) verwachtingen.addObject().put("kind", "consistent");
        input.putObject("parameters").putObject("vondstfactor").put("number", VONDSTFACTOR.toPlainString());
        doc.set("expected", verwacht ? verwachtingen : f.arrayNode());
        return doc;
    }

    private static ObjectNode instantie(JsonNodeFactory f, String label, String type, ObjectNode attributen,
                                        ObjectNode rollen) {
        ObjectNode i = f.objectNode();
        i.put("label", label);
        i.put("objectType", type);
        i.set("attributes", attributen);
        i.set("roles", rollen);
        i.putArray("characteristics");
        return i;
    }

    private static void waarde(ArrayNode verwachtingen, String label, String attribuut, BigDecimal waarde) {
        ObjectNode v = verwachtingen.addObject();
        v.put("kind", "attributeValue").put("label", label).put("attribute", attribuut);
        v.putObject("value").put("number", waarde.toPlainString());
    }

    /** Replays the case of {@code n} Wachters with its expected values through both branches, the replay's own
     *  comparison: the check that the harness measures a model both branches answer as the case says. */
    private static int controle(int n) throws Exception {
        Path ir = model();
        JsonNode c = casus(n, true);
        int fout = 0;
        for (DroolsBackend.Branch tak : TAKKEN) {
            String verdict = CaseRunner.speelCase(ir, c, tak);
            System.out.println(tak + ": " + (verdict == null ? "gelijk aan de verwachting" : verdict));
            if (verdict != null) fout = 1;
        }
        return fout;
    }

    // ---------------- one point ----------------

    /** One fire of the case: its firings, the fire and the write-back in nanoseconds, the facts just before the fire
     *  and their peak during it, the heap in use after a collection with the session open, and two outcomes that both
     *  branches must give alike. */
    record Vuurmeting(int vuringen, long vuurNanos, long terugzetNanos, long n0, long piek, long heap,
                      String kusttotaal, int inconsistent) {}

    /** Measures one point in this JVM and prints one line, {@code PUNT} with its fields tab-separated, or
     *  {@code WEIGERT} with the reason where the branch refuses a form of the model. */
    private static int punt(DroolsBackend.Branch tak, int n) throws Exception {
        Path ir = model();
        long[] bouw = new long[HERHALINGEN];
        DroolsBackend.Build b = null;
        try {
            for (int i = -OPWARMEN; i < HERHALINGEN; i++) {
                long begin = System.nanoTime();
                b = DroolsBackend.build(ir, tak);
                if (i >= 0) bouw[i] = System.nanoTime() - begin;
            }
        } catch (CaseRunner.Excluded e) {
            System.out.println("WEIGERT\t" + tak + "\t" + n + "\t" + e.getMessage());
            return 2;
        } catch (IrLoader.Refusal e) {
            System.out.println("WEIGERT\t" + tak + "\t" + n + "\tloader: " + e.getMessage());
            return 2;
        }
        int regels = regels(b.kieBase());
        CaseRunner runner = CaseRunner.voorMeting(ir, tak);
        JsonNode c = casus(n, false);
        List<Vuurmeting> metingen = new ArrayList<>();
        for (int i = -OPWARMEN; i < HERHALINGEN; i++) {
            Vuurmeting m = runner.metSessie(c, Schaalproef::vuur);
            if (i >= 0) metingen.add(m);
        }
        // The second fire after one input change (card point 9) comes here in step 4 of the card, through the public
        // fire of step 3: change the gewicht of one Vondst in the session, fire again, and set its firings and time
        // beside those of a fresh session with the changed case.
        Vuurmeting eerste = metingen.get(0);
        for (Vuurmeting m : metingen)
            if (m.n0() != eerste.n0() || !m.kusttotaal().equals(eerste.kusttotaal())
                    || m.inconsistent() != eerste.inconsistent())
                throw new IllegalStateException("herhalingen verschillen in invoer of uitkomst: " + metingen);
        System.out.println(String.join("\t", "PUNT", tak.name(), String.valueOf(n),
                bereik(metingen, Vuurmeting::vuringen),
                ms(mediaan(metingen, Vuurmeting::vuurNanos)), ms(mediaan(metingen, Vuurmeting::terugzetNanos)),
                ms(mediaan(metingen, m -> m.vuurNanos() + m.terugzetNanos())),
                bereik(metingen, m -> (int) m.piek()), String.valueOf(mediaan(metingen, Vuurmeting::heap) >> 20),
                ms(mediaan(bouw)), String.valueOf(regels), String.valueOf(eerste.n0()), eerste.kusttotaal(),
                String.valueOf(eerste.inconsistent())));
        return 0;
    }

    /** Fires one set-up session through the driver under a cap that never cuts it off, writes back, and measures. */
    private static Vuurmeting vuur(DroolsBackend.Build bouw, KieSession sessie, Map<String, Object> labels) {
        Feitenteller teller = new Feitenteller(sessie.getFactCount());
        sessie.addEventListener(teller);
        long begin = System.nanoTime();
        DroolsBackend.Vuring vuring = DroolsBackend.vuur(bouw, sessie, Integer.MAX_VALUE);
        long gevuurd = System.nanoTime();
        sessie.removeEventListener(teller);
        if (vuring.geenFixpunt() != null) throw new IllegalStateException(vuring.geenFixpunt());
        // counted before the write-back, which states the mark that closes the session on the truth-maintenance branch
        if (teller.stand != sessie.getFactCount())
            throw new IllegalStateException("de feitenteller staat op " + teller.stand + ", de sessie houdt "
                    + sessie.getFactCount() + " feiten");
        DroolsBackend.zetAfgeleideWaarden(bouw, sessie);
        long teruggezet = System.nanoTime();
        long heap = gebruikt();
        Object kusttotaal = RegelSpraakRuntime.get(labels.get("K"), "kusttotaal");
        int inconsistent = sessie.getObjects(o -> o.getClass().getSimpleName().equals("Inconsistentie")).size();
        return new Vuurmeting(vuring.vuringen(), gevuurd - begin, teruggezet - gevuurd, teller.n0, teller.piek, heap,
                kusttotaal instanceof BigDecimal d ? d.toPlainString() : String.valueOf(kusttotaal), inconsistent);
    }

    /** Counts the facts of a session from {@code n0} on through every insertion and deletion and keeps the running
     *  maximum: the count at the end of a fire understates what the fire held, since truth maintenance inserts
     *  intermediate states and retracts them. A logical insert of a fact equal to one that stands adds a justification
     *  and inserts nothing, so it counts as nothing. */
    private static final class Feitenteller implements RuleRuntimeEventListener {
        final long n0;
        long stand, piek;

        Feitenteller(long n0) {
            this.n0 = n0;
            this.stand = n0;
            this.piek = n0;
        }

        @Override public void objectInserted(ObjectInsertedEvent e) {
            if (++stand > piek) piek = stand;
        }

        @Override public void objectUpdated(ObjectUpdatedEvent e) {}

        @Override public void objectDeleted(ObjectDeletedEvent e) {
            stand--;
        }
    }

    private static long gebruikt() {
        Runtime rt = Runtime.getRuntime();
        System.gc();
        System.gc();
        return rt.totalMemory() - rt.freeMemory();
    }

    // ---------------- every point ----------------

    /** A point as its child JVM reported it: the fields of its {@code PUNT} line, or the refusal or fault. */
    private record Uitslag(DroolsBackend.Branch tak, int n, String[] velden, String anders) {
        String veld(int i) {
            return velden[i];
        }

        double ms(int i) {
            return Double.parseDouble(velden[i]);
        }
    }

    /** Measures every point of {@code takken}, each in a child JVM of its own, and prints the table. Returns 1 where a
     *  point was refused or failed, which the table shows. */
    private static int alles(List<DroolsBackend.Branch> takken) throws Exception {
        String java = Path.of(System.getProperty("java.home"), "bin", "java").toString();
        String cp = System.getProperty("java.class.path");
        Map<DroolsBackend.Branch, Map<Integer, Uitslag>> uitslagen = new LinkedHashMap<>();
        boolean volledig = true;
        for (DroolsBackend.Branch tak : takken)
            for (int n : NS) {
                Process p = new ProcessBuilder(java, "-Xmx16g", "-cp", cp, Schaalproef.class.getName(), "punt",
                        tak.name(), String.valueOf(n)).redirectError(ProcessBuilder.Redirect.INHERIT).start();
                String uit = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                int code = p.waitFor();
                String regel = uit.lines().filter(l -> l.startsWith("PUNT\t") || l.startsWith("WEIGERT\t"))
                        .reduce((a, z) -> z).orElse(null);
                Uitslag u;
                if (code == 0 && regel != null && regel.startsWith("PUNT\t"))
                    u = new Uitslag(tak, n, regel.split("\t"), null);
                else {
                    volledig = false;
                    u = new Uitslag(tak, n, null, regel != null ? regel.replace('\t', ' ')
                            : "kind-JVM eindigde met code " + code + " zonder regel");
                }
                uitslagen.computeIfAbsent(tak, t -> new LinkedHashMap<>()).put(n, u);
                System.err.println("schaalproef: " + tak + " N=" + n + " klaar");
            }
        System.out.print(tabel(uitslagen));
        return volledig ? 0 : 1;
    }

    private static String tabel(Map<DroolsBackend.Branch, Map<Integer, Uitslag>> uitslagen) {
        Runtime rt = Runtime.getRuntime();
        StringBuilder uit = new StringBuilder("# Schaalproef (kaart tms-omschakeling, punt 9)\n\n");
        uit.append("- JVM: ").append(System.getProperty("java.vm.name")).append(' ')
                .append(System.getProperty("java.runtime.version")).append(", ").append(System.getProperty("os.arch"))
                .append(", ").append(rt.availableProcessors()).append(" processors; elk punt in een eigen JVM met -Xmx16g.\n");
        uit.append("- Per punt: bouw 1 opwarmer + mediaan van ").append(HERHALINGEN).append("; vuren ").append(OPWARMEN)
                .append(" opwarmer + mediaan van ").append(HERHALINGEN)
                .append(", elk in een verse sessie; plafond Integer.MAX_VALUE.\n\n");
        uit.append("| tak | N | vuringen | vuren (ms) | terugzetten (ms) | vuren+terugzetten (ms) | piekfeiten | heap (MB)")
                .append(" | bouw (ms) | R | N0 | vuringen/(R×N0) | kusttotaal | inconsistent |\n");
        uit.append("|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|\n");
        for (Map<Integer, Uitslag> perN : uitslagen.values())
            for (Uitslag u : perN.values()) {
                uit.append("| ").append(u.tak()).append(" | ").append(u.n()).append(" | ");
                if (u.anders() != null) {
                    uit.append(u.anders().replace("|", "\\|")).append(" |||||||||||| \n");
                    continue;
                }
                String vuringen = u.veld(3);
                double r = Double.parseDouble(u.veld(10)), n0 = Double.parseDouble(u.veld(11));
                uit.append(vuringen).append(" | ").append(u.veld(4)).append(" | ").append(u.veld(5)).append(" | ")
                        .append(u.veld(6)).append(" | ").append(u.veld(7)).append(" | ").append(u.veld(8)).append(" | ")
                        .append(u.veld(9)).append(" | ").append(u.veld(10)).append(" | ").append(u.veld(11)).append(" | ")
                        .append(String.format(Locale.ROOT, "%.3f", eerste(vuringen) / (r * n0))).append(" | ")
                        .append(u.veld(12)).append(" | ").append(u.veld(13)).append(" |\n");
            }
        uit.append('\n');
        for (Map.Entry<DroolsBackend.Branch, Map<Integer, Uitslag>> e : uitslagen.entrySet()) {
            Uitslag a = e.getValue().get(1_000), z = e.getValue().get(10_000);
            uit.append("- Helling ").append(e.getKey()).append(" (vuren+terugzetten, log-log, N = 1 000 → 10 000): ")
                    .append(a != null && z != null && a.anders() == null && z.anders() == null
                            ? String.format(Locale.ROOT, "%.2f", Math.log10(z.ms(6) / a.ms(6))) : "–")
                    .append('\n');
        }
        Map<Integer, Uitslag> oud = uitslagen.get(DroolsBackend.Branch.EXEC_MODEL),
                nieuw = uitslagen.get(DroolsBackend.Branch.TRUTH_MAINTENANCE);
        if (oud != null && nieuw != null)
            for (int n : NS) {
                Uitslag o = oud.get(n), t = nieuw.get(n);
                if (o == null || t == null || o.anders() != null || t.anders() != null) continue;
                uit.append("- N = ").append(n).append(": vuren+terugzetten nieuw/oud ")
                        .append(String.format(Locale.ROOT, "%.2f", t.ms(6) / o.ms(6))).append("; uitkomst ")
                        .append(o.veld(12).equals(t.veld(12)) && o.veld(13).equals(t.veld(13)) ? "gelijk" : "VERSCHILLEND")
                        .append('\n');
            }
        return uit.toString();
    }

    // ---------------- helpers ----------------

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

    private static long mediaan(List<Vuurmeting> metingen, java.util.function.ToLongFunction<Vuurmeting> veld) {
        return mediaan(metingen.stream().mapToLong(veld).toArray());
    }

    /** The value of a count over the repetitions, or its range where they differ. */
    private static String bereik(List<Vuurmeting> metingen, java.util.function.ToIntFunction<Vuurmeting> veld) {
        int min = metingen.stream().mapToInt(veld).min().orElseThrow();
        int max = metingen.stream().mapToInt(veld).max().orElseThrow();
        return min == max ? String.valueOf(min) : min + "–" + max;
    }

    private static double eerste(String bereik) {
        int streep = bereik.indexOf('–');
        return Double.parseDouble(streep < 0 ? bereik : bereik.substring(0, streep));
    }

    private static String ms(long nanos) {
        return String.format(Locale.ROOT, "%.1f", nanos / 1e6);
    }
}
