package dev.uchar.regelspraak.transpiler.tms;

import dev.uchar.regelspraak.cases.CaseRunner;
import dev.uchar.regelspraak.cases.DroolsBackend;
import dev.uchar.regelspraak.loader.IrLoader;
import dev.uchar.regelspraak.transpiler.tms.plan.Consequence;
import dev.uchar.regelspraak.transpiler.tms.plan.Modelplan;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A build with readings ({@code DroolsBackend.build(…, readings)}) plans the rules of the forms as the build without does:
 * it adds its reading rules and changes no other rule. Every document of the corpus and every fixture of a document with
 * {@code inferFromEmpty: false} ({@code src/test/resources/open-values/}, {@code open-readings/},
 * {@code ref/test/fixtures/strict-empty/}) is built both ways on the exec-model branch, and the DRL text of its plan without
 * the reading rules ({@link DrlRendering}, which writes the plan rule by rule, its functions and globals with it) must be
 * equal. A reading needs the order of the members of a fold the rules read without it, and the object a path reaches;
 * the build with readings keeps both out of the rules of the forms (the order on the watch of the session, the object in
 * slots of the reading rule alone). {@code ReadingRulesFixpointWitnessTest} and {@code OpenFixturesFixpointWitnessTest}
 * hold the facts of the sessions to the same.
 *
 * <p><b>Red</b> on {@code 78baf8abb}, the step of card fold-order-in-readings before the object a path reaches left the
 * rules of the forms: 8 documents of the corpus differed in the slots after a read over a path (measured with this
 * comparison, 486 documents).
 *
 * <p>Floor, measured 2026-11-24 at card fold-order-in-readings: {@value #EXPECTED_MIN_DOCUMENTS} documents compared. A
 * document the build without readings refuses is not compared; one the build with readings refuses where the one without
 * builds fails by name.
 */
class ReadingRulesPlanWitnessTest {

    static final int EXPECTED_MIN_DOCUMENTS = 488;

    @Test void a_build_with_readings_plans_the_rules_of_the_forms_as_the_build_without() throws Exception {
        List<Path> documents = new ArrayList<>();
        for (Path root : List.of(Path.of("..", "..", "corpus", "golden"), Path.of("src", "test", "resources", "open-values"),
                Path.of("src", "test", "resources", "open-readings"),
                Path.of("..", "..", "ref", "test", "fixtures", "strict-empty")))
            try (Stream<Path> s = Files.walk(root)) {
                s.filter(p -> p.getFileName().toString().equals("ir.json")).sorted().forEach(documents::add);
            }
        List<String> differences = new ArrayList<>();
        int compared = 0;
        for (Path ir : documents) {
            String without;
            try {
                without = production(DroolsBackend.build(ir, DroolsBackend.Branch.EXEC_MODEL, false).execModelTranspiler()
                        .modelplan());
            } catch (IrLoader.Refusal | IrLoader.DocumentError | CaseRunner.Excluded notBuilt) {
                continue;
            }
            String with;
            try {
                with = production(DroolsBackend.build(ir, DroolsBackend.Branch.EXEC_MODEL, true).execModelTranspiler()
                        .modelplan());
            } catch (CaseRunner.Excluded e) {
                differences.add(ir + ": de build met lezingen weigert, " + e.code());
                continue;
            }
            compared++;
            String difference = firstDifference(without, with);
            if (difference != null) differences.add(ir + ": " + difference);
        }
        int n = compared;
        System.out.println("ReadingRulesPlanWitnessTest: " + n + " documenten vergeleken");
        assertTrue(n >= EXPECTED_MIN_DOCUMENTS, () -> "vergeleken documenten " + n + ", onder de vloer "
                + EXPECTED_MIN_DOCUMENTS);
        assertEquals(List.of(), differences, "documenten waarvan de regels van de vormen met en zonder lezingen verschillen");
    }

    /** The DRL text of {@code plan} without its reading rules. */
    private static String production(Modelplan plan) {
        return DrlRendering.text(new Modelplan(plan.globals(), plan.rules().stream()
                .filter(r -> !(r.consequence() instanceof Consequence.Reading)).toList()));
    }

    private static String firstDifference(String without, String with) {
        if (without.equals(with)) return null;
        String[] a = without.split("\n"), b = with.split("\n");
        for (int i = 0; i < Math.max(a.length, b.length); i++) {
            String x = i < a.length ? a[i] : "<einde>", y = i < b.length ? b[i] : "<einde>";
            if (!x.equals(y)) return "regel " + (i + 1) + ": zonder " + x + " | met " + y;
        }
        return "gelijke regels, ander einde";
    }
}
