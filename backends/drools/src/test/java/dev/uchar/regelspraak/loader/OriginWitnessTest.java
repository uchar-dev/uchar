package dev.uchar.regelspraak.loader;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import dev.uchar.regelspraak.cases.IrPath;
import dev.uchar.regelspraak.model.RegelSpraakModel;
import dev.uchar.regelspraak.model.RegelSpraakModel.Benoembaar;
import dev.uchar.regelspraak.model.RegelSpraakModel.GegenereerdUit;
import dev.uchar.regelspraak.model.RegelSpraakModel.Herkomstvorm;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The origin on the wire (IR-SPEC ch. 2.13), read back: the loader carries every {@code origin} into the model's table
 * {@code gegenereerd}, so a model read from the wire tells a declaration the frontend made from a written one, and
 * names what it comes from. A decision table, the kind {@code decisionTable} on the wire, is a form in the table, as
 * the frontend records it. The form and the kind are refused outside their closed sets, in the shared loader's
 * sentence (WF-121 (a)); what the source names is the verifier's and never a load refusal. That the loaded table equals
 * the frontend's on every rgs golden is the route suite's (gate 15, {@code OriginSourceRouteTest}). Measured before
 * this loader read the field: every golden that carries it was refused as an unknown field 'origin'.
 */
class OriginWitnessTest {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Path GOLDEN = Path.of("..", "..", "corpus", "golden");

    private static JsonNode golden(String name) throws Exception {
        return JSON.readTree(Files.readString(GOLDEN.resolve(name).resolve("ir.json")));
    }

    private static String element(Benoembaar b) {
        return b.soort() + " " + (b.eigenaar() == null ? "" : b.eigenaar() + ".") + b.naam();
    }

    /** The table as lines "<element> <- <source element> (<key>)", in the order of the map. */
    private static List<String> table(RegelSpraakModel m) {
        List<String> out = new ArrayList<>();
        for (Map.Entry<Benoembaar, GegenereerdUit> e : m.gegenereerd.entrySet())
            out.add(element(e.getKey()) + " <- " + element(e.getValue().bron()) + " (" + e.getValue().vorm().sleutel()
                    + ")");
        return out;
    }

    @Test void the_origin_of_a_decision_table_row_also_comes_into_the_model() throws Exception {
        RegelSpraakModel m = IrLoader.load(golden("jutterswet-opgave-vormen"));
        assertEquals(20, m.gegenereerd.size());
        assertEquals(Optional.of(new GegenereerdUit(Benoembaar.attribuut("Vondst", "waarde"),
                        Herkomstvorm.OPGAVE_BOVEN_BEREKENING)),
                m.gegenereerdUit(Benoembaar.attribuut("Vondst", "berekende waarde")));
        assertEquals(Optional.of(new GegenereerdUit(Benoembaar.attribuut("Aanspoelsel", "stukprijs"),
                        Herkomstvorm.OPGAVE_BOVEN_BEREKENING)),
                m.gegenereerdUit(Benoembaar.kenmerk("Aanspoelsel", "afwijkende opgave stukprijs")));
        // the table is a form in the model, as the frontend records it
        assertEquals(Optional.of(new GegenereerdUit(Benoembaar.vorm("stukprijs van een aanspoelsel"),
                        Herkomstvorm.BESLISTABELRIJ)),
                m.gegenereerdUit(Benoembaar.vorm("stukprijs van een aanspoelsel 2")));
        // a group under the name of the base rule the author wrote, and a written member of an unordered block
        assertEquals(Optional.empty(), m.gegenereerdUit(Benoembaar.vorm("waardering")));
        assertEquals(Optional.empty(), m.gegenereerdUit(Benoembaar.vorm("wachtgeld voor kustwachters")));
    }

    @Test void the_group_of_a_subtype_and_the_member_of_a_fold() throws Exception {
        assertEquals(List.of("VORM meldtermijn voor Strandjutter <- VORM meldtermijn (uitzondering-voor-subtype)"),
                table(IrLoader.load(golden("jutterswet-meldtermijn-strandjutter"))));
        List<String> fold = table(IrLoader.load(golden("jutterswet-zwaarste-weging")));
        assertTrue(fold.contains(
                "ATTRIBUUT Jutter.gewicht via vondst en weging <- ATTRIBUUT Weging.gewicht (stappen-na-de-verzameling)"),
                fold::toString);
        assertTrue(fold.contains(
                "VORM gewicht via vondst en weging <- ATTRIBUUT Weging.gewicht (stappen-na-de-verzameling)"),
                fold::toString);
        assertEquals(6, fold.size());
    }

    @Test void a_start_point_reads_back() throws Exception {
        // no golden writes a start-point determination: the anchor's origin is put on a member of a golden
        JsonNode ir = golden("jutterswet-opgave-bedrag");
        ObjectNode o = ((ObjectNode) IrPath.attribute(IrPath.objectType(ir, "Vondst"), "waarde")).putObject("origin");
        o.put("form", "startPoint");
        o.putObject("from").put("kind", "namedForm").put("name", "afwijking opgave waarde");
        RegelSpraakModel m = IrLoader.load(ir);
        assertEquals(Optional.of(new GegenereerdUit(Benoembaar.vorm("afwijking opgave waarde"), Herkomstvorm.STARTPUNT)),
                m.gegenereerdUit(Benoembaar.attribuut("Vondst", "waarde")));
    }

    @Test void a_form_or_kind_outside_its_set_is_refused_by_the_loader_in_the_sentence_of_the_shared_loader()
            throws Exception {
        JsonNode ir = golden("jutterswet-opgave-bedrag");
        ((ObjectNode) IrPath.attribute(IrPath.objectType(ir, "Vondst"), "berekende waarde").get("origin"))
                .put("form", "opgaveBovenBerekening");
        IrLoader.Documentfout form = assertThrows(IrLoader.Documentfout.class, () -> IrLoader.load(ir));
        assertEquals("/model/objectTypes/0/attributes/4/origin/form", form.site());
        assertTrue(form.getMessage().endsWith("the origin form 'opgaveBovenBerekening' is none of the forms of IR-SPEC "
                + "ch. 2.13 ('inputAboveComputation', 'exceptionForSubtype', 'decisionTableRow', 'stepsAfterSet', "
                + "'startPoint')"), form.getMessage());

        JsonNode ir2 = golden("jutterswet-opgave-bedrag");
        ((ObjectNode) IrPath.attribute(IrPath.objectType(ir2, "Vondst"), "berekende waarde").get("origin").get("from"))
                .put("kind", "form");
        IrLoader.Documentfout kind = assertThrows(IrLoader.Documentfout.class, () -> IrLoader.load(ir2));
        assertEquals("/model/objectTypes/0/attributes/4/origin/from/kind", kind.site());
        assertTrue(kind.getMessage().endsWith("the origin's source kind 'form' is none of the kinds of IR-SPEC "
                + "ch. 2.13 ('objectType', 'attribute', 'characteristic', 'factType', 'role', 'enumerationValue', "
                + "'parameter', 'namedForm', 'ruleGroup', 'decisionTable')"), kind.getMessage());
    }

    @Test void the_loader_does_not_refuse_what_an_origin_names() throws Exception {
        // a source no declaration answers, and a kind another form names, are the verifier's (WF-121 (b), (d))
        JsonNode ir = golden("jutterswet-opgave-bedrag");
        ObjectNode van = (ObjectNode) IrPath.attribute(IrPath.objectType(ir, "Vondst"), "berekende waarde")
                .get("origin").get("from");
        van.put("kind", "decisionTable").put("name", "waard");
        van.remove("owner");
        RegelSpraakModel m = IrLoader.load(ir);
        assertEquals(Optional.of(new GegenereerdUit(Benoembaar.vorm("waard"), Herkomstvorm.OPGAVE_BOVEN_BEREKENING)),
                m.gegenereerdUit(Benoembaar.attribuut("Vondst", "berekende waarde")));
    }

    @Test void the_origin_is_read_structurally() throws Exception {
        JsonNode ir = golden("jutterswet-opgave-bedrag");
        ((ObjectNode) IrPath.attribute(IrPath.objectType(ir, "Vondst"), "berekende waarde")).putNull("origin");
        IrLoader.Documentfout empty = assertThrows(IrLoader.Documentfout.class, () -> IrLoader.load(ir));
        assertTrue(empty.getMessage().contains("written whole or not at all"), empty.getMessage());

        JsonNode ir2 = golden("jutterswet-opgave-bedrag");
        ((ObjectNode) IrPath.attribute(IrPath.objectType(ir2, "Vondst"), "berekende waarde").get("origin").get("from"))
                .put("line", 3);
        IrLoader.Documentfout foreign = assertThrows(IrLoader.Documentfout.class, () -> IrLoader.load(ir2));
        assertEquals("/model/objectTypes/0/attributes/4/origin/from/line", foreign.site());

        JsonNode ir3 = golden("jutterswet-opgave-bedrag");
        ((ObjectNode) IrPath.attribute(IrPath.objectType(ir3, "Vondst"), "berekende waarde").get("origin").get("from"))
                .remove("name");
        IrLoader.Documentfout withoutSource = assertThrows(IrLoader.Documentfout.class, () -> IrLoader.load(ir3));
        assertEquals("/model/objectTypes/0/attributes/4/origin/from", withoutSource.site());
    }
}
