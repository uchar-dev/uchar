package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import dev.uchar.regelspraak.loader.IrLoader;
import dev.uchar.regelspraak.runtime.RegelSpraakRuntime;
import dev.uchar.regelspraak.transpiler.tms.Fixpunt;
import dev.uchar.regelspraak.transpiler.tms.Fout;

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
import java.util.IdentityHashMap;
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
 * <p><b>The model without (g)</b>, {@code schaal/zonder-creatie/}, is the same source without the rule group of the
 * creation chain, emitted the same way: the same declarations, so the same case serves both, and the same answers on
 * every input object. The second fire runs on it. A creation is stated and does not lapse (RUNTIME-SEMANTICS ch. 5.5),
 * so after a change of the input a second fire would keep what the first made and initialised, where a fresh session
 * evaluates the creations anew; {@link DroolsBackend#vuur} therefore refuses a second fire on a model with a creation.
 *
 * <p><b>The case</b> ({@link #casus}): one Kustwacht {@code K}; N Wachters {@code W1}…{@code WN} of rang 0, each a
 * Wachter of {@code K}; ten Vondsten {@code Vi_j} per Wachter, of gewicht 5 + ((7i + 11j) mod 36), so between 5 and 40;
 * the parameter vondstfactor 2. Both sides of the conditions are reached: the higher default of (b) holds where the
 * gewicht is above 25 (about 42% of the Vondsten), and (f) fails where the wachtsom reaches 300 (W7 among the first
 * eight, about 22% of the Wachters). <b>The change</b> of the second fire sets the gewicht of {@code V1_1} from 23 to
 * 40, which every form of (a) to (f) follows: its waarde goes from 46 to 80, so the higher default of (b) takes over
 * (beloning 1.00 becomes 80), the wachtsom of W1 goes from 270 to 349 and its hoogste beloning from 78 to 80, the
 * kusttotaal follows, and W1 becomes inconsistent.
 *
 * <p><b>A point</b> is one branch at one N, in a JVM of its own with {@code -Xmx16g}: the build of the document
 * ({@link DroolsBackend#build}: load, beans, lowering, KieBase), once to warm up and then the median of three, the beans
 * cached per JVM by content so that javac counts in the warm-up only; R, the rules in the KieBase; and one warm-up fire
 * and three measured ones, each in a fresh session that {@link CaseRunner#metSessie} sets up as the replay does, through
 * {@link DroolsBackend#voegIn}. Of a fire it measures the firings and the cap of the public {@link DroolsBackend#vuur},
 * the cap a consumer gets, the time of that fire and that of {@link DroolsBackend#zetAfgeleideWaarden} apart, N0 (the
 * facts just before the fire), the peak of the facts during the fire (a listener that counts insertions and deletions
 * from N0 on and keeps the running maximum; at the end its count must equal the session's), and the heap in use after
 * {@code System.gc()} with the session open. The kusttotaal and the number of inconsistencies go with every point, so
 * that the points can be seen to give the same answer. The table gives the median per column, the load of the machine
 * at the start of the point ({@code uptime}), the firings against the cap, and per branch the slope of fire plus
 * write-back in log-log from N = 1 000 to N = 10 000. Until the switch the harness measured the destructive exec-model
 * branch beside the one with truth maintenance: under its fixed cap of 100 000 firings it did not reach its fixpoint at
 * N = 10 000, and its measurement without a cap is in the report of step 1 of the card.
 *
 * <p><b>A point of the second fire</b> is one N on the model without (g), in a JVM of its own with {@code -Xmx16g}, one
 * warm-up and three measured rounds. A round fills a session through the public entry, fires it ({@link
 * DroolsBackend#vuur}) and reads it ({@link DroolsBackend#lees}), makes the change on the bean of {@code V1_1} and tells
 * the session ({@code update}), fires again and reads again; beside it a fresh session with the changed case, filled,
 * fired and read. The two readings after the change must be equal, every value, characteristic, fault and
 * inconsistency named by its case label, and the reading must differ from the one before the change; else the point
 * fails loudly. It measures the first fire, the second (the change and the fire together), and the fresh session, its
 * set-up (session, beans from the case, {@code voegIn}) and its fire apart.
 *
 * <p>Run it from the root of the repository; without arguments it measures every point of the exec-model branch, each
 * in a child JVM, and prints Markdown on standard output:
 * <pre>
 *   mvn -o -q -f backends/drools/pom.xml test-compile
 *   CP=$(mktemp -d)/cp.txt
 *   mvn -o -q -f backends/drools/pom.xml dependency:build-classpath -Dmdep.outputFile="$CP"
 *   java -cp "backends/drools/target/test-classes:backends/drools/target/classes:$(cat "$CP")" \
 *       dev.uchar.regelspraak.cases.Schaalproef
 * </pre>
 * With branch names as arguments it measures those branches; with {@code punt <branch> <N>} it measures one point in
 * its own JVM and prints its line; with {@code tweede} every point of the second fire, and with {@code tweedepunt <N>}
 * one; with {@code controle <N>} it replays a case of N Wachters with its expected values, and the changed case with
 * its own, through the exec-model branch on both models; with {@code case <N> <file> [gewijzigd]} it writes that
 * case, or the changed one, for the reference interpreter and clingo.
 */
public final class Schaalproef {

    private Schaalproef() {}

    private static final ObjectMapper JSON = new ObjectMapper();

    static final List<Integer> NS = List.of(100, 1_000, 10_000);

    /** The exec-model branch: the one the points measure by default, and the only one that reads a session between two
     *  fires ({@link DroolsBackend#lees}). */
    static final DroolsBackend.Branch TAK = DroolsBackend.Branch.EXEC_MODEL;

    private static final int OPWARMEN = 1, HERHALINGEN = 3;

    static final int VONDSTEN_PER_WACHTER = 10;

    private static final BigDecimal VONDSTFACTOR = BigDecimal.valueOf(2), DREMPEL = BigDecimal.valueOf(50),
            WACHTSOMGRENS = BigDecimal.valueOf(300), LAGERE_BELONING = new BigDecimal("1.00");

    /** The change of the second fire: the Vondst and its new gewicht. */
    static final String GEWIJZIGDE_VONDST = "V1_1";
    static final BigDecimal GEWIJZIGD_GEWICHT = new BigDecimal("40.00");

    public static void main(String[] args) throws Exception {
        List<String> opdrachten = List.of("punt", "tweede", "tweedepunt", "controle", "case");
        if (args.length == 0 || !opdrachten.contains(args[0])) {
            System.exit(alles(args.length == 0 ? List.of(TAK)
                    : Arrays.stream(args).map(DroolsBackend.Branch::valueOf).toList()));
            return;
        }
        switch (args[0]) {
            case "punt" -> System.exit(punt(DroolsBackend.Branch.valueOf(args[1]), Integer.parseInt(args[2])));
            case "tweede" -> System.exit(tweede());
            case "tweedepunt" -> System.exit(tweedepunt(Integer.parseInt(args[1])));
            case "controle" -> System.exit(controle(Integer.parseInt(args[1])));
            case "case" -> Files.writeString(Path.of(args[2]), JSON.writerWithDefaultPrettyPrinter().writeValueAsString(
                    casus(Integer.parseInt(args[1]), true, args.length > 3 && args[3].equals("gewijzigd"))));
            default -> throw new IllegalStateException(args[0]);
        }
    }

    // ---------------- the model and the case ----------------

    static Path model() throws Exception {
        return bron("/schaal/ir.json");
    }

    /** The model without (g), the creation chain: the model of the second fire. */
    static Path modelZonderCreatie() throws Exception {
        return bron("/schaal/zonder-creatie/ir.json");
    }

    private static Path bron(String pad) throws Exception {
        java.net.URL url = Schaalproef.class.getResource(pad);
        if (url == null)
            throw new IllegalStateException(pad.substring(1) + " niet op het klassepad: draai eerst test-compile");
        return Path.of(url.toURI());
    }

    static int gewicht(int wachter, int vondst) {
        return 5 + ((7 * wachter + 11 * vondst) % 36);
    }

    /** The case of {@code n} Wachters, with {@code gewijzigd} the one after the change of the second fire; with
     *  {@code verwacht} also every value the model derives on an input object, and the inconsistency of every Wachter
     *  whose wachtsom reaches the bound, as the case schema spells them. */
    static ObjectNode casus(int n, boolean verwacht, boolean gewijzigd) {
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
                BigDecimal gewicht = gewijzigd && vondst.equals(GEWIJZIGDE_VONDST) ? GEWIJZIGD_GEWICHT
                        : BigDecimal.valueOf(gewicht(i, j)).setScale(2);
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

    /** Replays the case of {@code n} Wachters and the changed case, each with its expected values, through the
     *  exec-model branch on the model and on the model without (g), the replay's own comparison: the check that the
     *  harness measures models the branch answers as the cases say. */
    private static int controle(int n) throws Exception {
        int fout = 0;
        for (Path ir : List.of(model(), modelZonderCreatie()))
            for (boolean gewijzigd : List.of(false, true)) {
                JsonNode c = casus(n, true, gewijzigd);
                String verdict = CaseRunner.speelCase(ir, c, TAK);
                System.out.println(ir.getParent().getFileName() + (gewijzigd ? ", gewijzigd" : "") + ", " + TAK
                        + ": " + (verdict == null ? "gelijk aan de verwachting" : verdict));
                if (verdict != null) fout = 1;
            }
        return fout;
    }

    // ---------------- one point ----------------

    /** One fire of the case: its firings and the cap it fired under, the fire and the write-back in nanoseconds, the
     *  facts just before the fire and their peak during it, the heap in use after a collection with the session open,
     *  and two outcomes that every point must give alike. */
    record Vuurmeting(int vuringen, int plafond, long vuurNanos, long terugzetNanos, long n0, long piek, long heap,
                      String kusttotaal, int inconsistent) {}

    /** Measures one point in this JVM and prints one line, {@code PUNT} with its fields tab-separated,
     *  {@code WEIGERT} with the reason where the branch refuses a form of the model, or {@code GEENFIXPUNT} with the
     *  sentence of the driver where a fire ends without its fixpoint. */
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
        JsonNode c = casus(n, false, false);
        List<Vuurmeting> metingen = new ArrayList<>();
        try {
            for (int i = -OPWARMEN; i < HERHALINGEN; i++) {
                Vuurmeting m = runner.metSessie(c, Schaalproef::vuur);
                if (i >= 0) metingen.add(m);
            }
        } catch (GeenFixpunt e) {
            System.out.println("GEENFIXPUNT\t" + tak + "\t" + n + "\tna " + e.vuringen + " vuringen in "
                    + ms(e.nanos) + " ms: " + e.getMessage());
            return 3;
        }
        Vuurmeting eerste = metingen.get(0);
        for (Vuurmeting m : metingen)
            if (m.n0() != eerste.n0() || m.plafond() != eerste.plafond() || !m.kusttotaal().equals(eerste.kusttotaal())
                    || m.inconsistent() != eerste.inconsistent())
                throw new IllegalStateException("herhalingen verschillen in invoer, plafond of uitkomst: " + metingen);
        System.out.println(String.join("\t", "PUNT", tak.name(), String.valueOf(n),
                bereik(metingen, Vuurmeting::vuringen),
                ms(mediaan(metingen, Vuurmeting::vuurNanos)), ms(mediaan(metingen, Vuurmeting::terugzetNanos)),
                ms(mediaan(metingen, m -> m.vuurNanos() + m.terugzetNanos())),
                bereik(metingen, m -> (int) m.piek()), String.valueOf(mediaan(metingen, Vuurmeting::heap) >> 20),
                ms(mediaan(bouw)), String.valueOf(regels), String.valueOf(eerste.n0()), eerste.kusttotaal(),
                String.valueOf(eerste.inconsistent()), String.valueOf(eerste.plafond())));
        return 0;
    }

    /** Fires one set-up session through the public driver, under the cap a consumer gets, writes back, and measures.
     *  A run that reaches no fixpoint fails with the sentence of the driver, since its numbers would measure the cap. */
    private static Vuurmeting vuur(DroolsBackend.Build bouw, KieSession sessie, Map<String, Object> labels) {
        Feitenteller teller = new Feitenteller(sessie.getFactCount());
        sessie.addEventListener(teller);
        long begin = System.nanoTime();
        DroolsBackend.Vuring vuring = DroolsBackend.vuur(bouw, sessie);
        long gevuurd = System.nanoTime();
        sessie.removeEventListener(teller);
        if (vuring.geenFixpunt() != null) throw new GeenFixpunt(vuring, gevuurd - begin);
        // counted before the write-back, which states the mark that closes the session on the truth-maintenance branch
        if (teller.stand != sessie.getFactCount())
            throw new IllegalStateException("de feitenteller staat op " + teller.stand + ", de sessie houdt "
                    + sessie.getFactCount() + " feiten");
        DroolsBackend.zetAfgeleideWaarden(bouw, sessie);
        long teruggezet = System.nanoTime();
        long heap = gebruikt();
        Object kusttotaal = RegelSpraakRuntime.get(labels.get("K"), "kusttotaal");
        int inconsistent = sessie.getObjects(o -> o.getClass().getSimpleName().equals("Inconsistentie")).size();
        return new Vuurmeting(vuring.vuringen(), vuring.plafond(), gevuurd - begin, teruggezet - gevuurd, teller.n0,
                teller.piek, heap, kusttotaal instanceof BigDecimal d ? d.toPlainString() : String.valueOf(kusttotaal),
                inconsistent);
    }

    /** A fire that ended without its fixpoint, at the cap or at the creation budget: its numbers would measure the
     *  cap, so the point reports the sentence of the driver instead, with the firings and the time it took. */
    private static final class GeenFixpunt extends RuntimeException {
        final int vuringen;
        final long nanos;

        GeenFixpunt(DroolsBackend.Vuring vuring, long nanos) {
            super(vuring.geenFixpunt());
            this.vuringen = vuring.vuringen();
            this.nanos = nanos;
        }
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

    // ---------------- one point of the second fire ----------------

    /** The session fired twice: the firings of the first fire and of the second, the cap of the second, N0, the time
     *  in nanoseconds of the first fire and that of the change and the second fire together, and the readings before
     *  and after the change. */
    private record Herhaald(int vuringenEerste, int vuringenTweede, int plafondTweede, long n0, long eersteNanos,
                            long tweedeNanos, List<String> voor, List<String> na) {}

    /** The fresh session with the changed case: its firings, its set-up and its fire in nanoseconds, and its reading. */
    private record Vers(int vuringen, long opzetNanos, long vuurNanos, List<String> lezing) {}

    /** One round of the second fire. */
    private record Tweede(Herhaald herhaald, Vers vers) {}

    /** Measures one point of the second fire in this JVM and prints one line, {@code TWEEDE} with its fields
     *  tab-separated. */
    private static int tweedepunt(int n) throws Exception {
        Path ir = modelZonderCreatie();
        int regels = regels(DroolsBackend.build(ir, TAK).kieBase());
        CaseRunner runner = CaseRunner.voorMeting(ir, TAK);
        JsonNode c = casus(n, false, false), gewijzigd = casus(n, false, true);
        List<Tweede> rondes = new ArrayList<>();
        for (int i = -OPWARMEN; i < HERHALINGEN; i++) {
            Tweede t = ronde(runner, c, gewijzigd);
            if (i >= 0) rondes.add(t);
        }
        Herhaald eerste = rondes.get(0).herhaald();
        for (Tweede t : rondes)
            if (!t.herhaald().na().equals(eerste.na()) || t.herhaald().n0() != eerste.n0()
                    || t.herhaald().plafondTweede() != eerste.plafondTweede())
                throw new IllegalStateException("de rondes verschillen in invoer, plafond of lezing");
        int verschil = verschil(eerste.voor(), eerste.na()).size();
        System.out.println(String.join("\t", "TWEEDE", String.valueOf(n), String.valueOf(regels),
                String.valueOf(eerste.n0()), bereik(rondes, t -> t.herhaald().vuringenEerste()),
                bereik(rondes, t -> t.herhaald().vuringenTweede()), bereik(rondes, t -> t.vers().vuringen()),
                String.valueOf(eerste.plafondTweede()),
                ms(mediaanVan(rondes, t -> t.herhaald().eersteNanos())),
                ms(mediaanVan(rondes, t -> t.herhaald().tweedeNanos())),
                ms(mediaanVan(rondes, t -> t.vers().opzetNanos())), ms(mediaanVan(rondes, t -> t.vers().vuurNanos())),
                String.valueOf(eerste.na().size()), String.valueOf(verschil)));
        return 0;
    }

    /** One round: a session filled with case {@code c}, fired, read, changed, fired and read again; then a fresh
     *  session with case {@code gewijzigd}, filled, fired and read. Fails where the reading after the change differs
     *  from the fresh one, or equals the one before it. */
    private static Tweede ronde(CaseRunner runner, JsonNode c, JsonNode gewijzigd) {
        Herhaald a = runner.metSessie(c, (bouw, sessie, labels) -> {
            long n0 = sessie.getFactCount();
            long begin = System.nanoTime();
            DroolsBackend.Vuring eerste = DroolsBackend.vuur(bouw, sessie);
            long gevuurd = System.nanoTime();
            if (eerste.geenFixpunt() != null) throw new IllegalStateException("eerste fire: " + eerste.geenFixpunt());
            List<String> voor = beschrijf(DroolsBackend.lees(bouw, sessie), labels);
            Object vondst = labels.get(GEWIJZIGDE_VONDST);
            long wijzig = System.nanoTime();
            RegelSpraakRuntime.set(vondst, "gewicht", GEWIJZIGD_GEWICHT);
            sessie.update(sessie.getFactHandle(vondst), vondst);
            DroolsBackend.Vuring tweede = DroolsBackend.vuur(bouw, sessie);
            long opnieuw = System.nanoTime();
            if (tweede.geenFixpunt() != null) throw new IllegalStateException("tweede fire: " + tweede.geenFixpunt());
            List<String> na = beschrijf(DroolsBackend.lees(bouw, sessie), labels);
            return new Herhaald(eerste.vuringen(), tweede.vuringen(), tweede.plafond(), n0, gevuurd - begin,
                    opnieuw - wijzig, voor, na);
        });
        long[] start = {System.nanoTime()};
        Vers v = runner.metSessie(gewijzigd, (bouw, sessie, labels) -> {
            long opgezet = System.nanoTime();
            DroolsBackend.Vuring vuring = DroolsBackend.vuur(bouw, sessie);
            long gevuurd = System.nanoTime();
            if (vuring.geenFixpunt() != null) throw new IllegalStateException("verse sessie: " + vuring.geenFixpunt());
            List<String> lezing = beschrijf(DroolsBackend.lees(bouw, sessie), labels);
            return new Vers(vuring.vuringen(), opgezet - start[0], gevuurd - opgezet, lezing);
        });
        if (!a.na().equals(v.lezing())) {
            List<String> anders = verschil(a.na(), v.lezing());
            throw new IllegalStateException("de lezing na de tweede fire (" + a.na().size() + " regels) verschilt van die"
                    + " van een verse sessie met de gewijzigde invoer (" + v.lezing().size() + " regels), in "
                    + anders.size() + " regels" + (anders.isEmpty() ? ", alleen in hoe vaak een regel voorkomt" : "")
                    + "; de eerste: " + anders.subList(0, Math.min(10, anders.size())));
        }
        if (a.voor().equals(a.na()))
            throw new IllegalStateException("de wijziging van " + GEWIJZIGDE_VONDST + " veranderde de lezing niet");
        return new Tweede(a, v);
    }

    /** {@code f} as sorted lines, every object named by its case label: a derived value as its bean field takes it, a
     *  derived characteristic, a fault and an inconsistency. The model without (g) makes no object and lays no link,
     *  so a reading with one fails. */
    private static List<String> beschrijf(Fixpunt f, Map<String, Object> labels) {
        Map<Object, String> label = new IdentityHashMap<>();
        labels.forEach((l, boon) -> label.put(boon, l));
        if (!f.objecten().isEmpty() || !f.koppelingen().isEmpty())
            throw new IllegalStateException("het model zonder (g) maakte " + f.objecten().size() + " objecten en "
                    + f.koppelingen().size() + " koppelingen");
        List<String> uit = new ArrayList<>();
        for (Fixpunt.AfgeleideWaarde w : f.waarden())
            uit.add(naam(label, w.object()) + "." + w.attribuut() + " = "
                    + (w.waarde() instanceof BigDecimal d ? d.toPlainString() : String.valueOf(w.waarde())));
        for (Fixpunt.AfgeleidKenmerk k : f.kenmerken()) uit.add(naam(label, k.object()) + " is " + k.kenmerk());
        for (Fout fout : f.fouten()) uit.add("fout: " + naam(label, fout.onderwerp()) + ": " + fout.melding());
        for (Object i : f.inconsistenties())
            uit.add("inconsistent: " + RegelSpraakRuntime.get(i, "regel") + " @ "
                    + naam(label, RegelSpraakRuntime.get(i, "subject")));
        uit.sort(null);
        return uit;
    }

    private static String naam(Map<Object, String> label, Object o) {
        String l = label.get(o);
        if (l == null) throw new IllegalStateException("een object zonder caselabel in de lezing: " + o);
        return l;
    }

    /** The lines that one of two sorted readings has and the other has not, each marked with the side it is on. */
    private static List<String> verschil(List<String> a, List<String> b) {
        List<String> uit = new ArrayList<>();
        for (String s : a) if (java.util.Collections.binarySearch(b, s) < 0) uit.add("- " + s);
        for (String s : b) if (java.util.Collections.binarySearch(a, s) < 0) uit.add("+ " + s);
        return uit;
    }

    // ---------------- every point ----------------

    /** A point as its child JVM reported it: the fields of its {@code PUNT} or {@code TWEEDE} line, or the refusal or
     *  fault, and the load of the machine when it started. */
    private record Uitslag(DroolsBackend.Branch tak, int n, String belasting, String[] velden, String anders) {
        String veld(int i) {
            return velden[i];
        }

        double ms(int i) {
            return Double.parseDouble(velden[i]);
        }
    }

    /** Runs one point in a child JVM with {@code -Xmx16g}, after reading the load of the machine. */
    private static Uitslag kind(DroolsBackend.Branch tak, int n, String soort, String... argumenten) throws Exception {
        String java = Path.of(System.getProperty("java.home"), "bin", "java").toString();
        String cp = System.getProperty("java.class.path");
        String belasting = belasting();
        List<String> opdracht = new ArrayList<>(List.of(java, "-Xmx16g", "-cp", cp, Schaalproef.class.getName()));
        opdracht.addAll(List.of(argumenten));
        Process p = new ProcessBuilder(opdracht).redirectError(ProcessBuilder.Redirect.INHERIT).start();
        String uit = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        int code = p.waitFor();
        String regel = uit.lines().filter(l -> l.startsWith(soort + "\t") || l.startsWith("WEIGERT\t")
                || l.startsWith("GEENFIXPUNT\t")).reduce((a, z) -> z).orElse(null);
        System.err.println("schaalproef: " + String.join(" ", argumenten) + " klaar");
        if (code == 0 && regel != null && regel.startsWith(soort + "\t"))
            return new Uitslag(tak, n, belasting, regel.split("\t"), null);
        return new Uitslag(tak, n, belasting, null, regel != null ? regel.replace('\t', ' ')
                : "kind-JVM eindigde met code " + code + " zonder regel");
    }

    /** The load averages of the machine over 1, 5 and 15 minutes, as {@code uptime} gives them. */
    private static String belasting() throws Exception {
        Process p = new ProcessBuilder("uptime").redirectErrorStream(true).start();
        String uit = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
        if (p.waitFor() != 0) throw new IllegalStateException("uptime faalde: " + uit);
        int i = uit.indexOf("load average");
        if (i < 0) throw new IllegalStateException("uptime noemt geen belasting: " + uit);
        return uit.substring(uit.indexOf(':', i) + 1).trim().replace(",", "");
    }

    /** Measures every point of {@code takken}, each in a child JVM of its own, and prints the table. Returns 1 where a
     *  point was refused or failed, which the table shows. */
    private static int alles(List<DroolsBackend.Branch> takken) throws Exception {
        Map<DroolsBackend.Branch, Map<Integer, Uitslag>> uitslagen = new LinkedHashMap<>();
        boolean volledig = true;
        for (DroolsBackend.Branch tak : takken)
            for (int n : NS) {
                Uitslag u = kind(tak, n, "PUNT", "punt", tak.name(), String.valueOf(n));
                if (u.anders() != null) volledig = false;
                uitslagen.computeIfAbsent(tak, t -> new LinkedHashMap<>()).put(n, u);
            }
        System.out.print(tabel(uitslagen));
        return volledig ? 0 : 1;
    }

    private static String tabel(Map<DroolsBackend.Branch, Map<Integer, Uitslag>> uitslagen) {
        StringBuilder uit = new StringBuilder("# Schaalproef (kaart tms-omschakeling, punt 9)\n\n");
        jvm(uit);
        uit.append("- Per punt: bouw 1 opwarmer + mediaan van ").append(HERHALINGEN).append("; vuren ").append(OPWARMEN)
                .append(" opwarmer + mediaan van ").append(HERHALINGEN).append(", elk in een verse sessie; vuren via de")
                .append(" publieke `DroolsBackend.vuur`, onder het plafond dat een afnemer krijgt. Belasting: de load")
                .append(" average over 1, 5 en 15 minuten (`uptime`) bij de start van het punt.\n\n");
        uit.append("| tak | N | belasting | vuringen | vuren (ms) | terugzetten (ms) | vuren+terugzetten (ms) | piekfeiten")
                .append(" | heap (MB) | bouw (ms) | R | N0 | vuringen/(R×N0) | plafond | vuringen/plafond | kusttotaal")
                .append(" | inconsistent |\n");
        uit.append("|---|---:|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|\n");
        for (Map<Integer, Uitslag> perN : uitslagen.values())
            for (Uitslag u : perN.values()) {
                uit.append("| ").append(u.tak()).append(" | ").append(u.n()).append(" | ").append(u.belasting())
                        .append(" | ");
                if (u.anders() != null) {
                    uit.append(u.anders().replace("|", "\\|")).append(" |").append(" |".repeat(13)).append('\n');
                    continue;
                }
                String vuringen = u.veld(3);
                double r = Double.parseDouble(u.veld(10)), n0 = Double.parseDouble(u.veld(11)),
                        plafond = Double.parseDouble(u.veld(14));
                uit.append(vuringen).append(" | ").append(u.veld(4)).append(" | ").append(u.veld(5)).append(" | ")
                        .append(u.veld(6)).append(" | ").append(u.veld(7)).append(" | ").append(u.veld(8)).append(" | ")
                        .append(u.veld(9)).append(" | ").append(u.veld(10)).append(" | ").append(u.veld(11)).append(" | ")
                        .append(String.format(Locale.ROOT, "%.3f", eerste(vuringen) / (r * n0))).append(" | ")
                        .append(u.veld(14)).append(" | ")
                        .append(String.format(Locale.ROOT, "%.3f", eerste(vuringen) / plafond)).append(" | ")
                        .append(u.veld(12)).append(" | ").append(u.veld(13)).append(" |\n");
            }
        uit.append('\n');
        for (Map.Entry<DroolsBackend.Branch, Map<Integer, Uitslag>> e : uitslagen.entrySet()) {
            Uitslag a = e.getValue().get(1_000), z = e.getValue().get(10_000);
            boolean beide = a != null && z != null && a.anders() == null && z.anders() == null;
            double helling = beide ? Math.log10(z.ms(6) / a.ms(6)) : Double.NaN;
            uit.append("- Helling ").append(e.getKey()).append(" (vuren+terugzetten, log-log, N = 1 000 → 10 000): ")
                    .append(beide ? String.format(Locale.ROOT, "%.2f", helling) : "–").append('\n');
            if (z != null && z.anders() == null) {
                double deel = eerste(z.veld(3)) / Double.parseDouble(z.veld(14));
                uit.append("- Vloer ").append(e.getKey()).append(" (vuringen onder de helft van het plafond bij N = 10 000,")
                        .append(" helling ten hoogste 1,2): vuringen/plafond ")
                        .append(String.format(Locale.ROOT, "%.3f", deel)).append(", helling ")
                        .append(beide ? String.format(Locale.ROOT, "%.2f", helling) : "–").append(": ")
                        .append(deel < 0.5 && beide && helling <= 1.2 ? "gehaald" : "NIET gehaald").append('\n');
            }
        }
        return uit.toString();
    }

    /** Measures every point of the second fire, each in a child JVM of its own, and prints the table. Returns 1 where a
     *  point failed, which the table shows. */
    private static int tweede() throws Exception {
        StringBuilder uit = new StringBuilder("# Schaalproef, de tweede fire (kaart tms-omschakeling, punt 9)\n\n");
        jvm(uit);
        uit.append("- Model zonder (g), de creatieketen (`schaal/zonder-creatie/`); tak ").append(TAK).append(". Per punt ")
                .append(OPWARMEN).append(" opwarmronde + mediaan van ").append(HERHALINGEN).append(" rondes. Een ronde:")
                .append(" `voegIn`, `vuur`, `lees`; het gewicht van ").append(GEWIJZIGDE_VONDST).append(" op ")
                .append(GEWIJZIGD_GEWICHT.toPlainString()).append(" met `update`; `vuur`, `lees`; daarnaast een verse")
                .append(" sessie met de gewijzigde case: `voegIn`, `vuur`, `lees`. De lezing na de tweede fire is gelijk aan")
                .append(" die van de verse sessie, anders faalt het punt. Belasting: de load average over 1, 5 en 15")
                .append(" minuten (`uptime`) bij de start van het punt.\n");
        uit.append("- Tweede fire: de wijziging (`set` en `update`) en `vuur` samen. Verse sessie, opzetten: sessie, bonen")
                .append(" uit de case en `voegIn` (`CaseRunner.metSessie`); vuren: `vuur`.\n\n");
        uit.append("| N | belasting | R | N0 | vuringen eerste fire | vuringen tweede fire | vuringen verse sessie")
                .append(" | plafond tweede fire | eerste fire (ms) | tweede fire (ms) | verse sessie: opzetten (ms)")
                .append(" | verse sessie: vuren (ms) | tweede/vers (vuren) | regels in de lezing | gewijzigd tegen de")
                .append(" eerste lezing |\n");
        uit.append("|---:|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|\n");
        boolean volledig = true;
        for (int n : NS) {
            Uitslag u = kind(TAK, n, "TWEEDE", "tweedepunt", String.valueOf(n));
            uit.append("| ").append(n).append(" | ").append(u.belasting()).append(" | ");
            if (u.anders() != null) {
                volledig = false;
                uit.append(u.anders().replace("|", "\\|")).append(" |").append(" |".repeat(12)).append('\n');
                continue;
            }
            uit.append(u.veld(2)).append(" | ").append(u.veld(3)).append(" | ").append(u.veld(4)).append(" | ")
                    .append(u.veld(5)).append(" | ").append(u.veld(6)).append(" | ").append(u.veld(7)).append(" | ")
                    .append(u.veld(8)).append(" | ").append(u.veld(9)).append(" | ").append(u.veld(10)).append(" | ")
                    .append(u.veld(11)).append(" | ")
                    .append(String.format(Locale.ROOT, "%.3f", u.ms(9) / u.ms(11))).append(" | ")
                    .append(u.veld(12)).append(" | ").append(u.veld(13)).append(" |\n");
        }
        System.out.print(uit);
        return volledig ? 0 : 1;
    }

    private static void jvm(StringBuilder uit) {
        Runtime rt = Runtime.getRuntime();
        uit.append("- JVM: ").append(System.getProperty("java.vm.name")).append(' ')
                .append(System.getProperty("java.runtime.version")).append(", ").append(System.getProperty("os.arch"))
                .append(", ").append(rt.availableProcessors()).append(" processors; elk punt in een eigen JVM met -Xmx16g.\n");
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

    private static <T> long mediaanVan(List<T> rondes, java.util.function.ToLongFunction<T> veld) {
        return mediaan(rondes.stream().mapToLong(veld).toArray());
    }

    /** The value of a count over the repetitions, or its range where they differ. */
    private static <T> String bereik(List<T> metingen, java.util.function.ToIntFunction<T> veld) {
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
