package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.uchar.regelspraak.refusal.DroolsCode;
import dev.uchar.regelspraak.transpiler.tms.ComputeHelpers;
import dev.uchar.regelspraak.transpiler.tms.ConstructorValue;
import dev.uchar.regelspraak.transpiler.tms.Period;
import dev.uchar.regelspraak.transpiler.tms.PeriodCondition;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The absence markers of RUNTIME-SEMANTICS ch. 9.5 on both branches, on the documents of the reference interpreter
 * ({@code ref/test/fixtures/absence-markers}): the payload of a timeline, which has a reading per day, is refused by
 * name; a payload in another unit is restated into the declared one; and a constructor value compares and reads as
 * ch. 9 says, per day on a timeline as well.
 */
class AbsenceMarkerWitnessTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    // cwd under surefire is the module dir (backends/drools); ref's fixtures are two levels up
    private static final Path FIXTURES = Path.of("..", "..", "ref", "test", "fixtures", "absence-markers");

    /** The payload of a timeline has a reading per day (ch. 9.5), which this backend does not execute: read in a scalar
     *  slot and summed over the members, it is refused by name on both branches, as the reference interpreter refuses
     *  it, on its fixtures. */
    @Test void the_payload_of_a_timeline_is_refused_by_name() throws Exception {
        // the sum without its filter, which WF-127 refuses and this loader reads: the fold's own source is refused, not
        // only the filter's read
        Path unfiltered = Files.createTempDirectory("sum-without-filter").resolve("ir.json");
        com.fasterxml.jackson.databind.node.ObjectNode doc = (com.fasterxml.jackson.databind.node.ObjectNode)
                JSON.readTree(Files.readString(FIXTURES.resolve("sum-of-a-timeline").resolve("ir.json")));
        for (JsonNode r : doc.get("rules").get("rules"))
            if (r.get("name").asText().equals("opslagtotaal"))
                ((com.fasterxml.jackson.databind.node.ObjectNode) r.get("versions").get(0).get("value")).remove("filter");
        Files.writeString(unfiltered, JSON.writeValueAsString(doc));
        for (String fixture : List.of("payload-of-a-timeline", "sum-of-a-timeline", "sum-without-filter")) {
            Path dir = FIXTURES.resolve(fixture.equals("sum-without-filter") ? "sum-of-a-timeline" : fixture);
            Path ir = fixture.equals("sum-without-filter") ? unfiltered : dir.resolve("ir.json");
            JsonNode c = JSON.readTree(Files.readString(dir.resolve("case.json")));
            String exec = null;
            for (boolean viaDrl : new boolean[] { false, true }) {
                CaseRunner.Excluded e = assertThrows(CaseRunner.Excluded.class,
                        () -> CaseRunner.playCase(ir, c, viaDrl), fixture);
                assertEquals(DroolsCode.PAYLOAD_ACCESS_OF_TIMELINE, e.code(), fixture + ": " + e.getMessage());
                assertTrue(e.getMessage().contains("'opslagkosten'"), fixture + ": " + e.getMessage());
                if (exec == null) exec = e.getMessage();
                else assertEquals(exec, e.getMessage(), fixture + ": the DRL-text branch refuses in the same sentence");
            }
        }
    }

    /** A value written into an attribute with markers is a value of the declared payload datatype (ch. 9.1, WF-062): the
     *  lading 2 t, and the literal 2 t, written into a gewicht in kg is restated into kg on both branches, and its
     *  reading is the reference interpreter's trace line, on its fixture. Red before the restatement: both branches
     *  {@code FAIL ... expected waarde(2000), actual waarde(2)}. */
    @Test void a_payload_in_another_unit_is_restated_into_the_declared_one() throws Exception {
        for (String fixture : List.of("payload-in-another-unit", "literal-payload-in-another-unit")) {
            Path dir = FIXTURES.resolve(fixture);
            BothBranches.green(dir.resolve("ir.json"), JSON.readTree(Files.readString(dir.resolve("case.json"))), "case.json");
            String line = "reading /rules/rules/0/versions/0 \"V\" 0 Construct \"waarde\"(2000)";
            for (DroolsBackend.Branch branch : DroolsBackend.Branch.values()) {
                List<String> lines = CaseRunner.readings(dir.resolve("ir.json"), List.of(dir.resolve("case.json")), branch);
                assertTrue(lines.contains(line), fixture + ", " + branch + " reads " + line + ": " + lines);
            }
        }
    }

    /** Two constructor values are one where their tags are one declared name and their payloads one value, numbers by
     *  value (ch. 9.3), and their hash codes agree, since equal logical facts are one fact; a payload of another tag is
     *  leeg (ch. 9.5), and a leeg payload leaves the constructor value leeg (ch. 9.1). */
    @Test void a_constructor_value_compares_and_reads_as_ch_9_says() {
        ConstructorValue a = new ConstructorValue("waarde", new BigDecimal("300")),
                b = new ConstructorValue("Waarde", new BigDecimal("300.00"));
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertNotEquals(a, new ConstructorValue("waarde", new BigDecimal("301")));
        assertNotEquals(new ConstructorValue("onbekend", null), new ConstructorValue("niet van toepassing", null));
        assertEquals(new BigDecimal("300"), ComputeHelpers.payload(a, "waarde"));
        assertNull(ComputeHelpers.payload(new ConstructorValue("schatting", new BigDecimal("300")), "waarde"));
        assertNull(ComputeHelpers.payload(new ConstructorValue("onbekend", null), "waarde"));
        assertNull(ComputeHelpers.payload(null, "waarde"));
        assertNull(ComputeHelpers.construct("waarde", null));
        assertEquals(a, ComputeHelpers.construct("waarde", new BigDecimal("300.0")));
    }

    /** The comparison of a timeline of a sum type with a constructor value per day (ch. 9.3, 9.4): equal where the day
     *  holds that value, unequal where it holds another or none, a gap day reading leeg. */
    @Test void a_timeline_of_constructor_values_is_compared_per_day() {
        ConstructorValue onbekend = new ConstructorValue("onbekend", null);
        LocalDate jan = LocalDate.of(2025, 1, 1), mar = LocalDate.of(2025, 3, 1), jun = LocalDate.of(2025, 6, 1);
        List<Period> timeline = List.of(new Period(jan, mar, onbekend),
                new Period(mar, jun, new ConstructorValue("waarde", new BigDecimal("10"))));
        // a segment of a constructor value is one segment with an equal one, as a fact of working memory is one fact
        Period a = new Period(jan, mar, new ConstructorValue("onbekend", null)), b = new Period(jan, mar,
                new ConstructorValue("Onbekend", null));
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertEquals(List.of(new Period(jan, mar, null)), PeriodCondition.whereEqual(timeline, onbekend, true, true));
        assertEquals(List.of(new Period(Period.OPEN_FROM, jan, null), new Period(mar, Period.OPEN_TO, null)),
                PeriodCondition.whereEqual(onbekend, timeline, false, false));
    }
}
