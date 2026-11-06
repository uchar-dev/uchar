package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static dev.uchar.regelspraak.cases.TmsDocuments.WHOLE;
import static dev.uchar.regelspraak.cases.TmsDocuments.attribute;
import static dev.uchar.regelspraak.cases.TmsDocuments.computingRule;
import static dev.uchar.regelspraak.cases.TmsDocuments.consistent;
import static dev.uchar.regelspraak.cases.TmsDocuments.instance;
import static dev.uchar.regelspraak.cases.TmsDocuments.number;
import static dev.uchar.regelspraak.cases.TmsDocuments.ownRead;
import static dev.uchar.regelspraak.cases.TmsDocuments.value;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Exact arithmetic everywhere a value of this branch can be read (RUNTIME-SEMANTICS ch. 2.1, 2.2, 2.4, 2.5), through
 * both branches, on numbers the ASP backend's integer range keeps out of the differential corpus.
 *
 * <ul>
 *   <li>plus, min and times are field operations on the exact rationals, also past the thirty-four digits of a
 *       decimal128 context: (10^31 + 0,001) − 10^31 is 0,001, and 100000000000000001 squared has all its 35 digits.</li>
 *   <li>a square root under its rounding is the true root rounded once (§13.4.17.13): √(1/9) to 20 decimals naar
 *       beneden is 0,33333333333333333333, and √2 to 40 decimals naar beneden and naar boven differ in the last digit
 *       only.</li>
 *   <li>a power with a fractional exponent is the exact root where it is rational (8 tot de macht 2/3 is 4, 1/3 tot de
 *       macht −1 is 3), and under its rounding the true power rounded once (10 tot de macht 1/3 to 10 decimals). An
 *       exponent whose numerator or denominator passes the bound of this branch is refused by name.</li>
 *   <li>a value of the unbounded form that initialises an attribute of a new object is that object's exact value for a
 *       reader: a third, times three, is one.</li>
 *   <li>the bound of a comparison under the whole-period qualifier is compared exactly: a segment of
 *       0,3333333333333333 is not at least a third.</li>
 *   <li>the reading of a node whose value does not terminate writes it as {@code t/n}.</li>
 * </ul>
 */
class ExactArithmeticWitnessTest {

    private static final String UNBOUNDED = "{\"node\":\"Number\",\"whole\":false,\"decimals\":0,\"sign\":\"unspecified\"}";

    private static String decimals(int d) {
        return "{\"node\":\"Number\",\"whole\":false,\"decimals\":" + d + ",\"sign\":\"unspecified\"}";
    }

    @Test void plus_min_and_times_are_exact_past_thirty_four_digits() throws Exception {
        String model = TmsDocuments.model(List.of(TmsDocuments.objectType("Jutter", List.of(attribute("a", UNBOUNDED),
                attribute("b", decimals(3)), attribute("y", decimals(3)), attribute("x", UNBOUNDED),
                attribute("p", UNBOUNDED)), List.of())), List.of());
        Path ir = TmsDocuments.document(model, List.of(
                computingRule("y van een jutter", "BEREKENING", "Jutter", "y", binary("minus",
                        binary("plus", ownRead("a", "Jutter"), ownRead("b", "Jutter"), UNBOUNDED),
                        ownRead("a", "Jutter"), UNBOUNDED), "null"),
                computingRule("p van een jutter", "BEREKENING", "Jutter", "p",
                        binary("times", ownRead("x", "Jutter"), ownRead("x", "Jutter"), UNBOUNDED), "null")));
        JsonNode c = TmsDocuments.caseDocument(List.of(instance("J1", "Jutter", Map.of("a",
                        "10000000000000000000000000000000", "b", "0.001", "x", "100000000000000001"), List.of(), Map.of())),
                List.of(value("J1", "y", "0.001"), value("J1", "p", "10000000000000000200000000000000001"), consistent()));
        assertEquals("gespeeld", TmsFaultWitnessTest.playBoth(ir, c));
    }

    @Test void a_square_root_under_its_rounding_is_the_true_root_rounded_once() throws Exception {
        String model = TmsDocuments.model(List.of(TmsDocuments.objectType("Wrakkist", List.of(attribute("n", WHOLE),
                attribute("d", WHOLE), attribute("r", decimals(20)), attribute("s", decimals(40)),
                attribute("t", decimals(40)), attribute("h", decimals(2))), List.of())), List.of());
        String third = binary("dividedBy", ownRead("n", "Wrakkist"), ownRead("d", "Wrakkist"), UNBOUNDED);
        Path ir = TmsDocuments.document(model, List.of(
                computingRule("r van een wrakkist", "BEREKENING", "Wrakkist", "r",
                        rounding(squareRoot(third), "floor", 20), "null"),
                computingRule("s van een wrakkist", "BEREKENING", "Wrakkist", "s",
                        rounding(squareRoot(number("2")), "floor", 40), "null"),
                computingRule("t van een wrakkist", "BEREKENING", "Wrakkist", "t",
                        rounding(squareRoot(number("2")), "ceiling", 40), "null"),
                computingRule("h van een wrakkist", "BEREKENING", "Wrakkist", "h",
                        rounding(squareRoot(number("2")), "halfAwayFromZero", 2), "null")));
        JsonNode c = TmsDocuments.caseDocument(List.of(instance("K1", "Wrakkist", Map.of("n", "1", "d", "9"), List.of(),
                        Map.of())),
                List.of(value("K1", "r", "0.33333333333333333333"),
                        value("K1", "s", "1.4142135623730950488016887242096980785696"),
                        value("K1", "t", "1.4142135623730950488016887242096980785697"), value("K1", "h", "1.41"),
                        consistent()));
        assertEquals("gespeeld", TmsFaultWitnessTest.playBoth(ir, c));
    }

    @Test void a_fractional_exponent_is_an_exact_root_or_the_true_power_rounded_once() throws Exception {
        String model = TmsDocuments.model(List.of(TmsDocuments.objectType("Wrakkist", List.of(attribute("n", WHOLE),
                attribute("d", WHOLE), attribute("e", UNBOUNDED), attribute("i", UNBOUNDED),
                attribute("f", decimals(10)), attribute("g", decimals(10)), attribute("w", decimals(20))), List.of())),
                List.of());
        String third = binary("dividedBy", ownRead("n", "Wrakkist"), ownRead("d", "Wrakkist"), UNBOUNDED);
        Path ir = TmsDocuments.document(model, List.of(
                computingRule("e van een wrakkist", "BEREKENING", "Wrakkist", "e",
                        binary("power", number("8"), number("2/3"), UNBOUNDED), "null"),
                computingRule("i van een wrakkist", "BEREKENING", "Wrakkist", "i",
                        binary("power", third, number("-1"), UNBOUNDED), "null"),
                computingRule("f van een wrakkist", "BEREKENING", "Wrakkist", "f",
                        rounding(binary("power", number("10"), number("1/3"), UNBOUNDED), "floor", 10), "null"),
                computingRule("g van een wrakkist", "BEREKENING", "Wrakkist", "g",
                        rounding(binary("power", number("10"), number("1/3"), UNBOUNDED), "ceiling", 10), "null"),
                computingRule("w van een wrakkist", "BEREKENING", "Wrakkist", "w",
                        rounding(binary("power", number("2"), number("0.5"), UNBOUNDED), "towardZero", 20), "null")));
        JsonNode c = TmsDocuments.caseDocument(List.of(instance("K1", "Wrakkist", Map.of("n", "1", "d", "3"), List.of(),
                        Map.of())),
                List.of(value("K1", "e", "4"), value("K1", "i", "3"), value("K1", "f", "2.1544346900"),
                        value("K1", "g", "2.1544346901"), value("K1", "w", "1.41421356237309504880"), consistent()));
        assertEquals("gespeeld", TmsFaultWitnessTest.playBoth(ir, c));
    }

    @Test void an_exponent_past_the_bound_of_this_branch_is_refused_by_name() throws Exception {
        String model = TmsDocuments.model(List.of(TmsDocuments.objectType("Wrakkist", List.of(
                attribute("f", decimals(2))), List.of())), List.of());
        Path ir = TmsDocuments.document(model, List.of(computingRule("f van een wrakkist", "BEREKENING", "Wrakkist", "f",
                rounding(binary("power", number("10"), number("1/1001"), UNBOUNDED), "floor", 2), "null")));
        for (DroolsBackend.Branch branch : TmsDocuments.BRANCHES) {
            CaseRunner.Result r = branch == DroolsBackend.Branch.EXEC_MODEL ? CaseRunner.viaExecModel(ir, List.of())
                    : CaseRunner.viaDrlText(ir, List.of());
            assertEquals("EXCLUDE", r.status(), () -> branch + ": " + r);
            assertEquals("DROOLS-300", r.code() == null ? null : r.code().id(), () -> branch + ": " + r);
        }
    }

    @Test void a_value_of_the_unbounded_form_is_assigned_to_a_new_object_exactly() throws Exception {
        String model = TmsDocuments.model(List.of(
                TmsDocuments.objectType("Jutter", List.of(attribute("controle", WHOLE)),
                        List.of()),
                TmsDocuments.objectType("Aandeel", List.of(attribute("deel", UNBOUNDED), attribute("geheel", WHOLE)),
                        List.of())),
                List.of(TmsDocuments.factType("aandeelname", "jutter", "Jutter", "aandeel", "Aandeel")));
        Path base = TmsDocuments.document(model, List.of(
                computingRule("geheel van een aandeel", "BEREKENING", "Aandeel", "geheel",
                        binary("times", ownRead("deel", "Aandeel"), number("3"), UNBOUNDED), "null"),
                computingRule("controle van een jutter", "BEREKENING", "Jutter", "controle",
                        TmsDocuments.pathRead("geheel", "Aandeel", "aandeelname", "aandeel"), "null")));
        String creation = "{\"node\":\"ObjectCreation\",\"name\":\"maak een aandeel\",\"versions\":[{\"createdType\":"
                + "\"Aandeel\",\"subject\":\"Jutter\",\"assignments\":[{\"node\":\"MemberAssignment\",\"member\":\"deel\","
                + "\"value\":" + IrPath.literal("1/3", UNBOUNDED, "een derde")
                + ",\"characteristic\":false}],\"validity\":{\"node\":\"Validity\"},\"connectingRole\":"
                + TmsDocuments.roleBinding("aandeelname", "aandeel") + "}]}";
        Path ir = Files.writeString(base, Files.readString(base).replace("\"objectCreations\":[]",
                "\"objectCreations\":[" + creation + "]"));
        // 1/3 times 3 is 1 on the new object, where 0.3333333333333333 gives a product that does not fit the whole geheel
        JsonNode c = TmsDocuments.caseDocument(List.of(instance("J1", "Jutter", Map.of(), List.of(), Map.of())),
                List.of(value("J1", "controle", "1"), consistent()));
        assertEquals("gespeeld", TmsFaultWitnessTest.playBoth(ir, c));
    }

    @Test void the_bound_of_a_whole_period_comparison_is_compared_exactly() throws Exception {
        String timeline = "{\"node\":\"TimelineFacet\",\"granularity\":\"month\",\"count\":1}";
        String model = TmsDocuments.model(List.of(TmsDocuments.objectType("Jutter", List.of(attribute("som", WHOLE),
                "{\"node\":\"Attribute\",\"name\":\"getij\",\"datatype\":" + decimals(16) + ",\"timeline\":" + timeline
                        + "}", attribute("toeslag", WHOLE)), List.of())), List.of());
        String third = binary("dividedBy", ownRead("som", "Jutter"), number("3"), UNBOUNDED);
        String comparison = "{\"node\":\"Comparison\",\"op\":\"greaterOrEqual\",\"left\":" + ownRead("getij", "Jutter")
                + ",\"right\":" + third + ",\"wholePeriod\":{\"node\":\"WholePeriod\",\"granularity\":\"month\","
                + "\"negated\":false},\"source\":{\"snippet\":\"zijn getij gedurende de gehele maand ten minste een derde\"}}";
        Path ir = TmsDocuments.document(model, List.of(computingRule("toeslag van een jutter", "BEREKENING", "Jutter",
                "toeslag", number("7"), comparison)));
        String j1 = "{\"label\":\"J1\",\"objectType\":\"Jutter\",\"attributes\":{\"som\":{\"number\":\"1\"},\"getij\":"
                + "{\"timeline\":[{\"from\":\"2025-01-01\",\"to\":\"2025-02-01\",\"value\":{\"number\":"
                + "\"0.3333333333333333\"}}]}},\"roles\":{},\"characteristics\":[]}";
        JsonNode c = TmsDocuments.caseDocument(List.of(j1), List.of(value("J1", "toeslag", null), consistent()));
        assertEquals("gespeeld", TmsFaultWitnessTest.playBoth(ir, c));
    }

    @Test void a_reading_writes_a_value_that_does_not_terminate_as_its_fraction() throws Exception {
        String model = TmsDocuments.model(List.of(TmsDocuments.objectType("Jutter", List.of(attribute("som", WHOLE),
                attribute("deel", UNBOUNDED)), List.of())), List.of());
        Path ir = TmsDocuments.document(model, List.of(computingRule("deel van een jutter", "BEREKENING", "Jutter", "deel",
                binary("dividedBy", ownRead("som", "Jutter"), number("-3"), UNBOUNDED), "null")));
        JsonNode c = TmsDocuments.caseDocument(List.of(instance("J1", "Jutter", Map.of("som", "1"), List.of(), Map.of())),
                List.of(consistent()));
        for (DroolsBackend.Branch branch : TmsDocuments.BRANCHES) {
            List<String> lines = CaseRunner.readings(ir, List.of(caseFile(c)), branch);
            assertTrue(lines.contains("reading /rules/rules/0/versions/0 \"J1\" 0 Binary -1/3"),
                    () -> branch + ": " + lines);
        }
    }

    /** corpus/red/jutterswet-tijdsevenredig-past-niet: the segment of February, 15 × 18/28 = 135/14, does not fit the 2
     *  decimals of the monthly ligdeel (EF-006, RUNTIME-SEMANTICS ch. 2.4, 3.7), its place the attribute, as the
     *  reference interpreter names it; the golden's own case, whose segments fit, plays. */
    @Test void a_pro_rata_segment_passes_the_write_boundary_of_its_target() throws Exception {
        Path red = Path.of("..", "..", "corpus", "red", "jutterswet-tijdsevenredig-past-niet");
        for (DroolsBackend.Branch branch : TmsDocuments.BRANCHES) {
            CaseRunner.RedResult r = CaseRunner.runRed(red.resolve("ir.json"), red.resolve("case1.json"), branch);
            assertEquals("FAULT", r.status(), () -> branch + ": " + r);
            assertEquals("EF-006", r.code(), () -> branch + ": " + r);
            assertEquals(List.of(Map.of("place", "L1.ligdeel", "value", "135/14", "decimals", "2")), r.params(),
                    () -> branch + ": " + r);
        }
    }

    /** A pro-rata part into a timeline of the unbounded form is refused by name: a segment of this branch holds a
     *  decimal, and 135/14 would hold sixteen digits of it. */
    @Test void a_pro_rata_part_into_the_unbounded_form_is_refused_by_name() throws Exception {
        Path red = Path.of("..", "..", "corpus", "red", "jutterswet-tijdsevenredig-past-niet");
        String ir = Files.readString(red.resolve("ir.json"));
        String bounded = "\"name\": \"ligdeel\",\n            \"datatype\": {\n              \"node\": \"Number\",\n"
                + "              \"whole\": false,\n              \"decimals\": 2,";
        assertTrue(ir.contains(bounded), "het ligdeel van het rode geval heeft 2 decimalen");
        Path unbounded = Files.writeString(Files.createTempFile("exact-arithmetic", ".json"),
                ir.replace(bounded, bounded.replace("\"decimals\": 2,", "\"decimals\": 0,")));
        unbounded.toFile().deleteOnExit();
        for (DroolsBackend.Branch branch : TmsDocuments.BRANCHES) {
            CaseRunner.Result r = branch == DroolsBackend.Branch.EXEC_MODEL ? CaseRunner.viaExecModel(unbounded, List.of())
                    : CaseRunner.viaDrlText(unbounded, List.of());
            assertEquals("EXCLUDE", r.status(), () -> branch + ": " + r);
            assertEquals("DROOLS-301", r.code() == null ? null : r.code().id(), () -> branch + ": " + r);
        }
    }

    private static Path caseFile(JsonNode caseDocument) throws Exception {
        Path tmp = Files.createTempFile("exact-arithmetic", ".json");
        tmp.toFile().deleteOnExit();
        return Files.writeString(tmp, caseDocument.toString());
    }

    /** A binary operation in its wire spelling, of the type {@code type}. */
    private static String binary(String op, String left, String right, String type) {
        return "{\"node\":\"Binary\",\"op\":\"" + op + "\",\"left\":" + left + ",\"right\":" + right + ",\"type\":" + type
                + ",\"source\":{\"snippet\":\"" + op + "\"}}";
    }

    private static String rounding(String base, String mode, int d) {
        return "{\"node\":\"Rounding\",\"base\":" + base + ",\"mode\":\"" + mode + "\",\"decimals\":" + d + ",\"type\":"
                + decimals(d) + ",\"source\":{\"snippet\":\"afgerond op " + d + " decimalen\"}}";
    }

    private static String squareRoot(String radicand) {
        return "{\"node\":\"BuiltinCall\",\"kind\":\"squareRoot\",\"arguments\":[" + radicand + "],\"type\":" + UNBOUNDED
                + ",\"source\":{\"snippet\":\"de wortel\"}}";
    }
}
