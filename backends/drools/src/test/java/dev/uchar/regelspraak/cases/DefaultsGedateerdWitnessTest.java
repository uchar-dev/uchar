package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * A defaults stack with a dated validity, through both branches of the analysis of card
 * drl-teksttak-op-truth-maintenance (step 4; decision 19 of the card's measurements): no golden and no case of the suite
 * fired a dated stack of a characteristic before. The document, the corpus golden {@code jutterswet-uitsluiting-gedateerd},
 * is {@code anchor/jutterswet-uitsluiting} with its defaults group valid from 2026-01-01, which this witness checks, with
 * five cases: the four of the anchor on 2026-06-01; and one on 2025-06-01, before the validity, where the stack decides
 * nothing, so that uitgesloten is absent and penninggerechtigd present. The red case
 * {@code jutterswet-uitsluiting-zonder-rekendatum} has the same document and a case without an evaluation date, where
 * every rule of the stack waits on the date and the rule of the missing date faults every Jutter. Each case gets the
 * same verdict on both branches and the same dump ({@link Takdumps}): the five of the golden play, the red one is the
 * evaluation fault of the missing date.
 *
 * <p>Until step 6b of the card the six cases were the card's own document in the test resources, measured by hand at
 * step 4 (the verifier PASS; ref and clingo PASS on the first five; on the sixth ref "EVALUATION FAULT: a
 * validity-bounded form needs an evaluation date; this session is time-free" and clingo "TRANSLATION FAULT" with the same
 * sentence, the fault both branches name). Since then gates 5, 6 and 12 replay the golden, and gate 8 holds each
 * executor to its sentence on the red case.
 *
 * <p><b>Red</b> on {@code e2bd685e}, before the step: the comparison failed on its first case, "het oordeel,
 * case1-geweigerd-2026.json ==&gt; expected: &lt;gespeeld&gt; but was: &lt;Excluded: drl-tms: de DRL-weergave draagt
 * Afloop nog niet&gt;"; the check of the document held.
 */
class DefaultsGedateerdWitnessTest {

    private static final ObjectMapper JSON = new ObjectMapper();
    // cwd under surefire is the module dir (backends/drools); the corpus is two levels up
    private static final Path CORPUS = Path.of("..", "..", "corpus");
    private static final Path DOCUMENT = CORPUS.resolve("golden").resolve("jutterswet-uitsluiting-gedateerd");
    private static final Path ROOD = CORPUS.resolve("rood").resolve("jutterswet-uitsluiting-zonder-rekendatum");
    private static final Path GOLDEN = CORPUS.resolve("golden").resolve("anchor").resolve("jutterswet-uitsluiting");

    /** The sentence of the fault of a case without an evaluation date, as the replay names it. */
    private static final String ZONDER_DATUM = "Rekenfout: fout in de regel \"defaults K:Jutter.uitgesloten\": a"
            + " validity-bounded form needs an evaluation date; this session is time-free";

    @Test void het_document_is_de_golden_met_een_gedateerde_groep() throws Exception {
        JsonNode golden = JSON.readTree(GOLDEN.resolve("ir.json").toFile());
        ((ObjectNode) golden.get("rules").get("prioritizedDefaults").get(0).get("validity")).put("from", "2026-01-01");
        assertEquals(golden, JSON.readTree(DOCUMENT.resolve("ir.json").toFile()));
        // the red case is the same document, byte for byte, so that only its case is red
        assertEquals(Files.readString(DOCUMENT.resolve("ir.json")), Files.readString(ROOD.resolve("ir.json")),
                "het document van de rode case");
    }

    @Test void elke_case_heeft_op_beide_takken_hetzelfde_oordeel_en_dezelfde_dump() throws Exception {
        Path ir = DOCUMENT.resolve("ir.json");
        List<Path> zaken = zaken(DOCUMENT), rood = zaken(ROOD);
        assertEquals(5, zaken.size(), "de cases van de golden");
        assertEquals(1, rood.size(), "de cases van de rode case");
        for (Path zaak : zaken) {
            JsonNode c = JSON.readTree(zaak.toFile());
            String welke = zaak.getFileName().toString();
            Takdumps.gelijk(ir, c, welke);
            assertEquals("gespeeld", TmsFoutWitnessTest.speel(ir, c, DroolsBackend.Branch.EXEC_MODEL), welke);
        }
        Path roodIr = ROOD.resolve("ir.json");
        JsonNode zonderDatum = JSON.readTree(rood.get(0).toFile());
        Takdumps.gelijk(roodIr, zonderDatum, "de rode case");
        assertEquals(ZONDER_DATUM, TmsFoutWitnessTest.speel(roodIr, zonderDatum, DroolsBackend.Branch.EXEC_MODEL),
                "de rode case");
    }

    private static List<Path> zaken(Path map) throws Exception {
        try (Stream<Path> s = Files.list(map)) {
            return s.filter(p -> p.getFileName().toString().matches("case.*\\.json")).sorted().toList();
        }
    }
}
