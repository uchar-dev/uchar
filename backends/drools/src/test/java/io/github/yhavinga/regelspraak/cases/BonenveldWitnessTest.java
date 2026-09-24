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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The creation and scope bindings (IR-SPEC ch. 3.4, 3.6): both Drools branches execute a §13.4.7 fact creation
 * and a parent-scoped uniqueness on the fields DERIVED from the role bindings ({@code factType}/{@code anchorRole},
 * the hops of the {@code path}, the last of which is the join, and the uniqueness {@code scope}) — since L2 of the
 * v1 wire migration the only thing the wire carries of either construct.
 *
 * <p>Each position gets two rows. The first puts a storage field back beside the binding, the shape the wire had
 * before L2: there is nothing left to twist, so it is an unknown field, and the loader refuses the document by
 * name at its pointer. The second twists the BINDING and demands the answer the twisted binding gives, the one ref
 * gives on the same document — a wrong binding gives a loud wrong answer or a refusal, never the old answer. The
 * added fact types in the hop and scope fixtures relate the same object types a second time, so a twisted binding
 * still resolves and the reading shows in the values instead of in a refusal.
 */
class BonenveldWitnessTest {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Path CORPUS = Path.of("..", "..", "corpus", "golden");
    private static final Path DUBBEL = CORPUS.resolve("jutterswet-dubbelverbinding");
    private static final Path POSTWACHT = CORPUS.resolve("jutterswet-postwacht");
    private static final Path KUSTREGISTER = CORPUS.resolve("anchor").resolve("jutterswet-kustregister");

    // ---------------- zetVeld: the field the creation writes ----------------

    @Test void een_schrijfveld_naast_de_binding_wordt_bij_naam_geweigerd() throws Exception {
        ObjectNode ir = lees(DUBBEL);
        creatie(ir, 0, "winterberging").put("zetVeld", "keurhaven");
        onbekendVeld(ir, "/rules/factCreations/0/versions/0/zetVeld", "FactCreationVersion");
    }

    @Test void zetveld_volgt_de_gemaakte_rol() throws Exception {
        ObjectNode ir = lees(DUBBEL);
        creatie(ir, 0, "winterberging").put("factType", "keurberging").put("anchorRole", "keurhaven")
                .put("targetRole", "keurschuit");
        BeideTakken.rood(tijdelijk("zet-binding", ir), casus(DUBBEL, "case1"), "attribuut wintertal: verwacht 2, werkelijk 0",
                "the creation must write the bound keurberging relation");
    }

    // ---------------- joinVeld: the field the doel joins on ----------------

    @Test void een_koppelveld_naast_de_binding_wordt_bij_naam_geweigerd() throws Exception {
        ObjectNode ir = lees(DUBBEL);
        creatie(ir, 0, "winterberging").put("joinVeld", "vlaghaven");
        onbekendVeld(ir, "/rules/factCreations/0/versions/0/joinVeld", "FactCreationVersion");
    }

    @Test void joinveld_volgt_de_joinbinding() throws Exception {
        ObjectNode ir = lees(DUBBEL);
        ArrayNode pad = (ArrayNode) creatie(ir, 0, "winterberging").get("path");
        pad.set(pad.size() - 1, binding("vlagvoering", "vlaggenschip"));
        BeideTakken.rood(tijdelijk("join-binding", ir), casus(DUBBEL, "case1"), "attribuut wintertal: verwacht 2, werkelijk 1",
                "the join must read the bound vlagvoering relation");
    }

    // ---------------- the hops ----------------

    @Test void een_stap_draagt_alleen_haar_binding() throws Exception {
        // the step's field, its side and its type are the backend's own derivation from the binding, so each one
        // written beside it is an unknown field
        for (String veld : new String[] { "veld", "viaOuderVeld", "type" }) {
            ObjectNode ir = lees(POSTWACHT);
            stap(ir).put(veld, veld.equals("viaOuderVeld") ? "true" : "Sein");
            onbekendVeld(ir, "/rules/factCreations/0/versions/0/path/0/" + veld, "RolBinding");
        }
    }

    @Test void tussenstap_volgt_de_hopbinding() throws Exception {
        ObjectNode ir = postwachtMetReserve();
        ((ArrayNode) creatie(ir, 0, null).get("path")).set(0, binding("reserveligging", "reservepost"));
        BeideTakken.rood(tijdelijk("hop-binding", ir), casus(POSTWACHT, "case"), "attribuut vakseinaantal: verwacht 2, werkelijk 0",
                "the hop must walk the bound reserveligging relation");
    }

    @Test void een_een_op_een_hop_leest_de_ouderzijde() throws Exception {
        // postwacht with kustligging made 1:1. Both beans now carry a field (Kustvak.post and Post.kustvak) and the
        // case input writes both (ch. 5.7a), so the two sides answer alike here and the document plays either way;
        // what this row pins is the CHOICE, in the one derivation both branches execute. The parent side is the
        // side the frontend picks for an objectcreatie-placed link; the only other witness sits in the temporary
        // parity zone. Verifier-clean, ref PASS.
        ObjectNode ir = lees(POSTWACHT);
        for (JsonNode r : IrPad.declaratie(ir, "factTypes", "kustligging").get("rollen"))
            if (r.get("naam").asText().equals("post")) ((ObjectNode) r).put("meervoudig", false).putNull("meervoud");
        ObjectNode c = (ObjectNode) casus(POSTWACHT, "case");
        for (JsonNode i : c.get("input").get("instances")) {
            ObjectNode rollen = (ObjectNode) i.get("roles");
            if (rollen.has("posten")) rollen.set("post", rollen.remove("posten"));
        }
        RegelSpraakModel m = IrLoader.loadAll(ir).model();
        FactModelGenerator.Creatievelden v = FactModelGenerator.creatievelden(m, m.feitCreaties.get(0));
        assertEquals(new FactModelGenerator.Creatiehop(m.objecttypeOpNaam("Post"), "post", true), v.hops().get(0),
                "a 1:1 hop reads the parent's field, named after the bound role");
        BeideTakken.groen(tijdelijk("hop-een-op-een", ir), c, "the 1:1 hop plays over the parent side");
    }

    // ---------------- scopeVeld: the uniqueness scope ----------------

    @Test void een_scopeveld_naast_de_binding_wordt_bij_naam_geweigerd() throws Exception {
        ObjectNode ir = lees(KUSTREGISTER);
        kaveletiket(ir).put("scopeVeld", "sectiecode");
        onbekendVeld(ir, "/rules/uniquenesses/" + kaveletiketIndex(ir) + "/versions/0/scopeVeld", "UniquenessVersion");
    }

    @Test void scopeveld_volgt_de_scopebinding() throws Exception {
        // wijkindeling relates Kavel and Gemeente a second time; no case fills it, so no Kavel has that scope
        // and none participates
        ObjectNode ir = lees(KUSTREGISTER);
        IrPad.modelLijst(ir, "factTypes").add(feittype("wijkindeling",
                "wijkgemeente", "Gemeente", false, "wijkkavel", "Kavel", true));
        kaveletiket(ir).set("scope", binding("wijkindeling", "wijkgemeente"));
        BeideTakken.rood(tijdelijk("scope-binding", ir), casus(KUSTREGISTER, "case3"), "werkelijk consistent",
                "the scope must be the bound wijkindeling relation");
    }

    // ---------------- the bindings are required, and there is no fallback ----------------

    @Test void de_bindingen_alleen_spelen_het_document() throws Exception {
        // the golden as the wire now carries it: every construct on its bindings, no storage field anywhere
        ObjectNode ir = lees(POSTWACHT);
        ObjectNode fc = creatie(ir, 0, null);
        for (String veld : new String[] { "zetVeld", "joinVeld" })
            assertTrue(!fc.has(veld), "no storage field on the creation: " + fc);
        for (JsonNode t : fc.get("path")) {
            java.util.List<String> velden = new java.util.ArrayList<>();
            t.fieldNames().forEachRemaining(velden::add);
            assertEquals(java.util.List.of("node", "feittype", "rol"), velden, t.toString());
        }
        BeideTakken.groen(tijdelijk("bindingen", ir), casus(POSTWACHT, "case"), "plays on the bindings alone");
    }

    @Test void een_creatie_zonder_heel_symbolisch_blok_laadt_niet() throws Exception {
        // IR-SPEC ch. 3.4: the symbolic block IS the creation, so a creation missing any part of it — here each of
        // the four, and a step's binding — is no document; ref and clingo fault on it, and before L2 both branches
        // refused it as unresolved
        for (String veld : new String[] { "factType", "anchorRole", "targetRole", "path" }) {
            ObjectNode ir = lees(POSTWACHT);
            creatie(ir, 0, null).remove(veld);
            IrLoader.Documentfout e = assertThrows(IrLoader.Documentfout.class, () -> IrLoader.loadAll(ir));
            assertEquals("/rules/factCreations/0/versions/0", e.plek(), e.getMessage());
            assertTrue(e.getMessage().contains("missing field '" + veld + "'"), e.getMessage());
        }
        ObjectNode ir = lees(POSTWACHT);
        stap(ir).remove("feittype");
        IrLoader.Documentfout e = assertThrows(IrLoader.Documentfout.class, () -> IrLoader.loadAll(ir));
        assertEquals("/rules/factCreations/0/versions/0/path/0", e.plek(), e.getMessage());
        assertTrue(e.getMessage().contains("missing field 'feittype'"), e.getMessage());
    }

    @Test void een_scopebinding_die_niet_oplost_valt_niet_terug_op_scopeveld() throws Exception {
        ObjectNode ir = lees(KUSTREGISTER);
        kaveletiket(ir).set("scope", binding("wijkindeling", "gemeente"));
        BeideTakken.geweigerd(tijdelijk("scope-los", ir), casus(KUSTREGISTER, "case3"), "scopebinding onopgelost",
                "a binding naming no declared relation is refused, not read by the written name");
    }

    // ---------------- fixtures ----------------

    /** postwacht with a second Kustvak-Post relation, so Post also carries a {@code reservevak} field. */
    private static ObjectNode postwachtMetReserve() throws Exception {
        ObjectNode ir = lees(POSTWACHT);
        IrPad.modelLijst(ir, "factTypes").add(feittype("reserveligging",
                "reservevak", "Kustvak", false, "reservepost", "Post", true));
        return ir;
    }

    /** The first hop of the creation's path, a step before the final hop. */
    private static ObjectNode stap(ObjectNode ir) {
        ObjectNode t = (ObjectNode) creatie(ir, 0, null).get("path").get(0);
        assertTrue(t.get("feittype").asText().equals("kustligging"), t.toString());
        return t;
    }

    /** The loader refuses a field the L2 shape does not have, at its own pointer, as an unknown field. */
    private static void onbekendVeld(ObjectNode ir, String plek, String knoop) {
        IrLoader.Documentfout e = assertThrows(IrLoader.Documentfout.class, () -> IrLoader.loadAll(ir));
        assertEquals(plek, e.plek(), e.getMessage());
        assertTrue(e.getMessage().contains("unknown field on " + knoop), e.getMessage());
    }

    private static int kaveletiketIndex(ObjectNode ir) {
        ArrayNode u = (ArrayNode) ir.get("rules").get("uniquenesses");
        for (int i = 0; i < u.size(); i++) if (u.get(i).get("name").asText().equals("eenmalig kaveletiket")) return i;
        throw new AssertionError("kustregister carries no 'eenmalig kaveletiket'");
    }

    /** The one version of the {@code i}-th fact creation. */
    private static ObjectNode creatie(ObjectNode ir, int i, String feittype) {
        ObjectNode fc = IrPad.versie(ir, "factCreations", i, 0);
        if (feittype != null) assertTrue(fc.get("factType").asText().equals(feittype), fc.toString());
        return fc;
    }

    /** The one version of the uniqueness 'eenmalig kaveletiket'. */
    private static ObjectNode kaveletiket(ObjectNode ir) {
        return IrPad.versie(ir, "uniquenesses", "eenmalig kaveletiket");
    }

    private static ObjectNode feittype(String naam, String rol1, String ot1, boolean veel1,
                                       String rol2, String ot2, boolean veel2) {
        ObjectNode ft = JSON.createObjectNode().put("node", "Feittype").put("naam", naam).put("wederkerig", false);
        ArrayNode rollen = ft.putArray("rollen");
        rollen.addObject().put("node", "Rol").put("naam", rol1).put("objecttypeNaam", ot1)
                .put("meervoud", veel1 ? rol1 + "en" : null).put("meervoudig", veel1);
        rollen.addObject().put("node", "Rol").put("naam", rol2).put("objecttypeNaam", ot2)
                .put("meervoud", veel2 ? rol2 + "en" : null).put("meervoudig", veel2);
        return ft;
    }

    private static ObjectNode binding(String feittype, String rol) {
        return JSON.createObjectNode().put("node", "RolBinding").put("feittype", feittype).put("rol", rol);
    }

    private static ObjectNode lees(Path golden) throws Exception {
        return (ObjectNode) JSON.readTree(Files.readString(golden.resolve("ir.json")));
    }

    private static JsonNode casus(Path golden, String prefix) throws Exception {
        try (var s = Files.list(golden)) {
            Path p = s.filter(f -> {
                String n = f.getFileName().toString();
                return n.equals(prefix + ".json") || n.startsWith(prefix + "-");
            }).findFirst().orElseThrow();
            return JSON.readTree(Files.readString(p));
        }
    }

    private static Path tijdelijk(String naam, ObjectNode ir) throws Exception {
        Path tmp = Files.createTempFile(naam, ".json");
        tmp.toFile().deleteOnExit();
        Files.writeString(tmp, ir.toString());
        return tmp;
    }
}
