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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * The verdeling-role witness (V1-WIREMIGRATIE P3): both Drools branches join a §9.7 verdeling's recipients on
 * the relation its wire {@code verbindendeRol} binds (IR-SPEC ch. 3.2), through
 * {@link FactModelGenerator#verdelingsjoin}, and ask the type pair only for a bindingless verdeling (WF-048).
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
        verdeling(ir).set("verbindendeRol", binding("reservedeelname", "reserveberger"));
        BeideTakken.rood(tijdelijk("reserve-twist", ir), reserveCasus(), "verwacht 60, werkelijk leeg",
                "the recipients must be those of the bound reservedeelname relation");
    }

    @Test void een_binding_met_meervoudige_zuster_wordt_op_de_opslagrichting_geweigerd() throws Exception {
        // bergingstoezicht binds Berger as toezichthouder; the source side 'bewaakte berging' is to-many, so the
        // relation is stored on Berging and Berger carries no field to join on
        ObjectNode ir = lees(TOEZICHT);
        verdeling(ir).set("verbindendeRol", binding("bergingstoezicht", "toezichthouder"));
        BeideTakken.geweigerd(tijdelijk("toezicht-twist", ir), casus(TOEZICHT),
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
        verdeling(ir).set("verbindendeRol", binding("bergingsdeelname", "berging"));
        BeideTakken.geweigerd(tijdelijk("bronrol", ir), casus(BERGING),
                "rol 'berger' op Berger geen referentieveld", "the bound role, not the pair, names the join");
    }

    @Test void een_binding_die_niet_oplost_wordt_geweigerd() throws Exception {
        ObjectNode ir = lees(BERGING);
        verdeling(ir).set("verbindendeRol", binding("bergingsdeelname", "bewaker"));
        BeideTakken.geweigerd(tijdelijk("los", ir), casus(BERGING), "verbindende rol onopgelost",
                "a dangling binding is refused, not replaced by the pair");
    }

    @Test void zonder_binding_beslist_het_typenpaar() throws Exception {
        // WF-048 admits the bindingless form while the pair is unique; B12 retires it
        ObjectNode ir = lees(BERGING);
        verdeling(ir).remove("verbindendeRol");
        BeideTakken.groen(tijdelijk("bindingloos", ir), casus(BERGING), "the unique pair still decides");
    }

    // ---------------- fixtures ----------------

    private static ObjectNode maatschapMetVerdeling() throws Exception {
        ObjectNode ir = lees(MAATSCHAP);
        ObjectNode attrs = (ObjectNode) ir.get("objecttypen").get("Jutter").get("attributen");
        attrs.set("maatdeel", attrs.get("maatwaarde").deepCopy());
        ObjectNode v = ir.putArray("verdelingen").addObject();
        v.put("node", "ResolvedVerdeling").put("naam", "verdeel de vondstwaarde over de maat")
                .put("bronType", "Jutter").put("bronAttribuut", "vondstwaarde")
                .put("doelType", "Jutter").put("doelAttribuut", "maatdeel").put("methode", "GELIJKE_DELEN")
                .putNull("ratoAttribuut").putNull("maximumAttribuut").putNull("afrondingDecimalen")
                .putNull("restantAttribuut").putNull("groepeerAttribuut").put("afnemend", false).putNull("voorwaarde");
        v.putObject("geldigheid").put("node", "Geldigheid").putNull("vanaf").putNull("tot");
        v.set("verbindendeRol", binding("maatschap", "maat"));
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
        ObjectNode ft = JSON.createObjectNode().put("node", "Feittype").put("naam", "reservedeelname").put("wederkerig", false);
        ArrayNode rollen = ft.putArray("rollen");
        rollen.addObject().put("node", "Rol").put("naam", "reserveberging").put("objecttypeNaam", "Berging")
                .putNull("meervoud").put("meervoudig", false);
        rollen.addObject().put("node", "Rol").put("naam", "reserveberger").put("objecttypeNaam", "Berger")
                .put("meervoud", "reservebergers").put("meervoudig", true);
        ((ObjectNode) ir.get("feittypen")).set("reservedeelname", ft);
        return ir;
    }

    private static JsonNode reserveCasus() throws Exception {
        ObjectNode c = (ObjectNode) casus(TOEZICHT);
        for (JsonNode i : c.get("input").get("instances"))
            if (i.get("label").asText().equals("B1"))
                ((ObjectNode) i.get("roles")).putArray("reservebergers").add("J2");
        return c;
    }

    private static ObjectNode verdeling(ObjectNode ir) {
        return (ObjectNode) ir.get("verdelingen").get(0);
    }

    private static ObjectNode binding(String feittype, String rol) {
        return JSON.createObjectNode().put("node", "RolBinding").put("feittype", feittype).put("rol", rol);
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
