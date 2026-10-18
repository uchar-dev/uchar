package dev.uchar.regelspraak.graph;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import dev.uchar.regelspraak.graph.ProducerConsumer.DefaultRead;
import dev.uchar.regelspraak.graph.ProducerConsumer.ElementKey;
import dev.uchar.regelspraak.graph.ProducerConsumer.Navigation;
import dev.uchar.regelspraak.graph.ProducerConsumer.Form;
import dev.uchar.regelspraak.graph.ProducerConsumer.FormKind;
import dev.uchar.regelspraak.ir.PrioritizedDefaults;
import dev.uchar.regelspraak.loader.IrLoader;
import dev.uchar.regelspraak.model.RegelSpraakModel;
import dev.uchar.regelspraak.resolver.Resolver;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * What each default of a defaults group reads ({@link ProducerConsumer#readings}): its justification and its
 * consequence apart, with its place in the tree, so a consumer can name the default whose read falls outside a scope.
 * The defaults come in the order of the group and, per default, the default before those of its exception groups, and
 * all of them together read exactly what the group reads ({@link ProducerConsumer#forms}). Checked on every defaults
 * group of the corpus, on the wire route and, where the golden carries its source, on the source route, and on the
 * forms a group can take, on both routes: a member and a characteristic check in a justification, a member in a
 * consequence, also of an exception that is no base, one and two roles away, a nested exception group, a member of an
 * {@code Uitzonderingen ongeordend op} block and the group-default of {@code <basis> voor <subtype>}.
 */
class GraphDefaultReadTest {

    private static final ObjectMapper JSON = new ObjectMapper();
    // the gate runs from the repository root
    private static final Path CORPUS = Path.of("corpus", "golden");

    /** Every default's reads together, in order, are the group's reads, as vormen gives them. */
    private static void union(RegelSpraakModel model, List<PrioritizedDefaults> groups, Navigation navigation,
                                   String where) {
        List<Form> forms = ProducerConsumer.forms(model, groups, navigation);
        List<Form> groupForms = forms.subList(forms.size() - groups.size(), forms.size());
        for (int i = 0; i < groups.size(); i++) {
            Form v = groupForms.get(i);
            assertEquals(FormKind.DEFAULTSGROEP, v.category(), where);
            Set<ElementKey> combined = new LinkedHashSet<>();
            for (DefaultRead d : ProducerConsumer.readings(model, groups.get(i), navigation)) {
                combined.addAll(d.justification());
                combined.addAll(d.consequence());
            }
            assertEquals(List.copyOf(v.reads()), List.copyOf(combined), where + ", groep " + v.naam());
        }
    }

    @TestFactory Stream<DynamicTest> the_reads_together_are_what_the_group_reads_on_the_corpus() throws Exception {
        List<Path> documents;
        try (Stream<Path> s = Files.walk(CORPUS)) {
            documents = s.filter(p -> p.getFileName().toString().equals("ir.json")).sorted().toList();
        }
        // A golden the loader refuses has no model to read; like gate 15 this compares the others, and their number has
        // a floor: measured 2026-10-01, 41 goldens carry a defaults group (nine of them anchors) and the loader refuses
        // one of them (jutterswet-kamerhuishouden-catala, for its function definitions).
        List<Path> comparable = new ArrayList<>();
        for (Path ir : documents) {
            JsonNode doc = JSON.readTree(Files.readString(ir));
            if (doc.path("rules").path("prioritizedDefaults").size() == 0) continue;
            try {
                IrLoader.loadAll(doc);
                comparable.add(ir);
            } catch (IrLoader.Refusal refused) {
                // not comparable: no model
            }
        }
        assertTrue(comparable.size() >= 40, "maar " + comparable.size() + " goldens met een defaultsgroep vergelijkbaar");
        return comparable.stream().map(ir -> DynamicTest.dynamicTest(CORPUS.relativize(ir.getParent()).toString(), () -> {
            IrLoader.Loaded w = IrLoader.loadAll(JSON.readTree(Files.readString(ir)));
            union(w.model(), w.prioritizedDefaults(), ProducerConsumer.wireNavigation(w), ir + " (wire)");
            Path rgs = ir.resolveSibling("bron.rgs");
            if (Files.exists(rgs)) {
                RegelSpraakModel m = Resolver.resolve(Files.readString(rgs) + "\n", rgs.toString());
                assertTrue(m.isOk(), () -> rgs + " resolvet niet: " + m.diagnostics);
                union(m, m.prioritizedDefaults, ProducerConsumer.sourceNavigation(m), rgs + " (bron)");
            }
        }));
    }

    private static final String MODEL = """
        Objecttype de Haven (mv: Havens)
            de sluitdag Datum in dagen;
        Objecttype de Kustwacht (mv: Kustwachten)
            de postdag Datum in dagen;
            is open kenmerk (bijvoeglijk);
        Objecttype de Jutter (mv: Jutters) (bezield)
            de vinddag Datum in dagen;
            de gemelde dag Datum in dagen;
            de meldtermijn Datum in dagen;
            is erkend kenmerk (bijvoeglijk);
        Objecttype de Strandjutter is een Jutter (mv: Strandjutters)
            het vergunningseinde Datum in dagen;
        Feittype kustwacht bewaakt jutter
            de kustwacht Kustwacht
            de jutter (mv: jutters) Jutter
        Eén kustwacht bewaakt meerdere jutters
        Feittype haven huisvest kustwacht
            de thuishaven Haven
            de wachtpost (mv: wachtposten) Kustwacht
        Eén thuishaven huisvest meerdere wachtposten
        """;

    /** A base, an exception with an exception of its own, a second exception, and a deviation for a subtype. */
    private static final String TREE = MODEL + """
        Regel meldtermijn
            geldig altijd
                De meldtermijn van een Jutter moet gesteld worden op zijn vinddag
                indien zijn vinddag gevuld is.
        Uitzondering op meldtermijn: gemelde dag gaat voor
            geldig altijd
                De meldtermijn van een Jutter moet gesteld worden op zijn gemelde dag
                indien zijn gemelde dag gevuld is.
        Uitzondering op gemelde dag gaat voor: postdag van een open kustwacht
            geldig altijd
                De meldtermijn van een Jutter moet gesteld worden op de postdag van de kustwacht van de Jutter
                indien de kustwacht van de Jutter open is.
        Uitzondering op meldtermijn: sluitdag van de thuishaven
            geldig altijd
                De meldtermijn van een Jutter moet gesteld worden op de sluitdag van de thuishaven van de kustwacht van de Jutter
                indien hij erkend is.
        Afwijking van meldtermijn: vergunningseinde
            geldig altijd
                De meldtermijn van een Strandjutter moet gesteld worden op zijn vergunningseinde
                indien zijn vergunningseinde gevuld is.
        """;

    /** An unordered block of two members under a base. */
    private static final String ONGEORDEND = MODEL + """
        Regel meldtermijn
            geldig altijd
                De meldtermijn van een Jutter moet gesteld worden op zijn vinddag
                indien zijn vinddag gevuld is.
        Uitzonderingen ongeordend op meldtermijn:
        Regel postdag van een open kustwacht
            geldig altijd
                De meldtermijn van een Jutter moet gesteld worden op de postdag van de kustwacht van de Jutter
                indien de kustwacht van de Jutter open is.
        Regel gemelde dag van een erkende jutter
            geldig altijd
                De meldtermijn van een Jutter moet gesteld worden op zijn gemelde dag
                indien hij erkend is.
        """;

    /** The reads of the group named {@code naam}, on both routes, one line per default: "pad R[…] C[…]". */
    private static void readings(String source, String naam, List<String> expected) {
        TwoRoutes.Twin t = TwoRoutes.read(source);
        RegelSpraakModel m = t.sourceModel();
        IrLoader.Loaded w = t.wire();
        assertEquals(expected, rules(m, group(m.prioritizedDefaults, naam), ProducerConsumer.sourceNavigation(m)), "bron");
        assertEquals(expected, rules(w.model(), group(w.prioritizedDefaults(), naam), ProducerConsumer.wireNavigation(w)),
                "wire");
        union(m, m.prioritizedDefaults, ProducerConsumer.sourceNavigation(m), "bron");
        union(w.model(), w.prioritizedDefaults(), ProducerConsumer.wireNavigation(w), "wire");
    }

    private static PrioritizedDefaults group(List<PrioritizedDefaults> groups, String naam) {
        return groups.stream().filter(p -> naam.equals(p.naam())).findFirst()
                .orElseThrow(() -> new AssertionError("geen groep '" + naam + "' in "
                        + groups.stream().map(PrioritizedDefaults::naam).toList()));
    }

    private static List<String> rules(RegelSpraakModel model, PrioritizedDefaults group, Navigation navigation) {
        return ProducerConsumer.readings(model, group, navigation).stream()
                .map(d -> d.path() + " R" + names(d.justification()) + " C" + names(d.consequence())).toList();
    }

    private static String names(Set<ElementKey> s) {
        return s.stream().map(e -> e.objectType() + "." + e.naam() + (e.characteristic() ? " (k)" : ""))
                .collect(Collectors.joining(", ", "[", "]"));
    }

    @Test void justification_and_consequence_one_and_two_roles_far_and_nested() {
        // The later-written exception ranks higher. Each exception carries the conditions of its carriers (grammar/
        // DIVERGENCES.md §4, form 2), so its justification reads theirs too; a navigated characteristic check reads the
        // characteristic of the reached type, a navigated read the member of the type its last hop reaches.
        readings(TREE, "meldtermijn", List.of(
                "[0] R[jutter.vinddag, jutter.erkend (k)] C[haven.sluitdag]",
                "[1] R[jutter.vinddag, jutter.gemelde dag] C[jutter.gemelde dag]",
                "[1, 0, 0] R[jutter.vinddag, jutter.gemelde dag, kustwacht.open (k)] C[kustwacht.postdag]",
                "[2] R[jutter.vinddag] C[jutter.vinddag]"));
    }

    @Test void the_group_default_of_a_base_for_a_subtype() {
        // The deviation for the Strandjutter lowers to one more group on the subtype, whose one default is a
        // group-default without justification or consequence, holding the deviation as its exception group.
        readings(TREE, "meldtermijn voor Strandjutter", List.of(
                "[0] R[] C[]",
                "[0, 0, 0] R[strandjutter.vergunningseinde] C[strandjutter.vergunningseinde]"));
    }

    @Test void the_members_of_exceptions_unordered_on() {
        // A group-default above the base holds each member as an exception group of its own; each member carries
        // the base's condition.
        readings(ONGEORDEND, "meldtermijn", List.of(
                "[0] R[] C[]",
                "[0, 0, 0] R[jutter.vinddag, kustwacht.open (k)] C[kustwacht.postdag]",
                "[0, 1, 0] R[jutter.vinddag, jutter.erkend (k)] C[jutter.gemelde dag]",
                "[1] R[jutter.vinddag] C[jutter.vinddag]"));
    }

    @Test void the_path_points_to_the_default() {
        // pad [i] is the i-th default of the group, [i, g, j] the j-th default of the g-th exception group of
        // default i; each DefaultRead carries the very default its path names.
        TwoRoutes.Twin t = TwoRoutes.read(TREE);
        RegelSpraakModel m = t.sourceModel();
        PrioritizedDefaults g = group(m.prioritizedDefaults, "meldtermijn");
        for (DefaultRead d : ProducerConsumer.readings(m, g, ProducerConsumer.sourceNavigation(m))) {
            List<Integer> path = d.path();
            assertFalse(path.isEmpty());
            var dflt = g.defaults().get(path.get(0));
            for (int i = 1; i < path.size(); i += 2) dflt = dflt.exceptions().members().get(path.get(i)).defaults().get(path.get(i + 1));
            assertTrue(dflt == d.dflt(), () -> "het pad " + path + " wijst een andere default aan");
        }
    }
}
