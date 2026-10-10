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
 * {@link FactModelGenerator#verdelingsjoin}; since L2 of the v1 wire migration the binding is required, and a
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
 * {@code gebondenRolpaar}'s wederkerig arm to null: the derivation assertion failed with the dangling-binding
 * refusal, and passed again once the arm was restored.
 */
class VerdelingsrolWitnessTest {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Path CORPUS = Path.of("..", "..", "corpus", "golden");
    private static final Path TOEZICHT = CORPUS.resolve("anchor").resolve("jutterswet-bergingstoezicht");
    private static final Path BERGING = CORPUS.resolve("jutterswet-berging");
    private static final Path MAATSCHAP = CORPUS.resolve("anchor").resolve("jutterswet-maatschap");

    // ---------------- the repeated pair: only the binding answers ----------------

    @Test void de_afleiding_leest_de_binding_waar_het_typenpaar_zwijgt() throws Exception {
        IrLoader.Loaded l = IrLoader.loadAll(lees(TOEZICHT));
        RegelSpraakModel m = l.model();
        var v = m.verdelingen.get(0);
        assertNull(m.verbindendeRol(v.doelType(), v.bronType()), "two fact types relate Berger and Berging");
        assertEquals(new FactModelGenerator.Verdelingsjoin("berging", null),
                FactModelGenerator.verdelingsjoin(m, v, l.rolBindingen().get(v)),
                "the join is the sibling of the bound recipient role 'berger'");
    }

    @Test void een_herhaald_typenpaar_speelt_op_de_binding() throws Exception {
        BeideTakken.groen(TOEZICHT.resolve("ir.json"), casus(TOEZICHT), "the bound bergingsdeelname relation decides the recipients");
    }

    @Test void met_een_derde_relatie_speelt_de_ongetwiste_binding() throws Exception {
        BeideTakken.groen(tijdelijk("reserve", reserve()), reserveCasus(), "the extra relation is not the bound one");
    }

    @Test void de_ontvangers_volgen_de_getwiste_binding() throws Exception {
        // reservedeelname links B1 to J2 alone: B1's 100 goes to J2, and J1 receives nothing (ref: the same cells)
        ObjectNode ir = reserve();
        verdeling(ir).set("connectingRole", binding("reservedeelname", "reserveberger"));
        BeideTakken.rood(tijdelijk("reserve-twist", ir), reserveCasus(), "verwacht 60, werkelijk leeg",
                "the recipients must be those of the bound reservedeelname relation");
    }

    @Test void een_binding_met_meervoudige_zuster_wordt_op_de_opslagrichting_geweigerd() throws Exception {
        // bergingstoezicht binds Berger as toezichthouder; the source side 'bewaakte berging' is to-many, so the
        // relation is stored on Berging and Berger carries no field to join on
        ObjectNode ir = lees(TOEZICHT);
        verdeling(ir).set("connectingRole", binding("bergingstoezicht", "toezichthouder"));
        BeideTakken.geweigerdApart(tijdelijk("toezicht-twist", ir), casus(TOEZICHT),
                "verdeling: verdeel de opbrengst van een berging — een verdeling over de enkelvoudige ontvangerrol"
                        + " 'toezichthouder' wiens zusterrol 'bewaakte berging' meervoudig is",
                "rol 'bewaakte berging' op Berger geen referentieveld", "the join follows the bound relation");
    }

    @Test void een_wederkerige_binding_joint_op_de_gebonden_rol_zelf() throws Exception {
        // maatschap is wederkerig: its one role 'maat' is its own sibling, so the join field is 'maat' itself.
        // A verdeling of vondstwaarde over the maat, added to jutterswet-maatschap: J1 and J2 are each other's
        // maat, so each receives the other's whole vondstwaarde; J3 has none. Verifier PASS, ref PASS.
        ObjectNode ir = maatschapMetVerdeling();
        IrLoader.Loaded l = IrLoader.loadAll(ir);
        var v = l.model().verdelingen.get(0);
        assertEquals(new FactModelGenerator.Verdelingsjoin("maat", null),
                FactModelGenerator.verdelingsjoin(l.model(), v, l.rolBindingen().get(v)),
                "a reciprocal role is its own sibling");
        ObjectNode c = (ObjectNode) JSON.readTree(Files.readString(MAATSCHAP.resolve("case1.json")));
        ArrayNode verwacht = (ArrayNode) c.get("expected");
        verwacht.insert(verwacht.size() - 1, maatdeel("J1", "12"));
        verwacht.insert(verwacht.size() - 1, maatdeel("J2", "30"));
        BeideTakken.groen(tijdelijk("maatschap", ir), c, "the reciprocal binding joins on the maat field");
    }

    // ---------------- a unique pair: the twist is read, not overruled ----------------

    @Test void een_binding_op_de_bronrol_wordt_niet_overschreven_door_het_typenpaar() throws Exception {
        ObjectNode ir = lees(BERGING);
        verdeling(ir).set("connectingRole", binding("bergingsdeelname", "berging"));
        BeideTakken.geweigerdApart(tijdelijk("bronrol", ir), casus(BERGING),
                "verdeling: verdeel de opbrengst van een berging — de join van de verdeling heeft geen beanveld — het"
                        + " beanmodel leidt voor de rol 'berging' op Berging geen referentieveld naar Berger af",
                "rol 'berger' op Berger geen referentieveld", "the bound role, not the pair, names the join");
    }

    @Test void een_binding_die_niet_oplost_wordt_geweigerd() throws Exception {
        ObjectNode ir = lees(BERGING);
        verdeling(ir).set("connectingRole", binding("bergingsdeelname", "bewaker"));
        BeideTakken.geweigerdApart(tijdelijk("los", ir), casus(BERGING), "verdeling: verdeel de opbrengst van een"
                        + " berging — de connectingRole van de verdeling noemt geen gedeclareerde rol (bergingsdeelname,"
                        + " bewaker)", "verbindende rol onopgelost",
                "a dangling binding is refused, not replaced by the pair");
    }

    @Test void zonder_binding_laadt_de_verdeling_niet() throws Exception {
        // IR-SPEC ch. 3.2: the recipient join IS the binding, and no reader derives a missing one from the type
        // pair, unique or not — the document is refused at load, by name
        ObjectNode ir = lees(BERGING);
        verdeling(ir).remove("connectingRole");
        IrLoader.Documentfout e = assertThrows(IrLoader.Documentfout.class, () -> IrLoader.loadAll(ir));
        assertEquals("/rules/allocations/0/versions/0", e.plek(), e.getMessage());
        assertTrue(e.getMessage().contains("missing field 'connectingRole'"), e.getMessage());
    }

    // ---------------- fixtures ----------------

    private static ObjectNode maatschapMetVerdeling() throws Exception {
        ObjectNode ir = lees(MAATSCHAP);
        ObjectNode jutter = IrPad.objectType(ir, "Jutter");
        IrPad.addAttribute(jutter, "maatdeel", IrPad.attribute(jutter, "maatwaarde").get("datatype").deepCopy());
        ObjectNode vorm = IrPad.rules(ir).putArray("allocations").addObject();
        vorm.put("node", "Allocation").put("name", "verdeel de vondstwaarde over de maat");
        ObjectNode v = vorm.putArray("versions").addObject();
        v.put("sourceType", "Jutter").put("sourceAttribute", "vondstwaarde")
                .put("targetType", "Jutter").put("targetAttribute", "maatdeel").put("method", "equalShares")
                .put("descending", false);
        v.putObject("validity").put("node", "Validity");
        v.set("connectingRole", binding("maatschap", "maat"));
        return ir;
    }

    private static ObjectNode maatdeel(String label, String waarde) {
        ObjectNode e = JSON.createObjectNode().put("kind", "attributeValue").put("label", label).put("attribute", "maatdeel");
        e.putObject("value").put("number", waarde);
        return e;
    }

    /** bergingstoezicht with a third Berging-Berger relation, whose to-one side sits on Berger. */
    private static ObjectNode reserve() throws Exception {
        ObjectNode ir = lees(TOEZICHT);
        ObjectNode ft = JSON.createObjectNode().put("node", "FactType").put("name", "reservedeelname").put("reciprocal", false);
        ArrayNode rollen = ft.putArray("roles");
        rollen.addObject().put("node", "Role").put("name", "reserveberging").put("objectType", "Berging")
                .put("many", false);
        rollen.addObject().put("node", "Role").put("name", "reserveberger").put("objectType", "Berger")
                .put("plural", "reservebergers").put("many", true);
        IrPad.modelLijst(ir, "factTypes").add(ft);
        return ir;
    }

    private static JsonNode reserveCasus() throws Exception {
        ObjectNode c = (ObjectNode) casus(TOEZICHT);
        for (JsonNode i : c.get("input").get("instances"))
            if (i.get("label").asText().equals("B1"))
                ((ObjectNode) i.get("roles")).putArray("reservebergers").add("J2");
        return c;
    }

    /** The one version of the document's one allocation. */
    private static ObjectNode verdeling(ObjectNode ir) {
        return IrPad.versie(ir, "allocations", 0, 0);
    }

    private static ObjectNode binding(String feittype, String rol) {
        return JSON.createObjectNode().put("node", "RoleBinding").put("factType", feittype).put("role", rol);
    }

    private static ObjectNode lees(Path golden) throws Exception {
        return (ObjectNode) JSON.readTree(Files.readString(golden.resolve("ir.json")));
    }

    private static JsonNode casus(Path golden) throws Exception {
        return JSON.readTree(Files.readString(golden.resolve("case.json")));
    }

    private static Path tijdelijk(String naam, ObjectNode ir) throws Exception {
        Path tmp = Files.createTempFile(naam, ".json");
        tmp.toFile().deleteOnExit();
        Files.writeString(tmp, ir.toString());
        return tmp;
    }
}
