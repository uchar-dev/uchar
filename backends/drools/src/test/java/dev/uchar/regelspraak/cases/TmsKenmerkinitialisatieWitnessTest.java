package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * A characteristic initialisation of an object creation whose value is no truth value: the golden
 * {@code anchor/jutterswet-rekenkamer-overzee} with "overzees gelijk aan 5" in place of its boolean value. A
 * characteristic is true or false (WF-027); the branch refuses the form by name. Before, its consequence read the 5 as
 * not true and left the characteristic unset, so the creation played.
 *
 * <p><b>Measured</b> 2026-10-10 on that document with the case of J1 (grondslag 800, heffing 800 expected): the
 * verifier refuses it, "WF-027 FAIL /rules/objectCreations/0/versions/0/initialisations/1: characteristic
 * initialisation's value must be a boolean-typed expression"; the reference interpreter faults, "characteristic
 * initialisation of "overzees" is not boolean: 5"; the ASP runner declines the expression at translation; the
 * exec-model and the DRL-text branch fault at fire, "kenmerkinitialisatie van 'overzees' is niet booleaans: 5 (WF-027)".
 *
 * <p><b>Red</b> on the claim of the card, 331b1179: the manifest carried no refusal, and the case played.
 */
class TmsKenmerkinitialisatieWitnessTest {

    // cwd under surefire is the module dir (backends/drools); the corpus is two levels up.
    private static final Path GOLDEN = Path.of("..", "..", "corpus", "golden", "anchor", "jutterswet-rekenkamer-overzee");

    private static final String ZAAK = """
            { "format": "uchar-case-v0", "asOfDate": null,
              "input": { "instances": [ { "label": "J1", "objectType": "Jutter",
                  "attributes": { "grondslag": { "number": "800" } },
                  "roles": {}, "characteristics": [] } ], "parameters": {} },
              "expected": [ { "kind": "attributeValue", "label": "J1", "attribute": "heffing",
                              "value": { "number": "800" } },
                            { "kind": "consistent" } ] }
            """;

    /** The golden with the value of its characteristic initialisation replaced by the number 5. */
    static ObjectNode metGetal() throws Exception {
        ObjectNode ir = (ObjectNode) TmsDocumenten.JSON.readTree(Files.readString(GOLDEN.resolve("ir.json")));
        for (JsonNode t : IrPad.versie(ir, "objectCreations", 0, 0).get("initialisations"))
            if (t.get("characteristic").asBoolean())
                ((ObjectNode) t).set("value", TmsDocumenten.JSON.readTree(TmsDocumenten.getal("5")));
        return ir;
    }

    @Test void een_kenmerkinitialisatie_met_een_getal_wordt_met_naam_geweigerd() throws Exception {
        Path ir = Files.writeString(Files.createTempFile("tms-kenmerkinitialisatie", ".json"),
                TmsDocumenten.JSON.writeValueAsString(metGetal()));
        ir.toFile().deleteOnExit();
        assertEquals(List.of("transpiler SKIP: objectcreatie: richt de rekenkamer op — een kenmerkinitialisatie van"
                        + " 'overzees' met een waarde van type Numeriek ('5'): een kenmerk is waar of onwaar (WF-027), en de"
                        + " referentie-interpreter faalt op zo'n waarde"),
                TmsTijdlijnWeigeringWitnessTest.weigeringen(ir));
    }

    @Test void de_golden_met_een_waarheidswaarde_speelt() throws Exception {
        // the narrowness of the refusal: the golden itself, every case of it
        try (var cases = Files.list(GOLDEN)) {
            for (Path c : cases.filter(p -> p.getFileName().toString().startsWith("case")).sorted().toList())
                assertEquals("gespeeld", TmsFoutWitnessTest.speelBeide(GOLDEN.resolve("ir.json"),
                        TmsDocumenten.JSON.readTree(Files.readString(c))),
                        c.getFileName().toString());
        }
    }

    /** Writes the refused document with its case under {@code args[0]}. */
    public static void main(String[] args) throws Exception {
        TmsTijdlijnWitnessTest.schrijf(Path.of(args[0]).resolve("kenmerk-getal"),
                TmsDocumenten.JSON.writeValueAsString(metGetal()), TmsDocumenten.JSON.readTree(ZAAK));
    }
}
