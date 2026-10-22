package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;

import dev.uchar.regelspraak.transpiler.tms.ConditionReading;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import static dev.uchar.regelspraak.cases.TmsDocuments.COUNTING;
import static dev.uchar.regelspraak.cases.TmsDocuments.WHOLE;
import static dev.uchar.regelspraak.cases.TmsDocuments.aggregation;
import static dev.uchar.regelspraak.cases.TmsDocuments.attribute;
import static dev.uchar.regelspraak.cases.TmsDocuments.attributeGroup;
import static dev.uchar.regelspraak.cases.TmsDocuments.binary;
import static dev.uchar.regelspraak.cases.TmsDocuments.builtin;
import static dev.uchar.regelspraak.cases.TmsDocuments.characteristicGroup;
import static dev.uchar.regelspraak.cases.TmsDocuments.characteristicRule;
import static dev.uchar.regelspraak.cases.TmsDocuments.check;
import static dev.uchar.regelspraak.cases.TmsDocuments.comparison;
import static dev.uchar.regelspraak.cases.TmsDocuments.compound;
import static dev.uchar.regelspraak.cases.TmsDocuments.computingRule;
import static dev.uchar.regelspraak.cases.TmsDocuments.dateValue;
import static dev.uchar.regelspraak.cases.TmsDocuments.decision;
import static dev.uchar.regelspraak.cases.TmsDocuments.defaultEntry;
import static dev.uchar.regelspraak.cases.TmsDocuments.factTypeOneToMany;
import static dev.uchar.regelspraak.cases.TmsDocuments.instance;
import static dev.uchar.regelspraak.cases.TmsDocuments.instanceWith;
import static dev.uchar.regelspraak.cases.TmsDocuments.instanceWithRoles;
import static dev.uchar.regelspraak.cases.TmsDocuments.literal;
import static dev.uchar.regelspraak.cases.TmsDocuments.number;
import static dev.uchar.regelspraak.cases.TmsDocuments.numberType;
import static dev.uchar.regelspraak.cases.TmsDocuments.numberValue;
import static dev.uchar.regelspraak.cases.TmsDocuments.objectType;
import static dev.uchar.regelspraak.cases.TmsDocuments.ownRead;
import static dev.uchar.regelspraak.cases.TmsDocuments.pathCheck;
import static dev.uchar.regelspraak.cases.TmsDocuments.sourceRole;
import static dev.uchar.regelspraak.cases.TmsDocuments.unit;
import static dev.uchar.regelspraak.cases.TmsDocuments.unitSystem;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * {@code DroolsBackend.readingsOf}, the readings of the conditions at the fixpoint of a build with readings (card
 * fixpoint-readings, acceptance (b)): per form with a condition and object it runs on, how the condition came out and per
 * node, numbered in the pre-order of the document, whether the evaluation reached it and what it found. Four small
 * documents of Jutterswet material, one form family per rule, each record written by hand from RUNTIME-SEMANTICS
 * ch. 1.2, 2.6, 3.8, 5.10, 7.1 and 8.2, on both branches, which render one analysis and must give the same records.
 *
 * <ul>
 *   <li>Conditions of one Jutter: an all-composite that stops at its second member, so that the third is not reached;
 *       an at-least-one block that stops at the member that holds; a comparison with a leeg operand, which is onwaar; a
 *       product with a leeg operand, which counts as 0; a duration in whole years; and a comparison whose right operand
 *       is restated from km into the m of the left, read in its own unit.</li>
 *   <li>Folds over a role, a Ploeg with its Wachten: a count with a filter, whose filter node is evaluated per member
 *       and has no reading; a quantifier fold over a role with two members, each a contributor with its verdict, in no
 *       order, since its matrix can neither fault nor deposit; and a characteristic check over a role.</li>
 *   <li>Defaults: a chain with its decider in the middle, the default above it failing and the one below it displaced;
 *       and an attribute default whose condition holds and whose consequence is leeg, which gave no value, so that the
 *       default below it decides.</li>
 *   <li>A form outside its validity, and an initialisation, at its turn where the target is empty and displaced where
 *       the input fills it.</li>
 * </ul>
 *
 * <p>A record is one line: the label, the pointer, the status, and per node its number, its kind and what the
 * evaluation found, {@code -} where it did not reach the node; a node evaluated per member of a fold is absent.
 *
 * <p><b>Red</b> on the card's first commit of code, which plans the reading rules and has no entry yet: this class did
 * not compile, since {@code DroolsBackend} had no {@code readingsOf} and the backend no {@code ConditionReading}.
 */
class ReadingsWitnessTest {

    private static final String R = "/rules/rules/";
    private static final String P = "/rules/prioritizedDefaults/";

    // ---------------- conditions of one Jutter ----------------

    private static final String METER = unit("m", "Afstand", "m", 1, 1), KILOMETER = unit("km", "Afstand", "m", 1000, 1);
    private static final String DAY = "{\"node\":\"Date\",\"precision\":\"day\"}";
    private static final String YEARS = numberType(true, 0, unit("jr"));

    private static final String CONDITIONS_MODEL = TmsDocuments.modelWith(List.of(objectType("Jutter",
            List.of(attribute("bonus", WHOLE), attribute("geboortedatum", DAY), attribute("peildatum", DAY),
                    attribute("afstand", numberType(true, 0, METER))),
            List.of("ijverig", "berispt", "erkend", "geschorst", "beloond", "gezien", "rijk", "arm", "volwassen", "ver"))),
            List.of(), List.of(), List.of(unitSystem("Afstand", List.of(METER, KILOMETER))));

    private static final List<String> CONDITIONS = List.of(
            characteristicRule("beloning", "Jutter", "beloond", compound("all", 0, List.of(check("ijverig", false),
                    check("berispt", false), check("erkend", false)))),
            characteristicRule("gezichtsveld", "Jutter", "gezien", compound("atLeast", 1, List.of(check("berispt", false),
                    check("erkend", false), check("geschorst", false)))),
            characteristicRule("rijkdom", "Jutter", "rijk", comparison("greater", ownRead("bonus", "Jutter"), number("5"))),
            characteristicRule("armoede", "Jutter", "arm", comparison("equal",
                    binary("times", ownRead("bonus", "Jutter"), number("2")), number("0"))),
            characteristicRule("volwassenheid", "Jutter", "volwassen", comparison("greaterOrEqual",
                    builtin("duration", List.of(ownRead("geboortedatum", "Jutter"), ownRead("peildatum", "Jutter")), YEARS,
                            "de tijdsduur van zijn geboortedatum tot zijn peildatum in hele jaren"), number("18"))),
            characteristicRule("verte", "Jutter", "ver", comparison("greater", ownRead("afstand", "Jutter"),
                    literal("0.5", numberType(false, 1, KILOMETER)))));

    @Test void the_conditions_of_one_object() throws Exception {
        Path ir = TmsDocuments.document(CONDITIONS_MODEL, CONDITIONS);
        JsonNode c = TmsDocuments.caseOn(null, List.of(
                instanceWith("J1", "Jutter", Map.of("geboortedatum", dateValue("2000-01-01"),
                        "peildatum", dateValue("2025-06-01"), "afstand", numberValue("700")), List.of("ijverig", "erkend")),
                instanceWith("J2", "Jutter", Map.of("bonus", numberValue("3"), "geboortedatum", dateValue("2010-01-01"),
                        "peildatum", dateValue("2025-06-01"), "afstand", numberValue("400")),
                        List.of("ijverig", "berispt", "erkend"))), List.of());
        bothBranches(ir, c, List.of(
                // all: the second member does not hold, so the composite stands at onwaar and the third is not read
                "J1 " + R + "0/versions/0 FAILS | 0 Compound false | 1 CharacteristicCheck true"
                        + " | 2 CharacteristicCheck false | 3 CharacteristicCheck -",
                // at least one: the second member holds, so the third is not read
                "J1 " + R + "1/versions/0 HOLDS | 0 Compound true | 1 CharacteristicCheck false"
                        + " | 2 CharacteristicCheck true | 3 CharacteristicCheck -",
                // a leeg operand: an ordering with it is onwaar (ch. 1.2)
                "J1 " + R + "2/versions/0 FAILS | 0 Comparison false | 1 AttributeReference leeg | 2 Literal 5",
                // a leeg operand of a product counts as 0 (ch. 1.2)
                "J1 " + R + "3/versions/0 HOLDS | 0 Comparison true | 1 Binary 0 | 2 AttributeReference leeg"
                        + " | 3 Literal 2 | 4 Literal 0",
                // 2000-01-01 to 2025-06-01: 25 whole years (ch. 3.8)
                "J1 " + R + "4/versions/0 HOLDS | 0 Comparison true | 1 BuiltinCall 25 jr"
                        + " | 2 AttributeReference 2000-01-01 | 3 AttributeReference 2025-06-01 | 4 Literal 18",
                // 700 m against 0,5 km, read in its own unit and restated into m for the comparison (ch. 2.6)
                "J1 " + R + "5/versions/0 HOLDS | 0 Comparison true | 1 AttributeReference 700 m | 2 Literal 0.5 km",
                "J2 " + R + "0/versions/0 HOLDS | 0 Compound true | 1 CharacteristicCheck true"
                        + " | 2 CharacteristicCheck true | 3 CharacteristicCheck true",
                // the first member holds, so neither of the others is read
                "J2 " + R + "1/versions/0 HOLDS | 0 Compound true | 1 CharacteristicCheck true"
                        + " | 2 CharacteristicCheck - | 3 CharacteristicCheck -",
                "J2 " + R + "2/versions/0 FAILS | 0 Comparison false | 1 AttributeReference 3 | 2 Literal 5",
                "J2 " + R + "3/versions/0 FAILS | 0 Comparison false | 1 Binary 6 | 2 AttributeReference 3"
                        + " | 3 Literal 2 | 4 Literal 0",
                "J2 " + R + "4/versions/0 FAILS | 0 Comparison false | 1 BuiltinCall 15 jr"
                        + " | 2 AttributeReference 2010-01-01 | 3 AttributeReference 2025-06-01 | 4 Literal 18",
                "J2 " + R + "5/versions/0 FAILS | 0 Comparison false | 1 AttributeReference 400 m | 2 Literal 0.5 km"));
    }

    // ---------------- folds over a role ----------------

    private static final String FOLDS_MODEL = TmsDocuments.model(List.of(
            objectType("Ploeg", List.of(), List.of("bemand", "gekwalificeerd")),
            objectType("Wacht", List.of(), List.of("bevoegd", "gecontroleerd"))),
            List.of(factTypeOneToMany("ploegindeling", "ploeg", "Ploeg", "wacht", "Wacht", "wachten")));

    private static final String WACHTEN = "{\"node\":\"AttributeReference\",\"attribute\":\"wachten\",\"subjectType\":"
            + "\"Ploeg\",\"source\":{\"snippet\":\"wachten van de Ploeg\"}}";

    private static final List<String> FOLDS = List.of(
            characteristicRule("bemanning", "Ploeg", "bemand", comparison("greater", aggregation("count", WACHTEN, "Wacht",
                    sourceRole("ploegindeling", "wacht"), check("bevoegd", false), "null", COUNTING,
                    "het aantal wachten van de Ploeg die bevoegd zijn"), number("0"))),
            characteristicRule("kwalificatie", "Ploeg", "gekwalificeerd", TmsQuantifierFoldWitnessTest.quantifierFold("all",
                    0, null, check("bevoegd", false), "alle wachten van de Ploeg bevoegd zijn")),
            characteristicRule("toezicht", "Wacht", "gecontroleerd", pathCheck("bemand", false, "ploegindeling", "ploeg")));

    @Test void folds_and_a_check_over_a_role() throws Exception {
        Path ir = TmsDocuments.document(FOLDS_MODEL, FOLDS);
        JsonNode c = TmsDocuments.caseOn(null, List.of(
                instanceWithRoles("P1", "Ploeg", Map.of(), List.of(), Map.of("wachten", List.of("W1", "W2"))),
                instanceWithRoles("P2", "Ploeg", Map.of(), List.of(), Map.of("wachten", List.of("W3"))),
                instanceWithRoles("W1", "Wacht", Map.of(), List.of("bevoegd"), Map.of()),
                instanceWithRoles("W2", "Wacht", Map.of(), List.of(), Map.of()),
                instanceWithRoles("W3", "Wacht", Map.of(), List.of(), Map.of())), List.of());
        bothBranches(ir, c, List.of(
                // the count over the members that pass the filter, which is read per member; its nodes 2 and 3, the
                // source and the filter, have no reading of their own
                "P1 " + R + "0/versions/0 HOLDS | 0 Comparison true | 1 Aggregation 1 [W1 leeg reached] | 4 Literal 0",
                // all over two members: the contributors with their verdicts, in no order, since nothing shows it
                "P1 " + R + "1/versions/0 FAILS | 0 QuantifierFold false [W1 true, W2 false]",
                "P2 " + R + "0/versions/0 FAILS | 0 Comparison false | 1 Aggregation 0 [] | 4 Literal 0",
                "P2 " + R + "1/versions/0 FAILS | 0 QuantifierFold false [W3 false]",
                // a characteristic check over the role of a Wacht, on what its Ploeg derived
                "W1 " + R + "2/versions/0 HOLDS | 0 CharacteristicCheck true",
                "W2 " + R + "2/versions/0 HOLDS | 0 CharacteristicCheck true",
                "W3 " + R + "2/versions/0 FAILS | 0 CharacteristicCheck false"));
    }

    // ---------------- defaults ----------------

    private static final String DEFAULTS_MODEL = TmsDocuments.model(List.of(objectType("Jutter",
            List.of(attribute("bonus", WHOLE), attribute("vergoeding", WHOLE)),
            List.of("ijverig", "erkend", "havenmeester", "vrijgesteld"))), List.of());

    private static final List<String> GROUPS = List.of(
            characteristicGroup("vrijstelling van een jutter", "Jutter", "vrijgesteld", List.of(
                    decision(check("ijverig", false), true),
                    decision(check("erkend", false), true),
                    decision(check("havenmeester", false), true))),
            attributeGroup("vergoeding van een jutter", "Jutter", "vergoeding", List.of(
                    defaultEntry(check("ijverig", false), ownRead("bonus", "Jutter")),
                    defaultEntry(check("erkend", false), number("10")),
                    defaultEntry(check("havenmeester", false), number("20")))));

    @Test void a_chain_of_defaults() throws Exception {
        Path ir = TmsDocuments.document(DEFAULTS_MODEL, List.of(), List.of(), GROUPS);
        JsonNode c = TmsDocuments.caseDocument(List.of(
                instance("J1", "Jutter", Map.of(), List.of("erkend", "havenmeester"), Map.of()),
                instance("J2", "Jutter", Map.of(), List.of("ijverig", "erkend"), Map.of())), List.of());
        bothBranches(ir, c, List.of(
                // the decider in the middle: the default above it fails, the one below it is displaced (ch. 7.1)
                "J1 " + P + "0/defaults/0 FAILS | 0 CharacteristicCheck false",
                "J1 " + P + "0/defaults/1 HOLDS | 0 CharacteristicCheck true",
                "J1 " + P + "0/defaults/2 DISPLACED",
                // the consequence of a default whose condition fails is not reached
                "J1 " + P + "1/defaults/0 FAILS | 0 CharacteristicCheck false | 1 AttributeReference -",
                "J1 " + P + "1/defaults/1 HOLDS | 0 CharacteristicCheck true | 1 Literal 10",
                "J1 " + P + "1/defaults/2 DISPLACED",
                "J2 " + P + "0/defaults/0 HOLDS | 0 CharacteristicCheck true",
                "J2 " + P + "0/defaults/1 DISPLACED",
                "J2 " + P + "0/defaults/2 DISPLACED",
                // the condition holds and the consequence is leeg: the default gave no value, and the one below decides
                "J2 " + P + "1/defaults/0 HOLDS | 0 CharacteristicCheck true | 1 AttributeReference leeg",
                "J2 " + P + "1/defaults/1 HOLDS | 0 CharacteristicCheck true | 1 Literal 10",
                "J2 " + P + "1/defaults/2 DISPLACED"));
    }

    // ---------------- validity and initialisation ----------------

    private static final String TURN_MODEL = TmsDocuments.model(List.of(objectType("Jutter",
            List.of(attribute("toeslag", WHOLE)), List.of("erkend", "toekomstig"))), List.of());

    private static final List<String> TURN = List.of(
            IrPath.rule("toekomst", "KENMERKTOEKENNING", "{\"node\":\"AttributeReference\",\"attribute\":\"toekomstig\","
                            + "\"subjectType\":\"Jutter\",\"source\":{\"snippet\":\"Een Jutter is toekomstig\"}}",
                    IrPath.literal("waar", "{\"node\":\"Boolean\"}", "waar"), check("erkend", false),
                    TmsDocuments.validity("2030-01-01", null)),
            computingRule("toeslag van een jutter", "INITIALISATIE", "Jutter", "toeslag", number("3"),
                    check("erkend", false)));

    @Test void a_form_outside_its_validity_and_an_initialisation() throws Exception {
        Path ir = TmsDocuments.document(TURN_MODEL, TURN);
        JsonNode c = TmsDocuments.caseOn("2025-06-01", List.of(
                instanceWith("J1", "Jutter", Map.of(), List.of("erkend")),
                instanceWith("J2", "Jutter", Map.of("toeslag", numberValue("9")), List.of("erkend")),
                instanceWith("J3", "Jutter", Map.of(), List.of())), List.of());
        bothBranches(ir, c, List.of(
                // valid from 2030 on, and the session's date is 2025-06-01 (ch. 8.2)
                "J1 " + R + "0/versions/0 OUTSIDE_VALIDITY",
                // the target is empty: the initialisation is at its turn, and its value is read
                "J1 " + R + "1/versions/0 HOLDS | 0 CharacteristicCheck true | 1 Literal 3",
                "J2 " + R + "0/versions/0 OUTSIDE_VALIDITY",
                // the input fills the target: not the initialisation's turn (ch. 5.4)
                "J2 " + R + "1/versions/0 DISPLACED",
                "J3 " + R + "0/versions/0 OUTSIDE_VALIDITY",
                // the condition does not hold, so the value is not reached
                "J3 " + R + "1/versions/0 FAILS | 0 CharacteristicCheck false | 1 Literal -"));
    }

    // ---------------- forms without a condition ----------------

    private static final String UNCONDITIONAL_MODEL = TmsDocuments.model(List.of(
            objectType("Ploeg", List.of(attribute("opbrengst", WHOLE), attribute("toeslag", WHOLE)), List.of("bemand")),
            objectType("Wacht", List.of(attribute("vangst", WHOLE), attribute("ploegopbrengst", WHOLE),
                    attribute("premie", WHOLE)), List.of())),
            List.of(factTypeOneToMany("ploegindeling", "ploeg", "Ploeg", "wacht", "Wacht", "wachten")));

    private static final List<String> UNCONDITIONAL = List.of(
            computingRule("opbrengst van een ploeg", "BEREKENING", "Ploeg", "opbrengst", aggregation("sum",
                    ownRead("vangst", "Wacht"), "Wacht", sourceRole("ploegindeling", "wacht"), "null", "null", WHOLE,
                    "de som van de vangst van alle wachten van de Ploeg"), "null"),
            computingRule("ploegopbrengst van een wacht", "GELIJKSTELLING", "Wacht", "ploegopbrengst",
                    TmsDocuments.pathRead("opbrengst", "Ploeg", "ploegindeling", "ploeg"), "null"),
            computingRule("premie van een wacht", "INITIALISATIE", "Wacht", "premie", number("5"), "null"));

    private static final List<String> UNCONDITIONAL_GROUPS = List.of(
            attributeGroup("toeslag van een ploeg", "Ploeg", "toeslag", List.of(
                    defaultEntry(check("bemand", false), number("10")),
                    TmsDocuments.initialisationEntry(number("3")))));

    /** A form without a condition reads its value (RUNTIME-SEMANTICS ch. 5.12; card readings-of-unconditional-forms):
     *  where it is at its turn its status is {@code HOLDS} and its nodes are those of its value, the root 0; where it is
     *  not, {@code DISPLACED}, without nodes. A sum over a role with its contributors, an empty sum without any; a
     *  {@code gesteld worden op} of a value one role away; an initialisation without a condition, at its turn where the
     *  target is empty and displaced where the input fills it; and an initialisation default, below a default that
     *  decides on P1, at its turn on P2 and displaced by the input on P3.
     *
     *  <p><b>Red</b> on the code before the card: a form without a condition had no reading rule, so its records were
     *  missing and the list held only the readings of the conditional default. */
    @Test void forms_without_a_condition() throws Exception {
        Path ir = TmsDocuments.document(UNCONDITIONAL_MODEL, UNCONDITIONAL, List.of(), UNCONDITIONAL_GROUPS);
        JsonNode c = TmsDocuments.caseOn(null, List.of(
                instanceWithRoles("P1", "Ploeg", Map.of(), List.of("bemand"), Map.of("wachten", List.of("W1", "W2"))),
                instanceWithRoles("P2", "Ploeg", Map.of(), List.of(), Map.of("wachten", List.of("W3"))),
                instanceWithRoles("P3", "Ploeg", Map.of("toeslag", "9"), List.of(), Map.of()),
                instanceWithRoles("W1", "Wacht", Map.of("vangst", "4"), List.of(), Map.of()),
                instanceWithRoles("W2", "Wacht", Map.of("vangst", "6", "premie", "7"), List.of(), Map.of()),
                instanceWithRoles("W3", "Wacht", Map.of(), List.of(), Map.of())), List.of());
        bothBranches(ir, c, List.of(
                "P1 " + P + "0/defaults/0 HOLDS | 0 CharacteristicCheck true | 1 Literal 10",
                // a higher default decided: the initialisation default is not at its turn (ch. 7.1)
                "P1 " + P + "0/defaults/1 DISPLACED",
                // the sum over both Wachten: each contributes its vangst
                "P1 " + R + "0/versions/0 HOLDS | 0 Aggregation 10 [W1 4 reached, W2 6 reached]",
                "P2 " + P + "0/defaults/0 FAILS | 0 CharacteristicCheck false | 1 Literal -",
                // the target is empty in the input: the initialisation default is at its turn
                "P2 " + P + "0/defaults/1 HOLDS | 0 Literal 3",
                // W3's vangst is leeg and contributes nothing, so the sum without a fallback is leeg (ch. 1.2e)
                "P2 " + R + "0/versions/0 HOLDS | 0 Aggregation leeg []",
                "P3 " + P + "0/defaults/0 FAILS | 0 CharacteristicCheck false | 1 Literal -",
                // the input fills the target: not the initialisation default's turn (ch. 7.3)
                "P3 " + P + "0/defaults/1 DISPLACED",
                // no Wachten: no contributors, and the sum is leeg
                "P3 " + R + "0/versions/0 HOLDS | 0 Aggregation leeg []",
                // gesteld worden op: the opbrengst of the Ploeg one role away
                "W1 " + R + "1/versions/0 HOLDS | 0 AttributeReference 10",
                "W1 " + R + "2/versions/0 HOLDS | 0 Literal 5",
                "W2 " + R + "1/versions/0 HOLDS | 0 AttributeReference 10",
                // the input fills the target: not the initialisation's turn (ch. 5.4)
                "W2 " + R + "2/versions/0 DISPLACED",
                "W3 " + R + "1/versions/0 HOLDS | 0 AttributeReference leeg",
                "W3 " + R + "2/versions/0 HOLDS | 0 Literal 5"));
    }

    // ---------------- the canonical text ----------------

    /** The canonical text of the readings, which {@code DroolsBackendMain --readings} prints per case and the reference
     *  interpreter's trace is held to (card ref-trace-and-readings-gate), on both branches alike: a composite that stops,
     *  a restated comparison with the units of its operands, and the contributors of a count and of a quantifier
     *  fold. */
    @Test void the_flag_readings_prints_the_canonical_text() throws Exception {
        Path ir = TmsDocuments.document(CONDITIONS_MODEL, List.of(CONDITIONS.get(0), CONDITIONS.get(5)));
        Path c = java.nio.file.Files.createTempFile("lezing", ".json");
        c.toFile().deleteOnExit();
        java.nio.file.Files.writeString(c, TmsDocuments.caseOn(null, List.of(instanceWith("J1", "Jutter",
                Map.of("afstand", numberValue("700")), List.of("ijverig", "erkend"))), List.of()).toString());
        String name = c.getFileName().toString();
        flag(ir, c, List.of("case " + name,
                "reading " + R + "0/versions/0 \"J1\" 0 Compound false",
                "reading " + R + "0/versions/0 \"J1\" 1 CharacteristicCheck true",
                "reading " + R + "0/versions/0 \"J1\" 2 CharacteristicCheck false",
                "reading " + R + "0/versions/0 \"J1\" 3 CharacteristicCheck unreached",
                "reading " + R + "0/versions/0 \"J1\" FAILS",
                "reading " + R + "1/versions/0 \"J1\" 0 Comparison true",
                "reading " + R + "1/versions/0 \"J1\" 1 AttributeReference 700 \"m\"",
                "reading " + R + "1/versions/0 \"J1\" 2 Literal 0.5 \"km\"",
                "reading " + R + "1/versions/0 \"J1\" HOLDS",
                "READINGS 1"));
        Path folds = TmsDocuments.document(FOLDS_MODEL, List.of(FOLDS.get(0), FOLDS.get(1)));
        java.nio.file.Files.writeString(c, TmsDocuments.caseOn(null, List.of(
                instanceWithRoles("P1", "Ploeg", Map.of(), List.of(), Map.of("wachten", List.of("W1", "W2"))),
                instanceWithRoles("W1", "Wacht", Map.of(), List.of("bevoegd"), Map.of()),
                instanceWithRoles("W2", "Wacht", Map.of(), List.of(), Map.of())), List.of()).toString());
        flag(folds, c, List.of("case " + name,
                "reading " + R + "0/versions/0 \"P1\" 0 Comparison true",
                "reading " + R + "0/versions/0 \"P1\" 1 Aggregation 1 contributors [\"W1\" empty reached]",
                "reading " + R + "0/versions/0 \"P1\" 4 Literal 0",
                "reading " + R + "0/versions/0 \"P1\" HOLDS",
                "reading " + R + "1/versions/0 \"P1\" 0 QuantifierFold false contributors [\"W1\" true unordered;"
                        + " \"W2\" false unordered]",
                "reading " + R + "1/versions/0 \"P1\" FAILS",
                "READINGS 1"));
    }

    // ---------------- the table of a quantifier fold ----------------

    private static final String UNRESOLVED = "{\"node\":\"Unresolved\",\"source\":{\"snippet\":\"zijn keuring is onleesbaar\"}}";

    private static final String TABLE_MODEL = TmsDocuments.model(List.of(
            objectType("Ploeg", List.of(), List.of("inzetbaar", "bemand")),
            objectType("Wacht", List.of(), List.of("gekeurd"))),
            List.of(factTypeOneToMany("ploegindeling", "ploeg", "Ploeg", "wacht", "Wacht", "wachten")));

    /** The fault arms of the table of ch. 3.9a that the reading of a quantifier fold records (RUNTIME-SEMANTICS
     *  ch. 5.12; card fold-contributor-verdicts), which no golden reaches: a matrix that ends in an {@code Unresolved}
     *  condition (EF-019), and a filter that reaches one on a member that is not gekeurd. On P1 the fold stops at W1,
     *  and the matrix faults on W2, which the stop did not reach: the fold has its verdict, the reading its status, and
     *  W2 carries the fault, not reached. On P2 the matrix faults on W3, which the stop reached: the fold faults, and W4
     *  after it carries its verdict, not reached. The filter of the second rule faults on W1 and on W4: the
     *  contributors are not settled, and the node carries none, where before the card it carried the member whose
     *  filter held (W2, W3), in no order. The same document is the reference interpreter's
     *  {@code WV-5.12-fout-in-een-kwantorvouw}. */
    @Test void the_table_of_a_quantifier_fold_with_its_faults() throws Exception {
        Path ir = TmsDocuments.document(TABLE_MODEL, List.of(
                characteristicRule("keuring", "Ploeg", "inzetbaar", TmsQuantifierFoldWitnessTest.quantifierFold("all", 0, null,
                        compound("all", 0, List.of(check("gekeurd", false), UNRESOLVED)),
                        "alle wachten van de Ploeg aan alle volgende voorwaarden voldoen")),
                characteristicRule("bemanning", "Ploeg", "bemand", TmsQuantifierFoldWitnessTest.quantifierFold("all", 0,
                        compound("atLeast", 1, List.of(check("gekeurd", false), UNRESOLVED)), check("gekeurd", false),
                        "alle wachten van de Ploeg die gekeurd of onleesbaar zijn gekeurd zijn"))));
        Path c = java.nio.file.Files.createTempFile("tabel", ".json");
        c.toFile().deleteOnExit();
        java.nio.file.Files.writeString(c, TmsDocuments.caseOn(null, List.of(
                instanceWithRoles("P1", "Ploeg", Map.of(), List.of(), Map.of("wachten", List.of("W1", "W2"))),
                instanceWithRoles("P2", "Ploeg", Map.of(), List.of(), Map.of("wachten", List.of("W3", "W4"))),
                instanceWithRoles("W1", "Wacht", Map.of(), List.of(), Map.of()),
                instanceWithRoles("W2", "Wacht", Map.of(), List.of("gekeurd"), Map.of()),
                instanceWithRoles("W3", "Wacht", Map.of(), List.of("gekeurd"), Map.of()),
                instanceWithRoles("W4", "Wacht", Map.of(), List.of(), Map.of())), List.of()).toString());
        flag(ir, c, List.of("case " + c.getFileName(),
                "reading " + R + "0/versions/0 \"P1\" 0 QuantifierFold false contributors [\"W1\" false reached;"
                        + " \"W2\" fault EF-019 unreached]",
                "reading " + R + "0/versions/0 \"P1\" FAILS",
                "reading " + R + "0/versions/0 \"P2\" 0 QuantifierFold fault EF-019 contributors [\"W3\" fault EF-019"
                        + " reached; \"W4\" false unreached]",
                "reading " + R + "0/versions/0 \"P2\" FAULTS",
                "reading " + R + "1/versions/0 \"P1\" 0 QuantifierFold fault EF-019",
                "reading " + R + "1/versions/0 \"P1\" FAULTS",
                "reading " + R + "1/versions/0 \"P2\" 0 QuantifierFold fault EF-019",
                "reading " + R + "1/versions/0 \"P2\" FAULTS",
                "READINGS 1"));
    }

    // ---------------- the clip of a temporal total ----------------

    /** The gedurende clause of a temporal total (RUNTIME-SEMANTICS ch. 3.6) is read as the part of the timeline in which
     *  it holds (ch. 5.12): the true periods of a characteristic, also beyond the totalled timeline; negated, the
     *  periods outside them within each segment of that timeline; a composite, the intersection of its members'
     *  periods, each member read on its own; each a period without a value; and a comparison of another timeline, the
     *  segments of that timeline where it holds, with their values, cut where the totalled timeline's segments are
     *  cut. Four rules without a condition on two Jutters, J2 without a wachtvergoeding, so that its totals are 0 and
     *  the negated check holds nowhere within its segments. The document, from the RegelSpraak frontend with its
     *  {@code source.rgs} beside it, is under {@code src/test/resources/readings/temporal-total-clip}, and is the
     *  reference interpreter's {@code WV-5.12-gedurende-clip}; each line was written by hand from ch. 3.6.
     *
     *  <p><b>Red</b> before the card readings-of-unconditional-forms gave the readings of the clause: each node of the
     *  clause read the timeline the clip leaves, the wachtvergoeding with its values cut to the clause, on both
     *  branches, J1's check {@code [[2024-03-01 2024-07-01 10], [2024-07-01 2025-01-01 20]]} and its comparison
     *  {@code [[2024-01-01 2024-04-01 10]]}. */
    @Test void the_clip_of_a_temporal_total_reads_the_periods_in_which_it_holds() {
        Path dir = Path.of("src", "test", "resources", "readings", "temporal-total-clip");
        String w = R + "0/versions/0 ", n = R + "1/versions/0 ", t = R + "2/versions/0 ", b = R + "3/versions/0 ";
        String whole = "1 AttributeReference [[2024-01-01 2024-07-01 10], [2024-07-01 2025-01-01 20]]";
        flag(dir.resolve("ir.json"), dir.resolve("case.json"), List.of("case case.json",
                // wachtplichtig from March 2024 to March 2025: 4 months of 10 and 6 of 20
                "reading " + w + "\"J1\" 0 Aggregation 160",
                "reading " + w + "\"J1\" " + whole,
                "reading " + w + "\"J1\" 2 CharacteristicCheck [[2024-03-01 2025-03-01 empty]]",
                "reading " + w + "\"J1\" HOLDS",
                "reading " + w + "\"J2\" 0 Aggregation 0",
                "reading " + w + "\"J2\" 1 AttributeReference empty",
                "reading " + w + "\"J2\" 2 CharacteristicCheck [[2024-01-01 2024-04-01 empty]]",
                "reading " + w + "\"J2\" HOLDS",
                // not aangesteld: before May in the first segment, nowhere in the second
                "reading " + n + "\"J1\" 0 Aggregation 40",
                "reading " + n + "\"J1\" " + whole,
                "reading " + n + "\"J1\" 2 CharacteristicCheck [[2024-01-01 2024-05-01 empty]]",
                "reading " + n + "\"J1\" HOLDS",
                "reading " + n + "\"J2\" 0 Aggregation 0",
                "reading " + n + "\"J2\" 1 AttributeReference empty",
                "reading " + n + "\"J2\" 2 CharacteristicCheck []",
                "reading " + n + "\"J2\" HOLDS",
                // both: March and April, 2 months of 10
                "reading " + t + "\"J1\" 0 Aggregation 20",
                "reading " + t + "\"J1\" " + whole,
                "reading " + t + "\"J1\" 2 Compound [[2024-03-01 2024-05-01 empty]]",
                "reading " + t + "\"J1\" 3 CharacteristicCheck [[2024-03-01 2025-03-01 empty]]",
                "reading " + t + "\"J1\" 4 CharacteristicCheck [[2024-01-01 2024-05-01 empty]]",
                "reading " + t + "\"J1\" HOLDS",
                "reading " + t + "\"J2\" 0 Aggregation 0",
                "reading " + t + "\"J2\" 1 AttributeReference empty",
                "reading " + t + "\"J2\" 2 Compound []",
                "reading " + t + "\"J2\" 3 CharacteristicCheck [[2024-01-01 2024-04-01 empty]]",
                "reading " + t + "\"J2\" 4 CharacteristicCheck []",
                "reading " + t + "\"J2\" HOLDS",
                // borgvergoeding 15 until April 2024, cut at 2024-01-01; January to March of 10
                "reading " + b + "\"J1\" 0 Aggregation 30",
                "reading " + b + "\"J1\" " + whole,
                "reading " + b + "\"J1\" 2 Comparison [[2023-07-01 2024-01-01 15], [2024-01-01 2024-04-01 15]]",
                "reading " + b + "\"J1\" 3 AttributeReference [[2023-07-01 2024-04-01 15], [2024-04-01 2025-03-01 5]]",
                "reading " + b + "\"J1\" 4 Literal 10",
                "reading " + b + "\"J1\" HOLDS",
                "reading " + b + "\"J2\" 0 Aggregation 0",
                "reading " + b + "\"J2\" 1 AttributeReference empty",
                "reading " + b + "\"J2\" 2 Comparison [[2024-01-01 2024-07-01 12]]",
                "reading " + b + "\"J2\" 3 AttributeReference [[2024-01-01 2024-07-01 12]]",
                "reading " + b + "\"J2\" 4 Literal 10",
                "reading " + b + "\"J2\" HOLDS",
                "READINGS 1"));
    }

    /** The output of {@code --readings} on {@code ir} and the case {@code c}, on both branches, the expected lines. */
    private static void flag(Path ir, Path c, List<String> expected) {
        for (boolean drl : List.of(false, true)) {
            java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
            List<String> arguments = new ArrayList<>(List.of("--readings"));
            if (drl) arguments.add("--drl");
            arguments.addAll(List.of(ir.toString(), c.toString()));
            int code = DroolsBackendMain.run(arguments.toArray(String[]::new),
                    new java.io.PrintStream(out, true, java.nio.charset.StandardCharsets.UTF_8));
            assertEquals(expected, out.toString(java.nio.charset.StandardCharsets.UTF_8).lines().toList(),
                    (drl ? "DRL_TEXT" : "EXEC_MODEL") + ": de canonieke tekst");
            assertEquals(0, code);
        }
    }

    // ---------------- the reading ----------------

    /** The readings of {@code c} on both branches, each the expected list. */
    private static void bothBranches(Path ir, JsonNode c, List<String> expected) throws Exception {
        for (DroolsBackend.Branch branch : TmsDocuments.BRANCHES) {
            List<String> records = CaseRunner.forMeasurement(ir, branch, true).withSession(c, (build, session, labels) -> {
                DroolsBackend.Firing firing = DroolsBackend.fire(build, session);
                assertEquals(null, firing.noFixpoint(), branch + ": een fixpunt");
                Map<Object, String> label = new IdentityHashMap<>();
                labels.forEach((l, o) -> label.put(o, l));
                List<String> out = new ArrayList<>();
                for (ConditionReading r : DroolsBackend.readingsOf(build, session)) out.add(line(label, r));
                return out.stream().sorted().toList();
            });
            assertEquals(expected, records, branch + ": de lezingen");
        }
    }

    /** A record as one line ({@link ReadingsWitnessTest}). */
    private static String line(Map<Object, String> label, ConditionReading r) {
        StringBuilder b = new StringBuilder(label.get(r.object()) + " " + r.form() + " " + r.status());
        for (ConditionReading.NodeReading n : r.nodes()) {
            b.append(" | ").append(n.node()).append(' ').append(n.kind()).append(' ');
            if (!n.reached()) {
                b.append('-');
                continue;
            }
            b.append(n.fault() != null ? "fault " + n.fault().getMessage() : text(n.value()));
            if (n.unit() != null) b.append(' ').append(n.unit().rendering());
            if (!n.contributors().isEmpty() || n.kind().equals("Aggregation") || n.kind().equals("QuantifierFold")) {
                List<String> cs = new ArrayList<>();
                for (ConditionReading.Contributor x : n.contributors())
                    cs.add(label.get(x.member()) + " " + text(x.value())
                            + (x.reached() == null ? "" : x.reached() ? " reached" : " unreached"));
                b.append(" [").append(String.join(", ", cs.stream().sorted().toList())).append(']');
            }
        }
        return b.toString();
    }

    private static String text(Object v) {
        return v == null ? "leeg" : v instanceof BigDecimal d ? d.stripTrailingZeros().toPlainString() : v.toString();
    }
}
