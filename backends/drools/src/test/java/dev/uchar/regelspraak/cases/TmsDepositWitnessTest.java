package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static dev.uchar.regelspraak.cases.TmsDocuments.attribute;
import static dev.uchar.regelspraak.cases.TmsDocuments.binary;
import static dev.uchar.regelspraak.cases.TmsDocuments.consistent;
import static dev.uchar.regelspraak.cases.TmsDocuments.numberType;
import static dev.uchar.regelspraak.cases.TmsDocuments.numberValue;
import static dev.uchar.regelspraak.cases.TmsDocuments.inconsistent;
import static dev.uchar.regelspraak.cases.TmsDocuments.instanceWith;
import static dev.uchar.regelspraak.cases.TmsDocuments.characteristicRule;
import static dev.uchar.regelspraak.cases.TmsDocuments.ownRead;
import static dev.uchar.regelspraak.cases.TmsDocuments.literal;
import static dev.uchar.regelspraak.cases.TmsDocuments.predicate;
import static dev.uchar.regelspraak.cases.TmsDocuments.computingRule;
import static dev.uchar.regelspraak.cases.TmsDocuments.compound;
import static dev.uchar.regelspraak.cases.TmsDocuments.expected;
import static dev.uchar.regelspraak.cases.TmsDocuments.comparison;
import static dev.uchar.regelspraak.cases.TmsDocuments.value;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The §6.5 deposit on the exec-model branch wherever the reference interpreter makes it (RUNTIME-SEMANTICS ch. 1.2,
 * 5.6): every evaluated division of a filled dividend by a leeg divisor, whatever its operands, in a condition as well
 * as in a value, and in a condition whether it then holds or not, but only at the places the evaluation reaches
 * (ch. 5.10). Fixround 1 of the card, findings 1 and 2: until then a condition deposited nothing, and a value deposited
 * only where both operands were reads of an attribute or of a binder.
 *
 * <p>The document: a Jutter with {@code a} and {@code grond b} as input and "De b van een Jutter moet berekend worden
 * als zijn grond b", read by "Een Jutter is zwaar indien zijn a gedeeld door zijn b groter is dan 1", "Een Jutter is
 * zeer zwaar indien hij aan alle volgende voorwaarden voldoet: zijn a is groter dan 10; zijn a gedeeld door zijn b is
 * groter dan 1", "De heffing van een Jutter moet berekend worden als 5 gedeeld door zijn b", "De toeslag van een
 * Jutter moet geïnitialiseerd worden op 5 gedeeld door zijn b" and the consistency "verhoudingscontrole", "zijn a
 * gedeeld door zijn b is leeg of gevuld", which always holds, in every order of the rules. Case 1: J1 (a 5) and J3
 * (a 50), neither with a grond b: b stays leeg, so every division deposits where it is evaluated, except the one of
 * zeer zwaar on J1, whose composite settles on "a groter dan 10" before it (ch. 5.10). Case 2: J2 (a 5, grond b 10):
 * the deposits made while b is still leeg, in an order where a reader fires before the b is derived, go again.
 *
 * <p><b>Measured</b> (2026-10-09, on every order): the reference interpreter's runner passes case 1 in all 120 orders,
 * every deposit above included and none for zeer zwaar on J1; it passes case 2 where the b rule comes first, and in 80
 * of the 96 other orders keeps a deposit of the heffing or the toeslag made while b was leeg, the deposit of an
 * intermediate state that the owner's decision of 2026-10-09 (PLAN §9) does not count. The ASP runner passes case 2 in
 * every order, and carries every deposit of case 1 but the initialisation's, "expected inconsistent (… "toeslag van
 * een jutter" on "J1"), but the model carries: verhoudingscontrole × J1; …", where the reference interpreter deposits.
 *
 * <p><b>Red</b> on a3842004: "case: verwacht inconsistent (zwaar bij verhouding @ J1), werkelijk consistent; verwacht
 * inconsistent (heffing van een jutter @ J1), werkelijk consistent; verwacht inconsistent (toeslag van een jutter @ J1),
 * werkelijk consistent; verwacht inconsistent (verhoudingscontrole @ J1), werkelijk consistent; …".
 */
class TmsDepositWitnessTest {

    private static final String TWO = numberType(false, 2, null), WHOLE = numberType(true, 0, null);

    static final String MODEL = TmsDocuments.model(List.of(TmsDocuments.objectType("Jutter", List.of(
            attribute("a", TWO), attribute("grond b", TWO), attribute("b", TWO), attribute("heffing", TWO),
            attribute("toeslag", TWO)), List.of("zwaar", "zeer zwaar"))), List.of());

    private static final String A = ownRead("a", "Jutter"), B = ownRead("b", "Jutter");

    private static String division(String counter) {
        return binary("dividedBy", counter, B, TWO);
    }

    static final List<String> RULES = List.of(
            computingRule("b van een jutter", "BEREKENING", "Jutter", "b", ownRead("grond b", "Jutter"), "null"),
            characteristicRule("zwaar bij verhouding", "Jutter", "zwaar",
                    comparison("greater", division(A), literal("1", WHOLE))),
            characteristicRule("zeer zwaar bij verhouding", "Jutter", "zeer zwaar", compound("all", 0, List.of(
                    comparison("greater", A, literal("10", WHOLE)),
                    comparison("greater", division(A), literal("1", WHOLE))))),
            computingRule("heffing van een jutter", "BEREKENING", "Jutter", "heffing", division(literal("5", WHOLE)), "null"),
            computingRule("toeslag van een jutter", "INITIALISATIE", "Jutter", "toeslag", division(literal("5", WHOLE)),
                    "null"));

    static final String CHECK = TmsDocuments.consistency("verhoudingscontrole", "Jutter", compound("atLeast", 1,
            List.of(predicate("empty", division(A)), predicate("filled", division(A)))));

    static JsonNode case1() throws Exception {
        return TmsDocuments.caseOn(null, List.of(
                instanceWith("J1", "Jutter", Map.of("a", numberValue("5.00")), List.of()),
                instanceWith("J3", "Jutter", Map.of("a", numberValue("50.00")), List.of())),
                List.of(expected("J1", "zwaar", false), expected("J1", "zeer zwaar", false), value("J1", "heffing", null),
                        value("J1", "toeslag", null), expected("J3", "zwaar", false), expected("J3", "zeer zwaar", false),
                        inconsistent("zwaar bij verhouding", "J1"), inconsistent("heffing van een jutter", "J1"),
                        inconsistent("toeslag van een jutter", "J1"), inconsistent("verhoudingscontrole", "J1"),
                        inconsistent("zwaar bij verhouding", "J3"), inconsistent("zeer zwaar bij verhouding", "J3"),
                        inconsistent("heffing van een jutter", "J3"), inconsistent("toeslag van een jutter", "J3"),
                        inconsistent("verhoudingscontrole", "J3")));
    }

    static JsonNode case2() throws Exception {
        return TmsDocuments.caseOn(null, List.of(
                instanceWith("J2", "Jutter", Map.of("a", numberValue("5.00"), "grond b", numberValue("10.00")), List.of())),
                List.of(expected("J2", "zwaar", false), value("J2", "heffing", "0.50"), value("J2", "toeslag", "0.50"),
                        consistent()));
    }

    @Test void every_reached_division_by_empty_also_deposits_in_a_condition() throws Exception {
        List<String> outcomes = new ArrayList<>(), expected = new ArrayList<>();
        for (List<String> order : TmsDocuments.permutations(RULES)) {
            List<String> names = TmsDocuments.ruleNames(order);
            Path ir = TmsDocuments.document(MODEL, order, List.of(CHECK));
            for (JsonNode caseDocument : List.of(case1(), case2())) {
                outcomes.add(names + " " + TmsFaultWitnessTest.play(ir, caseDocument, DroolsBackend.Branch.EXEC_MODEL));
                expected.add(names + " gespeeld");
            }
        }
        assertEquals(expected, outcomes);
    }
}
