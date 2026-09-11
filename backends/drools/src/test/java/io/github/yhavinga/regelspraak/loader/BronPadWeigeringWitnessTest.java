package io.github.yhavinga.regelspraak.loader;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The {@code bronPad} refusal witness (IR-SPEC ch. 4.3 / WF-102, maintainer decision 2026-09-11):
 * a MULTI-STEP fold-source range is refused by NAME, per form, and never silently dropped.
 *
 * <p>Why the refusal has to be explicit, and why a corpus golden cannot pin it alone. This loader
 * ignores most emitter-written role extensions on purpose — the transpiler re-derives the join from
 * the model — and {@code bronPad} looks like one more of those. It is the opposite: the model
 * CANNOT re-derive a multi-step range, and the by-pair search the transpiler falls back on will
 * bind a coincidental DIRECT relation between the fold subject and the element type and fold the
 * wrong set, while ref and the ASP backend fold the right one. That is precisely the silent
 * misbinding the field was introduced to close, so an ignored field would reopen it in this
 * executor alone.
 *
 * <p>The corpus anchor ({@code anchor/jutterswet-bootberging}) does NOT have such a direct relation,
 * so before this refusal it excluded anyway — through a join derivation that merely happened to
 * fail, with a generic scope note. That is the shape this test exists for: it builds the document
 * the corpus deliberately does not carry (the path PLUS a direct subject-to-element relation) and
 * shows that the two differ only in the reason before the refusal and not at all after it.
 */
class BronPadWeigeringWitnessTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    // cwd under surefire is the module dir (backends/drools); the corpus is two levels up.
    private static final Path CORPUS = Path.of("..", "..", "corpus", "golden");

    @Test void een_bronpad_wordt_per_vorm_geweigerd() throws Exception {
        JsonNode root = anker();
        IrLoader.Refusal r = assertThrows(IrLoader.Refusal.class, () -> IrLoader.load(root),
                "een bronPad hoort een benoemde per-vorm-weigering te zijn, geen genegeerd veld");
        assertEquals("Functie.bronPad", r.form(),
                "de weigering noemt de vorm, zodat gate 12 haar met reden in de kanaalvloer zet");
        assertTrue(r.getMessage().contains("WF-102"),
                "de reden verwijst naar de regel die de vorm definieert:\n" + r.getMessage());
    }

    /**
     * The same document plus a DIRECT relation between the fold subject and the element type — the
     * shape on which ignoring the field would answer, and answer wrongly. It must refuse for the
     * SAME reason as the bare anchor: the refusal keys on the field, never on whether a by-pair
     * fallback happens to be available.
     */
    @Test void ook_met_een_direct_feittype_blijft_het_een_weigering() throws Exception {
        ObjectNode root = (ObjectNode) anker();
        ObjectNode ft = (ObjectNode) root.get("feittypen");
        ObjectNode aanmelding = ft.putObject("vondstaanmelding");
        aanmelding.put("node", "Feittype").put("naam", "vondstaanmelding").put("wederkerig", false);
        ArrayNode rollen = aanmelding.putArray("rollen");
        rol(rollen, "aanmelder", "Jutter", "aanmelders", false);
        rol(rollen, "aangemelde vondst", "Vondst", "aangemelde vondsten", true);

        IrLoader.Refusal r = assertThrows(IrLoader.Refusal.class, () -> IrLoader.load(root));
        assertEquals("Functie.bronPad", r.form(),
                "een toevallig direct feittype mag de weigering niet omzeilen — dat is juist de stille "
                        + "misbinding die het veld sluit");
    }

    /** Without the field the very same document loads: the refusal is the FIELD's, not the model's. */
    @Test void zonder_het_veld_laadt_hetzelfde_document() throws Exception {
        ObjectNode root = (ObjectNode) anker();
        for (JsonNode regel : root.get("resolvedRegels"))
            ((ObjectNode) regel.get("waarde")).remove("bronPad");
        assertNotNull(IrLoader.load(root), "zonder bronPad is er niets onbereikbaars aan dit document");
    }

    private static void rol(ArrayNode rollen, String naam, String otn, String mv, boolean meervoudig) {
        rollen.addObject().put("node", "Rol").put("naam", naam).put("objecttypeNaam", otn)
                .put("meervoud", mv).put("meervoudig", meervoudig);
    }

    private static JsonNode anker() throws Exception {
        Path ir = CORPUS.resolve("anchor/jutterswet-bootberging").resolve("ir.json");
        assertTrue(Files.exists(ir), "golden ir.json present: " + ir.toAbsolutePath());
        JsonNode root = JSON.readTree(Files.readString(ir));
        assertTrue(root.path("resolvedRegels").path(0).path("waarde").hasNonNull("bronPad"),
                "de ankergolden draagt het veld waar deze getuige over gaat");
        return root;
    }
}
