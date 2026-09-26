package dev.uchar.regelspraak.graaf;

import com.fasterxml.jackson.databind.ObjectMapper;

import dev.uchar.regelspraak.loader.IrLoader;
import dev.uchar.regelspraak.model.RegelSpraakModel;
import dev.uchar.regelspraak.resolver.Resolver;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The graph works from {@code ir.json}: for every corpus golden that carries its RegelSpraak source, the graph of
 * the model the loader rebuilds from the committed {@code ir.json} renders byte-identically to the graph of the
 * model the resolver builds from {@code bron.rgs}. A consumer without sources therefore gets the same graph. (That
 * the committed {@code ir.json} is what the frontend emits of {@code bron.rgs} is gate 10's to hold.)
 */
class GraafBronrouteTest {

    private static final ObjectMapper JSON = new ObjectMapper();
    // the gate runs from the repository root
    private static final Path CORPUS = Path.of("corpus", "golden");

    @TestFactory Stream<DynamicTest> de_graaf_van_de_wire_is_de_graaf_van_de_bron() throws Exception {
        List<Path> bronnen;
        try (Stream<Path> s = Files.walk(CORPUS)) {
            bronnen = s.filter(p -> p.getFileName().toString().equals("bron.rgs")).sorted().toList();
        }
        assertFalse(bronnen.isEmpty(), "geen corpus/golden/**/bron.rgs gevonden (een lege set is nooit groen)");
        return bronnen.stream().map(rgs -> DynamicTest.dynamicTest(CORPUS.relativize(rgs.getParent()).toString(), () -> {
            // read exactly as the frontend's --ir mode reads one file
            RegelSpraakModel m = Resolver.resolve(Files.readString(rgs) + "\n", rgs.toString());
            assertTrue(m.isOk(), () -> rgs + " resolvet niet: " + m.diagnostics);
            IrLoader.Loaded w = IrLoader.loadAll(JSON.readTree(Files.readString(rgs.resolveSibling("ir.json"))));
            assertEquals(TweeRoutes.bouwBron(m).mermaid(), TweeRoutes.bouwWire(w).mermaid(),
                    rgs.getParent() + ": de graaf van de wire wijkt af van de graaf van de bron");
        }));
    }
}
