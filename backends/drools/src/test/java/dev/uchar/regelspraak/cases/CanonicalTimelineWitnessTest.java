package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.uchar.regelspraak.cases.CaseRunner.TimelineSegment;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * An expected timeline compares with the {@code List<Segment>} of its target in the canonical form of RUNTIME-SEMANTICS
 * ch. 3.1: the maximal runs of equal filled values, gaps omitted, open bounds as they stand. The form itself, and the
 * read-back on both branches over the documents of three goldens with the cases that the reference interpreter and the
 * ASP backend replay as well ({@code ref/test/fixtures/canonical-timeline}): each divides a run of equal values otherwise
 * than this backend writes it, and two read otherwise on some day and stay mismatches.
 *
 * <p><b>Red</b> on {@code 457debf65}, the segment-exact read-back: the three cases failed on both branches, the pro-rata
 * part with "verwacht [2024-01-01,2025-01-01)=12 [2025-01-01,2025-02-01)=18 [2025-02-01,2025-03-01)=4.5, werkelijk
 * [2024-01-01,2024-02-01)=12 [2024-02-01,2024-03-01)=12 …", one segment per month.
 */
class CanonicalTimelineWitnessTest {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Path CORPUS = Path.of("..", "..", "corpus", "golden");
    private static final Path FIXTURES = Path.of("..", "..", "ref", "test", "fixtures", "canonical-timeline");

    private static TimelineSegment segment(String from, String to, String value) {
        return new TimelineSegment(from == null ? null : LocalDate.parse(from), to == null ? null : LocalDate.parse(to),
                value == null ? null : new BigDecimal(value));
    }

    @Test void equal_neighbours_merge_a_leeg_segment_is_a_gap_and_an_open_bound_stays_open() {
        // 0 before 2024-02-01 in two segments, 12 in February and 12.00 in March, an explicit leeg April and 12 from
        // May on: three runs, the leeg April a gap between the two runs of 12
        assertEquals(List.of(segment(null, "2024-02-01", "0"), segment("2024-02-01", "2024-04-01", "12"),
                        segment("2024-05-01", null, "12")),
                CaseRunner.canonical(List.of(segment(null, "2024-01-01", "0"), segment("2024-01-01", "2024-02-01", "0"),
                        segment("2024-02-01", "2024-03-01", "12"), segment("2024-03-01", "2024-04-01", "12.00"),
                        segment("2024-04-01", "2024-05-01", null), segment("2024-05-01", null, "12"))));
    }

    @Test void unequal_neighbours_and_values_across_a_gap_stay_apart() {
        List<TimelineSegment> unequal = List.of(segment("2024-01-01", "2024-02-01", "50"),
                segment("2024-02-01", "2024-03-01", "70"));
        assertEquals(unequal, CaseRunner.canonical(unequal));
        List<TimelineSegment> gap = List.of(segment("2024-01-01", "2024-02-01", "40"),
                segment("2024-03-01", "2024-04-01", "40"));
        assertEquals(gap, CaseRunner.canonical(gap));
    }

    @Test void a_degenerate_segment_contributes_nothing() {
        assertEquals(List.of(), CaseRunner.canonical(List.of(segment("2024-05-01", "2024-05-01", "1"),
                segment("2024-05-01", "2024-04-01", "1"))));
    }

    @Test void the_read_back_compares_in_the_canonical_form_on_both_branches() throws Exception {
        List<String> got = new ArrayList<>(), expected = new ArrayList<>();
        for (String[] c : List.of(
                new String[]{"pro-rata", "anchor/jutterswet-borgdeel", null},
                new String[]{"day-count", "anchor/jutterswet-dagentelling", null},
                new String[]{"period-arithmetic", "jutterswet-maandloon", null},
                new String[]{"gap-not-bridged", "jutterswet-maandloon", "case: attribuut vondstopbrengst: verwacht"
                        + " [2024-01-01,2024-04-01)=40, werkelijk [2024-01-01,2024-02-01)=40 [2024-03-01,2024-04-01)=40"},
                new String[]{"value-differs", "jutterswet-maandloon", "case: attribuut vondstopbrengst: verwacht"
                        + " [2024-01-01,2024-02-01)=50 [2024-02-01,2024-03-01)=60 [2024-03-01,2024-04-01)=10, werkelijk"
                        + " [2024-01-01,2024-02-01)=50 [2024-02-01,2024-03-01)=70 [2024-03-01,2024-04-01)=10"}))
            for (DroolsBackend.Branch branch : DroolsBackend.Branch.values()) {
                JsonNode caseNode = JSON.readTree(Files.readString(FIXTURES.resolve(c[0] + ".json")));
                got.add(c[0] + " " + branch + " " + CaseRunner.playCase(CORPUS.resolve(c[1]).resolve("ir.json"), caseNode,
                        branch));
                expected.add(c[0] + " " + branch + " " + c[2]);
            }
        assertEquals(expected, got);
    }
}
