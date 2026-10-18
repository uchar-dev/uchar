package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;

import org.drools.model.Rule;
import org.junit.jupiter.api.Test;
import org.kie.api.event.rule.ObjectDeletedEvent;
import org.kie.api.event.rule.ObjectInsertedEvent;
import org.kie.api.event.rule.ObjectUpdatedEvent;
import org.kie.api.event.rule.RuleRuntimeEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static dev.uchar.regelspraak.cases.TmsDocuments.attribuut;
import static dev.uchar.regelspraak.cases.TmsDocuments.binary;
import static dev.uchar.regelspraak.cases.TmsDocuments.binderRead;
import static dev.uchar.regelspraak.cases.TmsDocuments.consistent;
import static dev.uchar.regelspraak.cases.TmsDocuments.numberType;
import static dev.uchar.regelspraak.cases.TmsDocuments.numberValue;
import static dev.uchar.regelspraak.cases.TmsDocuments.inconsistent;
import static dev.uchar.regelspraak.cases.TmsDocuments.instanceWith;
import static dev.uchar.regelspraak.cases.TmsDocuments.keuze;
import static dev.uchar.regelspraak.cases.TmsDocuments.let;
import static dev.uchar.regelspraak.cases.TmsDocuments.reading;
import static dev.uchar.regelspraak.cases.TmsDocuments.literal;
import static dev.uchar.regelspraak.cases.TmsDocuments.computingRule;
import static dev.uchar.regelspraak.cases.TmsDocuments.comparison;
import static dev.uchar.regelspraak.cases.TmsDocuments.waarde;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A {@code Let} (RUNTIME-SEMANTICS ch. 10.6), its {@code BinderRef} and an {@code IfThenElse} (ch. 10.2) on the
 * exec-model branch, over a binder whose value another rule derives later: "De grondslag van een Jutter moet
 * berekend worden als zijn inkomen" and "De heffing van een Jutter moet berekend worden als: k is zijn grondslag; als k
 * groter is dan 10,00 dan 5,00, anders k gedeeld door zijn restpost". The division in the else branch is a §6.5 place
 * (a leeg divisor gives leeg and a §9.5 inconsistency, ch. 1.2) and a fault place (a numeric zero, ch. 1.4): it is
 * evaluated only where the branch is taken, and the binder is read, and its value evaluated, only where a branch reads
 * it. A read of a binder whose value is leeg ends the {@code Let} with leeg (clause 2).
 *
 * <p>The case input puts a grondslag of 6,00 beside an inkomen of 40,00, so in an order where the heffing fires before
 * the grondslag is derived it reads 6, takes the else branch and divides; at the fixpoint the grondslag is 40 and the
 * then branch writes 5,00. Case A: J1 (that transient, no restpost), J4 (inkomen 6, restpost 3: else, 2,00) and J6
 * (nothing: the grondslag stays leeg, the read raises the barrier, heffing leeg) — the transient deposit of J1 goes
 * again, so the case is consistent. Case B: J3 (inkomen 6, no restpost): else at the fixpoint, heffing leeg with the
 * §9.5 inconsistency of the heffing on J3. Case C: J2 (inkomen 40, grondslag 6, restpost 0): the transient division by
 * zero goes again, heffing 5,00. Case D: J5 (inkomen 6, restpost 0): the division by zero stands at the fixpoint. The
 * second document, with the binder q whose value divides and the nested Let of the saldo, is described at its row.
 *
 * <p><b>Measured</b> (2026-10-09, the reference interpreter's runner and the ASP runner on every order of both
 * documents): where the grondslag rule comes first, the reference interpreter passes cases A, B, C, E and F and faults on
 * D and G, "division by numeric zero". Where the reader comes first it reads the input grondslag 6: it keeps the
 * deposit of J1 ("MISMATCH: expected consistent, but the channel carries: heffing van een jutter × J1") and faults on
 * the transient division of J2 in cases C and E. That is the fault and the deposit of an intermediate state, which the
 * owner's decision of 2026-10-09 (PLAN §9, "Faults and deposits count at the fixpoint") does not count and which the
 * reference interpreter gives up with the card fout-op-tussenstand. The ASP runner passes A, B, C, E and F in every
 * order and faults on D and G ("viol("division by numeric zero", …)").
 *
 * <p><b>Red before the change</b> (measured 2026-10-09 on a copy of the branch at its claim, where the branch refused
 * the forms): every row was refused, "Excluded: tms-transpiler SKIP: regel: heffing van een jutter — een Let in een
 * waarde ('k is …'): buiten de proef van de truth-maintenance-tak", and the tarief as "een Let in een waarde ('q is
 * …')".
 */
class TmsLetWitnessTest {

    private static final String TWO = numberType(false, 2, null);

    static final String MODEL = TmsDocuments.model(List.of(TmsDocuments.objecttype("Jutter", List.of(
            attribuut("inkomen", TWO), attribuut("grondslag", TWO), attribuut("restpost", TWO),
            attribuut("heffing", TWO)), List.of())), List.of());

    static final String BASE = computingRule("grondslag van een jutter", "BEREKENING", "Jutter", "grondslag",
            reading("inkomen", "Jutter"), "null");

    static final String HEFFING = computingRule("heffing van een jutter", "BEREKENING", "Jutter", "heffing",
            let("k", reading("grondslag", "Jutter"), keuze(
                    comparison("greater", binderRead("k"), literal("10.00", TWO)),
                    literal("5.00", TWO),
                    binary("dividedBy", binderRead("k"), reading("restpost", "Jutter"), TWO), TWO), TWO), "null");

    static final List<String> RULES = List.of(BASE, HEFFING);

    static JsonNode caseA() throws Exception {
        return TmsDocuments.caseOn(null, List.of(
                instanceWith("J1", "Jutter", Map.of("inkomen", numberValue("40.00"), "grondslag", numberValue("6.00")),
                        List.of()),
                instanceWith("J4", "Jutter", Map.of("inkomen", numberValue("6.00"), "restpost", numberValue("3.00")),
                        List.of()),
                instanceWith("J6", "Jutter", Map.of(), List.of())),
                List.of(waarde("J1", "heffing", "5.00"), waarde("J4", "heffing", "2.00"), waarde("J6", "heffing", null),
                        consistent()));
    }

    static JsonNode caseB() throws Exception {
        return TmsDocuments.caseOn(null, List.of(
                instanceWith("J3", "Jutter", Map.of("inkomen", numberValue("6.00")), List.of())),
                List.of(waarde("J3", "heffing", null), inconsistent("heffing van een jutter", "J3")));
    }

    static JsonNode caseC() throws Exception {
        return TmsDocuments.caseOn(null, List.of(
                instanceWith("J2", "Jutter", Map.of("inkomen", numberValue("40.00"), "grondslag", numberValue("6.00"),
                        "restpost", numberValue("0.00")), List.of())),
                List.of(waarde("J2", "heffing", "5.00"), consistent()));
    }

    static JsonNode caseD() throws Exception {
        return TmsDocuments.caseOn(null, List.of(
                instanceWith("J5", "Jutter", Map.of("inkomen", numberValue("6.00"), "restpost", numberValue("0.00")),
                        List.of())),
                List.of(consistent()));
    }

    static final String J5_BY_ZERO = "DelingDoorNul: deling door nul in de regel \"heffing van een jutter\": 6 gedeeld"
            + " door 0 — een numerieke nul als deler is een fout, geen leeg (RUNTIME-SEMANTICS ch. 1.4, punt 4)";

    @Test void the_taken_branch_computes_and_the_other_does_not_in_every_order() throws Exception {
        List<String> outcomes = new ArrayList<>(), verwacht = new ArrayList<>();
        for (List<String> order : TmsDocuments.permutations(RULES)) {
            List<String> names = TmsDocuments.ruleNames(order);
            java.nio.file.Path ir = TmsDocuments.document(MODEL, order);
            for (JsonNode caseDocument : List.of(caseA(), caseB(), caseC()))
                outcomes.add(names + " " + TmsFaultWitnessTest.play(ir, caseDocument, DroolsBackend.Branch.EXEC_MODEL));
            outcomes.add(names + " " + TmsFaultWitnessTest.play(ir, caseD(), DroolsBackend.Branch.EXEC_MODEL));
            for (int i = 0; i < 3; i++) verwacht.add(names + " gespeeld");
            verwacht.add(names + " " + J5_BY_ZERO);
        }
        assertEquals(verwacht, outcomes);
    }

    // ---------------- clause 3 and the barrier of a nested Let ----------------

    static final String MODEL_WITH_RATE = TmsDocuments.model(List.of(TmsDocuments.objecttype("Jutter", List.of(
            attribuut("inkomen", TWO), attribuut("grondslag", TWO), attribuut("restpost", TWO),
            attribuut("tarief", TWO), attribuut("saldo", TWO)), List.of())), List.of());

    /** "Het tarief van een Jutter moet berekend worden als: q is zijn inkomen gedeeld door zijn restpost; k is zijn
     *  grondslag; als k groter is dan 10,00 dan 5,00, anders q". */
    static final String RATE = computingRule("tarief van een jutter", "BEREKENING", "Jutter", "tarief",
            let("q", binary("dividedBy", reading("inkomen", "Jutter"), reading("restpost", "Jutter"), TWO),
                    let("k", reading("grondslag", "Jutter"), keuze(
                            comparison("greater", binderRead("k"), literal("10.00", TWO)),
                            literal("5.00", TWO), binderRead("q"), TWO), TWO), TWO), "null");

    /** "Het saldo van een Jutter moet berekend worden als: k is zijn grondslag; (x is zijn restpost; x) plus k". */
    static final String BALANCE = computingRule("saldo van een jutter", "BEREKENING", "Jutter", "saldo",
            let("k", reading("grondslag", "Jutter"),
                    binary("plus", let("x", reading("restpost", "Jutter"), binderRead("x"), TWO), binderRead("k"),
                            TWO), TWO), "null");

    static JsonNode caseE() throws Exception {
        return TmsDocuments.caseOn(null, List.of(
                instanceWith("J2", "Jutter", Map.of("inkomen", numberValue("40.00"), "grondslag", numberValue("6.00"),
                        "restpost", numberValue("0.00")), List.of()),
                instanceWith("J7", "Jutter", Map.of("inkomen", numberValue("40.00")), List.of())),
                List.of(waarde("J2", "tarief", "5.00"), waarde("J2", "saldo", "40.00"), waarde("J7", "tarief", "5.00"),
                        waarde("J7", "saldo", "40.00"), consistent()));
    }

    static JsonNode caseF() throws Exception {
        return TmsDocuments.caseOn(null, List.of(
                instanceWith("J8", "Jutter", Map.of("inkomen", numberValue("6.00")), List.of())),
                List.of(waarde("J8", "tarief", null), waarde("J8", "saldo", "6.00"),
                        inconsistent("tarief van een jutter", "J8")));
    }

    static JsonNode caseG() throws Exception {
        return TmsDocuments.caseOn(null, List.of(
                instanceWith("J5", "Jutter", Map.of("inkomen", numberValue("6.00"), "restpost", numberValue("0.00")),
                        List.of())),
                List.of(consistent()));
    }

    /** Clause 3 of ch. 10.6: the binder q, whose value divides, is read only on the else branch, so where the then
     *  branch is taken at the fixpoint its value is never evaluated: J2 (restpost 0) does not fault, and J7 (no
     *  restpost) does not deposit, although in an order where the tarief fires before the grondslag is derived J2
     *  reads the input grondslag 6, takes the else branch and divides by zero on the way. J8 (inkomen 6, no restpost)
     *  takes the else branch at the fixpoint: q is 6 divided by leeg, leeg with the §9.5 deposit (ch. 1.2), and its read
     *  ends the Let with leeg (clause 2). J5 (inkomen 6, restpost 0) reads q at the fixpoint and faults. The saldo
     *  holds a nested Let whose binder x reads the restpost: where it is leeg the read empties the nested Let alone,
     *  never the enclosing one (clause 2), and "plus" counts the leeg operand as 0 (ch. 1.2): J7 and J8 get their
     *  grondslag. */
    @Test void an_unread_binder_does_not_compute_and_a_nested_barrier_stays_within() throws Exception {
        List<String> outcomes = new ArrayList<>(), verwacht = new ArrayList<>();
        for (List<String> order : TmsDocuments.permutations(List.of(BASE, RATE, BALANCE))) {
            List<String> names = TmsDocuments.ruleNames(order);
            java.nio.file.Path ir = TmsDocuments.document(MODEL_WITH_RATE, order);
            for (JsonNode caseDocument : List.of(caseE(), caseF())) {
                outcomes.add(names + " " + TmsFaultWitnessTest.play(ir, caseDocument, DroolsBackend.Branch.EXEC_MODEL));
                verwacht.add(names + " gespeeld");
            }
            outcomes.add(names + " " + TmsFaultWitnessTest.play(ir, caseG(), DroolsBackend.Branch.EXEC_MODEL));
            verwacht.add(names + " DelingDoorNul: deling door nul in de regel \"tarief van een jutter\": 6 gedeeld door 0"
                    + " — een numerieke nul als deler is een fout, geen leeg (RUNTIME-SEMANTICS ch. 1.4, punt 4)");
        }
        assertEquals(verwacht, outcomes);
    }

    /** Every order of the generated rules, on the beans of case A's J1 and case C's J2: the fixpoint has no fault and no
     *  deposit, and in at least one order the heffing took the else branch on the transient grondslag, divided by a
     *  leeg restpost (a deposit that went again) and by a zero (a fault that went again). */
    @Test void the_intermediate_state_in_every_order_of_the_generated_rules() throws Exception {
        DroolsBackend.Build b = DroolsBackend.build(TmsDocuments.document(MODEL, RULES),
                DroolsBackend.Branch.EXEC_MODEL);
        int orders = 0, deposit = 0, fout = 0;
        for (List<Rule> order : TmsDocuments.permutations(b.execModelTranspiler().model().getRules())) {
            List<String> names = order.stream().map(Rule::getName).toList();
            Object j1 = TmsDocuments.beanObject(b.beanLoader(), "Jutter", Map.of("inkomen", "40.00", "grondslag", "6.00"),
                    List.of());
            Object j2 = TmsDocuments.beanObject(b.beanLoader(), "Jutter",
                    Map.of("inkomen", "40.00", "grondslag", "6.00", "restpost", "0.00"), List.of());
            Facts sessionFacts = new Facts();
            assertNull(TmsDocuments.fixpunt(b, order, List.of(j1, j2), sessie -> sessie.addEventListener(sessionFacts)),
                    () -> "volgorde " + names);
            assertEquals(sessionFacts.inserted, sessionFacts.withdrawn, () -> "foutfeiten en deponeringen in volgorde " + names);
            TmsFaultWitnessTest.assertNumber("5.00", TmsDocuments.lees(j1, "heffing"), "heffing van J1 in volgorde " + names);
            TmsFaultWitnessTest.assertNumber("5.00", TmsDocuments.lees(j2, "heffing"), "heffing van J2 in volgorde " + names);
            if (sessionFacts.depositCount > 0) deposit++;
            if (sessionFacts.fouten > 0) fout++;
            orders++;
        }
        assertTrue(deposit > 0, "geen volgorde deponeerde op de tijdelijke grondslag");
        assertTrue(fout > 0, "geen volgorde deelde door de tijdelijke nul");
        assertEquals(24, orders);
    }

    /** Counts the fault facts and the §9.5 inconsistencies a session inserts and withdraws. */
    static final class Facts implements RuleRuntimeEventListener {
        int inserted, withdrawn, fouten, depositCount;

        @Override public void objectInserted(ObjectInsertedEvent e) {
            String soort = e.getObject().getClass().getSimpleName();
            if (soort.equals("Fout")) { inserted++; fouten++; }
            if (soort.equals("Inconsistentie")) { inserted++; depositCount++; }
        }

        @Override public void objectUpdated(ObjectUpdatedEvent e) {}

        @Override public void objectDeleted(ObjectDeletedEvent e) {
            String soort = e.getOldObject().getClass().getSimpleName();
            if (soort.equals("Fout") || soort.equals("Inconsistentie")) withdrawn++;
        }
    }
}
