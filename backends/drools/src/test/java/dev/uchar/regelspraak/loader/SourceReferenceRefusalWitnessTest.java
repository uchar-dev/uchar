package dev.uchar.regelspraak.loader;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import dev.uchar.regelspraak.cases.IrPath;
import dev.uchar.regelspraak.ir.Conditie;
import dev.uchar.regelspraak.ir.Default;
import dev.uchar.regelspraak.ir.Expr;
import dev.uchar.regelspraak.ir.PrioritizedDefaults;
import dev.uchar.regelspraak.ir.ResolvedConsistentie;
import dev.uchar.regelspraak.ir.ResolvedRegelGroep;
import dev.uchar.regelspraak.model.RegelSpraakModel;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * The loader on the source record (L5 of the v1 wire migration, delta B26): what it refuses by name, and how it lays
 * the record back onto the frontend records, which keep their v0 components. Refused: the field the record replaced,
 * {@code bron}, on every node that carries a record, and a {@code line} beside the record on the two nodes that kept
 * one; a record that is no object, null, without its snippet, with a file or a column, with a snippet that is not a
 * string or is empty, and with a line that is no integer or is below 1 — the empty snippet and the line in the shared
 * OCaml loader's sentences. And, as the shared loader refuses it when it reads for an executor, a marker without a
 * record, in WF-113 (b)'s sentence. Read: the snippet into the record's anchor, "" where a node carries no record;
 * the line into a consistency version's and a rule group's line, 0 where the record has none; a line elsewhere is
 * checked and dropped; the defaults layer's source text as the string its records keep, null where there is none.
 *
 * <p>Every fixture is a corpus golden with ONE twist in memory; the corpus file is read and never written. Each
 * refusal is an {@link IrLoader.Documentfout} at a JSON pointer, never an {@link IrLoader.Refusal}: every twisted
 * document here is ill-formed.
 */
class SourceReferenceRefusalWitnessTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    /** cwd under surefire is the module dir (backends/drools); the corpus is two levels up. */
    private static final Path CORPUS = Path.of("..", "..", "corpus", "golden");

    /** Rule 0 computes an attribute of a Vondst from a Number literal, which carries a source record. */
    private static final String PREMIEVENSTER = "jutterswet-premievenster";
    /** Rule 0 attributes the characteristic penninggerechtigd of a Jutter, under a condition. */
    private static final String PENNINGDREMPEL = "jutterswet-penningdrempel";
    /** Consistency 0 carries a source record with line 1; rule group 0 carries no record. */
    private static final String WACHTERSKETEN = "anchor/jutterswet-wachtersketen";
    /** An emitter golden: consistencies 0 and 1 carry the lines 30 and 34. */
    private static final String KEURSPELLING = "jutterswet-keurspelling";
    /** Six defaults groups whose group and defaults each carry a source record. */
    private static final String TOELAGEGRONDEN = "anchor/jutterswet-toelagegronden";
    /** Three defaults groups, none of which carries a source record. */
    private static final String WACHTGELD = "jutterswet-wachtgeld";

    private static final String VERSION = "/rules/rules/0/versions/0";
    private static final String VALUE = VERSION + "/value";
    private static final String CONDITION = VERSION + "/condition";
    private static final String CONSISTENCY = "/rules/consistencies/0/versions/0";
    private static final String GROUP = "/rules/ruleGroups/0";

    // ---------------- every node kind reads its record ----------------

    /** The kinds that carry a record in the loadable goldens, each at its first place: {@code bron} in its stead is an
     *  unknown field of the kind, and a file or an empty snippet in the record is refused — so no kind loads its record
     *  and reads past it. The version records carry no discriminator and are named by their form. */
    @Test void each_node_kind_with_a_source_reference_reads_it_and_knows_no_source() throws Exception {
        Map<String, Site> first = new TreeMap<>();
        for (Path ir : goldens()) {
            JsonNode doc = JSON.readTree(Files.readString(ir));
            try { IrLoader.loadAll(doc); } catch (IrLoader.Refusal r) { continue; }
            collect(doc, "", null, ir, first);
        }
        // 27 kinds, measured 2026-09-24 on the source record of L5: 25 node kinds by their discriminator and the
        // version records of a consistency and a uniqueness; the goldens that carry the rest are refused as forms not
        // carried, and the marker stands in no golden (its rows are below)
        assertTrue(first.size() >= 27, () -> "te weinig knoopsoorten met een bronrecord: " + first.keySet());
        for (Map.Entry<String, Site> e : first.entrySet()) {
            String kind = e.getKey(), p = e.getValue().path();
            ObjectNode ir = (ObjectNode) JSON.readTree(Files.readString(e.getValue().ir()));
            ObjectNode node = (ObjectNode) ir.at(p);
            String snippet = node.get("source").get("snippet").asText();
            node.remove("source");
            node.put("bron", snippet);
            unknownField(ir, p + "/bron", " on " + kind);
            node.putNull("bron");
            unknownField(ir, p + "/bron", " on " + kind);

            ir = (ObjectNode) JSON.readTree(Files.readString(e.getValue().ir()));
            ((ObjectNode) ir.at(p + "/source")).put("file", "jutterswet.rgs");
            unknownField(ir, p + "/source/file", "");
            ir = (ObjectNode) JSON.readTree(Files.readString(e.getValue().ir()));
            ((ObjectNode) ir.at(p + "/source")).put("snippet", "");
            assertTrue(refuses(ir, p + "/source/snippet").getMessage().endsWith(": " + IrLoader.SNIPPET_EMPTY), kind);
        }
    }

    /** The kinds that carry a record and stand in no loadable golden, placed at a rule's value or condition with a file
     *  in their record: each reads its record, so the file is refused at the record's pointer. */
    @Test void the_node_kinds_without_a_golden_also_read_their_record() throws Exception {
        String w = VALUE, v = CONDITION, sourceReference = "\"source\":{\"snippet\":\"x\",\"file\":\"jutterswet.rgs\"}";
        Map<String, String[]> rows = new java.util.LinkedHashMap<>();
        rows.put("TextComposition", new String[] { w, "{\"node\":\"TextComposition\",\"parts\":[],\"type\":{\"node\":\"Text\"},"
                + sourceReference + "}", w });
        rows.put("VariableReference", new String[] { w, "{\"node\":\"VariableReference\",\"name\":\"x\"," + sourceReference + "}", w });
        rows.put("DimensionSelection", new String[] { w, "{\"node\":\"Aggregation\",\"kind\":\"sum\",\"arguments\":[],"
                + "\"dimensionSelection\":{\"node\":\"DimensionSelection\",\"dimension\":\"d\",\"labels\":[]," + sourceReference
                + "},\"type\":null,\"source\":{\"snippet\":\"x\"}}", w + "/dimensionSelection" });
        rows.put("ContextValue", new String[] { w, "{\"node\":\"ContextValue\",\"name\":\"evaluationDate\",\"type\":"
                + "{\"node\":\"Date\",\"precision\":\"day\"}," + sourceReference + "}", w });
        rows.put("Unresolved (expression)", new String[] { w, "{\"node\":\"Unresolved\"," + sourceReference + "}", w });
        rows.put("Regelversie", new String[] { v, "{\"node\":\"Regelversie\",\"regel\":\"x\",\"inconsistent\":false,"
                + sourceReference + "}", v });
        rows.put("Unresolved (condition)", new String[] { v, "{\"node\":\"Unresolved\"," + sourceReference + "}", v });
        for (Map.Entry<String, String[]> r : rows.entrySet()) {
            ObjectNode ir = golden(PENNINGDREMPEL);
            set(ir, r.getValue()[0], r.getValue()[1]);
            unknownField(ir, r.getValue()[2] + "/source/file", "");
        }
    }

    /** The line went into the record: beside it, on the two nodes that carried it, it is a field they do not have. */
    @Test void a_line_beside_the_record_is_an_unknown_field() throws Exception {
        ObjectNode ir = golden(WACHTERSKETEN);
        ((ObjectNode) ir.at(CONSISTENCY)).put("line", 1);
        unknownField(ir, CONSISTENCY + "/line", " on ConsistencyVersion");
        ir = golden(WACHTERSKETEN);
        ((ObjectNode) ir.at(GROUP)).put("line", 5);
        unknownField(ir, GROUP + "/line", " on RuleGroup");
        ir = golden(WACHTERSKETEN);
        ((ObjectNode) ir.at(GROUP)).set("source", IrPath.sourceReference("berging").put("line", 5));
        ((ObjectNode) ir.at(GROUP)).put("line", 5);
        unknownField(ir, GROUP + "/line", " on RuleGroup");
    }

    // ---------------- the record's own form ----------------

    /** At a literal and at a consistency version, the two kinds of place: a record that is no object, null, without
     *  its snippet, with a position, with a snippet or a line of the wrong type, each at its pointer. */
    @Test void the_record_has_the_form_of_the_schema() throws Exception {
        String[][] sites = { { PREMIEVENSTER, VALUE }, { WACHTERSKETEN, CONSISTENCY } };
        for (String[] site : sites) {
            String p = site[1];
            // the record, its value, the pointer of the refusal, and the end of its sentence
            String[][] rows = {
                    { "\"de waarde\"", p + "/source", "expected a JSON object, got \"de waarde\"" },
                    { "null", p + "/source", "an optional field is written whole or not at all, and null is neither" },
                    { "[]", p + "/source", "expected a JSON object, got []" },
                    { "{}", p + "/source", "missing field 'snippet'" },
                    { "{\"line\": 3}", p + "/source", "missing field 'snippet'" },
                    { "{\"snippet\": \"x\", \"file\": \"jutterswet.rgs\"}", p + "/source/file", "unknown field" },
                    { "{\"snippet\": \"x\", \"column\": 4}", p + "/source/column", "unknown field" },
                    { "{\"snippet\": \"x\", \"node\": \"Source\"}", p + "/source/node", "unknown field" },
                    { "{\"snippet\": 5}", p + "/source/snippet", "expected a string, got 5" },
                    { "{\"snippet\": null}", p + "/source/snippet", "expected a string, got null" },
                    { "{\"snippet\": \"\"}", p + "/source/snippet", IrLoader.SNIPPET_EMPTY },
                    { "{\"snippet\": \"x\", \"line\": \"3\"}", p + "/source/line", "expected an integer, got \"3\"" },
                    { "{\"snippet\": \"x\", \"line\": 1.5}", p + "/source/line", "expected an integer, got 1.5" },
                    { "{\"snippet\": \"x\", \"line\": null}", p + "/source/line",
                            "an optional field is written whole or not at all, and null is neither" },
                    { "{\"snippet\": \"x\", \"line\": 0}", p + "/source/line", IrLoader.SOURCE_LINE_UNDER_1 },
                    { "{\"snippet\": \"x\", \"line\": -1}", p + "/source/line", IrLoader.SOURCE_LINE_UNDER_1 } };
            for (String[] row : rows) {
                ObjectNode ir = golden(site[0]);
                ((ObjectNode) ir.at(p)).set("source", JSON.readTree(row[0]));
                IrLoader.Documentfout f = refuses(ir, row[1]);
                assertTrue(f.getMessage().endsWith(": " + row[2]), row[0] + " at " + p + ": " + f.getMessage());
            }
        }
    }

    // ---------------- the marker ----------------

    /** WF-113 (b)'s arm for a marker without a source, in the verifier's sentence at the marker itself: an executor has
     *  no text to name in a report, so the document is refused at load, for the expression marker (with or without a
     *  type) and the condition marker alike. A marker with a record loads — a blank snippet included, which is the
     *  verifier's to refuse (its blank arm), since the schema admits it. */
    @Test void a_marker_without_a_source_reference_is_refused_in_the_sentence_of_wf113() throws Exception {
        String[][] markers = {
                { PREMIEVENSTER, VALUE, "{\"node\":\"Unresolved\"}" },
                { PREMIEVENSTER, VALUE, "{\"node\":\"Unresolved\",\"type\":{\"node\":\"Number\",\"whole\":true,"
                        + "\"decimals\":0,\"sign\":\"unspecified\"}}" },
                { PENNINGDREMPEL, CONDITION, "{\"node\":\"Unresolved\"}" } };
        for (String[] m : markers) {
            ObjectNode ir = golden(m[0]);
            set(ir, m[1], m[2]);
            IrLoader.Documentfout f = refuses(ir, m[1]);
            assertTrue(f.getMessage().endsWith(": " + IrLoader.MARKER_WITHOUT_SOURCE), m[2] + ": " + f.getMessage());
            for (String snippet : List.of("de onbekende vondstwaarde", " ")) {
                ObjectNode with = (ObjectNode) JSON.readTree(m[2]);
                with.set("source", IrPath.sourceReference(snippet));
                ir = golden(m[0]);
                set(ir, m[1], with.toString());
                IrLoader.loadAll(ir);
            }
        }
        assertEquals("Unresolved marker without a source: no diagnostic message can contain it, so it cannot be"
                + " accounted for", IrLoader.MARKER_WITHOUT_SOURCE);
        assertEquals("a source snippet is never empty: a node without source text carries no source",
                IrLoader.SNIPPET_EMPTY);
        assertEquals("a source line is at least 1", IrLoader.SOURCE_LINE_UNDER_1);
        // the marker reads the snippet as any other node does
        ObjectNode ir = golden(PENNINGDREMPEL);
        set(ir, CONDITION, "{\"node\":\"Unresolved\",\"source\":{\"snippet\":\"de onbekende vondstwaarde\"}}");
        Conditie.Onopgelost o = assertInstanceOf(Conditie.Onopgelost.class,
                IrLoader.load(ir).resolvedRegels.get(0).voorwaarde());
        assertEquals("de onbekende vondstwaarde", o.bron().snippet());
    }

    // ---------------- what the records hold ----------------

    @Test void the_snippet_is_the_anchor_of_the_record_and_a_line_elsewhere_falls_away() throws Exception {
        ObjectNode ir = golden(PREMIEVENSTER);
        String snippet = ir.at(VALUE + "/source/snippet").asText();
        Expr.Literal l = (Expr.Literal) IrLoader.load(ir).resolvedRegels.get(0).waarde();
        assertEquals(snippet, l.bron().snippet());
        assertNull(l.bron().file(), "the wire carries no file");
        assertEquals(0, l.bron().line());
        // a line on a node that keeps none is admitted by the record and dropped
        ((ObjectNode) ir.at(VALUE + "/source")).put("line", 7);
        l = (Expr.Literal) IrLoader.load(ir).resolvedRegels.get(0).waarde();
        assertEquals(snippet, l.bron().snippet());
        assertEquals(0, l.bron().line());
        // a node without a record holds the empty anchor, as a null bron did before the record landed
        ((ObjectNode) ir.at(VALUE)).remove("source");
        l = (Expr.Literal) IrLoader.load(ir).resolvedRegels.get(0).waarde();
        assertEquals("", l.bron().snippet());
    }

    @Test void the_rule_goes_to_the_consistency_and_the_rule_group() throws Exception {
        List<ResolvedConsistentie> check = IrLoader.load(golden(KEURSPELLING)).consistenties;
        assertEquals(List.of(30, 34), check.stream().map(ResolvedConsistentie::line).toList());

        ObjectNode ir = golden(WACHTERSKETEN);
        RegelSpraakModel m = IrLoader.load(ir);
        assertEquals(1, m.consistenties.get(0).line());
        assertEquals(ir.at(CONSISTENCY + "/source/snippet").asText(), m.consistenties.get(0).bron().snippet());
        ResolvedRegelGroep g = m.regelGroepen.get(0);
        assertEquals(0, g.line(), "a rule group without a record has no line");
        assertEquals("", g.bron().snippet());

        ((ObjectNode) ir.at(CONSISTENCY + "/source")).remove("line");
        ((ObjectNode) ir.at(GROUP)).set("source", IrPath.sourceReference("berging").put("line", 5));
        m = IrLoader.load(ir);
        assertEquals(0, m.consistenties.get(0).line(), "a record without a line: the records' 0");
        assertEquals(5, m.regelGroepen.get(0).line());
        assertEquals("berging", m.regelGroepen.get(0).bron().snippet());

        ((ObjectNode) ir.at(CONSISTENCY)).remove("source");
        m = IrLoader.load(ir);
        assertEquals(0, m.consistenties.get(0).line());
        assertEquals("", m.consistenties.get(0).bron().snippet());
    }

    @Test void the_defaults_layer_keeps_the_source_text_as_a_string() throws Exception {
        ObjectNode ir = golden(TOELAGEGRONDEN);
        PrioritizedDefaults g = IrLoader.loadAll(ir).prioritizedDefaults().get(0);
        JsonNode wire = ir.at("/rules/prioritizedDefaults/0");
        assertEquals(wire.at("/source/snippet").asText(), g.bron());
        assertEquals(wire.at("/defaults/0/source/snippet").asText(), g.defaults().get(0).bron());
        for (PrioritizedDefaults withoutSource : IrLoader.loadAll(golden(WACHTGELD)).prioritizedDefaults()) {
            assertNull(withoutSource.bron(), withoutSource.naam());
            for (Default d : withoutSource.defaults()) assertNull(d.bron(), withoutSource.naam());
        }
    }

    // ---------------- helpers ----------------

    /** Every object with a record, under its kind: the discriminator, or for a version record its form's
     *  discriminator and "Version", as the loader names it. */
    private static void collect(JsonNode n, String path, String form, Path ir, Map<String, Site> first) {
        if (n instanceof ObjectNode o) {
            JsonNode kind = o.get("node");
            if (o.has("source"))
                first.putIfAbsent(kind != null ? kind.asText() : form + "Version", new Site(ir, path));
            String within = kind != null && o.path("versions").isArray() ? kind.asText() : null;
            o.properties().forEach(e -> {
                if (!e.getKey().equals("source"))
                    collect(e.getValue(), path + "/" + e.getKey().replace("~", "~0").replace("/", "~1"),
                            e.getKey().equals("versions") ? within : null, ir, first);
            });
        } else if (n instanceof ArrayNode a) {
            for (int i = 0; i < a.size(); i++) collect(a.get(i), path + "/" + i, form, ir, first);
        }
    }

    private record Site(Path ir, String path) {}

    private static List<Path> goldens() throws Exception {
        try (Stream<Path> s = Files.walk(CORPUS)) {
            List<Path> out = s.filter(p -> p.getFileName().toString().equals("ir.json")).sorted().toList();
            if (out.isEmpty()) fail("geen enkele golden gevonden onder " + CORPUS.toAbsolutePath());
            return new ArrayList<>(out);
        }
    }

    private static void set(ObjectNode ir, String path, String json) throws Exception {
        int slash = path.lastIndexOf('/');
        JsonNode parent = ir.at(path.substring(0, slash));
        if (!(parent instanceof ObjectNode o)) throw new AssertionError("no object at " + path.substring(0, slash));
        o.set(path.substring(slash + 1), JSON.readTree(json));
    }

    private static IrLoader.Documentfout refuses(JsonNode ir, String site) {
        IrLoader.Documentfout e = assertThrows(IrLoader.Documentfout.class, () -> IrLoader.loadAll(ir),
                "verwacht een documentfout op " + site);
        assertEquals(site, e.site(), e.getMessage());
        return e;
    }

    private static void unknownField(JsonNode ir, String site, String node) {
        IrLoader.Documentfout e = refuses(ir, site);
        assertTrue(e.getMessage().endsWith(": unknown field" + node), e.getMessage());
    }

    private static ObjectNode golden(String name) throws Exception {
        Path ir = CORPUS.resolve(name).resolve("ir.json");
        assertTrue(Files.exists(ir), "golden ir.json present: " + ir.toAbsolutePath());
        ObjectNode doc = (ObjectNode) JSON.readTree(Files.readString(ir));
        IrLoader.loadAll(doc);   // the untwisted golden loads, so the refusal below is the twist's alone
        return doc;
    }
}
