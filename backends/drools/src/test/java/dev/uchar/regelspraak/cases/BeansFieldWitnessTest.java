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
class BeansFieldWitnessTest {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Path CORPUS = Path.of("..", "..", "corpus", "golden");
    private static final Path DUPLICATE = CORPUS.resolve("jutterswet-dubbelverbinding");
    private static final Path POSTWACHT = CORPUS.resolve("jutterswet-postwacht");
    private static final Path KUSTREGISTER = CORPUS.resolve("anchor").resolve("jutterswet-kustregister");

    // ---------------- zetVeld: the field the creation writes ----------------

    @Test void a_write_field_beside_the_binding_is_refused_by_name() throws Exception {
        ObjectNode ir = lees(DUPLICATE);
        creatie(ir, 0, "winterberging").put("zetVeld", "keurhaven");
        unknownField(ir, "/rules/factCreations/0/versions/0/zetVeld", "FactCreationVersion");
    }

    @Test void set_field_follows_the_made_role() throws Exception {
        ObjectNode ir = lees(DUPLICATE);
        creatie(ir, 0, "winterberging").put("factType", "keurberging").put("anchorRole", "keurhaven")
                .put("targetRole", "keurschuit");
        BothBranches.red(temporary("zet-binding", ir), goldenCase(DUPLICATE, "case1"), "attribuut wintertal: verwacht 2, werkelijk 0",
                "the creation must write the bound keurberging relation");
    }

    // ---------------- joinVeld: the field the doel joins on ----------------

    @Test void a_link_field_beside_the_binding_is_refused_by_name() throws Exception {
        ObjectNode ir = lees(DUPLICATE);
        creatie(ir, 0, "winterberging").put("joinVeld", "vlaghaven");
        unknownField(ir, "/rules/factCreations/0/versions/0/joinVeld", "FactCreationVersion");
    }

    @Test void join_field_follows_the_join_binding() throws Exception {
        ObjectNode ir = lees(DUPLICATE);
        ArrayNode pad = (ArrayNode) creatie(ir, 0, "winterberging").get("path");
        pad.set(pad.size() - 1, binding("vlagvoering", "vlaggenschip"));
        BothBranches.red(temporary("join-binding", ir), goldenCase(DUPLICATE, "case1"), "attribuut wintertal: verwacht 2, werkelijk 1",
                "the join must read the bound vlagvoering relation");
    }

    // ---------------- the hops ----------------

    @Test void a_step_carries_only_its_binding() throws Exception {
        // the step's field, its side and its type are the backend's own derivation from the binding, so each one
        // written beside it is an unknown field
        for (String veld : new String[] { "veld", "viaOuderVeld", "type" }) {
            ObjectNode ir = lees(POSTWACHT);
            step(ir).put(veld, veld.equals("viaOuderVeld") ? "true" : "Sein");
            unknownField(ir, "/rules/factCreations/0/versions/0/path/0/" + veld, "RoleBinding");
        }
    }

    @Test void path_step_follows_the_hop_binding() throws Exception {
        ObjectNode ir = postwachtWithReserve();
        ((ArrayNode) creatie(ir, 0, null).get("path")).set(0, binding("reserveligging", "reservepost"));
        BothBranches.red(temporary("hop-binding", ir), goldenCase(POSTWACHT, "case"), "attribuut vakseinaantal: verwacht 2, werkelijk 0",
                "the hop must walk the bound reserveligging relation");
    }

    @Test void a_one_to_one_hop_reads_the_parent_side() throws Exception {
        // postwacht with kustligging made 1:1. Both beans now carry a field (Kustvak.post and Post.kustvak) and the
        // case input writes both (ch. 5.7a), so the two sides answer alike here and the document plays either way;
        // what this row pins is the CHOICE, in the one derivation both branches execute. The parent side is the
        // side the frontend picks for an objectcreatie-placed link; the only other witness sits in the
        // parity zone. Verifier-clean, ref PASS.
        ObjectNode ir = lees(POSTWACHT);
        for (JsonNode r : IrPath.declaratie(ir, "factTypes", "kustligging").get("roles"))
            if (r.get("name").asText().equals("post")) ((ObjectNode) r).put("many", false);
        ObjectNode c = (ObjectNode) goldenCase(POSTWACHT, "case");
        for (JsonNode i : c.get("input").get("instances")) {
            ObjectNode roles = (ObjectNode) i.get("roles");
            if (roles.has("posten")) roles.set("post", roles.remove("posten"));
        }
        RegelSpraakModel m = IrLoader.loadAll(ir).model();
        FactModelGenerator.CreationFields v = FactModelGenerator.creationFields(m, m.feitCreaties.get(0));
        assertEquals(new FactModelGenerator.CreationHop(m.objecttypeOpNaam("Post"), "post", true), v.hops().get(0),
                "a 1:1 hop reads the parent's field, named after the bound role");
        BothBranches.green(temporary("hop-een-op-een", ir), c, "the 1:1 hop plays over the parent side");
    }

    // ---------------- scopeField: the uniqueness scope ----------------

    @Test void a_scope_field_beside_the_binding_is_refused_by_name() throws Exception {
        ObjectNode ir = lees(KUSTREGISTER);
        kaveletiket(ir).put("scopeVeld", "sectiecode");
        unknownField(ir, "/rules/uniquenesses/" + kaveletiketIndex(ir) + "/versions/0/scopeVeld", "UniquenessVersion");
    }

    @Test void scope_field_follows_the_scope_binding() throws Exception {
        // wijkindeling relates Kavel and Gemeente a second time; no case fills it, so no Kavel has that scope
        // and none participates
        ObjectNode ir = lees(KUSTREGISTER);
        IrPath.modelList(ir, "factTypes").add(feittype("wijkindeling",
                "wijkgemeente", "Gemeente", false, "wijkkavel", "Kavel", true));
        kaveletiket(ir).set("scope", binding("wijkindeling", "wijkgemeente"));
        BothBranches.red(temporary("scope-binding", ir), goldenCase(KUSTREGISTER, "case3"), "werkelijk consistent",
                "the scope must be the bound wijkindeling relation");
    }

    // ---------------- the bindings are required, and there is no fallback ----------------

    @Test void the_bindings_only_play_the_document() throws Exception {
        // the golden as the wire now carries it: every construct on its bindings, no storage field anywhere
        ObjectNode ir = lees(POSTWACHT);
        ObjectNode fc = creatie(ir, 0, null);
        for (String veld : new String[] { "zetVeld", "joinVeld" })
            assertTrue(!fc.has(veld), "no storage field on the creation: " + fc);
        for (JsonNode t : fc.get("path")) {
            java.util.List<String> fields = new java.util.ArrayList<>();
            t.fieldNames().forEachRemaining(fields::add);
            assertEquals(java.util.List.of("node", "factType", "role"), fields, t.toString());
        }
        BothBranches.green(temporary("bindingen", ir), goldenCase(POSTWACHT, "case"), "plays on the bindings alone");
    }

    @Test void a_creation_without_whole_symbolic_block_does_not_load() throws Exception {
        // IR-SPEC ch. 3.4: the symbolic block IS the creation, so a creation missing any part of it — here each of
        // the four, and a step's binding — is no document; ref and clingo fault on it, and before L2 both branches
        // refused it as unresolved
        for (String veld : new String[] { "factType", "anchorRole", "targetRole", "path" }) {
            ObjectNode ir = lees(POSTWACHT);
            creatie(ir, 0, null).remove(veld);
            IrLoader.Documentfout e = assertThrows(IrLoader.Documentfout.class, () -> IrLoader.loadAll(ir));
            assertEquals("/rules/factCreations/0/versions/0", e.site(), e.getMessage());
            assertTrue(e.getMessage().contains("missing field '" + veld + "'"), e.getMessage());
        }
        ObjectNode ir = lees(POSTWACHT);
        step(ir).remove("factType");
        IrLoader.Documentfout e = assertThrows(IrLoader.Documentfout.class, () -> IrLoader.loadAll(ir));
        assertEquals("/rules/factCreations/0/versions/0/path/0", e.site(), e.getMessage());
        assertTrue(e.getMessage().contains("missing field 'factType'"), e.getMessage());
    }

    @Test void a_scope_binding_that_does_not_resolve_does_not_fall_back_on_the_scope_field() throws Exception {
        ObjectNode ir = lees(KUSTREGISTER);
        kaveletiket(ir).set("scope", binding("wijkindeling", "gemeente"));
        BothBranches.refused(temporary("scope-los", ir), goldenCase(KUSTREGISTER, "case3"),
                "uniciteit: eenmalig kaveletiket — de scope van een Uniqueness noemt geen gedeclareerde rol (wijkindeling,"
                        + " gemeente)",
                "a binding naming no declared relation is refused, not read by the written name");
    }

    // ---------------- fixtures ----------------

    /** postwacht with a second Kustvak-Post relation, so Post also carries a {@code reservevak} field. */
    private static ObjectNode postwachtWithReserve() throws Exception {
        ObjectNode ir = lees(POSTWACHT);
        IrPath.modelList(ir, "factTypes").add(feittype("reserveligging",
                "reservevak", "Kustvak", false, "reservepost", "Post", true));
        return ir;
    }

    /** The first hop of the creation's path, a step before the final hop. */
    private static ObjectNode step(ObjectNode ir) {
        ObjectNode t = (ObjectNode) creatie(ir, 0, null).get("path").get(0);
        assertTrue(t.get("factType").asText().equals("kustligging"), t.toString());
        return t;
    }

    /** The loader refuses a field the L2 shape does not have, at its own pointer, as an unknown field. */
    private static void unknownField(ObjectNode ir, String site, String jsonNode) {
        IrLoader.Documentfout e = assertThrows(IrLoader.Documentfout.class, () -> IrLoader.loadAll(ir));
        assertEquals(site, e.site(), e.getMessage());
        assertTrue(e.getMessage().contains("unknown field on " + jsonNode), e.getMessage());
    }

    private static int kaveletiketIndex(ObjectNode ir) {
        ArrayNode u = (ArrayNode) ir.get("rules").get("uniquenesses");
        for (int i = 0; i < u.size(); i++) if (u.get(i).get("name").asText().equals("eenmalig kaveletiket")) return i;
        throw new AssertionError("kustregister carries no 'eenmalig kaveletiket'");
    }

    /** The one version of the {@code i}-th fact creation. */
    private static ObjectNode creatie(ObjectNode ir, int i, String feittype) {
        ObjectNode fc = IrPath.version(ir, "factCreations", i, 0);
        if (feittype != null) assertTrue(fc.get("factType").asText().equals(feittype), fc.toString());
        return fc;
    }

    /** The one version of the uniqueness 'eenmalig kaveletiket'. */
    private static ObjectNode kaveletiket(ObjectNode ir) {
        return IrPath.version(ir, "uniquenesses", "eenmalig kaveletiket");
    }

    private static ObjectNode feittype(String naam, String role1, String ot1, boolean many1,
                                       String role2, String ot2, boolean many2) {
        ObjectNode ft = JSON.createObjectNode().put("node", "FactType").put("name", naam).put("reciprocal", false);
        ArrayNode roles = ft.putArray("roles");
        // a role without a plural leaves the field out: the wire writes an optional field whole or not at all
        ObjectNode r1 = roles.addObject().put("node", "Role").put("name", role1).put("objectType", ot1);
        if (many1) r1.put("plural", role1 + "en");
        r1.put("many", many1);
        ObjectNode r2 = roles.addObject().put("node", "Role").put("name", role2).put("objectType", ot2);
        if (many2) r2.put("plural", role2 + "en");
        r2.put("many", many2);
        return ft;
    }

    private static ObjectNode binding(String feittype, String rol) {
        return JSON.createObjectNode().put("node", "RoleBinding").put("factType", feittype).put("role", rol);
    }

    private static ObjectNode lees(Path golden) throws Exception {
        return (ObjectNode) JSON.readTree(Files.readString(golden.resolve("ir.json")));
    }

    private static JsonNode goldenCase(Path golden, String prefix) throws Exception {
        try (var s = Files.list(golden)) {
            Path p = s.filter(f -> {
                String n = f.getFileName().toString();
                return n.equals(prefix + ".json") || n.startsWith(prefix + "-");
            }).findFirst().orElseThrow();
            return JSON.readTree(Files.readString(p));
        }
    }

    private static Path temporary(String naam, ObjectNode ir) throws Exception {
        Path tmp = Files.createTempFile(naam, ".json");
        tmp.toFile().deleteOnExit();
        Files.writeString(tmp, ir.toString());
        return tmp;
    }
}
