package io.github.yhavinga.regelspraak.loader;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.github.yhavinga.regelspraak.cases.IrPad;
import io.github.yhavinga.regelspraak.ir.Conditie;
import io.github.yhavinga.regelspraak.ir.Default;
import io.github.yhavinga.regelspraak.ir.Expr;
import io.github.yhavinga.regelspraak.ir.PrioritizedDefaults;
import io.github.yhavinga.regelspraak.ir.ResolvedConsistentie;
import io.github.yhavinga.regelspraak.ir.ResolvedRegelGroep;
import io.github.yhavinga.regelspraak.model.RegelSpraakModel;

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
class BronrecordWeigeringWitnessTest {

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

    private static final String VERSIE = "/rules/rules/0/versions/0";
    private static final String WAARDE = VERSIE + "/value";
    private static final String VOORWAARDE = VERSIE + "/condition";
    private static final String CONSISTENTIE = "/rules/consistencies/0/versions/0";
    private static final String GROEP = "/rules/ruleGroups/0";

    // ---------------- every node kind reads its record ----------------

    /** The kinds that carry a record in the loadable goldens, each at its first place: {@code bron} in its stead is an
     *  unknown field of the kind, and a file or an empty snippet in the record is refused — so no kind loads its record
     *  and reads past it. The version records carry no discriminator and are named by their form. */
    @Test void elke_knoopsoort_met_een_bronrecord_leest_het_en_kent_geen_bron() throws Exception {
        Map<String, Plek> eerste = new TreeMap<>();
        for (Path ir : goldens()) {
            JsonNode doc = JSON.readTree(Files.readString(ir));
            try { IrLoader.loadAll(doc); } catch (IrLoader.Refusal r) { continue; }
            verzamel(doc, "", null, ir, eerste);
        }
        // 27 kinds, measured 2026-09-24 on the source record of L5: 25 node kinds by their discriminator and the
        // version records of a consistency and a uniqueness; the goldens that carry the rest are refused as forms not
        // carried, and the marker stands in no golden (its rows are below)
        assertTrue(eerste.size() >= 27, () -> "te weinig knoopsoorten met een bronrecord: " + eerste.keySet());
        for (Map.Entry<String, Plek> e : eerste.entrySet()) {
            String soort = e.getKey(), p = e.getValue().pad();
            ObjectNode ir = (ObjectNode) JSON.readTree(Files.readString(e.getValue().ir()));
            ObjectNode knoop = (ObjectNode) ir.at(p);
            String snippet = knoop.get("source").get("snippet").asText();
            knoop.remove("source");
            knoop.put("bron", snippet);
            onbekendVeld(ir, p + "/bron", " on " + soort);
            knoop.putNull("bron");
            onbekendVeld(ir, p + "/bron", " on " + soort);

            ir = (ObjectNode) JSON.readTree(Files.readString(e.getValue().ir()));
            ((ObjectNode) ir.at(p + "/source")).put("file", "jutterswet.rgs");
            onbekendVeld(ir, p + "/source/file", "");
            ir = (ObjectNode) JSON.readTree(Files.readString(e.getValue().ir()));
            ((ObjectNode) ir.at(p + "/source")).put("snippet", "");
            assertTrue(weigert(ir, p + "/source/snippet").getMessage().endsWith(": " + IrLoader.SNIPPET_LEEG), soort);
        }
    }

    /** The kinds that carry a record and stand in no loadable golden, placed at a rule's value or condition with a file
     *  in their record: each reads its record, so the file is refused at the record's pointer. */
    @Test void de_knoopsoorten_zonder_golden_lezen_hun_record_ook() throws Exception {
        String w = WAARDE, v = VOORWAARDE, bronrecord = "\"source\":{\"snippet\":\"x\",\"file\":\"jutterswet.rgs\"}";
        Map<String, String[]> rijen = new java.util.LinkedHashMap<>();
        rijen.put("TekstReeks", new String[] { w, "{\"node\":\"TextComposition\",\"parts\":[],\"type\":{\"node\":\"Text\"},"
                + bronrecord + "}", w });
        rijen.put("VariabeleRef", new String[] { w, "{\"node\":\"VariableReference\",\"name\":\"x\"," + bronrecord + "}", w });
        rijen.put("DimensieSelectie", new String[] { w, "{\"node\":\"Aggregation\",\"kind\":\"sum\",\"arguments\":[],"
                + "\"dimensionSelection\":{\"node\":\"DimensionSelection\",\"dimension\":\"d\",\"labels\":[]," + bronrecord
                + "},\"type\":null,\"source\":{\"snippet\":\"x\"}}", w + "/dimensionSelection" });
        rijen.put("ContextValue", new String[] { w, "{\"node\":\"ContextValue\",\"name\":\"evaluationDate\",\"type\":"
                + "{\"node\":\"Date\",\"precision\":\"day\"}," + bronrecord + "}", w });
        rijen.put("Onopgelost (expressie)", new String[] { w, "{\"node\":\"Unresolved\"," + bronrecord + "}", w });
        rijen.put("Regelversie", new String[] { v, "{\"node\":\"Regelversie\",\"regel\":\"x\",\"inconsistent\":false,"
                + bronrecord + "}", v });
        rijen.put("Onopgelost (conditie)", new String[] { v, "{\"node\":\"Unresolved\"," + bronrecord + "}", v });
        for (Map.Entry<String, String[]> r : rijen.entrySet()) {
            ObjectNode ir = golden(PENNINGDREMPEL);
            zet(ir, r.getValue()[0], r.getValue()[1]);
            onbekendVeld(ir, r.getValue()[2] + "/source/file", "");
        }
    }

    /** The line went into the record: beside it, on the two nodes that carried it, it is a field they do not have. */
    @Test void een_regel_naast_het_record_is_een_onbekend_veld() throws Exception {
        ObjectNode ir = golden(WACHTERSKETEN);
        ((ObjectNode) ir.at(CONSISTENTIE)).put("line", 1);
        onbekendVeld(ir, CONSISTENTIE + "/line", " on ConsistencyVersion");
        ir = golden(WACHTERSKETEN);
        ((ObjectNode) ir.at(GROEP)).put("line", 5);
        onbekendVeld(ir, GROEP + "/line", " on ResolvedRegelGroep");
        ir = golden(WACHTERSKETEN);
        ((ObjectNode) ir.at(GROEP)).set("source", IrPad.bronrecord("berging").put("line", 5));
        ((ObjectNode) ir.at(GROEP)).put("line", 5);
        onbekendVeld(ir, GROEP + "/line", " on ResolvedRegelGroep");
    }

    // ---------------- the record's own form ----------------

    /** At a literal and at a consistency version, the two kinds of place: a record that is no object, null, without
     *  its snippet, with a position, with a snippet or a line of the wrong type, each at its pointer. */
    @Test void het_record_heeft_de_vorm_van_het_schema() throws Exception {
        String[][] plekken = { { PREMIEVENSTER, WAARDE }, { WACHTERSKETEN, CONSISTENTIE } };
        for (String[] plek : plekken) {
            String p = plek[1];
            // the record, its value, the pointer of the refusal, and the end of its sentence
            String[][] rijen = {
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
                    { "{\"snippet\": \"\"}", p + "/source/snippet", IrLoader.SNIPPET_LEEG },
                    { "{\"snippet\": \"x\", \"line\": \"3\"}", p + "/source/line", "expected an integer, got \"3\"" },
                    { "{\"snippet\": \"x\", \"line\": 1.5}", p + "/source/line", "expected an integer, got 1.5" },
                    { "{\"snippet\": \"x\", \"line\": null}", p + "/source/line",
                            "an optional field is written whole or not at all, and null is neither" },
                    { "{\"snippet\": \"x\", \"line\": 0}", p + "/source/line", IrLoader.BRONREGEL_ONDER_1 },
                    { "{\"snippet\": \"x\", \"line\": -1}", p + "/source/line", IrLoader.BRONREGEL_ONDER_1 } };
            for (String[] rij : rijen) {
                ObjectNode ir = golden(plek[0]);
                ((ObjectNode) ir.at(p)).set("source", JSON.readTree(rij[0]));
                IrLoader.Documentfout f = weigert(ir, rij[1]);
                assertTrue(f.getMessage().endsWith(": " + rij[2]), rij[0] + " at " + p + ": " + f.getMessage());
            }
        }
    }

    // ---------------- the marker ----------------

    /** WF-113 (b)'s arm for a marker without a source, in the verifier's sentence at the marker itself: an executor has
     *  no text to name in a report, so the document is refused at load, for the expression marker (with or without a
     *  type) and the condition marker alike. A marker with a record loads — a blank snippet included, which is the
     *  verifier's to refuse (its blank arm), since the schema admits it. */
    @Test void een_marker_zonder_bronrecord_weigert_in_de_zin_van_wf113() throws Exception {
        String[][] markers = {
                { PREMIEVENSTER, WAARDE, "{\"node\":\"Unresolved\"}" },
                { PREMIEVENSTER, WAARDE, "{\"node\":\"Unresolved\",\"type\":{\"node\":\"Number\",\"whole\":true,"
                        + "\"decimals\":0,\"sign\":\"unspecified\"}}" },
                { PENNINGDREMPEL, VOORWAARDE, "{\"node\":\"Unresolved\"}" } };
        for (String[] m : markers) {
            ObjectNode ir = golden(m[0]);
            zet(ir, m[1], m[2]);
            IrLoader.Documentfout f = weigert(ir, m[1]);
            assertTrue(f.getMessage().endsWith(": " + IrLoader.MARKER_ZONDER_BRON), m[2] + ": " + f.getMessage());
            for (String snippet : List.of("de onbekende vondstwaarde", " ")) {
                ObjectNode met = (ObjectNode) JSON.readTree(m[2]);
                met.set("source", IrPad.bronrecord(snippet));
                ir = golden(m[0]);
                zet(ir, m[1], met.toString());
                IrLoader.loadAll(ir);
            }
        }
        assertEquals("Unresolved marker without a source: no diagnostic message can contain it, so it cannot be"
                + " accounted for", IrLoader.MARKER_ZONDER_BRON);
        assertEquals("a source snippet is never empty: a node without source text carries no source",
                IrLoader.SNIPPET_LEEG);
        assertEquals("a source line is at least 1", IrLoader.BRONREGEL_ONDER_1);
        // the marker reads the snippet as any other node does
        ObjectNode ir = golden(PENNINGDREMPEL);
        zet(ir, VOORWAARDE, "{\"node\":\"Unresolved\",\"source\":{\"snippet\":\"de onbekende vondstwaarde\"}}");
        Conditie.Onopgelost o = assertInstanceOf(Conditie.Onopgelost.class,
                IrLoader.load(ir).resolvedRegels.get(0).voorwaarde());
        assertEquals("de onbekende vondstwaarde", o.bron().snippet());
    }

    // ---------------- what the records hold ----------------

    @Test void de_snippet_is_het_anker_van_het_record_en_een_regel_elders_valt_weg() throws Exception {
        ObjectNode ir = golden(PREMIEVENSTER);
        String snippet = ir.at(WAARDE + "/source/snippet").asText();
        Expr.Literal l = (Expr.Literal) IrLoader.load(ir).resolvedRegels.get(0).waarde();
        assertEquals(snippet, l.bron().snippet());
        assertNull(l.bron().file(), "the wire carries no file");
        assertEquals(0, l.bron().line());
        // a line on a node that keeps none is admitted by the record and dropped
        ((ObjectNode) ir.at(WAARDE + "/source")).put("line", 7);
        l = (Expr.Literal) IrLoader.load(ir).resolvedRegels.get(0).waarde();
        assertEquals(snippet, l.bron().snippet());
        assertEquals(0, l.bron().line());
        // a node without a record holds the empty anchor, as a null bron did before the record landed
        ((ObjectNode) ir.at(WAARDE)).remove("source");
        l = (Expr.Literal) IrLoader.load(ir).resolvedRegels.get(0).waarde();
        assertEquals("", l.bron().snippet());
    }

    @Test void de_regel_gaat_naar_de_consistentie_en_de_regelgroep() throws Exception {
        List<ResolvedConsistentie> keur = IrLoader.load(golden(KEURSPELLING)).consistenties;
        assertEquals(List.of(30, 34), keur.stream().map(ResolvedConsistentie::line).toList());

        ObjectNode ir = golden(WACHTERSKETEN);
        RegelSpraakModel m = IrLoader.load(ir);
        assertEquals(1, m.consistenties.get(0).line());
        assertEquals(ir.at(CONSISTENTIE + "/source/snippet").asText(), m.consistenties.get(0).bron().snippet());
        ResolvedRegelGroep g = m.regelGroepen.get(0);
        assertEquals(0, g.line(), "a rule group without a record has no line");
        assertEquals("", g.bron().snippet());

        ((ObjectNode) ir.at(CONSISTENTIE + "/source")).remove("line");
        ((ObjectNode) ir.at(GROEP)).set("source", IrPad.bronrecord("berging").put("line", 5));
        m = IrLoader.load(ir);
        assertEquals(0, m.consistenties.get(0).line(), "a record without a line: the records' 0");
        assertEquals(5, m.regelGroepen.get(0).line());
        assertEquals("berging", m.regelGroepen.get(0).bron().snippet());

        ((ObjectNode) ir.at(CONSISTENTIE)).remove("source");
        m = IrLoader.load(ir);
        assertEquals(0, m.consistenties.get(0).line());
        assertEquals("", m.consistenties.get(0).bron().snippet());
    }

    @Test void de_defaultslaag_houdt_de_brontekst_als_string() throws Exception {
        ObjectNode ir = golden(TOELAGEGRONDEN);
        PrioritizedDefaults g = IrLoader.loadAll(ir).prioritizedDefaults().get(0);
        JsonNode wire = ir.at("/rules/prioritizedDefaults/0");
        assertEquals(wire.at("/source/snippet").asText(), g.bron());
        assertEquals(wire.at("/defaults/0/source/snippet").asText(), g.defaults().get(0).bron());
        for (PrioritizedDefaults zonder : IrLoader.loadAll(golden(WACHTGELD)).prioritizedDefaults()) {
            assertNull(zonder.bron(), zonder.naam());
            for (Default d : zonder.defaults()) assertNull(d.bron(), zonder.naam());
        }
    }

    // ---------------- helpers ----------------

    /** Every object with a record, under its kind: the discriminator, or for a version record its form's
     *  discriminator and "Version", as the loader names it. */
    private static void verzamel(JsonNode n, String pad, String vorm, Path ir, Map<String, Plek> eerste) {
        if (n instanceof ObjectNode o) {
            JsonNode soort = o.get("node");
            if (o.has("source"))
                eerste.putIfAbsent(soort != null ? soort.asText() : vorm + "Version", new Plek(ir, pad));
            String binnen = soort != null && o.path("versions").isArray() ? soort.asText() : null;
            o.properties().forEach(e -> {
                if (!e.getKey().equals("source"))
                    verzamel(e.getValue(), pad + "/" + e.getKey().replace("~", "~0").replace("/", "~1"),
                            e.getKey().equals("versions") ? binnen : null, ir, eerste);
            });
        } else if (n instanceof ArrayNode a) {
            for (int i = 0; i < a.size(); i++) verzamel(a.get(i), pad + "/" + i, vorm, ir, eerste);
        }
    }

    private record Plek(Path ir, String pad) {}

    private static List<Path> goldens() throws Exception {
        try (Stream<Path> s = Files.walk(CORPUS)) {
            List<Path> uit = s.filter(p -> p.getFileName().toString().equals("ir.json")).sorted().toList();
            if (uit.isEmpty()) fail("geen enkele golden gevonden onder " + CORPUS.toAbsolutePath());
            return new ArrayList<>(uit);
        }
    }

    private static void zet(ObjectNode ir, String pad, String json) throws Exception {
        int slash = pad.lastIndexOf('/');
        JsonNode ouder = ir.at(pad.substring(0, slash));
        if (!(ouder instanceof ObjectNode o)) throw new AssertionError("no object at " + pad.substring(0, slash));
        o.set(pad.substring(slash + 1), JSON.readTree(json));
    }

    private static IrLoader.Documentfout weigert(JsonNode ir, String plek) {
        IrLoader.Documentfout e = assertThrows(IrLoader.Documentfout.class, () -> IrLoader.loadAll(ir),
                "verwacht een documentfout op " + plek);
        assertEquals(plek, e.plek(), e.getMessage());
        return e;
    }

    private static void onbekendVeld(JsonNode ir, String plek, String knoop) {
        IrLoader.Documentfout e = weigert(ir, plek);
        assertTrue(e.getMessage().endsWith(": unknown field" + knoop), e.getMessage());
    }

    private static ObjectNode golden(String naam) throws Exception {
        Path ir = CORPUS.resolve(naam).resolve("ir.json");
        assertTrue(Files.exists(ir), "golden ir.json present: " + ir.toAbsolutePath());
        ObjectNode doc = (ObjectNode) JSON.readTree(Files.readString(ir));
        IrLoader.loadAll(doc);   // the untwisted golden loads, so the refusal below is the twist's alone
        return doc;
    }
}
