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
 *   <li>Every creation of a generation reads the same settled world: an object creation does not count the link a
 *       fact creation of its own generation lays ({@link #de_creaties_van_een_generatie_lezen_een_wereld}).</li>
 *   <li>The budget of 100 generations holds per stratum, as the reference interpreter counts its generations per
 *       stratum: two chains of 51 generations in two strata play ({@link #het_generatiebudget_geldt_per_stratum}), and
 *       so does a chain over three strata, 50 + 1 + 50 ({@link #een_keten_over_drie_strata_telt_per_stratum}).</li>
 *   <li>The budget counts the generations that make an object or lay a link (RUNTIME-SEMANTICS ch. 5.5): one chain
 *       plays 99 and 100 generations and ends without a fixpoint at a 101st
 *       ({@link #de_honderdste_generatie_speelt_en_een_101e_is_een_fout}); a chain of 100 generations plays with a rule
 *       of its stratum that gives the last object a value after the hundredth generation
 *       ({@link #een_generatie_waarin_alleen_de_regellaag_verandert_telt_niet_mee_voor_het_budget}), and so does the
 *       chain one shorter ({@link #een_keten_die_een_pas_korter_is_speelt}), the chain without the rule
 *       ({@link #dezelfde_keten_zonder_de_berekende_eigenschap_speelt}) and the chain with a rule of a higher stratum
 *       that reacts to the last object ({@link #een_reactie_uit_een_hoger_stratum_telt_niet_mee}).</li>
 * </ul>
 *
 * <p><b>The other executors</b> (measured 2026-10-09, cards creatie-initialisatie-leest-groeiende-waarde and
 * creatiebudget-ref-en-clingo). Every document written here passes the verifier, and the reference interpreter and the
 * ASP backend give the expectations of each case in every order of its creations and rules in the document, apart from
 * two. On the chain of document G that a 101st generation would extend both fault, the reference interpreter with "no
 * fixpoint within 100 generations — generation 101 still mints or links (object creation "werf de lange keten") …" and
 * the ASP backend with "UNSATISFIABLE — a fail-fast guard fired: viol("generation budget exhausted", …)". Document V the
 * reference interpreter plays and the ASP backend refuses by name: "the initialisation of beginstand reads
 * rel:maatschap, which fact creation "maak maten" writes on a dependency cycle through the creation …". Until
 * 2026-10-09 the two counted their budgets apart (measured 2026-10-08): on the chain of document L whose last Boei gets
 * its kopie both faulted, the reference interpreter with "no fixpoint within 100 passes — divergence is a fault
 * (ch. 5.7)", and on the chains of L to B101 without the kopie and with "genummerd" the reference interpreter played and
 * the ASP backend faulted with "generation budget exhausted": its budget was the depth of a chain of mints, which reached
 * 100 at the hundredth mint.
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
            // one phase per generation: the Penning in stratum 0's first generation; stratum 0 closes with a generation
            // that makes nothing (phase 2), and the Afrekening follows in the first generation of stratum 1
            assertEquals(List.of("Penning in fase 1", "Afrekening in fase 3"), soorten(objecten), () -> "volgorde " + namen);
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
            assertEquals(List.of("Penning in fase 1"), soorten(objecten), () -> "volgorde " + namen);
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
            // one phase per generation: stratum 0 makes the Penning (phase 1) and closes in phase 2; stratum 1 links it
            // in its first generation (3) and closes in 4; stratum 2 makes the Afrekening in its first generation (5)
            assertEquals(List.of("Penning in fase 1", "Afrekening in fase 5"), soorten(objecten), () -> "volgorde " + namen);
            assertEquals(1, links.size(), () -> "links in volgorde " + namen);
            assertTrue(links.get(0)[0] == j1 && links.get(0)[1] == objecten.get(0)[1], () -> "link in volgorde " + namen);
            assertEquals(3, links.get(0)[2], () -> "fase van de link in volgorde " + namen);
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
            // both of stratum 1 in its first generation, phase 3
            assertEquals(List.of("Afrekening in fase 3", "Kwitantie in fase 3", "Penning in fase 1"), soorten,
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
    static final String LEG = leg(52);

    /** The chain of Boeien while the nummer is below {@code grens}: from B1, {@code grens - 1} generations. */
    static String leg(int grens) {
        return objectcreatie("leg de volgende boei", "Boei", "Boei",
                List.of(init("nummer", binair("plus", lezing("nummer", "Boei"), getal("1"))),
                        init("wachterrang", padlezing("rang", "Wachter", "bewaking", "wachter"))),
                vergelijking("less", lezing("nummer", "Boei"), getal(String.valueOf(grens))), "boeiketen", "volgende boei");
    }

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

    /** Two chains of 51 generations each, 102 in all, in two strata. The reference interpreter counts at most 100
     *  generations per stratum and plays the document; a budget over the whole run would end it at generation 101.
     *  The Boeien wait for the Wachters, so the first Boei is made after the 51 generations of stratum 0 and the one
     *  that closes it, one phase each: in phase 52 + 1. */
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
                assertEquals("Wachter in fase " + (i + 1), soort(objecten.get(i)), "volgorde " + namen);
                assertEquals("Boei in fase " + (52 + i + 1), soort(objecten.get(51 + i)), "volgorde " + namen);
            }
            assertGetal("52", TmsDocumenten.lees(objecten.get(50)[1], "rang"), "rang van W52 in volgorde " + namen);
            assertGetal("52", TmsDocumenten.lees(objecten.get(101)[1], "nummer"), "nummer van B52 in volgorde " + namen);
            assertGetal("1", TmsDocumenten.lees(objecten.get(51)[1], "wachterrang"), "wachterrang van B2 in volgorde "
                    + namen);
        }
    }

    // ---------------- document L: a generation after which only the rule layer changes ----------------

    private static final String MODEL_L = TmsDocumenten.model(List.of(
            objecttype("Wachter", List.of(attribuut("rang", GEHEEL)), List.of()),
            objecttype("Boei", List.of(attribuut("nummer", GEHEEL), attribuut("wachterrang", GEHEEL),
                    attribuut("kopie", GEHEEL)), List.of("genummerd"))),
            List.of(feittype("opvolging", "voorganger", "Wachter", "opvolger", "Wachter"),
                    feittype("boeiketen", "vorige boei", "Boei", "volgende boei", "Boei"),
                    feittype("bewaking", "wachter", "Wachter", "boei", "Boei")));

    /** "De kopie van een Boei moet gesteld worden op zijn nummer": it reads what the chain of Boeien writes, positively,
     *  so it sits in the stratum of that chain. */
    static final String KOPIE = rekenregel("kopie van een boei", "GELIJKSTELLING", "Boei", "kopie",
            lezing("nummer", "Boei"), "null");

    /** "Een Boei is genummerd indien zijn nummer groter is dan 0": a characteristic attribution reads strictly, so it
     *  sits a stratum above the chain of Boeien. */
    static final String GENUMMERD = TmsDocumenten.kenmerkregel("genummerd", "Boei", "genummerd",
            vergelijking("greater", lezing("nummer", "Boei"), getal("0")));

    /** Document B with the chain of Boeien running while the nummer is below {@code grens}, beside {@code regels}. */
    static Path documentL(int grens, List<String> creaties, List<String> regels) throws Exception {
        List<String> c = new ArrayList<>();
        for (String creatie : creaties) c.add(creatie.equals(LEG) ? leg(grens) : creatie);
        return document(MODEL_L, regels, List.of(), c, List.of(),
                List.of(regelgroep("estafette", List.of("werf de opvolger")),
                        regelgroep("boeienlijn", List.of("leg de volgende boei"))));
    }

    static JsonNode zaakL(List<String> verwachtingen) throws Exception {
        List<String> v = new ArrayList<>(List.of(waarde("W1", "rang", "1")));
        v.addAll(verwachtingen);
        return TmsDocumenten.zaak(List.of(
                instantieMetRollen("W1", "Wachter", Map.of("rang", "1"), List.of(), Map.of("boei", List.of("B1"))),
                instantieMetRollen("B1", "Boei", Map.of("nummer", "1"), List.of(), Map.of())),
                v);
    }

    /** The scenario of the lens on the budget: the chain of Boeien runs to B101, 100 generations of stratum 1, and
     *  "kopie van een boei" writes the kopie of B101 after the hundredth generation, while the rule layer comes to rest.
     *  The budget counts generations that make an object or lay a link (RUNTIME-SEMANTICS ch. 5.5), so the document
     *  plays, as in the reference interpreter and the ASP backend. Until 2026-10-09 the driver counted the passes of
     *  the reference interpreter of then, among them a pass in which only the rule layer changed, and this document
     *  ended without a fixpoint: "geen fixpunt binnen 100 generaties: generatie 101 maakt niets meer, maar de regellaag
     *  verandert nog 1 waarde, kenmerk of link na wat generatie 100 maakte". */
    @Test void een_generatie_waarin_alleen_de_regellaag_verandert_telt_niet_mee_voor_het_budget() throws Exception {
        speeltInElkeVolgorde(List.of(WERF, LEG), List.of(KOPIE), (c, r) -> documentL(101, c, r),
                zaakL(List.of(waarde("B1", "kopie", "1"))));
        speeltDoorDeDriver(documentL(101, List.of(WERF, LEG), List.of(KOPIE)), 51 + 100);
    }

    /** The same chain one shorter, to B100, 99 generations of stratum 1. */
    @Test void een_keten_die_een_pas_korter_is_speelt() throws Exception {
        speeltInElkeVolgorde(List.of(WERF, LEG), List.of(KOPIE), (c, r) -> documentL(100, c, r),
                zaakL(List.of(waarde("B1", "kopie", "1"))));
        speeltDoorDeDriver(documentL(100, List.of(WERF, LEG), List.of(KOPIE)), 51 + 99);
    }

    /** The chain to B101 without "kopie van een boei", 100 generations of stratum 1. Until 2026-10-09 it already
     *  played: the reference interpreter's 101st pass changed nothing, since a new object without a written value was no
     *  change for it, and the driver did not count the standings and contributions the rule layer inserts for B101. */
    @Test void dezelfde_keten_zonder_de_berekende_eigenschap_speelt() throws Exception {
        speeltInElkeVolgorde(List.of(WERF, LEG), List.of(), (c, r) -> documentL(101, c, r), zaakL(List.of()));
        speeltDoorDeDriver(documentL(101, List.of(WERF, LEG), List.of()), 51 + 100);
    }

    /** The chain to B101 with "genummerd", which the rule layer attributes to B101 after the hundredth generation, from a
     *  stratum above the chain. It counts in no stratum, as no change of the rule layer counts. Until 2026-10-09 it
     *  already played: the driver counted a change of the rule layer only in the stratum of its writer and below, and
     *  the reference interpreter evaluated the rule only after the chain's fixpoint. */
    @Test void een_reactie_uit_een_hoger_stratum_telt_niet_mee() throws Exception {
        speeltInElkeVolgorde(List.of(WERF, LEG), List.of(GENUMMERD), (c, r) -> documentL(101, c, r),
                zaakL(List.of(TmsDocumenten.verwacht("B1", "genummerd", true))));
        speeltDoorDeDriver(documentL(101, List.of(WERF, LEG), List.of(GENUMMERD)), 51 + 100);
    }

    // ---------------- document G: the budget of one chain, at its bound ----------------

    private static final String MODEL_G = TmsDocumenten.model(List.of(
            objecttype("Wachter", List.of(attribuut("rang", GEHEEL)), List.of())),
            List.of(feittype("opvolging", "voorganger", "Wachter", "opvolger", "Wachter")));

    /** The chain of {@code corpus/rood} {@code finite-chain-over-budget} without its consistency, while the rang is below
     *  {@code grens}: from W1 of rang 1, {@code grens - 1} generations in one stratum. */
    static Path documentG(int grens) throws Exception {
        return document(MODEL_G, List.of(), List.of(),
                List.of(objectcreatie("werf de lange keten", "Wachter", "Wachter",
                        List.of(init("rang", binair("plus", lezing("rang", "Wachter"), getal("1")))),
                        vergelijking("less", lezing("rang", "Wachter"), getal(String.valueOf(grens))), "opvolging",
                        "opvolger")),
                List.of(), List.of(regelgroep("lange estafette", List.of("werf de lange keten"))));
    }

    private static final String BUDGET = "geen fixpunt binnen 100 generaties: generatie 101 maakt nog 1 object of link —"
            + " een creatie die niet stopt is een fout (RUNTIME-SEMANTICS ch. 5.5 en 5.7)";

    /** The bound of the generation budget (RUNTIME-SEMANTICS ch. 5.5), where the reference interpreter and the ASP backend
     *  put it: chains of 99 and 100 generations play, one generation per phase, and a 101st generation that still makes
     *  an object ends the run without a fixpoint, the 101st object made. */
    @Test void de_honderdste_generatie_speelt_en_een_101e_is_een_fout() throws Exception {
        JsonNode zaak = TmsDocumenten.zaak(List.of(instantieMetRollen("W1", "Wachter", Map.of("rang", "1"), List.of(),
                Map.of())), List.of(waarde("W1", "rang", "1")));
        for (int grens : List.of(100, 101, 102)) {
            int generaties = grens - 1;
            assertEquals(generaties <= 100 ? "gespeeld" : "case: " + BUDGET, TmsFoutWitnessTest.speel(documentG(grens),
                    zaak, DroolsBackend.Branch.TRUTH_MAINTENANCE), generaties + " generaties");
            DroolsBackend.Build b = DroolsBackend.build(documentG(grens), DroolsBackend.Branch.TRUTH_MAINTENANCE);
            for (List<Rule> volgorde : steekproef(b.tmsTranspiler().model().getRules(), 20261009L, 10)) {
                List<String> namen = volgorde.stream().map(Rule::getName).toList();
                Object w1 = TmsDocumenten.boon(b.beanLoader(), "Wachter", Map.of("rang", "1"), List.of());
                List<Object[]> objecten = new ArrayList<>();
                assertEquals(generaties <= 100 ? null : BUDGET, vuur(b, volgorde, List.of(w1), sessie -> { },
                        sessie -> objecten.addAll(gemaakt(sessie))), () -> generaties + " generaties in volgorde " + namen);
                assertEquals(Math.min(generaties, 101), objecten.size(),
                        () -> "objecten na " + generaties + " generaties in volgorde " + namen);
                for (int i = 0; i < objecten.size(); i++)
                    assertEquals("Wachter in fase " + (i + 1), soort(objecten.get(i)), "volgorde " + namen);
                if (generaties <= 100)
                    assertGetal(String.valueOf(grens), TmsDocumenten.lees(objecten.get(generaties - 1)[1], "rang"),
                            "rang van de laatste Wachter na " + generaties + " generaties in volgorde " + namen);
            }
        }
    }

    // ---------------- document D: a chain over three strata ----------------

    /** "Een Wachter heeft een boei met nummer gelijk aan 1 indien zijn rang gelijk is aan 51": it reads the rang that
     *  "werf de opvolger" writes, strictly, so it sits a stratum above that chain. */
    static final String BOEI_BIJ_51 = objectcreatie("leg een boei", "Wachter", "Boei", List.of(init("nummer", getal("1"))),
            vergelijking("equal", lezing("rang", "Wachter"), getal("51")), "bewaking", "boei");

    /** "werf de opvolger" while the rang is below 51, "leg een boei" and the chain of Boeien while the nummer is below 51,
     *  without a wachterrang: 50 generations in stratum 0, one in stratum 1 and 50 in stratum 2. */
    static Path documentD(List<String> creaties) throws Exception {
        List<String> c = new ArrayList<>();
        for (String creatie : creaties)
            c.add(creatie.equals(WERF) ? objectcreatie("werf de opvolger", "Wachter", "Wachter",
                    List.of(init("rang", binair("plus", lezing("rang", "Wachter"), getal("1")))),
                    vergelijking("less", lezing("rang", "Wachter"), getal("51")), "opvolging", "opvolger")
                    : creatie.equals(LEG) ? objectcreatie("leg de volgende boei", "Boei", "Boei",
                    List.of(init("nummer", binair("plus", lezing("nummer", "Boei"), getal("1")))),
                    vergelijking("less", lezing("nummer", "Boei"), getal("51")), "boeiketen", "volgende boei")
                    : creatie);
        return document(MODEL_B, List.of(), List.of(), c, List.of(),
                List.of(regelgroep("estafette", List.of("werf de opvolger")),
                        regelgroep("boeienlijn", List.of("leg de volgende boei"))));
    }

    /** The chain the ASP backend counted over the strata until 2026-10-09 (50 + 1 + 50 mints, UNSAT at the 100th), and
     *  that the budget per stratum of RUNTIME-SEMANTICS ch. 5.5 admits: it plays in every order of its creations. The
     *  phases: stratum 0 makes W2 to W51 in phases 1 to 50 and closes in 51, stratum 1 makes B1 in 52 and closes in 53,
     *  stratum 2 makes B2 to B51 in 54 to 103. */
    @Test void een_keten_over_drie_strata_telt_per_stratum() throws Exception {
        JsonNode zaak = TmsDocumenten.zaak(List.of(instantieMetRollen("W1", "Wachter", Map.of("rang", "1"), List.of(),
                Map.of())), List.of(waarde("W1", "rang", "1")));
        speeltInElkeVolgorde(List.of(WERF, BOEI_BIJ_51, LEG), List.of(), (creaties, regels) -> documentD(creaties), zaak);
        DroolsBackend.Build b = DroolsBackend.build(documentD(List.of(WERF, BOEI_BIJ_51, LEG)),
                DroolsBackend.Branch.TRUTH_MAINTENANCE);
        for (List<Rule> volgorde : steekproef(b.tmsTranspiler().model().getRules(), 20261009L, 10)) {
            List<String> namen = volgorde.stream().map(Rule::getName).toList();
            Object w1 = TmsDocumenten.boon(b.beanLoader(), "Wachter", Map.of("rang", "1"), List.of());
            List<Object[]> objecten = new ArrayList<>();
            assertNull(vuur(b, volgorde, List.of(w1), sessie -> { }, sessie -> objecten.addAll(gemaakt(sessie))),
                    () -> "volgorde " + namen);
            assertEquals(50 + 1 + 50, objecten.size(), () -> "objecten in volgorde " + namen);
            for (int i = 0; i < 50; i++) {
                assertEquals("Wachter in fase " + (i + 1), soort(objecten.get(i)), "volgorde " + namen);
                assertEquals("Boei in fase " + (54 + i), soort(objecten.get(51 + i)), "volgorde " + namen);
            }
            assertEquals("Boei in fase 52", soort(objecten.get(50)), "volgorde " + namen);
            assertGetal("51", TmsDocumenten.lees(objecten.get(49)[1], "rang"), "rang van W51 in volgorde " + namen);
            assertGetal("51", TmsDocumenten.lees(objecten.get(100)[1], "nummer"), "nummer van B51 in volgorde " + namen);
        }
    }

    // ---------------- document V: the creations of one generation read one world ----------------

    private static final String MAATGEVERS = "{\"node\":\"AttributeReference\",\"attribute\":\"maatgevers\","
            + "\"subjectType\":\"Wachter\",\"source\":{\"snippet\":\"maatgevers van de Wachter\"}}";

    /** "Eén voorganger heeft één opvolger" and "Een maatgever heeft meerdere maten, een maat meerdere maatgevers". */
    private static final String MODEL_V = TmsDocumenten.model(List.of(
            objecttype("Wachter", List.of(attribuut("rang", GEHEEL), attribuut("beginstand", GEHEEL),
                    attribuut("opvolgerbegin", GEHEEL), attribuut("verder", GEHEEL)), List.of())),
            List.of(feittype("opvolging", "voorganger", "Wachter", "opvolger", "Wachter"),
                    "{\"node\":\"FactType\",\"name\":\"maatschap\",\"reciprocal\":false,\"roles\":[{\"node\":\"Role\","
                            + "\"name\":\"maatgever\",\"objectType\":\"Wachter\",\"plural\":\"maatgevers\",\"many\":true},"
                            + "{\"node\":\"Role\",\"name\":\"maat\",\"objectType\":\"Wachter\",\"plural\":\"maten\","
                            + "\"many\":true}]}"));

    static final List<String> REGELS_V = List.of(
            rekenregel("opvolgerbegin", "GELIJKSTELLING", "Wachter", "opvolgerbegin",
                    padlezing("beginstand", "Wachter", "opvolging", "opvolger"), "null"),
            rekenregel("verder", "GELIJKSTELLING", "Wachter", "verder",
                    padlezing("opvolgerbegin", "Wachter", "opvolging", "opvolger"), "null"));

    /** Vorm V2 of the card creatie-initialisatie-leest-groeiende-waarde, the recursive group "estafette" of two creations
     *  on one cycle, so in one stratum: "werf de opvolger" gives a Wachter of rang below 4 an opvolger with rang plus 1
     *  and beginstand the number of its maatgevers, and "maak maten" makes every Wachter a maatgever of its opvolger.
     *  "opvolgerbegin" reads the beginstand of the opvolger and "verder" the opvolgerbegin of the opvolger. */
    static Path documentV(List<String> regels) throws Exception {
        String werf = objectcreatie("werf de opvolger", "Wachter", "Wachter", List.of(
                init("rang", binair("plus", lezing("rang", "Wachter"), getal("1"))),
                init("beginstand", aggregatie("count", MAATGEVERS, "Wachter", bronrol("maatschap", "maatgever"), "null",
                        "null", TELLING, "het aantal maatgevers van de Wachter"))),
                vergelijking("less", lezing("rang", "Wachter"), getal("4")), "opvolging", "opvolger");
        String maten = feitcreatie("maak maten", "maatschap", "maatgever", "maat", "Wachter",
                List.of(rolbinding("opvolging", "opvolger")), "null");
        return document(MODEL_V, regels, List.of(), List.of(werf), List.of(maten),
                List.of(regelgroep("estafette", List.of("werf de opvolger", "maak maten"))));
    }

    /**
     * Every creation of a generation reads the settled world of the generation before (RUNTIME-SEMANTICS ch. 5.5,
     * Generations), so an object creation does not count a link a fact creation of its own generation lays. W1 makes W2
     * in generation 1; in generation 2 "maak maten" makes W1 a maatgever of W2 while W2 makes W3, on the world in which
     * W2 has no maatgever yet: W3 starts at beginstand 0, and so does W4. The reference interpreter gives
     * opvolgerbegin(W1) = 0 and verder(W1) = 0 in every order of the rules. One phase per generation: W2, W3 and W4 in
     * phases 1, 2 and 3, the maatschap links in phases 2, 3 and 4, and phase 5 makes nothing.
     */
    @Test void de_creaties_van_een_generatie_lezen_een_wereld() throws Exception {
        JsonNode zaak = TmsDocumenten.zaak(List.of(instantieMetRollen("W1", "Wachter", Map.of("rang", "1"), List.of(),
                Map.of())), List.of(waarde("W1", "opvolgerbegin", "0"), waarde("W1", "verder", "0")));
        speeltInElkeVolgorde(List.of(), REGELS_V, (creaties, regels) -> documentV(regels), zaak);
        DroolsBackend.Build b = DroolsBackend.build(documentV(REGELS_V), DroolsBackend.Branch.TRUTH_MAINTENANCE);
        for (List<Rule> volgorde : steekproef(b.tmsTranspiler().model().getRules(), 20261009L, 100)) {
            List<String> namen = volgorde.stream().map(Rule::getName).toList();
            Object w1 = TmsDocumenten.boon(b.beanLoader(), "Wachter", Map.of("rang", "1"), List.of());
            List<Object[]> objecten = new ArrayList<>(), links = new ArrayList<>();
            assertNull(vuur(b, volgorde, List.of(w1), sessie -> { }, sessie -> {
                objecten.addAll(gemaakt(sessie));
                links.addAll(koppelingen(sessie, "maatschap"));
            }), () -> "volgorde " + namen);
            assertGetal("0", TmsDocumenten.lees(w1, "opvolgerbegin"), "opvolgerbegin van W1 in volgorde " + namen);
            assertGetal("0", TmsDocumenten.lees(w1, "verder"), "verder van W1 in volgorde " + namen);
            assertEquals(List.of("Wachter in fase 1", "Wachter in fase 2", "Wachter in fase 3"), soorten(objecten),
                    () -> "volgorde " + namen);
            for (Object[] g : objecten)
                assertGetal("0", TmsDocumenten.lees(g[1], "beginstand"), "beginstand in volgorde " + namen);
            List<Integer> fasen = new ArrayList<>();
            for (Object[] l : links) fasen.add((Integer) l[2]);
            fasen.sort(null);
            assertEquals(List.of(2, 3, 4), fasen, () -> "fasen van de maatschap in volgorde " + namen);
        }
    }

    /** W1 of rang 1, watching B1 of nummer 1, as beans for the driver. */
    private static List<Object> boeienlijn(DroolsBackend.Build b) throws Exception {
        Object w1 = TmsDocumenten.boon(b.beanLoader(), "Wachter", Map.of("rang", "1"), List.of());
        Object b1 = TmsDocumenten.boon(b.beanLoader(), "Boei", Map.of("nummer", "1"), List.of());
        zetRol(w1, "boei", b1);
        zetRol(b1, "wachter", w1);
        return List.of(w1, b1);
    }

    /** Fires document {@code ir} on {@link #boeienlijn} through the driver in a sample of orders of its rules: each
     *  reaches its fixpoint with {@code objecten} objects made. */
    private static void speeltDoorDeDriver(Path ir, int objecten) throws Exception {
        DroolsBackend.Build b = DroolsBackend.build(ir, DroolsBackend.Branch.TRUTH_MAINTENANCE);
        for (List<Rule> volgorde : steekproef(b.tmsTranspiler().model().getRules(), 20261008L, 10)) {
            List<String> namen = volgorde.stream().map(Rule::getName).toList();
            List<Object[]> gemaakt = new ArrayList<>();
            assertNull(vuur(b, volgorde, boeienlijn(b), sessie -> { }, sessie -> gemaakt.addAll(gemaakt(sessie))),
                    () -> "volgorde " + namen);
            assertEquals(objecten, gemaakt.size(), () -> "objecten in volgorde " + namen);
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
