package dev.uchar.regelspraak.loader;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import dev.uchar.regelspraak.model.RegelSpraakModel;

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
