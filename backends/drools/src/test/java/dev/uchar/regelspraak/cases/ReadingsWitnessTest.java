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
import static dev.uchar.regelspraak.cases.TmsDocuments.subtype;
import static dev.uchar.regelspraak.cases.TmsDocuments.unit;
import static dev.uchar.regelspraak.cases.TmsDocuments.unitSystem;
import static dev.uchar.regelspraak.cases.TmsDocuments.withExceptions;
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
 *   <li>Defaults: a chain with its decider in the middle, the default above it failing and the one below it displaced,
 *       and read all the same; and an attribute default whose condition holds and whose consequence is leeg, which gave
 *       no value, so that the default below it decides. The defaults a higher default displaced, in a chain, in an
 *       exception group, as the content of a default whose exception group decided, as a rule a subtype's stack takes
 *       in, and as the initialisation default.</li>
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
                // the decider in the middle: the default above it fails, the one below it is displaced (ch. 7.1), and
                // read all the same (ch. 5.12)
                "J1 " + P + "0/defaults/0 FAILS | 0 CharacteristicCheck false",
                "J1 " + P + "0/defaults/1 HOLDS | 0 CharacteristicCheck true",
                "J1 " + P + "0/defaults/2 DISPLACED | 0 CharacteristicCheck true",
                // the consequence of a default whose condition fails is not reached
                "J1 " + P + "1/defaults/0 FAILS | 0 CharacteristicCheck false | 1 AttributeReference -",
                "J1 " + P + "1/defaults/1 HOLDS | 0 CharacteristicCheck true | 1 Literal 10",
                "J1 " + P + "1/defaults/2 DISPLACED | 0 CharacteristicCheck true | 1 Literal 20",
                "J2 " + P + "0/defaults/0 HOLDS | 0 CharacteristicCheck true",
                "J2 " + P + "0/defaults/1 DISPLACED | 0 CharacteristicCheck true",
                "J2 " + P + "0/defaults/2 DISPLACED | 0 CharacteristicCheck false",
                // the condition holds and the consequence is leeg: the default gave no value, and the one below decides
                "J2 " + P + "1/defaults/0 HOLDS | 0 CharacteristicCheck true | 1 AttributeReference leeg",
                "J2 " + P + "1/defaults/1 HOLDS | 0 CharacteristicCheck true | 1 Literal 10",
                "J2 " + P + "1/defaults/2 DISPLACED | 0 CharacteristicCheck false | 1 Literal -"));
    }

    // ---------------- displaced defaults ----------------

    private static final String DISPLACED_MODEL = TmsDocuments.model(List.of(
            objectType("Jutter", List.of(attribute("bonus", WHOLE), attribute("vergoeding", WHOLE),
                            attribute("heffing", WHOLE), attribute("toeslag", WHOLE)),
                    List.of("ijverig", "erkend", "havenmeester", "vrijgesteld", "grondX", "grondY", "grondZ")),
            objectType("Strandrecht", List.of(attribute("uitkering", WHOLE)), List.of("betwist", "geborgen")),
            subtype("Wrakrecht", "Strandrecht", List.of(), List.of())), List.of());

    private static final List<String> DISPLACED_RULES = List.of(
            // a subtype's own rule on a target whose chain declares a defaults group: the base of the Wrakrecht stack,
            // below the exception it inherits (ch. 7.10)
            computingRule("uitkering van een wrakrecht", "BEREKENING", "Wrakrecht", "uitkering", number("20"),
                    check("geborgen", false)));

    private static final List<String> DISPLACED_GROUPS = List.of(
            characteristicGroup("vrijstelling van een jutter", "Jutter", "vrijgesteld", List.of(
                    decision(check("ijverig", false), true),
                    decision(check("havenmeester", false), false))),
            attributeGroup("vergoeding van een jutter", "Jutter", "vergoeding", List.of(
                    defaultEntry(check("ijverig", false), number("30")),
                    defaultEntry(check("erkend", false), ownRead("bonus", "Jutter")),
                    defaultEntry(check("havenmeester", false), number("20")))),
            attributeGroup("heffing van een jutter", "Jutter", "heffing", List.of(
                    defaultEntry(check("grondX", false), number("40")),
                    withExceptions(check("grondY", false), number("15"),
                            List.of(List.of(defaultEntry(check("grondZ", false), number("5"))))))),
            attributeGroup("toeslag van een jutter", "Jutter", "toeslag", List.of(
                    defaultEntry(check("erkend", false), number("10")),
                    TmsDocuments.initialisationEntry(number("3")))),
            attributeGroup("uitkering van een strandrecht", "Strandrecht", "uitkering", List.of(
                    defaultEntry(check("betwist", false), number("99")),
                    defaultEntry(compound("all", 0, List.of()), number("50")))));

    /** A default that a higher default displaced is read all the same (RUNTIME-SEMANTICS ch. 5.12; card
     *  readings-of-displaced-defaults): its status stays {@code DISPLACED}, and its nodes are those of its condition,
     *  and of its value where the condition holds, evaluated on the fixpoint world as at its turn. Below the decider of
     *  a chain of attribute defaults, on J1 a default that held and gave a value (the bonus 7) and on J2 one that held
     *  and gave none, and one that did not hold; a characteristic default below the decider, on J1 one that held and on
     *  J2 one that did not; on J1, below a default that decided, a form-a default and the default in its exception
     *  group; on J2, where that exception group decided, the content of the form-a default; on the Wrakrecht W1, the
     *  subtype's own rule, which the fold of ch. 7.10 takes into the stack below the inherited exception that decided;
     *  and the initialisation default below a default that decided, read on J1, whose target the input leaves empty,
     *  and without nodes on J2, whose target the input fills (ch. 7.3).
     *
     *  <p><b>Red</b> on the code before the card: every one of these displaced defaults had the status
     *  {@code DISPLACED} and no nodes. */
    @Test void displaced_defaults_are_read() throws Exception {
        Path ir = TmsDocuments.document(DISPLACED_MODEL, DISPLACED_RULES, List.of(), DISPLACED_GROUPS);
        JsonNode c = TmsDocuments.caseDocument(List.of(
                instance("J1", "Jutter", Map.of("bonus", "7"),
                        List.of("ijverig", "erkend", "havenmeester", "grondX", "grondY", "grondZ"), Map.of()),
                instance("J2", "Jutter", Map.of("toeslag", "9"), List.of("ijverig", "erkend", "grondY", "grondZ"),
                        Map.of()),
                instance("W1", "Wrakrecht", Map.of(), List.of("betwist", "geborgen"), Map.of())), List.of());
        String heffing = P + "2/defaults/1";
        bothBranches(ir, c, List.of(
                "J1 " + P + "0/defaults/0 HOLDS | 0 CharacteristicCheck true",
                // a characteristic default below the decider whose justification holds
                "J1 " + P + "0/defaults/1 DISPLACED | 0 CharacteristicCheck true",
                "J1 " + P + "1/defaults/0 HOLDS | 0 CharacteristicCheck true | 1 Literal 30",
                // held, and gave a value
                "J1 " + P + "1/defaults/1 DISPLACED | 0 CharacteristicCheck true | 1 AttributeReference 7",
                "J1 " + P + "1/defaults/2 DISPLACED | 0 CharacteristicCheck true | 1 Literal 20",
                "J1 " + P + "2/defaults/0 HOLDS | 0 CharacteristicCheck true | 1 Literal 40",
                // a form-a default below the decider, and the default in its exception group
                "J1 " + heffing + " DISPLACED | 0 CharacteristicCheck true | 1 Literal 15",
                "J1 " + heffing + "/exceptions/members/0/defaults/0 DISPLACED | 0 CharacteristicCheck true | 1 Literal 5",
                "J1 " + P + "3/defaults/0 HOLDS | 0 CharacteristicCheck true | 1 Literal 10",
                // the initialisation default below the decider, its target empty in the input
                "J1 " + P + "3/defaults/1 DISPLACED | 0 Literal 3",
                "J2 " + P + "0/defaults/0 HOLDS | 0 CharacteristicCheck true",
                "J2 " + P + "0/defaults/1 DISPLACED | 0 CharacteristicCheck false",
                "J2 " + P + "1/defaults/0 HOLDS | 0 CharacteristicCheck true | 1 Literal 30",
                // held, and gave no value
                "J2 " + P + "1/defaults/1 DISPLACED | 0 CharacteristicCheck true | 1 AttributeReference leeg",
                // did not hold
                "J2 " + P + "1/defaults/2 DISPLACED | 0 CharacteristicCheck false | 1 Literal -",
                "J2 " + P + "2/defaults/0 FAILS | 0 CharacteristicCheck false | 1 Literal -",
                // the exception group decided: the content of the form-a default, its fall-back, was not at its turn
                "J2 " + heffing + " DISPLACED | 0 CharacteristicCheck true | 1 Literal 15",
                "J2 " + heffing + "/exceptions/members/0/defaults/0 HOLDS | 0 CharacteristicCheck true | 1 Literal 5",
                "J2 " + P + "3/defaults/0 HOLDS | 0 CharacteristicCheck true | 1 Literal 10",
                // the input fills the target: the initialisation default has nothing to read (ch. 7.3)
                "J2 " + P + "3/defaults/1 DISPLACED",
                "W1 " + P + "4/defaults/0 HOLDS | 0 CharacteristicCheck true | 1 Literal 99",
                // the subtype's own rule, the base of its stack below the inherited exception
                "W1 " + R + "0/versions/0 DISPLACED | 0 CharacteristicCheck true | 1 Literal 20"));
        // the canonical text writes the nodes of a displaced default under its status line, as of every reading
        Path chain = TmsDocuments.document(DISPLACED_MODEL, List.of(), List.of(), List.of(DISPLACED_GROUPS.get(1)));
        Path cases = java.nio.file.Files.createTempFile("verdrongen", ".json");
        cases.toFile().deleteOnExit();
        java.nio.file.Files.writeString(cases, TmsDocuments.caseDocument(List.of(
                instance("J1", "Jutter", Map.of("bonus", "7"), List.of("ijverig", "erkend", "havenmeester"), Map.of()),
                instance("J2", "Jutter", Map.of(), List.of("ijverig", "erkend"), Map.of())), List.of()).toString());
        String r = "reading " + P + "0/defaults/";
        flag(chain, cases, List.of("case " + cases.getFileName(),
                r + "0 \"J1\" 0 CharacteristicCheck true",
                r + "0 \"J1\" 1 Literal 30",
                r + "0 \"J1\" HOLDS",
                r + "0 \"J2\" 0 CharacteristicCheck true",
                r + "0 \"J2\" 1 Literal 30",
                r + "0 \"J2\" HOLDS",
                r + "1 \"J1\" 0 CharacteristicCheck true",
                r + "1 \"J1\" 1 AttributeReference 7",
                r + "1 \"J1\" DISPLACED",
                r + "1 \"J2\" 0 CharacteristicCheck true",
                r + "1 \"J2\" 1 AttributeReference empty",
                r + "1 \"J2\" DISPLACED",
                r + "2 \"J1\" 0 CharacteristicCheck true",
                r + "2 \"J1\" 1 Literal 20",
                r + "2 \"J1\" DISPLACED",
                r + "2 \"J2\" 0 CharacteristicCheck false",
                r + "2 \"J2\" 1 Literal unreached",
                r + "2 \"J2\" DISPLACED",
                "READINGS 1"));
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
                // a higher default decided: the initialisation default is not at its turn (ch. 7.1), and is read all
                // the same (ch. 5.12)
                "P1 " + P + "0/defaults/1 DISPLACED | 0 Literal 3",
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

    // ---------------- the objects a reading read its members on ----------------

    /** A node that reads a member of one object names the object it read it on (RUNTIME-SEMANTICS ch. 5.12): the
     *  object of the reading for a member of its own, the object the path reached for one over a path, and none where
     *  a link on the path is absent. Seven forms on Kustvakken, Posten and Wachters: a computation from the Wachter's own
     *  diensttijd; a comparison of the hoogte of his post, one step; the gevarenklasse of the kustvak of his post, two
     *  steps, as the value of a computation without a condition; a check of the kustvak of a Post, of a characteristic
     *  of the input; a check of the post of a Wachter, of a characteristic a rule derives; the hoogte of his post in
     *  the value of a computation with a condition, which is not reached where the condition fails; and the
     *  gevarenklasse of his wachtvak, over the relation a fact creation lays. W1 holds post P1 in kustvak V1, W4 post
     *  P3, which lies in no kustvak, and W3 no post, so that his paths and W4's of two steps meet an absent link. The
     *  document, from the RegelSpraak frontend with its {@code source.rgs} beside it, is under
     *  {@code src/test/resources/readings/read-objects}, and is the reference interpreter's
     *  {@code WV-5.12-gelezen-objecten}; each object was written by hand from the case.
     *
     *  <p>A record is a line as {@link ReadingsWitnessTest} writes one, each node that names an object followed by
     *  {@code @} and its label.
     *
     *  <p><b>Red</b> before the card reading-read-objects: {@code ConditionReading.NodeReading} had no object, so this
     *  class did not compile, and the canonical text had no {@code on}. */
    @Test void the_objects_a_reading_read_its_members_on() throws Exception {
        Path dir = Path.of("src", "test", "resources", "readings", "read-objects");
        JsonNode c = TmsDocuments.JSON.readTree(dir.resolve("case.json").toFile());
        bothBranches(dir.resolve("ir.json"), c, true, List.of(
                // a characteristic of the input on the kustvak of a Post, and none where the Post lies in no kustvak
                "P1 " + R + "3/versions/0 HOLDS | 0 CharacteristicCheck true @V1",
                "P3 " + R + "3/versions/0 FAILS | 0 CharacteristicCheck false",
                // the Wachter's own diensttijd: the object of the reading
                "W1 " + R + "0/versions/0 HOLDS | 0 Binary 5 | 1 AttributeReference 4 @W1 | 2 Literal 1",
                // one step, in a condition
                "W1 " + R + "1/versions/0 HOLDS | 0 Comparison true | 1 AttributeReference 12 @P1 | 2 Literal 10",
                // two steps, the value of a computation without a condition
                "W1 " + R + "2/versions/0 HOLDS | 0 AttributeReference 5 @V1",
                // a characteristic a rule derives, on the post of the Wachter
                "W1 " + R + "4/versions/0 HOLDS | 0 CharacteristicCheck true @P1",
                // one step in the value of a computation with a condition, after the check of his own characteristic
                "W1 " + R + "5/versions/0 HOLDS | 0 CharacteristicCheck true @W1 | 1 Binary 14"
                        + " | 2 AttributeReference 12 @P1 | 3 Literal 2",
                // over the relation the fact creation vakbewaking lays: V1's posten are P1, whose wachter is W1
                "W1 " + R + "6/versions/0 HOLDS | 0 AttributeReference 5 @V1",
                "W3 " + R + "0/versions/0 HOLDS | 0 Binary 2 | 1 AttributeReference 1 @W3 | 2 Literal 1",
                // no post: every path of W3 meets an absent link at its first step, and names no object
                "W3 " + R + "1/versions/0 FAILS | 0 Comparison false | 1 AttributeReference leeg | 2 Literal 10",
                "W3 " + R + "2/versions/0 HOLDS | 0 AttributeReference leeg",
                "W3 " + R + "4/versions/0 FAILS | 0 CharacteristicCheck false",
                "W3 " + R + "5/versions/0 HOLDS | 0 CharacteristicCheck true @W3 | 1 Binary 2"
                        + " | 2 AttributeReference leeg | 3 Literal 2",
                "W3 " + R + "6/versions/0 HOLDS | 0 AttributeReference leeg",
                "W4 " + R + "0/versions/0 HOLDS | 0 Binary 8 | 1 AttributeReference 7 @W4 | 2 Literal 1",
                "W4 " + R + "1/versions/0 FAILS | 0 Comparison false | 1 AttributeReference 9 @P3 | 2 Literal 10",
                // P3 lies in no kustvak: the second step is absent
                "W4 " + R + "2/versions/0 HOLDS | 0 AttributeReference leeg",
                "W4 " + R + "4/versions/0 FAILS | 0 CharacteristicCheck false @P3",
                // the condition fails, so the read in the value is not reached and names no object
                "W4 " + R + "5/versions/0 FAILS | 0 CharacteristicCheck false @W4 | 1 Binary - | 2 AttributeReference -"
                        + " | 3 Literal -",
                // no kustvak, so no wachtvak either
                "W4 " + R + "6/versions/0 HOLDS | 0 AttributeReference leeg"));
        String r = "reading " + R;
        flag(dir.resolve("ir.json"), dir.resolve("case.json"), List.of("case case.json",
                r + "0/versions/0 \"W1\" 0 Binary 5",
                r + "0/versions/0 \"W1\" 1 AttributeReference 4",
                r + "0/versions/0 \"W1\" 2 Literal 1",
                r + "0/versions/0 \"W1\" HOLDS",
                r + "0/versions/0 \"W3\" 0 Binary 2",
                r + "0/versions/0 \"W3\" 1 AttributeReference 1",
                r + "0/versions/0 \"W3\" 2 Literal 1",
                r + "0/versions/0 \"W3\" HOLDS",
                r + "0/versions/0 \"W4\" 0 Binary 8",
                r + "0/versions/0 \"W4\" 1 AttributeReference 7",
                r + "0/versions/0 \"W4\" 2 Literal 1",
                r + "0/versions/0 \"W4\" HOLDS",
                r + "1/versions/0 \"W1\" 0 Comparison true",
                r + "1/versions/0 \"W1\" 1 AttributeReference 12 on \"P1\"",
                r + "1/versions/0 \"W1\" 2 Literal 10",
                r + "1/versions/0 \"W1\" HOLDS",
                r + "1/versions/0 \"W3\" 0 Comparison false",
                r + "1/versions/0 \"W3\" 1 AttributeReference empty on none",
                r + "1/versions/0 \"W3\" 2 Literal 10",
                r + "1/versions/0 \"W3\" FAILS",
                r + "1/versions/0 \"W4\" 0 Comparison false",
                r + "1/versions/0 \"W4\" 1 AttributeReference 9 on \"P3\"",
                r + "1/versions/0 \"W4\" 2 Literal 10",
                r + "1/versions/0 \"W4\" FAILS",
                r + "2/versions/0 \"W1\" 0 AttributeReference 5 on \"V1\"",
                r + "2/versions/0 \"W1\" HOLDS",
                r + "2/versions/0 \"W3\" 0 AttributeReference empty on none",
                r + "2/versions/0 \"W3\" HOLDS",
                r + "2/versions/0 \"W4\" 0 AttributeReference empty on none",
                r + "2/versions/0 \"W4\" HOLDS",
                r + "3/versions/0 \"P1\" 0 CharacteristicCheck true on \"V1\"",
                r + "3/versions/0 \"P1\" HOLDS",
                r + "3/versions/0 \"P3\" 0 CharacteristicCheck false on none",
                r + "3/versions/0 \"P3\" FAILS",
                r + "4/versions/0 \"W1\" 0 CharacteristicCheck true on \"P1\"",
                r + "4/versions/0 \"W1\" HOLDS",
                r + "4/versions/0 \"W3\" 0 CharacteristicCheck false on none",
                r + "4/versions/0 \"W3\" FAILS",
                r + "4/versions/0 \"W4\" 0 CharacteristicCheck false on \"P3\"",
                r + "4/versions/0 \"W4\" FAILS",
                r + "5/versions/0 \"W1\" 0 CharacteristicCheck true",
                r + "5/versions/0 \"W1\" 1 Binary 14",
                r + "5/versions/0 \"W1\" 2 AttributeReference 12 on \"P1\"",
                r + "5/versions/0 \"W1\" 3 Literal 2",
                r + "5/versions/0 \"W1\" HOLDS",
                r + "5/versions/0 \"W3\" 0 CharacteristicCheck true",
                r + "5/versions/0 \"W3\" 1 Binary 2",
                r + "5/versions/0 \"W3\" 2 AttributeReference empty on none",
                r + "5/versions/0 \"W3\" 3 Literal 2",
                r + "5/versions/0 \"W3\" HOLDS",
                r + "5/versions/0 \"W4\" 0 CharacteristicCheck false",
                r + "5/versions/0 \"W4\" 1 Binary unreached",
                r + "5/versions/0 \"W4\" 2 AttributeReference unreached",
                r + "5/versions/0 \"W4\" 3 Literal unreached",
                r + "5/versions/0 \"W4\" FAILS",
                r + "6/versions/0 \"W1\" 0 AttributeReference 5 on \"V1\"",
                r + "6/versions/0 \"W1\" HOLDS",
                r + "6/versions/0 \"W3\" 0 AttributeReference empty on none",
                r + "6/versions/0 \"W3\" HOLDS",
                r + "6/versions/0 \"W4\" 0 AttributeReference empty on none",
                r + "6/versions/0 \"W4\" HOLDS",
                "READINGS 1"));
    }

    /** The justification of a displaced default that faults: the fault stands at its nodes, by the EF code of its kind,
     *  and the status stays {@code DISPLACED} (RUNTIME-SEMANTICS ch. 5.12): the session never evaluated the default,
     *  and has no such fault. A characteristic default below one that decides on both Jutters, whose justification
     *  ends in an {@code Unresolved} condition (EF-019): on J1, who is erkend, the composite reaches it and faults; on
     *  J2 it stops at its first member. The same document is the reference interpreter's
     *  {@code WV-5.12-fout-in-een-verdrongen-default}.
     *
     *  <p><b>Red</b> on the code before the card readings-of-displaced-defaults: the displaced default had no nodes. */
    @Test void a_displaced_default_whose_justification_faults() throws Exception {
        String unresolved = "{\"node\":\"Unresolved\",\"source\":{\"snippet\":\"zijn erkenning is onleesbaar\"}}";
        Path ir = TmsDocuments.document(TmsDocuments.model(List.of("ijverig", "erkend", "vrijgesteld")), List.of(),
                List.of(), List.of(characteristicGroup("vrijstelling van een jutter", "Jutter", "vrijgesteld", List.of(
                        decision(check("ijverig", false), true),
                        decision(compound("all", 0, List.of(check("erkend", false), unresolved)), false)))));
        Path c = java.nio.file.Files.createTempFile("verdrongen-fout", ".json");
        c.toFile().deleteOnExit();
        java.nio.file.Files.writeString(c, TmsDocuments.caseDocument(List.of(
                TmsDocuments.jutter("J1", List.of("ijverig", "erkend")), TmsDocuments.jutter("J2", List.of("ijverig"))),
                List.of(TmsDocuments.consistent())).toString());
        String r = "reading " + P + "0/defaults/";
        flag(ir, c, List.of("case " + c.getFileName(),
                r + "0 \"J1\" 0 CharacteristicCheck true",
                r + "0 \"J1\" HOLDS",
                r + "0 \"J2\" 0 CharacteristicCheck true",
                r + "0 \"J2\" HOLDS",
                r + "1 \"J1\" 0 Compound fault EF-019",
                r + "1 \"J1\" 1 CharacteristicCheck true",
                r + "1 \"J1\" 2 Unresolved fault EF-019",
                r + "1 \"J1\" DISPLACED",
                r + "1 \"J2\" 0 Compound false",
                r + "1 \"J2\" 1 CharacteristicCheck false",
                r + "1 \"J2\" 2 Unresolved unreached",
                r + "1 \"J2\" DISPLACED",
                "READINGS 1"));
    }

    /** A condition that reads as many bound values as the test of a rule takes (thirteen, {@code MAX_READS}), three of
     *  them over a path, is carried by the build with readings as by the production build: the object a read over a
     *  path reached is bound for the reading, and stays out of the values the test reads. W1 holds post P1, W2 none, so
     *  that the composite stops on W2 at its first read over the path, which names no object.
     *
     *  <p><b>Red</b> on the code of card reading-read-objects ({@code f0ff72ab}): the build with readings put the slot
     *  of each object among the values of the test, sixteen, and refused the rule (DROOLS-264, "een voorwaarde die 16
     *  waarden leest"), on both branches, where the production build carried it. */
    @Test void a_condition_that_reads_as_many_values_as_a_test_takes() throws Exception {
        List<String> own = List.of("diensttijd", "rang", "leeftijd", "ervaring", "nachten", "dagen", "uren", "ronden",
                "meldingen", "vangsten");
        List<String> post = List.of("hoogte", "breedte", "diepte");
        List<String> attributes = new ArrayList<>();
        for (String a : own) attributes.add(attribute(a, WHOLE));
        List<String> postAttributes = new ArrayList<>();
        for (String a : post) postAttributes.add(attribute(a, WHOLE));
        String model = TmsDocuments.model(List.of(objectType("Wachter", attributes, List.of("inzetbaar")),
                        objectType("Post", postAttributes, List.of())),
                List.of(factTypeOneToMany("postbezetting", "post", "Post", "wachter", "Wachter", "wachters")));
        List<String> members = new ArrayList<>();
        for (String a : own) members.add(comparison("greater", ownRead(a, "Wachter"), number("0")));
        for (String a : post) members.add(comparison("greater", TmsDocuments.pathRead(a, "Post", "postbezetting", "post"),
                number("0")));
        Path ir = TmsDocuments.document(model, List.of(characteristicRule("inzet", "Wachter", "inzetbaar",
                compound("all", 0, members))));
        Map<String, String> ones = new java.util.TreeMap<>();
        for (String a : own) ones.put(a, "1");
        Map<String, String> postOnes = new java.util.TreeMap<>();
        for (String a : post) postOnes.put(a, "1");
        JsonNode c = TmsDocuments.caseOn(null, List.of(
                instanceWithRoles("P1", "Post", postOnes, List.of(), Map.of()),
                instanceWithRoles("W1", "Wachter", ones, List.of(), Map.of("post", List.of("P1"))),
                instanceWithRoles("W2", "Wachter", ones, List.of(), Map.of())), List.of());
        // the production build carries the rule: W1 is inzetbaar, W2 is not
        for (DroolsBackend.Branch branch : TmsDocuments.BRANCHES) {
            List<String> derived = CaseRunner.forMeasurement(ir, branch, false).withSession(c, (build, session, labels) -> {
                assertEquals(null, DroolsBackend.fire(build, session).noFixpoint(), branch + ": een fixpunt");
                Map<Object, String> label = new IdentityHashMap<>();
                labels.forEach((l, o) -> label.put(o, l));
                List<String> out = new ArrayList<>();
                for (var k : DroolsBackend.fixpointOf(build, session, label::get).characteristics())
                    out.add(label.get(k.object()) + " " + k.characteristic());
                return out;
            });
            assertEquals(List.of("W1 inzetbaar"), derived, branch + ": de productiebuild");
        }
        StringBuilder w1 = new StringBuilder("W1 " + R + "0/versions/0 HOLDS | 0 Compound true");
        StringBuilder w2 = new StringBuilder("W2 " + R + "0/versions/0 FAILS | 0 Compound false");
        for (int k = 0; k < 13; k++) {
            int n = 1 + 3 * k;
            boolean overPath = k >= own.size();
            w1.append(" | ").append(n).append(" Comparison true | ").append(n + 1).append(" AttributeReference 1 @")
                    .append(overPath ? "P1" : "W1").append(" | ").append(n + 2).append(" Literal 0");
            if (!overPath)
                w2.append(" | ").append(n).append(" Comparison true | ").append(n + 1)
                        .append(" AttributeReference 1 @W2 | ").append(n + 2).append(" Literal 0");
            // no post: the first read over the path is leeg and names no object, and the composite stops there
            else if (k == own.size())
                w2.append(" | ").append(n).append(" Comparison false | ").append(n + 1)
                        .append(" AttributeReference leeg | ").append(n + 2).append(" Literal 0");
            else
                w2.append(" | ").append(n).append(" Comparison - | ").append(n + 1).append(" AttributeReference - | ")
                        .append(n + 2).append(" Literal -");
        }
        bothBranches(ir, c, true, List.of(w1.toString(), w2.toString()));
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
        bothBranches(ir, c, false, expected);
    }

    /** As {@link #bothBranches(Path, JsonNode, List)}, each node that names an object followed by its label where
     *  {@code objects}. */
    private static void bothBranches(Path ir, JsonNode c, boolean objects, List<String> expected) throws Exception {
        for (DroolsBackend.Branch branch : TmsDocuments.BRANCHES) {
            List<String> records = CaseRunner.forMeasurement(ir, branch, true).withSession(c, (build, session, labels) -> {
                DroolsBackend.Firing firing = DroolsBackend.fire(build, session);
                assertEquals(null, firing.noFixpoint(), branch + ": een fixpunt");
                Map<Object, String> label = new IdentityHashMap<>();
                labels.forEach((l, o) -> label.put(o, l));
                List<String> out = new ArrayList<>();
                for (ConditionReading r : DroolsBackend.readingsOf(build, session)) out.add(line(label, r, objects));
                return out.stream().sorted().toList();
            });
            assertEquals(expected, records, branch + ": de lezingen");
        }
    }

    /** A record as one line ({@link ReadingsWitnessTest}), each node that names an object followed by {@code @} and its
     *  label where {@code objects}. */
    private static String line(Map<Object, String> label, ConditionReading r, boolean objects) {
        StringBuilder b = new StringBuilder(label.get(r.object()) + " " + r.form() + " " + r.status());
        for (ConditionReading.NodeReading n : r.nodes()) {
            b.append(" | ").append(n.node()).append(' ').append(n.kind()).append(' ');
            if (!n.reached()) {
                b.append('-');
                continue;
            }
            b.append(n.fault() != null ? "fault " + n.fault().getMessage() : text(n.value()));
            if (n.unit() != null) b.append(' ').append(n.unit().rendering());
            if (objects && n.object() != null) b.append(" @").append(label.get(n.object()));
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
