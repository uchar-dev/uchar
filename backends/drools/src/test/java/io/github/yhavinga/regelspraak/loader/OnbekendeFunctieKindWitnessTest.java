package io.github.yhavinga.regelspraak.loader;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.github.yhavinga.regelspraak.cases.IrPad;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A function kind outside its node's set is refused BY NAME, never through {@code Enum.valueOf}'s unattributed
 * {@code IllegalArgumentException}. Since L4 of the v1 wire migration the kind set is closed per node (IR-SPEC
 * ch. 4.3): an {@code Aggregation} carries one of the eight fold kinds and a {@code BuiltinCall} one of the
 * seventeen others, so a kind outside that set — a kind of the other node included — is a malformed document, a
 * {@link IrLoader.Documentfout} at the kind's pointer in the shared loader's sentence ({@code unknown kind value
 * "<v>"}), where until L4 an open-ended kind was a per-form {@link IrLoader.Refusal} the gate filed as not carried.
 *
 * <p>The stand-in is {@code GEMIDDELDE}, which is not an arbitrary token: it is the average fold maintainer decision
 * G1 of 2026-09-19 deliberately did NOT build, leaving {@code SOM ÷ AANTAL_GEVULDE} hand-spelled. So the unknown kind
 * named here is a DECIDED absence rather than a typo, and the day that decision is revisited this witness needs a
 * new stand-in — which is the right moment to be reminded of it.
 */
class OnbekendeFunctieKindWitnessTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    // cwd under surefire is the module dir (backends/drools); the corpus is two levels up.
    private static final Path CORPUS = Path.of("..", "..", "corpus", "golden");

    @Test void een_onbekende_vouwsoort_is_een_benoemde_documentfout() throws Exception {
        ObjectNode root = (ObjectNode) golden();
        ((ObjectNode) vouw(root)).put("kind", "GEMIDDELDE");
        IrLoader.Documentfout e = assertThrows(IrLoader.Documentfout.class, () -> IrLoader.load(root),
                "een vouwsoort buiten de gesloten verzameling hoort een benoemde documentfout te zijn,"
                        + " geen onbeheerde IllegalArgumentException");
        assertEquals(KIND, e.plek(), e.getMessage());
        assertTrue(e.getMessage().endsWith(": unknown kind value \"GEMIDDELDE\""), e.getMessage());
    }

    /** A kind of the other function node is as unknown as a kind of none: the node decides the set. */
    @Test void een_soort_van_de_andere_knoop_is_onbekend() throws Exception {
        ObjectNode root = (ObjectNode) golden();
        ((ObjectNode) vouw(root)).put("kind", "WORTEL");
        IrLoader.Documentfout e = assertThrows(IrLoader.Documentfout.class, () -> IrLoader.load(root));
        assertEquals(KIND, e.plek(), e.getMessage());
        assertTrue(e.getMessage().endsWith(": unknown kind value \"WORTEL\""), e.getMessage());
    }

    /** The control: untouched, the same document loads. The refusal above is about the kind and
     *  nothing else. */
    @Test void dezelfde_golden_laadt_ongewijzigd() throws Exception {
        assertNotNull(IrLoader.load(golden()), "de onaangeraakte golden laadt gewoon");
    }

    /** The pointer of the kind of the fold this witness re-spells. */
    private static final String KIND = "/rules/resolvedRegels/0/versions/0/value/kind";

    /** The count of {@code jutterswet-vondsttelling}, the fold this witness re-spells. */
    private static JsonNode vouw(JsonNode root) {
        return IrPad.versie(root, "resolvedRegels", 0, 0).get("value");
    }

    private static JsonNode golden() throws Exception {
        Path ir = CORPUS.resolve("jutterswet-vondsttelling").resolve("ir.json");
        assertTrue(Files.exists(ir), "golden ir.json present: " + ir.toAbsolutePath());
        JsonNode root = JSON.readTree(Files.readString(ir));
        assertEquals("AANTAL", vouw(root).get("kind").asText(),
                "deze getuige verspelt de soort van deze vouw, dus zij moet hem eerst vinden");
        return root;
    }
}
