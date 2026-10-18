package dev.uchar.regelspraak.loader;

import com.fasterxml.jackson.databind.ObjectMapper;

import dev.uchar.regelspraak.model.RegelSpraakModel;
import dev.uchar.regelspraak.resolver.Resolver;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The origins along the two routes to a model (IR-SPEC ch. 2.13): for every corpus golden with a {@code bron.rgs},
 * the table {@code gegenereerd} of the model the loader reads from the committed {@code ir.json} equals the table of
 * the model the resolver makes of the source. A consumer that holds only the IR document then knows which
 * declarations the frontend made, and from what, as the frontend does. Gate 10 holds the committed document to the
 * emission of that source byte for byte, so the comparison is the frontend's table against its own wire.
 */
class OriginSourceRouteTest {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Path GOLDEN = Path.of("corpus", "golden");

    @Test void per_golden_is_the_table_of_the_wire_that_of_the_source() throws Exception {
        int goldens = 0, withOrigin = 0;
        List<Path> directories;
        try (var s = Files.list(GOLDEN)) { directories = s.sorted().toList(); }
        for (Path map : directories) {
            Path source = map.resolve("bron.rgs");
            if (!Files.exists(source)) continue;
            RegelSpraakModel viaSource = Resolver.resolve(Files.readString(source));
            assertTrue(viaSource.isOk(), () -> map + " resolvet niet: " + viaSource.diagnostics);
            RegelSpraakModel viaWire = IrLoader.load(JSON.readTree(Files.readString(map.resolve("ir.json"))));
            assertEquals(viaSource.generated, viaWire.generated, map::toString);
            goldens++;
            if (!viaSource.generated.isEmpty()) withOrigin++;
        }
        // measured 2026-10-15: 125 rgs goldens, 9 of them with a declaration the frontend made
        assertTrue(goldens >= 125, "rgs-goldens vergeleken: " + goldens);
        assertTrue(withOrigin >= 9, "rgs-goldens met een gemaakte declaratie: " + withOrigin);
    }
}
