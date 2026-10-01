package dev.uchar.regelspraak.graaf;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import dev.uchar.regelspraak.graaf.ProducentConsument.DefaultLezing;
import dev.uchar.regelspraak.graaf.ProducentConsument.ElementSleutel;
import dev.uchar.regelspraak.graaf.ProducentConsument.Navigatie;
import dev.uchar.regelspraak.graaf.ProducentConsument.Vorm;
import dev.uchar.regelspraak.graaf.ProducentConsument.Vormsoort;
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
 * What each default of a defaults group reads ({@link ProducentConsument#lezingen}): its justification and its
 * consequence apart, with its place in the tree, so a consumer can name the default whose read falls outside a scope.
 * The defaults come in the order of the group and, per default, the default before those of its exception groups, and
 * all of them together read exactly what the group reads ({@link ProducentConsument#vormen}). Checked on every defaults
 * group of the corpus, on the wire route and, where the golden carries its source, on the source route, and on the
 * forms a group can take, on both routes: a member and a characteristic check in a justification, a member in a
 * consequence, also of an exception that is no base, one and two roles away, a nested exception group, a member of an
 * {@code Uitzonderingen ongeordend op} block and the group-default of {@code <basis> voor <subtype>}.
 */
class GraafDefaultLezingTest {

    private static final ObjectMapper JSON = new ObjectMapper();
    // the gate runs from the repository root
    private static final Path CORPUS = Path.of("corpus", "golden");

    /** Every default's reads together, in order, are the group's reads, as vormen gives them. */
    private static void vereniging(RegelSpraakModel model, List<PrioritizedDefaults> groepen, Navigatie navigatie,
                                   String waar) {
        List<Vorm> vormen = ProducentConsument.vormen(model, groepen, navigatie);
        List<Vorm> groepVormen = vormen.subList(vormen.size() - groepen.size(), vormen.size());
        for (int i = 0; i < groepen.size(); i++) {
            Vorm v = groepVormen.get(i);
            assertEquals(Vormsoort.DEFAULTSGROEP, v.soort(), waar);
            Set<ElementSleutel> samen = new LinkedHashSet<>();
            for (DefaultLezing d : ProducentConsument.lezingen(model, groepen.get(i), navigatie)) {
                samen.addAll(d.rechtvaardiging());
                samen.addAll(d.consequentie());
            }
            assertEquals(List.copyOf(v.leest()), List.copyOf(samen), waar + ", groep " + v.naam());
        }
    }

    @TestFactory Stream<DynamicTest> de_lezingen_samen_zijn_wat_de_groep_leest_op_het_corpus() throws Exception {
        List<Path> documenten;
        try (Stream<Path> s = Files.walk(CORPUS)) {
            documenten = s.filter(p -> p.getFileName().toString().equals("ir.json")).sorted().toList();
        }
        List<Path> metGroepen = new ArrayList<>();
        for (Path ir : documenten)
            if (JSON.readTree(Files.readString(ir)).path("rules").path("prioritizedDefaults").size() > 0) metGroepen.add(ir);
        // measured 2026-10-01: 32 goldens carry a defaults group; none is refused by the loader
        assertTrue(metGroepen.size() >= 32, "maar " + metGroepen.size() + " goldens met een defaultsgroep gevonden");
        return metGroepen.stream().map(ir -> DynamicTest.dynamicTest(CORPUS.relativize(ir.getParent()).toString(), () -> {
            JsonNode doc = JSON.readTree(Files.readString(ir));
            IrLoader.Loaded w = IrLoader.loadAll(doc);
            vereniging(w.model(), w.prioritizedDefaults(), ProducentConsument.wireNavigatie(w), ir + " (wire)");
            Path rgs = ir.resolveSibling("bron.rgs");
            if (Files.exists(rgs)) {
                RegelSpraakModel m = Resolver.resolve(Files.readString(rgs) + "\n", rgs.toString());
                assertTrue(m.isOk(), () -> rgs + " resolvet niet: " + m.diagnostics);
                vereniging(m, m.prioritizedDefaults, ProducentConsument.bronNavigatie(m), rgs + " (bron)");
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
    private static final String BOOM = MODEL + """
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
    private static void lezingen(String bron, String naam, List<String> verwacht) {
        TweeRoutes.Tweeling t = TweeRoutes.lees(bron);
        RegelSpraakModel m = t.bronModel();
        IrLoader.Loaded w = t.wire();
        assertEquals(verwacht, regels(m, groep(m.prioritizedDefaults, naam), ProducentConsument.bronNavigatie(m)), "bron");
        assertEquals(verwacht, regels(w.model(), groep(w.prioritizedDefaults(), naam), ProducentConsument.wireNavigatie(w)),
                "wire");
        vereniging(m, m.prioritizedDefaults, ProducentConsument.bronNavigatie(m), "bron");
        vereniging(w.model(), w.prioritizedDefaults(), ProducentConsument.wireNavigatie(w), "wire");
    }

    private static PrioritizedDefaults groep(List<PrioritizedDefaults> groepen, String naam) {
        return groepen.stream().filter(p -> naam.equals(p.naam())).findFirst()
                .orElseThrow(() -> new AssertionError("geen groep '" + naam + "' in "
                        + groepen.stream().map(PrioritizedDefaults::naam).toList()));
    }

    private static List<String> regels(RegelSpraakModel model, PrioritizedDefaults groep, Navigatie navigatie) {
        return ProducentConsument.lezingen(model, groep, navigatie).stream()
                .map(d -> d.pad() + " R" + namen(d.rechtvaardiging()) + " C" + namen(d.consequentie())).toList();
    }

    private static String namen(Set<ElementSleutel> s) {
        return s.stream().map(e -> e.objecttype() + "." + e.naam() + (e.kenmerk() ? " (k)" : ""))
                .collect(Collectors.joining(", ", "[", "]"));
    }

    @Test void rechtvaardiging_en_consequentie_een_en_twee_rollen_ver_en_genest() {
        // The later-written exception ranks higher. Each exception carries the conditions of its carriers (grammar/
        // DIVERGENCES.md §4, form 2), so its justification reads theirs too; a navigated characteristic check reads the
        // characteristic of the reached type, a navigated read the member of the type its last hop reaches.
        lezingen(BOOM, "meldtermijn", List.of(
                "[0] R[jutter.vinddag, jutter.erkend (k)] C[haven.sluitdag]",
                "[1] R[jutter.vinddag, jutter.gemelde dag] C[jutter.gemelde dag]",
                "[1, 0, 0] R[jutter.vinddag, jutter.gemelde dag, kustwacht.open (k)] C[kustwacht.postdag]",
                "[2] R[jutter.vinddag] C[jutter.vinddag]"));
    }

    @Test void de_groepdefault_van_een_basis_voor_een_subtype() {
        // The deviation for the Strandjutter lowers to one more group on the subtype, whose one default is a
        // group-default without justification or consequence, holding the deviation as its exception group.
        lezingen(BOOM, "meldtermijn voor Strandjutter", List.of(
                "[0] R[] C[]",
                "[0, 0, 0] R[strandjutter.vergunningseinde] C[strandjutter.vergunningseinde]"));
    }

    @Test void de_leden_van_uitzonderingen_ongeordend_op() {
        // A group-default above the base holds each member as an exception group of its own; each member carries
        // the base's condition.
        lezingen(ONGEORDEND, "meldtermijn", List.of(
                "[0] R[] C[]",
                "[0, 0, 0] R[jutter.vinddag, kustwacht.open (k)] C[kustwacht.postdag]",
                "[0, 1, 0] R[jutter.vinddag, jutter.erkend (k)] C[jutter.gemelde dag]",
                "[1] R[jutter.vinddag] C[jutter.vinddag]"));
    }

    @Test void het_pad_wijst_de_default_aan() {
        // pad [i] is the i-th default of the group, [i, g, j] the j-th default of the g-th exception group of
        // default i; each DefaultLezing carries the very default its path names.
        TweeRoutes.Tweeling t = TweeRoutes.lees(BOOM);
        RegelSpraakModel m = t.bronModel();
        PrioritizedDefaults g = groep(m.prioritizedDefaults, "meldtermijn");
        for (DefaultLezing d : ProducentConsument.lezingen(m, g, ProducentConsument.bronNavigatie(m))) {
            List<Integer> pad = d.pad();
            assertFalse(pad.isEmpty());
            var dflt = g.defaults().get(pad.get(0));
            for (int i = 1; i < pad.size(); i += 2) dflt = dflt.excepties().leden().get(pad.get(i)).defaults().get(pad.get(i + 1));
            assertTrue(dflt == d.dflt(), () -> "het pad " + pad + " wijst een andere default aan");
        }
    }
}
