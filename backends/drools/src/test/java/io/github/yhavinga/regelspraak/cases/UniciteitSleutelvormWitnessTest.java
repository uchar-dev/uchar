package io.github.yhavinga.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The ch. 5.9 key-SHAPE guard of both uniqueness forms, through {@link CaseRunner#speelCase} over corpus
 * {@code ir.json} files with cases authored HERE (no corpus expectation is read or touched): a uniqueness key
 * list carries AT MOST ONE temporal component, the arity ref ({@code Eval.ever_tuples}) and clingo
 * ({@code uni_key_guards}) have always held and the Drools back-end used to lower straight past — an
 * {@code rsOoitBotst} per component, running on to a model where the other two executors reject the shape.
 *
 * <p>Three claims, none of which the gates see on their own. Gate 8's red leg observes only the two-component
 * FAULT (the shape it pins), so the VALID one-component shape — the whole point of the arity, and the form
 * every anchor register carries — has no red-side witness at all: a guard that fired on ONE temporal
 * component would still leave gate 8 green while silently voiding the goldens' collision detection. And the
 * MOMENT of the fault is invisible to every gate: fire, not build, because that is where ref raises it (per
 * instance, during evaluation) and because a build-time throw would surface as a backend crash — gate 8's
 * ERROR — instead of a fault the red fence can pin. The instance-less arm pins exactly that.
 */
class UniciteitSleutelvormWitnessTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    // cwd under surefire is the module dir (backends/drools); the corpus is two levels up.
    private static final Path GOLDEN = Path.of("..", "..", "corpus", "golden");
    private static final Path ROOD = Path.of("..", "..", "corpus", "rood");

    /** The sentence ref and clingo both raise; the red fence pins this fragment for all three executors. */
    private static final String FRAGMENT = "at most one temporal key component";

    // ---------------- 1. two temporal components -> a fault AT FIRE ----------------

    @Test void twee_temporele_sleutelcomponenten_falen_bij_het_vuren() throws Exception {
        // vondstnummer AND kavelnummer are both JAAR-granular: the single form's key list is two ever-values
        // wide, which would need the same-moment tuple join. One instance suffices — the shape is wrong
        // regardless of whether anything collides (this very case ran clean to a model before the guard).
        Throwable t = assertThrows(RuntimeException.class,
                () -> speel(ROOD, "uni-two-temporal-keys", geval("Strandvondst", "kavelnummer")));
        assertTrue(oorzaakKeten(t).contains(FRAGMENT),
                "the single form must fault with the shared ch. 5.9 wording, got: " + oorzaakKeten(t));
    }

    @Test void twee_temporele_sleutelcomponenten_in_een_unie_falen_bij_het_vuren() throws Exception {
        // the union arm of the same guard: vondstnummer + volgnummer over Strandvondst ∪ Wrakvondst. Only the
        // Strandvondst source has an instance, so this also pins that the guard rides each SOURCE's own key
        // list — ref runs ever_tuples per source, not once for the construct.
        Throwable t = assertThrows(RuntimeException.class,
                () -> speel(ROOD, "uu-two-temporal-keys", geval("Strandvondst", "volgnummer")));
        assertTrue(oorzaakKeten(t).contains(FRAGMENT),
                "the union form must fault with the shared ch. 5.9 wording, got: " + oorzaakKeten(t));
    }

    // ---------------- 2. the fault is at FIRE, so it needs an instance ----------------

    @Test void zonder_instanties_bereikt_de_vormfout_niets() throws Exception {
        // the placement witness: ref raises the shape error per instance during evaluation, so a subject type
        // with no instances never reaches it. A build-time guard would fault here too — and would reach gate 8
        // as an ERROR (a backend crash), never as the pinnable FAULT the red fence requires.
        String leeg = """
                { "format": "uchar-case-v0", "asOfDate": null,
                  "input": { "instances": [], "parameters": {} },
                  "expected": [ { "kind": "consistent" } ] }
                """;
        assertNull(speel(ROOD, "uni-two-temporal-keys", leeg), "no instance, no fire, no fault");
        assertNull(speel(ROOD, "uu-two-temporal-keys", leeg), "no instance, no fire, no fault");
    }

    // ---------------- 3. ONE temporal component still lowers the real collision rule ----------------

    @Test void een_temporeel_sleutelcomponent_blijft_botsingen_vinden() throws Exception {
        // the valid shape, and the sharper claim: not merely "does not fault" but "still DETECTS" — two
        // Strandvondsten whose vondstnummer timelines never overlap in time yet both EVER carry 7, the ch. 5.9
        // ever-reading. A guard that over-fired on one component would lose this inconsistency silently.
        String c = """
                { "format": "uchar-case-v0", "asOfDate": null,
                  "input": { "instances": [
                      { "label": "S1", "objectType": "Strandvondst",
                        "attributes": { "vondstnummer": { "timeline": [
                            { "from": "2024-01-01", "to": "2025-01-01", "value": { "number": "7" } } ] } },
                        "roles": {}, "characteristics": [] },
                      { "label": "S2", "objectType": "Strandvondst",
                        "attributes": { "vondstnummer": { "timeline": [
                            { "from": "2026-01-01", "to": "2027-01-01", "value": { "number": "7" } } ] } },
                        "roles": {}, "characteristics": [] } ],
                    "parameters": {} },
                  "expected": [
                      { "kind": "inconsistent", "rule": "eenmalig nummer", "label": "S1" },
                      { "kind": "inconsistent", "rule": "eenmalig nummer", "label": "S2" } ] }
                """;
        assertNull(speel(GOLDEN, "anchor/jutterswet-strandregister", c),
                "one temporal key component is the carried shape — the ever-collision must still be found");
    }

    @Test void een_temporeel_component_naast_een_scalair_blijft_botsingen_vinden() throws Exception {
        // the MIXED tuple across the union: reekscode (scalar) + volgnummer (temporal) is exactly one temporal
        // component, so the arity holds and the cross-type ever-collision must still be found. An arity read as
        // "no temporal component beside another key" would wrongly fault right here.
        String c = """
                { "format": "uchar-case-v0", "asOfDate": null,
                  "input": { "instances": [
                      { "label": "S1", "objectType": "Strandvondst",
                        "attributes": {
                            "reekscode": { "number": "5" },
                            "volgnummer": { "timeline": [
                                { "from": "2024-01-01", "to": "2025-01-01", "value": { "number": "9" } } ] } },
                        "roles": {}, "characteristics": [] },
                      { "label": "W1", "objectType": "Wrakvondst",
                        "attributes": {
                            "reekscode": { "number": "5" },
                            "volgnummer": { "timeline": [
                                { "from": "2026-01-01", "to": "2027-01-01", "value": { "number": "9" } } ] } },
                        "roles": {}, "characteristics": [] } ],
                    "parameters": {} },
                  "expected": [
                      { "kind": "inconsistent", "rule": "eenmalige reekscode", "label": "S1" },
                      { "kind": "inconsistent", "rule": "eenmalige reekscode", "label": "W1" } ] }
                """;
        assertNull(speel(GOLDEN, "anchor/jutterswet-nummerregister", c),
                "a scalar + one temporal key component is the carried shape — the union collision must still be found");
    }

    // ---------------- helpers ----------------

    /** One authored case over a corpus {@code ir.json}; the per-case failure line, null on a match. */
    private static String speel(Path zone, String casus, String caseJson) throws Exception {
        JsonNode c = JSON.readTree(caseJson);
        return CaseRunner.speelCase(zone.resolve(casus).resolve("ir.json"), c);
    }

    /** A one-instance case on {@code type} whose vondstnummer and {@code tweedeSleutel} both carry a timeline —
     *  the two-temporal-component input, disjoint in time so nothing collides on the values themselves. */
    private static String geval(String type, String tweedeSleutel) {
        return """
                { "format": "uchar-case-v0", "asOfDate": null,
                  "input": { "instances": [
                      { "label": "S1", "objectType": "%s",
                        "attributes": {
                            "vondstnummer": { "timeline": [
                                { "from": "2024-01-01", "to": "2025-01-01", "value": { "number": "1" } } ] },
                            "%s": { "timeline": [
                                { "from": "2024-01-01", "to": "2025-01-01", "value": { "number": "2" } } ] } },
                        "roles": {}, "characteristics": [] } ],
                    "parameters": {} },
                  "expected": [ { "kind": "consistent" } ] }
                """.formatted(type, tweedeSleutel);
    }

    /** The whole cause chain as one string — Drools wraps a consequence throw, so the message the guard raised
     *  sits below the wrapper. */
    private static String oorzaakKeten(Throwable t) {
        StringBuilder sb = new StringBuilder();
        for (Throwable c = t; c != null; c = c.getCause() == c ? null : c.getCause())
            sb.append(c).append(" | ");
        return sb.toString();
    }
}
