package dev.uchar.regelspraak.graaf;

import com.fasterxml.jackson.databind.ObjectMapper;

import dev.uchar.regelspraak.emit.IrEmitter;
import dev.uchar.regelspraak.loader.IrLoader;
import dev.uchar.regelspraak.model.RegelSpraakModel;
import dev.uchar.regelspraak.resolver.Resolver;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * One RegelSpraak text read along the two routes to a model, and the {@link RegelGraaf} of each: the BRON route is
 * the resolver's own model, whose records say themselves whether a kenmerk check navigates; the WIRE route is the
 * model the loader rebuilds from the IR the emitter writes of that model, where the hop rides beside the model. A
 * consumer that holds only {@code ir.json} takes the second route, so every claim these tests make is checked on
 * both graphs, and the two graphs must render byte-identically.
 */
final class TweeRoutes {
    private TweeRoutes() {}

    private static final ObjectMapper JSON = new ObjectMapper();

    /** The two readings of one text: the models, and the graph of each. */
    record Tweeling(RegelSpraakModel bronModel, IrLoader.Loaded wire, RegelGraaf bron, RegelGraaf wireGraaf) {
        /** Both graphs, named by route, for a claim that must hold on each. */
        Map<String, RegelGraaf> grafen() { return Map.of("bron", bron, "wire", wireGraaf); }
    }

    static Tweeling lees(String tekst) {
        RegelSpraakModel m = Resolver.resolve(tekst);
        assertTrue(m.isOk(), () -> "de bron resolvet niet: " + m.diagnostics);
        assertTrue(IrEmitter.wireWeigeringen(m).isEmpty(), () -> "de wire draagt de bron niet: " + IrEmitter.wireWeigeringen(m));
        IrLoader.Loaded w;
        try {
            w = IrLoader.loadAll(JSON.readTree(IrEmitter.emit(m)));
        } catch (Exception e) {
            throw new AssertionError("de lader leest de geëmitteerde IR niet: " + e.getMessage(), e);
        }
        Tweeling t = new Tweeling(m, w, bouwBron(m), bouwWire(w));
        assertEquals(t.bron().mermaid(), t.wireGraaf().mermaid(), "de bronroute en de wireroute tekenen dezelfde graaf");
        return t;
    }

    /** The bron route's navigation answer: the resolver writes the hop's role onto the record. */
    static RegelGraaf bouwBron(RegelSpraakModel m) {
        return RegelGraaf.bouw(m, k -> k.navigatie() != null);
    }

    /** The wire route's navigation answer: the loader keeps the hop beside the model (IR-SPEC ch. 4.5). */
    static RegelGraaf bouwWire(IrLoader.Loaded w) {
        return RegelGraaf.bouw(w.model(), k -> w.navigatieBindingen().containsKey(k));
    }

    /** The graph's node by its human label. */
    static RegelGraaf.Node knoop(RegelGraaf g, String label) {
        return g.nodes().stream().filter(n -> n.label().equals(label)).findFirst()
                .orElseThrow(() -> new AssertionError("geen knoop met label '" + label + "' in " + g.nodes()));
    }

    /** The graph's edges as "label -> label", so an assertion reads on names rather than allocated ids. */
    static List<String> kanten(RegelGraaf g) {
        Map<String, String> label = g.nodes().stream().collect(Collectors.toMap(RegelGraaf.Node::id, RegelGraaf.Node::label));
        return g.edges().stream().map(e -> label.get(e.van()) + " -> " + label.get(e.naar())).toList();
    }
}
