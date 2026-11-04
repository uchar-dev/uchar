package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;

import dev.uchar.regelspraak.graph.ProducerConsumer;
import dev.uchar.regelspraak.loader.IrLoader;
import dev.uchar.regelspraak.model.RegelSpraakModel;
import dev.uchar.regelspraak.model.RegelSpraakModel.ObjectType;
import dev.uchar.regelspraak.runtime.Accessors;
import dev.uchar.regelspraak.runtime.Breuk;
import dev.uchar.regelspraak.schema.FactModelGenerator;
import dev.uchar.regelspraak.transpiler.core.Namen;
import dev.uchar.regelspraak.transpiler.tms.BucketInput;
import dev.uchar.regelspraak.transpiler.tms.ComputeHelpers;
import dev.uchar.regelspraak.transpiler.tms.ConditionReading;
import dev.uchar.regelspraak.transpiler.tms.DefaultsDecision;
import dev.uchar.regelspraak.transpiler.tms.Fixpoint;
import dev.uchar.regelspraak.transpiler.tms.Period;
import dev.uchar.regelspraak.transpiler.tms.PeriodDecisions;
import dev.uchar.regelspraak.transpiler.tms.StackComposition;
import dev.uchar.regelspraak.transpiler.tms.TmsTranspiler;

import org.junit.jupiter.api.Test;
import org.kie.api.runtime.ClassObjectFilter;
import org.kie.api.runtime.KieSession;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@code DroolsBackend.periodDecisionsOf}, who decided an attribute on a timeline per period (card
 * decisions-per-period; RUNTIME-SEMANTICS ch. 3.11, 7.11).
 *
 * <p>The worked example, on both branches: the golden {@code jutterswet-opgave-per-periode}, case 4, J4, whose
 * strandloon the opgave decides in January and February, the computation in spring, and whose input 99 stands on the
 * rest of 2025; and the anchor {@code jutterswet-borgdeel}, case 6, J1, whose borgdeel the rule decides in January and
 * February 2024 and whose input 99 stands in March.
 *
 * <p>The witness, on both branches of the build with readings: every case of every golden the exec-model branch plays,
 * every case of the red corpus, the documents and cases of {@code ref/test/fixtures/defaults-per-day} and
 * {@code -cases} and those of {@code ref/test/fixtures/writers-per-day}, and the document of
 * {@code ref/test/fixtures/conditions-per-period/twee-schrijvers-gelijk}, whose two rules write one value on the days
 * of November; per case every record. Where no fault stands:
 *
 * <ul>
 *   <li>the records are one per object whose type has the member and per attribute on a timeline that a stack or
 *       rules write, and a stack's root chains are those of its composition ({@code TmsTranspiler.stacks});</li>
 *   <li>the spans are those of the reading per day of the reference interpreter: per day, the decision of ch. 7.1, 7.2,
 *       7.3 and 7.9 with every justification and consequence read on that day, as {@code run_defaults_timeline} and
 *       {@code decide_days} take it, here from the readings of the defaults (ch. 5.12, which gate 17 holds to the
 *       reference interpreter's trace): a default decides a day of its turn on which the root of its consequence, the
 *       filled segments within its justification's periods, reads a value, the initialisation default only where the
 *       input leaves the target empty; unordered members and exception groups reconcile per day. For rules, per day the
 *       rules whose readings write a value there (ch. 3.11). On a day no writer decides, the input; maximal periods;</li>
 *   <li>the union of the values of the spans with the input is the timeline of the fixpoint: per day the value of the
 *       decider, of the first rule or of the input, and leeg where a span has none, is what {@code fixpointOf} gives
 *       the member, or the input where it derives none.</li>
 * </ul>
 *
 * A red case has no answer, and its records still have to be read where it reaches a fixpoint: both branches give
 * the same, on every case.
 *
 * <p>Floors, measured 2026-11-04: {@value #EXPECTED_MIN_RECORDS} records per branch, {@value #EXPECTED_MIN_STACK_RECORDS}
 * of them of a stack per day; spans with a decider {@value #EXPECTED_MIN_DECIDER_SPANS}, with a conflict
 * {@value #EXPECTED_MIN_CONFLICT_SPANS}, with rules {@value #EXPECTED_MIN_RULE_SPANS}, of them
 * {@value #EXPECTED_MIN_SHARED_RULE_SPANS} with two or more, and with the input
 * {@value #EXPECTED_MIN_INPUT_SPANS}; {@value #EXPECTED_MIN_FAULTED_CASES} red cases read where a fault stands. A smaller
 * count is a corpus or a fixture zone that lost a document or a case, or a selection that silently stopped checking.
 */
class PeriodDecisionsWitnessTest {

    private static final Path CORPUS = Path.of("..", "..", "corpus");
    private static final Path FIXTURES = Path.of("..", "..", "ref", "test", "fixtures");
    private static final Path GOLDEN_OPGAVE = CORPUS.resolve("golden").resolve("jutterswet-opgave-per-periode");
    private static final Path GOLDEN_BORGDEEL = CORPUS.resolve("golden").resolve("anchor").resolve("jutterswet-borgdeel");

    /** The records checked against the readings per branch, measured 2026-11-04. */
    static final int EXPECTED_MIN_RECORDS = 272;
    /** Of them those a stack per day decides, measured 2026-11-04. */
    static final int EXPECTED_MIN_STACK_RECORDS = 28;
    /** The spans with a decider, with a conflict, with rules, and with the input standing, measured 2026-11-04. */
    static final int EXPECTED_MIN_DECIDER_SPANS = 57;
    static final int EXPECTED_MIN_CONFLICT_SPANS = 2;
    static final int EXPECTED_MIN_RULE_SPANS = 454;
    /** Of the spans with rules those with two or more, measured 2026-11-04. */
    static final int EXPECTED_MIN_SHARED_RULE_SPANS = 1;
    static final int EXPECTED_MIN_INPUT_SPANS = 17;
    /** The red cases read at a fixpoint where a fault stands, per branch, measured 2026-11-04. */
    static final int EXPECTED_MIN_FAULTED_CASES = 43;

    // ---------------- the worked example ----------------

    @Test void the_worked_example() throws Exception {
        for (DroolsBackend.Branch branch : TmsDocuments.BRANCHES) {
            assertEquals(List.of(
                            "J4 strandloon [[/rules/prioritizedDefaults/0/defaults/0, /rules/prioritizedDefaults/0/defaults/1]]",
                            "[- 2025-01-01) leeg",
                            "[2025-01-01 2025-03-01) default /rules/prioritizedDefaults/0/defaults/0 10",
                            "[2025-03-01 2025-06-01) default /rules/prioritizedDefaults/0/defaults/1 20",
                            "[2025-06-01 2026-01-01) invoer 99",
                            "[2026-01-01 -) leeg"),
                    record(GOLDEN_OPGAVE.resolve("ir.json"), GOLDEN_OPGAVE.resolve("case4-invoer-op-het-doel.json"), branch,
                            "J4", "strandloon"), branch.toString());
            assertEquals(List.of(
                            "J1 borgdeel []",
                            "[- 2024-01-01) leeg",
                            "[2024-01-01 2024-03-01) regel /rules/rules/0/versions/0 12",
                            "[2024-03-01 2024-04-01) invoer 99",
                            "[2024-04-01 -) leeg"),
                    record(GOLDEN_BORGDEEL.resolve("ir.json"), GOLDEN_BORGDEEL.resolve("case6-berekend-over-input.json"),
                            branch, "J1", "borgdeel"), branch.toString());
        }
        JsonNode borgdeel = TmsDocuments.JSON.readTree(GOLDEN_BORGDEEL.resolve("ir.json").toFile());
        assertEquals("borgdeel van een jutter", borgdeel.at("/rules/rules/0/name").asText());
    }

    /** The record of {@code member} of {@code label} in case {@code c} over {@code ir} on {@code branch}, rendered. */
    private static List<String> record(Path ir, Path c, DroolsBackend.Branch branch, String label, String member)
            throws Exception {
        CaseRunner runner = CaseRunner.forMeasurement(ir, branch);
        JsonNode json = TmsDocuments.JSON.readTree(c.toFile());
        return runner.withSession(json, (build, session, labels) -> {
            DroolsBackend.fire(build, session);
            Map<Object, String> name = new IdentityHashMap<>();
            labels.forEach((l, o) -> name.put(o, l));
            List<String> out = null;
            for (PeriodDecisions d : DroolsBackend.periodDecisionsOf(build, session))
                if (d.object() == labels.get(label) && d.member().equals(member)) {
                    assertTrue(out == null, "twee records voor " + label + " " + member);
                    out = render(d, name);
                }
            assertTrue(out != null, "geen record voor " + label + " " + member);
            return out;
        });
    }

    // ---------------- the witness ----------------

    /** What the witness counted on one branch. */
    private static final class Counts {
        int records, stackRecords, deciders, conflicts, rules, sharedRules, inputs, faulted, builds;
    }

    /** One case to play: the document, the case, and whether a fault is to stand at its fixpoint. */
    private record Play(String name, Path ir, Path c, boolean red) {}

    @Test void every_record_is_the_reading_per_day_of_the_reference_interpreter() throws Exception {
        List<Play> plays = plays();
        List<String> differences = new ArrayList<>();
        Map<DroolsBackend.Branch, Counts> counts = new TreeMap<>();
        Map<String, List<String>> perBranch = new LinkedHashMap<>();
        for (DroolsBackend.Branch branch : TmsDocuments.BRANCHES) {
            Counts n = counts.computeIfAbsent(branch, b -> new Counts());
            // the plays come per document, so one build at a time
            Path last = null;
            CaseRunner runner = null;
            JsonNode document = null;
            IrLoader.Positioned p = null;
            for (Play play : plays) {
                if (!play.ir().equals(last)) {
                    last = play.ir();
                    document = TmsDocuments.JSON.readTree(play.ir().toFile());
                    try {
                        p = IrLoader.loadWithPositions(document);
                        runner = CaseRunner.forMeasurement(play.ir(), branch, true);
                    } catch (RuntimeException e) {
                        // a red document the backend does not carry, which the Drools leg of gate 8 names
                        if (!play.red()) throw e;
                        runner = null;
                        continue;
                    }
                    n.builds++;
                }
                if (runner == null) continue;
                JsonNode doc = document;
                IrLoader.Positioned positions = p;
                JsonNode c = TmsDocuments.JSON.readTree(play.c().toFile());
                String which = branch + " " + play.name();
                List<String> rendered;
                try {
                    rendered = runner.withSession(c, (build, session, labels) -> {
                        DroolsBackend.Firing firing = DroolsBackend.fire(build, session);
                        if (firing.noFixpoint() != null) {
                            // a red case may reach no fixpoint, which is its fault
                            if (!play.red()) differences.add(which + ": geen fixpunt");
                            return List.of();
                        }
                        return check(which, play, doc, positions, build, session, labels, n, differences);
                    });
                } catch (RuntimeException e) {
                    // a red case may fault while its input is read, before there is a session to read
                    if (!play.red()) throw e;
                    continue;
                }
                List<String> other = perBranch.putIfAbsent(play.name(), rendered);
                if (other != null && !other.equals(rendered))
                    differences.add(play.name() + ": de twee takken geven andere records, " + other + " en " + rendered);
            }
            System.out.println("PeriodDecisionsWitnessTest " + branch + ": " + n.builds + " builds, " + n.records
                    + " records (" + n.stackRecords + " van een stapel per dag), perioden met een beslisser " + n.deciders
                    + ", een conflict " + n.conflicts + ", regels " + n.rules + " (" + n.sharedRules + " met twee of"
                    + " meer), de invoer " + n.inputs + "; "
                    + n.faulted + " rode cases met een fout aan het fixpunt");
        }
        assertEquals(List.of(), differences, "records die niet die van de lezing per dag zijn");
        for (Map.Entry<DroolsBackend.Branch, Counts> e : counts.entrySet()) {
            Counts n = e.getValue();
            String b = e.getKey().toString();
            floor(b, "records", n.records, EXPECTED_MIN_RECORDS);
            floor(b, "records van een stapel per dag", n.stackRecords, EXPECTED_MIN_STACK_RECORDS);
            floor(b, "perioden met een beslisser", n.deciders, EXPECTED_MIN_DECIDER_SPANS);
            floor(b, "perioden met een conflict", n.conflicts, EXPECTED_MIN_CONFLICT_SPANS);
            floor(b, "perioden met regels", n.rules, EXPECTED_MIN_RULE_SPANS);
            floor(b, "perioden met twee regels of meer", n.sharedRules, EXPECTED_MIN_SHARED_RULE_SPANS);
            floor(b, "perioden met de invoer", n.inputs, EXPECTED_MIN_INPUT_SPANS);
            floor(b, "cases met een fout", n.faulted, EXPECTED_MIN_FAULTED_CASES);
        }
    }

    private static void floor(String branch, String what, int measured, int floor) {
        assertTrue(measured >= floor, () -> branch + ": " + what + " " + measured + ", onder de vloer " + floor);
    }

    /** The cases of the witness: the goldens the exec-model branch plays, the red corpus, and the fixtures. */
    private static List<Play> plays() throws Exception {
        List<Play> out = new ArrayList<>();
        for (String zone : List.of("golden", "red")) {
            List<Path> documents;
            try (Stream<Path> s = Files.walk(CORPUS.resolve(zone))) {
                documents = s.filter(p -> p.getFileName().toString().equals("ir.json")).map(Path::getParent).sorted()
                        .toList();
            }
            for (Path g : documents) {
                List<Path> cases;
                try (Stream<Path> s = Files.list(g)) {
                    cases = s.filter(p -> p.getFileName().toString().matches("case.*\\.json")).sorted().toList();
                }
                if (cases.isEmpty()) continue;
                boolean red = zone.equals("red");
                if (!red && !CaseRunner.viaExecModel(g.resolve("ir.json"), cases).status().equals("PLAYED")) continue;
                for (Path c : cases)
                    out.add(new Play(zone + "/" + CORPUS.resolve(zone).relativize(g) + "/" + c.getFileName(),
                            g.resolve("ir.json"), c, red));
            }
        }
        Path days = FIXTURES.resolve("defaults-per-day"), cases = FIXTURES.resolve("defaults-per-day-cases");
        for (String d : List.of("uitzondering", "ongeordend", "afwijking", "verdrongen-conflict", "geldig-vanaf",
                "lazy-consequence", "deposit"))
            out.add(fixture("defaults-per-day/" + d, days.resolve(d).resolve("ir.json"), days.resolve(d).resolve("case.json")));
        out.add(fixture("defaults-per-day-cases/turn-on-no-day", GOLDEN_OPGAVE.resolve("ir.json"),
                cases.resolve("turn-on-no-day").resolve("case.json")));
        out.add(fixture("defaults-per-day-cases/justified-on-no-day", days.resolve("uitzondering").resolve("ir.json"),
                cases.resolve("justified-on-no-day").resolve("case.json")));
        out.add(fixture("defaults-per-day-cases/deposit-dropped", days.resolve("deposit").resolve("ir.json"),
                cases.resolve("deposit-dropped").resolve("case.json")));
        out.add(fixture("defaults-per-day-cases/lazy-consequence-reached", days.resolve("lazy-consequence").resolve("ir.json"),
                cases.resolve("lazy-consequence-reached").resolve("case.json")));
        out.add(fixture("defaults-per-day-cases/bucket-input", GOLDEN_OPGAVE.resolve("ir.json"),
                cases.resolve("bucket-input").resolve("case.json")));
        Path writers = FIXTURES.resolve("writers-per-day");
        for (String d : List.of("een-schrijver", "een-schrijver-gat", "een-schrijver-gelijk", "twee-schrijvers"))
            out.add(fixture("writers-per-day/" + d, writers.resolve(d).resolve("ir.json"),
                    writers.resolve(d).resolve("case.json")));
        // two rules that write one value on a day stand on its span side by side, in the order of the document
        Path alike = FIXTURES.resolve("conditions-per-period").resolve("twee-schrijvers-gelijk");
        out.add(fixture("conditions-per-period/twee-schrijvers-gelijk", alike.resolve("ir.json"),
                alike.resolve("case.json")));
        return out;
    }

    private static Play fixture(String name, Path ir, Path c) throws Exception {
        return new Play(name, ir, c, TmsDocuments.JSON.readTree(c.toFile()).has("expectedFault"));
    }

    /** The checks of one session at its fixpoint; returns its records, rendered, for the comparison of the branches. */
    private static List<String> check(String which, Play play, JsonNode document, IrLoader.Positioned p,
                                      DroolsBackend.Build build, KieSession session, Map<String, Object> labels,
                                      Counts n, List<String> differences) {
        Map<Object, String> name = new IdentityHashMap<>();
        labels.forEach((l, o) -> name.put(o, l));
        Fixpoint fp = DroolsBackend.fixpointOf(build, session, name::get);
        List<PeriodDecisions> records = DroolsBackend.periodDecisionsOf(build, session);
        for (Fixpoint.MadeObject m : fp.objects()) name.putIfAbsent(m.object(), "een aangemaakte "
                + m.object().getClass().getSimpleName());
        List<String> rendered = new ArrayList<>();
        for (PeriodDecisions d : records) rendered.addAll(render(d, name));
        java.util.Collections.sort(rendered);
        // a red case has no answer, whether its fault stands at the fixpoint or comes at the write-back
        if (play.red()) {
            if (!fp.faults().isEmpty()) n.faulted++;
            return rendered;
        }
        if (!fp.faults().isEmpty()) {
            differences.add(which + ": een fout aan het fixpunt, " + fp.faults());
            return rendered;
        }
        RegelSpraakModel model = build.model();
        TmsTranspiler t = build.execModelTranspiler();
        List<ConditionReading> readings = DroolsBackend.readingsOf(build, session);
        Map<String, ConditionReading> reading = new HashMap<>();
        for (ConditionReading r : readings) reading.put(r.form() + " " + System.identityHashCode(r.object()), r);
        List<Object> buckets = new ArrayList<>(session.getObjects(new ClassObjectFilter(BucketInput.class)));
        // the members a stack or rules write, on a timeline
        Map<String, String> writtenMembers = new LinkedHashMap<>();
        for (JsonNode g : document.path("rules").path("prioritizedDefaults"))
            if (g.has("attribute")) writtenMembers.put(Namen.norm(g.get("attribute").asText()), g.get("attribute").asText());
        for (JsonNode r : document.path("rules").path("rules"))
            for (JsonNode v : r.path("versions"))
                if (v.path("target").path("node").asText().equals("AttributeReference") && v.has("value"))
                    writtenMembers.putIfAbsent(Namen.norm(v.path("target").path("attribute").asText()),
                            v.path("target").path("attribute").asText());
        List<PeriodDecisions> unmatched = new ArrayList<>(records);
        for (Object o : objects(labels, fp)) {
            ObjectType type = typeOf(model, o);
            for (String member : writtenMembers.values()) {
                ObjectType declarant = type.declarantOf(member);
                if (declarant == null || declarant.attributeGranularity.entrySet().stream()
                        .noneMatch(a -> a.getKey().equalsIgnoreCase(member) && a.getValue() != null)) continue;
                String at = which + " " + name.get(o) + " " + member;
                PeriodDecisions d = null;
                for (PeriodDecisions x : records)
                    if (x.object() == o && Namen.norm(x.member()).equals(Namen.norm(member))) {
                        if (d != null) differences.add(at + ": twee records");
                        d = x;
                    }
                if (d == null) {
                    differences.add(at + ": geen record");
                    continue;
                }
                unmatched.remove(d);
                n.records++;
                List<Period> input = inputOf(o, declarant, member, buckets);
                Stack stack = stackOf(t, document, type, member);
                List<PeriodDecisions.Span> expected;
                if (stack != null) {
                    n.stackRecords++;
                    List<List<String>> chains = stack.composition().chains().stream()
                            .map(c -> c.stream().map(StackComposition.Entry::pointer).toList()).toList();
                    if (!chains.equals(d.rootChains()))
                        differences.add(at + ": wortelketens " + d.rootChains() + ", de samenstelling " + chains);
                    expected = stackSpans(at, d, stack, declarant.name + "." + member, document, p, reading, o, input,
                            differences);
                } else {
                    if (!d.rootChains().isEmpty()) differences.add(at + ": wortelketens zonder stapel " + d.rootChains());
                    expected = ruleSpans(at, d, member, document, p, reading, o, input, differences);
                }
                List<String> want = render(new PeriodDecisions(o, d.member(), d.rootChains(), expected), name);
                List<String> got = render(d, name);
                if (!want.equals(got)) differences.add(at + ": " + got + ", de lezing per dag geeft " + want);
                union(at, d, fp, o, input, differences);
                for (PeriodDecisions.Span s : d.spans()) {
                    if (s.decider() != null) n.deciders++;
                    if (s.conflict() != null) n.conflicts++;
                    if (!s.rules().isEmpty()) n.rules++;
                    if (s.rules().size() > 1) n.sharedRules++;
                    if (s.input() != null) n.inputs++;
                }
            }
        }
        for (PeriodDecisions d : unmatched)
            differences.add(which + ": een record op geen tijdlijndoel van een schrijver: " + render(d, name));
        return rendered;
    }

    /** The stack of {@code t} that decides {@code member} on objects of exactly {@code type}, or null. */
    private record Stack(String name, StackComposition composition) {}

    private static Stack stackOf(TmsTranspiler t, JsonNode document, ObjectType type, String member) {
        Stack out = null;
        for (Map.Entry<String, TmsTranspiler.Stack> e : t.stacks().entrySet()) {
            TmsTranspiler.Stack s = e.getValue();
            if (!s.type().name().equals(type.name) || s.composition() == null) continue;
            String first = s.composition().chains().get(0).get(0).pointer();
            JsonNode at = document.at(first);
            String target = first.startsWith("/rules/prioritizedDefaults/")
                    ? document.at(first.substring(0, first.indexOf("/defaults/"))).path("attribute").asText(null)
                    : at.path("target").path("attribute").asText(null);
            if (target == null || !Namen.norm(target).equals(Namen.norm(member))) continue;
            if (out != null) throw new AssertionError("twee stapels op " + type.name + "." + member);
            out = new Stack(e.getKey(), s.composition());
        }
        return out;
    }

    // ---------------- the reading per day ----------------

    /** One day's outcome of a stack: a decider or a conflict. */
    private record Settled(DefaultsDecision.Decider decider, DefaultsDecision.Conflict conflict) {}

    /** What one stack's readings say, per day, for one object. */
    private record StackDay(Stack stack, String targetForm, JsonNode document, IrLoader.Positioned p,
                            Map<String, ConditionReading> reading, Object object, List<Period> input,
                            String at, List<String> differences) {

        /** The root of the stack on day {@code d}: its groups reconciled (ch. 7.2). */
        Settled root(LocalDate d) {
            return reconcile(stack.composition().chains(), List.of(), null, targetForm, d);
        }

        /** The members {@code chains} reconciled on day {@code d}: the first conflict in member order passes on;
         *  deciding members that differ conflict under {@code key}, recorded at {@code pointer}; deciding members that
         *  agree decide as the first. */
        Settled reconcile(List<List<StackComposition.Entry>> chains, List<Integer> path, String pointer, String key,
                          LocalDate d) {
            List<DefaultsDecision.Decider> deciders = new ArrayList<>();
            for (List<StackComposition.Entry> chain : chains) {
                Settled s = chain(chain, path, d);
                if (s == null) continue;
                if (s.conflict() != null) return s;
                deciders.add(s.decider());
            }
            if (deciders.isEmpty()) return null;
            for (DefaultsDecision.Decider x : deciders)
                if (!same(x.decision(), deciders.get(0).decision()))
                    return new Settled(null, new DefaultsDecision.Conflict(pointer, key, deciders));
            return new Settled(deciders.get(0), null);
        }

        /** The highest default of {@code chain} that settles day {@code d} (ch. 7.1, 7.9). */
        Settled chain(List<StackComposition.Entry> chain, List<Integer> path, LocalDate d) {
            for (int i = 0; i < chain.size(); i++) {
                StackComposition.Entry e = chain.get(i);
                List<Integer> own = new ArrayList<>(path);
                own.add(i);
                if (e.exceptions() != null) {
                    StringBuilder key = new StringBuilder(targetForm);
                    for (int x : own) key.append('/').append(x);
                    Settled s = reconcile(e.exceptions().chains(), own, e.pointer(), key.toString(), d);
                    if (s != null) return s;
                }
                DefaultsDecision.Decider own0 = own(e.pointer(), d);
                if (own0 != null) return new Settled(own0, null);
            }
            return null;
        }

        /** The default at {@code pointer} on day {@code d}: it decides the value of the root of its consequence in its
         *  reading there, the filled segments within its justification's periods, the initialisation default only
         *  where the input leaves the target empty (ch. 7.3, 7.11); none where it has no consequence. */
        DefaultsDecision.Decider own(String pointer, LocalDate d) {
            JsonNode node = document.at(pointer);
            boolean folded = pointer.startsWith("/rules/rules/");
            String consequence = folded ? "value" : "consequence", justification = folded ? "condition" : "justification";
            if (!node.has(consequence)) return null;
            ConditionReading r = reading.get(pointer + " " + System.identityHashCode(object));
            if (r == null) {
                differences.add(at + ": geen lezing van " + pointer);
                return null;
            }
            if (r.status() == ConditionReading.Status.OUTSIDE_VALIDITY || r.nodes().isEmpty()) return null;
            if (node.path("initialisation").asBoolean(false) && valueOn(input, d) != null) return null;
            Object j = p.positions().at(pointer + "/" + justification);
            int root = (j == null ? 0 : p.positions().subtree(j).size()) + throughLets(node.get(consequence));
            for (ConditionReading.NodeReading x : r.nodes())
                if (x.node() == root) {
                    if (!x.reached() || x.fault() != null || x.value() == null) return null;
                    Object v = exactOn((List<?>) x.value(), d);
                    return v == null ? null : new DefaultsDecision.Decider(pointer, v);
                }
            differences.add(at + ": de lezing van " + pointer + " heeft geen knoop " + root + ", de wortel van het gevolg");
            return null;
        }
    }

    /** The spans the readings of {@code stack} give per day for {@code d}'s object. */
    private static List<PeriodDecisions.Span> stackSpans(String at, PeriodDecisions d, Stack stack, String targetForm,
                                                         JsonNode document, IrLoader.Positioned p,
                                                         Map<String, ConditionReading> reading, Object o,
                                                         List<Period> input, List<String> differences) {
        StackDay day = new StackDay(stack, targetForm, document, p, reading, o, input, at, differences);
        TreeSet<LocalDate> cuts = cuts(d, input);
        for (ConditionReading r : reading.values())
            if (r.object() == o) for (ConditionReading.NodeReading x : r.nodes()) bounds(x.value(), cuts);
        List<PeriodDecisions.Span> out = new ArrayList<>();
        LocalDate previous = null;
        for (LocalDate c : cuts) {
            if (previous != null) {
                Settled s = day.root(previous);
                DefaultsDecision.Decider decider = s == null || s.decider() == null ? null : atRest(s.decider());
                DefaultsDecision.Conflict conflict = s == null || s.conflict() == null ? null
                        : new DefaultsDecision.Conflict(s.conflict().pointer(), s.conflict().key(),
                                s.conflict().deciders().stream().map(PeriodDecisionsWitnessTest::atRest).toList());
                add(out, new PeriodDecisions.Span(previous, c, decider, conflict, List.of(),
                        decider == null ? valueOn(input, previous) : null));
            }
            previous = c;
        }
        return out;
    }

    /** The spans the readings of the rules on {@code member} give per day for {@code d}'s object: per day the rules,
     *  in the order of the document, whose reading writes a value there, the root of the value after the nodes of the
     *  condition (ch. 3.11, 5.12). */
    private static List<PeriodDecisions.Span> ruleSpans(String at, PeriodDecisions d, String member, JsonNode document,
                                                        IrLoader.Positioned p, Map<String, ConditionReading> reading,
                                                        Object o, List<Period> input, List<String> differences) {
        List<String> pointers = new ArrayList<>();
        List<List<?>> timelines = new ArrayList<>();
        JsonNode rules = document.path("rules").path("rules");
        for (int k = 0; k < rules.size(); k++)
            for (int v = 0; v < rules.get(k).path("versions").size(); v++) {
                JsonNode version = rules.get(k).path("versions").get(v);
                if (!version.has("value") || !Namen.norm(version.path("target").path("attribute").asText())
                        .equals(Namen.norm(member)))
                    continue;
                String pointer = "/rules/rules/" + k + "/versions/" + v;
                ConditionReading r = reading.get(pointer + " " + System.identityHashCode(o));
                if (r == null) continue;
                if (version.path("initialisation").asBoolean(false) || version.path("target").has("path"))
                    differences.add(at + ": " + pointer + " is een initialisatie of schrijft over een pad, wat deze"
                            + " getuige niet per dag leest");
                if (r.status() != ConditionReading.Status.HOLDS) continue;
                Object c = p.positions().at(pointer + "/condition");
                int root = (c == null ? 0 : p.positions().subtree(c).size()) + throughLets(version.get("value"));
                for (ConditionReading.NodeReading x : r.nodes())
                    if (x.node() == root && x.reached() && x.fault() == null && x.value() != null) {
                        pointers.add(pointer);
                        timelines.add((List<?>) x.value());
                    }
            }
        TreeSet<LocalDate> cuts = cuts(d, input);
        for (List<?> t : timelines) bounds(t, cuts);
        List<PeriodDecisions.Span> out = new ArrayList<>();
        LocalDate previous = null;
        for (LocalDate c : cuts) {
            if (previous != null) {
                List<DefaultsDecision.Decider> deciders = new ArrayList<>();
                for (int i = 0; i < timelines.size(); i++) {
                    Object v = exactOn(timelines.get(i), previous);
                    if (v != null) deciders.add(atRest(new DefaultsDecision.Decider(pointers.get(i), v)));
                }
                add(out, new PeriodDecisions.Span(previous, c, null, null, deciders,
                        deciders.isEmpty() ? valueOn(input, previous) : null));
            }
            previous = c;
        }
        return out;
    }

    /** The union of the values of the spans of {@code d} with the input is the fixpoint's timeline of the member, per
     *  day; the input where the fixpoint derives none. */
    private static void union(String at, PeriodDecisions d, Fixpoint fp, Object o, List<Period> input,
                              List<String> differences) {
        Fixpoint.DerivedValue derived = null;
        for (Fixpoint.DerivedValue v : fp.values())
            if (v.object() == o && Namen.norm(v.attribute()).equals(Namen.norm(d.member()))) derived = v;
        List<Period> held = derived == null ? input : Period.list((List<?>) derived.value());
        TreeSet<LocalDate> cuts = cuts(d, held);
        LocalDate previous = null;
        for (LocalDate c : cuts) {
            if (previous != null) {
                PeriodDecisions.Span s = null;
                for (PeriodDecisions.Span x : d.spans())
                    if (!x.from().isAfter(previous) && x.to().isAfter(previous)) s = x;
                Object value = s == null ? null : s.decider() != null ? s.decider().decision()
                        : !s.rules().isEmpty() ? s.rules().get(0).decision() : s.input();
                Object want = valueOn(held, previous);
                if (!same(value, want))
                    differences.add(at + ": op " + previous + " geven de perioden " + value + ", het fixpunt " + want);
            }
            previous = c;
        }
    }

    // ---------------- helpers ----------------

    /** How many nodes the root of the value {@code value} lies before the node that reads its timeline: a {@code Let}
     *  whose body reads its own binder has the timeline at its value, the node after it, and is itself read unreached
     *  (ch. 5.12); 0 for every other value. */
    private static int throughLets(JsonNode value) {
        int skipped = 0;
        while (value.path("node").asText().equals("Let")) {
            JsonNode body = value.path("body");
            if (!body.path("node").asText().equals("BinderRef")
                    || !body.path("name").asText().equals(value.path("binder").asText()))
                throw new AssertionError("een Let als waarde die niet haar eigen binder leest: " + value);
            value = value.path("value");
            skipped++;
        }
        return skipped;
    }

    /** The input of {@code member} on the bean {@code o}, as the standing reads it: none where the case gives the
     *  asOfDate-bucket scalar. The bean holds the input until the write-back. */
    private static List<Period> inputOf(Object o, ObjectType declarant, String member, List<Object> buckets) {
        for (Object b : buckets)
            if (((BucketInput) b).subject() == o && ((BucketInput) b).attribute().equals(
                    ProducerConsumer.ElementKey.of(declarant.name, member, false)))
                return List.of();
        Object onBean = Accessors.leesEigenschap(o, Namen.property(member));
        return onBean == null ? List.of() : Period.list((List<?>) onBean);
    }

    private static List<Object> objects(Map<String, Object> labels, Fixpoint fp) {
        List<Object> out = new ArrayList<>(labels.values());
        for (Fixpoint.MadeObject m : fp.objects()) out.add(m.object());
        return out;
    }

    /** The object type whose bean class {@code o} is. */
    private static ObjectType typeOf(RegelSpraakModel model, Object o) {
        for (ObjectType t : model.objectTypes.values())
            if (o.getClass().getName().equals(FactModelGenerator.PACKAGE + "." + Namen.ident(t.name, true))) return t;
        throw new AssertionError("geen objecttype voor " + o.getClass().getName());
    }

    /** Every bound of the spans of {@code d} and of {@code timeline}, and the two open ends. */
    private static TreeSet<LocalDate> cuts(PeriodDecisions d, List<Period> timeline) {
        TreeSet<LocalDate> cuts = new TreeSet<>(List.of(Period.OPEN_FROM, Period.OPEN_TO));
        for (PeriodDecisions.Span s : d.spans()) {
            cuts.add(s.from());
            cuts.add(s.to());
        }
        bounds(timeline, cuts);
        return cuts;
    }

    /** The bounds of the segments or periods of {@code value}, where it is a timeline, into {@code cuts}. */
    private static void bounds(Object value, TreeSet<LocalDate> cuts) {
        if (!(value instanceof List<?> l)) return;
        for (Object x : l) {
            LocalDate[] b = bounds(x);
            if (b == null) return;
            cuts.add(b[0]);
            cuts.add(b[1]);
        }
    }

    /** The bounds of one segment, a {@link Period} or an exact segment of the arithmetic, null for anything else. */
    private static LocalDate[] bounds(Object x) {
        if (x instanceof Period p) return new LocalDate[]{p.from(), p.to()};
        if (x instanceof dev.uchar.regelspraak.transpiler.tms.PeriodArithmetic.Exact e)
            return new LocalDate[]{e.from(), e.to()};
        return null;
    }

    /** The exact value of the timeline {@code segments} on day {@code d}, null where it is leeg there. */
    private static Object exactOn(List<?> segments, LocalDate d) {
        for (Object x : segments) {
            LocalDate[] b = bounds(x);
            if (b == null) throw new AssertionError("geen segment: " + x);
            if (!b[0].isAfter(d) && b[1].isAfter(d))
                return x instanceof Period p ? p.value() : ((dev.uchar.regelspraak.transpiler.tms.PeriodArithmetic.Exact) x).value();
        }
        return null;
    }

    private static Object valueOn(List<Period> timeline, LocalDate d) {
        for (Period p : timeline) if (!p.from().isAfter(d) && p.to().isAfter(d)) return p.value();
        return null;
    }

    private static DefaultsDecision.Decider atRest(DefaultsDecision.Decider d) {
        return new DefaultsDecision.Decider(d.pointer(), ComputeHelpers.collapse(d.decision()));
    }

    /** Numbers as exact numbers, a constructor value of a timeline of a sum type by its equality. */
    private static boolean same(Object a, Object b) {
        if (a == null || b == null) return a == b;
        if (a instanceof Number && b instanceof Number) return Breuk.van(a).compareTo(Breuk.van(b)) == 0;
        return a.equals(b);
    }

    /** {@code span} after {@code spans}, joined with the last where it renders alike. */
    private static void add(List<PeriodDecisions.Span> spans, PeriodDecisions.Span span) {
        PeriodDecisions.Span last = spans.isEmpty() ? null : spans.get(spans.size() - 1);
        if (last != null && outcome(last).equals(outcome(span)))
            spans.set(spans.size() - 1, new PeriodDecisions.Span(last.from(), span.to(), last.decider(), last.conflict(),
                    last.rules(), last.input()));
        else spans.add(span);
    }

    /** {@code d} as lines: the object, the member and the root chains, then one line per span. */
    static List<String> render(PeriodDecisions d, Map<Object, String> name) {
        List<String> out = new ArrayList<>();
        out.add(Objects.requireNonNullElse(name.get(d.object()), "?") + " " + d.member() + " " + d.rootChains());
        for (PeriodDecisions.Span s : d.spans())
            out.add("[" + bound(s.from()) + " " + bound(s.to()) + ") " + outcome(s));
        return out;
    }

    private static String outcome(PeriodDecisions.Span s) {
        StringBuilder b = new StringBuilder();
        if (s.decider() != null) b.append("default ").append(decider(s.decider()));
        if (s.conflict() != null) {
            b.append("conflict ").append(s.conflict().key());
            if (s.conflict().pointer() != null) b.append(" onder ").append(s.conflict().pointer());
            for (DefaultsDecision.Decider x : s.conflict().deciders()) b.append(", ").append(decider(x));
            b.append("; ");
        }
        for (int i = 0; i < s.rules().size(); i++) b.append(i == 0 ? "regel " : ", regel ").append(decider(s.rules().get(i)));
        if (s.decider() == null && s.rules().isEmpty()) b.append(s.input() == null ? "leeg" : "invoer " + number(s.input()));
        return b.toString();
    }

    private static String decider(DefaultsDecision.Decider d) {
        return d.pointer() + " " + number(d.decision());
    }

    private static String number(Object v) {
        return v instanceof BigDecimal x ? x.stripTrailingZeros().toPlainString() : String.valueOf(v);
    }

    private static String bound(LocalDate d) {
        return d.equals(Period.OPEN_FROM) || d.equals(Period.OPEN_TO) ? "-" : d.toString();
    }
}
