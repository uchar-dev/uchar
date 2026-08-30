package io.github.yhavinga.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.yhavinga.regelspraak.loader.IrLoader;
import io.github.yhavinga.regelspraak.model.RegelSpraakModel;
import io.github.yhavinga.regelspraak.schema.FactModelGenerator;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The §13.3.9.2 wederkerig self-pair witness: the reference field {@link FactModelGenerator} derives for a
 * reciprocal role, and the BOTH-DIRECTIONS fill {@link CaseRunner} gives it from case input.
 *
 * <p>Gate 12 replays the two corpus goldens that carry the form and checks their pinned ref≡clingo VALUES —
 * it never sees the field NAME, its TYPE, or which leg of the pair was written. Those are exactly the parts
 * a wrong derivation gets wrong while the totals still happen to land, so they are asserted directly here:
 * off {@link FactModelGenerator#leden} for the derived member, and off {@link CaseRunner#invoerBeans} for the
 * fill. The models are corpus goldens (their {@code ir.json} off the wire); every case is authored here, so
 * no corpus expectation is read or touched.
 *
 * <p>What the derivation must reproduce is the ORIGINAL TOOLCHAIN's observable reading, whose beans carry the
 * self-pair field by hand rather than deriving it: a SCALAR self-typed reference named after the DECLARED
 * SINGULAR role name ({@code Jutter maat}, {@code getMaat()}/{@code setMaat(Jutter)}) — never the declared
 * plural, never a {@code List}. Its generated rules read exactly that: {@code $subject.getMaat().get…()} for
 * a scalar hop and {@code Jutter( maat == $subject )} for the reverse aggregation scope. Here the name is
 * derived SYMBOLICALLY from the role — the wire's inert v0 schema-field baggage is never read.
 *
 * <p>The FILL is mirrored, which the field alone does not imply: RUNTIME-SEMANTICS §4.1 makes one reciprocal
 * entry hold in BOTH directions, and ref's {@code Case.materialise_reciprocal} writes the reverse leg at load
 * so a one-sided case supply reads identically in every executor. Both goldens pin precisely that (a partner
 * that declares no roles of its own still reads its side), so the mirror is load-bearing, not cosmetic.
 */
class WederkerigZelfpaarWitnessTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    // cwd under surefire is the module dir (backends/drools); the corpus is two levels up.
    private static final Path CORPUS = Path.of("..", "..", "corpus", "golden");

    // ---------------- the derived member: name, type, accessors ----------------

    @Test void de_zelfpaarrol_levert_een_scalair_veld_met_de_enkelvoudige_rolnaam() throws Exception {
        RegelSpraakModel model = model("anchor/jutterswet-maatschap");
        var jutter = model.objecttypeOpNaam("Jutter");

        assertEquals("maat", FactModelGenerator.zelfpaarRol(model, jutter),
                "the DECLARED SINGULAR role name, never the declared plural 'maten'");

        FactModelGenerator.Lid lid = FactModelGenerator.leden(model, jutter).stream()
                .filter(l -> l.eigenschap().equals("maat")).findFirst().orElse(null);
        assertNotNull(lid, "the reciprocal role implies a bean member; without it a rule reading it has no getter");
        assertEquals("Jutter", lid.javaType(), "a SCALAR reference to the same fact class — never a List");
        assertTrue(!lid.isVorm(), "a reference is a get-form member, not the boolean is-form");
    }

    @Test void het_veld_draagt_de_getter_en_setter_die_de_regels_lezen() throws Exception {
        // the accessor pair the transpilers assume (Namen.getter/setter over the role name) must exist on the
        // GENERATED source, self-typed on both sides — this is the contract the exec-model build resolves.
        String bron = FactModelGenerator.genereer(model("anchor/jutterswet-maatschap")).get("Jutter");
        assertTrue(bron.contains("private Jutter maat;"), bron);
        assertTrue(bron.contains("public Jutter getMaat()"), bron);
        assertTrue(bron.contains("public void setMaat(Jutter v)"), bron);
    }

    @Test void een_gewoon_objecttype_zonder_wederkerig_feittype_leidt_geen_zelfveld_af() throws Exception {
        // under-implied, never over-implied: the derivation claims the wederkerig shape and nothing else, so
        // an objecttype with no reciprocal feittype gains no self-reference at all.
        RegelSpraakModel model = model("jutterswet-aanmelderkrediet");
        assertNull(FactModelGenerator.zelfpaarRol(model, model.objecttypeOpNaam("Vondst")),
                "Vondst plays no reciprocal role — it must derive no self-pair field");
        assertEquals("strandmaat", FactModelGenerator.zelfpaarRol(model, model.objecttypeOpNaam("Jutter")),
                "while the Jutter, which does, derives its role name");
    }

    // ---------------- the fill: one supplied leg becomes both ----------------

    @Test void een_eenzijdig_geleverde_rol_vult_beide_richtingen() throws Exception {
        // the mirror ref materialises at load. J2 declares NO roles, yet must read J1 as its maat — the two
        // goldens pin exactly this (J2's fold counts 1 and sums J1's vondstwaarde), so a forward-only fill
        // would leave the partner navigating a spuriously empty side.
        Map<String, Object> beans = beans("anchor/jutterswet-maatschap", maatschap("\"maat\": [\"J2\"]", "{}"));
        assertSame(beans.get("J2"), maat(beans.get("J1")), "the written leg J1 -> J2");
        assertSame(beans.get("J1"), maat(beans.get("J2")), "and the materialised reverse leg J2 -> J1");
        assertNull(maat(beans.get("J3")), "an unrelated instance stays unlinked — the mirror writes no more");
    }

    @Test void beide_poten_leveren_is_idempotent() throws Exception {
        // a case that supplies the relation from both sides means the same one relation, so the second leg is
        // a no-op — not a conflict. Without the identity check this would trip the WF-013 refusal below.
        Map<String, Object> beans = beans("anchor/jutterswet-maatschap",
                maatschap("\"maat\": [\"J2\"]", "\"maat\": [\"J1\"]"));
        assertSame(beans.get("J2"), maat(beans.get("J1")));
        assertSame(beans.get("J1"), maat(beans.get("J2")));
    }

    @Test void een_botsende_omgekeerde_poot_faalt_luid() throws Exception {
        // J1.maat = J2 and J2.maat = J3 cannot both hold on a to-one reciprocal role. ref refuses that case at
        // LOAD (WF-013), so silently overwriting a leg here would file a broken case under a green replay.
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> beans("anchor/jutterswet-maatschap", maatschap("\"maat\": [\"J2\"]", "\"maat\": [\"J3\"]")));
        assertTrue(e.getMessage().contains("WF-013"), "the refusal names the rule it enforces: " + e.getMessage());
    }

    @Test void de_botsing_wordt_gezien_ongeacht_de_aanlevervolgorde() throws Exception {
        // the same WF-013 chain with the instance ORDER reversed: spiegelWederkerigeRollen's separate-pass
        // rationale claims order independence, and together with the test above this is its falsifiable
        // witness — a per-write inline mirror sees the colliding leg in only one of the two orders.
        String c = """
                { "format": "uchar-case-v0", "asOfDate": null,
                  "input": { "instances": [
                      { "label": "J2", "objectType": "Jutter", "attributes": { "vondstwaarde": { "number": "12" } },
                        "roles": { "maat": ["J3"] }, "characteristics": [] },
                      { "label": "J3", "objectType": "Jutter", "attributes": { "vondstwaarde": { "number": "7" } },
                        "roles": {}, "characteristics": [] },
                      { "label": "J1", "objectType": "Jutter", "attributes": { "vondstwaarde": { "number": "30" } },
                        "roles": { "maat": ["J2"] }, "characteristics": [] } ],
                    "parameters": {} } }
                """;
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> beans("anchor/jutterswet-maatschap", c));
        assertTrue(e.getMessage().contains("WF-013"), e.getMessage());
    }

    @Test void een_naamdelend_kenmerk_verdringt_het_veld_en_wordt_luid_geweigerd() throws Exception {
        // leden() ports every member through a name-dedup (attribute > kenmerk > reference), so a kenmerk
        // sharing the reciprocal role's name displaces the self-pair field. That shape must be a loud
        // per-form Excluded (the name-sharing bean limit), never an unclassified ERROR at mirror time —
        // authored on an in-memory copy of the golden's ir, the corpus itself untouched.
        com.fasterxml.jackson.databind.node.ObjectNode ir = (com.fasterxml.jackson.databind.node.ObjectNode)
                JSON.readTree(Files.readString(CORPUS.resolve("anchor/jutterswet-maatschap").resolve("ir.json")));
        com.fasterxml.jackson.databind.node.ObjectNode jutter =
                (com.fasterxml.jackson.databind.node.ObjectNode) ir.get("objecttypen").get("Jutter");
        ((com.fasterxml.jackson.databind.node.ArrayNode) jutter.get("kenmerken")).add("maat");
        ((com.fasterxml.jackson.databind.node.ObjectNode) jutter.get("kenmerkSoorten")).put("maat", "BIJVOEGLIJK");
        Path tmp = Files.createTempFile("naamdeling-maat", ".json");
        try {
            Files.writeString(tmp, ir.toString());
            CaseRunner.Excluded e = assertThrows(CaseRunner.Excluded.class,
                    () -> CaseRunner.invoerBeans(tmp, JSON.readTree(maatschap("", "{}"))));
            assertTrue(e.getMessage().contains("name-sharing"), e.getMessage());
        } finally {
            Files.deleteIfExists(tmp);
        }
    }

    // ---------------- the read: what the rules do with the filled field ----------------

    @Test void de_gevulde_rol_wordt_aan_beide_kanten_gelezen() throws Exception {
        // the end-to-end consequence of the two above, on the SCALAR-hop golden ("het strandtegoed van zijn
        // strandmaat"): both partners read the other's value, and a jutter without a maat reads leeg — the
        // reciprocal hop is a real hop, never a degenerate self-read. Authored here, not a corpus expectation.
        assertNull(speelKrediet("""
                { "kind": "attributeValue", "label": "J1", "attribute": "maatdeling", "value": { "number": "25" } },
                { "kind": "attributeValue", "label": "J2", "attribute": "maatdeling", "value": { "number": "10" } },
                { "kind": "attributeEmpty", "label": "J3", "attribute": "maatdeling" }"""));

        // and the mirror is what makes J2's side non-empty: asserting leeg there must FAIL.
        String fout = speelKrediet(
                "{ \"kind\": \"attributeEmpty\", \"label\": \"J2\", \"attribute\": \"maatdeling\" }");
        assertTrue(fout != null, "J2 reads its materialised leg, so an empty expectation is a named mismatch");
    }

    // ---------------- helpers ----------------

    /** The resolved model of a corpus golden, straight off the wire. */
    private static RegelSpraakModel model(String golden) throws Exception {
        Path ir = CORPUS.resolve(golden).resolve("ir.json");
        assertTrue(Files.exists(ir), "golden ir.json present: " + ir.toAbsolutePath());
        return IrLoader.loadAll(JSON.readTree(Files.readString(ir))).model();
    }

    /** The input beans one authored case produces (build + instances + attributes + roles, before fire). */
    private static Map<String, Object> beans(String golden, String caseJson) throws Exception {
        return CaseRunner.invoerBeans(CORPUS.resolve(golden).resolve("ir.json"), JSON.readTree(caseJson));
    }

    /** Three Jutters with the given role objects on J1 and J2 — the maatschap shape, authored here. */
    private static String maatschap(String rollenJ1, String rollenJ2) {
        return """
                { "format": "uchar-case-v0", "asOfDate": null,
                  "input": { "instances": [
                      { "label": "J1", "objectType": "Jutter", "attributes": { "vondstwaarde": { "number": "30" } },
                        "roles": { %s }, "characteristics": [] },
                      { "label": "J2", "objectType": "Jutter", "attributes": { "vondstwaarde": { "number": "12" } },
                        "roles": %s, "characteristics": [] },
                      { "label": "J3", "objectType": "Jutter", "attributes": { "vondstwaarde": { "number": "7" } },
                        "roles": {}, "characteristics": [] } ],
                    "parameters": {} } }
                """.formatted(rollenJ1, rollenJ2.startsWith("{") ? rollenJ2 : "{ " + rollenJ2 + " }");
    }

    /** The aanmelderkrediet scalar-hop replay with the given expectation nodes; the failure line, null on a match. */
    private static String speelKrediet(String expectedNodes) throws Exception {
        String c = """
                { "format": "uchar-case-v0", "asOfDate": null,
                  "input": { "instances": [
                      { "label": "J1", "objectType": "Jutter", "attributes": { "strandtegoed": { "number": "10" } },
                        "roles": { "strandmaat": ["J2"] }, "characteristics": [] },
                      { "label": "J2", "objectType": "Jutter", "attributes": { "strandtegoed": { "number": "25" } },
                        "roles": {}, "characteristics": [] },
                      { "label": "J3", "objectType": "Jutter", "attributes": { "strandtegoed": { "number": "7" } },
                        "roles": {}, "characteristics": [] } ],
                    "parameters": {} },
                  "expected": [ %s ] }
                """.formatted(expectedNodes);
        return CaseRunner.speelCase(CORPUS.resolve("jutterswet-aanmelderkrediet").resolve("ir.json"), JSON.readTree(c));
    }

    /** The maat reference a bean carries, read through the very getter the generated rules use. */
    private static Object maat(Object bean) throws Exception {
        assertNotNull(bean, "the case's instance produced a bean");
        return bean.getClass().getMethod("getMaat").invoke(bean);
    }

    @Test void de_ledenlijst_draagt_het_zelfveld_precies_eenmaal() throws Exception {
        // the self-pair derivation sits OUTSIDE the per-doelwit loop, so it cannot depend on how many
        // objecttypen the model happens to carry; a duplicate would make the generated bean uncompilable.
        RegelSpraakModel model = model("anchor/jutterswet-maatschap");
        List<FactModelGenerator.Lid> leden = FactModelGenerator.leden(model, model.objecttypeOpNaam("Jutter"));
        assertEquals(1, leden.stream().filter(l -> l.eigenschap().equals("maat")).count(),
                "one bean property per name — the self-pair field is derived once, not per objecttype visited");
    }
}
