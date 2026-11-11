package dev.uchar.regelspraak.loader;

import dev.uchar.regelspraak.cases.DroolsBackend;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A key that stands twice in one object of a document is a load error: a reader that keeps the first value and one
 * that keeps the last would load two documents from one text. The fixture is {@code corpus/golden/jutterswet-gelijkdeling}
 * with the attribute {@code veilingdeel} carrying a second {@code name}, {@code veilingaandeel}.
 *
 * <p>Red before the strict reader: the build kept the last name and refused the allocation as a form it does not carry,
 * "EXCLUDE DROOLS-276 … 'veilingdeel' is not declared", where the document is not well-formed.
 */
class DuplicateKeyWitnessTest {

    private static final Path GOLDEN = Path.of("..", "..", "corpus", "golden", "jutterswet-gelijkdeling", "ir.json");

    @TempDir Path tmp;

    @Test void a_key_twice_in_one_object_is_a_document_error() throws Exception {
        String text = Files.readString(GOLDEN);
        String once = "\"name\": \"veilingdeel\",";
        assertEquals(text.indexOf(once), text.lastIndexOf(once), "the fixture names the attribute once");
        Path ir = tmp.resolve("ir.json");
        Files.writeString(ir, text.replace(once, once + " \"name\": \"veilingaandeel\","));
        for (DroolsBackend.Branch branch : DroolsBackend.Branch.values()) {
            IrLoader.DocumentError e = assertThrows(IrLoader.DocumentError.class, () -> DroolsBackend.build(ir, branch),
                    branch.name());
            assertEquals("/model/objectTypes/1/attributes/0/name", e.site(), e.getMessage());
            assertTrue(e.getMessage().contains("the key 'name' stands twice in one object"), e.getMessage());
        }
    }

    @Test void the_golden_itself_reads() throws Exception {
        IrLoader.load(IrLoader.read(Files.readString(GOLDEN)));
    }
}
