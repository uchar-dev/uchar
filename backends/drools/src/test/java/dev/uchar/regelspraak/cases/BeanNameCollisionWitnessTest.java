package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import dev.uchar.regelspraak.loader.IrLoader;
import dev.uchar.regelspraak.model.RegelSpraakModel;
import dev.uchar.regelspraak.schema.FactModelGenerator;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Two declarations that the bean model maps onto one Java name. {@code Namen.ident} is not injective: it drops
 * the spaces and hyphens of a name and camel-cases the words, so {@code 'Recht op premie'} and
 * {@code 'RechtOpPremie'} are one fact class, and {@code 'half-stok'} and {@code 'half stok'} one bean property.
 * The verifier, {@code ref} and the ASP backend keep those names apart (WF-001 and WF-002 compare them
 * case-insensitively, not as Java identifiers, and WF-002 lets an attribute and a characteristic share even the
 * same name), so every model below is verifier-clean where the square applies.
 *
 * <p>Measured on the backend as it stood before these refusals: two object types on one class
 * crashed the bean compile ({@code cyclic inheritance}) or broke the bean contract (a getter of the first type
 * missing on the class of the second), and with identical members built and answered for both types at once;
 * two members on one property either crashed at fire, failed the DRL compile for the whole document, or wrote
 * one member through the other, a wrong value with no refusal. Both branches alike.
 *
 * <p>What holds now, and is pinned per row: an object type that shares its fact class with another type, with a
 * class the bean model generates itself or with a {@code java.lang} type is refused at build, naming both and the
 * class; two roles that share one reference field of one type are refused at build; a member whose bean property
 * belongs to another declaration is refused where the document uses it — at build when a form of the rule layer
 * reads or writes it, and when a case sets or reads it — naming both declarations and the property. A member
 * whose property another declaration took and that nothing uses stays out of the way: a document with such a
 * member plays, as it did before.
 *
 * <p>Every model is an in-memory variant of a corpus golden; the corpus is untouched.
 */
class BeanNameCollisionWitnessTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    // cwd under surefire is the module dir (backends/drools); the corpus is two levels up.
    private static final Path CORPUS = Path.of("..", "..", "corpus", "golden");

    private static final String PREMIERECHT = "jutterswet-premierecht";
    private static final String REKENJAAR = "jutterswet-rekenjaar";
    private static final String TOELAGEGRONDEN = "anchor/jutterswet-toelagegronden";

    private static final String WHOLE = "{\"node\":\"Number\",\"whole\":true,\"decimals\":0,\"sign\":\"unspecified\"}";

    // ---------------- 1. two declarations on one fact class ----------------

    @Test void two_object_types_with_is_a_on_a_class_are_refused_by_name() throws Exception {
        // the shape first reported: a type written as one word, declared first, and its subtype as three
        // words; the subtype's class then extends itself ("cyclic inheritance")
        ObjectNode ir = ir(PREMIERECHT);
        ObjectNode parent = objecttype(ir, "RechtOpPremie", null);
        parent.set("attributes", lijst("{\"node\":\"Attribute\",\"name\":\"grondslag\",\"datatype\":" + WHOLE + "}"));
        ArrayNode typen = IrPath.modelList(ir, "objectTypes");
        typen.remove(typen.size() - 1);
        typen.insert(0, parent);
        IrPath.objectType(ir, "Recht op premie").put("supertype", "RechtOpPremie");
        refused(write(ir), goldenCase(PREMIERECHT),
                "beannaambotsing", "'Recht op premie'", "'RechtOpPremie'", "dev.uchar.regelspraak.facts.RechtOpPremie");
    }

    @Test void two_separate_object_types_on_a_class_are_refused_by_name() throws Exception {
        ObjectNode ir = ir(PREMIERECHT);
        objecttype(ir, "RechtOpPremie", null).set("attributes",
                lijst("{\"node\":\"Attribute\",\"name\":\"grondslag\",\"datatype\":" + WHOLE + "}"));
        refused(write(ir), goldenCase(PREMIERECHT),
                "beannaambotsing", "'Recht op premie'", "'RechtOpPremie'", "dev.uchar.regelspraak.facts.RechtOpPremie");
    }

    @Test void two_equal_object_types_on_a_class_are_refused_by_name() throws Exception {
        // identical members, the relation to a Jutter included, build one class that serves both types, so every
        // rule on the one matched the instances of the other: a wrong value with no refusal (R9 is a
        // RechtOpPremie, and no rule attributes or computes anything for it)
        ObjectNode ir = ir(PREMIERECHT);
        ObjectNode copy = IrPath.objectType(ir, "Recht op premie").deepCopy();
        copy.put("name", "RechtOpPremie").put("plural", "RechtenOpPremie");
        IrPath.modelList(ir, "objectTypes").add(copy);
        IrPath.modelList(ir, "factTypes").add(JSON.readTree("""
                { "node": "FactType", "name": "jutter heeft kopierecht", "reciprocal": false, "roles": [
                    { "node": "Role", "name": "rechthebbende", "objectType": "Jutter", "many": false },
                    { "node": "Role", "name": "kopierecht", "objectType": "RechtOpPremie",
                      "plural": "kopierechten", "many": true } ] }
                """));
        refused(write(ir), JSON.readTree("""
                { "format": "uchar-case-v0", "asOfDate": null,
                  "input": { "instances": [
                      { "label": "R9", "objectType": "RechtOpPremie", "attributes": { "vondstgewicht": { "number": "5" } },
                        "roles": {}, "characteristics": [] } ],
                    "parameters": { "premietarief": { "number": "4" } } },
                  "expected": [ { "kind": "characteristicAbsent", "label": "R9", "characteristic": "toegekend" },
                                { "kind": "attributeEmpty", "label": "R9", "attribute": "premie" } ] }
                """), "beannaambotsing", "'Recht op premie'", "'RechtOpPremie'", "dev.uchar.regelspraak.facts.RechtOpPremie");
    }

    @Test void an_object_type_with_the_name_of_a_helper_class_is_refused_by_name() throws Exception {
        ObjectNode ir = ir(PREMIERECHT);
        objecttype(ir, "Segment", null).set("attributes",
                lijst("{\"node\":\"Attribute\",\"name\":\"lengte\",\"datatype\":" + WHOLE + "}"));
        refused(write(ir), goldenCase(PREMIERECHT),
                "beannaambotsing", "'Segment'", "dev.uchar.regelspraak.facts.Segment", "hulpklasse");
    }

    @Test void an_object_type_with_the_name_of_a_java_lang_type_is_refused_by_name() throws Exception {
        ObjectNode ir = ir(PREMIERECHT);
        objecttype(ir, "String", null);
        refused(write(ir), goldenCase(PREMIERECHT),
                "beannaambotsing", "'String'", "dev.uchar.regelspraak.facts.String", "java.lang.String");
    }

    // ---------------- 2. a used member without a field of its own ----------------

    @Test void attribute_and_characteristic_with_the_same_name_are_refused_by_name() throws Exception {
        // the reported shape: WF-002 lets an attribute and a characteristic share a name, and the golden's
        // rules read the characteristic 'toegekend'. No rule may WRITE a name that is both (WF-022, the loader
        // refuses it as the verifier does), so the rule that attributes it goes and a case supplies it.
        ObjectNode ir = ir(PREMIERECHT);
        IrPath.addAttribute(IrPath.objectType(ir, "Recht op premie"), "toegekend", JSON.readTree(WHOLE));
        ArrayNode rules = IrPath.rulesList(ir, "rules");
        for (int i = 0; i < rules.size(); i++)
            if (rules.get(i).path("name").asText().equals("toekenning van een recht op premie")) rules.remove(i--);
        // before: the case's characteristic reached the attribute's setter, "kon setter setToegekend(boolean)"
        refused(write(ir), caseWithCharacteristic("\"characteristics\": [\"toegekend\"]",
                        "{ \"kind\": \"attributeValue\", \"label\": \"R1\", \"attribute\": \"premie\", \"value\": { \"number\": \"20\" } }"),
                "beannaambotsing",
                "het kenmerk 'toegekend' van Recht op premie", "het attribuut 'toegekend' van Recht op premie",
                "RechtOpPremie.toegekend", "regel 'premie van een recht op premie'");
    }

    @Test void attribute_and_characteristic_with_the_same_name_in_a_defaults_group_are_refused_by_name() throws Exception {
        // the one corpus golden with the shape: both branches refused it before, each on a sentence of its own
        // (the exec branch's defaults fence, the DRL-text branch's defaults refusal); the bean model's sentence
        // now comes first in both
        refused(CORPUS.resolve(TOELAGEGRONDEN).resolve("ir.json"), goldenCase(TOELAGEGRONDEN), "beannaambotsing",
                "het kenmerk 'toelage' van Jutter", "het attribuut 'toelage' van Jutter", "Jutter.toelage",
                "defaultsgroep");
    }

    @Test void two_attributes_on_a_property_are_refused_by_name() throws Exception {
        // 'aantal-toegekende rechten' goes first, so it takes the property and the golden's own attribute, which
        // its count rule writes, has no field left: before, the rule overwrote the value the case gave the other
        ObjectNode ir = ir(PREMIERECHT);
        ArrayNode attrs = (ArrayNode) IrPath.objectType(ir, "Jutter").get("attributes");
        attrs.insert(0, JSON.readTree("{\"node\":\"Attribute\",\"name\":\"aantal-toegekende rechten\",\"datatype\":"
                + WHOLE + "}"));
        refused(write(ir), JSON.readTree("""
                { "format": "uchar-case-v0", "asOfDate": null,
                  "input": { "instances": [
                      { "label": "J1", "objectType": "Jutter", "attributes": { "aantal-toegekende rechten": { "number": "9" } },
                        "roles": {}, "characteristics": [] } ],
                    "parameters": { "premietarief": { "number": "4" } } },
                  "expected": [ { "kind": "attributeValue", "label": "J1", "attribute": "aantal-toegekende rechten",
                                  "value": { "number": "9" } } ] }
                """), "beannaambotsing", "het attribuut 'aantal toegekende rechten' van Jutter",
                "het attribuut 'aantal-toegekende rechten' van Jutter", "Jutter.aantalToegekendeRechten",
                "regel 'aantal toegekende rechten'");
    }

    @Test void two_characteristics_on_a_property_are_refused_by_name() throws Exception {
        // 'laat-gemeld' goes first; the golden's 'laat gemeld', which its rule writes, has no field left, and a
        // case that asked after the other read the first one's truth
        ObjectNode ir = ir(REKENJAAR);
        ArrayNode kenmerken = (ArrayNode) IrPath.objectType(ir, "Vondst").get("characteristics");
        kenmerken.insert(0, JSON.readTree("{\"node\":\"Characteristic\",\"name\":\"laat-gemeld\",\"kind\":\"adjectival\"}"));
        refused(write(ir), JSON.readTree("""
                { "format": "uchar-case-v0", "asOfDate": "2024-12-31",
                  "input": { "instances": [
                      { "label": "V1", "objectType": "Vondst", "attributes": { "vondstjaar": { "number": "2020" } },
                        "roles": {}, "characteristics": [] } ],
                    "parameters": {} },
                  "expected": [ { "kind": "characteristicAbsent", "label": "V1", "characteristic": "laat-gemeld" } ] }
                """), "beannaambotsing", "het kenmerk 'laat gemeld' van Vondst", "het kenmerk 'laat-gemeld' van Vondst",
                "Vondst.laatGemeld", "regel 'Kenmerktoekenning laat gemeld'");
    }

    @Test void an_own_attribute_on_the_property_of_an_inherited_attribute_is_refused_by_name() throws Exception {
        // the subtype's own 'aantal-toegekende rechten' meets the inherited 'aantal toegekende rechten' on one
        // property of the Strandjutter bean; the ancestor's declaration keeps it, and the subtype's
        // initialisation wrote into the inherited value
        ObjectNode ir = ir(PREMIERECHT);
        objecttype(ir, "Strandjutter", "Jutter").set("attributes", lijst("{\"node\":\"Attribute\","
                + "\"name\":\"aantal-toegekende rechten\",\"datatype\":" + WHOLE + "}"));
        IrPath.rulesList(ir, "rules").add(JSON.readTree(IrPath.regel("beginstand van een strandjutter", "INITIALISATIE",
                "{\"node\":\"AttributeReference\",\"attribute\":\"aantal-toegekende rechten\",\"subjectType\":\"Strandjutter\","
                        + "\"source\":{\"snippet\":\"Het aantal-toegekende rechten van een Strandjutter\"}}",
                IrPath.literal("7", WHOLE, "7"), "null")));
        refused(write(ir), JSON.readTree("""
                { "format": "uchar-case-v0", "asOfDate": null,
                  "input": { "instances": [
                      { "label": "S1", "objectType": "Strandjutter", "attributes": {}, "roles": {}, "characteristics": [] } ],
                    "parameters": { "premietarief": { "number": "4" } } },
                  "expected": [ { "kind": "attributeValue", "label": "S1", "attribute": "aantal toegekende rechten",
                                  "value": { "number": "0" } },
                                { "kind": "attributeValue", "label": "S1", "attribute": "aantal-toegekende rechten",
                                  "value": { "number": "7" } } ] }
                """), "beannaambotsing",
                "het attribuut 'aantal-toegekende rechten' van Strandjutter",
                "het attribuut 'aantal toegekende rechten' van Jutter", "Strandjutter.aantalToegekendeRechten",
                "regel 'beginstand van een strandjutter'");
    }

    @Test void an_unused_member_without_its_own_field_lets_the_document_play() throws Exception {
        // a characteristic 'vondstgewicht' beside the attribute of that name, read and written by nothing: the
        // property carries the attribute and nobody asks after the characteristic, so the golden's two cases play
        // with its own values in both branches
        ObjectNode ir = ir(PREMIERECHT);
        ((ArrayNode) IrPath.objectType(ir, "Recht op premie").get("characteristics")).add(JSON.readTree(
                "{\"node\":\"Characteristic\",\"name\":\"vondstgewicht\",\"kind\":\"adjectival\"}"));
        Path doc = write(ir);
        for (String c : new String[] { "case1-tarief-vier.json", "case2-tarief-vijf-op-de-grens.json" })
            BothBranches.green(doc, JSON.readTree(Files.readString(CORPUS.resolve(PREMIERECHT).resolve(c))), c);
    }

    // ---------------- 3. a case that names a member without a field of its own ----------------

    @Test void a_case_in_which_the_characteristic_is_set_without_its_own_field_is_refused_by_name() throws Exception {
        // before: "kon setter setVondstgewicht(boolean) niet aanroepen", an unclassified ERROR
        refused(unusedCharacteristic(false), caseWithCharacteristic("\"characteristics\": [\"vondstgewicht\"]",
                        "{ \"kind\": \"attributeValue\", \"label\": \"R1\", \"attribute\": \"premie\", \"value\": { \"number\": \"20\" } }"),
                "beannaambotsing", "het kenmerk 'vondstgewicht' van Recht op premie",
                "het attribuut 'vondstgewicht' van Recht op premie", "RechtOpPremie.vondstgewicht", "de case zet het");
    }

    @Test void a_case_that_reads_the_characteristic_without_its_own_field_is_refused_by_name() throws Exception {
        // before: the expectation read the attribute's number as the characteristic's truth and passed, because a
        // number is not Boolean.TRUE — right by accident, and wrong the moment the attribute were a Boolean
        refused(unusedCharacteristic(false), caseWithCharacteristic("\"characteristics\": []",
                        "{ \"kind\": \"characteristicAbsent\", \"label\": \"R1\", \"characteristic\": \"vondstgewicht\" }"),
                "beannaambotsing", "het kenmerk 'vondstgewicht' van Recht op premie", "RechtOpPremie.vondstgewicht",
                "de case leest het");
    }

    @Test void a_case_in_which_the_timeline_of_the_characteristic_is_set_without_its_own_field_is_refused_by_name() throws Exception {
        // the path of the finding behind this class: a timeline for the characteristic reached the attribute's
        // setter, "kon setter setVondstgewicht(List) niet aanroepen"
        refused(unusedCharacteristic(true), caseWithCharacteristic(
                        "\"characteristics\": [], \"characteristicTimelines\": { \"vondstgewicht\": "
                                + "[ { \"from\": \"2024-01-01\", \"to\": \"2025-01-01\" } ] }",
                        "{ \"kind\": \"attributeValue\", \"label\": \"R1\", \"attribute\": \"premie\", \"value\": { \"number\": \"20\" } }"),
                "beannaambotsing", "het kenmerk 'vondstgewicht' van Recht op premie", "RechtOpPremie.vondstgewicht",
                "de case zet het");
    }

    // ---------------- 4. two roles on one reference field ----------------

    @Test void two_roles_on_one_reference_field_of_a_type_are_refused_by_name() throws Exception {
        // two relations, each giving a Recht op premie one Jutter, under names that differ by a hyphen: one field
        // vasteHouder of type Jutter serves both, so the per-use gate (name and type) lets either through
        ObjectNode ir = ir(PREMIERECHT);
        ArrayNode factTypes = IrPath.modelList(ir, "factTypes");
        factTypes.add(JSON.readTree("""
                { "node": "FactType", "name": "reservering", "reciprocal": false, "roles": [
                    { "node": "Role", "name": "vaste houder", "objectType": "Jutter", "many": false },
                    { "node": "Role", "name": "gereserveerd recht", "objectType": "Recht op premie",
                      "plural": "gereserveerde rechten", "many": true } ] }
                """));
        factTypes.add(JSON.readTree("""
                { "node": "FactType", "name": "bewaring", "reciprocal": false, "roles": [
                    { "node": "Role", "name": "vaste-houder", "objectType": "Jutter", "many": false },
                    { "node": "Role", "name": "bewaard recht", "objectType": "Recht op premie",
                      "plural": "bewaarde rechten", "many": true } ] }
                """));
        refused(write(ir), goldenCase(PREMIERECHT), "beannaambotsing", "de rol 'vaste houder' (feittype 'reservering')",
                "de rol 'vaste-houder' (feittype 'bewaring')", "RechtOpPremie.vasteHouder");
    }

    // ---------------- 5. the generator: one field per property on a chain ----------------

    @Test void a_grandchild_does_not_declare_an_inherited_reference_field_again() throws Exception {
        // leden() subtracted the DIRECT supertype's own members only, so a grandchild re-derived the role field its
        // grandparent already declares and hid it with a field of its own; the subtraction is over the chain now
        ObjectNode ir = ir(PREMIERECHT);
        objecttype(ir, "Wrakrecht op premie", "Recht op premie");
        objecttype(ir, "Diep wrakrecht op premie", "Wrakrecht op premie");
        RegelSpraakModel m = IrLoader.loadAll(ir).model();
        var grandchild = m.objecttypeOpNaam("Diep wrakrecht op premie");
        assertTrue(FactModelGenerator.members(m, m.objecttypeOpNaam("Recht op premie")).stream()
                .anyMatch(l -> l.property().equals("rechthebbende")), "the grandparent declares the field");
        assertFalse(FactModelGenerator.members(m, grandchild).stream().anyMatch(l -> l.property().equals("rechthebbende")),
                "the grandchild inherits it and declares no second one: " + FactModelGenerator.members(m, grandchild));
        assertTrue(FactModelGenerator.hasReferenceField(m, grandchild, "rechthebbende", m.objecttypeOpNaam("Jutter")),
                "and still carries it");
    }

    // ---------------- helpers ----------------

    /** Both branches refuse the document with a {@link CaseRunner.Excluded} whose sentence carries every fragment.
     *  A replay that refuses nothing fails with what it answered instead, so a row that goes red shows the verdict
     *  the refusal replaces. */
    private static void refused(Path ir, JsonNode c, String... fragments) throws Exception {
        for (boolean viaDrl : new boolean[] { false, true }) {
            String branch = viaDrl ? "drl-tekst" : "exec-model";
            String answer;
            try {
                answer = CaseRunner.playCase(ir, c, viaDrl);
            } catch (CaseRunner.Excluded e) {
                for (String f : fragments)
                    assertTrue(e.getMessage().contains(f), branch + " names '" + f + "': " + e.getMessage());
                continue;
            }
            fail(branch + " refuses nothing and answers " + (answer == null ? "every expectation matched" : answer));
        }
    }

    /** premierecht with a characteristic 'vondstgewicht' beside the attribute of that name, which no rule uses;
     *  {@code temporeel} gives the characteristic a year timeline. */
    private static Path unusedCharacteristic(boolean temporal) throws Exception {
        ObjectNode ir = ir(PREMIERECHT);
        ObjectNode k = (ObjectNode) JSON.readTree("{\"node\":\"Characteristic\",\"name\":\"vondstgewicht\",\"kind\":\"adjectival\"}");
        if (temporal) IrPath.timeline(k, "year");
        ((ArrayNode) IrPath.objectType(ir, "Recht op premie").get("characteristics")).add(k);
        return write(ir);
    }

    /** One Jutter holding one Recht op premie of vondstgewicht 5, the characteristic input {@code kenmerken} on the
     *  recht, and one expectation. */
    private static JsonNode caseWithCharacteristic(String kenmerken, String expectation) throws Exception {
        return JSON.readTree("""
                { "format": "uchar-case-v0", "asOfDate": null,
                  "input": { "instances": [
                      { "label": "J1", "objectType": "Jutter", "attributes": {},
                        "roles": { "rechten op premie": ["R1"] }, "characteristics": [] },
                      { "label": "R1", "objectType": "Recht op premie", "attributes": { "vondstgewicht": { "number": "5" } },
                        "roles": {}, %s } ],
                    "parameters": { "premietarief": { "number": "4" } } },
                  "expected": [ %s ] }
                """.formatted(kenmerken, expectation));
    }

    /** A new object type appended to the document, with no members; {@code supertype} may be null. */
    private static ObjectNode objecttype(ObjectNode ir, String naam, String supertype) {
        ObjectNode ot = IrPath.modelList(ir, "objectTypes").addObject();
        ot.put("node", "ObjectType").put("name", naam).put("animate", false);
        if (supertype != null) ot.put("supertype", supertype);
        ot.putArray("attributes");
        ot.putArray("characteristics");
        return ot;
    }

    private static ArrayNode lijst(String... elements) throws Exception {
        ArrayNode a = JSON.createArrayNode();
        for (String e : elements) a.add(JSON.readTree(e));
        return a;
    }

    private static ObjectNode ir(String golden) throws Exception {
        return (ObjectNode) JSON.readTree(Files.readString(CORPUS.resolve(golden).resolve("ir.json")));
    }

    private static JsonNode goldenCase(String golden) throws Exception {
        try (var s = Files.list(CORPUS.resolve(golden))) {
            Path c = s.filter(p -> p.getFileName().toString().startsWith("case")).sorted().findFirst().orElseThrow();
            return JSON.readTree(Files.readString(c));
        }
    }

    private static Path write(ObjectNode ir) throws Exception {
        Path tmp = Files.createTempFile("beannaambotsing", ".json");
        tmp.toFile().deleteOnExit();
        Files.writeString(tmp, ir.toString());
        return tmp;
    }
}
