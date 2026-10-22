package dev.uchar.regelspraak.cases;

import dev.uchar.regelspraak.refusal.Reason;
import dev.uchar.regelspraak.loader.IrLoader;
import dev.uchar.regelspraak.transpiler.tms.Cycles;

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
 * golden, every one of which the verifier passes (gate 3), has a cycle {@code Cycles} refuses, and every negative case
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
 * {@code wf045-subtype-creation-cycle}, a cycle through the population of a creation that mints a subtype of its own
 * subject.
 */
class TmsCycleVerifierWitnessTest {

    private static final Path CORPUS = Path.of("..", "..", "corpus");

    /** The goldens the loader carries, measured 2026-10-08; a floor, so that a loader that refuses more fails here. */
    private static final int EXPECTED_MIN_GOLDENS = 258;

    @Test void no_golden_has_a_cycle_that_the_branch_refuses() throws Exception {
        List<String> refused = new ArrayList<>();
        int loaded = 0;
        for (Path ir : documents(CORPUS.resolve("golden"), ".*")) {
            IrLoader.Loaded l;
            try {
                l = IrLoader.loadAll(TmsDocuments.JSON.readTree(Files.readString(ir)));
            } catch (IrLoader.Refusal e) {
                continue;
            }
            loaded++;
            Reason w = Cycles.refusal(l);
            if (w != null) refused.add(CORPUS.relativize(ir.getParent()) + ": " + w);
        }
        assertEquals(List.of(), refused);
        int n = loaded;
        assertTrue(n >= EXPECTED_MIN_GOLDENS, () -> "geladen goldens " + n + " onder de vloer " + EXPECTED_MIN_GOLDENS);
    }

    @Test void each_negative_document_of_wf045_wf046_and_wf051_is_refused() throws Exception {
        List<String> admitted = new ArrayList<>();
        int n = 0;
        for (Path ir : documents(CORPUS.resolve("negative"), "wf04[56]-.*|wf051-.*")) {
            IrLoader.Loaded l = IrLoader.loadAll(TmsDocuments.JSON.readTree(Files.readString(ir)));
            if (Cycles.refusal(l) == null) admitted.add(CORPUS.relativize(ir.getParent()).toString());
            n++;
        }
        assertEquals(List.of(), admitted);
        // 22 until 2026-10-22; the card cycle-across-instances added the eight witnesses of the instance-ordered cycle
        // (wf045-instance-cycle-*), each a WF-045 cycle that is no instance-ordered part, which this branch refuses
        // as the verifier does; the card instance-cycle-household-reads removed one of them (a fold over the
        // satellites of the predecessor is a shifted read since) and added two (the hub, the satellite of a satellite)
        assertEquals(31, n);
    }

    /** A negative whose pin is the verifier's arm WF-045.instance-read-outside (IR-SPEC ch. 5.3) is refused here with
     *  the read the arm names, its path and its clause; a negative of the instance-ordered cycle pinned on the cycle
     *  alone is refused for its cycle, without a read. */
    @Test void the_refusal_names_the_read_the_verifier_names() throws Exception {
        List<String> wrong = new ArrayList<>();
        int named = 0;
        for (Path ir : documents(CORPUS.resolve("negative"), "wf045-instance-cycle-.*")) {
            List<String> expect = Files.readAllLines(ir.resolveSibling("expect.txt"));
            Reason w = Cycles.refusal(IrLoader.loadAll(TmsDocuments.JSON.readTree(Files.readString(ir))));
            String doc = CORPUS.relativize(ir.getParent()).toString();
            if (expect.get(0).equals("WF-045.instance-read-outside")) {
                named++;
                String text = w == null ? "" : w.text(java.util.Locale.ENGLISH);
                String path = value(expect, "path"), clause = value(expect, "clause");
                if (w == null || w.code() != dev.uchar.regelspraak.refusal.DroolsCode.INSTANCE_READ_OUTSIDE
                        || !text.contains(" over [" + path + "] (clause " + clause + ")"))
                    wrong.add(doc + ": " + (w == null ? "admitted" : w.code().id() + " " + text));
            } else if (w == null || w.code() != dev.uchar.regelspraak.refusal.DroolsCode.CYCLE_OUTSIDE_RECURSIVE_CREATION) {
                wrong.add(doc + ": " + (w == null ? "admitted" : w.code().id() + " " + w.text()));
            }
        }
        assertEquals(List.of(), wrong);
        // measured 2026-10-22: six of the nine witnesses pin the arm
        int pinned = named;
        assertTrue(pinned >= 6, () -> "negatives pinned on the read outside: " + pinned);
    }

    private static String value(List<String> expect, String name) {
        return expect.stream().filter(l -> l.startsWith(name + "=")).map(l -> l.substring(name.length() + 1))
                .findFirst().orElseThrow(() -> new AssertionError("expect.txt names no " + name));
    }

    /** The instance-ordered cycle (IR-SPEC ch. 5.3): the golden whose einddag reads the restant of the vorige
     *  vergunning, in the form with a leeg-test and in the positive form over a satellite, has a cycle of the graph
     *  that is closed by shifted edges alone, which this branch admits as the verifier does. */
    @Test void the_instance_ordered_cycle_is_admitted() throws Exception {
        assertNull(Cycles.refusal(IrLoader.loadAll(TmsDocuments.JSON.readTree(Files.readString(
                CORPUS.resolve("golden").resolve("jutterswet-vergunningketen").resolve("ir.json"))))));
    }

    /** Reads in the household of the object (IR-SPEC ch. 5.3, card instance-cycle-household-reads): a fold over the
     *  own to-many termijnen, a filter and a quantifier that read the houder of a termijn, and a fold over the termijnen
     *  of the predecessor; the branch admits the cycle as the verifier does. */
    @Test void the_reads_in_the_household_are_admitted() throws Exception {
        assertNull(Cycles.refusal(IrLoader.loadAll(TmsDocuments.JSON.readTree(Files.readString(
                CORPUS.resolve("golden").resolve("jutterswet-vergunningtermijnen").resolve("ir.json"))))));
    }

    @Test void the_wrakboekingen_and_the_recursive_creations_have_no_cycle() throws Exception {
        for (String g : List.of("jutterswet-wrakboeking", "jutterswet-wrakboeking-rgs", "jutterswet-wrakboekwaarde",
                "anchor/jutterswet-wachtersketen", "jutterswet-wachtersketen-rgs", "jutterswet-opvolgerquorum",
                "jutterswet-opvolgerversies", "synth2-budgetgrens-onder", "anchor/jutterswet-erflijn"))
            assertNull(Cycles.refusal(IrLoader.loadAll(TmsDocuments.JSON.readTree(
                    Files.readString(CORPUS.resolve("golden").resolve(g).resolve("ir.json"))))), g);
    }

    /** Every {@code ir.json} under {@code squareRoot} whose directory name matches {@code name}, sorted. */
    private static List<Path> documents(Path squareRoot, String name) throws Exception {
        try (Stream<Path> s = Files.walk(squareRoot)) {
            return s.filter(p -> p.getFileName().toString().equals("ir.json"))
                    .filter(p -> p.getParent().getFileName().toString().matches(name)).sorted().toList();
        }
    }
}
