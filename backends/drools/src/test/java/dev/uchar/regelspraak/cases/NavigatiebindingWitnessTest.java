package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import dev.uchar.regelspraak.loader.IrLoader;
import dev.uchar.regelspraak.model.RegelSpraakModel;
import dev.uchar.regelspraak.schema.FactModelGenerator;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The IR-SPEC ch. 4.1a navigation-binding witness: both Drools branches decide a navigation by the wire's
 * {@code path} / {@code navigation} and by nothing else — since L2 of the v1 wire migration a
 * read carries no written step beside its binding, and a read without bindings is the subject's own.
 *
 * <p>The rows twist ONE binding on an in-memory variant of {@code jutterswet-dubbelverbinding}, where four feittypen
 * relate Schuit and Haven, so the twisted binding still resolves — to the sister relation — and the reading is
 * observable in the values: a wrong binding gives a loud wrong answer or a refusal, never the old answer. The
 * written step the wire carried before L2 has nothing left to twist: written beside a binding it is an unknown
 * field, and the loader refuses the document by name at its pointer.
 */
class NavigatiebindingWitnessTest {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Path GOLDEN = Path.of("..", "..", "corpus", "golden", "jutterswet-dubbelverbinding");

    /** The vlagprijs rule: "het kadegeld van de vlaghaven van de Schuit", bound to vlagvoering.vlaghaven. */
    private static final int VLAGPRIJS = 4;

    // ---------------- the value read, in each of its three written shapes ----------------

    @Test void de_lezing_volgt_de_wire_binding() throws Exception {
        // four relations between the pair: no derivation of the pair could name one, so the binding is the route
        ObjectNode ir = lees();
        BeideTakken.groen(tijdelijk("nav", ir), casus(1), "the bound hop plays");
        waarde(ir).set("path", bindingen("winterberging", "winterhaven"));
        BeideTakken.rood(tijdelijk("nav-twist", ir), casus(1), "verwacht leeg, werkelijk 40",
                "the hop must walk the bound winterhaven");
    }

    @Test void een_geschreven_stap_naast_de_binding_wordt_bij_naam_geweigerd() throws Exception {
        String plek = "/rules/rules/" + VLAGPRIJS + "/versions/0/value/";
        for (String veld : new String[] { "navigatieRol", "navigatiePad", "onderwerpBron", "set", "type" }) {
            ObjectNode ir = lees();
            switch (veld) {
                case "navigatiePad" -> waarde(ir).putArray(veld).add("vlaghaven");
                case "set" -> waarde(ir).put(veld, false);
                case "type" -> waarde(ir).set(veld, IrPad.attribute(IrPad.objectType(ir, "Haven"), "kadegeld").get("datatype"));
                default -> waarde(ir).put(veld, "vlaghaven");
            }
            IrLoader.Documentfout e = assertThrows(IrLoader.Documentfout.class, () -> IrLoader.loadAll(ir), veld);
            assertEquals(plek + veld, e.plek(), e.getMessage());
            assertTrue(e.getMessage().contains("unknown field on AttributeReference"), e.getMessage());
        }
    }

    // ---------------- the navigated characteristic check ----------------

    @Test void de_genavigeerde_kenmerkcheck_volgt_de_wire_binding() throws Exception {
        ObjectNode ir = lees();
        ObjectNode k = regel(ir).putObject("condition");
        k.put("node", "CharacteristicCheck").put("characteristic", "veilig").put("negated", false)
         .set("source", IrPad.bronrecord("zijn vlaghaven veilig is"));
        k.set("navigation", binding("vlagvoering", "vlaghaven"));
        BeideTakken.groen(tijdelijk("nav-kenmerk", ir), casus(2), "untwisted navigated check plays");
        k.set("navigation", binding("winterberging", "winterhaven"));
        BeideTakken.rood(tijdelijk("nav-kenmerk-twist", ir), casus(2), "verwacht 40, werkelijk leeg",
                "the check must read veilig on the bound winterhaven");
        k.put("navigatie", "vlaghaven");
        IrLoader.Documentfout e = assertThrows(IrLoader.Documentfout.class, () -> IrLoader.loadAll(ir));
        assertEquals("/rules/rules/" + VLAGPRIJS + "/versions/0/condition/navigatie", e.plek(), e.getMessage());
        assertTrue(e.getMessage().contains("unknown field on CharacteristicCheck"), e.getMessage());
    }

    // ---------------- no silent fallback ----------------

    @Test void een_binding_die_niet_past_valt_niet_terug_op_een_afleiding() throws Exception {
        // thuisligging.ligger is reached FROM a Haven, not from the Schuit this rule reads on. A branch that fell
        // back to a derivation of its own — the pair, the rule's text — would play. Both must refuse instead, for the
        // NAVIGATION — the rule name alone would let any refusal of this rule pass — in the one sentence of the
        // analysis; until card drl-teksttak-op-truth-maintenance the DRL-text branch said it in its own words ("TODO
        // navigatie: het kadegeld van de vlaghaven van de Schuit").
        ObjectNode ir = lees();
        waarde(ir).set("path", bindingen("thuisligging", "ligger"));
        BeideTakken.geweigerd(tijdelijk("nav-past-niet", ir), casus(1), "regel: vlagprijs van een schuit — de lezing 'het"
                + " kadegeld van de vlaghaven van de Schuit': de navigatie resolvet niet vanaf Schuit",
                "a binding that does not fit is refused for the navigation");
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
        ObjectNode v = regel(ir).putObject("condition");
        v.put("node", "Comparison").put("op", "greater");
        v.set("left", links);
        ObjectNode dertig = v.putObject("right").put("node", "Literal");
        dertig.putObject("value").put("number", "30");
        dertig.set("type", IrPad.attribute(IrPad.objectType(ir, "Haven"), "kadegeld").get("datatype"));
        dertig.set("source", IrPad.bronrecord("30"));
        v.set("source", IrPad.bronrecord("het kadegeld van de vlaghaven van de Schuit groter is dan 30"));
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
        // an empty list would read as the subject's own attribute, so it is no document
        ObjectNode ir = lees();
        waarde(ir).putArray("path");
        IrLoader.Documentfout e = assertThrows(IrLoader.Documentfout.class, () -> IrLoader.loadAll(ir));
        assertEquals("/rules/rules/" + VLAGPRIJS + "/versions/0/value/path", e.plek(), e.getMessage());
    }

    @Test void een_misvormde_binding_wordt_bij_het_laden_benoemd() throws Exception {
        // the verifier and ref give LOAD ERROR naming the field, and so does this loader, at the binding's pointer
        ObjectNode ir = lees();
        ((ObjectNode) waarde(ir).get("path").get(0)).remove("role");
        IrLoader.Documentfout e = assertThrows(IrLoader.Documentfout.class, () -> IrLoader.loadAll(ir));
        assertEquals("/rules/rules/" + VLAGPRIJS + "/versions/0/value/path/0", e.plek(), e.getMessage());
        assertTrue(e.getMessage().contains("missing field 'role'"), e.getMessage());

        ObjectNode ir2 = lees();
        ((ObjectNode) IrPad.versie(ir2, "rules", 3, 0).get("condition")).putNull("navigation");
        IrLoader.Documentfout e2 = assertThrows(IrLoader.Documentfout.class, () -> IrLoader.loadAll(ir2));
        assertEquals("/rules/rules/3/versions/0/condition/navigation", e2.plek(), e2.getMessage());
    }

    // ---------------- fixtures ----------------

    private static ObjectNode lees() throws Exception {
        return (ObjectNode) JSON.readTree(Files.readString(GOLDEN.resolve("ir.json")));
    }

    /** The one version of the rule 'vlagprijs van een schuit'. */
    private static ObjectNode regel(ObjectNode ir) {
        JsonNode r = IrPad.rulesLijst(ir, "rules").get(VLAGPRIJS);
        assertTrue(r.get("name").asText().equals("vlagprijs van een schuit"), r.get("name").asText());
        return IrPad.versie(ir, "rules", VLAGPRIJS, 0);
    }

    private static ObjectNode waarde(ObjectNode ir) { return (ObjectNode) regel(ir).get("value"); }

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
        return JSON.createObjectNode().put("node", "RoleBinding").put("factType", feittype).put("role", rol);
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
