package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;

import org.drools.model.Rule;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static dev.uchar.regelspraak.cases.TmsDocuments.DATE;
import static dev.uchar.regelspraak.cases.TmsDocuments.aggregation;
import static dev.uchar.regelspraak.cases.TmsDocuments.attribuut;
import static dev.uchar.regelspraak.cases.TmsDocuments.binary;
import static dev.uchar.regelspraak.cases.TmsDocuments.sourceRole;
import static dev.uchar.regelspraak.cases.TmsDocuments.builtin;
import static dev.uchar.regelspraak.cases.TmsDocuments.check;
import static dev.uchar.regelspraak.cases.TmsDocuments.consistent;
import static dev.uchar.regelspraak.cases.TmsDocuments.contextValue;
import static dev.uchar.regelspraak.cases.TmsDocuments.dateValue;
import static dev.uchar.regelspraak.cases.TmsDocuments.unit;
import static dev.uchar.regelspraak.cases.TmsDocuments.factTypeOneToMany;
import static dev.uchar.regelspraak.cases.TmsDocuments.validity;
import static dev.uchar.regelspraak.cases.TmsDocuments.numberType;
import static dev.uchar.regelspraak.cases.TmsDocuments.numberValue;
import static dev.uchar.regelspraak.cases.TmsDocuments.inconsistent;
import static dev.uchar.regelspraak.cases.TmsDocuments.instanceWith;
import static dev.uchar.regelspraak.cases.TmsDocuments.instanceWithRoles;
import static dev.uchar.regelspraak.cases.TmsDocuments.characteristicRule;
import static dev.uchar.regelspraak.cases.TmsDocuments.reading;
import static dev.uchar.regelspraak.cases.TmsDocuments.literal;
import static dev.uchar.regelspraak.cases.TmsDocuments.computingRule;
import static dev.uchar.regelspraak.cases.TmsDocuments.roleBinding;
import static dev.uchar.regelspraak.cases.TmsDocuments.compound;
import static dev.uchar.regelspraak.cases.TmsDocuments.standaard;
import static dev.uchar.regelspraak.cases.TmsDocuments.comparison;
import static dev.uchar.regelspraak.cases.TmsDocuments.window;
import static dev.uchar.regelspraak.cases.TmsDocuments.verwacht;
import static dev.uchar.regelspraak.cases.TmsDocuments.waarde;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The evaluation date on the exec-model branch (RUNTIME-SEMANTICS ch. 8): a version with a bounded validity
 * gates on it (ch. 8.2) — a characteristic attribution, an initialisation, a consistency, an object creation, a fact
 * creation and a prioritized-defaults group — on both sides of the date and on the bounds themselves, both inclusive
 * (IR-SPEC ch. 3.10); in a session without the date such a form faults (ch. 8.1), in the reference interpreter's
 * sentence. The context values evaluationYear and evaluationDate read the date, and fault without it (ch. 8.2). A
 * {@code TimeWindow} on a scalar target tests the date against its window (ch. 3.10): {@code from} inclusive, {@code to}
 * exclusive, a bound that is the subject's own attribute read per subject, an unset date and a leeg bound never
 * matching, without a fault; under {@code none} or a counting quantifier it is refused (WF-112). Every form reads a
 * value another rule derives later, in every order of the document's rules, and the defaults group and the creations
 * also in a sample of orders of the generated rules.
 *
 * <p><b>Measured</b> (2026-10-09, the reference interpreter's runner and the ASP runner on every order of every
 * document): the reference interpreter passes every session with a date and faults every time-free one in this
 * witness's sentences, except the defaults group without a Jutter, which it passes: it asks the window of a rule, a
 * consistency or a creation before it takes a subject, and that of a defaults group per object of the stack's type.
 * The ASP runner gives the same outcomes where it translates, but faults a validity-bounded form in a time-free session
 * at translation, also the defaults group without a Jutter, and declines a consistency with a validity window
 * ("consistency-check validity windows are not translated").
 *
 * <p><b>Red before the change</b> (measured 2026-10-09 on a copy of the branch at its claim): every row was refused on
 * its form, "een versie met een begrensde geldigheid (Validity met from of to): de truth-maintenance-tak draagt deze
 * vorm nog niet", "een defaultsgroep met een gedateerde geldigheid ('premie van een jutter')", "een contextwaarde
 * (Rekenjaar) in een waarde ('het evaluationYear'): buiten de proef van de truth-maintenance-tak" and "een TimeWindow in
 * de voorwaarde ('van … tot …')"; the row of the window under GEEN expected the WF-112 sentence and got the
 * TimeWindow's.
 */
class TmsEvaluationDateWitnessTest {

    private static final String WHOLE = numberType(true, 0, null);
    private static final String VALID_IN_2026 = validity("2026-01-01", "2026-12-31"), FROM_2026 = validity("2026-01-01", null);

    /** "Een Jutter is erkend indien hij aangemeld is". */
    static final String ERKENNING = characteristicRule("erkenning", "Jutter", "erkend", check("aangemeld", false));

    private static final String WITHOUT_DATE = "a validity-bounded form needs an evaluation date; this session is"
            + " time-free";

    // ---------------- a characteristic attribution with a bounded validity ----------------

    private static final String CHARACTERISTIC_MODEL = TmsDocuments.model(List.of(TmsDocuments.objecttype("Jutter", List.of(),
            List.of("aangemeld", "erkend", "premiewaardig"))), List.of());

    /** "Een Jutter is premiewaardig indien hij erkend is", geldig van 2026-01-01 tot en met 2026-12-31. */
    static final String PREMIEWAARDIG = IrPath.regel("premiewaardig in 2026", "KENMERKTOEKENNING",
            "{\"node\":\"AttributeReference\",\"attribute\":\"premiewaardig\",\"subjectType\":\"Jutter\",\"source\":"
                    + "{\"snippet\":\"Een Jutter is premiewaardig\"}}",
            IrPath.literal("waar", "{\"node\":\"Boolean\"}", "waar"), check("erkend", false), VALID_IN_2026);

    static JsonNode characteristicCase(String asOfDate, boolean premiewaardig) throws Exception {
        return TmsDocuments.caseOn(asOfDate, List.of(instanceWith("J1", "Jutter", Map.of(), List.of("aangemeld")),
                instanceWith("J2", "Jutter", Map.of(), List.of())),
                List.of(verwacht("J1", "erkend", true), verwacht("J1", "premiewaardig", premiewaardig),
                        verwacht("J2", "premiewaardig", false), consistent()));
    }

    @Test void a_characteristic_attribution_on_both_sides_of_the_evaluation_date() throws Exception {
        List<String> outcomes = new ArrayList<>(), verwacht = new ArrayList<>();
        Map<String, Boolean> data = new java.util.LinkedHashMap<>();
        data.put("2025-12-31", false);
        data.put("2026-01-01", true);
        data.put("2026-06-15", true);
        data.put("2026-12-31", true);
        data.put("2027-01-01", false);
        for (List<String> order : TmsDocuments.permutations(List.of(ERKENNING, PREMIEWAARDIG))) {
            List<String> names = TmsDocuments.ruleNames(order);
            Path ir = TmsDocuments.document(CHARACTERISTIC_MODEL, order);
            for (Map.Entry<String, Boolean> d : data.entrySet()) {
                outcomes.add(names + " " + d.getKey() + " " + TmsFaultWitnessTest.play(ir,
                        characteristicCase(d.getKey(), d.getValue()), DroolsBackend.Branch.EXEC_MODEL));
                verwacht.add(names + " " + d.getKey() + " gespeeld");
            }
            outcomes.add(names + " tijdvrij " + TmsFaultWitnessTest.play(ir, characteristicCase(null, false),
                    DroolsBackend.Branch.EXEC_MODEL));
            verwacht.add(names + " tijdvrij Rekenfout: fout in de regel \"premiewaardig in 2026\": " + WITHOUT_DATE);
            // the reference interpreter asks the window of a rule before it takes a subject, so a time-free session
            // faults also where the rule has none
            outcomes.add(names + " tijdvrij zonder Jutter " + TmsFaultWitnessTest.play(ir,
                    TmsDocuments.caseOn(null, List.of(), List.of(consistent())), DroolsBackend.Branch.EXEC_MODEL));
            verwacht.add(names + " tijdvrij zonder Jutter Rekenfout: fout in de regel \"premiewaardig in 2026\": "
                    + WITHOUT_DATE);
        }
        assertEquals(verwacht, outcomes);
    }

    // ---------------- an initialisation and a consistency with a bounded validity ----------------

    private static final String PREMIEMODEL = TmsDocuments.model(List.of(TmsDocuments.objecttype("Jutter",
            List.of(attribuut("premie", WHOLE)), List.of("aangemeld", "erkend"))), List.of());

    /** "De premie van een Jutter moet geïnitialiseerd worden op 50 indien hij erkend is", geldig vanaf 2026. */
    static final String STARTPREMIE = IrPath.regel("startpremie vanaf 2026", "INITIALISATIE",
            "{\"node\":\"AttributeReference\",\"attribute\":\"premie\",\"subjectType\":\"Jutter\",\"source\":"
                    + "{\"snippet\":\"De premie van een Jutter\"}}", literal("50", WHOLE), check("erkend", false),
            FROM_2026);

    /** "De premie van een Jutter is kleiner dan 40", geldig vanaf 2026. */
    static final String PREMIECONTROLE = "{\"node\":\"Consistency\",\"name\":\"premiecontrole vanaf 2026\",\"versions\":"
            + "[{\"subject\":\"Jutter\",\"assertion\":" + comparison("less", reading("premie", "Jutter"),
            literal("40", WHOLE)) + ",\"validity\":" + FROM_2026 + ",\"source\":{\"snippet\":\"premiecontrole\"}}]}";

    static JsonNode premieCase(String asOfDate, List<String> verwachtingen) throws Exception {
        return TmsDocuments.caseOn(asOfDate, List.of(instanceWith("J1", "Jutter", Map.of(), List.of("aangemeld")),
                instanceWith("J2", "Jutter", Map.of("premie", numberValue("10")), List.of()),
                instanceWith("J3", "Jutter", Map.of("premie", numberValue("20")), List.of("aangemeld"))), verwachtingen);
    }

    @Test void an_initialisation_and_a_consistency_on_both_sides_of_the_evaluation_date() throws Exception {
        List<String> outcomes = new ArrayList<>(), verwacht = new ArrayList<>();
        for (List<String> order : TmsDocuments.permutations(List.of(ERKENNING, STARTPREMIE))) {
            List<String> names = TmsDocuments.ruleNames(order);
            Path ir = TmsDocuments.document(PREMIEMODEL, order, List.of(PREMIECONTROLE));
            outcomes.add(names + " binnen " + TmsFaultWitnessTest.play(ir, premieCase("2026-06-01", List.of(
                    waarde("J1", "premie", "50"), waarde("J2", "premie", "10"), waarde("J3", "premie", "20"),
                    inconsistent("premiecontrole vanaf 2026", "J1"))), DroolsBackend.Branch.EXEC_MODEL));
            outcomes.add(names + " ervoor " + TmsFaultWitnessTest.play(ir, premieCase("2025-12-31", List.of(
                    waarde("J1", "premie", null), waarde("J2", "premie", "10"), consistent())),
                    DroolsBackend.Branch.EXEC_MODEL));
            outcomes.add(names + " tijdvrij " + TmsFaultWitnessTest.play(ir, premieCase(null, List.of(consistent())),
                    DroolsBackend.Branch.EXEC_MODEL));
            verwacht.add(names + " binnen gespeeld");
            verwacht.add(names + " ervoor gespeeld");
            verwacht.add(names + " tijdvrij Evaluatiefouten: aan het fixpunt staan 2 evaluatiefouten, in de regels"
                    + " \"premiecontrole vanaf 2026\", \"startpremie vanaf 2026\": fout in de regel \"premiecontrole vanaf"
                    + " 2026\": " + WITHOUT_DATE + "; fout in de regel \"startpremie vanaf 2026\": " + WITHOUT_DATE);
        }
        assertEquals(verwacht, outcomes);
    }

    // ---------------- a prioritized-defaults group with a dated validity ----------------

    /** "De premie van een Jutter is 70 indien hij erkend is", in a defaults group geldig vanaf 2026. */
    static final String PREMIE_GROUP = "{\"node\":\"PrioritizedDefaults\",\"name\":\"premie van een jutter\",\"subject\":"
            + "\"Jutter\",\"attribute\":\"premie\",\"defaults\":[" + standaard(check("erkend", false), literal("70", WHOLE))
            + "],\"validity\":" + FROM_2026 + "}";

    @Test void a_defaults_group_on_both_sides_of_the_evaluation_date() throws Exception {
        Path ir = TmsDocuments.document(PREMIEMODEL, List.of(ERKENNING), List.of(), List.of(PREMIE_GROUP));
        JsonNode inside = TmsDocuments.caseOn("2026-01-01", List.of(
                instanceWith("J1", "Jutter", Map.of(), List.of("aangemeld")), instanceWith("J2", "Jutter", Map.of(), List.of())),
                List.of(waarde("J1", "premie", "70"), waarde("J2", "premie", null), consistent()));
        JsonNode preceding = TmsDocuments.caseOn("2025-12-31", List.of(
                instanceWith("J1", "Jutter", Map.of(), List.of("aangemeld"))),
                List.of(waarde("J1", "premie", null), consistent()));
        JsonNode timeFree = TmsDocuments.caseOn(null, List.of(
                instanceWith("J1", "Jutter", Map.of(), List.of("aangemeld"))), List.of(consistent()));
        // the reference interpreter asks the window of a group where it evaluates the stack on an object, so a
        // time-free session without a Jutter faults nothing
        JsonNode timeFreeWithoutJutter = TmsDocuments.caseOn(null, List.of(), List.of(consistent()));
        assertEquals(List.of("gespeeld", "gespeeld", "Rekenfout: fout in de regel \"defaults A:Jutter.premie\": "
                        + WITHOUT_DATE, "gespeeld"),
                List.of(TmsFaultWitnessTest.play(ir, inside, DroolsBackend.Branch.EXEC_MODEL),
                        TmsFaultWitnessTest.play(ir, preceding, DroolsBackend.Branch.EXEC_MODEL),
                        TmsFaultWitnessTest.play(ir, timeFree, DroolsBackend.Branch.EXEC_MODEL),
                        TmsFaultWitnessTest.play(ir, timeFreeWithoutJutter, DroolsBackend.Branch.EXEC_MODEL)));
        // the group and the erkenning it reads, in a sample of the orders of the generated rules, on both sides of
        // the date
        DroolsBackend.Build b = DroolsBackend.build(ir, DroolsBackend.Branch.EXEC_MODEL);
        int orders = 0;
        for (List<Rule> order : TmsDocuments.sample(b.execModelTranspiler().model().getRules(), 20261009L, 60)) {
            List<String> names = order.stream().map(Rule::getName).toList();
            for (String date : List.of("2026-01-01", "2025-12-31")) {
                Object j1 = TmsDocuments.beanObject(b.beanLoader(), "Jutter", Map.of(), List.of("aangemeld"));
                assertNull(TmsDocuments.fixpunt(b, order, List.of(j1),
                        sessie -> sessie.setGlobal("rekendatum", LocalDate.parse(date))), () -> "volgorde " + names);
                if (date.startsWith("2026"))
                    TmsFaultWitnessTest.assertNumber("70", TmsDocuments.lees(j1, "premie"), "premie op " + date + " in"
                            + " volgorde " + names);
                else assertNull(TmsDocuments.lees(j1, "premie"), () -> "premie op " + date + " in volgorde " + names);
            }
            orders++;
        }
        assertTrue(orders > 2);
    }

    // ---------------- an object creation and a fact creation with a bounded validity ----------------

    private static final String MELDINGEN = "{\"node\":\"AttributeReference\",\"attribute\":\"meldingen\",\"subjectType\":"
            + "\"Jutter\",\"source\":{\"snippet\":\"meldingen van de Jutter\"}}";

    private static final String REPORT_MODEL = TmsDocuments.model(List.of(
            TmsDocuments.objecttype("Jutter", List.of(attribuut("meldingaantal", WHOLE)), List.of("aangemeld", "erkend")),
            TmsDocuments.objecttype("Melding", List.of(attribuut("waarde", WHOLE)), List.of())),
            List.of(factTypeOneToMany("jutmelding", "melder", "Jutter", "melding", "Melding", "meldingen")));

    private static final String MESSAGE_COUNT = computingRule("meldingaantal", "BEREKENING", "Jutter", "meldingaantal",
            aggregation("count", MELDINGEN, "Melding", sourceRole("jutmelding", "melding"), "null", "null", WHOLE,
                    "het aantal meldingen van de Jutter"), "null");

    /** "Een Jutter heeft een melding met waarde 7 indien hij erkend is", geldig vanaf 2026. */
    static Path reportDocument() throws Exception {
        String report = TmsCreationWitnessTest.objectCreation("meld vanaf 2026", "Jutter", "Melding",
                List.of(TmsCreationWitnessTest.init("waarde", literal("7", WHOLE))), check("erkend", false),
                "jutmelding", "melding").replace("\"validity\":{\"node\":\"Validity\"}", "\"validity\":" + FROM_2026);
        return TmsCreationWitnessTest.document(REPORT_MODEL, List.of(ERKENNING, MESSAGE_COUNT), List.of(), List.of(report),
                List.of(), List.of());
    }

    @Test void an_object_creation_on_both_sides_of_the_evaluation_date() throws Exception {
        Path ir = reportDocument();
        JsonNode inside = TmsDocuments.caseOn("2026-01-01", List.of(
                instanceWith("J1", "Jutter", Map.of(), List.of("aangemeld")), instanceWith("J2", "Jutter", Map.of(), List.of())),
                List.of(waarde("J1", "meldingaantal", "1"), waarde("J2", "meldingaantal", "0"), consistent()));
        JsonNode preceding = TmsDocuments.caseOn("2025-12-31", List.of(
                instanceWith("J1", "Jutter", Map.of(), List.of("aangemeld"))),
                List.of(waarde("J1", "meldingaantal", "0"), consistent()));
        JsonNode timeFree = TmsDocuments.caseOn(null, List.of(
                instanceWith("J1", "Jutter", Map.of(), List.of("aangemeld"))), List.of(consistent()));
        assertEquals(List.of("gespeeld", "gespeeld", "Rekenfout: fout in de regel \"meld vanaf 2026\": " + WITHOUT_DATE),
                List.of(TmsFaultWitnessTest.play(ir, inside, DroolsBackend.Branch.EXEC_MODEL),
                        TmsFaultWitnessTest.play(ir, preceding, DroolsBackend.Branch.EXEC_MODEL),
                        TmsFaultWitnessTest.play(ir, timeFree, DroolsBackend.Branch.EXEC_MODEL)));
        DroolsBackend.Build b = DroolsBackend.build(ir, DroolsBackend.Branch.EXEC_MODEL);
        for (List<Rule> order : TmsDocuments.sample(b.execModelTranspiler().model().getRules(), 20261009L, 60)) {
            List<String> names = order.stream().map(Rule::getName).toList();
            for (String date : List.of("2026-01-01", "2025-12-31")) {
                Object j1 = TmsDocuments.beanObject(b.beanLoader(), "Jutter", Map.of(), List.of("aangemeld"));
                assertNull(TmsCreationWitnessTest.vuur(b, order, List.of(j1),
                        sessie -> sessie.setGlobal("rekendatum", LocalDate.parse(date)), sessie -> { }),
                        () -> "volgorde " + names);
                TmsFaultWitnessTest.assertNumber(date.startsWith("2026") ? "1" : "0", TmsDocuments.lees(j1,
                        "meldingaantal"), "meldingaantal op " + date + " in volgorde " + names);
            }
        }
    }

    /** "Een Jutter heeft een melding met jaar het Rekenjaar indien hij aangemeld is", beside "Het tweede jaar van een
     *  Melding moet berekend worden als het Rekenjaar", in a session without a date (fixround 1 of the card, finding 3):
     *  the initialisation faults at the making, and the reference interpreter then makes nothing, so it reports that
     *  one fault, "EVALUATION FAULT: the evaluationYear context value needs an evaluation date; the case carries asOfDate
     *  null", measured 2026-10-09 (the ASP runner faults at translation in the same sentence). With the date 2026-06-01
     *  both play. <b>Red</b> on a3842004, which made the Melding all the same, so the rule faulted on it too: "but was:
     *  &lt;Evaluatiefouten: aan het fixpunt staan 2 evaluatiefouten, in de regels "melding van een jutter", "tweede jaar
     *  van een melding": …&gt;". */
    @Test void a_creation_whose_initialisation_fails_makes_nothing() throws Exception {
        String model = TmsDocuments.model(List.of(
                TmsDocuments.objecttype("Jutter", List.of(), List.of("aangemeld")),
                TmsDocuments.objecttype("Melding", List.of(attribuut("jaar", WHOLE), attribuut("tweede jaar", WHOLE)),
                        List.of())),
                List.of(factTypeOneToMany("jutmelding", "melder", "Jutter", "melding", "Melding", "meldingen")));
        String report = TmsCreationWitnessTest.objectCreation("melding van een jutter", "Jutter", "Melding",
                List.of(TmsCreationWitnessTest.init("jaar", contextValue("evaluationYear", WHOLE))),
                check("aangemeld", false), "jutmelding", "melding");
        String tweede = computingRule("tweede jaar van een melding", "BEREKENING", "Melding", "tweede jaar",
                contextValue("evaluationYear", WHOLE), "null");
        Path ir = TmsCreationWitnessTest.document(model, List.of(tweede), List.of(), List.of(report), List.of(), List.of());
        assertEquals(List.of("Rekenfout: fout in de regel \"melding van een jutter\": the evaluationYear context value"
                        + " needs an evaluation date; the case carries asOfDate null", "gespeeld"),
                List.of(TmsFaultWitnessTest.play(ir, TmsDocuments.caseOn(null, List.of(
                                instanceWith("J1", "Jutter", Map.of(), List.of("aangemeld"))), List.of(consistent())),
                                DroolsBackend.Branch.EXEC_MODEL),
                        TmsFaultWitnessTest.play(ir, TmsDocuments.caseOn("2026-06-01", List.of(
                                instanceWith("J1", "Jutter", Map.of(), List.of("aangemeld"))), List.of(consistent())),
                                DroolsBackend.Branch.EXEC_MODEL)));
        // a sample of the orders of the generated rules: the one fault, and no Melding that the rule could read
        DroolsBackend.Build b = DroolsBackend.build(ir, DroolsBackend.Branch.EXEC_MODEL);
        int orders = 0;
        for (List<Rule> order : TmsDocuments.sample(b.execModelTranspiler().model().getRules(), 20261009L, 60)) {
            Object j1 = TmsDocuments.beanObject(b.beanLoader(), "Jutter", Map.of(), List.of("aangemeld"));
            String fout;
            try {
                fout = TmsCreationWitnessTest.vuur(b, order, List.of(j1), sessie -> { }, sessie -> { });
            } catch (RuntimeException e) {
                fout = e.getClass().getSimpleName() + ": " + e.getMessage();
            }
            assertEquals("Rekenfout: fout in de regel \"melding van een jutter\": the evaluationYear context value needs"
                    + " an evaluation date; the case carries asOfDate null", fout,
                    "volgorde " + order.stream().map(Rule::getName).toList());
            orders++;
        }
        assertTrue(orders > 2);
    }

    private static final String HAVENMODEL = TmsDocuments.model(List.of(
            TmsDocuments.objecttype("Kustwacht", List.of(), List.of()),
            TmsDocuments.objecttype("Haven", List.of(attribuut("jutteraantal", WHOLE)), List.of("vergund", "open")),
            TmsDocuments.objecttype("Jutter", List.of(), List.of())), List.of(
            factTypeOneToMany("bewaking", "kustwacht", "Kustwacht", "bewaakte haven", "Haven", "bewaakte havens"),
            factTypeOneToMany("werving", "werver", "Kustwacht", "geworven jutter", "Jutter", "geworven jutters"),
            factTypeOneToMany("havenlid", "haven", "Haven", "havenjutter", "Jutter", "havenjutters")));

    /** "Een Haven is open indien zij vergund is". */
    static final String OPENING = characteristicRule("opening", "Haven", "open", check("vergund", false));

    /** "Het jutteraantal van een Haven moet berekend worden als het aantal havenjutters van de Haven". */
    static final String JUTTERAANTAL = computingRule("jutteraantal", "BEREKENING", "Haven", "jutteraantal",
            aggregation("count", "{\"node\":\"AttributeReference\",\"attribute\":\"havenjutters\",\"subjectType\":"
                    + "\"Haven\",\"source\":{\"snippet\":\"havenjutters van de Haven\"}}", "Jutter",
                    sourceRole("havenlid", "havenjutter"), "null", "null", WHOLE, "het aantal havenjutters van de Haven"),
            "null");

    /** "Een havenjutter van een Haven is een geworven jutter van de kustwacht van de Haven, indien de Haven open is",
     *  geldig tot en met 2025, beside {@code rules} in their order. */
    static Path havendocument(List<String> rules) throws Exception {
        String werving = TmsCreationWitnessTest.factCreation("werving tot en met 2025", "havenlid", "haven", "havenjutter",
                "Haven", List.of(roleBinding("bewaking", "kustwacht"), roleBinding("werving", "geworven jutter")),
                check("open", false)).replace("\"validity\":{\"node\":\"Validity\"}",
                "\"validity\":" + validity(null, "2025-12-31"));
        return TmsCreationWitnessTest.document(HAVENMODEL, rules, List.of(), List.of(), List.of(werving), List.of());
    }

    static JsonNode havenCase(String asOfDate, String aantal) throws Exception {
        String k1 = instanceWithRoles("K1", "Kustwacht", Map.of(), List.of(),
                Map.of("bewaakte havens", List.of("H1"), "geworven jutters", List.of("J1", "J2")));
        String h1 = instanceWithRoles("H1", "Haven", Map.of(), List.of("vergund"), Map.of("havenjutters", List.of("J1")));
        return TmsDocuments.caseOn(asOfDate, List.of(k1, h1, instanceWithRoles("J1", "Jutter", Map.of(), List.of(), Map.of()),
                instanceWithRoles("J2", "Jutter", Map.of(), List.of(), Map.of())),
                aantal == null ? List.of(consistent()) : List.of(waarde("H1", "jutteraantal", aantal), consistent()));
    }

    @Test void a_fact_creation_on_both_sides_of_the_evaluation_date() throws Exception {
        for (List<String> order : TmsDocuments.permutations(List.of(OPENING, JUTTERAANTAL))) {
            Path ir = havendocument(order);
            List<String> names = TmsDocuments.ruleNames(order);
            assertEquals(List.of("gespeeld", "gespeeld", "gespeeld",
                            "Rekenfout: fout in de regel \"werving tot en met 2025\": " + WITHOUT_DATE),
                    List.of(TmsFaultWitnessTest.play(ir, havenCase("2025-06-01", "2"), DroolsBackend.Branch.EXEC_MODEL),
                            TmsFaultWitnessTest.play(ir, havenCase("2025-12-31", "2"), DroolsBackend.Branch.EXEC_MODEL),
                            TmsFaultWitnessTest.play(ir, havenCase("2026-01-01", "1"), DroolsBackend.Branch.EXEC_MODEL),
                            TmsFaultWitnessTest.play(ir, havenCase(null, null), DroolsBackend.Branch.EXEC_MODEL)),
                    () -> "volgorde " + names);
        }
        // a sample of the orders of the generated rules, on both sides of the date
        DroolsBackend.Build b = DroolsBackend.build(havendocument(List.of(OPENING, JUTTERAANTAL)),
                DroolsBackend.Branch.EXEC_MODEL);
        int orders = 0;
        for (List<Rule> order : TmsDocuments.sample(b.execModelTranspiler().model().getRules(), 20261009L, 60)) {
            List<String> names = order.stream().map(Rule::getName).toList();
            for (String date : List.of("2025-12-31", "2026-01-01")) {
                Object k1 = TmsDocuments.beanObject(b.beanLoader(), "Kustwacht", Map.of(), List.of());
                Object h1 = TmsDocuments.beanObject(b.beanLoader(), "Haven", Map.of(), List.of("vergund"));
                Object j1 = TmsDocuments.beanObject(b.beanLoader(), "Jutter", Map.of(), List.of());
                Object j2 = TmsDocuments.beanObject(b.beanLoader(), "Jutter", Map.of(), List.of());
                setRole(h1, "kustwacht", k1);
                setRole(j1, "werver", k1);
                setRole(j2, "werver", k1);
                setRole(j1, "haven", h1);
                assertNull(TmsCreationWitnessTest.vuur(b, order, List.of(k1, h1, j1, j2),
                        sessie -> sessie.setGlobal("rekendatum", LocalDate.parse(date)), sessie -> { }),
                        () -> "volgorde " + names);
                TmsFaultWitnessTest.assertNumber(date.startsWith("2025") ? "2" : "1", TmsDocuments.lees(h1,
                        "jutteraantal"), "jutteraantal op " + date + " in volgorde " + names);
            }
            orders++;
        }
        assertTrue(orders > 2);
    }

    private static void setRole(Object beanObject, String rol, Object doel) throws Exception {
        for (java.lang.reflect.Method m : beanObject.getClass().getMethods())
            if (m.getName().equals(dev.uchar.regelspraak.transpiler.kern.Namen.setter(rol)) && m.getParameterCount() == 1) {
                m.invoke(beanObject, doel);
                return;
            }
        throw new AssertionError("geen setter voor " + rol + " op " + beanObject.getClass());
    }

    // ---------------- the context values ----------------

    private static final String GUARD_MODEL = TmsDocuments.model(List.of(TmsDocuments.objecttype("Jutter", List.of(
            attribuut("aanmelddatum", DATE), attribuut("startdatum", DATE), attribuut("wachtjaren", WHOLE),
            attribuut("verstreken", numberType(true, 0, unit("dagen"))), attribuut("bonus", WHOLE),
            attribuut("bonus vanaf start", WHOLE)), List.of())), List.of());

    /** "De startdatum van een Jutter moet berekend worden als zijn aanmelddatum plus 1 maand". */
    static final String START = computingRule("startdatum van een jutter", "BEREKENING", "Jutter", "startdatum",
            binary("plus", reading("aanmelddatum", "Jutter"), literal("1", numberType(true, 0, unit("maand"))), DATE),
            "null");

    static final String WAITING_YEARS = computingRule("wachtjaren van een jutter", "BEREKENING", "Jutter", "wachtjaren",
            binary("minus", contextValue("evaluationYear", WHOLE), builtin("yearOf",
                    List.of(reading("startdatum", "Jutter")), WHOLE, "het jaar uit zijn startdatum"), WHOLE), "null");

    static final String VERSTREKEN = computingRule("verstreken dagen van een jutter", "BEREKENING", "Jutter", "verstreken",
            builtin("duration", List.of(reading("startdatum", "Jutter"), contextValue("evaluationDate", DATE)),
                    numberType(true, 0, unit("dagen")), "de tijdsduur van zijn startdatum tot de Rekendatum in hele"
                            + " dagen"), "null");

    static JsonNode guardCase(String asOfDate, List<String> verwachtingen) throws Exception {
        return TmsDocuments.caseOn(asOfDate, List.of(
                instanceWith("J1", "Jutter", Map.of("aanmelddatum", dateValue("2024-11-15"),
                        "startdatum", dateValue("2000-01-01")), List.of()),
                instanceWith("J2", "Jutter", Map.of(), List.of())), verwachtingen);
    }

    @Test void the_evaluation_year_and_the_evaluation_date_beside_a_later_derived_start_date() throws Exception {
        List<String> outcomes = new ArrayList<>(), verwacht = new ArrayList<>();
        for (List<String> order : TmsDocuments.permutations(List.of(START, WAITING_YEARS))) {
            List<String> names = TmsDocuments.ruleNames(order);
            Path ir = TmsDocuments.document(GUARD_MODEL, order);
            outcomes.add(names + " " + TmsFaultWitnessTest.play(ir, guardCase("2026-06-01", List.of(
                    waarde("J1", "wachtjaren", "2"), waarde("J2", "wachtjaren", "2026"), consistent())),
                    DroolsBackend.Branch.EXEC_MODEL));
            outcomes.add(names + " " + TmsFaultWitnessTest.play(ir, guardCase(null, List.of(consistent())),
                    DroolsBackend.Branch.EXEC_MODEL));
            verwacht.add(names + " gespeeld");
            verwacht.add(names + " Evaluatiefouten: aan het fixpunt staan 2 evaluatiefouten, in de regel \"wachtjaren van"
                    + " een jutter\" (2×): fout in de regel \"wachtjaren van een jutter\": the evaluationYear context value"
                    + " needs an evaluation date; the case carries asOfDate null; fout in de regel \"wachtjaren van een"
                    + " jutter\": the evaluationYear context value needs an evaluation date; the case carries asOfDate null");
        }
        for (List<String> order : TmsDocuments.permutations(List.of(START, VERSTREKEN))) {
            List<String> names = TmsDocuments.ruleNames(order);
            Path ir = TmsDocuments.document(GUARD_MODEL, order);
            outcomes.add(names + " " + TmsFaultWitnessTest.play(ir, guardCase("2026-06-01", List.of(
                    waarde("J1", "verstreken", "533"), waarde("J2", "verstreken", null), consistent())),
                    DroolsBackend.Branch.EXEC_MODEL));
            outcomes.add(names + " " + TmsFaultWitnessTest.play(ir, guardCase(null, List.of(consistent())),
                    DroolsBackend.Branch.EXEC_MODEL));
            verwacht.add(names + " gespeeld");
            verwacht.add(names + " Evaluatiefouten: aan het fixpunt staan 2 evaluatiefouten, in de regel \"verstreken dagen"
                    + " van een jutter\" (2×): fout in de regel \"verstreken dagen van een jutter\": the evaluationDate"
                    + " context value needs an evaluation date; the case carries asOfDate null; fout in de regel"
                    + " \"verstreken dagen van een jutter\": the evaluationDate context value needs an evaluation date; the"
                    + " case carries asOfDate null");
        }
        assertEquals(verwacht, outcomes);
    }

    // ---------------- the time window ----------------

    /** "De bonus van een Jutter moet berekend worden als 100, indien het de periode van 2026-01-01 tot 2027-01-01 is". */
    static final String BONUS = computingRule("bonus van een jutter", "BEREKENING", "Jutter", "bonus", literal("100", WHOLE),
            window(literal("2026-01-01", DATE), literal("2027-01-01", DATE), false));

    /** "De bonus vanaf start van een Jutter moet berekend worden als 100, indien het de periode vanaf zijn startdatum
     *  is": a bound that another rule derives. */
    static final String BONUS_FROM_START = computingRule("bonus vanaf start", "BEREKENING", "Jutter", "bonus vanaf start",
            literal("100", WHOLE), window(reading("startdatum", "Jutter"), "null", false));

    static JsonNode windowCase(String asOfDate, String bonus, String j1, String j2) throws Exception {
        return TmsDocuments.caseOn(asOfDate, List.of(
                instanceWith("J1", "Jutter", Map.of("aanmelddatum", dateValue("2024-11-15"),
                        "startdatum", dateValue("2030-01-01")), List.of()),
                instanceWith("J2", "Jutter", Map.of("aanmelddatum", dateValue("2026-06-15")), List.of()),
                instanceWith("J3", "Jutter", Map.of(), List.of())),
                List.of(waarde("J1", "bonus", bonus), waarde("J1", "bonus vanaf start", j1),
                        waarde("J2", "bonus vanaf start", j2), waarde("J3", "bonus vanaf start", null), consistent()));
    }

    @Test void a_time_window_over_the_evaluation_date_and_a_later_derived_bound() throws Exception {
        List<String> outcomes = new ArrayList<>(), verwacht = new ArrayList<>();
        for (List<String> order : TmsDocuments.permutations(List.of(START, BONUS, BONUS_FROM_START))) {
            List<String> names = TmsDocuments.ruleNames(order);
            Path ir = TmsDocuments.document(GUARD_MODEL, order);
            for (JsonNode caseDocument : List.of(windowCase("2026-06-01", "100", "100", null),
                    windowCase("2026-01-01", "100", "100", null), windowCase("2026-12-31", "100", "100", "100"),
                    windowCase("2027-01-01", null, "100", "100"), windowCase("2025-12-31", null, "100", null),
                    windowCase(null, null, null, null)))
                outcomes.add(names + " " + caseDocument.get("asOfDate") + " " + TmsFaultWitnessTest.play(ir, caseDocument,
                        DroolsBackend.Branch.EXEC_MODEL));
            for (String d : List.of("\"2026-06-01\"", "\"2026-01-01\"", "\"2026-12-31\"", "\"2027-01-01\"",
                    "\"2025-12-31\"", "null"))
                verwacht.add(names + " " + d + " gespeeld");
        }
        assertEquals(verwacht, outcomes);
    }

    /** "… indien hij aan geen van de volgende voorwaarden voldoet: het is de periode van 2026-01-01 tot 2027-01-01". */
    @Test void a_time_window_under_none_is_refused() throws Exception {
        String regel = computingRule("bonus buiten 2026", "BEREKENING", "Jutter", "bonus", literal("100", WHOLE),
                compound("none", 0, List.of(window(literal("2026-01-01", DATE), literal("2027-01-01", DATE),
                        false))));
        CaseRunner.Excluded e = assertThrows(CaseRunner.Excluded.class, () -> DroolsBackend.build(
                TmsDocuments.document(GUARD_MODEL, List.of(regel)), DroolsBackend.Branch.EXEC_MODEL));
        assertEquals("transpiler SKIP: regel: bonus buiten 2026 — periodevenster in een GEEN-samenstelling (WF-112):"
                + " de kwantor telt of ontkent het vensterverdict, terwijl het venster de regel poort (RUNTIME-SEMANTICS"
                + " ch. 3.10), en voor die lezing is niets gespecificeerd: none 0", e.getMessage());
    }
}
