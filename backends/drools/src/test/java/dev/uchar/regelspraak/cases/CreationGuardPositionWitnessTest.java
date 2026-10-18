package dev.uchar.regelspraak.cases;

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
class CreationGuardPositionWitnessTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    /** cwd under surefire is the module dir (backends/drools); the corpus is two levels up. */
    private static final Path BAKENSEIN = Path.of("..", "..", "corpus", "golden", "jutterswet-bakensein");
    private static final Path SEINHIJSING = Path.of("..", "..", "corpus", "golden", "jutterswet-seinhijsing");
    private static final Path NEGATIEF = Path.of("..", "..", "corpus", "negatief");

    @TempDir Path tmp;

    private int n;

    /** The admitted position: the window as an ALLE member of an anker-side guard. V1 (the Kustvak) is gehesen, so
     *  on 2024-06-15 the guard holds and both seinen of its post get the relation; in a time-free session none. */
    @Test void the_window_as_all_member_of_an_anchor_guard_plays() throws Exception {
        Path ir = document(combined("all", GEHESEN_ANCHOR, WINDOW));
        BothBranches.green(ir, goldenCase("2024-06-15", "2"), "binnen het venster: twee vakseinen");
        BothBranches.green(ir, goldenCase(null, "0"), "tijdvrij: het venster matcht nooit");
    }

    /** WF-112 at the guard: both branches name the quantifier and the rule. */
    @Test void both_branches_refuse_a_window_under_none_in_a_guard_by_name() throws Exception {
        BothBranches.refused(document(combined("none", GEHESEN_ANCHOR, WINDOW)), goldenCase(null, "0"),
                "GEEN-samenstelling (WF-112)", "GEEN[gehesen, venster] als guard");
    }

    /** WF-109's first doel fence: a doel host inside a composite binds nothing, even where both checks are hosted
     *  on the doel's supertype and the doel pattern could carry them. */
    @Test void both_branches_refuse_a_target_host_in_a_compound_by_name() throws Exception {
        BothBranches.refused(document(combined("all", GEHESEN_BAKEN, GEHESEN_BAKEN)), goldenCase(null, "1"),
                "doelgastheer binnen een samengestelde of genavigeerde voorwaarde", "ALLE[host Baken, host Baken]");
    }

    /** The two corpus negatives of fences one and two, loaded without the verifier. */
    @Test void the_wf109_target_negatives_refuse_both_branches_by_name() throws Exception {
        JsonNode c = JSON.readTree(Files.readString(SEINHIJSING.resolve("case.json")));
        for (String negative : new String[] { "wf109-doelgastheer-samengesteld", "wf109-doelgastheer-genavigeerd" })
            BothBranches.refused(NEGATIEF.resolve(negative).resolve("ir.json"), c,
                    "doelgastheer binnen een samengestelde of genavigeerde voorwaarde: alleen de anker is gebonden (WF-109)", negative);
    }

    // ================= the document and the cases =================

    private static final String GEHESEN_ANCHOR = """
            { "node": "CharacteristicCheck", "characteristic": "gehesen", "negated": false,
              "source": { "snippet": "het kustvak gehesen is" } }""";

    private static final String GEHESEN_BAKEN = """
            { "node": "CharacteristicCheck", "characteristic": "gehesen", "negated": false, "host": "Baken",
              "source": { "snippet": "het sein gehesen is" } }""";

    private static final String WINDOW = """
            { "node": "TimeWindow",
              "from": { "node": "Literal", "value": { "date": "2024-01-01" }, "type": { "node": "Date", "precision": "day" }, "source": { "snippet": "2024-01-01" } },
              "to": { "node": "Literal", "value": { "date": "2025-01-01" }, "type": { "node": "Date", "precision": "day" }, "source": { "snippet": "2025-01-01" } },
              "toInclusive": false, "source": { "snippet": "van 1 januari 2024 tot 1 januari 2025" } }""";

    private static String combined(String quantifier, String... parts) {
        return "{ \"node\": \"Compound\", \"quantifier\": \"" + quantifier + "\", \"threshold\": 0, \"parts\": [ "
                + String.join(", ", parts) + " ], \"source\": {\"snippet\": \"het kustvak aan de volgende voorwaarden voldoet\"} }";
    }

    /** The golden's document with its creation guard replaced by {@code guard}. */
    private Path document(String guard) throws Exception {
        ObjectNode doc = (ObjectNode) JSON.readTree(Files.readString(BAKENSEIN.resolve("ir.json")));
        IrPath.version(doc, "factCreations", 0, 0).set("condition", JSON.readTree(guard));
        Path p = tmp.resolve("creatieguard-" + (n++) + ".json");
        Files.writeString(p, doc.toPrettyString());
        return p;
    }

    /** The golden's own case at {@code asOf}, with the kustvak's vakseinaantal expected at {@code vakseinen}. */
    private static JsonNode goldenCase(String asOf, String vakseinen) throws Exception {
        ObjectNode c = (ObjectNode) JSON.readTree(Files.readString(BAKENSEIN.resolve("case.json")));
        if (asOf == null) c.putNull("asOfDate"); else c.put("asOfDate", asOf);
        for (JsonNode e : c.withArray("expected"))
            if ("vakseinaantal".equals(e.path("attribute").asText()))
                ((ObjectNode) e).set("value", JSON.createObjectNode().put("number", vakseinen));
        return c;
    }
}
