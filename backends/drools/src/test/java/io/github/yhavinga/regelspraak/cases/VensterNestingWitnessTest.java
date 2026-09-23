package io.github.yhavinga.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * WF-112 positions NESTED inside a position WF-112 admits — a GEEN fold or a GEEN composite as a member of an
 * ALLE or DE composite, a GEEN composite in the filter of an aggregation that sits in an operand. Both branches
 * refuse every one of them by name.
 *
 * <p><b>Why.</b> Measured 2026-09-23 on documents built exactly as below (the lens round of card
 * drools-venster-vouwtoets). The exec-model branch named WF-112 at all of them; the DRL-text branch refused them
 * as "voorwaarde" or "aggregatie-bullet onder tellende kwantor", because its composite arm put its own generic
 * marker over a member's reason. On the card's base the same branch PLAYED the two nestings of a GEEN fold in an
 * ALLE and in a DE composite, with the complement in a time-free session: the flank was closed by the card's fold
 * refusal, but under a name that said nothing.
 *
 * <p>The documents are {@code corpus/golden/jutterswet-schouwploeg} with the voorwaarde of "bevoegd bij diploma"
 * replaced. The case is the golden's own first case (time-free): the refusals happen when the rule is built,
 * before any case is read.
 */
class VensterNestingWitnessTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    /** cwd under surefire is the module dir (backends/drools); the corpus is two levels up. */
    private static final Path SCHOUWPLOEG = Path.of("..", "..", "corpus", "golden", "jutterswet-schouwploeg");

    private static final String IN_GEEN_VOUW = "kwantorvouw onder GEEN (WF-112)";
    private static final String IN_GEEN_SAMENSTELLING = "periodevenster in een GEEN-samenstelling (WF-112)";

    @TempDir Path tmp;

    private int n;

    @Test void een_geen_vouw_in_een_alle_samenstelling_weigeren_beide_takken_bij_naam() throws Exception {
        BeideTakken.geweigerd(document(samen("ALLE", ERVAREN, geenVouw(VENSTER))), casus(), IN_GEEN_VOUW,
                "ALLE[ervaren, GEEN-vouw(venster)]");
    }

    @Test void een_geen_vouw_in_een_de_samenstelling_weigeren_beide_takken_bij_naam() throws Exception {
        BeideTakken.geweigerd(document(samen("DE", ERVAREN, geenVouw(VENSTER))), casus(), IN_GEEN_VOUW,
                "DE[ervaren, GEEN-vouw(venster)]");
    }

    @Test void een_geen_samenstelling_in_een_alle_samenstelling_weigeren_beide_takken_bij_naam() throws Exception {
        BeideTakken.geweigerd(document(samen("ALLE", ERVAREN, samen("GEEN", VENSTER))), casus(),
                IN_GEEN_SAMENSTELLING, "ALLE[ervaren, GEEN[venster]]");
    }

    /** The GEEN composite sits in the FILTER of a sum that is an operand: the position is inside an expression. */
    @Test void een_geen_samenstelling_in_het_filter_van_een_operand_weigeren_beide_takken_bij_naam() throws Exception {
        BeideTakken.geweigerd(document(samen("ALLE", NIET_ERVAREN, groter(som(samen("GEEN", VENSTER)), 3))), casus(),
                IN_GEEN_SAMENSTELLING, "ALLE[niet ervaren, SOM(… die GEEN[venster]) > 3]");
        BeideTakken.geweigerd(document(samen("GEEN", groter(som(samen("GEEN", VENSTER)), 3))), casus(),
                IN_GEEN_SAMENSTELLING, "GEEN[SOM(… die GEEN[venster]) > 3]");
    }

    // ================= the documents =================

    private static final String NUM = "{ \"node\": \"Numeriek\", \"geheel\": true, \"decimalen\": 0,"
            + " \"teken\": \"ONBEPAALD\", \"eenheid\": null }";
    private static final String WACHT = "{ \"node\": \"RolBinding\", \"feittype\": \"schouwindeling\", \"rol\": \"wacht\" }";

    private static final String VENSTER = """
            { "node": "Periode",
              "vanaf": { "node": "Literal", "waarde": "2024-01-01", "type": { "node": "DatumInDagen" }, "bron": "2024-01-01" },
              "tot": { "node": "Literal", "waarde": "2025-01-01", "type": { "node": "DatumInDagen" }, "bron": "2025-01-01" },
              "totInclusief": false, "bron": "van 1 januari 2024 tot 1 januari 2025" }""";

    private static final String ERVAREN = """
            { "node": "Kenmerk", "kenmerk": "ervaren", "navigatie": null, "negatie": false, "eigenaar": null,
              "periode": null, "bron": "hij ervaren is" }""";
    private static final String NIET_ERVAREN = """
            { "node": "Kenmerk", "kenmerk": "ervaren", "navigatie": null, "negatie": true, "eigenaar": null,
              "periode": null, "bron": "hij niet ervaren is" }""";

    private static String samen(String kwantor, String... delen) {
        return "{ \"node\": \"Samengesteld\", \"kwantor\": \"" + kwantor + "\", \"drempel\": 0, \"onderdelen\": [ "
                + String.join(", ", delen) + " ], \"bron\": \"de volgende voorwaarden\" }";
    }

    private static String vouw(String kwantor, String elem, String rol, String filter, String toets) {
        return "{ \"node\": \"Kwantorvouw\", \"kwantor\": \"" + kwantor + "\", \"elementObjecttype\": \"" + elem + "\","
                + " \"filter\": " + (filter == null ? "null" : filter) + ", \"toets\": " + toets + ","
                + " \"bron\": \"de vouw over " + elem + "\", \"bronRol\": " + rol + " }";
    }

    private static String geenVouw(String toets) {
        return vouw("GEEN", "Strandwacht", WACHT, null, toets);
    }

    private static String som(String filter) {
        return "{ \"node\": \"Functie\", \"kind\": \"SOM\", \"argumenten\": [ { \"node\": \"AttribuutRef\","
                + " \"attribuut\": \"dienstjaren\", \"objecttype\": \"Strandwacht\","
                + " \"onderwerpBron\": \"de wachten van de Schouwploeg\", \"navigatieRol\": null, \"type\": " + NUM + ","
                + " \"bron\": \"de dienstjaren van de wachten\", \"set\": true, \"navigatiePad\": [] } ],"
                + " \"elementObjecttype\": \"Strandwacht\", \"leegwaarde\": null, \"filter\": " + filter + ","
                + " \"dimensieSelectie\": null, \"type\": " + NUM + ", \"bron\": \"de som van de dienstjaren\","
                + " \"granulariteit\": null, \"bronRol\": " + WACHT + " }";
    }

    private static String groter(String links, int n) {
        return "{ \"node\": \"Vergelijking\", \"op\": \"GROTER\", \"links\": " + links + ", \"rechts\": { \"node\":"
                + " \"Literal\", \"waarde\": \"" + n + "\", \"type\": " + NUM + ", \"bron\": \"" + n + "\" },"
                + " \"periode\": null, \"bron\": \"de som groter is dan " + n + "\" }";
    }

    /** The golden's document with the voorwaarde of "bevoegd bij diploma" replaced by {@code voorwaarde}. */
    private Path document(String voorwaarde) throws Exception {
        ObjectNode doc = (ObjectNode) JSON.readTree(Files.readString(SCHOUWPLOEG.resolve("ir.json")));
        for (JsonNode r : doc.withArray("resolvedRegels"))
            if ("bevoegd bij diploma".equals(r.path("naam").asText()))
                ((ObjectNode) r).set("voorwaarde", JSON.readTree(voorwaarde));
        return schrijf(doc);
    }

    private Path schrijf(ObjectNode doc) throws Exception {
        Path p = tmp.resolve("vensternesting-" + (n++) + ".json");
        Files.writeString(p, doc.toPrettyString());
        return p;
    }

    private static JsonNode casus() throws Exception {
        return JSON.readTree(Files.readString(SCHOUWPLOEG.resolve("case1-allen-ervaren.json")));
    }
}
