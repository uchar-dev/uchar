package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static dev.uchar.regelspraak.cases.TmsDocuments.PERCENT;
import static dev.uchar.regelspraak.cases.TmsDocuments.rounding;
import static dev.uchar.regelspraak.cases.TmsDocuments.attribute;
import static dev.uchar.regelspraak.cases.TmsDocuments.clamp;
import static dev.uchar.regelspraak.cases.TmsDocuments.binary;
import static dev.uchar.regelspraak.cases.TmsDocuments.consistent;
import static dev.uchar.regelspraak.cases.TmsDocuments.unit;
import static dev.uchar.regelspraak.cases.TmsDocuments.unitSystem;
import static dev.uchar.regelspraak.cases.TmsDocuments.numberType;
import static dev.uchar.regelspraak.cases.TmsDocuments.numberValue;
import static dev.uchar.regelspraak.cases.TmsDocuments.instanceWith;
import static dev.uchar.regelspraak.cases.TmsDocuments.characteristicRule;
import static dev.uchar.regelspraak.cases.TmsDocuments.ownRead;
import static dev.uchar.regelspraak.cases.TmsDocuments.literal;
import static dev.uchar.regelspraak.cases.TmsDocuments.computingRule;
import static dev.uchar.regelspraak.cases.TmsDocuments.comparison;
import static dev.uchar.regelspraak.cases.TmsDocuments.expected;
import static dev.uchar.regelspraak.cases.TmsDocuments.value;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The arithmetic value forms on the exec-model branch, each over an operand another rule derives later: a
 * rounding (RUNTIME-SEMANTICS ch. 2.5), a clamp with both bounds (ch. 2.7), a power with a whole exponent (ch. 2.2), a
 * percentage of a value (ch. 2.2, 2.6), a sum of percentages, a comparison of percentages, a percentage that a rule
 * derives taken of a value, and the restatement of units (ch. 2.6) in an additive operand, at the write boundary, in a
 * comparison and in a clamp bound. The case input puts a value on every derived operand that the derivation replaces,
 * so in an order where the reader fires first it reads that value and must give up what it computed from it.
 *
 * <p>The value document: a Vondst with {@code basis} as input and "De waarde van een Vondst moet berekend worden als
 * zijn basis maal 1,5", read by "Het afgeronde van een Vondst moet berekend worden als zijn waarde naar beneden afgerond
 * op 0 decimalen", "Het begrensde … als zijn waarde begrensd op minimaal 10 en maximaal 20", "Het kwadraat … als zijn
 * waarde tot de macht 2" and "Het aandeel … als 20% van zijn waarde", each in a document of its own with the
 * derivation, in both orders. V1 (basis 12,34, waarde 100 in the input): waarde 18,51, afgerond 18, begrensd 18,51,
 * kwadraat 342,6201, aandeel 3,702. V2 (basis 20): waarde 30, begrensd 20, kwadraat 900, aandeel 6. V3 (basis 4): waarde
 * 6, begrensd 10. V4 (no basis): "maal" counts a leeg operand as 0 (ch. 1.2), waarde 0, begrensd 10.
 *
 * <p>The percentage document: "Het tarief van een Vondst moet berekend worden als zijn grondtarief plus 5%", "De toeslag
 * … als zijn tarief van zijn basis" and "Een Vondst is duur indien zijn tarief groter is dan 20%", in every order. V1
 * (grondtarief 18, basis 12,34, tarief 50 in the input): tarief 23, toeslag 2,8382, duur. V2 (grondtarief 10, basis 20):
 * tarief 15, toeslag 3, not duur.
 *
 * <p>The unit document: a Sleepbaan with {@code grondaanloop} in m, "De aanloop … als zijn grondaanloop maal 2", "De
 * sleeplengte … als 2,5 km plus zijn aanloop" (in m), "De begrensde aanloop … als zijn aanloop begrensd op maximaal 0,2
 * km" (in m) and "Een Sleepbaan is lang indien zijn sleeplengte groter is dan 3 km", in every order. S1 (grondaanloop
 * 150, aanloop 5000 in the input): aanloop 300, sleeplengte 2800, begrensde aanloop 200, not lang. S2 (grondaanloop
 * 400): aanloop 800, sleeplengte 3300, lang. The listed fold over the aanloop is described at its row.
 *
 * <p><b>Measured</b> (2026-10-09, on every order of every document): the reference interpreter's runner passes every
 * case. The ASP runner passes the value forms and the percentage document, and declines the unit documents ("non-identical
 * units under a comparison are not translated", "expression kind not translated" for the listed fold). The exec-model
 * branch refuses a percentage in arithmetic and in a comparison ("regel (waarde buiten scope)"); the DRL-text branch
 * plays the percentage documents.
 *
 * <p><b>Red before the change</b> (measured 2026-10-09 on a copy of the branch at its claim): every row was refused on
 * its form, "een Rounding in een waarde ('afgerond op 0 decimalen')", "een Clamp in een waarde ('begrensd')", "een
 * Binary power ('power')", "een Binary over een percentage ('percentageOf')" and "('plus')", each with "de
 * truth-maintenance-tak draagt deze vorm nog niet", and the units as "een additieve operandgrens over eenheden die
 * herrekend moeten worden (m naar km)" and "een §5.8.3 operand van een gelijste vouw over eenheden die herrekend moeten
 * worden (km naar m)".
 */
class TmsComputeFormWitnessTest {

    private static final String THREE = numberType(false, 3, null), SIX = numberType(false, 6, null),
            TWO = numberType(false, 2, null), FOUR = numberType(false, 4, null), WHOLE = numberType(true, 0, null);

    // ---------------- the value forms ----------------

    private static final String VALUE_MODEL = TmsDocuments.model(List.of(TmsDocuments.objectType("Vondst", List.of(
            attribute("basis", TWO), attribute("waarde", THREE), attribute("afgerond", WHOLE),
            attribute("begrensd", THREE), attribute("kwadraat", SIX), attribute("aandeel", THREE)), List.of())), List.of());

    private static final String VALUE = ownRead("waarde", "Vondst");

    /** "De waarde van een Vondst moet berekend worden als zijn basis maal 1,5". */
    static final String DERIVATION = computingRule("waarde van een vondst", "BEREKENING", "Vondst", "waarde",
            binary("times", ownRead("basis", "Vondst"), literal("1.5", numberType(false, 1, null)), THREE), "null");

    static final String ROUNDING = computingRule("afgerond van een vondst", "BEREKENING", "Vondst", "afgerond",
            rounding(VALUE, "floor", 0, WHOLE), "null");

    static final String CLAMP = computingRule("begrensd van een vondst", "BEREKENING", "Vondst", "begrensd",
            clamp(VALUE, literal("10", WHOLE), literal("20", WHOLE), THREE), "null");

    static final String POWER = computingRule("kwadraat van een vondst", "BEREKENING", "Vondst", "kwadraat",
            binary("power", VALUE, literal("2", WHOLE), SIX), "null");

    static final String PERCENT_OF = computingRule("aandeel van een vondst", "BEREKENING", "Vondst", "aandeel",
            binary("percentageOf", literal("20", numberType(true, 0, PERCENT)), VALUE, THREE), "null");

    static JsonNode valueCase(String target, List<String> values) throws Exception {
        List<String> expectations = new ArrayList<>();
        String[] labels = {"V1", "V2", "V3", "V4"};
        for (int i = 0; i < labels.length; i++) expectations.add(value(labels[i], target, values.get(i)));
        expectations.add(consistent());
        return TmsDocuments.caseOn(null, List.of(
                instanceWith("V1", "Vondst", Map.of("basis", numberValue("12.34"), "waarde", numberValue("100")), List.of()),
                instanceWith("V2", "Vondst", Map.of("basis", numberValue("20.00")), List.of()),
                instanceWith("V3", "Vondst", Map.of("basis", numberValue("4.00")), List.of()),
                instanceWith("V4", "Vondst", Map.of(), List.of())), expectations);
    }

    @Test void a_rounding_over_a_later_derived_value() throws Exception {
        assertEveryOrder(VALUE_MODEL, List.of(DERIVATION, ROUNDING),
                valueCase("afgerond", List.of("18", "30", "6", "0")));
    }

    @Test void a_clamp_over_a_later_derived_value() throws Exception {
        assertEveryOrder(VALUE_MODEL, List.of(DERIVATION, CLAMP),
                valueCase("begrensd", List.of("18.51", "20", "10", "10")));
    }

    @Test void a_power_over_a_later_derived_value() throws Exception {
        assertEveryOrder(VALUE_MODEL, List.of(DERIVATION, POWER),
                valueCase("kwadraat", List.of("342.6201", "900", "36", "0")));
    }

    @Test void a_percentage_of_a_later_derived_value() throws Exception {
        assertEveryOrder(VALUE_MODEL, List.of(DERIVATION, PERCENT_OF),
                valueCase("aandeel", List.of("3.702", "6", "1.2", "0")));
    }

    // ---------------- percentages ----------------

    private static final String PCT = numberType(true, 0, PERCENT);

    private static final String PERCENT_MODEL = TmsDocuments.model(List.of(TmsDocuments.objectType("Vondst", List.of(
            attribute("basis", TWO), attribute("grondtarief", PCT), attribute("tarief", PCT), attribute("toeslag", FOUR)),
            List.of("duur"))), List.of());

    static final List<String> PERCENT_RULES = List.of(
            computingRule("tarief van een vondst", "BEREKENING", "Vondst", "tarief",
                    binary("plus", ownRead("grondtarief", "Vondst"), literal("5", PCT), PCT), "null"),
            computingRule("toeslag van een vondst", "BEREKENING", "Vondst", "toeslag",
                    binary("percentageOf", ownRead("tarief", "Vondst"), ownRead("basis", "Vondst"), FOUR), "null"),
            characteristicRule("duur bij tarief", "Vondst", "duur",
                    comparison("greater", ownRead("tarief", "Vondst"), literal("20", PCT))));

    static JsonNode percentCase() throws Exception {
        return TmsDocuments.caseOn(null, List.of(
                instanceWith("V1", "Vondst", Map.of("grondtarief", numberValue("18"), "basis", numberValue("12.34"),
                        "tarief", numberValue("50")), List.of()),
                instanceWith("V2", "Vondst", Map.of("grondtarief", numberValue("10"), "basis", numberValue("20.00")),
                        List.of())),
                List.of(value("V1", "tarief", "23"), value("V1", "toeslag", "2.8382"), expected("V1", "duur", true),
                        value("V2", "tarief", "15"), value("V2", "toeslag", "3"), expected("V2", "duur", false),
                        consistent()));
    }

    @Test void computing_and_comparing_percentages_over_a_later_derived_rate() throws Exception {
        assertEveryOrder(PERCENT_MODEL, PERCENT_RULES, percentCase());
    }

    // ---------------- units ----------------

    private static final String METER = unit("m", "Afstand", "m", 1, 1), KILOMETER = unit("km", "Afstand", "m", 1000, 1);
    private static final String M = numberType(true, 0, METER);

    static final String UNIT_MODEL = TmsDocuments.modelWith(List.of(TmsDocuments.objectType("Sleepbaan", List.of(
            attribute("grondaanloop", M), attribute("aanloop", M), attribute("sleeplengte", M),
            attribute("begrensde aanloop", M), attribute("ruimste aanloop", M)), List.of("lang"))), List.of(), List.of(),
            List.of(unitSystem("Afstand", List.of(METER, KILOMETER))));

    static final List<String> UNIT_RULES = List.of(
            computingRule("aanloop van een sleepbaan", "BEREKENING", "Sleepbaan", "aanloop",
                    binary("times", ownRead("grondaanloop", "Sleepbaan"), literal("2", WHOLE), M), "null"),
            computingRule("sleeplengte van een sleepbaan", "BEREKENING", "Sleepbaan", "sleeplengte",
                    binary("plus", literal("2.5", numberType(false, 1, KILOMETER)), ownRead("aanloop", "Sleepbaan"),
                            numberType(false, 1, KILOMETER)), "null"),
            computingRule("begrensde aanloop van een sleepbaan", "BEREKENING", "Sleepbaan", "begrensde aanloop",
                    clamp(ownRead("aanloop", "Sleepbaan"), "null", literal("0.2", numberType(false, 1, KILOMETER)), M),
                    "null"),
            characteristicRule("lang bij sleeplengte", "Sleepbaan", "lang",
                    comparison("greater", ownRead("sleeplengte", "Sleepbaan"), literal("3", numberType(true, 0, KILOMETER)))));

    static JsonNode unitCase() throws Exception {
        return TmsDocuments.caseOn(null, List.of(
                instanceWith("S1", "Sleepbaan", Map.of("grondaanloop", numberValue("150"), "aanloop", numberValue("5000")),
                        List.of()),
                instanceWith("S2", "Sleepbaan", Map.of("grondaanloop", numberValue("400")), List.of())),
                List.of(value("S1", "aanloop", "300"), value("S1", "sleeplengte", "2800"),
                        value("S1", "begrensde aanloop", "200"), expected("S1", "lang", false),
                        value("S2", "aanloop", "800"), value("S2", "sleeplengte", "3300"),
                        value("S2", "begrensde aanloop", "200"), expected("S2", "lang", true), consistent()));
    }

    @Test void units_are_recomputed_over_a_later_derived_aanloop() throws Exception {
        assertEveryOrder(UNIT_MODEL, UNIT_RULES, unitCase());
    }

    /** "De ruimste aanloop van een Sleepbaan moet berekend worden als de maximale van zijn aanloop en 0,5 km" (in m):
     *  every operand after the first restated into the first one's unit, the unit of the result (§4.6 Eenheid1,
     *  ch. 2.6). S1: 500 (the aanloop 300 m against 0,5 km; 5000 m on the way, from the input), S2: 800. */
    @Test void a_listed_fold_recomputes_its_operands_over_a_later_derived_aanloop() throws Exception {
        String widest = computingRule("ruimste aanloop van een sleepbaan", "BEREKENING", "Sleepbaan", "ruimste aanloop",
                TmsDocuments.listedFold("maximum", List.of(ownRead("aanloop", "Sleepbaan"),
                        literal("0.5", numberType(false, 1, KILOMETER))), M, "de maximale van zijn aanloop en 0,5 km"),
                "null");
        assertEveryOrder(UNIT_MODEL, List.of(UNIT_RULES.get(0), widest), TmsDocuments.caseOn(null, List.of(
                instanceWith("S1", "Sleepbaan", Map.of("grondaanloop", numberValue("150"), "aanloop", numberValue("5000")),
                        List.of()),
                instanceWith("S2", "Sleepbaan", Map.of("grondaanloop", numberValue("400")), List.of())),
                List.of(value("S1", "ruimste aanloop", "500"), value("S2", "ruimste aanloop", "800"), consistent())));
    }

    /** "De totale lengte van een Sleepbaan moet berekend worden als de som van de lengte van alle baandelen van de
     *  Sleepbaan, of 1 km als die er niet zijn" (in m), beside "De lengte van een Baandeel moet berekend worden als zijn
     *  grondlengte": the fallback is restated into the unit of the folded member (ch. 2.6) and then, with the sum, into
     *  the target's. S1 (G1 300, G2 200 with a lengte 999 in the input): 500; S2 (no baandeel): 1000; S3 (G3 without a
     *  grondlengte, so its lengte stays leeg and the sum has no filled member): 1000. Measured 2026-10-09: the reference
     *  interpreter's runner passes the case in both orders; the ASP runner declines it ("sum fallback other than 0 is
     *  not translated yet"); the exec-model branch and the DRL-text branch write the fallback unrestated, 1 where 1000
     *  is expected. <b>Red</b> (2026-10-09) at the claim of the card: "een §5.8.2 terugvalwaarde over eenheden die
     *  herrekend moeten worden (km naar m): de truth-maintenance-tak draagt deze vorm nog niet". */
    @Test void the_fallback_value_of_a_sum_is_recomputed_beside_later_derived_lengths() throws Exception {
        String model = TmsDocuments.modelWith(List.of(
                TmsDocuments.objectType("Sleepbaan", List.of(attribute("totale lengte", M)), List.of()),
                TmsDocuments.objectType("Baandeel", List.of(attribute("grondlengte", M), attribute("lengte", M)),
                        List.of())),
                List.of(TmsDocuments.factTypeOneToMany("baanindeling", "baan", "Sleepbaan", "baandeel", "Baandeel",
                        "baandelen")),
                List.of(), List.of(unitSystem("Afstand", List.of(METER, KILOMETER))));
        String length = computingRule("lengte van een baandeel", "BEREKENING", "Baandeel", "lengte",
                ownRead("grondlengte", "Baandeel"), "null");
        String total = computingRule("totale lengte van een sleepbaan", "BEREKENING", "Sleepbaan", "totale lengte",
                TmsDocuments.aggregation("sum", ownRead("lengte", "Baandeel"), "Baandeel",
                        TmsDocuments.sourceRole("baanindeling", "baandeel"), "null",
                        literal("1", numberType(true, 0, KILOMETER)), M,
                        "de som van de lengte van alle baandelen van de Sleepbaan of 1 km als die er niet zijn"), "null");
        assertEveryOrder(model, List.of(length, total), TmsDocuments.caseOn(null, List.of(
                TmsDocuments.instanceWithRoles("S1", "Sleepbaan", Map.of(), List.of(),
                        Map.of("baandelen", List.of("G1", "G2"))),
                TmsDocuments.instanceWithRoles("S2", "Sleepbaan", Map.of(), List.of(), Map.of()),
                TmsDocuments.instanceWithRoles("S3", "Sleepbaan", Map.of(), List.of(), Map.of("baandelen", List.of("G3"))),
                TmsDocuments.instanceWithRoles("G1", "Baandeel", Map.of("grondlengte", "300"), List.of(), Map.of()),
                TmsDocuments.instanceWithRoles("G2", "Baandeel", Map.of("grondlengte", "200", "lengte", "999"), List.of(),
                        Map.of()),
                TmsDocuments.instanceWithRoles("G3", "Baandeel", Map.of(), List.of(), Map.of())),
                List.of(value("S1", "totale lengte", "500"), value("S2", "totale lengte", "1000"),
                        value("S3", "totale lengte", "1000"), consistent())));
    }

    // ---------------- helpers ----------------

    /** Every order of {@code rules} in the document of {@code model} plays {@code caseDocument}. */
    static void assertEveryOrder(String model, List<String> rules, JsonNode caseDocument) throws Exception {
        List<String> outcomes = new ArrayList<>(), expected = new ArrayList<>();
        for (List<String> order : TmsDocuments.permutations(rules)) {
            List<String> names = TmsDocuments.ruleNames(order);
            outcomes.add(names + " " + TmsFaultWitnessTest.play(TmsDocuments.document(model, order), caseDocument,
                    DroolsBackend.Branch.EXEC_MODEL));
            expected.add(names + " gespeeld");
        }
        assertEquals(expected, outcomes);
    }
}
