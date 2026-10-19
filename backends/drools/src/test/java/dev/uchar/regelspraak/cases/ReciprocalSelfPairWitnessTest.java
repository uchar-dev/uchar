package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import dev.uchar.regelspraak.loader.IrLoader;
import dev.uchar.regelspraak.model.RegelSpraakModel;
import dev.uchar.regelspraak.schema.FactModelGenerator;

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
 * off {@link FactModelGenerator#members} for the derived member, and off {@link CaseRunner#inputBeans} for the
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
 * <p>The FILL is mirrored, which the field alone does not imply: RUNTIME-SEMANTICS ch. 4.1 makes one reciprocal
 * entry hold in BOTH directions — since 2026-09-10 the reciprocal case of ch. 5.7a's general law — and ref's
 * {@code Case.materialise_relations} writes the reverse leg at load
 * so a one-sided case supply reads identically in every executor. Both goldens pin precisely that (a partner
 * that declares no roles of its own still reads its side), so the mirror is load-bearing, not cosmetic.
 */
class ReciprocalSelfPairWitnessTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    // cwd under surefire is the module dir (backends/drools); the corpus is two levels up.
    private static final Path CORPUS = Path.of("..", "..", "corpus", "golden");

    // ---------------- the derived member: name, type, accessors ----------------

    @Test void the_self_pair_role_yields_a_scalar_field_with_the_single_role_name() throws Exception {
        RegelSpraakModel model = model("anchor/jutterswet-maatschap");
        var jutter = model.objectTypeByName("Jutter");

        assertEquals("maat", FactModelGenerator.selfPairRole(model, jutter),
                "the DECLARED SINGULAR role name, never the declared plural 'maten'");

        FactModelGenerator.Member member = FactModelGenerator.members(model, jutter).stream()
                .filter(l -> l.property().equals("maat")).findFirst().orElse(null);
        assertNotNull(member, "the reciprocal role implies a bean member; without it a rule reading it has no getter");
        assertEquals("Jutter", member.javaType(), "a SCALAR reference to the same fact class — never a List");
        assertTrue(!member.isForm(), "a reference is a get-form member, not the boolean is-form");
    }

    @Test void the_field_carries_the_getter_and_setter_that_the_rules_read() throws Exception {
        // the accessor pair the transpilers assume (Namen.getter/setter over the role name) must exist on the
        // GENERATED source, self-typed on both sides — this is the contract the exec-model build resolves.
        String source = FactModelGenerator.generate(model("anchor/jutterswet-maatschap")).get("Jutter");
        assertTrue(source.contains("private Jutter maat;"), source);
        assertTrue(source.contains("public Jutter getMaat()"), source);
        assertTrue(source.contains("public void setMaat(Jutter v)"), source);
    }

    @Test void an_object_type_without_reciprocal_fact_type_derives_no_self_field() throws Exception {
        // under-implied, never over-implied: the derivation claims the wederkerig shape and nothing else, so
        // an objecttype with no reciprocal feittype gains no self-reference at all.
        RegelSpraakModel model = model("jutterswet-aanmelderkrediet");
        assertNull(FactModelGenerator.selfPairRole(model, model.objectTypeByName("Vondst")),
                "Vondst plays no reciprocal role — it must derive no self-pair field");
        assertEquals("strandmaat", FactModelGenerator.selfPairRole(model, model.objectTypeByName("Jutter")),
                "while the Jutter, which does, derives its role name");
    }

    // ---------------- the fill: one supplied leg becomes both ----------------

    @Test void a_one_sided_supplied_role_fills_both_directions() throws Exception {
        // the mirror ref materialises at load. J2 declares NO roles, yet must read J1 as its maat — the two
        // goldens pin exactly this (J2's fold counts 1 and sums J1's vondstwaarde), so a forward-only fill
        // would leave the partner navigating a spuriously empty side.
        Map<String, Object> beans = beans("anchor/jutterswet-maatschap", maatschap("\"maat\": [\"J2\"]", "{}"));
        assertSame(beans.get("J2"), maat(beans.get("J1")), "the written leg J1 -> J2");
        assertSame(beans.get("J1"), maat(beans.get("J2")), "and the materialised reverse leg J2 -> J1");
        assertNull(maat(beans.get("J3")), "an unrelated instance stays unlinked — the mirror writes no more");
    }

    @Test void both_legs_supply_is_idempotent() throws Exception {
        // a case that supplies the relation from both sides means the same one relation, so the second leg is
        // a no-op — not a conflict. Without the identity check this would trip the WF-013 refusal below.
        Map<String, Object> beans = beans("anchor/jutterswet-maatschap",
                maatschap("\"maat\": [\"J2\"]", "\"maat\": [\"J1\"]"));
        assertSame(beans.get("J2"), maat(beans.get("J1")));
        assertSame(beans.get("J1"), maat(beans.get("J2")));
    }

    @Test void a_colliding_reversed_leg_fails_loudly() throws Exception {
        // J1.maat = J2 and J2.maat = J3 cannot both hold on a to-one reciprocal role. ref refuses that case at
        // LOAD (WF-013), so silently overwriting a leg here would file a broken case under a green replay.
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> beans("anchor/jutterswet-maatschap", maatschap("\"maat\": [\"J2\"]", "\"maat\": [\"J3\"]")));
        assertTrue(e.getMessage().contains("WF-013"), "the refusal names the rule it enforces: " + e.getMessage());
    }

    @Test void the_collision_is_seen_regardless_of_the_delivery_order() throws Exception {
        // the same WF-013 chain with the instance ORDER reversed: spiegelReciprocalRoles's separate-pass
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

    @Test void a_name_dividing_characteristic_displaces_the_field_and_is_refused_loudly() throws Exception {
        // members() ports every member through a name-dedup (attribute > kenmerk > reference), so a kenmerk
        // sharing the reciprocal role's name displaces the self-pair field. That shape must be a loud
        // per-form Excluded (the name-sharing bean limit), never an unclassified ERROR at mirror time —
        // authored on an in-memory copy of the golden's ir, the corpus itself untouched.
        com.fasterxml.jackson.databind.node.ObjectNode ir = (com.fasterxml.jackson.databind.node.ObjectNode)
                JSON.readTree(Files.readString(CORPUS.resolve("anchor/jutterswet-maatschap").resolve("ir.json")));
        ((com.fasterxml.jackson.databind.node.ArrayNode) IrPath.objectType(ir, "Jutter").get("characteristics"))
                .addObject().put("node", "Characteristic").put("name", "maat").put("kind", "adjectival");
        Path tmp = Files.createTempFile("naamdeling-maat", ".json");
        try {
            Files.writeString(tmp, ir.toString());
            CaseRunner.Excluded e = assertThrows(CaseRunner.Excluded.class,
                    () -> CaseRunner.inputBeans(tmp, JSON.readTree(maatschap("", "{}"))));
            assertTrue(e.getMessage().contains("name-sharing"), e.getMessage());
        } finally {
            Files.deleteIfExists(tmp);
        }
    }

    @Test void a_second_other_target_on_a_to_one_role_fails_loudly() throws Exception {
        // "maat": ["J2","J3"] on a to-one reciprocal role: ref refuses this case at load (a to-one role
        // carrying 2 labels), so last-wins here would drop a leg and play a broken case green — the mirror
        // pass's argument, applied to the forward leg.
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> beans("anchor/jutterswet-maatschap", maatschap("\"maat\": [\"J2\", \"J3\"]", "{}")));
        assertTrue(e.getMessage().contains("WF-013"), e.getMessage());
    }

    @Test void an_ambiguous_self_pair_form_is_refused_loudly() throws Exception {
        // two wederkerig feittypen on one objecttype: §13.3.9.2 admits the shape, but the by-pair derivation
        // is ambiguous (connectingRole counts two candidates) and no field is derivable — before this refusal
        // the transpilers navigated by the written role name toward a getter no bean carries, an unclassified
        // fire-time ERROR. Authored on an in-memory ir copy, the corpus untouched.
        com.fasterxml.jackson.databind.node.ObjectNode ir = (com.fasterxml.jackson.databind.node.ObjectNode)
                JSON.readTree(Files.readString(CORPUS.resolve("anchor/jutterswet-maatschap").resolve("ir.json")));
        com.fasterxml.jackson.databind.node.ObjectNode ft2 = IrPath.declaration(ir, "factTypes", "maatschap").deepCopy();
        ft2.put("name", "maatschap2");
        ((com.fasterxml.jackson.databind.node.ObjectNode) ft2.get("roles").get(0)).put("name", "tweedemaat");
        ((com.fasterxml.jackson.databind.node.ObjectNode) ft2.get("roles").get(0)).put("plural", "tweedematen");
        IrPath.modelList(ir, "factTypes").add(ft2);
        Path tmp = Files.createTempFile("ambigu-zelfpaar", ".json");
        try {
            Files.writeString(tmp, ir.toString());
            CaseRunner.Excluded e = assertThrows(CaseRunner.Excluded.class,
                    () -> CaseRunner.inputBeans(tmp, JSON.readTree(maatschap("", "{}"))));
            assertTrue(e.getMessage().contains("derives no scalar reference field"), e.getMessage());
        } finally {
            Files.deleteIfExists(tmp);
        }
    }

    // ---------------- the read: what the rules do with the filled field ----------------

    @Test void the_filled_role_is_read_on_both_sides() throws Exception {
        // the end-to-end consequence of the two above, on the SCALAR-hop golden ("het strandtegoed van zijn
        // strandmaat"): both partners read the other's value, and a jutter without a maat reads leeg — the
        // reciprocal hop is a real hop, never a degenerate self-read. Authored here, not a corpus expectation.
        assertNull(playCredit("""
                { "kind": "attributeValue", "label": "J1", "attribute": "maatdeling", "value": { "number": "25" } },
                { "kind": "attributeValue", "label": "J2", "attribute": "maatdeling", "value": { "number": "10" } },
                { "kind": "attributeEmpty", "label": "J3", "attribute": "maatdeling" }"""));

        // and the mirror is what makes J2's side non-empty: asserting leeg there must FAIL.
        String fault = playCredit(
                "{ \"kind\": \"attributeEmpty\", \"label\": \"J2\", \"attribute\": \"maatdeling\" }");
        assertTrue(fault != null, "J2 reads its materialised leg, so an empty expectation is a named mismatch");
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
        return CaseRunner.inputBeans(CORPUS.resolve(golden).resolve("ir.json"), JSON.readTree(caseJson));
    }

    /** Three Jutters with the given role objects on J1 and J2 — the maatschap shape, authored here. */
    private static String maatschap(String rolesJ1, String rolesJ2) {
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
                """.formatted(rolesJ1, rolesJ2.startsWith("{") ? rolesJ2 : "{ " + rolesJ2 + " }");
    }

    /** The aanmelderkrediet scalar-hop replay with the given expectation nodes; the failure line, null on a match. */
    private static String playCredit(String expectedNodes) throws Exception {
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
        return CaseRunner.playCase(CORPUS.resolve("jutterswet-aanmelderkrediet").resolve("ir.json"), JSON.readTree(c));
    }

    /** The maat reference a bean carries, read through the very getter the generated rules use. */
    private static Object maat(Object bean) throws Exception {
        assertNotNull(bean, "the case's instance produced a bean");
        return bean.getClass().getMethod("getMaat").invoke(bean);
    }

    @Test void the_members_list_carries_the_self_field_exactly_once() throws Exception {
        // the self-pair derivation sits OUTSIDE the per-doelwit loop, so it cannot depend on how many
        // objecttypen the model happens to carry; a duplicate would make the generated bean uncompilable.
        RegelSpraakModel model = model("anchor/jutterswet-maatschap");
        List<FactModelGenerator.Member> members = FactModelGenerator.members(model, model.objectTypeByName("Jutter"));
        assertEquals(1, members.stream().filter(l -> l.property().equals("maat")).count(),
                "one bean property per name — the self-pair field is derived once, not per objecttype visited");
    }
}
