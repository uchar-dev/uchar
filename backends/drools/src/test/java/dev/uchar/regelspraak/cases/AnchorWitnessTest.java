package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.uchar.regelspraak.refusal.DroolsCode;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The form anchor (IR-SPEC ch. 4.1a, WF-122; RUNTIME-SEMANTICS ch. 3.4a) on both branches, beyond what the golden
 * {@code jutterswet-registervoorrang} plays: an anchored own read of an input attribute and the identity check.
 *
 * <p>The six goldens that spell a read of the rule subject in a filter or a quantifier matrix as the way back from the
 * element ("de afmeerdag van de schuit van de Lading", over a to-one role) are rewritten here into the anchored spelling
 * ("de afmeerdag van de Schuit zelf"): the first hop of the read, or the first step of the range of the fold, that walks
 * back over the last step of the range around it leaves, and the node gets {@code anchor: "form"}. Where the way back is
 * to-one the two spellings name one object (IR-SPEC ch. 4.1a), so every case of the golden keeps its pinned
 * expectations, on both branches. The rewritten documents carry what the corpus does not yet: an anchored read of a
 * derived attribute (the verlengde einddag), an anchored fold in a filter (the klachten of the Schuit), anchored reads
 * inside a listed fold in the filter of a value fold and of a quantifier. Measured 2026-10-23 before the rewrite into
 * this test: verifier-clean, and ref plays every case of each rewritten document; ref's trace and the readings of both
 * branches agree, as gate 17 compares them. Once the frontend writes the anchored spelling itself (card
 * subselection-reads-subject, step 6), a golden here may carry its anchors already: the floor counts the anchored nodes
 * after the rewrite, whichever spelling the golden had.
 */
class AnchorWitnessTest {

    private static final Path GOLDEN = Path.of("..", "..", "corpus", "golden");

    private static final List<String> WAY_BACK = List.of("fold-filter-nested-aggregate",
            "jutterswet-schone-maand-na-zware-klacht", "jutterswet-vergunningtermijnen",
            "fold-filter-inner-fold-via-element-role", "jutterswet-late-vondst",
            "jutterswet-vroege-vondsten-zonder-schorsing");

    /** The anchored nodes of the six documents after the rewrite, measured 2026-10-23: 19 (10 + 1 + 2 + 1 + 2 + 3). */
    private static final int EXPECTED_MIN_ANCHORED = 19;

    @Test void the_anchored_spelling_of_the_way_back_goldens_plays_their_cases_on_both_branches() throws Exception {
        int anchored = 0, cases = 0;
        for (String g : WAY_BACK) {
            ObjectNode doc = (ObjectNode) TmsDocuments.JSON.readTree(Files.readString(GOLDEN.resolve(g).resolve("ir.json")));
            anchorize(doc, factTypes(doc), List.of(), 0);
            anchored += count(doc);
            Path ir = temporary(doc);
            for (Path c : cases(GOLDEN.resolve(g))) {
                BothBranches.green(ir, TmsDocuments.JSON.readTree(Files.readString(c)), g + "/" + c.getFileName());
                cases++;
            }
        }
        int n = anchored;
        assertTrue(n >= EXPECTED_MIN_ANCHORED, () -> "anchored nodes " + n + " under the floor " + EXPECTED_MIN_ANCHORED);
        assertTrue(cases > 0, "no case played");
    }

    /** A characteristic of the subject of the form read in a filter, one a rule derives: "de Vergunning zelf heeft de
     *  voorrang" beside the two parts of the filter of the maximum. The maximum of a Vergunning without the voorrang
     *  folds no member; V1 of case 1 has it (20), V2 and V3 do not (leeg where the golden pins 15 and leeg). */
    @Test void an_anchored_check_of_a_derived_characteristic_reads_the_subject_of_the_form() throws Exception {
        ObjectNode doc = golden("jutterswet-registervoorrang");
        ObjectNode check = TmsDocuments.JSON.createObjectNode();
        check.put("node", "CharacteristicCheck").put("characteristic", "voorrang").put("negated", false)
                .put("anchor", "form").putObject("source").put("snippet", "de Vergunning zelf heeft de voorrang");
        ((ArrayNode) doc.at("/rules/rules/0/versions/0/value/filter/parts")).add(check);
        ObjectNode c = (ObjectNode) TmsDocuments.JSON.readTree(Files.readString(
                GOLDEN.resolve("jutterswet-registervoorrang").resolve("case1-drie-vergunningen.json")));
        int changed = 0;
        for (JsonNode e : c.get("expected"))
            if (e.path("label").asText().equals("V2") && e.path("attribute").asText().equals("hoogste latere tarief")) {
                ((ObjectNode) e).put("kind", "attributeEmpty").remove("value");
                changed++;
            }
        assertEquals(1, changed, "case 1 pins the maximum of V2");
        BothBranches.green(temporary(doc), c, "jutterswet-registervoorrang/case1 with the anchored check");
        // the probe: the golden's own pin of V2, 15, is what a check read on the member V3 instead would give
        BothBranches.red(temporary(doc), TmsDocuments.JSON.readTree(Files.readString(
                GOLDEN.resolve("jutterswet-registervoorrang").resolve("case1-drie-vergunningen.json"))),
                "hoogste latere tarief", "the golden's pin no longer holds with the check");
    }

    /** An anchored node in a fold that is read per member of another fold (the identity check and the comparison of the
     *  golden's quantifier, moved into a quantifier over the register of the member, in the test of the outer one): the
     *  rules of the inner fold bind no subject of the form, so both branches refuse the form by name, in one sentence. */
    @Test void an_anchored_node_in_a_fold_read_per_member_of_another_fold_is_refused_on_both_branches() throws Exception {
        ObjectNode doc = golden("jutterswet-registervoorrang");
        ObjectNode outer = (ObjectNode) doc.at("/rules/rules/1/versions/0/condition");
        ObjectNode inner = outer.deepCopy();
        inner.remove("source");
        inner.putObject("source").put("snippet", "alle vergunningen van het register van de vergunning");
        outer.set("test", inner);
        Path ir = temporary(doc);
        JsonNode c = TmsDocuments.JSON.readTree(Files.readString(
                GOLDEN.resolve("jutterswet-registervoorrang").resolve("case1-drie-vergunningen.json")));
        String exec = null;
        for (boolean viaDrl : new boolean[] {false, true}) {
            CaseRunner.Excluded e = assertThrows(CaseRunner.Excluded.class, () -> CaseRunner.playCase(ir, c, viaDrl));
            assertEquals(DroolsCode.ANCHOR_FORM_SUBJECT_UNBOUND, e.code(), e.getMessage());
            if (exec == null) exec = e.getMessage();
            else assertEquals(exec, e.getMessage(), "the DRL-text branch refuses in the sentence of the exec-model branch");
        }
    }

    /** The loader reads the anchor for its one value, in the shared loader's sentences: another string, another JSON
     *  value, and an identity check without its required anchor are each a document error at the anchor's pointer. */
    @Test void an_anchor_other_than_form_is_a_document_error_at_its_pointer() throws Exception {
        String read = "/rules/rules/0/versions/0/value/filter/parts/1/right";
        String identity = "/rules/rules/0/versions/0/value/filter/parts/0";
        ObjectNode doc = golden("jutterswet-registervoorrang");
        ((ObjectNode) doc.at(read)).put("anchor", "fold");
        assertDocumentError(doc, read + "/anchor", "unknown anchor value \"fold\"");
        doc = golden("jutterswet-registervoorrang");
        ((ObjectNode) doc.at(read)).put("anchor", true);
        assertDocumentError(doc, read + "/anchor", "an anchor is the string \"form\"");
        doc = golden("jutterswet-registervoorrang");
        ((ObjectNode) doc.at(identity)).remove("anchor");
        assertDocumentError(doc, identity, "anchor");
    }

    private static void assertDocumentError(JsonNode doc, String site, String fragment) {
        dev.uchar.regelspraak.loader.IrLoader.DocumentError e = assertThrows(
                dev.uchar.regelspraak.loader.IrLoader.DocumentError.class,
                () -> dev.uchar.regelspraak.loader.IrLoader.loadAll(doc));
        assertEquals(site, e.site(), e.getMessage());
        assertTrue(e.getMessage().contains(fragment), e.getMessage());
    }

    // ---------------- the rewrite into the anchored spelling ----------------

    /** The two roles of each fact type of {@code doc}, by its name in lower case. */
    private static Map<String, List<String>> factTypes(JsonNode doc) {
        Map<String, List<String>> out = new HashMap<>();
        for (JsonNode ft : doc.at("/model/factTypes")) {
            List<String> roles = new ArrayList<>();
            for (JsonNode r : ft.get("roles")) roles.add(r.path("name").asText());
            out.put(ft.get("name").asText().toLowerCase(), roles);
        }
        return out;
    }

    /** The range of a fold node: its path, or its one step; null without one. */
    private static List<JsonNode> range(JsonNode n) {
        List<JsonNode> out = new ArrayList<>();
        if (n.has("sourcePath")) n.get("sourcePath").forEach(out::add);
        else if (n.has("sourceRole")) out.add(n.get("sourceRole"));
        return out.isEmpty() ? null : out;
    }

    /** Whether {@code step} walks back over {@code last}: the other role of the same fact type. */
    private static boolean reverses(Map<String, List<String>> fts, JsonNode step, JsonNode last) {
        String ft = step.path("factType").asText();
        if (!ft.equals(last.path("factType").asText())) return false;
        List<String> roles = fts.get(ft.toLowerCase());
        if (roles == null || roles.size() != 2) return false;
        String lastRole = last.path("role").asText();
        String other = roles.get(0).equals(lastRole) ? roles.get(1) : roles.get(1).equals(lastRole) ? roles.get(0) : null;
        return step.path("role").asText().equals(other);
    }

    /** Rewrites, under {@code n}, every node in the filter or the test of a fold on the subject of the form ({@code depth}
     *  1) whose first hop or range step walks back over the last range step around it ({@code enclosing}). */
    private static void anchorize(JsonNode n, Map<String, List<String>> fts, List<JsonNode> enclosing, int depth) {
        if (n instanceof ObjectNode o) {
            String node = o.path("node").asText();
            if (!enclosing.isEmpty() && depth == 1) {
                JsonNode last = enclosing.get(enclosing.size() - 1);
                List<JsonNode> r = range(o);
                if ((node.equals("Aggregation") || node.equals("QuantifierFold")) && r != null && reverses(fts, r.get(0), last)) {
                    o.remove("sourcePath");
                    o.remove("sourceRole");
                    if (r.size() == 2) o.set("sourceRole", r.get(1));
                    else if (r.size() > 2) o.putArray("sourcePath").addAll(r.subList(1, r.size()));
                    o.put("anchor", "form");
                }
                if (node.equals("AttributeReference") && o.has("path") && reverses(fts, o.get("path").get(0), last)) {
                    ArrayNode p = (ArrayNode) o.get("path");
                    p.remove(0);
                    if (p.isEmpty()) o.remove("path");
                    o.put("anchor", "form");
                }
            }
            List<String> keys = new ArrayList<>();
            o.fieldNames().forEachRemaining(keys::add);
            for (String k : keys) {
                List<JsonNode> enc = enclosing;
                int d = depth;
                List<JsonNode> r = range(o);
                if ((node.equals("Aggregation") || node.equals("QuantifierFold")) && (k.equals("filter") || k.equals("test"))
                        && r != null) {
                    enc = new ArrayList<>(enclosing);
                    enc.add(r.get(r.size() - 1));
                    d = o.path("anchor").asText().equals("form") || depth == 0 ? 1 : depth + 1;
                }
                anchorize(o.get(k), fts, enc, d);
            }
        } else if (n instanceof ArrayNode a) {
            for (JsonNode x : a) anchorize(x, fts, enclosing, depth);
        }
    }

    /** The nodes under {@code n} that carry the anchor. */
    private static int count(JsonNode n) {
        int out = n.isObject() && n.path("anchor").asText().equals("form") && !n.path("node").asText().equals("IdentityCheck")
                ? 1 : 0;
        for (JsonNode x : n) out += count(x);
        return out;
    }

    private static ObjectNode golden(String name) throws Exception {
        return (ObjectNode) TmsDocuments.JSON.readTree(Files.readString(GOLDEN.resolve(name).resolve("ir.json")));
    }

    private static Path temporary(JsonNode doc) throws Exception {
        Path tmp = Files.createTempFile("anker-getuige", ".json");
        tmp.toFile().deleteOnExit();
        return Files.writeString(tmp, TmsDocuments.JSON.writeValueAsString(doc));
    }

    private static List<Path> cases(Path dir) throws Exception {
        try (Stream<Path> s = Files.list(dir)) {
            return s.filter(p -> p.getFileName().toString().matches("case.*\\.json")).sorted().toList();
        }
    }
}
