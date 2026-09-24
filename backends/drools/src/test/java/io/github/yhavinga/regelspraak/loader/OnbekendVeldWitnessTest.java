package io.github.yhavinga.regelspraak.loader;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Every node kind this loader reads refuses a field the schema does not give it, at its JSON pointer. The sweep takes
 * each node kind the corpus goldens carry, in the first golden that loads and carries it, adds one field no node has,
 * and asks for the {@link IrLoader.Documentfout} naming that field. A kind whose twisted document still loads is read
 * past; the list of such kinds must be empty.
 *
 * <p>Before L2's fix round most rule-side node kinds — the fold, the quantifier fold, the verdeling and a union's
 * selection among them, and the condition marker, which also admitted a {@code type} — loaded with the unknown field
 * and ignored it. The sweep reaches 60 kinds (measured 2026-09-24). Fifteen more occur only in goldens this backend
 * refuses as forms it does not carry (the list, sum, struct and function forms, {@code TemporalRead}, {@code Match}),
 * so their document is refused before the node is read; the kinds this backend reads that no loadable golden carries
 * have rows of their own below.
 */
class OnbekendVeldWitnessTest {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Path CORPUS = Path.of("..", "..", "corpus", "golden");
    private static final String VREEMD = "uchar-onbekend-veld";

    /** A golden with one rule whose value and condition the rows below replace. Both branches PLAYED. */
    private static final String PENNINGDREMPEL = "jutterswet-penningdrempel";
    /** An object creation carrying its verbindendeRol. Both branches PLAYED. */
    private static final String WACHTERSKETEN = "anchor/jutterswet-wachtersketen";

    @Test void elke_gelezen_knoopsoort_weigert_een_onbekend_veld_bij_naam() throws Exception {
        Map<String, Plek> eerste = new TreeMap<>();
        for (Path ir : goldens()) {
            JsonNode doc = JSON.readTree(Files.readString(ir));
            try { IrLoader.loadAll(doc); } catch (IrLoader.Refusal r) { continue; }
            verzamel(doc, "", ir, eerste);
        }
        // 60 kinds, measured 2026-09-24; the goldens that carry the rest are refused as forms not carried
        assertTrue(eerste.size() >= 60, () -> "te weinig knoopsoorten gevonden: " + eerste.keySet());
        List<String> doorgelaten = new ArrayList<>();
        for (Map.Entry<String, Plek> e : eerste.entrySet()) {
            Plek plek = e.getValue();
            JsonNode doc = JSON.readTree(Files.readString(plek.ir()));
            ((ObjectNode) doc.at(plek.pad())).put(VREEMD, 1);
            try {
                IrLoader.loadAll(doc);
                doorgelaten.add(e.getKey() + " (" + naam(plek.ir()) + " " + plek.pad() + ")");
            } catch (IrLoader.Documentfout f) {
                assertEquals(plek.pad() + "/" + VREEMD, f.plek(), e.getKey() + ": " + f.getMessage());
                assertTrue(f.getMessage().contains("unknown field"), e.getKey() + ": " + f.getMessage());
            }
        }
        assertEquals(List.of(), doorgelaten, "een knoopsoort las een veld dat het schema haar niet geeft stil voorbij");
    }

    /** The node kinds this backend reads that no loadable golden carries, placed at a rule's value or condition
     *  with the stray field. The key check runs before anything else of the node is read, so the rest of each
     *  node only has to be well-formed. */
    @Test void de_knoopsoorten_zonder_golden_weigeren_een_onbekend_veld_ook() throws Exception {
        String w = "/rules/resolvedRegels/0/waarde", v = "/rules/resolvedRegels/0/voorwaarde";
        String literal = "{\"node\":\"Literal\",\"waarde\":\"x\",\"type\":{\"node\":\"Tekst\"},\"bron\":\"x\"}";
        Map<String, String[]> rijen = new java.util.LinkedHashMap<>();
        rijen.put("TekstReeks", new String[] { w, "{\"node\":\"TekstReeks\",\"delen\":[],\"type\":{\"node\":\"Tekst\"},"
                + "\"bron\":\"x\",\"" + VREEMD + "\":1}", w });
        rijen.put("Letterlijk", new String[] { w, "{\"node\":\"TekstReeks\",\"delen\":[{\"node\":\"Letterlijk\","
                + "\"tekst\":\"x\",\"" + VREEMD + "\":1}],\"type\":{\"node\":\"Tekst\"},\"bron\":\"x\"}",
                w + "/delen/0" });
        rijen.put("Ingevoegd", new String[] { w, "{\"node\":\"TekstReeks\",\"delen\":[{\"node\":\"Ingevoegd\",\"expr\":"
                + literal + ",\"" + VREEMD + "\":1}],\"type\":{\"node\":\"Tekst\"},\"bron\":\"x\"}", w + "/delen/0" });
        rijen.put("VariabeleRef", new String[] { w, "{\"node\":\"VariabeleRef\",\"naam\":\"x\",\"bron\":\"x\",\""
                + VREEMD + "\":1}", w });
        rijen.put("Onopgelost (expressie)", new String[] { w, "{\"node\":\"Onopgelost\",\"bron\":\"x\",\"" + VREEMD
                + "\":1}", w });
        rijen.put("DimensieSelectie", new String[] { w, "{\"node\":\"Functie\",\"kind\":\"SOM\",\"argumenten\":[],"
                + "\"elementObjecttype\":null,\"leegwaarde\":null,\"filter\":null,\"dimensieSelectie\":{\"node\":"
                + "\"DimensieSelectie\",\"dimensie\":\"d\",\"labels\":[],\"bron\":\"x\",\"" + VREEMD + "\":1},"
                + "\"type\":null,\"bron\":\"x\",\"granulariteit\":null}", w + "/dimensieSelectie" });
        rijen.put("Regelversie", new String[] { v, "{\"node\":\"Regelversie\",\"regel\":\"x\",\"inconsistent\":false,"
                + "\"bron\":\"x\",\"" + VREEMD + "\":1}", v });
        rijen.put("Onopgelost (conditie)", new String[] { v, "{\"node\":\"Onopgelost\",\"bron\":\"x\",\"" + VREEMD
                + "\":1}", v });
        for (Map.Entry<String, String[]> r : rijen.entrySet()) {
            JsonNode doc = golden(PENNINGDREMPEL);
            String[] rij = r.getValue();
            int slash = rij[0].lastIndexOf('/');
            String ouder = rij[0].substring(0, slash), veld = rij[0].substring(slash + 1);
            ((ObjectNode) doc.at(ouder)).set(veld, JSON.readTree(rij[1]));
            IrLoader.Documentfout f = weigert(doc, r.getKey());
            assertEquals(rij[2] + "/" + VREEMD, f.plek(), r.getKey() + ": " + f.getMessage());
            assertTrue(f.getMessage().contains("unknown field"), r.getKey() + ": " + f.getMessage());
        }
    }

    /** A condition marker has no type (IR-SPEC ch. 4.4: that field is the expression marker's), and the creation's
     *  back-link, optional, is written whole or not at all. Both loaded until L2's fix round: the type was read past
     *  and a null back-link read as an absent one. */
    @Test void een_conditiemarker_met_type_en_een_lege_terugkoppeling_weigeren() throws Exception {
        JsonNode doc = golden(PENNINGDREMPEL);
        ((ObjectNode) doc.at("/rules/resolvedRegels/0")).set("voorwaarde", JSON.readTree(
                "{\"node\":\"Onopgelost\",\"type\":{\"node\":\"Booleaan\"},\"bron\":\"iets onleesbaars\"}"));
        IrLoader.Documentfout f = weigert(doc, "conditiemarker met type");
        assertEquals("/rules/resolvedRegels/0/voorwaarde/type", f.plek(), f.getMessage());
        assertTrue(f.getMessage().contains("unknown field on Onopgelost"), f.getMessage());

        doc = golden(WACHTERSKETEN);
        ((ObjectNode) doc.at("/rules/objectCreaties/0")).putNull("verbindendeRol");
        f = weigert(doc, "verbindendeRol null");
        assertEquals("/rules/objectCreaties/0/verbindendeRol", f.plek(), f.getMessage());
        assertTrue(f.getMessage().contains("null is neither"), f.getMessage());
    }

    private static JsonNode golden(String naam) throws Exception {
        JsonNode doc = JSON.readTree(Files.readString(CORPUS.resolve(naam).resolve("ir.json")));
        IrLoader.loadAll(doc);   // the untwisted golden loads, so the refusal is the twist's alone
        return doc;
    }

    private static IrLoader.Documentfout weigert(JsonNode doc, String wat) {
        try {
            IrLoader.loadAll(doc);
        } catch (IrLoader.Documentfout f) {
            return f;
        } catch (IrLoader.Refusal r) {
            fail(wat + ": verwacht een documentfout, kreeg een weigering van de vorm: " + r.getMessage());
        }
        return fail(wat + ": het document laadde");
    }

    /** The first position of every node kind (by its {@code node} value), in document order. */
    private static void verzamel(JsonNode n, String pad, Path ir, Map<String, Plek> eerste) {
        if (n instanceof ObjectNode o) {
            JsonNode soort = o.get("node");
            if (soort != null && soort.isTextual()) eerste.putIfAbsent(soort.asText(), new Plek(ir, pad));
            o.properties().forEach(e -> verzamel(e.getValue(),
                    pad + "/" + e.getKey().replace("~", "~0").replace("/", "~1"), ir, eerste));
        } else if (n instanceof ArrayNode a) {
            for (int i = 0; i < a.size(); i++) verzamel(a.get(i), pad + "/" + i, ir, eerste);
        }
    }

    private record Plek(Path ir, String pad) {}

    private static List<Path> goldens() throws Exception {
        try (Stream<Path> s = Files.walk(CORPUS)) {
            List<Path> uit = s.filter(p -> p.getFileName().toString().equals("ir.json")).sorted().toList();
            if (uit.isEmpty()) fail("geen enkele golden gevonden onder " + CORPUS.toAbsolutePath());
            return uit;
        }
    }

    private static String naam(Path ir) {
        return CORPUS.relativize(ir.getParent()).toString();
    }
}
