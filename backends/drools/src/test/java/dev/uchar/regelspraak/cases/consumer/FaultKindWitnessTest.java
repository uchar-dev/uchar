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

    private static CaseInput read(String red, String name) throws Exception {
        DroolsBackend.Build b = DroolsBackend.build(RED.resolve(red).resolve("ir.json"), DroolsBackend.Branch.EXEC_MODEL);
        return CaseInput.of(b, caseFile(red, name));
    }

    private static JsonNode caseFile(String red, String name) throws Exception {
        return JSON.readTree(Files.readString(RED.resolve(red).resolve(name + ".json")));
    }
}
