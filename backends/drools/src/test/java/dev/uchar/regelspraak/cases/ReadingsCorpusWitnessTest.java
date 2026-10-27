package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;

import dev.uchar.regelspraak.ir.Condition;
import dev.uchar.regelspraak.ir.Expr;
import dev.uchar.regelspraak.loader.IrLoader;
import dev.uchar.regelspraak.loader.NodePositions;
import dev.uchar.regelspraak.model.RegelSpraakModel;
import dev.uchar.regelspraak.model.RegelSpraakModel.ObjectType;
import dev.uchar.regelspraak.model.RegelSpraakModel.RoleBindingRef;
import dev.uchar.regelspraak.runtime.Accessors;
import dev.uchar.regelspraak.runtime.RegelSpraakRuntime;
import dev.uchar.regelspraak.transpiler.core.Namen;
import dev.uchar.regelspraak.transpiler.tms.ComputeHelpers;
import dev.uchar.regelspraak.transpiler.tms.ConditionReading;
import dev.uchar.regelspraak.transpiler.tms.DefaultsDecision;
import dev.uchar.regelspraak.transpiler.tms.Fixpoint;
import dev.uchar.regelspraak.transpiler.tms.Value;

import org.junit.jupiter.api.Test;
import org.kie.api.runtime.ClassObjectFilter;
import org.kie.api.runtime.KieSession;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Acceptance (c) of card fixpoint-readings, beside {@link DefaultsDecisionCorpusWitnessTest}: over every case of every
 * golden the exec-model branch plays, on both branches of the build with readings, the readings of the conditions
 * ({@code DroolsBackend.readingsOf}) agree with what the fixpoint gives, read without them:
 *
 * <ul>
 *   <li>the reading holds where the form fired: a characteristic attribution that holds stands derived on its object,
 *       and every derived characteristic has a reading of an attribution that holds, a deciding default or a creation
 *       that made its object; a rule on an attribute whose condition holds and whose value is filled stands as a
 *       {@code Value} of that rule on its object, and one whose condition fails or faults does not, where a rule without
 *       a condition holds wherever it is at its turn (card readings-of-unconditional-forms); a consistency
 *       holds where its object has no violation of it (EF-025); an object creation holds where it made an object for
 *       its subject; the default that decided ({@code DroolsBackend.decisionsOf}) holds, and where no default decided
 *       none holds with a decision;</li>
 *   <li>every operand that reads a member, an attribute read the evaluation reached, has the value the fixpoint gives
 *       that member on the object the read reaches ({@code DroolsBackend.reached}), the derived value or else the
 *       input; and every check of a characteristic of the object itself has the verdict of what stands there;</li>
 *   <li>every node that reads a member of one object, an attribute read or a characteristic check the evaluation
 *       reached without a fault, names the very object {@code DroolsBackend.reached} reaches over its walk on the
 *       written-back beans, by identity: the object of the reading for a member of its own, the object the path
 *       reached for one over a path, and none where a link on the path is absent (RUNTIME-SEMANTICS ch. 5.12); and no
 *       other node names one;</li>
 *   <li>a default that a higher default displaced (RUNTIME-SEMANTICS ch. 5.12) has the nodes of its condition, and of
 *       its value where the condition holds, as a reading at its turn, and the checks of the operands and the stops
 *       below hold for them as well; a displaced form that is no default has no nodes, and of the defaults only an
 *       initialisation, a default or a rule the fold of ch. 7.10 took in, whose target the input fills; a form outside
 *       its validity has no nodes;</li>
 *   <li>the stop of every composite follows from the verdicts of its members (RUNTIME-SEMANTICS ch. 5.10): a member is
 *       reached exactly where the stop rule did not settle the verdict before it, and the composite's verdict is the
 *       stop rule's;</li>
 *   <li>every node of a reading is the node of the document its number names, of the kind it says.</li>
 * </ul>
 *
 * <p>An operand is held to the beans once the fixpoint is written back on them, where a link a creation laid is on its
 * field; a session at whose fixpoint an evaluation fault stands has no answer, and its operands are not held. A rule
 * that the fold of RUNTIME-SEMANTICS ch. 7.10 took into a stack reads on the type of the stack as a default of it, which
 * the decisions hold.
 *
 * <p><b>Red</b> with a mutant, not committed: a composite of the recording rendering that does not stop, red on 1582
 * readings, among them {@code anchor/jutterswet-hardheid/case1-standaard.json} ("samenstelling 0 bereikte lid 2 na de
 * stop"), on both branches.
 *
 * <p>Floors, measured 2026-10-21: {@value #EXPECTED_MIN_READINGS} (case, form, object) with nodes per branch,
 * {@value #EXPECTED_MIN_NODES} nodes, {@value #EXPECTED_MIN_OPERANDS} operands held to the fixpoint,
 * {@value #EXPECTED_MIN_COMPOUNDS} composites held to the stop rule, {@value #EXPECTED_MIN_DISPLACED} displaced and
 * {@value #EXPECTED_MIN_OUTSIDE} outside their validity; measured 2026-10-27, {@value #EXPECTED_MIN_OBJECTS} nodes whose
 * object is held to the walk, {@value #EXPECTED_MIN_NAVIGATED} of them over a path, and
 * {@value #EXPECTED_MIN_DISPLACED_READ} displaced readings with nodes. A smaller count is a corpus that lost forms, or a
 * reading that silently stopped giving nodes or objects; a growing corpus needs no change here.
 */
class ReadingsCorpusWitnessTest {

    private static final Path CORPUS = Path.of("..", "..", "corpus");

    static final int EXPECTED_MIN_READINGS = 3635;
    static final int EXPECTED_MIN_NODES = 13971;
    static final int EXPECTED_MIN_OPERANDS = 3611;
    static final int EXPECTED_MIN_COMPOUNDS = 1199;
    static final int EXPECTED_MIN_DISPLACED = 178;
    static final int EXPECTED_MIN_DISPLACED_READ = 207;
    static final int EXPECTED_MIN_OUTSIDE = 13;
    static final int EXPECTED_MIN_OBJECTS = 5961;
    static final int EXPECTED_MIN_NAVIGATED = 746;

    /** What the witness counted on one branch. */
    private static final class Counts {
        int readings, nodes, operands, compounds, displaced, outside, holds, fails, faults;
        /** Displaced readings with nodes, those of a default a higher one displaced, and their nodes. */
        int displacedRead, displacedNodes;
        /** Nodes whose object is held to the walk; of them those over a path, and of those the ones without an object. */
        int objects, navigated, unlinked;
        final Map<String, Integer> kinds = new java.util.TreeMap<>();
    }

    @Test void the_readings_agree_with_the_fixpoint() throws Exception {
        List<Path> goldens;
        try (Stream<Path> s = Files.walk(CORPUS.resolve("golden"))) {
            goldens = s.filter(p -> p.getFileName().toString().equals("ir.json")).map(Path::getParent).sorted()
                    .toList();
        }
        List<String> differences = new ArrayList<>();
        for (DroolsBackend.Branch branch : TmsDocuments.BRANCHES) {
            Counts n = new Counts();
            for (Path g : goldens) {
                List<Path> casePaths = new ArrayList<>();
                for (Path casePath : casePaths(g))
                    if (!TmsDocuments.JSON.readTree(casePath.toFile()).has("expectedFault")) casePaths.add(casePath);
                if (casePaths.isEmpty()) continue;
                Path ir = g.resolve("ir.json");
                if (!CaseRunner.viaExecModel(ir, casePaths).status().equals("PLAYED")) continue;
                String name = CORPUS.resolve("golden").relativize(g).toString();
                JsonNode document = TmsDocuments.JSON.readTree(ir.toFile());
                IrLoader.Positioned p = IrLoader.loadWithPositions(document);
                CaseRunner runner = CaseRunner.forMeasurement(ir, branch, true);
                for (Path casePath : casePaths) {
                    JsonNode c = TmsDocuments.JSON.readTree(casePath.toFile());
                    String which = branch + " " + name + "/" + casePath.getFileName();
                    runner.withSession(c, (build, session, labels) -> {
                        DroolsBackend.Firing firing = DroolsBackend.fire(build, session);
                        if (firing.noFixpoint() != null) {
                            differences.add(which + ": geen fixpunt");
                            return null;
                        }
                        check(which, document, p, build, session, labels, n, differences);
                        return null;
                    });
                }
            }
            System.out.println("ReadingsCorpusWitnessTest " + branch + ": " + n.readings + " lezingen met knopen ("
                    + n.holds + " geldt, " + n.fails + " faalt, " + n.faults + " fout), " + n.nodes + " knopen, "
                    + n.operands + " operanden, " + n.compounds + " samenstellingen, " + n.displaced + " verdrongen ("
                    + n.displacedRead + " gelezen, met " + n.displacedNodes + " knopen), "
                    + n.outside + " buiten geldigheid, " + n.objects + " objecten (" + n.navigated + " over een pad, waarvan "
                    + n.unlinked + " zonder object); per soort " + n.kinds);
            int r = n.readings, k = n.nodes, o = n.operands, s = n.compounds, d = n.displaced, b = n.outside;
            assertTrue(r >= EXPECTED_MIN_READINGS, () -> branch + ": lezingen " + r + ", onder de vloer "
                    + EXPECTED_MIN_READINGS);
            assertTrue(k >= EXPECTED_MIN_NODES, () -> branch + ": knopen " + k + ", onder de vloer " + EXPECTED_MIN_NODES);
            assertTrue(o >= EXPECTED_MIN_OPERANDS, () -> branch + ": operanden " + o + ", onder de vloer "
                    + EXPECTED_MIN_OPERANDS);
            assertTrue(s >= EXPECTED_MIN_COMPOUNDS, () -> branch + ": samenstellingen " + s + ", onder de vloer "
                    + EXPECTED_MIN_COMPOUNDS);
            assertTrue(d >= EXPECTED_MIN_DISPLACED, () -> branch + ": verdrongen " + d + ", onder de vloer "
                    + EXPECTED_MIN_DISPLACED);
            int dr = n.displacedRead;
            assertTrue(dr >= EXPECTED_MIN_DISPLACED_READ, () -> branch + ": verdrongen met knopen " + dr
                    + ", onder de vloer " + EXPECTED_MIN_DISPLACED_READ);
            assertTrue(b >= EXPECTED_MIN_OUTSIDE, () -> branch + ": buiten geldigheid " + b + ", onder de vloer "
                    + EXPECTED_MIN_OUTSIDE);
            int j = n.objects, v = n.navigated;
            assertTrue(j >= EXPECTED_MIN_OBJECTS, () -> branch + ": objecten " + j + ", onder de vloer "
                    + EXPECTED_MIN_OBJECTS);
            assertTrue(v >= EXPECTED_MIN_NAVIGATED, () -> branch + ": objecten over een pad " + v + ", onder de vloer "
                    + EXPECTED_MIN_NAVIGATED);
        }
        assertEquals(List.of(), differences, "lezingen die niet met het fixpunt overeenstemmen");
    }

    /** The checks of one session at its fixpoint. */
    private static void check(String which, JsonNode document, IrLoader.Positioned p, DroolsBackend.Build build,
                              KieSession session, Map<String, Object> labels, Counts n, List<String> differences) {
        Map<Object, String> label = new IdentityHashMap<>();
        labels.forEach((l, o) -> label.put(o, l));
        Fixpoint fp = DroolsBackend.fixpointOf(build, session, label::get);
        List<ConditionReading> readings = DroolsBackend.readingsOf(build, session);
        List<DefaultsDecision> decisions = DroolsBackend.decisionsOf(build, session);
        Map<String, ConditionReading> byKey = new HashMap<>();
        for (ConditionReading r : readings) byKey.put(r.form() + " " + System.identityHashCode(r.object()), r);
        List<Value> values = new ArrayList<>();
        for (Object f : session.getObjects(new ClassObjectFilter(Value.class))) values.add((Value) f);
        RegelSpraakModel model = p.loaded().model();
        // the reads, held to the beans once the fixpoint is written back on them, a made link on its field among it
        List<Runnable> operands = new ArrayList<>();
        for (ConditionReading r : readings) {
            String at = which + " " + r.form() + " op " + name(label, r.object());
            // a rule that the fold of ch. 7.10 took into a stack on the type of the object reads there as a default of
            // that stack, which the decisions below hold
            boolean ofStack = decisions.stream().anyMatch(d -> d.object() == r.object() && inStack(d, r.form()));
            boolean ofDefault = ofStack || r.form().startsWith("/rules/prioritizedDefaults/");
            boolean displaced = r.status() == ConditionReading.Status.DISPLACED;
            if (r.status() == ConditionReading.Status.OUTSIDE_VALIDITY || displaced && r.nodes().isEmpty()) {
                if (!r.nodes().isEmpty()) differences.add(at + ": " + r.status() + " met knopen");
                if (displaced) {
                    n.displaced++;
                    // a default displaced by a higher one is read all the same; only the input on the target of an
                    // initialisation leaves a default nothing to read (ch. 7.3)
                    String unread = ofDefault ? unread(document, model, r) : null;
                    if (unread != null) differences.add(at + ": DISPLACED zonder knopen, " + unread);
                } else n.outside++;
                continue;
            }
            if (r.nodes().isEmpty()) {
                if (r.status() != ConditionReading.Status.FAULTS)
                    differences.add(at + ": " + r.status() + " zonder knopen");
                continue;
            }
            Form form = form(document, p, r.form());
            if (displaced) {
                // a default that was not at its turn, read as at its turn: the nodes of its condition, and of its
                // value where the condition holds
                n.displaced++;
                n.displacedRead++;
                n.displacedNodes += r.nodes().size();
                if (!ofDefault) differences.add(at + ": DISPLACED met knopen, en de vorm is geen default van een stapel");
                String turn = readAsAtItsTurn(form, r);
                if (turn != null) differences.add(at + ": DISPLACED " + turn);
            } else {
                n.readings++;
                n.nodes += r.nodes().size();
                switch (r.status()) {
                    case HOLDS -> n.holds++;
                    case FAILS -> n.fails++;
                    default -> n.faults++;
                }
                n.kinds.merge(form.kind(), 1, Integer::sum);
            }
            // every node is the node of the document its number names
            Map<Integer, ConditionReading.NodeReading> byNumber = new HashMap<>();
            int previous = -1;
            for (ConditionReading.NodeReading x : r.nodes()) {
                if (x.node() <= previous) differences.add(at + ": knoop " + x.node() + " na knoop " + previous);
                previous = x.node();
                byNumber.put(x.node(), x);
                if (x.node() >= form.nodes().size()) {
                    differences.add(at + ": knoop " + x.node() + " voorbij de " + form.nodes().size() + " knopen");
                    continue;
                }
                String kind = p.positions().of(form.nodes().get(x.node())).kind();
                if (!kind.equals(x.kind())) differences.add(at + ": knoop " + x.node() + " is een " + kind + ", niet " + x.kind());
            }
            // the reading holds where the form fired; a displaced default did not fire
            if (!ofStack && !displaced) fired(at, form, r, values, fp, session, byNumber, differences);
            // every operand that reads a member has the value the fixpoint gives it, and names the object it read it on
            for (ConditionReading.NodeReading x : r.nodes()) {
                if (x.node() >= form.nodes().size()) continue;
                Object node = form.nodes().get(x.node());
                boolean member = node instanceof Expr.AttributeReference || node instanceof Condition.CharacteristicCheck;
                if ((!member || !x.reached() || x.fault() != null) && x.object() != null)
                    differences.add(at + ": knoop " + x.node() + " (" + x.kind() + ") noemt het object "
                            + name(label, x.object()) + ", en leest geen lid of vond er niets");
                if (!x.reached() || x.fault() != null) continue;
                if (member)
                    operands.add(() -> {
                        String difference = object(model, p.loaded(), r.object(), node, x.object(), n, label);
                        if (difference != null) differences.add(at + ": knoop " + x.node() + " " + difference);
                    });
                if (node instanceof Expr.AttributeReference a) {
                    operands.add(() -> {
                        String difference = operand(model, p.loaded(), r.object(), a, x.value(), n);
                        if (difference != null) differences.add(at + ": knoop " + x.node() + " " + difference);
                    });
                } else if (node instanceof Condition.CharacteristicCheck k && k.period() == null
                        && !p.loaded().navigationBindings().containsKey(k) && x.value() instanceof Boolean) {
                    // a check that is a value, the true periods of a characteristic on a timeline that a clip takes, has
                    // no verdict
                    boolean holds = derived(fp, r.object(), k.characteristic()) || input(model, r.object(), k.characteristic());
                    if (!Objects.equals(x.value(), holds != k.negation()))
                        differences.add(at + ": knoop " + x.node() + " leest " + x.value() + ", en het fixpunt geeft het"
                                + " kenmerk " + k.characteristic() + " " + holds);
                    n.operands++;
                }
            }
            // the stop of every composite follows from its members' verdicts
            for (ConditionReading.NodeReading x : r.nodes()) {
                if (!x.reached() || x.fault() != null || x.node() >= form.nodes().size()) continue;
                if (!(form.nodes().get(x.node()) instanceof Condition.Compound s)) continue;
                // a composite the evaluation reads over a timeline, the clip of a temporal total, has the periods in
                // which it holds and no verdict: it reads every member over the timeline (ch. 3.6), and has no stop
                if (!(x.value() instanceof Boolean)) continue;
                String difference = stop(s, x, form.nodes(), byNumber);
                if (difference != null) differences.add(at + ": samenstelling " + x.node() + " " + difference);
                n.compounds++;
            }
        }
        // an evaluation fault that stands leaves the beans as they were: the session has no answer to hold the reads to
        if (fp.faults().isEmpty()) {
            DroolsBackend.setDerivedValues(build, session, label::get);
            operands.forEach(Runnable::run);
        }
        // every derived characteristic has a writer that holds
        for (Fixpoint.DerivedCharacteristic k : fp.characteristics()) {
            boolean made = fp.objects().stream().anyMatch(m -> m.object() == k.object());
            boolean decided = decisions.stream().anyMatch(d -> d.object() == k.object() && d.characteristic()
                    && Namen.norm(d.member()).equals(Namen.norm(k.characteristic())) && d.decider() != null
                    && Boolean.TRUE.equals(d.decider().decision()));
            boolean held = readings.stream().anyMatch(r -> r.object() == k.object()
                    && r.status() == ConditionReading.Status.HOLDS && characteristicTarget(document, r.form(), k.characteristic()));
            if (!made && !decided && !held)
                differences.add(which + ": " + name(label, k.object()) + " " + k.characteristic() + " staat afgeleid"
                        + " zonder lezing die geldt");
        }
        // the default that decided holds, and where none decided none holds with a decision
        for (DefaultsDecision d : decisions) {
            String at = which + " " + name(label, d.object()) + " " + d.member();
            if (d.decider() != null) {
                ConditionReading r = byKey.get(d.decider().pointer() + " " + System.identityHashCode(d.object()));
                if (r != null && r.status() != ConditionReading.Status.HOLDS)
                    differences.add(at + ": de beslisser " + d.decider().pointer() + " is " + r.status());
            }
            if (d.decider() == null && d.conflict() == null)
                for (ConditionReading r : readings) {
                    if (r.object() != d.object() || r.status() != ConditionReading.Status.HOLDS) continue;
                    if (!inStack(d, r.form())) continue;
                    Form form = form(document, p, r.form());
                    boolean decides = d.characteristic() || filledValue(r, form);
                    if (decides) differences.add(at + ": niemand besliste, en " + r.form() + " geldt met een besluit");
                }
        }
    }

    /** Null where a displaced reading of the default {@code r} without nodes is the reading of an initialisation, a
     *  default or a rule the fold of ch. 7.10 took in, whose target the input fills on the object: the one default
     *  that has nothing to read where it is displaced (RUNTIME-SEMANTICS ch. 5.12, 7.3); else why it should have been
     *  read. Read before the fixpoint is written back, so that the bean holds the input. */
    private static String unread(JsonNode document, RegelSpraakModel model, ConditionReading r) {
        JsonNode v = document.at(r.form());
        if (!v.path("initialisation").asBoolean(false))
            return "en een default die een hogere verdrong heeft de knopen van zijn voorwaarde en zijn waarde";
        String target = r.form().startsWith("/rules/prioritizedDefaults/")
                ? document.at(r.form().substring(0, r.form().indexOf("/defaults/"))).path("attribute").asText()
                : v.path("target").path("attribute").asText();
        String declared = declaredName(typeOf(model, r.object()), target);
        if (declared == null) return "en de initialisatie schrijft geen attribuut van het object: " + target;
        Object input = Accessors.leesEigenschap(r.object(), Namen.property(declared));
        return ComputeHelpers.isEmpty(input) ? "en de invoer laat het doel " + target + " van de initialisatie leeg"
                : null;
    }

    /** Null where the displaced reading {@code r} of {@code form} has the nodes of a reading at its turn: the root of
     *  its condition reached, and the root of its value reached exactly where the condition holds (a form without a
     *  condition, where its value is its reading); else the difference. */
    private static String readAsAtItsTurn(Form form, ConditionReading r) {
        Map<Integer, ConditionReading.NodeReading> byNumber = new HashMap<>();
        for (ConditionReading.NodeReading x : r.nodes()) byNumber.put(x.node(), x);
        int c = form.conditionNodes();
        boolean holds;
        if (c == 0) holds = true;
        else {
            ConditionReading.NodeReading root = byNumber.get(0);
            if (root == null || !root.reached()) return "las de wortel van zijn voorwaarde niet";
            if (root.fault() != null) holds = false;
            else if (root.value() instanceof Boolean b) holds = b;
            // a condition read over a timeline has periods and no verdict
            else return null;
        }
        if (form.nodes().size() == c) return null;
        ConditionReading.NodeReading value = byNumber.get(c);
        if (value == null) return "mist de wortel van zijn waarde, knoop " + c;
        if (holds && !value.reached()) return "las zijn waarde niet, waar zijn voorwaarde geldt";
        if (!holds && value.reached()) return "las zijn waarde, waar zijn voorwaarde niet geldt";
        return null;
    }

    /** One form of the document: its kind, its name as the writer of its values, the target it writes, and its nodes in
     *  the order of their numbers, those of the condition and then those of the value. */
    private record Form(String kind, String writer, String target, List<Object> nodes, int conditionNodes) {}

    /** The form at {@code pointer}, a version or a default. */
    private static Form form(JsonNode document, IrLoader.Positioned p, String pointer) {
        JsonNode v = document.at(pointer);
        if (v.isMissingNode()) throw new AssertionError("geen vorm op " + pointer);
        String kind, writer = null, target = null, conditionField, valueField = null;
        if (pointer.startsWith("/rules/prioritizedDefaults/")) {
            kind = v.has("decision") ? "default kenmerk" : "default attribuut";
            conditionField = "justification";
            if (!v.has("decision")) valueField = "consequence";
        } else if (pointer.startsWith("/rules/rules/")) {
            boolean attribute = v.has("presentation") || v.path("initialisation").asBoolean(false);
            kind = attribute ? (v.path("initialisation").asBoolean(false) ? "initialisatie" : "regel attribuut")
                    : "regel kenmerk";
            writer = formName(document, pointer);
            target = v.path("target").path("attribute").asText();
            conditionField = "condition";
            if (attribute) valueField = "value";
        } else if (pointer.startsWith("/rules/consistencies/")) {
            kind = "consistentie";
            writer = formName(document, pointer);
            conditionField = "assertion";
        } else if (pointer.startsWith("/rules/objectCreations/")) {
            kind = "objectcreatie";
            writer = formName(document, pointer);
            conditionField = "condition";
        } else if (pointer.startsWith("/rules/factCreations/")) {
            kind = "feitcreatie";
            conditionField = "condition";
        } else if (pointer.startsWith("/rules/allocations/")) {
            kind = "verdeling";
            conditionField = "condition";
        } else throw new AssertionError("geen vorm met een voorwaarde op " + pointer);
        NodePositions positions = p.positions();
        Object root = positions.at(pointer + "/" + conditionField);
        Object value = valueField == null ? null : positions.at(pointer + "/" + valueField);
        // a form without a condition is read where it computes a value, its nodes those of the value from 0 (card
        // readings-of-unconditional-forms)
        if (root == null && value == null)
            throw new AssertionError("geen voorwaarde op " + pointer + "/" + conditionField + ", en geen waarde");
        List<Object> nodes = new ArrayList<>(root == null ? List.of() : positions.subtree(root));
        int conditionNodes = nodes.size();
        if (value != null) nodes.addAll(positions.subtree(value));
        return new Form(kind, writer, target, nodes, conditionNodes);
    }

    /** The name of the form of the version at {@code pointer} as its records carry it: the form's name, and with two
     *  versions or more "{@code <name> #i}" (IR-SPEC ch. 3, Versioning). */
    private static String formName(JsonNode document, String pointer) {
        String[] parts = pointer.split("/");
        JsonNode form = document.at("/" + parts[1] + "/" + parts[2] + "/" + parts[3]);
        int version = Integer.parseInt(parts[5]);
        String name = form.path("name").asText();
        return form.path("versions").size() >= 2 ? name + " #" + (version + 1) : name;
    }

    /** Whether the form at {@code pointer} is a rule that attributes {@code characteristic}. */
    private static boolean characteristicTarget(JsonNode document, String pointer, String characteristic) {
        if (!pointer.startsWith("/rules/rules/")) return false;
        JsonNode v = document.at(pointer);
        return !v.has("presentation") && !v.path("initialisation").asBoolean(false)
                && Namen.norm(v.path("target").path("attribute").asText()).equals(Namen.norm(characteristic));
    }

    /** Whether the reading of {@code r} holds where its form fired, by the form's kind. */
    private static void fired(String at, Form form, ConditionReading r, List<Value> values, Fixpoint fp, KieSession session,
                              Map<Integer, ConditionReading.NodeReading> byNumber, List<String> differences) {
        boolean holds = r.status() == ConditionReading.Status.HOLDS;
        switch (form.kind()) {
            case "regel kenmerk" -> {
                if (holds && !derived(fp, r.object(), form.target()))
                    differences.add(at + ": geldt, en " + form.target() + " staat niet afgeleid");
            }
            case "regel attribuut", "initialisatie" -> {
                boolean written = values.stream().anyMatch(w -> w.subject() == r.object() && w.writer().equals(form.writer()));
                ConditionReading.NodeReading root = byNumber.get(form.conditionNodes());
                boolean filled = root != null && root.reached() && root.fault() == null
                        && !ComputeHelpers.isEmpty(root.value());
                if (holds && filled && !written) differences.add(at + ": geldt met een waarde, en schreef niets");
                if (!holds && written) differences.add(at + ": " + r.status() + ", en schreef een waarde");
            }
            case "consistentie" -> {
                boolean violated = fp.inconsistencies().stream().anyMatch(i ->
                        form.writer().equals(RegelSpraakRuntime.get(i, "rule")) && RegelSpraakRuntime.get(i, "subject") == r.object()
                                && ((List<?>) RegelSpraakRuntime.get(i, "codes")).contains("EF-025"));
                if (holds == violated) differences.add(at + ": " + r.status() + ", en de schending staat "
                        + (violated ? "wel" : "niet"));
            }
            case "objectcreatie" -> {
                boolean made = fp.objects().stream().anyMatch(m -> m.subject() == r.object()
                        && m.creation().equals(form.writer()));
                if (holds != made) differences.add(at + ": " + r.status() + ", en de creatie maakte " + (made ? "wel" : "niets"));
            }
            default -> { }
        }
    }

    /** Whether the value of the form of {@code r} is filled, where it has one. */
    private static boolean filledValue(ConditionReading r, Form form) {
        for (ConditionReading.NodeReading x : r.nodes())
            if (x.node() == form.conditionNodes()) return x.reached() && x.fault() == null && !ComputeHelpers.isEmpty(x.value());
        return false;
    }

    /** Whether {@code pointer} is a default of the stack of {@code d}: a default of its root chains or below one. */
    private static boolean inStack(DefaultsDecision d, String pointer) {
        for (List<String> chain : d.rootChains())
            for (String x : chain)
                if (pointer.equals(x) || pointer.startsWith(x + "/exceptions/")) return true;
        return false;
    }

    /** Null where {@code named}, the object the node {@code node} of a reading on {@code subject} names, an attribute
     *  read or a characteristic check, is the object its walk reaches on the beans once the fixpoint is written back
     *  ({@code DroolsBackend.reached}), by identity; else the difference. A walk the branch refuses is not held. */
    private static String object(RegelSpraakModel model, IrLoader.Loaded loaded, Object subject, Object node,
                                 Object named, Counts n, Map<Object, String> label) {
        ObjectType type = typeOf(model, subject);
        List<RoleBindingRef> walk =
                loaded.navigationBindings().getOrDefault(node, List.of());
        Object reached;
        try {
            reached = node instanceof Expr.AttributeReference a ? DroolsBackend.reached(model, subject, type, a, walk)
                    : DroolsBackend.reached(model, subject, type, (Condition.CharacteristicCheck) node, walk);
        } catch (CaseRunner.Excluded e) {
            return null;
        }
        n.objects++;
        if (!walk.isEmpty()) {
            n.navigated++;
            if (reached == null) n.unlinked++;
        }
        return named == reached ? null : "noemt het object " + (named == null ? "geen" : name(label, named))
                + ", en de wandeling bereikt " + (reached == null ? "geen" : name(label, reached));
    }

    /** Null where the value {@code read} of the read {@code a} on {@code subject} is the one the fixpoint gives the member
     *  on the object the read reaches, on the beans once the fixpoint is written back: the derived value, or else the
     *  input; else the difference. A read the walk refuses, over a to-many role, and a timeline are not held. */
    private static String operand(RegelSpraakModel model, IrLoader.Loaded loaded, Object subject,
                                  Expr.AttributeReference a, Object read, Counts n) {
        ObjectType type = typeOf(model, subject);
        Object reached;
        try {
            reached = DroolsBackend.reached(model, subject, type, a, loaded.navigationBindings().getOrDefault(a, List.of()));
        } catch (CaseRunner.Excluded e) {
            return null;
        }
        if (read instanceof List<?> || read instanceof java.util.Map<?, ?>
                || read != null && read.getClass().getSimpleName().equals("BucketScalar")) return null;
        Object expected;
        if (reached == null) expected = null;
        else {
            String declared = declaredName(typeOf(model, reached), a.attribute());
            if (declared == null) return null;
            expected = Accessors.leesEigenschap(reached, Namen.property(declared));
        }
        if (expected instanceof List<?>) return null;
        n.operands++;
        boolean equal = read instanceof BigDecimal x && expected instanceof BigDecimal y ? x.compareTo(y) == 0
                : Objects.equals(read, expected);
        return equal ? null : "leest " + read + ", en het fixpunt geeft " + a.attribute() + " " + expected;
    }

    /** Whether the stop of composite {@code s}, read as {@code x}, follows from its members' verdicts. */
    private static String stop(Condition.Compound s, ConditionReading.NodeReading x, List<Object> nodes,
                               Map<Integer, ConditionReading.NodeReading> byNumber) {
        int touch = 0, m = s.components().size();
        Boolean verdict = null;
        for (int i = 0; i < m; i++) {
            int number = indexOf(nodes, s.components().get(i));
            ConditionReading.NodeReading member = byNumber.get(number);
            if (member == null) return "mist lid " + number;
            Boolean fixed = verdict != null ? verdict
                    : ComputeHelpers.established(s.quantifier(), s.threshold(), touch, i, m - i);
            if (fixed != null) {
                verdict = fixed;
                if (member.reached()) return "bereikte lid " + number + " na de stop";
                continue;
            }
            if (!member.reached()) return "bereikte lid " + number + " niet, waar de stop nog niet vaststond";
            if (member.fault() != null) return null;
            if (Boolean.TRUE.equals(member.value())) touch++;
        }
        if (verdict == null) verdict = ComputeHelpers.endVerdict(s.quantifier(), s.threshold(), touch, m);
        return verdict.equals(x.value()) ? null : "geeft " + x.value() + ", en de stopregel " + verdict;
    }

    private static int indexOf(List<Object> nodes, Object node) {
        for (int i = 0; i < nodes.size(); i++) if (nodes.get(i) == node) return i;
        return -1;
    }

    private static boolean derived(Fixpoint fp, Object o, String characteristic) {
        return fp.characteristics().stream().anyMatch(k -> k.object() == o
                && Namen.norm(k.characteristic()).equals(Namen.norm(characteristic)));
    }

    /** Whether {@code o} carries {@code characteristic} in its input, on its bean. */
    private static boolean input(RegelSpraakModel model, Object o, String characteristic) {
        ObjectType t = typeOf(model, o);
        for (ObjectType x : t.chain())
            for (String k : x.characteristics)
                if (Namen.norm(k).equals(Namen.norm(characteristic)))
                    return Boolean.TRUE.equals(Accessors.leesEigenschap(o, Namen.property(k)));
        return false;
    }

    private static String declaredName(ObjectType t, String attribute) {
        for (ObjectType x : t.chain())
            for (String a : x.attributes.keySet())
                if (Namen.norm(a).equals(Namen.norm(attribute))) return a;
        return null;
    }

    private static ObjectType typeOf(RegelSpraakModel model, Object bean) {
        for (ObjectType t : model.objectTypes.values())
            if (Namen.ident(t.name, true).equals(bean.getClass().getSimpleName())) return t;
        throw new AssertionError("geen objecttype voor " + bean.getClass().getName());
    }

    private static String name(Map<Object, String> label, Object o) {
        String l = label.get(o);
        return l != null ? l : "een aangemaakte " + o.getClass().getSimpleName();
    }

    private static List<Path> casePaths(Path map) throws Exception {
        try (Stream<Path> s = Files.list(map)) {
            return s.filter(p -> p.getFileName().toString().matches("case.*\\.json")).sorted().toList();
        }
    }
}
