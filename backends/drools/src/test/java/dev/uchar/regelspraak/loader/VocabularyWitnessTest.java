package dev.uchar.regelspraak.loader;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import dev.uchar.regelspraak.cases.IrPath;
import dev.uchar.regelspraak.model.RegelSpraakModel;
import dev.uchar.regelspraak.model.RegelSpraakModel.Designatable;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The vocabulary side table on the wire (IR-SPEC ch. 2.12): the loader carries it into the model, where the public
 * reading entry answers a declaration's designation in a vocabulary, its effective name, and the way back from an
 * effective name to the declaration; the transpilers never read it. Measured before this loader read the fields:
 * the golden was refused as an unknown field 'vocabularies' at the root.
 */
class VocabularyWitnessTest {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Path CORPUS = Path.of("..", "..", "corpus");

    private static JsonNode golden() throws Exception {
        Path ir = CORPUS.resolve("golden/jutterswet-vocabulaires/ir.json");
        assertTrue(Files.exists(ir), "ir.json aanwezig: " + ir.toAbsolutePath());
        return JSON.readTree(Files.readString(ir));
    }

    @Test void the_vocabularies_and_their_designations_come_in_the_model() throws Exception {
        RegelSpraakModel m = IrLoader.load(golden());
        assertEquals(List.of(new RegelSpraakModel.Vocabulary("handboek", false, "nl"),
                new RegelSpraakModel.Vocabulary("cgm", true, "nl")), m.vocabularies);
        Designatable amount = Designatable.attribute("Jutter", "vondstbedrag");
        assertEquals(Optional.of("totale premie"), m.benaming("handboek", amount));
        assertEquals(Optional.of("VONDST_BEDRAG"), m.benaming("CGM", amount));   // a vocabulary by name, ci
        assertEquals(Optional.of("amber"), m.benaming("handboek", Designatable.enumerationValue("Vondstsoort", "barnsteen")));
        assertEquals(Optional.of("R_PREMIE_VONDST"), m.benaming("cgm", Designatable.form("premie van een vondst")));
        assertEquals(Optional.of("PREMIEBEREKENING"), m.benaming("cgm", Designatable.ruleGroup("premieberekening")));
        assertEquals(Optional.of("GEMELDE_VONDST"), m.benaming("cgm", Designatable.role("vondstaanmelding", "vondst")));
        assertEquals(Optional.empty(), m.benaming("wet", amount));
        // every designatable declaration of the golden, each named in the default vocabulary (WF-119)
        List<Designatable> all = m.designatable();
        assertEquals(23, all.size());
        for (Designatable b : all) assertTrue(m.benaming("cgm", b).isPresent(), () -> "geen cgm-benaming op " + b);
    }

    @Test void the_effective_name_goes_back_to_its_declaration() throws Exception {
        RegelSpraakModel m = IrLoader.load(golden());
        assertEquals(Optional.of(Designatable.attribute("Jutter", "vondstbedrag")),
                m.namedElement("cgm", Designatable.Kind.ATTRIBUUT, "Jutter", "vondst_bedrag"));
        assertEquals(Optional.of(Designatable.enumerationValue("Vondstsoort", "barnsteen")),
                m.namedElement("handboek", Designatable.Kind.ENUMERATIEWAARDE, "Vondstsoort", "amber"));
        assertEquals(Optional.of(Designatable.form("premie van een vondst")),
                m.namedElement("cgm", Designatable.Kind.VORM, null, "R_PREMIE_VONDST"));
        // a designation never stands in for a canonical name: in cgm the attribute is VONDST_AMOUNT, not vondstbedrag
        assertEquals(Optional.empty(), m.namedElement("cgm", Designatable.Kind.ATTRIBUUT, "Jutter", "vondstbedrag"));
        // the attribute of another type is not in the scope
        assertEquals(Optional.empty(), m.namedElement("cgm", Designatable.Kind.ATTRIBUUT, "Vondst", "VONDST_BEDRAG"));
    }

    @Test void without_designation_the_effective_name_is_the_canonical() throws Exception {
        JsonNode ir = golden();
        ObjectNode weight = IrPath.attribute(IrPath.objectType(ir, "Vondst"), "gewicht");
        ArrayNode list = (ArrayNode) weight.get("designations");
        list.remove(0);                                    // the handboek designation goes, the cgm one stays
        RegelSpraakModel m = IrLoader.load(ir);
        Designatable b = Designatable.attribute("Vondst", "gewicht");
        assertEquals("gewicht", m.effectiveName("handboek", b));
        assertEquals(Optional.of(b), m.namedElement("handboek", Designatable.Kind.ATTRIBUUT, "Vondst", "GEWICHT"));
        assertEquals("GEWICHT", m.effectiveName("cgm", b));
    }

    @Test void an_inherited_member_is_found_from_the_subtype() throws Exception {
        JsonNode ir = golden();
        ObjectNode sub = JSON.createObjectNode();
        sub.put("node", "ObjectType").put("name", "Zware vondst").put("supertype", "Vondst").put("animate", false);
        sub.putArray("attributes");
        sub.putArray("characteristics");
        IrPath.modelList(ir, "objectTypes").add(sub);
        RegelSpraakModel m = IrLoader.load(ir);
        assertEquals(Optional.of(Designatable.attribute("Vondst", "gewicht")),
                m.namedElement("cgm", Designatable.Kind.ATTRIBUUT, "Zware vondst", "GEWICHT"));
    }

    @Test void the_side_table_is_read_structurally() throws Exception {
        JsonNode ir = golden();
        ((ObjectNode) IrPath.objectType(ir, "Jutter")).putArray("designations");
        IrLoader.DocumentError empty = assertThrows(IrLoader.DocumentError.class, () -> IrLoader.load(ir));
        assertTrue(empty.getMessage().contains("a declaration's designations are at least one, or the field is absent"),
                empty.getMessage());

        JsonNode ir2 = golden();
        ((ObjectNode) ((ArrayNode) IrPath.objectType(ir2, "Jutter").get("designations")).get(0)).put("status", "x");
        IrLoader.DocumentError foreign = assertThrows(IrLoader.DocumentError.class, () -> IrLoader.load(ir2));
        assertEquals("/model/objectTypes/0/designations/0/status", foreign.site());

        JsonNode ir3 = golden();
        ((ObjectNode) ((ArrayNode) ir3.get("vocabularies")).get(1)).put("kind", "code");
        IrLoader.DocumentError kind = assertThrows(IrLoader.DocumentError.class, () -> IrLoader.load(ir3));
        assertEquals("/vocabularies/1/kind", kind.site());
    }

    @Test void the_loader_does_not_refuse_what_a_designation_says() throws Exception {
        // an undeclared vocabulary and a value that names no declared value are the verifier's (WF-117), never a load
        // refusal: no executor refuses a document for what its designations say
        JsonNode ir = golden();
        ObjectNode e = (ObjectNode) ((ArrayNode) IrPath.domain(ir, "Vondstsoort").get("datatype")
                .get("valueDesignations")).get(0);
        e.put("vocabulary", "wet").put("value", "gruis");
        RegelSpraakModel m = IrLoader.load(ir);
        assertFalse(m.designations.isEmpty());
    }
}
