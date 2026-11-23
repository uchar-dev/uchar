package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * A division that deposits (RUNTIME-SEMANTICS ch. 1.4 point 4, ch. 5.6) at a place the walker {@code deposits} of the
 * TMS analysis reaches only through the node that wraps it. Where that walker decides whether a form deposits, a
 * division under such a node deposits as one anywhere else, at each of its three call sites: in the consequence of a
 * default ({@code lowerOwnRule}), in a condition ({@code depositRule}) and for the producer that "regelversie X is
 * inconsistent" reads ({@code channelProducer}).
 *
 * <p>The document {@code deposit-in-a-wrapped-place}, written by hand on Jutterswet material, holds three such places on
 * a Vondst whose geschatte gewicht is 0,00: the deviation "gewogen toeslag" of the defaults group on the numeric
 * attribute toeslag, whose consequence "(zijn toeslaggrond gedeeld door zijn geschatte gewicht rekenkundig afgerond op 2
 * decimalen) plus 5,00" holds the division under a rounding and a sum and which decides 5,00 with its deposit (ch. 7.1);
 * the rule "vindloon", whose condition is the window "vanaf zijn vinddatum plus (zijn wachttijd gedeeld door zijn
 * geschatte gewicht …)"; and the rule "taxatie" on a target of a sum type ({@code Getaxeerd(bedrag)} |
 * {@code Ongetaxeerd}), whose value is the constructor {@code Getaxeerd} of the division and whose channel the
 * characteristic "verdacht" reads. Verifier PASS; ref PASS on both cases (case 1: three EF-005 deposits and verdacht;
 * case 2 without a zero, consistent).
 *
 * <p><b>Red before the fix</b> (measured 2026-11-11 on {@code 10f247fe1}, both branches alike, on the document of
 * then, whose defaults group wrote toeslag as an attribute with absence markers and so wrapped the consequence in a
 * constructor value as well): the document is refused as {@code EXCLUDE DROOLS-130} ("taxatie" is no producer of the
 * channel); without the rule "verdacht", case 1 ends in {@code FAIL … expected inconsistent (Vondst.toeslag @ V1) code
 * EF-005, not found among taxatie @ V1 [EF-005]; expected inconsistent (vindloon @ V1) code EF-005, not found among
 * taxatie @ V1 [EF-005]}. The walker reached a rounding and a sum before the fix as well, so the consequence of the
 * default as it stands now pins the call site in {@code lowerOwnRule}, not the fix.
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

    private static void play(Path document, String caseFile) throws Exception {
        JsonNode c = JSON.readTree(Files.readString(document.resolve(caseFile)));
        for (DroolsBackend.Branch branch : DroolsBackend.Branch.values())
            assertNull(CaseRunner.playCase(document.resolve("ir.json"), c, branch), () -> caseFile + " op " + branch);
    }
}
