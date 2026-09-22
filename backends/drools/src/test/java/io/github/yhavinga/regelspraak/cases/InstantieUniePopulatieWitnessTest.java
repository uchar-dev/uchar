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

    private static final String HEEL =
            "{\"node\":\"Numeriek\",\"geheel\":true,\"decimalen\":0,\"teken\":\"NIET_NEGATIEF\",\"eenheid\":null}";

    private static String objecttype(String naam, String mv) {
        return ("\"%s\":{\"node\":\"Objecttype\",\"naam\":\"%s\",\"attributen\":{},\"attribuutMeervoud\":{},"
                + "\"attribuutDimensies\":{},\"attribuutGranulariteit\":{},\"attribuutTijdlijnAantal\":{},"
                + "\"attribuutTijdlijnNaam\":{},\"kenmerken\":[],\"kenmerkSoorten\":{},"
                + "\"kenmerkGranulariteit\":{},\"kenmerkTijdlijnAantal\":{},\"bezield\":false,"
                + "\"meervoud\":\"%s\"}").formatted(naam, naam, mv);
    }

    private static String feittype(String naam, String eenRol, String veelRol, String veelType, String veelMv) {
        return ("\"%s\":{\"node\":\"Feittype\",\"naam\":\"%s\",\"wederkerig\":false,\"rollen\":["
                + "{\"node\":\"Rol\",\"naam\":\"%s\",\"objecttypeNaam\":\"Jutter\",\"meervoud\":null,"
                + "\"meervoudig\":false},"
                + "{\"node\":\"Rol\",\"naam\":\"%s\",\"objecttypeNaam\":\"%s\",\"meervoud\":\"%s\","
                + "\"meervoudig\":true}]}").formatted(naam, naam, eenRol, veelRol, veelType, veelMv);
    }

    private static String tak(String elementType, String feittype, String rol) {
        return ("{\"node\":\"InstanceSetBranch\",\"elementType\":\"%s\","
                + "\"sourceRole\":{\"node\":\"RolBinding\",\"feittype\":\"%s\",\"rol\":\"%s\"},"
                + "\"filter\":null,\"bron\":\"een selectie\"}").formatted(elementType, feittype, rol);
    }

    private static final String DOC = ("{\"formaat\":\"regelspraak-ir-v0\",\"objecttypen\":{"
            + "\"Jutter\":{\"node\":\"Objecttype\",\"naam\":\"Jutter\","
            + "\"attributen\":{\"aantal strandvondsten\":" + HEEL + "},\"attribuutMeervoud\":{},"
            + "\"attribuutDimensies\":{},\"attribuutGranulariteit\":{},\"attribuutTijdlijnAantal\":{},"
            + "\"attribuutTijdlijnNaam\":{},\"kenmerken\":[],\"kenmerkSoorten\":{},"
            + "\"kenmerkGranulariteit\":{},\"kenmerkTijdlijnAantal\":{},\"bezield\":true,"
            + "\"meervoud\":\"Jutters\"},"
            + objecttype("Vondst", "Vondsten") + "," + objecttype("Kavel", "Kavels")
            + "},\"domeinen\":{},\"parameters\":{},\"eenheidstelsels\":{},\"feittypen\":{"
            + feittype("vondstaanmelding", "aanmelder", "aangemelde vondst", "Vondst", "aangemelde vondsten")
            + "," + feittype("kaveltoewijzing", "kavelhouder", "duinkavel", "Kavel", "duinkavels")
            + "},\"dimensies\":{},\"tijdlijnen\":[],\"dagsoorten\":[],\"dagsoortPerEenheid\":{},"
            + "\"regels\":[{\"node\":\"Regel\",\"naam\":\"aantal strandvondsten\",\"soort\":\"regel\"}],"
            + "\"resolvedRegels\":[{\"node\":\"ResolvedRegel\",\"naam\":\"aantal strandvondsten\","
            + "\"soort\":\"BEREKENING\",\"doel\":{\"node\":\"AttribuutRef\","
            + "\"attribuut\":\"aantal strandvondsten\",\"objecttype\":\"Jutter\","
            + "\"onderwerpBron\":\"een Jutter\",\"navigatieRol\":null,\"type\":" + HEEL + ","
            + "\"bron\":\"Het aantal strandvondsten van een Jutter\",\"set\":false,\"navigatiePad\":[]},"
            + "\"waarde\":{\"node\":\"Functie\",\"kind\":\"AANTAL\",\"argumenten\":["
            + "{\"node\":\"InstanceSetUnion\",\"branches\":["
            + tak("Vondst", "vondstaanmelding", "aangemelde vondst") + ","
            + tak("Kavel", "kaveltoewijzing", "duinkavel")
            + "],\"bron\":\"de unie\"}],\"elementObjecttype\":null,\"leegwaarde\":null,\"filter\":null,"
            + "\"dimensieSelectie\":null,\"type\":" + HEEL + ",\"bron\":\"het aantal …\","
            + "\"granulariteit\":null},\"voorwaarde\":null,"
            + "\"geldigheid\":{\"node\":\"Geldigheid\",\"vanaf\":null,\"tot\":null}}],"
            + "\"resolvedDagsoorten\":[],\"objectCreaties\":[],\"verdelingen\":[],\"consistenties\":[],"
            + "\"uniciteiten\":[],\"uniciteitUnies\":[],\"feitCreaties\":[],\"resolvedStartpunten\":[],"
            + "\"regelGroepen\":[],\"diagnostics\":[]}");

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
