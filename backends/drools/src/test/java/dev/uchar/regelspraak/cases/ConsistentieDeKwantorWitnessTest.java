package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * IR-SPEC ch. 4.5 / RUNTIME-SEMANTICS ch. 5.10: a consistency whose assertion is a composite under the quantifier
 * {@code the}. On a {@code Compound} that quantifier reads as {@code all} does — every part must hold, h = n — and
 * {@code ref} and the ASP backend answer it so. Both Drools branches excluded it until the card
 * aantal-bronarm-en-drools-lader (the exec-model branch with a transpiler SKIP, "schending buiten Phase-D scope", the
 * DRL-text branch with a marker), while they played the same assertion under {@code all}; since that card both
 * branches lower it through their {@code all} arm. RegelSpraak spells no {@code the} in a consistency (its
 * kwantificatie there has no "de", and "het volgende criterium" is read as {@code all}), so no golden carries the
 * form; this witness does, on an in-memory variant of {@code jutterswet-keurcriteria}, read and never written.
 *
 * <p>Measured before the card on the same two documents with the same finds: {@code ref} and the ASP backend
 * matched every expectation below, both Drools branches excluded the rule.
 */
class ConsistentieDeKwantorWitnessTest {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Path GOLDEN = Path.of("..", "..", "corpus", "golden", "jutterswet-keurcriteria");
    private static final String NAAM = "keuring naar de criteria";

    @Test void de_kwantor_over_twee_criteria_vraagt_ze_allebei() throws Exception {
        // V1 meets both criteria; V9 meets the first only; V5 has no gewicht, so the first does not hold
        BeideTakken.groen(tijdelijk(metDe(2)), zaak("[" + vondst("V1", "2.50", "12") + "]", null),
                "the over two criteria, both hold: consistent");
        BeideTakken.groen(tijdelijk(metDe(2)), zaak("[" + vondst("V9", "2.50", "5") + "]", "V9"),
                "the over two criteria, one holds: violated, as under all");
        BeideTakken.groen(tijdelijk(metDe(2)), zaak("[" + vondst("V5", null, "12") + "]", "V5"),
                "the over two criteria, a leeg operand: its criterion does not hold");
    }

    @Test void de_kwantor_over_een_criterium_vraagt_dat_ene() throws Exception {
        BeideTakken.groen(tijdelijk(metDe(1)), zaak("[" + vondst("V9", "2.50", "5") + "]", null),
                "the over the first criterion alone, which holds: consistent");
        BeideTakken.groen(tijdelijk(metDe(1)), zaak("[" + vondst("V4", "0.00", "5") + "]", "V4"),
                "the over the first criterion alone, which does not hold: violated");
    }

    // ---------------- fixtures ----------------

    /** The golden reduced to its first consistency, under {@code the} and over its first {@code delen} criteria. */
    private static ObjectNode metDe(int delen) throws Exception {
        ObjectNode ir = (ObjectNode) JSON.readTree(Files.readString(GOLDEN.resolve("ir.json")));
        ObjectNode regels = (ObjectNode) ir.get("rules");
        ObjectNode k = (ObjectNode) regels.get("consistencies").get(0).deepCopy();
        k.put("name", NAAM);
        ObjectNode assertie = (ObjectNode) k.get("versions").get(0).get("assertion");
        assertie.put("quantifier", "the").put("threshold", 0);
        ArrayNode delenLijst = (ArrayNode) assertie.get("parts");
        while (delenLijst.size() > delen) delenLijst.remove(delenLijst.size() - 1);
        regels.putArray("consistencies").add(k);
        return ir;
    }

    private static String vondst(String label, String gewicht, String lengte) {
        return "{ \"label\": \"" + label + "\", \"objectType\": \"Vondst\", \"attributes\": {"
                + (gewicht == null ? "" : "\"gewicht\": { \"number\": \"" + gewicht + "\" }, ")
                + "\"lengte\": { \"number\": \"" + lengte + "\" } }, \"roles\": {}, \"characteristics\": [] }";
    }

    /** One case: consistent, or the rule violated on {@code geschonden}. */
    private static JsonNode zaak(String instanties, String geschonden) throws Exception {
        String verwacht = geschonden == null ? "{ \"kind\": \"consistent\" }"
                : "{ \"kind\": \"inconsistent\", \"rule\": \"" + NAAM + "\", \"label\": \"" + geschonden + "\" }";
        return JSON.readTree("{ \"format\": \"uchar-case-v0\", \"asOfDate\": null, \"input\": { \"instances\": "
                + instanties + ", \"parameters\": {} }, \"expected\": [ " + verwacht + " ] }");
    }

    private static Path tijdelijk(ObjectNode ir) throws Exception {
        Path tmp = Files.createTempFile("consistentie-de", ".json");
        tmp.toFile().deleteOnExit();
        Files.writeString(tmp, ir.toString());
        return tmp;
    }
}
