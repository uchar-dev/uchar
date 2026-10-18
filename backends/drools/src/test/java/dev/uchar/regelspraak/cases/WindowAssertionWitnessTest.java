package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * A TIME WINDOW (RUNTIME-SEMANTICS ch. 3.10) inside a CONSISTENCY ASSERTION (§13.4.8, ch. 9.5), replayed
 * through BOTH Drools branches: carried as a member of an ALLE assertion, which WF-112 admits, and refused BY
 * NAME under GEEN and the counting quantifiers, which it does not.
 *
 * <p><b>Why.</b> Measured 2026-09-23 (card drools-venster-vouwtoets, fix round 1) on documents built exactly as
 * below: the verifier passes the ALLE assertion and {@code ref} and clingo carry it with the values pinned here,
 * but both Drools branches refused it ("schending buiten Phase-D scope", "onopgelost consistentieregel") — the
 * same asymmetry the fold's toets had. Under GEEN and TEN MINSTE all four refuse, but the two Drools branches
 * under those same generic notes, where the position is WF-112's.
 *
 * <p><b>The document.</b> {@code corpus/golden/jutterswet-premievenster} reduced to its control rule
 * {@code basispremie} plus ONE consistency rule over the Vondst. A window criterion is violated where the day
 * lies outside it: a window is total, so outside is its plain negation, and an unset evaluation date or a leeg
 * bound puts the day outside (ch. 3.10). Beside the window the composite rows use "de meldatum is gevuld",
 * a criterion both branches carry; a characteristic criterion would not do, because the DRL-text branch
 * carries no characteristic in an assertion at all (gate 12 pins that for
 * {@code anchor/jutterswet-penninguitgifte}), window or no window.
 *
 * <p><b>The four Vondsten</b>: V1 meldatum 2024-03-01; V2 meldatum 2024-09-01 (after the evaluation date
 * 2024-06-15); V3 meldatum 2024-01-01; V4 without a meldatum (a leeg bound never matches, and the gevuld
 * criterion fails). The case format can only say "no inconsistency at all" or "these ones are inconsistent",
 * so the Vondsten that must stay consistent are pinned in a case of their own. Every verdict was computed by
 * hand and measured equal in {@code ref} and clingo on the same document.
 */
class WindowAssertionWitnessTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    /** cwd under surefire is the module dir (backends/drools); the corpus is two levels up. */
    private static final Path PREMIEVENSTER =
            Path.of("..", "..", "corpus", "golden", "jutterswet-premievenster");

    private static final String REGEL = "borg binnen het venster";

    @TempDir Path tmp;

    private int n;

    /** The bare window vanaf the Vondst's OWN meldatum as the whole assertion: on 2024-06-15 V1 and V3 hold it,
     *  V2 (meldatum after the date) and V4 (leeg meldatum) break it; in a time-free session all four do. */
    @Test void the_window_with_its_own_bound_as_assertion_plays() throws Exception {
        Path ir = document(MELDATUM);
        BothBranches.green(ir, caseDocument("2024-06-15", List.of(V1, V3)), "V1 en V3 houden de assertie");
        BothBranches.green(ir, caseDocument("2024-06-15", List.of(V2, V4), "V2", "V4"),
                "V2 buiten het venster, V4 zonder meldatum");
        BothBranches.green(ir, caseDocument(null, List.of(V1, V2, V3, V4), "V1", "V2", "V3", "V4"),
                "tijdvrij: het venster matcht nooit");
    }

    /** A literal window as an ALLE member beside "de meldatum is gevuld": V1 holds both on 2024-06-15, V4 breaks
     *  the first; on 2025-01-01 V1 breaks the window, whose tot is exclusive; time-free both break it. */
    @Test void a_literal_window_in_an_all_assertion_plays() throws Exception {
        Path ir = document(combined("all", GEVULD, LITERAL));
        BothBranches.green(ir, caseDocument("2024-06-15", List.of(V1)), "binnen het venster, gevuld");
        BothBranches.green(ir, caseDocument("2024-06-15", List.of(V4), "V4"), "binnen het venster, niet gevuld");
        BothBranches.green(ir, caseDocument("2025-01-01", List.of(V1), "V1"), "op de totgrens, die exclusief is");
        BothBranches.green(ir, caseDocument(null, List.of(V1, V4), "V1", "V4"), "tijdvrij: het venster matcht nooit");
    }

    /** WF-112's composite arm at the assertion: both branches refuse, naming the quantifier and the rule. */
    @Test void under_none_and_at_least_both_branches_refuse_the_window_by_name() throws Exception {
        BothBranches.refused(document(combined("none", GEVULD, LITERAL)), caseDocument(null, List.of(V1, V4)),
                "GEEN-samenstelling (WF-112)", "GEEN[gevuld, venster] als assertie");
        BothBranches.refused(document(togetherWithThreshold("atLeast", 1, GEVULD, LITERAL)),
                caseDocument(null, List.of(V1, V4)), "TEN_MINSTE-samenstelling (WF-112)",
                "TEN MINSTE 1[gevuld, venster] als assertie");
    }

    // ================= the document and the cases =================

    private static final String GEVULD = """
            { "node": "Predicate", "kind": "filled",
              "operand": { "node": "AttributeReference", "attribute": "meldatum", "subjectType": "Vondst",
                           "source": { "snippet": "de meldatum van de Vondst" } },
              "negated": false, "digits": 0,
              "source": { "snippet": "de meldatum van de Vondst gevuld is" } }""";

    private static final String MELDATUM = """
            { "node": "TimeWindow",
              "from": { "node": "AttributeReference", "attribute": "meldatum", "subjectType": "Vondst",
                         "source": { "snippet": "de meldatum van de Vondst" } },
              "toInclusive": false, "source": { "snippet": "vanaf de meldatum van de Vondst" } }""";

    private static final String LITERAL = """
            { "node": "TimeWindow",
              "from": { "node": "Literal", "value": { "date": "2024-01-01" }, "type": { "node": "Date", "precision": "day" }, "source": { "snippet": "2024-01-01" } },
              "to": { "node": "Literal", "value": { "date": "2025-01-01" }, "type": { "node": "Date", "precision": "day" }, "source": { "snippet": "2025-01-01" } },
              "toInclusive": false, "source": { "snippet": "van 1 januari 2024 tot 1 januari 2025" } }""";

    private static String combined(String quantifier, String... parts) {
        return togetherWithThreshold(quantifier, 0, parts);
    }

    private static String togetherWithThreshold(String quantifier, int threshold, String... parts) {
        return "{ \"node\": \"Compound\", \"quantifier\": \"" + quantifier + "\", \"threshold\": " + threshold
                + ", \"parts\": [ " + String.join(", ", parts) + " ],"
                + " \"source\": {\"snippet\": \"de Vondst aan de volgende voorwaarden voldoet\"} }";
    }

    /** The golden's document with its control rule only and one consistency rule asserting {@code assertion}. */
    private Path document(String assertion) throws Exception {
        ObjectNode doc = (ObjectNode) JSON.readTree(Files.readString(PREMIEVENSTER.resolve("ir.json")));
        ArrayNode out = JSON.createArrayNode();
        out.add(IrPath.form(doc, "rules", "basispremie van een vondst"));
        IrPath.rules(doc).set("rules", out);
        ArrayNode cons = JSON.createArrayNode();
        cons.add(JSON.readTree("{ \"node\": \"Consistency\", \"name\": \"" + REGEL + "\", \"versions\": [ {"
                + " \"subject\": \"Vondst\", \"assertion\": " + assertion + ","
                + " \"validity\": { \"node\": \"Validity\" },"
                + " \"source\": {\"snippet\": \"Een Vondst moet voldoen aan de volgende voorwaarden\", \"line\": 1} } ] }"));
        IrPath.rules(doc).set("consistencies", cons);
        Path p = tmp.resolve("vensterassertie-" + (n++) + ".json");
        Files.writeString(p, doc.toPrettyString());
        return p;
    }

    private static final String V1 = vondst("V1", "2024-03-01");
    private static final String V2 = vondst("V2", "2024-09-01");
    private static final String V3 = vondst("V3", "2024-01-01");
    private static final String V4 = vondst("V4", null);

    private static String vondst(String label, String meldatum) {
        return "{ \"label\": \"" + label + "\", \"objectType\": \"Vondst\", \"attributes\": "
                + (meldatum == null ? "{}" : "{ \"meldatum\": { \"date\": \"" + meldatum + "\" } }")
                + ", \"roles\": {}, \"characteristics\": [] }";
    }

    /** The given Vondsten at {@code asOf}: basispremie 1 on each (the control fires everywhere), and the named
     *  ones inconsistent under the rule — or, when none is named, no inconsistency at all. */
    private static JsonNode caseDocument(String asOf, List<String> vondsten, String... inconsistent) throws Exception {
        ObjectNode c = JSON.createObjectNode();
        c.put("format", "uchar-case-v0");
        if (asOf == null) c.putNull("asOfDate"); else c.put("asOfDate", asOf);
        ObjectNode input = c.putObject("input");
        input.set("instances", JSON.readTree("[ " + String.join(", ", vondsten) + " ]"));
        input.putObject("parameters");
        ArrayNode expected = c.putArray("expected");
        for (JsonNode v : input.get("instances"))
            expected.addObject().put("kind", "attributeValue").put("label", v.get("label").asText())
                    .put("attribute", "basispremie").set("value", JSON.createObjectNode().put("number", "1"));
        for (String label : inconsistent)
            expected.addObject().put("kind", "inconsistent").put("rule", REGEL).put("label", label);
        if (inconsistent.length == 0) expected.addObject().put("kind", "consistent");
        return c;
    }
}
