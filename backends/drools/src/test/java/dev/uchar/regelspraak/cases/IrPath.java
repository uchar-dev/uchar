package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import dev.uchar.regelspraak.ir.ResolvedRule;
import dev.uchar.regelspraak.ir.Spelling;

/**
 * Navigation into a {@code uchar-ir-v1} document for the witnesses that twist a corpus golden in memory. Since M1
 * a declaration is a list entry that carries its own name instead of a map entry under its name, so every
 * "the declaration called X" becomes a search; this class does that search once, and fails LOUDLY when the name
 * is absent — a twist that silently hit nothing would leave its witness testing the untwisted document.
 */
public final class IrPath {
    private IrPath() {}

    public static ObjectNode model(JsonNode ir) { return field(ir, "model"); }

    public static ObjectNode rules(JsonNode ir) { return field(ir, "rules"); }

    /** A list under {@code model} ({@code objectTypes}, {@code parameters}, …) or under {@code rules}. */
    public static ArrayNode modelList(JsonNode ir, String list) { return list(model(ir), list); }

    public static ArrayNode rulesList(JsonNode ir, String list) { return list(rules(ir), list); }

    public static ObjectNode objectType(JsonNode ir, String name) { return memberWith(modelList(ir, "objectTypes"), "name", name); }

    public static ObjectNode domain(JsonNode ir, String name) { return memberWith(modelList(ir, "domains"), "name", name); }

    /** A declaration of {@code parameters}, {@code factTypes}, {@code unitSystems} or {@code dimensions}, each named
     *  by {@code name} (a parameter and a unit system since L3, a fact type and a dimension since L5). */
    public static ObjectNode declaration(JsonNode ir, String list, String name) {
        return memberWith(modelList(ir, list), "name", name);
    }

    public static ObjectNode attribute(JsonNode objectType, String name) { return memberWith(list(objectType, "attributes"), "name", name); }

    public static ObjectNode characteristic(JsonNode objectType, String name) {
        return memberWith(list(objectType, "characteristics"), "name", name);
    }

    /** Appends an attribute node, the M1 form of a new entry in the v0 {@code attributen} map. */
    public static ObjectNode addAttribute(JsonNode objectType, String name, JsonNode datatype) {
        ObjectNode a = list(objectType, "attributes").addObject();
        a.put("node", "Attribute").put("name", name).set("datatype", datatype);
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

    /** The version record at {@code version} of the named form at {@code form} in the rules list {@code list} (IR-SPEC
     *  ch. 3, Versioning: a form is its name and its versions, and a version carries no node of its own). */
    public static ObjectNode version(JsonNode ir, String list, int form, int version) {
        JsonNode v = rulesList(ir, list).path(form).path("versions").path(version);
        if (!v.isObject()) throw new AssertionError("no version " + version + " of form " + form + " in '" + list + "'");
        return (ObjectNode) v;
    }

    /** The named form called {@code naam} in the rules list {@code list}, failing LOUDLY when there is none: a twist
     *  that silently hit nothing would leave its witness testing the untwisted document. */
    public static ObjectNode form(JsonNode ir, String list, String name) { return memberWith(rulesList(ir, list), "name", name); }

    /** The first version of the named form called {@code naam} in the rules list {@code list}. */
    public static ObjectNode version(JsonNode ir, String list, String name) {
        JsonNode v = form(ir, list, name).path("versions").path(0);
        if (!v.isObject()) throw new AssertionError("form '" + name + "' in '" + list + "' has no version");
        return (ObjectNode) v;
    }

    /** Source {@code source} of the one version of the {@code form}-th uniqueness (IR-SPEC ch. 3.6: the single-type
     *  form has one source, the union two or more, and both stand in the one list). */
    public static ObjectNode source(JsonNode ir, int form, int source) {
        JsonNode b = version(ir, "uniquenesses", form, 0).path("sources").path(source);
        if (!b.isObject()) throw new AssertionError("no source " + source + " on uniqueness " + form);
        return (ObjectNode) b;
    }

    /** A Literal in the wire's shape (IR-SPEC ch. 4.1): its value is the variant its {@code type} names — a boolean
     *  written from the lexeme waar or onwaar, every other variant its string — chosen as the frontend's emitter
     *  chooses it, so a builder that writes a lexeme and a type keeps doing only that. */
    public static String literal(String lexeme, String type, String source) {
        String variant = type.contains("\"Boolean\"") ? "boolean" : type.contains("\"Text\"") ? "text"
                : type.contains("\"millisecond\"") ? "dateTime" : type.contains("\"day\"") ? "date"
                : type.contains("\"Enumeration\"") ? "enumeration" : "number";
        String value = !variant.equals("boolean") ? "\"" + lexeme + "\""
                : switch (lexeme) {
                    case "waar" -> "true";
                    case "onwaar" -> "false";
                    default -> throw new AssertionError("no boolean lexeme: " + lexeme);
                };
        return "{\"node\":\"Literal\",\"value\":{\"" + variant + "\":" + value + "},\"type\":" + type
                + ",\"source\":{\"snippet\":\"" + source + "\"}}";
    }

    /** A source record in the wire's shape (IR-SPEC ch. 4, B26): its snippet, without a line — a line stands in the
     *  record of a consistency version and a rule group alone. A builder sets it with {@code set("source", …)}. */
    public static ObjectNode sourceReference(String snippet) {
        return JsonNodeFactory.instance.objectNode().put("snippet", snippet);
    }

    /** A Rule of one always-valid version in the wire's shape (IR-SPEC ch. 3.1): {@code category} names the kind the way
     *  the frontend's records name it and becomes the version's initialisation and presentation, the latter in its wire
     *  spelling through the translation table, and a {@code condition} of "null" writes no condition. */
    public static String rule(String name, String category, String target, String value, String condition) {
        return rule(name, category, target, value, condition,
                "{\"node\":\"Validity\"}");
    }

    /** As {@link #rule(String, String, String, String, String)}, with the version's {@code validity}. */
    public static String rule(String name, String category, String target, String value, String condition,
                               String validity) {
        String kindFields = switch (category) {
            case "GELIJKSTELLING", "BEREKENING" -> "\"initialisation\":false,\"presentation\":\""
                    + Spelling.PRESENTATION.spelling(ResolvedRule.Kind.valueOf(category)) + "\"";
            case "INITIALISATIE" -> "\"initialisation\":true";
            case "KENMERKTOEKENNING" -> "\"initialisation\":false";
            default -> throw new AssertionError("no rule kind: " + category);
        };
        return "{\"node\":\"Rule\",\"name\":\"" + name + "\",\"versions\":[{\"validity\":" + validity
                + ",\"target\":" + target + ",\"value\":" + value
                + (condition.equals("null") ? "" : ",\"condition\":" + condition) + "," + kindFields + "}]}";
    }

    public static ObjectNode memberWith(ArrayNode list, String nameField, String name) {
        for (JsonNode n : list)
            if (name.equals(n.path(nameField).asText(null))) return (ObjectNode) n;
        throw new AssertionError("no entry with " + nameField + " '" + name + "' in " + list);
    }

    private static ObjectNode field(JsonNode n, String name) {
        JsonNode v = n.get(name);
        if (v == null || !v.isObject()) throw new AssertionError("no object '" + name + "' in the document");
        return (ObjectNode) v;
    }

    private static ArrayNode list(JsonNode n, String name) {
        JsonNode v = n.get(name);
        if (v == null || !v.isArray()) throw new AssertionError("no list '" + name + "' here");
        return (ArrayNode) v;
    }
}
