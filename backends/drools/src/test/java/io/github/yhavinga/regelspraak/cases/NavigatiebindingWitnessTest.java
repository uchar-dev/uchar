package io.github.yhavinga.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.github.yhavinga.regelspraak.loader.IrLoader;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The IR-SPEC ch. 4.1a navigation-binding witness (V1-WIREMIGRATIE P1): both Drools branches decide a BOUND
 * navigation by the wire's {@code navigatieBindingen} / {@code navigatieBinding}, not by re-deriving it from the
 * written {@code navigatieRol} / {@code navigatiePad} — the v0 fields migration step B11 removes.
 *
 * <p>The written fields and the bindings agree on every corpus document the frontend emits, so a corpus sweep
 * cannot tell which one a branch reads. The rows below can: each twists ONE binding on an in-memory variant of
 * {@code jutterswet-dubbelverbinding}, where four feittypen relate Schuit and Haven, so the twisted binding still
 * resolves — to the sister relation — and the reading is observable in the values. RED FIRST, measured
 * 2026-09-22 on the jar built from main 10b1d74a (before the port): every twist below played PLAYED 4 in both
 * branches (the twist ignored), where ref answers the twisted walk, e.g. on the chain twist
 * {@code MISMATCH: S1.vlagprijs = leeg, expected 40}; and the by-pair row was refused by both branches
 * ({@code regel (waarde buiten scope)} / {@code TODO navigatie}) where ref plays it.
 *
 * <p>The twisted documents with a written step fail the verifier (WF-053: the written step must denote the
 * bound role); that is the point of the probe, not a defect of it. The by-pair twist carries no written step
 * and is verifier-clean: there the wire is the only thing that names the relation.
 */
class NavigatiebindingWitnessTest {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Path GOLDEN = Path.of("..", "..", "corpus", "golden", "jutterswet-dubbelverbinding");

    /** The vlagprijs rule: "het kadegeld van de vlaghaven van de Schuit", bound to vlagvoering.vlaghaven. */
    private static final int VLAGPRIJS = 4;

    // ---------------- the value read, in each of its three written shapes ----------------

    @Test void de_keten_volgt_de_wire_binding() throws Exception {
        // the golden's own shape: a one-step navigatiePad
        ObjectNode ir = lees();
        waarde(ir).set("navigatieBindingen", bindingen("winterberging", "winterhaven"));
        BeideTakken.rood(tijdelijk("nav-keten-twist", ir), casus(1), "verwacht leeg, werkelijk 40",
                "the chain must walk the bound winterhaven, not the written vlaghaven");
    }

    @Test void de_enkele_stap_op_naam_volgt_de_wire_binding() throws Exception {
        ObjectNode ir = enkeleStap();
        BeideTakken.groen(tijdelijk("nav-rol", ir), casus(1), "untwisted single hop plays");
        waarde(ir).set("navigatieBindingen", bindingen("winterberging", "winterhaven"));
        BeideTakken.rood(tijdelijk("nav-rol-twist", ir), casus(1), "verwacht leeg, werkelijk 40",
                "the single hop must read the bound role, not the written name");
    }

    @Test void de_stap_op_paar_leest_alleen_de_wire() throws Exception {
        // No written step, four relations between the pair: the by-pair derivation is ambiguous by construction,
        // so before the port both branches refused this verifier-clean document. The binding is the only route.
        ObjectNode ir = lees();
        waarde(ir).putNull("navigatieRol");
        waarde(ir).putArray("navigatiePad");
        BeideTakken.groen(tijdelijk("nav-paar", ir), casus(1), "the bound by-pair hop plays");
        waarde(ir).set("navigatieBindingen", bindingen("winterberging", "winterhaven"));
        BeideTakken.rood(tijdelijk("nav-paar-twist", ir), casus(1), "verwacht leeg, werkelijk 40",
                "the by-pair hop must follow the binding");
    }

    // ---------------- the navigated characteristic check ----------------

    @Test void de_genavigeerde_kenmerkcheck_volgt_de_wire_binding() throws Exception {
        ObjectNode ir = lees();
        ObjectNode k = regel(ir).putObject("voorwaarde");
        k.put("node", "Kenmerk").put("kenmerk", "veilig").put("navigatie", "vlaghaven").put("negatie", false)
         .putNull("eigenaar").putNull("periode").put("bron", "zijn vlaghaven veilig is");
        k.set("navigatieBinding", binding("vlagvoering", "vlaghaven"));
        BeideTakken.groen(tijdelijk("nav-kenmerk", ir), casus(2), "untwisted navigated check plays");
        k.set("navigatieBinding", binding("winterberging", "winterhaven"));
        BeideTakken.rood(tijdelijk("nav-kenmerk-twist", ir), casus(2), "verwacht 40, werkelijk leeg",
                "the check must read veilig on the bound winterhaven");
    }

    // ---------------- no silent fallback ----------------

    @Test void een_binding_die_niet_past_valt_niet_terug_op_de_geschreven_naam() throws Exception {
        // thuisligging.ligger is reached FROM a Haven, not from the Schuit this rule reads on. The written name
        // vlaghaven would resolve; a branch that fell back to it would play. Both must refuse instead.
        ObjectNode ir = enkeleStap();
        waarde(ir).set("navigatieBindingen", bindingen("thuisligging", "ligger"));
        BeideTakken.geweigerd(tijdelijk("nav-past-niet", ir), casus(1), "vlagprijs van een schuit",
                "a binding that does not fit the subject is refused, never re-read by name");
    }

    @Test void een_lege_bindingenlijst_wordt_bij_naam_geweigerd() throws Exception {
        ObjectNode ir = lees();
        waarde(ir).putArray("navigatieBindingen");
        IrLoader.Refusal r = assertThrows(IrLoader.Refusal.class, () -> IrLoader.loadAll(ir));
        assertTrue(r.form().equals("AttribuutRef.navigatieBindingen"), r.getMessage());
    }

    @Test void een_binding_op_een_niet_navigerende_kenmerkcheck_wordt_geweigerd() throws Exception {
        ObjectNode ir = lees();
        ObjectNode k = (ObjectNode) ir.get("resolvedRegels").get(3).get("voorwaarde");   // "de Schuit gekeurd is"
        k.set("navigatieBinding", binding("vlagvoering", "vlaghaven"));
        IrLoader.Refusal r = assertThrows(IrLoader.Refusal.class, () -> IrLoader.loadAll(ir));
        assertTrue(r.form().equals("Kenmerk.navigatieBinding"), r.getMessage());
    }

    // ---------------- fixtures ----------------

    private static ObjectNode lees() throws Exception {
        return (ObjectNode) JSON.readTree(Files.readString(GOLDEN.resolve("ir.json")));
    }

    /** The vlagprijs read in the single-hop by-name shape: written role, no chain. */
    private static ObjectNode enkeleStap() throws Exception {
        ObjectNode ir = lees();
        waarde(ir).put("navigatieRol", "vlaghaven");
        waarde(ir).putArray("navigatiePad");
        return ir;
    }

    private static ObjectNode regel(ObjectNode ir) {
        ObjectNode r = (ObjectNode) ir.get("resolvedRegels").get(VLAGPRIJS);
        assertTrue(r.get("naam").asText().equals("vlagprijs van een schuit"), r.get("naam").asText());
        return r;
    }

    private static ObjectNode waarde(ObjectNode ir) { return (ObjectNode) regel(ir).get("waarde"); }

    private static JsonNode casus(int n) throws Exception {
        try (var s = Files.list(GOLDEN)) {
            Path p = s.filter(f -> f.getFileName().toString().startsWith("case" + n + "-")).findFirst().orElseThrow();
            return JSON.readTree(Files.readString(p));
        }
    }

    private static ObjectNode binding(String feittype, String rol) {
        return JSON.createObjectNode().put("node", "RolBinding").put("feittype", feittype).put("rol", rol);
    }

    private static ArrayNode bindingen(String feittype, String rol) {
        return JSON.createArrayNode().add(binding(feittype, rol));
    }

    private static Path tijdelijk(String naam, ObjectNode ir) throws Exception {
        Path tmp = Files.createTempFile(naam, ".json");
        tmp.toFile().deleteOnExit();
        Files.writeString(tmp, ir.toString());
        return tmp;
    }
}
