package dev.uchar.regelspraak.loader;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import dev.uchar.regelspraak.cases.IrPad;
import dev.uchar.regelspraak.model.RegelSpraakModel;
import dev.uchar.regelspraak.model.RegelSpraakModel.Benoembaar;

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
class VocabulaireWitnessTest {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Path CORPUS = Path.of("..", "..", "corpus");

    private static JsonNode golden() throws Exception {
        Path ir = CORPUS.resolve("golden/jutterswet-vocabulaires/ir.json");
        assertTrue(Files.exists(ir), "ir.json aanwezig: " + ir.toAbsolutePath());
        return JSON.readTree(Files.readString(ir));
    }

    @Test void de_vocabulaires_en_hun_benamingen_komen_in_het_model() throws Exception {
        RegelSpraakModel m = IrLoader.load(golden());
        assertEquals(List.of(new RegelSpraakModel.Vocabulaire("handboek", false, "nl"),
                new RegelSpraakModel.Vocabulaire("cgm", true, "nl")), m.vocabulaires);
        Benoembaar bedrag = Benoembaar.attribuut("Jutter", "vondstbedrag");
        assertEquals(Optional.of("totale premie"), m.benaming("handboek", bedrag));
        assertEquals(Optional.of("VONDST_BEDRAG"), m.benaming("CGM", bedrag));   // a vocabulary by name, ci
        assertEquals(Optional.of("amber"), m.benaming("handboek", Benoembaar.enumeratiewaarde("Vondstsoort", "barnsteen")));
        assertEquals(Optional.of("R_PREMIE_VONDST"), m.benaming("cgm", Benoembaar.vorm("premie van een vondst")));
        assertEquals(Optional.of("PREMIEBEREKENING"), m.benaming("cgm", Benoembaar.regelgroep("premieberekening")));
        assertEquals(Optional.of("GEMELDE_VONDST"), m.benaming("cgm", Benoembaar.rol("vondstaanmelding", "vondst")));
        assertEquals(Optional.empty(), m.benaming("wet", bedrag));
        // every designatable declaration of the golden, each named in the default vocabulary (WF-119)
        List<Benoembaar> alle = m.benoembaar();
        assertEquals(23, alle.size());
        for (Benoembaar b : alle) assertTrue(m.benaming("cgm", b).isPresent(), () -> "geen cgm-benaming op " + b);
    }

    @Test void de_weg_terug_loopt_over_de_effectieve_naam() throws Exception {
        RegelSpraakModel m = IrLoader.load(golden());
        assertEquals(Optional.of(Benoembaar.attribuut("Jutter", "vondstbedrag")),
                m.benoemd("cgm", Benoembaar.Soort.ATTRIBUUT, "Jutter", "vondst_bedrag"));
        assertEquals(Optional.of(Benoembaar.enumeratiewaarde("Vondstsoort", "barnsteen")),
                m.benoemd("handboek", Benoembaar.Soort.ENUMERATIEWAARDE, "Vondstsoort", "amber"));
        assertEquals(Optional.of(Benoembaar.vorm("premie van een vondst")),
                m.benoemd("cgm", Benoembaar.Soort.VORM, null, "R_PREMIE_VONDST"));
        // a designation never stands in for a canonical name: in cgm the attribute is VONDST_BEDRAG, not vondstbedrag
        assertEquals(Optional.empty(), m.benoemd("cgm", Benoembaar.Soort.ATTRIBUUT, "Jutter", "vondstbedrag"));
        // the attribute of another type is not in the scope
        assertEquals(Optional.empty(), m.benoemd("cgm", Benoembaar.Soort.ATTRIBUUT, "Vondst", "VONDST_BEDRAG"));
    }

    @Test void zonder_benaming_is_de_effectieve_naam_de_canonieke() throws Exception {
        JsonNode ir = golden();
        ObjectNode gewicht = IrPad.attribute(IrPad.objectType(ir, "Vondst"), "gewicht");
        ArrayNode lijst = (ArrayNode) gewicht.get("designations");
        lijst.remove(0);                                    // the handboek designation goes, the cgm one stays
        RegelSpraakModel m = IrLoader.load(ir);
        Benoembaar b = Benoembaar.attribuut("Vondst", "gewicht");
        assertEquals("gewicht", m.effectieveNaam("handboek", b));
        assertEquals(Optional.of(b), m.benoemd("handboek", Benoembaar.Soort.ATTRIBUUT, "Vondst", "GEWICHT"));
        assertEquals("GEWICHT", m.effectieveNaam("cgm", b));
    }

    @Test void een_geerfd_lid_wordt_vanaf_het_subtype_gevonden() throws Exception {
        JsonNode ir = golden();
        ObjectNode sub = JSON.createObjectNode();
        sub.put("node", "ObjectType").put("name", "Zware vondst").put("supertype", "Vondst").put("animate", false);
        sub.putArray("attributes");
        sub.putArray("characteristics");
        IrPad.modelLijst(ir, "objectTypes").add(sub);
        RegelSpraakModel m = IrLoader.load(ir);
        assertEquals(Optional.of(Benoembaar.attribuut("Vondst", "gewicht")),
                m.benoemd("cgm", Benoembaar.Soort.ATTRIBUUT, "Zware vondst", "GEWICHT"));
    }

    @Test void de_zijtafel_wordt_structureel_gelezen() throws Exception {
        JsonNode ir = golden();
        ((ObjectNode) IrPad.objectType(ir, "Jutter")).putArray("designations");
        IrLoader.Documentfout leeg = assertThrows(IrLoader.Documentfout.class, () -> IrLoader.load(ir));
        assertTrue(leeg.getMessage().contains("a declaration's designations are at least one, or the field is absent"),
                leeg.getMessage());

        JsonNode ir2 = golden();
        ((ObjectNode) ((ArrayNode) IrPad.objectType(ir2, "Jutter").get("designations")).get(0)).put("status", "x");
        IrLoader.Documentfout vreemd = assertThrows(IrLoader.Documentfout.class, () -> IrLoader.load(ir2));
        assertEquals("/model/objectTypes/0/designations/0/status", vreemd.plek());

        JsonNode ir3 = golden();
        ((ObjectNode) ((ArrayNode) ir3.get("vocabularies")).get(1)).put("kind", "code");
        IrLoader.Documentfout soort = assertThrows(IrLoader.Documentfout.class, () -> IrLoader.load(ir3));
        assertEquals("/vocabularies/1/kind", soort.plek());
    }

    @Test void wat_een_benaming_zegt_weigert_de_lader_niet() throws Exception {
        // an undeclared vocabulary and a value that names no declared value are the verifier's (WF-117), never a load
        // refusal: no executor refuses a document for what its designations say
        JsonNode ir = golden();
        ObjectNode e = (ObjectNode) ((ArrayNode) IrPad.domain(ir, "Vondstsoort").get("datatype")
                .get("valueDesignations")).get(0);
        e.put("vocabulary", "wet").put("value", "gruis");
        RegelSpraakModel m = IrLoader.load(ir);
        assertFalse(m.benamingen.isEmpty());
    }
}
