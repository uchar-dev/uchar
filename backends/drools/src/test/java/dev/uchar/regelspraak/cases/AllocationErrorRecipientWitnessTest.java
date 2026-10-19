package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * WHERE the §9.5 allocation error lands (RUNTIME-SEMANTICS ch. 4.2/4.3/4.4): on each offending RECIPIENT, rule ×
 * subject (ch. 5.6), for an empty tie/proportional weight and for an empty or negative maximum — pinned on BOTH
 * transpiler branches. ref ({@code Alloc.allocate}'s {@code err ~id:r.id}) and clingo deposit it there; both Drools
 * branches laid it on the SOURCE, once per bron, until 2026-09-23.
 *
 * <p><b>Red before the fix</b> (measured 2026-09-23 through the backend CLI once the runner compared the label, card
 * drools-consistentie-label): {@code corpus/golden/jutterswet-rangmaximering} case2-nulgroep answered in both
 * branches {@code FAIL case2-nulgroep.json: verwacht inconsistent (verdeel het penningbudget van een penningkas @
 * W5), niet gevonden onder verdeel het penningbudget van een penningkas @ K1}; ref and clingo PASS it. Before the
 * label comparison the same golden PLAYED on the key alone.
 *
 * <p>The recipient set follows ref exactly: an empty source amount flags nothing (op volgorde distributes nothing;
 * the other methods fault in ref), and under naar rato a cap is only read for a weighted recipient of a split whose
 * weight sum is non-zero. Every row below was run through ref and clingo with the same verdict.
 */
class AllocationErrorRecipientWitnessTest {

    private static final ObjectMapper JSON = new ObjectMapper();
    // cwd under surefire is the module dir (backends/drools); the corpus is two levels up.
    private static final Path RANK = Path.of("..", "..", "corpus", "golden", "jutterswet-rangmaximering");
    private static final Path BERGING = Path.of("..", "..", "corpus", "golden", "jutterswet-berging");
    private static final String RANK_RULE = "verdeel het penningbudget van een penningkas";
    private static final String BERG_RULE = "verdeel de opbrengst van een berging";

    /** Op volgorde with a naar-rato tie: W5 carries no tie weight, so the error lands on W5 and not on the kas. */
    @Test void the_empty_weight_lands_on_the_recipient() throws Exception {
        BothBranches.green(RANK.resolve("ir.json"), rank(), "the empty-weight error on recipient W5");
        ObjectNode c = rank();
        expected(c, inconsistent(RANK_RULE, "K1"));
        BothBranches.red(RANK.resolve("ir.json"), c,
                "verwacht inconsistent (" + RANK_RULE + " @ K1), niet gevonden onder " + RANK_RULE + " @ W5",
                "no error on the source kas");
    }

    /** Naar rato with a cap: H has no weight and D a negative maximum — one fact on each, none on W or the berging. */
    @Test void every_violating_recipient_gets_its_own_error() throws Exception {
        ObjectNode c = twoViolators();
        expected(c, inconsistent(BERG_RULE, "H"), inconsistent(BERG_RULE, "D"));
        BothBranches.green(BERGING.resolve("ir.json"), c, "one error on H (weight), one on D (maximum)");
        for (String elsewhere : new String[] { "W", "B1" }) {
            ObjectNode d = twoViolators();
            expected(d, inconsistent(BERG_RULE, elsewhere));
            String out = BothBranches.red(BERGING.resolve("ir.json"), d,
                    "verwacht inconsistent (" + BERG_RULE + " @ " + elsewhere + "), niet gevonden onder", "no error on " + elsewhere);
            assertTrue(out.contains(BERG_RULE + " @ H") && out.contains(BERG_RULE + " @ D"), out);
        }
    }

    /** Naar rato, every weight 0: nothing is distributed, so no cap is read and the empty maximum on W flags nothing. */
    @Test void a_zero_weight_sum_reads_no_maximum() throws Exception {
        ObjectNode c = berging();
        for (JsonNode i : c.get("input").get("instances"))
            if (i.get("objectType").asText().equals("Berger"))
                ((ObjectNode) i.get("attributes")).set("geborgen gewicht", JSON.readTree("{\"number\": \"0\"}"));
        ((ObjectNode) instance(c, "W").get("attributes")).remove("bergingsmaximum");
        expected(c, JSON.readTree("{\"kind\": \"consistent\"}"));
        BothBranches.green(BERGING.resolve("ir.json"), c, "zero weight sum: no cap read, no error");
    }

    /** Op volgorde with an empty source amount: nothing is distributed and W5's empty weight flags nothing. */
    @Test void an_empty_pot_reports_nothing() throws Exception {
        ObjectNode c = rank();
        ((ObjectNode) instance(c, "K1").get("attributes")).remove("penningbudget");
        expected(c, JSON.readTree("{\"kind\": \"consistent\"}"));
        BothBranches.green(RANK.resolve("ir.json"), c, "empty pot: no distribution, no error");
    }

    /** The recipient set is what the allocation's join reaches from THIS source. A second berging B2 holds Berger Y
     *  without a weight; Berger X, also weightless, belongs to no berging. The error lands on Y alone: not on X, which
     *  no source reaches, and not on B1's bergers. Every other row has one source with every recipient linked, so a
     *  lowering that dropped the join passed them all. */
    @Test void only_what_the_join_reaches_gets_an_error() throws Exception {
        BothBranches.green(BERGING.resolve("ir.json"), twoPots(inconsistent(BERG_RULE, "Y")), "the error on Y alone");
        String out = BothBranches.red(BERGING.resolve("ir.json"), twoPots(inconsistent(BERG_RULE, "X")),
                "verwacht inconsistent (" + BERG_RULE + " @ X), niet gevonden onder " + BERG_RULE + " @ Y",
                "no error on the unlinked X");
        assertTrue(!out.contains(" @ W") && !out.contains(" @ H") && !out.contains(" @ D"), out);
    }

    // ---------------- fixtures ----------------

    private static ObjectNode twoPots(JsonNode expectation) throws Exception {
        ObjectNode c = berging();
        ArrayNode inst = (ArrayNode) c.get("input").get("instances");
        inst.add(JSON.readTree("{\"label\": \"X\", \"objectType\": \"Berger\", \"roles\": {}, \"characteristics\": [],"
                + " \"attributes\": {\"bergingsmaximum\": {\"number\": \"300\"}}}"));
        inst.add(JSON.readTree("{\"label\": \"B2\", \"objectType\": \"Berging\", \"roles\": {\"bergers\": [\"Y\"]},"
                + " \"characteristics\": [], \"attributes\": {\"opbrengst\": {\"number\": \"100\"}}}"));
        inst.add(JSON.readTree("{\"label\": \"Y\", \"objectType\": \"Berger\", \"roles\": {}, \"characteristics\": [],"
                + " \"attributes\": {\"bergingsmaximum\": {\"number\": \"300\"}}}"));
        expected(c, expectation);
        return c;
    }

    private static ObjectNode rank() throws Exception {
        return (ObjectNode) JSON.readTree(Files.readString(RANK.resolve("case2-nulgroep.json")));
    }

    private static ObjectNode berging() throws Exception {
        return (ObjectNode) JSON.readTree(Files.readString(BERGING.resolve("case.json")));
    }

    private static ObjectNode twoViolators() throws Exception {
        ObjectNode c = berging();
        ((ObjectNode) instance(c, "H").get("attributes")).remove("geborgen gewicht");
        ((ObjectNode) instance(c, "D").get("attributes")).set("bergingsmaximum", JSON.readTree("{\"number\": \"-5\"}"));
        return c;
    }

    private static JsonNode instance(ObjectNode c, String label) {
        for (JsonNode i : c.get("input").get("instances")) if (i.get("label").asText().equals(label)) return i;
        throw new IllegalStateException("fixture has no instance " + label);
    }

    private static JsonNode inconsistent(String rule, String label) {
        ObjectNode e = JSON.createObjectNode();
        e.put("kind", "inconsistent").put("rule", rule).put("label", label);
        return e;
    }

    /** Replace the case's expectations: the rows here pin the channel, not the shares. */
    private static void expected(ObjectNode c, JsonNode... e) {
        ArrayNode a = JSON.createArrayNode();
        for (JsonNode x : e) a.add(x);
        c.set("expected", a);
    }
}
