package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import dev.uchar.regelspraak.transpiler.tms.FilterFault;
import dev.uchar.regelspraak.transpiler.tms.Fout;
import dev.uchar.regelspraak.transpiler.tms.MemberFault;
import dev.uchar.regelspraak.transpiler.tms.TestTermWitnessTest;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The member-fault rule of a per-element fold over a timeline, fired. That rule tests the fold's filter without its
 * fault ({@code Term.WithoutFault}), and then that the member's timeline is the asOfDate-bucket scalar the fold refuses;
 * the filter-fault rule takes the filter's fault. {@code TestTermWitnessTest} pins the plan of these tests on
 * {@code jutterswet-keurlast} with a filter that divides ({@link TestTermWitnessTest#keurlastWithDividingFilter}); no
 * document of the corpus or of the suite has a per-element fold whose filter can fault, so the harness
 * {@code FireTrace} never fires such a rule, and this test fires that document.
 *
 * <p>The case: J1 with one Vondst V1, gewicht 10 and deelnemers 0, so the filter divides 10 by 0 (RUNTIME-SEMANTICS
 * ch. 1.4, point 4). Where V1's keurtarief is a bare number, the asOfDate-bucket scalar, the second test of the
 * member-fault rule holds, so only the first decides: the filter faults, the test does not hold, and no
 * {@link MemberFault} stands. What stands is the {@link FilterFault} of J1's fold for V1 and the {@link Fout} of the rule
 * "keurlast van een jutter", which reaches it. With a timeline on V1 the same faults stand. Measured 2026-10-13 on the
 * same document and cases: the reference interpreter's runner faults on both with "division by numeric zero"; the ASP
 * runner faults on the timeline with the same sentence and refuses the bucket scalar at translation ("a member without
 * its own timeline hangs on the answer", ch. 3.4a/3.6).
 *
 * <p><b>Red</b> under a mutation, since the behaviour itself did not change: a {@code WithoutFault} that catches a
 * {@code Rekenfout} only lets the division by zero, a {@code Breuk.DelingDoorNul}, leave the left-hand side of the
 * member-fault rule, and the fire throws.
 */
class TmsMemberFaultWitnessTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    private static final String BUCKET_SCALAR = "{\"number\":\"5\"}";
    private static final String TIJDLIJN =
            "{\"timeline\":[{\"from\":\"2026-01-01\",\"to\":\"2026-04-01\",\"value\":{\"number\":\"12\"}}]}";

    /** The fault of the rule that reaches the fold, which is the filter's division. */
    private static final String DIVISION = "DelingDoorNul: deling door nul in de regel \"keurlast van een jutter\": 10"
            + " gedeeld door 0 — een numerieke nul als deler is een fout, geen leeg (RUNTIME-SEMANTICS ch. 1.4, punt 4)";

    @Test void where_the_filter_fails_the_member_fault_rule_does_not_hold(@TempDir Path map) throws Exception {
        Path ir = Files.writeString(map.resolve("ir.json"),
                JSON.writeValueAsString(TestTermWitnessTest.keurlastWithDividingFilter()));
        List<String> outcomes = new ArrayList<>(), verwacht = new ArrayList<>();
        for (String keurtarief : List.of(BUCKET_SCALAR, TIJDLIJN)) {
            JsonNode caseDocument = caseDocument(keurtarief);
            outcomes.add(keurtarief + " " + TmsFaultWitnessTest.play(ir, caseDocument, DroolsBackend.Branch.EXEC_MODEL));
            verwacht.add(keurtarief + " " + DIVISION);
            outcomes.add(keurtarief + " " + fouten(ir, caseDocument));
            verwacht.add(keurtarief + " " + List.of(
                    "Filterfout J1 V1: DelingDoorNul",
                    "Fout J1 \"keurlast van een jutter\": " + DIVISION.substring("DelingDoorNul: ".length())));
        }
        assertEquals(verwacht, outcomes);
    }

    /** J1 with one Vondst V1, gewicht 10 and deelnemers 0, whose keurtarief is {@code keurtarief}. */
    private static JsonNode caseDocument(String keurtarief) throws Exception {
        return JSON.readTree("""
                {"format":"uchar-case-v0","asOfDate":"2026-03-15","input":{"instances":[
                  {"label":"J1","objectType":"Jutter","attributes":{},"roles":{"vondsten":["V1"]},"characteristics":[]},
                  {"label":"V1","objectType":"Vondst","attributes":{"keurtarief":%s,"gewicht":{"number":"10"},
                   "deelnemers":{"number":"0"}},"roles":{},"characteristics":[]}],"parameters":{}},
                 "expected":[{"kind":"consistent"}]}
                """.formatted(keurtarief));
    }

    /** Every fault that stands at the fixpoint of {@code caseDocument}, a {@link Fout}, a {@link FilterFault} or a
     *  {@link MemberFault}, with the case labels of the objects it names, sorted. Read off the session after the fire,
     *  before the write-back, which raises the first fault instead. */
    private static List<String> fouten(Path ir, JsonNode caseDocument) throws Exception {
        return CaseRunner.forMeasurement(ir, DroolsBackend.Branch.EXEC_MODEL).withSession(caseDocument, (bouw, sessie, labels) -> {
            DroolsBackend.Vuring firing = DroolsBackend.vuur(bouw, sessie);
            if (firing.geenFixpunt() != null) throw new AssertionError(firing.geenFixpunt());
            Map<Object, String> label = new IdentityHashMap<>();
            labels.forEach((l, beanObject) -> label.put(beanObject, l));
            List<String> out = new ArrayList<>();
            for (Object f : sessie.getObjects()) {
                if (f instanceof Fout x)
                    out.add("Fout " + label.get(x.onderwerp()) + " \"" + x.regel() + "\": " + x.melding());
                else if (f instanceof FilterFault x)
                    out.add("Filterfout " + label.get(x.onderwerp()) + " " + label.get(x.element()) + ": "
                            + x.fout().getClass().getSimpleName());
                else if (f instanceof MemberFault x)
                    out.add("Lidfout " + label.get(x.onderwerp()) + " " + label.get(x.element()) + ": "
                            + x.fout().getClass().getSimpleName());
            }
            out.sort(null);
            return out;
        });
    }
}
