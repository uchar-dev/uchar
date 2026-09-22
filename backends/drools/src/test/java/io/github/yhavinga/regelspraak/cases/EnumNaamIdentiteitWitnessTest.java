package io.github.yhavinga.regelspraak.cases;

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
 * {@code navigatieRol}; the binding form of that rule is not carried by the exec-model arm at all).
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
        ObjectNode vondst = (ObjectNode) ir.get("objecttypen").get("Vondst");
        JsonNode keurtype = vondst.get("attributen").get("staat");

        ObjectNode kavel = vondst.deepCopy();
        kavel.put("naam", "Kavel").put("meervoud", "Kavels");
        kavel.putObject("attributen").set("keur", keurtype.deepCopy());
        kavel.putObject("attribuutMeervoud");
        kavel.putArray("kenmerken");
        kavel.putObject("kenmerkSoorten");
        ((ObjectNode) ir.get("objecttypen")).set("Kavel", kavel);

        ArrayNode kenmerken = vondst.putArray("kenmerken");
        ObjectNode soorten = vondst.putObject("kenmerkSoorten");
        for (String k : List.of("kavelgaaf", "kavelongaaf", "normconform")) {
            kenmerken.add(k);
            soorten.put(k, "BIJVOEGLIJK");
        }
        ((ObjectNode) ir.get("feittypen")).set("kavelindeling", JSON.readTree("""
                {"node": "Feittype", "naam": "kavelindeling", "wederkerig": false, "rollen": [
                  {"node": "Rol", "naam": "kavel", "objecttypeNaam": "Kavel", "meervoud": null, "meervoudig": false},
                  {"node": "Rol", "naam": "vondst", "objecttypeNaam": "Vondst", "meervoud": "vondsten", "meervoudig": true}]}
                """));
        ObjectNode param = ((ObjectNode) ir.get("parameters")).putObject("keurnorm");
        param.put("node", "Parameter").put("naam", "keurnorm");
        param.set("datatype", keurtype.deepCopy());
        param.putNull("granulariteit");

        ObjectNode staat = eigenStaat(ir);   // before the rules below replace the golden's own
        ArrayNode regels = ir.putArray("regels");
        ArrayNode resolved = ir.putArray("resolvedRegels");
        resolved.add(kenmerkRegel(regels, "keur van de kavel", "kavelgaaf", "GELIJK",
                navigeerdeKeur(keurtype), literal(keurtype, "gaaf")));
        resolved.add(kenmerkRegel(regels, "ongave kavel", "kavelongaaf", "ONGELIJK",
                navigeerdeKeur(keurtype), literal(keurtype, "gaaf")));
        ObjectNode normRef = JSON.createObjectNode();
        normRef.put("node", "ParameterRef").put("parameter", "keurnorm").put("bron", "de keurnorm");
        normRef.set("type", keurtype.deepCopy());
        resolved.add(kenmerkRegel(regels, "keurnorm gehaald", "normconform", "GELIJK",
                staat, normRef));
        ir.putArray("consistenties");
        ir.putArray("uniciteiten");

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
        ir.putArray("consistenties");
        ir.putArray("uniciteiten");
        ir.set("uniciteitUnies", JSON.readTree("""
                [{"node": "ResolvedUniciteitUnie", "naam": "eenmalig keuroordeel", "bronnen": [
                   {"node": "Bron", "type": "Vondst", "sleutels": ["staat"]},
                   {"node": "Bron", "type": "Vondst", "sleutels": ["herkeuring"]}],
                  "bron": "Vondst verenigd met Vondst moeten uniek zijn.",
                  "geldigheid": {"node": "Geldigheid", "vanaf": null, "tot": null}}]
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
        naarTekst(ir.get("objecttypen"));
        naarTekst(ir.get("resolvedRegels"));
        naarTekst(ir.get("consistenties"));
        ir.putObject("domeinen");
        assertFalse(ir.toString().contains("Enumeratie"), "de variant hoort geen enumeratie meer te dragen");
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

    /** Every {@code Enumeratie} datatype node under {@code n} replaced by a {@code Tekst} node. */
    private static void naarTekst(JsonNode n) {
        if (n instanceof ObjectNode o) {
            var namen = new java.util.ArrayList<String>();
            o.fieldNames().forEachRemaining(namen::add);
            for (String k : namen) {
                JsonNode v = o.get(k);
                if (v.isObject() && "Enumeratie".equals(v.path("node").asText()))
                    o.putObject(k).put("node", "Tekst");
                else naarTekst(v);
            }
        } else if (n instanceof ArrayNode a) {
            for (JsonNode x : a) naarTekst(x);
        }
    }

    private static ObjectNode literal(JsonNode type, String label) {
        ObjectNode l = JSON.createObjectNode();
        l.put("node", "Literal").put("waarde", label);
        l.set("type", type.deepCopy());
        l.put("bron", "'" + label + "'");
        return l;
    }

    /** "de keur van zijn kavel", written with a {@code navigatieRol} — the form the exec-model branch's
     *  navigated literal arm reads. */
    private static ObjectNode navigeerdeKeur(JsonNode type) {
        ObjectNode a = JSON.createObjectNode();
        a.put("node", "AttribuutRef").put("attribuut", "keur").put("objecttype", "Kavel")
         .put("onderwerpBron", "zijn kavel").put("navigatieRol", "kavel");
        a.set("type", type.deepCopy());
        a.put("bron", "de keur van zijn kavel").put("set", false);
        a.putArray("navigatiePad");
        return a;
    }

    /** "zijn staat", lifted from the golden's own first rule so its shape is the emitter's. */
    private static ObjectNode eigenStaat(ObjectNode ir) {
        return (ObjectNode) ir.get("resolvedRegels").get(0).get("voorwaarde").get("links").deepCopy();
    }

    /** A KENMERKTOEKENNING "Een Vondst is {@code kenmerk} indien {@code links op rechts}", registered in
     *  {@code regels} as well. */
    private static ObjectNode kenmerkRegel(ArrayNode regels, String naam, String kenmerk, String op,
                                           JsonNode links, JsonNode rechts) throws Exception {
        regels.addObject().put("node", "Regel").put("naam", naam).put("soort", "regel");
        ObjectNode r = (ObjectNode) JSON.readTree("""
                {"node": "ResolvedRegel", "soort": "KENMERKTOEKENNING",
                 "doel": {"node": "AttribuutRef", "objecttype": "Vondst", "onderwerpBron": "Een Vondst",
                          "navigatieRol": null, "type": {"node": "Booleaan"}, "set": false, "navigatiePad": []},
                 "waarde": {"node": "Literal", "waarde": "waar", "type": {"node": "Booleaan"}, "bron": "waar"},
                 "geldigheid": {"node": "Geldigheid", "vanaf": null, "tot": null}}
                """);
        r.put("naam", naam);
        ((ObjectNode) r.get("doel")).put("attribuut", kenmerk).put("bron", "Een Vondst is " + kenmerk);
        ObjectNode v = r.putObject("voorwaarde");
        v.put("node", "Vergelijking").put("op", op);
        v.set("links", links);
        v.set("rechts", rechts);
        v.putNull("periode");
        v.put("bron", links.path("bron").asText() + " " + op.toLowerCase() + " " + rechts.path("bron").asText());
        assertTrue(r.get("doel").has("attribuut"));
        return r;
    }
}
