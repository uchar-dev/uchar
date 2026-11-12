package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A time-dependent characteristic within a period window, the window read per period, and the day count of formulation
 * A of the anchor case (RUNTIME-SEMANTICS ch. 3.8, 3.10, 3.11; spec/proposals/IMPLICIT-TIME.md points 6 and 7), on both
 * branches of the lowering with truth maintenance and in every order of the rules of each document. The documents are
 * the suite fixtures that the reference interpreter and the ASP backend share
 * ({@code ref/test/fixtures/conditions-per-period}, {@code ref/test/fixtures/period-units/sum-of-rate}) and the goldens
 * that carry the forms; their expectations are the fixtures' and the goldens', computed by hand.
 *
 * <ul>
 * <li>{@code kenmerk-venster}: "Een Afdracht is rentedragend van zijn vervaldatum plus 1 dg tot en met zijn
 * betaaldatum", a computed bound, and the day count over the characteristic the rule writes, 0 in every month without a
 * true day: 15, 31, 31, 28, 12 for A1, 19, 31, 4 for A2, and 0 on all time for A3, whose betaaldatum is leeg.
 * <li>{@code nul-en-dagsoort}: the day count "dat er aan alle volgende voorwaarden wordt voldaan: hij is rentedragend,
 * de dag is geen schrikkeldag" in a target in dg/mnd, 18 in February 2028, and beside it without the day-kind test 19.
 * <li>{@code venster-als-lid} and {@code venster-rekenen}: the window as a member of a composite, and as the whole
 * condition of the period arithmetic; golden {@code jutterswet-pachtvenster} the window as the whole condition of a
 * pro-rata part and of a day count, its bound an own attribute or a literal.
 * <li>{@code jutterswet-verzuimrente-30-360-afgerond}: formulation A, the characteristic within a window, two writers of
 * the rentedagen whose union the maandrente reads, the day count behind a variable with its day-kind test and its unit,
 * the rounding of the period arithmetic per segment and the total of a timeline whose bounds rules derive: 141,67 € and
 * 72,22 €.
 * <li>{@code sum-of-rate}: the sum per period of a rate in €/dg over the members of a set, maal 2 and verminderd met an
 * own rate, in a target in €/dg: 0 until 2024-01-01, 20, 30 and 10 €/dg per period and 0 from 2024-03-01 for the
 * first, 9, 14 and 4 for the second, the sum in its members' unit (ch. 3.5).
 * </ul>
 *
 * <p>The window of {@code corpus/red/time-window-timeline-characteristic-open} is open at its end, so the periods of
 * the characteristic have an open bound: the fault EF-023, naming the characteristic. A case that supplies true
 * periods of a characteristic a rule writes faults by name, as in the reference interpreter. Refused by name: a
 * time-dependent characteristic as the target of a rule without a window (DROOLS-102), a second rule on it
 * (DROOLS-103), a day count over two characteristics (DROOLS-114), and a read over a path of a time-dependent
 * characteristic a rule writes (DROOLS-268). The readings of the first two fixtures are line by line the reference
 * interpreter's trace ({@code runner --trace}).
 */
class TmsCharacteristicWindowWitnessTest {

    private static final ObjectMapper JSON = new ObjectMapper();
    /** cwd under surefire is the module dir (backends/drools); the repository is two levels up. */
    private static final Path FIXTURES = Path.of("..", "..", "ref", "test", "fixtures", "conditions-per-period");
    private static final Path GOLDEN = Path.of("..", "..", "corpus", "golden");
    private static final Path RED = Path.of("..", "..", "corpus", "red");
    private static final Path SUM_RATE = Path.of("..", "..", "ref", "test", "fixtures", "period-units", "sum-of-rate");

    @TempDir Path tmp;

    private int n;

    @Test void a_characteristic_within_a_window_and_its_day_count_in_every_order_on_both_branches() throws Exception {
        assertPlayed(FIXTURES.resolve("kenmerk-venster"));
        assertPlayed(GOLDEN.resolve("time-window-timeline-characteristic"));
    }

    @Test void the_day_count_with_a_day_kind_test_and_a_unit_in_every_order_on_both_branches() throws Exception {
        assertPlayed(FIXTURES.resolve("nul-en-dagsoort"));
    }

    @Test void the_window_read_per_period_in_every_order_on_both_branches() throws Exception {
        assertPlayed(FIXTURES.resolve("venster-als-lid"));
        assertPlayed(FIXTURES.resolve("venster-rekenen"));
        assertPlayed(GOLDEN.resolve("jutterswet-pachtvenster"));
    }

    @Test void formulation_a_in_every_order_on_both_branches() throws Exception {
        assertPlayed(GOLDEN.resolve("jutterswet-verzuimrente-30-360-afgerond"));
    }

    @Test void the_sum_per_period_of_a_rate_keeps_its_members_unit_on_both_branches() throws Exception {
        assertPlayed(SUM_RATE);
    }

    @Test void an_open_window_on_a_characteristic_faults_with_ef_023_on_both_branches() throws Exception {
        Path dir = RED.resolve("time-window-timeline-characteristic-open");
        ObjectNode c = (ObjectNode) JSON.readTree(Files.readString(dir.resolve("case1.json")));
        c.remove("expectedFault");
        c.set("expected", JSON.readTree("[ { \"kind\": \"consistent\" } ]"));
        assertEquals("CharacteristicPeriodsFault: fout in de regel \"berging van een vondst\": de perioden waarin het"
                + " tijdsafhankelijke kenmerk \"geborgen\" van \"V1\" geldt hebben een open grens: een kenmerk geldt alleen"
                + " in begrensde perioden (ch. 3.11). Geweigerd bij de evaluatie",
                TmsFaultWitnessTest.playBoth(dir.resolve("ir.json"), c));
    }

    @Test void true_periods_the_case_supplies_for_a_written_characteristic_fault_by_name() throws Exception {
        Path dir = FIXTURES.resolve("kenmerk-venster");
        JsonNode c = JSON.readTree("""
                { "format": "uchar-case-v0", "asOfDate": null,
                  "input": { "instances": [ { "label": "A1", "objectType": "Afdracht",
                      "attributes": { "vervaldatum": { "date": "2025-11-15" }, "betaaldatum": { "date": "2026-03-12" } },
                      "roles": {}, "characteristics": [],
                      "characteristicTimelines": { "rentedragend": [ { "from": "2025-01-01", "to": "2025-02-01" } ] } } ],
                    "parameters": {} },
                  "expected": [ { "kind": "consistent" } ] }""");
        assertEquals("CharacteristicPeriodsFault: fout in de regel \"rentedragend\": de case levert ware perioden van het"
                + " tijdsafhankelijke kenmerk \"rentedragend\" van \"A1\", dat een regel schrijft (ch. 3.12)",
                TmsFaultWitnessTest.playBoth(dir.resolve("ir.json"), c));
    }

    @Test void the_forms_the_branch_does_not_carry_are_refused_by_name_on_both_branches() throws Exception {
        Path window = FIXTURES.resolve("kenmerk-venster");
        JsonNode c = JSON.readTree(Files.readString(window.resolve("case.json")));
        // a time-dependent characteristic as the target of a rule without a window
        ObjectNode without = document(window);
        ((ObjectNode) version(without, 0)).remove("condition");
        TmsConditionPerPeriodWitnessTest.refused(write(without), c, "regel: rentedragend — het tijdsafhankelijke"
                + " kenmerk 'rentedragend' van Afdracht als doel van een regel zonder periodevenster", "DROOLS-102");
        // a second rule on the characteristic
        ObjectNode twice = document(window);
        ArrayNode rules = (ArrayNode) twice.at("/rules/rules");
        ObjectNode second = rules.get(0).deepCopy();
        second.put("name", "rentedragend bis");
        rules.add(second);
        TmsConditionPerPeriodWitnessTest.refused(write(twice), c, "regel: rentedragend — een tijdsafhankelijk doel met"
                + " een tweede schrijver ('rentedragend' van Afdracht wordt ook geschreven door \"rentedragend bis\")",
                "DROOLS-103");
        // a day count over two characteristics
        Path days = FIXTURES.resolve("nul-en-dagsoort");
        ObjectNode two = document(days);
        ArrayNode parts = (ArrayNode) version(two, 0).at("/value/filter/parts");
        parts.set(1, parts.get(0).deepCopy());
        TmsConditionPerPeriodWitnessTest.refused(write(two), JSON.readTree(Files.readString(days.resolve("case.json"))),
                "regel: rentedagen — een dagentelling", "DROOLS-114");
        // a read over a path of a time-dependent characteristic a rule writes
        String bedrijf = TmsDocuments.characteristicRule("bedrijf", "Werf", "bedrijvig", TmsDocuments.window(
                IrPath.literal("2024-01-01", TmsDocuments.DATE, "1 januari 2024"),
                IrPath.literal("2025-01-01", TmsDocuments.DATE, "1 januari 2025"), false));
        Path path = TmsTimelineDocuments.document(TmsWholePeriodWitnessTest.MODEL_B,
                List.of(bedrijf, TmsWholePeriodWitnessTest.KADEGELD), List.of());
        TmsConditionPerPeriodWitnessTest.refused(path, TmsTimelineDocuments.caseDocument(null,
                TmsWholePeriodWitnessTest.instancesB(), List.of(TmsDocuments.consistent())),
                "regel: kadegeld — het tijdsafhankelijke kenmerk 'bedrijvig' van Werf, dat een vorm schrijft, gelezen"
                        + " over een pad", "DROOLS-268");
    }

    /** The readings of the characteristic within a window with its computed bounds and of the day count with its
     *  day-kind test, line by line the reference interpreter's trace of the same document and case. */
    @Test void the_readings_of_the_window_and_the_day_kind_test_are_those_of_the_reference_interpreter() {
        Path w = FIXTURES.resolve("kenmerk-venster");
        String r = "reading /rules/rules/";
        TmsConditionPerPeriodWitnessTest.readings(w.resolve("ir.json"), w.resolve("case.json"), List.of(
                "case case.json",
                r + "0/versions/0 \"A1\" 0 TimeWindow [[2025-11-16 2026-03-13 empty]]",
                r + "0/versions/0 \"A1\" 1 Binary 2025-11-16",
                r + "0/versions/0 \"A1\" 2 AttributeReference 2025-11-15",
                r + "0/versions/0 \"A1\" 3 Literal 1 \"dg\"",
                r + "0/versions/0 \"A1\" 4 AttributeReference 2026-03-12",
                r + "0/versions/0 \"A1\" HOLDS",
                r + "0/versions/0 \"A2\" 0 TimeWindow [[2028-02-11 2028-04-05 empty]]",
                r + "0/versions/0 \"A2\" 1 Binary 2028-02-11",
                r + "0/versions/0 \"A2\" 2 AttributeReference 2028-02-10",
                r + "0/versions/0 \"A2\" 3 Literal 1 \"dg\"",
                r + "0/versions/0 \"A2\" 4 AttributeReference 2028-04-04",
                r + "0/versions/0 \"A2\" HOLDS",
                r + "0/versions/0 \"A3\" 0 TimeWindow []",
                r + "0/versions/0 \"A3\" 1 Binary 2025-11-16",
                r + "0/versions/0 \"A3\" 2 AttributeReference 2025-11-15",
                r + "0/versions/0 \"A3\" 3 Literal 1 \"dg\"",
                r + "0/versions/0 \"A3\" 4 AttributeReference empty",
                r + "0/versions/0 \"A3\" HOLDS",
                r + "1/versions/0 \"A1\" 0 BuiltinCall [[- 2025-11-01 0], [2025-11-01 2025-12-01 15],"
                        + " [2025-12-01 2026-01-01 31], [2026-01-01 2026-02-01 31], [2026-02-01 2026-03-01 28],"
                        + " [2026-03-01 2026-04-01 12], [2026-04-01 - 0]]",
                r + "1/versions/0 \"A1\" 1 CharacteristicCheck [[2025-11-16 2026-03-13 empty]]",
                r + "1/versions/0 \"A1\" HOLDS",
                r + "1/versions/0 \"A2\" 0 BuiltinCall [[- 2028-02-01 0], [2028-02-01 2028-03-01 19],"
                        + " [2028-03-01 2028-04-01 31], [2028-04-01 2028-05-01 4], [2028-05-01 - 0]]",
                r + "1/versions/0 \"A2\" 1 CharacteristicCheck [[2028-02-11 2028-04-05 empty]]",
                r + "1/versions/0 \"A2\" HOLDS",
                r + "1/versions/0 \"A3\" 0 BuiltinCall [[- - 0]]",
                r + "1/versions/0 \"A3\" 1 CharacteristicCheck []",
                r + "1/versions/0 \"A3\" HOLDS",
                "READINGS 1"));
        Path d = FIXTURES.resolve("nul-en-dagsoort");
        TmsConditionPerPeriodWitnessTest.readings(d.resolve("ir.json"), d.resolve("case.json"), List.of(
                "case case.json",
                r + "0/versions/0 \"A1\" 0 BuiltinCall [[- 2028-02-01 0], [2028-02-01 2028-03-01 18],"
                        + " [2028-03-01 2028-04-01 31], [2028-04-01 2028-05-01 4], [2028-05-01 - 0]]",
                r + "0/versions/0 \"A1\" 1 Compound [[2028-02-11 2028-02-29 empty], [2028-03-01 2028-04-05 empty]]",
                r + "0/versions/0 \"A1\" 2 CharacteristicCheck [[2028-02-11 2028-04-05 empty]]",
                r + "0/versions/0 \"A1\" 3 Predicate [[2028-02-11 2028-02-29 empty], [2028-03-01 2028-04-05 empty]]",
                r + "0/versions/0 \"A1\" 4 BoundDay unreached",
                r + "0/versions/0 \"A1\" HOLDS",
                r + "1/versions/0 \"A1\" 0 BuiltinCall [[- 2028-02-01 0], [2028-02-01 2028-03-01 19],"
                        + " [2028-03-01 2028-04-01 31], [2028-04-01 2028-05-01 4], [2028-05-01 - 0]]",
                r + "1/versions/0 \"A1\" 1 CharacteristicCheck [[2028-02-11 2028-04-05 empty]]",
                r + "1/versions/0 \"A1\" HOLDS",
                "READINGS 1"));
    }

    // ---------------- every order of the rules ----------------

    /** Every case of the document in {@code dir}, in every order of its rules, plays on both branches. */
    private void assertPlayed(Path dir) throws Exception {
        List<Path> cases;
        try (var s = Files.list(dir)) {
            cases = s.filter(p -> p.getFileName().toString().matches("case.*\\.json")).sorted().toList();
        }
        assertTrue(!cases.isEmpty(), dir + " heeft geen casebestand");
        ObjectNode doc = document(dir);
        int size = doc.at("/rules/rules").size();
        List<String> outcomes = new ArrayList<>(), expected = new ArrayList<>();
        for (List<Integer> order : TmsDocuments.permutations(IntStream.range(0, size).boxed().toList())) {
            ObjectNode permuted = doc.deepCopy();
            ArrayNode rules = (ArrayNode) permuted.at("/rules/rules");
            ArrayNode reordered = JSON.createArrayNode();
            for (int i : order) reordered.add(doc.at("/rules/rules").get(i).deepCopy());
            ((ObjectNode) permuted.get("rules")).set("rules", reordered);
            Path ir = write(permuted);
            for (Path c : cases) {
                JsonNode caseDocument = JSON.readTree(Files.readString(c));
                for (DroolsBackend.Branch b : DroolsBackend.Branch.values()) {
                    String head = dir.getFileName() + " " + order + " " + c.getFileName() + " " + b;
                    outcomes.add(head + " " + TmsFaultWitnessTest.play(ir, caseDocument, b));
                    expected.add(head + " gespeeld");
                }
            }
            assertTrue(rules.size() == size, "de volgorde houdt elke regel");
        }
        assertEquals(expected, outcomes);
    }

    private static ObjectNode document(Path dir) throws Exception {
        return (ObjectNode) JSON.readTree(Files.readString(dir.resolve("ir.json")));
    }

    /** The first version of rule {@code i} of {@code doc}. */
    private static JsonNode version(ObjectNode doc, int i) {
        return doc.at("/rules/rules/" + i + "/versions/0");
    }

    private Path write(ObjectNode doc) throws Exception {
        Path p = tmp.resolve("kenmerk-venster-" + (n++) + ".json");
        Files.writeString(p, doc.toPrettyString());
        return p;
    }
}
