package dev.uchar.regelspraak.loader;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import dev.uchar.regelspraak.model.RegelSpraakModel;
import dev.uchar.regelspraak.transpiler.kern.Tijdlijnen;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The fixed start point on the wire (IR-SPEC ch. 2.8): the loader reads it one to one, gives a doel on it the constant
 * anchor its walls are computed from, and refuses the three WF-115 witnesses in the verifier's sentence at the
 * verifier's pointer. Measured before this loader read the field: every one of the four documents was refused as an
 * unknown field on its Timeline or TimelineFacet.
 */
class VastStartpuntWitnessTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    /** cwd under surefire is the module dir (backends/drools); the corpus is two levels up. */
    private static final Path CORPUS = Path.of("..", "..", "corpus");

    private static JsonNode lees(String pad) throws Exception {
        Path ir = CORPUS.resolve(pad).resolve("ir.json");
        assertTrue(Files.exists(ir), "ir.json aanwezig: " + ir.toAbsolutePath());
        return JSON.readTree(Files.readString(ir));
    }

    @Test void de_golden_leest_een_op_een_en_geeft_het_anker() throws Exception {
        RegelSpraakModel m = IrLoader.load(lees("golden/jutterswet-vast-startpunt"));
        assertEquals("--10-01", m.tijdlijnen.get(0).vastStartpunt());
        RegelSpraakModel.Objecttype jutter = m.objecttypen.get("Jutter");
        assertEquals("--10-01", jutter.attribuutVastStartpunt.get("stormwachtdagen"));
        assertEquals("--01-31", jutter.attribuutVastStartpunt.get("maandwachtdagen"));
        assertEquals("2024-09-30", jutter.kenmerkVastStartpunt.get("wachtplichtig"));
        assertNull(jutter.attribuutVastStartpunt.get("toelagesom"));
        assertEquals(LocalDate.of(2000, 10, 1), m.vastStartpuntAnker(jutter, "stormwachtdagen"));
        assertEquals(LocalDate.of(2000, 1, 31), m.vastStartpuntAnker(jutter, "maandwachtdagen"));
        assertNull(m.vastStartpuntAnker(jutter, "toelagesom"));
    }

    /** The pro-rata document ref's and clingo's suites hold (WV-3.2-vast-prorata), with the target's and the base's
     *  fixed start point as parameters. */
    private static JsonNode proRata(String doel, String basis) throws Exception {
        String facet = "{\"node\":\"TimelineFacet\",\"granularity\":\"%s\",\"count\":1%s}";
        String tarief = String.format(facet, "year", basis == null ? "" : ",\"fixedStart\":\"" + basis + "\"");
        String deel = String.format(facet, "month", doel == null ? "" : ",\"fixedStart\":\"" + doel + "\"");
        return JSON.readTree("{\"format\":\"uchar-ir-v1\",\"model\":{\"objectTypes\":[{\"node\":\"ObjectType\","
                + "\"name\":\"Jutter\",\"animate\":true,\"plural\":\"Jutters\",\"attributes\":[{\"node\":\"Attribute\","
                + "\"name\":\"borgtarief\",\"datatype\":{\"node\":\"Number\",\"whole\":true,\"decimals\":0,"
                + "\"sign\":\"unspecified\"},\"timeline\":" + tarief + "},{\"node\":\"Attribute\",\"name\":\"borgdeel\","
                + "\"datatype\":{\"node\":\"Number\",\"whole\":false,\"decimals\":2,\"sign\":\"unspecified\"},"
                + "\"timeline\":" + deel + "}],\"characteristics\":[{\"node\":\"Characteristic\",\"name\":\"borgplichtig\","
                + "\"kind\":\"adjectival\",\"timeline\":{\"node\":\"TimelineFacet\",\"granularity\":\"month\",\"count\":1}}]}],"
                + "\"factTypes\":[],\"parameters\":[],\"domains\":[],\"unitSystems\":[],\"dimensions\":[],\"timelines\":[],"
                + "\"dayKinds\":[]},\"rules\":{\"rules\":[{\"node\":\"Rule\",\"name\":\"borgdeel van een jutter\","
                + "\"versions\":[{\"validity\":{\"node\":\"Validity\"},\"target\":{\"node\":\"AttributeReference\","
                + "\"attribute\":\"borgdeel\",\"subjectType\":\"Jutter\",\"source\":{\"snippet\":\"Het borgdeel van een "
                + "Jutter\"}},\"value\":{\"node\":\"BuiltinCall\",\"kind\":\"timeProportionalPart\",\"arguments\":"
                + "[{\"node\":\"AttributeReference\",\"attribute\":\"borgtarief\",\"subjectType\":\"Jutter\","
                + "\"source\":{\"snippet\":\"zijn borgtarief\"}}],\"filter\":{\"node\":\"CharacteristicCheck\","
                + "\"characteristic\":\"borgplichtig\",\"negated\":false,\"source\":{\"snippet\":\"gedurende de tijd "
                + "dat hij borgplichtig is\"}},\"granularity\":\"month\",\"type\":{\"node\":\"Number\",\"whole\":false,"
                + "\"decimals\":2,\"sign\":\"unspecified\"},\"source\":{\"snippet\":\"het tijdsevenredig deel per maand "
                + "van zijn borgtarief gedurende de tijd dat hij borgplichtig is\"}},\"initialisation\":false,"
                + "\"presentation\":\"calculation\"}]}],\"dayKindDefinitions\":[],\"objectCreations\":[],\"allocations\":[],"
                + "\"consistencies\":[],\"uniquenesses\":[],\"factCreations\":[],\"startPointDeterminations\":[],"
                + "\"ruleGroups\":[]},\"diagnostics\":[]}");
    }

    /** RUNTIME-SEMANTICS ch. 3.2: both branches ask this predicate before lowering a pro-rata part, and skip the rule
     *  with its reason when the target or the base lies on a fixed start point's grid, as ref and clingo refuse it;
     *  on the calendar grid it answers null and the rule lowers as before. */
    @Test void pro_rata_over_een_vast_startpunt_is_omheind() throws Exception {
        for (String[] geval : new String[][]{{null, null}, {"--01-15", null}, {null, "--10-01"}}) {
            RegelSpraakModel m = IrLoader.load(proRata(geval[0], geval[1]));
            var regel = m.resolvedRegels.get(0);
            String reden = Tijdlijnen.proRataVastRaster(m.objecttypen.get("Jutter"), "borgdeel", regel.waarde());
            if (geval[0] == null && geval[1] == null) assertNull(reden, "het kalenderraster is niet omheind");
            else assertTrue(reden != null && reden.contains("RUNTIME-SEMANTICS ch. 3.2"),
                    "omheind bij doel " + geval[0] + " en basis " + geval[1] + ": " + reden);
        }
    }

    private static void weigert(String getuige, String plek, String zin) throws Exception {
        JsonNode ir = lees("negatief/" + getuige);
        IrLoader.Documentfout e = assertThrows(IrLoader.Documentfout.class, () -> IrLoader.loadAll(ir), getuige);
        assertEquals(plek, e.plek(), e.getMessage());
        assertTrue(e.getMessage().endsWith(": " + zin), e.getMessage());
    }

    @Test void de_drie_getuigen_worden_in_de_zin_van_de_verifier_geweigerd() throws Exception {
        weigert("wf115-vast-startpunt-bij-variabel", "/model/timelines/0/fixedStart",
                "timeline 'het stormjaar' has a variable start and a fixedStart '--10-01': its periods run from the "
                        + "anchor its start-point determination binds per object, and a timeline has one start");
        weigert("wf115-vast-startpunt-buiten-raster", "/model/objectTypes/0/attributes/1/timeline/fixedStart",
                "fixed start point '2024-01-31' does not fit a month grid of count 1: a month, quarter or year grid "
                        + "of one unit starts on a day-month, written --mm-dd, a day of month mm in a leap year");
        weigert("wf115-vast-startpunt-niet-overgenomen", "/model/objectTypes/0/attributes/0/timeline/timelineName",
                "named-timeline adoption is partial: fixed start point none on the facet, '--10-01' on timeline "
                        + "'stormjaar'");
    }
}
