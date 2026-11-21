package dev.uchar.regelspraak.loader;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The root property {@code inferFromEmpty} in the Java loader: {@code false}, its one spelling, is read into the model;
 * absent, the model keeps the reading of RUNTIME-SEMANTICS ch. 1.2–1.4; every other value is a {@link IrLoader.DocumentError}
 * at {@code /inferFromEmpty} in the sentence of the OCaml loader, {@code null} included; and a property the root does
 * not have stays an unknown field. The loader refuses no document that carries the property: the build does
 * ({@code InferFromEmptyRefusalWitnessTest}). The fixture is the golden {@code jutterswet-afleiden-uit-leeg}, read and
 * twisted in memory.
 */
class InferFromEmptyLoaderWitnessTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    /** cwd under surefire is the module dir (backends/drools); the corpus is two levels up. */
    private static final Path GOLDEN = Path.of("..", "..", "corpus", "golden", "jutterswet-afleiden-uit-leeg");

    private static ObjectNode golden() throws Exception {
        return (ObjectNode) JSON.readTree(Files.readString(GOLDEN.resolve("ir.json")));
    }

    @Test void false_is_read_into_the_model_and_absent_keeps_the_default() throws Exception {
        ObjectNode ir = golden();
        assertEquals(JsonNodeFactory.instance.booleanNode(false), ir.get("inferFromEmpty"), "the golden carries it");
        assertFalse(IrLoader.loadAll(ir).model().inferFromEmpty());
        ir.remove("inferFromEmpty");
        assertTrue(IrLoader.loadAll(ir).model().inferFromEmpty());
    }

    @Test void every_other_value_is_refused_at_its_pointer_in_the_sentence_of_the_ocaml_loader() throws Exception {
        JsonNodeFactory f = JsonNodeFactory.instance;
        for (JsonNode value : List.of(f.booleanNode(true), f.nullNode(), f.textNode("false"), f.textNode("nee"),
                f.numberNode(0), f.arrayNode(), f.objectNode())) {
            ObjectNode ir = golden();
            ir.set("inferFromEmpty", value);
            IrLoader.DocumentError e = assertThrows(IrLoader.DocumentError.class, () -> IrLoader.loadAll(ir),
                    () -> "inferFromEmpty " + value);
            assertEquals("/inferFromEmpty", e.site());
            assertEquals("malformed uchar-ir-v1 document at '/inferFromEmpty': expected false, the one spelling of a "
                    + "document that infers nothing from an empty value, got " + value, e.getMessage());
        }
    }

    @Test void a_property_the_root_does_not_have_is_an_unknown_field() throws Exception {
        ObjectNode ir = golden();
        ir.put("inferFromNothing", false);
        IrLoader.DocumentError e = assertThrows(IrLoader.DocumentError.class, () -> IrLoader.loadAll(ir));
        assertEquals("/inferFromNothing", e.site());
        assertEquals("malformed uchar-ir-v1 document at '/inferFromNothing': unknown field", e.getMessage());
    }
}
