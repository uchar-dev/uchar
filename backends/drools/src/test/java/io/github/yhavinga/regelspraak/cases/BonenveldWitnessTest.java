package io.github.yhavinga.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The bean-field witness (V1-WIREMIGRATIE P2): both Drools branches execute a §13.4.7 fact creation and a
 * parent-scoped uniqueness on the fields DERIVED from the role bindings ({@code feittype}/{@code ankerRol},
 * {@code joinRol}, the hops' {@code rolBinding}, {@code scopeRol}), never on the wire's bean fields
 * ({@code zetVeld}, {@code joinVeld}, {@code tussenstappen[].veld/viaOuderVeld/type}, {@code scopeVeld}), which
 * migration steps B7 and B8 remove.
 *
 * <p>Bean field and binding agree on every golden a branch plays, so the corpus cannot tell which one decides.
 * Each position gets two rows here. The first twists the BEAN field to another field the bean really carries and
 * demands the pinned answer: the binding still decides. The second twists the BINDING and demands the answer the
 * twisted binding gives, the one ref gives on the same document. RED FIRST, measured 2026-09-22 on the source of
 * main 58ae7a4c (before the port), in both branches: every bean-twist row followed the twisted field (a wrong
 * value, or for the hop's side and type a refusal on a field the bean does not carry), every binding-twist row
 * played the untwisted answer, the bean-field-free document was refused as unresolved, and the bean-only
 * creation and the unresolvable scope binding played.
 *
 * <p>The bean-twisted documents fail the verifier (WF-034 (m), WF-054: the bean field must name the bound
 * relation's role), except the hop-side row: no rule crosses {@code viaOuderVeld}, so that twist is
 * verifier-clean, ref plays it, and before the port both branches refused it. The added fact types in the hop and scope fixtures relate
 * the same object types a second time, so a twisted field or binding still resolves and the reading shows in
 * the values instead of in a refusal.
 */
class BonenveldWitnessTest {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Path CORPUS = Path.of("..", "..", "corpus", "golden");
    private static final Path DUBBEL = CORPUS.resolve("jutterswet-dubbelverbinding");
    private static final Path POSTWACHT = CORPUS.resolve("jutterswet-postwacht");
    private static final Path KUSTREGISTER = CORPUS.resolve("anchor").resolve("jutterswet-kustregister");

    // ---------------- zetVeld: the field the creation writes ----------------

    @Test void zetveld_twist_op_de_boon_verandert_niets() throws Exception {
        // winterberging writing keurhaven would empty wintertal (H1 2 -> 0)
        ObjectNode ir = lees(DUBBEL);
        creatie(ir, 0, "winterberging").put("zetVeld", "keurhaven");
        BeideTakken.groen(tijdelijk("zet-boon", ir), casus(DUBBEL, "case1"), "the write follows ankerRol, not zetVeld");
    }

    @Test void zetveld_volgt_de_gemaakte_rol() throws Exception {
        ObjectNode ir = lees(DUBBEL);
        creatie(ir, 0, "winterberging").put("feittype", "keurberging").put("ankerRol", "keurhaven")
                .put("doelRol", "keurschuit");
        BeideTakken.rood(tijdelijk("zet-binding", ir), casus(DUBBEL, "case1"), "attribuut wintertal: verwacht 2, werkelijk 0",
                "the creation must write the bound keurberging relation");
    }

    // ---------------- joinVeld: the field the doel joins on ----------------

    @Test void joinveld_twist_op_de_boon_verandert_niets() throws Exception {
        // joining on vlaghaven would admit S1 alone (H1 wintertal 2 -> 1)
        ObjectNode ir = lees(DUBBEL);
        creatie(ir, 0, "winterberging").put("joinVeld", "vlaghaven");
        BeideTakken.groen(tijdelijk("join-boon", ir), casus(DUBBEL, "case1"), "the join follows joinRol, not joinVeld");
    }

    @Test void joinveld_volgt_de_joinbinding() throws Exception {
        ObjectNode ir = lees(DUBBEL);
        creatie(ir, 0, "winterberging").set("joinRol", binding("vlagvoering", "vlaggenschip"));
        BeideTakken.rood(tijdelijk("join-binding", ir), casus(DUBBEL, "case1"), "attribuut wintertal: verwacht 2, werkelijk 1",
                "the join must read the bound vlagvoering relation");
    }

    // ---------------- the hops ----------------

    @Test void tussenstap_twist_op_de_boon_verandert_niets() throws Exception {
        // Post carries reservevak too, so the twisted hop resolves and would reach no post (vakseinaantal 2 -> 0)
        ObjectNode ir = postwachtMetReserve();
        stap(ir).put("veld", "reservevak");
        BeideTakken.groen(tijdelijk("hop-boon", ir), casus(POSTWACHT, "case"), "the hop follows its rolBinding, not veld");
    }

    @Test void tussenstap_zijde_komt_uit_de_binding() throws Exception {
        // Verifier-clean: no WF rule crosses viaOuderVeld, and ref plays the document. Read from the wire it put
        // the field on Kustvak, and both branches refused what ref plays.
        ObjectNode ir = lees(POSTWACHT);
        stap(ir).put("viaOuderVeld", true);
        BeideTakken.groen(tijdelijk("hop-zijde", ir), casus(POSTWACHT, "case"), "the side follows the binding");
    }

    @Test void tussenstap_type_komt_uit_de_binding() throws Exception {
        ObjectNode ir = lees(POSTWACHT);
        stap(ir).put("type", "Sein");
        BeideTakken.groen(tijdelijk("hop-type", ir), casus(POSTWACHT, "case"), "the step type is the bound role's filler");
    }

    @Test void tussenstap_volgt_de_hopbinding() throws Exception {
        ObjectNode ir = postwachtMetReserve();
        stap(ir).set("rolBinding", binding("reserveligging", "reservepost"));
        BeideTakken.rood(tijdelijk("hop-binding", ir), casus(POSTWACHT, "case"), "attribuut vakseinaantal: verwacht 2, werkelijk 0",
                "the hop must walk the bound reserveligging relation");
    }

    // ---------------- scopeVeld: the uniqueness scope ----------------

    @Test void scopeveld_twist_op_de_boon_verandert_niets() throws Exception {
        // scoped by sectiecode, K1..K3 (all sectiecode 10, etiket 1) would collide
        ObjectNode ir = lees(KUSTREGISTER);
        kaveletiket(ir).put("scopeVeld", "sectiecode");
        BeideTakken.groen(tijdelijk("scope-boon", ir), casus(KUSTREGISTER, "case1"), "the scope follows scopeRol, not scopeVeld");
    }

    @Test void scopeveld_volgt_de_scopebinding() throws Exception {
        // wijkindeling relates Kavel and Gemeente a second time; no case fills it, so no Kavel has that scope
        // and none participates
        ObjectNode ir = lees(KUSTREGISTER);
        ((ObjectNode) ir.get("feittypen")).set("wijkindeling", feittype("wijkindeling",
                "wijkgemeente", "Gemeente", false, "wijkkavel", "Kavel", true));
        kaveletiket(ir).set("scopeRol", binding("wijkindeling", "wijkgemeente"));
        BeideTakken.rood(tijdelijk("scope-binding", ir), casus(KUSTREGISTER, "case3"), "werkelijk consistent",
                "the scope must be the bound wijkindeling relation");
    }

    // ---------------- B7/B8 ahead of time, and no fallback ----------------

    @Test void zonder_bonenvelden_speelt_het_document_gelijk() throws Exception {
        // the wire as B7 and B8 leave it: every bean field of both constructs gone
        ObjectNode ir = lees(POSTWACHT);
        ObjectNode fc = creatie(ir, 0, null);
        fc.remove("zetVeld");
        fc.remove("joinVeld");
        for (JsonNode t : fc.get("tussenstappen")) ((ObjectNode) t).remove(java.util.List.of("veld", "viaOuderVeld", "type"));
        for (JsonNode u : ir.get("uniciteiten")) if (u.hasNonNull("scopeRol")) ((ObjectNode) u).remove("scopeVeld");
        BeideTakken.groen(tijdelijk("zonder-bonen", ir), casus(POSTWACHT, "case"), "plays on the bindings alone");
    }

    @Test void een_creatie_zonder_symbolische_vorm_wordt_geweigerd() throws Exception {
        // WF-028 rejects the bean-only record and ref and clingo fault on it; the branches played it off the bean
        // fields until the port
        ObjectNode ir = lees(POSTWACHT);
        ObjectNode fc = creatie(ir, 0, null);
        fc.remove(java.util.List.of("feittype", "ankerRol", "doelRol", "joinRol"));
        for (JsonNode t : fc.get("tussenstappen")) ((ObjectNode) t).remove("rolBinding");
        BeideTakken.geweigerd(tijdelijk("alleen-bonen", ir), casus(POSTWACHT, "case"), "feitcreatie (onopgelost)",
                "the creation has no symbolic form to execute");
    }

    @Test void een_scopebinding_die_niet_oplost_valt_niet_terug_op_scopeveld() throws Exception {
        ObjectNode ir = lees(KUSTREGISTER);
        kaveletiket(ir).set("scopeRol", binding("wijkindeling", "gemeente"));
        BeideTakken.geweigerd(tijdelijk("scope-los", ir), casus(KUSTREGISTER, "case3"), "scopebinding onopgelost",
                "a binding naming no declared relation is refused, not read by the written name");
    }

    // ---------------- fixtures ----------------

    /** postwacht with a second Kustvak-Post relation, so Post also carries a {@code reservevak} field. */
    private static ObjectNode postwachtMetReserve() throws Exception {
        ObjectNode ir = lees(POSTWACHT);
        ((ObjectNode) ir.get("feittypen")).set("reserveligging", feittype("reserveligging",
                "reservevak", "Kustvak", false, "reservepost", "Post", true));
        return ir;
    }

    private static ObjectNode stap(ObjectNode ir) {
        ObjectNode t = (ObjectNode) creatie(ir, 0, null).get("tussenstappen").get(0);
        assertTrue(t.get("veld").asText().equals("kustvak"), t.toString());
        return t;
    }

    private static ObjectNode creatie(ObjectNode ir, int i, String feittype) {
        ObjectNode fc = (ObjectNode) ir.get("feitCreaties").get(i);
        if (feittype != null) assertTrue(fc.get("feittype").asText().equals(feittype), fc.toString());
        return fc;
    }

    private static ObjectNode kaveletiket(ObjectNode ir) {
        for (JsonNode u : ir.get("uniciteiten"))
            if (u.get("naam").asText().equals("eenmalig kaveletiket")) return (ObjectNode) u;
        throw new AssertionError("kustregister carries no 'eenmalig kaveletiket'");
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
