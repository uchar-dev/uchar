package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import dev.uchar.regelspraak.transpiler.tms.PeriodDecisions;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * A defaults group on a timeline target decides per day (RUNTIME-SEMANTICS ch. 7.11), on both branches of the lowering
 * with truth maintenance ({@code TmsTranspiler.dayStackRules}, {@code PeriodDefaults}), on the documents and cases that
 * the reference interpreter's suite plays as well ({@code WV-7.11-…}, {@code WV-5.12-default-per-dag}):
 *
 * <ul>
 *   <li>the documents of {@code ref/test/fixtures/defaults-per-day}: an exception above "zijn basisloon"; two unordered
 *       members with their conflict per day and one §9.5 fact; a form-a default with unordered exception groups, whose
 *       conflict on a day settles it; the same below a higher default that displaces that conflict on its day; a group
 *       with a validity window; a consequence that reads the asOfDate-bucket scalar where no day of its turn justifies
 *       it, which is not evaluated; and a consequence that divides by 0 on a day, whose deposit EF-005 stands where the
 *       default decides on some day and not where it decides none; and the cases of the golden
 *       {@code jutterswet-opgave-per-periode}, with beside them an opgave on all time, below which the computation is at
 *       its turn on no day;</li>
 *   <li>the faults of a default's evaluation: the consequence that reads the bucket scalar faults where it is
 *       justified on a day of its turn, and so does the initialisation default whose target's input is one (EF-037);</li>
 *   <li>the readings per day (ch. 5.12): the justification read per period, the consequence where it holds on some
 *       day, its root the filled segments within its periods, the status HOLDS where the default is at its turn on
 *       some day and DISPLACED where on none, and the initialisation default without nodes where the input fills its
 *       target on every day; line by line the reference interpreter's trace;</li>
 *   <li>{@code decisionsOf} has no record of such a stack, which has none per object, and {@code periodDecisionsOf}
 *       has one per object and target ({@code PeriodDecisionsWitnessTest} holds its periods).</li>
 * </ul>
 */
class TmsDefaultsPerDayWitnessTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    // cwd under surefire is the module dir (backends/drools); the fixtures are two levels up
    private static final Path FIXTURES = Path.of("..", "..", "ref", "test", "fixtures");
    private static final Path DOCUMENTS = FIXTURES.resolve("defaults-per-day");
    private static final Path CASES = FIXTURES.resolve("defaults-per-day-cases");
    private static final Path GOLDEN = Path.of("..", "..", "corpus", "golden", "jutterswet-opgave-per-periode");

    @Test void the_documents_play_per_day_on_both_branches() throws Exception {
        List<String> outcomes = new ArrayList<>(), expected = new ArrayList<>();
        List<Path[]> plays = new ArrayList<>();
        for (String d : List.of("uitzondering", "ongeordend", "afwijking", "verdrongen-conflict", "geldig-vanaf",
                "lazy-consequence", "deposit"))
            plays.add(new Path[]{DOCUMENTS.resolve(d).resolve("ir.json"), DOCUMENTS.resolve(d).resolve("case.json")});
        for (String c : List.of("case1-opgave-heel-2025", "case2-opgave-januari-februari", "case3-zonder-opgave",
                "case4-invoer-op-het-doel", "case5-stormtoelage-per-dag"))
            plays.add(new Path[]{GOLDEN.resolve("ir.json"), GOLDEN.resolve(c + ".json")});
        plays.add(new Path[]{GOLDEN.resolve("ir.json"), CASES.resolve("turn-on-no-day").resolve("case.json")});
        plays.add(new Path[]{DOCUMENTS.resolve("uitzondering").resolve("ir.json"),
                CASES.resolve("justified-on-no-day").resolve("case.json")});
        plays.add(new Path[]{DOCUMENTS.resolve("deposit").resolve("ir.json"),
                CASES.resolve("deposit-dropped").resolve("case.json")});
        for (Path[] p : plays) {
            JsonNode c = JSON.readTree(Files.readString(p[1]));
            for (boolean viaDrl : new boolean[]{false, true}) {
                String head = p[1].getParent().getFileName() + "/" + p[1].getFileName()
                        + (viaDrl ? " drl-tekst" : " exec-model");
                String verdict = CaseRunner.playCase(p[0], c, viaDrl);
                outcomes.add(head + " " + (verdict == null ? "gespeeld" : verdict));
                expected.add(head + " gespeeld");
            }
        }
        assertEquals(expected, outcomes);
    }

    @Test void a_default_faults_where_it_is_evaluated() {
        faults(DOCUMENTS.resolve("lazy-consequence").resolve("ir.json"),
                CASES.resolve("lazy-consequence-reached").resolve("case.json"), "kustloon", "J1");
        faults(GOLDEN.resolve("ir.json"), CASES.resolve("bucket-input").resolve("case.json"), "stormtoelage", "J7");
    }

    @Test void the_readings_per_day_are_those_of_the_reference_interpreter() {
        String a = "reading /rules/prioritizedDefaults/0/defaults/", b = "reading /rules/prioritizedDefaults/1/defaults/";
        String always = " 0 Compound [[- - empty]]";
        TmsConditionPerPeriodWitnessTest.readings(GOLDEN.resolve("ir.json"),
                GOLDEN.resolve("case2-opgave-januari-februari.json"), List.of("case case2-opgave-januari-februari.json",
                        a + "0 \"J2\"" + always, a + "0 \"J2\" 1 AttributeReference [[2025-01-01 2025-03-01 10]]",
                        a + "0 \"J2\" HOLDS", a + "1 \"J2\"" + always,
                        a + "1 \"J2\" 1 AttributeReference [[2025-03-01 2025-06-01 20]]", a + "1 \"J2\" HOLDS",
                        b + "0 \"J2\"" + always, b + "0 \"J2\" 1 AttributeReference []", b + "0 \"J2\" HOLDS",
                        b + "1 \"J2\"" + always, b + "1 \"J2\" 1 AttributeReference []", b + "1 \"J2\" HOLDS",
                        b + "2 \"J2\" 0 Literal [[- - 0]]", b + "2 \"J2\" HOLDS",
                        "reading /rules/rules/0/versions/0 \"J2\" 0 TimeWindow [[2025-03-01 2025-06-01 empty]]",
                        "reading /rules/rules/0/versions/0 \"J2\" 1 Literal 2025-03-01",
                        "reading /rules/rules/0/versions/0 \"J2\" 2 Literal 2025-06-01",
                        "reading /rules/rules/0/versions/0 \"J2\" 3 Literal [[2025-03-01 2025-06-01 20]]",
                        "reading /rules/rules/0/versions/0 \"J2\" HOLDS",
                        "reading /rules/rules/1/versions/0 \"J2\" 0 CharacteristicCheck []",
                        "reading /rules/rules/1/versions/0 \"J2\" 1 Literal []",
                        "reading /rules/rules/1/versions/0 \"J2\" HOLDS", "READINGS 1"));
        TmsConditionPerPeriodWitnessTest.readings(GOLDEN.resolve("ir.json"),
                CASES.resolve("turn-on-no-day").resolve("case.json"), List.of("case case.json",
                        a + "0 \"J6\"" + always, a + "0 \"J6\" 1 AttributeReference [[- - 10]]", a + "0 \"J6\" HOLDS",
                        a + "1 \"J6\"" + always, a + "1 \"J6\" 1 AttributeReference [[2025-03-01 2025-06-01 20]]",
                        a + "1 \"J6\" DISPLACED", b + "0 \"J6\"" + always,
                        b + "0 \"J6\" 1 AttributeReference [[- - 12]]", b + "0 \"J6\" HOLDS", b + "1 \"J6\"" + always,
                        b + "1 \"J6\" 1 AttributeReference [[2025-10-01 2026-01-01 30]]", b + "1 \"J6\" DISPLACED",
                        b + "2 \"J6\" DISPLACED",
                        "reading /rules/rules/0/versions/0 \"J6\" 0 TimeWindow [[2025-03-01 2025-06-01 empty]]",
                        "reading /rules/rules/0/versions/0 \"J6\" 1 Literal 2025-03-01",
                        "reading /rules/rules/0/versions/0 \"J6\" 2 Literal 2025-06-01",
                        "reading /rules/rules/0/versions/0 \"J6\" 3 Literal [[2025-03-01 2025-06-01 20]]",
                        "reading /rules/rules/0/versions/0 \"J6\" HOLDS",
                        "reading /rules/rules/1/versions/0 \"J6\" 0 CharacteristicCheck [[2025-10-01 2026-01-01 empty]]",
                        "reading /rules/rules/1/versions/0 \"J6\" 1 Literal [[2025-10-01 2026-01-01 30]]",
                        "reading /rules/rules/1/versions/0 \"J6\" HOLDS", "READINGS 1"));
        TmsConditionPerPeriodWitnessTest.readings(DOCUMENTS.resolve("uitzondering").resolve("ir.json"),
                CASES.resolve("justified-on-no-day").resolve("case.json"), List.of("case case.json",
                        a + "0 \"J3\" 0 CharacteristicCheck []", a + "0 \"J3\" 1 Literal unreached", a + "0 \"J3\" HOLDS",
                        a + "1 \"J3\"" + always, a + "1 \"J3\" 1 AttributeReference [[2025-01-01 2026-01-01 10]]",
                        a + "1 \"J3\" HOLDS", "READINGS 1"));
    }

    /** A consequence the session does not evaluate is read where its justification holds on a day (ch. 5.12), and a
     *  node carries the fault it raised or passed on: the read of the bucket scalar as the consequence's root, and as
     *  the left operand of a sum, whose right operand is not reached after the fault (the goldens
     *  {@code jutterswet-gevolg-buiten-de-beurt} and {@code -som}, the {@code ref/} tests
     *  {@code WV-5.12-default-per-dag-gevolg-faalt} and {@code -som}). */
    @Test void a_fault_in_a_consequence_stands_at_the_node_that_raised_it() {
        String d = "reading /rules/prioritizedDefaults/0/defaults/";
        Path plain = Path.of("..", "..", "corpus", "golden", "jutterswet-gevolg-buiten-de-beurt");
        Path sum = Path.of("..", "..", "corpus", "golden", "jutterswet-gevolg-buiten-de-beurt-som");
        List<String> head = List.of("case case1-buiten-de-beurt.json",
                d + "0 \"J1\" 0 CharacteristicCheck []", d + "0 \"J1\" 1 Literal unreached", d + "0 \"J1\" HOLDS",
                d + "0 \"J2\" 0 CharacteristicCheck [[2025-04-01 2025-05-01 empty]]",
                d + "0 \"J2\" 1 Literal [[2025-04-01 2025-05-01 60]]", d + "0 \"J2\" HOLDS",
                d + "1 \"J1\" 0 CharacteristicCheck []");
        List<String> tail = List.of(d + "2 \"J1\" 0 Compound [[- - empty]]", d + "2 \"J1\" 1 Literal [[- - 5]]",
                d + "2 \"J1\" HOLDS", d + "2 \"J2\" 0 Compound [[- - empty]]", d + "2 \"J2\" 1 Literal [[- - 5]]",
                d + "2 \"J2\" HOLDS", "READINGS 1");
        List<String> expected = new ArrayList<>(head);
        expected.addAll(List.of(d + "1 \"J1\" 1 AttributeReference unreached", d + "1 \"J1\" HOLDS",
                d + "1 \"J2\" 0 CharacteristicCheck [[2025-04-01 2025-05-01 empty]]",
                d + "1 \"J2\" 1 AttributeReference fault EF-037", d + "1 \"J2\" HOLDS"));
        expected.addAll(tail);
        TmsConditionPerPeriodWitnessTest.readings(plain.resolve("ir.json"),
                plain.resolve("case1-buiten-de-beurt.json"), expected);
        expected = new ArrayList<>(head);
        expected.addAll(List.of(d + "1 \"J1\" 1 Binary unreached", d + "1 \"J1\" 2 AttributeReference unreached",
                d + "1 \"J1\" 3 Literal unreached", d + "1 \"J1\" HOLDS",
                d + "1 \"J2\" 0 CharacteristicCheck [[2025-04-01 2025-05-01 empty]]",
                d + "1 \"J2\" 1 Binary fault EF-037", d + "1 \"J2\" 2 AttributeReference fault EF-037",
                d + "1 \"J2\" 3 Literal unreached", d + "1 \"J2\" HOLDS"));
        expected.addAll(tail);
        TmsConditionPerPeriodWitnessTest.readings(sum.resolve("ir.json"), sum.resolve("case1-buiten-de-beurt.json"),
                expected);
    }

    @Test void a_stack_per_day_has_its_decisions_per_period() throws Exception {
        CaseRunner runner = CaseRunner.forMeasurement(GOLDEN.resolve("ir.json"), DroolsBackend.Branch.EXEC_MODEL);
        JsonNode c = JSON.readTree(Files.readString(GOLDEN.resolve("case2-opgave-januari-februari.json")));
        runner.withSession(c, (build, session, labels) -> {
            DroolsBackend.fire(build, session);
            assertEquals(List.of(), DroolsBackend.decisionsOf(build, session));
            List<String> stacks = new ArrayList<>();
            for (PeriodDecisions d : DroolsBackend.periodDecisionsOf(build, session))
                if (d.object() == labels.get("J2") && !d.rootChains().isEmpty()) stacks.add(d.member());
            stacks.sort(null);
            assertEquals(List.of("stormtoelage", "strandloon"), stacks);
            return null;
        });
    }

    /** Both branches fault on the red case {@code c} over {@code ir} with EF-037 for {@code attribute} of {@code label}. */
    private static void faults(Path ir, Path c, String attribute, String label) {
        CaseRunner.RedResult exec = CaseRunner.runRed(ir, c, DroolsBackend.Branch.EXEC_MODEL);
        assertEquals(exec, CaseRunner.runRed(ir, c, DroolsBackend.Branch.DRL_TEXT), "de twee takken, " + c);
        assertEquals("FAULT", exec.status(), exec::detail);
        assertEquals("EF-037", exec.code(), exec::detail);
        assertEquals(List.of(Map.of("attribute", attribute, "label", label)), exec.params(), exec::detail);
    }
}
