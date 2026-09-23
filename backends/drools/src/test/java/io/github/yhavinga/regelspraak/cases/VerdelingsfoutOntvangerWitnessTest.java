package io.github.yhavinga.regelspraak.cases;

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
class VerdelingsfoutOntvangerWitnessTest {

    private static final ObjectMapper JSON = new ObjectMapper();
    // cwd under surefire is the module dir (backends/drools); the corpus is two levels up.
    private static final Path RANG = Path.of("..", "..", "corpus", "golden", "jutterswet-rangmaximering");
    private static final Path BERGING = Path.of("..", "..", "corpus", "golden", "jutterswet-berging");
    private static final String RANG_REGEL = "verdeel het penningbudget van een penningkas";
    private static final String BERG_REGEL = "verdeel de opbrengst van een berging";

    /** Op volgorde with a naar-rato tie: W5 carries no tie weight, so the error lands on W5 and not on the kas. */
    @Test void het_leeg_gewicht_landt_op_de_ontvanger() throws Exception {
        BeideTakken.groen(RANG.resolve("ir.json"), rang(), "the empty-weight error on recipient W5");
        ObjectNode c = rang();
        verwacht(c, inconsistent(RANG_REGEL, "K1"));
        BeideTakken.rood(RANG.resolve("ir.json"), c,
                "verwacht inconsistent (" + RANG_REGEL + " @ K1), niet gevonden onder " + RANG_REGEL + " @ W5",
                "no error on the source kas");
    }

    /** Naar rato with a cap: H has no weight and D a negative maximum — one fact on each, none on W or the berging. */
    @Test void elke_overtredende_ontvanger_krijgt_een_eigen_fout() throws Exception {
        ObjectNode c = tweeOvertreders();
        verwacht(c, inconsistent(BERG_REGEL, "H"), inconsistent(BERG_REGEL, "D"));
        BeideTakken.groen(BERGING.resolve("ir.json"), c, "one error on H (weight), one on D (maximum)");
        for (String elders : new String[] { "W", "B1" }) {
            ObjectNode d = tweeOvertreders();
            verwacht(d, inconsistent(BERG_REGEL, elders));
            String uit = BeideTakken.rood(BERGING.resolve("ir.json"), d,
                    "verwacht inconsistent (" + BERG_REGEL + " @ " + elders + "), niet gevonden onder", "no error on " + elders);
            assertTrue(uit.contains(BERG_REGEL + " @ H") && uit.contains(BERG_REGEL + " @ D"), uit);
        }
    }

    /** Naar rato, every weight 0: nothing is distributed, so no cap is read and the empty maximum on W flags nothing. */
    @Test void een_nulgewichtsom_leest_geen_maximum() throws Exception {
        ObjectNode c = berging();
        for (JsonNode i : c.get("input").get("instances"))
            if (i.get("objectType").asText().equals("Berger"))
                ((ObjectNode) i.get("attributes")).set("geborgen gewicht", JSON.readTree("{\"number\": \"0\"}"));
        ((ObjectNode) instantie(c, "W").get("attributes")).remove("bergingsmaximum");
        verwacht(c, JSON.readTree("{\"kind\": \"consistent\"}"));
        BeideTakken.groen(BERGING.resolve("ir.json"), c, "zero weight sum: no cap read, no error");
    }

    /** Op volgorde with an empty source amount: nothing is distributed and W5's empty weight flags nothing. */
    @Test void een_lege_pot_meldt_niets() throws Exception {
        ObjectNode c = rang();
        ((ObjectNode) instantie(c, "K1").get("attributes")).remove("penningbudget");
        verwacht(c, JSON.readTree("{\"kind\": \"consistent\"}"));
        BeideTakken.groen(RANG.resolve("ir.json"), c, "empty pot: no distribution, no error");
    }

    // ---------------- fixtures ----------------

    private static ObjectNode rang() throws Exception {
        return (ObjectNode) JSON.readTree(Files.readString(RANG.resolve("case2-nulgroep.json")));
    }

    private static ObjectNode berging() throws Exception {
        return (ObjectNode) JSON.readTree(Files.readString(BERGING.resolve("case.json")));
    }

    private static ObjectNode tweeOvertreders() throws Exception {
        ObjectNode c = berging();
        ((ObjectNode) instantie(c, "H").get("attributes")).remove("geborgen gewicht");
        ((ObjectNode) instantie(c, "D").get("attributes")).set("bergingsmaximum", JSON.readTree("{\"number\": \"-5\"}"));
        return c;
    }

    private static JsonNode instantie(ObjectNode c, String label) {
        for (JsonNode i : c.get("input").get("instances")) if (i.get("label").asText().equals(label)) return i;
        throw new IllegalStateException("fixture has no instance " + label);
    }

    private static JsonNode inconsistent(String regel, String label) {
        ObjectNode e = JSON.createObjectNode();
        e.put("kind", "inconsistent").put("rule", regel).put("label", label);
        return e;
    }

    /** Replace the case's expectations: the rows here pin the channel, not the shares. */
    private static void verwacht(ObjectNode c, JsonNode... e) {
        ArrayNode a = JSON.createArrayNode();
        for (JsonNode x : e) a.add(x);
        c.set("expected", a);
    }
}
