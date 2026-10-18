package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;

import dev.uchar.regelspraak.transpiler.tms.HasCharacteristic;

import org.drools.model.Rule;
import org.junit.jupiter.api.Test;
import org.kie.api.event.rule.DefaultRuleRuntimeEventListener;
import org.kie.api.event.rule.ObjectInsertedEvent;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static dev.uchar.regelspraak.cases.TmsDocuments.WHOLE;
import static dev.uchar.regelspraak.cases.TmsDocuments.attribuut;
import static dev.uchar.regelspraak.cases.TmsDocuments.sourceRole;
import static dev.uchar.regelspraak.cases.TmsDocuments.check;
import static dev.uchar.regelspraak.cases.TmsDocuments.consistent;
import static dev.uchar.regelspraak.cases.TmsDocuments.factTypeOneToMany;
import static dev.uchar.regelspraak.cases.TmsDocuments.number;
import static dev.uchar.regelspraak.cases.TmsDocuments.inconsistent;
import static dev.uchar.regelspraak.cases.TmsDocuments.instanceWithRoles;
import static dev.uchar.regelspraak.cases.TmsDocuments.characteristicRule;
import static dev.uchar.regelspraak.cases.TmsDocuments.reading;
import static dev.uchar.regelspraak.cases.TmsDocuments.computingRule;
import static dev.uchar.regelspraak.cases.TmsDocuments.compound;
import static dev.uchar.regelspraak.cases.TmsDocuments.comparison;
import static dev.uchar.regelspraak.cases.TmsDocuments.verwacht;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The quantifier fold on the exec-model branch (RUNTIME-SEMANTICS ch. 3.9a): one condition asked of every
 * member of a linked instance set, under all, none and the three counting quantifiers, over members whose derived
 * characteristic or attribute changes while the session fires.
 *
 * <p>The model: a Ploeg holds its Wachten (ploegindeling: ploeg, one Ploeg; wacht, many Wachten). "Een Wacht is
 * geschorst indien zijn klachten groter is dan 2" and "Een Wacht is bevoegd indien hij aan alle volgende voorwaarden
 * voldoet: zijn dienstjaren is groter dan 5; hij is niet geschorst", so in the orders where the bevoegdheid fires
 * before the schorsing, a Wacht with more than 2 klachten is bevoegd first and loses it afterwards. The folds read
 * that characteristic: sterk (all wachten bevoegd), zwak (none), bezet (at least 2), krap (at most 1) and paraat
 * (exactly 1), each in a document of its own with the two derivations, in every order of the three rules. Case A:
 * P1 with W1 (bevoegd) and W2 (8 dienstjaren, 3 klachten: geschorst), P2 with W3 and W4 (both bevoegd), P3 without
 * Wachten (the vacuous set) and P4 with W5 (geschorst); case B: P1 with W1, W2 and W6 (bevoegd), P2 with W7 alone,
 * geschorst as W2 is. Read on the transient bevoegdheid of W2 and W7, case B gives P1 sterk and P2 sterk, paraat and
 * not zwak, which the fixpoint does not. Then a fold with a filter over the same characteristic and a matrix over a derived attribute (punten,
 * computed from the opgave), in every order of four rules: gekwalificeerd, "de punten van alle wachten van de Ploeg
 * die bevoegd zijn groter is dan 5".
 *
 * <p>The short circuit (ch. 3.9a, 5.10): the contributors are settled in full first, filter included, and the
 * matrix is then read in the contributors' order and stops at the first member after which the verdict stands; a
 * member it does not reach raises no fault and deposits nothing. The matrix "zijn opgave gedeeld door zijn
 * deelnemers is groter dan 2" divides by a value derived from the aanmelding: A (10 by 1) holds, B (no aanmelding)
 * deposits the §6.5 inconsistency of a division by leeg and fails, C (aanmelding 0) faults. Under rustig (none) the
 * list A, B stops at A and B deposits nothing, while B, A deposits; under volledig (all) both lists deposit; A, C
 * under none plays and C, A faults. Krap and bezet over C alone settle on the empty prefix (at most 1 over one
 * contributor, at least 2 out of reach) and read nothing, so they neither fault nor deposit. Keurbaar (all wachten
 * whose quotient exceeds 2 are gediplomeerd) over D (10 by 1, not gediplomeerd) and B: the filter of every member is
 * read before the matrix, so B's filter deposits although D's matrix settles the fold.
 *
 * <p>The documents fix the order of the forms, not the order in which the agenda fires the generated rules, and in the
 * orders of the documents W2 need not pass through the transient bevoegdheid at all. So per quantifier the generated
 * rules of its document fire in 32 sampled orders over P1 of case B: the verdict is the fixpoint's in every one, and in
 * some W2 was bevoegd on the way.
 *
 * <p><b>Measured</b> (2026-10-09, on the documents of this class, every order; the verifier passes all 136). The
 * reference interpreter's runner passes every case the branch plays, in every order, and faults "EVALUATION FAULT:
 * division by numeric zero" on exactly the six the branch faults on (rustig over C, A; volledig over both lists; each
 * in both orders). The ASP runner passes the 110 documents without a leeg or zero divisor; on the other 26 it neither
 * stops short nor keys a deposit on the fold's subject: it faults "viol("division by numeric zero","C")" on all twelve
 * documents with C, also where the verdict stands before C (rustig over A, C; krap and bezet over C alone), and it
 * deposits on the member, "… × B", also where the matrix does not reach B (rustig over A, B; krap over B alone).
 *
 * <p><b>Red</b> on deeaaa26: all six tests, every play "Excluded: tms-transpiler SKIP: regel: … — een
 * QuantifierFold in de voorwaarde ('…'): buiten de proef van de truth-maintenance-tak", the sampled orders on the
 * build of their first document with the same sentence for sterkte. <b>Red by mutation</b> (not committed): with the
 * stop triggers taken out of the replay, the two tests of the short circuit fail.
 */
class TmsQuantifierFoldWitnessTest {

    static final String MODEL = TmsDocuments.model(List.of(
            TmsDocuments.objecttype("Ploeg", List.of(), List.of("sterk", "zwak", "bezet", "krap", "paraat",
                    "gekwalificeerd", "rustig", "volledig", "keurbaar")),
            TmsDocuments.objecttype("Wacht", List.of(attribuut("dienstjaren", WHOLE), attribuut("klachten", WHOLE),
                    attribuut("opgave", WHOLE), attribuut("punten", WHOLE), attribuut("aanmelding", WHOLE),
                    attribuut("deelnemers", WHOLE)), List.of("geschorst", "bevoegd", "gediplomeerd"))),
            List.of(factTypeOneToMany("ploegindeling", "ploeg", "Ploeg", "wacht", "Wacht", "wachten")));

    /** A quantifier fold over the Wachten of the Ploeg; a threshold of 0 writes none (all, none). */
    static String quantifierFold(String quantifier, int threshold, String filter, String test, String bron) {
        return "{\"node\":\"QuantifierFold\",\"quantifier\":\"" + quantifier + "\""
                + (threshold > 0 ? ",\"threshold\":" + threshold : "") + ",\"elementType\":\"Wacht\""
                + (filter == null ? "" : ",\"filter\":" + filter) + ",\"test\":" + test + ","
                + sourceRole("ploegindeling", "wacht") + ",\"source\":{\"snippet\":\"" + bron + "\"}}";
    }

    static final String SCHORSING = characteristicRule("schorsing", "Wacht", "geschorst",
            comparison("greater", reading("klachten", "Wacht"), number("2")));
    static final String BEVOEGDHEID = characteristicRule("bevoegdheid", "Wacht", "bevoegd", compound("all", 0, List.of(
            comparison("greater", reading("dienstjaren", "Wacht"), number("5")), check("geschorst", true))));
    static final String POINTS = computingRule("punten", "BEREKENING", "Wacht", "punten", reading("opgave", "Wacht"),
            "null");
    static final String DEELNEMERS = computingRule("deelnemers", "BEREKENING", "Wacht", "deelnemers",
            reading("aanmelding", "Wacht"), "null");

    /** The five quantifiers over "hij is bevoegd": the Ploeg's characteristic, the rule, the fold. */
    static final List<List<String>> QUANTIFIERS = List.of(
            List.of("sterk", characteristicRule("sterkte", "Ploeg", "sterk", quantifierFold("all", 0, null, check("bevoegd", false),
                    "alle wachten van de Ploeg bevoegd zijn")), "all"),
            List.of("zwak", characteristicRule("zwakte", "Ploeg", "zwak", quantifierFold("none", 0, null, check("bevoegd", false),
                    "geen van de wachten van de Ploeg bevoegd is")), "none"),
            List.of("bezet", characteristicRule("bezetting", "Ploeg", "bezet", quantifierFold("atLeast", 2, null,
                    check("bevoegd", false), "ten minste 2 van de wachten van de Ploeg bevoegd zijn")), "atLeast"),
            List.of("krap", characteristicRule("krapte", "Ploeg", "krap", quantifierFold("atMost", 1, null, check("bevoegd", false),
                    "ten hoogste 1 van de wachten van de Ploeg bevoegd is")), "atMost"),
            List.of("paraat", characteristicRule("paraatheid", "Ploeg", "paraat", quantifierFold("exactly", 1, null,
                    check("bevoegd", false), "precies 1 van de wachten van de Ploeg bevoegd is")), "exactly"));

    static final String QUALIFIED = characteristicRule("kwalificatie", "Ploeg", "gekwalificeerd", quantifierFold("all", 0,
            check("bevoegd", false), comparison("greater", reading("punten", "Wacht"), number("5")),
            "de punten van alle wachten van de Ploeg die bevoegd zijn groter is dan 5"));

    /** "zijn opgave gedeeld door zijn deelnemers is groter dan 2", a new node on every call. */
    static String perParticipant() {
        return comparison("greater", TmsFilterErrorWitnessTest.quotient(reading("opgave", "Wacht"),
                reading("deelnemers", "Wacht")), number("2"));
    }

    static final String CALM = characteristicRule("rustigheid", "Ploeg", "rustig", quantifierFold("none", 0, null,
            perParticipant(), "geen van de wachten van de Ploeg aan de volgende voorwaarde voldoet: zijn opgave gedeeld door"
                    + " zijn deelnemers is groter dan 2"));
    static final String COMPLETE = characteristicRule("volledigheid", "Ploeg", "volledig", quantifierFold("all", 0, null,
            perParticipant(), "alle wachten van de Ploeg aan de volgende voorwaarde voldoen: zijn opgave gedeeld door zijn"
                    + " deelnemers is groter dan 2"));
    static final String KRAP_PER_PARTICIPANT = characteristicRule("krapte", "Ploeg", "krap", quantifierFold("atMost", 1, null,
            perParticipant(), "ten hoogste 1 van de wachten van de Ploeg aan de volgende voorwaarde voldoet: zijn opgave"
                    + " gedeeld door zijn deelnemers is groter dan 2"));
    static final String BEZET_PER_PARTICIPANT = characteristicRule("bezetting", "Ploeg", "bezet", quantifierFold("atLeast", 2,
            null, perParticipant(), "ten minste 2 van de wachten van de Ploeg aan de volgende voorwaarde voldoen: zijn"
                    + " opgave gedeeld door zijn deelnemers is groter dan 2"));
    static final String CHECKABLE = characteristicRule("keurbaarheid", "Ploeg", "keurbaar", quantifierFold("all", 0,
            perParticipant(), check("gediplomeerd", false), "alle wachten van de Ploeg waarvan de opgave gedeeld door de"
                    + " deelnemers groter is dan 2, gediplomeerd zijn"));

    static String guard(String label, int dienstjaren, int complaints, int input) {
        return instanceWithRoles(label, "Wacht", Map.of("dienstjaren", "" + dienstjaren, "klachten", "" + complaints,
                "opgave", "" + input), List.of(), Map.of());
    }

    /** A Wacht with an opgave and, where not null, an aanmelding; gediplomeerd or not. */
    static String melder(String label, int input, Integer aanmelding, boolean gediplomeerd) {
        return instanceWithRoles(label, "Wacht", aanmelding == null ? Map.of("opgave", "" + input)
                        : Map.of("opgave", "" + input, "aanmelding", "" + aanmelding),
                gediplomeerd ? List.of("gediplomeerd") : List.of(), Map.of());
    }

    static String ploeg(String label, List<String> wachten) {
        return instanceWithRoles(label, "Ploeg", Map.of(), List.of(),
                wachten.isEmpty() ? Map.of() : Map.of("wachten", wachten));
    }

    static List<String> worldA() {
        return List.of(ploeg("P1", List.of("W1", "W2")), ploeg("P2", List.of("W3", "W4")), ploeg("P3", List.of()),
                ploeg("P4", List.of("W5")), guard("W1", 8, 0, 7), guard("W2", 9, 3, 2), guard("W3", 6, 1, 6),
                guard("W4", 7, 0, 3), guard("W5", 2, 5, 9));
    }

    static List<String> worldB() {
        return List.of(ploeg("P1", List.of("W1", "W2", "W6")), ploeg("P2", List.of("W7")), guard("W1", 8, 0, 7),
                guard("W2", 9, 3, 2), guard("W6", 10, 0, 8), guard("W7", 9, 3, 2));
    }

    /** The verdict of each Ploeg in world A and in world B, per quantifier, in the order of {@link #QUANTIFIERS}. */
    static final Map<String, List<Boolean>> OUTCOME_A = Map.of("sterk", List.of(false, true, true, false),
            "zwak", List.of(false, false, true, true), "bezet", List.of(false, true, false, false),
            "krap", List.of(true, false, true, true), "paraat", List.of(true, false, false, false),
            "gekwalificeerd", List.of(true, false, true, true));
    static final Map<String, List<Boolean>> OUTCOME_B = Map.of("sterk", List.of(false, false),
            "zwak", List.of(false, true), "bezet", List.of(true, false), "krap", List.of(false, true),
            "paraat", List.of(false, false), "gekwalificeerd", List.of(true, true));

    static JsonNode caseDocument(List<String> world, String kenmerk, List<Boolean> outcome) throws Exception {
        List<String> verwachtingen = new ArrayList<>();
        for (int i = 0; i < outcome.size(); i++) verwachtingen.add(verwacht("P" + (i + 1), kenmerk, outcome.get(i)));
        verwachtingen.add(verwacht("W1", "bevoegd", true));
        verwachtingen.add(verwacht("W2", "geschorst", true));
        verwachtingen.add(verwacht("W2", "bevoegd", false));
        verwachtingen.add(consistent());
        return TmsDocuments.caseDocument(world, verwachtingen);
    }

    @Test void every_quantifier_over_a_characteristic_that_later_lapses() throws Exception {
        List<String> outcomes = new ArrayList<>(), verwacht = new ArrayList<>();
        for (List<String> k : QUANTIFIERS) {
            String kenmerk = k.get(0);
            for (List<String> order : TmsDocuments.permutations(List.of(SCHORSING, BEVOEGDHEID, k.get(1)))) {
                List<String> names = TmsDocuments.ruleNames(order);
                Path ir = TmsDocuments.document(MODEL, order);
                for (JsonNode caseDocument : List.of(caseDocument(worldA(), kenmerk, OUTCOME_A.get(kenmerk)),
                        caseDocument(worldB(), kenmerk, OUTCOME_B.get(kenmerk)))) {
                    outcomes.add(k.get(2) + " " + names + " "
                            + TmsFaultWitnessTest.play(ir, caseDocument, DroolsBackend.Branch.EXEC_MODEL));
                    verwacht.add(k.get(2) + " " + names + " gespeeld");
                }
            }
        }
        assertEquals(verwacht, outcomes);
    }

    @Test void a_filter_and_a_matrix_over_later_derived_values() throws Exception {
        List<String> outcomes = new ArrayList<>(), verwacht = new ArrayList<>();
        for (List<String> order : TmsDocuments.permutations(List.of(SCHORSING, BEVOEGDHEID, POINTS, QUALIFIED))) {
            List<String> names = TmsDocuments.ruleNames(order);
            Path ir = TmsDocuments.document(MODEL, order);
            for (JsonNode caseDocument : List.of(caseDocument(worldA(), "gekwalificeerd", OUTCOME_A.get("gekwalificeerd")),
                    caseDocument(worldB(), "gekwalificeerd", OUTCOME_B.get("gekwalificeerd")))) {
                outcomes.add(names + " " + TmsFaultWitnessTest.play(ir, caseDocument, DroolsBackend.Branch.EXEC_MODEL));
                verwacht.add(names + " gespeeld");
            }
        }
        assertEquals(verwacht, outcomes);
    }

    @Test void a_characteristic_that_lapses_intermediate_does_not_count_in_every_order_of_the_generated_rules() throws Exception {
        for (List<String> k : QUANTIFIERS) {
            String kenmerk = k.get(0);
            DroolsBackend.Build b = DroolsBackend.build(TmsDocuments.document(MODEL,
                    List.of(SCHORSING, BEVOEGDHEID, k.get(1))), DroolsBackend.Branch.EXEC_MODEL);
            int intermediate = 0;
            for (List<Rule> order : TmsDocuments.sample(b.execModelTranspiler().model().getRules(), 20261009L, 30)) {
                List<String> names = order.stream().map(Rule::getName).toList();
                Object p1 = TmsDocuments.beanObject(b.beanLoader(), "Ploeg", Map.of(), List.of());
                Object w1 = TmsDocuments.beanObject(b.beanLoader(), "Wacht", Map.of("dienstjaren", "8", "klachten", "0"), List.of());
                Object w2 = TmsDocuments.beanObject(b.beanLoader(), "Wacht", Map.of("dienstjaren", "9", "klachten", "3"), List.of());
                Object w6 = TmsDocuments.beanObject(b.beanLoader(), "Wacht", Map.of("dienstjaren", "10", "klachten", "0"),
                        List.of());
                for (Object w : List.of(w1, w2, w6)) setRole(w, "ploeg", p1);
                int[] bevoegd = {0};
                TmsDocuments.vuur(b, order, List.of(p1, w1, w2, w6), sessie -> sessie.addEventListener(
                        new DefaultRuleRuntimeEventListener() {
                            @Override public void objectInserted(ObjectInsertedEvent e) {
                                if (e.getObject() instanceof HasCharacteristic h && h.onderwerp() == w2
                                        && h.kenmerk().naam().equals("bevoegd")) bevoegd[0]++;
                            }
                        }), sessie -> { });
                assertEquals(OUTCOME_B.get(kenmerk).get(0), TmsDocuments.has(p1, kenmerk),
                        () -> kenmerk + " van P1 in volgorde " + names);
                assertEquals(false, TmsDocuments.has(w2, "bevoegd"), () -> "W2 in volgorde " + names);
                if (bevoegd[0] > 0) intermediate++;
            }
            assertTrue(intermediate > 0, "geen volgorde liet W2 onderweg bevoegd worden onder " + k.get(2));
        }
    }

    private static void setRole(Object beanObject, String rol, Object doel) throws Exception {
        for (java.lang.reflect.Method m : beanObject.getClass().getMethods())
            if (m.getName().equals(dev.uchar.regelspraak.transpiler.kern.Namen.setter(rol)) && m.getParameterCount() == 1) {
                m.invoke(beanObject, doel);
                return;
            }
        throw new AssertionError("geen setter voor " + rol + " op " + beanObject.getClass());
    }

    static final String A = melder("A", 10, 1, false), B = melder("B", 10, null, false),
            C = melder("C", 10, 0, false), D = melder("D", 10, 1, false);

    /** The fault of a division by zero in the condition of {@code regel}. */
    static String division(String regel) {
        return "DelingDoorNul: deling door nul in de regel \"" + regel + "\": 10 gedeeld door 0 — een numerieke nul als"
                + " deler is een fout, geen leeg (RUNTIME-SEMANTICS ch. 1.4, punt 4)";
    }

    /** {@code regel} beside the derivation of the deelnemers, in both orders, over P1 with {@code leden} in that order
     *  (and the members' instances {@code instanties}), with the expectations {@code verwachtingen}; each outcome is
     *  {@code uitkomst}. */
    private static void playBoth(String regel, List<String> members, List<String> instanties, List<String> verwachtingen,
                                   String outcome, List<String> outcomes, List<String> verwacht) throws Exception {
        for (List<String> order : TmsDocuments.permutations(List.of(DEELNEMERS, regel))) {
            List<String> names = TmsDocuments.ruleNames(order);
            Path ir = TmsDocuments.document(MODEL, order);
            List<String> world = new ArrayList<>(List.of(ploeg("P1", members)));
            world.addAll(instanties);
            JsonNode caseDocument = TmsDocuments.caseDocument(world, verwachtingen);
            outcomes.add(names + " " + members + " " + TmsFaultWitnessTest.play(ir, caseDocument,
                    DroolsBackend.Branch.EXEC_MODEL));
            verwacht.add(names + " " + members + " " + outcome);
        }
    }

    @Test void the_matrix_deposits_only_for_a_member_that_reaches_the_short_circuit() throws Exception {
        List<String> outcomes = new ArrayList<>(), verwacht = new ArrayList<>();
        playBoth(CALM, List.of("A", "B"), List.of(A, B), List.of(verwacht("P1", "rustig", false), consistent()),
                "gespeeld", outcomes, verwacht);
        playBoth(CALM, List.of("B", "A"), List.of(A, B), List.of(verwacht("P1", "rustig", false),
                inconsistent("rustigheid", "P1")), "gespeeld", outcomes, verwacht);
        playBoth(COMPLETE, List.of("A", "B"), List.of(A, B), List.of(verwacht("P1", "volledig", false),
                inconsistent("volledigheid", "P1")), "gespeeld", outcomes, verwacht);
        playBoth(COMPLETE, List.of("B", "A"), List.of(A, B), List.of(verwacht("P1", "volledig", false),
                inconsistent("volledigheid", "P1")), "gespeeld", outcomes, verwacht);
        assertEquals(verwacht, outcomes);
    }

    @Test void the_matrix_fails_only_for_a_member_that_reaches_the_short_circuit() throws Exception {
        List<String> outcomes = new ArrayList<>(), verwacht = new ArrayList<>();
        playBoth(CALM, List.of("A", "C"), List.of(A, C), List.of(verwacht("P1", "rustig", false), consistent()),
                "gespeeld", outcomes, verwacht);
        playBoth(CALM, List.of("C", "A"), List.of(A, C), List.of(consistent()), division("rustigheid"), outcomes,
                verwacht);
        playBoth(COMPLETE, List.of("A", "C"), List.of(A, C), List.of(consistent()), division("volledigheid"), outcomes,
                verwacht);
        playBoth(COMPLETE, List.of("C", "A"), List.of(A, C), List.of(consistent()), division("volledigheid"), outcomes,
                verwacht);
        // the empty prefix (ch. 5.10): at most 1 over one contributor cannot be exceeded, at least 2 is out of reach
        playBoth(KRAP_PER_PARTICIPANT, List.of("C"), List.of(C), List.of(verwacht("P1", "krap", true), consistent()),
                "gespeeld", outcomes, verwacht);
        playBoth(BEZET_PER_PARTICIPANT, List.of("C"), List.of(C), List.of(verwacht("P1", "bezet", false), consistent()),
                "gespeeld", outcomes, verwacht);
        playBoth(KRAP_PER_PARTICIPANT, List.of("B"), List.of(B), List.of(verwacht("P1", "krap", true), consistent()),
                "gespeeld", outcomes, verwacht);
        assertEquals(verwacht, outcomes);
    }

    @Test void the_filter_of_each_member_is_read_for_the_matrix() throws Exception {
        List<String> outcomes = new ArrayList<>(), verwacht = new ArrayList<>();
        for (List<String> members : List.of(List.of("D", "B"), List.of("B", "D")))
            playBoth(CHECKABLE, members, List.of(D, B), List.of(verwacht("P1", "keurbaar", false),
                    inconsistent("keurbaarheid", "P1")), "gespeeld", outcomes, verwacht);
        // without B the filter deposits nothing, and D alone decides
        playBoth(CHECKABLE, List.of("D"), List.of(D), List.of(verwacht("P1", "keurbaar", false), consistent()),
                "gespeeld", outcomes, verwacht);
        assertEquals(verwacht, outcomes);
    }
}
