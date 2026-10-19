package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import dev.uchar.regelspraak.transpiler.tms.FilterFault;
import dev.uchar.regelspraak.transpiler.tms.Fault;
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
 * <p>The case: J1 with one Vondst V1, gewicht 2, and the filter of that document with the root of the gewicht in place
 * of its division ({@link #keurlastWithRootFilter}), so the filter takes the root of 2, which is no square
 * (RUNTIME-SEMANTICS ch. 2.2). Until the card zero-division-channel (2026-10-19) the filter divided 10 by 0, which
 * deposits since (ch. 1.4, point 4), and the measurements below are those of the division. Where V1's keurtarief is a
 * bare number, the asOfDate-bucket scalar, the second test of the
 * member-fault rule holds, so only the first decides: the filter faults, the test does not hold, and no
 * {@link MemberFault} stands. What stands is the {@link FilterFault} of J1's fold for V1 and the {@link Fault} of the rule
 * "keurlast van een jutter", which reaches it. With a timeline on V1 the same faults stand. Measured 2026-10-13 on the
 * same document and cases: the reference interpreter's runner faults on both with "division by numeric zero"; the ASP
 * runner faults on the timeline with the same sentence and refuses the bucket scalar at translation ("a member without
 * its own timeline hangs on the answer", ch. 3.4a/3.6).
 *
 * <p><b>Red</b> under a mutation, since the behaviour itself did not change: a {@code WithoutFault} that catches a
 * {@code ComputeFault} only let the division by zero, a {@code Rational.DelingDoorNul}, leave the left-hand side of the
 * member-fault rule, and the fire threw; with the root it is a {@code WithoutFault} that does not catch a
 * {@code ComputeFault} that does so.
 */
class TmsMemberFaultWitnessTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    private static final String BUCKET_SCALAR = "{\"number\":\"5\"}";
    private static final String TIJDLIJN =
            "{\"timeline\":[{\"from\":\"2026-01-01\",\"to\":\"2026-04-01\",\"value\":{\"number\":\"12\"}}]}";

    /** The fault of the rule that reaches the fold, which is the filter's root. */
    private static final String ROOT = "ComputeFault: " + TmsDocuments.irrational("keurlast van een jutter", "2");

    /** {@link TestTermWitnessTest#keurlastWithDividingFilter} with "de wortel van het gewicht" in place of the
     *  filter's division: the device of a fault in a filter since the card zero-division-channel. */
    static JsonNode keurlastWithRootFilter() throws Exception {
        com.fasterxml.jackson.databind.node.ObjectNode d =
                (com.fasterxml.jackson.databind.node.ObjectNode) TestTermWitnessTest.keurlastWithDividingFilter();
        JsonNode fold = d.path("rules").path("rules").get(0).path("versions").get(0).path("value");
        if (!replaceDivision(fold.path("filter")))
            throw new AssertionError("geen deling in het filter van " + fold);
        return d;
    }

    /** Replaces the first division under {@code node} by the root of its dividend; whether it found one. */
    private static boolean replaceDivision(JsonNode node) throws Exception {
        if (node instanceof com.fasterxml.jackson.databind.node.ObjectNode o) {
            var fields = o.fields();
            while (fields.hasNext()) {
                var f = fields.next();
                JsonNode v = f.getValue();
                if (v.path("node").asText().equals("Binary") && v.path("op").asText().equals("dividedBy")) {
                    o.set(f.getKey(), JSON.readTree(TmsDocuments.root(v.path("left").toString(), "de wortel van het gewicht")));
                    return true;
                }
                if (replaceDivision(v)) return true;
            }
        } else if (node instanceof com.fasterxml.jackson.databind.node.ArrayNode a) {
            for (int i = 0; i < a.size(); i++) {
                JsonNode v = a.get(i);
                if (v.path("node").asText().equals("Binary") && v.path("op").asText().equals("dividedBy")) {
                    a.set(i, JSON.readTree(TmsDocuments.root(v.path("left").toString(), "de wortel van het gewicht")));
                    return true;
                }
                if (replaceDivision(v)) return true;
            }
        }
        return false;
    }

    @Test void where_the_filter_fails_the_member_fault_rule_does_not_hold(@TempDir Path map) throws Exception {
        Path ir = Files.writeString(map.resolve("ir.json"), JSON.writeValueAsString(keurlastWithRootFilter()));
        List<String> outcomes = new ArrayList<>(), expected = new ArrayList<>();
        for (String keurtarief : List.of(BUCKET_SCALAR, TIJDLIJN)) {
            JsonNode caseDocument = caseDocument(keurtarief);
            outcomes.add(keurtarief + " " + TmsFaultWitnessTest.play(ir, caseDocument, DroolsBackend.Branch.EXEC_MODEL));
            expected.add(keurtarief + " " + ROOT);
            outcomes.add(keurtarief + " " + faults(ir, caseDocument));
            expected.add(keurtarief + " " + List.of(
                    "Filterfout J1 V1: ComputeFault",
                    "Fout J1 \"keurlast van een jutter\": " + ROOT.substring("ComputeFault: ".length())));
        }
        assertEquals(expected, outcomes);
    }

    /** J1 with one Vondst V1, gewicht 2, whose keurtarief is {@code keurtarief}. */
    private static JsonNode caseDocument(String keurtarief) throws Exception {
        return JSON.readTree("""
                {"format":"uchar-case-v0","asOfDate":"2026-03-15","input":{"instances":[
                  {"label":"J1","objectType":"Jutter","attributes":{},"roles":{"vondsten":["V1"]},"characteristics":[]},
                  {"label":"V1","objectType":"Vondst","attributes":{"keurtarief":%s,"gewicht":{"number":"2"}},
                   "roles":{},"characteristics":[]}],"parameters":{}},
                 "expected":[{"kind":"consistent"}]}
                """.formatted(keurtarief));
    }

    /** Every fault that stands at the fixpoint of {@code caseDocument}, a {@link Fault}, a {@link FilterFault} or a
     *  {@link MemberFault}, with the case labels of the objects it names, sorted. Read off the session after the fire,
     *  before the write-back, which raises the first fault instead. */
    private static List<String> faults(Path ir, JsonNode caseDocument) throws Exception {
        return CaseRunner.forMeasurement(ir, DroolsBackend.Branch.EXEC_MODEL).withSession(caseDocument, (build, session, labels) -> {
            DroolsBackend.Firing firing = DroolsBackend.fire(build, session);
            if (firing.noFixpoint() != null) throw new AssertionError(firing.noFixpoint());
            Map<Object, String> label = new IdentityHashMap<>();
            labels.forEach((l, beanObject) -> label.put(beanObject, l));
            List<String> out = new ArrayList<>();
            for (Object f : session.getObjects()) {
                if (f instanceof Fault x)
                    out.add("Fout " + label.get(x.subject()) + " \"" + x.rule() + "\": " + x.message());
                else if (f instanceof FilterFault x)
                    out.add("Filterfout " + label.get(x.subject()) + " " + label.get(x.element()) + ": "
                            + x.fault().getClass().getSimpleName());
                else if (f instanceof MemberFault x)
                    out.add("Lidfout " + label.get(x.subject()) + " " + label.get(x.element()) + ": "
                            + x.fault().getClass().getSimpleName());
            }
            out.sort(null);
            return out;
        });
    }
}
