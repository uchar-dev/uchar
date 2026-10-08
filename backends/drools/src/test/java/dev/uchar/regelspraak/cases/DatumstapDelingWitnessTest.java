package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A date step whose count is a division that does not terminate: "zijn meldtijdstip plus zijn wachttijd gedeeld door
 * 3". RUNTIME-SEMANTICS ch. 3.8 truncates a fractional count toward zero before the step, so 10 hours divided by 3 step
 * 3 hours and -10 divided by 3 step -3. The RegelSpraak frontend refuses such a source sentence by its form, but the
 * wire carries it and the verifier admits it, so the documents here are corpus documents twisted in memory: the red
 * case {@code datumtijdstap-buiten-bereik} for the calendar-unit step, and the goldens {@code jutterswet-bergingsklok}
 * and {@code jutterswet-getijklok} for the day-kind step on a date and on a date-time. Every expectation is the
 * reference interpreter's answer on the same twisted document and case (measured 2026-10-09): 2024-01-01T03:00:00,
 * 2023-12-31T21:00:00, and three springtijdagen from 2026-03-10, 2026-04-15 with and without its 09:15:30. The ASP
 * backend refuses the documents at translation ("a unit-carrying multiplicative operand is not translated").
 *
 * <p>The exact quotient is a {@code Breuk}, and the step's count has to accept one. On the truth-maintenance branch the
 * day-kind rows stay refused: day kinds are outside that branch's trial, and the rows pin the refusal, so a branch that
 * starts carrying them fails here until the rows expect its values.
 *
 * <p>RED FIRST, measured on this branch's first commit (cf54b8b9), where the runtime took the count as a
 * {@code BigDecimal}: the exec-model and truth-maintenance branches cast the quotient to one and threw
 * "ClassCastException: class dev.uchar.regelspraak.runtime.Breuk cannot be cast to class java.math.BigDecimal" at
 * fire; the DRL-text branch did not compile the rule ("Unable to Analyse Expression ... rsDatumReken(...,
 * rsGedeeld(...), ...)", the method took no {@code Object}) and the replay refused the document, also where the
 * division terminates.
 */
class DatumstapDelingWitnessTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    // cwd under surefire is the module dir (backends/drools); the corpus is two levels up.
    private static final Path CORPUS = Path.of("..", "..", "corpus");

    @Test void een_niet_afbrekend_aantal_in_een_kalenderstap_in_elke_tak() throws Exception {
        Path ir = gedeeld(CORPUS.resolve("rood/datumtijdstap-buiten-bereik/ir.json"), "sluittijdstip van een getijpost");
        JsonNode tien = zaak("Getijpost", "G1", "{\"meldtijdstip\":{\"dateTime\":\"2024-01-01T00:00:00\"},"
                + "\"wachttijd\":{\"number\":\"10\"}}", "sluittijdstip", "{\"dateTime\":\"2024-01-01T03:00:00\"}");
        JsonNode minTien = zaak("Getijpost", "G1", "{\"meldtijdstip\":{\"dateTime\":\"2024-01-01T00:00:00\"},"
                + "\"wachttijd\":{\"number\":\"-10\"}}", "sluittijdstip", "{\"dateTime\":\"2023-12-31T21:00:00\"}");
        for (DroolsBackend.Branch tak : DroolsBackend.Branch.values()) {
            assertNull(CaseRunner.speelCase(ir, tien, tak), tak + ": 10 uur gedeeld door 3 is een stap van 3 uur");
            assertNull(CaseRunner.speelCase(ir, minTien, tak), tak + ": -10 uur gedeeld door 3 is een stap van -3 uur");
        }
    }

    @Test void een_niet_afbrekend_aantal_in_een_dagsoortstap() throws Exception {
        Path datum = gedeeld(CORPUS.resolve("golden/jutterswet-bergingsklok/ir.json"),
                "uiterste bergingsdatum van een bergingsploeg");
        JsonNode berging = zaak("Bergingsploeg", "P1", "{\"meldingsdatum\":{\"date\":\"2026-03-10\"},"
                        + "\"opleverdatum\":{\"date\":\"2026-04-20\"},\"bergingsbestek\":{\"number\":\"10\"}}",
                "uiterste bergingsdatum", "{\"date\":\"2026-04-15\"}");
        Path tijdstip = gedeeld(CORPUS.resolve("golden/jutterswet-getijklok/ir.json"), "sluittijdstip van een getijpost");
        JsonNode getij = zaak("Getijpost", "G1", "{\"meldtijdstip\":{\"dateTime\":\"2026-03-10T09:15:30\"},"
                + "\"wachttermijn\":{\"number\":\"10\"}}", "sluittijdstip", "{\"dateTime\":\"2026-04-15T09:15:30\"}");
        for (DroolsBackend.Branch tak : new DroolsBackend.Branch[] {
                DroolsBackend.Branch.EXEC_MODEL, DroolsBackend.Branch.DRL_TEXT }) {
            assertNull(CaseRunner.speelCase(datum, berging, tak), tak + ": 10 springtijdagen gedeeld door 3 op een datum");
            assertNull(CaseRunner.speelCase(tijdstip, getij, tak), tak + ": hetzelfde op een datum-tijd");
        }
        for (Path ir : new Path[] { datum, tijdstip }) {
            JsonNode c = ir == datum ? berging : getij;
            CaseRunner.Excluded e = assertThrows(CaseRunner.Excluded.class,
                    () -> CaseRunner.speelCase(ir, c, DroolsBackend.Branch.TRUTH_MAINTENANCE), "truth maintenance");
            assertTrue(e.getMessage().contains("dagsoorten zijn buiten de proef"), e.getMessage());
        }
    }

    /** {@code bron} with the duration operand X of the rule {@code regel} replaced by "X gedeeld door 3", typed as the
     *  quotient of X's unit, written to a temporary file. */
    private static Path gedeeld(Path bron, String regel) throws Exception {
        ObjectNode ir = (ObjectNode) JSON.readTree(Files.readString(bron));
        ObjectNode waarde = (ObjectNode) IrPad.versie(ir, "rules", regel).get("value");
        JsonNode duur = waarde.get("right");
        JsonNode eenheid = IrPad.attribute(IrPad.objectType(ir, duur.get("subjectType").asText()),
                duur.get("attribute").asText()).get("datatype").get("unit");
        ObjectNode deling = JSON.createObjectNode();
        deling.put("node", "Binary").put("op", "dividedBy");
        deling.set("left", duur);
        deling.set("right", JSON.readTree(IrPad.literal("3",
                "{\"node\":\"Number\",\"whole\":true,\"decimals\":0,\"sign\":\"unspecified\"}", "3")));
        ObjectNode type = deling.putObject("type");
        type.put("node", "Number").put("whole", false).put("decimals", 2).put("sign", "unspecified");
        type.set("unit", eenheid);
        deling.set("source", IrPad.bronrecord(duur.get("source").get("snippet").asText() + " gedeeld door 3"));
        waarde.set("right", deling);
        Path tmp = Files.createTempFile("datumstap-deling", ".json");
        tmp.toFile().deleteOnExit();
        Files.writeString(tmp, ir.toString());
        return tmp;
    }

    private static JsonNode zaak(String objecttype, String label, String attributen, String doel, String waarde)
            throws Exception {
        return JSON.readTree("""
            { "format": "uchar-case-v0", "asOfDate": null,
              "input": { "instances": [ { "label": "%2$s", "objectType": "%1$s", "attributes": %3$s, "roles": {},
                  "characteristics": [] } ], "parameters": {} },
              "expected": [ { "kind": "attributeValue", "label": "%2$s", "attribute": "%4$s", "value": %5$s } ] }
            """.formatted(objecttype, label, attributen, doel, waarde));
    }
}
