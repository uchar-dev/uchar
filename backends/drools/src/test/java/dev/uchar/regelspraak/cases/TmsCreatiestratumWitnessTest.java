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

import static dev.uchar.regelspraak.cases.TmsCreatieWitnessTest.document;
import static dev.uchar.regelspraak.cases.TmsCreatieWitnessTest.feitcreatie;
import static dev.uchar.regelspraak.cases.TmsCreatieWitnessTest.gemaakt;
import static dev.uchar.regelspraak.cases.TmsCreatieWitnessTest.init;
import static dev.uchar.regelspraak.cases.TmsCreatieWitnessTest.koppelingen;
import static dev.uchar.regelspraak.cases.TmsCreatieWitnessTest.objectcreatie;
import static dev.uchar.regelspraak.cases.TmsCreatieWitnessTest.regelgroep;
import static dev.uchar.regelspraak.cases.TmsCreatieWitnessTest.vuur;
import static dev.uchar.regelspraak.cases.TmsDocumenten.GEHEEL;
import static dev.uchar.regelspraak.cases.TmsDocumenten.TELLING;
import static dev.uchar.regelspraak.cases.TmsDocumenten.aggregatie;
import static dev.uchar.regelspraak.cases.TmsDocumenten.attribuut;
import static dev.uchar.regelspraak.cases.TmsDocumenten.binair;
import static dev.uchar.regelspraak.cases.TmsDocumenten.bronrol;
import static dev.uchar.regelspraak.cases.TmsDocumenten.feittype;
import static dev.uchar.regelspraak.cases.TmsDocumenten.feittypeEenNaarVeel;
import static dev.uchar.regelspraak.cases.TmsDocumenten.getal;
import static dev.uchar.regelspraak.cases.TmsDocumenten.instantieMetRollen;
import static dev.uchar.regelspraak.cases.TmsDocumenten.lezing;
import static dev.uchar.regelspraak.cases.TmsDocumenten.objecttype;
import static dev.uchar.regelspraak.cases.TmsDocumenten.padlezing;
import static dev.uchar.regelspraak.cases.TmsDocumenten.permutaties;
import static dev.uchar.regelspraak.cases.TmsDocumenten.regelnamen;
import static dev.uchar.regelspraak.cases.TmsDocumenten.rekenregel;
import static dev.uchar.regelspraak.cases.TmsDocumenten.rolbinding;
import static dev.uchar.regelspraak.cases.TmsDocumenten.steekproef;
import static dev.uchar.regelspraak.cases.TmsDocumenten.vergelijking;
import static dev.uchar.regelspraak.cases.TmsDocumenten.waarde;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Creations in the evaluation strata of the reference interpreter (RUNTIME-SEMANTICS ch. 6.1 and 6.3): a creation
 * writes irrevocably, so it reads strictly and sits in a stratum above the producers of what it reads, and the forms of
 * a stratum reach their fixpoint before a form of the next one fires ({@code ref/lib/eval.ml}). The driver opens the
 * creation phases stratum by stratum, each in generations, the rule layer under truth maintenance throughout
 * ({@code DroolsBackend.vuur}). Each document plays through the replay in every order of its creations and rules in the
 * document, and fires through the driver in the order of the rules the branch generates, its reverse and a hundred
 * shuffles.
 *
 * <ul>
 *   <li>An initialisation reads what a creation of a lower stratum made: "maak een afrekening" counts the Penning that
 *       "geef een penning" makes, where both fired against one state before and the count stayed 0, fixed at creation
 *       ({@link #een_initialisatie_leest_wat_een_creatie_uit_een_lager_stratum_maakte}).</li>
 *   <li>A condition with an operand that grows is decided on the value after the lower stratum: the sum of the
 *       penningen is 10 there, not the 5 of the input, so no Afrekening is made
 *       ({@link #een_voorwaarde_met_een_groeiende_operand_beslist_na_het_lagere_stratum}).</li>
 *   <li>A chain object creation, fact creation, object creation runs in three strata, and the last initialisation
 *       counts the link the fact creation made over the object the first made
 *       ({@link #een_keten_van_objectcreatie_feitcreatie_en_objectcreatie_loopt_in_drie_strata}).</li>
 *   <li>Two creations of one stratum that do not read each other fire in one generation, in one phase, and both read
 *       the Penning of the stratum below ({@link #twee_creaties_van_een_stratum_vuren_in_een_generatie}).</li>
 *   <li>The budget of 100 generations holds per stratum, as the reference interpreter bounds each stratum's fixpoint
 *       by 100 passes: two chains of 51 generations in two strata play
 *       ({@link #het_generatiebudget_geldt_per_stratum}).</li>
 * </ul>
 *
 * <p><b>The other executors</b> (2026-10-08). Every document written here passes the verifier, and the reference
 * interpreter and the ASP backend give the expectations of each case in every order of its creations and rules in the
 * document.
 *
 * <p><b>Red before the change</b> (measured 2026-10-08 on {@code 6b4c966f}, all creations of a generation in one
 * phase): all five failed, in every document order: "case: attribuut penningaantal: verwacht 1, werkelijk 0" on P and
 * on K, "case: attribuut afrekeningaantal: verwacht 0, werkelijk 1" on S, "case: attribuut penningaantal: verwacht 1,
 * werkelijk 0; attribuut kwitantieaantal: verwacht 1, werkelijk 0" on T, and on B the two chains ran side by side:
 * "expected: &lt;Wachter in fase 2&gt; but was: &lt;Boei in fase 2&gt;". Mutants of the change, each caught: a budget
 * over the whole run (B: "geen fixpunt binnen 100 generaties: generatie 50 maakt nog 1 object of link"), every
 * creation in a stratum of its own above the reference interpreter's (T: "Kwitantie in fase 10"), strata in the order
 * the transpiler lowers the creations (P, S, K and T), and strata without the strict edges (all five).
 */
class TmsCreatiestratumWitnessTest {

    // ---------------- the Jutter, his penningen and his afrekening ----------------

    private static final String PENNINGEN = "{\"node\":\"AttributeReference\",\"attribute\":\"penningen\",\"subjectType\":"
            + "\"Jutter\",\"source\":{\"snippet\":\"penningen van de Jutter\"}}";

    private static final String AANTAL_PENNINGEN = aggregatie("count", PENNINGEN, "Penning",
            bronrol("jutterheeftpenning", "penning"), "null", "null", TELLING, "het aantal penningen van de Jutter");

    /** "Eén houder heeft meerdere penningen." */
    private static final String PENNINGFEIT = feittypeEenNaarVeel("jutterheeftpenning", "houder", "Jutter", "penning",
            "Penning", "penningen");

    /** "Eén afrekenaar heeft één afrekening." */
    private static final String AFREKENFEIT = feittype("jutterheeftafrekening", "afrekenaar", "Jutter", "afrekening",
            "Afrekening");

    /** "Een Jutter heeft een penning met bedrag gelijk aan 5." */
    static final String GEEF = objectcreatie("geef een penning", "Jutter", "Penning", List.of(init("bedrag", getal("5"))),
            "null", "jutterheeftpenning", "penning");

    /** "Een Jutter heeft een afrekening met penningtelling gelijk aan het aantal penningen van de Jutter." */
    static final String MAAK = objectcreatie("maak een afrekening", "Jutter", "Afrekening",
            List.of(init("penningtelling", AANTAL_PENNINGEN)), "null", "jutterheeftafrekening", "afrekening");

    /** "Het penningaantal van een Jutter moet berekend worden als de penningtelling van de afrekening van de Jutter." */
    static final String PENNINGAANTAL = rekenregel("penningaantal", "BEREKENING", "Jutter", "penningaantal",
            padlezing("penningtelling", "Afrekening", "jutterheeftafrekening", "afrekening"), "null");

    // ---------------- document P: an initialisation reads a creation of a lower stratum ----------------

    private static final String MODEL_P = TmsDocumenten.model(List.of(
            objecttype("Jutter", List.of(attribuut("penningaantal", GEHEEL)), List.of()),
            objecttype("Penning", List.of(attribuut("bedrag", GEHEEL)), List.of()),
            objecttype("Afrekening", List.of(attribuut("penningtelling", GEHEEL)), List.of())),
            List.of(PENNINGFEIT, AFREKENFEIT));

    /** The scenario of the final lens: "maak een afrekening" reads the penningen that "geef een penning" makes, a strict
     *  read (it writes irrevocably), so it sits a stratum above it; "penningaantal" reads the count off the afrekening. */
    static Path documentP(List<String> creaties, List<String> regels) throws Exception {
        return document(MODEL_P, regels, List.of(), creaties, List.of(), List.of());
    }

    static JsonNode zaakP() throws Exception {
        return TmsDocumenten.zaak(List.of(instantieMetRollen("J1", "Jutter", Map.of(), List.of(), Map.of())),
                List.of(waarde("J1", "penningaantal", "1")));
    }

    @Test void een_initialisatie_leest_wat_een_creatie_uit_een_lager_stratum_maakte() throws Exception {
        speeltInElkeVolgorde(List.of(GEEF, MAAK), List.of(PENNINGAANTAL), TmsCreatiestratumWitnessTest::documentP, zaakP());
        DroolsBackend.Build b = DroolsBackend.build(documentP(List.of(GEEF, MAAK), List.of(PENNINGAANTAL)),
                DroolsBackend.Branch.TRUTH_MAINTENANCE);
        for (List<Rule> volgorde : steekproef(b.tmsTranspiler().model().getRules(), 20261008L, 100)) {
            List<String> namen = volgorde.stream().map(Rule::getName).toList();
            Object j1 = TmsDocumenten.boon(b.beanLoader(), "Jutter", Map.of(), List.of());
            List<Object[]> objecten = new ArrayList<>();
            assertNull(vuur(b, volgorde, List.of(j1), sessie -> { }, sessie -> objecten.addAll(gemaakt(sessie))),
                    () -> "volgorde " + namen);
            assertGetal("1", TmsDocumenten.lees(j1, "penningaantal"), "penningaantal in volgorde " + namen);
            // the Penning in the object phase of stratum 0's first generation; stratum 0 closes with a generation that
            // makes nothing (phases 3 and 4), and the Afrekening follows in the object phase of stratum 1
            assertEquals(List.of("Penning in fase 2", "Afrekening in fase 6"), soorten(objecten), () -> "volgorde " + namen);
        }
    }

    // ---------------- document S: a condition whose operand grows ----------------

    private static final String AFREKENINGEN = "{\"node\":\"AttributeReference\",\"attribute\":\"afrekeningen\","
            + "\"subjectType\":\"Jutter\",\"source\":{\"snippet\":\"afrekeningen van de Jutter\"}}";

    private static final String SOM = aggregatie("sum", lezing("bedrag", "Penning"), "Penning",
            bronrol("jutterheeftpenning", "penning"), "null", getal("0"), GEHEEL,
            "de som van het bedrag van alle penningen van de Jutter of 0 als die er niet zijn");

    private static final String MODEL_S = TmsDocumenten.model(List.of(
            objecttype("Jutter", List.of(attribuut("afrekeningaantal", GEHEEL), attribuut("penningsom", GEHEEL)),
                    List.of()),
            objecttype("Penning", List.of(attribuut("bedrag", GEHEEL)), List.of()),
            objecttype("Afrekening", List.of(), List.of())),
            List.of(PENNINGFEIT, feittypeEenNaarVeel("jutterheeftafrekening", "afrekenaar", "Jutter", "afrekening",
                    "Afrekening", "afrekeningen")));

    /** "Een Jutter heeft een afrekening indien de som van het bedrag van alle penningen van de Jutter kleiner is dan
     *  8." */
    static final String MAAK_ONDER_ACHT = objectcreatie("maak een afrekening", "Jutter", "Afrekening", List.of(),
            vergelijking("less", SOM, getal("8")), "jutterheeftafrekening", "afrekening");

    static final List<String> REGELS_S = List.of(
            rekenregel("afrekeningaantal", "BEREKENING", "Jutter", "afrekeningaantal", aggregatie("count", AFREKENINGEN,
                    "Afrekening", bronrol("jutterheeftafrekening", "afrekening"), "null", "null", TELLING,
                    "het aantal afrekeningen van de Jutter"), "null"),
            rekenregel("penningsom", "BEREKENING", "Jutter", "penningsom", SOM, "null"));

    static Path documentS(List<String> creaties, List<String> regels) throws Exception {
        return document(MODEL_S, regels, List.of(), creaties, List.of(), List.of());
    }

    /** J1 holds P1 of 5 in the input; "geef een penning" adds one of 5, so the sum is 10 after stratum 0. */
    static JsonNode zaakS() throws Exception {
        return TmsDocumenten.zaak(List.of(
                instantieMetRollen("J1", "Jutter", Map.of(), List.of(), Map.of("penningen", List.of("P1"))),
                instantieMetRollen("P1", "Penning", Map.of("bedrag", "5"), List.of(), Map.of())),
                List.of(waarde("J1", "afrekeningaantal", "0"), waarde("J1", "penningsom", "10")));
    }

    @Test void een_voorwaarde_met_een_groeiende_operand_beslist_na_het_lagere_stratum() throws Exception {
        speeltInElkeVolgorde(List.of(GEEF, MAAK_ONDER_ACHT), REGELS_S, TmsCreatiestratumWitnessTest::documentS, zaakS());
        DroolsBackend.Build b = DroolsBackend.build(documentS(List.of(GEEF, MAAK_ONDER_ACHT), REGELS_S),
                DroolsBackend.Branch.TRUTH_MAINTENANCE);
        for (List<Rule> volgorde : steekproef(b.tmsTranspiler().model().getRules(), 20261008L, 100)) {
            List<String> namen = volgorde.stream().map(Rule::getName).toList();
            Object j1 = TmsDocumenten.boon(b.beanLoader(), "Jutter", Map.of(), List.of());
            Object p1 = TmsDocumenten.boon(b.beanLoader(), "Penning", Map.of("bedrag", "5"), List.of());
            zetRol(p1, "houder", j1);
            List<Object[]> objecten = new ArrayList<>();
            assertNull(vuur(b, volgorde, List.of(j1, p1), sessie -> { }, sessie -> objecten.addAll(gemaakt(sessie))),
                    () -> "volgorde " + namen);
            assertGetal("0", TmsDocumenten.lees(j1, "afrekeningaantal"), "afrekeningaantal in volgorde " + namen);
            assertGetal("10", TmsDocumenten.lees(j1, "penningsom"), "penningsom in volgorde " + namen);
            assertEquals(List.of("Penning in fase 2"), soorten(objecten), () -> "volgorde " + namen);
        }
    }

    // ---------------- document K: object creation, fact creation, object creation ----------------

    private static final String BEWAARDE = "{\"node\":\"AttributeReference\",\"attribute\":\"bewaarde penningen\","
            + "\"subjectType\":\"Jutter\",\"source\":{\"snippet\":\"bewaarde penningen van de Jutter\"}}";

    private static final String MODEL_K = TmsDocumenten.model(List.of(
            objecttype("Jutter", List.of(attribuut("penningaantal", GEHEEL)), List.of()),
            objecttype("Penning", List.of(attribuut("bedrag", GEHEEL)), List.of()),
            objecttype("Afrekening", List.of(attribuut("penningtelling", GEHEEL)), List.of())),
            List.of(PENNINGFEIT, feittypeEenNaarVeel("bewaring", "bewaarder", "Jutter", "bewaarde penning", "Penning",
                    "bewaarde penningen"), AFREKENFEIT));

    /** "Een bewaarde penning van een Jutter is een penning van de Jutter." */
    static final String BEWAAR = feitcreatie("bewaar de penningen", "bewaring", "bewaarder", "bewaarde penning",
            "Jutter", List.of(rolbinding("jutterheeftpenning", "penning")), "null");

    /** "Een Jutter heeft een afrekening met penningtelling gelijk aan het aantal bewaarde penningen van de Jutter." */
    static final String MAAK_BEWAARD = objectcreatie("maak een afrekening", "Jutter", "Afrekening",
            List.of(init("penningtelling", aggregatie("count", BEWAARDE, "Penning", bronrol("bewaring", "bewaarde penning"),
                    "null", "null", TELLING, "het aantal bewaarde penningen van de Jutter"))),
            "null", "jutterheeftafrekening", "afrekening");

    static Path documentK(List<String> creaties, List<String> regels) throws Exception {
        return document(MODEL_K, regels, List.of(), creaties, List.of(BEWAAR), List.of());
    }

    @Test void een_keten_van_objectcreatie_feitcreatie_en_objectcreatie_loopt_in_drie_strata() throws Exception {
        speeltInElkeVolgorde(List.of(GEEF, MAAK_BEWAARD), List.of(PENNINGAANTAL), TmsCreatiestratumWitnessTest::documentK,
                zaakP());
        DroolsBackend.Build b = DroolsBackend.build(documentK(List.of(GEEF, MAAK_BEWAARD), List.of(PENNINGAANTAL)),
                DroolsBackend.Branch.TRUTH_MAINTENANCE);
        for (List<Rule> volgorde : steekproef(b.tmsTranspiler().model().getRules(), 20261008L, 100)) {
            List<String> namen = volgorde.stream().map(Rule::getName).toList();
            Object j1 = TmsDocumenten.boon(b.beanLoader(), "Jutter", Map.of(), List.of());
            List<Object[]> objecten = new ArrayList<>(), links = new ArrayList<>();
            assertNull(vuur(b, volgorde, List.of(j1), sessie -> { }, sessie -> {
                objecten.addAll(gemaakt(sessie));
                links.addAll(koppelingen(sessie, "bewaring"));
            }), () -> "volgorde " + namen);
            assertGetal("1", TmsDocumenten.lees(j1, "penningaantal"), "penningaantal in volgorde " + namen);
            // stratum 0 makes the Penning (phase 2) and closes in phases 3 and 4; stratum 1 links it in its first fact
            // phase (5) and closes in phases 7 and 8; stratum 2 makes the Afrekening in its first object phase (10)
            assertEquals(List.of("Penning in fase 2", "Afrekening in fase 10"), soorten(objecten), () -> "volgorde " + namen);
            assertEquals(1, links.size(), () -> "links in volgorde " + namen);
            assertTrue(links.get(0)[0] == j1 && links.get(0)[1] == objecten.get(0)[1], () -> "link in volgorde " + namen);
            assertEquals(5, links.get(0)[2], () -> "fase van de link in volgorde " + namen);
        }
    }

    // ---------------- document T: two creations of one stratum ----------------

    private static final String MODEL_T = TmsDocumenten.model(List.of(
            objecttype("Jutter", List.of(attribuut("penningaantal", GEHEEL), attribuut("kwitantieaantal", GEHEEL)),
                    List.of()),
            objecttype("Penning", List.of(attribuut("bedrag", GEHEEL)), List.of()),
            objecttype("Afrekening", List.of(attribuut("penningtelling", GEHEEL)), List.of()),
            objecttype("Kwitantie", List.of(attribuut("penningtelling", GEHEEL)), List.of())),
            List.of(PENNINGFEIT, AFREKENFEIT, feittype("jutterheeftkwitantie", "ontvanger", "Jutter", "kwitantie",
                    "Kwitantie")));

    /** "Een Jutter heeft een kwitantie met penningtelling gelijk aan het aantal penningen van de Jutter." */
    static final String KWITEER = objectcreatie("schrijf een kwitantie", "Jutter", "Kwitantie",
            List.of(init("penningtelling", AANTAL_PENNINGEN)), "null", "jutterheeftkwitantie", "kwitantie");

    static final List<String> REGELS_T = List.of(PENNINGAANTAL, rekenregel("kwitantieaantal", "BEREKENING", "Jutter",
            "kwitantieaantal", padlezing("penningtelling", "Kwitantie", "jutterheeftkwitantie", "kwitantie"), "null"));

    /** "maak een afrekening" and "schrijf een kwitantie" both read the penningen and neither reads what the other
     *  writes: one stratum, above "geef een penning". */
    static Path documentT(List<String> creaties, List<String> regels) throws Exception {
        return document(MODEL_T, regels, List.of(), creaties, List.of(), List.of());
    }

    static JsonNode zaakT() throws Exception {
        return TmsDocumenten.zaak(List.of(instantieMetRollen("J1", "Jutter", Map.of(), List.of(), Map.of())),
                List.of(waarde("J1", "penningaantal", "1"), waarde("J1", "kwitantieaantal", "1")));
    }

    @Test void twee_creaties_van_een_stratum_vuren_in_een_generatie() throws Exception {
        speeltInElkeVolgorde(List.of(GEEF, MAAK, KWITEER), REGELS_T, TmsCreatiestratumWitnessTest::documentT, zaakT());
        DroolsBackend.Build b = DroolsBackend.build(documentT(List.of(GEEF, MAAK, KWITEER), REGELS_T),
                DroolsBackend.Branch.TRUTH_MAINTENANCE);
        for (List<Rule> volgorde : steekproef(b.tmsTranspiler().model().getRules(), 20261008L, 100)) {
            List<String> namen = volgorde.stream().map(Rule::getName).toList();
            Object j1 = TmsDocumenten.boon(b.beanLoader(), "Jutter", Map.of(), List.of());
            List<Object[]> objecten = new ArrayList<>();
            assertNull(vuur(b, volgorde, List.of(j1), sessie -> { }, sessie -> objecten.addAll(gemaakt(sessie))),
                    () -> "volgorde " + namen);
            assertGetal("1", TmsDocumenten.lees(j1, "penningaantal"), "penningaantal in volgorde " + namen);
            assertGetal("1", TmsDocumenten.lees(j1, "kwitantieaantal"), "kwitantieaantal in volgorde " + namen);
            List<String> soorten = new ArrayList<>(soorten(objecten));
            soorten.sort(null);
            // both of stratum 1 in its first object phase, one generation
            assertEquals(List.of("Afrekening in fase 6", "Kwitantie in fase 6", "Penning in fase 2"), soorten,
                    () -> "volgorde " + namen);
        }
    }

    // ---------------- document B: the budget per stratum ----------------

    private static final String MODEL_B = TmsDocumenten.model(List.of(
            objecttype("Wachter", List.of(attribuut("rang", GEHEEL)), List.of()),
            objecttype("Boei", List.of(attribuut("nummer", GEHEEL), attribuut("wachterrang", GEHEEL)), List.of())),
            List.of(feittype("opvolging", "voorganger", "Wachter", "opvolger", "Wachter"),
                    feittype("boeiketen", "vorige boei", "Boei", "volgende boei", "Boei"),
                    feittype("bewaking", "wachter", "Wachter", "boei", "Boei")));

    /** A chain of Wachters while the rang is below 52: W1 of rang 1 makes W2, and so on to W52, 51 generations. */
    static final String WERF = objectcreatie("werf de opvolger", "Wachter", "Wachter",
            List.of(init("rang", binair("plus", lezing("rang", "Wachter"), getal("1")))),
            vergelijking("less", lezing("rang", "Wachter"), getal("52")), "opvolging", "opvolger");

    /** A chain of Boeien while the nummer is below 52, 51 generations; the wachterrang reads the rang of the Boei's
     *  wachter, which "werf de opvolger" writes, so this chain sits a stratum above the other. */
    static final String LEG = objectcreatie("leg de volgende boei", "Boei", "Boei",
            List.of(init("nummer", binair("plus", lezing("nummer", "Boei"), getal("1"))),
                    init("wachterrang", padlezing("rang", "Wachter", "bewaking", "wachter"))),
            vergelijking("less", lezing("nummer", "Boei"), getal("52")), "boeiketen", "volgende boei");

    static Path documentB(List<String> creaties) throws Exception {
        return document(MODEL_B, List.of(), List.of(), creaties, List.of(),
                List.of(regelgroep("estafette", List.of("werf de opvolger")),
                        regelgroep("boeienlijn", List.of("leg de volgende boei"))));
    }

    static JsonNode zaakB() throws Exception {
        return TmsDocumenten.zaak(List.of(
                instantieMetRollen("W1", "Wachter", Map.of("rang", "1"), List.of(), Map.of("boei", List.of("B1"))),
                instantieMetRollen("B1", "Boei", Map.of("nummer", "1"), List.of(), Map.of())),
                List.of(waarde("W1", "rang", "1")));
    }

    /** Two chains of 51 generations each, 102 in all, in two strata. The reference interpreter bounds the fixpoint of
     *  each stratum by 100 passes and plays the document; a budget over the whole run would end it at generation 101.
     *  The Boeien wait for the Wachters, so the first Boei is made after the 51 generations of stratum 0 and the one
     *  that closes it: in phase 2 &middot; 52 + 2. */
    @Test void het_generatiebudget_geldt_per_stratum() throws Exception {
        speeltInElkeVolgorde(List.of(WERF, LEG), List.of(), (creaties, regels) -> documentB(creaties), zaakB());
        DroolsBackend.Build b = DroolsBackend.build(documentB(List.of(WERF, LEG)), DroolsBackend.Branch.TRUTH_MAINTENANCE);
        for (List<Rule> volgorde : steekproef(b.tmsTranspiler().model().getRules(), 20261008L, 10)) {
            List<String> namen = volgorde.stream().map(Rule::getName).toList();
            Object w1 = TmsDocumenten.boon(b.beanLoader(), "Wachter", Map.of("rang", "1"), List.of());
            Object b1 = TmsDocumenten.boon(b.beanLoader(), "Boei", Map.of("nummer", "1"), List.of());
            zetRol(w1, "boei", b1);
            zetRol(b1, "wachter", w1);
            List<Object[]> objecten = new ArrayList<>();
            assertNull(vuur(b, volgorde, List.of(w1, b1), sessie -> { }, sessie -> objecten.addAll(gemaakt(sessie))),
                    () -> "volgorde " + namen);
            assertEquals(102, objecten.size(), () -> "objecten in volgorde " + namen);
            for (int i = 0; i < 51; i++) {
                assertEquals("Wachter in fase " + 2 * (i + 1), soort(objecten.get(i)), "volgorde " + namen);
                assertEquals("Boei in fase " + (2 * 52 + 2 * (i + 1)), soort(objecten.get(51 + i)), "volgorde " + namen);
            }
            assertGetal("52", TmsDocumenten.lees(objecten.get(50)[1], "rang"), "rang van W52 in volgorde " + namen);
            assertGetal("52", TmsDocumenten.lees(objecten.get(101)[1], "nummer"), "nummer van B52 in volgorde " + namen);
            assertGetal("1", TmsDocumenten.lees(objecten.get(51)[1], "wachterrang"), "wachterrang van B2 in volgorde "
                    + namen);
        }
    }

    // ---------------- playing and reading ----------------

    /** A document of {@code creaties} and {@code regels} in the order given. */
    interface Bouw {
        Path van(List<String> creaties, List<String> regels) throws Exception;
    }

    /** Plays case {@code zaak} through the replay on every order of {@code creaties} and of {@code regels} in the
     *  document; each must play. */
    private static void speeltInElkeVolgorde(List<String> creaties, List<String> regels, Bouw bouw, JsonNode zaak)
            throws Exception {
        List<String> uitkomsten = new ArrayList<>(), verwacht = new ArrayList<>();
        for (List<String> c : permutaties(creaties))
            for (List<String> r : permutaties(regels)) {
                String namen = regelnamen(c) + " " + regelnamen(r);
                uitkomsten.add(namen + " " + TmsFoutWitnessTest.speel(bouw.van(c, r), zaak,
                        DroolsBackend.Branch.TRUTH_MAINTENANCE));
                verwacht.add(namen + " gespeeld");
            }
        assertEquals(verwacht, uitkomsten);
    }

    /** The made objects as "&lt;type&gt; in fase &lt;phase&gt;", in the order of {@link TmsCreatieWitnessTest#gemaakt}. */
    private static List<String> soorten(List<Object[]> gemaakt) {
        return gemaakt.stream().map(TmsCreatiestratumWitnessTest::soort).toList();
    }

    private static String soort(Object[] g) {
        return g[1].getClass().getSimpleName() + " in fase " + g[2];
    }

    private static void zetRol(Object boon, String rol, Object doel) throws Exception {
        for (Method m : boon.getClass().getMethods())
            if (m.getName().equals(dev.uchar.regelspraak.transpiler.kern.Namen.setter(rol)) && m.getParameterCount() == 1) {
                m.invoke(boon, doel);
                return;
            }
        throw new AssertionError("geen setter voor " + rol + " op " + boon.getClass());
    }

    private static void assertGetal(String verwacht, Object werkelijk, String wat) {
        assertTrue(werkelijk instanceof BigDecimal w && new BigDecimal(verwacht).compareTo(w) == 0,
                () -> wat + ": verwacht " + verwacht + ", werkelijk " + werkelijk);
    }
}
