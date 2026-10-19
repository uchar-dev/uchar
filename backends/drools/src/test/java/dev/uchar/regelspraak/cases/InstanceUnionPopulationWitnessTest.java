package dev.uchar.regelspraak.cases;

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
 * <p>Why this needs a witness of its own. The wire refuses the shape (WF-110 (d), negative
 * {@code wf110-union-unequal-element-types}) and so does the RegelSpraak frontend, so no golden can carry it
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
class InstanceUnionPopulationWitnessTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    @Test void a_union_over_two_populations_is_refused_by_name_in_both_branches(@TempDir Path dir)
            throws Exception {
        Path ir = dir.resolve("ir.json");
        Files.writeString(ir, DOC);
        BothBranches.refused(ir, JSON.readTree(CASE), "WF-110 (d)",
                "een unie over twee populaties hoort een benoemde weigering te zijn");
    }

    /** IR-SPEC ch. 4.3 / WF-110 (a) — the union standing where a VALUE is written.
     *
     *  <p>Rood-eerst, measured 2026-09-22 on {@code corpus/negative/wf110-union-as-rule-value}: ref answered an
     *  EVALUATION FAULT and the ASP backend a TRANSLATION FAULT, both citing WF-110; the exec-model branch
     *  skipped with the GENERIC {@code regel (waarde buiten scope)}, which names no construct; and the DRL-TEXT
     *  branch CRASHED — {@code NullPointerException} on {@code r.value().type().isNumber()}, because the
     *  union is the only loadable expression node whose {@code type()} is null by construction and the §4 unit
     *  restate read it unguarded. A crash names no form and reaches the gate as an ERROR instead of as a
     *  channel with a reason. This row is the fence for both halves: both branches refuse, and both name the
     *  rule in the SAME words. */
    @Test void the_union_on_a_value_site_is_refused_by_name_in_both_branches(@TempDir Path dir)
            throws Exception {
        Path ir = dir.resolve("ir.json");
        Files.writeString(ir, BARE_UNION_DOC);
        BothBranches.refused(ir, JSON.readTree(CASE), "WF-110 (a)",
                "een unie op een waardeplek hoort een benoemde weigering te zijn, nooit een crash");
    }

    private static final String WHOLE =
            "{\"node\":\"Number\",\"whole\":true,\"decimals\":0,\"sign\":\"nonNegative\"}";

    private static String objectType(String naam, String mv) {
        return ("{\"node\":\"ObjectType\",\"name\":\"%s\",\"animate\":false,\"plural\":\"%s\","
                + "\"attributes\":[],\"characteristics\":[]}").formatted(naam, mv);
    }

    private static String factType(String naam, String oneRole, String manyRole, String manyType, String manyMv) {
        return ("{\"node\":\"FactType\",\"name\":\"%s\",\"reciprocal\":false,\"roles\":["
                + "{\"node\":\"Role\",\"name\":\"%s\",\"objectType\":\"Jutter\","
                + "\"many\":false},"
                + "{\"node\":\"Role\",\"name\":\"%s\",\"objectType\":\"%s\",\"plural\":\"%s\","
                + "\"many\":true}]}").formatted(naam, oneRole, manyRole, manyType, manyMv);
    }

    private static String branch(String elementType, String factType, String role) {
        return ("{\"node\":\"InstanceSetBranch\",\"elementType\":\"%s\","
                + "\"sourceRole\":{\"node\":\"RoleBinding\",\"factType\":\"%s\",\"role\":\"%s\"},"
                + "\"filter\":null,\"source\":{\"snippet\":\"een selectie\"}}").formatted(elementType, factType, role);
    }

    private static final String DOC = ("{\"format\":\"uchar-ir-v1\",\"model\":{\"objectTypes\":["
            + "{\"node\":\"ObjectType\",\"name\":\"Jutter\",\"animate\":true,\"plural\":\"Jutters\","
            + "\"attributes\":[{\"node\":\"Attribute\",\"name\":\"aantal strandvondsten\",\"datatype\":" + WHOLE + "}],"
            + "\"characteristics\":[]},"
            + objectType("Vondst", "Vondsten") + "," + objectType("Kavel", "Kavels")
            + "],\"factTypes\":["
            + factType("vondstaanmelding", "aanmelder", "aangemelde vondst", "Vondst", "aangemelde vondsten")
            + "," + factType("kaveltoewijzing", "kavelhouder", "duinkavel", "Kavel", "duinkavels")
            + "],\"parameters\":[],\"domains\":[],\"unitSystems\":[],\"dimensions\":[],\"timelines\":[],"
            + "\"dayKinds\":[]},"
            + "\"rules\":{\"rules\":[{\"node\":\"Rule\",\"name\":\"aantal strandvondsten\","
            + "\"versions\":[{\"validity\":{\"node\":\"Validity\"},"
            + "\"target\":{\"node\":\"AttributeReference\","
            + "\"attribute\":\"aantal strandvondsten\",\"subjectType\":\"Jutter\","
            + "\"source\":{\"snippet\":\"Het aantal strandvondsten van een Jutter\"}},"
            + "\"value\":{\"node\":\"Aggregation\",\"kind\":\"count\",\"arguments\":["
            + "{\"node\":\"InstanceSetUnion\",\"branches\":["
            + branch("Vondst", "vondstaanmelding", "aangemelde vondst") + ","
            + branch("Kavel", "kaveltoewijzing", "duinkavel")
            + "],\"source\":{\"snippet\":\"de unie\"}}],\"type\":" + WHOLE + ",\"source\":{\"snippet\":\"het aantal …\"}},"
            + "\"initialisation\":false,\"presentation\":\"calculation\"}]}],"
            + "\"dayKindDefinitions\":[],\"objectCreations\":[],\"allocations\":[],\"consistencies\":[],"
            + "\"uniquenesses\":[],\"factCreations\":[],\"startPointDeterminations\":[],"
            + "\"ruleGroups\":[]},\"diagnostics\":[]}");

    /** The same model, with the union standing as the rule's WHOLE value — no count above it. */
    private static final String BARE_UNION_DOC = DOC.replace(
            "\"value\":{\"node\":\"Aggregation\",\"kind\":\"count\",\"arguments\":[", "\"value\":")
            .replace("],\"type\":" + WHOLE + ",\"source\":{\"snippet\":\"het aantal …\"}}", "");

    private static final String CASE = """
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
