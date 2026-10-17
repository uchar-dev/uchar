package dev.uchar.regelspraak.loader;

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
 * and ignored it. The sweep reaches 64 kinds (measured 2026-09-24 on L4, and again 2026-09-25 on L5, where the
 * renamed kinds took the places of their v0 names one for one): 58 node kinds by their discriminator — the eleven
 * rule- and expression-layer kinds L4 replaced gave way to its eleven, but {@code ContextValue} stands only in a
 * golden refused as a form not carried — and the six version records of the named forms, which carry no
 * discriminator and are swept under their form's kind ({@code RuleVersion}, …). The exact factor has no discriminator
 * either and its rows stand in {@code TypeSystemRefusalWitnessTest}. Fifteen more kinds occur only in goldens this
 * backend refuses as forms it does not carry (the list, sum, struct and function forms, {@code TemporalRead},
 * {@code Match}), so their document is refused before the node is read; the kinds this backend reads that no
 * loadable golden carries have rows of their own below.
 */
class UnknownFieldWitnessTest {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Path CORPUS = Path.of("..", "..", "corpus", "golden");
    private static final String FOREIGN = "uchar-onbekend-veld";

    /** A golden with one rule whose value and condition the rows below replace. Both branches PLAYED. */
    private static final String PENNINGDREMPEL = "jutterswet-penningdrempel";
    /** An object creation carrying its verbindendeRol. Both branches PLAYED. */
    private static final String WACHTERSKETEN = "anchor/jutterswet-wachtersketen";

    @Test void each_read_node_kind_refuses_an_unknown_field_by_name() throws Exception {
        Map<String, Site> first = new TreeMap<>();
        for (Path ir : goldens()) {
            JsonNode doc = JSON.readTree(Files.readString(ir));
            try { IrLoader.loadAll(doc); } catch (IrLoader.Refusal r) { continue; }
            collect(doc, "", ir, first);
        }
        // 64 kinds, measured 2026-09-24 on L4 and 2026-09-25 on L5; the goldens that carry the rest are refused as
        // forms not carried
        assertTrue(first.size() >= 64, () -> "te weinig knoopsoorten gevonden: " + first.keySet());
        List<String> admitted = new ArrayList<>();
        for (Map.Entry<String, Site> e : first.entrySet()) {
            Site site = e.getValue();
            JsonNode doc = JSON.readTree(Files.readString(site.ir()));
            ((ObjectNode) doc.at(site.path())).put(FOREIGN, 1);
            try {
                IrLoader.loadAll(doc);
                admitted.add(e.getKey() + " (" + name(site.ir()) + " " + site.path() + ")");
            } catch (IrLoader.Documentfout f) {
                assertEquals(site.path() + "/" + FOREIGN, f.plek(), e.getKey() + ": " + f.getMessage());
                assertTrue(f.getMessage().contains("unknown field"), e.getKey() + ": " + f.getMessage());
            }
        }
        assertEquals(List.of(), admitted, "een knoopsoort las een veld dat het schema haar niet geeft stil voorbij");
    }

    /** The node kinds this backend reads that no loadable golden carries, placed at a rule's value or condition
     *  with the stray field. The key check runs before anything else of the node is read, so the rest of each
     *  node only has to be well-formed. */
    @Test void the_node_kinds_without_a_golden_also_refuse_an_unknown_field() throws Exception {
        String w = "/rules/rules/0/versions/0/value", v = "/rules/rules/0/versions/0/condition";
        String literal = "{\"node\":\"Literal\",\"value\":{\"text\":\"x\"},\"type\":{\"node\":\"Text\"},\"source\":{\"snippet\":\"x\"}}";
        Map<String, String[]> rows = new java.util.LinkedHashMap<>();
        rows.put("TextComposition", new String[] { w, "{\"node\":\"TextComposition\",\"parts\":[],\"type\":{\"node\":\"Text\"},"
                + "\"source\":{\"snippet\":\"x\"},\"" + FOREIGN + "\":1}", w });
        rows.put("LiteralChunk", new String[] { w, "{\"node\":\"TextComposition\",\"parts\":[{\"node\":\"LiteralChunk\","
                + "\"text\":\"x\",\"" + FOREIGN + "\":1}],\"type\":{\"node\":\"Text\"},\"source\":{\"snippet\":\"x\"}}",
                w + "/parts/0" });
        rows.put("EmbeddedExpression", new String[] { w, "{\"node\":\"TextComposition\",\"parts\":[{\"node\":\"EmbeddedExpression\",\"expr\":"
                + literal + ",\"" + FOREIGN + "\":1}],\"type\":{\"node\":\"Text\"},\"source\":{\"snippet\":\"x\"}}", w + "/parts/0" });
        rows.put("VariableReference", new String[] { w, "{\"node\":\"VariableReference\",\"name\":\"x\",\"source\":{\"snippet\":\"x\"},\""
                + FOREIGN + "\":1}", w });
        rows.put("Unresolved (expression)", new String[] { w, "{\"node\":\"Unresolved\",\"source\":{\"snippet\":\"x\"},\"" + FOREIGN
                + "\":1}", w });
        rows.put("DimensionSelection", new String[] { w, "{\"node\":\"Aggregation\",\"kind\":\"sum\",\"arguments\":[],"
                + "\"dimensionSelection\":{\"node\":\"DimensionSelection\",\"dimension\":\"d\",\"labels\":[],"
                + "\"source\":{\"snippet\":\"x\"},\"" + FOREIGN + "\":1},\"type\":null,\"source\":{\"snippet\":\"x\"}}", w + "/dimensionSelection" });
        rows.put("ContextValue", new String[] { w, "{\"node\":\"ContextValue\",\"name\":\"evaluationDate\",\"type\":"
                + "{\"node\":\"Date\",\"precision\":\"day\"},\"source\":{\"snippet\":\"de Rekendatum\"},\"" + FOREIGN + "\":1}", w });
        rows.put("Regelversie", new String[] { v, "{\"node\":\"Regelversie\",\"regel\":\"x\",\"inconsistent\":false,"
                + "\"source\":{\"snippet\":\"x\"},\"" + FOREIGN + "\":1}", v });
        rows.put("Unresolved (condition)", new String[] { v, "{\"node\":\"Unresolved\",\"source\":{\"snippet\":\"x\"},\"" + FOREIGN
                + "\":1}", v });
        for (Map.Entry<String, String[]> r : rows.entrySet()) {
            JsonNode doc = golden(PENNINGDREMPEL);
            String[] row = r.getValue();
            int slash = row[0].lastIndexOf('/');
            String parent = row[0].substring(0, slash), field = row[0].substring(slash + 1);
            ((ObjectNode) doc.at(parent)).set(field, JSON.readTree(row[1]));
            IrLoader.Documentfout f = refuses(doc, r.getKey());
            assertEquals(row[2] + "/" + FOREIGN, f.plek(), r.getKey() + ": " + f.getMessage());
            assertTrue(f.getMessage().contains("unknown field"), r.getKey() + ": " + f.getMessage());
        }
    }

    /** A condition marker has no type (IR-SPEC ch. 4.4: that field is the expression marker's), and the creation's
     *  back-link, optional, is written whole or not at all. Both loaded until L2's fix round: the type was read past
     *  and a null back-link read as an absent one. */
    @Test void a_condition_marker_with_type_and_an_empty_back_link_are_each_refused() throws Exception {
        JsonNode doc = golden(PENNINGDREMPEL);
        ((ObjectNode) doc.at("/rules/rules/0/versions/0")).set("condition", JSON.readTree(
                "{\"node\":\"Unresolved\",\"type\":{\"node\":\"Boolean\"},\"source\":{\"snippet\":\"iets onleesbaars\"}}"));
        IrLoader.Documentfout f = refuses(doc, "conditiemarker met type");
        assertEquals("/rules/rules/0/versions/0/condition/type", f.plek(), f.getMessage());
        assertTrue(f.getMessage().contains("unknown field on Unresolved"), f.getMessage());

        doc = golden(WACHTERSKETEN);
        ((ObjectNode) doc.at("/rules/objectCreations/0/versions/0")).putNull("connectingRole");
        f = refuses(doc, "connectingRole null");
        assertEquals("/rules/objectCreations/0/versions/0/connectingRole", f.plek(), f.getMessage());
        assertTrue(f.getMessage().contains("null is neither"), f.getMessage());
    }

    /** The derived positions whose record computes its own type — a rounding, a clamp, a text composition — still
     *  carry a wire type that is a datatype or null; until the advisory round of L2 it was not read at all, so an
     *  {@code Onbekend} there loaded. */
    @Test void an_unknown_type_on_a_derived_position_is_refused() throws Exception {
        String w = "/rules/rules/0/versions/0/value";
        String literal = "{\"node\":\"Literal\",\"value\":{\"number\":\"2\"},\"type\":{\"node\":\"Number\",\"whole\":true,"
                + "\"decimals\":0,\"sign\":\"unspecified\"},\"source\":{\"snippet\":\"2\"}}";
        String unknown = "{\"node\":\"Onbekend\",\"reden\":\"niet opgelost\"}";
        Map<String, String> rows = new java.util.LinkedHashMap<>();
        rows.put("Rounding", "{\"node\":\"Rounding\",\"base\":" + literal + ",\"mode\":\"halfAwayFromZero\","
                + "\"decimals\":0,\"type\":" + unknown + ",\"source\":{\"snippet\":\"x\"}}");
        rows.put("Clamp", "{\"node\":\"Clamp\",\"base\":" + literal + ","
                + "\"type\":" + unknown + ",\"source\":{\"snippet\":\"x\"}}");
        rows.put("TextComposition", "{\"node\":\"TextComposition\",\"parts\":[],\"type\":" + unknown + ",\"source\":{\"snippet\":\"x\"}}");
        for (Map.Entry<String, String> r : rows.entrySet()) {
            JsonNode doc = golden(PENNINGDREMPEL);
            ((ObjectNode) doc.at("/rules/rules/0/versions/0")).set("value", JSON.readTree(r.getValue()));
            IrLoader.Documentfout f = refuses(doc, r.getKey());
            // the shared loader's sentence, at the discriminator
            assertEquals(w + "/type/node", f.plek(), r.getKey() + ": " + f.getMessage());
            assertTrue(f.getMessage().endsWith(": unknown datatype node \"Onbekend\""),
                    r.getKey() + ": " + f.getMessage());
        }
    }

    /** A scalar where a node stands is refused at its own pointer. Jackson shares small numbers, so a scalar has no
     *  place of its own to look up; until the advisory round of L2 a validity of 1 ended in an
     *  IllegalStateException that named no place, and a scalar expression in a placeholder place. */
    @Test void a_scalar_on_the_site_of_a_node_is_refused_with_a_site() throws Exception {
        String[][] rows = {
                { "/rules/rules/0/versions/0", "validity", "/rules/rules/0/versions/0/validity" },
                { "/rules/rules/0/versions/0", "value", "/rules/rules/0/versions/0/value" },
                { "/rules/rules/0/versions/0", "condition", "/rules/rules/0/versions/0/condition" } };
        for (String[] row : rows) {
            JsonNode doc = golden(PENNINGDREMPEL);
            ((ObjectNode) doc.at(row[0])).put(row[1], 1);
            IrLoader.Documentfout f = refuses(doc, row[1]);
            assertEquals(row[2], f.plek(), row[1] + ": " + f.getMessage());
            assertTrue(f.getMessage().contains("expected a JSON object, got 1"), row[1] + ": " + f.getMessage());
        }
        // ...and an entry of a node list, at its index
        JsonNode doc = golden(PENNINGDREMPEL);
        ((ObjectNode) doc.at("/rules/rules/0/versions/0")).set("value", JSON.readTree("{\"node\":\"Aggregation\","
                + "\"kind\":\"sum\",\"arguments\":[1],\"type\":null,\"source\":{\"snippet\":\"x\"}}"));
        IrLoader.Documentfout f = refuses(doc, "arguments");
        assertEquals("/rules/rules/0/versions/0/value/arguments/0", f.plek(), f.getMessage());
    }

    private static JsonNode golden(String name) throws Exception {
        JsonNode doc = JSON.readTree(Files.readString(CORPUS.resolve(name).resolve("ir.json")));
        IrLoader.loadAll(doc);   // the untwisted golden loads, so the refusal is the twist's alone
        return doc;
    }

    private static IrLoader.Documentfout refuses(JsonNode doc, String what) {
        try {
            IrLoader.loadAll(doc);
        } catch (IrLoader.Documentfout f) {
            return f;
        } catch (IrLoader.Refusal r) {
            fail(what + ": verwacht een documentfout, kreeg een weigering van de vorm: " + r.getMessage());
        }
        return fail(what + ": het document laadde");
    }

    /** The first position of every node kind (by its {@code node} value), in document order; a version record of a
     *  named form, which carries no discriminator, under its form's kind with "Version" appended. */
    private static void collect(JsonNode n, String path, Path ir, Map<String, Site> first) {
        if (n instanceof ObjectNode o) {
            JsonNode kind = o.get("node");
            if (kind != null && kind.isTextual()) first.putIfAbsent(kind.asText(), new Site(ir, path));
            if (kind != null && o.path("versions").isArray())
                for (int i = 0; i < o.get("versions").size(); i++)
                    first.putIfAbsent(kind.asText() + "Version", new Site(ir, path + "/versions/" + i));
            o.properties().forEach(e -> collect(e.getValue(),
                    path + "/" + e.getKey().replace("~", "~0").replace("/", "~1"), ir, first));
        } else if (n instanceof ArrayNode a) {
            for (int i = 0; i < a.size(); i++) collect(a.get(i), path + "/" + i, ir, first);
        }
    }

    private record Site(Path ir, String path) {}

    private static List<Path> goldens() throws Exception {
        try (Stream<Path> s = Files.walk(CORPUS)) {
            List<Path> out = s.filter(p -> p.getFileName().toString().equals("ir.json")).sorted().toList();
            if (out.isEmpty()) fail("geen enkele golden gevonden onder " + CORPUS.toAbsolutePath());
            return out;
        }
    }

    private static String name(Path ir) {
        return CORPUS.relativize(ir.getParent()).toString();
    }
}
