package io.github.yhavinga.regelspraak.loader;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A {@code Functie.kind} this backend's IR records do not carry is a NAMED per-form
 * {@link IrLoader.Refusal}, never {@code Enum.valueOf}'s unattributed
 * {@code IllegalArgumentException} — the same answer {@code Kwantorvouw.kwantor} already gives an
 * unknown quantifier token.
 *
 * <p><b>Why this matters before any such document is in the corpus.</b> The distinction is not
 * cosmetic: gate 12 groups a Refusal as a reasoned per-form channel and prints its sentence, while an
 * unattributed runtime exception is an ERROR on the golden — a red gate whose report names a Java
 * class and no wire form.
 *
 * <p>The live case used to be {@code AANTAL_GEVULDE}, the filled-value count (IR-SPEC ch. 4.3,
 * RUNTIME-SEMANTICS ch. 3.4c), which went onto the wire before any consumer here carried it. Both
 * transpiler branches carry it from 2026-09-20, so the kind no longer reaches this refusal —
 * exactly what the wire-first order predicted, and the reason the arm was written for the CLASS
 * rather than for that one kind.
 *
 * <p>The witness therefore re-spells the fold as {@code GEMIDDELDE}, which is not an arbitrary
 * token: it is the average fold maintainer decision G1 of 2026-09-19 deliberately did NOT build,
 * leaving {@code SOM ÷ AANTAL_GEVULDE} hand-spelled. So the unknown kind named here is a DECIDED
 * absence rather than a typo, and the day that decision is revisited this witness needs a new
 * stand-in — which is the right moment to be reminded of it.
 */
class OnbekendeFunctieKindWitnessTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    // cwd under surefire is the module dir (backends/drools); the corpus is two levels up.
    private static final Path CORPUS = Path.of("..", "..", "corpus", "golden");

    @Test void een_niet_gedragen_vouwsoort_is_een_benoemde_weigering() throws Exception {
        ObjectNode root = (ObjectNode) golden();
        ((ObjectNode) vouw(root)).put("kind", "GEMIDDELDE");

        IrLoader.Refusal r = assertThrows(IrLoader.Refusal.class, () -> IrLoader.load(root),
                "een vouwsoort die dit backend niet draagt hoort een benoemde per-vorm-weigering te zijn,"
                        + " geen onbeheerde IllegalArgumentException");
        assertEquals("Functie.kind", r.form(),
                "de weigering noemt de vorm, zodat gate 12 haar met reden in het kanaalverslag zet");
        assertTrue(r.getMessage().contains("GEMIDDELDE"),
                "en zij noemt de soort die arriveerde, niet alleen de positie: " + r.getMessage());
    }

    /** The control: untouched, the same document loads. The refusal above is about the kind and
     *  nothing else. */
    @Test void dezelfde_golden_laadt_ongewijzigd() throws Exception {
        assertNotNull(IrLoader.load(golden()), "de onaangeraakte golden laadt gewoon");
    }

    /** The count of {@code jutterswet-vondsttelling}, the fold this witness re-spells. */
    private static JsonNode vouw(JsonNode root) {
        return root.get("resolvedRegels").get(0).get("waarde");
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
