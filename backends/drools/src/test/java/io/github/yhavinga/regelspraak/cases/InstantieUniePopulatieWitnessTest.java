package io.github.yhavinga.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * IR-SPEC ch. 4.3 / WF-110 (d) — an instance-set union over TWO POPULATIONS, refused BY NAME in both
 * transpiler branches.
 *
 * <p>Why this needs a witness of its own. The wire refuses the shape (WF-110 (d), negatief
 * {@code wf110-unie-ongelijke-elementtypes}) and so does the RegelSpraak frontend, so no golden can carry it
 * and no gate-12 channel will ever name it. What remains is the executor's own answer to a document that
 * reached it anyway: one accumulate has ONE source pattern and therefore one element CLASS, so a second
 * population would have to be dropped — and dropping it counts the wrong number silently, which is exactly
 * the failure the v0 toolchain's own marker at this shape exists to avoid. Both branches must refuse, and
 * both must say the same thing: a producer reading the report should not have to know which branch answered.
 *
 * <p>The counting half of the union — the numbers, the deduplication, the unfiltered selection — lives in
 * the corpus goldens {@code jutterswet-vondstunie} and {@code jutterswet-keurunie}, replayed by gate 12
 * against the absolutes ref and clingo pin.
 */
class InstantieUniePopulatieWitnessTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    @Test void een_unie_over_twee_populaties_wordt_in_beide_takken_benoemd_geweigerd(@TempDir Path dir)
            throws Exception {
        Path ir = dir.resolve("ir.json");
        Files.writeString(ir, DOC);
        BeideTakken.geweigerd(ir, JSON.readTree(GEVAL), "WF-110 (d)",
                "een unie over twee populaties hoort een benoemde weigering te zijn");
    }

    /** IR-SPEC ch. 4.3 / WF-110 (a) — the union standing where a VALUE is written.
     *
     *  <p>Rood-eerst, measured 2026-09-22 on {@code corpus/negatief/wf110-unie-als-regelwaarde}: ref answered an
     *  EVALUATION FAULT and the ASP backend a TRANSLATION FAULT, both citing WF-110; the exec-model branch
     *  skipped with the GENERIC {@code regel (waarde buiten scope)}, which names no construct; and the DRL-TEXT
     *  branch CRASHED — {@code NullPointerException} on {@code r.waarde().type().isNumeriek()}, because the
     *  union is the only loadable expression node whose {@code type()} is null by construction and the §4 unit
     *  restate read it unguarded. A crash names no form and reaches the gate as an ERROR instead of as a
     *  channel with a reason. This row is the fence for both halves: both branches refuse, and both name the
     *  rule in the SAME words. */
    @Test void de_unie_op_een_waardeplek_wordt_in_beide_takken_benoemd_geweigerd(@TempDir Path dir)
            throws Exception {
        Path ir = dir.resolve("ir.json");
        Files.writeString(ir, KALE_UNIE_DOC);
        BeideTakken.geweigerd(ir, JSON.readTree(GEVAL), "WF-110 (a)",
                "een unie op een waardeplek hoort een benoemde weigering te zijn, nooit een crash");
    }

    private static final String HEEL =
            "{\"node\":\"Number\",\"whole\":true,\"decimals\":0,\"sign\":\"NIET_NEGATIEF\"}";

    private static String objecttype(String naam, String mv) {
        return ("{\"node\":\"ObjectType\",\"name\":\"%s\",\"animate\":false,\"plural\":\"%s\","
                + "\"attributes\":[],\"characteristics\":[]}").formatted(naam, mv);
    }

    private static String feittype(String naam, String eenRol, String veelRol, String veelType, String veelMv) {
        return ("{\"node\":\"Feittype\",\"naam\":\"%s\",\"wederkerig\":false,\"rollen\":["
                + "{\"node\":\"Rol\",\"naam\":\"%s\",\"objecttypeNaam\":\"Jutter\",\"meervoud\":null,"
                + "\"meervoudig\":false},"
                + "{\"node\":\"Rol\",\"naam\":\"%s\",\"objecttypeNaam\":\"%s\",\"meervoud\":\"%s\","
                + "\"meervoudig\":true}]}").formatted(naam, eenRol, veelRol, veelType, veelMv);
    }

    private static String tak(String elementType, String feittype, String rol) {
        return ("{\"node\":\"InstanceSetBranch\",\"elementType\":\"%s\","
                + "\"sourceRole\":{\"node\":\"RolBinding\",\"feittype\":\"%s\",\"rol\":\"%s\"},"
                + "\"filter\":null,\"source\":{\"snippet\":\"een selectie\"}}").formatted(elementType, feittype, rol);
    }

    private static final String DOC = ("{\"format\":\"uchar-ir-v1\",\"model\":{\"objectTypes\":["
            + "{\"node\":\"ObjectType\",\"name\":\"Jutter\",\"animate\":true,\"plural\":\"Jutters\","
            + "\"attributes\":[{\"node\":\"Attribute\",\"name\":\"aantal strandvondsten\",\"datatype\":" + HEEL + "}],"
            + "\"characteristics\":[]},"
            + objecttype("Vondst", "Vondsten") + "," + objecttype("Kavel", "Kavels")
            + "],\"factTypes\":["
            + feittype("vondstaanmelding", "aanmelder", "aangemelde vondst", "Vondst", "aangemelde vondsten")
            + "," + feittype("kaveltoewijzing", "kavelhouder", "duinkavel", "Kavel", "duinkavels")
            + "],\"parameters\":[],\"domains\":[],\"unitSystems\":[],\"dimensions\":[],\"timelines\":[],"
            + "\"dayKinds\":[]},"
            + "\"rules\":{\"resolvedRegels\":[{\"node\":\"Rule\",\"name\":\"aantal strandvondsten\","
            + "\"versions\":[{\"validity\":{\"node\":\"Geldigheid\",\"vanaf\":null,\"tot\":null},"
            + "\"target\":{\"node\":\"AttribuutRef\","
            + "\"attribuut\":\"aantal strandvondsten\",\"objecttype\":\"Jutter\","
            + "\"source\":{\"snippet\":\"Het aantal strandvondsten van een Jutter\"}},"
            + "\"value\":{\"node\":\"Aggregation\",\"kind\":\"AANTAL\",\"arguments\":["
            + "{\"node\":\"InstanceSetUnion\",\"branches\":["
            + tak("Vondst", "vondstaanmelding", "aangemelde vondst") + ","
            + tak("Kavel", "kaveltoewijzing", "duinkavel")
            + "],\"source\":{\"snippet\":\"de unie\"}}],\"type\":" + HEEL + ",\"source\":{\"snippet\":\"het aantal …\"}},"
            + "\"initialisation\":false,\"presentation\":\"BEREKENING\"}]}],"
            + "\"resolvedDagsoorten\":[],\"objectCreaties\":[],\"verdelingen\":[],\"consistenties\":[],"
            + "\"uniciteiten\":[],\"feitCreaties\":[],\"resolvedStartpunten\":[],"
            + "\"regelGroepen\":[]},\"diagnostics\":[]}");

    /** The same model, with the union standing as the rule's WHOLE value — no count above it. */
    private static final String KALE_UNIE_DOC = DOC.replace(
            "\"value\":{\"node\":\"Aggregation\",\"kind\":\"AANTAL\",\"arguments\":[", "\"value\":")
            .replace("],\"type\":" + HEEL + ",\"source\":{\"snippet\":\"het aantal …\"}}", "");

    private static final String GEVAL = """
            { "format": "uchar-case-v0", "asOfDate": null,
              "input": { "instances": [
                  { "label": "J1", "objectType": "Jutter", "attributes": {},
                    "roles": { "aangemelde vondsten": ["V1"], "duinkavels": ["K1"] },
                    "characteristics": [] },
                  { "label": "V1", "objectType": "Vondst", "attributes": {}, "roles": {},
                    "characteristics": [] },
                  { "label": "K1", "objectType": "Kavel", "attributes": {}, "roles": {},
                    "characteristics": [] } ],
                "parameters": {} },
              "expected": [ { "kind": "attributeValue", "label": "J1",
                              "attribute": "aantal strandvondsten", "value": { "number": "2" } } ] }
            """;
}
