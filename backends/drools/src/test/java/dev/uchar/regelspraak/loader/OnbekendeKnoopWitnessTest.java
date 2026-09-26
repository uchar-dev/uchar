package io.github.yhavinga.regelspraak.loader;

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
 * ({@code HernoemingWeigeringWitnessTest}); the rows here are names no version of the wire had, a member's name in
 * another case, and members of one union at a position of another.
 */
class OnbekendeKnoopWitnessTest {

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
    private record Unie(String def, String pad, String vorm, String zin, Set<String> nietGedragen) {}

    private static final Unie EXPRESSIE = new Unie("expr", W, "Expr:", "unknown expr node",
            Set.of("TemporalRead", "Construct", "Match", "StructLiteral", "FieldAccess", "ListLiteral", "Lambda",
                    "Invoke", "Fold", "MapList", "FilterList", "ListLength"));
    private static final Unie CONDITIE = new Unie("condition", V, "Conditie:", "unknown condition node", Set.of());
    private static final Unie DATATYPE = new Unie("datatype", D, "Datatype:", "unknown datatype node",
            Set.of("SumType", "ListType", "StructType"));
    private static final Unie DEEL = new Unie("TextCompositionPart", W + "/parts/0", "TekstReeks.Deel:",
            "unknown TextCompositionPart node", Set.of());

    // ================= a name outside the union =================

    @Test void een_expressieknoop_buiten_de_unie_is_een_documentfout() throws Exception {
        // a name no wire had, a member in another case, and members of the condition, datatype and part unions
        for (String naam : List.of("Quotient", "literal", "Regelversie", "Number", "ListType", "LiteralChunk"))
            onbekend(EXPRESSIE, naam, met(EXPRESSIE, naam));
    }

    @Test void een_conditieknoop_buiten_de_unie_is_een_documentfout() throws Exception {
        // the name the lens measured, a member in another case, two expression members — one this backend carries,
        // one it refuses as a form; at a condition position both are no condition at all — and the two role checks
        // of the L1 shape, whose refusal carried, until the L5 landing's fix round, a hint the shared loader's
        // sentence does not have
        for (String naam : List.of("RuleStatusCheck", "comparison", "Literal", "ListLength", "Rollidmaatschap",
                "VerreRollidmaatschap"))
            onbekend(CONDITIE, naam, met(CONDITIE, naam));
    }

    @Test void een_datatypeknoop_buiten_de_unie_is_een_documentfout() throws Exception {
        for (String naam : List.of("Decimal", "number", "ListLiteral", "Unit"))
            onbekend(DATATYPE, naam, met(DATATYPE, naam));
        // a type annotation reads the same union: the literal's type at the value position
        ObjectNode doc = golden();
        knoop(doc, W + "/type").put("node", "Decimal");
        IrLoader.Documentfout e = weigert(doc, "Decimal as a type annotation");
        assertEquals(W + "/type/node", e.plek(), e.getMessage());
        assertTrue(e.getMessage().endsWith(": unknown datatype node \"Decimal\""), e.getMessage());
    }

    @Test void een_tekstdeel_buiten_de_unie_is_een_documentfout() throws Exception {
        for (String naam : List.of("Chunk", "literalChunk", "Literal"))
            onbekend(DEEL, naam, met(DEEL, naam));
    }

    // ================= every member of the union =================

    /** Each member of each union, a bare node at a position of that union: a member with a record here loads or fails
     *  on anything but its name (a bare node lacks its required fields), and a member without one is the per-form
     *  refusal, the temporal read written whole because its keys are checked first. No member of the schema is called
     *  unknown. */
    @Test void elke_knoop_van_het_schema_is_gedragen_of_een_weigering_van_de_vorm() throws Exception {
        JsonNode defs = JSON.readTree(Files.readString(SCHEMA)).get("$defs");
        int leden = 0;
        for (Unie u : List.of(EXPRESSIE, CONDITIE, DATATYPE, DEEL)) {
            List<String> namen = leden(defs, u.def());
            assertTrue(namen.containsAll(u.nietGedragen()),
                    u.def() + ": every form refused by name is a member of the union — " + u.nietGedragen() + " / " + namen);
            for (String naam : namen) {
                ObjectNode doc = naam.equals("TemporalRead") ? golden(W, TEMPORAL_READ) : met(u, naam);
                RuntimeException t = uitkomst(doc);
                String wat = u.def() + " " + naam + ": " + (t == null ? "loads" : t.getMessage());
                if (u.nietGedragen().contains(naam)) {
                    if (!(t instanceof IrLoader.Refusal r)) throw new AssertionError("expected the form refusal, " + wat);
                    assertEquals(u.vorm() + naam, r.form(), wat);
                } else {
                    assertTrue(t == null || t instanceof IrLoader.Documentfout || t instanceof IrLoader.Refusal, wat);
                    assertTrue(!(t instanceof IrLoader.Refusal r) || !r.form().equals(u.vorm() + naam), wat);
                    assertTrue(!(t instanceof IrLoader.Documentfout) || !t.getMessage().endsWith(zin(u, naam)), wat);
                }
                leden++;
            }
        }
        // measured 2026-09-25 on the schema of the L5 landing: 30 expression, 9 condition and 8 datatype members, and
        // the two parts. A floor, not a pin: a member the schema gains is swept as it comes.
        assertTrue(leden >= 49, "the four unions of the schema: " + leden + " members");
    }

    // ================= helpers =================

    /** A TemporalRead written whole: its keys are checked before the form is refused. */
    private static final String TEMPORAL_READ = "{\"node\":\"TemporalRead\",\"base\":{\"node\":\"Literal\","
            + "\"value\":{\"text\":\"x\"},\"type\":{\"node\":\"Text\"}},\"date\":{\"node\":\"Literal\","
            + "\"value\":{\"date\":\"2024-01-01\"},\"type\":{\"node\":\"Date\",\"precision\":\"day\"}},"
            + "\"type\":{\"node\":\"Text\"},\"source\":{\"snippet\":\"x op 1 januari 2024\"}}";

    /** The golden with a bare node of the given name at the union's position. */
    private static ObjectNode met(Unie u, String naam) throws Exception {
        if (u == DEEL)
            return golden(W, "{\"node\":\"TextComposition\",\"parts\":[{\"node\":\"" + naam + "\"}],"
                    + "\"type\":{\"node\":\"Text\"},\"source\":{\"snippet\":\"x\"}}");
        return golden(u.pad(), "{\"node\":\"" + naam + "\"}");
    }

    private static void onbekend(Unie u, String naam, ObjectNode doc) {
        IrLoader.Documentfout e = weigert(doc, u.def() + " " + naam);
        assertEquals(u.pad() + "/node", e.plek(), naam + ": " + e.getMessage());
        assertTrue(e.getMessage().endsWith(zin(u, naam)), naam + ": " + e.getMessage());
    }

    private static String zin(Unie u, String naam) {
        return ": " + u.zin() + " \"" + naam + "\"";
    }

    /** The node names of a union of the schema: the {@code node} constant of each member. */
    private static List<String> leden(JsonNode defs, String unie) {
        List<String> uit = new ArrayList<>();
        for (JsonNode ref : defs.get(unie).get("oneOf")) {
            String def = ref.get("$ref").asText().substring("#/$defs/".length());
            uit.add(defs.get(def).get("properties").get("node").get("const").asText());
        }
        if (uit.isEmpty()) fail("the schema's union " + unie + " has no member");
        return uit;
    }

    private static RuntimeException uitkomst(JsonNode doc) {
        try {
            IrLoader.loadAll(doc);
            return null;
        } catch (RuntimeException e) {
            return e;
        }
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

    private static ObjectNode knoop(JsonNode doc, String pad) {
        JsonNode n = doc.at(pad);
        if (!(n instanceof ObjectNode o)) throw new AssertionError("no object at " + pad);
        return o;
    }

    /** The golden with the node at {@code pad} replaced by {@code json}. */
    private static ObjectNode golden(String pad, String json) throws Exception {
        ObjectNode doc = golden();
        int slash = pad.lastIndexOf('/');
        knoop(doc, pad.substring(0, slash)).set(pad.substring(slash + 1), JSON.readTree(json));
        return doc;
    }

    private static ObjectNode golden() throws Exception {
        ObjectNode doc = (ObjectNode) JSON.readTree(Files.readString(CORPUS.resolve(PENNINGDREMPEL).resolve("ir.json")));
        IrLoader.loadAll(doc);   // the untwisted golden loads, so a refusal is the row's alone
        return doc;
    }
}
