package dev.uchar.regelspraak.loader;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * A node name is read against the schema's union at its position — the expression and the condition union of IR-SPEC
 * ch. 4, the datatype union of ch. 2.4, the two parts of a text composition — and it can fail to load here in two
 * ways. A member of the union that has no record in this backend is a form it does not carry: a per-form
 * {@link IrLoader.Refusal}, which the differential gate files as a reason-carrying exclusion. Any other name is no node
 * of this format: an {@link IrLoader.Documentfout} at the discriminator's pointer, in the shared OCaml loader's
 * sentence ({@code unknown expr node "<v>"} and its three siblings), where the verifier, ref and the ASP backend stop
 * at load with the same sentence at the same pointer. Until the L5 landing's fix round every such name was a Refusal:
 * {@code RuleStatusCheck} at a condition position read {@code Conditie:RuleStatusCheck (unknown condition node)},
 * the channel of a form not carried, for a document no reader can read (measured on the L5 union, at the four
 * positions below).
 *
 * <p>The unions are read from the schema, so a member the schema gains that this loader neither carries nor lists
 * among the forms it refuses fails here instead of being called unknown. The v0 node names are the renaming witness's
 * ({@code RenamingRefusalWitnessTest}); the rows here are names no version of the wire had, a member's name in
 * another case, and members of one union at a position of another.
 */
class UnknownNodeWitnessTest {

    private static final ObjectMapper JSON = new ObjectMapper();
    // cwd under surefire is the module dir (backends/drools); the corpus and the schema are two levels up
    private static final Path CORPUS = Path.of("..", "..", "corpus", "golden");
    private static final Path SCHEMA = Path.of("..", "..", "spec", "schema", "uchar-ir-v1.schema.json");

    /** Rule 0 attributes a characteristic under a condition; its value, its condition and the datatype of the first
     *  attribute take the rows. */
    private static final String PENNINGDREMPEL = "jutterswet-penningdrempel";
    private static final String W = "/rules/rules/0/versions/0/value", V = "/rules/rules/0/versions/0/condition";
    private static final String D = "/model/objectTypes/0/attributes/0/datatype";

    /** A position of one union: where a node of it stands, the schema's name for the union, the refusal's form prefix
     *  and sentence, and the members this backend refuses as forms it does not carry. */
    private record Union(String def, String path, String formPrefix, String sentence, Set<String> notCarried) {}

    private static final Union EXPRESSION = new Union("expr", W, "Expr:", "unknown expr node",
            Set.of("TemporalRead", "Construct", "Match", "StructLiteral", "FieldAccess", "ListLiteral", "Lambda",
                    "Invoke", "Fold", "MapList", "FilterList", "ListLength"));
    private static final Union CONDITION = new Union("condition", V, "Conditie:", "unknown condition node", Set.of());
    private static final Union DATATYPE = new Union("datatype", D, "Datatype:", "unknown datatype node",
            Set.of("SumType", "ListType", "StructType"));
    private static final Union PART = new Union("TextCompositionPart", W + "/parts/0", "TekstReeks.Deel:",
            "unknown TextCompositionPart node", Set.of());

    // ================= a name outside the union =================

    @Test void an_expression_node_outside_the_union_is_a_document_error() throws Exception {
        // a name no wire had, a member in another case, and members of the condition, datatype and part unions
        for (String name : List.of("Quotient", "literal", "Regelversie", "Number", "ListType", "LiteralChunk"))
            unknown(EXPRESSION, name, with(EXPRESSION, name));
    }

    @Test void a_condition_node_outside_the_union_is_a_document_error() throws Exception {
        // the name the lens measured, a member in another case, two expression members — one this backend carries,
        // one it refuses as a form; at a condition position both are no condition at all — and the two role checks
        // of the L1 shape, whose refusal carried, until the L5 landing's fix round, a hint the shared loader's
        // sentence does not have
        for (String name : List.of("RuleStatusCheck", "comparison", "Literal", "ListLength", "Rollidmaatschap",
                "VerreRollidmaatschap"))
            unknown(CONDITION, name, with(CONDITION, name));
    }

    @Test void a_datatype_node_outside_the_union_is_a_document_error() throws Exception {
        for (String name : List.of("Decimal", "number", "ListLiteral", "Unit"))
            unknown(DATATYPE, name, with(DATATYPE, name));
        // a type annotation reads the same union: the literal's type at the value position
        ObjectNode doc = golden();
        node(doc, W + "/type").put("node", "Decimal");
        IrLoader.Documentfout e = refuses(doc, "Decimal as a type annotation");
        assertEquals(W + "/type/node", e.site(), e.getMessage());
        assertTrue(e.getMessage().endsWith(": unknown datatype node \"Decimal\""), e.getMessage());
    }

    @Test void a_text_part_outside_the_union_is_a_document_error() throws Exception {
        for (String name : List.of("Chunk", "literalChunk", "Literal"))
            unknown(PART, name, with(PART, name));
    }

    // ================= every member of the union =================

    /** Each member of each union, a bare node at a position of that union: a member with a record here loads or fails
     *  on anything but its name (a bare node lacks its required fields), and a member without one is the per-form
     *  refusal, the temporal read written whole because its keys are checked first. No member of the schema is called
     *  unknown. */
    @Test void each_node_of_the_schema_is_carried_or_a_refusal_of_the_form() throws Exception {
        JsonNode defs = JSON.readTree(Files.readString(SCHEMA)).get("$defs");
        int members = 0;
        for (Union u : List.of(EXPRESSION, CONDITION, DATATYPE, PART)) {
            List<String> names = members(defs, u.def());
            assertTrue(names.containsAll(u.notCarried()),
                    u.def() + ": every form refused by name is a member of the union — " + u.notCarried() + " / " + names);
            for (String name : names) {
                ObjectNode doc = name.equals("TemporalRead") ? golden(W, TEMPORAL_READ) : with(u, name);
                RuntimeException t = outcome(doc);
                String what = u.def() + " " + name + ": " + (t == null ? "loads" : t.getMessage());
                if (u.notCarried().contains(name)) {
                    if (!(t instanceof IrLoader.Refusal r)) throw new AssertionError("expected the form refusal, " + what);
                    assertEquals(u.formPrefix() + name, r.form(), what);
                } else {
                    assertTrue(t == null || t instanceof IrLoader.Documentfout || t instanceof IrLoader.Refusal, what);
                    assertTrue(!(t instanceof IrLoader.Refusal r) || !r.form().equals(u.formPrefix() + name), what);
                    assertTrue(!(t instanceof IrLoader.Documentfout) || !t.getMessage().endsWith(sentence(u, name)), what);
                }
                members++;
            }
        }
        // measured 2026-09-25 on the schema of the L5 landing: 30 expression, 9 condition and 8 datatype members, and
        // the two parts. A floor, not a pin: a member the schema gains is swept as it comes.
        assertTrue(members >= 49, "the four unions of the schema: " + members + " members");
    }

    // ================= helpers =================

    /** A TemporalRead written whole: its keys are checked before the form is refused. */
    private static final String TEMPORAL_READ = "{\"node\":\"TemporalRead\",\"base\":{\"node\":\"Literal\","
            + "\"value\":{\"text\":\"x\"},\"type\":{\"node\":\"Text\"}},\"date\":{\"node\":\"Literal\","
            + "\"value\":{\"date\":\"2024-01-01\"},\"type\":{\"node\":\"Date\",\"precision\":\"day\"}},"
            + "\"type\":{\"node\":\"Text\"},\"source\":{\"snippet\":\"x op 1 januari 2024\"}}";

    /** The golden with a bare node of the given name at the union's position. */
    private static ObjectNode with(Union u, String name) throws Exception {
        if (u == PART)
            return golden(W, "{\"node\":\"TextComposition\",\"parts\":[{\"node\":\"" + name + "\"}],"
                    + "\"type\":{\"node\":\"Text\"},\"source\":{\"snippet\":\"x\"}}");
        return golden(u.path(), "{\"node\":\"" + name + "\"}");
    }

    private static void unknown(Union u, String name, ObjectNode doc) {
        IrLoader.Documentfout e = refuses(doc, u.def() + " " + name);
        assertEquals(u.path() + "/node", e.site(), name + ": " + e.getMessage());
        assertTrue(e.getMessage().endsWith(sentence(u, name)), name + ": " + e.getMessage());
    }

    private static String sentence(Union u, String name) {
        return ": " + u.sentence() + " \"" + name + "\"";
    }

    /** The node names of a union of the schema: the {@code node} constant of each member. */
    private static List<String> members(JsonNode defs, String union) {
        List<String> out = new ArrayList<>();
        for (JsonNode ref : defs.get(union).get("oneOf")) {
            String def = ref.get("$ref").asText().substring("#/$defs/".length());
            out.add(defs.get(def).get("properties").get("node").get("const").asText());
        }
        if (out.isEmpty()) fail("the schema's union " + union + " has no member");
        return out;
    }

    private static RuntimeException outcome(JsonNode doc) {
        try {
            IrLoader.loadAll(doc);
            return null;
        } catch (RuntimeException e) {
            return e;
        }
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

    private static ObjectNode node(JsonNode doc, String path) {
        JsonNode n = doc.at(path);
        if (!(n instanceof ObjectNode o)) throw new AssertionError("no object at " + path);
        return o;
    }

    /** The golden with the node at {@code path} replaced by {@code json}. */
    private static ObjectNode golden(String path, String json) throws Exception {
        ObjectNode doc = golden();
        int slash = path.lastIndexOf('/');
        node(doc, path.substring(0, slash)).set(path.substring(slash + 1), JSON.readTree(json));
        return doc;
    }

    private static ObjectNode golden() throws Exception {
        ObjectNode doc = (ObjectNode) JSON.readTree(Files.readString(CORPUS.resolve(PENNINGDREMPEL).resolve("ir.json")));
        IrLoader.loadAll(doc);   // the untwisted golden loads, so a refusal is the row's alone
        return doc;
    }
}
