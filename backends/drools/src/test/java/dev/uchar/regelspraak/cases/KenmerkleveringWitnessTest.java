package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The two case deliveries of a characteristic, and why they are not interchangeable (maintainer decision
 * 2026-09-09) — witnessed through {@link CaseRunner#speelCase} over a corpus golden's {@code ir.json} with
 * cases authored HERE (no corpus expectation is read, moved or touched).
 *
 * <p>The flat {@code characteristics} list asserts a scalar truth; {@code characteristicTimelines} supplies
 * periods. Every temporal reader — the gedurende clip, the pro-rata fold, the day count — reads the timelines
 * and nothing else. A characteristic the document declares TIME-DEPENDENT, asserted in the static list,
 * therefore delivers a fact no reader consults. In the interpreter and the ASP backend that produced a silent
 * 0 where this very golden pins 30; here it produced an {@code EXCLUDE}, which is worse than it sounds — it
 * filed a broken case under "a form the v0 backend does not carry", a counted and reasoned refusal in the
 * gate-12 report, which is exactly where a defective case would never be looked for.
 *
 * <p>So the first test pins that the refusal is a HARD one and not an {@code Excluded}: the distinction IS the
 * finding, and an implementation that merely kept refusing would pass a test that only asserted "it throws".
 *
 * <p>The second test is the over-rejection guard on the same characteristic: delivered as a timeline it must
 * still play, and play to the hand-computed value. Without it, "refuse every temporal characteristic input"
 * would satisfy the first test.
 */
class KenmerkleveringWitnessTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    // cwd under surefire is the module dir (backends/drools); the corpus is two levels up.
    private static final Path IR = Path.of("..", "..", "corpus", "golden", "jutterswet-tegenwacht", "ir.json");

    /** A Jutter earning 10 a month across 2024, borgplichtig Jan–Jun, aangesteld Apr–Dec.
     *  <p>Hand-computed, both rules: {@code niet aangesteld} is Jan–Mar (3 months), and borgplichtig
     *  intersected with it is Jan–Mar as well, so tegenwachtsom = stilwachtsom = 3 x 10 = 30.
     *  @param borgplichtigStatisch deliver borgplichtig as a scalar truth instead of as periods */
    private static String jutter(boolean borgplichtigStatisch) {
        String borgTl = borgplichtigStatisch ? ""
                : "\"borgplichtig\": [ { \"from\": \"2024-01-01\", \"to\": \"2024-07-01\" } ], ";
        return "{ \"format\": \"uchar-case-v0\", \"asOfDate\": null, \"input\": { \"instances\": [ {"
                + " \"label\": \"J1\", \"objectType\": \"Jutter\","
                + " \"attributes\": { \"wachtvergoeding per maand\": { \"timeline\": [ {"
                + "     \"from\": \"2024-01-01\", \"to\": \"2025-01-01\", \"value\": { \"number\": \"10\" } } ] } },"
                + " \"roles\": {},"
                + " \"characteristics\": [ " + (borgplichtigStatisch ? "\"borgplichtig\"" : "") + " ],"
                + " \"characteristicTimelines\": { " + borgTl
                + "     \"aangesteld\": [ { \"from\": \"2024-04-01\", \"to\": \"2025-01-01\" } ] } } ],"
                + " \"parameters\": {} },"
                + " \"expected\": ["
                + "   { \"kind\": \"attributeValue\", \"label\": \"J1\", \"attribute\": \"tegenwachtsom\","
                + "     \"value\": { \"number\": \"30\" } },"
                + "   { \"kind\": \"attributeValue\", \"label\": \"J1\", \"attribute\": \"stilwachtsom\","
                + "     \"value\": { \"number\": \"30\" } } ] }";
    }

    @Test void een_temporeel_kenmerk_statisch_geleverd_is_een_harde_casefout() {
        RuntimeException e = assertThrows(RuntimeException.class,
                () -> CaseRunner.speelCase(IR, JSON.readTree(jutter(true))));
        assertFalse(e instanceof CaseRunner.Excluded,
                "a defective case must not be filed as a form the backend does not carry: " + e.getMessage());
        assertTrue(e instanceof IllegalArgumentException, "expected a hard case error, got " + e.getClass());
        assertTrue(e.getMessage().contains("borgplichtig"), "the refusal must name the characteristic: " + e.getMessage());
        assertTrue(e.getMessage().contains("characteristicTimelines"),
                "the refusal must name the delivery that IS right: " + e.getMessage());
    }

    @Test void hetzelfde_kenmerk_als_tijdlijn_speelt_naar_de_handberekende_waarde() throws Exception {
        assertNull(CaseRunner.speelCase(IR, JSON.readTree(jutter(false))),
                "the timeline delivery must still play, and play to 30 — the refusal above may not widen "
                        + "into a refusal of every temporal characteristic input");
    }
}
