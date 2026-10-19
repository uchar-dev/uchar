package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;

import dev.uchar.regelspraak.transpiler.tms.ComputeFault;
import dev.uchar.regelspraak.transpiler.tms.State;

import org.drools.model.Rule;
import org.junit.jupiter.api.Test;
import org.kie.api.event.rule.AfterMatchFiredEvent;
import org.kie.api.event.rule.AgendaEventListener;
import org.kie.api.event.rule.DefaultAgendaEventListener;
import org.kie.api.event.rule.ObjectDeletedEvent;
import org.kie.api.event.rule.ObjectInsertedEvent;
import org.kie.api.event.rule.ObjectUpdatedEvent;
import org.kie.api.event.rule.RuleRuntimeEventListener;
import org.kie.api.runtime.KieSession;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static dev.uchar.regelspraak.cases.TmsDocuments.WHOLE;
import static dev.uchar.regelspraak.cases.TmsDocuments.attribute;
import static dev.uchar.regelspraak.cases.TmsDocuments.attributeGroup;
import static dev.uchar.regelspraak.cases.TmsDocuments.check;
import static dev.uchar.regelspraak.cases.TmsDocuments.consistent;
import static dev.uchar.regelspraak.cases.TmsDocuments.number;
import static dev.uchar.regelspraak.cases.TmsDocuments.instance;
import static dev.uchar.regelspraak.cases.TmsDocuments.characteristicRule;
import static dev.uchar.regelspraak.cases.TmsDocuments.ownRead;
import static dev.uchar.regelspraak.cases.TmsDocuments.predicate;
import static dev.uchar.regelspraak.cases.TmsDocuments.computingRule;
import static dev.uchar.regelspraak.cases.TmsDocuments.defaultEntry;
import static dev.uchar.regelspraak.cases.TmsDocuments.value;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * An evaluation fault in a rule's value on the exec-model branch: a fault, like a value, belongs to the fixpoint
 * (RUNTIME-SEMANTICS ch. 5.2). The branch has no
 * strata and passes intermediate states the reference interpreter does not see, and a fault thrown out of a consequence
 * cannot be withdrawn, so the fault is a fact inserted logically, justified by the activation that raised it, and the
 * session reports the faults that stand once it reaches its fixpoint, in an order that does not depend on the order of
 * the rules.
 *
 * <p><b>The device</b> is the square root without a rounding of a number that is no square of a rational, the fault of
 * an irrational root (ch. 2.2; {@link TmsDocuments#root}), since the card zero-division-channel (2026-10-19). Until then
 * it was a division by a numeric zero, which v1 routes into the §9.5 channel since (ch. 1.4, point 4), and the documents
 * and measurements below are those of the division: the reader divided its opbrengst by the bergloon or the deelnemers,
 * where it now takes the root of the bergloon, the opbrengst or the taxatie, and the transient 0 of the lower default
 * is a transient 2. Each row asks what it asked of the division.
 *
 * <p>The transient document: a Vondst with {@code taxatie}, {@code opbrengst}, {@code bergloon} and {@code aandeel},
 * "Een Vondst is erkend indien hij gemeld is", a defaults group on the bergloon, higher "op zijn taxatie indien hij
 * erkend is" and lower "op 0 indien hij gemeld is", and the reader "Het aandeel van een Vondst moet berekend worden als
 * zijn opbrengst gedeeld door zijn bergloon, indien zijn bergloon gevuld is". V1 (gemeld, taxatie 30, opbrengst 90):
 * bergloon 30, aandeel 3, consistent. Where the lower default decides before the standing of {@code erkend} arrives, the
 * reader divides 90 by the transient 0; that fault goes when the higher default displaces the lower one. The reference
 * interpreter's runner and the ASP runner passed the case in both orders of the document's rules (measured
 * 2026-10-08). The reader's condition keeps the reference interpreter off a division by leeg: without it, that runner
 * computed the reader before the defaults group decided and kept the §6.5 deposit of 90 divided by leeg, "MISMATCH:
 * expected consistent, but the channel carries: aandeel × V1", in both orders (RUNTIME-SEMANTICS ch. 6.1, "Recorded
 * residuals"), where the ASP runner passed and this branch withdraws the deposit.
 *
 * <p>The standing document: "Het aandeel van een Vondst moet berekend worden als zijn opbrengst gedeeld door zijn
 * deelnemers, indien hij erkend is" beside the erkenning; V1 (gemeld, opbrengst 100, deelnemers 0 in the input) faults
 * at the fixpoint, with the sentence the destructive exec-model branch gave out of its fire. The document with two
 * faults: "Het aandeel … als zijn opbrengst gedeeld door zijn deelnemers", "De verhouding … als zijn taxatie gedeeld
 * door zijn deelnemers, indien hij erkend is" and the erkenning; V1 (gemeld, opbrengst 100, taxatie 30, deelnemers 0)
 * reports both, ordered by the name of the rule, in every order. Both runners faulted on these two documents in every
 * order, "division by numeric zero" (measured 2026-10-08).
 *
 * <p><b>Red before the change</b> (measured 2026-10-08 on the branch at its claim, where a consequence threw the
 * fault): the transient document played into the fault in both orders, "[erkenning, aandeel] DelingDoorNul: deling door
 * nul in de regel "aandeel": 90 gedeeld door 0 — een numerieke nul als deler is een fout, geen leeg (RUNTIME-SEMANTICS
 * ch. 1.4, punt 4)"; every row that fires a session itself stopped in the fire, "de fire wierp in volgorde [erkenning,
 * aandeel, kenmerkstand Vondst.erkend: waar uit de invoer, …]: Exception executing consequence for rule "aandeel" in
 * dev.uchar.regelspraak.tms: dev.uchar.regelspraak.runtime.Rational$DelingDoorNul: Breuk deling door 0"; and the document
 * with two faults reported the first fault the agenda reached and no other, "[aandeel, verhouding, erkenning]
 * DelingDoorNul: deling door nul in de regel "aandeel": 100 gedeeld door 0 — …", in each of its six orders. The replay
 * of the single standing fault and the exec-model rows were green before: they pin what stays.
 *
 * <p><b>Red by mutation</b> (measured the same day, not committed). With the value's fault inserted by {@code insert}
 * instead of {@code logicalInsert}, the transient fault stayed: "[erkenning, aandeel] DelingDoorNul: deling door nul in
 * de regel "aandeel": 90 gedeeld door 0 — …" in both document orders, and in the sample "expected: &lt;null&gt; but
 * was: &lt;DelingDoorNul: deling door nul in de regel "aandeel": 90 gedeeld door 0 — …&gt;". Without the sort at the
 * write-back, the two faults came in the order of the session: "but was: &lt;Evaluatiefouten: aan het fixpunt staan 2
 * evaluatiefouten: deling door nul in de regel "verhouding": 30 gedeeld door 0 — …; deling door nul in de regel
 * "aandeel": 100 gedeeld door 0 — …&gt;" in the order [aandeel, verhouding, kenmerkstand Vondst.erkend: waar uit een
 * afleiding, erkenning, …].
 *
 * <p><b>The CLI's ERROR line.</b> The CLI bounded its ERROR line at 200 characters across the whole message, so on the
 * document with two faults the line broke off in the second sentence, before the rule "verhouding", while the rows above
 * compared the exception and not that line. The message now opens with a head that names the rule of every fault, and
 * the CLI bounds only the sentences after it. <b>Red before the change</b> (measured 2026-10-08 through
 * {@code DroolsBackendMain.run}, the CLI without its exit, with the line formed as before; the jar of the branch printed
 * the same line): in each of the six orders "[noemt "verhouding" niet, opent niet met exit 3 en de kop]: 3 ERROR
 * Evaluatiefouten: aan het fixpunt staan 2 evaluatiefouten: deling door nul in de regel "aandeel": 100 gedeeld door 0 —
 * een numerieke nul als deler is een fout, geen leeg (RUNTIME-SEMANTICS ch. 1.4, punt 4); deling door…", and on the
 * document of long rule names "[noemt "de verhouding … van de gemeente" niet, noemt "het aandeel … van de gemeente"
 * niet, opent niet met exit 3 en de kop]: 3 ERROR Evaluatiefouten: aan het fixpunt staan 4 evaluatiefouten: deling door
 * nul in de regel "de verhouding van een vondst … op het strand van de g…". <b>Red by mutation</b> (not committed): with
 * the head in the message and the bound across the whole line again, the document of long rule names lost its second
 * rule in each order, "[noemt "het aandeel … van de gemeente" niet, opent niet met exit 3 en de kop]: 3 ERROR
 * Evaluatiefouten: aan het fixpunt staan 4 evaluatiefouten, in de regels "de verhouding … van de gemeente" (2×), …",
 * while the document with two faults stayed green, its head being shorter than the bound.
 */
class TmsFaultWitnessTest {

    static final String MODEL = TmsDocuments.model(List.of(TmsDocuments.objectType("Vondst",
            List.of(attribute("taxatie", WHOLE), attribute("opbrengst", WHOLE), attribute("deelnemers", WHOLE),
                    attribute("bergloon", WHOLE), attribute("aandeel", WHOLE), attribute("verhouding", WHOLE)),
            List.of("gemeld", "erkend"))), List.of());

    private static final String ERKENNING = characteristicRule("erkenning", "Vondst", "erkend", check("gemeld", false));

    // ---------------- a transient fault ----------------

    /** "Het aandeel van een Vondst moet berekend worden als de wortel van zijn bergloon, indien zijn bergloon gevuld
     *  is". */
    static final List<String> TEMPORARY = List.of(ERKENNING,
            computingRule("aandeel", "BEREKENING", "Vondst", "aandeel",
                    TmsDocuments.root(ownRead("bergloon", "Vondst"), "de wortel van zijn bergloon"),
                    predicate("filled", ownRead("bergloon", "Vondst"))));

    /** The bergloon: higher "op zijn taxatie indien hij erkend is", lower "op 2 indien hij gemeld is". */
    static final String GROUP = attributeGroup("bergloon van een vondst", "Vondst", "bergloon", List.of(
            defaultEntry(check("erkend", false), ownRead("taxatie", "Vondst")),
            defaultEntry(check("gemeld", false), number("2"))));

    /** V1, gemeld, taxatie 9: bergloon 9, aandeel 3, consistent; the reader takes the root of the transient 2 on the
     *  way where the lower default decides first. */
    static JsonNode temporaryCase() throws Exception {
        return TmsDocuments.caseDocument(
                List.of(instance("V1", "Vondst", Map.of("taxatie", "9"), List.of("gemeld"), Map.of())),
                List.of(value("V1", "bergloon", "9"), value("V1", "aandeel", "3"), consistent()));
    }

    static Path temporary(List<String> rules) throws Exception {
        return TmsDocuments.document(MODEL, rules, List.of(), List.of(GROUP));
    }

    @Test void a_temporary_fault_disappears_at_the_fixpoint_in_every_order_of_the_document()
            throws Exception {
        List<String> outcomes = new ArrayList<>(), expected = new ArrayList<>();
        for (List<String> order : TmsDocuments.permutations(TEMPORARY)) {
            List<String> names = TmsDocuments.ruleNames(order);
            outcomes.add(names + " " + play(temporary(order), temporaryCase(), DroolsBackend.Branch.EXEC_MODEL));
            expected.add(names + " gespeeld");
        }
        assertEquals(expected, outcomes);
        assertEquals(2, outcomes.size());
    }

    /** The two rules, the two defaults, and the standings of the bergloon and of erkend; a seeded sample of their
     *  orders, at least one of which must let the reader take the root of the transient 2 of the lower default. */
    @Test void sample_of_orders_of_the_generated_rules() throws Exception {
        DroolsBackend.Build b = DroolsBackend.build(temporary(TEMPORARY), DroolsBackend.Branch.EXEC_MODEL);
        List<Rule> generated = b.execModelTranspiler().model().getRules();
        int onTheIntermediateState = 0;
        for (List<Rule> order : TmsDocuments.sample(generated, 20261008L, 100)) {
            List<String> names = order.stream().map(Rule::getName).toList();
            Object v1 = TmsDocuments.beanObject(b.beanLoader(), "Vondst", Map.of("taxatie", "9"), List.of("gemeld"));
            boolean[] intermediateState = {false};
            FaultFacts facts = new FaultFacts();
            String fault = fire(b, order, List.of(v1), new DefaultAgendaEventListener() {
                @Override public void afterMatchFired(AfterMatchFiredEvent e) {
                    if (e.getMatch().getRule().getName().equals("aandeel")
                            && e.getMatch().getObjects().stream().anyMatch(o -> o instanceof State s
                                    && s.value() instanceof BigDecimal d && d.compareTo(BigDecimal.TWO) == 0))
                        intermediateState[0] = true;
                }
            }, facts);
            assertNull(fault, () -> "volgorde " + names);
            assertEquals(facts.inserted, facts.withdrawn, () -> "foutfeiten in volgorde " + names);
            assertNumber("9", TmsDocuments.read(v1, "bergloon"), "bergloon in volgorde " + names);
            assertNumber("3", TmsDocuments.read(v1, "aandeel"), "aandeel in volgorde " + names);
            if (intermediateState[0]) {
                assertTrue(facts.inserted > 0, () -> "de wortel uit de tijdelijke 2 gaf geen foutfeit, volgorde " + names);
                onTheIntermediateState++;
            }
        }
        assertTrue(onTheIntermediateState > 0, "geen volgorde liet de lezer de wortel nemen uit de tijdelijke 2 van de"
                + " lagere default");
    }

    // ---------------- a fault that stands at the fixpoint ----------------

    /** "Het aandeel van een Vondst moet berekend worden als de wortel van zijn opbrengst, indien hij erkend is". */
    static final List<String> PERSISTENT = List.of(ERKENNING,
            computingRule("aandeel", "BEREKENING", "Vondst", "aandeel",
                    TmsDocuments.root(ownRead("opbrengst", "Vondst"), "de wortel van zijn opbrengst"),
                    check("erkend", false)));

    /** V1, gemeld, opbrengst 2: the root of 2 at the fixpoint. */
    static JsonNode persistentCase() throws Exception {
        return TmsDocuments.caseDocument(
                List.of(instance("V1", "Vondst", Map.of("opbrengst", "2"), List.of("gemeld"), Map.of())),
                List.of(value("V1", "aandeel", null), consistent()));
    }

    static Path persistent(List<String> rules) throws Exception {
        return TmsDocuments.document(MODEL, rules);
    }

    private static final String AANDEEL_ROOT = "ComputeFault: " + TmsDocuments.irrational("aandeel", "2");

    @Test void a_persistent_fault_gives_in_every_order_the_same_message_as_the_exec_model_branch()
            throws Exception {
        List<String> outcomes = new ArrayList<>(), expected = new ArrayList<>();
        for (List<String> order : TmsDocuments.permutations(PERSISTENT)) {
            List<String> names = TmsDocuments.ruleNames(order);
            outcomes.add(names + " " + play(persistent(order), persistentCase(), DroolsBackend.Branch.EXEC_MODEL));
            expected.add(names + " " + AANDEEL_ROOT);
        }
        assertEquals(expected, outcomes);
        assertEquals(2, outcomes.size());
    }

    /** The fire reaches its fixpoint without throwing, in every order of the two rules and the three standings of
     *  erkend, and the write-back reports the fault that stands there, as a consumer that fires a KieBase itself
     *  meets it. */
    @Test void the_fault_counts_at_the_fixpoint_and_not_in_the_fire() throws Exception {
        DroolsBackend.Build b = DroolsBackend.build(persistent(PERSISTENT), DroolsBackend.Branch.EXEC_MODEL);
        int orders = 0;
        for (List<Rule> order : TmsDocuments.permutations(b.execModelTranspiler().model().getRules())) {
            List<String> names = order.stream().map(Rule::getName).toList();
            Object v1 = TmsDocuments.beanObject(b.beanLoader(), "Vondst", Map.of("opbrengst", "2"), List.of("gemeld"));
            assertEquals(AANDEEL_ROOT, fire(b, order, List.of(v1), new DefaultAgendaEventListener() {},
                    new FaultFacts()), () -> "volgorde " + names);
            assertNull(TmsDocuments.read(v1, "aandeel"), () -> "een fout schrijft niets terug, volgorde " + names);
            orders++;
        }
        assertEquals(120, orders);
    }

    // ---------------- two faults that stand ----------------

    /** "Het aandeel … als de wortel van zijn opbrengst", "De verhouding … als de wortel van zijn taxatie, indien hij
     *  erkend is" and the erkenning. */
    static final List<String> TWO = List.of(
            computingRule("aandeel", "BEREKENING", "Vondst", "aandeel",
                    TmsDocuments.root(ownRead("opbrengst", "Vondst"), "de wortel van zijn opbrengst"), "null"),
            computingRule("verhouding", "BEREKENING", "Vondst", "verhouding",
                    TmsDocuments.root(ownRead("taxatie", "Vondst"), "de wortel van zijn taxatie"),
                    check("erkend", false)),
            ERKENNING);

    /** V1, gemeld, opbrengst 2 and taxatie 3: a fault in each rule. */
    static JsonNode twoCase() throws Exception {
        return TmsDocuments.caseDocument(
                List.of(instance("V1", "Vondst", Map.of("opbrengst", "2", "taxatie", "3"), List.of("gemeld"), Map.of())),
                List.of(value("V1", "aandeel", null), value("V1", "verhouding", null), consistent()));
    }

    static Path two(List<String> rules) throws Exception {
        return TmsDocuments.document(MODEL, rules);
    }

    /** The head of the two faults: their number and the rule of each, in the order of the report. */
    private static final String TWO_HEAD = "aan het fixpunt staan 2 evaluatiefouten, in de regels \"aandeel\","
            + " \"verhouding\"";

    private static final String TWO_FAULTS = "EvaluationFaults: " + TWO_HEAD + ": " + TmsDocuments.irrational("aandeel", "2")
            + "; " + TmsDocuments.irrational("verhouding", "3");

    @Test void two_persistent_faults_are_in_every_order_in_the_same_fixed_order() throws Exception {
        List<String> outcomes = new ArrayList<>(), expected = new ArrayList<>();
        for (List<String> order : TmsDocuments.permutations(TWO)) {
            List<String> names = TmsDocuments.ruleNames(order);
            outcomes.add(names + " " + play(two(order), twoCase(), DroolsBackend.Branch.EXEC_MODEL));
            expected.add(names + " " + TWO_FAULTS);
        }
        assertEquals(expected, outcomes);
        assertEquals(6, outcomes.size());
    }

    /** The three rules and the three standings of erkend: 720 orders, every one tried. */
    @Test void two_persistent_faults_in_every_order_of_the_generated_rules() throws Exception {
        DroolsBackend.Build b = DroolsBackend.build(two(TWO), DroolsBackend.Branch.EXEC_MODEL);
        int orders = 0;
        for (List<Rule> order : TmsDocuments.permutations(b.execModelTranspiler().model().getRules())) {
            List<String> names = order.stream().map(Rule::getName).toList();
            Object v1 = TmsDocuments.beanObject(b.beanLoader(), "Vondst", Map.of("opbrengst", "2", "taxatie", "3"),
                    List.of("gemeld"));
            assertEquals(TWO_FAULTS, fire(b, order, List.of(v1), new DefaultAgendaEventListener() {},
                    new FaultFacts()), () -> "volgorde " + names);
            orders++;
        }
        assertEquals(720, orders);
    }

    /** The fault a consumer meets at the fixpoint is the fault named after its rule, with the unnamed fault the
     *  evaluation raised as its cause, so the red path's cause chain reads both. Two faults are one exception whose cause
     *  is the first fault of its message and whose suppressed exception is the second. Until the card
     *  zero-division-channel the fault was the named division, with the division as {@code Rational.delen} threw it as
     *  its cause. */
    @Test void the_faults_at_the_fixpoint_carry_the_unnamed_fault_as_cause() throws Exception {
        DroolsBackend.Build b = DroolsBackend.build(persistent(PERSISTENT), DroolsBackend.Branch.EXEC_MODEL);
        RuntimeException one = fixpointFault(b, b.execModelTranspiler().model().getRules(), List.of(TmsDocuments.beanObject(
                b.beanLoader(), "Vondst", Map.of("opbrengst", "2"), List.of("gemeld"))),
                new DefaultAgendaEventListener() {}, new FaultFacts());
        assertInstanceOf(ComputeFault.class, one);
        assertEquals(AANDEEL_ROOT, "ComputeFault: " + one.getMessage());
        assertInstanceOf(ComputeFault.class, one.getCause());
        assertEquals("irrational square root of 2 reached without an explicit rounding (§13.4.17.13 mandates one)",
                one.getCause().getMessage());

        DroolsBackend.Build b2 = DroolsBackend.build(two(TWO), DroolsBackend.Branch.EXEC_MODEL);
        RuntimeException two = fixpointFault(b2, b2.execModelTranspiler().model().getRules(), List.of(TmsDocuments.beanObject(
                b2.beanLoader(), "Vondst", Map.of("opbrengst", "2", "taxatie", "3"), List.of("gemeld"))),
                new DefaultAgendaEventListener() {}, new FaultFacts());
        assertEquals(TWO_FAULTS, two.getClass().getSimpleName() + ": " + two.getMessage());
        assertEquals(AANDEEL_ROOT, "ComputeFault: " + two.getCause().getMessage());
        assertEquals(1, two.getSuppressed().length);
        assertInstanceOf(ComputeFault.class, two.getSuppressed()[0]);
        assertTrue(two.getSuppressed()[0].getMessage().startsWith("fout in de regel \"verhouding\": irrational square"
                + " root of 3"), () -> "onderdrukt: " + two.getSuppressed()[0]);
    }

    // ---------------- the CLI's status line ----------------

    /** The CLI's ERROR line on the document with two faults, in each of its six orders: exit code 3, and a line that
     *  opens with the head naming both rules and ends where the bound shortened the sentences. The line is the
     *  exception's message with its sentences bounded, and that message is pinned over the 720 orders of the generated
     *  rules by {@link #two_persistent_faults_in_every_order_of_the_generated_rules}. */
    @Test void the_error_line_of_the_cli_names_both_standing_faults_in_every_order() throws Exception {
        Path casePath = caseFile(twoCase());
        List<String> outcomes = new ArrayList<>(), expected = new ArrayList<>();
        for (List<String> order : TmsDocuments.permutations(TWO)) {
            List<String> names = TmsDocuments.ruleNames(order);
            String rule = cli(two(order).toString(), casePath.toString());
            List<String> mis = defects(rule, TWO_HEAD, List.of("aandeel", "verhouding"));
            outcomes.add(names + (mis.isEmpty() ? " in orde" : " " + mis + ": " + rule));
            expected.add(names + " in orde");
        }
        assertEquals(expected, outcomes);
        assertEquals(6, outcomes.size());
    }

    static final String LONG_AANDEEL = "het aandeel van een vondst in de opbrengst van de veiling over het aantal"
            + " deelnemers aan de berging op het strand van de gemeente";
    static final String LONG_RATIO = "de verhouding van een vondst tussen de taxatie door de strandvonder en het"
            + " aantal deelnemers aan de berging op het strand van de gemeente";

    /** The document with two faults under rule names of over a hundred characters each. */
    static final List<String> LONG = List.of(
            computingRule(LONG_AANDEEL, "BEREKENING", "Vondst", "aandeel",
                    TmsDocuments.root(ownRead("opbrengst", "Vondst"), "de wortel van zijn opbrengst"), "null"),
            computingRule(LONG_RATIO, "BEREKENING", "Vondst", "verhouding",
                    TmsDocuments.root(ownRead("taxatie", "Vondst"), "de wortel van zijn taxatie"),
                    check("erkend", false)),
            ERKENNING);

    /** V1 and V2, each with an opbrengst and a taxatie that are no squares: each rule faults on both, four faults in
     *  all. */
    static JsonNode twoVondstenCase() throws Exception {
        return TmsDocuments.caseDocument(List.of(
                instance("V1", "Vondst", Map.of("opbrengst", "2", "taxatie", "3"), List.of("gemeld"), Map.of()),
                instance("V2", "Vondst", Map.of("opbrengst", "5", "taxatie", "6"), List.of("gemeld"), Map.of())),
                List.of(consistent()));
    }

    /** The bound drops no fault where the head alone is longer than the bound: two rules of long names, each faulting
     *  on two objects, in each of the six orders of the document. The head names each rule once with the number of its
     *  faults, in the fixed order (by name: "de verhouding …" before "het aandeel …"). */
    @Test void the_bound_of_the_error_line_leaves_no_standing_fault_out() throws Exception {
        Path casePath = caseFile(twoVondstenCase());
        String head = "aan het fixpunt staan 4 evaluatiefouten, in de regels \"" + LONG_RATIO + "\" (2×), \""
                + LONG_AANDEEL + "\" (2×)";
        assertTrue(head.length() > 200, () -> "de kop past binnen de grens: " + head.length() + " tekens");
        Map<String, String> shortQuote = Map.of(LONG_AANDEEL, "aandeel …", LONG_RATIO, "verhouding …");
        List<String> outcomes = new ArrayList<>(), expected = new ArrayList<>();
        for (List<String> order : TmsDocuments.permutations(LONG)) {
            List<String> names = TmsDocuments.ruleNames(order).stream().map(n -> shortQuote.getOrDefault(n, n)).toList();
            String rule = cli(TmsDocuments.document(MODEL, order).toString(), casePath.toString());
            List<String> mis = defects(rule, head, List.of(LONG_RATIO, LONG_AANDEEL));
            outcomes.add(names + (mis.isEmpty() ? " in orde" : " " + mis + ": " + rule));
            expected.add(names + " in orde");
        }
        assertEquals(expected, outcomes);
        assertEquals(6, outcomes.size());
    }

    /** What is wrong with the CLI's {@code rule} (exit code, a space, the last line it printed) on a document whose
     *  faults stand under {@code head}: empty where the exit code is 3, the line opens with the ERROR head and
     *  {@code head}, names every rule of {@code rules}, and the bound shortened the sentences after the head. */
    private static List<String> defects(String rule, String head, List<String> rules) {
        List<String> mis = new ArrayList<>();
        for (String name : rules)
            if (!rule.contains("\"" + name + "\"")) mis.add("noemt \"" + name + "\" niet");
        if (!rule.startsWith("3 ERROR EvaluationFaults: " + head + ": ")) mis.add("opent niet met exit 3 en de kop");
        if (!rule.endsWith("…")) mis.add("de grens kortte de zinnen niet in");
        return mis;
    }

    /** The exit code of {@code DroolsBackendMain} on {@code args}, a space, and the last line it printed: the status
     *  line gate 12 and the corpus sweep read ({@code java -jar … | tail -1}), from the CLI's own code in this JVM. */
    private static String cli(String... args) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int code = DroolsBackendMain.run(args, new PrintStream(out, true, StandardCharsets.UTF_8));
        List<String> rules = out.toString(StandardCharsets.UTF_8).lines().toList();
        return code + " " + (rules.isEmpty() ? "(geen uitvoer)" : rules.get(rules.size() - 1));
    }

    /** {@code caseDocument} in a temporary file, for the CLI. */
    private static Path caseFile(JsonNode caseDocument) throws Exception {
        Path tmp = Files.createTempFile("tms-getuige-zaak", ".json");
        tmp.toFile().deleteOnExit();
        return Files.writeString(tmp, caseDocument.toString());
    }

    // ---------------- helpers ----------------

    /** The replay of {@code caseDocument} on {@code branch}: "gespeeld" where every expectation matched, the replay's verdict line
     *  where one did not, and the exception the replay raised as the CLI's ERROR line names it. */
    static String play(Path ir, JsonNode caseDocument, DroolsBackend.Branch branch) throws Exception {
        try {
            String verdict = CaseRunner.playCase(ir, caseDocument, branch);
            return verdict == null ? "gespeeld" : verdict;
        } catch (RuntimeException e) {
            return e.getClass().getSimpleName() + ": " + e.getMessage();
        }
    }

    /** {@link #play} on the exec-model branch and on {@code DRL_TEXT}, its analysis rendered as DRL text, which gives
     *  the same outcome (card drl-teksttak-op-truth-maintenance, step 5): that outcome. */
    static String playBoth(Path ir, JsonNode caseDocument) throws Exception {
        String exec = play(ir, caseDocument, DroolsBackend.Branch.EXEC_MODEL);
        assertEquals(exec, play(ir, caseDocument, DroolsBackend.Branch.DRL_TEXT), () -> "DRL_TEXT speelt " + ir
                + " anders dan de exec-model-tak");
        return exec;
    }

    /** {@link #fixpointFault} as "SimpleName: message", or null where the write-back raises nothing. */
    static String fire(DroolsBackend.Build b, List<Rule> order, List<Object> beans, AgendaEventListener agenda,
                       FaultFacts facts) {
        RuntimeException fault = fixpointFault(b, order, beans, agenda, facts);
        return fault == null ? null : fault.getClass().getSimpleName() + ": " + fault.getMessage();
    }

    /** Fires {@code beans} through the rules of {@code b} added in {@code order}, with {@code agenda} on the agenda
     *  and {@code facts} on working memory. The fire must reach its fixpoint below the cap without throwing: an
     *  evaluation fault is a fact of the session, not an exception out of the fire. Returns the fault the write-back
     *  raises at the fixpoint, or null when it writes the derivations back. */
    static RuntimeException fixpointFault(DroolsBackend.Build b, List<Rule> order, List<Object> beans,
                                        AgendaEventListener agenda, FaultFacts facts) {
        List<String> names = order.stream().map(Rule::getName).toList();
        ClassLoader previous = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(b.beanLoader());
        KieSession session = TmsDocuments.kieBase(b, order).newKieSession();
        try {
            session.addEventListener(agenda);
            session.addEventListener(facts);
            for (Object beanObject : beans) session.insert(beanObject);
            DroolsBackend.Firing firing;
            try {
                firing = DroolsBackend.fire(b, session, 100_000);
            } catch (RuntimeException e) {
                return fail("de fire wierp in volgorde " + names + ": " + e);
            }
            assertNull(firing.noFixpoint(), () -> "geen fixpunt in volgorde " + names);
            try {
                DroolsBackend.setDerivedValues(b, session);
                return null;
            } catch (RuntimeException e) {
                return e;
            }
        } finally {
            session.dispose();
            Thread.currentThread().setContextClassLoader(previous);
        }
    }

    /** Counts the fault facts a session inserts and withdraws, by the simple name of their class. */
    static final class FaultFacts implements RuleRuntimeEventListener {
        int inserted, withdrawn;

        @Override public void objectInserted(ObjectInsertedEvent e) {
            if (e.getObject().getClass().getSimpleName().equals("Fault")) inserted++;
        }

        @Override public void objectUpdated(ObjectUpdatedEvent e) {}

        @Override public void objectDeleted(ObjectDeletedEvent e) {
            if (e.getOldObject().getClass().getSimpleName().equals("Fault")) withdrawn++;
        }
    }

    static void assertNumber(String expected, Object actual, String description) {
        assertTrue(actual instanceof BigDecimal w && new BigDecimal(expected).compareTo(w) == 0,
                () -> description + ": verwacht " + expected + ", werkelijk " + actual);
    }
}
