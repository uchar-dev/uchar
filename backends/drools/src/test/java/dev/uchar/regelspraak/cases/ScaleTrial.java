package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import dev.uchar.regelspraak.loader.IrLoader;
import dev.uchar.regelspraak.runtime.RegelSpraakRuntime;
import dev.uchar.regelspraak.transpiler.tms.Fixpoint;
import dev.uchar.regelspraak.transpiler.tms.Fault;

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
 * evaluates the creations anew; {@link DroolsBackend#fire} therefore refuses a second fire on a model with a creation.
 *
 * <p><b>The case</b> ({@link #caseDocument}): one Kustwacht {@code K}; N Wachters {@code W1}…{@code WN} of rang 0, each a
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
 * and three measured ones, each in a fresh session that {@link CaseRunner#withSession} sets up as the replay does, through
 * {@link DroolsBackend#insert}. Of a fire it measures the firings and the cap of the public {@link DroolsBackend#fire},
 * the cap a consumer gets, the time of that fire and that of {@link DroolsBackend#setDerivedValues} apart, N0 (the
 * facts just before the fire), the peak of the facts during the fire (a listener that counts insertions and deletions
 * from N0 on and keeps the running maximum; at the end its count must equal the session's), and the heap in use after
 * {@code System.gc()} with the session open. The kusttotaal and the number of inconsistencies go with every point, so
 * that the points can be seen to give the same answer. The table gives the median per column, the load of the machine
 * at the start of the point ({@code uptime}), the firings against the cap, and per branch the slope of fire plus
 * write-back in log-log from N = 1 000 to N = 10 000, and per branch the floor of point 9 of the card: firings under half
 * the cap at N = 10 000 and a slope of at most 1.2. Run without arguments or with branch names, the harness ends with
 * exit code 1 where a point failed and 2 where every point was measured but a branch misses the floor, so a script that
 * runs it cannot take a missed floor for a pass. Until the switch the harness measured the destructive exec-model
 * branch beside the one with truth maintenance: under its fixed cap of 100 000 firings it did not reach its fixpoint at
 * N = 10 000, and its measurement without a cap is in the report of step 1 of the card.
 *
 * <p><b>A point of the second fire</b> is one N on the model without (g), in a JVM of its own with {@code -Xmx16g}, one
 * warm-up and three measured rounds. A round fills a session through the public entry, fires it ({@link
 * DroolsBackend#fire}) and reads it ({@link DroolsBackend#read}), makes the change on the bean of {@code V1_1} and tells
 * the session ({@code update}), fires again and reads again; beside it a fresh session with the changed case, filled,
 * fired and read. The two readings after the change must be equal, every value, characteristic, fault and
 * inconsistency named by its case label, and the reading must differ from the one before the change; else the point
 * fails loudly. It measures the first fire, the second (the change and the fire together), and the fresh session, its
 * set-up (session, beans from the case, {@code insert}) and its fire apart.
 *
 * <p>Run it from the root of the repository; without arguments it measures every point of the exec-model branch, each
 * in a child JVM, and prints Markdown on standard output:
 * <pre>
 *   mvn -o -q -f backends/drools/pom.xml test-compile
 *   CP=$(mktemp -d)/cp.txt
 *   mvn -o -q -f backends/drools/pom.xml dependency:build-classpath -Dmdep.outputFile="$CP"
 *   java -cp "backends/drools/target/test-classes:backends/drools/target/classes:$(cat "$CP")" \
 *       dev.uchar.regelspraak.cases.ScaleTrial
 * </pre>
 * With branch names as arguments it measures those branches; with {@code punt <branch> <N>} it measures one point in
 * its own JVM and prints its line; with {@code tweede} every point of the second fire, and with {@code tweedepunt <N>}
 * one; with {@code controle <N>} it replays a case of N Wachters with its expected values, and the changed case with
 * its own, through the exec-model branch on both models; with {@code case <N> <file> [gewijzigd]} it writes that
 * case, or the changed one, for the reference interpreter and clingo.
 */
public final class ScaleTrial {

    private ScaleTrial() {}

    private static final ObjectMapper JSON = new ObjectMapper();

    static final List<Integer> NS = List.of(100, 1_000, 10_000);

    /** The exec-model branch: the one the points measure by default, and the only one that reads a session between two
     *  fires ({@link DroolsBackend#read}). */
    static final DroolsBackend.Branch BRANCH = DroolsBackend.Branch.EXEC_MODEL;

    private static final int WARM_UP = 1, REPETITIONS = 3;

    static final int VONDSTEN_PER_WACHTER = 10;

    private static final BigDecimal VONDSTFACTOR = BigDecimal.valueOf(2), THRESHOLD = BigDecimal.valueOf(50),
            WACHTSOMGRENS = BigDecimal.valueOf(300), LOWER_BELONING = new BigDecimal("1.00");

    /** The change of the second fire: the Vondst and its new gewicht. */
    static final String CHANGED_VONDST = "V1_1";
    static final BigDecimal CHANGED_WEIGHT = new BigDecimal("40.00");

    public static void main(String[] args) throws Exception {
        List<String> tasks = List.of("punt", "tweede", "tweedepunt", "controle", "case");
        if (args.length == 0 || !tasks.contains(args[0])) {
            System.exit(measureAll(args.length == 0 ? List.of(BRANCH)
                    : Arrays.stream(args).map(DroolsBackend.Branch::valueOf).toList()));
            return;
        }
        switch (args[0]) {
            case "punt" -> System.exit(point(DroolsBackend.Branch.valueOf(args[1]), Integer.parseInt(args[2])));
            case "tweede" -> System.exit(second());
            case "tweedepunt" -> System.exit(secondPoint(Integer.parseInt(args[1])));
            case "controle" -> System.exit(check(Integer.parseInt(args[1])));
            case "case" -> Files.writeString(Path.of(args[2]), JSON.writerWithDefaultPrettyPrinter().writeValueAsString(
                    caseDocument(Integer.parseInt(args[1]), true, args.length > 3 && args[3].equals("gewijzigd"))));
            default -> throw new IllegalStateException(args[0]);
        }
    }

    // ---------------- the model and the case ----------------

    static Path model() throws Exception {
        return source("/scale/ir.json");
    }

    /** The model without (g), the creation chain: the model of the second fire. */
    static Path modelWithoutCreation() throws Exception {
        return source("/scale/without-creation/ir.json");
    }

    private static Path source(String path) throws Exception {
        java.net.URL url = ScaleTrial.class.getResource(path);
        if (url == null)
            throw new IllegalStateException(path.substring(1) + " niet op het klassepad: draai eerst test-compile");
        return Path.of(url.toURI());
    }

    static int weight(int wachter, int vondst) {
        return 5 + ((7 * wachter + 11 * vondst) % 36);
    }

    /** The case of {@code n} Wachters, with {@code changed} the one after the change of the second fire; with
     *  {@code expected} also every value the model derives on an input object, and the inconsistency of every Wachter
     *  whose wachtsom reaches the bound, as the case schema spells them. */
    static ObjectNode caseDocument(int n, boolean expected, boolean changed) {
        JsonNodeFactory f = JsonNodeFactory.instance;
        ObjectNode doc = f.objectNode();
        doc.put("format", "uchar-case-v0");
        doc.putNull("asOfDate");
        ObjectNode input = doc.putObject("input");
        ArrayNode instances = input.putArray("instances");
        ArrayNode expectations = f.arrayNode();
        instances.add(instance(f, "K", "Kustwacht", f.objectNode(), f.objectNode()));
        BigDecimal kusttotaal = new BigDecimal("0.00");
        boolean inconsistent = false;
        for (int i = 1; i <= n; i++) {
            String wachter = "W" + i;
            ObjectNode attributes = f.objectNode();
            attributes.putObject("rang").put("number", "0");
            ObjectNode roles = f.objectNode();
            roles.putArray("kustwacht").add("K");
            instances.add(instance(f, wachter, "Wachter", attributes, roles));
            BigDecimal wachtsom = new BigDecimal("0.00"), highest = null;
            for (int j = 1; j <= VONDSTEN_PER_WACHTER; j++) {
                String vondst = "V" + i + "_" + j;
                BigDecimal weight = changed && vondst.equals(CHANGED_VONDST) ? CHANGED_WEIGHT
                        : BigDecimal.valueOf(weight(i, j)).setScale(2);
                ObjectNode va = f.objectNode();
                va.putObject("gewicht").put("number", weight.toPlainString());
                ObjectNode vr = f.objectNode();
                vr.putArray("beheerder").add(wachter);
                instances.add(instance(f, vondst, "Vondst", va, vr));
                BigDecimal value = weight.multiply(VONDSTFACTOR);
                BigDecimal beloning = value.compareTo(THRESHOLD) > 0 ? value : LOWER_BELONING;
                wachtsom = wachtsom.add(beloning);
                highest = highest == null || beloning.compareTo(highest) > 0 ? beloning : highest;
                value(expectations, vondst, "waarde", value);
                value(expectations, vondst, "beloning", beloning);
            }
            value(expectations, wachter, "wachtsom", wachtsom);
            value(expectations, wachter, "hoogste beloning", highest);
            if (wachtsom.compareTo(WACHTSOMGRENS) >= 0) {
                inconsistent = true;
                expectations.addObject().put("kind", "inconsistent").put("rule", "wachtsomgrens").put("label", wachter);
            }
            kusttotaal = kusttotaal.add(wachtsom);
        }
        value(expectations, "K", "kusttotaal", kusttotaal);
        if (!inconsistent) expectations.addObject().put("kind", "consistent");
        input.putObject("parameters").putObject("vondstfactor").put("number", VONDSTFACTOR.toPlainString());
        doc.set("expected", expected ? expectations : f.arrayNode());
        return doc;
    }

    private static ObjectNode instance(JsonNodeFactory f, String label, String type, ObjectNode attributes,
                                        ObjectNode roles) {
        ObjectNode i = f.objectNode();
        i.put("label", label);
        i.put("objectType", type);
        i.set("attributes", attributes);
        i.set("roles", roles);
        i.putArray("characteristics");
        return i;
    }

    private static void value(ArrayNode expectations, String label, String attribute, BigDecimal value) {
        ObjectNode v = expectations.addObject();
        v.put("kind", "attributeValue").put("label", label).put("attribute", attribute);
        v.putObject("value").put("number", value.toPlainString());
    }

    /** Replays the case of {@code n} Wachters and the changed case, each with its expected values, through the
     *  exec-model branch on the model and on the model without (g), the replay's own comparison: the check that the
     *  harness measures models the branch answers as the cases say. */
    private static int check(int n) throws Exception {
        int fault = 0;
        for (Path ir : List.of(model(), modelWithoutCreation()))
            for (boolean changed : List.of(false, true)) {
                JsonNode c = caseDocument(n, true, changed);
                String verdict = CaseRunner.playCase(ir, c, BRANCH);
                System.out.println(ir.getParent().getFileName() + (changed ? ", gewijzigd" : "") + ", " + BRANCH
                        + ": " + (verdict == null ? "gelijk aan de verwachting" : verdict));
                if (verdict != null) fault = 1;
            }
        return fault;
    }

    // ---------------- one point ----------------

    /** One fire of the case: its firings and the cap it fired under, the fire and the write-back in nanoseconds, the
     *  facts just before the fire and their peak during it, the heap in use after a collection with the session open,
     *  and two outcomes that every point must give alike. */
    record FireMeasurement(int firings, int cap, long fireNanos, long writeBackNanos, long n0, long peak, long heap,
                      String kusttotaal, int inconsistent) {}

    /** Measures one point in this JVM and prints one line, {@code PUNT} with its fields tab-separated,
     *  {@code WEIGERT} with the reason where the branch refuses a form of the model, or {@code GEENFIXPUNT} with the
     *  sentence of the driver where a fire ends without its fixpoint. */
    private static int point(DroolsBackend.Branch branch, int n) throws Exception {
        Path ir = model();
        long[] build = new long[REPETITIONS];
        DroolsBackend.Build b = null;
        try {
            for (int i = -WARM_UP; i < REPETITIONS; i++) {
                long begin = System.nanoTime();
                b = DroolsBackend.build(ir, branch);
                if (i >= 0) build[i] = System.nanoTime() - begin;
            }
        } catch (CaseRunner.Excluded e) {
            System.out.println("WEIGERT\t" + branch + "\t" + n + "\t" + e.getMessage());
            return 2;
        } catch (IrLoader.Refusal e) {
            System.out.println("WEIGERT\t" + branch + "\t" + n + "\tloader: " + e.getMessage());
            return 2;
        }
        int rules = rules(b.kieBase());
        CaseRunner runner = CaseRunner.forMeasurement(ir, branch);
        JsonNode c = caseDocument(n, false, false);
        List<FireMeasurement> metingen = new ArrayList<>();
        try {
            for (int i = -WARM_UP; i < REPETITIONS; i++) {
                FireMeasurement m = runner.withSession(c, ScaleTrial::fire);
                if (i >= 0) metingen.add(m);
            }
        } catch (NoFixpoint e) {
            System.out.println("GEENFIXPUNT\t" + branch + "\t" + n + "\tna " + e.firings + " vuringen in "
                    + ms(e.nanos) + " ms: " + e.getMessage());
            return 3;
        }
        FireMeasurement first = metingen.get(0);
        for (FireMeasurement m : metingen)
            if (m.n0() != first.n0() || m.cap() != first.cap() || !m.kusttotaal().equals(first.kusttotaal())
                    || m.inconsistent() != first.inconsistent())
                throw new IllegalStateException("herhalingen verschillen in invoer, plafond of uitkomst: " + metingen);
        System.out.println(String.join("\t", "PUNT", branch.name(), String.valueOf(n),
                range(metingen, FireMeasurement::firings),
                ms(median(metingen, FireMeasurement::fireNanos)), ms(median(metingen, FireMeasurement::writeBackNanos)),
                ms(median(metingen, m -> m.fireNanos() + m.writeBackNanos())),
                range(metingen, m -> (int) m.peak()), String.valueOf(median(metingen, FireMeasurement::heap) >> 20),
                ms(median(build)), String.valueOf(rules), String.valueOf(first.n0()), first.kusttotaal(),
                String.valueOf(first.inconsistent()), String.valueOf(first.cap())));
        return 0;
    }

    /** Fires one set-up session through the public driver, under the cap a consumer gets, writes back, and measures.
     *  A run that reaches no fixpoint fails with the sentence of the driver, since its numbers would measure the cap. */
    private static FireMeasurement fire(DroolsBackend.Build build, KieSession session, Map<String, Object> labels) {
        FactCounter counter = new FactCounter(session.getFactCount());
        session.addEventListener(counter);
        long begin = System.nanoTime();
        DroolsBackend.Firing firing = DroolsBackend.fire(build, session);
        long fired = System.nanoTime();
        session.removeEventListener(counter);
        if (firing.noFixpoint() != null) throw new NoFixpoint(firing, fired - begin);
        // counted before the write-back, which states the mark that closes the session on the exec-model branch
        if (counter.state != session.getFactCount())
            throw new IllegalStateException("de feitenteller staat op " + counter.state + ", de sessie houdt "
                    + session.getFactCount() + " feiten");
        DroolsBackend.setDerivedValues(build, session);
        long writtenBack = System.nanoTime();
        long heap = used();
        Object kusttotaal = RegelSpraakRuntime.get(labels.get("K"), "kusttotaal");
        int inconsistent = session.getObjects(o -> o.getClass().getSimpleName().equals("Inconsistentie")).size();
        return new FireMeasurement(firing.firings(), firing.cap(), fired - begin, writtenBack - fired, counter.n0,
                counter.peak, heap, kusttotaal instanceof BigDecimal d ? d.toPlainString() : String.valueOf(kusttotaal),
                inconsistent);
    }

    /** A fire that ended without its fixpoint, at the cap or at the creation budget: its numbers would measure the
     *  cap, so the point reports the sentence of the driver instead, with the firings and the time it took. */
    private static final class NoFixpoint extends RuntimeException {
        final int firings;
        final long nanos;

        NoFixpoint(DroolsBackend.Firing firing, long nanos) {
            super(firing.noFixpoint());
            this.firings = firing.firings();
            this.nanos = nanos;
        }
    }

    /** Counts the facts of a session from {@code n0} on through every insertion and deletion and keeps the running
     *  maximum: the count at the end of a fire understates what the fire held, since truth maintenance inserts
     *  intermediate states and retracts them. A logical insert of a fact equal to one that stands adds a justification
     *  and inserts nothing, so it counts as nothing. */
    private static final class FactCounter implements RuleRuntimeEventListener {
        final long n0;
        long state, peak;

        FactCounter(long n0) {
            this.n0 = n0;
            this.state = n0;
            this.peak = n0;
        }

        @Override public void objectInserted(ObjectInsertedEvent e) {
            if (++state > peak) peak = state;
        }

        @Override public void objectUpdated(ObjectUpdatedEvent e) {}

        @Override public void objectDeleted(ObjectDeletedEvent e) {
            state--;
        }
    }

    private static long used() {
        Runtime rt = Runtime.getRuntime();
        System.gc();
        System.gc();
        return rt.totalMemory() - rt.freeMemory();
    }

    // ---------------- one point of the second fire ----------------

    /** The session fired twice: the firings of the first fire and of the second, the cap of the second, N0, the time
     *  in nanoseconds of the first fire and that of the change and the second fire together, and the readings before
     *  and after the change. */
    private record Repeated(int firstFirings, int secondFirings, int secondCap, long n0, long firstNanos,
                            long secondNanos, List<String> before, List<String> after) {}

    /** The fresh session with the changed case: its firings, its set-up and its fire in nanoseconds, and its reading. */
    private record Fresh(int firings, long setupNanos, long fireNanos, List<String> read) {}

    /** One round of the second fire. */
    private record Second(Repeated repeated, Fresh fresh) {}

    /** Measures one point of the second fire in this JVM and prints one line, {@code TWEEDE} with its fields
     *  tab-separated. */
    private static int secondPoint(int n) throws Exception {
        Path ir = modelWithoutCreation();
        int rules = rules(DroolsBackend.build(ir, BRANCH).kieBase());
        CaseRunner runner = CaseRunner.forMeasurement(ir, BRANCH);
        JsonNode c = caseDocument(n, false, false), changed = caseDocument(n, false, true);
        List<Second> rounds = new ArrayList<>();
        for (int i = -WARM_UP; i < REPETITIONS; i++) {
            Second t = ronde(runner, c, changed);
            if (i >= 0) rounds.add(t);
        }
        Repeated first = rounds.get(0).repeated();
        for (Second t : rounds)
            if (!t.repeated().after().equals(first.after()) || t.repeated().n0() != first.n0()
                    || t.repeated().secondCap() != first.secondCap())
                throw new IllegalStateException("de rondes verschillen in invoer, plafond of lezing");
        int difference = difference(first.before(), first.after()).size();
        System.out.println(String.join("\t", "TWEEDE", String.valueOf(n), String.valueOf(rules),
                String.valueOf(first.n0()), range(rounds, t -> t.repeated().firstFirings()),
                range(rounds, t -> t.repeated().secondFirings()), range(rounds, t -> t.fresh().firings()),
                String.valueOf(first.secondCap()),
                ms(medianOf(rounds, t -> t.repeated().firstNanos())),
                ms(medianOf(rounds, t -> t.repeated().secondNanos())),
                ms(medianOf(rounds, t -> t.fresh().setupNanos())), ms(medianOf(rounds, t -> t.fresh().fireNanos())),
                String.valueOf(first.after().size()), String.valueOf(difference)));
        return 0;
    }

    /** One round: a session filled with case {@code c}, fired, read, changed, fired and read again; then a fresh
     *  session with case {@code changed}, filled, fired and read. Fails where the reading after the change differs
     *  from the fresh one, or equals the one before it. */
    private static Second ronde(CaseRunner runner, JsonNode c, JsonNode changed) {
        Repeated a = runner.withSession(c, (build, session, labels) -> {
            long n0 = session.getFactCount();
            long begin = System.nanoTime();
            DroolsBackend.Firing first = DroolsBackend.fire(build, session);
            long fired = System.nanoTime();
            if (first.noFixpoint() != null) throw new IllegalStateException("eerste fire: " + first.noFixpoint());
            List<String> before = describe(DroolsBackend.fixpointOf(build, session), labels);
            Object vondst = labels.get(CHANGED_VONDST);
            long change = System.nanoTime();
            RegelSpraakRuntime.set(vondst, "gewicht", CHANGED_WEIGHT);
            session.update(session.getFactHandle(vondst), vondst);
            DroolsBackend.Firing second = DroolsBackend.fire(build, session);
            long refiredAt = System.nanoTime();
            if (second.noFixpoint() != null) throw new IllegalStateException("tweede fire: " + second.noFixpoint());
            List<String> after = describe(DroolsBackend.fixpointOf(build, session), labels);
            return new Repeated(first.firings(), second.firings(), second.cap(), n0, fired - begin,
                    refiredAt - change, before, after);
        });
        long[] start = {System.nanoTime()};
        Fresh v = runner.withSession(changed, (build, session, labels) -> {
            long setUp = System.nanoTime();
            DroolsBackend.Firing firing = DroolsBackend.fire(build, session);
            long fired = System.nanoTime();
            if (firing.noFixpoint() != null) throw new IllegalStateException("verse sessie: " + firing.noFixpoint());
            List<String> read = describe(DroolsBackend.fixpointOf(build, session), labels);
            return new Fresh(firing.firings(), setUp - start[0], fired - setUp, read);
        });
        if (!a.after().equals(v.read())) {
            List<String> differences = difference(a.after(), v.read());
            throw new IllegalStateException("de lezing na de tweede fire (" + a.after().size() + " regels) verschilt van die"
                    + " van een verse sessie met de gewijzigde invoer (" + v.read().size() + " regels), in "
                    + differences.size() + " regels" + (differences.isEmpty() ? ", alleen in hoe vaak een regel voorkomt" : "")
                    + "; de eerste: " + differences.subList(0, Math.min(10, differences.size())));
        }
        if (a.before().equals(a.after()))
            throw new IllegalStateException("de wijziging van " + CHANGED_VONDST + " veranderde de lezing niet");
        return new Second(a, v);
    }

    /** {@code f} as sorted lines, every object named by its case label: a derived value as its bean field takes it, a
     *  derived characteristic, a fault and an inconsistency. The model without (g) makes no object and lays no link,
     *  so a reading with one fails. */
    private static List<String> describe(Fixpoint f, Map<String, Object> labels) {
        Map<Object, String> label = new IdentityHashMap<>();
        labels.forEach((l, beanObject) -> label.put(beanObject, l));
        if (!f.objects().isEmpty() || !f.links().isEmpty())
            throw new IllegalStateException("het model zonder (g) maakte " + f.objects().size() + " objecten en "
                    + f.links().size() + " koppelingen");
        List<String> out = new ArrayList<>();
        for (Fixpoint.DerivedValue w : f.waarden())
            out.add(naam(label, w.object()) + "." + w.attribute() + " = "
                    + (w.value() instanceof BigDecimal d ? d.toPlainString() : String.valueOf(w.value())));
        for (Fixpoint.DerivedCharacteristic k : f.kenmerken()) out.add(naam(label, k.object()) + " is " + k.characteristic());
        for (Fault fault : f.faults()) out.add("fout: " + naam(label, fault.onderwerp()) + ": " + fault.message());
        for (Object i : f.inconsistencies())
            out.add("inconsistent: " + RegelSpraakRuntime.get(i, "regel") + " @ "
                    + naam(label, RegelSpraakRuntime.get(i, "subject")));
        out.sort(null);
        return out;
    }

    private static String naam(Map<Object, String> label, Object o) {
        String l = label.get(o);
        if (l == null) throw new IllegalStateException("een object zonder caselabel in de lezing: " + o);
        return l;
    }

    /** The lines that one of two sorted readings has and the other has not, each marked with the side it is on. */
    private static List<String> difference(List<String> a, List<String> b) {
        List<String> out = new ArrayList<>();
        for (String s : a) if (java.util.Collections.binarySearch(b, s) < 0) out.add("- " + s);
        for (String s : b) if (java.util.Collections.binarySearch(a, s) < 0) out.add("+ " + s);
        return out;
    }

    // ---------------- every point ----------------

    /** A point as its child JVM reported it: the fields of its {@code PUNT} or {@code TWEEDE} line, or the refusal or
     *  fault, and the load of the machine when it started. */
    private record Result(DroolsBackend.Branch branch, int n, String belasting, String[] fields, String failure) {
        String field(int i) {
            return fields[i];
        }

        double ms(int i) {
            return Double.parseDouble(fields[i]);
        }
    }

    /** Runs one point in a child JVM with {@code -Xmx16g}, after reading the load of the machine. */
    private static Result kind(DroolsBackend.Branch branch, int n, String category, String... arguments) throws Exception {
        String java = Path.of(System.getProperty("java.home"), "bin", "java").toString();
        String cp = System.getProperty("java.class.path");
        String belasting = belasting();
        List<String> task = new ArrayList<>(List.of(java, "-Xmx16g", "-cp", cp, ScaleTrial.class.getName()));
        task.addAll(List.of(arguments));
        Process p = new ProcessBuilder(task).redirectError(ProcessBuilder.Redirect.INHERIT).start();
        String out = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        int code = p.waitFor();
        String regel = out.lines().filter(l -> l.startsWith(category + "\t") || l.startsWith("WEIGERT\t")
                || l.startsWith("GEENFIXPUNT\t")).reduce((a, z) -> z).orElse(null);
        System.err.println("schaalproef: " + String.join(" ", arguments) + " klaar");
        if (code == 0 && regel != null && regel.startsWith(category + "\t"))
            return new Result(branch, n, belasting, regel.split("\t"), null);
        return new Result(branch, n, belasting, null, regel != null ? regel.replace('\t', ' ')
                : "kind-JVM eindigde met code " + code + " zonder regel");
    }

    /** The load averages of the machine over 1, 5 and 15 minutes, as {@code uptime} gives them. */
    private static String belasting() throws Exception {
        Process p = new ProcessBuilder("uptime").redirectErrorStream(true).start();
        String out = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
        if (p.waitFor() != 0) throw new IllegalStateException("uptime faalde: " + out);
        int i = out.indexOf("load average");
        if (i < 0) throw new IllegalStateException("uptime noemt geen belasting: " + out);
        return out.substring(out.indexOf(':', i) + 1).trim().replace(",", "");
    }

    /** Measures every point of {@code branches}, each in a child JVM of its own, and prints the table. Returns 1 where a
     *  point was refused or failed, which the table shows, and 2 where a branch misses the floor ({@link Floor}). */
    private static int measureAll(List<DroolsBackend.Branch> branches) throws Exception {
        Map<DroolsBackend.Branch, Map<Integer, Result>> results = new LinkedHashMap<>();
        boolean complete = true;
        for (DroolsBackend.Branch branch : branches)
            for (int n : NS) {
                Result u = kind(branch, n, "PUNT", "punt", branch.name(), String.valueOf(n));
                if (u.failure() != null) complete = false;
                results.computeIfAbsent(branch, t -> new LinkedHashMap<>()).put(n, u);
            }
        System.out.print(table(results));
        if (!complete) return 1;
        return results.values().stream().allMatch(perN -> Floor.van(perN).reached()) ? 0 : 2;
    }

    /** The floor of point 9 of the card on one branch: the firings against the cap at N = 10 000, under half, and the
     *  slope of fire plus write-back from N = 1 000 to N = 10 000 in log-log, at most 1.2; NaN where a point is missing
     *  or failed, which misses the floor. */
    private record Floor(double capShare, double helling) {
        static Floor van(Map<Integer, Result> perN) {
            Result a = perN.get(1_000), z = perN.get(10_000);
            boolean zGemeten = z != null && z.failure() == null;
            boolean both = zGemeten && a != null && a.failure() == null;
            return new Floor(zGemeten ? first(z.field(3)) / Double.parseDouble(z.field(14)) : Double.NaN,
                    both ? Math.log10(z.ms(6) / a.ms(6)) : Double.NaN);
        }

        boolean reached() {
            return capShare < 0.5 && helling <= 1.2;
        }
    }

    private static String table(Map<DroolsBackend.Branch, Map<Integer, Result>> results) {
        StringBuilder out = new StringBuilder("# Schaalproef (kaart tms-omschakeling, punt 9)\n\n");
        jvm(out);
        out.append("- Per punt: bouw 1 opwarmer + mediaan van ").append(REPETITIONS).append("; vuren ").append(WARM_UP)
                .append(" opwarmer + mediaan van ").append(REPETITIONS).append(", elk in een verse sessie; vuren via de")
                .append(" publieke `DroolsBackend.vuur`, onder het plafond dat een afnemer krijgt. Belasting: de load")
                .append(" average over 1, 5 en 15 minuten (`uptime`) bij de start van het punt.\n\n");
        out.append("| tak | N | belasting | vuringen | vuren (ms) | terugzetten (ms) | vuren+terugzetten (ms) | piekfeiten")
                .append(" | heap (MB) | bouw (ms) | R | N0 | vuringen/(R×N0) | plafond | vuringen/plafond | kusttotaal")
                .append(" | inconsistent |\n");
        out.append("|---|---:|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|\n");
        for (Map<Integer, Result> perN : results.values())
            for (Result u : perN.values()) {
                out.append("| ").append(u.branch()).append(" | ").append(u.n()).append(" | ").append(u.belasting())
                        .append(" | ");
                if (u.failure() != null) {
                    out.append(u.failure().replace("|", "\\|")).append(" |").append(" |".repeat(13)).append('\n');
                    continue;
                }
                String firings = u.field(3);
                double r = Double.parseDouble(u.field(10)), n0 = Double.parseDouble(u.field(11)),
                        cap = Double.parseDouble(u.field(14));
                out.append(firings).append(" | ").append(u.field(4)).append(" | ").append(u.field(5)).append(" | ")
                        .append(u.field(6)).append(" | ").append(u.field(7)).append(" | ").append(u.field(8)).append(" | ")
                        .append(u.field(9)).append(" | ").append(u.field(10)).append(" | ").append(u.field(11)).append(" | ")
                        .append(String.format(Locale.ROOT, "%.3f", first(firings) / (r * n0))).append(" | ")
                        .append(u.field(14)).append(" | ")
                        .append(String.format(Locale.ROOT, "%.3f", first(firings) / cap)).append(" | ")
                        .append(u.field(12)).append(" | ").append(u.field(13)).append(" |\n");
            }
        out.append('\n');
        for (Map.Entry<DroolsBackend.Branch, Map<Integer, Result>> e : results.entrySet()) {
            Floor v = Floor.van(e.getValue());
            boolean both = !Double.isNaN(v.helling());
            out.append("- Helling ").append(e.getKey()).append(" (vuren+terugzetten, log-log, N = 1 000 → 10 000): ")
                    .append(both ? String.format(Locale.ROOT, "%.2f", v.helling()) : "–").append('\n');
            if (!Double.isNaN(v.capShare()))
                out.append("- Vloer ").append(e.getKey()).append(" (vuringen onder de helft van het plafond bij N = 10 000,")
                        .append(" helling ten hoogste 1,2): vuringen/plafond ")
                        .append(String.format(Locale.ROOT, "%.3f", v.capShare())).append(", helling ")
                        .append(both ? String.format(Locale.ROOT, "%.2f", v.helling()) : "–").append(": ")
                        .append(v.reached() ? "gehaald" : "NIET gehaald").append('\n');
        }
        return out.toString();
    }

    /** Measures every point of the second fire, each in a child JVM of its own, and prints the table. Returns 1 where a
     *  point failed, which the table shows. */
    private static int second() throws Exception {
        StringBuilder out = new StringBuilder("# Schaalproef, de tweede fire (kaart tms-omschakeling, punt 9)\n\n");
        jvm(out);
        out.append("- Model zonder (g), de creatieketen (`schaal/zonder-creatie/`); tak ").append(BRANCH).append(". Per punt ")
                .append(WARM_UP).append(" opwarmronde + mediaan van ").append(REPETITIONS).append(" rondes. Een ronde:")
                .append(" `voegIn`, `vuur`, `lees`; het gewicht van ").append(CHANGED_VONDST).append(" op ")
                .append(CHANGED_WEIGHT.toPlainString()).append(" met `update`; `vuur`, `lees`; daarnaast een verse")
                .append(" sessie met de gewijzigde case: `voegIn`, `vuur`, `lees`. De lezing na de tweede fire is gelijk aan")
                .append(" die van de verse sessie, anders faalt het punt. Belasting: de load average over 1, 5 en 15")
                .append(" minuten (`uptime`) bij de start van het punt.\n");
        out.append("- Tweede fire: de wijziging (`set` en `update`) en `vuur` samen. Verse sessie, opzetten: sessie, beans")
                .append(" uit de case en `voegIn` (`CaseRunner.metSessie`); vuren: `vuur`.\n\n");
        out.append("| N | belasting | R | N0 | vuringen eerste fire | vuringen tweede fire | vuringen verse sessie")
                .append(" | plafond tweede fire | eerste fire (ms) | tweede fire (ms) | verse sessie: opzetten (ms)")
                .append(" | verse sessie: vuren (ms) | tweede/vers (vuren) | regels in de lezing | gewijzigd tegen de")
                .append(" eerste lezing |\n");
        out.append("|---:|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|\n");
        boolean complete = true;
        for (int n : NS) {
            Result u = kind(BRANCH, n, "TWEEDE", "tweedepunt", String.valueOf(n));
            out.append("| ").append(n).append(" | ").append(u.belasting()).append(" | ");
            if (u.failure() != null) {
                complete = false;
                out.append(u.failure().replace("|", "\\|")).append(" |").append(" |".repeat(12)).append('\n');
                continue;
            }
            out.append(u.field(2)).append(" | ").append(u.field(3)).append(" | ").append(u.field(4)).append(" | ")
                    .append(u.field(5)).append(" | ").append(u.field(6)).append(" | ").append(u.field(7)).append(" | ")
                    .append(u.field(8)).append(" | ").append(u.field(9)).append(" | ").append(u.field(10)).append(" | ")
                    .append(u.field(11)).append(" | ")
                    .append(String.format(Locale.ROOT, "%.3f", u.ms(9) / u.ms(11))).append(" | ")
                    .append(u.field(12)).append(" | ").append(u.field(13)).append(" |\n");
        }
        System.out.print(out);
        return complete ? 0 : 1;
    }

    private static void jvm(StringBuilder out) {
        Runtime rt = Runtime.getRuntime();
        out.append("- JVM: ").append(System.getProperty("java.vm.name")).append(' ')
                .append(System.getProperty("java.runtime.version")).append(", ").append(System.getProperty("os.arch"))
                .append(", ").append(rt.availableProcessors()).append(" processors; elk punt in een eigen JVM met -Xmx16g.\n");
    }

    // ---------------- helpers ----------------

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

    private static long median(List<FireMeasurement> metingen, java.util.function.ToLongFunction<FireMeasurement> field) {
        return median(metingen.stream().mapToLong(field).toArray());
    }

    private static <T> long medianOf(List<T> rounds, java.util.function.ToLongFunction<T> field) {
        return median(rounds.stream().mapToLong(field).toArray());
    }

    /** The value of a count over the repetitions, or its range where they differ. */
    private static <T> String range(List<T> metingen, java.util.function.ToIntFunction<T> field) {
        int min = metingen.stream().mapToInt(field).min().orElseThrow();
        int max = metingen.stream().mapToInt(field).max().orElseThrow();
        return min == max ? String.valueOf(min) : min + "–" + max;
    }

    private static double first(String range) {
        int streep = range.indexOf('–');
        return Double.parseDouble(streep < 0 ? range : range.substring(0, streep));
    }

    private static String ms(long nanos) {
        return String.format(Locale.ROOT, "%.1f", nanos / 1e6);
    }
}
