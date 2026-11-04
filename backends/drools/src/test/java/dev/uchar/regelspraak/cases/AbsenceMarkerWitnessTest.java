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
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The absence markers of RUNTIME-SEMANTICS ch. 9.5 on both branches: an attribute with markers is an attribute of a sum
 * type, a marker written is a {@code Construct}, a marker tested an equality with it, and the value is read with a
 * {@code PayloadAccess} under a test of it, scalar, in a fold over members and per period on a timeline. The golden
 * {@code jutterswet-afwezigheidsmarkers} plays every case on both branches, its red twin faults with EF-016 on both, the
 * readings of ch. 5.12 write a constructor value as the reference interpreter's trace does, and the payload of a timeline,
 * which has a reading per day, is refused by name.
 *
 * <p>RED FIRST, measured on main {@code d9c218ce1} with this class beside it: both branches refused the golden and the red
 * twin at load ({@code EXCLUDE DROOLS-010: loader: form not carried by the v0 Drools backend: Datatype:SumType}), so every
 * row below failed.
 */
class AbsenceMarkerWitnessTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    // cwd under surefire is the module dir (backends/drools); the corpus and ref's fixtures are two levels up
    private static final Path GOLDEN = Path.of("..", "..", "corpus", "golden", "jutterswet-afwezigheidsmarkers");
    private static final Path RED = Path.of("..", "..", "corpus", "red", "jutterswet-afwezigheidsmarkers-botsing");
    private static final Path FIXTURES = Path.of("..", "..", "ref", "test", "fixtures", "absence-markers");

    @Test void every_case_of_the_golden_plays_on_both_branches() throws Exception {
        List<Path> cases = cases(GOLDEN);
        assertEquals(10, cases.size(), "the golden's ten case files: " + cases);
        for (Path c : cases)
            BothBranches.green(GOLDEN.resolve("ir.json"), JSON.readTree(Files.readString(c)), c.getFileName().toString());
    }

    /** A marker against a number, and two markers on one day of a timeline, are the fault of two writers (ch. 5.7). */
    @Test void the_red_twin_faults_with_two_writers_on_both_branches() throws Exception {
        List<Path> cases = cases(RED);
        assertEquals(2, cases.size(), "the red twin's two case files: " + cases);
        for (Path c : cases) {
            JsonNode pinned = JSON.readTree(Files.readString(c)).get("expectedFault");
            for (DroolsBackend.Branch branch : DroolsBackend.Branch.values()) {
                CaseRunner.RedResult r = CaseRunner.runRed(RED.resolve("ir.json"), c, branch);
                String row = c.getFileName() + " on " + branch + ": " + r.detail();
                assertEquals("FAULT", r.status(), row);
                assertTrue(r.code() != null && List.of(r.code().split(", ")).contains(pinned.get("code").asText()), row);
                assertTrue(r.params().stream().anyMatch(p -> holds(p, pinned.get("params"))), row + " " + r.params());
            }
        }
    }

    /** The readings of ch. 5.12, lines of the reference interpreter's trace of the golden: a constructor value as its tag
     *  quoted and its payload, a payload access with the payload, a marker tested on a timeline per period, and a marker
     *  written into a timeline target as the timeline it writes. */
    @Test void the_readings_write_a_constructor_value_as_the_trace_does() throws Exception {
        for (DroolsBackend.Branch branch : DroolsBackend.Branch.values()) {
            List<String> one = CaseRunner.readings(GOLDEN.resolve("ir.json"),
                    List.of(GOLDEN.resolve("case1-schatting.json")), branch);
            for (String line : List.of(
                    "reading /rules/rules/0/versions/0 \"V1\" 0 Construct \"waarde\"(300)",
                    "reading /rules/rules/3/versions/0 \"V1\" 1 PayloadAccess 300",
                    "reading /rules/rules/3/versions/0 \"V1\" 2 AttributeReference \"waarde\"(300)",
                    "reading /rules/rules/4/versions/0 \"V1\" 2 Construct \"onbekend\"",
                    "reading /rules/rules/8/versions/0 \"V1\" 3 Construct [[2025-01-01 2025-03-01 \"onbekend\"]]",
                    "reading /rules/rules/10/versions/0 \"V1\" 0 Comparison [[2025-01-01 2025-03-01 \"onbekend\"]]"))
                assertTrue(one.contains(line), branch + " reads " + line + ": " + one);
            List<String> seven = CaseRunner.readings(GOLDEN.resolve("ir.json"),
                    List.of(GOLDEN.resolve("case7-som-van-de-waarden.json")), branch);
            String fold = "reading /rules/rules/7/versions/0 \"J1\" 0 Aggregation 400 contributors [\"V1\" 300 reached;"
                    + " \"V3\" 100 reached]";
            assertTrue(seven.contains(fold), branch + " reads " + fold + ": " + seven);
        }
    }

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

    /** A constructor value of the case is a declared constructor of the attribute's sum type, with a payload exactly
     *  where it is unary, as the reference interpreter's case loader holds it. */
    @Test void a_constructor_the_sum_type_does_not_declare_is_a_defect_of_the_case() throws Exception {
        com.fasterxml.jackson.databind.node.ObjectNode c = (com.fasterxml.jackson.databind.node.ObjectNode)
                JSON.readTree(Files.readString(GOLDEN.resolve("case2-onbekend-verdringt-invoer.json")));
        com.fasterxml.jackson.databind.node.ObjectNode taxatie = (com.fasterxml.jackson.databind.node.ObjectNode)
                c.get("input").get("instances").get(0).get("attributes").get("taxatie");
        taxatie.put("constructor", "onbekent");
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> CaseRunner.playCase(GOLDEN.resolve("ir.json"), c, false));
        assertTrue(e.getMessage().contains("\"onbekent\" is not a declared constructor"), e.getMessage());
        taxatie.put("constructor", "onbekend");
        e = assertThrows(IllegalArgumentException.class, () -> CaseRunner.playCase(GOLDEN.resolve("ir.json"), c, false));
        assertTrue(e.getMessage().contains("nullary constructor \"onbekend\" carries a payload"), e.getMessage());
    }

    private static List<Path> cases(Path dir) throws Exception {
        try (Stream<Path> s = Files.list(dir)) {
            return s.filter(p -> p.getFileName().toString().matches("case.*\\.json")).sorted().toList();
        }
    }

    /** Whether the parameters {@code emitted} hold every pinned parameter of {@code pinned}. */
    private static boolean holds(Map<String, String> emitted, JsonNode pinned) {
        for (var e : pinned.properties())
            if (!e.getValue().asText().equals(emitted.get(e.getKey()))) return false;
        return true;
    }
}
