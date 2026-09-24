package io.github.yhavinga.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * A TIME WINDOW (RUNTIME-SEMANTICS ch. 3.10) in the gate of a DISTRIBUTION (§13.4.10) and of an OBJECT CREATION
 * (§13.4.6), replayed through BOTH Drools branches: carried as the whole gate or an ALLE member, which WF-112
 * admits, and refused BY NAME under GEEN and a counting quantifier, which it does not. A uniqueness rule is not
 * here: its wire node carries no condition at all, so WF-112 has no position to reach there.
 *
 * <p><b>Why.</b> Measured 2026-09-23 (card drools-venster-vouwtoets, fix round 3) on documents built exactly as
 * below. The verifier, {@code ref} and clingo carry the admitted forms with the values pinned here and refuse the
 * WF-112 forms. Both Drools branches played the admitted forms already. Under GEEN and TEN MINSTE the exec-model
 * branch named WF-112 at both gates, and so did the DRL-text branch at the creation; at the distribution it
 * refused as "onopgelost verdeling (voorwaarde)" only, because that emitter writes its own marker over the
 * condition's.
 *
 * <p><b>The documents.</b> {@code corpus/golden/jutterswet-voorwaardedeling} with its gate (vrijgavescore ≥ 10)
 * wrapped: B1 (score 12) distributes 100 naar rato over J1 (60) and J2 (20), B2 (score 7) and B3 (no score) do
 * not, so the golden's values stand on 2024-06-15 and nothing is distributed in a time-free session; the bare
 * window lets B2 and B3 distribute too (J3 40, J4 50). {@code corpus/golden/synth2-budgetgrens-onder} with its
 * creation guard (rang &lt; 98) wrapped: on 2024-06-15 the chain is minted and W1's opvolgerrang is 2, as in the
 * golden; in a time-free session nothing is minted and it is 0. Every value was measured equal in {@code ref} and
 * clingo on the same documents.
 */
class VensterGatepositieWitnessTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    /** cwd under surefire is the module dir (backends/drools); the corpus is two levels up. */
    private static final Path VERDELING = Path.of("..", "..", "corpus", "golden", "jutterswet-voorwaardedeling");
    private static final Path CREATIE = Path.of("..", "..", "corpus", "golden", "synth2-budgetgrens-onder");

    @TempDir Path tmp;

    private int n;

    @Test void het_venster_in_de_poort_van_een_verdeling_speelt() throws Exception {
        Path alle = verdeling(samen("ALLE", 0, "{score}", VENSTER));
        BeideTakken.groen(alle, verdelingCasus("2024-06-15", "75", "25", null, null), "ALLE[score, venster], binnen");
        BeideTakken.groen(alle, verdelingCasus(null, null, null, null, null), "ALLE[score, venster], tijdvrij");
        Path kaal = verdeling(VENSTER);
        BeideTakken.groen(kaal, verdelingCasus("2024-06-15", "75", "25", "40", "50"), "het kale venster, binnen");
        BeideTakken.groen(kaal, verdelingCasus(null, null, null, null, null), "het kale venster, tijdvrij");
    }

    @Test void onder_geen_en_ten_minste_weigeren_beide_takken_de_verdeling_bij_naam() throws Exception {
        BeideTakken.geweigerd(verdeling(samen("GEEN", 0, "{score}", VENSTER)), verdelingCasus(null, null, null, null, null),
                "periodevenster in een GEEN-samenstelling (WF-112)", "GEEN[score, venster] als verdelingspoort");
        BeideTakken.geweigerd(verdeling(samen("TEN_MINSTE", 1, "{score}", VENSTER)),
                verdelingCasus(null, null, null, null, null),
                "periodevenster in een TEN_MINSTE-samenstelling (WF-112)", "TEN MINSTE 1[score, venster]");
    }

    @Test void het_venster_in_de_guard_van_een_objectcreatie_speelt() throws Exception {
        Path alle = creatie(samen("ALLE", 0, "{guard}", VENSTER));
        BeideTakken.groen(alle, creatieCasus("2024-06-15", "2"), "ALLE[rang < 98, venster], binnen");
        BeideTakken.groen(alle, creatieCasus(null, "0"), "ALLE[rang < 98, venster], tijdvrij: niets gemunt");
    }

    @Test void onder_geen_en_ten_minste_weigeren_beide_takken_de_objectcreatie_bij_naam() throws Exception {
        BeideTakken.geweigerd(creatie(samen("GEEN", 0, "{guard}", VENSTER)), creatieCasus(null, "0"),
                "periodevenster in een GEEN-samenstelling (WF-112)", "GEEN[rang < 98, venster] als creatieguard");
        BeideTakken.geweigerd(creatie(samen("TEN_MINSTE", 1, "{guard}", VENSTER)), creatieCasus(null, "0"),
                "periodevenster in een TEN_MINSTE-samenstelling (WF-112)", "TEN MINSTE 1[rang < 98, venster]");
    }

    // ================= the documents and the cases =================

    private static final String VENSTER = """
            { "node": "TimeWindow",
              "from": { "node": "Literal", "value": { "date": "2024-01-01" }, "type": { "node": "Date", "precision": "day" }, "source": { "snippet": "2024-01-01" } },
              "to": { "node": "Literal", "value": { "date": "2025-01-01" }, "type": { "node": "Date", "precision": "day" }, "source": { "snippet": "2025-01-01" } },
              "toInclusive": false, "source": { "snippet": "van 1 januari 2024 tot 1 januari 2025" } }""";

    /** A composite over the golden's own gate ({@code {score}} / {@code {guard}}, filled in by the caller) and the window. */
    private static String samen(String kwantor, int drempel, String gate, String venster) {
        return "{ \"node\": \"Compound\", \"quantifier\": \"" + kwantor + "\", \"threshold\": " + drempel
                + ", \"parts\": [ " + gate + ", " + venster + " ], \"source\": {\"snippet\": \"de volgende voorwaarden\"} }";
    }

    /** The distribution golden with its gate replaced; a {@code {score}} placeholder is the golden's own gate. */
    private Path verdeling(String gate) throws Exception {
        ObjectNode doc = (ObjectNode) JSON.readTree(Files.readString(VERDELING.resolve("ir.json")));
        ObjectNode v = IrPad.versie(doc, "allocations", 0, 0);
        v.set("condition", JSON.readTree(gate.replace("{score}", v.get("condition").toString())));
        return schrijf(doc);
    }

    /** The creation golden with its guard replaced; a {@code {guard}} placeholder is the golden's own guard. */
    private Path creatie(String guard) throws Exception {
        ObjectNode doc = (ObjectNode) JSON.readTree(Files.readString(CREATIE.resolve("ir.json")));
        ObjectNode oc = IrPad.versie(doc, "objectCreations", 0, 0);
        oc.set("condition", JSON.readTree(guard.replace("{guard}", oc.get("condition").toString())));
        return schrijf(doc);
    }

    private Path schrijf(ObjectNode doc) throws Exception {
        Path p = tmp.resolve("vensterpoort-" + (n++) + ".json");
        Files.writeString(p, doc.toPrettyString());
        return p;
    }

    /** The distribution golden's case at {@code asOf}, with J1..J4's bergdeel expected (null: empty). */
    private static JsonNode verdelingCasus(String asOf, String j1, String j2, String j3, String j4) throws Exception {
        ObjectNode c = (ObjectNode) JSON.readTree(Files.readString(VERDELING.resolve("case.json")));
        if (asOf == null) c.putNull("asOfDate"); else c.put("asOfDate", asOf);
        var verwacht = c.putArray("expected");
        String[] labels = { "J1", "J2", "J3", "J4" }, waarden = { j1, j2, j3, j4 };
        for (int i = 0; i < 4; i++) {
            ObjectNode e = verwacht.addObject().put("label", labels[i]).put("attribute", "bergdeel");
            if (waarden[i] == null) e.put("kind", "attributeEmpty");
            else e.put("kind", "attributeValue").set("value", JSON.createObjectNode().put("number", waarden[i]));
        }
        verwacht.addObject().put("kind", "consistent");
        return c;
    }

    /** The creation golden's case at {@code asOf}, with W1's opvolgerrang expected at {@code rang}. */
    private static JsonNode creatieCasus(String asOf, String rang) throws Exception {
        ObjectNode c = (ObjectNode) JSON.readTree(Files.readString(CREATIE.resolve("case1.json")));
        if (asOf == null) c.putNull("asOfDate"); else c.put("asOfDate", asOf);
        for (JsonNode e : c.withArray("expected"))
            if ("opvolgerrang".equals(e.path("attribute").asText()))
                ((ObjectNode) e).set("value", JSON.createObjectNode().put("number", rang));
        return c;
    }
}
