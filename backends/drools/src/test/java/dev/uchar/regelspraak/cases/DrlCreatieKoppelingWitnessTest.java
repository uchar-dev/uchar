package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import dev.uchar.regelspraak.loader.IrLoader;
import dev.uchar.regelspraak.transpiler.DrlTranspiler;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The §13.4.6 objectcreatie's LINK, resolved the same way in both transpiler branches (card
 * drl-creatie-koppelloos, 2026-09-23).
 *
 * <p>The exec branch has read the creation's own {@code connectingRole} binding since 2026-08-30, and refuses
 * by name every creation whose link the bean model cannot hold. The DRL-text branch derived the link from the
 * type pair alone. On a repeated or self-typed pair that derivation answers null, and the branch then emitted
 * an {@code insert($new)} with no link at all, behind a comment line only the marker scan in {@code meet()}
 * caught. Where one end was displaced by a same-named member it wrote the other end and played on. Where the
 * subject-side role was to-many it wrote a setter no bean carries, and the whole package failed to compile
 * without naming the form. And a non-boolean characteristic initialisation read as "unset" where ref and the
 * exec branch fault (WF-027). Every row below demands one verdict of BOTH branches.
 *
 * <p>The models are corpus goldens' {@code ir.json} or a programmatic variant of one; each variant was run
 * through the verifier, ref and clingo first (all PASS on the played cases; the WF-027 row is verifier-FAIL
 * and a ref EVALUATION FAULT by design). No corpus file is read for its expectations, moved or touched.
 */
class DrlCreatieKoppelingWitnessTest {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Path CORPUS = Path.of("..", "..", "corpus", "golden");

    /** Jutter -> Rekenkamer over `taxatieopdracht`, 1:1; the jutter reads its heffing through the mint. */
    private static final String REKENKAMER = "anchor/jutterswet-rekenkamer";
    /** The same shape with a kenmerkwaardetoekenning (`overzees`) on the mint. */
    private static final String OVERZEE = "anchor/jutterswet-rekenkamer-overzee";
    /** A Wachter creates a Wachter over `opvolging`: two distinct sibling roles, both filled by Wachter. */
    private static final String WACHTERSKETEN = "anchor/jutterswet-wachtersketen";
    /** Jutter and Insigne related by two fact types, and a creation with no binding. */
    private static final Path AMBIGU = Path.of("..", "..", "corpus", "rood", "ambiguous-backlink", "ir.json");

    // ---------------- the binding: repeated and self-typed pairs ----------------

    @Test void een_herhaald_paar_koppelt_via_de_binding_in_beide_takken() throws Exception {
        // A second fact type between Jutter and Rekenkamer makes the type-pair derivation ambiguous; the
        // creation's binding (taxatieopdracht.rekenkamer) still names the pair. ref: J1.heffing = 150.
        BeideTakken.groen(herhaaldPaar(), jutter("150"),
                "the creation's binding resolves the link on a repeated pair");
    }

    @Test void het_herhaalde_paar_leest_in_beide_takken_een_echte_waarde() throws Exception {
        BeideTakken.rood(herhaaldPaar(), jutter("149"), "heffing",
                "the navigated read through the mint must be observably 150");
    }

    @Test void een_zelfgetypeerd_paar_koppelt_via_de_binding_in_beide_takken() throws Exception {
        // ref on this model and case: W1.opvolgerrang = 5, rangdetector violated by the mint (rank 5).
        BeideTakken.groen(ir(WACHTERSKETEN), wachter("5"),
                "the creation's binding resolves the link on a self-typed pair");
        BeideTakken.rood(ir(WACHTERSKETEN), wachter("4"), "opvolgerrang",
                "the fold over the mint must be observably 5");
    }

    @Test void de_gegenereerde_drl_draagt_geen_koppelloze_insert() throws Exception {
        // The transpiler itself, not the replay: before the fix the rule text held
        // "// onopgelost terugkoppeling naar Wachter" followed by insert($new), and only meet()'s scan refused it.
        String drl = drl(ir(WACHTERSKETEN));
        assertFalse(drl.contains("// onopgelost"), drl);
        assertTrue(drl.contains("$new.setVoorganger( $subject );"), "the mint references the subject: " + drl);
        assertTrue(drl.contains("setOpvolger( $new )"), "and the subject references the mint: " + drl);
        assertTrue(drl.contains("opvolger == null"), "the 1:1 guard reads the forward field: " + drl);
    }

    @Test void een_binding_zonder_rolpaar_wordt_in_beide_takken_bij_naam_geweigerd() throws Exception {
        // verifier: WF-047 FAIL. The branches refuse by the same sentence rather than guess.
        BeideTakken.geweigerd(metBinding(REKENKAMER, "taxatieopdracht", "bewaker"), jutter("150"),
                "wijst geen rolpaar aan", "a binding that names no declared role");
    }

    // ---------------- ends the bean model cannot hold: refused by name ----------------

    @Test void een_verdrongen_voorwaarts_veld_wordt_in_beide_takken_geweigerd() throws Exception {
        // A Numeriek `rekenkamer` on Jutter displaces the forward reference. Until this card the DRL-text
        // branch wrote only the back-reference and played.
        BeideTakken.geweigerd(gelijknamigLid("Jutter", "rekenkamer"), jutterZonderLezing(),
                "de rol 'rekenkamer' op Jutter deelt haar naam met een gedeclareerd lid",
                "a displaced forward end is refused by name");
    }

    @Test void een_verdrongen_terugverwijzing_wordt_in_beide_takken_geweigerd() throws Exception {
        // A Numeriek `opdrachtgever` on Rekenkamer displaces the back-reference. The DRL-text branch used to
        // emit the write anyway, and the package failed to compile without naming the form.
        BeideTakken.geweigerd(gelijknamigLid("Rekenkamer", "opdrachtgever"), jutterZonderLezing(),
                "de rol 'opdrachtgever' op Rekenkamer deelt haar naam met een gedeclareerd lid",
                "a displaced back-reference is refused by name");
    }

    @Test void twee_meervoudige_rollen_worden_in_beide_takken_geweigerd() throws Exception {
        BeideTakken.geweigerd(cardinaliteit(true, true), jutterZonderLezing(),
                "beide rollen zijn meervoudig", "no end carries a bean field");
    }

    @Test void een_meervoudige_subjectzijdige_rol_speelt_in_beide_takken() throws Exception {
        // The mint carries no back-reference; the jutter's forward field carries the link. The DRL-text
        // branch used to write the back-reference and guard on it, and failed to compile. ref: 150.
        BeideTakken.groen(schrijf(cardinaliteitIr(true, false), "subjectrol-veel"), jutter("150"),
                "one writable end is enough to play");
    }

    @Test void een_ambigu_paar_zonder_binding_wordt_in_de_drl_tak_bij_naam_geweigerd() throws Exception {
        // The exec branch lowers this to a rule that faults at fire (ObjectCreatieWitnessTest); the DRL-text
        // branch has no fault-rule idiom and refuses it with the same sentence instead of minting unlinked.
        CaseRunner.Excluded e = assertThrows(CaseRunner.Excluded.class,
                () -> CaseRunner.speelCase(AMBIGU, jutterZonderLezing(), true));
        assertTrue(e.getMessage().contains("the created object's back-link is ambiguous"), e.getMessage());
    }

    // ---------------- WF-027: a non-boolean characteristic initialisation faults ----------------

    @Test void een_niet_booleaanse_kenmerkwaarde_faalt_in_beide_takken() throws Exception {
        // ref: EVALUATION FAULT "characteristic initialisation of "overzees" is not boolean: 5"; the verifier rejects
        // the document (WF-027). The DRL-text branch faults at fire; the truth-maintenance branch refuses the creation
        // by name when it lowers it, on the effective type of the value, so no case reaches a fire there.
        Path ir = kenmerkGetal();
        RuntimeException e = assertThrows(RuntimeException.class, () -> CaseRunner.speelCase(ir, overzeeJutter(), true));
        String keten = oorzaken(e);
        assertTrue(keten.contains("niet booleaans: 5") && keten.contains("(WF-027)"), "drl-tekst: " + keten);
        CaseRunner.Excluded w = assertThrows(CaseRunner.Excluded.class,
                () -> CaseRunner.speelCase(ir, overzeeJutter(), DroolsBackend.Branch.TRUTH_MAINTENANCE));
        assertTrue(w.getMessage().contains("een kenmerkinitialisatie van 'overzees' met een waarde van type Numeriek"
                + " ('5'): een kenmerk is waar of onwaar (WF-027)"), "tms: " + w.getMessage());
    }

    // ---------------- the authored documents ----------------

    private static Path ir(String golden) {
        return CORPUS.resolve(golden).resolve("ir.json");
    }

    private static ObjectNode lees(String golden) throws Exception {
        return (ObjectNode) JSON.readTree(Files.readString(ir(golden)));
    }

    private static String drl(Path ir) throws Exception {
        IrLoader.Loaded l = IrLoader.loadAll(JSON.readTree(Files.readString(ir)));
        return DrlTranspiler.build(l.model(), l.lets(), l.prioritizedDefaults(), l.bronPaden(),
                l.rolBindingen(), l.navigatieBindingen()).drl();
    }

    /** The rekenkamer model plus a second fact type `reservetaxatie` between Jutter and Rekenkamer. */
    private static Path herhaaldPaar() throws Exception {
        ObjectNode ir = lees(REKENKAMER);
        IrPad.modelLijst(ir, "factTypes").add(JSON.readTree("""
                { "node": "FactType", "name": "reservetaxatie", "reciprocal": false, "roles": [
                    { "node": "Role", "name": "reserveopdrachtgever", "objectType": "Jutter",
                      "many": false },
                    { "node": "Role", "name": "reserverekenkamer", "objectType": "Rekenkamer",
                      "many": false } ] }"""));
        return schrijf(ir, "herhaald-paar");
    }

    private static Path metBinding(String golden, String feittype, String rol) throws Exception {
        ObjectNode ir = lees(golden);
        IrPad.versie(ir, "objectCreations", 0, 0).set("connectingRole", JSON.readTree(
                "{ \"node\": \"RoleBinding\", \"factType\": \"" + feittype + "\", \"role\": \"" + rol + "\" }"));
        return schrijf(ir, "binding-" + rol);
    }

    /** A Numeriek attribute named after a link role on {@code objecttype}, and without the rule that reads
     *  through the created-side role (it would refuse on its own §13.3.9 reason first). */
    private static Path gelijknamigLid(String objecttype, String rol) throws Exception {
        ObjectNode ir = zonderNavigerendeRegel(lees(REKENKAMER));
        IrPad.addAttribute(IrPad.objectType(ir, objecttype), rol, JSON.readTree("""
                { "node": "Number", "whole": true, "decimals": 0, "sign": "unspecified" }"""));
        return schrijf(ir, "lid-" + rol);
    }

    private static Path cardinaliteit(boolean subjectVeel, boolean gemunteVeel) throws Exception {
        return schrijf(zonderNavigerendeRegel(cardinaliteitIr(subjectVeel, gemunteVeel)), "beide-veel");
    }

    /** Role 0 of taxatieopdracht is the subject side (`opdrachtgever`), role 1 the created side (`rekenkamer`). */
    private static ObjectNode cardinaliteitIr(boolean subjectVeel, boolean gemunteVeel) throws Exception {
        ObjectNode ir = lees(REKENKAMER);
        JsonNode rollen = IrPad.declaratie(ir, "factTypes", "taxatieopdracht").get("roles");
        ((ObjectNode) rollen.get(0)).put("many", subjectVeel);
        ((ObjectNode) rollen.get(1)).put("many", gemunteVeel);
        return ir;
    }

    private static ObjectNode zonderNavigerendeRegel(ObjectNode ir) {
        var behouden = JSON.createArrayNode();
        for (JsonNode r : IrPad.rulesLijst(ir, "rules"))
            if (!"heffing van een jutter".equals(r.path("name").asText())) behouden.add(r);
        if (behouden.size() != IrPad.rulesLijst(ir, "rules").size() - 1)
            throw new AssertionError("'heffing van een jutter' is not in the document");
        IrPad.rules(ir).set("rules", behouden);
        return ir;
    }

    /** The overzee model with the kenmerk toekenning's value replaced by the Numeriek literal 5. */
    private static Path kenmerkGetal() throws Exception {
        ObjectNode ir = lees(OVERZEE);
        for (JsonNode t : IrPad.versie(ir, "objectCreations", 0, 0).get("initialisations"))
            if (t.get("characteristic").asBoolean())
                ((ObjectNode) t).set("value", JSON.readTree("""
                        { "node": "Literal", "value": { "number": "5" }, "type": { "node": "Number", "whole": true, "decimals": 0, "sign": "unspecified" }, "source": { "snippet": "5" } }"""));
        return schrijf(ir, "kenmerk-getal");
    }

    private static Path schrijf(JsonNode ir, String naam) throws Exception {
        Path p = Files.createTempDirectory("uchar-drlkoppeling-" + naam).resolve("ir.json");
        Files.writeString(p, JSON.writerWithDefaultPrettyPrinter().writeValueAsString(ir));
        p.toFile().deleteOnExit();
        return p;
    }

    private static JsonNode jutter(String heffing) throws Exception {
        return JSON.readTree(("""
                { "format": "uchar-case-v0", "asOfDate": null,
                  "input": { "instances": [ { "label": "J1", "objectType": "Jutter",
                      "attributes": { "grondslag": { "number": "240" }, "aftrek": { "number": "90" } },
                      "roles": {}, "characteristics": [] } ], "parameters": {} },
                  "expected": [ { "kind": "attributeValue", "label": "J1", "attribute": "heffing",
                                  "value": { "number": "%s" } },
                                { "kind": "consistent" } ] }
                """).formatted(heffing));
    }

    private static JsonNode jutterZonderLezing() throws Exception {
        return JSON.readTree("""
                { "format": "uchar-case-v0", "asOfDate": null,
                  "input": { "instances": [ { "label": "J1", "objectType": "Jutter",
                      "attributes": { "grondslag": { "number": "240" }, "aftrek": { "number": "90" } },
                      "roles": {}, "characteristics": [] } ], "parameters": {} },
                  "expected": [ { "kind": "attributeEmpty", "label": "J1", "attribute": "heffing" },
                                { "kind": "consistent" } ] }
                """);
    }

    private static JsonNode overzeeJutter() throws Exception {
        return JSON.readTree("""
                { "format": "uchar-case-v0", "asOfDate": null,
                  "input": { "instances": [ { "label": "J1", "objectType": "Jutter",
                      "attributes": { "grondslag": { "number": "800" } },
                      "roles": {}, "characteristics": [] } ], "parameters": {} },
                  "expected": [ { "kind": "attributeValue", "label": "J1", "attribute": "heffing",
                                  "value": { "number": "800" } },
                                { "kind": "consistent" } ] }
                """);
    }

    /** W1 at rank 4: the chain advances one generation, so W1.opvolgerrang is the mint's rank. */
    private static JsonNode wachter(String opvolgerrang) throws Exception {
        return JSON.readTree(("""
                { "format": "uchar-case-v0", "asOfDate": null,
                  "input": { "instances": [ { "label": "W1", "objectType": "Wachter",
                      "attributes": { "rang": { "number": "4" } }, "roles": {}, "characteristics": [] } ],
                      "parameters": {} },
                  "expected": [ { "kind": "attributeValue", "label": "W1", "attribute": "opvolgerrang",
                                  "value": { "number": "%s" } },
                                { "kind": "inconsistent", "rule": "rangdetector" } ] }
                """).formatted(opvolgerrang));
    }

    private static String oorzaken(Throwable t) {
        StringBuilder sb = new StringBuilder();
        for (Throwable c = t; c != null; c = c.getCause()) sb.append(c).append(" <- ");
        return sb.toString();
    }
}
