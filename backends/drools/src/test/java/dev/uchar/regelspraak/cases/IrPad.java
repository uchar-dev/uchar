package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import dev.uchar.regelspraak.ir.ResolvedRegel;
import dev.uchar.regelspraak.ir.Spelling;

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

    /** A declaration of {@code parameters}, {@code factTypes}, {@code unitSystems} or {@code dimensions}, each named
     *  by {@code name} (a parameter and a unit system since L3, a fact type and a dimension since L5). */
    public static ObjectNode declaratie(JsonNode ir, String lijst, String naam) {
        return lid(modelLijst(ir, lijst), "name", naam);
    }

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

    /** The version record at {@code versie} of the named form at {@code vorm} in the rules list {@code lijst} (IR-SPEC
     *  ch. 3, Versioning: a form is its name and its versions, and a version carries no node of its own). */
    public static ObjectNode versie(JsonNode ir, String lijst, int vorm, int versie) {
        JsonNode v = rulesLijst(ir, lijst).path(vorm).path("versions").path(versie);
        if (!v.isObject()) throw new AssertionError("no version " + versie + " of form " + vorm + " in '" + lijst + "'");
        return (ObjectNode) v;
    }

    /** The named form called {@code naam} in the rules list {@code lijst}, failing LOUDLY when there is none: a twist
     *  that silently hit nothing would leave its witness testing the untwisted document. */
    public static ObjectNode vorm(JsonNode ir, String lijst, String naam) { return lid(rulesLijst(ir, lijst), "name", naam); }

    /** The first version of the named form called {@code naam} in the rules list {@code lijst}. */
    public static ObjectNode versie(JsonNode ir, String lijst, String naam) {
        JsonNode v = vorm(ir, lijst, naam).path("versions").path(0);
        if (!v.isObject()) throw new AssertionError("form '" + naam + "' in '" + lijst + "' has no version");
        return (ObjectNode) v;
    }

    /** Source {@code bron} of the one version of the {@code vorm}-th uniqueness (IR-SPEC ch. 3.6: the single-type
     *  form has one source, the union two or more, and both stand in the one list). */
    public static ObjectNode bron(JsonNode ir, int vorm, int bron) {
        JsonNode b = versie(ir, "uniquenesses", vorm, 0).path("sources").path(bron);
        if (!b.isObject()) throw new AssertionError("no source " + bron + " on uniqueness " + vorm);
        return (ObjectNode) b;
    }

    /** A Literal in the wire's shape (IR-SPEC ch. 4.1): its value is the variant its {@code type} names — a boolean
     *  written from the lexeme waar or onwaar, every other variant its string — chosen as the frontend's emitter
     *  chooses it, so a builder that writes a lexeme and a type keeps doing only that. */
    public static String literal(String lexeem, String type, String bron) {
        String variant = type.contains("\"Boolean\"") ? "boolean" : type.contains("\"Text\"") ? "text"
                : type.contains("\"millisecond\"") ? "dateTime" : type.contains("\"day\"") ? "date"
                : type.contains("\"Enumeration\"") ? "enumeration" : "number";
        String waarde = !variant.equals("boolean") ? "\"" + lexeem + "\""
                : switch (lexeem) {
                    case "waar" -> "true";
                    case "onwaar" -> "false";
                    default -> throw new AssertionError("no boolean lexeme: " + lexeem);
                };
        return "{\"node\":\"Literal\",\"value\":{\"" + variant + "\":" + waarde + "},\"type\":" + type
                + ",\"source\":{\"snippet\":\"" + bron + "\"}}";
    }

    /** A source record in the wire's shape (IR-SPEC ch. 4, B26): its snippet, without a line — a line stands in the
     *  record of a consistency version and a rule group alone. A builder sets it with {@code set("source", …)}. */
    public static ObjectNode bronrecord(String snippet) {
        return JsonNodeFactory.instance.objectNode().put("snippet", snippet);
    }

    /** A Rule of one always-valid version in the wire's shape (IR-SPEC ch. 3.1): {@code soort} names the kind the way
     *  the frontend's records name it and becomes the version's initialisation and presentation, the latter in its wire
     *  spelling through the translation table, and a {@code voorwaarde} of "null" writes no condition. */
    public static String regel(String naam, String soort, String doel, String waarde, String voorwaarde) {
        return regel(naam, soort, doel, waarde, voorwaarde,
                "{\"node\":\"Validity\"}");
    }

    /** As {@link #regel(String, String, String, String, String)}, with the version's {@code validity}. */
    public static String regel(String naam, String soort, String doel, String waarde, String voorwaarde,
                               String geldigheid) {
        String soortvelden = switch (soort) {
            case "GELIJKSTELLING", "BEREKENING" -> "\"initialisation\":false,\"presentation\":\""
                    + Spelling.PRESENTATION.spelling(ResolvedRegel.Soort.valueOf(soort)) + "\"";
            case "INITIALISATIE" -> "\"initialisation\":true";
            case "KENMERKTOEKENNING" -> "\"initialisation\":false";
            default -> throw new AssertionError("no rule kind: " + soort);
        };
        return "{\"node\":\"Rule\",\"name\":\"" + naam + "\",\"versions\":[{\"validity\":" + geldigheid
                + ",\"target\":" + doel + ",\"value\":" + waarde
                + (voorwaarde.equals("null") ? "" : ",\"condition\":" + voorwaarde) + "," + soortvelden + "}]}";
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
