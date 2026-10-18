package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * The whole-period leeg/gevuld test over an operand reached across a CHAIN of to-one hops (RUNTIME-SEMANTICS ch.
 * 3.9: "a time-dependent attribute of the subject, or one reached over to-one hops"), played in both branches on the
 * document and the case the reference interpreter's and the ASP backend's suites hold for the same form: a Schuit
 * reaches its Werf over {@code ligplaats} and the Werf its Haven over {@code havenligging}; {@code meldpremie} is 40
 * when "het havengeld van de thuishaven van de werf gedurende het gehele jaar gevuld (leeg) is". H1 carries
 * {@code havengeld} over the whole of 2025, H2 over its first half; S1 reaches H1, S2 reaches H2, S3 reaches a Werf
 * without a Haven and S4 no Werf, and the last two read the null timeline, which touches no bucket and passes.
 * Gevuld holds for S1, S3 and S4, leeg for S3 and S4.
 *
 * <p><b>Red before the fix round of 2026-09-26</b> (measured on the union e6844fba): both branches declined the rule
 * with a reason that named no form — the exec-model branch {@code SKIP … (voorwaarde buiten Phase-B scope)}, the
 * DRL-text branch {@code MARKER … TODO §8.2 gehele-periode} — because each read a navigated whole-period operand over
 * one hop only, while ref and the ASP backend fold the chain. No golden carries a chain of hops under the qualifier,
 * so this class is the Drools third of the square the two OCaml suites pin.
 */
class WholePeriodChainWitnessTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    private static final String GETAL = "{\"node\":\"Number\",\"whole\":true,\"decimals\":0,\"sign\":\"unspecified\"}";

    private static Path document(String soort) throws Exception {
        String operand = "{\"node\":\"AttributeReference\",\"attribute\":\"havengeld\",\"subjectType\":\"Haven\","
                + "\"source\":{\"snippet\":\"het havengeld van de thuishaven van de werf\"},\"path\":["
                + "{\"node\":\"RoleBinding\",\"factType\":\"ligplaats\",\"role\":\"werf\"},"
                + "{\"node\":\"RoleBinding\",\"factType\":\"havenligging\",\"role\":\"thuishaven\"}]}";
        String ir = "{\"format\":\"uchar-ir-v1\",\"model\":{\"objectTypes\":["
                + "{\"node\":\"ObjectType\",\"name\":\"Haven\",\"animate\":false,\"plural\":\"Havens\",\"attributes\":["
                + "{\"node\":\"Attribute\",\"name\":\"havengeld\",\"datatype\":" + GETAL + ",\"timeline\":"
                + "{\"node\":\"TimelineFacet\",\"granularity\":\"month\",\"count\":1}}],\"characteristics\":[]},"
                + "{\"node\":\"ObjectType\",\"name\":\"Werf\",\"animate\":false,\"plural\":\"Werven\",\"attributes\":[],"
                + "\"characteristics\":[]},"
                + "{\"node\":\"ObjectType\",\"name\":\"Schuit\",\"animate\":false,\"plural\":\"Schuiten\",\"attributes\":["
                + "{\"node\":\"Attribute\",\"name\":\"meldpremie\",\"datatype\":" + GETAL + "}],\"characteristics\":[]}],"
                + "\"factTypes\":["
                + "{\"node\":\"FactType\",\"name\":\"ligplaats\",\"reciprocal\":false,\"roles\":["
                + "{\"node\":\"Role\",\"name\":\"werf\",\"objectType\":\"Werf\",\"many\":false},"
                + "{\"node\":\"Role\",\"name\":\"schuit\",\"objectType\":\"Schuit\",\"plural\":\"schuiten\",\"many\":true}]},"
                + "{\"node\":\"FactType\",\"name\":\"havenligging\",\"reciprocal\":false,\"roles\":["
                + "{\"node\":\"Role\",\"name\":\"thuishaven\",\"objectType\":\"Haven\",\"many\":false},"
                + "{\"node\":\"Role\",\"name\":\"aangesloten werf\",\"objectType\":\"Werf\","
                + "\"plural\":\"aangesloten werven\",\"many\":true}]}],"
                + "\"parameters\":[],\"domains\":[],\"unitSystems\":[],\"dimensions\":[],\"timelines\":[],\"dayKinds\":[]},"
                + "\"rules\":{\"rules\":[{\"node\":\"Rule\",\"name\":\"meldpremie\",\"versions\":[{"
                + "\"initialisation\":false,\"presentation\":\"calculation\",\"target\":{\"node\":\"AttributeReference\","
                + "\"attribute\":\"meldpremie\",\"subjectType\":\"Schuit\",\"source\":{\"snippet\":\"De meldpremie\"}},"
                + "\"value\":{\"node\":\"Literal\",\"value\":{\"number\":\"40\"},\"type\":" + GETAL
                + ",\"source\":{\"snippet\":\"40\"}},\"condition\":{\"node\":\"Predicate\",\"kind\":\"" + soort + "\","
                + "\"operand\":" + operand + ",\"negated\":false,\"digits\":0,\"wholePeriod\":{\"node\":\"WholePeriod\","
                + "\"granularity\":\"year\",\"negated\":false},\"source\":{\"snippet\":\"leeg-toets\"}},"
                + "\"validity\":{\"node\":\"Validity\"}}]}],"
                + "\"dayKindDefinitions\":[],\"objectCreations\":[],\"allocations\":[],\"consistencies\":[],"
                + "\"uniquenesses\":[],\"factCreations\":[],\"startPointDeterminations\":[],\"ruleGroups\":[]},"
                + "\"diagnostics\":[]}";
        Path tmp = Files.createTempFile("geheel-periode-keten", ".json");
        tmp.toFile().deleteOnExit();
        Files.writeString(tmp, ir);
        return tmp;
    }

    private static JsonNode caseDocument(List<String> hits) throws Exception {
        String whole = "{\"timeline\":[{\"from\":\"2025-01-01\",\"to\":\"2026-01-01\",\"value\":{\"number\":\"312\"}}]}";
        String half = "{\"timeline\":[{\"from\":\"2025-01-01\",\"to\":\"2025-07-01\",\"value\":{\"number\":\"312\"}}]}";
        StringBuilder rows = new StringBuilder();
        for (String s : List.of("S1", "S2", "S3", "S4"))
            rows.append(hits.contains(s)
                    ? "{\"kind\":\"attributeValue\",\"label\":\"" + s + "\",\"attribute\":\"meldpremie\","
                            + "\"value\":{\"number\":\"40\"}},"
                    : "{\"kind\":\"attributeEmpty\",\"label\":\"" + s + "\",\"attribute\":\"meldpremie\"},");
        return JSON.readTree("{\"format\":\"uchar-case-v0\",\"asOfDate\":null,\"input\":{\"instances\":["
                + "{\"label\":\"H1\",\"objectType\":\"Haven\",\"attributes\":{\"havengeld\":" + whole + "},\"roles\":{},"
                + "\"characteristics\":[]},"
                + "{\"label\":\"H2\",\"objectType\":\"Haven\",\"attributes\":{\"havengeld\":" + half + "},\"roles\":{},"
                + "\"characteristics\":[]},"
                + "{\"label\":\"W1\",\"objectType\":\"Werf\",\"attributes\":{},\"roles\":{\"thuishaven\":[\"H1\"]},"
                + "\"characteristics\":[]},"
                + "{\"label\":\"W2\",\"objectType\":\"Werf\",\"attributes\":{},\"roles\":{\"thuishaven\":[\"H2\"]},"
                + "\"characteristics\":[]},"
                + "{\"label\":\"W3\",\"objectType\":\"Werf\",\"attributes\":{},\"roles\":{},\"characteristics\":[]},"
                + "{\"label\":\"S1\",\"objectType\":\"Schuit\",\"attributes\":{},\"roles\":{\"werf\":[\"W1\"]},"
                + "\"characteristics\":[]},"
                + "{\"label\":\"S2\",\"objectType\":\"Schuit\",\"attributes\":{},\"roles\":{\"werf\":[\"W2\"]},"
                + "\"characteristics\":[]},"
                + "{\"label\":\"S3\",\"objectType\":\"Schuit\",\"attributes\":{},\"roles\":{\"werf\":[\"W3\"]},"
                + "\"characteristics\":[]},"
                + "{\"label\":\"S4\",\"objectType\":\"Schuit\",\"attributes\":{},\"roles\":{},\"characteristics\":[]}],"
                + "\"parameters\":{}},\"expected\":[" + rows + "{\"kind\":\"consistent\"}]}");
    }

    @Test void filled_over_a_chain_of_two_hops() throws Exception {
        BothBranches.green(document("filled"), caseDocument(List.of("S1", "S3", "S4")), "gevuld over a chain of two hops");
    }

    @Test void empty_over_a_chain_of_two_hops() throws Exception {
        BothBranches.green(document("empty"), caseDocument(List.of("S3", "S4")), "leeg over a chain of two hops");
    }
}
