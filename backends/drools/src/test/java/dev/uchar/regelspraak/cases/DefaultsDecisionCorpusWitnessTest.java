package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;

import dev.uchar.regelspraak.model.RegelSpraakModel;
import dev.uchar.regelspraak.model.RegelSpraakModel.ObjectType;
import dev.uchar.regelspraak.schema.FactModelGenerator;
import dev.uchar.regelspraak.transpiler.core.Namen;
import dev.uchar.regelspraak.transpiler.tms.DefaultsDecision;
import dev.uchar.regelspraak.transpiler.tms.Fixpoint;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@code DroolsBackend.decisionsOf} against {@code DroolsBackend.fixpointOf} over the corpus (card defaults-decider,
 * acceptance (b)), beside {@link BranchDumpsCorpusWitnessTest}: every case of every golden with a defaults group that
 * the exec-model branch plays, fired on both branches, and per case, effective stack and object of the stack's type,
 * the input's beans and the objects the creations made, the entry gives exactly one record (card
 * undecided-stack-record), which must agree with what the fixpoint holds:
 *
 * <ul>
 *   <li>an attribution with the characteristic derived, and a do-not-attribute decision with none;</li>
 *   <li>an attribute's decision with its derived value, a number compared by {@code compareTo};</li>
 *   <li>a record with neither a decider nor a conflict, no default deciding, with nothing derived on the target, which
 *       keeps its input;</li>
 *   <li>a conflict with nothing derived and the §9.5 deposit of its key on the object; and every deposit of a defaults
 *       conflict (EF-027) with the conflict of a record.</li>
 * </ul>
 *
 * Every pointer of a record, the root chains of a record on which no default decided included, resolves in the
 * golden's document to a node that writes the record's target: a
 * {@code Default} of a group on that member, or the version of a rule whose target it is; and the decider lies on or
 * below a default of the root chains. The toolchain lays these pointers back on its model, so a pointer that drifts
 * fails here and not in its explanation.
 *
 * A record on an object and member that is no stack's fails too. The two branches render one analysis, so they must
 * check the same count.
 *
 * <p>Floors, measured 2026-10-21: {@value #EXPECTED_MIN_CHECKED} (case, stack, object) checked per branch, over
 * {@value #EXPECTED_MIN_GOLDENS} goldens, {@value #EXPECTED_MIN_UNDECIDED} of them on which no default decided, and
 * {@value #EXPECTED_MIN_POINTERS} pointers resolved; a smaller count is a corpus that lost defaults goldens or cases, or
 * a selection that silently stopped checking. Beside them at least one rule-version pointer and one (stack, object)
 * on a made object.
 */
class DefaultsDecisionCorpusWitnessTest {

    // cwd under surefire is the module dir (backends/drools); the corpus is two levels up
    private static final Path GOLDEN = Path.of("..", "..", "corpus", "golden");

    /** The (case, stack, object) checked per branch, measured 2026-10-21 at card defaults-decider and again at card
     *  undecided-stack-record: 48 attributions, 19 do-not-attribute decisions, 194 attribute decisions, 12 conflicts
     *  and 51 on which no default decided, which until the second card had no record; 4 of the 324 on an object a
     *  creation made. */
    static final int EXPECTED_MIN_CHECKED = 324;
    /** The goldens with a defaults group whose cases are checked, measured 2026-10-21 at both cards. */
    static final int EXPECTED_MIN_GOLDENS = 42;
    /** The records on which no default decided, on the exec-model branch, measured 2026-10-21 at card
     *  undecided-stack-record. */
    static final int EXPECTED_MIN_UNDECIDED = 51;
    /** The pointers resolved in the documents, on the exec-model branch, measured 2026-10-21 at card
     *  undecided-stack-record: 996 to a {@code Default}, 21 to a rule version, the root chains of the records on which
     *  no default decided among them. */
    static final int EXPECTED_MIN_POINTERS = 1017;

    private static final Pattern GROUP = Pattern.compile("/rules/prioritizedDefaults/[0-9]+");
    private static final Pattern RULE_VERSION = Pattern.compile("/rules/rules/[0-9]+/versions/[0-9]+");

    /** One target of a defaults group as the document declares it: the group's subject, the member and its kind. */
    private record Target(String subject, String member, boolean characteristic) {}

    @Test void every_record_agrees_with_the_fixpoint() throws Exception {
        List<Path> goldens;
        try (Stream<Path> s = Files.walk(GOLDEN)) {
            goldens = s.filter(p -> p.getFileName().toString().equals("ir.json")).map(Path::getParent).sorted().toList();
        }
        List<String> differences = new ArrayList<>();
        Map<DroolsBackend.Branch, int[]> checked = new TreeMap<>();
        Map<String, Integer> kinds = new TreeMap<>();
        int compared = 0;
        for (Path g : goldens) {
            JsonNode ir = TmsDocuments.JSON.readTree(g.resolve("ir.json").toFile());
            List<Target> targets = new ArrayList<>();
            for (JsonNode p : ir.path("rules").path("prioritizedDefaults"))
                targets.add(p.has("characteristic")
                        ? new Target(p.get("subject").asText(), p.get("characteristic").asText(), true)
                        : new Target(p.get("subject").asText(), p.get("attribute").asText(), false));
            if (targets.isEmpty()) continue;
            List<Path> casePaths = new ArrayList<>();
            try (Stream<Path> s = Files.list(g)) {
                for (Path casePath : s.filter(p -> p.getFileName().toString().matches("case.*\\.json")).sorted().toList())
                    if (!TmsDocuments.JSON.readTree(casePath.toFile()).has("expectedFault")) casePaths.add(casePath);
            }
            if (casePaths.isEmpty()) continue;
            if (!CaseRunner.viaExecModel(g.resolve("ir.json"), casePaths).status().equals("PLAYED")) continue;
            compared++;
            String name = GOLDEN.relativize(g).toString();
            for (DroolsBackend.Branch branch : TmsDocuments.BRANCHES) {
                CaseRunner runner = CaseRunner.forMeasurement(g.resolve("ir.json"), branch);
                for (Path casePath : casePaths) {
                    String which = branch + " " + name + "/" + casePath.getFileName();
                    JsonNode c = TmsDocuments.JSON.readTree(casePath.toFile());
                    int n = runner.withSession(c, (build, session, labels) -> {
                        DroolsBackend.fire(build, session);
                        return check(which, ir, build, targets, labels, DroolsBackend.fixpointOf(build, session),
                                DroolsBackend.decisionsOf(build, session), differences,
                                branch == DroolsBackend.Branch.EXEC_MODEL ? kinds : new TreeMap<>());
                    });
                    checked.computeIfAbsent(branch, b -> new int[1])[0] += n;
                }
            }
        }
        assertEquals(List.of(), differences, "beslissingen die niet met het fixpunt overeenkomen");
        int exec = checked.get(DroolsBackend.Branch.EXEC_MODEL)[0], drl = checked.get(DroolsBackend.Branch.DRL_TEXT)[0];
        assertEquals(exec, drl, "de twee takken toetsen even veel (case, stapel, object)");
        int goldenCount = compared;
        assertTrue(exec >= EXPECTED_MIN_CHECKED, () -> "getoetst " + exec + " (case, stapel, object) in " + goldenCount
                + " goldens, " + kinds + ", onder de vloer " + EXPECTED_MIN_CHECKED);
        assertTrue(goldenCount >= EXPECTED_MIN_GOLDENS, () -> "getoetste goldens " + goldenCount + ", onder de vloer "
                + EXPECTED_MIN_GOLDENS);
        int undecided = kinds.getOrDefault("niemand besliste", 0);
        assertTrue(undecided >= EXPECTED_MIN_UNDECIDED, () -> "records waarop niemand besliste " + undecided + ", "
                + kinds + ", onder de vloer " + EXPECTED_MIN_UNDECIDED);
        int pointers = kinds.getOrDefault("defaultpointer", 0) + kinds.getOrDefault("regelpointer", 0);
        assertTrue(pointers >= EXPECTED_MIN_POINTERS, () -> "opgeloste pointers " + pointers + ", " + kinds
                + ", onder de vloer " + EXPECTED_MIN_POINTERS);
        // the corpus has goldens whose subtype stack takes in an ordinary rule (5 at the card): its pointer must be read
        assertTrue(kinds.getOrDefault("regelpointer", 0) > 0, () -> "geen pointer naar een regelversie opgelost: " + kinds);
        // and goldens whose creations make an object with a stack of its type: a made object has its records too
        assertTrue(kinds.getOrDefault("op een aangemaakt object", 0) > 0, () -> "geen (case, stapel, object) op een"
                + " aangemaakt object getoetst: " + kinds);
    }

    /** The checks of one case: per object of the session and target of a group its type is an instance of. Returns the
     *  number of (stack, object) checked. */
    private static int check(String which, JsonNode ir, DroolsBackend.Build build, List<Target> targets,
                             Map<String, Object> labels,
                             Fixpoint fixpoint, List<DefaultsDecision> decisions, List<String> differences,
                             Map<String, Integer> kinds) {
        RegelSpraakModel model = build.model();
        Map<Object, String> label = new IdentityHashMap<>();
        labels.forEach((l, o) -> label.put(o, l));
        Set<Object> objects = Collections.newSetFromMap(new IdentityHashMap<>());
        objects.addAll(labels.values());
        for (Fixpoint.MadeObject m : fixpoint.objects()) objects.add(m.object());
        List<DefaultsDecision> unmatched = new ArrayList<>(decisions);
        int n = 0;
        for (Object o : objects) {
            ObjectType type = typeOf(model, o);
            Set<String> seen = new LinkedHashSet<>();
            for (Target t : targets) {
                ObjectType subject = model.objectTypeByName(t.subject());
                if (subject == null || !type.isA(subject)) continue;
                if (!seen.add((t.characteristic() ? "K:" : "A:") + Namen.norm(t.member()))) continue;
                n++;
                String where = which + " " + name(label, o) + " " + t.member();
                DefaultsDecision r = null;
                for (DefaultsDecision d : decisions)
                    if (d.object() == o && d.characteristic() == t.characteristic()
                            && Namen.norm(d.member()).equals(Namen.norm(t.member()))) {
                        if (r != null) differences.add(where + ": twee records, " + r + " en " + d);
                        r = d;
                    }
                unmatched.remove(r);
                String kind = verify(where, t, o, r, fixpoint, differences);
                kinds.merge(kind, 1, Integer::sum);
                if (!label.containsKey(o)) kinds.merge("op een aangemaakt object", 1, Integer::sum);
            }
        }
        for (DefaultsDecision d : unmatched)
            if (d != null) differences.add(which + ": een record op geen stapel: " + d);
        for (DefaultsDecision d : decisions) resolve(which + " " + name(label, d.object()) + " " + d.member(), ir, d,
                differences, kinds);
        // every deposit of a defaults conflict is the conflict of a record
        for (Object i : fixpoint.inconsistencies()) {
            @SuppressWarnings("unchecked")
            List<String> codes = (List<String>) getter(i, "getCodes");
            if (!codes.contains("EF-027")) continue;
            Object subject = getter(i, "getSubject");
            String rule = (String) getter(i, "getRule");
            boolean matched = decisions.stream().anyMatch(d -> d.object() == subject && d.conflict() != null
                    && d.conflict().key().equals(rule));
            if (!matched) differences.add(which + " " + name(label, subject) + ": het §9.5-feit " + rule
                    + " zonder het conflict van een record");
        }
        return n;
    }

    /** The agreement of record {@code r} (null: none) with the fixpoint on target {@code t} of {@code o}; returns the
     *  kind of the record, for the report. */
    private static String verify(String where, Target t, Object o, DefaultsDecision r, Fixpoint fixpoint,
                                 List<String> differences) {
        boolean derivedCharacteristic = fixpoint.characteristics().stream().anyMatch(h -> h.object() == o
                && Namen.norm(h.characteristic()).equals(Namen.norm(t.member())));
        Fixpoint.DerivedValue derivedValue = fixpoint.values().stream().filter(v -> v.object() == o
                && Namen.norm(v.attribute()).equals(Namen.norm(t.member()))).findFirst().orElse(null);
        boolean derived = t.characteristic() ? derivedCharacteristic : derivedValue != null;
        if (r == null) {
            differences.add(where + ": geen record, waar elk object per stapel van zijn type er één heeft");
            return "geen record";
        }
        if (r.decider() == null && r.conflict() == null) {
            if (derived) differences.add(where + ": niemand besliste, en het fixpunt leidt het doel af: " + r);
            return "niemand besliste";
        }
        if (r.conflict() != null) {
            if (derived) differences.add(where + ": een conflict, en het fixpunt leidt het doel af: " + r);
            boolean deposited = fixpoint.inconsistencies().stream().anyMatch(i -> getter(i, "getSubject") == o
                    && r.conflict().key().equals(getter(i, "getRule")));
            if (!deposited) differences.add(where + ": een conflict zonder het §9.5-feit " + r.conflict().key());
            return "conflict";
        }
        Object decision = r.decider().decision();
        if (t.characteristic()) {
            if (!(decision instanceof Boolean b)) {
                differences.add(where + ": een kenmerkbesluit dat geen waarheidswaarde is: " + r);
                return "fout";
            }
            if (b != derivedCharacteristic)
                differences.add(where + ": besluit " + b + ", en het fixpunt " + (derivedCharacteristic ? "leidt het"
                        + " kenmerk af" : "leidt het kenmerk niet af"));
            return b ? "toekennen" : "niet toekennen";
        }
        if (derivedValue == null) differences.add(where + ": besluit " + decision + ", en het fixpunt leidt niets af");
        else if (!sameValue(decision, derivedValue.value()))
            differences.add(where + ": besluit " + decision + ", en het fixpunt leidt " + derivedValue.value() + " af");
        return "waarde";
    }

    /** Every pointer of {@code d} names a node of {@code ir} that writes the target of {@code d}, and its decider lies on
     *  or below a default of its root chains. */
    private static void resolve(String where, JsonNode ir, DefaultsDecision d, List<String> differences,
                                Map<String, Integer> kinds) {
        List<String> roots = d.rootChains().stream().flatMap(List::stream).toList();
        List<String> pointers = new ArrayList<>(roots);
        if (d.decider() != null) pointers.add(d.decider().pointer());
        if (d.conflict() != null) {
            if (d.conflict().pointer() != null) pointers.add(d.conflict().pointer());
            for (DefaultsDecision.Decider x : d.conflict().deciders()) pointers.add(x.pointer());
        }
        for (String p : pointers) {
            JsonNode node = ir.at(p);
            Matcher group = GROUP.matcher(p);
            String target;
            if (group.lookingAt()) {
                if (!node.path("node").asText().equals("Default")) {
                    differences.add(where + ": pointer " + p + " noemt geen Default maar " + node);
                    continue;
                }
                target = ir.at(group.group()).path(d.characteristic() ? "characteristic" : "attribute").asText(null);
                kinds.merge("defaultpointer", 1, Integer::sum);
            } else if (RULE_VERSION.matcher(p).matches()) {
                target = node.path("target").path("attribute").asText(null);
                kinds.merge("regelpointer", 1, Integer::sum);
            } else {
                differences.add(where + ": een pointer van onbekende vorm: " + p);
                continue;
            }
            if (target == null || !Namen.norm(target).equals(Namen.norm(d.member())))
                differences.add(where + ": pointer " + p + " noemt een knooppunt met doel " + target + ", niet "
                        + d.member());
        }
        List<String> deciders = new ArrayList<>();
        if (d.decider() != null) deciders.add(d.decider().pointer());
        if (d.conflict() != null) d.conflict().deciders().forEach(x -> deciders.add(x.pointer()));
        for (String p : deciders)
            if (roots.stream().noneMatch(r -> p.equals(r) || p.startsWith(r + "/")))
                differences.add(where + ": beslisser " + p + " ligt op of onder geen default van " + d.rootChains());
    }

    private static boolean sameValue(Object a, Object b) {
        if (a instanceof BigDecimal x && b instanceof BigDecimal y) return x.compareTo(y) == 0;
        return Objects.equals(a, b);
    }

    /** The object type whose bean class {@code o} is. */
    private static ObjectType typeOf(RegelSpraakModel model, Object o) {
        for (ObjectType t : model.objectTypes.values())
            if (o.getClass().getName().equals(FactModelGenerator.PACKAGE + "." + Namen.ident(t.name, true))) return t;
        throw new AssertionError("geen objecttype voor " + o.getClass().getName());
    }

    private static String name(Map<Object, String> label, Object o) {
        String l = label.get(o);
        return l != null ? l : "een aangemaakte " + o.getClass().getSimpleName();
    }

    private static Object getter(Object o, String getter) {
        try {
            return o.getClass().getMethod(getter).invoke(o);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(getter + " op " + o, e);
        }
    }
}
