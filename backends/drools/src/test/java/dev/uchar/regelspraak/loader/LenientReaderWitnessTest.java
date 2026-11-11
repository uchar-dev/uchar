package dev.uchar.regelspraak.loader;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The readers of the loader that take an optional field ({@code text}, {@code bool}, {@code intOf},
 * {@code stringList}, {@code elements}) read an absent field as none, and refuse a value of another JSON type at the
 * field's pointer, as the strict readers do. An integer beyond a Java {@code int} is well-formed (the OCaml loader
 * reads it into its 63-bit int, and the verifier admits a threshold of 4294967297) and is a form this backend does
 * not carry, a {@link IrLoader.Refusal} under {@code <node>.<field>}.
 *
 * <p>Measured on {@code ccf6de21c}: every twist below loaded. A threshold of 4294967297 read as 1 through
 * {@code asInt}, so "at least 4294967297 of" played as "at least 1 of"; {@code "3"} and {@code 3.0} read as 3;
 * {@code parts: {}} read as a compound without parts; {@code negated: "true"} as true; an attribute name {@code 7}
 * as the name "7"; a label list written as a string as no labels.
 *
 * <p>Every fixture is a corpus golden with ONE twist in memory; the corpus file is read and never written.
 */
class LenientReaderWitnessTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    /** cwd under surefire is the module dir (backends/drools); the corpus is two levels up. */
    private static final Path CORPUS = Path.of("..", "..", "corpus", "golden");

    /** Rule 2's condition is a Compound "at least 3"; rule 0 rounds a quotient inside its condition. */
    private static final String SEINWACHT = "jutterswet-seinwacht";
    private static final String COMPOUND = "/rules/rules/2/versions/0/condition";
    private static final String ROUNDING = "/rules/rules/0/versions/0/condition/parts/2/left";
    /** Rule 0's condition is a QuantifierFold "at least 2". */
    private static final String VUURTOREN = "jutterswet-vuurtoren";
    private static final String FOLD = "/rules/rules/0/versions/0/condition";
    /** Rule 2's condition is a Predicate of kind filled, digits 0. */
    private static final String AFWEZIGHEIDSMARKERS = "jutterswet-afwezigheidsmarkers-cel";
    private static final String PREDICATE = "/rules/rules/2/versions/0/condition";
    /** The named timeline "het penningjaar". */
    private static final String PENNINGJAAR = "jutterswet-penningjaar";
    private static final String TIMELINE = "/model/timelines/0";
    /** Rule 0's condition is a CharacteristicCheck with host Vondst. */
    private static final String KEURHOUDER = "jutterswet-keurhouder";
    private static final String CHECK = "/rules/rules/0/versions/0/condition";
    /** Rule 3 sums over a DimensionSelection of three seasons. */
    private static final String PLOEGSEIZOEN = "jutterswet-ploegseizoen";
    private static final String SELECTION = "/rules/rules/3/versions/0/value/dimensionSelections/0";

    @Test void an_integer_beyond_int_is_a_form_not_carried_and_not_wrapped() throws Exception {
        // 2^32 + 1 wraps to 1 under asInt, 2^32 + 2 to 2
        outsideInt(SEINWACHT, COMPOUND, "threshold", "Compound.threshold", 4_294_967_297L);
        outsideInt(VUURTOREN, FOLD, "threshold", "QuantifierFold.threshold", 4_294_967_298L);
        outsideInt(SEINWACHT, ROUNDING, "decimals", "Rounding.decimals", 4_294_967_298L);
        outsideInt(AFWEZIGHEIDSMARKERS, PREDICATE, "digits", "Predicate.digits", 4_294_967_296L);
        outsideInt(PENNINGJAAR, TIMELINE, "count", "Timeline.count", 4_294_967_297L);
    }

    @Test void an_integer_field_holds_an_integer() throws Exception {
        for (String written : new String[] { "\"3\"", "3.0", "null", "true" }) {
            ObjectNode ir = golden(SEINWACHT);
            ((ObjectNode) ir.at(COMPOUND)).set("threshold", JSON.readTree(written));
            assertTrue(refuses(ir, COMPOUND + "/threshold").getMessage().contains("expected an integer"), written);
        }
    }

    @Test void a_list_field_holds_an_array() throws Exception {
        ObjectNode ir = golden(SEINWACHT);
        ((ObjectNode) ir.at(COMPOUND)).putObject("parts");
        assertTrue(refuses(ir, COMPOUND + "/parts").getMessage().contains("expected an array"));

        ir = golden(PLOEGSEIZOEN);
        ((ObjectNode) ir.at(SELECTION)).put("labels", "voorjaar");
        assertTrue(refuses(ir, SELECTION + "/labels").getMessage().contains("expected an array"));

        ir = golden(PLOEGSEIZOEN);
        ((ObjectNode) ir.at(SELECTION)).putArray("labels").add(1);
        assertTrue(refuses(ir, SELECTION + "/labels/0").getMessage().contains("expected a string"));
    }

    @Test void a_boolean_field_holds_a_boolean() throws Exception {
        for (String written : new String[] { "\"true\"", "1", "null" }) {
            ObjectNode ir = golden(KEURHOUDER);
            ((ObjectNode) ir.at(CHECK)).set("negated", JSON.readTree(written));
            assertTrue(refuses(ir, CHECK + "/negated").getMessage().contains("expected a boolean"), written);
        }
    }

    @Test void a_name_field_holds_a_string() throws Exception {
        ObjectNode ir = golden(KEURHOUDER);
        ((ObjectNode) ir.at(CHECK)).put("characteristic", 7);
        assertTrue(refuses(ir, CHECK + "/characteristic").getMessage().contains("expected a string"));
    }

    // ---------------- helpers ----------------

    private static void outsideInt(String golden, String at, String field, String form, long value) throws Exception {
        ObjectNode ir = golden(golden);
        ((ObjectNode) ir.at(at)).put(field, value);
        IrLoader.Refusal r = assertThrows(IrLoader.Refusal.class, () -> IrLoader.loadAll(ir), form + " " + value);
        assertEquals(form, r.form(), r.getMessage());
        assertTrue(r.getMessage().contains("outside this backend's integer range"), r.getMessage());
        assertTrue(r.getMessage().contains(value + " at '" + at + "/" + field + "'"), r.getMessage());
    }

    private static IrLoader.DocumentError refuses(JsonNode ir, String site) {
        IrLoader.DocumentError e = assertThrows(IrLoader.DocumentError.class, () -> IrLoader.loadAll(ir),
                "verwacht een documentfout op " + site);
        assertEquals(site, e.site(), e.getMessage());
        return e;
    }

    private static ObjectNode golden(String name) throws Exception {
        Path ir = CORPUS.resolve(name).resolve("ir.json");
        assertTrue(Files.exists(ir), "golden ir.json present: " + ir.toAbsolutePath());
        ObjectNode doc = (ObjectNode) JSON.readTree(Files.readString(ir));
        IrLoader.loadAll(doc);   // the untwisted golden loads, so the refusal below is the twist's alone
        return doc;
    }
}
