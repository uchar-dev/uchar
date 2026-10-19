package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;

import org.drools.model.Rule;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static dev.uchar.regelspraak.cases.TmsCreationWitnessTest.document;
import static dev.uchar.regelspraak.cases.TmsCreationWitnessTest.factCreation;
import static dev.uchar.regelspraak.cases.TmsCreationWitnessTest.made;
import static dev.uchar.regelspraak.cases.TmsCreationWitnessTest.init;
import static dev.uchar.regelspraak.cases.TmsCreationWitnessTest.links;
import static dev.uchar.regelspraak.cases.TmsCreationWitnessTest.objectCreation;
import static dev.uchar.regelspraak.cases.TmsCreationWitnessTest.ruleGroup;
import static dev.uchar.regelspraak.cases.TmsCreationWitnessTest.fire;
import static dev.uchar.regelspraak.cases.TmsCreationWitnessTest.fireInOrder;
import static dev.uchar.regelspraak.cases.TmsDocuments.WHOLE;
import static dev.uchar.regelspraak.cases.TmsDocuments.BRANCHES;
import static dev.uchar.regelspraak.cases.TmsDocuments.COUNTING;
import static dev.uchar.regelspraak.cases.TmsDocuments.aggregation;
import static dev.uchar.regelspraak.cases.TmsDocuments.attribute;
import static dev.uchar.regelspraak.cases.TmsDocuments.binary;
import static dev.uchar.regelspraak.cases.TmsDocuments.sourceRole;
import static dev.uchar.regelspraak.cases.TmsDocuments.factType;
import static dev.uchar.regelspraak.cases.TmsDocuments.factTypeOneToMany;
import static dev.uchar.regelspraak.cases.TmsDocuments.generated;
import static dev.uchar.regelspraak.cases.TmsDocuments.number;
import static dev.uchar.regelspraak.cases.TmsDocuments.instanceWithRoles;
import static dev.uchar.regelspraak.cases.TmsDocuments.ownRead;
import static dev.uchar.regelspraak.cases.TmsDocuments.objectType;
import static dev.uchar.regelspraak.cases.TmsDocuments.pathRead;
import static dev.uchar.regelspraak.cases.TmsDocuments.permutations;
import static dev.uchar.regelspraak.cases.TmsDocuments.ruleNames;
import static dev.uchar.regelspraak.cases.TmsDocuments.computingRule;
import static dev.uchar.regelspraak.cases.TmsDocuments.roleBinding;
import static dev.uchar.regelspraak.cases.TmsDocuments.sample;
import static dev.uchar.regelspraak.cases.TmsDocuments.comparison;
import static dev.uchar.regelspraak.cases.TmsDocuments.value;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Creations in the evaluation strata of the reference interpreter (RUNTIME-SEMANTICS ch. 6.1 and 6.3): a creation
 * writes irrevocably, so it reads strictly and sits in a stratum above the producers of what it reads, and the forms of
 * a stratum reach their fixpoint before a form of the next one fires ({@code ref/lib/eval.ml}). The driver opens the
 * creation phases stratum by stratum, each in generations, the rule layer under truth maintenance throughout
 * ({@code DroolsBackend.fire}). Each document plays through the replay in every order of its creations and rules in the
 * document, and fires through the driver in the order of the rules the branch generates, its reverse and a hundred
 * shuffles, on the exec-model branch and on {@code DRL_TEXT} (the last paragraph).
 *
 * <ul>
 *   <li>An initialisation reads what a creation of a lower stratum made: "maak een afrekening" counts the Penning that
 *       "geef een penning" makes, where both fired against one state before and the count stayed 0, fixed at creation
 *       ({@link #an_initialisation_reads_what_a_creation_from_a_lower_stratum_made}).</li>
 *   <li>A condition with an operand that grows is decided on the value after the lower stratum: the sum of the
 *       penningen is 10 there, not the 5 of the input, so no Afrekening is made
 *       ({@link #a_condition_with_a_growing_operand_decides_after_the_lower_stratum}).</li>
 *   <li>A chain object creation, fact creation, object creation runs in three strata, and the last initialisation
 *       counts the link the fact creation made over the object the first made
 *       ({@link #a_chain_of_object_creation_fact_creation_and_object_creation_runs_in_three_strata}).</li>
 *   <li>Two creations of one stratum that do not read each other fire in one generation, in one phase, and both read
 *       the Penning of the stratum below ({@link #two_creations_of_one_stratum_fire_in_one_generation}).</li>
 *   <li>Every creation of a generation reads the same settled world: an object creation does not count the link a
 *       fact creation of its own generation lays ({@link #the_creations_of_a_generation_read_a_world}).</li>
 *   <li>An object creation whose condition the rule layer makes true after a generation fires in the next generation,
 *       and the creations of that generation do not read what it makes: W3 starts at beginbeloning 0, though W1 gets
 *       its Beloning in the generation in which W2 makes W3
 *       ({@link #an_object_creation_that_the_rule_layer_makes_possible_fires_in_the_next_generation}).</li>
 *   <li>The creation budget holds per stratum, as the reference interpreter counts the depth of a mint in the stratum
 *       of its creation: two chains of 51 mints in two strata play ({@link #the_creation_budget_applies_per_stratum}), and
 *       so does a chain over three strata, 50 + 1 + 50 ({@link #a_chain_over_three_strata_counts_per_stratum}).</li>
 *   <li>The budget counts the depth of a chain of mints (RUNTIME-SEMANTICS ch. 5.5): one chain plays 99 and 100 mints,
 *       and its mint at depth 101 is the fault, at the making
 *       ({@link #a_creation_at_depth_100_plays_and_one_at_depth_101_is_a_fault}); a chain of 100 mints plays with a
 *       rule of its stratum that gives the last object a value after the hundredth generation
 *       ({@link #a_generation_where_only_the_rule_layer_changes_does_not_count_for_the_budget}), and so does the
 *       chain one shorter ({@link #a_chain_that_is_one_step_shorter_plays}), the chain without the rule
 *       ({@link #the_same_chain_without_the_computed_property_plays}) and the chain with a rule of a higher stratum
 *       that reacts to the last object ({@link #a_reaction_from_a_higher_stratum_does_not_count}). A recursive fact
 *       creation that lays a closure of 102 hops plays, since a fact creation mints nothing and has no budget
 *       ({@link #a_fact_creation_has_no_budget}); a chain whose every mint waits a generation for a link plays to
 *       depth 100, in 199 generations, and its mint at depth 101 is the fault
 *       ({@link #a_creation_that_waits_for_a_link_keeps_the_depth_of_its_subject}); and a subject that a higher
 *       stratum made, which no verifier-clean document gives a creation, fails loudly
 *       ({@link #a_subject_from_a_higher_stratum_fails_loudly}).</li>
 * </ul>
 *
 * <p><b>The other executors</b> (measured 2026-10-09, cards creatie-initialisatie-leest-groeiende-waarde and
 * creatiebudget-ref-en-clingo). Every document written here passes the verifier, and the reference interpreter and the
 * ASP backend give the expectations of each case in every order of its creations and rules in the document, apart from
 * two. On the chains of document G and document E that a mint at depth 101 would extend both fault, in the words this
 * branch faults with: the reference interpreter with "creation budget exhausted: object creation "werf de lange keten"
 * of stratum 0 would mint at creation depth 101, on oc0::…::W1 — …" and the ASP backend with "UNSATISFIABLE — a
 * fail-fast guard fired: viol("creation budget exhausted", …)" (measured on G and, for E, on the same shape written by
 * hand, to rang 101 and 102). Document V the reference interpreter plays and the ASP backend refuses by name: "the
 * initialisation of beginstand reads rel:maatschap, which fact creation "maak maten" writes on a dependency cycle
 * through the creation …"; document R likewise, "the initialisation of beginbeloning reads attr:wachter:erfbeloning,
 * which rule "erfbeloning" writes on a dependency cycle through the creation …". The anchor
 * {@code jutterswet-erflijn} over 103 Wachters plays in both ({@code ref/test} {@code WV-5.5-budget-erflijn}, the clingo
 * suite). Until 2026-10-09 the two counted their budgets apart (measured 2026-10-08): on the chain of document L whose
 * last Boei gets its kopie both faulted, the reference interpreter with "no fixpoint within 100 passes — divergence is a
 * fault (ch. 5.7)", and on the chains of L to B101 without the kopie and with "genummerd" the reference interpreter
 * played and the ASP backend faulted with "generation budget exhausted": its budget was the depth of a chain of mints,
 * which reached 100 at the hundredth mint. Later that day the reference interpreter counted 100 generations per stratum
 * that mint or link and faulted on the erflijn over 103 Wachters and on E to rang 101, both of which the ASP backend
 * played.
 *
 * <p><b>Red before the change</b> (measured 2026-10-08 on {@code 6b4c966f}, all creations of a generation in one
 * phase): all five failed, in every document order: "case: attribuut penningaantal: verwacht 1, werkelijk 0" on P and
 * on K, "case: attribuut afrekeningaantal: verwacht 0, werkelijk 1" on S, "case: attribuut penningaantal: verwacht 1,
 * werkelijk 0; attribuut kwitantieaantal: verwacht 1, werkelijk 0" on T, and on B the two chains ran side by side:
 * "expected: &lt;Wachter in fase 2&gt; but was: &lt;Boei in fase 2&gt;". Mutants of the change, each caught: a budget
 * over the whole run (B: "geen fixpunt binnen 100 generaties: generatie 50 maakt nog 1 object of link"), every
 * creation in a stratum of its own above the reference interpreter's (T: "Kwitantie in fase 10"), strata in the order
 * the transpiler lowers the creations (P, S, K and T), and strata without the strict edges (all five).
 *
 * <p><b>Red before the pass count</b> (measured 2026-10-08 on {@code fdaa52f4}, which counted only the generations that
 * create): the branch played the chain whose last Boei gets its kopie, "but was: &lt;[[werf de opvolger, leg de
 * volgende boei] gespeeld, [leg de volgende boei, werf de opvolger] gespeeld]&gt;". The three that play played there as
 * well; they hold the count to what the reference interpreter counts. Mutants of the change, each caught: a change of
 * the rule layer counted whatever the stratum of its writer ("een reactie uit een hoger stratum" ends without a
 * fixpoint), and the facts a creation states counted as a change of the rule layer (the chain without the kopie ends
 * without a fixpoint, and the sentence on the chain with it names more than one change).
 *
 * <p><b>Red before the generation count</b> (measured 2026-10-09 on {@code e15baf01}, which counted a pass in which only
 * the rule layer changed and fired a generation in two phases, the rule layer at rest between them): the branch ended
 * the chain of document L whose last Boei gets its kopie without a fixpoint, "case: geen fixpunt binnen 100 generaties:
 * generatie 101 maakt niets meer, maar de regellaag verandert nog 1 waarde, kenmerk of link na wat generatie 100
 * maakte …", in both orders of its creations, and on document V an object creation read the link a fact creation of its
 * own generation had laid, "case: attribuut verder: verwacht 0, werkelijk 1", in both orders of the rules. Documents G
 * and D played and ended where they end now; they failed on the phases alone ("expected: &lt;Wachter in fase 1&gt; but
 * was: &lt;Wachter in fase 2&gt;"), as did every witness above that pins a phase, since a generation is one phase now.
 *
 * <p><b>Red before the creation depth</b> (measured 2026-10-09 on {@code 7b86bd55}, which counted at most 100
 * generations per stratum that make an object or lay a link): the erflijn over 103 Wachters ended without a fixpoint,
 * "expected: &lt;gespeeld&gt; but was: &lt;case: geen fixpunt binnen 100 generaties: generatie 101 maakt nog 1 object of
 * link — …&gt;", and so did E to rang 101 ("tot rang 101 ==&gt; expected: &lt;gespeeld&gt;"); G at 101 mints ended with
 * that sentence instead of the budget's, and the forged mark of the invariant could not be written ("wrong number of
 * arguments: 6 expected: 4"). Mutants of the change, each caught: a depth that runs on over the strata (D), the budget
 * one lower (G, E and L here, F and the corpus in {@code TmsCreationWitnessTest}), and without the test of the invariant
 * (the forged mark).
 *
 * <p><b>The samples on the text</b> (card drl-teksttak-op-truth-maintenance, round 5b, on the lens of round 5). Every
 * sample of this class fires on the exec-model branch and on {@code DRL_TEXT}, its analysis rendered as DRL text, with
 * the rules of the text in the same orders ({@code TextOrder}), since the order of the rules in the text is the
 * order in which Drools loads them; each branch meets the same demands. <b>Red</b> on {@code b4d83a69}, before the
 * rendering carried the creations: fifteen of the sixteen failed at the replay, "DRL_TMS speelt … anders dan de
 * exec-model-tak ==&gt; expected: &lt;gespeeld&gt; but was: &lt;Excluded: drl-tms: de DRL-weergave draagt Creatie nog
 * niet&gt;" (the erflijn: "… draagt Koppeling …"); with the replay on {@code DRL_TMS} left out of a copy, the fourteen
 * with a sample failed at the sample on the text, "Excluded drl-tms: de DRL-weergave draagt Creatie nog niet".
 */
class TmsCreationStratumWitnessTest {

    // ---------------- the Jutter, his penningen and his afrekening ----------------

    private static final String PENNINGEN = "{\"node\":\"AttributeReference\",\"attribute\":\"penningen\",\"subjectType\":"
            + "\"Jutter\",\"source\":{\"snippet\":\"penningen van de Jutter\"}}";

    private static final String COUNT_PENNINGEN = aggregation("count", PENNINGEN, "Penning",
            sourceRole("jutterheeftpenning", "penning"), "null", "null", COUNTING, "het aantal penningen van de Jutter");

    /** "Eén houder heeft meerdere penningen." */
    private static final String PENNING_FACT = factTypeOneToMany("jutterheeftpenning", "houder", "Jutter", "penning",
            "Penning", "penningen");

    /** "Eén afrekenaar heeft één afrekening." */
    private static final String SETTLEMENT_FACT = factType("jutterheeftafrekening", "afrekenaar", "Jutter", "afrekening",
            "Afrekening");

    /** "Een Jutter heeft een penning met bedrag gelijk aan 5." */
    static final String GEEF = objectCreation("geef een penning", "Jutter", "Penning", List.of(init("bedrag", number("5"))),
            "null", "jutterheeftpenning", "penning");

    /** "Een Jutter heeft een afrekening met penningtelling gelijk aan het aantal penningen van de Jutter." */
    static final String MAKE = objectCreation("maak een afrekening", "Jutter", "Afrekening",
            List.of(init("penningtelling", COUNT_PENNINGEN)), "null", "jutterheeftafrekening", "afrekening");

    /** "Het penningaantal van een Jutter moet berekend worden als de penningtelling van de afrekening van de Jutter." */
    static final String PENNING_COUNT = computingRule("penningaantal", "BEREKENING", "Jutter", "penningaantal",
            pathRead("penningtelling", "Afrekening", "jutterheeftafrekening", "afrekening"), "null");

    // ---------------- document P: an initialisation reads a creation of a lower stratum ----------------

    private static final String MODEL_P = TmsDocuments.model(List.of(
            objectType("Jutter", List.of(attribute("penningaantal", WHOLE)), List.of()),
            objectType("Penning", List.of(attribute("bedrag", WHOLE)), List.of()),
            objectType("Afrekening", List.of(attribute("penningtelling", WHOLE)), List.of())),
            List.of(PENNING_FACT, SETTLEMENT_FACT));

    /** The scenario of the final lens: "maak een afrekening" reads the penningen that "geef een penning" makes, a strict
     *  read (it writes irrevocably), so it sits a stratum above it; "penningaantal" reads the count off the afrekening. */
    static Path documentP(List<String> creations, List<String> rules) throws Exception {
        return document(MODEL_P, rules, List.of(), creations, List.of(), List.of());
    }

    static JsonNode caseP() throws Exception {
        return TmsDocuments.caseDocument(List.of(instanceWithRoles("J1", "Jutter", Map.of(), List.of(), Map.of())),
                List.of(value("J1", "penningaantal", "1")));
    }

    @Test void an_initialisation_reads_what_a_creation_from_a_lower_stratum_made() throws Exception {
        playsInEveryOrder(List.of(GEEF, MAKE), List.of(PENNING_COUNT), TmsCreationStratumWitnessTest::documentP, caseP());
        Path ir = documentP(List.of(GEEF, MAKE), List.of(PENNING_COUNT));
        for (DroolsBackend.Branch branch : BRANCHES) {
            DroolsBackend.Build b = DroolsBackend.build(ir, branch);
            for (List<String> names : sample(generated(b), 20261008L, 100)) {
                String where = names + " op " + branch;
                Object j1 = TmsDocuments.beanObject(b.beanLoader(), "Jutter", Map.of(), List.of());
                List<Object[]> objects = new ArrayList<>();
                assertNull(fireInOrder(b, names, List.of(j1), session -> { },
                        session -> objects.addAll(made(session))),
                        () -> "volgorde " + where);
                assertNumber("1", TmsDocuments.read(j1, "penningaantal"), "penningaantal in volgorde " + where);
                // one phase per generation: the Penning in stratum 0's first generation; stratum 0 closes with a
                // generation that makes nothing (phase 2), and the Afrekening follows in the first generation of
                // stratum 1
                assertEquals(List.of("Penning in fase 1", "Afrekening in fase 3"), kinds(objects),
                        () -> "volgorde " + where);
            }
        }
    }

    // ---------------- document S: a condition whose operand grows ----------------

    private static final String AFREKENINGEN = "{\"node\":\"AttributeReference\",\"attribute\":\"afrekeningen\","
            + "\"subjectType\":\"Jutter\",\"source\":{\"snippet\":\"afrekeningen van de Jutter\"}}";

    private static final String SUM = aggregation("sum", ownRead("bedrag", "Penning"), "Penning",
            sourceRole("jutterheeftpenning", "penning"), "null", number("0"), WHOLE,
            "de som van het bedrag van alle penningen van de Jutter of 0 als die er niet zijn");

    private static final String MODEL_S = TmsDocuments.model(List.of(
            objectType("Jutter", List.of(attribute("afrekeningaantal", WHOLE), attribute("penningsom", WHOLE)),
                    List.of()),
            objectType("Penning", List.of(attribute("bedrag", WHOLE)), List.of()),
            objectType("Afrekening", List.of(), List.of())),
            List.of(PENNING_FACT, factTypeOneToMany("jutterheeftafrekening", "afrekenaar", "Jutter", "afrekening",
                    "Afrekening", "afrekeningen")));

    /** "Een Jutter heeft een afrekening indien de som van het bedrag van alle penningen van de Jutter kleiner is dan
     *  8." */
    static final String MAKE_UNDER_EIGHT = objectCreation("maak een afrekening", "Jutter", "Afrekening", List.of(),
            comparison("less", SUM, number("8")), "jutterheeftafrekening", "afrekening");

    static final List<String> RULES_S = List.of(
            computingRule("afrekeningaantal", "BEREKENING", "Jutter", "afrekeningaantal", aggregation("count", AFREKENINGEN,
                    "Afrekening", sourceRole("jutterheeftafrekening", "afrekening"), "null", "null", COUNTING,
                    "het aantal afrekeningen van de Jutter"), "null"),
            computingRule("penningsom", "BEREKENING", "Jutter", "penningsom", SUM, "null"));

    static Path documentS(List<String> creations, List<String> rules) throws Exception {
        return document(MODEL_S, rules, List.of(), creations, List.of(), List.of());
    }

    /** J1 holds P1 of 5 in the input; "geef een penning" adds one of 5, so the sum is 10 after stratum 0. */
    static JsonNode caseS() throws Exception {
        return TmsDocuments.caseDocument(List.of(
                instanceWithRoles("J1", "Jutter", Map.of(), List.of(), Map.of("penningen", List.of("P1"))),
                instanceWithRoles("P1", "Penning", Map.of("bedrag", "5"), List.of(), Map.of())),
                List.of(value("J1", "afrekeningaantal", "0"), value("J1", "penningsom", "10")));
    }

    @Test void a_condition_with_a_growing_operand_decides_after_the_lower_stratum() throws Exception {
        playsInEveryOrder(List.of(GEEF, MAKE_UNDER_EIGHT), RULES_S, TmsCreationStratumWitnessTest::documentS, caseS());
        Path ir = documentS(List.of(GEEF, MAKE_UNDER_EIGHT), RULES_S);
        for (DroolsBackend.Branch branch : BRANCHES) {
            DroolsBackend.Build b = DroolsBackend.build(ir, branch);
            for (List<String> names : sample(generated(b), 20261008L, 100)) {
                String where = names + " op " + branch;
                Object j1 = TmsDocuments.beanObject(b.beanLoader(), "Jutter", Map.of(), List.of());
                Object p1 = TmsDocuments.beanObject(b.beanLoader(), "Penning", Map.of("bedrag", "5"), List.of());
                setRole(p1, "houder", j1);
                List<Object[]> objects = new ArrayList<>();
                assertNull(fireInOrder(b, names, List.of(j1, p1), session -> { },
                        session -> objects.addAll(made(session))),
                        () -> "volgorde " + where);
                assertNumber("0", TmsDocuments.read(j1, "afrekeningaantal"), "afrekeningaantal in volgorde " + where);
                assertNumber("10", TmsDocuments.read(j1, "penningsom"), "penningsom in volgorde " + where);
                assertEquals(List.of("Penning in fase 1"), kinds(objects), () -> "volgorde " + where);
            }
        }
    }

    // ---------------- document K: object creation, fact creation, object creation ----------------

    private static final String KEPT = "{\"node\":\"AttributeReference\",\"attribute\":\"bewaarde penningen\","
            + "\"subjectType\":\"Jutter\",\"source\":{\"snippet\":\"bewaarde penningen van de Jutter\"}}";

    private static final String MODEL_K = TmsDocuments.model(List.of(
            objectType("Jutter", List.of(attribute("penningaantal", WHOLE)), List.of()),
            objectType("Penning", List.of(attribute("bedrag", WHOLE)), List.of()),
            objectType("Afrekening", List.of(attribute("penningtelling", WHOLE)), List.of())),
            List.of(PENNING_FACT, factTypeOneToMany("bewaring", "bewaarder", "Jutter", "bewaarde penning", "Penning",
                    "bewaarde penningen"), SETTLEMENT_FACT));

    /** "Een bewaarde penning van een Jutter is een penning van de Jutter." */
    static final String KEEP = factCreation("bewaar de penningen", "bewaring", "bewaarder", "bewaarde penning",
            "Jutter", List.of(roleBinding("jutterheeftpenning", "penning")), "null");

    /** "Een Jutter heeft een afrekening met penningtelling gelijk aan het aantal bewaarde penningen van de Jutter." */
    static final String MAKE_KEPT = objectCreation("maak een afrekening", "Jutter", "Afrekening",
            List.of(init("penningtelling", aggregation("count", KEPT, "Penning", sourceRole("bewaring", "bewaarde penning"),
                    "null", "null", COUNTING, "het aantal bewaarde penningen van de Jutter"))),
            "null", "jutterheeftafrekening", "afrekening");

    static Path documentK(List<String> creations, List<String> rules) throws Exception {
        return document(MODEL_K, rules, List.of(), creations, List.of(KEEP), List.of());
    }

    @Test void a_chain_of_object_creation_fact_creation_and_object_creation_runs_in_three_strata() throws Exception {
        playsInEveryOrder(List.of(GEEF, MAKE_KEPT), List.of(PENNING_COUNT), TmsCreationStratumWitnessTest::documentK,
                caseP());
        Path ir = documentK(List.of(GEEF, MAKE_KEPT), List.of(PENNING_COUNT));
        for (DroolsBackend.Branch branch : BRANCHES) {
            DroolsBackend.Build b = DroolsBackend.build(ir, branch);
            for (List<String> names : sample(generated(b), 20261008L, 100)) {
                String where = names + " op " + branch;
                Object j1 = TmsDocuments.beanObject(b.beanLoader(), "Jutter", Map.of(), List.of());
                List<Object[]> objects = new ArrayList<>(), left = new ArrayList<>();
                assertNull(fireInOrder(b, names, List.of(j1), session -> { }, session -> {
                    objects.addAll(made(session));
                    left.addAll(links(session, "bewaring"));
                }), () -> "volgorde " + where);
                assertNumber("1", TmsDocuments.read(j1, "penningaantal"), "penningaantal in volgorde " + where);
                // one phase per generation: stratum 0 makes the Penning (phase 1) and closes in phase 2; stratum 1
                // links it in its first generation (3) and closes in 4; stratum 2 makes the Afrekening in its first
                // generation (5)
                assertEquals(List.of("Penning in fase 1", "Afrekening in fase 5"), kinds(objects),
                        () -> "volgorde " + where);
                assertEquals(1, left.size(), () -> "links in volgorde " + where);
                assertTrue(left.get(0)[0] == j1 && left.get(0)[1] == objects.get(0)[1],
                        () -> "link in volgorde " + where);
                assertEquals(3, left.get(0)[2], () -> "fase van de link in volgorde " + where);
            }
        }
    }

    // ---------------- document T: two creations of one stratum ----------------

    private static final String MODEL_T = TmsDocuments.model(List.of(
            objectType("Jutter", List.of(attribute("penningaantal", WHOLE), attribute("kwitantieaantal", WHOLE)),
                    List.of()),
            objectType("Penning", List.of(attribute("bedrag", WHOLE)), List.of()),
            objectType("Afrekening", List.of(attribute("penningtelling", WHOLE)), List.of()),
            objectType("Kwitantie", List.of(attribute("penningtelling", WHOLE)), List.of())),
            List.of(PENNING_FACT, SETTLEMENT_FACT, factType("jutterheeftkwitantie", "ontvanger", "Jutter", "kwitantie",
                    "Kwitantie")));

    /** "Een Jutter heeft een kwitantie met penningtelling gelijk aan het aantal penningen van de Jutter." */
    static final String KWITEER = objectCreation("schrijf een kwitantie", "Jutter", "Kwitantie",
            List.of(init("penningtelling", COUNT_PENNINGEN)), "null", "jutterheeftkwitantie", "kwitantie");

    static final List<String> RULES_T = List.of(PENNING_COUNT, computingRule("kwitantieaantal", "BEREKENING", "Jutter",
            "kwitantieaantal", pathRead("penningtelling", "Kwitantie", "jutterheeftkwitantie", "kwitantie"), "null"));

    /** "maak een afrekening" and "schrijf een kwitantie" both read the penningen and neither reads what the other
     *  writes: one stratum, above "geef een penning". */
    static Path documentT(List<String> creations, List<String> rules) throws Exception {
        return document(MODEL_T, rules, List.of(), creations, List.of(), List.of());
    }

    static JsonNode caseT() throws Exception {
        return TmsDocuments.caseDocument(List.of(instanceWithRoles("J1", "Jutter", Map.of(), List.of(), Map.of())),
                List.of(value("J1", "penningaantal", "1"), value("J1", "kwitantieaantal", "1")));
    }

    @Test void two_creations_of_one_stratum_fire_in_one_generation() throws Exception {
        playsInEveryOrder(List.of(GEEF, MAKE, KWITEER), RULES_T, TmsCreationStratumWitnessTest::documentT, caseT());
        Path ir = documentT(List.of(GEEF, MAKE, KWITEER), RULES_T);
        for (DroolsBackend.Branch branch : BRANCHES) {
            DroolsBackend.Build b = DroolsBackend.build(ir, branch);
            for (List<String> names : sample(generated(b), 20261008L, 100)) {
                String where = names + " op " + branch;
                Object j1 = TmsDocuments.beanObject(b.beanLoader(), "Jutter", Map.of(), List.of());
                List<Object[]> objects = new ArrayList<>();
                assertNull(fireInOrder(b, names, List.of(j1), session -> { },
                        session -> objects.addAll(made(session))),
                        () -> "volgorde " + where);
                assertNumber("1", TmsDocuments.read(j1, "penningaantal"), "penningaantal in volgorde " + where);
                assertNumber("1", TmsDocuments.read(j1, "kwitantieaantal"), "kwitantieaantal in volgorde " + where);
                List<String> kinds = new ArrayList<>(kinds(objects));
                kinds.sort(null);
                // both of stratum 1 in its first generation, phase 3
                assertEquals(List.of("Afrekening in fase 3", "Kwitantie in fase 3", "Penning in fase 1"), kinds,
                        () -> "volgorde " + where);
            }
        }
    }

    // ---------------- document B: the budget per stratum ----------------

    private static final String MODEL_B = TmsDocuments.model(List.of(
            objectType("Wachter", List.of(attribute("rang", WHOLE)), List.of()),
            objectType("Boei", List.of(attribute("nummer", WHOLE), attribute("wachterrang", WHOLE)), List.of())),
            List.of(factType("opvolging", "voorganger", "Wachter", "opvolger", "Wachter"),
                    factType("boeiketen", "vorige boei", "Boei", "volgende boei", "Boei"),
                    factType("bewaking", "wachter", "Wachter", "boei", "Boei")));

    /** A chain of Wachters while the rang is below 52: W1 of rang 1 makes W2, and so on to W52, 51 generations. */
    static final String WERF = objectCreation("werf de opvolger", "Wachter", "Wachter",
            List.of(init("rang", binary("plus", ownRead("rang", "Wachter"), number("1")))),
            comparison("less", ownRead("rang", "Wachter"), number("52")), "opvolging", "opvolger");

    /** A chain of Boeien while the nummer is below 52, 51 generations; the wachterrang reads the rang of the Boei's
     *  wachter, which "werf de opvolger" writes, so this chain sits a stratum above the other. */
    static final String LEG = leg(52);

    /** The chain of Boeien while the nummer is below {@code bound}: from B1, {@code grens - 1} generations. */
    static String leg(int bound) {
        return objectCreation("leg de volgende boei", "Boei", "Boei",
                List.of(init("nummer", binary("plus", ownRead("nummer", "Boei"), number("1"))),
                        init("wachterrang", pathRead("rang", "Wachter", "bewaking", "wachter"))),
                comparison("less", ownRead("nummer", "Boei"), number(String.valueOf(bound))), "boeiketen", "volgende boei");
    }

    static Path documentB(List<String> creations) throws Exception {
        return document(MODEL_B, List.of(), List.of(), creations, List.of(),
                List.of(ruleGroup("estafette", List.of("werf de opvolger")),
                        ruleGroup("boeienlijn", List.of("leg de volgende boei"))));
    }

    static JsonNode caseB() throws Exception {
        return TmsDocuments.caseDocument(List.of(
                instanceWithRoles("W1", "Wachter", Map.of("rang", "1"), List.of(), Map.of("boei", List.of("B1"))),
                instanceWithRoles("B1", "Boei", Map.of("nummer", "1"), List.of(), Map.of())),
                List.of(value("W1", "rang", "1")));
    }

    /** Two chains of 51 mints each, 102 in all, in two strata. The reference interpreter counts the depth of a mint in the
     *  stratum of its creation, where neither chain comes near 100, and plays the document; a depth that ran on over
     *  the strata, or a count over the whole run, would end it at its 101st mint. The Boeien wait for the Wachters, so
     *  the first Boei is made after the 51 generations of stratum 0 and the one that closes it, one phase each: in phase
     *  52 + 1. */
    @Test void the_creation_budget_applies_per_stratum() throws Exception {
        playsInEveryOrder(List.of(WERF, LEG), List.of(), (creations, rules) -> documentB(creations), caseB());
        Path ir = documentB(List.of(WERF, LEG));
        for (DroolsBackend.Branch branch : BRANCHES) {
            DroolsBackend.Build b = DroolsBackend.build(ir, branch);
            for (List<String> names : sample(generated(b), 20261008L, 10)) {
                String where = names + " op " + branch;
                Object w1 = TmsDocuments.beanObject(b.beanLoader(), "Wachter", Map.of("rang", "1"), List.of());
                Object b1 = TmsDocuments.beanObject(b.beanLoader(), "Boei", Map.of("nummer", "1"), List.of());
                setRole(w1, "boei", b1);
                setRole(b1, "wachter", w1);
                List<Object[]> objects = new ArrayList<>();
                assertNull(fireInOrder(b, names, List.of(w1, b1), session -> { },
                        session -> objects.addAll(made(session))),
                        () -> "volgorde " + where);
                assertEquals(102, objects.size(), () -> "objecten in volgorde " + where);
                for (int i = 0; i < 51; i++) {
                    assertEquals("Wachter in fase " + (i + 1), category(objects.get(i)), "volgorde " + where);
                    assertEquals("Boei in fase " + (52 + i + 1), category(objects.get(51 + i)), "volgorde " + where);
                }
                assertNumber("52", TmsDocuments.read(objects.get(50)[1], "rang"), "rang van W52 in volgorde " + where);
                assertNumber("52", TmsDocuments.read(objects.get(101)[1], "nummer"),
                        "nummer van B52 in volgorde " + where);
                assertNumber("1", TmsDocuments.read(objects.get(51)[1], "wachterrang"),
                        "wachterrang van B2 in volgorde " + where);
            }
        }
    }

    // ---------------- document L: a generation after which only the rule layer changes ----------------

    private static final String MODEL_L = TmsDocuments.model(List.of(
            objectType("Wachter", List.of(attribute("rang", WHOLE)), List.of()),
            objectType("Boei", List.of(attribute("nummer", WHOLE), attribute("wachterrang", WHOLE),
                    attribute("kopie", WHOLE)), List.of("genummerd"))),
            List.of(factType("opvolging", "voorganger", "Wachter", "opvolger", "Wachter"),
                    factType("boeiketen", "vorige boei", "Boei", "volgende boei", "Boei"),
                    factType("bewaking", "wachter", "Wachter", "boei", "Boei")));

    /** "De kopie van een Boei moet gesteld worden op zijn nummer": it reads what the chain of Boeien writes, positively,
     *  so it sits in the stratum of that chain. */
    static final String COPY = computingRule("kopie van een boei", "GELIJKSTELLING", "Boei", "kopie",
            ownRead("nummer", "Boei"), "null");

    /** "Een Boei is genummerd indien zijn nummer groter is dan 0": a characteristic attribution reads strictly, so it
     *  sits a stratum above the chain of Boeien. */
    static final String NUMBERED = TmsDocuments.characteristicRule("genummerd", "Boei", "genummerd",
            comparison("greater", ownRead("nummer", "Boei"), number("0")));

    /** Document B with the chain of Boeien running while the nummer is below {@code bound}, beside {@code rules}. */
    static Path documentL(int bound, List<String> creations, List<String> rules) throws Exception {
        List<String> c = new ArrayList<>();
        for (String creation : creations) c.add(creation.equals(LEG) ? leg(bound) : creation);
        return document(MODEL_L, rules, List.of(), c, List.of(),
                List.of(ruleGroup("estafette", List.of("werf de opvolger")),
                        ruleGroup("boeienlijn", List.of("leg de volgende boei"))));
    }

    static JsonNode caseL(List<String> expectations) throws Exception {
        List<String> v = new ArrayList<>(List.of(value("W1", "rang", "1")));
        v.addAll(expectations);
        return TmsDocuments.caseDocument(List.of(
                instanceWithRoles("W1", "Wachter", Map.of("rang", "1"), List.of(), Map.of("boei", List.of("B1"))),
                instanceWithRoles("B1", "Boei", Map.of("nummer", "1"), List.of(), Map.of())),
                v);
    }

    /** The scenario of the lens on the budget: the chain of Boeien runs to B101, 100 mints of stratum 1, and "kopie van
     *  een boei" writes the kopie of B101 after the hundredth generation, while the rule layer comes to rest. The budget
     *  counts the depth of a chain of mints (RUNTIME-SEMANTICS ch. 5.5), B101 stands at depth 100, and the document
     *  plays, as in the reference interpreter and the ASP backend. Until 2026-10-09 the driver counted the passes of
     *  the reference interpreter of then, among them a pass in which only the rule layer changed, and this document
     *  ended without a fixpoint: "geen fixpunt binnen 100 generaties: generatie 101 maakt niets meer, maar de regellaag
     *  verandert nog 1 waarde, kenmerk of link na wat generatie 100 maakte". */
    @Test void a_generation_where_only_the_rule_layer_changes_does_not_count_for_the_budget() throws Exception {
        playsInEveryOrder(List.of(WERF, LEG), List.of(COPY), (c, r) -> documentL(101, c, r),
                caseL(List.of(value("B1", "kopie", "1"))));
        playsThroughTheDriver(documentL(101, List.of(WERF, LEG), List.of(COPY)), 51 + 100);
    }

    /** The same chain one shorter, to B100, 99 mints of stratum 1. */
    @Test void a_chain_that_is_one_step_shorter_plays() throws Exception {
        playsInEveryOrder(List.of(WERF, LEG), List.of(COPY), (c, r) -> documentL(100, c, r),
                caseL(List.of(value("B1", "kopie", "1"))));
        playsThroughTheDriver(documentL(100, List.of(WERF, LEG), List.of(COPY)), 51 + 99);
    }

    /** The chain to B101 without "kopie van een boei", 100 mints of stratum 1. Until 2026-10-09 it already
     *  played: the reference interpreter's 101st pass changed nothing, since a new object without a written value was no
     *  change for it, and the driver did not count the standings and contributions the rule layer inserts for B101. */
    @Test void the_same_chain_without_the_computed_property_plays() throws Exception {
        playsInEveryOrder(List.of(WERF, LEG), List.of(), (c, r) -> documentL(101, c, r), caseL(List.of()));
        playsThroughTheDriver(documentL(101, List.of(WERF, LEG), List.of()), 51 + 100);
    }

    /** The chain to B101 with "genummerd", which the rule layer attributes to B101 after the hundredth generation, from a
     *  stratum above the chain. No change of the rule layer counts toward the budget. Until 2026-10-09 it
     *  already played: the driver counted a change of the rule layer only in the stratum of its writer and below, and
     *  the reference interpreter evaluated the rule only after the chain's fixpoint. */
    @Test void a_reaction_from_a_higher_stratum_does_not_count() throws Exception {
        playsInEveryOrder(List.of(WERF, LEG), List.of(NUMBERED), (c, r) -> documentL(101, c, r),
                caseL(List.of(TmsDocuments.expected("B1", "genummerd", true))));
        playsThroughTheDriver(documentL(101, List.of(WERF, LEG), List.of(NUMBERED)), 51 + 100);
    }

    // ---------------- document G: the budget of one chain, at its bound ----------------

    private static final String MODEL_G = TmsDocuments.model(List.of(
            objectType("Wachter", List.of(attribute("rang", WHOLE)), List.of())),
            List.of(factType("opvolging", "voorganger", "Wachter", "opvolger", "Wachter")));

    /** The chain of {@code corpus/red} {@code finite-chain-over-budget} without its consistency, while the rang is below
     *  {@code bound}: from W1 of rang 1, {@code grens - 1} generations in one stratum. */
    static Path documentG(int bound) throws Exception {
        return document(MODEL_G, List.of(), List.of(),
                List.of(objectCreation("werf de lange keten", "Wachter", "Wachter",
                        List.of(init("rang", binary("plus", ownRead("rang", "Wachter"), number("1")))),
                        comparison("less", ownRead("rang", "Wachter"), number(String.valueOf(bound))), "opvolging",
                        "opvolger")),
                List.of(), List.of(ruleGroup("lange estafette", List.of("werf de lange keten"))));
    }

    /** The sentence of a mint at depth 101 by the object creation {@code creation} of stratum {@code stratum}, on a subject
     *  of type {@code type} (RUNTIME-SEMANTICS ch. 5.5). */
    static String budget(String creation, int stratum, String type) {
        return "creation budget exhausted: objectcreatie \"" + creation + "\" van stratum " + stratum + " zou een object"
                + " maken op aanmaakdiepte 101, voor een " + type + " op diepte 100 — een keten van aanmaken die niet"
                + " stopt is een fout, nooit een stille afkapping (RUNTIME-SEMANTICS ch. 5.5)";
    }

    private static final String BUDGET = budget("werf de lange keten", 0, "Wachter");

    /** The bound of the creation budget (RUNTIME-SEMANTICS ch. 5.5), where the reference interpreter and the ASP backend
     *  put it: chains of 99 and 100 mints play, one generation per phase, and the mint at depth 101 is the fault, at the
     *  making, so the 101st object is not made. On this chain every generation mints one object on the last, so its
     *  depth is the generation that made it. Until 2026-10-09 the driver counted generations: the 101st generation still
     *  made its object, and the run ended after it without a fixpoint, "geen fixpunt binnen 100 generaties: generatie
     *  101 maakt nog 1 object of link", with 101 objects made. */
    @Test void a_creation_at_depth_100_plays_and_one_at_depth_101_is_a_fault() throws Exception {
        JsonNode caseDocument = TmsDocuments.caseDocument(List.of(instanceWithRoles("W1", "Wachter", Map.of("rang", "1"), List.of(),
                Map.of())), List.of(value("W1", "rang", "1")));
        for (int bound : List.of(100, 101, 102)) {
            int generations = bound - 1;
            assertEquals(generations <= 100 ? "gespeeld" : "case: " + BUDGET, TmsFaultWitnessTest.playBoth(documentG(bound),
                    caseDocument), generations + " generaties");
            Path ir = documentG(bound);
            for (DroolsBackend.Branch branch : BRANCHES) {
                DroolsBackend.Build b = DroolsBackend.build(ir, branch);
                for (List<String> names : sample(generated(b), 20261009L, 10)) {
                    String where = names + " op " + branch;
                    Object w1 = TmsDocuments.beanObject(b.beanLoader(), "Wachter", Map.of("rang", "1"), List.of());
                    List<Object[]> objects = new ArrayList<>();
                    assertEquals(generations <= 100 ? null : BUDGET, fireInOrder(b, names, List.of(w1), session -> { },
                            session -> objects.addAll(made(session))),
                            () -> generations + " generaties in volgorde " + where);
                    assertEquals(Math.min(generations, 100), objects.size(),
                            () -> "objecten na " + generations + " generaties in volgorde " + where);
                    for (int i = 0; i < objects.size(); i++)
                        assertEquals("Wachter in fase " + (i + 1), category(objects.get(i)), "volgorde " + where);
                    if (generations <= 100)
                        assertNumber(String.valueOf(bound), TmsDocuments.read(objects.get(generations - 1)[1], "rang"),
                                "rang van de laatste Wachter na " + generations + " generaties in volgorde " + where);
                }
            }
        }
    }

    // ---------------- document D: a chain over three strata ----------------

    /** "Een Wachter heeft een boei met nummer gelijk aan 1 indien zijn rang gelijk is aan 51": it reads the rang that
     *  "werf de opvolger" writes, strictly, so it sits a stratum above that chain. */
    static final String BOEI_AT_51 = objectCreation("leg een boei", "Wachter", "Boei", List.of(init("nummer", number("1"))),
            comparison("equal", ownRead("rang", "Wachter"), number("51")), "bewaking", "boei");

    /** "werf de opvolger" while the rang is below 51, "leg een boei" and the chain of Boeien while the nummer is below 51,
     *  without a wachterrang: 50 generations in stratum 0, one in stratum 1 and 50 in stratum 2. */
    static Path documentD(List<String> creations) throws Exception {
        List<String> c = new ArrayList<>();
        for (String creation : creations)
            c.add(creation.equals(WERF) ? objectCreation("werf de opvolger", "Wachter", "Wachter",
                    List.of(init("rang", binary("plus", ownRead("rang", "Wachter"), number("1")))),
                    comparison("less", ownRead("rang", "Wachter"), number("51")), "opvolging", "opvolger")
                    : creation.equals(LEG) ? objectCreation("leg de volgende boei", "Boei", "Boei",
                    List.of(init("nummer", binary("plus", ownRead("nummer", "Boei"), number("1")))),
                    comparison("less", ownRead("nummer", "Boei"), number("51")), "boeiketen", "volgende boei")
                    : creation);
        return document(MODEL_B, List.of(), List.of(), c, List.of(),
                List.of(ruleGroup("estafette", List.of("werf de opvolger")),
                        ruleGroup("boeienlijn", List.of("leg de volgende boei"))));
    }

    /** The chain the ASP backend counted over the strata until 2026-10-09 (50 + 1 + 50 mints, UNSAT at the 100th), and
     *  that the budget per stratum of RUNTIME-SEMANTICS ch. 5.5 admits: it plays in every order of its creations. The
     *  phases: stratum 0 makes W2 to W51 in phases 1 to 50 and closes in 51, stratum 1 makes B1 in 52 and closes in 53,
     *  stratum 2 makes B2 to B51 in 54 to 103. */
    @Test void a_chain_over_three_strata_counts_per_stratum() throws Exception {
        JsonNode caseDocument = TmsDocuments.caseDocument(List.of(instanceWithRoles("W1", "Wachter", Map.of("rang", "1"), List.of(),
                Map.of())), List.of(value("W1", "rang", "1")));
        playsInEveryOrder(List.of(WERF, BOEI_AT_51, LEG), List.of(), (creations, rules) -> documentD(creations), caseDocument);
        Path ir = documentD(List.of(WERF, BOEI_AT_51, LEG));
        for (DroolsBackend.Branch branch : BRANCHES) {
            DroolsBackend.Build b = DroolsBackend.build(ir, branch);
            for (List<String> names : sample(generated(b), 20261009L, 10)) {
                String where = names + " op " + branch;
                Object w1 = TmsDocuments.beanObject(b.beanLoader(), "Wachter", Map.of("rang", "1"), List.of());
                List<Object[]> objects = new ArrayList<>();
                assertNull(fireInOrder(b, names, List.of(w1), session -> { },
                        session -> objects.addAll(made(session))),
                        () -> "volgorde " + where);
                assertEquals(50 + 1 + 50, objects.size(), () -> "objecten in volgorde " + where);
                for (int i = 0; i < 50; i++) {
                    assertEquals("Wachter in fase " + (i + 1), category(objects.get(i)), "volgorde " + where);
                    assertEquals("Boei in fase " + (54 + i), category(objects.get(51 + i)), "volgorde " + where);
                }
                assertEquals("Boei in fase 52", category(objects.get(50)), "volgorde " + where);
                assertNumber("51", TmsDocuments.read(objects.get(49)[1], "rang"), "rang van W51 in volgorde " + where);
                assertNumber("51", TmsDocuments.read(objects.get(100)[1], "nummer"),
                        "nummer van B51 in volgorde " + where);
            }
        }
    }

    // ---------------- the depth, not the generations ----------------

    /** The anchor {@code jutterswet-erflijn} over a chain of {@code n} Wachters W1 → … → Wn of the input. */
    private static JsonNode inheritanceLineCase(int n) throws Exception {
        List<String> wachters = new ArrayList<>();
        for (int i = 1; i <= n; i++)
            wachters.add(instanceWithRoles("W" + i, "Wachter", Map.of(), List.of(),
                    i < n ? Map.of("opvolger", List.of("W" + (i + 1))) : Map.of()));
        return TmsDocuments.caseDocument(wachters, List.of(value("W1", "erfvolgeraantal", String.valueOf(n - 1))));
    }

    /** A fact creation mints nothing and carries no creation budget (RUNTIME-SEMANTICS ch. 5.5): over the finite universe
     *  the mints of a stratum leave, its links are finite. The anchor {@code jutterswet-erflijn} over 103 Wachters: "erf de
     *  lijn door" lays W1 → W103, 102 hops, in the 101st generation of its stratum, and the document plays, erfvolgeraantal
     *  of W1 102. Until 2026-10-09 the driver counted the generations that make an object or lay a link and ended this
     *  run without a fixpoint at the 101st, as the reference interpreter did; the ASP backend played it. */
    @Test void a_fact_creation_has_no_budget() throws Exception {
        Path d = Path.of("..", "..", "corpus", "golden", "anchor", "jutterswet-erflijn");
        assertEquals("gespeeld", TmsFaultWitnessTest.playBoth(d.resolve("ir.json"), inheritanceLineCase(103)));
    }

    /** "werf de opvolger" gives a Wachter whose rang is below {@code bound} and who has a maatgever an opvolger with rang
     *  plus 1, and "maak maten" makes every Wachter a maatgever of its opvolger; both are members of the recursive group
     *  "estafette", so they share one stratum. "Eén maat heeft meerdere maatgevers": a Wachter has one maat, so the case
     *  can give W0 its maat W1 in a reference field of the bean model, which a relation of two to-many roles lacks. */
    static Path documentE(int bound) throws Exception {
        String model = TmsDocuments.model(List.of(objectType("Wachter", List.of(attribute("rang", WHOLE)), List.of())),
                List.of(factType("opvolging", "voorganger", "Wachter", "opvolger", "Wachter"),
                        factTypeOneToMany("maatschap", "maat", "Wachter", "maatgever", "Wachter", "maatgevers")));
        String werf = objectCreation("werf de opvolger", "Wachter", "Wachter",
                List.of(init("rang", binary("plus", ownRead("rang", "Wachter"), number("1")))),
                TmsDocuments.compound("all", 0, List.of(
                        comparison("less", ownRead("rang", "Wachter"), number(String.valueOf(bound))),
                        comparison("greater", aggregation("count", MAATGEVERS, "Wachter", sourceRole("maatschap", "maatgever"),
                                "null", "null", COUNTING, "het aantal maatgevers van de Wachter"), number("0")))),
                "opvolging", "opvolger");
        String maten = factCreation("maak maten", "maatschap", "maatgever", "maat", "Wachter",
                List.of(roleBinding("opvolging", "opvolger")), "null");
        return document(model, List.of(), List.of(), List.of(werf), List.of(maten),
                List.of(ruleGroup("estafette", List.of("werf de opvolger", "maak maten"))));
    }

    /**
     * The budget counts the depth of a chain of mints, not the generations it takes (RUNTIME-SEMANTICS ch. 5.5): a mint
     * whose condition waits for a link keeps its subject's depth. W1 has the maatgever W0 in the input and makes W2 in
     * generation 1; W2 becomes a maat of W1 in generation 2 and makes W3 in generation 3, so W(k + 1) is made in
     * generation 2k - 1 at depth k. To rang 101 the chain makes W101 at depth 100, in generation 199, and plays, 100
     * objects made; to rang 102 the mint of W102 at depth 101 is the fault, and W102 is not made. The reference
     * interpreter and the ASP backend give the same on this shape ({@code ref/test} {@code WV-5.5-budget-wacht-op-link},
     * the clingo suite's {@code ch5.5 creation budget} frames). Until 2026-10-09 the driver counted generations and ended
     * the chain without a fixpoint from rang 52 on, where W52 is made in generation 101.
     */
    @Test void a_creation_that_waits_for_a_link_keeps_the_depth_of_its_subject() throws Exception {
        JsonNode caseDocument = TmsDocuments.caseDocument(List.of(
                instanceWithRoles("W0", "Wachter", Map.of("rang", "0"), List.of(), Map.of("maat", List.of("W1"))),
                instanceWithRoles("W1", "Wachter", Map.of("rang", "1"), List.of(), Map.of())),
                List.of(value("W1", "rang", "1")));
        String fault = budget("werf de opvolger", 0, "Wachter");
        for (int bound : List.of(101, 102)) {
            assertEquals(bound <= 101 ? "gespeeld" : "case: " + fault, TmsFaultWitnessTest.playBoth(documentE(bound), caseDocument), "tot rang " + bound);
            Path ir = documentE(bound);
            for (DroolsBackend.Branch branch : BRANCHES) {
                DroolsBackend.Build b = DroolsBackend.build(ir, branch);
                for (List<String> names : sample(generated(b), 20261009L, 10)) {
                    String where = names + " op " + branch;
                    Object w0 = TmsDocuments.beanObject(b.beanLoader(), "Wachter", Map.of("rang", "0"), List.of());
                    Object w1 = TmsDocuments.beanObject(b.beanLoader(), "Wachter", Map.of("rang", "1"), List.of());
                    setRole(w0, "maat", w1);
                    List<Object[]> objects = new ArrayList<>();
                    assertEquals(bound <= 101 ? null : fault, fireInOrder(b, names, List.of(w0, w1), session -> { },
                            session -> objects.addAll(made(session))),
                            () -> "tot rang " + bound + " in volgorde " + where);
                    assertEquals(100, objects.size(), () -> "objecten tot rang " + bound + " in volgorde " + where);
                    for (int i = 0; i < objects.size(); i++)
                        assertEquals("Wachter in fase " + (2 * i + 1), category(objects.get(i)), "volgorde " + where);
                    // a run that ends at the budget writes nothing back onto the beans
                    if (bound <= 101)
                        assertNumber("101", TmsDocuments.read(objects.get(99)[1], "rang"),
                                "rang van de laatste Wachter in volgorde " + where);
                }
            }
        }
    }

    /** A creation never meets a subject that a higher stratum made: a creation reads its subject's population strictly
     *  (RUNTIME-SEMANTICS ch. 6.1), so it sits at or above every creation that makes its subjects, and no verifier-clean
     *  document gives it such a subject. The creation rule tests it where it computes the depth of what it makes, and
     *  fails loudly rather than count from a stratum that has not run: here a forged mark, put in before the fire, says
     *  that a creation of stratum 1 made W1, the subject of "werf de lange keten" of stratum 0. */
    @Test void a_subject_from_a_higher_stratum_fails_loudly() throws Exception {
        DroolsBackend.Build b = DroolsBackend.build(documentG(5), DroolsBackend.Branch.EXEC_MODEL);
        Object w1 = TmsDocuments.beanObject(b.beanLoader(), "Wachter", Map.of("rang", "1"), List.of());
        Object maker = TmsDocuments.beanObject(b.beanLoader(), "Wachter", Map.of("rang", "0"), List.of());
        Object falsified = Class.forName("dev.uchar.regelspraak.transpiler.tms.CreationStamp").getConstructors()[0]
                .newInstance("vervalst", maker, w1, 1000, 1, 1);
        RuntimeException e = org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class,
                () -> fire(b, b.execModelTranspiler().model().getRules(), List.of(w1, falsified), session -> { }, session -> { }));
        // Drools hands what a consequence throws on inside its own ConsequenceException
        StringBuilder chain = new StringBuilder();
        for (Throwable t = e; t != null; t = t.getCause() == t ? null : t.getCause())
            chain.append(t.getClass().getSimpleName()).append(": ").append(t.getMessage()).append(" | ");
        assertTrue(chain.toString().contains("IllegalStateException: exec-model: objectcreatie \"werf de lange keten\" van"
                + " stratum 0 ontmoet een onderwerp dat een creatie van stratum 1 maakte"), chain::toString);
    }

    // ---------------- document V: the creations of one generation read one world ----------------

    private static final String MAATGEVERS = "{\"node\":\"AttributeReference\",\"attribute\":\"maatgevers\","
            + "\"subjectType\":\"Wachter\",\"source\":{\"snippet\":\"maatgevers van de Wachter\"}}";

    /** "Eén voorganger heeft één opvolger" and "Een maatgever heeft meerdere maten, een maat meerdere maatgevers". */
    private static final String MODEL_V = TmsDocuments.model(List.of(
            objectType("Wachter", List.of(attribute("rang", WHOLE), attribute("beginstand", WHOLE),
                    attribute("opvolgerbegin", WHOLE), attribute("verder", WHOLE)), List.of())),
            List.of(factType("opvolging", "voorganger", "Wachter", "opvolger", "Wachter"),
                    "{\"node\":\"FactType\",\"name\":\"maatschap\",\"reciprocal\":false,\"roles\":[{\"node\":\"Role\","
                            + "\"name\":\"maatgever\",\"objectType\":\"Wachter\",\"plural\":\"maatgevers\",\"many\":true},"
                            + "{\"node\":\"Role\",\"name\":\"maat\",\"objectType\":\"Wachter\",\"plural\":\"maten\","
                            + "\"many\":true}]}"));

    static final List<String> RULES_V = List.of(
            computingRule("opvolgerbegin", "GELIJKSTELLING", "Wachter", "opvolgerbegin",
                    pathRead("beginstand", "Wachter", "opvolging", "opvolger"), "null"),
            computingRule("verder", "GELIJKSTELLING", "Wachter", "verder",
                    pathRead("opvolgerbegin", "Wachter", "opvolging", "opvolger"), "null"));

    /** Vorm V2 of the card creatie-initialisatie-leest-groeiende-waarde, the recursive group "estafette" of two creations
     *  on one cycle, so in one stratum: "werf de opvolger" gives a Wachter of rang below 4 an opvolger with rang plus 1
     *  and beginstand the number of its maatgevers, and "maak maten" makes every Wachter a maatgever of its opvolger.
     *  "opvolgerbegin" reads the beginstand of the opvolger and "verder" the opvolgerbegin of the opvolger. */
    static Path documentV(List<String> rules) throws Exception {
        String werf = objectCreation("werf de opvolger", "Wachter", "Wachter", List.of(
                init("rang", binary("plus", ownRead("rang", "Wachter"), number("1"))),
                init("beginstand", aggregation("count", MAATGEVERS, "Wachter", sourceRole("maatschap", "maatgever"), "null",
                        "null", COUNTING, "het aantal maatgevers van de Wachter"))),
                comparison("less", ownRead("rang", "Wachter"), number("4")), "opvolging", "opvolger");
        String maten = factCreation("maak maten", "maatschap", "maatgever", "maat", "Wachter",
                List.of(roleBinding("opvolging", "opvolger")), "null");
        return document(MODEL_V, rules, List.of(), List.of(werf), List.of(maten),
                List.of(ruleGroup("estafette", List.of("werf de opvolger", "maak maten"))));
    }

    /**
     * Every creation of a generation reads the settled world of the generation before (RUNTIME-SEMANTICS ch. 5.5,
     * Generations), so an object creation does not count a link a fact creation of its own generation lays. W1 makes W2
     * in generation 1; in generation 2 "maak maten" makes W1 a maatgever of W2 while W2 makes W3, on the world in which
     * W2 has no maatgever yet: W3 starts at beginstand 0, and so does W4. The reference interpreter gives
     * opvolgerbegin(W1) = 0 and verder(W1) = 0 in every order of the rules. One phase per generation: W2, W3 and W4 in
     * phases 1, 2 and 3, the maatschap links in phases 2, 3 and 4, and phase 5 makes nothing.
     */
    @Test void the_creations_of_a_generation_read_a_world() throws Exception {
        JsonNode caseDocument = TmsDocuments.caseDocument(List.of(instanceWithRoles("W1", "Wachter", Map.of("rang", "1"), List.of(),
                Map.of())), List.of(value("W1", "opvolgerbegin", "0"), value("W1", "verder", "0")));
        playsInEveryOrder(List.of(), RULES_V, (creations, rules) -> documentV(rules), caseDocument);
        Path ir = documentV(RULES_V);
        for (DroolsBackend.Branch branch : BRANCHES) {
            DroolsBackend.Build b = DroolsBackend.build(ir, branch);
            for (List<String> names : sample(generated(b), 20261009L, 100)) {
                String where = names + " op " + branch;
                Object w1 = TmsDocuments.beanObject(b.beanLoader(), "Wachter", Map.of("rang", "1"), List.of());
                List<Object[]> objects = new ArrayList<>(), left = new ArrayList<>();
                assertNull(fireInOrder(b, names, List.of(w1), session -> { }, session -> {
                    objects.addAll(made(session));
                    left.addAll(links(session, "maatschap"));
                }), () -> "volgorde " + where);
                assertNumber("0", TmsDocuments.read(w1, "opvolgerbegin"), "opvolgerbegin van W1 in volgorde " + where);
                assertNumber("0", TmsDocuments.read(w1, "verder"), "verder van W1 in volgorde " + where);
                assertEquals(List.of("Wachter in fase 1", "Wachter in fase 2", "Wachter in fase 3"), kinds(objects),
                        () -> "volgorde " + where);
                for (Object[] g : objects)
                    assertNumber("0", TmsDocuments.read(g[1], "beginstand"), "beginstand in volgorde " + where);
                List<Integer> phases = new ArrayList<>();
                for (Object[] l : left) phases.add((Integer) l[2]);
                phases.sort(null);
                assertEquals(List.of(2, 3, 4), phases, () -> "fasen van de maatschap in volgorde " + where);
            }
        }
    }

    // ---------------- document R: an object creation the rule layer enables waits for the next generation ----------

    private static final String OPVOLGERS = "{\"node\":\"AttributeReference\",\"attribute\":\"opvolgers\","
            + "\"subjectType\":\"Wachter\",\"source\":{\"snippet\":\"opvolgers van de Wachter\"}}";

    private static final String BELONINGEN = "{\"node\":\"AttributeReference\",\"attribute\":\"beloningen\","
            + "\"subjectType\":\"Wachter\",\"source\":{\"snippet\":\"beloningen van de Wachter\"}}";

    /** "Eén voorganger heeft één opvolger", the opvolger role with the plural a count over it spells, and "Een
     *  ontvanger heeft meerdere beloningen". */
    private static final String MODEL_R = TmsDocuments.model(List.of(
            objectType("Wachter", List.of(attribute("rang", WHOLE), attribute("stand", WHOLE),
                    attribute("beloningaantal", WHOLE), attribute("erfbeloning", WHOLE),
                    attribute("beginbeloning", WHOLE), attribute("opvolgerbegin", WHOLE),
                    attribute("verder", WHOLE)), List.of()),
            objectType("Beloning", List.of(), List.of())),
            List.of("{\"node\":\"FactType\",\"name\":\"opvolging\",\"reciprocal\":false,\"roles\":["
                            + "{\"node\":\"Role\",\"name\":\"voorganger\",\"objectType\":\"Wachter\",\"many\":false},"
                            + "{\"node\":\"Role\",\"name\":\"opvolger\",\"objectType\":\"Wachter\","
                            + "\"plural\":\"opvolgers\",\"many\":false}]}",
                    factTypeOneToMany("toekenning", "ontvanger", "Wachter", "beloning", "Beloning", "beloningen")));

    /** "werf de opvolger": a Wachter of rang below 4 gets an opvolger with rang plus 1 and beginbeloning its
     *  erfbeloning. */
    private static final String WERF_R = objectCreation("werf de opvolger", "Wachter", "Wachter", List.of(
            init("rang", binary("plus", ownRead("rang", "Wachter"), number("1"))),
            init("beginbeloning", ownRead("erfbeloning", "Wachter"))),
            comparison("less", ownRead("rang", "Wachter"), number("4")), "opvolging", "opvolger");

    /** "beloon de wachter": a Wachter whose stand is above 0 gets a Beloning. */
    private static final String BELOON_R = objectCreation("beloon de wachter", "Wachter", "Beloning", List.of(),
            comparison("greater", ownRead("stand", "Wachter"), number("0")), "toekenning", "beloning");

    /** The rules on the cycle of document R: the stand counts the opvolgers, the beloningaantal the beloningen, and the
     *  erfbeloning reads the beloningaantal of the voorganger. */
    private static final List<String> CYCLE_R = List.of(
            computingRule("stand van een wachter", "BEREKENING", "Wachter", "stand", aggregation("count", OPVOLGERS,
                    "Wachter", sourceRole("opvolging", "opvolger"), "null", "null", COUNTING,
                    "het aantal opvolgers van de Wachter"), "null"),
            computingRule("beloningaantal", "BEREKENING", "Wachter", "beloningaantal", aggregation("count", BELONINGEN,
                    "Beloning", sourceRole("toekenning", "beloning"), "null", "null", COUNTING,
                    "het aantal beloningen van de Wachter"), "null"),
            computingRule("erfbeloning", "GELIJKSTELLING", "Wachter", "erfbeloning",
                    pathRead("beloningaantal", "Wachter", "opvolging", "voorganger"), "null"));

    /** The rules that read the chain back onto W1: "opvolgerbegin" the beginbeloning of the opvolger, "verder" the
     *  opvolgerbegin of the opvolger. */
    private static final List<String> OBSERVATION_R = List.of(
            computingRule("opvolgerbegin", "GELIJKSTELLING", "Wachter", "opvolgerbegin",
                    pathRead("beginbeloning", "Wachter", "opvolging", "opvolger"), "null"),
            computingRule("verder", "GELIJKSTELLING", "Wachter", "verder",
                    pathRead("opvolgerbegin", "Wachter", "opvolging", "opvolger"), "null"));

    /** The recursive group "estafette" of "werf de opvolger" and "beloon de wachter", the {@code creations} and the
     *  rules on the cycle ({@code rules}) in the order given, then {@link #OBSERVATION_R}. One cycle runs through both
     *  creations, so they share a stratum: "werf" makes the opvolger that "stand" counts, "beloon" reads the stand and
     *  makes the Beloning that "beloningaantal" counts, and "werf" initialises the beginbeloning from the
     *  erfbeloning, which reads that count on the voorganger. */
    static Path documentR(List<String> creations, List<String> rules) throws Exception {
        List<String> all = new ArrayList<>(rules);
        all.addAll(OBSERVATION_R);
        return document(MODEL_R, all, List.of(), creations, List.of(),
                List.of(ruleGroup("estafette", List.of("werf de opvolger", "beloon de wachter"))));
    }

    /**
     * An object creation whose condition the rule layer makes true after a generation fires in the next generation,
     * on the settled world, together with the other creations of that generation, and none of them reads what it
     * makes (RUNTIME-SEMANTICS ch. 5.5, Generations). W1 makes W2 in generation 1; after it the stand of W1 is 1, so
     * "beloon" gives W1 its Beloning in generation 2, while W2 makes W3 on the world of generation 1, in which W1 has
     * no Beloning yet: W3 starts at beginbeloning 0. The same holds one link further: W4 starts at 0, and the
     * Beloningen of W1, W2 and W3 come in generations 2, 3 and 4. W2 starts without a beginbeloning, since W1 has no
     * voorganger. The reference interpreter gives stand(W1) = 1, beloningaantal(W1) = 1, opvolgerbegin(W1) leeg and
     * verder(W1) = 0 in each of the twelve orders of the two creations and the three rules on the cycle in the
     * document.
     *
     * <p>This is the witness for an object creation that fires outside the driver ({@code DroolsBackend.fire}). The
     * driver gives the group of the object creations the focus once per generation, at a fixpoint of the rule layer.
     * With auto-focus on the object creations "beloon" gets its match, and the focus, as soon as the rule layer sets
     * the stand of W1 to 1 in generation 1, and fires in that generation, on a world that is not settled. The rule
     * layer then counts the Beloning before W2 makes W3, so W3 and W4 start at beginbeloning 1, verder(W1) is 1, and
     * the Beloningen come in phases 1, 2 and 3. Measured on 2026-10-09 with that mutant
     * ({@code .attribute(Rule.Attribute.AUTO_FOCUS, true)} on the rule of an object creation in
     * {@code TmsTranspiler.objectCreation}): the replay fails in all twelve orders, "case: attribuut verder: verwacht
     * 0, werkelijk 1". Until then no witness of this class or of {@code TmsCreationWitnessTest} failed on that mutant.
     *
     * <p>The ASP backend refuses the document by name: "the initialisation of beginbeloning reads
     * attr:wachter:erfbeloning, which rule "erfbeloning" writes on a dependency cycle through the creation …"
     * (RUNTIME-SEMANTICS ch. 5.5, the ASP backend's second posture).
     */
    @Test void an_object_creation_that_the_rule_layer_makes_possible_fires_in_the_next_generation() throws Exception {
        JsonNode caseDocument = TmsDocuments.caseDocument(List.of(instanceWithRoles("W1", "Wachter", Map.of("rang", "1"), List.of(),
                Map.of())), List.of(value("W1", "stand", "1"), value("W1", "beloningaantal", "1"),
                value("W1", "opvolgerbegin", null), value("W1", "verder", "0")));
        playsInEveryOrder(List.of(WERF_R, BELOON_R), CYCLE_R, TmsCreationStratumWitnessTest::documentR, caseDocument);
        Path ir = documentR(List.of(WERF_R, BELOON_R), CYCLE_R);
        for (DroolsBackend.Branch branch : BRANCHES) {
            DroolsBackend.Build b = DroolsBackend.build(ir, branch);
            for (List<String> names : sample(generated(b), 20261009L, 100)) {
                String where = names + " op " + branch;
                Object w1 = TmsDocuments.beanObject(b.beanLoader(), "Wachter", Map.of("rang", "1"), List.of());
                List<Object[]> objects = new ArrayList<>();
                assertNull(fireInOrder(b, names, List.of(w1), session -> { },
                        session -> objects.addAll(made(session))),
                        () -> "volgorde " + where);
                assertNumber("0", TmsDocuments.read(w1, "verder"), "verder van W1 in volgorde " + where);
                assertNumber("1", TmsDocuments.read(w1, "beloningaantal"), "beloningaantal van W1 in volgorde " + where);
                List<String> kinds = new ArrayList<>(kinds(objects));
                kinds.sort(null);
                assertEquals(List.of("Beloning in fase 2", "Beloning in fase 3", "Beloning in fase 4",
                        "Wachter in fase 1", "Wachter in fase 2", "Wachter in fase 3"), kinds,
                        () -> "volgorde " + where);
                for (Object[] g : objects) {
                    if (!g[1].getClass().getSimpleName().equals("Wachter")) continue;
                    if ((Integer) g[2] == 1)
                        assertNull(TmsDocuments.read(g[1], "beginbeloning"),
                                () -> "beginbeloning van W2 in volgorde " + where);
                    else
                        assertNumber("0", TmsDocuments.read(g[1], "beginbeloning"),
                                "beginbeloning in volgorde " + where);
                }
            }
        }
    }

    /** W1 of rang 1, watching B1 of nummer 1, as beans for the driver. */
    private static List<Object> boeienlijn(DroolsBackend.Build b) throws Exception {
        Object w1 = TmsDocuments.beanObject(b.beanLoader(), "Wachter", Map.of("rang", "1"), List.of());
        Object b1 = TmsDocuments.beanObject(b.beanLoader(), "Boei", Map.of("nummer", "1"), List.of());
        setRole(w1, "boei", b1);
        setRole(b1, "wachter", w1);
        return List.of(w1, b1);
    }

    /** Fires document {@code ir} on {@link #boeienlijn} through the driver in a sample of orders of its rules: each
     *  reaches its fixpoint with {@code objects} objects made. */
    private static void playsThroughTheDriver(Path ir, int objects) throws Exception {
        for (DroolsBackend.Branch branch : BRANCHES) {
            DroolsBackend.Build b = DroolsBackend.build(ir, branch);
            for (List<String> names : sample(generated(b), 20261008L, 10)) {
                String where = names + " op " + branch;
                List<Object[]> made = new ArrayList<>();
                assertNull(fireInOrder(b, names, boeienlijn(b), session -> { },
                        session -> made.addAll(made(session))),
                        () -> "volgorde " + where);
                assertEquals(objects, made.size(), () -> "objecten in volgorde " + where);
            }
        }
    }

    // ---------------- playing and reading ----------------

    /** A document of {@code creations} and {@code rules} in the order given. */
    interface Build {
        Path van(List<String> creations, List<String> rules) throws Exception;
    }

    /** Plays case {@code caseDocument} through the replay on every order of {@code creations} and of {@code rules} in the
     *  document; each must play. */
    private static void playsInEveryOrder(List<String> creations, List<String> rules, Build build, JsonNode caseDocument)
            throws Exception {
        List<String> outcomes = new ArrayList<>(), expected = new ArrayList<>();
        for (List<String> c : permutations(creations))
            for (List<String> r : permutations(rules)) {
                String names = ruleNames(c) + " " + ruleNames(r);
                outcomes.add(names + " " + TmsFaultWitnessTest.playBoth(build.van(c, r), caseDocument));
                expected.add(names + " gespeeld");
            }
        assertEquals(expected, outcomes);
    }

    /** The made objects as "&lt;type&gt; in fase &lt;phase&gt;", in the order of {@link TmsCreationWitnessTest#made}. */
    private static List<String> kinds(List<Object[]> made) {
        return made.stream().map(TmsCreationStratumWitnessTest::category).toList();
    }

    private static String category(Object[] g) {
        return g[1].getClass().getSimpleName() + " in fase " + g[2];
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
