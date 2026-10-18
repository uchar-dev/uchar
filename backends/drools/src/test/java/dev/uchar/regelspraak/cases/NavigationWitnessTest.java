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
class NavigationWitnessTest {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Path GOLDEN = Path.of("..", "..", "corpus", "golden", "jutterswet-dubbelverbinding");

    /** The vlagprijs rule: "het kadegeld van de vlaghaven van de Schuit", bound to vlagvoering.vlaghaven. */
    private static final int VLAGPRIJS = 4;

    // ---------------- the value read, in each of its three written shapes ----------------

    @Test void the_read_follows_the_wire_binding() throws Exception {
        // four relations between the pair: no derivation of the pair could name one, so the binding is the route
        ObjectNode ir = read();
        BothBranches.green(temporary("nav", ir), goldenCase(1), "the bound hop plays");
        value(ir).set("path", bindings("winterberging", "winterhaven"));
        BothBranches.red(temporary("nav-twist", ir), goldenCase(1), "verwacht leeg, werkelijk 40",
                "the hop must walk the bound winterhaven");
    }

    @Test void a_written_step_beside_the_binding_is_refused_by_name() throws Exception {
        String site = "/rules/rules/" + VLAGPRIJS + "/versions/0/value/";
        for (String field : new String[] { "navigatieRol", "navigatiePad", "onderwerpBron", "set", "type" }) {
            ObjectNode ir = read();
            switch (field) {
                case "navigatiePad" -> value(ir).putArray(field).add("vlaghaven");
                case "set" -> value(ir).put(field, false);
                case "type" -> value(ir).set(field, IrPath.attribute(IrPath.objectType(ir, "Haven"), "kadegeld").get("datatype"));
                default -> value(ir).put(field, "vlaghaven");
            }
            IrLoader.DocumentError e = assertThrows(IrLoader.DocumentError.class, () -> IrLoader.loadAll(ir), field);
            assertEquals(site + field, e.site(), e.getMessage());
            assertTrue(e.getMessage().contains("unknown field on AttributeReference"), e.getMessage());
        }
    }

    // ---------------- the navigated characteristic check ----------------

    @Test void the_navigated_characteristic_check_follows_the_wire_binding() throws Exception {
        ObjectNode ir = read();
        ObjectNode k = regel(ir).putObject("condition");
        k.put("node", "CharacteristicCheck").put("characteristic", "veilig").put("negated", false)
         .set("source", IrPath.sourceReference("zijn vlaghaven veilig is"));
        k.set("navigation", binding("vlagvoering", "vlaghaven"));
        BothBranches.green(temporary("nav-kenmerk", ir), goldenCase(2), "untwisted navigated check plays");
        k.set("navigation", binding("winterberging", "winterhaven"));
        BothBranches.red(temporary("nav-kenmerk-twist", ir), goldenCase(2), "verwacht 40, werkelijk leeg",
                "the check must read veilig on the bound winterhaven");
        k.put("navigatie", "vlaghaven");
        IrLoader.DocumentError e = assertThrows(IrLoader.DocumentError.class, () -> IrLoader.loadAll(ir));
        assertEquals("/rules/rules/" + VLAGPRIJS + "/versions/0/condition/navigatie", e.site(), e.getMessage());
        assertTrue(e.getMessage().contains("unknown field on CharacteristicCheck"), e.getMessage());
    }

    // ---------------- no silent fallback ----------------

    @Test void a_binding_that_does_not_fit_does_not_fall_back_on_a_derivation() throws Exception {
        // thuisligging.ligger is reached FROM a Haven, not from the Schuit this rule reads on. A branch that fell
        // back to a derivation of its own — the pair, the rule's text — would play. Both must refuse instead, for the
        // NAVIGATION — the rule name alone would let any refusal of this rule pass — in the one sentence of the
        // analysis; until card drl-teksttak-op-truth-maintenance the DRL-text branch said it in its own words ("TODO
        // navigatie: het kadegeld van de vlaghaven van de Schuit").
        ObjectNode ir = read();
        value(ir).set("path", bindings("thuisligging", "ligger"));
        BothBranches.refused(temporary("nav-past-niet", ir), goldenCase(1), "regel: vlagprijs van een schuit — de lezing 'het"
                + " kadegeld van de vlaghaven van de Schuit': de navigatie resolvet niet vanaf Schuit",
                "a binding that does not fit is refused for the navigation");
    }

    @Test void a_bound_pair_read_in_a_condition_is_no_own_read() throws Exception {
        // The own-read arms keyed on navigationRole == null alone. A bound by-pair read has no written step either, so
        // once the subject declared a same-named attribute the exec branch read Schuit.kadegeld (never filled)
        // instead of walking vlagvoering to the Haven. RED FIRST on the port before this row: exec
        // "attribuut vlagprijs: verwacht 40, werkelijk leeg" in three cases, DRL PLAYED 4, ref PASS, verifier PASS.
        ObjectNode ir = read();
        IrPath.addAttribute(IrPath.objectType(ir, "Schuit"), "kadegeld",
                IrPath.attribute(IrPath.objectType(ir, "Haven"), "kadegeld").get("datatype"));
        ObjectNode left = value(ir).deepCopy();
        ObjectNode v = regel(ir).putObject("condition");
        v.put("node", "Comparison").put("op", "greater");
        v.set("left", left);
        ObjectNode thirty = v.putObject("right").put("node", "Literal");
        thirty.putObject("value").put("number", "30");
        thirty.set("type", IrPath.attribute(IrPath.objectType(ir, "Haven"), "kadegeld").get("datatype"));
        thirty.set("source", IrPath.sourceReference("30"));
        v.set("source", IrPath.sourceReference("het kadegeld van de vlaghaven van de Schuit groter is dan 30"));
        BothBranches.green(temporary("nav-paar-voorwaarde", ir), goldenCase(1), "the bound read in the guard walks to the Haven");
    }

    @Test void the_endpoint_may_be_a_supertype_of_the_filler_not_a_subtype() throws Exception {
        // WF-053 and ref: the last bound role's filler must be the endpoint or a subtype of it (filler ⊑ endpoint).
        ObjectNode ir = read();
        ArrayNode types = IrPath.modelList(ir, "objectTypes");
        ObjectNode haven = IrPath.objectType(ir, "Haven");
        types.add(emptyType(haven, "Ligplaats", null));
        types.add(emptyType(haven, "Zeehaven", "Haven"));
        haven.put("supertype", "Ligplaats");
        RegelSpraakModel m = IrLoader.loadAll(ir).model();
        List<RegelSpraakModel.RoleBindingRef> step = List.of(new RegelSpraakModel.RoleBindingRef("vlagvoering", "vlaghaven"));
        RegelSpraakModel.Objecttype schuit = m.objectTypeByName("Schuit");
        assertNotNull(FactModelGenerator.navigationWalk(m, schuit, step, m.objectTypeByName("Haven")));
        assertNotNull(FactModelGenerator.navigationWalk(m, schuit, step, m.objectTypeByName("Ligplaats")),
                "a Haven is a Ligplaats, so every object the walk reaches may be addressed as one");
        assertNull(FactModelGenerator.navigationWalk(m, schuit, step, m.objectTypeByName("Zeehaven")),
                "not every Haven is a Zeehaven");
    }

    @Test void an_empty_bindings_list_is_refused_by_name() throws Exception {
        // an empty list would read as the subject's own attribute, so it is no document
        ObjectNode ir = read();
        value(ir).putArray("path");
        IrLoader.DocumentError e = assertThrows(IrLoader.DocumentError.class, () -> IrLoader.loadAll(ir));
        assertEquals("/rules/rules/" + VLAGPRIJS + "/versions/0/value/path", e.site(), e.getMessage());
    }

    @Test void a_malformed_binding_is_refused_by_name_on_loading() throws Exception {
        // the verifier and ref give LOAD ERROR naming the field, and so does this loader, at the binding's pointer
        ObjectNode ir = read();
        ((ObjectNode) value(ir).get("path").get(0)).remove("role");
        IrLoader.DocumentError e = assertThrows(IrLoader.DocumentError.class, () -> IrLoader.loadAll(ir));
        assertEquals("/rules/rules/" + VLAGPRIJS + "/versions/0/value/path/0", e.site(), e.getMessage());
        assertTrue(e.getMessage().contains("missing field 'role'"), e.getMessage());

        ObjectNode ir2 = read();
        ((ObjectNode) IrPath.version(ir2, "rules", 3, 0).get("condition")).putNull("navigation");
        IrLoader.DocumentError e2 = assertThrows(IrLoader.DocumentError.class, () -> IrLoader.loadAll(ir2));
        assertEquals("/rules/rules/3/versions/0/condition/navigation", e2.site(), e2.getMessage());
    }

    // ---------------- fixtures ----------------

    private static ObjectNode read() throws Exception {
        return (ObjectNode) JSON.readTree(Files.readString(GOLDEN.resolve("ir.json")));
    }

    /** The one version of the rule 'vlagprijs van een schuit'. */
    private static ObjectNode regel(ObjectNode ir) {
        JsonNode r = IrPath.rulesList(ir, "rules").get(VLAGPRIJS);
        assertTrue(r.get("name").asText().equals("vlagprijs van een schuit"), r.get("name").asText());
        return IrPath.version(ir, "rules", VLAGPRIJS, 0);
    }

    private static ObjectNode value(ObjectNode ir) { return (ObjectNode) regel(ir).get("value"); }

    private static JsonNode goldenCase(int n) throws Exception {
        try (var s = Files.list(GOLDEN)) {
            Path p = s.filter(f -> f.getFileName().toString().startsWith("case" + n + "-")).findFirst().orElseThrow();
            return JSON.readTree(Files.readString(p));
        }
    }

    /** A member-less object type shaped like {@code model}, with the given supertype. */
    private static ObjectNode emptyType(JsonNode model, String naam, String supertype) {
        ObjectNode t = model.deepCopy();
        t.put("name", naam).put("plural", naam + "s");
        if (supertype == null) t.remove("supertype"); else t.put("supertype", supertype);
        t.putArray("attributes");
        t.putArray("characteristics");
        return t;
    }

    private static ObjectNode binding(String factType, String role) {
        return JSON.createObjectNode().put("node", "RoleBinding").put("factType", factType).put("role", role);
    }

    private static ArrayNode bindings(String factType, String role) {
        return JSON.createArrayNode().add(binding(factType, role));
    }

    private static Path temporary(String naam, ObjectNode ir) throws Exception {
        Path tmp = Files.createTempFile(naam, ".json");
        tmp.toFile().deleteOnExit();
        Files.writeString(tmp, ir.toString());
        return tmp;
    }
}
