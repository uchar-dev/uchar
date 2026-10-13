package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A uniqueness over the union of three sources (RUNTIME-SEMANTICS ch. 5.9, rule 1), fired through both branches of the
 * analysis of card drl-teksttak-op-truth-maintenance (lens on step 3b): "De reekscode met het volgnummer van
 * Strandvondst verenigd met Wrakvondst verenigd met Drijfvondst moeten uniek zijn." The analysis carries two or more
 * sources, but until this witness no session fired the keys of a third one: the plan of the forms of
 * {@code DrlWeergaveWitnessTest} has sources 0 and 1 and is only compiled, and the witnesses of a uniqueness that fire
 * have two sources at most. Each case plays on both branches, and its dump is the same on both ({@link Takdumps}).
 *
 * <p>The document under {@code uniciteit-drie-bronnen} is own work on Jutterswet material: a key of two components,
 * whose order counts, over three object types. In the first case S1 (Strandvondst) and D1 (Drijfvondst) carry the key
 * (7, 1) and collide, while W1 (Wrakvondst) carries (1, 7); in the second D1 carries (1, 7) and nothing collides. The
 * document lies outside the corpus, so no gate runs it through the verifier, the reference interpreter or the ASP
 * backend; measured at fix round 3b, on {@code 55a24e61} with the dump of that round: the verifier PASS, ref and clingo
 * PASS on both cases, and on the first case with the expectation "consistent" both name exactly S1 and D1. The raw
 * output is kept with the measurements of the card, in {@code stap4/fix3b/unie-ruw.txt}.
 *
 * <p><b>Red by mutation</b>, measured at fix round 3b on {@code 6313a221} in a copy whose rendering swaps the first two
 * components of a key of source 2 and of no other source, the mutation the lens on step 3b found no red witness for:
 * the exec-model branch played both cases, and {@code DRL_TMS} answered "case: verwacht inconsistent (eenmalige
 * reekscode @ S1), niet gevonden onder eenmalige reekscode @ D1, eenmalige reekscode @ W1" on the first and "case:
 * verwacht consistent, werkelijk inconsistent (eenmalige reekscode @ D1, eenmalige reekscode @ S1)" on the second.
 */
class DrieBronnenUniciteitWitnessTest {

    private static final ObjectMapper JSON = new ObjectMapper();
    // cwd under surefire is the module dir (backends/drools)
    private static final Path DOCUMENT = Path.of("src", "test", "resources", "uniciteit-drie-bronnen");

    @Test void de_unie_over_drie_bronnen_speelt_op_beide_takken_met_gelijke_dumps() throws Exception {
        Path ir = DOCUMENT.resolve("ir.json");
        List<Path> zaken;
        try (Stream<Path> s = Files.list(DOCUMENT)) {
            zaken = s.filter(p -> p.getFileName().toString().matches("case.*\\.json")).sorted().toList();
        }
        assertEquals(2, zaken.size(), "de cases van het document");
        List<String> oordelen = new ArrayList<>();
        for (Path zaak : zaken)
            for (DroolsBackend.Branch tak : List.of(DroolsBackend.Branch.EXEC_MODEL, DroolsBackend.Branch.DRL_TMS)) {
                String oordeel = CaseRunner.speelCase(ir, JSON.readTree(zaak.toFile()), tak);
                if (oordeel != null) oordelen.add(tak + " — " + zaak.getFileName() + ": " + oordeel);
            }
        assertEquals(List.of(), oordelen, "de cases die niet spelen");
        for (Path zaak : zaken) {
            String welke = zaak.getFileName().toString();
            String dump = Takdumps.gelijk(ir, JSON.readTree(zaak.toFile()), welke);
            assertTrue(dump.contains("feit Uniciteitssleutel(uniciteit=String(\"eenmalige reekscode\"), bron=Integer(2),"
                    + " element=\"D1\""), () -> "de sleutel van de derde bron staat niet in de dump van " + welke + ":\n"
                    + dump);
        }
    }
}
