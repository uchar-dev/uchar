package dev.uchar.regelspraak.loader;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.uchar.regelspraak.cases.IrPath;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The nameless-declaration witness. Until M1 a keyed declaration whose {@code name} was null took its MAP KEY as its
 * name (WF-094 clause (d), maintainer decision 2026-09-09), on all five map-shaped tables; before that fall-back
 * existed, a null name reached the identifier contract and the load died in a NullPointerException. M1 turned the
 * five tables into lists (delta B2): the name is written once, on the node, so there is no key left to stand in for
 * it and a nameless declaration has no name at all. The loader refuses it by the node's own name field instead of
 * keying the model on null — since 2026-09-23 as a document defect ({@link IrLoader.DocumentError}, a hard error at
 * the JSON position of that field), no longer as a "form not carried" {@link IrLoader.Refusal}, which the
 * differential gate would have filed under its auto-excluded channel.
 *
 * <p>The five tables are pinned one by one, as the fall-back was: a test that covered only some of them would let a
 * later edit take the refusal away from the others.
 */
class SelfNameFallbackWitnessTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    // cwd under surefire is the module dir (backends/drools); the corpus is two levels up.
    private static final Path CORPUS = Path.of("..", "..", "corpus", "golden");

    @Test void object_type_without_a_name_is_refused() throws Exception {
        refusesWithoutName("anchor/jutterswet-maatschap", "objectTypes", "name", "Jutter");
    }

    @Test void parameter_without_a_name_is_refused() throws Exception {
        refusesWithoutName("jutterswet-penningdrempel", "parameters", "name", "vondstdrempel");
    }

    @Test void unit_system_without_a_name_is_refused() throws Exception {
        refusesWithoutName("jutterswet-berging", "unitSystems", "name", "Munt");
    }

    @Test void fact_type_without_a_name_is_refused() throws Exception {
        refusesWithoutName("jutterswet-vondsttelling", "factTypes", "name", "vondstaanmelding");
    }

    /** The dimension arm has no PLAYABLE witness — the goldens carrying a {@code dimensions} list have no case
     *  file — so the loader level is the only place its refusal can be pinned at all. */
    @Test void dimension_without_a_name_is_refused() throws Exception {
        refusesWithoutName("jutterswet-seizoenvondst", "dimensions", "name", "seizoen");
    }

    /** Loads a golden with ONE declaration's name replaced by null and demands the refusal at that field. */
    private void refusesWithoutName(String golden, String list, String nameField, String name) throws Exception {
        Path ir = CORPUS.resolve(golden).resolve("ir.json");
        assertTrue(Files.exists(ir), "golden ir.json present: " + ir.toAbsolutePath());
        JsonNode root = JSON.readTree(Files.readString(ir));
        ArrayNode declarations = IrPath.modelList(root, list);
        ObjectNode declaration = IrPath.memberWith(declarations, nameField, name);
        int index = 0;
        while (declarations.get(index) != declaration) index++;
        declaration.putNull(nameField);

        IrLoader.DocumentError e = assertThrows(IrLoader.DocumentError.class, () -> IrLoader.load(root),
                golden + ": a nameless declaration must be refused, not keyed on null");
        assertEquals("/model/" + list + "/" + index + "/" + nameField, e.site(), e.getMessage());
        assertTrue(e.getMessage().contains("a declaration without a name"), e.getMessage());
    }
}
