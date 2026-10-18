package dev.uchar.regelspraak.graaf;

import com.fasterxml.jackson.databind.ObjectMapper;

import dev.uchar.regelspraak.loader.IrLoader;
import dev.uchar.regelspraak.transpiler.kern.Namen;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.TreeSet;

/**
 * The command line the rule-graph gate drives (scripts/check-graaf.sh): for every {@code ir.json} named, the
 * {@link RegelGraaf} of the model the LOADER rebuilds from the wire, with the defaults groups and the navigation
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
 * element's name already is, see {@link ProducentConsument.ElementSleutel}), so the lines compare with the
 * verifier's {@code --dependency-edges} report projected onto attributes and characteristics. The type of an
 * element is left out on purpose: this graph keys a member on its DECLARING type and the verifier's graph on the
 * type a read spells, so the two agree on which forms depend on which member and not on the spelling of the type
 * in between.
 */
public final class GraphProjection {
    private GraphProjection() {}

    public static void main(String[] args) throws Exception {
        ObjectMapper json = new ObjectMapper();
        for (String pad : args) {
            System.out.println("document\t" + pad);
            IrLoader.Loaded loaded;
            try {
                loaded = IrLoader.loadAll(json.readTree(Files.readString(Path.of(pad))));
            } catch (IrLoader.Refusal e) {
                System.out.println("weigering\t" + e.getMessage().replace('\n', ' '));
                continue;
            }
            RegelGraaf g = RegelGraaf.bouw(loaded.model(), loaded.prioritizedDefaults(),
                    ProducentConsument.wireNavigatie(loaded));
            TreeSet<String> rules = new TreeSet<>();
            for (RegelGraaf.Dependency a : g.dependencies())
                rules.add(String.join("\t", "rand", lijst(a.producerForm()), Namen.norm(a.producer()),
                        a.element().kenmerk() ? "char" : "attr", a.element().naam(),
                        lijst(a.consumerForm()), Namen.norm(a.consumer())));
            rules.forEach(System.out::println);
        }
    }

    /** The document list a form of this kind sits in ({@code /rules/<list>}), the class the verifier's pointer
     *  names. */
    private static String lijst(ProducentConsument.Vormsoort soort) {
        return switch (soort) {
            case REGEL -> "rules";
            case OBJECTCREATIE -> "objectCreations";
            case VERDELING -> "allocations";
            case CONSISTENTIE -> "consistencies";
            case UNICITEIT -> "uniquenesses";
            case FEITCREATIE -> "factCreations";
            case DEFAULTSGROEP -> "prioritizedDefaults";
        };
    }
}
