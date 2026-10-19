package dev.uchar.regelspraak.loader;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import dev.uchar.regelspraak.model.RegelSpraakModel;
import dev.uchar.regelspraak.transpiler.core.Timelines;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The fixed start point on the wire (IR-SPEC ch. 2.8): the loader reads it one to one, gives a target on it the constant
 * anchor its walls are computed from, and refuses the three WF-115 witnesses in the verifier's sentence at the
 * verifier's pointer. Measured before this loader read the field: every one of the four documents was refused as an
 * unknown field on its Timeline or TimelineFacet.
 */
class FixedStartPointWitnessTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    /** cwd under surefire is the module dir (backends/drools); the corpus is two levels up. */
    private static final Path CORPUS = Path.of("..", "..", "corpus");

    private static JsonNode read(String path) throws Exception {
        Path ir = CORPUS.resolve(path).resolve("ir.json");
        assertTrue(Files.exists(ir), "ir.json aanwezig: " + ir.toAbsolutePath());
        return JSON.readTree(Files.readString(ir));
    }

    @Test void the_golden_reads_one_to_one_and_gives_the_anchor() throws Exception {
        RegelSpraakModel m = IrLoader.load(read("golden/jutterswet-vast-startpunt"));
        assertEquals("--10-01", m.timelines.get(0).fixedStartPoint());
        RegelSpraakModel.ObjectType jutter = m.objectTypes.get("Jutter");
        assertEquals("--10-01", jutter.attributeFixedStartPoint.get("stormwachtdagen"));
        assertEquals("--01-31", jutter.attributeFixedStartPoint.get("maandwachtdagen"));
        assertEquals("2024-09-30", jutter.characteristicFixedStartPoint.get("wachtplichtig"));
        assertNull(jutter.attributeFixedStartPoint.get("toelagesom"));
        assertEquals(LocalDate.of(2000, 10, 1), m.fixedStartPointAnchor(jutter, "stormwachtdagen"));
        assertEquals(LocalDate.of(2000, 1, 31), m.fixedStartPointAnchor(jutter, "maandwachtdagen"));
        assertNull(m.fixedStartPointAnchor(jutter, "toelagesom"));
    }

    /** The pro-rata document ref's and clingo's suites hold (WV-3.2-vast-prorata), with the target's and the base's
     *  fixed start point as parameters. */
    private static JsonNode proRata(String target, String base) throws Exception {
        String facet = "{\"node\":\"TimelineFacet\",\"granularity\":\"%s\",\"count\":1%s}";
        String rate = String.format(facet, "year", base == null ? "" : ",\"fixedStart\":\"" + base + "\"");
        String part = String.format(facet, "month", target == null ? "" : ",\"fixedStart\":\"" + target + "\"");
        return JSON.readTree("{\"format\":\"uchar-ir-v1\",\"model\":{\"objectTypes\":[{\"node\":\"ObjectType\","
                + "\"name\":\"Jutter\",\"animate\":true,\"plural\":\"Jutters\",\"attributes\":[{\"node\":\"Attribute\","
                + "\"name\":\"borgtarief\",\"datatype\":{\"node\":\"Number\",\"whole\":true,\"decimals\":0,"
                + "\"sign\":\"unspecified\"},\"timeline\":" + rate + "},{\"node\":\"Attribute\",\"name\":\"borgdeel\","
                + "\"datatype\":{\"node\":\"Number\",\"whole\":false,\"decimals\":2,\"sign\":\"unspecified\"},"
                + "\"timeline\":" + part + "}],\"characteristics\":[{\"node\":\"Characteristic\",\"name\":\"borgplichtig\","
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
     *  with its reason when the target or the base lies on a fixed start point's grid, as ref and clingo refuse it,
     *  in their sentence word for word (it was a Dutch sentence of this backend's own until the fix round of
     *  2026-09-26); on the calendar grid it answers null and the rule lowers as before. */
    @Test void pro_rata_over_a_fixed_start_point_is_fenced() throws Exception {
        for (String[] fixedStarts : new String[][]{{null, null}, {"--01-15", null}, {null, "--10-01"}}) {
            RegelSpraakModel m = IrLoader.load(proRata(fixedStarts[0], fixedStarts[1]));
            var rule = m.resolvedRules.get(0);
            String reason = Timelines.proRataFixedGrid(m.objectTypes.get("Jutter"), "borgdeel", rule.value());
            if (fixedStarts[0] == null && fixedStarts[1] == null) assertNull(reason, "het kalenderraster is niet omheind");
            else assertEquals("pro-rata onto or from a grid that does not lie on the calendar is fenced"
                    + " (RUNTIME-SEMANTICS ch. 3.2)", reason, "omheind bij doel " + fixedStarts[0] + " en basis " + fixedStarts[1]);
        }
    }

    private static void refuses(String witness, String site, String sentence) throws Exception {
        JsonNode ir = read("negative/" + witness);
        IrLoader.DocumentError e = assertThrows(IrLoader.DocumentError.class, () -> IrLoader.loadAll(ir), witness);
        assertEquals(site, e.site(), e.getMessage());
        assertTrue(e.getMessage().endsWith(": " + sentence), e.getMessage());
    }

    @Test void the_three_witnesses_are_refused_in_the_sentence_of_the_verifier() throws Exception {
        refuses("wf115-fixed-start-point-at-variable", "/model/timelines/0/fixedStart",
                "timeline 'het stormjaar' has a variable start and a fixedStart '--10-01': its periods run from the "
                        + "anchor its start-point determination binds per object, and a timeline has one start");
        refuses("wf115-fixed-start-point-outside-grid", "/model/objectTypes/0/attributes/1/timeline/fixedStart",
                "fixed start point '2024-01-31' does not fit a month grid of count 1: a month, quarter or year grid "
                        + "of one unit starts on a day-month, written --mm-dd, a day of month mm in a leap year");
        refuses("wf115-fixed-start-point-negated-adopted", "/model/objectTypes/0/attributes/0/timeline/timelineName",
                "named-timeline adoption is partial: fixed start point none on the facet, '--10-01' on timeline "
                        + "'stormjaar'");
    }
}
