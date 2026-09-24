package io.github.yhavinga.regelspraak.loader;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.github.yhavinga.regelspraak.model.Datatype;
import io.github.yhavinga.regelspraak.model.Eenheid;
import io.github.yhavinga.regelspraak.model.RegelSpraakModel;
import io.github.yhavinga.regelspraak.model.RegelSpraakModel.Eenheidstelsel;
import io.github.yhavinga.regelspraak.model.RegelSpraakModel.Granulariteit;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
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
 * The loader on the type system of the wire (L3 of the v1 wire migration): what it refuses by name, and how it lays the
 * wire back onto the frontend records. Refused: the type-system nodes of the L2 shape and each of their fields, a
 * factor that is not an exact fraction {@code {num, den}} of two integers at least 1 in lowest terms, a number without
 * its sign or with a presentation tag other than "percentage" or at a type annotation, a date without its precision, a
 * spelling where a unit occurs or equal to its abbreviation, and a parameter without its datatype or with a timeline
 * name. Read: a number with the unit '%' as the record's percentage and any other as its number, a precision as the two
 * date records, a fraction as the record's decimal factor with the fraction itself kept in the unit system, a unit's
 * spellings as the system's aliases, a parameter's timeline facet as its granularity.
 *
 * <p>Every fixture is a corpus golden with ONE twist in memory; the corpus file is read and never written. Each
 * refusal of a malformed document is an {@link IrLoader.Documentfout} at a JSON pointer, never an
 * {@link IrLoader.Refusal}; the two forms this backend does not carry — a percentage with a sign, a parameter timeline
 * of more than one period — are Refusals, because those documents are well-formed.
 */
class TypesysteemWeigeringWitnessTest {

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
    private static final String EENHEID = "/model/parameters/1/datatype/unit";
    private static final String FACTOR = EENHEID + "/factorToCanonical";
    private static final String STELSEL = "/model/unitSystems/0";
    private static final String TARIEF = "/model/objectTypes/0/attributes/0/datatype";
    private static final String ANNOTATIE = "/rules/resolvedRegels/0/waarde/type";

    // ---------------- the L2 shape is unknown by name ----------------

    @Test void de_datatypeknopen_van_de_l2_vorm_zijn_onbekend() throws Exception {
        Map<String, String> knopen = Map.of(
                "Numeriek", "{\"node\":\"Numeriek\",\"geheel\":true,\"decimalen\":0,\"teken\":\"ONBEPAALD\","
                        + "\"eenheid\":null}",
                "Percentage", "{\"node\":\"Percentage\",\"geheel\":true,\"decimalen\":0,\"eenheid\":null}",
                "DatumInDagen", "{\"node\":\"DatumInDagen\"}",
                "DatumTijd", "{\"node\":\"DatumTijd\"}");
        for (Map.Entry<String, String> k : knopen.entrySet()) {
            ObjectNode ir = golden(PENNINGJAAR);
            zet(ir, GETAL, k.getValue());
            assertTrue(weigert(ir, GETAL).getMessage().contains("unknown datatype node \"" + k.getKey() + "\""));
            ir = golden(VONDSTTARIEF);
            zet(ir, ANNOTATIE, k.getValue());
            assertTrue(weigert(ir, ANNOTATIE).getMessage().contains("unknown datatype node \"" + k.getKey() + "\""));
        }
    }

    @Test void de_eenheid_en_het_stelsel_van_de_l2_vorm_zijn_onbekend() throws Exception {
        ObjectNode ir = golden(PENNINGJAAR);
        ((ObjectNode) ir.at(EENHEID)).put("node", "Eenheid");
        assertTrue(weigert(ir, EENHEID + "/node").getMessage().contains("expected \"Unit\", got \"Eenheid\""));

        ir = golden(PENNINGJAAR);
        ((ObjectNode) ir.at(STELSEL)).put("node", "Eenheidstelsel");
        assertTrue(weigert(ir, STELSEL + "/node").getMessage().contains("expected \"UnitSystem\", got \"Eenheidstelsel\""));
    }

    @Test void de_velden_van_de_l2_vorm_zijn_onbekende_velden() throws Exception {
        for (String veld : List.of("geheel", "decimalen", "teken", "eenheid")) {
            ObjectNode ir = golden(PENNINGJAAR);
            ((ObjectNode) ir.at(GETAL)).put(veld, "x");
            onbekendVeld(ir, GETAL + "/" + veld, "Number");
        }
        for (String veld : List.of("afkorting", "stelsel", "canoniek", "factorNaarCanoniek", "noemer")) {
            ObjectNode ir = golden(PENNINGJAAR);
            ((ObjectNode) ir.at(EENHEID)).put(veld, "x");
            onbekendVeld(ir, EENHEID + "/" + veld, "Unit");
        }
        for (String veld : List.of("naam", "eenheden", "aliassen")) {
            ObjectNode ir = golden(PENNINGJAAR);
            ((ObjectNode) ir.at(STELSEL)).put(veld, "x");
            onbekendVeld(ir, STELSEL + "/" + veld, "UnitSystem");
        }
        for (String veld : List.of("naam", "granulariteit")) {
            ObjectNode ir = golden(PENNINGJAAR);
            ((ObjectNode) ir.at(PARAMETER)).put(veld, "x");
            onbekendVeld(ir, PARAMETER + "/" + veld, "Parameter");
        }
        ObjectNode ir = golden(PENNINGJAAR);
        ((ObjectNode) ir.get("model")).putObject("dagsoortPerEenheid");
        assertTrue(weigert(ir, "/model/dagsoortPerEenheid").getMessage().contains("unknown field"));
    }

    // ---------------- the exact factor ----------------

    @Test void de_factor_is_een_exacte_breuk_in_laagste_termen() throws Exception {
        String[][] rijen = {
                { "12", FACTOR, "expected an exact fraction {num, den}, got 12" },
                { "\"12\"", FACTOR, "expected an exact fraction {num, den}, got \"12\"" },
                { "{\"num\":1}", FACTOR, "missing field 'den'" },
                { "{\"num\":1,\"den\":0}", FACTOR + "/den", "expected an integer of at least 1, got 0" },
                { "{\"num\":0,\"den\":1}", FACTOR + "/num", "expected an integer of at least 1, got 0" },
                { "{\"num\":-3,\"den\":1}", FACTOR + "/num", "expected an integer of at least 1, got -3" },
                { "{\"num\":1.5,\"den\":1}", FACTOR + "/num", "expected an integer, got 1.5" },
                { "{\"num\":\"1\",\"den\":1}", FACTOR + "/num", "expected an integer, got \"1\"" },
                { "{\"num\":2,\"den\":4}", FACTOR, IrLoader.FACTOR_LAAGSTE_TERMEN },
                { "{\"num\":1,\"den\":1,\"node\":\"Factor\"}", FACTOR + "/node", "unknown field" },
                { "null", FACTOR, "null is neither" } };
        for (String[] rij : rijen) {
            ObjectNode ir = golden(PENNINGJAAR);
            ((ObjectNode) ir.at(EENHEID)).set("factorToCanonical", JSON.readTree(rij[0]));
            IrLoader.Documentfout e = weigert(ir, rij[1]);
            assertTrue(e.getMessage().contains(rij[2]), rij[0] + ": " + e.getMessage());
        }
    }

    @Test void een_breuk_wordt_de_decimale_factor_van_het_record_en_blijft_in_het_stelsel() throws Exception {
        // the record's decimal is the fraction rounded to 16 significant digits, a whole number exactly; the unit
        // system keeps the fraction itself
        ObjectNode ir = golden(PENNINGJAAR);
        ArrayNode eenheden = (ArrayNode) ir.at(STELSEL + "/units");
        eenheden.add(JSON.readTree("{\"node\":\"Unit\",\"abbreviation\":\"ct\",\"system\":\"Munt\",\"canonical\":\"EUR\","
                + "\"factorToCanonical\":{\"num\":1,\"den\":100}}"));
        eenheden.add(JSON.readTree("{\"node\":\"Unit\",\"abbreviation\":\"kwartje\",\"system\":\"Munt\","
                + "\"canonical\":\"EUR\",\"factorToCanonical\":{\"num\":1,\"den\":24}}"));
        eenheden.add(JSON.readTree("{\"node\":\"Unit\",\"abbreviation\":\"daalder\",\"system\":\"Munt\","
                + "\"canonical\":\"EUR\",\"factorToCanonical\":{\"num\":12,\"den\":1}}"));
        Eenheidstelsel munt = IrLoader.load(ir).eenheidstelsels.get("Munt");
        assertEquals("0.01", munt.eenheden.get("ct").factorNaarCanoniek().toPlainString());
        assertEquals("0.04166666666666667", munt.eenheden.get("kwartje").factorNaarCanoniek().toPlainString());
        assertEquals("12", munt.eenheden.get("daalder").factorNaarCanoniek().toPlainString());
        assertEquals("1", munt.eenheden.get("EUR").factorNaarCanoniek().toPlainString());
        assertEquals(new Eenheidstelsel.Factor(BigInteger.ONE, BigInteger.valueOf(24)), munt.exacteFactoren.get("kwartje"));
        assertEquals(new Eenheidstelsel.Factor(BigInteger.valueOf(12), BigInteger.ONE), munt.exacteFactoren.get("daalder"));
        assertEquals(Eenheidstelsel.Factor.EEN, munt.exacteFactoren.get("EUR"));
    }

    // ---------------- the sign and the presentation tag ----------------

    @Test void het_teken_is_verplicht_en_de_tag_hoort_bij_een_declaratie() throws Exception {
        ObjectNode ir = golden(PENNINGJAAR);
        ((ObjectNode) ir.at(GETAL)).putNull("sign");
        assertTrue(weigert(ir, GETAL + "/sign").getMessage().contains("expected a string, got null"));

        ir = golden(PENNINGJAAR);
        ((ObjectNode) ir.at(GETAL)).remove("sign");
        assertTrue(weigert(ir, GETAL).getMessage().contains("missing field 'sign'"));

        ir = golden(PENNINGJAAR);
        ((ObjectNode) ir.at(GETAL)).put("sign", "ONBEKEND");
        assertTrue(weigert(ir, GETAL + "/sign").getMessage().contains("'ONBEKEND' is not one of"));

        ir = golden(VONDSTTARIEF);
        ((ObjectNode) ir.at(TARIEF)).put("presentation", "procent");
        assertTrue(weigert(ir, TARIEF + "/presentation").getMessage().contains("'procent' is not one of [percentage]"));

        ir = golden(VONDSTTARIEF);
        ((ObjectNode) ir.at(TARIEF)).putNull("presentation");
        assertTrue(weigert(ir, TARIEF + "/presentation").getMessage().contains("null is neither"));

        // WF-007: the tag stands at a declaration, and a type annotation is none
        ir = golden(VONDSTTARIEF);
        ((ObjectNode) ir.at(ANNOTATIE)).put("presentation", "percentage");
        assertTrue(weigert(ir, ANNOTATIE + "/presentation").getMessage().contains(IrLoader.TAG_OP_ANNOTATIE));
    }

    @Test void een_percentage_met_een_teken_draagt_deze_backend_niet() throws Exception {
        // the record's percentage has no sign to hold one, so the well-formed document is a form not carried
        ObjectNode ir = golden(VONDSTTARIEF);
        ((ObjectNode) ir.at(TARIEF)).put("sign", "POSITIEF");
        IrLoader.Refusal r = assertThrows(IrLoader.Refusal.class, () -> IrLoader.loadAll(ir));
        assertEquals("Number.sign", r.form(), r.getMessage());
    }

    // ---------------- the date ----------------

    @Test void een_datum_draagt_zijn_precisie() throws Exception {
        ObjectNode ir = golden(MELDSTIP);
        String datum = eerste(ir, "Date", "");
        ((ObjectNode) ir.at(datum)).remove("precision");
        assertTrue(weigert(ir, datum).getMessage().contains("missing field 'precision'"));

        ir = golden(MELDSTIP);
        ((ObjectNode) ir.at(datum)).put("precision", "second");
        assertTrue(weigert(ir, datum + "/precision").getMessage().contains("'second' is not one of [day, millisecond]"));
    }

    // ---------------- the spellings ----------------

    @Test void spellingen_staan_alleen_op_de_declaratie_en_nooit_gelijk_aan_de_afkorting() throws Exception {
        ObjectNode ir = golden(PENNINGJAAR);
        ((ObjectNode) ir.at(EENHEID)).putArray("spellings").add("euro");
        onbekendVeld(ir, EENHEID + "/spellings", "Unit");

        String euro = STELSEL + "/units/0";
        ir = golden(PENNINGJAAR);
        ((ObjectNode) ir.at(euro)).putArray("spellings");
        assertTrue(weigert(ir, euro + "/spellings").getMessage().contains("an empty list of spellings"));

        ir = golden(PENNINGJAAR);
        ((ObjectNode) ir.at(euro)).putArray("spellings").add("euro").add("EUR");
        assertTrue(weigert(ir, euro + "/spellings/1").getMessage().contains(IrLoader.SPELLING_IS_AFKORTING));

        ir = golden(PENNINGJAAR);
        ((ObjectNode) ir.at(euro)).putNull("spellings");
        assertTrue(weigert(ir, euro + "/spellings").getMessage().contains("null is neither"));

        ir = golden(PENNINGJAAR);
        ((ArrayNode) ir.at(STELSEL + "/units")).add(ir.at(euro).deepCopy());
        assertTrue(weigert(ir, STELSEL + "/units/1/abbreviation").getMessage().contains("declared twice"));
    }

    // ---------------- the parameter ----------------

    @Test void een_parameter_draagt_zijn_datatype_en_een_tijdlijnfacet_zonder_naam() throws Exception {
        ObjectNode ir = golden(PENNINGJAAR);
        ((ObjectNode) ir.at(PARAMETER)).putNull("datatype");
        assertTrue(weigert(ir, GETAL).getMessage().contains("expected a datatype object, got null"));

        ir = golden(PENNINGJAAR);
        ((ObjectNode) ir.at(PARAMETER)).remove("datatype");
        assertTrue(weigert(ir, PARAMETER).getMessage().contains("missing field 'datatype'"));

        String facet = "/model/parameters/1/timeline";
        ir = golden(PENNINGJAAR);
        ((ObjectNode) ir.at(facet)).put("timelineName", "boekjaar");
        onbekendVeld(ir, facet + "/timelineName", "TimelineFacet");

        ir = golden(PENNINGJAAR);
        ((ObjectNode) ir.at("/model/parameters/1")).putNull("timeline");
        assertTrue(weigert(ir, facet).getMessage().contains("null is neither"));

        ObjectNode tweeJaar = golden(PENNINGJAAR);
        ((ObjectNode) tweeJaar.at(facet)).put("count", 2);
        IrLoader.Refusal r = assertThrows(IrLoader.Refusal.class, () -> IrLoader.loadAll(tweeJaar));
        assertEquals("Parameter.timeline", r.form(), r.getMessage());
    }

    // ---------------- read back one to one ----------------

    @Test void de_lader_legt_de_wire_een_op_een_op_de_records() throws Exception {
        RegelSpraakModel m = IrLoader.load(golden(PENNINGJAAR));
        // a number declared without a sign reads as ONBEPAALD, the sign the wire writes for it
        Datatype.Numeriek drempel = assertInstanceOf(Datatype.Numeriek.class, m.parameters.get("vondstdrempel").datatype());
        assertEquals(Datatype.Teken.ONBEPAALD, drempel.teken());
        assertNull(drempel.eenheid());
        assertNull(m.parameters.get("vondstdrempel").granulariteit());
        assertEquals(Granulariteit.JAAR, m.parameters.get("penningbedrag").granulariteit());
        Eenheidstelsel munt = m.eenheidstelsels.get("Munt");
        Eenheid eur = munt.eenheden.get("EUR");
        assertEquals(List.of("EUR"), List.copyOf(munt.eenheden.keySet()));
        assertEquals(List.of("€", "euro", "euros"), List.copyOf(munt.aliassen.keySet()));
        for (Eenheid e : munt.aliassen.values()) assertEquals(eur, e);
        assertEquals(eur, ((Datatype.Numeriek) m.parameters.get("penningbedrag").datatype()).eenheid());

        // a number with the unit '%' is the record's percentage, tagged or not, and its unit is the built-in one
        m = IrLoader.load(golden(VONDSTTARIEF));
        Datatype.Percentage tarief = assertInstanceOf(Datatype.Percentage.class,
                m.objecttypen.get("Vondst").attributen.get("vondsttarief"));
        assertEquals(Datatype.Percentage.PROCENT, tarief.eenheid());
        assertInstanceOf(Datatype.Percentage.class, m.resolvedRegels.get(0).waarde().type());

        // the two precisions are the two date records
        ObjectNode ir = golden(MELDSTIP);
        String datum = eerste(ir, "Date", "");
        assertEquals("millisecond", ir.at(datum + "/precision").asText());
        assertTrue(bevat(IrLoader.load(ir), Datatype.DatumTijd.class));
        ((ObjectNode) ir.at(datum)).put("precision", "day");
        assertTrue(bevat(IrLoader.load(ir), Datatype.DatumInDagen.class));
    }

    // ---------------- helpers ----------------

    private static boolean bevat(RegelSpraakModel m, Class<? extends Datatype> soort) {
        return m.objecttypen.values().stream().flatMap(ot -> ot.attributen.values().stream()).anyMatch(soort::isInstance);
    }

    /** The pointer of the first node of kind {@code soort} under {@code n}, in document order. */
    private static String eerste(JsonNode n, String soort, String pad) {
        if (n.isObject()) {
            if (soort.equals(n.path("node").asText())) return pad;
            for (var it = n.fields(); it.hasNext(); ) {
                var e = it.next();
                String p = eerste(e.getValue(), soort, pad + "/" + e.getKey());
                if (p != null) return p;
            }
        } else if (n.isArray()) {
            for (int i = 0; i < n.size(); i++) {
                String p = eerste(n.get(i), soort, pad + "/" + i);
                if (p != null) return p;
            }
        }
        return null;
    }

    private static void zet(ObjectNode ir, String pad, String json) throws Exception {
        int slash = pad.lastIndexOf('/');
        ((ObjectNode) ir.at(pad.substring(0, slash))).set(pad.substring(slash + 1), JSON.readTree(json));
    }

    private static IrLoader.Documentfout weigert(JsonNode ir, String plek) {
        IrLoader.Documentfout e = assertThrows(IrLoader.Documentfout.class, () -> IrLoader.loadAll(ir),
                "verwacht een documentfout op " + plek);
        assertEquals(plek, e.plek(), e.getMessage());
        return e;
    }

    private static void onbekendVeld(JsonNode ir, String plek, String knoop) {
        IrLoader.Documentfout e = weigert(ir, plek);
        assertTrue(e.getMessage().contains("unknown field on " + knoop), e.getMessage());
    }

    private static ObjectNode golden(String naam) throws Exception {
        Path ir = CORPUS.resolve(naam).resolve("ir.json");
        assertTrue(Files.exists(ir), "golden ir.json present: " + ir.toAbsolutePath());
        ObjectNode doc = (ObjectNode) JSON.readTree(Files.readString(ir));
        IrLoader.loadAll(doc);   // the untwisted golden loads, so the refusal below is the twist's alone
        return doc;
    }
}
