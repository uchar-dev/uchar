package dev.uchar.regelspraak.loader;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import dev.uchar.regelspraak.model.RegelSpraakModel;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * The member field {@code complete} in the Java loader (IR-SPEC ch. 2.2): {@code true}, its one spelling, on an
 * Attribute or a Characteristic is read into the model's {@code completeAttributes} and {@code completeCharacteristics},
 * as the frontend holds it; absent, the member is in neither; every other value is a {@link IrLoader.DocumentError} at
 * {@code …/complete} in the sentence of the OCaml loader, {@code null} included; and a misspelt field stays an unknown
 * field. The loader gives the field no meaning: the build refuses a document with {@code inferFromEmpty: false}
 * ({@code InferFromEmptyRefusalWitnessTest}), and without that property the field changes no outcome. The fixture is
 * the golden {@code jutterswet-afleiden-uit-leeg}, read and twisted in memory: its attribute {@code gewicht} and its
 * characteristic {@code gemeld}, both input there.
 */
class CompleteLoaderWitnessTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    /** cwd under surefire is the module dir (backends/drools); the corpus is two levels up. */
    private static final Path GOLDEN = Path.of("..", "..", "corpus", "golden", "jutterswet-afleiden-uit-leeg");

    private static ObjectNode golden() throws Exception {
        return (ObjectNode) JSON.readTree(Files.readString(GOLDEN.resolve("ir.json")));
    }

    private static ObjectNode vondst(ObjectNode ir) {
        return (ObjectNode) ir.get("model").get("objectTypes").get(0);
    }

    /** The member of the Vondst named {@code name} in the list {@code list}, with its pointer. */
    private static ObjectNode member(ObjectNode ir, String list, String name) {
        for (JsonNode m : vondst(ir).get(list))
            if (m.get("name").asText().equals(name)) return (ObjectNode) m;
        throw new IllegalStateException("the golden declares no " + list + " member '" + name + "'");
    }

    private static String pointer(ObjectNode ir, String list, String name) {
        int i = 0;
        for (JsonNode m : vondst(ir).get(list)) {
            if (m.get("name").asText().equals(name)) return "/model/objectTypes/0/" + list + "/" + i;
            i++;
        }
        throw new IllegalStateException("the golden declares no " + list + " member '" + name + "'");
    }

    private static RegelSpraakModel.ObjectType loadedVondst(ObjectNode ir) {
        return IrLoader.loadAll(ir).model().objectTypes.get("Vondst");
    }

    @Test void true_is_read_into_the_model_and_absent_leaves_the_member_out() throws Exception {
        ObjectNode ir = golden();
        RegelSpraakModel.ObjectType plain = loadedVondst(ir);
        assertEquals(Set.of(), plain.completeAttributes, "the golden declares no member complete");
        assertEquals(Set.of(), plain.completeCharacteristics);
        member(ir, "attributes", "gewicht").put("complete", true);
        member(ir, "characteristics", "gemeld").put("complete", true);
        addFactType(ir).put("complete", true);
        RegelSpraakModel loaded = IrLoader.loadAll(ir).model();
        RegelSpraakModel.ObjectType twisted = loaded.objectTypes.get("Vondst");
        assertEquals(Set.of("gewicht"), twisted.completeAttributes);
        assertEquals(Set.of("gemeld"), twisted.completeCharacteristics);
        assertEquals(true, loaded.factTypes.get("vinding").roles.get(0).complete());
        assertEquals(false, loaded.factTypes.get("vinding").roles.get(1).complete());
    }

    private static ObjectNode addFactType(ObjectNode ir) {
        JsonNodeFactory f = JsonNodeFactory.instance;
        ObjectNode r0 = f.objectNode();
        r0.put("node", "Role");
        r0.put("name", "vinder");
        r0.put("objectType", "Vondst");
        r0.put("many", false);
        ObjectNode r1 = f.objectNode();
        r1.put("node", "Role");
        r1.put("name", "hervondst");
        r1.put("objectType", "Vondst");
        r1.put("many", true);
        ObjectNode ft = f.objectNode();
        ft.put("node", "FactType");
        ft.put("name", "vinding");
        ft.put("reciprocal", false);
        ft.set("roles", f.arrayNode().add(r0).add(r1));
        ((com.fasterxml.jackson.databind.node.ArrayNode) ir.get("model").get("factTypes")).add(ft);
        return r0;
    }

    @Test void every_other_value_is_refused_at_its_pointer_in_the_sentence_of_the_ocaml_loader() throws Exception {
        JsonNodeFactory f = JsonNodeFactory.instance;
        for (String list : List.of("attributes", "characteristics", "roles")) {
            String name = list.equals("attributes") ? "gewicht" : "gemeld";
            for (JsonNode value : List.of(f.booleanNode(false), f.nullNode(), f.textNode("true"), f.textNode("volledig"),
                    f.numberNode(1), f.arrayNode(), f.objectNode())) {
                ObjectNode ir = golden();
                String at;
                if (list.equals("roles")) {
                    addFactType(ir).set("complete", value);
                    at = "/model/factTypes/0/roles/0/complete";
                } else {
                    member(ir, list, name).set("complete", value);
                    at = pointer(ir, list, name) + "/complete";
                }
                IrLoader.DocumentError e = assertThrows(IrLoader.DocumentError.class, () -> IrLoader.loadAll(ir),
                        () -> list + " complete " + value);
                assertEquals(at, e.site());
                assertEquals("malformed uchar-ir-v1 document at '" + at + "': expected true, the one spelling of a "
                        + "member whose input is complete, got " + value, e.getMessage());
            }
        }
    }

    @Test void a_misspelt_field_is_an_unknown_field() throws Exception {
        ObjectNode ir = golden();
        member(ir, "attributes", "gewicht").put("completed", true);
        IrLoader.DocumentError e = assertThrows(IrLoader.DocumentError.class, () -> IrLoader.loadAll(ir));
        assertEquals(pointer(ir, "attributes", "gewicht") + "/completed", e.site());
        assertEquals("malformed uchar-ir-v1 document at '" + e.site() + "': unknown field on Attribute", e.getMessage());

        ObjectNode irRole = golden();
        addFactType(irRole).put("completed", true);
        IrLoader.DocumentError er = assertThrows(IrLoader.DocumentError.class, () -> IrLoader.loadAll(irRole));
        assertEquals("/model/factTypes/0/roles/0/completed", er.site());
        assertEquals("malformed uchar-ir-v1 document at '/model/factTypes/0/roles/0/completed': unknown field on Role",
                er.getMessage());
    }
}
