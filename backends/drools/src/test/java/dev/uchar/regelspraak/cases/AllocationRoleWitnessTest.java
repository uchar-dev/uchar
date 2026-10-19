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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The verdeling-role witness (V1-WIREMIGRATIE P3): both Drools branches join a §9.7 verdeling's recipients on
 * the relation its wire {@code connectingRole} binds (IR-SPEC ch. 3.2), through
 * {@link FactModelGenerator#allocationJoin}; since L2 of the v1 wire migration the binding is required, and a
 * verdeling without one does not load.
 *
 * <p>On a uniquely connected pair the binding and the pair agree, and every golden but one is such a pair, so
 * the corpus alone cannot tell which decides. {@code anchor/jutterswet-bergingstoezicht} is the one: Berging and
 * Berger are related twice ({@code bergingsdeelname}, {@code bergingstoezicht}), the pair answers nothing, and
 * the binding is the only route. RED FIRST, measured 2026-09-22 on the source of main 788d355b (before the
 * port), in both branches: the repeated-pair rows were refused as "onopgelost naar_rato" whatever the binding
 * said, and the three rows that twist the binding on {@code jutterswet-berging} (source role bound, dangling
 * role) played the pinned answer, the twist unread. Only the bindingless row was green then as now.
 *
 * <p>ref and the verifier per row, measured the same day: the repeated-pair document, its reserve variant and
 * the reserve twist are verifier-clean; ref plays the first two and gives exactly the MISMATCH pinned below for
 * the twist. The toezicht twist is verifier-clean too, and ref faults on it (no fixpoint), where both branches
 * refuse it by the storage direction. The source-role and dangling twists fail the verifier on WF-047; ref reads
 * an empty recipient set on the first and faults on the second.
 *
 * <p>The reciprocal row (added in the fix round) was red-first against a mutant that dropped
 * {@code boundRolePair}'s wederkerig arm to null: the derivation assertion failed with the dangling-binding
 * refusal, and passed again once the arm was restored.
 */
class AllocationRoleWitnessTest {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Path CORPUS = Path.of("..", "..", "corpus", "golden");
    private static final Path TOEZICHT = CORPUS.resolve("anchor").resolve("jutterswet-bergingstoezicht");
    private static final Path BERGING = CORPUS.resolve("jutterswet-berging");
    private static final Path MAATSCHAP = CORPUS.resolve("anchor").resolve("jutterswet-maatschap");

    // ---------------- the repeated pair: only the binding answers ----------------

    @Test void the_derivation_reads_the_binding_where_the_type_pair_is_silent() throws Exception {
        IrLoader.Loaded l = IrLoader.loadAll(read(TOEZICHT));
        RegelSpraakModel m = l.model();
        var v = m.allocations.get(0);
        assertNull(m.connectingRole(v.targetType(), v.sourceType()), "two fact types relate Berger and Berging");
        assertEquals(new FactModelGenerator.AllocationJoin("berging", null),
                FactModelGenerator.allocationJoin(m, v, l.roleBindings().get(v)),
                "the join is the sibling of the bound recipient role 'berger'");
    }

    @Test void a_repeated_type_pair_plays_on_the_binding() throws Exception {
        BothBranches.green(TOEZICHT.resolve("ir.json"), goldenCase(TOEZICHT), "the bound bergingsdeelname relation decides the recipients");
    }

    @Test void with_a_third_relation_plays_the_untwisted_binding() throws Exception {
        BothBranches.green(temporary("reserve", reserve()), reserveCase(), "the extra relation is not the bound one");
    }

    @Test void the_recipients_follow_the_twisted_binding() throws Exception {
        // reservedeelname links B1 to J2 alone: B1's 100 goes to J2, and J1 receives nothing (ref: the same cells)
        ObjectNode ir = reserve();
        allocation(ir).set("connectingRole", binding("reservedeelname", "reserveberger"));
        BothBranches.red(temporary("reserve-twist", ir), reserveCase(), "verwacht 60, werkelijk leeg",
                "the recipients must be those of the bound reservedeelname relation");
    }

    @Test void a_binding_with_many_sister_is_refused_on_the_storage_direction() throws Exception {
        // bergingstoezicht binds Berger as toezichthouder; the source side 'bewaakte berging' is to-many, so the
        // relation is stored on Berging and Berger carries no field to join on
        ObjectNode ir = read(TOEZICHT);
        allocation(ir).set("connectingRole", binding("bergingstoezicht", "toezichthouder"));
        BothBranches.refused(temporary("toezicht-twist", ir), goldenCase(TOEZICHT),
                "verdeling: verdeel de opbrengst van een berging — een verdeling over de enkelvoudige ontvangerrol"
                        + " 'toezichthouder' wiens zusterrol 'bewaakte berging' meervoudig is", "the join follows the bound relation");
    }

    @Test void a_reciprocal_binding_joins_on_the_bound_role_itself() throws Exception {
        // maatschap is wederkerig: its one role 'maat' is its own sibling, so the join field is 'maat' itself.
        // A verdeling of vondstwaarde over the maat, added to jutterswet-maatschap: J1 and J2 are each other's
        // maat, so each receives the other's whole vondstwaarde; J3 has none. Verifier PASS, ref PASS.
        ObjectNode ir = maatschapWithAllocation();
        IrLoader.Loaded l = IrLoader.loadAll(ir);
        var v = l.model().allocations.get(0);
        assertEquals(new FactModelGenerator.AllocationJoin("maat", null),
                FactModelGenerator.allocationJoin(l.model(), v, l.roleBindings().get(v)),
                "a reciprocal role is its own sibling");
        ObjectNode c = (ObjectNode) JSON.readTree(Files.readString(MAATSCHAP.resolve("case1.json")));
        ArrayNode expected = (ArrayNode) c.get("expected");
        expected.insert(expected.size() - 1, measurePart("J1", "12"));
        expected.insert(expected.size() - 1, measurePart("J2", "30"));
        BothBranches.green(temporary("maatschap", ir), c, "the reciprocal binding joins on the maat field");
    }

    // ---------------- a unique pair: the twist is read, not overruled ----------------

    @Test void a_binding_on_the_source_role_is_not_overwritten_by_the_type_pair() throws Exception {
        ObjectNode ir = read(BERGING);
        allocation(ir).set("connectingRole", binding("bergingsdeelname", "berging"));
        BothBranches.refused(temporary("bronrol", ir), goldenCase(BERGING),
                "verdeling: verdeel de opbrengst van een berging — de join van de verdeling heeft geen beanveld — het"
                        + " beanmodel leidt voor de rol 'berging' op Berging geen referentieveld naar Berger af", "the bound role, not the pair, names the join");
    }

    @Test void a_binding_that_does_not_resolve_is_refused() throws Exception {
        ObjectNode ir = read(BERGING);
        allocation(ir).set("connectingRole", binding("bergingsdeelname", "bewaker"));
        BothBranches.refused(temporary("los", ir), goldenCase(BERGING), "verdeling: verdeel de opbrengst van een"
                        + " berging — de connectingRole van de verdeling noemt geen gedeclareerde rol (bergingsdeelname,"
                        + " bewaker)",
                "a dangling binding is refused, not replaced by the pair");
    }

    @Test void without_binding_the_allocation_does_not_load() throws Exception {
        // IR-SPEC ch. 3.2: the recipient join IS the binding, and no reader derives a missing one from the type
        // pair, unique or not — the document is refused at load, by name
        ObjectNode ir = read(BERGING);
        allocation(ir).remove("connectingRole");
        IrLoader.DocumentError e = assertThrows(IrLoader.DocumentError.class, () -> IrLoader.loadAll(ir));
        assertEquals("/rules/allocations/0/versions/0", e.site(), e.getMessage());
        assertTrue(e.getMessage().contains("missing field 'connectingRole'"), e.getMessage());
    }

    // ---------------- fixtures ----------------

    private static ObjectNode maatschapWithAllocation() throws Exception {
        ObjectNode ir = read(MAATSCHAP);
        ObjectNode jutter = IrPath.objectType(ir, "Jutter");
        IrPath.addAttribute(jutter, "maatdeel", IrPath.attribute(jutter, "maatwaarde").get("datatype").deepCopy());
        ObjectNode form = IrPath.rules(ir).putArray("allocations").addObject();
        form.put("node", "Allocation").put("name", "verdeel de vondstwaarde over de maat");
        ObjectNode v = form.putArray("versions").addObject();
        v.put("sourceType", "Jutter").put("sourceAttribute", "vondstwaarde")
                .put("targetType", "Jutter").put("targetAttribute", "maatdeel").put("method", "equalShares")
                .put("descending", false);
        v.putObject("validity").put("node", "Validity");
        v.set("connectingRole", binding("maatschap", "maat"));
        return ir;
    }

    private static ObjectNode measurePart(String label, String value) {
        ObjectNode e = JSON.createObjectNode().put("kind", "attributeValue").put("label", label).put("attribute", "maatdeel");
        e.putObject("value").put("number", value);
        return e;
    }

    /** bergingstoezicht with a third Berging-Berger relation, whose to-one side sits on Berger. */
    private static ObjectNode reserve() throws Exception {
        ObjectNode ir = read(TOEZICHT);
        ObjectNode ft = JSON.createObjectNode().put("node", "FactType").put("name", "reservedeelname").put("reciprocal", false);
        ArrayNode roles = ft.putArray("roles");
        roles.addObject().put("node", "Role").put("name", "reserveberging").put("objectType", "Berging")
                .put("many", false);
        roles.addObject().put("node", "Role").put("name", "reserveberger").put("objectType", "Berger")
                .put("plural", "reservebergers").put("many", true);
        IrPath.modelList(ir, "factTypes").add(ft);
        return ir;
    }

    private static JsonNode reserveCase() throws Exception {
        ObjectNode c = (ObjectNode) goldenCase(TOEZICHT);
        for (JsonNode i : c.get("input").get("instances"))
            if (i.get("label").asText().equals("B1"))
                ((ObjectNode) i.get("roles")).putArray("reservebergers").add("J2");
        return c;
    }

    /** The one version of the document's one allocation. */
    private static ObjectNode allocation(ObjectNode ir) {
        return IrPath.version(ir, "allocations", 0, 0);
    }

    private static ObjectNode binding(String factType, String role) {
        return JSON.createObjectNode().put("node", "RoleBinding").put("factType", factType).put("role", role);
    }

    private static ObjectNode read(Path golden) throws Exception {
        return (ObjectNode) JSON.readTree(Files.readString(golden.resolve("ir.json")));
    }

    private static JsonNode goldenCase(Path golden) throws Exception {
        return JSON.readTree(Files.readString(golden.resolve("case.json")));
    }

    private static Path temporary(String name, ObjectNode ir) throws Exception {
        Path tmp = Files.createTempFile(name, ".json");
        tmp.toFile().deleteOnExit();
        Files.writeString(tmp, ir.toString());
        return tmp;
    }
}
