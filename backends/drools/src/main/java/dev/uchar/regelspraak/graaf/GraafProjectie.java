package dev.uchar.regelspraak.graaf;

import com.fasterxml.jackson.databind.ObjectMapper;

import dev.uchar.regelspraak.loader.IrLoader;
import dev.uchar.regelspraak.transpiler.kern.Namen;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.TreeSet;

/**
 * The command line the rule-graph gate drives (scripts/check-graaf.sh): for every {@code ir.json} named, the
 * {@link RegelGraaf} of the model the LOADER rebuilds from the wire, printed as its dependencies. One block per
 * document:
 *
 * <pre>
 *   document  &lt;path&gt;
 *   rand      &lt;producent&gt;  attr|char  &lt;lid&gt;  &lt;consument&gt;     one per dependency, sorted, no repeats
 *   weigering &lt;reason&gt;                                        instead, when the loader refuses a form
 * </pre>
 *
 * Fields are tab-separated and every name is normalised as the verifier normalises it ({@link Namen#norm}; an
 * element's name already is, see {@link ProducentConsument.ElementSleutel}), so the lines compare with the
 * verifier's {@code --dependency-edges} report projected onto rules, attributes and characteristics. The type of
 * an element is left out on purpose: this graph keys a member on its DECLARING type and the verifier's graph on
 * the type a read spells, so the two agree on which rules depend on which member and not on the spelling of the
 * type in between.
 */
public final class GraafProjectie {
    private GraafProjectie() {}

    public static void main(String[] args) throws Exception {
        ObjectMapper json = new ObjectMapper();
        for (String pad : args) {
            System.out.println("document\t" + pad);
            IrLoader.Loaded geladen;
            try {
                geladen = IrLoader.loadAll(json.readTree(Files.readString(Path.of(pad))));
            } catch (IrLoader.Refusal e) {
                System.out.println("weigering\t" + e.getMessage().replace('\n', ' '));
                continue;
            }
            RegelGraaf g = RegelGraaf.bouw(geladen.model(), ProducentConsument.wireNavigatie(geladen));
            TreeSet<String> regels = new TreeSet<>();
            for (RegelGraaf.Afhankelijkheid a : g.afhankelijkheden())
                regels.add(String.join("\t", "rand", Namen.norm(a.producent()),
                        a.element().kenmerk() ? "char" : "attr", a.element().naam(),
                        Namen.norm(a.consument())));
            regels.forEach(System.out::println);
        }
    }
}
