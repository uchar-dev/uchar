package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import dev.uchar.regelspraak.loader.IrLoader;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * IR-SPEC ch. 4.1a: a read without a path is the subject's OWN read, so its {@code subjectType} is the
 * subject type — equal, not a supertype and not another type (WF-053). A bindingless read of any other type is
 * refused while loading, in the sentence ref and the verifier share; until the advisory round of L2 both transpiler
 * branches refused it with a generic reason, and neither derived a relation for it. A read of an INHERITED
 * attribute stays an own read when it names the subject type itself.
 *
 * <p>Every model is an in-memory variant of {@code jutterswet-munttelling}: Munt ⊑ Vondst, and Vondst declares
 * {@code gewicht}. Each row adds a Munt attribute {@code muntwaarde} and one rule that writes it from an own read of
 * {@code gewicht}; only the type the read names differs. The corpus files are read and never written.
 */
class OwnReadWitnessTest {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Path GOLDEN = Path.of("..", "..", "corpus", "golden", "jutterswet-munttelling");

    @Test void the_own_read_of_an_inherited_attribute_on_the_subject_type_plays() throws Exception {
        // M1 is a Munt with gewicht 7.00: its muntwaarde is its own gewicht
        BothBranches.green(temporary(withMuntwaarde("Munt")), JSON.readTree(CASE), "own read of an inherited attribute");
    }

    @Test void the_own_read_via_the_supertype_is_refused() throws Exception {
        // Munt ⊑ Vondst, and still no own read: the read names a type that is not the subject's
        refuses(withMuntwaarde("Vondst"), "Vondst");
    }

    @Test void the_own_read_of_another_type_is_refused() throws Exception {
        refuses(withMuntwaarde("Jutter"), "Jutter");
    }

    /** The load refusal, for either branch: nothing is lowered from a document that names another type there. */
    private static void refuses(ObjectNode ir, String type) {
        IrLoader.DocumentError e = assertThrows(IrLoader.DocumentError.class, () -> IrLoader.loadAll(ir), type);
        assertEquals("/rules/rules/0/versions/0/value", e.site(), e.getMessage());
        assertTrue(e.getMessage().endsWith(": an attribute reference without a path is the subject's own"
                + " read, so its subjectType is the subject type 'Munt' — '" + type + "' names another type, which only"
                + " a navigation reaches"), e.getMessage());
    }

    // ---------------- fixtures ----------------

    private static final String CASE = """
            { "format": "uchar-case-v0", "asOfDate": null,
              "input": { "instances": [
                  { "label": "M1", "objectType": "Munt",
                    "attributes": { "gewicht": { "number": "7.00" }, "jaartal": { "number": "1650" } },
                    "roles": {}, "characteristics": [] } ],
                "parameters": {} },
              "expected": [
                { "kind": "attributeValue", "label": "M1", "attribute": "muntwaarde",
                  "value": { "number": "7.00" } } ] }
            """;

    /** The golden reduced to one rule, "De muntwaarde van een Munt moet berekend worden als het gewicht", whose read
     *  names {@code objectType} as its subjectType. */
    private static ObjectNode withMuntwaarde(String objectType) throws Exception {
        ObjectNode ir = (ObjectNode) JSON.readTree(Files.readString(GOLDEN.resolve("ir.json")));
        JsonNode weight = IrPath.attribute(IrPath.objectType(ir, "Vondst"), "gewicht").get("datatype");
        IrPath.addAttribute(IrPath.objectType(ir, "Munt"), "muntwaarde", weight.deepCopy());
        ObjectNode form = IrPath.rules(ir).putArray("rules").addObject();
        form.put("node", "Rule").put("name", "muntwaarde");
        ObjectNode rule = form.putArray("versions").addObject();
        rule.putObject("validity").put("node", "Validity");
        rule.putObject("target").put("node", "AttributeReference").put("attribute", "muntwaarde").put("subjectType", "Munt")
             .set("source", IrPath.sourceReference("De muntwaarde van een Munt"));
        rule.putObject("value").put("node", "AttributeReference").put("attribute", "gewicht").put("subjectType", objectType)
             .set("source", IrPath.sourceReference("het gewicht"));
        rule.put("initialisation", false).put("presentation", "calculation");
        return ir;
    }

    private static Path temporary(ObjectNode ir) throws Exception {
        Path tmp = Files.createTempFile("eigen-lezing", ".json");
        tmp.toFile().deleteOnExit();
        Files.writeString(tmp, ir.toString());
        return tmp;
    }
}
