package io.github.yhavinga.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Navigation into a {@code uchar-ir-v1} document for the witnesses that twist a corpus golden in memory. Since M1
 * a declaration is a list entry that carries its own name instead of a map entry under its name, so every
 * "the declaration called X" becomes a search; this class does that search once, and fails LOUDLY when the name
 * is absent — a twist that silently hit nothing would leave its witness testing the untwisted document.
 */
public final class IrPad {
    private IrPad() {}

    public static ObjectNode model(JsonNode ir) { return veld(ir, "model"); }

    public static ObjectNode rules(JsonNode ir) { return veld(ir, "rules"); }

    /** A list under {@code model} ({@code objectTypes}, {@code parameters}, …) or under {@code rules}. */
    public static ArrayNode modelLijst(JsonNode ir, String lijst) { return lijst(model(ir), lijst); }

    public static ArrayNode rulesLijst(JsonNode ir, String lijst) { return lijst(rules(ir), lijst); }

    public static ObjectNode objectType(JsonNode ir, String naam) { return lid(modelLijst(ir, "objectTypes"), "name", naam); }

    public static ObjectNode domain(JsonNode ir, String naam) { return lid(modelLijst(ir, "domains"), "name", naam); }

    /** A declaration whose node kept its v0 shape: {@code parameters}, {@code factTypes}, {@code unitSystems},
     *  {@code dimensions} — named by {@code naam}. */
    public static ObjectNode declaratie(JsonNode ir, String lijst, String naam) { return lid(modelLijst(ir, lijst), "naam", naam); }

    public static ObjectNode attribute(JsonNode objectType, String naam) { return lid(lijst(objectType, "attributes"), "name", naam); }

    public static ObjectNode characteristic(JsonNode objectType, String naam) {
        return lid(lijst(objectType, "characteristics"), "name", naam);
    }

    /** Appends an attribute node, the M1 form of a new entry in the v0 {@code attributen} map. */
    public static ObjectNode addAttribute(JsonNode objectType, String naam, JsonNode datatype) {
        ObjectNode a = lijst(objectType, "attributes").addObject();
        a.put("node", "Attribute").put("name", naam).set("datatype", datatype);
        return a;
    }

    /** Sets a member's timeline facet, the M1 form of a v0 granularity entry. A member that already carries one
     *  fails LOUDLY: replacing it would silently drop its count and timeline name, and the twist would test a
     *  document other than the one its witness describes. */
    public static ObjectNode timeline(JsonNode member, String granularity) {
        if (member.has("timeline"))
            throw new AssertionError("member '" + member.path("name").asText() + "' already carries a timeline facet: "
                    + member.get("timeline"));
        ObjectNode t = ((ObjectNode) member).putObject("timeline");
        t.put("node", "TimelineFacet").put("granularity", granularity).put("count", 1);
        return t;
    }

    public static ObjectNode lid(ArrayNode lijst, String naamVeld, String naam) {
        for (JsonNode n : lijst)
            if (naam.equals(n.path(naamVeld).asText(null))) return (ObjectNode) n;
        throw new AssertionError("no entry with " + naamVeld + " '" + naam + "' in " + lijst);
    }

    private static ObjectNode veld(JsonNode n, String naam) {
        JsonNode v = n.get(naam);
        if (v == null || !v.isObject()) throw new AssertionError("no object '" + naam + "' in the document");
        return (ObjectNode) v;
    }

    private static ArrayNode lijst(JsonNode n, String naam) {
        JsonNode v = n.get(naam);
        if (v == null || !v.isArray()) throw new AssertionError("no list '" + naam + "' here");
        return (ArrayNode) v;
    }
}
