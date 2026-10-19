package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;

import dev.uchar.regelspraak.transpiler.tms.CreationPhase;
import dev.uchar.regelspraak.transpiler.tms.State;
import dev.uchar.regelspraak.transpiler.tms.Value;

import org.drools.model.Rule;
import org.junit.jupiter.api.Test;
import org.kie.api.KieBase;
import org.kie.api.event.rule.DefaultAgendaEventListener;
import org.kie.api.event.rule.DefaultRuleRuntimeEventListener;
import org.kie.api.event.rule.MatchCreatedEvent;
import org.kie.api.event.rule.ObjectDeletedEvent;
import org.kie.api.event.rule.ObjectInsertedEvent;
import org.kie.api.runtime.KieSession;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static dev.uchar.regelspraak.cases.TmsDocuments.WHOLE;
import static dev.uchar.regelspraak.cases.TmsDocuments.COUNTING;
import static dev.uchar.regelspraak.cases.TmsDocuments.aggregation;
import static dev.uchar.regelspraak.cases.TmsDocuments.attribute;
import static dev.uchar.regelspraak.cases.TmsDocuments.attributeGroup;
import static dev.uchar.regelspraak.cases.TmsDocuments.binary;
import static dev.uchar.regelspraak.cases.TmsDocuments.sourceRole;
import static dev.uchar.regelspraak.cases.TmsDocuments.check;
import static dev.uchar.regelspraak.cases.TmsDocuments.factTypeOneToMany;
import static dev.uchar.regelspraak.cases.TmsDocuments.number;
import static dev.uchar.regelspraak.cases.TmsDocuments.instanceWithRoles;
import static dev.uchar.regelspraak.cases.TmsDocuments.ownRead;
import static dev.uchar.regelspraak.cases.TmsDocuments.pathRead;
import static dev.uchar.regelspraak.cases.TmsDocuments.computingRule;
import static dev.uchar.regelspraak.cases.TmsDocuments.roleBinding;
import static dev.uchar.regelspraak.cases.TmsDocuments.compound;
import static dev.uchar.regelspraak.cases.TmsDocuments.defaultEntry;
import static dev.uchar.regelspraak.cases.TmsDocuments.comparison;
import static dev.uchar.regelspraak.cases.TmsDocuments.value;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Object and fact creation on the exec-model branch (RUNTIME-SEMANTICS ch. 5.5): a creation fires only at a
 * fixpoint of the rule layer, what it makes is stated and never lapses, and the driver fires in generations
 * ({@code DroolsBackend.fire}). Each document runs once through the replay and then in a sample of orders of the rules
 * the branch generates (the order of the model, its reverse and a hundred shuffles), through the driver.
 *
 * <ul>
 *   <li>A creation makes one object per subject, also when a later generation finds its condition true again, and its
 *       member initialisations are the values of the fixpoint at which it fires: the vangst a lower default gives first
 *       is a state the creation does not see ({@link #an_object_creation_makes_an_object_per_subject_with_the_values_of_the_fixpoint}).</li>
 *   <li>A creation whose condition holds only on an intermediate state does not happen: with a lower default that
 *       decides first ({@link #a_creation_that_applies_only_on_a_lower_default_does_not_happen}); in the smallest
 *       shape the verifier admits where the condition reads its own output, where a logical object kept itself
 *       standing by its own count once the state that made it had passed
 *       ({@link #a_creation_that_reads_its_own_output_does_not_sustain_itself}); and while the rule layer
 *       reacts to an object a creation made ({@link #a_creation_does_not_fire_while_the_rule_layer_reacts_to_a_creation}),
 *       where with a condition read positively the creation does not even get a match on that state
 *       ({@link #an_object_creation_gets_after_a_creation_phase_no_match_on_an_intermediate_state}).</li>
 *   <li>A recursive chain that WF-045 admits stops where its condition no longer holds, an initialisation keeps the
 *       value it had when the object was made, and a navigation through the created relation reads the created link
 *       ({@link #recursive_chain_stops_where_the_condition_not_more_applies}).</li>
 *   <li>A fact creation makes each link once, beside a link of the input
 *       ({@link #a_fact_creation_lays_every_link_one_time}), and a second distinct filler of a to-one role is a fault at
 *       the fixpoint ({@link #a_second_filling_of_a_to_one_role_is_a_fault}).</li>
 *   <li>An object made in a phase is the subject of a creation only in a later phase, after the rule layer has reached
 *       a fixpoint over it, and a chain that does not stop meets the creation budget at its mint at depth 101
 *       ({@link #an_object_is_only_a_subject_in_the_next_generation}); a fact creation walks a link only in a later
 *       phase than the one that made it ({@link #a_link_is_only_a_path_in_the_next_generation}); the corpus pins the
 *       two sides of the budget ({@link #the_creation_budget_lies_where_the_corpus_pins_it}).</li>
 *   <li>The three wrakboeking documents play ({@link #the_wrakboekingen_play}).</li>
 * </ul>
 *
 * <p><b>The other executors</b> (2026-10-08). Every document written here passes the verifier. The reference
 * interpreter and the ASP backend give the expectations of A, A2, B, D1, G and H. On D2 both fault: "to-one role "haven"
 * on "J1" already links H1; a second filler is ill-typed (ch. 5.5)" and "UNSATISFIABLE — a fail-fast guard fired:
 * viol("to-one second filler on role haven","J1")". On F both fault: "no fixpoint within 100 passes — divergence is a
 * fault (ch. 5.7)" and "UNSATISFIABLE — a fail-fast guard fired: no viol/2 witness (the input is not stratified?)". On C
 * the reference interpreter gives the expectations, and the ASP backend answers opvolgerbegin(W1) = 1: its stable model
 * evaluates the initialisation of the beginstand against the final stand of W1, where ch. 5.5 and the reference
 * interpreter evaluate it when the object is made, at stand 0; a split of the class RUNTIME-SEMANTICS ch. 6.1 records as
 * "a value that grows", on which this branch follows the reference interpreter. Measured again on 2026-10-09 (cards
 * creatie-initialisatie-leest-groeiende-waarde and creatiebudget-ref-en-clingo): the reference interpreter counts the
 * creation depth and says on F "creation budget exhausted: object creation "werf" of stratum 0 would mint at creation
 * depth 101, on oc0::…::W1 — …" (before the third step of the second card "no fixpoint within 100 generations —
 * generation 101 still mints or links (object creation "werf")"), the ASP backend as before; on C the ASP backend now refuses by
 * name ("the initialisation of beginstand reads attr:wachter:stand, which rule "stand van een wachter" writes on a
 * dependency cycle through the creation …").
 *
 * <p><b>Red before the change</b> (measured 2026-10-08 on the branch after its cycle refusal, {@code fdeb6dfb}): all
 * eleven failed at the build, which refused the creation, for instance "expected: &lt;gespeeld&gt; but was:
 * &lt;Excluded: tms-transpiler SKIP: objectcreatie: meld de vangst — een ObjectCreation: buiten de proef van de
 * truth-maintenance-tak&gt;", "… SKIP: feitcreatie: werving van havenjutters — een FactCreation: buiten de proef van de
 * truth-maintenance-tak", and for the corpus documents "jutterswet-wrakboeking: tms-transpiler SKIP: objectcreatie: boek
 * een strandrecht bij elk wrakrecht — … ==> expected: &lt;PLAYED&gt; but was: &lt;EXCLUDE&gt;". Mutants of the branch,
 * each caught: without the phase on the mark of a made object (F), without the mark per rule and subject (A), without
 * the agenda group of the object creations (every object creation test), with the creations in the main group beside
 * the rule layer (G, F, C and the budget), with a fact creation that does not test for the link it makes (D1, D2 and
 * the erflijn), and without the phase on a walked link (the erflijn). Measured on {@code 582b200e}: auto-focus on the
 * object creations (H by its match on the intermediate state, "expected: &lt;[]&gt; but was: &lt;[{aantal=1,
 * kopie=0}]&gt;"; C, F and the budget by the phase), and an eager object creation without auto-focus, no-loop (H, the
 * same match).
 *
 * <p><b>The samples of document A on the text</b> (card drl-teksttak-op-truth-maintenance, round 5b, on the lens of
 * round 5). The two samples where a lower default decides before a creation fire on the exec-model branch and on
 * {@code DRL_TEXT}, its analysis rendered as DRL text, with the rules of the text in the same orders
 * ({@code TextOrder}), since the order of the rules in the text is the order in which Drools loads them; each
 * branch meets the same demands, among them an order in which the lower default decides first. <b>Red</b> on
 * {@code b4d83a69}, before the rendering carried the creations: both failed at the replay, "DRL_TMS speelt … anders
 * dan de exec-model-tak ==&gt; expected: &lt;gespeeld&gt; but was: &lt;Excluded: drl-tms: de DRL-weergave draagt
 * Creatie nog niet&gt;", and with the replay on {@code DRL_TMS} left out of a copy, at the sample on the text,
 * "Excluded drl-tms: de DRL-weergave draagt Creatie nog niet".
 *
 * <p><b>Every other sample on the text</b> (fix round 5b, in step 6a, on the lens of round 5b): those of documents B,
 * G, H, C, D (both cases), F and the erflijn fire on both branches as well, each branch with the same demands; their
 * intermediate states, a fault and a phase can depend on the order in which the rules are loaded while the replay in
 * the order of the document passes. On the text a sample takes the first {@link TmsDocuments#TEXT_SHUFFLES}
 * shuffles of the seed beside the order of the document and its reverse ({@link TmsDocuments#shuffles}), since
 * every order there compiles a DRL text of its own; the two samples of document A keep their hundred.
 */
class TmsCreationWitnessTest {

    private static final Path CORPUS = Path.of("..", "..", "corpus");

    // ---------------- document A: one object per subject, with the values of the fixpoint ----------------

    private static final String MELDINGEN = "{\"node\":\"AttributeReference\",\"attribute\":\"meldingen\",\"subjectType\":"
            + "\"Jutter\",\"source\":{\"snippet\":\"meldingen van de Jutter\"}}";

    private static final String MODEL_A = TmsDocuments.model(List.of(
            TmsDocuments.objectType("Jutter", List.of(attribute("klasse", WHOLE), attribute("vangst", WHOLE),
                    attribute("meldingaantal", WHOLE), attribute("meldingswaarde", WHOLE)), List.of("gemeld")),
            TmsDocuments.objectType("Melding", List.of(attribute("waarde", WHOLE)), List.of())),
            List.of(factTypeOneToMany("jutmelding", "melder", "Jutter", "melding", "Melding", "meldingen")));

    private static final List<String> RULES_A = List.of(
            computingRule("klasse van een jutter", "GELIJKSTELLING", "Jutter", "klasse", number("1"), "null"),
            computingRule("meldingaantal", "BEREKENING", "Jutter", "meldingaantal", aggregation("count", MELDINGEN, "Melding",
                    sourceRole("jutmelding", "melding"), "null", "null", COUNTING, "het aantal meldingen van de Jutter"),
                    "null"),
            computingRule("meldingswaarde", "BEREKENING", "Jutter", "meldingswaarde", aggregation("sum",
                    ownRead("waarde", "Melding"), "Melding", sourceRole("jutmelding", "melding"), "null", number("0"), WHOLE,
                    "de som van de waarde van alle meldingen van de Jutter of 0 als die er niet zijn"), "null"));

    /** The vangst: 7 where the klasse, which a rule sets, is 1, and else 3 for a gemeld Jutter; the lower default can
     *  decide before the rule has set the klasse. */
    private static final String VANGST = attributeGroup("vangst van een jutter", "Jutter", "vangst", List.of(
            defaultEntry(comparison("equal", ownRead("klasse", "Jutter"), number("1")), number("7")),
            defaultEntry(check("gemeld", false), number("3"))));

    static Path documentA(String condition) throws Exception {
        String naam = condition.equals("null") ? "meld de vangst" : "meld een lage vangst";
        return document(MODEL_A, RULES_A, List.of(VANGST),
                List.of(objectCreation(naam, "Jutter", "Melding", List.of(init("waarde", ownRead("vangst", "Jutter"))),
                        condition, "jutmelding", "melding")), List.of(), List.of());
    }

    static JsonNode caseA(String count, String sum) throws Exception {
        return TmsDocuments.caseDocument(List.of(instanceWithRoles("J1", "Jutter", Map.of(), List.of("gemeld"), Map.of())),
                List.of(value("J1", "meldingaantal", count), value("J1", "meldingswaarde", sum)));
    }

    private static final String LAGE_VANGST = comparison("less", ownRead("vangst", "Jutter"), number("5"));

    @Test void an_object_creation_makes_an_object_per_subject_with_the_values_of_the_fixpoint() throws Exception {
        Path ir = documentA("null");
        assertEquals("gespeeld", TmsFaultWitnessTest.playBoth(ir, caseA("1", "7")));
        for (DroolsBackend.Branch branch : TmsDocuments.BRANCHES) {
            DroolsBackend.Build b = DroolsBackend.build(ir, branch);
            int intermediateState = 0;
            for (List<String> names : TmsDocuments.sample(TmsDocuments.generated(b), 20261008L, 100)) {
                String where = " op " + branch + " in volgorde " + names;
                Object j1 = TmsDocuments.beanObject(b.beanLoader(), "Jutter", Map.of(), List.of("gemeld"));
                int[] lower = {0};
                List<Object[]> made = new ArrayList<>();
                assertNull(fireInOrder(b, names, List.of(j1),
                        session -> session.addEventListener(valueCounter("vangst", "3", lower)),
                        session -> made.addAll(made(session))), () -> "de vuring" + where);
                assertNumber("1", TmsDocuments.read(j1, "meldingaantal"), "meldingaantal" + where);
                assertNumber("7", TmsDocuments.read(j1, "meldingswaarde"), "meldingswaarde" + where);
                assertEquals(1, made.size(), () -> "objecten" + where);
                assertTrue(made.get(0)[0] == j1, () -> "onderwerp" + where);
                if (lower[0] > 0) intermediateState++;
            }
            assertTrue(intermediateState > 0, branch + ": geen volgorde liet de lagere default eerst beslissen");
        }
    }

    @Test void a_creation_that_applies_only_on_a_lower_default_does_not_happen() throws Exception {
        Path ir = documentA(LAGE_VANGST);
        assertEquals("gespeeld", TmsFaultWitnessTest.playBoth(ir, caseA("0", "0")));
        for (DroolsBackend.Branch branch : TmsDocuments.BRANCHES) {
            DroolsBackend.Build b = DroolsBackend.build(ir, branch);
            int intermediateState = 0;
            for (List<String> names : TmsDocuments.sample(TmsDocuments.generated(b), 20261008L, 100)) {
                String where = " op " + branch + " in volgorde " + names;
                Object j1 = TmsDocuments.beanObject(b.beanLoader(), "Jutter", Map.of(), List.of("gemeld"));
                int[] lower = {0};
                List<Object[]> made = new ArrayList<>();
                assertNull(fireInOrder(b, names, List.of(j1),
                        session -> session.addEventListener(valueCounter("vangst", "3", lower)),
                        session -> made.addAll(made(session))), () -> "de vuring" + where);
                assertNumber("0", TmsDocuments.read(j1, "meldingaantal"), "meldingaantal" + where);
                assertEquals(0, made.size(), () -> "objecten" + where);
                if (lower[0] > 0) intermediateState++;
            }
            assertTrue(intermediateState > 0, branch + ": geen volgorde liet de lagere default eerst beslissen");
        }
    }

    // ---------------- document B: a condition that reads its own output ----------------

    private static final String OPVOLGERS = "{\"node\":\"AttributeReference\",\"attribute\":\"opvolgers\",\"subjectType\":"
            + "\"Wachter\",\"source\":{\"snippet\":\"opvolgers van de Wachter\"}}";

    private static final String COUNT_OPVOLGERS = aggregation("count", OPVOLGERS, "Wachter",
            sourceRole("opvolging", "opvolger"), "null", "null", COUNTING, "het aantal opvolgers van de Wachter");

    /** The smallest shape of the advice on the branch (2026-10-08, part 1): a recursive creation whose condition holds
     *  where the rang is not 5, or where the Wachter has an opvolger already. At the fixpoint the rang is 5 and there
     *  is no opvolger; a lower default gives the rang 0 first in some orders. */
    static Path documentB() throws Exception {
        String model = TmsDocuments.model(List.of(TmsDocuments.objectType("Wachter", List.of(attribute("code", WHOLE),
                attribute("rang", WHOLE), attribute("opvolgeraantal", WHOLE)), List.of("startwachter"))),
                List.of(factTypeOneToMany("opvolging", "voorganger", "Wachter", "opvolger", "Wachter", "opvolgers")));
        List<String> rules = List.of(
                computingRule("code van een wachter", "GELIJKSTELLING", "Wachter", "code", number("1"), "null"),
                computingRule("opvolgeraantal", "BEREKENING", "Wachter", "opvolgeraantal", COUNT_OPVOLGERS, "null"));
        String rank = attributeGroup("rang van een wachter", "Wachter", "rang", List.of(
                defaultEntry(comparison("equal", ownRead("code", "Wachter"), number("1")), number("5")),
                defaultEntry(check("startwachter", false), number("0"))));
        String condition = compound("all", 0, List.of(check("startwachter", false), compound("atLeast", 1,
                List.of(comparison("notEqual", ownRead("rang", "Wachter"), number("5")),
                        comparison("greater", COUNT_OPVOLGERS, number("0"))))));
        return document(model, rules, List.of(rank),
                List.of(objectCreation("werf", "Wachter", "Wachter", List.of(), condition, "opvolging", "opvolger")),
                List.of(), List.of(ruleGroup("estafette", List.of("werf"))));
    }

    static JsonNode caseB() throws Exception {
        return TmsDocuments.caseDocument(List.of(instanceWithRoles("W1", "Wachter", Map.of(), List.of("startwachter"), Map.of())),
                List.of(value("W1", "opvolgeraantal", "0")));
    }

    @Test void a_creation_that_reads_its_own_output_does_not_sustain_itself() throws Exception {
        Path ir = documentB();
        assertEquals("gespeeld", TmsFaultWitnessTest.playBoth(ir, caseB()));
        for (DroolsBackend.Branch branch : TmsDocuments.BRANCHES) {
            DroolsBackend.Build b = DroolsBackend.build(ir, branch);
            int intermediateState = 0;
            for (List<String> names : TmsDocuments.sample(TmsDocuments.generated(b), 20261008L,
                    TmsDocuments.shuffles(branch, 100))) {
                String where = " op " + branch + " in volgorde " + names;
                Object w1 = TmsDocuments.beanObject(b.beanLoader(), "Wachter", Map.of(), List.of("startwachter"));
                int[] lower = {0};
                List<Object[]> made = new ArrayList<>();
                assertNull(fireInOrder(b, names, List.of(w1),
                        session -> session.addEventListener(valueCounter("rang", "0", lower)),
                        session -> made.addAll(made(session))), () -> "de vuring" + where);
                assertNumber("0", TmsDocuments.read(w1, "opvolgeraantal"), "opvolgeraantal" + where);
                assertNumber("5", TmsDocuments.read(w1, "rang"), "rang" + where);
                assertEquals(0, made.size(), () -> "objecten" + where);
                if (lower[0] > 0) intermediateState++;
            }
            assertTrue(intermediateState > 0, branch + ": geen volgorde liet de lagere default eerst beslissen");
        }
    }

    // ---------------- document G: the rule layer after a creation ----------------

    private static final String WARNINGS = "{\"node\":\"AttributeReference\",\"attribute\":\"waarschuwingen\","
            + "\"subjectType\":\"Jutter\",\"source\":{\"snippet\":\"waarschuwingen van de Jutter\"}}";

    /** "meld" makes one Melding per Jutter; the aantal counts the meldingen and the kopie copies the aantal, so the two
     *  differ only between those two rules, when the Melding has arrived; "waarschuw" makes a Waarschuwing where they
     *  differ. At every fixpoint of the rule layer they are equal. */
    static Path documentG() throws Exception {
        return warnDocument(comparison("notEqual", ownRead("aantal", "Jutter"), ownRead("kopie", "Jutter")));
    }

    /** The model and rules of G, with {@code condition} as the condition of "waarschuw". */
    private static Path warnDocument(String condition) throws Exception {
        String model = TmsDocuments.model(List.of(
                TmsDocuments.objectType("Jutter", List.of(attribute("aantal", WHOLE), attribute("kopie", WHOLE),
                        attribute("waarschuwingaantal", WHOLE)), List.of()),
                TmsDocuments.objectType("Melding", List.of(), List.of()),
                TmsDocuments.objectType("Waarschuwing", List.of(), List.of())), List.of(
                factTypeOneToMany("jutmelding", "melder", "Jutter", "melding", "Melding", "meldingen"),
                factTypeOneToMany("jutwaarschuwing", "gewaarschuwde", "Jutter", "waarschuwing", "Waarschuwing",
                        "waarschuwingen")));
        List<String> rules = List.of(
                computingRule("aantal", "BEREKENING", "Jutter", "aantal", aggregation("count", MELDINGEN, "Melding",
                        sourceRole("jutmelding", "melding"), "null", "null", COUNTING, "het aantal meldingen van de Jutter"),
                        "null"),
                computingRule("kopie", "GELIJKSTELLING", "Jutter", "kopie", ownRead("aantal", "Jutter"), "null"),
                computingRule("waarschuwingaantal", "BEREKENING", "Jutter", "waarschuwingaantal", aggregation("count",
                        WARNINGS, "Waarschuwing", sourceRole("jutwaarschuwing", "waarschuwing"), "null", "null",
                        COUNTING, "het aantal waarschuwingen van de Jutter"), "null"));
        return document(model, rules, List.of(), List.of(
                objectCreation("meld", "Jutter", "Melding", List.of(), "null", "jutmelding", "melding"),
                objectCreation("waarschuw", "Jutter", "Waarschuwing", List.of(), condition, "jutwaarschuwing",
                        "waarschuwing")), List.of(), List.of());
    }

    static JsonNode caseG() throws Exception {
        return TmsDocuments.caseDocument(List.of(instanceWithRoles("J1", "Jutter", Map.of(), List.of(), Map.of())),
                List.of(value("J1", "aantal", "1"), value("J1", "kopie", "1"), value("J1", "waarschuwingaantal", "0")));
    }

    @Test void a_creation_does_not_fire_while_the_rule_layer_reacts_to_a_creation() throws Exception {
        Path ir = documentG();
        assertEquals("gespeeld", TmsFaultWitnessTest.playBoth(ir, caseG()));
        for (DroolsBackend.Branch branch : TmsDocuments.BRANCHES) {
            DroolsBackend.Build b = DroolsBackend.build(ir, branch);
            int intermediateState = 0;
            for (List<String> names : TmsDocuments.sample(TmsDocuments.generated(b), 20261008L,
                    TmsDocuments.shuffles(branch, 100))) {
                String where = " op " + branch + " in volgorde " + names;
                Object j1 = TmsDocuments.beanObject(b.beanLoader(), "Jutter", Map.of(), List.of());
                List<String> valuesOrder = new ArrayList<>();
                List<Object[]> made = new ArrayList<>();
                assertNull(fireInOrder(b, names, List.of(j1), session -> session.addEventListener(
                        new DefaultRuleRuntimeEventListener() {
                            @Override public void objectInserted(ObjectInsertedEvent e) {
                                if (e.getObject() instanceof Value w && w.value() instanceof BigDecimal x
                                        && x.compareTo(BigDecimal.ONE) == 0)
                                    valuesOrder.add(w.attribute().naam().toLowerCase(java.util.Locale.ROOT));
                            }
                        }), session -> made.addAll(made(session))), () -> "de vuring" + where);
                assertNumber("0", TmsDocuments.read(j1, "waarschuwingaantal"), "waarschuwingaantal" + where);
                assertEquals(1, made.size(), () -> "objecten" + where);
                // the aantal became 1 while the kopie was still 0: the state on which "waarschuw" would hold
                if (valuesOrder.indexOf("aantal") >= 0 && valuesOrder.indexOf("aantal")
                        < valuesOrder.indexOf("kopie")) intermediateState++;
            }
            assertTrue(intermediateState > 0, branch + ": geen volgorde liet het aantal voor de kopie veranderen");
        }
    }

    // ---------------- document H: the rule layer after a creation phase, read positively ----------------

    /** G with the condition of "waarschuw" read positively, "het aantal van de Jutter is groter dan de kopie van de
     *  Jutter": a comparison with a leeg operand does not hold (RUNTIME-SEMANTICS ch. 1.2), so it holds only where the
     *  aantal has grown past a kopie that is there, and the expected answer rests on no condition that holds on absence
     *  (ch. 6.1). At every fixpoint of the rule layer the two are equal, and the case expects G's answer. */
    static Path documentH() throws Exception {
        return warnDocument(comparison("greater", ownRead("aantal", "Jutter"), ownRead("kopie", "Jutter")));
    }

    /**
     * After a creation phase, while its {@code CreationPhase} stands, the rule layer passes a state on which "waarschuw"
     * holds and which no fixpoint has: "meld" has made the Melding, the aantal standing is 1 and the kopie standing still
     * 0. Measured per order, over working memory: the standings "waarschuw" reads, and every match Drools creates for
     * "waarschuw". In every order the state stood with a {@code CreationPhase} in the session, no Waarschuwing is made, and
     * "waarschuw" does not even get a match: Drools 8.44 evaluates a rule lazily, when the agenda takes it from the group
     * that has the focus, and the object creations' group has the focus only when the driver gives it, at a fixpoint of
     * the rule layer ({@code DroolsBackend.fire}).
     *
     * <p>This is the witness for auto-focus on the object creations, which G does not see. Auto-focus makes a rule eager
     * ({@code RuleImpl.setAutoFocus} sets the eager flag) and gives its group the focus where an evaluation creates a
     * match ({@code PhreakRuleTerminalNode.doLeftInserts}): "waarschuw" gets a match on the state above, in every order,
     * and this test fails on it. Measured on this document, the rule layer then fires on, the copy of the aantal included,
     * while the group has the focus, and the match is cancelled before it fires, so the mutant makes no Waarschuwing.
     * The answer it does change is the phase: an object creation that holds when a generation opens fires in the phase
     * of the fact creations, on which {@link #recursive_chain_stops_where_the_condition_not_more_applies},
     * {@link #an_object_is_only_a_subject_in_the_next_generation} and
     * {@link #the_creation_budget_lies_where_the_corpus_pins_it} fail.
     *
     * <p>Measured again on 2026-10-09. Since the creations fire per stratum (2026-10-08), "waarschuw" sits a stratum above
     * "meld" and binds no {@code CreationPhase} of meld's stratum, so it gets no match on the state above under the mutant
     * either, and this test passes on it; on {@code e15baf01} only the three tests named above failed in this class, on
     * the phase. Since a generation is one phase (2026-10-09) there is no phase of the fact creations to fire in, and
     * under the mutant every test of this class passes. The witness for it is now document R of
     * {@code TmsCreationStratumWitnessTest}
     * ({@code an_object_creation_that_the_rule_layer_makes_possible_fires_in_the_next_generation}): an object creation
     * whose condition holds once the rule layer has settled, and stays true, fires there under the mutant in the
     * generation before its own, and the next creation of the chain reads what it made.
     */
    @Test void an_object_creation_gets_after_a_creation_phase_no_match_on_an_intermediate_state() throws Exception {
        Path ir = documentH();
        assertEquals("gespeeld", TmsFaultWitnessTest.playBoth(ir, caseG()));
        for (DroolsBackend.Branch branch : TmsDocuments.BRANCHES) {
            DroolsBackend.Build b = DroolsBackend.build(ir, branch);
            assertTrue(b.execModelTranspiler().creationRules().contains("waarschuw"), () -> "creatieregels "
                    + b.execModelTranspiler().creationRules());
            for (List<String> names : TmsDocuments.sample(TmsDocuments.generated(b), 20261008L,
                    TmsDocuments.shuffles(branch, 100))) {
                String where = " op " + branch + " in volgorde " + names;
                Object j1 = TmsDocuments.beanObject(b.beanLoader(), "Jutter", Map.of(), List.of());
                IntermediateState state = new IntermediateState(j1);
                List<String> matches = new ArrayList<>();
                List<Object[]> made = new ArrayList<>();
                assertNull(fireInOrder(b, names, List.of(j1), session -> {
                    session.addEventListener(state);
                    session.addEventListener(new DefaultAgendaEventListener() {
                        @Override public void matchCreated(MatchCreatedEvent e) {
                            if (e.getMatch().getRule().getName().equals("waarschuw"))
                                matches.add(state.states.toString());
                        }
                    });
                }, session -> made.addAll(made(session))), () -> "de vuring" + where);
                assertEquals(1, made.size(), () -> "objecten" + where);
                assertNumber("0", TmsDocuments.read(j1, "waarschuwingaantal"), "waarschuwingaantal" + where);
                assertNumber("1", TmsDocuments.read(j1, "kopie"), "kopie" + where);
                assertEquals(List.of(), matches, () -> "matches van waarschuw, met de standen," + where);
                assertTrue(state.seen, () -> "geen aantal boven de kopie terwijl een Creatiefase stond," + where);
            }
        }
    }

    /** Follows the standings of {@code onderwerp} and the {@link CreationPhase} facts through working memory, and records
     *  whether the aantal standing stood above the kopie standing, both gevuld, while a {@code CreationPhase} did: the
     *  state on which "waarschuw" of document H holds. */
    private static final class IntermediateState extends DefaultRuleRuntimeEventListener {
        private final Object onderwerp;
        final Map<String, Object> states = new java.util.TreeMap<>();
        private int phases;
        boolean seen;

        IntermediateState(Object onderwerp) { this.onderwerp = onderwerp; }

        @Override public void objectInserted(ObjectInsertedEvent e) {
            if (e.getObject() instanceof CreationPhase) phases++;
            else if (e.getObject() instanceof State s && s.onderwerp() == onderwerp) states.put(naam(s), s.value());
            look();
        }

        @Override public void objectDeleted(ObjectDeletedEvent e) {
            if (e.getOldObject() instanceof CreationPhase) phases--;
            else if (e.getOldObject() instanceof State s && s.onderwerp() == onderwerp
                    && java.util.Objects.equals(states.get(naam(s)), s.value())) states.remove(naam(s));
            look();
        }

        private void look() {
            if (phases > 0 && states.get("aantal") instanceof BigDecimal a && states.get("kopie") instanceof BigDecimal k
                    && a.compareTo(k) > 0) seen = true;
        }

        private static String naam(State s) {
            return s.attribute().naam().toLowerCase(java.util.Locale.ROOT);
        }
    }

    // ---------------- document C: a recursive chain that stops ----------------

    /** "Eén voorganger heeft één opvolger", the opvolger role with its plural, as a count over it spells its source. */
    private static final String ONE_SUCCESSOR = "{\"node\":\"FactType\",\"name\":\"opvolging\",\"reciprocal\":false,"
            + "\"roles\":[{\"node\":\"Role\",\"name\":\"voorganger\",\"objectType\":\"Wachter\",\"many\":false},"
            + "{\"node\":\"Role\",\"name\":\"opvolger\",\"objectType\":\"Wachter\",\"plural\":\"opvolgers\","
            + "\"many\":false}]}";

    /** A chain of Wachters, each made by its voorganger while that one's rang is below 4, with the rang one higher and
     *  the beginstand the stand of the voorganger when it makes the opvolger; the stand counts the opvolgers, so the
     *  voorganger's stand becomes 1 right after, and opvolgerbegin reads the beginstand through the created link. */
    static Path documentC() throws Exception {
        String model = TmsDocuments.model(List.of(TmsDocuments.objectType("Wachter", List.of(attribute("rang", WHOLE),
                attribute("stand", WHOLE), attribute("beginstand", WHOLE), attribute("opvolgerbegin", WHOLE)),
                List.of())), List.of(ONE_SUCCESSOR));
        List<String> rules = List.of(
                computingRule("stand van een wachter", "BEREKENING", "Wachter", "stand", COUNT_OPVOLGERS, "null"),
                computingRule("opvolgerbegin", "GELIJKSTELLING", "Wachter", "opvolgerbegin",
                        pathRead("beginstand", "Wachter", "opvolging", "opvolger"), "null"));
        String werf = objectCreation("werf de opvolger", "Wachter", "Wachter", List.of(
                init("rang", binary("plus", ownRead("rang", "Wachter"), number("1"))),
                init("beginstand", ownRead("stand", "Wachter"))),
                comparison("less", ownRead("rang", "Wachter"), number("4")), "opvolging", "opvolger");
        return document(model, rules, List.of(), List.of(werf), List.of(),
                List.of(ruleGroup("estafette", List.of("werf de opvolger"))));
    }

    static JsonNode caseC() throws Exception {
        return TmsDocuments.caseDocument(List.of(instanceWithRoles("W1", "Wachter", Map.of("rang", "1"), List.of(), Map.of())),
                List.of(value("W1", "stand", "1"), value("W1", "opvolgerbegin", "0")));
    }

    @Test void recursive_chain_stops_where_the_condition_not_more_applies() throws Exception {
        Path ir = documentC();
        assertEquals("gespeeld", TmsFaultWitnessTest.playBoth(ir, caseC()));
        for (DroolsBackend.Branch branch : TmsDocuments.BRANCHES) {
            DroolsBackend.Build b = DroolsBackend.build(ir, branch);
            for (List<String> names : TmsDocuments.sample(TmsDocuments.generated(b), 20261008L,
                    TmsDocuments.shuffles(branch, 100))) {
                String where = " op " + branch + " in volgorde " + names;
                Object w1 = TmsDocuments.beanObject(b.beanLoader(), "Wachter", Map.of("rang", "1"), List.of());
                List<Object[]> made = new ArrayList<>();
                assertNull(fireInOrder(b, names, List.of(w1), session -> { },
                        session -> made.addAll(made(session))), () -> "de vuring" + where);
                assertNumber("1", TmsDocuments.read(w1, "stand"), "stand van W1" + where);
                assertNumber("0", TmsDocuments.read(w1, "opvolgerbegin"), "opvolgerbegin van W1" + where);
                // one phase per generation: W1 makes W2 in phase 1, W2 makes W3 in phase 2, W3 makes W4 in phase 3,
                // and W4, of rang 4, makes nothing
                assertEquals(3, made.size(), () -> "objecten" + where);
                Object onderwerp = w1;
                for (int i = 0; i < 3; i++) {
                    Object[] g = made.get(i);
                    int phase = i + 1;
                    assertTrue(g[0] == onderwerp, () -> "keten" + where);
                    assertEquals(phase, g[2], () -> "fase" + where);
                    assertNumber(String.valueOf(i + 2), TmsDocuments.read(g[1], "rang"), "rang" + where);
                    assertNumber("0", TmsDocuments.read(g[1], "beginstand"), "beginstand" + where);
                    onderwerp = g[1];
                }
            }
        }
    }

    // ---------------- document D: a fact creation ----------------

    /** "Een havenjutter van een Haven is een geworven jutter van de kustwacht van de Haven": each Jutter has at most
     *  one haven, so a Jutter that the kustwacht of two havens recruits gets two. */
    static Path documentD() throws Exception {
        String model = TmsDocuments.model(List.of(
                TmsDocuments.objectType("Kustwacht", List.of(), List.of()),
                TmsDocuments.objectType("Haven", List.of(attribute("jutteraantal", WHOLE)), List.of()),
                TmsDocuments.objectType("Jutter", List.of(), List.of())), List.of(
                factTypeOneToMany("bewaking", "kustwacht", "Kustwacht", "bewaakte haven", "Haven", "bewaakte havens"),
                factTypeOneToMany("werving", "werver", "Kustwacht", "geworven jutter", "Jutter", "geworven jutters"),
                factTypeOneToMany("havenlid", "haven", "Haven", "havenjutter", "Jutter", "havenjutters")));
        String havenjutters = "{\"node\":\"AttributeReference\",\"attribute\":\"havenjutters\",\"subjectType\":\"Haven\","
                + "\"source\":{\"snippet\":\"havenjutters van de Haven\"}}";
        List<String> rules = List.of(computingRule("jutteraantal", "BEREKENING", "Haven", "jutteraantal",
                aggregation("count", havenjutters, "Jutter", sourceRole("havenlid", "havenjutter"), "null", "null", COUNTING,
                        "het aantal havenjutters van de Haven"), "null"));
        String werving = factCreation("werving van havenjutters", "havenlid", "haven", "havenjutter", "Haven",
                List.of(roleBinding("bewaking", "kustwacht"), roleBinding("werving", "geworven jutter")), "null");
        return document(model, rules, List.of(), List.of(), List.of(werving), List.of());
    }

    /** K1 guards H1 and recruits J1 and J2; the input has J1 in H1 already. */
    static JsonNode caseD1() throws Exception {
        return TmsDocuments.caseDocument(List.of(
                instanceWithRoles("K1", "Kustwacht", Map.of(), List.of(),
                        Map.of("bewaakte havens", List.of("H1"), "geworven jutters", List.of("J1", "J2"))),
                instanceWithRoles("H1", "Haven", Map.of(), List.of(), Map.of("havenjutters", List.of("J1"))),
                instanceWithRoles("J1", "Jutter", Map.of(), List.of(), Map.of()),
                instanceWithRoles("J2", "Jutter", Map.of(), List.of(), Map.of())),
                List.of(value("H1", "jutteraantal", "2")));
    }

    /** K1 guards H1 and H2 and recruits J1: J1 would be a havenjutter of both. */
    static JsonNode caseD2() throws Exception {
        return TmsDocuments.caseDocument(List.of(
                instanceWithRoles("K1", "Kustwacht", Map.of(), List.of(),
                        Map.of("bewaakte havens", List.of("H1", "H2"), "geworven jutters", List.of("J1"))),
                instanceWithRoles("H1", "Haven", Map.of(), List.of(), Map.of()),
                instanceWithRoles("H2", "Haven", Map.of(), List.of(), Map.of()),
                instanceWithRoles("J1", "Jutter", Map.of(), List.of(), Map.of())),
                List.of(value("H1", "jutteraantal", "1")));
    }

    @Test void a_fact_creation_lays_every_link_one_time() throws Exception {
        Path ir = documentD();
        assertEquals("gespeeld", TmsFaultWitnessTest.playBoth(ir, caseD1()));
        for (DroolsBackend.Branch branch : TmsDocuments.BRANCHES) {
            DroolsBackend.Build b = DroolsBackend.build(ir, branch);
            for (List<String> names : TmsDocuments.sample(TmsDocuments.generated(b), 20261008L,
                    TmsDocuments.shuffles(branch, 100))) {
                String where = " op " + branch + " in volgorde " + names;
                Object k1 = TmsDocuments.beanObject(b.beanLoader(), "Kustwacht", Map.of(), List.of());
                Object h1 = TmsDocuments.beanObject(b.beanLoader(), "Haven", Map.of(), List.of());
                Object j1 = TmsDocuments.beanObject(b.beanLoader(), "Jutter", Map.of(), List.of());
                Object j2 = TmsDocuments.beanObject(b.beanLoader(), "Jutter", Map.of(), List.of());
                setRole(h1, "kustwacht", k1);
                setRole(j1, "werver", k1);
                setRole(j2, "werver", k1);
                setRole(j1, "haven", h1);
                List<Object[]> left = new ArrayList<>();
                assertNull(fireInOrder(b, names, List.of(k1, h1, j1, j2), session -> { },
                        session -> left.addAll(links(session, "havenlid"))), () -> "de vuring" + where);
                assertNumber("2", TmsDocuments.read(h1, "jutteraantal"), "jutteraantal" + where);
                // the link of the input, and the one link the creation made, in the phase of generation 1; the
                // creation fired once
                assertEquals(2, left.size(), () -> "links" + where);
                for (Object[] l : left) {
                    assertTrue(l[0] == h1, () -> "haven" + where);
                    assertEquals(l[1] == j1 ? 0 : 1, l[2], () -> "fase" + where);
                }
            }
        }
    }

    @Test void a_second_filling_of_a_to_one_role_is_a_fault() throws Exception {
        Path ir = documentD();
        String sentence = "IllegalStateException: de naar-één-rol \"haven\" van een Jutter heeft twee verschillende vullingen;"
                + " een tweede vulling is een fout, geen overschrijving (RUNTIME-SEMANTICS ch. 5.5)";
        assertEquals(sentence, TmsFaultWitnessTest.playBoth(ir, caseD2()));
        for (DroolsBackend.Branch branch : TmsDocuments.BRANCHES) {
            DroolsBackend.Build b = DroolsBackend.build(ir, branch);
            for (List<String> names : TmsDocuments.sample(TmsDocuments.generated(b), 20261008L,
                    TmsDocuments.shuffles(branch, 100))) {
                Object k1 = TmsDocuments.beanObject(b.beanLoader(), "Kustwacht", Map.of(), List.of());
                Object h1 = TmsDocuments.beanObject(b.beanLoader(), "Haven", Map.of(), List.of());
                Object h2 = TmsDocuments.beanObject(b.beanLoader(), "Haven", Map.of(), List.of());
                Object j1 = TmsDocuments.beanObject(b.beanLoader(), "Jutter", Map.of(), List.of());
                setRole(h1, "kustwacht", k1);
                setRole(h2, "kustwacht", k1);
                setRole(j1, "werver", k1);
                String fault = null;
                try {
                    fireInOrder(b, names, List.of(k1, h1, h2, j1), session -> { }, session -> { });
                } catch (IllegalStateException e) {
                    fault = "IllegalStateException: " + e.getMessage();
                }
                assertEquals(sentence, fault, () -> "op " + branch + " in volgorde " + names);
            }
        }
    }

    // ---------------- document F: one generation at a time, and the budget ----------------

    /** A chain that does not stop: every Wachter without an opvolger makes one. The condition reads only the count,
     *  which an accumulate over no contribution answers at once, so a new Wachter would match it in the phase that made
     *  it if the phase did not hold it back. Each generation makes one Wachter on the last, at one depth more, so the
     *  101st generation would make one at depth 101, and there the creation budget ends the run, at the making
     *  (RUNTIME-SEMANTICS ch. 5.5). */
    static Path documentF() throws Exception {
        String model = TmsDocuments.model(List.of(TmsDocuments.objectType("Wachter", List.of(
                attribute("opvolgeraantal", WHOLE)), List.of())),
                List.of(factTypeOneToMany("opvolging", "voorganger", "Wachter", "opvolger", "Wachter", "opvolgers")));
        List<String> rules = List.of(
                computingRule("opvolgeraantal", "BEREKENING", "Wachter", "opvolgeraantal", COUNT_OPVOLGERS, "null"));
        return document(model, rules, List.of(), List.of(objectCreation("werf", "Wachter", "Wachter", List.of(),
                comparison("equal", COUNT_OPVOLGERS, number("0")), "opvolging", "opvolger")), List.of(),
                List.of(ruleGroup("estafette", List.of("werf"))));
    }

    static JsonNode caseF() throws Exception {
        return TmsDocuments.caseDocument(List.of(instanceWithRoles("W1", "Wachter", Map.of(), List.of(), Map.of())),
                List.of(value("W1", "opvolgeraantal", "1")));
    }

    private static final String BUDGET = TmsCreationStratumWitnessTest.budget("werf", 0, "Wachter");

    /** Until 2026-10-09 the driver counted generations, and the 101st still made its Wachter before the run ended
     *  without a fixpoint, "geen fixpunt binnen 100 generaties: generatie 101 maakt nog 1 object of link", 101 objects
     *  made; the mint at depth 101 is now the fault at the making, and 100 objects are made. */
    @Test void an_object_is_only_a_subject_in_the_next_generation() throws Exception {
        Path ir = documentF();
        assertEquals("case: " + BUDGET, TmsFaultWitnessTest.playBoth(ir, caseF()));
        for (DroolsBackend.Branch branch : TmsDocuments.BRANCHES) {
            DroolsBackend.Build b = DroolsBackend.build(ir, branch);
            for (List<String> names : TmsDocuments.sample(TmsDocuments.generated(b), 20261008L,
                    TmsDocuments.shuffles(branch, 100))) {
                String where = " op " + branch + " in volgorde " + names;
                Object w1 = TmsDocuments.beanObject(b.beanLoader(), "Wachter", Map.of(), List.of());
                List<Object[]> made = new ArrayList<>();
                assertEquals(BUDGET, fireInOrder(b, names, List.of(w1), session -> { },
                        session -> made.addAll(made(session))), () -> "de vuring" + where);
                // one object in each phase, the subject of each the object of the one before
                assertEquals(100, made.size(), () -> "objecten" + where);
                Object onderwerp = w1;
                for (int i = 0; i < made.size(); i++) {
                    Object[] g = made.get(i);
                    int phase = i + 1;
                    assertTrue(g[0] == onderwerp, () -> "keten" + where);
                    assertEquals(phase, g[2], () -> "fase" + where);
                    onderwerp = g[1];
                }
            }
        }
    }

    /** A fact creation walks a link only in a later phase than the one that made it, after the rule layer has reached a
     *  fixpoint over it, as an object waits to be a subject. The golden {@code anchor/jutterswet-erflijn} builds the
     *  erflijn over a chain W1 → W2 → W3 → W4 of opvolgers: "erf de directe opvolger" links each Wachter to its
     *  opvolger, and the recursive "erf de lijn door" walks an erflijn link and then an opvolger. "erf de lijn door"
     *  reads the erflijn links "erf de directe opvolger" writes, strictly, so it sits a stratum above it (RUNTIME-SEMANTICS
     *  ch. 6.1, 6.3). One phase per generation: the links of the direct opvolgers arise in stratum 0's first generation
     *  (phase 1), which its second generation, making nothing, closes (phase 2); those two steps away arise in stratum
     *  1's first generation and W1 → W4 in its second: phases 1, 3 and 4. Until the creations fired per stratum
     *  (2026-10-08) they were phases 1, 3 and 5, and until a generation became one phase (2026-10-09) phases 1, 5 and
     *  7. */
    @Test void a_link_is_only_a_path_in_the_next_generation() throws Exception {
        Path d = CORPUS.resolve("golden").resolve("anchor").resolve("jutterswet-erflijn");
        assertEquals("PLAYED", CaseRunner.viaExecModel(d.resolve("ir.json"), List.of(d.resolve("case.json")))
                .status());
        assertEquals("PLAYED", CaseRunner.viaDrlText(d.resolve("ir.json"), List.of(d.resolve("case.json"))).status());
        for (DroolsBackend.Branch branch : TmsDocuments.BRANCHES) {
            DroolsBackend.Build b = DroolsBackend.build(d.resolve("ir.json"), branch);
            for (List<String> names : TmsDocuments.sample(TmsDocuments.generated(b), 20261008L,
                    TmsDocuments.shuffles(branch, 100))) {
                String where = " op " + branch + " in volgorde " + names;
                List<Object> w = new ArrayList<>();
                for (int i = 0; i < 4; i++) w.add(TmsDocuments.beanObject(b.beanLoader(), "Wachter", Map.of(), List.of()));
                for (int i = 0; i < 3; i++) {
                    setRole(w.get(i), "opvolger", w.get(i + 1));
                    setRole(w.get(i + 1), "voorganger", w.get(i));
                }
                List<Object[]> left = new ArrayList<>();
                assertNull(fireInOrder(b, names, w, session -> { },
                        session -> left.addAll(links(session, "erflijn"))), () -> "de vuring" + where);
                assertEquals(6, left.size(), () -> "links" + where);
                for (Object[] l : left) {
                    int afstand = w.indexOf(l[1]) - w.indexOf(l[0]);
                    assertEquals(afstand == 1 ? 1 : afstand + 1, l[2],
                            () -> "fase van een link over " + afstand + where);
                }
                for (int i = 0; i < 4; i++)
                    assertNumber(String.valueOf(3 - i), TmsDocuments.read(w.get(i), "erfvolgeraantal"),
                            "erfvolgeraantal van W" + (i + 1) + where);
            }
        }
    }

    /** The two sides of the budget the reference interpreter and the ASP backend pin: a chain of 97 mints plays
     *  ({@code synth2-budgetgrens-onder}), and the chains of the three red cases fault at the mint at depth 101
     *  ({@code corpus/red} {@code jutterswet-budgetgrens-over}, {@code finite-chain-over-budget} and
     *  {@code runaway-recursion}), with a sentence that names the creation, its stratum, the depth of the mint and the
     *  type and depth of its subject. The red cases pin the same place for the other two: the reference interpreter's
     *  fragment names the same creation, stratum and depth, and the ASP backend's names its witness on a mint of that
     *  creation, whose depth its own suite checks (the gate's fragment cannot carry a term of 101 levels). Until
     *  2026-10-09 the three executors pinned only the shared words "creation budget exhausted", which a fault on
     *  another object or at another depth satisfied as well. A red case pins no expectation, so it plays here with
     *  none. */
    @Test void the_creation_budget_lies_where_the_corpus_pins_it() throws Exception {
        Path under = CORPUS.resolve("golden").resolve("synth2-budgetgrens-onder");
        assertEquals("PLAYED", CaseRunner.viaExecModel(under.resolve("ir.json"),
                List.of(under.resolve("case1.json"))).status());
        for (Map.Entry<String, String> red : Map.of("jutterswet-budgetgrens-over", "werf de lange keten",
                "finite-chain-over-budget", "werf de lange keten", "runaway-recursion", "werf eeuwig").entrySet()) {
            Path d = CORPUS.resolve("red").resolve(red.getKey());
            JsonNode caseDocument = TmsDocuments.JSON.readTree(Files.readString(d.resolve("case1.json")));
            ((com.fasterxml.jackson.databind.node.ObjectNode) caseDocument).putArray("expected");
            String sentence = TmsFaultWitnessTest.playBoth(d.resolve("ir.json"), caseDocument);
            assertEquals("case: " + TmsCreationStratumWitnessTest.budget(red.getValue(), 0, "Wachter"), sentence,
                    red.getKey());
            JsonNode pin = caseDocument.get("expectedFault");
            assertEquals("EF-015", pin.get("code").asText(), () -> red.getKey() + ": the pin is the creation budget");
            CaseRunner.RedResult red1 = CaseRunner.runRed(d.resolve("ir.json"), d.resolve("case1.json"));
            assertEquals(List.of(Map.of("creation", red.getValue(), "stratum", "0", "depth", "101")), red1.params(),
                    () -> red.getKey() + ": the parameters name the creation, the stratum and the depth of this branch");
        }
    }

    @Test void the_wrakboekingen_play() throws Exception {
        for (String g : List.of("jutterswet-wrakboeking", "jutterswet-wrakboeking-rgs", "jutterswet-wrakboekwaarde")) {
            Path d = CORPUS.resolve("golden").resolve(g);
            List<Path> cases;
            try (var s = Files.list(d)) {
                cases = s.filter(p -> p.getFileName().toString().matches("case.*\\.json")).sorted().toList();
            }
            CaseRunner.Result r = CaseRunner.viaExecModel(d.resolve("ir.json"), cases);
            assertEquals("PLAYED", r.status(), () -> g + ": " + r.reason() + " " + r.caseFailures());
        }
    }

    // ---------------- the wire of a creation ----------------

    /** A document with object creations, fact creations and rule groups beside the forms {@link TmsDocuments} writes. */
    static Path document(String model, List<String> rules, List<String> defaults, List<String> objectCreations,
                         List<String> factCreations, List<String> ruleGroups) throws Exception {
        String ir = "{\"format\":\"uchar-ir-v1\",\"model\":" + model + ",\"rules\":{\"rules\":[" + String.join(",", rules)
                + "],\"dayKindDefinitions\":[],\"objectCreations\":[" + String.join(",", objectCreations)
                + "],\"allocations\":[],\"consistencies\":[],\"uniquenesses\":[],\"factCreations\":["
                + String.join(",", factCreations) + "],\"startPointDeterminations\":[],\"ruleGroups\":["
                + String.join(",", ruleGroups) + "],\"prioritizedDefaults\":[" + String.join(",", defaults)
                + "]},\"diagnostics\":[]}";
        Path tmp = Files.createTempFile("tms-creatie", ".json");
        tmp.toFile().deleteOnExit();
        return Files.writeString(tmp, ir);
    }

    /** "Een {@code subject} heeft een {@code role} met …": an object creation of one always-valid version, its
     *  {@code connectingRole} the role {@code role} of {@code factType}, the role the created object plays. */
    static String objectCreation(String naam, String subject, String fresh, List<String> inits, String condition,
                                String factType, String role) {
        return "{\"node\":\"ObjectCreation\",\"name\":\"" + naam + "\",\"versions\":[{\"createdType\":\"" + fresh
                + "\",\"subject\":\"" + subject + "\",\"initialisations\":[" + String.join(",", inits) + "]"
                + (condition.equals("null") ? "" : ",\"condition\":" + condition)
                + ",\"validity\":{\"node\":\"Validity\"},\"connectingRole\":" + roleBinding(factType, role) + "}]}";
    }

    /** An initialisation of attribute {@code member} of the created object. */
    static String init(String member, String value) {
        return "{\"node\":\"MemberInit\",\"member\":\"" + member + "\",\"value\":" + value + ",\"characteristic\":false}";
    }

    /** "Een {@code targetRole} van een {@code anchor} is een … van …": a fact creation of one always-valid version over
     *  {@code path}, each step a RoleBinding. */
    static String factCreation(String naam, String factType, String anchorRole, String targetRole, String anchor, List<String> path,
                              String condition) {
        return "{\"node\":\"FactCreation\",\"name\":\"" + naam + "\",\"versions\":[{\"factType\":\"" + factType
                + "\",\"anchorRole\":\"" + anchorRole + "\",\"targetRole\":\"" + targetRole + "\",\"anchor\":\"" + anchor
                + "\",\"path\":[" + String.join(",", path) + "]"
                + (condition.equals("null") ? "" : ",\"condition\":" + condition) + ",\"validity\":{\"node\":\"Validity\"}}]}";
    }

    /** A recursive rule group of {@code members}. */
    static String ruleGroup(String naam, List<String> members) {
        List<String> l = new ArrayList<>();
        for (String member : members) l.add("\"" + member + "\"");
        return "{\"node\":\"RuleGroup\",\"name\":\"" + naam + "\",\"recursive\":true,\"members\":[" + String.join(",", l)
                + "],\"source\":{\"snippet\":\"" + naam + "\"}}";
    }

    // ---------------- firing through the driver ----------------

    /**
     * Fires {@code beans} through the rules of {@code b} added in {@code order}, with the backend's own driver
     * ({@code DroolsBackend.fire}, reached by name: this witness also compiles against the code before creation was
     * carried, whose build refuses these documents before any fire). {@code before} sees the session before the fire. The
     * fire must stay below the cap. At a fixpoint the derivations are written back and null is returned; at the
     * generation budget the driver's sentence. {@code after} sees the session either way, before it is disposed.
     */
    static String fire(DroolsBackend.Build b, List<Rule> order, List<Object> beans, Consumer<KieSession> before,
                       Consumer<KieSession> after) throws Exception {
        return fire(b, TmsDocuments.kieBase(b, order), order.stream().map(Rule::getName).toList(), beans, before,
                after);
    }

    /** {@link #fire(DroolsBackend.Build, List, List, Consumer, Consumer)} through the rules of {@code b} named in
     *  {@code names}, in that order, as its branch builds them ({@link TmsDocuments#kieBaseOf}): on the exec-model
     *  branch the rules of its executable model, on {@code DRL_TEXT} those of its text (round 5b). */
    static String fireInOrder(DroolsBackend.Build b, List<String> names, List<Object> beans,
                                 Consumer<KieSession> before, Consumer<KieSession> after) throws Exception {
        return fire(b, TmsDocuments.kieBaseOf(b, names), names, beans, before, after);
    }

    private static String fire(DroolsBackend.Build b, KieBase kieBase, List<String> names, List<Object> beans,
                               Consumer<KieSession> before, Consumer<KieSession> after) throws Exception {
        ClassLoader previous = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(b.beanLoader());
        KieSession session = kieBase.newKieSession();
        try {
            before.accept(session);
            for (Object beanObject : beans) session.insert(beanObject);
            Method m = DroolsBackend.class.getDeclaredMethod("fire", DroolsBackend.Build.class, KieSession.class, int.class);
            m.setAccessible(true);
            Object firing;
            try {
                firing = m.invoke(null, b, session, 100_000);
            } catch (InvocationTargetException e) {
                if (e.getCause() instanceof RuntimeException r) throw r;
                throw e;
            }
            Method firings = firing.getClass().getDeclaredMethod("firings");
            Method noFixpoint = firing.getClass().getDeclaredMethod("noFixpoint");
            firings.setAccessible(true);
            noFixpoint.setAccessible(true);
            int n = (Integer) firings.invoke(firing);
            String sentence = (String) noFixpoint.invoke(firing);
            if (n >= 100_000)
                throw new AssertionError(b.branch() + ": vuurplafond in volgorde " + names);
            if (sentence == null) DroolsBackend.setDerivedValues(b, session);
            after.accept(session);
            return sentence;
        } finally {
            session.dispose();
            Thread.currentThread().setContextClassLoader(previous);
        }
    }

    /** The marks of the object creations in {@code session} ({@code CreationStamp}, read by name), as {subject, object,
     *  phase}, in the order of their phase. */
    static List<Object[]> made(KieSession session) {
        List<Object[]> out = new ArrayList<>();
        for (Object f : session.getObjects(o -> o.getClass().getSimpleName().equals("CreationStamp")))
            out.add(new Object[]{field(f, "onderwerp"), field(f, "object"), field(f, "phase")});
        out.sort(java.util.Comparator.comparingInt(g -> (Integer) g[2]));
        return out;
    }

    /** The links of {@code relation} in {@code session} ({@code RelationLink}, read by name), as {first, second, phase}. */
    static List<Object[]> links(KieSession session, String relation) {
        List<Object[]> out = new ArrayList<>();
        for (Object f : session.getObjects(o -> o.getClass().getSimpleName().equals("RelationLink")))
            if (relation.equals(field(f, "relation"))) out.add(new Object[]{field(f, "first"), field(f, "second"), field(f, "phase")});
        return out;
    }

    private static Object field(Object fact, String naam) {
        try {
            return fact.getClass().getMethod(naam).invoke(fact);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    /** Counts into {@code counter} every {@link Value} of attribute {@code attribute} with value {@code lexeme} that is
     *  inserted: an intermediate state of the defaults group of that attribute passing. */
    private static DefaultRuleRuntimeEventListener valueCounter(String attribute, String lexeme, int[] counter) {
        BigDecimal v = new BigDecimal(lexeme);
        return new DefaultRuleRuntimeEventListener() {
            @Override public void objectInserted(ObjectInsertedEvent e) {
                if (e.getObject() instanceof Value w && w.attribute().naam().equalsIgnoreCase(attribute)
                        && w.value() instanceof BigDecimal x && x.compareTo(v) == 0) counter[0]++;
            }
        };
    }

    private static void setRole(Object beanObject, String role, Object target) throws Exception {
        for (Method m : beanObject.getClass().getMethods())
            if (m.getName().equals(dev.uchar.regelspraak.transpiler.core.Namen.setter(role)) && m.getParameterCount() == 1) {
                m.invoke(beanObject, target);
                return;
            }
        throw new AssertionError("geen setter voor " + role + " op " + beanObject.getClass());
    }

    private static void assertNumber(String expected, Object actual, String description) {
        assertTrue(actual instanceof BigDecimal w && new BigDecimal(expected).compareTo(w) == 0,
                () -> description + ": verwacht " + expected + ", werkelijk " + actual);
    }
}
