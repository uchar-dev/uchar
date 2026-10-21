package dev.uchar.regelspraak.loader;

import com.fasterxml.jackson.databind.ObjectMapper;

import dev.uchar.regelspraak.ir.Default;
import dev.uchar.regelspraak.ir.PrioritizedDefaults;
import dev.uchar.regelspraak.model.RegelSpraakModel;
import dev.uchar.regelspraak.resolver.Resolver;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The names of the defaults along the two routes to a model (IR-SPEC ch. 6.1, {@code Default.name}; card
 * defaults-decider): for every corpus golden with a {@code source.rgs}, every default of the groups the loader reads
 * from the committed {@code ir.json} carries the name the default at the same place carries in the model the resolver
 * makes of the source, by its JSON pointer. A consumer that holds only the IR document names the default that decided
 * as the frontend does. Gate 10 holds the committed document to the emission of that source byte for byte, so the
 * comparison is the frontend's names against its own wire.
 */
class DefaultNameSourceRouteTest {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Path GOLDEN = Path.of("corpus", "golden");

    @Test void per_golden_are_the_names_of_the_wire_those_of_the_source() throws Exception {
        int goldens = 0, named = 0;
        List<Path> directories;
        try (var s = Files.list(GOLDEN)) { directories = s.sorted().toList(); }
        for (Path map : directories) {
            Path source = map.resolve("source.rgs");
            if (!Files.exists(source)) continue;
            RegelSpraakModel viaSource = Resolver.resolve(Files.readString(source));
            assertTrue(viaSource.isOk(), () -> map + " resolvet niet: " + viaSource.diagnostics);
            IrLoader.Loaded viaWire = IrLoader.loadAll(JSON.readTree(Files.readString(map.resolve("ir.json"))));
            List<String> a = names(viaSource.prioritizedDefaults), b = names(viaWire.prioritizedDefaults());
            assertEquals(a, b, map::toString);
            if (a.stream().anyMatch(n -> !n.endsWith(" null"))) goldens++;
            named += (int) a.stream().filter(n -> !n.endsWith(" null")).count();
        }
        // measured 2026-10-21: 20 rgs goldens whose defaults carry 64 names
        assertTrue(goldens >= 20, "rgs-goldens met een benoemde default: " + goldens);
        assertTrue(named >= 64, "benoemde defaults: " + named);
    }

    /** Every default of {@code groups} as its pointer and its name, in document order. */
    private static List<String> names(List<PrioritizedDefaults> groups) {
        List<String> out = new ArrayList<>();
        for (int i = 0; i < groups.size(); i++) names(groups.get(i).defaults(), "/rules/prioritizedDefaults/" + i, out);
        return out;
    }

    private static void names(List<Default> chain, String holder, List<String> out) {
        for (int j = 0; j < chain.size(); j++) {
            Default d = chain.get(j);
            String p = holder + "/defaults/" + j;
            out.add(p + " " + d.name());
            if (d.exceptions() != null)
                for (int m = 0; m < d.exceptions().members().size(); m++)
                    names(d.exceptions().members().get(m).defaults(), p + "/exceptions/members/" + m, out);
        }
    }
}
