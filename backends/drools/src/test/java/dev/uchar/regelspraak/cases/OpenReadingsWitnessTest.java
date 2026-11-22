package dev.uchar.regelspraak.cases;

import dev.uchar.regelspraak.refusal.DroolsCode;
import dev.uchar.regelspraak.transpiler.tms.ConditionReading;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.TreeSet;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The readings and the end state of a document that infers nothing from an empty value (RUNTIME-SEMANTICS ch. 5.12, "Open
 * values", and 5.12a, "open members"), on both branches. The pins {@code src/test/resources/open-readings/<example>.<case>.trace}
 * are the lines {@code runner --trace} of the reference interpreter prints for a case of a document of
 * {@code open-values/} or of a golden, and {@code open-readings/<document>/} holds a document with its cases and their pins.
 * A test is named after its example of ch. 5.12 or 5.12a; the two this backend refuses have a test of the refusal. The
 * position of a contributor is compared where the branch writes one, as gate 17 compares it.
 */
class OpenReadingsWitnessTest {

    private static final Path PINS = Path.of("src", "test", "resources", "open-readings");
    private static final Path VALUES = Path.of("src", "test", "resources", "open-values");
    private static final Path GOLDEN = Path.of("..", "..", "corpus", "golden");

    /** WV-5.12-afleiden-uit-leeg and WV-5.12a-afleiden-uit-leeg, every case of the golden. */
    @Test void wv_5_12_afleiden_uit_leeg() throws Exception {
        pinned("afleiden-uit-leeg", GOLDEN.resolve("jutterswet-afleiden-uit-leeg"));
        for (DroolsBackend.Branch branch : DroolsBackend.Branch.values()) {
            Path golden = GOLDEN.resolve("jutterswet-afleiden-uit-leeg");
            List<String> case2 = lines(golden, golden.resolve("case2-taxatie-open.json"), branch);
            for (String line : List.of(
                    "/rules/rules/0/versions/0 \"V1\" 0 Comparison undetermined open \"V1\" \"taxatie\"",
                    "/rules/rules/0/versions/0 \"V1\" 1 AttributeReference undetermined open \"V1\" \"taxatie\"",
                    "/rules/rules/0/versions/0 \"V1\" 2 Literal unreached",
                    "/rules/rules/0/versions/0 \"V1\" OPEN",
                    "/rules/rules/2/versions/0 \"V1\" 0 CharacteristicCheck undetermined open \"V1\" \"kostbaar\"",
                    "/rules/rules/2/versions/0 \"V1\" OPEN",
                    "/rules/rules/3/versions/0 \"V1\" 0 Predicate true closure",
                    "/rules/rules/3/versions/0 \"V1\" HOLDS"))
                assertTrue(case2.contains("reading " + line), () -> branch + ": " + line);
        }
    }

    /** WV-5.12-open-composite: members after one that is undetermined, decided by one and by none; and the assertion of a
     *  consistency that is undetermined. */
    @Test void wv_5_12_open_composite() throws Exception {
        pinned("open-composite", VALUES.resolve("composite-tables"));
        pinned("open-consistency", VALUES.resolve("consistency"));
    }

    /** WV-5.12-open-past-undetermined: a member read provisionally that faults. */
    @Test void wv_5_12_open_past_undetermined() throws Exception {
        pinned("open-past-undetermined", VALUES.resolve("past-undetermined-fault"));
    }

    /** WV-5.12-open-quantifier-fold, without a filter, and in the order of its members where the matrix can deposit. */
    @Test void wv_5_12_open_quantifier_fold() throws Exception {
        pinned("open-quantifier-fold", VALUES.resolve("quantifier-fold"));
        pinned("open-quantifier-fold-in-order", VALUES.resolve("past-undetermined-fold"));
    }

    /** WV-5.12-open-fold-over-a-set: the first member in the order of the fold, every filter before every value, selection by
     *  selection in a union, and in a fold nested in a filter; no contributors where it is broken off. */
    @Test void wv_5_12_open_fold_over_a_set() throws Exception {
        pinned("open-fold-over-a-set", VALUES.resolve("set-fold"));
        pinned("open-filter-faults", VALUES.resolve("fold-over-a-union"));
        pinned("open-nested-fold", VALUES.resolve("fold-in-a-filter"));
        pinned("open-fold-in-a-binder", VALUES.resolve("fold-in-a-binder"));
        documents("reading-nested");
        documents("reading-diamond");
    }

    /** WV-5.12-open-stack: below an open default the initialisation at its turn, the lower of two as well, every other default
     *  displaced and read; the content of a default whose exception groups are open. */
    @Test void wv_5_12_open_stack() throws Exception {
        pinned("open-stack", VALUES.resolve("stack-opgave"));
        pinned("open-initialisation", VALUES.resolve("stack-two-initialisations"));
        pinned("open-exception-groups", VALUES.resolve("stack-exception-groups"));
    }

    /** WV-5.12-open-path: a read over a path names the object it reached open. */
    @Test void wv_5_12_open_path() throws Exception {
        pinned("open-path", VALUES.resolve("link"));
    }

    /** Composites and quantifier folds in one another: a quantifier fold that closes on its first member and is undetermined on
     *  a later, a member after it that faults, the form's own negation of a leeg test, a fault within a nested composite. */
    @Test void composites_over_open_values() throws Exception {
        documents("reading-composite");
    }

    /** WV-5.12-open-creation and WV-5.12a-open-link: a creation is refused, with readings as without. */
    @Test void wv_5_12_open_creation_is_refused() {
        Path ir = Path.of("..", "..", "ref", "test", "fixtures", "strict-empty", "object-creation", "ir.json");
        for (DroolsBackend.Branch branch : DroolsBackend.Branch.values())
            assertEquals(DroolsCode.INFER_FROM_EMPTY_FALSE.reason("form", "object-creation").text(),
                    assertThrows(CaseRunner.Excluded.class, () -> DroolsBackend.build(ir, branch, true)).getMessage());
    }

    /** A fold that can be open over a relation both of whose roles are to-many: which member it names cannot be told, so the
     *  build with readings refuses it, and the one without plays it. */
    @Test void a_many_to_many_fold_is_refused_with_readings() throws Exception {
        var ir = (com.fasterxml.jackson.databind.node.ObjectNode) TmsDocuments.JSON.readTree(
                GOLDEN.resolve("jutterswet-muntketen").resolve("ir.json").toFile());
        ir.put("inferFromEmpty", false);
        for (DroolsBackend.Branch branch : DroolsBackend.Branch.values()) {
            DroolsBackend.build(ir, branch, false);
            assertEquals(DroolsCode.MANY_TO_MANY_FOLD_ORDER,
                    assertThrows(CaseRunner.Excluded.class, () -> DroolsBackend.build(ir, branch, true)).code());
        }
    }

    /** WV-5.12-open-status: a rule is open exactly where the session left its target open; and a status, a judgement and a
     *  value are told apart in the records: an undetermined node has no value, a closure has a truth. */
    @Test void wv_5_12_open_status() throws Exception {
        int open = 0, closures = 0;
        for (String document : List.of("derived", "filled-test", "set-fold", "closure-in-a-value", "past-undetermined-fault"))
            for (Path c : cases(VALUES.resolve(document))) {
                CaseRunner runner = CaseRunner.reader(DroolsBackend.build(VALUES.resolve(document).resolve("ir.json"),
                        DroolsBackend.Branch.EXEC_MODEL, true));
                int[] counted = runner.withSession(TmsDocuments.JSON.readTree(c.toFile()), (build, session, labels) -> {
                    DroolsBackend.fire(build, session);
                    int[] n = new int[2];
                    for (ConditionReading r : DroolsBackend.readingsOf(build, session)) {
                        boolean left = session.getObjects(f -> f instanceof dev.uchar.regelspraak.transpiler.tms.LeftOpen l
                                && l.subject() == r.object() && r.form().equals(formOf(build, l.writer()))).iterator().hasNext();
                        if (r.form().startsWith("/rules/rules/")) assertEquals(left, r.status() == ConditionReading.Status.OPEN,
                                document + " " + r.form());
                        if (r.status() == ConditionReading.Status.OPEN) n[0]++;
                        for (ConditionReading.NodeReading x : r.nodes()) {
                            assertFalse(x.undetermined() && (x.value() != null || x.object() != null));
                            if (x.closure()) n[1]++;
                        }
                    }
                    return n;
                });
                open += counted[0];
                closures += counted[1];
            }
        assertTrue(open > 0 && closures > 0, open + " open, " + closures + " closures");
    }

    /** The pointer of the first version of the rule named {@code writer}. */
    private static String formOf(DroolsBackend.Build build, String writer) {
        for (int k = 0; k < build.model().resolvedRules.size(); k++)
            if (writer.equals(build.model().resolvedRules.get(k).name())) return "/rules/rules/" + k + "/versions/0";
        return null;
    }

    /** Every pin {@code <example>.<case>.trace} against the case of the document in {@code document}. */
    private static void pinned(String example, Path document) throws Exception {
        int n = 0;
        try (Stream<Path> files = Files.list(PINS)) {
            for (Path pin : files.filter(p -> p.getFileName().toString().startsWith(example + ".")).sorted().toList()) {
                String name = pin.getFileName().toString();
                compare(document, document.resolve(name.substring(example.length() + 1, name.length() - ".trace".length()) + ".json"),
                        pin);
                n++;
            }
        }
        assertTrue(n > 0, example + " has no pin");
    }

    /** Every case of the document {@code open-readings/<name>} against its pin. */
    private static void documents(String name) throws Exception {
        List<Path> cases = cases(PINS.resolve(name));
        for (Path c : cases)
            compare(PINS.resolve(name), c, PINS.resolve(name).resolve(c.getFileName().toString().replace(".json", ".trace")));
        assertFalse(cases.isEmpty(), name);
    }

    private static void compare(Path document, Path c, Path pin) throws Exception {
        for (DroolsBackend.Branch branch : DroolsBackend.Branch.values()) {
            List<String> expected = Files.readAllLines(pin), mine = lines(document, c, branch);
            Collections.sort(expected);
            List<String> differences = new ArrayList<>();
            TreeSet<String> left = new TreeSet<>(expected);
            for (String line : mine)
                if (!left.remove(line) && !(line.contains(" unordered")
                        && left.removeIf(e -> withoutPositions(e).equals(withoutPositions(line))))) differences.add("+ " + line);
            for (String line : left) differences.add("- " + line);
            assertEquals(List.of(), differences, branch + " " + pin);
        }
    }

    private static String withoutPositions(String line) {
        return line.replaceAll(" (reached|unreached|unordered)(?=[;\\]])", "");
    }

    /** The lines of the readings and the end state of case {@code c}. */
    private static List<String> lines(Path document, Path c, DroolsBackend.Branch branch) throws Exception {
        List<String> out = new ArrayList<>(CaseRunner.readings(document.resolve("ir.json"), List.of(c), branch));
        out.removeIf(l -> l.startsWith("case "));
        return out;
    }

    private static List<Path> cases(Path document) throws Exception {
        try (Stream<Path> files = Files.list(document)) {
            return files.filter(p -> p.getFileName().toString().matches("case.*\\.json")).sorted().toList();
        }
    }
}
