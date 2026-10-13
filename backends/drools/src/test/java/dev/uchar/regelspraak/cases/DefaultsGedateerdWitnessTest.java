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
 * fired a dated stack of a characteristic before. The document under {@code uitsluiting-gedateerd} is
 * {@code anchor/jutterswet-uitsluiting} with its defaults group valid from 2026-01-01, which this witness checks, and six
 * cases: the four of the golden on 2026-06-01; one on 2025-06-01, before the validity, where the stack decides nothing,
 * so that uitgesloten is absent and penninggerechtigd present; and one without an evaluation date, where every rule of
 * the stack waits on the date and the rule of the missing date faults every Jutter. Each case gets the same verdict on
 * both branches and the same dump ({@link Takdumps}): the first five play, the sixth is the evaluation fault of the
 * missing date.
 *
 * <p>The document lies outside the corpus, so no gate runs it through the verifier, the reference interpreter or the
 * ASP backend; measured at step 4 on {@code 1073bd9a}: the verifier PASS; ref and clingo PASS on the first five cases;
 * on the sixth ref "EVALUATION FAULT: a validity-bounded form needs an evaluation date; this session is time-free" and
 * clingo "TRANSLATION FAULT" with the same sentence, the fault both branches name. The raw output is kept with the
 * measurements of the card, in {@code stap4/gedateerd-ruw.txt}.
 *
 * <p><b>Red</b> on {@code e2bd685e}, before the step: the comparison failed on its first case, "het oordeel,
 * case1-geweigerd-2026.json ==&gt; expected: &lt;gespeeld&gt; but was: &lt;Excluded: drl-tms: de DRL-weergave draagt
 * Afloop nog niet&gt;"; the check of the document held.
 */
class DefaultsGedateerdWitnessTest {

    private static final ObjectMapper JSON = new ObjectMapper();
    // cwd under surefire is the module dir (backends/drools); the corpus is two levels up
    private static final Path DOCUMENT = Path.of("src", "test", "resources", "uitsluiting-gedateerd");
    private static final Path GOLDEN = Path.of("..", "..", "corpus", "golden", "anchor", "jutterswet-uitsluiting");

    /** The sentence of the fault of a case without an evaluation date, as the replay names it. */
    private static final String ZONDER_DATUM = "Rekenfout: fout in de regel \"defaults K:Jutter.uitgesloten\": a"
            + " validity-bounded form needs an evaluation date; this session is time-free";

    @Test void het_document_is_de_golden_met_een_gedateerde_groep() throws Exception {
        JsonNode golden = JSON.readTree(GOLDEN.resolve("ir.json").toFile());
        ((ObjectNode) golden.get("rules").get("prioritizedDefaults").get(0).get("validity")).put("from", "2026-01-01");
        assertEquals(golden, JSON.readTree(DOCUMENT.resolve("ir.json").toFile()));
    }

    @Test void elke_case_heeft_op_beide_takken_hetzelfde_oordeel_en_dezelfde_dump() throws Exception {
        Path ir = DOCUMENT.resolve("ir.json");
        List<Path> zaken;
        try (Stream<Path> s = Files.list(DOCUMENT)) {
            zaken = s.filter(p -> p.getFileName().toString().matches("case.*\\.json")).sorted().toList();
        }
        assertEquals(6, zaken.size(), "de cases van het document");
        for (Path zaak : zaken) {
            JsonNode c = JSON.readTree(zaak.toFile());
            String welke = zaak.getFileName().toString();
            Takdumps.gelijk(ir, c, welke);
            assertEquals(welke.startsWith("case6") ? ZONDER_DATUM : "gespeeld",
                    TmsFoutWitnessTest.speel(ir, c, DroolsBackend.Branch.EXEC_MODEL), welke);
        }
    }
}
