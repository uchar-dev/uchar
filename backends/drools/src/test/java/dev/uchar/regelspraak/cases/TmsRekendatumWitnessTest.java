package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;

import org.drools.model.Rule;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static dev.uchar.regelspraak.cases.TmsDocumenten.DATUM;
import static dev.uchar.regelspraak.cases.TmsDocumenten.aggregatie;
import static dev.uchar.regelspraak.cases.TmsDocumenten.attribuut;
import static dev.uchar.regelspraak.cases.TmsDocumenten.binair;
import static dev.uchar.regelspraak.cases.TmsDocumenten.bronrol;
import static dev.uchar.regelspraak.cases.TmsDocumenten.builtin;
import static dev.uchar.regelspraak.cases.TmsDocumenten.check;
import static dev.uchar.regelspraak.cases.TmsDocumenten.consistent;
import static dev.uchar.regelspraak.cases.TmsDocumenten.contextwaarde;
import static dev.uchar.regelspraak.cases.TmsDocumenten.datumwaarde;
import static dev.uchar.regelspraak.cases.TmsDocumenten.eenheid;
import static dev.uchar.regelspraak.cases.TmsDocumenten.feittypeEenNaarVeel;
import static dev.uchar.regelspraak.cases.TmsDocumenten.geldigheid;
import static dev.uchar.regelspraak.cases.TmsDocumenten.getaltype;
import static dev.uchar.regelspraak.cases.TmsDocumenten.getalwaarde;
import static dev.uchar.regelspraak.cases.TmsDocumenten.inconsistent;
import static dev.uchar.regelspraak.cases.TmsDocumenten.instantieMet;
import static dev.uchar.regelspraak.cases.TmsDocumenten.instantieMetRollen;
import static dev.uchar.regelspraak.cases.TmsDocumenten.kenmerkregel;
import static dev.uchar.regelspraak.cases.TmsDocumenten.lezing;
import static dev.uchar.regelspraak.cases.TmsDocumenten.literaal;
import static dev.uchar.regelspraak.cases.TmsDocumenten.rekenregel;
import static dev.uchar.regelspraak.cases.TmsDocumenten.rolbinding;
import static dev.uchar.regelspraak.cases.TmsDocumenten.samengesteld;
import static dev.uchar.regelspraak.cases.TmsDocumenten.standaard;
import static dev.uchar.regelspraak.cases.TmsDocumenten.vergelijking;
import static dev.uchar.regelspraak.cases.TmsDocumenten.venster;
import static dev.uchar.regelspraak.cases.TmsDocumenten.verwacht;
import static dev.uchar.regelspraak.cases.TmsDocumenten.waarde;
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
class TmsRekendatumWitnessTest {

    private static final String GEHEEL = getaltype(true, 0, null);
    private static final String GELDIG_IN_2026 = geldigheid("2026-01-01", "2026-12-31"), VANAF_2026 = geldigheid("2026-01-01", null);

    /** "Een Jutter is erkend indien hij aangemeld is". */
    static final String ERKENNING = kenmerkregel("erkenning", "Jutter", "erkend", check("aangemeld", false));

    private static final String ZONDER_DATUM = "a validity-bounded form needs an evaluation date; this session is"
            + " time-free";

    // ---------------- a characteristic attribution with a bounded validity ----------------

    private static final String KENMERKMODEL = TmsDocumenten.model(List.of(TmsDocumenten.objecttype("Jutter", List.of(),
            List.of("aangemeld", "erkend", "premiewaardig"))), List.of());

    /** "Een Jutter is premiewaardig indien hij erkend is", geldig van 2026-01-01 tot en met 2026-12-31. */
    static final String PREMIEWAARDIG = IrPad.regel("premiewaardig in 2026", "KENMERKTOEKENNING",
            "{\"node\":\"AttributeReference\",\"attribute\":\"premiewaardig\",\"subjectType\":\"Jutter\",\"source\":"
                    + "{\"snippet\":\"Een Jutter is premiewaardig\"}}",
            IrPad.literal("waar", "{\"node\":\"Boolean\"}", "waar"), check("erkend", false), GELDIG_IN_2026);

    static JsonNode kenmerkzaak(String asOfDate, boolean premiewaardig) throws Exception {
        return TmsDocumenten.zaakOp(asOfDate, List.of(instantieMet("J1", "Jutter", Map.of(), List.of("aangemeld")),
                instantieMet("J2", "Jutter", Map.of(), List.of())),
                List.of(verwacht("J1", "erkend", true), verwacht("J1", "premiewaardig", premiewaardig),
                        verwacht("J2", "premiewaardig", false), consistent()));
    }

    @Test void een_kenmerktoekenning_aan_beide_kanten_van_de_rekendatum() throws Exception {
        List<String> uitkomsten = new ArrayList<>(), verwacht = new ArrayList<>();
        Map<String, Boolean> data = new java.util.LinkedHashMap<>();
        data.put("2025-12-31", false);
        data.put("2026-01-01", true);
        data.put("2026-06-15", true);
        data.put("2026-12-31", true);
        data.put("2027-01-01", false);
        for (List<String> volgorde : TmsDocumenten.permutaties(List.of(ERKENNING, PREMIEWAARDIG))) {
            List<String> namen = TmsDocumenten.regelnamen(volgorde);
            Path ir = TmsDocumenten.document(KENMERKMODEL, volgorde);
            for (Map.Entry<String, Boolean> d : data.entrySet()) {
                uitkomsten.add(namen + " " + d.getKey() + " " + TmsFoutWitnessTest.speel(ir,
                        kenmerkzaak(d.getKey(), d.getValue()), DroolsBackend.Branch.EXEC_MODEL));
                verwacht.add(namen + " " + d.getKey() + " gespeeld");
            }
            uitkomsten.add(namen + " tijdvrij " + TmsFoutWitnessTest.speel(ir, kenmerkzaak(null, false),
                    DroolsBackend.Branch.EXEC_MODEL));
            verwacht.add(namen + " tijdvrij Rekenfout: fout in de regel \"premiewaardig in 2026\": " + ZONDER_DATUM);
            // the reference interpreter asks the window of a rule before it takes a subject, so a time-free session
            // faults also where the rule has none
            uitkomsten.add(namen + " tijdvrij zonder Jutter " + TmsFoutWitnessTest.speel(ir,
                    TmsDocumenten.zaakOp(null, List.of(), List.of(consistent())), DroolsBackend.Branch.EXEC_MODEL));
            verwacht.add(namen + " tijdvrij zonder Jutter Rekenfout: fout in de regel \"premiewaardig in 2026\": "
                    + ZONDER_DATUM);
        }
        assertEquals(verwacht, uitkomsten);
    }

    // ---------------- an initialisation and a consistency with a bounded validity ----------------

    private static final String PREMIEMODEL = TmsDocumenten.model(List.of(TmsDocumenten.objecttype("Jutter",
            List.of(attribuut("premie", GEHEEL)), List.of("aangemeld", "erkend"))), List.of());

    /** "De premie van een Jutter moet geïnitialiseerd worden op 50 indien hij erkend is", geldig vanaf 2026. */
    static final String STARTPREMIE = IrPad.regel("startpremie vanaf 2026", "INITIALISATIE",
            "{\"node\":\"AttributeReference\",\"attribute\":\"premie\",\"subjectType\":\"Jutter\",\"source\":"
                    + "{\"snippet\":\"De premie van een Jutter\"}}", literaal("50", GEHEEL), check("erkend", false),
            VANAF_2026);

    /** "De premie van een Jutter is kleiner dan 40", geldig vanaf 2026. */
    static final String PREMIECONTROLE = "{\"node\":\"Consistency\",\"name\":\"premiecontrole vanaf 2026\",\"versions\":"
            + "[{\"subject\":\"Jutter\",\"assertion\":" + vergelijking("less", lezing("premie", "Jutter"),
            literaal("40", GEHEEL)) + ",\"validity\":" + VANAF_2026 + ",\"source\":{\"snippet\":\"premiecontrole\"}}]}";

    static JsonNode premiezaak(String asOfDate, List<String> verwachtingen) throws Exception {
        return TmsDocumenten.zaakOp(asOfDate, List.of(instantieMet("J1", "Jutter", Map.of(), List.of("aangemeld")),
                instantieMet("J2", "Jutter", Map.of("premie", getalwaarde("10")), List.of()),
                instantieMet("J3", "Jutter", Map.of("premie", getalwaarde("20")), List.of("aangemeld"))), verwachtingen);
    }

    @Test void een_initialisatie_en_een_consistentie_aan_beide_kanten_van_de_rekendatum() throws Exception {
        List<String> uitkomsten = new ArrayList<>(), verwacht = new ArrayList<>();
        for (List<String> volgorde : TmsDocumenten.permutaties(List.of(ERKENNING, STARTPREMIE))) {
            List<String> namen = TmsDocumenten.regelnamen(volgorde);
            Path ir = TmsDocumenten.document(PREMIEMODEL, volgorde, List.of(PREMIECONTROLE));
            uitkomsten.add(namen + " binnen " + TmsFoutWitnessTest.speel(ir, premiezaak("2026-06-01", List.of(
                    waarde("J1", "premie", "50"), waarde("J2", "premie", "10"), waarde("J3", "premie", "20"),
                    inconsistent("premiecontrole vanaf 2026", "J1"))), DroolsBackend.Branch.EXEC_MODEL));
            uitkomsten.add(namen + " ervoor " + TmsFoutWitnessTest.speel(ir, premiezaak("2025-12-31", List.of(
                    waarde("J1", "premie", null), waarde("J2", "premie", "10"), consistent())),
                    DroolsBackend.Branch.EXEC_MODEL));
            uitkomsten.add(namen + " tijdvrij " + TmsFoutWitnessTest.speel(ir, premiezaak(null, List.of(consistent())),
                    DroolsBackend.Branch.EXEC_MODEL));
            verwacht.add(namen + " binnen gespeeld");
            verwacht.add(namen + " ervoor gespeeld");
            verwacht.add(namen + " tijdvrij Evaluatiefouten: aan het fixpunt staan 2 evaluatiefouten, in de regels"
                    + " \"premiecontrole vanaf 2026\", \"startpremie vanaf 2026\": fout in de regel \"premiecontrole vanaf"
                    + " 2026\": " + ZONDER_DATUM + "; fout in de regel \"startpremie vanaf 2026\": " + ZONDER_DATUM);
        }
        assertEquals(verwacht, uitkomsten);
    }

    // ---------------- a prioritized-defaults group with a dated validity ----------------

    /** "De premie van een Jutter is 70 indien hij erkend is", in a defaults group geldig vanaf 2026. */
    static final String PREMIEGROEP = "{\"node\":\"PrioritizedDefaults\",\"name\":\"premie van een jutter\",\"subject\":"
            + "\"Jutter\",\"attribute\":\"premie\",\"defaults\":[" + standaard(check("erkend", false), literaal("70", GEHEEL))
            + "],\"validity\":" + VANAF_2026 + "}";

    @Test void een_defaultsgroep_aan_beide_kanten_van_de_rekendatum() throws Exception {
        Path ir = TmsDocumenten.document(PREMIEMODEL, List.of(ERKENNING), List.of(), List.of(PREMIEGROEP));
        JsonNode binnen = TmsDocumenten.zaakOp("2026-01-01", List.of(
                instantieMet("J1", "Jutter", Map.of(), List.of("aangemeld")), instantieMet("J2", "Jutter", Map.of(), List.of())),
                List.of(waarde("J1", "premie", "70"), waarde("J2", "premie", null), consistent()));
        JsonNode ervoor = TmsDocumenten.zaakOp("2025-12-31", List.of(
                instantieMet("J1", "Jutter", Map.of(), List.of("aangemeld"))),
                List.of(waarde("J1", "premie", null), consistent()));
        JsonNode tijdvrij = TmsDocumenten.zaakOp(null, List.of(
                instantieMet("J1", "Jutter", Map.of(), List.of("aangemeld"))), List.of(consistent()));
        // the reference interpreter asks the window of a group where it evaluates the stack on an object, so a
        // time-free session without a Jutter faults nothing
        JsonNode tijdvrijZonderJutter = TmsDocumenten.zaakOp(null, List.of(), List.of(consistent()));
        assertEquals(List.of("gespeeld", "gespeeld", "Rekenfout: fout in de regel \"defaults A:Jutter.premie\": "
                        + ZONDER_DATUM, "gespeeld"),
                List.of(TmsFoutWitnessTest.speel(ir, binnen, DroolsBackend.Branch.EXEC_MODEL),
                        TmsFoutWitnessTest.speel(ir, ervoor, DroolsBackend.Branch.EXEC_MODEL),
                        TmsFoutWitnessTest.speel(ir, tijdvrij, DroolsBackend.Branch.EXEC_MODEL),
                        TmsFoutWitnessTest.speel(ir, tijdvrijZonderJutter, DroolsBackend.Branch.EXEC_MODEL)));
        // the group and the erkenning it reads, in a sample of the orders of the generated rules, on both sides of
        // the date
        DroolsBackend.Build b = DroolsBackend.build(ir, DroolsBackend.Branch.EXEC_MODEL);
        int volgordes = 0;
        for (List<Rule> volgorde : TmsDocumenten.steekproef(b.execModelTranspiler().model().getRules(), 20261009L, 60)) {
            List<String> namen = volgorde.stream().map(Rule::getName).toList();
            for (String datum : List.of("2026-01-01", "2025-12-31")) {
                Object j1 = TmsDocumenten.boon(b.beanLoader(), "Jutter", Map.of(), List.of("aangemeld"));
                assertNull(TmsDocumenten.fixpunt(b, volgorde, List.of(j1),
                        sessie -> sessie.setGlobal("rekendatum", LocalDate.parse(datum))), () -> "volgorde " + namen);
                if (datum.startsWith("2026"))
                    TmsFoutWitnessTest.assertGetal("70", TmsDocumenten.lees(j1, "premie"), "premie op " + datum + " in"
                            + " volgorde " + namen);
                else assertNull(TmsDocumenten.lees(j1, "premie"), () -> "premie op " + datum + " in volgorde " + namen);
            }
            volgordes++;
        }
        assertTrue(volgordes > 2);
    }

    // ---------------- an object creation and a fact creation with a bounded validity ----------------

    private static final String MELDINGEN = "{\"node\":\"AttributeReference\",\"attribute\":\"meldingen\",\"subjectType\":"
            + "\"Jutter\",\"source\":{\"snippet\":\"meldingen van de Jutter\"}}";

    private static final String MELDMODEL = TmsDocumenten.model(List.of(
            TmsDocumenten.objecttype("Jutter", List.of(attribuut("meldingaantal", GEHEEL)), List.of("aangemeld", "erkend")),
            TmsDocumenten.objecttype("Melding", List.of(attribuut("waarde", GEHEEL)), List.of())),
            List.of(feittypeEenNaarVeel("jutmelding", "melder", "Jutter", "melding", "Melding", "meldingen")));

    private static final String MELDINGAANTAL = rekenregel("meldingaantal", "BEREKENING", "Jutter", "meldingaantal",
            aggregatie("count", MELDINGEN, "Melding", bronrol("jutmelding", "melding"), "null", "null", GEHEEL,
                    "het aantal meldingen van de Jutter"), "null");

    /** "Een Jutter heeft een melding met waarde 7 indien hij erkend is", geldig vanaf 2026. */
    static Path melddocument() throws Exception {
        String meld = TmsCreatieWitnessTest.objectcreatie("meld vanaf 2026", "Jutter", "Melding",
                List.of(TmsCreatieWitnessTest.init("waarde", literaal("7", GEHEEL))), check("erkend", false),
                "jutmelding", "melding").replace("\"validity\":{\"node\":\"Validity\"}", "\"validity\":" + VANAF_2026);
        return TmsCreatieWitnessTest.document(MELDMODEL, List.of(ERKENNING, MELDINGAANTAL), List.of(), List.of(meld),
                List.of(), List.of());
    }

    @Test void een_objectcreatie_aan_beide_kanten_van_de_rekendatum() throws Exception {
        Path ir = melddocument();
        JsonNode binnen = TmsDocumenten.zaakOp("2026-01-01", List.of(
                instantieMet("J1", "Jutter", Map.of(), List.of("aangemeld")), instantieMet("J2", "Jutter", Map.of(), List.of())),
                List.of(waarde("J1", "meldingaantal", "1"), waarde("J2", "meldingaantal", "0"), consistent()));
        JsonNode ervoor = TmsDocumenten.zaakOp("2025-12-31", List.of(
                instantieMet("J1", "Jutter", Map.of(), List.of("aangemeld"))),
                List.of(waarde("J1", "meldingaantal", "0"), consistent()));
        JsonNode tijdvrij = TmsDocumenten.zaakOp(null, List.of(
                instantieMet("J1", "Jutter", Map.of(), List.of("aangemeld"))), List.of(consistent()));
        assertEquals(List.of("gespeeld", "gespeeld", "Rekenfout: fout in de regel \"meld vanaf 2026\": " + ZONDER_DATUM),
                List.of(TmsFoutWitnessTest.speel(ir, binnen, DroolsBackend.Branch.EXEC_MODEL),
                        TmsFoutWitnessTest.speel(ir, ervoor, DroolsBackend.Branch.EXEC_MODEL),
                        TmsFoutWitnessTest.speel(ir, tijdvrij, DroolsBackend.Branch.EXEC_MODEL)));
        DroolsBackend.Build b = DroolsBackend.build(ir, DroolsBackend.Branch.EXEC_MODEL);
        for (List<Rule> volgorde : TmsDocumenten.steekproef(b.execModelTranspiler().model().getRules(), 20261009L, 60)) {
            List<String> namen = volgorde.stream().map(Rule::getName).toList();
            for (String datum : List.of("2026-01-01", "2025-12-31")) {
                Object j1 = TmsDocumenten.boon(b.beanLoader(), "Jutter", Map.of(), List.of("aangemeld"));
                assertNull(TmsCreatieWitnessTest.vuur(b, volgorde, List.of(j1),
                        sessie -> sessie.setGlobal("rekendatum", LocalDate.parse(datum)), sessie -> { }),
                        () -> "volgorde " + namen);
                TmsFoutWitnessTest.assertGetal(datum.startsWith("2026") ? "1" : "0", TmsDocumenten.lees(j1,
                        "meldingaantal"), "meldingaantal op " + datum + " in volgorde " + namen);
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
    @Test void een_creatie_waarvan_een_initialisatie_faalt_maakt_niets() throws Exception {
        String model = TmsDocumenten.model(List.of(
                TmsDocumenten.objecttype("Jutter", List.of(), List.of("aangemeld")),
                TmsDocumenten.objecttype("Melding", List.of(attribuut("jaar", GEHEEL), attribuut("tweede jaar", GEHEEL)),
                        List.of())),
                List.of(feittypeEenNaarVeel("jutmelding", "melder", "Jutter", "melding", "Melding", "meldingen")));
        String meld = TmsCreatieWitnessTest.objectcreatie("melding van een jutter", "Jutter", "Melding",
                List.of(TmsCreatieWitnessTest.init("jaar", contextwaarde("evaluationYear", GEHEEL))),
                check("aangemeld", false), "jutmelding", "melding");
        String tweede = rekenregel("tweede jaar van een melding", "BEREKENING", "Melding", "tweede jaar",
                contextwaarde("evaluationYear", GEHEEL), "null");
        Path ir = TmsCreatieWitnessTest.document(model, List.of(tweede), List.of(), List.of(meld), List.of(), List.of());
        assertEquals(List.of("Rekenfout: fout in de regel \"melding van een jutter\": the evaluationYear context value"
                        + " needs an evaluation date; the case carries asOfDate null", "gespeeld"),
                List.of(TmsFoutWitnessTest.speel(ir, TmsDocumenten.zaakOp(null, List.of(
                                instantieMet("J1", "Jutter", Map.of(), List.of("aangemeld"))), List.of(consistent())),
                                DroolsBackend.Branch.EXEC_MODEL),
                        TmsFoutWitnessTest.speel(ir, TmsDocumenten.zaakOp("2026-06-01", List.of(
                                instantieMet("J1", "Jutter", Map.of(), List.of("aangemeld"))), List.of(consistent())),
                                DroolsBackend.Branch.EXEC_MODEL)));
        // a sample of the orders of the generated rules: the one fault, and no Melding that the rule could read
        DroolsBackend.Build b = DroolsBackend.build(ir, DroolsBackend.Branch.EXEC_MODEL);
        int volgordes = 0;
        for (List<Rule> volgorde : TmsDocumenten.steekproef(b.execModelTranspiler().model().getRules(), 20261009L, 60)) {
            Object j1 = TmsDocumenten.boon(b.beanLoader(), "Jutter", Map.of(), List.of("aangemeld"));
            String fout;
            try {
                fout = TmsCreatieWitnessTest.vuur(b, volgorde, List.of(j1), sessie -> { }, sessie -> { });
            } catch (RuntimeException e) {
                fout = e.getClass().getSimpleName() + ": " + e.getMessage();
            }
            assertEquals("Rekenfout: fout in de regel \"melding van een jutter\": the evaluationYear context value needs"
                    + " an evaluation date; the case carries asOfDate null", fout,
                    "volgorde " + volgorde.stream().map(Rule::getName).toList());
            volgordes++;
        }
        assertTrue(volgordes > 2);
    }

    private static final String HAVENMODEL = TmsDocumenten.model(List.of(
            TmsDocumenten.objecttype("Kustwacht", List.of(), List.of()),
            TmsDocumenten.objecttype("Haven", List.of(attribuut("jutteraantal", GEHEEL)), List.of("vergund", "open")),
            TmsDocumenten.objecttype("Jutter", List.of(), List.of())), List.of(
            feittypeEenNaarVeel("bewaking", "kustwacht", "Kustwacht", "bewaakte haven", "Haven", "bewaakte havens"),
            feittypeEenNaarVeel("werving", "werver", "Kustwacht", "geworven jutter", "Jutter", "geworven jutters"),
            feittypeEenNaarVeel("havenlid", "haven", "Haven", "havenjutter", "Jutter", "havenjutters")));

    /** "Een Haven is open indien zij vergund is". */
    static final String OPENING = kenmerkregel("opening", "Haven", "open", check("vergund", false));

    /** "Het jutteraantal van een Haven moet berekend worden als het aantal havenjutters van de Haven". */
    static final String JUTTERAANTAL = rekenregel("jutteraantal", "BEREKENING", "Haven", "jutteraantal",
            aggregatie("count", "{\"node\":\"AttributeReference\",\"attribute\":\"havenjutters\",\"subjectType\":"
                    + "\"Haven\",\"source\":{\"snippet\":\"havenjutters van de Haven\"}}", "Jutter",
                    bronrol("havenlid", "havenjutter"), "null", "null", GEHEEL, "het aantal havenjutters van de Haven"),
            "null");

    /** "Een havenjutter van een Haven is een geworven jutter van de kustwacht van de Haven, indien de Haven open is",
     *  geldig tot en met 2025, beside {@code regels} in their order. */
    static Path havendocument(List<String> regels) throws Exception {
        String werving = TmsCreatieWitnessTest.feitcreatie("werving tot en met 2025", "havenlid", "haven", "havenjutter",
                "Haven", List.of(rolbinding("bewaking", "kustwacht"), rolbinding("werving", "geworven jutter")),
                check("open", false)).replace("\"validity\":{\"node\":\"Validity\"}",
                "\"validity\":" + geldigheid(null, "2025-12-31"));
        return TmsCreatieWitnessTest.document(HAVENMODEL, regels, List.of(), List.of(), List.of(werving), List.of());
    }

    static JsonNode havenzaak(String asOfDate, String aantal) throws Exception {
        String k1 = instantieMetRollen("K1", "Kustwacht", Map.of(), List.of(),
                Map.of("bewaakte havens", List.of("H1"), "geworven jutters", List.of("J1", "J2")));
        String h1 = instantieMetRollen("H1", "Haven", Map.of(), List.of("vergund"), Map.of("havenjutters", List.of("J1")));
        return TmsDocumenten.zaakOp(asOfDate, List.of(k1, h1, instantieMetRollen("J1", "Jutter", Map.of(), List.of(), Map.of()),
                instantieMetRollen("J2", "Jutter", Map.of(), List.of(), Map.of())),
                aantal == null ? List.of(consistent()) : List.of(waarde("H1", "jutteraantal", aantal), consistent()));
    }

    @Test void een_feitcreatie_aan_beide_kanten_van_de_rekendatum() throws Exception {
        for (List<String> volgorde : TmsDocumenten.permutaties(List.of(OPENING, JUTTERAANTAL))) {
            Path ir = havendocument(volgorde);
            List<String> namen = TmsDocumenten.regelnamen(volgorde);
            assertEquals(List.of("gespeeld", "gespeeld", "gespeeld",
                            "Rekenfout: fout in de regel \"werving tot en met 2025\": " + ZONDER_DATUM),
                    List.of(TmsFoutWitnessTest.speel(ir, havenzaak("2025-06-01", "2"), DroolsBackend.Branch.EXEC_MODEL),
                            TmsFoutWitnessTest.speel(ir, havenzaak("2025-12-31", "2"), DroolsBackend.Branch.EXEC_MODEL),
                            TmsFoutWitnessTest.speel(ir, havenzaak("2026-01-01", "1"), DroolsBackend.Branch.EXEC_MODEL),
                            TmsFoutWitnessTest.speel(ir, havenzaak(null, null), DroolsBackend.Branch.EXEC_MODEL)),
                    () -> "volgorde " + namen);
        }
        // a sample of the orders of the generated rules, on both sides of the date
        DroolsBackend.Build b = DroolsBackend.build(havendocument(List.of(OPENING, JUTTERAANTAL)),
                DroolsBackend.Branch.EXEC_MODEL);
        int volgordes = 0;
        for (List<Rule> volgorde : TmsDocumenten.steekproef(b.execModelTranspiler().model().getRules(), 20261009L, 60)) {
            List<String> namen = volgorde.stream().map(Rule::getName).toList();
            for (String datum : List.of("2025-12-31", "2026-01-01")) {
                Object k1 = TmsDocumenten.boon(b.beanLoader(), "Kustwacht", Map.of(), List.of());
                Object h1 = TmsDocumenten.boon(b.beanLoader(), "Haven", Map.of(), List.of("vergund"));
                Object j1 = TmsDocumenten.boon(b.beanLoader(), "Jutter", Map.of(), List.of());
                Object j2 = TmsDocumenten.boon(b.beanLoader(), "Jutter", Map.of(), List.of());
                zetRol(h1, "kustwacht", k1);
                zetRol(j1, "werver", k1);
                zetRol(j2, "werver", k1);
                zetRol(j1, "haven", h1);
                assertNull(TmsCreatieWitnessTest.vuur(b, volgorde, List.of(k1, h1, j1, j2),
                        sessie -> sessie.setGlobal("rekendatum", LocalDate.parse(datum)), sessie -> { }),
                        () -> "volgorde " + namen);
                TmsFoutWitnessTest.assertGetal(datum.startsWith("2025") ? "2" : "1", TmsDocumenten.lees(h1,
                        "jutteraantal"), "jutteraantal op " + datum + " in volgorde " + namen);
            }
            volgordes++;
        }
        assertTrue(volgordes > 2);
    }

    private static void zetRol(Object boon, String rol, Object doel) throws Exception {
        for (java.lang.reflect.Method m : boon.getClass().getMethods())
            if (m.getName().equals(dev.uchar.regelspraak.transpiler.kern.Namen.setter(rol)) && m.getParameterCount() == 1) {
                m.invoke(boon, doel);
                return;
            }
        throw new AssertionError("geen setter voor " + rol + " op " + boon.getClass());
    }

    // ---------------- the context values ----------------

    private static final String WACHTMODEL = TmsDocumenten.model(List.of(TmsDocumenten.objecttype("Jutter", List.of(
            attribuut("aanmelddatum", DATUM), attribuut("startdatum", DATUM), attribuut("wachtjaren", GEHEEL),
            attribuut("verstreken", getaltype(true, 0, eenheid("dagen"))), attribuut("bonus", GEHEEL),
            attribuut("bonus vanaf start", GEHEEL)), List.of())), List.of());

    /** "De startdatum van een Jutter moet berekend worden als zijn aanmelddatum plus 1 maand". */
    static final String START = rekenregel("startdatum van een jutter", "BEREKENING", "Jutter", "startdatum",
            binair("plus", lezing("aanmelddatum", "Jutter"), literaal("1", getaltype(true, 0, eenheid("maand"))), DATUM),
            "null");

    static final String WACHTJAREN = rekenregel("wachtjaren van een jutter", "BEREKENING", "Jutter", "wachtjaren",
            binair("minus", contextwaarde("evaluationYear", GEHEEL), builtin("yearOf",
                    List.of(lezing("startdatum", "Jutter")), GEHEEL, "het jaar uit zijn startdatum"), GEHEEL), "null");

    static final String VERSTREKEN = rekenregel("verstreken dagen van een jutter", "BEREKENING", "Jutter", "verstreken",
            builtin("duration", List.of(lezing("startdatum", "Jutter"), contextwaarde("evaluationDate", DATUM)),
                    getaltype(true, 0, eenheid("dagen")), "de tijdsduur van zijn startdatum tot de Rekendatum in hele"
                            + " dagen"), "null");

    static JsonNode wachtzaak(String asOfDate, List<String> verwachtingen) throws Exception {
        return TmsDocumenten.zaakOp(asOfDate, List.of(
                instantieMet("J1", "Jutter", Map.of("aanmelddatum", datumwaarde("2024-11-15"),
                        "startdatum", datumwaarde("2000-01-01")), List.of()),
                instantieMet("J2", "Jutter", Map.of(), List.of())), verwachtingen);
    }

    @Test void het_rekenjaar_en_de_rekendatum_naast_een_later_afgeleide_startdatum() throws Exception {
        List<String> uitkomsten = new ArrayList<>(), verwacht = new ArrayList<>();
        for (List<String> volgorde : TmsDocumenten.permutaties(List.of(START, WACHTJAREN))) {
            List<String> namen = TmsDocumenten.regelnamen(volgorde);
            Path ir = TmsDocumenten.document(WACHTMODEL, volgorde);
            uitkomsten.add(namen + " " + TmsFoutWitnessTest.speel(ir, wachtzaak("2026-06-01", List.of(
                    waarde("J1", "wachtjaren", "2"), waarde("J2", "wachtjaren", "2026"), consistent())),
                    DroolsBackend.Branch.EXEC_MODEL));
            uitkomsten.add(namen + " " + TmsFoutWitnessTest.speel(ir, wachtzaak(null, List.of(consistent())),
                    DroolsBackend.Branch.EXEC_MODEL));
            verwacht.add(namen + " gespeeld");
            verwacht.add(namen + " Evaluatiefouten: aan het fixpunt staan 2 evaluatiefouten, in de regel \"wachtjaren van"
                    + " een jutter\" (2×): fout in de regel \"wachtjaren van een jutter\": the evaluationYear context value"
                    + " needs an evaluation date; the case carries asOfDate null; fout in de regel \"wachtjaren van een"
                    + " jutter\": the evaluationYear context value needs an evaluation date; the case carries asOfDate null");
        }
        for (List<String> volgorde : TmsDocumenten.permutaties(List.of(START, VERSTREKEN))) {
            List<String> namen = TmsDocumenten.regelnamen(volgorde);
            Path ir = TmsDocumenten.document(WACHTMODEL, volgorde);
            uitkomsten.add(namen + " " + TmsFoutWitnessTest.speel(ir, wachtzaak("2026-06-01", List.of(
                    waarde("J1", "verstreken", "533"), waarde("J2", "verstreken", null), consistent())),
                    DroolsBackend.Branch.EXEC_MODEL));
            uitkomsten.add(namen + " " + TmsFoutWitnessTest.speel(ir, wachtzaak(null, List.of(consistent())),
                    DroolsBackend.Branch.EXEC_MODEL));
            verwacht.add(namen + " gespeeld");
            verwacht.add(namen + " Evaluatiefouten: aan het fixpunt staan 2 evaluatiefouten, in de regel \"verstreken dagen"
                    + " van een jutter\" (2×): fout in de regel \"verstreken dagen van een jutter\": the evaluationDate"
                    + " context value needs an evaluation date; the case carries asOfDate null; fout in de regel"
                    + " \"verstreken dagen van een jutter\": the evaluationDate context value needs an evaluation date; the"
                    + " case carries asOfDate null");
        }
        assertEquals(verwacht, uitkomsten);
    }

    // ---------------- the time window ----------------

    /** "De bonus van een Jutter moet berekend worden als 100, indien het de periode van 2026-01-01 tot 2027-01-01 is". */
    static final String BONUS = rekenregel("bonus van een jutter", "BEREKENING", "Jutter", "bonus", literaal("100", GEHEEL),
            venster(literaal("2026-01-01", DATUM), literaal("2027-01-01", DATUM), false));

    /** "De bonus vanaf start van een Jutter moet berekend worden als 100, indien het de periode vanaf zijn startdatum
     *  is": a bound that another rule derives. */
    static final String BONUS_VANAF_START = rekenregel("bonus vanaf start", "BEREKENING", "Jutter", "bonus vanaf start",
            literaal("100", GEHEEL), venster(lezing("startdatum", "Jutter"), "null", false));

    static JsonNode vensterzaak(String asOfDate, String bonus, String j1, String j2) throws Exception {
        return TmsDocumenten.zaakOp(asOfDate, List.of(
                instantieMet("J1", "Jutter", Map.of("aanmelddatum", datumwaarde("2024-11-15"),
                        "startdatum", datumwaarde("2030-01-01")), List.of()),
                instantieMet("J2", "Jutter", Map.of("aanmelddatum", datumwaarde("2026-06-15")), List.of()),
                instantieMet("J3", "Jutter", Map.of(), List.of())),
                List.of(waarde("J1", "bonus", bonus), waarde("J1", "bonus vanaf start", j1),
                        waarde("J2", "bonus vanaf start", j2), waarde("J3", "bonus vanaf start", null), consistent()));
    }

    @Test void een_periodevenster_over_de_rekendatum_en_een_later_afgeleide_grens() throws Exception {
        List<String> uitkomsten = new ArrayList<>(), verwacht = new ArrayList<>();
        for (List<String> volgorde : TmsDocumenten.permutaties(List.of(START, BONUS, BONUS_VANAF_START))) {
            List<String> namen = TmsDocumenten.regelnamen(volgorde);
            Path ir = TmsDocumenten.document(WACHTMODEL, volgorde);
            for (JsonNode zaak : List.of(vensterzaak("2026-06-01", "100", "100", null),
                    vensterzaak("2026-01-01", "100", "100", null), vensterzaak("2026-12-31", "100", "100", "100"),
                    vensterzaak("2027-01-01", null, "100", "100"), vensterzaak("2025-12-31", null, "100", null),
                    vensterzaak(null, null, null, null)))
                uitkomsten.add(namen + " " + zaak.get("asOfDate") + " " + TmsFoutWitnessTest.speel(ir, zaak,
                        DroolsBackend.Branch.EXEC_MODEL));
            for (String d : List.of("\"2026-06-01\"", "\"2026-01-01\"", "\"2026-12-31\"", "\"2027-01-01\"",
                    "\"2025-12-31\"", "null"))
                verwacht.add(namen + " " + d + " gespeeld");
        }
        assertEquals(verwacht, uitkomsten);
    }

    /** "… indien hij aan geen van de volgende voorwaarden voldoet: het is de periode van 2026-01-01 tot 2027-01-01". */
    @Test void een_periodevenster_onder_geen_wordt_geweigerd() throws Exception {
        String regel = rekenregel("bonus buiten 2026", "BEREKENING", "Jutter", "bonus", literaal("100", GEHEEL),
                samengesteld("none", 0, List.of(venster(literaal("2026-01-01", DATUM), literaal("2027-01-01", DATUM),
                        false))));
        CaseRunner.Excluded e = assertThrows(CaseRunner.Excluded.class, () -> DroolsBackend.build(
                TmsDocumenten.document(WACHTMODEL, List.of(regel)), DroolsBackend.Branch.EXEC_MODEL));
        assertEquals("transpiler SKIP: regel: bonus buiten 2026 — periodevenster in een GEEN-samenstelling (WF-112):"
                + " de kwantor telt of ontkent het vensterverdict, terwijl het venster de regel poort (RUNTIME-SEMANTICS"
                + " ch. 3.10), en voor die lezing is niets gespecificeerd: none 0", e.getMessage());
    }
}
