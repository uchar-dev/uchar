package dev.uchar.regelspraak.loader;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import dev.uchar.regelspraak.model.Datatype;
import dev.uchar.regelspraak.model.Unit;
import dev.uchar.regelspraak.model.RegelSpraakModel;
import dev.uchar.regelspraak.model.RegelSpraakModel.UnitSystem;
import dev.uchar.regelspraak.model.RegelSpraakModel.Granulariteit;

import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The loader on the type system of the wire (L3 of the v1 wire migration): what it refuses by name, and how it lays
 * the wire back onto the frontend records. Refused: the type-system nodes of the L2 shape and each of their fields, a
 * factor that is not an exact fraction {@code {num, den}} of two integers from 1 to 2^62 - 1, a number without its
 * sign, a percentage with a sign, a presentation tag other than "percentage", on a number that is no percentage or at
 * a type annotation, a date without its precision, a spelling where a unit occurs or an empty list of them, and a
 * parameter without its datatype, with a timeline name or with a timeline count other than 1 — the new sentences in
 * the words of the shared OCaml loader and the verifier. Read: a number with the unit '%' as the record's percentage
 * and any other as its number, a precision as the two date records, a fraction as the record's decimal factor with
 * the fraction itself kept in the unit system (a fraction not in lowest terms too, as the shared loader reads it;
 * WF-092 is the verifier's), a unit's spellings as the system's aliases, a parameter's timeline facet as its
 * granularity.
 *
 * <p>Every fixture is a corpus golden with ONE twist in memory; the corpus file is read and never written. Each
 * refusal is an {@link IrLoader.DocumentError} at a JSON pointer, never an {@link IrLoader.Refusal}: every twisted
 * document here is ill-formed, a parameter timeline of more than one period included, which the verifier refuses
 * (WF-005) while multi-period grids are open.
 */
class TypeSystemRefusalWitnessTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    /** cwd under surefire is the module dir (backends/drools); the corpus is two levels up. */
    private static final Path CORPUS = Path.of("..", "..", "corpus", "golden");

    /** Parameter 0 a number, parameter 1 a number in EUR with a year timeline; Munt declares EUR with three
     *  spellings. */
    private static final String PENNINGJAAR = "jutterswet-penningjaar";
    /** Two percentage attributes, tagged at their declaration; rule 0 writes a percentage literal, untagged. */
    private static final String VONDSTTARIEF = "jutterswet-vondsttarief";
    /** A date-time attribute. */
    private static final String MELDSTIP = "jutterswet-meldstip";

    private static final String PARAMETER = "/model/parameters/0";
    private static final String GETAL = PARAMETER + "/datatype";
    private static final String UNIT = "/model/parameters/1/datatype/unit";
    private static final String FACTOR = UNIT + "/factorToCanonical";
    private static final String SYSTEM = "/model/unitSystems/0";
    private static final String RATE = "/model/objectTypes/0/attributes/0/datatype";
    private static final String ANNOTATION = "/rules/rules/0/versions/0/value/type";

    // ---------------- the L2 shape is unknown by name ----------------

    @Test void the_datatype_nodes_of_the_l2_form_are_unknown() throws Exception {
        Map<String, String> nodes = Map.of(
                "Numeriek", "{\"node\":\"Numeriek\",\"geheel\":true,\"decimalen\":0,\"teken\":\"ONBEPAALD\","
                        + "\"eenheid\":null}",
                "Percentage", "{\"node\":\"Percentage\",\"geheel\":true,\"decimalen\":0,\"eenheid\":null}",
                "DatumInDagen", "{\"node\":\"DatumInDagen\"}",
                "DatumTijd", "{\"node\":\"DatumTijd\"}");
        // the shared loader's sentence, at the node discriminator as it names it
        for (Map.Entry<String, String> k : nodes.entrySet()) {
            ObjectNode ir = golden(PENNINGJAAR);
            set(ir, GETAL, k.getValue());
            assertTrue(refuses(ir, GETAL + "/node").getMessage()
                    .endsWith(": unknown datatype node \"" + k.getKey() + "\""));
            ir = golden(VONDSTTARIEF);
            set(ir, ANNOTATION, k.getValue());
            assertTrue(refuses(ir, ANNOTATION + "/node").getMessage()
                    .endsWith(": unknown datatype node \"" + k.getKey() + "\""));
        }
    }

    @Test void the_unit_and_the_system_of_the_l2_form_are_unknown() throws Exception {
        ObjectNode ir = golden(PENNINGJAAR);
        ((ObjectNode) ir.at(UNIT)).put("node", "Eenheid");
        assertTrue(refuses(ir, UNIT + "/node").getMessage().contains("expected \"Unit\", got \"Eenheid\""));

        ir = golden(PENNINGJAAR);
        ((ObjectNode) ir.at(SYSTEM)).put("node", "Eenheidstelsel");
        assertTrue(refuses(ir, SYSTEM + "/node").getMessage().contains("expected \"UnitSystem\", got \"Eenheidstelsel\""));
    }

    @Test void the_fields_of_the_l2_form_are_unknown_fields() throws Exception {
        for (String field : List.of("geheel", "decimalen", "teken", "eenheid")) {
            ObjectNode ir = golden(PENNINGJAAR);
            ((ObjectNode) ir.at(GETAL)).put(field, "x");
            unknownField(ir, GETAL + "/" + field, "Number");
        }
        for (String field : List.of("afkorting", "stelsel", "canoniek", "factorNaarCanoniek", "noemer")) {
            ObjectNode ir = golden(PENNINGJAAR);
            ((ObjectNode) ir.at(UNIT)).put(field, "x");
            unknownField(ir, UNIT + "/" + field, "Unit");
        }
        for (String field : List.of("naam", "eenheden", "aliassen")) {
            ObjectNode ir = golden(PENNINGJAAR);
            ((ObjectNode) ir.at(SYSTEM)).put(field, "x");
            unknownField(ir, SYSTEM + "/" + field, "UnitSystem");
        }
        for (String field : List.of("naam", "granulariteit")) {
            ObjectNode ir = golden(PENNINGJAAR);
            ((ObjectNode) ir.at(PARAMETER)).put(field, "x");
            unknownField(ir, PARAMETER + "/" + field, "Parameter");
        }
        ObjectNode ir = golden(PENNINGJAAR);
        ((ObjectNode) ir.get("model")).putObject("dagsoortPerEenheid");
        assertTrue(refuses(ir, "/model/dagsoortPerEenheid").getMessage().contains("unknown field"));
    }

    // ---------------- the exact factor ----------------

    @Test void the_factor_is_an_exact_rational() throws Exception {
        // one sentence for every other form, the value quoted as written — the shared loader's
        List<String> forms = List.of("12", "\"12\"", "null", "{\"num\":1}", "{\"num\":1,\"den\":0}",
                "{\"num\":0,\"den\":1}", "{\"num\":-3,\"den\":1}", "{\"num\":1.5,\"den\":1}",
                "{\"num\":\"1\",\"den\":1}", "{\"num\":1,\"den\":1,\"node\":\"Factor\"}",
                "{\"num\":4611686018427387904,\"den\":1}");
        for (String form : forms) {
            ObjectNode ir = golden(PENNINGJAAR);
            ((ObjectNode) ir.at(UNIT)).set("factorToCanonical", JSON.readTree(form));
            IrLoader.DocumentError e = refuses(ir, FACTOR);
            assertTrue(e.getMessage().endsWith(": " + IrLoader.FACTOR_FORM + form), form + ": " + e.getMessage());
        }
        // the bound the sentence names is admitted itself: 2^62 - 1 loads, 2^62 (above) does not
        ObjectNode bound = golden(PENNINGJAAR);
        ((ObjectNode) bound.at(UNIT)).set("factorToCanonical",
                JSON.readTree("{\"num\":4611686018427387903,\"den\":1}"));
        Unit large = ((Datatype.Numeriek) IrLoader.load(bound).parameters.get("penningbedrag").datatype()).eenheid();
        assertEquals("4611686018427387903", large.factorNaarCanoniek().toPlainString());

        // not in lowest terms is WF-092 (c), the verifier's: the loader reads the value
        ObjectNode ir = golden(PENNINGJAAR);
        ((ObjectNode) ir.at(UNIT)).set("factorToCanonical", JSON.readTree("{\"num\":2,\"den\":4}"));
        Unit half = ((Datatype.Numeriek) IrLoader.load(ir).parameters.get("penningbedrag").datatype()).eenheid();
        assertEquals("0.5", half.factorNaarCanoniek().toPlainString());
    }

    @Test void a_rational_becomes_the_decimal_factor_of_the_record_and_stays_in_the_system() throws Exception {
        // the record's decimal is the fraction rounded to 16 significant digits, a whole number exactly; the unit
        // system keeps the fraction itself
        ObjectNode ir = golden(PENNINGJAAR);
        ArrayNode units = (ArrayNode) ir.at(SYSTEM + "/units");
        units.add(JSON.readTree("{\"node\":\"Unit\",\"abbreviation\":\"ct\",\"system\":\"Munt\",\"canonical\":\"EUR\","
                + "\"factorToCanonical\":{\"num\":1,\"den\":100}}"));
        units.add(JSON.readTree("{\"node\":\"Unit\",\"abbreviation\":\"kwartje\",\"system\":\"Munt\","
                + "\"canonical\":\"EUR\",\"factorToCanonical\":{\"num\":1,\"den\":24}}"));
        units.add(JSON.readTree("{\"node\":\"Unit\",\"abbreviation\":\"daalder\",\"system\":\"Munt\","
                + "\"canonical\":\"EUR\",\"factorToCanonical\":{\"num\":12,\"den\":1}}"));
        UnitSystem unitSystem = IrLoader.load(ir).unitSystems.get("Munt");
        assertEquals("0.01", unitSystem.units.get("ct").factorNaarCanoniek().toPlainString());
        assertEquals("0.04166666666666667", unitSystem.units.get("kwartje").factorNaarCanoniek().toPlainString());
        assertEquals("12", unitSystem.units.get("daalder").factorNaarCanoniek().toPlainString());
        assertEquals("1", unitSystem.units.get("EUR").factorNaarCanoniek().toPlainString());
        assertEquals(new UnitSystem.Factor(BigInteger.ONE, BigInteger.valueOf(24)), unitSystem.exactFactors.get("kwartje"));
        assertEquals(new UnitSystem.Factor(BigInteger.valueOf(12), BigInteger.ONE), unitSystem.exactFactors.get("daalder"));
        assertEquals(UnitSystem.Factor.EEN, unitSystem.exactFactors.get("EUR"));
    }

    // ---------------- the sign and the presentation tag ----------------

    @Test void the_sign_is_required_and_the_tag_belongs_to_a_declaration() throws Exception {
        ObjectNode ir = golden(PENNINGJAAR);
        ((ObjectNode) ir.at(GETAL)).putNull("sign");
        assertTrue(refuses(ir, GETAL + "/sign").getMessage().contains("expected a string, got null"));

        ir = golden(PENNINGJAAR);
        ((ObjectNode) ir.at(GETAL)).remove("sign");
        assertTrue(refuses(ir, GETAL).getMessage().contains("missing field 'sign'"));

        ir = golden(PENNINGJAAR);
        ((ObjectNode) ir.at(GETAL)).put("sign", "ONBEKEND");
        assertTrue(refuses(ir, GETAL + "/sign").getMessage().endsWith(": unknown sign value \"ONBEKEND\""));

        ir = golden(VONDSTTARIEF);
        ((ObjectNode) ir.at(RATE)).put("presentation", "procent");
        assertTrue(refuses(ir, RATE + "/presentation").getMessage()
                .endsWith(": unknown presentation value \"procent\""));

        ir = golden(VONDSTTARIEF);
        ((ObjectNode) ir.at(RATE)).putNull("presentation");
        assertTrue(refuses(ir, RATE + "/presentation").getMessage().contains("null is neither"));

        // WF-007: the tag stands at a declaration, and a type annotation is none
        ir = golden(VONDSTTARIEF);
        ((ObjectNode) ir.at(ANNOTATION)).put("presentation", "percentage");
        assertTrue(refuses(ir, ANNOTATION + "/presentation").getMessage().contains(IrLoader.TAG_ON_ANNOTATION));

        // the tag presents a percentage: on a number without the unit '%' it presents nothing
        ir = golden(PENNINGJAAR);
        ((ObjectNode) ir.at(GETAL)).put("presentation", "percentage");
        assertTrue(refuses(ir, GETAL + "/presentation").getMessage().contains(IrLoader.TAG_WITHOUT_PERCENT));

        // a percentage is written unsigned: the record has no sign to hold a restriction
        ir = golden(VONDSTTARIEF);
        ((ObjectNode) ir.at(RATE)).put("sign", "positive");
        assertTrue(refuses(ir, RATE + "/sign").getMessage().contains(IrLoader.PERCENT_WITH_SIGN));
    }

    // ---------------- the date ----------------

    @Test void a_date_carries_its_precision() throws Exception {
        ObjectNode ir = golden(MELDSTIP);
        String date = first(ir, "Date", "");
        ((ObjectNode) ir.at(date)).remove("precision");
        assertTrue(refuses(ir, date).getMessage().contains("missing field 'precision'"));

        ir = golden(MELDSTIP);
        ((ObjectNode) ir.at(date)).put("precision", "second");
        assertTrue(refuses(ir, date + "/precision").getMessage().endsWith(": unknown precision value \"second\""));
    }

    // ---------------- the spellings ----------------

    @Test void spellings_are_only_on_the_declaration() throws Exception {
        ObjectNode ir = golden(PENNINGJAAR);
        ((ObjectNode) ir.at(UNIT)).putArray("spellings").add("euro");
        unknownField(ir, UNIT + "/spellings", "Unit");

        String euro = SYSTEM + "/units/0";
        ir = golden(PENNINGJAAR);
        ((ObjectNode) ir.at(euro)).putArray("spellings");
        assertTrue(refuses(ir, euro + "/spellings").getMessage().contains(IrLoader.SPELLINGS_EMPTY));

        ir = golden(PENNINGJAAR);
        ((ObjectNode) ir.at(euro)).putNull("spellings");
        assertTrue(refuses(ir, euro + "/spellings").getMessage().contains("null is neither"));

        // one Unit per abbreviation, under the name identity (WF-009): a second EUR, and an eur, are the same unit
        for (String second : List.of("EUR", "eur")) {
            ir = golden(PENNINGJAAR);
            ((ArrayNode) ir.at(SYSTEM + "/units")).add(((ObjectNode) ir.at(euro).deepCopy()).put("abbreviation", second));
            assertTrue(refuses(ir, SYSTEM + "/units/1/abbreviation").getMessage().endsWith(": the unit '" + second
                    + "' is declared twice in the unit system 'Munt': one Unit per abbreviation"), second);
        }
    }

    // ---------------- the parameter ----------------

    @Test void a_parameter_carries_its_datatype_and_a_timeline_facet_without_a_name() throws Exception {
        ObjectNode ir = golden(PENNINGJAAR);
        ((ObjectNode) ir.at(PARAMETER)).putNull("datatype");
        assertTrue(refuses(ir, GETAL).getMessage().contains("expected a datatype object, got null"));

        ir = golden(PENNINGJAAR);
        ((ObjectNode) ir.at(PARAMETER)).remove("datatype");
        assertTrue(refuses(ir, PARAMETER).getMessage().contains("missing field 'datatype'"));

        String facet = "/model/parameters/1/timeline";
        ir = golden(PENNINGJAAR);
        ((ObjectNode) ir.at(facet)).put("timelineName", "boekjaar");
        unknownField(ir, facet + "/timelineName", "TimelineFacet");

        ir = golden(PENNINGJAAR);
        ((ObjectNode) ir.at("/model/parameters/1")).putNull("timeline");
        assertTrue(refuses(ir, facet).getMessage().contains("null is neither"));

        // WF-005: a parameter's timeline is one period, in the verifier's two sentences
        ir = golden(PENNINGJAAR);
        ((ObjectNode) ir.at(facet)).put("count", 2);
        assertTrue(refuses(ir, facet + "/count").getMessage().endsWith(": the timeline facet of parameter 'penningbedrag'"
                + " has count 2 > 1: a parameter's timeline is one period while multi-period grids are open"
                + " (RUNTIME-SEMANTICS ch. 3.12)"));

        ir = golden(PENNINGJAAR);
        ((ObjectNode) ir.at(facet)).put("count", 0);
        assertTrue(refuses(ir, facet + "/count").getMessage()
                .endsWith(": the timeline facet of parameter 'penningbedrag' has count 0 < 1"));
    }

    // ---------------- read back one to one ----------------

    @Test void the_loader_lays_the_wire_one_to_one_on_the_records() throws Exception {
        RegelSpraakModel m = IrLoader.load(golden(PENNINGJAAR));
        // a number declared without a sign restriction reads as the record's ONBEPAALD, the wire's unspecified
        Datatype.Numeriek threshold = assertInstanceOf(Datatype.Numeriek.class, m.parameters.get("vondstdrempel").datatype());
        assertEquals(Datatype.Teken.ONBEPAALD, threshold.teken());
        assertNull(threshold.eenheid());
        assertNull(m.parameters.get("vondstdrempel").granulariteit());
        assertEquals(Granulariteit.JAAR, m.parameters.get("penningbedrag").granulariteit());
        UnitSystem unitSystem = m.unitSystems.get("Munt");
        Unit eur = unitSystem.units.get("EUR");
        assertEquals(List.of("EUR"), List.copyOf(unitSystem.units.keySet()));
        assertEquals(List.of("€", "euro", "euros"), List.copyOf(unitSystem.spellings.keySet()));
        for (Unit e : unitSystem.spellings.values()) assertEquals(eur, e);
        assertEquals(eur, ((Datatype.Numeriek) m.parameters.get("penningbedrag").datatype()).eenheid());

        // a number with the unit '%' is the record's percentage, tagged or not, and its unit is the built-in one
        m = IrLoader.load(golden(VONDSTTARIEF));
        Datatype.Percentage rate = assertInstanceOf(Datatype.Percentage.class,
                m.objecttypen.get("Vondst").attributen.get("vondsttarief"));
        assertEquals(Datatype.Percentage.PERCENT, rate.eenheid());
        assertInstanceOf(Datatype.Percentage.class, m.resolvedRegels.get(0).value().type());

        // the two precisions are the two date records
        ObjectNode ir = golden(MELDSTIP);
        String date = first(ir, "Date", "");
        assertEquals("millisecond", ir.at(date + "/precision").asText());
        assertTrue(contains(IrLoader.load(ir), Datatype.DateTime.class));
        ((ObjectNode) ir.at(date)).put("precision", "day");
        assertTrue(contains(IrLoader.load(ir), Datatype.DatumInDagen.class));
    }

    // ---------------- helpers ----------------

    private static boolean contains(RegelSpraakModel m, Class<? extends Datatype> kind) {
        return m.objecttypen.values().stream().flatMap(ot -> ot.attributen.values().stream()).anyMatch(kind::isInstance);
    }

    /** The pointer of the first node of kind {@code kind} under {@code n}, in document order. */
    private static String first(JsonNode n, String kind, String path) {
        if (n.isObject()) {
            if (kind.equals(n.path("node").asText())) return path;
            for (var it = n.fields(); it.hasNext(); ) {
                var e = it.next();
                String p = first(e.getValue(), kind, path + "/" + e.getKey());
                if (p != null) return p;
            }
        } else if (n.isArray()) {
            for (int i = 0; i < n.size(); i++) {
                String p = first(n.get(i), kind, path + "/" + i);
                if (p != null) return p;
            }
        }
        return null;
    }

    private static void set(ObjectNode ir, String path, String json) throws Exception {
        int slash = path.lastIndexOf('/');
        ((ObjectNode) ir.at(path.substring(0, slash))).set(path.substring(slash + 1), JSON.readTree(json));
    }

    private static IrLoader.DocumentError refuses(JsonNode ir, String site) {
        IrLoader.DocumentError e = assertThrows(IrLoader.DocumentError.class, () -> IrLoader.loadAll(ir),
                "verwacht een documentfout op " + site);
        assertEquals(site, e.site(), e.getMessage());
        return e;
    }

    private static void unknownField(JsonNode ir, String site, String node) {
        IrLoader.DocumentError e = refuses(ir, site);
        assertTrue(e.getMessage().contains("unknown field on " + node), e.getMessage());
    }

    private static ObjectNode golden(String name) throws Exception {
        Path ir = CORPUS.resolve(name).resolve("ir.json");
        assertTrue(Files.exists(ir), "golden ir.json present: " + ir.toAbsolutePath());
        ObjectNode doc = (ObjectNode) JSON.readTree(Files.readString(ir));
        IrLoader.loadAll(doc);   // the untwisted golden loads, so the refusal below is the twist's alone
        return doc;
    }
}
