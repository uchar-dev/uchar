package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;

import dev.uchar.regelspraak.transpiler.tms.ListPlace;
import dev.uchar.regelspraak.transpiler.tms.Value;

import org.drools.model.Rule;
import org.junit.jupiter.api.Test;
import org.kie.api.event.rule.AfterMatchFiredEvent;
import org.kie.api.event.rule.DefaultAgendaEventListener;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static dev.uchar.regelspraak.cases.TmsDocuments.WHOLE;
import static dev.uchar.regelspraak.cases.TmsDocuments.COUNTING;
import static dev.uchar.regelspraak.cases.TmsDocuments.aggregation;
import static dev.uchar.regelspraak.cases.TmsDocuments.attribuut;
import static dev.uchar.regelspraak.cases.TmsDocuments.attributeGroup;
import static dev.uchar.regelspraak.cases.TmsDocuments.binary;
import static dev.uchar.regelspraak.cases.TmsDocuments.sourcePath;
import static dev.uchar.regelspraak.cases.TmsDocuments.sourceRole;
import static dev.uchar.regelspraak.cases.TmsDocuments.check;
import static dev.uchar.regelspraak.cases.TmsDocuments.consistent;
import static dev.uchar.regelspraak.cases.TmsDocuments.factTypeOneToMany;
import static dev.uchar.regelspraak.cases.TmsDocuments.listedFold;
import static dev.uchar.regelspraak.cases.TmsDocuments.number;
import static dev.uchar.regelspraak.cases.TmsDocuments.instanceWithRoles;
import static dev.uchar.regelspraak.cases.TmsDocuments.characteristicRule;
import static dev.uchar.regelspraak.cases.TmsDocuments.reading;
import static dev.uchar.regelspraak.cases.TmsDocuments.computingRule;
import static dev.uchar.regelspraak.cases.TmsDocuments.roleBinding;
import static dev.uchar.regelspraak.cases.TmsDocuments.standaard;
import static dev.uchar.regelspraak.cases.TmsDocuments.comparison;
import static dev.uchar.regelspraak.cases.TmsDocuments.waarde;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Aggregations on the exec-model branch (RUNTIME-SEMANTICS ch. 1.2 "Aggregations", ch. 3.4a-c): a fold over the
 * members its range reaches recomputes when a member, or a value derived for a member, changes, and its activation
 * that fires again leaves one value for its target, not two.
 *
 * <p>The model: a Jutter reports Vondsten (aanmelding: aanmelder, one Jutter; vondst, many Vondsten, plural vondsten).
 * A Vondst has taxatie and bergloon and the characteristics gemeld, erkend and vrij; "Een Vondst is erkend indien hij
 * gemeld is", "Een Vondst is vrij indien hij niet erkend is"; a defaults group on the bergloon, higher "op zijn taxatie
 * indien hij erkend is", lower "op 5 indien hij gemeld is". The folds: "de som van het bergloon van alle vondsten van de
 * Jutter, of 0 als die er niet zijn" (bergsom), "het aantal vondsten van de Jutter die erkend zijn" (erkendaantal) and
 * "… die vrij zijn" (vrijaantal). J1 reports V1 (gemeld, taxatie 30), V2 (gemeld, taxatie 20) and V3 (taxatie 10, not
 * gemeld, so no bergloon); J2 reports nothing. Fixpoint: bergsom 50, erkendaantal 2 and vrijaantal 1 for J1, the leeg
 * bergloon of V3 skipped; 0, 0 and 0 for J2, the sum by its fallback. On the way the folds pass the lower default's 5
 * as a member's value, a count of erkende vondsten below 2, and a count of vrije vondsten that still holds V1 and V2,
 * which are vrij until erkend is derived for them: a contribution that stayed after its member changed would show in
 * every one of the three. The reference interpreter's runner and the ASP runner passed every case of this class in
 * every document order (measured 2026-10-08), the two standing divisions by zero excepted, on which both fault.
 *
 * <p>Besides: a fold in the justification and the consequence of a default; a division by a count that is 0 on the
 * way and 2 at the fixpoint, beside one whose count stays 0; a fault in a fold's filter; a listed fold over an operand
 * derived later; and a two-step range in which one element is reached along two routes and contributes once
 * (ch. 3.4b).
 *
 * <p><b>Red before the change</b> (measured 2026-10-08 on the branch after its first step): every row of this branch
 * was refused, "Excluded: tms-transpiler SKIP: regel: bergsom — een Aggregation in een waarde ('de som van het bergloon
 * van alle vondsten van de Jutter of 0 als die er niet zijn'): buiten de proef van de truth-maintenance-tak", the default
 * on "een Aggregation in een waarde ('het aantal vondsten van de Jutter die erkend zijn')", and the listed fold, which
 * carries no element type, as a builtin: "een BuiltinCall in een waarde ('de eerste van zijn taxatie en zijn
 * bergloon')". The exec-model rows were green before.
 *
 * <p><b>Red by mutation</b> (measured the same day, not committed). With the contributions inserted by {@code insert}
 * instead of {@code logicalInsert}, a contribution outlived the value it was made from: "bergsom van J1 in volgorde
 * [kenmerkstand Vondst.vrij: onwaar, …]", the document orders, the default and the division failed, five rows in all.
 * (Before the lower default was 5 and the count of vrije vondsten was added, the stale contributions were 0 and a
 * member that stayed erkend, and that mutant passed every row; hence those two.) With contributions that never merge
 * (equality by identity), this class passed and the corpus witness failed on the five goldens with an instance-set
 * union, "jutterswet-keurunie: … attribuut aantal keurstukken: verwacht 3, werkelijk 4": two selections of a union
 * select one member twice, and only the merge of equal contributions counts it once.
 */
class TmsAggregationWitnessTest {

    static final String MODEL = TmsDocuments.model(List.of(
            TmsDocuments.objecttype("Jutter", List.of(attribuut("bergsom", WHOLE), attribuut("erkendaantal", WHOLE),
                    attribuut("vrijaantal", WHOLE), attribuut("gemiddeld bergloon", WHOLE), attribuut("premie", WHOLE)),
                    List.of()),
            TmsDocuments.objecttype("Vondst", List.of(attribuut("taxatie", WHOLE), attribuut("bergloon", WHOLE),
                    attribuut("ondergrens", WHOLE)), List.of("gemeld", "erkend", "vrij"))),
            List.of(factTypeOneToMany("aanmelding", "aanmelder", "Jutter", "vondst", "Vondst", "vondsten")));

    private static final String ERKENNING = characteristicRule("erkenning", "Vondst", "erkend", check("gemeld", false));

    private static final String VRIJGAVE = characteristicRule("vrijgave", "Vondst", "vrij", check("erkend", true));

    private static final String BERGLOON = attributeGroup("bergloon van een vondst", "Vondst", "bergloon", List.of(
            standaard(check("erkend", false), reading("taxatie", "Vondst")),
            standaard(check("gemeld", false), number("5"))));

    private static final String VONDSTEN = "{\"node\":\"AttributeReference\",\"attribute\":\"vondsten\",\"subjectType\":"
            + "\"Jutter\",\"source\":{\"snippet\":\"vondsten van de Jutter\"}}";

    static final String SUM = aggregation("sum", reading("bergloon", "Vondst"), "Vondst", sourceRole("aanmelding", "vondst"),
            "null", number("0"), WHOLE, "de som van het bergloon van alle vondsten van de Jutter of 0 als die er niet zijn");

    static final String ERKEND_COUNT = aggregation("count", VONDSTEN, "Vondst", sourceRole("aanmelding", "vondst"),
            check("erkend", false), "null", COUNTING, "het aantal vondsten van de Jutter die erkend zijn");

    static final String COUNT = aggregation("count", VONDSTEN, "Vondst", sourceRole("aanmelding", "vondst"), "null", "null",
            COUNTING, "het aantal vondsten van de Jutter");

    static final String FREE_COUNT = aggregation("count", VONDSTEN, "Vondst", sourceRole("aanmelding", "vondst"),
            check("vrij", false), "null", COUNTING, "het aantal vondsten van de Jutter die vrij zijn");

    static final List<String> RULES = List.of(ERKENNING, VRIJGAVE,
            computingRule("bergsom", "BEREKENING", "Jutter", "bergsom", SUM, "null"),
            computingRule("erkendaantal", "BEREKENING", "Jutter", "erkendaantal", ERKEND_COUNT, "null"),
            computingRule("vrijaantal", "BEREKENING", "Jutter", "vrijaantal", FREE_COUNT, "null"));

    static Path document(List<String> rules) throws Exception {
        return TmsDocuments.document(MODEL, rules, List.of(), List.of(BERGLOON));
    }

    private static List<String> vondsten() {
        return List.of(
                instanceWithRoles("V1", "Vondst", Map.of("taxatie", "30"), List.of("gemeld"), Map.of()),
                instanceWithRoles("V2", "Vondst", Map.of("taxatie", "20"), List.of("gemeld"), Map.of()),
                instanceWithRoles("V3", "Vondst", Map.of("taxatie", "10"), List.of(), Map.of()));
    }

    static JsonNode caseDocument() throws Exception {
        List<String> instanties = new ArrayList<>(List.of(
                instanceWithRoles("J1", "Jutter", Map.of(), List.of(), Map.of("vondsten", List.of("V1", "V2", "V3"))),
                instanceWithRoles("J2", "Jutter", Map.of(), List.of(), Map.of())));
        instanties.addAll(vondsten());
        return TmsDocuments.caseDocument(instanties, List.of(waarde("J1", "bergsom", "50"), waarde("J1", "erkendaantal", "2"),
                waarde("J1", "vrijaantal", "1"), waarde("J2", "bergsom", "0"), waarde("J2", "erkendaantal", "0"),
                waarde("J2", "vrijaantal", "0"), waarde("V1", "bergloon", "30"), waarde("V2", "bergloon", "20"),
                waarde("V3", "bergloon", null), consistent()));
    }

    // ---------------- a fold over members whose values arrive later ----------------

    @Test void every_order_of_the_rules_in_the_document() throws Exception {
        List<String> outcomes = new ArrayList<>(), verwacht = new ArrayList<>();
        for (List<String> order : TmsDocuments.permutations(RULES)) {
            List<String> names = TmsDocuments.ruleNames(order);
            outcomes.add(names + " " + TmsFaultWitnessTest.play(document(order), caseDocument(),
                    DroolsBackend.Branch.EXEC_MODEL));
            verwacht.add(names + " gespeeld");
        }
        assertEquals(verwacht, outcomes);
        assertEquals(120, outcomes.size());
    }

    /**
     * A seeded sample of orders of the generated rules. In every order the beans end at the fixpoint and the session
     * holds exactly one {@link Value} for the bergsom of J1 and one for its erkendaantal: when the result of an
     * accumulate changes, the activation that read it fires again, and Drools withdraws the logical facts its earlier
     * firing inserted and did not insert again. At least one order must let the bergsom fire on J1 more than once with
     * different values on the way, and the count fire on a count below 2.
     */
    @Test void a_fold_firing_again_leaves_a_value_for_the_target() throws Exception {
        DroolsBackend.Build b = DroolsBackend.build(document(RULES), DroolsBackend.Branch.EXEC_MODEL);
        List<Rule> gegenereerd = b.execModelTranspiler().model().getRules();
        int refiredAt = 0, intermediate = 0, dropped = 0;
        for (List<Rule> order : TmsDocuments.sample(gegenereerd, 20261008L, 100)) {
            List<String> names = order.stream().map(Rule::getName).toList();
            Object j1 = TmsDocuments.beanObject(b.beanLoader(), "Jutter", Map.of(), List.of());
            Object j2 = TmsDocuments.beanObject(b.beanLoader(), "Jutter", Map.of(), List.of());
            Object v1 = TmsDocuments.beanObject(b.beanLoader(), "Vondst", Map.of("taxatie", "30"), List.of("gemeld"));
            Object v2 = TmsDocuments.beanObject(b.beanLoader(), "Vondst", Map.of("taxatie", "20"), List.of("gemeld"));
            Object v3 = TmsDocuments.beanObject(b.beanLoader(), "Vondst", Map.of("taxatie", "10"), List.of());
            for (Object v : List.of(v1, v2, v3)) setRole(v, "aanmelder", j1);
            Map<String, List<Object>> seen = new java.util.HashMap<>();
            List<Long> counts = new ArrayList<>(), exemptions = new ArrayList<>();
            int[] waarden = {0, 0};
            TmsDocuments.vuur(b, order, List.of(j1, j2, v1, v2, v3), sessie ->
                    sessie.addEventListener(new DefaultAgendaEventListener() {
                        @Override public void afterMatchFired(AfterMatchFiredEvent e) {
                            String regel = e.getMatch().getRule().getName();
                            if ((regel.equals("bergsom") || regel.equals("erkendaantal"))
                                    && e.getMatch().getObjects().contains(j1))
                                seen.computeIfAbsent(regel, r -> new ArrayList<>()).add(boundNumber(e));
                            if (regel.equals("erkendaantal") && e.getMatch().getObjects().contains(j1))
                                counts.add(smallestNumber(e));
                            if (regel.equals("vrijaantal") && e.getMatch().getObjects().contains(j1))
                                exemptions.add(smallestNumber(e));
                        }
                    }), sessie -> {
                        waarden[0] = sessie.getObjects(o -> o instanceof Value w && w.onderwerp() == j1
                                && w.attribuut().naam().equals("bergsom")).size();
                        waarden[1] = sessie.getObjects(o -> o instanceof Value w && w.onderwerp() == j1
                                && w.attribuut().naam().equals("erkendaantal")).size();
                    });
            assertEquals(List.of(1, 1), List.of(waarden[0], waarden[1]), () -> "Waarde-feiten van J1 in volgorde " + names);
            assertNumber("50", TmsDocuments.lees(j1, "bergsom"), "bergsom van J1 in volgorde " + names);
            assertNumber("2", TmsDocuments.lees(j1, "erkendaantal"), "erkendaantal van J1 in volgorde " + names);
            assertNumber("1", TmsDocuments.lees(j1, "vrijaantal"), "vrijaantal van J1 in volgorde " + names);
            assertNumber("0", TmsDocuments.lees(j2, "bergsom"), "bergsom van J2 in volgorde " + names);
            assertNumber("0", TmsDocuments.lees(j2, "erkendaantal"), "erkendaantal van J2 in volgorde " + names);
            assertNumber("0", TmsDocuments.lees(j2, "vrijaantal"), "vrijaantal van J2 in volgorde " + names);
            List<Object> sums = seen.getOrDefault("bergsom", List.of());
            if (sums.size() > 1 && sums.stream().distinct().count() > 1) refiredAt++;
            if (counts.stream().anyMatch(x -> x != null && x < 2)) intermediate++;
            if (exemptions.stream().anyMatch(x -> x != null && x > 1)) dropped++;
        }
        assertTrue(refiredAt > 0, "geen volgorde liet de som van J1 met verschillende waarden opnieuw vuren");
        assertTrue(intermediate > 0, "geen volgorde liet de telling van J1 vuren op een telling onder 2");
        assertTrue(dropped > 0, "geen volgorde liet de telling van vrije vondsten van J1 vuren voordat V1 en V2 hun"
                + " vrij verloren");
    }

    // ---------------- a fold in a default ----------------

    private static final String PREMIE = attributeGroup("premie van een jutter", "Jutter", "premie", List.of(
            standaard(comparison("greater", ERKEND_COUNT, number("1")), aggregation("sum", reading("bergloon", "Vondst"),
                    "Vondst", sourceRole("aanmelding", "vondst"), "null", "null", WHOLE,
                    "de som van het bergloon van alle vondsten van de Jutter")),
            standaard(comparison("greater", COUNT, number("0")), number("5"))));

    static final List<String> DEFAULT_RULES = List.of(ERKENNING);

    static Path defaultdocument(List<String> rules) throws Exception {
        return TmsDocuments.document(MODEL, rules, List.of(), List.of(BERGLOON, PREMIE));
    }

    /** J1 as above: two erkende vondsten, so the higher default decides the sum, 50. J3 reports V4 (gemeld, taxatie 7):
     *  one erkende vondst, so the lower default decides 5. J2 reports nothing: no default holds, premie leeg. */
    static JsonNode defaultCase() throws Exception {
        List<String> instanties = new ArrayList<>(List.of(
                instanceWithRoles("J1", "Jutter", Map.of(), List.of(), Map.of("vondsten", List.of("V1", "V2", "V3"))),
                instanceWithRoles("J2", "Jutter", Map.of(), List.of(), Map.of()),
                instanceWithRoles("J3", "Jutter", Map.of(), List.of(), Map.of("vondsten", List.of("V4")))));
        instanties.addAll(vondsten());
        instanties.add(instanceWithRoles("V4", "Vondst", Map.of("taxatie", "7"), List.of("gemeld"), Map.of()));
        return TmsDocuments.caseDocument(instanties, List.of(waarde("J1", "premie", "50"), waarde("J2", "premie", null),
                waarde("J3", "premie", "5"), consistent()));
    }

    @Test void a_fold_in_justification_and_consequence_of_a_default() throws Exception {
        assertEquals("gespeeld", TmsFaultWitnessTest.play(defaultdocument(DEFAULT_RULES), defaultCase(),
                DroolsBackend.Branch.EXEC_MODEL));
    }

    /** The generated rules in a seeded sample of orders; at least one lets the lower default decide 5 for J1 before
     *  the count of its erkende vondsten reaches 2, and the higher default take over. */
    @Test void sample_of_the_default_with_folds() throws Exception {
        DroolsBackend.Build b = DroolsBackend.build(defaultdocument(DEFAULT_RULES), DroolsBackend.Branch.EXEC_MODEL);
        int adopted = 0;
        for (List<Rule> order : TmsDocuments.sample(b.execModelTranspiler().model().getRules(), 20261008L, 100)) {
            List<String> names = order.stream().map(Rule::getName).toList();
            Object j1 = TmsDocuments.beanObject(b.beanLoader(), "Jutter", Map.of(), List.of());
            Object j2 = TmsDocuments.beanObject(b.beanLoader(), "Jutter", Map.of(), List.of());
            Object j3 = TmsDocuments.beanObject(b.beanLoader(), "Jutter", Map.of(), List.of());
            Object v1 = TmsDocuments.beanObject(b.beanLoader(), "Vondst", Map.of("taxatie", "30"), List.of("gemeld"));
            Object v2 = TmsDocuments.beanObject(b.beanLoader(), "Vondst", Map.of("taxatie", "20"), List.of("gemeld"));
            Object v3 = TmsDocuments.beanObject(b.beanLoader(), "Vondst", Map.of("taxatie", "10"), List.of());
            Object v4 = TmsDocuments.beanObject(b.beanLoader(), "Vondst", Map.of("taxatie", "7"), List.of("gemeld"));
            for (Object v : List.of(v1, v2, v3)) setRole(v, "aanmelder", j1);
            setRole(v4, "aanmelder", j3);
            List<String> fired = new ArrayList<>();
            TmsDocuments.vuur(b, order, List.of(j1, j2, j3, v1, v2, v3, v4), sessie ->
                    sessie.addEventListener(new DefaultAgendaEventListener() {
                        @Override public void afterMatchFired(AfterMatchFiredEvent e) {
                            if (e.getMatch().getObjects().contains(j1)) fired.add(e.getMatch().getRule().getName());
                        }
                    }), sessie -> { });
            assertNumber("50", TmsDocuments.lees(j1, "premie"), "premie van J1 in volgorde " + names);
            assertNull(TmsDocuments.lees(j2, "premie"), () -> "premie van J2 in volgorde " + names);
            assertNumber("5", TmsDocuments.lees(j3, "premie"), "premie van J3 in volgorde " + names);
            if (fired.contains("defaults A:Jutter.premie: default 1")) adopted++;
        }
        assertTrue(adopted > 0, "geen volgorde liet de lagere default voor J1 beslissen voordat de telling 2 werd");
    }

    // ---------------- a division by a count ----------------

    private static final String AVERAGE = computingRule("gemiddeld bergloon", "BEREKENING", "Jutter", "gemiddeld bergloon",
            binary("dividedBy", SUM, ERKEND_COUNT), "null");

    static final List<String> DIVISION_RULES = List.of(ERKENNING, AVERAGE);

    static Path divisionDocument(List<String> rules) throws Exception {
        return TmsDocuments.document(MODEL, rules, List.of(), List.of(BERGLOON));
    }

    /** J1 as above: 50 divided by 2, where the count passes 0 on the way. */
    static JsonNode temporaryDivisionCase() throws Exception {
        List<String> instanties = new ArrayList<>(List.of(
                instanceWithRoles("J1", "Jutter", Map.of(), List.of(), Map.of("vondsten", List.of("V1", "V2", "V3")))));
        instanties.addAll(vondsten());
        return TmsDocuments.caseDocument(instanties, List.of(waarde("J1", "gemiddeld bergloon", "25"), consistent()));
    }

    /** J2 reports nothing: the sum is its fallback 0 and the count 0, at the fixpoint as well. */
    static JsonNode persistentDivisionCase() throws Exception {
        return TmsDocuments.caseDocument(List.of(instanceWithRoles("J2", "Jutter", Map.of(), List.of(), Map.of())),
                List.of(consistent()));
    }

    @Test void a_division_by_a_count_that_is_intermediate_zero() throws Exception {
        List<String> outcomes = new ArrayList<>(), verwacht = new ArrayList<>();
        for (List<String> order : TmsDocuments.permutations(DIVISION_RULES)) {
            List<String> names = TmsDocuments.ruleNames(order);
            outcomes.add(names + " " + TmsFaultWitnessTest.play(divisionDocument(order), temporaryDivisionCase(),
                    DroolsBackend.Branch.EXEC_MODEL));
            verwacht.add(names + " gespeeld");
            outcomes.add(names + " " + TmsFaultWitnessTest.play(divisionDocument(order), persistentDivisionCase(),
                    DroolsBackend.Branch.EXEC_MODEL));
            verwacht.add(names + " DelingDoorNul: deling door nul in de regel \"gemiddeld bergloon\": 0 gedeeld door 0 —"
                    + " een numerieke nul als deler is een fout, geen leeg (RUNTIME-SEMANTICS ch. 1.4, punt 4)");
        }
        assertEquals(verwacht, outcomes);
        assertEquals(4, outcomes.size());
    }

    /** The generated rules in a seeded sample of orders: the fire ends at its fixpoint without throwing, and at least
     *  one order divides by the count while it is still 0, a fault fact that is gone at the fixpoint. */
    @Test void sample_of_the_division_by_a_count() throws Exception {
        DroolsBackend.Build b = DroolsBackend.build(divisionDocument(DIVISION_RULES), DroolsBackend.Branch.EXEC_MODEL);
        int intermediate = 0;
        for (List<Rule> order : TmsDocuments.sample(b.execModelTranspiler().model().getRules(), 20261008L, 100)) {
            List<String> names = order.stream().map(Rule::getName).toList();
            Object j1 = TmsDocuments.beanObject(b.beanLoader(), "Jutter", Map.of(), List.of());
            Object v1 = TmsDocuments.beanObject(b.beanLoader(), "Vondst", Map.of("taxatie", "30"), List.of("gemeld"));
            Object v2 = TmsDocuments.beanObject(b.beanLoader(), "Vondst", Map.of("taxatie", "20"), List.of("gemeld"));
            Object v3 = TmsDocuments.beanObject(b.beanLoader(), "Vondst", Map.of("taxatie", "10"), List.of());
            for (Object v : List.of(v1, v2, v3)) setRole(v, "aanmelder", j1);
            TmsFaultWitnessTest.FaultFacts sessionFacts = new TmsFaultWitnessTest.FaultFacts();
            assertNull(TmsFaultWitnessTest.vuur(b, order, List.of(j1, v1, v2, v3), new DefaultAgendaEventListener() {},
                    sessionFacts), () -> "volgorde " + names);
            assertEquals(sessionFacts.inserted, sessionFacts.withdrawn, () -> "foutfeiten in volgorde " + names);
            assertNumber("25", TmsDocuments.lees(j1, "gemiddeld bergloon"), "gemiddeld bergloon in volgorde " + names);
            if (sessionFacts.inserted > 0) intermediate++;
        }
        assertTrue(intermediate > 0, "geen volgorde deelde door de telling terwijl die nog 0 was");
    }

    // ---------------- a fault in a fold's filter ----------------

    static final String FILTERMODEL = TmsDocuments.model(List.of(
            TmsDocuments.objecttype("Jutter", List.of(attribuut("zware vondsten", WHOLE)), List.of()),
            TmsDocuments.objecttype("Vondst", List.of(attribuut("gewicht", WHOLE), attribuut("deelnemers", WHOLE)),
                    List.of())),
            List.of(factTypeOneToMany("aanmelding", "aanmelder", "Jutter", "vondst", "Vondst", "vondsten")));

    static final List<String> FILTER_RULES = List.of(computingRule("zware vondsten", "BEREKENING", "Jutter",
            "zware vondsten", aggregation("count", VONDSTEN, "Vondst", sourceRole("aanmelding", "vondst"),
                    comparison("greater", binary("dividedBy", reading("gewicht", "Vondst"), reading("deelnemers", "Vondst")),
                            number("2")), "null", COUNTING, "het aantal vondsten van de Jutter die aan de volgende voorwaarde"
                            + " voldoen: zijn gewicht gedeeld door zijn deelnemers is groter dan 2"), "null"));

    /** J1 reports V1 (gewicht 10, deelnemers 2: 5, counted) and V2 (gewicht 9, deelnemers 0): the filter divides 9 by
     *  0 for V2, and the rule, which has no condition, reaches the fold: a fault at the fixpoint, on J1 and named after
     *  the rule. */
    static JsonNode filterCase() throws Exception {
        return TmsDocuments.caseDocument(List.of(
                instanceWithRoles("J1", "Jutter", Map.of(), List.of(), Map.of("vondsten", List.of("V1", "V2"))),
                instanceWithRoles("V1", "Vondst", Map.of("gewicht", "10", "deelnemers", "2"), List.of(), Map.of()),
                instanceWithRoles("V2", "Vondst", Map.of("gewicht", "9", "deelnemers", "0"), List.of(), Map.of())),
                List.of(consistent()));
    }

    static Path filterdocument(List<String> rules) throws Exception {
        return TmsDocuments.document(FILTERMODEL, rules);
    }

    /** The fault of a fold's filter is the fault of the rule that reads the fold, which the rule raises where its
     *  evaluation reaches the fold while the member's {@code FilterError} stands ({@code TmsFilterErrorWitnessTest}),
     *  reported at the fixpoint with the sentence the exec-model branch gives for the same filter, and the fire ends at
     *  its fixpoint in every order of the generated rules. */
    @Test void a_fault_in_the_filter_of_a_fold_stands_at_the_fixpoint() throws Exception {
        String sentence = "DelingDoorNul: deling door nul in de regel \"zware vondsten\": 9 gedeeld door 0 — een numerieke"
                + " nul als deler is een fout, geen leeg (RUNTIME-SEMANTICS ch. 1.4, punt 4)";
        assertEquals(sentence, TmsFaultWitnessTest.play(filterdocument(FILTER_RULES), filterCase(),
                DroolsBackend.Branch.EXEC_MODEL));
        DroolsBackend.Build b = DroolsBackend.build(filterdocument(FILTER_RULES), DroolsBackend.Branch.EXEC_MODEL);
        int orders = 0;
        for (List<Rule> order : TmsDocuments.permutations(b.execModelTranspiler().model().getRules())) {
            List<String> names = order.stream().map(Rule::getName).toList();
            Object j1 = TmsDocuments.beanObject(b.beanLoader(), "Jutter", Map.of(), List.of());
            Object v1 = TmsDocuments.beanObject(b.beanLoader(), "Vondst", Map.of("gewicht", "10", "deelnemers", "2"), List.of());
            Object v2 = TmsDocuments.beanObject(b.beanLoader(), "Vondst", Map.of("gewicht", "9", "deelnemers", "0"), List.of());
            setRole(v1, "aanmelder", j1);
            setRole(v2, "aanmelder", j1);
            // the places of the list the case would write, which a fold whose filter can fault reads
            List<Object> invoer = List.of(j1, v1, v2, new ListPlace(v1, "aanmelder", 0), new ListPlace(v2, "aanmelder", 1));
            assertEquals(sentence, TmsFaultWitnessTest.vuur(b, order, invoer, new DefaultAgendaEventListener() {},
                    new TmsFaultWitnessTest.FaultFacts()), () -> "volgorde " + names);
            orders++;
        }
        // four generated rules: the reading rule, the contribution rule, the filter fault rule and, since the filter
        // divides, the filter deposit rule (fixround 1 of tms-expressievormen-en-rekendatum)
        assertEquals(24, orders);
    }

    // ---------------- a listed fold ----------------

    private static final String ONDERGRENS = computingRule("ondergrens", "BEREKENING", "Vondst", "ondergrens",
            listedFold("first", List.of(reading("taxatie", "Vondst"), reading("bergloon", "Vondst")), WHOLE,
                    "de eerste van zijn taxatie en zijn bergloon"), "null");

    static final List<String> LISTED_RULES = List.of(ERKENNING, ONDERGRENS);

    /** "De eerste van zijn taxatie en zijn bergloon", the least of the two (RUNTIME-SEMANTICS ch. 1.2e). V1 and V2: 5 on
     *  the way where the lower default decides first, then the taxatie; V3 has no bergloon, so the leeg operand is
     *  skipped and the fold is its taxatie. The ASP runner translates no listed minimum ("expression kind not
     *  translated", measured 2026-10-08), so the witness takes the eerste, which both runners carry. */
    static JsonNode listedCase() throws Exception {
        return TmsDocuments.caseDocument(vondsten(), List.of(waarde("V1", "ondergrens", "30"), waarde("V2", "ondergrens", "20"),
                waarde("V3", "ondergrens", "10"), consistent()));
    }

    @Test void a_listed_fold_over_a_later_derived_operand() throws Exception {
        List<String> outcomes = new ArrayList<>(), verwacht = new ArrayList<>();
        for (List<String> order : TmsDocuments.permutations(LISTED_RULES)) {
            List<String> names = TmsDocuments.ruleNames(order);
            outcomes.add(names + " " + TmsFaultWitnessTest.play(document(order), listedCase(),
                    DroolsBackend.Branch.EXEC_MODEL));
            verwacht.add(names + " gespeeld");
        }
        assertEquals(verwacht, outcomes);
    }

    // ---------------- a two-step range ----------------

    static final String PATH_MODEL = TmsDocuments.model(List.of(
            TmsDocuments.objecttype("Jutter", List.of(attribuut("vangstsom", WHOLE)), List.of()),
            TmsDocuments.objecttype("Boot", List.of(), List.of()),
            TmsDocuments.objecttype("Bergplaats", List.of(attribuut("ruw gewicht", WHOLE),
                    attribuut("vangstgewicht", WHOLE)), List.of())),
            List.of(factTypeOneToMany("bemanning", "schipper", "Jutter", "gevoerde boot", "Boot", "gevoerde boten"),
                    factTypeOneToMany("lossing", "losplaats", "Bergplaats", "lossende boot", "Boot", "lossende boten")));

    static final List<String> PATH_RULES = List.of(
            computingRule("vangst", "BEREKENING", "Bergplaats", "vangstgewicht",
                    binary("plus", reading("ruw gewicht", "Bergplaats"), number("1")), "null"),
            computingRule("vangstsom", "BEREKENING", "Jutter", "vangstsom", aggregation("sum",
                    reading("vangstgewicht", "Bergplaats"), "Bergplaats", sourcePath(List.of(
                            roleBinding("bemanning", "gevoerde boot"), roleBinding("lossing", "losplaats"))),
                    "null", number("0"), WHOLE,
                    "de som van het vangstgewicht van alle losplaatsen van alle gevoerde boten van de Jutter"), "null"));

    /** J1 sails B1, B2 and B3; B1 and B2 unload at P1 (ruw gewicht 2, vangstgewicht 3), B3 at P2 (4, so 5). P1 is
     *  reached along two boats and contributes once: 3 + 5 = 8, not 11. J2 sails nothing: the fallback, 0. */
    static JsonNode pathCase() throws Exception {
        return TmsDocuments.caseDocument(List.of(
                instanceWithRoles("J1", "Jutter", Map.of(), List.of(), Map.of("gevoerde boot", List.of("B1", "B2", "B3"))),
                instanceWithRoles("J2", "Jutter", Map.of(), List.of(), Map.of()),
                instanceWithRoles("B1", "Boot", Map.of(), List.of(), Map.of("losplaats", List.of("P1"))),
                instanceWithRoles("B2", "Boot", Map.of(), List.of(), Map.of("losplaats", List.of("P1"))),
                instanceWithRoles("B3", "Boot", Map.of(), List.of(), Map.of("losplaats", List.of("P2"))),
                instanceWithRoles("P1", "Bergplaats", Map.of("ruw gewicht", "2"), List.of(), Map.of()),
                instanceWithRoles("P2", "Bergplaats", Map.of("ruw gewicht", "4"), List.of(), Map.of())),
                List.of(waarde("J1", "vangstsom", "8"), waarde("J2", "vangstsom", "0"), consistent()));
    }

    static Path pathDocument(List<String> rules) throws Exception {
        return TmsDocuments.document(PATH_MODEL, rules);
    }

    @Test void a_member_reached_via_two_routes_contributes_once() throws Exception {
        List<String> outcomes = new ArrayList<>(), verwacht = new ArrayList<>();
        for (List<String> order : TmsDocuments.permutations(PATH_RULES)) {
            List<String> names = TmsDocuments.ruleNames(order);
            outcomes.add(names + " " + TmsFaultWitnessTest.play(pathDocument(order), pathCase(),
                    DroolsBackend.Branch.EXEC_MODEL));
            verwacht.add(names + " gespeeld");
        }
        assertEquals(verwacht, outcomes);
    }

    // ---------------- helpers ----------------

    /** The numbers a firing bound, by declaration: the results of its folds. */
    private static Object boundNumber(AfterMatchFiredEvent e) {
        Map<String, String> out = new java.util.TreeMap<>();
        for (String d : e.getMatch().getDeclarationIds())
            if (e.getMatch().getDeclarationValue(d) instanceof Number n) out.put(d, n.toString());
        return out.toString();
    }

    /** The least number a firing bound, or null where it bound none. */
    private static Long smallestNumber(AfterMatchFiredEvent e) {
        Long out = null;
        for (String d : e.getMatch().getDeclarationIds())
            if (e.getMatch().getDeclarationValue(d) instanceof Number n && (out == null || n.longValue() < out))
                out = n.longValue();
        return out;
    }

    /** Sets the to-one reference field {@code rol} of {@code beanObject} to {@code doel}, as the case reader does. */
    private static void setRole(Object beanObject, String rol, Object doel) throws Exception {
        for (java.lang.reflect.Method m : beanObject.getClass().getMethods())
            if (m.getName().equals(dev.uchar.regelspraak.transpiler.kern.Namen.setter(rol)) && m.getParameterCount() == 1) {
                m.invoke(beanObject, doel);
                return;
            }
        throw new AssertionError("geen setter voor " + rol + " op " + beanObject.getClass());
    }

    private static void assertNumber(String verwacht, Object werkelijk, String description) {
        assertTrue(werkelijk instanceof BigDecimal w && new BigDecimal(verwacht).compareTo(w) == 0,
                () -> description + ": verwacht " + verwacht + ", werkelijk " + werkelijk);
    }

}
