package io.github.yhavinga.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.github.yhavinga.regelspraak.loader.IrLoader;
import io.github.yhavinga.regelspraak.model.RegelSpraakModel;
import io.github.yhavinga.regelspraak.schema.FactModelGenerator;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
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
        // Each branch must refuse for the NAVIGATION, in its own words for it — the rule name alone would let any
        // refusal of this rule pass.
        ObjectNode ir = enkeleStap();
        waarde(ir).set("navigatieBindingen", bindingen("thuisligging", "ligger"));
        Path p = tijdelijk("nav-past-niet", ir);
        JsonNode c = casus(1);
        CaseRunner.Excluded exec = assertThrows(CaseRunner.Excluded.class, () -> CaseRunner.speelCase(p, c, false));
        assertTrue(exec.getMessage().contains("vlagprijs van een schuit — regel (waarde buiten scope)"), exec.getMessage());
        CaseRunner.Excluded drl = assertThrows(CaseRunner.Excluded.class, () -> CaseRunner.speelCase(p, c, true));
        assertTrue(drl.getMessage().contains("TODO navigatie: het kadegeld van de vlaghaven van de Schuit"), drl.getMessage());
    }

    @Test void een_gebonden_paarlezing_in_een_voorwaarde_is_geen_eigen_lezing() throws Exception {
        // The own-read arms keyed on navigatieRol == null alone. A bound by-pair read has no written step either, so
        // once the subject declared a same-named attribute the exec branch read Schuit.kadegeld (never filled)
        // instead of walking vlagvoering to the Haven. RED FIRST on the port before this row: exec
        // "attribuut vlagprijs: verwacht 40, werkelijk leeg" in three cases, DRL PLAYED 4, ref PASS, verifier PASS.
        ObjectNode ir = lees();
        IrPad.addAttribute(IrPad.objectType(ir, "Schuit"), "kadegeld",
                IrPad.attribute(IrPad.objectType(ir, "Haven"), "kadegeld").get("datatype"));
        ObjectNode links = waarde(ir).deepCopy();
        links.putNull("navigatieRol");
        links.putArray("navigatiePad");
        ObjectNode v = regel(ir).putObject("voorwaarde");
        v.put("node", "Vergelijking").put("op", "GROTER");
        v.set("links", links);
        ObjectNode dertig = v.putObject("rechts").put("node", "Literal").put("waarde", "30");
        dertig.set("type", links.get("type"));
        dertig.put("bron", "30");
        v.putNull("periode").put("bron", "het kadegeld van de vlaghaven van de Schuit groter is dan 30");
        BeideTakken.groen(tijdelijk("nav-paar-voorwaarde", ir), casus(1), "the bound read in the guard walks to the Haven");
    }

    @Test void het_eindpunt_mag_een_supertype_van_de_vuller_zijn_niet_een_subtype() throws Exception {
        // WF-053 and ref: the last bound role's filler must be the endpoint or a subtype of it (filler ⊑ endpoint).
        ObjectNode ir = lees();
        ArrayNode typen = IrPad.modelLijst(ir, "objectTypes");
        ObjectNode haven = IrPad.objectType(ir, "Haven");
        typen.add(leegType(haven, "Ligplaats", null));
        typen.add(leegType(haven, "Zeehaven", "Haven"));
        haven.put("supertype", "Ligplaats");
        RegelSpraakModel m = IrLoader.loadAll(ir).model();
        List<RegelSpraakModel.RolBindingRef> stap = List.of(new RegelSpraakModel.RolBindingRef("vlagvoering", "vlaghaven"));
        RegelSpraakModel.Objecttype schuit = m.objecttypeOpNaam("Schuit");
        assertNotNull(FactModelGenerator.navigatiewandeling(m, schuit, stap, m.objecttypeOpNaam("Haven")));
        assertNotNull(FactModelGenerator.navigatiewandeling(m, schuit, stap, m.objecttypeOpNaam("Ligplaats")),
                "a Haven is a Ligplaats, so every object the walk reaches may be addressed as one");
        assertNull(FactModelGenerator.navigatiewandeling(m, schuit, stap, m.objecttypeOpNaam("Zeehaven")),
                "not every Haven is a Zeehaven");
    }

    @Test void een_lege_bindingenlijst_wordt_bij_naam_geweigerd() throws Exception {
        ObjectNode ir = lees();
        waarde(ir).putArray("navigatieBindingen");
        IrLoader.Refusal r = assertThrows(IrLoader.Refusal.class, () -> IrLoader.loadAll(ir));
        assertTrue(r.form().equals("AttribuutRef.navigatieBindingen"), r.getMessage());
    }

    @Test void een_binding_op_een_niet_navigerende_kenmerkcheck_wordt_geweigerd() throws Exception {
        ObjectNode ir = lees();
        ObjectNode k = (ObjectNode) IrPad.rulesLijst(ir, "resolvedRegels").get(3).get("voorwaarde");   // "de Schuit gekeurd is"
        k.set("navigatieBinding", binding("vlagvoering", "vlaghaven"));
        IrLoader.Refusal r = assertThrows(IrLoader.Refusal.class, () -> IrLoader.loadAll(ir));
        assertTrue(r.form().equals("Kenmerk.navigatieBinding"), r.getMessage());
    }

    @Test void een_misvormde_binding_wordt_bij_het_laden_benoemd() throws Exception {
        // the verifier and ref give LOAD ERROR naming the field; before this row the Drools loader read the hop
        // leniently and the document surfaced only as a generic unresolved-navigation skip
        ObjectNode ir = lees();
        ((ObjectNode) waarde(ir).get("navigatieBindingen").get(0)).remove("rol");
        IrLoader.Refusal r = assertThrows(IrLoader.Refusal.class, () -> IrLoader.loadAll(ir));
        assertTrue(r.getMessage().contains("missing required field 'rol'"), r.getMessage());

        ObjectNode ir2 = lees();
        ((ObjectNode) IrPad.rulesLijst(ir2, "resolvedRegels").get(3).get("voorwaarde")).putNull("navigatieBinding");
        IrLoader.Refusal r2 = assertThrows(IrLoader.Refusal.class, () -> IrLoader.loadAll(ir2));
        assertTrue(r2.getMessage().contains("malformed"), "null is malformed before it is misplaced: " + r2.getMessage());
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
        ObjectNode r = (ObjectNode) IrPad.rulesLijst(ir, "resolvedRegels").get(VLAGPRIJS);
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

    /** A member-less object type shaped like {@code model}, with the given supertype. */
    private static ObjectNode leegType(JsonNode model, String naam, String supertype) {
        ObjectNode t = model.deepCopy();
        t.put("name", naam).put("plural", naam + "s");
        if (supertype == null) t.remove("supertype"); else t.put("supertype", supertype);
        t.putArray("attributes");
        t.putArray("characteristics");
        return t;
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
