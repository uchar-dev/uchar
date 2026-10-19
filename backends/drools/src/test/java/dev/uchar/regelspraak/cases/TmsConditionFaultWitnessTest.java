package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;

import org.drools.model.Rule;
import org.junit.jupiter.api.Test;
import org.kie.api.event.rule.DefaultAgendaEventListener;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static dev.uchar.regelspraak.cases.TmsDocuments.WHOLE;
import static dev.uchar.regelspraak.cases.TmsDocuments.attribute;
import static dev.uchar.regelspraak.cases.TmsDocuments.attributeGroup;
import static dev.uchar.regelspraak.cases.TmsDocuments.check;
import static dev.uchar.regelspraak.cases.TmsDocuments.consistent;
import static dev.uchar.regelspraak.cases.TmsDocuments.consistency;
import static dev.uchar.regelspraak.cases.TmsDocuments.number;
import static dev.uchar.regelspraak.cases.TmsDocuments.instance;
import static dev.uchar.regelspraak.cases.TmsDocuments.characteristicRule;
import static dev.uchar.regelspraak.cases.TmsDocuments.ownRead;
import static dev.uchar.regelspraak.cases.TmsDocuments.computingRule;
import static dev.uchar.regelspraak.cases.TmsDocuments.defaultEntry;
import static dev.uchar.regelspraak.cases.TmsDocuments.expected;
import static dev.uchar.regelspraak.cases.TmsDocuments.comparison;
import static dev.uchar.regelspraak.cases.TmsDocuments.value;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * An evaluation fault in a condition on the exec-model branch. A condition is tested in the left-hand side,
 * where Drools cannot take a fault as a fact: a division by a numeric zero there left the fire as an exception of the
 * engine that named neither the rule nor the division. Its test now answers that the condition does not hold where it
 * faults, and a rule of the form's own inserts the fault logically, so the fault stands while the reads it rested on
 * stand and is reported at the fixpoint, as a fault in a value is ({@link TmsFaultWitnessTest}).
 *
 * <p><b>The device</b> is the square root without a rounding of a number that is no square of a rational
 * ({@link TmsDocuments#root}), since the card zero-division-channel (2026-10-19); until then it was a division by a
 * numeric zero, which deposits since (RUNTIME-SEMANTICS ch. 1.4, point 4), and the documents and measurements below are
 * those of the division: where the condition divided by the bergloon or the deelnemers it now takes the root of the
 * bergloon or the opbrengst, and the transient 0 of the lower default is a transient 2.
 *
 * <p>The transient document: a Vondst with {@code taxatie}, {@code opbrengst} and {@code bergloon}, "Een Vondst is
 * erkend indien hij gemeld is", the defaults group of {@link TmsFaultWitnessTest} on the bergloon (higher "op zijn
 * taxatie indien hij erkend is", lower "op 0 indien hij gemeld is"), and "Een Vondst is zwaar indien zijn opbrengst
 * gedeeld door zijn bergloon groter is dan 2". V1 (gemeld, taxatie 30, opbrengst 90): bergloon 30, zwaar, consistent;
 * where the lower default decides first, the condition divides 90 by the transient 0. The reference interpreter's
 * runner and the ASP runner passed the case in both orders of the document's rules (measured 2026-10-08).
 *
 * <p>The standing documents: one form whose condition or assertion reads "zijn opbrengst gedeeld door zijn deelnemers"
 * beside the erkenning, per kind of form that carries a condition here: a characteristic attribution, an assignment,
 * an initialisation and a consistency. V1 (gemeld, opbrengst 100, deelnemers 0 in the input) faults at the fixpoint in
 * every order, with the sentence the destructive exec-model branch named the same fault with out of its fire, where it
 * lowered the form. Both runners faulted on each of them, "division by numeric zero" (measured 2026-10-08).
 *
 * <p><b>Red before the change</b> (measured 2026-10-08 on the branch at its claim): every row of this branch left the
 * fire with the engine's exception, "[erkenning, zwaarte] RuntimeException:
 * org.drools.modelcompiler.constraints.LambdaEvalExpression@e410d4c : java.lang.RuntimeException:
 * dev.uchar.regelspraak.runtime.Rational$DelingDoorNul: Breuk deling door 0", the transient document in both orders and
 * each standing form, and the rows that fire a session themselves stopped in the fire with it, "de fire wierp in
 * volgorde [erkenning, zwaarte]: java.lang.RuntimeException: …LambdaEvalExpression@e410d4a : …". The exec-model rows
 * were green before.
 *
 * <p><b>Red by mutation</b> (measured the same day, not committed): with the condition's fault inserted by
 * {@code insert} instead of {@code logicalInsert}, the transient fault stayed, "[erkenning, zwaarte] DelingDoorNul:
 * deling door nul in de regel "zwaarte": 90 gedeeld door 0 — …" in both document orders and in the sample.
 */
class TmsConditionFaultWitnessTest {

    static final String MODEL = TmsDocuments.model(List.of(TmsDocuments.objectType("Vondst",
            List.of(attribute("taxatie", WHOLE), attribute("opbrengst", WHOLE), attribute("deelnemers", WHOLE),
                    attribute("bergloon", WHOLE), attribute("toeslag", WHOLE)),
            List.of("gemeld", "erkend", "zwaar"))), List.of());

    private static final String ERKENNING = characteristicRule("erkenning", "Vondst", "erkend", check("gemeld", false));

    // ---------------- a transient fault in a condition ----------------

    /** "Een Vondst is zwaar indien de wortel van zijn bergloon groter is dan 2". */
    static final List<String> TEMPORARY = List.of(ERKENNING,
            characteristicRule("zwaarte", "Vondst", "zwaar", comparison("greater",
                    TmsDocuments.root(ownRead("bergloon", "Vondst"), "de wortel van zijn bergloon"), number("2"))));

    /** The bergloon: higher "op zijn taxatie indien hij erkend is", lower "op 2 indien hij gemeld is". */
    static final String GROUP = attributeGroup("bergloon van een vondst", "Vondst", "bergloon", List.of(
            defaultEntry(check("erkend", false), ownRead("taxatie", "Vondst")),
            defaultEntry(check("gemeld", false), number("2"))));

    /** V1, gemeld, taxatie 9: bergloon 9, zwaar (the root 3), consistent; the root of the transient 2 on the way. */
    static JsonNode temporaryCase() throws Exception {
        return TmsDocuments.caseDocument(
                List.of(instance("V1", "Vondst", Map.of("taxatie", "9"), List.of("gemeld"), Map.of())),
                List.of(value("V1", "bergloon", "9"), expected("V1", "zwaar", true), consistent()));
    }

    static Path temporary(List<String> rules) throws Exception {
        return TmsDocuments.document(MODEL, rules, List.of(), List.of(GROUP));
    }

    @Test void a_temporary_fault_in_a_condition_disappears_at_the_fixpoint_in_every_order_of_the_document()
            throws Exception {
        List<String> outcomes = new ArrayList<>(), expected = new ArrayList<>();
        for (List<String> order : TmsDocuments.permutations(TEMPORARY)) {
            List<String> names = TmsDocuments.ruleNames(order);
            outcomes.add(names + " " + TmsFaultWitnessTest.play(temporary(order), temporaryCase(),
                    DroolsBackend.Branch.EXEC_MODEL));
            expected.add(names + " gespeeld");
        }
        assertEquals(expected, outcomes);
        assertEquals(2, outcomes.size());
    }

    /** The generated rules in a seeded sample of orders, at least one of which must test the condition against the
     *  transient 2 of the lower default: there a fault fact is inserted, and it is gone again at the fixpoint. */
    @Test void sample_of_orders_of_the_generated_rules() throws Exception {
        DroolsBackend.Build b = DroolsBackend.build(temporary(TEMPORARY), DroolsBackend.Branch.EXEC_MODEL);
        List<Rule> generated = b.execModelTranspiler().model().getRules();
        int onTheIntermediateState = 0;
        for (List<Rule> order : TmsDocuments.sample(generated, 20261008L, 100)) {
            List<String> names = order.stream().map(Rule::getName).toList();
            Object v1 = TmsDocuments.beanObject(b.beanLoader(), "Vondst", Map.of("taxatie", "9"), List.of("gemeld"));
            TmsFaultWitnessTest.FaultFacts facts = new TmsFaultWitnessTest.FaultFacts();
            String fault = TmsFaultWitnessTest.fire(b, order, List.of(v1), new DefaultAgendaEventListener() {}, facts);
            assertNull(fault, () -> "volgorde " + names);
            assertEquals(facts.inserted, facts.withdrawn, () -> "foutfeiten in volgorde " + names);
            TmsFaultWitnessTest.assertNumber("9", TmsDocuments.read(v1, "bergloon"), "bergloon in volgorde " + names);
            assertTrue(TmsDocuments.has(v1, "zwaar"), () -> "zwaar in volgorde " + names);
            if (facts.inserted > 0) onTheIntermediateState++;
        }
        assertTrue(onTheIntermediateState > 0, "geen volgorde toetste de voorwaarde tegen de tijdelijke 2 van de lagere"
                + " default");
    }

    // ---------------- a fault in a condition that stands ----------------

    /** "de wortel van zijn opbrengst"; until the card zero-division-channel "zijn opbrengst gedeeld door zijn
     *  deelnemers". */
    private static final String DEELNEMERS = TmsDocuments.root(ownRead("opbrengst", "Vondst"), "de wortel van zijn opbrengst");

    /** One document per kind of form with a condition: its name, its rules and its consistencies. */
    record Form(String name, List<String> rules, List<String> consistencies) {}

    static final List<Form> FORMS = List.of(
            new Form("zwaarte", List.of(ERKENNING, characteristicRule("zwaarte", "Vondst", "zwaar",
                    comparison("greater", DEELNEMERS, number("10")))), List.of()),
            new Form("toeslag", List.of(ERKENNING, computingRule("toeslag", "GELIJKSTELLING", "Vondst", "toeslag",
                    number("5"), comparison("greater", DEELNEMERS, number("10")))), List.of()),
            new Form("standaardtoeslag", List.of(ERKENNING, computingRule("standaardtoeslag", "INITIALISATIE", "Vondst",
                    "toeslag", number("5"), comparison("greater", DEELNEMERS, number("10")))), List.of()),
            new Form("controle", List.of(ERKENNING),
                    List.of(consistency("controle", "Vondst", comparison("greater", DEELNEMERS, number("0"))))));

    /** V1, gemeld, opbrengst 2: the root of 2 in every form. */
    static JsonNode persistentCase() throws Exception {
        return TmsDocuments.caseDocument(
                List.of(instance("V1", "Vondst", Map.of("opbrengst", "2"), List.of("gemeld"), Map.of())),
                List.of(consistent()));
    }

    private static String message(String rule) {
        return "ComputeFault: " + TmsDocuments.irrational(rule, "2");
    }

    @Test void a_persistent_fault_in_a_condition_gives_in_every_order_the_message_of_the_exec_model_branch()
            throws Exception {
        List<String> outcomes = new ArrayList<>(), expected = new ArrayList<>();
        for (Form v : FORMS) {
            for (List<String> order : TmsDocuments.permutations(v.rules())) {
                List<String> names = TmsDocuments.ruleNames(order);
                outcomes.add(v.name() + " " + names + " " + TmsFaultWitnessTest.play(
                        TmsDocuments.document(MODEL, order, v.consistencies()), persistentCase(),
                        DroolsBackend.Branch.EXEC_MODEL));
                expected.add(v.name() + " " + names + " " + message(v.name()));
            }
        }
        assertEquals(expected, outcomes);
        assertEquals(7, outcomes.size());
    }

    /** The fire reaches its fixpoint without throwing in every order of the generated rules of each document (the form,
     *  its fault rule, its deposit rule and the erkenning: 24 orders each; six until the deposit rule of a condition,
     *  fixround 1 of card tms-expressievormen-en-rekendatum), and the write-back reports the fault. */
    @Test void the_fault_in_a_condition_counts_at_the_fixpoint_and_not_in_the_fire() throws Exception {
        List<String> outcomes = new ArrayList<>(), expected = new ArrayList<>();
        for (Form v : FORMS) {
            DroolsBackend.Build b = DroolsBackend.build(TmsDocuments.document(MODEL, v.rules(), v.consistencies()),
                    DroolsBackend.Branch.EXEC_MODEL);
            for (List<Rule> order : TmsDocuments.permutations(b.execModelTranspiler().model().getRules())) {
                List<String> names = order.stream().map(Rule::getName).toList();
                Object v1 = TmsDocuments.beanObject(b.beanLoader(), "Vondst", Map.of("opbrengst", "2"), List.of("gemeld"));
                outcomes.add(names + " " + TmsFaultWitnessTest.fire(b, order, List.of(v1),
                        new DefaultAgendaEventListener() {}, new TmsFaultWitnessTest.FaultFacts()));
                expected.add(names + " " + message(v.name()));
            }
        }
        assertEquals(expected, outcomes);
        assertEquals(96, outcomes.size());
    }
}
