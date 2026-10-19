package dev.uchar.regelspraak.graph;

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
 * model the resolver builds from {@code source.rgs}. A consumer without sources therefore gets the same graph. (That
 * the committed {@code ir.json} is what the frontend emits of {@code source.rgs} is gate 10's to hold.)
 */
class GraphSourceRouteTest {

    private static final ObjectMapper JSON = new ObjectMapper();
    // the gate runs from the repository root
    private static final Path CORPUS = Path.of("corpus", "golden");

    @TestFactory Stream<DynamicTest> the_graph_of_the_wire_is_the_graph_of_the_source() throws Exception {
        List<Path> sources;
        try (Stream<Path> s = Files.walk(CORPUS)) {
            sources = s.filter(p -> p.getFileName().toString().equals("source.rgs")).sorted().toList();
        }
        assertFalse(sources.isEmpty(), "geen corpus/golden/**/source.rgs gevonden (een lege set is nooit groen)");
        return sources.stream().map(rgs -> DynamicTest.dynamicTest(CORPUS.relativize(rgs.getParent()).toString(), () -> {
            // read exactly as the frontend's --ir mode reads one file
            RegelSpraakModel m = Resolver.resolve(Files.readString(rgs) + "\n", rgs.toString());
            assertTrue(m.isOk(), () -> rgs + " resolvet niet: " + m.diagnostics);
            IrLoader.Loaded w = IrLoader.loadAll(JSON.readTree(Files.readString(rgs.resolveSibling("ir.json"))));
            assertEquals(TwoRoutes.buildSource(m).mermaid(), TwoRoutes.buildWire(w).mermaid(),
                    rgs.getParent() + ": de graaf van de wire wijkt af van de graaf van de bron");
        }));
    }
}
