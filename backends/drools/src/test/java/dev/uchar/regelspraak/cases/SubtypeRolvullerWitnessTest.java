package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import dev.uchar.regelspraak.loader.IrLoader;
import dev.uchar.regelspraak.model.RegelSpraakModel;
import dev.uchar.regelspraak.model.RegelSpraakModel.Objecttype;
import dev.uchar.regelspraak.schema.FactModelGenerator;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * IR-SPEC ch. 2.6: a role's declared filler is an UPPER bound, so the role takes an instance of every subtype of its
 * filler. {@code ref} and the ASP backend carry that; this backend asked the class of a reference field by
 * EQUALITY, so a {@code Wrakvondst} (a {@code Vondst}) did not fit the field of a role filled by {@code Vondst}.
 *
 * <p>Rood-eerst, measured 2026-09-28 on the backend before the change, over the golden
 * {@code jutterswet-wrakmelding} and the in-memory variants below, both branches alike: the fold, quantifier,
 * allocation and role-check joins on the subtype skipped with "het beanmodel leidt voor de rol 'vondst' op Melding
 * geen referentieveld naar Wrakvondst af"; with those rules gone the case input refused the link with "whose field a
 * same-named role toward another type already holds", a second role the document does not have; a subtype declared
 * before its supertype typed the field {@code Melding.vondst} as {@code Scheepswrak}, so even a plain {@code Vondst}
 * no longer fitted it; and the reciprocal role on {@code Vondst} was typed {@code Wrakvondst} for the same reason and
 * refused as "shares its name with a declared member". The case input's refusal named a second role wherever the
 * cause was not a member of the same name, also where an attribute held the property under another spelling. ref and
 * clingo play the documents of rows 1 to 8 with the values those rows pin; rows 9 to 11 pin this backend's refusal
 * sentences.
 *
 * <p>Every model is the golden or an in-memory variant of it, except the fence of row 5 and the sentence of row 9,
 * which twist or read the red case {@code ambiguous-case-role-key}; the corpus is untouched. Row 11 pins the
 * cardinality sentence of a to-many step on a document without subtypes, the same sentence family.
 */
class SubtypeRolvullerWitnessTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    // cwd under surefire is the module dir (backends/drools); the corpus is two levels up.
    private static final Path WRAKMELDING = Path.of("..", "..", "corpus", "golden", "jutterswet-wrakmelding");
    private static final Path ROLSLEUTEL = Path.of("..", "..", "corpus", "rood", "ambiguous-case-role-key");

    private static final List<String> CASES = List.of("case1-een-wrakvondst-in-de-rol-van-de-vondst.json",
            "case2-een-scheepswrak-twee-niveaus-onder-de-vuller.json", "case3-een-rapport-van-beide-kanten.json");

    private static final String GEHEEL = "{\"node\":\"Number\",\"whole\":true,\"decimals\":0,\"sign\":\"unspecified\"}";

    @TempDir Path tmp;

    // ---------------- 1-2. the golden: what the input becomes, and the replay in both branches ----------------

    /** The case reader's product, which no gate observes: gate 12 sees the values after fire only. The link lands on
     *  the field of the FILLER's class and holds the subtype's bean itself, and on the one-to-one relation the second
     *  write of RUNTIME-SEMANTICS ch. 5.7a lands on the other end as well, from either end and two levels down. */
    @Test void de_case_lezer_zet_een_subtype_in_het_veld_van_de_vuller() throws Exception {
        Map<String, Object> een = CaseRunner.invoerBeans(WRAKMELDING.resolve("ir.json"), geval(CASES.get(0)));
        assertEquals("Vondst", veldklasse(een.get("M1"), "getVondst"), "het veld heeft de klasse van de vuller");
        assertEquals("Wrakvondst", een.get("W").getClass().getSimpleName());
        for (String m : List.of("M1", "M2", "M3")) assertSame(een.get("W"), lees(een.get(m), "getVondst"), m);
        assertSame(een.get("V"), lees(een.get("M4"), "getVondst"), "M4");

        Map<String, Object> drie = CaseRunner.invoerBeans(WRAKMELDING.resolve("ir.json"), geval(CASES.get(2)));
        assertSame(drie.get("W"), lees(drie.get("R"), "getGerapporteerdeVondst"), "vanaf de rapportkant geschreven");
        assertSame(drie.get("R"), lees(drie.get("W"), "getRapport"), "en ook aan de wrakkant gezet");
        assertSame(drie.get("P"), lees(drie.get("S"), "getRapport"), "vanaf het scheepswrak geschreven");
        assertSame(drie.get("S"), lees(drie.get("P"), "getGerapporteerdeVondst"), "en in het Vondst-veld van P gezet");
    }

    /** The golden's three cases through both branches: the to-one read on the storing side, a count, a sum, a
     *  quantifier, a role check and an allocation from the subtype, a count on the supertype and the one-to-one
     *  relation read from both ends. Gate 12 replays the same files; this row is their red-first witness. */
    @Test void beide_takken_spelen_de_vormen_van_de_golden() throws Exception {
        for (String c : CASES) BeideTakken.groen(WRAKMELDING.resolve("ir.json"), geval(c), c);
    }

    // ---------------- 3-5. the bean model: the field's class, and whose field it is ----------------

    /** The field's class is the role's declared filler whatever the declaration order. It used to be the type the
     *  derivation met first: with the subtypes declared before their supertype, the golden's {@code Melding.vondst}
     *  was a {@code Scheepswrak} field and every rule over the link refused, a plain {@code Vondst} included. */
    @Test void het_referentieveld_heeft_de_klasse_van_de_vuller_in_elke_volgorde() throws Exception {
        ObjectNode ir = golden();
        ArrayNode typen = IrPad.modelLijst(ir, "objectTypes");
        assertEquals("[\"Vondst\",\"Wrakvondst\",\"Scheepswrak\",\"Melding\",\"Rapport\"]", namen(typen));
        JsonNode scheepswrak = typen.remove(2), wrakvondst = typen.remove(1);
        typen.insert(0, wrakvondst);
        typen.insert(0, scheepswrak);
        assertEquals("[\"Scheepswrak\",\"Wrakvondst\",\"Vondst\",\"Melding\",\"Rapport\"]", namen(typen));
        RegelSpraakModel model = IrLoader.load(ir);
        assertTrue(FactModelGenerator.leden(model, model.objecttypeOpNaam("Melding"))
                .contains(new FactModelGenerator.Lid("vondst", "Vondst", false)), "Melding.vondst is een Vondst-veld");
        assertTrue(FactModelGenerator.leden(model, model.objecttypeOpNaam("Rapport"))
                .contains(new FactModelGenerator.Lid("gerapporteerdeVondst", "Vondst", false)));
        Path pad = schrijf(ir);
        for (String c : CASES) BeideTakken.groen(pad, geval(c), c);
    }

    /** The type gate is assignability: the field of the role filled by {@code Vondst} takes a {@code Vondst}, a
     *  {@code Wrakvondst} and a {@code Scheepswrak}, and nothing that is no {@code Vondst}. */
    @Test void het_veld_neemt_elk_subtype_van_de_vuller_voor_dezelfde_rol() throws Exception {
        RegelSpraakModel model = IrLoader.load(golden());
        Objecttype melding = model.objecttypeOpNaam("Melding");
        for (String t : List.of("Vondst", "Wrakvondst", "Scheepswrak"))
            assertTrue(FactModelGenerator.heeftReferentieVeld(model, melding, "vondst", model.objecttypeOpNaam(t)), t);
        assertFalse(FactModelGenerator.heeftReferentieVeld(model, melding, "vondst", model.objecttypeOpNaam("Rapport")));
    }

    /** The fence on that assignability. Twisted from the red case: {@code Vlot} is made a {@code Ponton}, so both
     *  roles named {@code ligger} accept a {@code Vlot}, and the rule reads over {@code vlotligging}. The bean of
     *  {@code Schuit} holds ONE field {@code ligger}, of the class {@code Ponton}, and it is {@code pontonligging}'s;
     *  {@code Vlot} fits its class, and it is still refused, because reading it would answer a {@code vlotligging} read
     *  from {@code pontonligging}'s storage. The refusal itself held before and after the change, so it fences the
     *  change; its sentence is new: it said "het beanmodel leidt voor de rol 'ligger' op Schuit geen referentieveld
     *  naar Vlot af", true and without a cause, and now names the role that holds the field. ref and clingo play the
     *  document (the case writes no link, so the read is leeg). */
    @Test void een_veld_van_een_andere_rol_neemt_geen_subtype_aan() throws Exception {
        ObjectNode ir = (ObjectNode) JSON.readTree(Files.readString(ROLSLEUTEL.resolve("ir.json")));
        IrPad.objectType(ir, "Vlot").put("supertype", "Ponton");
        ObjectNode lezing = (ObjectNode) IrPad.versie(ir, "rules", "ligdiepte van een schuit").get("value");
        lezing.put("attribute", "dekhoogte").put("subjectType", "Vlot");
        ((ObjectNode) lezing.get("path").get(0)).put("factType", "vlotligging");
        RegelSpraakModel model = IrLoader.load(ir);
        Objecttype schuit = model.objecttypeOpNaam("Schuit");
        assertTrue(FactModelGenerator.heeftReferentieVeld(model, schuit, "ligger", model.objecttypeOpNaam("Ponton")));
        assertFalse(FactModelGenerator.heeftReferentieVeld(model, schuit, "ligger", model.objecttypeOpNaam("Vlot")));
        // the truth-maintenance branch names the missing field in its own sentence; the DRL-text branch names who
        // holds the field
        BeideTakken.geweigerd(schrijf(ir), JSON.readTree("""
                { "format": "uchar-case-v0", "asOfDate": null,
                  "input": { "instances": [ { "label": "S1", "objectType": "Schuit", "attributes": {}, "roles": {},
                                              "characteristics": [] } ], "parameters": {} },
                  "expected": [ { "kind": "attributeEmpty", "label": "S1", "attribute": "ligdiepte" } ] }
                """), "regel: ligdiepte van een schuit — de lezing 'het draagvlak van de ligger van de Schuit': Schuit"
                        + " heeft geen referentieveld 'ligger' naar Vlot (§13.3.9 opslagrichting)",
                "het veld van pontonligging leest geen vlotligging, en de zin noemt wie het veld houdt");
    }

    // ---------------- 6-8. the reciprocal role and the two creations, on the golden's model ----------------

    /** A reciprocal role filled by {@code Vondst}, its one link written on the {@code Wrakvondst}: the mirror pass
     *  asked the reciprocal role of the bean's own type only and skipped the subtype's leg, so {@code V} read an empty
     *  relation (measured with the field class fixed and the pass unchanged: "maatwaarde: verwacht 5, werkelijk
     *  leeg", both branches). ref and clingo: maatwaarde V = 5, W = 2. */
    @Test void de_wederkerige_rol_spiegelt_ook_voor_een_subtype() throws Exception {
        ObjectNode ir = golden();
        IrPad.addAttribute(IrPad.objectType(ir, "Vondst"), "maatwaarde", JSON.readTree(GEHEEL));
        IrPad.modelLijst(ir, "factTypes").add(JSON.readTree("""
                {"node":"FactType","name":"vondstpaar","reciprocal":true,
                 "roles":[{"node":"Role","name":"maat","objectType":"Vondst","many":false}]}"""));
        IrPad.rulesLijst(ir, "rules").add(JSON.readTree("""
                {"node":"Rule","name":"maatwaarde van een vondst","versions":[{"validity":{"node":"Validity"},
                 "target":{"node":"AttributeReference","attribute":"maatwaarde","subjectType":"Vondst",
                           "source":{"snippet":"De maatwaarde van een Vondst"}},
                 "value":{"node":"AttributeReference","attribute":"waarde","subjectType":"Vondst",
                          "source":{"snippet":"de waarde van de maat van de Vondst"},
                          "path":[{"node":"RoleBinding","factType":"vondstpaar","role":"maat"}]},
                 "initialisation":false,"presentation":"equation"}]}"""));
        JsonNode c = JSON.readTree("""
                { "format": "uchar-case-v0", "asOfDate": null,
                  "input": { "instances": [
                      { "label": "V", "objectType": "Vondst", "attributes": { "waarde": { "number": "2" } },
                        "roles": {}, "characteristics": [] },
                      { "label": "W", "objectType": "Wrakvondst", "attributes": { "waarde": { "number": "5" } },
                        "roles": { "maat": [ "V" ] }, "characteristics": [] } ], "parameters": {} },
                  "expected": [
                      { "kind": "attributeValue", "label": "V", "attribute": "maatwaarde", "value": { "number": "5" } },
                      { "kind": "attributeValue", "label": "W", "attribute": "maatwaarde", "value": { "number": "2" } } ] }
                """);
        Path pad = schrijf(ir);
        Map<String, Object> invoer = CaseRunner.invoerBeans(pad, c);
        assertSame(invoer.get("V"), lees(invoer.get("W"), "getMaat"));
        assertSame(invoer.get("W"), lees(invoer.get("V"), "getMaat"), "de poot van het subtype gespiegeld");
        BeideTakken.groen(pad, c, "wederkerig met een subtype");
    }

    /** An object creation on the subtype links the minted {@code Melding} to the {@code Wrakvondst} that created it,
     *  through the field of the filler's class. W (waarde 4, one melding of ernst 3) gets a melding of ernst 4, X
     *  (waarde 1, none) one of ernst 1, and V, a plain {@code Vondst}, none. ref and clingo: meldsom W = 3 + 4 = 7,
     *  aantal meldingen W = 2, meldsom X = 1, aantal meldingen X = 1, meldingental V = 0. */
    @Test void een_objectcreatie_op_het_subtype_koppelt_aan_het_subtype() throws Exception {
        ObjectNode ir = golden();
        IrPad.rulesLijst(ir, "objectCreations").add(JSON.readTree("""
                {"node":"ObjectCreation","name":"meld elke wrakvondst","versions":[{"createdType":"Melding",
                 "subject":"Wrakvondst","initialisations":[{"node":"MemberInit","member":"ernst",
                   "value":{"node":"AttributeReference","attribute":"waarde","subjectType":"Wrakvondst",
                            "source":{"snippet":"zijn waarde"}},"characteristic":false}],
                 "validity":{"node":"Validity"},
                 "connectingRole":{"node":"RoleBinding","factType":"vondst heeft melding","role":"melding"}}]}"""));
        BeideTakken.groen(schrijf(ir), JSON.readTree("""
                { "format": "uchar-case-v0", "asOfDate": null,
                  "input": { "instances": [
                      { "label": "V", "objectType": "Vondst", "attributes": { "waarde": { "number": "9" } },
                        "roles": {}, "characteristics": [] },
                      { "label": "W", "objectType": "Wrakvondst",
                        "attributes": { "waarde": { "number": "4" }, "meldgeld": { "number": "6" } },
                        "roles": {}, "characteristics": [] },
                      { "label": "X", "objectType": "Wrakvondst",
                        "attributes": { "waarde": { "number": "1" }, "meldgeld": { "number": "2" } },
                        "roles": {}, "characteristics": [] },
                      { "label": "M1", "objectType": "Melding", "attributes": { "ernst": { "number": "3" } },
                        "roles": { "vondst": [ "W" ] }, "characteristics": [] } ], "parameters": {} },
                  "expected": [
                      { "kind": "attributeValue", "label": "W", "attribute": "meldsom", "value": { "number": "7" } },
                      { "kind": "attributeValue", "label": "W", "attribute": "aantal meldingen", "value": { "number": "2" } },
                      { "kind": "attributeValue", "label": "X", "attribute": "meldsom", "value": { "number": "1" } },
                      { "kind": "attributeValue", "label": "X", "attribute": "aantal meldingen", "value": { "number": "1" } },
                      { "kind": "attributeValue", "label": "V", "attribute": "meldingental", "value": { "number": "0" } } ] }
                """), "objectcreatie op het subtype");
    }

    /** A fact creation anchored on the subtype: every melding of a {@code Wrakvondst} becomes one of its
     *  aandachtsmeldingen. Its join from the anchor and its write both land on reference fields of the class
     *  {@code Vondst}, holding the {@code Wrakvondst}. ref and clingo: aandachtstal W = 2, V = 0 (the rule is on the
     *  subtype, and V's melding M3 is no aandachtsmelding). */
    @Test void een_feitcreatie_op_het_subtype_koppelt_aan_het_subtype() throws Exception {
        ObjectNode ir = golden();
        IrPad.addAttribute(IrPad.objectType(ir, "Vondst"), "aandachtstal", JSON.readTree(GEHEEL));
        IrPad.modelLijst(ir, "factTypes").add(JSON.readTree("""
                {"node":"FactType","name":"aandacht","reciprocal":false,
                 "roles":[{"node":"Role","name":"aandachtsvondst","objectType":"Vondst","many":false},
                          {"node":"Role","name":"aandachtsmelding","objectType":"Melding",
                           "plural":"aandachtsmeldingen","many":true}]}"""));
        IrPad.rulesLijst(ir, "rules").add(JSON.readTree("""
                {"node":"Rule","name":"aandachtstal van een vondst","versions":[{"validity":{"node":"Validity"},
                 "target":{"node":"AttributeReference","attribute":"aandachtstal","subjectType":"Vondst",
                           "source":{"snippet":"Het aandachtstal van een Vondst"}},
                 "value":{"node":"Aggregation","kind":"count","arguments":[{"node":"AttributeReference",
                            "attribute":"aandachtsmeldingen","subjectType":"Vondst",
                            "source":{"snippet":"aandachtsmeldingen van de Vondst"}}],
                          "elementType":"Melding",
                          "sourceRole":{"node":"RoleBinding","factType":"aandacht","role":"aandachtsmelding"},
                          "type":{"node":"Number","whole":true,"decimals":0,"sign":"nonNegative"},
                          "source":{"snippet":"het aantal aandachtsmeldingen van de Vondst"}},
                 "initialisation":false,"presentation":"calculation"}]}"""));
        IrPad.rulesLijst(ir, "factCreations").add(JSON.readTree("""
                {"node":"FactCreation","name":"aandacht voor wrakken","versions":[{"factType":"aandacht",
                 "anchorRole":"aandachtsvondst","targetRole":"aandachtsmelding","anchor":"Wrakvondst",
                 "path":[{"node":"RoleBinding","factType":"vondst heeft melding","role":"melding"}],
                 "validity":{"node":"Validity"}}]}"""));
        BeideTakken.groen(schrijf(ir), JSON.readTree("""
                { "format": "uchar-case-v0", "asOfDate": null,
                  "input": { "instances": [
                      { "label": "W", "objectType": "Wrakvondst", "attributes": { "meldgeld": { "number": "2" } },
                        "roles": {}, "characteristics": [] },
                      { "label": "V", "objectType": "Vondst", "attributes": {}, "roles": {}, "characteristics": [] },
                      { "label": "M1", "objectType": "Melding", "attributes": {}, "roles": { "vondst": [ "W" ] },
                        "characteristics": [] },
                      { "label": "M2", "objectType": "Melding", "attributes": {}, "roles": { "vondst": [ "W" ] },
                        "characteristics": [] },
                      { "label": "M3", "objectType": "Melding", "attributes": {}, "roles": { "vondst": [ "V" ] },
                        "characteristics": [] } ], "parameters": {} },
                  "expected": [
                      { "kind": "attributeValue", "label": "W", "attribute": "aandachtstal", "value": { "number": "2" } },
                      { "kind": "attributeValue", "label": "V", "attribute": "aandachtstal", "value": { "number": "0" } } ] }
                """), "feitcreatie op het subtype");
    }

    // ---------------- 9. the refusal names what holds the field ----------------

    /** Where the case input still refuses, the sentence names the declaration that holds the property. The red case
     *  writes one key {@code ligger} toward a {@code Ponton} and a {@code Vlot}; the field {@code Schuit.ligger} is
     *  {@code pontonligging}'s. The sentence "a same-named role toward another type" was written for this shape, and
     *  it was also what every subtype target got, where no second role exists. */
    @Test void de_case_lezer_noemt_de_rol_die_het_veld_houdt() throws Exception {
        CaseRunner.RoodResult r = CaseRunner.runRood(ROLSLEUTEL.resolve("ir.json"), ROLSLEUTEL.resolve("case1.json"));
        assertEquals("EXCLUDE", r.status(), r.detail());
        assertTrue(r.detail().contains("whose field Schuit.ligger another role already holds, the role 'ligger' of fact"
                + " type 'pontonligging'"), r.detail());
        assertFalse(r.detail().contains("same-named role toward another type"), r.detail());
    }

    /** The same reader where a declared MEMBER holds the property under another spelling: the attribute
     *  {@code gerapporteerde-vondst} and the role {@code gerapporteerde vondst} fall on one bean property, which the
     *  attribute keeps (attribute &gt; characteristic &gt; reference). The names differ, so the member test by name
     *  misses it, and the sentence blamed a second role that does not exist. The twist drops the one rule that reads
     *  over the role, which the transpiler would refuse first; case 3 of the golden then writes the role. */
    @Test void de_case_lezer_noemt_het_lid_dat_de_eigenschap_houdt() throws Exception {
        ObjectNode ir = golden();
        IrPad.addAttribute(IrPad.objectType(ir, "Rapport"), "gerapporteerde-vondst", JSON.readTree(GEHEEL));
        ArrayNode regels = IrPad.rulesLijst(ir, "rules");
        regels.remove(indexVan(regels, IrPad.vorm(ir, "rules", "rapportwaarde van een rapport")));
        BeideTakken.geweigerd(schrijf(ir), geval(CASES.get(2)), "whose bean property Rapport.gerapporteerdeVondst"
                + " belongs to the attribute 'gerapporteerde-vondst' of Rapport", "een rol op de eigenschap van een lid");
    }

    /** The cardinality sentence of a to-many step, on a document without subtypes: a WF-102 path from {@code Vondst}
     *  over its meldingen, whose storage is the field {@code Melding.vondst} at the far end, while the attribute
     *  {@code vondst} of {@code Melding} holds that property. The sentence said "(beide zijden meervoudig)" there,
     *  and the role {@code vondst} is to-one; the same sentence came out of the path from the subtype before the
     *  change, whose far field did exist. Twisted from the golden: its rules make way for the one path. */
    @Test void een_pad_over_een_verdrongen_zusterveld_noemt_het_lid() throws Exception {
        ObjectNode ir = golden();
        IrPad.addAttribute(IrPad.objectType(ir, "Melding"), "vondst", JSON.readTree(GEHEEL));
        IrPad.addAttribute(IrPad.objectType(ir, "Vondst"), "bijlagengewicht", JSON.readTree(GEHEEL));
        IrPad.modelLijst(ir, "objectTypes").add(JSON.readTree("""
                {"node":"ObjectType","name":"Bijlage","animate":false,"plural":"Bijlagen",
                 "attributes":[{"node":"Attribute","name":"gewicht","datatype":%s}],"characteristics":[]}"""
                .formatted(GEHEEL)));
        IrPad.modelLijst(ir, "factTypes").add(JSON.readTree("""
                {"node":"FactType","name":"melding heeft bijlage","reciprocal":false,
                 "roles":[{"node":"Role","name":"gemelde melding","objectType":"Melding","many":false},
                          {"node":"Role","name":"bijlage","objectType":"Bijlage","plural":"bijlagen","many":true}]}"""));
        IrPad.rulesLijst(ir, "allocations").removeAll();
        ArrayNode regels = IrPad.rulesLijst(ir, "rules");
        regels.removeAll();
        regels.add(JSON.readTree("""
                {"node":"Rule","name":"bijlagengewicht van een vondst","versions":[{"validity":{"node":"Validity"},
                 "target":{"node":"AttributeReference","attribute":"bijlagengewicht","subjectType":"Vondst",
                           "source":{"snippet":"Het bijlagengewicht van een Vondst"}},
                 "value":{"node":"Aggregation","kind":"sum","arguments":[{"node":"AttributeReference",
                            "attribute":"gewicht","subjectType":"Bijlage",
                            "source":{"snippet":"het gewicht van alle bijlagen van alle meldingen van de Vondst"}}],
                          "elementType":"Bijlage",
                          "sourcePath":[{"node":"RoleBinding","factType":"vondst heeft melding","role":"melding"},
                                        {"node":"RoleBinding","factType":"melding heeft bijlage","role":"bijlage"}],
                          "type":%s,
                          "source":{"snippet":"de som van het gewicht van alle bijlagen van alle meldingen van de Vondst"}},
                 "initialisation":false,"presentation":"calculation"}]}""".formatted(GEHEEL)));
        Path pad = schrijf(ir);
        JsonNode c = JSON.readTree("""
                { "format": "uchar-case-v0", "asOfDate": null,
                  "input": { "instances": [ { "label": "V", "objectType": "Vondst", "attributes": {}, "roles": {},
                                              "characteristics": [] } ], "parameters": {} },
                  "expected": [ { "kind": "attributeEmpty", "label": "V", "attribute": "bijlagengewicht" } ] }
                """);
        BeideTakken.geweigerd(pad, c, "de rol 'melding' op Vondst is meervoudig, dus haar opslag is het veld aan de"
                + " overzijde, en dat veld ontbreekt: de rol 'vondst' op Melding deelt haar naam met een gedeclareerd"
                + " lid", "de zusterrol is enkelvoudig; haar veld is door een lid genomen");
        for (boolean viaDrl : new boolean[] { false, true }) {
            CaseRunner.Excluded e = org.junit.jupiter.api.Assertions.assertThrows(CaseRunner.Excluded.class,
                    () -> CaseRunner.speelCase(pad, c, viaDrl));
            assertFalse(e.getMessage().contains("beide zijden meervoudig"), e.getMessage());
        }
    }

    // ---------------- helpers ----------------

    private static ObjectNode golden() throws Exception {
        return (ObjectNode) JSON.readTree(Files.readString(WRAKMELDING.resolve("ir.json")));
    }

    private static JsonNode geval(String naam) throws Exception {
        return JSON.readTree(Files.readString(WRAKMELDING.resolve(naam)));
    }

    private Path schrijf(JsonNode ir) throws Exception {
        Path pad = Files.createTempFile(tmp, "ir", ".json");
        Files.writeString(pad, JSON.writeValueAsString(ir));
        return pad;
    }

    private static Object lees(Object bean, String getter) throws Exception {
        return bean.getClass().getMethod(getter).invoke(bean);
    }

    private static String veldklasse(Object bean, String getter) throws Exception {
        return bean.getClass().getMethod(getter).getReturnType().getSimpleName();
    }

    private static int indexVan(ArrayNode lijst, JsonNode lid) {
        for (int i = 0; i < lijst.size(); i++) if (lijst.get(i) == lid) return i;
        throw new AssertionError("niet in de lijst: " + lid.path("name").asText());
    }

    private static String namen(ArrayNode typen) {
        ArrayNode uit = JSON.createArrayNode();
        for (JsonNode t : typen) uit.add(t.path("name").asText());
        return uit.toString();
    }
}
