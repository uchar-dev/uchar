package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/**
 * An allocation restates units as a rule does (RUNTIME-SEMANTICS ch. 4.2, 2.4, 2.6): the distribution runs in the unit
 * of the pot, a cap in another unit is restated into it before it is compared (ch. 2.6 rule 5), and a share and the
 * rest are restated out of it into the unit of their targets (ch. 2.4, ch. 2.6 rule 6). The rows put the goldens
 * {@code jutterswet-rangmaximering} (ordered) and {@code jutterswet-buitplafond} (proportional) in euros and cents and
 * replay them on both branches.
 *
 * <p>Red before the restatement: the ordered row with the cap in cents gave 1800, 0, 0, 0, 0 and rest 0 (the bare 50000
 * cent against 1800 euro); the proportional row with the cap in cents gave 20, 40, 60 and rest 0; the row with the
 * share and the rest in cents wrote the euro values 500, …, 26 unchanged.
 */
class AllocationUnitsWitnessTest {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Path GOLDEN = Path.of("..", "..", "corpus", "golden");
    private static final String EUR = "{\"node\": \"Unit\", \"abbreviation\": \"EUR\", \"system\": \"Munt\","
            + " \"canonical\": \"EUR\", \"factorToCanonical\": {\"num\": 1, \"den\": 1}}";
    private static final String CT = "{\"node\": \"Unit\", \"abbreviation\": \"ct\", \"system\": \"Munt\","
            + " \"canonical\": \"EUR\", \"factorToCanonical\": {\"num\": 1, \"den\": 100}}";

    @TempDir Path tmp;

    /** ch. 4.6's full recurrence with the cap of 500 euro written as 50000 cent: the same shares and rest as in euros. */
    @Test void an_ordered_cap_in_cents_compares_restated() throws Exception {
        Path ir = document("jutterswet-rangmaximering", Map.of("penningbudget", EUR, "onuitgekeerd budget", EUR,
                "penningmaximum", CT, "penningtoewijzing", EUR));
        ObjectNode c = rankCase(true);
        expect(c, "W1", "penningtoewijzing", "500", "W2", "penningtoewijzing", "500", "W3", "penningtoewijzing", "433",
                "W4", "penningtoewijzing", "250", "W5", "penningtoewijzing", "91", "K1", "onuitgekeerd budget", "26");
        BothBranches.green(ir, c, "cap in cents, pot and shares in euros: 500/500/433/250/91, rest 26");
    }

    /** The same recurrence with the cap in euros and the share and the rest in cents: each written restated. */
    @Test void an_ordered_share_and_rest_in_cents_are_written_restated() throws Exception {
        Path ir = document("jutterswet-rangmaximering", Map.of("penningbudget", EUR, "onuitgekeerd budget", CT,
                "penningmaximum", EUR, "penningtoewijzing", CT));
        ObjectNode c = rankCase(false);
        expect(c, "W1", "penningtoewijzing", "50000", "W2", "penningtoewijzing", "50000", "W3", "penningtoewijzing",
                "43300", "W4", "penningtoewijzing", "25000", "W5", "penningtoewijzing", "9100", "K1",
                "onuitgekeerd budget", "2600");
        BothBranches.green(ir, c, "share and rest in cents: 50000/50000/43300/25000/9100, rest 2600");
    }

    /** Proportional with a cap: 120 euro over 1:2:3, caps 30, 25 and 50 euro written as cents → 20, 25, 50, rest 25. */
    @Test void a_proportional_cap_in_cents_compares_restated() throws Exception {
        Path ir = document("jutterswet-buitplafond", Map.of("buitopbrengst", EUR, "buitrest", EUR, "buitplafond", CT,
                "buitdeel", EUR));
        ObjectNode c = (ObjectNode) JSON.readTree(Files.readString(GOLDEN.resolve("jutterswet-buitplafond/case.json")));
        timesHundred(c, "buitplafond");
        expect(c, "J1", "buitdeel", "20", "J2", "buitdeel", "25", "J3", "buitdeel", "50", "B1", "buitrest", "25");
        BothBranches.green(ir, c, "proportional cap in cents: 20/25/50, rest 25");
    }

    // ---------------- fixtures ----------------

    /** The golden {@code name} with the unit system Munt (EUR, ct = 1/100 EUR) and each attribute of {@code units} in
     *  its unit, written to the temporary directory. */
    private Path document(String name, Map<String, String> units) throws Exception {
        ObjectNode d = (ObjectNode) JSON.readTree(Files.readString(GOLDEN.resolve(name).resolve("ir.json")));
        ObjectNode system = (ObjectNode) JSON.readTree("{\"node\": \"UnitSystem\", \"name\": \"Munt\", \"units\": []}");
        ObjectNode eur = (ObjectNode) JSON.readTree(EUR), ct = (ObjectNode) JSON.readTree(CT);
        eur.set("spellings", JSON.readTree("[\"euro\"]"));
        ct.set("spellings", JSON.readTree("[\"cent\"]"));
        ((ArrayNode) system.get("units")).add(eur).add(ct);
        ((ObjectNode) d.get("model")).set("unitSystems", JSON.createArrayNode().add(system));
        for (JsonNode ot : d.get("model").get("objectTypes"))
            for (JsonNode a : ot.get("attributes")) {
                String unit = units.get(a.get("name").asText());
                if (unit != null) ((ObjectNode) a.get("datatype")).set("unit", JSON.createArrayNode().add(JSON.readTree(unit)));
            }
        Path ir = tmp.resolve(name + ".json");
        Files.writeString(ir, JSON.writeValueAsString(d));
        return ir;
    }

    /** The golden's case of {@code jutterswet-rangmaximering}, its caps in cents where {@code capInCents}. */
    private static ObjectNode rankCase(boolean capInCents) throws Exception {
        ObjectNode c = (ObjectNode) JSON.readTree(Files.readString(GOLDEN.resolve("jutterswet-rangmaximering/case.json")));
        if (capInCents) timesHundred(c, "penningmaximum");
        return c;
    }

    private static void timesHundred(ObjectNode c, String attribute) {
        for (JsonNode i : c.get("input").get("instances")) {
            JsonNode v = i.get("attributes").get(attribute);
            if (v != null) ((ObjectNode) v).put("number", String.valueOf(Long.parseLong(v.get("number").asText()) * 100));
        }
    }

    /** Replaces the expectations of {@code c} by the attribute values {@code triples} (label, attribute, number) and
     *  consistency. */
    private static void expect(ObjectNode c, String... triples) {
        ArrayNode e = JSON.createArrayNode();
        for (int i = 0; i < triples.length; i += 3) {
            ObjectNode x = e.addObject();
            x.put("kind", "attributeValue").put("label", triples[i]).put("attribute", triples[i + 1]);
            x.putObject("value").put("number", triples[i + 2]);
        }
        e.addObject().put("kind", "consistent");
        c.set("expected", e);
    }
}
