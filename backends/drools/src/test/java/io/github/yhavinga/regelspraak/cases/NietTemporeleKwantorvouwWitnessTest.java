package io.github.yhavinga.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The QUANTIFIER FOLD with a NON-temporal matrix (RUNTIME-SEMANTICS ch. 3.9a), replayed through BOTH
 * transpiler branches over the corpus golden that pins it.
 *
 * <p><b>Why this test exists beside gate 12.</b> Gate 12 replays every golden through the EXEC-MODEL
 * branch only, and gate 13 drives the DRL-TEXT branch over the goldens it can TWIST — which are the
 * unit-bearing ones, and this document declares no units. So the DRL-text lowering of this node has no
 * gate that would notice it breaking. This is that gate: one file, both branches, the corpus's own
 * hand-computed expectations on both sides.
 *
 * <p>A pass here is the square closing on this form: the same five cells that {@code ref} and the
 * clingo backend replay (gates 5 and 6) are replayed by the two Drools lowerings, each of which
 * reaches them by a different road — the exec branch collects the contributors and asks a leaf
 * predicate of each, the DRL branch denies the failure witness with a {@code not} pattern.
 *
 * <p>The corpus files are READ and never written; the mutation row below copies a case into a
 * {@link TempDir} first.
 */
class NietTemporeleKwantorvouwWitnessTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    /** cwd under surefire is the module dir (backends/drools); the corpus is two levels up. */
    private static final Path GOLDEN =
            Path.of("..", "..", "corpus", "golden", "jutterswet-schouwploeg");

    @TempDir Path tmp;

    private static List<Path> cases() throws IOException {
        try (Stream<Path> s = Files.list(GOLDEN)) {
            return s.filter(p -> p.getFileName().toString().startsWith("case")).sorted().toList();
        }
    }

    @Test void de_exec_model_tak_speelt_de_cellen() throws Exception {
        List<Path> cases = cases();
        assertFalse(cases.isEmpty(), "de golden hoort casebestanden te dragen");
        CaseRunner.Result r = CaseRunner.viaExecModel(GOLDEN.resolve("ir.json"), cases);
        assertEquals("PLAYED", r.status(),
                "de exec-model-tak hoort de niet-temporele kwantorvouw te loweren: " + r.reason());
        assertEquals(cases.size(), r.casesPlayed(), "elke cel hoort gespeeld te zijn, niet overgeslagen");
    }

    /**
     * De DRL-tekst-tak lowert dezelfde vouw als een ONTKENDE GETUIGE: {@code not <Element>( <join>,
     * <matrix of haar ontkenning> )}. Deze rij is de enige plek in de hele poortverzameling waar die
     * tak op deze knoop gemeten wordt — gate 12 speelt alleen de exec-tak, en gate 13 raakt dit
     * document niet, want het draagt geen eenheidsdragende annotatie om te verdraaien.
     */
    @Test void de_drl_tekst_tak_speelt_dezelfde_cellen() throws Exception {
        List<Path> cases = cases();
        CaseRunner.Result r = CaseRunner.viaDrlTekst(GOLDEN.resolve("ir.json"), cases);
        assertEquals("PLAYED", r.status(),
                "de DRL-tekst-tak hoort de niet-temporele kwantorvouw te loweren: " + r.reason());
        assertEquals(cases.size(), r.casesPlayed(), "elke cel hoort gespeeld te zijn, niet overgeslagen");
    }

    /**
     * PLAYED alleen is geen getuige. Een regel die stil niet lowert schrijft niets, en elke cel die een
     * kenmerk AFWEZIG verwacht haalt dan vanzelf — zo zou een tak die de hele vouw overslaat nog steeds
     * kunnen "spelen". Daarom deze mutant: de vacuümcel, waar alle vier de vouwen WAAR lezen, met één
     * verwachting omgeklapt naar afwezig. Beide takken moeten hem nu VERWERPEN. Faalt deze rij, dan zegt
     * PLAYED hierboven niets over de kwantor.
     */
    @Test void een_mutant_op_de_vacuumcel_wordt_door_beide_takken_verworpen() throws Exception {
        Path bron = GOLDEN.resolve("case4-lege-ploeg.json");
        ObjectNode doc = (ObjectNode) JSON.readTree(Files.readString(bron));
        boolean geraakt = false;
        for (JsonNode e : doc.withArray("expected"))
            if ("characteristicPresent".equals(e.path("kind").asText())
                    && "ervaren".equals(e.path("characteristic").asText())) {
                ((ObjectNode) e).put("kind", "characteristicAbsent");
                geraakt = true;
            }
        assertTrue(geraakt, "de vacuümcel hoort 'ervaren' AANWEZIG te verwachten — anders muteert deze rij niets");
        Path mutant = tmp.resolve("case4-mutant.json");
        Files.writeString(mutant, doc.toPrettyString());

        Path ir = GOLDEN.resolve("ir.json");
        assertEquals("FAIL", CaseRunner.viaExecModel(ir, List.of(mutant)).status(),
                "de exec-model-tak hoort de omgeklapte vacuümverwachting te verwerpen");
        assertEquals("FAIL", CaseRunner.viaDrlTekst(ir, List.of(mutant)).status(),
                "de DRL-tekst-tak hoort hem even goed te verwerpen");
    }
}
