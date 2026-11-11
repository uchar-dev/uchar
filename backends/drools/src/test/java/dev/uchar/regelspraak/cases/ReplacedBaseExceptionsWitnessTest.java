package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Where a subtype replaces the base of an inherited defaults group, the nested exception groups of that inherited base
 * stay (RUNTIME-SEMANTICS ch. 7.10, 7.9): they inherit as ordinary inherited exceptions above the subtype's own base,
 * and only the base's own consequence is replaced. The fixture is {@code corpus/golden/jutterswet-wrakbasis} with the
 * supertype's group reduced to one form-a default, "de grondslag" with the exception "99,00 indien betwist" nested in
 * it; the subtype {@code Wrakrecht} contributes its own base, once as an ordinary rule and once as a defaults group,
 * which writes 20,00 on a grondslag of 10,00. A betwist Wrakrecht gets 99,00 from the inherited exception.
 *
 * <p>Red before: both variants gave W2 20,00 on both branches, the nested exception replaced with its base.
 */
class ReplacedBaseExceptionsWitnessTest {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Path GOLDEN = Path.of("..", "..", "corpus", "golden", "jutterswet-wrakbasis");

    @TempDir Path tmp;

    @Test void the_inherited_exception_outranks_a_subtype_rule_as_base() throws Exception {
        BothBranches.green(document(false), betwistCase(), "subtype rule as base: W2 99,00");
    }

    @Test void the_inherited_exception_outranks_a_subtype_group_as_base() throws Exception {
        BothBranches.green(document(true), betwistCase(), "subtype group as base: W2 99,00");
    }

    /** The golden with the supertype's exception nested in its base; with the subtype's rule turned into a defaults
     *  group where {@code subtypeGroup}. */
    private Path document(boolean subtypeGroup) throws Exception {
        ObjectNode d = (ObjectNode) JSON.readTree(Files.readString(GOLDEN.resolve("ir.json")));
        ArrayNode groups = (ArrayNode) d.get("rules").get("prioritizedDefaults");
        ObjectNode group = (ObjectNode) groups.get(0);
        ArrayNode defaults = (ArrayNode) group.get("defaults");
        ObjectNode exception = (ObjectNode) defaults.get(0), base = (ObjectNode) defaults.get(1);
        ObjectNode nested = JSON.createObjectNode().put("unordered", false);
        nested.putArray("members").addObject().put("node", "ExceptionGroup").putArray("defaults").add(exception);
        base.set("exceptions", nested);
        group.set("defaults", JSON.createArrayNode().add(base));
        if (subtypeGroup) {
            ArrayNode rules = (ArrayNode) d.get("rules").get("rules");
            JsonNode version = rules.remove(0).get("versions").get(0);
            ObjectNode own = groups.addObject().put("node", "PrioritizedDefaults")
                    .put("name", "uitkering van een wrakrecht").put("subject", "Wrakrecht").put("attribute", "uitkering");
            ObjectNode own0 = own.putArray("defaults").addObject().put("node", "Default");
            own0.set("justification", JSON.readTree("{\"node\": \"Compound\", \"quantifier\": \"all\", \"threshold\": 0,"
                    + " \"parts\": []}"));
            own0.set("consequence", version.get("value"));
            own.putObject("validity").put("node", "Validity");
        }
        Path ir = tmp.resolve(subtypeGroup ? "group.json" : "rule.json");
        Files.writeString(ir, JSON.writeValueAsString(d));
        return ir;
    }

    /** Two Strandrechten and two Wrakrechten on a grondslag of 10,00, one of each betwist. */
    private static ObjectNode betwistCase() throws Exception {
        ObjectNode c = (ObjectNode) JSON.readTree(Files.readString(GOLDEN.resolve("case1-regel-als-basis.json")));
        ArrayNode instances = ((ObjectNode) c.get("input")).putArray("instances");
        instance(instances, "S1", "Strandrecht", false);
        instance(instances, "S2", "Strandrecht", true);
        instance(instances, "W1", "Wrakrecht", false);
        instance(instances, "W2", "Wrakrecht", true);
        ArrayNode expected = c.putArray("expected");
        expect(expected, "S1", "10.00");
        expect(expected, "S2", "99.00");
        expect(expected, "W1", "20.00");
        expect(expected, "W2", "99.00");
        return c;
    }

    private static void instance(ArrayNode instances, String label, String type, boolean betwist) {
        ObjectNode i = instances.addObject().put("label", label).put("objectType", type);
        i.putObject("attributes").putObject("grondslag").put("number", "10.00");
        i.putObject("roles");
        ArrayNode characteristics = i.putArray("characteristics");
        if (betwist) characteristics.add("betwist");
    }

    private static void expect(ArrayNode expected, String label, String value) {
        ObjectNode x = expected.addObject().put("kind", "attributeValue").put("label", label).put("attribute", "uitkering");
        x.putObject("value").put("number", value);
    }
}
