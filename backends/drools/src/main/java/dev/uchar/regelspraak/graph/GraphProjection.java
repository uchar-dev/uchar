package dev.uchar.regelspraak.graph;

import com.fasterxml.jackson.databind.ObjectMapper;

import dev.uchar.regelspraak.loader.IrLoader;
import dev.uchar.regelspraak.transpiler.core.Namen;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.TreeSet;

/**
 * The command line the rule-graph gate drives (scripts/check-graph.sh): for every {@code ir.json} named, the
 * {@link RuleGraph} of the model the LOADER rebuilds from the wire, with the defaults groups and the navigation
 * bindings the loader returns beside it, printed as its dependencies. One block per document:
 *
 * <pre>
 *   document  &lt;path&gt;
 *   rand      &lt;list&gt;  &lt;producent&gt;  attr|char  &lt;lid&gt;  &lt;list&gt;  &lt;consument&gt;   one per dependency, sorted, no repeats
 *   weigering &lt;reason&gt;                                                   instead, when the loader refuses a form
 * </pre>
 *
 * Fields are tab-separated. A form is named by the document list it sits in ({@code rules}, {@code objectCreations},
 * {@code allocations}, {@code consistencies}, {@code uniquenesses}, {@code factCreations},
 * {@code prioritizedDefaults}) and by its name, normalised as the verifier normalises it ({@link Namen#norm}; an
 * element's name already is, see {@link ProducerConsumer.ElementKey}), so the lines compare with the
 * verifier's {@code --dependency-edges} report projected onto attributes and characteristics. The type of an
 * element is left out on purpose: this graph keys a member on its DECLARING type and the verifier's graph on the
 * type a read spells, so the two agree on which forms depend on which member and not on the spelling of the type
 * in between.
 */
public final class GraphProjection {
    private GraphProjection() {}

    public static void main(String[] args) throws Exception {
        ObjectMapper json = new ObjectMapper();
        for (String path : args) {
            System.out.println("document\t" + path);
            IrLoader.Loaded loaded;
            try {
                loaded = IrLoader.loadAll(json.readTree(Files.readString(Path.of(path))));
            } catch (IrLoader.Refusal e) {
                System.out.println("weigering\t" + e.getMessage().replace('\n', ' '));
                continue;
            }
            RuleGraph g = RuleGraph.build(loaded.model(), loaded.prioritizedDefaults(),
                    ProducerConsumer.wireNavigation(loaded));
            TreeSet<String> rules = new TreeSet<>();
            for (RuleGraph.Dependency a : g.dependencies())
                rules.add(String.join("\t", "rand", list(a.producerForm()), Namen.norm(a.producer()),
                        a.element().characteristic() ? "char" : "attr", a.element().name(),
                        list(a.consumerForm()), Namen.norm(a.consumer())));
            rules.forEach(System.out::println);
        }
    }

    /** The document list a form of this kind sits in ({@code /rules/<list>}), the class the verifier's pointer
     *  names. */
    private static String list(ProducerConsumer.FormKind category) {
        return switch (category) {
            case REGEL -> "rules";
            case OBJECT_CREATION -> "objectCreations";
            case ALLOCATION -> "allocations";
            case CONSISTENCY -> "consistencies";
            case UNIQUENESS -> "uniquenesses";
            case FACT_CREATION -> "factCreations";
            case DEFAULTS_GROUP -> "prioritizedDefaults";
        };
    }
}
