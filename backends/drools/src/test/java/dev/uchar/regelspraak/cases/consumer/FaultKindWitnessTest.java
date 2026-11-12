package dev.uchar.regelspraak.cases.consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.uchar.regelspraak.cases.CaseInput;
import dev.uchar.regelspraak.cases.CaseRunner;
import dev.uchar.regelspraak.cases.DroolsBackend;

import org.junit.jupiter.api.Test;
import org.kie.api.runtime.KieSession;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * The EF code and the parameters of a fault (RUNTIME-SEMANTICS ch. 5.7b) as a consumer outside the package of the
 * backend reads them, a decision service that answers a fault with its code: through {@link CaseRunner#faultKinds} and
 * through the {@code code()} and {@code params()} of the fault types the case input raises. Every row reads a red case
 * of the corpus through the public entries and expects what the case pins.
 */
class FaultKindWitnessTest {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Path RED = Path.of("..", "..", "corpus", "red");

    @Test void a_fault_of_the_case_input_names_its_kind() throws Exception {
        CaseRunner.CaseDateFault date = assertThrows(CaseRunner.CaseDateFault.class,
                () -> read("as-of-date-calendar", "case1"));
        assertEquals("EF-003", date.code());
        assertEquals(Map.of("field", "asOfDate", "lexeme", "2026-02-30"), date.params());
        assertEquals(List.of(new CaseRunner.FaultKind("EF-003", date.params())), CaseRunner.faultKinds(date));

        CaseInput.LinkCycleFault cycle = assertThrows(CaseInput.LinkCycleFault.class,
                () -> read("instance-order-link-cycle", "case1-twee-vergunningen"));
        assertEquals("EF-033", cycle.code());
        assertEquals(Map.of("factType", "vorige vergunning", "cycle", "V1, V2"), cycle.params());
        assertEquals(List.of(new CaseRunner.FaultKind("EF-033", cycle.params())), CaseRunner.faultKinds(cycle));
    }

    @Test void a_fault_at_the_fixpoint_names_its_kind() throws Exception {
        DroolsBackend.Build b = DroolsBackend.build(RED.resolve("aanvoergeld-parameter-unbound").resolve("ir.json"),
                DroolsBackend.Branch.EXEC_MODEL);
        CaseInput input = CaseInput.of(b, caseFile("aanvoergeld-parameter-unbound", "case1-tarief-ongebonden"));
        ClassLoader previous = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(b.beanLoader());
        KieSession session = b.kieBase().newKieSession();
        try {
            input.setGlobals(session);
            DroolsBackend.insert(b, session, input.inputData());
            assertNull(DroolsBackend.fire(b, session).noFixpoint());
            RuntimeException f = assertThrows(RuntimeException.class, () -> DroolsBackend.setDerivedValues(b, session));
            assertEquals(List.of(new CaseRunner.FaultKind("EF-014", Map.of("parameter", "aanvoertarief"))),
                    CaseRunner.faultKinds(f));
        } finally {
            session.dispose();
            Thread.currentThread().setContextClassLoader(previous);
        }
    }

    /** The faults at the fixpoint whose kinds RUNTIME-SEMANTICS ch. 5.7b names since the red case of each landed:
     *  supplied periods of a characteristic a rule writes (EF-041), the subject's own total over a full timeline
     *  (EF-042) and without an evaluation date (EF-043), an irrational power without a rounding (EF-045) and a part of
     *  a date construction that is no whole number (EF-046). Each names its code and the parameters its red case pins,
     *  on both branches.
     *
     *  <p>Red before: the backend named no kind for any of them, so a consumer reading {@link CaseRunner#faultKinds}
     *  got an empty list. */
    @Test void every_fault_at_the_fixpoint_names_its_kind() throws Exception {
        for (String red : List.of("characteristic-periods-beside-rule", "own-total-full-timeline",
                "own-total-without-evaluation-date", "irrational-power-without-rounding", "date-part-not-whole")) {
            JsonNode pin = caseFile(red, "case1").get("expectedFault");
            Map<String, String> params = new java.util.LinkedHashMap<>();
            pin.get("params").fields().forEachRemaining(e -> params.put(e.getKey(), e.getValue().asText()));
            for (DroolsBackend.Branch branch : DroolsBackend.Branch.values())
                assertEquals(List.of(new CaseRunner.FaultKind(pin.get("code").asText(), params)),
                        fixpointFaultKinds(red, "case1", branch), red + " on " + branch);
        }
    }

    /** The kinds of what the write-back throws at the fixpoint of the red case {@code red}/{@code name} on
     *  {@code branch}, read through the public entries as a consumer reads them. */
    private static List<CaseRunner.FaultKind> fixpointFaultKinds(String red, String name, DroolsBackend.Branch branch)
            throws Exception {
        DroolsBackend.Build b = DroolsBackend.build(RED.resolve(red).resolve("ir.json"), branch);
        CaseInput input = CaseInput.of(b, caseFile(red, name));
        ClassLoader previous = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(b.beanLoader());
        KieSession session = b.kieBase().newKieSession();
        try {
            input.setGlobals(session);
            DroolsBackend.insert(b, session, input.inputData());
            assertNull(DroolsBackend.fire(b, session).noFixpoint());
            // the write-back names an object by its case label, as the red run of gate 8 does
            Map<Object, String> labels = new java.util.IdentityHashMap<>();
            input.beans().forEach((label, bean) -> labels.put(bean, label));
            return CaseRunner.faultKinds(assertThrows(RuntimeException.class,
                    () -> DroolsBackend.setDerivedValues(b, session, labels::get)));
        } finally {
            session.dispose();
            Thread.currentThread().setContextClassLoader(previous);
        }
    }

    private static CaseInput read(String red, String name) throws Exception {
        DroolsBackend.Build b = DroolsBackend.build(RED.resolve(red).resolve("ir.json"), DroolsBackend.Branch.EXEC_MODEL);
        return CaseInput.of(b, caseFile(red, name));
    }

    private static JsonNode caseFile(String red, String name) throws Exception {
        return JSON.readTree(Files.readString(RED.resolve(red).resolve(name + ".json")));
    }
}
