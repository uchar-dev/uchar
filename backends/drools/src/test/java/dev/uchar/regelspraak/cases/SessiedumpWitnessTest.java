package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static dev.uchar.regelspraak.cases.TmsDocumenten.attribuut;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The dump of a session ({@link Sessiedump}) replaces the identity of an object, {@code Type@hex}, by the name of that
 * object, and never touches the content of a text value: two sessions whose input differs only in a text that looks
 * like an identity, "Post@dead" against "Post@beef", get different dumps, each with its text as it is. Without that,
 * the harness of card drl-teksttak-op-truth-maintenance saw two different facts as one (lens on step 3a).
 *
 * <p><b>Red</b> on {@code d02ccea9}, before the fix: both dumps carried {@code String("Post@?")}, and the two were
 * equal.
 */
class SessiedumpWitnessTest {

    private static final String TEKST = "{\"node\":\"Text\"}";

    @Test void een_tekst_die_op_een_identiteit_lijkt_blijft_zoals_hij_is() throws Exception {
        String model = TmsDocumenten.model(List.of(TmsDocumenten.objecttype("Vondst", List.of(attribuut("merk", TEKST)),
                List.of())), List.of());
        Path ir = TmsDocumenten.document(model, List.of());
        CaseRunner runner = CaseRunner.voorMeting(ir, DroolsBackend.Branch.EXEC_MODEL);
        String dead = dump(runner, "Post@dead"), beef = dump(runner, "Post@beef");
        assertNotEquals(dead, beef, "twee teksten die alleen in de hex na de @ verschillen, kregen dezelfde dump:\n" + dead);
        assertTrue(dead.contains("String(\"Post@dead\")"), () -> "de dump citeert de tekst niet zoals hij is:\n" + dead);
        assertTrue(beef.contains("String(\"Post@beef\")"), () -> "de dump citeert de tekst niet zoals hij is:\n" + beef);
    }

    /** The dump of the one case of a Vondst V1 whose merk is {@code merk}. */
    private static String dump(CaseRunner runner, String merk) throws Exception {
        JsonNode c = TmsDocumenten.zaak(List.of("{\"label\":\"V1\",\"objectType\":\"Vondst\",\"attributes\":{\"merk\":"
                + "{\"text\":\"" + merk + "\"}},\"roles\":{},\"characteristics\":[]}"), List.of(TmsDocumenten.consistent()));
        return runner.metSessie(c, (bouw, sessie, labels) -> {
            Sessiedump d = Sessiedump.op(sessie, labels);
            StringBuilder uit = new StringBuilder();
            d.schrijf(uit, bouw, DroolsBackend.vuur(bouw, sessie), labels);
            return uit.toString();
        });
    }
}
