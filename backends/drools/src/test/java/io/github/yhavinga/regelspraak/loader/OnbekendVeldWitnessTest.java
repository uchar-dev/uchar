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
 * and ignored it. The sweep reaches 64 kinds (measured 2026-09-24 on L4): 58 node kinds by their discriminator — the
 * eleven rule- and expression-layer kinds L4 replaced gave way to its eleven, but {@code ContextValue} stands only in
 * a golden refused as a form not carried — and the six version records of the named forms, which carry no
 * discriminator and are swept under their form's kind ({@code RuleVersion}, …). The exact factor has no discriminator
 * either and its rows stand in {@code TypesysteemWeigeringWitnessTest}. Fifteen more kinds occur only in goldens this
 * backend refuses as forms it does not carry (the list, sum, struct and function forms, {@code TemporalRead},
 * {@code Match}), so their document is refused before the node is read; the kinds this backend reads that no
 * loadable golden carries have rows of their own below.
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
        // 64 kinds, measured 2026-09-24 on L4; the goldens that carry the rest are refused as forms not carried
        assertTrue(eerste.size() >= 64, () -> "te weinig knoopsoorten gevonden: " + eerste.keySet());
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
        String w = "/rules/resolvedRegels/0/versions/0/value", v = "/rules/resolvedRegels/0/versions/0/condition";
        String literal = "{\"node\":\"Literal\",\"value\":{\"text\":\"x\"},\"type\":{\"node\":\"Tekst\"},\"bron\":\"x\"}";
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
        rijen.put("DimensieSelectie", new String[] { w, "{\"node\":\"Aggregation\",\"kind\":\"SOM\",\"arguments\":[],"
                + "\"dimensionSelection\":{\"node\":\"DimensieSelectie\",\"dimensie\":\"d\",\"labels\":[],"
                + "\"bron\":\"x\",\"" + VREEMD + "\":1},\"type\":null,\"bron\":\"x\"}", w + "/dimensionSelection" });
        rijen.put("ContextValue", new String[] { w, "{\"node\":\"ContextValue\",\"name\":\"Rekendatum\",\"type\":"
                + "{\"node\":\"Date\",\"precision\":\"day\"},\"bron\":\"de Rekendatum\",\"" + VREEMD + "\":1}", w });
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
        ((ObjectNode) doc.at("/rules/resolvedRegels/0/versions/0")).set("condition", JSON.readTree(
                "{\"node\":\"Onopgelost\",\"type\":{\"node\":\"Booleaan\"},\"bron\":\"iets onleesbaars\"}"));
        IrLoader.Documentfout f = weigert(doc, "conditiemarker met type");
        assertEquals("/rules/resolvedRegels/0/versions/0/condition/type", f.plek(), f.getMessage());
        assertTrue(f.getMessage().contains("unknown field on Onopgelost"), f.getMessage());

        doc = golden(WACHTERSKETEN);
        ((ObjectNode) doc.at("/rules/objectCreaties/0/versions/0")).putNull("connectingRole");
        f = weigert(doc, "connectingRole null");
        assertEquals("/rules/objectCreaties/0/versions/0/connectingRole", f.plek(), f.getMessage());
        assertTrue(f.getMessage().contains("null is neither"), f.getMessage());
    }

    /** The derived positions whose record computes its own type — a rounding, a clamp, a text sequence — still
     *  carry a wire type that is a datatype or null; until the advisory round of L2 it was not read at all, so an
     *  {@code Onbekend} there loaded. */
    @Test void een_onbekend_type_op_een_afgeleide_positie_weigert() throws Exception {
        String w = "/rules/resolvedRegels/0/versions/0/value";
        String literal = "{\"node\":\"Literal\",\"value\":{\"number\":\"2\"},\"type\":{\"node\":\"Number\",\"whole\":true,"
                + "\"decimals\":0,\"sign\":\"ONBEPAALD\"},\"bron\":\"2\"}";
        String onbekend = "{\"node\":\"Onbekend\",\"reden\":\"niet opgelost\"}";
        Map<String, String> rijen = new java.util.LinkedHashMap<>();
        rijen.put("Afgerond", "{\"node\":\"Afgerond\",\"basis\":" + literal + ",\"modus\":\"REKENKUNDIG\","
                + "\"decimalen\":0,\"type\":" + onbekend + ",\"bron\":\"x\"}");
        rijen.put("Begrensd", "{\"node\":\"Begrensd\",\"basis\":" + literal + ",\"minimum\":null,"
                + "\"maximum\":null,\"type\":" + onbekend + ",\"bron\":\"x\"}");
        rijen.put("TekstReeks", "{\"node\":\"TekstReeks\",\"delen\":[],\"type\":" + onbekend + ",\"bron\":\"x\"}");
        for (Map.Entry<String, String> r : rijen.entrySet()) {
            JsonNode doc = golden(PENNINGDREMPEL);
            ((ObjectNode) doc.at("/rules/resolvedRegels/0/versions/0")).set("value", JSON.readTree(r.getValue()));
            IrLoader.Documentfout f = weigert(doc, r.getKey());
            // the shared loader's sentence, at the discriminator
            assertEquals(w + "/type/node", f.plek(), r.getKey() + ": " + f.getMessage());
            assertTrue(f.getMessage().endsWith(": unknown datatype node \"Onbekend\""),
                    r.getKey() + ": " + f.getMessage());
        }
    }

    /** A scalar where a node stands is refused at its own pointer. Jackson shares small numbers, so a scalar has no
     *  place of its own to look up; until the advisory round of L2 {@code geldigheid: 1} ended in an
     *  IllegalStateException that named no place, and a scalar expression in a placeholder place. */
    @Test void een_scalair_op_de_plek_van_een_knoop_weigert_met_een_plek() throws Exception {
        String[][] rijen = {
                { "/rules/resolvedRegels/0/versions/0", "validity", "/rules/resolvedRegels/0/versions/0/validity" },
                { "/rules/resolvedRegels/0/versions/0", "value", "/rules/resolvedRegels/0/versions/0/value" },
                { "/rules/resolvedRegels/0/versions/0", "condition", "/rules/resolvedRegels/0/versions/0/condition" } };
        for (String[] rij : rijen) {
            JsonNode doc = golden(PENNINGDREMPEL);
            ((ObjectNode) doc.at(rij[0])).put(rij[1], 1);
            IrLoader.Documentfout f = weigert(doc, rij[1]);
            assertEquals(rij[2], f.plek(), rij[1] + ": " + f.getMessage());
            assertTrue(f.getMessage().contains("expected a JSON object, got 1"), rij[1] + ": " + f.getMessage());
        }
        // ...and an entry of a node list, at its index
        JsonNode doc = golden(PENNINGDREMPEL);
        ((ObjectNode) doc.at("/rules/resolvedRegels/0/versions/0")).set("value", JSON.readTree("{\"node\":\"Aggregation\","
                + "\"kind\":\"SOM\",\"arguments\":[1],\"type\":null,\"bron\":\"x\"}"));
        IrLoader.Documentfout f = weigert(doc, "arguments");
        assertEquals("/rules/resolvedRegels/0/versions/0/value/arguments/0", f.plek(), f.getMessage());
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

    /** The first position of every node kind (by its {@code node} value), in document order; a version record of a
     *  named form, which carries no discriminator, under its form's kind with "Version" appended. */
    private static void verzamel(JsonNode n, String pad, Path ir, Map<String, Plek> eerste) {
        if (n instanceof ObjectNode o) {
            JsonNode soort = o.get("node");
            if (soort != null && soort.isTextual()) eerste.putIfAbsent(soort.asText(), new Plek(ir, pad));
            if (soort != null && o.path("versions").isArray())
                for (int i = 0; i < o.get("versions").size(); i++)
                    eerste.putIfAbsent(soort.asText() + "Version", new Plek(ir, pad + "/versions/" + i));
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
