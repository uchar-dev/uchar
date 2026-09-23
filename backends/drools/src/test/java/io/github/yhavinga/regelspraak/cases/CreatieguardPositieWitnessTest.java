package io.github.yhavinga.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * The GUARD of a fact creation (§13.4.7, ch. 3.4) at the two positions its doel attempt meets: a time window
 * under GEEN (WF-112) and a doel host nested in a composite or on a navigating check (WF-109's first two doel
 * fences). Both branches refuse both BY NAME; the window as an ALLE member of the guard plays.
 *
 * <p><b>Why.</b> Measured 2026-09-23 (card drools-venster-vouwtoets, fix round 1) on documents built from
 * {@code corpus/golden/jutterswet-bakensein} — anker {@code Kustvak}, doel {@code Sein} ⊑ {@code Baken}. A
 * GEEN[window] guard: the verifier rejects it (WF-112), {@code ref} and clingo refuse it, the exec-model branch
 * named WF-112 and the DRL-text branch refused it as "voorwaarde niet naar anker of doel te lowereren" only.
 * A composite guard whose two checks are both hosted on {@code Baken}: the verifier rejects it (WF-109: inside
 * a composite only the anker is bound), {@code ref} and clingo refuse it, the exec-model branch refused it
 * generically — and the DRL-text branch PLAYED it, inlining the composite on the doel pattern. A document that
 * skips the verifier reaches this backend as it is, so both branches carry the fence themselves.
 *
 * <p>The two corpus negatives of the same fences ({@code wf109-doelgastheer-samengesteld},
 * {@code wf109-doelgastheer-genavigeerd}) were refused by both branches already, under the generic note; they
 * now carry the rule's name as well.
 */
class CreatieguardPositieWitnessTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    /** cwd under surefire is the module dir (backends/drools); the corpus is two levels up. */
    private static final Path BAKENSEIN = Path.of("..", "..", "corpus", "golden", "jutterswet-bakensein");
    private static final Path SEINHIJSING = Path.of("..", "..", "corpus", "golden", "jutterswet-seinhijsing");
    private static final Path NEGATIEF = Path.of("..", "..", "corpus", "negatief");

    @TempDir Path tmp;

    private int n;

    /** The admitted position: the window as an ALLE member of an anker-side guard. V1 (the Kustvak) is gehesen, so
     *  on 2024-06-15 the guard holds and both seinen of its post get the relation; in a time-free session none. */
    @Test void het_venster_als_alle_lid_van_een_ankerguard_speelt() throws Exception {
        Path ir = document(samen("ALLE", GEHESEN_ANKER, VENSTER));
        BeideTakken.groen(ir, casus("2024-06-15", "2"), "binnen het venster: twee vakseinen");
        BeideTakken.groen(ir, casus(null, "0"), "tijdvrij: het venster matcht nooit");
    }

    /** WF-112 at the guard: both branches name the quantifier and the rule. */
    @Test void een_venster_onder_geen_in_een_guard_weigeren_beide_takken_bij_naam() throws Exception {
        BeideTakken.geweigerd(document(samen("GEEN", GEHESEN_ANKER, VENSTER)), casus(null, "0"),
                "GEEN-samenstelling (WF-112)", "GEEN[gehesen, venster] als guard");
    }

    /** WF-109's first doel fence: a doel host inside a composite binds nothing, even where both checks are hosted
     *  on the doel's supertype and the doel pattern could carry them. */
    @Test void een_doelgastheer_in_een_samenstelling_weigeren_beide_takken_bij_naam() throws Exception {
        BeideTakken.geweigerd(document(samen("ALLE", GEHESEN_BAKEN, GEHESEN_BAKEN)), casus(null, "1"),
                "doelgastheer binnen een samengestelde of genavigeerde voorwaarde", "ALLE[host Baken, host Baken]");
    }

    /** The two corpus negatives of fences one and two, loaded without the verifier. */
    @Test void de_wf109_doelnegatieven_weigeren_beide_takken_bij_naam() throws Exception {
        JsonNode c = JSON.readTree(Files.readString(SEINHIJSING.resolve("case.json")));
        for (String negatief : new String[] { "wf109-doelgastheer-samengesteld", "wf109-doelgastheer-genavigeerd" })
            BeideTakken.geweigerd(NEGATIEF.resolve(negatief).resolve("ir.json"), c,
                    "doelgastheer binnen een samengestelde of genavigeerde voorwaarde: alleen de anker is gebonden (WF-109)", negatief);
    }

    // ================= the document and the cases =================

    private static final String GEHESEN_ANKER = """
            { "node": "Kenmerk", "kenmerk": "gehesen", "negatie": false, "eigenaar": null,
              "periode": null, "bron": "het kustvak gehesen is" }""";

    private static final String GEHESEN_BAKEN = """
            { "node": "Kenmerk", "kenmerk": "gehesen", "negatie": false, "eigenaar": "Baken",
              "periode": null, "bron": "het sein gehesen is" }""";

    private static final String VENSTER = """
            { "node": "Periode",
              "vanaf": { "node": "Literal", "waarde": "2024-01-01", "type": { "node": "DatumInDagen" },
                         "bron": "2024-01-01" },
              "tot": { "node": "Literal", "waarde": "2025-01-01", "type": { "node": "DatumInDagen" },
                       "bron": "2025-01-01" },
              "totInclusief": false, "bron": "van 1 januari 2024 tot 1 januari 2025" }""";

    private static String samen(String kwantor, String... delen) {
        return "{ \"node\": \"Samengesteld\", \"kwantor\": \"" + kwantor + "\", \"drempel\": 0, \"onderdelen\": [ "
                + String.join(", ", delen) + " ], \"bron\": \"het kustvak aan de volgende voorwaarden voldoet\" }";
    }

    /** The golden's document with its creation guard replaced by {@code guard}. */
    private Path document(String guard) throws Exception {
        ObjectNode doc = (ObjectNode) JSON.readTree(Files.readString(BAKENSEIN.resolve("ir.json")));
        ((ObjectNode) IrPad.rulesLijst(doc, "feitCreaties").get(0)).set("voorwaarde", JSON.readTree(guard));
        Path p = tmp.resolve("creatieguard-" + (n++) + ".json");
        Files.writeString(p, doc.toPrettyString());
        return p;
    }

    /** The golden's own case at {@code asOf}, with the kustvak's vakseinaantal expected at {@code vakseinen}. */
    private static JsonNode casus(String asOf, String vakseinen) throws Exception {
        ObjectNode c = (ObjectNode) JSON.readTree(Files.readString(BAKENSEIN.resolve("case.json")));
        if (asOf == null) c.putNull("asOfDate"); else c.put("asOfDate", asOf);
        for (JsonNode e : c.withArray("expected"))
            if ("vakseinaantal".equals(e.path("attribute").asText()))
                ((ObjectNode) e).set("value", JSON.createObjectNode().put("number", vakseinen));
        return c;
    }
}
