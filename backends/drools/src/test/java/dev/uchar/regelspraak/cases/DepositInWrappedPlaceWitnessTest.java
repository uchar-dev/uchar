package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * A division that deposits (RUNTIME-SEMANTICS ch. 1.4 point 4, ch. 5.6) at a place the walker {@code deposits} of the
 * TMS analysis reaches only through a constructor value, a payload access or the bound of a time window. Where that
 * walker decides whether a form deposits, a division under such a node deposits as one anywhere else: in the
 * consequence of a default ({@code lowerOwnRule}), in a condition ({@code depositRule}) and for the producer that
 * "regelversie X is inconsistent" reads ({@code channelProducer}).
 *
 * <p>The document {@code deposit-in-a-wrapped-place}, authored on Jutterswet material with its {@code source.rgs} beside
 * it, holds three such places on a Vondst whose geschatte gewicht is 0,00: the deviation "gewogen toeslag", whose
 * consequence the frontend wraps in the constructor {@code waarde} of a target with absence markers and which decides
 * 5,00 with its deposit (ch. 7.1); the rule "vindloon", whose condition is the window "vanaf zijn vinddatum plus (zijn
 * wachttijd gedeeld door zijn geschatte gewicht …)"; and the rule "taxatie" on a target with absence markers, whose
 * channel the characteristic "verdacht" reads. The document {@code payload-base} is written by hand, since no source
 * spells a payload access over anything but an attribute: the condition of "premie" is a gevuld test of the payload of
 * a constructor value whose argument is the division (WF-127). Verifier PASS; ref PASS on all three cases (case 1:
 * three EF-005 deposits and verdacht; case 2 without a zero, consistent; the payload case one EF-005 deposit). The ASP
 * backend refuses both documents (a composite constructor payload, a payload access over a constructor value).
 *
 * <p><b>Red before the fix</b> (measured 2026-11-11 on {@code 10f247fe1}, both branches alike): the document is refused
 * as {@code EXCLUDE DROOLS-130} ("taxatie" is no producer of the channel); without the rule "verdacht", case 1 ends in
 * {@code FAIL … expected inconsistent (Vondst.toeslag @ V1) code EF-005, not found among taxatie @ V1 [EF-005];
 * expected inconsistent (vindloon @ V1) code EF-005, not found among taxatie @ V1 [EF-005]}; the payload case ends in
 * {@code FAIL … expected inconsistent (premie @ V1) code EF-005, actual consistent}.
 */
class DepositInWrappedPlaceWitnessTest {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Path DOCUMENT = Path.of("src", "test", "resources", "deposit-in-a-wrapped-place");

    @Test void a_zero_divisor_under_a_constructor_and_in_a_window_bound_deposits_on_both_branches() throws Exception {
        play(DOCUMENT, "case1-gewicht-nul.json");
    }

    @Test void the_document_without_a_zero_divisor_plays_consistent() throws Exception {
        play(DOCUMENT, "case2-geen-nul.json");
    }

    @Test void a_zero_divisor_in_the_base_of_a_payload_access_deposits_on_both_branches() throws Exception {
        play(DOCUMENT.resolve("payload-base"), "case1-gewicht-nul.json");
    }

    private static void play(Path document, String caseFile) throws Exception {
        JsonNode c = JSON.readTree(Files.readString(document.resolve(caseFile)));
        for (DroolsBackend.Branch branch : DroolsBackend.Branch.values())
            assertNull(CaseRunner.playCase(document.resolve("ir.json"), c, branch), () -> caseFile + " op " + branch);
    }
}
