package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static dev.uchar.regelspraak.cases.TmsDocuments.WHOLE;
import static dev.uchar.regelspraak.cases.TmsDocuments.COUNTING;
import static dev.uchar.regelspraak.cases.TmsDocuments.aggregation;
import static dev.uchar.regelspraak.cases.TmsDocuments.attribuut;
import static dev.uchar.regelspraak.cases.TmsDocuments.sourceRole;
import static dev.uchar.regelspraak.cases.TmsDocuments.check;
import static dev.uchar.regelspraak.cases.TmsDocuments.consistent;
import static dev.uchar.regelspraak.cases.TmsDocuments.factTypeOneToMany;
import static dev.uchar.regelspraak.cases.TmsDocuments.number;
import static dev.uchar.regelspraak.cases.TmsDocuments.inconsistent;
import static dev.uchar.regelspraak.cases.TmsDocuments.instanceWithRoles;
import static dev.uchar.regelspraak.cases.TmsDocuments.characteristicRule;
import static dev.uchar.regelspraak.cases.TmsDocuments.ownRead;
import static dev.uchar.regelspraak.cases.TmsDocuments.predicate;
import static dev.uchar.regelspraak.cases.TmsDocuments.computingRule;
import static dev.uchar.regelspraak.cases.TmsDocuments.roleBinding;
import static dev.uchar.regelspraak.cases.TmsDocuments.compound;
import static dev.uchar.regelspraak.cases.TmsDocuments.comparison;
import static dev.uchar.regelspraak.cases.TmsDocuments.verwacht;
import static dev.uchar.regelspraak.cases.TmsDocuments.waarde;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The §6.5 deposit of a fold's filter on the exec-model branch. The reference interpreter evaluates a fold's
 * filter per member of the fold's range, only where the evaluation of the form that reads the fold reaches the fold
 * (RUNTIME-SEMANTICS ch. 5.10, 5.11), with that form's collector of deposits: a filter that divides a filled dividend
 * by a leeg divisor deposits a §9.5 inconsistency under the reading form and its subject, the fold's subject (ch. 1.2,
 * 5.6). Fixround 1 of the card, continued: until then the filter of a fold deposited nothing on this branch, and the
 * second case of {@code jutterswet-filterwacht} with a Vondst without deelnemers played consistent where the reference
 * interpreter answers "zware vondsten × J2; drukte × J2".
 *
 * <p>The model: a Jutter reports Vondsten (aanmelding: aanmelder, one Jutter; vondst, many Vondsten); a Vondst has
 * gewicht and opgave as input and "De deelnemers van een Vondst moet berekend worden als zijn opgave", so the divisor
 * of every filter is a value derived later in some orders of the rules. The readers, in every order of the four rules:
 * "De zware vondsten van een Jutter moet berekend worden als het aantal vondsten van de Jutter die aan de volgende
 * voorwaarde voldoen: zijn gewicht gedeeld door zijn deelnemers is groter dan 2, indien hij actief is" (the fold in a
 * value), "Een Jutter is druk indien hij aan alle volgende voorwaarden voldoet: hij is actief; dat aantal is groter
 * dan 0" (the fold in a condition, after the member that settles the composite where he is not actief), and "De
 * zorgvuldige vondsten van een Jutter moet berekend worden als het aantal vondsten van de Jutter die aan alle volgende
 * voorwaarden voldoen: zijn deelnemers is gevuld; zijn gewicht gedeeld door zijn deelnemers is groter dan 2" (a filter
 * whose own composite settles before its division where the deelnemers are leeg).
 *
 * <ul>
 *   <li>Case 1: J1, not actief, with V1 (gewicht 10, opgave 2) and V2 (gewicht 9, no opgave), and V9 (gewicht 5, no
 *       opgave) in no Jutter's range. No reader reaches a division by leeg: the value's condition does not hold, the
 *       composite stops after "hij is actief", the careful filter stops after "gevuld", and V9 is in no range.
 *       Consistent; zorgvuldige vondsten 1.</li>
 *   <li>Case 2: J1 as in case 1, and J2, actief, with V3 (gewicht 10, no opgave) and V4 (gewicht 3, opgave 3). The
 *       readers of J2 reach V3's 10 by leeg: zware vondsten × J2 and drukte × J2, and nothing for J1.</li>
 *   <li>Case 3: J2 with V3 (gewicht 10, opgave 2) and V4 as in case 2: no deposit at the fixpoint; in an order where a
 *       reader fires before the deelnemers are derived, the deposit of the transient leeg goes again.</li>
 * </ul>
 *
 * <p>Two more documents: a count over an instance-set union whose first selection's filter divides (ch. 3.4d, a
 * contribution rule per selection), and the fold whose filter faults for one member and deposits for another, in
 * both list orders: the reference interpreter stops at the fault where it comes first and deposits before it where it
 * comes second, and the session faults either way, so the outcome is the fault.
 *
 * <p>The argument of a fold over a set source holds no place of its own: a division there is ill-formed (WF-037), and
 * the verifier, the reference interpreter, the ASP runner and the loader of every Drools branch refuse it with the
 * sentence "a sum aggregation carries elementType "Vondst" and a Binary operand: the source of an instance fold is an
 * AttributeReference" (measured 2026-10-09).
 *
 * <p><b>Measured</b> (2026-10-09, on the documents of this class, every order; the verifier passes all of them). The
 * reference interpreter's runner passes cases 1 and 2 in all 24 orders, its channel in case 2 exactly "zware vondsten ×
 * J2; drukte × J2" (measured with the expectation consistent); case 3 in the 12 orders where the deelnemers rule comes
 * before the zware vondsten rule, and in the other 12 it keeps "zware vondsten × J2", made while the deelnemers were
 * still leeg: the deposit of an intermediate state, which the owner's decision of 2026-10-09 (PLAN §9) does not count.
 * The union: its channel exactly "unietelling × J2" in both orders; the case without a deposit passes where the
 * deelnemers rule comes first and keeps "unietelling × J2" of the intermediate state in the other order. The fault:
 * "EVALUATION FAULT: division by numeric zero" in all four. The ASP runner passes cases 1 and 3 in every order and
 * faults on the fault document ("viol("division by numeric zero","V3")"), but deposits a filter's division on the
 * member instead of the fold's subject, and also where the reading form does not reach the fold: case 2 "expected
 * inconsistent (… "drukte" on "J2"), but the model carries: drukte × V2; drukte × V3; zware vondsten × V2; zware
 * vondsten × V3", the union "unietelling × V3".
 *
 * <p><b>Red</b> on ba8127f2: case 2 in all 24 orders, "case: verwacht inconsistent (zware vondsten @ J2), werkelijk
 * consistent; verwacht inconsistent (drukte @ J2), werkelijk consistent", and the union with a deposit in both orders,
 * "case: verwacht inconsistent (unietelling @ J2), werkelijk consistent". Cases 1 and 3 and the fault document were
 * green before: they pin what stays.
 */
class TmsFilterDepositWitnessTest {

    static final String MODEL = TmsDocuments.model(List.of(
            TmsDocuments.objecttype("Jutter", List.of(attribuut("zware vondsten", WHOLE),
                    attribuut("zorgvuldige vondsten", WHOLE), attribuut("unietelling", WHOLE)), List.of("actief", "druk")),
            TmsDocuments.objecttype("Vondst", List.of(attribuut("gewicht", WHOLE), attribuut("opgave", WHOLE),
                    attribuut("deelnemers", WHOLE)), List.of())),
            List.of(factTypeOneToMany("aanmelding", "aanmelder", "Jutter", "vondst", "Vondst", "vondsten")));

    private static final String VONDSTEN = "{\"node\":\"AttributeReference\",\"attribute\":\"vondsten\",\"subjectType\":"
            + "\"Jutter\",\"source\":{\"snippet\":\"vondsten van de Jutter\"}}";

    /** "zijn gewicht gedeeld door zijn deelnemers is groter dan 2", a new node on every call. */
    private static String zwaarPerParticipant() {
        return comparison("greater", TmsFilterFaultWitnessTest.quotient(ownRead("gewicht", "Vondst"),
                ownRead("deelnemers", "Vondst")), number("2"));
    }

    /** The fold of zware vondsten and drukte, a new node on every call, so each form reads a fold of its own. */
    private static String zwaar() {
        return aggregation("count", VONDSTEN, "Vondst", sourceRole("aanmelding", "vondst"), zwaarPerParticipant(), "null",
                COUNTING, "het aantal vondsten van de Jutter die aan de volgende voorwaarde voldoen: zijn gewicht gedeeld"
                        + " door zijn deelnemers is groter dan 2");
    }

    private static String zorgvuldig() {
        return aggregation("count", VONDSTEN, "Vondst", sourceRole("aanmelding", "vondst"), compound("all", 0, List.of(
                predicate("filled", ownRead("deelnemers", "Vondst")), zwaarPerParticipant())), "null", COUNTING,
                "het aantal vondsten van de Jutter die aan alle volgende voorwaarden voldoen: zijn deelnemers is gevuld;"
                        + " zijn gewicht gedeeld door zijn deelnemers is groter dan 2");
    }

    /** One selection of the union, over the Jutter's vondsten. */
    private static String selection(String filter, String bron) {
        return "{\"node\":\"InstanceSetBranch\",\"elementType\":\"Vondst\",\"sourceRole\":" + roleBinding("aanmelding",
                "vondst") + ",\"filter\":" + filter + ",\"source\":{\"snippet\":\"" + bron + "\"}}";
    }

    /** "het aantal vondsten die zwaar zijn per deelnemer en vondsten die zwaarder zijn dan 8". */
    private static String union() {
        return "{\"node\":\"Aggregation\",\"kind\":\"count\",\"arguments\":[{\"node\":\"InstanceSetUnion\",\"branches\":["
                + selection(zwaarPerParticipant(), "vondsten van de Jutter die zwaar zijn per deelnemer") + ","
                + selection(comparison("greater", ownRead("gewicht", "Vondst"), number("8")),
                        "vondsten van de Jutter die zwaarder zijn dan 8")
                + "],\"source\":{\"snippet\":\"vondsten die zwaar zijn per deelnemer en vondsten die zwaarder zijn dan"
                + " 8\"}}],\"type\":" + COUNTING + ",\"source\":{\"snippet\":\"het aantal vondsten die zwaar zijn per"
                + " deelnemer en vondsten die zwaarder zijn dan 8\"}}";
    }

    static final String DEELNEMERS = computingRule("deelnemers van een vondst", "BEREKENING", "Vondst", "deelnemers",
            ownRead("opgave", "Vondst"), "null");
    static final String ZWARE = computingRule("zware vondsten", "BEREKENING", "Jutter", "zware vondsten", zwaar(),
            check("actief", false));
    static final String DRUKTE = characteristicRule("drukte", "Jutter", "druk", compound("all", 0, List.of(
            check("actief", false), comparison("greater", zwaar(), number("0")))));
    static final String ZORGVULDIG = computingRule("zorgvuldige vondsten", "BEREKENING", "Jutter", "zorgvuldige vondsten",
            zorgvuldig(), "null");
    static final String UNION = computingRule("unietelling", "BEREKENING", "Jutter", "unietelling", union(), "null");

    /** A Vondst with {@code gewicht} and, where not null, {@code opgave}. */
    private static String vondst(String label, String weight, String input) {
        return instanceWithRoles(label, "Vondst", input == null ? Map.of("gewicht", weight)
                : Map.of("gewicht", weight, "opgave", input), List.of(), Map.of());
    }

    private static String jutter(String label, boolean actief, List<String> vondsten) {
        return instanceWithRoles(label, "Jutter", Map.of(), actief ? List.of("actief") : List.of(),
                Map.of("vondsten", vondsten));
    }

    static JsonNode case1() throws Exception {
        return TmsDocuments.caseDocument(List.of(jutter("J1", false, List.of("V1", "V2")), vondst("V1", "10", "2"),
                vondst("V2", "9", null), vondst("V9", "5", null)),
                List.of(waarde("J1", "zware vondsten", null), verwacht("J1", "druk", false),
                        waarde("J1", "zorgvuldige vondsten", "1"), consistent()));
    }

    static JsonNode case2() throws Exception {
        return TmsDocuments.caseDocument(List.of(jutter("J1", false, List.of("V1", "V2")), jutter("J2", true, List.of("V3", "V4")),
                vondst("V1", "10", "2"), vondst("V2", "9", null), vondst("V3", "10", null), vondst("V4", "3", "3")),
                List.of(waarde("J1", "zware vondsten", null), verwacht("J1", "druk", false),
                        waarde("J1", "zorgvuldige vondsten", "1"), waarde("J2", "zware vondsten", "0"),
                        verwacht("J2", "druk", false), waarde("J2", "zorgvuldige vondsten", "0"),
                        inconsistent("zware vondsten", "J2"), inconsistent("drukte", "J2")));
    }

    static JsonNode case3() throws Exception {
        return TmsDocuments.caseDocument(List.of(jutter("J2", true, List.of("V3", "V4")), vondst("V3", "10", "2"),
                vondst("V4", "3", "3")),
                List.of(waarde("J2", "zware vondsten", "1"), verwacht("J2", "druk", true),
                        waarde("J2", "zorgvuldige vondsten", "1"), consistent()));
    }


    @Test void a_filter_deposits_under_the_form_that_the_fold_reaches() throws Exception {
        List<String> outcomes = new ArrayList<>(), verwacht = new ArrayList<>();
        for (List<String> order : TmsDocuments.permutations(List.of(DEELNEMERS, ZWARE, DRUKTE, ZORGVULDIG))) {
            List<String> names = TmsDocuments.ruleNames(order);
            Path ir = TmsDocuments.document(MODEL, order);
            for (JsonNode caseDocument : List.of(case1(), case2(), case3())) {
                outcomes.add(names + " " + TmsFaultWitnessTest.play(ir, caseDocument, DroolsBackend.Branch.EXEC_MODEL));
                verwacht.add(names + " gespeeld");
            }
        }
        assertEquals(verwacht, outcomes);
    }

    @Test void the_filter_of_a_selection_of_a_union_also_deposits() throws Exception {
        JsonNode withDeposit = TmsDocuments.caseDocument(List.of(jutter("J2", true, List.of("V3", "V4")),
                vondst("V3", "10", null), vondst("V4", "3", "3")),
                List.of(waarde("J2", "unietelling", "1"), inconsistent("unietelling", "J2")));
        JsonNode without = TmsDocuments.caseDocument(List.of(jutter("J2", true, List.of("V3", "V4")),
                vondst("V3", "10", "2"), vondst("V4", "3", "3")), List.of(waarde("J2", "unietelling", "1"), consistent()));
        List<String> outcomes = new ArrayList<>(), verwacht = new ArrayList<>();
        for (List<String> order : TmsDocuments.permutations(List.of(DEELNEMERS, UNION))) {
            List<String> names = TmsDocuments.ruleNames(order);
            Path ir = TmsDocuments.document(MODEL, order);
            for (JsonNode caseDocument : List.of(withDeposit, without)) {
                outcomes.add(names + " " + TmsFaultWitnessTest.play(ir, caseDocument, DroolsBackend.Branch.EXEC_MODEL));
                verwacht.add(names + " gespeeld");
            }
        }
        assertEquals(verwacht, outcomes);
    }

    @Test void a_filter_that_fails_for_a_member_and_for_another_deposits_fails() throws Exception {
        String fout = "DelingDoorNul: deling door nul in de regel \"zware vondsten\": 10 gedeeld door 0 — een numerieke"
                + " nul als deler is een fout, geen leeg (RUNTIME-SEMANTICS ch. 1.4, punt 4)";
        List<String> outcomes = new ArrayList<>(), verwacht = new ArrayList<>();
        for (List<String> order : TmsDocuments.permutations(List.of(DEELNEMERS, ZWARE))) {
            List<String> names = TmsDocuments.ruleNames(order);
            Path ir = TmsDocuments.document(MODEL, order);
            for (List<String> members : List.of(List.of("V3", "V4"), List.of("V4", "V3"))) {
                JsonNode caseDocument = TmsDocuments.caseDocument(List.of(jutter("J2", true, members), vondst("V3", "10", "0"),
                        vondst("V4", "3", null)), List.of(consistent()));
                outcomes.add(names + " " + members + " " + TmsFaultWitnessTest.play(ir, caseDocument,
                        DroolsBackend.Branch.EXEC_MODEL));
                verwacht.add(names + " " + members + " " + fout);
            }
        }
        assertEquals(verwacht, outcomes);
    }
}
