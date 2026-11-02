package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static dev.uchar.regelspraak.cases.TmsDocuments.WHOLE;
import static dev.uchar.regelspraak.cases.TmsDocuments.attribute;
import static dev.uchar.regelspraak.cases.TmsDocuments.computingRule;
import static dev.uchar.regelspraak.cases.TmsDocuments.consistent;
import static dev.uchar.regelspraak.cases.TmsDocuments.expected;
import static dev.uchar.regelspraak.cases.TmsDocuments.instance;
import static dev.uchar.regelspraak.cases.TmsDocuments.number;
import static dev.uchar.regelspraak.cases.TmsDocuments.ownRead;
import static dev.uchar.regelspraak.cases.TmsDocuments.value;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The exact value at rest on the unbounded form (RUNTIME-SEMANTICS ch. 2.1, 2.4 point 3), through both branches: a value
 * written into an attribute of the unbounded form "getal" ({@code whole: false}, {@code decimals: 0}) stays the exact
 * rational in working memory, so a reader and a condition compute with the value the form wrote, and the write-back
 * puts it on the bean as a decimal, exact where it terminates and at sixteen significant digits rounded half to even
 * where it does not.
 *
 * <ul>
 *   <li>"het quotient van een Deling moet berekend worden als zijn teller gedeeld door zijn noemer", 1 divided by 3: the
 *       fact holds the exact 1/3, the bean the decimal 0.3333333333333333, and the case value written as the rational
 *       1/3 is refused by name ("rational value on 'quotient' is not carried by the v0 backend"). The document stays
 *       in the test resources ({@code rational/one-third}).</li>
 *   <li>a third times three is one, for a reader and for a condition: "het deel van een Jutter moet berekend worden als
 *       zijn som gedeeld door 3" into the unbounded form, "het geheel … als zijn deel maal 3" into a whole number, and
 *       "een Jutter is heel indien zijn deel maal 3 gelijk is aan zijn som". On a decimal of sixteen digits the product
 *       is 0.9999999999999999, which does not fit the whole number and is not equal to 1.</li>
 *   <li>factor maal inkomen is precies de grens ({@code rational/factor-at-the-boundary}): "de factor van een Jutter moet
 *       berekend worden als zijn begrensde opbrengst gedeeld door zijn volle opbrengst", 240 over 360, the factor 2/3 of
 *       the unbounded form; "het vrijgelaten inkomen … als zijn strandinkomen maal zijn factor" into 2 decimals; and
 *       "een Jutter is boven de grens indien zijn strandinkomen maal zijn factor groter is dan zijn vrijlatingsgrens".
 *       With strandinkomen 6851,25 and vrijlatingsgrens 4567,50 the product is exactly 4567,50, which fits and is not
 *       greater than the grens; on the decimal 0.6666666666666667 it is 4567,500000000000228, which does not fit and is
 *       greater. The reference interpreter plays the case (RUNTIME-SEMANTICS ch. 2.4, the worked example of the
 *       unbounded form at rest, {@code WV-2.4-p} in {@code ref/test}); the ASP backend refuses the unbounded form where
 *       a value is not whole.</li>
 *   <li>a fold over the value of an attribute of the unbounded form is refused by name (DROOLS-296): the accumulates of
 *       this branch fold decimals, and three times 1/3 would sum to 0.9999999999999999.</li>
 * </ul>
 *
 * <p><b>Red</b> on {@code a0e0fa0ee}, before the card, with the shaded jar of that commit (the outcomes are in the
 * card's Result): the fact held the decimal 0.3333333333333333; the third times three gave the geheel
 * 0.9999999999999999 and no heel; the factor case reported "boven de grens" and the vrijgelaten inkomen
 * 4567.500000000000228; the fold summed to 0.9999999999999999.
 */
class RationalAtRestWitnessTest {

    // cwd under surefire is the module dir (backends/drools)
    private static final Path RESOURCES = Path.of("src", "test", "resources", "rational");

    private static final String UNBOUNDED = "{\"node\":\"Number\",\"whole\":false,\"decimals\":0,\"sign\":\"unspecified\"}";

    @Test void a_third_at_rest_is_exact_in_the_facts_and_a_decimal_on_the_bean() throws Exception {
        Path document = RESOURCES.resolve("one-third");
        Path ir = document.resolve("ir.json");
        JsonNode caseDocument = TmsDocuments.JSON.readTree(Files.readString(document.resolve("case.json")));
        String dump = BranchDumps.equal(ir, caseDocument, "een derde");
        assertEquals(List.of("fact Value(subject=\"D1\", attribute=\"deling\".\"quotient\", value=Breuk(\"1/3\"),"
                        + " writer=String(\"quotient van een deling\"))"),
                rules(dump, "fact ", "1/3", "0.3333"), () -> "de waarde in de feiten:\n" + dump);
        assertEquals(List.of("read value \"D1\".\"quotient\" = BigDecimal(0.3333333333333333)"),
                rules(dump, "read ", "quotient", "quotient"), () -> "lees:\n" + dump);
        assertEquals("Excluded: rational value on 'quotient' is not carried by the v0 backend",
                TmsFaultWitnessTest.play(ir, caseDocument, DroolsBackend.Branch.EXEC_MODEL),
                "het oordeel over de verwachting 1/3");
    }

    @Test void a_third_times_three_is_one_for_a_reader_and_a_condition() throws Exception {
        String model = TmsDocuments.model(List.of(TmsDocuments.objectType("Jutter", List.of(attribute("som", WHOLE),
                attribute("deel", UNBOUNDED), attribute("geheel", WHOLE)), List.of("heel"))), List.of());
        String times3 = binary("times", ownRead("deel", "Jutter"), number("3"), UNBOUNDED);
        Path ir = TmsDocuments.document(model, List.of(
                computingRule("deel van een jutter", "BEREKENING", "Jutter", "deel",
                        binary("dividedBy", ownRead("som", "Jutter"), number("3"), UNBOUNDED), "null"),
                computingRule("geheel van een jutter", "BEREKENING", "Jutter", "geheel", times3, "null"),
                TmsDocuments.characteristicRule("heel", "Jutter", "heel",
                        TmsDocuments.comparison("equal", times3, ownRead("som", "Jutter")))));
        JsonNode c = TmsDocuments.caseDocument(List.of(instance("J1", "Jutter", Map.of("som", "1"), List.of(), Map.of())),
                List.of(value("J1", "geheel", "1"), expected("J1", "heel", true), consistent()));
        assertEquals("gespeeld", TmsFaultWitnessTest.playBoth(ir, c));
    }

    @Test void factor_times_income_is_exactly_the_boundary() throws Exception {
        Path document = RESOURCES.resolve("factor-at-the-boundary");
        Path ir = document.resolve("ir.json");
        JsonNode caseDocument = TmsDocuments.JSON.readTree(Files.readString(document.resolve("case.json")));
        String dump = BranchDumps.equal(ir, caseDocument, "factor maal inkomen");
        assertEquals(List.of("fact Value(subject=\"J1\", attribute=\"jutter\".\"factor\", value=Breuk(\"2/3\"),"
                + " writer=String(\"factor\"))"), rules(dump, "fact Value(", "\"factor\"", "\"factor\""),
                () -> "de factor in de feiten:\n" + dump);
        assertEquals(List.of("read value \"J1\".\"factor\" = BigDecimal(0.6666666666666667)"),
                rules(dump, "read value", "\"factor\"", "\"factor\""), () -> "de factor op de bean:\n" + dump);
        assertEquals("gespeeld", TmsFaultWitnessTest.playBoth(ir, caseDocument));
    }

    @Test void a_fold_over_a_value_of_the_unbounded_form_is_refused_by_name() throws Exception {
        String model = TmsDocuments.model(List.of(
                TmsDocuments.objectType("Ploeg", List.of(attribute("ploegsom", WHOLE)), List.of()),
                TmsDocuments.objectType("Jutter", List.of(attribute("som", WHOLE), attribute("deel", UNBOUNDED)),
                        List.of())),
                List.of(TmsDocuments.factTypeOneToMany("ploegdeelname", "ploeg", "Ploeg", "lid", "Jutter", "leden")));
        Path ir = TmsDocuments.document(model, List.of(
                computingRule("deel van een jutter", "BEREKENING", "Jutter", "deel",
                        binary("dividedBy", ownRead("som", "Jutter"), number("3"), UNBOUNDED), "null"),
                computingRule("ploegsom van een ploeg", "BEREKENING", "Ploeg", "ploegsom",
                        TmsDocuments.aggregation("sum", ownRead("deel", "Jutter"), "Jutter",
                                TmsDocuments.sourceRole("ploegdeelname", "lid"), "null", "null", UNBOUNDED,
                                "de som van de delen van zijn leden"), "null")));
        JsonNode c = TmsDocuments.caseDocument(List.of(
                TmsDocuments.instanceWithRoles("P1", "Ploeg", Map.of(), List.of(), Map.of("leden", List.of("J1", "J2", "J3"))),
                instance("J1", "Jutter", Map.of("som", "1"), List.of(), Map.of()),
                instance("J2", "Jutter", Map.of("som", "1"), List.of(), Map.of()),
                instance("J3", "Jutter", Map.of("som", "1"), List.of(), Map.of())),
                List.of(value("P1", "ploegsom", "1"), consistent()));
        for (DroolsBackend.Branch branch : TmsDocuments.BRANCHES) {
            String verdict = TmsFaultWitnessTest.play(ir, c, branch);
            assertTrue(verdict.startsWith("Excluded: ") && verdict.contains("'deel'"), branch + ": " + verdict);
        }
        CaseRunner.Result r = CaseRunner.viaExecModel(ir, List.of());
        assertEquals("DROOLS-296", r.code().id(), r.reason());
    }

    /** A binary operation in its wire spelling, of the type {@code type}. */
    private static String binary(String op, String left, String right, String type) {
        return "{\"node\":\"Binary\",\"op\":\"" + op + "\",\"left\":" + left + ",\"right\":" + right + ",\"type\":" + type
                + ",\"source\":{\"snippet\":\"" + op + "\"}}";
    }

    /** The lines of {@code dump} that start with {@code section} and hold {@code a} or {@code b}, in their order. */
    private static List<String> rules(String dump, String section, String a, String b) {
        return Stream.of(dump.split("\n")).filter(r -> r.startsWith(section) && (r.contains(a) || r.contains(b)))
                .toList();
    }
}
