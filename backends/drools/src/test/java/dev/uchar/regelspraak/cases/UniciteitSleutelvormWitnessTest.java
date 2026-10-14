package dev.uchar.regelspraak.cases;

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
 *
 * <p><b>On the exec-model branch with truth maintenance</b> (card tms-omschakeling, 2026-10-10) the claims about the
 * malformed shape are refusals: that branch refuses a key with two temporal components by name when it lowers the
 * uniqueness, in every world, since the same-moment tuple join has no corpus case (RUNTIME-SEMANTICS ch. 5.9); the red
 * fence still pins ref's and clingo's fault, and a refusal reaches gate 8 as an exclusion, not as an ERROR. Where ref
 * answers because the fault is not reached — no instance, or the evaluation date outside the window — the destructive
 * exec-model branch answered too; the exec-model branch with truth maintenance refuses there as well, a narrowing
 * accepted with the card. The valid one-component shapes (3.) play on every branch.
 */
class UniciteitSleutelvormWitnessTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    // cwd under surefire is the module dir (backends/drools); the corpus is two levels up.
    private static final Path GOLDEN = Path.of("..", "..", "corpus", "golden");
    private static final Path ROOD = Path.of("..", "..", "corpus", "rood");

    // ---------------- 1. two temporal components -> refused by name ----------------

    @Test void twee_temporele_sleutelcomponenten_weigeren_bij_de_bouw() throws Exception {
        // vondstnummer AND kavelnummer are both JAAR-granular: the single form's key list is two ever-values
        // wide, which would need the same-moment tuple join. ref faults on it per instance with the shared ch. 5.9
        // wording, "a uniqueness key list carries at most one temporal key component (the same-moment tuple join has
        // no corpus case; ch. 5.9)"; the exec-model branch refuses the uniqueness by name.
        geweigerd(ROOD.resolve("uni-two-temporal-keys").resolve("ir.json"), geval("Strandvondst", "kavelnummer"),
                "uniciteit: dubbel temporeel enkelvoudig", "kavelnummer");
    }

    @Test void twee_temporele_sleutelcomponenten_in_een_unie_weigeren_bij_de_bouw() throws Exception {
        // the union arm of the same guard: vondstnummer + volgnummer over Strandvondst ∪ Wrakvondst. NB both
        // sources are malformed here, so per-source vs per-construct arity is indistinguishable on this case;
        // WF-030 keeps the asymmetric-arity union out of the corpus.
        geweigerd(ROOD.resolve("uu-two-temporal-keys").resolve("ir.json"), geval("Strandvondst", "volgnummer"),
                "uniciteit-unie: dubbel temporeel", "volgnummer");
    }

    // ---------------- 2. ref faults only with an instance; the branch refuses without one too ----------------

    @Test void zonder_instanties_weigert_de_vorm_ook() throws Exception {
        // ref raises the shape error per instance during evaluation, so a subject type with no instances never
        // reaches it, and clingo's uni_key_guards fires at TRANSLATION time, instance-independent. The
        // exec-model branch refuses at build, as clingo does, where ref answers consistent: the narrowing of
        // card tms-omschakeling, recorded here.
        String leeg = """
                { "format": "uchar-case-v0", "asOfDate": null,
                  "input": { "instances": [], "parameters": {} },
                  "expected": [ { "kind": "consistent" } ] }
                """;
        geweigerd(ROOD.resolve("uni-two-temporal-keys").resolve("ir.json"), leeg, "uniciteit: dubbel temporeel enkelvoudig",
                "kavelnummer");
        geweigerd(ROOD.resolve("uu-two-temporal-keys").resolve("ir.json"), leeg, "uniciteit-unie: dubbel temporeel",
                "volgnummer");
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
        // the MIXED tuple WITHIN one key list (both union types declare identical granularities — the
        // cross-source mixed form is a separate, pre-existing crash, see the Result): reekscode (scalar) +
        // volgnummer (temporal) is exactly one temporal
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

    // ---------------- 4. the validity window rides the guard rule too ----------------

    @Test void buiten_het_geldigheidsvenster_weigert_de_vorm_ook() throws Exception {
        // the guard rule carries the same geldigGuard as the rule it replaces: ref applies the construct's
        // window at document level (applies env u.uni_geldigheid), so a malformed key OUTSIDE its window
        // faults in neither ref nor Drools — clingo faults on it for an UNRELATED reason (uniqueness validity
        // windows are not translated) — verified against ref on this exact model (lens 1, 2026-08-30, who
        // also proved by mutation that no gate covers this arm: dropping the guard leaves gate 8 green).
        // Authored on an in-memory ir copy; the corpus rood case itself stays unwindowed. The truth-maintenance
        // branch refuses the shape at build, outside the window too: the narrowing of card tms-omschakeling.
        com.fasterxml.jackson.databind.node.ObjectNode ir = (com.fasterxml.jackson.databind.node.ObjectNode)
                JSON.readTree(java.nio.file.Files.readString(
                        ROOD.resolve("uni-two-temporal-keys").resolve("ir.json")));
        ((com.fasterxml.jackson.databind.node.ObjectNode) IrPad.versie(ir, "uniquenesses", 0, 0).get("validity"))
                .put("from", "2030-01-01");
        Path tmp = java.nio.file.Files.createTempFile("venster-sleutelvorm", ".json");
        try {
            java.nio.file.Files.writeString(tmp, ir.toString());
            String c = geval("Strandvondst", "kavelnummer")
                    .replace("\"asOfDate\": null", "\"asOfDate\": \"2024-06-01\"");
            geweigerd(tmp, c, "uniciteit: dubbel temporeel enkelvoudig", "kavelnummer");
        } finally {
            java.nio.file.Files.deleteIfExists(tmp);
        }
    }

    @Test void buiten_het_geldigheidsvenster_weigert_de_unie_vorm_ook() throws Exception {
        // the union arm of the window witness. Same construction: window from 2030, evaluation date 2024 — ref
        // stays silent; the exec-model branch refuses the shape at build, as the row above.
        com.fasterxml.jackson.databind.node.ObjectNode ir = (com.fasterxml.jackson.databind.node.ObjectNode)
                JSON.readTree(java.nio.file.Files.readString(
                        ROOD.resolve("uu-two-temporal-keys").resolve("ir.json")));
        ((com.fasterxml.jackson.databind.node.ObjectNode) IrPad.versie(ir, "uniquenesses", 0, 0).get("validity"))
                .put("from", "2030-01-01");
        Path tmp = java.nio.file.Files.createTempFile("venster-unie-sleutelvorm", ".json");
        try {
            java.nio.file.Files.writeString(tmp, ir.toString());
            String c = geval("Strandvondst", "volgnummer")
                    .replace("\"asOfDate\": null", "\"asOfDate\": \"2024-06-01\"");
            geweigerd(tmp, c, "uniciteit-unie: dubbel temporeel", "volgnummer");
        } finally {
            java.nio.file.Files.deleteIfExists(tmp);
        }
    }

    // ---------------- helpers ----------------

    /** The exec-model branch refuses the uniqueness {@code vorm} of {@code ir} by name: its key {@code sleutel}
     *  is a second temporal component. */
    private static void geweigerd(Path ir, String caseJson, String vorm, String sleutel) throws Exception {
        JsonNode c = JSON.readTree(caseJson);
        CaseRunner.Excluded e = assertThrows(CaseRunner.Excluded.class,
                () -> CaseRunner.speelCase(ir, c, DroolsBackend.Branch.EXEC_MODEL));
        assertTrue(e.getMessage().contains(vorm + " — een uniciteitssleutel '" + sleutel + "' van Strandvondst op een"
                + " tijdlijn naast een tweede: een sleutel heeft hoogstens één tijdsafhankelijk deel"), e.getMessage());
    }

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
}
