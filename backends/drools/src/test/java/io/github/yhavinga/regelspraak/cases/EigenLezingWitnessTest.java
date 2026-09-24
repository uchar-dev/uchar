package io.github.yhavinga.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * IR-SPEC ch. 4.1a: a read without navigation bindings is the subject's OWN read, so its {@code objecttype} is the
 * subject type — equal, not a supertype and not another type. Both transpiler branches hold to that: the own-read
 * test is identity of the two types, and a bindingless read of any other type is refused: neither branch derives a
 * relation for it. A read of an INHERITED attribute stays an own read when it names the subject type itself.
 *
 * <p>Every model is an in-memory variant of {@code jutterswet-munttelling}: Munt ⊑ Vondst, and Vondst declares
 * {@code gewicht}. Each row adds a Munt attribute {@code muntwaarde} and one rule that writes it from an own read of
 * {@code gewicht}; only the type the read names differs. The corpus files are read and never written.
 */
class EigenLezingWitnessTest {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Path GOLDEN = Path.of("..", "..", "corpus", "golden", "jutterswet-munttelling");

    @Test void de_eigen_lezing_van_een_geerfd_attribuut_op_het_subjecttype_speelt() throws Exception {
        // M1 is a Munt with gewicht 7.00: its muntwaarde is its own gewicht
        BeideTakken.groen(tijdelijk(metMuntwaarde("Munt")), JSON.readTree(ZAAK), "own read of an inherited attribute");
    }

    @Test void de_eigen_lezing_via_het_supertype_weigeren_beide_takken() throws Exception {
        // Munt ⊑ Vondst, and still no own read: the read names a type that is not the subject's
        BeideTakken.geweigerd(tijdelijk(metMuntwaarde("Vondst")), JSON.readTree(ZAAK), "muntwaarde",
                "a bindingless read of the subject's supertype");
    }

    @Test void de_eigen_lezing_van_een_ander_type_weigeren_beide_takken() throws Exception {
        BeideTakken.geweigerd(tijdelijk(metMuntwaarde("Jutter")), JSON.readTree(ZAAK), "muntwaarde",
                "a bindingless read of an unrelated type");
    }

    // ---------------- fixtures ----------------

    private static final String ZAAK = """
            { "format": "uchar-case-v0", "asOfDate": null,
              "input": { "instances": [
                  { "label": "M1", "objectType": "Munt",
                    "attributes": { "gewicht": { "number": "7.00" }, "jaartal": { "number": "1650" } },
                    "roles": {}, "characteristics": [] } ],
                "parameters": {} },
              "expected": [
                { "kind": "attributeValue", "label": "M1", "attribute": "muntwaarde",
                  "value": { "number": "7.00" } } ] }
            """;

    /** The golden reduced to one rule, "De muntwaarde van een Munt moet berekend worden als het gewicht", whose read
     *  names {@code objecttype}. */
    private static ObjectNode metMuntwaarde(String objecttype) throws Exception {
        ObjectNode ir = (ObjectNode) JSON.readTree(Files.readString(GOLDEN.resolve("ir.json")));
        JsonNode gewicht = IrPad.attribute(IrPad.objectType(ir, "Vondst"), "gewicht").get("datatype");
        IrPad.addAttribute(IrPad.objectType(ir, "Munt"), "muntwaarde", gewicht.deepCopy());
        ObjectNode regel = IrPad.rules(ir).putArray("resolvedRegels").addObject();
        regel.put("node", "ResolvedRegel").put("naam", "muntwaarde").put("soort", "BEREKENING");
        regel.putObject("doel").put("node", "AttribuutRef").put("attribuut", "muntwaarde").put("objecttype", "Munt")
             .put("bron", "De muntwaarde van een Munt");
        regel.putObject("waarde").put("node", "AttribuutRef").put("attribuut", "gewicht").put("objecttype", objecttype)
             .put("bron", "het gewicht");
        regel.putNull("voorwaarde");
        regel.putObject("geldigheid").put("node", "Geldigheid").putNull("vanaf").putNull("tot");
        return ir;
    }

    private static Path tijdelijk(ObjectNode ir) throws Exception {
        Path tmp = Files.createTempFile("eigen-lezing", ".json");
        tmp.toFile().deleteOnExit();
        Files.writeString(tmp, ir.toString());
        return tmp;
    }
}
