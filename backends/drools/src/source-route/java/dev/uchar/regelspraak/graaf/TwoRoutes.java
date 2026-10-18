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
final class TwoRoutes {
    private TwoRoutes() {}

    private static final ObjectMapper JSON = new ObjectMapper();

    /** The two readings of one text: the models, and the graph of each. */
    record Twin(RegelSpraakModel sourceModel, IrLoader.Loaded wire, RegelGraaf bron, RegelGraaf wireGraph) {
        /** Both graphs, named by route, for a claim that must hold on each. */
        Map<String, RegelGraaf> graphs() { return Map.of("bron", bron, "wire", wireGraph); }
    }

    static Twin lees(String text) {
        RegelSpraakModel m = Resolver.resolve(text);
        assertTrue(m.isOk(), () -> "de bron resolvet niet: " + m.diagnostics);
        // an allocation the wire has no spelling for stays out of the IR, so the two routes would read two models
        assertTrue(IrEmitter.ongeschrevenVerdelingen(m).isEmpty(),
                () -> "de wire laat een verdeling weg: " + IrEmitter.ongeschrevenVerdelingen(m));
        IrLoader.Loaded w;
        try {
            w = IrLoader.loadAll(JSON.readTree(IrEmitter.emit(m)));
        } catch (Exception e) {
            throw new AssertionError("de lader leest de geëmitteerde IR niet: " + e.getMessage(), e);
        }
        Twin t = new Twin(m, w, buildSource(m), buildWire(w));
        assertEquals(t.bron().mermaid(), t.wireGraph().mermaid(), "de bronroute en de wireroute tekenen dezelfde graaf");
        return t;
    }

    /** The bron route's side tables: the resolver's model holds the defaults groups, and its records name the role
     *  a hop walks. */
    static RegelGraaf buildSource(RegelSpraakModel m) {
        return RegelGraaf.bouw(m, m.prioritizedDefaults, ProducentConsument.bronNavigatie(m));
    }

    /** The wire route's side tables: the loader keeps the defaults groups and the hops beside the model (IR-SPEC
     *  ch. 4.5). */
    static RegelGraaf buildWire(IrLoader.Loaded w) {
        return RegelGraaf.bouw(w.model(), w.prioritizedDefaults(), ProducentConsument.wireNavigatie(w));
    }

    /** The graph's node by its human label. */
    static RegelGraaf.Node jsonNode(RegelGraaf g, String label) {
        return g.nodes().stream().filter(n -> n.label().equals(label)).findFirst()
                .orElseThrow(() -> new AssertionError("geen knoop met label '" + label + "' in " + g.nodes()));
    }

    /** The graph's edges as "label -> label", so an assertion reads on names rather than allocated ids. */
    static List<String> edges(RegelGraaf g) {
        Map<String, String> label = g.nodes().stream().collect(Collectors.toMap(RegelGraaf.Node::id, RegelGraaf.Node::label));
        return g.edges().stream().map(e -> label.get(e.van()) + " -> " + label.get(e.naar())).toList();
    }
}
