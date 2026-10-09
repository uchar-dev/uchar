package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;

import dev.uchar.regelspraak.transpiler.tms.Creatiefase;
import dev.uchar.regelspraak.transpiler.tms.Stand;
import dev.uchar.regelspraak.transpiler.tms.Waarde;

import org.drools.model.Rule;
import org.junit.jupiter.api.Test;
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

import static dev.uchar.regelspraak.cases.TmsDocumenten.GEHEEL;
import static dev.uchar.regelspraak.cases.TmsDocumenten.TELLING;
import static dev.uchar.regelspraak.cases.TmsDocumenten.aggregatie;
import static dev.uchar.regelspraak.cases.TmsDocumenten.attribuut;
import static dev.uchar.regelspraak.cases.TmsDocumenten.attribuutgroep;
import static dev.uchar.regelspraak.cases.TmsDocumenten.binair;
import static dev.uchar.regelspraak.cases.TmsDocumenten.bronrol;
import static dev.uchar.regelspraak.cases.TmsDocumenten.check;
import static dev.uchar.regelspraak.cases.TmsDocumenten.feittypeEenNaarVeel;
import static dev.uchar.regelspraak.cases.TmsDocumenten.getal;
import static dev.uchar.regelspraak.cases.TmsDocumenten.instantieMetRollen;
import static dev.uchar.regelspraak.cases.TmsDocumenten.lezing;
import static dev.uchar.regelspraak.cases.TmsDocumenten.padlezing;
import static dev.uchar.regelspraak.cases.TmsDocumenten.rekenregel;
import static dev.uchar.regelspraak.cases.TmsDocumenten.rolbinding;
import static dev.uchar.regelspraak.cases.TmsDocumenten.samengesteld;
import static dev.uchar.regelspraak.cases.TmsDocumenten.standaard;
import static dev.uchar.regelspraak.cases.TmsDocumenten.vergelijking;
import static dev.uchar.regelspraak.cases.TmsDocumenten.waarde;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Object and fact creation on the truth-maintenance branch (RUNTIME-SEMANTICS ch. 5.5): a creation fires only at a
 * fixpoint of the rule layer, what it makes is stated and never lapses, and the driver fires in generations
 * ({@code DroolsBackend.vuur}). Each document runs once through the replay and then in a sample of orders of the rules
 * the branch generates (the order of the model, its reverse and a hundred shuffles), through the driver.
 *
 * <ul>
 *   <li>A creation makes one object per subject, also when a later generation finds its condition true again, and its
 *       member initialisations are the values of the fixpoint at which it fires: the vangst a lower default gives first
 *       is a state the creation does not see ({@link #een_objectcreatie_maakt_een_object_per_onderwerp_met_de_waarden_van_het_fixpunt}).</li>
 *   <li>A creation whose condition holds only on an intermediate state does not happen: with a lower default that
 *       decides first ({@link #een_creatie_die_alleen_op_een_lagere_default_geldt_gebeurt_niet}); in the smallest
 *       shape the verifier admits where the condition reads its own output, where a logical object kept itself
 *       standing by its own count once the state that made it had passed
 *       ({@link #een_creatie_die_haar_eigen_uitvoer_leest_houdt_zichzelf_niet_in_stand}); and while the rule layer
 *       reacts to an object a creation made ({@link #een_creatie_vuurt_niet_terwijl_de_regellaag_op_een_creatie_reageert}),
 *       where with a condition read positively the creation does not even get a match on that state
 *       ({@link #een_objectcreatie_krijgt_na_een_creatiefase_geen_match_op_een_tussenstand}).</li>
 *   <li>A recursive chain that WF-045 admits stops where its condition no longer holds, an initialisation keeps the
 *       value it had when the object was made, and a navigation through the created relation reads the created link
 *       ({@link #een_recursieve_keten_stopt_waar_de_voorwaarde_niet_meer_geldt}).</li>
 *   <li>A fact creation makes each link once, beside a link of the input
 *       ({@link #een_feitcreatie_legt_elke_link_een_keer}), and a second distinct filler of a to-one role is a fault at
 *       the fixpoint ({@link #een_tweede_vulling_van_een_naar_een_rol_is_een_fout}).</li>
 *   <li>An object made in a phase is the subject of a creation only in a later phase, after the rule layer has reached
 *       a fixpoint over it, and a chain that does not stop meets the creation budget at its mint at depth 101
 *       ({@link #een_object_is_pas_in_de_volgende_generatie_onderwerp}); a fact creation walks a link only in a later
 *       phase than the one that made it ({@link #een_link_is_pas_in_de_volgende_generatie_pad}); the corpus pins the
 *       two sides of the budget ({@link #het_creatiebudget_ligt_waar_het_corpus_het_pint}).</li>
 *   <li>The three wrakboeking documents play ({@link #de_wrakboekingen_spelen}).</li>
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
 */
class TmsCreatieWitnessTest {

    private static final Path CORPUS = Path.of("..", "..", "corpus");

    // ---------------- document A: one object per subject, with the values of the fixpoint ----------------

    private static final String MELDINGEN = "{\"node\":\"AttributeReference\",\"attribute\":\"meldingen\",\"subjectType\":"
            + "\"Jutter\",\"source\":{\"snippet\":\"meldingen van de Jutter\"}}";

    private static final String MODEL_A = TmsDocumenten.model(List.of(
            TmsDocumenten.objecttype("Jutter", List.of(attribuut("klasse", GEHEEL), attribuut("vangst", GEHEEL),
                    attribuut("meldingaantal", GEHEEL), attribuut("meldingswaarde", GEHEEL)), List.of("gemeld")),
            TmsDocumenten.objecttype("Melding", List.of(attribuut("waarde", GEHEEL)), List.of())),
            List.of(feittypeEenNaarVeel("jutmelding", "melder", "Jutter", "melding", "Melding", "meldingen")));

    private static final List<String> REGELS_A = List.of(
            rekenregel("klasse van een jutter", "GELIJKSTELLING", "Jutter", "klasse", getal("1"), "null"),
            rekenregel("meldingaantal", "BEREKENING", "Jutter", "meldingaantal", aggregatie("count", MELDINGEN, "Melding",
                    bronrol("jutmelding", "melding"), "null", "null", TELLING, "het aantal meldingen van de Jutter"),
                    "null"),
            rekenregel("meldingswaarde", "BEREKENING", "Jutter", "meldingswaarde", aggregatie("sum",
                    lezing("waarde", "Melding"), "Melding", bronrol("jutmelding", "melding"), "null", getal("0"), GEHEEL,
                    "de som van de waarde van alle meldingen van de Jutter of 0 als die er niet zijn"), "null"));

    /** The vangst: 7 where the klasse, which a rule sets, is 1, and else 3 for a gemeld Jutter; the lower default can
     *  decide before the rule has set the klasse. */
    private static final String VANGST = attribuutgroep("vangst van een jutter", "Jutter", "vangst", List.of(
            standaard(vergelijking("equal", lezing("klasse", "Jutter"), getal("1")), getal("7")),
            standaard(check("gemeld", false), getal("3"))));

    static Path documentA(String voorwaarde) throws Exception {
        String naam = voorwaarde.equals("null") ? "meld de vangst" : "meld een lage vangst";
        return document(MODEL_A, REGELS_A, List.of(VANGST),
                List.of(objectcreatie(naam, "Jutter", "Melding", List.of(init("waarde", lezing("vangst", "Jutter"))),
                        voorwaarde, "jutmelding", "melding")), List.of(), List.of());
    }

    static JsonNode zaakA(String aantal, String som) throws Exception {
        return TmsDocumenten.zaak(List.of(instantieMetRollen("J1", "Jutter", Map.of(), List.of("gemeld"), Map.of())),
                List.of(waarde("J1", "meldingaantal", aantal), waarde("J1", "meldingswaarde", som)));
    }

    private static final String LAGE_VANGST = vergelijking("less", lezing("vangst", "Jutter"), getal("5"));

    @Test void een_objectcreatie_maakt_een_object_per_onderwerp_met_de_waarden_van_het_fixpunt() throws Exception {
        Path ir = documentA("null");
        assertEquals("gespeeld", TmsFoutWitnessTest.speel(ir, zaakA("1", "7"), DroolsBackend.Branch.TRUTH_MAINTENANCE));
        DroolsBackend.Build b = DroolsBackend.build(ir, DroolsBackend.Branch.TRUTH_MAINTENANCE);
        int tussenstand = 0;
        for (List<Rule> volgorde : TmsDocumenten.steekproef(b.tmsTranspiler().model().getRules(), 20261008L, 100)) {
            List<String> namen = volgorde.stream().map(Rule::getName).toList();
            Object j1 = TmsDocumenten.boon(b.beanLoader(), "Jutter", Map.of(), List.of("gemeld"));
            int[] lager = {0};
            List<Object[]> gemaakt = new ArrayList<>();
            assertNull(vuur(b, volgorde, List.of(j1), sessie -> sessie.addEventListener(waardeteller("vangst", "3", lager)),
                    sessie -> gemaakt.addAll(gemaakt(sessie))), () -> "volgorde " + namen);
            assertGetal("1", TmsDocumenten.lees(j1, "meldingaantal"), "meldingaantal in volgorde " + namen);
            assertGetal("7", TmsDocumenten.lees(j1, "meldingswaarde"), "meldingswaarde in volgorde " + namen);
            assertEquals(1, gemaakt.size(), () -> "objecten in volgorde " + namen);
            assertTrue(gemaakt.get(0)[0] == j1, () -> "onderwerp in volgorde " + namen);
            if (lager[0] > 0) tussenstand++;
        }
        assertTrue(tussenstand > 0, "geen volgorde liet de lagere default eerst beslissen");
    }

    @Test void een_creatie_die_alleen_op_een_lagere_default_geldt_gebeurt_niet() throws Exception {
        Path ir = documentA(LAGE_VANGST);
        assertEquals("gespeeld", TmsFoutWitnessTest.speel(ir, zaakA("0", "0"), DroolsBackend.Branch.TRUTH_MAINTENANCE));
        DroolsBackend.Build b = DroolsBackend.build(ir, DroolsBackend.Branch.TRUTH_MAINTENANCE);
        int tussenstand = 0;
        for (List<Rule> volgorde : TmsDocumenten.steekproef(b.tmsTranspiler().model().getRules(), 20261008L, 100)) {
            List<String> namen = volgorde.stream().map(Rule::getName).toList();
            Object j1 = TmsDocumenten.boon(b.beanLoader(), "Jutter", Map.of(), List.of("gemeld"));
            int[] lager = {0};
            List<Object[]> gemaakt = new ArrayList<>();
            assertNull(vuur(b, volgorde, List.of(j1), sessie -> sessie.addEventListener(waardeteller("vangst", "3", lager)),
                    sessie -> gemaakt.addAll(gemaakt(sessie))), () -> "volgorde " + namen);
            assertGetal("0", TmsDocumenten.lees(j1, "meldingaantal"), "meldingaantal in volgorde " + namen);
            assertEquals(0, gemaakt.size(), () -> "objecten in volgorde " + namen);
            if (lager[0] > 0) tussenstand++;
        }
        assertTrue(tussenstand > 0, "geen volgorde liet de lagere default eerst beslissen");
    }

    // ---------------- document B: a condition that reads its own output ----------------

    private static final String OPVOLGERS = "{\"node\":\"AttributeReference\",\"attribute\":\"opvolgers\",\"subjectType\":"
            + "\"Wachter\",\"source\":{\"snippet\":\"opvolgers van de Wachter\"}}";

    private static final String AANTAL_OPVOLGERS = aggregatie("count", OPVOLGERS, "Wachter",
            bronrol("opvolging", "opvolger"), "null", "null", TELLING, "het aantal opvolgers van de Wachter");

    /** The smallest shape of the advice on the branch (2026-10-08, part 1): a recursive creation whose condition holds
     *  where the rang is not 5, or where the Wachter has an opvolger already. At the fixpoint the rang is 5 and there
     *  is no opvolger; a lower default gives the rang 0 first in some orders. */
    static Path documentB() throws Exception {
        String model = TmsDocumenten.model(List.of(TmsDocumenten.objecttype("Wachter", List.of(attribuut("code", GEHEEL),
                attribuut("rang", GEHEEL), attribuut("opvolgeraantal", GEHEEL)), List.of("startwachter"))),
                List.of(feittypeEenNaarVeel("opvolging", "voorganger", "Wachter", "opvolger", "Wachter", "opvolgers")));
        List<String> regels = List.of(
                rekenregel("code van een wachter", "GELIJKSTELLING", "Wachter", "code", getal("1"), "null"),
                rekenregel("opvolgeraantal", "BEREKENING", "Wachter", "opvolgeraantal", AANTAL_OPVOLGERS, "null"));
        String rang = attribuutgroep("rang van een wachter", "Wachter", "rang", List.of(
                standaard(vergelijking("equal", lezing("code", "Wachter"), getal("1")), getal("5")),
                standaard(check("startwachter", false), getal("0"))));
        String voorwaarde = samengesteld("all", 0, List.of(check("startwachter", false), samengesteld("atLeast", 1,
                List.of(vergelijking("notEqual", lezing("rang", "Wachter"), getal("5")),
                        vergelijking("greater", AANTAL_OPVOLGERS, getal("0"))))));
        return document(model, regels, List.of(rang),
                List.of(objectcreatie("werf", "Wachter", "Wachter", List.of(), voorwaarde, "opvolging", "opvolger")),
                List.of(), List.of(regelgroep("estafette", List.of("werf"))));
    }

    static JsonNode zaakB() throws Exception {
        return TmsDocumenten.zaak(List.of(instantieMetRollen("W1", "Wachter", Map.of(), List.of("startwachter"), Map.of())),
                List.of(waarde("W1", "opvolgeraantal", "0")));
    }

    @Test void een_creatie_die_haar_eigen_uitvoer_leest_houdt_zichzelf_niet_in_stand() throws Exception {
        Path ir = documentB();
        assertEquals("gespeeld", TmsFoutWitnessTest.speel(ir, zaakB(), DroolsBackend.Branch.TRUTH_MAINTENANCE));
        DroolsBackend.Build b = DroolsBackend.build(ir, DroolsBackend.Branch.TRUTH_MAINTENANCE);
        int tussenstand = 0;
        for (List<Rule> volgorde : TmsDocumenten.steekproef(b.tmsTranspiler().model().getRules(), 20261008L, 100)) {
            List<String> namen = volgorde.stream().map(Rule::getName).toList();
            Object w1 = TmsDocumenten.boon(b.beanLoader(), "Wachter", Map.of(), List.of("startwachter"));
            int[] lager = {0};
            List<Object[]> gemaakt = new ArrayList<>();
            assertNull(vuur(b, volgorde, List.of(w1), sessie -> sessie.addEventListener(waardeteller("rang", "0", lager)),
                    sessie -> gemaakt.addAll(gemaakt(sessie))), () -> "volgorde " + namen);
            assertGetal("0", TmsDocumenten.lees(w1, "opvolgeraantal"), "opvolgeraantal in volgorde " + namen);
            assertGetal("5", TmsDocumenten.lees(w1, "rang"), "rang in volgorde " + namen);
            assertEquals(0, gemaakt.size(), () -> "objecten in volgorde " + namen);
            if (lager[0] > 0) tussenstand++;
        }
        assertTrue(tussenstand > 0, "geen volgorde liet de lagere default eerst beslissen");
    }

    // ---------------- document G: the rule layer after a creation ----------------

    private static final String WAARSCHUWINGEN = "{\"node\":\"AttributeReference\",\"attribute\":\"waarschuwingen\","
            + "\"subjectType\":\"Jutter\",\"source\":{\"snippet\":\"waarschuwingen van de Jutter\"}}";

    /** "meld" makes one Melding per Jutter; the aantal counts the meldingen and the kopie copies the aantal, so the two
     *  differ only between those two rules, when the Melding has arrived; "waarschuw" makes a Waarschuwing where they
     *  differ. At every fixpoint of the rule layer they are equal. */
    static Path documentG() throws Exception {
        return waarschuwdocument(vergelijking("notEqual", lezing("aantal", "Jutter"), lezing("kopie", "Jutter")));
    }

    /** The model and rules of G, with {@code voorwaarde} as the condition of "waarschuw". */
    private static Path waarschuwdocument(String voorwaarde) throws Exception {
        String model = TmsDocumenten.model(List.of(
                TmsDocumenten.objecttype("Jutter", List.of(attribuut("aantal", GEHEEL), attribuut("kopie", GEHEEL),
                        attribuut("waarschuwingaantal", GEHEEL)), List.of()),
                TmsDocumenten.objecttype("Melding", List.of(), List.of()),
                TmsDocumenten.objecttype("Waarschuwing", List.of(), List.of())), List.of(
                feittypeEenNaarVeel("jutmelding", "melder", "Jutter", "melding", "Melding", "meldingen"),
                feittypeEenNaarVeel("jutwaarschuwing", "gewaarschuwde", "Jutter", "waarschuwing", "Waarschuwing",
                        "waarschuwingen")));
        List<String> regels = List.of(
                rekenregel("aantal", "BEREKENING", "Jutter", "aantal", aggregatie("count", MELDINGEN, "Melding",
                        bronrol("jutmelding", "melding"), "null", "null", TELLING, "het aantal meldingen van de Jutter"),
                        "null"),
                rekenregel("kopie", "GELIJKSTELLING", "Jutter", "kopie", lezing("aantal", "Jutter"), "null"),
                rekenregel("waarschuwingaantal", "BEREKENING", "Jutter", "waarschuwingaantal", aggregatie("count",
                        WAARSCHUWINGEN, "Waarschuwing", bronrol("jutwaarschuwing", "waarschuwing"), "null", "null",
                        TELLING, "het aantal waarschuwingen van de Jutter"), "null"));
        return document(model, regels, List.of(), List.of(
                objectcreatie("meld", "Jutter", "Melding", List.of(), "null", "jutmelding", "melding"),
                objectcreatie("waarschuw", "Jutter", "Waarschuwing", List.of(), voorwaarde, "jutwaarschuwing",
                        "waarschuwing")), List.of(), List.of());
    }

    static JsonNode zaakG() throws Exception {
        return TmsDocumenten.zaak(List.of(instantieMetRollen("J1", "Jutter", Map.of(), List.of(), Map.of())),
                List.of(waarde("J1", "aantal", "1"), waarde("J1", "kopie", "1"), waarde("J1", "waarschuwingaantal", "0")));
    }

    @Test void een_creatie_vuurt_niet_terwijl_de_regellaag_op_een_creatie_reageert() throws Exception {
        Path ir = documentG();
        assertEquals("gespeeld", TmsFoutWitnessTest.speel(ir, zaakG(), DroolsBackend.Branch.TRUTH_MAINTENANCE));
        DroolsBackend.Build b = DroolsBackend.build(ir, DroolsBackend.Branch.TRUTH_MAINTENANCE);
        int tussenstand = 0;
        for (List<Rule> volgorde : TmsDocumenten.steekproef(b.tmsTranspiler().model().getRules(), 20261008L, 100)) {
            List<String> namen = volgorde.stream().map(Rule::getName).toList();
            Object j1 = TmsDocumenten.boon(b.beanLoader(), "Jutter", Map.of(), List.of());
            List<String> volgordeVanWaarden = new ArrayList<>();
            List<Object[]> gemaakt = new ArrayList<>();
            assertNull(vuur(b, volgorde, List.of(j1), sessie -> sessie.addEventListener(
                    new DefaultRuleRuntimeEventListener() {
                        @Override public void objectInserted(ObjectInsertedEvent e) {
                            if (e.getObject() instanceof Waarde w && w.waarde() instanceof BigDecimal x
                                    && x.compareTo(BigDecimal.ONE) == 0)
                                volgordeVanWaarden.add(w.attribuut().naam().toLowerCase(java.util.Locale.ROOT));
                        }
                    }), sessie -> gemaakt.addAll(gemaakt(sessie))), () -> "volgorde " + namen);
            assertGetal("0", TmsDocumenten.lees(j1, "waarschuwingaantal"), "waarschuwingaantal in volgorde " + namen);
            assertEquals(1, gemaakt.size(), () -> "objecten in volgorde " + namen);
            // the aantal became 1 while the kopie was still 0: the state on which "waarschuw" would hold
            if (volgordeVanWaarden.indexOf("aantal") >= 0 && volgordeVanWaarden.indexOf("aantal")
                    < volgordeVanWaarden.indexOf("kopie")) tussenstand++;
        }
        assertTrue(tussenstand > 0, "geen volgorde liet het aantal voor de kopie veranderen");
    }

    // ---------------- document H: the rule layer after a creation phase, read positively ----------------

    /** G with the condition of "waarschuw" read positively, "het aantal van de Jutter is groter dan de kopie van de
     *  Jutter": a comparison with a leeg operand does not hold (RUNTIME-SEMANTICS ch. 1.2), so it holds only where the
     *  aantal has grown past a kopie that is there, and the expected answer rests on no condition that holds on absence
     *  (ch. 6.1). At every fixpoint of the rule layer the two are equal, and the case expects G's answer. */
    static Path documentH() throws Exception {
        return waarschuwdocument(vergelijking("greater", lezing("aantal", "Jutter"), lezing("kopie", "Jutter")));
    }

    /**
     * After a creation phase, while its {@code Creatiefase} stands, the rule layer passes a state on which "waarschuw"
     * holds and which no fixpoint has: "meld" has made the Melding, the aantal standing is 1 and the kopie standing still
     * 0. Measured per order, over working memory: the standings "waarschuw" reads, and every match Drools creates for
     * "waarschuw". In every order the state stood with a {@code Creatiefase} in the session, no Waarschuwing is made, and
     * "waarschuw" does not even get a match: Drools 8.44 evaluates a rule lazily, when the agenda takes it from the group
     * that has the focus, and the object creations' group has the focus only when the driver gives it, at a fixpoint of
     * the rule layer ({@code DroolsBackend.vuur}).
     *
     * <p>This is the witness for auto-focus on the object creations, which G does not see. Auto-focus makes a rule eager
     * ({@code RuleImpl.setAutoFocus} sets the eager flag) and gives its group the focus where an evaluation creates a
     * match ({@code PhreakRuleTerminalNode.doLeftInserts}): "waarschuw" gets a match on the state above, in every order,
     * and this test fails on it. Measured on this document, the rule layer then fires on, the copy of the aantal included,
     * while the group has the focus, and the match is cancelled before it fires, so the mutant makes no Waarschuwing.
     * The answer it does change is the phase: an object creation that holds when a generation opens fires in the phase
     * of the fact creations, on which {@link #een_recursieve_keten_stopt_waar_de_voorwaarde_niet_meer_geldt},
     * {@link #een_object_is_pas_in_de_volgende_generatie_onderwerp} and
     * {@link #het_creatiebudget_ligt_waar_het_corpus_het_pint} fail.
     *
     * <p>Measured again on 2026-10-09. Since the creations fire per stratum (2026-10-08), "waarschuw" sits a stratum above
     * "meld" and binds no {@code Creatiefase} of meld's stratum, so it gets no match on the state above under the mutant
     * either, and this test passes on it; on {@code e15baf01} only the three tests named above failed in this class, on
     * the phase. Since a generation is one phase (2026-10-09) there is no phase of the fact creations to fire in, and
     * under the mutant every test of this class passes. The witness for it is now document R of
     * {@code TmsCreatiestratumWitnessTest}
     * ({@code een_objectcreatie_die_de_regellaag_mogelijk_maakt_vuurt_in_de_volgende_generatie}): an object creation
     * whose condition holds once the rule layer has settled, and stays true, fires there under the mutant in the
     * generation before its own, and the next creation of the chain reads what it made.
     */
    @Test void een_objectcreatie_krijgt_na_een_creatiefase_geen_match_op_een_tussenstand() throws Exception {
        Path ir = documentH();
        assertEquals("gespeeld", TmsFoutWitnessTest.speel(ir, zaakG(), DroolsBackend.Branch.TRUTH_MAINTENANCE));
        DroolsBackend.Build b = DroolsBackend.build(ir, DroolsBackend.Branch.TRUTH_MAINTENANCE);
        assertTrue(b.tmsTranspiler().creatieregels().contains("waarschuw"), () -> "creatieregels "
                + b.tmsTranspiler().creatieregels());
        for (List<Rule> volgorde : TmsDocumenten.steekproef(b.tmsTranspiler().model().getRules(), 20261008L, 100)) {
            List<String> namen = volgorde.stream().map(Rule::getName).toList();
            Object j1 = TmsDocumenten.boon(b.beanLoader(), "Jutter", Map.of(), List.of());
            Tussenstand stand = new Tussenstand(j1);
            List<String> matches = new ArrayList<>();
            List<Object[]> gemaakt = new ArrayList<>();
            assertNull(vuur(b, volgorde, List.of(j1), sessie -> {
                sessie.addEventListener(stand);
                sessie.addEventListener(new DefaultAgendaEventListener() {
                    @Override public void matchCreated(MatchCreatedEvent e) {
                        if (e.getMatch().getRule().getName().equals("waarschuw")) matches.add(stand.standen.toString());
                    }
                });
            }, sessie -> gemaakt.addAll(gemaakt(sessie))), () -> "volgorde " + namen);
            assertEquals(1, gemaakt.size(), () -> "objecten in volgorde " + namen);
            assertGetal("0", TmsDocumenten.lees(j1, "waarschuwingaantal"), "waarschuwingaantal in volgorde " + namen);
            assertGetal("1", TmsDocumenten.lees(j1, "kopie"), "kopie in volgorde " + namen);
            assertEquals(List.of(), matches, () -> "matches van waarschuw, met de standen, in volgorde " + namen);
            assertTrue(stand.gezien, () -> "geen aantal boven de kopie terwijl een Creatiefase stond, in volgorde " + namen);
        }
    }

    /** Follows the standings of {@code onderwerp} and the {@link Creatiefase} facts through working memory, and records
     *  whether the aantal standing stood above the kopie standing, both gevuld, while a {@code Creatiefase} did: the
     *  state on which "waarschuw" of document H holds. */
    private static final class Tussenstand extends DefaultRuleRuntimeEventListener {
        private final Object onderwerp;
        final Map<String, Object> standen = new java.util.TreeMap<>();
        private int fasen;
        boolean gezien;

        Tussenstand(Object onderwerp) { this.onderwerp = onderwerp; }

        @Override public void objectInserted(ObjectInsertedEvent e) {
            if (e.getObject() instanceof Creatiefase) fasen++;
            else if (e.getObject() instanceof Stand s && s.onderwerp() == onderwerp) standen.put(naam(s), s.waarde());
            kijk();
        }

        @Override public void objectDeleted(ObjectDeletedEvent e) {
            if (e.getOldObject() instanceof Creatiefase) fasen--;
            else if (e.getOldObject() instanceof Stand s && s.onderwerp() == onderwerp
                    && java.util.Objects.equals(standen.get(naam(s)), s.waarde())) standen.remove(naam(s));
            kijk();
        }

        private void kijk() {
            if (fasen > 0 && standen.get("aantal") instanceof BigDecimal a && standen.get("kopie") instanceof BigDecimal k
                    && a.compareTo(k) > 0) gezien = true;
        }

        private static String naam(Stand s) {
            return s.attribuut().naam().toLowerCase(java.util.Locale.ROOT);
        }
    }

    // ---------------- document C: a recursive chain that stops ----------------

    /** "Eén voorganger heeft één opvolger", the opvolger role with its plural, as a count over it spells its source. */
    private static final String EEN_OPVOLGER = "{\"node\":\"FactType\",\"name\":\"opvolging\",\"reciprocal\":false,"
            + "\"roles\":[{\"node\":\"Role\",\"name\":\"voorganger\",\"objectType\":\"Wachter\",\"many\":false},"
            + "{\"node\":\"Role\",\"name\":\"opvolger\",\"objectType\":\"Wachter\",\"plural\":\"opvolgers\","
            + "\"many\":false}]}";

    /** A chain of Wachters, each made by its voorganger while that one's rang is below 4, with the rang one higher and
     *  the beginstand the stand of the voorganger when it makes the opvolger; the stand counts the opvolgers, so the
     *  voorganger's stand becomes 1 right after, and opvolgerbegin reads the beginstand through the created link. */
    static Path documentC() throws Exception {
        String model = TmsDocumenten.model(List.of(TmsDocumenten.objecttype("Wachter", List.of(attribuut("rang", GEHEEL),
                attribuut("stand", GEHEEL), attribuut("beginstand", GEHEEL), attribuut("opvolgerbegin", GEHEEL)),
                List.of())), List.of(EEN_OPVOLGER));
        List<String> regels = List.of(
                rekenregel("stand van een wachter", "BEREKENING", "Wachter", "stand", AANTAL_OPVOLGERS, "null"),
                rekenregel("opvolgerbegin", "GELIJKSTELLING", "Wachter", "opvolgerbegin",
                        padlezing("beginstand", "Wachter", "opvolging", "opvolger"), "null"));
        String werf = objectcreatie("werf de opvolger", "Wachter", "Wachter", List.of(
                init("rang", binair("plus", lezing("rang", "Wachter"), getal("1"))),
                init("beginstand", lezing("stand", "Wachter"))),
                vergelijking("less", lezing("rang", "Wachter"), getal("4")), "opvolging", "opvolger");
        return document(model, regels, List.of(), List.of(werf), List.of(),
                List.of(regelgroep("estafette", List.of("werf de opvolger"))));
    }

    static JsonNode zaakC() throws Exception {
        return TmsDocumenten.zaak(List.of(instantieMetRollen("W1", "Wachter", Map.of("rang", "1"), List.of(), Map.of())),
                List.of(waarde("W1", "stand", "1"), waarde("W1", "opvolgerbegin", "0")));
    }

    @Test void een_recursieve_keten_stopt_waar_de_voorwaarde_niet_meer_geldt() throws Exception {
        Path ir = documentC();
        assertEquals("gespeeld", TmsFoutWitnessTest.speel(ir, zaakC(), DroolsBackend.Branch.TRUTH_MAINTENANCE));
        DroolsBackend.Build b = DroolsBackend.build(ir, DroolsBackend.Branch.TRUTH_MAINTENANCE);
        for (List<Rule> volgorde : TmsDocumenten.steekproef(b.tmsTranspiler().model().getRules(), 20261008L, 100)) {
            List<String> namen = volgorde.stream().map(Rule::getName).toList();
            Object w1 = TmsDocumenten.boon(b.beanLoader(), "Wachter", Map.of("rang", "1"), List.of());
            List<Object[]> gemaakt = new ArrayList<>();
            assertNull(vuur(b, volgorde, List.of(w1), sessie -> { }, sessie -> gemaakt.addAll(gemaakt(sessie))),
                    () -> "volgorde " + namen);
            assertGetal("1", TmsDocumenten.lees(w1, "stand"), "stand van W1 in volgorde " + namen);
            assertGetal("0", TmsDocumenten.lees(w1, "opvolgerbegin"), "opvolgerbegin van W1 in volgorde " + namen);
            // one phase per generation: W1 makes W2 in phase 1, W2 makes W3 in phase 2, W3 makes W4 in phase 3, and
            // W4, of rang 4, makes nothing
            assertEquals(3, gemaakt.size(), () -> "objecten in volgorde " + namen);
            Object onderwerp = w1;
            for (int i = 0; i < 3; i++) {
                Object[] g = gemaakt.get(i);
                int fase = i + 1;
                assertTrue(g[0] == onderwerp, () -> "keten in volgorde " + namen);
                assertEquals(fase, g[2], () -> "fase in volgorde " + namen);
                assertGetal(String.valueOf(i + 2), TmsDocumenten.lees(g[1], "rang"), "rang in volgorde " + namen);
                assertGetal("0", TmsDocumenten.lees(g[1], "beginstand"), "beginstand in volgorde " + namen);
                onderwerp = g[1];
            }
        }
    }

    // ---------------- document D: a fact creation ----------------

    /** "Een havenjutter van een Haven is een geworven jutter van de kustwacht van de Haven": each Jutter has at most
     *  one haven, so a Jutter that the kustwacht of two havens recruits gets two. */
    static Path documentD() throws Exception {
        String model = TmsDocumenten.model(List.of(
                TmsDocumenten.objecttype("Kustwacht", List.of(), List.of()),
                TmsDocumenten.objecttype("Haven", List.of(attribuut("jutteraantal", GEHEEL)), List.of()),
                TmsDocumenten.objecttype("Jutter", List.of(), List.of())), List.of(
                feittypeEenNaarVeel("bewaking", "kustwacht", "Kustwacht", "bewaakte haven", "Haven", "bewaakte havens"),
                feittypeEenNaarVeel("werving", "werver", "Kustwacht", "geworven jutter", "Jutter", "geworven jutters"),
                feittypeEenNaarVeel("havenlid", "haven", "Haven", "havenjutter", "Jutter", "havenjutters")));
        String havenjutters = "{\"node\":\"AttributeReference\",\"attribute\":\"havenjutters\",\"subjectType\":\"Haven\","
                + "\"source\":{\"snippet\":\"havenjutters van de Haven\"}}";
        List<String> regels = List.of(rekenregel("jutteraantal", "BEREKENING", "Haven", "jutteraantal",
                aggregatie("count", havenjutters, "Jutter", bronrol("havenlid", "havenjutter"), "null", "null", TELLING,
                        "het aantal havenjutters van de Haven"), "null"));
        String werving = feitcreatie("werving van havenjutters", "havenlid", "haven", "havenjutter", "Haven",
                List.of(rolbinding("bewaking", "kustwacht"), rolbinding("werving", "geworven jutter")), "null");
        return document(model, regels, List.of(), List.of(), List.of(werving), List.of());
    }

    /** K1 guards H1 and recruits J1 and J2; the input has J1 in H1 already. */
    static JsonNode zaakD1() throws Exception {
        return TmsDocumenten.zaak(List.of(
                instantieMetRollen("K1", "Kustwacht", Map.of(), List.of(),
                        Map.of("bewaakte havens", List.of("H1"), "geworven jutters", List.of("J1", "J2"))),
                instantieMetRollen("H1", "Haven", Map.of(), List.of(), Map.of("havenjutters", List.of("J1"))),
                instantieMetRollen("J1", "Jutter", Map.of(), List.of(), Map.of()),
                instantieMetRollen("J2", "Jutter", Map.of(), List.of(), Map.of())),
                List.of(waarde("H1", "jutteraantal", "2")));
    }

    /** K1 guards H1 and H2 and recruits J1: J1 would be a havenjutter of both. */
    static JsonNode zaakD2() throws Exception {
        return TmsDocumenten.zaak(List.of(
                instantieMetRollen("K1", "Kustwacht", Map.of(), List.of(),
                        Map.of("bewaakte havens", List.of("H1", "H2"), "geworven jutters", List.of("J1"))),
                instantieMetRollen("H1", "Haven", Map.of(), List.of(), Map.of()),
                instantieMetRollen("H2", "Haven", Map.of(), List.of(), Map.of()),
                instantieMetRollen("J1", "Jutter", Map.of(), List.of(), Map.of())),
                List.of(waarde("H1", "jutteraantal", "1")));
    }

    @Test void een_feitcreatie_legt_elke_link_een_keer() throws Exception {
        Path ir = documentD();
        assertEquals("gespeeld", TmsFoutWitnessTest.speel(ir, zaakD1(), DroolsBackend.Branch.TRUTH_MAINTENANCE));
        DroolsBackend.Build b = DroolsBackend.build(ir, DroolsBackend.Branch.TRUTH_MAINTENANCE);
        for (List<Rule> volgorde : TmsDocumenten.steekproef(b.tmsTranspiler().model().getRules(), 20261008L, 100)) {
            List<String> namen = volgorde.stream().map(Rule::getName).toList();
            Object k1 = TmsDocumenten.boon(b.beanLoader(), "Kustwacht", Map.of(), List.of());
            Object h1 = TmsDocumenten.boon(b.beanLoader(), "Haven", Map.of(), List.of());
            Object j1 = TmsDocumenten.boon(b.beanLoader(), "Jutter", Map.of(), List.of());
            Object j2 = TmsDocumenten.boon(b.beanLoader(), "Jutter", Map.of(), List.of());
            zetRol(h1, "kustwacht", k1);
            zetRol(j1, "werver", k1);
            zetRol(j2, "werver", k1);
            zetRol(j1, "haven", h1);
            List<Object[]> links = new ArrayList<>();
            assertNull(vuur(b, volgorde, List.of(k1, h1, j1, j2), sessie -> { },
                    sessie -> links.addAll(koppelingen(sessie, "havenlid"))), () -> "volgorde " + namen);
            assertGetal("2", TmsDocumenten.lees(h1, "jutteraantal"), "jutteraantal in volgorde " + namen);
            // the link of the input, and the one link the creation made, in the phase of generation 1; the creation
            // fired once
            assertEquals(2, links.size(), () -> "links in volgorde " + namen);
            for (Object[] l : links) {
                assertTrue(l[0] == h1, () -> "haven in volgorde " + namen);
                assertEquals(l[1] == j1 ? 0 : 1, l[2], () -> "fase in volgorde " + namen);
            }
        }
    }

    @Test void een_tweede_vulling_van_een_naar_een_rol_is_een_fout() throws Exception {
        Path ir = documentD();
        String zin = "IllegalStateException: de naar-één-rol \"haven\" van een Jutter heeft twee verschillende vullingen;"
                + " een tweede vulling is een fout, geen overschrijving (RUNTIME-SEMANTICS ch. 5.5)";
        assertEquals(zin, TmsFoutWitnessTest.speel(ir, zaakD2(), DroolsBackend.Branch.TRUTH_MAINTENANCE));
        DroolsBackend.Build b = DroolsBackend.build(ir, DroolsBackend.Branch.TRUTH_MAINTENANCE);
        for (List<Rule> volgorde : TmsDocumenten.steekproef(b.tmsTranspiler().model().getRules(), 20261008L, 100)) {
            List<String> namen = volgorde.stream().map(Rule::getName).toList();
            Object k1 = TmsDocumenten.boon(b.beanLoader(), "Kustwacht", Map.of(), List.of());
            Object h1 = TmsDocumenten.boon(b.beanLoader(), "Haven", Map.of(), List.of());
            Object h2 = TmsDocumenten.boon(b.beanLoader(), "Haven", Map.of(), List.of());
            Object j1 = TmsDocumenten.boon(b.beanLoader(), "Jutter", Map.of(), List.of());
            zetRol(h1, "kustwacht", k1);
            zetRol(h2, "kustwacht", k1);
            zetRol(j1, "werver", k1);
            String fout = null;
            try {
                vuur(b, volgorde, List.of(k1, h1, h2, j1), sessie -> { }, sessie -> { });
            } catch (IllegalStateException e) {
                fout = "IllegalStateException: " + e.getMessage();
            }
            assertEquals(zin, fout, () -> "volgorde " + namen);
        }
    }

    // ---------------- document F: one generation at a time, and the budget ----------------

    /** A chain that does not stop: every Wachter without an opvolger makes one. The condition reads only the count,
     *  which an accumulate over no contribution answers at once, so a new Wachter would match it in the phase that made
     *  it if the phase did not hold it back. Each generation makes one Wachter on the last, at one depth more, so the
     *  101st generation would make one at depth 101, and there the creation budget ends the run, at the making
     *  (RUNTIME-SEMANTICS ch. 5.5). */
    static Path documentF() throws Exception {
        String model = TmsDocumenten.model(List.of(TmsDocumenten.objecttype("Wachter", List.of(
                attribuut("opvolgeraantal", GEHEEL)), List.of())),
                List.of(feittypeEenNaarVeel("opvolging", "voorganger", "Wachter", "opvolger", "Wachter", "opvolgers")));
        List<String> regels = List.of(
                rekenregel("opvolgeraantal", "BEREKENING", "Wachter", "opvolgeraantal", AANTAL_OPVOLGERS, "null"));
        return document(model, regels, List.of(), List.of(objectcreatie("werf", "Wachter", "Wachter", List.of(),
                vergelijking("equal", AANTAL_OPVOLGERS, getal("0")), "opvolging", "opvolger")), List.of(),
                List.of(regelgroep("estafette", List.of("werf"))));
    }

    static JsonNode zaakF() throws Exception {
        return TmsDocumenten.zaak(List.of(instantieMetRollen("W1", "Wachter", Map.of(), List.of(), Map.of())),
                List.of(waarde("W1", "opvolgeraantal", "1")));
    }

    private static final String BUDGET = TmsCreatiestratumWitnessTest.budget("werf", 0, "Wachter");

    /** Until 2026-10-09 the driver counted generations, and the 101st still made its Wachter before the run ended
     *  without a fixpoint, "geen fixpunt binnen 100 generaties: generatie 101 maakt nog 1 object of link", 101 objects
     *  made; the mint at depth 101 is now the fault at the making, and 100 objects are made. */
    @Test void een_object_is_pas_in_de_volgende_generatie_onderwerp() throws Exception {
        Path ir = documentF();
        assertEquals("case: " + BUDGET, TmsFoutWitnessTest.speel(ir, zaakF(), DroolsBackend.Branch.TRUTH_MAINTENANCE));
        DroolsBackend.Build b = DroolsBackend.build(ir, DroolsBackend.Branch.TRUTH_MAINTENANCE);
        for (List<Rule> volgorde : TmsDocumenten.steekproef(b.tmsTranspiler().model().getRules(), 20261008L, 100)) {
            List<String> namen = volgorde.stream().map(Rule::getName).toList();
            Object w1 = TmsDocumenten.boon(b.beanLoader(), "Wachter", Map.of(), List.of());
            List<Object[]> gemaakt = new ArrayList<>();
            assertEquals(BUDGET, vuur(b, volgorde, List.of(w1), sessie -> { }, sessie -> gemaakt.addAll(gemaakt(sessie))),
                    () -> "volgorde " + namen);
            // one object in each phase, the subject of each the object of the one before
            assertEquals(100, gemaakt.size(), () -> "objecten in volgorde " + namen);
            Object onderwerp = w1;
            for (int i = 0; i < gemaakt.size(); i++) {
                Object[] g = gemaakt.get(i);
                int fase = i + 1;
                assertTrue(g[0] == onderwerp, () -> "keten in volgorde " + namen);
                assertEquals(fase, g[2], () -> "fase in volgorde " + namen);
                onderwerp = g[1];
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
    @Test void een_link_is_pas_in_de_volgende_generatie_pad() throws Exception {
        Path d = CORPUS.resolve("golden").resolve("anchor").resolve("jutterswet-erflijn");
        assertEquals("PLAYED", CaseRunner.viaTruthMaintenance(d.resolve("ir.json"), List.of(d.resolve("case.json")))
                .status());
        DroolsBackend.Build b = DroolsBackend.build(d.resolve("ir.json"), DroolsBackend.Branch.TRUTH_MAINTENANCE);
        for (List<Rule> volgorde : TmsDocumenten.steekproef(b.tmsTranspiler().model().getRules(), 20261008L, 100)) {
            List<String> namen = volgorde.stream().map(Rule::getName).toList();
            List<Object> w = new ArrayList<>();
            for (int i = 0; i < 4; i++) w.add(TmsDocumenten.boon(b.beanLoader(), "Wachter", Map.of(), List.of()));
            for (int i = 0; i < 3; i++) {
                zetRol(w.get(i), "opvolger", w.get(i + 1));
                zetRol(w.get(i + 1), "voorganger", w.get(i));
            }
            List<Object[]> links = new ArrayList<>();
            assertNull(vuur(b, volgorde, w, sessie -> { }, sessie -> links.addAll(koppelingen(sessie, "erflijn"))),
                    () -> "volgorde " + namen);
            assertEquals(6, links.size(), () -> "links in volgorde " + namen);
            for (Object[] l : links) {
                int afstand = w.indexOf(l[1]) - w.indexOf(l[0]);
                assertEquals(afstand == 1 ? 1 : afstand + 1, l[2],
                        () -> "fase van een link over " + afstand + " in volgorde " + namen);
            }
            for (int i = 0; i < 4; i++)
                assertGetal(String.valueOf(3 - i), TmsDocumenten.lees(w.get(i), "erfvolgeraantal"),
                        "erfvolgeraantal van W" + (i + 1) + " in volgorde " + namen);
        }
    }

    /** The two sides of the budget the reference interpreter and the ASP backend pin: a chain of 97 mints plays
     *  ({@code synth2-budgetgrens-onder}), and the chains of the three red cases fault at the mint at depth 101
     *  ({@code corpus/rood} {@code jutterswet-budgetgrens-over}, {@code finite-chain-over-budget} and
     *  {@code runaway-recursion}), with a sentence that names the creation, its stratum, the depth of the mint and the
     *  type and depth of its subject. The red cases pin the same place for the other two: the reference interpreter's
     *  fragment names the same creation, stratum and depth, and the ASP backend's names its witness on a mint of that
     *  creation, whose depth its own suite checks (the gate's fragment cannot carry a term of 101 levels). Until
     *  2026-10-09 the three executors pinned only the shared words "creation budget exhausted", which a fault on
     *  another object or at another depth satisfied as well. A red case pins no expectation, so it plays here with
     *  none. */
    @Test void het_creatiebudget_ligt_waar_het_corpus_het_pint() throws Exception {
        Path onder = CORPUS.resolve("golden").resolve("synth2-budgetgrens-onder");
        assertEquals("PLAYED", CaseRunner.viaTruthMaintenance(onder.resolve("ir.json"),
                List.of(onder.resolve("case1.json"))).status());
        for (Map.Entry<String, String> rood : Map.of("jutterswet-budgetgrens-over", "werf de lange keten",
                "finite-chain-over-budget", "werf de lange keten", "runaway-recursion", "werf eeuwig").entrySet()) {
            Path d = CORPUS.resolve("rood").resolve(rood.getKey());
            JsonNode zaak = TmsDocumenten.JSON.readTree(Files.readString(d.resolve("case1.json")));
            ((com.fasterxml.jackson.databind.node.ObjectNode) zaak).putArray("expected");
            String zin = TmsFoutWitnessTest.speel(d.resolve("ir.json"), zaak, DroolsBackend.Branch.TRUTH_MAINTENANCE);
            assertEquals("case: " + TmsCreatiestratumWitnessTest.budget(rood.getValue(), 0, "Wachter"), zin,
                    rood.getKey());
            JsonNode pin = zaak.get("expectedFault");
            assertEquals("creation budget exhausted: object creation \"" + rood.getValue()
                    + "\" of stratum 0 would mint at creation depth 101", pin.get("ref").asText(),
                    () -> rood.getKey() + ": het fragment van ref noemt de creatie, het stratum en de diepte van deze tak");
            assertEquals("a fail-fast guard fired: viol(\"creation budget exhausted\",oc0(", pin.get("clingo").asText(),
                    () -> rood.getKey() + ": het fragment van clingo noemt de getuige op een aanmaak van de creatie");
        }
    }

    @Test void de_wrakboekingen_spelen() throws Exception {
        for (String g : List.of("jutterswet-wrakboeking", "jutterswet-wrakboeking-rgs", "jutterswet-wrakboekwaarde")) {
            Path d = CORPUS.resolve("golden").resolve(g);
            List<Path> zaken;
            try (var s = Files.list(d)) {
                zaken = s.filter(p -> p.getFileName().toString().matches("case.*\\.json")).sorted().toList();
            }
            CaseRunner.Result r = CaseRunner.viaTruthMaintenance(d.resolve("ir.json"), zaken);
            assertEquals("PLAYED", r.status(), () -> g + ": " + r.reason() + " " + r.caseFailures());
        }
    }

    // ---------------- the wire of a creation ----------------

    /** A document with object creations, fact creations and rule groups beside the forms {@link TmsDocumenten} writes. */
    static Path document(String model, List<String> regels, List<String> defaults, List<String> objectcreaties,
                         List<String> feitcreaties, List<String> regelgroepen) throws Exception {
        String ir = "{\"format\":\"uchar-ir-v1\",\"model\":" + model + ",\"rules\":{\"rules\":[" + String.join(",", regels)
                + "],\"dayKindDefinitions\":[],\"objectCreations\":[" + String.join(",", objectcreaties)
                + "],\"allocations\":[],\"consistencies\":[],\"uniquenesses\":[],\"factCreations\":["
                + String.join(",", feitcreaties) + "],\"startPointDeterminations\":[],\"ruleGroups\":["
                + String.join(",", regelgroepen) + "],\"prioritizedDefaults\":[" + String.join(",", defaults)
                + "]},\"diagnostics\":[]}";
        Path tmp = Files.createTempFile("tms-creatie", ".json");
        tmp.toFile().deleteOnExit();
        return Files.writeString(tmp, ir);
    }

    /** "Een {@code subject} heeft een {@code rol} met …": an object creation of one always-valid version, its
     *  {@code connectingRole} the role {@code rol} of {@code feittype}, the role the created object plays. */
    static String objectcreatie(String naam, String subject, String nieuw, List<String> inits, String voorwaarde,
                                String feittype, String rol) {
        return "{\"node\":\"ObjectCreation\",\"name\":\"" + naam + "\",\"versions\":[{\"createdType\":\"" + nieuw
                + "\",\"subject\":\"" + subject + "\",\"initialisations\":[" + String.join(",", inits) + "]"
                + (voorwaarde.equals("null") ? "" : ",\"condition\":" + voorwaarde)
                + ",\"validity\":{\"node\":\"Validity\"},\"connectingRole\":" + rolbinding(feittype, rol) + "}]}";
    }

    /** An initialisation of attribute {@code lid} of the created object. */
    static String init(String lid, String waarde) {
        return "{\"node\":\"MemberInit\",\"member\":\"" + lid + "\",\"value\":" + waarde + ",\"characteristic\":false}";
    }

    /** "Een {@code doelRol} van een {@code anker} is een … van …": a fact creation of one always-valid version over
     *  {@code pad}, each step a RoleBinding. */
    static String feitcreatie(String naam, String feittype, String ankerRol, String doelRol, String anker, List<String> pad,
                              String voorwaarde) {
        return "{\"node\":\"FactCreation\",\"name\":\"" + naam + "\",\"versions\":[{\"factType\":\"" + feittype
                + "\",\"anchorRole\":\"" + ankerRol + "\",\"targetRole\":\"" + doelRol + "\",\"anchor\":\"" + anker
                + "\",\"path\":[" + String.join(",", pad) + "]"
                + (voorwaarde.equals("null") ? "" : ",\"condition\":" + voorwaarde) + ",\"validity\":{\"node\":\"Validity\"}}]}";
    }

    /** A recursive rule group of {@code leden}. */
    static String regelgroep(String naam, List<String> leden) {
        List<String> l = new ArrayList<>();
        for (String lid : leden) l.add("\"" + lid + "\"");
        return "{\"node\":\"RuleGroup\",\"name\":\"" + naam + "\",\"recursive\":true,\"members\":[" + String.join(",", l)
                + "],\"source\":{\"snippet\":\"" + naam + "\"}}";
    }

    // ---------------- firing through the driver ----------------

    /**
     * Fires {@code bonen} through the rules of {@code b} added in {@code volgorde}, with the backend's own driver
     * ({@code DroolsBackend.vuur}, reached by name: this witness also compiles against the code before creation was
     * carried, whose build refuses these documents before any fire). {@code voor} sees the session before the fire. The
     * fire must stay below the cap. At a fixpoint the derivations are written back and null is returned; at the
     * generation budget the driver's sentence. {@code na} sees the session either way, before it is disposed.
     */
    static String vuur(DroolsBackend.Build b, List<Rule> volgorde, List<Object> bonen, Consumer<KieSession> voor,
                       Consumer<KieSession> na) throws Exception {
        ClassLoader vorige = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(b.beanLoader());
        KieSession sessie = TmsDocumenten.kieBase(b, volgorde).newKieSession();
        try {
            voor.accept(sessie);
            for (Object boon : bonen) sessie.insert(boon);
            Method m = DroolsBackend.class.getDeclaredMethod("vuur", DroolsBackend.Build.class, KieSession.class, int.class);
            m.setAccessible(true);
            Object vuring;
            try {
                vuring = m.invoke(null, b, sessie, 100_000);
            } catch (InvocationTargetException e) {
                if (e.getCause() instanceof RuntimeException r) throw r;
                throw e;
            }
            Method vuringen = vuring.getClass().getDeclaredMethod("vuringen");
            Method geenFixpunt = vuring.getClass().getDeclaredMethod("geenFixpunt");
            vuringen.setAccessible(true);
            geenFixpunt.setAccessible(true);
            int n = (Integer) vuringen.invoke(vuring);
            String zin = (String) geenFixpunt.invoke(vuring);
            if (n >= 100_000)
                throw new AssertionError("vuurplafond in volgorde " + volgorde.stream().map(Rule::getName).toList());
            if (zin == null) DroolsBackend.zetAfgeleideWaarden(b, sessie);
            na.accept(sessie);
            return zin;
        } finally {
            sessie.dispose();
            Thread.currentThread().setContextClassLoader(vorige);
        }
    }

    /** The marks of the object creations in {@code sessie} ({@code Gemaakt}, read by name), as {subject, object,
     *  phase}, in the order of their phase. */
    static List<Object[]> gemaakt(KieSession sessie) {
        List<Object[]> uit = new ArrayList<>();
        for (Object f : sessie.getObjects(o -> o.getClass().getSimpleName().equals("Gemaakt")))
            uit.add(new Object[]{veld(f, "onderwerp"), veld(f, "object"), veld(f, "fase")});
        uit.sort(java.util.Comparator.comparingInt(g -> (Integer) g[2]));
        return uit;
    }

    /** The links of {@code relatie} in {@code sessie} ({@code Koppeling}, read by name), as {first, second, phase}. */
    static List<Object[]> koppelingen(KieSession sessie, String relatie) {
        List<Object[]> uit = new ArrayList<>();
        for (Object f : sessie.getObjects(o -> o.getClass().getSimpleName().equals("Koppeling")))
            if (relatie.equals(veld(f, "relatie"))) uit.add(new Object[]{veld(f, "eerste"), veld(f, "tweede"), veld(f, "fase")});
        return uit;
    }

    private static Object veld(Object feit, String naam) {
        try {
            return feit.getClass().getMethod(naam).invoke(feit);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    /** Counts into {@code teller} every {@link Waarde} of attribute {@code attribuut} with value {@code lexeem} that is
     *  inserted: an intermediate state of the defaults group of that attribute passing. */
    private static DefaultRuleRuntimeEventListener waardeteller(String attribuut, String lexeem, int[] teller) {
        BigDecimal v = new BigDecimal(lexeem);
        return new DefaultRuleRuntimeEventListener() {
            @Override public void objectInserted(ObjectInsertedEvent e) {
                if (e.getObject() instanceof Waarde w && w.attribuut().naam().equalsIgnoreCase(attribuut)
                        && w.waarde() instanceof BigDecimal x && x.compareTo(v) == 0) teller[0]++;
            }
        };
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
