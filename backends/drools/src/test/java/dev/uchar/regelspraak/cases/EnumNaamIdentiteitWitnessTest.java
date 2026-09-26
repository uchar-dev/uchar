package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The ENUMERATION NAME IDENTITY at rule level, both transpiler branches (card `drools-enumnaam-identiteit`,
 * maintainer decision 2026-09-20: one identity for the whole square).
 *
 * <p><b>What was wrong.</b> An enumeration label is a DECLARED NAME. ref compares two labels up to
 * {@code Checks.norm} ({@code arith.ml}: {@code Enum a, Enum b -> norm a = norm b}) and the ASP backend stores
 * the normalised label, but both Drools branches compared bytes wherever a RULE compared enumerations: the
 * own-attribute, navigated and parameter condition arms, the attribute-against-attribute condition, the
 * assertie against a literal and against an attribute, and the uniqueness-key join of the single form and the
 * union. A case input spelled "Gaaf" against a declared "gaaf" therefore fired different rules in Drools than
 * in ref and clingo. The case comparator had already been put on name identity by card
 * `drools-enumlabel-lezer`; this is the rule side of the same identity. Both branches now read each
 * enumeration operand through {@code RegelSpraakRuntime.rsEnumNaam} ({@code Namen.norm}, null left null).
 *
 * <p><b>Red first, measured 2026-09-22 on the pre-fix jar.</b> On the golden {@code jutterswet-keurspelling},
 * ref and clingo PASS all four cases; both branches answered, identically:
 * <pre>
 *   case1 kenmerk veilbaar: verwacht aanwezig, werkelijk afwezig; kenmerk afgekeurd: verwacht afwezig,
 *         werkelijk aanwezig; kenmerk bevestigd: verwacht aanwezig, werkelijk afwezig; verwacht consistent,
 *         werkelijk inconsistent (herkeuring volgt de staat @ V1)
 *   case2 kenmerk bevestigd: verwacht aanwezig, werkelijk afwezig; verwacht inconsistent (geen wrakhout ter
 *         keuring @ V1), niet gevonden
 *   case4 kenmerk bevestigd (twice); verwacht inconsistent (eenmalige staat @ V1 / @ V2), niet gevonden
 * </pre>
 * On the two in-memory documents below, which clingo refuses at translation (a navigated or parameter
 * enumeration operand, an enumeration union key) and ref plays: both branches FAILED every name-identity
 * expectation (the navigated row was an exec-model FAIL too once the rule was written with a
 * {@code navigatieRol}, the written step of the time; since L2 the row is written with its binding, which both
 * branches read).
 *
 * <p><b>Why a class beside the golden.</b> Gate 12 replays the golden and pins its channel, but only as a
 * count; clingo's refusals keep the navigated, parameter and union arms out of the corpus entirely. These rows
 * hold both branches to every arm, and the Tekst row holds the other side: §13.3.3.4 gives text EXACT
 * equality, so the normalisation must not leak there.
 */
class EnumNaamIdentiteitWitnessTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    /** cwd under surefire is the module dir (backends/drools); the corpus is two levels up. */
    private static final Path GOLDEN = Path.of("..", "..", "corpus", "golden", "jutterswet-keurspelling");

    @TempDir Path tmp;

    // ---------------- 1. the golden, every case, both branches ----------------

    @Test void beide_takken_spelen_de_keurspelling() throws Exception {
        Path ir = GOLDEN.resolve("ir.json");
        List<Path> cases;
        try (java.util.stream.Stream<Path> s = Files.list(GOLDEN)) {
            cases = s.filter(p -> p.getFileName().toString().startsWith("case")).sorted().toList();
        }
        assertFalse(cases.isEmpty(), "de golden hoort casebestanden te dragen");
        for (Path c : cases)
            BeideTakken.groen(ir, JSON.readTree(Files.readString(c)), c.getFileName()
                    + ": een andere spelling van een gedeclareerd label is hetzelfde label");
    }

    // ---------------- 2. the arms clingo keeps out of the corpus ----------------

    /**
     * De genavigeerde arm (GELIJK en ONGELIJK tegen een literal) en de parameterarm, op één in-memory document:
     * de golden met een Kavel die een keur draagt, een naar-één-rol van Vondst naar Kavel, en een
     * enumeratieparameter. ref speelt dit document met exact deze verwachtingen (gemeten 2026-09-22); clingo
     * weigert het bij vertaling.
     */
    @Test void de_genavigeerde_en_de_parameterarm_lezen_een_naam() throws Exception {
        ObjectNode ir = golden();
        ObjectNode vondst = IrPad.objectType(ir, "Vondst");
        JsonNode keurtype = IrPad.attribute(vondst, "staat").get("datatype");

        ObjectNode kavel = vondst.deepCopy();
        kavel.put("name", "Kavel").put("plural", "Kavels");
        kavel.putArray("attributes");
        IrPad.addAttribute(kavel, "keur", keurtype.deepCopy());
        kavel.putArray("characteristics");
        IrPad.modelLijst(ir, "objectTypes").add(kavel);

        ArrayNode kenmerken = vondst.putArray("characteristics");
        for (String k : List.of("kavelgaaf", "kavelongaaf", "normconform"))
            kenmerken.addObject().put("node", "Characteristic").put("name", k).put("kind", "adjectival");
        IrPad.modelLijst(ir, "factTypes").add(JSON.readTree("""
                {"node": "FactType", "name": "kavelindeling", "reciprocal": false, "roles": [
                  {"node": "Role", "name": "kavel", "objectType": "Kavel", "many": false},
                  {"node": "Role", "name": "vondst", "objectType": "Vondst", "plural": "vondsten", "many": true}]}
                """));
        ObjectNode param = IrPad.modelLijst(ir, "parameters").addObject();
        param.put("node", "Parameter").put("name", "keurnorm");
        param.set("datatype", keurtype.deepCopy());

        ObjectNode staat = eigenStaat(ir);   // before the rules below replace the golden's own
        ArrayNode resolved = IrPad.rules(ir).putArray("rules");
        resolved.add(kenmerkRegel("keur van de kavel", "kavelgaaf", "equal",
                navigeerdeKeur(keurtype), literal(keurtype, "gaaf")));
        resolved.add(kenmerkRegel("ongave kavel", "kavelongaaf", "notEqual",
                navigeerdeKeur(keurtype), literal(keurtype, "gaaf")));
        ObjectNode normRef = JSON.createObjectNode();
        normRef.put("node", "ParameterReference").put("parameter", "keurnorm").set("source", IrPad.bronrecord("de keurnorm"));
        resolved.add(kenmerkRegel("keurnorm gehaald", "normconform", "equal",
                staat, normRef));
        IrPad.rules(ir).putArray("consistencies");
        IrPad.rules(ir).putArray("uniquenesses");

        Path doc = schrijf(ir, "genavigeerd-en-parameter.json");
        JsonNode c = JSON.readTree("""
                {"format": "uchar-case-v0", "asOfDate": null, "input": {"instances": [
                  {"label": "V1", "objectType": "Vondst", "attributes": {"staat": {"enumeration": "Gaaf"}},
                   "roles": {"kavel": ["K1"]}, "characteristics": []},
                  {"label": "K1", "objectType": "Kavel", "attributes": {"keur": {"enumeration": "GAAF"}},
                   "roles": {}, "characteristics": []}],
                  "parameters": {"keurnorm": {"enumeration": "gaaf"}}},
                 "expected": [
                  {"kind": "characteristicPresent", "label": "V1", "characteristic": "kavelgaaf"},
                  {"kind": "characteristicAbsent", "label": "V1", "characteristic": "kavelongaaf"},
                  {"kind": "characteristicPresent", "label": "V1", "characteristic": "normconform"}]}
                """);
        BeideTakken.groen(doc, c, "de keur 'GAAF' van de kavel en de keurnorm 'gaaf' zijn het label gaaf");
    }

    /**
     * De unievorm van de uniciteit: twee bronnen over hetzelfde type (staat verenigd met herkeuring), zodat
     * de botsing ALLEEN over de bronnen heen kan ontstaan — "Gaaf" als staat van V1 tegen "gaaf" als
     * herkeuring van V2. ref speelt dit met beide markeringen (gemeten 2026-09-22, na de fix van zijn
     * eigen sleutel); clingo weigert een enumeratiesleutel in een unie bij vertaling.
     */
    @Test void de_uniesleutel_is_een_naam() throws Exception {
        ObjectNode ir = golden();
        IrPad.rules(ir).putArray("consistencies");
        IrPad.rules(ir).set("uniquenesses", JSON.readTree("""
                [{ "node": "Uniqueness", "name": "eenmalig keuroordeel", "versions": [ {
                    "sources": [ { "node": "Source", "objectType": "Vondst", "keys": ["staat"] }, { "node": "Source", "objectType": "Vondst", "keys": ["herkeuring"] } ],
                    "validity": {"node": "Validity"},
                    "source": { "snippet": "Vondst verenigd met Vondst moeten uniek zijn." } } ] }]
                """));
        Path doc = schrijf(ir, "unie.json");
        BeideTakken.groen(doc, JSON.readTree("""
                {"format": "uchar-case-v0", "asOfDate": null, "input": {"instances": [
                  {"label": "V1", "objectType": "Vondst", "attributes": {"staat": {"enumeration": "Gaaf"},
                   "herkeuring": {"enumeration": "beschadigd"}}, "roles": {}, "characteristics": []},
                  {"label": "V2", "objectType": "Vondst", "attributes": {"staat": {"enumeration": "wrakhout"},
                   "herkeuring": {"enumeration": "gaaf"}}, "roles": {}, "characteristics": []}],
                  "parameters": {}},
                 "expected": [
                  {"kind": "inconsistent", "rule": "eenmalig keuroordeel", "label": "V1"},
                  {"kind": "inconsistent", "rule": "eenmalig keuroordeel", "label": "V2"}]}
                """), "de staat 'Gaaf' en de herkeuring 'gaaf' zijn één sleutel over de bronnen heen");
    }

    // ---------------- 3. the Tekst flank: the normalisation does not leak ----------------

    /**
     * Dezelfde golden met elk enumeratietype vervangen door Tekst en de invoer als tekst: §13.3.3.4 geeft
     * tekst EXACTE gelijkheid, dus "Gaaf" is hier NIET "gaaf". ref en clingo spelen deze lezing (gemeten
     * 2026-09-22); een normalisatie die naar Tekst lekte, vuurde veilbaar en bevestigd en verloor de
     * inconsistentie.
     */
    @Test void een_tekstvergelijking_blijft_exact() throws Exception {
        ObjectNode ir = golden();
        naarTekst(IrPad.modelLijst(ir, "objectTypes"));
        naarTekst(IrPad.rulesLijst(ir, "rules"));
        naarTekst(IrPad.rulesLijst(ir, "consistencies"));
        IrPad.model(ir).putArray("domains");
        assertFalse(ir.toString().contains("Enumeration"), "de variant hoort geen enumeratie meer te dragen");
        Path doc = schrijf(ir, "tekst.json");
        BeideTakken.groen(doc, JSON.readTree("""
                {"format": "uchar-case-v0", "asOfDate": null, "input": {"instances": [
                  {"label": "V1", "objectType": "Vondst", "attributes": {"staat": {"text": "Gaaf"},
                   "herkeuring": {"text": "gaaf"}}, "roles": {}, "characteristics": []}],
                  "parameters": {}},
                 "expected": [
                  {"kind": "characteristicAbsent", "label": "V1", "characteristic": "veilbaar"},
                  {"kind": "characteristicPresent", "label": "V1", "characteristic": "afgekeurd"},
                  {"kind": "characteristicAbsent", "label": "V1", "characteristic": "bevestigd"},
                  {"kind": "inconsistent", "rule": "herkeuring volgt de staat", "label": "V1"}]}
                """), "een Tekst vergelijkt byte-exact — de enumnormalisatie hoort daar niet te gelden");
    }

    // ---------------- fixtures ----------------

    private static ObjectNode golden() throws Exception {
        return (ObjectNode) JSON.readTree(Files.readString(GOLDEN.resolve("ir.json")));
    }

    private Path schrijf(ObjectNode ir, String naam) throws Exception {
        Path p = tmp.resolve(naam);
        Files.writeString(p, ir.toPrettyString());
        return p;
    }

    /** Every {@code Enumeration} datatype node under {@code n} replaced by a {@code Text} node, and the value of every
     *  literal typed so read as the text it names: a literal's value is the variant its type names (WF-114). */
    private static void naarTekst(JsonNode n) {
        if (n instanceof ObjectNode o) {
            if ("Literal".equals(o.path("node").asText()) && o.path("value").has("enumeration")) {
                String label = o.get("value").get("enumeration").asText();
                o.putObject("value").put("text", label);
            }
            var namen = new java.util.ArrayList<String>();
            o.fieldNames().forEachRemaining(namen::add);
            for (String k : namen) {
                JsonNode v = o.get(k);
                if (v.isObject() && "Enumeration".equals(v.path("node").asText()))
                    o.putObject(k).put("node", "Text");
                else naarTekst(v);
            }
        } else if (n instanceof ArrayNode a) {
            for (JsonNode x : a) naarTekst(x);
        }
    }

    private static ObjectNode literal(JsonNode type, String label) {
        ObjectNode l = JSON.createObjectNode();
        l.put("node", "Literal").putObject("value").put("Text".equals(type.path("node").asText()) ? "text" : "enumeration",
                label);
        l.set("type", type.deepCopy());
        l.set("source", IrPad.bronrecord("'" + label + "'"));
        return l;
    }

    /** "de keur van zijn kavel", bound over kavelindeling — the form the exec-model branch's navigated literal
     *  arm reads. */
    private static ObjectNode navigeerdeKeur(JsonNode type) {
        ObjectNode a = JSON.createObjectNode();
        a.put("node", "AttributeReference").put("attribute", "keur").put("subjectType", "Kavel");
        a.set("source", IrPad.bronrecord("de keur van zijn kavel"));
        a.putArray("path").addObject()
         .put("node", "RoleBinding").put("factType", "kavelindeling").put("role", "kavel");
        return a;
    }

    /** "zijn staat", lifted from the golden's own first rule so its shape is the emitter's. */
    private static ObjectNode eigenStaat(ObjectNode ir) {
        return (ObjectNode) IrPad.versie(ir, "rules", 0, 0).get("condition").get("left").deepCopy();
    }

    /** A KENMERKTOEKENNING "Een Vondst is {@code kenmerk} indien {@code links op rechts}". */
    private static ObjectNode kenmerkRegel(String naam, String kenmerk, String op,
                                           JsonNode links, JsonNode rechts) throws Exception {
        ObjectNode r = (ObjectNode) JSON.readTree("""
                {"node": "Rule", "name": "", "versions": [ {
                 "validity": {"node": "Validity"},
                 "target": {"node": "AttributeReference", "subjectType": "Vondst"},
                 "value": {"node": "Literal", "value": {"boolean": true}, "type": {"node": "Boolean"}, "source": { "snippet": "waar" }},
                 "initialisation": false } ] }
                """);
        r.put("name", naam);
        ObjectNode versie = (ObjectNode) r.get("versions").get(0);
        ((ObjectNode) versie.get("target")).put("attribute", kenmerk)
                .set("source", IrPad.bronrecord("Een Vondst is " + kenmerk));
        ObjectNode v = JSON.createObjectNode();
        v.put("node", "Comparison").put("op", op);
        v.set("left", links);
        v.set("right", rechts);
        v.set("source", IrPad.bronrecord(links.path("source").path("snippet").asText() + " " + op.toLowerCase() + " "
                + rechts.path("source").path("snippet").asText()));
        versie.set("condition", v);
        assertTrue(versie.get("target").has("attribute"));
        return r;
    }
}
