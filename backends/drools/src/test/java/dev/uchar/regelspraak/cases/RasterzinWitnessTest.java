package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * The sentences in which a grid without an operation is refused (RUNTIME-SEMANTICS ch. 3.2), in both branches and
 * word for word those of the reference interpreter and the ASP backend: for a case timeline the sentence of ref's
 * case loader with its position ({@code J1.m2}, {@code expected J1.m2}), for the asOfDate bucket of a scalar the
 * sentence of ref's grid function. Three grids lay no such period: a grid of several units, a variable start, whose
 * periods run from a per-object anchor the case does not give, and the calendar's day, week and quarter grid.
 *
 * <p><b>Red before the fix round of 2026-09-26</b> (measured on the union e6844fba): the case runner refused each of
 * them in words of its own, one of them in the v0 spelling of the granularity, for example
 * {@code multi-period grid (2 × MAAND) on 'm2' is not carried: a grid of several units carries no operation (ch. 3.2)}
 * and {@code variable-start grid on 'waterstand' is not carried: its periods run from a per-object anchor (ch. 3.2)},
 * where ref and clingo said {@code J1.m2: the declared grid is 2 month units, a multi-period grid, …}.
 *
 * <p>No golden carries these refusals: a case on such a grid is refused by every executor, so it cannot replay. ref's
 * suite ({@code WV-3.2-zinnen-*}) and the ASP backend's pin the same sentences.
 */
class RasterzinWitnessTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    private static final String GETAL = "{\"node\":\"Number\",\"whole\":true,\"decimals\":0,\"sign\":\"unspecified\"}";

    /** A Jutter with the temporal member {@code m} on {@code facet} and a scalar {@code t} a literal writes, so that
     *  no rule reads {@code m} and the document lowers in both branches: what is refused is the case, not a rule. */
    private static Path document(String facet, String tijdlijnen, String bepalingen, String extra) throws Exception {
        String ir = "{\"format\":\"uchar-ir-v1\",\"model\":{\"objectTypes\":[{\"node\":\"ObjectType\",\"name\":\"Jutter\","
                + "\"animate\":true,\"plural\":\"Jutters\",\"attributes\":[" + extra
                + "{\"node\":\"Attribute\",\"name\":\"m\",\"datatype\":" + GETAL + ",\"timeline\":" + facet + "},"
                + "{\"node\":\"Attribute\",\"name\":\"t\",\"datatype\":" + GETAL + "}],\"characteristics\":[]}],"
                + "\"factTypes\":[],\"parameters\":[],\"domains\":[],\"unitSystems\":[],\"dimensions\":[],"
                + "\"timelines\":[" + tijdlijnen + "],\"dayKinds\":[]},\"rules\":{\"rules\":[{\"node\":\"Rule\","
                + "\"name\":\"t\",\"versions\":[{\"validity\":{\"node\":\"Validity\"},\"target\":{\"node\":"
                + "\"AttributeReference\",\"attribute\":\"t\",\"subjectType\":\"Jutter\",\"source\":{\"snippet\":\"t\"}},"
                + "\"value\":{\"node\":\"Literal\",\"value\":{\"number\":\"7\"},\"type\":" + GETAL + ",\"source\":"
                + "{\"snippet\":\"7\"}},\"initialisation\":false,\"presentation\":\"calculation\"}]}],"
                + "\"dayKindDefinitions\":[],\"objectCreations\":[],\"allocations\":[],\"consistencies\":[],"
                + "\"uniquenesses\":[],\"factCreations\":[],\"startPointDeterminations\":[" + bepalingen + "],"
                + "\"ruleGroups\":[]},\"diagnostics\":[]}";
        Path tmp = Files.createTempFile("rasterzin", ".json");
        tmp.toFile().deleteOnExit();
        Files.writeString(tmp, ir);
        return tmp;
    }

    private static Path document(String facet) throws Exception {
        return document(facet, "", "", "");
    }

    private static final String MEERVOUDIG =
            "{\"node\":\"TimelineFacet\",\"granularity\":\"month\",\"count\":2,\"fixedStart\":\"2024-01-01\"}";
    private static final String KALENDERWEEK = "{\"node\":\"TimelineFacet\",\"granularity\":\"week\",\"count\":1}";
    private static final String KALENDERDAG = "{\"node\":\"TimelineFacet\",\"granularity\":\"day\",\"count\":1}";

    private static Path variabel() throws Exception {
        return document("{\"node\":\"TimelineFacet\",\"granularity\":\"year\",\"count\":1,\"timelineName\":\"boekjaar\"}",
                "{\"node\":\"Timeline\",\"name\":\"boekjaar\",\"variableStart\":true,\"granularity\":\"year\",\"count\":1}",
                "{\"node\":\"StartPointDetermination\",\"timeline\":\"boekjaar\",\"objectType\":\"Jutter\","
                        + "\"member\":\"meetdatum\"}",
                "{\"node\":\"Attribute\",\"name\":\"meetdatum\",\"datatype\":{\"node\":\"Date\",\"precision\":\"day\"}},");
    }

    /** A case with {@code m} set to {@code waarde} (a timeline or a scalar) and, when {@code verwacht} is not null,
     *  an expected timeline on {@code m}. */
    private static JsonNode zaak(String waarde, String verwacht) throws Exception {
        return JSON.readTree("{\"format\":\"uchar-case-v0\",\"asOfDate\":\"2024-06-01\",\"input\":{\"instances\":"
                + "[{\"label\":\"J1\",\"objectType\":\"Jutter\",\"attributes\":{" + (waarde == null ? "" : "\"m\":" + waarde)
                + "},\"roles\":{},\"characteristics\":[]}],\"parameters\":{}},\"expected\":["
                + (verwacht == null ? "" : "{\"kind\":\"attributeTimeline\",\"label\":\"J1\",\"attribute\":\"m\","
                        + "\"timeline\":" + verwacht + "},")
                + "{\"kind\":\"consistent\"}]}");
    }

    private static final String TIJDLIJN =
            "{\"timeline\":[{\"from\":\"2024-01-01\",\"to\":\"2024-03-01\",\"value\":{\"number\":\"3\"}}]}";
    private static final String SCALAIR = "{\"number\":\"3\"}";

    /** Both branches refuse the case in exactly {@code zin}. */
    private static void weigert(Path ir, JsonNode c, String zin) {
        for (boolean viaDrl : new boolean[] { false, true }) {
            CaseRunner.Excluded e = assertThrows(CaseRunner.Excluded.class, () -> CaseRunner.speelCase(ir, c, viaDrl),
                    viaDrl ? "drl-tekst" : "exec-model");
            assertEquals(zin, e.getMessage(), viaDrl ? "drl-tekst" : "exec-model");
        }
    }

    @Test void een_tijdlijn_op_een_raster_van_meer_perioden() throws Exception {
        String zin = ": the declared grid is 2 month units, a multi-period grid, which carries no operation, a case"
                + " input included (RUNTIME-SEMANTICS ch. 3.2, OPEN in ch. 3.12): refused as a v0 limit, not as a"
                + " defect of the case";
        weigert(document(MEERVOUDIG), zaak(TIJDLIJN, null), "J1.m" + zin);
        weigert(document(MEERVOUDIG), zaak(null, "[]"), "expected J1.m" + zin);
    }

    @Test void een_tijdlijn_op_een_variabel_startpunt() throws Exception {
        weigert(variabel(), zaak(TIJDLIJN, null), "J1.m: the declared grid is laid per object from the anchor of a"
                + " variable start, which this loader does not read (RUNTIME-SEMANTICS ch. 3.2): refused as a v0 limit,"
                + " not as a defect of the case");
    }

    @Test void een_tijdlijn_op_de_week_en_de_dag_van_de_kalender() throws Exception {
        weigert(document(KALENDERWEEK), zaak(TIJDLIJN, null), "J1.m: the declared grid is week, and v0 defines"
                + " calendar buckets for month and year only (RUNTIME-SEMANTICS ch. 3.2, OPEN in ch. 3.12), so this"
                + " loader has no week grid to check the segment boundaries against: refused as a v0 limit, not as a"
                + " defect of the case");
        weigert(document(KALENDERDAG), zaak(TIJDLIJN, null), "J1.m: the declared grid is day, which every day"
                + " boundary sits on, yet this loader checks a timeline's grid through the calendar bucketing and v0"
                + " defines buckets for month and year only (RUNTIME-SEMANTICS ch. 3.2, OPEN in ch. 3.12): refused as a"
                + " v0 limit, not as a defect of the case");
    }

    @Test void de_peildatumemmer_van_een_scalair() throws Exception {
        weigert(document(MEERVOUDIG), zaak(SCALAIR, null),
                "a multi-period grid (2 month periods) carries no operation (RUNTIME-SEMANTICS ch. 3.2, OPEN in ch. 3.12)");
        weigert(variabel(), zaak(SCALAIR, null), "a variable-start grid is laid per object from the anchor its"
                + " start-point determination binds, which this interpreter does not execute (ch. 3.2)");
        weigert(document(KALENDERWEEK), zaak(SCALAIR, null), "bucketing is defined for month and year only (ch. 3.2)");
    }
}
