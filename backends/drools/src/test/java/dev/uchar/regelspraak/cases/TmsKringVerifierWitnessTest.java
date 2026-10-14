package dev.uchar.regelspraak.cases;

import dev.uchar.regelspraak.loader.IrLoader;
import dev.uchar.regelspraak.transpiler.tms.Kringen;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The cycle refusal of the exec-model branch agrees with the verifier on WF-045 and WF-046 over the corpus: no
 * golden, every one of which the verifier passes (gate 3), has a cycle {@code Kringen} refuses, and every negative case
 * of WF-045, WF-046 and WF-051 (the defaults fold of the same graph) is refused. Among the goldens are the recursive
 * creations WF-045 admits ({@code anchor/jutterswet-wachtersketen} and its kin) and the three wrakboeking documents,
 * whose creation mints a {@code Strandrecht} and reads {@code Wrakrecht.bergingsvergoeding}, which a rule computes from
 * {@code Wrakrecht.grondslag}: the creation writes {@code grondslag} on the type it mints and its ancestors only
 * (IR-SPEC ch. 5.1), so there is no cycle. A document the loader refuses is counted apart: it never reaches the branch.
 *
 * <p><b>Red before the change</b> (measured 2026-10-08 with the refusal of the branch after its second step, which keyed
 * members on their declaring type, drew no population or relation node and refused every cycle): of the 258 goldens
 * the loader carries, 8 were refused, the five recursive creations, "tms: kring in de afhankelijkheden over het lid
 * Wachter.rang via de objectcreatie "werf de opvolger" — elke kring is onwelgevormd (WF-045; …" (in
 * {@code anchor/jutterswet-wachtersketen}, {@code jutterswet-wachtersketen-rgs}, {@code jutterswet-opvolgerquorum},
 * {@code jutterswet-opvolgerversies} and {@code synth2-budgetgrens-onder}), and the three wrakboekingen, "over de leden
 * Strandrecht.grondslag en Wrakrecht.bergingsvergoeding via de regel "bereken de bergingsvergoeding van een wrakrecht"
 * en de objectcreatie "boek een strandrecht bij elk wrakrecht""; and of the 22 negative cases one passed,
 * {@code wf045-subtypecreatie-cyclus}, a cycle through the population of a creation that mints a subtype of its own
 * subject.
 */
class TmsKringVerifierWitnessTest {

    private static final Path CORPUS = Path.of("..", "..", "corpus");

    /** The goldens the loader carries, measured 2026-10-08; a floor, so that a loader that refuses more fails here. */
    private static final int EXPECTED_MIN_GOLDENS = 258;

    @Test void geen_golden_heeft_een_kring_die_de_tak_weigert() throws Exception {
        List<String> geweigerd = new ArrayList<>();
        int geladen = 0;
        for (Path ir : documenten(CORPUS.resolve("golden"), ".*")) {
            IrLoader.Loaded l;
            try {
                l = IrLoader.loadAll(TmsDocumenten.JSON.readTree(Files.readString(ir)));
            } catch (IrLoader.Refusal e) {
                continue;
            }
            geladen++;
            String w = Kringen.weigering(l);
            if (w != null) geweigerd.add(CORPUS.relativize(ir.getParent()) + ": " + w);
        }
        assertEquals(List.of(), geweigerd);
        int n = geladen;
        assertTrue(n >= EXPECTED_MIN_GOLDENS, () -> "geladen goldens " + n + " onder de vloer " + EXPECTED_MIN_GOLDENS);
    }

    @Test void elk_negatief_document_van_wf045_wf046_en_wf051_wordt_geweigerd() throws Exception {
        List<String> doorgelaten = new ArrayList<>();
        int n = 0;
        for (Path ir : documenten(CORPUS.resolve("negatief"), "wf04[56]-.*|wf051-.*")) {
            IrLoader.Loaded l = IrLoader.loadAll(TmsDocumenten.JSON.readTree(Files.readString(ir)));
            if (Kringen.weigering(l) == null) doorgelaten.add(CORPUS.relativize(ir.getParent()).toString());
            n++;
        }
        assertEquals(List.of(), doorgelaten);
        assertEquals(22, n);
    }

    @Test void de_wrakboekingen_en_de_recursieve_creaties_hebben_geen_kring() throws Exception {
        for (String g : List.of("jutterswet-wrakboeking", "jutterswet-wrakboeking-rgs", "jutterswet-wrakboekwaarde",
                "anchor/jutterswet-wachtersketen", "jutterswet-wachtersketen-rgs", "jutterswet-opvolgerquorum",
                "jutterswet-opvolgerversies", "synth2-budgetgrens-onder", "anchor/jutterswet-erflijn"))
            assertNull(Kringen.weigering(IrLoader.loadAll(TmsDocumenten.JSON.readTree(
                    Files.readString(CORPUS.resolve("golden").resolve(g).resolve("ir.json"))))), g);
    }

    /** Every {@code ir.json} under {@code wortel} whose directory name matches {@code naam}, sorted. */
    private static List<Path> documenten(Path wortel, String naam) throws Exception {
        try (Stream<Path> s = Files.walk(wortel)) {
            return s.filter(p -> p.getFileName().toString().equals("ir.json"))
                    .filter(p -> p.getParent().getFileName().toString().matches(naam)).sorted().toList();
        }
    }
}
